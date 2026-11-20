import { createContext, useContext } from 'react';

/** Barvy výkladu – shodné s index.css a s aplikací (ui/Theme.kt), světlé i tmavé téma. Hodnoty jsou CSS barvy. */
export interface Colors {
  dark: boolean;
  bg: string;
  paper: string;
  ink: string;
  inkSoft: string;
  line: string;
  ext: string;
  beam: string;
  load: string;
  loadFill: string;
  react: string;
  derived: string;
  accent: string;
  subtle: string;
  okBg: string;
  nudge: string;
  nudgeBg: string;
  warnBg: string;
  warnLine: string;
  hintBg: string;
  infoBg: string;
  infoIcon: string;
  chipReactBorder: string;
  chipLoadBorder: string;
  activeBorder: string;
  focusBg: string;
  star: string;
  highlight: string;
}

export const LIGHT: Colors = {
  dark: false,
  bg: '#f5f6f8',
  paper: '#ffffff',
  ink: '#1b2330',
  inkSoft: '#6b7482',
  line: '#e3e6eb',
  ext: '#c9ced6',
  beam: '#eef1f5',
  load: '#d1432b',
  loadFill: 'rgba(209,67,43,0.06)',
  react: '#2459d6',
  derived: '#7c3aed',
  accent: '#0f9d6e',
  subtle: '#f6f7f9',
  okBg: '#ecf8f2',
  nudge: '#f3bd55',
  nudgeBg: '#fff8eb',
  warnBg: '#fff1d6',
  warnLine: '#f0b43c',
  hintBg: '#eef3ff',
  infoBg: '#f3f4f6',
  infoIcon: '#9aa3af',
  chipReactBorder: '#c9d6f5',
  chipLoadBorder: '#f1cfc8',
  activeBorder: '#9db4ee',
  focusBg: '#f6f9ff',
  star: '#f5b301',
  highlight: 'rgba(255,186,0,0.95)',
};

export const DARK: Colors = {
  dark: true,
  bg: '#0f1216',
  paper: '#181c22',
  ink: '#e6e9ee',
  inkSoft: '#98a2b0',
  line: '#2b3139',
  ext: '#3a424d',
  beam: '#2a313a',
  load: '#ff7b5f',
  loadFill: 'rgba(255,123,95,0.09)',
  react: '#6f9fff',
  derived: '#b692ff',
  accent: '#36c48f',
  subtle: '#1f242b',
  okBg: '#15271f',
  nudge: '#e0a53a',
  nudgeBg: '#2a2317',
  warnBg: '#3a2e14',
  warnLine: '#d19a2a',
  hintBg: '#1a2438',
  infoBg: '#22272e',
  infoIcon: '#6b7482',
  chipReactBorder: '#2f4675',
  chipLoadBorder: '#5c3128',
  activeBorder: '#3f5fa6',
  focusBg: '#1b2330',
  star: '#f5b301',
  highlight: 'rgba(255,186,0,0.95)',
};

/** Barva s jinou průhledností (obdoba Color.copy(alpha = …) v Compose). */
export function alpha(color: string, a: number): string {
  let r: number, g: number, b: number;
  if (color.startsWith('#')) {
    const h = color.slice(1);
    r = parseInt(h.slice(0, 2), 16);
    g = parseInt(h.slice(2, 4), 16);
    b = parseInt(h.slice(4, 6), 16);
  } else {
    const m = color.match(/rgba?\(([^)]+)\)/);
    const p = m ? m[1].split(',').map((x) => parseFloat(x)) : [0, 0, 0];
    [r, g, b] = p;
  }
  return `rgba(${r},${g},${b},${a})`;
}

export const ColorsContext = createContext<Colors>(LIGHT);
export const useColors = () => useContext(ColorsContext);
