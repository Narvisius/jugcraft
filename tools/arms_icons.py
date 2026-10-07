"""The arms' 16x16 inventory icons, in the owner's style (docs/features/arms-icons-16.md).

On 5 October 2026 the owner sent a sheet of weapon icons showing "how I draw my style for texturing most weapons":
vanilla's own size, 16x16, on the diagonal with the hand at the bottom left and the point or head at the top right, a
one-pixel outline in a dark tone of each part's own material, light from the top left, a few flat tones, and chunky
parts that read at a glance. The sheet's icons are third-party art and were studied for that style only; nothing of
them is used. Every map here is drawn fresh.

Each kind (and each Arms VII variant) is one hand-drawn map, tools/arms_icons/<name>.txt: 16 lines of 16 symbols, which
the owner can edit directly. A map names no colours, only what each pixel is made of, so one map serves every metal:
it is coloured from the style's materials (tools/arms_pixel.py STYLES, or a variant's own in tools/arms_variants_art.py),
the same materials its 3D model in the hand is drawn in.

  .        transparent
  O D M L H  blade or head: outline, dark, mid, light, highlight      (style.blade)
  g f F Y    fittings, guard, pommel, rings, socket: outline, dark, mid, light   (style.fitting)
  w b B      haft or handle wood: outline, dark, light               (style.haft)
  k K        grip wrap: dark, light (outlined with w)                (style.grip)
  a A        set stone: dark, light                                   (style.gem)
  e E        the style's accent (runes, magma, a hazard stripe): dark, light   (style.accent)
  c C        chain: dark, light
  x X        flame: orange, yellow (a brazier's flame flickers between frames)
"""
import os

from PIL import Image

import arms_pixel as px

FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), "arms_icons")
SIZE = 16


def path(name):
    return os.path.join(FOLDER, name + ".txt")


def has(name):
    """Whether `name` (a kind or a variant) has a 16x16 map."""
    return os.path.exists(path(name))


def load(name):
    """The map's 16 rows (comment lines, starting with #, are skipped)."""
    with open(path(name), encoding="utf-8") as f:
        rows = [line.rstrip("\n") for line in f if line.strip() and not line.startswith("#")]
    if len(rows) != SIZE or any(len(row) != SIZE for row in rows):
        raise ValueError(f"{path(name)}: a map is {SIZE} rows of {SIZE} symbols, not {[len(r) for r in rows]}")
    return rows


def palette(style):
    """Symbol -> RGBA, from a style's materials (each a ramp: outline dark, outline light, dark, mid, light, highlight)."""
    def rgba(colour):
        return tuple(colour) + (255,)

    blade, fitting, haft, grip, gem, accent = style.blade, style.fitting, style.haft, style.grip, style.gem, style.accent
    return {
        "O": rgba(blade.outline_dark), "D": rgba(blade.dark), "M": rgba(blade.mid), "L": rgba(blade.light),
        "H": rgba(blade.highlight),
        "g": rgba(fitting.outline_dark), "f": rgba(fitting.dark), "F": rgba(fitting.mid), "Y": rgba(fitting.light),
        "w": rgba(haft.outline_dark), "b": rgba(haft.mid), "B": rgba(haft.light),
        "k": rgba(grip.dark), "K": rgba(grip.light),
        "a": rgba(gem.dark), "A": rgba(gem.light),
        "e": rgba(accent.mid), "E": rgba(accent.light),
        "c": rgba(px.CHAIN.outline_light), "C": rgba(px.CHAIN.light),
        "x": rgba(px.EMBER.mid), "X": rgba(px.EMBER.light),
    }


def flicker(rows, frame):
    """A flame's look in `frame`: every other frame its yellow tips and orange body trade places on alternate pixels,
    and its topmost pixels come and go, so the fire licks without the brazier moving."""
    if frame == 0:
        return rows
    out = [list(row) for row in rows]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in "xX" and (x + y + frame) % 2 == 0:
                out[y][x] = "X" if ch == "x" else "x"
    tips = [(x, y) for y, row in enumerate(rows) for x, ch in enumerate(row)
            if ch in "xX" and (y == 0 or rows[y - 1][x] not in "xX")]
    for i, (x, y) in enumerate(tips):
        if (i + frame) % 3 == 0:
            out[y][x] = "."
    return ["".join(row) for row in out]


def draw(name, style, frame=0):
    """`name`'s 16x16 icon in `style`'s materials (frame: for a flickering flame)."""
    rows = flicker(load(name), frame)
    colours = palette(style)
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch not in colours:
                raise ValueError(f"{path(name)}: unknown symbol {ch!r} at {x},{y}")
            image.putpixel((x, y), colours[ch])
    return image


def icon(kind, metal, frame=0):
    """A kind's icon in a metal (tools/arms_pixel.py STYLES)."""
    return draw(kind, px.STYLES[metal], frame)
