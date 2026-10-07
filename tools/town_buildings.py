"""The walled town's buildings (tools/town.py lays them out): half-timbered houses and shops, the church, the gatehouses,
market stalls, the fountain and lamp posts. Every building is an original design built from vanilla blocks.

Each builder works in its own frame: the building fills x 0..w-1 and z 0..d-1 with its front (its door) on the north
side at z = 0, and y = 0 is the ground it stands on (its floor); eaves, steps and shutters may reach one block out.
The layout turns it to face its street. A builder returns a Piece: the blocks, plus the decor sites the town's
decorators change with the seasons, the places townsfolk stand, and the town's ATMs.
"""
import math
import random

from town_voxels import S, Voxels, turn_xz, turn_facing


class Piece:
    def __init__(self, w, d):
        self.w, self.d = w, d
        self.v = Voxels()
        self.sites = []      # {"kind", "facing", "blocks": [[x, y, z, slot], ...]}
        self.spots = []      # {"role", "pos": [x, y, z], ...}
        self.atms = []       # [x, y, z, facing]
        self.door = None     # (x, z) of the doorstep outside the front door

    def site(self, kind, facing, blocks):
        self.sites.append({"kind": kind, "facing": facing, "blocks": [list(b) for b in blocks]})

    def spot(self, role, x, y, z, **extra):
        self.spots.append({"role": role, "pos": [x, y, z], **extra})

    def placed(self, ox, oy, oz, turns):
        """This piece's blocks, sites, spots and ATMs moved into town coordinates: turned `turns` quarter turns
        clockwise (the front then faces east, south or west) and its frame's corner put at (ox, oy, oz)."""
        size = (self.w, self.d)
        out = Voxels()
        out.paste(self.v, ox, oy, oz, turns, size)

        def move(x, y, z):
            rx, rz = turn_xz(x, z, turns, size)
            return rx + ox, y + oy, rz + oz

        sites = []
        for site in self.sites:
            blocks = [[*move(x, y, z), slot] for x, y, z, slot in site["blocks"]]
            sites.append({**site, "facing": turn_facing(site["facing"], turns), "blocks": blocks})
        spots = [{**spot, "pos": list(move(*spot["pos"]))} for spot in self.spots]
        atms = [[*move(x, y, z), turn_facing(f, turns)] for x, y, z, f in self.atms]
        return out, sites, spots, atms


# ---------------------------------------------------------------- styles

PLASTERS = ["calcite", "white_terracotta", "pale_oak_planks", "smooth_sandstone", "mushroom_stem"]
TIMBERS = [("dark_oak_log", "dark_oak"), ("spruce_log", "spruce"), ("stripped_dark_oak_log", "dark_oak"),
           ("oak_log", "oak")]
ROOFS = ["spruce", "dark_oak", "brick", "mangrove", "resin_brick", "deepslate_tile", "mud_brick"]
BASES = ["cobblestone", "stone_bricks", "mossy_cobblestone", "andesite", "tuff_bricks"]
FLOORS = ["spruce_planks", "oak_planks", "dark_oak_planks", "birch_planks"]


def style(rng, **force):
    timber, wood = rng.choice(TIMBERS)
    s = {
        "plaster": rng.choice(PLASTERS),
        "timber": timber,
        "wood": wood,
        "roof": rng.choice(ROOFS),
        "base": rng.choice(BASES),
        "floor": rng.choice(FLOORS),
        "door": rng.choice(["spruce", "dark_oak", "oak"]),
        "shutter": rng.choice(["spruce", "dark_oak", "oak", "jungle"]),
        "stone_ground": rng.random() < 0.6,
    }
    s.update(force)
    return s


def roof_stairs(roof):
    return f"{roof}_stairs"


def roof_slab(roof):
    return f"{roof}_slab"


def mix(rng, base):
    """A wall block with a little variety: stone bricks crack and mossy, cobblestone goes mossy."""
    r = rng.random()
    if base == "stone_bricks":
        return S("cracked_stone_bricks") if r < 0.12 else S("mossy_stone_bricks") if r < 0.22 else S("stone_bricks")
    if base == "cobblestone":
        return S("mossy_cobblestone") if r < 0.15 else S("cobblestone")
    if base == "mossy_cobblestone":
        return S("cobblestone") if r < 0.5 else S("mossy_cobblestone")
    if base == "andesite":
        return S("polished_andesite") if r < 0.3 else S("andesite")
    if base == "tuff_bricks":
        return S("chiseled_tuff_bricks") if r < 0.06 else S("tuff_bricks")
    return S(base)


def log(timber, axis="y"):
    return S(timber, axis=axis)


# ---------------------------------------------------------------- house

