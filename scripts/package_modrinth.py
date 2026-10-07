"""Validate the reviewed library lock and build a reproducible, offline .mrpack.

Only the explicit `verify` command downloads library artifacts. Neither the mod
nor pack creation downloads executable code. The player's launcher installs the
locked files when they choose to import/install the resulting pack.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import sys
from urllib.parse import urlparse
from urllib.request import Request, urlopen
import zipfile

ROOT = Path(__file__).resolve().parents[1]
LOCK = ROOT / "distribution/frameworks.lock.json"
DOWNLOAD_HOSTS = {"cdn.modrinth.com", "github.com", "raw.githubusercontent.com", "gitlab.com"}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def json_bytes(value: object) -> bytes:
    return (json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def load_lock(path: Path = LOCK) -> dict:
    lock = json.loads(path.read_text(encoding="utf-8"))
    validate_lock(lock)
    return lock


def validate_lock(lock: dict) -> None:
    require(lock.get("schemaVersion") == 1, "Unsupported framework lock schema")
    for field in ("minecraft", "fabricLoader", "kotlin"):
        require(isinstance(lock.get(field), str) and bool(lock[field]), f"Missing {field} pin")
    require(lock.get("java") == 25, "Unexpected Java platform; review platform changes explicitly")
    libraries = lock.get("dependencies", [])
    require(bool(libraries), "The pack must include its reviewed dependencies")
    for field in ("key", "modId", "filename"):
        values = [library[field] for library in libraries]
        require(len(set(values)) == len(values), f"Duplicate dependency {field}")
    projects = {library["modrinthProject"]: library["modrinthVersion"]
                for library in libraries if library["modrinthProject"]}
    embedded = {project for library in libraries for project in library["embeddedProjects"]}
    for library in libraries:
        name = library["key"]
        filename = library["filename"]
        require(bool(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._+\-]*\.jar", filename)),
                f"Unsafe artifact filename: {filename}")
        require(library["side"] in {"both", "client"}, f"Invalid side for {name}")
        require(isinstance(library["requiredByJugcraft"], bool), f"Missing dependency policy for {name}")
        require(not library["requiredByJugcraft"] or library["side"] == "both",
                f"Client-only {name} cannot be required by the common Jugcraft manifest")
        require(isinstance(library["size"], int) and library["size"] > 0, f"Invalid size for {name}")
        for algorithm, length in (("sha1", 40), ("sha512", 128)):
            require(bool(re.fullmatch(r"[0-9a-f]{%d}" % length, library["hashes"].get(algorithm, ""))),
                    f"Missing or invalid {algorithm} for {name}")
        require(bool(library["downloads"]), f"No authorized download for {name}")
        for url in library["downloads"]:
            parsed = urlparse(url)
            require(parsed.scheme == "https" and parsed.hostname in DOWNLOAD_HOSTS
                    and not parsed.username and not parsed.password and not parsed.fragment,
                    f"Unsupported pack download URL for {name}")
        project, version = library["modrinthProject"], library["modrinthVersion"]
        require(bool(project) == bool(version), f"Incomplete Modrinth pin for {name}")
        if project:
            require(library["maven"] == f"maven.modrinth:{project}:{version}",
                    f"Development and pack versions differ for {name}")
        for dependency in library["requiredProjects"]:
            dep = dependency["project"]
            require(dep in projects or dep in embedded, f"{name} needs missing project {dep}")
            if dependency["version"]:
                require(projects.get(dep) == dependency["version"],
                        f"{name} needs exact dependency version {dependency['version']}")
        require(bool(library["license"]) and bool(library["sourceUrl"]), f"Missing provenance for {name}")


def check_repository(lock: dict, root: Path = ROOT) -> None:
    properties = dict(re.findall(r"^([A-Za-z_]+)=(.+)$",
                                (root / "gradle.properties").read_text(), re.MULTILINE))
    for property_name, lock_name in (("minecraft_version", "minecraft"),
                                    ("loader_version", "fabricLoader"), ("kotlin_version", "kotlin")):
        require(properties[property_name] == lock[lock_name], f"Drift in {property_name}")
    by_key = {library["key"]: library for library in lock["dependencies"]}
    for property_name, key in (("fabric_api_version", "fabric-api"), ("jei_version", "jei")):
        require(properties[property_name] == by_key[key]["modVersion"], f"Drift in {property_name}")
    source = json.loads((root / "src/main/resources/fabric.mod.json").read_text())
    require(source["depends"]["fabricloader"] == ">=" + lock["fabricLoader"], "Fabric metadata loader drift")
    require(source["depends"]["minecraft"] == "~" + lock["minecraft"], "Fabric metadata Minecraft drift")


def verify_bytes(library: dict, data: bytes) -> None:
    require(len(data) == library["size"], f"Size mismatch: {library['key']}")
    for algorithm in ("sha1", "sha512"):
        require(hashlib.new(algorithm, data).hexdigest() == library["hashes"][algorithm],
                f"{algorithm} mismatch: {library['key']}")


def verify_downloads(lock: dict, cache: Path) -> None:
    cache.mkdir(parents=True, exist_ok=True)
    for library in lock["dependencies"]:
        destination = cache / library["filename"]
        if destination.exists():
            data = destination.read_bytes()
        else:
            request = Request(library["downloads"][0], headers={
                "User-Agent": "Jugcraft-pack-verifier/1.0 (https://github.com/jimbozoomer-byte/jugcraft)"})
            with urlopen(request, timeout=90) as response:
                data = response.read(library["size"] + 1)
            verify_bytes(library, data)
            destination.write_bytes(data)
        verify_bytes(library, data)
        print(f"Verified {library['key']} {library['modVersion']}")


def inspect_jugcraft_jar(jar: Path, lock: dict) -> dict:
    with zipfile.ZipFile(jar) as archive:
        metadata = json.loads(archive.read("fabric.mod.json"))
        require(metadata.get("id") == "jugcraft", "Input is not the Jugcraft mod")
        require(bool(metadata.get("version")) and "${" not in metadata["version"], "Unbuilt Jugcraft metadata")
        require(metadata["depends"].get("minecraft") == "~" + lock["minecraft"], "Wrong Minecraft build")
        require(metadata["depends"].get("fabricloader") == ">=" + lock["fabricLoader"], "Wrong loader build")
        for library in lock["dependencies"]:
            if library["requiredByJugcraft"]:
                require(metadata["depends"].get(library["modId"]) == ">=" + library["modVersion"],
                        f"Built JAR is missing the locked {library['key']} requirement")
        require(not any(name.endswith(".jar") for name in archive.namelist()),
                "Jugcraft must not bundle third-party mod JARs")
    return metadata


def make_index(lock: dict, version: str) -> dict:
    return {
        "formatVersion": 1, "game": "minecraft", "versionId": version,
        "name": "Jugcraft Complete",
        "summary": "Jugcraft with its pinned animation, magic, interface, and presentation libraries.",
        "files": [{
            "path": "mods/" + library["filename"],
            "hashes": {algorithm: library["hashes"][algorithm] for algorithm in ("sha1", "sha512")},
            "env": {"client": "required", "server": "required" if library["side"] == "both" else "unsupported"},
            "downloads": library["downloads"], "fileSize": library["size"],
        } for library in lock["dependencies"]],
        "dependencies": {"minecraft": lock["minecraft"], "fabric-loader": lock["fabricLoader"]},
    }


def publication_metadata(lock: dict, version: str) -> dict:
    return {
        "name": f"Jugcraft {version}", "version_number": version, "version_type": "alpha",
        "game_versions": [lock["minecraft"]], "loaders": ["fabric"],
        "dependencies": [{
            "project_id": library["modrinthProject"], "version_id": library["modrinthVersion"],
            "dependency_type": "required" if library["requiredByJugcraft"] else "optional",
        } for library in lock["dependencies"] if library["modrinthProject"]],
    }


def add_entry(archive: zipfile.ZipFile, name: str, data: bytes) -> None:
    info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o644 << 16
    archive.writestr(info, data)


def build_pack(lock: dict, jar: Path, output: Path, version: str | None = None, root: Path = ROOT) -> Path:
    metadata = inspect_jugcraft_jar(jar, lock)
    version = version or metadata["version"]
    require(bool(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._+\-]*", version)), "Unsafe pack version")
    output.mkdir(parents=True, exist_ok=True)
    result = output / f"jugcraft-complete-{version}.mrpack"
    with zipfile.ZipFile(result, "w") as archive:
        add_entry(archive, "modrinth.index.json", json_bytes(make_index(lock, version)))
        # Only Jugcraft's own artifact is carried in overrides. Libraries remain official downloads.
        add_entry(archive, "overrides/mods/jugcraft.jar", jar.read_bytes())
        add_entry(archive, "overrides/JUGCRAFT-LIBRARIES.json", json_bytes(lock))
        for folder in ("overrides", "client-overrides", "server-overrides"):
            base = root / "distribution" / folder
            if base.exists():
                for path in sorted(base.rglob("*")):
                    require(not path.is_symlink(), "Pack overrides must not contain symlinks")
                    if not path.is_file():
                        continue
                    relative = path.relative_to(base).as_posix()
                    require(".." not in PurePosixPath(relative).parts and path.suffix != ".jar",
                            "Pack overrides must not contain external JARs or path escapes")
                    require(relative not in {"JUGCRAFT-LIBRARIES.json", "mods/jugcraft.jar"},
                            "Reserved pack override path")
                    add_entry(archive, folder + "/" + relative, path.read_bytes())
    (output / "modrinth-version.json").write_bytes(json_bytes(publication_metadata(lock, metadata["version"])))
    checksum = hashlib.sha512(result.read_bytes()).hexdigest()
    (output / (result.name + ".sha512")).write_text(f"{checksum}  {result.name}\n", encoding="utf-8")
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("check", "verify", "build"))
    parser.add_argument("--jar", type=Path, help="Compiled Jugcraft JAR (otherwise discover the single build/libs JAR)")
    parser.add_argument("--output", type=Path, default=ROOT / "build/distributions")
    parser.add_argument("--cache", type=Path, default=ROOT / "build/framework-cache")
    parser.add_argument("--version", help="Pack version; defaults to the compiled mod version")
    args = parser.parse_args()
    try:
        lock = load_lock()
        check_repository(lock)
        if args.command == "verify":
            verify_downloads(lock, args.cache)
        elif args.command == "build":
            jar = args.jar
            if jar is None:
                candidates = [p for p in (ROOT / "build/libs").glob("*.jar") if not p.name.endswith("-sources.jar")]
                require(len(candidates) == 1, "Provide --jar or leave exactly one built mod JAR in build/libs")
                jar = candidates[0]
            result = build_pack(lock, jar, args.output, args.version)
            print(f"Created {result}. Launcher import/gameplay testing is a separate check.")
        print(f"PASS: {len(lock['dependencies'])} locked libraries and platform pins.")
        return 0
    except (ValueError, KeyError, OSError, zipfile.BadZipFile) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
