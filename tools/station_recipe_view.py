"""JEI copies of bundled station recipes, including imported owner recipe data.

Read the finished server resources, not a second hand-maintained ingredient table.
The material generator calls this after writing recipes. Server datapack overrides
remain outside this default-data catalog, like the existing machine JEI integration.
"""
import json
from pathlib import Path

RES = Path(__file__).resolve().parents[1] / 'src/main/resources'
STATIONS = {
    'pot_cooking': ('cooking_pot', 'Heat source below the pot.'),
    'cutting': ('cutting_board', 'Use the displayed knife on the board; it takes durability damage.'),
    'household_construction': ('household_workbench', 'Build using materials in your inventory.'),
    'household_oven': ('household_light_stove', 'Requires furnace fuel.'),
    'household_freezer': ('household_light_freezer', 'Cooled using snowballs, snow blocks, or ice.'),
    'household_pan': ('household_frying_pan', 'Requires appliance fuel.'),
    'household_toaster': ('household_light_toaster', 'Requires furnace fuel.'),
    'household_microwave': ('household_light_microwave', 'Requires furnace fuel.'),
}


def ingredient(value):
    if isinstance(value, list):
        return [ingredient(x) for x in value]
    if isinstance(value, dict):
        return '#' + value['tag'] if 'tag' in value else value['item']
    return value


def build(resources=RES):
    groups = {}
    household = json.loads((resources / 'jugcraft/menagerie/household.json').read_text(encoding='utf-8'))
    for kind, (block, note) in STATIONS.items():
        behavior = 'cutting' if kind == 'cutting' else kind.removeprefix('household_')
        if kind == 'household_construction':
            behavior = 'workbench'
        catalysts = ['jugcraft:' + block]
        if kind != 'pot_cooking':
            catalysts += ['jugcraft:' + name for name, info in household['blocks'].items()
                          if info['kind'] == behavior and name != block]
        groups[kind] = dict(type=kind, catalysts=catalysts, recipes=[], note=note)
    for path in sorted((resources / 'data/jugcraft/recipe').rglob('*.json')):
        data = json.loads(path.read_text(encoding='utf-8'))
        kind = data['type'].removeprefix('jugcraft:')
        if kind not in groups:
            continue
        row = dict(id=path.relative_to(resources / 'data/jugcraft/recipe').with_suffix('').as_posix(),
                   inputs=data.get('ingredients', [dict(ingredient=data.get('ingredient'), count=1)]),
                   outputs=data.get('results', [data.get('result')]),
                   tools=[data['tool']] if 'tool' in data else [],
                   ticks=data.get('time', data.get('cookingtime', 0)), notes=[])
        groups[kind]['recipes'].append(row)
    # These workings are public recipes, separate from the hidden Concordance alchemy formulas.
    operations = {'imbuement': 'imbuement_chamber', 'enchanting_apparatus': 'enchanting_apparatus',
                  'glyph': 'scribes_table', 'book_upgrade': 'alteration_table', 'dye': 'alteration_table'}
    for path in sorted((resources / 'data/jugcraft/nouveau_recipe').rglob('*.json')):
        data = json.loads(path.read_text(encoding='utf-8'))
        op = data['type']
        if op not in operations:
            continue
        station = operations[op]
        group = groups.setdefault('nouveau_' + station, dict(type='nouveau_' + station,
            catalysts=['jugcraft:nouveau_' + station], recipes=[], note='Select the working at the station.'))
        inputs = [data[k] for k in ('input', 'reagent') if k in data]
        inputs += data.get('inputs', []) + data.get('ingredients', [])
        tools = data.get('pedestalItems', []) if op == 'imbuement' else []
        if op != 'imbuement':
            inputs += data.get('pedestalItems', [])
        source = data.get('sourceCost', data.get('source', 0))
        notes = [f'{(source + 99) // 100} Ley; {data.get("exp", 0)} experience points.']
        if tools:
            notes.append('Displayed pedestal ingredients are retained.')
        if op == 'imbuement':
            notes.append('Ley speeds up imbuement; without charge it works slowly.')
        if op == 'enchanting_apparatus':
            notes.append('Requires an Arcane Core below the apparatus.')
        if op == 'dye':
            notes.append('Color follows the chosen dye; existing item data is preserved.')
        group['recipes'].append(dict(id='nouveau/' + path.stem,
            inputs=[dict(ingredient=ingredient(x), count=1) for x in inputs],
            outputs=[data.get('result', data.get('output'))], tools=[ingredient(x) for x in tools], ticks=0, notes=notes))
    return list(groups.values())
