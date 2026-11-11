"""The NT-7 plasma rifle (P6, take 3 of rifle A, approved by Justin on 9 November 2026): one description of its parts that
makes the item (and so the crew's rifle, which is the item drawn in their hands) and that the first-person view is
placed from.

What it is (from the sheet): a bullpup carbine. One moulded body from the butt to the guard (tungsten grey), its floor
stepping up behind the grip in a 45-degree fillet, NT-7 small over the cell; a dark metal upper with a rail its whole
length; a holographic sight on the rail (a clamp and its throw lever, a body with its battery cap and two buttons, a
hood sloping at the front over two windows, the front one coated); a pistol grip leaned back 18 degrees (stippled sides,
palm swell, two finger ridges, flared base, beavertail); a full-hand guard moulded with the body and a curved steel
trigger in it; the power cell in the stock's floor with its charge window (four bars, live); the accelerator under the
upper between six gunmetal fins (dark until a shot: then it flares purple and darkens until the next is ready); an
angled fore grip, sling points, a bright steel muzzle with its emitter.

The rifle's frame: x across (+x the rifle's left as its shooter holds it), y up, z along the barrel (+z the muzzle), in
the rifle's pixels; its origin at the top of the grip (where the hand closes). The item's model space: model = (z + 8,
y + 8, 8 - x), so the grip's top is the item's centre (the barrel along +x, the top up, the left side north).

Its states (STATES): the accelerator's glow and the cell's bars, by how much of the cooldown is left (1 at the shot, 0
ready): the item definition picks them with jugcraft:plasma_charge (client/PlasmaCharge: a player's cooldown, a crew
member's time since their last shot).

The reticle is not part of the model: a real holographic reticle shows only to the eye looking through the window, so
it's drawn over the middle of the screen while aiming (client/PlasmaRifleView), where the bolts go.
"""
from __future__ import annotations

import json
import math
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
TPX = 4                                   # texels to the rifle's pixel (the fine parts: twice that)
TEX_W, TEX_H = 256, 128                   # the item's texture
SHIFT = 1.0                               # the sheet's grip top was at z 1: the frame here has it at 0
SIGHT_Y, SIGHT_Z = 4.21, 0.5 - SHIFT      # the reticle's centre: the rear window's middle (the bolts' line when aimed)
FRONT_GLASS_Z = 1.88 - SHIFT              # the front window

# colours (0-1)
BLACK = (0.095, 0.10, 0.11)               # polymer furniture
HOUSING = (0.25, 0.26, 0.28)              # tungsten grey
GUNM = (0.40, 0.41, 0.43)                 # gunmetal: fins, rail, cell
STEEL = (0.62, 0.63, 0.65)                # the muzzle
UPPER = (0.20, 0.205, 0.22)               # the metal upper: dark anodised
RUBBER = (0.07, 0.07, 0.075)
HOOD = (0.17, 0.175, 0.185)
TEXT = (0.90, 0.90, 0.86)
VIOLET = (0.72, 0.46, 1.0)
TRIGGER = (0.58, 0.59, 0.61)

FONT5 = {'N': ["X...X", "XX..X", "X.X.X", "X..XX", "X...X"], 'T': ["XXX", ".X.", ".X.", ".X.", ".X."],
         '-': ["..", "..", "XX", "..", ".."], '7': ["XXXX", "...X", "..X.", ".X..", ".X.."]}

# (state name, the accelerator's glow 0-1, the cell's bars lit, from this much cooldown left): the item definition takes
# the one with the highest threshold under the cooldown left; "ready" is the fallback
STATES = (("ready", 0.0, 4, None), ("cooling_4", 0.12, 3, 0.001), ("cooling_3", 0.30, 2, 0.25), ("cooling_2", 0.60, 1, 0.50),
          ("fired", 1.0, 0, 0.75))


# ------------------------------------------------------------------ painters: (H, W, 4) floats, as seen from outside, top row up
def glyphs(text, font):
    cols = []
    for k, ch in enumerate(text):
        g = np.array([[c == 'X' for c in row] for row in font[ch]])
        if k:
            cols.append(np.zeros((g.shape[0], 1), bool))
        cols.append(g)
    return np.concatenate(cols, axis=1)


