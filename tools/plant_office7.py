"""Round 10 of the plant kit: the warehouse.

- The storage rack: one piece of pallet racking three bays long and three levels high (a block deep), which is a real
  store (54 slots, six to a bay). Its frame is a block model here; what is on its pallets is drawn by
  client/StorageRackRenderer.java from what is in it: a pallet appears in a bay with its first item, and boxes pile up
  on it (two by two, then a second layer) as the bay fills. The look of each load comes from the item: kraft cartons,
  white cartons, labelled cartons, blue tote crates, steel drums, wooden crates.
- High-bay lights: round LED fittings hung close under a high roof on a short chain with the power cable run down it.
- The chain with its cable (for the lights hung lower in other buildings).
- Pallet racking with drums (the turbine building's oil store), which had no model.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import plant_office3 as O3
import plant_office5 as O5

S = 128
cyl = O5.cyl


# ---------------------------------------------------------------------------------------------- the storage rack

def rack_part(c, r):
    """Bay c (0..2, left to right as you face the rack), level r (0..2) of the storage rack's frame: blue uprights
    front and back at each end of the bay (the middle ones shared between two bays), orange step beams front and back
    under every level but the floor's and over the top one, a wire deck on the beams, cross braces in the end frames,
    base plates on the floor. Front at the south (z 16)."""
    out = []
    x0 = 0.0 if c == 0 else -0.8          # a shared upright straddles the bays' boundary
    x1 = 16.0 if c == 2 else 16.8
    left = (0, 1.5) if c == 0 else (0, 0.8)
    right = (14.5, 16) if c == 2 else (15.2, 16)
    for a, b in (left, right):
        for z0, z1 in ((0.6, 2.0), (14.0, 15.4)):
            out.append((a, 0, z0, b, 16, z1, {"*": "pi_upright", "up": "pi_upright_end", "down": "pi_upright_end"}))
        if r == 0:
            for z0, z1 in ((0.2, 2.4), (13.6, 15.8)):
                out.append((a - (0.3 if a == 0 and c == 0 else 0), 0, z0, b + (0.3 if b == 16 and c == 2 else 0), 0.4, z1, "pi_upright"))
    # The end frames' bracing: horizontals and a diagonal between front and back uprights.
    for edge, (a, b) in ((0, left), (2, right)):
        if c == edge:
            mid = (a + b) / 2
            for y in (3.0, 11.0):
                out.append((mid - 0.3, y, 2.0, mid + 0.3, y + 0.5, 14.0, "pi_upright"))
            out.append(O5.turned((mid - 0.25, 7.0 - 0.25, 2.0, mid + 0.25, 7.0 + 0.25, 14.0, "pi_upright"), "x", 45, (mid, 7.0, 8.0)))
    # Beams: under this level (not on the floor), and over the top level.
    lo = 1.5 if c == 0 else 0.8
    hi = 14.5 if c == 2 else 15.2
    if r > 0:
        out += [(lo, 0, 0.3, hi, 1.8, 1.4, {"*": "pi_beam", "up": "pi_beam_top"}), (lo, 0, 14.6, hi, 1.8, 15.7, {"*": "pi_beam", "up": "pi_beam_top"}),
                (lo, 1.2, 1.4, hi, 1.4, 14.6, "pi_wire_deck")]
    if r == 2:
        out += [(lo, 14.4, 0.3, hi, 16, 1.4, {"*": "pi_beam", "up": "pi_beam_top"}), (lo, 14.4, 14.6, hi, 16, 15.7, {"*": "pi_beam", "up": "pi_beam_top"})]
    return out


# ---------------------------------------------------------------------------------------------- lights

def chain_planes(x, z, y0, y1):
    """The game's chain (its own texture) between y0 and y1: two crossed planes turned 45 degrees."""
    a = O5.turned((x - 1.5, y0, z, x + 1.5, y1, z, {"*": None, "north": ("mc_iron_chain", [0, 0, 3, 16]),
                                                   "south": ("mc_iron_chain", [0, 0, 3, 16])}), "y", 45, (x, y0, z))
    b = O5.turned((x, y0, z - 1.5, x, y1, z + 1.5, {"*": None, "east": ("mc_iron_chain", [3, 0, 6, 16]),
                                                   "west": ("mc_iron_chain", [3, 0, 6, 16])}), "y", 45, (x, y0, z))
    return [a, b]


