"""Adam-ondi-Ahman (docs/features/adam-ondi-ahman.md, docs/branches/BOSSES.md): a quiet river valley of prairie grass,
wildflowers and lone oaks and birches, with a knoll of three stacked stone altars at its heart, modelled on the real
Adam-ondi-Ahman on the bluffs above the Grand River in northern Missouri. On the top altar lies the Ancient Altar, a
stone older than the rest, carved with a red star; in part 1 it is dormant.

Where the valleys are (world/AdamOndiAhman, which reads these numbers; tools/check_mod_data.py holds them together):
- one near the start of every new world, START["min"] to START["max"] blocks from the world's start, chosen once on the
  world's first start, after Alpine Spawn and the town, and saved;
- elsewhere, one in CHANCE of the cells SPACING blocks square (the seed decides which), at the first of TRIES places
  in the cell (inset INSET blocks from its edges) whose land is suitable (SUITABLE).
A valley is the land within RADIUS (give or take WOBBLE) blocks of its knoll; rivers, oceans and beaches there keep
their own biomes (except on the knoll's own ground, KNOLL_GROUND), and so does everything below the surface (DEPTH).
Terrain does not depend on biomes: the knoll itself is the structure template written here, which world/AltarKnolls
builds at each site once every chunk under it (and its blend ring) is newly generated and loaded, as the town is
built: it levels the ground over the knoll's pad (HALF blocks either way of the site) to the knoll's height, blends it
back to the land's own over a ring BLEND blocks wide, and places the template, turned as the site's number says.

This module lays the knoll out (the template, a list of named parts) and states the constants Java reads.
"""
import math

from lair_layout import block, hash01, template, write as write_nbt

BIOME = "adam_ondi_ahman"
DISPLAY = "Adam-ondi-Ahman"
FEATURE = "adam_ondi_ahman"
TEMPLATE = "adam_ondi_ahman/altar_knoll"
ALTAR = "ancient_altar"
ALTAR_DISPLAY = "Ancient Altar"
DORMANT = "The altar is dormant. Its oldest stone is carved with a red star."

# The rare valleys: one candidate in CHANCE of the cells SPACING blocks square, inset INSET blocks from the cell's edges
# (so a valley never reaches past its own cell). Settings adam_ondi_ahman.spacing (MIN_SPACING to MAX_SPACING) and
# adam_ondi_ahman.chance (0 to 1).
SPACING = 3072
MIN_SPACING = 1024
MAX_SPACING = 16384
CHANCE = 0.5
# A valley's reach from its site, in blocks: RADIUS, wavering by up to WOBBLE round its edge.
RADIUS = 176
WOBBLE = 32
INSET = RADIUS + WOBBLE + 64
# A cell tries up to TRIES places in turn; the first whose land suits is its valley.
TRIES = 6
# The valley near the start: START["min"] to START["max"] blocks from the world's start, tried in rings START["step"]
# apart and every START["angle"] degrees round each, nearest first; never within ORIGIN_CLEARANCE blocks of the origin
# (where vanilla's own spawn search may already have generated chunks). Setting adam_ondi_ahman.start (on or off).
START = {"min": 800, "max": 1600, "step": 100, "angle": 15}
ORIGIN_CLEARANCE = 768
# Suitable land for a knoll, read from the land's noise without generating anything:
# - the climate there: inland (continentalness), not the mountains (erosion), temperate (temperature and humidity in
#   vanilla's middle three bands) and not a river's bed (peaks and valleys, from weirdness);
# - the ground: dry (no water over it) at a 3 x 3 grid of samples REACH blocks apart round the site, at least
#   GROUND["min"] and at most GROUND["max"] high at its middle, and no more than GROUND["slope"] blocks between the
#   highest and the lowest sample.
SUITABLE = {"continentalness": -0.11, "erosion": -0.375, "temperature": [-0.45, 0.55], "humidity": [-0.35, 0.3],
            "peaks_valleys": -0.2}
REACH = 40
GROUND = {"min": 65, "max": 110, "slope": 10}
# Biome lookups deeper than this (vanilla's depth parameter, where its cave biomes begin) keep their own biomes.
DEPTH = 0.2
# The knoll's own ground: within this many blocks of its site (a square) the valley is the biome even over a river, so
# the knoll's structure, which checks the biome at its template's middle, always finds it.
KNOLL_GROUND = 64

