"""The speed gate (nuclear/SpeedGateBlock.java, SpeedGatePedestalBlock.java, client/SpeedGateRenderer.java): a sliding
glass security gate like the new fare gates in New York's Grand Central (Conduent, 2024) and office speed gates.

Justin, October 8 2026: "the ones with the two doors that open ... it'll just open one way. So there'll be the through
lane, it'll open for you, and then the exit lane will open for you if you're exiting"; then "I want it to slide
inwards ... the sliding doors that like slide in and then slide back toward each other ... You can make them bigger",
with the Grand Central gates as the example: long brushed stainless pedestals with rounded ends, a stainless arch on
each housing the glass doors when they slide in, tall glass doors edge-lit with green light bars, a card reader and a
small screen at each end, a dark mat down the lane, and a sign gantry overhead (a green arrow over a lane you may go
through, a red cross over one you may not).

A player needs a lane a block wide, so across the lane everything is about twice real size: the pedestals are 12 px
(0.75 m) wide, two blocks long, 1 m high (the arch 1.6 m); each door 10.5 px wide, 1.3 m of glass 0.19 m off the floor,
and the two overlap a pixel in the middle (they run a little apart, one each side of the line). A door slides into its
pedestal, under the arch, and out again. The gate is the lane block at the end you come in by; the block ahead of it
is left clear (the lane's second block); a pedestal block each side of both (shared between lanes side by side).

Models are drawn for a lane you walk through going north (facing north): the lane block is the south (approach)
block, the doors run along its north edge (the line between the gate's two blocks). Pedestals are drawn lengthwise
north-south (axis z), the half on the south side of the line (half=b); the north half is its mirror image.
"""
import numpy as np
from PIL import Image, ImageDraw

import nuclear_materials as M

MOD = "jugcraft"
GATE = "speed_gate"
PEDESTAL = "speed_gate_pedestal"
DISPLAY = "Speed Gate"
CRAFTING = {GATE: (["GSG", "PRP"], {"G": "minecraft:glass_pane", "S": "#c:plates/steel", "P": "#c:plates/steel",
                                     "R": "minecraft:redstone"}, 1)}
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
PARTS = ("lane", "ghost", "door_left", "door_right", "leds_wait", "leds_go", "gantry", "gantry_east")
LIFT = 16.0             # px: the gantry is modelled this much lower than it stands (a model must keep within -16..32;
                        # the renderer lifts it back), on a post up from a pedestal's arch
DOOR_W = 10.0           # px: each door, from its pedestal's face to the middle of the lane
SLOT = 2.0              # px: the doors run in one of two slots this far either side of the line (lanes side by side
                        # alternate, so the two lanes' doors that slide into a shared pedestal have a slot each)
TEXTURES = ["sg_steel", "sg_steel_top", "sg_end", "sg_plinth", "sg_reader", "sg_screen", "sg_glass", "sg_door_lights",
            "sg_mat", "sg_post", "sg_sign_case", "sg_sign_go", "sg_sign_stop", "sg_led_blue", "sg_led_green", "sg_slot",
            "sg_rubber"]
ALL = ("north", "south", "east", "west", "up", "down")


def _box(f, t, tex, emit=None, faces=ALL):
    out = {}
    for face in faces:
        name = tex.get(face, tex.get("*")) if isinstance(tex, dict) else tex
        if name is None:
            continue
        d = {"texture": "#" + name.rstrip("!")}
        if name.endswith("!"):
            d["uv"] = [0, 0, 16, 16]
        out[face] = d
    e = {"from": [round(v, 4) for v in f], "to": [round(v, 4) for v in t], "faces": out}
    if emit:
        e["light_emission"] = emit
    return e


# ---------------------------------------------------------------- the pedestal (half b: the south half, z 0..16)

