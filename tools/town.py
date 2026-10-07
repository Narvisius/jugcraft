"""The walled town near the start of every new world: its plan and every block, written to
src/main/resources/data/jugcraft/town/town.json.gz for the mod to build (town/TownData.java), with the decor sites
the townsfolk change with the seasons, where the townsfolk stand, the ATMs and the shops.

The town is an original design (the owner's reference pictures of walled medieval towns guided the idea only): a
curtain wall of stone with round towers and three gatehouses (east, south and west), a main street through a market
square, a church with two spires facing the square, a bank and a town hall on it, market stalls, a fountain, and
streets of half-timbered houses, shops, a tavern and a bakery.

Frame: the data covers SIZE x SIZE columns; y = 0 is the town's ground (the mod puts it at the height it levels the
town to), and the data runs from Y_MIN to Y_MIN + HEIGHT - 1. The mod fills under the ground, clears the land above
it inside the wall, and blends the ground outside the wall back to the land around over BLEND blocks.

Run `python3 tools/town.py` to write the data (tools/generate_material_data.py runs it too); add `--preview` for
pictures in build/town/.
"""
import base64
import gzip
import json
import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import numpy as np  # noqa: E402

import town_buildings as tb  # noqa: E402
import town_decor  # noqa: E402
import town_shops  # noqa: E402
import town_skins  # noqa: E402
from town_voxels import S, Voxels, connections, stair_shapes, parse, vanilla_states, check_state  # noqa: E402

SEED = 1907
SIZE = 192
C = SIZE // 2
Y_MIN = -8
HEIGHT = 64
# Wall: an irregular ring of VERTICES points on an ellipse, three of them gatehouses.
RADIUS_X, RADIUS_Z = 76, 71
VERTICES = 16
GATES = {0: "east", 4: "south", 8: "west"}
WALL_THICK = 3
WALL_TOP = 9          # the wall walk's floor; players walk at WALL_TOP + 1
TOWER_RADIUS = 4.5
TOWER_TOP = 14
BLEND = 12            # blocks outside the wall over which the ground blends back to the land
PROTECT = 8           # blocks outside the wall that are still protected
RING_LANE = (4, 7)    # the lane inside the wall, by distance from the wall's outer face

MAIN_STREET = (20, 172, 94, 98)
SOUTH_STREET = (94, 98, 112, 170)
SQUARE = (72, 120, 86, 112)
LANES = [
    (48, 50, 26, 166),    # west lane, north to south
    (142, 144, 26, 166),  # east lane
    (16, 176, 133, 135),  # south lane, west to east
    (16, 81, 64, 66),     # north-west lane
    (111, 176, 64, 66),   # north-east lane
    (58, 79, 44, 46),     # far north-west lane
    (113, 134, 44, 46),   # far north-east lane
    (73, 75, 113, 133),   # south-west alley, main street to the south lane
    (117, 119, 113, 133), # south-east alley
    (56, 136, 153, 155),  # far south lane
    (20, 47, 114, 116),   # west close
    (145, 172, 114, 116), # east close
]
FOUNTAIN_CENTRE = (88, 104)
CENTERPIECE = (101, 101, 107, 107)   # the square's seasonal centrepiece (x0, z0, x1, z1)

OUT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data" / "jugcraft" / "town" / "town.json.gz"

# Shops: who keeps them and where (see tools/town_shops.py for what they sell).
SHOPS = ["general", "seasonal", "curios", "florist"]


# ---------------------------------------------------------------- the wall's outline

def outline(rng):
    """The wall's corners: VERTICES points on an ellipse, angle 0 east and 90 south, jittered except at the gates."""
    points = []
    for i in range(VERTICES):
        a = 2 * math.pi * i / VERTICES
        jitter = 0 if i in GATES else rng.uniform(-3.0, 3.0)
        rx, rz = RADIUS_X + jitter, RADIUS_Z + jitter
        points.append((C + rx * math.cos(a), C + rz * math.sin(a)))
    return points


def signed_distance(points):
    """For every column centre, the distance to the wall's outline: negative inside."""
    xs, zs = np.meshgrid(np.arange(SIZE) + 0.5, np.arange(SIZE) + 0.5, indexing="xy")
    best = np.full(xs.shape, 1e9)
    inside = np.zeros(xs.shape, dtype=bool)
    n = len(points)
    for i in range(n):
        (x0, z0), (x1, z1) = points[i], points[(i + 1) % n]
        dx, dz = x1 - x0, z1 - z0
        t = np.clip(((xs - x0) * dx + (zs - z0) * dz) / (dx * dx + dz * dz), 0, 1)
        d = np.hypot(xs - (x0 + t * dx), zs - (z0 + t * dz))
        best = np.minimum(best, d)
        crosses = ((z0 > zs) != (z1 > zs)) & (xs < (x1 - x0) * (zs - z0) / (z1 - z0 + 1e-12) + x0)
        inside ^= crosses
    return np.where(inside, -best, best)  # indexed [z, x]


# ---------------------------------------------------------------- the town

