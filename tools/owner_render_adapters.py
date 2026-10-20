"""Native model/particle adapters for supplied renderer-only resources. PNGs remain unchanged."""
import copy
from menagerie_content import ROOT,RES,ASSET,DATA,read,write


def box(start,end,uv=(0,0),tex='all',atlas=(64,64),skin_size=None):
    w,h,d=skin_size or tuple(end[i]-start[i] for i in range(3));u,v=uv;sx,sy=16/atlas[0],16/atlas[1]
    rects={'west':(u,v+d,u+d,v+d+h),'north':(u+d,v+d,u+d+w,v+d+h),'east':(u+d+w,v+d,u+2*d+w,v+d+h),'south':(u+2*d+w,v+d,u+2*d+2*w,v+d+h),'up':(u+d,v,u+d+w,v+d),'down':(u+d+w,v,u+d+2*w,v+d)}
    return {'from':start,'to':end,'faces':{f:{'texture':'#'+tex,'uv':[a*sx,b*sy,c*sx,e*sy]} for f,(a,b,c,e) in rects.items()}}


def model(group,name,texture,elements,extra=None):
    path=f'menagerie/{group}/block/{name}';write(ASSET/f'models/{path}.json',{'parent':'minecraft:block/block','textures':{'all':f'jugcraft:menagerie/{group}/{texture}','particle':f'jugcraft:menagerie/{group}/{texture}',**(extra or {})},'elements':elements})
    return 'jugcraft:'+path