def cable(x, z, y0, y1):
    """The power cable down beside a chain, with a tie every few pixels."""
    out = [(x - 0.3, y0, z - 0.3, x + 0.3, y1, z + 0.3, "pi_cable")]
    y = y0 + 1.5
    while y < y1 - 0.5:
        out.append((x - 0.45, y, z - 0.45, x + 0.45, y + 0.35, z + 0.45, "pi_cable_tie"))
        y += 4
    return out


# A light hung lower down on chain: the chain with the cable run down it (stands in for a plain chain block).
LIGHT_HANGER = chain_planes(8, 8, 0, 16) + cable(9.3, 8.6, 0, 16)

# The high-bay light: a ceiling plate, a short chain and its cable, the driver with its cooling fins, the spun
# aluminium reflector widening down to the lens, which glows.
HIGH_BAY = (
    [(5.8, 15.6, 5.8, 10.2, 16, 10.2, "pi_steel_plate")]
    + chain_planes(8, 8, 11.6, 15.6) + cable(9.3, 8.6, 11.6, 15.6)
    + cyl(8, 8, 2.3, 10.4, 11.6, {"*": "pi_heatsink", "up": "pi_heatsink_top", "down": "pi_heatsink_top"})
    + cyl(8, 8, 3.2, 9.7, 10.4, "pi_reflector")
    + cyl(8, 8, 4.4, 9.0, 9.7, "pi_reflector")
    + cyl(8, 8, 5.5, 8.3, 9.0, "pi_reflector")
    + cyl(8, 8, 5.2, 8.0, 8.3, {"*": "pi_reflector", "down": "pi_highbay_lens"})
)


# ---------------------------------------------------------------------------------------------- drums on racking

def drum(cx, cz, y0, h, tex="pi_drum_blue"):
    """A 200-litre steel drum standing on a pallet: the body with its rolling hoops, the lid with its bung."""
    return (cyl(cx, cz, 2.9, y0, y0 + h, {"*": tex, "up": "pi_drum_lid", "down": "pi_drum_lid"})
            + cyl(cx, cz, 3.05, y0 + h * 0.33, y0 + h * 0.33 + 0.4, tex) + cyl(cx, cz, 3.05, y0 + h * 0.66, y0 + h * 0.66 + 0.4, tex))


DRUM_LOAD = ([(1, 1.8, 1.5, 15, 3, 14.5, {"*": "pi_pallet", "up": "pi_pallet_top"})]
             + drum(4.6, 4.6, 3, 9.0) + drum(11.4, 4.6, 3, 9.0, "pi_drum_red") + drum(4.6, 11.4, 3, 9.0, "pi_drum_red")
             + drum(11.4, 11.4, 3, 9.0))


# ---------------------------------------------------------------------------------------------- textures

def _img(c, size=S):
    a = np.zeros((size, size, 4), np.uint8)
    a[..., :3] = c[:3]
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _heatsink():
    img = _img((60, 62, 66))
    d = ImageDraw.Draw(img)
    for x in range(0, S, 10):
        d.rectangle([x, 0, x + 4, S], fill=(96, 100, 106, 255))
    return img


def _heatsink_top():
    img = _img((70, 72, 76))
    d = ImageDraw.Draw(img)
    for k in range(16):
        a = k / 16 * math.tau
        d.line([(64, 64), (64 + math.cos(a) * 60, 64 + math.sin(a) * 60)], fill=(110, 114, 120, 255), width=3)
    d.ellipse([46, 46, 82, 82], fill=(50, 52, 56, 255))
    return img


