"""Bind recovered geometry to existing stable entity/variant IDs and exact skins."""
import json
import re
import shutil
import hashlib
from pathlib import Path
from PIL import Image
from recover_creature_models import ROOT, OUT, REPORT, INPUT, write, body, num, split_args

SOURCES={'beast':('beasts','jims mobs'),'cavern':('caves','jims caves'),
         'landscape':('caves','jims caves'),'mutant':('mutants','mutant monster things'),
         'fauna':('fauna','familiar fauna'),'rotten':('rotten','rotten creature thing')}
ALIASES={'bone_serpent':'boneserpenthead','centipede_head':'cavecentipede',
         'catfish':'catfishsmall','underminer':'underminerdwarf','murmur':'murmurbody'}

def skin_layers(group, source, skin):
    """Renderer-declared overlays must never replace an animal's complete skin."""
    folder,name=skin.rsplit('/',1);layers=[]
    def overlay(base,mode='cutout'):
        layers.append(dict(skin=skin,mode=mode))
        return folder+'/'+base
    if group=='beast':
        replacements={'crocodile_crown':'crocodile_0','raccoon_bandana':'raccoon',
                      'grizzly_bear_honey':'grizzly_bear','grizzly_bear_snowy':'grizzly_bear',
                      'komodo_dragon_maid':'komodo_dragon','moose_snowy':'moose','moose_snowy_antlered':'moose_antlered',
                      'bison_snowy':'bison','bison_baby_snowy':'bison_baby',
                      'seal_crying':'seal_brown_0','seal_tongue':'seal_brown_0','blue_jay_shiny':'blue_jay',
                      'elephant_chest':'elephant','alligator_snapping_turtle_moss':'alligator_snapping_turtle',
                      'tusklin_hooves':'tusklin','laviathan_gear':'laviathan','rhinoceros_potion':'rhinoceros'}
        if name in replacements:skin=overlay(replacements[name])
        elif name.startswith('crimson_mosquito_blood'):skin=overlay('crimson_mosquito_blue' if name.endswith('_blue') else 'crimson_mosquito')
        elif name.startswith('soul_vulture_flames_'):skin=overlay('soul_vulture','glow')
        elif name=='frilled_shark_teeth':skin=overlay('frilled_shark','glow')
        elif name.startswith(('terrapin_shell_pattern_','terrapin_skin_pattern_')):skin=overlay('terrapin_with_overlays')
        elif name=='sunbird_shine':skin=folder+'/sunbird' # Billboard effect, not an entity atlas.
        elif name.startswith('leafcutter_ant_leaf_'):skin=folder+'/leafcutter_ant' # Separate carried leaf mesh.
        elif name=='platypus_fedora':skin=folder+'/platypus_perry' # Separate hat mesh.
    if group=='cavern':
        if source=='watcher' and name=='watcher_moth':skin=overlay('watcher','glow')
        if source in ('nucleeper','gumbeeper'):
            skin=folder+'/'+source
            layers.append(dict(skin=skin+'_glass',mode='translucent'))
            if source=='nucleeper':
                layers.append(dict(skin=skin+'_glow',mode='glow'))
                for frame in range(3):layers.append(dict(skin=skin+'_buttons_'+str(frame),mode='glow',frame=frame))
            if name.endswith('_possessed'):layers.append(dict(skin=folder+'/'+name,mode='glow'))
            layers.append(dict(skin=skin+'_explode',mode='glow',condition='attack'))
        if source=='caramel_cube':
            skin=skin.removesuffix('_outside')
            layers.append(dict(skin=skin+'_outside',mode='translucent'))
    if group=='mutant' and source=='mutant_snow_golem' and name=='jack_o_lantern':skin=folder+'/mutant_snow_golem'
    return skin,layers

