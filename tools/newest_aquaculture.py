"""Native resources and exact supplied fish meshes/animations. Original classes are only static inputs."""
from pathlib import Path
import re,json
from newest_building import write as raw_write,copy,map_value,read,merge_tag
from menagerie_data import loot as native_loot
def write(path,data):raw_write(path,native_loot(data) if "/loot_table/" in str(path).replace("\\","/") else data)
from newest_twigs import recipe
ROOT=Path(__file__).resolve().parents[1];SRC=ROOT/'art/owner-library/originals/Edited-2026-10-29/Aquacultah';RES=ROOT/'src/main/resources';REF=ROOT/'build/owner-oct29-reference/com/teammetallurgy/aquaculture'
def loot_conditions(v):
 if isinstance(v,list):return [loot_conditions(x) for x in v]
 if not isinstance(v,dict):return v
 if v.get('condition')=='aquaculture:biome_tag_check':
  pred=v['predicate'];tests=[]
  if pred.get('include'):
   terms=[{'condition':'minecraft:location_check','predicate':{'biomes':'#'+tag}} for tag in pred['include']]
   tests.append({'condition':'minecraft:all_of' if pred.get('and') else 'minecraft:any_of','terms':terms})
  for tag in pred.get('exclude',[]):tests.append({'condition':'minecraft:inverted','term':{'condition':'minecraft:location_check','predicate':{'biomes':'#'+tag}}})
  return {'condition':'minecraft:all_of','terms':tests}
 out={k:loot_conditions(x) for k,x in v.items()}
 if out.get('condition')=='minecraft:location_check' and 'biome' in out.get('predicate',{}):out['predicate']['biomes']=out['predicate'].pop('biome')
 return out

