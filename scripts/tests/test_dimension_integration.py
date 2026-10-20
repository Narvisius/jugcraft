"""Merged dimension placements must leave every published biome reachable."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import biomes
import end_noise


class DimensionIntegrationTests(unittest.TestCase):
    def test_combined_end_picker_preserves_requested_shares(self):
        entries = biomes.dimension_file()["end"] + biomes.owner_dimension_file()["end"]
        highlands = [entry for entry in entries if entry["zone"] == "highlands"]
        self.assertEqual(len(highlands), 8)
        self.assertEqual(len({entry["biome"] for entry in entries}), len(entries))
        total = 1 + sum(entry["weight"] for entry in highlands)
        share = 1 - sum(entry["share"] for entry in highlands)
        self.assertAlmostEqual(share, .25)
        cumulative = 1
        self.assertAlmostEqual(cumulative / total, end_noise.quantile(share), delta=.00005)
        for entry in highlands:
            share += entry["share"]
            cumulative += entry["weight"]
            self.assertAlmostEqual(cumulative / total, end_noise.quantile(share), delta=.00005)

    def test_magma_and_inferno_each_win_at_their_climate_center(self):
        entries = biomes.dimension_file()["nether"] + biomes.owner_dimension_file()["nether"]
        for name in ("jugcraft:magma_fields", "jugcraft:landscape_erupting_inferno"):
            center = next(entry for entry in entries if entry["biome"] == name)
            winner = min(entries, key=lambda entry: (entry["temperature"] - center["temperature"]) ** 2
                         + (entry["humidity"] - center["humidity"]) ** 2 + entry["offset"] ** 2)
            self.assertEqual(winner["biome"], name)

    def test_runtime_files_match_combined_layout(self):
        for filename, expected in (("dimension_biomes.json", biomes.dimension_file()),
                                   ("menagerie/landscape-dimensions.json", biomes.owner_dimension_file())):
            self.assertEqual(json.loads((ROOT / "src/main/resources/jugcraft" / filename).read_text()), expected)
