"""The diesel generator rooms' control and protection panels (round 13; plant_office2.PIECES, built in
tools/medium_plant.py's diesel rooms in place of the row of relay cabinets).

A real emergency diesel generator room has these, side by side along a wall:
- the local engine control panel (engine gauges, alarm annunciator, mode selector, start/stop and emergency stop);
- the generator control panel, two sections wide (switchboard meters, the synchroscope and its lamps, a mimic of the
  generator breaker onto its 4160 V bus, the breaker's pistol-grip control switch, voltage and speed adjust);
- the excitation / automatic voltage regulator cabinet;
- the generator protection relay panel (differential, overcurrent, reverse power, loss of field, negative sequence,
  frequency and voltage relays, test switches, the lockout relay's handle);
- the load sequencer (it starts the safety loads one after another when the generator takes the bus);
- the 125 V DC battery charger;
- the battery rack (two tiers of cells on an open steel frame).

Each is a floor-standing piece two blocks high (one or two wide) placed as one ("multi:w:2:p", MultiFurnitureBlock).
Its front is drawn whole (16 image px a model pixel) and cut into blocks, so meters, windows and doors run across the
block line; what stands proud of the door (meters, relay cases, the annunciator, lamps, buttons, knobs, handles) is a
real box whose front shows the same drawing. Models are in model pixels for the piece facing north: x across to the
user's right, y up from the floor (0..32), its front the south face (z 15).
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import nuclear_materials as M

U = 16  # image px a model pixel
PAINT = (196, 198, 192)
INK = (24, 24, 26)
FONT_DIR = "/usr/share/fonts/truetype/dejavu/"


def font(size, bold=False):
    return ImageFont.truetype(FONT_DIR + ("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"), size)


def cfont(size, bold=False):
    return ImageFont.truetype(FONT_DIR + ("DejaVuSansCondensed-Bold.ttf" if bold else "DejaVuSansCondensed.ttf"), size)


PANELS = {
    "edg_engine_panel": {"display": "Diesel Engine Control Panel", "width": 1},
    "edg_generator_panel": {"display": "Generator Control Panel", "width": 2},
    "edg_voltage_regulator": {"display": "Excitation and Voltage Regulator", "width": 1},
    "edg_protection_panel": {"display": "Generator Protection Relay Panel", "width": 1},
    "edg_load_sequencer": {"display": "Load Sequencer", "width": 1},
    "edg_battery_charger": {"display": "Battery Charger", "width": 1},
    "edg_battery_rack": {"display": "Battery Rack", "width": 1},
}
for _k, _v in PANELS.items():
    _v["multi"] = (_v["width"], 2, "panel")
    _v["panel"] = _k

CRAFTING = {
    "edg_engine_panel": (["PGP", "PCP", "PRP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
    "edg_generator_panel": (["PGP", "CRC", "PPP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
    "edg_voltage_regulator": (["PCP", "PRP", "PCP"], {"P": "#c:plates/steel", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
    "edg_protection_panel": (["PGP", "PCP", "PCP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit"}, 1),
    "edg_load_sequencer": (["PXP", "PCP", "PRP"], {"P": "#c:plates/steel", "X": "jugcraft:processor", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
    "edg_battery_charger": (["PRP", "PCP", "PRP"], {"P": "#c:plates/steel", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
    "edg_battery_rack": (["S S", "BBB", "S S"], {"S": "#c:plates/steel", "B": "jugcraft:lead_acid_pack"}, 1),
}


# ------------------------------------------------------------------------------------------------ the drawing kit

class Front:
    """A panel's front elevation in model pixels (y up), drawn at U image px a pixel."""

    def __init__(self, width, seed):
        self.W = width * 16
        w, h = self.W * U, 32 * U
        rng = np.random.default_rng(seed)
        base = np.ones((h, w, 3)) * np.array(PAINT, float)
        # Faint mottling and a few scuffs, like the painted cabinets beside them.
        small = rng.normal(0, 1, (h // 32 + 1, w // 32 + 1))
        noise = np.kron(small, np.ones((32, 32)))[:h, :w]
        base += noise[..., None] * 2.2 + rng.normal(0, 1.2, (h, w))[..., None]
        img = np.clip(base, 0, 255).astype(np.uint8)
        self.img = Image.fromarray(np.dstack([img, np.full((h, w), 255, np.uint8)]), "RGBA")
        self.d = ImageDraw.Draw(self.img)
        self.boxes = []  # what stands proud of the door: (x0, y0, x1, y1, depth, side texture)
        self.live = []   # what moves (client/LivePanelRenderer.java draws it from the generator's state)

    def px(self, x, y):
        return x * U, (32 - y) * U

    def R(self, x0, y0, x1, y1, **kw):
        self.d.rectangle([x0 * U, (32 - y1) * U, x1 * U - 1, (32 - y0) * U - 1], **kw)

    def E(self, cx, cy, r, **kw):
        self.d.ellipse([(cx - r) * U, (32 - cy - r) * U, (cx + r) * U, (32 - cy + r) * U], **kw)

    def T(self, cx, cy, text, size, fill=INK, bold=False, anchor="mm", condensed=True):
        f = cfont(size, bold) if condensed else font(size, bold)
        self.d.text(self.px(cx, cy), text, font=f, fill=fill, anchor=anchor)

    def line(self, pts, fill, width):
        self.d.line([self.px(x, y) for x, y in pts], fill=fill, width=width)

    def proud(self, x0, y0, x1, y1, depth, side="pi_pnl_black"):
        self.boxes.append((x0, y0, x1, y1, depth, side))

    # -- pieces of a panel
    def door(self, x0, x1, handle=True):
        """A door in the cabinet front: its seam, a hinge at each end, a handle (a real one) on the right."""
        self.R(x0 + 0.05, 1.45, x1 - 0.05, 30.45, outline=(120, 122, 118, 255), width=3)
        self.R(x0 + 0.2, 1.6, x1 - 0.2, 30.3, outline=(214, 216, 210, 255), width=2)
        for y in (4, 27):
            self.R(x0 + 0.25, y, x0 + 0.55, y + 1.6, fill=(150, 152, 148, 255))
        if handle:
            self.proud(x1 - 1.3, 14.2, x1 - 0.8, 17.6, 0.6, "pi_chrome")
            self.R(x1 - 1.35, 14.1, x1 - 0.75, 17.7, fill=(70, 72, 74, 255))

    def nameplate(self, cx, cy, w, lines, h=None):
        h = h or (1.0 + 0.75 * len(lines))
        self.R(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2, fill=(22, 22, 24, 255), outline=(180, 182, 184, 255), width=2)
        for i, text in enumerate(lines):
            y = cy + (len(lines) - 1) * 0.42 - i * 0.84
            self.T(cx, y, text, 13 if i == 0 else 10, fill=(236, 236, 230, 255), bold=i == 0)
        self.proud(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2, 0.12, "pi_pnl_black")

    def tag(self, cx, cy, text, size=9, w=None):
        """An engraved label: white plastic, black letters."""
        f = cfont(size, True)
        tw = (self.d.textlength(text, font=f) + 10) / U if w is None else w
        self.R(cx - tw / 2, cy - 0.38, cx + tw / 2, cy + 0.38, fill=(240, 240, 234, 255), outline=(90, 90, 92, 255), width=1)
        self.d.text(self.px(cx, cy), text, font=f, fill=INK, anchor="mm")

    def dial(self, cx, cy, r, label, scale, value, unit="", red=None, live=None):
        """A round gauge in a square black bezel: white face, ticks round 240 degrees, numbers, a red band, the needle
        (drawn live from channel live, if given, instead of painted)."""
        self.R(cx - r - 0.35, cy - r - 0.35, cx + r + 0.35, cy + r + 0.35, fill=(30, 30, 32, 255))
        self.E(cx, cy, r + 0.12, fill=(90, 92, 96, 255))
        self.E(cx, cy, r, fill=(246, 246, 240, 255))
        lo, hi = scale
        start, sweep = 210, 240
        if red:
            a0 = math.radians(start - sweep * (red[0] - lo) / (hi - lo))
            a1 = math.radians(start - sweep * (red[1] - lo) / (hi - lo))
            for k in range(24):
                a = a0 + (a1 - a0) * k / 23
                x, y = cx + math.cos(a) * r * 0.86, cy + math.sin(a) * r * 0.86
                self.E(x, y, r * 0.07, fill=(200, 40, 34, 255))
        for k in range(11):
            a = math.radians(start - sweep * k / 10)
            r0 = r * (0.74 if k % 5 == 0 else 0.8)
            self.line([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * r * 0.93, cy + math.sin(a) * r * 0.93)],
                      INK, 3 if k % 5 == 0 else 2)
            if k % 5 == 0 and (r >= 1.6 or k != 5):   # a small gauge shows only its ends
                v = lo + (hi - lo) * k / 10
                self.T(cx + math.cos(a) * r * 0.52, cy + math.sin(a) * r * 0.52, f"{v:g}", 8 if r < 1.6 else 9, bold=True)
        self.T(cx, cy - r - 0.75, label, 8, bold=True)
        if unit:
            self.T(cx, cy - r * 0.45, unit, 8, bold=True)
        if live:
            self.live.append({"t": "dial", "x": cx, "y": cy, "r": r, "z": 15.45, "lo": lo, "hi": hi, "start": start, "sweep": sweep,
                              "ch": live})
        else:
            a = math.radians(start - sweep * (value - lo) / (hi - lo))
            self.line([(cx - math.cos(a) * r * 0.12, cy - math.sin(a) * r * 0.12), (cx + math.cos(a) * r * 0.82, cy + math.sin(a) * r * 0.82)],
                      (30, 30, 30, 255), 4)
        self.E(cx, cy, r * 0.1, fill=(30, 30, 30, 255))
        self.proud(cx - r - 0.35, cy - r - 0.35, cx + r + 0.35, cy + r + 0.35, 0.45)

    def meter(self, x0, y0, x1, y1, label, scale, value, unit, live=None):
        """A square switchboard meter: black case, white quarter-arc scale, the needle, its legend."""
        self.R(x0, y0, x1, y1, fill=(28, 28, 30, 255))
        self.R(x0 + 0.3, y0 + 0.3, x1 - 0.3, y1 - 0.3, fill=(244, 244, 238, 255))
        cx, cy, r = x0 + 0.5, y0 + 0.5, min(x1 - x0, y1 - y0) - 1.2
        lo, hi = scale
        for k in range(21):
            a = math.radians(90 - 90 * k / 20)
            r0 = r * (0.86 if k % 5 == 0 else 0.92)
            self.line([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * r, cy + math.sin(a) * r)], INK, 2)
            if k % 10 == 0:
                v = lo + (hi - lo) * k / 20
                self.T(cx + math.cos(a) * r * 0.66 + (0.35 if k == 0 else 0), cy + math.sin(a) * r * 0.66 + (0.3 if k == 20 else 0),
                       f"{v:g}", 7, bold=True)
        if live:
            self.live.append({"t": "meter", "x": cx, "y": cy, "r": r * 0.95, "z": 15.5, "lo": lo, "hi": hi, "ch": live})
        else:
            a = math.radians(90 - 90 * (value - lo) / (hi - lo))
            self.line([(cx, cy), (cx + math.cos(a) * r * 0.95, cy + math.sin(a) * r * 0.95)], (34, 34, 34, 255), 3)
        self.E(cx, cy, 0.16, fill=(34, 34, 34, 255))
        self.T(x1 - 0.55, y1 - 0.85, unit, 9, bold=True, anchor="rm")
        self.T((x0 + x1) / 2, y0 - 0.55, label, 8, bold=True)
        self.proud(x0, y0, x1, y1, 0.5)

    def lamp(self, cx, cy, colour, lit, label=None, r=0.55, live=None):
        """A pilot light: drawn dark, and lit live from channel live (if given), or painted lit."""
        if live:
            self.live.append({"t": "lamp", "x": cx, "y": cy, "r": r, "z": 15.3, "colour": list(colour), "ch": live})
            lit = False
        dark = tuple(int(c * 0.35) for c in colour)
        self.E(cx, cy, r + 0.18, fill=(40, 40, 42, 255))
        self.E(cx, cy, r, fill=(colour if lit else dark) + (255,))
        if lit:
            self.E(cx - r * 0.3, cy + r * 0.3, r * 0.3, fill=tuple(min(255, c + 70) for c in colour) + (255,))
        self.proud(cx - r - 0.18, cy - r - 0.18, cx + r + 0.18, cy + r + 0.18, 0.3)
        if label:
            self.T(cx, cy - r - 0.7, label, 7, bold=True)

    def button(self, cx, cy, colour, label, side):
        self.E(cx, cy, 0.85, fill=(50, 50, 52, 255))
        self.E(cx, cy, 0.6, fill=colour + (255,))
        self.E(cx - 0.18, cy + 0.18, 0.2, fill=tuple(min(255, c + 60) for c in colour) + (255,))
        self.proud(cx - 0.6, cy - 0.6, cx + 0.6, cy + 0.6, 0.55, side)
        self.T(cx, cy - 1.35, label, 8, bold=True)

    def knob(self, cx, cy, positions, at, label):
        """A selector switch: the positions engraved round it, the black knob with its white pointer (real)."""
        self.E(cx, cy, 1.0, fill=(46, 46, 48, 255))
        for i, name in enumerate(positions):
            a = math.radians(135 - 90 * i / max(1, len(positions) - 1))
            self.T(cx + math.cos(a) * 1.75, cy + math.sin(a) * 1.55, name, 7, bold=True)
        a = math.radians(135 - 90 * at / max(1, len(positions) - 1))
        self.proud(cx - 0.6, cy - 0.6, cx + 0.6, cy + 0.6, 0.45)
        self.line([(cx, cy), (cx + math.cos(a) * 0.55, cy + math.sin(a) * 0.55)], (240, 240, 236, 255), 4)
        self.T(cx, cy - 1.55, label, 8, bold=True)

    def annunciator(self, x0, y0, cols, rows, cw, ch, texts, lit=(), live=None):
        """An alarm window panel; with live, window i lights from channel live[i] (amber, flashing when new)."""
        self.R(x0 - 0.25, y0 - 0.25, x0 + cols * cw + 0.25, y0 + rows * ch + 0.25, fill=(30, 30, 32, 255))
        for i, text in enumerate(texts):
            c, r = i % cols, i // cols
            wx0, wy1 = x0 + c * cw + 0.1, y0 + (rows - r) * ch - 0.1
            fill = (250, 214, 120, 255) if i in lit and not live else (232, 232, 222, 255)
            self.R(wx0, wy1 - ch + 0.2, wx0 + cw - 0.2, wy1, fill=fill)
            if live and live[i]:
                # Lit, the window glows amber with its legend still on it (drawn again over the light).
                self.live.append({"t": "window", "x0": wx0, "y0": wy1 - ch + 0.2, "x1": wx0 + cw - 0.2, "y1": wy1, "z": 15.35,
                                  "colour": [255, 200, 80], "ch": live[i], "alpha": 1.0, "label": text.split("|")})
            words = text.split("|")
            for k, wd in enumerate(words):
                self.T(wx0 + (cw - 0.2) / 2, wy1 - (ch - 0.2) / 2 + (len(words) - 1) * 0.28 - k * 0.56, wd, 7, bold=True)
        self.proud(x0 - 0.25, y0 - 0.25, x0 + cols * cw + 0.25, y0 + rows * ch + 0.25, 0.35)

    def text(self, x, y, z, size, colour, ch, align="left"):
        """Live text (a display, a counter digit): size is the letters' height in model pixels."""
        self.live.append({"t": "text", "x": x, "y": y, "z": z, "size": size, "colour": list(colour), "ch": ch, "align": align})

    def glow(self, x0, y0, x1, y1, z, colour, ch, alpha=0.6, colour2=None):
        """A live coloured overlay (a mimic's breaker, a screen's row): the channel's 1 shows colour, 2 colour2."""
        e = {"t": "window", "x0": x0, "y0": y0, "x1": x1, "y1": y1, "z": z, "colour": list(colour), "ch": ch, "alpha": alpha}
        if colour2:
            e["colour2"] = list(colour2)
        self.live.append(e)

    def vent(self, x0, y0, x1, y1):
        self.R(x0, y0, x1, y1, fill=(170, 172, 168, 255))
        y = y0 + 0.25
        while y + 0.3 < y1:
            self.R(x0 + 0.3, y, x1 - 0.3, y + 0.28, fill=(40, 40, 42, 255))
            y += 0.55

    def warning(self, cx, cy, text):
        """A yellow electrical hazard triangle with its legend."""
        s = 1.1
        pts = [self.px(cx, cy + s), self.px(cx - s * 1.1, cy - s * 0.8), self.px(cx + s * 1.1, cy - s * 0.8)]
        self.d.polygon(pts, fill=(250, 204, 30, 255), outline=(20, 20, 20, 255), width=3)
        self.line([(cx + 0.15, cy + 0.6), (cx - 0.2, cy - 0.05), (cx + 0.2, cy - 0.05), (cx - 0.15, cy - 0.6)], (20, 20, 20, 255), 3)
        self.T(cx, cy - s - 0.55, text, 7, bold=True)

    def cut(self, key):
        """The drawing cut into the piece's blocks: pi_<key>_f_<part>, part = row * width + column (row 0 at the floor)."""
        width = self.W // 16
        out = {}
        for r in range(2):
            for c in range(width):
                out[f"pi_{key}_f_{r * width + c}"] = self.img.crop((c * 16 * U, (1 - r) * 16 * U, (c + 1) * 16 * U, (2 - r) * 16 * U))
        return out


