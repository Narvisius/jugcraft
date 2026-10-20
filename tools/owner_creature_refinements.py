"""Native shape/UV refinements after rendered review. Supplied PNGs stay byte-identical."""
from wildlife_geometry import cube


def refine(model,s):
    name=s['source'];bones=model['minecraft:geometry'][0]['bones'];w=s['width']*16;h=s['height']*16
    def part(n,o,size,parent='root',patch=None,role='fur',pivot=None):
        c=cube(o,size,role)
        if patch:c['_skin_patch']=patch
        b={'name':n,'parent':parent,'pivot':pivot or [o[0]+size[0]/2,o[1]+size[1],o[2]+size[2]/2],'cubes':[c]}
        bones.append(b);return b
    if name=='void_worm':
        # Separated, tapered segments and a recognizable open jaw replace the old solid rectangular silhouette.
        bones[:]=[{'name':'root','pivot':[0,0,0]}]
        blue=[31,33,6,10];rim=[29,3,7,9];dark=[116,42,5,8];tooth=[3,66,3,3]
        for i in range(7):
            r=w*(.32-i*.032);y=h*.48
            part('segment_'+str(i),[-r,y-r,w*(-.12+i*.37)],[r*2,r*2,w*.34],patch=blue,pivot=[0,y,w*(-.12+i*.37)])
            for side in [-1,1]:
                part(f'fin_{i}_{side}',[side*r-(r*.5 if side<0 else 0),y-r*.12,w*(i*.37)],[r*.5,r*.24,w*.18],parent='segment_'+str(i),patch=dark)
        part('head',[-w*.38,h*.2,-w*.62],[w*.76,h*.65,w*.46],patch=blue,pivot=[0,h*.52,-w*.2])
        part('mouth',[-w*.29,h*.29,-w*.65],[w*.58,h*.45,w*.025],parent='head',patch=dark)
        for side in [-1,1]:
            part('jaw_side_'+str(side),[side*w*.34-w*.055,h*.23,-w*.8],[w*.11,h*.57,w*.18],parent='head',patch=rim)
            part('eye_'+str(side),[side*w*.23-w*.06,h*.73,-w*.68],[w*.12,h*.09,w*.04],parent='head',patch=tooth)
            part('horn_'+str(side),[side*w*.3-w*.045,h*.8,-w*.4],[w*.09,h*.2,w*.12],parent='head',patch=dark)
            for row in (0,1):
                part(f'tooth_{side}_{row}',[side*w*.17-w*.035,h*(.3 if row==0 else .62),-w*.79],[w*.07,h*.13,w*.07],parent='head',patch=tooth)
        for row in (0,1):part('jaw_bar_'+str(row),[-w*.29,h*(.22 if row==0 else .76),-w*.8],[w*.58,h*.06,w*.18],parent='head',patch=rim)
        model['minecraft:geometry'][0]['description']['visible_bounds_width']=max(9,s['width']*4)
    elif name=='camel':
        # Long legs, raised neck, small muzzle and a tapered hump; the old hump was submerged in the torso.
        bones[:]=[{'name':'root','pivot':[0,0,0]}]
        part('body',[-w*.34,h*.38,-w*.55],[w*.68,h*.3,w*1.1])
        part('hump',[-w*.23,h*.66,-w*.15],[w*.46,h*.17,w*.48],'body')
        part('hump_top',[-w*.14,h*.81,-w*.07],[w*.28,h*.08,w*.3],'body')
        part('neck',[-w*.13,h*.57,-w*.7],[w*.26,h*.33,w*.25],'body')
        part('head',[-w*.17,h*.86,-w*.89],[w*.34,h*.14,w*.5],'neck')
        part('muzzle',[-w*.13,h*.86,-w*1.03],[w*.26,h*.07,w*.2],'head')
        for i in range(4):
            x=(-1 if i%2==0 else 1)*w*.23;z=(-1 if i//2==0 else 1)*w*.4
            part('leg_'+str(i),[x-w*.065,0,z-w*.065],[w*.13,h*.4,w*.13],pivot=[x,h*.4,z])
        for side in [-1,1]:
            part('ear_'+str(side),[side*w*.17-w*.035,h*.96,-w*.5],[w*.07,h*.04,w*.1],'head')
            part('eye_'+str(side),[side*w*.13-w*.025,h*.92,-w*.897],[w*.05,h*.025,.1],'head',role='dark')
        part('tail',[-w*.035,h*.36,w*.55],[w*.07,h*.28,w*.08],'body')
    elif name=='mutant_zombie':
        by={b['name']:b for b in bones};head=by['head'];head['cubes']=head['cubes'][:1]
        for b in bones:
            for c in b.get('cubes',[]):
                c['_skin_patch']=([27,45,11,10] if b['name'].startswith('leg') else [80,23,16,8] if b['name']=='head' else [16,16,24,28])
        head['cubes'][0]['_skin_faces']={'north':[8,74,16,16]}
        body=by['body']['cubes'][0];body['origin'][0]=-w*.43;body['size'][0]=w*.86
        for side in [-1,1]:
            arm=by['arm_'+str(side)];arm['rotation']=[-18,0,side*-8]
            c=arm['cubes'][0];c['_skin_faces']={'north':[80,23,16,8],'south':[80,23,16,8]}
            part('hand_'+str(side),[side*w*.48-w*.14,h*.12,-w*.15],[w*.28,h*.15,w*.3],arm['name'],patch=[80,23,12,8])
    elif name=='goblin_king':
        by={b['name']:b for b in bones};head=by['head'];head['cubes']=head['cubes'][:1]
        for b in bones:
            for c in b.get('cubes',[]):c['_skin_patch']=[9,28,14,5]
        head['cubes'][0]['_skin_patch']=[10,1,8,8]
        head['cubes'][0]['_skin_faces']={'north':[10,10,12,14]}
        for side in [-1,1]:
            part('ear_'+str(side),[side*w*.28-w*.08,h*.87,-w*.08],[w*.16,h*.07,w*.12],'head',patch=[10,1,8,8])
        part('nose',[-w*.07,h*.84,-w*.32],[w*.14,h*.07,w*.1],'head',patch=[10,1,8,8])
        part('crown_band',[-w*.28,h*.97,-w*.28],[w*.56,h*.055,w*.56],'head',patch=[41,6,3,3])
        for i in range(5):part('crown_point_'+str(i),[-w*.27+i*w*.12,h*1.02,-w*.28],[w*.06,h*.08,w*.06],'head',patch=[41,6,3,3])
    elif s['shape']=='theropod':
        head=next(b for b in bones if b['name']=='head');c=head['cubes'][0];x,y,z=c['origin'];a,b,d=c['size']
        for side in [-1,1]:
            part('eye_'+str(side),[x+(.16 if side<0 else .76)*a,y+b*.65,z-.08],[a*.08,b*.17,.12],'head',role='light')
            part('pupil_'+str(side),[x+(.18 if side<0 else .78)*a,y+b*.67,z-.13],[a*.035,b*.13,.08],'head',role='dark')
        part('lower_jaw',[x,y-b*.08,z],[a,b*.18,d*.9],'head',role='dark')
    return model


def animate(animation,s):
    if s['source']=='void_worm':
        for clip in ('idle','fly','swim'):
            animation['animations'][clip]={'loop':True,'animation_length':2,'bones':{
                'segment_'+str(i):{'rotation':[0,f'math.sin(query.anim_time * 150 - {i*28}) * 7',0],
                                  'position':[0,f'math.sin(query.anim_time * 150 - {i*28}) * 1.5',0]} for i in range(7)}}
