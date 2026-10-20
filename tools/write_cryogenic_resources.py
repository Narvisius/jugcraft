"""Author package 7 resources; deliberately no build, audit, test or game launch."""
import json
from PIL import Image
import generate_material_data as data
import cryogenic_fuels as fuels
import petro
import petro_textures
import write_ceramics_resources

def main():
    write_ceramics_resources.main()
    assets, dest = data.ASSETS, data.DATA/'jugcraft'
    lang_path=assets/'lang/en_us.json'
    lang=json.loads(lang_path.read_text(encoding='utf-8'))
    # Emit this slice only: leave earlier custom item models intact.
    for fluid,info in fuels.FLUIDS.items():
        lang['block.jugcraft.'+fluid]=info['display']
        data.write(assets/'blockstates'/(fluid+'.json'),{'variants':{'':{'model':'jugcraft:block/'+fluid}}})
        data.write(assets/'models/block'/(fluid+'.json'),{'textures':{'particle':'jugcraft:block/'+fluid+'_still'}})
        name=fluid+'_bucket'
        lang['item.jugcraft.'+name]=info['display']+' Bucket'
        data.write(assets/'models/item'/(name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/'+name}})
        data.write(assets/'items'/(name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:item/'+name}})
    for name,display in fuels.ITEMS.items():
        lang['item.jugcraft.'+name]=display
        data.write(assets/'models/item'/(name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/'+name}})
        data.write(assets/'items'/(name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:item/'+name}})
    fuels.write_extras(data.write,assets,dest,lang,data.condition)
    from cryogenic_models import tank_model
    alternate=data.PACKS/'alternate_machines/assets/jugcraft'
    for name in fuels.BLOCKS:
        data.write(alternate/'blockstates'/(name+'.json'),{'variants':{'':{'model':'jugcraft:block/'+name}}})
        data.write(alternate/'models/block'/(name+'.json'),tank_model(name=='vacuum_insulated_tank'))
        data.write(alternate/'items'/(name+'.json'),{'model':{'type':'minecraft:model','model':'jugcraft:block/'+name}})
    data.tags()
    for kind,name,recipe in petro.fluid_recipe_files(data.condition):
        if name.startswith('cryo_'): data.write(dest/'recipe'/kind/(name+'.json'),recipe)
    data.write(lang_path,dict(sorted(lang.items())))
    def save(image,kind,name):
        path=assets/'textures'/kind/(name+'.png')
        path.parent.mkdir(parents=True,exist_ok=True)
        image.save(path,optimize=True)
    fuels.draw_all(save)
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
