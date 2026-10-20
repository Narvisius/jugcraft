"""Final native recipe/registry adapters for the October 18 owner collections.

Run after collection generators. Originals are never changed. Optional outside
recipe serializers are replaced with Jugcraft's existing cooking/cutting rules.
"""
import shutil
from menagerie_content import ROOT, RES, ASSET, DATA, read, write

GROUPS=('household','ecology','fauna','mutant','beast','landscape','cavern')
SWITCH=[{'condition':'jugcraft:feature_enabled','feature':'agriculture'}]
ALIASES={
    'minecraft:chain':'minecraft:iron_chain',
    'minecraft:grass':'minecraft:short_grass',
    'minecraft:scute':'minecraft:turtle_scute',
    'farmersdelight:pie_crust':'jugcraft:pastry_dough',
    '#c:shears':'minecraft:shears',
    'forge:rods/wooden':'minecraft:stick','forge:glass/colorless':'minecraft:glass',
    'forge:glass':'minecraft:glass','forge:barrels/wooden':'minecraft:barrel',
    'forge:gravel':'minecraft:gravel','forge:string':'minecraft:string',
    'forge:raw_materials/uranium':'jugcraft:cavern_uranium',
    'jugcraft:cavern_raw_uranium':'jugcraft:cavern_uranium',
    'forge:storage_blocks/uranium':'jugcraft:cavern_block_of_uranium',
    'forge:dusts/sulfur':'jugcraft:cavern_sulfur_dust',
    'farmersdelight:tree_bark':'jugcraft:sawdust','rusticdelight:batter':'jugcraft:pastry_dough',
    'jugcraft:dough':'jugcraft:pastry_dough',
}
for metal in ('iron','gold','netherite'):
    ALIASES['forge:ingots/'+metal]='minecraft:'+metal+'_ingot'
    if metal!='netherite':
        ALIASES['forge:nuggets/'+metal]='minecraft:'+metal+'_nugget'
        ALIASES['forge:storage_blocks/'+metal]='minecraft:'+metal+'_block'
        ALIASES['forge:raw_materials/'+metal]='minecraft:raw_'+metal
ALIASES.update({'forge:gems/diamond':'minecraft:diamond','forge:storage_blocks/redstone':'minecraft:redstone_block'})
for color in 'white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split():
    ALIASES['forge:dyes/'+color]='minecraft:'+color+'_dye'


def refs(v):
    if isinstance(v,str):
        v=v.replace('goblinsanddungeons:','jugcraft:landscape_')
        if v=='#c:shears':return 'minecraft:shears'
        if v=='#farmersdelight:tools/knives':return '#jugcraft:knives'
        return ALIASES.get(v.lstrip('#'),v)
    if isinstance(v,list):return [refs(x) for x in v]
    if isinstance(v,dict):return {k:refs(x) for k,x in v.items()}
    return v


def owned(path):
    return any(part==g or part.startswith(g+'_') for part in path.relative_to(DATA/'recipe').parts for g in GROUPS)


def recipes():
    changed=[]
    for path in (DATA/'recipe').rglob('*.json'):
        if not owned(path):continue
        old=read(path);d=refs(old);kind=d['type']
        for key in ('conditions','neoforge:conditions'):d.pop(key,None)
        d['fabric:load_conditions']=SWITCH
        if kind=='farmersdelight:cutting':
            ingredients=d['ingredients'];tool=d['tool'];results=d['result']
            if 'salvaging' in path.parts and path.stem.endswith('_furniture'):
                # Native deterministic reclaim: four pieces return one plank.
                # Never return a full plank for a cheap button/pressure plate.
                result=results[0].get('item',results[0]);result.pop('chance',None)
                d={'type':'minecraft:crafting_shapeless','ingredients':ingredients*4,'result':result,'fabric:load_conditions':SWITCH}
            else:
                native_tool='#jugcraft:knives' if isinstance(tool,str) else '#minecraft:hoes' if 'chest_boat' in path.stem else '#minecraft:axes'
                d={'type':'jugcraft:cutting','ingredient':ingredients[0],'tool':native_tool,'results':results,'fabric:load_conditions':SWITCH}
        elif kind=='farmersdelight:cooking':
            d={'type':'minecraft:crafting_shapeless','ingredients':d['ingredients'],'result':d['result'],'fabric:load_conditions':SWITCH}
        elif kind=='minecraft:brewing' and 'mutant' in path.parts:
            # Chemical X is a usable Jugcraft item, not an unregistered effect.
            d['output']={'id':'jugcraft:mutant_chemical_x'}
        elif kind=='minecraft:smithing_trim':d['pattern']='jugcraft:cavern_polarity'
        if isinstance(d.get('result'),dict) and d['result'].get('id') in ('jugcraft:cavern_limestone_spear','jugcraft:cavern_frostmint_spear'):
            d['result']['count']=1  # Native durable weapons cannot form four-item stacks.
        if d!=old:write(path,d);changed.append(path.relative_to(DATA).as_posix())
    return changed


