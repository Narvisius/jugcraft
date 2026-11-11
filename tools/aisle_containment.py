"""The data hall's new pieces (Tri-Pillar work list item 13, approved by Justin on 22 October 2026 from the mockup; see
claude/jugcraft-tripillar-data-hall.md): the Aisle Containment Door, the Aisle Containment Roof and the Data Hall Sign.
They close the cold aisle between two rows of the plant's server racks (tools/server_room.py), the way a contained data
hall is built.

Aisle Containment Door (tower/AisleContainmentDoorBlock.java, client/AisleContainmentDoorRenderer.java): bi-parting
automatic sliding doors at an end of the aisle, two blocks wide and two high, set against the end racks. An anodised
aluminium frame: jambs on the racks' front edges, the header and track over both end racks (so each leaf slides a block
over its end rack, in front of it), the label plate (COLD AISLE and the aisle's tag, the rows' tags over their racks:
drawn live by the renderer), a motion sensor under the header's middle each side with its ready light. Each leaf: an
aluminium frame (stiles, top rail, a deep bottom rail), tinted tempered glass, a brush seal along its foot, a rubber seal
on its meeting edge, a slim pull bar on both faces.

The door is drawn facing north (its outside to -z, the aisle behind it to +z): the racks' end plane at z 16 of its cells,
the frame standing out to z 13.76. Across, the aisle's middle is x 16 (the master cell x 0..16, the other 16..32); the
header runs x -16..48 (over the end racks), so its models are split at the middle: frame_left (x -16..16) and frame_right
(drawn a block over: x 0..32). The leaves are their own models (leaf_right also drawn a block over), slid by the renderer.

Aisle Containment Roof (tower/AisleContainmentRoofBlock.java): over the aisle on the racks' tops, a block a half of the
aisle's width: an aluminium rail resting on the racks' top front edge, a tee every metre with its red fusible links (the
panels drop away in a fire so the clean agent gets in), a frosted polycarbonate panel, the LED light bar along the aisle's
middle (lit). Drawn facing north: the north half of an aisle running east-west, its rail to the north, the aisle's middle
at z 16.

Data Hall Sign (a MultiFurnitureBlock 4 wide, 1 high): the light box over the data hall's doors: a dark steel case, the
lit face (DATA HALL in amber, AUTHORISED PERSONNEL ONLY and CLEAN AGENT PROTECTED under it), aluminium trims. Drawn
facing north, its back to the wall at z 0.

Run from the repository root: python3 tools/aisle_containment.py
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import nuclear_materials as M
import tower_control_console as TCC

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"
TEX = ASSETS / "textures/block"
DOOR, DOOR_PART, ROOF, SIGN = "aisle_containment_door", "aisle_containment_door_part", "aisle_containment_roof", "data_hall_sign"
NAMES = {DOOR: "Aisle Containment Door", DOOR_PART: "Aisle Containment Door", ROOF: "Aisle Containment Roof", SIGN: "Data Hall Sign"}
PARTS = ("none", "frame_left", "frame_right", "leaf_left", "leaf_right", "led_ready", "led_open")
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
GLOW = 15
ALL = ("north", "south", "east", "west", "up", "down")

# ---------------------------------------------------------------- the door, in its own units (blocks)
# u across the aisle (its middle 0, the racks' fronts at +-1.034), v up from the floor, w out of the aisle from the end
# racks' plane. (The mockup's numbers: claude/src/dronetower/tools/mockups/dh_parts.py.)
OPEN_U = 0.985                   # how far each leaf slides (Java: AisleContainmentDoorBlock.SLIDE)
HEADER = (1.86, 1.99, 0.0, 0.14)  # v0, v1, w0, w1
PLATE = (-0.56, 0.56, 1.885, 1.965, 0.147)    # u0, u1, v0, v1, w (its face: 0.11 px proud of the header's)
TAG_U, TAG_HW = 1.5, 0.23        # the rows' tags: their middles at u +-1.5, half width
SENSOR = (-0.07, 0.07, 1.805, 1.86)
LEAF_W = (0.095, 0.13)
STILE = 0.045


def px(u):
    return 16 + 16 * u


def door_boxes():
    """The fixed frame: [(u0, u1, v0, v1, w0, w1, texture, faces)]."""
    b = []
    for s in (-1, 1):
        b.append((*sorted((s * 1.00, s * 1.075)), 0.0, 1.86, 0.0, 0.085, "acd_alu", ALL))
        b.append((*sorted((s * 1.075, s * 1.09)), 0.02, 1.84, 0.0, 0.07, "acd_rubber", ("north", "east", "west", "up", "down")))
        b.append((*sorted((s * 1.98, s * 2.0)), 1.86, 1.99, 0.0, 0.14, "acd_alu_end", ALL))
    v0, v1, w0, w1 = HEADER
    b.append((-1.98, 1.98, v0, v1, w0, w1, "acd_alu", ("north", "south", "up", "down")))
    b.append((-1.98, 1.98, 1.845, 1.86, 0.11, 0.135, "acd_alu_dark", ("north", "south", "down")))
    # the label plate and the rows' tag plates, proud of the header's face
    u0, u1, pv0, pv1, pw = PLATE
    b.append((u0, u1, pv0, pv1, w1, pw, "acd_plate", ("north", "east", "west", "up", "down")))
    for s in (-1, 1):
        b.append((s * TAG_U - TAG_HW, s * TAG_U + TAG_HW, 1.895, 1.955, w1, pw, "acd_tag", ("north", "east", "west", "up", "down")))
    # the sensors: outside and in
    su0, su1, sv0, sv1 = SENSOR
    b.append((su0, su1, sv0, sv1, 0.14, 0.20, "acd_black", ALL))
    b.append((su0, su1, sv0, sv1, -0.06, 0.0, "acd_black", ALL))
    b.append((-0.05, 0.03, 1.815, 1.85, 0.20, 0.2065, "acd_lens", ALL))
    b.append((-0.05, 0.03, 1.815, 1.85, -0.0665, -0.06, "acd_lens", ALL))
    return b


def leaf_boxes(s):
    """A leaf, closed: s -1 the one over u -1.025..0, +1 over 0..1.025."""
    lo, hi = sorted((0.0, s * 1.025))
    w0, w1 = LEAF_W
    b = [(lo, lo + STILE, 0.03, 1.84, w0, w1, "acd_alu", ALL), (hi - STILE, hi, 0.03, 1.84, w0, w1, "acd_alu", ALL),
         (lo + STILE, hi - STILE, 1.78, 1.84, w0, w1, "acd_alu", ("north", "south", "up", "down")),
         (lo + STILE, hi - STILE, 0.03, 0.13, w0, w1, "acd_alu", ("north", "south", "up", "down")),
         (lo, hi, 0.004, 0.03, w0 + 0.008, w1 - 0.008, "acd_seal", ALL),
         (lo + STILE, hi - STILE, 0.13, 1.78, 0.1105, 0.1145, "acd_glass", ("north", "south"))]
    # the meeting edge's rubber seal, standing just proud of the stile (never sharing its faces)
    m0, m1 = (lo - 0.008, lo) if s > 0 else (hi, hi + 0.008)
    b.append((m0, m1, 0.035, 1.835, w0 + 0.01, w1 - 0.01, "acd_rubber", ALL))
    pu = s * 0.11
    for out in (True, False):
        wb = (w1, w1 + 0.022) if out else (w0 - 0.022, w0)
        for vy in (0.86, 1.14):
            b.append((pu - 0.008, pu + 0.008, vy - 0.008, vy + 0.008, *wb, "acd_alu_dark", ALL))
        wbar = (w1 + 0.022, w1 + 0.038) if out else (w0 - 0.038, w0 - 0.022)
        b.append((pu - 0.009, pu + 0.009, 0.84, 1.16, *wbar, "acd_alu", ALL))
    return b


def led_boxes(tex):
    return [(0.042, 0.056, 1.826, 1.840, 0.20, 0.204, tex, ALL), (0.042, 0.056, 1.826, 1.840, -0.064, -0.06, tex, ALL)]


# ---------------------------------------------------------------- JSON elements

def _uv_for(face, f, t):
    """Minecraft's own UV for a face, as a box's position gives it (so tiling textures line up)."""
    x0, y0, z0 = f
    x1, y1, z1 = t
    if face == "north":
        return [16 - x1, 16 - y1, 16 - x0, 16 - y0]
    if face == "south":
        return [x0, 16 - y1, x1, 16 - y0]
    if face == "west":
        return [z0, 16 - y1, z1, 16 - y0]
    if face == "east":
        return [16 - z1, 16 - y1, 16 - z0, 16 - y0]
    if face == "up":
        return [x0, z0, x1, z1]
    return [x0, 16 - z1, x1, 16 - z0]


def _wrap(uv):
    """Keeps a face's UV inside 0..16 (shifted by whole blocks; the textures tile)."""
    u0, v0, u1, v1 = uv
    du = -16 * np.floor(min(u0, u1) / 16)
    dv = -16 * np.floor(min(v0, v1) / 16)
    out = [u0 + du, v0 + dv, u1 + du, v1 + dv]
    if max(out[0], out[2]) > 16.001 or max(out[1], out[3]) > 16.001:
        # a face wider than a block: squeeze it into one tile (only small faces do: rails, plates)
        out = [max(0, min(16, c)) for c in out]
    return [round(c, 4) for c in out]


