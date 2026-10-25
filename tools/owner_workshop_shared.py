"""Retain the Oct22 workshop registrations when shared resources are regenerated.

Only explicit additions from the supplied-content integration are merged. Conflicting
registrations fail loudly; unrelated generated values and ordering remain intact.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'


def apply():
    additions = json.loads((ROOT / 'tools/data/owner-workshop-shared.json').read_text(encoding='utf-8'))
    for relative, patch in additions.items():
        target = (RES / relative).resolve()
        if not target.is_relative_to(RES.resolve()):
            raise ValueError('Workshop resource escapes resource directory')
        data = json.loads(target.read_text(encoding='utf-8'))
        if 'entries' in patch:
            for key, value in patch['entries'].items():
                if key in data and data[key] != value:
                    raise ValueError(f'Conflicting workshop registration: {relative}: {key}')
                data[key] = value
        else:
            rows = data[patch['list']]
            for row in patch['append']:
                if row not in rows:
                    rows.append(row)
        target.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


if __name__ == '__main__':
    apply()