# The knoll's template: SIZE (x, y, z). Its layer 0 is subsoil and layer 1 the ground, which the builder sets on the
# land's height at the site (the middle of nine samples over the pad, from the land's noise). It turns round CENTRE.
SIZE = (48, 18, 48)
GROUND_Y = 1
CENTRE = (23, 23)
# The builder (world/AltarKnolls): the pad levelled for the knoll reaches HALF blocks either way of the site (the
# template reaches CENTRE + 1 either way, however it is turned); the ground blends back to the land's own height over a
# ring BLEND blocks wide round it; a hollow under it is filled at most FILL_DEPTH deep, and land over it cleared at most
# CLEAR_HEIGHT high.
BUILD = {"half": 25, "blend": 8, "fill_depth": 24, "clear_height": 32}
# The knoll: a mound MOUND["radius"] blocks round, rising MOUND["peak"] blocks to a flat crown MOUND["crown"] round.
MOUND = {"radius": 21, "peak": 8, "crown": 6}
# The altars on its crown, stacked: (half-width, block weights); each one block over the last. The Ancient Altar lies
# on the top one's middle.
TIERS = [
    (5, [("minecraft:cobblestone", 0.48), ("minecraft:mossy_cobblestone", 0.37), (None, 0.15)]),
    (3, [("minecraft:stone_bricks", 0.5), ("minecraft:cracked_stone_bricks", 0.25), ("minecraft:mossy_stone_bricks", 0.25)]),
    (1, [("minecraft:chiseled_stone_bricks", 1.0)]),
]
CROWN_Y = GROUND_Y + MOUND["peak"]
ALTAR_POS = (CENTRE[0], CROWN_Y + len(TIERS) + 1, CENTRE[1])
# The path up the south side, PATH_HALF blocks either side of the middle.
PATH_HALF = 1
# The old settlement at the knoll's foot: two cabins' log footings (x0, z0, x1, z1 inclusive), the first with a fallen
# chimney; a spring in a ring of stones (middle and radius); and a ring of flat stones for gathering (middle, radius).
CABINS = [(2, 2, 8, 7), (39, 2, 45, 7)]
CHIMNEY = (2, 4)
SPRING = ((41, 41), 3)
GATHERING = ((6, 42), 3)
FLOWERS = ["minecraft:dandelion", "minecraft:poppy", "minecraft:oxeye_daisy", "minecraft:cornflower",
           "minecraft:azure_bluet", "jugcraft:goldenrod"]
SALT = 1838


def mound_height(x, z):
    """The mound's height over the ground at (x, z): 0 off the mound, MOUND["peak"] on the crown."""
    d = math.hypot(x - CENTRE[0], z - CENTRE[1])
    r, peak, crown = MOUND["radius"], MOUND["peak"], MOUND["crown"]
    if d >= r:
        return 0
    if d <= crown:
        return peak
    wobble = (hash01(x, z, SALT) - 0.5) * 0.8
    return max(0, min(peak, round(peak * ((r - d) / (r - crown)) ** 1.3 + wobble)))


def on_path(x, z):
    return abs(x - CENTRE[0]) <= PATH_HALF and z > CENTRE[1] + MOUND["crown"]


def pick(weights, *at):
    roll = hash01(*at)
    for name, weight in weights:
        if roll < weight:
            return name
        roll -= weight
    return weights[-1][0]


def heights():
    """The ground's top layer at every column: the mound's, with the path up the south side kept to steps of one."""
    tops = {(x, z): GROUND_Y + mound_height(x, z) for x in range(SIZE[0]) for z in range(SIZE[2])}
    for x in range(CENTRE[0] - PATH_HALF, CENTRE[0] + PATH_HALF + 1):
        # From the crown down to the south edge, never more than one block lower than the column before.
        for z in range(CENTRE[1] + MOUND["crown"] + 1, SIZE[2]):
            tops[(x, z)] = max(tops[(x, z)], tops[(x, z - 1)] - 1)
    return tops


