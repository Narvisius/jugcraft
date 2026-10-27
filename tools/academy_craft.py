"""Hand-fitted architectural and autumn detail for Frostward's three public rooms.

Fonta59's permission-cleared Hall and court supplied the *sectional grammar*:
several stepped slab/stair cornices, thick piers framing recessed bays, a high
annular gallery, and bookshelves that climb to the next occupied level. These
features are redrawn around Frostward's original rooms, routes and stations.
See docs/features/frostward-fonta59-adaptations.md for source bounds.
"""
from math import hypot, cos, sin, pi

from retro_game_shop import state


def _put(put, x, y, z, name, **props):
    put(x, y, z, state(name if ":" in name else "minecraft:" + name, **props))


def craft_elements_hall(cells, put):
    """A high teaching theatre with carved supports, layered roof and dais."""
    def b(x, y, z, name, **props):
        _put(put, x, y, z, name, **props)

    # The source Hall repeatedly changes section at Y118/130/132/135/141.
    # Frostward translates that rhythm to Y25/30/34/37/43, with deliberately
    # unequal north and south bays. Restrict changes to occupied masonry: the
    # existing windows, side stairs, exterior passages and magic benches stay.
    for z, width in ((16, 7), (48, 5)):
        for x in range(54, 83):
            if abs(x - 68) > width and (x - 68) % 7 in (-3, 3):
                for y in range(26, 35):
                    old = cells.get((x, y, z), ("minecraft:air",))[0]
                    if old in ("minecraft:stone_bricks", "minecraft:chiseled_stone_bricks"):
                        b(x, y, z, "polished_andesite" if y in (30, 34) else "cracked_stone_bricks")
        for x in range(56, 81):
            if abs(x - 68) <= width:
                continue
            old = cells.get((x, 35, z), ("minecraft:air",))[0]
            if old.startswith("minecraft:stone"):
                b(x, 35, z, "stone_brick_stairs", facing="south" if z < 33 else "north",
                  half="top", shape="straight", waterlogged="false")

    # Monumental columns are thick in plan and have bases, shafts, corbels and
    # capitals. The original four quartz sticks are recut into Nordic stone.
    for x in (54, 82):
        for z in (24, 43):
            for y in range(25, 36):
                b(x, y, z, "chiseled_stone_bricks" if y in (25, 26, 30, 34, 35)
                  else "polished_andesite")
            for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                for y in (26, 34):
                    b(x + dx, y, z + dz, "stone_brick_stairs",
                      facing="east" if dx < 0 else "west" if dx > 0 else "south" if dz < 0 else "north",
                      half="top" if y == 34 else "bottom", shape="straight", waterlogged="false")
            b(x, 36, z, "sea_lantern")

    # A nested, dark timber and stone octagon breaks the flat underside of the
    # old gallery. Lanterns hang in the coffers, while the central bridges
    # remain supported at Y38 and the teaching wells stay open.
    for x in range(57, 80):
        for z in range(22, 45):
            d = max(abs(x - 68), abs(z - 33))
            if d in (6, 11):
                b(x, 35 if d == 11 else 36, z, "polished_andesite" if (x + z) % 3 else "chiseled_stone_bricks")
            elif d == 10 and (x + z) % 4 == 0:
                b(x, 36, z, "dark_oak_planks")
    for spoke in range(12):
        angle = spoke * pi / 6
        for radius in range(4, 16):
            x = 68 + round(radius * cos(angle))
            z = 33 + round(radius * sin(angle))
            if (x, z) in ((54, 24), (54, 43), (82, 24), (82, 43)):
                continue
            b(x, 34 if radius < 11 else 35, z,
              "chiseled_stone_bricks" if radius in (6, 11, 15) else "polished_andesite")
    for x, z in ((61, 26), (75, 26), (61, 40), (75, 40),
                 (68, 22), (68, 44)):
        b(x, 35, z, "lantern", hanging="true", waterlogged="false")

    # A suspended five-point arcane luminary gives the vast central teaching
    # circle a focal object visible from both the ground and open gallery.
    # Its lowest block is Y30: four air blocks remain over the room anchor.
    for y in range(32, 36):
        b(68, y, 33, "iron_chain", axis="y", waterlogged="false")
    b(68, 31, 33, "chiseled_stone_bricks")
    b(68, 30, 33, "amethyst_block")
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        b(68 + dx // 2, 31, 33 + dz // 2, "polished_andesite")
        b(68 + dx, 31, 33 + dz, "sea_lantern")
        b(68 + dx, 32, 33 + dz, "chiseled_stone_bricks")

    # The north teaching end gets a broad, shallow stone dais and a stepped
    # focus. The middle floor, two Lampwright benches and Arctus remain free.
    for x in range(63, 74):
        for z in range(18, 25):
            if abs(x - 68) + abs(z - 21) <= 8:
                b(x, 25, z, "polished_deepslate" if z in (18, 24) or x in (63, 73)
                  else "polished_andesite")
    for x in range(65, 72):
        b(x, 25, 25, "polished_andesite_stairs", facing="south", half="bottom",
          shape="straight", waterlogged="false")
    for x in (64, 72):
        for z in (20, 23):
            b(x, 26, z, "chiseled_stone_bricks")
            b(x, 27, z, "lantern", hanging="false", waterlogged="false")
    for x in (65, 71):
        for y in range(26, 31):
            b(x, y, 17, "chiseled_stone_bricks" if y in (26, 30) else "polished_andesite")
    for x in range(66, 71):
        b(x, 30, 17, "polished_andesite")
    b(68, 26, 20, "enchanting_table")
    for x in (62, 74):
        for y in range(25, 33):
            b(x, y, 17, "chiseled_stone_bricks" if y in (25, 29, 32)
              else "polished_andesite")
    for x in range(63, 74):
        b(x, 33, 17, "stone_brick_stairs", facing="south", half="top",
          shape="straight", waterlogged="false")
    for x in (60, 76):
        for y in range(25, 31):
            b(x, y, 18, "bookshelf" if y not in (27, 30) else "dark_oak_planks")
        b(x, 31, 18, "lantern", hanging="false", waterlogged="false")
    for x, z in ((59, 20), (77, 20), (59, 46), (77, 46)):
        b(x, 25, z, "dark_oak_trapdoor", facing="north", half="top", open="false", powered="false", waterlogged="false")

    # Three curved seminar tiers turn the large southern floor into an actual
    # university teaching theatre. A five-wide center aisle and outer aisles
    # still connect the south portal, stations and north dais.
    for z, half_width in ((39, 7), (43, 9), (47, 10)):
        for x in range(68 - half_width, 69 + half_width):
            if 66 <= x <= 70:
                continue
            b(x, 25, z, "spruce_stairs", facing="north", half="bottom",
              shape="straight", waterlogged="false")
            b(x, 25, z - 1, "dark_oak_slab", type="bottom", waterlogged="false")
        for x in (68 - half_width, 68 + half_width):
            b(x, 26, z - 1, "lantern", hanging="false", waterlogged="false")
    for x, name in ((63, "amethyst_block"), (73, "copper_block")):
        b(x, 25, 28, "chiseled_stone_bricks")
        b(x, 26, 28, name)
        b(x, 27, 28, "end_rod", facing="up")

    # Three distinct exterior belts interrupt the sheer Hall drum. Restrict
    # these to cardinal frontages so the round wall does not gain a uniform
    # procedural stripe. South portal shoulders project into the court.
    for z in (51, 52, 53):
        for x in list(range(58, 63)) + list(range(74, 79)):
            if (x + z) % 2 == 0:
                b(x, 34, z, "stone_brick_stairs", facing="south", half="top",
                  shape="straight", waterlogged="false")
    for x in (59, 77):
        for z in (50, 51, 52, 53):
            for y in range(25, 46):
                b(x, y, z, "chiseled_stone_bricks" if y in (25, 30, 37, 44)
                  else "polished_andesite" if y % 5 == 0 else "stone_bricks")
            b(x, 46, z, "stone_brick_slab", type="bottom", waterlogged="false")
    for x in range(60, 77):
        for y in (37, 44, 52):
            if cells.get((x, y, 52), ("minecraft:air",))[0] != "minecraft:air":
                b(x, y, 52, "polished_andesite" if x % 3 else "chiseled_stone_bricks")

    # A three-deep Nordic stone overdoor gives the court-facing Hall a real
    # threshold. The sides descend into the old rock plinth; the five-wide
    # processional center is open from the floor through Y36. Its stepped
    # cornice interprets the donor's slab transitions without Gothic points.
    for x in (60, 76):
        for z in (53, 54):
            for y in range(16, 25):
                if cells.get((x, y, z), ("minecraft:air",))[0] == "minecraft:air":
                    b(x, y, z, "tuff" if y < 22 else "stone_bricks")
            for y in range(25, 43):
                b(x, y, z, "chiseled_stone_bricks" if y in (25, 30, 37, 42)
                  else "polished_andesite" if y % 5 == 0 else "stone_bricks")
            b(x, 43, z, "deepslate_tile_slab", type="bottom", waterlogged="false")
    for x in range(61, 76):
        y = 40 + min(x - 60, 76 - x) // 3
        for z in (53, 54):
            b(x, y, z, "chiseled_stone_bricks" if x in (64, 68, 72)
              else "polished_andesite")
            b(x, y + 1, z, "deepslate_tile_slab", type="bottom", waterlogged="false")
    for x in (62, 74):
        for y in range(29, 35):
            b(x, y, 53, "orange_stained_glass")
            b(x, y, 52, "sea_lantern")
        b(x, 35, 53, "stone_brick_stairs", facing="south", half="top",
          shape="straight", waterlogged="false")

    # The east Hall elevation overlooks the scholarly wing. Two deliberately
    # offset, deeply framed amber archive windows flank its older blue slot.
    # Their projecting jambs stand on the already solid Y40 wing roof.
    for z0 in (25, 41):
        for y in range(44, 55):
            b(85, y, z0, "orange_stained_glass" if 47 <= y <= 52
              else "chiseled_stone_bricks")
            if 47 <= y <= 52:
                b(84, y, z0, "sea_lantern")
            for side in (-1, 1):
                b(85, y, z0 + side, "chiseled_stone_bricks" if y in (44, 50, 54)
                  else "polished_andesite")
        for z in range(z0 - 2, z0 + 3):
            b(85, 55, z, "deepslate_tile_slab", type="bottom", waterlogged="false")

    # A stone-carver's screen, supported from the research-wing roof, breaks
    # the broad gray field between the east windows. Its uneven pier rhythm
    # and contrasting blind recesses follow the donor's sectional masonry
    # rather than tracing a continuous circular ring around the Hall. Leave
    # the older central blue window and all openings untouched.
    for z in (27, 39):
        for y in range(40, 64):
            if cells.get((85, y, z), ("minecraft:air",))[0] == "minecraft:air":
                continue
            b(86, y, z, "chiseled_stone_bricks" if y in (40, 44, 50, 56, 63)
              else "polished_tuff" if y % 4 else "tuff_bricks")
    for z in range(28, 39):
        if z == 33:
            continue
        for y in (43, 56, 63):
            if cells.get((85, y, z), ("minecraft:air",))[0] != "minecraft:air":
                b(86, y, z, "deepslate_tile_slab", type="bottom", waterlogged="false")
    for z0 in (29, 36):
        for z in range(z0, z0 + 3):
            for y in range(46, 54):
                if cells.get((85, y, z), ("minecraft:air",))[0] != "minecraft:stone_bricks":
                    continue
                edge = z in (z0, z0 + 2) or y in (46, 53)
                b(85, y, z, "polished_tuff" if edge else "polished_deepslate")
    for z0 in (25, 41):
        for z in range(z0 - 2, z0 + 3):
            b(86, 55, z, "deepslate_tile_slab", type="bottom", waterlogged="false")

    # Three different windowed research bays interrupt the long, nearly blank
    # south face of the connecting academic gallery seen from the east court.
    for cx in (85, 90, 93):
        for y in range(27, 33):
            b(cx, y, 49, "orange_stained_glass_pane" if 28 <= y <= 31
              else "chiseled_stone_bricks")
            if y == 29:
                b(cx, y, 48, "sea_lantern")
            for x in (cx - 1, cx + 1):
                b(x, y, 49, "polished_andesite" if y not in (27, 32)
                  else "chiseled_stone_bricks")
        for x in range(cx - 1, cx + 2):
            b(x, 34, 50, "stone_brick_stairs", facing="south", half="top",
              shape="straight", waterlogged="false")

    # A slate roof surface replaces the unbroken light-gray cap, while the
    # donor-derived andesite relief remains visible as its pale embroidery.
    for x in range(50, 87):
        for z in range(15, 52):
            r = hypot(x - 68, z - 33)
            if not 6 < r < 18:
                continue
            for y in range(86, 70, -1):
                if cells.get((x, y, z), ("minecraft:air",))[0] == "minecraft:stone_bricks":
                    if cells.get((x, y + 1, z), ("minecraft:air",))[0] == "minecraft:air":
                        b(x, y, z, "deepslate_tiles" if int(r) % 5 else "polished_andesite")
                    break
    # Three amber clerestory recesses make the Hall legible after dark from
    # the long bridge, with a carved central bay instead of a flat drum face.
    for cx, y0, y1 in ((64, 56, 58), (68, 55, 60), (72, 56, 58)):
        for y in range(y0, y1 + 1):
            b(cx, y, 52, "orange_stained_glass")
            b(cx, y, 51, "sea_lantern")
            for x in (cx - 1, cx + 1):
                b(x, y, 52, "polished_andesite" if y % 2 else "chiseled_stone_bricks")
        for x in range(cx - 1, cx + 2):
            b(x, y1 + 1, 52, "stone_brick_stairs", facing="south", half="top",
              shape="straight", waterlogged="false")


def craft_arcanaeum(cells, put):
    """Multi-storey shelf courts, framed reading desks and a carved crown."""
    def b(x, y, z, name, **props):
        _put(put, x, y, z, name, **props)

    def inside(x, z, inset=0):
        return ((x - 147) / (22 - inset)) ** 2 + ((z - 34) / (23 - inset)) ** 2 <= 1

    # The original round library also receives a crafted central nave and
    # tall side archives. Its Conclave lectern and broad east-west aisle stay.
    for x in (100, 101, 123, 124):
        for z in range(25, 44):
            if z in range(29, 32) or z in range(38, 41):
                continue
            for y in range(25, 32):
                b(x, y, z, "bookshelf" if y not in (29, 31) else
                  "dark_oak_planks" if y == 29 else "chiseled_stone_bricks")
    for z in (25, 34, 43):
        for x in (105, 119):
            for y in range(25, 32):
                b(x, y, z, "chiseled_stone_bricks" if y in (25, 29, 31)
                  else "polished_andesite")
        for x in range(106, 119):
            b(x, 32, z, "dark_oak_planks" if x % 4 else "chiseled_stone_bricks")
    for x in range(106, 119):
        for z in range(25, 44):
            if x in (107, 112, 117) or z in (25, 34, 43):
                b(x, 33, z, "dark_oak_planks" if (x + z) % 4 else "polished_andesite")
    for x, z in ((107, 26), (117, 26), (107, 42), (117, 42)):
        b(x, 31, z, "lantern", hanging="true", waterlogged="false")
    for x, z in ((110, 26), (114, 42)):
        b(x, 25, z, "chiseled_bookshelf", facing="south" if z < 34 else "north")
        b(x, 26, z, "amethyst_block")
    for x in range(107, 118):
        for y in range(25, 32):
            b(x, y, 20, "bookshelf" if x not in (107, 112, 117) and y not in (29, 31)
              else "dark_oak_planks" if y == 29 else "chiseled_stone_bricks")
    for x in (107, 117):
        b(x, 26, 21, "chiseled_stone_bricks")
        b(x, 27, 21, "lantern", hanging="false", waterlogged="false")
    b(112, 30, 21, "amethyst_block")

    # Fonta59's x31..54/Z-188..-165 archive groups books in tall returns.
    # Refit separate north and south runs into Frostward's larger half oval.
    # Every third bay is a warm niche or carved support, not another shelf.
    for x in range(150, 165):
        for z in (22, 23, 45, 46):
            if not inside(x, z, 3):
                continue
            if (x - 150) % 5 == 0:
                for y in range(25, 35):
                    b(x, y, z, "chiseled_stone_bricks" if y in (25, 29, 34)
                      else "polished_andesite")
                b(x, 30, z - 1 if z < 34 else z + 1, "lantern", hanging="false", waterlogged="false")
            else:
                for y in range(25, 34):
                    b(x, y, z, "bookshelf" if y not in (29, 33)
                      else "dark_oak_planks" if y == 29 else "stone_brick_slab")
                b(x, 34, z, "stone_brick_stairs", facing="south" if z < 34 else "north",
                  half="top", shape="straight", waterlogged="false")

    # A continuous occupied upper gallery already exists at Y36. Give it
    # readable stacks, shelf returns, dark timber handrails and hanging lamps.
    for x in range(151, 165):
        for z in (24, 44):
            if inside(x, z, 2) and (x - 151) % 4 != 0:
                for y in range(37, 42):
                    b(x, y, z, "bookshelf" if y != 40 else "dark_oak_planks")
                b(x, 42, z, "chiseled_stone_bricks")
    for x in range(151, 164):
        if x in (153, 158, 163):
            continue
        for z in (29, 39):
            if cells.get((x, 36, z), ("minecraft:air",))[0] != "minecraft:air":
                b(x, 37, z, "dark_oak_fence")
    for x in (153, 159, 164):
        for z in (26, 42):
            if inside(x, z, 2):
                b(x, 41, z, "lantern", hanging="true", waterlogged="false")

    # Pairs of square-column arches break the vast gray internal wall and
    # support stepped coffers beneath the Y44 roof. The central opening remains
    # high and the established eastern stair at Z41 is untouched.
    for x in (150, 158, 165):
        for z in (26, 42):
            if inside(x, z, 2):
                for y in range(25, 36):
                    b(x, y, z, "chiseled_stone_bricks" if y in (25, 29, 34, 35)
                      else "polished_andesite")
                for dz in (-1, 1):
                    b(x, 34, z + dz, "stone_brick_stairs", facing="north" if dz > 0 else "south",
                      half="top", shape="straight", waterlogged="false")
    for x in range(151, 165):
        for z in range(25, 44):
            if inside(x, z, 2) and (x in (151, 158, 164) or z in (25, 43)):
                b(x, 42, z, "dark_oak_planks" if (x + z) % 4 else "chiseled_stone_bricks")

    # Two wide, distinctly furnished work islands. They leave the door,
    # central room anchor and stair approach clear.
    for cx, cz in ((151, 31), (160, 37)):
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 1, cz + 2):
                b(x, 24, z, "dark_oak_planks")
                if z == cz and abs(x - cx) <= 1:
                    b(x, 25, z, "dark_oak_slab", type="top", waterlogged="false")
        b(cx, 25, cz - 1, "lectern", facing="south", has_book="false", powered="false")
        for x in (cx - 2, cx + 2):
            b(x, 25, cz + 1, "spruce_stairs", facing="north", half="bottom",
              shape="straight", waterlogged="false")
        b(cx, 26, cz, "lantern", hanging="false", waterlogged="false")

    # A deep east archive frontage replaces the blank oval wall seen from the
    # principal reading aisle. Projecting piers and recessed book wells give
    # the two bays a real three-block section; daylight slots behind remain.
    for z in range(28, 41):
        if z in (28, 34, 40):
            for y in range(25, 36):
                b(162, y, z, "chiseled_stone_bricks" if y in (25, 29, 34, 35)
                  else "polished_andesite")
            b(163, 35, z, "stone_brick_stairs", facing="west", half="top",
              shape="straight", waterlogged="false")
            continue
        for x in range(163, 166):
            b(x, 24, z, "dark_oak_planks")
        for y in range(25, 34):
            b(165, y, z, "bookshelf" if y != 29 else "dark_oak_planks")
        b(164, 34, z, "stone_brick_stairs", facing="west", half="top",
          shape="straight", waterlogged="false")
    for z in (31, 37):
        b(163, 31, z, "lantern", hanging="true", waterlogged="false")
        b(162, 25, z, "spruce_stairs", facing="east", half="bottom",
          shape="straight", waterlogged="false")
        b(165, 30, z, "chiseled_bookshelf", facing="west")

    # A third, communal research table fills part of the former vacant middle
    # without touching the west entry, upper stair or named room point.
    for x in range(155, 160):
        for z in range(30, 33):
            b(x, 24, z, "dark_oak_planks")
    for x in range(156, 159):
        for z in (31, 32):
            b(x, 25, z, "dark_oak_slab", type="top", waterlogged="false")
    b(157, 25, 30, "lectern", facing="south", has_book="false", powered="false")
    b(157, 26, 31, "lantern", hanging="false", waterlogged="false")
    for x in (155, 159):
        b(x, 25, 33, "spruce_stairs", facing="north", half="bottom",
          shape="straight", waterlogged="false")

    # The high central reading well gains a suspended, warm arcane fixture;
    # it remains above the lower headroom and below the solid roof.
    for y in range(40, 44):
        b(156, y, 35, "iron_chain", axis="y", waterlogged="false")
    b(156, 39, 35, "amethyst_block")
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        b(156 + dx, 39, 35 + dz, "sea_lantern")
        b(156 + dx, 40, 35 + dz, "polished_andesite")

    # Carved exterior spandrels and a dark stepped crown give the reading wing
    # its own skyline; source sectional rhythm is adapted, not copied.
    for x in range(145, 167):
        for z in (12, 56):
            if not inside(x, z, -1):
                continue
            for y in (30, 35, 42):
                if cells.get((x, y, z), ("minecraft:air",))[0] == "minecraft:stone_bricks":
                    b(x, y, z, "chiseled_stone_bricks" if y == 35 else "polished_andesite")
    for x in range(142, 165):
        for z in range(15, 54):
            if inside(x, z, 7) and not inside(x, z, 11):
                b(x, 46, z, "deepslate_tile_slab", type="bottom", waterlogged="false")


