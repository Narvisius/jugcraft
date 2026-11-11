"""The tower's flight planning office (work list item 22 part B, room 5), to the standard Justin set on the lab (take 2):
everything at its real size (a pixel is 6.25 cm), modelled rather than painted on, the screens live in game, materials
at one density and seamless, things varied.

Pieces: dispatch_counter (3 x 1), chart_table (2 x 1, its top lit), strip_board (2 x 1, on a wall), notam_board (2 x 1,
on a wall), area_chart (3 x 2, on a wall), zone_clocks (2 x 1, on a wall), wx_kiosk (two high), chart_bin, binder_shelf
(2 x 1). With them the tower's console desks and wall screen, and the lab's plan chest and plotter.
Pieces wider than a block are built whole and cut into parts (part=0..); taller ones into halves (half=lower|upper).
"""
from __future__ import annotations

import json
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import model_writer as MW
import training_kit as TK
import manager_kit as MK
import lab_kit as LK
import rest_kit as RK
from training_kit import box, mat, faces, round_part, round_ring, cut_parts, swap_yz, TX, FONT, FONT_R, FACINGS

ROOT = Path(__file__).resolve().parent.parent
A = None        # the pictures atlas (16 texels a pixel)
SCR = None      # the screens and prints atlas (64 texels a pixel)
MAP = None      # the charts' atlas (32 texels a pixel)
MONO = MK.MONO
swap_xy, rod_x = LK.swap_xy, LK.rod_x
cable, cut_y, bevel_box, label_plate = RK.cable, RK.cut_y, RK.bevel_box, RK.label_plate
_fit = MK._text_fit


class ShelfAtlas(TK.Atlas):
    """An Atlas that packs its pictures on shelves by height: each goes on the first shelf tall enough (and not far too
    tall) with room left on it, or opens a new shelf under the others."""

    def __init__(self, size, tx, key):
        super().__init__(size, tx, key)
        self.shelves = []             # [y, height, x used]

    def region(self, key, w, h, draw):
        if key not in self.regions:
            tw, th = round(w * self.tx), round(h * self.tx)
            fits = [sh for sh in self.shelves if th <= sh[1] and sh[2] + tw <= self.size]
            tight = [sh for sh in fits if sh[1] <= th * 1.6 + 8]
            y_next = (self.shelves[-1][0] + self.shelves[-1][1] + 2) if self.shelves else 0
            spot = min(tight, key=lambda sh: sh[1]) if tight else None
            if spot is None and y_next + th > self.size and fits:
                spot = min(fits, key=lambda sh: sh[1])          # (no room for a new shelf: the best loose fit)
            if spot is None:
                y = (self.shelves[-1][0] + self.shelves[-1][1] + 2) if self.shelves else 0
                assert y + th <= self.size, ("atlas full", key)
                spot = [y, th, 0]
                self.shelves.append(spot)
            x, y = spot[2], spot[0]
            sub = TK.drawn(self, key, draw, (tw, th))
            self.img.paste(sub, (x, y))
            k = 16 / self.size
            self.regions[key] = [round(x * k, 5), round(y * k, 5), round((x + tw) * k, 5), round((y + th) * k, 5)]
            spot[2] += tw + 2
            self.y, self.row = spot[0], spot[1]
            if self.shelves:
                last = self.shelves[-1]
                self.y, self.row = last[0], last[1]
        return self.regions[key]


# ------------------------------------------------------------------ materials
def cork(seed=3501):
    """Cork board: warm brown with dark and light granules."""
    rng = np.random.default_rng(seed)
    a = np.stack([np.full((256, 256), c, float) for c in (176, 128, 84)], -1) + TK._noise(seed, 6)[..., None]
    a[rng.random((256, 256)) < 0.12] *= 0.78
    a[rng.random((256, 256)) < 0.08] *= 1.14
    return TK._img(a)


def laminate(base, seed):
    """An office laminate: flat colour, a faint fine speckle."""
    a = np.stack([np.full((256, 256), c, float) for c in base], -1) + TK._noise(seed, 1.2)[..., None]
    a[np.random.default_rng(seed + 1).random((256, 256)) < 0.03] *= 0.96
    return TK._img(a)


MATERIALS = {
    "pk_cork": lambda: cork(), "pk_laminate": lambda: laminate((214, 214, 208), 3502), "pk_dark": lambda: laminate((58, 62, 68), 3503),
    "pk_frosted": lambda: TK.solid((206, 214, 210), 3504, 0.8), "pk_holder_blue": lambda: TK.solid((52, 98, 176), 3505, 1.2),
    "pk_holder_green": lambda: TK.solid((54, 140, 80), 3506, 1.2), "pk_holder_yellow": lambda: TK.solid((222, 186, 40), 3507, 1.2),
    "pk_holder_red": lambda: TK.solid((180, 46, 40), 3508, 1.2), "pk_holder_white": lambda: TK.solid((226, 228, 226), 3509, 1.2),
    "pk_strip": lambda: TK.solid((240, 232, 206), 3510, 1.0), "pk_rubber_band": lambda: TK.solid((170, 120, 70), 3511, 1.5),
    "pk_pencil": lambda: TK.solid((232, 186, 30), 3512, 1.5), "pk_eraser": lambda: TK.solid((222, 140, 150), 3513, 1.5),
    "pk_acrylic": lambda: MK.rgba((226, 240, 246), 120, 3514), "pk_wire": lambda: TK.solid((38, 40, 44), 3515, 1.0),
    "pk_powder": lambda: laminate((190, 192, 194), 3516), "pk_manila": lambda: TK.solid((216, 190, 140), 3517, 1.5),
    "pk_susp": lambda: TK.solid((74, 118, 82), 3518, 1.5),
    "pk_mfp": lambda: laminate((222, 224, 226), 3519), "pk_mfp_dark": lambda: laminate((64, 68, 74), 3530),
}
BINDER_COLOURS = {"navy": (36, 52, 96), "red": (160, 40, 36), "green": (40, 110, 70), "black": (34, 34, 38), "grey": (120, 124, 130),
                  "blue": (50, 100, 170), "orange": (210, 110, 30)}
MATERIALS.update({f"pk_bnd_{k}": (lambda c=c, k=k: TK.solid(c, 3520 + len(k), 1.2)) for k, c in BINDER_COLOURS.items()})


# ------------------------------------------------------------------ the region's chart (one map, cropped for each print)
WORLD = (60.0, 37.3)                 # km across and down
TOWER = (30.0, 18.65)
WAYPOINTS = {"ALPHA": (44.0, 18.65), "BRAVO": (40.0, 8.65), "CHARLIE": (30.0, 5.0), "DELTA": (21.0, 27.65),
             "ECHO": (14.0, 18.65), "FOXTROT": (38.0, 26.65)}
TOWNS = [("MILLBROOK", (18.0, 9.0), 1.6), ("KESTREL BAY", (47.0, 30.0), 2.2), ("HOLLOW RIDGE", (9.0, 29.0), 1.1),
         ("ASHFORD", (41.0, 15.0), 1.0), ("WENDLE", (24.0, 31.0), 0.9), ("CARRICK", (52.0, 8.0), 1.2)]


def _vnoise(kx, ky, cell, seed):
    """Value noise over the world (km), smooth (bilinear between lattice values): the same at any crop."""
    g = np.random.default_rng(seed).random((int(WORLD[1] / cell) + 3, int(WORLD[0] / cell) + 3))
    fx, fy = kx / cell, ky / cell
    ix, iy = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - ix, fy - iy
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    ix, iy = np.clip(ix, 0, g.shape[1] - 2), np.clip(iy, 0, g.shape[0] - 2)
    a = g[iy, ix] * (1 - tx) + g[iy, ix + 1] * tx
    b = g[iy + 1, ix] * (1 - tx) + g[iy + 1, ix + 1] * tx
    return a * (1 - ty) + b * ty


def _elev(kx, ky):
    e = sum(_vnoise(kx, ky, c, 3600 + k) * w for k, (c, w) in enumerate(((14.0, 1.0), (6.0, 0.5), (2.6, 0.25), (1.1, 0.12))))
    e = e / 1.87
    e += 0.25 * np.clip((ky - 24.0) / 14.0, 0, 1) * np.clip((22.0 - kx) / 22.0, 0, 1) * 2.0      # the ridge to the south-west
    e -= 0.30 * np.clip(1 - np.hypot(kx - 50.0, ky - 33.0) / 9.0, 0, 1)                         # the bay
    return e


