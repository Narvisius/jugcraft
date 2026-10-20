"""Reconstructed wildlife geometry using opaque regions of each supplied skin.

No bitmap is edited. These are new Jugcraft shapes, not recovered original models.
Each adult/baby variant gets UVs appropriate to its own atlas, including dye skins.
"""
from collections import Counter
from copy import deepcopy
from functools import lru_cache
from PIL import Image
from menagerie_content import ASSET, RES, ROOT, read, write


@lru_cache(maxsize=None)
def palette(texture):
    with Image.open(ASSET/'textures'/(texture+'.png')) as image:
        image=image.convert('RGBA');w,h=image.size;pixels=list(image.getdata())
    # Jellyfish and spectral skins deliberately use translucent pixels.
    # Preserve that alpha in the source texture instead of treating them as missing art.
    alpha=240 if any(p[3]>=240 for p in pixels) else max(p[3] for p in pixels)
    if alpha==0:raise ValueError('Empty atlas: '+texture)
    solid=[p for p in pixels if p[3]>=alpha]
    dominant=Counter(tuple(c//32 for c in p[:3]) for p in solid).most_common(1)[0][0]
    best=None
    for side in [8,6,4,2,1]:
        candidates=[]
        for y in range(0,h-side+1,max(1,side//2)):
            for x in range(0,w-side+1,max(1,side//2)):
                patch=[pixels[(y+dy)*w+x+dx] for dy in range(side) for dx in range(side)]
                if any(p[3]<alpha for p in patch):continue
                distance=sum(sum(abs(p[c]/32-dominant[c]-.5) for c in range(3)) for p in patch)/len(patch)
                candidates.append((distance,x,y,side))
        if candidates:
            best=min(candidates);break
    _,x,y,side=best
    opaque=[(sum(p[:3]),i%w,i//w) for i,p in enumerate(pixels) if p[3]>=alpha]
    dark=min(opaque);light=max(opaque)
    return w,h,(x,y,side,side),(dark[1],dark[2],1,1),(light[1],light[2],1,1)


def cube(origin,size,role='fur'):
    return {'origin':origin,'size':size,'_skin_role':role}


@lru_cache(maxsize=None)
def atlas_panels(texture):
    """Maximal opaque atlas rectangles; retain large authored markings instead of repeating a small swatch."""
    import numpy as np
    with Image.open(ASSET/'textures'/(texture+'.png')) as image:a=np.asarray(image.convert('RGBA'))
    alpha=240 if a[:,:,3].max()>=240 else int(a[:,:,3].max())
    heights=[0]*a.shape[1];rectangles=set()
    for y,row in enumerate(a[:,:,3]>=alpha):
        heights=[h+1 if on else 0 for h,on in zip(heights,row)];stack=[]
        for x,height in enumerate(heights+[0]):
            start=x
            while stack and stack[-1][1]>height:
                start,h=stack.pop();rectangles.add((start,y-h+1,x-start,h))
            if not stack or stack[-1][1]<height:stack.append((start,height))
    return sorted(rectangles,key=lambda r:r[2]*r[3],reverse=True)[:128]


@lru_cache(maxsize=8192)
def surface_patch(texture, ratio):
    best=None
    for x,y,w,h in atlas_panels(texture):
        crop_w=min(w,max(1,round(h*ratio)));crop_h=min(h,max(1,round(w/ratio)))
        area=crop_w*crop_h
        candidate=(area, min(crop_w,crop_h),-y,-x,x+(w-crop_w)//2,y+(h-crop_h)//2,crop_w,crop_h)
        if best is None or candidate>best:best=candidate
    return best[-4:]


def refine_shape(model,species):
    bones=model['minecraft:geometry'][0]['bones'];name=species['source'];family=species['shape']
    by_name={b['name']:b for b in bones};head=by_name.get('head')
    w=species['width']*16;h=species['height']*16
    def part(n,o,size,parent='root',role='fur',rotation=None):
        b={'name':n,'parent':parent,'pivot':[o[0]+size[0]/2,o[1]+size[1],o[2]+size[2]/2],'cubes':[cube(o,size,role)]}
        if rotation:b['rotation']=rotation
        bones.append(b);return b
    if family=='ostrich':
        by_name['body']['cubes']=[cube([-w*.4,h*.35,-w*.55],[w*.8,h*.28,w*1.1])]
        part('neck',[-w*.065,h*.59,-w*.48],[w*.13,h*.29,w*.15])
        head['cubes']=[cube([-w*.14,h*.85,-w*.65],[w*.28,h*.12,w*.32])];head['pivot']=[0,h*.85,-w*.48]
        by_name['beak']['cubes']=[cube([-w*.09,h*.86,-w*.85],[w*.18,h*.04,w*.23])]
        for sign in (-1,1):
            by_name['leg_'+str(sign)]['cubes']=[cube([sign*w*.2-w*.04,0,-w*.05],[w*.08,h*.36,w*.12])]
            part('foot_'+str(sign),[sign*w*.2-w*.09,0,-w*.23],[w*.18,h*.035,w*.3],parent='leg_'+str(sign))
    if family=='reptile':
        head['cubes']=[cube([-w*.28,h*.26,-w*1.25],[w*.56,h*.23,w*.75])]
        part('jaw',[-w*.25,h*.21,-w*1.27],[w*.5,h*.08,w*.72],parent='head')
        bones[:]=[b for b in bones if b['name']!='tail']
        for i in range(4):part('tail_'+str(i),[-w*(.2-i*.04),h*.22,w*(.6+i*.4)],[w*(.4-i*.08),h*(.24-i*.04),w*.45])
        if name in ('alligator','crocodile','caiman'):
            for i in range(5):part('scute_'+str(i),[-w*.24,h*.64,-w*.65+i*w*.3],[w*.48,h*.1,w*.12])
    if family in ('fish','shark','whale'):
        tail=by_name['tail'];tail['cubes']=[cube([-w*.12,h*.3,w*.75],[w*.24,h*.35,w*.5])]
        if family=='whale':part('fluke',[-w*.65,h*.36,w*1.05],[w*1.3,h*.12,w*.4],parent='tail')
        else:part('tail_fin',[-w*.045,h*.08,w*1.05],[w*.09,h*.85,w*.4],parent='tail')
        part('fish_head',[-w*.38,h*.24,-w*1.12],[w*.76,h*.56,w*.45],parent='body')
        for side in (-1,1):part('fish_eye_'+str(side),[side*w*.38-w*.025,h*.59,-w*.9],[w*.05,h*.09,w*.09],parent='fish_head',role='dark')
    if family=='ray':
        by_name['body']['cubes']=[cube([-w*.4,h*.25,-w*.7],[w*.8,h*.45,w*1.25])]
        for side in (-1,1):
            by_name['fin_'+str(side)]['cubes']=[cube([side*w*.4-(w*.85 if side<0 else 0),h*.35,-w*.45],[w*.85,h*.13,w*.8])]
        by_name['tail']['cubes']=[cube([-w*.04,h*.35,w*.55],[w*.08,h*.08,w*1.8])]
    if family=='elephant':
        for side in (-1,1):part('tusk_'+str(side),[side*w*.22-w*.045,h*.28,-w*1.08],[w*.09,h*.22,w*.43],parent='head',role='light',rotation=[-20,0,0])
    if name in ('tortoise','snapping_turtle'):
        by_name['body']['cubes']=[cube([-w*.48,h*.18,-w*.6],[w*.96,h*.43,w*1.2]),cube([-w*.35,h*.58,-w*.45],[w*.7,h*.2,w*.9])]
    if name=='hedgehog':
        by_name['body']['cubes'].append(cube([-w*.38,h*.58,-w*.38],[w*.76,h*.3,w*.8]))
    if name=='giraffe':
        by_name['body']['cubes']=[cube([-7,20,-11],[14,14,22])]
        by_name['neck']['cubes']=[cube([-3,32,-13],[6,20,6])]
        head['cubes']=[cube([-4,50,-20],[8,6,12])];head['pivot']=[0,51,-12]
        for i in range(4):
            x=(-1 if i%2==0 else 1)*5;z=(-1 if i//2==0 else 1)*8
            by_name['leg_'+str(i)].update(pivot=[x,21,z],cubes=[cube([x-1.5,0,z-1.5],[3,21,3])])
        for sign in [-1,1]:
            by_name['horn_'+str(sign)].update(pivot=[sign*2,56,-13],cubes=[cube([sign*2-.5,55.5,-13],[1,3.5,1])])
    if name=='rhino':
        bones[:]=[b for b in bones if not b['name'].startswith('horn')]
        x,y,z=head['cubes'][0]['origin'];w,h,d=head['cubes'][0]['size']
        head['cubes'].extend([cube([x+w*.43,y+h*.75,z-.2],[w*.14,h*.65,w*.14]),cube([x+w*.45,y+h*.9,z+d*.32],[w*.1,h*.3,w*.1])])
    if family in {'crab','scorpion','insect'} and name!='firefly':
        bones[:]=[b for b in bones if not b['name'].startswith(('wing','leg'))]
        w=species['width']*16;h=species['height']*16
        for side in [-1,1]:
            for i in range(4 if family in {'crab','scorpion'} else 3):
                x=side*w*.4;z=(i-1.5)*w*.25
                bones.append({'name':f'leg_{side}_{i}','parent':'root','pivot':[x,h*.3,z],
                              'cubes':[cube([x-(w*.5 if side<0 else 0),h*.08,z],[w*.5,max(.35,h*.12),max(.4,w*.08)])]})
    if name=='clam':
        w=species['width']*16;h=species['height']*16
        bones[:]=[{'name':'root','pivot':[0,0,0]},
                  {'name':'body','parent':'root','pivot':[0,h/2,0],'cubes':[cube([-w/2,0,-w/2],[w,h*.45,w])]},
                  {'name':'lid','parent':'root','pivot':[0,h/2,w/2],'cubes':[cube([-w/2,h*.52,-w/2],[w,h*.48,w])]}]
        head=None
    if name=='snail':
        bones[:]=[b for b in bones if not b['name'].startswith('leg')]
        w=species['width']*16;h=species['height']*16
        by_name['body']['cubes']=[cube([-w*.42,h*.25,-w*.15],[w*.84,h*.75,w*.9])]
        bones.append({'name':'foot','parent':'root','pivot':[0,0,0],'cubes':[cube([-w*.35,0,-w*.85],[w*.7,h*.2,w*1.6])]})
    if head:
        origin=head['cubes'][0]['origin'];size=head['cubes'][0]['size'];x,y,z=origin;w,h,d=size
        # Small inset eyes, constructed with existing opaque dark/light texels.
        eye=max(.35,w*.12)
        for sign in [-1,1]:
            ex=x+w*(.23 if sign<0 else .77)-eye/2
            head['cubes'].append(cube([ex,y+h*.6,z-.05],[eye,eye,.08],'dark'))
            head['cubes'].append(cube([ex+eye*.12,y+h*.6+eye*.55,z-.09],[eye*.28,eye*.28,.05],'light'))
        if family in {'bear','cat','rodent','horse','boar','deer','giraffe'}:
            for sign in [-1,1]:
                ex=x+w*(.05 if sign<0 else .73)
                head['cubes'].append(cube([ex,y+h-.05,z+d*.4],[w*.22,h*.28,d*.28]))
            head['cubes'].append(cube([x+w*.25,y+h*.12,z-d*.22],[w*.5,h*.3,d*.24]))
            head['cubes'].append(cube([x+w*.4,y+h*.3,z-d*.23],[w*.2,h*.12,.08],'dark'))
        if name=='snail':
            for sign in [-1,1]:head['cubes'].append(cube([x+w*(.15 if sign<0 else .75),y+h*.8,z],[w*.1,h*.8,w*.1],'dark'))
    # Keep the new silhouette within the species' declared height.
    top=max(c['origin'][1]+c['size'][1] for b in bones for c in b.get('cubes',[]))
    factor=species['height']*16/top
    for b in bones:
        b['pivot'][1]*=factor
        for c in b.get('cubes',[]):c['origin'][1]*=factor;c['size'][1]*=factor


def apply_skin(model,texture):
    w,h,fur,dark,light=palette(texture)
    geo=model['minecraft:geometry'][0];geo['description']['texture_width']=w;geo['description']['texture_height']=h
    for bone in geo['bones']:
        for c in bone.get('cubes',[]):
            role=c.pop('_skin_role','fur')
            explicit='_skin_patch' in c
            patch=c.pop('_skin_patch',{'fur':fur,'dark':dark,'light':light}[role])
            x,y,width,height=patch
            c['uv']={face:{'uv':[x,y],'uv_size':[width,height]} for face in ['north','east','south','west','up','down']}
            if role=='fur' and not explicit:
                sx,sy,sz=c['size']
                for face,a,b in [('north',sx,sy),('south',sx,sy),('east',sz,sy),('west',sz,sy),('up',sx,sz),('down',sx,sz)]:
                    x,y,width,height=surface_patch(texture,round(max(.15,min(6,a/max(.01,b))),2))
                    c['uv'][face]={'uv':[x,y],'uv_size':[width,height]}
            for face,rect in c.pop('_skin_faces',{}).items():
                x,y,width,height=rect;c['uv'][face]={'uv':[x,y],'uv_size':[width,height]}
    return model


def refine_animations(animation,model):
    names={b['name'] for b in model['minecraft:geometry'][0]['bones']}
    for clip in animation['animations'].values():
        clip['bones']={k:v for k,v in clip.get('bones',{}).items() if k in names}
    for i,name in enumerate(sorted(names)):
        if name.startswith('tail_'):
            for clip,speed,amplitude in [('idle',90,3),('walk',240,8),('swim',240,16)]:
                animation['animations'][clip]['bones'][name]={'rotation':[0,f'math.sin(query.anim_time * {speed} - {i*24}) * {amplitude}',0]}


def adapt(catalog):
    count=0
    # Native layers recovered from the owner's October 19 model exports take
    # precedence. Never rebuild their anatomy or replace their authored UVs.
    native_bindings=ASSET/'wildlife_models/bindings.json'
    recovered=read(native_bindings) if native_bindings.exists() else {}
    for species in catalog['species']:
        if not species['id'].startswith('wildlife_'):continue
        if species['id'] in recovered:continue
        from menagerie_content import animal_geometry
        animal_geometry(species)
        base=read(ASSET/f"geckolib/models/entity/menagerie/{species['id']}.geo.json")
        refine_shape(base,species)
        animation_path=ASSET/f"geckolib/animations/entity/menagerie/{species['model']}.animation.json"
        animation=read(animation_path)
        refine_animations(animation,base)
        walk=animation['animations']['walk']['bones']
        for i,bone in enumerate(base['minecraft:geometry'][0]['bones']):
            if bone['name'].startswith('leg_'):
                walk[bone['name']]={'rotation':[f'math.sin(query.anim_time * 360 + {180 if i%2 else 0}) * 18',0,0]}
        write(animation_path,animation)
        # Legacy *_baby dye entries remain as saved aliases, but ordinary dyeing
        # selects the adult skin and its matching baby rather than mixing atlases.
        for variant in species['variants']:
            texture=variant['texture']
            if '/dye/' in texture and not texture.endswith('_baby'):
                baby=texture+'_baby'
                if (ASSET/'textures'/(baby+'.png')).exists():variant['babyTexture']=baby
            for field,skin in [('model','texture'),('babyModel','babyTexture')]:
                model_name=f"wildlife/{species['source']}/{variant['name']}_{field}"
                model=apply_skin(deepcopy(base),variant[skin]);model['minecraft:geometry'][0]['description']['identifier']='geometry.'+model_name.replace('/','.')
                write(ASSET/f'geckolib/models/entity/menagerie/{model_name}.geo.json',model);variant[field]=model_name;count+=1
    return count


def main():
    catalog=read(RES/'jugcraft/menagerie/catalog.json');count=adapt(catalog);write(RES/'jugcraft/menagerie/catalog.json',catalog)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18/wildlife-geometry-adaptation.json',{
        'runtimeModels':count,'originalBitmapsChanged':False,'scope':'Reconstructed silhouettes with atlas-specific opaque UV patches; new eyes/ears use supplied palette texels. These are not original mod geometry.',
        'remaining':'Individual anatomical/artistic refinement and all-variant visual acceptance.',
        'supersededBy':'../naturalist-models-2026-10-19/recovery.json: recovered wildlife use native owner model layers; this generator skips them.'})
    print('Mapped',count,'wildlife adult/baby models without editing source textures.')


if __name__=='__main__':main()
