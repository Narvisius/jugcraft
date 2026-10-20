"""Batch 11: absolute selection shares converted to vanilla's sequential chances."""


def configure(biomes, extras):
    def mix(biome, shares):
        remaining = 1.0
        picks = []
        for feature, share in shares[:-1]:
            picks.append([feature, share / remaining])
            remaining -= share
        assert abs(remaining - shares[-1][1]) < 1e-9
        biomes[biome]['trees'].update(default=shares[-1][0], picks=picks)

    mix('tropics', [('jugcraft:small_palm_checked', .25), ('minecraft:jungle_bush', .15),
                    ('jugcraft:flame_tree_checked', .10), ('jugcraft:banana_checked', .0765),
                    ('jugcraft:palm_checked', .4235)])
    mix('jacaranda_glade', [('minecraft:cherry_checked', .07), ('minecraft:oak_bees_002', .17),
                            ('jugcraft:azalea_tree_checked', .068), ('jugcraft:big_jacaranda_checked', .15),
                            ('jugcraft:flame_tree_checked', .08), ('jugcraft:jacaranda_checked', .462)])
    mix('overgrown_beach', [('jugcraft:palm_checked', .2), ('jugcraft:small_palm_checked', .2),
                           ('jugcraft:sea_grape_checked', .6)])
    mix('karst_pinnacles', [('jugcraft:spruce_bush_checked', .2), ('jugcraft:osmanthus_checked', .15),
                           ('jugcraft:pine_checked', .65)])
    mix('shrine_springs', [('minecraft:jungle_bush', .3), ('jugcraft:banyan_checked', .14),
                          ('jugcraft:great_oak_checked', .56)])
    mix('lush_grassland', [('jugcraft:oak_bush_checked', .3), ('jugcraft:mango_checked', .2),
                          ('jugcraft:small_palm_checked', .1), ('minecraft:jungle_bush', .4)])
    for biome, rarity in [('dune_beach', 6), ('gravel_beach', 12)]:
        name = biome + '_driftwood'
        extras[name] = {'configured': {'type': 'jugcraft:driftwood'}, 'step': 9, 'placement': [
            {'type': 'minecraft:rarity_filter', 'chance': rarity}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING_NO_LEAVES'}, {'type': 'minecraft:biome'}]}
        biomes[biome].setdefault('extras', []).append(name)
