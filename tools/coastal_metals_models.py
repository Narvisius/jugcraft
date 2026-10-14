"""Supported, connected native machinery using the existing industrial material kit."""
from starter_chemistry_models import skid,cabinet,drawer,coupling,fan,STEEL,OLIVE,CHROME,SILVER,RIBS,RUBBER,LIGHT
from steampunk_models import box,cyl
from coastal_metals import FORMS
from biorefinery_models import pipe_run

def base(depth):
 return skid(3,depth)+cabinet(3,4,2,11,15,8)

def vessel(depth,height=35):
 m=base(depth)
 m += [box((-27,3,13),(11,12,19),STEEL),box((-27,3,depth*16-18),(11,12,depth*16-12),STEEL)]
 m+=cyl('z',-8,26,16,14,depth*16-8,SILVER)
 for z in (14,depth*16-11):m+=cyl('z',-8,26,16.5,z,z+2,CHROME)
 m+=cyl('z',-8,26,13,11,14,OLIVE)
 m.append(box((-12,40,22),(-4,43,30),STEEL))
 return m

def intake():
 m=base(3)
 m.append(box((-28,3,13),(11,7,43),STEEL))
 m+=cyl('z',-8,17,11,14,40,OLIVE)
 for z in (14,38):m+=cyl('z',-8,17,11.5,z,z+2,CHROME)
 # Lower front-right intake mouth, continuously connected to the pump housing.
 m+=cyl('z',-24,8,4,0,19,STEEL)
 m+=cyl('x',8,18,3,-24,-8,STEEL)
 for x in (-27,-24,-21):m.append(box((x,4,0),(x+1,12,1),RIBS))
 m+=cyl('z',-8,17,3,9,14,STEEL)
 return m

def concentrator():
 m=base(3)
 m+=[box((-27,3,12),(11,7,41),STEEL),box((-25,7,13),(9,25,40),SILVER)]
 m+=cyl('y',-10,27,10,25,42,OLIVE)
 for y in (26,39):m+=cyl('y',-10,27,10.5,y,y+2,CHROME)
 m+=cyl('z',-8,17,3,9,14,STEEL)
 # Condenser rests on the sump; both its pipes return into the vessel.
 m.append(box((0,25,20),(8,40,36),STEEL))
 for y in (28,33,38):m.append(box((-3,y,23),(1,y+2,25),CHROME))
 return m

def conditioner():
 m=base(3)
 m.append(box((-28,3,14),(12,12,41),STEEL))
 for x in (-18,2):
  m+=cyl('y',x,27,10,12,36,SILVER)
  for y in (12,33):m+=cyl('y',x,27,10.5,y,y+2,CHROME)
 m.append(box((-27,36,21),(10,39,33),OLIVE))
 m+=cyl('z',-8,22,3,9,17,STEEL)
 m.append(box((-13,19,15),(-3,25,25),STEEL))
 return m

def dryer():
 m=base(3)
 m+=[box((-28,3,10),(12,8,42),STEEL),box((-27,8,11),(11,36,41),SILVER),box((-23,12,9),(7,32,12),OLIVE)]
 m.append(box((-18,16,8.8),(2,19,9),LIGHT))
 for y in (14,24,33):m.append(box((-27,y,12),(11,y+2,40),STEEL))
 m+=cyl('z',-8,26,2,7,11,STEEL)
 return m

def magnesium():
 m=base(3)
 m+=[box((-28,3,12),(12,8,41),STEEL),box((-27,8,13),(11,33,40),SILVER)]
 for x in (-23,2):
  m.append(box((x,31,18),(x+5,42,36),STEEL))
  m.append(box((x-1,42,17),(x+6,44,37),'sp_copper'))
 m.append(box((-23,39,24),(7,42,29),'copper_busbar'))
 m+=cyl('z',-8,24,2,7,14,STEEL)
 return m

