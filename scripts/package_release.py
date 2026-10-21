"""Offline release assembly from a built JAR and the verified dependency cache.

Produces a player pack, Windows import helper and hosting upload bundle. Never
starts Minecraft, edits a launcher profile, downloads code or deploys a server.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
import zipfile

import package_modrinth as pack

# Modrinth's published Windows installer records InstallLocation; passing a local
# .mrpack to its executable triggers its native RunMRPack import flow.
OPEN_MODRINTH = (
    "$app=Get-ItemProperty 'HKCU:/Software/Microsoft/Windows/CurrentVersion/Uninstall/*',"
    "'HKLM:/Software/Microsoft/Windows/CurrentVersion/Uninstall/*',"
    "'HKLM:/Software/WOW6432Node/Microsoft/Windows/CurrentVersion/Uninstall/*' -ErrorAction SilentlyContinue "
    "| Where-Object { $_.DisplayName -eq 'Modrinth App' -and $_.InstallLocation } | Select-Object -First 1; "
    "if ($app) { $exe=Join-Path $app.InstallLocation.Trim([char]34) 'Modrinth App.exe'; "
    "if (!(Test-Path -LiteralPath $exe -PathType Leaf)) { throw 'Modrinth App executable was not found.' }; "
    "Start-Process -FilePath $exe -ArgumentList (''+[char]34+$p+[char]34) } "
    "else { Start-Process -FilePath $p }"
)


def sha512(path: Path) -> str:
    digest = hashlib.sha512()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def installer(pack_name: str, digest: str) -> bytes:
    # Names come from the restricted release-version alphabet, never user paths.
    # The download location passes through an environment variable, not code.
    return (r'''@echo off
setlocal DisableDelayedExpansion
title Install JUGCRAFT
set "JUGCRAFT_PACK=%~dp0PACK_NAME"
echo Checking JUGCRAFT before opening the Modrinth import screen...
powershell.exe -NoProfile -Command "$ErrorActionPreference='Stop'; try { $p=$env:JUGCRAFT_PACK; if (!(Test-Path -LiteralPath $p -PathType Leaf)) { throw 'Extract the complete ZIP first, then run this file again.' }; if ((Get-FileHash -LiteralPath $p -Algorithm SHA512).Hash -ne 'PACK_HASH') { throw 'The pack checksum does not match. Download the release again.' }; Start-Process -FilePath $p } catch { Write-Host $_.Exception.Message; Write-Host 'Install Modrinth App from https://modrinth.com/app, then use Create instance > From file to import the .mrpack beside this file.'; exit 1 }"
if errorlevel 1 (pause & exit /b 1)
echo Choose Modrinth App if Windows asks, then confirm the import as JUGCRAFT.
echo No existing profiles or worlds have been changed by this helper.
pause
'''.replace('PACK_NAME', pack_name).replace('PACK_HASH', digest)
        .replace('Start-Process -FilePath $p', OPEN_MODRINTH).replace('\n', '\r\n')).encode('utf-8')


def online_installer(version: str, pack_name: str, digest: str) -> bytes:
    """One downloadable file, pinned to an immutable published release and hash."""
    url = f'https://github.com/jimbozoomer-byte/jugcraft/releases/download/pack-{version}/{pack_name}'
    command = (
        "$ErrorActionPreference='Stop'; try { "
        "$d=Join-Path ([IO.Path]::GetTempPath()) ('JUGCRAFT-'+[guid]::NewGuid().ToString('N')); "
        "New-Item -ItemType Directory -Path $d | Out-Null; "
        f"$p=Join-Path $d '{pack_name}'; "
        "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; "
        f"Invoke-WebRequest -UseBasicParsing -Uri '{url}' -OutFile $p; "
        f"if ((Get-FileHash -LiteralPath $p -Algorithm SHA512).Hash -ne '{digest}') "
        "{ throw 'Downloaded pack failed verification. Nothing will be installed.' }; "
        + OPEN_MODRINTH +
        " } catch { Write-Host $_.Exception.Message; "
        "Write-Host 'This requires a published GitHub release and Modrinth App (https://modrinth.com/app). "
        "You can also download the player ZIP and import its .mrpack manually.'; exit 1 }"
    )
    return ('@echo off\r\nsetlocal DisableDelayedExpansion\r\ntitle Install JUGCRAFT\r\n'
            f'echo Downloading verified JUGCRAFT {version}...\r\n'
            f'powershell.exe -NoProfile -Command "{command}"\r\n'
            'if errorlevel 1 (pause & exit /b 1)\r\n'
            'echo Confirm the JUGCRAFT import in Modrinth App.\r\npause\r\n').encode('utf-8')


def server_entries(lock: dict, jar: Path, cache: Path, root: Path):
    yield 'mods/jugcraft.jar', jar.read_bytes()
    for library in lock['dependencies']:
        if library['side'] != 'both':
            continue
        source = cache / library['filename']
        data = source.read_bytes()
        pack.verify_bytes(library, data)
        yield 'mods/' + library['filename'], data
    overrides = {}
    for folder in ('overrides', 'server-overrides'):
        base = root / 'distribution' / folder
        if not base.exists():
            continue
        for path in sorted(base.rglob('*')):
            pack.require(not path.is_symlink(), 'Server overrides cannot contain symlinks')
            if not path.is_file():
                continue
            name = path.relative_to(base).as_posix()
            # Deliberate allowlist: never copy saves, eula, server identity, player
            # lists, options.txt, local launcher files or client-only overrides.
            if name.split('/')[0] not in {'config', 'defaultconfigs'}:
                continue
            pack.require(path.suffix in {'.json', '.json5', '.toml', '.properties', '.txt'},
                         'Unsupported server configuration: ' + name)
            overrides[name] = path.read_bytes()
    yield from sorted(overrides.items())


def build_release(lock: dict, jar: Path, cache: Path, output: Path, version: str,
                  commit: str, root: Path = pack.ROOT) -> dict:
    pack.require(bool(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._+\-]{0,79}', version)), 'Unsafe release version')
    pack.require(bool(re.fullmatch(r'[0-9a-f]{40}', commit)), 'Supply the exact 40-character source commit')
    metadata = pack.inspect_jugcraft_jar(jar, lock)
    # Fail before producing a release if any server dependency is missing/corrupt.
    for library in lock['dependencies']:
        if library['side'] == 'both':
            pack.verify_bytes(library, (cache / library['filename']).read_bytes())
    pack.require(not output.exists() or not any(output.iterdir()),
                 'Release output must be empty; never overwrite an existing release')
    output.mkdir(parents=True, exist_ok=True)
    mrpack = pack.build_pack(lock, jar, output, version, root)
    # Separate the pack listing from the standalone mod's dependency metadata.
    (output / 'modrinth-pack-version.json').write_bytes(pack.json_bytes({
        'name': 'JUGCRAFT ' + version, 'version_number': version,
        'version_type': 'alpha', 'game_versions': [lock['minecraft']],
        'loaders': ['fabric'], 'dependencies': [], 'featured': False,
    }))
    manifest = {
        'schemaVersion': 1, 'name': 'JUGCRAFT', 'releaseVersion': version,
        'modVersion': metadata['version'], 'sourceCommit': commit,
        'minecraft': lock['minecraft'], 'fabricLoader': lock['fabricLoader'], 'java': lock['java'],
        'frameworkLockSha512': hashlib.sha512(pack.json_bytes(lock)).hexdigest(),
        'updateMethod': 'Modrinth Server Project linked pack; launcher confirmation required',
        'serverAddress': None, 'modrinthServerProject': None,
    }
    host = output / ('JUGCRAFT-server-' + version + '.zip')
    installed = []
    with zipfile.ZipFile(host, 'w') as archive:
        for name, data in server_entries(lock, jar, cache, root):
            pack.add_entry(archive, name, data)
            installed.append({'path': name, 'sha512': hashlib.sha512(data).hexdigest(), 'size': len(data)})
        pack.add_entry(archive, 'JUGCRAFT-SERVER-FILES.json', pack.json_bytes(installed))
        pack.add_entry(archive, 'JUGCRAFT-RELEASE.json', pack.json_bytes(manifest))
        pack.add_entry(archive, 'START-HERE.txt', (
            f'JUGCRAFT {version}\nSource: {commit}\n'
            f'Host runtime: Minecraft {lock["minecraft"]}, Fabric Loader {lock["fabricLoader"]}, Java {lock["java"]}.\n'
            'This upload bundle contains server-compatible mods and default configuration, not a Fabric launcher.\n'
            'Use your host panel to select the exact runtime. Stop the server before replacing files.\n'
            'FIRST INSTALL: upload mods/, config/ and defaultconfigs/ to a fresh staging server.\n'
            'UPDATES: back up world, playerdata, configs and old mods. Replace only the previous release\n'
            'managed mod files listed in JUGCRAFT-SERVER-FILES.json. Keep separately installed mods\n'
            'only after checking compatibility. Merge config changes; never overwrite live settings blindly.\n'
            'Existing world/serverconfig may override defaultconfigs; review it without resetting it.\n'
            'The owner must handle EULA acceptance, access lists, address, backups and host credentials.\n'
            'Start on a COPY of the world first; test joins and a restart, then schedule deployment.\n'
            'Rollback restores BOTH the matching world backup and the previous release files.\n'
            'No server deployment or human multiplayer test is implied by this package.\n'
        ).encode())
    player = output / ('JUGCRAFT-player-' + version + '.zip')
    with zipfile.ZipFile(player, 'w') as archive:
        pack.add_entry(archive, mrpack.name, mrpack.read_bytes())
        pack.add_entry(archive, 'Install-JUGCRAFT.cmd', installer(mrpack.name, sha512(mrpack)))
        pack.add_entry(archive, 'READ-ME.txt', (
            f'JUGCRAFT {version}\nWindows: extract this entire ZIP, then run Install-JUGCRAFT.cmd.\n'
            'It verifies the pack and opens its import screen using the registered .mrpack application.\n'
            'Choose Modrinth App and confirm the import. If no association exists, install Modrinth App\n'
            'from https://modrinth.com/app and use Create instance > From file with the included .mrpack.\n'
            'macOS/Linux: import the .mrpack directly in Modrinth App.\n'
            'The profile name is JUGCRAFT. Microsoft/Minecraft sign-in remains inside the launcher.\n'
            'This standalone file does not subscribe to automatic updates. For ongoing server play,\n'
            'use the owner\'s Modrinth Server Project Play link once it is configured; accept its linked\n'
            'pack so Modrinth can prompt you for the required update before launch.\n'
            'No launch, profile replacement, world changes or security-setting changes are automated.\n'
        ).encode())
    online = output / ('Install-JUGCRAFT-' + version + '.cmd')
    online.write_bytes(online_installer(version, mrpack.name, sha512(mrpack)))
    manifest['artifacts'] = [{'name': path.name, 'size': path.stat().st_size, 'sha512': sha512(path)}
                             for path in (mrpack, host, player, online)]
    (output / 'release-manifest.json').write_bytes(pack.json_bytes(manifest))
    (output / 'SHA512SUMS.txt').write_text(''.join(
        f'{entry["sha512"]}  {entry["name"]}\n' for entry in manifest['artifacts']), encoding='utf-8')
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', required=True, type=Path)
    parser.add_argument('--cache', type=Path, default=pack.ROOT / 'build/framework-cache')
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--version', required=True)
    parser.add_argument('--commit', required=True)
    args = parser.parse_args()
    try:
        lock = pack.load_lock()
        pack.check_repository(lock)
        result = build_release(lock, args.jar, args.cache, args.output, args.version, args.commit)
        print(json.dumps(result, indent=2))
        return 0
    except (ValueError, OSError, KeyError, zipfile.BadZipFile) as exc:
        print('Release assembly failed: ' + str(exc), file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