def render_profile(collection, species):
    candidates=[p for p in (INPUT/collection).rglob('*.java') if p.stem.lower() in ('render'+species.replace('_',''),species.replace('_','')+'renderer')]
    profile={'scale':[1,1,1],'glow':[],'unbound':[]}
    if len(candidates)!=1:return profile
    src=candidates[0].read_text();profile['source']=candidates[0].relative_to(INPUT).as_posix()
    try:
        setup=body(src,r'protected void scale\(');depth=0
        for line in setup.splitlines():
            before=depth;depth+=line.count('{')-line.count('}')
            match=re.search(r'\.(?:m_85841_|scale|method_22905)\((.*)\);',line)
            if not match:continue
            if before:profile['unbound'].append(line.strip());continue
            try:profile['scale']=[num(x) for x in split_args(match[1])]
            except (ValueError,SyntaxError):profile['unbound'].append(line.strip())
    except ValueError:pass
    # Exact texture declared by the supplied unconditional basic-glow layer.
    for constant in re.findall(r'new LayerBasicGlow\(this, (\w+)\)',src):
        match=re.search(r'\b'+constant+r' = .*?"[^"\n]*textures/([^"\n]+)\.png"',src)
        if match:profile['glow'].append(match[1])
    if collection=='beasts' and species=='mungus':profile['glow'].append('entity/mungus_sack')
    return profile

