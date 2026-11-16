#!/usr/bin/env python3
# Export textů výkladu „Základy statiky“ do docs/zaklady-texty.md (spouštět z adresáře mobile/).
import re,glob,os,sys
D='shared/src/commonMain/kotlin/cz/usharik/statika/learn/'
out=['# Základy statiky — texty k vyčtení / тексты для вычитки','',
     'Vygenerováno z `mobile/shared/src/commonMain/kotlin/cz/usharik/statika/learn/`. Opravy pište přímo sem nebo do kódu; ',
     'značky: `[[F_x]]` = symbol s indexem, `**…**` = tučně.','']
pat=re.compile(r'L\(\s*"((?:[^"\\]|\\.)*)"\s*,\s*"((?:[^"\\]|\\.)*)"\s*,?\s*\)',re.S)
fn=re.compile(r'private fun CardScope\.(\w+)\(')
for i in range(1,10):
    src=open(D+f'Lesson{i}.kt').read()
    t=re.search(r'title = L\("([^"]*)", "([^"]*)"\)',src)
    out+=[f'## Lekce {i}: {t.group(1)} / {t.group(2)}','']
    # split by card functions
    parts=fn.split(src)
    pre=parts[0]
    for k in range(1,len(parts),2):
        name,body=parts[k],parts[k+1]
        out+=[f'### {name}','','| čeština | русский |','|---|---|']
        for m in pat.finditer(body):
            cs,ru=m.group(1).replace('|','\\|'),m.group(2).replace('|','\\|')
            if 'lessonN' in cs: continue
            out.append(f'| {cs} | {ru} |')
        out.append('')
g=open(D+'LearnScreens.kt').read()
out+=['## Slovníček / Словарик','','| čeština | русский |','|---|---|']
for m in pat.finditer(g[g.index('val GLOSSARY'):]):
    out.append(f'| {m.group(1)} | {m.group(2)} |')
open('docs/zaklady-texty.md','w').write('\n'.join(out)+'\n')
print(len(out),'lines')
