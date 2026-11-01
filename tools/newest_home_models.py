"""Recover declarative Home model geometry without loading supplied bytecode.

Input is CFR's static output in build/owner-oct29-reference. Preserve local
pivots, arbitrary rotations, native box UVs, mirror flags and inflation.
"""
import hashlib
import json
import re
from pathlib import Path
from recover_wildlife_models import geometry

ROOT = Path(__file__).resolve().parents[1]
REF = ROOT / 'build/owner-oct29-reference/net/luckystudio/cozyhome'
ORIGINAL = ROOT / 'art/owner-library/originals/Edited-2026-10-29/home/net/luckystudio/cozyhome'
OUT = ROOT / 'src/main/resources/assets/jugcraft/newest_home_models'


def generate():
    records = []
    for source in sorted(REF.rglob('*Model.java')):
        text = source.read_text(encoding='utf-8')
        if 'public static LayerDefinition getTexturedModelData()' not in text: continue
        text = text.replace('getTexturedModelData()', 'createBodyLayer()')
        # A few exported parts aren't retained in Java local variables. Give the
        # parser a synthetic variable without altering their actual bone names.
        text = re.sub(r'(?m)^(\s*)(\w+)\.addOrReplaceChild\("([^"\n]+)"',
                      lambda m: m[1] + 'PartDefinition recovered_' + re.sub(r'\W', '_', m[3]) + ' = ' + m[2] + '.addOrReplaceChild("' + m[3] + '"', text)
        data = geometry(text)
        name = source.stem
        target = OUT / (name + '.json')
        target.parent.mkdir(parents=True, exist_ok=True)
        value = json.dumps(data, indent=2) + '\n'
        if not target.exists() or target.read_text() != value: target.write_text(value, encoding='utf-8')
        relative = source.relative_to(REF).with_suffix('.class')
        records.append({'source': 'home/net/luckystudio/cozyhome/' + relative.as_posix(),
                        'sha256': hashlib.sha256((ORIGINAL / relative).read_bytes()).hexdigest(),
                        'runtime': target.relative_to(ROOT / 'src/main/resources').as_posix(),
                        'parts': len(data['parts']), 'cubes': sum(len(p['cubes']) for p in data['parts'])})
    report = ROOT / 'art/owner-library/catalog/edited-2026-10-29/home-model-recovery.json'
    report.write_text(json.dumps({'sourceExecuted': False, 'method': 'Strict static declarative geometry parser',
                                 'models': records, 'runtimeBinding': 'Native OwnerHomeRenderer uses nine supplied rigs; OminousChairModel and SeatEntityModel retained as unused source geometry. Runtime validation pending.'}, indent=2) + '\n')
    print('Recovered supplied Home model geometry:', len(records))


if __name__ == '__main__': generate()
