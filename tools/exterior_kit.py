"""The realistic building exterior (Justin, 2026-10-17: "let's just make it look straight up realistic ... you can make
walls look realistic. You can make the stairs look actually real ... let's just realism max"), first used by the
warehouses (part W3):

- Wall Panel: an insulated metal panel one block (1 m) high, the way modern logistics buildings are clad: its micro-rib
  skin, the shadow joint at its foot, the white liner inside. Shapes: plain; joint (the top-hat cover strip over the
  vertical joint at a column line); column (the joint, and the steel H-column standing inside on the liner); corner
  (the corner flashing); pipe (a downpipe on its brackets in front); light (an LED wall pack, lit at night). Tones:
  silver, anthracite, and the anthracite trapezoidal profile (the stair towers).
- Precast Plinth: the precast concrete panels round the foot of the walls (two rows), the drip flashing on top, sealant
  joints at the column lines; the shoe of a downpipe over its gully; the sprinkler inlet.
- Parapet: the anthracite panel over the roof with the membrane turned up inside and the aluminium coping over it; a
  scupper and hopper head where a downpipe starts.
- Roof Deck: the single-ply membrane on top; underneath, the trapezoidal metal deck and (on the column lines) the
  steel girder carrying it. Floor slabs above a storey use the same soffit under their topping.
- Rooflight (opal polycarbonate dome on its kerb: daylight down into the top floor), Smoke Vent (twin flaps that open
  when the fire system trips), Roof Hatch.
- Steel stairs at a real angle (riser 1/6 block, tread 1/4 block: 33.7 degrees), their landings and handrails.

Every piece is drawn with its outer face to +z (the facing it is placed with is where that face looks), x across.
Block models turn only by 22.5 or 45 degrees, so a stair's stringers and handrails (33.7 degrees) are drawn by its
block entity renderer from stair_rails()."""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import plant_panels as PP
import warehouse as W
from warehouse import box

# ------------------------------------------------------------------------------------------------ textures
SILVER, ANTHRACITE, TRAP = "ex_imp_silver", "ex_imp_anthracite", "ex_trap_anthracite"
LINER, FLASH, FLASH_D = "ex_imp_liner", "ex_flash_silver", "ex_flash_anthracite"
PRECAST, PRECAST_LOW, SEALANT = "ex_precast", "ex_precast_low", "ex_sealant"
COPING, MEMBRANE, DECK, STEEL = "ex_coping", "ex_membrane", "ex_deck", "ex_steel_paint"
GULLY, FDC_RED, FDC_SIGN, POLY = "ex_gully", "ex_fdc_red", "ex_fdc_sign", "ex_polycarb"
LENS_ON, LENS_OFF, BLACK = "pi_wh_wallpack", "ex_lens_off", W.BLACK
YARD, CHROME = "ex_yard_concrete", W.CHROME
TREAD, NOSING, CHECKER, RAIL_PAINT, RAIL_YELLOW = "ex_tread", "ex_nosing", "ex_checker", "ex_stair_paint", "ex_handrail"

TONES = {"silver": SILVER, "anthracite": ANTHRACITE, "trapezoid": TRAP}


def _arr(img):
    return np.asarray(img.convert("RGBA")).astype(np.float32)


def _noise(size, seed, amp, scale=32):
    import nuclear_materials as M
    return M.fbm(size, seed, 4, scale) * amp


def _imp_skin(colour, seed, rib=True, streak=0.6):
    """An insulated panel's outer skin: powder-coated steel, a micro-rib every 4 px (lit top edge, shadow under it),
    a little mottling and the faint weathering streaks rain leaves."""
    import nuclear_materials as M
    n = 128
    a = M.base(colour, n)
    M.shade(a, _noise(n, seed, 5))
    if rib:
        for y in range(2, n, 8):
            a[y, :, :3] += 9
            a[y + 1, :, :3] -= 6
    if streak:
        M.shade(a, M.drips(n, seed + 1, count=5, amount=7 * streak))
    return M.to_img(a)


def _trapezoid(seed):
    import nuclear_materials as M
    a = M.base((64, 68, 72), 128)
    M.shade(a, M.streaks(128, seed, "v", count=120, amount=4))
    M.shade(a, M.drips(128, seed + 1, count=4, amount=5))
    return M.to_img(a)


def _flat(colour, seed, amp=4):
    import nuclear_materials as M
    a = M.base(colour, 64)
    M.shade(a, _noise(64, seed, amp, 16))
    return M.to_img(a)


