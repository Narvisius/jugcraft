"""Editable scary-creature resources, preserved by the shared deterministic generator."""
import json
from pathlib import Path


def write_all(write, resources, lang):
    data = json.loads(Path(__file__).with_suffix('.json').read_text(encoding='utf-8'))
    lang.update(data['language'])
    sound_file = resources / 'assets/jugcraft/sounds.json'
    sounds = json.loads(sound_file.read_text(encoding='utf-8'))
    sounds.update(data['sounds'])
    write(sound_file, sounds)
    for path, value in data['resources'].items():
        target = resources / path
        if '/tags/' in path and target.exists():
            existing = json.loads(target.read_text(encoding='utf-8'))
            value = {**existing, 'values': list(dict.fromkeys(existing['values'] + value['values']))}
        write(target, value)
