# Generate LittleAnt AI API  to docs/

#!/usr/bin/env python3
import json,re
from pathlib import Path
src=Path('src/main/java/net/tianyang928/littleant/entity/ai/brain/ModuleRegistry.java').read_text()
pat=re.compile(r'add\("([^"]+)",\s*"([^"]+)",\s*BlockShape\.([A-Z_]+),\s*(.*?),\s*List\.of\(', re.S)
mods=[]
inp=re.compile(r'new InputDefinition\("([^"]+)",\s*ValueType\.([A-Z]+),\s*"([^"]*)"(?:,\s*(true|false))?\)')
for m in pat.finditer(src):
    raw=m.group(4)
    inputs=[{'name':x.group(1),'type':x.group(2),'default':x.group(3),'required':x.group(4)=='true'} for x in inp.finditer(raw)]
    if not inputs:
        if 'blockPos(' in raw: inputs=[{'name':n,'type':'NUMBER','default':'0','required':True} for n in ('x','y','z')]
        elif 'numbers(' in raw: inputs=[{'name':n,'type':'NUMBER','default':'0','required':True} for n in ('a','b')]
        elif 'craftItem(' in raw: inputs=[{'name':'amount','type':'NUMBER','default':'1','required':True},{'name':'recipe_slots','type':'TEXT','default':'minecraft:air','required':True}]
    mods.append({'opcode':m.group(1),'category':m.group(2),'shape':m.group(3),'inputs':inputs})
Path('docs').mkdir(exist_ok=True)
Path('docs/ai-api.json').write_text(json.dumps({'version':1,'dsl':'LittleAnt DSL (Python-like, not full Python)','modules':mods},ensure_ascii=False,indent=2)+'\n')
lines=['# LittleAnt AI API','','Generated from `ModuleRegistry` (the single source of truth). The DSL is restricted and is not a Python interpreter.','','## Example','','```python','@tick_start','def main():','    say("hello")','```','','## Modules','']
for x in mods:
    lines += [f"### `{x['opcode']}`",'',f"Category: `{x['category']}` -- Shape: `{x['shape']}`"]
    if x['inputs']:
        lines.append('\nParameters:')
        for i in x['inputs']:
            req='; required' if i['required'] else ''
            lines.append(f"- `{i['name']}` (`{i['type']}`), default `{i['default']}`{req}")
    else: lines.append('\nParameters: none')
    lines.append('')
Path('docs/ai-api.md').write_text('\n'.join(lines)+'\n')
print(f'Generated {len(mods)} module entries')
