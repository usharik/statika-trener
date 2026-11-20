import { useEffect, useRef, useState, type ReactNode } from 'react';
import { PrimBuilder, type Prim, type Pt } from './prims';
import { useColors, alpha as withAlpha, type Colors } from './colors';

/**
 * Interaktivní obrázek karty (Kit.kt Stage). Souřadnice: šířka [w] jednotek, výška [h]; [draw] kreslí přes PrimBuilder.
 * [onDrag] dostává polohu prstu (start = true na začátku tahu), [onTap] klepnutí. Obrázek se při posouvání karty
 * přilepí k hornímu okraji (jen když zabírá nejvýš ~40 % výšky okna), aby úkol zůstal na očích.
 */
export function Stage({
  w = 400,
  h = 220,
  description = '',
  onDrag,
  onDragEnd,
  onTap,
  draw,
  sticky = true,
}: {
  w?: number;
  h?: number;
  description?: string;
  onDrag?: (p: Pt, start: boolean) => void;
  onDragEnd?: () => void;
  onTap?: (p: Pt) => void;
  draw: (b: PrimBuilder, c: Colors) => void;
  sticky?: boolean;
}) {
  const c = useColors();
  const b = new PrimBuilder(w, c);
  draw(b, c);
  const svg = useRef<SVGSVGElement>(null);
  const box = useRef<HTMLDivElement>(null);
  const [stick, setStick] = useState(false);
  // gesta: vždy volat aktuální obsluhu
  const handlers = useRef({ onDrag, onDragEnd, onTap });
  handlers.current = { onDrag, onDragEnd, onTap };

  useEffect(() => {
    if (!sticky) return;
    const el = box.current;
    if (!el) return;
    const upd = () => {
      const host = el.closest('.lesson-scroll') as HTMLElement | null;
      const hh = host?.clientHeight ?? window.innerHeight;
      setStick(el.offsetHeight < hh * 0.42);
    };
    upd();
    const ro = new ResizeObserver(upd);
    ro.observe(el);
    window.addEventListener('resize', upd);
    return () => {
      ro.disconnect();
      window.removeEventListener('resize', upd);
    };
  }, [sticky]);

  const toPt = (e: { clientX: number; clientY: number }): Pt => {
    const r = svg.current!.getBoundingClientRect();
    const k = r.width / w;
    return { x: (e.clientX - r.left) / k, y: (e.clientY - r.top) / k };
  };

  const drag = useRef<{ id: number; x: number; y: number; started: boolean } | null>(null);
  const interactive = !!onDrag || !!onTap;
  const onPointerDown = (e: React.PointerEvent) => {
    if (!interactive) return;
    drag.current = { id: e.pointerId, x: e.clientX, y: e.clientY, started: false };
    if (handlers.current.onDrag) (e.currentTarget as Element).setPointerCapture?.(e.pointerId);
  };
  const onPointerMove = (e: React.PointerEvent) => {
    const d = drag.current;
    if (!d || d.id !== e.pointerId || !handlers.current.onDrag) return;
    if (!d.started) {
      // práh pohybu jako v Compose (touch slop)
      if (Math.hypot(e.clientX - d.x, e.clientY - d.y) < 6) return;
      d.started = true;
      handlers.current.onDrag(toPt({ clientX: d.x, clientY: d.y }), true);
    }
    e.preventDefault();
    handlers.current.onDrag(toPt(e), false);
  };
  const end = (e: React.PointerEvent, cancel: boolean) => {
    const d = drag.current;
    if (!d || d.id !== e.pointerId) return;
    drag.current = null;
    if (d.started) handlers.current.onDragEnd?.();
    else if (!cancel) handlers.current.onTap?.(toPt(e));
  };

  return (
    <div ref={box} className={'stage-wrap' + (stick ? ' stage-sticky' : '')}>
      <div className="stage">
      <svg
        ref={svg}
        viewBox={`0 0 ${w} ${h}`}
        className={'stage-svg' + (onDrag ? ' stage-drag' : onTap ? ' stage-tap' : '')}
        role="img"
        aria-label={description.replace(/\[\[|\]\]|\*\*/g, '')}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={(e) => end(e, false)}
        onPointerCancel={(e) => end(e, true)}
      >
        {b.prims.map((p, i) => renderPrim(p, i, c))}
      </svg>
      </div>
    </div>
  );
}

