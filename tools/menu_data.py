"""JSON resources for the menu (tools/menu.py), in the owner's own textures: each placed dish's model (fitted to the owner's
icon, which the model wears as block/menu/<dish>), blockstate, loot and name; the Cooking Pot in the owner's pot.

Templates (tools/menu.py DISHES "model"; a face's uv is where that part is drawn on the icon):
- bowl: the owner's small soup bowl: a foot, the bowl's band, a rim, the soup inside below the rim, and a heap of what
  is in it (`heap` pixels high), the soup's top read from `content` on the icon.
- plate: the owner's wide plate-bowl, the meal heaped on it.
- stack: a sandwich or burger: one box from `lo` to `hi`, its top read from `top` and its sides from `side`.
- flat: the icon extruded one pixel, lying flat, as a dropped item lies (one element for each run of opaque pixels).
- stand: the icon extruded upright (mugs and bottles), facing the way it was set down.
- box: the owner's popcorn box: a striped box heaped with popcorn.

Called from agriculture_data.py (assets, loot, tags); the foods' items, names and tags come from tools/agriculture.py
ITEMS, their recipes from POT_RECIPES, SHAPELESS and COOKING. Formats follow vanilla Minecraft 26.3's own files.
"""
import os

from PIL import Image

import agriculture as ag
import menu
import orchard
from decor_data import MOD, rid, turned, self_drop

LIBRARY = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "art", "owner-library", "originals", "Blocks",
                       "farming and food textures")
SIDES = ("north", "south", "east", "west")
# A dish's model is drawn with its front to the south (the icon reads the right way to a player standing south of it), and
# a dish set down faces the player who set it, so the blockstate turns the model from its opposite side.
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}


def face(uv, texture="#dish"):
    return {"texture": texture, "uv": [round(v, 3) for v in uv]}


def box(lo, hi, faces, texture="#dish"):
    return {"from": list(lo), "to": list(hi), "faces": {side: face(uv, texture) for side, uv in faces.items()}}


def around(uv):
    return {side: uv for side in SIDES}


def icon(name):
    """The icon a dish wears (its alpha decides the extruded templates' shape): the owner's, or, for the orchards' juices
    (tools/orchard.py, in Jugcraft's own art), the one tools/orchard_textures.py draws."""
    if name in orchard.DISHES:
        import orchard_textures
        return orchard_textures.dish_icon(name)
    image = Image.open(os.path.join(LIBRARY, menu.owner(name) + ".png")).convert("RGBA")
    return image.crop((0, 0, 16, 16))


def runs(image):
    """(row, first, end) for each run of opaque pixels in each row of a 16 x 16 image."""
    alpha = image.split()[3]
    for row in range(16):
        x = 0
        while x < 16:
            if not alpha.getpixel((x, row)):
                x += 1
                continue
            start = x
            while x < 16 and alpha.getpixel((x, row)):
                x += 1
            yield row, start, x


def edge(alpha, row, start, end):
    """Whether a run's edge is open: any of its pixels has nothing next to it in that row."""
    return row < 0 or row > 15 or any(not alpha.getpixel((i, row)) for i in range(start, end))


# ---------------------------------------------------------------- templates

def bowl(name, content=(3, 3, 13, 8), heap=0, band=(2, 8, 14, 11)):
    x0, y0, x1, y1 = band
    rim = (x0, y0, x1, y0 + 1)
    out = [box((5.5, 0, 5.5), (10.5, 1, 10.5), {**around((5, 11, 11, 12)), "down": (5, 11, 11, 12)}),
           box((3.5, 1, 3.5), (12.5, 4, 12.5), {**around(band), "up": content, "down": (5, 11, 11, 12)})]
    for lo, hi in (((3, 4, 3), (13, 5, 4)), ((3, 4, 12), (13, 5, 13)), ((3, 4, 4), (4, 5, 12)), ((12, 4, 4), (13, 5, 12))):
        out.append(box(lo, hi, {**around(rim), "up": rim, "down": rim}))
    out.append(box((4, 4.5, 4), (12, 4.5, 12), {"up": content}))
    if heap:
        top = (content[0] + 1, content[1], content[2] - 1, content[3] - 1)
        out.append(box((5, 4.5, 5), (11, 4.5 + heap, 11), {"up": top, "down": top, **around((top[0], top[3] - heap, top[2], top[3]))}))
    return out


def plate(name, content=(1, 3, 15, 10), heap=2, band=(0, 10, 16, 13)):
    x0, y0, x1, y1 = band
    rim = (x0, y0, x1, y0 + 1)
    out = [box((4.5, 0, 4.5), (11.5, 1, 11.5), {**around((4, 13, 12, 14)), "down": (4, 13, 12, 14)}),
           box((1.5, 1, 1.5), (14.5, 3, 14.5), {**around((x0, y0 + 1, x1, y1)), "up": content, "down": (4, 13, 12, 14)})]
    for lo, hi in (((1, 3, 1), (15, 4, 2)), ((1, 3, 14), (15, 4, 15)), ((1, 3, 2), (2, 4, 14)), ((14, 3, 2), (15, 4, 14))):
        out.append(box(lo, hi, {**around(rim), "up": rim, "down": rim}))
    out.append(box((2, 3.5, 2), (14, 3.5, 14), {"up": content}))
    if heap:
        top = (content[0] + 2, content[1], content[2] - 2, content[3] - 1)
        out.append(box((3.5, 3.5, 3.5), (12.5, 3.5 + heap, 12.5), {"up": top, "down": top, **around((top[0], top[3] - heap, top[2], top[3]))}))
    return out


def stack(name, lo, hi, top, side):
    return [box(lo, hi, {"up": top, **around(side), "down": side})]