def trim():
    catalog=read(RES/'jugcraft/menagerie/cavern.json')
    catalog['items']['cavern_polarity_armor_trim_smithing_template']={'source':'polarity_armor_trim_smithing_template','food':False,'armor':''}
    write(RES/'jugcraft/menagerie/cavern.json',catalog)
    write(ASSET/'items/cavern_polarity_armor_trim_smithing_template.json',{'model':{'type':'minecraft:model','model':'jugcraft:menagerie/cavern/item/polarity_armor_trim_smithing_template'}})
    lang=read(ASSET/'lang/en_us.json');lang['item.jugcraft.cavern_polarity_armor_trim_smithing_template']='Polarity Armor Trim';write(ASSET/'lang/en_us.json',lang)
    write(DATA/'trim_pattern/cavern_polarity.json',{'asset_id':'jugcraft:cavern_polarity','decal':False,'description':{'text':'Polarity'}})
    # Native 26.3 armor trim atlas paths; reuse the supplied monochrome patterns.
    for source,target in [('polarity','humanoid'),('polarity_leggings','humanoid_leggings')]:
        dest=ASSET/f'textures/trims/entity/{target}/cavern_polarity.png';dest.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(ASSET/f'textures/menagerie/cavern/trims/models/armor/{source}.png',dest)
        # 26.3 resolves trim palettes from per-texture metadata, not an atlas.
        write(dest.with_suffix('.png.mcmeta'),{'palette':{'base_palette':'minecraft:trim_base'}})


def tags():
    aliases={
        '#forge:sand':['#minecraft:sand'],'#forge:ores/iron':['minecraft:iron_ore','minecraft:deepslate_iron_ore'],
        '#forge:ores/netherite_scrap':['minecraft:ancient_debris'],'#forge:storage_blocks/raw_iron':['minecraft:raw_iron_block'],
        '#forge:storage_blocks/netherite':['minecraft:netherite_block'],'#forge:eggs':['#minecraft:eggs'],
        '#forge:crops/banana':['jugcraft:beast_banana'],'#forge:fruits/banana':['jugcraft:beast_banana'],
        '#forge:crops/carrot':['minecraft:carrot'],'#forge:crops/rice':['jugcraft:rice'],
        '#forge:seeds':['minecraft:wheat_seeds','minecraft:beetroot_seeds','minecraft:pumpkin_seeds','minecraft:melon_seeds'],
        '#c:foods/nut':['jugcraft:ecology_walnut','jugcraft:ecology_coconut_slice'],
        '#minecraft:overworld_carver_replaceables':['#minecraft:base_stone_overworld','minecraft:dirt','minecraft:grass_block'],
        '#base_stone_overworld':['#minecraft:base_stone_overworld'],
        '#forge:bosses':['minecraft:ender_dragon','minecraft:wither'],
        'jugcraft:beast_murmur_head':['jugcraft:beast_murmur'],
        # Native portals are blocks, while worms have one authoritative entity.
        'jugcraft:beast_void_portal':[], 'jugcraft:cavern_gum_worm_segment':['jugcraft:cavern_gum_worm'],
        'jugcraft:cavern_melted_caramel':['jugcraft:cavern_projectile'],
        'jugcraft:cavern_spinning_peppermint':['jugcraft:cavern_projectile'],
        'jugcraft:cavern_sugar_staff_hex':['jugcraft:cavern_projectile'],
        'jugcraft:landscape_flowing_blood':['jugcraft:flowing_landscape_blood'],
        'jugcraft:landscape_flowing_liquid_null':['jugcraft:flowing_landscape_liquid_null'],
    }
    aliases['#forge:ores']=['minecraft:'+p+s+'_ore' for p in ('','deepslate_') for s in ('coal','iron','copper','gold','redstone','lapis','diamond','emerald')]
    for path in (DATA/'tags').rglob('*.json'):
        if not any(path.stem.startswith(g+'_') for g in GROUPS):continue
        d=read(path);values=[]
        for entry in d['values']:
            optional=isinstance(entry,dict);value=entry['id'] if optional else entry
            values.extend(aliases.get(value,[refs(entry)]))
        d['values']=list({str(v):v for v in values}.values());write(path,d)


