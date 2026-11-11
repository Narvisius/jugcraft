"""The Console Desk remade (Tri-Pillar work list item 14; Justin on the first mockup: "you have one chair, three
keyboards, three mouses, and three monitors. Tilt the monitors inwards a little bit towards the chair and get rid of the
two keyboards on the side, and make the desk look like one big desk").

A desk is the console_desk blocks in a row (tower/ConsoleDeskBlock: `left` and `right`, a desk of the same facing beside
it on its user's left or right). Together they make one long bench desk: one continuous top with its rubber front
edge, a modesty panel and cable tray down the back, end panels only at the outer ends, a T-leg under each middle block.
Each block carries one monitor on a stand; the end blocks' monitors are turned 22.5 degrees in toward the middle (to the
chair). One keyboard and one mouse, on the middle block (a desk on its own has them too). client/ConsoleDeskRenderer
draws the monitor's page on its glass.

Drawn facing north: the user at +z looking north, their left -x. Run from the repository root: python3 tools/ops_desk.py
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
ALL = ("north", "south", "east", "west", "up", "down")
TILT = 22.5
# the monitor (px, facing north, untilted): its housing, its glass (the page is drawn on its face), the yaw's pivot
HOUSING = (0.8, 14.9, 2.2, 15.2, 24.25, 3.4)
GLASS = (1.2, 15.5, 3.4, 14.8, 23.75, 3.7)
PIVOT = (8.0, 19.5, 2.8)
VARIANTS = {"single": (False, False), "left": (False, True), "middle": (True, True), "right": (True, False)}


def uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    r = {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
         "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}[face]
    u0, v0, u1, v1 = r
    du, dv = -16 * (min(u0, u1) // 16), -16 * (min(v0, v1) // 16)
    r = [u0 + du, v0 + dv, u1 + du, v1 + dv]
    return [round(max(0, min(16, c)), 4) for c in r]


def el(f, t, tex, faces=ALL, rot=None, uvs=None):
    """A box; tex is one texture for every face or a {face: texture} map (faces not in it take its "*")."""
    pick = (lambda face: tex.get(face, tex["*"])) if isinstance(tex, dict) else (lambda face: tex)
    e = {"from": [round(c, 4) for c in f], "to": [round(c, 4) for c in t],
         "faces": {face: {"texture": "#" + pick(face), "uv": (uvs or {}).get(face) or uv(face, f, t)} for face in faces}}
    if rot:
        e["rotation"] = rot
    return e


def desk(left, right):
    """The block's elements: joined on its user's left / right."""
    x0, x1 = (0.0 if left else 0.4), (16.0 if right else 15.6)
    e = []
    sides = tuple(f for f in ("east", "west") if (f == "west" and not left) or (f == "east" and not right))
    # the top and its rubber front edge, the back edge
    e.append(el((x0, 10.4, 1.2), (x1, 11.2, 14.6), "top", ("up", "down", "north") + sides))
    e.append(el((x0, 10.25, 14.6), (x1, 11.3, 15.1), "rubber", ("north", "south", "up", "down") + sides))
    # the modesty panel and the cable tray along the back
    m0, m1 = (0.0 if left else 1.4), (16.0 if right else 14.6)
    e.append(el((m0, 2.5, 1.6), (m1, 10.4, 2.2), "panel", ("north", "south", "down") + sides))
    e.append(el((m0, 8.6, 2.2), (m1, 9.4, 4.6), "dark", ("south", "down", "up") + sides))
    # end panels at the outer ends, on their feet; a T-leg under a middle block
    for end, ex0, ex1 in ((not left, 0.4, 1.4), (not right, 14.6, 15.6)):
        if end:
            e.append(el((ex0, 0.4, 1.6), (ex1, 10.4, 14.0), "panel", ALL))
            e.append(el((ex0 - 0.1, 0.0, 1.2), (ex1 + 0.1, 0.4, 14.6), "steel", ALL))
    if left and right:
        e.append(el((7.2, 0.4, 2.2), (8.8, 10.4, 3.8), "panel", ("east", "west", "south", "north")))
        e.append(el((6.0, 0.0, 1.6), (10.0, 0.4, 9.0), "steel", ALL))
    # the monitor on its stand, turned in toward the middle at the ends
    angle = TILT if (right and not left) else -TILT if (left and not right) else 0
    rot = {"origin": list(PIVOT), "axis": "y", "angle": angle} if angle else None
    e.append(el((5.6, 11.2, 1.4), (10.4, 11.45, 5.0), "steel", ALL, rot))
    e.append(el((7.3, 11.45, 2.0), (8.7, 15.0, 2.7), "steel", ALL, rot))
    hx0, hy0, hz0, hx1, hy1, hz1 = HOUSING
    e.append(el((hx0, hy0, hz0), (hx1, hy1, hz1), "dark", ALL, rot))
    gx0, gy0, gz0, gx1, gy1, gz1 = GLASS
    e.append(el((gx0, gy0, gz0), (gx1, gy1, gz1), "glass", ("south", "east", "west", "up", "down"), rot))
    # one keyboard and mouse: on the middle block, or on a desk standing alone
    if left == right:
        e.append(el((3.6, 11.2, 9.2), (12.4, 11.55, 12.2), {"up": "keys", "*": "dark"}, ("up", "north", "south", "east", "west"),
                    uvs={"up": [0, 0, 16, 5.45]}))
        e.append(el((12.9, 11.2, 9.0), (15.3, 11.4, 12.6), "rubber", ("up", "north", "south", "east", "west")))
        e.append(el((13.6, 11.4, 10.2), (14.4, 11.76, 11.6), "dark", ("up", "north", "south", "east", "west")))
    tex = {"top": "jugcraft:block/tower_detail_light_steel", "rubber": "jugcraft:block/tower_detail_rubber",
           "panel": "jugcraft:block/tower_detail_panel", "dark": "jugcraft:block/tower_detail_dark",
           "steel": "jugcraft:block/tower_detail_steel", "glass": "jugcraft:block/tower_detail_glass",
           "keys": "jugcraft:block/ops_desk_keys", "particle": "jugcraft:block/tower_detail_steel"}
    return {"parent": "minecraft:block/block", "textures": tex, "elements": e}


def keys_texture():
    """The keyboard's top (its keys in rows, the number pad), 128 px across, 0.34 of it deep."""
    W = 128
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    h = round(W * 3.0 / 8.8)
    d.rectangle((0, 0, W - 1, h - 1), fill=(34, 36, 40, 255))
    pitch = 5.6
    rows = [15, 15, 14, 13, 11]
    for r, n in enumerate(rows):
        y = 2 + r * (h - 4) / 6
        for k in range(n):
            x = 2 + k * pitch + (r * 1.4 if r else 0)
            d.rectangle((x, y, x + pitch - 1.2, y + (h - 4) / 6 - 1.2), fill=(62, 66, 72, 255))
    d.rectangle((2 + 3 * pitch, 2 + 5 * (h - 4) / 6, 2 + 9 * pitch, h - 3), fill=(62, 66, 72, 255))
    for r in range(5):
        for k in range(4):
            x = 99 + k * pitch
            y = 2 + (r + 1) * (h - 4) / 6
            d.rectangle((x, y, x + pitch - 1.2, y + (h - 4) / 6 - 1.2), fill=(62, 66, 72, 255))
    return img


