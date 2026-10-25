"""Explicit native registry inventory shared by generators and offline checks."""
import json
from functools import cache
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = json.loads((ROOT/'src/main/resources/jugcraft/menagerie/catalog.json').read_text(encoding='utf-8'))
POCKETS = json.loads((ROOT/'src/main/resources/jugcraft/menagerie/pockets.json').read_text(encoding='utf-8'))
GROVE_PATH=ROOT/'src/main/resources/jugcraft/menagerie/grove.json'
GROVE=json.loads(GROVE_PATH.read_text(encoding='utf-8')) if GROVE_PATH.exists() else {'blocks':{},'items':{},'species':[]}
FABLE_PATH=ROOT/'src/main/resources/jugcraft/menagerie/fable.json'
FABLE=json.loads(FABLE_PATH.read_text(encoding='utf-8')) if FABLE_PATH.exists() else {'blocks':{},'items':{},'species':[]}
COMPANION_PATH=ROOT/'src/main/resources/jugcraft/menagerie/companion.json'
COMPANION=json.loads(COMPANION_PATH.read_text(encoding='utf-8')) if COMPANION_PATH.exists() else {'blocks':{},'items':{},'species':[]}
HOUSEHOLD_PATH=ROOT/'src/main/resources/jugcraft/menagerie/household.json'
HOUSEHOLD=json.loads(HOUSEHOLD_PATH.read_text(encoding='utf-8')) if HOUSEHOLD_PATH.exists() else {'blocks':{},'items':{}}
ECOLOGY_PATH=ROOT/'src/main/resources/jugcraft/menagerie/ecology.json'
ECOLOGY=json.loads(ECOLOGY_PATH.read_text(encoding='utf-8')) if ECOLOGY_PATH.exists() else {'blocks':{},'items':{},'species':[]}
MUTANT_PATH=ROOT/'src/main/resources/jugcraft/menagerie/mutant.json'
MUTANT=json.loads(MUTANT_PATH.read_text(encoding='utf-8')) if MUTANT_PATH.exists() else {'blocks':{},'items':{},'species':[]}
BEAST_PATH=ROOT/'src/main/resources/jugcraft/menagerie/beast.json'
BEAST=json.loads(BEAST_PATH.read_text(encoding='utf-8')) if BEAST_PATH.exists() else {'blocks':{},'items':{},'species':[]}
LANDSCAPE_PATH=ROOT/'src/main/resources/jugcraft/menagerie/landscape.json'
LANDSCAPE=json.loads(LANDSCAPE_PATH.read_text(encoding='utf-8')) if LANDSCAPE_PATH.exists() else {'blocks':{},'items':{},'species':[],'fluids':{}}
CAVERN_PATH=ROOT/'src/main/resources/jugcraft/menagerie/cavern.json'
CAVERN=json.loads(CAVERN_PATH.read_text(encoding='utf-8')) if CAVERN_PATH.exists() else {'blocks':{},'items':{},'species':[],'fluids':{}}
ROTTEN_PATH=ROOT/'src/main/resources/jugcraft/menagerie/rotten.json'
ROTTEN=json.loads(ROTTEN_PATH.read_text(encoding='utf-8')) if ROTTEN_PATH.exists() else {'items':[],'species':[]}
CRAFTED=json.loads((ROOT/'src/main/resources/jugcraft/menagerie/crafted.json').read_text(encoding='utf-8'))
AIRCRAFT=json.loads((ROOT/'src/main/resources/jugcraft/aircraft.json').read_text(encoding='utf-8'))
AIRCRAFT_ITEMS=['aircraft_'+n for n in list(AIRCRAFT['craft'])+list(AIRCRAFT['upgrades'])+['hull','engine','sail','propeller','boiler','rotary_cannon','heavy_crossbow','telescope','bomb_bay']]
COLORS = 'white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()
EXTRA_BLOCKS = ['pocket_cloud_block','pocket_sand_block','pocket_stone_block','mount_firebird_feather','mount_firebird_feather2','mount_firebird_feather3','mount_firebird_feather4','herbal_spinning_wheel','herbal_pumpkling_crop'] + [f'herbal_gravestone_{s}_{m}' for s in ('cross','upright') for m in ('stone','mossy')]
EXTRA_ITEMS = ['mount_summoning_staff','menagerie_field_guide','herbal_ghost_spawn_egg','herbal_pumpkling_spawn_egg','herbal_pumpkling_seeds'] + [f'herbal_{c}_cloth' for c in COLORS] + ['herbal_'+s+'_'+p for s,parts in [('witch',['hat','robe','pants','boots']),('murder',['mask','coat','pants','boots'])] for p in parts]


def blocks():
    return list(CRAFTED['blocks']) + ['death_gravestone','artisans_workstation','artisan_furniture'] + list(CATALOG['blocks']) + EXTRA_BLOCKS + list(GROVE['blocks']) + list(FABLE['blocks']) + list(COMPANION['blocks']) + list(HOUSEHOLD['blocks']) + list(ECOLOGY['blocks']) + list(MUTANT['blocks']) + list(BEAST['blocks']) + list(LANDSCAPE['blocks']) + list(CAVERN['blocks']) + (['rotten_tnt_barrel','rotten_treasure_chest'] if ROTTEN['species'] else [])


def items():
    return ['furniture_material'] + list(CRAFTED['items']) + AIRCRAFT_ITEMS + list(CATALOG['items']) + list(POCKETS) + EXTRA_ITEMS + list(GROVE['items']) + list(FABLE['items']) + list(COMPANION['items']) + list(HOUSEHOLD['items']) + list(ECOLOGY['items']) + list(MUTANT['items']) + list(BEAST['items']) + list(LANDSCAPE['items']) + list(CAVERN['items']) + [g+'_'+f+'_bucket' for g,c in [('landscape',LANDSCAPE),('cavern',CAVERN)] for f in c['fluids']] + ['rotten_'+i for i in ROTTEN['items']] + [s['id']+'_spawn_egg' for s in ROTTEN['species']] + (['rotten_hunter_wolf_spawn_egg'] if ROTTEN['species'] else [])


@cache
def entries():
    return frozenset(blocks() + items())


def fluids():
    return {prefix+group+'_'+name for group,catalog in [('landscape',LANDSCAPE),('cavern',CAVERN)] for name in catalog['fluids'] for prefix in ('','flowing_')}


def entities():
    return {e['id'] for e in CATALOG['species']+GROVE['species']+FABLE['species']+COMPANION['species']+ECOLOGY['species']+MUTANT['species']+BEAST['species']+LANDSCAPE['species']+CAVERN['species']+ROTTEN['species']} | {f'ecology_{wood}_{kind}' for wood in ['azalea','flowering_azalea','coconut','maple','walnut'] for kind in ['boat','chest_boat']} | {id for c in [LANDSCAPE,CAVERN] for id in c['items'] if id.endswith('_boat')} | {'cavern_burrowing_arrow','cavern_seeking_arrow','beast_ancient_dart','beast_shark_tooth_arrow','beast_straddleboard','cavern_projectile','cavern_submarine','rotten_hunter_wolf','recovered_creature_part','herbal_pumpkling','veil_ghost','fable_dart','fable_wisp_purse'}


def feature():
    return 'agriculture'


@cache
def itemless_blocks():
    # These registrars create items only from their explicit item catalogs.
    return frozenset({'death_gravestone'}) | frozenset(k for c in (ECOLOGY,MUTANT,LANDSCAPE,CAVERN) for k in c['blocks'] if k not in c['items'])
