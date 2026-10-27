"""Seven original gel stages; bounded claim-aware native spreading and portable-water recovery."""
from pathlib import Path
import hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets/buildcraftfactory'
STAGES=['spread_0','spread_1','spread_2','spread_3','gelling_0','gelling_1','gel']
def blocks():return {'jimta_water_gel'}
def items():return {'jimta_water_gel_seed','jimta_gelled_water'}
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def generate(shared=True):
 manifest=[]
 def copy(source,target):
  src=SOURCE/source;dst=RES/target;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
  manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'changes':'None'})
 for stage in STAGES:
  copy(f'textures/blocks/gel/{stage}.png',f'assets/jugcraft/textures/block/jimtafacts/gel/{stage}.png')
  dump(RES/f'assets/jugcraft/models/block/jimtafacts/gel/{stage}.json',{'parent':'minecraft:block/cube_all','textures':{'all':'jugcraft:block/jimtafacts/gel/'+stage}})
 dump(RES/'assets/jugcraft/blockstates/jimta_water_gel.json',{'variants':{f'stage={n}':{'model':'jugcraft:block/jimtafacts/gel/'+s} for n,s in enumerate(STAGES)}})
 for ident,source in [('jimta_water_gel_seed','water_gel'),('jimta_gelled_water','gel')]:
  copy(f'textures/items/{source}.png',f'assets/jugcraft/textures/item/jimtafacts/gel/{source}.png')
  dump(RES/f'assets/jugcraft/models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/gel/'+source}})
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+ident}})
 # Native oil-residue equivalent: the existing vacuum-distillation asphalt binder.
 dump(RES/'data/jugcraft/recipe/jimtafacts_factory/water_gel_seed.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':[' S ','SRS',' S '],'key':{'S':'minecraft:sand','R':'jugcraft:asphalt_binder'},'result':{'id':'jugcraft:jimta_water_gel_seed'}})
 dump(RES/'data/jugcraft/recipe/jimtafacts_factory/gel_to_water.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['jugcraft:jimta_gelled_water','minecraft:bucket'],'result':{'id':'minecraft:water_bucket'}})
 dump(RES/'data/jugcraft/loot_table/blocks/jimta_water_gel.json',{'type':'minecraft:block','pools':[{'rolls':1,'condition':{'type':'minecraft:survives_explosion'},'entries':[{'type':'minecraft:item','name':'jugcraft:jimta_gelled_water'}]}]})
 if shared:
  p=RES/'assets/jugcraft/lang/en_us.json';v=json.loads(p.read_text());v.update({'block.jugcraft.jimta_water_gel':'Water Gel','item.jugcraft.jimta_water_gel_seed':'Water Gel Seed','item.jugcraft.jimta_gelled_water':'Gelled Water'});dump(p,v)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/water-gel-runtime-copies.json',{'copies':manifest,'adaptations':['Seven original stage textures, 200/400-tick stage pacing.','Native finite spread: at most 3 sources per node stage, 64 visited/128 discovered positions, 12-block radius from origin, claims and loaded-only access. Pauses for absent owner.','One portable gel per broken block; original random extra drops omitted. Existing asphalt binder replaces source oil-residue bucket.','No library programs executed. ItemWaterGel and BlockWaterGel classes read statically with CFR.']})
 return items()
if __name__=='__main__':generate()