const SERIF = "Cambria, 'Times New Roman', serif";

function renderPrim(p: Prim, i: number, c: Colors): ReactNode {
  const glow = withAlpha('#ffba00', 0.55);
  switch (p.k) {
    case 'line':
      return (
        <g key={i} opacity={p.alpha}>
          {p.hl && <line x1={p.x1} y1={p.y1} x2={p.x2} y2={p.y2} stroke={glow} strokeWidth={p.width + 6} strokeLinecap="round" />}
          <line x1={p.x1} y1={p.y1} x2={p.x2} y2={p.y2} stroke={p.color} strokeWidth={p.width} strokeDasharray={p.dash ? p.dash.join(' ') : undefined} />
        </g>
      );
    case 'poly': {
      const d = p.pts.map((q) => q.join(',')).join(' ');
      return (
        <g key={i} opacity={p.alpha}>
          {p.hl && <polygon points={d} fill="none" stroke={glow} strokeWidth={6} strokeLinejoin="round" />}
          <polygon points={d} fill={p.fill ?? 'none'} stroke={p.stroke ?? 'none'} strokeWidth={p.stroke ? p.width : 0} strokeLinejoin="round" />
        </g>
      );
    }
    case 'rect':
      return <rect key={i} x={p.x} y={p.y} width={Math.max(0, p.w)} height={Math.max(0, p.h)} fill={p.fill ?? 'none'} stroke={p.stroke ?? 'none'} strokeWidth={p.stroke ? p.width : 0} opacity={p.alpha} />;
    case 'circle':
      return (
        <circle key={i} cx={p.cx} cy={p.cy} r={Math.max(0, p.r)} fill={p.fill ?? 'none'} stroke={p.stroke ?? 'none'} strokeWidth={p.stroke ? p.width : 0} strokeDasharray={p.dash ? p.dash.join(' ') : undefined} opacity={p.alpha} />
      );
    case 'arc': {
      const d = arcPath(p.cx, p.cy, p.r, p.start, p.sweep);
      return (
        <g key={i} opacity={p.alpha}>
          {p.hl && <path d={d} fill="none" stroke={glow} strokeWidth={p.width + 6} strokeLinecap="round" />}
          <path d={d} fill="none" stroke={p.color} strokeWidth={p.width} />
        </g>
      );
    }
    case 'label': {
      const sym = p.sym;
      const iu = sym ? sym.indexOf('_') : -1;
      const main = sym ? (iu < 0 ? sym : sym.slice(0, iu)) : null;
      const sub = sym && iu >= 0 ? sym.slice(iu + 1) : '';
      return (
        <text
          key={i}
          x={p.x}
          y={p.y}
          textAnchor={p.anchor}
          fill={p.color}
          fontSize={p.size}
          fontWeight={p.bold ? 600 : 400}
          opacity={p.alpha}
          className="stage-text"
          style={{ fontVariantNumeric: 'tabular-nums' }}
        >
          {main && (
            <tspan fontStyle="italic" fontFamily={SERIF}>
              {main}
            </tspan>
          )}
          {sub && (
            <tspan dy={p.size * 0.25} fontSize={p.size * 0.75} fontFamily={SERIF}>
              {sub}
            </tspan>
          )}
          {p.text && <tspan dy={sub ? -p.size * 0.25 : 0}>{p.text}</tspan>}
        </text>
      );
    }
  }
  void c;
}

/** SVG oblouk: úhly ve stupních po směru hodin od +x (osa y dolů). */
export function arcPath(cx: number, cy: number, r: number, start: number, sweep: number): string {
  if (Math.abs(sweep) >= 359.99) {
    return `M${cx + r},${cy} A${r},${r} 0 1 1 ${cx - r},${cy} A${r},${r} 0 1 1 ${cx + r},${cy}`;
  }
  const a0 = (start * Math.PI) / 180;
  const a1 = ((start + sweep) * Math.PI) / 180;
  const x0 = cx + r * Math.cos(a0);
  const y0 = cy + r * Math.sin(a0);
  const x1 = cx + r * Math.cos(a1);
  const y1 = cy + r * Math.sin(a1);
  const large = Math.abs(sweep) > 180 ? 1 : 0;
  const sw = sweep > 0 ? 1 : 0;
  return `M${x0},${y0} A${r},${r} 0 ${large} ${sw} ${x1},${y1}`;
}
