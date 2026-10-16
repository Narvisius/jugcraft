"""Textures for the Foundry Trench (tools/foundry_trench.py, docs/features/foundry-trench.md): its four lair-only blocks
and the Sealed Orders' icon (its map, tools/item_icons/sealed_orders.txt). Painted here by code, 16 x 16, in the clean
style the other lairs use: a few flat tones a material, crisp edges, the patterns (a lattice's bracing, a panel's
switch, a plate's rivets) laid out whole within the block rather than as noise. Called from tools/lair_textures.py. No
Mojang texture is read, traced or copied.

Dieselpunk, as docs/ART_DIRECTION.md asks of the higher tiers: olive-drab paint on riveted steel, gunmetal, copper,
bakelite and hazard stripes.
- the pylon lattice: steel posts banded at their bolted joints, and between them a plane of X bracing with a strut at
  its top and foot, the gaps cut out so the far side's bracing shows through;
- the pylon crown: a ribbed brown porcelain insulator, a ring and ball of copper; live, the copper burns violet-white
  with the charge in it;
- the breaker: an olive-drab steel cabinet; on its face a slate panel with a copper jaw for the blade, a yellow warning
  placard with a bolt on it and a hazard stripe at its foot; a caged amber lamp, dark or lit; a copper blade with a bakelite handle;
  and the plate that fouls it, gunmetal torn from the Dreadnought's hull, a rivet line and a hazard stripe on it;
- the ground terminal: a copper plate riveted into the concrete floor, a bolted lug in its middle and four straps out
  from it; live, the copper burns violet-white.

The models' faces (tools/lair_data.py) take the textures at a texel a pixel: the bracing shows 2 to 14 across, the
breaker's panel 2 to 14 across and 3 to 16 down.
"""
from PIL import Image

import item_icons


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*hexes):
    return [rgb(h) + (255,) for h in hexes]


STEEL = pal("1e2226", "2c3238", "3c444a", "4e585e", "66717a", "8a959c")     # the lattice: dark to bright
OLIVE = pal("23261a", "33381f", "454b29", "596035", "6e7642")               # olive-drab paint
GUNMETAL = pal("1c1e22", "2a2d33", "3a3e45", "4c5159", "636970")
COPPER = pal("4a2410", "6e3618", "974c22", "bf6a32", "dc8e4c", "f2b67a")
PORCELAIN = pal("2e1a10", "4a2c1a", "6a4026", "8a5634", "a86e44")        # brown-glazed insulator
LIVE = pal("2a1046", "4a1e7a", "7036b0", "9c62dc", "c8a0f4", "f0e4ff")     # the charge: deep violet to white-hot
AMBER = pal("3a2408", "5a3a0e", "8a5a16", "c08420", "eab040", "fde08a")
SLATE = pal("16181b", "202328", "2b2f35", "383d44")
BAKELITE = pal("140c08", "24160e", "3a2416", "523420")
HAZARD = pal("1a1a1a", "e0b020")                                            # black, yellow
CONCRETE = pal("3e4146", "474a50", "52565c", "5c6066")


def new(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (16, 16), fill)


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c)


def rows(img, colours):
    """Fills each row y with colours[y]: a texture that looks the same whichever columns a face crops."""
    for y, c in enumerate(colours):
        for x in range(16):
            img.putpixel((x, y), c)


# ---------------------------------------------------------------- the pylon

def lattice_post():
    """A lattice post: steel, a bolted joint band every half block (a dark seam, a bright bolt row over it)."""
    img = new()
    colours = []
    for y in range(16):
        k = y % 8
        colours.append(STEEL[0] if k == 7 else STEEL[5] if k == 0 else STEEL[4] if k == 1 else STEEL[3] if k < 5 else
                       STEEL[2])
    rows(img, colours)
    return img


def lattice_side():
    """The bracing between the posts (shown from column 2 to 13): a strut along the top and the foot, and an X of two
    flat bars, one lit and one in shadow, the gaps open."""
    img = new()
    for x in range(2, 14):
        put(img, x, 0, STEEL[4])
        put(img, x, 1, STEEL[2])
        put(img, x, 14, STEEL[3])
        put(img, x, 15, STEEL[1])
    for i in range(12):
        x, y = 2 + i, 2 + i
        put(img, x, y, STEEL[4])
        put(img, x, y + 1, STEEL[3])
        put(img, 13 - i, y, STEEL[2])
        put(img, 13 - i, y + 1, STEEL[1])
    # Bolts where the bars cross and meet the struts.
    for x, y in ((7, 7), (8, 8), (2, 2), (13, 2), (2, 13), (13, 13)):
        put(img, x, y, STEEL[5])
    return img


