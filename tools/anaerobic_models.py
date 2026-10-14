"""Sealed farm-industry equipment, in the shared native block/rotor model pipeline."""
from starter_chemistry_models import skid,flange,cabinet,drawer,coupling,fan,STEEL,OLIVE,CHROME,SILVER,RIBS,RUBBER,GAUGE,LIGHT
from steampunk_models import box,cyl,dial
import math

def rail(m,x0,x1,y,z):
    for x in range(int(x0),int(x1)+1,12):m.append(box((x,y,z),(x+1,y+9,z+1),STEEL))
    m.append(box((x0,y+8,z),(x1+1,y+9,z+1),CHROME))

def digester():
    m=skid(6,6)
    # Massive tank kept dominant: broad feet, quiet pale shell, stepped sealed dome.
    for x in (-65,-10):
        for z in (18,73):
            m += [box((x,3,z),(x+8,14,z+8),STEEL),box((x-1,3,z-1),(x+9,5,z+9),CHROME)]
    m += cyl('y',-33,50,34,12,68,SILVER)
    for y in (12,30,49,66):m += cyl('y',-33,50,35,y,y+2,STEEL)
    for radius,y in ((33,68),(29,72),(24,76),(17,80),(9,84)):
        m += cyl('y',-33,50,radius,y,y+4,OLIVE)
    # Vertical ribs stop the large wall from becoming one unbroken texture.
    for angle in range(0,360,45):
        a=math.radians(angle);x=-33+34*math.cos(a);z=50+34*math.sin(a)
        m.append(box((x-1,14,z-1),(x+1,66,z+1),STEEL))
    # Front gallery, weather roof, ladder and maintenance ring within the 96-pixel bounds.
    m += [box((-71,18,7),(5,21,16),RIBS),box((-71,45,7),(5,47,17),OLIVE)]
    for x in (-69,2):m.append(box((x,5,8),(x+2,45,10),STEEL))
    rail(m,-68,-3,21,7)
    for x in (-74,-66):m.append(box((x,4,31),(x+1,80,32),STEEL))
    for y in range(8,80,5):m.append(box((-74,y,30),(-65,y+1,31),CHROME))
    m += [box((-76,69,29),(-65,72,72),RIBS),box((-70,69,81),(0,72,88),RIBS)]
    rail(m,-67,-1,72,86)
    # Capped feed auger/gallery on the owner's left, with a guarded slowly turning drive.
    m += drawer(1,14,26,37,20,35)
    m += cyl('x',32,28,4,-8,8,STEEL)
    m += [box((1,37,20),(14,40,35),OLIVE),box((0,40,19),(15,42,36),CHROME)]
    for z in (20,35):m.append(box((12,26,z),(14,37,z+1),STEEL))
    # Rear gas header, water feed and digestate manifold at their declared collars.
    m += cyl('y',-60,72,2,66,90,STEEL)
    m += cyl('x',90,72,2,-72,-60,STEEL)
    m += cyl('y',-72,72,2,72,90,STEEL)
    m += flange('z',-72,72,72,95.5,2.5)
    m += flange('z',-72,8,64,95.5,2.5)
    m += cyl('y',-72,64,2.5,8,28,STEEL)
    m += cyl('x',28,64,2.5,-72,-60,STEEL)
    m += flange('z',8,8,66,95.5,3)
    m += cyl('y',8,66,3,8,22,STEEL)
    m += cyl('x',22,66,3,-6,8,STEEL)
    # Top mixer gearbox and its exposed guarded coupling, alongside the gas dome.
    m += [box((-42,86,45),(-24,90,56),STEEL),box((-44,90,48),(-38,95,56),OLIVE)]
    m += cyl('x',92,52,2,-25,-21,STEEL)
    m += cabinet(3,4,2,11,17,11)
    m += cyl('z',-33,38,8,14,18,CHROME)
    m += cyl('z',-33,38,6.5,13,14,STEEL)
    m.append(box((-37,34,12.75),(-29,37,13),LIGHT))
    return m

def cleaner():
    m=skid(2,3)
    for x in (-8,8):
        m += cyl('y',x,27,6,6,38,OLIVE if x<0 else SILVER)
        for y in (7,19,34):m+=cyl('y',x,27,6.5,y,y+1.5,STEEL)
        m+=cyl('y',x,27,5,38,42,STEEL)
    m+=flange('z',-8,40,27,47.5,2)
    m+=flange('z',8,8,27,47.5,2)
    m+=flange('z',-8,40,.5,27,2)
    m+=flange('z',-8,8,.5,27,2)
    m+=cyl('x',32,27,2,-8,8,CHROME)
    m+=cyl('y',8,14,1.5,14,32,CHROME)
    m+=cyl('z',8,32,1.5,14,27,CHROME)
    m+=drawer(-13,-3,12,20,3,17)
    m+=cabinet(4,4,2,10,14,9)
    # Small pump motor and two bearings, with moving coupling between them.
    m+=cyl('z',-8,26,4,15,21,STEEL)
    m+=cyl('z',-8,26,3,6,8,STEEL)
    for x in (-14,-3):m.append(box((x,21,5),(x+1,32,15),STEEL))
    m.append(dial('north',(8,27,20),4,.5,GAUGE,CHROME))
    return m

