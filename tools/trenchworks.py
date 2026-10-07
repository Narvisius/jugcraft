"""Trench works (batch 50, docs/features/trench-works.md): the dieselpunk front line.

Sandbags, timber revetment, duckboards, barbed wire that snags and cuts whatever pushes through it, field telephones
that carry a redstone signal to every other telephone on their channel, and searchlights that throw a long beam where
they are pointed. Every texture is drawn here; the searchlight's moving head and beam are exported as quads
(assets/jugcraft/trench_quads.json) for client/SearchlightRenderer.

Java: building/Trenchworks.java (the blocks), building/FieldTelephoneBlock (and its block entity),
building/SearchlightBlock (and its block entity). tools/check_mod_data.py keeps the numbers the same.
"""
import json
import math

from PIL import Image

from clean_metal import corner_bolts, plate
from steampunk_models import box, cyl
from zeppelin import tiled_quads

MOD = "jugcraft"

# id: (display name, kind, hardness, blast resistance). "family" and "full" are written by tools/dieselworks.py's
# write_blocks; "duckboard", "wire", "telephone" and "searchlight" here.
BLOCKS = {
    "sandbags": ("Sandbags", "family", 2.0, 12.0),
    "timber_revetment": ("Timber Revetment", "family", 2.0, 3.0),
    "duckboard": ("Duckboard", "duckboard", 1.0, 3.0),
    "barbed_wire": ("Barbed Wire", "wire", 2.0, 6.0),
    "field_telephone": ("Field Telephone", "telephone", 1.5, 6.0),
    "searchlight": ("Searchlight", "searchlight", 2.5, 6.0),
}
TEXTURES = {"sandbags": "ts_sandbags", "timber_revetment": "ts_revetment"}
TOOLTIPS = {
    "sandbags": "Shrugs off blasts: build parapets and gun pits.",
    "duckboard": "A slatted walkway to keep boots out of the mud.",
    "barbed_wire": "Slows and cuts whatever pushes through it. Sneak to pick your way across slowly but unhurt.",
    "field_telephone": "Power it from behind and every telephone on its channel within 256 blocks rings and gives a "
                       "signal from its front. Use a dye to set the channel.",
    "searchlight": "Use to turn it, sneak and use to tilt it. A redstone signal switches it off.",
}
# Barbed wire: how much it slows (fraction of normal speed kept) and the damage each time it cuts.
WIRE_SLOW = 0.35
WIRE_DAMAGE = 1.0
# How far a field telephone's ring carries, in blocks.
PHONE_RANGE = 256
# The searchlight: yaw steps (a full turn), the highest tilt step (each 15 degrees up), its light and beam length.
SEARCHLIGHT_YAWS = 16
SEARCHLIGHT_TILTS = 5
SEARCHLIGHT_LIGHT = 15
BEAM_BLOCKS = 24
# The searchlight head's pivot in the block, in pixels. Keep in sync with client/SearchlightRenderer.
PIVOT = (8, 11, 8)

