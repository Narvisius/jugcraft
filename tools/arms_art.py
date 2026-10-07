"""The arms' art (docs/features/arms.md to arms-vi.md; restyled in docs/features/arms-restyle.md): one design a kind,
drawn by tools/arms_pixel.py as a crisp pixel-art inventory icon on the diagonal and as a 3D model in the hand.

Each design is along its own axis (s from the butt up, t across, negative on the lit side), about 28 design units for
each unit of the kind's in-hand size (tools/arms.py KINDS "held"), so every arm in the hand has about the same texel
size. Bronze arms are steampunk: a warm bronze blade, brass fittings, leather grips and oak hafts, a garnet set in the
ornate ones. Steel arms are kaiserpunk: blued steel, gunmetal fittings, black rubber grips, dark hafts, brass rivets and
a lit green phosphor stone. All original: after studying how Simply Swords, Epic Knights and RPG Style More Weapons make
weapons that read well (flat tones, one-pixel outlines, the 45-degree diagonal, chunky 3D parts), nothing of theirs is
copied.
"""
import math

from PIL import Image

import arms_pixel as px
from arms_pixel import DARK, HIGHLIGHT, LIGHT, MID, Design, STYLES

EMBER = px.EMBER
CHAIN = px.CHAIN


# ---------------------------------------------------------------- shared parts


def pommel(d, s, r, st, cap=True):
    """A round pommel at s, and a short cap below it."""
    d.disc(s, 0.0, r, st.fitting, depth=r * 1.4)
    if cap:
        d.strip(s - r - 0.8, s - r + 0.2, 0.6, material=st.fitting, depth=1.4)


def grip(d, s0, s1, w, st, period=2.0):
    """A wrapped grip from s0 to s1, `w` either side: the wrap's turns in the dark tone."""
    d.strip(s0, s1, w, material=st.grip, depth=w * 1.9, stripes=(period, DARK))


def haft(d, s0, s1, w, st, rings=()):
    """A wooden haft from s0 to s1, with metal rings at the given s."""
    d.strip(s0, s1, w, material=st.haft, depth=w * 2.0)
    for s in rings:
        d.strip(s - 0.6, s + 0.6, w + 0.4, material=st.fitting, depth=w * 2.0 + 0.6, z=1)


def guard(d, s, half, st, thick=1.4, curl=0.0):
    """A crossguard at s, `half` either side, with flared ends (swept towards the point by `curl`)."""
    d.strip(s - thick / 2, s + thick / 2, half, material=st.fitting, depth=2.6)
    for side in (1, -1):
        d.poly([(s - thick / 2 - 0.3, side * (half + 0.6)), (s + thick / 2 + 0.3 + curl, side * (half + 0.6 + curl * 0.3)),
                (s + thick / 2, side * (half - 1.0)), (s - thick / 2, side * (half - 1.0))], st.fitting, depth=2.6)


def gem(d, s, t, r, st):
    """A set stone in a metal bezel."""
    d.disc(s, t, r + 0.7, st.fitting, depth=2.8, z=2)
    d.disc(s, t, r, st.gem, depth=3.2, z=3)


def blade(d, s0, s1, w0, w1, st, tip=4.0, fuller=None, ridge=False, depth=1.0):
    """A straight double-edged blade from s0 to s1, `w0` either side at its root narrowing to `w1`, then a point of
    length `tip`. `fuller`: a groove (f0, f1), shares of the blade's length; `ridge`: a raised spine down the middle."""
    def width(s):
        return w0 + (w1 - w0) * (s - s0) / max(1e-6, s1 - s0)
    # Two faces meeting down the middle (lit and shaded), and a glint near the point on the lit edge.
    d.strip(s0, s1, width, material=st.blade, depth=depth, part="blade", bevel=0.0)
    d.poly([(s1, -w1), (s1 + tip, 0.0), (s1, w1)], st.blade, depth=depth, part="blade", bevel=0.0)
    d.glint(s1 - 0.5, -w1 * 0.55)
    if fuller:
        f0, f1 = fuller
        a, b = s0 + (s1 - s0) * f0, s0 + (s1 - s0) * f1
        d.strip(a, b, 0.45, material=st.blade, depth=depth * 0.7, tone=DARK, z=1)
    if ridge:
        d.strip(s0, s1 + tip * 0.6, lambda s: 0.45 if s < s1 else 0.45 * (1 - (s - s1) / (tip * 0.6 + 1e-6)),
                material=st.blade, depth=depth * 1.6, z=1, part="ridge")


def curved(d, s0, s1, w, bend, st, tip=3.0, edge=True, depth=1.0):
    """A curved single-edged blade from s0 to s1, `w` either side of a centre line that sweeps back by bend(s); the
    edge (right) bright as tempered steel, the back a shade darker; a point of length `tip` swept back with it."""
    d.strip(s0, s1, lambda s: w + bend(s), lambda s: w - bend(s), material=st.blade, depth=depth, part="blade")
    e = bend(s1)
    d.poly([(s1, -w - e), (s1 + tip, -w * 0.4 - e * 1.3), (s1, w - e)], st.blade, depth=depth, part="blade")
    if edge:
        d.strip(s0 + 0.8, s1 - 0.5, lambda s: -0.25 + bend(s), lambda s: w - bend(s), material=st.blade, depth=depth, z=1,
                tone=HIGHLIGHT, part="edge")
        d.strip(s0 + 0.8, s1 - 0.5, lambda s: w + bend(s), lambda s: -0.45 * w - bend(s), material=st.blade,
                depth=depth * 1.3, z=1, tone=MID, part="back")


def socket(d, s0, s1, w, st):
    """A metal socket binding a head to its haft."""
    d.strip(s0, s1, w, material=st.fitting, depth=w * 2.0 + 0.4)
    d.strip(s0, s0 + 0.8, w + 0.4, material=st.fitting, depth=w * 2.0 + 0.8, z=1)


def rivets(d, points, st):
    for s, t in points:
        d.disc(s, t, 0.55, st.accent, depth=2.0, z=4, tone=LIGHT)


