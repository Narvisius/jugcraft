"""Shielded storage for radioactive material (round 14): the lead pig, the lead-lined locker, the shielded waste
drum, the lead-lined door and lead glass. Writes their textures and block models (tools/generate_textures.py and
tools/generate_material_data.py call write_textures / write_models).

Everything is a real 3D model in model pixels (16 a block), front to the south for a block facing north:
- lead pig: a squat stainless-clad lead flask with a lead lid and a wire bail, round (16-sided);
- lead-lined locker: a double-door steel cabinet whose walls show their lead lining when it stands open;
- shielded waste drum: a 208-litre steel drum (concrete-lined inside), rolling hoops, a bolt-ring lid;
- lead-lined door: a two-high steel door with heavy hinges, lever handles and a door closer;
- lead glass: amber-tinted shielding glass.
Round parts are 16-sided: eight boxes turned 22.5 degrees apart (their ends are the sides), with one flat quad on top
whose texture is cut to the same 16-sided outline, so no two faces ever lie in the same plane (no z-fighting).
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import nuclear_materials as M

MOD = "jugcraft"
FONT_DIR = "/usr/share/fonts/truetype/dejavu/"
YELLOW = (240, 196, 34)
INK = (22, 22, 24)
SIDES = 16


def font(size, bold=True, condensed=False):
    name = "DejaVuSans" + ("Condensed" if condensed else "") + ("-Bold" if bold else "") + ".ttf"
    return ImageFont.truetype(FONT_DIR + name, size)


def trefoil(d, cx, cy, r, fg=INK, bg=YELLOW, ring=False):
    """The radiation trefoil (ISO 361: one blade pointing down, two up), radius r."""
    if ring:
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=bg)
    r0, r1 = r * 0.2, r * 0.8
    for k in range(3):
        a = 90 + k * 120
        d.pieslice([cx - r1, cy - r1, cx + r1, cy + r1], a - 30, a + 30, fill=fg)
    d.ellipse([cx - r0 * 1.5, cy - r0 * 1.5, cx + r0 * 1.5, cy + r0 * 1.5], fill=bg)
    d.ellipse([cx - r0, cy - r0, cx + r0, cy + r0], fill=fg)


# ------------------------------------------------------------------------------------------------ materials

def _np(img):
    return np.asarray(img.convert("RGBA")).astype(np.float32)


def _noise(size, seed, amount, scale=32):
    return M.fbm(size, seed, 4, scale) * amount


def brushed(colour, size, seed, direction="h", amount=10):
    a = M.base(colour, size)
    M.shade(a, _noise(size, seed, 12, size // 4))
    M.shade(a, M.streaks(size, seed + 1, direction=direction, count=size * 2, length=(size // 6, size // 2), amount=amount))
    M.shade(a, M.grain(size, seed + 2, 3))
    return a


def paint(colour, size, seed, dirt=0.4):
    a = M.base(colour, size)
    M.shade(a, _noise(size, seed, 10, size // 2))
    M.shade(a, M.grain(size, seed + 1, 2.5))
    M.shade(a, M.drips(size, seed + 2, count=4, amount=10 * dirt))
    return a


def lead_metal(size, seed):
    """Cast lead: dull blue-grey, mottled, with a little white oxide bloom."""
    a = M.base((104, 108, 116), size)
    M.shade(a, _noise(size, seed, 16, size // 3))
    M.shade(a, M.grain(size, seed + 1, 4))
    bloom = np.clip(M.fbm(size, seed + 2, 3, size // 4) * 3 - 0.6, 0, 1)
    M.tint(a, bloom, (170, 174, 176), 0.35)
    return a


def img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def stencil(d, xy, text, size, fill=INK, anchor="mm", bold=True, condensed=True):
    d.text(xy, text, font=font(size, bold, condensed), fill=fill, anchor=anchor)


def hazard_stripes(d, box, width=10, fg=INK, bg=YELLOW):
    x0, y0, x1, y1 = box
    d.rectangle(box, fill=bg)
    h = y1 - y0
    for x in range(int(x0) - h, int(x1) + h, width * 2):
        d.polygon([(x, y1), (x + width, y1), (x + width + h, y0), (x + h, y0)], fill=fg)
    # Clip what ran past the ends.


def polygon_mask(size, sides=SIDES, scale=1.0):
    """A 16-sided outline filling a square (flats at 0, 22.5, ... degrees), as an alpha mask."""
    m = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(m)
    c = size * 2
    R = size * 2 * scale
    pts = [(c + R * math.cos(math.radians(180 / sides + k * 360 / sides)), c + R * math.sin(math.radians(180 / sides + k * 360 / sides)))
           for k in range(sides)]
    d.polygon(pts, fill=255)
    return m.resize((size, size), Image.LANCZOS)


# ------------------------------------------------------------------------------------------------ textures

def wrap_labels(strip, draw, copies=2):
    """Draws round a cylinder's wrap strip: draw(d, cx) is called with cx the middle of the front face (and then
    the back), on a double-width canvas folded back so labels may run across the strip's seam."""
    S, h = strip.size
    canvas = Image.new("RGBA", (2 * S, h))
    canvas.paste(strip, (0, 0))
    canvas.paste(strip, (S, 0))
    d = ImageDraw.Draw(canvas)
    front = S + S / SIDES / 2
    for k in range(copies):
        c = front + k * S / copies
        draw(d, c if c < 3 * S / 2 else c - S)
    out = Image.new("RGBA", (S, h))
    out.paste(canvas.crop((S, 0, S + S // 2, h)), (0, 0))
    out.paste(canvas.crop((S // 2, 0, S, h)), (S // 2, 0))
    return out


def lead_pig_texture():
    """256 px: the body's wrap (v 0..3.77), the lid's side (v 4..4.66), the base ring (v 5..5.33), steel for the
    bail (u 0..8, v 8..16) and the lid's top (u 8..16, v 8..16)."""
    S = 256
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    # Body: stainless cladding, brushed round, a yellow label at the front and the back, a rolled bead top and bottom.
    body_h = int(round(3.77 * 16))
    body = img(brushed((176, 180, 184), S, 31, "h", 8)).crop((0, 0, S, body_h))
    d = ImageDraw.Draw(body)
    for y in (2, body_h - 3):
        d.line([(0, y), (S, y)], fill=(120, 124, 130, 255), width=2)
        d.line([(0, y + 2), (S, y + 2)], fill=(214, 218, 222, 255), width=1)
    y0, y1 = int(body_h * 0.24), int(body_h * 0.80)

    def label(d, cx):
        d.rectangle([cx - 38, y0, cx + 38, y1], fill=YELLOW, outline=INK, width=2)
        trefoil(d, cx - 24, (y0 + y1) / 2, (y1 - y0) * 0.34)
        stencil(d, (cx + 10, y0 + (y1 - y0) * 0.36), "RADIOACTIVE", 8)
        stencil(d, (cx + 10, y0 + (y1 - y0) * 0.74), "Pb 50 mm", 8, bold=False)
    out.paste(wrap_labels(body, label), (0, 0))
    # Lid side: painted lead, knurled.
    lid = img(lead_metal(S, 32)).crop((0, 0, S, 11))
    d = ImageDraw.Draw(lid)
    for x in range(0, S, 4):
        d.line([(x, 2), (x, 9)], fill=(70, 74, 80, 255), width=1)
    out.paste(lid, (0, 64))
    base = img(brushed((120, 124, 130), S, 33, "h")).crop((0, 0, S, 6))
    out.paste(base, (0, 80))
    steel = img(brushed((188, 192, 196), 128, 34, "v"))
    out.paste(steel, (0, 128))
    # Lid top: lead with a turned ring and an embossed trefoil.
    top = img(lead_metal(128, 35))
    d = ImageDraw.Draw(top)
    d.ellipse([10, 10, 118, 118], outline=(78, 82, 90, 255), width=3)
    d.ellipse([14, 14, 114, 114], outline=(150, 154, 160, 255), width=1)
    d.ellipse([44, 44, 84, 84], fill=(92, 96, 104, 255))
    trefoil(d, 64, 64, 17, fg=(56, 60, 66, 255), bg=(92, 96, 104, 255))
    stencil(d, (64, 100), "LEAD", 11, fill=(70, 74, 80, 255))
    top.putalpha(polygon_mask(128))
    out.paste(top, (128, 128))
    return out


def waste_drum_texture():
    """256 px: body wrap (v 0..6.58: top to bottom of the drum between its chimes), hoop (v 7..7.375), closure ring
    (v 7.5..7.875), steel (u 0..4, v 8..16), a bung's top (u 4..8, v 8..12) and the lid (u 8..16, v 8..16)."""
    S = 256
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    h = int(round(6.58 * 16))
    body = img(paint((214, 178, 38), S, 41, dirt=0.8)).crop((0, 0, S, h))
    # Where the parts of the drum fall on the strip (it runs from y 14.7 at the top to 0.6 at the bottom).
    row = lambda y: (14.7 - y) / 14.1 * h

    def labels(d, cx):
        # Upper third: the stencilled warning; between the hoops: the trefoil label; lower third: the drum's number.
        stencil(d, (cx, row(13.4)), "RADIOACTIVE", 12)
        stencil(d, (cx, row(11.6)), "WASTE", 14)
        cy, r = row(7.5), 16
        d.polygon([(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)], fill=(250, 250, 244, 255), outline=INK)
        d.polygon([(cx, cy - r + 3), (cx + r - 3, cy), (cx, cy + r - 3), (cx - r + 3, cy)], fill=YELLOW)
        trefoil(d, cx, cy - 2, 6.5)
        stencil(d, (cx, cy + 8), "7", 6)
        stencil(d, (cx, row(3.6)), "DRUM 2041", 10)
        stencil(d, (cx, row(2.2)), "LLW  CONCRETE LINED", 7, bold=False)
    out.paste(wrap_labels(body, labels), (0, 0))
    hoop = img(paint((198, 162, 30), S, 42, dirt=1.0)).crop((0, 0, S, 6))
    ImageDraw.Draw(hoop).line([(0, 1), (S, 1)], fill=(232, 204, 90, 255), width=1)
    out.paste(hoop, (0, 112))
    ring = img(brushed((150, 154, 160), S, 43, "h")).crop((0, 0, S, 6))
    ImageDraw.Draw(ring).line([(0, 1), (S, 1)], fill=(206, 210, 214, 255), width=1)
    out.paste(ring, (0, 120))
    chime = img(paint((190, 156, 28), S, 46, dirt=1.2)).crop((0, 0, S, 6))
    out.paste(chime, (0, 104))
    steel = img(brushed((150, 154, 160), 64, 44, "v")).resize((64, 128))
    out.paste(steel, (0, 128))
    bung = img(paint((190, 156, 28), 64, 47, dirt=0.4))
    d = ImageDraw.Draw(bung)
    d.ellipse([4, 4, 60, 60], outline=(130, 104, 20, 255), width=5)
    d.rectangle([12, 28, 52, 36], fill=(140, 112, 22, 255))
    bung.putalpha(polygon_mask(64))
    out.paste(bung, (64, 128))
    # Lid: the rolled chime round the edge, the lid pressed in, two bung holes and stencilled lettering.
    top = img(paint((206, 170, 34), 128, 45, dirt=0.6))
    d = ImageDraw.Draw(top)
    d.ellipse([3, 3, 125, 125], outline=(160, 128, 22, 255), width=6)
    d.ellipse([9, 9, 119, 119], outline=(236, 210, 96, 255), width=2)
    d.ellipse([14, 14, 114, 114], outline=(176, 142, 26, 255), width=2)
    stencil(d, (64, 66), "RAD WASTE", 12)
    trefoil(d, 64, 90, 9)
    top.putalpha(polygon_mask(128))
    out.paste(top, (128, 128))
    return out


def locker_front():
    """256 px: the front of the locker, 16 px a model pixel (x 0..16 across, y 16..0 down): two doors with the
    radiation label, the lead-lining plate and the plinth's hazard stripe."""
    S = 256
    a = paint((206, 208, 202), S, 51, dirt=0.3)
    im = img(a)
    d = ImageDraw.Draw(im)
    U = 16
    # Door outlines (they are separate boxes; the drawing keeps the edges darker and a pressed panel line).
    for x0, x1 in ((1.3, 7.95), (8.05, 14.7)):
        X0, X1 = int(x0 * U), int(x1 * U)
        Y0, Y1 = int((16 - 14.8) * U), int((16 - 1.4) * U)
        d.rectangle([X0 + 10, Y0 + 10, X1 - 10, Y1 - 10], outline=(180, 182, 176, 255), width=2)
        d.rectangle([X0 + 12, Y0 + 12, X1 - 12, Y1 - 12], outline=(226, 228, 222, 255), width=1)
    # The label on the left door.
    lx0, lx1 = int(2.3 * U), int(6.95 * U)
    ly0, ly1 = int((16 - 12.6) * U), int((16 - 7.4) * U)
    d.rectangle([lx0, ly0, lx1, ly1], fill=YELLOW, outline=INK, width=3)
    trefoil(d, (lx0 + lx1) / 2, ly0 + 26, 19)
    stencil(d, ((lx0 + lx1) / 2, ly0 + 52), "CAUTION", 13)
    stencil(d, ((lx0 + lx1) / 2, ly0 + 66), "RADIOACTIVE", 10)
    stencil(d, ((lx0 + lx1) / 2, ly0 + 77), "MATERIAL", 10)
    # The lining plate on the right door.
    px0, px1 = int(10.2 * U), int(13.6 * U)
    py0, py1 = int((16 - 12.2) * U), int((16 - 11.0) * U)
    d.rectangle([px0, py0, px1, py1], fill=(222, 224, 226, 255), outline=(96, 98, 102, 255), width=1)
    stencil(d, ((px0 + px1) / 2, (py0 + py1) / 2 - 4), "LEAD LINED", 8)
    stencil(d, ((px0 + px1) / 2, (py0 + py1) / 2 + 5), "Pb 25 mm", 7, bold=False)
    # Hazard stripe on the plinth (y 0..1 at the front).
    hazard_stripes(d, (0, int(15 * U), S, S), width=12)
    return im


def locker_parts():
    """64 px tiles: painted side, lead lining, door edges (steel / lead / steel), plinth shadow."""
    return {
        "lead_locker_paint": img(paint((200, 202, 196), 64, 52, dirt=0.4)),
        "lead_locker_lead": img(lead_metal(64, 53)),
        "lead_locker_edge": _edge_tex(64, 54),
    }


def _edge_tex(size, seed):
    """A steel-skinned lead sandwich seen edge on: bands across (u), along the edge (v)."""
    a = brushed((180, 184, 188), size, seed, "v", 6)
    im = img(a)
    lead = img(lead_metal(size, seed + 1))
    # Lead core: the middle 60 % of the width.
    x0, x1 = int(size * 0.2), int(size * 0.8)
    im.paste(lead.crop((x0, 0, x1, size)), (x0, 0))
    d = ImageDraw.Draw(im)
    d.line([(x0, 0), (x0, size)], fill=(60, 62, 66, 255))
    d.line([(x1, 0), (x1, size)], fill=(60, 62, 66, 255))
    return im


def door_faces():
    """The lead-lined door's faces, 256 px a half (16 px a model pixel): outside (sign) and inside, bottom and top.
    Hinges and handles are 3D parts, so the faces are drawn symmetric and never mirrored."""
    S = 256
    out = {}
    for side in ("outside", "inside"):
        for half in ("bottom", "top"):
            a = paint((150, 156, 162), S, 61 + (side == "inside") * 2 + (half == "top"), dirt=0.25)
            im = img(a)
            d = ImageDraw.Draw(im)
            # The door's raised edge frame (a pressed steel door).
            if half == "bottom":
                d.line([(10, 0), (10, S - 10), (S - 10, S - 10), (S - 10, 0)], fill=(116, 122, 128, 255), width=3)
                d.line([(13, 0), (13, S - 13), (S - 13, S - 13), (S - 13, 0)], fill=(178, 184, 190, 255), width=1)
                # Stainless kick plate with a hazard band.
                kp = img(brushed((190, 194, 198), S, 65, "h", 6)).crop((0, 0, S, 52))
                im.paste(kp, (0, S - 52))
                d.line([(0, S - 52), (S, S - 52)], fill=(110, 114, 118, 255), width=2)
                hazard_stripes(d, (0, S - 50, S, S - 38), width=10)
            else:
                d.line([(10, S), (10, 10), (S - 10, 10), (S - 10, S)], fill=(116, 122, 128, 255), width=3)
                d.line([(13, S), (13, 13), (S - 13, 13), (S - 13, S)], fill=(178, 184, 190, 255), width=1)
                if side == "outside":
                    # The warning sign at eye level (on the steel, never on glass).
                    x0, x1, y0, y1 = 56, 200, 34, 166
                    d.rectangle([x0, y0, x1, y1], fill=YELLOW, outline=INK, width=4)
                    trefoil(d, 128, 84, 40)
                    stencil(d, (128, 138), "CAUTION", 18)
                    stencil(d, (128, 156), "RADIATION AREA", 12)
                    # The lead-lining plate under it.
                    d.rectangle([84, 186, 172, 210], fill=(214, 216, 218, 255), outline=(80, 82, 86, 255), width=1)
                    stencil(d, (128, 193), "LEAD LINED DOOR", 9)
                    stencil(d, (128, 204), "6 mm Pb", 8, bold=False)
                else:
                    d.rectangle([84, 120, 172, 144], fill=(214, 216, 218, 255), outline=(80, 82, 86, 255), width=1)
                    stencil(d, (128, 132), "KEEP CLOSED", 11)
            out[f"lead_door_{side}_{half}"] = im
    out["lead_door_edge"] = _edge_tex(64, 66)
    return out


def lead_glass():
    """Amber shielding glass: a light tint, faint pour streaks, a thin dark edge; mostly see-through (translucent)."""
    S = 64
    a = M.base((226, 196, 128), S)
    M.shade(a, _noise(S, 71, 12, 16))
    M.shade(a, M.streaks(S, 72, direction="v", count=40, length=(10, 40), amount=10))
    a[..., 3] = 70
    a[..., 3] += np.clip(_noise(S, 73, 30, 16), -15, 20)
    # Edges darker and more solid.
    for k, alpha in ((0, 210), (1, 150)):
        for sl in (np.s_[k, :], np.s_[S - 1 - k, :], np.s_[:, k], np.s_[:, S - 1 - k]):
            a[sl][..., :3] *= 0.7
            a[sl][..., 3] = alpha
    # A couple of glints.
    im = img(a)
    d = ImageDraw.Draw(im)
    d.line([(10, 50), (24, 36)], fill=(246, 228, 190, 150), width=2)
    d.line([(16, 54), (26, 44)], fill=(246, 228, 190, 120), width=1)
    return im


BEACON_COLOURS = {"amber": (255, 170, 30), "red": (232, 40, 32), "green": (60, 220, 90)}


def beacon_lens(colour):
    """A beacon's moulded lens, lit: bright in the middle, Fresnel ribs round it, a hot spot where the bulb is."""
    S = 64
    r, g, b = colour
    a = np.zeros((S, S, 4), np.float32)
    ys, xs = np.mgrid[0:S, 0:S].astype(np.float32)
    rib = 0.82 + 0.18 * np.cos(xs / S * 2 * math.pi * 16)            # 16 ribs round (u wraps once)
    glow = 0.78 + 0.22 * np.exp(-((ys - S * 0.55) / (S * 0.3)) ** 2)  # brightest at the bulb's height
    k = rib * glow
    a[..., 0] = np.clip(r * k + 60 * (k - 0.8), 0, 255)
    a[..., 1] = np.clip(g * k + 60 * (k - 0.8), 0, 255)
    a[..., 2] = np.clip(b * k + 60 * (k - 0.8), 0, 255)
    a[..., 3] = 255
    # The lens top (u, v 8..16 of the texture): a dome seen from above, ribbed in rings.
    cx = cy = S * 0.75
    rr = np.hypot(xs - cx, ys - cy) / (S * 0.25)
    top = (xs >= S // 2) & (ys >= S // 2)
    ring = 0.8 + 0.2 * np.cos(rr * math.pi * 5)
    for i, c in enumerate((r, g, b)):
        a[..., i][top] = np.clip(c * ring[top] * (1.08 - 0.3 * rr[top]) + 40, 0, 255)
    return img(a)


def textures():
    out = {"lead_pig": lead_pig_texture(), "waste_drum": waste_drum_texture(), "lead_locker_front": locker_front(),
           "lead_glass": lead_glass()}
    for name, c in BEACON_COLOURS.items():
        out[f"warning_beacon_{name}"] = beacon_lens(c)
    out.update(locker_parts())
    out.update(door_faces())
    return out


SHARP = {"lead_pig", "waste_drum", "lead_locker_front", "lead_door_outside_bottom", "lead_door_outside_top",
         "lead_door_inside_bottom", "lead_door_inside_top"}
HI_RES = {"lead_glass", "lead_locker_paint", "lead_locker_lead", "lead_locker_edge", "lead_door_edge", "warning_beacon_amber",
          "warning_beacon_red", "warning_beacon_green"}


# ------------------------------------------------------------------------------------------------ models

def box(f, t, faces, rotation=None, shade=True):
    """faces: {direction: {"texture": "#x", "uv": [...], "rotation": n}} (directions left out are not drawn)."""
    e = {"from": [round(v, 4) for v in f], "to": [round(v, 4) for v in t], "faces": faces}
    if rotation:
        e["rotation"] = rotation
    if not shade:
        e["shade"] = False
    return e


def all_faces(tex, skip=(), uv=None):
    return {d: ({"texture": tex, "uv": uv} if uv else {"texture": tex}) for d in ("north", "south", "east", "west", "up", "down") if d not in skip}


def cylinder(cx, cz, r, y0, y1, tex, v0, v1, top=None, bottom=False, u_span=16.0):
    """A 16-sided upright cylinder (apothem r): eight boxes 22.5 degrees apart, their ends the sides, each side its
    slice of a wrap texture (u 0..u_span round, v0..v1 down). top: (tex, (u0, v0, u1, v1)) for a cut-out lid."""
    s = 2 * r * math.tan(math.pi / SIDES)
    out = []
    # (long axis, angle): x-boxes at 0, +-22.5, +-45 and z-boxes at 0, +-22.5 give all eight directions.
    for axis, angle in (("x", 0), ("x", 22.5), ("x", 45), ("x", -45), ("x", -22.5), ("z", 0), ("z", 22.5), ("z", -22.5)):
        if axis == "x":
            f, t = (cx - r, y0, cz - s / 2), (cx + r, y1, cz + s / 2)
            ends = {"east": (1, 0), "west": (-1, 0)}
        else:
            f, t = (cx - s / 2, y0, cz - r), (cx + s / 2, y1, cz + r)
            ends = {"south": (0, 1), "north": (0, -1)}
        a = math.radians(angle)
        faces = {}
        for face, (nx, nz) in ends.items():
            # The game turns +x towards -z for a positive angle (x' = x cos + z sin, z' = -x sin + z cos).
            rx, rz = nx * math.cos(a) + nz * math.sin(a), -nx * math.sin(a) + nz * math.cos(a)
            psi = math.degrees(math.atan2(rz, rx))
            j = round(((90 - psi) % 360) / (360 / SIDES)) % SIDES
            w = u_span / SIDES
            faces[face] = {"texture": tex, "uv": [round(j * w, 4), v0, round((j + 1) * w, 4), v1]}
        rot = {"origin": [cx, y0, cz], "axis": "y", "angle": angle} if angle else None
        out.append(box(f, t, faces, rot))
    if top:
        R = r / math.cos(math.pi / SIDES)
        ttex, uv = top
        out.append(box((cx - R, y1, cz - R), (cx + R, y1, cz + R), {"up": {"texture": ttex, "uv": list(uv)}}))
    if bottom:
        R = r / math.cos(math.pi / SIDES)
        ttex, uv = bottom
        out.append(box((cx - R, y0, cz - R), (cx + R, y0, cz + R), {"down": {"texture": ttex, "uv": list(uv)}}))
    return out


def model(textures, elements, parent="minecraft:block/block", ao=True):
    m = {"parent": parent, "textures": textures, "elements": elements}
    if not ao:
        m["ambientocclusion"] = False
    return m


def rid(path):
    return f"{MOD}:{path}"


def lead_pig_model():
    t = "#pig"
    els = []
    # Base ring, body, lid.
    els += cylinder(8, 8, 5.3, 0, 0.7, t, 5, 5.33, top=(t, (8, 8, 16, 16)), bottom=(t, (8, 8, 16, 16)))
    els += cylinder(8, 8, 5.0, 0.7, 8.2, t, 0, 3.77)
    els += cylinder(8, 8, 5.35, 8.2, 9.6, t, 4, 4.66, top=(t, (8, 8, 16, 16)), bottom=(t, (8, 8, 16, 16)))
    steel = {"texture": t, "uv": [1, 9, 2, 10]}
    # The wire bail: lugs on the lid, two legs and the grip.
    for x in (3.9, 11.3):
        els.append(box((x, 9.6, 7.2), (x + 0.8, 10.2, 8.8), {d: steel for d in ("north", "south", "east", "west", "up")}))
        els.append(box((x + 0.15, 10.2, 7.65), (x + 0.65, 11.6, 8.15), {d: steel for d in ("north", "south", "east", "west")}))
    els.append(box((4.05, 11.6, 7.6), (11.95, 12.2, 8.2), {d: steel for d in ("north", "south", "east", "west", "up", "down")}))
    return model({"particle": rid("block/lead_pig"), "pig": rid("block/lead_pig")}, els)


def waste_drum_model():
    t = "#drum"
    lid = (t, (8, 8, 16, 16))
    els = []
    els += cylinder(8, 8, 5.7, 0, 0.6, t, 6.5, 6.75, top=lid, bottom=lid)                  # bottom chime
    els += cylinder(8, 8, 5.5, 0.6, 15.3, t, 0, 6.58)                                       # body
    for y in (4.7, 9.6):                                                                    # rolling hoops
        els += cylinder(8, 8, 5.82, y, y + 0.7, t, 7.0, 7.25, top=lid, bottom=lid)
    # The closure ring on the top chime, its top the lid: a full block high, so a drum stands flush on the one below.
    els += cylinder(8, 8, 5.92, 15.2, 16.0, t, 7.5, 7.75, top=lid, bottom=lid)
    # The closure ring's bolt lug at the front right.
    steel = {"texture": t, "uv": [1, 9, 2, 10]}
    rot = {"origin": [8, 15, 8], "axis": "y", "angle": -22.5}
    els.append(box((9.8, 15.15, 13.7), (12.0, 15.95, 14.5), {d: steel for d in ("north", "south", "east", "west", "up", "down")}, rot))
    els.append(box((11.9, 15.3, 13.85), (12.6, 15.8, 14.35), {d: steel for d in ("south", "east", "up", "down", "north")}, rot))
    return model({"particle": rid("block/waste_drum"), "drum": rid("block/waste_drum")}, els)


def locker_model(open_=False):
    """The lead-lined locker, facing north (front to the south). Walls 2 px thick (steel skin over lead) so an
    open locker shows its lining; closed, the doors hide the inside."""
    P, L, E, F = "#paint", "#lead", "#edge", "#front"
    els = []
    # Plinth, set back 0.5 px under the body.
    els.append(box((1.5, 0, 2.5), (14.5, 1, 14.1), {"south": {"texture": F, "uv": [1.5, 15, 14.5, 16]},
                                                    "east": {"texture": P}, "west": {"texture": P}, "north": {"texture": P}}))
    # Walls: left, right (full), bottom, top, back between them.
    edge_v = {"texture": E, "uv": [0, 0, 16, 16]}
    els.append(box((1, 1, 2), (3, 15.2, 14.6), {"west": {"texture": P}, "east": {"texture": L},
                                                "north": {"texture": P}, "south": {"texture": E, "uv": [0, 0, 16, 16]},
                                                "down": {"texture": P}}))
    els.append(box((13, 1, 2), (15, 15.2, 14.6), {"east": {"texture": P}, "west": {"texture": L},
                                                  "north": {"texture": P}, "south": {"texture": E, "uv": [0, 0, 16, 16]},
                                                  "down": {"texture": P}}))
    els.append(box((3, 1, 2), (13, 3, 14.6), {"up": {"texture": L}, "south": {"texture": E, "uv": [0, 0, 16, 16], "rotation": 90},
                                              "down": {"texture": P}, "north": {"texture": P}}))
    els.append(box((3, 13.2, 2), (13, 15.2, 14.6), {"north": {"texture": P}, "down": {"texture": L},
                                                    "south": {"texture": E, "uv": [0, 0, 16, 16], "rotation": 90}}))
    els.append(box((3, 3, 2), (13, 13.2, 4), {"north": {"texture": P}, "south": {"texture": L}}))
    # A shelf (lead-topped steel) half way up.
    els.append(box((3, 7.6, 4), (13, 8.2, 14.2), {"up": {"texture": L}, "down": {"texture": P}, "south": {"texture": E, "uv": [0, 0, 16, 16], "rotation": 90}}))
    # Top lip.
    els.append(box((0.8, 15.2, 1.8), (15.2, 15.6, 14.8), all_faces(P, uv=None)))
    # Doors: 1 px thick, proud of the body; hinged on the outer edges.
    doors = [((1.3, 1.4, 14.6), (7.95, 14.8, 15.6), (1.3, 15.6), -100), ((8.05, 1.4, 14.6), (14.7, 14.8, 15.6), (14.7, 15.6), 100)]
    for (f, t, hinge, swing) in doors:
        faces = {"south": {"texture": F, "uv": [f[0], 16 - t[1], t[0], 16 - f[1]]}, "north": {"texture": L},
                 "east": {"texture": E, "uv": [0, 0, 16, 16]}, "west": {"texture": E, "uv": [0, 0, 16, 16]},
                 "up": {"texture": E, "uv": [0, 0, 16, 16], "rotation": 90}, "down": {"texture": E, "uv": [0, 0, 16, 16], "rotation": 90}}
        rot = {"origin": [hinge[0], 8, hinge[1]], "axis": "y", "angle": swing} if open_ else None
        els.append(box(f, t, faces, rot))
        # Hinge knuckles on the outer edge.
        hx = 0.8 if hinge[0] < 8 else 14.7
        for y in (3.0, 11.0):
            els.append(box((hx, y, 15.0), (hx + 0.5, y + 2, 15.7), {d: {"texture": "#steel"} for d in ("north", "south", "east", "west", "up", "down")}))
    # The handle on the right door (bar handle with a lock), moving with the door.
    hr = {"origin": [14.7, 8, 15.6], "axis": "y", "angle": 100} if open_ else None
    steel = {d: {"texture": "#steel"} for d in ("south", "east", "west", "up", "down")}
    els.append(box((8.7, 6.4, 15.6), (9.3, 7.0, 16.2), steel, hr))
    els.append(box((8.7, 9.6, 15.6), (9.3, 10.2, 16.2), steel, hr))
    els.append(box((8.65, 6.2, 16.2), (9.35, 10.4, 16.6), {d: {"texture": "#steel"} for d in ("north", "south", "east", "west", "up", "down")}, hr))
    els.append(box((10.2, 7.9, 15.6), (10.9, 8.6, 15.9), {d: {"texture": "#steel"} for d in ("south", "east", "west", "up", "down")}, hr))
    return model({"particle": rid("block/lead_locker_paint"), "paint": rid("block/lead_locker_paint"), "lead": rid("block/lead_locker_lead"),
                  "edge": rid("block/lead_locker_edge"), "front": rid("block/lead_locker_front"), "steel": rid("block/nu_steel")}, els)


def door_model(half, hinge, open_):
    """The lead-lined door, a vanilla door's shape (3 px slab at the west edge, facing east) with 3D hinges, handles
    and (top) a door closer. hinge 'left' has its hinges at z 0; 'right' and the open models are mirrored in z."""
    O, I, E = "#outside", "#inside", "#edge"
    els = []
    slab = {"west": {"texture": O, "uv": [0, 0, 16, 16]}, "east": {"texture": I, "uv": [0, 0, 16, 16]},
            "north": {"texture": E, "uv": [0, 0, 16, 16]}, "south": {"texture": E, "uv": [0, 0, 16, 16]}}
    if half == "bottom":
        slab["down"] = {"texture": E, "uv": [0, 0, 16, 16], "rotation": 90, "cullface": "down"}
    else:
        slab["up"] = {"texture": E, "uv": [0, 0, 16, 16], "rotation": 90, "cullface": "up"}
    els.append(box((0, 0, 0), (3, 16, 16), slab))
    S = "#steel"
    st = lambda skip=(): {d: {"texture": S} for d in ("north", "south", "east", "west", "up", "down") if d not in skip}
    # Heavy hinges: knuckles standing proud of both faces at the hinge edge (z 0), two a half.
    for y in ((2.5, 12.0) if half == "bottom" else (10.5,)):
        els.append(box((-0.5, y, -0.4), (3.5, y + 2.4, 0.6), st()))
    if half == "bottom":
        # Lever handles on both faces near the free edge (z 16), at about a metre: rose, spindle, lever (pointing
        # back towards the hinge). Each part's faces sit inside its neighbour, never in a shared plane.
        for out in (True, False):
            sx = -1 if out else 1
            x0 = 0 if out else 3
            rose = sorted((x0, x0 + 0.4 * sx))
            spin = sorted((x0 + 0.4 * sx, x0 + 1.0 * sx))
            lever = sorted((x0 + 0.8 * sx, x0 + 1.4 * sx))
            near = "east" if out else "west"
            els.append(box((rose[0], 12.0, 12.6), (rose[1], 15.0, 14.0), st((near,))))
            els.append(box((spin[0], 13.65, 13.05), (spin[1], 14.15, 13.55), st((near,))))
            els.append(box((lever[0], 13.5, 9.8), (lever[1], 14.3, 13.8), st()))
    else:
        # The door closer on the inside face near the hinge, and its arm.
        els.append(box((3, 12.4, 1.4), (4.4, 14.0, 6.4), st(("west",))))
        els.append(box((3.2, 14.0, 4.6), (3.8, 14.4, 9.6), st()))
    m = {"parent": "minecraft:block/block", "ambientocclusion": False,
         "textures": {"particle": rid(f"block/lead_door_outside_{half}"), "outside": rid(f"block/lead_door_outside_{half}"),
                      "inside": rid(f"block/lead_door_inside_{half}"), "edge": rid("block/lead_door_edge"), "steel": rid("block/nu_steel")},
         "elements": els}
    if (hinge == "right") != open_:
        m["elements"] = [mirror_z(e) for e in els]
    return m


def mirror_z(e):
    """An element mirrored in z (16 - z); faces keep their own (unmirrored) texture so lettering still reads."""
    e = json_copy(e)
    f, t = e["from"], e["to"]
    e["from"], e["to"] = [f[0], f[1], round(16 - t[2], 4)], [t[0], t[1], round(16 - f[2], 4)]
    faces = e["faces"]
    if "north" in faces or "south" in faces:
        n, s = faces.pop("north", None), faces.pop("south", None)
        if n:
            faces["south"] = n
        if s:
            faces["north"] = s
    if "rotation" in e:
        r = e["rotation"]
        r["origin"][2] = 16 - r["origin"][2]
        if r["axis"] in ("y", "x"):
            r["angle"] = -r["angle"]
    return e


def json_copy(x):
    import json
    return json.loads(json.dumps(x))


def beacon_model(colour):
    """A wall-mounted warning beacon (round 14, replacing the cube warning light in the plants): a steel bracket off
    the wall, a round steel base, a ribbed lens that glows, a steel cap and a wire guard over the lens. Facing north:
    the wall is to the south (z 16), the beacon stands 6.5 px out from it."""
    S, L = "#steel", "#lens"
    st = lambda skip=(): {d: {"texture": S} for d in ("north", "south", "east", "west", "up", "down") if d not in skip}
    cz = 9.5
    els = []
    els.append(box((5.5, 2.5, 15.0), (10.5, 8.5, 16.0), dict(st(("south",)), south={"texture": S, "cullface": "south"})))  # wall plate
    els.append(box((7.0, 3.0, 11.0), (9.0, 4.0, 15.0), st(("south",))))                                              # arm
    els += cylinder(8, cz, 2.8, 4.0, 5.4, S, 0, 2, top=(S, (0, 0, 16, 16)), bottom=(S, (0, 0, 16, 16)))              # base
    lens = cylinder(8, cz, 2.3, 5.4, 9.4, L, 0, 8, top=(L, (8, 8, 16, 16)))                                          # lens
    for e in lens:
        e["light_emission"] = 15
        e["shade"] = False
    els += lens
    els += cylinder(8, cz, 1.1, 9.4, 9.9, S, 0, 2, top=(S, (0, 0, 16, 16)))                                          # cap
    # The wire guard: four rods round the lens and a cross over it (the cross's two bars at different heights).
    for dx, dz in ((3.0, 0), (-3.0, 0), (0, 3.0), (0, -3.0)):
        top_y = 10.25 if dz == 0 else 10.2
        els.append(box((8 + dx - 0.25, 5.4, cz + dz - 0.25), (8 + dx + 0.25, top_y, cz + dz + 0.25), st(("down",) if False else ())))
    els.append(box((4.75, 10.25, cz - 0.25), (11.25, 10.75, cz + 0.25), st()))
    els.append(box((7.75, 10.2, cz - 3.25), (8.25, 10.7, cz + 3.25), st()))
    return model({"particle": rid(f"block/warning_beacon_{colour}"), "steel": rid("block/nu_steel"), "lens": rid(f"block/warning_beacon_{colour}")},
                 els, ao=False)


def models():
    out = {"lead_pig": lead_pig_model(), "waste_drum": waste_drum_model(),
           "lead_locker": locker_model(False), "lead_locker_open": locker_model(True),
           "lead_glass": {"parent": "minecraft:block/cube_all",
                          "textures": {"all": {"sprite": rid("block/lead_glass"), "force_translucent": True}}}}
    for half in ("bottom", "top"):
        for hinge in ("left", "right"):
            for open_ in (False, True):
                out[f"lead_door_{half}_{hinge}{'_open' if open_ else ''}"] = door_model(half, hinge, open_)
    for colour in BEACON_COLOURS:
        out[f"warning_beacon_{colour}"] = beacon_model(colour)
    return out


# ------------------------------------------------------------------------------------------------ game data

BLOCKS = ["lead_pig", "lead_locker", "waste_drum", "lead_door", "lead_glass", "warning_beacon"]
NAMES = {"lead_pig": "Lead Pig", "lead_locker": "Lead-Lined Locker", "waste_drum": "Shielded Waste Drum",
         "lead_door": "Lead-Lined Door", "lead_glass": "Lead Glass", "warning_beacon": "Warning Beacon"}
LANG = {
    "container.jugcraft.lead_pig": "Lead Pig",
    "container.jugcraft.lead_locker": "Lead-Lined Locker",
    "container.jugcraft.waste_drum": "Shielded Waste Drum",
    "message.jugcraft.airlock_interlock": "Interlocked: the airlock's other door is open",
}
CRAFTING = {
    "lead_pig": ([" S ", "L L", "LLL"], {"S": "#c:plates/steel", "L": "#c:ingots/lead"}, 1),
    "lead_locker": (["PLP", "P P", "PLP"], {"P": "#c:plates/steel", "L": "#c:plates/lead"}, 1),
    "waste_drum": (["PCP", "P P", "PCP"], {"P": "#c:plates/steel", "C": "minecraft:light_gray_concrete"}, 1),
    "lead_door": (["PL", "PL", "PL"], {"P": "#c:plates/steel", "L": "#c:plates/lead"}, 1),
    "lead_glass": ([" G ", "GLG", " G "], {"G": "minecraft:glass", "L": "#c:dusts/lead"}, 4),
    "warning_beacon": (["G", "R", "P"], {"G": "minecraft:orange_stained_glass", "R": "minecraft:redstone_lamp", "P": "#c:plates/steel"}, 2),
}
TURN = {"north": 0, "east": 90, "south": 180, "west": 270}


def _turned(model, f):
    v = {"model": model}
    if TURN[f]:
        v["y"] = TURN[f]
    return v


def write_assets(write, rid_, assets, lang):
    """Models, blockstates, item models and names (tools/nuclear.py calls this)."""
    import zfight
    write = zfight.unfighting(write)  # no two faces of a model in the same place (Justin: never any z-fighting)
    mdir = assets / "models" / "block"
    for name, m in models().items():
        write(mdir / f"{name}.json", m)
    for name in BLOCKS:
        lang[f"block.{MOD}.{name}"] = NAMES[name]
    lang.update(LANG)
    for name in ("lead_pig", "waste_drum"):
        write(assets / "blockstates" / f"{name}.json", {"variants": {f"facing={f}": _turned(rid(f"block/{name}"), f) for f in TURN}})
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
    write(assets / "blockstates" / "lead_locker.json", {"variants": {
        f"facing={f},open={o}": _turned(rid("block/lead_locker" + ("_open" if o == "true" else "")), f) for f in TURN for o in ("false", "true")}})
    write(assets / "items" / "lead_locker.json", {"model": {"type": "minecraft:model", "model": rid("block/lead_locker")}})
    write(assets / "blockstates" / "warning_beacon.json", {"variants": {
        f"colour={c},facing={f}": _turned(rid(f"block/warning_beacon_{c}"), f) for c in BEACON_COLOURS for f in TURN}})
    write(assets / "items" / "warning_beacon.json", {"model": {"type": "minecraft:model", "model": rid("block/warning_beacon_amber")}})
    write(assets / "blockstates" / "lead_glass.json", {"variants": {"": {"model": rid("block/lead_glass")}}})
    write(assets / "items" / "lead_glass.json", {"model": {"type": "minecraft:model", "model": rid("block/lead_glass")}})
    # The door: the vanilla door blockstate pattern over its own models.
    closed = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, y0 in closed.items():
        for half, part in (("lower", "bottom"), ("upper", "top")):
            for hinge in ("left", "right"):
                for opened in (False, True):
                    y = (y0 + (90 if hinge == "left" else 270)) % 360 if opened else y0
                    v = {"model": rid(f"block/lead_door_{part}_{hinge}{'_open' if opened else ''}")}
                    if y:
                        v["y"] = y
                    variants[f"facing={facing},half={half},hinge={hinge},open={'true' if opened else 'false'}"] = v
    write(assets / "blockstates" / "lead_door.json", {"variants": variants})
    write(assets / "models" / "item" / "lead_door.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/lead_door")}})
    write(assets / "items" / "lead_door.json", {"model": {"type": "minecraft:model", "model": rid("item/lead_door")}})


def write_loot(write, rid_, out, self_drop):
    """The pig keeps what is in it when broken (like a shulker box); the others drop themselves (the locker's and the
    drum's contents spill); the door drops from its lower half (26.x loot format)."""
    for name in ("lead_locker", "waste_drum", "lead_glass", "warning_beacon"):
        write(out / f"{name}.json", self_drop(name))
    write(out / "lead_pig.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": rid("lead_pig"),
        "modifier": {"type": "minecraft:copy_components", "source": "block_entity",
                     "include": ["minecraft:custom_name", "minecraft:container"]}}]}], "random_sequence": rid("blocks/lead_pig")})
    import plant_interior
    write(out / "lead_door.json", plant_interior.door_loot(rid_, "lead_door"))


def door_item_icon():
    """The door's item icon: its outside face, top over bottom, at icon size."""
    faces = door_faces()
    top, bottom = faces["lead_door_outside_top"], faces["lead_door_outside_bottom"]
    tall = Image.new("RGBA", (top.width, top.height * 2))
    tall.paste(top, (0, 0))
    tall.paste(bottom, (0, top.height))
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    icon.paste(tall.resize((8, 16), Image.LANCZOS), (4, 0))
    return icon


def write_textures(root):
    from pathlib import Path
    tdir = Path(root) / "src/main/resources/assets/jugcraft/textures/block"
    for name, im in textures().items():
        im.save(tdir / f"{name}.png")


def write_models(root):
    import json
    from pathlib import Path
    mdir = Path(root) / "src/main/resources/assets/jugcraft/models/block"
    for name, m in models().items():
        (mdir / f"{name}.json").write_text(json.dumps(m, indent=2) + "\n")


if __name__ == "__main__":
    import os
    import sys
    root = os.path.join(os.path.dirname(__file__), "..")
    write_textures(root)
    write_models(root)
