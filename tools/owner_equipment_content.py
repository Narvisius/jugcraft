"""Native equipment layers and item model adapters, using unchanged owner PNGs.

This generates runtime resources; it is not a validation pass. Hand-coded source
renderers were not included, so their inventory icons supply native item models.
"""
from pathlib import Path
from menagerie_content import ROOT,RES,ASSET,DATA,read,write
from beasts_content import ARMOR as BEAST
from cavern_content import ARMOR as CAVERN
import shutil


def armor(group,name,slot):
    source=ASSET/'textures/menagerie'/group/'armor'
    if group=='beast':texture=name
    else:
        texture='primordial_armor' if name.startswith('primordial') else 'hazmat_suit' if name.startswith('hazmat') else 'diving_suit' if name.startswith('diving') else 'gingerbread_armor' if name.startswith('gingerbread') else 'darkness_armor' if name.endswith('darkness') else name
        if (source/(texture+'_0.png')).exists():texture+='_'+('1' if slot=='leggings' else '0')
    image=source/(texture+'.png')
    if not image.exists():return # Only supplied, registered pieces are processed below.
    layers={}
    for layer in ('humanoid','humanoid_leggings','humanoid_baby'):
        target=ASSET/'textures/entity/equipment'/layer/group/(name+'.png');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(image,target)
        layers[layer]=[{'texture':'jugcraft:'+group+'/'+name}]
    write(ASSET/'equipment'/group/(name+'.json'),{'layers':layers})


def items(group):
    models=ASSET/'models/menagerie'/group/'item';textures=ASSET/'textures/menagerie'/group/'item'
    catalog=read(RES/f'jugcraft/menagerie/{group}.json')
    adapted=[]
    for id,info in catalog['items'].items():
        name=info['source'];path=models/(name+'.json')
        model=read(path) if path.exists() else {}
        parent=model.get('parent','')
        if model and parent not in ('builtin/entity','minecraft:builtin/entity'):continue
        if name.startswith('spawn_egg_'):continue
        # These are the owner's actual inventory art, not a stretched entity atlas.
        image=textures/(name+'.png')
        if not image.exists() and name.startswith('shattered_'):image=textures/(name.removeprefix('shattered_')+'.png')
        if not image.exists():continue
        write(path,{'parent':'minecraft:item/handheld' if any(s in name for s in ('sword','staff','spear','club','dagger','gauntlet','carver','whip','pickaxe','grapple')) else 'minecraft:item/generated','textures':{'layer0':f'jugcraft:menagerie/{group}/item/{image.stem}'}})
        adapted.append(id)
    return adapted


def ogg_length(path):
    import struct
    data=path.read_bytes();header=data.index(b'\x01vorbis');rate=struct.unpack_from('<I',data,header+12)[0];last=data.rfind(b'OggS');samples=struct.unpack_from('<Q',data,last+6)[0]
    return round(samples/rate,2)


def patterns_and_music():
    for name in ('bear','australia_0','australia_1','new_mexico','brazil'):
        write(DATA/f'banner_pattern/beast_{name}.json',{'asset_id':'jugcraft:beast_'+name,'translation_key':'block.jugcraft.banner.beast_'+name})
        write(DATA/f'tags/banner_pattern/beast_pattern/{name}.json',{'values':['jugcraft:beast_'+name]})
        for kind in ('banner','shield'):
            image=ASSET/f'textures/menagerie/beast/entity/{kind}/{name}.png'
            if image.exists():
                target=ASSET/f'textures/entity/{kind}/beast_{name}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(image,target)
                atlas=RES/f'assets/minecraft/atlases/{kind}_patterns.json';d=read(atlas) if atlas.exists() else {'sources':[]};entry={'type':'minecraft:single','resource':f'jugcraft:entity/{kind}/beast_{name}'}
                if entry not in d['sources']:d['sources'].append(entry)
                write(atlas,d)
    for group,names in [('beast',('thime','daze')),('cavern',('fusion','tasty'))]:
        for name in names:
            event=f'{group}.music_disc_{name}' if group=='beast' else f'{group}.{name}_music_disc';audio=ASSET/f'sounds/menagerie/{group}/{"music_disc" if group=="beast" else "music"}/{name}.ogg'
            write(DATA/f'jukebox_song/{group}_{name}.json',{'sound_event':'jugcraft:'+event,'description':{'translate':f'item.jugcraft.{group}_music_disc_{name}.desc'},'length_in_seconds':ogg_length(audio),'comparator_output':12})


def main():
    changes={}
    lang=read(ASSET/'lang/en_us.json')
    for group,table in [('beast',BEAST),('cavern',CAVERN)]:
        catalog=read(RES/f'jugcraft/menagerie/{group}.json')
        for name,slot in table.items():
            if group+'_'+name in catalog['items']:armor(group,name,slot)
        changes[group]=items(group)
        repair=['minecraft:leather'] if group=='beast' else ['jugcraft:cavern_tough_hide','jugcraft:cavern_polymer_plate','jugcraft:cavern_shadow_silk','jugcraft:cavern_gingerbread_crumbs']
        write(DATA/f'tags/item/{group}_armor_repair.json',{'replace':False,'values':repair})
    patterns_and_music()
    import encyclopedia
    for page,title in encyclopedia.PAGES.items():lang['encyclopedia.jugcraft.page.'+page]=title
    for group,names in [('beast',('thime','daze')),('cavern',('fusion','tasty'))]:
        for name in names:lang[f'item.jugcraft.{group}_music_disc_{name}.desc']=name.title()
    for name in ('bear','australia_0','australia_1','new_mexico','brazil'):
        for color in 'white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split():lang[f'block.jugcraft.banner.beast_{name}.{color}']=color.replace('_',' ').title()+' '+name.replace('_',' ').title()
    write(ASSET/'lang/en_us.json',lang)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/equipment-runtime.json',{'native_inventory_models':changes,'note':'Source item renderer geometry was not supplied. Native inventory models use the unchanged item icons; special held models and custom armor silhouettes remain to be refined. Not tested.'})


if __name__=='__main__':main()
