"""Narrow, source-verified geometry exceptions; never a whole-import waiver."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / 'src/main/resources/assets/jugcraft/models/newest'
SOURCE = ROOT / 'art/owner-library/originals/Edited-2026-10-29'


def intentional_reversed_elements(path, elements):
    """Recognize authored two-sided shells and the named upside-down potato.

    Paper lanterns and whirlwind rings pair outward and inward surfaces. Basket
    handles have the matching inner surface. Dinnerbone reverses TWO axes, a
    proper 180-degree rotation rather than an inside-out cube. Preserve their
    original bounds, rotations and per-face UVs; any changed geometry fails this
    exception and returns to the regular art audit.
    """
    path = Path(path).resolve()
    if not path.is_relative_to(MODELS):
        return False
    relative = path.relative_to(MODELS).as_posix()
    folders = {
        'forbidden': ('forbidden aracane', 'forbidden_arcanus'),
        'jugtania': ('Jugtania', 'botania'),
        'supps': ('SUPPS', 'supplementaries'),
        'twigworks': ('twig', 'twigs'),
    }
    family, _, name = relative.partition('/')
    baskets = {'block/lunch_basket' + state + dye + '.json'
               for state in ('', '_open', '_hanging', '_hanging_open')
               for dye in ('', '_dyed')}
    lanterns = {'block/' + color + 'paper_lantern' + hanging + '.json'
                for color in ('', 'template_', 'allium_', 'blue_orchid_',
                              'crimson_roots_', 'dandelion_', 'torchflower_')
                for hanging in ('', '_hanging')}
    if not ((family == 'forbidden' and name == 'block/whirlwind.json')
            or (family == 'jugtania' and name == 'tiny_potato/dinnerbone.json')
            or (family == 'supps' and name in baskets)
            or (family == 'twigworks' and name in lanterns)):
        return False
    folder, namespace = folders[family]
    source_models = SOURCE / folder / 'assets' / namespace / 'models'
    source = source_models / name
    for _ in range(8):
        if not source.is_file():
            return False
        data = json.loads(source.read_text(encoding='utf-8'))
        if 'elements' in data:
            return data['elements'] == elements
        parent = data.get('parent', '')
        if not parent.startswith(namespace + ':'):
            return False
        source = source_models / (parent.split(':', 1)[1] + '.json')
    return False
