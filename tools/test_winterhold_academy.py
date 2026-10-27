"""Offline structure contracts; Minecraft rendering/behavior is tested separately."""
import gzip
import struct
import unittest
from collections import deque

from winterhold_academy import ENTRY, OUT, ROOMS, SIZE, STATIONS, build, nbt_bytes


def read_nbt(blob):
    """Independent reader of the primitive NBT types used by the shared writer."""
    from io import BytesIO
    stream = BytesIO(gzip.decompress(blob))

    def read(fmt):
        return struct.unpack(fmt, stream.read(struct.calcsize(fmt)))[0]

    def string():
        return stream.read(read(">H")).decode("utf-8")

    def value(tag):
        if tag == 1:
            return read(">b")
        if tag == 3:
            return read(">i")
        if tag == 6:
            return read(">d")
        if tag == 8:
            return string()
        if tag == 9:
            child, count = read(">b"), read(">i")
            return [value(child) for _ in range(count)]
        if tag == 10:
            result = {}
            while (child := read(">b")):
                name = string()
                result[name] = value(child)
            return result
        raise AssertionError(f"Unexpected tag {tag}")

    assert read(">b") == 10 and string() == ""
    result = value(10)
    assert not stream.read(), "Trailing NBT data"
    return result


class AcademyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.template = build()
        cls.palette = cls.template["palette"]
        cls.cells = {tuple(b["pos"]): cls.palette[b["state"]] for b in cls.template["blocks"]}

    def block(self, pos):
        return self.cells.get(pos, {"id": "minecraft:air"})["id"]

    def test_nbt_round_trip_and_committed_output(self):
        packed = nbt_bytes(self.template)
        self.assertEqual(read_nbt(packed), self.template)
        self.assertEqual(gzip.decompress(OUT.read_bytes()), gzip.decompress(packed))
        self.assertEqual(build(), self.template)

    def test_bounds_unique_positions_and_native_schema(self):
        self.assertEqual(len(self.cells), len(self.template["blocks"]))
        self.assertLess(len(self.cells), 350_000)
        self.assertEqual(self.template["size"], list(SIZE))
        for pos in self.cells:
            self.assertTrue(all(0 <= c < n for c, n in zip(pos, SIZE)), pos)
        for s in self.palette:
            self.assertIn("id", s)
            self.assertNotIn("Name", s)
            self.assertNotIn("Properties", s)
            self.assertTrue(s["id"].startswith(("minecraft:", "jugcraft:")))

    def test_existing_stations_no_rewards_or_owners(self):
        expected = {block for _, block in STATIONS.values() if block.startswith("jugcraft:")}
        expected.add("jugcraft:academy_ward")
        expected.update("jugcraft:" + colour + "_leaf_pile" for colour in ("red", "orange", "yellow"))
        expected.add("jugcraft:ecology_maple_log")
        expected.update("jugcraft:ecology_" + colour + "_maple_leaves" for colour in ("red", "orange", "yellow"))
        # The crafted court adds only registered seasonal decoration, never a
        # new academy station, reward block or owned progression object.
        expected.update("jugcraft:" + colour + "_mum" for colour in ("red", "orange", "yellow", "purple"))
        expected.add("jugcraft:autumn_wreath")
        self.assertEqual({s["id"] for s in self.palette if s["id"].startswith("jugcraft:")}, expected)
        for name, (pos, block) in STATIONS.items():
            self.assertEqual(self.block(pos), block, name)
        self.assertEqual(self.template["entities"], [])
        for b in self.template["blocks"]:
            data = b.get("nbt", {})
            self.assertFalse({"Items", "LootTable", "owner", "Book", "book"} & data.keys())
            if data:
                self.assertEqual(data["id"], "minecraft:sign")
                self.assertEqual(len(data["front_text"]["messages"]), 4)

    def test_selective_donor_roof_and_autumn_court(self):
        # The adapted donor motif contributes only a bounded roof detail, while
        # the existing Hall, stations, court and all room routes remain intact.
        from pathlib import Path
        import json
        motif = json.loads(Path(__file__).with_name("fonta59_hall_roof_motif.json").read_text())
        self.assertLess(sum(row.count("#") for row in motif["rows"]), 150)
        self.assertEqual(self.block((68, 87, 33)), "minecraft:lantern")
        self.assertEqual(self.block((151, 25, 19)), "minecraft:bookshelf")
        self.assertEqual(self.block((64, 38, 29)), "minecraft:air")
        self.assertNotEqual(self.block((68, 38, 33)), "minecraft:air")
        self.assertEqual(self.block((52, 29, 75)), "jugcraft:ecology_maple_log")
        self.assertEqual(self.block((46, 28, 65)), "minecraft:orange_stained_glass_pane")
        self.assertIn("jugcraft:ecology_red_maple_leaves", {s["id"] for s in self.palette})
        self.assertNotIn("minecraft:snow", {s["id"] for s in self.palette})

    def test_every_room_and_station_can_be_reached(self):
        # Two-block headroom navigation with one-block steps and hand-openable doors. This catches sealed rooms,
        # overwritten stair openings and missing landings, but does not replace game collision tests.
        def free(x, y, z):
            return self.block((x, y, z)) in {"minecraft:air", "minecraft:snow", "minecraft:spruce_door"}

        def stand(pos):
            x, y, z = pos
            return 0 <= x < SIZE[0] and 1 <= y < SIZE[1]-1 and 0 <= z < SIZE[2] \
                and free(x, y, z) and free(x, y+1, z) and not free(x, y-1, z)

        self.assertTrue(stand(ENTRY))
        visited, queue = {ENTRY}, deque([ENTRY])
        while queue:
            x, y, z = queue.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                for dy in (0, 1, -1):
                    p = x+dx, y+dy, z+dz
                    if p not in visited and stand(p) and (dy <= 0 or free(x, y+2, z)):
                        visited.add(p)
                        queue.append(p)
        for name, pos in ROOMS.items():
            self.assertTrue(pos in visited, f"Unreachable room: {name} at {pos}")
        for pos in ((105, 35, 34), (112, 35, 27), (119, 35, 34)):
            self.assertTrue(pos in visited, f"Unreachable Arcanaeum gallery: {pos}")
        for name, (pos, _) in STATIONS.items():
            x, y, z = pos
            self.assertTrue(any((x+dx, y, z+dz) in visited
                                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))), name)

    def test_beds_and_signs_have_support(self):
        for (x, y, z), s in self.cells.items():
            if s["id"] == "minecraft:blue_bed":
                part = s["properties"]["part"]
                other = self.cells[x, y, z+(-1 if part == "foot" else 1)]
                self.assertEqual(other["id"], s["id"])
                self.assertNotEqual(other["properties"]["part"], part)
                self.assertNotEqual(self.block((x, y-1, z)), "minecraft:air")
            if s["id"] == "minecraft:spruce_wall_sign":
                self.assertEqual(s["properties"]["facing"], "south")
                self.assertNotEqual(self.block((x, y, z-1)), "minecraft:air")

    def test_doors_and_autumn_groundcover_have_full_support(self):
        doors = []
        piles = 0
        for (x, y, z), s in self.cells.items():
            if s["id"] == "minecraft:spruce_door":
                props = s["properties"]
                lower = props["half"] == "lower"
                other = self.cells[x, y+(1 if lower else -1), z]
                self.assertEqual(other["id"], s["id"])
                self.assertNotEqual(other["properties"]["half"], props["half"])
                self.assertEqual(other["properties"]["facing"], props["facing"])
                if lower:
                    doors.append((x, y, z))
                    self.assertNotEqual(self.block((x, y-1, z)), "minecraft:air")
            self.assertNotEqual(s["id"], "minecraft:snow")
            if s["id"].endswith("_leaf_pile"):
                piles += 1
                self.assertNotEqual(self.block((x, y-1, z)), "minecraft:air")
                self.assertEqual(s["properties"]["layers"], "1")
        self.assertEqual(len(doors), 8)  # six bedrooms, archive, secret lab
        self.assertGreaterEqual(piles, 12)
        self.assertEqual(sum(s["id"] == "minecraft:carved_pumpkin" for s in self.cells.values()), 4)

    def test_compact_court_gate_and_connected_academic_wing(self):
        # The court has a defined edge, a framed open-sky bridge entrance,
        # and an enclosed scholarly cross-wing joining Hall and library.
        for x, z in ((38, 85), (98, 85), (68, 56)):
            self.assertNotEqual(self.block((x, 32, z)), "minecraft:air")
        for z in (114, 120, 126):
            self.assertEqual(self.block((68, 25, z)), "minecraft:air")
            self.assertEqual(self.block((68, 33, z)), "minecraft:air")
            self.assertNotEqual(self.block((62, 33, z)), "minecraft:air")
            self.assertNotEqual(self.block((74, 33, z)), "minecraft:air")
        for z in (114, 126):
            self.assertEqual(self.block((68, 32, z)), "minecraft:chiseled_stone_bricks")
        for z in (25, 34, 44):
            self.assertNotEqual(self.block((90, 36, z)), "minecraft:air")
        for x in (83, 97):
            self.assertEqual(self.block((x, 27, 34)), "minecraft:air")
        self.assertEqual(self.block((32, 24, 121)), "minecraft:air")
        self.assertEqual(self.block((104, 24, 121)), "minecraft:air")
        self.assertEqual(self.block((68, 31, 85)), "minecraft:air")  # open-sky court
        self.assertEqual(self.block((112, 36, 34)), "minecraft:air")  # opened Arcanaeum vault
        self.assertNotEqual(self.block((112, 46, 34)), "minecraft:air")  # raised roof
        lecterns = [b for b in self.template["blocks"]
                    if self.palette[b["state"]]["id"] == "jugcraft:conclave_lectern"]
        self.assertEqual(len(lecterns), 1)

    def test_foundation_and_existing_conclave_have_no_fabricated_state(self):
        from winterhold_academy import FLOOR, ROCK_DEPTH
        self.assertEqual(FLOOR, 24)
        self.assertGreaterEqual(ROCK_DEPTH, 16)
        self.assertIn(self.block((68, 0, 85)), {"minecraft:andesite", "minecraft:tuff"})
        self.assertEqual(self.block(STATIONS["conclave"][0]), "jugcraft:conclave_lectern")
        self.assertFalse(any(b.get("nbt", {}).get("owner") for b in self.template["blocks"]))

    def test_memory_ward_controls_and_instructor_home(self):
        self.assertEqual(self.cells[(68, 27, 85)], {"id": "jugcraft:academy_ward",
                         "properties": {"facing": "north", "reflected": "false"}})
        for i, pos in enumerate(((64, 25, 81), (72, 25, 81), (72, 25, 89), (64, 25, 89))):
            self.assertEqual(self.cells[pos], {"id": "jugcraft:academy_rune", "properties": {"rune": str(i)}})
        self.assertEqual(self.block((59, 25, 31)), "minecraft:air")
        self.assertEqual(self.block((59, 26, 31)), "minecraft:air")
        self.assertNotEqual(self.block((59, 24, 31)), "minecraft:air")

    def test_library_has_connected_archives_and_masonry_enclosure(self):
        # Navigation above independently reaches each archive/alcove. Also
        # protect the authored arches and windowless side suites from regressions.
        for x in (104, 120):
            for z in (29, 39):
                for y in range(25, 29):
                    self.assertEqual(self.block((x, y, z)), 'minecraft:air')
            self.assertEqual(self.block((x, 27, 34)), 'minecraft:quartz_pillar')
        shelves = sum(s['id'] == 'minecraft:bookshelf'
                      for (x, y, z), s in self.cells.items()
                      if 98 <= x <= 126 and 25 <= y <= 29 and 22 <= z <= 46)
        self.assertGreaterEqual(shelves, 96)
        self.assertFalse(any('glass' in s['id'] for (x, y, z), s in self.cells.items()
                             if 94 <= x <= 130 and 25 <= y <= 35 and 16 <= z <= 52))
        self.assertEqual(self.block((112, 24, 34)), 'minecraft:polished_diorite')

    def test_grand_reading_wing_has_a_real_upper_route(self):
        self.assertEqual(SIZE, (176, 88, 161))
        self.assertEqual(self.block((134, 27, 34)), 'minecraft:air')
        self.assertEqual(self.block((133, 44, 34)), 'minecraft:stone_bricks')
        self.assertEqual(self.block((150, 24, 34)), 'minecraft:dark_oak_planks')
        self.assertEqual(self.block((148, 36, 41)), 'minecraft:stone_brick_stairs')
        self.assertEqual(self.block((153, 36, 42)), 'minecraft:polished_diorite')
        self.assertEqual(self.block((147, 44, 34)), 'minecraft:stone_bricks')
        self.assertEqual(self.block((169, 30, 34)), 'minecraft:light_blue_stained_glass_pane')
        self.assertEqual(self.block((140, 26, 30)), 'minecraft:lantern')

    def test_teaching_hall_and_private_suite_have_distinct_furnishings(self):
        self.assertEqual(self.block((68, 24, 25)), 'minecraft:amethyst_block')
        self.assertEqual(self.block((68, 24, 33)), 'minecraft:amethyst_block')
        self.assertEqual(self.block((68, 26, 29)), 'minecraft:soul_lantern')
        self.assertEqual(self.block((56, 25, 35)), 'minecraft:spruce_stairs')
        self.assertEqual(self.block((43, 50, 33)), 'minecraft:red_terracotta')
        self.assertEqual(self.block((45, 51, 32)), 'minecraft:spruce_stairs')
        self.assertEqual(self.block((75, 65, 37)), 'minecraft:orange_stained_glass')
        self.assertEqual(self.block((107, 34, 34)), 'minecraft:polished_tuff_stairs')
        self.assertEqual(self.block((35, 52, 35)), 'minecraft:lantern')
        self.assertEqual(self.block((62, 36, 116)), 'minecraft:lantern')

    def test_hall_vault_and_teaching_zones_preserve_working_layout(self):
        self.assertEqual(self.block((68, 48, 24)), 'minecraft:chiseled_stone_bricks')
        self.assertEqual(self.block((68, 48, 42)), 'minecraft:chiseled_stone_bricks')
        self.assertEqual(self.block((66, 47, 26)), 'minecraft:lantern')
        self.assertEqual(self.block((60, 26, 31)), 'minecraft:cartography_table')
        self.assertEqual(self.block((76, 26, 31)), 'minecraft:smithing_table')
        self.assertEqual(self.block((65, 27, 21)), 'minecraft:lectern')
        self.assertEqual(self.block((71, 27, 21)), 'minecraft:lectern')
        self.assertEqual(self.block((68, 26, 20)), 'minecraft:enchanting_table')
        self.assertEqual(self.block((68, 39, 33)), 'minecraft:air')
        self.assertEqual(self.block((68, 38, 23)), 'minecraft:air')
        self.assertEqual(self.block((68, 38, 43)), 'minecraft:air')
        self.assertNotEqual(self.block((68, 38, 21)), 'minecraft:air')
        self.assertNotEqual(self.block((68, 38, 45)), 'minecraft:air')
        self.assertEqual(self.block((68, 34, 33)), 'minecraft:amethyst_block')
        self.assertEqual(self.block((68, 38, 27)), 'minecraft:chiseled_stone_bricks')
        self.assertEqual(self.block((65, 35, 33)), 'minecraft:iron_bars')
        self.assertEqual(self.block((68, 49, 52)), 'minecraft:amethyst_block')
        self.assertEqual(self.block((68, 26, 30)), 'minecraft:lectern')
        self.assertEqual(self.block((65, 26, 29)), 'minecraft:amethyst_block')
        self.assertEqual(self.block((71, 26, 29)), 'minecraft:waxed_cut_copper')
        self.assertEqual(self.block((68, 25, 33)), 'minecraft:air')
        for pos, block in STATIONS.values():
            self.assertEqual(self.block(pos), block)


if __name__ == "__main__":
    unittest.main()
