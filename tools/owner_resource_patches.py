"""Apply explicit authored resource repairs after canonical generation.

Preconditions make upstream generator changes visible instead of silently replacing
new content. These are data patches; no supplied library code is executed.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'


def apply():
    patches = json.loads((ROOT / 'tools/data/owner-resource-patches.json').read_text(encoding='utf-8'))
    for resource in patches['resources']:
        target = (RES / resource['file']).resolve()
        if not target.is_relative_to(RES.resolve()):
            raise ValueError('Resource patch escapes the resource directory')
        data = json.loads(target.read_text(encoding='utf-8'))
        for change in resource['changes']:
            parent = data
            for key in change['path'][:-1]:
                parent = parent[key]
            key = change['path'][-1]
            if change['op'] == 'add':
                if key in parent and parent[key] != change['after']:
                    raise ValueError(f'Authored resource addition conflicts: {target}: {change["path"]}')
                parent[key] = change['after']
            elif change['op'] == 'remove':
                if key not in parent:
                    continue
                if parent[key] != change['before']:
                    raise ValueError(f'Generated resource changed: {target}: {change["path"]}')
                del parent[key]
            else:
                if parent.get(key) not in (change['before'], change['after']):
                    raise ValueError(f'Generated resource changed: {target}: {change["path"]}')
                parent[key] = change['after']
        target.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


if __name__ == '__main__':
    apply()
