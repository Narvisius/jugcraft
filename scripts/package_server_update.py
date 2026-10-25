"""Build a non-destructive changed-mods ZIP from two verified server bundles.

Only changed/new managed mods enter the ZIP. Removed mods and changed defaults
are listed for explicit review, never deleted or overwritten by this tool.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import zipfile

import package_modrinth as pack


def inventory(archive: zipfile.ZipFile) -> dict:
    rows = json.loads(archive.read('JUGCRAFT-SERVER-FILES.json'))
    result = {}
    names = archive.namelist()
    pack.require(len(names) == len(set(names)), 'Duplicate ZIP paths')
    for row in rows:
        name = row['path']
        parts = PurePosixPath(name).parts
        pack.require(not name.startswith('/') and '\\' not in name and ':' not in name
                     and '..' not in parts and len(parts) >= 2, 'Unsafe managed path')
        pack.require(parts[0] in {'mods', 'config', 'defaultconfigs'}, 'Unexpected managed folder')
        pack.require(parts[0] != 'mods' or len(parts) == 2 and name.endswith('.jar'), 'Invalid mod path')
        pack.require(name not in result, 'Duplicate managed path')
        data = archive.read(name)
        pack.require(len(data) == row['size'] and hashlib.sha512(data).hexdigest() == row['sha512'],
                     'Managed file integrity mismatch: ' + name)
        result[name] = row
    return result


def build_update(previous: Path, current: Path, output: Path) -> dict:
    pack.require(not output.exists() or not any(output.iterdir()), 'Update output must be empty')
    with zipfile.ZipFile(previous) as old, zipfile.ZipFile(current) as new:
        before, after = inventory(old), inventory(new)
        changed = [name for name in after if name not in before or before[name]['sha512'] != after[name]['sha512']]
        mods = sorted(name for name in changed if name.startswith('mods/'))
        removals = sorted(name for name in before if name.startswith('mods/') and name not in after)
        configs = sorted(name for name in changed if not name.startswith('mods/'))
        old_release = json.loads(old.read('JUGCRAFT-RELEASE.json'))
        new_release = json.loads(new.read('JUGCRAFT-RELEASE.json'))
        same_platform = all(old_release[k] == new_release[k] for k in ('minecraft', 'fabricLoader', 'java'))
        pack.require(same_platform, 'Platform changed: update the host runtime separately; no automatic patch produced')
        output.mkdir(parents=True, exist_ok=True)
        target = output / 'JUGCRAFT-changed-mods.zip'
        with zipfile.ZipFile(target, 'x') as archive:
            for name in mods:
                pack.add_entry(archive, name, new.read(name))
        report = {
            'fromVersion': old_release['releaseVersion'], 'toVersion': new_release['releaseVersion'],
            'replacements': [after[name] for name in mods],
            'removeOldManagedModsManually': removals,
            'mergeChangedDefaultsManually': configs,
            'zipSha512': hashlib.sha512(target.read_bytes()).hexdigest(),
            'instructions': 'Obtain the owner\'s explicit restart confirmation, notify players in-game, '
                'and wait the full 30 seconds. Back up and stop the server. Extract into the server root. '
                'Review listed removed managed mods before restart. Merge config defaults deliberately. '
                'No worlds, inventories, server settings, deletions or configs are included in this ZIP. '
                'Verify startup and a matching client join before promoting this release through '
                'scripts/promote_player_update.py. Do not submit or select a public Modrinth Server Project pack.',
        }
        (output / 'UPDATE-PLAN.json').write_bytes(pack.json_bytes(report))
        return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--previous', type=Path, required=True)
    parser.add_argument('--current', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(build_update(args.previous, args.current, args.output), indent=2))
