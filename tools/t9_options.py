"""Six concept options for the tier 9 drone (Superconducting Heavy Lifter), for Justin to choose from.

All in the tower's theme (graphene hull, dull red YBCO coil glow only), each at most one 5x5 pad wide and
under 5 blocks tall so it fits its large hangar, and each a different layout from tiers 4-8 (quad ducts,
tiltrotor, tandem, airship, flying wing). `python tools/t9_options.py` writes build/drones/t9_options.png.
"""
import math

from PIL import Image, ImageDraw, ImageFont

import drone_concepts as dc
import drone_portraits as dp

M = dc.M
RED = dc.DULL_RED


def ring_lifter(g):
    """A: Ring Lifter. A hexagonal ring hull with six lift fans set into it, a glowing coil band round
    the ring, and an armoured cargo pod slung in the open middle on three struts."""
    hull, dark = M["graphene"], M["graphene_dark"]
    for k in range(6):
        g.push()
        g.yaw(k * math.pi / 3)
        g.centred(0, 1.15, 1.75, 1.95, 0.42, 0.7, hull)
        g.centred(0, 1.4, 1.75, 1.7, 0.08, 0.5, dark)
        g.centred(0, 1.445, 1.75, 0.62, 0.01, 0.42, M["vent"])
        dp.rotor(g, 0, 1.44, 1.75, 0.27, k, M["blades5"])
        dp.glow(g, RED, lambda: g.centred(0, 1.05, 2.105, 1.9, 0.05, 0.02, M["conduit"]))
        g.pop()
    for k in range(3):
        g.push()
        g.yaw(k * 2 * math.pi / 3 + 0.5)
        g.push()
        g.translate(0, 1.0, 0.9).pitch(-0.75)
        g.centred(0, 0, 0, 0.08, 0.08, 1.2, M["steel"])
        g.pop()
        g.centred(0, 0.35, 1.65, 0.1, 0.7, 0.1, dark)
        g.centred(0, 0.02, 1.65, 0.28, 0.04, 0.28, M["frame"])
        g.pop()
    g.centred(0, 0.62, 0, 0.9, 0.5, 0.9, hull)
    g.centred(0, 0.9, 0, 0.6, 0.1, 0.6, dark)
    g.centred(0, 0.32, 0, 0.6, 0.1, 0.6, M["hazard"])
    dc.light(g, 0, 1.0, 0, 0.08)


def great_duct(g):
    """B: Great Duct. One huge armoured ducted fan, 4.4 blocks across, with stator vanes holding a long
    armoured fuselage underneath; a dull red coil ring round the duct lip; three landing legs."""
    hull, dark = M["graphene"], M["graphene_dark"]
    for k in range(16):
        g.push()
        g.yaw(k * math.pi / 8)
        g.centred(0, 1.5, 2.05, 0.86, 0.85, 0.22, hull)
        g.centred(0, 1.96, 2.05, 0.86, 0.08, 0.3, dark)
        dp.glow(g, RED, lambda: g.centred(0, 1.9, 2.17, 0.86, 0.06, 0.02, M["conduit"]))
        g.pop()
    for k in range(4):
        g.push()
        g.yaw(k * math.pi / 2 + math.pi / 4)
        g.centred(0, 1.45, 1.0, 0.1, 0.3, 2.0, dark)
        g.pop()
    dp.rotor(g, 0, 1.75, 0, 1.92, 0.2, M["blades6"])
    g.centred(0, 1.75, 0, 0.5, 0.3, 0.5, M["steel"])
    g.centred(0, 1.05, 0, 0.8, 0.6, 3.0, hull)
    g.centred(0, 1.0, 1.6, 0.6, 0.4, 0.3, dark)
    g.centred(0, 0.72, 1.55, 0.2, 0.18, 0.2, M["sensor"])
    g.centred(0, 0.72, -0.2, 0.6, 0.06, 1.4, M["hazard"])
    for k in range(3):
        g.push()
        g.yaw(k * 2 * math.pi / 3)
        g.push()
        g.translate(0, 0.9, 0.5).pitch(0.5)
        g.centred(0, -0.45, 0, 0.1, 0.95, 0.1, hull)
        g.pop()
        g.centred(0, 0.03, 0.92, 0.3, 0.06, 0.3, M["frame"])
        g.pop()
    dc.light(g, 0, 1.95, 0, 0.08)


