"""Read-only completeness audit of the owner's October 18 resource collection.

This inventories inputs; it does not register content or certify gameplay parity.
Run with --write to refresh the recorded audit after adding missing source inputs.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/owner-library/originals/Edited-2026-10-18'
CATALOG = ROOT / 'art/owner-library/catalog/edited-2026-10-18'


def audit():
    manifest = json.loads((CATALOG / 'files.json').read_text(encoding='utf-8'))
    errors = []
    for record in manifest['files']:
        path = SOURCE / record['path']
        data = path.read_bytes()
        if len(data) != record['bytes'] or hashlib.sha256(data).hexdigest() != record['sha256']:
            errors.append('Original changed: ' + record['path'])
    parsed = {}
    lenient_files = []
    for path in SOURCE.rglob('*.json'):
        try:
            text = path.read_text(encoding='utf-8-sig')
            try:
                value = json.loads(text)
            except ValueError:
                # Older resource packs use comments/trailing commas. Preserve originals,
                # decode these explicitly for the inventory, and record each conversion.
                text = re.sub(r'("(?:\\.|[^"\\])*")|//[^\n]*|/\*[\s\S]*?\*/',
                              lambda m: m.group(1) or '', text)
                text = re.sub(r'("(?:\\.|[^"\\])*")|,\s*(?=[}\]])',
                              lambda m: m.group(1) or '', text)
                value = json.loads(text)
                lenient_files.append(path.relative_to(SOURCE).as_posix())
            parsed[path.relative_to(SOURCE).as_posix()] = value
        except (UnicodeError, ValueError) as exc:
            errors.append(f'{path.relative_to(SOURCE)}: {exc}')
    locations, processors = set(), set()

    def walk(value):
        if isinstance(value, dict):
            if value.get('element_type') == 'minecraft:single_pool_element':
                locations.add(value['location'])
                processor = value.get('processors')
                if isinstance(processor, str) and not processor.startswith('minecraft:'):
                    processors.add(processor)
            for child in value.values():
                walk(child)
        elif isinstance(value, list):
            for child in value:
                walk(child)

    for name, data in parsed.items():
        if name.startswith('structures/'):
            walk(data)
    animals = parsed['jimbos_heavily modified naturalist animals/lang/en_us.json']
    mounts = []
    for name, data in parsed.items():
        if name.startswith('mythicmounts/geo/'):
            stem = Path(name).name.removesuffix('.geo.json')
            animation = 'mythicmounts/animations/' + stem + '.animation.json'
            mounts.append({'model': name, 'animation': animation,
                           'animationPresent': animation in parsed,
                           'clips': list(parsed.get(animation, {}).get('animations', {}))})
    return {
        'verifiedOriginals': len(manifest['files']), 'jsonFiles': len(parsed),
        'jsonFilesWithCommentsOrTrailingCommas': lenient_files,
        'sourceCodeFiles': [str(p.relative_to(SOURCE)) for p in SOURCE.rglob('*') if p.suffix in {'.java', '.kt'}],
        'naturalistEntityLabels': {k: v for k, v in animals.items() if k.startswith('entity.naturalist.')},
        'naturalistEntityModelFiles': [name for name in parsed if name.startswith('jimbos_heavily modified naturalist animals/') and ('.geo.' in name or '/models/entity/' in name)],
        'mounts': mounts,
        'referencedStructureTemplatesNotSupplied': sorted(locations),
        'referencedCustomProcessorsNotSupplied': sorted(processors),
        'suppliedStructureNbtFiles': sorted(str(p.relative_to(SOURCE)) for p in SOURCE.rglob('*.nbt')),
        'validationErrors': errors,
        'runtimeImplemented': False,
        'limitation': 'Resource names and language descriptions do not contain the owner-edited gameplay algorithms. Structure pool JSON does not contain building blocks. Entity textures do not contain model geometry.'
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    result = audit()
    if args.write:
        (CATALOG / 'audit.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(f"Verified {result['verifiedOriginals']} originals; parsed {result['jsonFiles']} JSON files.")
    print(f"Source code files: {len(result['sourceCodeFiles'])}; mount model/animation pairs: {len(result['mounts'])}.")
    print(f"Structure templates missing from supplied collection: {len(result['referencedStructureTemplatesNotSupplied'])}.")
    for error in result['validationErrors']:
        print(error)
    raise SystemExit(bool(result['validationErrors']))
