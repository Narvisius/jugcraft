"""Recover the owner's original statue geometry and poses; no source programs execute."""
from pathlib import Path
import hashlib
import json
import shutil

ID='jimta_spellbooks_player_statue'
IDS=(ID,'jimta_spellbooks_keeper_statue','jimta_spellbooks_ice_spider_statue')
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
SOURCE=ROOT/'art/owner-library/originals/Oct25/irons_lib/assets/irons_lib'

def generate(shared=True):
    records=[]
    def dump(p,v):
        if '/recipe/' in p.as_posix():v.setdefault('fabric:load_conditions',[{'condition':'jugcraft:feature_enabled','feature':'concordance'}])
        p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2,ensure_ascii=True)+'\n',encoding='utf-8')
    def copy(src,dst):
        dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
        records.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'changes':'None; original geometry, UVs, pixels or animation preserved.'})
    for name in ('player','player_slim','player_legacy','sbeve','statue_base'):
        copy(SOURCE/f'geckolib/models/statue/{name}.geo.json',RES/f'assets/jugcraft/geckolib/models/block/jimtafacts/statue/{name}.geo.json')
    copy(SOURCE/'geckolib/animations/statue_poses.animation.json',RES/'assets/jugcraft/geckolib/animations/block/jimtafacts/statue_poses.animation.json')
    for kind,mesh in (('keeper','citadel_keeper'),('ice_spider','ice_spider')):
        copy(ROOT/f'art/owner-library/originals/Oct23/Jimtafacts/assets/irons_spellbooks/geo/{mesh}.geo.json',RES/f'assets/jugcraft/geckolib/models/block/jimtafacts/statue/{kind}.geo.json')
    for name in ('sbeve','statue_base','ice_spider','keeper'):
        copy(SOURCE/f'textures/entity/statue/{name}.png',RES/f'assets/jugcraft/textures/jimtafacts/statue/{name}.png')
    copy(SOURCE/'textures/block/statue_particle.png',RES/'assets/jugcraft/textures/block/jimtafacts/statue_particle.png')
    # Exact original 14 x 2 x 14 pedestal and original per-face UVs. Native block
    # model renders the base; Gecko renders the poseable figure above it.
    faces={d:{'texture':'#base','uv':[0,14,14,16]} for d in ('north','south','east','west')}
    for d in ('up','down'):faces[d]={'texture':'#base','uv':[14,14,0,0]}
    base='jugcraft:block/jimtafacts/statue_base'
    dump(RES/'assets/jugcraft/models/block/jimtafacts/statue_base.json',{'textures':{'particle':'jugcraft:block/jimtafacts/statue_particle','base':'jugcraft:jimtafacts/statue/statue_base'},'elements':[{'from':[1,0,1],'to':[15,2,15],'faces':faces}]})
    dump(RES/'assets/jugcraft/models/block/jimtafacts/statue_empty.json',{'textures':{'particle':'jugcraft:block/jimtafacts/statue_particle'}})
    dump(RES/f'assets/jugcraft/blockstates/{ID}.json',{'variants':{'y_offset=0':{'model':base},'y_offset=1':{'model':'jugcraft:block/jimtafacts/statue_empty'}}})
    dump(RES/f'assets/jugcraft/models/item/{ID}.json',{'parent':'minecraft:block/block','display':{'gui':{'rotation':[25,-30,0],'translation':[0,-3,0],'scale':[.45,.45,.45]},'ground':{'translation':[0,3,0],'scale':[.25,.25,.25]},'fixed':{'translation':[0,-8,0],'scale':[.5,.5,.5]}}})
    dump(RES/f'assets/jugcraft/items/{ID}.json',{'model':{'type':'minecraft:special','base':'jugcraft:item/'+ID,'model':{'type':'geckolib:geckolib'}}})
    dump(RES/f'data/jugcraft/recipe/{ID}.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':[' S ',' S ','BBB'],'key':{'S':'minecraft:stone','B':'minecraft:smooth_stone_slab'},'result':{'id':'jugcraft:'+ID}})
    dump(RES/f'data/jugcraft/loot_table/blocks/{ID}.json',{'type':'minecraft:block','pools':[{'rolls':1,'condition':{'type':'minecraft:all_of','terms':[{'type':'minecraft:match_block','blocks':'jugcraft:'+ID,'state':{'y_offset':'0'}},{'type':'minecraft:survives_explosion'}]},'entries':[{'type':'minecraft:item','name':'jugcraft:'+ID}]}]})
    lang={'block.jugcraft.'+ID:'Poseable Player Statue'}
    for kind in ('keeper','ice_spider'):
        ident='jimta_spellbooks_'+kind+'_statue'
        for folder in ('blockstates','items','models/item'):
            value=json.loads((RES/f'assets/jugcraft/{folder}/{ID}.json').read_text())
            if folder=='items':value['model']['base']='jugcraft:item/'+ident
            if folder=='models/item':
                factor=.12 if kind=='ice_spider' else .8
                for transform in value['display'].values():transform['scale']=[v*factor for v in transform['scale']]
                for context in ('firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand','head'):
                    value['display'][context]={'rotation':[0,0,0],'translation':[0,0,0],'scale':[factor*.5]*3}
            dump(RES/f'assets/jugcraft/{folder}/{ident}.json',value)
        value=json.loads((RES/f'data/jugcraft/loot_table/blocks/{ID}.json').read_text())
        value=json.loads(json.dumps(value).replace(ID,ident))
        dump(RES/f'data/jugcraft/loot_table/blocks/{ident}.json',value)
        dump(RES/f'data/jugcraft/recipe/{ident}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['jugcraft:'+ID,'jugcraft:jimta_spellbooks_'+('icy_fang' if kind=='ice_spider' else 'cinder_essence')],'result':{'id':'jugcraft:'+ident}})
        lang['block.jugcraft.'+ident]=('Ice Spider' if kind=='ice_spider' else 'Citadel Keeper')+' Statue'
    if shared:
        path=RES/'assets/jugcraft/lang/en_us.json';data=json.loads(path.read_text());data.update(lang);dump(path,data)
    tag=RES/'data/minecraft/tags/block/mineable/pickaxe.json';data=json.loads(tag.read_text());data['values']=sorted(set(data['values'])|{'jugcraft:'+ident for ident in IDS});dump(tag,data)
    dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/statue-runtime-copies.json',{'source':'Owner-supplied Oct25 irons_lib','copies':records,'adaptations':['Native two-block placement and 23-pose menu. Pedestal has exact source box and UVs.','Player figures use original wide/slim/legacy geometry and vanilla client-known/default skins, converted into a bounded native stone palette. No source color-clustering pass, stone overlay or external supporter lookup.','Original sbeve mesh/texture is the fallback. Keeper and ice-spider variants pair the original creature geometry with the matching original stone texture; fixed neutral poses, scaled to pedestal size (0.8 and 0.12), retain authored UVs and geometry. They have no player pose menu.']})
    return lang

if __name__=='__main__':generate()
