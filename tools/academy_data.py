"""Academy M0 fixtures and text; called by the shared material generator."""
import json
from pathlib import Path

LANG = {
    "academy.jugcraft.rune.0": "Dawn rune.",
    "academy.jugcraft.rune.1": "Dusk rune.",
    "academy.jugcraft.rune.2": "Noon rune.",
    "academy.jugcraft.rune.3": "Night rune.",
    "academy.jugcraft.effects": "College atmosphere: %s",
    "block.jugcraft.academy_ward": "College Memory Ward",
    "block.jugcraft.academy_rune": "Courtyard Rune",
    "journal.jugcraft.section.academy": "College of Winterhold",
    "academy.jugcraft.welcome": "Professor Arctus Vale, Master of Radiance: Welcome to the College. Your journal records your own research. Sneak and speak to me for a reminder without opening it.",
    "academy.jugcraft.lesson": "Radiance lesson: examine luminous specimens through the existing Concordance system, then study at a Lampwright's Bench in the Hall of the Elements. Bring your own materials; the bench and journal show requirements. No research is granted by speaking to an instructor.",
    "academy.jugcraft.clue": "The ward remembers four lights. Watch the courtyard stones, then use them in order: Dawn (gold), Noon (white), Dusk (blue), Night (violet). Each stone carries its own color; pause longer than ten seconds or press a wrong stone to restart.",
    "academy.jugcraft.correct": "The ward answers: %s of 4 lights remembered.",
    "academy.jugcraft.reset": "The lights scatter. Begin again with Dawn. Use the central amethyst ward to read the sequence.",
    "academy.jugcraft.lore": "Courtyard memory: The first wardens bound no storm to this stone. They bound a promise: knowledge must pass from hand to hand. A worn inscription points toward the passages beneath the teaching hall. This memory is now recorded in your Concordance Journal.",
    "academy.jugcraft.dormant": "This rune has no connected College memory ward.",
    "academy.jugcraft.busy": "The ward is attending to other apprentices. Try again in ten seconds.",
}


def assets(root, write, lang):
    lang.update(LANG)
    write(root / "blockstates/academy_ward.json", {"variants": {"": {"model": "jugcraft:block/academy_ward"}}})
    write(root / "models/block/academy_ward.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "minecraft:block/amethyst_block"}})
    # Each top has a distinct material, so the clue does not depend solely on particle visibility.
    tops = ("gold_block", "blue_ice", "calcite", "amethyst_block")
    write(root / "blockstates/academy_rune.json", {"variants": {
        f"rune={i}": {"model": f"jugcraft:block/academy_rune_{i}"} for i in range(4)}})
    for i, top in enumerate(tops):
        write(root / f"models/block/academy_rune_{i}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": "minecraft:block/chiseled_stone_bricks", "bottom": "minecraft:block/stone_bricks", "top": f"minecraft:block/{top}"}})


def main():
    root = Path(__file__).resolve().parents[1]
    resources = root / "src/main/resources"
    target = resources / "assets/jugcraft"
    written = []
    def write(path, data):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
        written.append(path.relative_to(resources).as_posix())
    lang_path = target / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    assets(target, write, lang)
    # Preserve post-generation extension ordering/Unicode in the shared language file.
    text = lang_path.read_text(encoding="utf-8")
    lines = text.splitlines()
    for key, value in LANG.items():
        encoded = json.dumps(key)
        replacement = "  " + encoded + ": " + json.dumps(value) + ","
        existing = next((i for i, line in enumerate(lines) if line.lstrip().startswith(encoded + ":")), None)
        if existing is not None:
            lines[existing] = replacement.rstrip(",") if not lines[existing].endswith(",") else replacement
        else:
            insert = next(i for i, line in enumerate(lines) if line.startswith('  "') and line.split('"')[1] > key)
            lines.insert(insert, replacement)
    lang_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    manifest = root / "tools/data/generated-material-resources.json"
    manifest.write_text(json.dumps(sorted(set(json.loads(manifest.read_text())) | set(written)), indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
