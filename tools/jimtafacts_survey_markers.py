"""Original placed marker geometry and pixels, connected to native notebook surveys."""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets/buildcraftcore'

def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

def generate(shared=True):
    manifest=[]
    for kind, dye in [('volume','blue'),('path','green')]:
        name=f'jimta_{kind}_beacon'
        model=json.loads((SOURCE/'models/block/torch_center_lit.json').read_text())
        model['parent']='minecraft:block/block'
        model['textures']['all']=f'jugcraft:block/jimtafacts/industry/marker_{kind}'
        source=SOURCE/f'textures/blocks/marker_{kind}.png'
        target=RES/f'assets/jugcraft/textures/block/jimtafacts/industry/marker_{kind}.png'
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source,target)
        dump(RES/f'assets/jugcraft/models/block/{name}.json',model)
        dump(RES/f'assets/jugcraft/blockstates/{name}.json',{'variants':{'':{'model':f'jugcraft:block/{name}'}}})
        dump(RES/f'assets/jugcraft/items/{name}.json',{'model':{'type':'minecraft:model','model':f'jugcraft:block/{name}'}})
        dump(RES/f'data/jugcraft/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'jugcraft:{name}'}],'condition':{'type':'minecraft:survives_explosion'}}]})
        dump(RES/f'data/jugcraft/recipe/jimtafacts_industry/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['minecraft:redstone_torch',f'minecraft:{dye}_dye'],'result':{'id':f'jugcraft:{name}','count':1}})
        manifest.append({'source':str(source.relative_to(ROOT)).replace('\\','/'),'runtime':str(target.relative_to(ROOT)).replace('\\','/'),'geometry':'models/block/torch_center_lit.json','changes':'namespace only; original UVs and pixels'})
    if shared:
        path=RES/'assets/jugcraft/lang/en_us.json'
        lang=json.loads(path.read_text(encoding='utf-8'))
        lang.update({'block.jugcraft.jimta_volume_beacon':'Volume Survey Beacon','block.jugcraft.jimta_path_beacon':'Path Survey Beacon'})
        dump(path,lang)
    dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/survey-marker-runtime-copies.json',manifest)

if __name__=='__main__':
    generate()
