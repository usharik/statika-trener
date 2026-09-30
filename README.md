# Statika · nosníky — trenažér

**Online:** https://usharik.github.io/statika-trener/

Webová aplikace (React + TypeScript + Vite) pro trénink výpočtu reakcí staticky určitých nosníků
na úrovni SPŠ / technického lycea. Rozhraní česky i rusky.

## Spuštění

```bash
npm install
npm run dev        # http://localhost:5173
npm test           # generátor (18 000 úloh), mechanika, rozbor rovnic
npm run build      # statická verze do dist/
```

## Jak hra probíhá

1. **Uvolnění** – každou vazbu (pevná / posuvná kloubová podpora, vetknutí, vnitřní kloub) nahradíš reakcemi, pak určíš počet neznámých a rovnic (statická určitost).
2. **Zatížení** – spojité zatížení → výslednice Q a její poloha, šikmé síly → složky.
3. **Rovnice** – rovnice ΣFx, ΣFy, ΣM (bod si volíš) a M_K = 0 v kloubu píšeš jako text
   (paleta značek a číslic, např. `RB*6 - F1*2 - Q*4,5 = 0`). Rovnice se rozebere a porovná se
   správnou až na násobek (libovolný kladný smysl, tvar „… = …“, čísla i sin/cos); nápovědy
   ukazují na konkrétní člen a umí jeden člen doplnit.
4. **Výpočet** – dopočítáš reakce, zkontroluje se to s tolerancí 1 %.
5. **Hotovo** – obrázek se skutečnými směry reakcí, kontrola momentovou podmínkou k jinému bodu.

Nápovědy nikdy neříkají „špatně“: nejdřív připomenou princip, pak ukážou na konkrétní sílu
(případně zakreslí rameno), teprve nakonec doplní odpověď.

## Úlohy

- typy: prostý nosník, nosník s převislým koncem/konci, vetknutý nosník, Gerberův nosník s kloubem
  (vetknutí + kloub + posuvná podpora; pevná + posuvná + kloub + posuvná v obou uspořádáních),
- zatížení: svislé a šikmé (30°, 45°, 60°) síly, spojité rovnoměrné zatížení, osamělý moment,
- 3 obtížnosti, deterministický generátor: číslo úlohy je v adrese (`#12345`) a dá se sdílet,
- každá úloha je ověřena řešením soustavy rovnic (nesingulární → staticky určitá a geometricky nepohyblivá).

## Struktura

- `src/model/` – typy, generátor, mechanika (reakce, rovnice, Gaussova eliminace), rozbor zapsaných rovnic (`eqparse.ts`), testy
- `src/components/BeamDrawing.tsx` – technický výkres v SVG (podpory, kóty, síly, reakce, rozmístění popisků)
- `src/steps/` – jednotlivé kroky hry
- `src/i18n.ts` – texty a nápovědy (cs, ru)

## Nasazení

Každý push do `main` spustí GitHub Actions (`.github/workflows/pages.yml`): testy → build → GitHub Pages.