# ------------------------------------------------------------------------------------------------ the panels

def engine_panel():
    f = Front(1, 401)
    f.door(0.4, 15.6)
    f.nameplate(8, 28.9, 13.2, ["ENGINE CONTROL", "EMERGENCY DIESEL GENERATOR"])
    # Rated 514 rpm (a 14-pole 60 Hz generator); the overspeed trip at 115%.
    f.dial(4.3, 24.5, 1.9, "ENGINE SPEED", (0, 800), 0, "RPM", red=(590, 800), live="rpm")
    f.dial(11.7, 24.5, 1.9, "START AIR", (0, 40), 29, "BAR", red=(0, 17), live="air")
    f.dial(2.9, 18.6, 1.3, "LUBE OIL", (0, 10), 0.4, "BAR", red=(0, 2.5), live="lube")
    f.dial(7.0, 18.6, 1.3, "JKT WATER", (0, 120), 52, "°C", red=(95, 120), live="jw")
    # The running hour meter (the tenths on red), its digits live.
    f.R(9.6, 17.8, 14.6, 19.4, fill=(28, 28, 30, 255))
    for k in range(6):
        f.R(9.9 + k * 0.76, 18.1, 10.5 + k * 0.76, 19.1, fill=(240, 240, 236, 255) if k < 5 else (200, 40, 34, 255))
        f.text(10.2 + k * 0.76, 18.6, 15.32, 0.62, INK if k < 5 else (255, 255, 255), f"hours{k}", "center")
    f.T(12.1, 17.1, "HOURS RUN", 7, bold=True)
    f.proud(9.6, 17.8, 14.6, 19.4, 0.3)
    f.annunciator(1.6, 9.2, 3, 4, 4.25, 1.45, [
        "LOW LUBE|OIL PRESS", "HIGH JKT|WATER TEMP", "OVER|SPEED", "FAIL TO|START", "LOW START|AIR PRESS", "LOW DAY|TANK LEVEL",
        "HIGH CRANK|CASE PRESS", "BATTERY|CHGR FAIL", "GEN BKR|TRIPPED", "EMERG|STOP", "ENGINE|RUNNING", "READY FOR|AUTO START"], lit={11},
        live=["ann_low_lube", "ann_high_jw", "ann_overspeed", "ann_fail_start", "ann_low_air", "ann_low_day_tank", "ann_crankcase",
              "ann_charger_fail", "ann_bkr_trip", "ann_estop", "ann_running", "ann_ready"])
    f.knob(3.4, 5.6, ["OFF", "LOCAL", "AUTO"], 2, "MODE")
    f.button(7.4, 6.2, (40, 170, 70), "START", "pi_pnl_green")
    f.button(10.0, 6.2, (200, 40, 34), "STOP", "pi_pnl_red")
    f.lamp(7.4, 3.0, (240, 240, 230), True, "READY", live="lamp_ready")
    f.lamp(10.0, 3.0, (230, 60, 40), False, "RUN", live="lamp_run")
    # The emergency stop: a red mushroom on a yellow collar (real), its legend.
    f.E(13.3, 5.8, 1.15, fill=(250, 204, 30, 255))
    f.T(13.3, 3.9, "EMERG", 7, bold=True)
    f.T(13.3, 3.3, "STOP", 7, bold=True)
    f.proud(12.35, 4.85, 14.25, 6.75, 0.2, "pi_pnl_yellow")
    f.proud(12.6, 5.1, 14.0, 6.5, 0.75, "pi_pnl_red")
    f.E(13.3, 5.8, 0.7, fill=(200, 36, 30, 255))
    return f


