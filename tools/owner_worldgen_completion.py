"""Upgrade preserved legacy generation to bounded native 26.3 implementations."""
from menagerie_content import RES,DATA,read,write
import owner_template_data


def feature_order():
    """Retain shared 26.3 feature order, then consistently order owner-only detail.

    Existing biomes remain unchanged. Legacy lists may reverse shared features
    (notably pumpkin/sugar cane), which otherwise crashes the game's sorter.
    """
    import graphlib
    from biome_bases import BASES
    paths=list((DATA/'worldgen/biome').glob('*.json'))
    owners=[p for p in paths if p.stem.startswith(('landscape_','cavern_'))]
    baselines=[read(p)['features'] for p in paths if p not in owners]
    baselines += [b['steps'] for b in BASES.values()]
    ranks=[]
    for step in range(11):
        graph={}
        for features in baselines:
            seq=features[step] if step<len(features) else []
            for f in seq:graph.setdefault(f,set())
            for a,b in zip(seq,seq[1:]):
                if a!=b:graph[b].add(a)
        order=list(graphlib.TopologicalSorter(graph).static_order())
        ranks.append({name:i for i,name in enumerate(order)})
    for p in owners:
        before=read(p);after=dict(before);features=[]
        for step,seq in enumerate(before['features']):
            rank=ranks[step]
            features.append(sorted(set(seq),key=lambda f:(rank.get(f,len(rank)),f)))
        after['features']=features
        if after!=before:write(p,after)


def modern(v):
    if isinstance(v,str):
        return v.replace('goblinsanddungeons:','jugcraft:landscape_').replace('minecraft:weighted_state_provider','minecraft:weighted') if v!='minecraft:grass' else 'minecraft:short_grass'
    if isinstance(v,list):return [modern(x) for x in v]
    if not isinstance(v,dict):return v
    d={k:modern(x) for k,x in v.items()}
    # The block became short_grass; the configured feature is still named grass.
    if d.get('feature')=='minecraft:short_grass':d['feature']='minecraft:grass'
    if d.get('type')=='minecraft:random_patch':d['type']='jugcraft:owner_patch'
    if d.get('type')=='minecraft:random_offset':
        d={'type':'minecraft:offset','x':d['xz_spread'],'z':d['xz_spread'],'y':d['y_spread']}
    if d.get('type')=='minecraft:lake':
        d.setdefault('can_place_feature',{'type':'minecraft:true'})
        d.setdefault('can_replace_with_air_or_fluid',{'type':'minecraft:not','predicate':{'type':'minecraft:matching_block_tag','tag':'minecraft:features_cannot_replace'}})
        d.setdefault('can_replace_with_barrier',{'type':'minecraft:not','predicate':{'type':'minecraft:matching_block_tag','tag':'minecraft:lava_pool_stone_cannot_replace'}})
    if 'spawns_by_category' in d:
        species={s['id']:s for g in ('cavern','landscape') for s in read(RES/f'jugcraft/menagerie/{g}.json')['species']}
        spawns={}
        for category,entries in d['spawns_by_category'].items():
            for entry in entries:
                s=species.get(entry['type'].removeprefix('jugcraft:'))
                native=('monster' if s['mood']=='hostile' else 'water_creature' if s['movement']=='water' else 'ambient' if s['movement']=='air' and not s['mount'] else 'creature') if s else category
                if ':' in native:native='water_creature' if 'sea' in native else 'creature'
                spawns.setdefault(native,[]).append(entry)
        d['spawns_by_category']=spawns
    return d


def main():
    for folder in ('worldgen','tags','loot_table'):
        for p in (DATA/folder).rglob('*.json'):
            if not p.stem.startswith(('landscape_','cavern_')):continue
            before=read(p);after=modern(before)
            if after!=before:write(p,after)
    from owner_landmarks import register
    for i,name in enumerate(('small_goblin_camp','medium_goblin_camp','large_goblin_camp','ruined_keep')):
        register('landscape_'+name,['landscape_'+name],'#jugcraft:has_structure/landscape_'+name,height=0,surface=True,salt=183500+i)
        p=DATA/f'structure/landscape_{name}.nbt'
        p.write_bytes(owner_template_data.adapt(p.read_bytes(),modern))
    feature_order()


if __name__=='__main__':main()
