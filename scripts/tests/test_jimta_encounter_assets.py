"""Original asset fidelity; authored now, run with the final combined suite."""
from pathlib import Path
import hashlib
import json
import unittest

ROOT=Path(__file__).resolve().parents[2]
class EncounterAssets(unittest.TestCase):
    def test_original_meshes_clips_and_skins_are_exact(self):
        source=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/assets/irons_spellbooks'
        target=ROOT/'src/main/resources/assets/jugcraft'
        pairs=[('geo/ice_spider.geo.json','geckolib/models/entity/jimtafacts/ice_spider.geo.json'),('geo/citadel_keeper.geo.json','geckolib/models/entity/jimtafacts/citadel_keeper.geo.json'),('animations/ice_spider.animation.json','geckolib/animations/entity/jimtafacts/ice_spider.animation.json'),('animations/casting_animations.json','geckolib/animations/entity/jimtafacts/citadel_keeper.animation.json')]
        for path in ('entity/ice_spider/ice_spider','entity/keeper/keeper','entity/keeper/keeper_restored','entity/keeper/keeper_ghost','entity/ice_spike','entity/ice_tomb/ice_tomb','entity/ice_tomb/ice_tomb_cull'):
            pairs.append((f'textures/{path}.png',f'textures/jimtafacts/spellbooks/{path}.png'))
        for before,after in pairs:
            if before.endswith('.json'):
                # Git normalizes runtime text to LF; the archived source keeps its original CRLF.
                self.assertEqual(json.loads((source/before).read_text(encoding='utf-8')),json.loads((target/after).read_text(encoding='utf-8')),before)
            else:
                self.assertEqual(hashlib.sha256((source/before).read_bytes()).digest(),hashlib.sha256((target/after).read_bytes()).digest(),before)
