"""Original mage meshes/skins and source casting clips; native bounded encounter AI."""
KINDS={'pyromancer':'abstract_casting_mob','cryomancer':'abstract_casting_mob',
       'archevoker':'archevoker','cultist':'abstract_casting_mob','priest':'archevoker',
       'apothecarist':'piglin_casting_mob','necromancer':'skeleton_mob',
       'cursed_armor_stand':'armor_stand','dead_king':'dead_king','fire_boss':'tyros','dead_king_corpse':'dead_king'}
def entities(): return ['jimtafacts_'+name for name in KINDS]+['jimtafacts_wizard_bolt']
def items(): return ['jimta_spellbooks_'+name+'_spawn_egg' for name in KINDS]
def generate(copy,texture,dump,RES,ART,lang):
    for name,geo in KINDS.items():
        copy(ART/f'geo/{geo}.geo.json',RES/f'assets/jugcraft/geckolib/models/entity/jimtafacts/mage_{name}.geo.json')
        if name!='cursed_armor_stand':
            variants={'dead_king':['dead_king/dead_king','dead_king/dead_king_enraged'],
                      'fire_boss':['fire_boss/tyros','fire_boss/tyros_soul_mode'],
                      'priest':['priest/priest'],'dead_king_corpse':['dead_king/dead_king_resting']}.get(name,[name])
            for variant in variants: texture('irons_spellbooks:entity/'+variant)
        title={'fire_boss':'Tyros, the Cinderous King','dead_king':'Dead King'}.get(name,name.replace('_',' ').title())
        lang['entity.jugcraft.jimtafacts_'+name]=title
        lang['item.jugcraft.jimta_spellbooks_'+name+'_spawn_egg']=title+' Spawn Egg'
        dump(RES/f'assets/jugcraft/items/jimta_spellbooks_{name}_spawn_egg.json',{'model':{'type':'minecraft:model','model':'minecraft:item/evoker_spawn_egg'}})
    copy(ART/'animations/casting_animations.json',RES/'assets/jugcraft/geckolib/animations/entity/jimtafacts/mage_casting.animation.json')
    lang['entity.jugcraft.jimtafacts_wizard_bolt']='Mage Projectile'