def main():
    models={f.stem:json.loads(f.read_text()) for f in OUT.glob('*.json') if f.stem!='bindings'}
    bindings={};issues=[];copied=[];dimensions={}
    def provenance(runtime,source,collection,folder):
        return dict(runtime=runtime,source=str(source),collection=collection,
                    sourcePath=source.relative_to(Path('C:/Users/james/Downloads/Magic Mods')/folder).as_posix(),
                    sha256=hashlib.sha256(source.read_bytes()).hexdigest())
    for group,(collection,folder) in SOURCES.items():
        catalog=ROOT/'src/main/resources/jugcraft/menagerie'/('ecology.json' if group=='fauna' else group+'.json')
        textures=list((Path('C:/Users/james/Downloads/Magic Mods')/folder).rglob('*.png'))
        catalog_data=json.loads(catalog.read_text())
        registry_files=[p for p in (INPUT/collection).rglob('*.java') if p.name in ('AMEntityRegistry.java','ACEntityRegistry.java','ModEntityTypes.java')]
        original_sizes={}
        for registry in registry_files:
            for line in registry.read_text().splitlines():
                match=re.search(r'(?:register|registerEntityType)\("([^"]+)".*?\.(?:sized|m_20699_)\(([\d.]+)f?, ([\d.]+)f?\)',line)
                if match:original_sizes[match[1]]=[float(match[2]),float(match[3])]
        for s in catalog_data['species']:
            if not s['id'].startswith(group+'_'):continue
            if s['source'] in original_sizes:
                s['width'],s['height']=original_sizes[s['source']]
                dimensions[s['id']]=original_sizes[s['source']]
            plain=ALIASES.get(s['source'],s['source'].replace('_',''))
            adult=next((k for k in models if k.startswith(collection+'_') and k[len(collection)+1:].replace('model','')==plain),None)
            if adult is None:issues.append([s['id'],'missing geometry']);continue
            baby=next((k for k in models if k==adult+'baby' or k==adult.replace('model','babymodel')),adult)
            pair=dict(adult='recovered/'+adult,baby='recovered/'+baby,variants={},render=render_profile(collection,s['source']))
            for i,glow in enumerate(pair['render']['glow']):
                candidates=[p for p in textures if p.as_posix().endswith('/textures/'+glow+'.png')]
                if len(candidates)!=1:raise ValueError('Ambiguous supplied glow '+glow)
                skin='menagerie/'+group+'/'+glow;path=ROOT/'src/main/resources/assets/jugcraft/textures'/(skin+'.png');path.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(candidates[0],path)
                copied.append(provenance(skin,candidates[0],collection,folder))
                pair['render']['glow'][i]=skin
            for v in s['variants']:
                ages={}
                for age in ('adult','baby'):
                    key=adult if age=='adult' else baby
                    skin=v['babyTexture' if age=='baby' else 'texture']
                    if group=='mutant' and s['source']=='creeper_minion':
                        # The supplied renderer explicitly delegates to vanilla's atlas.
                        ages[age]=dict(model='recovered/'+key,skin='minecraft:entity/creeper/creeper',dye='',layers=[])
                        continue
                    if group=='beast' and s['source']=='leafcutter_ant' and 'queen' in skin:key='beasts_modelleafcutterantqueen'
                    if group=='beast' and s['source']=='underminer' and not 'dwarf' in skin:key='beasts_underminerhumanoid'
                    if group=='beast' and s['source']=='tarantula_hawk' and age=='baby':skin='menagerie/beast/entity/tarantula_hawk_baby'
                    if group=='beast' and s['source']=='bison' and age=='baby':skin='menagerie/beast/entity/bison_baby'+('_snowy' if 'snowy' in skin else '')
                    if group=='beast' and s['source']=='mungus':skin='menagerie/beast/entity/mungus'
                    if group=='cavern' and s['source']=='mine_guardian' and any(x in skin for x in ('anchor','explode')):skin='menagerie/cavern/entity/mine_guardian'
                    if group=='cavern' and s['source']=='nucleeper' and 'buttons_pattern' in skin:skin='menagerie/cavern/entity/nucleeper'
                    if group=='cavern' and s['source']=='gum_worm' and 'connection' in skin:skin='menagerie/cavern/entity/gum_worm'
                    if group=='cavern' and s['source']=='deep_one' and 'mage' in skin:key='caves_deeponemagemodel'
                    if group=='cavern' and s['source']=='deep_one' and 'knight' in skin:key='caves_deeponeknightmodel'
                    if s['source']=='catfish' and group=='beast':key='beasts_modelcatfish'+('large' if 'large' in skin else 'medium' if 'medium' in skin else 'small')
                    if s['source']=='blobfish' and group=='beast' and 'depressurized' in skin:key='beasts_modelblobfishdepressurized'
                    if s['source']=='flutter' and group=='beast' and 'potted' in skin:key='beasts_modelflutterpotted'
                    skin,layers=skin_layers(group,s['source'],skin)
                    for layer in layers:
                        layerpath=ROOT/'src/main/resources/assets/jugcraft/textures'/(layer['skin']+'.png')
                        tail=layer['skin'].split('/'+group+'/',1)[-1]+'.png'
                        layerfiles=[p for p in textures if p.as_posix().endswith('/'+tail)]
                        if not layerfiles:layerfiles=[p for p in textures if p.name==layerpath.name]
                        if len(layerfiles)!=1:raise ValueError('Ambiguous/missing supplied layer '+layer['skin'])
                        layerpath.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(layerfiles[0],layerpath)
                        copied.append(provenance(layer['skin'],layerfiles[0],collection,folder))
                    path=ROOT/'src/main/resources/assets/jugcraft/textures'/(skin+'.png')
                    tail=skin.split('/'+group+'/',1)[-1]+'.png'
                    candidates=[p for p in textures if p.as_posix().endswith('/'+tail)]
                    if not candidates:candidates=[p for p in textures if p.name==path.name]
                    # Only unambiguous supplied textures replace runtime pixels.
                    if len(candidates)==1:
                        path.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(candidates[0],path)
                        copied.append(provenance(skin,candidates[0],collection,folder))
                    with Image.open(path) as im:sz=list(im.size)
                    expected=models[key]['textureSize']
                    if sz[0]*expected[1]!=sz[1]*expected[0]:issues.append([s['id'],v['name'],age,'atlas aspect mismatch',sz,expected])
                    ages[age]=dict(model='recovered/'+key,skin=skin,dye='',layers=layers)
                pair['variants'][v['name']]=ages
            bindings[s['id']]=pair
        write(catalog,catalog_data)
    write(OUT/'bindings.json',bindings)
    profiles={}
    for species,pair in bindings.items():
        actions={}
        for variant in pair['variants'].values():
            for age in variant.values():
                model=models[age['model'].removeprefix('recovered/')]
                for k,v in model['actions'].items():actions[k]=max(actions.get(k,0),max(1,round(v['length']*20)))
        if species=='mutant_mutant_zombie':actions.update(slam_ground=24,roar=120)
        profiles[species]=actions
    write(ROOT/'src/main/resources/jugcraft/menagerie/recovered_actions.json',profiles)
    write(REPORT/'bindings-report.json',dict(species=len(bindings),issues=issues,textureCopies=copied,originalDimensions=dimensions))
    print('Bound',len(bindings),'species;',len(issues),'issues')
    for issue in issues:print(issue)

if __name__=='__main__':main()