def _precast(seed, low=False):
    """Precast concrete off a steel mould: smooth, pale, a few blowholes, faint mould marks; the lower row darker at
    its foot where rain splashes back."""
    import nuclear_materials as M
    n = 128
    a = M.base((184, 182, 174), n)
    M.shade(a, M.fbm(n, seed, 5, 64) * 12)
    M.shade(a, M.grain(n, seed + 1, 5))
    r = np.random.default_rng(seed + 2)
    for _ in range(40):
        x, y = int(r.integers(0, n)), int(r.integers(0, n))
        a[y, x, :3] -= 34
        if y + 1 < n:
            a[y + 1, x, :3] += 10
    if low:
        ramp = np.clip((np.arange(n) - n * 0.72) / (n * 0.28), 0, 1)[:, None]
        M.shade(a, -ramp * 34 + _noise(n, seed + 3, 10) * ramp)
    return M.to_img(a)


def _membrane(seed):
    """Light grey single-ply membrane: dull, a little mottled, the welded lap near one edge."""
    import nuclear_materials as M
    n = 128
    a = M.base((176, 180, 180), n)
    M.shade(a, _noise(n, seed, 5, 64))
    a[10:12, :, :3] -= 7
    a[12, :, :3] += 4
    return M.to_img(a)


def _deck(seed):
    """Galvanised steel deck: spangle and roll marks along the ribs."""
    import nuclear_materials as M
    n = 64
    a = M.base((178, 182, 184), n)
    M.shade(a, M.fbm(n, seed, 3, 8) * 14)
    M.shade(a, M.streaks(n, seed + 1, "h", count=60, amount=5))
    return M.to_img(a)


def _gully():
    img = Image.new("RGBA", (64, 64), (34, 34, 36, 255))
    d = ImageDraw.Draw(img)
    for x in range(4, 60, 7):
        d.rectangle([x, 4, x + 3, 59], fill=(10, 10, 12, 255))
    d.rectangle([0, 0, 63, 63], outline=(70, 70, 72, 255), width=3)
    return img


def _fdc_sign():
    img = Image.new("RGBA", (128, 32), (196, 30, 28, 255))
    d = ImageDraw.Draw(img)
    f = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 13)
    for i, t in enumerate(("SPRINKLER", "INLET")):
        w = d.textlength(t, font=f)
        d.text(((128 - w) / 2, 1 + i * 15), t, font=f, fill=(250, 250, 250, 255))
    # Square for the atlas (a texture wider than tall doesn't load): stretched four times up, the face's whole-texture
    # UV squeezes it back.
    return img.resize((128, 128), Image.LANCZOS)


def _poly():
    """Opal twin-wall polycarbonate: milky, the walls of its cells faintly showing."""
    img = Image.new("RGBA", (64, 64), (226, 232, 230, 168))
    d = ImageDraw.Draw(img)
    for x in range(0, 64, 4):
        d.line([(x, 0), (x, 63)], fill=(206, 214, 214, 190))
    return img


def _yard(seed):
    """Broom-finished concrete hardstanding (seamless: its joints are far apart)."""
    import nuclear_materials as M
    n = 128
    a = M.base((168, 166, 160), n)
    M.shade(a, M.fbm(n, seed, 5, 64) * 14)
    M.shade(a, M.streaks(n, seed + 1, "h", count=400, length=(30, 120), amount=6))
    return M.to_img(a)


