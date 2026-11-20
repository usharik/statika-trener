/* Rejstřík typů úloh procvičování (shodné s aplikací – practice/Exercises.kt). */
import { Rng } from '../../model/rng';
import type { Exercise, ExerciseType } from '../exercise';
import { E01, E02, E03, E04, E05, E06 } from './e01_06';
import { E07, E08, E09, E10, E11, E12 } from './e07_12';
import { E13, E14, E15, E16, E17, E17S } from './e13_17';
import { E18, E19, E20, E21, E22, E23 } from './e18_23';

export const EXERCISES: ExerciseType[] = [E01, E02, E03, E04, E05, E06, E07, E08, E09, E10, E11, E12, E13, E14, E15, E16, E17, E17S, E18, E19, E20, E21, E22, E23];
export const EXERCISE_BY_ID: Record<string, ExerciseType> = Object.fromEntries(EXERCISES.map((t) => [t.id, t]));

/** úloha typu id pro číslo seed a úroveň level (deterministicky) */
export const makeExercise = (id: string, seed: number, level: number): Exercise => EXERCISE_BY_ID[id].make(new Rng(seed), level, seed);