class Town:
    def __init__(self, seed=SEED):
        self.rng = random.Random(seed)
        self.v = Voxels()
        self.sites = []
        self.spots = []
        self.atms = []
        self.points = outline(self.rng)
        self.sd = signed_distance(self.points)
        self.used = np.zeros((SIZE, SIZE), dtype=np.int8)   # [z, x]: 1 street/square, 2 building, 3 wall
        self.surface = {}   # (x, z) -> state of the top block outside the wall (paths on the blended ground)
        self.houses = []

    # ------------------------------------------------ helpers

    def sdist(self, x, z):
        if 0 <= x < SIZE and 0 <= z < SIZE:
            return float(self.sd[z, x])
        return 1e9

    def mark(self, x0, z0, x1, z1, value):
        x0, x1 = max(0, min(x0, x1)), min(SIZE - 1, max(x0, x1))
        z0, z1 = max(0, min(z0, z1)), min(SIZE - 1, max(z0, z1))
        self.used[z0:z1 + 1, x0:x1 + 1] = np.maximum(self.used[z0:z1 + 1, x0:x1 + 1], value)

    def free(self, x0, z0, x1, z1, inner=-8.0):
        """Whether every column of the box is unused and at least `inner` inside the wall."""
        if x0 < 0 or z0 < 0 or x1 >= SIZE or z1 >= SIZE:
            return False
        if self.used[z0:z1 + 1, x0:x1 + 1].any():
            return False
        return bool((self.sd[z0:z1 + 1, x0:x1 + 1] <= inner).all())

    def put(self, piece, ox, oz, turns, purpose=None, y=0):
        blocks, sites, spots, atms = piece.placed(ox, y, oz, turns)
        for pos, state in blocks.b.items():
            self.v.b[pos] = state
        self.sites += sites
        for spot in spots:
            if spot["role"] == "shopkeeper":
                if purpose in SHOPS:
                    spot["shop"] = purpose
                else:
                    spot["role"] = "baker" if piece_purpose(piece) == "bakery" else "vendor"
            self.spots.append(spot)
        self.atms += atms

    @staticmethod
    def size_after(piece, turns):
        return (piece.d, piece.w) if turns % 2 else (piece.w, piece.d)

    def place_centred(self, piece, cx, cz, turns, **kw):
        w, d = self.size_after(piece, turns)
        ox, oz = int(round(cx - (w - 1) / 2)), int(round(cz - (d - 1) / 2))
        self.put(piece, ox, oz, turns, **kw)
        return ox, oz, ox + w - 1, oz + d - 1

    # ------------------------------------------------ ground, streets, square

    def ground(self):
        rng = self.rng
        for z in range(SIZE):
            for x in range(SIZE):
                if self.sd[z, x] <= 0:
                    self.v.set(x, 0, z, S("grass_block", snowy="false"))
                    for y in range(1, HEIGHT + Y_MIN):
                        pass
        # The ring lane inside the wall.
        lo, hi = RING_LANE
        for z in range(SIZE):
            for x in range(SIZE):
                if -hi < self.sd[z, x] <= -lo:
                    self.v.set(x, 0, z, rng.choice([S("dirt_path"), S("dirt_path"), S("coarse_dirt"), S("gravel")]))
                    self.used[z, x] = 1

    def street(self, x0, x1, z0, z1, paved=True):
        rng = self.rng
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if not (0 <= x < SIZE and 0 <= z < SIZE) or self.sd[z, x] > -RING_LANE[0]:
                    continue
                if self.used[z, x] >= 2:
                    continue
                if paved:
                    edge = x in (x0, x1) and x1 - x0 < z1 - z0 or z in (z0, z1) and z1 - z0 < x1 - x0
                    state = S("polished_andesite") if edge else rng.choice(
                        [S("cobblestone"), S("cobblestone"), S("stone_bricks"), S("andesite"), S("mossy_cobblestone")])
                else:
                    state = rng.choice([S("dirt_path"), S("dirt_path"), S("coarse_dirt"), S("gravel"), S("dirt_path")])
                self.v.set(x, 0, z, state)
                self.used[z, x] = 1

    def square(self):
        rng = self.rng
        x0, x1, z0, z1 = SQUARE
        fx, fz = FOUNTAIN_CENTRE
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                r = math.hypot(x - fx, z - fz)
                if 5 <= r < 7 and int(r) == 5:
                    state = S("polished_andesite")
                elif r < 9 and (x + z) % 2 == 0:
                    state = S("stone_bricks")
                elif x in (x0, x1) or z in (z0, z1):
                    state = S("polished_andesite")
                else:
                    state = rng.choice([S("stone_bricks"), S("stone_bricks"), S("cobblestone"), S("andesite"),
                                        S("polished_andesite"), S("cracked_stone_bricks")])
                self.v.set(x, 0, z, state)
        self.mark(x0, z0, x1, z1, 1)
        self.place_centred(tb.fountain(), fx, fz, 0)
        # The seasonal centrepiece's plinth: a low round platform; what stands on it is a decor site.
        cx0, cz0, cx1, cz1 = CENTERPIECE
        for x in range(cx0, cx1 + 1):
            for z in range(cz0, cz1 + 1):
                self.v.set(x, 0, z, S("chiseled_stone_bricks") if (x, z) == ((cx0 + cx1) // 2, (cz0 + cz1) // 2)
                           else S("polished_andesite"))
        self.sites.append(centerpiece_site(cx0, cz0, cx1, cz1))
        # Benches round the fountain.
        for dx, dz, facing in ((-7, 0, "east"), (7, 0, "west"), (0, 7, "north")):
            for k in (-1, 0, 1):
                bx, bz = (fx + dx, fz + dz + k) if dx else (fx + dx + k, fz + dz)
                self.v.set(bx, 1, bz, S("spruce_stairs", facing=facing, half="bottom"))
        # Planters at the square's corners.
        for x, z in ((x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)):
            self.sites.append({"kind": "planter", "facing": "north", "blocks": [[x, 1, z, 0]]})
        # Lamp posts along the square's edges.
        for x in range(x0 + 6, x1 - 2, 8):
            for z in (z0 + 1, z1 - 1):
                if not (MAIN_STREET[2] - 1 <= z <= MAIN_STREET[3] + 1) and self.empty_around(x, z, 1, 1, 6):
                    self.put(tb.lamp_post(), x, z, 0)

    # ------------------------------------------------ the wall, towers and gatehouses

    def wall(self):
        rng = self.rng
        n = len(self.points)
        gate_boxes = []
        # Gatehouses first, so the wall leaves their passage open.
        for i, outward in GATES.items():
            vx, vz = self.points[i]
            ux, uz = math.cos(2 * math.pi * i / VERTICES), math.sin(2 * math.pi * i / VERTICES)
            cx, cz = vx - ux * 2.5, vz - uz * 2.5
            turns = {"north": 0, "east": 1, "south": 2, "west": 3}[outward]
            piece = tb.gatehouse(rng)
            box = self.place_centred(piece, cx, cz, turns)
            gate_boxes.append(box)
            self.mark(*box[:2], *box[2:], 3)
            # The road out through the blended ground.
            for k in range(-2, BLEND + 4):
                for s in range(-2, 3):
                    x = int(round(vx + ux * k + (-uz) * s))
                    z = int(round(vz + uz * k + ux * s))
                    if 0 <= x < SIZE and 0 <= z < SIZE and self.sd[z, x] > 0:
                        self.surface[(x, z)] = rng.choice([S("dirt_path"), S("dirt_path"), S("coarse_dirt"), S("gravel")])
        self.gate_boxes = gate_boxes

        def in_gate(x, z):
            return any(b[0] <= x <= b[2] and b[1] <= z <= b[3] for b in gate_boxes)

        towers = [self.points[i] for i in range(n) if i not in GATES]
        for z in range(SIZE):
            for x in range(SIZE):
                d = self.sd[z, x]
                if not (-WALL_THICK < d <= 0) or in_gate(x, z):
                    continue
                if any(math.hypot(x + 0.5 - tx, z + 0.5 - tz) <= TOWER_RADIUS for tx, tz in towers):
                    continue
                self.used[z, x] = 3
                outer, inner = d > -1, d <= -WALL_THICK + 1
                for y in range(0, WALL_TOP + 1):
                    self.v.set(x, y, z, S("polished_andesite") if y == 1 and outer else tb.mix(rng, "stone_bricks"))
                if outer:
                    self.v.set(x, WALL_TOP + 1, z, tb.mix(rng, "stone_bricks"))
                    if (x + z) % 2 == 0:
                        self.v.set(x, WALL_TOP + 2, z, tb.mix(rng, "stone_bricks"))
                elif inner:
                    self.v.set(x, WALL_TOP + 1, z, S("stone_brick_wall"))
                    if (x * 7 + z * 13) % 23 == 0:
                        self.v.set(x, WALL_TOP + 2, z, S("lantern", hanging="false", waterlogged="false"))
        # A plinth along the outside foot: stairs leaning on the wall.
        for z in range(SIZE):
            for x in range(SIZE):
                d = self.sd[z, x]
                if 0 < d <= 1 and not in_gate(x, z) and not any(
                        math.hypot(x + 0.5 - tx, z + 0.5 - tz) <= TOWER_RADIUS + 1 for tx, tz in towers):
                    facing = facing_towards(x, z, C, C)
                    self.v.set(x, 1, z, S("stone_brick_stairs", facing=facing, half="bottom"))
                    self.v.set(x, 0, z, S("stone_bricks"))
                    self.used[z, x] = 3
        for idx, (tx, tz) in enumerate(towers):
            self.tower(tx, tz, idx)

    def tower(self, tx, tz, idx):
        rng = self.rng
        conical = idx % 2 == 0
        r_out = TOWER_RADIUS
        door = facing_towards(int(tx), int(tz), C, C)
        dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[door]
        cx, cz = int(math.floor(tx)), int(math.floor(tz))
        for x in range(cx - 5, cx + 6):
            for z in range(cz - 5, cz + 6):
                r = math.hypot(x + 0.5 - tx, z + 0.5 - tz)
                if r > r_out:
                    continue
                if 0 <= x < SIZE and 0 <= z < SIZE:
                    self.used[z, x] = 3
                shell = r > r_out - 1.2
                self.v.set(x, 0, z, S("stone_bricks"))
                for y in range(1, TOWER_TOP + 1):
                    if shell:
                        self.v.set(x, y, z, S("polished_andesite") if y in (1, WALL_TOP + 1) else tb.mix(rng, "stone_bricks"))
                    elif y in (WALL_TOP, TOWER_TOP):
                        self.v.set(x, y, z, S("spruce_planks"))
                    else:
                        self.v.set(x, y, z, S("air"))
                if shell and not conical and (x + z) % 2 == 0:
                    self.v.set(x, TOWER_TOP + 1, z, tb.mix(rng, "stone_bricks"))
        # Door from the town, openings onto the wall walk, arrow slits, a ladder up.
        for k in (1, 2):
            px, pz = cx + dx * 4, cz + dz * 4
            for j in range(3):
                qx, qz = cx + dx * (4 - j), cz + dz * (4 - j)
                if math.hypot(qx + 0.5 - tx, qz + 0.5 - tz) > r_out - 1.3:
                    self.v.set(qx, k, qz, S("air"))
        for y in (WALL_TOP + 1, WALL_TOP + 2):
            for x in range(cx - 5, cx + 6):
                for z in range(cz - 5, cz + 6):
                    if 0 <= x < SIZE and 0 <= z < SIZE:
                        r = math.hypot(x + 0.5 - tx, z + 0.5 - tz)
                        if r_out - 1.2 < r <= r_out and -WALL_THICK < self.sd[z, x] <= -1:
                            neighbours = [(x + a, z + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))]
                            if any(0 <= p < SIZE and 0 <= q < SIZE and self.used[q, p] == 3 and
                                   math.hypot(p + 0.5 - tx, q + 0.5 - tz) > r_out for p, q in neighbours):
                                self.v.set(x, y, z, S("air"))
        # Ladder on the inside of the wall opposite the door.
        lx, lz = cx - dx * 2, cz - dz * 2
        for y in range(1, TOWER_TOP):
            self.v.set(lx, y, lz, S("ladder", facing=door, waterlogged="false"))
        self.v.set(lx, WALL_TOP, lz, S("ladder", facing=door, waterlogged="false"))
        self.v.set(lx, TOWER_TOP, lz, S("ladder", facing=door, waterlogged="false"))
        self.v.set(cx, WALL_TOP - 1, cz, S("lantern", hanging="true", waterlogged="false"))
        if conical:
            cone(self.v, tx, tz, r_out + 0.8, TOWER_TOP + 1, "deepslate_tile" if idx % 4 == 0 else "spruce")
        else:
            self.v.set(cx, TOWER_TOP + 1, cz, S("dark_oak_fence"))
            self.v.set(cx, TOWER_TOP + 2, cz, S("dark_oak_fence"))
            self.v.set(cx, TOWER_TOP + 3, cz, S("dark_oak_fence"))
            self.sites.append({"kind": "tower_flag", "facing": door, "blocks": [[cx, TOWER_TOP + 4, cz, 0]]})

    # ------------------------------------------------ special buildings

    def landmarks(self):
        rng = self.rng
        # The church faces the square from the north.
        church = tb.church(rng)
        self.put(church, 84, 39, 2)
        self.mark(80, 37, 112, 85, 2)
        # Bank on the square's west side, facing east; town hall on the east side, facing west.
        bank = tb.bank(rng)
        box = self.place_centred(bank, 64, 106, 1)
        self.mark(box[0] - 1, box[1] - 2, box[2] + 2, box[3] + 1, 2)
        hall = tb.town_hall(rng)
        box = self.place_centred(hall, 128, 107, 3)
        self.mark(box[0] - 4, box[1] - 1, box[2] + 1, box[3] + 1, 2)
        # Market stalls round the square.
        stalls = [((75, 101), 1, "seasonal"), ((75, 108), 1, "curios"), ((117, 101), 3, "florist"),
                  ((117, 108), 3, None), ((76, 89), 2, None), ((116, 89), 2, None)]
        for (cx, cz), turns, shop in stalls:
            piece = tb.stall(rng)
            if shop is None:
                piece.spots = [{**s, "role": "vendor"} for s in piece.spots]
            self.place_centred(piece, cx, cz, turns, purpose=shop)

    # ------------------------------------------------ houses along the streets

    def houses_along(self, x0, x1, z0, z1, purposes=(), tall=0.3):
        """Houses down both sides of a street box, fronts towards it."""
        rng = self.rng
        horizontal = (x1 - x0) > (z1 - z0)
        placed = []
        purposes = list(purposes)
        for side in (-1, 1):
            pos = (x0 if horizontal else z0) + 2
            end = (x1 if horizontal else z1) - 2
            while pos < end:
                w = rng.choice([7, 8, 8, 9, 10])
                d = rng.choice([8, 9, 9, 10])
                for tries in range(4):
                    if horizontal:
                        turns = 2 if side < 0 else 0
                        if side < 0:
                            fx0, fz0, fx1, fz1 = pos, z0 - 2 - d + 1, pos + w - 1, z0 - 2
                        else:
                            fx0, fz0, fx1, fz1 = pos, z1 + 2, pos + w - 1, z1 + 2 + d - 1
                    else:
                        turns = 1 if side < 0 else 3
                        if side < 0:
                            fx0, fz0, fx1, fz1 = x0 - 2 - d + 1, pos, x0 - 2, pos + w - 1
                        else:
                            fx0, fz0, fx1, fz1 = x1 + 2, pos, x1 + 2 + d - 1, pos + w - 1
                    if self.free(fx0, fz0 - 1, fx1, fz1 + 1) if horizontal else self.free(fx0 - 1, fz0, fx1 + 1, fz1):
                        break
                    d -= 1
                    if d < 7:
                        break
                else:
                    pos += 2
                    continue
                if d < 7 or not (self.free(fx0, fz0 - 1, fx1, fz1 + 1) if horizontal else self.free(fx0 - 1, fz0, fx1 + 1, fz1)):
                    pos += 2
                    continue
                purpose = purposes.pop(0) if purposes else "home"
                storeys = 3 if rng.random() < tall and w >= 8 else 2
                piece = tb.house(rng, w, d, storeys=storeys, purpose=("shop" if purpose in SHOPS else purpose))
                piece.purpose = purpose
                self.put(piece, fx0, fz0, turns, purpose=purpose if purpose in SHOPS else None)
                self.mark(fx0, fz0 - 1, fx1, fz1 + 1, 2) if horizontal else self.mark(fx0 - 1, fz0, fx1 + 1, fz1, 2)
                placed.append((fx0, fz0, fx1, fz1, purpose))
                self.houses.append((fx0, fz0, fx1, fz1, purpose))
                pos += w + 1
        return placed

    # ------------------------------------------------ gardens, trees, lamps

    def gardens(self):
        rng = self.rng
        trees = []
        reserved = {(b[0], b[2]) for site in self.sites for b in site["blocks"]}
        for z in range(SIZE):
            for x in range(SIZE):
                if self.sd[z, x] > -RING_LANE[1] or self.used[z, x] or (x, z) in reserved:
                    continue
                r = rng.random()
                if r < 0.10:
                    self.v.set(x, 1, z, S("short_grass"))
                elif r < 0.13:
                    self.v.set(x, 1, z, S(rng.choice(["poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet"])))
                elif r < 0.135:
                    self.v.set(x, 1, z, S("hay_block", axis=rng.choice("xz")))
                elif r < 0.17 and self.clear_around(x, z, 3) and all(math.hypot(x - a, z - b) > 7 for a, b in trees):
                    trees.append((x, z))
                    tree(self.v, rng, x, z, rng.choice(["oak", "oak", "birch", "cherry", "spruce"]))
                    self.mark(x - 2, z - 2, x + 2, z + 2, 2)

    def clear_around(self, x, z, r):
        x0, z0, x1, z1 = x - r, z - r, x + r, z + r
        if x0 < 0 or z0 < 0 or x1 >= SIZE or z1 >= SIZE:
            return False
        return not self.used[z0:z1 + 1, x0:x1 + 1].any() and bool((self.sd[z0:z1 + 1, x0:x1 + 1] <= -RING_LANE[1]).all())

    def lamps(self):
        """Lamp posts down the streets, on alternate sides, where nothing stands."""
        for x0, x1, z0, z1, step in [(*MAIN_STREET, 9), (*SOUTH_STREET, 9)] + [(*lane, 12) for lane in LANES]:
            horizontal = (x1 - x0) > (z1 - z0)
            k = 0
            for t in range((x0 if horizontal else z0) + 4, (x1 if horizontal else z1) - 2, step):
                k += 1
                if horizontal:
                    x, z = t, (z0 - 1) if k % 2 else (z1 + 1)
                else:
                    x, z = (x0 - 1) if k % 2 else (x1 + 1), t
                if 0 <= x < SIZE and 0 <= z < SIZE and self.sd[z, x] < -RING_LANE[1] and self.used[z, x] in (0, 2) \
                        and self.v.get(x, 1, z) is None and self.v.get(x, 2, z) is None and self.v.get(x, 3, z) is None \
                        and self.v.get(x, 4, z) is None and self.v.get(x, 5, z) is None and not self.near_door(x, z):
                    self.put(tb.lamp_post(), x, z, 0)

    def empty_around(self, x, z, r, y0, y1):
        return all(self.v.get(x + dx, y, z + dz) is None for dx in range(-r, r + 1) for dz in range(-r, r + 1)
                   for y in range(y0, y1 + 1))

    def infill(self, tries=4000):
        """More houses wherever a house fits with its front two blocks from a street, lane or the square."""
        rng = self.rng
        cells = [(x, z) for z in range(0, SIZE) for x in range(0, SIZE)]
        rng.shuffle(cells)
        for x0, z0 in cells:
            w = rng.choice([7, 7, 8, 9])
            d = rng.choice([7, 8, 8, 9])
            for turns in rng.sample(range(4), 4):
                fw, fd = (d, w) if turns % 2 else (w, d)
                x1, z1 = x0 + fw - 1, z0 + fd - 1
                if not self.free(x0 - 1, z0 - 1, x1 + 1, z1 + 1):
                    continue
                # A street within a few blocks of the front; a garden path leads to it from the door.
                piece = tb.house(rng, w, d, storeys=3 if rng.random() < 0.2 and w >= 8 else 2)
                dx_, dz_ = piece.door
                door_x, door_z = turn_xz_point(dx_, dz_ + 1, turns, (w, d), x0, z0)
                step = {0: (0, -1), 1: (1, 0), 2: (0, 1), 3: (-1, 0)}[turns]
                path = []
                px, pz = door_x + step[0], door_z + step[1]
                reached = False
                for k in range(5):
                    if not (0 <= px < SIZE and 0 <= pz < SIZE):
                        break
                    if self.used[pz, px] == 1:
                        reached = k >= 1
                        break
                    if self.used[pz, px] != 0:
                        break
                    path.append((px, pz))
                    px, pz = px + step[0], pz + step[1]
                if not reached:
                    continue
                self.put(piece, x0, z0, turns)
                for px, pz in path:
                    self.v.set(px, 0, pz, rng.choice([S("dirt_path"), S("cobblestone"), S("dirt_path")]))
                    self.used[pz, px] = 1
                self.mark(x0 - 1, z0 - 1, x1 + 1, z1 + 1, 2)
                self.houses.append((x0, z0, x1, z1, "home"))
                break

    def near_door(self, x, z):
        for (px, py, pz), state in ((p, s) for p, s in self.v.b.items() if False):
            pass
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                state = self.v.get(x + dx, 1, z + dz)
                if state and ("_door" in state or "trapdoor" in state or "stairs" in state):
                    return True
        return False

    def planters(self):
        """Planters (decor sites) at street corners and crossings."""
        spots = [(MAIN_STREET[0] + 12, MAIN_STREET[2] - 1), (MAIN_STREET[1] - 12, MAIN_STREET[3] + 1),
                 (SOUTH_STREET[0] - 1, SOUTH_STREET[3] - 12), (SOUTH_STREET[1] + 1, SOUTH_STREET[2] + 2)]
        for lane in LANES:
            spots.append((lane[0] - 1, lane[2] - 1 if lane[2] > MAIN_STREET[3] else lane[3] + 1))
        for x, z in spots:
            if 0 <= x < SIZE and 0 <= z < SIZE and self.v.get(x, 1, z) is None and self.sd[z, x] < -RING_LANE[1]:
                self.sites.append({"kind": "planter", "facing": "north", "blocks": [[x, 1, z, 0]]})

    def name_everyone(self):
        """Gives every place a townsperson stands a name and a skin (deterministic)."""
        rng = random.Random(SEED + 1)
        first = ["Aldous", "Brenna", "Cedric", "Dalia", "Edmund", "Freya", "Gareth", "Hilde", "Ivo", "Juno", "Kester",
                 "Linnea", "Merrick", "Nell", "Osric", "Perpetua", "Quill", "Rosalind", "Silas", "Tamsin", "Ulric",
                 "Vesna", "Wendel", "Ysolde", "Alaric", "Bettany", "Corwin", "Delphine", "Emrys", "Fenella"]
        last = ["Fenwick", "Thatcher", "Mallory", "Brook", "Holt", "Ashdown", "Stonewell", "Marsh", "Penrose", "Weaver",
                "Lowe", "Hart", "Vale", "Cobb", "Dunmore", "Finch", "Barrow", "Wren", "Brandt", "Reed", "Haddon",
                "Thorne", "Pike", "Gray", "Kettle", "Bramble", "Oakes", "Merriweather"]
        rng.shuffle(first)
        rng.shuffle(last)
        used = {}
        for i, spot in enumerate(self.spots):
            role = spot["role"]
            if role == "shopkeeper" and spot.get("shop") == "florist":
                skins = town_skins.ROLE_SKINS["florist"]
            else:
                skins = town_skins.ROLE_SKINS.get(role, town_skins.ROLE_SKINS["townsfolk"])
            k = used.get(role, 0)
            used[role] = k + 1
            spot["skin"] = skins[k % len(skins)]
            spot["name"] = f"{first[i % len(first)]} {last[(i * 7) % len(last)]}"

    def townsfolk(self):
        """Places where townsfolk and decorators first stand: street spots spread over the town."""
        rng = self.rng
        streets = [MAIN_STREET, SOUTH_STREET] + LANES
        picks = []
        for x0, x1, z0, z1 in streets:
            for _ in range(2):
                x, z = rng.randint(x0, x1), rng.randint(z0, z1)
                if self.sd[z, x] < -RING_LANE[1] and self.used[z, x] == 1:
                    picks.append((x, z))
        for x, z in picks[:10]:
            self.spots.append({"role": "townsfolk", "pos": [x, 1, z]})
        sx0, sx1, sz0, sz1 = SQUARE
        for k in range(3):
            self.spots.append({"role": "decorator", "pos": [96 + (k - 1) * 6, 1, 99]})
        # Guards walk the wall: one on the walk by each conical tower's neighbour.

    # ------------------------------------------------ everything

    def build(self):
        self.ground()
        self.wall()
        self.landmarks()
        self.square()
        self.street(*MAIN_STREET)
        self.street(*SOUTH_STREET)
        for lane in LANES:
            self.street(*lane, paved=False)
        rng = self.rng
        self.houses_along(MAIN_STREET[0] + 8, SQUARE[0] - 1, MAIN_STREET[2], MAIN_STREET[3],
                          purposes=["general", "bakery", "tavern"], tall=0.5)
        self.houses_along(SQUARE[1] + 1, MAIN_STREET[1] - 8, MAIN_STREET[2], MAIN_STREET[3], purposes=["tailor"], tall=0.5)
        self.houses_along(SOUTH_STREET[0], SOUTH_STREET[1], SQUARE[3] + 2, SOUTH_STREET[3] - 8, purposes=["tavern"], tall=0.4)
        for lane in LANES:
            self.houses_along(*lane, tall=0.15)
        # Fill the rest: houses facing each lane box again with smaller depth, then gardens.
        self.houses_along(SQUARE[0], SQUARE[1], SQUARE[2] - 1, SQUARE[2] - 1, tall=0.4)
        self.infill()
        self.planters()
        self.lamps()
        self.gardens()
        self.townsfolk()
        self.name_everyone()
        for x, y, z, facing in self.atms:
            self.v.set(x, y, z, S("jugcraft:atm", facing=facing))
        stair_shapes(self.v)
        connections(self.v)
        return self


