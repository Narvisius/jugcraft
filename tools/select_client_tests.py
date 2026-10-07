#!/usr/bin/env python3
"""Choose which client game test classes a change needs, and share them out between CI's client jobs.

The client game tests start a real game and photograph showrooms, which takes CI about 15 to 25 minutes for every
class. Most pull requests change a few blocks, so a pull request runs only the classes that show what it changed;
main, a manual run and a pull request labelled "full-client-tests" run them all (docs/TESTING.md).

How a changed file picks classes (src/gametest/resources/fabric.mod.json lists the classes):
- a client test class itself, or one newly listed: that class;
- docs, Markdown, the generators and checks in tools/ and scripts/ (their output is committed and judged below),
  data under src/main/resources/data, the language file and server-only game tests: nothing;
- a Java class under src/main or src/client that only gained code (a feature registering itself in a registry, one
  more entry in a list): the classes matching the names and IDs it gained; comments do not count;
- a Java class whose existing code changed: every class that shows it (names it, or whose IDs match it with "Block",
  "Renderer" and similar endings taken off: "GiantBeatingHeartRenderer" -> giant_beating_heart) or shows a Jugcraft
  class that uses it ("CrewedGun" -> the classes showing the Siege Mortar, Flak Gun and tower guns); a class no test
  shows picks nothing, as the mod job's server game tests still run;
- a model, blockstate, texture or other asset: every class naming its ID, or an ID it starts with
  ("ready_rack_0" -> ready_rack);
- anything else (build files, the workflow, mixins, the test mod's helpers), or a change that picks half the classes
  or more: every class.

A test "names" an ID when its source has it as a string ("pipe_organ", "jugcraft:pipe_organ") or a constant
(PIPE_ORGAN), or when a Jugcraft class it uses that few tests use (a feature's own list, not a shared registry) has it.

Usage: select_client_tests.py [--base REV] [--head REV] [--all] [--shards N] [--github-output FILE]
Without --base, or with --all, every class runs. Prints a summary and one line per shard.
"""
import argparse
import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
MOD_JSON = "src/gametest/resources/fabric.mod.json"
TEST_DIR = ROOT / "src" / "gametest" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "test"
CODE_DIRS = (ROOT / "src" / "main" / "java", ROOT / "src" / "client" / "java")

# Rough seconds each class takes on a CI runner, to share them out evenly; others are estimated from their length.
# Estimates from the client jobs' logs of October 2026, not measurements per class: with the biome tour at 600, its
# job took 22 minutes and the others 15 and 14.
WEIGHTS = {
    "BiomeClientGameTests": 900,
    "AlpineClientGameTests": 180,
    "JugcraftClientGameTests": 180,
    "TownClientGameTests": 120,
    "GuideScreenshotGameTests": 120,
    "SeasonClientGameTests": 90,
    "JugcraftServerClientGameTests": 90,
}
HUB = 5  # a class this many test classes name is a shared registry, not one feature
ENDINGS = ("BlockEntityRenderer", "BlockEntity", "Renderer", "Block", "Item", "Entity", "Model", "Screen", "Menu",
           "Blocks", "Items")
# Files that change nothing a client test shows.
NOTHING = (
    re.compile(r"^docs/"),
    re.compile(r"\.md$"),
    re.compile(r"^tools/"),
    re.compile(r"^scripts/"),
    re.compile(r"^src/main/resources/data/"),
    re.compile(r"^src/main/resources/assets/jugcraft/lang/"),
    re.compile(r"^project-status\.json$"),
    re.compile(r"^LICENSE"),
    re.compile(r"^\.github/(?!workflows/build\.yml$)"),
    re.compile(r"^src/gametest/java/.*(?<!Client)GameTests\.java$"),
)
ASSET = re.compile(r"^src/main/resources/assets/jugcraft/(?!lang/)(?:.*/)?([a-z0-9_]+)\.[a-z0-9.]+$")
CODE = re.compile(r"^src/(?:main|client)/java/(?:.*/)?([A-Za-z0-9_]+)\.java$")
CLIENT_TEST = re.compile(r"^src/gametest/java/(?:.*/)?([A-Za-z0-9_]+)\.java$")


def git(*args):
    return subprocess.run(("git",) + args, cwd=ROOT, check=True, capture_output=True, text=True).stdout