SAND = "#minecraft:sand"
RECIPES = [
    ("sandbags", "sandbags", ["SSS", "SWS", "SSS"], {"S": SAND, "W": "#minecraft:wool"}, 8),
    ("timber_revetment", "timber_revetment", ["TPT", "TPT", "TPT"], {"T": "minecraft:stick", "P": "#minecraft:planks"}, 6),
    ("duckboard", "duckboard", ["TTT", "PPP"], {"T": "minecraft:stick", "P": "#minecraft:wooden_slabs"}, 6),
    ("barbed_wire", "barbed_wire", ["N N", " N ", "N N"], {"N": "minecraft:iron_nugget"}, 4),
    ("field_telephone", "field_telephone", ["PBP", "CRC"],
     {"P": "#c:plates/iron", "B": "minecraft:bell", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, 2),
    ("searchlight", "searchlight", ["SGS", "SLS", " I "],
     {"S": "#c:plates/steel", "G": "minecraft:glass", "L": "minecraft:glowstone", "I": "minecraft:iron_ingot"}, 1),
]
LAMP_TEXTURES = {}


def blocks():
    out = []
    for block, (_, kind, _, _) in BLOCKS.items():
        out.append(block)
        if kind == "family":
            out += [f"{block}_slab", f"{block}_stairs"]
    return out


# ------------------------------------------------------------------ models

def _faces(texture, **overrides):
    faces = {face: {"texture": texture} for face in ("north", "south", "east", "west", "up", "down")}
    for face, tex in overrides.items():
        faces[face] = {"texture": tex}
    return faces


def duckboard_elements():
    """Two bearers along z and four slats across them."""
    out = [{"from": [x, 0, 0], "to": [x + 2, 1, 16], "faces": _faces("#wood")} for x in (2, 12)]
    out += [{"from": [0, 1, z], "to": [16, 2.5, z + 3], "faces": _faces("#wood")} for z in (0.5, 4.5, 8.5, 12.5)]
    return out


def telephone_elements():
    """An olive field telephone facing north: the case with its dial plate, the crank and the handset on its cradle."""
    return [
        {"from": [4, 0, 5], "to": [12, 8, 12], "faces": _faces("#case", north="#front")},
        {"from": [3, 3, 7], "to": [4, 4, 8], "faces": _faces("#metal")},
        {"from": [2, 3, 6], "to": [3, 6, 7], "faces": _faces("#metal")},
        {"from": [5, 8, 7], "to": [11, 9, 9], "faces": _faces("#metal")},
        {"from": [4.5, 9, 7.5], "to": [11.5, 10.5, 8.5], "faces": _faces("#handset")},
        {"from": [4, 9, 7], "to": [5.5, 11, 9], "faces": _faces("#handset")},
        {"from": [10.5, 9, 7], "to": [12, 11, 9], "faces": _faces("#handset")},
    ]


def searchlight_base_elements():
    """The searchlight's fixed pedestal; the yoke, drum and beam turn on top of it (client/SearchlightRenderer)."""
    return [
        {"from": [3, 0, 3], "to": [13, 2, 13], "faces": _faces("#base")},
        {"from": [5, 2, 5], "to": [11, 4, 11], "faces": _faces("#base", up="#top")},
    ]


RIVETED, LACQUER, BRASS, SKID = "ik_lacquer_riveted", "ik_lacquer", "ik_brass", "dr_skid"


def yoke():
    """The yoke, about the pivot: a turntable and two arms holding the drum (turns with the yaw only)."""
    m = cyl("y", 0, 0, 3.5, -7, -5, SKID, BRASS)
    for x0, x1 in ((-6.5, -5), (5, 6.5)):
        m.append(box((x0, -5, -1), (x1, 1.5, 1), SKID))
    return m


def drum():
    """The lamp drum, about the pivot, pointing along +z: a lacquered barrel with brass rims and cooling fins."""
    m = cyl("z", 0, 0, 5, -5, 4, RIVETED, LACQUER)
    m += cyl("z", 0, 0, 5.5, 3.5, 5, BRASS)
    m += cyl("z", 0, 0, 5.5, -5.5, -4.5, BRASS)
    for z in (-3, -1, 1):
        m += cyl("z", 0, 0, 5.4, z, z + 0.5, SKID)
    return m


def lens():
    """The glowing lens in the drum's mouth (drawn at full brightness)."""
    return [box((-4, -4, 4.75), (4, 4, 5.25), {"*": SKID, "south": "ts_lens!"})]


def beam():
    """The light beam, along +z from the lens: three widening, fading segments, seen from inside and out."""
    m = []
    for (z0, z1, r, tex) in ((5, 96, 4, "ts_beam_1"), (96, 224, 7, "ts_beam_2"), (224, BEAM_BLOCKS * 16, 11, "ts_beam_3")):
        faces = {face: f"{tex}!" for face in ("north", "south", "east", "west", "up", "down")}
        faces["north"] = faces["south"] = None
        m.append(box((-r, -r, z0), (r, r, z1), {k: v for k, v in faces.items() if v}))
    return m


def export():
    beam_quads = tiled_quads(beam())
    for quad in beam_quads:
        quad["nocull"] = True
    return {"searchlight_yoke": tiled_quads(yoke()), "searchlight_drum": tiled_quads(drum()),
            "searchlight_lens": tiled_quads(lens()), "searchlight_beam": beam_quads}


def write_all(write, assets, data, lang, condition, self_drop):
    import sys
    from dieselworks import write_blocks

    class Standard:
        """The family blocks, written by tools/dieselworks.write_blocks."""
        BLOCKS = {k: v for k, v in BLOCKS.items() if v[1] == "family"}
        TEXTURES = TEXTURES
        TOOLTIPS = {}
        RECIPES = []
        LAMP_TEXTURES = {}

    Standard.RECIPES = [r for r in RECIPES if BLOCKS[r[1]][1] == "family"]
    write_blocks(write, assets, data, lang, condition, self_drop, Standard)
    models = assets / "models" / "block"
    special = {
        "duckboard": {"ambientocclusion": False, "textures": {"wood": f"{MOD}:block/ts_wood", "particle": f"{MOD}:block/ts_wood"},
                      "elements": duckboard_elements()},
        "barbed_wire": {"parent": "minecraft:block/cross", "textures": {"cross": f"{MOD}:block/ts_barbed_wire"}},
        "field_telephone": {"textures": {"case": f"{MOD}:block/ts_phone_case", "front": f"{MOD}:block/ts_phone_front",
                                         "metal": f"{MOD}:block/dr_skid", "handset": f"{MOD}:block/ik_lacquer",
                                         "particle": f"{MOD}:block/ts_phone_case"}, "elements": telephone_elements()},
        "searchlight": {"textures": {"base": f"{MOD}:block/dr_skid", "top": f"{MOD}:block/ik_brass",
                                     "particle": f"{MOD}:block/dr_skid"}, "elements": searchlight_base_elements()},
    }
    for block, model in special.items():
        name = BLOCKS[block][0]
        lang[f"block.{MOD}.{block}"] = name
        write(models / f"{block}.json", model)
        ref = f"{MOD}:block/{block}"
        if block == "field_telephone":
            state = {"variants": {f"facing={f}": ({"model": ref, "y": y} if y else {"model": ref})
                                  for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}
        else:
            state = {"variants": {"": {"model": ref}}}
        write(assets / "blockstates" / f"{block}.json", state)
        if block in ("barbed_wire", "searchlight"):
            write(assets / "models" / "item" / f"{block}.json",
                  {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{block}"}})
            write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{block}"}})
        else:
            write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": ref}})
        write(data / "loot_table" / "blocks" / f"{block}.json", self_drop(block))
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"message.{MOD}.telephone.channel"] = "Field telephone on the %s channel"
    lang[f"message.{MOD}.searchlight.aim"] = "Searchlight turned to %s°, tilted %s° up"
    from dieselworks import shaped
    for name, result, pattern, key, count in RECIPES:
        if BLOCKS[result][1] != "family":
            write(data / "recipe" / f"{name}.json", shaped(condition, pattern, key, result, count))
    (assets / "trench_quads.json").write_text(json.dumps(export(), separators=(",", ":")) + "\n", encoding="utf-8")


# ------------------------------------------------------------------ art

def _img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def _put(img, x, y, c, a=255):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c) + (a,))


BURLAP = [(98, 80, 52), (128, 106, 72), (156, 132, 92), (182, 158, 114), (204, 182, 138)]
WOOD = [(58, 40, 24), (84, 60, 36), (110, 80, 48), (136, 102, 64), (160, 124, 80)]


def sandbags():
    """Stacked burlap sandbags: four courses of plump, wide sacks laid like brickwork, lit from above with dark seams."""
    img = _img()
    for y in range(16):
        course = y // 4
        by = y % 4
        for x in range(16):
            bx = (x + (4 if course % 2 else 0)) % 8
            shade = (4, 3, 2, 1)[by]
            if bx in (0, 7):
                shade = max(0, shade - 2)
            if by == 3 or (bx == 0 and by != 0):
                shade = 0
            _put(img, x, y, BURLAP[shade])
    return img


def wood(seed=5002):
    """Rough timber planks along x: four boards, each lit along its top edge, with a few grain lines and a knot."""
    img = _img()
    for y in range(16):
        row = y % 4
        for x in range(16):
            _put(img, x, y, WOOD[3] if row == 0 else WOOD[0] if row == 3 else WOOD[2])
    for x, y, length in ((2, 1, 5), (9, 2, 4), (5, 6, 6), (1, 10, 4), (8, 9, 5), (4, 14, 5), (12, 13, 3)):
        for i in range(length):
            _put(img, (x + i) % 16, y, WOOD[1])
    _put(img, 12, 5, WOOD[1])
    _put(img, 13, 5, WOOD[0])
    return img


def revetment():
    """A timber revetment: horizontal boards between two dark posts, a strand of wire across them."""
    img = wood(5003)
    for y in range(16):
        if y % 4 == 3:
            for x in range(16):
                _put(img, x, y, WOOD[0])
        for x in (1, 2, 13, 14):
            _put(img, x, y, WOOD[1] if x in (1, 13) else WOOD[0])
    for x in range(16):
        _put(img, x, 6 + (x // 4) % 2, (120, 118, 112))
    return img


def barbed_wire():
    """A coil of barbed wire seen side on (a cross model), see-through between the strands, with barbs."""
    img = _img()
    steel = [(70, 68, 64), (110, 106, 100), (150, 146, 138)]
    for i in range(3):
        cy = 4 + i * 4
        for x in range(16):
            y = round(cy + 2.2 * math.sin((x + i * 3) / 16 * 2 * math.pi * 2))
            _put(img, x, y, steel[1 + (x + i) % 2])
            if x % 4 == i:
                _put(img, x, y - 1, steel[2])
                _put(img, x, y + 1, steel[0])
    for x in (0, 15):
        for y in range(2, 16):
            if y % 3 == 0:
                _put(img, x, y, steel[0])
    return img


def phone_case():
    """The telephone's olive-drab case: a bevelled panel with a dark seam round it and a bolt in each corner."""
    img = _img()
    olive = [(56, 62, 36), (72, 80, 46), (88, 98, 58), (104, 114, 70), (128, 136, 90)]
    plate(img, olive)
    corner_bolts(img, olive)
    return img


def phone_front():
    """The front: a brass plate with a dial and a call button on the olive case."""
    img = phone_case()
    for y in range(3, 13):
        for x in range(3, 13):
            _put(img, x, y, BRASS_C[2])
    for y in range(4, 10):
        for x in range(5, 11):
            r = math.hypot(x - 7.5, y - 6.5)
            if 1.2 < r <= 2.8:
                _put(img, x, y, (30, 30, 34))
            elif r <= 1.2:
                _put(img, x, y, BRASS_C[3])
    for x in (6, 7, 8, 9):
        _put(img, x, 11, (150, 30, 30))
    return img


BRASS_C = [(104, 78, 34), (146, 112, 52), (184, 148, 74), (214, 182, 104)]


def lens_tex():
    """The searchlight's lens: bright white-gold glass with concentric Fresnel rings."""
    img = _img()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            t = max(0.0, 1 - r / 11)
            c = (round(230 + 25 * t), round(220 + 32 * t), round(170 + 70 * t))
            if round(r) in (3, 5, 7):
                c = tuple(max(0, v - 30) for v in c)
            _put(img, x, y, c)
    return img


def beam_tex(alpha):
    """A beam segment: soft warm-white light, brightest along the middle and fading toward the edges."""
    img = _img()
    for y in range(16):
        for x in range(16):
            edge = 1 - abs(x - 7.5) / 8
            _put(img, x, y, (255, 246, 214), round(alpha * (0.35 + 0.65 * edge)))
    return img


def icon_wire():
    img = barbed_wire()
    return img


def icon_searchlight():
    """The item: a black searchlight on its yoke, its lens glowing."""
    img = _img()
    black, brass, steel = (30, 30, 38), (196, 160, 80), (90, 88, 84)
    for y in range(3, 11):
        for x in range(3, 13):
            _put(img, x, y, black)
    for y in range(4, 10):
        for x in range(10, 14):
            _put(img, x, y, (255, 240, 190) if x >= 12 else brass)
    for x in range(4, 12):
        _put(img, x, 12, steel)
        _put(img, x, 14, steel)
    for y in range(11, 15):
        _put(img, 7, y, steel)
        _put(img, 8, y, steel)
    return img


def draw_all(save):
    for name, img in (("ts_sandbags", sandbags()), ("ts_wood", wood()), ("ts_revetment", revetment()),
                      ("ts_barbed_wire", barbed_wire()), ("ts_phone_case", phone_case()), ("ts_phone_front", phone_front()),
                      ("ts_lens", lens_tex()), ("ts_beam_1", beam_tex(150)), ("ts_beam_2", beam_tex(95)),
                      ("ts_beam_3", beam_tex(50))):
        save(img, "block", name)
    save(icon_wire(), "item", "barbed_wire")
    save(icon_searchlight(), "item", "searchlight")
