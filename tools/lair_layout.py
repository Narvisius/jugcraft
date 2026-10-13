"""What every lair's template generator shares (tools/hollow_acre.py, tools/spindle_loft.py): palette entries checked
against the blocks' own blockstate files, a deterministic hash for scattering things, and the structure template file in
26.3's format (tools/retro_game_shop.py's: palette entries {"id", "properties"}, DataVersion 26.3's).
"""
import json
from pathlib import Path

from lairs import LAIR_BLOCKS
from retro_game_shop import DATA_VERSION, nbt_bytes

ROOT = Path(__file__).resolve().parents[1]
BLOCKSTATES = ROOT / "src" / "main" / "resources" / "assets" / "jugcraft" / "blockstates"

# Every lair's own blocks: their blockstate files are written with the lairs' data, so they are not read here.
LAIR_ONLY = set(LAIR_BLOCKS)


def _jugcraft_properties(block):
    """The properties and values a Jugcraft block's blockstate file uses."""
    data = json.loads((BLOCKSTATES / f"{block}.json").read_text(encoding="utf-8"))
    found = {}
    if "variants" in data:
        for key in data["variants"]:
            for pair in filter(None, key.split(",")):
                name, value = pair.split("=")
                found.setdefault(name, set()).add(value)
    for part in data.get("multipart", []):
        for when in [part.get("when", {})] + part.get("when", {}).get("OR", []):
            for name, value in when.items():
                if name not in ("OR", "AND"):
                    found.setdefault(name, set()).update(str(value).split("|"))
    return found


def block(name, **properties):
    """A palette entry. Jugcraft blocks are checked against their blockstate files, so a renamed property fails here."""
    props = {key: str(value).lower() for key, value in properties.items()}
    if name.startswith("jugcraft:") and name[9:] not in LAIR_ONLY:
        known = _jugcraft_properties(name[9:])
        for key, value in props.items():
            if key not in known or value not in known[key]:
                raise ValueError(f"{name}: no {key}={value} in its blockstate ({sorted(known)})")
    return name, tuple(sorted(props.items()))


def hash01(*values):
    """A deterministic 0..1 for a position and a salt."""
    h = 2166136261
    for v in values:
        h = ((h ^ (int(v) & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * 1274126177) & 0xFFFFFFFF
    return (h ^ (h >> 16)) / 0xFFFFFFFF


def template(blocks, size):
    """The structure template of {(x, y, z): state} (no air: a lair's empty cells stay as the void left them)."""
    palette = sorted(set(blocks.values()))
    index = {state: i for i, state in enumerate(palette)}
    return {
        "DataVersion": DATA_VERSION,
        "size": list(size),
        "palette": [{"id": name, **({"properties": dict(props)} if props else {})} for name, props in palette],
        "blocks": [{"pos": list(pos), "state": index[blocks[pos]]} for pos in sorted(blocks)],
        "entities": [],
    }


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(nbt_bytes(data))
