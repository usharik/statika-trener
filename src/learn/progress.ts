import { Store } from '../trainer/store';
import { storage } from '../trainer/settings';
import { COURSES, type CourseMeta } from './titles';

const P = 'statika.learn.';

/** Postup ve výkladu uložený v prohlížeči (shodné klíče jako v aplikaci, s předponou). */
export class LearnProgress extends Store {
  private getInt(k: string) {
    return Number(storage.get(P + k) ?? 0) || 0;
  }

  private putInt(k: string, v: number) {
    storage.set(P + k, String(v));
  }

  seen(lesson: number): number {
    return this.getInt(`seen.${lesson}`);
  }

  isDone(lesson: number): boolean {
    return this.getInt(`done.${lesson}`) === 1;
  }

  markSeen(lesson: number, cardIndex: number) {
    if (cardIndex + 1 > this.seen(lesson)) {
      this.putInt(`seen.${lesson}`, cardIndex + 1);
      this.emit();
    }
  }

  /** karta, jejíž úkoly žák opravdu splnil (jen ta se při další návštěvě nemusí znovu dělat) */
  cardDone(lesson: number, card: number): boolean {
    return this.getInt(`card.${lesson}.${card}`) === 1;
  }

  markCardDone(lesson: number, card: number) {
    if (!this.cardDone(lesson, card)) this.putInt(`card.${lesson}.${card}`, 1);
  }

  markDone(lesson: number) {
    if (!this.isDone(lesson)) {
      this.putInt(`done.${lesson}`, 1);
      this.emit();
    }
  }

  /** první lekce kurzu, která ještě není hotová (všechny hotové → první lekce kurzu) */
  next(of: CourseMeta = this.courseMeta): number {
    return of.lessons.find((l) => !this.isDone(l)) ?? of.lessons[0];
  }

  get course(): 'st' | 'tg' {
    const c = storage.get(P + 'course');
    return c === 'tg' ? 'tg' : 'st';
  }

  set course(v: 'st' | 'tg') {
    if (v === this.course) return;
    storage.set(P + 'course', v);
    this.emit();
  }

  get courseMeta(): CourseMeta {
    return COURSES.find((c) => c.id === this.course) ?? COURSES[0];
  }

  get firstRunAsked(): boolean {
    return this.getInt('firstRun') === 1;
  }

  setFirstRunAsked() {
    this.putInt('firstRun', 1);
  }
}
