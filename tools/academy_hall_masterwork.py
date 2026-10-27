"""Final, room-specific stonework for the Hall of Elements.

The donor Hall's inhabited annulus and alternating thick/thin masonry courses
inform this new composition. All work is fitted to Frostward's existing tower,
teaching floor, gallery, stairs, stations and upper suite; no donor room is
transplanted. See the Fonta59 adaptation ledger for the bounded source region.
"""
from retro_game_shop import state


def craft_hall_masterwork(cells, put):
    def b(x, y, z, name, **props):
        put(x, y, z, state(name if ":" in name else "minecraft:" + name, **props))

    def old(x, y, z):
        return cells.get((x, y, z), ("minecraft:air",))[0]

    # The first runtime capture still exposed a broad, flat Y38 gallery lip.
    # Open two larger central bays toward the north and south walls. Preserve
    # the narrow occupied Y38 wall walks at Z20–21/Z45–46 and the three-wide
    # east-west crossing through the named gallery anchor.
    for x in range(63, 74):
        for z in (*range(22, 26), *range(41, 45)):
            for y in range(38, 41):
                b(x, y, z, "air")
    for z in (21, 45):
        for x in (*range(63, 67), *range(70, 74)):
            if old(x, 38, z) != "minecraft:air":
                b(x, 39, z, "chiseled_stone_bricks")

    # Two high, unequal barrel bays rise through the existing teaching wells.
    # The four ribs are stepped in whole stone, shallow stair and slab courses;
    # dark coffer fields between them make their height legible from the floor.
    # Y38 gallery floors and Y50 private-suite floor remain untouched.
    for north, z_courses in ((True, (24, 28)), (False, (38, 42))):
        for z in z_courses:
            for x in range(59, 78):
                span = abs(x - 68)
                rise = max(0, 9 - span)
                y = 41 + min(7, (rise * 7) // 9)
                if span <= 7:
                    b(x, y, z, "chiseled_stone_bricks" if span in (0, 4, 7)
                      else "polished_tuff")
                    if span in (2, 5):
                        b(x, y - 1, z, "polished_tuff_stairs",
                          facing="east" if x < 68 else "west", half="top",
                          shape="straight", waterlogged="false")
                    if span in (1, 6):
                        b(x, y - 1, z, "deepslate_tile_slab", type="top",
                          waterlogged="false")
                elif span == 8:
                    b(x, 42, z, "tuff_bricks")
                    b(x, 43, z, "polished_tuff_stairs",
                      facing="east" if x < 68 else "west", half="top",
                      shape="straight", waterlogged="false")
                else:
                    b(x, 41, z, "chiseled_stone_bricks")
        z0, z1 = z_courses
        for z in range(z0 + 1, z1):
            for x in range(61, 76):
                if old(x, 50, z) == "minecraft:air":
                    continue
                b(x, 49, z, "dark_oak_planks" if x in (62, 68, 74)
                  else "deepslate_tiles" if (x + z) % 4
                  else "polished_tuff")
        # A bronze-edged lantern well is inset into each vault, not hung as a
        # bright cube below it. The gallery has a clear view past both bays.
        lamp_z = 26 if north else 40
        for x in (66, 70):
            b(x, 48, lamp_z, "waxed_cut_copper_slab", type="top",
              waterlogged="false")
            b(x, 47, lamp_z, "lantern", hanging="true", waterlogged="false")

    # Give the crossing bridge a layered underside. Its narrow timber spine
    # and paired stone brackets visually connect the ribs without closing the
    # tall wells or adding a low false ceiling over the central compass.
    for z in range(23, 44):
        if z in (32, 33, 34):
            continue
        for x in (66, 70):
            if old(x, 38, z) == "minecraft:air":
                continue
            b(x, 37, z, "dark_oak_planks")
            if z in (26, 30, 36, 40):
                b(x, 36, z, "polished_tuff_stairs", facing="north" if z < 33
                  else "south", half="top", shape="straight", waterlogged="false")

    # Four great piers gain three-dimensional capitals and bronze fillets.
    # Their shafts are grounded by the earlier pass; the new brackets lead the
    # eye up to the crossing gallery instead of repeating one stone texture.
    for x, z in ((57, 25), (79, 25), (57, 41), (79, 41)):
        inward = 1 if x < 68 else -1
        for y in (31, 36, 41):
            b(x + inward, y, z, "polished_tuff_stairs",
              facing="west" if inward > 0 else "east", half="top",
              shape="straight", waterlogged="false")
        for y in range(29, 36):
            b(x, y, z + (1 if z < 33 else -1),
              "dark_oak_planks" if y in (30, 34) else "polished_deepslate")
        b(x, 35, z + (1 if z < 33 else -1), "waxed_cut_copper_slab",
          type="top", waterlogged="false")

    # The upper north wall is an occupied academic screen around its original
    # window. The south-facing book court and unequal instrument recess were
    # built in the previous pass; stepped arches and dark horizontal ties now
    # bind them to the stone jambs. No block enters the Y39 walking ring.
    for x0, x1 in ((59, 63), (73, 77)):
        for x in range(x0, x1 + 1):
            edge = min(x - x0, x1 - x)
            y = 48 + edge // 2
            b(x, y, 19, "chiseled_stone_bricks" if x in (x0, x1)
              else "polished_tuff")
            if x not in (x0, x1):
                b(x, y - 1, 19, "polished_tuff_stairs", facing="south",
                  half="top", shape="straight", waterlogged="false")
        for y in (40, 44):
            b(x0, y, 19, "waxed_cut_copper_slab", type="top",
              waterlogged="false")
            b(x1, y, 19, "waxed_cut_copper_slab", type="top",
              waterlogged="false")
    for x in range(63, 74):
        if x in (67, 68, 69):
            continue  # blue clerestory and the north lecture sightline
        b(x, 45, 19, "dark_oak_planks")
        b(x, 46, 19, "polished_tuff_stairs", facing="south", half="top",
          shape="straight", waterlogged="false")

    # These are Nordic stepped stone arches, with broad rounded crowns rather
    # than narrow Gothic points. Their feet land on the previous grounded
    # piers, and the newly opened gallery bays expose their intrados from the
    # entrance. A dark timber soffit distinguishes the structure from the
    # surrounding light masonry.
    for z in (23, 43):
        for x in range(61, 76):
            d = abs(x - 68)
            y = 45 - (d * d) // 10
            b(x, y, z, "chiseled_stone_bricks" if d in (0, 4, 7)
              else "polished_tuff")
            if d <= 5:
                b(x, y - 1, z, "dark_oak_planks" if d in (2, 3)
                  else "polished_tuff_stairs", **({} if d in (2, 3)
                  else {"facing": "east" if x < 68 else "west",
                        "half": "top", "shape": "straight",
                        "waterlogged": "false"}))
        for x in (61, 75):
            for y in range(28, 39):
                if old(x, y, z) == "minecraft:air":
                    b(x, y, z, "polished_tuff" if y % 4 else "tuff_bricks")
            b(x, 39, z, "chiseled_stone_bricks")
            b(x, 40, z, "polished_tuff")

    # Lower transept ribs finally reveal the vault profile from the entrance.
    # Their feet stand beside, rather than on, the two working Lampwright
    # stations, and the arch crosses only the already open high teaching well.
    for z in (27, 39):
        for x in (58, 78):
            for y in range(25, 32):
                b(x, y, z, "chiseled_stone_bricks" if y in (25, 30, 31)
                  else "polished_tuff")
        for x in range(58, 79):
            d = abs(x - 68)
            y = 38 - (d * d * 7) // 100
            b(x, y, z, "chiseled_stone_bricks" if d in (0, 5, 10)
              else "polished_tuff")
            if d in (2, 4, 7):
                b(x, y - 1, z, "deepslate_tile_slab", type="top",
                  waterlogged="false")

    # The occupied east-west bridge keeps its floor, but a full-width dark
    # coffer and six visible carved transverse ties replace the pale flat
    # soffit seen in the second Minecraft capture.
    for x in range(58, 79):
        for z in range(32, 35):
            if old(x, 38, z) == "minecraft:air":
                continue
            b(x, 37, z, "chiseled_stone_bricks" if x in (60, 64, 68, 72, 76)
              else "dark_oak_planks" if z in (32, 34)
              else "deepslate_tiles")
            if x in (60, 64, 72, 76) and z in (32, 34):
                b(x, 36, z, "polished_tuff_stairs",
                  facing="east" if x < 68 else "west", half="top",
                  shape="straight", waterlogged="false")

    # A four-block-deep rear teaching console makes the apse an institution's
    # working lectern rather than an empty altar. The enchanted table at
    # (68,26,20), center lectern, and side lecterns retain their exact cells.
    for x in range(64, 73):
        for z in (21, 22):
            if (x, z) in ((65, 21), (68, 21), (68, 22), (71, 21)):
                continue
            b(x, 27, z, "dark_oak_slab", type="top", waterlogged="false")
        if x in (64, 68, 72):
            b(x, 27, 21, "chiseled_stone_bricks")
            b(x, 28, 21, "waxed_cut_copper_slab", type="bottom",
              waterlogged="false")
    for x in (63, 73):
        for z in (21, 24):
            b(x, 26, z, "polished_tuff")
            b(x, 27, z, "dark_oak_trapdoor", facing="south", half="top",
              open="false", powered="false", waterlogged="false")
    for x, material in ((61, "amethyst_block"), (75, "waxed_cut_copper")):
        # Separate demonstration plinths flank the instructor but stay clear
        # of the two original Lampwright benches at (57/79,25,28).
        b(x, 25, 24, "chiseled_stone_bricks")
        b(x, 26, 24, material)
        b(x, 27, 24, "iron_bars", north="false", south="false",
          east="false", west="false", waterlogged="false")
        b(x, 28, 24, "lantern", hanging="false", waterlogged="false")

    # Distinct floor-level research pockets complement the eastern archive:
    # shallow walnut worktops, specimen plinths, and book returns. Their
    # footprints sit outside the five-wide central procession and stairways.
    for x0, side in ((60, -1), (76, 1)):
        for z in (30, 32):
            b(x0, 25, z, "dark_oak_planks")
            b(x0, 26, z, "dark_oak_slab", type="top", waterlogged="false")
        b(x0, 25, 31, "chiseled_stone_bricks")
        b(x0, 26, 31, "cartography_table" if side < 0 else "smithing_table")
        b(x0 + side, 25, 30, "bookshelf")
        b(x0 + side, 26, 30, "chiseled_bookshelf",
          facing="east" if side < 0 else "west")
        b(x0 + side, 27, 30, "polished_tuff_stairs",
          facing="east" if side < 0 else "west", half="top",
          shape="straight", waterlogged="false")

    # Bring the professor's active demonstration into the middle of the
    # auditorium instead of leaving a broad empty gray floor before the
    # northern apse. This low, nine-wide instrument deck meets the older
    # compass and side plinths while leaving the named center anchor at
    # (68,25,33), both side routes and the Y26 soul lantern unobstructed.
    for x in range(64, 73):
        for z in range(28, 32):
            edge = x in (64, 72) or z in (28, 31)
            b(x, 25, z, "polished_deepslate" if edge
              else "dark_oak_planks" if (x + z) % 3 else "polished_tuff")
    for x in range(65, 72):
        b(x, 25, 32, "polished_tuff_stairs", facing="south", half="bottom",
          shape="straight", waterlogged="false")
    for x, material in ((65, "amethyst_block"), (71, "waxed_cut_copper")):
        b(x, 26, 29, material)
        b(x, 27, 29, "end_rod", facing="up")
        b(x, 26, 31, "chiseled_stone_bricks")
        b(x, 27, 31, "lantern", hanging="false", waterlogged="false")
    for x in (66, 70):
        b(x, 26, 30, "dark_oak_slab", type="top", waterlogged="false")
    b(68, 26, 30, "lectern", facing="south", has_book="false",
      powered="false")

    # The tower front is curved in plan: at Y49 its outer stone lies at Z49,
    # Z50 or Z51 depending on X. The first runtime panel mistakenly assumed
    # one flat Z51 wall and appeared as a narrow center stripe. Fit each bay
    # to its actual outermost stone, retaining the existing upper windows and
    # south processional threshold. The result is a broad, recessed Nordic
    # heraldic screen with thick light jambs and a slate/bronze center.
    def frontage(x, y):
        for z in range(52, 47, -1):
            if old(x, y, z) in ("minecraft:stone_bricks",
                                "minecraft:chiseled_stone_bricks",
                                "minecraft:polished_andesite",
                                "minecraft:polished_tuff",
                                "minecraft:tuff_bricks",
                                "minecraft:polished_deepslate",
                                "minecraft:deepslate_tiles"):
                return z
        return None

    for x in range(63, 74):
        for y in range(45, 54):
            z = frontage(x, y)
            if z is None:
                continue
            frame = x in (63, 73) or y in (45, 53)
            b(x, y, z, "chiseled_stone_bricks" if frame
              else "polished_deepslate" if (x + y) % 5
              else "deepslate_tiles")
            if x in (63, 73):
                b(x, y, z + 1, "polished_tuff" if y not in (45, 49, 53)
                  else "chiseled_stone_bricks")
            if y == 53:
                b(x, y + 1, z + 1, "deepslate_tile_slab", type="bottom",
                  waterlogged="false")
    for x, y in ((68, 47), (67, 48), (69, 48), (66, 49),
                 (70, 49), (67, 50), (69, 50), (68, 51)):
        z = frontage(x, y)
        if z is not None:
            b(x, y, z + 1, "waxed_cut_copper_slab", type="top",
              waterlogged="false")
    center_z = frontage(68, 49)
    if center_z is not None:
        b(68, 49, center_z + 1, "amethyst_block")
    for x in (61, 75):
        for y in range(44, 61):
            z = frontage(x, y)
            if z is not None:
                b(x, y, z + 1, "chiseled_stone_bricks" if y in (44, 50, 56, 60)
                  else "polished_tuff")

    # The first horizontal hoop and the second copper meridians both hid the
    # architecture in Minecraft. Clear those old fixture cells and hang a
    # slender iron instrument with four warm lamps below the newly coffered
    # bridge. Its lit violet heart stays six blocks over the walking floor.
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            x, z = 68 + dx, 33 + dz
            for y in range(31, 38):
                if old(x, y, z) in ("minecraft:waxed_cut_copper",
                                    "minecraft:waxed_cut_copper_slab",
                                    "minecraft:iron_bars", "minecraft:lantern",
                                    "minecraft:amethyst_block",
                                    "minecraft:purple_stained_glass"):
                    b(x, y, z, "air")
    for d in range(-3, 4):
        if d == 0:
            continue
        for x, z in ((68 + d, 33), (68, 33 + d)):
            b(x, 35, z, "iron_bars", north="true", south="true",
              east="true", west="true", waterlogged="false")
    for y in (35, 36):
        b(68, y, 33, "iron_chain", axis="y", waterlogged="false")
    b(68, 34, 33, "amethyst_block")
    b(68, 33, 33, "purple_stained_glass")
    b(68, 32, 33, "lantern", hanging="true", waterlogged="false")
    for x, z in ((64, 33), (72, 33), (68, 29), (68, 37)):
        b(x, 34, z, "waxed_cut_copper_slab", type="top", waterlogged="false")
        b(x, 33, z, "lantern", hanging="true", waterlogged="false")