def _reflector():
    rr = np.random.default_rng(7101)
    a = np.zeros((S, S, 4), np.uint8)
    y = np.arange(S)[:, None]
    base = 196 + 30 * np.sin(y / S * math.pi)
    a[..., :3] = np.clip(base[..., None] + rr.normal(0, 3, (S, S, 1)), 0, 255)
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _lens():
    img = _img((255, 252, 240))
    d = ImageDraw.Draw(img)
    for r in range(10, 64, 10):
        d.ellipse([64 - r, 64 - r, 64 + r, 64 + r], outline=(236, 236, 228, 255), width=2)
    return img


def _drum(colour, seed, label="plain"):
    img = _img(colour)
    d = ImageDraw.Draw(img)
    rr = random.Random(seed)
    for _ in range(40):
        x, y = rr.randrange(S), rr.randrange(S)
        d.point((x, y), fill=tuple(max(0, c - 30) for c in colour) + (255,))
    if label == "corrosive":
        # The corrosive diamond: white over black halves, on a white label.
        d.rectangle([36, 34, 92, 86], fill=(246, 246, 240, 255))
        d.polygon([(64, 38), (88, 60), (64, 82), (40, 60)], fill=(250, 250, 250, 255), outline=(20, 20, 22, 255))
        d.polygon([(42, 60), (86, 60), (64, 81)], fill=(20, 20, 22, 255))
        d.rectangle([54, 46, 58, 54], fill=(20, 20, 22, 255))
        d.rectangle([68, 46, 72, 54], fill=(20, 20, 22, 255))
    elif label == "hazard":
        # A black band with a yellow-on-black hazard stripe.
        d.rectangle([0, 50, S, 70], fill=(24, 24, 26, 255))
        for x in range(-20, S, 16):
            d.polygon([(x, 70), (x + 8, 70), (x + 28, 50), (x + 20, 50)], fill=tuple(colour) + (255,))
    else:
        d.rectangle([40, 40, 88, 80], fill=(236, 236, 230, 255))
        d.rectangle([40, 40, 88, 50], fill=(30, 30, 34, 255))
    return img


def _jerrycan():
    """A natural plastic jerrycan's side: the embossed X, the seam and a fill line."""
    c = (226, 222, 204)
    img = O5._speckle(c, 7210, [(214, 210, 192, 255), (236, 232, 216, 255)], 300)
    d = ImageDraw.Draw(img)
    dark = (196, 190, 168, 255)
    d.rectangle([0, 0, S - 1, S - 1], outline=dark, width=4)
    d.line([(14, 18), (S - 14, S - 18)], fill=dark, width=8)
    d.line([(S - 14, 18), (14, S - 18)], fill=dark, width=8)
    d.line([(0, 30), (S, 30)], fill=(186, 180, 158, 255), width=2)
    # The contents show through the plastic up to the fill line.
    d.rectangle([4, 34, 8, S - 6], fill=(170, 160, 120, 255))
    return img


def _jerrycan_top():
    img = _img((222, 218, 200))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=(196, 190, 168, 255), width=6)
    return img


def _kraft_fragile(c, seed):
    img = _kraft(c, seed)
    d = ImageDraw.Draw(img)
    d.rectangle([18, 30, 110, 62], fill=(200, 30, 36, 255))
    d.text((30, 38), "FRAGILE", font=O3._font(16), fill=(250, 250, 250, 255))
    # The wine glass, and the this-way-up arrows.
    d.polygon([(50, 72), (78, 72), (70, 90), (58, 90)], fill=(200, 30, 36, 255))
    d.rectangle([62, 90, 66, 104], fill=(200, 30, 36, 255))
    d.rectangle([54, 104, 74, 108], fill=(200, 30, 36, 255))
    for x in (16, 98):
        d.polygon([(x, 84), (x + 6, 74), (x + 12, 84)], fill=(40, 40, 46, 255))
        d.rectangle([x + 4, 84, x + 8, 98], fill=(40, 40, 46, 255))
    return img


