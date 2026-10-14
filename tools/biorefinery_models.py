"""Native industrial models built from the existing restrained steel/ceramic material kit."""
from starter_chemistry_models import skid,flange,cabinet,drawer,coupling,fan,STEEL,OLIVE,CHROME,SILVER,RIBS,RUBBER,GAUGE,LIGHT
from steampunk_models import box,cyl,dial
from biorefinery import FORMS

def kettle():
 m=skid(3,3)
 for x in (-20,4):m.append(box((x,3,15),(x+4,12,39),STEEL))
 m+=cyl('y',-6,28,15,10,35,SILVER)
 for y in (11,32):m+=cyl('y',-6,28,15.5,y,y+2,STEEL)
 m+=cyl('y',-6,28,13,35,38,OLIVE)
 m+=[box((-28,20,3),(-15,34,15),OLIVE),box((-30,34,2),(-13,36,16),CHROME),box((-26,36,5),(-17,44,13),STEEL)]
 m+=cyl('z',-23,26,4,1,3,STEEL)
 m+=cyl('z',-23,26,4,13,16,STEEL)
 m+=cyl('x',26,20,3,-24,-6,STEEL)
 m+=cabinet(3,4,2,11,16,10)
 m+=cyl('y',-6,28,4,38,40,STEEL)
 m+=cyl('y',-6,28,4,44,46,STEEL)
 m+=[box((-10,44,24),(-2,47,32),OLIVE),box((-13,37,24),(-11,47,32),STEEL),box((-13,46,24),(-2,47,32),STEEL)]
 for x in (-27,-18):m.append(box((x,3,5),(x+2,20,14),STEEL))
 m+=cyl('z',-23,26,3,14,20,STEEL)
 return m

def fermenter():
 m=skid(3,3)
 for x in (-21,1):
  for z in (18,34):m.append(box((x,3,z),(x+4,24,z+4),STEEL))
 for rad,y in ((7,8),(11,12),(15,16)):m+=cyl('y',-8,28,rad,y,y+4,CHROME)
 m+=cyl('y',-8,28,17,20,51,SILVER)
 for y in (20,35,49):m+=cyl('y',-8,28,17.4,y,y+1.5,STEEL)
 for rad,y in ((16,51),(12,54),(7,57)):m+=cyl('y',-8,28,rad,y,y+3,OLIVE)
 m+=cyl('y',-8,28,3,60,63,STEEL)
 m+=cyl('z',-8,36,6,9,12,CHROME)
 m+=cyl('z',-8,36,4.5,8,9,STEEL)
 m.append(box((8.2,24,20),(9,46,23),LIGHT))
 m+=cabinet(3,4,2,11,15,10)
 m.append(box((-30,15,25),(-25,43,38),RIBS))
 m+=drawer(-30,-20,5,12,3,16)
 m.append(box((-30,3,3),(-20,5,16),STEEL))
 for y in (21,39):m.append(box((-30,y,27),(-19,y+2,35),STEEL))
 return m

def distiller():
 m=skid(4,4)
 # Offset column leaves the condenser and low reboiler visible from the aisle.
 m+=cyl('y',-29,40,13,5,83,SILVER)
 for y in (7,22,39,56,73,82):m+=cyl('y',-29,40,13.5,y,y+2,CHROME)
 m+=cyl('y',-29,40,10,84,89,STEEL)
 m+=cyl('y',-3, 40,6,24,77,OLIVE)
 for y in (25,47, 72):m+=cyl('y',-3,40,6.5,y,y+2,CHROME)
 m+=cyl('x',80,40,2,-29,-3,STEEL);m+=cyl('y',-3,40,2,77,80,STEEL)
 m+=cyl('z',-25,17,10,5,25,OLIVE)
 m+=cyl('z',-25,17,10.5,4,6,CHROME)
 m+=cabinet(2,4,3,12,28,17)
 m+=cyl('z',-2,20,4,18,23,STEEL)
 m+=cyl('z',-2,20,3,25,28,STEEL)
 m.append(box((-7,3,16),(3,16,29),STEEL))
 m.append(box((-41,3,28),(-17,5,52),STEEL))
 for z in (8,20):m.append(box((-31,3,z),(-19,9,z+3),STEEL))
 for y in (30,65):m.append(box((-19,y,39),(-6,y+2,41),STEEL))
 m+=cyl('z',-2,20,2,28,40,STEEL)
 m+=cyl('y',-2,40,2,20,26,STEEL)
 m+=cyl('x',20,18,2,-17,-2,STEEL)
 for x in (-46,-39):m.append(box((x,3,15),(x+1, 82,16),STEEL))
 for y in range(8,82,5):m.append(box((-46,y,14),(-38,y+1,15),CHROME))
 m.append(box((-47,70,19),(-39,72,57),RIBS))
 for z in (20, 54):m.append(box((-46,72,z),(-45,81,z+1),STEEL))
 m.append(box((-46,80,20),(-45,81,55),CHROME))
 return m