def generator_panel():
    f = Front(2, 402)
    f.door(0.4, 16.0, handle=False)
    f.door(16.0, 31.6)
    f.nameplate(16, 28.9, 15, ["GENERATOR CONTROL", "4160 V  3 PH  60 HZ  7000 KW"])
    meters = [("AC VOLTS", (0, 5), 0, "kV", "kv"), ("AC AMPS", (0, 1500), 0, "A", "amps"), ("KILOWATTS", (0, 8000), 0, "kW", "kw"),
              ("KILOVARS", (-4000, 4000), 0, "kVAR", "kvar")]
    for i, (label, scale, value, unit, ch) in enumerate(meters):
        x = 1.6 + i * 7.4
        f.meter(x, 22.2, x + 5.4, 26.6, label, scale, value, unit, live=ch)
    f.meter(1.6, 15.6, 7.0, 20.0, "FREQUENCY", (55, 65), 55, "Hz", live="hz")
    f.meter(24.6, 15.6, 30.0, 20.0, "POWER FACTOR", (0, 1), 0, "PF", live="pf")
    # The synchroscope in the middle, its two sync lamps either side and its legend.
    cx, cy, r = 16, 17.6, 2.7
    f.R(cx - r - 0.5, cy - r - 0.5, cx + r + 0.5, cy + r + 0.5, fill=(28, 28, 30, 255))
    f.E(cx, cy, r, fill=(244, 244, 238, 255))
    for k in range(24):
        a = math.radians(90 - 15 * k)
        f.line([(cx + math.cos(a) * r * 0.82, cy + math.sin(a) * r * 0.82), (cx + math.cos(a) * r * 0.95, cy + math.sin(a) * r * 0.95)],
               INK, 3 if k % 6 == 0 else 2)
    f.R(cx - 0.3, cy + r * 0.62, cx + 0.3, cy + r * 0.95, fill=(200, 40, 34, 255))
    f.T(cx - 1.25, cy + 0.45, "SLOW", 8, bold=True)
    f.T(cx + 1.25, cy + 0.45, "FAST", 8, bold=True)
    f.line([(cx, cy), (cx, cy + r * 0.78)], (30, 30, 30, 255), 4)
    f.E(cx, cy, 0.25, fill=(30, 30, 30, 255))
    f.T(cx, cy - r - 1.0, "SYNCHROSCOPE", 8, bold=True)
    f.proud(cx - r - 0.5, cy - r - 0.5, cx + r + 0.5, cy + r + 0.5, 0.55)
    f.lamp(10.6, 17.6, (240, 240, 230), False, "SYNC")
    f.lamp(21.4, 17.6, (240, 240, 230), False, "SYNC")
    # The mimic: the generator onto its breaker onto the 4160 V bus.
    green, red, mim = (40, 160, 70), (200, 40, 34), (30, 60, 140)
    f.E(3.2, 11.6, 1.2, outline=mim + (255,), width=5)
    f.T(3.2, 11.6, "G", 14, fill=mim + (255,), bold=True)
    f.line([(4.4, 11.6), (8.2, 11.6)], mim + (255,), 6)
    f.R(8.2, 10.7, 10.0, 12.5, fill=green + (255,), outline=(20, 20, 20, 255), width=2)
    f.glow(8.3, 10.8, 9.9, 12.4, 15.06, red, "bkr_closed", alpha=0.92)    # the breaker shown closed (red)
    f.line([(10.0, 11.6), (11.8, 11.6)], mim + (255,), 6)
    f.line([(11.8, 9.2), (11.8, 14.0)], mim + (255,), 8)
    f.T(11.8, 8.5, "BUS 1A", 8, bold=True)
    f.T(9.1, 13.2, "52G", 8, bold=True)
    # The breaker's control switch (pistol grip, real) with its open (green) and closed (red) lamps.
    f.lamp(19.6, 13.2, green, True, None, live="bkr_open")
    f.lamp(22.6, 13.2, red, False, None, live="bkr_closed")
    f.T(19.6, 14.3, "OPEN", 7, bold=True)
    f.T(22.6, 14.3, "CLOSED", 7, bold=True)
    f.E(21.1, 10.3, 1.05, fill=(46, 46, 48, 255))
    f.T(19.3, 11.3, "TRIP", 7, bold=True)
    f.T(22.9, 11.3, "CLOSE", 7, bold=True)
    f.proud(20.75, 9.3, 21.45, 11.3, 0.7)
    f.T(21.1, 8.5, "GEN BREAKER", 8, bold=True)
    f.knob(26.4, 10.6, ["LOWER", "", "RAISE"], 1, "VOLTAGE ADJ")
    f.knob(6.0, 6.0, ["LOWER", "", "RAISE"], 1, "SPEED ADJ")
    f.knob(11.6, 6.0, ["OFF", "ON"], 0, "SYNC")
    f.knob(26.4, 5.4, ["OFF", "ON"], 1, "EXCITATION")
    f.tag(19.6, 6.6, "LOCAL / REMOTE TRANSFER IN PANEL 1A-C", 7)
    f.vent(2.0, 1.8, 14.0, 3.2)
    f.vent(18.0, 1.8, 30.0, 3.2)
    return f