def pedestal_elements():
    """The south half of a pedestal lengthwise north-south: the door line is its north edge (z 0). A body 12 px wide
    with a rounded end at z 16 (stepped in) under a brushed lid; over the line, the doors' housing, a stainless box as
    wide as the body and a little taller than the doors, rounded on top; the door slot on this side of the line (2 px
    south of it) down both long faces from the floor to the housing's top, lined with black brush seals (the north
    half has the other slot); a card reader pad and a small screen at the end."""
    steel = {"*": "sg_steel", "up": "sg_steel_top", "north": None}
    els = [
        _box((2.6, 0, 0), (13.4, 0.6, 14.6), {"*": "sg_plinth", "north": None}),
        _box((2, 0.6, 0), (14, 14.4, 13.6), steel),
        # The rounded end: two steps in, top to bottom.
        _box((2.6, 0.6, 13.6), (13.4, 14.4, 15.1), {**steel, "south": "sg_end!"}),
        _box((3.6, 0.6, 15.1), (12.4, 14.4, 15.8), {**steel, "south": "sg_end!"}),
        # The lid.
        _box((2, 14.4, 4.4), (14, 15, 13.6), steel),
        _box((2.6, 14.4, 13.6), (13.4, 15, 15.3), steel),
        _box((3.6, 14.4, 15.3), (12.4, 15, 16), steel),
        # The doors' housing over the line, on the body (z 0..4.4; the north half has the rest), rounded on top.
        _box((2, 14.4, 0), (14, 24.6, 4.4), steel),
        _box((2.6, 24.6, 0), (13.4, 25.4, 4.0), steel),
        _box((3.6, 25.4, 0), (12.4, 25.8, 3.4), steel),
    ]
    # The slot this side of the line on both long faces: black brush seals a hair proud of the steel, as long as the
    # door is high (floor to the housing's top).
    for x0, x1 in ((1.9, 2.0), (14.0, 14.1)):
        els.append(_box((x0, 2.4, SLOT - 0.7), (x1, 24.8, SLOT + 0.7), {"*": "sg_slot", "north": "sg_slot", "south": "sg_slot"}))
    els += [
        # The end: a card reader pad on the lid and a small screen on a raised wedge facing whoever comes up.
        _box((4.5, 15, 8.6), (11.5, 15.3, 12.2), {"*": "sg_plinth", "up": "sg_reader!"}, 6),
        _box((4.5, 15, 12.8), (11.5, 18.5, 14.8), {"*": "sg_steel", "south": "sg_screen!"}, 9),
    ]
    return els


def _mirror_z(elements):
    """The north half: the south half mirrored north-south (z 16 - z)."""
    out = []
    for e in elements:
        f, t = e["from"], e["to"]
        m = dict(e)
        m["from"] = [f[0], f[1], round(16 - t[2], 4)]
        m["to"] = [t[0], t[1], round(16 - f[2], 4)]
        faces = {}
        for face, d in e["faces"].items():
            d = dict(d)
            if "uv" in d and face in ("east", "west"):
                u0, v0, u1, v1 = d["uv"]
                d["uv"] = [u1, v0, u0, v1]
            if "uv" in d and face in ("up", "down"):
                u0, v0, u1, v1 = d["uv"]
                d["uv"] = [u0, v1, u1, v0]
            faces[{"north": "south", "south": "north"}.get(face, face)] = d
        m["faces"] = faces
        out.append(m)
    return out


# ---------------------------------------------------------------- the lane (facing north; the approach block)

def lane_elements():
    """What the lane block draws itself: the rubber mat down both of the gate's blocks."""
    return [_box((-2, 0, -16), (18, 0.4, 16), {"up": "sg_mat"}, faces=("up",))]


