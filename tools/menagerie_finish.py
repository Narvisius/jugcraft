"""Shared acquisitions, variant wiring and compatibility details for the native menagerie."""
from menagerie_content import *

def item_model(id,texture):
    write(ASSET/f'models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':texture}})
    write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}})

def recipe(id,inputs,result,count=1):write(DATA/f'recipe/{id}.json',{'type':'minecraft:crafting_shapeless','ingredients':inputs,'result':{'id':result,'count':count}})

def main():
    from menagerie_data import write_loot
    write_loot()
    write(ASSET/'items/wildlife_knapsack.json',{'model':{'type':'minecraft:condition','property':'minecraft:component','predicate':'minecraft:custom_data','value':'{BagFilled:1b}','on_true':{'type':'minecraft:model','model':'jugcraft:menagerie/wildlife/item/knapsack_filled'},'on_false':{'type':'minecraft:model','model':'jugcraft:menagerie/wildlife/item/knapsack'}}})
    c=read(RES/'jugcraft/menagerie/catalog.json');lang=read(ASSET/'lang/en_us.json')
    atlas_path=RES/'assets/minecraft/atlases/blocks.json';atlas=read(atlas_path) if atlas_path.exists() else {'sources':[]}
    for group in ['wildlife','mount','pocket','herbal']:
        for kind in ['block','item']:
            directory=f'menagerie/{group}/{kind}'
            entry={'type':'minecraft:directory','source':directory,'prefix':directory+'/'}
            if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(atlas_path,atlas)
    directory='menagerie/wildlife/entity/snail'
    entry={'type':'minecraft:directory','source':directory,'prefix':directory+'/'}
    if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(atlas_path,atlas)
    from pocket_content import PETS
    for layer,kind in [(1,'humanoid'),(2,'humanoid_leggings')]:copy(PETS/f'textures/models/armor/solstice_layer_{layer}.png',ASSET/f'textures/entity/equipment/{kind}/menagerie/solstice.png')
    write(ASSET/'equipment/menagerie/solstice.json',{'layers':{kind:[{'texture':'jugcraft:menagerie/solstice'}] for kind in ['humanoid','humanoid_leggings']}})
    for name,reagent in [('antivenom','scorpion_poison_gland'),('forest_dasher','antler'),('glowing','glow_goop')]:
        write(DATA/f'recipe/brewing/wildlife_{name}.json',{'type':'minecraft:brewing','input':{'item':'minecraft:potion','potion_contents':{'potions':'minecraft:awkward'}},'reagent':{'item':'jugcraft:wildlife_'+reagent},'output':{'id':'minecraft:potion','components':{'minecraft:potion_contents':{'potion':'jugcraft:wildlife_'+name}}}})
        for bottle,reagent_item,output in [('potion','gunpowder','splash_potion'),('splash_potion','dragon_breath','lingering_potion')]:
            write(DATA/f'recipe/brewing/wildlife_{name}_{output}.json',{'type':'minecraft:brewing','input':{'item':'minecraft:'+bottle,'potion_contents':{'potions':'jugcraft:wildlife_'+name}},'reagent':{'item':'minecraft:'+reagent_item},'output':{'id':'minecraft:'+output,'components':{'minecraft:potion_contents':{'potion':'jugcraft:wildlife_'+name}}}})
        for bottle in ['potion','splash_potion','lingering_potion']:lang[f'item.minecraft.{bottle}.effect.wildlife_{name}']=name.replace('_',' ').title()+(' Potion' if bottle=='potion' else ' '+bottle.replace('_',' ').title())
    lang['effect.jugcraft.wildlife_antivenom']='Antivenom'
    copy(NATURE/'textures/item/scorpion_poison_gland.png',ASSET/'textures/mob_effect/wildlife_antivenom.png')
    copy(NATURE/'textures/item/scorpion_poison_gland.png',ASSET/'textures/gui/sprites/mob_effect/wildlife_antivenom.png')
    painting=read(NATURE/'naturalist/painting_variant/bear_necessities.json');painting['asset_id']='jugcraft:menagerie/wildlife/painting/bear_necessities'
    # Painting assets resolve beneath textures/painting, unlike ordinary model textures.
    painting['asset_id']='jugcraft:wildlife_bear_necessities'
    copy(NATURE/'textures/painting/bear_necessities.png',ASSET/'textures/painting/wildlife_bear_necessities.png')
    write(DATA/'painting_variant/wildlife_bear_necessities.json',painting)
    tag_path=RES/'data/minecraft/tags/painting_variant/placeable.json';tag=read(tag_path) if tag_path.exists() else {'values':[]}
    if 'jugcraft:wildlife_bear_necessities' not in tag['values']:tag['values'].append('jugcraft:wildlife_bear_necessities')
    write(tag_path,tag)
    import diagonal_connections as diagonal
    diagonal.write_all(write,ASSET,RES/'assets/minecraft')
    for path,ids in [(DATA/'tags/block/connects_diagonally.json',['jugcraft:'+n for n in diagonal.JUGCRAFT_WALLS+diagonal.JUGCRAFT_PANES]),(RES/'data/minecraft/tags/block/walls.json',['jugcraft:'+prefix+n for n in diagonal.JUGCRAFT_WALLS for prefix in ['', 'diagonal_']])]:
        tag=read(path) if path.exists() else {'values':[]}
        for id in ids:
            if id not in tag['values']:tag['values'].append(id)
        write(path,tag)
    original_lang=read(NATURE/'lang/en_us.json')
    for key,value in original_lang.items():
        if key.startswith('painting.naturalist.'):lang[key]=value
    for s in c['species']:
        s['armorTextures']={}
        if s['mount']:
            prefix={'netherbat':'bat','geckotoalizard':'groundlizard'}.get(s['source'],s['source'])
            for p in (MOUNTS/'textures/model/entity/armor').glob(prefix+'_armor_*.png'):
                key=p.stem.split('_armor_')[1];key='golden' if key=='gold' else key
                s['armorTextures'][key]='menagerie/mount/model/entity/armor/'+p.stem
        else:
            for p in sorted((NATURE/'textures/entity'/s['source']/'dye').glob('*.png')):
                name='dye_'+p.stem;texture='menagerie/wildlife/entity/'+s['source']+'/dye/'+p.stem
                if not any(v['name']==name for v in s['variants']):s['variants'].append({'name':name,'texture':texture,'babyTexture':texture,'weight':0})
        source=NATURE/'naturalist/naturalist'/f"{s['source']}_variant"
        for key in [s['source'],s['source']+'_bucket']:
            if not (NATURE/f'models/item/{key}.json').exists() or key not in {v.get('source') for v in c['items'].values()}:continue
            model={'type':'minecraft:model','model':'jugcraft:menagerie/wildlife/item/'+key}
            for index,variant in reversed(list(enumerate(s['variants']))):
                record=source/(variant['name']+'.json')
                if not record.exists():continue
                chosen=read(record).get('item_model','')
                if not chosen or not chosen.endswith('/'+variant['name']) or key not in chosen:continue
                model={'type':'minecraft:condition','property':'minecraft:component','predicate':'minecraft:custom_data','value':'{Variant:'+str(index)+'}',
                       'on_true':{'type':'minecraft:model','model':remap(chosen,resource=True)},'on_false':model}
            write(ASSET/f'items/wildlife_{key}.json',{'model':model})
    from wildlife_geometry import adapt
    adapt(c)
    write(RES/'jugcraft/menagerie/catalog.json',c)
    for key in c['blocks']:
        path=DATA/f'loot_table/blocks/{key}.json'
        if not path.exists():
            pool={'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+key}]}
            if key.endswith('froglass_pane'):pool['condition']='minecraft:tool/can_silk_touch'
            write(path,{'type':'minecraft:block','pools':[pool]})
    egg_tag=RES/'data/c/tags/item/eggs.json';eggs=read(egg_tag) if egg_tag.exists() else {'values':[]}
    for item in ['#minecraft:eggs','jugcraft:wildlife_duck_egg']:
        if item not in eggs['values']:eggs['values'].append(item)
    write(egg_tag,eggs)
    # Resolve the legacy in-model texture alias in the runtime copy; originals retain it.
    egg_model=ASSET/'models/menagerie/wildlife/block/ostrich_egg.json';egg=read(egg_model);egg['textures']['particle']=egg['textures']['all'];write(egg_model,egg)
    item_model('mount_summoning_staff','jugcraft:menagerie/mount/item/summoning_staff')
    item_model('menagerie_field_guide','jugcraft:menagerie/mount/item/mythical_mounts')
    item_model('wildlife_cattail_fluff','jugcraft:menagerie/wildlife/item/fur')
    item_model('wildlife_cooked_fish_fillet','minecraft:item/cooked_cod')
    for id,texture in [('cattail','minecraft:block/sugar_cane'),('duckweed','minecraft:block/lily_pad')]:
        write(ASSET/f'models/menagerie/wildlife/block/{id}.json',{'parent':'minecraft:block/cross','textures':{'cross':texture}})
        write(ASSET/f'blockstates/wildlife_{id}.json',{'variants':{'':{'model':'jugcraft:menagerie/wildlife/block/'+id}}})
        item_model('wildlife_'+id,texture)
    write(ASSET/'models/menagerie/wildlife/block/duckweed.json',{'parent':'minecraft:block/lily_pad','textures':{'texture':'minecraft:block/lily_pad'}})
    for id in ['cloud_block','sand_block','stone_block']:lang['block.jugcraft.pocket_'+id]=id.replace('_',' ').title()
    for i in range(1,5):lang['block.jugcraft.mount_firebird_feather'+('' if i==1 else str(i))]='Firebird Feather Block'+('' if i==1 else ' '+str(i))
    recipe('menagerie_field_guide',['minecraft:book','minecraft:feather'],'jugcraft:menagerie_field_guide')
    recipe('mount_summoning_staff',['minecraft:stick','minecraft:amethyst_shard','minecraft:gold_ingot'],'jugcraft:mount_summoning_staff')
    recipe('wildlife_hide_leather',['jugcraft:wildlife_hide'],'minecraft:leather',2)
    recipe('wildlife_fur_string',['jugcraft:wildlife_fur'],'minecraft:string',3)
    recipe('wildlife_fat_candle',['jugcraft:wildlife_fat','minecraft:string'],'minecraft:candle',2)
    recipe('wildlife_shellstone',['jugcraft:wildlife_snail_shell','minecraft:calcite'],'jugcraft:wildlife_shellstone',4)
    recipe('wildlife_cattail',['minecraft:sugar_cane','minecraft:wheat'],'jugcraft:wildlife_cattail',2)
    recipe('wildlife_duckweed',['minecraft:lily_pad'],'jugcraft:wildlife_duckweed',2)
    recipe('wildlife_cattail_fluff',['jugcraft:wildlife_cattail'],'jugcraft:wildlife_cattail_fluff',2)
    recipe('wildlife_fluff_string',['jugcraft:wildlife_cattail_fluff','jugcraft:wildlife_cattail_fluff'],'minecraft:string')
    recipe('wildlife_tooth_arrows',['jugcraft:wildlife_tooth','minecraft:stick','minecraft:feather'],'minecraft:arrow',4)
    recipe('wildlife_antler_bone_meal',['jugcraft:wildlife_antler'],'minecraft:bone_meal',4)
    recipe('wildlife_morsel',['minecraft:wheat_seeds','minecraft:sweet_berries'],'jugcraft:wildlife_morsel',2)
    recipe('wildlife_ant_hill',['minecraft:dirt','jugcraft:wildlife_queen_ant'],'jugcraft:wildlife_ant_hill')
    recipe('wildlife_gland_fermented_eye',['jugcraft:wildlife_scorpion_poison_gland','minecraft:sugar','minecraft:brown_mushroom'],'minecraft:fermented_spider_eye')
    recipe('wildlife_chrysalis',['jugcraft:wildlife_caterpillar','minecraft:oak_leaves'],'jugcraft:wildlife_chrysalis')
    for id in ['alligator_egg','ostrich_egg','tortoise_egg','snail_eggs','plush_bear','snail_shell','glow_goop']:
        path=DATA/f'loot_table/blocks/wildlife_{id}.json'
        if not path.exists():write(path,{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:wildlife_'+id}]}]})
    write(DATA/'worldgen/feature/menagerie_habitat.json',{'type':'jugcraft:menagerie_habitat'})
    write(DATA/'worldgen/placed_feature/menagerie_habitat.json',{'feature':'jugcraft:menagerie_habitat','placement':[{'type':'minecraft:rarity_filter','chance':32},{'type':'minecraft:in_square'},{'type':'minecraft:biome'}]})
    lang.update({'item.jugcraft.mount_summoning_staff':'Mount Summoning Staff','item.jugcraft.menagerie_field_guide':'Menagerie Field Guide','encyclopedia.jugcraft.page.menagerie':'Wildlife & Companions'})
    for name in ['death_by_hogs','wild_ones']:lang['jukebox_song.naturalist.'+name]=name.replace('_',' ').title()
    for key,name in {'pocket_banana':'Pocket Banana','pocket_rock_candy':'Crystalline Rock Candy','herbal_witch_hat':'Herbalist Witch Hat','pocket_holiday_gift':'Winter Gift','pocket_siamese_gift':'Siamese Gift','wildlife_music_disc_death_by_hogs':'Music Disc — Death by Hogs','wildlife_music_disc_wild_ones':'Music Disc — Wild Ones'}.items():lang['item.jugcraft.'+key]=name
    write(ASSET/'lang/en_us.json',lang)
    from materials import feature_of
    for path in (DATA/'recipe').glob('*.json'):
        if path.stem.startswith(('wildlife_','pocket_','herbal_','mount_','menagerie_')):
            value=read(path);result=value.get('result',{}).get('id','')
            feature=feature_of(result.split(':',1)[1]) if result.startswith('jugcraft:') else 'agriculture'
            value['fabric:load_conditions']=[{'condition':'jugcraft:feature_enabled','feature':feature}]
            write(path,value)
    print('Wired variant dyes, mount armor, guide, habitat generation and shared resource recipes.')

if __name__=='__main__':main()