def sickle(d, cs, ct, r, w, a0, a1, st, steps=12):
    """A sickle's or scythe's curved blade: along a circle about (cs, ct) of radius r from angle a0 to a1 (degrees, 0
    along +s, 90 along +t), `w` wide at its root narrowing to a point."""
    outer, inner, edge = [], [], []
    for i in range(steps + 1):
        f = i / steps
        a = math.radians(a0 + (a1 - a0) * f)
        # Broad most of the way, sweeping in to the point over the last third.
        width = w * (1 - f ** 2.2) + 0.45
        outer.append((cs + math.cos(a) * r, ct + math.sin(a) * r))
        inner.append((cs + math.cos(a) * (r - width), ct + math.sin(a) * (r - width)))
        edge.append((cs + math.cos(a) * (r - width + 0.7), ct + math.sin(a) * (r - width + 0.7)))
    d.poly(outer + inner[::-1], st.blade, depth=1.0, part="blade")
    d.poly(edge + inner[::-1], st.blade, depth=1.0, z=1, tone=HIGHLIGHT)


# ---------------------------------------------------------------- batch 42


def longsword(st):
    d = Design(36, grip=5.5)
    pommel(d, 1.7, 1.6, st)
    grip(d, 2.6, 8.6, 0.95, st)
    guard(d, 9.3, 5.5, st, curl=0.6)
    blade(d, 10.0, 31.5, 1.55, 1.25, st, tip=4.0, fuller=(0.04, 0.72))
    d.disc(9.3, 0.0, 0.8, st.accent, depth=3.0, z=3)
    return d


def greatsword(st):
    d = Design(48, grip=7.0)
    pommel(d, 1.8, 1.8, st)
    grip(d, 3.0, 12.5, 1.05, st)
    guard(d, 13.3, 7.5, st, thick=1.6, curl=1.0)
    d.strip(14.1, 18.5, 1.9, material=st.blade, depth=1.3, part="blade")   # the ricasso
    for side in (1, -1):
        d.poly([(17.6, side * 1.9), (19.2, side * 3.6), (19.8, side * 2.6), (18.6, side * 1.9)], st.fitting, depth=2.0)
    blade(d, 18.5, 43.0, 2.3, 1.8, st, tip=4.6, fuller=(0.02, 0.6))
    d.disc(13.3, 0.0, 0.9, st.accent, depth=3.0, z=3)
    return d


def rapier(st):
    d = Design(34, grip=5.0)
    pommel(d, 1.6, 1.4, st)
    grip(d, 2.4, 7.8, 0.85, st, period=1.6)
    # The swept hilt, kept plain: quillons and a knuckle bow standing clear of the grip.
    guard(d, 8.5, 3.4, st, thick=1.0, curl=0.8)
    d.line(8.2, -3.8, 2.8, -3.8, 0.8, st.fitting, depth=1.6)
    d.line(2.8, -3.8, 1.6, -1.2, 0.8, st.fitting, depth=1.6)
    blade(d, 9.0, 31.0, 0.75, 0.55, st, tip=3.0, ridge=True)
    return d


def flanged_mace(st):
    d = Design(32, grip=5.5)
    d.disc(1.0, 0.0, 1.3, st.fitting, depth=2.4)
    haft(d, 1.0, 22.0, 0.85, st)
    grip(d, 1.8, 9.8, 1.0, st)
    socket(d, 19.5, 22.0, 1.3, st)
    # Flanges to either side (and, in the model, before and behind: the core is as deep as they are wide).
    for side in (1, -1):
        d.poly([(22.0, side * 1.3), (23.0, side * 4.4), (28.5, side * 4.4), (30.5, side * 1.3)], st.blade, depth=1.2,
               part=f"flange{side}")
    d.strip(21.5, 30.5, 1.3, material=st.blade, depth=6.0, part="core")
    d.poly([(30.5, -1.3), (32.0, 0.0), (30.5, 1.3)], st.blade, depth=2.6, part="core")
    return d


def war_hammer(st):
    d = Design(38, grip=6.0)
    d.disc(1.0, 0.0, 1.3, st.fitting, depth=2.4)
    haft(d, 1.0, 32.0, 0.85, st)
    grip(d, 1.8, 10.5, 1.0, st)
    socket(d, 25.0, 32.0, 1.2, st)
    # The head: a squared face to the right, a tapering beak behind, a top spike.
    d.poly([(27.0, 1.2), (33.5, 1.2), (34.0, 6.6), (26.5, 6.6)], st.blade, depth=4.0, part="face")
    d.poly([(28.0, -1.2), (32.5, -1.2), (29.6, -7.0)], st.blade, depth=2.0, part="beak")
    d.poly([(32.0, -1.0), (37.5, 0.0), (32.0, 1.0)], st.blade, depth=1.8)
    return d


def glaive(st):
    d = Design(56, grip=12.0)
    d.disc(0.9, 0.0, 1.1, st.fitting, depth=2.2)
    haft(d, 0.8, 41.0, 0.8, st, rings=(22.0,))
    grip(d, 7.0, 17.0, 0.95, st)
    socket(d, 37.0, 41.0, 1.15, st)
    # A long knife blade, its edge curving out on the right and back to the point.
    d.poly([(40.5, -1.1), (41.0, 1.1), (45.0, 2.6), (50.0, 2.8), (54.0, 1.6), (56.0, -0.3), (52.0, -1.2), (46.0, -1.4)],
           st.blade, depth=1.0, part="blade")
    d.poly([(41.0, 1.1), (45.0, 2.6), (50.0, 2.8), (54.0, 1.6), (55.0, 0.8), (50.0, 1.8), (45.0, 1.7)], st.blade,
           depth=1.0, z=1, tone=HIGHLIGHT)
    d.strip(42.0, 53.0, 0.4, material=st.blade, depth=1.6, z=1, part="ridge")
    d.poly([(41.0, -1.1), (42.5, -3.4), (43.2, -1.1)], st.blade, depth=1.0)   # a back spur
    return d