def piece_purpose(piece):
    return getattr(piece, "purpose", None)


def turn_xz_point(x, z, turns, size, ox, oz):
    """A piece's local column (x, z) in town coordinates, once turned and placed at (ox, oz)."""
    from town_voxels import turn_xz
    rx, rz = turn_xz(x, z, turns, size)
    return rx + ox, rz + oz


def facing_towards(x, z, tx, tz):
    dx, dz = tx - x, tz - z
    if abs(dx) > abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def cone(v, cx, cz, radius, y0, roof):
    """A round tower's conical roof: rings of tiles narrowing upward, stairs on each ring's edge, a finial on top."""
    stairs = f"{roof}_stairs"
    full = f"{roof}s" if roof.endswith("tile") else f"{roof}_planks"
    k = 0
    r = radius
    while r > 0.6:
        y = y0 + k
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for z in range(int(cz - r) - 1, int(cz + r) + 2):
                d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
                if d <= r:
                    if d > r - 1.0:
                        v.set(x, y, z, S(stairs, facing=facing_towards(x, z, int(cx), int(cz)), half="bottom", waterlogged="false"))
                    else:
                        v.set(x, y, z, S(full))
        r -= 0.7
        k += 1
    v.set(int(cx), y0 + k, int(cz), S(f"{roof}_fence" if roof != "deepslate_tile" else "deepslate_tile_wall"))
    v.set(int(cx), y0 + k + 1, int(cz), S("lightning_rod", facing="up", powered="false", waterlogged="false"))


