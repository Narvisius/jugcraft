"""Registered collection IDs and source-verified texture imports for shared audits."""
from functools import cache
from pathlib import Path
import importlib
import json

ROOT = Path(__file__).resolve().parents[1]
MODULES = ('industry', 'gear', 'robots', 'automation', 'transport', 'crops',
           'production', 'reactor', 'matter', 'civil', 'infrastructure', 'upgrades',
           'construction', 'laser', 'heat', 'tables', 'utilities', 'services',
           'personal', 'engines', 'factory', 'cables', 'terraforming', 'computers', 'spellbooks')

@cache
def blocks():
    import jimtafacts_network
    result = {'jimta_' + name for name in jimtafacts_network.BLOCKS}
    for name in MODULES:
        module = importlib.import_module('jimtafacts_' + name)
        if hasattr(module, 'blocks'): result.update(module.blocks())
    return frozenset(result)

@cache
def items():
    import jimtafacts, jimtafacts_network
    result = set(jimtafacts.items()) | set(jimtafacts_network.ids())
    for name in MODULES:
        module = importlib.import_module('jimtafacts_' + name)
        if hasattr(module, 'items'): result.update(module.items())
    # Computer blocks are registered as BlockItems separately from their portable items.
    import jimtafacts_computers
    result.update(jimtafacts_computers.blocks())
    return frozenset(result)

@cache
def texture_sources():
    result = {}
    def visit(value):
        if isinstance(value, list):
            for row in value: visit(row)
        elif isinstance(value, dict):
            source, runtime = value.get('source'), value.get('runtime')
            if isinstance(source, str) and isinstance(runtime, str) and runtime.endswith('.png'):
                if source.startswith('art/owner-library/originals/Oct23/Jimtafacts/'):
                    result[runtime] = source
            for row in value.values():
                if isinstance(row, (dict, list)): visit(row)
    for path in (ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts').glob('*copies.json'):
        visit(json.loads(path.read_text(encoding='utf-8')))
    return result

def original_texture(path):
    """An exception applies only while the exact supplied pixels remain intact."""
    path = Path(path).resolve()
    if not path.is_relative_to(ROOT): return False
    source = texture_sources().get(path.relative_to(ROOT).as_posix())
    return bool(source and (ROOT / source).is_file() and path.read_bytes() == (ROOT / source).read_bytes())

def original_inward_shell(path, elements):
    """Named source models deliberately use mirrored cubes for visible interior faces."""
    path=Path(path).resolve()
    base=ROOT/'src/main/resources/assets/jugcraft/models/jimtafacts/spellbooks'
    if not path.is_relative_to(base):return False
    names={'armor_pile','brazier','brazier_off','brazier_soul','brazier_template',
           'hanging_brazier','hanging_brazier_off','hanging_brazier_soul','hanging_brazier_template',
           'portal_frame','pocket_dimension_portal_frame'}
    if path.stem not in names:return False
    import jimtafacts_spellbooks
    source=jimtafacts_spellbooks.ART/'models'/path.relative_to(base)
    data=json.loads(source.read_text(encoding='utf-8'))
    for _ in range(8):
        if 'elements' in data:break
        parent=data.get('parent','')
        if not parent.startswith('irons_spellbooks:'):return False
        source=jimtafacts_spellbooks.ART/'models'/(parent.split(':',1)[1]+'.json')
        data=json.loads(source.read_text(encoding='utf-8'))
    from jimtafacts_model_repairs import repair
    ref='jimtafacts/spellbooks/'+source.relative_to(jimtafacts_spellbooks.ART/'models').with_suffix('').as_posix()
    original=repair(ref,data).get('elements',[])
    return len(original)==len(elements) and all(
        all(a.get(k)==b.get(k) for k in ('from','to','rotation')) for a,b in zip(original,elements))
