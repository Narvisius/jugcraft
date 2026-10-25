"""Source continuous-cast families; each native pulse pays shared Focus once."""
CHANNELS={
 'blaze_storm':('fire',1,15,2.75,'blaze',32,2,'ignite',40,'blaze_rod'),
 'starfall':('ender',1,35,8,'comet',40,4,'',0,'nether_star'),
 'fire_breath':('fire',1,20,5,'cone',10,1,'ignite',80,'blaze_powder'),
 'dragon_breath':('ender',1,20,5,'cone',10,1,'',0,'dragon_breath'),
 'poison_breath':('nature',1,20,5,'cone',10,1,'poison',60,'spider_eye'),
 'cone_of_cold':('ice',1,20,5,'cone',10,1,'freeze',80,'snowball'),
 'electrocute':('lightning',1,20,5,'cone',10,2,'',0,'copper_ingot'),
 'ray_of_siphoning':('blood',2,25,5,'ray',12,1,'siphon',0,'fermented_spider_eye'),
 'cloud_of_regeneration':('holy',2,35,10,'allies',5,1,'heal',0,'ghast_tear'),
}
def definitions():
 result={}
 for name,(school,cost,cd,duration,shape,reach,damage,status,ticks,reagent) in CHANNELS.items():
  effect='heal nearby allies' if shape=='allies' else 'drain health from the aimed enemy' if shape=='ray' else 'sweep a short cone in front of you'
  if shape in ('blaze','comet'):
   effect='launch a stream of original animated fireballs' if shape=='blaze' else 'rain pairs of original animated comets over the first aimed location'
  period=.25 if shape=='blaze' else .2 if shape=='comet' else .5
  result[name]=(school,cost,cd,duration,f'Hold the casting control to {effect} for up to {duration} seconds. Costs {cost} Focus per {period:g}-second pulse; release to stop. A loose scroll produces one pulse.')
 return result
def profiles():
 return {name:dict(shape=r[4],range=r[5],damage=r[6],secondary=r[7],duration=r[8]) for name,r in CHANNELS.items()}
