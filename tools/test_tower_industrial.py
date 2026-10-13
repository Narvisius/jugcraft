"""Targeted geometry/save-layout invariants for the industrial tower generator."""
import collections
import gzip
import hashlib
import math
import tempfile
import unittest
from pathlib import Path

import drone_tower as legacy
import tower_exchanges
import tower_industrial as industrial


class IndustrialTowerGeometryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.legacy_path = industrial.RESOURCES / "data/jugcraft/drone_tower/tower.json.gz"
        cls.legacy_hash = hashlib.sha256(cls.legacy_path.read_bytes()).hexdigest()
        cls.v2_path = cls.legacy_path.with_name("tower_industrial_v2.json.gz")
        cls.v2_hash = hashlib.sha256(cls.v2_path.read_bytes()).hexdigest()
        cls.v3_path = cls.legacy_path.with_name("tower_industrial_v3.json.gz")
        cls.v3_hash = hashlib.sha256(cls.v3_path.read_bytes()).hexdigest()
        cls.v3_gui = industrial.RESOURCES / "assets/jugcraft/textures/gui/tower_industrial_v3"
        cls.v3_gui_hash = hashlib.sha256(b"".join((cls.v3_gui / f"tier_{i}.png").read_bytes() for i in range(10))).hexdigest()
        cls.v4_path = cls.legacy_path.with_name("tower_industrial_v4.json.gz")
        cls.v4_hash = hashlib.sha256(cls.v4_path.read_bytes()).hexdigest()
        cls.v4_gui = industrial.RESOURCES / "assets/jugcraft/textures/gui/tower_industrial_v4"
        cls.v4_gui_hash = hashlib.sha256(b"".join((cls.v4_gui / f"tier_{i}.png").read_bytes() for i in range(10))).hexdigest()
        cls.v5_path = cls.legacy_path.with_name("tower_industrial_v5.json.gz")
        cls.v5_hash = hashlib.sha256(cls.v5_path.read_bytes()).hexdigest()
        cls.v5_gui = industrial.RESOURCES / "assets/jugcraft/textures/gui/tower_industrial_v5"
        cls.v5_gui_hash = hashlib.sha256(b"".join((cls.v5_gui / f"tier_{i}.png").read_bytes() for i in range(10))).hexdigest()
        cls.v6_path = cls.legacy_path.with_name("tower_industrial_v6.json.gz")
        cls.v6_hash = hashlib.sha256(cls.v6_path.read_bytes()).hexdigest()
        cls.v6_gui = industrial.RESOURCES / "assets/jugcraft/textures/gui/tower_industrial_v6"
        cls.v6_gui_hash = hashlib.sha256(b"".join((cls.v6_gui / f"tier_{i}.png").read_bytes() for i in range(10))).hexdigest()
        cls.blocks = {tier: industrial.build(tier) for tier in range(1, 10)}

    @staticmethod
    def minimum_light(x, y, z):
        # Outdoor ground needs positive block light; the occupied command room
        # and every higher floor keep their useful task-light standard.
        inside_room = abs(x - industrial.C) <= 7 and -6 <= z - industrial.C <= 8
        return 5 if y > 0 or inside_room else 1

    def test_every_tier_retains_complete_real_exchange(self):
        expected = {}
        tower_exchanges.build(expected, large=True)
        self.assertEqual(270, sum(industrial.base(m) == "energy_exchange" for m in expected.values()))
        self.assertEqual(270, sum(industrial.base(m) == "cargo_exchange" for m in expected.values()))
        self.assertTrue(all("large=true" in m for m in expected.values()))
        for tier, blocks in self.blocks.items():
            for p, material in expected.items():
                with self.subTest(tier=tier, part=material):
                    self.assertEqual(material, blocks.get(p), "architecture/flight carving removed a formed machine part")

    def test_tower_stays_tall_with_constructed_eighty_one_block_apron(self):
        blocks = self.blocks[9]
        self.assertEqual(214, max(p[1] for p in blocks))
        self.assertEqual(-1, min(p[1] for p in blocks))
        for axis in (0, 2):
            self.assertEqual(-40, min(p[axis] - industrial.C for p in blocks))
            self.assertEqual(40, max(p[axis] - industrial.C for p in blocks))
        self.assertNotIn((industrial.C, 0, industrial.C), blocks, "must never replace the live core")

    def test_large_inputs_have_full_dimensions_and_clear_supported_pad_approaches(self):
        expected = {}
        tower_exchanges.build(expected, large=True)
        for machine in ("energy_exchange", "cargo_exchange"):
            cells = [p for p, m in expected.items() if industrial.base(m) == machine]
            # Both fronts face east/west: six deep on X, five tall on Y, nine wide on Z.
            self.assertEqual((6, 5, 9), tuple(max(p[a] for p in cells) - min(p[a] for p in cells) + 1 for a in range(3)))
        for tier, blocks in self.blocks.items():
            for pad in industrial.ground_pads():
                for dx in range(5):
                    for dz in range(5):
                        p = (industrial.C + pad["x"] + dx, 1, industrial.C + pad["z"] + dz)
                        self.assertNotIn(p, expected, (tier, "large intake overlaps a landing plate", p))
                if abs(pad["x"]) < 23:
                    continue
                sign = 1 if pad["x"] > 0 else -1
                for distance in range(29, 33):
                    for dz in range(math.floor(pad["ez"]) - 1, math.floor(pad["ez"]) + 2):
                        for y in (2, 3, 4):
                            self.assertNotIn((industrial.C + sign * distance, y, industrial.C + dz), blocks,
                                             (tier, "outer pad approach is blocked", pad, distance, dz, y))

    def test_frozen_v4_retains_original_station_and_ground_pad_layout(self):
        import json
        self.assertEqual("8b6f8c0f6c68a0a644be63fa16fa56f2891f3da8c5a41ccd0fe8b33e475504a4", self.v4_hash)
        with gzip.open(self.v4_path, "rt") as stream:
            data = json.load(stream)
        self.assertEqual(4, data["layout_version"])
        self.assertFalse(any("large=true" in m for m in data["palette"]))
        self.assertNotEqual(data["pads"], industrial.ground_pads())

    def test_functional_machine_anchors_and_hangar_plates_do_not_move(self):
        old_preview = legacy.PREVIEW
        legacy.PREVIEW = False
        try:
            for tier, blocks in self.blocks.items():
                original = legacy.build_raw(tier)
                for p, material in original.items():
                    if industrial.base(material) in {"landing_platform", "hangar_pad", "supply_pickup", "cargo_packager",
                                                      "drone_depot_terminal", "holo_table"}:
                        self.assertEqual(material, blocks.get(p), (tier, p, material))
        finally:
            legacy.PREVIEW = old_preview

    def test_functional_field_stays_connected_to_eight_pads_and_ground_pickup(self):
        for tier, blocks in self.blocks.items():
            platform = {(x, z) for (x, y, z), m in blocks.items() if y == 0 and industrial.base(m) == "landing_platform"}
            start = next(iter(platform))
            seen, pending = {start}, [start]
            while pending:
                x, z = pending.pop()
                for p in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)):
                    if p in platform and p not in seen:
                        seen.add(p)
                        pending.append(p)
            self.assertEqual(platform, seen, (tier, "functional landing base is disconnected"))
            for pad in industrial.ground_pads():
                for dz in range(5):
                    for dx in range(5):
                        self.assertIn((industrial.C + pad["x"] + dx, industrial.C + pad["z"] + dz), seen)
            for x in range(29, 32):
                for z in range(29, 32):
                    self.assertIn((x, z), seen, (tier, "cargo pickup not reachable by depot platform scan"))
            self.assertLess(len(seen), 4096)

    def test_both_foundations_preview_the_current_layout(self):
        scratch = industrial.ROOT / "build/tower-tests"
        scratch.mkdir(parents=True, exist_ok=True)
        self.assertTrue(scratch.resolve().is_relative_to(industrial.ROOT.resolve()))
        with tempfile.TemporaryDirectory(dir=scratch) as tmp:
            target = Path(tmp) / "drone_tower_industrial_foundation.json.gz"
            industrial.blueprint_stages(target)
            with gzip.open(target, "rt") as f:
                current = __import__("json").load(f)
            with gzip.open(target.with_name("drone_tower_foundation.json.gz"), "rt") as f:
                self.assertEqual(current, __import__("json").load(f))

    def test_new_geometry_never_fills_a_hangar_or_its_door_mouth(self):
        for tier, blocks in self.blocks.items():
            for dock in industrial._METADATA[tier]["docks"]:
                x0, z0, x1, z1, y0, h, ox, oz = dock["door"]
                for depth in range(3):
                    for x in range(x0, x1 + 1):
                        for z in range(z0, z1 + 1):
                            for y in range(y0, y0 + h):
                                self.assertNotIn((x + ox * depth, y, z + oz * depth), blocks,
                                                 (tier, dock["tier"], dock["door"], depth))

    def test_export_preserves_saved_navigation_metadata_and_legacy_resource_bytes(self):
        scratch = industrial.ROOT / "build/tower-tests"
        scratch.mkdir(parents=True, exist_ok=True)
        self.assertTrue(scratch.resolve().is_relative_to(industrial.ROOT.resolve()))
        with tempfile.TemporaryDirectory(dir=scratch) as tmp:
            out = Path(tmp) / "industrial.json.gz"
            data = industrial.export(out)
            with gzip.open(out, "rt", encoding="utf-8") as f:
                self.assertEqual(data, __import__("json").load(f))
            # Identical metadata is necessary to retain saved flights and fleet slots.
            old = industrial.legacy_data()
            for key in ("terminal", "plinth"):
                self.assertEqual(old[key], data[key])
            self.assertEqual(industrial.ground_pads(), data["pads"])
            self.assertEqual(8, len(data["pads"]))
            for before, after in zip(old["pads"], data["pads"]):
                self.assertEqual(before["ez"], after["ez"])
                self.assertEqual((31.5 if before["ex"] > 0 else -31.5) if abs(after["x"]) >= 23 else before["ex"], after["ex"])
                self.assertEqual(industrial.PAD_SHIFT + ((11 if before["x"] > 0 else 12) if abs(after["x"]) >= 23 else 0),
                                 abs(before["x"] - after["x"]) + abs(before["z"] - after["z"]))
            self.assertEqual(7, data["layout_version"])
            for before, after in zip(old["tiers"], data["tiers"]):
                for key in ("tier", "top", "docks", "pickups", "capacity"):
                    self.assertEqual(before[key], after[key], (before["tier"], key))
                self.assertTrue(all(0 < count <= 1024 for count in after["modules"].values()))
            self.assertEqual(133, data["tiers"][-1]["capacity"])
            self.assertEqual(101, sum(len(t["docks"]) for t in data["tiers"]))
            self.assertEqual(self.legacy_hash, hashlib.sha256(self.legacy_path.read_bytes()).hexdigest())
            self.assertEqual(self.v2_hash, hashlib.sha256(self.v2_path.read_bytes()).hexdigest())
            self.assertEqual(self.v3_hash, hashlib.sha256(self.v3_path.read_bytes()).hexdigest())
            self.assertEqual(self.v3_gui_hash, hashlib.sha256(b"".join((self.v3_gui / f"tier_{i}.png").read_bytes() for i in range(10))).hexdigest())
            self.assertEqual(self.v4_hash, hashlib.sha256(self.v4_path.read_bytes()).hexdigest())
            self.assertEqual(self.v4_gui_hash, hashlib.sha256(b"".join((self.v4_gui / f"tier_{i}.png").read_bytes() for i in range(10))).hexdigest())
            self.assertEqual(self.v5_hash, hashlib.sha256(self.v5_path.read_bytes()).hexdigest())
            self.assertEqual(self.v5_gui_hash, hashlib.sha256(b"".join((self.v5_gui / f"tier_{i}.png").read_bytes() for i in range(10))).hexdigest())
            self.assertEqual(self.v6_hash, hashlib.sha256(self.v6_path.read_bytes()).hexdigest())
            self.assertEqual(self.v6_gui_hash, hashlib.sha256(b"".join((self.v6_gui / f"tier_{i}.png").read_bytes() for i in range(10))).hexdigest())
            # A continuous 2m surface needs the correct quadrant on every axis,
            # including negative coordinates, rather than a repeated 1m tile.
            for stage in data["tiers"]:
                for x, y, z, idx in stage["place"]:
                    value = data["palette"][idx]
                    material = industrial.base(value).removeprefix("jugcraft:")
                    if material in industrial.PHASE_IDS:
                        expected = (x & 1) | ((y & 1) << 1) | ((z & 1) << 2)
                        self.assertIn(f"phase={expected}", value, (x, y, z, value))
                    else:
                        self.assertNotIn("phase=", value, value)

    def test_new_model_detail_and_wide_apron_exist(self):
        counts = collections.Counter(industrial.base(m) for m in self.blocks[9].values())
        for material in ("tower_service_grating", "tower_service_railing", "tower_ladder", "tower_vent"):
            self.assertGreater(counts[material], 100, material)
        # Both service-road edges are constructed, not just changed in a picture.
        for dz in (-24, 24):
            for dx in range(-20, 21):
                self.assertIn((industrial.C + dx, 0, industrial.C + dz), self.blocks[1])
        self.assertEqual((142, 434), industrial.screen_image(9).size)

    def test_new_apron_is_continuous_and_its_optional_cells_never_rewrite_required_floors(self):
        import json
        scratch = industrial.ROOT / "build/tower-tests"
        scratch.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as tmp:
            data = industrial.export(Path(tmp) / "apron.json.gz")
        optional = {tuple(p) for p in data["cosmetic_apron"]}
        self.assertEqual(len(optional), len(data["cosmetic_apron"]), "mask coordinates are unique")
        self.assertGreater(len(optional), 5000, "both broad paving and its backing are part of the real build")
        self.assertEqual({-1, 0}, {p[1] for p in optional})
        self.assertEqual({(x, -1, z) for x, y, z in optional if y == 0},
                         {p for p in optional if p[1] == -1}, "each removable paving tile has its own backing")
        with gzip.open(self.v5_path, "rt", encoding="utf-8") as f:
            old = json.load(f)
        required = {tuple(p[:3]) for tier in old["tiers"] for p in tier["place"] + tier["clear"] + tier["air"]}
        self.assertFalse(optional & required, "removable paving contains no saved structural or functional cell")
        first = {tuple(p[:3]) for p in data["tiers"][0]["place"]}
        self.assertTrue(optional <= first, "the core actually builds every advertised optional tile")
        for tier in data["tiers"][1:]:
            touched = {tuple(p[:3]) for p in tier["place"] + tier["clear"] + tier["air"]}
            self.assertFalse(optional & touched, (tier["tier"], "upgrades must leave removed or customized paving alone"))
        c = industrial.C
        for tier, blocks in self.blocks.items():
            for dx in range(-40, 41):
                for dz in range(-40, 41):
                    if dx == dz == 0:
                        continue  # the live core is the sole centre cell
                    self.assertIn((c + dx, 0, c + dz), blocks, (tier, "hole in constructed apron", dx, dz))
            stripped = {p: m for p, m in blocks.items() if (p[0] - c, p[1], p[2] - c) not in optional}
            for p, m in blocks.items():
                if industrial.base(m) in {"landing_platform", "landing_pad", "supply_pickup", "cargo_packager",
                                           "energy_exchange", "cargo_exchange", "drone_depot_terminal", "holo_table"}:
                    self.assertEqual(m, stripped.get(p), (tier, "removal touched a functional cell", p))

    def test_frozen_v5_geometry_and_status_images_are_immutable(self):
        self.assertEqual("6b417d609783c831de9d1805c0a682fe98ad91bd986301e937e84eceb6f78ccc", self.v5_hash)
        self.assertEqual("79f18572684c962a7fe0727c60501cacaa9c95ce57ac2511b90fe75583acb78b", self.v5_gui_hash)

    def test_ground_light_reduction_preserves_frozen_v6_and_every_higher_fixture(self):
        import json
        import tower_lights
        self.assertEqual("0a4bca75ad8cae4727071c3914ebcd905087547a49d6394ec11a220f45ae6c78", self.v6_hash)
        self.assertEqual("45f8a2eea0e2ad896906c34a5311d6949e322cc650a375ce2b9c600638518150", self.v6_gui_hash)
        with gzip.open(self.v6_path, "rt", encoding="utf-8") as f:
            old = json.load(f)
        sources = {legacy.BLOCK_IDS.get(m, "jugcraft:" + m).split("[")[0] for m in tower_lights.EMIT}
        frozen = {}
        for stage in old["tiers"]:
            for row in stage["clear"]:
                frozen.pop(tuple(row), None)
            for x, y, z, idx in stage["place"]:
                frozen[(x, y, z)] = old["palette"][idx]
            tier = stage["tier"]
            current = {(x - industrial.C, y, z - industrial.C): industrial.state(m, (x - industrial.C, y, z - industrial.C))
                       for (x, y, z), m in self.blocks[tier].items()}
            before_lights = {p: m for p, m in frozen.items() if p[1] > 0 and m.split("[")[0] in sources}
            after_lights = {p: m for p, m in current.items() if p[1] > 0 and m.split("[")[0] in sources}
            self.assertEqual(before_lights, after_lights, (tier, "higher fixtures changed during a ground-only request"))
            old_ground = sum(p[1] == 0 and m.split("[")[0] == "jugcraft:red_light_strip" for p, m in frozen.items())
            new_ground = sum(p[1] == 0 and m.split("[")[0] == "jugcraft:red_light_strip" for p, m in current.items())
            self.assertLess(new_ground, old_ground, (tier, "bottom floor-light count did not decrease", old_ground, new_ground))
            # A reduced light replaces its ordinary floor material; it must not
            # punch holes or remodel unrelated geometry while thinning lamps.
            self.assertEqual(set(frozen), set(current), (tier, "ground-light reduction changed occupied cells"))
            for p, material in frozen.items():
                if p[1] != 0 or (material.split("[")[0] != "jugcraft:red_light_strip"
                                 and current[p].split("[")[0] != "jugcraft:red_light_strip"):
                    self.assertEqual(material, current[p], (tier, "changed unrelated geometry", p))

    def test_entrance_has_a_clear_three_metre_approach_in_every_tier(self):
        c = industrial.C
        for tier, blocks in self.blocks.items():
            for dx in (-1, 0, 1):
                for dz in range(8, 21):
                    self.assertIn((c + dx, 0, c + dz), blocks, (tier, "entrance route has no floor", dx, dz))
                    for y in range(1, 4):
                        self.assertNotIn((c + dx, y, c + dz), blocks,
                                         (tier, "pillar or fixture obstructs the entrance", dx, y, dz))
            if tier >= 2:
                self.assertEqual("girder_z", blocks[(c, 11, c + 11)], "the overhead transfer stays continuous")

    def test_required_floor_lighting_survives_removal_of_the_optional_apron(self):
        import tower_lights
        scratch = industrial.ROOT / "build/tower-tests"
        scratch.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as tmp:
            data = industrial.export(Path(tmp) / "lighting-apron.json.gz")
        optional = {tuple(p) for p in data["cosmetic_apron"]}
        previous = set(tower_lights.SEE_THROUGH)
        tower_lights.SEE_THROUGH.update(("tower_service_grating", "tower_service_railing", "tower_ladder", "tower_door_mechanism",
                                        "wall_task_light", "linear_ceiling_light", "tower_master_console"))
        try:
            for tier in (1, 2, 9):
                blocks = {p: m for p, m in self.blocks[tier].items()
                          if (p[0] - industrial.C, p[1], p[2] - industrial.C) not in optional}
                light = {}
                for pos, material in blocks.items():
                    level = tower_lights.EMIT.get(industrial.base(material), 0)
                    if level:
                        tower_lights.spread(blocks, light, pos, level)
                for x, y, z in tower_lights.surfaces(blocks):
                    if industrial.base(blocks[(x, y, z)]) in tower_lights.FLOORS:
                        self.assertGreaterEqual(light.get((x, y + 1, z), 0), self.minimum_light(x, y, z),
                                                (tier, "retained floor relies on an optional apron light", x, y, z))
        finally:
            tower_lights.SEE_THROUGH.clear()
            tower_lights.SEE_THROUGH.update(previous)

    def test_status_picture_shows_unbuilt_next_tier_as_a_translucent_ghost(self):
        image = industrial.screen_image(0)
        ghost = [(x, y) for y in range(image.height) for x in range(image.width)
                 if 0 < image.getpixel((x, y))[3] < 255]
        self.assertGreater(len(ghost), 500, "a foundation-only line hides the next tier promised by the legend")
        self.assertLess(min(y for _, y in ghost), 415, "the command post must be visible above the foundation")
        self.assertTrue(all(pixel[3] in (0, 255) for pixel in industrial.screen_image(9).get_flattened_data()),
                        "completed towers have no unbuilt stage")

    def test_hangar_machinery_has_real_guides_anchors_drive_cassette_and_threshold(self):
        counts = collections.Counter(m.split("part=")[-1].rstrip("]") for m in self.blocks[9].values()
                                     if industrial.base(m) == "tower_door_mechanism")
        for part in range(6):
            self.assertGreater(counts[str(part)], 20, f"missing physical doorway assembly part {part}")
        for dock in industrial._METADATA[9]["docks"]:
            x0, z0, x1, z1, y, h, _, _ = dock["door"]
            # Every bay, including those under an earlier bay's flight climb,
            # retains its physical drive; clearances must not silently erase it.
            self.assertIn("part=4", self.blocks[9].get((x1, y + h, z1), ""), dock)

    def test_wider_command_room_has_readable_workstations_and_clear_aisles(self):
        c = industrial.C
        blocks = self.blocks[1]
        for dx in (-4, -3, -2, 2, 3, 4):
            for y in (5, 6):
                self.assertEqual("control_screen[facing=south]", blocks[(c + dx, y, c - 6)])
        for side in (-1, 1):
            facing = "west" if side < 0 else "east"
            self.assertEqual(f"console_desk[facing={facing}]", blocks[(c + side * 6, 1, c - 2)])
            self.assertEqual(f"operator_chair[facing={facing}]", blocks[(c + side * 5, 1, c - 2)])
        self.assertEqual(2, sum(industrial.base(m) == "console_desk" for m in blocks.values()))
        master = {p: m for p, m in blocks.items() if industrial.base(m) == "tower_master_console"}
        self.assertEqual({(c + x, 1, c + z) for x in (-1, 0, 1) for z in (3, 4)}, set(master))
        self.assertEqual({f"tower_master_console[facing=south,part={part}]" for part in range(6)}, set(master.values()))
        # The entrance connects around both sides of the fixed table and to
        # each operating position; furniture cannot seal off a workstation.
        walkable = {(x, z) for x in range(c - 7, c + 8) for z in range(c - 6, c + 9)
                    if (x, 1, z) not in blocks and (x, 2, z) not in blocks}
        pending, reached = [(c, c + 8)], {(c, c + 8)}
        while pending:
            x, z = pending.pop()
            for p in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
                if p in walkable and p not in reached:
                    reached.add(p)
                    pending.append(p)
        for dx, dz in ((-5, -1), (-5, 1), (2, 5), (3, 5), (4, 5), (-2, -2), (2, -2), (-6, 0), (6, 0)):
            self.assertIn((c + dx, c + dz), reached, (dx, dz))
        # The widened eastern/western circulation space remains usable.
        for dx in (-4, 4):
            for dz in range(-5, 6):
                self.assertNotIn((c + dx, 1, c + dz), blocks)
        for (x, y, z), material in blocks.items():
            if industrial.base(material) == "equipment_rack" and abs(x - c) <= 7 and abs(z - c) <= 6:
                self.assertNotEqual("tower_vent", industrial.base(blocks.get((x, 3, z), "")))
        self.assertEqual(12, sum(industrial.base(m) == "linear_ceiling_light" for (x, y, z), m in blocks.items()
                                 if abs(x - c) <= 7 and abs(z - c) <= 6))

    def test_wall_monitors_are_two_separate_formable_three_by_two_groups(self):
        c = industrial.C
        for tier, blocks in self.blocks.items():
            panels = {p for p, m in blocks.items() if industrial.base(m) == "control_screen"}
            expected = {(c + x, y, c - 6) for x in (-4, -3, -2, 2, 3, 4) for y in (5, 6)}
            self.assertEqual(expected, panels, tier)
            groups = []
            while panels:
                start = panels.pop()
                pending, group = [start], {start}
                while pending:
                    x, y, z = pending.pop()
                    for p in ((x - 1, y, z), (x + 1, y, z), (x, y - 1, z), (x, y + 1, z)):
                        if p in panels:
                            panels.remove(p)
                            group.add(p)
                            pending.append(p)
                groups.append(group)
            self.assertEqual([6, 6], sorted(map(len, groups)), tier)
            for group in groups:
                self.assertEqual(3, len({x for x, y, z in group}))
                self.assertEqual(2, len({y for x, y, z in group}))
                self.assertTrue(all(blocks[p] == "control_screen[facing=south]" for p in group))

    def test_eight_shifted_ground_pads_have_complete_supported_plates(self):
        expected = {(industrial.C + p["x"] + dx, 1, industrial.C + p["z"] + dz)
                    for p in industrial.ground_pads() for dx in range(5) for dz in range(5)}
        for tier, blocks in self.blocks.items():
            actual = {p for p, m in blocks.items() if industrial.base(m) == "landing_pad"}
            self.assertEqual(expected, actual, tier)
            self.assertEqual(200, len(actual))
            for x, y, z in actual:
                self.assertEqual("landing_platform", blocks[(x, 0, z)])

    def test_station_working_yards_have_supported_grating_and_clear_outward_io(self):
        c = industrial.C
        for tier, blocks in self.blocks.items():
            for side in (-1, 1):
                for dx in range(23 if side < 0 else 21, 30):
                    for dz in (-1, 0, 1):
                        x, z = c + side * dx, c + dz
                        self.assertEqual("tower_service_grating", blocks[(x, 1, z)])
                        self.assertIn((x, 0, z), blocks)
                        self.assertNotIn((x, 2, z), blocks, (tier, x, z))
                        self.assertNotIn((x, 3, z), blocks, (tier, x, z))
            # No old freestanding switchgear/rack banks clutter either apron.
            for (x, y, z), material in blocks.items():
                if 21 <= abs(x - c) <= 29 and abs(z - c) <= 7 and 2 <= y <= 8:
                    self.assertIn(industrial.base(material), {"energy_exchange", "cargo_exchange"}, (tier, x, y, z, material))

    def test_closed_upper_roof_and_spire_have_no_floating_cross_arms(self):
        blocks = self.blocks[9]
        c = industrial.C
        for (x, y, z), material in blocks.items():
            if y > 190:
                self.assertNotIn(industrial.base(material), {"copper_busbar", "steel_girder", "girder_x", "girder_z"})
            if y == 186 and industrial.base(material) == "tower_service_grating":
                self.assertIn((x, 185, z), blocks)
        for dx in range(-5, 6):
            for dz in range(-5, 6):
                if abs(dx) + abs(dz) <= 8:
                    in_flight = False
                    for dock in industrial._METADATA[9]["docks"]:
                        radius = {"small": 1, "medium": 2, "large": 3}[dock["size"]]
                        in_flight |= abs(c + dx - math.floor(dock["ex"])) <= radius and abs(c + dz - math.floor(dock["ez"])) <= radius
                    if not in_flight:
                        self.assertTrue((c + dx, 185, c + dz) in blocks, (dx, 185, dz))

    def test_task_lighting_covers_editable_full_floors(self):
        import tower_lights
        previous = set(tower_lights.SEE_THROUGH)
        tower_lights.SEE_THROUGH.update(("tower_service_grating", "tower_service_railing", "tower_ladder", "tower_door_mechanism",
                                        "wall_task_light", "linear_ceiling_light", "tower_master_console"))
        try:
            for tier in (1, 2, 9):
                blocks, light = self.blocks[tier], {}
                for pos, material in blocks.items():
                    level = tower_lights.EMIT.get(industrial.base(material), 0)
                    if level:
                        tower_lights.spread(blocks, light, pos, level)
                for x, y, z in tower_lights.surfaces(blocks):
                    if industrial.base(blocks[(x, y, z)]) in tower_lights.FLOORS:
                        self.assertGreaterEqual(light.get((x, y + 1, z), 0), self.minimum_light(x, y, z), (tier, x, y, z))
        finally:
            tower_lights.SEE_THROUGH.clear()
            tower_lights.SEE_THROUGH.update(previous)

    def test_ceiling_luminaires_are_flush_supported_without_hanging_conduit_stems(self):
        c = industrial.C
        for tier, blocks in self.blocks.items():
            lights = {p for p, m in blocks.items() if industrial.base(m) in {"ceiling_light_panel", "linear_ceiling_light"} and p[1] < 12}
            self.assertEqual(12, sum(y == 9 for x, y, z in lights), tier)
            if tier >= 2:
                self.assertGreater(sum(y == 11 for x, y, z in lights), 20, tier)
            for x, y, z in lights:
                self.assertIn(y, (9, 11), (tier, x, y, z))
                self.assertIn(industrial.base(blocks.get((x, y + 1, z), "")),
                              {"steel_armor", "reinforced_concrete", "hangar_floor", "dock_floor", "light_cyan"},
                              (tier, "fixture has no solid ceiling immediately above", x, y, z))
                self.assertNotIn((x, y - 1, z), blocks, (tier, "fixture projects into occupied space", x, y, z))
            # The former command-room and apron pendants left nearly 200 ringed
            # conduit blocks hanging into this otherwise open working volume.
            # Services above the enclosed room ceiling remain concealed; the
            # open volume below either ceiling must contain no long rods.
            stems = {p for p, m in blocks.items() if industrial.base(m) == "conduit"
                     and abs(p[0] - c) <= 16 and abs(p[2] - c) <= 16
                     and 1 <= p[1] < (10 if abs(p[0] - c) <= industrial.ROOM_X + 2
                                     and abs(p[2] - c) <= industrial.ROOM_Z + 2 else 11)}
            self.assertFalse(stems, (tier, stems))

    def test_every_floor_beacon_has_a_full_height_native_bearing_beneath_its_base(self):
        from preview_tower_industrial import local_faces
        for tier, blocks in self.blocks.items():
            beacons = {p for p, m in blocks.items() if industrial.base(m) in {"light_red", "warning_light"}}
            self.assertGreaterEqual(len(beacons), 4, "keep physically mounted outdoor markers")
            for x, y, z in beacons:
                below = (x, y - 1, z)
                self.assertIn(below, blocks, (tier, "beacon floats over air", x, y, z))
                state = industrial.state(blocks[below], (x - industrial.C, y - 1, z - industrial.C))
                faces, _ = local_faces(state)
                tops = [pts for pts, uv, normal, texture in faces
                        if normal[1] > .999 and all(abs(p[1] - 1) < 1e-6 for p in pts)]
                # Test the actual exported geometry, not collision's full-cell
                # placeholder: a half-height beacon or low grating cannot bear
                # the next lamp at its cell boundary. Cover the 9px base corners.
                for fx in (3.75 / 16, .5, 12.25 / 16):
                    for fz in (3.75 / 16, .5, 12.25 / 16):
                        self.assertTrue(any(min(p[0] for p in face) <= fx <= max(p[0] for p in face)
                                            and min(p[2] for p in face) <= fz <= max(p[2] for p in face)
                                            for face in tops), (tier, "beacon base lacks native bearing", x, y, z, state))

    def test_delivered_v3_layout_and_status_images_remain_exactly_frozen(self):
        self.assertEqual("2e5670de475d32343eaf07d49d88fef5851119dd38fd46e90f7bb0fe57194de8", self.v3_hash)
        self.assertEqual("087a52d818bdc5aec4ccd1d931efd03f6ee955ae6c7215ab7b4425b98c17943b", self.v3_gui_hash)
        with gzip.open(self.v3_path, "rt") as stream:
            data = __import__("json").load(stream)
        self.assertEqual(3, data["layout_version"])
        self.assertFalse(any("tower_master_console" in state for state in data["palette"]))

    def test_wall_luminaires_keep_closed_shells_and_have_real_backing(self):
        c = industrial.C
        deltas = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}
        for tier, blocks in self.blocks.items():
            for dx in range(-9, 10):
                for dz in range(-8, 9):
                    if abs(dx) <= 7 and abs(dz) <= 6:
                        continue
                    for y in range(1, 10):
                        if dz >= 7 and abs(dx) <= 1 and y <= 3:
                            self.assertNotIn((c + dx, y, c + dz), blocks)
                        else:
                            self.assertIn(industrial.base(blocks.get((c + dx, y, c + dz), "")),
                                          {"reinforced_concrete", "acoustic_wall_panel", "blast_glass"},
                                          (tier, "wall shell contains a hole or recessed beacon", dx, y, dz))
            lamps = {p: m for p, m in blocks.items() if industrial.base(m) == "wall_task_light"}
            self.assertEqual(29, len(lamps), tier)
            for (x, y, z), material in lamps.items():
                facing = material.split("facing=")[1].split(",")[0].rstrip("]")
                dx, dz = deltas[facing]
                self.assertIn(industrial.base(blocks.get((x - dx, y, z - dz), "")),
                              {"reinforced_concrete", "acoustic_wall_panel"},
                              (tier, "wall lamp has no intact wall behind its mounting face", x, y, z))

    def test_overhead_trays_form_one_connected_route_with_matching_native_arms(self):
        c = industrial.C
        directions = ((0, -1), (1, 0), (0, 1), (-1, 0))  # N/E/S/W
        facing_turn = {"north": 0, "east": 1, "south": 2, "west": 3}
        native = {"straight": {1, 3}, "corner": {0, 3}, "tee": {0, 1, 3}, "cross": {0, 1, 2, 3}, "end": {3}}
        for tier, blocks in self.blocks.items():
            trays = {p: m for p, m in blocks.items() if industrial.base(m) == "cable_tray"
                     and p[1] == 9 and abs(p[0] - c) <= 7 and abs(p[2] - c) <= 6}
            self.assertGreater(len(trays), 40, tier)
            arms = {}
            for pos, state in trays.items():
                props = dict(p.split("=") for p in state.split("[")[1].rstrip("]").split(","))
                arms[pos] = {(arm + facing_turn[props["facing"]]) % 4 for arm in native[props["shape"]]}
                x, y, z = pos
                self.assertIn(industrial.base(blocks.get((x, y + 1, z), "")), {"steel_armor", "light_cyan"})
            pending, seen = [next(iter(trays))], set()
            while pending:
                pos = pending.pop()
                if pos in seen:
                    continue
                seen.add(pos)
                x, y, z = pos
                for arm in arms[pos]:
                    dx, dz = directions[arm]
                    other = (x + dx, y, z + dz)
                    self.assertIn(other, trays, (tier, "tray arm opens into air", pos, arm))
                    self.assertIn((arm + 2) % 4, arms[other], (tier, "tray meshes do not meet", pos, other))
                    pending.append(other)
            self.assertEqual(set(trays), seen, (tier, "disconnected overhead cable segment"))

    def test_power_socket_keeps_clear_service_volume_and_continuous_west_bearings(self):
        c = industrial.C
        for tier, blocks in self.blocks.items():
            self.assertIn("part=58", blocks[(c - 22, 2, c)])
            for dx in range(-29, -22):
                for dz in range(-1, 2):
                    self.assertEqual("tower_service_grating", blocks[(c + dx, 1, c + dz)])
                    self.assertIn((c + dx, 0, c + dz), blocks)
                    for y in range(2, 9):
                        self.assertNotIn((c + dx, y, c + dz), blocks, (tier, "power service access obstructed", dx, y, dz))
            if tier < 2:
                continue
            for u in (9, 29):
                px, pz = legacy.local(0, 0, legacy.SIZE, 3, u, 2, False)
                for x in range(px - 1, px + 2):
                    for z in range(pz - 1, pz + 2):
                        for y in range(1, 11):
                            self.assertIn(industrial.base(blocks.get((x, y, z), "")), {"reinforced_concrete", "steel_armor"},
                                          (tier, "west pier has a missing bearing cell", x, y, z))


if __name__ == "__main__":
    unittest.main()
