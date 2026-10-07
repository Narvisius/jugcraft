"""Item icons that are not arms, drawn from 16x16 text maps in the owner's manner (docs/ITEM_ICONS.md).

The first family moved to maps is the war machines' (7 October 2026): the five tower guns, the Siege Mortar, the
Self-Propelled Howitzer, the Flak Gun, the Range Finder, the four shells, the Landship, the Diesel Walker, the Zeppelin
and the Observation Balloon. Their old 32x32 icons were redrawn at 16x16 after the icon-size rule (ITEM_ICONS.md, rule 1)
merged with them.

Each icon is one map, tools/item_icons/<name>.txt: 16 lines of 16 symbols the owner can edit in any text editor, with
the shared symbol table of ITEM_ICONS.md ("The symbols"). A map names roles, not colours; its `# materials:` line says
which material of tools/icon_materials.py fills each role, e.g.

    # materials: main=steel second=hazard_yellow wood=concrete wrap=rubber

and the map is coloured exactly as tools/check_icon_maps.py previews it (arms_icons.palette over those materials).
tools/check_icon_maps.py checks every map here in CI (FOLDERS), and tools/check_mod_data.py checks that each committed
texture is the map's drawing.
"""
import os

from PIL import Image

import arms_icons
import arms_pixel
import icon_materials

FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), "item_icons")
SIZE = 16
# The role a `# materials:` line names -> the arms_pixel.Style part that arms_icons.palette colours it from.
PARTS = {"main": "blade", "second": "fitting", "wood": "haft", "wrap": "grip", "stone": "gem", "accent": "accent"}


def path(name):
    return os.path.join(FOLDER, name + ".txt")


def has(name):
    """Whether `name` has a 16x16 map here."""
    return os.path.exists(path(name))


def names():
    """Every item drawn from a map here."""
    return sorted(f[:-4] for f in os.listdir(FOLDER) if f.endswith(".txt"))


def load(name):
    """(rows, {role: material name}) of a map: its 16 rows and its `# materials:` line."""
    rows, materials = [], {}
    with open(path(name), encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\r\n")
            if not line.strip():
                continue
            if line.startswith("#"):
                key, _, words = line[1:].partition(":")
                if key.strip() == "materials":
                    for word in words.split():
                        role, _, material = word.partition("=")
                        materials[role] = material
                continue
            rows.append(line)
    if len(rows) != SIZE or any(len(row) != SIZE for row in rows):
        raise ValueError(f"{path(name)}: a map is {SIZE} rows of {SIZE} symbols, not {[len(r) for r in rows]}")
    if not materials:
        raise ValueError(f"{path(name)}: no '# materials:' line")
    return rows, materials


def style(materials):
    """The arms_pixel.Style a map's materials make (roles it does not name are never drawn)."""
    for role, name in materials.items():
        if role not in PARTS or name not in icon_materials.MATERIALS:
            raise ValueError(f"unknown role or material {role}={name} (tools/icon_materials.py)")
    chosen = {PARTS[role]: icon_materials.MATERIALS[name] for role, name in materials.items()}
    filler = arms_pixel.STYLES["bronze"]
    return arms_pixel.Style(*(chosen.get(part, getattr(filler, part))
                              for part in ("blade", "fitting", "grip", "haft", "gem", "accent")))


def draw(name):
    """`name`'s 16x16 icon."""
    rows, materials = load(name)
    colours = arms_icons.palette(style(materials))
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch not in colours:
                raise ValueError(f"{path(name)}: unknown symbol {ch!r} at {x},{y}")
            image.putpixel((x, y), colours[ch])
    return image
