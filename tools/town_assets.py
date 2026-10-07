"""The walled town's assets and data for tools/generate_material_data.py: the Jug Teller (the town's ATM: model,
blockstate, item model, textures drawn here, loot), the town's usable-blocks tag, its text, the townsfolk's skins and
the town itself (tools/town.py)."""
from PIL import Image

import town_lang

MOD = "jugcraft"
BLOCKS = ["atm"]
ITEMS = []


def blocks():
    return list(BLOCKS)


def items():
    return list(ITEMS)


def rid(path):
    return f"{MOD}:{path}"


# ---------------------------------------------------------------- textures

IRON = (54, 56, 62)
IRON_LIGHT = (84, 88, 96)
IRON_LIT = (64, 66, 73)
IRON_SHADE = (46, 48, 53)
BRASS = (196, 150, 64)
BRASS_LIGHT = (222, 180, 92)
BRASS_SHADE = (170, 126, 50)
BRASS_DARK = (138, 98, 36)
SCREEN = (30, 70, 40)
GLOW = (140, 255, 156)


def textures():
    """The Jug Teller's faces: a riveted cast-iron body, a brass-framed green screen with a jug on it and a keypad.
    Flat tones lit from the top left (docs/ART_DIRECTION.md), no noise."""
    side = Image.new("RGBA", (16, 16), IRON + (255,))
    px = side.load()
    for i in range(1, 15):
        px[i, 1] = IRON_LIT + (255,)
        px[1, i] = IRON_LIT + (255,)
        px[i, 14] = IRON_SHADE + (255,)
        px[14, i] = IRON_SHADE + (255,)
    for i in range(16):
        px[i, 0] = BRASS_DARK + (255,)
        px[i, 15] = BRASS_DARK + (255,)
        px[0, i] = IRON_LIGHT + (255,)
        px[15, i] = IRON_LIGHT + (255,)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (2, 7), (13, 7)):
        px[x, y] = BRASS + (255,)
    for y in range(4, 12):
        px[7, y] = IRON_LIGHT + (255,)
        px[8, y] = (40, 42, 46, 255)
    front = side.copy()
    px = front.load()
    for x in range(3, 13):
        for y in range(2, 9):
            px[x, y] = BRASS + (255,) if x in (3, 12) or y in (2, 8) else SCREEN + (255,)
    # A little jug glowing on the screen.
    for x, y in ((7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (10, 5)):
        px[x, y] = GLOW + (255,)
    for row in range(3):
        for col in range(3):
            px[5 + col * 2, 10 + row * 2 - 1] = (220, 210, 190, 255)
    for x in range(11, 13):
        px[x, 11] = (20, 20, 22, 255)
    top = Image.new("RGBA", (16, 16), BRASS + (255,))
    px = top.load()
    for i in range(16):
        px[i, 0] = BRASS_DARK + (255,)
        px[i, 15] = BRASS_DARK + (255,)
        px[0, i] = BRASS_DARK + (255,)
        px[15, i] = BRASS_DARK + (255,)
    for i in range(1, 15):
        px[i, 1] = BRASS_LIGHT + (255,)
        px[1, i] = BRASS_LIGHT + (255,)
        px[i, 14] = BRASS_SHADE + (255,)
        px[14, i] = BRASS_SHADE + (255,)
    for x in range(5, 11):
        px[x, 7] = (30, 24, 16, 255)
        px[x, 8] = (30, 24, 16, 255)
    return {"atm_side": side, "atm_front": front, "atm_top": top}


def model():
    """A plinth, the machine's body, a brass cap and a brass shelf under the screen. Front: north."""
    def face(tex, uv=None):
        out = {"texture": f"#{tex}"}
        if uv:
            out["uv"] = uv
        return out

    def cube(frm, to, faces):
        return {"from": frm, "to": to, "faces": faces}

    elements = [
        cube([1, 0, 1], [15, 2, 15], {d: face("side", [1, 14, 15, 16] if d in ("north", "south", "east", "west") else [1, 1, 15, 15])
                                      for d in ("north", "south", "east", "west", "up", "down")}),
        cube([2, 2, 3], [14, 15, 14], {"north": face("front", [2, 1, 14, 14]), "south": face("side", [2, 1, 14, 14]),
                                       "east": face("side", [2, 1, 13, 14]), "west": face("side", [3, 1, 14, 14]),
                                       "up": face("top", [2, 3, 14, 14]), "down": face("side")}),
        cube([1, 15, 2], [15, 16, 15], {d: face("top", [1, 2, 15, 15] if d in ("up", "down") else [1, 0, 15, 1])
                                        for d in ("north", "south", "east", "west", "up", "down")}),
        cube([4, 6, 1], [12, 7, 3], {d: face("top", [4, 1, 12, 3] if d in ("up", "down") else [4, 7, 12, 8])
                                     for d in ("north", "south", "east", "west", "up", "down")}),
    ]
    return {"parent": "minecraft:block/block",
            "textures": {"particle": rid("block/atm_side"), "front": rid("block/atm_front"), "side": rid("block/atm_side"),
                         "top": rid("block/atm_top")},
            "elements": elements}


def assets(assets_dir, write, lang):
    tex = assets_dir / "textures" / "block"
    tex.mkdir(parents=True, exist_ok=True)
    for name, img in textures().items():
        img.save(tex / f"{name}.png", optimize=True)
    write(assets_dir / "models" / "block" / "atm.json", model())
    rotations = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
    write(assets_dir / "blockstates" / "atm.json", {"variants": {
        f"facing={facing}": {"model": rid("block/atm"), **rot} for facing, rot in rotations.items()}})
    write(assets_dir / "items" / "atm.json", {"model": {"type": "minecraft:model", "model": rid("block/atm")}})
    lang.update(town_lang.lang())


USABLE = ["#minecraft:wooden_doors", "#minecraft:fence_gates", "#minecraft:buttons", "minecraft:lever",
          "minecraft:crafting_table", "#minecraft:beds", rid("atm")]