def element(f, t, tex, faces=ALL, emit=None, uv=None):
    e = {"from": [round(c, 4) for c in f], "to": [round(c, 4) for c in t], "faces": {}}
    for face in faces:
        d = {"texture": "#" + tex}
        d["uv"] = uv[face] if uv and face in uv else _wrap(_uv_for(face, f, t))
        e["faces"][face] = d
    if emit:
        e["light_emission"] = emit
    return e


def door_element(box, shift=0.0, emit=None, uv=None):
    """A door box (u, v, w) as an element of the door drawn facing north (x across, y up, the outside to -z), moved
    shift px across."""
    u0, u1, v0, v1, w0, w1, tex, faces = box
    if tex == "acd_glass":
        uv = {"north": [0, 0, 16, 16], "south": [0, 0, 16, 16]}
    f = (px(u0) - shift, 16 * v0, 16 - 16 * w1)
    t = (px(u1) - shift, 16 * v1, 16 - 16 * w0)
    return element(f, t, tex, faces, emit, uv)


def split(boxes, x_split=16.0):
    """The frame's boxes cut at the aisle's middle into those of frame_left (x <= 16) and frame_right (x >= 16)."""
    left, right = [], []
    for b in boxes:
        u0, u1 = b[0], b[1]
        x0, x1 = px(u0), px(u1)
        if x1 <= x_split + 1e-6:
            left.append(b)
        elif x0 >= x_split - 1e-6:
            right.append(b)
        else:
            um = (x_split - 16) / 16
            fl = tuple(f for f in b[7] if f != "east")
            fr = tuple(f for f in b[7] if f != "west")
            left.append((u0, um) + tuple(b[2:7]) + (fl,))
            right.append((um, u1) + tuple(b[2:7]) + (fr,))
    return left, right


