"""New fungal/End sprites in the existing editable 16px natural texture pipeline."""
from pathlib import Path
import random
from PIL import Image
from crop_textures import Canvas, rgb

FRONDS=('102a2a','173836','1f4744','295752','346861','457c74')


def textures():
    from wood_style import paint_leaves, pal
    out={}
    # Broad, joined fans: no detached needles against the End sky.
    c=Canvas()
    for cx,cy in ((4,5),(12,11),(12,3),(4,13)):
        for dx,dy in ((-4,-3),(-2,-4),(0,-4),(2,-3),(4,-2),(-4,0),(4,1)):
            c.line(cx,cy,cx+dx,cy+dy,rgb(FRONDS[3 if dx>0 else 5]))
            c.line(cx,cy,cx+dx,cy+dy+1,rgb(FRONDS[2]))
        c.rect(cx-1,cy-1,cx+1,cy+1,rgb(FRONDS[3]))
    out['block','dragonblood_fronds']=c.img
    c=Canvas(); c.rect(7,8,8,15,rgb('879291'))
    for x,y in ((3,6),(11,5),(8,3)):
        c.line(8,11,x,y,rgb('a6afab'))
        for dx,dy in ((-3,-1),(-2,-2),(0,-3),(2,-2),(3,0)):
            c.line(x,y,x+dx,y+dy,rgb(FRONDS[4]))
    out['block','dragonblood_sapling']=c.img
    c=Canvas()
    for x,y in ((5,7),(10,10)):
        for dx,dy,t in ((0,-3,0),(-1,-2,0),(0,-2,2),(-2,-1,0),(-1,-1,3),(0,-1,2),(1,-1,0),(-2,0,0),(-1,0,2),(0,0,1),(1,0,0),(-1,1,0),(0,1,0)):
            c.px(x+dx,y+dy,rgb(('512323','81352c','b9543d','d58358')[t]))
    out['item','dragonblood_resin']=c.img
    for name,palette,seed in (
        ('glowcap_block',('1d5a6e','2a8aa0','3fbfd0','79e6ee','c4fbff'),10901),
        ('glowcap_pores',('718278','8a9a8c','a1afa1','b0bcb0','d6e2d2'),10902),
        ('blight_wart_block',('1e1a1e','2c262c','3c343a','4e444c','9a9296'),10903)):
        c=Canvas(); rng=random.Random(seed)
        c.rect(0,0,15,15,rgb(palette[1]))
        for _ in range(30):
            x,y=rng.randrange(16),rng.randrange(16); tone=rng.randrange(3)
            for dx in range(rng.randint(2,4)):
                for dy in range(rng.randint(2,3)):
                    c.px((x+dx)%16,(y+dy)%16,rgb(palette[tone]))
        for _ in range(10):
            x,y=rng.randrange(16),rng.randrange(16)
            c.px(x,y,rgb(palette[rng.choice((3,4))]))
        out['block',name]=c.img
    c=Canvas(); c.rect(7,8,8,15,rgb('665966')); c.rect(3,5,12,8,rgb('332d3b'))
    c.rect(5,3,10,5,rgb('584554')); c.line(4,6,9,6,rgb('75607c')); c.px(10,8,rgb('24212b'))
    out['block','blight_fungus']=c.img
    original=Path(__file__).resolve().parents[1]/'art/owner-library/originals/Blocks/biomes and tree blocks/bramble_leaves.png'
    out['block','bramble_bush_leaves']=Image.open(original).convert('RGBA')
    return out