def _checker(seed):
    """Checker plate: the raised lozenges in alternating directions, galvanised."""
    import nuclear_materials as M
    n = 64
    a = M.base((150, 154, 156), n)
    M.shade(a, _noise(n, seed, 8, 16))
    img = M.to_img(a)
    d = ImageDraw.Draw(img)
    for gy in range(0, n, 8):
        for gx in range(0, n, 8):
            if (gx // 8 + gy // 8) % 2:
                d.line([(gx + 1, gy + 6), (gx + 5, gy + 2)], fill=(196, 200, 202, 255), width=2)
            else:
                d.line([(gx + 1, gy + 2), (gx + 5, gy + 6)], fill=(196, 200, 202, 255), width=2)
    return img


def _tread(seed):
    """A pan tread's concrete fill: steel-trowelled, a little worn in the middle."""
    import nuclear_materials as M
    a = M.base((150, 150, 146), 64)
    M.shade(a, M.fbm(64, seed, 4, 16) * 12)
    return M.to_img(a)


def _nosing(seed):
    """The tread's nosing: yellow anti-slip insert with its grit."""
    import nuclear_materials as M
    a = M.base((226, 184, 30), 32)
    M.shade(a, M.grain(32, seed, 26))
    return M.to_img(a)


def ex_makers():
    import nuclear_materials as M
    return {
        SILVER: lambda: _imp_skin((200, 204, 206), 4101), ANTHRACITE: lambda: _imp_skin((66, 70, 74), 4102, streak=0.3),
        LINER: lambda: _imp_skin((228, 230, 226), 4103, streak=0.0), TRAP: lambda: _trapezoid(4104),
        FLASH: lambda: _flat((188, 192, 194), 4105), FLASH_D: lambda: _flat((60, 64, 68), 4106),
        PRECAST: lambda: _precast(4107), PRECAST_LOW: lambda: _precast(4108, low=True), SEALANT: lambda: _flat((54, 56, 58), 4109, 2),
        COPING: lambda: _flat((58, 62, 66), 4110, 3), MEMBRANE: lambda: _membrane(4111), DECK: lambda: _deck(4112),
        STEEL: lambda: M.painted((92, 104, 116), 4113, chip_count=2, drip_count=0, dirt=0.1, size=64),
        GULLY: _gully, FDC_RED: lambda: _flat((196, 30, 28), 4114, 6), FDC_SIGN: _fdc_sign, POLY: _poly,
        LENS_OFF: lambda: _flat((150, 152, 150), 4115, 3), YARD: lambda: _yard(4116), CHECKER: lambda: _checker(4117),
        TREAD: lambda: _tread(4118), NOSING: lambda: _nosing(4119),
        RAIL_PAINT: lambda: M.painted((70, 76, 84), 4120, chip_count=3, drip_count=0, dirt=0.1, size=64),
        RAIL_YELLOW: lambda: M.painted((232, 186, 24), 4121, chip_count=4, drip_count=0, dirt=0.15, size=64),
    }


EX_SHARP = {SILVER, ANTHRACITE, LINER, TRAP, FLASH, FLASH_D, PRECAST, PRECAST_LOW, SEALANT, COPING, MEMBRANE, DECK, GULLY,
            FDC_RED, FDC_SIGN, LENS_OFF, YARD, CHECKER, TREAD, NOSING}
EX_SEE_THROUGH = {POLY}


# ------------------------------------------------------------------------------------------------ walls
SKIN_Z = 15.8                 # the panels' outer face (the plinth's is 15.9, the trims stand proud)


def _downpipe(y0, y1, brackets=True):
    """A 110 mm downpipe (anthracite) 2.6 px off the wall, its brackets every block."""
    out = PP.round_y(8.0, 19.0, 1.7, y0, y1, FLASH_D)
    if brackets:
        for y in (y0 + 6.0,):
            if y + 1.2 <= y1:
                out += [box(7.4, y, SKIN_Z - 0.1, 8.6, y + 1.0, 17.2, FLASH_D), box(5.9, y - 0.2, 17.2, 10.1, y + 1.4, 20.8, FLASH_D)]
    return out


def _wall_pack(lit):
    """An LED wall pack: die-cast housing with its fins, the lens under it facing the ground, on a back plate."""
    return [box(4.6, 7.0, SKIN_Z - 0.1, 11.4, 11.0, 16.4, FLASH_D),
            box(5.0, 7.6, 16.4, 11.0, 10.8, 20.4, FLASH_D),
            box(5.4, 10.8, 16.8, 10.6, 11.4, 20.0, FLASH_D),
            box(5.4, 7.4, 16.8, 10.6, 7.6, 20.0, LENS_ON if lit else LENS_OFF)]


def wall_panel(tone="silver", shape="plain", lit=False):
    """An insulated metal panel, one block high: the core with the liner inside, the skin outside, the shadow joint
    at its foot. Shapes as the module says."""
    skin = TONES[tone]
    edge = FLASH_D if tone != "silver" else FLASH
    if shape == "corner":
        out = [box(0.0, 0.0, 0.0, 15.1, 16.0, 15.1, LINER),
               box(0.0, 0.5, 15.0, 15.0, 16.0, SKIN_Z, skin),
               box(15.0, 0.5, 0.0, SKIN_Z, 16.0, 15.0, skin),
               box(0.0, 0.0, 15.0, 15.0, 0.5, 15.4, SEALANT), box(15.0, 0.0, 0.0, 15.4, 0.5, 15.0, SEALANT),
               box(14.7, 0.0, 14.7, 16.4, 16.0, 16.4, edge)]
        return out
    out = [box(0.0, 0.0, 0.0, 16.0, 16.0, 15.1, {"*": LINER, "south": skin})]
    if tone == "trapezoid":
        # Trapezoidal sheet laid vertically: three crests a block, the troughs between them.
        out.append(box(0.0, 0.0, 15.0, 16.0, 16.0, 15.4, {"*": edge, "south": skin}))
        for x in (0.9, 6.2, 11.5):
            out += [box(x, 0.0, 15.4, x + 3.6, 16.0, 15.8, {"*": edge, "south": skin}),
                    box(x + 0.7, 0.0, 15.8, x + 2.9, 16.0, 16.6, {"*": edge, "south": skin})]
    else:
        out += [box(0.0, 0.5, 15.0, 16.0, 16.0, SKIN_Z, skin),
                box(0.0, 0.0, 15.0, 16.0, 0.5, 15.4, SEALANT)]
    if shape in ("joint", "column", "pipe"):
        out += [box(6.4, 0.0, SKIN_Z - 0.1, 9.6, 16.0, 16.3, edge), box(7.0, 0.0, 16.3, 9.0, 16.0, 16.6, edge)]
    if shape in ("column", "pipe"):
        # A UC section standing on the liner: back flange, web, front flange (its ends meet the next block's).
        out += [box(3.6, 0.0, -0.7, 12.4, 16.0, -0.1, STEEL), box(7.5, 0.0, -5.0, 8.5, 16.0, -0.7, STEEL),
                box(3.6, 0.0, -5.6, 12.4, 16.0, -5.0, STEEL)]
    if shape == "pipe":
        out += _downpipe(0.0, 16.0)
    if shape == "light":
        out += _wall_pack(lit)
    return out


def wall_plinth(row="upper", shape="plain"):
    """A precast plinth panel's block (rows: lower at the ground, upper under the cladding with the drip flashing)."""
    tex = PRECAST_LOW if row == "lower" else PRECAST
    out = []
    if shape == "corner":
        out += [box(0.0, 0.0, 0.0, 15.9, 16.0, 15.9, {"*": tex}),
                box(15.2, 0.0, 15.2, 16.1, 16.0, 16.1, tex)]
        if row == "upper":
            out += [box(0.0, 15.0, 15.9, 16.7, 16.0, 16.7, FLASH_D), box(15.9, 15.0, 0.0, 16.7, 16.0, 15.9, FLASH_D),
                    box(0.0, 14.4, 16.3, 16.7, 15.0, 16.7, FLASH_D), box(16.3, 14.4, 0.0, 16.7, 15.0, 16.3, FLASH_D)]
        return out
    if shape in ("joint", "pipe"):
        out += [box(0.0, 0.0, 0.0, 16.0, 16.0, 15.4, tex), box(0.0, 0.0, 15.3, 7.6, 16.0, 15.9, tex),
                box(8.4, 0.0, 15.3, 16.0, 16.0, 15.9, tex), box(7.6, 0.0, 15.3, 8.4, 16.0, 15.6, SEALANT)]
    else:
        out.append(box(0.0, 0.0, 0.0, 16.0, 16.0, 15.9, tex))
    if row == "upper":
        out += [box(0.0, 15.0, 15.9, 16.0, 16.0, 16.7, FLASH_D), box(0.0, 14.4, 16.3, 16.0, 15.0, 16.7, FLASH_D)]
    if shape == "pipe":
        if row == "upper":
            out += _downpipe(0.0, 14.4) + PP.round_y(8.0, 19.0, 1.7, 15.0, 16.0, FLASH_D)
        else:
            # The shoe: the pipe turns out over the gully at the foot.
            out += PP.round_y(8.0, 19.0, 1.7, 4.0, 16.0, FLASH_D) + [
                box(6.3, 1.6, 17.3, 9.7, 4.0, 22.6, FLASH_D), box(6.6, 1.4, 21.4, 9.4, 3.6, 22.9, FLASH_D),
                box(4.6, 0.1, 16.2, 11.4, 0.5, 25.4, {"*": W.GRAPHITE, "up": GULLY})]
    if shape == "inlet" and row == "lower":
        # The sprinkler inlet: a red cabinet on the plinth, its two hose couplings with caps on chains, its plate.
        out += [box(3.0, 3.0, 15.8, 13.0, 12.6, 17.8, FDC_RED), box(3.4, 3.4, 17.8, 12.6, 12.2, 18.1, FDC_RED)]
        for cx in (5.6, 10.4):
            out += W.round_z(cx, 7.0, 1.5, 18.1, 20.4, CHROME)
            out += W.round_z(cx, 7.0, 1.7, 20.4, 21.4, W.BLACK)
        out.append(box(3.6, 13.2, 15.8, 12.4, 15.6, 16.2, {"*": FDC_RED, "south": FDC_SIGN + "!"}))
    return out


def parapet(shape="plain"):
    """The parapet: anthracite panel outside, the membrane turned up its inside, the aluminium coping over the top
    with its drip legs both sides and a joint strap at each length's end."""
    if shape == "corner":
        return [box(0.0, 0.0, 0.0, 15.1, 15.0, 15.1, {"*": FLASH_D, "north": MEMBRANE, "west": MEMBRANE}),
                box(0.0, 0.5, 15.0, 15.0, 15.0, SKIN_Z, {"*": FLASH_D, "south": ANTHRACITE}),
                box(15.0, 0.5, 0.0, SKIN_Z, 15.0, 15.0, {"*": FLASH_D, "east": ANTHRACITE}),
                box(0.0, 0.0, 15.0, 15.0, 0.5, 15.4, SEALANT), box(15.0, 0.0, 0.0, 15.4, 0.5, 15.0, SEALANT),
                box(14.7, 0.0, 14.7, 16.4, 15.0, 16.4, FLASH_D),
                box(-0.9, 15.0, -0.9, 16.9, 16.0, 16.9, COPING),
                box(-0.9, 13.4, 16.3, 16.9, 15.0, 16.9, COPING), box(16.3, 13.4, -0.9, 16.9, 15.0, 16.3, COPING),
                box(-0.9, 13.8, -0.9, -0.3, 15.0, 16.3, COPING), box(-0.3, 13.8, -0.9, 16.3, 15.0, -0.3, COPING)]
    out = [box(0.0, 0.0, 0.0, 16.0, 15.0, 15.1, {"*": FLASH_D, "north": MEMBRANE}),
           box(0.0, 0.5, 15.0, 16.0, 15.0, SKIN_Z, {"*": FLASH_D, "south": ANTHRACITE}),
           box(0.0, 0.0, 15.0, 16.0, 0.5, 15.4, SEALANT),
           box(0.0, 15.0, -0.9, 16.0, 16.0, 16.9, COPING),
           box(0.0, 13.4, 16.3, 16.0, 15.0, 16.9, COPING), box(0.0, 13.8, -0.9, 16.0, 15.0, -0.3, COPING),
           box(0.0, 16.0, -1.0, 0.9, 16.2, 17.0, COPING)]
    if shape == "scupper":
        # The outlet through the upstand at roof level with its grate, the hopper head under it outside.
        out += [box(5.0, 0.4, -0.3, 11.0, 4.0, 0.1, GULLY),
                box(4.2, 0.6, 16.0, 11.8, 7.4, 21.6, FLASH_D), box(4.6, 7.4, 16.4, 11.4, 8.0, 21.2, FLASH_D)]
        out += PP.round_y(8.0, 19.0, 1.7, -0.01, 0.6, FLASH_D)
    return out


# ------------------------------------------------------------------------------------------------ roof and slabs
def soffit(girder=False):
    """Under a roof or a floor slab: the trapezoidal deck (ribs along x, four a block) and, on a column line, the
    steel girder under it (along z). Hangs from y 12.6 (the slab's underside) down."""
    out = [box(0.0, 11.8, 0.0, 16.0, 12.6, 16.0, DECK)]
    for z in (0.4, 4.4, 8.4, 12.4):
        out.append(box(0.0, 9.6, z, 16.0, 11.8, z + 2.2, DECK))
    if girder:
        out += [box(4.4, 8.9, 0.0, 11.6, 9.6, 16.0, STEEL), box(7.5, 0.7, 0.0, 8.5, 8.9, 16.0, STEEL),
                box(4.4, 0.0, 0.0, 11.6, 0.7, 16.0, STEEL)]
    return out


def roof_deck(girder=False):
    """The roof: membrane over the insulation board on the deck, the soffit underneath."""
    return [box(0.0, 12.6, 0.0, 16.0, 16.0, 16.0, {"*": MEMBRANE, "down": DECK})] + soffit(girder)


def _kerb(top, inner=2.0):
    """A rooflight's or vent's upstand: insulated kerb wrapped in membrane, the opening through the deck lined white."""
    out = [box(0.6, 12.6, 0.6, 15.4, top, 2.6, MEMBRANE), box(0.6, 12.6, 13.4, 15.4, top, 15.4, MEMBRANE),
           box(0.6, 12.6, 2.6, 2.6, top, 13.4, MEMBRANE), box(13.4, 12.6, 2.6, 15.4, top, 13.4, MEMBRANE)]
    # The roof round the kerb (its membrane) and the deck cut back to the opening, lined.
    out += [box(0.0, 12.6, 0.0, 16.0, 15.9, 0.6, {"*": SEALANT, "up": MEMBRANE, "down": DECK}),
            box(0.0, 12.6, 15.4, 16.0, 15.9, 16.0, {"*": SEALANT, "up": MEMBRANE, "down": DECK}),
            box(0.0, 12.6, 0.6, 0.6, 15.9, 15.4, {"*": SEALANT, "up": MEMBRANE, "down": DECK}),
            box(15.4, 12.6, 0.6, 16.0, 15.9, 15.4, {"*": SEALANT, "up": MEMBRANE, "down": DECK})]
    out += [box(2.6, 9.6, 2.4, 13.4, 12.6, 2.6, LINER), box(2.6, 9.6, 13.4, 13.4, 12.6, 13.6, LINER),
            box(2.4, 9.6, 2.6, 2.6, 12.6, 13.4, LINER), box(13.4, 9.6, 2.6, 13.6, 12.6, 13.4, LINER)]
    return out


def rooflight():
    """A flat rooflight on its kerb: the aluminium frame and the opal twin-wall glazing laid to a slight fall, so
    daylight comes down into the top floor."""
    out = _kerb(20.4)
    out += [box(0.4, 20.4, 0.4, 15.6, 21.0, 15.6, FLASH_D),
            box(1.0, 21.0, 1.0, 15.0, 21.6, 15.0, {"*": FLASH_D, "up": POLY, "down": POLY}),
            box(0.6, 21.0, 0.6, 15.4, 21.9, 1.0, FLASH_D), box(0.6, 21.0, 15.0, 15.4, 21.9, 15.4, FLASH_D),
            box(0.6, 21.0, 1.0, 1.0, 21.9, 15.0, FLASH_D), box(15.0, 21.0, 1.0, 15.4, 21.9, 15.0, FLASH_D)]
    return out


def smoke_vent(open_=False):
    """A natural smoke and heat exhaust vent: aluminium frame on its kerb, twin polycarbonate flaps hinged at the
    sides that stand up open (their rams out) when the fire system trips."""
    out = _kerb(20.4) + [box(0.4, 20.4, 0.4, 15.6, 21.4, 15.6, FLASH_D)]
    if not open_:
        out += [box(1.0, 21.4, 1.0, 7.9, 22.2, 15.0, {"*": FLASH, "up": POLY}),
                box(8.1, 21.4, 1.0, 15.0, 22.2, 15.0, {"*": FLASH, "up": POLY})]
        out += [box(3.0, 20.2, 15.6, 13.0, 21.4, 16.4, STEEL)]
    else:
        out += [box(0.4, 21.4, 1.0, 1.2, 28.3, 15.0, {"*": FLASH, "east": POLY, "west": POLY}),
                box(14.8, 21.4, 1.0, 15.6, 28.3, 15.0, {"*": FLASH, "east": POLY, "west": POLY}),
                box(3.0, 20.2, 15.6, 13.0, 21.4, 16.4, STEEL), box(1.2, 22.0, 15.2, 3.0, 23.0, 16.2, CHROME),
                box(13.0, 22.0, 15.2, 14.8, 23.0, 16.2, CHROME)]
    return out


def roof_hatch(open_=False):
    """The roof hatch over the stair tower's ladder: galvanised lid on its kerb, its hinges along +z, the grab handle;
    open, the lid stands up off its hinges (as far as a model may turn it: 45 degrees)."""
    out = _kerb(19.6) + [box(2.0, 19.0, 14.8, 4.0, 20.2, 15.8, STEEL), box(12.0, 19.0, 14.8, 14.0, 20.2, 15.8, STEEL)]
    if not open_:
        out += [box(0.2, 19.6, 0.2, 15.8, 21.0, 15.8, FLASH), box(6.0, 21.0, 0.8, 10.0, 22.4, 1.4, W.YELLOW)]
    else:
        rot = W.rot_x(8.0, 20.3, 15.8, 45)
        out += [box(0.2, 19.6, 0.2, 15.8, 21.0, 15.8, FLASH, rot), box(6.0, 21.0, 0.8, 10.0, 22.4, 1.4, W.YELLOW, rot)]
    return out


def door_canopy(part="single"):
    """A cantilevered aluminium canopy over a door, in the cell outside the wall (the wall at -z): its deck and
    fascia, the soffit with a downlight, and at its ends (part left, right or single) the tie rods up to the wall at
    45 degrees with their wall plates."""
    out = [box(0.0, 1.8, 0.0, 16.0, 2.6, 13.4, FLASH_D), box(0.0, 1.2, 13.2, 16.0, 3.4, 14.0, FLASH_D),
           box(0.0, 1.6, 0.4, 16.0, 1.8, 13.2, FLASH), box(6.4, 1.4, 6.0, 9.6, 1.6, 9.0, LENS_ON)]
    ends = {"single": (1.6, 14.4), "left": (1.6,), "right": (14.4,), "mid": ()}[part]
    L = 12.6 * math.sqrt(2.0)
    for x in ends:
        out.append(box(x - 0.3, 9.2 - 0.3, 6.3 - L / 2, x + 0.3, 9.2 + 0.3, 6.3 + L / 2, FLASH_D,
                       W.rot_x(x, 9.2, 6.3, 45)))
        out.append(box(x - 0.9, 14.6, -0.15, x + 0.9, 17.0, 0.35, FLASH_D))
    return out


def yard():
    return [box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0, {"*": PRECAST, "up": YARD})]