def plate_uv(box, side):
    """The label plate's face cut in two: the half on frame_right holds the image's left (seen from outside, -z, the
    viewer's left is +x)."""
    u0, u1 = box[0], box[1]
    p0, p1 = PLATE[0], PLATE[1]
    # image u (0..16) at the face's left edge seen from outside (larger x) and its right edge
    a = 16 * (p1 - max(u0, u1)) / (p1 - p0)
    c = 16 * (p1 - min(u0, u1)) / (p1 - p0)
    edge = [0, 0, 0.5, 0.5]       # the plate's blue border
    return {"north": [round(a, 4), 0, round(c, 4), 1.125], "east": edge, "west": edge, "up": edge, "down": edge}


def door_models():
    out = {}
    textures = {k: f"jugcraft:block/{k}" for k in ("acd_alu", "acd_alu_dark", "acd_alu_end", "acd_rubber", "acd_seal", "acd_glass",
                                                    "acd_plate", "acd_tag", "acd_black", "acd_lens", "acd_led_green", "acd_led_amber")}
    textures["particle"] = "jugcraft:block/acd_alu"
    left, right = split(door_boxes())
    for name, boxes, shift in (("frame_left", left, 0.0), ("frame_right", right, 16.0)):
        els = []
        for b in boxes:
            uv = plate_uv(b, name) if b[6] == "acd_plate" else None
            els.append(door_element(b, shift, uv=uv))
        out[f"{DOOR}_{name}"] = {"textures": textures, "elements": els}
    out[f"{DOOR}_leaf_left"] = {"textures": textures, "elements": [door_element(b) for b in leaf_boxes(-1)]}
    out[f"{DOOR}_leaf_right"] = {"textures": textures, "elements": [door_element(b, 16.0) for b in leaf_boxes(1)]}
    out[f"{DOOR}_led_ready"] = {"textures": textures, "elements": [door_element(b, emit=GLOW) for b in led_boxes("acd_led_green")]}
    out[f"{DOOR}_led_open"] = {"textures": textures, "elements": [door_element(b, emit=GLOW) for b in led_boxes("acd_led_amber")]}
    out[f"{DOOR}_none"] = {"textures": {"particle": "jugcraft:block/acd_alu"}, "elements": []}
    return out


