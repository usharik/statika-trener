import type { Problem, SupportType } from '../model/types';
import { reactionSetOf, unknownsOf, type ReactionSet } from '../model/mechanics';
import { parseNum } from '../format';
import type { TKey } from '../i18n';
import { msg, type Msg, type TrainerSession } from './session';

export type ReleaseTask =
  | { kind: 'support'; id: string; label: string; type: SupportType }
  | { kind: 'hinge'; id: string; label: string }
  | { kind: 'count' };

export type HingeAnswer = 'forces' | 'all' | 'none';
export const REACTION_SETS: ReactionSet[] = ['xy', 'y', 'x', 'xym'];
export const HINGE_ANSWERS: HingeAnswer[] = ['forces', 'all', 'none'];

const setOf = (type: SupportType) => reactionSetOf({ id: '', label: '', x: 0, type });

/** Krok 1: uvolnění – u každé vazby výběr reakcí, u kloubu co přenáší, nakonec statická určitost. */
export class ReleaseStep {
  readonly tasks: ReleaseTask[];
  idx = 0;
  msgs: Msg[] = [];
  hintLevel = 0;
  solved = false;
  /** vybraná odpověď (ReactionSet nebo HingeAnswer) */
  picked: string | null = null;
  nU = '';
  nE = '';
  log: Msg[] = [];
  readonly nUnknowns: number;
  readonly nEqs: number;

  private p: Problem;
  private session: TrainerSession;

  constructor(p: Problem, session: TrainerSession) {
    this.p = p;
    this.session = session;
    this.tasks = [
      ...p.supports.map((s) => ({ kind: 'support' as const, id: s.id, label: s.label, type: s.type })),
      ...p.hinges.map((h) => ({ kind: 'hinge' as const, id: h.id, label: h.label })),
      { kind: 'count' as const },
    ];
    this.nUnknowns = unknownsOf(p).length;
    this.nEqs = 3 + p.hinges.length;
  }

  get task(): ReleaseTask {
    return this.tasks[this.idx];
  }

  get maxHint(): number {
    return this.task.kind === 'support' ? 3 : 2;
  }

  private changed() {
    this.session.emit();
  }

  setNU(v: string) {
    this.nU = v;
    this.changed();
  }

  setNE(v: string) {
    this.nE = v;
    this.changed();
  }

  advance() {
    const next = this.idx + 1;
    if (next >= this.tasks.length) return this.session.releaseDone();
    const ok = this.msgs.find((m) => m.tone === 'ok');
    if (ok) this.log = [...this.log, ok];
    this.idx = next;
    this.msgs = [];
    this.hintLevel = 0;
    this.solved = false;
    this.picked = null;
    const t = this.tasks[next];
    this.session.activeSupport = t.kind === 'support' ? t.id : null;
    this.changed();
  }

  pickSet(set: ReactionSet) {
    const t = this.task;
    if (t.kind !== 'support' || this.solved) return;
    this.picked = set;
    if (set === setOf(t.type)) {
      this.solved = true;
      this.session.releaseSupport(t.id);
      this.msgs = [msg('ok', (tr) => tr(`s1_ok_${t.type}` as TKey))];
    } else {
      this.msgs = [msg('nudge', (tr) => tr(`s1_${t.type}_${set}` as TKey, { p: t.label }), [6, 1])];
    }
    this.changed();
  }

  pickHinge(a: HingeAnswer) {
    const t = this.task;
    if (t.kind !== 'hinge' || this.solved) return;
    this.picked = a;
    if (a === 'forces') {
      this.solved = true;
      this.msgs = [msg('ok', (tr) => tr('s1_hinge_ok', { p: t.label }))];
    } else {
      this.msgs = [msg('nudge', (tr) => tr(a === 'all' ? 's1_hinge_all' : 's1_hinge_none'), [9, 0])];
    }
    this.changed();
  }

  checkCount() {
    if (this.solved) return;
    const u = parseNum(this.nU);
    const e = parseNum(this.nE);
    const out: Msg[] = [];
    if (u !== this.nUnknowns) out.push(msg('nudge', (tr) => tr('count_u'), [9, 2]));
    if (e !== this.nEqs) out.push(msg('nudge', (tr) => tr(this.p.hinges.length && e === 3 ? 'count_eh' : 'count_e3'), [9, 2]));
    if (!out.length) {
      this.solved = true;
      out.push(msg('ok', (tr) => tr('count_ok', { n: this.nUnknowns })));
    }
    this.msgs = out;
    this.changed();
  }

  hint() {
    this.session.hint();
    const lvl = ++this.hintLevel;
    const t = this.task;
    let m: Msg;
    if (t.kind === 'support') {
      m = msg('hint', (tr) => {
        const setName = tr(`set_${setOf(t.type)}` as TKey);
        return lvl === 1 ? tr('s1_h1', { p: t.label }) : lvl === 2 ? tr(`s1_h2_${t.type}` as TKey) : tr('s1_h3', { p: t.label, set: setName });
      });
    } else if (t.kind === 'hinge') {
      m = msg('hint', (tr) => (lvl === 1 ? tr('s1_hinge_h1') : tr('s1_h3', { p: t.label, set: tr('hinge_forces') })));
    } else {
      m = msg('hint', (tr) => (lvl === 1 ? tr('count_h1') : tr('count_h2', { n: this.nUnknowns, r: this.nEqs })));
    }
    this.msgs = [...this.msgs.filter((x) => x.tone !== 'hint'), m];
    this.changed();
  }
}

/** Popisek sady reakcí, např. „[[R_Ax]], [[R_Ay]]“. */
export function setLabel(set: ReactionSet, label: string): string {
  const parts: Record<ReactionSet, string[]> = {
    xy: [`[[R_${label}x]]`, `[[R_${label}y]]`],
    y: [`[[R_${label}]]`],
    x: [`[[R_${label}x]]`],
    xym: [`[[R_${label}x]]`, `[[R_${label}y]]`, `[[M_${label}]]`],
  };
  return parts[set].join(', ');
}