def main():
 sound_defs=read(SRC/'assets/aquaculture/sounds.json');sound_out=read(RES/'assets/jugcraft/sounds.json')
 for name,d in sound_defs.items():
  d=dict(d);entries=[]
  for entry in d['sounds']:
   if isinstance(entry,str):entries.append(entry.replace('aquaculture:','jugcraft:newest/aquatic/'))
   else:
    entry=dict(entry);entry['name']=entry['name'].replace('aquaculture:','jugcraft:aquatic_' if entry.get('type')=='event' else 'jugcraft:newest/aquatic/');entries.append(entry)
  d['sounds']=entries;sound_out['aquatic_'+name]=d
 write(RES/'assets/jugcraft/sounds.json',sound_out)
 write(RES/'jugcraft/newest_aquatic_sounds.json',{'sounds':list(sound_defs)})
 for path in (SRC/'assets/aquaculture/sounds').rglob('*'):
  if path.is_file():copy(path,RES/'assets/jugcraft/sounds/newest/aquatic'/path.relative_to(SRC/'assets/aquaculture/sounds'))
 fishes=[]
 for line in (REF/'init/AquaItems.java').read_text(encoding='utf-8').splitlines():
  if 'FishRegistry.register(' not in line:continue
  name=re.search(r'"([a-z_]+)"',line)[1];kind=re.search(r'FishType\.([A-Z]+)',line)
  fishes.append({'name':name,'shape':kind[1] if kind else 'MEDIUM','nutrition':0 if name in ('minnow','jellyfish') else 1 if 'SMALL_FISH_RAW' in line else 3 if 'LARGE_FISH_RAW' in line else 2})
 weights={}
 for line in (REF/'loot/FishWeightHandler.java').read_text(encoding='utf-8').splitlines():
  m=re.search(r'FISH_DATA.add\(\(Item\)AquaItems.([A-Z_]+).get\(\), ([0-9.]+), ([0-9.]+), (\d+)\)',line)
  if m:weights[m[1].lower()]={'min':float(m[2]),'max':float(m[3]),'fillets':int(m[4])}
 for fish in fishes:fish.update(weights.get(fish['name'],{}))
 names=[n for fish in fishes for n in (fish['name'],fish['name']+'_bucket',fish['name']+'_spawn_egg')]+['fish_fillet_raw','fish_fillet_cooked','sushi','fish_bones','algae']
 names += [n+'_fillet_knife' for n in ('wooden','stone','iron','gold','diamond','neptunium')]
 names += [n+'_fishing_rod' for n in ('iron','gold','diamond','neptunium')]
 names += [n+'_hook' for n in ('iron','gold','diamond','light','heavy','double','redstone','note','nether_star')]
 names += ['worm','leech','fishing_line','bobber','neptunium_nugget','neptunium_ingot','driftwood','tin_can','goldfish']
 names += ['neptunium_'+n for n in ('axe','pickaxe','shovel','hoe','sword','bow','helmet','chestplate','leggings','boots','block')]
 names += ['box_turtle','arrau_turtle','starshell_turtle','turtle_soup']
 names += [n+'_spawn_egg' for n in ('box_turtle','arrau_turtle','starshell_turtle')]
 names += ['tackle_box','neptunes_bounty','worm_farm','farmland','box','lockbox','treasure_chest','message_in_a_bottle','fish_mount']
 for folder in ('textures','models'):
  for p in (SRC/'assets/aquaculture'/folder).rglob('*'):
   if not p.is_file():continue
   target=RES/'assets/jugcraft'/folder/'newest/aquatic'/p.relative_to(SRC/'assets/aquaculture'/folder)
   if p.suffix=='.json':write(target,map_value(read(p),'aquaculture','aquatic',True))
   else:copy(p,target)
 for name in names:
  source=SRC/f'assets/aquaculture/items/{name}.json'
  write(RES/f'assets/jugcraft/items/aquatic_{name}.json',map_value(read(source),'aquaculture','aquatic',True) if source.exists() else {'model':{'type':'minecraft:model','model':f'jugcraft:newest/aquatic/item/{name}'}})
  if name.endswith('_spawn_egg') and not source.exists():write(RES/f'assets/jugcraft/items/aquatic_{name}.json',{'model':{'type':'minecraft:model','model':'minecraft:item/tropical_fish_spawn_egg'}})
 for name in ('farmland','neptunium_block','worm_farm'):
  write(RES/f'assets/jugcraft/blockstates/aquatic_{name}.json',map_value(read(SRC/f'assets/aquaculture/blockstates/{name}.json'),'aquaculture','aquatic',True))
  write(RES/f'data/jugcraft/loot_table/blocks/aquatic_{name}.json',map_value(read(SRC/f'data/aquaculture/loot_table/blocks/{name}.json'),'aquaculture','aquatic'))
 equipment=read(SRC/'assets/aquaculture/equipment/neptunium.json')
 for layers in equipment['layers'].values():
  for layer in layers:layer['texture']='jugcraft:newest/aquatic/neptunium'
 write(RES/'assets/jugcraft/equipment/aquatic_neptunium.json',equipment)
 for path in (SRC/'data/aquaculture/loot_table/gameplay/fishing').glob('*.json'):
  write(RES/'data/jugcraft/loot_table/aquatic_gameplay/fishing'/path.name,map_value(loot_conditions(read(path)),'aquaculture','aquatic'))
 for path in (SRC/'data/aquaculture/loot_table/box').glob('*.json'):
  write(RES/'data/jugcraft/loot_table/aquatic_box'/path.name,map_value(loot_conditions(read(path)),'aquaculture','aquatic'))
 # Optional Twilight compatibility predicate: no installed realm means no matches.
 write(RES/'data/c/tags/worldgen/biome/is_twilight.json',{'replace':False,'values':[]})
 for path in (SRC/'data/aquaculture/recipe').glob('neptunium_*.json'):
  write(RES/'data/jugcraft/recipe/newest/aquatic'/path.name,recipe(map_value(read(path),'aquaculture','aquatic')))
 write(RES/'data/jugcraft/recipe/newest/aquatic/fish_mount.json',recipe(map_value(read(SRC/'data/aquaculture/recipe/fish_mount.json'),'aquaculture','aquatic')))
 # Append instead of replacing shared vanilla/common tags owned by other Jugcraft systems.
 # Native arrows retain their existing IDs; the underwater behavior is attached to the bow's arrow.
 for namespace in ('minecraft','c','aquaculture'):
  for path in (SRC/f'data/{namespace}/tags').rglob('*.json'):
   relative=path.relative_to(SRC/f'data/{namespace}/tags').as_posix()
   if relative=='entity_type/arrows.json' or relative.startswith('worldgen/'):continue
   target=RES/f'data/{namespace}/tags'/relative
   if namespace=='aquaculture':
    kind,tail=relative.split('/',1);target=RES/f'data/jugcraft/tags/{kind}/aquatic_{tail}'
   merge_tag(target,map_value(read(path),'aquaculture','aquatic')['values'],write)
 write(RES/'jugcraft/newest_aquatic_messages.json',{'messages':[v for k,v in read(SRC/'assets/aquaculture/lang/en_us.json').items() if re.fullmatch(r'aquaculture.message[0-9]+',k)]})
 for family,name in [('ingots','neptunium_ingot'),('nuggets','neptunium_nugget'),('storage_blocks','neptunium_block')]:write(RES/f'data/c/tags/item/{family}/neptunium.json',{'values':['jugcraft:aquatic_'+name]})
 turtle_spawns=[read(SRC/f'data/aquaculture/neoforge/biome_modifier/{n}_spawn.json') for n in ('box_turtle','arrau_turtle','starshell_turtle')]
 for d in turtle_spawns:
  for key in ('includeBiomes','excludeBiomes'):
   if isinstance(d.get(key),str):d[key]=[d[key].replace('#aquaculture:','#jugcraft:aquatic_')]
 write(RES/'jugcraft/newest_aquatic_turtles.json',{'spawns':turtle_spawns})
 write(RES/'data/jugcraft/tags/item/aquatic_turtle.json',{'values':['jugcraft:aquatic_'+n for n in ('box_turtle','arrau_turtle','starshell_turtle')]})
 write(RES/'data/jugcraft/recipe/newest/aquatic/worm_farm.json',recipe(map_value(read(SRC/'data/aquaculture/recipe/worm_farm.json'),'aquaculture','aquatic')))
 write(RES/'data/jugcraft/tags/item/aquatic_turtle_edible.json',read(SRC/'data/aquaculture/tags/item/turtle_edible.json'))
 for name in ('box_turtle','arrau_turtle','starshell_turtle'):
  write(RES/f'data/jugcraft/loot_table/entities/aquatic_{name}.json',map_value(read(SRC/f'data/aquaculture/loot_table/entities/{name}.json'),'aquaculture','aquatic'))
 write(RES/'data/jugcraft/recipe/newest/aquatic/turtle_soup.json',recipe(map_value(read(SRC/'data/aquaculture/recipe/turtle_soup.json'),'aquaculture','aquatic')))
 for name in ('tackle_box','neptunes_bounty'):
  write(RES/f'assets/jugcraft/items/aquatic_{name}.json',{'model':{'type':'minecraft:special','base':f'jugcraft:newest/aquatic/item/{name}','model':{'type':'jugcraft:aquatic_station','tackle':name=='tackle_box'}}})
  write(RES/f'assets/jugcraft/blockstates/aquatic_{name}.json',{'variants':{'':{'model':f'jugcraft:newest/aquatic/item/{name}'}}})
  write(RES/f'data/jugcraft/loot_table/blocks/aquatic_{name}.json',map_value(read(SRC/f'data/aquaculture/loot_table/blocks/{name}.json'),'aquaculture','aquatic'))
  write(RES/f'data/jugcraft/recipe/newest/aquatic/{name}.json',recipe(map_value(read(SRC/f'data/aquaculture/recipe/{name}.json'),'aquaculture','aquatic')))
 write(RES/'data/jugcraft/tags/item/aquatic_tackle_box_green.json',map_value(read(SRC/'data/aquaculture/tags/item/tackle_box_green.json'),'aquaculture','aquatic'))
 methods=[]
 for source,method in [('TackleBoxModel','tackle'),('NeptunesBountyModel','bounty')]:
  text=(REF/f'client/model/{source}.java').read_text(encoding='utf-8')
  text=re.search(r'public static LayerDefinition .*?\n    }',text,re.S)[0]
  text=re.sub(r'public static LayerDefinition [^(]+',f'public static LayerDefinition {method}',text).replace('BOTTOM','"bottom"').replace('LID','"lid"')
  methods.append(text)
 target=ROOT/'src/client/java/io/github/jimbozoomer/jugcraft/client/aquatic/OwnerAquaticStationMeshes.java'
 target.write_text('package io.github.jimbozoomer.jugcraft.client.aquatic;\nimport net.minecraft.client.model.geom.PartPose;\nimport net.minecraft.client.model.geom.builders.*;\n/** Original supplied station cuboids, UVs and pivots. */\npublic final class OwnerAquaticStationMeshes {\n'+'\n'.join(methods)+'\n}\n',encoding='utf-8')
 spawns=[]
 for p in (SRC/'data/aquaculture/neoforge/biome_modifier').glob('*.json'):
  d=read(p)
  if d.get('spawn',{}).get('type','').split(':')[-1] in {f['name'] for f in fishes}:spawns.append(map_value(d,'aquaculture','aquatic'))
 for p in (SRC/'data/aquaculture/tags/worldgen/biome').rglob('*.json'):
  write(RES/'data/jugcraft/tags/worldgen/biome'/('aquatic_'+p.relative_to(SRC/'data/aquaculture/tags/worldgen/biome').as_posix()),map_value(read(p),'aquaculture','aquatic'))
 for fish in fishes:
  name=fish['name'];write(RES/f'data/jugcraft/loot_table/entities/aquatic_{name}.json',map_value(read(SRC/f'data/aquaculture/loot_table/entities/{name}.json'),'aquaculture','aquatic'))
 for name in ('cooked_fish_fillet','cooked_fish_fillet_from_campfire','cooked_fish_fillet_from_smoking','sushi','bonemeal_from_fish_bones','brown_mushroom_from_fish','red_mushroom_from_red_shrooma','wooden_fillet_knife','stone_fillet_knife','iron_fillet_knife','gold_fillet_knife','diamond_fillet_knife','neptunium_fillet_knife','iron_fishing_rod','golden_fishing_rod','diamond_fishing_rod','neptunium_fishing_rod','iron_hook','gold_hook','diamond_hook','double_hook','light_hook','heavy_hook','redstone_hook','note_hook','nether_star_hook','fishing_line','bobber','jellyfish_to_slimeball','planks_from_driftwood','tin_can_to_iron_nugget','tin_can_to_iron_nugget_from_blasting','gold_nugget_from_gold_fish','neptunium_ingot_from_nuggets','neptunium_nugget'):
  p=SRC/f'data/aquaculture/recipe/{name}.json'
  if p.exists():write(RES/f'data/jugcraft/recipe/newest/aquatic/{name}.json',recipe(map_value(read(p),'aquaculture','aquatic')))
 write(RES/'data/jugcraft/recipe/newest/aquatic/fish_fillet.json',{'type':'jugcraft:aquatic_fillet'})
 write(RES/'data/jugcraft/tags/item/aquatic_repairs_neptunium.json',{'values':['jugcraft:aquatic_neptunium_ingot']})
 merge_tag(RES/'data/minecraft/tags/item/fishes.json',['jugcraft:aquatic_'+f['name'] for f in fishes],write)
 models=['FishBaseModel','FishSmallModel','FishMediumModel','FishLargeModel','FishLongnoseModel','FishCathfishModel','JellyfishModel','AquaTropicalFishBModel','TurtleLandModel']
 for name in models:
  text=(REF/f'client/renderer/entity/model/{name}.java').read_text(encoding='utf-8')
  text=re.sub(r'/\*.*?\*/','',text,flags=re.S).strip()
  text=text.replace('com.teammetallurgy.aquaculture.client.renderer.entity.model','io.github.jimbozoomer.jugcraft.client.aquatic').replace('com.teammetallurgy.aquaculture.client.renderer.entity.state','io.github.jimbozoomer.jugcraft.client.aquatic')
  text=text.replace('import javax.annotation.Nonnull;','').replace('@Nonnull ','').replace('super.setupAnim((Object)renderState);','super.setupAnim(renderState);')
  out=ROOT/f'src/client/java/io/github/jimbozoomer/jugcraft/client/aquatic/{name}.java';out.parent.mkdir(parents=True,exist_ok=True);out.write_text('// Original supplied mesh and animation, with native package/API adaptation.\n'+text+'\n',encoding='utf-8')
 write(RES/'jugcraft/newest_aquatic_fishing.json',{'fish':read(SRC/'data/aquaculture/loot_table/gameplay/fishing/fish.json')['pools'][0]['entries']})
 write(RES/'jugcraft/newest_aquatic.json',{'fish':fishes,'spawns':spawns})
 langfile=RES/'assets/jugcraft/lang/en_us.json';lang=read(langfile)
 for key,value in read(SRC/'assets/aquaculture/lang/en_us.json').items():
  if key.startswith(('item.aquaculture.','entity.aquaculture.')):lang[key.replace('.aquaculture.','.jugcraft.aquatic_',1)]=value
 for key,value in read(SRC/'assets/aquaculture/lang/en_us.json').items():
  if key.startswith('aquaculture.subtitles.'):lang[key]=value
 for name in [f['name'] for f in fishes]+['box_turtle','arrau_turtle','starshell_turtle']:
  lang['item.jugcraft.aquatic_'+name+'_spawn_egg']=lang.get('entity.jugcraft.aquatic_'+name,name.replace('_',' ').title())+' Spawn Egg'
 for name in ['farmland','neptunium_block','worm_farm','tackle_box','neptunes_bounty']:
  lang['block.jugcraft.aquatic_'+name]=lang.get('item.jugcraft.aquatic_'+name,name.replace('_',' ').title())
 write(langfile,lang)
 write(ROOT/'art/owner-library/catalog/edited-2026-10-29/aquatic-integration.json',{'source':'Aquacultah','fish':fishes,'spawns':spawns,'models':models,'sourceExecuted':False,'status':'Local fish/turtles, fishing tackle, dye presentation, native weighted loot, worm farming, storage, fish trophies and neptunium equipment implemented. Native API/resource closure and combined validation remain. Not compiled or tested.'})
 print('Generated',len(fishes),'native fish descriptors and original meshes')
if __name__=='__main__':main()