def snake(name):
    for ending in ENDINGS:
        if name.endswith(ending) and len(name) > len(ending):
            name = name[: -len(ending)]
            break
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()


def real_ids():
    """Every item, block and entity texture or model name the mod has, so stray words ("fire", "noon") are not IDs."""
    assets = ROOT / "src" / "main" / "resources" / "assets" / "jugcraft"
    found = set()
    for folder in ("items", "blockstates", "models", "textures"):
        found |= {p.stem for p in (assets / folder).rglob("*") if p.is_file()}
    return found


def strip_comments(text):
    return re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.S)


def names_in(text):
    """The class names, IDs and constants (as lower-case IDs) a Java source mentions, comments left out."""
    text = strip_comments(text)
    classes = set(re.findall(r"\b[A-Z][a-z][A-Za-z0-9]*\b", text))
    ids = set(re.findall(r'"(?:jugcraft:)?([a-z][a-z0-9_]{2,})"', text))
    ids |= {c.lower() for c in re.findall(r"\b[A-Z][A-Z0-9]*_[A-Z0-9_]+\b|\b[A-Z]{4,}\b", text)}
    return classes, ids


def test_classes(listed):
    """For each listed class: the Jugcraft classes and the IDs it names."""
    sources = {p.stem: p for d in CODE_DIRS for p in d.rglob("*.java")}
    ids_known = real_ids()
    tests = {}
    for entry in listed:
        name = entry.rsplit(".", 1)[-1]
        path = TEST_DIR / f"{name}.java"
        text = path.read_text(encoding="utf-8") if path.is_file() else ""
        classes, ids = names_in(text)
        tests[name] = {"classes": classes & sources.keys(), "ids": ids}
    users = {}
    for info in tests.values():
        for cls in info["classes"]:
            users[cls] = users.get(cls, 0) + 1
    for info in tests.values():
        for cls in info["classes"]:
            if users[cls] <= 3:  # a feature's own class: its IDs count as the test's
                info["ids"] |= names_in(sources[cls].read_text(encoding="utf-8"))[1]
    for info in tests.values():
        info["ids"] &= ids_known
    return tests


def users_of():
    """For each Jugcraft class, the other Jugcraft classes whose source names it."""
    sources = {p.stem: strip_comments(p.read_text(encoding="utf-8")) for d in CODE_DIRS for p in d.rglob("*.java")}
    users = {}
    for stem, text in sources.items():
        for cls in set(re.findall(r"\b[A-Z][a-z][A-Za-z0-9]*\b", text)) & sources.keys() - {stem}:
            users.setdefault(cls, set()).add(stem)
    return users


def showing(name, tests):
    """The test classes that show a Jugcraft class: name it, or name the ID it is for."""
    stem = snake(name)
    return {t for t, info in tests.items() if name in info["classes"] or match_id(stem, info["ids"])}


def match_id(stem, ids):
    return stem in ids or any(stem.startswith(i + "_") for i in ids if len(i) >= 4)


def added_only(base, head, path):
    """The code a file gained, or None when existing code changed or went. Comments do not count, and a line that
    only grew at its end (one more entry appended to a list) counts as gaining the new part."""
    removed, added = [], []
    for line in git("diff", "-U0", base, head, "--", path).splitlines():
        if line.startswith(("---", "+++")) or not line.startswith(("-", "+")):
            continue
        text = line[1:].strip()
        if not text or text.startswith(("*", "//", "/*")):
            continue
        (removed if line.startswith("-") else added).append(text)
    gained = []
    for line in added:
        grown = next((r for r in removed if line.startswith(r.rstrip(");}, ")) and len(r.rstrip(");}, ")) > 8), None)
        if grown is not None:
            removed.remove(grown)
            gained.append(line[len(grown.rstrip(");}, ")):])
        else:
            gained.append(line)
    return None if removed else gained