# ------------------------------------------------------------------------------------------------ stairs
# A flight rises 2/3 of a block for each block it runs (riser 1/6 block, tread 1/4: 167 mm and 250 mm, 33.7 degrees,
# a building-code stair): three blocks along, two up, then it repeats. Its cells: part 0 (column 0, the lower
# block), 1 (column 1, lower), 2 (column 1, upper), 3 (column 2, upper). A flight is two blocks wide (side left and
# right, as seen walking up). Model space: it climbs toward -z (placed facing north it climbs north); tread j of a
# flight (0..) runs from 4j to 4(j + 1) px past its foot with its top (j + 1) risers up, its nosing on its foot side.
RISE, RUN = 16.0 / 6.0, 4.0
STAIR_CELLS = ((0, 0), (1, 0), (1, 1), (2, 1))     # part: (column, level)
STRINGER_X = (0.25, 1.15)                           # the stringer plate across the lane's outer side (px, left side)
RAIL_UP = 14.4                                      # the handrail 0.9 m over the nosings
POST_EVERY = 6                                      # a guard post every six treads (1.5 m)


def stair_cell(part, side="left"):
    """The treads in one stair cell: steel pans with their concrete fill and the yellow nosing, the pan's lip under
    the nosing, each spanning the lane between its stringers."""
    c, lv = STAIR_CELLS[part]
    xa, xb = (STRINGER_X[1], 16.0) if side == "left" else (0.0, 16.0 - STRINGER_X[1])
    out = []
    for k in range(4):
        top = (4 * c + k + 1) * RISE - lv * 16.0
        if not 0.0 < top <= 16.0 + 1e-6:
            continue
        z0, z1 = 16.0 - (k + 1) * RUN, 16.0 - k * RUN
        out += [box(xa, top - 1.2, z0 - 0.3, xb, top - 0.1, z1 - 0.9, {"*": RAIL_PAINT, "up": TREAD}),
                box(xa, top - 0.25, z1 - 0.9, xb, top, z1 - 0.1, NOSING),
                box(xa, top - 1.7, z1 - 0.9, xb, top - 0.25, z1 - 0.1, RAIL_PAINT)]
    return out