def tri_fan(g):
    """C: Tri-Fan. Three big armoured ducted fans on a Y-shaped frame round a heavy central fuselage with
    a cargo cradle underneath; the coils glow on each duct rim."""
    hull, dark = M["graphene"], M["graphene_dark"]
    g.centred(0, 0.95, 0, 1.0, 0.7, 1.6, hull)
    g.centred(0, 1.36, 0, 0.6, 0.14, 1.1, dark)
    g.centred(0, 0.9, 0.9, 0.7, 0.5, 0.25, dark)
    g.centred(0, 0.62, 0.85, 0.2, 0.18, 0.2, M["sensor"])
    for k in range(3):
        g.push()
        g.yaw(k * 2 * math.pi / 3 + math.pi)
        g.centred(0, 1.1, 1.05, 0.22, 0.2, 1.3, dark)
        dc.duct(g, 0, 0.85, 1.75, 0.68, 0.5, 0.1, hull)

        def rim():
            g.box(-0.68, 1.35, 1.07, 0.68, 1.4, 1.13, M["conduit"])
            g.box(-0.68, 1.35, 2.37, 0.68, 1.4, 2.43, M["conduit"])
        dp.glow(g, RED, rim)
        dp.rotor(g, 0, 1.12, 1.75, 0.6, k, M["blades5"])
        g.centred(0, 1.1, 1.75, 0.18, 0.16, 0.18, M["steel"])
        g.centred(0, 0.42, 1.75, 0.1, 0.84, 0.1, M["frame"])
        g.centred(0, 0.02, 1.75, 0.3, 0.04, 0.3, M["frame"])
        g.pop()
    g.centred(0, 0.45, 0, 1.2, 0.08, 0.2, M["steel"])
    g.centred(0, 0.45, 0, 0.2, 0.08, 1.2, M["steel"])
    for gx, gz in ((-0.55, 0), (0.55, 0), (0, -0.55), (0, 0.55)):
        g.centred(gx, 0.28, gz, 0.08, 0.3, 0.08, dark)
        g.centred(gx, 0.12, gz, 0.14, 0.06, 0.14, M["hazard"])
    dc.light(g, 0, 1.48, -0.3, 0.07)


def box_wing(g):
    """D: Box-Wing Transport. A long armoured cargo fuselage with a joined box wing (low front wing, high
    rear wing, tip plates), lift fans under grilles in both wings, a belly cargo bay with a hazard edge."""
    hull, dark = M["graphene"], M["graphene_dark"]
    g.centred(0, 0.75, 0, 0.9, 0.7, 4.0, hull)
    g.centred(0, 1.15, -0.2, 0.55, 0.12, 3.0, dark)
    g.centred(0, 0.7, 2.08, 0.6, 0.45, 0.2, dark)
    g.centred(0, 0.42, 1.85, 0.2, 0.18, 0.2, M["sensor"])
    g.centred(0, 0.4, -0.3, 0.7, 0.03, 1.8, M["hazard"])
    g.centred(0, 0.68, 0.9, 4.4, 0.1, 0.8, hull)
    g.centred(0, 1.6, -1.3, 4.4, 0.1, 0.7, hull)
    for sx in (-1, 1):
        for y, z in ((0.9, 0.55), (1.1, 0.05), (1.32, -0.55), (1.5, -1.05)):
            g.centred(sx * 2.17, y, z, 0.08, 0.3, 0.55, dark)
        for fx in (0.9, 1.6):
            g.centred(sx * fx, 0.735, 0.9, 0.55, 0.01, 0.55, M["vent"])
            g.centred(sx * fx, 1.655, -1.3, 0.5, 0.01, 0.5, M["vent"])
        dp.glow(g, RED, lambda: g.centred(sx * 1.25, 0.66, 1.31, 1.7, 0.04, 0.02, M["conduit"]))
        dc.light(g, sx * 2.22, 1.65, -1.3, 0.06)
        g.centred(sx * 0.35, 0.2, 1.2, 0.08, 0.4, 0.08, M["frame"])
        g.centred(sx * 0.35, 0.2, -1.4, 0.08, 0.4, 0.08, M["frame"])
    g.centred(0, 1.6, -1.3, 0.5, 0.5, 0.3, dark)


