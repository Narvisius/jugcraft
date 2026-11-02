"""Audited placeable foods. Uses original textures; never edits source art."""
import json, io, zipfile, copy
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
VANILLA=Path.home()/'.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
FACES=('up','down','north','south','east','west')
def read_asset(ns,path):
    p=RES/'assets'/ns/path
    if p.exists(): return p.read_bytes()
    if ns=='minecraft':
        with zipfile.ZipFile(VANILLA) as z:return z.read('assets/minecraft/'+path)
    raise FileNotFoundError(p)
def model(ref, seen=()):
    if ref in seen:raise ValueError(ref)
    ns,p=ref.split(':') if ':' in ref else ('minecraft',ref)
    d=json.loads(read_asset(ns,'models/'+p+'.json'))
    parent=d.get('parent','')
    if parent and parent not in ('minecraft:item/generated','item/generated','minecraft:item/handheld','item/handheld','builtin/generated','minecraft:builtin/generated'):
        base=model(parent,seen+(ref,)); base.update({k:v for k,v in d.items() if k!='textures'});base['textures']={**base.get('textures',{}),**d.get('textures',{})};d=base
    return d

def reference(node):
    if isinstance(node,dict):
        if isinstance(node.get('model'),str):return node['model']
        if isinstance(node.get('base'),str):return node['base']
        for v in node.values():
            r=reference(v)
            if r:return r
    if isinstance(node,list):
        for v in node:
            r=reference(v)
            if r:return r

def source(item):
    ns,p=item.split(':'); ref=reference(json.loads(read_asset(ns,'items/'+p+'.json')))
    if not ref:raise ValueError('No model: '+item)
    d=model(ref); tex=d.get('textures',{}); t=tex.get('layer0') or tex.get('particle') or next(iter(tex.values()))
    while t.startswith('#'):t=tex[t[1:]]
    n,p=t.split(':') if ':' in t else ('minecraft',t)
    im=Image.open(io.BytesIO(read_asset(n,'textures/'+p+'.png'))).convert('RGBA');im=im.crop((0,0,im.width,min(im.height,im.width)))
    return ref,d,t,im

def classify(n):
    # Ordered families, frozen into the reviewed catalog; unknowns are fatal.
    rules=[('bucket',('bucket',)),('bottle',('bottle','cider','nog','latte','punch','cola','tin_can','tea','syrup','fish_oil','slime','jam','tincan')),
    ('bowl',('soup','stew','salad','porridge','chili','rice','stuffing','sauce','sopa_','soy lent','soylent','bowl_of','nachos','slam','biome_treat','alexs_meal','alex_meal','sauerkraut','pasta','custard','tempura','repellent')),
    ('pizza',('pizza',)),('sandwich',('sandwich','toastie','burger','toast','bread_slice')),
    ('slice',('slice','tart','pie')),('cake',('cake','cupcake','pan_de_muerto','pudding')),
    ('cookie',('cookie','donut','biscuit','pancake','waffle','spelunkie')),('bread',('bread','roll','dough')),
    ('cane',('candy_cane','licorice')),('lollipop',('lollipop',)),('candy',('candy','caramel','taffy','fudge','toffee','gum','jelly','marshmallow','meringue','popcorn','fizz','sugar','ice_cream','sundae','gingerbread','sweetish')),
    ('fish',('fish','salmon','cod','trilocaris','bayad','boulti','capitaine','synodontis','halibut','grouper','tuna','trout','perch','bass','gar','carp','arapaima','tambaqui','piranha','pollock','herring','mackerel','bluegill','muskellunge','sardine','jellyfish','puffer','shrooma','sushi','fillet','lobster','shrimp','crab','mussel','squid','tentacle','radgill','catfish','flounder')),
    ('meat',('meat','steak','beef','pork','mutton','chicken','rabbit','turkey','venison','rib','bacon','sausage','nugget','tail','morsel','flesh','wing','kangaroo','emu','fat','drumstick','duck','egg','eye','maggot')),
    ('leaf',('kelp','algae','lettuce','cabbage','herb','spinach','gongylidia')),
    ('root',('carrot','potato','turnip','radish','beetroot','onion','corn','finger')),
    ('berry',('berries','berry','grape','peas','nut','pecan','walnut','almond','acorn','pinenut','seed','arils','olives','dates','haws')),
    ('fruit',('apple','pear','peach','plum','cherry','cherries','orange','lemon','lime','apricot','mango','banana','coconut','melon','fruit','pod','pumpkin','cucumber','tomato','avocado','persimmon','pepper','zucchini','chanterelles','porcini','puffball')),
    ('cheese',('cheese',)),('pile',('flour','salt','crumb','dried','chocolate','cocoa'))]
    for family,words in rules:
        if any(w in n for w in words):return family
    return None