def legacy_signs():
    for wood in ('pewen','thornwood'):
        for hanging in (False,True):
            stem=wood+('_hanging_sign' if hanging else '_sign');texture='entity/signs/'+('hanging/' if hanging else '')+wood
            board=box([1,0,7],[15,10,9],(0,12),atlas=(64,32),skin_size=(14,10,2)) if hanging else box([0,9.333,7.333],[16,17.333,8.667],atlas=(64,32),skin_size=(24,12,2))
            extras=[box([3,10,7.5],[4,16,8.5],(0,0),atlas=(64,32)),box([12,10,7.5],[13,16,8.5],(0,0),atlas=(64,32))] if hanging else [box([7.333,0,7.333],[8.667,9.333,8.667],(0,14),atlas=(64,32),skin_size=(2,14,2))]
            variants={}
            for rotation in range(16):
                angle=(rotation%4)*22.5;y=(rotation//4)*90
                if angle>45:angle-=90;y=(y+90)%360
                elements=copy.deepcopy([board]+extras)
                if angle:
                    for e in elements:e['rotation']={'origin':[8,0,8],'axis':'y','angle':angle}
                ref=model('cavern',stem+f'_native_{rotation}',texture,elements)
                variants['rotation='+str(rotation)]={'model':ref,'y':y}
            write(ASSET/f'blockstates/cavern_{stem}.json',{'variants':variants})
            wall=copy.deepcopy(board)
            for axis in ('from','to'):wall[axis][2]+=6;wall[axis][1]+=4 if hanging else -4
            elements=[wall]+([box([1,14,12],[15,16,16],(0,0),atlas=(64,32))] if hanging else [])
            ref=model('cavern',wood+('_wall_hanging_sign' if hanging else '_wall_sign')+'_native',texture,elements)
            write(ASSET/f'blockstates/cavern_{wood}{"_wall_hanging_sign" if hanging else "_wall_sign"}.json',{'variants':{f'facing={f}':{'model':ref,'y':y} for f,y in [('north',0),('east',90),('south',180),('west',270)]}})


def blocks():
    shapes={
        'conversion_crucible': [box([2,0,2],[5,4,5],(0,32)),box([11,0,2],[14,4,5],(0,32)),box([2,0,11],[5,4,14],(0,32)),box([11,0,11],[14,4,14],(0,32)),box([1,4,1],[15,7,15]),box([1,7,1],[3,15,15]),box([13,7,1],[15,15,15]),box([3,7,1],[13,15,3]),box([3,7,13],[13,15,15])],
        'beholder':[box([3,0,3],[13,3,13],(0,32)),box([6,3,6],[10,9,10],(0,16)),box([2,9,3],[14,16,13]),box([5,10,2],[11,15,3],(0,0),'eye')],
        'copper_valve':[box([5,0,5],[11,7,11]),box([3,7,3],[13,9,5],(0,16)),box([3,7,11],[13,9,13],(0,16)),box([3,7,5],[5,9,11],(0,16)),box([11,7,5],[13,9,11],(0,16)),box([7,7,5],[9,9,11],(0,24))],
        'siren_light':[box([2,0,2],[14,3,14],(0,32)),box([4,3,4],[12,10,12]),box([5,10,5],[11,12,11])],
        'gobthumper':[box([4,0,4],[12,3,12],atlas=(32,32)),box([6,3,6],[10,11,10],(0,16),atlas=(32,32)),box([2,11,2],[14,15,14],atlas=(32,32))]
    }
    catalog=read(RES/'jugcraft/menagerie/cavern.json')
    for name,elements in shapes.items():
        model('cavern',name,'entity/'+name,elements,{'eye':'jugcraft:menagerie/cavern/entity/beholder_eye'})
        definition=catalog['blocks']['cavern_'+name]['definition'];definition['shapes']={'':[[0,0,0,16,16,16]]}
    write(ASSET/'models/menagerie/cavern/block/active_nuclear_furnace.json',{'parent':'jugcraft:menagerie/cavern/block/nuclear_furnace_component'})
    # Old active-only models were empty entity-render hooks; reuse the supplied inactive mesh.
    for target,source in [('active_magnet','azure_magnet'),('active_nuclear_furnace','nuclear_furnace')]:
        path=ASSET/f'models/menagerie/cavern/block/{source}.json'
        if path.exists():write(ASSET/f'models/menagerie/cavern/block/{target}.json',read(path))
    write(RES/'jugcraft/menagerie/cavern.json',catalog)
    beak=[box([2,0,2],[14,3,14],(0,0)),box([4,3,5],[12,11,11],(0,16)),box([6,11,6],[10,16,10],(0,32))]
    model('beast','void_worm_beak','entity/void_worm/void_worm_beak',beak)


def skull():
    # Native skull placement and equipment, with the supplied bone armor atlas rather than a vanilla skull renderer.
    texture='entity/equipment/humanoid/mutant_skeleton'
    elements=[box([4,0,4],[12,8,12],(0,0),atlas=(64,32))]
    base=model('mutant','mutant_skeleton_skull',texture,elements)
    variants={}
    for i in range(16):
        angle=i%4*22.5;y=i//4*90
        if angle>45:angle-=90;y=(y+90)%360
        parts=copy.deepcopy(elements)
        if angle:parts[0]['rotation']={'origin':[8,0,8],'axis':'y','angle':angle}
        ref=model('mutant',f'skull_native_{i}',texture,parts);variants[f'rotation={i}']={'model':ref,'y':y}
    write(ASSET/'blockstates/mutant_mutant_skeleton_skull.json',{'variants':variants})
    wall=copy.deepcopy(elements)
    for e in wall:
        for v in ('from','to'):e[v][1]+=4;e[v][2]+=4
    ref=model('mutant','mutant_skeleton_wall_skull',texture,wall)
    write(ASSET/'blockstates/mutant_mutant_skeleton_wall_skull.json',{'variants':{f'facing={f}':{'model':ref,'y':y} for f,y in [('north',0),('east',90),('south',180),('west',270)]}})
    write(ASSET/'items/mutant_mutant_skeleton_skull.json',{'model':{'type':'minecraft:model','model':base}})


def particles():
    ids=[]
    collections={'beast':('jims mobs','alexsmobs'),'cavern':('jims caves','alexscaves'),'mutant':('mutations','mutantmonsters')}
    base=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2'
    for group,(folder,namespace) in collections.items():
        for source in (base/folder/'particles').glob('*.json'):
            data=read(source);textures=[]
            for value in data.get('textures',[]):
                if value.startswith(namespace+':'):value='jugcraft:menagerie/'+group+'/'+value.split(':',1)[1]
                textures.append(value)
            if textures:
                id=group+'_'+source.stem;write(ASSET/f'particles/{id}.json',{'textures':textures});ids.append(id)
    write(RES/'jugcraft/menagerie/particles.json',ids)


def compatibility_aliases():
    write(ASSET/'models/menagerie/cavern/block/thornwood_leaves.json',{'parent':'jugcraft:menagerie/cavern/block/thornwood_branch'})
    write(ASSET/'models/menagerie/ecology/block/maple_leaves.json',{'parent':'minecraft:block/leaves','textures':{'all':'jugcraft:menagerie/ecology/block/green_maple_leaves'}})
    write(ASSET/'models/menagerie/ecology/item/walnut_slice.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:menagerie/ecology/item/walnut'}})
    write(ASSET/'models/menagerie/landscape/block/bush.json',{'parent':'minecraft:block/bush'})
    for species in read(RES/'jugcraft/menagerie/mutant.json')['species']:
        write(ASSET/f"geckolib/models/entity/menagerie/{species['model']}.geo.json",read(ASSET/f"geckolib/models/entity/menagerie/{species['variants'][0]['model']}.geo.json"))


