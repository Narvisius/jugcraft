"""Dry-country foliage and food in the existing 16px natural-art pipeline; owner roster palettes."""
from crop_textures import Canvas, rgb

PINE = ('232f20', '31422d', '40573b', '4f6a4a', '61825a', '779970')
TAMARISK = ('222d24', '303f32', '3f5342', '4e6652', '607c64', '76937a')
YUCCA = ('33371f', '484d2b', '5e6439', '747a48', '8d9558', 'a2aa74')


def cones(image, ripe):
    c=Canvas(); c.img=image.copy()
    colors=tuple(map(rgb, ('49301e','79502e','a07848') if ripe else ('37412a','59633c','7c8850')))
    for x,y in ((3,3),(11,6),(6,11)):
        for dx,dy,t in ((0,0,2),(1,0,1),(-1,1,1),(0,1,0),(1,1,1),(0,2,1),(1,2,0),(0,3,0)):
            c.px(x+dx,y+dy,colors[t])
    return c.img


def textures():
    from wood_style import paint_leaves, pal
    pine=paint_leaves(pal(*PINE),'needles',10801,gap_threshold=.08)
    tamarisk=paint_leaves(pal(*TAMARISK),'leaves',10802,gap_threshold=.15)
    c=Canvas(); c.img=tamarisk
    for x,y in ((2,3),(9,1),(12,10),(5,11)):
        c.line(x,y+3,x+1,y,rgb(TAMARISK[2]))
        for dx,dy,tone in ((0,0,'c09aa8'),(-1,1,'a07888'),(1,1,'a07888'),(0,2,'7e5a6a')):
            c.px(x+dx,y+dy,rgb(tone))
    wattle=paint_leaves(pal('303e2a','405039','546142','6e7c5a','87936e','a1aa87'),'leaves',10803,gap_threshold=.08)
    c=Canvas(); c.img=wattle
    for x,y in ((2,2),(8,2),(12,5),(4,7),(9,10),(2,12),(13,13)):
        for dx,dy,tone in ((0,0,'dabc77'),(1,0,'d3a844'),(-1,1,'b98f2b'),(0,1,'d3a844'),(1,1,'b98f2b'),(0,2,'997520')):
            c.px(x+dx,y+dy,rgb(tone))
    # Each spiky rosette has an opaque joined heart, so fine tips never look like detached pixels.
    c=Canvas()
    for cx,cy in ((4,5),(11,11),(12,3),(3,12)):
        for dx,dy in ((-3,-4),(-1,-4),(1,-4),(3,-3),(-4,-1),(4,0),(-3,3),(2,3)):
            c.line(cx,cy,cx+dx,cy+dy,rgb(YUCCA[4 if dx<0 else 2]))
        c.line(cx-1,cy,cx+1,cy,rgb(YUCCA[3])); c.px(cx,cy-1,rgb(YUCCA[5]))
    fronds=c.img
    out={('block','stone_pine_needles'):pine,('block','stone_pine_needles_unripe'):cones(pine,False),
         ('block','stone_pine_needles_ripe'):cones(pine,True),('block','wattle_leaves'):wattle,
         ('block','tamarisk_leaves'):tamarisk,('block','joshua_tree_fronds'):fronds}
    for tree,colors,bark in (('stone_pine',PINE,'805a3d'),('wattle',('546142','997520','b98f2b','d3a844','dabc77','6e7c5a'),'70624d'),
                             ('tamarisk',TAMARISK,'745153'),('joshua_tree',YUCCA,'aca688')):
        c=Canvas(); c.line(8,15,8,5,rgb(bark))
        for x,y in ((4,6),(11,5),(4,10),(12,10)):
            c.line(8,12,x,y,rgb(bark))
            if tree=='joshua_tree':
                for dx,dy in ((-2,-3),(0,-3),(2,-2),(-2,0),(2,0)): c.line(x,y,x+dx,y+dy,rgb(colors[4]))
            else:
                for dx,dy in ((-2,0),(-1,-1),(0,-1),(1,0),(0,1)): c.px(x+dx,y+dy,rgb(colors[4 if dy<0 else 2]))
        out['block',tree+'_sapling']=c.img
    for name,colors in (('pine_nuts',('78603b','bca477','e2cea3')),('roasted_pine_nuts',('654027','a87543','d6a66b'))):
        c=Canvas()
        for x,y in ((4,4),(10,3),(7,9),(12,10)):
            for dx,dy,t in ((0,-1,0),(-1,0,0),(0,0,2),(1,0,0),(-1,1,0),(0,1,2),(1,1,1),(0,2,1),(1,2,0),(0,3,0)):
                c.px(x+dx,y+dy,rgb(colors[t]))
        out['item',name]=c.img
    return out