def separator():
    m=skid(2,3)
    for z in (11,37):m.append(box((-11,4,z),(11,11,z+4),STEEL))
    # Closed screw barrel, reinforcing rings and rear hopper. Drain trough and fiber drawer below.
    m+=cyl('z',0,22,9,12,39,OLIVE)
    for z in (12,22,34):m+=cyl('z',0,22,9.5,z,z+1.5,CHROME)
    m += [box((-6,31,29),(6,37,41),STEEL),box((-8,37,27),(8,40,43),OLIVE)]
    m+=drawer(-14,-2,5,12,1,23)
    m+=cabinet(3,4,2,11,12,8)
    m+=flange('z',8,24,34,47.5,2.5)
    m+=flange('z',-8,8,30,47.5,2)
    m+=cyl('y',-8,30,2,8,15,CHROME)
    m+=cyl('z',0,22,4,3,5,STEEL)
    for x in (-7,6):m.append(box((x,16,3),(x+1,29,12),STEEL))
    m.append(box((-6,28,3),(6,29,12),STEEL))
    m+=cyl('z',0,22,3,10,12,STEEL)
    m.append(box((-3,41,29),(3,45,39),STEEL))
    m.append(dial('north',(0,42,28),3,.5,GAUGE,CHROME))
    return m

def generator():
    m=skid(3,4)
    m += [box((-23,4,7),(-9,11,54),STEEL),box((-26,10,24),(-6,15,33),STEEL)]
    m+=cyl('z',-16,25,11,10,29,OLIVE)
    for z in (10,17,26):m+=cyl('z',-16,25,11.5,z,z+1.5,CHROME)
    m+=cyl('z',-16,25,10,38,57,RIBS)
    m+=cyl('z',-16,25,10.5,56,59,STEEL)
    m+=cyl('z',-16,25,4,3,5,CHROME)
    # Front flywheel guard and coupling cage; moving assemblies are renderer-owned.
    for x in (-29,-4):m.append(box((x,12,3),(x+1,38,5),STEEL))
    for y in (12,37):m.append(box((-28,y,3),(-3,y+1,5),STEEL))
    for x in (-23,-10):m.append(box((x,17,29),(x+1,33,38),STEEL))
    m.append(box((-22,32,29),(-9,33,38),STEEL))
    m+=cabinet(2,4,39,12,30,19)
    m.append(box((3,35,41),(13,44,57),RIBS))
    # Short folded, capped exhaust stays within the compact machine footprint.
    m+=cyl('y',-26,21,2,28,43,RUBBER)
    m+=cyl('z',-26,43,2,21,57,RUBBER)
    m+=cyl('z',-26,43,3,56,59,STEEL)
    m+=flange('x',8,24,-31.5,-15,2.5)
    m+=flange('z',-24,8,48,63.5,2.5)
    m+=cyl('y',-16,24,2,8,17,STEEL)
    m.append(dial('north',(-16,25,9.75),5,.5,GAUGE,CHROME))
    return m

MODELS={'anaerobic_digester':digester(),'biogas_cleaner':cleaner(),'digestate_separator':separator(),'bio_generator':generator()}
ROTORS={
 'digester_mixer':dict(block='anaerobic_digester',axis='x',center=[-31,92,52],property='lit',speed=1.5,ease=60,always=True,quads=coupling('x',-38,-25,92,52,2.5)),
 'digester_feed':dict(block='anaerobic_digester',axis='x',center=[13,32,28],property='lit',speed=3,ease=40,always=True,quads=coupling('x',11,15,32,28,3)),
 'biogas_cleaner_pump':dict(block='biogas_cleaner',axis='z',center=[-8,26,11],property='lit',speed=8,ease=25,always=True,quads=coupling('z',8,15,-8,26,3)),
 'digestate_screw_drive':dict(block='digestate_separator',axis='z',center=[0,22,7],property='lit',speed=4,ease=30,always=True,quads=coupling('z',5,10,0,22,4)),
 'bio_generator_train':dict(block='bio_generator',axis='z',center=[-16,25,32],property='lit',speed=9,ease=40,always=True,quads=fan('z',5,7,-16,25,10)+coupling('z',29,38,-16,25,5)),
}
ITEM_EXTRAS={
 'anaerobic_digester':cyl('x',92,52,2.5,-38,-25,CHROME,'dr_flange')+cyl('x',32,28,3,11,15,CHROME,'dr_flange'),
 'biogas_cleaner':cyl('z',-8,26,3,8,15,CHROME,'dr_flange'),
 'digestate_separator':cyl('z',0,22,4,5,10,CHROME,'dr_flange'),
 'bio_generator':cyl('z',-16,25,10,5,7,CHROME,'dr_fan')+cyl('z',-16,25,5,29,38,CHROME,'dr_flange'),
}