def tree(v, rng, x, z, kind):
    """A small garden tree of vanilla logs and persistent leaves."""
    if kind == "spruce":
        h = rng.randint(6, 8)
        for y in range(1, h + 1):
            v.set(x, y, z, S("spruce_log", axis="y"))
        for y in range(3, h + 2):
            r = max(0, (h + 1 - y) // 2) if y < h + 1 else 0
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if (dx, dz) != (0, 0) or y > h:
                        if abs(dx) + abs(dz) <= r + (0 if y % 2 else -1):
                            v.set(x + dx, y, z + dz, S("spruce_leaves", distance="1", persistent="true", waterlogged="false"))
        return
    log, leaves = {"oak": ("oak_log", "oak_leaves"), "birch": ("birch_log", "birch_leaves"),
                   "cherry": ("cherry_log", "cherry_leaves")}[kind]
    h = rng.randint(4, 6)
    for y in range(1, h + 1):
        v.set(x, y, z, S(log, axis="y"))
    for y in range(h - 2, h + 2):
        r = 2 if y < h + 1 else 1
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) == r and abs(dz) == r and (rng.random() < 0.6 or y >= h):
                    continue
                if (dx, dz) == (0, 0) and y <= h:
                    continue
                v.set(x + dx, y, z + dz, S(leaves, distance="1", persistent="true", waterlogged="false"))


