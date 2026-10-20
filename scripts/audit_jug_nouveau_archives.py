"""Read owner archives as ZIP data, preserve colliding resource variants, never execute code.

Usage: python scripts/audit_jug_nouveau_archives.py <core.jar> <elemental.jar>
Compiled classes/dependencies stay in the local archives, not in Jugcraft's JAR.
"""
from pathlib import Path, PurePosixPath
from zipfile import ZipFile
import csv
import hashlib
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
LIBRARY = ROOT / "art/owner-library/originals/Magic"
DEST = ROOT / "art/owner-library/ports/jug-nouveau"
ARCHIVES = ("ars_jimbaux-1.21.1-5.13.3.jar", "ars_jymbaumental-1.21.1-0.7.10.4.jar")


def digest(data):
    return hashlib.sha256(data).hexdigest()


def audit(paths):
    DEST.mkdir(parents=True, exist_ok=True)
    records, summaries = [], []
    for expected, path in zip(ARCHIVES, paths, strict=True):
        if path.name != expected:
            raise ValueError(f"Expected {expected}, got {path.name}; review a new version separately")
        summary = {"archive": path.name, "sha256": digest(path.read_bytes()),
                   "compiled_classes": 0, "source_files": 0, "resources_in_original_library": 0,
                   "preserved_resource_variants": 0, "preserved_metadata": 0}
        with ZipFile(path) as archive:
            for info in archive.infolist():
                if info.is_dir():
                    continue
                name = PurePosixPath(info.filename)
                if name.is_absolute() or ".." in name.parts or "\\" in info.filename or ":" in info.filename:
                    raise ValueError(f"Unsafe archive path: {info.filename}")
                summary["compiled_classes"] += name.suffix == ".class"
                summary["source_files"] += name.suffix in (".java", ".kt")
                is_resource = name.parts[0] in ("assets", "data")
                is_metadata = (name.parts[0] == "META-INF" and name.suffix != ".jar") or info.filename in (
                    "pack.mcmeta", "ars_nouveau.mixins.json", "ars_elemental.mixins.json", "ars_elemental_logo.png")
                if not is_resource and not is_metadata:
                    continue
                data = archive.read(info)
                original = LIBRARY.joinpath(*name.parts)
                if is_resource and original.is_file() and original.read_bytes() == data:
                    target = original
                    status = "preserved_in_original_library"
                    summary["resources_in_original_library"] += 1
                else:
                    # Namespace-shared tags may differ between two source mods. Keep both
                    # versions instead of overwriting whichever source the first upload held.
                    target = DEST / path.stem / Path(*name.parts)
                    target.parent.mkdir(parents=True, exist_ok=True)
                    if target.exists() and target.read_bytes() != data:
                        raise ValueError(f"Preserved source differs: {target}; keep it and review this archive separately")
                    target.write_bytes(data)
                    status = "metadata_reference" if is_metadata else "preserved_resource_variant"
                    summary["preserved_metadata" if is_metadata else "preserved_resource_variants"] += 1
                records.append({"archive": path.name, "source": info.filename, "bytes": len(data),
                                "sha256": digest(data), "preserved_at": target.relative_to(ROOT).as_posix(), "status": status})
        summaries.append(summary)
    with (DEST / "resources.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=records[0].keys())
        writer.writeheader()
        writer.writerows(records)
    (DEST / "summary.json").write_text(json.dumps({"archives": summaries, "executed_archive_code": False,
        "runtime_compatibility": "NeoForge 1.21.1 archives; gameplay requires a Fabric 26.3 port, not a drop-in copy.",
        "resources": sum(r["resources_in_original_library"] + r["preserved_resource_variants"] for r in summaries)}, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summaries, indent=2))


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    audit([Path(arg) for arg in sys.argv[1:]])
