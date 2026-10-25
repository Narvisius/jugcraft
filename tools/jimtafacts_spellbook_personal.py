"""Source close-range, frozen-copy and weapon-echo workings."""
PERSONAL = {
    'blood_step': ('blood',10,12,0,'Blink up to eight blocks, or behind an aimed enemy, and gain five seconds of true invisibility.','fermented_spider_eye'),
    'frost_step': ('ice',6,12,0,'Blink eight blocks, leaving a frozen copy that distracts enemies and shatters into eight ice shards.','packed_ice'),
    'frostbite': ('ice',16,60,1,'For thirty seconds, killing a fully frozen foe leaves a frozen copy that taunts enemies and shatters into damaging ice.','blue_ice'),
    'echoing_strikes': ('ender',10,60,0,'Your next three melee or arrow hits within two minutes summon a sword or arrow echo for seventy-five percent of that hit’s damage.','amethyst_shard'),
    'devour': ('blood',6,20,.8,'A spectral jaw bites the aimed enemy for six damage and returns fifteen percent as health. Kills grant two maximum health for one minute, stacking to twenty.','rotten_flesh'),
    'ice_spikes': ('ice',6,15,0,'Raise eight growing ice spikes along the ground. Small spikes deal six damage; the final spike deals twelve. They launch and freeze enemies.','ice'),
}
def definitions():return {name:row[:-1] for name,row in PERSONAL.items()}
def recipes():return {name:row[-1] for name,row in PERSONAL.items()}
def generate(copy,texture,dump,res,art,lang):
    for path in ('entity/ice_spike','entity/devour_jaw','entity/frozen_humanoid','entity/frozen_humanoid_alt','entity/icicle_projectile','entity/magic_arrow'):
        if (art/('textures/'+path+'.png')).exists():texture('irons_spellbooks:'+path)
    for kind in ('ice_spike','devour_jaw','frozen_copy','echoing_sword'):
        lang['entity.jugcraft.jimtafacts_'+kind]=kind.replace('_',' ').title()
