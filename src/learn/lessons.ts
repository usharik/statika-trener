import type { ComponentType } from 'react';
import { cards as l1 } from './lessons/Lesson1';
import { cards as l2 } from './lessons/Lesson2';
import { cards as l3 } from './lessons/Lesson3';
import { cards as l4 } from './lessons/Lesson4';
import { cards as l5 } from './lessons/Lesson5';
import { cards as l6 } from './lessons/Lesson6';
import { cards as l7 } from './lessons/Lesson7';
import { cards as l8 } from './lessons/Lesson8';
import { cards as l9 } from './lessons/Lesson9';
import { cards as t1 } from './lessons/trig/TrigLesson1';
import { cards as t2 } from './lessons/trig/TrigLesson2';
import { cards as t3 } from './lessons/trig/TrigLesson3';
import { cards as t4 } from './lessons/trig/TrigLesson4';
import { cards as t5 } from './lessons/trig/TrigLesson5';
import { cards as t6 } from './lessons/trig/TrigLesson6';
import { cards as t7 } from './lessons/trig/TrigLesson7';
import { cards as t8 } from './lessons/trig/TrigLesson8';

/** Karty lekcí podle id (statika 1–9, trigonometrie 101–108). */
export const LESSON_CARDS: Record<number, ComponentType[]> = {
  1: l1,
  2: l2,
  3: l3,
  4: l4,
  5: l5,
  6: l6,
  7: l7,
  8: l8,
  9: l9,
  101: t1,
  102: t2,
  103: t3,
  104: t4,
  105: t5,
  106: t6,
  107: t7,
  108: t8,
};