def stair_collision(part):
    """The cell's collision boxes (blocks): each tread a step 1/4 block deep under its top, so it walks up and there is
    room under the flight."""
    c, lv = STAIR_CELLS[part]
    out = []
    for k in range(4):
        top = ((4 * c + k + 1) * RISE - lv * 16.0) / 16.0
        if not 0.0 < top <= 1.0 + 1e-6:
            continue
        z0, z1 = (16.0 - (k + 1) * RUN) / 16.0, (16.0 - k * RUN) / 16.0
        out.append((0.0, max(0.0, top - 0.25), z0, 1.0, top, z1))
    return out


def stair_landing(edge=False):
    """A steel landing: checker plate on its channel frame (two joists under each block); edge: the trimmer channel
    and the yellow nosing along its +z side, where the flights meet it."""
    out = [box(0.0, 15.4, 0.0, 16.0, 16.0, 16.0, {"*": RAIL_PAINT, "up": CHECKER}),
           box(3.6, 11.4, 0.0, 4.4, 15.4, 16.0, RAIL_PAINT), box(11.6, 11.4, 0.0, 12.4, 15.4, 16.0, RAIL_PAINT),
           box(2.6, 11.0, 0.0, 5.4, 11.4, 16.0, RAIL_PAINT), box(10.6, 11.0, 0.0, 13.4, 11.4, 16.0, RAIL_PAINT)]
    if edge:
        out += [box(0.0, 11.0, 15.2, 16.0, 15.4, 16.0, RAIL_PAINT), box(0.0, 15.75, 15.0, 16.0, 16.1, 15.9, NOSING)]
    return out