def geometry(name,family,im):
    px=[(x,y,im.getpixel((x,y))) for y in range(im.height) for x in range(im.width) if im.getpixel((x,y))[3]==255]
    if not px:raise ValueError(name+' has no opaque texels')
    # Dominant saturated food color; vessel colors remain original texture palette.
    colors={}
    for _,_,c in px:colors[c[:3]]=colors.get(c[:3],0)+1
    main=max(colors,key=lambda c:colors[c]*(1+(max(c)-min(c))/80))
    dark=tuple(int(c*.65) for c in main);light=tuple(min(255,int(c*1.15+12)) for c in main)
    cache={};out=[]
    def cube(a,b,c):
        c=tuple(c)
        if c not in cache:
            x,y,_=min(px,key=lambda p:sum((p[2][i]-c[i])**2 for i in range(3)))
            cache[c]=[(x+.15)*16/im.width,(y+.15)*16/im.height,(x+.85)*16/im.width,(y+.85)*16/im.height]
        out.append({'from':list(a),'to':list(b),'faces':{f:{'texture':'#food','uv':cache[c]} for f in FACES}})
    cream=(225,211,171);brown=(126,73,38);green=(72,107,39)
    def plate():
        cube((2,0,2),(14,.7,14),cream)
        for a,b in [((1,.7,2),(2,1.2,14)),((14,.7,2),(15,1.2,14)),((2,.7,1),(14,1.2,2)),((2,.7,14),(14,1.2,15))]:cube(a,b,cream)
    if family=='bowl':
        cube((4,0,4),(12,1,12),brown)
        for a,b in [((3,1,3),(13,5.5,4)),((3,1,12),(13,5.5,13)),((3,1,4),(4,5.5,12)),((12,1,4),(13,5.5,12))]:cube(a,b,brown)
        cube((4,1,4),(12,4.8,12),main)
        for i,(x,z) in enumerate(((5,5),(8,5),(6,8),(9,9))):cube((x,4.8,z),(x+1.5,5.3,z+1),(cream,green,light)[i%3])
    elif family in ('bottle','bucket'):
        cube((5,0,5),(11,1,11),dark);cube((4,1,4),(12,9,12),main)
        if family=='bottle':
            cube((5,9,5),(11,10,11),light);cube((6,10,6),(10,13,10),main);cube((5.5,13,5.5),(10.5,14,10.5),brown)
            cube((3.9,3,5),(4,6,11),cream)
        else:
            for a,b in [((3.5,9,3.5),(12.5,10,4.5)),((3.5,9,11.5),(12.5,10,12.5)),((3.5,9,4.5),(4.5,10,11.5)),((11.5,9,4.5),(12.5,10,11.5))]:cube(a,b,cream)
            cube((4,10,7),(5,13,8),dark);cube((11,10,7),(12,13,8),dark);cube((4,13,7),(12,14,8),dark)
    elif family=='fish':
        plate();cube((3,1,6),(11,3,10),main);cube((4,3,6.5),(10,3.6,9.5),light)
        cube((11,1,7),(13,2.8,9),main);cube((13,1,5.5),(14,3,10.5),dark)
        cube((3,2.4,5.9),(3.8,3,6.1),(25,25,25));cube((6,1,4.5),(8,1.7,6),dark)
    elif family in ('meat','sandwich','pizza','slice','cake','cookie','bread','cheese'):
        plate()
        if family=='pizza':
            cube((3,1,3),(13,1.8,13),brown);cube((4,1.8,4),(12,2.2,12),cream)
            for x,z in ((5,5),(9,5),(6,9),(10,9)):cube((x,2.2,z),(x+1.4,2.5,z+1.4),main)
        elif family=='sandwich':
            for y,c in ((1,brown),(1.8,green),(2.3,main),(3,cream),(3.5,brown)):cube((4,y,4),(12,y+.7,12),c)
        elif family in ('slice','cheese'):
            for i in range(4):
                x=4+i*2;z=4+i
                cube((x,1,z),(x+2,4,12-i),main)
                if family=='slice':cube((x,4,z),(x+2,4.5,12-i),cream)
        elif family=='cake':
            cube((4,1,4),(12,4,12),brown);cube((3.5,4,3.5),(12.5,5,12.5),cream);cube((7,5,7),(9,5.5,9),main)
        elif family=='cookie':
            cube((4,1,3),(12,1.8,13),main);cube((3,1,4),(13,1.8,12),main)
            for x,z in ((5,5),(9,5),(6,9),(10,10)):cube((x,1.8,z),(x+1,2,z+1),dark)
            if 'donut' in name or 'onion_ring' in name: # four segments with a real center hole
                out=out[:5]
                for a,b in [((4,1,3),(12,3,6)),((4,1,10),(12,3,13)),((3,1,6),(6,3,10)),((10,1,6),(13,3,10))]:cube(a,b,main)
        elif family=='bread':
            cube((3,1,5),(13,3,11),dark);cube((4,3,5),(12,4.5,11),main)
            for x in (5,8,11):cube((x,4.5,6),(x+.5,4.7,10),cream)
        else:
            cube((4,1,4),(11,3,11),main);cube((5,3,5),(10,3.7,10),light)
            if any(w in name for w in ('rib','leg','wing','chop','chicken','turkey')):cube((7,1.5,10),(9,2.5,14),cream)
            else:
                for x in (5,7,9):cube((x,3.7,5),(x+.3,3.8,10),dark)
    elif family in ('cane','lollipop','candy'):
        if family=='lollipop':
            cube((7.5,0,7.5),(8.5,8,8.5),brown);cube((5,8,7),(11,13,9),main);cube((6,7,7),(10,14,9),main)
            cube((7,9,6.9),(9,12,7),cream)
        elif family=='cane':
            for y in range(0,12,2):cube((5,y,7),(7,y+2,9),main if y%4 else cream)
            cube((7,10,7),(11,12,9),cream);cube((9,8,7),(11,10,9),main)
        else:
            cube((5,0,5),(11,4,11),main);cube((6,4,6),(10,5,10),light)
            if 'bar' in name or 'fudge' in name:
                for x in (5,8):cube((x,4,5),(x+2,4.5,10),dark)
            elif 'ice_cream' in name or 'sundae' in name:cube((6,5,6),(10,7,10),cream)
    elif family=='berry':
        for i,(x,z) in enumerate(((4,5),(8,5),(6,8),(9,9),(5,10))):
            cube((x,0,z),(x+2.5,2.5,z+2.5),main if i%2 else dark);cube((x+.6,2.5,z+.6),(x+1.9,2.8,z+1.9),light)
    elif family=='root':
        if 'potato' in name:
            cube((4,0,5),(12,3,11),main);cube((5,3,6),(11,4,10),light)
            for x,z in ((6,6),(9,8),(7,9)):cube((x,3.9,z),(x+.5,4.05,z+.5),dark)
        elif any(v in name for v in ('turnip','radish','beet')):
            cube((5,0,5),(11,4,11),main);cube((6,4,6),(10,5,10),light)
            cube((7,5,7),(9,8,8),green);cube((5,6,7),(11,6.6,9),green)
        elif 'corn' in name:
            cube((3,0,6),(13,2,10),green)
            for x in range(4,12,2):
                for z in (6,8):cube((x,2,z),(x+1.7,3.5,z+1.7),main)
        else:
            for i in range(4):cube((4+i*2,0,6+i*.4),(6+i*2,3-i*.5,10-i*.4),main)
            cube((2,.2,6),(4,1,10),green);cube((1,.2,7),(4,1.4,8),green)
    elif family=='leaf':
        for i in range(4):cube((3+i,0+i*.45,3+i),(13-i,.6+i*.45,13-i),main if i%2 else dark)
        cube((7,1.8,4),(8,2.1,12),light)
    elif family=='fruit':
        if 'banana' in name:
            for i in range(5):cube((3+i*2,0,5+abs(i-2)),(5+i*2,2,7+abs(i-2)),main)
        else:
            cube((5,0,5),(11,1,11),dark);cube((4,1,4),(12,6,12),main);cube((5,6,5),(11,7,11),light)
            cube((7,7,7),(8,9,8),brown);cube((8,7,7),(11,7.5,9),green)
    elif family in ('pie','pancake','sushi','mushroom','egg','fillet','produce_slice','shellfish','tentacle'):
        plate()
        if family=='pie':
            cube((3,1,4),(13,2.8,12),brown);cube((4,1,3),(12,2.8,13),brown)
            cube((4,2.8,4),(12,3.3,12),main)
            for x in (5,8,11):cube((x,3.3,4),(x+.5,3.6,12),cream)
            for z in (5,8,11):cube((4,3.3,z),(12,3.6,z+.5),cream)
        elif family=='pancake':
            for y in (1,2,3):
                cube((4,y,3),(12,y+.8,13),main);cube((3,y,4),(13,y+.8,12),main)
            cube((6,3.8,6),(10,4.3,10),cream)
        elif family=='sushi':
            for x,z in ((4,4),(9,4),(4,9),(9,9)):
                cube((x,1,z),(x+3,3,z+3),dark);cube((x+.3,3,z+.3),(x+2.7,3.2,z+2.7),cream)
                cube((x+1,3.2,z+1),(x+2,3.3,z+2),main)
        elif family=='mushroom':
            for x,z in ((4,4),(9,5),(6,9)):
                cube((x+1,1,z+1),(x+2,2.5,z+2),cream);cube((x,2.5,z),(x+3,3.3,z+3),main)
        elif family=='egg':
            cube((4,1,4),(12,1.6,12),cream);cube((3,1,5),(13,1.6,11),cream);cube((6,1.6,6),(10,2.6,10),main)
        elif family=='fillet':
            cube((3,1,5),(12,2.3,11),main);cube((4,2.3,6),(11,2.7,10),light)
            for x in (5,7,9):cube((x,2.7,6),(x+.3,2.8,10),cream)
        elif family=='produce_slice':
            cube((3,1,4),(13,1.8,12),dark);cube((4,1.8,5),(12,2.8,11),main)
            for x in (5,8,10):cube((x,2.8,7),(x+.4,2.9,8),brown)
        elif family=='shellfish':
            for i in range(4):cube((4+i*2,1,5+i*.3),(6+i*2,3.6-i*.4,11-i*.3),main if i%2 else dark)
            cube((11,1,4),(13,2,12),light)
        else:
            for i in range(4):
                cube((3+i*2,1,4+i),(5+i*2,2.5,7+i),main)
                cube((3.5+i*2,2.5,5+i),(4.3+i*2,2.8,5.8+i),cream)
    elif family=='pile':
        for i in range(4):cube((3+i,i*.65,3+i),(13-i,(i+1)*.65,13-i),main if i%2 else light)
    else:raise ValueError(family)
    import model_writer
    model_writer.finish_closed(out)
    # Separation can nudge coplanar bottom faces below zero. Keep the complete
    # closed mesh resting on the surface, with UVs unchanged.
    low=min(e['from'][1] for e in out)
    for e in out:
        e['from'][1]=round(e['from'][1]-low,6)
        e['to'][1]=round(e['to'][1]-low,6)
    return out