# ---------------------------------------------------------------- the roof (a half of the aisle, facing north)

def roof_model():
    e = []
    t = {"alu": "jugcraft:block/acd_alu", "dark": "jugcraft:block/acd_alu_dark", "frost": "jugcraft:block/acr_frost",
         "led": "jugcraft:block/acr_led", "red": "jugcraft:block/acr_link", "particle": "jugcraft:block/acd_alu"}
    z = lambda a: 16 - 16 * a          # the aisle's middle at z 16, a blocks out from it
    # the rail on the racks' top front edge
    e.append(element((0, -0.08, z(1.12)), (16, 0.64, z(0.95)), "alu", ("north", "south", "up", "down")))
    # half tees at both edges (a whole tee where two cells meet), their fusible links
    for x0, x1 in ((0, 0.32), (15.68, 16)):
        e.append(element((x0, 0, z(0.95)), (x1, 0.64, 16), "alu", ("east", "west", "up", "down")))
    for x0, x1 in ((0, 0.19), (15.81, 16)):
        e.append(element((x0, -0.32, z(0.45) - 0.19), (x1, 0, z(0.45) + 0.19), "red", ("north", "south", "east", "west", "down")))
    # the frosted panel (both faces)
    e.append(element((0.32, 0.46, z(0.95)), (15.68, 0.5, 16), "frost", ("up", "down")))
    # the LED bar's body along the aisle's middle and its lit diffuser under it
    e.append(element((0.64, 0.16, z(0.035)), (15.36, 0.46, 16), "dark", ("north", "down", "east", "west")))
    e.append(element((0.8, 0.02, z(0.026)), (15.2, 0.06, 16), "led", ("north", "up", "down", "east", "west"), emit=GLOW))
    return {"textures": t, "elements": e}


# ---------------------------------------------------------------- the sign (4 parts, facing north: its back at z 0)

SIGN_Y = (2.72, 13.28)


def sign_models():
    out = {}
    y0, y1 = SIGN_Y
    for p in range(4):
        t = {"case": "jugcraft:block/dhs_case", "trim": "jugcraft:block/acd_alu_dark", "face": f"jugcraft:block/dhs_face_{p}",
             "particle": "jugcraft:block/dhs_case"}
        sides = ("up", "down") + (("west",) if p == 0 else ()) + (("east",) if p == 3 else ())
        e = [element((0, y0, 0), (16, y1, 1.12), "case", sides),
             element((0, y0, 1.12), (16, y0 + 0.48, 1.36), "trim", ("south", "up", "down") + sides[2:]),
             element((0, y1 - 0.48, 1.12), (16, y1, 1.36), "trim", ("south", "up", "down") + sides[2:]),
             element((0, y0 + 0.48, 1.12), (16, y1 - 0.48, 1.2), "face", ("south",), emit=GLOW,
                     uv={"south": [0, 0, 16, round(16 * (y1 - y0 - 0.96) / 16, 4)]})]
        out[f"{SIGN}_{p}"] = {"textures": t, "elements": e}
    return out


