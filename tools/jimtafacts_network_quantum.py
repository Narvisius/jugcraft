"""Source QnbFormedModel cube dimensions/light overlays adapted to native multipart models."""
BLOCKS = ['quantum_ring', 'quantum_link']
ITEMS = ['quantum_entangled_singularity']

def generate(source, assets, res, model, texture, dump, recipe):
    faces = ['west','east','north','south','down','up']
    refs = {k:texture('ae2:'+v) for k,v in {'ring':'block/quantum_ring','link':'block/quantum_link','light':'block/quantum_ring_light','corner':'block/quantum_ring_light_corner','covered':'part/cable/covered/transparent','glass':'part/cable/glass/transparent'}.items()}
    def cube(lo, hi, ref, emission=0):
        return {'from':lo,'to':hi,'light_emission':emission,'faces':{f:{'texture':'#'+ref} for f in faces}}
    def mesh(name, elements):
        from model_writer import fit_uvs
        fit_uvs(elements)
        path='block/jimtafacts/ae2/quantum_'+name
        dump(assets/f'models/{path}.json',{'parent':'minecraft:block/block','textures':dict(refs,particle=refs['ring']),'elements':elements})
        return 'jugcraft:'+path
    center=mesh('edge',[cube([0,2,2],[16,14,14],'ring'),cube([2,0,2],[14,16,14],'ring'),cube([2,2,0],[14,14,16],'ring')])
    glow=mesh('edge_light',[cube([-.01]*3,[16.01]*3,'light',15)])
    corner=mesh('corner_light',[cube([1.99]*3,[14.01]*3,'corner',15)])
    ringparts=[{'when':{'formed':'false'},'apply':{'model':model('block/quantum_ring')}}, {'when':{'formed':'true','corner':'true'},'apply':{'model':model('block/quantum_ring')}}, {'when':{'formed':'true','corner':'false'},'apply':{'model':center}}, {'when':{'formed':'true','corner':'false','lit':'true'},'apply':{'model':glow}}, {'when':{'formed':'true','corner':'true','lit':'true'},'apply':{'model':corner}}]
    linkparts=[{'apply':{'model':model('block/quantum_link')}}]
    for typ,thick,pull,ref in [('corner',3.008,.8,'covered'),('link_glass',1.76,2.256,'glass'),('link_covered',3.008,3,'covered')]:
        for face in faces:
            axis=0 if face in ['west','east'] else 1 if face in ['down','up'] else 2
            lo=[8-thick]*3; hi=[8+thick]*3
            if face in ['west','north','down']:lo[axis]=0;hi[axis]=8-thick-pull
            else:lo[axis]=8+thick+pull;hi[axis]=16
            refname=mesh(typ+'_'+face,[cube(lo,hi,ref)])
            when={'formed':'true',face:'true'}
            if typ=='corner':when['corner']='true'
            (ringparts if typ=='corner' else linkparts).append({'when':when,'apply':{'model':refname}})
    for name,parts in [('quantum_ring',ringparts),('quantum_link',linkparts)]:
        dump(assets/f'blockstates/jimta_{name}.json',{'multipart':parts})
        dump(assets/f'items/jimta_{name}.json',{'model':{'type':'minecraft:model','model':model('block/'+name)}})
    dump(assets/'items/jimta_quantum_entangled_singularity.json',{'model':{'type':'minecraft:model','model':model('item/quantum_entangled_singularity')}})
    for name,pattern,keys in [('quantum_ring',['III','FQF','IEI'],{'I':'minecraft:iron_ingot','F':'jugcraft:jimta_fluix_crystal','Q':'jugcraft:jimta_quartz_glass','E':'jugcraft:jimta_engineering_processor'}),('quantum_link',['GGG','GPG','GGG'],{'G':'jugcraft:jimta_quartz_glass','P':'jugcraft:jimta_fluix_pearl'})]:
        recipe(name,{'type':'minecraft:crafting_shaped','pattern':pattern,'key':keys,'result':{'id':'jugcraft:jimta_'+name}})
    recipe('quantum_entangled_singularity',{'type':'minecraft:crafting_shapeless','ingredients':['jugcraft:jimta_singularity','jugcraft:jimta_ender_dust'],'result':{'id':'jugcraft:jimta_quantum_entangled_singularity','count':2}})
