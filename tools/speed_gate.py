"""The speed gate (nuclear/SpeedGateBlock.java, SpeedGatePedestalBlock.java, client/SpeedGateRenderer.java): a sliding
glass security gate, in the Tri-Pillar's vestibule and at every Nuclear Center gatehouse.

Justin, October 8 2026: "the ones with the two doors that open ... it'll just open one way. So there'll be the through
lane, it'll open for you, and then the exit lane will open for you if you're exiting"; then "I want it to slide
inwards ... the sliding doors that like slide in and then slide back toward each other", with the Grand Central fare
gates as the example (a housing over each pedestal that takes the doors when they slide in).

Remade realistic on 22 October 2026 (Tri-Pillar work list item 4; claude/jugcraft-tripillar-speed-gates.md), the way
real sliding speed gates are built (Boon Edam Speedlane Slide, Automatic Systems SlimLane), and Justin asked for it at
the Nuclear Center too ("I like this one a lot better"): brushed stainless cabinets 1.04 high with rounded ends on a
recessed black kick; a glossy black glass top with a stainless rim, the card reader under it (its mark lit on the lane's
right-hand cabinet at the end you come up to) and LED guide lines along the lane's edges (white waiting, green open);
two bands of dark infrared sensor windows down each lane face; black brush seals at the door slots; pictograms on the
cabinets' ends (a green arrow on the right-hand cabinet where you come up, a red cross on the far end for whoever comes
the wrong way); over each cabinet at the door line a stainless-framed glass housing the doors slide into, so they show
parked in it. The doors are 10 mm tempered glass 1.55 high (too tall to jump) with polished edges, a frosted band of
dots at 1.40-1.50, a clear rubber safety edge and a status light along the top edge. No sign gantry: the cabinets sign
the lanes.

A player needs a lane a block wide between pedestals two blocks apart, so the cabinets are 0.66 wide (a real wide lane's
are about 0.51) and the passage 1.34: each door 10.56 px from its cabinet's face to (a hair short of) the lane's middle,
sliding the same into its cabinet. The doors run 0.6 px either side of the line, lanes side by side alternating
(SpeedGateBlock.slotSign), so two lanes' doors park in one housing.

The lane block is the end you come in by (facing the way through); the block ahead of it is left clear; a pedestal block
each side of both, shared with the lane beside. Lane parts are drawn facing north: the lane block is the south (approach)
block, the door line its north edge (z 0), the block ahead z -16..0, the pedestals' cabinets centred on x -8 and 24.
Pedestals are drawn lengthwise north-south (axis z), half b the south half (the line its north edge); half a is its
mirror image.
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
PARTS = ("lane", "ghost", "door_left", "door_right", "leds_wait", "leds_go", "gantry", "gantry_east",
         "light_left_wait", "light_left_go", "light_right_wait", "light_right_go")
# the cabinet (px, in its pedestal block: its middle x 8, the line z 0 for half b)
CAB_HW = 5.28           # half width (0.33 m)
CAB_HL = 14.4           # half length (0.9 m)
TOP_Y = 16.64           # the glass top's surface (1.04 m)
TOP_T = 0.19            # the glass top's thickness
KICK, KI = 0.8, 0.24    # the recessed kick
HOUSE_T = 1.6           # the housing's half depth over the line
HOUSE_Y = 27.84         # its top (1.74 m)
FRAME = 0.56            # its frame's width
# the doors (px, lane frame)
DOOR_W = 10.56          # each door, from its cabinet's face to a hair short of the lane's middle
SLOT = 0.6              # the doors run this far either side of the line
DOOR_Y0, DOOR_Y1 = 1.92, 26.72
DOOR_T = 0.16           # 10 mm
FACE_W, FACE_E = 8 - 16 + CAB_HW, 24 - CAB_HW    # the cabinets' faces either side of a lane: -2.72, 18.72
TEXTURES = ["sg_steel", "sg_trim", "sg_top", "sg_kick", "sg_ir", "sg_slot", "sg_house_glass", "sg_glass", "sg_glass_edge",
            "sg_rubber", "sg_led_white", "sg_led_green", "sg_reader", "sg_picto_go", "sg_picto_stop", "sg_picto_case"]
ALL = ("north", "south", "east", "west", "up", "down")
GLOW = 15


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
    """The south half of a cabinet lengthwise north-south, the door line its north edge (z 0): the kick, the stainless
    body with a rounded end (two steps in), the glass top with its rim, the sensor windows and the door slot's seals on
    both long faces, and over the line the housing's half: two posts, a top bar, a glass pane in its south face."""
    x0, x1 = 8 - CAB_HW, 8 + CAB_HW
    top0 = TOP_Y - TOP_T
    end = ((x0, x1, 0, CAB_HL - 0.96), (x0 + 0.24, x1 - 0.24, CAB_HL - 0.96, CAB_HL - 0.32), (x0 + 0.8, x1 - 0.8, CAB_HL - 0.32, CAB_HL))
    els = [_box((x0 + KI, 0, 0), (x1 - KI, KICK, CAB_HL - KI), "sg_kick", faces=("east", "west", "south", "down"))]
    for i, (a, b, z0, z1) in enumerate(end):
        els.append(_box((a, KICK, z0), (b, top0, z1), "sg_steel", faces=("east", "west", "south", "down")))
        # the glass top over it (the housing takes the top over the line)
        els.append(_box((a, top0, max(z0, HOUSE_T)), (b, TOP_Y, z1), {"*": "sg_trim", "up": "sg_top"}, faces=("east", "west", "south", "up")))
    # the rim: a stainless line round the top's edge
    els += [_box((x0, TOP_Y, HOUSE_T), (x0 + 0.12, TOP_Y + 0.03, CAB_HL - 0.96), "sg_trim", faces=("up", "west", "east")),
            _box((x1 - 0.12, TOP_Y, HOUSE_T), (x1, TOP_Y + 0.03, CAB_HL - 0.96), "sg_trim", faces=("up", "west", "east")),
            _box((x0 + 0.8, TOP_Y, CAB_HL - 0.12), (x1 - 0.8, TOP_Y + 0.03, CAB_HL), "sg_trim", faces=("up", "north", "south"))]
    # the infrared windows (two bands) and the door slot's brush seals, on both long faces
    for face, xa, xb in (("west", x0 - 0.04, x0), ("east", x1, x1 + 0.04)):
        for y0, y1 in ((4.16, 4.96), (11.84, 12.64)):
            els.append(_box((xa, y0, HOUSE_T + 0.9), (xb, y1, CAB_HL - 1.9), "sg_ir", faces=(face,)))
        els.append(_box((xa, KICK + 0.8, SLOT - 0.25), (xb, top0, SLOT + 0.25), "sg_slot", faces=(face,)))
    # under the housing, the dark slot the doors come up through
    els.append(_box((x0 + FRAME, top0, 0), (x1 - FRAME, TOP_Y - 0.02, HOUSE_T), "sg_kick", faces=("up",)))
    # the housing: two posts (a seal down each lane-side post at the slot), the top bar, the glass pane
    for xa, xb, face in ((x0, x0 + FRAME, "west"), (x1 - FRAME, x1, "east")):
        els.append(_box((xa, top0, 0), (xb, HOUSE_Y, HOUSE_T), "sg_trim", faces=("east", "west", "south", "up")))
        sx0, sx1 = (xa - 0.03, xa) if face == "west" else (xb, xb + 0.03)
        els.append(_box((sx0, TOP_Y, SLOT - 0.25), (sx1, DOOR_Y1 + 0.3, SLOT + 0.25), "sg_slot", faces=(face,)))
    els.append(_box((x0 + FRAME, HOUSE_Y - FRAME, 0), (x1 - FRAME, HOUSE_Y, HOUSE_T), "sg_trim", faces=("up", "down", "south")))
    els.append(_box((x0 + FRAME, TOP_Y, HOUSE_T - 0.16), (x1 - FRAME, HOUSE_Y - FRAME, HOUSE_T - 0.08), "sg_house_glass!", faces=("north", "south")))
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


