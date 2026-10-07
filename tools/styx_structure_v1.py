"""Original 27 x 21 x 28 tower-greenhouse, stored as a palette plus ordered placements.
Only explicit placements are written; air above the floor must be clear before the operator builds.
"""
def build():
    cells={}
    def put(x,y,z,b): cells[x,y,z]=b if ':' in b else 'minecraft:'+b
    def fill(x0,y0,z0,x1,y1,z1,b):
        for x in range(x0,x1+1):
            for y in range(y0,y1+1):
                for z in range(z0,z1+1):put(x,y,z,b)
    # Stone garden footing and a warm timber interior.
    fill(0,0,0,26,0,20,'mossy_stone_bricks')
    fill(1,0,4,11,0,14,'dark_oak_planks')
    fill(12,0,5,25,0,17,'polished_andesite')
    for x in range(27):
        for z in (0,20):
            if not (4<=x<=7 and z==20):put(x,1,z,'stone_brick_wall')
    for z in range(1,20):
        for x in (0,26):put(x,1,z,'stone_brick_wall')
    # Octagonal-looking tower: cut the four corners of an eleven-block square.
    for y in range(1,20):
        for x in range(1,12):
            for z in range(4,15):
                corner=(x in (1,11) and z in (4,14))
                edge=x in (1,11) or z in (4,14)
                if edge and not corner:
                    put(x,y,z,'deepslate_bricks' if y%7 else 'polished_blackstone_bricks')
        for x,z in ((2,5),(10,5),(2,13),(10,13)):
            put(x,y,z,'stripped_dark_oak_log')
    for floor in (7,14):
        fill(2,floor,5,10,floor,13,'dark_oak_planks')
    # Two roomy stair flights, aligned with the library/observatory landings.
    for n in range(7):
        for x in (3,4):
            put(x,n,5+n,'dark_oak_stairs[facing=south]')
            put(x,7,5+n,'air')
        for x in (6,7):
            put(x,7+n,12-n,'dark_oak_stairs[facing=north]')
            put(x,14,12-n,'air')
    # Doorway faces south, with a porch; arch connects greenhouse to ground floor.
    fill(5,1,14,6,3,14,'air')
    fill(4,0,15,7,0,19,'dark_oak_planks')
    for x in (4,7):fill(x,1,17,x,3,17,'dark_oak_fence')
    fill(4,4,14,7,4,17,'dark_oak_slab[type=bottom]')
    fill(11,1,9,12,3,11,'air')
    for y in (3,10,17):
        for x in (5,6,7):
            for yy in (y,y+1):put(x,yy,4,'purple_stained_glass')
        for z in (8,9,10):
            for yy in (y,y+1):put(1,yy,z,'purple_stained_glass')
    # The public-facing facade needs windows too, including above the greenhouse.
    for y in (3,10,17):
        for yy in (y,y+1):
            for x in (8,9):put(x,yy,14,'purple_stained_glass')
            for z in (7,8):put(11,yy,z,'purple_stained_glass')
    for y in range(2,7):put(9,y,15,'vine[north=true]')
    # Tapered dark roof with violet ribs and a copper finial.
    for n in range(6):
        lo,hi=1+n,11-n
        if lo>hi:break
        for x in range(lo,hi+1):
            for z in range(4+n,15-n):
                if x in (lo,hi) or z in (4+n,14-n):
                    put(x,20+n,z,'purple_terracotta' if x==6 or z==9 else 'deepslate_tiles')
    for x in range(0,13):
        put(x,20,3,'deepslate_tile_stairs[facing=south,half=top]')
        put(x,20,15,'deepslate_tile_stairs[facing=north,half=top]')
    for z in range(4,15):
        put(0,20,z,'deepslate_tile_stairs[facing=east,half=top]')
        put(12,20,z,'deepslate_tile_stairs[facing=west,half=top]')
    put(6,26,9,'lightning_rod[facing=up]')
    # Greenhouse frame and a pitched glass roof.
    for x in range(12,26):
        for z in (5,17):
            for y in range(1,5):put(x,y,z,'glass' if y>1 else 'stone_bricks')
    for z in range(5,18):
        for y in range(1,5):put(25,y,z,'glass' if y>1 else 'stone_bricks')
    for x in (12,16,20,25):
        for z in (5,17):fill(x,1,z,x,4,z,'stripped_dark_oak_log')
    for z in range(5,18):
        height=5+min(z-5,17-z)//2
        for x in range(12,26):
            put(x,height,z,'purple_stained_glass' if x in (12,16,20,25) else 'glass')
    # Eight beds with all cultivars and clear central and cross aisles.
    # Frozen order: old saves persist an index into this layout's placement list.
    # Do not couple the legacy layout to future additions to the flower registry.
    FLOWERS=('stolas_starflower','witchglass_orchid','ravenquill_lupine','amethyst_mourningbell',
             'eclipse_camellia','astral_verbena','inkvein_helleborine','violet_lanternbloom')
    TALL='ravenquill_lupine'
    for i,name in enumerate(FLOWERS):
        x=14+(i%4)*3;z=7 if i<4 else 14
        fill(x,1,z,x+1,1,z+1,'dirt')
        for xx in (x,x+1):
            put(xx,1,z-1,'dark_oak_trapdoor[facing=north,open=true]')
            put(xx,1,z+2,'dark_oak_trapdoor[facing=south,open=true]')
        for xx,zz in ((x,z),(x+1,z+1)):
            put(xx,2,zz,'jugcraft:'+name+('[half=lower]' if name==TALL else '[age=2]'))
            if name==TALL:put(xx,3,zz,'jugcraft:'+name+'[half=upper]')
    # Workbench, water trough, bench, library, shrine and observatory furnishings.
    fill(13,1,10,13,1,12,'spruce_slab[type=top]')
    put(13,2,10,'potted_fern');put(13,2,12,'potted_dead_bush')
    fill(23,1,10,24,1,12,'copper_grate');put(23,2,11,'cauldron')
    for y in (1,2,3,8,9,10):fill(8,y,5,9,y,5,'bookshelf')
    put(9,1,11,'crafting_table');put(9,1,12,'barrel')
    put(8,8,11,'lectern[facing=west]');put(9,8,11,'amethyst_block')
    put(9,9,11,'purple_candle[candles=3,lit=true]')
    put(8,8,13,'purple_bed[facing=west,part=foot]');put(7,8,13,'purple_bed[facing=west,part=head]')
    put(9,8,8,'white_carpet');put(9,8,9,'purple_carpet')
    fill(8,15,6,9,15,6,'dark_oak_slab[type=top]')
    put(8,16,6,'amethyst_cluster[facing=up]')
    put(9,16,6,'copper_bulb[lit=true]')
    for x,y,z in [(6,3,13),(8,5,9),(9,12,9),(9,18,10),(16,4,11),(20,4,11),(24,4,11)]:
        put(x,y,z,'lantern[hanging=true]')
    # Small flower garden and outdoor seat.
    fill(14,0,19,23,0,19,'grass_block')
    for x in (14,17,20,23):put(x,1,19,'allium')
    for z in (1,2):put(10,1,z,'spruce_stairs[facing=east]')
    for x in (2,10,25):
        put(x,2,1,'lantern');put(x,1,1,'mossy_stone_bricks')
    palette=sorted(set(cells.values()))
    indices={s:i for i,s in enumerate(palette)}
    return {'size':[27,28,21],'palette':palette,'blocks':[[x,y,z,indices[b]] for (x,y,z),b in sorted(cells.items(),key=lambda e:(e[0][1],e[0][2],e[0][0]))]}