def halberd(st):
    d = Design(59, grip=13.0)
    d.disc(0.9, 0.0, 1.1, st.fitting, depth=2.2)
    haft(d, 0.8, 44.0, 0.8, st, rings=(24.0,))
    grip(d, 8.0, 18.0, 0.95, st)
    socket(d, 37.0, 47.0, 1.15, st)
    d.poly([(46.0, -1.2), (59.0, 0.0), (46.0, 1.2)], st.blade, depth=1.4)   # the spike
    # The axe blade on the right: a long edge between two horns, drawn large enough to read.
    d.poly(HALBERD_AXE, st.blade, depth=1.1, part="axe")
    d.poly(HALBERD_EDGE, st.blade, depth=1.1, z=1, tone=HIGHLIGHT)
    d.poly(HALBERD_HOOK, st.blade, depth=1.1, part="hook")   # the back hook
    return d


# The halberd's head (shared with its variants).
HALBERD_AXE = [(38.0, 1.15), (48.0, 1.15), (51.0, 8.4), (44.0, 7.2), (36.0, 8.4)]
HALBERD_EDGE = [(51.0, 8.4), (44.0, 7.2), (36.0, 8.4), (37.0, 6.8), (44.0, 5.8), (49.6, 6.8)]
HALBERD_HOOK = [(41.5, -1.15), (45.0, -1.15), (49.0, -6.4), (46.6, -7.0), (42.8, -4.0)]


def spear(st):
    d = Design(28, grip=7.0)
    d.disc(0.7, 0.0, 0.8, st.fitting, depth=1.8)
    haft(d, 0.6, 20.5, 0.7, st, rings=(13.0,))
    grip(d, 4.5, 10.0, 0.85, st, period=1.6)
    socket(d, 19.0, 21.5, 0.95, st)
    d.poly([(21.0, -0.9), (22.5, -1.9), (26.0, -0.9), (28.0, 0.0), (26.0, 0.9), (22.5, 1.9), (21.0, 0.9)], st.blade,
           depth=1.0, part="blade")
    d.strip(21.5, 26.5, 0.35, material=st.blade, depth=1.6, z=1, part="ridge")
    return d


def lance(st):
    d = Design(36, grip=9.0)
    d.disc(1.0, 0.0, 1.0, st.fitting, depth=2.0)
    d.strip(1.0, 8.0, 0.9, material=st.haft, depth=1.8)
    grip(d, 8.0, 11.0, 1.0, st, period=1.5)
    # The vamplate: a cone guard over the hand; then the long fluted shaft tapering to a steel point.
    d.poly([(11.0, -0.9), (12.4, -3.6), (13.6, -3.6), (13.6, 3.6), (12.4, 3.6), (11.0, 0.9)], st.fitting, depth=3.0)
    d.strip(13.6, 31.0, lambda s: 1.9 - 1.2 * (s - 13.6) / 17.4, material=st.haft, depth=2.4, stripes=(3.0, MID))
    d.poly([(30.5, -0.8), (36.0, 0.0), (30.5, 0.8)], st.blade, depth=1.6)
    d.strip(30.0, 31.0, 0.9, material=st.fitting, depth=2.0, z=1)
    return d


# ---------------------------------------------------------------- Arms II (batch 45)


def dagger(st):
    d = Design(24, grip=3.8)
    pommel(d, 1.3, 1.2, st, cap=False)
    grip(d, 2.0, 6.0, 0.8, st, period=1.4)
    guard(d, 6.6, 3.0, st, thick=1.1)
    blade(d, 7.2, 20.0, 1.25, 0.95, st, tip=3.6, ridge=True)
    return d


def sabre(st):
    d = Design(34, grip=4.8)
    pommel(d, 1.4, 1.2, st, cap=False)
    grip(d, 2.2, 7.6, 0.85, st, period=1.6)
    # A D-guard: from the crosspiece round the knuckles (the lit side) to the pommel.
    d.strip(7.8, 8.8, 4.0, 1.8, material=st.fitting, depth=2.2)
    d.line(8.3, -3.6, 2.6, -3.6, 0.8, st.fitting, depth=1.6)
    d.line(2.6, -3.6, 1.4, -1.0, 0.8, st.fitting, depth=1.6)
    curved(d, 8.8, 30.5, 1.15, lambda s: 0.006 * (s - 8.8) ** 2, st, tip=3.2)
    return d


def estoc(st):
    d = Design(38, grip=6.0)
    pommel(d, 1.6, 1.5, st)
    grip(d, 2.5, 9.6, 0.9, st)
    guard(d, 10.3, 4.6, st, curl=0.8)
    d.disc(10.3, 0.0, 0.8, st.accent, depth=3.0, z=3)
    # A stiff thrusting blade: narrow, with a bright ridge and a long point.
    blade(d, 11.0, 32.0, 1.0, 0.8, st, tip=5.5, ridge=True)
    return d


def battle_axe(st):
    d = Design(42, grip=7.5)
    d.disc(1.0, 0.0, 1.3, st.fitting, depth=2.4)
    haft(d, 1.0, 41.0, 0.85, st)
    grip(d, 1.8, 12.0, 1.0, st)
    socket(d, 31.5, 39.0, 1.2, st)
    # A bearded bit to the right, its edge a long arc; a short spike behind.
    d.poly([(38.0, 1.2), (41.5, 4.5), (41.0, 9.6), (37.0, 11.8), (31.0, 11.5), (26.5, 9.0), (30.5, 6.0), (31.5, 1.2)],
           st.blade, depth=1.2, part="bit")
    d.poly([(41.2, 8.8), (41.0, 9.6), (37.0, 11.8), (31.0, 11.5), (27.4, 9.6), (31.0, 10.6), (37.0, 10.8)], st.blade,
           depth=1.1, tone=HIGHLIGHT, z=1)
    d.poly([(37.0, -1.2), (39.0, -4.6), (36.0, -3.8), (34.0, -1.2)], st.blade, depth=1.2)
    return d