def gantry_elements(west=True):
    """The sign gantry over the lane, LIFT lower than it stands (client/SpeedGateRenderer.java lifts it): a sign box
    from the west pedestal's middle to the east one's, so the boxes of lanes side by side make one gantry, a green
    arrow on its face toward whoever comes in by this end and a red cross on the other (each face showing the middle
    band of its sign texture), on a post up from a pedestal's arch. The post is on the west pedestal (gantry) or the
    east one (gantry_east, for lanes facing south or west: turned, theirs then stands on the west or north pedestal
    too, so each pedestal carries one post)."""
    y0, y1 = 40 - LIFT, 47 - LIFT
    els = [_box((-8, y0, -2.4), (24, y1, 2.4), {"*": "sg_sign_case"}, faces=("east", "west", "up", "down"))]
    # The faces: a thin glowing panel on each open side of the box.
    for face, tex, z0, z1 in (("south", "sg_sign_go", 2.4, 2.5), ("north", "sg_sign_stop", -2.5, -2.4)):
        panel = _box((-8, y0, z0), (24, y1, z1), {"*": "sg_sign_case"}, 13, faces=tuple(f for f in ALL if f != ("north" if face == "south" else "south")))
        panel["faces"][face] = {"texture": "#" + tex, "uv": [0, 6.25, 16, 9.75]}
        els.append(panel)
    x0 = -9.0 if west else 23.0
    els.append(_box((x0, 25.8 - LIFT, -0.8), (x0 + 2, 40 - LIFT, 0.8), "sg_post", faces=("north", "south", "east", "west", "down")))
    return els


def led_elements(colour):
    """Light strips along both pedestals' long faces by the lane, just under the lid (either side of the housing),
    blue while waiting and green while the doors are open."""
    tex = f"sg_led_{colour}"
    els = []
    for x0, x1 in ((-2.0, -1.9), (17.9, 18.0)):
        for z0, z1 in ((-15, -4.6), (4.6, 15)):
            els.append(_box((x0, 13.4, z0), (x1, 14.2, z1), tex, 15))
    return els


def door_elements(side):
    """A door, shut, on the line (the renderer sets it in its slot, SLOT either side): the one from the left (west)
    pedestal reaches east to the middle of the lane, the right one west; glass with green light bars edge-lit in it
    (a hair off each face) and a black rubber edge where they meet."""
    if side == "left":
        g0, g1, r0, r1 = -2.0, 8.0 - 0.5, 8.0 - 0.5, 8.0
    else:
        g0, g1, r0, r1 = 8.0 + 0.5, 18.0, 8.0, 8.0 + 0.5
    z0, z1 = -0.3, 0.3
    glass = {"north": "sg_glass!", "south": "sg_glass!", "up": "sg_glass", "down": "sg_glass"}
    return [
        _box((g0, 3, z0), (g1, 24, z1), glass, faces=("north", "south", "up", "down")),
        _box((g0, 3, z0 - 0.12), (g1, 24, z1 + 0.12), {"north": "sg_door_lights!", "south": "sg_door_lights!"}, 15,
             faces=("north", "south")),
        _box((r0, 3, z0 - 0.05), (r1, 24, z1 + 0.05), "sg_rubber"),
    ]


def textures(rid):
    tex = {name: rid(f"block/{name}") for name in TEXTURES}
    tex["sg_glass"] = {"sprite": rid("block/sg_glass"), "force_translucent": True}
    tex["particle"] = rid("block/sg_steel")
    return tex


def models(rid):
    parts = {
        "lane": lane_elements(),
        # The placement preview: the gate as it stands, but for the gantry (out of a model's reach, and the renderer
        # does not draw a preview).
        "ghost": lane_elements() + led_elements("blue")
                 + [_shift(e, 0, 0, SLOT) for e in door_elements("left") + door_elements("right")],
        "door_left": door_elements("left"),
        "door_right": door_elements("right"),
        "leds_wait": led_elements("blue"),
        "leds_go": led_elements("green"),
        "gantry": gantry_elements(True),
        "gantry_east": gantry_elements(False),
    }
    out = {f"{GATE}_{part}": {"textures": textures(rid), "elements": els} for part, els in parts.items()}
    south = pedestal_elements()
    out[f"{PEDESTAL}_b"] = {"parent": "minecraft:block/block", "textures": textures(rid), "elements": south}
    out[f"{PEDESTAL}_a"] = {"parent": "minecraft:block/block", "textures": textures(rid), "elements": _mirror_z(south)}
    out[f"{GATE}_item"] = {"parent": "minecraft:block/block", "textures": textures(rid),
                           "elements": [_shift(e, 0, 0, -6) for e in south]}
    return out