# ---------------------------------------------------------------- textures

def _img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _solid(rgb, alpha=255, size=16):
    return Image.new("RGBA", (size, size), tuple(rgb) + (alpha,))


def _alu(seed, rgb=(184, 188, 193)):
    return M.stylize(M.brushed(rgb, seed, "v", dirt=0.02), 16)


def _glass():
    """Tinted tempered glass: faint grey-green, a soft diagonal sheen."""
    y, x = np.mgrid[0:64, 0:64] / 64.0
    d = x + y * 0.6
    sheen = np.exp(-((d - 0.55) ** 2) / 0.006) * 0.10 + np.exp(-((d - 0.78) ** 2) / 0.002) * 0.06
    a = np.zeros((64, 64, 4))
    a[..., 0] = (0.55 + sheen * 1.5) * 255
    a[..., 1] = (0.64 + sheen * 1.5) * 255
    a[..., 2] = (0.68 + sheen * 1.5) * 255
    a[..., 3] = (0.24 + sheen) * 255
    return _img(a)


def _seal():
    img = Image.new("RGBA", (64, 64), (18, 18, 20, 255))
    d = ImageDraw.Draw(img)
    for x in range(0, 64, 2):
        d.line([(x, 0), (x, 63)], fill=(30 + (x * 7) % 9, 30, 33, 255))
    return img


def _plate():
    """The label plate (512 px): white, a blue border, the aisle tag's blue band on its left; the text is drawn live."""
    W, H = 512, 36
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W - 1, H - 1), fill=(246, 247, 248, 255))
    d.rectangle((0, 0, W - 1, H - 1), outline=(30, 80, 160, 255), width=2)
    d.rectangle((2, 2, 70, H - 3), fill=(30, 90, 180, 255))
    return img


def _tag():
    img = Image.new("RGBA", (64, 64), (232, 233, 228, 255))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 63, 63), outline=(40, 40, 42, 255), width=3)
    return img


def _frost():
    rnd = np.random.default_rng(9)
    a = np.zeros((64, 64, 4))
    a[..., :3] = 245 + rnd.uniform(-5, 5, (64, 64, 1))
    a[..., 3] = 107
    return _img(a)


def _sign_faces():
    """The light box's face, 4 blocks by 0.6 (2048 x 307 px), cut into the four parts' square textures."""
    W, H = 2048, 307
    im = Image.new("RGBA", (W, H), (16, 17, 19, 255))
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype(FONT_B, 170)
    fs = ImageFont.truetype(FONT_B, 44)
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(glow).text((W / 2, 122), "DATA HALL", font=f, fill=(255, 170, 60, 255), anchor="mm")
    im.alpha_composite(glow.filter(ImageFilter.GaussianBlur(9)))
    d.text((W / 2, 122), "DATA HALL", font=f, fill=(255, 196, 110, 255), anchor="mm")
    d.line((230, 228, W - 230, 228), fill=(90, 92, 96, 255), width=4)
    d.text((W / 2, 268), "AUTHORISED PERSONNEL ONLY   ·   CLEAN AGENT PROTECTED", font=fs, fill=(226, 228, 230, 255), anchor="mm")
    out = []
    for p in range(4):
        tile = Image.new("RGBA", (512, 512), (16, 17, 19, 255))
        tile.paste(im.crop((p * 512, 0, p * 512 + 512, H)), (0, 0))
        out.append(tile)
    return out


