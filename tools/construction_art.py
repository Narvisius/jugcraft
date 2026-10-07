"""High-detail (64x64) item art for construction chemistry (batch 32), drawn with tools/hd_art.py.

The foam sprayer is a dieselpunk tool: an olive-drab housing worn to bare gunmetal at the edges, a hazard-striped
front collar, a pressure gauge, a yellow foam canister clamped on top with a rubber hose down to a steel lance, a
brass nozzle and a puff of foam leaving it. All original.
"""
import math

import hd_art as hd
from hd_art import Canvas


class Tool:
    """Maps tool space (s along the barrel towards the nozzle, t up off it) to the canvas."""

    def __init__(self, origin, angle, scale=1.0):
        self.ox, self.oy = origin
        self.angle = angle
        self.k = scale
        a = math.radians(angle)
        self.ax, self.ay = math.cos(a), math.sin(a)
        self.ux, self.uy = self.ay, -self.ax  # perpendicular, towards the top left

    def __call__(self, s, t=0.0):
        s, t = s * self.k, t * self.k
        return (self.ox + self.ax * s + self.ux * t, self.oy + self.ay * s + self.uy * t)

    def r(self, radius):
        """A size in tool space, on the canvas."""
        return radius * self.k


def foam_sprayer():
    c = Canvas()
    tool = Tool((20, 40), -45, scale=0.92)
    R = tool.r
    deg = tool.angle

    # The rubber hose, behind everything: from the canister's valve, looping over to the lance's inlet.
    hose = [tool(14, 10.5), tool(19, 12), tool(24, 10), tool(26, 5.5), tool(24, 2.5)]
    for a, b in zip(hose, hose[1:]):
        c.capsule(a, b, R(1.45), hd.RUBBER)

    # Pistol grip with finger grooves and a steel pommel.
    c.box(tool(-8.5, -9.5), R(3.4), R(8.2), deg + 66, hd.RUBBER, bevel=R(2.0),
          paint=lambda u, v: hd.HAZARD_BLACK if int(v + 20) % 4 == 0 and abs(u) < R(2.4) else None)
    c.capsule(tool(-12.2, -17.0), tool(-15.2, -15.6), R(2.0), hd.GUNMETAL)

    # Trigger guard and trigger.
    c.capsule(tool(-2.5, -5.2), tool(-0.8, -10.6), R(0.85), hd.STEEL)
    c.capsule(tool(-0.8, -10.6), tool(-6.0, -12.4), R(0.85), hd.STEEL)
    c.capsule(tool(-3.2, -5.6), tool(-3.5, -9.0), R(1.05), hd.CHROME)

    # The housing: olive paint worn to gunmetal at the edges, louvred vents, a top rail and bolts.
    body_w, body_h = R(13.0), R(5.4)
    wear = hd.worn(hd.OLIVE, hd.GUNMETAL, edge=1.4, seed=3201, chance=0.5)(body_w, body_h)

    def body_paint(u, v):
        if R(-11) < u < R(-3) and abs(v) < R(2.4) and int(u + 40) % 2 == 0:
            return hd.GUNMETAL
        return wear(u, v)
    c.box(tool(0, 0), body_w, body_h, deg, hd.OLIVE, bevel=R(1.6), paint=body_paint)
    c.box(tool(0, 5.9), R(11.0), R(1.0), deg, hd.GUNMETAL, bevel=0.7)  # top rail
    c.box(tool(13.8, 0), R(2.0), R(6.2), deg, hd.SAFETY_YELLOW, bevel=1.0, paint=hd.hazard(3))  # front collar
    for s_, t_ in ((-11, 3.8), (-11, -3.8), (10.6, 3.8), (10.6, -3.8)):
        c.disc(tool(s_, t_), R(1.0), hd.CHROME, 0.9)

    # Pressure gauge: chrome bezel, cream face, red zone and needle.
    g = tool(5.5, -0.6)
    c.ring(g, R(4.4), R(3.1), hd.CHROME)
    c.disc(g, R(3.2), hd.GAUGE_FACE, 0.25)
    for k in range(5):  # tick marks round the dial
        a = math.radians(140 + k * 52)
        c.pixel(g[0] + math.cos(a) * R(2.5), g[1] + math.sin(a) * R(2.5), (70, 70, 74))
    c.pixel(g[0] + R(1.8), g[1] + R(0.8), (190, 30, 30))
    c.pixel(g[0] + R(2.0), g[1], (190, 30, 30))
    c.line(g, (g[0] + R(1.3), g[1] - R(1.5)), (30, 30, 34))

    # The lance: steel tube with brass ferrules and a flared brass nozzle.
    c.capsule(tool(15, 0), tool(40, 0), R(2.0), hd.STEEL,
              bands=[(0.0, 0.07, hd.BRASS), (0.44, 0.5, hd.BRASS), (0.92, 1.0, hd.BRASS)])
    c.capsule(tool(24, 2.4), tool(24, 0.5), R(1.5), hd.BRASS)  # hose inlet
    c.capsule(tool(40, 0), tool(43.5, 0), R(2.8), hd.BRASS, flat_ends=True)
    c.capsule(tool(43.5, 0), tool(45.5, 0), R(3.5), hd.BRASS, flat_ends=True)
    c.disc(tool(45.8, 0), R(1.5), hd.GUNMETAL, 0.2)

    # The foam canister clamped on top: yellow can, white label band with a hazard diamond, black cap, chrome valve.
    c.capsule(tool(-9, 10.5), tool(10, 10.5), R(4.6), hd.SAFETY_YELLOW, bands=[(0.4, 0.62, hd.WHITE_PAINT)])
    c.capsule(tool(10, 10.5), tool(12.5, 10.5), R(2.3), hd.HAZARD_BLACK, flat_ends=True)
    c.capsule(tool(12.5, 10.5), tool(14.2, 10.5), R(1.2), hd.CHROME, flat_ends=True)
    for s_ in (-5, 7):
        c.box(tool(s_, 8.3), R(0.9), R(5.4), deg, hd.GUNMETAL, bevel=0.6)
        c.disc(tool(s_, 5.4), R(0.8), hd.CHROME, 0.9)
    for t_ in (9.0, 12.0):  # stencilled lines on the label band
        c.line(tool(-1.5, t_), tool(1.0, t_), (110, 110, 116))
    m = tool(3.5, 10.5)
    d = R(1.9)
    c.polygon([(m[0], m[1] - d), (m[0] + d, m[1]), (m[0], m[1] + d), (m[0] - d, m[1])], hd.RED)

    # A puff of foam leaving the nozzle.
    c.blob(tool(50.5, 0.3), R(3.6), hd.FOAM, seed=3202, lumps=6)
    for s_, t_, r_ in ((54.5, 2.6, 1.2), (54, -3.0, 1.0)):
        c.disc(tool(s_, t_), R(r_), hd.FOAM, 0.9)
    return c.finish()


