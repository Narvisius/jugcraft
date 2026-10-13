"""The owner's milkshakes' models (tools/milkshakes.py): the glass as the page draws it, in its own coordinates (a dish
set down faces whoever set it down with its south side, so the drawing's view is the view of it set down). Every face
wears the milkshake's one texture (block/menu/<name>, tools/milkshake_art.py) at its place in milkshakes.LAYOUT; the
north and west sides wear the south's and east's.

Called from menu_data.py (the "milkshake" template, as every set-down dish's model is made) and agriculture_data.py
(each milkshake's item is its 3D glass, shown as large as the other small 3D props are). Formats follow vanilla
Minecraft 26.3's own files.
"""
import milkshakes as ms
from decor_data import rid

SIDES = ("north", "south", "east", "west")
# How the glass is held and shown as an item: as the flora's small 3D plants are (tools/flora_art.py PLANT_DISPLAY), near
# a slot's or an item frame's full size, where a full block's would show the narrow glass at half that.
DISPLAY = {
    "gui": {"rotation": [25, 225, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2.5, 1.5], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
}
# Which of a part's two drawn sides each side wears.
WEARS = {"north": "s", "south": "s", "east": "e", "west": "e"}


def uv(key, part=None):
    """A face's place on the texture: the whole of LAYOUT[key], or `part` of it ((u0, v0, u1, v1) texels from its corner)."""
    u0, v0, u1, v1 = ms.LAYOUT[key]
    if part:
        return [u0 + part[0], v0 + part[1], u0 + part[2], v0 + part[3]]
    return [u0, v0, u1, v1]


def element(lo, hi, faces, rotation=None):
    out = {"from": list(lo), "to": list(hi),
           "faces": {side: {"texture": "#dish", "uv": u, **extra} for side, (u, extra) in faces.items()}}
    if rotation:
        out["rotation"] = rotation
    return out


def part(lo, hi, name, up=None, down=None):
    """A box wearing `name`'s sides (name_s, name_e) and, if given, a top and an underside."""
    faces = {side: (uv(f"{name}_{WEARS[side]}"), {}) for side in SIDES}
    if up:
        faces["up"] = (uv(up), {})
    if down:
        faces["down"] = (uv(down), {})
    return element(lo, hi, faces)


def bar(lo, hi):
    """A foot bar: its long sides and top along it, its ends square; the bars along z wear their top turned."""
    along_x = hi[0] - lo[0] > hi[2] - lo[2]
    long_sides, ends = (("north", "south"), ("east", "west")) if along_x else (("east", "west"), ("north", "south"))
    faces = {side: (uv("bar_side"), {}) for side in long_sides}
    faces.update({side: (uv("bar_end"), {}) for side in ends})
    turn = {} if along_x else {"rotation": 90}
    faces["up"] = (uv("bar_up"), dict(turn))
    faces["down"] = (uv("bar_up"), {**turn, "cullface": "down"})
    return element(lo, hi, faces)


def elements(name):
    """The glass, part by part: the foot's bars, the base, the glass and its band, the cream, the fruit and the straw."""
    out = [bar(lo, hi) for lo, hi in ms.FOOT["bars"]]
    out.append(part(*ms.PLATE, "plate", down="plate_down"))
    out.append(part(*ms.BODY, "body", down="plate_down"))
    out.append(part(*ms.BAND, "band", up="band_up", down="band_up"))
    out.append(part(*ms.CAP, "cap", up="cap_up"))
    out.append(part(*ms.FRUIT, "fruit", up="fruit_up"))
    straw = part(ms.STRAW["from"], ms.STRAW["to"], "straw", up="straw_up")
    straw["rotation"] = {"origin": list(ms.STRAW["origin"]), "axis": ms.STRAW["axis"], "angle": ms.STRAW["angle"]}
    out.append(straw)
    return out


def assets(root, write):
    """Each milkshake's item is its 3D glass: the model menu_data.py writes for it set down, shown as DISPLAY says."""
    for name in ms.SHAKES:
        write(root / "models" / "item" / f"{name}.json", {"parent": rid(f"block/{name}"), "display": DISPLAY})
        write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})
