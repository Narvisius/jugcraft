"""Author package 6 resources only; no builds, checks, tests or launches."""
import json
from PIL import Image
import generate_material_data as data
import material_icons
from materials import MINERALS
import synthetic_fuels as fuels
import petro
import petro_textures
import write_ceramics_resources

def main():
    write_ceramics_resources.main()
    assets, dest = data.ASSETS, data.DATA/'jugcraft'
    lang_path=assets/'lang/en_us.json'
    lang=json.loads(lang_path.read_text(encoding='utf-8'))
    data.petro_assets(lang)
    fuels.write_extras(data.write,assets,dest,lang,data.condition)
    blocks=['cobaltite_ore','deepslate_cobaltite_ore','cobaltite_block']
    for block in blocks:
        data.write(assets/'blockstates'/(block+'.json'),{'variants':{'':{'model':data.rid('block/'+block)}}})
        data.write(assets/'models/block'/(block+'.json'), material_icons.block_model(block)
                   or {'parent':'minecraft:block/cube_all','textures':{'all':data.rid('block/'+block)}})
        data.write(assets/'items'/(block+'.json'),{'model':{'type':'minecraft:model','model':data.rid('block/'+block)}})
        lang['block.jugcraft.'+block]=data.block_name(block)
        loot=data.ore_drop(block,'cobaltite',1,1) if block.endswith('_ore') else data.self_drop(block)
        data.write(dest/'loot_table/blocks'/(block+'.json'),loot)
    data.write(assets/'models/item/cobaltite.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/cobaltite'}})
    data.write(assets/'items/cobaltite.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/cobaltite'}})
    lang['item.jugcraft.cobaltite']='Cobaltite'
    data.compaction(dest/'recipe','machines','jugcraft:cobaltite','cobaltite','cobaltite_block')
    data.tags()
    gen=MINERALS['cobaltite']['gen']
    data.write(dest/'worldgen/feature/ore_cobaltite.json',{
        'type':data.rid('surface_linked_ore'),
        'ore':{'size':gen['size'],'discard_chance_on_air_exposure':0.0,
               'targets':data.layered_targets('cobaltite_ore','deepslate_cobaltite_ore')},
        'surface_biomes':data.rid('has_surface_ore/cobaltite'),
        'outcrop':data.rid('cobaltite_ore'),'outcrop_chance':gen['outcrop_chance']})
    data.placed_feature('cobaltite',gen)
    for kind,name,recipe in petro.fluid_recipe_files(data.condition):
        if name.startswith('synthetic_'): data.write(dest/'recipe'/kind/(name+'.json'),recipe)
    data.write(lang_path,dict(sorted(lang.items())))
    def save(image,kind,name):
        path=assets/'textures'/kind/(name+'.png')
        path.parent.mkdir(parents=True,exist_ok=True)
        image.save(path,optimize=True)
    fuels.draw_all(save)
    for (kind,name),img in material_icons.material_textures().items():
        if name in blocks+['cobaltite']: save(img,kind,name)
    for index,(name,info) in enumerate(petro.FLUIDS.items()):
        if name not in fuels.FLUIDS: continue
        for suffix,frames,delay in [('still',petro_textures.still(info['colors'],800+index),3),
                                    ('flow',petro_textures.flowing(info['colors'],850+index),2)]:
            strip=Image.new('RGBA',(16,16*len(frames)))
            for at,frame in enumerate(frames): strip.paste(frame,(0,16*at))
            save(strip,'block',name+'_'+suffix)
            data.write(assets/'textures/block'/f'{name}_{suffix}.png.mcmeta',dict(animation=dict(frametime=delay)))
        save(petro_textures.bucket(info['colors'],900+index),'item',name+'_bucket')

if __name__=='__main__': main()