def compound(g):
    """E: Coaxial Compound. A sleek armoured fuselage with stub wings, a coaxial pair of 5-blade rotors
    on a short mast, a ducted pusher fan at the tail for speed, and an internal cargo bay."""
    hull, dark = M["graphene"], M["graphene_dark"]
    g.centred(0, 0.85, 0.2, 0.9, 0.8, 3.0, hull)
    g.centred(0, 0.8, 1.85, 0.7, 0.6, 0.5, hull)
    g.centred(0, 0.75, 2.15, 0.44, 0.4, 0.2, dark)
    g.centred(0, 0.45, 1.9, 0.2, 0.18, 0.2, M["sensor"])
    g.centred(0, 0.95, -1.5, 0.5, 0.5, 0.8, hull)
    g.centred(0, 0.4, 0.2, 0.7, 0.03, 1.6, M["hazard"])
    g.centred(0, 0.9, 0.0, 2.6, 0.1, 0.6, dark)
    for sx in (-1, 1):
        dc.light(g, sx * 1.32, 0.92, 0.0, 0.06)
        g.centred(sx * 0.4, 0.2, 0.9, 0.08, 0.4, 0.08, M["frame"])
        g.centred(sx * 0.4, 0.2, -0.6, 0.08, 0.4, 0.08, M["frame"])
    g.centred(0, 1.42, 0.3, 0.6, 0.35, 0.9, dark)
    dp.glow(g, RED, lambda: g.centred(0, 1.65, 0.3, 0.4, 0.06, 0.4, M["conduit"]))
    g.centred(0, 1.8, 0.3, 0.16, 0.5, 0.16, M["steel"])
    dp.rotor(g, 0, 1.75, 0.3, 2.05, 0.0, M["blades5"])
    dp.rotor(g, 0, 1.98, 0.3, 2.0, 0.3, M["blades5"])
    g.push()
    g.translate(0, 1.0, -2.05).pitch(math.pi / 2)
    dc.duct(g, 0, -0.08, 0, 0.5, 0.16, 0.06, dark)
    dp.rotor(g, 0, 0, 0, 0.44, 0.4, M["blades5"])
    g.pop()
    g.centred(0, 1.45, -2.05, 0.06, 0.5, 0.3, dark)
    dc.light(g, 0, 2.1, 0.3, 0.07)