def voltage_regulator():
    f = Front(1, 403)
    f.door(0.4, 15.6)
    f.nameplate(8, 28.9, 13.2, ["EXCITATION SYSTEM", "AUTOMATIC VOLTAGE REGULATOR"])
    # The regulator's display and status lights.
    f.R(2.0, 22.6, 14.0, 26.4, fill=(26, 28, 30, 255))
    f.R(2.5, 23.1, 13.5, 25.9, fill=(16, 40, 26, 255))
    f.text(3.0, 25.1, 15.27, 0.62, (110, 240, 140), "avr1")
    f.text(3.0, 23.8, 15.27, 0.62, (110, 240, 140), "avr2")
    f.proud(2.0, 22.6, 14.0, 26.4, 0.25)
    for i, (name, col, lit) in enumerate((("POWER", (40, 180, 70), True), ("AUTO", (40, 180, 70), True),
                                          ("MANUAL", (240, 170, 30), False), ("FAULT", (220, 40, 34), False))):
        f.lamp(3.0 + i * 3.35, 20.0, col, lit, name, r=0.45)
    f.meter(1.6, 13.4, 7.6, 17.6, "FIELD VOLTS", (0, 250), 0, "V", live="field_v")
    f.meter(8.4, 13.4, 14.4, 17.6, "FIELD AMPS", (0, 150), 0, "A", live="field_a")
    f.vent(2.0, 6.2, 14.0, 11.6)
    f.warning(8.0, 3.8, "DANGER  HIGH VOLTAGE")
    return f


