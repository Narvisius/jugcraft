"""Distinct source strike geometry, travelling ground attacks and movement abilities."""
STRIKES={
 'divine_smite':('holy',6,10,.8,'glowstone_dust','Smite a small area just ahead with eight damage plus the melee damage of your offhand weapon.'),
 'flaming_strike':('fire',6,10,.6,'fire_charge','Sweep enemies in a short forward arc for five damage plus your offhand weapon damage, igniting them.'),
 'shadow_slash':('ender',6,15,0,'ender_pearl','Slash the nearest enemy ahead and nearby foes, parry incoming projectiles, then surge toward the strike.'),
 'burning_dash':('fire',8,15,0,'blaze_rod','Dash forward, striking each contacted enemy once and leaving them burning. Terrain and claims constrain the dash.'),
 'volt_strike':('lightning',6,10,0,'copper_ingot','Surge forward in lightning, striking contacted enemies once for six damage.'),
 'ascension':('lightning',10,15,0,'feather','Launch upward in a lightning blast. Damage decreases away from you; brief fall protection follows.'),
 'chain_lightning':('lightning',5,20,0,'redstone','Strike an aimed enemy for six damage, then arc to up to three more enemies within four blocks of each link.'),
 'lightning_bolt':('lightning',15,25,0,'lightning_rod','Call lightning at an aimed location. Ten damage at the centre falls away across four blocks; never starts fires.'),
 'fang_strike':('evocation',5,10,1,'flint','Send eight evoker fangs forward in a travelling line. Fangs respect claims, allies and PvP.'),
 'fang_ward':('evocation',7,15,1,'bone','Raise two expanding rings of evoker fangs around yourself.'),
 'stomp':('nature',10,15,.5,'stone','Send a five-block ground shock forward, striking grounded enemies and throwing up harmless terrain particles.'),
}
def definitions():return {n:(*v[:4],v[5]) for n,v in STRIKES.items()}