def _flip_half_a(elements):
    """Half a is half b mirrored about the line; mirrored, its z is 16 - z of half b's in its own block: so half b's
    line edge z 0 becomes half a's z 16."""
    return _mirror_z(elements)


# ---------------------------------------------------------------- the lane's parts (facing north)

def door_elements(side):
    """A door, shut, on the line (the renderer sets it in its slot): tempered glass, its polished edges, the frosted band
    of dots (in the glass's texture), the clear rubber safety edge where the two meet."""
    if side == "left":
        g0, g1, r0, r1 = FACE_W, FACE_W + DOOR_W - 0.16, FACE_W + DOOR_W - 0.16, FACE_W + DOOR_W
        outer = "west"
    else:
        g0, g1, r0, r1 = FACE_E - DOOR_W + 0.16, FACE_E, FACE_E - DOOR_W, FACE_E - DOOR_W + 0.16
        outer = "east"
    z0, z1 = -DOOR_T / 2, DOOR_T / 2
    glass = {"north": "sg_glass!", "south": "sg_glass!", "up": "sg_glass_edge", "down": "sg_glass_edge", outer: "sg_glass_edge"}
    return [
        _box((g0, DOOR_Y0, z0), (g1, DOOR_Y1, z1), glass, faces=("north", "south", "up", "down", outer)),
        _box((r0, DOOR_Y0, z0 - 0.08), (r1, DOOR_Y1, z1 + 0.08), "sg_rubber"),
    ]