def select(changed, tests, newly_listed, base=None, head=None):
    """(classes, reasons): the classes the changed files need, or None for every class."""
    chosen, reasons = set(), []
    everything = None
    users = users_of()
    existing = set(git("ls-tree", "-r", "--name-only", base).splitlines()) if base else set()
    for path in changed:
        test = CLIENT_TEST.match(path)
        if test and test.group(1) in tests:
            chosen.add(test.group(1))
            continue
        if any(rule.search(path) for rule in NOTHING):
            continue
        if path == MOD_JSON:
            chosen |= newly_listed
            continue
        code = CODE.match(path)
        if code and "/mixin/" not in path:
            name = code.group(1)
            existed = base and path in existing
            added = added_only(base, head, path) if existed else None
            if added is not None:
                classes, ids = names_in("\n".join(added))
                classes = {c for c in classes if sum(c in info["classes"] for info in tests.values()) < HUB}
                ids |= {snake(c) for c in classes}
                hits = {t for t, info in tests.items()
                        if classes & info["classes"] or any(match_id(i, info["ids"]) for i in ids)}
            else:
                hits = showing(name, tests)
                for user in users.get(name, ()):
                    if sum(user in info["classes"] for info in tests.values()) < HUB:  # not a registry many tests use
                        hits |= showing(user, tests)
            if not hits:
                reasons.append(f"{path}: no client test shows {name}")
            chosen |= hits
            continue
        asset = ASSET.match(path)
        if asset:
            stem = asset.group(1)
            for ending in ("_inventory", "_in_hand", "_item"):
                stem = stem.removesuffix(ending)
            hits = {t for t, info in tests.items() if match_id(stem, info["ids"])}
            if not hits:
                reasons.append(f"{path}: no client test shows {stem}")
            chosen |= hits
            continue
        everything = everything or f"{path}: shared file"
    if everything:
        return None, [everything]
    if len(chosen) * 2 >= len(tests):
        return None, [f"{len(chosen)} of {len(tests)} classes changed"]
    return chosen, reasons


def share(classes, tests, shards):
    """Longest first onto the least-loaded job."""
    def weight(name):
        if name in WEIGHTS:
            return WEIGHTS[name]
        path = TEST_DIR / f"{name}.java"
        return 8 + (len(path.read_text(encoding="utf-8").splitlines()) // 15 if path.is_file() else 0)

    jobs = [[0, []] for _ in range(shards)]
    for name in sorted(classes, key=lambda n: (-weight(n), n)):
        job = min(jobs, key=lambda j: j[0])
        job[0] += weight(name)
        job[1].append(name)
    return jobs


def listed_at(rev):
    try:
        return json.loads(git("show", f"{rev}:{MOD_JSON}"))["entrypoints"]["fabric-client-gametest"]
    except (subprocess.CalledProcessError, KeyError, ValueError):
        return []


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--base", help="compare against this revision (a pull request's base)")
    parser.add_argument("--head", default="HEAD")
    parser.add_argument("--all", action="store_true", help="run every class")
    parser.add_argument("--shards", type=int, default=3)
    parser.add_argument("--github-output", help="append shard0..shardN-1 (comma-separated classes) to this file")
    args = parser.parse_args()

    listed = json.loads((ROOT / MOD_JSON).read_text(encoding="utf-8"))["entrypoints"]["fabric-client-gametest"]
    tests = test_classes(listed)
    if args.all or not args.base:
        chosen, reasons = None, ["full run requested" if args.all else "no base to compare with"]
    else:
        changed = [p for p in git("diff", "--name-only", args.base, args.head).splitlines() if p]
        before = {e.rsplit(".", 1)[-1] for e in listed_at(args.base)}
        newly = {name for name in tests if name not in before}
        chosen, reasons = select(changed, tests, newly, args.base, args.head)
        print(f"{len(changed)} changed files")
    if chosen is None:
        chosen = set(tests)
        print(f"Every client test class ({len(chosen)}): {reasons[0]}")
    else:
        print(f"{len(chosen)} of {len(tests)} client test classes: {', '.join(sorted(chosen)) or 'none'}")
        for reason in reasons:
            print(f"  note: {reason}")
    jobs = share(chosen, tests, args.shards)
    lines = []
    for i, (seconds, names) in enumerate(jobs):
        print(f"shard {i}: about {seconds // 60} min, {len(names)} classes: {', '.join(names) or 'none'}")
        lines.append(f"shard{i}={','.join(names)}")
    if args.github_output:
        with open(args.github_output, "a", encoding="utf-8") as out:
            out.write("\n".join(lines) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
