"""Collection recipes using the existing finite-fluid Chemical Reactor."""
def recipes():
    return {name: {'type':'jugcraft:chemical_reaction',
                  'fabric:load_conditions':[{'condition':'jugcraft:feature_enabled','feature':'machines'}],
                  'items':[{'ingredient':'minecraft:lapis_lazuli','count':lapis}],
                  'fluids':[{'fluid':water,'amount':1000}],
                  'fluid_results':[{'fluid':'jugcraft:jimta_coolant','amount':1000}], 'time':100}
            for name, water, lapis in [('jimta_coolant','minecraft:water',2),
                                        ('jimta_distilled_coolant','jugcraft:distilled_water',1)]}
