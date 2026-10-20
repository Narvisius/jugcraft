"""Translate the owner's block states and models to the actual native block families.

This changes state bindings and geometry metadata, not the supplied artwork.
"""
import copy
import json


def write_all(write, assets, data):
    from nouveau_content import CATALOG, ASSET, INTERNAL, id_of, asset_id, walk, presentation
    def model(path): return 'jugcraft:nouveau/' + path
    def save_state(name, value): write(assets / 'blockstates' / (id_of(name)+'.json'), value)
    def read(path): return json.loads(path.read_text(encoding='utf-8'))
    # Sprite atlases only discover their default block/item/particle directories.
    # Owner files deliberately live under nouveau/ and need explicit atlas sources.
    write(assets.parent/'minecraft/atlases/blocks.json',{'sources':[{'type':'minecraft:directory','source':'nouveau/'+folder,'prefix':'nouveau/'+folder+'/'} for folder in ('block','item','entity/chest','entity/signs')]+[{'type':'minecraft:single','resource':'jugcraft:nouveau/entity/carbuncle_shades'}]})
    write(assets.parent/'minecraft/atlases/particles.json',{'sources':[{'type':'minecraft:directory','source':'nouveau/particle','prefix':'nouveau/'}]})
    horizontal={'north':0,'east':90,'south':180,'west':270}
    for name,spec in CATALOG['blocks'].items():
        if spec['kind']=='existing': continue
        original=ASSET/'blockstates'/(name+'.json')
        state=walk(read(original),asset_id) if original.exists() else {'variants':{'':{'model':model('block/'+name)}}}
        if name=='portal': state={'variants':{'axis=x':{'model':model('block/warp_portal_ew')},'axis=z':{'model':model('block/warp_portal_ns')},'axis=y':{'model':model('block/warp_portal_ud')}}}
        elif name=='magic_fire': state={'multipart':[{'apply':[{'model':model('block/fire_floor'+str(i))} for i in (0,1)]}]+[{'apply':[{'model':model('block/fire_side'+str(i)),'y':rotation} for i in (0,1)]} for rotation in (0,90,180,270)]}
        elif name=='magelight_torch': state={'variants':{'facing='+f:{'model':model('block/magelight_torch'), **rotation} for f,rotation in {'up':{},'down':{'x':180},'north':{'x':90},'south':{'x':90,'y':180},'east':{'x':90,'y':90},'west':{'x':90,'y':270}}.items()}}
        elif spec['kind'] in {'station','animated_decor'}:
            if spec['geo']:
                state={'variants':{'':{'model':model('block/'+name)}}}
                write(assets/'models/nouveau/block'/(name+'.json'),{'textures':{'particle':model('block/'+presentation()[spec['geo']]['texture'])},'elements':[]})
            elif name in {'source_jar','creative_source_jar','potion_jar'}: pass
            elif name=='arcane_pedestal': pass
            elif 'variants' in state:
                variants=state['variants']
                directional={k:v for k,v in variants.items() if k.startswith('facing=') and ',' not in k}
                if directional and 'facing=up' not in directional:
                    base=directional.get('facing=north',next(iter(directional.values())))
                    directional['facing=up']={**base,'x':270};directional['facing=down']={**base,'x':90}
                state={'variants':directional or {'':next(iter(variants.values()))}}
        elif spec['kind']=='building' and name!='starbuncle_plush' and 'variants' in state:
            state={'variants':{'':next(iter(state['variants'].values()))}}
        if name == 'particle_block':
            write(assets/'models/nouveau/block/particle_block.json', {'textures':{'particle':'minecraft:block/glass'},'elements':[]})
        save_state(name,state)
        if name in INTERNAL or spec['kind'] in {'wall_sign','hanging_wall_sign','potted'}:
            item=assets/'items'/(spec['id']+'.json')
            if item.exists(): item.unlink()
            assignment=data/'spell_assignments'/(spec['id']+'.json')
            if assignment.exists(): assignment.unlink()
            write(data/'loot_table/blocks'/(spec['id']+'.json'),{'type':'minecraft:block','pools':[]})
        if spec['kind']=='station' and spec['geo'] or spec['kind']=='animated_decor':
            write(assets/'items'/(spec['id']+'.json'),{'model':{'type':'minecraft:special','base':model('item/'+name),'model':{'type':'geckolib:geckolib'}}})
    # Every non-Gecko block keeps its native item model. Old predicate overrides are replaced
    # by native components where applicable, rather than left as inert model metadata.
    for group in ('items','blocks'):
        for name,spec in CATALOG[group].items():
            if spec['kind']=='existing' or name in INTERNAL or spec['kind'] in {'wall_sign','hanging_wall_sign','potted'}:continue
            target=assets/'models/nouveau/item'/(name+'.json')
            if not target.exists():
                texture=ASSET/'textures/item'/(name+'.png')
                write(target,{'parent':'minecraft:item/generated','textures':{'layer0':model('item/'+name)}} if texture.exists() else {'parent':model('block/'+name)})
    empty={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.empty','texture_width':16,'texture_height':16,'visible_bounds_width':2,'visible_bounds_height':2,'visible_bounds_offset':[0,0,0]},'bones':[{'name':'root','pivot':[0,0,0]}]}]}
    for category in ('block','item','armor','entity'):
        write(assets/f'geckolib/models/{category}/nouveau/empty.geo.json',empty)
        write(assets/f'geckolib/animations/{category}/nouveau/empty.animation.json',{'format_version':'1.8.0','animations':{'idle':{'loop':True,'bones':{}}}})
    for name in ('source_jar','creative_source_jar','potion_jar'):
        original=read(ASSET/'models/item'/('source_jar.json' if name=='creative_source_jar' else name+'.json'))
        potion=name=='potion_jar'
        def baked(path):
            result={'type':'minecraft:model','model':asset_id(path)}
            if potion: result['tints']=[{'type':'minecraft:potion','default':-1}]
            return result
        write(assets/'items'/(id_of(name)+'.json'),{'model':{'type':'minecraft:range_dispatch','property':'jugcraft:nouveau_fill','potion':potion,'fallback':baked(original['parent']),'entries':[{'threshold':next(iter(v['predicate'].values())),'model':baked(v['model'])} for v in original.get('overrides',[])]}})
    for name,spec in CATALOG['items'].items():
        if spec['kind']=='potion' and not spec.get('geo'):
            write(assets/'items'/(spec['id']+'.json'),{'model':{'type':'minecraft:model','model':model('item/'+name),'tints':[{'type':'minecraft:potion','default':-1},{'type':'minecraft:constant','value':-1}]}})
    write(assets/'items/nouveau_dowsing_rod.json', {'model':{'type':'minecraft:range_dispatch','property':'minecraft:damage','normalize':True,'fallback':{'type':'minecraft:model','model':model('item/dowsing_rod_100')},'entries':[{'threshold':threshold,'model':{'type':'minecraft:model','model':model('item/dowsing_rod_'+str(stage))}} for threshold,stage in ((.25,75),(.5,50),(.75,25))]}})
    write(assets/'blockstates/nouveau_archwood_chest.json',{'variants':{'':{'model':model('block/archwood_chest')}}})
    # Reuse the native chest's closed geometry for the inventory icon, with the original UV atlas.
    def chest_cube(start,size,uv):
        x,y,z=size;u,v=uv
        faces={'north':[u+z,v+z,u+z+x,v+z+y],'south':[u+z+x+z,v+z,u+z+x+z+x,v+z+y],'west':[u,v+z,u+z,v+z+y],'east':[u+z+x,v+z,u+z+x+z,v+z+y],'up':[u+z,v,u+z+x,v+z],'down':[u+z+x,v,u+z+x+x,v+z]}
        return {'from':start,'to':[start[i]+size[i] for i in range(3)],'faces':{face:{'texture':'#chest','uv':[n/4 for n in coords]} for face,coords in faces.items()}}
    write(assets/'models/nouveau/item/archwood_chest.json',{'parent':'minecraft:block/block','textures':{'chest':'jugcraft:nouveau/entity/chest/archwood_chest','particle':'jugcraft:nouveau/block/archwood_planks'},'elements':[chest_cube([1,0,1],[14,10,14],[0,19]),chest_cube([1,10,1],[14,5,14],[0,0]),chest_cube([7,7,0],[2,4,1],[0,0])]})
    signs(write,assets)
    # Retired source models reference art that was renamed or removed from the supplied
    # collection. Bind those old references to current equivalents; originals stay intact.
    aliases={"item/amulet_of_environmental_protection":"jugcraft:nouveau/item/amulet_of_mana_regen",
             "item/ring_of_amplify":"jugcraft:nouveau/item/amplify",
             "item/ring_of_extend_time":"jugcraft:nouveau/item/extend_time",
             "item/novice_spell_book":"jugcraft:nouveau/item/spellbook_purple",
             "item/greater_discount_ring":"jugcraft:nouveau/item/ring_of_greater_discount",
             "item/lesser_discount_ring":"jugcraft:nouveau/item/ring_of_lesser_discount",
             "item/whelp_charm":"jugcraft:nouveau/item/bookwyrm_charm",
             "item/blaze_fiber":"jugcraft:nouveau/item/magebloom_fiber",
             "block/ars_jimbaux_gold":"minecraft:block/gold_block",
             "block/warding_stone":"jugcraft:block/warding_stone"}
    for path in (assets/"models/nouveau").rglob("*.json"):
        value=read(path);textures=value.get("textures",{});changed=False
        for key,ref in textures.items():
            old=ref.removeprefix("jugcraft:nouveau/")
            if old in aliases:textures[key]=aliases[old];changed=True
        if changed:write(path,value)


def signs(write,assets):
    """Native editable signs with the owner's original 64x32 entity UV layout."""
    from nouveau_content import id_of
    def box(start,end,uv,angle=0):
        value={'from':start,'to':end,'faces':{face:{'texture':'#all','uv':coords} for face,coords in uv.items()}}
        if angle:value['rotation']={'origin':[8,0,8],'axis':'y','angle':angle}
        return value
    board_uv={'north':[.5,1,6.5,7],'south':[7,1,13,7],'east':[0,1,.5,7],'west':[6.5,1,7,7],'up':[.5,0,6.5,1],'down':[6.5,0,12.5,1]}
    post_uv={'north':[.5,8,1,15],'east':[0,8,.5,15],'south':[1.5,8,2,15],'west':[1,8,1.5,15],'up':[.5,7,1,8],'down':[1,7,1.5,8]}
    for hanging in (False,True):
        name='archwood_hanging_sign' if hanging else 'archwood_sign'
        texture='jugcraft:nouveau/entity/signs/'+('hanging/' if hanging else '')+'archwood'
        variants={}
        for i in range(4):
            angle=i*22.5
            # Model rotations use a 45 degree state rotation for the final 67.5-degree case.
            actual=angle if angle<=45 else -22.5
            board=box([0,9.33333,7.33333],[16,17.33333,8.66667],board_uv,actual)
            elements=[board,box([7.33333,0,7.33333],[8.66667,9.33333,8.66667],post_uv,actual)]
            if hanging:
                board=box([1,0,7],[15,10,9],{'north':[.5,7,7.5,12],'south':[8.5,7,15.5,12],'east':[0,7,.5,12],'west':[7.5,7,8.5,12],'up':[.5,6,7.5,7],'down':[7.5,6,14.5,7]},actual)
                elements=[board]+[box([x,10,7.5],[x+1,16,8.5],post_uv,actual) for x in (3,12)]
            write(assets/'models/nouveau/block'/f'{name}_rot_{i}.json',{'textures':{'all':texture,'particle':'jugcraft:nouveau/block/archwood_planks'},'elements':elements})
        for rotation in range(16):variants[f'rotation={rotation}']={'model':f'jugcraft:nouveau/block/{name}_rot_{rotation%4}','y':((rotation//4)*90+(90 if rotation%4==3 else 0))%360}
        write(assets/'blockstates'/f'{id_of(name)}.json',{'variants':variants})
        wall='archwood_hanging_wall_sign' if hanging else 'archwood_wall_sign'
        # Wall signs share the same editable native text plane; the board is mounted at the back.
        geometry=[box([0,4.33333,14],[16,12.33333,15.33333],board_uv)]
        if hanging:geometry=[box([1,0,7],[15,10,9],board_uv),box([0,14,7],[16,16,9],post_uv)]+[box([x,10,7.5],[x+1,14,8.5],post_uv) for x in (3,12)]
        write(assets/'models/nouveau/block'/f'{wall}.json',{'textures':{'all':texture,'particle':'jugcraft:nouveau/block/archwood_planks'},'elements':geometry})
        write(assets/'blockstates'/f'{id_of(wall)}.json',{'variants':{'facing='+d:{'model':f'jugcraft:nouveau/block/{wall}','y':y} for d,y in {'north':0,'east':90,'south':180,'west':270}.items()}})
        write(assets/'models/nouveau/item'/f'{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:nouveau/item/'+name}})
