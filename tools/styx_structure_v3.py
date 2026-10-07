"""Owner-reference reconstruction. Unseen interiors and garden are original adaptations.
South-facing fronts: brick tower center (13,20); timber greenhouse (42..62,10..40).
"""
WIDTH,HEIGHT,DEPTH=67,63,49

def build():
    cells={}
    def put(x,y,z,b):
        assert 0<=x<WIDTH and 0<=y<HEIGHT and 0<=z<DEPTH,(x,y,z,b)
        cells[x,y,z]=b if ':' in b else 'minecraft:'+b
    def fill(x0,y0,z0,x1,y1,z1,b):
        for x in range(x0,x1+1):
            for y in range(y0,y1+1):
                for z in range(z0,z1+1):put(x,y,z,b)
    def octagon(x,z,cx=13,cz=20,r=7):
        dx,dz=abs(x-cx),abs(z-cz)
        return max(dx,dz)<=r and dx+dz<=2*r-2
    def edge(shape,x,z):return shape(x,z) and any(not shape(x+dx,z+dz) for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)))
    def disk(x,z,cx,cz,r):return (x-cx)**2+(z-cz)**2<=r*r
    def lamp(x,y,z,top):
        put(x,y,z,'lantern[hanging=true]')
        if top>y:fill(x,y+1,z,x,top,z,'iron_chain[axis=y]')
    def flower(x,y,z,name):
        tall=name=='ravenquill_lupine'
        put(x,y,z,'jugcraft:'+name+('[half=lower]' if tall else '[age=2]'))
        if tall:put(x,y+1,z,'jugcraft:'+name+'[half=upper]')

    # A shared garden connects two distinct buildings without changing their silhouettes.
    fill(0,0,0,66,0,48,'grass_block')
    for x in range(10,56):
        for z in range(43,46):put(x,0,z,'gravel' if (x+z)%4==0 else 'cobblestone')
    for a,b,z0,z1 in ((11,15,27,44),(50,54,40,44),(28,32,36,44)):fill(a,0,z0,b,0,z1,'cobblestone')
    for x in (3,23,39,65):
        for z in (5,43):
            put(x,1,z,'mossy_cobblestone_wall');put(x,2,z,'spruce_fence');put(x,3,z,'lantern')
    for x,z in ((4,10),(4,32),(24,9),(36,9),(38,34),(45,45),(59,45)):
        fill(x,1,z,x+2,1,z+1,'azalea_leaves[persistent=true]');put(x+1,2,z,'flowering_azalea_leaves[persistent=true]')

    # Tall red-brick shaft, gray stone foundation, white belt and dark projecting cornices.
    for x in range(5,22):
        for z in range(12,29):
            if octagon(x,z):
                for y in (0,8,16,24,32,40):put(x,y,z,'spruce_planks')
            if edge(octagon,x,z):
                for y in range(1,40):
                    b='stone_bricks' if y<8 else 'bricks'
                    if y<8 and (x+y+z)%7==0:b='andesite'
                    if y in (8,23):b='polished_deepslate'
                    if 24<=y<=27:b='light_gray_stained_glass'
                    put(x,y,z,b)
    for y,r,b in ((7,8,'polished_deepslate_slab[type=top]'),(8,8,'smooth_quartz_slab[type=bottom]'),
                   (21,8,'deepslate_bricks'),(22,9,'deepslate_tile_slab[type=top]'),(23,8,'polished_deepslate'),
                   (28,8,'waxed_cut_copper_slab[type=bottom]'),(40,8,'deepslate_bricks'),
                   (41,9,'deepslate_tile_slab[type=top]'),(42,8,'polished_deepslate')):
        shape=lambda a,c:octagon(a,c,r=r)
        for x in range(3,24):
            for z in range(10,31):
                if edge(shape,x,z):put(x,y,z,b)
    for x,z in ((7,14),(13,12),(19,14),(21,20),(19,26),(13,28),(7,26),(5,20)):
        put(x,20,z,'deepslate_brick_wall');put(x,39,z,'deepslate_brick_wall')
        for y in (21,40):put(x,y,z,'polished_deepslate')
    for side in ('south','west','north','east'):
        def face(t,y,d=0):
            return {'south':(13+t,y,27+d),'north':(13-t,y,13-d),'west':(6-d,y,20+t),'east':(20+d,y,20-t)}[side]
        for t in (-3,3):
            for y in list(range(2,7))+list(range(10,20)):put(*face(t,y),'gray_stained_glass' if y<8 else 'black_stained_glass')
            for y in range(29,39):put(*face(t,y,1),'brick_wall')
        for t in (-4,0,4):
            for y in range(24,28):put(*face(t,y),'polished_basalt[axis=y]')
        for dx in range(-2,3):
            for dy in range(-2,3):
                if abs(dx)+abs(dy)<=2:put(*face(dx,34+dy),'polished_andesite' if abs(dx)+abs(dy)==2 else 'light_gray_stained_glass')
        for t in (-5,5):
            fill(*face(t,14,1),*face(t,22,1),'waxed_oxidized_cut_copper')
            put(*face(t,13,1),'waxed_oxidized_cut_copper_slab[type=top]')
    # Green copper dome split around the instrument, with orange open shutters.
    radii=(8,8,7,7,6,5,4,2,0)
    for n,r in enumerate(radii):
        y=43+n;nr=radii[n+1] if n+1<len(radii) else -1
        shape=lambda a,b:disk(a,b,13,20,r)
        for x in range(5,22):
            for z in range(12,29):
                if not shape(x,z):continue
                if nr>=0 and disk(x,z,13,20,nr) and not edge(shape,x,z):continue
                if 11<=x<=15 and z>=20:continue
                put(x,y,z,'waxed_weathered_cut_copper' if (x+z)%7==0 else 'waxed_oxidized_cut_copper')
    for side in (-1,1):
        for y in range(44,52):
            x=13+side*(6 if y<49 else 5)
            for z in range(23,28 if y<49 else 27):put(x,y,z,'waxed_cut_copper')
            put(x,y,28 if y<49 else 27,'waxed_copper_trapdoor[facing=south,open=true]')
        put(13+side*5,52,24,'waxed_cut_copper_stairs[facing=north]');put(13+side*5,52,25,'waxed_cut_copper_slab[type=bottom]')
    # The large diagonal telescope is visible from the ground, as in the reference.
    fill(11,41,18,15,41,22,'polished_deepslate');fill(12,42,19,14,44,21,'polished_basalt[axis=y]')
    for x in (11,15):put(x,44,20,'waxed_cut_copper')
    for n in range(13):
        cy,cz=45+n,20+n
        for dx in range(-1,2):
            for dy in range(-1,2):put(13+dx,cy+dy,cz,'polished_blackstone' if n%4 else 'polished_deepslate')
        if n in (3,7,11):
            for dx,dy in ((-2,0),(2,0),(0,-2),(0,2)):put(13+dx,cy+dy,cz,'deepslate_bricks')
    for dx in range(-2,3):
        for dy in range(-2,3):
            if abs(dx)==2 and abs(dy)==2:continue
            rim=max(abs(dx),abs(dy))==2
            put(13+dx,58+dy,33,'smooth_stone' if rim else 'black_concrete')
            if not rim:put(13+dx,58+dy,34,'gray_stained_glass')
    put(13,44,18,'end_rod[facing=north]')

    # Brick side hall, green roof, external staircase and contrasting white turret.
    for x in range(20,37):
        for z in range(17,30):
            for y in (0,8):put(x,y,z,'spruce_planks')
            if x in (20,36) or z in (17,29):
                fill(x,1,z,x,7,z,'stone_bricks');fill(x,9,z,x,17,z,'bricks')
                put(x,7,z,'polished_deepslate');put(x,8,z,'smooth_quartz')
            roof=18+min(z-17,29-z);put(x,roof,z,'waxed_oxidized_cut_copper')
            if x in (20,36):
                fill(x,18,z,x,roof,z,'bricks');put(x,roof+1,z,'waxed_weathered_cut_copper_slab[type=bottom]')
    for x in (24,29,34):
        fill(x-1,11,29,x+1,14,29,'light_gray_stained_glass')
        put(x,15,30,'polished_deepslate');put(x-1,14,30,'brick_stairs[facing=east,half=top]');put(x+1,14,30,'brick_stairs[facing=west,half=top]')
    fill(20,1,18,20,4,22,'air');fill(20,9,18,20,12,22,'air');fill(29,9,29,31,12,29,'air')
    for n in range(9):
        fill(29,0,37-n,31,n,37-n,'stone_bricks');fill(29,n,37-n,31,n,37-n,'stone_brick_stairs[facing=north]')
        for x in (28,32):put(x,n+1,37-n,'stone_brick_wall')
    for y,r in ((13,1),(14,2),(15,3)):
        for x in range(30,37):
            for z in range(26,33):
                if octagon(x,z,33,29,r):put(x,y,z,'waxed_cut_copper' if y==14 else 'spruce_planks')
    turret=lambda a,b:octagon(a,b,33,29,3)
    for x in range(30,37):
        for z in range(26,33):
            if not turret(x,z):continue
            for y in (16,27):put(x,y,z,'spruce_planks')
            if edge(turret,x,z):fill(x,17,z,x,26,z,'birch_log' if x in (31,35) or z in (27,31) else 'calcite')
    fill(33,20,32,33,24,32,'black_stained_glass')
    for y,r in ((28,4),(29,3),(30,3),(31,2),(32,2),(33,1),(34,1)):
        for x in range(29,38):
            for z in range(25,34):
                if disk(x,z,33,29,r):put(x,y,z,'waxed_oxidized_cut_copper' if (x+z)%3 else 'waxed_weathered_cut_copper')
    fill(33,35,29,33,37,29,'iron_bars');put(33,38,29,'lightning_rod[facing=up]')

    # A rounded barrel profile, with white bands below its stepped timber ribs.
    arch=(19,19,19,18,18,17,16,15,14,12,10)
    for x in range(42,63):
        for z in range(10,41):
            put(x,0,z,'mossy_cobblestone' if (x+2*z)%5==0 else 'cobblestone')
            if x in (42,62) or z in (10,40):put(x,1,z,'mossy_cobblestone');fill(x,2,z,x,9,z,'glass')
            dx=abs(x-52);roof=arch[dx];rib=z in (10,15,20,25,30,35,40)
            low=min(roof,arch[dx+1]+1) if dx<10 else roof
            fill(x,low,z,x,roof,z,'spruce_planks' if rib else 'white_stained_glass')
            if rib:put(x,roof-1,z,'smooth_quartz')
            if z in (10,40):fill(x,10,z,x,roof-2,z,'glass')
            if rib and dx>2:put(x,roof+1,z,'spruce_stairs[facing='+('east' if x<52 else 'west')+']')
            if dx<=2 and rib:put(x,roof+1,z,'spruce_slab[type=bottom]')
    for z in (10,15,20,25,30,35,40):
        for x in (42,62):
            fill(x,1,z,x,9,z,'oak_log');put(x,1,z,'mossy_cobblestone')
            outer=x-1 if x==42 else x+1
            fill(outer,1,z,outer,2,z,'mossy_cobblestone_wall');fill(outer,3,z,outer,5,z,'oak_leaves[persistent=true]')
            put(outer,8,z,'spruce_stairs[facing='+('east' if x==42 else 'west')+',half=top]');put(outer,9,z,'spruce_slab[type=top]')
    # The reference's text and red arrow are annotations, never part of the build.
    for z in (10,40):
        for dx in range(-5,6):
            for dy in range(-5,6):
                d=max(abs(dx),abs(dy))
                if abs(dx)+abs(dy)>8:continue
                outer=d<=4 and abs(dx)+abs(dy)<=6
                inner=d<=3 and abs(dx)+abs(dy)<=4
                put(52+dx,13+dy,z,'glass' if inner else 'spruce_planks' if outer else 'smooth_quartz')
        fill(49,1,z,49,4,z,'spruce_planks');fill(55,1,z,55,4,z,'spruce_planks');fill(49,5,z,55,5,z,'spruce_planks')
        fill(50,6,z,54,6,z,'spruce_planks');put(52,7,z,'spruce_slab[type=bottom]');fill(50,1,z,54,4,z,'air')
        put(50,4,z,'spruce_stairs[facing=east,half=top]');put(54,4,z,'spruce_stairs[facing=west,half=top]')
    from styx import FLOWERS
    for i,name in enumerate(FLOWERS):
        x=45 if i<4 else 57;z=(13,19,27,33)[i%4]
        fill(x,1,z,x+2,1,z+3,'dirt')
        for xx in range(x,x+3):
            put(xx,1,z-1,'spruce_trapdoor[facing=north,open=true]');put(xx,1,z+4,'spruce_trapdoor[facing=south,open=true]')
        for zz in range(z,z+4):
            put(x-1,1,zz,'spruce_trapdoor[facing=west,open=true]');put(x+3,1,zz,'spruce_trapdoor[facing=east,open=true]')
        for dx,dz in ((0,0),(2,0),(1,1),(0,3),(2,3)):flower(x+dx,2,z+dz,name)
    for z in range(11,40):
        for x in (51,52,53):put(x,0,z,'stone_bricks' if (x+z)%5 else 'mossy_stone_bricks')
    for z in (16,24,32):
        for x in (49,55):put(x,1,z,'water_cauldron[level=3]');lamp(x,7,z,17)
    for x in (44,60):
        for z in (17,25,33):
            put(x,6,z,'spruce_trapdoor[half=top]');put(x,7,z,'potted_fern');fill(x,8,z,x,13,z,'iron_chain[axis=y]')
    for x in (45,46,47,57,58,59):put(x,1,38,'barrel[facing=south]');put(x,2,38,'potted_fern' if x%2 else 'flower_pot')
    put(46,2,38,'brewing_stand');put(58,2,38,'potted_azalea_bush')
    # Furnished receiving study, library, sleeping room and upper instrument rooms.
    for floor in (0,8,16,24,32):
        for x in range(10,17):fill(x,floor+1,15,x,floor+3,15,'bookshelf')
        for x in (11,15):lamp(x,floor+5,18,floor+6)
        fill(9,floor+7,18,17,floor+7,18,'spruce_slab[type=top]')
        for x in range(11,16):
            for z in range(20,24):put(x,floor+1,z,'purple_carpet' if x in (11,15) or z in (20,23) else 'gray_carpet')
        put(17,floor+1,21,'cartography_table');put(17,floor+1,22,'barrel');put(17,floor+2,22,'lantern');put(16,floor+1,24,'lectern[facing=west]')
    put(15,17,17,'purple_bed[facing=east,part=foot]');put(16,17,17,'purple_bed[facing=east,part=head]')
    put(17,9,17,'enchanting_table');put(17,9,18,'amethyst_block');put(17,10,18,'purple_candle[candles=3,lit=true]')
    put(17,41,23,'lectern[facing=west]');put(17,41,25,'cartography_table');put(17,42,25,'lantern')
    # Adjacent alternating flights have clear landings and avoid a low switchback ceiling.
    for floor in (0,8,16,24,32):
        south=(floor//8)%2==0
        for n in range(8):
            z=16+n if south else 24-n
            for x in ((8,9) if south else (10,11)):
                put(x,floor+n,z,'spruce_stairs[facing='+('south' if south else 'north')+']')
                fill(x,floor+n+1,z,x,max(floor+8,floor+n+3),z,'air')
    fill(11,1,27,15,4,28,'air');fill(11,5,28,15,5,28,'stone_brick_slab[type=top]')
    for x in (10,16):put(x,3,28,'lantern')
    # Authored connection states keep railings coherent without broad neighbor updates.
    connectables={'mossy_cobblestone_wall','stone_brick_wall','deepslate_brick_wall','brick_wall','spruce_fence','iron_bars'}
    solid={'stone_bricks','mossy_cobblestone','bricks','polished_deepslate','deepslate_bricks','spruce_planks','oak_log'}
    for (x,y,z),block in list(cells.items()):
        kind=block.removeprefix('minecraft:')
        if kind not in connectables:continue
        props={}
        for name,(dx,dz) in {'north':(0,-1),'south':(0,1),'west':(-1,0),'east':(1,0)}.items():
            neighbor=cells.get((x+dx,y,z+dz),'minecraft:air').removeprefix('minecraft:').split('[')[0]
            joins=neighbor==kind or neighbor in solid
            props[name]=('low' if joins else 'none') if kind.endswith('_wall') else str(joins).lower()
        if kind.endswith('_wall'):props['up']='true'
        put(x,y,z,kind+'['+','.join(k+'='+v for k,v in props.items())+']')
    palette=sorted(set(cells.values()));indices={s:i for i,s in enumerate(palette)}
    return {'size':[WIDTH,HEIGHT,DEPTH],'palette':palette,'blocks':[[x,y,z,indices[b]] for (x,y,z),b in sorted(cells.items(),key=lambda e:(e[0][1],e[0][2],e[0][0]))]}
