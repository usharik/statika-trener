#!/usr/bin/env python3
# Export textů kurzu „Základy trigonometrie“ (lekce, slovníček, procvičování) do docs/trigonometrie-texty.md.
# Spouštět z adresáře mobile/. Stejný formát jako docs/zaklady-texty.md (scripts/export_learn_texts.py).
import re, glob, os

ROOT = 'shared/src/commonMain/kotlin/cz/usharik/statika/'
TRIG = ROOT + 'learn/trig/'
PRACTICE = ROOT + 'practice/'

# L("cs", "ru") – i víceřádkové; řetězce s escapy
PAT = re.compile(r'L\(\s*"((?:[^"\\]|\\.)*)"\s*,\s*"((?:[^"\\]|\\.)*)"\s*,?\s*\)', re.S)
FN = re.compile(r'(?:private |internal )?fun CardScope\.(\w+)\(')


def rows(text):
    out = []
    for m in PAT.finditer(text):
        cs, ru = (g.replace('|', '\\|').replace('\\"', '"') for g in m.groups())
        out.append(f'| {cs} | {ru} |')
    return out


def table(lines):
    return ['| čeština | русский |', '|---|---|'] + lines + ['']


out = ['# Základy trigonometrie — texty k vyčtení / тексты для вычитки', '',
       'Vygenerováno skriptem `scripts/export_trig_texts.py` z `learn/trig/` a `practice/`. Opravy pište sem nebo do kódu; ',
       'značky: `[[F_x]]` = symbol s indexem, `**…**` = tučně, `{x}` = doplní se číslo.', '']

for i in range(1, 9):
    path = TRIG + f'TrigLesson{i}.kt'
    if not os.path.exists(path):
        continue
    src = open(path).read()
    t = re.search(r'title = L\("([^"]*)", "([^"]*)"\)', src)
    head = f'{t.group(1)} / {t.group(2)}' if t else ''
    out += [f'## Lekce {i}: {head}', '']
    parts = FN.split(src)
    if len(parts) == 1:
        out += table(rows(src))
        continue
    for k in range(1, len(parts), 2):
        name, body = parts[k], parts[k + 1]
        r = rows(body)
        if r:
            out += [f'### {name}', ''] + table(r)

g = TRIG + 'TrigGlossary.kt'
if os.path.exists(g):
    out += ['## Slovníček / Словарик', ''] + table(rows(open(g).read()))

out += ['## Procvičování / Тренировка', '']
for name, path in [('Sady a obrazovka', PRACTICE + 'Exercises.kt'), ('Typické chyby (Traps)', PRACTICE + 'Traps.kt'),
                   ('Společné texty úloh', PRACTICE + 'Exercise.kt'), ('Obrazovka procvičování', ROOT + 'learn/PracticeScreen.kt')]:
    if os.path.exists(path):
        r = rows(open(path).read())
        if r:
            out += [f'### {name}', ''] + table(r)
for path in sorted(glob.glob(PRACTICE + 'types/*.kt')):
    r = rows(open(path).read())
    if r:
        out += [f'### Úloha {os.path.basename(path)[:-3]}', ''] + table(r)

os.makedirs('docs', exist_ok=True)
open('docs/trigonometrie-texty.md', 'w').write('\n'.join(out) + '\n')
print(len(out), 'lines')
