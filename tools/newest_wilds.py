"""Native owner creature assets; source bytecode is static reference only."""
from pathlib import Path
import re,hashlib
import nouveau_nbt
from newest_building import SOURCE,RES,CAT,read,write,copy,map_value,merge_tag
from newest_twigs import recipe
from menagerie_data import loot
from recover_creature_models import constructor,normalized,body,layer
from recover_creature_animation import extract
S=SOURCE/'jugziesmobs';A=S/'assets/mowziesmobs';D=S/'data/mowziesmobs';O=RES/'assets/jugcraft'
ROOT=Path(__file__).resolve().parents[1]
REF=ROOT/'build/owner-oct29-reference/com/bobmowzie/mowziesmobs'
BLOCKS=['painted_acacia','painted_acacia_slab','thatch_block','clawed_log','raked_sand','red_raked_sand','gong','gong_part']
ITEMS=['earthrend_gauntlet','sculptor_staff','sculptor_spawn_egg','geomancer_beads','geomancer_robe','geomancer_belt','geomancer_sandals','grant_suns_blessing','foliaath_seed','sand_rake','foliaath_spawn_egg','captured_grottol','glowing_jelly','grottol_spawn_egg','lantern_spawn_egg','dart','blowgun','spear','naga_fang','naga_fang_dagger','naga_spawn_egg','wrought_helmet','wrought_axe','wroughtnaut_spawn_egg','ice_crystal','frostmaw_spawn_egg','music_disc_petiole','bluff_rod','bluff_spawn_egg','elokosa_howler_spawn_egg','elokosa_spawn_egg','umvuthi_spawn_egg','umvuthana_spawn_egg','umvuthana_raptor_spawn_egg','umvuthana_crane_spawn_egg','sol_visage','sunblock_staff']+['umvuthana_mask_'+m for m in ['fury','fear','rage','bliss','misery','faith']]+['elokosa_paw_'+p for p in ['full','gibbous','half','crescent','new']]
def mapped(v,resource=False):return map_value(v,'mowziesmobs','wilds',resource)
def main():
 import newest_wilds_monastery
 newest_wilds_monastery.main()
 for folder in ['models','textures','geo','animations','sounds']:
  for p in (A/folder).rglob('*'):
   if not p.is_file():continue
   dest=O/folder/'newest/wilds'/p.relative_to(A/folder)
   if p.suffix=='.json':write(dest,mapped(read(p),True))
   else:copy(p,dest)
 sounds=read(A/'sounds.json');combined=read(O/'sounds.json')
 for name,value in sounds.items():combined['wilds_'+name]=mapped(value,True)
 write(O/'sounds.json',combined)
 # GeckoLib 5 resource roots; retain source joints/clips, render spike blocks at authored sockets.
 for creature in ['bluff','earth_spike','elokosa','umvuthi','umvuthana','sculptor']:
  geo=read(A/'geo'/(creature+'.geo.json'))
  if creature=='earth_spike':
   for geometry in geo['minecraft:geometry']:
    for bone in geometry['bones']:bone.pop('cubes',None)
  write(O/'geckolib/models/entity/wilds'/(creature+'.geo.json'),geo)
  write(O/'geckolib/animations/entity/wilds'/(creature+'.animation.json'),read(A/'animations'/(creature+'.animation.json')))
 for name in ['moon_full','moon_gibbous','moon_half','moon_crescent','moon_new','pixel','snowflake','glow']:
  definition=read(A/'particles'/(name+'.json'));textures=[]
  for texture in definition['textures']:
   relative=texture.split(':',1)[-1];copy(A/'textures/particle'/(relative+'.png'),O/'textures/particle/wilds'/(relative+'.png'));textures.append('jugcraft:wilds/'+relative)
  write(O/'particles'/('wilds_'+name+'.json'),{'textures':textures})
 known=set(BLOCKS+ITEMS)
 for name in BLOCKS:
  write(O/'blockstates'/('wilds_'+name+'.json'),mapped(read(A/'blockstates'/(name+'.json')),True))
  source=D/'loot_tables/blocks'/(name+'.json')
  if source.exists():write(RES/'data/jugcraft/loot_table/blocks'/('wilds_'+name+'.json'),loot(mapped(read(source))))
 for name in known:
  if name=='gong_part':continue
  source=A/'models/item'/(name+'.json')
  if source.exists():write(O/'items'/('wilds_'+name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:newest/wilds/item/'+name}})
 write(O/'items/wilds_foliaath_spawn_egg.json',{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg','tints':[{'type':'minecraft:constant','value':4705339},{'type':'minecraft:constant','value':12598220}]}})
 write(O/'items/wilds_blowgun.json',{'model':{'type':'minecraft:condition','property':'minecraft:using_item','on_true':{'type':'minecraft:model','model':'jugcraft:newest/wilds/item/blowgun_using'},'on_false':{'type':'minecraft:model','model':'jugcraft:newest/wilds/item/blowgun'}}})
 write(RES/'data/jugcraft/loot_table/blocks/wilds_gong_part.json',{'type':'minecraft:block','pools':[]})
 # Source includes no survival recipe for these two functional tools.
 for name,pattern,keys in [('sand_rake',['PPP',' S ',' S '],{'P':'#minecraft:planks','S':'minecraft:stick'}),('gong',['PPP','IGI','IGI'],{'P':'jugcraft:wilds_painted_acacia','I':'minecraft:iron_ingot','G':'minecraft:gold_block'})]:
  write(RES/'data/jugcraft/recipe/wilds'/(name+'.json'),{'type':'minecraft:crafting_shaped','pattern':pattern,'key':keys,'result':{'id':'jugcraft:wilds_'+name,'count':1}})
 for name in ['foliaath','baby_foliaath','lantern','naga','ferrous_wroughtnaut','bluff','umvuthi']:
  p=D/'loot_tables/entities'/(name+'.json')
  if p.exists():write(RES/'data/jugcraft/loot_table/entities'/('wilds_'+name+'.json'),loot(mapped(read(p))))
 for tag in ['can_grottol_dig','geomancy_useable']:
  source=D/'tags/blocks'/(tag+'.json')
  if source.exists():write(RES/'data/jugcraft/tags/block'/('wilds_'+tag+'.json'),mapped(read(source)))
 write(RES/'data/jugcraft/loot_table/entities/wilds_grottol.json',{'type':'minecraft:entity','pools':[]})
 for name,colors in [('grottol',[0x777777,12378367]),('lantern',[7203328,2316816]),('naga',[1394768,9295705]),('wroughtnaut',[0x8C8C8C,0xFFFFFF]),('frostmaw',[0xF7FAFF,11521535]),('bluff',[6570550,7123502])]:
  write(O/'items'/('wilds_'+name+'_spawn_egg.json'),{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg','tints':[{'type':'minecraft:constant','value':c} for c in colors]}})
 for name in ['elokosa_howler','elokosa_follower_howler']:
  write(RES/'data/jugcraft/loot_table/entities'/('wilds_'+name+'.json'),{'type':'minecraft:entity','pools':[]})
 for name in ['umvuthana','umvuthana_raptor','umvuthana_crane','umvuthana_follower_player']:
  write(RES/'data/jugcraft/loot_table/entities'/('wilds_'+name+'.json'),{'type':'minecraft:entity','pools':[]})
 for name in ['umvuthi','umvuthana','umvuthana_raptor','umvuthana_crane']:
  write(O/'items'/('wilds_'+name+'_spawn_egg.json'),{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg','tints':[{'type':'minecraft:constant','value':0x80572C},{'type':'minecraft:constant','value':0xE9C543}]}})
 for phase in ['fury','fear','rage','bliss','misery','faith','sol']:
  model='sol_visage' if phase=='sol' else 'mask_'+phase
  item='sol_visage' if phase=='sol' else 'umvuthana_mask_'+phase
  write(O/'geckolib/models/armor/wilds'/(model+'.geo.json'),read(A/'geo'/(model+'.geo.json')))
  write(O/'geckolib/animations/armor/wilds'/(model+'.animation.json'),read(A/'animations'/('sol_visage.animation.json' if phase=='sol' else 'umvuthana_mask.animation.json')))
  write(O/'items'/('wilds_'+item+'.json'),{'model':{'type':'minecraft:special','base':'jugcraft:newest/wilds/item/'+item,'model':{'type':'geckolib:geckolib'}}})
 # Original equipment and stone constructs. Static items get an empty animation document, not invented clips.
 for name in ['earthrend_gauntlet','sculptor_staff','geomancer_armor']:
  write(O/'geckolib/models/equipment/wilds'/(name+'.geo.json'),read(A/'geo'/(name+'.geo.json')))
  animation=A/'animations'/(name+'.animation.json')
  write(O/'geckolib/animations/equipment/wilds'/(name+'.animation.json'),read(animation) if animation.exists() else {'format_version':'1.8.0','animations':{}})
 for name in ['earthrend_gauntlet','sculptor_staff','geomancer_beads','geomancer_robe','geomancer_belt','geomancer_sandals']:
  base={'type':'minecraft:model','model':'jugcraft:newest/wilds/item/'+name}
  write(O/'items'/('wilds_'+name+'.json'),{'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':['gui','ground','fixed'],'model':base}],'fallback':{'type':'minecraft:special','base':base['model'],'model':{'type':'geckolib:geckolib'}}}})
 for name in ['boulder_1','boulder_2','boulder_3','rock_sling']:
  write(O/'geckolib/models/entity/wilds'/(name+'.geo.json'),read(A/'geo'/(name+'.geo.json')))
 write(O/'geckolib/animations/entity/wilds/rock_sling.animation.json',read(A/'animations/rock_sling.animation.json'))
 # Repeat the original two-block pillar cross-section; don't stretch one texture over sixty blocks.
 import copy as deepcopy_module
 pillar=read(A/'geo/geomancy_pillar.geo.json');geometry=pillar['minecraft:geometry'][0];original=geometry['bones'];segments=[]
 selected=[b for b in original if b['name']=='tier2' or b.get('parent')=='tier2']
 for segment in range(64):
  for bone in selected:
   b=deepcopy_module.deepcopy(bone);b['name']+='_'+str(segment)
   if 'parent' in b:b['parent']+='_'+str(segment)
   if 'pivot' in b:b['pivot'][1]+=segment*16
   for cube in b.get('cubes',[]):
    cube['origin'][1]+=segment*16
    if 'pivot' in cube:cube['pivot'][1]+=segment*16
   segments.append(b)
 geometry['bones']=segments;geometry['description'].update(visible_bounds_width=4,visible_bounds_height=68,visible_bounds_offset=[0,32,0])
 write(O/'geckolib/models/entity/wilds/geomancy_pillar.geo.json',pillar)
 for name in ['sculptor','sculptor_test']:
  value=loot(mapped(read(D/'loot_tables/entities'/(name+'.json'))));value['type']='minecraft:entity'
  for pool in value['pools']:
   pool.pop('name',None)
   if pool.get('condition',{}).get('type')=='killed_by_player':pool['condition']['type']='minecraft:killed_by_player'
   for entry in pool['entries']:
    if entry.get('type')=='item':entry['type']='minecraft:item'
  write(RES/'data/jugcraft/loot_table/entities'/('wilds_'+name+'.json'),value)
 write(RES/'data/jugcraft/tags/item/repairs_geomancer_armor.json',{'replace':False,'values':['jugcraft:wilds_bluff_rod']})
 write(O/'items/wilds_sculptor_spawn_egg.json',{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg','tints':[{'type':'minecraft:constant','value':0x95775B},{'type':'minecraft:constant','value':0xDBCFAD}]}})
 # Locateable authored sun-tribe grove, original layouts and marker payloads.
 for path in (D/'structures/umvuthana').glob('*.nbt'):
  target=RES/'data/jugcraft/structure/wilds/umvuthana'/path.name;target.parent.mkdir(parents=True,exist_ok=True)
  target.write_bytes(nouveau_nbt.adapt(path.read_bytes(),lambda v:mapped(v).replace('minecraft:grass','minecraft:short_grass') if v=='minecraft:grass' else mapped(v)))
 for phase in ['fury','fear','rage','bliss','misery','faith']:
  value=loot(mapped(read(D/'loot_tables/entities'/('umvuthana_'+phase+'.json'))));value['type']='minecraft:entity'
  for pool in value['pools']:
   pool.pop('name',None)
   for entry in pool.get('entries',[]):
    if entry.get('type')=='item':entry['type']='minecraft:item'
    for modifier in entry.get('modifier',[]):
     if modifier.get('type')=='set_count':modifier['type']='minecraft:set_count'
     if isinstance(modifier.get('count'),dict):modifier['count'].setdefault('type','minecraft:uniform')
  write(RES/'data/jugcraft/loot_table/entities'/('wilds_umvuthana_'+phase+'.json'),value)
 chest=loot(mapped(read(D/'loot_tables/chests/umvuthana_grove_chest.json')))
 for pool in chest['pools']:
  for entry in pool['entries']:
   if entry.get('name')=='minecraft:grass':entry['name']='minecraft:short_grass'
 write(RES/'data/jugcraft/loot_table/chests/wilds_umvuthana_grove.json',chest)
 write(RES/'data/jugcraft/worldgen/structure/wilds_umvuthana_grove.json',{'type':'jugcraft:wilds_grove','biomes':'#minecraft:is_savanna','step':'surface_structures','terrain_adaptation':'none','spawn_overrides':{}})
 write(RES/'data/jugcraft/worldgen/structure_set/wilds_umvuthana_grove.json',{'structures':[{'structure':'jugcraft:wilds_umvuthana_grove','weight':1}],'placement':{'type':'minecraft:random_spread','spacing':25,'separation':8,'salt':23311138,'spread_type':'triangular','exclusion_zone':{'other_set':'minecraft:villages','chunk_count':10}}})
 for effect in ['fragility','eclipsed','moons_curse','suns_blessing','sunblock']:
  original=A/'textures/mob_effect'/(effect+'.png')
  if original.exists():copy(original,O/'textures/mob_effect'/('wilds_'+effect+'.png'))
 for name in ['elokosa','elokosa_howler']:
  write(O/'items'/('wilds_'+name+'_spawn_egg.json'),{'model':{'type':'minecraft:model','model':'minecraft:item/cow_spawn_egg','tints':[{'type':'minecraft:constant','value':1315350},{'type':'minecraft:constant','value':6051049 if name=='elokosa_howler' else 12666664}]}})
 for phase in ['full','gibbous','half','crescent','new']:
  # Original closed hand while the shared paw cooldown is active.
  base='jugcraft:newest/wilds/item/elokosa_paw_'+phase
  closed='jugcraft:newest/wilds/item/elokosa_paw_new_closed' if phase=='new' else 'jugcraft:newest/wilds/item/elokosa_paw_closed'
  write(O/'items'/('wilds_elokosa_paw_'+phase+'.json'),{'model':{'type':'minecraft:range_dispatch','property':'minecraft:cooldown','entries':[{'threshold':0.001,'model':{'type':'minecraft:model','model':closed}}],'fallback':{'type':'minecraft:model','model':base}}})
 def available(v):
  if isinstance(v,str):return not v.startswith('mowziesmobs:') or v.split(':',1)[1] in known
  if isinstance(v,dict):return all(available(x) for x in v.values())
  if isinstance(v,list):return all(available(x) for x in v)
  return True
 recipes=[]
 for p in (D/'recipes').rglob('*.json'):
  v=read(p)
  if available(v) and v.get('type','').startswith('minecraft:'):write(RES/'data/jugcraft/recipe/wilds'/p.relative_to(D/'recipes'),recipe(mapped(v)));recipes.append(p.stem)
 lang=read(O/'lang/en_us.json')
 for key,value in read(A/'lang/en_us.json').items():
  if '.mowziesmobs.' in key:lang[key.replace('.mowziesmobs.','.jugcraft.wilds_')]=value
 lang.update({'item.jugcraft.wilds_sunblock_staff': 'Sunblock Staff'})
 write(O/'lang/en_us.json',lang)
 for tag,names in {'mineable/axe':['painted_acacia','painted_acacia_slab','clawed_log'],'mineable/hoe':['thatch_block'],'mineable/shovel':['raked_sand','red_raked_sand'],'slabs':['painted_acacia_slab'],'sand':['raked_sand','red_raked_sand']}.items():merge_tag(RES/'data/minecraft/tags/block'/(tag+'.json'),['jugcraft:wilds_'+n for n in names],write)
 helm=REF/'client/model/armor/WroughtHelmModel.java'
 helm_src=normalized(helm.read_text()).replace('CubeDeformation.f_171458_','new CubeDeformation(0.0f)')
 helm_geo=layer(helm_src)
 # Owner armor hides inherited skin cubes; the authored shell/horns remain.
 for part in helm_geo['parts']:
  if part['name'] in ['head','hat','body','right_arm','left_arm','right_leg','left_leg']:part['cubes']=[]
 write(O/'recovered_creatures/wilds_wrought_helmet.json',helm_geo)
 for tag,names in {'arrows':['dart'],'enchantable/bow':['blowgun'],'axes':['wrought_axe'],'enchantable/weapon':['wrought_axe','spear','naga_fang_dagger'],'head_armor':['wrought_helmet']}.items():merge_tag(RES/'data/minecraft/tags/item'/(tag+'.json'),['jugcraft:wilds_'+n for n in names],write)
 gong=REF/'client/render/block/GongRenderer.java'
 geometry=layer(normalized(gong.read_text()));geometry.update(animationRoot='$mesh',headLook='',actions={},motions=[],expressions=[],poses=[],source=gong.relative_to(ROOT).as_posix(),sourceSha256=hashlib.sha256(gong.read_bytes()).hexdigest())
 write(O/'recovered_creatures/wilds_gong.json',geometry)
 recovered={'GongRenderer':{'parts':len(geometry['parts']),'actions':['native_damped_swing']}};limitations={}
 for name in ['ModelFoliaath','ModelFoliaathBaby','ModelWroughtnaut','ModelFrostmaw','ModelNaga','ModelGrottol','ModelLantern','ModelPoisonBall','ModelIceBall','ModelSuperNova']:
  path=REF/'client/model/entity'/(name+'.java')
  if not path.exists():limitations[name]='No static decompilation yet';continue
  src=normalized(path.read_text()).replace('AdvancedModelRenderer','AdvancedModelBox').replace('this.setRotationAngle','this.setRotateAngle').replace(name+'.setRotateAngle','this.setRotateAngle')
  try:
   geometry=re.sub(r'this\.\w+\.setOpacity\([^;]+;', '',src)
   geometry=re.sub(r'this\.\w+ = new AdvancedModelBox\[\]\{[^}]*\};','',geometry)
   geometry=re.sub(r'this\.\w+ = new AdvancedModelBox\[[^;]*;','',geometry)
   if name=='ModelWroughtnaut':
    eye=body(geometry,r'if \(eyesLayer\)');geometry=geometry.replace('if (eyesLayer) {'+eye+'}','')
   geometry=re.sub(r'public '+name+r'\(\) \{\s*this\(false\);\s*\}','',geometry)
   geometry=re.sub(r'(\.addBox\([^;]*?), (true|false)\);',lambda m:m[1]+', 0.0f, '+m[2]+');' if len(m[1].split(','))==6 else m[0],geometry)
   data=constructor(geometry,name)
   actions,motions,missing=extract(src,data['parts'])
   data.update(animationRoot='$mesh',headLook='',actions=actions,motions=[],expressions=[],poses=[],source=path.relative_to(ROOT).as_posix(),sourceSha256=hashlib.sha256(path.read_bytes()).hexdigest())
   write(O/'recovered_creatures'/('wilds_'+name.lower()+'.json'),data)
   recovered[name]={'parts':len(data['parts']),'actions':list(actions)}
   if missing:limitations[name]=missing
  except (ValueError,KeyError,SyntaxError) as error:limitations[name]=str(error)
 # Original chamber, including its authored guardian entity; native jigsaw preserves entities.
 chamber=RES/'data/jugcraft/structure/wilds/wroughtnaut_chamber.nbt';chamber.parent.mkdir(parents=True,exist_ok=True)
 chamber.write_bytes(nouveau_nbt.adapt((D/'structures/wroughtnaut_chamber.nbt').read_bytes(),lambda v:mapped(v)))
 ident='wilds_wrought_chamber'
 write(RES/'data/jugcraft/worldgen/template_pool'/(ident+'.json'),{'name':'jugcraft:'+ident,'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'jugcraft:wilds/wroughtnaut_chamber','processors':'minecraft:empty','projection':'rigid','override_liquid_settings':'ignore_waterlogging'}}]})
 write(RES/'data/jugcraft/tags/worldgen/biome/wilds_wrought_chamber.json',{'replace':False,'values':['#minecraft:is_forest','#minecraft:is_taiga','#minecraft:is_mountain','#minecraft:is_hill','#minecraft:is_jungle','#minecraft:is_savanna','#minecraft:is_badlands','minecraft:plains','minecraft:sunflower_plains','minecraft:desert','minecraft:snowy_plains','minecraft:swamp','minecraft:mangrove_swamp']})
 write(RES/'data/jugcraft/worldgen/structure'/(ident+'.json'),{'type':'minecraft:jigsaw','biomes':'#jugcraft:wilds_wrought_chamber','step':'underground_structures','spawn_overrides':{},'terrain_adaptation':'bury','start_pool':'jugcraft:'+ident,'size':0,'start_height':{'type':'minecraft:uniform','min_inclusive':{'absolute':20},'max_inclusive':{'absolute':50}},'max_distance_from_center':32,'use_expansion_hack':False})
 write(RES/'data/jugcraft/worldgen/structure_set'/(ident+'.json'),{'structures':[{'structure':'jugcraft:'+ident,'weight':1}],'placement':read(D/'worldgen/structure_set/wrought_chambers.json')['placement']})
 write(RES/'data/jugcraft/loot_table/entities/wilds_frostmaw.json',{'type':'minecraft:entity','pools':[]})
 write(RES/'data/jugcraft/jukebox_song/wilds_petiole.json',{'sound_event':'jugcraft:wilds_music.petiole','description':{'translate':'item.jugcraft.wilds_music_disc_petiole.desc'},'length_in_seconds':140,'comparator_output':14})
 lang=read(O/'lang/en_us.json');lang['effect.jugcraft.wilds_frozen']='Frozen';lang.setdefault('item.jugcraft.wilds_music_disc_petiole.desc','Petiole');write(O/'lang/en_us.json',lang)
 if (A/'textures/mob_effect/frozen.png').exists():copy(A/'textures/mob_effect/frozen.png',O/'textures/mob_effect/wilds_frozen.png')
 frost=RES/'data/jugcraft/structure/wilds/frostmaw_spawn.nbt';frost.parent.mkdir(parents=True,exist_ok=True);frost.write_bytes(nouveau_nbt.adapt((D/'structures/frostmaw_spawn.nbt').read_bytes(),lambda v:mapped(v)))
 ident='wilds_frostmaw_spawn'
 write(RES/'data/jugcraft/worldgen/template_pool'/(ident+'.json'),{'name':'jugcraft:'+ident,'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'jugcraft:wilds/frostmaw_spawn','processors':'minecraft:empty','projection':'rigid'}}]})
 write(RES/'data/jugcraft/worldgen/structure'/(ident+'.json'),{'type':'jugcraft:wilds_frost_site','biomes':['minecraft:snowy_plains','minecraft:ice_spikes','minecraft:snowy_slopes','minecraft:frozen_peaks','minecraft:jagged_peaks'],'step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin'})
 write(RES/'data/jugcraft/worldgen/structure_set'/(ident+'.json'),{'structures':[{'structure':'jugcraft:'+ident,'weight':1}],'placement':read(D/'worldgen/structure_set/frostmaw_spawns.json')['placement']})
 write(RES/'jugcraft/newest_wilds.json',{'blocks':BLOCKS,'items':ITEMS,'sounds':list(sounds)})
 write(CAT/'wilds-local.json',{'implemented_blocks':BLOCKS,'implemented_items':ITEMS,'recipes':recipes,'recovered_geometry':recovered,'recovery_limitations':limitations,'validated':False})
 print('Wilds source geometry:',recovered)
 print('Source conversion limitations:',{k:len(v) if isinstance(v,list) else v for k,v in limitations.items()})
if __name__=='__main__':main()