def centerpiece_site(x0, z0, x1, z1):
    """The square's centrepiece: one site spanning its plinth and the air above (up to 13 high); each theme fills its
    own blocks (slot numbers index tools/town_decor.py's CENTERPIECES)."""
    blocks = []
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(1, 14):
                blocks.append([x, y, z, (x - x0) + 7 * (z - z0) + 49 * (y - 1)])
    return {"kind": "centerpiece", "facing": "north", "blocks": blocks}


# ---------------------------------------------------------------- export

def mask_bytes(town):
    """Per column: 255 inside the wall (the ground is levelled), 1..BLEND outside it (blocks from the wall), 0 beyond."""
    out = bytearray(SIZE * SIZE)
    for z in range(SIZE):
        for x in range(SIZE):
            d = town.sd[z, x]
            out[z * SIZE + x] = 255 if d <= 0 else int(math.ceil(d)) if d <= BLEND else 0
    return bytes(out)


def export(town, path=OUT):
    """Writes the town as gzipped JSON: a block palette, the blocks as little-endian 16-bit palette indexes (0: leave the
    world's block, 1: air) in x, then z, then y order, the column mask and surface, sites, spots, ATMs and gates."""
    palette = ["keep", "minecraft:air"]
    index = {"keep": 0, "minecraft:air": 1}
    vol = np.zeros((HEIGHT, SIZE, SIZE), dtype="<u2")   # [y, z, x]
    # Inside the wall, everything above the ground that the town does not fill is cleared.
    inside = town.sd <= 0
    for y in range(1, HEIGHT + Y_MIN):
        vol[y - Y_MIN][inside] = 1
    for (x, y, z), state in town.v.b.items():
        if not (0 <= x < SIZE and 0 <= z < SIZE and Y_MIN <= y < Y_MIN + HEIGHT):
            raise ValueError(f"block outside the town's data: {x} {y} {z} {state}")
        if state not in index:
            index[state] = len(palette)
            palette.append(state)
        vol[y - Y_MIN, z, x] = index[state]
    surface = np.zeros((SIZE, SIZE), dtype="<u2")
    for (x, z), state in town.surface.items():
        if state not in index:
            index[state] = len(palette)
            palette.append(state)
        surface[z, x] = index[state]
    sites = []
    for site in town.sites:
        sites.append({"kind": site["kind"], "facing": site["facing"], "blocks": site["blocks"]})
    data = {
        "size": SIZE, "y_min": Y_MIN, "height": HEIGHT, "blend": BLEND, "protect": PROTECT,
        "wall_top": WALL_TOP, "centre": [C, C],
        "gates": [[int(round(town.points[i][0])), int(round(town.points[i][1])), GATES[i]] for i in sorted(GATES)],
        "palette": palette,
        "blocks": base64.b64encode(vol.tobytes()).decode(),
        "mask": base64.b64encode(mask_bytes(town)).decode(),
        "surface": base64.b64encode(surface.tobytes()).decode(),
        "sites": sites,
        "spots": town.spots,
        "atms": town.atms,
        "decor": town_decor.data(),
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    shops = {"shops": town_shops.data(),
             "economy": {"welcome": town_shops.WELCOME_JUGS, "max_balance": town_shops.MAX_BALANCE,
                         "trade_range": town_shops.TRADE_RANGE, "atm_range": town_shops.ATM_RANGE}}
    (path.parent / "shops.json").write_text(json.dumps(shops, indent=2) + "\n")
    raw = json.dumps(data, separators=(",", ":"), sort_keys=True).encode()
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(raw)
    return data


def check(town):
    """Every block state is a real one (against 26.3's summary when it is at hand) and every site sits in air."""
    errors = []
    summary = vanilla_states()
    for state in set(town.v.b.values()):
        check_state(state, summary, errors)
    for state in town_decor.all_states():
        check_state(state, summary, errors)
    errors += town_shops.loop_errors()
    for site in town.sites:
        for x, y, z, slot in site["blocks"]:
            if site["kind"] != "centerpiece" and town.v.get(x, y, z) not in (None, S("air")):
                errors.append(f"{site['kind']} site at {x} {y} {z} is not empty: {town.v.get(x, y, z)}")
    return errors


def write():
    """Writes the skins, the town data and the shops (for tools/generate_material_data.py); returns any problems."""
    town_skins.write()
    town = Town().build()
    errors = check(town)
    export(town)
    return errors


def main():
    town_skins.write()
    town = Town().build()
    errors = check(town)
    for e in sorted(set(errors))[:40]:
        print("town:", e)
    data = export(town)
    kinds = {}
    for s in town.sites:
        kinds[s["kind"]] = kinds.get(s["kind"], 0) + 1
    roles = {}
    for s in town.spots:
        roles[s["role"]] = roles.get(s["role"], 0) + 1
    print(f"town: {len(town.houses)} houses, {len(town.v.b)} blocks, palette {len(data['palette'])}, sites {kinds}, "
          f"spots {roles}, atms {len(town.atms)}, {OUT.stat().st_size // 1024} KiB")
    if "--preview" in sys.argv:
        import town_preview as tp
        out = Path(__file__).resolve().parent.parent / "build" / "town"
        out.mkdir(parents=True, exist_ok=True)
        tp.plan(town.v, SIZE, 5).save(out / "plan.png")
        tp.iso(town.v, scale=2).save(out / "town_iso.png")
        tp.iso(town.v, scale=2, back=True).save(out / "town_iso_back.png")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
