"""Retain authored collection translations and wearable tags after shared generation.
The language snapshot contains only shipped dcf6b249 keys missing from generation;
new generator-owned values take precedence. No source programs are executed.
"""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'

def apply(writer=None):
    def write(path, data):
        if writer: writer(path, data)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    path = RES/'assets/jugcraft/lang/en_us.json'
    data = json.loads(path.read_text(encoding='utf-8'))
    preserved = json.loads((ROOT/'tools/data/authored-language-preserved.json').read_text(encoding='utf-8'))
    for key, value in preserved.items(): data.setdefault(key, value)
    write(path, data)
    from newest_ars_additions import CHARMS
    slots = {
        'legs/belt': ['jugcraft:jugtania_'+n for n in ('travel_belt','super_travel_belt','speed_up_belt','knockback_belt')],
        'legs/charm': ['jugcraft:ars_additions_'+n for n in CHARMS+['warp_index','stabilized_warp_index']] + ['jugcraft:jugtania_goddess_charm','jugcraft:jugtania_diva_charm']
    }
    for slot, additions in slots.items():
        path = RES/'data/trinkets/tags/item'/(slot+'.json')
        data = json.loads(path.read_text(encoding='utf-8'))
        data['values'] = sorted(set(data['values']) | set(additions))
        write(path, data)
    write(RES/'data/c/tags/item/nuggets/gold.json', {'replace':False,'values':['minecraft:gold_nugget']})

if __name__ == '__main__': apply()
