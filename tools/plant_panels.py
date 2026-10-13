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
    # The control building's DC system (round 18): the station battery, its distribution switchboard and the vital
    # inverter (the battery chargers are the EDG rooms' charger panel).
    "station_battery": {"display": "Station Battery (125 VDC, 30 Cells)", "width": 4},
    "dc_switchboard": {"display": "125 VDC Distribution Switchboard", "width": 2},
    "vital_inverter": {"display": "Vital AC Inverter", "width": 1},
}
for _k, _v in PANELS.items():
    _v["multi"] = (_v["width"], 2, "panel")
    _v["panel"] = _k

# The DC power cables from a station battery to its switchboard (round 18): up the wall from the rack's end in a riser
# (two heavy cables on a strut, cleated), turning into a cable tray under the ceiling at the top, the tray across to
# the switchboard and a riser down into its top. Plain pieces, facing north: the wall behind them is at z 0 and the
# tray runs along x. The cables are where the battery's rise out of it (SB_RISE_*).
_RED, _BLACK = (6.2, 7.8), (8.6, 10.2)
_CZ = (0.5, 2.1)
_TRAY = [(0, 12, 4, 16, 12.5, 12, "pi_chrome"), (0, 12.5, 11.5, 16, 14, 12, "pi_chrome"),
         (0, 12.5, 5, 16, 13.5, 7, "pi_sb_cable_red"), (0, 12.5, 7.5, 16, 13.5, 11, "pi_sb_cable"),
         (7.5, 14, 7.5, 8.5, 16, 8.5, "pi_chrome")]
RISERS = {
    "dc_cable_tray": {"display": "DC Cable Tray", "boxes": [(0, 12.5, 4, 16, 14, 4.5, "pi_chrome")] + _TRAY},
    "dc_cable_riser": {"display": "DC Cable Riser", "boxes": [
        (5.4, 0, 0, 11.0, 16, 0.4, "pi_chrome"),
        (_RED[0], 0, _CZ[0], _RED[1], 16, _CZ[1], "pi_sb_cable_red"), (_BLACK[0], 0, _CZ[0], _BLACK[1], 16, _CZ[1], "pi_sb_cable"),
        (5.6, 3, 0.4, 10.8, 4, 2.4, "pi_sb_frame"), (5.6, 11, 0.4, 10.8, 12, 2.4, "pi_sb_frame")]},
    # The top of a riser: the cables turn out from the wall into the tray (its back wall left open there).
    "dc_cable_riser_top": {"display": "DC Cable Riser (Top)", "boxes": [
        (5.4, 0, 0, 11.0, 12, 0.4, "pi_chrome"),
        (_RED[0], 0, _CZ[0], _RED[1], 13.5, _CZ[1], "pi_sb_cable_red"), (_BLACK[0], 0, _CZ[0], _BLACK[1], 14.3, _CZ[1], "pi_sb_cable"),
        (5.6, 3, 0.4, 10.8, 4, 2.4, "pi_sb_frame"),
        (_RED[0], 12.5, _CZ[1], _RED[1], 13.5, 5.0, "pi_sb_cable_red"),
        (_BLACK[0], 13.5, _CZ[1], _BLACK[1], 14.3, 9.0, "pi_sb_cable"),
        (0, 12.5, 4, 5.6, 14, 4.5, "pi_chrome"), (10.8, 12.5, 4, 16, 14, 4.5, "pi_chrome")] + _TRAY},
}

