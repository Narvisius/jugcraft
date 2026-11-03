"""Placed meals modeled from solid ingredients, using the owner's unchanged icon palette.

The old templates stretched transparent regions of an inventory icon over whole
bowl walls and meal heaps. Sample opaque texels as materials instead; geometry
describes the vessel and the individual ingredients. No PNGs are rewritten.

Eight plated meals are no longer drawn here: Cornbread Stuffing, Vegetable Noodles, Squid Ink Pasta, Bacon and Eggs,
Steak and Potatoes, Roasted Mutton Chops, Pasta with Meatballs and Tomato Sauce are sculpted in their own textures by
tools/menu_meal_models.py (tools/menu_data.py dish_model sends them there).
"""
from functools import lru_cache

FACES = ('up', 'down', 'north', 'south', 'east', 'west')


def elements(name, kind, image, heap=0):
    pixels = [(x, y, image.getpixel((x, y))) for y in range(16) for x in range(16)
              if image.getpixel((x, y))[3] == 255]
    if not pixels:
        raise ValueError(f'{name}: no opaque material texels')

    @lru_cache(maxsize=None)
    def material(rgb):
        x, y, _ = min(pixels, key=lambda p: sum((p[2][i] - rgb[i])**2 for i in range(3)))
        return [x + .15, y + .15, x + .85, y + .85]

    out = []
    def cuboid(lo, hi, rgb):
        out.append({'from': list(lo), 'to': list(hi), 'faces': {
            f: {'texture': '#dish', 'uv': material(tuple(max(0, int(c * (.78 if f == 'down' else .9 if f in ('north', 'east') else 1))) for c in rgb))}
            for f in FACES}})

    cream=(220,205,169); brown=(124,70,37); green=(77,112,40)
    if kind == 'bowl':
        # A supported foot and closed floor; four solid walls, with the soup
        # below their rim. Each material texel is opaque, including side views.
        cuboid((5,0,5),(11,.8,11),brown)
        cuboid((3.5,.8,3.5),(12.5,1.6,12.5),(145,91,47))
        for lo, hi in [((3,1.6,3),(13,5,4)),((3,1.6,12),(13,5,13)),
                       ((3,1.6,4),(4,5,12)),((12,1.6,4),(13,5,12))]:
            cuboid(lo,hi,(173,116,62))
        soup=(202,123,39) if 'pumpkin' in name else (153,100,43) if 'onion' in name else (185,145,74)
        cuboid((4,1.6,4),(12,4.3,12),soup)
        if 'onion' in name:
            for x,z in ((5,5),(8,7),(6,9)):
                cuboid((x,4.3,z),(x+2.4,4.6,z+.5),cream)
                cuboid((x,4.3,z+.5),(x+.5,4.6,z+1.4),cream)
        else:
            for i,(x,z) in enumerate(((5,5),(8,5),(6,8),(9,9),(9,7))):
                cuboid((x,4.3,z),(x+1,4.7+min(heap,1),z+1), (green,(218,139,48),cream)[i%3])
        return out

    # Low ceramic serving plate with a rim, rather than a deep square bowl.
    cuboid((3,0,3),(13,.45,13),(176,165,142))
    cuboid((1.5,.45,1.5),(14.5,.85,14.5),cream)
    for lo,hi in [((1,.85,2),(2,1.35,14)),((14,.85,2),(15,1.35,14)),
                  ((2,.85,1),(14,1.35,2)),((2,.85,14),(14,1.35,15))]:
        cuboid(lo,hi,(235,220,190))
    if name == 'grilled_salmon':
        cuboid((3,.85,4),(11,2.2,8),(204,111,73))
        cuboid((11,.85,5),(13,1.8,7),(185,90,54))
        for x in (4,6,8,10):
            cuboid((x,2.2,4.2),(x+.35,2.28,7.8),(107,58,35))
        for x in (4,7,10):cuboid((x,.85,10),(x+1.8,1.8,12),green)
    elif name == 'ratatouille':
        colors=((107,62,89),(180,55,33),(95,126,51),(218,160,57))
        for row,z in enumerate((3,6,9)):
            for i,x in enumerate((3,5.5,8,10.5)):
                color=colors[(i+row)%4]
                cuboid((x,.85,z),(x+2,2.1,z+2.5),color)
                cuboid((x+.4,2.1,z+.4),(x+1.6,2.25,z+2.1),tuple(min(255,c+22) for c in color))
    elif name == 'pasta_with_mutton_chop':
        noodle=(222,173,85)
        for i in range(8):
            x=3+i*1.2
            cuboid((x,.85,3+(i%2)),(x+.7,1.5,12-(i%3)),noodle)
        for i in range(5):cuboid((3,1.5,4+i*1.4),(12,1.8,4.5+i*1.4),noodle)
        cuboid((6,1.8,6),(10,3.0,10),brown)
        cuboid((8,2.2,10),(9,2.7,12),cream)
    else:
        # Rice has separate grains and mushroom caps.
        cuboid((3,.85,3),(13,1.6,13),(207,182,128))
        for z in range(4,12,2):
            for x in range(4,12,2):cuboid((x,1.6,z),(x+1.5,1.95+(x+z)%3*.15,z+.8),cream)
        if name == 'mushroom_rice':
            for x,z in ((4,4),(9,5),(6,9)):
                cuboid((x+.5,1.9,z+.5),(x+1.2,2.5,z+1.2),cream)
                cuboid((x,2.5,z),(x+2,3.0,z+2),brown)
    return out