def side_by_side(g):
    """F: Twin-Coaxial Freighter. A wide transverse wing carrying a coaxial rotor pair at each tip (no
    tail rotor needed), with a heavy armoured freighter hull hung underneath and a rear loading ramp."""
    hull, dark = M["graphene"], M["graphene_dark"]
    g.centred(0, 0.75, 0, 1.0, 0.85, 3.6, hull)
    g.centred(0, 1.2, -0.1, 0.6, 0.1, 2.8, dark)
    g.centred(0, 0.7, 1.85, 0.75, 0.6, 0.2, dark)
    g.centred(0, 0.42, 1.75, 0.2, 0.18, 0.2, M["sensor"])
    g.centred(0, 0.55, -1.84, 0.8, 0.5, 0.08, dark)
    g.centred(0, 0.32, -1.85, 0.8, 0.05, 0.09, M["hazard"])
    g.centred(0, 1.6, 0.0, 3.0, 0.14, 0.7, hull)
    for sx in (-1, 1):
        g.push()
        g.translate(sx * 0.5, 1.4, 0).roll(sx * 0.9)
        g.centred(0, 0, 0, 0.08, 0.5, 0.5, dark)
        g.pop()
        g.centred(sx * 1.5, 1.7, 0, 0.42, 0.5, 0.6, dark)
        dp.glow(g, RED, lambda: g.centred(sx * 1.5, 1.98, 0, 0.36, 0.06, 0.36, M["conduit"]))
        g.centred(sx * 1.5, 2.1, 0, 0.12, 0.4, 0.12, M["steel"])
        dp.rotor(g, sx * 1.5, 2.05, 0, 1.05, sx * 0.2, M["blades4"])
        dp.rotor(g, sx * 1.5, 2.25, 0, 1.0, sx * 0.6, M["blades4"])
        dc.light(g, sx * 1.75, 1.6, 0.35, 0.06)
        for z in (1.1, -1.1):
            g.centred(sx * 0.45, 0.17, z, 0.12, 0.34, 0.2, M["frame"])
    dc.light(g, 0, 1.3, -0.9, 0.07)


OPTIONS = [("A", "Ring Lifter", "ring hull, six fans set in it, cargo pod in the middle", ring_lifter),
           ("B", "Great Duct", "one huge ducted fan over an armoured fuselage", great_duct),
           ("C", "Tri-Fan", "three big ducted fans on a Y frame", tri_fan),
           ("D", "Box-Wing Transport", "joined box wing, lift fans in the wings", box_wing),
           ("E", "Coaxial Compound", "coaxial rotors, stub wings, pusher fan", compound),
           ("F", "Twin-Coaxial Freighter", "coaxial rotor pair at each wing tip", side_by_side)]


def main():
    out = dp.ROOT / "build" / "drones"
    F = "/usr/share/fonts/truetype/dejavu/"
    big = ImageFont.truetype(F + "DejaVuSans-Bold.ttf", 36)
    mid = ImageFont.truetype(F + "DejaVuSans-Bold.ttf", 24)
    sm = ImageFont.truetype(F + "DejaVuSans.ttf", 17)
    cell, cols = 600, 3
    sheet = Image.new("RGB", (cell * cols + 40, 120 + 2 * (cell + 110)), (16, 12, 14))
    d = ImageDraw.Draw(sheet)
    d.text((20, 20), "TIER 9 — SUPERCONDUCTING HEAVY LIFTER: SIX OPTIONS", font=big, fill=(220, 80, 64))
    d.text((20, 68), "All graphene hull with dull red YBCO coil glow, at most one 5x5 pad wide and under 5 tall (fits its hangar). "
                     "Stats unchanged: 64 blocks/trip, 20 blocks/s.", font=sm, fill=(160, 140, 140))
    for i, (letter, name, text, fn) in enumerate(OPTIONS):
        g = dp.Geometry()
        fn(g)
        img = dp.render(g.quads, texture=dc.TEX, pitch_deg=20)
        x = 20 + (i % cols) * cell
        y = 110 + (i // cols) * (cell + 110)
        tile = img.resize((cell - 20, cell - 20), Image.LANCZOS)
        panel = Image.new("RGBA", tile.size, (74, 74, 78, 255))
        panel.alpha_composite(tile)
        panel.convert("RGB").save(out / f"t9_option_{letter}.png")
        sheet.paste(panel.convert("RGB"), (x, y))
        d.text((x + 10, y + cell - 8), f"{letter}  {name}", font=mid, fill=(236, 230, 230))
        d.text((x + 10, y + cell + 26), text, font=sm, fill=(200, 190, 190))
    sheet.save(out / "t9_options.png")
    print(out / "t9_options.png")


if __name__ == "__main__":
    main()
