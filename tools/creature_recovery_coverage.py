"""Publish recovery coverage without treating parsed geometry as finished gameplay."""
from collections import Counter, defaultdict
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / 'src/main/resources/assets/jugcraft/recovered_creatures'
OUT = ROOT / 'art/owner-library/catalog/creature-recovery-2026-10-19'


def main():
    bindings = json.loads((MODELS / 'bindings.json').read_text())
    users = defaultdict(set)
    for species, binding in bindings.items():
        for variant in binding['variants'].values():
            for age in variant.values():
                users[age['model'].removeprefix('recovered/')].add(species)
    rows, outstanding = [], []
    for path in sorted(MODELS.glob('*.json')):
        if path.stem == 'bindings':
            continue
        data = json.loads(path.read_text())
        row = dict(model=path.stem, species=';'.join(sorted(users[path.stem])),
                   source=data.get('source', ''), parts=len(data['parts']),
                   keyframe_clips=len(data.get('actions', {})),
                   gait_channels=len(data.get('motions', [])),
                   expressions=len(data.get('expressions', [])),
                   poses=len(data.get('poses', [])),
                   known_unbound_calls=len(data.get('unboundAnimationCalls', [])))
        rows.append(row)
        for call in data.get('unboundAnimationCalls', []):
            outstanding.append(dict(model=path.stem, species=row['species'], **call))
    with (OUT / 'model-coverage.csv').open('w', newline='', encoding='utf-8') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    totals = Counter()
    for row in rows:
        if row['species']:
            totals.update({key: row[key] for key in ('keyframe_clips', 'gait_channels', 'expressions', 'poses', 'known_unbound_calls')})
    report = dict(boundSpecies=len(bindings), boundModels=len([r for r in rows if r['species']]),
                  generatedModels=len(rows), boundModelTotals=dict(totals),
                  note='Counts describe extracted declarations, not complete source behavior parity. Helper methods and custom render passes require manual review. Models with no species binding can be accessories or multipart pieces; their presence does not establish use in game.',
                  outstanding=outstanding)
    (OUT / 'animation-coverage.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({k: v for k, v in report.items() if k != 'outstanding'}, indent=2))


if __name__ == '__main__':
    main()