CRAFTING = {
    "edg_engine_panel": (["PGP", "PCP", "PRP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
    "edg_generator_panel": (["PGP", "CRC", "PPP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
    "edg_voltage_regulator": (["PCP", "PRP", "PCP"], {"P": "#c:plates/steel", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
    "edg_protection_panel": (["PGP", "PCP", "PCP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit"}, 1),
    "edg_load_sequencer": (["PXP", "PCP", "PRP"], {"P": "#c:plates/steel", "X": "jugcraft:processor", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
    "edg_battery_charger": (["PRP", "PCP", "PRP"], {"P": "#c:plates/steel", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
    "edg_battery_rack": (["S S", "BBB", "S S"], {"S": "#c:plates/steel", "B": "jugcraft:lead_acid_pack"}, 1),
    "station_battery": (["BBB", "BBB", "SSS"], {"S": "#c:plates/steel", "B": "jugcraft:lead_acid_pack"}, 1),
    "dc_switchboard": (["PGP", "CRC", "PPP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit",
                                               "R": "minecraft:redstone"}, 1),
    "vital_inverter": (["PCP", "PXP", "PRP"], {"P": "#c:plates/steel", "C": "jugcraft:basic_circuit", "X": "jugcraft:processor",
                                               "R": "minecraft:redstone"}, 1),
    "dc_cable_tray": (["P P", "CPC"], {"P": "#c:plates/iron", "C": "minecraft:copper_ingot"}, 4),
    "dc_cable_riser": (["PC", "P ", "PC"], {"P": "#c:plates/iron", "C": "minecraft:copper_ingot"}, 4),
    "dc_cable_riser_top": (["PCP", "PC "], {"P": "#c:plates/iron", "C": "minecraft:copper_ingot"}, 2),
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


def dc_switchboard():
    """The 125 VDC distribution switchboard a station battery and its chargers feed (two sections): the bus meters, the
    ground detector (a lamp on each side of the bus to ground, both lit while neither side is grounded), the main
    breaker, and the feeder breakers to everything that runs on the battery, each labelled."""
    f = Front(2, 407)
    f.door(0.4, 16.0, handle=False)
    f.door(16.0, 31.6)
    f.nameplate(16, 29.0, 21, ["125 VDC DISTRIBUTION SWITCHBOARD", "DC BUS 1A    1200 A    FROM BATTERY 1A"])
    f.meter(2.0, 21.4, 7.6, 25.6, "BUS VOLTS", (0, 150), 132, "V", live="dc_v")
    f.meter(8.6, 21.4, 14.2, 25.6, "BATT AMPS", (0, 200), 12, "A", live="dc_a")
    # The ground detector.
    f.R(2.0, 15.6, 14.2, 19.6, fill=(52, 54, 58, 255))
    f.T(8.1, 18.8, "GROUND DETECTOR", 7, fill=(236, 236, 230, 255), bold=True)
    f.lamp(5.0, 17.0, (240, 240, 230), True, None, r=0.5)
    f.lamp(11.2, 17.0, (240, 240, 230), True, None, r=0.5)
    f.T(5.0, 15.95, "+ BUS", 6, fill=(236, 236, 230, 255), bold=True)
    f.T(11.2, 15.95, "- BUS", 6, fill=(236, 236, 230, 255), bold=True)
    f.proud(2.0, 15.6, 14.2, 19.6, 0.25)
    # The main breaker: an insulated-case breaker in its cubicle window, the charging handle and the OPEN/CLOSED flag.
    f.R(3.0, 5.0, 13.2, 13.8, fill=(36, 38, 42, 255))
    f.R(4.0, 6.0, 12.2, 12.8, fill=(22, 22, 24, 255))
    f.R(6.4, 10.6, 9.8, 11.8, fill=(200, 40, 34, 255))
    f.T(8.1, 11.2, "CLOSED", 7, fill=(255, 255, 255, 255), bold=True)
    f.T(8.1, 9.4, "MAIN 72-1A", 8, fill=(236, 236, 230, 255), bold=True)
    f.T(8.1, 8.4, "1200 A  250 VDC", 7, fill=(200, 200, 196, 255), bold=True)
    f.proud(4.0, 6.0, 12.2, 12.8, 0.4)
    f.proud(7.3, 6.6, 8.9, 7.8, 0.9)
    f.warning(8.1, 2.9, "125 VDC")
    # The feeders: two columns of moulded-case breakers, each with its handle up (on) and its label.
    feeders = ["INVERTER 1A", "CHARGER 1A", "4160V SWGR 1A CTL", "480V LC 1A CTL", "EDG 1A CONTROL", "MCR PANELS TRAIN A",
               "RELAY RM CAB 1-6", "TURB AUX OIL PUMP", "EMERG LIGHTING", "AFW TURB CONTROL", "COMPUTER UPS 1A", "SPARE"]
    for i, name in enumerate(feeders):
        c, r = i % 2, i // 2
        x0, y1 = 17.4 + c * 7.3, 26.4 - r * 3.7
        f.R(x0, y1 - 2.6, x0 + 6.2, y1, fill=(30, 30, 32, 255))
        f.R(x0 + 2.5, y1 - 2.1, x0 + 3.7, y1 - 0.5, fill=(54, 56, 60, 255))
        f.proud(x0, y1 - 2.6, x0 + 6.2, y1, 0.45)
        f.proud(x0 + 2.75, y1 - 1.3, x0 + 3.45, y1 - 0.6, 0.85)    # the handle, up: on
        f.tag(x0 + 3.1, y1 - 3.05, name, size=6, w=6.2)
    f.vent(17.6, 2.0, 30.4, 4.6)
    return f


def vital_inverter():
    """A 120 VAC vital bus inverter: the battery's DC made into steady AC for the instrument and control buses, with
    its static transfer switch to the bypass supply."""
    f = Front(1, 408)
    f.door(0.4, 15.6)
    f.nameplate(8, 29.0, 13.4, ["VITAL AC INVERTER 1A", "125 VDC IN  120 VAC 60 HZ 10 KVA"])
    f.meter(1.6, 22.0, 7.6, 26.2, "AC VOLTS", (0, 150), 120, "V")
    f.meter(8.4, 22.0, 14.4, 26.2, "AC AMPS", (0, 100), 46, "A")
    # The frequency meter: a small digital readout.
    f.R(4.0, 18.0, 12.0, 20.2, fill=(26, 26, 28, 255))
    f.R(4.4, 18.4, 11.6, 19.8, fill=(18, 40, 26, 255))
    f.T(8.0, 19.1, "60.00 Hz", 9, fill=(120, 250, 150, 255), bold=True)
    f.proud(4.0, 18.0, 12.0, 20.2, 0.3)
    for i, (name, col, lit) in enumerate((("DC IN", (40, 180, 70), True), ("INVERTER", (40, 180, 70), True),
                                          ("ON INV", (240, 240, 230), True), ("BYPASS", (240, 170, 30), False))):
        f.lamp(3.0 + i * 3.35, 15.0, col, lit, name, r=0.45)
    f.knob(5.0, 9.6, ["INV", "BYPASS"], 0, "MANUAL BYPASS")
    f.button(11.4, 10.0, (240, 240, 230), "ALARM RESET", "pi_pnl_white")
    f.vent(2.0, 2.4, 14.0, 6.6)
    return f


DRAWINGS = {"edg_engine_panel": engine_panel, "edg_generator_panel": generator_panel, "edg_voltage_regulator": voltage_regulator,
            "edg_protection_panel": protection_panel, "edg_load_sequencer": load_sequencer, "edg_battery_charger": battery_charger,
            "dc_switchboard": dc_switchboard, "vital_inverter": vital_inverter}
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
    if key == "station_battery":
        return station_battery()
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


# The station battery (round 18): a two-step seismic rack four blocks long holding 30 large flooded lead-acid cells,
# 15 on the lower step at the front and 15 on the upper step behind it, so two racks make the 60-cell 125 VDC battery.
# The cells' textures follow their place in the block (no whole-picture faces), so a cell can straddle two blocks.
SB_CELLS = 15
SB_X0, SB_PITCH, SB_W = 2.15, 4.0, 3.7          # the first cell's left side, cell to cell, a cell's width
SB_LOW = (9.2, 15.2, 3.4, 14.4)                   # the lower step's cells: z0, z1, jar bottom, jar top
SB_HIGH = (2.4, 8.4, 16.4, 27.4)                  # the upper step's cells
SB_COVER = 0.8                                    # the black cover on top of the jar
SB_POST = 0.9                                     # how far the posts stand up out of the cover
# Where the two main cables rise out of the rack's back at its right-hand end (red: positive, black: negative), as the
# riser above it (dc_cable_riser) carries them on: x across the block they rise in, z.
SB_RISE_RED, SB_RISE_BLACK, SB_RISE_Z = (6.2, 7.8), (8.6, 10.2), (0.5, 2.1)
# The station battery's clear parts (kept translucent: plant_office2.TRANSLUCENT).
SEE_THROUGH = {"pi_sb_jar_glass", "pi_sb_acid"}
# The nameplate (round 25b): where it is (x0, y0, x1, y1, on the lower step's front rail, in the first block) and the
# band of its texture it shows, so the texture's pixel letters stay square on it (128 texels across its 12 pixels,
# 15 texel rows down its 1.4).
SB_TAG = (2.5, 2.0, 14.5, 3.4)
SB_TAG_ROWS = 15
SB_TAG_UV = [0, 0, 16, round(16 * SB_TAG_ROWS / 128, 4)]


def station_battery():
    """The station battery, facing north (its front, the lower step, toward whoever stands at it, +z; its back to the
    wall): a spill-containment pan on the floor, the black epoxy-coated step rack (uprights at the ends and the middle,
    rails under each step), 15 cells a step in clear jars (the plates show through, the electrolyte line, the black
    cover with its flame-arrester vent and two posts), lead-plated intercell connectors post to post, a flexible jumper
    from the lower step to the upper at the left end, and at the right end the battery's two terminals with their heavy
    cables, run along the back and up out of the top into the cable riser above."""
    B = "pi_sb_frame"
    boxes = [(0.3, 0, 0.3, 63.7, 1.0, 15.7, {"*": "pi_sb_pan", "up": "pi_sb_pan_top"}),
             # The pan's lip.
             (0.3, 1.0, 15.3, 63.7, 2.0, 15.7, "pi_sb_pan"), (0.3, 1.0, 0.3, 63.7, 2.0, 0.7, "pi_sb_pan"),
             (0.3, 1.0, 0.7, 0.7, 2.0, 15.3, "pi_sb_pan"), (63.3, 1.0, 0.7, 63.7, 2.0, 15.3, "pi_sb_pan")]
    lz0, lz1, ly0, ly1 = SB_LOW
    hz0, hz1, hy0, hy1 = SB_HIGH
    # The rack: uprights at both ends and in the middle, under the upper step's two rails and the lower step's two
    # (under the cells, so they never meet a cell), cross members under each step and a tie between the steps.
    for x0 in (0.8, 31.5, 62.2):
        x1 = x0 + 1.0
        for z0 in (hz0 + 0.2, hz1 - 1.0):
            boxes.append((x0, 1.0, z0, x1, hy0 - 1.2, z0 + 0.8, B))
        for z0 in (lz0 + 0.2, lz1 - 1.0):
            boxes.append((x0, 1.0, z0, x1, ly0 - 1.2, z0 + 0.8, B))
        boxes.append((x0, hy0 - 1.2, hz0 + 1.0, x1, hy0, hz1 - 1.0, B))
        boxes.append((x0, ly0 - 1.2, lz0 + 1.0, x1, ly0, lz1 - 1.0, B))
        boxes.append((x0, 6.4, hz1 - 0.2, x1, 7.2, lz0 + 0.2, B))
    for z0 in (hz0 + 0.2, hz1 - 1.0):
        boxes.append((0.8, hy0 - 1.2, z0, 63.2, hy0, z0 + 0.8, "pi_sb_rail"))
    for z0 in (lz0 + 0.2, lz1 - 1.0):
        boxes.append((0.8, ly0 - 1.2, z0, 63.2, ly0, z0 + 0.8, "pi_sb_rail"))
    # The seismic restraint (round 25b): on each step a rail along the front and the back of the cells at two thirds of
    # their height, held by posts at the ends and in the middle, so an earthquake can't walk the cells off the rack.
    for (z0, z1, y0, y1) in (SB_LOW, SB_HIGH):
        ry = y0 + 6.4
        for rz0, rz1 in ((z1 + 0.1, z1 + 0.6), (z0 - 0.6, z0 - 0.1)):
            boxes.append((0.8, ry, rz0, 63.2, ry + 0.6, rz1, "pi_sb_rail"))
            for px in (0.9, 31.7, 62.5):
                boxes.append((px, y0 - 1.2 if px != 31.7 else y0, rz0 + 0.05, px + 0.6, ry, rz1 - 0.05, B))
    # The cells: each a clear jar with its plates, bus bars and acid inside (sb_cell).
    for step in (SB_LOW, SB_HIGH):
        for k in range(SB_CELLS):
            boxes += sb_cell(SB_X0 + k * SB_PITCH, step, k, k < SB_CELLS - 1)
    # The jumper from the lower step's first cell up to the upper step's first cell, round the left end.
    lt, ht = ly1 + SB_COVER, hy1 + SB_COVER
    lzc, hzc = (lz0 + lz1) / 2, (hz0 + hz1) / 2
    px = SB_X0 + 0.95
    cab = "pi_sb_cable"
    boxes += [(1.0, lt + 0.4, lzc - 0.4, px + 0.4, lt + 1.2, lzc + 0.4, cab),
              (1.0, lt + 1.2, lzc - 0.4, 1.8, ht + 0.6, lzc + 0.4, cab),
              (1.0, ht + 0.6, hzc - 0.4, 1.8, ht + 1.4, lzc + 0.4, cab),
              (1.8, ht + 0.6, hzc - 0.4, px + 0.4, ht + 1.4, hzc + 0.4, cab)]
    # The terminals at the right end: the upper step's last post (positive) and the lower step's (negative), each with a
    # copper lug, and the main cables along the back to where they rise.
    last = SB_X0 + (SB_CELLS - 1) * SB_PITCH + SB_W - 0.95
    rz0, rz1 = SB_RISE_Z
    red, black = "pi_sb_cable_red", cab
    rx0, rx1 = 48 + SB_RISE_RED[0], 48 + SB_RISE_RED[1]
    bx0, bx1 = 48 + SB_RISE_BLACK[0], 48 + SB_RISE_BLACK[1]
    # (Where a run turns into a riser it starts inside it, so no two faces of one cable lie in the same plane.)
    boxes += [(last - 0.5, ht + 0.3, hzc - 0.5, last + 0.5, ht + 1.0, hzc + 0.5, "pi_sb_copper"),
              (last - 0.4, ht + 1.0, hzc - 0.4, last + 0.4, ht + 2.6, hzc + 0.4, red),
              (last - 0.4, ht + 1.8, rz0 + 0.4, last + 0.4, ht + 2.6, hzc - 0.4, red),
              (rx0 + 0.4, ht + 1.8, rz0 + 0.4, last - 0.4, ht + 2.6, rz1 - 0.4, red),
              (rx0, ht + 1.6, rz0, rx1, 32, rz1, red)]
    boxes += [(last - 0.5, lt + 0.3, lzc - 0.5, last + 0.5, lt + 1.0, lzc + 0.5, "pi_sb_copper"),
              (last + 0.4, lt + 0.4, lzc - 0.4, 62.8, lt + 1.2, lzc + 0.4, black),
              (62.0, lt + 1.2, lzc - 0.4, 62.8, ht + 3.4, lzc + 0.4, black),
              (62.0, ht + 2.6, rz0 + 0.4, 62.8, ht + 3.4, lzc - 0.4, black),
              (bx0 + 0.4, ht + 2.6, rz0 + 0.4, 62.0, ht + 3.4, rz1 - 0.4, black),
              (bx0, ht + 2.4, rz0, bx1, 32, rz1, black)]
    # The battery's nameplate on the lower step's front rail, at the left: engraved, sized so its pixel letters stay
    # square (Justin, round 25b: the old one was too blurry to read).
    boxes.append((SB_TAG[0], SB_TAG[1], lz1 + 0.02, SB_TAG[2], SB_TAG[3], lz1 + 0.14, {"*": "pi_pnl_white", "south": ("pi_sb_tag", SB_TAG_UV)}))
    return boxes


def sb_cell(x0, step, k, linked):
    """One flooded lead-acid cell in its clear jar (round 25b; Justin: "I want the battery racks to look better"): the jar
    on its foot; through it the plates standing in the acid, dark brown positives and grey negatives, on the sediment at
    the bottom, the lead bus bars across their tops and the posts rising from them; the MAX and MIN lines on the jar; on
    top the black cover, the flame-arrester vent, the two posts with their colour collars and bolts, and the
    intercell connector bolted across to the next cell; a rubber spacer between it and the next."""
    z0, z1, y0, y1 = step
    x1 = x0 + SB_W
    zc = (z0 + z1) / 2
    acid = y1 - 1.6
    plates_top = y1 - 2.4
    top = y1 + SB_COVER
    out = [(x0 + 0.1, y0, z0 + 0.1, x1 - 0.1, y0 + 0.4, z1 - 0.1, "pi_sb_jar_foot"),
           (x0, y0 + 0.4, z0, x1, y1, z1, "pi_sb_jar_glass"),
           (x0 + 0.15, y0 + 0.7, z0 + 0.15, x1 - 0.15, acid, z1 - 0.15, "pi_sb_acid"),
           (x0 + 0.3, y0 + 0.55, z0 + 0.3, x1 - 0.3, y0 + 1.0, z1 - 0.3, "pi_sb_sediment")]
    for i in range(6):
        px = x0 + 0.4 + i * 0.5
        out.append((px, y0 + 1.0, z0 + 0.4, px + 0.28, plates_top, z1 - 0.4, "pi_sb_plate_pos" if i % 2 == 0 else "pi_sb_plate_neg"))
    # The bus bars across the plates' lugs (positive behind, negative in front), the posts down to them.
    out += [(x0 + 0.35, plates_top, zc - 0.5, x1 - 0.35, plates_top + 0.4, zc, "pi_sb_post"),
            (x0 + 0.35, plates_top, zc, x1 - 0.35, plates_top + 0.4, zc + 0.5, "pi_sb_lead")]
    posts = ((x0 + 0.6, k % 2 == 0), (x1 - 1.3, k % 2 == 1))
    for px, plus in posts:
        out.append((px, plates_top + 0.4, zc - 0.35, px + 0.7, y1 - 0.15, zc + 0.35, "pi_sb_post"))
    # The MAX and MIN lines moulded on the jar's front.
    for ly in (acid + 0.45, acid - 0.45):
        out.append((x0 + 0.3, ly - 0.06, z1, x0 + 1.4, ly + 0.06, z1 + 0.12, "pi_pnl_white"))
    # The cover, the vent (its collar and white ceramic flame arrester), the posts, their collars and bolts.
    xm = (x0 + x1) / 2
    out += [(x0 - 0.05, y1, z0 - 0.05, x1 + 0.05, top, z1 + 0.05, "pi_sb_cover"),
            (xm - 0.5, top, zc - 1.7, xm + 0.5, top + 0.25, zc - 0.7, "pi_sb_frame"),
            (xm - 0.35, top + 0.25, zc - 1.55, xm + 0.35, top + 1.0, zc - 0.85, "pi_sb_vent_top"),
            (xm - 0.42, top + 1.0, zc - 1.62, xm + 0.42, top + 1.15, zc - 0.78, "pi_sb_frame")]
    for px, plus in posts:
        out += [(px - 0.1, top, zc - 0.45, px + 0.8, top + 0.25, zc + 0.45, "pi_pnl_red" if plus else "pi_pnl_black"),
                (px, top + 0.25, zc - 0.35, px + 0.7, top + SB_POST, zc + 0.35, "pi_sb_post"),
                (px + 0.12, top + SB_POST, zc - 0.23, px + 0.58, top + SB_POST + 0.2, zc + 0.23, "pi_steel")]
    if linked:
        # The intercell connector to the next cell's left post, under the posts' tops; a rubber spacer between the jars.
        out += [(x1 - 0.95, top + 0.45, zc - 0.5, x1 + SB_PITCH - SB_W + 0.95, top + 0.75, zc + 0.5, "pi_link"),
                (x1, y0 + 3.5, zc - 1.2, x1 + SB_PITCH - SB_W, y0 + 6.5, zc + 1.2, "pi_sb_cover")]
    return out


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
        # Where a see-through box goes on into the next block, its face on the cut is inside it: left out (round 25b: a
        # clear jar cut in two showed a pane of glass across its middle). Solid boxes keep theirs (the art check counts
        # a solid box without them as a hole).
        cut = [f for f, inside in (("west", b[0] < x0 - 1e-6), ("east", b[3] > x1 + 1e-6), ("down", b[1] < y0 - 1e-6), ("up", b[4] > y1 + 1e-6))
               if inside] if isinstance(t, str) and t in SEE_THROUGH else []
        if cut:
            t = dict(t) if isinstance(t, dict) else {"*": t}
            for f in cut:
                t[f] = None
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


def _sb_tag():
    """The battery's engraved nameplate in the pixel font (tools/control_boards.py), one texel a font pixel, in the
    texture's top band (SB_TAG_UV): its name, and the warning in red."""
    import control_boards as CB
    img = Image.new("RGBA", (128, 128), (236, 236, 230, 255))
    d = ImageDraw.Draw(img)
    rows = SB_TAG_ROWS
    d.rectangle([0, 0, 127, rows - 1], fill=(238, 238, 232, 255), outline=(60, 60, 60, 255))
    for line, y, ink in (("125 VDC STATION BATTERY", 2, (24, 24, 26, 255)), ("60 CELLS - NO SMOKING", 8, (190, 28, 24, 255))):
        CB.text(d, (128 - CB.text_width(line)) // 2, y, line, ink)
    return img


def _see_through(colour, alpha, seed):
    """A clear material (a cell's jar, its acid): a faint tint, mostly see-through (kept translucent: plant_office2)."""
    img = _solid(colour, seed, 0.02).convert("RGBA")
    img.putalpha(alpha)
    return img


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
        # The station battery.
        "pi_sb_frame": lambda: _solid((34, 36, 38), 420, 0.08), "pi_sb_rail": lambda: _solid((44, 46, 50), 421, 0.1),
        "pi_sb_pan": lambda: _solid((52, 54, 56), 422, 0.06), "pi_sb_pan_top": lambda: _solid((40, 42, 44), 423, 0.15),
        "pi_sb_jar_glass": lambda: _see_through((218, 230, 232), 70, 431), "pi_sb_acid": lambda: _see_through((176, 206, 222), 46, 432),
        "pi_sb_jar_foot": lambda: _solid((168, 178, 180), 433, 0.05), "pi_sb_sediment": lambda: _solid((72, 64, 58), 434, 0.1),
        "pi_sb_plate_pos": lambda: _solid((96, 66, 50), 435, 0.08), "pi_sb_plate_neg": lambda: _solid((124, 128, 132), 436, 0.08),
        "pi_sb_lead": lambda: _solid((112, 114, 116), 437, 0.05),
        "pi_sb_cover": lambda: _solid((26, 26, 28), 424, 0.04), "pi_sb_post": lambda: _solid((150, 150, 146), 425, 0.05),
        "pi_sb_vent_top": lambda: _solid((200, 200, 196), 427, 0.03),
        "pi_sb_cable": lambda: _solid((24, 24, 26), 428, 0.02), "pi_sb_cable_red": lambda: _solid((168, 30, 26), 429, 0.02),
        "pi_sb_copper": lambda: _solid((196, 120, 70), 430, 0.04), "pi_sb_tag": _sb_tag,
    })
    return out


# Kept at full size so the writing reads.
SHARP = {f"pi_{key}_f_{p}" for key in DRAWINGS for p in range(2 * PANELS[key]["width"])} | {"pi_battery_tag", "pi_battery_front",
                                                                                           "pi_sb_tag"}


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
    if key == "station_battery":
        # The two steps of cells from the front, on the black rack.
        d = ImageDraw.Draw(img)
        d.rectangle([4, 100, 124, 112], fill=(40, 42, 44, 255))
        for step, (y0, y1, x0) in enumerate(((60, 98, 6), (18, 56, 12))):
            for k in range(7):
                x = x0 + k * 16
                d.rectangle([x, y0, x + 13, y1], fill=(196, 208, 212, 255), outline=(40, 40, 44, 255), width=2)
                d.rectangle([x + 2, y0 + 10, x + 11, y1 - 3], fill=(98, 76, 60, 255))
                d.rectangle([x, y0 - 4, x + 13, y0], fill=(26, 26, 28, 255))
                d.rectangle([x + 2, y0 - 7, x + 4, y0 - 4], fill=(196, 38, 32, 255))
                d.rectangle([x + 9, y0 - 7, x + 11, y0 - 4], fill=(150, 150, 146, 255))
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