def draw_map(view, labels=1.0, route=False, tone=1.0):
    """A VFR drone chart of the tower's region (km window view = x0, y0, x1, y1): hypsometric tints and forests, the
    lakes and the river, the roads and towns, the control zone, the restricted and danger areas, the drone corridors
    and waypoints, the tower, the grid and its numbers; route: a pencilled route with its time marks."""
    x0, y0, x1, y1 = view

    def draw(img):
        W, H = img.size
        sx, sy = W / (x1 - x0), H / (y1 - y0)
        P = lambda kx, ky: ((kx - x0) * sx, (ky - y0) * sy)
        jj, ii = np.meshgrid((np.arange(W) + 0.5) / sx + x0, (np.arange(H) + 0.5) / sy + y0)
        e = _elev(jj, ii)
        bands = [(0.30, (150, 190, 228)), (0.40, (204, 224, 184)), (0.48, (222, 230, 186)), (0.56, (232, 226, 174)),
                 (0.64, (232, 212, 164)), (0.72, (220, 192, 148)), (9.0, (200, 170, 130))]
        a = np.zeros((H, W, 3))
        lo = -9.0
        for hi, c in bands:
            a[(e >= lo) & (e < hi)] = c
            lo = hi
        forest = (_vnoise(jj, ii, 1.6, 3700) > 0.62) & (e > 0.36) & (e < 0.66)
        a[forest] = a[forest] * 0.86 + np.array([96, 150, 90]) * 0.14
        # contour lines at the band edges
        for t in (0.40, 0.48, 0.56, 0.64, 0.72):
            edge = (e[:, 1:] >= t) != (e[:, :-1] >= t)
            a[:, 1:][edge] = a[:, 1:][edge] * 0.72
        shore = ((e[:, 1:] >= 0.30) != (e[:, :-1] >= 0.30))
        a[:, 1:][shore] = (70, 110, 170)
        img.paste(TK._img(a), (0, 0))
        d = ImageDraw.Draw(img)
        lw = max(1, round(sx * 0.08))
        f = lambda s: ImageFont.truetype(FONT, max(7, round(sx * s * labels)))
        fr = lambda s: ImageFont.truetype(FONT_R, max(7, round(sx * s * labels)))
        # the river (into the bay)
        river = [(2, 14), (8, 15.5), (13, 13.5), (19, 15.0), (25, 22.5), (31, 24.5), (37, 23.0), (43, 26.5), (47, 31)]
        wig = []
        for (ax, ay), (bx, by) in zip(river, river[1:]):
            ln = math.hypot(bx - ax, by - ay)
            ux, uy = -(by - ay) / ln, (bx - ax) / ln
            for k in range(int(ln * 4)):
                t = k / (ln * 4)
                w = 0.45 * math.sin(t * math.tau * 2.0 + ax) + 0.2 * math.sin(t * math.tau * 5.0 + ay)
                wig.append((ax + (bx - ax) * t + ux * w * math.sin(math.pi * t), ay + (by - ay) * t + uy * w * math.sin(math.pi * t)))
        wig.append(river[-1])
        d.line([P(*p) for p in wig], fill=(70, 120, 190), width=lw * 2, joint="curve")
        # roads: the motorway (yellow, edged), the main roads (red-brown)
        mway = [(0, 33), (12, 27), (22, 24), (30, 21.5), (40, 17.5), (52, 9), (60, 6)]
        d.line([P(*p) for p in mway], fill=(60, 50, 40), width=lw * 4, joint="curve")
        d.line([P(*p) for p in mway], fill=(244, 206, 70), width=lw * 2, joint="curve")
        for road in ([(18, 9), (24, 12), (30, 18.65)], [(30, 18.65), (36, 25), (47, 30)], [(9, 29), (16, 25), (24, 31)], [(41, 15), (52, 8)],
                     [(18, 9), (10, 4), (2, 3)], [(30, 18.65), (30, 37.3)]):
            d.line([P(*p) for p in road], fill=(250, 246, 236), width=lw + 3, joint="curve")
            d.line([P(*p) for p in road], fill=(186, 74, 52), width=lw + 1, joint="curve")
        # the railway (black with ticks)
        rail = [(0, 21), (14, 20.5), (28, 16.2), (44, 12.5), (60, 13)]
        d.line([P(*p) for p in rail], fill=(30, 30, 30), width=lw)
        for (ax, ay), (bx, by) in zip(rail, rail[1:]):
            n = int(math.hypot(bx - ax, by - ay) / 1.2)
            for k in range(n):
                t = (k + 0.5) / n
                cx, cy = ax + (bx - ax) * t, ay + (by - ay) * t
                ux, uy = -(by - ay), bx - ax
                ln = math.hypot(ux, uy)
                d.line([P(cx - ux / ln * 0.25, cy - uy / ln * 0.25), P(cx + ux / ln * 0.25, cy + uy / ln * 0.25)], fill=(30, 30, 30), width=lw)
        # towns: yellow with a black edge, their names
        for name, (tx, ty), r in TOWNS:
            pts = [P(tx + r * math.cos(t) * (1 + 0.25 * math.sin(3 * t + tx)), ty + r * 0.8 * math.sin(t) * (1 + 0.2 * math.cos(2 * t + ty))) for t in np.linspace(0, math.tau, 24)]
            d.polygon(pts, fill=(250, 226, 96), outline=(40, 36, 30))
            d.text(P(tx, ty - r - 0.5), name, font=f(0.62), fill=(20, 20, 24), anchor="ms")
        # the grid (every 5 km) and its numbers
        for gx in range(5, int(WORLD[0]), 5):
            if x0 < gx < x1:
                d.line([P(gx, y0), P(gx, y1)], fill=(60, 60, 90), width=1)
                d.text((P(gx, y0)[0] + 2, 2), f"{gx:02d}", font=fr(0.5), fill=(40, 40, 80))
        for gy in range(5, int(WORLD[1]), 5):
            if y0 < gy < y1:
                d.line([P(x0, gy), P(x1, gy)], fill=(60, 60, 90), width=1)
                d.text((2, P(x0, gy)[1] + 2), f"{gy:02d}", font=fr(0.5), fill=(40, 40, 80))
        # the airspace: the control zone (blue, dashed), the restricted area (red, hatched), the danger area
        def ring(cx, cy, r, col, dash, width):
            n = int(r * 9)
            for k in range(n):
                if dash and k % 2:
                    continue
                t0, t1 = k / n * math.tau, (k + 1) / n * math.tau
                d.line([P(cx + r * math.cos(t0), cy + r * math.sin(t0)), P(cx + r * math.cos(t1), cy + r * math.sin(t1))], fill=col, width=width)
        ring(*TOWER, 8.0, (40, 80, 190), True, lw * 2)
        d.text(P(TOWER[0] - 5.6, TOWER[1] + 6.9), "TRI-PILLAR CTR", font=f(0.62), fill=(40, 80, 190))
        d.text(P(TOWER[0] - 5.6, TOWER[1] + 7.7), "SFC - 1500 FT", font=fr(0.55), fill=(40, 80, 190))
        for (cx, cy, r, name, txt) in ((51.0, 21.0, 3.2, "R-101", "SFC-2000"), (11.0, 9.5, 2.6, "D-204", "SFC-1200")):
            ring(cx, cy, r, (196, 40, 40), False, lw * 2)
            for k in range(48):
                t = k / 48 * math.tau
                d.line([P(cx + r * math.cos(t), cy + r * math.sin(t)), P(cx + (r - 0.45) * math.cos(t + 0.06), cy + (r - 0.45) * math.sin(t + 0.06))], fill=(196, 40, 40), width=max(1, lw))
            d.text(P(cx, cy - 0.2), name, font=f(0.7), fill=(180, 30, 30), anchor="mm")
            d.text(P(cx, cy + 0.8), txt, font=fr(0.5), fill=(180, 30, 30), anchor="mm")
        # the drone corridors (magenta) and their waypoints (blue triangles)
        for name, (wx, wy) in WAYPOINTS.items():
            d.line([P(*TOWER), P(wx, wy)], fill=(196, 40, 150), width=lw * 3)
            dx, dy = wx - TOWER[0], wy - TOWER[1]
            ln = math.hypot(dx, dy)
            mx, my = TOWER[0] + dx * 0.55, TOWER[1] + dy * 0.55
            ux, uy = dx / ln, dy / ln
            d.polygon([P(mx + ux * 0.7, my + uy * 0.7), P(mx - uy * 0.4, my + ux * 0.4), P(mx + uy * 0.4, my - ux * 0.4)], fill=(196, 40, 150))
            tri = [P(wx, wy - 0.7), P(wx - 0.62, wy + 0.42), P(wx + 0.62, wy + 0.42)]
            d.polygon(tri, fill=(255, 255, 255), outline=(30, 60, 170))
            d.text(P(wx + 0.9, wy - 0.6), name, font=f(0.6), fill=(30, 60, 170))
            d.text(P(wx + 0.9, wy + 0.2), f"{round((math.degrees(math.atan2(dx, -dy))) % 360):03d}\N{DEGREE SIGN}  {ln:.1f} KM", font=fr(0.46), fill=(30, 60, 170))
        # the tower: a magenta ring with its mark, the compass rose round it
        tx, ty = TOWER
        ring(tx, ty, 2.3, (30, 60, 170), False, 1)
        for k in range(36):
            t = math.radians(k * 10)
            r0 = 2.0 if k % 9 else 1.6
            d.line([P(tx + r0 * math.sin(t), ty - r0 * math.cos(t)), P(tx + 2.3 * math.sin(t), ty - 2.3 * math.cos(t))], fill=(30, 60, 170), width=1)
        d.ellipse([P(tx - 0.55, ty - 0.55), P(tx + 0.55, ty + 0.55)], fill=(196, 40, 150), outline=(80, 20, 60))
        d.text(P(tx, ty + 1.2), "TRI-PILLAR", font=f(0.66), fill=(110, 20, 80), anchor="mt")
        if route:
            pts = [TOWER, (35.0, 13.65), WAYPOINTS["BRAVO"]]
            d.line([P(*p) for p in pts], fill=(70, 70, 76), width=max(1, lw // 2 + 1))
            for (px, py), t in zip(pts[1:], ("+04", "+07")):
                d.line([P(px - 0.35, py - 0.35), P(px + 0.35, py + 0.35)], fill=(70, 70, 76), width=1)
                d.text(P(px + 0.5, py), t, font=fr(0.5), fill=(70, 70, 76))
        if tone != 1.0:                        # (a print lying flat under the lights reads a shade darker than one on a wall)
            img.paste(TK._img(np.asarray(img, float) * tone), (0, 0))
        return img
    return draw


def chart_title(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(250, 250, 246), outline=(30, 30, 34), width=max(1, H // 40))
    d.rectangle((0, 0, W, H * 0.3), fill=(30, 60, 170))
    d.text((W / 2, H * 0.15), "TRI-PILLAR CONTROL ZONE", font=_fit(d, "TRI-PILLAR CONTROL ZONE", FONT, round(H * 0.2), W - 8), fill=(255, 255, 255), anchor="mm")
    for k, t in enumerate(("VFR DRONE CHART  1:50 000", "EDITION 7  -  OCT 2026", "NOT FOR NAVIGATION OUTSIDE THE CTR")):
        d.text((W / 2, H * (0.45 + k * 0.18)), t, font=_fit(d, t, FONT if k == 0 else FONT_R, round(H * 0.13), W - 10), fill=(30, 30, 34), anchor="mm")
    return img


def chart_legend(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(250, 250, 246), outline=(30, 30, 34), width=max(1, H // 60))
    d.text((W * 0.06, H * 0.08), "LEGEND", font=ImageFont.truetype(FONT, round(H * 0.08)), fill=(30, 30, 34), anchor="lm")
    rows = [((196, 40, 150), "line", "DRONE CORRIDOR"), ((30, 60, 170), "tri", "WAYPOINT"), ((40, 80, 190), "dash", "CONTROL ZONE"),
            ((196, 40, 40), "line", "RESTRICTED / DANGER"), ((244, 206, 70), "line", "MOTORWAY"), ((176, 70, 52), "line", "MAIN ROAD"),
            ((70, 120, 190), "line", "RIVER")]
    f = ImageFont.truetype(FONT_R, round(H * 0.065))
    for k, (c, kind, t) in enumerate(rows):
        y = H * (0.2 + k * 0.11)
        if kind == "tri":
            d.polygon([(W * 0.12, y - H * 0.03), (W * 0.08, y + H * 0.03), (W * 0.16, y + H * 0.03)], outline=c, fill=(255, 255, 255))
        else:
            for s in range(4 if kind == "dash" else 1):
                xa = W * 0.06 + s * W * 0.03
                d.line((xa, y, xa + (W * 0.02 if kind == "dash" else W * 0.12), y), fill=c, width=max(2, round(H * 0.02)))
        d.text((W * 0.24, y), t, font=f, fill=(30, 30, 34), anchor="lm")
    return img


# ------------------------------------------------------------------ the prints and screens
def strip_pic(call, kind, route, etd, alt, band):
    """A flight progress strip: the coloured band, the callsign, the type, the route, the time, the height."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(242, 236, 214))
        d.rectangle((0, 0, W * 0.05, H), fill=band)
        for x in (0.3, 0.44, 0.78):
            d.line((W * x, 0, W * x, H), fill=(150, 140, 120), width=1)
        d.text((W * 0.07, H * 0.5), call, font=_fit(d, call, FONT, round(H * 0.5), W * 0.21), fill=(20, 20, 24), anchor="lm")
        d.text((W * 0.37, H * 0.5), kind, font=_fit(d, kind, MONO, round(H * 0.36), W * 0.12), fill=(40, 40, 44), anchor="mm")
        d.text((W * 0.46, H * 0.3), route, font=_fit(d, route, MONO, round(H * 0.32), W * 0.31), fill=(40, 40, 44), anchor="lm")
        d.text((W * 0.46, H * 0.74), alt, font=_fit(d, alt, MONO, round(H * 0.3), W * 0.31), fill=(90, 90, 96), anchor="lm")
        d.text((W * 0.97, H * 0.5), etd, font=_fit(d, etd, FONT, round(H * 0.5), W * 0.18), fill=(150, 30, 30), anchor="rm")
        return img
    return draw


def bay_label(text):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(26, 28, 32))
        d.text((W / 2, H / 2), text, font=_fit(d, text, FONT, round(H * 0.62), W - 8), fill=(236, 180, 60), anchor="mm")
        return img
    return draw


NOTAMS = [
    ["XTPL NOTAM A0412/26", "Q) XTPL/QRRCA/IV/BO/W/000/020", "A) XTPL", "B) 2610230600 C) 2610231800", "E) RESTRICTED AREA R-101", "ACTIVE. CRANE WORKS AT", "THE KESTREL BAY QUAY.", "F) SFC  G) 2000FT AMSL"],
    ["XTPL NOTAM A0415/26", "Q) XTPL/QOBCE/IV/M/A/000/006", "A) XTPL", "B) 2610211200 C) 2611042359", "E) OBST ERECTED: MAST", "184FT AGL 1.2KM W OF", "WP ECHO. LIT RED.", "F) SFC  G) 600FT AMSL"],
    ["XTPL NOTAM A0418/26", "Q) XTPL/QWDLW/IV/BO/W/000/012", "A) XTPL", "B) 2610230900 C) 2610231500", "E) DANGER AREA D-204", "ACTIVE: FIREWORKS DISPLAY", "TEST, MILLBROOK FIELD.", "F) SFC  G) 1200FT AMSL"],
    ["XTPL NOTAM A0420/26", "Q) XTPL/QNMAS/IV/BO/AE/000/015", "A) XTPL", "B) 2610220000 C) 2610252359", "E) DRONE CORRIDOR DELTA", "WIDTH REDUCED TO 300M", "BTN KM 4 AND KM 7.", "F) SFC  G) 1500FT AMSL"],
    ["XTPL NOTAM A0421/26", "Q) XTPL/QFALC/IV/NBO/A/000/999", "A) XTPL", "B) 2610240600 C) 2610241000", "E) TOWER PAD 3 CLSD FOR", "RESURFACING. USE PAD 1", "OR 2 FOR ALL DEPARTURES.", ""],
    ["XTPL NOTAM A0423/26", "Q) XTPL/QWULW/IV/BO/W/000/004", "A) XTPL", "B) 2610231300 C) 2610231700", "E) MODEL AIRCRAFT EVENT", "HOLLOW RIDGE PARK", "R 500M.", "F) SFC  G) 400FT AGL"],
]


def notam_sheet(lines, highlight=None):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(250, 250, 248))
        d.rectangle((0, 0, W, H * 0.07), fill=(40, 46, 56))
        d.text((W * 0.05, H * 0.035), "AERONAUTICAL INFORMATION", font=_fit(d, "AERONAUTICAL INFORMATION", FONT, round(H * 0.04), W * 0.9), fill=(255, 255, 255), anchor="lm")
        fm = ImageFont.truetype(MONO, max(7, round(H * 0.036)))
        y = H * 0.12
        for k, t in enumerate(lines):
            if highlight == k:
                d.rectangle((W * 0.04, y - H * 0.008, W * 0.96, y + H * 0.044), fill=(250, 240, 110))
            d.text((W * 0.05, y), t, font=_fit(d, t, MONO, round(H * 0.036), W * 0.9) if t else fm, fill=(24, 24, 28))
            y += H * 0.056
        for k in range(9):
            d.line((W * 0.05, y + k * H * 0.04, W * (0.95 if k % 3 else 0.6), y + k * H * 0.04), fill=(200, 200, 204), width=max(1, H // 300))
        d.text((W * 0.95, H * 0.97), "XTPL AIS", font=ImageFont.truetype(FONT_R, max(7, round(H * 0.025))), fill=(120, 120, 126), anchor="rs")
        return img
    return draw


def map_sheet(img):
    """A NOTAM's sketch: the restricted area ringed red on a photocopied piece of the chart."""
    W, H = img.size
    sub = draw_map((44.0, 14.0, 58.0, 28.0), labels=1.6)(Image.new("RGB", (W, int(W))))
    g = np.asarray(sub.convert("L"), float)
    g = 120 + g * 0.5
    img.paste(TK._img(np.stack([g, g, g], -1)), (0, int(H * 0.12)))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H * 0.12), fill=(250, 250, 248))
    d.text((W / 2, H * 0.06), "R-101  ACTIVE TODAY", font=_fit(d, "R-101  ACTIVE TODAY", FONT, round(H * 0.06), W * 0.9), fill=(180, 30, 30), anchor="mm")
    d.rectangle((0, H * 0.12 + W, W, H), fill=(250, 250, 248))
    cx, cy, r = W * (51 - 44) / 14, H * 0.12 + W * (21 - 14) / 14, W * 3.6 / 14
    for k in range(3):
        d.ellipse((cx - r - k, cy - r - k, cx + r + k, cy + r + k), outline=(210, 30, 30))
    d.text((W * 0.05, H * 0.95), "crane jib 60m - keep clear", font=ImageFont.truetype(FONT_R, max(7, round(H * 0.04))), fill=(40, 60, 160), anchor="ls")
    return img


def header_card(title, sub, band):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=band)
        d.text((W / 2, H * 0.4), title, font=_fit(d, title, FONT, round(H * 0.46), W - 10), fill=(255, 255, 255), anchor="mm")
        d.text((W / 2, H * 0.8), sub, font=_fit(d, sub, FONT_R, round(H * 0.22), W - 10), fill=(240, 240, 240), anchor="mm")
        return img
    return draw


def sticky(text, col=(250, 236, 110)):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=col)
        d.rectangle((0, 0, W, H * 0.14), fill=tuple(int(c * 0.93) for c in col))
        y = H * 0.24
        for t in text.split("\n"):
            d.text((W * 0.08, y), t, font=_fit(d, t, FONT_R, round(H * 0.16), W * 0.86), fill=(40, 50, 120))
            y += H * 0.2
        return img
    return draw


DEPARTURES = [("TP417", "T1", "TWR-BRAVO", "14:40", "BRIEFED"), ("TP418", "T2", "TWR-ALPHA", "14:45", "LOADING"),
              ("TP419", "T1", "TWR-DELTA", "14:52", "PLANNED"), ("TP420", "T2", "TWR-FOXTROT", "15:05", "PLANNED"),
              ("TP421", "T1", "TWR-ECHO", "15:10", "HOLD R-101"), ("TP422", "T2", "TWR-CHARLIE", "15:20", "PLANNED")]


def dispatch_screen(img):
    """The dispatcher's monitor (live in game: the tower's missions): today's departures and their state."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(10, 12, 16))
    d.rectangle((0, 0, W, H * 0.12), fill=(30, 34, 42))
    d.text((W * 0.03, H * 0.06), "DISPATCH", font=ImageFont.truetype(FONT, round(H * 0.07)), fill=(236, 160, 40), anchor="lm")
    TK.live_part(img, (W * 0.97, H * 0.06), "14:32  23 OCT", "14:32", ImageFont.truetype(MONO, round(H * 0.06)), (200, 200, 210), "rm",
                 bg=(30, 34, 42), src="time")
    fm = ImageFont.truetype(MONO, round(H * 0.055))
    cols = (0.03, 0.2, 0.3, 0.6, 0.74)
    for c, t in zip(cols, ("FLIGHT", "TYPE", "ROUTE", "ETD", "STATUS")):
        d.text((W * c, H * 0.19), t, font=fm, fill=(120, 130, 150), anchor="lm")
    for k, row in enumerate(DEPARTURES):
        y = H * (0.3 + k * 0.105)
        if k % 2 == 0:
            d.rectangle((0, y - H * 0.045, W, y + H * 0.045), fill=(18, 22, 28))
        for c, t in zip(cols, row):
            col = (236, 236, 240)
            if t in ("BRIEFED",):
                col = (90, 220, 120)
            elif t.startswith("HOLD"):
                col = (240, 90, 70)
            elif t == "LOADING":
                col = (236, 190, 60)
            d.text((W * c, y), t, font=fm, fill=col, anchor="lm")
    return img


def wx_screen(img):
    """The weather kiosk's screen (live in game: the tower's weather): the METAR and TAF, the wind, the rain radar, the
    drone flying state."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(12, 16, 24))
    d.rectangle((0, 0, W, H * 0.11), fill=(28, 46, 84))
    d.text((W * 0.03, H * 0.055), "WEATHER BRIEFING", font=ImageFont.truetype(FONT, round(H * 0.065)), fill=(255, 255, 255), anchor="lm")
    TK.live_part(img, (W * 0.97, H * 0.055), "XTPL  14:32", "14:32", ImageFont.truetype(MONO, round(H * 0.055)), (190, 210, 240), "rm",
                 bg=(28, 46, 84), src="time")
    fm = ImageFont.truetype(MONO, round(H * 0.042))
    fb = ImageFont.truetype(FONT, round(H * 0.045))
    d.text((W * 0.03, H * 0.18), "METAR", font=fb, fill=(120, 200, 255))
    d.text((W * 0.03, H * 0.25), "XTPL 231420Z 24012KT 9999", font=fm, fill=(230, 230, 236))
    d.text((W * 0.03, H * 0.31), "FEW030 SCT045 14/08 Q1016", font=fm, fill=(230, 230, 236))
    d.text((W * 0.03, H * 0.41), "TAF", font=fb, fill=(120, 200, 255))
    for k, t in enumerate(("XTPL 231100Z 2312/2412", "24012KT 9999 SCT035", "TEMPO 2316/2320 4000 SHRA", "BKN014 BECMG 2400/2402", "27008KT")):
        d.text((W * 0.03, H * (0.48 + k * 0.06)), t, font=fm, fill=(230, 230, 236))
    # the wind dial
    cx, cy, r = W * 0.6, H * 0.42, H * 0.2
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=(120, 140, 170), width=2)
    for k in range(12):
        t = math.radians(k * 30)
        d.line((cx + r * 0.85 * math.sin(t), cy - r * 0.85 * math.cos(t), cx + r * math.sin(t), cy - r * math.cos(t)), fill=(120, 140, 170), width=2)
    t = math.radians(240)
    d.line((cx, cy, cx + r * 0.8 * math.sin(t), cy - r * 0.8 * math.cos(t)), fill=(240, 200, 60), width=4)
    d.text((cx, cy + r + H * 0.05), "240 / 12 KT", font=fb, fill=(240, 200, 60), anchor="mm")
    # the rain radar
    rx0, ry0, rx1, ry1 = W * 0.74, H * 0.16, W * 0.97, H * 0.66
    d.rectangle((rx0, ry0, rx1, ry1), fill=(20, 40, 30), outline=(60, 90, 70))
    rng = random.Random(77)
    for _ in range(26):
        x, y, s = rng.uniform(rx0 + 8, rx1 - 30), rng.uniform(ry0 + 8, ry1 - 30), rng.uniform(6, 20)
        col = rng.choice(((60, 170, 80), (60, 170, 80), (210, 200, 60), (220, 100, 50)))
        d.ellipse((x, y, x + s * 1.4, y + s), fill=col)
    d.ellipse(((rx0 + rx1) / 2 - 4, (ry0 + ry1) / 2 - 4, (rx0 + rx1) / 2 + 4, (ry0 + ry1) / 2 + 4), fill=(255, 255, 255))
    d.text((rx0 + 6, ry1 - 6), "RADAR 50 KM", font=ImageFont.truetype(FONT_R, round(H * 0.035)), fill=(200, 220, 200), anchor="ls")
    # the flying state
    d.rectangle((W * 0.03, H * 0.82, W * 0.97, H * 0.95), fill=(26, 90, 50))
    d.text((W / 2, H * 0.885), "DRONE OPS: GREEN  -  SHOWERS AFTER 16:00", font=_fit(d, "DRONE OPS: GREEN  -  SHOWERS AFTER 16:00", FONT, round(H * 0.06), W * 0.9), fill=(255, 255, 255), anchor="mm")
    return img


