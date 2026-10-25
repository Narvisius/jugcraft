"""Independent field mechanics, adapted from the supplied spell and AOE classes."""
# school, Focus, cooldown, cast, radius, lifetime, interval, burst, pulse,
# secondary, following caster, horizontal drift, recipe reagent, description
FIELDS={
 'healing_circle':('holy',8,25,1,5,200,40,0,1,'heal',False,0,'pink_petals','Place a five-block healing circle for ten seconds; heals nearby allies every two seconds.'),
 'blizzard':('ice',8,22,1.25,8,230,10,0,0,'blizzard',False,.05,'powder_snow_bucket','Send a slowly moving blizzard that freezes and swirls enemies for eleven seconds.'),
 'earthquake':('nature',10,16,2,8,240,20,0,2,'quake',False,0,'coarse_dirt','Shake a chosen eight-block area for twelve seconds, damaging and slowing grounded enemies without breaking terrain.'),
 'thunderstorm':('lightning',14,120,2,20,400,40,0,8,'storm',True,0,'copper_bulb','Carry a twenty-second storm that strikes visible enemies within twenty blocks every two seconds; no world-damaging lightning.'),
 'scorch':('fire',10,12,1,2.5,200,20,8,1,'ignite',False,0,'magma_block','Scorch a chosen circle for eight damage, leaving a damaging burning field for ten seconds; never places fire blocks.'),
 'poison_splash':('nature',8,20,.75,3,140,20,8,1,'poison',False,0,'spider_eye','Erupt poison at the aimed location for eight damage, then leave a seven-second poison cloud.'),
}
def definitions():
    return {name:(v[0],v[1],v[2],v[3],v[-1]) for name,v in FIELDS.items()}
def profiles():
    return {name:dict(radius=v[4],lifetime=v[5],interval=v[6],burst=v[7],pulse=v[8],secondary=v[9],follow=v[10],drift=v[11]) for name,v in FIELDS.items()}
