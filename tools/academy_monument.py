"""A sectional, inhabited Hall of Elements fitted into Frostward's existing tower.

The owner's permitted Fonta59 Hall has a deliberately open teaching void at
Y118-130, an occupied stone ring at Y131, and successive slab/stair courses at
Y132-135. This pass adapts that section to Frostward's larger Y25-50 Hall;
the old stations, stairs, center gallery anchor and upper suites stay in place.
No donor room, wall volume or complete building is copied.
"""
from math import hypot

from retro_game_shop import state


def craft_monumental_elements(cells, put):
    def b(x, y, z, name, **props):
        put(x, y, z, state(name if ":" in name else "minecraft:" + name, **props))

    def old(x, y, z):
        return cells.get((x, y, z), ("minecraft:air",))[0]

    # The first detail pass left a suspended low stone net below the gallery.
    # Remove that net, including its five low lights, so the four enlarged
    # teaching wells really reveal the Y50 upper structure from ground level.
    former_net = {
        "minecraft:polished_andesite", "minecraft:chiseled_stone_bricks",
        "minecraft:dark_oak_planks", "minecraft:lantern", "minecraft:iron_chain",
        "minecraft:sea_lantern", "minecraft:amethyst_block",
    }
    for x in range(57, 80):
        for z in range(22, 45):
            if hypot(x - 68, z - 33) > 11:
                continue
            for y in range(30, 38):
                if old(x, y, z) in former_net:
                    b(x, y, z, "air")

    # Deep, dark coffers remain high above the occupied gallery. They vary in
    # width so the room reads as masonry work instead of a tiled flat ceiling.
    for x in range(58, 79):
        for z in range(23, 44):
            dx, dz = x - 68, z - 33
            if abs(dx) < 2 or abs(dz) < 2 or dx * dx + dz * dz > 11 * 11:
                continue
            if old(x, 50, z) == "minecraft:air":
                continue
            if abs(dx) in (5, 9) or abs(dz) in (5, 9):
                b(x, 49, z, "dark_oak_planks" if (x + z) % 5 else "polished_tuff")
            elif abs(dx) <= 8 and abs(dz) <= 8 and (x + z) % 3 == 0:
                b(x, 50, z, "deepslate_tiles")

    # Eight asymmetrically weighted supports grow from the teaching floor to
    # the upper ring. The broad bases and paired corbels give the vault a load
    # path rather than leaving decorative ribs floating in the air.
    piers = ((57, 25), (79, 25), (57, 41), (79, 41),
             (61, 20), (75, 20), (60, 46), (76, 46))
    for x, z in piers:
        for y in range(25, 44):
            b(x, y, z, "chiseled_stone_bricks" if y in (25, 26, 31, 38, 42, 43)
              else "polished_tuff" if y % 3 else "tuff_bricks")
        for y in (27, 39):
            for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                if hypot(x + dx - 68, z + dz - 33) < 18:
                    b(x + dx, y, z + dz, "stone_brick_stairs",
                      facing="east" if dx < 0 else "west" if dx > 0 else
                      "south" if dz < 0 else "north", half="top",
                      shape="straight", waterlogged="false")
        b(x, 44, z, "polished_deepslate")
        b(x, 45, z, "deepslate_tile_slab", type="bottom", waterlogged="false")

    # Four paired, rising stone vaults follow the donor's alternating full
    # block/slab/stair section. They cross the open quadrants well above the
    # Y38 walking bridge, with dark intrados and pale carved keystones.
    for sx in (-1, 1):
        for sz in (-1, 1):
            for radius in range(5, 13):
                for offset in (-1, 0, 1):
                    x = 68 + sx * round(radius * .707) + (offset if sz == sx else -offset)
                    z = 33 + sz * round(radius * .707) + offset
                    if not 55 <= x <= 81 or not 20 <= z <= 46:
                        continue
                    y = 49 - (radius - 5) // 2
                    b(x, y, z, "chiseled_stone_bricks" if radius in (5, 8, 11)
                      else "polished_tuff" if offset == 0 else "tuff_bricks")
                    if offset == 0 and radius % 2 == 0:
                        b(x, y - 1, z, "deepslate_tile_slab", type="top", waterlogged="false")
            # Shallow pointed ribs near the gallery's perimeter are supported
            # by the new piers, not generic Gothic flying buttresses.
            for leg in range(4):
                x = 68 + sx * (9 + leg)
                z = 33 + sz * (4 + leg)
                b(x, 43 - leg // 2, z, "polished_andesite")

    # Stone-framed lamplight up in each high well makes the new section legible
    # at night without flooding the floor with cold white blocks.
    for x, z in ((62, 27), (74, 27), (62, 39), (74, 39)):
        for y in range(46, 49):
            b(x, y, z, "iron_chain", axis="y", waterlogged="false")
        b(x, 45, z, "lantern", hanging="true", waterlogged="false")
        for dx, dz in ((-1, 0), (1, 0)):
            b(x + dx, 49, z + dz, "chiseled_stone_bricks")

    # A single slender bronze armillary hangs from the crossing. Copper
    # half-slabs and iron spokes keep the teaching vault visible through it;
    # the first full-block rim read as an oppressive plate in Minecraft.
    # The occupied upper crossing is directly above the hub at Y38. Its
    # center must stay clear at Y39 for the room anchor and gallery route.
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            radius = hypot(dx, dz)
            if 3.5 <= radius <= 4.5:
                b(68 + dx, 36, 33 + dz, "waxed_cut_copper_slab",
                  type="top", waterlogged="false")
                if (dx, dz) in ((-4, 0), (4, 0), (0, -4), (0, 4)):
                    b(68 + dx, 35, 33 + dz, "lantern", hanging="true", waterlogged="false")
            elif (dx == 0 or dz == 0) and radius <= 3:
                b(68 + dx, 36, 33 + dz, "iron_bars",
                  north="true" if dx == 0 else "false",
                  south="true" if dx == 0 else "false",
                  east="true" if dz == 0 else "false",
                  west="true" if dz == 0 else "false", waterlogged="false")
    b(68, 36, 33, "amethyst_block")
    b(68, 35, 33, "purple_stained_glass")
    for dx, dz in ((-3, -3), (-3, 3), (3, -3), (3, 3)):
        b(68 + dx, 37, 33 + dz, "chiseled_stone_bricks")

    # The central demonstrations happen inside a worn compass of dark slate,
    # tuff and copper. It is entirely flush with the Y24 walking floor.
    for dx in range(-5, 6):
        for dz in range(-5, 6):
            radius = hypot(dx, dz)
            x, z = 68 + dx, 33 + dz
            if 4.4 <= radius <= 5.2:
                b(x, 24, z, "polished_deepslate")
            elif 3 <= radius <= 4.4 and (dx == 0 or dz == 0):
                b(x, 24, z, "waxed_cut_copper")
            elif radius <= 3.6:
                b(x, 24, z, "polished_tuff" if (dx + dz) % 3 else "tuff_bricks")
    b(68, 24, 33, "amethyst_block")
    for dx, dz in ((-5, 0), (5, 0), (0, -5), (0, 5)):
        b(68 + dx, 24, 33 + dz, "amethyst_block")

    # The north teaching apse has two occupied tiers and a carved, illuminated
    # magical diagram. Keep the earlier enchanting table and wide aisle.
    for x in range(63, 74):
        for z in range(18, 25):
            if abs(x - 68) + abs(z - 21) > 8:
                continue
            b(x, 25, z, "polished_deepslate" if z in (18, 24) or x in (63, 73)
              else "polished_tuff")
            if z <= 21 and (x, z) != (68, 20):
                b(x, 26, z, "polished_andesite")
    for x in range(64, 73):
        b(x, 25, 25, "polished_tuff_stairs", facing="south", half="bottom",
          shape="straight", waterlogged="false")
    for x in (62, 74):
        for z in (19, 23):
            for y in range(26, 35):
                b(x, y, z, "chiseled_stone_bricks" if y in (26, 31, 34)
                  else "tuff_bricks")
            b(x, 35, z, "sea_lantern")
    # A dark, recessed slate field gives the north wall a deliberate focal
    # relief. Four amethyst arcs frame a lit central rune; narrow copper
    # inlays and carved jambs replace the first pass's noisy checkerboard.
    for x in range(63, 74):
        for y in range(28, 39):
            if x in (63, 73) or y in (28, 38):
                b(x, y, 17, "chiseled_stone_bricks")
            else:
                b(x, y, 17, "polished_deepslate")
    for x in (64, 72):
        for y in range(29, 38):
            b(x, y, 18, "waxed_cut_copper" if y in (29, 33, 37) else "polished_tuff")
    for x in range(65, 72):
        for y in range(30, 37):
            dist = abs(x - 68) + abs(y - 33)
            if dist == 3:
                b(x, y, 17, "amethyst_block" if x == 68 or y == 33 else "budding_amethyst")
            elif (x == 68 and y in (31, 35)) or (y == 33 and x in (66, 70)):
                b(x, y, 17, "waxed_cut_copper")
    b(68, 33, 17, "sea_lantern")
    b(68, 33, 18, "purple_stained_glass")
    for x in (65, 71):
        b(x, 27, 21, "lectern", facing="south", has_book="false", powered="false")
    b(68, 26, 23, "lectern", facing="south", has_book="false", powered="false")
    for x in (66, 70):
        b(x, 26, 23, "waxed_cut_copper_slab", type="bottom", waterlogged="false")

    # The teaching wall's two blank wings become unequal research niches.
    # Their carved backs sit north of the instructor and active stations;
    # the central rune and all approaches to the raised dais remain clear.
    for x0, x1 in ((57, 61), (75, 79)):
        for x in range(x0, x1 + 1):
            for y in range(28, 36):
                b(x, y, 18, "chiseled_stone_bricks" if x in (x0, x1) or
                  y in (28, 35) else "dark_oak_planks" if y in (31, 34)
                  else "polished_deepslate")
        for x in range(x0 + 1, x1):
            for y in (29, 30, 32, 33):
                b(x, y, 19, "bookshelf" if x != x0 + 2 or y < 32
                  else "chiseled_bookshelf", **({} if x != x0 + 2 or y < 32
                  else {"facing": "south"}))
        for x in (x0, x1):
            b(x, 36, 18, "polished_tuff_stairs", facing="south", half="top",
              shape="straight", waterlogged="false")
    b(59, 34, 19, "amethyst_block")
    b(59, 35, 19, "orange_stained_glass")
    b(77, 34, 19, "waxed_cut_copper")
    b(77, 35, 19, "lantern", hanging="false", waterlogged="false")

    # The east side is a continuous, recessed teaching archive beneath the
    # gallery, while the opposite daylight bay has a carved window surround.
    # These unequal treatments replace the broad identical gray side walls.
    for z in range(29, 39):
        for y in range(29, 38):
            if z in (29, 34, 38):
                b(79, y, z, "chiseled_stone_bricks" if y in (29, 33, 37)
                  else "polished_tuff")
            else:
                b(80, y, z, "polished_deepslate" if y in (29, 33, 37)
                  else "tuff_bricks")
                if z in (31, 32, 36, 37) and y in (30, 31, 32):
                    b(79, y, z, "bookshelf" if y != 31 else "chiseled_bookshelf",
                      **({} if y != 31 else {"facing": "west"}))
    for z in (30, 35):
        b(78, 33, z, "chiseled_stone_bricks")
        b(78, 34, z, "lantern", hanging="false", waterlogged="false")
    for z in (29, 38):
        for y in range(29, 37):
            b(57, y, z, "chiseled_stone_bricks" if y in (29, 33, 36)
              else "polished_tuff")
    for z in range(30, 38):
        b(57, 37, z, "polished_deepslate" if z % 3 else "chiseled_stone_bricks")

    # Real three-level seating decks and individual writing places replace
    # three rows of identical stair benches. The center is five blocks wide;
    # both side aisles and the stations remain clear.
    for x in list(range(59, 66)) + list(range(71, 78)):
        for z in range(35, 49):
            for y in range(25, 29):
                if old(x, y, z) in ("minecraft:spruce_stairs", "minecraft:dark_oak_slab"):
                    b(x, y, z, "air")
    for z, deck in ((37, 24), (42, 25), (47, 26)):
        for x in list(range(60, 66)) + list(range(71, 77)):
            for zz in range(z - 1, z + 2):
                for y in range(24, deck + 1):
                    b(x, y, zz, "polished_deepslate" if y == deck else "stone_bricks")
            # Paired individual scholars' chairs and desk leaves interrupt the
            # former continuous timber bands. Carved stone standards take the
            # place of every third seat and make each terrace a designed bay.
            if x in (60, 62, 64, 71, 73, 75):
                b(x, deck + 1, z + 1, "spruce_stairs", facing="north", half="bottom",
                  shape="straight", waterlogged="false")
                b(x, deck + 1, z - 1, "dark_oak_slab", type="top", waterlogged="false")
            elif x in (61, 74):
                b(x, deck + 1, z + 1, "chiseled_stone_bricks")
                b(x, deck + 2, z + 1, "waxed_cut_copper_slab",
                  type="bottom", waterlogged="false")
                b(x, deck + 1, z - 1, "chiseled_stone_bricks")
            else:
                b(x, deck + 1, z + 1, "dark_oak_planks")
                b(x, deck + 1, z - 1, "dark_oak_planks")
        for x in (59, 77):
            b(x, deck + 1, z, "chiseled_stone_bricks")
            b(x, deck + 2, z, "lantern", hanging="false", waterlogged="false")
        for x in (62, 74):
            b(x, deck + 2, z - 1, "lectern", facing="north", has_book="false", powered="false")
    for x in (59, 77):
        b(x, 25, 40, "polished_tuff_stairs", facing="south", half="bottom",
          shape="straight", waterlogged="false")
        b(x, 26, 45, "polished_tuff_stairs", facing="south", half="bottom",
          shape="straight", waterlogged="false")

    # The high north teaching gallery should read as an inhabited academic
    # wall, not a window stranded in a gray cylinder. A deep west book court
    # and an unequal eastern instrument cabinet flank the preserved window,
    # with a shared carved lintel carried by their stone jambs. Both sit
    # against the back wall; the Y38 walking ring and north lectern stay open.
    for x in (59, 63, 73, 77):
        for y in range(39, 48):
            b(x, y, 18, "chiseled_stone_bricks" if y in (39, 43, 47)
              else "polished_tuff")
        b(x, 48, 18, "polished_tuff_stairs", facing="south", half="top",
          shape="straight", waterlogged="false")
    for x in range(60, 63):
        for y in range(40, 47):
            b(x, y, 18, "dark_oak_planks" if y in (43, 46)
              else "chiseled_stone_bricks" if y == 40 else "bookshelf")
        b(x, 47, 19, "polished_deepslate")
    for x in range(74, 77):
        for y in range(40, 47):
            b(x, y, 18, "dark_oak_planks" if y in (40, 46)
              else "polished_deepslate")
    for x, y in ((75, 42), (74, 43), (76, 43), (75, 44)):
        b(x, y, 19, "waxed_cut_copper")
    b(75, 43, 19, "amethyst_block")
    for x in range(63, 74):
        b(x, 47, 18, "chiseled_stone_bricks" if x in (63, 68, 73)
          else "dark_oak_planks")
    for x in (61, 75):
        b(x, 46, 19, "iron_chain", axis="y", waterlogged="false")
        b(x, 45, 19, "lantern", hanging="true", waterlogged="false")
