"""Explicit promotion of a tested, deployed player version; dry run by default.

The server version is an operator declaration, not a remote server health check.
Only the channel pointer is mutable. Fixed release payloads are never replaced.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
from urllib.request import Request, urlopen

REPO = 'jimbozoomer-byte/jugcraft'
BASE = 'https://github.com/' + REPO + '/releases/download/'


def gh(*args):
    return subprocess.check_output(['gh', *args, '--repo', REPO], text=True)


def validate(candidate: dict, server_version: str):
    version = candidate['releaseVersion']
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._+\-]{0,79}', version):
        raise ValueError('Unsafe release version')
    if candidate.get('schemaVersion') != 1 or version != server_version:
        raise ValueError('Candidate must match the operator-confirmed live server version')
    if candidate['manifestUrl'] != BASE + 'pack-' + version + '/player-update.json':
        raise ValueError('Manifest must belong to the exact Jugcraft release')
    if not re.fullmatch('[0-9a-f]{128}', candidate['manifestSha512']):
        raise ValueError('Invalid manifest checksum')
    return version


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--candidate', type=Path, required=True)
    parser.add_argument('--server-version', required=True, help='Version already deployed and startup-verified by operator')
    parser.add_argument('--publish', action='store_true', help='Actually promote; otherwise validate only')
    args = parser.parse_args()
    candidate = json.loads(args.candidate.read_text(encoding='utf-8'))
    version = validate(candidate, args.server_version)
    release = json.loads(gh('release', 'view', 'pack-' + version, '--json', 'isDraft,assets'))
    if release['isDraft']:
        raise ValueError('Publish the fixed release first')
    with urlopen(Request(candidate['manifestUrl'], headers={'User-Agent': 'JUGCRAFT-release-check'}), timeout=30) as response:
        body = response.read(2 * 1024 * 1024 + 1)
    if hashlib.sha512(body).hexdigest() != candidate['manifestSha512']:
        raise ValueError('Public manifest differs from the tested candidate')
    manifest = json.loads(body)
    if manifest['releaseVersion'] != version:
        raise ValueError('Manifest version mismatch')
    by_name = {a['name']: a for a in release['assets']}
    for row in [manifest['pack'], *manifest['files']]:
        if row['url'].startswith(BASE):
            prefix = BASE + 'pack-' + version + '/'
            if not row['url'].startswith(prefix):
                raise ValueError('Payload points at a different release')
            asset = by_name.get(row['url'][len(prefix):])
            if asset is None or asset['size'] != row['size']:
                raise ValueError('Public payload is missing or wrong-sized: ' + row['url'])
    print('Verified public manifest and release payload sizes for ' + version)
    print('Live-server startup/version confirmation is supplied by the operator, not probed by this tool.')
    if not args.publish:
        print('DRY RUN: channel unchanged. Add --publish only after server deployment is verified.')
        return
    # The stable basename is intentional: do not upload a differently named pointer.
    if args.candidate.name != 'stable.json':
        raise ValueError('Candidate must be named stable.json')
    exists = subprocess.run(['gh', 'release', 'view', 'player-channel', '--repo', REPO],
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0
    if not exists:
        gh('release', 'create', 'player-channel', '--target', manifest['sourceCommit'], '--prerelease',
           '--title', 'JUGCRAFT player update channel', '--notes',
           'Owner-promoted update pointer for the JUGCRAFT installer. Game payloads remain in immutable versioned releases.')
    gh('release', 'upload', 'player-channel', str(args.candidate.resolve()), '--clobber')
    print('PROMOTED: ' + version)


if __name__ == '__main__':
    main()