def crown_insulator():
    """A ribbed porcelain insulator, brown-glazed: a rib every two rows, its lip lit and its underside shaded."""
    img = new()
    rows(img, [PORCELAIN[4] if y % 4 == 0 else PORCELAIN[3] if y % 4 == 1 else PORCELAIN[1] if y % 4 == 2 else
               PORCELAIN[0] for y in range(16)])
    return img


def crown_copper():
    """Polished copper: bands of light across it, as round metal catches the light, and a seam."""
    img = new()
    rows(img, [COPPER[5] if y in (2, 9) else COPPER[4] if y in (1, 3, 8, 10) else COPPER[1] if y in (6, 13) else
               COPPER[0] if y == 15 else COPPER[3] if y < 8 else COPPER[2] for y in range(16)])
    return img


def crown_live():
    """The copper burning with the charge: violet, white-hot bands where the copper was bright, and a few sparks."""
    img = new()
    rows(img, [LIVE[5] if y in (2, 9) else LIVE[4] if y in (1, 3, 8, 10) else LIVE[1] if y in (6, 13) else
               LIVE[2] if y >= 11 else LIVE[3] for y in range(16)])
    for x, y in ((3, 5), (11, 4), (6, 12), (13, 11), (1, 14)):
        put(img, x, y, LIVE[5])
    return img


# ---------------------------------------------------------------- the breaker

def breaker_cabinet():
    """The cabinet: olive-drab steel, a seam where its door meets the frame, a line of rivets along it, the paint worn
    to bare steel along its top."""
    img = new(OLIVE[2])
    for x in range(16):
        put(img, x, 0, OLIVE[4])
        put(img, x, 1, OLIVE[3])
        put(img, x, 15, OLIVE[0])
    for y in range(16):
        put(img, 0, y, OLIVE[3])
        put(img, 15, y, OLIVE[1])
        put(img, 8, y, OLIVE[1])
    for y in (3, 7, 11):
        put(img, 3, y, OLIVE[4])
        put(img, 12, y, OLIVE[4])
    for x in (2, 6, 13):
        put(img, x, 0, GUNMETAL[4])
    return img


def breaker_panel():
    """The switch panel (shown from column 2 to 13, row 3 to 15): slate in an olive frame; the copper jaw the blade
    closes into at its top, a white placard with a bolt on it, and a hazard stripe along its foot."""
    img = new(OLIVE[2])
    for x in range(3, 13):
        for y in range(4, 13):
            put(img, x, y, SLATE[2])
    for x in range(3, 13):
        put(img, x, 4, SLATE[0])
    for y in range(4, 13):
        put(img, 3, y, SLATE[0])
        put(img, 12, y, SLATE[3])
    # The jaw: two copper cheeks either side of where the blade stands (columns 7 and 8).
    for y in (5, 6):
        put(img, 6, y, COPPER[4])
        put(img, 9, y, COPPER[2])
    put(img, 7, 7, COPPER[1])
    put(img, 8, 7, COPPER[1])
    # The warning placard, low on the panel's left: a black bolt zigzagging down yellow.
    for x in range(4, 7):
        for y in range(8, 12):
            put(img, x, y, HAZARD[1])
    for x, y in ((6, 8), (5, 9), (6, 9), (4, 10), (5, 10), (4, 11)):
        put(img, x, y, HAZARD[0])
    # Rivets in the panel's corners.
    for x, y in ((4, 5), (11, 5), (11, 11)):
        put(img, x, y, SLATE[3])
    # The hazard stripe: diagonal bands of black and yellow along the foot.
    for x in range(2, 14):
        for y in (13, 14):
            put(img, x, y, HAZARD[1] if (x + y) % 4 < 2 else HAZARD[0])
    for x in range(2, 14):
        put(img, x, 15, OLIVE[0])
    return img


def breaker_lamp(lit):
    """An amber lamp in a steel cage: the cage's bars across and down, the glass dark or burning between them."""
    glass = AMBER[5] if lit else AMBER[1]
    glow = AMBER[4] if lit else AMBER[2]
    img = new(glass)
    for x in range(16):
        for y in range(16):
            if (x + y) % 5 == 0 and lit:
                put(img, x, y, glow)
            elif not lit and (x * 3 + y) % 7 == 0:
                put(img, x, y, glow)
    for i in range(16):
        for b in (0, 5, 10, 15):
            put(img, b, i, GUNMETAL[1])
            put(img, i, b, GUNMETAL[2])
    return img


def breaker_blade():
    """The blade: a bar of copper, its edges bright and its middle shaded."""
    img = new()
    rows(img, [COPPER[5] if y % 5 == 0 else COPPER[3] if y % 5 in (1, 4) else COPPER[2] for y in range(16)])
    for y in range(16):
        put(img, 0, y, COPPER[4])
        put(img, 15, y, COPPER[1])
    return img