def flail_handle(d, st):
    """The flail's handle: pommel, haft, grip, collar and the eye its chain hangs from (FLAIL_EYE). In the hand this is
    all the model is; the chain and ball are drawn live, swinging, by client/arms/FlailHeads.java (tools/arms_heads.py)."""
    d.disc(1.0, 0.0, 1.2, st.fitting, depth=2.4)
    haft(d, 1.0, 14.0, 0.85, st)
    grip(d, 1.8, 9.8, 1.0, st)
    d.strip(13.5, 15.5, 1.1, material=st.fitting, depth=2.6)
    d.ring(FLAIL_EYE, 0.0, 1.1, 0.45, st.fitting, depth=1.2, part="eye")
    return d


# Where the flail's chain hangs from its handle: the eye, along the haft (design units).
FLAIL_EYE = 16.0


def flail(st):
    """The flail as it was drawn whole, chain slung out and ball beside: the layout (icon fit, grip, the hand's size)
    is still worked out from it, so the handle is held exactly where it always was."""
    d = Design(34, grip=5.5)
    d.disc(1.0, 0.0, 1.2, st.fitting, depth=2.4)
    haft(d, 1.0, 14.0, 0.85, st)
    grip(d, 1.8, 9.8, 1.0, st)
    d.strip(13.5, 15.5, 1.1, material=st.fitting, depth=2.6)
    # The chain, slung out to the right, and the spiked ball.
    for i, (s, t) in enumerate([(16.0, 0.6), (17.6, 1.6), (19.0, 2.8), (20.6, 3.6), (22.2, 4.0)]):
        d.ring(s, t, 0.95, 0.35, CHAIN, depth=0.9 if i % 2 else 1.6, part=f"link{i}")
    for a in range(0, 360, 60):
        r = math.radians(a)
        d.poly([(26.6 + math.cos(r) * 3.0 - math.sin(r) * 0.8, 4.2 + math.sin(r) * 3.0 + math.cos(r) * 0.8),
                (26.6 + math.cos(r) * 5.2, 4.2 + math.sin(r) * 5.2),
                (26.6 + math.cos(r) * 3.0 + math.sin(r) * 0.8, 4.2 + math.sin(r) * 3.0 - math.cos(r) * 0.8)],
               st.blade, depth=1.6, part="spike")
    d.disc(26.6, 4.2, 3.6, st.blade, depth=6.0, z=1, part="ball")
    return d


def scythe(st):
    d = Design(53, grip=10.0)
    d.disc(0.9, 0.0, 1.1, st.fitting, depth=2.2)
    haft(d, 0.8, 50.0, 0.8, st)
    grip(d, 6.0, 14.0, 0.95, st)
    d.line(25.0, -0.8, 25.0, -3.6, 1.1, st.haft, depth=1.8)   # the nib, a second handle
    d.strip(47.5, 50.5, 1.1, material=st.fitting, depth=2.4)
    # The long curved blade, from the top of the snath out to the left and down to its point.
    d.strip(49.0, 51.5, 2.2, 0.9, material=st.fitting, depth=2.4)
    sickle(d, 40.0, -1.0, 11.5, 4.2, 0.0, -112.0, st, steps=16)
    return d


def quarterstaff(st):
    d = Design(50, grip=25.0)
    d.strip(0.0, 50.0, 1.0, material=st.haft, depth=2.0)
    # Iron-shod ends, each with a stud, and bands along the staff.
    for s0, s1 in ((0.0, 3.0), (47.0, 50.0)):
        d.strip(s0, s1, 1.3, material=st.fitting, depth=2.6, z=1)
        rivets(d, [((s0 + s1) / 2, 0.0)], st)
    for s in (5.0, 11.0, 39.0, 45.0):
        d.strip(s - 0.4, s + 0.4, 1.15, material=st.fitting, depth=2.4, z=1)
    grip(d, 20.0, 30.0, 1.15, st)
    return d


def pike(st):
    d = Design(64, grip=16.0)
    d.disc(0.8, 0.0, 1.0, st.fitting, depth=2.0)
    haft(d, 0.8, 54.0, 0.75, st, rings=(30.0,))
    grip(d, 10.0, 22.0, 0.9, st)
    # Langets (iron strips down the haft below the socket), then a broad leaf head.
    d.strip(46.0, 52.0, 0.9, material=st.fitting, depth=1.9, z=1)
    socket(d, 52.0, 55.5, 1.05, st)
    d.poly([(55.0, -1.0), (57.0, -2.4), (61.0, -1.1), (64.0, 0.0), (61.0, 1.1), (57.0, 2.4), (55.0, 1.0)], st.blade,
           depth=1.0, part="blade", bevel=0.0)
    d.strip(55.5, 62.0, 0.35, material=st.blade, depth=1.5, z=1, part="ridge")
    d.glint(59.0, -1.0)
    return d


# ---------------------------------------------------------------- Arms III (batch 46)


def zweihander(st):
    d = Design(59, grip=9.0)
    pommel(d, 1.9, 1.9, st)
    grip(d, 3.2, 15.5, 1.1, st)
    guard(d, 16.3, 8.0, st, thick=1.7, curl=1.4)
    d.strip(17.1, 23.0, 1.7, material=st.blade, depth=1.4, part="blade")   # the ricasso, leather-bound
    d.strip(17.5, 21.0, 1.9, material=st.grip, depth=2.6, z=1, stripes=(1.6, DARK))
    for side in (1, -1):
        d.poly([(22.4, side * 1.7), (24.2, side * 4.0), (24.8, side * 2.8), (23.4, side * 1.7)], st.fitting, depth=2.2)
    blade(d, 23.0, 54.0, 2.4, 1.9, st, tip=5.0, fuller=(0.02, 0.55))
    d.disc(16.3, 0.0, 1.0, st.accent, depth=3.2, z=3)
    return d


def maul(st):
    d = Design(48, grip=8.0)
    d.disc(1.0, 0.0, 1.3, st.fitting, depth=2.6)
    haft(d, 1.0, 37.0, 0.95, st, rings=(19.0,))
    grip(d, 2.0, 13.0, 1.1, st)
    socket(d, 34.0, 37.5, 1.4, st)
    # A great block of a head, banded.
    d.strip(37.0, 47.5, 6.6, material=st.blade, depth=7.0, part="head")
    for s in (38.2, 46.3):
        d.strip(s - 0.6, s + 0.6, 6.9, material=st.fitting, depth=7.4, z=1, part=f"band{s}")
    return d