def protection_panel():
    f = Front(1, 404)
    f.door(0.4, 15.6)
    f.nameplate(8, 28.9, 13.2, ["GENERATOR PROTECTION", "RELAYS  52G / 86G"])
    relays = [("87G", "GEN DIFF"), ("51V", "VOLT RESTR O/C"), ("32", "REVERSE POWER"), ("40", "LOSS OF FIELD"),
              ("46", "NEG SEQUENCE"), ("81", "FREQUENCY"), ("27/59", "UNDER/OVER V"), ("64G", "STATOR GND")]
    for i, (num, name) in enumerate(relays):
        c, r = i % 2, i // 2
        x0, y1 = 1.4 + c * 7.0, 26.6 - r * 4.2
        f.R(x0, y1 - 3.6, x0 + 6.2, y1, fill=(36, 38, 42, 255))
        f.R(x0 + 0.3, y1 - 3.3, x0 + 5.9, y1 - 0.3, fill=(70, 76, 82, 255))
        f.R(x0 + 0.6, y1 - 1.6, x0 + 3.4, y1 - 0.6, fill=(26, 30, 28, 255))
        f.text(x0 + 2.0, y1 - 1.1, 15.62, 0.5, (120, 240, 150), f"relay{i}", "center")
        f.lamp(x0 + 4.6, y1 - 1.1, (40, 180, 70), True, None, r=0.32)
        f.T(x0 + 3.1, y1 - 2.4, name, 7, fill=(230, 230, 226, 255), bold=True)
        f.proud(x0, y1 - 3.6, x0 + 6.2, y1, 0.6)
    # The test switches: a row of black levers in their block.
    f.R(1.6, 8.2, 14.4, 9.8, fill=(30, 30, 32, 255))
    for k in range(10):
        f.R(2.1 + k * 1.24, 8.6, 2.6 + k * 1.24, 9.4, fill=(210, 40, 34, 255) if k in (0, 9) else (230, 230, 226, 255))
    f.proud(1.6, 8.2, 14.4, 9.8, 0.35)
    f.T(8.0, 7.5, "TEST SWITCHES", 7, bold=True)
    # The lockout relay: its round face and the black T-handle (real).
    f.E(8.0, 4.4, 1.6, fill=(30, 30, 32, 255))
    f.E(8.0, 4.4, 1.35, fill=(236, 236, 230, 255))
    f.T(6.0, 5.9, "RESET", 7, bold=True)
    f.T(10.0, 5.9, "TRIP", 7, bold=True)
    f.T(8.0, 2.2, "86G  LOCKOUT", 8, bold=True)
    f.proud(7.75, 3.0, 8.25, 5.8, 0.7)
    f.proud(7.2, 5.3, 8.8, 5.8, 0.75)
    return f