def bauxite():
 m=skid(3,4)
 for z in (15, 44):m.append(box((-26,3,z),(6,12,z+7),STEEL))
 m+=cyl('z',-9,27,16,14, 54,SILVER)
 for z in (14,32,52):m+=cyl('z',-9,27,16.5,z,z+2,STEEL)
 m+=cyl('z',-9,27,13,10,14,CHROME)
 m+=cyl('z',-9,27,10,9,10,OLIVE)
 for x in (-21,1):
  for y in (19, 34):m.append(box((x,y,8),(x+3,y+3,11),STEEL))
 m+=cabinet(3,4,2,11,15,8)
 # Closed maintenance cap, seated on the pressure vessel; no decorative open pipe.
 m+=cyl('y',-9,36,5,41,45,OLIVE)
 m.append(box((-12,45,35),(-6,47,37),STEEL))
 m+=cyl('z',-25,8,3,32,36,RUBBER)
 m+=cyl('z',-25,8,3,44,52,RUBBER)
 m.append(box((-29,3,32),(-21,5,52),STEEL))
 return m

def press():
 m=skid(3,4)
 for x in (-26,3):m.append(box((x,3,6),(x+5,14,57),STEEL))
 for z in (8, 52):m.append(box((-26,14,z),(8,40,z+4),OLIVE))
 for x in (-23,4):
  m+=cyl('z',x,34,1.7,8,56,CHROME)
 for z in range(17,49,4):
  m+=[box((-23,17,z),(7,37,z+2),CHROME),box((-20,19,z-.2),(4,35,z),SILVER)]
 m+=cyl('z',-8,28,4,1,2,STEEL)
 m+=cyl('z',-8,28,4,6,8,STEEL)
 m+=drawer(-26,-13,5,12,44, 60)
 m+=drawer(-10,2,5,12,44,60)
 m+=cabinet(3,4,2,11,14,9)
 m.append(box((-20,12,16),(4,15,50),STEEL))
 return m

def calciner():
 m=skid(3,4)
 for z in (13,45):m.append(box((-25,3,z),(5,14,z+8),STEEL))
 m+=cyl('z',-9,26,16,12,55,SILVER)
 for z in (12,29,51):m+=cyl('z',-9,26,16.5,z,z+2,STEEL)
 m+=cyl('z',-9,26,12,9,12,STEEL)
 m+=cyl('z',-9,26,9,8,9,OLIVE)
 m.append(box((-13,22,7.8),(-5,25,8),LIGHT))
 m+=cyl('z',-9,26,2,7,9,STEEL)
 m+=drawer(-28,-17,4,10,47,60)
 m+=cabinet(3,4,2,11,14,9)
 m.append(box((-22,42,20),(3,45,50),RIBS))
 return m

def cell():
 m=skid(3,3)
 m+=[box((-29,5,12),(13,25, 44),SILVER),box((-26,25,14),(10,28,42),STEEL)]
 for x in (-27,8):
  m.append(box((x,26,15),(x+3,53,43),SILVER))
  m.append(box((x-1,53,14),(x+4,56,44),'sp_copper'))
 for z in (19,29,39):
  m.append(box((-26,51,z),(10,54,z+3),'copper_busbar'))
  m.append(box((-12,28,z-1),(-4,51,z+4),STEEL))
 m+=drawer(-29,-18,4,13,31,47)
 m+=cabinet(3,4,2,11,15,8)
 m.append(box((-21,14,11.5),(4,17,12),LIGHT))
 m+=cyl('z',-22,34,5,7,10,STEEL)
 m.append(box((-26,29,9),(-18,39,18),STEEL))
 m.append(box((-28,3,13),(12,5,43),STEEL))
 return m

