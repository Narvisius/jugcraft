"""Bolted, supported chemistry vessels; every service pipe reaches a process body."""
from fluorochemistry import FORMS
from coastal_metals_models import base
from starter_chemistry_models import drawer,coupling,STEEL,OLIVE,CHROME,SILVER,LIGHT
from steampunk_models import box,cyl
from biorefinery_models import pipe_run

def concentrator():
 m=base(3)+[box((-27,3,12),(11,12,43),STEEL)]
 m+=cyl('y',-11,27,14,12,54,SILVER)
 for y in (13,35,50):m+=cyl('y',-11,27,14.5,y,y+2,STEEL)
 m+=[box((1,12,20),(9,48,36),OLIVE),box((-15,54,23),(-7,60,31),STEEL)]
 for y in (22,34,44):pipe_run(m,[(-1,y,27),(5,y,27)])
 return m
def dissolution():
 m=base(4)+[box((-28,3,12),(12,12,57),STEEL)]
 m+=cyl('z',-8,26,15,16,55,SILVER)
 for z in (16,34,52):m+=cyl('z',-8,26,15.5,z,z+2,STEEL)
 m += [box((-27,12,18),(-18,38,31),OLIVE),box((-15,38,24),(-1,44,39),OLIVE)]
 return m
def reactor(depth=4,expanded=False):
 top=68 if expanded else 53
 m=base(depth)+[box((-27,3,13),(11,12,depth*16-6),STEEL)]
 m+=cyl('y',-8,32,16,12,top,SILVER)
 for y in (13,35,top-4):m+=cyl('y',-8,32,16.5,y,y+2,STEEL)
 m+=[box((-13,top,27),(-3,top+7,37),OLIVE),box((-22,12,47),(6,30,depth*16-6),OLIVE)]
 if expanded:
  for y in (18,32,46,60):m.append(box((-25,y,23),(-23,y+2,41),SILVER))
  m.append(box((-22,30,52),(6,45,70),SILVER))
 return m
def cracker():
 m=base(4)+[box((-28,3,12),(12,12,57),STEEL),box((-26,12,16),(10,40,52),SILVER)]
 for z in (16,28,40,50):m.append(box((-27,13,z),(11,41,z+2),STEEL))
 m.append(box((-21,16,13),(5,35,16),OLIVE))
 m.append(box((-18,20,12.8),(2,23,13),LIGHT))
 return m
def oven():
 m=base(4)+[box((-28,3,12),(12,9,58),STEEL),box((-27,9,13),(11,39,56),SILVER)]
 for z in (14,53):m.append(box((-28,10,z),(12,40,z+2),STEEL))
 m.append(box((-18,16,11), (5,34,13),OLIVE))
 m.append(box((-15,21,10.8),(2,24,11),LIGHT))
 m.append(box((-26,39,24),(10,45,44),OLIVE))
 return m
FACTORIES=dict(acid_concentrator=concentrator,chemical_dissolution_chamber=dissolution,fluorocarbon_reactor=reactor,fluoromonomer_cracker=cracker,ptfe_sintering_oven=oven,lined_fluorochemical_reactor=lambda:reactor(5,True))
MODELS={n:f() for n,f in FACTORIES.items()}
PIPE_PATHS={}
for name,m in MODELS.items():
 f=FORMS[name];depth=len(f['layers'][0]);PIPE_PATHS[name]={}
 # Motor shaft reaches the vessel/front frame; the visible rotating coupling seats on it.
 m+=cyl('z',-8,26,2.5,7,20,STEEL)
 for _,kind,target,col,row,layer,side in f['ports']:
  x=8-16*col;y=8+16*layer
  if kind.startswith('FLUID'):
   points=[(x,y,16*depth-.5),(x,y,32)]
   if x != -8:points.append((-8,y,32))
   if y==8:points.append((-8,18,32))
   pipe_run(m,points);PIPE_PATHS[name][kind,target]=points
  elif kind.startswith('ITEM'):
   z=1 if side=='FRONT' else 16*depth-7
   m+=drawer(x-5,x+5,y-4,y+4,z,z+5)
   lo,hi=(6,26) if side=='FRONT' else (42,16*depth-7)
   m.append(box((x-3,y-3,lo),(x+3,y+3,hi),STEEL))
   m.append(box((x-4,9,18 if side=='FRONT' else 45),(x+4,y+3,26 if side=='FRONT' else 55),OLIVE))
ROTORS={n+'_drive':dict(block=n,axis='z',center=[-8,26,8],property='lit',speed=4,ease=30,always=True,quads=coupling('z',7,9,-8,26,3)) for n in MODELS}
ITEM_EXTRAS={}
