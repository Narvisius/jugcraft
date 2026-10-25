"""Copy the owner's original wearable atlases; never redraw or execute source binaries.

The five native mesh classes are recovered declarative geometry from the matching
preserved classes. --recover-from accepts trusted CFR output for those five files
only; ordinary resource generation requires no decompiler or source executable.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art/owner-library/originals/Oct23/Jimtafacts"
TEXTURES = SOURCE / "assets/artifacts/textures/entity/wearable"
TARGET = ROOT / "src/main/resources/assets/jugcraft/textures/entity/jimtafacts/wearable"
JAVA = ROOT / "src/client/java/io/github/jimbozoomer/jugcraft/client/jimtafacts/mesh"
MESHES = ("ArmsMeshDefinitions", "BeltMeshDefinitions", "BodyMeshDefinitions", "HeadMeshDefinitions", "LegsMeshDefinitions")


def recover(decompiled: Path):
    """Transcribe inspected, declarative model builders, preserving UVs and poses."""
    JAVA.mkdir(parents=True, exist_ok=True)
    for name in MESHES:
        source = decompiled / "artifacts/client/item/mesh" / (name + ".java")
        text = source.read_text(encoding="utf-8")
        text = re.sub(r"\A/\*.*?\*/\s*", "", text, count=1, flags=re.S)
        text = text.replace("package artifacts.client.item.mesh;", "package io.github.jimbozoomer.jugcraft.client.jimtafacts.mesh;")
        header = ("/* Owner-supplied Jimtafacts geometry, recovered from artifacts/client/item/mesh/" + name + ".class.\n"
                  " * Cube dimensions, UVs, deformation and part poses are retained; package adapted for Jugcraft.\n"
                  " * Source and provenance: art/owner-library/originals/Oct23/Jimtafacts; see jimtafacts-wearables.md.\n"
                  " * Integration assistance: OpenAI Codex (GPT-6).\n */\n")
        (JAVA / (name + ".java")).write_text(header + text, encoding="utf-8", newline="\n")


def apply():
    copies = []
    for source in sorted(TEXTURES.rglob("*")):
        if not source.is_file() or source.suffix not in (".png", ".mcmeta"):
            continue
        destination = TARGET / source.relative_to(TEXTURES)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        copies.append({"source": source.relative_to(ROOT).as_posix(),
                       "runtime": destination.relative_to(ROOT).as_posix(),
                       "sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
                       "changes": "None; byte-identical wearable atlas."})
    geometry = []
    for name in MESHES:
        original = SOURCE / "artifacts/client/item/mesh" / (name + ".class")
        geometry.append({"source": original.relative_to(ROOT).as_posix(),
                         "sha256": hashlib.sha256(original.read_bytes()).hexdigest(),
                         "runtime": (JAVA / (name + ".java")).relative_to(ROOT).as_posix(),
                         "changes": "Declarative geometry transcribed; package renamed. No source bytecode loaded."})
    report = ROOT / "art/owner-library/catalog/edited-2026-10-23-jimtafacts/wearable-copies.json"
    report.write_text(json.dumps({"textures": copies, "geometry": geometry}, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--recover-from", type=Path)
    args = parser.parse_args()
    if args.recover_from:
        recover(args.recover_from)
    apply()