def load_sequencer():
    f = Front(1, 405)
    f.door(0.4, 15.6)
    f.nameplate(8, 28.9, 13.2, ["LOAD SEQUENCER", "SAFETY BUS 1A"])
    # The operator screen: the sequence of loads it starts after the generator takes the bus.
    f.R(1.8, 18.6, 14.2, 26.8, fill=(26, 28, 30, 255))
    f.R(2.2, 19.0, 13.8, 26.4, fill=(232, 238, 244, 255))
    f.R(2.2, 25.6, 13.8, 26.4, fill=(30, 80, 150, 255))
    f.text(8.0, 26.0, 15.27, 0.48, (255, 255, 255), "seq_head", "center")
    steps = [("1", "4160V BUS 1A", "0"), ("2", "SAFETY INJ PUMP", "5"), ("3", "RHR PUMP", "10"), ("4", "COMP COOL PUMP", "15"),
             ("5", "SERVICE WTR PUMP", "20"), ("6", "CONT SPRAY PUMP", "25"), ("7", "HVAC FANS", "30"), ("8", "BATT CHARGERS", "35")]
    for i, (n, load, t) in enumerate(steps):
        y = 24.9 - i * 0.74
        f.T(2.7, y, n, 8, bold=True, anchor="lm")
        f.T(3.6, y, load, 8, anchor="lm")
        f.T(13.3, y, f"T+{t}s", 8, bold=True, anchor="rm")
        f.glow(2.3, y - 0.34, 13.7, y + 0.34, 15.27, (70, 200, 100), f"seq_row{i}", alpha=0.38, colour2=(250, 190, 50))
    f.proud(1.8, 18.6, 14.2, 26.8, 0.25)
    for i in range(8):
        f.lamp(2.2 + i * 1.66, 16.2, (240, 240, 230), False, str(i + 1), r=0.42, live=f"seq_lamp{i}")
    f.T(8.0, 14.5, "STEP", 7, bold=True)
    # The controller behind its window: a rack of modules with their lights.
    f.R(1.8, 6.4, 14.2, 13.2, fill=(60, 64, 70, 255))
    f.R(2.2, 6.8, 13.8, 12.8, fill=(126, 150, 160, 255))
    rng = random.Random(7)
    for k in range(7):
        x = 2.5 + k * 1.6
        f.R(x, 7.1, x + 1.35, 12.5, fill=(48, 52, 58, 255))
        f.R(x + 0.15, 11.4, x + 1.2, 12.2, fill=(200, 200, 196, 255))
        for j in range(6):
            col = rng.choice([(60, 220, 90), (60, 220, 90), (60, 60, 60), (240, 170, 30)])
            f.R(x + 0.35, 10.6 - j * 0.55, x + 0.6, 10.85 - j * 0.55, fill=col + (255,))
    f.proud(1.8, 6.4, 14.2, 13.2, 0.2)
    f.knob(5.0, 3.2, ["NORMAL", "TEST"], 0, "MODE")
    f.button(11.0, 3.6, (240, 170, 30), "RESET", "pi_pnl_yellow")
    return f


