"""Separate audited overlapping surfaces without replacing supplied meshes or pixels."""
import copy
from model_writer import finish_elements, finish_closed

CLOSED = {'jimtafacts/ae2/block/' + name for name in ('charger','crank_handle','drive_base','inscriber','molecular_assembler','sky_stone_tank')}

REPAIRED = {
    'jimtafacts/spellbooks/block/brazier_template',
    'jimtafacts/spellbooks/block/hanging_brazier_template',
    'jimtafacts/spellbooks/block/brazier_off',
    'jimtafacts/spellbooks/block/hanging_brazier_off',
    'block/jimta_programming_table', 'item/jimtafacts/mimic_spawn_egg',
    'jimtafacts/ae2/block/me_chest_lights_on',
    'jimtafacts/ae2/block/quartz_fixture_wall', 'jimtafacts/ae2/block/quartz_fixture_wall_odd',
    'jimtafacts/ae2/item/color_applicator', 'jimtafacts/ae2/item/color_applicator_colored',
    'jimtafacts/ae2/part/transition_plane_has_channel', 'jimtafacts/ae2/part/transition_plane_off',
    'jimtafacts/spellbooks/block/scroll_forge', 'jimtafacts/spellbooks/block/ice_spider_egg_frosted',
    'jimtafacts/spellbooks/item/staff_of_the_nines',
} | {'jimtafacts/spellbooks/block/book_stack_' + n for n in ('02','04','05','08','09','10','11','14')}

def repair(ref, model):
    if ref not in REPAIRED | CLOSED or not model.get('elements'): return model
    model=copy.deepcopy(model)
    # Retain the supplied inward-facing shell and its UVs. Its floor coincides
    # with the pedestal lid, so recess only the latter by a tenth of a pixel.
    if ref.rsplit('/',1)[-1] in ('brazier_template','hanging_brazier_template','brazier_off','hanging_brazier_off'):
        model['elements'][2]['to'][1] -= .1
        return model
    if ref in CLOSED: finish_closed(model['elements'])
    else: finish_elements(model['elements'])
    if ref.endswith('/ice_spider_egg_frosted'):
        # The transparent frost layer extends below zero, outside the generic
        # separation pass's boundary clamp. Keep that shell clear of the egg.
        model['elements'][1]['from'][1] = -.1
    return model
