"""Source fidelity checks for the final combined validation; no source programs execute."""
from pathlib import Path
import hashlib
import json
import sys
import unittest

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from nouveau_nbt import read

class WardAssetTests(unittest.TestCase):
    def test_original_geometry_animation_and_textures(self):
        source=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/assets/irons_spellbooks'
        resources=ROOT/'src/main/resources/assets/jugcraft'
        pairs=[('geo/scapegoat.geo.json','geckolib/models/entity/jimtafacts/scapegoat.geo.json'),('animations/scapegoat_animations.animation.json','geckolib/animations/entity/jimtafacts/scapegoat.animation.json')]
        for name in ('entity/scapegoat','entity/ender_chain','entity/shield/shield_overlay','entity/shield/shield_trim','entity/portal/portal_round','entity/portal/pocket_dimension_portal_round'):
            pairs.append((f'textures/{name}.png',f'textures/jimtafacts/spellbooks/{name}.png'))
        for old,new in pairs:
            if old.endswith('.json'):
                # Preserve all geometry/animation data while allowing Git's runtime LF normalization.
                self.assertEqual(json.loads((source/old).read_text(encoding='utf-8')),json.loads((resources/new).read_text(encoding='utf-8')),old)
            else:
                self.assertEqual(hashlib.sha256((source/old).read_bytes()).digest(),hashlib.sha256((resources/new).read_bytes()).digest(),old)

    def test_original_room_layout_preserved_and_palette_registered(self):
        source=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/data/irons_spellbooks/structure/pocket_room.nbt'
        native=ROOT/'src/main/resources/data/jugcraft/structure/jimtafacts/pocket_room.nbt'
        a=read(source.read_bytes())[2];b=read(native.read_bytes())[2]
        self.assertEqual(a['size'],b['size']);self.assertEqual(len(a['blocks'][1][1]),len(b['blocks'][1][1]))
        for before,after in zip(a['blocks'][1][1],b['blocks'][1][1]):
            self.assertEqual(before['pos'],after['pos']);self.assertEqual(before['state'],after['state'])
        allowed={'minecraft:barrier','minecraft:air','jugcraft:jimta_spellbooks_voidstone','jugcraft:jimta_spellbooks_pocket_dimension_portal_frame'}
        for entry in b['palette'][1][1]:self.assertIn(entry['id'][1],allowed)

    def test_repayment_cannot_be_absorbed_again(self):
        for name in ('bypasses_armor','bypasses_effects','bypasses_invulnerability','bypasses_enchantments','bypasses_resistance','bypasses_cooldown'):
            data=json.loads((ROOT/f'src/main/resources/data/minecraft/tags/damage_type/{name}.json').read_text())
            self.assertIn('jugcraft:jimtafacts_heartstop',data['values'])

if __name__=='__main__':unittest.main()
