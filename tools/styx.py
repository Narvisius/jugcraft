"""Styxhexenhammer's original model, flowers and conservatory. Owner reference: docs/images/styxhexenhammer-reference.jpg.
Geometry and palette are editable here; no pixels are copied from the JPEG. No new runtime dependency.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
FLOWERS = {
    'stolas_starflower': 'Stolas Starflower', 'witchglass_orchid': 'Witchglass Orchid',
    'ravenquill_lupine': 'Ravenquill Lupine', 'amethyst_mourningbell': 'Amethyst Mourningbell',
    'eclipse_camellia': 'Eclipse Camellia', 'astral_verbena': 'Astral Verbena',
    'inkvein_helleborine': 'Inkvein Helleborine', 'violet_lanternbloom': 'Violet Lanternbloom',
}
TALL = 'ravenquill_lupine'
PALETTE = ['292435','352a44','453153','614271','735187','171923','232633','303445','424454',
           '38281c','4c3524','624731','7b593d','987256','b5886c','d5a588','e6b799',
           '333630','585b50','a1a695','efeee4','25201b','816b42','ad905b','42334f','634577',
           '9460b9','bc88db','211a24','493449','814558','c2757a','6f6e74','adadb3','d5a588']

def blocks():
    return list(FLOWERS) + ['potted_' + n for n in FLOWERS if n != TALL]

def wizard_model():
    boxes = []
    def box(part, color, x,y,z,w,h,d, turn=0):
        boxes.append(dict(part=part,color=color,box=[x,y,z,w,h,d],turn=turn))
    # Normal player-sized head with a flat player-style face texture. The glasses are separate thin frames; eyes and nose are texture pixels.
    box('head',34,-4,-8,-4,8,8,8)
    box('head',10,-4.1,-8.1,2.1,8.2,10.1,2.1)
    for x in (-4.2,3.5):
        box('head',10,x,-7.8,-2.8,0.7,9.8,6.8)
        box('head',12,x,-5.8,-2.9,0.35,6.8,0.2)
    # Thin separate glasses: the eye pixels stay flat on the face; only the open frames project.
    for x in (-3.55,0.35):
        for y in (-5.75,-3.30):box('head',18,x,y,-4.52,3.20,0.22,0.22)
        for xx in (x,x+2.98):box('head',18,xx,-5.53,-4.52,0.22,2.23,0.22)
    box('head',18,-0.35,-4.95,-4.57,0.70,0.20,0.24)
    for side in (-1,1):
        box('head',17,3.35 if side>0 else -4.25,-5.66,-4.40,0.90,0.20,0.22)
        box('head',17,4.03 if side>0 else -4.25,-5.66,-4.18,0.22,0.20,4.60)
    hat_start=len(boxes)
    # Uneven brim, tiered crown and bent-over tip, keeping the reference silhouette.
    box('head',0,-8,-9.1,-7.2,16,0.85,14)
    box('head',2,-7.2,-9.65,-6.6,14.4,0.65,12.5)
    box('head',1,-5.4,-12.2,-5.1,10.8,2.8,10)
    box('head',3,-5.85,-10.9,-5.5,11.7,0.95,10.8)
    box('head',28,-4.8,-13.2,-4.8,9.6,1.6,9.4)
    box('head',1,-4.7,-16.0,-4.2,8.8,3.2,8.4)
    box('head',2,-4.3,-18.4,-3.8,7.1,2.9,7.4)
    box('head',1,-5.0,-20,-3.2,5.7,2,6.2)
    box('head',0,-6.4,-19.5,-2.9,3,2.1,5.5)
    box('head',0,-7.35,-18.2,-2.5,1.5,2.2,4.4)
    box('head',3,1.1,-16.1,-4.26,1.3,1.1,0.14)
    box('head',4,-3.8,-12.15,-5.15,1.0,1.3,0.14)
    box('head',3,4,-9.55,-6.66,1.5,0.6,0.15)
    # Seat the brim slightly into the hairline instead of leaving air above the head.
    for piece in boxes[hat_start:]:piece['box'][1]+=0.65
    # Long dark robes, purple placket, shoulder pieces and chest locks.
    box('body',6,-4.3,0,-2.35,8.6,11.5,4.7)
    box('body',1,-1.5,0.4,-2.6,3,10.9,0.5)
    for x in (-1.8,1.3):
        box('body',3,x,0.8,-2.9,0.5,10.5,0.45)
        for y in (3,7.5): box('body',22,x,y,-3,0.5,0.7,0.4)
    for x,h in ((-3.6,8.9),(2.1,8.1)):
        box('body',10,x,-0.5,-3.05,1.6,h,0.75)
        box('body',12,x+0.5,1,-3.2,0.6,h-0.4,0.45)
        box('body',9,x+0.2,h-0.5,-3.0,0.9,1.5,0.65)
    for part in ('left_arm','right_arm'):
        box(part,7,-2,-2,-2,4,10,4)
        box(part,8,-2.3,-2.35,-2.3,4.6,3,4.6)
        box(part,3,-2.35,0.45,-2.4,4.7,0.7,4.7)
        box(part,0,-2.2,7,-2.2,4.4,1.5,4.4)
        box(part,3,-2.25,7.5,-2.25,4.5,0.6,4.5)
        box(part,15,-1.8,8.5,-1.8,3.6,2.7,3.6)
    box('body',21,-4.6,10,-2.6,9.2,1.1,5.2)
    box('body',32,-0.7,9.85,-2.9,1.4,1.5,0.7)
    for x,c in ((-3.1,30),(2.3,26)):
        box('body',22,x+0.2,9.15,-3,0.8,0.75,0.6)
        box('body',32,x,10.0,-3.7,1.5,2.4,1.1)
        box('body',c,x+0.2,10.55,-3.83,1.1,1.45,0.8)
        box('body',33,x+0.15,10.2,-3.85,0.35,1,0.2)
    for part in ('left_leg','right_leg'):
        box(part,6,-2.1,0,-2.15,4.2,10.8,4.3)
        box(part,1,-1.2,0,-2.4,2.4,10.4,0.55)
        for x in (-1.6,1.1):
            box(part,3,x,0.2,-2.6,0.5,10.5,0.35)
            for y in (2.7,6.3): box(part,22,x,y,-2.7,0.5,0.85,0.3)
        box(part,3,-2.15,10.1,-2.35,4.3,0.8,4.6)
        box(part,21,-2,10.9,-2.5,4,1.1,4.8)
    # Staff attached to left hand: open frame instead of a solid paddle.
    box('staff',28,2.2,-8.6,-1.5,0.85,32,0.85)
    box('staff',10,2.15,-8,-1.55,0.35,30,0.2)
    for y in (-7,-1,6,15,22): box('staff',1,1.9,y,-1.8,1.5,0.9,1.4)
    for x in (0.2,4.2): box('staff',0,x,-14,-1.6,0.8,5.4,1)
    box('staff',2,0.8,-15,-1.6,3.7,0.8,1)
    box('staff',1,1.0,-9.1,-1.6,3.4,0.8,1)
    box('staff',25,1.25,-13.6,-1.65,2.8,3.6,1.1)
    box('staff',26,1.8,-13.8,-1.85,1.3,3.9,1.3)
    box('staff',27,1.85,-13.4,-1.97,0.65,1.3,0.2)
    # Pivot the staff through the hand, with its crystal leaning clear of the hat.
    for piece in boxes:
        if piece['part']=='staff':
            piece['box'][0]-=1.2
            piece['box'][1]-=9
    return boxes

def wizard_texture():
    im=Image.new('RGBA',(512,512))
    d=ImageDraw.Draw(im)
    for i,c in enumerate(PALETTE):
        x,y=(i%8)*64,(i//8)*64
        d.rectangle((x,y,x+63,y+63),fill='#'+c)
    # Larger atlas allows thin glasses while keeping the normal eight-unit player head and short eyes.
    # Robe/hair patches remain aligned to Minecraft-sized pixels.
    for index,patches in [(6,[(1,2,3,5,'292d3b'),(5,8,7,11,'1d202b'),(8,3,11,5,'292d3b'),(13,12,15,14,'1d202b')]),
                          (10,[(1,1,2,6,'60452e'),(4,4,5,10,'3e2d21'),(9,1,10,12,'644730'),(13,7,14,14,'3e2d21')])]:
        x,y=(index%8)*64,(index//8)*64
        for x0,y0,x1,y1,color in patches:d.rectangle((x+x0,y+y0,x+x1,y+y1),fill='#'+color)
    im=im.resize((1024,1024),Image.Resampling.NEAREST)
    d=ImageDraw.Draw(im)
    x,y=((34%8)*64+8)*2,((34//8)*64+8)*2
    def rect(box,color):d.rectangle((x+box[0],y+box[1],x+box[2],y+box[3]),fill='#'+color)
    skin='d5a588';hair='4c3524';beard='624731';frame='50534a'
    rect((0,0,15,15),skin)
    rect((0,0,15,1),hair);rect((0,2,1,15),hair);rect((14,2,15,15),hair)
    rect((3,3,6,3),hair);rect((9,3,12,3),hair)
    rect((2,6,5,7),'e9e9de');rect((4,6,5,7),'352f24')
    rect((10,6,13,7),'e9e9de');rect((10,6,11,7),'352f24')
    rect((7,8,8,10),'b5886c')
    rect((2,11,13,15),beard);rect((2,10,3,12),beard);rect((12,10,13,12),beard)
    rect((5,10,10,11),hair);rect((6,12,9,12),'c39477');rect((5,13,10,13),'73533a')
    return im

def flower_sprite(name,age=2):
    """Original 16-pixel pixel art on vanilla crossed planes, not sculpted petals or stems."""
    tall=name==TALL
    im=Image.new('RGBA',(16,32 if tall else 16));d=ImageDraw.Draw(im)
    stem='#40603b';leaf='#668449';shade='#2b4832'
    bottom=im.height-1
    def line(points,color=stem):d.line(points,fill=color,width=1)
    def leaves(x,y):
        line([(x-4,y-2),(x-2,y-2),(x,y)],leaf)
        line([(x+1,y-3),(x+3,y-5),(x+4,y-5)],leaf)
        d.point((x-2,y-1),fill=shade)
    if age<2 and not tall:
        h=5 if age==0 else 9
        line([(8,bottom),(8,bottom-h)])
        leaves(8,13)
        if age==1:d.rectangle((7,5,9,7),fill='#795584')
        return im
    if name=='stolas_starflower':
        line([(7,15),(7,7)]);leaves(7,13)
        for x,y in [(7,4),(3,7),(10,7),(5,10),(9,10)]:
            d.rectangle((x,y,x+1,y+1),fill='#dacddb')
            line([(x,y),(7,7)],'#b5a4c9')
        d.rectangle((6,6,8,8),fill='#795393');d.point((7,7),fill='#e8d5b0')
    elif name=='witchglass_orchid':
        line([(8,15),(8,6)]);leaves(8,14)
        for rect in [(4,4,6,6),(9,4,11,6),(5,7,10,8),(7,9,8,10)]:d.rectangle(rect,fill='#ac80bc')
        d.rectangle((7,5,8,7),fill='#5a315f');d.point((8,7),fill='#e7caaa')
    elif tall:
        line([(8,31),(8,3)]);leaves(8,28);leaves(8,24)
        for y in range(4,23,3):
            width=1 if y<10 else 2
            d.rectangle((8-width,y,8+width,y+1),fill='#51425f')
            d.point((8-width,y),fill='#b8b5c5')
        d.point((8,2),fill='#8b829b')
    elif name=='amethyst_mourningbell':
        line([(8,15),(8,5),(6,4),(4,5)]);line([(8,8),(11,6),(13,7)]);leaves(8,14)
        for x,y in [(4,5),(12,7)]:
            d.rectangle((x-1,y,x+1,y+3),fill='#835a9b');d.line((x-2,y+3,x+2,y+3),fill='#b99dca');d.point((x,y+1),fill='#aa8abb')
    elif name=='eclipse_camellia':
        line([(8,15),(8,8)]);leaves(8,13)
        d.rectangle((5,4,10,9),fill='#552d45');d.rectangle((4,5,11,8),fill='#552d45')
        d.rectangle((6,5,9,8),fill='#864361');d.rectangle((7,6,8,7),fill='#c5a35c');d.point((5,5),fill='#aa627a')
    elif name=='astral_verbena':
        for x,y in [(4,5),(8,3),(12,6)]:
            line([(8,15),(8,10),(x,y+1)])
            d.line((x-1,y,x+1,y),fill='#c2d8df');d.line((x,y-1,x,y+1),fill='#e0e4db');d.point((x,y),fill='#a69bbe')
        leaves(8,14)
    elif name=='inkvein_helleborine':
        line([(7,15),(7,5)]);leaves(7,13)
        for x,y in [(5,7),(10,4)]:
            line([(7,10),(x,y+2)])
            d.rectangle((x-2,y,x+2,y+2),fill='#bcc39a');d.rectangle((x-1,y+1,x+1,y+3),fill='#d9d9b5');d.point((x,y+2),fill='#76566c');d.point((x-1,y),fill='#76566c')
    else:
        line([(8,15),(8,5),(5,4),(4,5)]);line([(8,8),(11,6),(12,7)]);leaves(8,14)
        for x,y in [(4,5),(12,7)]:
            d.rectangle((x-1,y,x+1,y+4),fill='#795076');d.rectangle((x-2,y+1,x+2,y+3),fill='#795076');d.rectangle((x,y+1,x+1,y+3),fill='#c590ba');d.point((x,y+5),fill='#51384f')
    return im


def write_all(write,assets,data,lang):
    lang['tag.item.jugcraft.styx_flowers']='Nightglass Flowers'
    lang['entity.jugcraft.styxhexenhammer']='Styxhexenhammer'
    lang['message.jugcraft.styx.full']='Make room for eight flower cuttings, then speak to me again.'
    lang['message.jugcraft.styx.gift']='Styxhexenhammer: Take a cutting. Bring back a garden.'
    lines=['Stolas teaches the stars. The greenhouse teaches patience. I find I need more of the second.',
           'The orchids are thriving. Their names are proving more troublesome.',
           'These flowers are still a mystery. For now, let them grow.',
           'Rituals and pacts will come in time. Tonight, I study the stars.']
    for i,line in enumerate(lines):lang[f'message.jugcraft.styx.line.{i}']='Styxhexenhammer: '+line
    write(assets/'styx_model.json',wizard_model())
    for name,display in FLOWERS.items():
        lang['block.jugcraft.'+name]=display
        if name==TALL:
            for half,offset in [('lower',0),('upper',16)]:
                write(assets/'models/block'/f'{name}_{half}.json',{'parent':'minecraft:block/cross','textures':{'cross':f'jugcraft:block/{name}_{half}'}})
            variants={f'half={h}':{'model':f'jugcraft:block/{name}_{h}'} for h in ('lower','upper')}
        else:
            for age in range(3):
                texture=name if age==2 else f'{name}_{age}'
                write(assets/'models/block'/f'{name}_{age}.json',{'parent':'minecraft:block/cross','textures':{'cross':f'jugcraft:block/{texture}'}})
            variants={f'age={a}':{'model':f'jugcraft:block/{name}_{a}'} for a in range(3)}
            pot='potted_'+name;lang['block.jugcraft.'+pot]='Potted '+display
            write(assets/'models/block'/f'{pot}.json',{'parent':'minecraft:block/flower_pot_cross','textures':{'plant':'jugcraft:block/'+name}})
            write(assets/'blockstates'/f'{pot}.json',{'variants':{'':{'model':'jugcraft:block/'+pot}}})
            write(assets/'items'/f'{pot}.json',{'model':{'type':'minecraft:model','model':'jugcraft:block/'+pot}})
            write(data/'loot_table/blocks'/f'{pot}.json',{'type':'minecraft:block','pools':[pool('minecraft:flower_pot'),pool('jugcraft:'+name)]})
        write(assets/'blockstates'/f'{name}.json',{'variants':variants})
        write(assets/'models/item'/f'{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'jugcraft:block/{name}'+('_upper' if name==TALL else '')}})
        write(assets/'items'/f'{name}.json',{'model':{'type':'minecraft:model','model':f'jugcraft:item/{name}'}})
        loot=pool('jugcraft:'+name)
        if name==TALL:loot['entries'][0]['condition']={'type':'minecraft:match_block','blocks':'jugcraft:'+name,'state':{'half':'lower'}}
        write(data/'loot_table/blocks'/f'{name}.json',{'type':'minecraft:block','pools':[loot]})
    for registry in ('item','block'):
        write(data/f'tags/{registry}/styx_flowers.json',{'replace':False,'values':['jugcraft:'+n for n in FLOWERS]})
    import styx_structure, styx_structure_v1, styx_structure_v2
    write(data/'styx/conservatory.json',styx_structure.build())
    write(data/'styx/conservatory_v1.json',styx_structure_v1.build())
    write(data/'styx/conservatory_v2.json',styx_structure_v2.build())

def pool(item):
    return {'rolls':1,'entries':[{'type':'minecraft:item','name':item}],'condition':{'type':'minecraft:survives_explosion'}}

def textures():
    out=ROOT/'src/main/resources/assets/jugcraft/textures'
    (out/'entity').mkdir(parents=True,exist_ok=True)
    wizard_texture().save(out/'entity/styxhexenhammer.png')
    for name in FLOWERS:
        sprite=flower_sprite(name)
        if name==TALL:
            sprite.crop((0,0,16,16)).save(out/'block'/f'{name}_upper.png')
            sprite.crop((0,16,16,32)).save(out/'block'/f'{name}_lower.png')
        else:
            sprite.save(out/'block'/f'{name}.png')
            for age in (0,1):flower_sprite(name,age).save(out/'block'/f'{name}_{age}.png')

if __name__=='__main__':textures()
