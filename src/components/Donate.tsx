import { useEffect, useRef, useState } from 'react';
import { useLang, useT } from '../i18n';

/**
 * Podpora autora: widget usharik.dev (stejný jako na usharik.dev/support/ a chords.usharik.dev).
 * Zdroj pravdy je tam (repozitář claudflare-domain-web, donate/config.json): částky a měny, platební stránka Stripe,
 * PayPal, Ko-fi a texty. Tady se jen načte skript a widget se připojí do kontejneru. Kdyby se skript nenačetl
 * (blokátor, bez internetu), zůstanou obyčejné odkazy.
 */
const WIDGET = 'https://usharik.dev/donate/widget.js';
export const SUPPORT_URL: Record<'cs' | 'ru', string> = { cs: 'https://usharik.dev/cs/support/', ru: 'https://usharik.dev/ru/support/' };
const KOFI = 'https://ko-fi.com/X8X8NI26';
const PAYPAL = 'https://www.paypal.me/usharik';

interface Widget {
  mount(element: HTMLElement): () => void;
}

let loading: Promise<Widget | null> | null = null;
function loadWidget(): Promise<Widget | null> {
  const ready = () => (window as unknown as { usharikDonate?: Widget }).usharikDonate ?? null;
  loading ??= new Promise((resolve) => {
    if (ready()) return resolve(ready());
    const script = Object.assign(document.createElement('script'), { src: WIDGET, async: true });
    script.onload = () => resolve(ready());
    script.onerror = () => {
      loading = null;
      resolve(null);
    };
    document.head.append(script);
  });
  return loading;
}

/** Celý blok s volbou částky a způsobu platby. */
export function DonateWidget() {
  const lang = useLang();
  const t = useT();
  const box = useRef<HTMLDivElement>(null);
  const [failed, setFailed] = useState(false);
  useEffect(() => {
    const element = box.current;
    if (!element) return;
    let unmount: (() => void) | null = null;
    let gone = false;
    void loadWidget().then((widget) => {
      if (gone) return;
      if (widget) unmount = widget.mount(element);
      else setFailed(true);
    });
    return () => {
      gone = true;
      unmount?.();
    };
  }, [lang]);
  return (
    <div className="donate">
      <div ref={box} key={lang} className="usharik-donate" data-usharik-donate data-lang={lang} data-site="statika" />
      {failed && (
        <p className="small">
          {t('donateFallback')}{' '}
          <a href={SUPPORT_URL[lang]} target="_blank" rel="noopener noreferrer">
            usharik.dev
          </a>
          ,{' '}
          <a href={KOFI} target="_blank" rel="noopener noreferrer">
            Ko-fi
          </a>{' '}
          ·{' '}
          <a href={PAYPAL} target="_blank" rel="noopener noreferrer">
            PayPal
          </a>
        </p>
      )}
    </div>
  );
}

/** Nenápadná výzva s tlačítkem, které rozbalí widget. */
export function DonatePrompt({ text, onClose }: { text: string; onClose?: () => void }) {
  const t = useT();
  const [open, setOpen] = useState(false);
  return (
    <div className="donate-prompt">
      <p className="donate-text">
        <span className="heart" aria-hidden>
          ♥
        </span>{' '}
        {text}
      </p>
      {open ? (
        <DonateWidget />
      ) : (
        <div className="actions">
          <button className="btn btn-donate" onClick={() => setOpen(true)}>
            ♥ {t('donate')}
          </button>
          {onClose && (
            <button className="btn btn-ghost" onClick={onClose}>
              {t('donateLater')}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
