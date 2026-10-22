"""Architectural finish for the existing town pieces, in their original local frames.

Keep footprints, resident anchors and seasonal site order stable. These edits are
compiled into town.json.gz. Existing saves use the explicit versioned TownUpgrades migration, never regeneration.
"""
import math

from town_voxels import S, block_name


def church(p):
    v = p.v
    # Keep wear near the ground rather than speckling the entire cathedral.
    for (x, y, z), state in list(v.b.items()):
        if y > 3 and block_name(state) in ("mossy_stone_bricks", "cracked_stone_bricks"):
            v.set(x, y, z, S("stone_bricks"))

    # Replace intersecting gables with one exterior roof surface. The previous
    # nave began seven blocks behind its facade and left the vestibule open.
    for x, y, z in list(v.b):
        tower = 0 <= z <= 6 and (0 <= x <= 6 or 18 <= x <= 24)
        if y >= 21 and not tower:
            v.remove(x, y, z)
    roof = {}
    for x in range(6, 19):
        for z in range(-1, 40):
            roof[x, z] = (21 + min(x - 6, 18 - x), "east" if x < 12 else "west", x == 12)
    for x in range(-5, 30):
        for z in range(25, 36):
            candidate = (21 + min(z - 25, 35 - z), "south" if z < 30 else "north", z == 30)
            if candidate[0] > roof.get((x, z), (-1,))[0]:
                roof[x, z] = candidate
    for (x, z), (y, facing, ridge) in roof.items():
        if 0 <= z <= 6 and x in (6, 18):
            continue  # tower masonry owns the contact with the nave
        v.set(x, y, z, S("deepslate_tile_slab", type="bottom") if ridge else
              S("deepslate_tile_stairs", facing=facing, half="bottom"))
        # Fascia stays below the eaves, not floating beside the roof.
        if y == 21:
            v.set(x, 20, z, S("stone_brick_stairs", facing=facing, half="top"))
    # Closed front/back and transept end gables, with recessed lancets.
    for z in (0, 38):
        for x in range(7, 18):
            for y in range(17 if z == 38 else 21, roof[x, z][0]):
                v.set(x, y, z, S("stone_bricks"))
    for x in (-4, 28):
        for z in range(26, 35):
            for y in range(21, roof[x, z][0]):
                v.set(x, y, z, S("stone_bricks"))
        v.set(x, 22, 30, S("blue_stained_glass"))
        v.set(x, 23, 30, S("light_blue_stained_glass"))

    # Enclose the two rear aisles. Previously both ended in an open wall-sized gap.
    for a, b in ((0, 7), (17, 24)):
        v.box(a, 1, 38, b, 11, 38, S("stone_bricks"))
        for x in range(a, b + 1):
            underside = 12 + min(x, 24 - x) // 2
            for y in range(12, underside):
                v.set(x, y, 38, S("stone_bricks"))
        for x in range(a + 2, b - 1):
            for y in range(4, 8):
                v.set(x, y, 38, S("light_blue_stained_glass"))

    # A continuous half-apse: choose its shell by neighbouring columns, avoiding
    # the diagonal leaks that a thin mathematical circle leaves in a voxel grid.
    apse = {(x, z) for x in range(6, 19) for z in range(39, 45)
            if math.hypot(x - 12, z - 38) <= 5.5}
    for x in range(6, 19):
        for z in range(39, 46):
            for y in range(3, 24):
                v.remove(x, y, z)
    for x, z in sorted(apse):
        edge = any((x + dx, z + dz) not in apse and z + dz > 38
                   for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            for y in range(1, 17):
                v.set(x, y, z, S("polished_andesite") if y in (1, 16) else S("stone_bricks"))
            if x in (9, 12, 15):
                for y in range(6, 12):
                    v.set(x, y, z, S("blue_stained_glass" if y in (6, 11) else "light_blue_stained_glass"))
        y = 17 + max(0, 5 - math.ceil(math.hypot(x - 12, z - 38)))
        v.set(x, y, z, S("deepslate_tiles"))

    # Arcades: stepped springers make the columns read as arches, not poles.
    for x in (7, 17):
        for z in range(9, 38, 4):
            if 26 <= z <= 34:
                continue
            for dz, facing in ((-1, "south"), (1, "north")):
                v.set(x, 10, z + dz, S("stone_brick_stairs", facing=facing, half="top"))
            v.set(x, 9, z, S("chiseled_stone_bricks"))
            inner_x = x + (1 if x == 7 else -1)
            v.set(inner_x, 4, z, S("stone_brick_stairs", facing="east" if x == 7 else "west", half="top"))
            v.set(inner_x, 3, z, S("lantern", hanging="true"))
    # Roof-height chains, with timber ties spanning the nave underneath.
    for z in (11, 19, 27, 35):
        v.box(8, 20, z, 16, 20, z, S("dark_oak_log", axis="x"))
        v.box(12, 16, z, 12, 26, z, S("iron_chain", axis="y", waterlogged="false"))
        v.set(12, 15, z, S("lantern", hanging="true"))
    # Supported aisle lights: horizontal brackets from the outer walls.
    for x, direction in ((1, "east"), (23, "west")):
        for z in range(10, 38, 6):
            if 26 < z < 34:
                # The transept opens the side wall here: suspend from its roof
                # instead of leaving a wall bracket floating in the crossing.
                for y in range(10, roof[x, z][0]):
                    v.set(x, y, z, S("iron_chain", axis="y", waterlogged="false"))
            else:
                v.set(x, 10, z, S("stone_brick_stairs", facing=direction, half="top"))
    # Calm floor, broad central runner, stone border and cross-aisle.
    for (x, y, z), state in list(v.b.items()):
        if y == 0:
            v.set(x, y, z, S("polished_andesite" if x in (7, 10, 14, 17) or z in (7, 26, 34) else "smooth_stone"))
    for z in range(1, 38):
        for x in range(11, 14):
            v.set(x, 1, z, S("red_carpet"))

    # Two usable doors and a glazed transom in a deeply framed portal.
    for x in range(11, 14):
        v.box(x, 1, 0, x, 5, 0, S("dark_oak_planks"))
    for x, hinge in ((11, "left"), (12, "right")):
        for y, half in ((1, "lower"), (2, "upper")):
            v.set(x, y, 0, S("dark_oak_door", facing="north", half=half, hinge=hinge, open="false", powered="false"))
        v.set(x, 3, 0, S("light_blue_stained_glass"))
    # Stone tracery and a restrained blue/gold rose; solid glass avoids unjoined
    # panes at the stepped perimeter. Keep seasonal banners on either side free.
    for x in range(8, 17):
        for y in range(8, 17):
            radius = math.hypot(x - 12, y - 12)
            if radius <= 3.9:
                v.remove(x, y, -1)
                if radius >= 3:
                    v.set(x, y, -1, S("polished_andesite"))
                    v.set(x, y, 0, S("stone_bricks"))
                else:
                    glass = "yellow" if x == 12 and y == 12 else "light_blue" if (x + y) % 2 else "blue"
                    v.set(x, y, 0, S(f"{glass}_stained_glass"))
                    if (x == 12 or y == 12) and radius > 0:
                        v.set(x, y, -1, S("stone_brick_wall"))
    # Tower string courses, corner piers, recessed windows and accessible belfries.
    for tx in (0, 18):
        # Slender projecting piers give the facade actual relief from street level.
        for x in (tx, tx + 6):
            v.set(x, 0, -1, S("stone_bricks"))
            v.set(x, 1, -1, S("polished_andesite"))
            v.box(x, 2, -1, x, 21, -1, S("stone_brick_wall"))
            v.set(x, 22, -1, S("stone_brick_stairs", facing="south", half="bottom"))
        for y in (1, 10, 20, 24, 30):
            for x in range(tx, tx + 7):
                for z in (0, 6):
                    v.set(x, y, z, S("polished_andesite"))
            for z in range(7):
                for x in (tx, tx + 6):
                    v.set(x, y, z, S("polished_andesite"))
        for x in (tx, tx + 6):
            for z in (0, 6):
                v.box(x, 1, z, x, 30, z, S("polished_andesite"))
        for centre_y in (5, 14):
            for y in range(centre_y, centre_y + 3):
                v.set(tx + 3, y, 0, S("light_blue_stained_glass"))
            for dx, facing in ((-1, "east"), (1, "west")):
                v.set(tx + 3 + dx, centre_y + 3, 0, S("stone_brick_stairs", facing=facing, half="top"))
        v.box(tx + 3, 1, 6, tx + 3, 3, 6, S("air"))
        v.box(tx + 1, 1, 5, tx + 1, 29, 5, S("ladder", facing="north", waterlogged="false"))
        # A real corbelled base for the formerly narrow, abrupt spire.
        for x in range(tx, tx + 7):
            for z in range(7):
                if x in (tx, tx + 6) or z in (0, 6):
                    facing = "east" if x == tx else "west" if x == tx + 6 else "south" if z == 0 else "north"
                    v.set(x, 31, z, S("deepslate_tile_stairs", facing=facing, half="bottom"))
        # Bell hangs from a beam that reaches both tower walls.
        v.box(tx + 1, 28, 3, tx + 5, 28, 3, S("dark_oak_log", axis="x"))
    for x in range(7, 18):
        v.set(x, 18, -1, S("stone_brick_stairs", facing="south", half="top"))
    for y in (22, 23, 24):
        v.set(12, y, 0, S("light_blue_stained_glass"))
    # Three-block-deep stone threshold; no raised step obstructs the doors.
    v.box(9, 0, -2, 15, 0, -1, S("stone_bricks"))


def house(p, st, storeys, purpose):
    v, w, d = p.v, p.w, p.d
    # Roof edges contrast with the main tile without introducing another palette.
    for (x, y, z), state in list(v.b.items()):
        if y >= 11 and block_name(state) == f"{st['roof']}_stairs" and (x in (-1, w) or z in (-storeys, d)):
            v.set(x, y, z, state.replace(f"{st['roof']}_stairs", f"{st['wood']}_stairs"))
    # Every occupied storey has furnishings; keep the ladder column clear.
    for storey in range(2, storeys):
        y = 1 + 5 * storey
        v.set(1, y, d - 3, S("brown_bed", facing="south", part="foot", occupied="false"))
        v.set(1, y, d - 2, S("brown_bed", facing="south", part="head", occupied="false"))
        v.set(1, y, 1, S("bookshelf"))
        v.set(1, y + 1, 1, S("potted_fern"))
        v.set(w - 2, y, 1, S("barrel", facing="up", open="false"))
    # Shop identity is carried by its furnishings, not a random residential table.
    if purpose == "hall":
        v.set(w // 2, 1, d // 2, S("air"))
        v.set(w // 2, 2, d // 2, S("air"))
        for x in range(2, w - 2):
            v.set(x, 1, d - 3, S("dark_oak_slab", type="top"))
        v.set(2, 2, d - 3, S("lectern", facing="north", has_book="false", powered="false"))


def bank(p):
    v, w, d = p.v, p.w, p.d
    # Window bays and cornice give the blank counting-house walls some depth.
    for x in (0, w - 1):
        for z in (3, 7):
            for y in range(3, 6):
                v.set(x, y, z, S("light_gray_stained_glass"))
            v.set(x, 2, z, S("chiseled_stone_bricks"))
    for x in range(w):
        v.set(x, 7, 0, S("polished_andesite"))
    # Doors remain usable when neighbouring blocks update (iron doors could shut
    # permanently without any reachable switch).
    for x in (w // 2 - 1, w // 2):
        for y in (1, 2):
            v.set(x, y, 0, v.get(x, y, 0).replace("iron_door", "dark_oak_door"))
    v.box(0, 0, -2, w - 1, 0, -1, S("polished_andesite"))


def hall(p):
    v, mid = p.v, p.w // 2
    # The clock turret is supported from its portico, with a readable five-wide
    # dial rather than a single chiseled block pretending to be a clock.
    for x in (3, 12):
        v.box(x, 1, -3, x, 14, -3, S("dark_oak_log", axis="y"))
        v.set(x, 0, -3, S("stone_bricks"))
    v.box(3, 14, -3, 12, 14, -1, S("dark_oak_planks"))
    v.walls(mid - 2, 15, -3, mid + 2, 21, -1, S("stone_bricks"))
    for x in range(mid - 1, mid + 2):
        for y in range(17, 20):
            v.set(x, y, -3, S("calcite"))
    for x, y in ((mid, 18), (mid, 19), (mid + 1, 18)):
        v.set(x, y, -3, S("polished_blackstone"))
    for x in range(mid - 2, mid + 3):
        v.set(x, 16, -3, S("polished_andesite"))
        v.set(x, 20, -3, S("polished_andesite"))