def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def generate(writer=None):
    emit=writer or write
    catalog=ROOT/'tools/food_servings_catalog.json'
    if catalog.exists():rows=json.loads(catalog.read_text(encoding='utf-8'))
    else:
        audit=json.loads((ROOT/'build/food-placement-inventory.json').read_text())
        rows=[];unknown=[]
        for r in audit:
            if r['menu_dish'] or r['id']=='minecraft:pumpkin_pie' or r.get('block_class','').endswith('HouseholdBlock'):continue
            f=classify(r['id'].split(':')[1])
            if not f:unknown.append(r['id'])
            rows.append({'id':r['id'],'family':f})
        if unknown:raise ValueError('Unclassified: '+str(unknown))
        write(catalog,rows)
    manifest=[];counts={}
    atlaspath=RES/'assets/minecraft/atlases/blocks.json'
    atlas=json.loads(atlaspath.read_text(encoding='utf-8'))
    atlas['sources']=[v for v in atlas['sources'] if not v.get('sprite','').startswith('jugcraft:block/food_serving_material/')]
    for row in rows:
        item=row['id'];family=row['family'];ref,d,tex,im=source(item)
        ns,n=item.split(':');path='food_serving/'+ns+'/'+n
        elems=geometry(n,family,im)
        sprite='jugcraft:block/food_serving_material/'+ns+'/'+n
        atlas['sources'].append({'type':'minecraft:single','resource':tex,'sprite':sprite})
        m={'textures':{'food':sprite,'particle':sprite},'elements':elems}
        row['source_model']=ref;row['source_texture']=tex
        # Native solid source models retain their geometry, materials and UV mapping.
        native=d.get('elements',[])
        if native and all(all(e['to'][i]>e['from'][i] for i in range(3)) for e in native):
            lo=[min(e['from'][i] for e in native) for i in range(3)];hi=[max(e['to'][i] for e in native) for i in range(3)]
            scale=min(12/(hi[0]-lo[0]),12/(hi[2]-lo[2]),14/(hi[1]-lo[1]),1)
            shift=[8-(lo[0]+hi[0])*scale/2,-lo[1]*scale,8-(lo[2]+hi[2])*scale/2]
            elems=copy.deepcopy(native)
            for e in elems:
                x,y,z=e['from'];X,Y,Z=e['to'];uv={'down':[x,16-Z,X,16-z],'up':[x,z,X,Z],'north':[16-X,16-Y,16-x,16-y],'south':[x,16-Y,X,16-y],'west':[z,16-Y,Z,16-y],'east':[16-Z,16-Y,16-z,16-y]}
                for f,v in e['faces'].items():v.setdefault('uv',uv[f]);v.pop('cullface',None)
                for k in ('from','to'):e[k]=[round(e[k][i]*scale+shift[i],5) for i in range(3)]
                if 'rotation' in e:e['rotation']['origin']=[round(e['rotation']['origin'][i]*scale+shift[i],5) for i in range(3)]
            m={'textures':d['textures'],'elements':elems};row['native_geometry']=True
        else:row['native_geometry']=False
        shape='SERVING'
        manifest.append({'id':item,'shape':shape,'bounds':[min(e['from'][i] for e in elems) for i in range(3)]+[max(e['to'][i] for e in elems) for i in range(3)]})
        emit(RES/'assets/jugcraft/models/block'/f'{path}.json',m)
        emit(RES/'assets/jugcraft/blockstates'/f'{path}.json',{'variants':{f'facing={f}':{'model':'jugcraft:block/'+path,'y':a} for f,a in [('north',0),('east',90),('south',180),('west',270)]}})
        emit(RES/'data/jugcraft/loot_table/blocks'/f'{path}.json',{'type':'minecraft:block','pools':[]})
        counts[family]=counts.get(family,0)+1
    write(catalog,rows);emit(RES/'jugcraft/food_servings.json',manifest)
    langpath=RES/'assets/jugcraft/lang/en_us.json'
    lang=json.loads(langpath.read_text(encoding='utf-8'))
    for row in rows:
        ns,n=row['id'].split(':'); name=lang.get('item.'+ns+'.'+n,lang.get('block.'+ns+'.'+n,n.replace('_',' ').title()))
        lang['block.jugcraft.food_serving.'+ns+'.'+n]='Placed '+name
    emit(langpath,lang)
    emit(atlaspath,atlas)
    print(len(rows),'servings',counts,'native',sum(r['native_geometry'] for r in rows))
if __name__=='__main__':generate()
