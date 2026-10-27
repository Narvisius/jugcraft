"""Defensive and dimensional spells. Original art bytes and source shield geometry are retained."""
import json
from nouveau_nbt import adapt

WARDS={
 'abyssal_shroud':('eldritch',30,200,0,'echo_shard','For six seconds, dodge incoming attacks into nearby clear, permitted ground. Fall and bypass damage still apply.'),
 'arcane_shackle':('ender',4,45,.5,'iron_chain','Launch an ender chain that tethers a permitted enemy to a breakable fifteen-health anchor for eleven seconds.'),
 'heartstop':('blood',10,120,0,'redstone','Defer post-armour damage for ten seconds, then repay the entire accumulated debt. Cleansing or logout cannot erase the debt.'),
 'planar_sight':('eldritch',15,200,0,'ender_eye','See nearby living creatures through terrain for forty seconds. Only your view changes.'),
 'pocket_dimension':('eldritch',30,60,2,'nether_star','Leave combat and enter your own persistent room. Touch its portal frame to return. The original room is built only on first entry.'),
 'telekinesis':('eldritch',1,35,7,'ender_pearl','Hold a permitted enemy along your gaze for up to seven seconds. Each tenth-second pulse costs one Focus; releasing stops the force.'),
 'evasion':('ender',4,180,0,'chorus_fruit','Dodge the next incoming attack to safe ground. Lasts one minute; fall and bypass damage are excluded.'),
 'portal':('ender',20,180,0,'ender_eye','Cast twice within two minutes to link two locations for five minutes, or two crafted Portal Frames permanently. Both endpoints must be loaded and permitted. Dye frames to recolour; sneak-use an empty hand to unlink.'),
 'recall':('ender',10,300,4,'ender_pearl','Outside combat, return to your bed or respawn anchor, or world spawn when no valid respawn point remains.'),
 'invisibility':('evocation',4,45,2,'fermented_spider_eye','Hide yourself and your equipment for ten seconds. Nearby enemies lose their target; attacking breaks the concealment.'),
 'scapegoat':('evocation',3,45,0,'wheat','Send an animated twenty-health decoy running toward your aim for fifteen seconds. Nearby permitted enemies chasing you follow it.'),
 'shield':('evocation',4,8,0,'amethyst_shard','Place a fifteen-health magical shield in front of you for twenty seconds. Its original animated surface intercepts projectiles and attacks crossing the shield.'),
}
RECASTS={'portal'}
CHANNEL_TICKS={'telekinesis':2}
def definitions():return {n:(*v[:4],v[5]) for n,v in WARDS.items()}
def recipes():return {n:v[4] for n,v in WARDS.items()}

