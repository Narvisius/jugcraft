import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'tools'))
from jimtafacts_survival_rewards import REWARDS, PREFIX

class SurvivalRewardsTests(unittest.TestCase):
    def test_reward_tables_keep_sources_and_have_single_new_pool(self):
        for rel, rewards in REWARDS.items():
            source = ROOT/'art/owner-library/originals/Oct23/Jimtafacts/data/irons_spellbooks/loot_table'/rel
            actual = json.loads((ROOT/'src/main/resources/data/jugcraft/loot_table/jimtafacts/spellbooks'/rel).read_text(encoding='utf-8'))
            old = json.loads(source.read_text(encoding='utf-8'))
            self.assertEqual(len(old['pools'])+len(rewards), len(actual['pools']))
            for item, chance in rewards:
                pools=[p for p in actual['pools'] if any(e.get('name')==PREFIX+item for e in p['entries'])]
                self.assertEqual(1,len(pools),item)
                self.assertEqual(1,pools[0]['rolls'])

    def test_chest_rewards_are_referenced_by_actual_structure_templates(self):
        templates=list((ROOT/'src/main/resources/data/jugcraft/structure/jimtafacts/spellbooks').rglob('*.nbt'))
        import gzip
        payload=b''.join(gzip.decompress(p.read_bytes()) for p in templates)
        tables=ROOT/'src/main/resources/data/jugcraft/loot_table/jimtafacts/spellbooks'
        reachable={p.relative_to(tables).as_posix()[:-5] for p in tables.rglob('*.json')
                   if ('jugcraft:jimtafacts/spellbooks/'+p.relative_to(tables).as_posix()[:-5]).encode() in payload}
        def refs(value):
            if isinstance(value,dict):
                if value.get('type')=='minecraft:loot_table':yield value.get('value','').removeprefix('jugcraft:jimtafacts/spellbooks/')
                for child in value.values():yield from refs(child)
            elif isinstance(value,list):
                for child in value:yield from refs(child)
        for _ in range(16):
            children={r for path in reachable if (tables/(path+'.json')).exists() for r in refs(json.loads((tables/(path+'.json')).read_text(encoding='utf-8')))}
            if children <= reachable:break
            reachable |= children
        for route in REWARDS:
            if route.startswith('chests/'):self.assertIn(route[:-5],reachable,route)

    def test_aircraft_preserves_logical_uv_coordinates(self):
        from owner_aircraft import ASSETS
        model=json.loads((ASSETS/'objects/biplane.bbmodel').read_text(encoding='utf-8'))
        actual=json.loads((ROOT/'src/main/resources/assets/jugcraft/aircraft_models.json').read_text(encoding='utf-8'))['biplane']
        def faces(nodes):
            for n in nodes:
                yield from n['quads']
                yield from faces(n['children'])
        # The supplied plane occupies the full logical atlas; the prior /128 mapping
        # incorrectly squeezed every UV into its top-left eighth.
        self.assertEqual(16,model['textures'][0]['uv_width'])
        self.assertGreater(max(v[3] for q in faces(actual) for v in q['vertices']), .75)
        self.assertLessEqual(max(v[3] for q in faces(actual) for v in q['vertices']), 1.001)

if __name__=='__main__': unittest.main()