def door_light_elements(side, colour):
    """The status light along a door's top edge (moves with the door)."""
    g0, g1 = (FACE_W, FACE_W + DOOR_W - 0.16) if side == "left" else (FACE_E - DOOR_W + 0.16, FACE_E)
    return [_box((g0, DOOR_Y1, -0.07), (g1, DOOR_Y1 + 0.16, 0.07), f"sg_led_{colour}", GLOW, faces=("north", "south", "east", "west", "up"))]


def led_elements(colour):
    """What the lane lights on its cabinets: the guide lines along the lane's edges of both tops (white waiting, green
    open), the reader's mark under the right-hand (east) cabinet's top at the end you come up to (south), a green arrow
    on that end of it, and on the far (north) end of the left-hand (west) cabinet a red cross for whoever comes the
    wrong way."""
    tex = f"sg_led_{colour}"
    els = []
    for xa, xb in ((FACE_W - 0.48, FACE_W - 0.28), (FACE_E + 0.28, FACE_E + 0.48)):
        for za, zb in ((-CAB_HL + 1.6, -HOUSE_T - 0.5), (HOUSE_T + 0.5, CAB_HL - 1.6)):
            els.append(_box((xa, TOP_Y, za), (xb, TOP_Y + 0.06, zb), tex, GLOW, faces=("up",)))
    # the reader's mark (0.18 m square) under the east cabinet's top, toward the lane, near the south end
    rx, rz = 24 - 2.4, CAB_HL - 3.5
    els.append(_box((rx - 1.44, TOP_Y, rz - 1.44), (rx + 1.44, TOP_Y + 0.04, rz + 1.44), {"up": "sg_reader!"}, 10, faces=("up",)))
    # the pictograms (0.20 m square, 0.70-0.90 high) on the cabinets' ends, in the half nearer the lane, on a dark case
    for (cx, z, face, tex_) in ((24 - 2.08, CAB_HL, "south", "sg_picto_go!"), (-8 + 2.08, -CAB_HL, "north", "sg_picto_stop!")):
        s = 1 if face == "south" else -1
        za, zb = sorted((z, z + s * 0.03))
        els.append(_box((cx - 1.8, 11.0, za), (cx + 1.8, 14.6, zb), {face: "sg_picto_case"}, faces=(face,)))
        za, zb = sorted((z, z + s * 0.06))
        els.append(_box((cx - 1.6, 11.2, za), (cx + 1.6, 14.4, zb), {face: tex_}, GLOW, faces=(face,)))
    return els