def generate(copy,texture,dump,RES,ART,lang):
    for name in ('scapegoat',):
        copy(ART/f'geo/{name}.geo.json',RES/f'assets/jugcraft/geckolib/models/entity/jimtafacts/{name}.geo.json')
        copy(ART/f'animations/{name}_animations.animation.json',RES/f'assets/jugcraft/geckolib/animations/entity/jimtafacts/{name}.animation.json')
        texture('irons_spellbooks:entity/'+name)
    for path in ('entity/shield/shield_overlay','entity/shield/shield_trim','entity/ender_chain','entity/portal/portal_round','entity/portal/portal_square','entity/portal/portal_square_color','entity/portal/pocket_dimension_portal_round','entity/portal/pocket_dimension_portal_square','environment/pocket_dimension_sky','environment/pocket_clouds','environment/single_cloud','environment/noise_tile'):
        texture('irons_spellbooks:'+path)
    for name in ('abyssal_shroud','heartstop','planar_sight','evasion','true_invisibility'):
        original=ART/f'textures/mob_effect/{name}.png'
        if original.exists():copy(original,RES/f'assets/jugcraft/textures/mob_effect/jimtafacts_{name}.png')
        lang['effect.jugcraft.jimtafacts_'+name]=name.replace('_',' ').title()
    for name in ('scapegoat','shield','portal','pocket_portal','arcane_shackle'):lang['entity.jugcraft.jimtafacts_'+name]=name.replace('_',' ').title()
    lang['death.attack.jimtafacts_heartstop']='%1$s could not repay their Heartstop debt'
    dump(RES/'data/jugcraft/damage_type/jimtafacts_heartstop.json',{'message_id':'jimtafacts_heartstop','scaling':'never','exhaustion':0})
    for tag in ('bypasses_armor','bypasses_effects','bypasses_resistance','bypasses_invulnerability','bypasses_enchantments','bypasses_cooldown'):
        path=RES/f'data/minecraft/tags/damage_type/{tag}.json'
        data=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False,'values':[]}
        if 'jugcraft:jimtafacts_heartstop' not in data['values']:data['values'].append('jugcraft:jimtafacts_heartstop')
        dump(path,data)
    copied=set()
    def model(ref):
        if not ref.startswith('irons_spellbooks:'):return ref
        path=ref.split(':',1)[1];native='jugcraft:jimtafacts/spellbooks/'+path
        if path in copied:return native
        copied.add(path);data=json.loads((ART/f'models/{path}.json').read_text(encoding='utf-8'))
        if 'parent' in data:data['parent']=model(data['parent'])
        for key,value in data.get('textures',{}).items():data['textures'][key]=texture(value)
        data.pop('render_type',None)
        dump(RES/f'assets/jugcraft/models/jimtafacts/spellbooks/{path}.json',data);return native
    for name in ('voidstone','pocket_dimension_portal_frame','portal_frame'):
        state=json.loads((ART/f'blockstates/{name}.json').read_text(encoding='utf-8'))
        def visit(value):
            if isinstance(value,list):return [visit(x) for x in value]
            if isinstance(value,dict):return {k:model(v) if k=='model' else visit(v) for k,v in value.items()}
            return value
        dump(RES/f'assets/jugcraft/blockstates/jimta_spellbooks_{name}.json',visit(state))
        lang['block.jugcraft.jimta_spellbooks_'+name]=name.replace('_',' ').title()
    frame='jugcraft:jimta_spellbooks_portal_frame'
    dump(RES/'assets/jugcraft/items/jimta_spellbooks_portal_frame.json',{'model':{'type':'minecraft:model','model':model('irons_spellbooks:item/portal_frame')}})
    dump(RES/'data/jugcraft/recipe/jimta_spellbooks_portal_frame.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['MEM','I I','I I'],'key':{'M':'minecraft:netherite_ingot','E':'minecraft:ender_pearl','I':'jugcraft:jimta_spellbooks_arcane_ingot'},'result':{'id':frame,'count':2}})
    dump(RES/'data/jugcraft/loot_table/blocks/jimta_spellbooks_portal_frame.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':frame}],'condition':{'type':'minecraft:all_of','terms':[{'type':'minecraft:survives_explosion'},{'type':'minecraft:match_block','blocks':frame,'state':{'half':'lower'}}]}}]})
    template=ART.parents[1]/'data/irons_spellbooks/structure/pocket_room.nbt'
    target=RES/'data/jugcraft/structure/jimtafacts/pocket_room.nbt';target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(adapt(template.read_bytes(),lambda s:s.replace('irons_spellbooks:voidstone','jugcraft:jimta_spellbooks_voidstone').replace('irons_spellbooks:pocket_dimension_portal_frame','jugcraft:jimta_spellbooks_pocket_dimension_portal_frame')))
    dimension=json.loads((RES/'data/jugcraft/dimension_type/nouveau_planarium.json').read_text(encoding='utf-8'))
    dimension['attributes']['minecraft:visual/sky_color']='#1a082c';dimension['attributes']['minecraft:visual/fog_color']='#1a082c'
    dump(RES/'data/jugcraft/dimension_type/jimtafacts_pocket_dimension.json',dimension)
    dump(RES/'data/jugcraft/dimension/jimtafacts_pocket_dimension.json',{'type':'jugcraft:jimtafacts_pocket_dimension','generator':{'type':'minecraft:flat','settings':{'biome':'minecraft:the_void','features':False,'lakes':False,'layers':[],'structure_overrides':[]}}})
