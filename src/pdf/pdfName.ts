import type { Problem } from '../model/types';
import type { Tr } from '../i18n';

/** Název souboru PDF podle čísla úlohy a obsahu. */
export const problemPdfName = (p: Problem, withSolution: boolean, tr: Tr) => `statika-${p.seed}-${tr(withSolution ? 'pdfFileSolution' : 'pdfFileTask')}.pdf`;
