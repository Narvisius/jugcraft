"""Create a direct-download player manifest; never promotes it or edits a profile."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile
import package_modrinth as pack

BASE = 'https://github.com/jimbozoomer-byte/jugcraft/releases/download/'


def build(mrpack: Path, output: Path, commit: str) -> dict:
    pack.require(re.fullmatch(r'[0-9a-f]{40}', commit), 'Exact source commit required')
    pack.require(not output.exists() or not any(output.iterdir()), 'Output must be empty')
    with zipfile.ZipFile(mrpack) as archive:
        names = archive.namelist()
        pack.require(len(names) == len(set(names)), 'Duplicate pack entries')
        index = json.loads(archive.read('modrinth.index.json'))
        version = index['versionId']
        pack.require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._+\-]{0,79}', version), 'Unsafe version')
        base = BASE + 'pack-' + version + '/'
        files = []
        for row in index['files']:
            pack.require(re.fullmatch(r'mods/[A-Za-z0-9][A-Za-z0-9._+\-]*\.jar', row['path']), 'Only managed mod downloads supported')
            pack.require(row.get('env', {}).get('client') != 'unsupported', 'Server-only mod in player pack')
            files.append({'path': row['path'], 'sha512': row['hashes']['sha512'],
                          'size': row['fileSize'], 'url': row['downloads'][0], 'policy': 'managed'})
        jar = archive.read('overrides/mods/jugcraft.jar')
        files.append({'path': 'mods/jugcraft.jar', 'size': len(jar),
                      'sha512': hashlib.sha512(jar).hexdigest(), 'url': base + 'jugcraft-client.jar', 'policy': 'managed'})
        # Configs and options deliberately stay owned by the player. The original
        # import supplies defaults; future migrations require an explicit design.
        pack.require(len({f['path'].lower() for f in files}) == len(files), 'Duplicate managed paths')
        manifest = {'schemaVersion': 1, 'updaterVersion': 1, 'releaseVersion': version,
                    'sourceCommit': commit, 'minecraft': index['dependencies']['minecraft'],
                    'fabricLoader': index['dependencies']['fabric-loader'], 'java': 25,
                    'serverAddress': 'jugcraft.modrinth.gg', 'files': files,
                    'pack': {'url': base + mrpack.name, 'size': mrpack.stat().st_size,
                             'sha512': hashlib.sha512(mrpack.read_bytes()).hexdigest()}}
    output.mkdir(parents=True, exist_ok=True)
    (output / 'jugcraft-client.jar').write_bytes(jar)
    encoded = pack.json_bytes(manifest)
    (output / 'player-update.json').write_bytes(encoded)
    channel = {'schemaVersion': 1, 'releaseVersion': version, 'manifestUrl': base + 'player-update.json',
               'manifestSha512': hashlib.sha512(encoded).hexdigest()}
    (output / 'stable.json').write_bytes(pack.json_bytes(channel))
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pack', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--commit', required=True)
    args = parser.parse_args()
    result = build(args.pack, args.output, args.commit)
    print(f'Prepared {result["releaseVersion"]}: {len(result["files"])} managed mods. NOT promoted.')
