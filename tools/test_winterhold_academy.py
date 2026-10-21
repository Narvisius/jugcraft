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

    def test_doors_are_complete_and_snow_has_full_support(self):
        doors = []
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
            if s["id"] == "minecraft:snow":
                self.assertIn(self.block((x, y-1, z)), {
                    "minecraft:stone_bricks", "minecraft:chiseled_stone_bricks", "minecraft:polished_andesite"})
                self.assertEqual(s["properties"]["layers"], "1")
        self.assertEqual(len(doors), 8)  # six bedrooms, archive, secret lab

    def test_circular_enclosure_and_separate_library(self):
        # The footprint must not regress to three detached towers on a rectangle.
        for x, z in ((38, 85), (98, 85), (68, 115), (68, 56)):
            self.assertNotEqual(self.block((x, 32, z)), "minecraft:air")
        self.assertEqual(self.block((32, 24, 121)), "minecraft:air")
        self.assertEqual(self.block((104, 24, 121)), "minecraft:air")
        self.assertEqual(self.block((68, 31, 85)), "minecraft:air")  # open-sky court
        self.assertNotEqual(self.block((112, 36, 34)), "minecraft:air")  # low library roof
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


if __name__ == "__main__":
    unittest.main()