def landscape_autumn_court(cells, put):
    """Planted quadrants, irregular maples, leaf drift and warm Halloween light."""
    def b(x, y, z, name, **props):
        _put(put, x, y, z, name, **props)

    # Four old isolated maples become centers of unequal, raised garden beds.
    # The garden outlines are deliberately noncircular; all four radial walks
    # and the S01 ward's central approach remain at their original level.
    gardens = ((52, 75, 8, 7, "red"), (84, 75, 8, 7, "orange"),
               (52, 96, 8, 7, "yellow"), (83, 96, 8, 7, "red"))
    for cx, cz, rx, rz, colour in gardens:
        for x in range(cx - rx, cx + rx + 1):
            for z in range(cz - rz, cz + rz + 1):
                dx, dz = abs(x - cx), abs(z - cz)
                if dx + dz // 2 > rx + 1 or dz + dx // 3 > rz + 1:
                    continue
                if abs(x - 68) <= 5 or abs(z - 85) <= 5 or hypot(x - 68, z - 85) > 22:
                    continue
                if cells.get((x, 25, z), ("minecraft:air",))[0] == "minecraft:carved_pumpkin":
                    continue
                edge = dx + dz // 2 >= rx or dz + dx // 3 >= rz
                b(x, 24, z, "mossy_stone_bricks" if edge else "coarse_dirt" if (x + z) % 3 else "moss_block")
                if edge:
                    b(x, 25, z, "mossy_stone_brick_slab", type="bottom", waterlogged="false")
                elif (x * 7 + z * 3) % 5 == 0 and (x, z) != (cx, cz):
                    b(x, 25, z, "jugcraft:" + colour + "_leaf_pile", layers=1)
                elif (x * 13 + z * 11) % 13 == 0 and (x, z) != (cx, cz):
                    b(x, 25, z, "jugcraft:" + ("purple" if (x + z) % 7 == 0 else colour) + "_mum")
                elif (x * 3 + z * 7) % 23 == 0 and (x, z) != (cx, cz):
                    b(x, 25, z, "jugcraft:ecology_" + colour + "_maple_leaves")

    # Varied tree heights, fork directions and asymmetric canopies prevent a
    # repeated four-tree postcard silhouette. No trunk is on the formal paths.
    trees = ((58, 72, "yellow", 5, 2), (76, 73, "red", 7, 3),
             (56, 100, "orange", 6, 2), (79, 101, "yellow", 5, 2),
             (48, 89, "orange", 4, 2), (87, 89, "red", 4, 2))
    for tx, tz, colour, height, radius in trees:
        if hypot(tx - 68, tz - 85) > 22 or abs(tx - 68) <= 5 or abs(tz - 85) <= 2:
            continue
        b(tx, 24, tz, "coarse_dirt")
        for y in range(25, 25 + height):
            b(tx, y, tz, "jugcraft:ecology_maple_log", axis="y")
        for dx in (-1, 1):
            b(tx + dx, 27 + height - 5, tz, "jugcraft:ecology_maple_log", axis="x")
        for y, rr in ((23 + height, radius - 1), (24 + height, radius),
                      (25 + height, radius + 1), (26 + height, radius),
                      (27 + height, radius - 1)):
            for x in range(tx - rr, tx + rr + 1):
                for z in range(tz - rr, tz + rr + 1):
                    if (x - tx) ** 2 + (z - tz) ** 2 <= rr * rr and (x + 2 * z + y) % 7 != 0:
                        b(x, y, z, "jugcraft:ecology_" + colour + "_maple_leaves")

    # Pumpkin groupings and garden lanterns make the scene celebratory by day
    # and eerie at night. These are at the beds, away from the ward/rune paths.
    for x, z, facing in ((55, 78, "south"), (49, 76, "east"),
                         (82, 79, "south"), (88, 75, "west"),
                         (55, 98, "north"), (48, 96, "east"),
                         (81, 98, "north"), (87, 96, "west")):
        b(x, 25, z, "jack_o_lantern", facing=facing)
        b(x + (1 if x < 68 else -1), 25, z, "pumpkin")
    for x, z in ((59, 77), (77, 77), (58, 97), (78, 98)):
        b(x, 25, z, "mossy_stone_bricks")
        b(x, 26, z, "lantern", hanging="false", waterlogged="false")

    # Wreaths are native Jugcraft blocks attached to proven stone support.
    # Gatehouse and Hall entries each have a different seasonal colour.
    for x, z, colour in ((62, 127, "orange"), (74, 127, "red"),
                         (59, 54, "yellow"), (77, 54, "purple")):
        behind = cells.get((x, 28, z - 1), ("minecraft:air",))[0]
        if behind in ("minecraft:stone_bricks", "minecraft:polished_andesite",
                      "minecraft:chiseled_stone_bricks"):
            b(x, 28, z, "jugcraft:autumn_wreath", facing="south", flowers=colour)

    # Leaf garlands are carried by the existing eight arcade frames, kept well
    # above eye level. Narrow amber/green highlights enrich the covered walk.
    for x, z in ((48, 74), (48, 96), (88, 74), (88, 96),
                 (52, 65), (84, 65), (52, 105), (84, 105)):
        for dx in range(-2, 3):
            if cells.get((x + dx, 31, z), ("minecraft:air",))[0] != "minecraft:air":
                b(x + dx, 30, z, "jugcraft:ecology_" + ("red" if dx < 0 else "orange") + "_maple_leaves")

    for bay in range(8):
        angle = pi / 8 + bay * pi / 4
        x0 = round(68 + 23 * cos(angle))
        z0 = round(85 + 23 * sin(angle))
        for along in range(-2, 3):
            x = x0 + round(-sin(angle) * along)
            z = z0 + round(cos(angle) * along)
            if cells.get((x, 30, z), ("minecraft:air",))[0] == "minecraft:air":
                colour = ("red", "orange", "yellow")[(bay + (along > 0)) % 3]
                b(x, 30, z, "jugcraft:ecology_" + colour + "_maple_leaves")
                if along in (-1, 1) and cells.get((x, 29, z), ("minecraft:air",))[0] == "minecraft:air":
                    b(x, 29, z, "jugcraft:ecology_" + colour + "_maple_leaves")


def craft_bridge_gate(put):
    """Carve and light the two reception façades that flank the open gate."""
    def b(x, y, z, name, **props):
        _put(put, x, y, z, name, **props)

    for cx in (63, 73):
        for y in range(27, 31):
            b(cx, y, 126, "orange_stained_glass_pane")
            b(cx, y, 125, "sea_lantern")
        for x in (cx - 1, cx + 1):
            for y in range(26, 33):
                b(x, y, 126, "chiseled_stone_bricks" if y in (26, 31, 32)
                  else "polished_andesite")
        for x in range(cx - 2, cx + 3):
            b(x, 32, 127, "stone_brick_stairs", facing="south", half="top",
              shape="straight", waterlogged="false")
