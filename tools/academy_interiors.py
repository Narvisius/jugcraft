"""Authored stone interiors; no loot or additional progression systems."""
from retro_game_shop import state


def furnish(cells, put):
    def block(x, y, z, name, **props):
        put(x, y, z, state('minecraft:' + name, **props))

    # Retain structural wood only where its shape/door mechanics matter. Broad
    # plank surfaces become pale carved stone, with dark library floor inlays.
    for (x, y, z), old in list(cells.items()):
        if old[0] == 'minecraft:spruce_planks' and y >= 24:
            block(x, y, z, 'polished_diorite')
        if 94 <= x <= 130 and 16 <= z <= 52 and 25 <= y <= 35:
            if 'glass' in old[0]:
                block(x, y, z, 'stone_bricks')

    def box(x0, x1, y0, y1, z0, z1, name):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    block(x, y, z, name)

    # Keep the central entrance/Conclave axis open. Two side suites contain
    # windowless stacks and research alcoves, connected through broad arches.
    for x in (104, 120):
        box(x, x, 25, 32, 24, 44, 'stone_bricks')
        for z in (29, 39):
            box(x, x, 25, 28, z-1, z+1, 'air')
            box(x, x, 29, 29, z-1, z+1, 'chiseled_stone_bricks')
        for z in (24, 34, 44):
            box(x, x, 25, 31, z, z, 'quartz_pillar')
            block(x, 32, z, 'chiseled_quartz_block')

    # Two end-to-end stack rooms on each side; passages at each end and along
    # the outer wall remain at least two blocks wide.
    for x0, x1 in ((99, 102), (122, 125)):
        for z in (26, 34, 42):
            box(x0, x1, 25, 28, z, z, 'bookshelf')
            box(x0, x1, 29, 29, z, z, 'chiseled_quartz_block')
        box(x0, x1, 30, 30, 34, 34, 'sea_lantern')

    # Reading islands: stone tables, individual facing seats, light pedestals.
    for x in (108, 116):
        for z in (29, 39):
            block(x, 25, z, 'quartz_pillar')
            block(x, 26, z, 'smooth_quartz_slab', type='top', waterlogged='false')
            for dx, facing in ((-1, 'east'), (1, 'west')):
                block(x+dx, 25, z, 'polished_diorite_stairs', facing=facing,
                      half='bottom', shape='straight', waterlogged='false')
            block(x, 31, z, 'sea_lantern')
            for dx in (-1, 1):
                block(x+dx, 31, z, 'chiseled_quartz_block')

    # Inlaid stone floor and a coffered ceiling make the main aisle legible.
    for x in range(98, 127):
        for z in range(22, 47):
            if cells.get((x, 24, z), ('',))[0] == 'minecraft:polished_diorite':
                block(x, 24, z, 'polished_deepslate' if x in (105, 119) or z in (24, 44)
                      else 'polished_diorite')
    for x in range(105, 120):
        for z in range(24, 45):
            block(x, 33, z, 'chiseled_quartz_block' if x % 4 == 0 or z % 5 == 0
                  else 'smooth_quartz')