def _shift(e, dx, dy, dz):
    e = dict(e)
    e["from"] = [e["from"][0] + dx, e["from"][1] + dy, e["from"][2] + dz]
    e["to"] = [e["to"][0] + dx, e["to"][1] + dy, e["to"][2] + dz]
    return e


def blockstates(rid):
    gate, ped = {}, {}
    for facing, y in FACINGS.items():
        for part in PARTS:
            for opened in ("false", "true"):
                v = {"model": rid(f"block/{GATE}_{part}")}
                if y:
                    v["y"] = y
                gate[f"facing={facing},open={opened},part={part}"] = v
    for axis, y in (("z", 0), ("x", 90)):
        for half in ("a", "b"):
            v = {"model": rid(f"block/{PEDESTAL}_{half}")}
            if y:
                v["y"] = y
            ped[f"axis={axis},half={half}"] = v
    return {GATE: {"variants": gate}, PEDESTAL: {"variants": ped}}


# ---------------------------------------------------------------- textures (64 px)

def _steel(direction="h", seed=7401):
    return M.stylize(M.brushed((186, 190, 194), seed, direction, dirt=0.04), 12)


def _steel_top():
    return M.stylize(M.brushed((196, 200, 204), 7402, "v", dirt=0.03), 12)


def _end():
    """A pedestal's rounded end: brushed steel, a dark band of passage sensors."""
    img = M.brushed((186, 190, 194), 7403, "h", dirt=0.04)
    d = ImageDraw.Draw(img)
    d.rectangle([14, 20, 113, 32], fill=(24, 26, 30, 255))
    for x in range(22, 110, 14):
        d.ellipse([x, 23, x + 6, 29], fill=(120, 30, 30, 255))
    return M.stylize(img, 12)


def _plinth():
    return M.stylize(M.painted((44, 46, 48), 7404, chip_count=0, drip_count=0, dirt=0.2), 8)


def _reader():
    """The card reader: a black oval pad with the contactless mark."""
    img = Image.new("RGBA", (128, 128), (40, 42, 46, 255))
    d = ImageDraw.Draw(img)
    d.ellipse([6, 14, 121, 113], fill=(14, 15, 17, 255), outline=(90, 94, 100, 255), width=4)
    for r in (14, 26, 38):
        d.arc([44 - r, 64 - r, 44 + r, 64 + r], -50, 50, fill=(210, 214, 220, 255), width=6)
    return M.stylize(img, 8)


def _screen():
    """The small screen: blue, a white 'go' arrow and a line of text."""
    img = Image.new("RGBA", (128, 128), (60, 62, 66, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 10, 117, 117], fill=(24, 64, 150, 255))
    d.rectangle([10, 10, 117, 30], fill=(16, 40, 100, 255))
    d.polygon([(64, 40), (92, 68), (76, 68), (76, 96), (52, 96), (52, 68), (36, 68)], fill=(240, 244, 250, 255))
    d.rectangle([24, 102, 104, 108], fill=(180, 196, 230, 255))
    return M.stylize(img, 10)


def _glass(size=64):
    out = np.zeros((size, size, 4), np.uint8)
    out[..., :3] = (200, 222, 222)
    out[..., 3] = 46
    return Image.fromarray(out, "RGBA")


