"""Batch 9/9b additions to existing biome budgets; counts are attempts, not guaranteed growth."""
from copy import deepcopy


def configure(biomes, extras):
    original = dict(extras)
    def layer(name, feature, count):
        extras[name]={'feature':'jugcraft:'+feature,'step':9,'placement':[
            {'type':'minecraft:count_on_every_layer','count':count},{'type':'minecraft:biome'}]}
    def surface(name, feature, count=1, rarity=None, floor=None):
        placement=([{'type':'minecraft:rarity_filter','chance':rarity}] if rarity else [])
        placement += [{'type':'minecraft:count','count':count},{'type':'minecraft:in_square'},
                      {'type':'minecraft:heightmap','heightmap':'MOTION_BLOCKING_NO_LEAVES'},{'type':'minecraft:biome'}]
        if floor: placement.append({'type':'minecraft:block_predicate_filter','predicate':{
            'type':'minecraft:matching_blocks','blocks':floor,'offset':[0,-1,0]}})
        extras[name]={'feature':'jugcraft:'+feature,'step':9,'placement':placement}
    for name,feature,count in (('thicket_huge_glowcaps','huge_glowcap',2),('sands_blight_fungi','huge_blight',1),
                               ('hollow_blight_fungi','huge_blight',3),('marrow_fungi','huge_marrow',1),
                               ('netherbrush_bushes','huge_bramble',4),('sands_bramble_bushes','huge_bramble',2),
                               ('nether_small_blight','blight_fungus',4)):
        layer(name,feature,count)
    surface('field_huge_glowcaps','huge_glowcap')
    surface('rotted_blight_fungi','huge_blight',floor='minecraft:soul_soil')
    surface('outer_dragonblood','dragonblood',rarity=10,floor='minecraft:end_stone')
    cave=deepcopy(extras['grotto_glowcaps']['placement'])
    cave[0]['count']={'type':'minecraft:uniform','min_inclusive':2,'max_inclusive':4}
    cave[2]={'type':'jugcraft:cave_fungal_floor'}
    extras['grotto_huge_glowcaps']={'feature':'jugcraft:huge_glowcap','step':9,'placement':cave}
    additions={
        'fungal_thicket':['thicket_huge_glowcaps'], 'glowcap_grotto':['grotto_huge_glowcaps'],
        'toadstool_field':['field_huge_glowcaps'], 'blighted_sands':['sands_blight_fungi','sands_bramble_bushes','nether_small_blight'],
        'withered_hollow':['hollow_blight_fungi','nether_small_blight'], 'marrow_heap':['marrow_fungi'],
        'netherbrush':['netherbrush_bushes'], 'rotted_expanse':['rotted_blight_fungi'], 'outer_flats':['outer_dragonblood']}
    for biome,names in additions.items():
        # Giants first, so tiny plants placed later cannot veto an otherwise clear trunk/cap.
        biomes[biome]['extras']=names+biomes[biome].get('extras',[])
    # Sequential selector probabilities: leave other existing shares unchanged, remove 7 points from bushes.
    biomes['mycelial_jungle']['trees']['picks'].append(['jugcraft:huge_glowcap',.07/(.7*.85*.85)])
    biomes['ender_wilds']['trees']={'count':[2,3], 'default':'jugcraft:jacaranda_checked', 'picks':[
        ['jugcraft:dragonblood_checked',.45], ['jugcraft:dragonblood_shrub_checked',.20/.55], ['jugcraft:huge_glowcap',.05/.35]]}

    new = {k:v for k,v in extras.items() if k not in original}
    extras.clear()
    grotto = new.pop("grotto_huge_glowcaps")
    extras.update(new)
    for key, value in original.items():
        if key == "grotto_glowcaps": extras["grotto_huge_glowcaps"] = grotto
        extras[key] = value
