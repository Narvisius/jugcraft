"""Small Discord download opening the stable Modrinth linked-server install page.

This opens a website, not Minecraft. Modrinth owns installation, account sign-in,
and update consent. A project must be approved before non-members can join.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import zipfile

import package_modrinth as pack


def load_settings(root: Path = pack.ROOT) -> dict:
    settings = json.loads((root / 'distribution/server-project.json').read_text(encoding='utf-8'))
    pack.require(bool(re.fullmatch(r'https://modrinth\.com/server/[a-zA-Z0-9_-]+', settings['projectUrl'])),
                 'Expected a Modrinth server project URL')
    pack.require(bool(re.fullmatch(r'[a-zA-Z0-9.-]+(?::[0-9]{1,5})?', settings['serverAddress'])),
                 'Invalid server address')
    return settings


def build_join(output: Path, root: Path = pack.ROOT) -> Path:
    settings = load_settings(root)
    output.mkdir(parents=True, exist_ok=True)
    target = output / 'JUGCRAFT-Join.zip'
    pack.require(not target.exists(), 'Never replace an existing join bundle')
    url = settings['projectUrl']
    readme = (
        'JUGCRAFT - install once, keep the server instance linked\n\n'
        '1. Install Modrinth App from https://modrinth.com/app and sign into Minecraft.\n'
        '2. Open Join-JUGCRAFT.url, or paste this address into your browser:\n'
        f'   {url}\n'
        '3. Click Play, allow the website to open Modrinth App, and accept the required pack.\n'
        '4. Launch the linked JUGCRAFT server instance for future sessions.\n\n'
        'When the server owner publishes a compatibility update, the launcher prompts you\n'
        'to accept it before the next launch. Keep the instance linked to receive updates.\n'
        'Do not import a loose .mrpack if you want this server update subscription.\n'
        'This shortcut opens the official website; it does not run an installer or change files.\n'
        'macOS/Linux: use the same website link in your browser.\n\n'
        f'Server: {settings["serverAddress"]}\n'
        'Minecraft 26.3 / Fabric. The selected server pack supplies the correct versions.\n'
        'B: encyclopedia. M: map. N: backpack. R: reload.\n\n'
        'The server project must be approved by Modrinth before public access is available.\n'
        'If the page is unavailable, wait for the owner\'s launch announcement.\n'
    )
    with zipfile.ZipFile(target, 'x') as archive:
        pack.add_entry(archive, 'Join-JUGCRAFT.url', ('[InternetShortcut]\r\nURL=' + url + '\r\n').encode())
        pack.add_entry(archive, 'READ-ME.txt', readme.encode())
    return target


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    print(build_join(args.output))