MODEL_FACTORIES={'mashing_station':kettle,'industrial_fermenter':fermenter,'chemical_distillation_station':distiller,'bauxite_digester':bauxite,'aluminate_filter_press':press,'alumina_calciner':calciner,'aluminum_reduction_cell':cell}
MODELS={name:factory() for name,factory in MODEL_FACTORIES.items()}
# Every route starts at a working exposed port and terminates inside a real process component.
# Interior endpoints are submerged in the authored vessel/tray/housing, never free-standing tube ends.
PIPE_PATHS={
 'mashing_station':{('FLUID_IN',0):[(8,8,38),(0,8,38),(0,20,38)],('FLUID_OUT',0):[(-8,8,36),(-8,14,36)]},
 'industrial_fermenter':{('FLUID_IN',0):[(-24,8,36),(-8,8,36),(-8,14,36)],('FLUID_OUT',0):[(8,8,34),(0,8,34),(0,20,34)],('FLUID_OUT',1):[(-8,56,36)]},
 'chemical_distillation_station':{('FLUID_IN',0):[(-24,24,50)],('FLUID_OUT',0):[(8,24,40),(2,24,40)],('FLUID_OUT',1):[(-24,8,43)]},
 'bauxite_digester':{('FLUID_IN',0):[(8,24,49),(4,24,49)],('FLUID_IN',1):[(-8,24,53)],('FLUID_OUT',0):[(-24,8,44),(-9,8,44),(-9,14,44)]},
 'aluminate_filter_press':{('FLUID_IN',0):[(-8,24,54)],('FLUID_OUT',0):[(8,8,44),(2,8,44),(2,13,44)],('FLUID_OUT',1):[(-8,8,44),(-8,13,44)]},
 'alumina_calciner':{('FLUID_IN',0):[(-8,8,53),(-8,14,53)],('FLUID_OUT',0):[(8,24,51),(4,24,51)]},
 'aluminum_reduction_cell':{('FLUID_OUT',0):[(8,24,42)]},
}
def pipe_run(parts,points):
 for a,b in zip(points,points[1:]):
  changed=[i for i in range(3) if a[i]!=b[i]]
  assert len(changed)==1,('Pipe segment must be axis-aligned',a,b)
  axis=changed[0];others=[i for i in range(3) if i!=axis]
  parts+=cyl('xyz'[axis],a[others[0]],a[others[1]],2,min(a[axis],b[axis]),max(a[axis],b[axis]),STEEL)
 for x,y,z in points[1:-1]:parts.append(box((x-2.1,y-2.1,z-2.1),(x+2.1,y+2.1,z+2.1),STEEL))
for name,parts in MODELS.items():
 f=FORMS[name];d=len(f['layers'][0])
 for _,kind,target,col,row,layer,side in f['ports']:
  x=8-16*col;y=8+16*layer
  if kind.startswith('FLUID'):
   parts+=flange('z',x,y,16*d-3,16*d-.5,2)
   pipe_run(parts,[(x,y,16*d-.5)]+PIPE_PATHS[name][(kind,target)])
  elif kind.startswith('ITEM'):
   z=1 if side=='FRONT' else 16*d-7
   parts+=drawer(x-5,x+5,y-4,y+4,z,z+5)
   lo,hi=(6,18) if side=='FRONT' else (16*d-18,16*d-7)
   parts.append(box((x-3,y-3,lo),(x+3,y+3,hi),STEEL))
# Slow enclosed drives, guarded pump shafts, and cooling fans; no oscillating whole structures.
_specs={
 'mashing_station':('y',[-6,42,28],coupling('y',40,44,-6,28,3),4),
 'industrial_fermenter':('y',[-8,61,28],fan('y',60,63,-8,28,5),1.5),
 'chemical_distillation_station':('z',[-2,20,24],coupling('z',23,25,-2,20,3),8),
 'bauxite_digester':('z',[-25,8,41],coupling('z',36,44,-25,8,3),4),
 'aluminate_filter_press':('z',[-8,28,4],coupling('z',2,6,-8,28,3),3),
 'alumina_calciner':('z',[-9,26,7],fan('z',6,8,-9,26,7),2),
 'aluminum_reduction_cell':('z',[-22,34,7],fan('z',5,7,-22,34,4),5),
}
ROTORS={name+'_drive':dict(block=name,axis=axis,center=center,property='lit',speed=speed,ease=30,always=True,quads=quads) for name,(axis,center,quads,speed) in _specs.items()}
ITEM_EXTRAS={}