def flat(name):
    image = icon(name)
    alpha = image.split()[3]
    out = []
    for row, start, end in runs(image):
        faces = {"up": (start, row, end, row + 1), "down": (start, row, end, row + 1),
                 "west": (start, row, start + 1, row + 1), "east": (end - 1, row, end, row + 1)}
        if edge(alpha, row - 1, start, end):
            faces["north"] = (start, row, end, row + 1)
        if edge(alpha, row + 1, start, end):
            faces["south"] = (start, row, end, row + 1)
        out.append(box((start, 0, row), (end, 1, row + 1), faces))
    return out


def stand(name, depth=4):
    image = icon(name)
    alpha = image.split()[3]
    z0, z1 = 8 - depth / 2, 8 + depth / 2
    out = []
    for row, start, end in runs(image):
        faces = {"south": (start, row, end, row + 1), "north": (end, row, start, row + 1),
                 "west": (start, row, start + 1, row + 1), "east": (end - 1, row, end, row + 1)}
        if edge(alpha, row - 1, start, end):
            faces["up"] = (start, row, end, row + 1)
        if edge(alpha, row + 1, start, end):
            faces["down"] = (start, row, end, row + 1)
        out.append(box((start, 15 - row, z0), (end, 16 - row, z1), faces))
    return out


def popcorn_box(name):
    """The owner's popcorn box (a 32 x 32 texture: two striped sides above, the popcorn and a third side below, kernels
    to the right; uv in sixteenths of it)."""
    sides = {"north": (0, 0, 5, 5.5), "south": (0, 0, 5, 5.5), "east": (5, 0, 10, 5.5), "west": (5, 5.5, 10, 10.5)}
    popcorn = (0, 5.5, 5, 10.5)
    return [box((3, 0, 3), (13, 11, 13), {**sides, "up": popcorn, "down": popcorn}),
            box((4, 11, 4), (12, 12.5, 12), {"up": popcorn, **around((0.5, 6, 4.5, 6.75))}),
            box((6, 12.5, 6), (9, 13.5, 9), {"up": popcorn, **around((1, 7, 2.5, 7.5))})]


def milkshake(name):
    """The owner's milkshake glass (tools/milkshake_data.py), on its own 64 x 64 texture."""
    import milkshake_data
    return milkshake_data.elements(name)


TEMPLATES = {"bowl": bowl, "plate": plate, "stack": stack, "flat": flat, "stand": stand, "box": popcorn_box, "milkshake": milkshake}


def dish_model(name):
    template, *params = menu.all_placed()[name]
    elements = TEMPLATES[template](name, **(params[0] if params else {}))
    texture = rid(f"block/menu/{name}")
    return {"parent": "minecraft:block/block", "textures": {"particle": texture, "dish": texture}, "elements": elements}


# ---------------------------------------------------------------- the Cooking Pot, in the owner's pot

def cooking_pot_model(contents):
    """The owner's iron pot: a body, its rim at the top, a lug each side and a bail handle over it; the soup (Jugcraft's
    own texture) shows inside while it cooks."""
    pot = {"particle": rid("block/cooking_pot_side"), "side": rid("block/cooking_pot_side"), "top": rid("block/cooking_pot_top"),
           "bottom": rid("block/cooking_pot_bottom"), "handle": rid("block/cooking_pot_handle"), "parts": rid("block/cooking_pot_parts")}
    elements = [box((2, 0, 2), (14, 10, 14), {**around((2, 6, 14, 16)), "up": (2, 2, 14, 14)}, "#side"),
                box((0.5, 6, 6.5), (2, 8, 9.5), {**around((5, 0, 8, 2)), "up": (5, 0, 8, 2), "down": (5, 0, 8, 2)}, "#parts"),
                box((14, 6, 6.5), (15.5, 8, 9.5), {**around((5, 0, 8, 2)), "up": (5, 0, 8, 2), "down": (5, 0, 8, 2)}, "#parts"),
                box((1, 8, 7.5), (2, 14, 8.5), {**around((0, 2, 2, 8))}, "#handle"),
                box((14, 8, 7.5), (15, 14, 8.5), {**around((0, 2, 2, 8))}, "#handle"),
                box((1, 14, 7.5), (15, 15, 8.5), {**around((0, 0, 16, 2)), "up": (0, 0, 16, 2), "down": (0, 0, 16, 2)}, "#handle")]
    elements[0]["faces"]["up"]["texture"] = "#top"
    elements[0]["faces"]["down"] = face((2, 2, 14, 14), "#bottom")
    if contents == "soup":
        pot["contents"] = rid("block/cooking_pot_soup")
        elements.append(box((3, 8.5, 3), (13, 8.5, 13), {"up": (3, 3, 13, 13)}, "#contents"))
    return {"parent": "minecraft:block/block", "textures": pot, "elements": elements}


# ---------------------------------------------------------------- writers

def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    for name in menu.all_placed():
        write(models / f"{name}.json", dish_model(name))
        write(states / f"{name}.json", {"variants": {f"facing={f}": turned(rid(f"block/{name}"), OPPOSITE[f])
                                                     for f in ("north", "south", "east", "west")}})
        lang[f"block.{MOD}.{name}"] = (menu.DISHES.get(name) or ag.ITEMS[name])["display"]
    write(models / "cooking_pot.json", cooking_pot_model("empty"))
    write(models / "cooking_pot_cooking.json", cooking_pot_model("soup"))


def loot(out, write):
    """A placed dish drops its food."""
    for name in menu.all_placed():
        write(out / f"{name}.json", {"type": "minecraft:block", "random_sequence": rid(f"blocks/{name}"), "pools": [{
            "rolls": 1, "entries": [{"type": "minecraft:item", "name": rid(name)}],
            "condition": {"type": "minecraft:survives_explosion"}}]})


def tags(tags):
    pass
