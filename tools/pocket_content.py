"""Adapt the owner's pocket-pet icons, foods and recipes without changing originals."""
from menagerie_content import *

PETS=SOURCE/'jimbos version of inventory pets/inventorypets'

def ingredient(v):
    if isinstance(v,list):return [ingredient(x) for x in v]
    if isinstance(v,dict) and set(v)=={'item'}:return v['item']
    if isinstance(v,dict) and set(v)=={'tag'}:return '#'+v['tag']
    return v

def main():
    catalog={};lang=read(ASSET/'lang/en_us.json');names=read(PETS/'lang/en_us.json')
    omitted={}
    for key,name in names.items():
        if not key.startswith('item.inventorypets.'):continue
        source=key.rsplit('.',1)[1]
        if source.startswith(('temp_','petachieve_','patreon_')) or source.startswith('pet_meta_') or source in {'feed_bag_new','pet_sated_chest','pet_sated_double_chest'}:
            omitted[source]='Legacy temporary inventory, presentation state or supporter label; retained in source, not a duplicate gameplay object.';continue
        if not (PETS/f'models/item/{source}.json').exists():continue
        pet=source.startswith('pet_');kind=source[4:] if pet else source
        id='pocket_'+source
        catalog[id]={'source':source,'name':name,'pet':pet,'ability':kind}
        lang['item.jugcraft.'+id]=name
        write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':f'jugcraft:menagerie/pocket/item/{source}'}})
    for folder in ['textures','sounds']:
        for p in (PETS/folder).rglob('*'):
            if p.is_file():copy(p,ASSET/folder/'menagerie/pocket'/p.relative_to(PETS/folder))
    for p in (PETS/'models').rglob('*.json'):
        j=remap(read(p),'pocket','inventorypets',True);j.pop('overrides',None)
        write(ASSET/'models/menagerie/pocket'/p.relative_to(PETS/'models'),j)
    sounds=read(ASSET/'sounds.json')
    for key,j in read(PETS/'sounds.json').items():
        j=remap(j,'pocket','inventorypets',True);j.pop('subtitle',None);sounds['pocket_'+key]=j
    write(ASSET/'sounds.json',sounds)
    aliases={'pocket_feed_bag_new':'pocket_feed_bag','pocket_pet_sated_chest':'pocket_pet_chest','pocket_pet_sated_double_chest':'pocket_pet_double_chest'}
    for p in (PETS/'recipe').glob('*.json'):
        j=remap(read(p),'pocket','inventorypets');text=json.dumps(j)
        for a,b in aliases.items():text=text.replace('jugcraft:'+a,'jugcraft:'+b)
        j=json.loads(text)
        if 'key' in j:j['key']={k:ingredient(v) for k,v in j['key'].items()}
        if 'ingredients' in j:j['ingredients']=[ingredient(v) for v in j['ingredients']]
        if 'ingredient' in j:j['ingredient']=ingredient(j['ingredient'])
        ids=re.findall(r'jugcraft:(pocket_[a-z0-9_]+)',json.dumps(j))
        if all(id in catalog for id in ids):write(DATA/f'recipe/pocket_{p.name}',j)
    for id,e in catalog.items():
        if e['pet'] and not any(read(p).get('result',{}).get('id')=='jugcraft:'+id for p in (DATA/'recipe').glob('pocket_*.json')):
            write(DATA/f'recipe/{id}.json',{'type':'minecraft:crafting_shaped','pattern':['WWW','WGW','WDW'],'key':{'W':'minecraft:gold_nugget','G':'minecraft:diamond','D':'minecraft:emerald'},'result':{'id':'jugcraft:'+id}})
    write(ASSET/'lang/en_us.json',lang);write(RES/'jugcraft/menagerie/pockets.json',catalog)
    write(ROOT/'art/owner-library/catalog/edited-2026-10-18/pocket-adaptation.json',{'registeredItems':len(catalog),'excludedDuplicateOrPresentationEntries':omitted,'runtimeFiles':PROVENANCE})
    print('Adapted',len(catalog),'pocket items; legacy temporary inventories are not separate objects.')

if __name__=='__main__':main()