# What the stair's renderer draws (the parts at 33.7 degrees), in a flight's own frame: px, x across the lane (0 at
# its left side as seen walking up, 32 at its right), z from the foot (0) up the flight (toward -z), y from the
# foot's walking level. Each part is a prism: ("slab", tex, x0, x1, [(z, y) four corners]) or a round bar
# ("bar", tex, radius, (x, y, z) from, (x, y, z) to).


def _nose(j):
    """Tread j's nosing: (z, y) in the flight's frame."""
    return -RUN * j, RISE * (j + 1)


def flight_parts(treads, left="wall", right="guard", rail_ext=4.8):
    """The sloped parts of a flight of treads treads: on each side its stringer (a channel: the plate and its two
    flanges), and either a wall handrail on brackets (wall) or a guard: posts every POST_EVERY treads, the handrail
    and a knee rail (guard). The handrails run on 0.3 m level past both ends."""
    out = []
    slope = RISE / RUN
    zl = -(RISE * (treads - 1) - 1.2) / slope         # the stringer's top end: where its top edge meets the landing
    for side in ("left", "right"):
        kind = left if side == "left" else right
        xs = STRINGER_X if side == "left" else (32.0 - STRINGER_X[1], 32.0 - STRINGER_X[0])
        top = lambda z: RISE + (-z) * slope + 1.6
        bot = lambda z: RISE + (-z) * slope - 3.2
        z0 = 1.2
        out.append(("slab", RAIL_PAINT, xs[0], xs[1], [(z0, bot(z0)), (zl, bot(zl)), (zl, top(zl)), (z0, top(z0))]))
        fl0, fl1 = (xs[1], xs[1] + 1.2) if side == "left" else (xs[0] - 1.2, xs[0])
        for yo, yi in ((top, lambda z: top(z) - 0.5), (lambda z: bot(z) + 0.5, bot)):
            out.append(("slab", RAIL_PAINT, fl0, fl1, [(z0, yi(z0)), (zl, yi(zl)), (zl, yo(zl)), (z0, yo(z0))]))
        # The handrail over the nosings, level extensions at both ends.
        xr = (2.6 if kind == "wall" else 0.7) if side == "left" else (32.0 - (2.6 if kind == "wall" else 0.7))
        zf, yf = _nose(0)
        zt, yt = _nose(treads - 1)
        a = (xr, yf + RAIL_UP, zf + rail_ext)
        b = (xr, yf + RAIL_UP, zf)
        c = (xr, yt + RAIL_UP, zt)
        d = (xr, yt + RAIL_UP, zt - rail_ext)
        tex = RAIL_YELLOW
        out += [("bar", tex, 0.62, a, b), ("bar", tex, 0.62, b, c), ("bar", tex, 0.62, c, d)]
        if kind == "wall":
            # Brackets: a short arm from the wall to under the rail, every POST_EVERY treads.
            for j in range(2, treads, POST_EVERY):
                z, y = _nose(j)
                y += RAIL_UP - 1.0
                xw = 0.0 if side == "left" else 32.0
                out.append(("bar", RAIL_PAINT, 0.32, (xw, y, z), (xr, y, z)))
                out.append(("bar", RAIL_PAINT, 0.32, (xr, y, z), (xr, y + 0.5, z)))
        else:
            # The guard: posts from the stringer's top flange to the rail, the knee rail between.
            xp = xr
            for j in list(range(0, treads, POST_EVERY)) + [treads - 1]:
                z, y = _nose(j)
                out.append(("bar", RAIL_PAINT, 0.5, (xp, y - 1.4 + 1.6, z), (xp, y + RAIL_UP, z)))
            out.append(("bar", RAIL_PAINT, 0.45, (xp, yf + RAIL_UP / 2, zf), (xp, yt + RAIL_UP / 2, zt)))
    return out