def _kraft_label_c(c, seed):
    img = _kraft(c, seed)
    d = ImageDraw.Draw(img)
    # A shipping label: green header, a code block, the part number.
    d.rectangle([60, 20, 116, 100], fill=(246, 246, 240, 255))
    d.rectangle([60, 20, 116, 32], fill=(40, 130, 70, 255))
    rr = random.Random(seed)
    for i in range(6):
        for j in range(6):
            if rr.random() < 0.5:
                d.rectangle([66 + i * 5, 38 + j * 5, 69 + i * 5, 41 + j * 5], fill=(30, 30, 34, 255))
    d.text((64, 72), "PN 2290-C", font=O3._font(10), fill=(40, 40, 46, 255))
    d.text((64, 84), "QTY 24", font=O3._font(10), fill=(40, 40, 46, 255))
    # Strapping tape down the side.
    d.rectangle([20, 0, 30, S], fill=(206, 170, 110, 255))
    return img


def _drum_lid():
    img = _img((150, 154, 158))
    d = ImageDraw.Draw(img)
    d.ellipse([8, 8, 120, 120], outline=(110, 114, 118, 255), width=6)
    d.ellipse([26, 26, 42, 42], fill=(80, 84, 88, 255))
    d.ellipse([86, 86, 96, 96], fill=(80, 84, 88, 255))
    return img


def _cable_tie():
    return _img((30, 30, 32))


def makers():
    return {
        "pi_heatsink": _heatsink, "pi_heatsink_top": _heatsink_top, "pi_reflector": _reflector, "pi_highbay_lens": _lens,
        "pi_steel_plate": lambda: O5._brushed((170, 174, 178), 7102), "pi_cable_tie": _cable_tie,
        "pi_drum_blue": lambda: _drum((40, 80, 160), 7103), "pi_drum_red": lambda: _drum((170, 40, 40), 7104),
        "pi_drum_lid": _drum_lid, "pi_upright_end": lambda: _img((30, 60, 120)),
        "pi_beam_top": lambda: _img((214, 110, 30)),
    }


SHARP = set()
TRANSLUCENT = set()


# ---------------------------------------------------------------------------------------------- the rack's loads
# client/StorageRackRenderer.java draws the pallets and loads from this sheet: 256 x 256, sixteen 64 x 64 pictures
# in this order (its region(col, row)).

RACK_MATERIALS = ["kraft", "kraft_top", "kraft_label", "white",
                  "white_top", "tote", "tote_lid", "drum",
                  "drum_top", "crate", "pallet", "pallet_dark",
                  "drum_red", "kraft_dark", "white_label", "crate_top"]
# The second half of the sheet (columns 4-7, rows 0-1): the other fluid containers and two more carton labels.
RACK_MATERIALS_2 = ["drum_black", "drum_green", "drum_yellow", "drum_white",
                    "jerrycan", "jerrycan_top", "kraft_fragile", "kraft_label_c"]


def _kraft(c, seed, label=False, top=False):
    img = O5._speckle(c, seed, [tuple(max(0, v - 14) for v in c) + (255,), tuple(min(255, v + 10) for v in c) + (255,)], 500)
    d = ImageDraw.Draw(img)
    # The edges darken a little (the corners of a carton).
    d.rectangle([0, 0, S - 1, S - 1], outline=tuple(max(0, v - 30) for v in c) + (255,), width=3)
    if top:
        # Tape across the flaps, the seam under it.
        d.rectangle([52, 0, 76, S], fill=(206, 170, 110, 255))
        d.line([(64, 0), (64, S)], fill=tuple(max(0, v - 40) for v in c) + (255,), width=2)
    elif label:
        d.rectangle([22, 30, 106, 86], fill=(246, 246, 240, 255))
        d.rectangle([22, 30, 106, 42], fill=(30, 80, 160, 255))
        for k in range(14):
            if k % 3:
                d.rectangle([28 + k * 5, 50, 30 + k * 5, 68], fill=(30, 30, 34, 255))
        d.text((28, 72), "PN 4417-B", font=O3._font(11), fill=(40, 40, 46, 255))
        # This way up arrows.
        for x in (14, 104):
            d.polygon([(x, 104), (x + 5, 96), (x + 10, 104)], fill=(40, 40, 46, 255))
            d.rectangle([x + 3, 104, x + 7, 114], fill=(40, 40, 46, 255))
    return img


