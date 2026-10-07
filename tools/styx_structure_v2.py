"""Nightglass Observatory, layout 2: 49 x 39 garden, 35 high. Original vanilla-block architecture.
The former layout lives in styx_structure_v1.py so unfinished builds and residents retain their old home.
"""
WIDTH,HEIGHT,DEPTH=49,35,39

def build():
    cells={}
    def put(x,y,z,b):
        assert 0<=x<WIDTH and 0<=y<HEIGHT and 0<=z<DEPTH,(x,y,z,b)
        cells[x,y,z]=b if ':' in b else 'minecraft:'+b
    def fill(x0,y0,z0,x1,y1,z1,b):
        for x in range(x0,x1+1):
            for y in range(y0,y1+1):
                for z in range(z0,z1+1):put(x,y,z,b)
    def octagon(x,z,r=9):
        dx,dz=abs(x-12),abs(z-18)
        return max(dx,dz)<=r and dx+dz<=r*2-3
    def circle(x,z,r):return (x-12)**2+(z-18)**2<=r*r
    def edge(test,x,z):return test(x,z) and any(not test(x+dx,z+dz) for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)))
    def flower(x,y,z,name):
        if name=='ravenquill_lupine':
            put(x,y,z,'jugcraft:'+name+'[half=lower]');put(x,y+1,z,'jugcraft:'+name+'[half=upper]')
        else:put(x,y,z,'jugcraft:'+name+'[age=2]')
    # A landscaped court rather than a single flat stone rectangle.
    fill(0,0,0,48,0,38,'grass_block')
    for x in range(49):
        for z in range(39):
            if circle(x,z,11) or 26<=x<=47 and 4<=z<=33:
                put(x,0,z,'stone_bricks' if (x+z)%5 else 'mossy_stone_bricks')
            if 10<=x<=14 and z>=27 or 35<=x<=39 and z>=32 or 23<=x<=27 and 16<=z<=20:
                put(x,0,z,'polished_andesite' if (x+z)%4 else 'chiseled_stone_bricks')
    for x in range(1,48):
        for z in (1,37):
            if z==37 and (10<=x<=14 or 35<=x<=39):continue
            put(x,1,z,'stone_brick_wall')
    for z in range(2,37):
        for x in (0,48):put(x,1,z,'stone_brick_wall')
    for x,z in ((2,2),(12,2),(24,2),(36,2),(46,2),(2,36),(8,36),(16,36),(26,36),(33,36),(41,36),(46,36)):
        put(x,1,z,'chiseled_stone_bricks');put(x,2,z,'stone_brick_wall');put(x,3,z,'lantern')
    # Octagonal observatory drum: stone foundations, warm panels, structural columns and two inhabited floors.
    for x in range(2,23):
        for z in range(8,29):
            if octagon(x,z):
                for y in (0,8,16):put(x,y,z,'dark_oak_planks')
            if edge(octagon,x,z):
                for y in range(1,16):
                    block='stone_bricks' if y<=2 else 'gray_terracotta'
                    if y in (7,8,15):block='polished_blackstone_bricks'
                    put(x,y,z,block)
                for y in (3,11):
                    if abs(x-12)>=6 and abs(z-18)>=6:
                        fill(x,y,z,x,y+3,z,'purple_stained_glass')
    for x,z in ((3,12),(6,9),(18,9),(21,12),(21,24),(18,27),(6,27),(3,24)):
        fill(x,1,z,x,15,z,'stripped_dark_oak_log')
        put(x,1,z,'polished_blackstone_bricks');put(x,2,z,'chiseled_stone_bricks')
        put(x,7,z,'waxed_exposed_cut_copper');put(x,15,z,'waxed_exposed_cut_copper')
    # Tall lancet windows, stone sills and shallow exterior buttresses.
    for floor in (0,8):
        for t in range(-2,3):
            for y in range(floor+3,floor+7):
                glass='purple_stained_glass' if abs(t)==2 else 'glass'
                for x,z in ((12+t,9),(12+t,27),(3,18+t),(21,18+t)):put(x,y,z,glass)
        for t in (-1,0,1):
            for x,z in ((12+t,9),(12+t,27),(3,18+t),(21,18+t)):put(x,floor+7,z,'purple_stained_glass')
        for t in range(-3,4):
            for x,z in ((12+t,8),(12+t,28),(2,18+t),(22,18+t)):put(x,floor+2,z,'stone_brick_slab[type=top]')
    for x,z in ((7,8),(17,8),(2,13),(2,23),(22,13),(22,23),(7,28),(17,28)):
        fill(x,1,z,x,5,z,'stone_bricks');put(x,6,z,'stone_brick_wall');put(x,7,z,'lantern')
    # Walkable viewing terrace, continuous floor and iron balustrade.
    for x in range(1,24):
        for z in range(7,30):
            if circle(x,z,11):
                put(x,16,z,'polished_blackstone_bricks' if not circle(x,z,9) else 'dark_oak_planks')
            if edge(lambda a,b:circle(a,b,11),x,z):
                put(x,17,z,'iron_bars')
                if x in (1,12,23) or z in (7,18,29):put(x,18,z,'lantern')
    # Glazed observation drum above the library, supporting the rotating-looking dome ring.
    for x in range(2,23):
        for z in range(8,29):
            if edge(lambda a,b:circle(a,b,9),x,z):
                for y in range(17,22):
                    put(x,y,z,'stripped_dark_oak_log' if x in (6,12,18) or z in (12,18,24) else 'glass')
                put(x,21,z,'waxed_exposed_cut_copper')
    # Terrace doors, with no doors or narrow corners to obstruct the resident.
    fill(11,17,27,13,19,27,'air');fill(21,17,17,21,19,19,'air')
    # Copper hemispherical dome with a north-facing open observation slit and dark shutter rails.
    radii=[10,9,9,8,7,6,4,2,0]
    for n,r in enumerate(radii):
        y=22+n;next_r=radii[n+1] if n+1<len(radii) else -1
        for x in range(2,23):
            for z in range(8,29):
                if not circle(x,z,r):continue
                if next_r>=0 and circle(x,z,next_r) and not edge(lambda a,b:circle(a,b,r),x,z):continue
                if 11<=x<=13 and z<=18 and y<30:continue
                material='waxed_oxidized_copper'
                if x in (10,14) and z<=18:material='polished_blackstone_bricks'
                elif x==12 or z==18 or abs(x-12)==abs(z-18):material='waxed_weathered_cut_copper'
                put(x,y,z,material)
    put(12,31,18,'waxed_exposed_cut_copper');put(12,32,18,'lightning_rod[facing=up]')
    # Lit southern portico, exterior benches and an east glass gallery joining the greenhouse.
    fill(10,1,27,14,4,28,'air')
    for x in (9,15):
        fill(x,1,31,x,4,31,'stripped_dark_oak_log');put(x,1,31,'stone_bricks')
        put(x,5,31,'dark_oak_stairs[facing=north,half=top]')
    fill(9,5,28,15,5,31,'dark_oak_slab[type=top]')
    fill(10,6,28,14,6,30,'waxed_weathered_cut_copper_slab[type=bottom]')
    put(12,4,30,'lantern[hanging=true]')
    fill(21,1,16,28,4,20,'air');fill(22,0,16,28,0,20,'polished_andesite')
    for x in range(22,29):
        for z in (15,21):
            put(x,1,z,'stone_bricks');fill(x,2,z,x,4,z,'glass')
        for z in range(15,22):put(x,5 if z in (15,21) else 6,z,'glass')
    for x in (23,27):
        for z in (15,21):fill(x,1,z,x,5,z,'stripped_dark_oak_log')
        put(x,5,18,'lantern[hanging=true]')
    # Victorian-style greenhouse: broad glazing, repeated timber bays, copper roof arches and a raised lantern roof.
    for x in range(28,47):
        for z in range(5,33):
            put(x,0,z,'polished_andesite' if (x+z)%3 else 'stone_bricks')
            if x in (28,46) or z in (5,32):
                put(x,1,z,'stone_bricks')
                for y in range(2,7):put(x,y,z,'glass')
    for x in (28,32,37,42,46):
        for z in (5,32):
            fill(x,1,z,x,6,z,'stripped_dark_oak_log');put(x,1,z,'chiseled_stone_bricks')
    for x in (28,46):
        for z in (5,11,18,25,32):
            fill(x,1,z,x,6,z,'stripped_dark_oak_log');put(x,1,z,'chiseled_stone_bricks')
    for x in range(28,47):
        roof=7+min(x-28,46-x)//2
        for z in range(5,33):
            clerestory=35<=x<=39 and 8<=z<=29
            if not clerestory:put(x,roof,z,'waxed_oxidized_cut_copper' if z in (5,11,18,25,32) else 'glass')
            if z in (5,32):
                for y in range(7,roof):put(x,y,z,'glass' if (x+y)%5 else 'purple_stained_glass')
    for z in range(8,30):
        for x in (35,39):
            for y in range(10,13):put(x,y,z,'waxed_oxidized_cut_copper' if z in (8,11,18,25,29) else 'glass')
        for x in range(35,40):put(x,13,z,'waxed_oxidized_cut_copper' if x in (35,39) or z in (8,11,18,25,29) else 'glass')
        put(37,14,z,'waxed_weathered_cut_copper_slab[type=bottom]')
    for z in (8,29):fill(36,11,z,38,12,z,'glass')
    for x in (28,46):
        for z in range(5,33):put(x,7,z,'waxed_oxidized_cut_copper_slab[type=top]')
    # Three entrances, arched door surrounds and a front rose-window pattern.
    fill(28,1,16,28,4,20,'air')
    fill(36,1,32,38,3,32,'air');fill(36,1,5,38,3,5,'air')
    for x in (35,39):put(x,4,32,'dark_oak_stairs[facing='+('east' if x==35 else 'west')+',half=top]')
    fill(36,5,32,38,5,32,'dark_oak_slab[type=top]')
    for x,z in ((35,34),(39,34)):put(x,1,z,'stone_brick_wall');put(x,2,z,'lantern')
    for dx,dy in ((0,0),(-1,0),(1,0),(0,-1),(0,1)):
        put(37+dx,8+dy,32,'purple_stained_glass' if dx or dy else 'amethyst_block')
    # Eight generous bordered beds. A continuous central aisle and transverse gallery remain clear.
    FLOWERS=('stolas_starflower','witchglass_orchid','ravenquill_lupine','amethyst_mourningbell',
             'eclipse_camellia','astral_verbena','inkvein_helleborine','violet_lanternbloom')
    for i,name in enumerate(FLOWERS):
        x=30 if i<4 else 42;z=(7,13,22,28)[i%4];length=3
        fill(x,1,z,x+2,1,z+length-1,'dirt')
        for xx in range(x,x+3):
            put(xx,1,z-1,'dark_oak_trapdoor[facing=north,open=true]')
            put(xx,1,z+length,'dark_oak_trapdoor[facing=south,open=true]')
        for zz in range(z,z+length):
            put(x-1,1,zz,'dark_oak_trapdoor[facing=west,open=true]')
            put(x+3,1,zz,'dark_oak_trapdoor[facing=east,open=true]')
        for xx,zz in ((x,z),(x+2,z),(x+1,z+1),(x,z+2),(x+2,z+2)):flower(xx,2,zz,name)
    for z in range(6,32):
        for x in (36,37,38):put(x,0,z,'calcite' if x!=37 else 'purple_terracotta')
    # Water station, potting counter, shelves, propagation pots, seed barrels and hanging baskets.
    for x in (35,36,37,38,39):put(x,1,18,'waxed_oxidized_cut_copper')
    for x in (36,37,38):put(x,2,18,'water_cauldron[level=3]')
    for z in (17,19):
        put(35,1,z,'waxed_oxidized_cut_copper_stairs[facing=east]');put(39,1,z,'waxed_oxidized_cut_copper_stairs[facing=west]')
    for z in range(22,28):put(29,1,z,'barrel[facing=east]')
    put(29,2,23,'brewing_stand');put(29,2,25,'potted_fern');put(29,2,27,'flower_pot')
    for x in (30,32,34,40,42,44):
        put(x,1,6,'spruce_slab[type=top]');put(x,2,6,'potted_azalea_bush' if x%4 else 'potted_fern')
    for x in (33,41):
        for z in (11,25):
            put(x,5,z,'dark_oak_trapdoor[half=top]');put(x,6,z,'potted_fern')
            fill(x,7,z,x,8,z,'iron_chain[axis=y]')
    for x in (34,40):
        for z in (8,14,23,29):
            put(x,6,z,'lantern[hanging=true]')
            fill(x,7,z,x,9,z,'iron_chain[axis=y]')
    # Ground-floor study, a lived-in library, private sleeping alcove and Stolas's mineral/herbal shrine.
    for y in (1,2,3,9,10,11):
        for x in range(9,17):put(x,y,11,'bookshelf')
    for x in (16,17,18):put(x,1,23,'spruce_slab[type=top]')
    put(17,2,23,'potted_fern');put(18,2,23,'lantern');put(16,1,22,'spruce_stairs[facing=south]')
    put(18,1,16,'cartography_table');put(18,1,17,'barrel');put(18,2,17,'flower_pot')
    put(16,9,22,'lectern[facing=west]');put(17,9,22,'cartography_table');put(18,9,22,'barrel')
    put(18,10,22,'lantern');put(16,9,23,'spruce_stairs[facing=north]')
    put(6,9,24,'purple_bed[facing=east,part=foot]');put(7,9,24,'purple_bed[facing=east,part=head]')
    put(6,9,23,'barrel');put(6,10,23,'lantern');put(7,9,25,'purple_carpet')
    put(16,9,14,'enchanting_table');put(17,9,14,'amethyst_block');put(18,9,14,'chiseled_bookshelf')
    put(17,10,14,'purple_candle[candles=3,lit=true]');put(18,10,14,'potted_fern')
    # Reading carpets, specimen cabinets, ceiling beams and properly suspended lamps.
    for floor in (0,8):
        for z in (13,23):
            fill(7,floor+7,z,17,floor+7,z,'dark_oak_slab[type=top]')
            for x in (10,14):
                put(x,floor+6,z,'iron_chain[axis=y]');put(x,floor+5,z,'lantern[hanging=true]')
        for x in range(10,15):
            for z in range(19,23):
                put(x,floor+1,z,'purple_carpet' if x in (10,14) or z in (19,22) else 'gray_carpet')
        for z in (14,16,18,20):
            put(19,floor+1,z,'barrel[facing=west]')
            put(19,floor+2,z,'potted_fern' if z%4 else 'amethyst_cluster[facing=up]')
    for x in (11,13,15):put(x,12,11,'potted_fern' if x==13 else 'purple_candle[candles=2,lit=true]')
    # Star-shaped brass floor inlay below the telescope's mount.
    for x in range(7,18):
        for z in range(13,24):
            if x==12 or z==18 or abs(x-12)==abs(z-18):put(x,16,z,'waxed_exposed_cut_copper')
    fill(11,17,17,13,17,19,'polished_blackstone_bricks')
    put(12,18,18,'chiseled_polished_blackstone');put(12,19,18,'waxed_exposed_cut_copper')
    for x in (11,13):put(x,20,18,'polished_blackstone_wall')
    # Large ascending telescope tube, copper collars, lens and an eyepiece facing the viewing platform.
    for n in range(7):
        y=20+n//2;z=18-n
        put(12,y,z,'polished_basalt[axis=z]')
        for x in (11,13):put(x,y,z,'waxed_exposed_cut_copper' if n in (0,3,6) else 'blackstone_slab[type=top]')
    put(12,23,11,'light_blue_stained_glass');put(12,20,19,'end_rod[facing=south]')
    put(15,17,24,'lectern[facing=west]');put(17,17,24,'cartography_table');put(18,17,24,'potted_fern')
    for x,z in ((5,18),(12,12),(19,18),(12,25)):
        put(x,20,z,'lantern[hanging=true]')
        for y in range(21,31):
            if cells.get((x,y,z),'minecraft:air')!='minecraft:air':break
            put(x,y,z,'iron_chain[axis=y]')
    # Two wide internal stair flights. Cutting the ceiling holes last keeps furnishings from blocking the stairs.
    for n in range(8):
        for x in (5,6):
            put(x,n,14+n,'dark_oak_stairs[facing=south]')
            fill(x,n+1,14+n,x,max(8,n+3),14+n,'air')
        for x in (8,9):
            put(x,8+n,23-n,'dark_oak_stairs[facing=north]')
            fill(x,9+n,23-n,x,max(16,11+n),23-n,'air')
    # Gardens, specimen pots, herb pergola, courtyard bench and a small moon-shaped pool.
    for x in (5,19,25,44):
        for z in (3,35):
            put(x,1,z,'azalea_leaves[persistent=true]')
            put(x+1,1,z,'flowering_azalea_leaves[persistent=true]')
    for x in (19,23):
        fill(x,1,34,x,4,34,'dark_oak_fence');fill(x,1,36,x,4,36,'dark_oak_fence')
    fill(19,5,34,23,5,36,'dark_oak_slab[type=bottom]')
    for x in (20,22):put(x,6,35,'flowering_azalea_leaves[persistent=true]')
    for x in (20,21,22):put(x,1,35,'spruce_stairs[facing=north]')
    put(21,4,35,'lantern[hanging=true]')
    for x in range(3,8):
        for z in range(31,35):
            put(x,0,z,'mossy_stone_bricks')
    for x,z in ((4,32),(5,32),(6,32),(4,33),(5,33)):
        put(x,1,z,'water_cauldron[level=3]')
    for x in (5,7,16,24,29,43):
        put(x,1,4,'potted_fern' if x%2 else 'potted_azalea_bush')
    # Blueprint placement deliberately avoids broad neighbor updates. Author the
    # connections explicitly so rails, walls and pergola fences are continuous.
    directions={'north':(0,-1),'south':(0,1),'west':(-1,0),'east':(1,0)}
    for (x,y,z),block in list(cells.items()):
        kind=block.removeprefix('minecraft:')
        if kind not in ('stone_brick_wall','iron_bars','dark_oak_fence'):continue
        connections={}
        for direction,(dx,dz) in directions.items():
            neighbor=cells.get((x+dx,y,z+dz),'minecraft:air').removeprefix('minecraft:').split('[')[0]
            joins=neighbor==kind or neighbor in ('stone_bricks','chiseled_stone_bricks','stripped_dark_oak_log','polished_blackstone_bricks','waxed_exposed_cut_copper')
            connections[direction]=('low' if joins else 'none') if kind=='stone_brick_wall' else str(joins).lower()
        if kind=='stone_brick_wall':connections['up']='true'
        put(x,y,z,kind+'['+','.join(k+'='+v for k,v in connections.items())+']')
    palette=sorted(set(cells.values()));indices={s:i for i,s in enumerate(palette)}
    return {'size':[WIDTH,HEIGHT,DEPTH],'palette':palette,'blocks':[[x,y,z,indices[b]] for (x,y,z),b in sorted(cells.items(),key=lambda e:(e[0][1],e[0][2],e[0][0]))]}