def zone_face(label, time):
    """A zone clock's face: the red figures, its label under them."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(10, 6, 6))
        on, off = (255, 58, 36), (44, 14, 12)
        k = W / 12.8
        dw, dh = 1.75 * k, 3.0 * k
        y = 0.5 * k
        for n, ch in enumerate(time.replace(":", "")):
            TK.seven_seg(d, [1.0, 3.35, 6.95, 9.3][n] * k + 0.3 * k, y, dw, dh, ch, on, off)
        for cy in (y + dh * 0.32, y + dh * 0.68):
            d.ellipse((6.05 * k, cy - 0.17 * k, 6.39 * k, cy + 0.17 * k), fill=on)
        if label in ZONE_HOURS:                         # (sunset stays as it is)
            TK.live_clock(img, [x * k + 0.3 * k for x in (1.0, 3.35, 6.95, 9.3)], y, dw, dh, (6.05 * k, 6.39 * k), on, off, (10, 6, 6),
                          hours=ZONE_HOURS[label])
        return img
    return draw


def name_plate(text, bg=(230, 230, 226), fg=(24, 24, 28)):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=bg)
        d.text((W / 2, H / 2), text, font=_fit(d, text, FONT, round(H * 0.64), W - 6), fill=fg, anchor="mm")
        return img
    return draw


def binder_spine(text, colour):
    """A lever arch file's spine: its colour, the label window with the title along it, the finger hole."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=colour)
        d.rectangle((W * 0.14, H * 0.08, W * 0.86, H * 0.62), fill=(244, 242, 232))
        lab = Image.new("RGB", (int(H * 0.52), int(W * 0.7)), (244, 242, 232))
        dl = ImageDraw.Draw(lab)
        dl.text((lab.width / 2, lab.height / 2), text, font=_fit(dl, text, FONT, round(lab.height * 0.62), lab.width - 6), fill=(24, 24, 28), anchor="mm")
        img.paste(lab.rotate(90, expand=True), (int(W * 0.15), int(H * 0.09)))
        r = W * 0.22
        d.ellipse((W / 2 - r, H * 0.78 - r, W / 2 + r, H * 0.78 + r), fill=tuple(int(c * 0.35) for c in colour), outline=(200, 200, 204), width=max(1, W // 12))
        return img
    return draw


def plotter_scale(img):
    """A navigation plotter's print: the scales along its edges, its protractor, its name."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(214, 232, 238))
    for k in range(0, 101):
        x = W * 0.02 + k * W * 0.96 / 100
        ln = H * (0.22 if k % 10 == 0 else 0.14 if k % 5 == 0 else 0.08)
        d.line((x, 0, x, ln), fill=(30, 30, 40), width=1)
        d.line((x, H, x, H - ln), fill=(30, 30, 40), width=1)
    cx, cy, r = W / 2, H * 0.62, H * 0.32
    d.arc((cx - r, cy - r, cx + r, cy + r), 180, 360, fill=(30, 30, 40), width=1)
    for k in range(0, 181, 10):
        t = math.radians(k)
        d.line((cx - r * math.cos(t), cy - r * math.sin(t), cx - r * 0.85 * math.cos(t), cy - r * 0.85 * math.sin(t)), fill=(30, 30, 40), width=1)
    d.text((W * 0.08, H / 2), "VFR PLOTTER  1:50 000", font=ImageFont.truetype(FONT, max(7, round(H * 0.16))), fill=(30, 30, 40), anchor="lm")
    return img


def counter_sign(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(28, 30, 34))
    d.rectangle((0, H * 0.84, W, H), fill=(236, 150, 30))
    d.text((W / 2, H * 0.42), "FLIGHT DISPATCH", font=_fit(d, "FLIGHT DISPATCH", FONT, round(H * 0.42), W - 20), fill=(240, 240, 240), anchor="mm")
    return img


def kiosk_header(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(28, 46, 84))
    d.text((W / 2, H / 2), "WEATHER", font=_fit(d, "WEATHER", FONT, round(H * 0.6), W - 10), fill=(255, 255, 255), anchor="mm")
    return img


def pack_cover(n):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(236, 230, 210))
        d.rectangle((0, 0, W, H * 0.2), fill=(236, 150, 30))
        d.text((W / 2, H * 0.1), "BRIEFING PACK", font=_fit(d, "BRIEFING PACK", FONT, round(H * 0.12), W - 6), fill=(255, 255, 255), anchor="mm")
        d.text((W / 2, H * 0.45), DEPARTURES[n][0], font=_fit(d, DEPARTURES[n][0], FONT, round(H * 0.18), W - 6), fill=(24, 24, 28), anchor="mm")
        d.text((W / 2, H * 0.68), DEPARTURES[n][2], font=_fit(d, DEPARTURES[n][2], MONO, round(H * 0.1), W - 6), fill=(60, 60, 66), anchor="mm")
        return img
    return draw


def signout_sheet(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(250, 250, 248))
    d.text((W / 2, H * 0.05), "PILOT SIGN-OUT", font=_fit(d, "PILOT SIGN-OUT", FONT, round(H * 0.05), W - 10), fill=(24, 24, 28), anchor="mm")
    fm = ImageFont.truetype(MONO, max(7, round(H * 0.03)))
    for k in range(14):
        y = H * (0.12 + k * 0.06)
        d.line((W * 0.04, y + H * 0.045, W * 0.96, y + H * 0.045), fill=(170, 170, 176), width=1)
        if k < 5:
            d.text((W * 0.05, y + H * 0.01), f"{DEPARTURES[k][0]}  {DEPARTURES[k][3]}  ", font=fm, fill=(30, 40, 120))
            d.line((W * 0.6, y + H * 0.035, W * 0.68, y + H * 0.01, W * 0.75, y + H * 0.035, W * 0.86, y + H * 0.015), fill=(30, 40, 120), width=max(1, H // 300))
    return img


def rot180(draw):
    """A print turned to be read from the far side (the pilots' side of the counter)."""
    def f(img):
        return draw(img).rotate(180)
    return f


# ------------------------------------------------------------------ the dispatch counter
def dispatch_counter():
    """The flight dispatch counter (3 x 1, built whole along x 0..48; the dispatcher looks north at the pilots): the dark
    front on its recessed plinth with the FLIGHT DISPATCH sign on its standoffs, the oak transaction ledge on its
    brackets; on the ledge the sign-out clipboard, the pen on its coiled cord, the tray of briefing packs, the bell. The
    dispatcher's side: the work top, the drawer pedestal, the computer under it; the monitor (live in game: the day's
    departures), keyboard and mouse, the desk phone, the headset on its stand, the label printer printing a pack's
    cover, a document tray, a stamp and pad, a pen pot, a mug."""
    dk, lam = "dark", "laminate"
    out = [box((0.8, 0, 1.8), (47.2, 0.8, 3.0), mat("black")), box((0.8, 0.8, 1.0), (47.2, 17.0, 2.0), mat(dk)),
           box((0.0, 0, 1.0), (0.8, 11.6, 15.6), mat(dk)), box((47.2, 0, 1.0), (48.0, 11.6, 15.6), mat(dk)),
           box((0.0, 11.6, 1.0), (0.8, 17.0, 4.4), mat(dk)), box((47.2, 11.6, 1.0), (48.0, 17.0, 4.4), mat(dk)),
           box((0.0, 17.0, -0.6), (48.0, 17.8, 4.4), mat("oak")), box((0.8, 0.8, 0.9), (47.2, 1.6, 1.0), mat("alu")),
           {**box((0.6, 16.8, -0.5), (47.4, 17.0, -0.3), mat("light")), "shade": False}]
    for x in (8.0, 24.0, 40.0):
        out += [box((x - 0.15, 16.4, -0.4), (x + 0.15, 17.0, 1.0), mat("chrome")), box((x - 0.15, 15.4, 0.4), (x + 0.15, 16.4, 1.0), mat("chrome"))]
    out.append(box((14.0, 8.6, 0.6), (34.0, 13.6, 0.8), faces(dk, north=MAP.face("pk_sign", 20.0, 5.0, counter_sign))))
    for x in (14.8, 33.2):
        for y in (9.4, 12.8):
            out += swap_yz(round_part(x, y, 0.25, 0.8, 1.0, "chrome", n=3))
    # the dispatcher's side
    out += [box((0.8, 11.0, 2.0), (47.2, 11.6, 15.4), mat(lam)), box((38.4, 0.4, 3.0), (47.2, 11.0, 15.0), mat(lam))]
    for y0, y1 in ((7.8, 10.6), (4.4, 7.6), (0.6, 4.2)):
        out += [box((38.6, y0, 15.0), (47.0, y1, 15.3), mat(lam)), box((41.2, y1 - 0.9, 15.3), (44.4, y1 - 0.6, 15.6), mat("chrome"))]
    out += [box((2.0, 0, 6.0), (5.0, 7.4, 13.0), mat("black")), box((2.0, 6.2, 13.0), (5.0, 6.8, 13.05), mat("graphite")),
            box((3.3, 5.6, 13.0), (3.7, 5.8, 13.05), mat("light"))]
    t = 11.6
    out += [box((20.6, t, 6.0), (23.4, t + 0.3, 8.0), mat("graphite")), box((21.6, t + 0.3, 6.6), (22.4, t + 3.0, 7.2), mat("graphite")),
            box((17.6, t + 2.0, 7.2), (26.4, t + 7.4, 7.8), mat("black")),
            {**box((17.9, t + 2.3, 7.8), (26.1, t + 7.1, 7.85), faces("black", south=SCR.face("pk_disp", 8.2, 4.8, dispatch_screen))), "shade": False},
            box((18.0, t, 10.0), (26.0, t + 0.35, 12.6), faces("black", up=A.region("pk_kbd", 8.0, 2.6, MK.keyboard_top))),
            box((27.0, t, 10.6), (28.0, t + 0.4, 12.0), mat("black"))]
    out += [box((30.0, t, 6.0), (34.4, t + 0.6, 10.0), mat("black")),
            box((30.8, t + 0.4, 6.2), (34.0, t + 0.8, 9.8), faces("black", up=SCR.face("pk_phone", 3.2, 3.6, MK.phone_face)),
                {"angle": 22.5, "axis": "x", "origin": [32.4, t + 0.6, 8.0]}),
            box((29.6, t + 0.6, 6.4), (30.4, t + 1.2, 9.6), mat("black"))]
    # the headset on its stand
    out += round_part(37.0, 7.0, 0.8, t, t + 0.25, "graphite", n=3) + round_part(37.0, 7.0, 0.15, t + 0.25, t + 3.6, "chrome", n=3)
    out += [box((35.8, t + 3.4, 6.8), (38.2, t + 3.7, 7.2), mat("black")), box((35.6, t + 2.0, 6.6), (35.9, t + 3.6, 7.4), mat("black")),
            box((38.1, t + 2.0, 6.6), (38.4, t + 3.6, 7.4), mat("black")), box((35.2, t + 2.1, 6.4), (35.6, t + 3.2, 7.6), mat("black")),
            box((38.4, t + 2.1, 6.4), (38.8, t + 3.2, 7.6), mat("black"))]
    out += cable([(35.4, t + 2.3, 7.6), (35.4, t + 2.3, 9.2)], 0.06, "graphite") + [box((35.25, t + 2.15, 9.2), (35.55, t + 2.45, 9.6), mat("black"))]
    # the label printer and the pack cover coming out of it; the document tray; the stamp; the pen pot; the mug
    out += [box((5.0, t, 5.0), (9.0, t + 2.0, 9.0), mat("pla_black")), box((5.6, t + 2.0, 5.6), (8.4, t + 2.05, 7.4), mat("pla_grey")),
            box((5.8, t + 1.2, 4.2), (8.2, t + 1.25, 5.0), faces("paper", up=SCR.face("pk_cover_out", 2.4, 0.8, label_plate("TP417  BRAVO", (240, 236, 220))))),
            box((8.2, t + 1.4, 8.2), (8.6, t + 1.6, 8.4), mat("light"))]
    out += [box((9.8, t, 9.6), (13.8, t + 0.15, 14.4), mat("grip_black")), box((9.8, t + 0.15, 9.6), (13.8, t + 0.8, 9.75), mat("grip_black")),
            box((9.8, t + 0.15, 14.25), (13.8, t + 0.8, 14.4), mat("grip_black")),
            box((10.0, t + 0.15, 9.8), (13.6, t + 0.55, 14.2), faces("paper", up=A.region("pk_tray_sheet", 3.6, 4.4, MK.page_sheet(941))))]
    out += [box((14.4, t, 9.4), (16.6, t + 0.25, 11.0), mat("black")), box((14.6, t + 0.25, 9.6), (16.4, t + 0.3, 10.8), mat("red"))]
    out += round_part(17.2, 13.2, 0.45, t, t + 0.4, "graphite", n=3) + round_part(17.2, 13.2, 0.3, t + 0.4, t + 1.5, "oak", n=3)
    out += round_ring(42.0, 6.0, 0.6, 0.1, t, t + 1.4, "black", 0.2, "black", n=4)
    out += [box((41.8, t + 0.2, 5.8), (41.95, t + 2.2, 5.95), mat("glove_blue")), box((42.1, t + 0.2, 6.0), (42.25, t + 2.0, 6.15), mat("red"))]
    out += MK.round_ring(44.6, 9.6, 0.55, 0.1, t, t + 1.4, "mug_red", 1.0, "coffee", n=4) + [box((45.15, t + 0.3, 9.45), (45.5, t + 1.1, 9.75), mat("mug_red"))]
    # the ledge: the sign-out clipboard, the pen on its cord, the briefing packs, the bell
    y = 17.8
    out += [box((10.0, y, 0.2), (13.6, y + 0.12, 4.2), mat("cardboard")),
            box((10.2, y + 0.12, 0.6), (13.4, y + 0.16, 4.1), faces("paper", up=SCR.face("pk_signout", 3.2, 3.5, rot180(signout_sheet)))),
            box((11.2, y + 0.12, 0.3), (12.4, y + 0.36, 0.9), mat("chrome"))]
    out += [box((14.6, y, 1.0), (16.6, y + 0.28, 1.28), mat("navy")), box((14.3, y + 0.06, 1.06), (14.6, y + 0.22, 1.22), mat("chrome"))]
    out += round_part(17.6, 0.6, 0.35, y, y + 0.3, "chrome", n=3) + cable([(16.6, y + 0.1, 1.14), (17.6, y + 0.1, 1.14), (17.6, y + 0.1, 0.6)], 0.08, "coil")
    out += RK.lipped_tray(29.6, 34.4, -0.2, 4.0, y, base=0.15, lip=0.5, wall=0.15, tex="grip_black")
    for k in range(3):
        yy = y + 0.15 + k * 0.14
        out.append(box((29.9 + k * 0.1, yy, 0.0 + k * 0.08), (34.1 - k * 0.1, yy + 0.14, 3.8 - k * 0.06), faces("paper", up=SCR.face(f"pk_pack_{k}", 4.2 - k * 0.2, 3.8 - k * 0.14, rot180(pack_cover(k))))))
    out.append(box((30.0, y + 0.2, -0.24), (34.0, y + 0.6, -0.2), faces("grip_black", north=SCR.face("pk_packs_lbl", 4.0, 0.4, label_plate("BRIEFING PACKS", (236, 150, 30), (255, 255, 255))))))
    out += round_part(38.0, 2.0, 0.6, y, y + 0.2, "black", n=3) + round_part(38.0, 2.0, 0.5, y + 0.2, y + 0.55, "chrome", n=3)
    out += round_part(38.0, 2.0, 0.3, y + 0.55, y + 0.7, "chrome", n=3) + round_part(38.0, 2.0, 0.1, y + 0.7, y + 0.9, "chrome", n=3)
    return out


# ------------------------------------------------------------------ the chart table
TABLE_VIEW = (18.0, 13.5, 42.0, 23.65)


def chart_table():
    """The chart table (2 x 1, built whole along x 0..32, 95 cm: its users stand at its long sides): square steel legs on
    levellers, the apron, the lower shelf with folded charts on it; the aluminium-rimmed top, its frosted glass (lit from
    under when it is switched on in game); the chart spread on it (the control zone, a route pencilled on it), the plotter, the
    dividers, pencils, a highlighter, the flight computer, an eraser."""
    g = "graphite"
    out = []
    for x0 in (1.0, 30.0):
        for z0 in (1.6, 13.4):
            out += [box((x0, 0.4, z0), (x0 + 1.0, 14.0, z0 + 1.0), mat(g)), box((x0 + 0.2, 0, z0 + 0.2), (x0 + 0.8, 0.4, z0 + 0.8), mat("black"))]
    out += [box((2.0, 12.6, 1.8), (30.0, 14.0, 2.4), mat(g)), box((2.0, 12.6, 13.6), (30.0, 14.0, 14.2), mat(g)),
            box((1.2, 12.6, 2.6), (1.8, 14.0, 13.4), mat(g)), box((30.2, 12.6, 2.6), (30.8, 14.0, 13.4), mat(g)),
            box((2.0, 3.0, 2.6), (30.0, 3.4, 13.4), mat(g)), box((2.0, 2.6, 1.8), (30.0, 3.4, 2.6), mat(g)), box((2.0, 2.6, 13.4), (30.0, 3.4, 14.2), mat(g))]
    rng = random.Random(4110)
    for k, (x0, x1) in enumerate(((3.0, 11.0), (12.0, 20.0), (21.0, 29.0))):
        y = 3.4
        for j in range(rng.randint(2, 4)):
            dx, dz = rng.uniform(-0.3, 0.3), rng.uniform(-0.3, 0.3)
            key = f"pk_fold_{k}_{j}"
            vx, vy = rng.uniform(0, 45), rng.uniform(0, 22)
            view = (vx, vy, vx + 12.0, vy + 13.2)
            out.append(box((x0 + dx, y, 3.6 + dz), (x1 + dx, y + 0.35, 12.4 + dz),
                           faces(rng.choice(("paper", "strip")), up=A.region(key, x1 - x0, 8.8, draw_map(view, labels=0.6, tone=0.86)))))
            y += 0.35
    a = "alu"
    out += [box((0.4, 14.0, 1.0), (31.6, 15.2, 2.0), mat(a)), box((0.4, 14.0, 14.0), (31.6, 15.2, 15.0), mat(a)),
            box((0.4, 14.0, 2.0), (1.4, 15.2, 14.0), mat(a)), box((30.6, 14.0, 2.0), (31.6, 15.2, 14.0), mat(a)),
            box((1.4, 14.6, 2.0), (30.6, 14.9, 14.0), mat("frosted"))]
    t = 14.9
    out.append(box((3.0, t, 2.5), (29.0, t + 0.05, 13.5), faces("paper", up=MAP.face("pk_tchart", 26.0, 11.0, draw_map(TABLE_VIEW, labels=0.85, route=True, tone=0.86)))))
    t += 0.05
    out.append(box((6.0, t, 9.4), (16.0, t + 0.1, 11.4), faces("acrylic", up=SCR.face("pk_plotter", 10.0, 2.0, plotter_scale)), {"angle": 22.5, "axis": "y", "origin": [11.0, t, 10.4]}))
    out += round_part(20.0, 5.0, 0.3, t, t + 0.3, "chrome", n=3)
    for ang in (22.5, -22.5):
        out.append(box((16.4, t, 4.93), (20.0, t + 0.12, 5.07), mat("chrome"), {"angle": ang, "axis": "y", "origin": [20.0, t, 5.0]}))
    for (x0, z0, ang) in ((21.0, 9.2, 0.0), (22.6, 11.4, 45.0)):
        pen = [box((x0 + 0.4, t, z0), (x0 + 3.4, t + 0.25, z0 + 0.25), mat("pencil")), box((x0 + 3.4, t, z0), (x0 + 3.8, t + 0.25, z0 + 0.25), mat("eraser")),
               box((x0, t + 0.05, z0 + 0.06), (x0 + 0.4, t + 0.2, z0 + 0.19), mat("beech")), box((x0 - 0.2, t + 0.09, z0 + 0.1), (x0, t + 0.16, z0 + 0.15), mat("black"))]
        if ang:
            for e in pen:
                e["rotation"] = {"angle": ang, "axis": "y", "origin": [x0 + 1.9, t, z0 + 0.12]}
        out += pen
    out += [box((26.0, t, 11.2), (28.4, t + 0.4, 11.6), mat("holder_yellow")), box((28.4, t, 11.2), (29.0, t + 0.4, 11.6), mat("black"))]
    out += round_part(25.8, 5.4, 1.6, t, t + 0.12, "white", n=4) + round_part(25.8, 5.4, 1.15, t + 0.12, t + 0.2, "acrylic", n=4)
    out += round_part(25.8, 5.4, 0.15, t + 0.2, t + 0.3, "chrome", n=3)
    out += [box((4.0, t, 11.8), (5.2, t + 0.35, 12.4), mat("white")), box((4.0, t, 12.4), (5.2, t + 0.35, 12.5), mat("glove_blue"))]
    return out


# ------------------------------------------------------------------ the flight strip board
BAYS = ["PENDING", "BRIEFED", "AIRBORNE", "LANDED"]


def strip_board():
    """The flight progress strip board (2 x 1 on a wall, its face to the north): the aluminium back, the black header with
    its four bays (PENDING, BRIEFED, AIRBORNE, LANDED), the rails between them, the strips in their coloured holders
    (each its flight, type, route, height and time), the marker tray under them with two markers."""
    out = [box((0.2, 3.6, 15.2), (31.8, 15.6, 16.0), mat("alu")), box((0.2, 13.6, 14.6), (31.8, 15.6, 15.2), mat("dark")),
           box((0.2, 3.6, 14.0), (31.8, 4.0, 15.2), mat("alu")), box((0.2, 4.0, 14.0), (31.8, 4.3, 14.2), mat("alu"))]
    out += [box((3.0, 4.0, 14.3), (5.6, 4.3, 14.6), mat("black")), box((5.6, 4.0, 14.3), (6.2, 4.3, 14.6), mat("red")),
            box((7.0, 4.0, 14.3), (9.6, 4.3, 14.6), mat("black")), box((9.6, 4.0, 14.3), (10.2, 4.3, 14.6), mat("glove_blue"))]
    w = 7.75
    rng = random.Random(5120)
    counts = (16, 9, 13, 21)
    flights = list(range(401, 401 + sum(counts)))
    n = 0
    for i, name in enumerate(BAYS):
        hi = 31.4 - i * w
        lo = hi - w
        out.append(box((lo + 0.3, 13.9, 14.58), (hi - 0.3, 15.3, 14.6), faces("dark", north=MAP.face(f"pk_bay_{i}", w - 0.6, 1.4, bay_label(name)))))
        if i:
            out.append(box((hi - 0.2, 4.3, 14.4), (hi + 0.2, 13.6, 15.2), mat("alu")))
        for j in range(counts[i]):
            col, row = j % 2, j // 2
            x0 = hi - 0.35 - (col + 1) * 3.5
            y0 = 12.8 - row * 0.72
            if y0 < 4.4:
                break
            kind = rng.choice(("T1", "T1", "T2"))
            colour = "red" if (i == 0 and j == 2) else "yellow" if rng.random() < 0.12 else ("blue" if kind == "T1" else "green")
            call = f"TP{flights[n]}"
            route = "TWR-" + rng.choice(list(WAYPOINTS))
            etd = f"{(13 + (i * 7 + j) // 5) % 24:02d}:{(j * 7 + i * 13) % 60:02d}"
            band = {"blue": (52, 98, 176), "green": (54, 140, 80), "red": (180, 46, 40), "yellow": (222, 186, 40)}[colour]
            out += [box((x0, y0, 14.7), (x0 + 3.4, y0 + 0.62, 15.2), mat(f"holder_{colour}")),
                    box((x0 + 0.16, y0 + 0.07, 14.66), (x0 + 3.24, y0 + 0.55, 14.7), faces("strip", north=SCR.face(f"pk_strip_{n}", 3.08, 0.48, strip_pic(call, kind, route, etd, "120M", band))))]
            n += 1
    return out


# ------------------------------------------------------------------ the NOTAM board
def notam_board():
    """The NOTAM board (2 x 1 on a wall, its face to the north): the cork in its aluminium frame, the NOTAM header card,
    the day's notices pinned up (a line highlighted on one), the restricted area's sketch ringed red, the pad notice,
    sticky notes; push pins in four colours."""
    out = [box((0.4, 0.4, 15.0), (31.6, 1.0, 16.0), mat("alu")), box((0.4, 15.0, 15.0), (31.6, 15.6, 16.0), mat("alu")),
           box((0.4, 1.0, 15.0), (1.0, 15.0, 16.0), mat("alu")), box((31.0, 1.0, 15.0), (31.6, 15.0, 16.0), mat("alu")),
           box((1.0, 1.0, 15.2), (31.0, 15.0, 16.0), mat("cork"))]
    out.append(box((17.6, 12.4, 15.15), (30.0, 14.6, 15.2), faces("white", north=SCR.face("pk_notam_hdr", 12.4, 2.2, header_card("NOTAM", "AIRSPACE NOTICES - READ BEFORE EVERY FLIGHT", (176, 40, 40))))))
    pins = ["red", "glove_blue", "holder_yellow", "glove_green"]
    rng = random.Random(6230)
    sheets = []
    for k in range(6):
        sheets.append((30.0 - 3.4 - k * 4.62, 7.0 + rng.uniform(-0.25, 0.2), f"pk_notam_{k}", notam_sheet(NOTAMS[k], highlight=4 if k == 0 else None)))
    sheets += [(30.0 - 3.4, 1.6, "pk_notam_map", map_sheet), (30.0 - 3.4 - 4.62, 1.5, "pk_notam_pad", notam_sheet(["PAD STATUS 23 OCT", "", "PAD 1   OPEN", "PAD 2   OPEN", "PAD 3   CLOSED", "        RESURFACING", "        FROM 24 OCT 0600", "SERVICE BAY  OPEN"], highlight=4))]
    for x0, y0, key, draw in sheets:
        out.append(box((x0, y0, 15.15), (x0 + 3.4, y0 + 4.75, 15.2), faces("paper", north=SCR.face(key, 3.4, 4.75, draw))))
        for px in (x0 + 0.5, x0 + 2.9):
            out += swap_yz(round_part(px, y0 + 4.35, 0.2, 14.85, 15.15, rng.choice(pins), n=3))
    for k, (x0, y0, text, col) in enumerate(((19.4, 4.2, "CHECK R-101\nBEFORE\nBRAVO RUNS", (250, 236, 110)), (15.8, 2.0, "PAD 3 CLSD\nFRI 06-10", (250, 180, 200)),
                                             (11.6, 3.4, "NEW MAST\nW OF ECHO!", (160, 220, 250)))):
        out.append(box((x0, y0, 15.14), (x0 + 2.4, y0 + 2.4, 15.2), faces("paper", north=SCR.face(f"pk_sticky_{k}", 2.4, 2.4, sticky(text, col)))))
    out += [box((2.0, 2.0, 15.15), (8.8, 6.2, 15.2), faces("paper", north=SCR.face("pk_notam_wx", 6.8, 4.2, notam_sheet(["SIGWX 23 OCT 12-18Z", "SHRA FROM W AFTER 16Z", "GUSTS 25KT ON RIDGES", "FRZL 6000FT"], highlight=1))))]
    out += swap_yz(round_part(2.5, 5.8, 0.2, 14.85, 15.15, "red", n=3)) + swap_yz(round_part(8.3, 5.8, 0.2, 14.85, 15.15, "red", n=3))
    return out


# ------------------------------------------------------------------ the area chart
SCALE = 46.0 / WORLD[0]          # pixels a km on the wall chart


def chart_xy(kx, ky):
    """Where a point of the region (km) falls on the wall chart's face (its face to the north: east is to -x)."""
    return round(47.0 - kx * SCALE, 4), round(30.6 - ky * SCALE, 4)


def area_chart():
    """The region's VFR drone chart on the wall (3 x 2, its face to the north): the aluminium frame, the chart itself (the
    control zone, the corridors, the waypoints, the restricted and danger areas, the towns, the river, the roads), the
    title block and the legend stuck on it, a pin at the tower and at each waypoint, the red cords of the corridors
    strung between them."""
    out = [box((0.4, 1.4, 15.0), (47.6, 2.0, 16.0), mat("alu")), box((0.4, 30.6, 15.0), (47.6, 31.2, 16.0), mat("alu")),
           box((0.4, 2.0, 15.0), (1.0, 30.6, 16.0), mat("alu")), box((47.0, 2.0, 15.0), (47.6, 30.6, 16.0), mat("alu")),
           box((1.0, 2.0, 15.45), (47.0, 30.6, 16.0), faces("white", north=MAP.face("pk_wchart", 46.0, 28.6, draw_map((0.0, 0.0, WORLD[0], WORLD[1]))))),
           box((1.6, 2.6, 15.4), (9.6, 6.6, 15.45), faces("white", north=SCR.face("pk_title", 8.0, 4.0, chart_title))),
           box((40.4, 25.0, 15.4), (46.4, 30.0, 15.45), faces("white", north=SCR.face("pk_legend", 6.0, 5.0, chart_legend)))]
    tx, ty = chart_xy(*TOWER)
    for name, (kx, ky) in WAYPOINTS.items():
        wx, wy = chart_xy(kx, ky)
        out += cable([(tx, ty, 15.32), (wx, wy, 15.32)], 0.05, "red")
        out += swap_yz(round_part(wx, wy, 0.24, 14.8, 15.45, "glove_blue", n=3))
    out += swap_yz(round_part(tx, ty, 0.34, 14.7, 15.45, "red", n=3))
    return out


# ------------------------------------------------------------------ the zone clocks
ZONES = [("UTC", "13:32"), ("LOCAL", "14:32"), ("SUNSET", "18:07")]
ZONE_HOURS = {"UTC": -1, "LOCAL": 0}                 # live: the tower's time, and UTC an hour behind it


def zone_clocks():
    """The zone clocks (2 x 1 on a wall, their faces to the north): the graphite bar, three faces (live in game: UTC, the
    tower's time, sunset), each with its label plate under it."""
    out = [box((0.6, 4.0, 14.0), (31.4, 12.0, 16.0), mat("graphite")), box((0.6, 11.4, 13.6), (31.4, 12.0, 14.0), mat("graphite")),
           box((0.6, 4.0, 13.6), (31.4, 4.4, 14.0), mat("graphite")), box((0.6, 4.4, 13.6), (1.2, 11.4, 14.0), mat("graphite")),
           box((30.8, 4.4, 13.6), (31.4, 11.4, 14.0), mat("graphite"))]
    for k, (label, time) in enumerate(ZONES):
        cx = 26.0 - k * 10.0
        out += [{**box((cx - 4.4, 6.6, 13.8), (cx + 4.4, 11.0, 14.0), {"north": MAP.face(f"pk_zone_{k}", 8.8, 4.4, zone_face(label, time))}), "shade": False},
                box((cx - 3.0, 4.7, 13.74), (cx + 3.0, 6.2, 13.8), faces("graphite", north=MAP.face(f"pk_zl_{k}", 6.0, 1.5, name_plate(label, (44, 46, 50), (232, 232, 232)))))]
        if k:
            out.append(box((cx + 4.75, 4.4, 13.7), (cx + 5.25, 11.4, 14.0), mat("black")))
    return out


# ------------------------------------------------------------------ the weather kiosk
def wx_kiosk():
    """The weather briefing kiosk (two high, its screen to the north): the base plate, the column with its ticket printer
    (a forecast hanging out of it), the hand rest, the head tilted back with its touch screen (live in game: the
    tower's METAR, TAF, wind and rain radar), the WEATHER header on top."""
    dk = "dark"
    rot = {"angle": 22.5, "axis": "x", "origin": [8.0, 17.0, 8.2]}
    out = [box((2.4, 0, 3.0), (13.6, 0.6, 13.0), mat("graphite")), box((5.6, 0.6, 7.0), (10.4, 17.0, 10.6), mat(dk)),
           box((6.4, 13.8, 6.9), (9.6, 14.3, 7.0), mat("black")), box((6.8, 12.4, 6.84), (9.2, 14.1, 6.88), faces("paper", north=SCR.face("pk_ticket", 2.4, 1.7, notam_sheet(["XTPL TAF", "24012KT 9999", "TEMPO SHRA"])))),
           box((2.0, 16.4, 4.6), (14.0, 17.0, 7.4), mat(dk)),
           box((1.6, 17.0, 7.0), (14.4, 26.4, 9.4), mat(dk), dict(rot)),
           {**box((2.2, 17.8, 6.96), (13.8, 25.8, 7.0), {"north": SCR.face("pk_wx", 11.6, 8.0, wx_screen)}, dict(rot)), "shade": False},
           box((2.0, 26.4, 7.4), (14.0, 28.6, 9.0), faces(dk, north=MAP.face("pk_kiosk_hdr", 12.0, 2.2, kiosk_header)), dict(rot))]
    return out


# ------------------------------------------------------------------ the chart bin
def chart_bin():
    """A wire bin of rolled charts (45 cm square, 60 cm tall): its base, posts, rims and wires; the rolls standing in it
    (charts, plans, a blueprint), their rubber bands."""
    w = "wire"
    out = [box((4.4, 0.4, 4.4), (11.6, 0.6, 11.6), mat(w))]
    for x in (4.4, 11.3):
        for z in (4.4, 11.3):
            out.append(box((x, 0, z), (x + 0.3, 9.6, z + 0.3), mat(w)))
    for y in (4.8, 9.3):
        out += [box((4.7, y, 4.4), (11.3, y + 0.3, 4.6), mat(w)), box((4.7, y, 11.4), (11.3, y + 0.3, 11.6), mat(w)),
                box((4.4, y, 4.7), (4.6, y + 0.3, 11.3), mat(w)), box((11.4, y, 4.7), (11.6, y + 0.3, 11.3), mat(w))]
    for k in range(1, 8):
        v = 4.4 + k * 0.9
        out += [box((v, 0.6, 4.45), (v + 0.1, 9.3, 4.55), mat(w)), box((v, 0.6, 11.45), (v + 0.1, 9.3, 11.55), mat(w)),
                box((4.45, 0.6, v), (4.55, 9.3, v + 0.1), mat(w)), box((11.45, 0.6, v), (11.55, 9.3, v + 0.1), mat(w))]
    rolls = [(6.0, 6.0, 0.8, 14.5, "paper"), (7.9, 5.9, 0.6, 12.0, "strip"), (9.9, 6.2, 0.75, 16.5, "holder_blue"),
             (5.8, 8.0, 0.55, 11.0, "holder_white"), (7.7, 7.9, 0.85, 17.5, "cardboard"), (9.9, 8.3, 0.6, 13.0, "strip"),
             (6.1, 10.0, 0.7, 15.0, "holder_white"), (8.1, 9.9, 0.55, 10.5, "paper"), (10.0, 10.2, 0.65, 14.0, "paper")]
    for k, (cx, cz, r, h, tex) in enumerate(rolls):
        out += round_part(cx, cz, r, 0.6, h, tex, n=3)
        if tex == "cardboard":                        # a chart tube with its cap
            out += round_part(cx, cz, r + 0.06, h - 0.8, h + 0.1, "black", n=3)
            continue
        out += round_part(cx, cz, r * 0.34, h, h + 0.02, "cardboard", n=3)
        for yb in (h * 0.3, h * 0.75):
            out += round_part(cx, cz, r + 0.04, yb, yb + 0.15, "rubber_band", n=3)
    return out


# ------------------------------------------------------------------ the binder shelf
BINDERS = ["AIP VOL 1", "AIP VOL 2", "AIP VOL 3", "OPS MANUAL A", "OPS MANUAL B", "NOTAMS 2026", "NOTAMS 2025", "ROUTES", "WEATHER",
           "SAFETY", "SMS", "TRAINING", "CHARTS", "MAINT LOG", "INCIDENTS", "PAD PLANS", "FLEET T1", "FLEET T2", "RADIO", "AIRSPACE",
           "CREW ROSTER", "AUDITS", "LOADING", "BATTERIES", "SURVEYS", "PERMITS", "INSURANCE", "CONTRACTS"]


def binder_shelf():
    """A low open shelf of lever arch files (2 x 1, 80 cm, its front to the north): the laminate carcass on its plinth,
    the divider and the shelf, the files on both shelves (their colours, label windows, finger holes), one fallen over
    against the others, magazine files; on top a document tray, a hole punch and a stack of folders."""
    lam = "laminate"
    out = [box((0, 12.2, 8.6), (32, 12.8, 16.0), mat(lam)), box((0.6, 0, 9.4), (31.4, 0.6, 15.6), mat("black")),
           box((0.6, 0.6, 9.0), (31.4, 1.0, 15.6), mat(lam)), box((0, 0, 8.6), (0.6, 12.2, 16.0), mat(lam)), box((31.4, 0, 8.6), (32.0, 12.2, 16.0), mat(lam)),
           box((0.6, 1.0, 15.6), (31.4, 12.2, 16.0), mat(lam)), box((15.7, 1.0, 9.0), (16.3, 12.2, 15.6), mat(lam)), box((0.6, 6.4, 9.0), (31.4, 6.8, 15.6), mat(lam))]
    rng = random.Random(7340)
    names = list(BINDERS)
    rng.shuffle(names)
    cols = list(BINDER_COLOURS)
    n = 0
    for (x0, x1) in ((0.7, 15.6), (16.4, 31.3)):
        for y0 in (1.0, 6.8):
            x = x0 + 0.05
            k = 0
            while x + 1.28 < x1 - 0.05:
                if rng.random() < 0.08 and k > 2:
                    x += 1.6
                    continue
                c = rng.choice(cols)
                name = names[n % len(names)]
                e = box((x, y0, 9.6), (x + 1.28, y0 + 5.0, 14.4), faces(f"bnd_{c}", north=MAP.face(f"pk_bnd_{n}", 1.28, 5.0, binder_spine(name, BINDER_COLOURS[c]))))
                out.append(e)
                n += 1
                k += 1
                x += 1.34
            if y0 == 1.0 and x0 > 16:
                out.append(box((x1 - 5.0, y0, 9.6), (x1 - 0.05, y0 + 1.28, 14.4), faces("bnd_grey", up=MAP.face("pk_bnd_flat", 4.95, 4.8, rot180(binder_spine("OLD NOTAMS", BINDER_COLOURS["grey"]))))))
    # on top: the document tray, the hole punch, the folders
    t = 12.8
    out += [box((2.0, t, 9.4), (6.4, t + 0.2, 14.6), mat("grip_black")), box((2.0, t + 1.2, 9.4), (6.4, t + 1.4, 14.6), mat("grip_black")),
            box((2.0, t + 0.2, 9.4), (2.2, t + 1.2, 14.6), mat("grip_black")), box((6.2, t + 0.2, 9.4), (6.4, t + 1.2, 14.6), mat("grip_black")),
            box((2.2, t + 0.2, 9.6), (6.2, t + 0.5, 14.4), faces("paper", up=A.region("pk_tray2", 4.0, 4.8, MK.page_sheet(951)))),
            box((2.2, t + 1.4, 9.6), (6.2, t + 1.6, 14.4), faces("paper", up=A.region("pk_tray3", 4.0, 4.8, MK.page_sheet(952))))]
    out += [box((9.0, t, 11.0), (12.0, t + 0.6, 13.4), mat("graphite")), box((9.2, t + 0.6, 11.4), (11.8, t + 1.0, 13.0), mat("graphite")),
            box((9.4, t + 0.6, 12.8), (11.6, t + 1.6, 13.0), mat("graphite"), {"angle": -22.5, "axis": "x", "origin": [10.5, t + 0.6, 12.9]})]
    for k, c in enumerate(("holder_blue", "holder_red", "holder_green", "holder_yellow")):
        out.append(box((20.0 + k * 0.1, t + k * 0.12, 9.8 + k * 0.05), (26.0 + k * 0.1, t + (k + 1) * 0.12, 14.2 + k * 0.05), mat(c)))
    return out


# ------------------------------------------------------------------ the filing cabinets
DRAWER_LABELS = [["A - F", "G - L", "M - R", "S - Z"], ["PERMITS", "SURVEYS", "INSURANCE", "CONTRACTS"],
                 ["PILOTS", "LOGBOOKS", "MEDICALS", "TRAINING"], ["2024", "2025", "2026", "SPARE"],
                 ["ROUTES", "NOTAMS", "WX", "INCIDENTS"], ["FLEET T1", "FLEET T2", "PARTS", "WARRANTY"]]


def filing_cabinet(x0, labels, key, open_drawer=None):
    """One four-drawer steel filing cabinet (47 by 62 cm, 1.32 m tall) from x0, its front to the north: the light grey
    carcass on its recessed plinth, the four drawer fronts proud of it with the dark gaps between, each drawer's bar
    pull on its posts and the label in its chrome holder, the lock in the top drawer. open_drawer (0 the top): that
    drawer pulled out, the suspension files hanging in it."""
    p = "powder"
    x1 = x0 + 7.6
    xc = (x0 + x1) / 2
    out = [box((x0 + 0.3, 0, 6.8), (x1 - 0.3, 0.6, 15.4), mat("black")),
           box((x0, 0.6, 6.2), (x0 + 0.15, 21.0, 15.8), mat(p)), box((x1 - 0.15, 0.6, 6.2), (x1, 21.0, 15.8), mat(p)),
           box((x0, 21.0, 6.0), (x1, 21.2, 15.8), mat(p)), box((x0 + 0.15, 0.6, 15.6), (x1 - 0.15, 21.0, 15.8), mat(p)),
           box((x0 + 0.15, 0.6, 6.3), (x1 - 0.15, 0.7, 15.6), mat(p))]
    for k in range(4):
        y1 = 20.9 - k * 5.08
        y0 = y1 - 4.96
        dz = -4.4 if open_drawer == k else 0.0
        if open_drawer == k:
            # the drawer's body pulled out: its sides, bottom and back, the files hanging in it; the dark inside behind
            out += [box((x0 + 0.5, y0 + 0.4, 6.25 + dz), (x0 + 0.6, y1 - 1.0, 6.3), mat(p)), box((x1 - 0.6, y0 + 0.4, 6.25 + dz), (x1 - 0.5, y1 - 1.0, 6.3), mat(p)),
                    box((x0 + 0.6, y0 + 0.4, 6.25 + dz), (x1 - 0.6, y0 + 0.5, 6.3), mat(p)),
                    box((x0 + 0.15, y0, 6.3), (x1 - 0.15, y1, 6.4), mat("black"))]
            rng = random.Random(8800 + int(x0 * 10))
            for j in range(9):
                fz = 2.3 + j * 0.42
                h = rng.uniform(3.0, 3.4)
                col = rng.choice(("susp", "susp", "manila"))
                out.append(box((x0 + 0.75, y0 + 0.5, fz), (x1 - 0.75, y0 + 0.5 + h, fz + 0.08), mat(col)))
                tx = rng.uniform(x0 + 1.0, x1 - 2.2)
                out.append(box((tx, y0 + 0.5 + h, fz), (tx + 1.0, y0 + 0.9 + h, fz + 0.06), mat(rng.choice(("white", "holder_red", "holder_blue", "holder_yellow")))))
        else:
            out.append(box((x0 + 0.15, y0, 6.3), (x1 - 0.15, y1, 6.4), mat("black")))
        out.append(box((x0 + 0.2, y0, 5.9 + dz), (x1 - 0.2, y1, 6.3 + dz), mat(p)))
        hy = y1 - 2.0
        out += [box((xc - 1.6, hy, 5.4 + dz), (xc + 1.6, hy + 0.35, 5.6 + dz), mat("chrome")),
                box((xc - 1.5, hy, 5.6 + dz), (xc - 1.2, hy + 0.35, 5.9 + dz), mat("chrome")), box((xc + 1.2, hy, 5.6 + dz), (xc + 1.5, hy + 0.35, 5.9 + dz), mat("chrome")),
                box((xc - 1.2, hy + 0.7, 5.82 + dz), (xc + 1.2, hy + 1.6, 5.9 + dz), mat("chrome")),
                box((xc - 1.05, hy + 0.8, 5.8 + dz), (xc + 1.05, hy + 1.5, 5.82 + dz), faces("paper", north=SCR.face(f"{key}_{k}", 2.1, 0.7, name_plate(labels[k], (246, 244, 236)))))]
        if k == 0:
            out += swap_yz(round_part(x1 - 1.0, y1 - 0.7, 0.25, 5.7 + dz, 5.9 + dz, "chrome", n=3))
    return out


def steel_filing_cabinet(v):
    """Two four-drawer filing cabinets side by side (a block wide, two high: 1.32 m), their fronts to the north. v 0 plain;
    v 1 a drawer pulled out, a box of folders on top; v 2 a snake plant and a stack of binders on top."""
    out = filing_cabinet(0.2, DRAWER_LABELS[(2 * v) % 6], f"pk_fc{v}a") + filing_cabinet(8.2, DRAWER_LABELS[(2 * v + 1) % 6], f"pk_fc{v}b", open_drawer=1 if v == 1 else None)
    t = 21.2
    if v == 1:
        out += [box((1.0, t, 8.0), (6.6, t + 3.0, 14.8), faces("cardboard", north=A.region("pk_boxfile", 5.6, 3.0, label_plate("ARCHIVE 2025", (236, 230, 214)))))]
        out += [box((1.0 + k * 0.3, t + 3.0, 8.4), (1.3 + k * 0.3, t + 4.2, 14.0), mat(c)) for k, c in enumerate(("manila", "susp", "manila", "holder_red"))]
    elif v == 2:
        cx, cz = 4.0, 11.0
        out += round_ring(cx, cz, 1.6, 0.22, t, t + 2.6, "pot", 2.2, "soil", n=5) + MK.snake_plant(cx, cz, t + 2.2, scale=0.8)
        for k, c in enumerate(("bnd_navy", "bnd_red", "bnd_grey")):
            out.append(box((9.2 + k * 0.15, t + k * 1.28, 8.6), (14.2 + k * 0.15, t + (k + 1) * 1.28, 13.2), mat(c)))
    return out


# ------------------------------------------------------------------ the copier
def mfp_panel(img):
    """The copier's touch panel on standby (live in game: it wakes when used)."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(14, 18, 24))
    d.text((W / 2, H * 0.32), "READY", font=_fit(d, "READY", FONT, round(H * 0.2), W * 0.8), fill=(120, 200, 255), anchor="mm")
    for k, t in enumerate(("COPY", "SCAN", "PRINT")):
        x0 = W * (0.06 + k * 0.31)
        d.rectangle((x0, H * 0.58, x0 + W * 0.26, H * 0.88), fill=(34, 44, 58), outline=(80, 110, 150))
        d.text((x0 + W * 0.13, H * 0.73), t, font=_fit(d, t, FONT, round(H * 0.12), W * 0.22), fill=(220, 230, 240), anchor="mm")
    return img


def mfp_copier():
    """The office copier (a multifunction printer: 60 by 65 cm, 1.15 m to the top of its feeder; two high, its front to
    the north): on castors, the paper deck's two big trays and the main unit's two trays (each a drawer with its pull,
    the dark gaps round it, a paper gauge), the front door's seam, the output bay with printed sheets in it, the
    scanner and the document feeder with paper in its tray, the touch panel on its tilted arm (on standby until used),
    the bypass tray folded on the right side."""
    w, dk = "mfp", "mfp_dark"
    out = []
    for x in (3.6, 11.6):
        for z in (5.4, 14.0):
            out += [box((x, 0.4, z), (x + 0.8, 0.7, z + 0.8), mat("black"))] + rod_x(x + 0.15, x + 0.65, 0.3, z + 0.4, 0.3, "grip_black", n=3)
    out += [box((3.2, 0.7, 5.0), (12.8, 0.9, 15.2), mat("black")), box((3.2, 0.9, 5.2), (12.8, 15.2, 15.2), mat(w)),
            box((3.4, 0.9, 5.0), (12.6, 11.4, 5.2), mat("black"))]
    for k, (y0, y1) in enumerate(((1.0, 3.9), (4.0, 6.9), (7.3, 9.1), (9.2, 11.0))):
        out += [box((3.3, y0 + 0.05, 4.8), (12.7, y1 - 0.05, 5.1), mat(w)),
                box((6.2, y1 - 0.9, 4.7), (9.8, y1 - 0.5, 4.8), mat(dk)), box((6.2, y1 - 0.5, 4.75), (9.8, y1 - 0.45, 5.0), mat(dk)),
                box((10.8, y0 + 0.6, 4.76), (11.8, y0 + 1.0, 4.8), mat("black")), box((10.9, y0 + 0.7, 4.75), (10.9 + 0.2 + 0.6 * ((k * 3 + 1) % 4) / 3, y0 + 0.9, 4.76), mat("white"))]
    # the front door's seam (over the trays' row), the output bay and its prints
    out += [box((3.2, 11.4, 5.0), (12.8, 11.6, 5.2), mat(dk)), box((3.4, 11.6, 5.2), (12.6, 13.6, 13.6), mat(dk)),
            box((3.2, 13.6, 5.0), (12.8, 13.8, 15.2), mat(w)),
            box((4.0, 11.6, 5.6), (11.2, 11.9, 12.4), mat("black"), {"angle": -22.5, "axis": "x", "origin": [7.6, 11.6, 12.4]})]
    for k in range(3):
        out.append(box((4.4 + k * 0.05, 11.95 + k * 0.08, 6.0), (10.8 - k * 0.05, 12.0 + k * 0.08, 12.2),
                       faces("paper", up=A.region(f"pk_mfp_print_{k}", 6.4, 6.2, MK.page_sheet(960 + k))), {"angle": -22.5, "axis": "x", "origin": [7.6, 11.6, 12.4]}))
    # the scanner, the document feeder and its paper; the touch panel; the bypass tray; the model plate
    out += [box((3.0, 13.8, 4.8), (13.0, 16.4, 15.4), mat(w)), box((3.0, 16.4, 5.0), (13.0, 16.6, 15.4), mat(dk)),
            box((3.4, 16.6, 6.6), (12.6, 17.6, 15.0), mat(w)), box((4.0, 17.6, 9.0), (12.0, 18.4, 15.0), mat(w)),
            box((4.6, 17.6, 6.8), (11.4, 17.7, 9.0), mat(dk)),
            box((4.8, 18.4, 9.6), (11.2, 18.9, 14.6), faces("paper", up=A.region("pk_mfp_feed", 6.4, 5.0, MK.page_sheet(963)))),
            box((3.4, 14.4, 4.7), (6.4, 14.8, 4.8), faces(w, north=SCR.face("pk_mfp_plate", 3.0, 0.4, label_plate("MF-45  COLOUR", (222, 224, 226))))),
            box((9.2, 14.6, 3.6), (12.8, 15.0, 6.2), mat(dk), {"angle": -22.5, "axis": "x", "origin": [11.0, 14.8, 6.2]}),
            {**box((9.4, 15.0, 3.8), (12.6, 15.05, 6.0), faces(dk, up=SCR.face("pk_mfp_panel", 3.2, 2.2, rot180(mfp_panel))), {"angle": -22.5, "axis": "x", "origin": [11.0, 14.8, 6.2]}), "shade": False},
            box((12.8, 4.4, 7.0), (13.2, 10.4, 13.0), mat(w)), box((12.9, 9.6, 8.6), (13.25, 10.0, 11.4), mat(dk))]
    return out


# ------------------------------------------------------------------ writing
def write(assets, ns="jugcraft", pages=None):
    global A, SCR, MAP
    TK.PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    mats = {**TK.MATERIALS, **MK.MATERIALS, **LK.MATERIALS, **RK.MATERIALS, **MATERIALS}
    for name, make in mats.items():
        make().save(tex_dir / f"{name}.png")
    A = TK.Atlas(1024, TX, "atlas")
    SCR = ShelfAtlas(2048, 4 * TX, "screens")
    MAP = ShelfAtlas(2048, 2 * TX, "maps")
    TK.SCR = SCR
    MK.A, MK.SCR = A, SCR
    LK.A, LK.SCR = A, SCR
    RK.A, RK.SCR = A, SCR
    ref = lambda n: f"{ns}:block/{n}"
    T = {k[3:]: ref(k) for k in mats}
    T.update({"atlas": ref("pk_atlas"), "screens": ref("pk_screens"), "maps": ref("pk_maps"), "particle": ref("pk_laminate")})

    def model(name, elements, prepared=False):
        if not prepared:
            prep(elements)
        MW.fit_uvs(elements)
        (model_dir / f"{name}.json").write_text(json.dumps({"textures": T, "elements": elements}, indent=1) + "\n")

    def states(name, variants):
        (bs_dir / f"{name}.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")

    def prep(elements):
        for e in elements:
            for i in range(3):
                if e["from"][i] > e["to"][i]:
                    e["from"][i], e["to"][i] = e["to"][i], e["from"][i]
        MW.close_open_faces(elements)
        MW.separate_coplanar(elements, pin_uv=False)
        return elements

    def faced(model_name, extra="", off=0):
        return {f"facing={f}{extra}": {"model": ref(model_name), **({"y": (y - off) % 360} if (y - off) % 360 else {})} for f, y in FACINGS.items()}

    def single(name, els):
        model(name, els)
        states(name, faced(name))

    def tall(name, els):
        v = {}
        for h, part in zip(("lower", "upper"), cut_y(prep(els), 2)):
            model(f"{name}_{h}", part, True)
            v.update(faced(f"{name}_{h}", f",half={h}"))
        return v

    def wide(name, els, n):
        v = {}
        for p, part in enumerate(cut_parts(prep(els), n)):
            model(f"{name}_{p}", part, True)
            v.update(faced(f"{name}_{p}", f",part={p}"))
        return v

    states("dispatch_counter", wide("dispatch_counter", dispatch_counter(), 3))
    states("chart_table", wide("chart_table", chart_table(), 2))
    states("strip_board", wide("strip_board", strip_board(), 2))
    states("notam_board", wide("notam_board", notam_board(), 2))
    states("zone_clocks", wide("zone_clocks", zone_clocks(), 2))
    states("binder_shelf", wide("binder_shelf", binder_shelf(), 2))
    states("wx_kiosk", tall("wx_kiosk", wx_kiosk()))
    single("chart_bin", chart_bin())
    v = {}
    for k in range(3):
        for h, part in zip(("lower", "upper"), cut_y(prep(steel_filing_cabinet(k)), 2)):
            model(f"steel_filing_cabinet_{k}_{h}", part, True)
            v.update(faced(f"steel_filing_cabinet_{k}_{h}", f",half={h},variant={k}"))
    states("steel_filing_cabinet", v)
    states("mfp_copier", tall("mfp_copier", mfp_copier()))
    v = {}
    for h, half in zip(("lower", "upper"), cut_y(prep(area_chart()), 2)):
        for p, part in enumerate(cut_parts(half, 3)):
            model(f"area_chart_{h}_{p}", part, True)
            v.update(faced(f"area_chart_{h}_{p}", f",half={h},part={p}"))
    states("area_chart", v)
    A.img.save(tex_dir / "pk_atlas.png")
    SCR.img.save(tex_dir / "pk_screens.png")
    MAP.img.save(tex_dir / "pk_maps.png")
    return A.y + A.row, SCR.y + SCR.row, MAP.y + MAP.row


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
