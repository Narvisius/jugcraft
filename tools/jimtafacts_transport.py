from jimtafacts_crafting import distinct
"""Original transport pixels on native connected pipe geometry and bounded logistics."""
from pathlib import Path
import json, shutil, hashlib
ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo/assets/buildcrafttransport/textures/pipes'
MATERIALS = ('cobblestone', 'stone', 'andesite', 'sandstone', 'quartz', 'gold')
KINDS = [m+'_'+flow for m in MATERIALS for flow in ('item','fluid','power') if not (m=='andesite' and flow=='power')] + ['iron_item','wood_item','diamond_item','iron_fluid','wood_fluid','diamond_fluid']
KINDS += ['clay_item','clay_fluid','iron_power','diamond_power','wood_power','diamond_wood_power','structure','void_item','void_fluid']
KINDS += ['obsidian_item','stripes_item','diamond_wood_item','diamond_wood_fluid','lapis_item','daizuli_item','emzuli_item']
def blocks(): return {'jimta_pipe_'+name for name in KINDS}
def items(): return blocks()
def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
def generate(shared=True):
    manifest=[]; lang={}
    def texture(name):
        src=SOURCE/(name+'.png'); dst=RES/'assets/jugcraft/textures/block/jimtafacts/transport'/src.name
        dst.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(src,dst)
        manifest.append({'source':src.relative_to(ROOT).as_posix(),'runtime':dst.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
        return 'jugcraft:block/jimtafacts/transport/'+name
    directions=[('north',{}),('east',{'y':90}),('south',{'y':180}),('west',{'y':270}),('up',{'x':270}),('down',{'x':90})]
    def cube(low,high):
        return {'from':low,'to':high,'faces':{face:{'texture':'#pipe','uv':[0,0,16,16]} for face,_ in directions}}
    for name in KINDS:
        id='jimta_pipe_'+name; material,flow=name.rsplit('_',1) if name!='structure' else ('structure','control')
        limiter=material in ('iron','diamond') and flow=='power'
        colored=material in ('lapis','daizuli','emzuli')
        device=material in ('obsidian','stripes','diamond_wood') and flow!='power'
        tex=texture(name+'_blue' if material in ('lapis','daizuli') else name+'_m128' if limiter else name+'_clear' if material in ('wood','iron','diamond_wood','emzuli') else name)
        base={'parent':'minecraft:block/block','textures':{'pipe':tex,'particle':'#pipe'}}
        radius=2 if flow in ('fluid','control') else 3
        low,high=8-radius,8+radius
        for suffix,elements in [('',[cube([low,low,low],[high,high,high])]),('_arm',[cube([low,low,0],[high,high,low])])]:
            dump(RES/f'assets/jugcraft/models/block/{id}{suffix}.json',{**base,'elements':elements})
        parts=[{'apply':{'model':'jugcraft:block/'+id}}]
        if material in ('lapis','daizuli'):
            parts=[]
            for color_index,color in enumerate(('white','orange','magenta','light_blue','yellow','lime','pink','gray','silver','cyan','purple','blue','brown','green','red','black')):
                model=id+'_color_'+str(color_index)
                dump(RES/f'assets/jugcraft/models/block/{model}.json',{**base,'textures':{'pipe':texture(name+'_'+color),'particle':'#pipe'},'elements':[cube([low,low,low],[high,high,high])]})
                parts.append({'when':{'color':str(color_index)},'apply':{'model':'jugcraft:block/'+model}})
        if material=='emzuli':
            for color_index,color in enumerate(('white','orange','magenta','light_blue','yellow','lime','pink','gray','silver','cyan','purple','blue','brown','green','red','black')):
                model=id+'_color_'+str(color_index)
                # Supplied colored lapis skin forms an attached collar; the emzuli body remains unchanged.
                dump(RES/f'assets/jugcraft/models/block/{model}.json',{**base,'textures':{'pipe':texture('lapis_item_'+color),'particle':'#pipe'},'elements':[cube([low-.2,high-.4,low-.2],[high+.2,high+.2,high+.2])]})
                parts.append({'when':{'color':str(color_index)},'apply':{'model':'jugcraft:block/'+model}})
        if limiter:
            parts=[]
            for index,source_rate in enumerate((0,4,8,16,32,64,128)):
                model=id+'_limit_'+str(index)
                dump(RES/f'assets/jugcraft/models/block/{model}.json',{**base,'textures':{'pipe':texture(name+'_m'+str(source_rate)),'particle':'#pipe'},'elements':[cube([low,low,low],[high,high,high])]})
                parts.append({'when':{'limit':str(index)},'apply':{'model':'jugcraft:block/'+model}})
        for face,rot in directions:
            entry={'apply':{'model':'jugcraft:block/'+id+'_arm',**rot}}
            if (material!='wood' or flow=='power') and name!='diamond_item' and material!='void' and not device: entry['when']={face:'true'}
            parts.append(entry)
        if material in ('iron','wood','diamond_wood') and flow!='power' or name=='diamond_item':
            # The bright end marker makes the outlet (wood: intake) visible on every orientation.
            end=texture(material+'_'+flow+'_filled' if material in ('iron','wood','diamond_wood') else 'diamond_item_north')
            dump(RES/f'assets/jugcraft/models/block/{id}_port.json',{**base,'textures':{'pipe':end,'particle':'#pipe'},'elements':[cube([low-.2,low-.2,0],[high+.2,high+.2,1])]})
            for face,rot in directions: parts.append({'when':{'facing':face},'apply':{'model':'jugcraft:block/'+id+'_port',**rot}})
        if material in ('stripes','obsidian'):
            # A raised collar marks the working face without inventing a replacement texture.
            dump(RES/f'assets/jugcraft/models/block/{id}_port.json',{**base,'elements':[cube([low-.5,low-.5,0],[high+.5,high+.5,1.5])]})
            for face,rot in directions:parts.append({'when':{'facing':face},'apply':{'model':'jugcraft:block/'+id+'_port',**rot}})
        dump(RES/f'assets/jugcraft/blockstates/{id}.json',{'multipart':parts})
        dump(RES/f'assets/jugcraft/items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+id}})
        lang['block.jugcraft.'+id]=material.title()+(' JE Pipe' if flow=='power' else ' '+flow.title()+' Pipe')
        if material=='wood' and flow!='power': lang['block.jugcraft.'+id]='Wood '+flow.title()+' Extraction Pipe'
        if material=='diamond' and flow!='power': lang['block.jugcraft.'+id]='Diamond '+flow.title()+' Sorting Pipe'
        if limiter:lang['block.jugcraft.'+id]=material.title()+' JE Limiter Pipe'
        if name=='structure':lang['block.jugcraft.'+id]='Structural Control Pipe'
        if material=='void':lang['block.jugcraft.'+id]='Void '+flow.title()+' Disposal Pipe'
        names={'obsidian':'Obsidian Collection Pipe','stripes':'Stripes Placement / Breaking Pipe','lapis':'Lapis Route Painter','daizuli':'Daizuli Color Gate','emzuli':'Emzuli Filtered Route Painter'}
        if material in names:lang['block.jugcraft.'+id]=names[material]
        if material=='diamond_wood' and flow!='power':lang['block.jugcraft.'+id]='Diamond Wood '+flow.title()+' Filtered Extractor'
        ingredient={'gold':'minecraft:gold_ingot','iron':'minecraft:iron_ingot','wood':'minecraft:oak_planks','diamond_wood':'minecraft:diamond','diamond':'minecraft:diamond','quartz':'minecraft:quartz','structure':'minecraft:cobblestone','void':'minecraft:obsidian','clay':'minecraft:clay_ball','stripes':'minecraft:gold_ingot','lapis':'minecraft:lapis_lazuli','daizuli':'minecraft:diamond','emzuli':'minecraft:emerald'}.get(material,'minecraft:'+material)
        ingredients=[ingredient,'minecraft:glass',ingredient]
        if flow=='fluid': ingredients.append('jugcraft:rubber')
        if flow=='power': ingredients.append('minecraft:redstone')
        if material in ('wood','diamond','diamond_wood','stripes','emzuli','daizuli'): ingredients.append('jugcraft:basic_circuit')
        dump(RES/f'data/jugcraft/recipe/jimtafacts_transport/{id}.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':distinct('jimtafacts_transport/'+id,ingredients),'result':{'id':'jugcraft:'+id,'count':1 if material in ('wood','diamond') or device or colored else 8}})
        dump(RES/f'data/jugcraft/loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id}],'condition':{'type':'minecraft:survives_explosion'}}]})
    dump(RES/'data/jugcraft/tags/block/jimtafacts_transport.json',{'replace':False,'values':['jugcraft:'+id for id in sorted(blocks())]})
    if shared:
        path=RES/'assets/jugcraft/lang/en_us.json'; data=json.loads(path.read_text(encoding='utf-8')); data.update(lang); dump(path,data)
        path=RES/'data/minecraft/tags/block/mineable/pickaxe.json'; data=json.loads(path.read_text(encoding='utf-8'))
        if '#jugcraft:jimtafacts_transport' not in data['values']: data['values'].append('#jugcraft:jimtafacts_transport')
        dump(path,data)
    dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/transport-runtime-copies.json',manifest)
    return items()
if __name__=='__main__': print(f'Generated {len(generate())} native transport variants.')
