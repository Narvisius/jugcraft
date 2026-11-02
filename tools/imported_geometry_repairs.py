"""Exact, bounded runtime mesh repairs. Original owner files are never edited.

The manifest stores reviewed element patches and hashes, not a blanket geometry
normalizer. A changed importer must be reviewed rather than silently overwritten.
"""
import copy
import hashlib
import json
from functools import lru_cache
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / 'src/main/resources/assets/jugcraft/models'
MANIFEST = Path(__file__).with_name('imported_geometry_repairs.json')


def digest(elements):
    return hashlib.sha256(json.dumps(elements, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


@lru_cache(maxsize=1)
def records():
    return json.loads(MANIFEST.read_text(encoding='utf-8'))


def original_elements(path, elements):
    """Source-comparison view of an EXACT repaired mesh, including inherited ones."""
    current = digest(elements)
    for record in records().values():
        if current == record['after_sha256']:
            original = copy.deepcopy(elements)
            for index, change in record['elements'].items():
                original[int(index)] = change['before']
            if digest(original) == record['before_sha256']:
                return original
    return elements


def apply(root=MODELS):
    changed = 0
    for ref, record in json.loads(MANIFEST.read_text(encoding='utf-8')).items():
        path = root / ref
        model = json.loads(path.read_text(encoding='utf-8'))
        elements = model.get('elements', [])
        current = digest(elements)
        if current == record['after_sha256']:
            continue
        if current != record['before_sha256']:
            raise ValueError('Imported geometry changed; review before patching: ' + ref)
        result = copy.deepcopy(elements)
        for index, patch in record['elements'].items():
            result[int(index)] = patch['after']
        if digest(result) != record['after_sha256']:
            raise ValueError('Invalid geometry repair manifest: ' + ref)
        model['elements'] = result
        path.write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
        changed += 1
    return changed


if __name__ == '__main__':
    print('Applied runtime geometry repairs:', apply())
