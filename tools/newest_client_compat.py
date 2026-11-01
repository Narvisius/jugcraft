"""Native client codec adaptations. Only runtime copies change; original art is retained."""
import json
import re
import struct
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/jugcraft'

def write(path,value):
    text=json.dumps(value,indent=2,ensure_ascii=False)+'\n'
    if path.read_text(encoding='utf-8')!=text:path.write_text(text,encoding='utf-8')

def animation(value):
    # Old Gecko timelines accept arrays; Gecko 5 expects one script per timestamp.
    # Preserve every instruction, and sort numeric keyframes for its streaming baker.
    if isinstance(value,list):return [animation(v) for v in value]
    if isinstance(value,dict):
        result={k:animation(v) for k,v in value.items()}
        if result and all(re.fullmatch(r'\d+(?:\.\d+)?',k) for k in result):
            result=dict(sorted(result.items(),key=lambda kv:float(kv[0])))
        if 'timeline' in result:
            result['timeline']={t:' '.join(v) if isinstance(v,list) else v for t,v in result['timeline'].items()}
        return result
    # Seven corrupt authoring-export offsets. Keep the authored sine/cosine motion
    # and finite angles; an undefined additive offset has no playable meaning.
    if isinstance(value,str):return re.sub(r'\bNaN\b','0',value,flags=re.I)
    return value

def finish():
    for path in (ASSETS/'textures/newest').rglob('*.png.mcmeta'):
        data=json.loads(path.read_text());a=data.get('animation')
        if isinstance(a,dict) and a.get('frames')==[]:
            png=Path(str(path)[:-7])
            with png.open('rb') as f:width,height=struct.unpack('>II',f.read(24)[16:24])
            fw=a.get('width',min(width,height));fh=a.get('height',min(width,height))
            if width%fw or height%fh:raise ValueError(f'Invalid animation grid: {path}')
            a.update(width=fw,height=fh,frames=list(range((width//fw)*(height//fh))))
            write(path,data)
    for path in (ASSETS/'geckolib/animations').rglob('*.json'):
        if '/wilds/' not in path.as_posix():continue
        data=animation(json.loads(path.read_text()))
        if data.get('animations')=={}:
            # Static supplied meshes still need a valid Gecko animation resource.
            data['animations']={'static_pose':{'loop':True,'animation_length':1,'bones':{}}}
        write(path,data)
    labels={
        'forbidden_dark_matter':'Arcane Dark Matter','wilds_dart':'Blowgun Dart',
        'supps_flax':'Household Flax','supps_flax_seeds':'Household Flax Seeds',
        'forbidden_rune':'Hephaestus Rune','supps_soap':'Household Soap',
        'aquatic_fish_bones':'Angler Fish Bones','ribbits_maraca':'Frog Maraca',
        'wilds_blowgun':'Tribal Blowgun','jugtania_pixie_dust':'Elven Pixie Dust',
        'supps_wrench':'Rotation Wrench','holidayworks_candy_cane':'Festive Candy Cane',
        'wilds_spear':'Tribal Spear','aquatic_treasure_chest':'Angler Treasure Chest',
        'aquatic_catfish_bucket':'Bucket of Angler Catfish','aquatic_catfish_spawn_egg':'Angler Catfish Spawn Egg',
        'aquatic_fish_fillet_cooked':'Cooked Angler Fish Fillet',
        'aquatic_jellyfish_bucket':'Bucket of Angler Jellyfish','aquatic_jellyfish_spawn_egg':'Angler Jellyfish Spawn Egg',
        'aquatic_piranha':'Angler Piranha','aquatic_piranha_bucket':'Bucket of Angler Piranha',
        'aquatic_piranha_spawn_egg':'Angler Piranha Spawn Egg','aquatic_tin_can':'Discarded Fishing Tin',
        'jugtania_ender_dagger':'Ender Dagger','jugtania_record_gaia_1':'Scathed Music Disc — I',
        'jugtania_record_gaia_2':'Scathed Music Disc — II',
    }
    language=ASSETS/'lang/en_us.json';data=json.loads(language.read_text(encoding='utf-8'))
    for name,label in labels.items():data['item.jugcraft.'+name]=label
    write(language,data)
    for path in (ASSETS/'models/newest').rglob('*.json'):
        data=json.loads(path.read_text());textures=data.get('textures',{})
        # This source used a comment inside the texture-ID dictionary.
        for key,value in list(textures.items()):
            if value=='for LLibrary bug not putting particle into textures set':del textures[key]
        # Optional Create fluid sheets were not supplied. Use installed honey and
        # the owner's chocolate surface on these native sink fill planes.
        for key,value in list(textures.items()):
            if value in ('create:fluid/honey_still','create:fluid/honey_flow'):textures[key]='minecraft:block/honey_block_top'
            elif value in ('create:fluid/chocolate_still','create:fluid/chocolate_flow'):textures[key]='jugcraft:menagerie/cavern/block/chocolate'
            elif value=='supplementaries:block/rope':textures[key]='jugcraft:newest/supps/block/rope'
        # Four cross-mod candle variants lack their optional dependency sheets.
        # Preserve supplied holder geometry and use matching native candle wax/
        # wick UVs (skull-candle sheets have an incompatible 32x16 layout).
        candle_materials={'buzzier_bees:block/cupric_candle':'cyan',
            'buzzier_bees:block/ender_candle':'purple',
            'buzzier_bees:block/soul_candle':'light_blue',
            'cave_enhancements:block/spectacle_candle':'white'}
        for key,value in list(textures.items()):
            base=value.removesuffix('_lit')
            if base in candle_materials:
                textures[key]='minecraft:block/'+candle_materials[base]+'_candle'+('_lit' if value.endswith('_lit') else '')
        if data.get('parent')=='minecraft:block/chain':data['parent']='minecraft:block/iron_chain'
        if data.get('parent')=='minecraft:block/template_farmland':
            data['parent']='minecraft:block/template_cube_bottom_top_indented'
            dirt=textures.pop('dirt','minecraft:block/dirt');textures.update(bottom=dirt,side=dirt)
        if data.get('parent')=='minecraft:builtin/entity':
            # Legacy base carries transforms only; native item definitions own the
            # registered special renderer. Do not invent a cube for entity items.
            data['parent']='minecraft:block/block'
        write(path,data)

if __name__=='__main__':finish()
