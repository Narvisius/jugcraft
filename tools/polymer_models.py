"""Supported process bodies, continuous feed paths and visible guarded drives."""
from polymers import FORMS
from coastal_metals_models import base,vessel
from starter_chemistry_models import drawer,coupling,STEEL,OLIVE,CHROME,SILVER,RUBBER
from steampunk_models import box,cyl
from biorefinery_models import pipe_run

def monomer():
 m=vessel(4)
 m += [box((-26,3,40),(-17,33,54),OLIVE),box((-25,31,34),(-18,34,44),STEEL)]
 m+=cyl('z',-8,26,3,7,14,STEEL)
 return m
def kettle():
 m=base(3)
 m += [box((-27,3,12),(11,12,42),STEEL)]
 m+=cyl('y',-8,27,17,12,53,SILVER)
 for y in (13,34,50):m+=cyl('y',-8,27,17.5,y,y+2,STEEL)
 m.append(box((-14,53,20),(-2,60,34),OLIVE))
 m+=cyl('z',-8,26,3,7,14,STEEL)
 return m
def extruder():
 m=base(5)
 m.append(box((-28,3,12),(12,10,74),STEEL))
 # Heated screw barrel is bolted to two saddles; the hopper reaches its feed throat.
 for z in (15,32):m.append(box((-18,10,z),(2,18,z+5),OLIVE))
 m+=cyl('z',-8,24,9,13,43,OLIVE)
 for z in (16,26,36):m+=cyl('z',-8,24,9.5,z,z+2,SILVER)
 m.append(box((-16,30,17),(0,42,30),OLIVE))
 m.append(box((-25,22,20),(-14,28,26),STEEL)) # feed hatch joins the screw housing
 m+=cyl('z',-8,24,3,7,14,STEEL)
 # Die head seats in the cooling bed; pull rollers have bearings on both sides.
 m+=cyl('z',-8,24,6,43,48,STEEL)
 m.append(box((-20,10,44),(4,19,70),SILVER))
 for z in (51,62):
  for x in (-23,3):m.append(box((x,10,z-3),(x+4,29,z+3),OLIVE))
  m+=cyl('x',24,z,3,-19,3,RUBBER)
 m.append(box((-26,28,58),(-19,41,74),STEEL)) # offcut chute seated on frame
 m.append(box((-26,10,68),(-19,28,74),OLIVE))
 return m
def reclaimer():
 m=base(3)
 m += [box((-27,3,12),(11,12,43),STEEL),box((-25,12,13),(9,30,41),OLIVE),box((-22,30,16),(6,43,34),STEEL)]
 m+=cyl('z',-8,24,3,7,14,STEEL)
 return m
FACTORIES={'monomer_preparation_unit':monomer,'polymerization_kettle':kettle,'polymer_extruder':extruder,'polymer_reclaimer':reclaimer}
MODELS={n:factory() for n,factory in FACTORIES.items()}
PIPE_PATHS={}
for name,m in MODELS.items():
 f=FORMS[name];depth=len(f['layers'][0]);PIPE_PATHS[name]={}
 for _,kind,target,col,row,layer,side in f['ports']:
  x=8-16*col;y=8+16*layer
  if kind.startswith('FLUID'):
   # All service piping enters the sealed process body; endpoints verified in tests.
   points=[(x,y,16*depth-.5),(x,y,30)]
   points.append((0,y,30))
   if y==8:points.append((0,18,30))
   pipe_run(m,points);PIPE_PATHS[name][kind,target]=points
  elif kind.startswith('ITEM'):
   z=1 if side=='FRONT' else 16*depth-7
   m+=drawer(x-5,x+5,y-4,y+4,z,z+5)
   lo,hi=(6,25) if side=='FRONT' else (16*depth-23,16*depth-7)
   m.append(box((x-3,y-3,lo),(x+3,y+3,hi),STEEL))
   # Feed hatches and outlet throats are supported, including side-of-barrel intake.
   if side=='FRONT':m.append(box((x-4,10,18),(x+4,y+4,26),OLIVE))
ROTORS={n+'_drive':dict(block=n,axis='z',center=[-8,26 if n in ('monomer_preparation_unit','polymerization_kettle') else 24,8],property='lit',speed=4,ease=30,always=True,
 quads=coupling('z',7,9,-8,26 if n in ('monomer_preparation_unit','polymerization_kettle') else 24,3)) for n in MODELS}
ITEM_EXTRAS={}