def loot_tables():
    from menagerie_data import loot
    def upgrade(v,field=''):
        if isinstance(v,list):return [upgrade(x,field) for x in v if not isinstance(x,dict) or x.get('type')!='minecraft:copy_nbt']
        if not isinstance(v,dict):return v
        out={k:upgrade(x,k) for k,x in v.items()}
        if out.get('type') in ('minecraft:tool/can_silk_touch','minecraft:tool/can_shear') and 'name' in out:
            out['condition']=out['type'];out['type']='minecraft:item'
        if field in ('count','rolls','bonus_rolls') and set(out)=={'min','max'}:out['type']='minecraft:uniform'
        if out.get('type')=='minecraft:loot_table' and 'name' in out:out['value']=out.pop('name')
        if out.get('type')=='minecraft:set_nbt':
            import re
            match=re.fullmatch(r'\{CaveBiome:"alexscaves:([a-z_]+)"\}',out.pop('tag'))
            if not match:raise ValueError('Unrecognized supplied item data')
            out['type']='minecraft:set_custom_data';out['tag']='{BoundBiome:"jugcraft:cavern_'+match[1]+'"}'
        if out.get('type')=='minecraft:match_tool':
            predicate=out.get('predicate',{})
            if 'enchantments' in predicate:
                enchantments=predicate.pop('enchantments')
                for e in enchantments:
                    if 'enchantment' in e:e['enchantments']=e.pop('enchantment')
                predicate.setdefault('predicates',{})['minecraft:enchantments']=enchantments
        return out
    for p in (DATA/'loot_table').rglob('*.json'):
        if not p.stem.startswith(tuple(g+'_' for g in GROUPS)):continue
        old=read(p);new=upgrade(refs(loot(old)))
        # Owner collection loot is native; old optional-addon load conditions no longer apply.
        new.pop('fabric:load_conditions',None);new.pop('neoforge:conditions',None)
        if p.stem in ('landscape_blood','landscape_liquid_null'):new={'type':'minecraft:block','pools':[]}
        if new!=old:write(p,new)


def main():
    import owner_goblin_completion, owner_worldgen_completion
    owner_goblin_completion.main();owner_worldgen_completion.main()
    changed=recipes();trim();tags();loot_tables()
    import owner_template_data
    for p in (DATA/'structure').rglob('*.nbt'):
        if any('cavern' in part or 'ecology' in part for part in p.relative_to(DATA/'structure').parts):
            before=p.read_bytes();after=owner_template_data.adapt(before,lambda v:v)
            if before!=after:p.write_bytes(after)
    for p in (ASSET/'models/menagerie').rglob('*.json'):
        d=read(p)
        if d.get('parent')=='minecraft:item/template_spawn_egg':
            d['parent']='minecraft:item/zombie_spawn_egg';write(p,d)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/native-data-adapters.json',{'recipes':changed,'aliases':ALIASES,'note':'Native cutting, reclaim, Chemical X brewing and Polarity trim. Source originals retained.'})
    import owner_resource_completion
    owner_resource_completion.main()
    import owner_mesh_compatibility
    owner_mesh_compatibility.main()
    import owner_visual_polish
    owner_visual_polish.main()
    import survival_recipe_completion
    survival_recipe_completion.main()
    print('Adapted',len(changed),'owner recipes to native dependencies.')


if __name__=='__main__':main()