_NORMALS = {"north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1), "down": (1, -1), "up": (1, 1)}


def close_faces(elements):
    """Gives every box the faces it left out that nothing else in the model covers (as tools/art_check.py's O1 rule
    probes them: a 4 x 4 grid just outside the face), in the texture of its first face: a left-out face is only for
    one another box hides. Seams on the block's 0/16 planes and faces out past them are left alone."""
    boxes = [(i, e["from"], e["to"]) for i, e in enumerate(elements)]
    for i, f, t in boxes:
        e = elements[i]
        present = set(e["faces"])
        if not present or len(present) == 6 or any(t[k] - f[k] <= 1e-6 for k in range(3)):
            continue
        tex = next(iter(e["faces"].values()))["texture"]
        for face, (axis, sign) in _NORMALS.items():
            if face in present:
                continue
            plane = t[axis] if sign > 0 else f[axis]
            if abs(plane) < 1e-6 or abs(plane - 16) < 1e-6 or (plane < 0 if sign < 0 else plane > 16):
                continue
            others = [k for k in range(3) if k != axis]
            free = False
            for a in range(4):
                for b in range(4):
                    q = [0.0, 0.0, 0.0]
                    q[axis] = plane + sign * 0.01
                    q[others[0]] = f[others[0]] + (a + 0.5) / 4 * (t[others[0]] - f[others[0]])
                    q[others[1]] = f[others[1]] + (b + 0.5) / 4 * (t[others[1]] - f[others[1]])
                    if not any(all(F[k] - 1e-6 <= q[k] <= T[k] + 1e-6 for k in range(3)) for j, F, T in boxes if j != i):
                        free = True
            if free:
                e["faces"][face] = {"texture": tex}
    return elements


def textures(rid):
    tex = {name: rid(f"block/{name}") for name in TEXTURES}
    for name in ("sg_glass", "sg_house_glass"):
        tex[name] = {"sprite": rid(f"block/{name}"), "force_translucent": True}
    tex["particle"] = rid("block/sg_steel")
    return tex


def models(rid):
    shut = door_elements("left") + door_elements("right")
    parts = {
        "lane": [],
        # the placement preview: the lane's doors shut and its lights waiting (its new pedestals have their own)
        "ghost": shut + led_elements("white") + door_light_elements("left", "white") + door_light_elements("right", "white"),
        "door_left": door_elements("left"),
        "door_right": door_elements("right"),
        "leds_wait": led_elements("white"),
        "leds_go": led_elements("green"),
        "gantry": [],
        "gantry_east": [],
        "light_left_wait": door_light_elements("left", "white"),
        "light_left_go": door_light_elements("left", "green"),
        "light_right_wait": door_light_elements("right", "white"),
        "light_right_go": door_light_elements("right", "green"),
    }
    out = {f"{GATE}_{part}": {"textures": textures(rid), "elements": close_faces(els)} for part, els in parts.items()}
    south = close_faces(pedestal_elements())
    out[f"{PEDESTAL}_b"] = {"parent": "minecraft:block/block", "textures": textures(rid), "elements": south}
    out[f"{PEDESTAL}_a"] = {"parent": "minecraft:block/block", "textures": textures(rid), "elements": _mirror_z(south)}
    out[f"{GATE}_item"] = {"parent": "minecraft:block/block", "textures": textures(rid),
                           "elements": close_faces([_shift(e, 0, 0, -6) for e in pedestal_elements()])}
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

def _steel():
    """The cabinets' sides: brushed stainless, the grain upright."""
    return M.stylize(M.brushed((180, 184, 190), 7401, "v", dirt=0.03), 16)


def _trim():
    return M.stylize(M.brushed((198, 202, 208), 7402, "h", dirt=0.02), 12)


def _top():
    """The glossy black glass top: near black, a soft reflected sheen across it."""
    a = np.zeros((64, 64, 4), np.float32)
    y = np.arange(64)[:, None] / 64
    x = np.arange(64)[None, :] / 64
    sheen = np.exp(-((x + y * 0.4 - 0.6) / 0.18) ** 2) * 16
    for c in range(3):
        a[..., c] = 9 + sheen
    a[..., 3] = 255
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _solid(rgb, alpha=255, size=16):
    return Image.new("RGBA", (size, size), tuple(rgb) + (alpha,))


def _ir():
    img = _solid((22, 12, 14), size=64)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 28, 63, 34], fill=(34, 20, 22, 255))
    return img