def executioner(st):
    d = Design(53, grip=8.0)
    pommel(d, 1.8, 1.8, st)
    grip(d, 3.0, 13.5, 1.1, st)
    d.strip(13.5, 15.2, 4.4, material=st.fitting, depth=2.8)   # a straight, short crossguard
    # A broad, parallel blade, square at the end, a fuller near the back and three holes in it.
    d.strip(15.2, 50.5, 3.4, material=st.blade, depth=1.1, part="blade")
    d.poly([(50.5, -3.4), (52.5, -2.6), (52.5, 3.4), (50.5, 3.4)], st.blade, depth=1.1, part="blade")
    d.strip(17.0, 40.0, 2.2, -1.4, material=st.blade, depth=0.8, tone=DARK, z=1)
    for s in (20.0, 27.0, 34.0):
        d.disc(s, 2.6, 0.6, st.blade, depth=0.4, tone=px.OUT_DARK, z=2)
    return d


def bill(st):
    d = Design(62, grip=14.0)
    d.disc(0.9, 0.0, 1.1, st.fitting, depth=2.2)
    haft(d, 0.8, 46.0, 0.8, st, rings=(26.0,))
    grip(d, 8.5, 19.0, 0.95, st)
    socket(d, 41.0, 47.0, 1.15, st)
    # A hooked blade: a cleaver edge forward (right) sweeping up into a hook, and a spike.
    d.poly(BILL_BLADE, st.blade, depth=1.1, part="blade")
    d.poly([(52.5, -0.9), (62.0, 0.0), (52.5, 0.9)], st.blade, depth=1.3)
    return d


# The bill's head (shared with its variants).
BILL_BLADE = [(45.5, -1.15), (45.5, 1.15), (47.6, 5.4), (53.0, 6.0), (59.0, 7.6), (58.6, 5.0), (55.0, 2.6), (53.0, 0.0),
              (53.0, -1.15)]


# ---------------------------------------------------------------- Arms IV (batch 47): ornate, each with a set stone


def labrys(st):
    d = Design(45, grip=7.5)
    d.disc(1.0, 0.0, 1.3, st.fitting, depth=2.4)
    haft(d, 1.0, 42.0, 0.9, st)
    grip(d, 1.8, 13.0, 1.05, st)
    socket(d, 32.0, 42.0, 1.3, st)
    # Two crescent bits, mirror images, flaring wide at their edges.
    for side in (1, -1):
        d.poly([(33.0, side * 1.3), (31.0, side * 4.0), (28.0, side * 9.5), (32.0, side * 11.6), (37.0, side * 12.0),
                (42.0, side * 11.6), (45.0, side * 9.5), (42.0, side * 4.0), (40.0, side * 1.3)], st.blade, depth=1.2,
               part=f"bit{side}")
        d.poly([(28.0, side * 9.5), (32.0, side * 11.6), (37.0, side * 12.0), (42.0, side * 11.6), (45.0, side * 9.5),
                (42.0, side * 10.4), (37.0, side * 10.8), (32.0, side * 10.4)], st.blade, depth=1.1, z=1, tone=HIGHLIGHT)
    gem(d, 36.5, 0.0, 1.2, st)
    return d


def battleblade(st):
    d = Design(50, grip=7.5)
    pommel(d, 1.8, 1.8, st)
    grip(d, 3.0, 12.5, 1.1, st)
    guard(d, 13.3, 6.0, st, thick=1.8, curl=1.6)
    # A great cleaver: a broad blade, straight edge (right), a saw-toothed back (left).
    d.strip(14.2, 46.0, 3.6, 2.6, material=st.blade, depth=1.2, part="blade")
    d.poly([(46.0, -3.6), (49.5, -1.4), (49.5, 2.6), (46.0, 2.6)], st.blade, depth=1.2, part="blade")
    for k in range(9):
        s = 17.0 + k * 3.3
        d.poly([(s, -3.6), (s + 1.2, -5.0), (s + 2.2, -3.6)], st.blade, depth=1.0, part="teeth")
    d.strip(15.0, 44.0, 0.5, material=st.blade, depth=1.7, z=1, part="spine")
    gem(d, 13.3, 0.0, 1.0, st)
    return d


def war_fork(st):
    d = Design(62, grip=14.0)
    d.disc(0.9, 0.0, 1.1, st.fitting, depth=2.2)
    haft(d, 0.8, 49.0, 0.8, st, rings=(26.0,))
    grip(d, 8.5, 19.0, 0.95, st)
    socket(d, 45.0, 50.0, 1.2, st)
    # Three barbed tines from a crossbar.
    d.strip(49.5, 51.5, 5.4, material=st.fitting, depth=2.6)
    for t in (-4.4, 0.0, 4.4):
        top = 59.0 if t == 0 else 57.0
        d.poly([(51.5, t - 0.65), (top, t - 0.65), (top, t - 0.9), (top + 3.0, t), (top, t + 0.9), (top, t + 0.65),
                (51.5, t + 0.65)], st.blade, depth=1.6, part=f"tine{t}")
        d.poly([(top - 0.4, t + 0.65), (top - 1.8, t + 1.9), (top - 2.2, t + 0.65)], st.blade, depth=1.4)
    gem(d, 50.5, 0.0, 0.9, st)
    return d


def kama(st):
    d = Design(27, grip=4.5)
    d.disc(0.8, 0.0, 1.0, st.fitting, depth=2.0)
    haft(d, 0.8, 18.0, 0.8, st)
    grip(d, 1.5, 9.5, 0.95, st, period=1.6)
    d.strip(16.5, 19.0, 1.05, material=st.fitting, depth=2.4)
    # A sickle blade from the top of the handle, swept out to the right and down.
    sickle(d, 12.0, 0.6, 7.6, 3.0, 0.0, 125.0, st, steps=12)
    gem(d, 17.8, 0.0, 0.7, st)
    return d