def opaque_atlas_faces(elements,texture):
    # Missing source renderer UVs must not leave transparent holes in solid native parts.
    from PIL import Image
    from wildlife_geometry import surface_patch
    with Image.open(ASSET/'textures'/(texture+'.png')) as image:
        image=image.convert('RGBA');w,h=image.size
        for element in elements:
            for face,spec in element['faces'].items():
                uv=spec['uv'];bounds=(max(0,int(min(uv[0],uv[2])*w/16)),max(0,int(min(uv[1],uv[3])*h/16)),min(w,int(max(uv[0],uv[2])*w/16)),min(h,int(max(uv[1],uv[3])*h/16)))
                region=image.crop(bounds) if bounds[2]>bounds[0] and bounds[3]>bounds[1] else None
                if region is None or region.getchannel('A').getextrema()[0]<240:
                    ratio=max(.1,abs(uv[2]-uv[0])*w/max(.001,abs(uv[3]-uv[1])*h))
                    x,y,pw,ph=surface_patch(texture,round(ratio,3));spec['uv']=[x*16/w,y*16/h,(x+pw)*16/w,(y+ph)*16/h]


def renderer_only_items():
    """Reconstruct two missing renderer shapes from supplied atlases; no fabricated bitmap icons."""
    import model_writer as mw
    # A vertical ship wheel: rim, spokes, hub and a supported control pedestal.
    wheel=[]
    def beam(lo,hi,angle=0):
        e=box(lo,hi,(0,32),atlas=(64,64))
        if angle:e['rotation']={'origin':[8,10,8],'axis':'z','angle':angle}
        wheel.append(e)
    beam([6,0,6],[10,3,10]);beam([7,3,8],[9,10,10])
    for angle in (0,45,-45):beam([7.5,3,7],[8.5,17,9],angle)
    beam([1,9.5,7],[15,10.5,9])
    for lo,hi in [([3,4,7],[13,5,9]),([3,15,7],[13,16,9]),([2,5,7],[3,15,9]),([13,5,7],[14,15,9]),([6.5,8.5,6.5],[9.5,11.5,9.5])]:beam(lo,hi)
    mw.finish_closed(wheel)
    opaque_atlas_faces(wheel,'menagerie/beast/entity/end_pirate/ship_wheel')
    ref=model('beast','end_pirate_ship_wheel_native','entity/end_pirate/ship_wheel',wheel)
    write(ASSET/'blockstates/beast_end_pirate_ship_wheel.json',{'variants':{'':{'model':ref}}})
    write(ASSET/'models/menagerie/beast/item/end_pirate_ship_wheel.json',{'parent':ref})
    # Chunky candy launcher: red chassis/grip, metal muzzle, and gumball hopper.
    from PIL import Image
    atlas=Image.open(ASSET/'textures/menagerie/cavern/entity/shot_gum.png').size
    pieces=[box([5,7,3],[11,11,13],(0,0),atlas=atlas),
            box([6,3,10],[10,7,13],(0,48),atlas=atlas),
            box([6,8,0],[10,10,3],(0,16),atlas=atlas),
            box([5,11,6],[11,16,12],(32,0),atlas=atlas),
            box([5,16,6],[11,17,12],(0,48),atlas=atlas),
            box([6,6,5],[7,7,10],(0,16),atlas=atlas),
            box([9,6,5],[10,7,10],(0,16),atlas=atlas)]
    mw.finish_closed(pieces)
    opaque_atlas_faces(pieces,'menagerie/cavern/entity/shot_gum')
    # Keep candy only in the hopper. The authored red and metal panels belong to the chassis.
    for index,part in enumerate(pieces):
        if index==3:continue
        rect=(52,44,60,52) if index in (0,1,4) else (12,20,24,28)
        for face in part['faces'].values():face['uv']=[rect[0]*16/atlas[0],rect[1]*16/atlas[1],rect[2]*16/atlas[0],rect[3]*16/atlas[1]]
    data={'parent':'minecraft:block/block','textures':{'all':'jugcraft:menagerie/cavern/entity/shot_gum','particle':'jugcraft:menagerie/cavern/entity/shot_gum'},'elements':pieces,
          'display':{'gui':{'rotation':[20,145,0],'scale':[.8,.8,.8]},'firstperson_righthand':{'rotation':[0,-90,0],'translation':[1,0,-2],'scale':[.6,.6,.6]},'firstperson_lefthand':{'rotation':[0,90,0],'translation':[1,0,-2],'scale':[.6,.6,.6]},'thirdperson_righthand':{'rotation':[0,-90,0],'translation':[0,1,1],'scale':[.6,.6,.6]},'thirdperson_lefthand':{'rotation':[0,90,0],'translation':[0,1,1],'scale':[.6,.6,.6]}}}
    write(ASSET/'models/menagerie/cavern/item/shot_gum.json',data)
    def shape(group,name,texture,parts,block=False):
        atlas=Image.open(ASSET/f'textures/menagerie/{group}/{texture}.png').size
        elements=[box(lo,hi,atlas=atlas) for lo,hi in parts]
        mw.finish_closed(elements);opaque_atlas_faces(elements,f'menagerie/{group}/{texture}')
        ref=model(group,name+'_native',texture,elements)
        write(ASSET/f'models/menagerie/{group}/item/{name}.json',{'parent':ref})
        if block:write(ASSET/f'blockstates/{group}_{name}.json',{'variants':{'':{'model':ref}}})
    shape('beast','end_pirate_anchor','entity/end_pirate/anchor',[
        ([7,2,7],[9,15,9]),([2,4,7],[14,6,9]),([1,5,7],[3,10,9]),([13,5,7],[15,10,9]),([5,13,7],[11,15,9])])
    shape('beast','end_pirate_anchor_winch','entity/end_pirate/anchor_winch',[
        ([1,0,1],[15,3,15]),([2,3,3],[4,13,13]),([12,3,3],[14,13,13]),([4,5,4],[12,11,12]),([4,11,7],[12,13,9])],True)
    shape('beast','transmutation_table','entity/farseer/transmutation_table',[
        ([1,12,1],[15,15,15]),([3,0,3],[5,12,5]),([11,0,3],[13,12,5]),([3,0,11],[5,12,13]),([11,0,11],[13,12,13]),([5,9,5],[11,12,11])],True)
    shape('beast','shield_of_the_deep','armor/shield_of_the_deep',[
        ([3,3,7],[13,14,9]),([5,1,7],[11,3,9]),([6,6,9],[10,10,11]),([2,5,7],[3,12,9]),([13,5,7],[14,12,9])])
    write(ASSET/'models/menagerie/beast/item/shield_of_the_deep_blocking.json',{'parent':'jugcraft:menagerie/beast/item/shield_of_the_deep'})
    for name in ('beholder','copper_valve','gobthumper','siren_light'):
        write(ASSET/f'models/menagerie/cavern/item/{name}.json',{'parent':f'jugcraft:menagerie/cavern/block/{name}'})
    shape('cavern','galena_gauntlet','entity/galena_gauntlet',[
        ([4,1,5],[12,7,13]),([3,7,4],[13,12,13]),([2,7,5],[4,10,11]),([4,12,4],[6,14,13]),([6,12,4],[8,14,13]),([8,12,4],[10,14,13]),([10,12,4],[12,14,13])])
    shape('cavern','resistor_shield','entity/resistor_shield',[
        ([3,2,7],[13,14,9]),([5,0,7],[11,2,9]),([6,6,6],[10,10,10]),([1,5,7],[3,11,9]),([13,5,7],[15,11,9])])
    shape('cavern','raygun','entity/raygun/raygun',[
        ([5,7,4],[11,12,13]),([6,3,10],[10,7,13]),([6,8,0],[10,11,4]),([4,8,2],[12,11,3]),([6,12,7],[10,14,11])])
    write(ASSET/'models/menagerie/beast/item/tab_icon.json',{'parent':'jugcraft:menagerie/beast/item/transmutation_table'})



def main():
    legacy_signs();blocks();skull();particles();compatibility_aliases();renderer_only_items()
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18-part2/render-adapters.json',{'description':'New native block geometry and legacy sign UV layouts. Supplied PNG files are unchanged. Source renderer code was not included.','validation':'Deferred until implementation is complete.'})


if __name__=='__main__':main()
