"""Author package 4 assets without building, launching, auditing or testing the mod."""
import json
from PIL import Image
import generate_material_data as data
import advanced_electronics as electronics
import petro
import petro_textures
import write_ceramics_resources

def main():
    write_ceramics_resources.main()
    lang_path=data.ASSETS/'lang/en_us.json'
    lang=json.loads(lang_path.read_text(encoding='utf-8'))
    electronics.write_extras(data.write,data.ASSETS,data.DATA/'jugcraft',lang,data.condition)
    data.petro_assets(lang)
    data.tags()
    for kind,name,recipe in petro.fluid_recipe_files(data.condition):
        if name.startswith('electronics_'):
            data.write(data.DATA/'jugcraft/recipe'/kind/(name+'.json'),recipe)
    data.write(lang_path,dict(sorted(lang.items())))
    def save(image,kind,name):
        path=data.ASSETS/'textures'/kind/(name+'.png')
        path.parent.mkdir(parents=True,exist_ok=True)
        image.save(path,optimize=True)
    electronics.draw_all(save)
    # Match the canonical full texture writer's fluid seeds and animation metadata.
    for index,(name,info) in enumerate(petro.FLUIDS.items()):
        if name not in electronics.FLUIDS: continue
        for suffix,frames,delay in [('still',petro_textures.still(info['colors'],800+index),3),
                                    ('flow',petro_textures.flowing(info['colors'],850+index),2)]:
            strip=Image.new('RGBA',(16,16*len(frames)))
            for at,frame in enumerate(frames): strip.paste(frame,(0,16*at))
            save(strip,'block',name+'_'+suffix)
            data.write(data.ASSETS/'textures/block'/f'{name}_{suffix}.png.mcmeta',dict(animation=dict(frametime=delay)))
        save(petro_textures.bucket(info['colors'],900+index),'item',name+'_bucket')

if __name__=='__main__': main()
