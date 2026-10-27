"""Room-specific craft for Frostward's library, autumn court and private suite.

The permitted Fonta59 world supplies the sectional idea of deep shelf returns,
stone-framed openings and occupied perimeter bays. These are newly composed
around Frostward's existing rooms, stations, stairs and named routes.
"""
from math import hypot

from retro_game_shop import state


def craft_flagship_rooms(cells, put):
    def b(x, y, z, name, **props):
        put(x, y, z, state(name if ":" in name else "minecraft:" + name, **props))

    def old(x, y, z):
        return cells.get((x, y, z), ("minecraft:air",))[0]

    # LIBRARY: replace the original low ceiling over the reading nave with
    # a full-height archive lantern and reachable upper reading gallery. The
    # side stacks and Conclave at (112,25,23) stay outside this roof cut.
    # Fonta59's stepped shelf returns become inhabited perimeter archive bays.
    for x in range(103, 122):
        for z in range(25, 44):
            for y in range(32, 46):
                b(x, y, z, "air")
            b(x, 46, z, "deepslate_tiles" if (x + z) % 7 else "polished_deepslate")
    for z in (27, 34, 41):
        for x in range(103, 122):
            dx = abs(x - 112)
            y = 45 - (dx * 2 // 3)
            b(x, y, z, "chiseled_stone_bricks" if dx in (0, 5, 9) else "polished_tuff")
            if dx < 7:
                b(x, y - 1, z, "polished_tuff_stairs", facing="east" if x < 112 else "west",
                  half="top", shape="straight", waterlogged="false")
    for x in (103, 121):
        for z in range(25, 44):
            for y in range(37, 46):
                b(x, y, z, "chiseled_stone_bricks" if z in (25, 34, 43) or y in (37, 45)
                  else "orange_stained_glass" if z % 4 in (1, 2) and y in (40, 41)
                  else "polished_deepslate")
    for z in (25, 43):
        for x in range(104, 121):
            for y in range(37, 46):
                b(x, y, z, "chiseled_stone_bricks" if x in (104, 112, 120) or y in (37, 45)
                  else "orange_stained_glass" if y in (40, 41)
                  else "polished_deepslate")
    # An uneven stepped Nordic crown makes the new lantern part of the campus
    # roof hierarchy instead of an unbroken black box above the library.
    for half_width, z0, z1, y in ((7, 27, 41, 47), (5, 28, 40, 48),
                                  (3, 29, 39, 49), (1, 30, 38, 50)):
        for x in range(112 - half_width, 113 + half_width):
            for z in range(z0, z1 + 1):
                b(x, y, z, "deepslate_tiles" if abs(x - 112) == half_width
                  else "polished_deepslate")
    for z in range(31, 38):
        b(112, 51, z, "waxed_cut_copper_slab", type="bottom", waterlogged="false")
    for x in (102, 122):
        for z in (25, 34, 43):
            for y in range(36, 46):
                b(x, y, z, "chiseled_stone_bricks" if y in (36, 41, 45)
                  else "polished_tuff")
    # The y34 balcony wraps the new void. A broad nine-rise stair from the
    # south reading floor reaches its west side; a gap in the rail is kept at
    # the landing. Upper shelves face the central teaching/reading well.
    for z in range(27, 43):
        for x in list(range(104, 108)) + list(range(117, 121)):
            b(x, 34, z, "dark_oak_planks" if x in (105, 106, 118, 119)
              else "chiseled_stone_bricks")
    for z in (27, 42):
        for x in range(104, 121):
            b(x, 34, z, "dark_oak_planks" if x % 4 else "chiseled_stone_bricks")
    for z in range(28, 42):
        for x in (107, 117):
            if not (x == 107 and z in (33, 34, 35)):
                b(x, 35, z, "dark_oak_fence")
    for x in range(108, 117):
        for z in (28, 41):
            b(x, 35, z, "dark_oak_fence")
    for z in range(29, 41):
        for x in (104, 120):
            for y in range(35, 40):
                b(x, y, z, "dark_oak_planks" if y == 38 else "bookshelf")
            b(x, 40, z, "chiseled_stone_bricks")
    for z in range(43, 33, -1):
        height = 25 + 43 - z
        for x in (107, 108):
            for y in range(25, height):
                # A diagonal spine and three grounded posts keep the flight
                # believable without the first pass's view-blocking wedge.
                b(x, y, z, "polished_tuff" if y == height - 1 or
                  (x == 107 and z in (43, 40, 37, 34)) else "air")
            b(x, height, z, "polished_tuff_stairs", facing="north",
              half="bottom", shape="straight", waterlogged="false")
            for y in (height + 1, height + 2):
                if y < 37:
                    b(x, y, z, "air")
        if z not in (34, 35):
            b(106, height, z, "chiseled_stone_bricks")
            b(106, height + 1, z, "dark_oak_fence")
    for z in (29, 39):
        for x in (110, 114):
            b(x, 43, z, "iron_chain", axis="y", waterlogged="false")
            b(x, 42, z, "lantern", hanging="true", waterlogged="false")
    # High archive fronts are separated by dark oak bands and deep stone
    # returns. Four deliberately different research bays flank the clear
    # central aisle instead of repeating one bench down the room.
    for x in (101, 123):
        for z in range(25, 44):
            if z in (29, 30, 38, 39):
                continue
            for y in range(25, 32):
                b(x, y, z, "chiseled_stone_bricks" if z % 5 == 0 and y in (25, 29, 31)
                  else "dark_oak_planks" if y == 29 else "bookshelf")
        for z in (27, 35, 42):
            b(x + (1 if x < 112 else -1), 31, z, "lantern", hanging="true", waterlogged="false")
    for cx, cz, material in ((106, 30, "dark_oak_planks"), (118, 30, "spruce_planks"),
                             (106, 39, "spruce_planks"), (118, 39, "dark_oak_planks")):
        for x in range(cx - 1, cx + 2):
            b(x, 25, cz, material)
            b(x, 26, cz, "dark_oak_slab", type="top", waterlogged="false")
        b(cx, 26, cz - 1, "lectern", facing="south", has_book="false", powered="false")
        b(cx - 1, 25, cz + 2, "spruce_stairs", facing="north", half="bottom",
          shape="straight", waterlogged="false")
        b(cx + 1, 25, cz + 2, "spruce_stairs", facing="north", half="bottom",
          shape="straight", waterlogged="false")
        b(cx + 2 if cx < 112 else cx - 2, 25, cz, "chiseled_bookshelf",
          facing="west" if cx < 112 else "east")
    # A single charting instrument under the nave, not a row of identical
    # white electric fixtures. Its bright center is softened by amber glass.
    for x in range(109, 116):
        for z in range(32, 37):
            if old(x, 31, z) in ("minecraft:sea_lantern", "minecraft:smooth_quartz"):
                b(x, 31, z, "air")
    for x, z in ((110, 34), (114, 34), (112, 32), (112, 36)):
        b(x, 43, z, "waxed_cut_copper")
    b(112, 43, 34, "sea_lantern")
    b(112, 42, 34, "orange_stained_glass")

    # GRAND READING WING: a long inlaid walnut reading floor gives the
    # formerly white, repetitive shelf hall a deliberate institutional axis.
    # The extant three work islands and (150,25,34) route remain above it.
    for x in range(143, 162):
        for z in range(28, 40):
            if x in (143, 161) or z in (28, 39):
                b(x, 24, z, "polished_deepslate")
            elif x in (147, 153, 159):
                b(x, 24, z, "waxed_cut_copper" if z in (30, 34, 37)
                  else "polished_tuff")
            elif z in (31, 36):
                b(x, 24, z, "spruce_planks")
            else:
                b(x, 24, z, "dark_oak_planks")
    for x in (145, 157):
        for z in (29, 38):
            b(x, 24, z, "chiseled_stone_bricks")

    # COURT: the planted ground now meets the Nordic arcade in a continuous
    # irregular herbaceous verge. Formal five-wide radial walks, S01 runes and
    # portals stay at the original grade; the verge gives maples a landscape
    # instead of four isolated patches on bare paving.
    for x in range(44, 93):
        for z in range(61, 110):
            r = hypot(x - 68, z - 85)
            if not 15 <= r <= 22 or abs(x - 68) <= 5 or abs(z - 85) <= 5:
                continue
            if old(x, 24, z) == "minecraft:air" or old(x, 25, z) not in (
                    "minecraft:air", "minecraft:mossy_stone_brick_slab"):
                continue
            if (x * 7 + z * 11) % 11 < 3:
                b(x, 24, z, "moss_block" if (x + z) % 3 else "coarse_dirt")
            else:
                b(x, 24, z, "mossy_stone_bricks" if (x + z) % 3 else "cracked_stone_bricks")
            if (x * 17 + z * 13) % 8 == 0:
                colour = ("red", "orange", "yellow")[(x + z) % 3]
                b(x, 25, z, "jugcraft:" + colour + "_leaf_pile", layers=1)
            elif (x * 5 + z * 3) % 19 == 0:
                b(x, 25, z, "jugcraft:" + ("purple" if x % 4 else "orange") + "_mum")
    # Interlock maple branches with the arcade's planted plinths. These low
    # balconies of foliage frame the institution without filling its paths.
    for x, z, colour in ((49, 71, "red"), (87, 71, "yellow"),
                         (48, 98, "orange"), (88, 98, "red")):
        for dx in range(-2, 3):
            for dz in range(-1, 2):
                if abs(dx) + abs(dz) > 3:
                    continue
                b(x + dx, 30, z + dz, "jugcraft:ecology_" + colour + "_maple_leaves")
        b(x, 27, z, "jack_o_lantern", facing="south" if z < 85 else "north")
    for x, z in ((53, 83), (83, 83), (53, 88), (83, 88)):
        b(x, 24, z, "mossy_stone_bricks")
        b(x, 25, z, "chiseled_stone_bricks")
        b(x, 26, z, "lantern", hanging="false", waterlogged="false")

    # ARCH-MAGE: the upper public study gains a carved ceiling, a pair of
    # working scholarly desks, botanical cabinets and a dark reading hearth.
    # Keep room anchors at (68,51,33) and (73,51,36) clear.
    for x in range(56, 81):
        for z in range(22, 45):
            if hypot(x - 68, z - 33) > 11:
                continue
            if (x - 68) % 6 == 0 or (z - 33) % 6 == 0:
                if old(x, 62, z) != "minecraft:air":
                    b(x, 61, z, "dark_oak_planks" if (x + z) % 4 else "chiseled_stone_bricks")
    for x, z in ((58, 27), (78, 27), (58, 39), (78, 39)):
        for y in range(51, 59):
            b(x, y, z, "polished_tuff" if y not in (51, 54, 58) else "chiseled_stone_bricks")
        b(x, 59, z, "dark_oak_planks")
    for cx, cz in ((61, 29), (76, 30)):
        for x in range(cx - 2, cx + 3):
            b(x, 51, cz, "dark_oak_planks")
            b(x, 52, cz, "dark_oak_slab", type="top", waterlogged="false")
        b(cx, 52, cz - 1, "lectern", facing="south", has_book="false", powered="false")
        for x in (cx - 2, cx + 2):
            b(x, 51, cz + 2, "spruce_stairs", facing="north", half="bottom",
              shape="straight", waterlogged="false")
        b(cx, 53, cz, "lantern", hanging="false", waterlogged="false")
    for x in range(63, 74):
        for y in range(52, 58):
            if x in (63, 68, 73):
                b(x, y, 22, "chiseled_stone_bricks")
            else:
                b(x, y, 22, "bookshelf" if y not in (54, 57) else "dark_oak_planks")
    for x in (64, 72):
        b(x, 58, 23, "lantern", hanging="true", waterlogged="false")

    # The main study needs a commanding, usable center rather than a clear
    # parquet field with furniture only at its rim. A backed writing table
    # sits between the existing north library wall and the open study anchor.
    # Its two unequal side cabinets are newly composed around the original
    # stair, conservatory approach and the two scholarly desks above.
    for x in range(64, 73):
        for z in (26, 27):
            b(x, 51, z, "dark_oak_planks" if x in (64, 68, 72) else "polished_deepslate")
            b(x, 52, z, "dark_oak_slab", type="top", waterlogged="false")
    for x in (65, 68, 71):
        b(x, 53, 26, "waxed_cut_copper_slab", type="bottom", waterlogged="false")
    b(68, 53, 27, "lectern", facing="south", has_book="false", powered="false")
    for x in (64, 72):
        b(x, 53, 27, "lantern", hanging="false", waterlogged="false")
    for x in (65, 71):
        b(x, 51, 29, "spruce_stairs", facing="north", half="bottom",
          shape="straight", waterlogged="false")
    for z in range(24, 29):
        for x in (62, 74):
            for y in range(51, 57):
                b(x, y, z, "chiseled_stone_bricks" if y in (51, 55) or z in (24, 28)
                  else "dark_oak_planks" if y == 54 else "bookshelf")
    for x in (62, 74):
        b(x, 56, 26, "lantern", hanging="true", waterlogged="false")
    # Framed collections sit in the side walls instead of another repeated
    # wall of shelves. Warm stone and amethyst mark the private collection.
    for x, z0, z1 in ((60, 34, 39), (77, 29, 32)):
        for z in range(z0, z1 + 1):
            for y in range(52, 58):
                b(x, y, z, "chiseled_stone_bricks" if y in (52, 57) or z in (z0, z1)
                  else "polished_deepslate" if y in (53, 56)
                  else "bookshelf")
        b(x + (1 if x < 68 else -1), 54, (z0 + z1) // 2,
          "amethyst_block")
        b(x + (1 if x < 68 else -1), 55, (z0 + z1) // 2,
          "orange_stained_glass")

    # A generous parquet reading floor and dark masonry border give the
    # Arch-Mage's principal room a residential scale. The conservatory's
    # planted eastern floor and the center study/garden anchors remain open.
    for x in range(58, 79):
        for z in range(24, 44):
            radius = hypot(x - 68, z - 33)
            if radius > 10 or (x >= 72 and z >= 34):
                continue
            if radius >= 8.5:
                b(x, 50, z, "polished_deepslate")
            elif 63 <= x <= 73 and 28 <= z <= 38:
                if x in (63, 73) or z in (28, 38):
                    b(x, 50, z, "red_terracotta" if (x + z) % 7 else "purple_terracotta")
                else:
                    b(x, 50, z, "dark_oak_planks")
            else:
                b(x, 50, z, "dark_oak_planks" if (x + z) % 4 else "spruce_planks")
    for x in range(64, 73):
        for z in (28, 38):
            b(x, 50, z, "waxed_cut_copper" if x in (64, 68, 72) else "polished_deepslate")
    for z in range(29, 38):
        for x in (64, 72):
            b(x, 50, z, "waxed_cut_copper" if z in (29, 33, 37) else "polished_deepslate")
    # A small scholar's celestial chandelier is suspended high enough to
    # keep the suite's original route and study floor unobstructed.
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if 2.2 <= hypot(dx, dz) <= 3.2:
                b(68 + dx, 59, 33 + dz, "waxed_cut_copper_slab",
                  type="top", waterlogged="false")
    for x, z in ((65, 33), (71, 33), (68, 30), (68, 36)):
        b(x, 58, z, "lantern", hanging="true", waterlogged="false")
    b(68, 59, 33, "amethyst_block")
    b(68, 58, 33, "orange_stained_glass")

    # The annex is a private collection with real wall-scale shelves and a
    # framed, existing bed. Wood ceiling coffers and copper lights add warmth;
    # its room and bedroom anchors remain walkable beside the furniture.
    for x in range(34, 53):
        for z in range(25, 42):
            if hypot(x - 43, z - 33) > 9:
                continue
            if (x - 43) % 5 == 0 or (z - 33) % 5 == 0:
                b(x, 59, z, "dark_oak_planks" if (x + z) % 4 else "chiseled_stone_bricks")
    for x in (34, 35, 50, 51):
        for z in range(29, 38):
            if z in (32, 33):
                continue
            for y in range(51, 58):
                if old(x, y, z) in ("minecraft:air", "minecraft:bookshelf"):
                    b(x, y, z, "dark_oak_planks" if y in (54, 57) else "bookshelf")
    for x, z in ((36, 30), (49, 30), (36, 39), (49, 39)):
        b(x, 51, z, "chiseled_quartz_block")
        b(x, 52, z, "lantern", hanging="false", waterlogged="false")
    for x in (38, 41):
        for z in (35, 39):
            for y in range(51, 55):
                b(x, y, z, "dark_oak_log", axis="y")
            b(x, 55, z, "dark_oak_planks")
    for x in range(38, 42):
        for z in (35, 39):
            b(x, 55, z, "dark_oak_planks")
    for z in range(36, 39):
        for x in (38, 41):
            b(x, 55, z, "dark_oak_planks")
    b(39, 51, 36, "blue_bed", facing="north", part="head", occupied="false")
    b(39, 51, 37, "blue_bed", facing="north", part="foot", occupied="false")
    for x in range(44, 49):
        b(x, 51, 37, "dark_oak_planks")
        b(x, 52, 37, "dark_oak_slab", type="top", waterlogged="false")
    b(46, 52, 36, "lectern", facing="south", has_book="false", powered="false")
    b(47, 53, 37, "lantern", hanging="false", waterlogged="false")

    # A glazed autumn conservatory grows out of the study's existing small
    # planted corner. It is local to the upper suite and sealed by a copper
    # frame, not a climate or weather change. The garden anchor remains open.
    for x in range(72, 80):
        for z in range(34, 42):
            for y in range(62, 65):
                b(x, y, z, "air")
            b(x, 65, z, "waxed_cut_copper" if x in (72, 79) or z in (34, 41)
              else "orange_stained_glass")
            if x in (74, 77) and z in (36, 39):
                b(x, 66, z, "sea_lantern")
    for x, z in ((72, 34), (79, 34), (72, 41), (79, 41)):
        for y in range(51, 65):
            b(x, y, z, "chiseled_stone_bricks" if y in (51, 57, 64)
              else "polished_tuff")
    for x in range(74, 79):
        for z in range(36, 41):
            if x in (74, 78) or z in (36, 40):
                b(x, 50, z, "moss_block" if (x + z) % 3 else "coarse_dirt")
            if (x, z) in ((75, 38), (77, 37), (76, 40)):
                b(x, 51, z, "jugcraft:" + ("purple" if x == 75 else "orange") + "_mum")
    for x, z, colour in ((76, 37, "red"), (78, 39, "orange")):
        b(x, 51, z, "jugcraft:ecology_maple_log", axis="y")
        for y in (54, 55):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    if abs(dx) + abs(dz) < 3:
                        b(x + dx, y, z + dz, "jugcraft:ecology_" + colour + "_maple_leaves")
    for x, z in ((73, 40), (79, 35)):
        b(x, 51, z, "chiseled_quartz_block")
        b(x, 52, z, "lantern", hanging="false", waterlogged="false")

    # Receiving room: a stone-bordered walnut carpet, reading seats and a
    # scholar's instrument make the large upper floor occupied without
    # constricting the west gallery or center route.
    for x in range(62, 69):
        for z in range(38, 43):
            if hypot(x - 65, z - 40) <= 3.5:
                b(x, 50, z, "dark_oak_planks" if x in (62, 68) or z in (38, 42)
                  else "red_terracotta" if (x + z) % 3 else "purple_terracotta")
    for x in range(64, 67):
        b(x, 51, 40, "dark_oak_planks")
        b(x, 52, 40, "dark_oak_slab", type="top", waterlogged="false")
    for x in (63, 67):
        b(x, 51, 41, "spruce_stairs", facing="north", half="bottom",
          shape="straight", waterlogged="false")
    b(65, 53, 40, "lantern", hanging="false", waterlogged="false")

    # Replace the annex's small solitary quartz pedestal with a long writing
    # table and map instruments, then frame its former slit as a tall amber
    # window between deeply recessed northern archive cases.
    for y in (51, 52):
        b(42, y, 30, "air")
    for x in range(40, 47):
        for z in (29, 30):
            b(x, 51, z, "dark_oak_planks" if x in (40, 43, 46) else "air")
            b(x, 52, z, "dark_oak_slab", type="top", waterlogged="false")
    b(43, 52, 28, "lectern", facing="south", has_book="false", powered="false")
    for x in (41, 45):
        b(x, 53, 30, "lantern", hanging="false", waterlogged="false")
        b(x, 51, 32, "spruce_stairs", facing="north", half="bottom",
          shape="straight", waterlogged="false")
    for x in range(40, 47):
        for y in range(52, 59):
            if x in (40, 46) or y in (52, 58):
                b(x, y, 22, "chiseled_stone_bricks")
            elif x in (42, 43, 44) and y in (54, 55, 56):
                b(x, y, 22, "orange_stained_glass")
            else:
                b(x, y, 22, "polished_deepslate")