def breaker_handle():
    """Black bakelite, a sheen along its top."""
    img = new(BAKELITE[1])
    for x in range(16):
        put(img, x, 0, BAKELITE[3])
        put(img, x, 1, BAKELITE[2])
        put(img, x, 15, BAKELITE[0])
    for x in range(0, 16, 3):
        put(img, x, 6, BAKELITE[2])
    return img


def breaker_plate():
    """A torn plate of the Dreadnought's hull: gunmetal, a line of rivets, a hazard stripe along one edge and the torn
    edge bright where it ripped."""
    img = new(GUNMETAL[2])
    for x in range(16):
        put(img, x, 0, GUNMETAL[4])
        put(img, x, 15, GUNMETAL[0])
        for y in (11, 12, 13):
            put(img, x, y, HAZARD[1] if (x + y) % 4 < 2 else HAZARD[0])
    for x in range(1, 16, 3):
        put(img, x, 4, GUNMETAL[4])
        put(img, x, 5, GUNMETAL[0])
    for y, x in ((1, 15), (2, 14), (3, 15), (6, 14), (7, 15), (8, 14), (9, 15)):
        put(img, x, y, GUNMETAL[4])
    return img


# ---------------------------------------------------------------- the ground terminal

def terminal_side():
    """The terminal's sides: plain concrete, the floor's grey, a joint line under its top."""
    img = new(CONCRETE[1])
    for x in range(16):
        put(img, x, 0, CONCRETE[3])
        put(img, x, 1, CONCRETE[0])
    for x, y in ((3, 5), (11, 4), (7, 9), (13, 11), (2, 13), (9, 14)):
        put(img, x, y, CONCRETE[2])
    return img


def terminal_top(live):
    """A copper plate riveted into the concrete: a margin of concrete round it, a rivet in each corner, a bolted lug in
    its middle and four straps out from it to the plate's edges. Live, the copper burns."""
    tones = LIVE if live else COPPER
    img = new(CONCRETE[1])
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                put(img, x, y, CONCRETE[0] if (x + y) % 3 else CONCRETE[2])
    for x in range(1, 15):
        for y in range(1, 15):
            put(img, x, y, tones[2])
    for i in range(1, 15):
        put(img, i, 1, tones[4])
        put(img, 1, i, tones[3])
        put(img, i, 14, tones[1])
        put(img, 14, i, tones[1])
    # The straps: a brighter band across and down the middle.
    for i in range(2, 14):
        for j in (7, 8):
            put(img, i, j, tones[3])
            put(img, j, i, tones[3])
    # The lug: a ring round a bolt.
    for x, y in ((6, 6), (7, 6), (8, 6), (9, 6), (6, 9), (7, 9), (8, 9), (9, 9), (6, 7), (6, 8), (9, 7), (9, 8)):
        put(img, x, y, tones[4])
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        put(img, x, y, tones[5] if live else tones[0])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(img, x, y, tones[5])
    return img


def foundry_trench_textures():
    return {
        ("block", "pylon_lattice_post"): lattice_post(),
        ("block", "pylon_lattice_side"): lattice_side(),
        ("block", "pylon_crown_insulator"): crown_insulator(),
        ("block", "pylon_crown_copper"): crown_copper(),
        ("block", "pylon_crown_live"): crown_live(),
        ("block", "breaker_cabinet"): breaker_cabinet(),
        ("block", "breaker_panel"): breaker_panel(),
        ("block", "breaker_lamp"): breaker_lamp(False),
        ("block", "breaker_lamp_lit"): breaker_lamp(True),
        ("block", "breaker_blade"): breaker_blade(),
        ("block", "breaker_handle"): breaker_handle(),
        ("block", "breaker_plate"): breaker_plate(),
        ("block", "ground_terminal_side"): terminal_side(),
        ("block", "ground_terminal_top"): terminal_top(False),
        ("block", "ground_terminal_live"): terminal_top(True),
        ("item", "sealed_orders"): item_icons.draw("sealed_orders"),
    }


if __name__ == "__main__":
    import sys
    sheet = foundry_trench_textures()
    keys = sorted(sheet)
    scale, cols = 8, 6
    out = Image.new("RGBA", (cols * 16 * scale + (cols + 1) * 4, ((len(keys) + cols - 1) // cols) * (16 * scale + 4) + 4),
                    (90, 90, 96, 255))
    for i, key in enumerate(keys):
        img = sheet[key].resize((16 * scale, 16 * scale), Image.NEAREST)
        out.paste(img, (4 + (i % cols) * (16 * scale + 4), 4 + (i // cols) * (16 * scale + 4)), img)
    out.save(sys.argv[1])
