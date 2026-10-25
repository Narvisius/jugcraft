"""Owner seasonal skins on native Jugcraft seasonal systems; originals remain unchanged."""
from menagerie_content import *

def cube(a,b,texture='#all'):
    return {'from':a,'to':b,'faces':{face:{'uv':[0,0,16,16],'texture':texture} for face in ['north','south','east','west','up','down']}}

def gravestone_recipe(style, material):
    return {'type': 'minecraft:crafting_shaped',
            'pattern': [' S ', 'SSS', ' S '] if style == 'cross' else ['SS', 'SS', ' S'],
            'key': {'S': 'minecraft:mossy_cobblestone' if material == 'mossy' else 'minecraft:stone'},
            'result': {'id': f'jugcraft:herbal_gravestone_{style}_{material}'}}


def main():
    lang=read(ASSET/'lang/en_us.json');sounds=read(ASSET/'sounds.json')
    for p in (SOURCE/'textures').rglob('*.png'):copy(p,ASSET/'textures/menagerie/herbal'/p.relative_to(SOURCE/'textures'))
    for p in (SOURCE/'sounds').glob('*.ogg'):
        copy(p,ASSET/'sounds/menagerie/herbal'/p.name);sounds['herbal_'+p.stem]={'sounds':['jugcraft:menagerie/herbal/'+p.stem]}
    for p in (SOURCE/'textures/item').glob('*.png'):
        if p.stem in {'favicon1','yarn','candy_corn','ectoplasm'}:continue
        id='herbal_'+p.stem
        write(ASSET/f'models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:menagerie/herbal/item/'+p.stem}})
        write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'jugcraft:item/'+id}})
        lang['item.jugcraft.'+id]=p.stem.replace('_',' ').title()
    for source,id in [('yarn','yarn'),('candy_corn','candy_corn'),('ectoplasm','ectoplasm')]:
        # Existing shared materials retain identity and function; use the supplied item icon.
        write(ASSET/f'models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:menagerie/herbal/item/'+source}})
    for style in ['cross','upright']:
        for material in ['stone','mossy']:
            id=f'herbal_gravestone_{style}_{material}';lang['block.jugcraft.'+id]=(material+' '+style+' Gravestone').title()
            elements=[cube([2,0,4],[14,3,12])]
            if style=='cross':elements += [cube([6,3,7],[10,10,9]),cube([6,13,7],[10,16,9]),cube([2,10,7],[14,13,9])]
            else:elements += [cube([3,3,6],[13,15,10])]
            write(ASSET/f'models/block/{id}.json',{'textures':{'all':f'jugcraft:menagerie/herbal/block/gravestone_{style}_{material}','particle':f'jugcraft:menagerie/herbal/block/gravestone_{style}_{material}'},'elements':elements})
            write(ASSET/f'blockstates/{id}.json',{'variants':{f'facing={direction}':{'model':'jugcraft:block/'+id,'y':rotation} for direction,rotation in [('north',0),('east',90),('south',180),('west',270)]}})
            write(DATA/f'loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:'+id}]}]})
            write(DATA/f'recipe/{id}.json',gravestone_recipe(style, material))
    wheel=read(ASSET/'models/block/spinning_wheel.json')
    wheel['textures']={k:'jugcraft:menagerie/herbal/block/spinning_wheel' for k in wheel['textures']}
    write(ASSET/'models/block/herbal_spinning_wheel.json',wheel)
    write(ASSET/'blockstates/herbal_spinning_wheel.json',json.loads(json.dumps(read(ASSET/'blockstates/spinning_wheel.json')).replace('jugcraft:block/spinning_wheel','jugcraft:block/herbal_spinning_wheel')))
    write(ASSET/'items/herbal_spinning_wheel.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/herbal_spinning_wheel'}})
    lang['block.jugcraft.herbal_spinning_wheel']='Herbalist Spinning Wheel'
    write(DATA/'recipe/herbal_spinning_wheel.json',{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:spinning_wheel','minecraft:green_dye'],'result':{'id':'jugcraft:herbal_spinning_wheel'}})
    write(DATA/'loot_table/blocks/herbal_spinning_wheel.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:herbal_spinning_wheel'}]}]})
    write(ASSET/'blockstates/herbal_pumpkling_crop.json',{'variants':{f'age={i}':{'model':f'jugcraft:block/herbal_pumpkling_stage{i}'} for i in range(8)}})
    for i in range(8):write(ASSET/f'models/block/herbal_pumpkling_stage{i}.json',{'parent':'minecraft:block/cross','textures':{'cross':f'jugcraft:menagerie/herbal/block/pumpkling_stage{i}'}})
    write(ASSET/'items/herbal_pumpkling_crop.json',read(ASSET/'items/herbal_pumpkling_seeds.json'))
    write(DATA/'loot_table/blocks/herbal_pumpkling_crop.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jugcraft:herbal_pumpkling_seeds'}]}]})
    write(DATA/'recipe/herbal_pumpkling_seeds.json',{'type':'minecraft:crafting_shapeless','ingredients':['minecraft:pumpkin_seeds','jugcraft:ectoplasm'],'result':{'id':'jugcraft:herbal_pumpkling_seeds','count':2}})
    for color in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']:
        write(DATA/f'recipe/herbal_{color}_cloth.json',{'type':'minecraft:crafting_shaped','pattern':['YY','YY'],'key':{'Y':'jugcraft:yarn'},'result':{'id':f'jugcraft:herbal_{color}_cloth'}} if color=='white' else {'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:herbal_white_cloth','minecraft:'+color+'_dye'],'result':{'id':f'jugcraft:herbal_{color}_cloth'}})
    write(DATA/'tags/item/herbal_cloth.json',{'values':['jugcraft:herbal_'+c+'_cloth' for c in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']]})
    for set in ['witch','murder']:
        for layer,kind in [(1,'humanoid'),(2,'humanoid_leggings')]:copy(SOURCE/f'textures/models/armor/{set}_layer_{layer}.png',ASSET/f'textures/entity/equipment/{kind}/herbal/{set}.png')
        write(ASSET/f'equipment/herbal/{set}.json',{'layers':{kind:[{'texture':'jugcraft:herbal/'+set}] for kind in ['humanoid','humanoid_leggings']}})
        for part,pattern in [('hat' if set=='witch' else 'mask',['CCC','C C']),('robe' if set=='witch' else 'coat',['C C','CCC','CCC']),('pants',['CCC','C C','C C']),('boots',['C C','C C'])]:
            write(DATA/f'recipe/herbal_{set}_{part}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{'C':'jugcraft:herbal_'+('purple' if set=='witch' else 'black')+'_cloth'},'result':{'id':f'jugcraft:herbal_{set}_{part}'}})
    for name in ['pumpkling','ghost']:
        tw,th=Image.open(SOURCE/f'textures/entity/{name}.png').size
        bones=[{'name':'root','pivot':[0,0,0]}]
        def part(n,origin,size,uv):bones.append({'name':n,'parent':'root','pivot':[0,origin[1]+size[1],0],'cubes':[{'origin':origin,'size':size,'uv':uv}]})
        if name=='pumpkling':
            part('head',[-4,4,-4],[8,8,8],[0,0]);part('stem',[-1,12,-1],[2,3,2],[0,16]);part('left',[-3,0,-1],[2,4,2],[0,18]);part('right',[1,0,-1],[2,4,2],[0,18])
        else:
            part('head',[-4,16,-4],[8,8,8],[0,0]);part('body',[-4,5,-2],[8,11,4],[16,16]);part('left',[-8,7,-2],[4,11,4],[40,16]);part('right',[4,7,-2],[4,11,4],[40,16])
        id='herbal_'+name
        write(ASSET/f'geckolib/models/entity/menagerie/{id}.geo.json',{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.'+id,'texture_width':tw,'texture_height':th,'visible_bounds_width':3,'visible_bounds_height':3,'visible_bounds_offset':[0,1,0]},'bones':bones}]})
        write(ASSET/f'geckolib/animations/entity/menagerie/{id}.animation.json',{'format_version':'1.8.0','animations':{'idle':{'loop':True,'animation_length':2,'bones':{'root':{'position':[0,'math.sin(query.anim_time * 180) * 0.5',0]}}},'walk':{'loop':True,'animation_length':1,'bones':{'left':{'rotation':['math.sin(query.anim_time * 360) * 25',0,0]},'right':{'rotation':['math.sin(query.anim_time * 360) * -25',0,0]}}}}})
    lang['entity.jugcraft.herbal_pumpkling']='Herbalist Pumpkling';lang['entity.jugcraft.veil_ghost']='Veil Ghost'
    lang['block.jugcraft.herbal_pumpkling_crop']='Pumpkling Vine'
    write(ASSET/'lang/en_us.json',lang);write(ASSET/'sounds.json',sounds)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18/seasonal-adaptation.json',{'runtimeFiles':PROVENANCE,'sharedIdentities':['yarn','candy_corn','ectoplasm'],'newCodeReuses':['Pumpkling','RestlessSpirit','GravestoneBlock','SpinningWheelBlock'],'excludedPresentationFiles':['item/favicon1.png'],'geometry':'New native geometry for ghost/pumpkling; supplied files contain only textures.'})

if __name__=='__main__':main()
