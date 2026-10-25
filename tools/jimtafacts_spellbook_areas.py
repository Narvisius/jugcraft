"""Distinct persistent source spell outcomes, with server-protected target loops."""
AREAS={
 'black_hole':('ender',60,120,5,'ender_eye','Create a six-block gravity well for thirty seconds. It draws permitted foes inward and deals two damage near the core every half-second. Terrain stays intact.'),
 'gravity_fissure':('ender',30,45,.75,'chorus_fruit','Send a moving gravity well forward for three seconds, drawing permitted foes with it without direct damage.'),
 'ball_lightning':('lightning',4,1,0,'copper_ingot','Launch a five-damage lightning orb that can hit again after half a second. It bounces off six surfaces or expires after five seconds.'),
 'firefly_swarm':('nature',8,20,1.5,'glow_berries','Send a pursuing swarm after your target for ten seconds. Its two-block cloud deals two damage every three-quarters of a second.'),
 'fang_swirl':('evocation',11,28,1,'pointed_dripstone','Send fangs toward your aimed point, then sweep an eight-second, five-block ring of four-damage fangs around it.'),
 'sunbeam':('holy',8,20,0,'glowstone_dust','Mark a two-block area with a narrowing column of sunlight. After three-quarters of a second it strikes for twelve damage.'),
 'wall_of_fire':('fire',6,30,0,'blaze_powder','Choose up to four ground anchors within two seconds to draw a thirteen-block fire wall. It lasts twelve seconds, burns foes and never ignites terrain.'),
 'ice_tomb':('ice',6,30,0,'packed_ice','Enclose yourself in breakable ice for five seconds, healing one health each second. Sneak to leave early; attacks break the shell.'),
}
RECASTS={'wall_of_fire'}
def definitions():return {n:(*v[:4],v[5]) for n,v in AREAS.items()}
def recipes():return {n:v[4] for n,v in AREAS.items()}
def generate(copy,texture,dump,res,art,lang):
    for path in ('entity/black_hole/black_hole','entity/black_hole/beam','entity/ice_tomb/ice_tomb','entity/ice_tomb/ice_tomb_cull'):
        texture('irons_spellbooks:'+path)
    for path in (art/'textures/particle').glob('*firefly*.png'):texture('irons_spellbooks:particle/'+path.stem)
    for name in ('black_hole','gravity_fissure','firefly_swarm','fang_swirl','sunbeam','wall_of_fire','ice_tomb'):
        lang['entity.jugcraft.jimtafacts_'+name]=name.replace('_',' ').title()