def _tote():
    img = _img((40, 90, 170))
    d = ImageDraw.Draw(img)
    for y in range(18, S - 10, 14):
        d.rectangle([10, y, S - 10, y + 6], fill=(30, 72, 140, 255))
    d.rectangle([40, 8, 88, 16], fill=(24, 60, 120, 255))     # the hand hold
    return img


def _tote_lid():
    img = _img((50, 104, 190))
    d = ImageDraw.Draw(img)
    d.line([(64, 0), (64, S)], fill=(30, 72, 140, 255), width=4)
    d.rectangle([4, 4, S - 5, S - 5], outline=(30, 72, 140, 255), width=4)
    return img


def _crate():
    img = _img((176, 136, 86))
    d = ImageDraw.Draw(img)
    for y in range(0, S, 32):
        d.line([(0, y), (S, y)], fill=(130, 96, 56, 255), width=3)
    d.rectangle([0, 0, 12, S], fill=(150, 112, 68, 255))
    d.rectangle([S - 12, 0, S, S], fill=(150, 112, 68, 255))
    d.line([(12, S - 8), (S - 12, 8)], fill=(150, 112, 68, 255), width=10)
    return img


def _pallet(c):
    img = _img(c)
    d = ImageDraw.Draw(img)
    rr = random.Random(c[0])
    for _ in range(60):
        y = rr.randrange(S)
        d.line([(rr.randrange(S), y), (rr.randrange(S), y)], fill=tuple(max(0, v - 24) for v in c) + (255,))
    return img


def draw_rack_sheet():
    tiles = {
        "kraft": _kraft((196, 154, 100), 7201), "kraft_top": _kraft((196, 154, 100), 7202, top=True),
        "kraft_label": _kraft((196, 154, 100), 7203, label=True), "white": _kraft((236, 234, 226), 7204),
        "white_top": _kraft((236, 234, 226), 7205, top=True), "tote": _tote(), "tote_lid": _tote_lid(),
        "drum": _drum((40, 80, 160), 7206), "drum_top": _drum_lid(), "crate": _crate(),
        "pallet": _pallet((186, 150, 100)), "pallet_dark": _pallet((140, 108, 70)), "drum_red": _drum((170, 40, 40), 7207),
        "kraft_dark": _kraft((170, 128, 80), 7208), "white_label": _kraft((236, 234, 226), 7209, label=True),
        "crate_top": _crate(),
        "drum_black": _drum((34, 34, 38), 7211), "drum_green": _drum((40, 120, 64), 7212),
        "drum_yellow": _drum((226, 184, 30), 7213, label="hazard"),
        "drum_white": _drum((232, 232, 226), 7214, label="corrosive"),
        "jerrycan": _jerrycan(), "jerrycan_top": _jerrycan_top(),
        "kraft_fragile": _kraft_fragile((196, 154, 100), 7215), "kraft_label_c": _kraft_label_c((196, 154, 100), 7216),
    }
    sheet = Image.new("RGBA", (512, 256), (0, 0, 0, 255))
    for k, name in enumerate(RACK_MATERIALS):
        sheet.paste(tiles[name].resize((64, 64), Image.LANCZOS).convert("RGBA"), ((k % 4) * 64, (k // 4) * 64))
    for k, name in enumerate(RACK_MATERIALS_2):
        sheet.paste(tiles[name].resize((64, 64), Image.LANCZOS).convert("RGBA"), (256 + (k % 4) * 64, (k // 4) * 64))
    return sheet