def _slot():
    """A slot's brush seal: black bristles."""
    img = Image.new("RGBA", (64, 64), (16, 16, 18, 255))
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 3):
        d.line([(0, y), (63, y)], fill=(34, 34, 38, 255))
    return img


def _door_glass():
    """Clear tempered glass, a faint green tint; the frosted band of dots 1.40-1.50 above the floor (v 0.11-0.175 of the
    door from its top)."""
    w, h = 64, 64
    img = Image.new("RGBA", (w, h), (176, 214, 206, 64))
    d = ImageDraw.Draw(img)
    b0, b1 = int(h * 0.11), int(h * 0.175)
    d.rectangle([0, b0, w - 1, b0], fill=(240, 244, 244, 120))
    d.rectangle([0, b1, w - 1, b1], fill=(240, 244, 244, 120))
    for x in range(2, w, 8):
        d.rectangle([x, b0 + 1, x + 2, b1 - 1], fill=(240, 244, 244, 190))
    return img


def _reader():
    """The reader's mark printed on the back of the glass top: a rounded square and the contactless waves, lit."""
    img = Image.new("RGBA", (64, 64), (9, 9, 10, 255))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((5, 5, 58, 58), 8, outline=(220, 226, 230, 255), width=2)
    for r in (9, 16, 23):
        d.arc((20 - r, 32 - r, 20 + r, 32 + r), -45, 45, fill=(220, 226, 230, 255), width=3)
    d.ellipse((16, 28, 24, 36), fill=(220, 226, 230, 255))
    return img


def _picto(kind):
    img = Image.new("RGBA", (64, 64), (6, 6, 7, 255))
    d = ImageDraw.Draw(img)
    if kind == "go":
        d.polygon([(32, 8), (54, 32), (41, 32), (41, 56), (23, 56), (23, 32), (10, 32)], fill=(80, 255, 120, 255))
    else:
        d.line([(14, 14), (50, 50)], fill=(255, 64, 56, 255), width=11)
        d.line([(50, 14), (14, 50)], fill=(255, 64, 56, 255), width=11)
    return img


def draw_textures():
    return {"sg_steel": _steel(), "sg_trim": _trim(), "sg_top": _top(), "sg_kick": _solid((14, 14, 16)), "sg_ir": _ir(),
            "sg_slot": _slot(), "sg_house_glass": _solid((214, 232, 230), 30), "sg_glass": _door_glass(),
            "sg_glass_edge": _solid((96, 160, 140)), "sg_rubber": _solid((176, 184, 184)),
            "sg_led_white": _solid((246, 250, 255)), "sg_led_green": _solid((90, 255, 130)), "sg_reader": _reader(),
            "sg_picto_go": _picto("go"), "sg_picto_stop": _picto("stop"), "sg_picto_case": _solid((10, 10, 12))}


def write_assets(write, rid, assets, lang):
    lang[f"block.{MOD}.{GATE}"] = DISPLAY
    lang[f"block.{MOD}.{PEDESTAL}"] = "Speed Gate Pedestal"
    for name, model in models(rid).items():
        write(assets / "models" / "block" / f"{name}.json", model)
    for name, bs in blockstates(rid).items():
        write(assets / "blockstates" / f"{name}.json", bs)
    write(assets / "items" / f"{GATE}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{GATE}_item")}})
