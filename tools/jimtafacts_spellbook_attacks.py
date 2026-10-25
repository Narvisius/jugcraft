"""Source level-one attack outcomes and native server-safe adaptations."""
ATTACKS = {
    'soulfire_ray': ('fire',10,12,.25,'Strike the first target along a thirty-block soulfire ray for twelve damage.','soul_sand'),
    'eldritch_blast': ('eldritch',18,15,0,'Cast three fifteen-damage beams within four seconds. Pay once; subsequent beams have no extra Focus cost.','sculk'),
    'flaming_barrage': ('fire',16,15,0,'Launch five cursor-steered fireballs within six seconds, for three damage each. Pay once for the volley.','fire_charge'),
    'gust': ('evocation',6,15,.75,'Push enemies in a forward eight-block gust; targets hurled into walls take collision damage. Casting against a nearby wall gives a small kickback.','feather'),
    'throw': ('evocation',2,5,.25,'Throw a spectral copy of your offhand item for one damage plus its weapon damage. The original stays in your hand.','stick'),
    'sacrifice': ('blood',5,30,0,'Consume an owned summoned defender to explode for twelve plus half its remaining health. No terrain damage or equipment drops.','bone'),
    'snowball': ('ice',8,18,1,'Lob a snowball that chills nearby foes and leaves a four-block frost field for ten seconds.','snowball'),
    'magma_bomb': ('fire',6,15,1,'Lob an eight-damage magma bomb that leaves a four-block burning field for ten seconds.','magma_cream'),
    'lob_creeper': ('evocation',4,8,0,'Throw an explosive creeper head for up to six damage. Explosions respect claims and never damage terrain.','gunpowder'),
    'chain_creeper': ('evocation',8,25,1.5,'Scatter three creeper heads at the aimed location. A killing blast can create another ring, capped at two additional generations.','creeper_head'),
    'firecracker': ('evocation',4,8,0,'Burst a firework at your aimed location for four damage to nearby foes.','firework_rocket'),
    'arrow_volley': ('evocation',8,20,1.5,'Rain five rows of five spectral arrows over the aimed location. Each arrow deals two damage and cannot be collected.','arrow'),
    'raise_hell': ('fire',18,30,.8,'Cast three expanding ground eruptions within four seconds. Each reaches eight blocks and adds your offhand weapon damage. Pay once.','magma_block'),
    'thunder_step': ('lightning',15,30,0,'Launch a moving thunder orb, then cast again within five seconds to teleport to it and strike foes along your path. Maximum travel: thirty-two blocks.','ender_pearl'),
}
RECASTS = {'eldritch_blast','flaming_barrage','raise_hell','thunder_step'}
def definitions():return {name:row[:-1] for name,row in ATTACKS.items()}
def recipes():return {name:row[-1] for name,row in ATTACKS.items()}
def generate(copy,texture,dump,res,art,lang):
    for path in ['entity/fireball/magma','entity/comet/comet']+[f'entity/ball_lightning/ball_lightning_{i}' for i in range(5)]+[f'entity/fireball/swirl_{i}' for i in range(11)]+[f'entity/fireball/fire_{i}' for i in range(4)]+[f'entity/comet/fire_{i}' for i in range(1,5)]:texture('irons_spellbooks:'+path)
    lang['entity.jugcraft.jimtafacts_shot']='Spell Projectile'
