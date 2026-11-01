"""Preserve and inventory NEWEST ADDITION as data; never execute supplied files."""
from pathlib import Path
from collections import Counter
import hashlib
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = Path('C:/Users/james/Downloads/NEWEST ADDITION')
DEST = ROOT / 'art/owner-library/originals/Edited-2026-10-29'
CAT = ROOT / 'art/owner-library/catalog/edited-2026-10-29'


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=True) + '\n', encoding='utf-8')


def main():
    old = {}
    for path in (ROOT / 'art/owner-library/catalog').rglob('*.json'):
        if path.parent == CAT or path.name not in ('files.json', 'manifest.json', 'source-manifest.json'):
            continue
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        for row in data.get('files', []) if isinstance(data, dict) else []:
            if isinstance(row, dict) and 'sha256' in row:
                old.setdefault(row['sha256'], str(path.relative_to(ROOT)))
    files, groups = [], {}
    for folder in sorted(SOURCE.iterdir()):
        if not folder.is_dir():
            continue
        group = {'files': 0, 'bytes': 0, 'extensions': Counter(), 'previouslyCataloguedIdentical': 0,
                 'namespaces': set(), 'blockstates': [], 'itemModels': [], 'entityModels': [],
                 'animations': [], 'recipes': Counter(), 'compiledClasses': 0,
                 'status': 'pending behavior/resource integration'}
        groups[folder.name] = group
        for path in sorted(folder.rglob('*')):
            if not path.is_file():
                continue
            rel = path.relative_to(SOURCE)
            raw = path.read_bytes()
            digest = hashlib.sha256(raw).hexdigest()
            target = DEST / rel
            if target.exists():
                if hashlib.sha256(target.read_bytes()).hexdigest() != digest:
                    raise ValueError('Preserved source changed; do not overwrite: ' + str(rel))
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(path, target)
            if hashlib.sha256(target.read_bytes()).hexdigest() != digest:
                raise ValueError('Copy verification failed: ' + str(rel))
            row = {'path': rel.as_posix(), 'bytes': len(raw), 'sha256': digest,
                   'role': 'reference_only' if path.suffix in ('.class', '.jar', '.py', '.exe', '.dll') else 'supplied_resource'}
            if digest in old:
                row['previousCatalog'] = old[digest]
                group['previouslyCataloguedIdentical'] += 1
            files.append(row)
            group['files'] += 1
            group['bytes'] += len(raw)
            group['extensions'][path.suffix] += 1
            group['compiledClasses'] += path.suffix == '.class'
            parts = path.relative_to(folder).parts
            if len(parts) > 2 and parts[0] in ('assets', 'data'):
                group['namespaces'].add(parts[1])
                resource = '/'.join(parts[3:])
                if parts[2] == 'blockstates': group['blockstates'].append(parts[1] + ':' + resource)
                if parts[2] == 'models' and len(parts) > 3 and parts[3] == 'item': group['itemModels'].append(parts[1] + ':' + '/'.join(parts[4:]))
                if parts[2] in ('geo', 'animations'): group['entityModels' if parts[2] == 'geo' else 'animations'].append(parts[1] + ':' + resource)
                if parts[2] in ('recipe', 'recipes') and path.suffix == '.json':
                    try: group['recipes'][json.loads(raw).get('type', 'no_type')] += 1
                    except (ValueError, UnicodeError): group['recipes']['unparsed'] += 1
        group['namespaces'] = sorted(group['namespaces'])
        if not group['files']: group['status'] = 'empty supplied folder; nothing to integrate'
        print(folder.name, group['files'], 'files;', group['previouslyCataloguedIdentical'], 'previously catalogued identical', flush=True)
    write(CAT / 'files.json', {'source': str(SOURCE), 'ownerDeclaration': 'Owner authorizes direct native Jugcraft integration of heavily edited files; legacy names retained from editing/testing. Preserve notices and originals.', 'sourceExecuted': False, 'files': files})
    write(CAT / 'audit.json', {'files': len(files), 'bytes': sum(row['bytes'] for row in files), 'collections': groups,
                            'preservationIsNotImplementation': True, 'compiledClassesAreReferenceOnly': True})
    print('Preserved and verified', len(files), 'files in', len(groups), 'folders.', flush=True)


if __name__ == '__main__': main()