def war_pick(st):
    d = Design(31, grip=5.0)
    d.disc(0.9, 0.0, 1.1, st.fitting, depth=2.2)
    haft(d, 0.8, 26.0, 0.85, st)
    grip(d, 1.6, 9.0, 1.0, st, period=1.6)
    socket(d, 21.0, 27.5, 1.2, st)
    # A long down-curved beak (right), a small hammer face behind (left), a top spike.
    d.poly([(25.0, 1.2), (26.5, 1.2), (25.5, 6.0), (22.0, 10.5), (21.0, 10.0), (23.0, 5.5)], st.blade, depth=1.6,
           part="beak")
    d.strip(23.0, 26.5, 4.0, -1.2, material=st.blade, depth=3.4, part="face")
    d.poly([(27.0, -0.8), (31.0, 0.0), (27.0, 0.8)], st.blade, depth=1.6)
    return d


# ---------------------------------------------------------------- Arms V (batch 48)


def twinblade(st):
    d = Design(48, grip=24.0)
    grip(d, 18.0, 30.0, 1.05, st)
    for sign in (1, -1):
        g = 24.0 + sign * 6.6
        d.strip(min(g, g + sign * 1.4), max(g, g + sign * 1.4), 3.6, material=st.fitting, depth=2.6, part=f"guard{sign}")
        b0, b1 = g + sign * 1.4, 24.0 + sign * 21.0
        d.strip(min(b0, b1), max(b0, b1), 1.4, material=st.blade, depth=1.0, part=f"blade{sign}")
        d.poly([(b1, -1.4), (b1 + sign * 3.0, -0.2 * sign), (b1, 1.4)], st.blade, depth=1.0, part=f"blade{sign}")
        r0, r1 = b0, b1 - sign * 2.0
        d.strip(min(r0, r1), max(r0, r1), 0.4, material=st.blade, depth=1.5, z=1, part=f"ridge{sign}")
    gem(d, 24.0, 0.0, 0.8, st)
    return d


def nodachi(st):
    d = Design(56, grip=9.0)
    d.strip(0.0, 1.4, 1.1, material=st.fitting, depth=2.4)   # the kashira
    grip(d, 1.4, 16.0, 1.05, st, period=1.8)
    d.disc(16.8, 0.0, 3.0, st.fitting, depth=2.4)            # the tsuba
    d.strip(17.6, 18.8, 1.4, material=st.fitting, depth=2.2, z=1)   # the habaki
    curved(d, 18.8, 52.5, 1.5, lambda s: 0.0016 * (s - 18.8) ** 2, st, tip=3.4)
    gem(d, 16.8, 0.0, 0.8, st)
    return d


def earthbreaker(st):
    d = Design(50, grip=8.0)
    d.disc(1.0, 0.0, 1.4, st.fitting, depth=2.6)
    haft(d, 1.0, 38.0, 1.0, st, rings=(20.0,))
    grip(d, 2.0, 13.5, 1.15, st)
    socket(d, 34.0, 38.0, 1.5, st)
    # A siege hammer: a huge drum head, ringed, flanged at both faces.
    d.strip(38.0, 49.5, 5.8, material=st.blade, depth=6.4, part="head")
    for s in (38.0, 49.5):
        d.strip(s - 0.8, s + 0.8, 7.4, material=st.fitting, depth=7.6, z=1, part=f"flange{s}")
    d.strip(42.5, 45.0, 6.4, material=st.fitting, depth=7.0, z=1)
    gem(d, 43.75, 0.0, 1.0, st)
    return d


def katar(st):
    d = Design(24, grip=4.0)
    # An H frame: two side bars and a cross grip, the blade rising from the top bar.
    d.poly([(0.0, -4.1), (8.0, -4.1), (8.0, -2.7), (0.0, -2.7)], st.fitting, depth=1.8, part="frame")
    d.poly([(0.0, 2.7), (8.0, 2.7), (8.0, 4.1), (0.0, 4.1)], st.fitting, depth=1.8, part="frame")
    d.strip(3.2, 5.0, 2.7, material=st.grip, depth=1.8, part="crossgrip")
    d.strip(7.6, 9.2, 4.1, material=st.fitting, depth=2.4, part="top")
    d.poly([(9.2, -2.6), (24.0, 0.0), (9.2, 2.6)], st.blade, depth=1.1, part="blade")
    d.strip(9.2, 21.0, lambda s: 0.5 * (1 - (s - 9.2) / 14), material=st.blade, depth=1.7, z=1, part="ridge")
    return d


def moonblade(st):
    d = Design(50, grip=7.0)
    pommel(d, 1.7, 1.6, st)
    grip(d, 2.8, 12.0, 1.05, st)
    guard(d, 12.8, 3.8, st, curl=1.0)
    # A crescent greatsword: the blade swells into a moon's curve on its edge (right) and narrows to the point.
    d.poly([(13.6, -1.6), (30.0, -2.4), (44.0, -1.8), (50.0, 0.0), (46.0, 2.2), (38.0, 5.4), (28.0, 6.4), (19.0, 4.8),
            (13.6, 1.8)], st.blade, depth=1.1, part="blade")
    d.poly([(46.0, 2.2), (38.0, 5.4), (28.0, 6.4), (19.0, 4.8), (20.0, 3.8), (28.0, 5.2), (38.0, 4.3), (45.0, 1.4)],
           st.blade, depth=1.1, z=1, tone=HIGHLIGHT)
    d.strip(14.0, 42.0, 0.45, material=st.blade, depth=1.6, z=1, part="ridge")
    gem(d, 12.8, 0.0, 0.9, st)
    return d