def cast(rgb, grad=0.10, top=0.25, bottom=0.30, sides=0.12, edge=1, pins=(), lines=()):
    """A part as cast or moulded: a gentle light-to-shade down the face, a lit top edge and a shaded bottom edge, the ends
    a touch darker; steel pins ((u, v) fractions), panel lines (('h' or 'v', fraction))."""
    def f(a):
        H, W = a.shape[:2]
        base = np.array(rgb, np.float32)
        g = np.linspace(1 + grad / 2, 1 - grad / 2, H)[:, None, None]
        a[..., :3] = np.clip(base * g, 0, 1)
        e = min(edge, max(0, H // 3))
        if H > 2:
            a[:e, :, :3] = np.clip(base * (1 + top) + 0.02, 0, 1)
            a[H - e:, :, :3] = base * (1 - bottom)
        if W > 2:
            a[e:H - e, :e, :3] *= 1 - sides
            a[e:H - e, W - e:, :3] *= 1 - sides
        for kind, fr in lines:
            if kind == 'h':
                y = int(round(fr * (H - 1)))
                a[y, e:W - e, :3] = base * 0.45
                if y + 1 < H - e:
                    a[y + 1, e:W - e, :3] = np.clip(base * 1.12, 0, 1)
            else:
                x = int(round(fr * (W - 1)))
                a[e:H - e, x, :3] = base * 0.45
                if x + 1 < W - e:
                    a[e:H - e, x + 1, :3] = np.clip(base * 1.12, 0, 1)
        for u, v in pins:
            x, y = int(round(u * (W - 1))), int(round(v * (H - 1)))
            a[y:y + 2, x:x + 2, :3] = (0.34, 0.35, 0.37)
            a[y, x, :3] = (0.52, 0.53, 0.55)
    return f


def slots(rgb, n, slot=0.4, horizontal=False, inner=None):
    """Clean slots across a moulded face (rail teeth, ports, a butt pad's ribs)."""
    def f(a):
        cast(rgb, grad=0.0, top=0.22, bottom=0.32)(a)
        H, W = a.shape[:2]
        L = H if horizontal else W
        c = np.array(inner if inner is not None else np.array(rgb) * 0.3)
        for k in range(n):
            c0 = int(round((k + 0.5 - slot / 2) / n * L))
            c1 = max(c0 + 1, int(round((k + 0.5 + slot / 2) / n * L)))
            if horizontal:
                a[c0:c1, 1:W - 1, :3] = c
            else:
                a[1:H - 1, c0:c1, :3] = c
    return f


def charge(lit_n, n=4):
    """The cell's window: n bars (two texels wide, a texel apart), the first lit_n lit (the rifle's charge)."""
    def f(a):
        H, W = a.shape[:2]
        a[..., :3] = (0.035, 0.035, 0.045)
        bw = max(1, (W - 1) // n - 1)
        x = (W - (n * (bw + 1) - 1)) // 2
        r0, r1 = (1, H - 1) if H > 2 else (0, H)
        for k in range(n):
            a[r0:r1, x:x + bw, :3] = VIOLET if k < lit_n else (0.10, 0.08, 0.14)
            x += bw + 1
    return f


def core(level):
    """The accelerator seen between the fins (and the emitter): smoked violet glass with its coil's rings when dark; lit,
    plasma, purple, hottest at the shot."""
    def f(a):
        H, W = a.shape[:2]
        if level <= 0:
            a[..., :3] = (0.075, 0.06, 0.11)
            for x in range(0, W, 3):
                a[:, x, :3] = (0.12, 0.09, 0.17)
            a[H // 2 - 1:H // 2 + 1, :, :3] = (0.10, 0.08, 0.15)
        else:
            hot = np.array((0.74, 0.44, 1.0)) * level + np.array((0.46, 0.22, 0.88)) * (1 - level)
            a[..., :3] = hot * (0.30 + 0.70 * level)
            mid = np.abs(np.linspace(-1, 1, H))[:, None]
            a[..., :3] *= (1 - 0.35 * mid)[..., None]
    return f


def stipple(rgb):
    """A grip's side: a smooth border and a finely stippled panel inside it."""
    def f(a):
        cast(rgb, grad=0.08, edge=2)(a)
        H, W = a.shape[:2]
        for y in range(3, H - 3):
            for x in range(3, W - 3):
                a[y, x, :3] = np.array(rgb) * (0.90 if (x + y) % 2 == 0 else 1.04)
    return f


def body_side(rgb, text_u, open_u, pins=()):
    """The body's side (one moulding, the butt to the guard), at twice the texels: a gentle light-to-shade, a lit top
    edge, the bottom edge shaded only where it is open (`open_u`: over the stock the floor runs on down), NT-7 five
    texels tall at text_u over the cell, steel pins."""
    def f(a):
        H, W = a.shape[:2]
        base = np.array(rgb, np.float32)
        a[..., :3] = base * np.linspace(1.05, 0.96, H)[:, None, None]
        a[:2, :, :3] = np.clip(base * 1.25 + 0.02, 0, 1)
        u0, u1 = int(open_u[0] * W), int(open_u[1] * W)
        a[H - 2:, u0:u1, :3] = base * 0.70
        a[2:, :2, :3] *= 0.88
        a[2:, W - 2:, :3] *= 0.88
        gl = glyphs('NT-7', FONT5)
        gh, gw = gl.shape
        x0 = int(round(text_u * W)) - gw // 2
        y0 = int(round(0.58 * H)) - gh // 2
        a[y0:y0 + gh, x0:x0 + gw, :3][gl] = TEXT
        for u, v in pins:
            x, y = int(round(u * (W - 1))), int(round(v * (H - 1)))
            a[y:y + 2, x:x + 2, :3] = (0.34, 0.35, 0.37)
            a[y, x, :3] = (0.55, 0.56, 0.58)
    return f


def floor_side(rgb):
    """The stock's floor, under the body's side: it carries on the side's shade, no edge where they meet."""
    def f(a):
        H, W = a.shape[:2]
        base = np.array(rgb, np.float32)
        a[..., :3] = base * 0.955
        a[H - 2:, :, :3] = base * 0.70
        a[:, :2, :3] *= 0.88
        a[:, W - 2:, :3] *= 0.88
    return f


def glass(rgb, alpha):
    def f(a):
        a[..., :3] = rgb
        a[..., 3] = alpha
    return f


# ------------------------------------------------------------------ the parts
@dataclass
class Part:
    name: str
    lo: tuple                       # the box in its own frame (rifle px, before it's turned)
    hi: tuple
    faces: dict                     # '+x' '-x' '+y' '-y' '+z' '-z' or '*': (key, painter)
    angle: float = 0.0              # turned this much about the rifle's x axis (degrees, y toward z), about its middle
    glow: bool = False              # drawn full bright
    tpx: int = TPX
    only: tuple = ()                # a flat pane: just these faces

    def centre(self):
        return tuple((a + b) / 2 for a, b in zip(self.lo, self.hi))


class Builder:
    def __init__(self):
        self.parts: list[Part] = []

    def box(self, x0, y0, z0, x1, y1, z1, faces, glow=False, tpx=TPX, name='part'):
        self.parts.append(Part(name, (x0, y0, z0 - SHIFT), (x1, y1, z1 - SHIFT), faces, 0.0, glow, tpx))

    def obox(self, c, size, angle, faces, glow=False, tpx=TPX, name='part'):
        c = (c[0], c[1], c[2] - SHIFT)
        lo = tuple(c[i] - size[i] / 2 for i in range(3))
        hi = tuple(c[i] + size[i] / 2 for i in range(3))
        self.parts.append(Part(name, lo, hi, faces, float(angle), glow, tpx))

    def pane(self, x0, y0, x1, y1, z, faces, name='glass'):
        self.parts.append(Part(name, (x0, y0, z - SHIFT), (x1, y1, z - SHIFT), faces, 0.0, False, TPX, only=('+z', '-z')))


def F(key, fn):
    return {'*': (key, fn)}


def rx(a):
    a = math.radians(a)
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def grip(b: Builder):
    """The pistol grip, leaned back 18 degrees about its top (the sheet's (0, 0, 1)): stippled sides, palm swell, finger
    ridges, flared base, beavertail. Returns where its base's front-bottom corner is (the guard's bar meets it)."""
    Rg = rx(-18)
    top = np.array([0.0, 0.0, 1.0])
    def part(lo, hi_, faces, extra=0.0, tpx=TPX, name='grip'):
        c = (np.array(lo) + np.array(hi_)) / 2
        b.obox(top + Rg @ c, np.array(hi_) - np.array(lo), -18 + extra, faces, tpx=tpx, name=name)
    side = ('g_st', stipple(BLACK))
    BK = cast(BLACK, top=0.3)
    part((-0.48, -2.25, -0.55), (0.48, 0.05, 0.55), {'*': ('g_core', cast(BLACK, grad=0.06)), '+x': side, '-x': side}, tpx=2 * TPX)
    part((-0.51, -1.70, -0.66), (0.51, -0.80, -0.40), {'*': ('g_swell', BK)}, name='palm_swell')
    part((-0.44, -0.28, -1.12), (0.44, 0.05, -0.50), {'*': ('g_tail', BK)}, extra=12, name='beavertail')
    for yc in (-0.85, -1.50):
        part((-0.40, yc - 0.09, 0.50), (0.40, yc + 0.09, 0.62), {'*': ('g_ridge', cast(BLACK, top=0.4, bottom=0.4))}, name='finger_ridge')
    part((-0.56, -2.52, -0.76), (0.56, -2.22, 0.62), {'*': ('g_base', BK)}, name='grip_base')
    c = top + Rg @ np.array([0, -2.37, 0.62])
    b.obox(c, (1.12, 0.21, 0.21), -18 + 45, {'*': ('g_basef', BK)}, name='grip_base_front')
    return top + Rg @ np.array([0, -2.52, 0.70])


def holo(b: Builder):
    """The holographic sight on the rail: clamp and throw lever, body, battery cap, two buttons, the hood over two windows."""
    b.box(-0.66, 3.10, 0.05, 0.66, 3.40, 2.35, F('h_clamp', cast((0.13, 0.135, 0.145), lines=(('h', 0.3),))), name='sight_clamp')
    b.obox((0.74, 3.26, 1.30), (0.12, 0.18, 1.05), 4, F('h_lever', cast(BLACK)), name='sight_lever')
    b.box(0.66, 3.18, 1.75, 0.82, 3.36, 1.95, F('h_pivot', cast(STEEL)), name='sight_pivot')
    b.box(-0.62, 3.40, 0.05, 0.62, 3.80, 2.35, F('h_body', cast(BLACK, lines=(('h', 0.8),))), name='sight_body')
    # the battery cap across the front on the shooter's right: an octagon, two boxes, one turned 45 degrees
    b.obox((-0.73, 3.60, 1.85), (0.22, 0.30, 0.30), 0, F('h_cap', cast(GUNM)), name='battery_cap')
    b.obox((-0.73, 3.60, 1.85), (0.21, 0.30, 0.30), 45, F('h_cap', cast(GUNM)), name='battery_cap')
    b.box(-0.88, 3.56, 1.81, -0.84, 3.64, 1.89, F('h_capc', cast(STEEL)), name='battery_cap_coin')
    for x0 in (0.10, 0.36):
        b.box(x0, 3.52, -0.03, x0 + 0.18, 3.70, 0.05, F('h_btn', cast((0.06, 0.06, 0.065), top=0.4)), name='sight_button')
    # the hood: walls and roof to z 1.95, their front sloping down to the body's front (a slab turned 19 degrees, its
    # front face on the slope, the rest inside the wall and the body)
    ang = math.degrees(math.atan2(0.40, 1.14))
    nrm = np.array([0, math.sin(math.radians(ang)), math.cos(math.radians(ang))])
    mid = np.array([0, (4.94 + 3.80) / 2, (1.95 + 2.35) / 2])
    for sx in (-1, 1):
        xa, xb = sorted((sx * 0.46, sx * 0.62))
        b.box(xa, 3.80, 0.10, xb, 4.78, 1.95, F('h_wall', cast(HOOD)), name='hood_wall')
        b.obox(mid - nrm * 0.25 + np.array([(xa + xb) / 2, 0, 0]), (0.16, 1.21, 0.5), -ang, F('h_slope', cast(HOOD)), name='hood_slope')
    b.box(-0.62, 4.78, 0.10, 0.62, 4.94, 1.95, F('h_roof', cast(HOOD, top=0.35)), name='hood_roof')
    b.box(-0.46, 4.62, 0.30, 0.46, 4.78, 0.42, F('h_frame', cast(HOOD)), name='hood_frame')
    b.box(-0.46, 4.62, 1.80, 0.46, 4.78, 1.95, F('h_frame', cast(HOOD)), name='hood_frame')
    b.pane(-0.46, 3.80, 0.46, 4.62, FRONT_GLASS_Z + SHIFT, F('h_front_glass', glass((0.32, 0.58, 0.60), 0.32)), name='front_window')
    b.pane(-0.46, 3.80, 0.46, 4.62, SIGHT_Z + SHIFT, F('h_rear_glass', glass((0.78, 0.82, 0.86), 0.10)), name='rear_window')


def rifle(level=0.0, bars=4) -> list[Part]:
    """The parts, the accelerator glowing `level` (0 dark) and `bars` of the cell's four lit."""
    b = Builder()
    z0, z1 = -6.35, 4.40
    zt = -3.15                                          # NT-7 over the cell's middle
    L = z1 - z0
    zp = (0.55, 0.95)                                   # the two cross pins over the grip
    def u_n(z): return (z1 - z) / L                     # +x (north): its texture runs from the muzzle back
    def u_s(z): return (z - z0) / L                     # -x (south): from the butt forward
    side_n = ('body_n', body_side(HOUSING, u_n(zt), (0.0, u_n(-1.2)), pins=[(u_n(z), 0.28) for z in zp]))
    side_s = ('body_s', body_side(HOUSING, u_s(zt), (u_s(-1.2), 1.0), pins=[(u_s(z), 0.28) for z in zp]))
    b.box(-1.0, 0.25, z0, 1.0, 2.20, z1, {'*': ('body', cast(HOUSING)), '+x': side_n, '-x': side_s}, tpx=2 * TPX, name='body')
    b.box(-1.0, -0.85, z0, 1.0, 0.25, -1.2, {'*': ('floor', cast(HOUSING, top=0.0)), '+x': ('floor_s', floor_side(HOUSING)),
                                             '-x': ('floor_s', floor_side(HOUSING))}, tpx=2 * TPX, name='stock_floor')
    b.box(-0.78, 0.0, -1.2, 0.78, 0.25, z1, F('recb', cast(HOUSING, top=0.0)), name='body_chamfer')
    b.obox((0, 0.0, -1.2), (1.56, 0.80, 0.80), 45, F('fillet', cast(HOUSING)), name='floor_fillet')
    # the butt pad and the rear sling point
    b.box(-0.95, -0.85, -6.72, 0.95, 2.78, z0, {'*': ('pad', cast(RUBBER)),
                                                '-z': ('pad_r', slots(RUBBER, 6, 0.3, horizontal=True, inner=(0.03, 0.03, 0.03)))}, name='butt_pad')
    b.box(-1.12, 0.55, -5.95, -1.0, 0.95, -5.55, F('qd', cast(STEEL)), name='sling_point')
    # the cheek rest and the metal upper with its rail
    b.box(-0.92, 2.20, z0, 0.92, 2.62, -3.1, F('cheek', cast(BLACK)), name='cheek_rest')
    b.box(-0.72, 2.62, z0, 0.72, 2.80, -3.1, F('cheek2', cast(BLACK, top=0.35)), name='cheek_rest_top')
    b.box(-0.86, 2.20, -3.1, 0.86, 2.78, 10.2, F('upper', cast(UPPER, lines=(('v', 0.36),))), name='upper')
    b.box(-0.55, 2.78, -2.9, 0.55, 3.10, 9.95, {'*': ('rail', cast(GUNM)), '+y': ('rail_t', slots(GUNM, 26, 0.34)),
                                                '+x': ('rail_s', slots(GUNM, 26, 0.34)), '-x': ('rail_s', slots(GUNM, 26, 0.34))}, name='rail')
    # the selector, both sides
    zs = 0.15
    for sx in (-1, 1):
        b.box(min(sx * 1.0, sx * 1.06), 1.02, zs - 0.13, max(sx * 1.0, sx * 1.06), 1.28, zs + 0.13, F('boss', cast(UPPER)), name='selector_boss')
        b.obox((sx * 1.09, 1.15 - 0.22 * math.sin(math.radians(35)), zs + 0.22 * math.cos(math.radians(35))), (0.06, 0.14, 0.50), -35,
               F('sel', cast(UPPER, top=0.4)), name='selector')
    # the cell and its charge window (live)
    b.box(-0.8, -2.15, -4.7, 0.8, -0.85, -1.6, F('cell', cast(GUNM, lines=(('h', 0.15),))), name='cell')
    b.box(-0.86, -2.45, -4.8, 0.86, -2.15, -1.5, F('cell_base', cast(BLACK)), name='cell_base')
    for x0, x1 in ((0.80, 0.83), (-0.83, -0.80)):
        b.box(x0, -1.85, -4.5, x1, -1.15, -1.8, F('bars%d' % bars, charge(bars)), glow=True, name='charge_bars')
    # the grip, the full-hand guard and the trigger
    base = grip(b)
    b.box(-0.36, -2.30, 3.50, 0.36, 0.0, 4.00, F('post', cast(BLACK)), name='guard_post')
    b.obox((0, 0.0, 3.50), (0.72, 0.50, 0.50), 45, F('post_f', cast(BLACK)), name='guard_fillet')
    b.obox((0, -2.374, 3.719), (0.72, 0.30, 0.52), -45, F('corner', cast(BLACK)), name='guard_corner')
    b.box(-0.36, -2.66, float(base[2]) - 0.2, 0.36, -2.30, 3.65, F('bar', cast(BLACK, top=0.3)), name='guard_bar')
    b.box(-0.22, -0.04, 1.95, 0.22, 0.02, 2.75, F('slot', cast((0.04, 0.04, 0.045))), name='trigger_slot')
    b.obox((0, -0.30, 2.30), (0.24, 0.58, 0.22), 0, F('trig', cast(TRIGGER, top=0.3)), name='trigger')
    b.obox((0, -0.78, 2.40), (0.24, 0.46, 0.22), -22, F('trig', cast(TRIGGER, top=0.3)), name='trigger')
    b.obox((0, -1.12, 2.58), (0.24, 0.30, 0.22), -50, F('trig', cast(TRIGGER, top=0.3)), name='trigger')
    # the accelerator between six fins; the spine
    b.box(-0.5, 0.65, z1, 0.5, 1.85, 10.2, F('core%.2f' % level, core(level)), glow=level > 0, name='accelerator')
    for i in range(6):
        z = 4.75 + i * 0.95
        b.box(-0.98, 0.30, z, 0.98, 2.20, z + 0.45, F('fin', cast(GUNM, top=0.35, bottom=0.35, sides=0.2)), name='fin')
    b.box(-0.72, 0.0, z1, 0.72, 0.30, 10.2, F('spine', cast(BLACK, pins=((0.15, 0.4), (0.85, 0.4)))), name='spine')
    # the fore grip, the front sling point, the muzzle and its emitter
    b.obox((0, -0.98, 7.35), (0.85, 2.1, 0.85), -14, F('fg', cast(BLACK, grad=0.12, lines=(('h', 0.85),))), name='fore_grip')
    b.box(-1.12, 0.9, 9.55, -0.98, 1.25, 9.95, F('qd', cast(STEEL)), name='sling_point')
    b.box(-0.95, 0.0, 10.2, 0.95, 2.78, 11.8, {'*': ('mz', cast(STEEL)), '+x': ('mz_p', slots(STEEL, 2, 0.45)),
                                               '-x': ('mz_p', slots(STEEL, 2, 0.45)), '+y': ('mz_t', slots(STEEL, 2, 0.45))}, name='muzzle')
    b.box(-0.6, 0.7, 11.8, 0.6, 1.9, 11.9, F('lens%.2f' % level, core(level)), glow=level > 0, name='emitter')
    holo(b)
    return b.parts


# ------------------------------------------------------------------ the item
FACE_OF = {'+x': 'north', '-x': 'south', '+y': 'up', '-y': 'down', '+z': 'east', '-z': 'west'}


def to_model(p):
    x, y, z = p
    return (z + 8, y + 8, 8 - x)


def face_dims(part, side):
    """A face's (width, height) in rifle px, as Minecraft lays its texture: sides along the barrel then up; the ends across
    then up; the top and bottom along the barrel then across."""
    (x0, y0, z0), (x1, y1, z1) = part.lo, part.hi
    dx, dy, dz = x1 - x0, y1 - y0, z1 - z0
    return {'+x': (dz, dy), '-x': (dz, dy), '+z': (dx, dy), '-z': (dx, dy), '+y': (dz, dx), '-y': (dz, dx)}[side]


class Atlas:
    """The item's texture: each painted face once (a face the same key and size shares its texels), packed in rows."""

    def __init__(self, width=256):
        self.w = width
        self.img = np.zeros((1024, width, 4), np.float32)
        self.x = self.y = self.row = 0
        self.at = {}

    def place(self, key, W, H, painter):
        k = (key, W, H)
        if k in self.at:
            return self.at[k]
        if self.x + W > self.w:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        a = np.ones((H, W, 4), np.float32)
        painter(a)
        self.img[self.y:self.y + H, self.x:self.x + W] = a
        self.at[k] = (self.x, self.y)
        self.x += W
        self.row = max(self.row, H)
        return self.at[k]

def elements(parts, atlas: Atlas, size):
    """The model's elements (uv in sixteenths of the texture's width and height: it is size = (width, height) texels)."""
    out = []
    for p in parts:
        lo, hi = to_model(p.lo), to_model(p.hi)
        frm = [round(min(a, b), 4) for a, b in zip(lo, hi)]
        to = [round(max(a, b), 4) for a, b in zip(lo, hi)]
        faces = {}
        for side in (p.only or ('+x', '-x', '+y', '-y', '+z', '-z')):
            key, fn = p.faces.get(side, p.faces.get('*'))
            fw, fh = face_dims(p, side)
            W, H = max(1, round(fw * p.tpx)), max(1, round(fh * p.tpx))
            x, y = atlas.place(key, W, H, fn)
            faces[FACE_OF[side]] = {"uv": [round(x * 16 / size[0], 5), round(y * 16 / size[1], 5), round((x + W) * 16 / size[0], 5),
                                           round((y + H) * 16 / size[1], 5)], "texture": "#0"}
        e = {"name": p.name, "from": frm, "to": to, "faces": faces}
        if p.angle:
            # the rifle's x is the model's -z: a turn about it (y toward z) is the opposite turn about the model's z
            e["rotation"] = {"origin": [round(v, 4) for v in to_model(p.centre())], "axis": "z", "angle": round(-p.angle, 4)}
        if p.glow:
            e["light_emission"] = 15
        out.append(e)
    return out


def display(s3=0.95):
    """Held: in third person in the raised gun hand (client GunPose raises the arm along the look, the other on the fore
    grip): the barrel out along the arm, the top up, the grip's top (the item's centre) at the fist; in first person
    nothing (client/PlasmaRifleView places it: the grip's top is where its numbers start); in the menus on the slant."""
    ident = {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}
    return {"thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [s3] * 3},
            "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [s3] * 3},
            "firstperson_righthand": ident, "firstperson_lefthand": ident,
            "gui": {"rotation": [0, 0, 32], "translation": gui_shift(0, 32, 0.66), "scale": [0.66] * 3},
            "ground": {"rotation": [0, 0, 0], "translation": [round(v, 3) for v in np.array(gui_shift(0, 0, 0.45)) + (0, 2, 0)], "scale": [0.45] * 3},
            "fixed": {"rotation": [0, 180, 32], "translation": gui_shift(180, 32, 0.66), "scale": [0.66] * 3}}


def gui_shift(ry, rz, scale):
    """The translation (px) that brings the rifle's middle (not the grip's top, the item's centre) to the middle of a
    slot or frame, for the display rotation [0, ry, rz] (JOML's XYZ: z first) and scale."""
    parts = rifle()
    lo = np.min([to_model(p.lo) for p in parts] + [to_model(p.hi) for p in parts], axis=0)
    hi = np.max([to_model(p.lo) for p in parts] + [to_model(p.hi) for p in parts], axis=0)
    c = (lo + hi) / 2 - 8
    a, b = math.radians(rz), math.radians(ry)
    Rz = np.array([[math.cos(a), -math.sin(a), 0], [math.sin(a), math.cos(a), 0], [0, 0, 1]])
    Ry = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    return [round(float(v), 3) for v in -(Ry @ Rz @ c) * scale]


def item_models():
    """The models, one a state, and their shared texture."""
    atlas = Atlas(TEX_W)
    built = {name: elements(rifle(level, bars), atlas, (TEX_W, TEX_H)) for name, level, bars, _ in STATES}
    assert atlas.y + atlas.row <= TEX_H, "the NT-7's texture is full (%d rows)" % (atlas.y + atlas.row)
    tex = (np.clip(atlas.img[:TEX_H], 0, 1) * 255).round().astype(np.uint8)
    models = {}
    for name, els in built.items():
        models[name] = {"textures": {"0": "jugcraft:item/plasma_rifle", "particle": "jugcraft:item/plasma_rifle"},
                        "elements": els, "display": display()}
    return models, Image.fromarray(tex, "RGBA")


def model_id(name):
    return "jugcraft:item/plasma_rifle" + ("" if name == "ready" else "_" + name)


def definition():
    """The item: the state by jugcraft:plasma_charge (the cooldown left: 1 at the shot, 0 ready)."""
    entries = [{"threshold": t, "model": {"type": "minecraft:model", "model": model_id(n)}} for n, _, _, t in STATES if t is not None]
    return {"model": {"type": "minecraft:range_dispatch", "property": "jugcraft:plasma_charge", "entries": sorted(entries, key=lambda e: e["threshold"]),
                      "fallback": {"type": "minecraft:model", "model": model_id("ready")}}}


def reticle_texture(n=128, ring=0.30, width=0.035, dot=0.035):
    """The holographic reticle (textures/gui/plasma_reticle.png): a ring with four ticks and a centre dot, violet, its
    edges soft (drawn over the middle of the screen while aiming)."""
    ss = 4
    N = n * ss
    y, x = (np.mgrid[0:N, 0:N] + 0.5) / N
    r = np.hypot(x - 0.5, y - 0.5)
    on = (np.abs(r - ring) < width / 2) | (r < dot / 2)
    for (cx, cy, hw, hh) in ((0.5, 0.5 - ring, 0.018, 0.055), (0.5, 0.5 + ring, 0.018, 0.055), (0.5 - ring, 0.5, 0.055, 0.018), (0.5 + ring, 0.5, 0.055, 0.018)):
        on |= (np.abs(x - cx) < hw) & (np.abs(y - cy) < hh)
    a = on.reshape(n, ss, n, ss).mean(axis=(1, 3))
    img = np.zeros((n, n, 4), np.uint8)
    img[..., 0], img[..., 1], img[..., 2] = 210, 160, 255
    img[..., 3] = np.clip(a * 255, 0, 255).astype(np.uint8)
    return Image.fromarray(img, "RGBA")


def write(assets=ASSETS):
    assets = Path(assets)
    models, tex = item_models()
    (assets / "items").mkdir(parents=True, exist_ok=True)
    (assets / "items/plasma_rifle.json").write_text(json.dumps(definition(), indent=2) + "\n")
    for name, m in models.items():
        (assets / ("models/item/" + model_id(name).split("/")[-1] + ".json")).write_text(json.dumps(m, indent=1) + "\n")
    tex.save(assets / "textures/item/plasma_rifle.png", optimize=True)
    (assets / "textures/gui").mkdir(parents=True, exist_ok=True)
    reticle_texture().save(assets / "textures/gui/plasma_reticle.png", optimize=True)


if __name__ == "__main__":
    write()
    print("NT-7 written")