def draw_textures():
    t = {"acd_alu": _alu(7701), "acd_alu_dark": _alu(7702, (118, 123, 130)), "acd_alu_end": _alu(7703, (160, 165, 170)),
         "acd_rubber": _solid((26, 26, 28)), "acd_seal": _seal(), "acd_glass": _glass(), "acd_plate": _plate(), "acd_tag": _tag(),
         "acd_black": _solid((22, 23, 25)), "acd_lens": _solid((40, 30, 36)), "acd_led_green": _solid((90, 240, 120)),
         "acd_led_amber": _solid((255, 176, 60)), "acr_frost": _frost(), "acr_led": _solid((255, 251, 240)),
         "acr_link": _solid((200, 40, 36)), "dhs_case": M.stylize(M.brushed((58, 62, 68), 7704, "h", dirt=0.05), 14)}
    for p, img in enumerate(_sign_faces()):
        t[f"dhs_face_{p}"] = img
    return t


# ---------------------------------------------------------------- blockstates, items, data

def blockstates():
    door = {}
    for f in FACINGS:
        for o in ("false", "true"):
            for p in PARTS:
                door[f"facing={f},open={o},part={p}"] = {"model": f"jugcraft:block/{DOOR}_{p}"}
    roof = {f"facing={f}": {"model": f"jugcraft:block/{ROOF}", **({"y": y} if y else {})} for f, y in FACINGS.items()}
    sign = {f"facing={f},part={p}": {"model": f"jugcraft:block/{SIGN}_{p}", **({"y": y} if y else {})}
            for f, y in FACINGS.items() for p in range(4)}
    return {DOOR: {"variants": door}, DOOR_PART: {"variants": {"": {"model": f"jugcraft:block/{DOOR}_none"}}},
            ROOF: {"variants": roof}, SIGN: {"variants": sign}}


RECIPES = {
    DOOR: (["IGI", "IGI", "IRI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "R": "minecraft:redstone"}, 1),
    ROOF: (["GGG", "ILI"], {"G": "minecraft:glass_pane", "I": "minecraft:iron_ingot", "L": "minecraft:glowstone_dust"}, 4),
    SIGN: (["SSS", "GDG"], {"S": "#c:plates/steel", "G": "minecraft:glowstone_dust", "D": "minecraft:orange_dye"}, 1),
}


def write_all():
    j = TCC._json
    TEX.mkdir(parents=True, exist_ok=True)
    for name, img in draw_textures().items():
        img.save(TEX / f"{name}.png", optimize=True)
    models = door_models()
    models[ROOF] = roof_model()
    models.update(sign_models())
    for name, m in models.items():
        j(ASSETS / "models/block" / f"{name}.json", m)
    for name, bs in blockstates().items():
        j(ASSETS / "blockstates" / f"{name}.json", bs)
    import item_icons
    for item in (DOOR, ROOF, SIGN):
        j(ASSETS / "models/item" / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{item}"}})
        j(ASSETS / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{item}"}})
        j(DATA / "jugcraft/loot_table/blocks" / f"{item}.json", TCC._loot(item))
        pattern, key, count = RECIPES[item]
        j(DATA / "jugcraft/recipe" / f"{item}.json", TCC._recipe(pattern, key, item, count))
        item_icons.draw(item).save(ASSETS / f"textures/item/{item}.png")
    j(DATA / "jugcraft/loot_table/blocks" / f"{DOOR_PART}.json",
      {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{DOOR_PART}"})
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    for k, v in NAMES.items():
        lang[f"block.jugcraft.{k}"] = v
    lang[f"message.jugcraft.{DOOR}.blocked"] = "The Aisle Containment Door needs a clear space 2 blocks wide and 2 high"
    lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    for i in (DOOR, DOOR_PART, ROOF, SIGN):
        if f"jugcraft:{i}" not in t["values"]:
            t["values"].append(f"jugcraft:{i}")
    p.write_text(json.dumps(t, indent=2) + "\n")
    return models


if __name__ == "__main__":
    models = write_all()
    print(len(models), "models;", sum(len(m["elements"]) for m in models.values()), "elements")