def battery_charger():
    f = Front(1, 406)
    f.door(0.4, 15.6)
    f.nameplate(8, 28.9, 13.2, ["125 VDC BATTERY CHARGER", "INPUT 480 VAC  OUTPUT 150 A"])
    f.meter(1.6, 21.8, 7.6, 26.0, "DC VOLTS", (0, 150), 132, "V", live="dc_v")
    f.meter(8.4, 21.8, 14.4, 26.0, "DC AMPS", (0, 200), 12, "A", live="dc_a")
    for i, (name, col, lit, ch) in enumerate((("AC ON", (40, 180, 70), True, "chg_ac"), ("DC ON", (40, 180, 70), True, "chg_dc"),
                                              ("FLOAT", (240, 240, 230), True, "chg_float"), ("EQUAL", (240, 170, 30), False, "chg_equal"))):
        f.lamp(3.0 + i * 3.35, 18.2, col, lit, name, r=0.45, live=ch)
    f.knob(5.0, 12.8, ["FLOAT", "EQUAL"], 0, "MODE")
    # The AC input breaker's handle (real) in its window.
    f.R(9.6, 11.2, 13.0, 14.6, fill=(40, 42, 46, 255))
    f.R(10.8, 12.0, 11.8, 14.0, fill=(20, 20, 22, 255))
    f.T(11.3, 10.5, "AC INPUT", 7, bold=True)
    f.proud(10.95, 12.6, 11.65, 13.9, 0.6)
    f.vent(2.0, 3.0, 14.0, 8.6)
    return f


DRAWINGS = {"edg_engine_panel": engine_panel, "edg_generator_panel": generator_panel, "edg_voltage_regulator": voltage_regulator,
            "edg_protection_panel": protection_panel, "edg_load_sequencer": load_sequencer, "edg_battery_charger": battery_charger}
_FRONTS = {}


def layout():
    """What moves on each panel (assets/jugcraft/live_panels.json, read by client/LivePanelRenderer.java): positions in
    model pixels on the piece facing north (x across to the right, y up from the floor, z out to the front)."""
    return {key: {"width": PANELS[key]["width"], "elements": front(key).live} for key in DRAWINGS}


def front(key):
    if key not in _FRONTS:
        _FRONTS[key] = DRAWINGS[key]()
    return _FRONTS[key]


# ------------------------------------------------------------------------------------------------ the models

def cabinet(key):
    """The whole piece: plinth, body (its front the drawing), top, and what stands proud of the door."""
    if key == "edg_battery_rack":
        return battery_rack()
    W = PANELS[key]["width"] * 16
    F = f"pi_{key}_f"
    boxes = [(0.8, 0, 2.6, W - 0.8, 1.2, 14.6, "pi_toe_kick"),
             (0.4, 1.2, 2, W - 0.4, 30.6, 15, {"*": "pi_cabinet", "south": F}),
             (0.2, 30.6, 1.8, W - 0.2, 31.4, 15.3, "pi_cabinet_dark")]
    for x0, y0, x1, y1, depth, side in front(key).boxes:
        boxes.append((x0, y0, 15, x1, y1, 15 + depth, {"*": side, "south": F}))
    return boxes


def battery_rack():
    """Two tiers of lead-acid cells on an open steel rack: three cells a tier, their posts, the links between them."""
    boxes = []
    for x in (0.6, 14.4):
        for z in (3.2, 12.8):
            boxes.append((x, 0, z, x + 1.0, 30.4, z + 1.0, "pi_rack_green"))
    for y in (1.2, 16.0):
        boxes.append((0.6, y, 3.2, 15.4, y + 0.6, 4.2, "pi_rack_green"))
        boxes.append((0.6, y, 12.8, 15.4, y + 0.6, 13.8, "pi_rack_green"))
        boxes.append((1.6, y + 0.2, 4.2, 14.4, y + 0.6, 12.8, "pi_rack_rail"))
        top = y + 0.6
        for k in range(3):
            x0 = 1.9 + k * 4.25
            boxes.append((x0, top, 4.6, x0 + 3.9, top + 10.4, 12.4, {"*": "pi_battery_cell", "up": "pi_battery_top!",
                                                                    "south": "pi_battery_front!", "north": "pi_battery_front!"}))
            for px, tex in ((x0 + 0.8, "pi_pnl_red"), (x0 + 2.6, "pi_pnl_black")):
                boxes.append((px, top + 10.4, 7.8, px + 0.6, top + 11.1, 8.4, tex))
            if k < 2:
                boxes.append((x0 + 2.55, top + 11.1, 7.85, x0 + 5.05, top + 11.4, 8.35, "pi_link"))
    boxes.append((0.6, 30.4, 3.2, 15.4, 31.0, 13.8, "pi_rack_green"))
    boxes.append((4.0, 27.4, 13.8, 12.0, 29.4, 13.9, {"*": "pi_pnl_white", "south": "pi_battery_tag!"}))
    return boxes