def chlorinator():
 m=base(4)
 m+=[box((-27,3,16),(11,9,54),STEEL),box((-25,9,18),(9,15,52),OLIVE)]
 m+=cyl('y',-8,35,17,15,55,SILVER)
 for y in (16,34,52):m+=cyl('y',-8,35,17.5,y,y+2,STEEL)
 m+=cyl('y',-8,35,13,55,60,OLIVE)
 m+=cyl('z',-8,26,3,9,19,STEEL)
 m.append(box((-13,21,16),(-3,31,24),STEEL))
 return m

def retort():
 m=vessel(4)
 m+=cyl('z',-8,26,3,7,11,STEEL)
 # Vacuum cleanup pump sits on the skid and connects back into the sealed vessel.
 m.append(box((-28,3,38),(-20,12,55),STEEL))
 m+=cyl('y',-24,47,2,10,19,STEEL)
 # The upper salt drawer seats on the sealed cleanup manifold, not in empty air.
 m.append(box((-27,33,40),(-16,41,58),STEEL))
 return m

def press():
 m=base(4)
 m.append(box((-28,3,10),(12,10,55),STEEL))
 for x in (-27,6):
  for z in (14,45):m.append(box((x,10,z),(x+5,43,z+5),OLIVE))
 m.append(box((-27,40,14),(11,46,50),STEEL))
 m.append(box((-23,10,17),(7,14,47),CHROME))
 m.append(box((-22,22,18),(6,26,46),CHROME))
 m+=cyl('y',-8,32,5,26,42,STEEL)
 # Seated injection barrel and feed hopper join the mold bed.
 m+=cyl('z',-8,18,5,3,28,OLIVE)
 m.append(box((-13,20,4),(-3,30,14),OLIVE))
 m+=cyl('z',-8,18,2,1,4,STEEL)
 m.append(box((-25,14,34),(-21,23,48),STEEL))
 return m

MODEL_FACTORIES={'coastal_seawater_intake':intake,'brine_concentrator':concentrator,'mineral_conditioner':conditioner,'chloride_dryer':dryer,'magnesium_electrolysis_cell':magnesium,'titanium_chlorinator':chlorinator,'titanium_reduction_retort':retort,'polymer_molding_press':press}
MODELS={n:f() for n,f in MODEL_FACTORIES.items()}
PIPE_PATHS={}
for name,parts in MODELS.items():
 f=FORMS[name];d=len(f['layers'][0]);PIPE_PATHS[name]={}
 for _,kind,target,col,row,layer,side in f['ports']:
  x=8-16*col;y=8+16*layer
  if kind.startswith('FLUID'):
   # Service runs enter the sump or vessel; exposed ends are real transport sockets.
   endz=30 if d==3 else 40
   points=[(x,y,16*d-.5),(x,y,endz)]
   if name=='coastal_seawater_intake':points.append((-8,y,endz));x=-8
   if y==8:points.append((x,18,endz))
   pipe_run(parts,points);PIPE_PATHS[name][kind,target]=points
  elif kind.startswith('ITEM'):
   z=1 if side=='FRONT' else 16*d-7
   parts+=drawer(x-5,x+5,y-4,y+4,z,z+5)
   lo,hi=(6,22) if side=='FRONT' else (16*d-23,16*d-7)
   if name=='titanium_chlorinator' and side=='FRONT':hi=34
   parts.append(box((x-3,y-3,lo),(x+3,y+3,hi),STEEL))
ROTORS={}
for name in MODELS:
 y={'coastal_seawater_intake':17,'brine_concentrator':17,'mineral_conditioner':22,'polymer_molding_press':18}.get(name,26)
 if name=='magnesium_electrolysis_cell':y=24
 z=2 if name=='polymer_molding_press' else 8
 quads=fan('z',z-1,z+1,-8,y,5) if name in ('chloride_dryer','magnesium_electrolysis_cell') else coupling('z',z-1,z+1,-8,y,3)
 ROTORS[name+'_drive']=dict(block=name,axis='z',center=[-8,y,z],property='lit',speed=4,ease=30,always=True,quads=quads)
ITEM_EXTRAS={}