def blocks():
    """The template as {(x, y, z): state}: the knoll, its altars, the settlement's ruins and the spring."""
    out = {}
    tops = heights()
    grass = block("minecraft:grass_block", snowy="false")
    dirt = block("minecraft:dirt")
    air = block("minecraft:air")
    for (x, z), top in tops.items():
        out[(x, 0, z)] = dirt
        for y in range(GROUND_Y, top):
            out[(x, y, z)] = dirt
        if on_path(x, z) and top > GROUND_Y:
            out[(x, top, z)] = block("minecraft:coarse_dirt") if hash01(x, z, SALT + 1) < 0.6 else block("minecraft:gravel")
        else:
            out[(x, top, z)] = grass
        # The mound is cleared above, so the land round it never buries it.
        if top > GROUND_Y or math.hypot(x - CENTRE[0], z - CENTRE[1]) < MOUND["radius"]:
            for y in range(top + 1, SIZE[1]):
                out[(x, y, z)] = air
    # The altars, stacked on the crown.
    for i, (half, weights) in enumerate(TIERS):
        y = CROWN_Y + 1 + i
        for x in range(CENTRE[0] - half, CENTRE[0] + half + 1):
            for z in range(CENTRE[1] - half, CENTRE[1] + half + 1):
                name = pick(weights, x, y, z, SALT + 2)
                # Stones are missing only from an altar's open edge, never under the altar above it.
                if name is None and i + 1 < len(TIERS) and max(abs(x - CENTRE[0]), abs(z - CENTRE[1])) <= TIERS[i + 1][0]:
                    name = weights[0][0]
                corner = abs(x - CENTRE[0]) == half and abs(z - CENTRE[1]) == half
                if i == 2 and not corner and (x, z) != CENTRE:
                    name = "minecraft:stone_bricks"
                if name is not None:
                    out[(x, y, z)] = block(name)
    out[ALTAR_POS] = (f"jugcraft:{ALTAR}", ())
    # Fallen stones round the crown's rim: wall stubs and rubble.
    for k in range(6):
        angle = 2 * math.pi * (k + hash01(k, SALT + 3) * 0.6) / 6
        x = round(CENTRE[0] + (MOUND["crown"] + 2) * math.cos(angle))
        z = round(CENTRE[1] + (MOUND["crown"] + 2) * math.sin(angle))
        if on_path(x, z):
            continue
        y = tops[(x, z)] + 1
        out[(x, y, z)] = block("minecraft:mossy_cobblestone_wall") if k % 2 == 0 else block("minecraft:mossy_cobblestone")
    # The cabins' footings: one course of logs, a few gone; the first has a chimney, fallen.
    for n, (x0, z0, x1, z1) in enumerate(CABINS):
        log = "minecraft:oak_log" if n == 0 else "minecraft:stripped_oak_log"
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge_x = z in (z0, z1)
                edge_z = x in (x0, x1)
                if not (edge_x or edge_z):
                    if hash01(x, z, SALT + 4) < 0.3:
                        out[(x, GROUND_Y, z)] = block("minecraft:coarse_dirt")
                    continue
                if hash01(x, z, SALT + 5 + n) < 0.2:
                    continue
                out[(x, GROUND_Y + 1, z)] = block(log, axis="x" if edge_x else "z")
    cx, cz = CHIMNEY
    out[(cx, GROUND_Y + 1, cz)] = block("minecraft:mossy_cobblestone")
    out[(cx, GROUND_Y + 2, cz)] = block("minecraft:cobblestone")
    for i in range(1, 4):
        out[(cx - 1, GROUND_Y + 1, cz + i)] = block("minecraft:cobblestone" if i % 2 else "minecraft:mossy_cobblestone")
    # The spring: still water in a ring of stones.
    (sx, sz), sr = SPRING
    for x in range(sx - sr - 1, sx + sr + 2):
        for z in range(sz - sr - 1, sz + sr + 2):
            d = math.hypot(x - sx, z - sz)
            if d <= sr - 0.5:
                out[(x, 0, z)] = block("minecraft:gravel")
                out[(x, GROUND_Y, z)] = block("minecraft:water", level="0")
            elif d <= sr + 0.5 and hash01(x, z, SALT + 7) < 0.7:
                out[(x, GROUND_Y + 1, z)] = block("minecraft:mossy_cobblestone" if hash01(x, z, SALT + 8) < 0.5 else "minecraft:cobblestone")
    # The gathering place: flat stones in a ring round a mossy stone.
    (gx, gz), gr = GATHERING
    out[(gx, GROUND_Y + 1, gz)] = block("minecraft:mossy_cobblestone")
    for k in range(6):
        angle = 2 * math.pi * k / 6
        x, z = round(gx + gr * math.cos(angle)), round(gz + gr * math.sin(angle))
        out[(x, GROUND_Y + 1, z)] = block("minecraft:smooth_stone_slab", type="bottom", waterlogged="false")
    # Grass and flowers on open ground (never on the path, the altars or the ruins).
    for (x, z), top in tops.items():
        if on_path(x, z) or out.get((x, top, z)) != grass or out.get((x, top + 1, z), air) != air:
            continue
        roll = hash01(x, z, SALT + 9)
        if roll < 0.035:
            out[(x, top + 1, z)] = block(FLOWERS[int(hash01(x, z, SALT + 10) * len(FLOWERS)) % len(FLOWERS)])
        elif roll < 0.055 and top + 2 < SIZE[1] and out.get((x, top + 2, z), air) == air:
            out[(x, top + 1, z)] = block("minecraft:tall_grass", half="lower")
            out[(x, top + 2, z)] = block("minecraft:tall_grass", half="upper")
        elif roll < 0.3:
            out[(x, top + 1, z)] = block("minecraft:short_grass")
    return out


def blocks_registered():
    """Every block Adam-ondi-Ahman registers: the Ancient Altar, which has no item."""
    return [ALTAR]


def parts():
    """Named places in the template, for the audit and the tests: the altar, the crown, the path's foot."""
    return {"altar": ALTAR_POS, "crown": (CENTRE[0], CROWN_Y, CENTRE[1]),
            "path_foot": (CENTRE[0], GROUND_Y, SIZE[2] - 1)}


def write(path):
    write_nbt(path, template(blocks(), SIZE))


if __name__ == "__main__":
    placed = blocks()
    counts = {}
    for name, _ in placed.values():
        counts[name] = counts.get(name, 0) + 1
    for name, count in sorted(counts.items(), key=lambda item: -item[1]):
        print(f"{count:6d} {name}")