def part(key, c, r):
    """Block (c, r)'s share of the piece (r 0 at the floor), its front texture the drawing's slice for that block."""
    width = PANELS[key]["width"]
    F = f"pi_{key}_f"
    out = []
    for b in cabinet(key):
        x0, x1 = max(b[0], c * 16), min(b[3], c * 16 + 16)
        y0, y1 = max(b[1], r * 16), min(b[4], r * 16 + 16)
        if x1 - x0 < 0.01 or y1 - y0 < 0.01:
            continue
        t = b[6]
        if isinstance(t, dict):
            t = {k: (f"{F}_{r * width + c}" if v == F else v) for k, v in t.items()}
        out.append((x0 - c * 16, y0 - r * 16, b[2], x1 - c * 16, y1 - r * 16, b[5], t) + tuple(b[7:]))
    return out


# ------------------------------------------------------------------------------------------------ textures

def _solid(colour, seed, dirt=0.05):
    return M.painted(colour, seed, seams=(), chip_count=0, drip_count=0, dirt=dirt)


def _cell():
    """A cell's clear jar from the side: the plates inside, the electrolyte line, the case edge."""
    img = Image.new("RGBA", (128, 128), (40, 40, 44, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 127, 127], fill=(176, 186, 180, 255))
    d.rectangle([6, 24, 121, 121], fill=(70, 74, 72, 255))
    for x in range(10, 120, 8):
        d.rectangle([x, 30, x + 4, 118], fill=(92, 76, 64, 255) if (x // 8) % 2 else (64, 66, 70, 255))
    d.line([(4, 22), (123, 22)], fill=(120, 170, 200, 255), width=3)
    d.rectangle([0, 0, 127, 12], fill=(30, 30, 32, 255))
    return img


def _cell_front():
    img = _cell()
    d = ImageDraw.Draw(img)
    d.rectangle([36, 62, 92, 90], fill=(240, 240, 234, 255), outline=(30, 30, 30, 255), width=2)
    d.text((64, 70), "2.20 V", font=cfont(14, True), fill=INK, anchor="mm")
    d.text((64, 83), "CELL", font=cfont(10, True), fill=INK, anchor="mm")
    return img


def _cell_top():
    img = Image.new("RGBA", (128, 128), (32, 32, 34, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 123, 123], fill=(46, 46, 50, 255))
    d.ellipse([52, 52, 76, 76], fill=(200, 200, 196, 255))   # the vent cap
    d.ellipse([58, 58, 70, 70], fill=(120, 120, 118, 255))
    return img


def _battery_tag():
    img = Image.new("RGBA", (256, 64), (240, 240, 234, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 63], outline=(30, 30, 30, 255), width=3)
    d.text((128, 22), "125 VDC BATTERY 1A", font=cfont(24, True), fill=INK, anchor="mm")
    d.text((128, 46), "NO SMOKING  NO SPARKS", font=cfont(16, True), fill=(200, 30, 30, 255), anchor="mm")
    return img.resize((128, 128))


def makers():
    out = {}
    for key in DRAWINGS:
        for name, img in front(key).cut(key).items():
            out[name] = (lambda img=img: img)
    out.update({
        "pi_pnl_black": lambda: _solid((30, 30, 32), 411), "pi_pnl_red": lambda: _solid((196, 38, 32), 412),
        "pi_pnl_green": lambda: _solid((40, 160, 70), 413), "pi_pnl_yellow": lambda: _solid((236, 196, 30), 414),
        "pi_pnl_white": lambda: _solid((236, 236, 230), 415), "pi_rack_green": lambda: _solid((70, 100, 80), 416, 0.12),
        "pi_rack_rail": lambda: _solid((150, 40, 30), 417, 0.1), "pi_link": lambda: _solid((150, 152, 156), 418),
        "pi_battery_cell": _cell, "pi_battery_front": _cell_front, "pi_battery_top": _cell_top, "pi_battery_tag": _battery_tag,
    })
    return out


# Kept at full size so the writing reads.
SHARP = {f"pi_{key}_f_{p}" for key in DRAWINGS for p in range(2 * PANELS[key]["width"])} | {"pi_battery_tag", "pi_battery_front"}


def icon(key):
    """The inventory icon: the front, small."""
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    if key == "edg_battery_rack":
        d = ImageDraw.Draw(img)
        d.rectangle([28, 4, 100, 124], outline=(70, 100, 80, 255), width=6)
        for y in (20, 80):
            for k in range(3):
                d.rectangle([34 + k * 21, y, 52 + k * 21, y + 38], fill=(176, 186, 180, 255), outline=(40, 40, 44, 255), width=2)
        return M.stylize(img, 16)
    f = front(key).img
    w = f.width * 124 // f.height
    small = f.resize((w, 124), Image.LANCZOS)
    img.paste(small, ((128 - w) // 2, 2))
    return M.stylize(img, 24)


def live_sheet():
    """client/LivePanelRenderer.java's sheet (textures/entity/live_panel.png, 64 px): white (0, 0), a lamp's lit lens
    (1, 0: a disc with a bright spot, tinted by the renderer), a round hub (2, 0), each 16 px."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], fill=(255, 255, 255, 255))
    big = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    b = ImageDraw.Draw(big)
    b.ellipse([0, 0, 63, 63], fill=(205, 205, 205, 255))
    b.ellipse([10, 8, 34, 30], fill=(255, 255, 255, 255))
    img.paste(big.resize((16, 16), Image.LANCZOS), (16, 0))
    hub = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    ImageDraw.Draw(hub).ellipse([0, 0, 63, 63], fill=(255, 255, 255, 255))
    img.paste(hub.resize((16, 16), Image.LANCZOS), (32, 0))
    # Cut the soft edges out (the renderer uses cutout): alpha either 0 or 255.
    a = np.asarray(img).copy()
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    return Image.fromarray(a, "RGBA")