def kusarigama(st):
    d = Design(28, grip=4.5)
    d.disc(0.8, 0.0, 1.0, st.fitting, depth=2.0)
    haft(d, 0.8, 18.0, 0.8, st)
    grip(d, 1.5, 9.5, 0.95, st, period=1.6)
    d.strip(16.5, 19.0, 1.05, material=st.fitting, depth=2.4)
    sickle(d, 12.0, 0.6, 7.6, 3.0, 0.0, 125.0, st, steps=12)
    # The chain, swung out from the butt down the lit side, clear of the handle, and its weight.
    for i, (s, t) in enumerate([(0.4, -1.8), (1.6, -3.4), (3.4, -4.6), (5.4, -5.2)]):
        d.ring(s, t, 0.85, 0.3, CHAIN, depth=0.9 if i % 2 else 1.4, part=f"link{i}")
    d.disc(7.8, -5.4, 1.5, st.fitting, depth=3.0)
    return d


# ---------------------------------------------------------------- Arms VI (batch 55)


def katana(st):
    d = Design(36, grip=5.5)
    d.strip(0.0, 1.0, 1.0, material=st.fitting, depth=2.2)
    grip(d, 1.0, 10.0, 0.95, st, period=1.6)
    d.disc(10.6, 0.0, 2.3, st.fitting, depth=2.2)
    d.strip(11.2, 12.2, 1.2, material=st.fitting, depth=2.0, z=1)
    curved(d, 12.2, 33.0, 1.2, lambda s: 0.0022 * (s - 12.2) ** 2, st, tip=3.0)
    return d


def brazier_mace(st, frame=0):
    d = Design(35, grip=5.5)
    d.disc(1.0, 0.0, 1.2, st.fitting, depth=2.4)
    haft(d, 1.0, 23.0, 0.85, st)
    grip(d, 1.8, 10.0, 1.0, st)
    socket(d, 21.0, 23.5, 1.2, st)
    # The brazier: a bowl of coals, cage bars curving up to a crown ring, a spike, and a flame inside the cage.
    sway = (0.0, 0.6, -0.4, 0.4)[frame % 4]
    tall = (0.0, 1.2, 0.6, -0.6)[frame % 4]
    d.poly([(23.5, -1.2), (25.5, -4.0), (26.5, -4.0), (26.5, 4.0), (25.5, 4.0), (23.5, 1.2)], st.fitting, depth=4.0)
    for t0, t1 in ((-4.0, -3.4), (4.0, 3.4), (-1.6, -1.0), (1.6, 1.0)):
        d.line(26.5, t0, 32.0, t1, 0.8, st.fitting, depth=1.0 if abs(t0) < 2 else 4.2, part=f"bar{t0}")
    d.strip(31.6, 32.8, 3.6, material=st.fitting, depth=4.2, part="crown")
    d.poly([(32.8, -0.8), (35.0, 0.0), (32.8, 0.8)], st.blade, depth=1.6)
    d.poly([(26.5, -3.0), (29.5, -3.2 + sway), (33.5 + tall, sway), (29.5, 3.2 + sway), (26.5, 3.0)], EMBER, depth=2.6,
           z=-1, tone=LIGHT)
    d.poly([(26.5, -1.8), (29.0, -1.8 + sway), (31.8 + tall, sway * 0.6), (29.0, 1.8 + sway), (26.5, 1.8)], EMBER,
           depth=2.8, z=-1, tone=HIGHLIGHT)
    d.strip(26.5, 27.6, 3.2, material=EMBER, depth=3.0, z=-1, tone=MID)
    return d


# ---------------------------------------------------------------- Arms VIII (batch 59): thrown arms


def javelin(st):
    d = Design(45, grip=17.0)
    d.disc(0.7, 0.0, 0.8, st.fitting, depth=1.8)
    haft(d, 0.6, 36.0, 0.65, st)
    grip(d, 14.0, 20.0, 0.85, st, period=1.5)   # bound at the balance, where it is thrown from
    socket(d, 34.0, 37.5, 0.9, st)
    d.poly([(37.0, -0.85), (38.6, -1.8), (42.4, -0.8), (45.0, 0.0), (42.4, 0.8), (38.6, 1.8), (37.0, 0.85)], st.blade,
           depth=1.0, part="blade", bevel=0.0)
    d.strip(37.5, 43.0, 0.3, material=st.blade, depth=1.4, z=1, part="ridge")
    d.glint(40.0, -1.0)
    return d


def francisca(st):
    d = Design(28, grip=5.0)
    d.disc(0.9, 0.0, 1.0, st.fitting, depth=2.0)
    haft(d, 0.8, 24.0, 0.8, st)
    grip(d, 1.5, 9.0, 0.95, st, period=1.6)
    socket(d, 19.5, 24.5, 1.1, st)
    # A narrow neck flaring into an upswept bit: its toe sweeps up past the top of the haft, as a francisca's does.
    d.poly([(21.0, 1.1), (23.8, 1.1), (24.6, 2.8), (28.4, 5.6), (27.4, 8.2), (23.4, 8.4), (19.2, 7.6), (17.0, 6.0),
            (20.6, 2.8)], st.blade, depth=1.2, part="bit")
    d.poly([(28.4, 5.6), (27.4, 8.2), (23.4, 8.4), (19.2, 7.6), (17.0, 6.0), (18.4, 5.8), (19.8, 6.8), (23.4, 7.4),
            (26.8, 7.2), (27.6, 5.4)], st.blade, depth=1.2, z=1, tone=HIGHLIGHT)
    return d


def chakram(st):
    d = Design(22, grip=2.0)
    # A bladed ring, its outer edge ground bright, held by a wrapped stretch of its rim.
    d.ring(11.0, 0.0, 10.5, 8.2, st.blade, depth=1.0, part="ring")
    d.ring(11.0, 0.0, 10.5, 9.5, st.blade, depth=1.0, z=1, tone=HIGHLIGHT)
    d.strip(0.3, 2.9, 3.0, material=st.grip, depth=2.0, z=2, stripes=(1.0, DARK), part="wrap")
    return d


