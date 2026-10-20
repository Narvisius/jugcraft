"""Batch 12 absolute tree shares, preserving each biome's existing attempt budget."""

SHARES = {
    'rainforest': [('jugcraft:giant_mahogany_checked', .12), ('jugcraft:emergent_mahogany_checked', .07),
                   ('jugcraft:rubber_tree_checked', .12), ('jugcraft:tree_fern_checked', .09),
                   ('jugcraft:banana_checked', .10), ('jugcraft:mahogany_checked', .50)],
    'tropics': [('jugcraft:small_palm_checked', .25), ('jugcraft:flame_tree_checked', .10),
                ('jugcraft:banana_checked', .15), ('minecraft:jungle_bush', .0765), ('jugcraft:palm_checked', .4235)],
    'lush_grassland': [('jugcraft:oak_bush_checked', .25), ('jugcraft:mango_checked', .20),
                       ('jugcraft:small_palm_checked', .10), ('jugcraft:banana_checked', .45)],
    'eucalyptus_forest': [('jugcraft:big_eucalyptus_checked', .25), ('jugcraft:wattle_bush_checked', .20),
                          ('jugcraft:giant_eucalyptus_checked', .08), ('jugcraft:tree_fern_checked', .05),
                          ('jugcraft:eucalyptus_checked', .42)],
    'temperate_rainforest': [('jugcraft:redwood_checked', .30), ('jugcraft:tall_fir_checked', .14),
                            ('jugcraft:mossy_maple_checked', .11), ('jugcraft:willow_checked', .04),
                            ('jugcraft:tree_fern_checked', .03), ('jugcraft:fir_checked', .38)],
}


def configure(biomes):
    for biome, shares in SHARES.items():
        remaining, picks = 1., []
        for feature, share in shares[:-1]:
            picks.append([feature, share / remaining])
            remaining -= share
        assert abs(remaining - shares[-1][1]) < 1e-9
        biomes[biome]['trees'].update(default=shares[-1][0], picks=picks)
