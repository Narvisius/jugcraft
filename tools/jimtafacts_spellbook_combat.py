"""Distinct native combat deliveries recovered from the supplied spell classes.

Damage and costs are calibrated to Jugcraft's shared Focus and bounded effects;
projectiles, rays, areas, secondary statuses and original sprite identities stay
separate. This table is not a blanket alias from every source spell to one bolt.
"""
COMBAT = {
    # name: school, Focus, cooldown seconds, cast seconds, delivery, damage, range,
    # radius, secondary, secondary duration, source sprite, velocity, homing, pierce
    'firebolt': ('fire',2,1,0,'PROJECTILE',6,32,0,'ignite',60,'entity/fireball/firebolt',1.5,0,0),
    'magic_missile': ('ender',2,1,0,'PROJECTILE',6,32,0,'',0,'entity/magic_missile/magic_missile',1.3,5,0),
    'icicle': ('ice',2,1,0,'PROJECTILE',6,32,0,'freeze',80,'entity/icicle_projectile',1.6,0,0),
    'fireball': ('fire',12,25,2,'PROJECTILE',10,40,3,'ignite',60,'entity/fireball/fireball_core',.8,0,0),
    'magic_arrow': ('ender',8,8,1.5,'PROJECTILE',10,48,0,'',0,'entity/magic_arrow',2.6,0,2),
    'fire_arrow': ('fire',8,8,1,'PROJECTILE',11,48,3,'ignite',80,'entity/fire_arrow',2.2,0,0),
    'poison_arrow': ('nature',8,15,1,'PROJECTILE',5,40,0,'poison',160,'entity/arrow',2,0,0),
    'lightning_lance': ('lightning',10,8,2,'PROJECTILE',14,48,0,'',0,'entity/lightning_lance/lightning_lance',2.4,0,4),
    'wither_skull': ('blood',4,1,0,'PROJECTILE',6,32,0,'wither',100,'entity/wither_skull',1,0,0),
    'guiding_bolt': ('holy',4,8,0,'PROJECTILE',3,40,0,'guided',500,'entity/guiding_bolt/guiding_bolt',1.5,0,0),
    'acid_orb': ('nature',8,15,.75,'PROJECTILE',0,24,3,'rend',400,'entity/acid_orb/acid_orb',.9,0,0),
    'ray_of_frost': ('ice',5,15,0,'BEAM',12,30,0,'freeze',90,'entity/ray_of_frost/core',0,0,0),
    'sonic_boom': ('eldritch',22,25,1.5,'BEAM',20,32,0,'knockback',0,'entity/ray/solid',0,0,8),
    'shockwave': ('lightning',14,30,.8,'AREA',10,9,9,'knockback',0,'entity/ray/solid',0,0,0),
    'frostwave': ('ice',10,45,1,'AREA',0,6.75,6.75,'chilled',200,'entity/ray_of_frost/core',0,0,0),
    'heat_surge': ('fire',10,45,1,'AREA',0,6.5,6.5,'rend_fire',200,'entity/flame',0,0,0),
    'blood_needles': ('blood',5,10,0,'NATIVE_BLOOD',2,32,0,'leech25',0,'entity/blood_needle/needle_5',1.2,0,0),
    'acupuncture': ('blood',5,15,0,'NATIVE_BLOOD',2,32,0,'',0,'entity/blood_needle/needle_5',.35,0,0),
    'blood_slash': ('blood',5,10,0,'NATIVE_BLOOD',10,32,0,'leech15',0,'entity/blood_slash/blood_slash_large',.8,0,16),
}

def definitions():
    result = {}
    for name, (school,cost,cooldown,cast,delivery,damage,reach,radius,secondary,duration,texture,speed,homing,pierce) in COMBAT.items():
        action = 'Launch' if delivery == 'PROJECTILE' else 'Project' if delivery == 'BEAM' else 'Release'
        extras = {'ignite':'ignites targets', 'freeze':'freezes targets', 'chilled':'slows and chills targets',
                  'poison':'poisons targets', 'wither':'withers targets', 'guided':'marks targets for your next critical strike',
                  'rend':'weakens armor', 'rend_fire':'weakens armor and ignites targets', 'knockback':'pushes targets away',
                  'leech25':'five converging needles restore a quarter of the health actually removed', 'leech15':'a widening slash restores fifteen percent of the health actually removed'}
        description = f'{action} a {name.replace("_", " ")}' + (f' for {damage} damage' if damage else '')
        if secondary:
            description += '; ' + extras[secondary]
        if radius:
            description += f' in a {radius:g}-block radius'
        description += '. Respects claims, parties and PvP; never destroys terrain.'
        if name=='acupuncture':description='Surround the aimed enemy with five inward-flying needles for two damage each. Walls can intercept them. Respects claims, parties and PvP.'
        result[name] = (school,cost,cooldown,cast,description)
    return result

def profile(name):
    school,cost,cooldown,cast,delivery,damage,reach,radius,secondary,duration,texture,speed,homing,pierce=COMBAT[name]
    return {'delivery':delivery,'damage':damage,'range':reach,'radius':radius,'secondary':secondary,'duration':duration,
            'texture':texture,'velocity':speed,'homing':homing,'pierce':pierce}

