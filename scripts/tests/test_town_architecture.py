"""Town geometry regressions: weather cover, player access and saved anchors."""
from collections import deque
from pathlib import Path
import base64
import gzip
import json
import random
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import town
import town_buildings as buildings
from town_voxels import S, block_name


class TownArchitectureTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.church = buildings.church(random.Random(1907))
        cls.town = town.Town().build()

    def test_church_has_weather_cover_over_every_interior_column(self):
        v = self.church.v
        # Include the formerly uncovered vestibule and both rear aisles.
        for x in range(1, 24):
            for z in range(1, 38):
                with self.subTest(x=x, z=z):
                    self.assertTrue(any(v.get(x, y, z) not in (None, S("air"))
                                        for y in range(12, 49)))

    def test_church_entry_reaches_priest_and_both_aisles(self):
        v = self.church.v
        def free(x, z):
            def passable(state):
                return state is None or block_name(state) == "air" or block_name(state).endswith(("_door", "_carpet"))
            return v.get(x, 0, z) is not None and all(passable(v.get(x, y, z)) for y in (1, 2))
        queue = deque([(12, -1)])
        seen = set(queue)
        while queue:
            x, z = queue.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                at = x + dx, z + dz
                if at not in seen and free(*at):
                    seen.add(at)
                    queue.append(at)
        for pos in ((12, 38), (3, 37), (21, 37), (3, 3), (21, 3)):
            self.assertIn(pos, seen)

    def test_rear_is_enclosed_and_chandeliers_attach_to_roof(self):
        v = self.church.v
        for x in (*range(0, 8), *range(17, 25)):
            for y in range(1, 12 + min(x, 24 - x) // 2):
                self.assertIsNotNone(v.get(x, y, 38))
        for z in (11, 19, 27, 35):
            for y in range(16, 27):
                self.assertEqual(block_name(v.get(12, y, z)), "iron_chain")
            self.assertEqual(block_name(v.get(12, 27, z)), "deepslate_tile_slab")
        for x in (1, 23):
            for y in range(10, 24):
                self.assertEqual(block_name(v.get(x, y, 28)), "iron_chain")
            self.assertEqual(block_name(v.get(x, 24, 28)), "deepslate_tile_stairs")

    def test_houses_keep_beds_and_ladder_clear_of_flues(self):
        for seed in range(24):
            for storeys in (2, 3):
                p = buildings.house(random.Random(seed), 7 + seed % 4, 7 + seed % 4, storeys=storeys)
                for floor in range(1, storeys):
                    self.assertTrue(block_name(p.v.get(1, 1 + floor * 5, p.d - 2)).endswith("_bed"))
                self.assertNotEqual(p.v.get(0, 1, p.d - 3), S("air"))
                for y in range(1, 1 + 5 * (storeys - 1)):
                    self.assertEqual(block_name(p.v.get(p.w - 2, y, p.d - 2)), "ladder")
                    self.assertIsNotNone(p.v.get(p.w - 2, y, p.d - 1))

    def test_seasonal_sites_remain_empty_and_palette_valid(self):
        self.assertEqual(town.check(self.town), [])

    def test_retired_map_and_stable_roster(self):
        self.assertEqual(self.town.map_boards, [])
        self.assertFalse(any("MAP" in line for sign in self.town.signs for line in sign["lines"]))
        self.assertEqual(len(self.town.houses), 44)
        self.assertEqual(len(self.town.spots), 48)
        self.assertEqual(sum(s["role"] == "guard" for s in self.town.spots), 12)
        previous = json.loads(gzip.decompress((ROOT / "src/main/resources/data/jugcraft/town/revisions/1.json.gz").read_bytes()))
        for old, new in zip(previous["spots"], self.town.spots):
            for key in ("pos", "role", "skin", "shop"):
                self.assertEqual(old.get(key), new.get(key), f"Resident {old['name']} keeps their identity")
        self.assertEqual(len(self.town.atms), 4)

    def test_new_defenses_preserve_revision_two_roster_and_have_clear_space(self):
        previous = json.loads(gzip.decompress((ROOT / "src/main/resources/data/jugcraft/town/revisions/2.json.gz").read_bytes()))
        self.assertEqual(previous['spots'], self.town.spots[:len(previous['spots'])])
        self.assertEqual(len(previous['sites']), len(self.town.sites))
        for old,new in zip(previous['sites'],self.town.sites):
            if old['kind']=='tower_flag':
                self.assertEqual(new['kind'],'retired');self.assertEqual(old['blocks'],new['blocks'])
            else:self.assertEqual(old,new)
        for spot in self.town.spots[len(previous['spots']):]:
            x,y,z=spot['pos']
            for dx in (-1,0,1):
                for dz in (-1,0,1):
                    for dy in range(4):
                        self.assertIn(self.town.v.get(x+dx,y+dy,z+dz),(None,S('air')),str(spot))
        bank=buildings.bank(random.Random(1))
        self.assertEqual(block_name(bank.v.get(2,1,3)), 'ender_chest')
        self.assertEqual(block_name(bank.v.get(2,2,3)), 'air')

    def test_pews_face_altar_and_priest_is_named(self):
        from town_voxels import parse
        pews = [(pos, parse(state)[1]) for pos,state in self.church.v.b.items()
                if pos[1] == 1 and block_name(state) == "spruce_stairs"]
        self.assertGreater(len(pews), 20)
        self.assertTrue(all(props['facing'] == 'north' for _,props in pews),
                        'Stair backs face north, their seats open south toward the altar')
        self.assertIn('Priest of Notch', next(s['name'] for s in self.town.spots if s['role'] == 'priest'))
        self.assertTrue(any(s['lines'][0] == 'CHURCH OF NOTCH' for s in self.town.signs))

    def test_beds_are_unique_and_reachable_from_the_square(self):
        from town_voxels import parse
        def passable(x,z):
            for y in (1,2):
                state=self.town.v.get(x,y,z)
                if state is None: continue
                name,props=parse(state)
                if name == 'minecraft:air' or name.endswith(('_carpet','_door','_fence_gate')): continue
                return False
            return True
        queue=deque([(96,94)]); seen=set(queue)
        while queue:
            x,z=queue.popleft()
            for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
                at=x+dx,z+dz
                if at not in seen and 0<=at[0]<192 and 0<=at[1]<192 and passable(*at):
                    seen.add(at);queue.append(at)
        beds=[]
        for person in self.town.spots:
            if person['role'] in ('guard','flak_guard','mecha_guard'):
                self.assertNotIn('bed', person);continue
            bed=tuple(person['bed']);beds.append(bed)
            x,y,z=bed
            self.assertEqual(y,1)
            self.assertTrue(block_name(self.town.v.get(*bed)).endswith('_bed'))
            self.assertTrue(any((x+dx,z+dz) in seen for dx,dz in ((1,0),(-1,0),(0,1),(0,-1))), str(person))
        self.assertEqual(len(set(beds)),26)

    def test_runtime_payload_matches_generator(self):
        payload = json.loads(gzip.decompress((ROOT / "src/main/resources/data/jugcraft/town/town.json.gz").read_bytes()))
        self.assertEqual(payload["spots"], self.town.spots)
        self.assertEqual(payload["sites"], self.town.sites)
        data = base64.b64decode(payload["blocks"])
        for (x, y, z), state in self.town.v.b.items():
            index = ((y - town.Y_MIN) * town.SIZE * town.SIZE + z * town.SIZE + x) * 2
            self.assertEqual(payload["palette"][int.from_bytes(data[index:index + 2], "little")], state)

    def test_bank_door_can_be_opened_by_player(self):
        p = buildings.bank(random.Random(1907))
        for x in (5, 6):
            self.assertEqual(block_name(p.v.get(x, 1, 0)), "dark_oak_door")

    def test_wayfinding_is_supported_and_does_not_cover_decor(self):
        from town_voxels import parse, STEP
        self.assertGreaterEqual(len(self.town.signs), 10)
        titles = {s["lines"][0] for s in self.town.signs}
        self.assertTrue({"GENERAL STORE", "SEASONAL STALL", "CURIOSITIES", "FLORIST"} <= titles)
        for entry in self.town.signs:
            x, y, z = entry["pos"]
            name, props = parse(self.town.v.get(x, y, z))
            self.assertEqual(name, "minecraft:spruce_wall_sign")
            dx, dz = STEP[props["facing"]]
            self.assertEqual(self.town.v.get(x-dx, y, z-dz), S("spruce_planks"))
            self.assertEqual(len(entry["lines"]), 4)
        self.assertEqual(town.check(self.town), [])


if __name__ == "__main__":
    unittest.main()