ROLE_MESSAGE = ("message.jugcraft.console_desk.role", "Desk role: %s (sneak and use it again for the next)")


def write_all(write=None, lang=None):
    """The four desk models, the keyboard's texture, the blockstate (facing x left x right; the role, tower/ConsoleDeskBlock
    ROLE, changes no model) and the item (a desk on its own), and the role change's message. write(path, obj) writes a JSON
    file and lang is the language table being built (tools/generate_material_data.py passes its own: tools/tower.py calls
    this after writing the old one-piece desk's blockstate and item, which these replace); run on its own it writes the
    files itself and adds the message to en_us.json."""
    if write is None:
        def write(path, obj):
            path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")
        lang_path = ASSETS / "lang/en_us.json"
        table = json.loads(lang_path.read_text(encoding="utf-8"))
        table[ROLE_MESSAGE[0]] = ROLE_MESSAGE[1]
        lang_path.write_text(json.dumps(dict(sorted(table.items())), indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    if lang is not None:
        lang[ROLE_MESSAGE[0]] = ROLE_MESSAGE[1]
    models = {f"console_desk_{v}": desk(*lr) for v, lr in VARIANTS.items()}
    for name, m in models.items():
        write(ASSETS / "models/block" / f"{name}.json", m)
    keys_texture().save(ASSETS / "textures/block/ops_desk_keys.png")
    variants = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for v, (l, r) in VARIANTS.items():
            key = f"facing={f},left={'true' if l else 'false'},right={'true' if r else 'false'}"
            variants[key] = {"model": f"jugcraft:block/console_desk_{v}", **({"y": y} if y else {})}
    write(ASSETS / "blockstates/console_desk.json", {"variants": variants})
    write(ASSETS / "items/console_desk.json", {"model": {"type": "minecraft:model", "model": "jugcraft:block/console_desk_single"}})
    return models


if __name__ == "__main__":
    m = write_all()
    print(len(m), "models", sum(len(v["elements"]) for v in m.values()), "elements")