def cube(x,y,z,dx,dy,dz,u,v,tw,th):
    """Minecraft ModelPart cube UV unfolding, recovered from the supplied createBodyLayer."""
    rects={'west':[u,v+dz,u+dz,v+dz+dy], 'north':[u+dz,v+dz,u+dz+dx,v+dz+dy],
           'east':[u+dz+dx,v+dz,u+dz+dx+dz,v+dz+dy], 'south':[u+dz+dx+dz,v+dz,u+2*dz+2*dx,v+dz+dy],
           'up':[u+dz,v,u+dz+dx,v+dz], 'down':[u+dz+dx,v+dz,u+dz+2*dx,v]}
    return {'from':[8+x,8-y-dy,8+z],'to':[8+x+dx,8-y,8+z+dz],
            'faces':{face:{'uv':[a*16/(tw if i%2==0 else th) for i,a in enumerate(uv)],'texture':'#sprite'} for face,uv in rects.items()}}

def projectile_model(name,tex):
    orientation='TOWARDS_CAMERA';scale=.5
    if name=='icicle':
        elements=[cube(-1,-1,-6,2,2,6,0,8,16,16),cube(-1.5,-1.5,0,3,3,5,0,0,16,16)]
        orientation='TOWARDS_MOTION';scale=1
    elif name=='firebolt':
        elements=[cube(-1,-1,-4,2,2,8,0,0,20,10)]
        orientation='TOWARDS_MOTION';scale=1
    elif name in ('fire_arrow','magic_arrow','poison_arrow'):
        # Source renderModel emits four crossed arrow faces; only the arrow strip
        # (half atlas width x 5/32 atlas height) belongs on those faces.
        uv=[0,0,8,2.5]
        elements=[{'from':[8,6,0],'to':[8,10,16],'shade':False,'faces':{f:{'uv':uv,'texture':'#sprite'} for f in ('east','west')}},
                  {'from':[6,8,0],'to':[10,8,16],'shade':False,'faces':{f:{'uv':uv,'texture':'#sprite'} for f in ('up','down')}}]
        orientation='TOWARDS_MOTION';scale=2.08
    elif name=='wither_skull':
        elements=[cube(-4,-4,-4,8,8,8,0,0,32,32)];orientation='TOWARDS_MOTION';scale=1
    elif name in ('fireball','magic_missile'):
        # Both supplied renderers use the same 8px body cube and 48x24 UV atlas.
        elements=[cube(-4,-4,-4,8,8,8,0,0,48,24)];scale=.35 if name=='magic_missile' else 1
    elif name=='acid_orb':
        # Source renderer wraps an 8px cube over the repeating 8px texture.
        elements=[{'from':[4,4,4],'to':[12,12,12],'faces':{f:{'uv':[0,0,16,16],'texture':'#sprite'} for f in ('up','down','north','south','east','west')}}];scale=1
    else:
        # Original magic-missile/fireball/guiding-bolt cores are particle sprites.
        # The animated lightning atlas uses a single frame here until its shader
        # renderer is ported; do not stretch all seven frames across the projectile.
        uv=[0,0,16/7,16] if name=='lightning_lance' else [0,0,16,16]
        elements=[{'from':[0,0,8],'to':[16,16,8],'shade':False,'faces':{f:{'uv':uv,'texture':'#sprite'} for f in ('north','south')}}]
    return {'textures':{'particle':tex,'sprite':tex},'elements':elements},orientation,scale

def spell(name, school_id, copy_texture, dump, res):
    from pathlib import Path
    school,cost,cooldown,cast,delivery,damage,reach,radius,secondary,duration,texture,speed,homing,pierce=COMBAT[name]
    result={'school':school_id,'range':reach,'tier':1,'group':'jimtafacts_spellbooks','type':'ACTIVE',
            'active':{'cast':{'duration':cast,'animation':{'id':'jugcraft:kindle_cast'}}},
            'release':{'animation':{'id':'jugcraft:kindle_release'}},
            'target':{'type':'AIM','aim':{'required':False}},'deliver':{'type':'DIRECT'},
            'impacts':[{'action':{'type':'CUSTOM','custom':{'handler':'jugcraft:jimtafacts_combat','intent':'HARMFUL'}}}],
            'cost':{'exhaust':0,'durability':0,'cooldown':{'duration':cooldown,'hosting_item':False}}}
    tex = copy_texture('irons_spellbooks:' + texture)
    if delivery == 'NATIVE_BLOOD':
        result['deliver']={'type':'CUSTOM','custom':{'handler':'jugcraft:jimtafacts_blood'}}
    elif delivery == 'PROJECTILE':
        # Supplied projectile renderer sprites remain billboarded/oriented, rather
        # than replacing them with a guessed creature cube or generic item.
        model='spell_projectile/jimtafacts/' + name
        shape,orientation,scale=projectile_model(name,tex)
        dump(res / f'assets/jugcraft/models/{model}.json',shape)
        result['deliver']={'type':'PROJECTILE','projectile':{'launch_properties':{'velocity':speed},'projectile':{
            'homing_angle':homing,'perks':{'pierce':pierce},'client_data':{'light_level':12,'composite_model':{'models':[
                {'orientation':orientation,'fx':{'model_id':'jugcraft:'+model,'scale':scale,'light_emission':'RADIATE'}}]}}}}}
        if radius:
            result['area_impact']={'radius':radius,'triggering_action_type':'CUSTOM','execute_action_type':'CUSTOM','force_indirect':True,
                                   'area':{'distance_dropoff':'NONE','include_caster':False}}
    elif delivery == 'BEAM':
        result['target']={'type':'BEAM','cap':8 if pierce else 1,'beam':{'texture_id':tex.replace('jugcraft:','jugcraft:textures/')+'.png',
                           'width':.22 if name=='sonic_boom' else .12,'color_rgba':0xffffffff,'inner_color_rgba':0xffffffff}}
    else:
        result['target']={'type':'AREA','cap':16,'area':{'distance_dropoff':'NONE','include_caster':False,'angle_degrees':360}}
    return result