def _door_lights(size=128):
    """The doors' light: a green line round the edge and three rows of dashed green bars in the glass (cut out
    everywhere else), as on the Grand Central gates."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    g = (70, 255, 140, 255)
    d.rectangle([3, 3, size - 4, size - 4], outline=g, width=4)
    for y in (int(size * 0.22), int(size * 0.5), int(size * 0.78)):
        for x in range(14, size - 20, 22):
            d.rectangle([x, y - 2, x + 12, y + 2], fill=g)
    return img.resize((64, 64), Image.NEAREST)


def _mat():
    """The lane's rubber mat: dark, ribbed along the lane."""
    img = M.painted((40, 42, 44), 7405, chip_count=0, drip_count=0, dirt=0.3)
    d = ImageDraw.Draw(img)
    for x in range(0, 128, 8):
        d.rectangle([x, 0, x + 2, 127], fill=(28, 30, 32, 255))
    return M.stylize(img, 8)


def _post():
    return M.stylize(M.painted((70, 74, 78), 7406, chip_count=0, drip_count=0, dirt=0.1), 8)


def _sign_case():
    return M.stylize(M.painted((50, 52, 56), 7407, chip_count=0, drip_count=0, dirt=0.1), 8)


def _sign(colour, arrow):
    """A gantry sign's face: dark, the pictogram in the middle of the band its face shows (v 50..78 of 128, as wide
    as the box is long: 32 px of box to 7 px of height), over the lane in the middle of the box."""
    img = Image.new("RGBA", (128, 128), (20, 22, 24, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 50, 127, 78], outline=(70, 74, 80, 255), width=1)
    cx, top, bottom = 64, 53, 75
    if arrow:
        d.polygon([(cx, bottom), (cx + 10, bottom - 10), (cx + 4, bottom - 10), (cx + 4, top), (cx - 4, top),
                   (cx - 4, bottom - 10), (cx - 10, bottom - 10)], fill=colour)
    else:
        d.line([(cx - 9, top + 1), (cx + 9, bottom - 1)], fill=colour, width=5)
        d.line([(cx + 9, top + 1), (cx - 9, bottom - 1)], fill=colour, width=5)
    return img


def _led(colour):
    img = Image.new("RGBA", (64, 64), colour + (255,))
    d = ImageDraw.Draw(img)
    for x in range(2, 64, 8):
        d.rectangle([x, 0, x + 4, 63], fill=tuple(min(255, c + 40) for c in colour) + (255,))
    return img


def _slot():
    """A slot's brush seal: black bristles."""
    img = Image.new("RGBA", (64, 64), (16, 16, 18, 255))
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 3):
        d.line([(0, y), (63, y)], fill=(34, 34, 38, 255))
    return img


def _rubber():
    return M.stylize(M.painted((30, 30, 32), 7408, chip_count=0, drip_count=0, dirt=0.1), 6)


def draw_textures():
    return {"sg_steel": _steel(), "sg_steel_top": _steel_top(), "sg_end": _end(), "sg_plinth": _plinth(),
            "sg_reader": _reader(), "sg_screen": _screen(), "sg_glass": _glass(), "sg_door_lights": _door_lights(),
            "sg_mat": _mat(), "sg_post": _post(), "sg_sign_case": _sign_case(),
            "sg_sign_go": _sign((70, 235, 110, 255), True), "sg_sign_stop": _sign((240, 60, 50, 255), False),
            "sg_led_blue": _led((60, 140, 255)), "sg_led_green": _led((60, 230, 100)), "sg_slot": _slot(),
            "sg_rubber": _rubber()}


def write_assets(write, rid, assets, lang):
    lang[f"block.{MOD}.{GATE}"] = DISPLAY
    lang[f"block.{MOD}.{PEDESTAL}"] = "Speed Gate Pedestal"
    for name, model in models(rid).items():
        write(assets / "models" / "block" / f"{name}.json", model)
    for name, bs in blockstates(rid).items():
        write(assets / "blockstates" / f"{name}.json", bs)
    write(assets / "items" / f"{GATE}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{GATE}_item")}})
