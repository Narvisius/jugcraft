"""Build a searchable implementation inventory, excluding subtitle/tooltip pseudo-entities."""
from menagerie_content import ROOT,read,write
from collections import Counter
SOURCE=ROOT/'art/owner-library/originals/Edited-2026-10-18-Part2'
OUT=ROOT/'art/owner-library/catalog/edited-2026-10-18-part2'
NAMESPACES={'cnb':'cnb','furniture':'refurbished_furniture','jimbologics':'ecologics','jims biomes':'biomesoplenty','jims caves':'alexscaves','jims mobs':'alexsmobs','jug_fauna':'fauna','legendarycreatures':'legendarycreatures','mutations':'mutantmonsters'}

def main():
    catalogs=[];errors=[]
    for group in sorted(SOURCE.iterdir()):
        if not group.is_dir():continue
        lang={}
        for p in [group/'lang/en_us.json',group/'lang/en_us.lang',group/'lang/en_US.lang']:
            if not p.exists():continue
            if p.suffix=='.json':lang.update(read(p))
            else:
                for line in p.read_text(encoding='utf-8-sig').splitlines():
                    if '=' in line and not line.startswith('#'):k,v=line.split('=',1);lang[k]=v
        labels={k:v for k,v in lang.items() if k.split('.')[0] in {'block','item','entity'} and ((len(k.split('.'))==3 and k.split('.')[1] in {NAMESPACES[group.name],'minecraft'}) or k.endswith('.name')) and v and '%' not in v}
        models=[];animations=[];recipes=[];biomes=[];structures=[]
        for p in sorted(group.rglob('*.json')):
            try:v=read(p)
            except (ValueError,UnicodeError) as e:errors.append({'path':p.relative_to(SOURCE).as_posix(),'error':str(e)});continue
            rel=p.relative_to(group).as_posix()
            if 'minecraft:geometry' in v:models.append({'path':rel,'identifiers':[g.get('description',{}).get('identifier') for g in v['minecraft:geometry']]})
            if isinstance(v.get('animations'),dict):animations.append({'path':rel,'clips':list(v['animations'])})
            if p.parts[len(group.parts)] in {'recipe','recipes'}:recipes.append({'path':rel,'type':v.get('type')})
            if rel.startswith('worldgen/biome/'):biomes.append(rel)
        structures=[p.relative_to(group).as_posix() for p in group.rglob('*.nbt')]
        namespaces=Counter(k.split('.')[1] for k in labels if len(k.split('.'))>=3)
        runtime=ROOT/'src/main/resources/assets/jugcraft/items'
        collisions=[]
        for key in labels:
            leaf=key.split('.')[2]
            if (runtime/(leaf+'.json')).exists():collisions.append({'sourceLabel':key,'existingItemId':'jugcraft:'+leaf})
        implementation={'cnb':('grove','Nine woodland creatures and supporting equipment/interactions'),
                        'legendarycreatures':('fable','Twenty fable creatures and supporting encounters/utilities'),
                        'jims mobs':('companion','Only the sixteen complete companion models; the larger Alex-namespaced roster is pending')}.get(group.name)
        runtime_details={}
        if implementation:
            key,scope=implementation
            runtime_path=ROOT/f'src/main/resources/jugcraft/menagerie/{key}.json'
            if runtime_path.exists():runtime_details={'runtimeManifest':key+'-runtime.json','localScope':scope,'implementedSpecies':[s['id'] for s in read(runtime_path)['species']],'validation':'See docs/features/owner-menagerie-oct18.md; native registration is not visual/playtest acceptance'}
        catalogs.append({'collection':group.name,'status':'local native subset implemented; validation and remaining scope tracked separately' if runtime_details else 'preserved; native implementation pending',**runtime_details,'labels':labels,'legacyNamespaces':dict(namespaces),'entityModels':models,'animations':animations,'recipes':recipes,'biomes':biomes,'structures':structures,'existingItemNameOverlaps':collisions,'compiledClasses':len(list(group.rglob('*.class'))),'javaSources':len(list(group.rglob('*.java'))),'kotlinSources':len(list(group.rglob('*.kt')))})
    write(OUT/'content.json',{'collections':catalogs,'parseErrors':errors})
    print('Cataloged',sum(len(c['labels']) for c in catalogs),'named content entries;',len(errors),'JSON files need format adaptation.')
    for c in catalogs:print(c['collection'],len(c['entityModels']),'entity models,',len(c['recipes']),'recipes,',len(c['existingItemNameOverlaps']),'existing item names to reconcile')

if __name__=='__main__':main()