def foam_canister():
    """An aerosol can of foam: yellow body, white label band with a hazard diamond, a black shoulder cap, a valve."""
    c = Canvas()
    c.capsule((32, 18), (32, 57), 13, hd.SAFETY_YELLOW, flat_ends=True,
              bands=[(0.32, 0.62, hd.WHITE_PAINT), (0.95, 1.0, hd.GUNMETAL)])
    c.box((32, 57), 13, 1.6, 0, hd.STEEL, bevel=1.0)  # bottom rim
    c.capsule((32, 14), (32, 19), 10, hd.HAZARD_BLACK, flat_ends=True)  # shoulder cap
    c.box((32, 19), 13, 1.4, 0, hd.STEEL, bevel=0.9)  # top seam
    c.capsule((32, 6), (32, 13), 3.2, hd.CHROME, flat_ends=True)  # valve stem
    c.capsule((32, 6), (40, 6), 2.2, hd.HAZARD_BLACK)  # spray nozzle
    # Label: a red hazard diamond and printed lines.
    c.polygon([(32, 28), (38, 34), (32, 40), (26, 34)], hd.RED)
    c.polygon([(32, 31), (35, 34), (32, 37), (29, 34)], hd.WHITE_PAINT)
    for y in (43, 45):
        c.line((24, y), (40, y), (90, 90, 96))
    # A drip of foam down the side.
    c.blob((44, 9), 2.6, hd.FOAM, seed=3203, lumps=3)
    c.capsule((44, 10), (44, 15), 1.1, hd.FOAM)
    return c.finish()


def cement():
    """A paper sack of cement: folded top, a printed grey band, a stitched seam and a little spilled dust."""
    c = Canvas()
    c.box((32, 37), 17, 20, 0, hd.PAPER, bevel=4.0,
          paint=lambda u, v: hd.CONCRETE if -5 < v < 4 else None)
    c.box((32, 15), 15, 3.2, 0, hd.PAPER, bevel=2.2)  # folded top
    for x in range(18, 47, 3):  # stitching
        c.pixel(x, 18, (110, 92, 60))
    # Printed lettering stand-in: a dark band of blocks on the grey.
    for i, x in enumerate(range(21, 43, 4)):
        c.box((x + 1.5, 36), 1.4, 2.0 if i % 2 else 2.6, 0, hd.GUNMETAL, bevel=0.4)
    c.blob((44, 58), 3.0, hd.CONCRETE, seed=3204, lumps=4)
    return c.finish()


def cement_mix():
    """A heap of crushed limestone, clay and sand: grey-brown, with a few clay-red and pale grains (a clean heap from
    tools/material_style.py)."""
    import material_style as ms
    img = ms.dust([(96, 92, 86), (132, 126, 116), (156, 150, 140), (178, 172, 160), (204, 198, 186)])
    for (x, y), colour in zip(((6, 9), (10, 10), (4, 12), (8, 12), (12, 12)),
                              ((176, 104, 70), (230, 222, 196), (176, 104, 70), (230, 222, 196), (176, 104, 70))):
        img.putpixel((x, y), colour + (255,))
    return img


def rebar():
    """Three ribbed steel reinforcing bars laid diagonally, one gone a little rusty."""
    c = Canvas()
    for k, off in enumerate((-8, 0, 8)):
        # Each bar runs bottom left to top right, offset across the diagonal.
        a = (7 + off, 57 + off)
        b = (57 + off, 7 + off)
        a = (max(4, min(60, a[0])), max(4, min(60, a[1])))
        b = (max(4, min(60, b[0])), max(4, min(60, b[1])))
        mat = hd.RUST if k == 1 else hd.STEEL
        c.capsule(a, b, 2.8, mat)
        length = math.hypot(b[0] - a[0], b[1] - a[1])
        for i in range(1, int(length / 3.2)):
            t = i * 3.2 / length
            x, y = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
            c.capsule((x - 1.9, y - 1.9), (x + 1.9, y + 1.9), 0.75, mat, tint=-0.12)
    return c.finish()


ITEMS = {"foam_sprayer": foam_sprayer, "foam_canister": foam_canister, "cement": cement, "cement_mix": cement_mix,
         "rebar": rebar}