def harpoon(st):
    d = Design(45, grip=14.0)
    d.disc(0.7, 0.0, 0.8, st.fitting, depth=1.8)
    haft(d, 0.6, 36.0, 0.7, st, rings=(26.0,))
    grip(d, 10.0, 18.0, 0.9, st, period=1.4)
    socket(d, 33.0, 37.0, 0.95, st)
    # A narrow barbed head: a point and two barbs swept back.
    d.poly([(36.5, -0.8), (42.0, -0.8), (45.0, 0.0), (42.0, 0.8), (36.5, 0.8)], st.blade, depth=1.0, part="blade", bevel=0.0)
    for side in (1, -1):
        d.poly([(40.5, side * 0.8), (37.2, side * 2.8), (38.6, side * 0.8)], st.blade, depth=1.0, part=f"barb{side}")
    d.glint(42.0, -0.4)
    return d


WEAPONS = {"longsword": longsword, "greatsword": greatsword, "rapier": rapier, "flanged_mace": flanged_mace,
           "war_hammer": war_hammer, "glaive": glaive, "halberd": halberd, "spear": spear, "lance": lance,
           "dagger": dagger, "sabre": sabre, "estoc": estoc, "battle_axe": battle_axe, "flail": flail, "scythe": scythe,
           "quarterstaff": quarterstaff, "pike": pike, "zweihander": zweihander, "maul": maul, "executioner": executioner,
           "bill": bill, "labrys": labrys, "battleblade": battleblade, "war_fork": war_fork, "kama": kama,
           "war_pick": war_pick, "twinblade": twinblade, "nodachi": nodachi, "earthbreaker": earthbreaker, "katar": katar,
           "moonblade": moonblade, "kusarigama": kusarigama, "katana": katana, "brazier_mace": brazier_mace,
           "javelin": javelin, "francisca": francisca, "chakram": chakram, "harpoon": harpoon}
# Kinds whose sprite flickers (an animated texture, its frames top to bottom in one strip): frames, ticks each.
ANIMATED = {"brazier_mace": (4, 3)}
# Kinds whose head swings free in the hand (tools/arms_heads.py HEADS): their 3D model is the handle alone, the design
# function here, and the head is drawn live by client/arms/FlailHeads.java.
HANDLES = {"flail": lambda st: flail_handle(Design(34, grip=5.5), st)}
# A kind's icon is 32 pixels if it is held smaller than LARGE, 48 if larger (the great arms and polearms).
LARGE = 1.55
# The 3D models' textures: the upright design at the top left of a square this size.
MODEL_TEXTURE = 64


def design(kind, metal, frame=0):
    style = STYLES[metal]
    return WEAPONS[kind](style, frame) if kind in ANIMATED else WEAPONS[kind](style)


def model_design(kind, metal, frame=0):
    """The design a kind's 3D model is built from: the handle alone for a kind whose head swings free (HANDLES), else
    the whole design."""
    return HANDLES[kind](STYLES[metal]) if kind in HANDLES else design(kind, metal, frame)


def icon_size(held):
    return 32 if held < LARGE else 48


def layout(kind, held):
    """Where a kind's icon lies on its canvas: (size, grip pixel, diagonal steps to a design unit, hand factor). The
    design is fitted, whole, a pixel in from the edges, along the diagonal; the hand factor is how much larger it must be
    held to be as long in the hand as one that fills the diagonal (a wide head makes the rest of the icon smaller)."""
    d = design(kind, "bronze")
    size = icon_size(held)
    grip_px, scale = px.fit([d], size)
    return size, grip_px, scale, ((size - 3.0) / d.length) / scale


def draw(kind, metal, held, frame=0, mirrored=False):
    """A kind's inventory icon in a metal (mirrored: point to the top left, as the spear's hand pose wants it)."""
    size, grip_px, scale, _factor = layout(kind, held)
    if mirrored:
        grip_px = (size - grip_px[0], grip_px[1])
    return px.icon(design(kind, metal, frame), size, grip_px, scale, mirrored=mirrored)


def held_at(kind, held, mirrored=False):
    """Where a kind's icon is held, in pixels from its top left; the icon's size; and its hand factor (layout)."""
    size, (gx, gy), _scale, factor = layout(kind, held)
    return ((size - gx) if mirrored else gx, gy), size, factor


def model(kind, metal, held, frame=0, mirrored=False):
    """A kind's 3D model in a metal: (its texture, a MODEL_TEXTURE square; its elements), lying over the icon."""
    size, grip_px, scale, _factor = layout(kind, held)
    if mirrored:
        grip_px = (size - grip_px[0], grip_px[1])
    unit = scale * math.sqrt(2.0) * 16.0 / size
    grip_model = (grip_px[0] * 16.0 / size, 16.0 - grip_px[1] * 16.0 / size)
    # An animated arm's model is shaped to fit every frame, and each frame's texture laid out alike. A kind whose head
    # swings free is built from its handle alone, laid out as the whole was, so the hand holds it where it always did.
    frames = [model_design(kind, metal, f) for f in range(ANIMATED.get(kind, (1, 0))[0])]
    geometry = px.merged(frames)
    upright, elements = px.model_elements(model_design(kind, metal, frame), MODEL_TEXTURE, (0, 0), grip_model, unit,
                                          mirrored=mirrored, geometry=geometry, width=px.upright_width(geometry))
    texture = Image.new("RGBA", (MODEL_TEXTURE, MODEL_TEXTURE), (0, 0, 0, 0))
    texture.paste(upright, (0, 0))
    if kind in HANDLES:
        import arms_heads   # (here: arms_heads draws on this module's layout)
        style = STYLES[metal]
        arms_heads.paint_swatches(texture, {"chain": CHAIN, "blade": style.blade, "fitting": style.fitting})
    return texture, elements


def head_layout(kind, held):
    """For a kind whose head swings free: (the hand's point in model pixels, model pixels a design unit, the grip and
    the eye along the haft in design units), from the same layout as its model (tools/arms_heads.py entry)."""
    size, grip_px, scale, _factor = layout(kind, held)
    unit = scale * math.sqrt(2.0) * 16.0 / size
    grip_model = (grip_px[0] * 16.0 / size, 16.0 - grip_px[1] * 16.0 / size)
    return grip_model, unit, design(kind, "bronze").grip, FLAIL_EYE