def house(rng, w, d, storeys=2, purpose="home", gable_front=None, st=None):
    """A half-timbered house: a stone or timbered ground floor, jettied upper floors of timber and plaster, a steep
    gable roof (ridge along the street, or a gable facing it), a chimney, shutters, window boxes and a lit door.
    `purpose` furnishes the ground floor: home, shop (a counter and a shopkeeper's place), tavern or bakery."""
    st = st or style(rng)
    p = Piece(w, d)
    v = p.v
    if gable_front is None:
        gable_front = rng.random() < 0.45 and w <= 9
    floor = S(st["floor"])
    timber = st["timber"]
    plaster = S(st["plaster"])
    door_x = w // 2 if purpose != "home" else rng.choice([2, w // 2, w - 3])
    # Ground floor: y 1..4, floor at 0.
    for x in range(w):
        for z in range(d):
            edge = x in (0, w - 1) or z in (0, d - 1)
            v.set(x, 0, z, mix(rng, st["base"]) if edge else floor)
    for y in range(1, 5):
        for x in range(w):
            for z in (0, d - 1):
                v.set(x, y, z, ground_wall(rng, st, x, y, w))
        for z in range(d):
            for x in (0, w - 1):
                v.set(x, y, z, ground_wall(rng, st, z, y, d))
    for y in range(1, 5):
        for x, z in ((0, 0), (w - 1, 0), (0, d - 1), (w - 1, d - 1)):
            v.set(x, y, z, log(timber) if not st["stone_ground"] else mix(rng, "stone_bricks" if st["base"] != "tuff_bricks" else "tuff_bricks"))
    # Door with a lintel, a lantern on a bracket and a step.
    door = S(f"{st['door']}_door", facing="north", half="lower", hinge="left", open="false", powered="false")
    v.set(door_x, 1, 0, door)
    v.set(door_x, 2, 0, S(f"{st['door']}_door", facing="north", half="upper", hinge="left", open="false", powered="false"))
    v.set(door_x, 3, 0, log(timber, "x"))
    p.door = (door_x, -1)
    lamp_x = door_x + 1 if door_x + 1 < w - 1 else door_x - 1
    v.set(lamp_x, 2, -1, S(f"{st['wood']}_stairs" if st['wood'] != "oak" else "oak_stairs", facing="south", half="top"))
    v.set(lamp_x, 3, -1, S("lantern", hanging="false"))
    # Ground-floor windows (front and back) with shutters and window boxes.
    for x in window_columns(w, door_x):
        ground_window(p, st, x, 0, "north")
        ground_window(p, st, x, d - 1, "south")
    for z in range(2, d - 2, 3):
        side_window(v, st, 0, z, 2, "west")
        side_window(v, st, w - 1, z, 2, "east")
    # Upper floors.
    zf, zb, top = 0, d - 1, 4
    for storey in range(1, storeys):
        jetty = storey <= 2
        nzf = zf - 1 if jetty else zf
        beam_y = top + 1
        # Floor beams and boards; corbels under the jetty.
        for x in range(w):
            for z in range(nzf, zb + 1):
                edge = x in (0, w - 1) or z in (nzf, zb)
                v.set(x, beam_y, z, log(timber, "x" if z in (nzf, zb) else "z") if edge else floor)
        if jetty:
            for x in range(0, w, 2 if w % 2 else 3):
                v.set(x, beam_y - 1, nzf, S(f"{st['wood']}_stairs", facing="south", half="top"))
            v.set(w - 1, beam_y - 1, nzf, S(f"{st['wood']}_stairs", facing="south", half="top"))
        y0, y1 = beam_y + 1, beam_y + 4
        timber_walls(p, rng, st, w, nzf, zb, y0, y1, storey)
        # Ladder up through a hole in the floor.
        lx, lz = w - 2, zb - 1
        for y in range(top - 3 if storey == 1 else top - 3, beam_y + 1):
            v.set(lx, y, lz, S("ladder", facing="north", waterlogged="false"))
        zf, top = nzf, y1
    # Attic floor (the top storey's ceiling, so its lantern can hang) and top plate.
    for x in range(w):
        for z in range(zf, zb + 1):
            edge = x in (0, w - 1) or z in (zf, zb)
            v.set(x, top + 1, z, log(timber, "x" if z in (zf, zb) else "z") if edge else floor)
    roof(p, rng, st, w, zf, zb, top + 2, gable_front)
    chimney(p, rng, st, w, d, zf, zb, top + 2, gable_front)
    furnish(p, rng, st, w, d, purpose, door_x)
    # Upper-floor lanterns hang from the beams.
    yy = 4
    for storey in range(storeys):
        v.set(w // 2, yy, (zf + zb) // 2 if storey else d // 2, S("lantern", hanging="true"))
        yy += 5
    # A banner on the upper front, beside a window (a decor site).
    if storeys > 1:
        bx = 1 if door_x > 2 else w - 2
        p.site("banner", "north", [(bx, 8, -2, 0)])
    return p


def ground_wall(rng, st, i, y, length):
    if st["stone_ground"]:
        return mix(rng, st["base"])
    if i in (0, length - 1) or i % 3 == 0:
        return log(st["timber"])
    if y in (1,):
        return mix(rng, st["base"])
    return S(st["plaster"])


def window_columns(w, door_x):
    cols = []
    for x in range(1, w - 1):
        if abs(x - door_x) >= 2 and x not in (1, w - 2) and (x - door_x) % 3 == 0:
            cols.append(x)
    if not cols:
        for x in (2, w - 3):
            if abs(x - door_x) >= 2 and 0 < x < w - 1:
                cols.append(x)
    return cols


def ground_window(p, st, x, z, facing):
    v = p.v
    out = -1 if facing == "north" else 1
    for y in (2, 3):
        v.set(x, y, z, S("glass_pane"))
    # Shutters, open against the wall either side.
    for sx in (x - 1, x + 1):
        for y in (2, 3):
            v.set(sx, y, z + out, S(f"{st['shutter']}_trapdoor", facing=facing, half="bottom", open="true", powered="false",
                                    waterlogged="false"))
    if facing == "north":
        # Window box: a trapdoor shelf; the pot on it is a decor site.
        v.set(x, 1, z + out, S(f"{st['shutter']}_trapdoor", facing="south", half="top", open="false", powered="false",
                               waterlogged="false"))
        p.site("window_box", facing, [(x, 2, z + out, 0)])


def side_window(v, st, x, z, y, facing):
    v.set(x, y, z, S("glass_pane"))
    v.set(x, y + 1, z, S("glass_pane"))


def timber_walls(p, rng, st, w, zf, zb, y0, y1, storey):
    """Upper storey walls: posts at corners and every third block, rails top and bottom, plaster between, windows."""
    v = p.v
    plaster = S(st["plaster"])
    timber = st["timber"]
    for y in range(y0, y1 + 1):
        for x in range(w):
            for z in (zf, zb):
                post = x in (0, w - 1) or x % 3 == 0
                v.set(x, y, z, log(timber) if post else plaster)
        for z in range(zf, zb + 1):
            for x in (0, w - 1):
                post = z in (zf, zb) or (z - zf) % 3 == 0
                v.set(x, y, z, log(timber) if post else plaster)
    # A mid rail on blank panels.
    mid = y0 + 1
    for x in range(1, w - 1):
        for z in (zf, zb):
            if x % 3 != 0:
                v.set(x, mid, z, log(timber, "x"))
    # Windows between posts (two high), shutters on the front, a window box under each front one.
    for x in range(1, w - 1):
        if x % 3 == 0 or (x % 3 == 2 and w > 7 and x + 1 < w - 1 and (x + 1) % 3 != 0):
            continue
        if x % 3 == 1 and x + 1 < w - 1 and (x + 1) % 3 != 0:
            for z, facing in ((zf, "north"), (zb, "south")):
                for y in (mid + 1, mid + 2):
                    if y <= y1:
                        v.set(x, y, z, S("glass_pane"))
                        v.set(x + 1, y, z, S("glass_pane"))
            # Window box under the front pair.
            v.set(x, mid, zf - 1, S(f"{st['shutter']}_trapdoor", facing="south", half="top", open="false", powered="false",
                                    waterlogged="false"))
            v.set(x + 1, mid, zf - 1, S(f"{st['shutter']}_trapdoor", facing="south", half="top", open="false", powered="false",
                                        waterlogged="false"))
            p.site("window_box", "north", [(x, mid + 1, zf - 1, 0), (x + 1, mid + 1, zf - 1, 1)])
    for z in range(zf + 2, zb - 1, 3):
        for x in (0, w - 1):
            v.set(x, mid + 1, z, S("glass_pane"))


def roof(p, rng, st, w, zf, zb, y0, gable_front):
    """A 45-degree gable roof with a one-block overhang; gables filled with plaster and timber, a window in each."""
    v = p.v
    stairs = roof_stairs(st["roof"])
    slab = roof_slab(st["roof"])
    plaster = S(st["plaster"])
    if not gable_front:
        # Ridge along x: slopes face front and back.
        k = 0
        while True:
            zl, zr = zf - 1 + k, zb + 1 - k
            y = y0 + k
            if zl > zr:
                break
            for x in range(-1, w + 1):
                if zl == zr:
                    v.set(x, y, zl, S(slab, type="bottom", waterlogged="false"))
                else:
                    v.set(x, y, zl, S(stairs, facing="south", half="bottom", waterlogged="false"))
                    v.set(x, y, zr, S(stairs, facing="north", half="bottom", waterlogged="false"))
            # Gable ends under the slopes.
            for z in range(zl + 1, zr):
                for x in (0, w - 1):
                    v.set(x, y, z, log(st["timber"]) if (z - zf) % 3 == 0 else plaster)
            # Cap the ridge if the slopes meet edge to edge.
            if zr == zl + 1:
                for x in range(-1, w + 1):
                    v.set(x, y + 1, zl, S(slab, type="bottom", waterlogged="false"))
                    v.set(x, y + 1, zr, S(slab, type="bottom", waterlogged="false"))
                break
            k += 1
        mid = (zf + zb) // 2
        if zb - zf >= 6:
            for x in (0, w - 1):
                v.set(x, y0 + 1, mid, S("glass_pane"))
    else:
        # Ridge along z: the gable faces the street.
        k = 0
        while True:
            xl, xr = -1 + k, w - k
            y = y0 + k
            if xl > xr:
                break
            for z in range(zf - 1, zb + 2):
                if xl == xr:
                    v.set(xl, y, z, S(slab, type="bottom", waterlogged="false"))
                else:
                    v.set(xl, y, z, S(stairs, facing="east", half="bottom", waterlogged="false"))
                    v.set(xr, y, z, S(stairs, facing="west", half="bottom", waterlogged="false"))
            for x in range(xl + 1, xr):
                for z in (zf, zb):
                    v.set(x, y, z, log(st["timber"]) if x % 3 == 0 or x == w // 2 and k >= 2 else plaster)
            if xr == xl + 1:
                for z in range(zf - 1, zb + 2):
                    v.set(xl, y + 1, z, S(slab, type="bottom", waterlogged="false"))
                    v.set(xr, y + 1, z, S(slab, type="bottom", waterlogged="false"))
                break
            k += 1
        if w >= 7:
            v.set(w // 2, y0 + 1, zf, S("glass_pane"))
            v.set(w // 2, y0 + 2, zf, S("glass_pane"))


def chimney(p, rng, st, w, d, zf, zb, roof_y, gable_front):
    v = p.v
    cx = 1 if rng.random() < 0.5 else w - 2
    cz = d - 2
    material = "bricks" if rng.random() < 0.5 else "cobblestone"
    top = roof_y + (max(w, zb - zf) // 2) + 2
    for y in range(1, top + 1):
        v.set(cx, y, cz, S(material))
    v.set(cx, top + 1, cz, S("campfire", facing="north", lit="true", signal_fire="false", waterlogged="false"))
    # A fireplace below.
    v.set(cx, 1, cz - 1, S("air"))
    v.set(cx, 0, cz - 1, S("netherrack") if False else S(material))


def furnish(p, rng, st, w, d, purpose, door_x):
    v = p.v
    wood = st["wood"]
    inner = [(x, z) for x in range(1, w - 1) for z in range(1, d - 1)]
    # Keep the door path, ladder and fireplace clear.
    if purpose == "shop" or purpose == "bakery":
        # A counter across the room with the shopkeeper behind it.
        cz = 3 if d >= 8 else 2
        for x in range(1, w - 1):
            if x != w - 2:
                v.set(x, 1, cz, S(f"{wood}_slab" if wood != "dark_oak" else "dark_oak_slab", type="top", waterlogged="false"))
            v.set(x, 1, cz - 1, S("air"))
        v.set(w - 2, 1, cz, S(f"{wood}_fence_gate", facing="east", in_wall="false", open="false", powered="false"))
        p.spot("shopkeeper", door_x, 1, cz + 1)
        for x in range(1, w - 1):
            if x not in (door_x,):
                v.set(x, 1, d - 2, S("barrel", facing="up", open="false"))
                if x % 2 == 0:
                    v.set(x, 2, d - 2, S("barrel", facing="up", open="false"))
        if purpose == "bakery":
            v.set(1, 1, d - 3, S("smoker", facing="east", lit="true"))
            v.set(1, 2, d - 3, S("bricks"))
    elif purpose == "tavern":
        for z in range(3, d - 2, 3):
            for x in range(2, w - 2, 4):
                v.set(x, 1, z, S(f"{wood}_fence"))
                v.set(x, 2, z, S(f"{wood}_pressure_plate", powered="false"))
                v.set(x - 1, 1, z, S(f"{wood}_stairs", facing="east", half="bottom"))
                v.set(x + 1, 1, z, S(f"{wood}_stairs", facing="west", half="bottom"))
        for x in range(1, w - 2):
            v.set(x, 1, d - 2, S("barrel", facing="north", open="false"))
        p.spot("innkeeper", w // 2, 1, d - 3)
    else:
        v.set(1, 1, 1 if door_x != 1 else d - 3, S("crafting_table"))
        v.set(w - 2, 1, 1 if door_x != w - 2 else 2, S("barrel", facing="up", open="false"))
        v.set(2, 1, d - 2, S("furnace", facing="north", lit="false"))
        tx, tz = w // 2, d // 2
        v.set(tx, 1, tz, S(f"{wood}_fence"))
        v.set(tx, 2, tz, S(f"{wood}_pressure_plate", powered="false"))
        v.set(tx - 1, 1, tz, S(f"{wood}_stairs", facing="east", half="bottom"))
        v.set(1, 1, d - 3, S("bookshelf"))
        v.set(1, 2, d - 3, S("potted_red_tulip" if rng.random() < 0.5 else "potted_fern"))
    # Upstairs: a bed, a chest, a rug.
    if True:
        bx, bz = 1, d - 3
        bed = rng.choice(["red", "blue", "green", "brown", "cyan", "yellow"])
        v.set(bx, 6, bz, S(f"{bed}_bed", facing="south", occupied="false", part="foot"))
        v.set(bx, 6, bz + 1, S(f"{bed}_bed", facing="south", occupied="false", part="head"))
        v.set(w - 2, 6, 1, S("chest", facing="south", type="single", waterlogged="false"))
        carpet = rng.choice(["red", "brown", "green", "light_gray"])
        for x in range(2, w - 2):
            for z in range(1, min(4, d - 1)):
                if v.get(x, 6, z) is None:
                    v.set(x, 6, z, S(f"{carpet}_carpet"))


# ---------------------------------------------------------------- market stall

def stall(rng, w=5, d=4):
    """A market stall: four posts, a counter at the front, crates behind, and a striped wool awning (a decor site,
    its stripes change colour with the seasons)."""
    p = Piece(w, d)
    v = p.v
    wood = rng.choice(["spruce", "oak", "dark_oak"])
    for x, z in ((0, 0), (w - 1, 0), (0, d - 1), (w - 1, d - 1)):
        for y in range(1, 4):
            v.set(x, y, z, S(f"{wood}_fence"))
    for x in range(1, w - 1):
        v.set(x, 1, 0, S(f"{wood}_slab", type="top", waterlogged="false"))
        v.set(x, 1, d - 1, S("barrel", facing="up", open="false"))
    v.set(1, 2, 0, S("potted_cactus") if rng.random() < 0.3 else S("lantern", hanging="false"))
    # Awning: stripes across the stall, sloping down to the front.
    blocks = []
    for x in range(-1, w + 1):
        for z in range(0, d):
            y = 4 if z >= 1 else 4
            blocks.append((x, y, z, (x + 1) % 2))
    # The front edge one lower (a valance).
    for x in range(-1, w + 1):
        blocks.append((x, 3, -1, (x + 1) % 2))
    p.site("awning", "north", blocks)
    p.spot("shopkeeper", w // 2, 1, 1)
    return p


# ---------------------------------------------------------------- lamp post and planter

def lamp_post(height=3):
    p = Piece(1, 1)
    for y in range(1, height + 1):
        p.v.set(0, y, 0, S("dark_oak_fence"))
    p.site("lamp", "north", [(0, height + 1, 0, 0)])
    return p


# ---------------------------------------------------------------- fountain

def fountain():
    """A round stone fountain, nine across: a basin of water and a column with a bowl of water on top. The bowl's own
    rim holds its water: water spilling from it would spread over the basin's rim and flood the square (it did in CI)."""
    n = 9
    p = Piece(n, n)
    v = p.v
    c = n // 2
    for x in range(n):
        for z in range(n):
            r = math.hypot(x - c, z - c)
            if r <= 4.3:
                v.set(x, 0, z, S("polished_andesite") if r > 3.3 else S("stone_bricks"))
            if 3.3 < r <= 4.3:
                # Full blocks only: a waterloggable rim block (a wall, a slab) beside two of the basin's sources would
                # be filled by the game's infinite-water rule and spill out over the square (it did in CI).
                v.set(x, 1, z, S("chiseled_stone_bricks") if (x + z) % 2 else S("stone_bricks"))
            elif r <= 3.3:
                v.set(x, 1, z, S("water", level="0"))
                v.set(x, 0, z, S("stone_bricks"))
                v.set(x, -1, z, S("stone_bricks"))
    for y in range(1, 4):
        v.set(c, y, c, S("chiseled_stone_bricks") if y == 2 else S("stone_bricks"))
    for dx, dz, facing in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
        v.set(c + dx, 3, c + dz, S("stone_brick_stairs", facing=facing, half="top"))
    v.set(c, 4, c, S("water", level="0"))
    for dx, dz, facing in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
        v.set(c + dx, 4, c + dz, S("stone_brick_stairs", facing=facing, half="bottom"))
    return p


# ---------------------------------------------------------------- church

def church(rng):
    """The church: a nave with aisles and a transept, an apse, two west towers with tall spires and a rose window over
    the portal, buttresses and tall stained-glass windows; inside, pews, an altar and chandeliers."""
    W, L = 25, 46
    p = Piece(W, L)
    v = p.v
    wall = lambda: mix(rng, "stone_bricks")
    trim = S("polished_andesite")
    roof = "deepslate_tile"
    nave_x0, nave_x1 = 7, 17           # nave walls (inner nave 8..16)
    aisle_h, nave_h = 11, 20
    tower = 7
    body_z0, body_z1 = 0, 38           # nave body; apse beyond
    trans_z0, trans_z1 = 26, 34
    # Floor.
    for x in range(-5, W + 5):
        for z in range(0, L):
            inside = in_church(x, z, W, L)
            if inside:
                v.set(x, 0, z, S("polished_andesite") if (x + z) % 2 else S("stone_bricks"))
    # Aisle walls and nave walls.
    for z in range(tower, body_z1 + 1):
        for y in range(1, aisle_h + 1):
            v.set(0, y, z, wall())
            v.set(W - 1, y, z, wall())
        for y in range(aisle_h + 1, nave_h + 1):
            v.set(nave_x0, y, z, wall())
            v.set(nave_x1, y, z, wall())
    # Arcade: pillars between nave and aisles every 4.
    for z in range(tower + 2, body_z1, 4):
        for x in (nave_x0, nave_x1):
            for y in range(1, aisle_h + 1):
                v.set(x, y, z, S("polished_andesite") if y < aisle_h else S("chiseled_stone_bricks"))
    for z in range(tower, body_z1 + 1):
        for x in (nave_x0, nave_x1):
            v.set(x, aisle_h, z, S("stone_bricks"))
    # Aisle roofs (lean-to) and ceilings.
    for z in range(tower - 1, body_z1 + 2):
        for i, x in enumerate(range(-1, nave_x0)):
            v.set(x, aisle_h + 1 + i // 2 if False else aisle_h + 1, z, S(f"{roof}_stairs", facing="east", half="bottom")) if x == -1 else None
        for x in range(0, nave_x0):
            y = aisle_h + 1 + (x // 2)
            if y <= nave_h:
                v.set(x, y, z, S(f"{roof}_stairs", facing="east", half="bottom") if x % 2 == 0 else S(f"{roof}_slab", type="top"))
        for x in range(nave_x1 + 1, W):
            y = aisle_h + 1 + ((W - 1 - x) // 2)
            v.set(x, y, z, S(f"{roof}_stairs", facing="west", half="bottom") if (W - 1 - x) % 2 == 0 else S(f"{roof}_slab", type="top"))
    # Transept walls.
    for z in range(trans_z0, trans_z1 + 1):
        for y in range(1, nave_h + 1):
            v.set(-4, y, z, wall())
            v.set(W + 3, y, z, wall())
    for x in range(-4, W + 4):
        if 0 <= x < W and nave_x0 <= x <= nave_x1:
            continue
        for y in range(1, nave_h + 1):
            v.set(x, y, trans_z0, wall()) if (x < 0 or x >= W) or y > aisle_h else None
            v.set(x, y, trans_z1, wall()) if (x < 0 or x >= W) or y > aisle_h else None
    # Clear the transept's inside (it crosses the aisles).
    for x in range(-3, W + 3):
        for z in range(trans_z0 + 1, trans_z1):
            for y in range(1, nave_h + 1):
                if (x, z) and not (x in (nave_x0, nave_x1) and False):
                    v.remove(x, y, z)
    # Apse: a half circle behind the nave.
    cx, cz = (nave_x0 + nave_x1) / 2, body_z1
    for x in range(nave_x0 - 1, nave_x1 + 2):
        for z in range(body_z1, L):
            r = math.hypot(x - cx, z - cz)
            if 4.4 <= r <= 5.5 and z > body_z1:
                for y in range(1, nave_h - 3):
                    v.set(x, y, z, wall())
            if r < 5.5 and z >= body_z1:
                v.set(x, 0, z, S("polished_andesite"))
    # Nave roof: a steep gable over the nave and over the transept arms.
    gable_roof(v, nave_x0 - 1, nave_x1 + 1, tower, body_z1 + 1, nave_h + 1, roof, along="z")
    gable_roof(v, -5, W + 4, trans_z0 - 1, trans_z1 + 1, nave_h + 1, roof, along="x")
    # Apse roof: a stepped cone.
    for k in range(0, 6):
        y = nave_h - 3 + k
        for x in range(nave_x0 - 2, nave_x1 + 3):
            for z in range(body_z1 + 1, L + 1):
                r = math.hypot(x - cx, z - cz)
                if 5.6 - k - 1 < r <= 5.6 - k:
                    v.set(x, y, z, S("deepslate_tiles" if roof == "deepslate_tile" else f"{roof}_tiles"))
    # West front: two towers with spires.
    for tx in (0, W - tower):
        for x in range(tx, tx + tower):
            for z in range(0, tower):
                edge = x in (tx, tx + tower - 1) or z in (0, tower - 1)
                for y in range(1, 31):
                    if edge:
                        corner = x in (tx, tx + tower - 1) and z in (0, tower - 1)
                        v.set(x, y, z, trim if corner and y % 6 == 0 else wall())
                if x not in (tx, tx + tower - 1) and z not in (0, tower - 1):
                    for y in (10, 20, 30):
                        v.set(x, y, z, S("stone_bricks"))
        # Belfry openings and a bell.
        for y in (25, 26, 27):
            for x in (tx + 2, tx + 4):
                v.remove(x, y, 0)
                v.remove(x, y, tower - 1)
            for z in (2, 4):
                v.remove(tx, y, z)
                v.remove(tx + tower - 1, y, z)
        v.set(tx + 3, 27, 3, S("bell", attachment="ceiling", facing="north", powered="false"))
        v.set(tx + 3, 28, 3, S("stone_bricks"))
        # Corner pinnacles and the spire.
        for x, z in ((tx, 0), (tx + tower - 1, 0), (tx, tower - 1), (tx + tower - 1, tower - 1)):
            v.set(x, 31, z, S("stone_brick_wall"))
            v.set(x, 32, z, S("stone_brick_wall"))
        spire(v, tx, 0, tower, 31, roof)
        # Slit windows.
        for y in (5, 6, 14, 15):
            v.set(tx + 3, y, 0, S("glass_pane"))
    # Facade between the towers: the portal, the rose window, a gable.
    for x in range(tower, W - tower):
        for y in range(1, nave_h + 1):
            v.set(x, y, 0, wall())
    for k in range(0, 8):
        for x in range(nave_x0 - 1 + k + 1, nave_x1 + 1 - k):
            v.set(x, nave_h + 1 + k, 0, wall())
    v.set(W // 2, nave_h + 4, 0, S("yellow_stained_glass_pane"))
    v.set(W // 2, nave_h + 5, 0, S("yellow_stained_glass_pane"))
    # Portal: a pointed arch of dark oak doors, three wide.
    mid = W // 2
    for x in (mid - 1, mid, mid + 1):
        for y in range(1, 6):
            v.remove(x, y, 0)
    for dx, hinge in ((-1, "left"), (1, "right")):
        v.set(mid + dx, 1, 0, S("dark_oak_door", facing="north", half="lower", hinge=hinge, open="false", powered="false"))
        v.set(mid + dx, 2, 0, S("dark_oak_door", facing="north", half="upper", hinge=hinge, open="false", powered="false"))
    v.set(mid, 1, 0, S("air"))
    v.set(mid, 2, 0, S("air"))
    for y in (3, 4, 5):
        for x in (mid - 1, mid, mid + 1):
            v.set(x, y, 0, S("dark_oak_planks") if y < 5 or x == mid else S("stone_brick_stairs", facing="east" if x < mid else "west", half="top"))
    v.set(mid, 3, 0, S("air"))
    v.set(mid, 4, 0, S("air"))
    for y in range(1, 7):
        v.set(mid - 2, y, -1, S("polished_andesite") if y < 6 else S("stone_brick_stairs", facing="east", half="top"))
        v.set(mid + 2, y, -1, S("polished_andesite") if y < 6 else S("stone_brick_stairs", facing="west", half="top"))
    v.set(mid - 1, 7, -1, S("stone_brick_stairs", facing="east", half="top"))
    v.set(mid + 1, 7, -1, S("stone_brick_stairs", facing="west", half="top"))
    v.set(mid, 7, -1, S("chiseled_stone_bricks"))
    v.set(mid, 8, -1, S("stone_brick_wall"))
    # Rose window.
    colours = ["blue", "light_blue", "yellow", "red", "magenta", "cyan"]
    for x in range(mid - 3, mid + 4):
        for y in range(9, 16):
            r = math.hypot(x - mid, y - 12)
            if r <= 3.2:
                if r < 1:
                    v.set(x, y, 0, S("yellow_stained_glass_pane"))
                else:
                    angle = int((math.atan2(y - 12, x - mid) + math.pi) / (2 * math.pi) * 8) % 2
                    v.set(x, y, 0, S(f"{colours[angle + (2 if r > 2.3 else 0)]}_stained_glass_pane"))
            elif r <= 3.9:
                v.set(x, y, -1, S("polished_andesite"))
    # Tall aisle and clerestory windows, buttresses between.
    for z in range(tower + 1, body_z1, 4):
        if trans_z0 - 1 <= z <= trans_z1 + 1:
            continue
        for x, out, facing in ((0, -1, "east"), (W - 1, 1, "west")):
            for y in range(3, 9):
                glass = "light_blue" if y < 6 else "blue"
                v.set(x, y, z, S(f"{glass}_stained_glass_pane"))
            v.set(x, 9, z, S("stone_brick_stairs", facing="north", half="top"))
        for x in (nave_x0, nave_x1):
            for y in range(aisle_h + 3, nave_h - 1):
                v.set(x, y, z, S("yellow_stained_glass_pane" if y % 2 else "orange_stained_glass_pane"))
        bz = z + 2
        if bz < body_z1 and not trans_z0 - 1 <= bz <= trans_z1 + 1:
            for x, out, facing in ((-1, -1, "east"), (W, 1, "west")):
                for y in range(1, 8):
                    v.set(x, y, bz, mix(rng, "stone_bricks"))
                v.set(x, 8, bz, S("stone_brick_stairs", facing=facing, half="bottom"))
                v.set(x + out, 1, bz, mix(rng, "stone_bricks"))
                v.set(x + out, 2, bz, mix(rng, "stone_bricks"))
                v.set(x + out, 3, bz, S("stone_brick_stairs", facing=facing, half="bottom"))
    # Transept end windows (big lancets).
    for x, out in ((-4, -1), (W + 3, 1)):
        for y in range(4, 15):
            for z in (trans_z0 + 3, trans_z0 + 5):
                v.set(x, y, z, S("purple_stained_glass_pane" if y > 11 else "light_blue_stained_glass_pane"))
    # Inside: pews, a red carpet up the middle, the altar, chandeliers.
    for z in range(tower + 2, trans_z0 - 1, 2):
        for x in range(nave_x0 + 1, nave_x1):
            if abs(x - mid) <= 1:
                continue
            v.set(x, 1, z, S("spruce_stairs", facing="south", half="bottom"))
    for z in range(1, body_z1 + 3):
        v.set(mid, 1, z, S("red_carpet")) if v.get(mid, 1, z) is None and z >= tower else None
    for x in range(mid - 2, mid + 3):
        v.set(x, 1, body_z1 + 1, S("polished_andesite_slab", type="bottom", waterlogged="false"))
    v.set(mid, 1, body_z1 + 2, S("chiseled_quartz_block"))
    v.set(mid - 1, 1, body_z1 + 2, S("quartz_stairs", facing="east", half="top"))
    v.set(mid + 1, 1, body_z1 + 2, S("quartz_stairs", facing="west", half="top"))
    v.set(mid - 1, 2, body_z1 + 2, S("candle", candles="3", lit="true", waterlogged="false"))
    v.set(mid + 1, 2, body_z1 + 2, S("candle", candles="3", lit="true", waterlogged="false"))
    v.set(mid, 2, body_z1 + 2, S("gold_block"))
    p.spot("priest", mid, 1, body_z1)
    for z in range(tower + 4, body_z1, 8):
        for y in range(nave_h - 4, nave_h + 1):
            v.set(mid, y, z, S("iron_chain", axis="y", waterlogged="false"))
        v.set(mid, nave_h - 5, z, S("lantern", hanging="true"))
        for x in (nave_x0 + 1, nave_x1 - 1):
            v.set(x, aisle_h - 1, z, S("lantern", hanging="true")) if v.get(x, aisle_h, z) else None
    for x in (1, W - 2):
        for z in range(tower + 3, body_z1, 6):
            v.set(x, aisle_h, z, S("stone_bricks"))
            v.set(x, aisle_h - 1, z, S("lantern", hanging="true"))
    # Seasonal decor at the portal (two planters either side of the steps).
    p.site("planter", "north", [(mid - 3, 1, -2, 0)])
    p.site("planter", "north", [(mid + 3, 1, -2, 0)])
    p.site("banner", "north", [(mid - 4, 9, -1, 0)])
    p.site("banner", "north", [(mid + 4, 9, -1, 0)])
    p.door = (mid, -1)
    return p


def in_church(x, z, W, L):
    if 0 <= x < W and 0 <= z <= 38:
        return True
    if -4 <= x <= W + 3 and 26 <= z <= 34:
        return True
    return math.hypot(x - 12, z - 38) < 5.5 and z >= 38


def gable_roof(v, x0, x1, z0, z1, y0, roof, along):
    """A 45-degree gable over the box x0..x1, z0..z1 (inclusive), ridge running `along` x or z."""
    stairs, slab = f"{roof}_stairs", f"{roof}_slab"
    if along == "z":
        k = 0
        while x0 + k <= x1 - k:
            xl, xr = x0 + k, x1 - k
            y = y0 + k
            for z in range(z0, z1 + 1):
                if xl == xr:
                    v.set(xl, y, z, S(slab, type="bottom", waterlogged="false"))
                else:
                    v.set(xl, y, z, S(stairs, facing="east", half="bottom", waterlogged="false"))
                    v.set(xr, y, z, S(stairs, facing="west", half="bottom", waterlogged="false"))
                    for x in range(xl + 1, xr):
                        if z in (z0, z1):
                            v.set(x, y, z, S("stone_bricks"))
            k += 1
    else:
        k = 0
        while z0 + k <= z1 - k:
            zl, zr = z0 + k, z1 - k
            y = y0 + k
            for x in range(x0, x1 + 1):
                if zl == zr:
                    v.set(x, y, zl, S(slab, type="bottom", waterlogged="false"))
                else:
                    v.set(x, y, zl, S(stairs, facing="south", half="bottom", waterlogged="false"))
                    v.set(x, y, zr, S(stairs, facing="north", half="bottom", waterlogged="false"))
                    for z in range(zl + 1, zr):
                        if x in (x0, x1):
                            v.set(x, y, z, S("stone_bricks"))
            k += 1


def spire(v, x0, z0, size, y0, roof):
    """A slender spire on a square tower of `size`: five across for six blocks, three across for six, then a needle,
    each step dressed with stairs, a lightning rod on top. Returns the height reached."""
    stairs = f"{roof}_stairs"
    tiles = f"{roof}s" if roof.endswith("tile") else f"{roof}_tiles"
    c = size // 2
    y = y0
    for half, rise in ((2, 6), (1, 6)):
        for k in range(rise):
            for dx in range(-half, half + 1):
                for dz in range(-half, half + 1):
                    edge = abs(dx) == half or abs(dz) == half
                    if k == rise - 1 and edge:
                        facing = "east" if dx == -half else "west" if dx == half else "south" if dz == -half else "north"
                        v.set(x0 + c + dx, y, z0 + c + dz, S(stairs, facing=facing, half="bottom", waterlogged="false"))
                    else:
                        v.set(x0 + c + dx, y, z0 + c + dz, S(tiles))
            y += 1
    for k in range(4):
        v.set(x0 + c, y, z0 + c, S(f"{roof}_wall"))
        y += 1
    v.set(x0 + c, y, z0 + c, S("lightning_rod", facing="up", powered="false", waterlogged="false"))
    return y + 1


# ---------------------------------------------------------------- gatehouse

def gatehouse(rng):
    """A gatehouse: two square towers flanking an arched passage five wide, a guardroom over it, a portcullis drawn up
    under the arch, crenellations and tall roofs; banners (decor sites) hang on the outer face. The outside is north."""
    W, D = 17, 9
    p = Piece(W, D)
    v = p.v
    roof = "deepslate_tile"
    wall = lambda: mix(rng, "stone_bricks")
    for x in range(W):
        for z in range(D):
            v.set(x, 0, z, S("stone_bricks"))
    # Towers.
    for tx in (0, W - 6):
        for x in range(tx, tx + 6):
            for z in range(0, D):
                edge = x in (tx, tx + 5) or z in (0, D - 1)
                for y in range(1, 17):
                    if edge:
                        v.set(x, y, z, wall())
                if not edge:
                    for y in (6, 11, 16):
                        v.set(x, y, z, S("spruce_planks"))
        for y in (4, 5, 9, 10, 14):
            v.set(tx + 2 + (1 if tx else 0), y, 0, S("iron_bars"))
        # Crenellations and a pyramid roof.
        for x in range(tx, tx + 6):
            for z in range(0, D):
                if x in (tx, tx + 5) or z in (0, D - 1):
                    if (x + z) % 2 == 0:
                        v.set(x, 17, z, wall())
        for x in range(tx, tx + 6):
            v.set(x, 16, -1, S("stone_brick_stairs", facing="south", half="top"))
        for x in range(tx + 1, tx + 5):
            for z in range(1, D - 1):
                v.set(x, 16, z, S("stone_bricks"))
    # The middle: passage with an arch, the guardroom above.
    for x in range(6, W - 6):
        for z in range(0, D):
            for y in range(1, 13):
                passage = 6 <= x <= W - 7 and y <= 6
                if not passage and z in (0, D - 1):
                    v.set(x, y, z, wall())
            v.set(x, 7, z, S("stone_bricks"))
            v.set(x, 12, z, S("spruce_planks"))
    for z in (0, D - 1):
        v.set(6, 6, z, S("stone_brick_stairs", facing="west", half="top"))
        v.set(W - 7, 6, z, S("stone_brick_stairs", facing="east", half="top"))
        for x in range(7, W - 7):
            v.set(x, 6, z, S("stone_bricks") if x in (7, W - 8) and False else S("air"))
    gable_roof(v, 6, W - 7, -1, D, 13, roof, along="x")
    # Portcullis drawn up into the arch.
    for x in range(6, W - 6):
        v.set(x, 6, 1, S("iron_bars"))
        v.set(x, 5, 1, S("iron_bars"))
    # Passage floor and lanterns.
    for x in range(6, W - 6):
        for z in range(0, D):
            v.set(x, 0, z, S("cobblestone") if (x + z) % 3 else S("stone_bricks"))
    v.set(6, 4, 4, S("lantern", hanging="false")) if False else None
    v.set(W // 2, 6, 4, S("lantern", hanging="true"))
    # Guardroom windows.
    for x in (W // 2 - 1, W // 2 + 1):
        v.set(x, 9, 0, S("iron_bars"))
        v.set(x, 10, 0, S("iron_bars"))
    # Banners on the towers' outer faces.
    p.site("gate_banner", "north", [(2, 12, -1, 0), (3, 12, -1, 1)])
    p.site("gate_banner", "north", [(W - 4, 12, -1, 1), (W - 3, 12, -1, 0)])
    p.spot("guard", 5, 1, -2)
    p.spot("guard", W - 6, 1, -2)
    return p


def spire_rect(v, x0, z0, x1, z1, y0, roof):
    stairs = f"{roof}_stairs"
    y = y0
    while x0 <= x1 and z0 <= z1:
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    if x0 == x1 or z0 == z1:
                        v.set(x, y, z, S(f"{roof}_slab", type="bottom", waterlogged="false"))
                    else:
                        facing = "east" if x == x0 else "west" if x == x1 else "south" if z == z0 else "north"
                        v.set(x, y, z, S(stairs, facing=facing, half="bottom", waterlogged="false"))
        x0, x1, z0, z1 = x0 + 1, x1 - 1, z0 + 1, z1 - 1
        y += 1


# ---------------------------------------------------------------- bank

def bank(rng):
    """The Jugs bank: a stone counting house with columns, a tiled hip-less roof and the town's ATMs in its front."""
    W, D = 13, 11
    p = Piece(W, D)
    v = p.v
    for x in range(W):
        for z in range(D):
            v.set(x, 0, z, S("polished_andesite") if x in (0, W - 1) or z in (0, D - 1) else
                  S("stone_bricks") if (x + z) % 2 else S("polished_andesite"))
    for y in range(1, 8):
        for x in range(W):
            for z in (0, D - 1):
                v.set(x, y, z, S("smooth_stone") if y in (1, 7) else S("stone_bricks"))
        for z in range(D):
            for x in (0, W - 1):
                v.set(x, y, z, S("smooth_stone") if y in (1, 7) else S("stone_bricks"))
    # Columns across the front.
    for x in range(1, W - 1, 3):
        for y in range(1, 7):
            v.set(x, y, -1, S("quartz_pillar", axis="y"))
        v.set(x, 7, -1, S("chiseled_quartz_block"))
    for x in range(0, W):
        v.set(x, 8, -1, S("smooth_stone_slab", type="bottom", waterlogged="false"))
    # Double door.
    mid = W // 2
    for x, hinge in ((mid - 1, "left"), (mid, "right")):
        v.set(x, 1, 0, S("iron_door", facing="north", half="lower", hinge=hinge, open="true", powered="false"))
        v.set(x, 2, 0, S("iron_door", facing="north", half="upper", hinge=hinge, open="true", powered="false"))
    # ATMs set into the front wall, facing out, and two more inside.
    for x in (2, W - 3):
        p.atms.append([x, 1, 0, "north"])
    for x in (2, W - 3):
        p.atms.append([x, 1, D - 1, "north"])
    # Windows.
    for x in (3, W - 4):
        for y in (3, 4, 5):
            v.set(x, y, 0, S("glass_pane"))
    # Roof: a low tiled pyramid.
    spire_rect(v, -1, -2, W, D, 8, "deepslate_tile")
    for x in range(1, W - 1):
        for z in range(1, D - 1):
            v.set(x, 7, z, S("stone_bricks"))
    for z in (3, 7):
        v.set(mid, 6, z, S("lantern", hanging="true"))
    # A counter for the banker.
    for x in range(2, W - 2):
        v.set(x, 1, D - 4, S("polished_andesite_slab", type="top", waterlogged="false"))
    p.spot("banker", mid, 1, D - 3)
    p.site("banner", "north", [(1, 6, -2, 0)])
    p.site("banner", "north", [(W - 2, 6, -2, 0)])
    p.door = (mid, -2)
    return p


# ---------------------------------------------------------------- town hall

def town_hall(rng):
    """The town hall: a long timber hall on a stone arcade with a clock tower over the door."""
    st = style(rng, plaster="calcite", timber="dark_oak_log", wood="dark_oak", roof="resin_brick", base="stone_bricks",
               stone_ground=True, floor="spruce_planks", door="dark_oak", shutter="dark_oak")
    p = house(rng, 15, 11, storeys=3, purpose="hall", gable_front=False, st=st)
    v = p.v
    # Clock tower over the door.
    mid = 15 // 2
    top = 20
    for y in range(14, top + 1):
        for x in range(mid - 1, mid + 2):
            for z in range(-3, 0):
                edge = x in (mid - 1, mid + 1) or z in (-3, -1)
                if edge:
                    v.set(x, y, z, S("stone_bricks"))
    v.set(mid, 17, -3, S("chiseled_stone_bricks"))
    v.set(mid, 18, -4, S("bell", attachment="floor", facing="north", powered="false")) if False else None
    spire_rect(v, mid - 2, -4, mid + 2, 0, top + 1, "deepslate_tile")
    p.spot("mayor", mid, 1, 5)
    return p
