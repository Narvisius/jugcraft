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

# Image px a model pixel: the drawings are 512 px a block (round 26, 8e; Justin: "Make sure all the text is clearly
# legible ... that applies to everything", the 256 px drawings' smallest letters were a blur). Sizes and line widths
# in the drawings are given as they were at 16 px a model pixel; K scales them (fonts, widths) to U.
U = 32
K = U // 16
PAINT = (196, 198, 192)
INK = (24, 24, 26)
FONT_DIR = "/usr/share/fonts/truetype/dejavu/"


def font(size, bold=False):
    return ImageFont.truetype(FONT_DIR + ("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"), round(size * K))


def cfont(size, bold=False):
    return ImageFont.truetype(FONT_DIR + ("DejaVuSansCondensed-Bold.ttf" if bold else "DejaVuSansCondensed.ttf"), round(size * K))


def rfont(size, bold=False):
    """The condensed font at a size in the image's own pixels (for textures drawn outside the 16 px a pixel kit)."""
    return ImageFont.truetype(FONT_DIR + ("DejaVuSansCondensed-Bold.ttf" if bold else "DejaVuSansCondensed.ttf"), round(size))


class _Draw(ImageDraw.ImageDraw):
    """ImageDraw with line widths given at 16 px a model pixel (scaled by K)."""

    def _w(self, kw):
        if "width" in kw:
            kw["width"] = max(1, round(kw["width"] * K))
        return kw

    def rectangle(self, xy, **kw):
        return super().rectangle(xy, **self._w(kw))

    def ellipse(self, xy, **kw):
        return super().ellipse(xy, **self._w(kw))

    def line(self, xy, *a, **kw):
        if a:
            kw["fill"] = a[0]
            if len(a) > 1:
                kw["width"] = a[1]
        return super().line(xy, **self._w(kw))

    def polygon(self, xy, **kw):
        return super().polygon(xy, **self._w(kw))


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
    "dc_switchboard": {"display": "125 VDC Distribution Switchboard", "width": 2, "labels": ["1A", "1B"]},
    "vital_inverter": {"display": "Vital AC Inverter", "width": 1, "item3d": True, "labels": ["1A-1", "1A-2", "1B-1", "1B-2"]},
    # The DC equipment rooms' own battery chargers (round 26, 8b): station_charger below.
    "station_charger": {"display": "Station Battery Charger", "width": 1, "item3d": True, "labels": ["1A-1", "1A-2", "1B-1", "1B-2"]},
    # Each inverter's standby source (round 26, 8d): reg_transformer below.
    "reg_transformer": {"display": "Regulating Transformer", "width": 1, "item3d": True, "labels": ["1A-1", "1A-2", "1B-1", "1B-2"]},
    # Each train's DC ground detector and battery monitor (round 26, 8f): dc_ground_detector below.
    "dc_ground_detector": {"display": "DC Ground Detector & Battery Monitor", "width": 1, "item3d": True, "labels": ["1A", "1B"]},
    # Each inverter's 120 VAC vital instrument panel (round 26, 8e): vital_panel below. Hung on a wall, its door opens.
    "vital_panel": {"display": "120 VAC Vital Instrument Panel", "width": 1, "item3d": True, "labels": ["1A-1", "1A-2", "1B-1", "1B-2"],
                    "door": True},
    # The battery rooms' hydrogen monitor and exhaust fan (round 26, #9): wall pieces two blocks high, below.
    "h2_monitor": {"display": "Hydrogen Monitor", "width": 1, "item3d": True, "labels": ["1A", "1B"]},
    "exhaust_fan": {"display": "Battery Room Exhaust Fan", "width": 1, "item3d": True, "labels": ["1A", "1B"]},
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
# The battery monitor's sense cable (round 26, #9; Justin: "Cell sense wiring"): from each rack's sense junction box up
# the riser beside the power cables and along the tray, inside its front lip, on to the ground detector's battery
# monitor (SB_SENSE_X: where it rises, x in the riser's block).
SENSE = "pi_sense_cable"
SB_SENSE_X, SB_SENSE_Z, SENSE_Z, SENSE_Y = (12.2, 12.8), (0.35, 0.85), (11.05, 11.45), (12.5, 12.9)
_SENSE_RUN = [(0, SENSE_Y[0], SENSE_Z[0], 16, SENSE_Y[1], SENSE_Z[1], SENSE)]
_SENSE_RISE = [(SB_SENSE_X[0], 0, SB_SENSE_Z[0], SB_SENSE_X[1], 16, SB_SENSE_Z[1], SENSE)]
# The sense cable rising out of the wall's riser and over the tray's back wall into its lane.
_SENSE_TURN = [(SB_SENSE_X[0], 0, SB_SENSE_Z[0], SB_SENSE_X[1], 14.65, SB_SENSE_Z[1], SENSE),
               (SB_SENSE_X[0], 14.05, SB_SENSE_Z[1], SB_SENSE_X[1], 14.65, SENSE_Z[1], SENSE),
               (SB_SENSE_X[0], SENSE_Y[0], SENSE_Z[0], SB_SENSE_X[1], 14.05, SENSE_Z[1], SENSE)]


def _dc_tray_bend():
    """The DC tray turning a corner (round 26, #9: the second string's run from its wall to the first's), facing north:
    from the west edge (along the north wall) round to the south edge (along the east wall), the back wall outside the
    turn, the front lip inside it, the cables and the sense cable turning with it."""
    return [(0, 12, 4, 12, 12.5, 12, "pi_chrome"), (4, 12, 12, 12, 12.5, 16, "pi_chrome"),
            (0, 12.5, 4, 12, 14, 4.5, "pi_chrome"), (11.5, 12.5, 4, 12, 14, 16, "pi_chrome"),
            (0, 12.5, 11.5, 4.5, 14, 12, "pi_chrome"), (4, 12.5, 12, 4.5, 14, 16, "pi_chrome"),
            (0, 12.5, 5, 11, 13.5, 7, "pi_sb_cable_red"), (9, 12.5, 7, 11, 13.5, 16, "pi_sb_cable_red"),
            (0, 12.5, 7.5, 8.5, 13.5, 11, "pi_sb_cable"), (5, 12.5, 11, 8.5, 13.5, 16, "pi_sb_cable"),
            (0, SENSE_Y[0], SENSE_Z[0], 16 - SENSE_Z[0], SENSE_Y[1], SENSE_Z[1], SENSE),
            (16 - SENSE_Z[1], SENSE_Y[0], SENSE_Z[1], 16 - SENSE_Z[0], SENSE_Y[1], 16, SENSE),
            (7.5, 14, 7.5, 8.5, 16, 8.5, "pi_chrome")]


def _dc_tray_tee():
    """The DC tray with a branch joining it from the south (round 26, #9: where the second string's run joins the
    first's on its way to the switchboard), facing north: the run along x, the branch's cables lying into the bundle."""
    return [(0, 12, 4, 16, 12.5, 12, "pi_chrome"), (4, 12, 12, 12, 12.5, 16, "pi_chrome"),
            (0, 12.5, 4, 16, 14, 4.5, "pi_chrome"),
            (0, 12.5, 11.5, 4.5, 14, 12, "pi_chrome"), (11.5, 12.5, 11.5, 16, 14, 12, "pi_chrome"),
            (4, 12.5, 12, 4.5, 14, 16, "pi_chrome"), (11.5, 12.5, 12, 12, 14, 16, "pi_chrome"),
            (0, 12.5, 5, 16, 13.5, 7, "pi_sb_cable_red"), (0, 12.5, 7.5, 16, 13.5, 11, "pi_sb_cable"),
            # The branch's: the black into the bundle's black, the red up over it onto the red.
            (5, 12.55, 10.5, 8.5, 13.45, 16, "pi_sb_cable"),
            (9, 12.55, 11.6, 11, 13.45, 16, "pi_sb_cable_red"), (9, 13.45, 6.0, 11, 14.25, 12.4, "pi_sb_cable_red")] + _SENSE_RUN + [
            (16 - SENSE_Z[1], SENSE_Y[0], SENSE_Z[1], 16 - SENSE_Z[0], SENSE_Y[1], 16, SENSE),
            (7.5, 14, 7.5, 8.5, 16, 8.5, "pi_chrome")]


RISERS = {
    "dc_cable_tray": {"display": "DC Cable Tray", "boxes": [(0, 12.5, 4, 16, 14, 4.5, "pi_chrome")] + _TRAY + _SENSE_RUN},
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
        (0, 12.5, 4, 5.6, 14, 4.5, "pi_chrome"), (10.8, 12.5, 4, 16, 14, 4.5, "pi_chrome")] + _TRAY + _SENSE_RUN},
}
# The battery rooms' (round 26, #9): the riser up from a rack's end and its top with the battery monitor's sense cable
# beside the power cables; the tray's bend and tee where a second string's run joins.
RISERS.update({
    "dc_cable_riser_sense": {"display": "DC Cable Riser (With Sense Cable)", "boxes": RISERS["dc_cable_riser"]["boxes"] + _SENSE_RISE},
    "dc_cable_riser_top_sense": {"display": "DC Cable Riser (Top, With Sense Cable)", "boxes":
        [b for b in RISERS["dc_cable_riser_top"]["boxes"] if b != (10.8, 12.5, 4, 16, 14, 4.5, "pi_chrome")]
        + [(10.8, 12.5, 4, SB_SENSE_X[0] - 0.05, 14, 4.5, "pi_chrome"), (SB_SENSE_X[1] + 0.05, 12.5, 4, 16, 14, 4.5, "pi_chrome"),
           (SB_SENSE_X[0] - 0.05, 12.5, 4, SB_SENSE_X[1] + 0.05, 13.95, 4.5, "pi_chrome")] + _SENSE_TURN},
    "dc_cable_tray_bend": {"display": "DC Cable Tray (Bend)", "boxes": _dc_tray_bend()},
    "dc_cable_tray_tee": {"display": "DC Cable Tray (Tee)", "boxes": _dc_tray_tee()},
})

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
    "station_charger": (["PMP", "CRC", "PLP"], {"P": "#c:plates/steel", "M": "minecraft:clock", "C": "jugcraft:basic_circuit",
                                                "R": "minecraft:redstone", "L": "minecraft:lever"}, 1),
    "dc_ground_detector": (["PGP", "CXC", "PRP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit",
                                                   "X": "minecraft:compass", "R": "minecraft:redstone"}, 1),
    "vital_panel": (["PGP", "CRC", "PPP"], {"P": "#c:plates/steel", "G": "minecraft:paper", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, 1),
    "reg_transformer": (["PCP", "CIC", "PCP"], {"P": "#c:plates/steel", "C": "minecraft:copper_ingot", "I": "minecraft:iron_block"}, 1),
    "dc_cable_tray": (["P P", "CPC"], {"P": "#c:plates/iron", "C": "minecraft:copper_ingot"}, 4),
    "dc_cable_riser": (["PC", "P ", "PC"], {"P": "#c:plates/iron", "C": "minecraft:copper_ingot"}, 4),
    "dc_cable_riser_top": (["PCP", "PC "], {"P": "#c:plates/iron", "C": "minecraft:copper_ingot"}, 2),
    "dc_cable_riser_sense": (["PC", "PS", "PC"], {"P": "#c:plates/iron", "C": "minecraft:copper_ingot", "S": "minecraft:string"}, 4),
    "dc_cable_riser_top_sense": (["PCP", "PCS"], {"P": "#c:plates/iron", "C": "minecraft:copper_ingot", "S": "minecraft:string"}, 2),
    "dc_cable_tray_bend": (["PP ", "CPP", " CP"], {"P": "#c:plates/iron", "C": "minecraft:copper_ingot"}, 2),
    "dc_cable_tray_tee": (["PPP", "CPC", " P "], {"P": "#c:plates/iron", "C": "minecraft:copper_ingot"}, 2),
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
        small = rng.normal(0, 1, (h // (2 * U) + 1, w // (2 * U) + 1))
        noise = np.kron(small, np.ones((2 * U, 2 * U)))[:h, :w]
        base += noise[..., None] * 2.2 + rng.normal(0, 1.2, (h, w))[..., None]
        img = np.clip(base, 0, 255).astype(np.uint8)
        self.img = Image.fromarray(np.dstack([img, np.full((h, w), 255, np.uint8)]), "RGBA")
        self.d = _Draw(self.img)
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
        tw = (self.d.textlength(text, font=f) + 10 * K) / U if w is None else w
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


def dc_switchboard(label="1A"):
    """The 125 VDC distribution switchboard a station battery and its chargers feed (two sections): the bus meters, the
    ground detector (a lamp on each side of the bus to ground, both lit while neither side is grounded), the main
    breaker, and the feeder breakers to everything that runs on the battery, each labelled."""
    f = Front(2, 407)
    f.door(0.4, 16.0, handle=False)
    f.door(16.0, 31.6)
    f.nameplate(16, 29.0, 21, ["125 VDC DISTRIBUTION SWITCHBOARD " + label, f"DC BUS {label}    1200 A    FROM BATTERY {label}"])
    f.meter(2.0, 21.4, 7.6, 25.6, "BUS VOLTS", (0, 150), 132, "V", live="dc_v")
    f.meter(8.6, 21.4, 14.2, 25.6, "BATT AMPS", (0, 200), 12, "A", live="dc_a")
    # The ground detector.
    f.R(2.0, 15.6, 14.2, 19.6, fill=(52, 54, 58, 255))
    f.T(8.1, 18.8, "GROUND DETECTOR", 7, fill=(236, 236, 230, 255), bold=True)
    f.lamp(5.0, 17.0, (240, 240, 230), True, None, r=0.5, live="gnd_lamp")
    f.lamp(11.2, 17.0, (240, 240, 230), True, None, r=0.5, live="gnd_lamp")
    f.T(5.0, 15.95, "+ BUS", 6, fill=(236, 236, 230, 255), bold=True)
    f.T(11.2, 15.95, "- BUS", 6, fill=(236, 236, 230, 255), bold=True)
    f.proud(2.0, 15.6, 14.2, 19.6, 0.25)
    # The main breaker: an insulated-case breaker in its cubicle window, the charging handle and the OPEN/CLOSED flag.
    f.R(3.0, 5.0, 13.2, 13.8, fill=(36, 38, 42, 255))
    f.R(4.0, 6.0, 12.2, 12.8, fill=(22, 22, 24, 255))
    f.R(6.4, 10.6, 9.8, 11.8, fill=(200, 40, 34, 255))
    f.T(8.1, 11.2, "CLOSED", 7, fill=(255, 255, 255, 255), bold=True)
    f.T(8.1, 9.4, f"MAIN 72-{label}", 8, fill=(236, 236, 230, 255), bold=True)
    f.T(8.1, 8.4, "1200 A  250 VDC", 7, fill=(200, 200, 196, 255), bold=True)
    f.proud(4.0, 6.0, 12.2, 12.8, 0.4)
    f.proud(7.3, 6.6, 8.9, 7.8, 0.9)
    f.warning(8.1, 2.9, "125 VDC")
    # The feeders: two columns of moulded-case breakers, each with its handle up (on) and its label.
    train = label[-1]
    feeders = [f"INVERTER {label}-1", f"INVERTER {label}-2", f"4160V SWGR {label} CTL", f"480V LC {label} CTL", f"EDG {label} CONTROL",
               f"MCR PANELS TRAIN {train}", "RELAY RM CAB 1-6" if train == "A" else "RELAY RM CAB 7-12", "TURB AUX OIL PUMP" if train == "A"
               else "MD AFW PUMP CTL", "EMERG LIGHTING", "AFW TURB CONTROL" if train == "A" else "DIESEL FUEL XFER", f"COMPUTER UPS {label}", "SPARE"]
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


DRAWINGS = {"edg_engine_panel": engine_panel, "edg_generator_panel": generator_panel, "edg_voltage_regulator": voltage_regulator,
            "edg_protection_panel": protection_panel, "edg_load_sequencer": load_sequencer, "edg_battery_charger": battery_charger,
            "dc_switchboard": dc_switchboard, "vital_inverter": None, "station_charger": None, "reg_transformer": None, "vital_panel": None,
            "dc_ground_detector": None, "h2_monitor": None, "exhaust_fan": None}
_FRONTS = {}


def layout():
    """What moves on each panel (assets/jugcraft/live_panels.json, read by client/LivePanelRenderer.java): positions in
    model pixels on the piece facing north (x across to the right, y up from the floor, z out to the front)."""
    out = {}
    for key in DRAWINGS:
        out[key] = {"width": PANELS[key]["width"], "elements": front(key).live}
        # A panel whose labels are different drawings (round 26, #12: the TSC's video wall displays and status boards)
        # moves differently by variant: each variant's elements.
        if variants(key) > 1 and not PANELS[key].get("shared"):
            each = [front(key, v).live for v in range(variants(key))]
            if any(e != each[0] for e in each):
                out[key]["variants"] = each
    return out


def front(key, v=0):
    """A panel's drawing; for a panel labelled by train (round 26, 8c: PANELS "labels"), variant v's (one drawing for
    all of a "shared" panel's: round 26, #10, the relay panels, their names lettered live)."""
    if PANELS[key].get("shared"):
        v = 0
    if (key, v) not in _FRONTS:
        labels = PANELS[key].get("labels")
        _FRONTS[(key, v)] = DRAWINGS[key](labels[v]) if labels else DRAWINGS[key]()
    return _FRONTS[(key, v)]


def texture_key(key, v):
    """The textures' stem for variant v of a panel (variant 0 keeps the plain name; a "shared" panel's variants all
    have it)."""
    return key if v == 0 or PANELS[key].get("shared") else f"{key}_v{v}"


def variants(key):
    return len(PANELS[key].get("labels") or [None])


# ------------------------------------------------------------------------------------------------ the models

# Pieces whose boxes come from elsewhere (round 26, #10: tools/relay_room.py's cabinets), by key; and more textures.
CABINETS = {}
EXTRA_MAKERS = []


def cabinet(key):
    """The whole piece: plinth, body (its front the drawing), top, and what stands proud of the door."""
    if key in CABINETS:
        return CABINETS[key]()
    if key == "edg_battery_rack":
        return battery_rack()
    if key == "station_battery":
        return station_battery()
    if key == "station_charger":
        return station_charger_boxes()
    if key == "vital_inverter":
        return vital_inverter_boxes()
    if key == "reg_transformer":
        return reg_transformer_boxes()
    if key == "vital_panel":
        return vital_panel_boxes(False)
    if key == "dc_ground_detector":
        return ground_detector_boxes()
    if key == "h2_monitor":
        return h2_monitor_boxes()
    if key == "exhaust_fan":
        return exhaust_fan_boxes()
    W = PANELS[key]["width"] * 16
    F = f"pi_{key}_f"
    boxes = [(0.8, 0, 2.6, W - 0.8, 1.2, 14.6, "pi_toe_kick"),
             (0.4, 1.2, 2, W - 0.4, 30.6, 15, {"*": "pi_cabinet", "south": F}),
             (0.2, 30.6, 1.8, W - 0.2, 31.4, 15.3, "pi_cabinet_dark")]
    for x0, y0, x1, y1, depth, side in front(key).boxes:
        boxes.append((x0, y0, 15, x1, y1, 15 + depth, {"*": side, "south": F}))
    if key == "dc_switchboard":
        # Its chargers' and inverters' cables come and go through a conduit hub on its second section's top (round 26,
        # 8f: up to the tray; the battery's cables come down into the first through their riser).
        boxes += _top_hub(24.0, 31.4)
    return boxes


def _top_hub(x, top, z=8.0):
    """A conduit's hub and locknut on a cabinet's top at (x, z), the conduit's square stub up to the block's top (the
    conduit above it is the plant's)."""
    return [(x - 1.4, top, z - 1.4, x + 1.4, top + 0.3, z + 1.4, "pi_steel_dark"),
            (x - 1.4, top, z - 1.4, x + 1.4, top + 0.25, z + 1.4, "pi_steel_dark", {"rotation": {"angle": 45, "axis": "y", "origin": [x, top, z]}}),
            (x - 1.0, top + 0.2, z - 1.0, x + 1.0, 32.0, z + 1.0, "pi_chrome")]


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
    # The battery monitor's cell sensing (round 26, #9; Justin: "Cell sense wiring"): a sensor clipped to each cell's
    # positive post (sb_cell), daisy-chained along the front of the covers on each step; at the right end both chains
    # go back to the sense junction box on the wall behind the upper step, its cable up out of the top into the riser
    # beside the power cables (SB_SENSE_*), on through the tray to the ground detector's monitor.
    for z0, z1, y0, y1 in (SB_LOW, SB_HIGH):
        zc, top = (z0 + z1) / 2, y1 + SB_COVER
        boxes.append((SB_X0 - 0.2, top + 0.02, zc + 1.75, 62.12, top + 0.22, zc + 1.95, SENSE))
    lzs, hzs = (lz0 + lz1) / 2 + 1.75, (hz0 + hz1) / 2 + 1.75
    jb = (59.6, 24.0, SB_SENSE_Z[0], 63.2, 29.0, 2.2)
    boxes += [(61.92, lt + 0.02, lzs, 62.12, 25.5, lzs + 0.2, SENSE), (61.92, 25.3, jb[5], 62.12, 25.5, lzs, SENSE),
              (61.92, ht + 0.02, jb[5], 62.12, ht + 0.22, hzs, SENSE),
              (jb[0], jb[1], jb[2], jb[3], jb[4], jb[5], {"*": "pi_cabinet_dark", "south": "pi_sense_jb!"}),
              (48 + SB_SENSE_X[0], jb[4], SB_SENSE_Z[0], 48 + SB_SENSE_X[1], 32, SB_SENSE_Z[1], SENSE)]
    # The battery's nameplate on the lower step's front rail, at the left: engraved, sized so its pixel letters stay
    # square (Justin, round 25b: the old one was too blurry to read).
    boxes.append((SB_TAG[0], SB_TAG[1], lz1 + 0.02, SB_TAG[2], SB_TAG[3], lz1 + 0.14, {"*": "pi_pnl_white", "south": ("pi_sb_tag", SB_TAG_UV)}))
    return boxes


def _sense_jb():
    """The cell sense junction box's front: grey, its cover screws, an engraved tag."""
    img = Image.new("RGBA", (256, 256), (150, 154, 156, 255))
    d = _Draw(img)
    d.rectangle([6, 6, 249, 249], outline=(110, 112, 114, 255), width=4)
    for x, y in ((22, 22), (233, 22), (22, 233), (233, 233)):
        d.ellipse([x - 8, y - 8, x + 8, y + 8], fill=(190, 192, 194, 255), outline=(90, 92, 94, 255), width=2)
    d.rectangle([40, 100, 215, 156], fill=(236, 236, 230, 255), outline=(60, 60, 62, 255), width=2)
    d.text((128, 128), "CELL SENSE", font=rfont(30, True), fill=(24, 24, 26, 255), anchor="mm")
    return img


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
    # The cell's sensor (round 26, #9): clipped to its positive post, its lead to the post, its green OK light.
    pp = next(px for px, plus in posts if plus)
    out += [(pp - 0.1, top + 0.02, zc + 0.95, pp + 0.8, top + 0.55, zc + 1.7, "pi_sense_module"),
            (pp + 0.5, top + 0.3, zc + 1.7, pp + 0.7, top + 0.45, zc + 1.78, "pi_pnl_green"),
            (pp + 0.25, top + 0.25, zc + 0.3, pp + 0.45, top + 0.4, zc + 0.95, SENSE)]
    if linked:
        # The intercell connector to the next cell's left post, under the posts' tops; a rubber spacer between the jars.
        out += [(x1 - 0.95, top + 0.45, zc - 0.5, x1 + SB_PITCH - SB_W + 0.95, top + 0.75, zc + 0.5, "pi_link"),
                (x1, y0 + 3.5, zc - 1.2, x1 + SB_PITCH - SB_W, y0 + 6.5, zc + 1.2, "pi_sb_cover")]
    return out


def _cut_uv(b, x0, x1, y0, y1):
    """A box's faces where it is cut at a block's edge: a picture stretched over its whole front or back ("name!": a
    monitor's screen, a sign) keeps to its share of the picture on each piece (round 26, #12), instead of the whole
    picture squeezed onto each."""
    t = b[6]
    whole = x0 <= b[0] + 1e-6 and x1 >= b[3] - 1e-6 and y0 <= b[1] + 1e-6 and y1 >= b[4] - 1e-6

    def fix(face, name):
        if whole or not isinstance(name, str) or not name.endswith("!"):
            return name
        u0, u1 = (x0 - b[0]) / (b[3] - b[0]) * 16, (x1 - b[0]) / (b[3] - b[0]) * 16
        if face == "north":
            u0, u1 = 16 - u1, 16 - u0
        v0, v1 = (b[4] - y1) / (b[4] - b[1]) * 16, (b[4] - y0) / (b[4] - b[1]) * 16
        return (name[:-1], [round(u0, 4), round(v0, 4), round(u1, 4), round(v1, 4)])
    if whole:
        return t
    spec = dict(t) if isinstance(t, dict) else {"*": t}
    for face in ("south", "north"):
        name = spec.get(face, spec.get("*"))
        fixed = fix(face, name)
        if fixed is not name:
            spec[face] = fixed
    return spec if isinstance(t, dict) or len(spec) > 1 else t


def part(key, c, r, open_=False):
    """Block (c, r)'s share of the piece (r 0 at the floor), its front texture the drawing's slice for that block.
    open_: a panel with a door (PANELS "door"), its door open (round 26, 8e: the vital panel)."""
    width = PANELS[key]["width"]
    top = PANELS[key].get("rows", 2) - 1
    F = f"pi_{key}_f"
    drawn = {F, f"pi_{key}_i", f"pi_{key}_o"}
    out = []
    for b in (vital_panel_boxes(open_) if key == VP_KEY else cabinet(key)):
        x0, x1 = max(b[0], c * 16), min(b[3], c * 16 + 16)
        # The top row keeps what stands up out of it (round 26, #12: a one-row console's monitors).
        y0, y1 = max(b[1], r * 16), (min(b[4], r * 16 + 16) if r < top else b[4])
        if x1 - x0 < 0.01 or y1 - y0 < 0.01:
            continue
        t = _cut_uv(b, x0, x1, y0, y1)
        if isinstance(t, dict):
            t = {k: (f"{v}_{r * width + c}" if isinstance(v, str) and v in drawn else v) for k, v in t.items()}
        # Where a see-through box goes on into the next block, its face on the cut is inside it: left out (round 25b: a
        # clear jar cut in two showed a pane of glass across its middle). Solid boxes keep theirs (the art check counts
        # a solid box without them as a hole).
        cut = [f for f, inside in (("west", b[0] < x0 - 1e-6), ("east", b[3] > x1 + 1e-6), ("down", b[1] < y0 - 1e-6), ("up", b[4] > y1 + 1e-6))
               if inside] if isinstance(t, str) and t in SEE_THROUGH else []
        if cut:
            t = dict(t) if isinstance(t, dict) else {"*": t}
            for f in cut:
                t[f] = None
        extra = tuple(b[7:])
        if extra and isinstance(extra[0], dict) and "rotation" in extra[0] and (c or r):
            # A turned box's pivot moves with it into the block's own frame.
            rot = dict(extra[0]["rotation"])
            ox, oy, oz = rot["origin"]
            rot["origin"] = [ox - c * 16, oy - r * 16, oz]
            extra = ({**extra[0], "rotation": rot},) + extra[1:]
        out.append((x0 - c * 16, y0 - r * 16, b[2], x1 - c * 16, y1 - r * 16, b[5], t) + extra)
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
    """A diesel room battery cell's front: its tag at twice the cell's resolution, so it reads (round 26, 8e)."""
    img = _cell().resize((256, 256), Image.NEAREST)
    d = ImageDraw.Draw(img)
    d.rectangle([72, 124, 184, 180], fill=(240, 240, 234, 255), outline=(30, 30, 30, 255), width=4)
    d.text((128, 140), "2.20 V", font=rfont(28, True), fill=INK, anchor="mm")
    d.text((128, 166), "CELL", font=rfont(20, True), fill=INK, anchor="mm")
    return img


def _cell_top():
    img = Image.new("RGBA", (128, 128), (32, 32, 34, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 123, 123], fill=(46, 46, 50, 255))
    d.ellipse([52, 52, 76, 76], fill=(200, 200, 196, 255))   # the vent cap
    d.ellipse([58, 58, 70, 70], fill=(120, 120, 118, 255))
    return img


def _battery_tag():
    """The diesel room battery's nameplate: drawn at its plate's shape (4 to 1), at 512 px across (round 26, 8e: so it
    reads), stretched to a square texture."""
    img = Image.new("RGBA", (512, 128), (240, 240, 234, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 511, 127], outline=(30, 30, 30, 255), width=6)
    d.text((256, 44), "125 VDC BATTERY 1A", font=rfont(48, True), fill=INK, anchor="mm")
    d.text((256, 92), "NO SMOKING  NO SPARKS", font=rfont(32, True), fill=(200, 30, 30, 255), anchor="mm")
    return img.resize((512, 512), Image.BICUBIC)


def _sb_tag():
    """The battery's engraved nameplate in the texture's top band (SB_TAG_UV): its name, and the warning in red; drawn
    at 512 px across (round 26, 8e; Justin: all the text clearly legible), the band the same share of it."""
    size = 512
    img = Image.new("RGBA", (size, size), (236, 236, 230, 255))
    d = ImageDraw.Draw(img)
    rows = SB_TAG_ROWS * size // 128
    d.rectangle([0, 0, size - 1, rows - 1], fill=(238, 238, 232, 255), outline=(60, 60, 60, 255), width=3)
    for line, y, ink in (("125 VDC STATION BATTERY", rows * 0.3, (24, 24, 26, 255)), ("60 CELLS - NO SMOKING", rows * 0.72, (190, 28, 24, 255))):
        d.text((size / 2, y), line, font=rfont(22, True), fill=ink, anchor="mm")
    return img


def _see_through(colour, alpha, seed):
    """A clear material (a cell's jar, its acid): a faint tint, mostly see-through (kept translucent: plant_office2)."""
    img = _solid(colour, seed, 0.02).convert("RGBA")
    img.putalpha(alpha)
    return img


def makers():
    out = {}
    for key in DRAWINGS:
        for v in range(1 if PANELS[key].get("shared") else variants(key)):
            for name, img in front(key, v).cut(texture_key(key, v)).items():
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
        # The battery monitor's cell sensing (round 26, #9).
        SENSE: lambda: _solid((70, 96, 140), 438, 0.03), "pi_sense_module": lambda: _solid((44, 46, 50), 439, 0.05),
        "pi_sense_jb": _sense_jb,
        # The battery rooms' fittings (round 26, #9).
        "pi_eyewash_flag": _eyewash_flag, "pi_spill_front": _spill_front, "pi_h2_tag": _h2_tag,
        "pi_spill_yellow": lambda: _solid((236, 190, 30), 440, 0.06),
    })
    out.update(relay_cabinet_textures())
    out.update(vital_panel_textures())
    out.update({"pi_lens_green": lambda: _solid((26, 84, 40), 530, 0.02), "pi_lens_white": lambda: _solid((150, 150, 144), 531, 0.02),
                "pi_lens_amber": lambda: _solid((130, 84, 20), 532, 0.02), SC_GLASS: lambda: _see_through((220, 232, 236), 40, 533),
                "pi_lens_red": lambda: _solid((110, 22, 18), 534, 0.02)})
    out.update({f"pi_{k}": (lambda k=k: _sign_image(k)) for k in SIGNS})
    for extra in EXTRA_MAKERS:
        out.update(extra())
    return out


# Kept at full size so the writing reads.
SHARP = {f"pi_{texture_key(key, v)}_f_{p}" for key in DRAWINGS for v in range(variants(key)) for p in range(2 * PANELS[key]["width"])} | {"pi_battery_tag", "pi_battery_front", "pi_sense_jb", "pi_eyewash_flag",
                                                                                           "pi_spill_front", "pi_h2_tag", "pi_sign_no_smoking", "pi_sign_battery_acid",
                                                                                           "pi_sign_hydrogen", "pi_sign_eyewash",
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


# ------------------------------------------------------------------------------------------------ the relay cabinet

# The relay cabinet (round 26, 7e; plant_interior.FURNITURE "relay_cabinet", a stacking piece two blocks high in the
# DC rooms and the relay room), in real 3D instead of a picture on a box (Justin: "those look too simple"). A steel
# cabinet with a glass-front door; through the glass:
# - above (the upper block): the relay panel, a row of three protective relays (each in its drawout case: the black
#   cover frame, the relay's face, its tap block and tap screw, the time dial, the induction disc standing out of its
#   slot between the drag magnet's jaws, the target and seal-in unit, the cover latches and the target reset), the
#   engraved nameplate under each, its test switch (a row of levers, red for the trip circuit), the lockout relay (86)
#   with its handle, and two pilot lamps;
# - below (the lower block): the wiring, two strips of terminal blocks on DIN rails, each wired across into a slotted
#   wiring duct (wires through the gaps between its fingers), the control cables up through the floor into a clamp,
#   their cores fanning into the ducts, two miniature breakers, and the wireway along the top.
# Like the panels above, the front is drawn whole (16 image px a model pixel, RC_KEY's two textures, row 0 the lower
# block) and every box's front shows the drawing where it stands; the rest of a box is a plain material. Built in
# model pixels for the piece facing north over its two blocks (y 0..32), then cut into the blocks: what is the same in
# every block of a stack (the sides and back), the upper block's, the lower block's (with the plinth) and the cap.
RC_KEY = "relay_cab"
RC_F = f"pi_{RC_KEY}_f"
RC_GLASS = "pi_relay_glass"
SEE_THROUGH.add(RC_GLASS)
RC_RELAYS = [  # left side of the case, the device's number and name, the tap screw's place on the tap block (0..1)
    (2.7, "50/51", "PHASE O/C", 0.55),
    (6.3, "51N", "GROUND O/C", 0.3),
    (9.9, "27", "UNDERVOLT", 0.75),
]
RC_W, RC_Y0, RC_Y1 = 3.4, 22.4, 28.2      # a relay case's width, bottom, top
RC_RIM = 0.35                             # its cover frame
RC_WINDOW = (2.1, 3.0, 13.9, 29.0)        # the door's window (x0, y0, x1, y1)
RC_GLASS_Z = 14.8                         # its glass (0.1 thick)
RC_PANEL_Z = 11.2                         # the relay panel's front: the relays close behind the glass
RC_FACE_Z = RC_PANEL_Z + 0.7              # a relay's face, inside its frame
RC_FRAME_Z = RC_PANEL_Z + 1.8             # the frame's front
RC_PLATE_Z = 9.0                          # the lower mounting plate's front
RC_TS = (19.0, 21.0)                      # the test switches' bases (bottom, top)
RC_TAG = (21.3, 22.2)                     # the nameplates (two lines of the pixel font)
RC_LOR = (8.0, 17.75, 0.95)               # the lockout relay's round face: centre, radius
RC_LAMPS = [(4.4, 17.9, "pi_pnl_white", "125 VDC"), (11.6, 17.9, "pi_pnl_yellow", "TRIP CKT")]
RC_TERM = (3.7, 0.56, 0.5, 18)            # terminal blocks: the first's bottom, pitch, height, how many
RC_STRIPS = [(2.8, 4.6, 5.2, 5.4, "TB1"), (11.4, 13.2, 10.6, 10.8, "TB2")]   # x0, x1, duct finger wall x0, x1
RC_DUCTS = [(5.2, 6.8), (9.2, 10.8)]
RC_WIRES = ["pi_sb_cable", "pi_cab_white", "pi_pnl_red", "pi_cap_blue"]


def _round_z(cx, cy, z0, z1, r, tex):
    """A round part facing the front (a dial, a lamp, a hub) as five columns side by side across x: a stepped circle of
    radius r about (cx, cy). They touch but never overlap (tools/zfight.py)."""
    return [(cx + a0 * r, cy - h * r, z0, cx + a1 * r, cy + h * r, z1, tex)
            for a0, a1, h in ((-1.0, -0.75, 0.45), (-0.75, -0.35, 0.8), (-0.35, 0.35, 1.0), (0.35, 0.75, 0.8), (0.75, 1.0, 0.45))]


def _rc_terminals(strip):
    """One strip's terminal blocks: (y0, y1, side material) from the bottom; every sixth is a blue return terminal."""
    y, pitch, h, n = RC_TERM
    return [(y + k * pitch, y + k * pitch + h, "pi_cap_blue" if k % 6 == 5 else "pi_relay_term") for k in range(n)]


def relay_cabinet_front(nameplate=True):
    """The drawing: the door (its frame and nameplate), and through the window everything inside as seen from the front.
    nameplate: False leaves the plate off (round 26, #10: the relay room's panels have a wider one, lettered live)."""
    f = Front(1, 520)
    T = f.T
    import control_boards as CB

    def R(x0, y0, x1, y1, **kw):
        """A rectangle at least a texel each way (a thin seam)."""
        px0, py0, px1, py1 = round(x0 * U), round((32 - y1) * U), round(x1 * U) - 1, round((32 - y0) * U) - 1
        f.d.rectangle([px0, py0, max(px0, px1), max(py0, py1)], **kw)

    def pix(cx, cy, s, ink, scale=1):
        """A small legend centred on (cx, cy): bold letters the old pixel font's height (5 texels at 16 px a model
        pixel, a scale's worth more), now drawn at the drawings' 512 px a block so they read."""
        T(cx, cy, s, 7 * scale, fill=ink, bold=True)

    WHITE, BLACK = (238, 238, 232, 255), (26, 26, 28, 255)
    # The door: its edge, the nameplate on the top rail.
    R(1.0, 1.2, 15.0, 31.4, outline=(130, 132, 128, 255), width=3)
    R(1.1, 1.3, 14.9, 31.3, outline=(214, 216, 210, 255), width=1)
    wx0, wy0, wx1, wy1 = RC_WINDOW
    R(wx0 - 0.12, wy0 - 0.12, wx1 + 0.12, wy1 + 0.12, outline=(150, 152, 148, 255), width=2)
    if nameplate:
        R(4.4, 29.5, 11.6, 30.9, fill=BLACK, outline=(170, 172, 174, 255), width=2)
        T(8.0, 30.42, "RELAY CABINET", 12, fill=WHITE, bold=True)
        T(8.0, 29.86, "125 VDC CONTROL", 8, fill=WHITE, bold=True)
    # Upper: the relay panel.
    R(1.6, 16.0, 14.4, 29.0, fill=(204, 206, 200, 255))
    for x0, num, name, tap in RC_RELAYS:
        x1 = x0 + RC_W
        ix0, ix1, iy0, iy1 = x0 + RC_RIM, x1 - RC_RIM, RC_Y0 + RC_RIM, RC_Y1 - RC_RIM
        cx = (x0 + x1) / 2
        R(ix0, iy0, ix1, iy1, fill=(226, 222, 204, 255))                       # the relay's face
        R(ix0 + 0.3, iy1 - 1.0, ix1 - 0.3, iy1 - 0.35, fill=BLACK)              # tap block
        for k in range(5):
            tx = ix0 + 0.5 + k * (ix1 - ix0 - 1.0) / 4
            f.E(tx, iy1 - 0.55, 0.07, fill=(140, 140, 136, 255))
            pix(tx, iy1 - 0.83, str((1, 2, 3, 4, 6)[k]), WHITE)
        dy = iy1 - 1.75                                                       # the time dial
        f.E(cx, dy, 0.55, fill=(236, 236, 230, 255), outline=(60, 60, 60, 255), width=2)
        for k in range(11):
            a = math.radians(210 - 24 * k)
            f.line([(cx + math.cos(a) * 0.4, dy + math.sin(a) * 0.4), (cx + math.cos(a) * 0.52, dy + math.sin(a) * 0.52)], INK, 1)
        f.line([(cx, dy), (cx + 0.3, dy + 0.25)], (190, 30, 26, 255), 2)
        yd = iy0 + 2.0                                                        # the disc's slot
        R(cx - 1.15, yd - 0.08, cx + 1.15, yd + 0.2, fill=(20, 20, 22, 255))
        pix(cx, iy0 + 1.55, "TIME", (60, 60, 60, 255))
        R(ix0 + 0.3, iy0 + 0.3, ix0 + 1.3, iy0 + 1.2, fill=BLACK)              # target
        R(ix0 + 0.45, iy0 + 0.5, ix0 + 1.15, iy0 + 0.95, fill=(236, 236, 230, 255))
        R(ix0 + 0.45, iy0 + 0.5, ix0 + 0.8, iy0 + 0.95, fill=BLACK)
        R(ix1 - 1.2, iy0 + 0.3, ix1 - 0.3, iy0 + 1.2, fill=(60, 62, 66, 255))  # seal-in unit
        pix(ix1 - 0.75, iy0 + 0.75, "SI", WHITE)
        # Its nameplate (engraved: white letters through black) and its test switch.
        R(x0 + 0.2, RC_TAG[0], x1 - 0.2, RC_TAG[1], fill=BLACK, outline=(150, 150, 150, 255), width=1)
        pix(cx, RC_TAG[1] - 0.3, num, WHITE)
        pix(cx, RC_TAG[0] + 0.28, name, WHITE)
        R(x0 + 0.1, RC_TS[0], x1 - 0.1, RC_TS[1], fill=(54, 56, 60, 255))
        R(x0 + 0.3, RC_TS[0] + 0.05, x1 - 0.3, RC_TS[0] + 0.48, fill=(236, 236, 230, 255))
        pix(cx, RC_TS[0] + 0.27, "TS-" + num.split("/")[-1], BLACK)
        for k in range(7):
            xs = x0 + 0.3 + k * (RC_W - 0.6) / 6
            f.line([(xs, RC_TS[0] + 0.55), (xs, RC_TS[1] - 0.15)], (120, 120, 116, 255), 1)
    # The lockout relay: its plate, the round face with TRIP and RESET.
    lx, ly, lr = RC_LOR
    R(6.4, 16.45, 9.6, 19.0, fill=BLACK)
    f.E(lx, ly, lr, fill=(222, 222, 216, 255))
    f.E(lx, ly, lr * 0.45, fill=(200, 200, 196, 255))
    for k in range(2):                                                       # its two positions' marks
        a = math.radians(135 - 90 * k)
        f.line([(lx + math.cos(a) * lr * 0.62, ly + math.sin(a) * lr * 0.62), (lx + math.cos(a) * lr * 0.92, ly + math.sin(a) * lr * 0.92)],
               (190, 30, 26, 255) if k else INK, 3)
    pix(6.85, 18.66, "RST", WHITE)
    pix(9.15, 18.66, "TRP", (236, 70, 60, 255))
    pix(6.8, 16.75, "86", WHITE)
    pix(lx, 16.2, "LOCKOUT", BLACK)
    for cx, cy, _, label in RC_LAMPS:
        pix(cx, cy - 0.95, label, BLACK)
    # Lower: the mounting plate, its terminal strips, ducts, breakers and the wireway.
    R(1.6, 1.2, 14.4, 14.6, fill=(222, 224, 220, 255))
    for s in RC_STRIPS:
        x0, x1 = s[0], s[1]
        R(x0, 13.82, x1, 14.2, fill=BLACK)                                    # the top end stop carries the strip's name
        pix((x0 + x1) / 2, 14.01, s[4], WHITE)
        for k, (y0, y1, side) in enumerate(_rc_terminals(s)):
            body = (70, 110, 180, 255) if side == "pi_cap_blue" else (178, 180, 176, 255)
            R(x0, y0, x1, y1, fill=body)
            R(x0, y0, x1, y0 + 0.06, fill=(120, 122, 118, 255))
            for sx in (x0 + 0.28, x1 - 0.28):
                f.E(sx, (y0 + y1) / 2, 0.14, fill=(110, 112, 110, 255))
                f.line([(sx - 0.1, (y0 + y1) / 2), (sx + 0.1, (y0 + y1) / 2)], (40, 40, 40, 255), 1)
            R(x0 + 0.55, y0 + 0.08, x1 - 0.55, y1 - 0.06, fill=(244, 244, 240, 255))
            pix((x0 + x1) / 2, (y0 + y1) / 2 + 0.01, str(k + 1), BLACK)
    for x0, x1 in RC_DUCTS:
        R(x0, 3.2, x1, 14.6, fill=(202, 204, 200, 255))
        R((x0 + x1) / 2 - 0.05, 3.2, (x0 + x1) / 2 + 0.05, 14.6, fill=(184, 186, 182, 255))
    R(1.6, 14.6, 14.4, 16.0, fill=(202, 204, 200, 255))
    R(1.6, 15.25, 14.4, 15.35, fill=(184, 186, 182, 255))
    for i, x0 in enumerate((7.05, 8.05)):
        R(x0, 10.5, x0 + 0.9, 13.5, fill=(240, 240, 236, 255))
        R(x0 + 0.25, 11.9, x0 + 0.65, 13.0, fill=(60, 60, 62, 255))
        R(x0 + 0.3, 11.2, x0 + 0.6, 11.5, fill=(200, 40, 34, 255))
        pix(x0 + 0.45, 10.85, f"CB{i + 1}", BLACK)
        pix(x0 + 0.45, 13.25, "C6", BLACK)
    return f


def relay_cabinet_boxes():
    """The whole cabinet over its two blocks: (always, upper, lower, cap, plinth), each a list of boxes; F stands for
    the drawing (the block's slice of it)."""
    F = RC_F
    always = [(1.0, 0, 2.0, 1.6, 16, 14.6, "pi_cabinet"), (14.4, 0, 2.0, 15.0, 16, 14.6, "pi_cabinet"),
              (1.6, 0, 2.0, 14.4, 16, 2.6, "pi_cabinet")]
    front = {"*": "pi_cabinet", "south": F}
    blk = {"*": "pi_pnl_black", "south": F}
    (wx0, wy0, wx1, wy1), gz = RC_WINDOW, RC_GLASS_Z
    b = []
    # The door: stiles, rails, the glass behind a black gasket, hinges, the handle, the nameplate.
    b += [(1.0, 1.2, 14.6, wx0, 31.4, 15.2, front), (wx1, 1.2, 14.6, 15.0, 31.4, 15.2, front),
          (wx0, 1.2, 14.6, wx1, wy0, 15.2, front), (wx0, wy1, 14.6, wx1, 31.4, 15.2, front),
          (wx0, wy0, gz, wx1, wy1, gz + 0.1, {"south": RC_GLASS, "north": RC_GLASS}),
          (wx0, wy0, gz + 0.1, wx0 + 0.2, wy1, 15.1, "pi_pnl_black"), (wx1 - 0.2, wy0, gz + 0.1, wx1, wy1, 15.1, "pi_pnl_black"),
          (wx0 + 0.2, wy0, gz + 0.1, wx1 - 0.2, wy0 + 0.2, 15.1, "pi_pnl_black"), (wx0 + 0.2, wy1 - 0.2, gz + 0.1, wx1 - 0.2, wy1, 15.1, "pi_pnl_black"),
          (0.6, 5.0, 14.7, 1.0, 7.0, 15.1, "pi_chrome"), (0.6, 25.0, 14.7, 1.0, 27.0, 15.1, "pi_chrome"),
          (14.05, 16.8, 15.2, 14.85, 21.6, 15.35, "pi_pnl_black"), (14.2, 17.4, 15.35, 14.7, 20.8, 15.8, "pi_chrome"),
          (14.3, 21.0, 15.35, 14.6, 21.35, 15.5, "pi_brass"),
          (4.4, 29.5, 15.2, 11.6, 30.9, 15.34, blk)]
    # Upper: the relay panel and what is on it.
    z0 = RC_PANEL_Z
    b.append((1.6, 16.0, z0 - 0.6, 14.4, 31.4, z0, front))
    for x0, num, name, tap in RC_RELAYS:
        x1 = x0 + RC_W
        ix0, ix1, iy0, iy1 = x0 + RC_RIM, x1 - RC_RIM, RC_Y0 + RC_RIM, RC_Y1 - RC_RIM
        cx = (x0 + x1) / 2
        fz, rz = RC_FACE_Z, RC_FRAME_Z
        b += [(x0, RC_Y0, z0, ix0, RC_Y1, rz, "pi_pnl_black"), (ix1, RC_Y0, z0, x1, RC_Y1, rz, "pi_pnl_black"),
              (ix0, RC_Y0, z0, ix1, iy0, rz, "pi_pnl_black"), (ix0, iy1, z0, ix1, RC_Y1, rz, "pi_pnl_black"),
              (ix0, iy0, z0, ix1, iy1, fz, blk)]
        # Tap block and its screw; the time dial, its pointer; the disc out of its slot, the drag magnet's jaws.
        b.append((ix0 + 0.3, iy1 - 1.0, fz, ix1 - 0.3, iy1 - 0.35, fz + 0.35, blk))
        tx = ix0 + 0.5 + tap * (ix1 - ix0 - 1.0)
        b.append((tx - 0.17, iy1 - 0.85, fz + 0.35, tx + 0.17, iy1 - 0.5, fz + 0.6, "pi_brass"))
        dy = iy1 - 1.75
        b += [box[:6] + ({"*": "pi_chrome", "south": F},) for box in _round_z(cx, dy, fz, fz + 0.3, 0.55, None)]
        yd = iy0 + 2.0
        b += [(cx - 1.1, yd, fz, cx + 1.1, yd + 0.12, fz + 0.4, "pi_chrome"), (cx - 0.8, yd, fz + 0.4, cx + 0.8, yd + 0.12, fz + 0.7, "pi_chrome"),
              (cx - 0.4, yd, fz + 0.7, cx + 0.4, yd + 0.12, fz + 0.85, "pi_chrome"),
              (cx - 1.0, yd + 0.12, fz, cx - 0.45, yd + 0.42, fz + 0.55, "pi_pnl_black"),
              (cx - 1.0, yd - 0.3, fz, cx - 0.45, yd, fz + 0.55, "pi_pnl_black"),
              (cx - 0.06, yd + 0.12, fz + 0.12, cx + 0.06, dy - 0.55, fz + 0.24, "pi_chrome")]
        # Target, seal-in unit; the reset rod out of the frame's bottom, the cover latches.
        b += [(ix0 + 0.3, iy0 + 0.3, fz, ix0 + 1.3, iy0 + 1.2, fz + 0.3, blk), (ix1 - 1.2, iy0 + 0.3, fz, ix1 - 0.3, iy0 + 1.2, fz + 0.45, blk),
              (cx - 0.17, RC_Y0 + 0.05, rz, cx + 0.17, RC_Y0 + 0.3, rz + 0.25, "pi_chrome"),
              (x0 + 0.05, RC_Y1 - 0.6, rz, x0 + 0.3, RC_Y1 - 0.15, rz + 0.15, "pi_chrome"),
              (x1 - 0.3, RC_Y1 - 0.6, rz, x1 - 0.05, RC_Y1 - 0.15, rz + 0.15, "pi_chrome")]
        # Nameplate; the test switch: its base, the levers (each a blade and its grip, red on the trip circuit's).
        b.append((x0 + 0.2, RC_TAG[0], z0, x1 - 0.2, RC_TAG[1], z0 + 0.12, blk))
        b.append((x0 + 0.1, RC_TS[0], z0, x1 - 0.1, RC_TS[1], z0 + 0.6, blk))
        for k in range(6):
            xs = x0 + 0.3 + (k + 0.5) * (RC_W - 0.6) / 6
            b += [(xs - 0.12, RC_TS[0] + 0.6, z0 + 0.6, xs + 0.12, RC_TS[1] - 0.35, z0 + 0.85, "pi_chrome"),
                  (xs - 0.17, RC_TS[1] - 0.9, z0 + 0.85, xs + 0.17, RC_TS[1] - 0.35, z0 + 1.2, "pi_pnl_red" if k in (0, 5) else "pi_sb_cable")]
    lx, ly, lr = RC_LOR
    b.append((6.4, 16.45, z0, 9.6, 19.0, z0 + 0.15, blk))
    b += [box[:6] + ({"*": "pi_chrome", "south": F},) for box in _round_z(lx, ly, z0 + 0.15, z0 + 0.35, lr, None)]
    b += _round_z(lx, ly, z0 + 0.35, z0 + 0.75, 0.45, "pi_pnl_black")
    b += [(6.95, ly - 0.2, z0 + 0.75, lx, ly + 0.2, z0 + 1.2, "pi_pnl_black"), (6.5, ly - 0.3, z0 + 0.75, 6.95, ly + 0.3, z0 + 1.25, "pi_pnl_black")]
    for cx, cy, lens, _ in RC_LAMPS:
        b += _round_z(cx, cy, z0, z0 + 0.25, 0.6, "pi_chrome") + _round_z(cx, cy, z0 + 0.25, z0 + 0.55, 0.4, lens)
    # Lower: the mounting plate, the wireway along its top (out to the relay panel's front).
    lz = RC_PLATE_Z
    b += [(1.6, 1.2, lz - 0.6, 14.4, 14.6, lz, front), (1.6, 14.6, lz - 0.6, 14.4, 16.0, z0, {"*": "pi_relay_duct", "south": F})]
    y, pitch, h, n = RC_TERM
    for (sx0, sx1, fx0, fx1, _), (dx0, dx1) in zip(RC_STRIPS, RC_DUCTS):
        rx = (sx0 + sx1) / 2
        b += [(rx - 0.4, 3.2, lz, rx + 0.4, 14.2, lz + 0.3, "pi_chrome"),
              (sx0, 3.2, lz + 0.3, sx1, 3.6, lz + 1.6, "pi_pnl_black"), (sx0, 13.82, lz + 0.3, sx1, 14.2, lz + 1.6, blk)]
        left = sx1 < dx0    # the duct is to the strip's right
        for k, (ty0, ty1, side) in enumerate(_rc_terminals(None)):
            b.append((sx0, ty0, lz + 0.3, sx1, ty1, lz + 1.9, {"*": side, "south": F}))
            yc = (ty0 + ty1) / 2
            wx0_, wx1_ = (sx1, fx1) if left else (fx0, sx0)
            b.append((wx0_, yc - 0.08, lz + 1.0, wx1_, yc + 0.08, lz + 1.16, RC_WIRES[k % 4]))
        # The duct: fingers on the strip's side (a wire through each gap), the far wall (open where the cores come
        # in), the bundle inside, the cover.
        gaps = [y + k * pitch + h / 2 for k in range(n)]
        edges = [3.2] + [g for c in gaps for g in (c - 0.13, c + 0.13)] + [14.6]
        for e0, e1 in zip(edges[0::2], edges[1::2]):
            b.append((fx0, e0, lz, fx1, e1, lz + 2.4, "pi_relay_duct"))
        ox0, ox1 = (dx1 - 0.2, dx1) if left else (dx0, dx0 + 0.2)
        b += [(ox0, 3.2, lz, ox1, 9.6, lz + 2.4, "pi_relay_duct"), (ox0, 10.2, lz, ox1, 14.6, lz + 2.4, "pi_relay_duct"),
              (dx0 + 0.3, 3.2, lz + 0.1, dx1 - 0.3, 14.6, lz + 2.2, "pi_relay_bundle"),
              (dx0, 3.2, lz + 2.4, dx1, 14.6, lz + 2.65, {"*": "pi_relay_duct", "south": F})]
    # The control cables up through the floor, clamped to a strut, their cores fanning out into the ducts.
    b += [(7.2, 1.2, lz + 0.2, 7.8, 9.6, lz + 0.8, "pi_sb_cable"), (8.2, 1.2, lz + 0.2, 8.8, 9.6, lz + 0.8, "pi_cabinet_dark"),
          (7.1, 9.3, lz + 0.1, 7.9, 9.75, lz + 0.95, "pi_pnl_black"), (8.1, 9.3, lz + 0.1, 8.9, 9.75, lz + 0.95, "pi_pnl_black"),
          (6.7, 9.6, lz + 0.25, 7.8, 10.1, lz + 0.75, "pi_relay_bundle"), (8.2, 9.6, lz + 0.25, 9.3, 10.1, lz + 0.75, "pi_relay_bundle"),
          (6.9, 5.0, lz, 9.1, 5.7, lz + 1.1, "pi_chrome")]
    # The miniature breakers on their rail.
    b.append((7.0, 11.7, lz, 9.0, 12.3, lz + 0.3, "pi_chrome"))
    for x0 in (7.05, 8.05):
        b += [(x0, 10.5, lz + 0.3, x0 + 0.9, 13.5, lz + 2.0, {"*": "pi_cab_white", "south": F}),
              (x0 + 0.28, 12.1, lz + 2.0, x0 + 0.62, 12.85, lz + 2.4, "pi_pnl_black")]
    cap = [(0.8, 15.4, 1.8, 15.2, 16, 15.4, "pi_cabinet_dark")]
    plinth = [(0.8, 0, 1.8, 15.2, 1.2, 15.2, "pi_cabinet_dark")]
    upper, lower = [], []
    for box in b:
        y0, y1 = box[1], box[4]
        if y0 < 16 - 1e-6:
            lower.append(_rc_cut(box, y0, min(y1, 16), 0, y1 > 16 + 1e-6, "up"))
        if y1 > 16 + 1e-6:
            upper.append(_rc_cut(box, max(y0, 16), y1, 1, y0 < 16 - 1e-6, "down"))
    return always, upper, lower, cap, plinth


def _rc_cut(box, y0, y1, row, cut, face):
    """A box's share of one block (row 0 the lower): moved into the block, its front the block's slice of the drawing;
    a see-through box cut by the block line loses its face there."""
    t = box[6]
    if isinstance(t, dict):
        t = {k: (f"{RC_F}_{row}" if v == RC_F else v) for k, v in t.items()}
    if cut and (t == RC_GLASS or (isinstance(t, dict) and RC_GLASS in t.values())):
        t = dict(t) if isinstance(t, dict) else {"*": t}
        t[face] = None
    return (box[0], y0 - 16 * row, box[2], box[3], y1 - 16 * row, box[5], t) + tuple(box[7:])


def relay_cabinet_textures():
    tex = relay_cabinet_front().cut(RC_KEY)

    def bundle():
        img = Image.new("RGBA", (64, 64), (30, 30, 32, 255))
        d = ImageDraw.Draw(img)
        cols = [(30, 30, 32), (236, 236, 230), (190, 36, 30), (50, 80, 170), (30, 30, 32), (236, 236, 230)]
        for i, x in enumerate(range(0, 64, 4)):
            d.rectangle([x, 0, x + 3, 63], fill=cols[(i * 5) % len(cols)] + (255,))
            d.line([(x, 0), (x, 63)], fill=(16, 16, 18, 255))
        return img

    def glass():
        img = Image.new("RGBA", (128, 128), (200, 214, 220, 34))
        d = ImageDraw.Draw(img)
        for x0, w in ((18, 10), (40, 4), (84, 14)):
            d.polygon([(x0, 127), (x0 + w, 127), (x0 + w + 60, 0), (x0 + 60, 0)], fill=(236, 244, 248, 58))
        return img
    out = {name: (lambda img=img: img) for name, img in tex.items()}
    out.update({"pi_relay_term": lambda: _solid((178, 180, 176), 521, 0.03), "pi_relay_duct": lambda: _solid((204, 206, 202), 522, 0.03),
                "pi_relay_bundle": bundle, RC_GLASS: glass})
    return out


SHARP |= {f"{RC_F}_0", f"{RC_F}_1"}


# ------------------------------------------------------------------------------------------------ the station charger

# The DC equipment rooms' battery chargers (round 26, 8b; Justin: they reused the diesel room's charger, they get a
# model of their own): a 125 VDC Class 1E station battery charger (an SCR charger, 480 VAC three-phase in, 150 A out),
# one block wide and two high, every part that stands out of its door modelled: the hinges and the locking T-handle,
# the engraved nameplate, two switchboard meters (square black cases, the scale behind glass, the needle live), four
# pilot lamps in chrome bezels (lit live), the alarm annunciator (six windows in a black grid, CHARGER FAIL and
# EQUALIZE live) with its TEST and RESET buttons, the FLOAT/EQUALIZE selector, the equalize timer, the float and
# equalize voltage adjusters, the AC input and DC output breakers through their door cut-outs (toggles up, ON), the
# louvred air inlet at the bottom (real slats) and the exhaust hood on top. Model pixels, the piece facing north: x
# across, y up 0..32, its door's face at z 15.
SC_KEY = "station_charger"
SC_DOOR = 15.0                      # the door's face
SC_METERS = [(1.4, "DC VOLTS", (0, 150), "V", "dc_v"), (8.6, "DC AMPS", (0, 200), "A", "dc_a")]
SC_METER = (6.0, 21.6, 27.2)        # a meter's width, bottom, top
SC_LAMPS = [(2.9, "AC ON", (40, 180, 70), "chg_ac", "pi_lens_green"), (6.3, "DC ON", (40, 180, 70), "chg_dc", "pi_lens_green"),
            (9.7, "FLOAT", (240, 240, 230), "chg_float", "pi_lens_white"), (13.1, "EQUALIZE", (240, 170, 30), "chg_equal", "pi_lens_amber")]
SC_LAMP_Y = 19.0
SC_ANN = (1.4, 13.0, 3, 2, 2.9, 1.7)   # the annunciator: x0, y0, columns, rows, window width, height
SC_WINDOWS = [("AC|FAIL", "ann_charger_fail"), ("DC|LOW", "chg_dc_low"), ("DC|HIGH", "chg_dc_high"),
              ("CHGR|FAIL", "ann_charger_fail"), ("GND|FAULT", "chg_gnd"), ("EQUAL|IZING", "chg_equal")]
SC_GLASS = "pi_meter_glass"
SEE_THROUGH.add(SC_GLASS)


def station_charger(label="1A-1"):
    f = Front(1, 520)
    T = f.T
    WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)
    # The door's edge and its gasket line, the nameplate.
    f.R(0.6, 1.4, 15.4, 30.2, outline=(130, 132, 128, 255), width=3)
    f.R(2.0, 28.3, 14.0, 29.9, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    T(8.0, 29.4, f"BATTERY CHARGER {label}", 11, fill=WHITE, bold=True)
    T(8.0, 28.75, "125 VDC 150 A OUT   480 VAC 3 PH IN", 7, fill=WHITE, bold=True)
    # The meters' faces (the case and glass are real): white, a quarter-arc scale, the unit; the needles are live.
    w, y0, y1 = SC_METER
    for x0, label, scale, unit, ch in SC_METERS:
        f.meter(x0, y0, x0 + w, y1, label, scale, 0, unit, live=ch)
        f.live[-1]["z"] = SC_DOOR + 0.3
    # The lamps' legends, the annunciator's windows (unlit), the controls' legends.
    for x, label, colour, ch, _ in SC_LAMPS:
        T(x, SC_LAMP_Y - 1.25, label, 7, bold=True)
        f.live.append({"t": "lamp", "x": x, "y": SC_LAMP_Y, "r": 0.5, "z": SC_DOOR + 0.56, "colour": list(colour), "ch": ch})
    ax, ay, cols, rows, ww, wh = SC_ANN
    f.R(ax - 0.3, ay - 0.3, ax + cols * ww + 0.3, ay + rows * wh + 0.3, fill=(30, 30, 32, 255))
    for i, (text, ch) in enumerate(SC_WINDOWS):
        c, r = i % cols, i // cols
        wx0, wy1 = ax + c * ww + 0.12, ay + (rows - r) * wh - 0.12
        wx1, wy0 = wx0 + ww - 0.24, wy1 - wh + 0.24
        f.R(wx0, wy0, wx1, wy1, fill=(232, 232, 222, 255))
        words = text.split("|")
        for k, word in enumerate(words):
            T((wx0 + wx1) / 2, (wy0 + wy1) / 2 + (len(words) - 1) * 0.28 - k * 0.56, word, 7, bold=True)
        if ch:
            f.live.append({"t": "window", "x0": wx0, "y0": wy0, "x1": wx1, "y1": wy1, "z": SC_DOOR + 0.33,
                           "colour": [255, 200, 80], "ch": ch, "alpha": 1.0, "label": words})
    T(12.1, 14.95, "TEST", 6, bold=True)
    T(12.1, 12.95, "RESET", 6, bold=True)
    # MODE: the selector's two positions; the equalize timer's dial; the adjusters.
    T(2.2, 11.55, "FLOAT", 6, bold=True)
    T(4.6, 11.55, "EQUAL", 6, bold=True)
    T(3.4, 8.35, "MODE", 7, bold=True)
    f.E(7.4, 10.0, 1.35, fill=(238, 238, 232, 255), outline=(40, 40, 42, 255), width=2)
    for k in range(13):
        a = math.radians(225 - 270 * k / 12)
        r0 = 1.0 if k % 3 else 0.85
        f.line([(7.4 + math.cos(a) * r0, 10.0 + math.sin(a) * r0), (7.4 + math.cos(a) * 1.25, 10.0 + math.sin(a) * 1.25)], INK, 2)
    T(7.4, 8.35, "EQ TIMER (H)", 7, bold=True)
    # Its pointer is live: the hours of equalize left, from 0 at the bottom left round to 24 (Justin: "any meters and
    # such are actually animated to work realistically").
    f.live.append({"t": "dial", "x": 7.4, "y": 10.0, "r": 1.35, "z": SC_DOOR + 0.62, "lo": 0, "hi": 24, "start": 225, "sweep": 270,
                   "ch": "eq_hours"})
    for x, label in ((10.9, "FLOAT ADJ"), (13.5, "EQ ADJ")):
        T(x, 8.35, label, 6, bold=True)
    # The breakers' cut-outs and legends.
    for cx, label, rating in ((4.5, "AC INPUT", "480 V  100 A"), (11.5, "DC OUTPUT", "125 V  225 A")):
        T(cx, 7.65, label, 7, bold=True)
        T(cx, 4.15, rating, 6, bold=True)
    # The louvred inlet's opening: dark behind the slats.
    f.R(2.0, 1.6, 14.0, 3.6, fill=(22, 22, 24, 255))
    return f


def _sc_round(cx, cy, z0, z1, r, tex):
    """A round part facing out of the door, as touching columns (tools/zfight.py)."""
    return [(cx + a0 * r, cy - h * r, z0, cx + a1 * r, cy + h * r, z1, tex)
            for a0, a1, h in ((-1.0, -0.75, 0.45), (-0.75, -0.35, 0.8), (-0.35, 0.35, 1.0), (0.35, 0.75, 0.8), (0.75, 1.0, 0.45))]


def station_charger_boxes():
    """The whole charger (both blocks): body, door and everything on it; F is the drawing."""
    F = f"pi_{SC_KEY}_f"
    D = SC_DOOR
    face = {"*": "pi_pnl_black", "south": F}
    blk = "pi_pnl_black"
    # The cabinet: plinth, body, door, cap, the exhaust hood (in two halves either side of its DC output conduit's hub:
    # round 26, 8f, up to the tray), hinges, the locking T-handle, the louvred inlet.
    b = _cabinet_shell(F, hub="one")
    # The nameplate.
    b.append((2.0, 28.3, D, 14.0, 29.9, D + 0.12, face))
    # The meters: the black case standing out of the door, its face set back inside, the glass in front.
    w, y0, y1 = SC_METER
    for x0, *_ in SC_METERS:
        x1 = x0 + w
        b += [(x0, y0, D, x0 + 0.4, y1, D + 0.7, blk), (x1 - 0.4, y0, D, x1, y1, D + 0.7, blk),
              (x0 + 0.4, y0, D, x1 - 0.4, y0 + 0.4, D + 0.7, blk), (x0 + 0.4, y1 - 0.4, D, x1 - 0.4, y1, D + 0.7, blk),
              (x0 + 0.4, y0 + 0.4, D, x1 - 0.4, y1 - 0.4, D + 0.15, face),
              (x0 + 0.4, y0 + 0.4, D + 0.55, x1 - 0.4, y1 - 0.4, D + 0.6, {"south": SC_GLASS, "north": SC_GLASS}),
              # The zero adjuster under the glass.
              (x0 + w / 2 - 0.25, y0 + 0.05, D + 0.7, x0 + w / 2 + 0.25, y0 + 0.35, D + 0.85, "pi_cap_black")]
    # The pilot lamps: a chrome bezel and the lens (unlit; the light is the game's, live).
    for x, _, _, _, lens in SC_LAMPS:
        b += _sc_round(x, SC_LAMP_Y, D, D + 0.3, 0.75, "pi_chrome") + _sc_round(x, SC_LAMP_Y, D + 0.3, D + 0.5, 0.5, lens)
    # The annunciator: its black grid standing out, the windows set back in it; TEST and RESET.
    ax, ay, cols, rows, ww, wh = SC_ANN
    gx0, gy0, gx1, gy1 = ax - 0.3, ay - 0.3, ax + cols * ww + 0.3, ay + rows * wh + 0.3
    b.append((gx0, gy0, D, gx1, gy1, D + 0.25, face))
    for c in range(cols + 1):
        x = ax + c * ww
        b.append((x - (0.3 if c in (0, cols) else 0.12), gy0, D + 0.25, x + (0.3 if c in (0, cols) else 0.12), gy1, D + 0.5, blk))
    for r in range(rows + 1):
        y = ay + r * wh
        for c in range(cols):
            xa = ax + c * ww + (0.3 if c == 0 else 0.12)
            xb = ax + (c + 1) * ww - (0.3 if c + 1 == cols else 0.12)
            b.append((xa, y - (0.3 if r in (0, rows) else 0.12), D + 0.25, xb, y + (0.3 if r in (0, rows) else 0.12), D + 0.5, blk))
    for y in (15.8, 13.8):
        b += [(11.6, y - 0.5, D, 12.6, y + 0.5, D + 0.12, blk)] + _sc_round(12.1, y, D + 0.12, D + 0.5, 0.42, "pi_cap_black")
    # MODE selector: its plate, the knob, the pointer ridge (up: FLOAT).
    b += [(2.6, 9.2, D, 4.2, 10.8, D + 0.12, blk)] + _sc_round(3.4, 10.0, D + 0.12, D + 0.5, 0.6, "pi_cap_black")
    b.append((3.3, 10.0, D + 0.5, 3.5, 10.55, D + 0.62, "pi_pnl_white"))
    # The equalize timer: its dial face set in a bezel, the knob and its pointer (at 0).
    b += _sc_round(7.4, 10.0, D, D + 0.12, 1.5, "pi_chrome")
    b += [(x, y0_, D + 0.12, x1_, y1_, D + 0.25, face) for x, y0_, _, x1_, y1_, _, _ in _sc_round(7.4, 10.0, 0, 0, 1.35, None)]
    b += _sc_round(7.4, 10.0, D + 0.25, D + 0.6, 0.45, "pi_cap_black")
    # The adjusters: a slotted shaft in a lock ring.
    for x in (10.9, 13.5):
        b += _sc_round(x, 10.0, D, D + 0.15, 0.6, "pi_chrome") + _sc_round(x, 10.0, D + 0.15, D + 0.45, 0.42, "pi_brass")
        b.append((x - 0.25, 9.95, D + 0.45, x + 0.25, 10.05, D + 0.56, "pi_pnl_black"))
    # The breakers: the door's cut-out frame, the breaker's moulded front inside it, its toggle up (ON) with its grip.
    for cx in (4.5, 11.5):
        b += [(cx - 2.0, 4.6, D, cx + 2.0, 7.2, D + 0.15, "pi_cabinet_dark"),
              (cx - 1.4, 4.9, D + 0.15, cx + 1.4, 6.9, D + 0.45, "pi_pnl_black"),
              (cx - 0.35, 5.6, D + 0.45, cx + 0.35, 6.6, D + 0.75, "pi_pnl_black"),
              (cx - 0.45, 6.6, D + 0.45, cx + 0.45, 6.85, D + 0.95, "pi_cab_white")]
    return b



# ------------------------------------------------------------------------------------------------ the vital inverter

# The DC rooms' vital AC inverters, rebuilt to match the station chargers (round 26, 8g; Justin: rebuild them "to
# match", every meter live). A 10 kVA inverter: the station battery's 125 VDC made into steady 120 VAC for the vital
# instrument buses, with its static transfer switch onto the bypass (the regulating transformer the MCC's REG XF
# breaker feeds) when its DC fails, and the manual bypass switch. Its door: the nameplate; DC VOLTS, AC VOLTS and AC
# AMPS meters behind glass; the output frequency's green digits; the mimic of its power path (DC in, the inverter, the
# static switch, the load; the bypass into the static switch), its lines lit by what carries the load; DC IN, INVERTER,
# ON INV and BYPASS lamps; a six-window annunciator and ALARM RESET; the manual bypass switch's handle (NORMAL); the DC
# input breaker; the louvred inlet, the exhaust hood, hinges and the T-handle. All of it moves with the battery and the
# bypass (client/EdgTelemetry.java inverter).
VI_KEY = "vital_inverter"
VI_METERS = [(1.15, "DC VOLTS", (0, 150), "V", "inv_dc_v"), (5.85, "AC VOLTS", (0, 150), "V", "inv_ac_v"),
             (10.55, "AC AMPS", (0, 100), "A", "inv_ac_a")]
VI_METER = (4.3, 22.8, 27.4)
VI_FREQ = (5.6, 20.0, 12.6, 21.6)            # the frequency display's bezel
VI_MIMIC = (1.4, 15.6, 12.6, 19.4)           # its mimic panel
VI_MAIN_Y, VI_BYP_Y = 18.4, 16.6             # the mimic's lines
VI_LAMPS = [(2.6, "DC IN", (40, 180, 70), "inv_dc", "pi_lens_green"), (5.6, "INVERTER", (40, 180, 70), "inv_run", "pi_lens_green"),
            (8.6, "ON INV", (240, 240, 230), "inv_on", "pi_lens_white"), (11.6, "BYPASS", (240, 170, 30), "inv_byp", "pi_lens_amber")]
VI_LAMP_Y = 14.4
VI_ANN = (1.4, 8.8, 3, 2, 2.9, 1.7)
VI_WINDOWS = [("INV|FAIL", "inv_fail"), ("LOW|DC", "inv_low_dc"), ("BYP SRC|FAIL", "inv_byp_fail"),
              ("ON|BYPASS", "inv_byp"), ("OUT OF|SYNC", "inv_sync"), ("FAN|FAIL", "inv_fan")]


def vital_inverter(label="1A-1"):
    """The vital inverter's door, drawn (the case, glass, lamps and handles are real: vital_inverter_boxes)."""
    f = Front(1, 540)
    T = f.T
    WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)
    D = SC_DOOR
    f.R(0.6, 1.4, 15.4, 30.2, outline=(130, 132, 128, 255), width=3)
    f.R(2.0, 28.3, 14.0, 29.9, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    T(8.0, 29.4, f"VITAL AC INVERTER {label}", 11, fill=WHITE, bold=True)
    T(8.0, 28.75, "125 VDC IN    120 VAC 60 HZ 10 KVA", 7, fill=WHITE, bold=True)
    w, y0, y1 = VI_METER
    for x0, label, scale, unit, ch in VI_METERS:
        f.meter(x0, y0, x0 + w, y1, label, scale, 0, unit, live=ch)
        f.live[-1]["z"] = D + 0.3
    # The frequency display: dark green glass; its digits are live.
    bx0, by0, bx1, by1 = VI_FREQ
    f.R(bx0 + 0.3, by0 + 0.3, bx1 - 0.3, by1 - 0.3, fill=(14, 34, 22, 255))
    T(3.3, (by0 + by1) / 2 + 0.25, "OUTPUT", 7, bold=True)
    T(3.3, (by0 + by1) / 2 - 0.35, "FREQ HZ", 7, bold=True)
    f.text((bx0 + bx1) / 2, (by0 + by1) / 2, D + 0.2, 0.75, (120, 250, 150), "inv_hz", "center")
    # The mimic: its lines and labels drawn light on the dark panel (lit over live), the inverter and static switch
    # boxes (real) labelled.
    mx0, my0, mx1, my1 = VI_MIMIC
    f.R(mx0, my0, mx1, my1, fill=(52, 54, 58, 255))
    LINE = (170, 172, 168, 255)
    for x0, x1, y in ((1.9, 4.6, VI_MAIN_Y), (6.6, 8.6, VI_MAIN_Y), (10.2, 12.2, VI_MAIN_Y), (1.9, 8.6, VI_BYP_Y)):
        f.R(x0, y - 0.1, x1, y + 0.1, fill=LINE)
    for x, y, label in ((3.25, VI_MAIN_Y + 0.5, "DC IN"), (3.25, VI_BYP_Y + 0.5, "BYPASS"), (11.2, VI_MAIN_Y + 0.5, "LOAD"),
                        (5.6, VI_MAIN_Y, "INV"), (9.4, (VI_MAIN_Y + VI_BYP_Y) / 2, "SS")):
        T(x, y, label, 6, fill=WHITE if label in ("INV", "SS") else (220, 222, 216, 255), bold=True)
    for x0, x1, y, ch, c1, c2 in ((1.9, 4.6, VI_MAIN_Y, "inv_seg_dc", (90, 230, 120), None), (6.6, 8.6, VI_MAIN_Y, "inv_seg_inv", (90, 230, 120), None),
                                  (1.9, 8.6, VI_BYP_Y, "inv_seg_byp", (250, 190, 70), None),
                                  (10.2, 12.2, VI_MAIN_Y, "inv_seg_out", (90, 230, 120), (250, 190, 70))):
        f.glow(x0, y - 0.1, x1, y + 0.1, D + 0.21, c1, ch, alpha=1.0, colour2=c2)
    # The lamps' legends; the annunciator (live); ALARM RESET.
    for x, label, colour, ch, _ in VI_LAMPS:
        T(x, VI_LAMP_Y - 1.15, label, 6, bold=True)
        f.live.append({"t": "lamp", "x": x, "y": VI_LAMP_Y, "r": 0.5, "z": D + 0.56, "colour": list(colour), "ch": ch})
    ax, ay, cols, rows, ww, wh = VI_ANN
    f.R(ax - 0.3, ay - 0.3, ax + cols * ww + 0.3, ay + rows * wh + 0.3, fill=(30, 30, 32, 255))
    for i, (text, ch) in enumerate(VI_WINDOWS):
        c, r = i % cols, i // cols
        wx0, wy1 = ax + c * ww + 0.12, ay + (rows - r) * wh - 0.12
        wx1, wy0 = wx0 + ww - 0.24, wy1 - wh + 0.24
        f.R(wx0, wy0, wx1, wy1, fill=(232, 232, 222, 255))
        words = text.split("|")
        for k, word in enumerate(words):
            T((wx0 + wx1) / 2, (wy0 + wy1) / 2 + (len(words) - 1) * 0.28 - k * 0.56, word, 6, bold=True)
        f.live.append({"t": "window", "x0": wx0, "y0": wy0, "x1": wx1, "y1": wy1, "z": D + 0.33,
                       "colour": [255, 200, 80], "ch": ch, "alpha": 1.0, "label": words})
    T(12.1, 9.55, "ALARM", 6, bold=True)
    T(12.1, 9.0, "RESET", 6, bold=True)
    # The manual bypass switch's positions and name; the DC input breaker's.
    T(4.0, 7.9, "NORMAL", 6, bold=True)
    T(6.6, 6.1, "BYPASS", 6, bold=True)
    T(4.0, 4.25, "MANUAL BYPASS", 6, bold=True)
    T(11.5, 7.65, "DC INPUT", 7, bold=True)
    T(11.5, 4.15, "125 V  150 A", 6, bold=True)
    f.R(2.0, 1.6, 14.0, 3.6, fill=(22, 22, 24, 255))
    return f


# Where a cabinet's conduits leave its top (x, z: model pixels, the piece facing north), by _cabinet_shell's hub.
CABINET_HUBS = {None: (), "one": ((8.0, 8.0),), "pair": ((4.5, 8.0), (11.5, 8.0))}


def _cabinet_shell(F, hub=None):
    """The station charger's and the inverter's cabinet: plinth, body, door, cap, exhaust hood, hinges, T-handle,
    louvred inlet. hub: where conduits come out of its top (round 26, 8e/8f: up to the tray): "one" in the middle (a
    charger's DC output, a ground detector's), the hood in two halves either side; "pair" (an inverter's DC feed and AC
    output, side by side), the hood narrowed between them."""
    D = SC_DOOR
    b = [(0.8, 0, 2.6, 15.2, 1.2, 14.0, "pi_toe_kick"),
         (0.4, 1.2, 1.6, 15.6, 30.4, 14.6, "pi_cabinet"),
         (0.6, 1.4, 14.6, 15.4, 30.2, D, {"*": "pi_cabinet", "south": F}),
         (0.2, 30.4, 1.4, 15.8, 31.2, 15.2, "pi_cabinet_dark")]
    hoods = {None: ((2.5, 13.5),), "one": ((2.5, 6.3), (9.7, 13.5)), "pair": ((6.3, 9.7),)}[hub]
    for hx0, hx1 in hoods:
        b += [(hx0, 31.9, 4.0, hx1, 32.0, 12.4, "pi_cabinet_dark"), (hx0, 31.2, 4.0, hx0 + 0.4, 31.9, 12.4, "pi_cabinet_dark"),
              (hx1 - 0.4, 31.2, 4.0, hx1, 31.9, 12.4, "pi_cabinet_dark"), (hx0 + 0.4, 31.2, 4.0, hx1 - 0.4, 31.9, 4.4, "pi_pnl_black")]
        for k in range(3):
            b.append((hx0 + 0.4, 31.25 + k * 0.22, 12.0, hx1 - 0.4, 31.37 + k * 0.22, 12.4, "pi_cabinet_dark"))
    for x, z in CABINET_HUBS[hub]:
        b += _top_hub(x, 31.2, z)
    for y in (4.0, 26.0):
        b.append((0.15, y, 14.5, 0.6, y + 2.0, 15.25, "pi_chrome"))
    b += [(13.7, 12.4, D, 14.7, 17.4, D + 0.15, "pi_pnl_black"), (14.0, 13.0, D + 0.15, 14.4, 16.6, D + 0.5, "pi_chrome"),
          (13.85, 15.8, D + 0.5, 14.55, 16.4, D + 0.75, "pi_chrome"), (14.05, 12.7, D + 0.15, 14.35, 12.95, D + 0.3, "pi_brass")]
    b += [(1.8, 1.4, D, 14.2, 1.6, D + 0.3, "pi_cabinet_dark"), (1.8, 3.6, D, 14.2, 3.8, D + 0.3, "pi_cabinet_dark"),
          (1.8, 1.6, D, 2.0, 3.6, D + 0.3, "pi_cabinet_dark"), (14.0, 1.6, D, 14.2, 3.6, D + 0.3, "pi_cabinet_dark")]
    for k in range(4):
        yc = 1.95 + k * 0.5
        b.append((2.0, yc - 0.06, D - 0.1, 14.0, yc + 0.06, D + 0.4, "pi_cabinet_dark",
                  {"rotation": {"angle": -22.5, "axis": "x", "origin": [8.0, yc, D + 0.15]}}))
    return b


def _meter_case(x0, y0, x1, y1, face):
    """A switchboard meter's black case out of the door, its face set back inside it, the glass in front, the zero
    adjuster under the glass."""
    D = SC_DOOR
    blk = "pi_pnl_black"
    xm = (x0 + x1) / 2
    return [(x0, y0, D, x0 + 0.4, y1, D + 0.7, blk), (x1 - 0.4, y0, D, x1, y1, D + 0.7, blk),
            (x0 + 0.4, y0, D, x1 - 0.4, y0 + 0.4, D + 0.7, blk), (x0 + 0.4, y1 - 0.4, D, x1 - 0.4, y1, D + 0.7, blk),
            (x0 + 0.4, y0 + 0.4, D, x1 - 0.4, y1 - 0.4, D + 0.15, face),
            (x0 + 0.4, y0 + 0.4, D + 0.55, x1 - 0.4, y1 - 0.4, D + 0.6, {"south": SC_GLASS, "north": SC_GLASS}),
            (xm - 0.25, y0 + 0.05, D + 0.7, xm + 0.25, y0 + 0.35, D + 0.85, "pi_cap_black")]


def _annunciator(ax, ay, cols, rows, ww, wh, face):
    """An annunciator's black grid out of the door, its windows set back in it."""
    D = SC_DOOR
    blk = "pi_pnl_black"
    gx0, gy0, gx1, gy1 = ax - 0.3, ay - 0.3, ax + cols * ww + 0.3, ay + rows * wh + 0.3
    b = [(gx0, gy0, D, gx1, gy1, D + 0.25, face)]
    for c in range(cols + 1):
        x = ax + c * ww
        b.append((x - (0.3 if c in (0, cols) else 0.12), gy0, D + 0.25, x + (0.3 if c in (0, cols) else 0.12), gy1, D + 0.5, blk))
    for r in range(rows + 1):
        y = ay + r * wh
        for c in range(cols):
            xa = ax + c * ww + (0.3 if c == 0 else 0.12)
            xb = ax + (c + 1) * ww - (0.3 if c + 1 == cols else 0.12)
            b.append((xa, y - (0.3 if r in (0, rows) else 0.12), D + 0.25, xb, y + (0.3 if r in (0, rows) else 0.12), D + 0.5, blk))
    return b


def vital_inverter_boxes():
    F = f"pi_{VI_KEY}_f"
    D = SC_DOOR
    face = {"*": "pi_pnl_black", "south": F}
    blk = "pi_pnl_black"
    b = _cabinet_shell(F, hub="pair")
    b.append((2.0, 28.3, D, 14.0, 29.9, D + 0.12, face))
    w, y0, y1 = VI_METER
    for x0, *_ in VI_METERS:
        b += _meter_case(x0, y0, x0 + w, y1, face)
    # The frequency display: its bezel round the glass, the glass set back.
    bx0, by0, bx1, by1 = VI_FREQ
    b += [(bx0, by0, D, bx0 + 0.3, by1, D + 0.4, blk), (bx1 - 0.3, by0, D, bx1, by1, D + 0.4, blk),
          (bx0 + 0.3, by0, D, bx1 - 0.3, by0 + 0.3, D + 0.4, blk), (bx0 + 0.3, by1 - 0.3, D, bx1 - 0.3, by1, D + 0.4, blk),
          (bx0 + 0.3, by0 + 0.3, D, bx1 - 0.3, by1 - 0.3, D + 0.1, face)]
    # The mimic panel; its inverter and static switch boxes standing out of it.
    mx0, my0, mx1, my1 = VI_MIMIC
    b.append((mx0, my0, D, mx1, my1, D + 0.15, face))
    b += [(4.6, VI_MAIN_Y - 0.6, D + 0.15, 6.6, VI_MAIN_Y + 0.6, D + 0.35, face),
          (8.6, VI_BYP_Y - 0.5, D + 0.15, 10.2, VI_MAIN_Y + 0.5, D + 0.35, face)]
    for x, _, _, _, lens in VI_LAMPS:
        b += _sc_round(x, VI_LAMP_Y, D, D + 0.3, 0.7, "pi_chrome") + _sc_round(x, VI_LAMP_Y, D + 0.3, D + 0.5, 0.5, lens)
    b += _annunciator(*VI_ANN, face)
    b += [(11.6, 10.0, D, 12.6, 11.0, D + 0.12, blk)] + _sc_round(12.1, 10.5, D + 0.12, D + 0.5, 0.42, "pi_cap_black")
    # The manual bypass switch: its plate, the hub and the pistol-grip handle up at NORMAL.
    b += [(2.6, 4.7, D, 5.4, 7.5, D + 0.15, "pi_chrome")] + _sc_round(4.0, 6.1, D + 0.15, D + 0.5, 0.65, "pi_cap_black")
    b += [(3.7, 6.1, D + 0.5, 4.3, 7.3, D + 0.85, blk), (3.6, 7.3, D + 0.5, 4.4, 7.55, D + 0.95, blk)]
    # The DC input breaker through its cut-out, its toggle up (ON).
    cx = 11.5
    b += [(cx - 2.0, 4.6, D, cx + 2.0, 7.2, D + 0.15, "pi_cabinet_dark"),
          (cx - 1.4, 4.9, D + 0.15, cx + 1.4, 6.9, D + 0.45, "pi_pnl_black"),
          (cx - 0.35, 5.6, D + 0.45, cx + 0.35, 6.6, D + 0.75, "pi_pnl_black"),
          (cx - 0.45, 6.6, D + 0.45, cx + 0.45, 6.85, D + 0.95, "pi_cab_white")]
    return b


# ------------------------------------------------------------------------------------------------ the ground detector

# Each DC room's ground detector and battery monitor (round 26, 8f; Justin: "Healthy, with TEST", "Yes, with a screen",
# "End of the equipment row"): a cabinet like the chargers' at the end of the row, on the train's 125 VDC bus. The
# battery monitor's screen at the top: the string's volts and amps (in on charge, out on discharge), its state of
# charge and temperature, what it is doing (FLOAT, RECHARGE, EQUALIZE, DISCHARGE), every one of its 60 cells' volts as
# a bar (two rows of 30), its MENU and ALARM ACK keys and ALARM lamp. Below, the ground detector: + BUS TO GROUND and
# - BUS TO GROUND voltmeters (each half the bus while neither side is grounded), the + GND and - GND lamps (that side
# grounded) and DC ON, and the + BUS and - BUS test buttons, which put a ground on that side through the test resistor
# while held: its meter drops to nearly nothing, the other reads the whole bus, its lamp lights and the alarm sounds
# (nuclear/GroundDetectorBlock.java). All of it reads the train's station battery (client/EdgTelemetry.java
# groundDetector). Its conduit goes up from its top into the tray over it.
GD_KEY = "dc_ground_detector"
GD_SCREEN = (1.5, 19.8, 14.5, 27.7)          # the screen's bezel
GD_FRAME = 0.35                              # the bezel's width
GD_LCD = ((10, 26, 16, 255), (110, 196, 132, 255), (140, 255, 170))   # the screen: its glass, the printed legends, the readings
GD_HEAD_Y, GD_ROWS = 26.75, (25.4, 24.2)     # the screen's heading and its two rows of readings
GD_VALUE_X = (7.3, 13.8)                     # where each column's readings end (right-aligned)
GD_BARS = (3.25, 13.8, ((22.0, 23.3), (20.5, 21.8)))   # the cells' bars: from x, to x; each row's bottom and top
GD_CELLS, GD_CELL_LO, GD_CELL_HI, GD_CELL_WARN = 60, 2.0, 2.35, 2.10   # the bars' scale (volts a cell), amber below WARN
GD_KEY_Y = 18.95                              # the monitor's keys and ALARM lamp
GD_KEYS = [(2.5, "MENU"), (6.6, "ALARM ACK")]
GD_ALARM = 13.4
GD_METER = (5.4, 12.0, 17.0)                 # a ground voltmeter's width, bottom, top
GD_METERS = [(1.2, "+ BUS TO GROUND", "gd_pos_v"), (7.4, "- BUS TO GROUND", "gd_neg_v")]
GD_LAMP_Y = 9.6
GD_LAMPS = [(3.9, "+ GND", (240, 170, 30), "gd_pos", "pi_lens_amber"), (7.0, "DC ON", (240, 240, 230), "gd_dc", "pi_lens_white"),
            (10.1, "- GND", (240, 170, 30), "gd_neg", "pi_lens_amber")]
GD_TEST_Y = 5.7
GD_TESTS = [(3.9, "+ BUS"), (10.1, "- BUS")]  # the test buttons (nuclear/GroundDetectorBlock.java TESTS)


def ground_detector(label="1A"):
    """The ground detector's door, drawn (the bezel, meter cases, glass, lamps, keys and buttons are real:
    ground_detector_boxes)."""
    f = Front(1, 580)
    T = f.T
    WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)
    D = SC_DOOR
    f.R(0.6, 1.4, 15.4, 30.2, outline=(130, 132, 128, 255), width=3)
    f.R(2.0, 28.3, 14.0, 29.9, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    T(8.0, 29.4, f"DC GROUND DETECTOR {label}", 11, fill=WHITE, bold=True)
    T(8.0, 28.75, "BATTERY MONITOR    125 VDC    60 CELLS A STRING", 7, fill=WHITE, bold=True)
    # The battery monitor's screen: its printed legends and rules; the readings, the state and the cells' bars are live.
    GLASS, INKG, LIT = GD_LCD
    x0, y0, x1, y1 = GD_SCREEN
    gx0, gy0, gx1, gy1 = x0 + GD_FRAME, y0 + GD_FRAME, x1 - GD_FRAME, y1 - GD_FRAME
    f.R(x0, y0, x1, y1, fill=(28, 28, 30, 255))
    f.R(gx0, gy0, gx1, gy1, fill=GLASS)
    zt = D + 0.2
    T(2.2, GD_HEAD_Y, f"BATTERY {label}", 8, fill=INKG, bold=True, anchor="lm")
    # Which string the screen shows (round 26, #9: two strings in parallel; MENU steps through them), what it is doing.
    f.text(5.55, GD_HEAD_Y, zt, 0.55, LIT, "bm_str", "left")
    f.text(GD_VALUE_X[1], GD_HEAD_Y, zt, 0.55, LIT, "bm_mode", "right")
    for y in (26.2, 23.65):
        f.R(2.1, y - 0.04, 13.9, y + 0.04, fill=INKG)
    for (la, ca), (lb, cb), y in ((("VOLTS", "bm_v"), ("AMPS", "bm_a"), GD_ROWS[0]), (("CHARGE %", "bm_soc"), ("TEMP \u00b0C", "bm_t"), GD_ROWS[1])):
        T(2.2, y, la, 7, fill=INKG, bold=True, anchor="lm")
        T(8.0, y, lb, 7, fill=INKG, bold=True, anchor="lm")
        f.text(GD_VALUE_X[0], y, zt, 0.75, LIT, ca, "right")
        f.text(GD_VALUE_X[1], y, zt, 0.75, LIT, cb, "right")
    # The cells' bars: each cell's slot (dark), the float level (2.20 V a cell) dashed across behind them, the row's
    # first cell number at its left.
    bx0, bx1, rows = GD_BARS
    pitch = (bx1 - bx0) / (GD_CELLS // 2)
    for r, (ry0, ry1) in enumerate(rows):
        T(2.65, (ry0 + ry1) / 2, str(1 + r * GD_CELLS // 2), 6, fill=INKG, bold=True)
        level = ry0 + (ry1 - ry0) * (2.20 - GD_CELL_LO) / (GD_CELL_HI - GD_CELL_LO)
        f.R(bx0 - 0.05, level - 0.025, bx1, level + 0.025, fill=INKG)
        for k in range(GD_CELLS // 2):
            cx0 = bx0 + k * pitch + 0.06
            cx1 = cx0 + pitch - 0.12
            f.R(cx0, ry0, cx1, ry1, fill=(22, 48, 32, 255))
            f.live.append({"t": "bar", "x0": round(cx0, 4), "y0": ry0, "x1": round(cx1, 4), "y1": ry1, "z": D + 0.16, "lo": GD_CELL_LO,
                           "hi": GD_CELL_HI, "warn": GD_CELL_WARN, "colour": [110, 235, 140], "colour2": [255, 190, 60],
                           "ch": f"bm_c{r * GD_CELLS // 2 + k}"})
    # Its keys' legends, the ALARM lamp (live: an alarm flashes it).
    for x, label_ in GD_KEYS:
        T(x + 0.75, GD_KEY_Y, label_, 7, bold=True, anchor="lm")
    T(GD_ALARM - 0.95, GD_KEY_Y, "ALARM", 7, bold=True, anchor="rm")
    f.live.append({"t": "lamp", "x": GD_ALARM, "y": GD_KEY_Y, "r": 0.5, "z": D + 0.56, "colour": [240, 170, 30], "ch": "bm_alarm"})
    # The ground detector: its heading between two rules; the voltmeters (needles live); its lamps; the test buttons.
    T(7.0, 17.55, "GROUND DETECTOR", 8, bold=True)
    f.R(1.2, 17.51, 4.3, 17.59, fill=INK)
    f.R(9.7, 17.51, 12.8, 17.59, fill=INK)
    w, my0, my1 = GD_METER
    for mx0, label_, ch in GD_METERS:
        f.meter(mx0, my0, mx0 + w, my1, label_, (0, 150), 0, "V", live=ch)
        f.live[-1]["z"] = D + 0.3
    for x, label_, colour, ch, _ in GD_LAMPS:
        T(x, GD_LAMP_Y - 1.2, label_, 7, bold=True)
        f.live.append({"t": "lamp", "x": x, "y": GD_LAMP_Y, "r": 0.5, "z": D + 0.56, "colour": list(colour), "ch": ch})
    for x, label_ in GD_TESTS:
        T(x, GD_TEST_Y + 1.5, f"{label_} TEST", 7, bold=True)
    T(7.0, GD_TEST_Y + 0.35, "GROUND TEST", 7, bold=True)
    T(7.0, GD_TEST_Y - 0.35, "PUSH AND HOLD", 6, bold=True)
    f.R(2.0, 1.6, 14.0, 3.6, fill=(22, 22, 24, 255))
    return f


def ground_detector_boxes():
    """The whole ground detector (both blocks): the cabinet (its conduit's hub on its top), the nameplate, the screen's bezel and glass, the keys, the lamps, the voltmeters, the test buttons in their guard rings."""
    F = f"pi_{GD_KEY}_f"
    D = SC_DOOR
    face = {"*": "pi_pnl_black", "south": F}
    blk = "pi_pnl_black"
    b = _cabinet_shell(F, hub="one")
    b.append((2.0, 28.3, D, 14.0, 29.9, D + 0.12, face))
    x0, y0, x1, y1 = GD_SCREEN
    t = GD_FRAME
    b += [(x0, y0, D, x0 + t, y1, D + 0.4, face), (x1 - t, y0, D, x1, y1, D + 0.4, face),
          (x0 + t, y0, D, x1 - t, y0 + t, D + 0.4, face), (x0 + t, y1 - t, D, x1 - t, y1, D + 0.4, face),
          (x0 + t, y0 + t, D, x1 - t, y1 - t, D + 0.1, face)]
    for x, _ in GD_KEYS:
        b += [(x - 0.45, GD_KEY_Y - 0.45, D, x + 0.45, GD_KEY_Y + 0.45, D + 0.12, blk),
              (x - 0.32, GD_KEY_Y - 0.32, D + 0.12, x + 0.32, GD_KEY_Y + 0.32, D + 0.4, "pi_cap_black")]
    b += _sc_round(GD_ALARM, GD_KEY_Y, D, D + 0.3, 0.7, "pi_chrome") + _sc_round(GD_ALARM, GD_KEY_Y, D + 0.3, D + 0.5, 0.5, "pi_lens_amber")
    w, my0, my1 = GD_METER
    for mx0, *_ in GD_METERS:
        b += _meter_case(mx0, my0, mx0 + w, my1, face)
    for x, _, _, _, lens in GD_LAMPS:
        b += _sc_round(x, GD_LAMP_Y, D, D + 0.3, 0.7, "pi_chrome") + _sc_round(x, GD_LAMP_Y, D + 0.3, D + 0.5, 0.5, lens)
    for x, _ in GD_TESTS:
        b += _sc_round(x, GD_TEST_Y, D, D + 0.25, 0.95, "pi_chrome") + _sc_round(x, GD_TEST_Y, D + 0.25, D + 0.7, 0.62, "pi_pnl_yellow")
    return b


# ------------------------------------------------------------------------------------------------ the regulating transformer

# Each vital inverter's standby source (round 26, 8d; Justin: "Beside their inverters", "Read the MCC breaker", "Meters
# and a lamp", "Overhead tray now"): a 10 kVA constant-voltage (ferroresonant) regulating transformer, 480 V single
# phase in from its MCC's REG XF breaker, a steady 120 V out to its inverter's static switch and manual bypass. It
# stands in the lineup beside its inverter, the same cabinet (plinth, cap, grey) but a transformer's: no door, a
# bolted front cover with two lifting handles, a big louvred air inlet low down, louvred vents in both sides up high
# and two on the top, the hub its feeder conduit comes down into from the overhead tray. Its cover: the engraved
# nameplate; INPUT VOLTS, OUTPUT VOLTS and OUTPUT AMPS behind glass; the winding temperature gauge; the POWER ON lamp;
# a FED FROM tag; the DANGER 480 VOLTS sign and the riveted data plate. All of it moves with its MCC breaker and its
# inverter (client/EdgTelemetry.java transformer): 480 V in and 120 V out when the breaker is closed and the MCC's bus
# alive, no load until its inverter goes to bypass, the windings warming up and cooling down slowly.
RX_KEY = "reg_transformer"
RX_METERS = [(1.15, "INPUT VOLTS", (0, 600), "V", "xf_in_v"), (5.85, "OUTPUT VOLTS", (0, 150), "V", "xf_out_v"),
             (10.55, "OUTPUT AMPS", (0, 100), "A", "xf_out_a")]
RX_DIAL = (3.6, 19.75, 1.7)                  # the winding temperature gauge: centre, radius
RX_LAMP = (8.3, 20.4)                        # POWER ON
RX_TAG = (10.2, 18.3, 14.9, 21.5)            # FED FROM
RX_SIGN = (1.2, 11.7, 7.0, 16.7)             # DANGER 480 VOLTS
RX_DATA = (8.0, 11.7, 14.8, 16.7)            # the data plate
RX_LOUVRE = (2.0, 2.6, 14.0, 10.0)           # the air inlet
RX_HANDLES = [(2.6, 5.6), (10.4, 13.4)]
RX_HANDLE_Y = (10.75, 11.25)
RX_BOLTS = [(0.95, 1.95), (8.0, 1.95), (15.05, 1.95), (0.95, 6.3), (15.05, 6.3), (0.95, 14.5), (15.05, 14.5), (0.95, 19.4),
            (15.05, 19.4), (0.95, 29.6), (15.05, 29.6)]
RX_SIDE_VENT = (21.6, 28.0, 4.2, 11.8)       # each side's vent: y0, y1, z0, z1


def _rx_feed(label):
    """Where it is fed from: train 1A's MCC 1A, REG XF 1 for transformer 1A-1."""
    train, number = label.split("-")
    return f"MCC {train}", f"REG XF {number}"


def reg_transformer(label="1A-1"):
    """The transformer's cover, drawn (the meter cases, glass, gauge bezel, lamp, plates, handles and louvres are
    real: reg_transformer_boxes)."""
    f = Front(1, 560)
    T = f.T
    WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)
    D = SC_DOOR
    # The cover's edge, the nameplate.
    f.R(0.6, 1.4, 15.4, 30.2, outline=(130, 132, 128, 255), width=3)
    f.R(2.0, 28.3, 14.0, 29.9, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    T(8.0, 29.4, f"REGULATING XFMR {label}", 11, fill=WHITE, bold=True)
    T(8.0, 28.75, "480 V 1 PH IN    120 V 60 HZ 10 KVA OUT", 7, fill=WHITE, bold=True)
    w, y0, y1 = VI_METER
    for x0, name, scale, unit, ch in RX_METERS:
        f.meter(x0, y0, x0 + w, y1, name, scale, 0, unit, live=ch)
        f.live[-1]["z"] = D + 0.3
    # The winding temperature gauge: its face (the bezel is real), the red band from 130 C, the pointer live.
    cx, cy, r = RX_DIAL
    f.E(cx, cy, r, fill=(246, 246, 240, 255))
    lo, hi, start, sweep = 0, 150, 225, 270
    for k in range(16):
        a = math.radians(start - sweep * k / 15)
        r0 = r * (0.72 if k % 5 == 0 else 0.8)
        f.line([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * r * 0.93, cy + math.sin(a) * r * 0.93)],
               INK, 3 if k % 5 == 0 else 2)
    for k in range(14):
        a = math.radians(start - sweep * (130 + 20 * k / 13) / 150)
        f.E(cx + math.cos(a) * r * 0.86, cy + math.sin(a) * r * 0.86, r * 0.07, fill=(200, 40, 34, 255))
    for v in (0, 50, 100, 150):
        a = math.radians(start - sweep * v / 150)
        T(cx + math.cos(a) * r * 0.52, cy + math.sin(a) * r * 0.52, f"{v}", 7, bold=True)
    T(cx, cy + r * 0.4, "\u00b0C", 7, bold=True)
    T(cx, cy - r - 0.75, "WINDING TEMP", 7, bold=True)
    f.live.append({"t": "dial", "x": cx, "y": cy, "r": r, "z": D + 0.4, "lo": lo, "hi": hi, "start": start, "sweep": sweep,
                   "ch": "xf_temp"})
    # POWER ON (live), its legend.
    lx, ly = RX_LAMP
    T(lx, ly - 1.25, "POWER ON", 7, bold=True)
    f.live.append({"t": "lamp", "x": lx, "y": ly, "r": 0.5, "z": D + 0.56, "colour": [240, 240, 230], "ch": "xf_on"})
    # FED FROM: an engraved tag, black with white letters.
    tx0, ty0, tx1, ty1 = RX_TAG
    f.R(tx0, ty0, tx1, ty1, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    mcc, breaker = _rx_feed(label)
    for k, line in enumerate(("FED FROM", mcc, breaker)):
        T((tx0 + tx1) / 2, ty1 - 0.68 - k * 0.92, line, 8 if k else 7, fill=WHITE, bold=True)
    # DANGER 480 VOLTS: the red oval on its black band, the white body, the black legend.
    sx0, sy0, sx1, sy1 = RX_SIGN
    f.R(sx0, sy0, sx1, sy1, fill=(246, 246, 240, 255), outline=(40, 40, 42, 255), width=2)
    f.R(sx0 + 0.15, sy1 - 1.65, sx1 - 0.15, sy1 - 0.15, fill=BLACK)
    f.d.ellipse([(sx0 + 0.55) * U, (32 - (sy1 - 0.3)) * U, (sx1 - 0.55) * U, (32 - (sy1 - 1.5)) * U], fill=(206, 32, 30, 255),
                outline=(246, 246, 240, 255), width=3)
    T((sx0 + sx1) / 2, sy1 - 0.9, "DANGER", 11, fill=(246, 246, 240, 255), bold=True)
    T((sx0 + sx1) / 2, sy0 + 2.15, "480 VOLTS", 12, fill=BLACK, bold=True)
    T((sx0 + sx1) / 2, sy0 + 0.95, "KEEP COVERS ON", 7, fill=BLACK, bold=True)
    # The data plate: brushed aluminium, stamped.
    dx0, dy0, dx1, dy1 = RX_DATA
    f.R(dx0, dy0, dx1, dy1, fill=(200, 202, 200, 255), outline=(120, 122, 124, 255), width=2)
    rows = ["CONSTANT VOLTAGE XFMR", "KVA 10    PHASE 1    HZ 60", "PRI 480 V    20.8 A", "SEC 120 V    83.3 A",
            "INSULATION CLASS H  RISE 115 C", "SERIAL 26-4471-" + label.replace("-", "")]
    for k, line in enumerate(rows):
        T((dx0 + dx1) / 2, dy1 - 0.75 - k * 0.7, line, 6, fill=(30, 30, 32, 255), bold=k == 0)
    # The air inlet's opening: dark behind the slats.
    lx0, ly0, lx1, ly1 = RX_LOUVRE
    f.R(lx0, ly0, lx1, ly1, fill=(22, 22, 24, 255))
    return f


def _rx_bolt(x, y, z):
    """A hex bolt head standing out of the cover: a square and the same square turned 45 degrees, a little shallower."""
    return [(x - 0.26, y - 0.26, z, x + 0.26, y + 0.26, z + 0.2, "pi_steel"),
            (x - 0.26, y - 0.26, z, x + 0.26, y + 0.26, z + 0.14, "pi_steel", {"rotation": {"angle": 45, "axis": "z", "origin": [x, y, z]}})]


def reg_transformer_boxes():
    F = f"pi_{RX_KEY}_f"
    D = SC_DOOR
    face = {"*": "pi_pnl_black", "south": F}
    blk, dark = "pi_pnl_black", "pi_cabinet_dark"
    # The plinth, the body, the cover (its face the drawing), the cap.
    b = [(0.8, 0, 2.6, 15.2, 1.2, 14.0, "pi_toe_kick"),
         (0.4, 1.2, 1.6, 15.6, 30.4, 14.6, "pi_cabinet"),
         (0.6, 1.4, 14.6, 15.4, 30.2, D, {"*": "pi_cabinet", "south": F}),
         (0.2, 30.4, 1.4, 15.8, 31.2, 15.2, dark)]
    # On top: two louvred vents either side of the feeder conduit's hub (the conduit itself is the plant's, up to the
    # tray: plant_office2 tray_* pieces), its locknut and the conduit's stub.
    for z0, z1 in ((2.6, 5.8), (10.2, 13.4)):
        b += [(2.0, 31.2, z0, 14.0, 31.3, z1, blk),
              (2.0, 31.3, z0, 14.0, 31.8, z0 + 0.2, dark), (2.0, 31.3, z1 - 0.2, 14.0, 31.8, z1, dark),
              (2.0, 31.3, z0 + 0.2, 2.2, 31.8, z1 - 0.2, dark), (13.8, 31.3, z0 + 0.2, 14.0, 31.8, z1 - 0.2, dark)]
        for k in range(5):
            zc = z0 + 0.55 + k * 0.52
            b.append((2.2, 31.5 - 0.05, zc - 0.17, 13.8, 31.5 + 0.05, zc + 0.17, dark,
                      {"rotation": {"angle": 22.5, "axis": "x", "origin": [8.0, 31.5, zc]}}))
    # (The stub is square, as the conduit above it is at its foot: a turned stub would share the turned conduit's faces.)
    b += [(6.5, 31.2, 6.5, 9.5, 31.5, 9.5, "pi_steel_dark"),
          (6.5, 31.2, 6.5, 9.5, 31.45, 9.5, "pi_steel_dark", {"rotation": {"angle": 45, "axis": "y", "origin": [8, 31.2, 8]}}),
          (7.0, 31.4, 7.0, 9.0, 32.0, 9.0, "pi_chrome")]
    # Each side's vent high up: the dark opening, the slats tilted down to the outside.
    y0, y1, z0, z1 = RX_SIDE_VENT
    for x_in, x_out, sgn in ((0.4, 0.3, -1), (15.6, 15.7, 1)):
        lo, hi = min(x_in, x_out), max(x_in, x_out)
        b.append((lo, y0, z0, hi, y1, z1, blk))
        fx0, fx1 = (0.1, 0.3) if sgn < 0 else (15.7, 15.9)
        b += [(fx0, y0 - 0.2, z0 - 0.2, fx1, y0, z1 + 0.2, dark), (fx0, y1, z0 - 0.2, fx1, y1 + 0.2, z1 + 0.2, dark),
              (fx0, y0, z0 - 0.2, fx1, y1, z0, dark), (fx0, y0, z1, fx1, y1, z1 + 0.2, dark)]
        for k in range(9):
            yc = y0 + 0.4 + k * 0.7
            xm = (fx0 + fx1) / 2
            b.append((xm - 0.22, yc - 0.05, z0, xm + 0.22, yc + 0.05, z1, dark,
                      {"rotation": {"angle": -22.5 * sgn, "axis": "z", "origin": [xm, yc, (z0 + z1) / 2]}}))
    # The nameplate; the meters.
    b.append((2.0, 28.3, D, 14.0, 29.9, D + 0.12, face))
    w, my0, my1 = VI_METER
    for x0, *_ in RX_METERS:
        b += _meter_case(x0, my0, x0 + w, my1, face)
    # The winding temperature gauge: a chrome back plate, the bezel's ring standing out of it, the face set back inside
    # the ring, the glass across the ring's mouth (the pointer is live, between the face and the glass). Ring, face
    # and glass share their columns, so the ring's inside is the face's edge.
    cx, cy, r = RX_DIAL
    b += _rx_disc(cx, cy, r + 0.35, D, D + 0.15, "pi_chrome", r + 0.35)
    b += _rx_ring(cx, cy, r, r + 0.35, D + 0.15, D + 0.62, "pi_chrome")
    b += _rx_disc(cx, cy, r, D + 0.15, D + 0.3, face, r + 0.35)
    b += _rx_disc(cx, cy, r, D + 0.55, D + 0.58, {"south": SC_GLASS, "north": SC_GLASS}, r + 0.35)
    # POWER ON: a chrome bezel and the white lens.
    lx, ly = RX_LAMP
    b += _sc_round(lx, ly, D, D + 0.3, 0.7, "pi_chrome") + _sc_round(lx, ly, D + 0.3, D + 0.5, 0.5, "pi_lens_white")
    # The FED FROM tag, the danger sign and the data plate stand off the cover, each riveted at its corners.
    for x0, y0_, x1, y1_ in (RX_TAG, RX_SIGN, RX_DATA):
        side = "pi_pnl_black" if (x0, y0_) == RX_TAG[:2] else ("pi_pnl_white" if (x0, y0_) == RX_SIGN[:2] else "pi_chrome")
        b.append((x0, y0_, D, x1, y1_, D + 0.12, {"*": side, "south": F}))
        for rx, ry in ((x0 + 0.3, y0_ + 0.3), (x1 - 0.3, y0_ + 0.3), (x0 + 0.3, y1_ - 0.3), (x1 - 0.3, y1_ - 0.3)):
            if (x0, y0_) == RX_SIGN[:2] and ry > y0_ + 1:
                continue    # the sign's top corners are in its black band: riveted lower down only
            b.append((rx - 0.13, ry - 0.13, D + 0.12, rx + 0.13, ry + 0.13, D + 0.2, "pi_chrome"))
    # The lifting handles: a bar on two standoffs.
    hy0, hy1 = RX_HANDLE_Y
    for x0, x1 in RX_HANDLES:
        b += [(x0, hy0, D, x0 + 0.5, hy1, D + 0.8, "pi_chrome"), (x1 - 0.5, hy0, D, x1, hy1, D + 0.8, "pi_chrome"),
              (x0, hy0 + 0.05, D + 0.8, x1, hy1 - 0.05, D + 1.15, "pi_chrome")]
    # The cover's bolts.
    for x, y in RX_BOLTS:
        b += _rx_bolt(x, y, D)
    # The air inlet: its frame, the slats tilted down to the front.
    lx0, ly0, lx1, ly1 = RX_LOUVRE
    b += [(lx0 - 0.2, ly0 - 0.2, D, lx1 + 0.2, ly0, D + 0.3, dark), (lx0 - 0.2, ly1, D, lx1 + 0.2, ly1 + 0.2, D + 0.3, dark),
          (lx0 - 0.2, ly0, D, lx0, ly1, D + 0.3, dark), (lx1, ly0, D, lx1 + 0.2, ly1, D + 0.3, dark)]
    k = 0
    while ly0 + 0.35 + k * 0.52 < ly1 - 0.15:
        yc = ly0 + 0.35 + k * 0.52
        b.append((lx0, yc - 0.06, D - 0.1, lx1, yc + 0.06, D + 0.4, dark,
                  {"rotation": {"angle": -22.5, "axis": "x", "origin": [8.0, yc, D + 0.15]}}))
        k += 1
    return b


RX_COLUMNS = 11


def _rx_columns(r_out):
    """The columns a round part is built of, across its outer radius: (x0, x1, middle) offsets from the centre."""
    w = 2 * r_out / RX_COLUMNS
    return [(-r_out + k * w, -r_out + (k + 1) * w, -r_out + (k + 0.5) * w) for k in range(RX_COLUMNS)]


def _rx_half(r, mid, r_out):
    """A circle of radius r's half height over a column (its middle at mid), the outermost columns a little fuller."""
    m = min(abs(mid), r_out - r_out / RX_COLUMNS * 0.5)
    return math.sqrt(r * r - m * m) if m < r else 0.0


def _rx_disc(cx, cy, r, z0, z1, tex, r_out):
    """A disc of radius r facing out of the cover, as touching columns laid out over radius r_out (tools/zfight.py)."""
    out = []
    for x0, x1, mid in _rx_columns(r_out):
        h = _rx_half(r, mid, r_out)
        if h > 0.05:
            out.append((cx + max(x0, -r), cy - h, z0, cx + min(x1, r), cy + h, z1, tex))
    return out


def _rx_ring(cx, cy, r0, r1, z0, z1, tex):
    """A ring between radius r0 and r1 in the same columns as _rx_disc(r0), so its inside is the disc's edge."""
    out = []
    for x0, x1, mid in _rx_columns(r1):
        ho, hi = _rx_half(r1, mid, r1), _rx_half(r0, mid, r1)
        if hi <= 0.05:
            out.append((cx + x0, cy - ho, z0, cx + x1, cy + ho, z1, tex))
            continue
        xa, xb = cx + x0, cx + x1
        out += [(xa, cy + hi, z0, xb, cy + ho, z1, tex), (xa, cy - ho, z0, xb, cy - hi, z1, tex)]
        # Where the disc is narrower than its column (its outermost), the ring fills the column's rest.
        if -r0 > x0:
            out.append((xa, cy - hi, z0, cx - r0, cy + hi, z1, tex))
        if r0 < x1:
            out.append((cx + r0, cy - hi, z0, xb, cy + hi, z1, tex))
    return out


# ------------------------------------------------------------------------------------------------ the vital panel

# Each vital inverter's 120 VAC vital instrument panel (round 26, 8e; Justin: "Side wall, under the tray", "Switchable
# breakers", "Door that opens"): a surface-mounted panelboard hung on the DC room's side wall, its feeder conduit up
# the wall into the tray from its top. Closed: the hinged door with its latch, the circuit directory in a clear holder
# and a WARNING 120 VAC label; over the door the engraved nameplate and the POWER ON lamp (live: its inverter's output
# alive and the main closed). Open (the block's "open" state, its door swung out on its hinges): the dead front, the
# two-pole main breaker at the top and twelve branch breakers in two columns, odd on the left and even on the right as
# in any panelboard, each with its name beside it; their handles are the game's (live: ON toward the middle, OFF
# outward; the main up ON). Each closed breaker carries its share of the inverter's load (nuclear/VitalPanel.java).
# Model pixels, the piece facing north: the wall behind it at z 0, its front out at z 5.3.
VP_KEY = "vital_panel"
VP_BOX = (2.5, 8.4, 13.5, 31.6)              # the enclosure: x0, y0, x1, y1
VP_FRONT, VP_DOOR = 5.3, 5.6                 # the enclosure's front, the closed door's face
VP_OPENING = (2.8, 8.7, 13.2, 28.3)          # the door's opening
VP_DOORLEAF = (2.85, 8.75, 13.15, 28.25)     # the door leaf
VP_HINGE = (2.85, VP_DOOR)                   # its hinge line (x, z): it swings out to the left
VP_DEAD = (4.4, 4.8)                         # the dead front (z)
VP_LAMP = (12.1, 30.15)
VP_PLATE = (3.1, 28.75, 10.9, 31.0)
VP_CARD = (3.9, 12.6, 11.3, 26.8)            # the directory's clear holder on the door
VP_LATCH = (12.0, 17.2, 12.6, 20.4)
VP_WARN = (5.2, 9.3, 10.8, 11.6)
VP_MAIN = (6.3, 24.5, 9.7, 27.3)             # the main breaker's body
VP_ROWS = [22.7 - i * 2.15 for i in range(6)]
VP_BODY = ((6.4, 7.95), (8.05, 9.6))         # the branch breakers' bodies, left and right column (x)
VP_HANDLE = (0.5, 0.55, 0.3)                 # a branch handle: x, y, z size
VP_CHANNELS = {"1A-1": ("I", "N41"), "1A-2": ("III", "N43"), "1B-1": ("II", "N42"), "1B-2": ("IV", "N44")}


def vp_circuits(label):
    """The twelve branch circuits of panel label: (name's two lines), circuit k + 1 (odd on the left)."""
    ch, nis = VP_CHANNELS[label]
    return [("NIS PWR RANGE", nis), ("PROCESS PROT", f"RACK {ch}"), ("SSPS INPUT", f"CH {ch}"), ("SG LEVEL", f"XMTRS CH {ch}"),
            ("PZR PRESS/LVL", f"CH {ch}"), ("RCS FLOW", f"XMTRS CH {ch}"), ("STM GEN PRESS", f"CH {ch}"), ("RVLIS / ICCM", f"CH {ch}"),
            ("PAM INDICATION", f"CH {ch}"), ("AFW FLOW IND", f"CH {ch}"), ("SPARE", ""), ("SPARE", "")]


def vp_short(label):
    """Each branch circuit's short name, as marked beside its breaker (the directory has them in full)."""
    ch, nis = VP_CHANNELS[label]
    return [f"NIS {nis}", f"PROT RACK {ch}", f"SSPS CH {ch}", f"SG LVL CH {ch}", f"PZR P/L CH {ch}", f"RCS FLOW {ch}", f"SG PRESS {ch}",
            f"RVLIS CH {ch}", f"PAM CH {ch}", f"AFW FLOW {ch}", "SPARE", "SPARE"]


def vp_breaker(k):
    """Branch breaker k (0..11, circuit k + 1): its column (0 left, 1 right), its row's middle (y)."""
    return k % 2, VP_ROWS[k // 2]


def vp_handles():
    """Each breaker's handle, ON and OFF: [(on box, off box)], the main first (x0, y0, z0, x1, y1, z1)."""
    z0 = VP_DEAD[1] + 0.35
    mx = (VP_MAIN[0] + VP_MAIN[2]) / 2
    out = [((mx - 0.45, 25.95, z0, mx + 0.45, 26.95, z0 + 0.4), (mx - 0.45, 24.85, z0, mx + 0.45, 25.85, z0 + 0.4))]
    hx, hy, hz = VP_HANDLE
    for k in range(12):
        col, y = vp_breaker(k)
        bx0, bx1 = VP_BODY[col]
        inner, outer = (bx1 - 0.15 - hx, bx0 + 0.15) if col == 0 else (bx0 + 0.15, bx1 - 0.15 - hx)
        out.append(((inner, y - hy / 2, z0, inner + hx, y + hy / 2, z0 + hz), (outer, y - hy / 2, z0, outer + hx, y + hy / 2, z0 + hz)))
    return out


def vital_panel(label="1A-1"):
    """The closed panel's front, drawn: the nameplate strip over the door, the door with its directory card and
    warning label. Its live parts: the POWER ON lamp, and (drawn only with the door open) the breakers' handles."""
    f = Front(1, 580)
    T = f.T
    WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)
    ch, _ = VP_CHANNELS[label]
    x0, y0, x1, y1 = VP_BOX
    # The door's seam round it, the strip over it.
    dx0, dy0, dx1, dy1 = VP_DOORLEAF
    f.R(dx0, dy0, dx1, dy1, outline=(120, 122, 118, 255), width=2)
    # The nameplate: engraved black, white letters.
    px0, py0, px1, py1 = VP_PLATE
    f.R(px0, py0, px1, py1, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    T((px0 + px1) / 2, py1 - 0.7, f"VITAL INST PANEL {label}", 9, fill=WHITE, bold=True)
    T((px0 + px1) / 2, py0 + 0.6, f"120 VAC 1 PH   CH {ch}   FROM INV {label}", 6, fill=WHITE, bold=True)
    lx, ly = VP_LAMP
    T(lx, ly - 1.05, "POWER ON", 6, bold=True)
    f.live.append({"t": "lamp", "x": lx, "y": ly, "r": 0.4, "z": VP_FRONT + 0.56, "colour": [240, 240, 230], "ch": "vb_on"})
    # The circuit directory in its holder: typed on a white card.
    cx0, cy0, cx1, cy1 = VP_CARD
    f.R(cx0, cy0, cx1, cy1, fill=(246, 246, 240, 255), outline=(60, 60, 62, 255), width=2)
    T((cx0 + cx1) / 2, cy1 - 0.65, "CIRCUIT DIRECTORY", 7, bold=True)
    T((cx0 + cx1) / 2, cy1 - 1.35, f"VITAL PANEL {label}   CH {ch}", 6, bold=True)
    f.line([(cx0 + 0.3, cy1 - 1.8), (cx1 - 0.3, cy1 - 1.8)], INK, 2)
    T(cx0 + 0.4, cy1 - 2.35, "MAIN  60 A  2 POLE", 6, bold=True, anchor="lm")
    for k, (a, b) in enumerate(vp_circuits(label)):
        y = cy1 - 3.15 - k * 0.88
        T(cx0 + 0.4, y, f"{k + 1:>2}", 6, bold=True, anchor="lm")
        T(cx0 + 1.2, y, (a + " " + b).strip(), 6, bold=k < 10, anchor="lm")
        f.line([(cx0 + 0.3, y - 0.44), (cx1 - 0.3, y - 0.44)], (190, 196, 210, 255), 1)
    # The warning label.
    wx0, wy0, wx1, wy1 = VP_WARN
    f.R(wx0, wy0, wx1, wy1, fill=(244, 200, 30, 255), outline=(24, 24, 26, 255), width=2)
    f.R(wx0 + 0.15, wy1 - 0.95, wx1 - 0.15, wy1 - 0.15, fill=BLACK)
    T((wx0 + wx1) / 2, wy1 - 0.55, "WARNING", 7, fill=(244, 200, 30, 255), bold=True)
    T((wx0 + wx1) / 2, wy0 + 0.6, "120 VAC  KEEP CLOSED", 6, bold=True)
    # The breakers' handles (live, with the door open: "open").
    for i, (on, off) in enumerate(vp_handles()):
        f.live.append({"t": "toggle", "on": [round(v, 3) for v in on], "off": [round(v, 3) for v in off], "colour": [118, 120, 124],
                       "ch": f"vb_{i}", "open": True})
    return f


def vital_panel_interior(label="1A-1"):
    """The open panel's dead front, drawn: each breaker's name and number beside it, the main's legend, the gutters'
    lines; the breakers' bodies stand through it (vital_panel_boxes)."""
    f = Front(1, 581)
    T = f.T
    ox0, oy0, ox1, oy1 = VP_OPENING
    f.R(ox0, oy0, ox1, oy1, fill=(176, 178, 172, 255))
    # The dead front's plate edge and its screws' places; the breakers' slots.
    f.R(ox0 + 0.2, oy0 + 0.2, ox1 - 0.2, oy1 - 0.2, outline=(120, 122, 118, 255), width=2)
    f.R(VP_BODY[0][0] - 0.15, VP_ROWS[-1] - 1.1, VP_BODY[1][1] + 0.15, VP_ROWS[0] + 1.1, fill=(40, 40, 42, 255))
    f.R(VP_MAIN[0] - 0.15, VP_MAIN[1] - 0.15, VP_MAIN[2] + 0.15, VP_MAIN[3] + 0.15, fill=(40, 40, 42, 255))
    T(4.6, 26.4, "MAIN", 7, bold=True)
    T(4.6, 25.5, "60 A 2P", 6, bold=True)
    T(11.4, 26.4, "ON", 6, bold=True)
    T(11.4, 25.3, "OFF", 6, bold=True)
    for k, name in enumerate(vp_short(label)):
        col, y = vp_breaker(k)
        # The circuit's number by its breaker, its short name beyond it, the breaker's rating under the name.
        if col == 0:
            T(6.2, y + 0.25, f"{k + 1}", 7, bold=True, anchor="rm")
            T(5.4, y + 0.25, name, 6, bold=True, anchor="rm")
            T(5.4, y - 0.4, "20 A" if name != "SPARE" else "15 A", 6, anchor="rm")
        else:
            T(9.75, y + 0.25, f"{k + 1}", 7, bold=True, anchor="lm")
            T(10.65, y + 0.25, name, 6, bold=True, anchor="lm")
            T(10.65, y - 0.4, "20 A" if name != "SPARE" else "15 A", 6, anchor="lm")
    T(8.0, VP_ROWS[-1] - 1.6, "ON  → MIDDLE      OFF → OUT", 6, bold=True)
    return f


def vp_door_outside(label="1A-1"):
    """The open door's outside (its west face once swung): the closed front's door, moved along to where the leaf's
    face lies (z = x + the hinge's 2.75 px)."""
    src = front(VP_KEY, PANELS[VP_KEY]["labels"].index(label)).img
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    dx0, dy0, dx1, dy1 = VP_DOORLEAF
    shift = VP_HINGE[1] - VP_HINGE[0]
    region = src.crop((int(dx0 * U), int((32 - dy1) * U), int(dx1 * U), int((32 - dy0) * U)))
    out.paste(region, (int(round((dx0 + shift) * U)), int((32 - dy1) * U)))
    # Elsewhere the steel (never seen: the faces there are the leaf's edges).
    base = Front(1, 582).img
    return Image.alpha_composite(base, out)


def _vp_open_box(b):
    """A closed door's box swung open on its hinge: x along the door goes out along z, the door's outward z goes to -x;
    its faces turn with it (south to west, east to south, north to east, west to north)."""
    hx, hz = VP_HINGE
    x0, y0, z0, x1, y1, z1, tex = b[:7]
    turn = {"south": "west", "east": "south", "north": "east", "west": "north", "up": "up", "down": "down"}
    if isinstance(tex, dict):
        tex = {("*" if k == "*" else turn[k]): v for k, v in tex.items()}
    return (hx - (z1 - hz), y0, hz + (x0 - hx), hx - (z0 - hz), y1, hz + (x1 - hx), tex) + tuple(b[7:])


def _vp_door(open_):
    """The door leaf and what is on it: its hinges' knuckles, the latch and its key, the directory's clear holder, the
    warning label (closed: on the front; open: swung out)."""
    D = VP_DOOR
    F = f"pi_{VP_KEY}_f"
    O = f"pi_{VP_KEY}_o"
    dx0, dy0, dx1, dy1 = VP_DOORLEAF
    leaf = (dx0, dy0, VP_FRONT, dx1, dy1, D, {"*": "pi_cabinet", "south": O if open_ else F})
    on = [leaf]
    lx0, ly0, lx1, ly1 = VP_LATCH
    on += [(lx0, ly0, D, lx1, ly1, D + 0.15, "pi_pnl_black"), (lx0 + 0.15, ly0 + 0.3, D + 0.15, lx1 - 0.15, ly1 - 0.3, D + 0.45, "pi_chrome")]
    on += _sc_round(12.3, 16.4, D, D + 0.2, 0.42, "pi_chrome")
    cx0, cy0, cx1, cy1 = VP_CARD
    frame = "pi_pnl_black"
    on += [(cx0 - 0.25, cy0 - 0.25, D, cx1 + 0.25, cy0, D + 0.18, frame), (cx0 - 0.25, cy1, D, cx1 + 0.25, cy1 + 0.25, D + 0.18, frame),
           (cx0 - 0.25, cy0, D, cx0, cy1, D + 0.18, frame), (cx1, cy0, D, cx1 + 0.25, cy1, D + 0.18, frame),
           (cx0, cy0, D + 0.12, cx1, cy1, D + 0.14, {"south": SC_GLASS, "north": SC_GLASS})]
    wx0, wy0, wx1, wy1 = VP_WARN
    on.append((wx0, wy0, D, wx1, wy1, D + 0.06, {"*": "pi_pnl_yellow", "south": O if open_ else F}))
    if open_:
        on = [_vp_open_box(b) for b in on]
    # The hinges' knuckles stay on the hinge line.
    hx, hz = VP_HINGE
    for y in (11.0, 25.0):
        on.append((hx - 0.35, y, hz - 0.35, hx + 0.05, y + 1.6, hz + 0.05, "pi_chrome"))
    return on


def vital_panel_boxes(open_=False):
    """The panel, closed or open: the enclosure on the wall, the nameplate and the lamp over the door, the door; open,
    the dead front and the breakers' bodies behind it (their handles are the game's)."""
    F = f"pi_{VP_KEY}_f"
    I = f"pi_{VP_KEY}_i"
    face = {"*": "pi_pnl_black", "south": F}
    x0, y0, x1, y1 = VP_BOX
    ox0, oy0, ox1, oy1 = VP_OPENING
    cab = "pi_cabinet"
    b = [(x0, y0, 0, x1, y1, 0.4, cab),
         (x0, y0, 0.4, ox0, y1, VP_FRONT, cab), (ox1, y0, 0.4, x1, y1, VP_FRONT, cab),
         (ox0, oy1, 0.4, ox1, y1, VP_FRONT, {"*": cab, "south": F}), (ox0, y0, 0.4, ox1, oy0, VP_FRONT, cab)]
    # Its feeder conduit's hub on the top, at the wall (the conduit goes on up the wall: plant_office2 wall_conduit).
    b += [(7.0, y1, 1.6, 9.0, y1 + 0.3, 3.6, "pi_steel_dark"), (7.3, y1 + 0.3, 1.9, 8.7, 32.0, 3.3, "pi_chrome")]
    # The nameplate, the lamp in its bezel.
    px0, py0, px1, py1 = VP_PLATE
    b.append((px0, py0, VP_FRONT, px1, py1, VP_FRONT + 0.12, face))
    lx, ly = VP_LAMP
    b += _sc_round(lx, ly, VP_FRONT, VP_FRONT + 0.3, 0.6, "pi_chrome") + _sc_round(lx, ly, VP_FRONT + 0.3, VP_FRONT + 0.5, 0.42, "pi_lens_white")
    b += _vp_door(open_)
    if open_:
        dz0, dz1 = VP_DEAD
        b.append((ox0, oy0, dz0, ox1, oy1, dz1, {"*": cab, "south": I}))
        # Behind the dead front, the box's inside (dark).
        b.append((ox0, oy0, 0.4, ox1, oy1, 0.5, "pi_pnl_black"))
        mx0, my0, mx1, my1 = VP_MAIN
        b.append((mx0, my0, dz1, mx1, my1, dz1 + 0.35, "pi_pnl_black"))
        b.append((mx0 + 0.2, my0 + 0.25, dz1 + 0.35, mx1 - 0.2, my1 - 0.25, dz1 + 0.4, "pi_pnl_black"))
        for k in range(12):
            col, y = vp_breaker(k)
            bx0, bx1 = VP_BODY[col]
            b.append((bx0, y - 0.85, dz1, bx1, y + 0.85, dz1 + 0.35, "pi_pnl_black"))
    return b


def vital_panel_textures():
    """The open panel's drawings (dead front, the door's outside swung) for each label: pi_<stem>_i_<part>,
    pi_<stem>_o_<part>."""
    out = {}
    for v, label in enumerate(PANELS[VP_KEY]["labels"]):
        stem = texture_key(VP_KEY, v)
        inside, door = vital_panel_interior(label).img, vp_door_outside(label)
        for r in range(2):
            box = (0, (1 - r) * 16 * U, 16 * U, (2 - r) * 16 * U)
            out[f"pi_{stem}_i_{r}"] = (lambda img=inside.crop(box): img)
            out[f"pi_{stem}_o_{r}"] = (lambda img=door.crop(box): img)
    return out


SHARP |= {f"pi_{texture_key(VP_KEY, v)}_{k}_{r}" for v in range(len(PANELS[VP_KEY]["labels"])) for k in "io" for r in range(2)}


# ------------------------------------------------------------------------------------------------ the battery rooms

# The battery rooms' fittings (round 26, #9; Justin: "Eyewash and spill kit", "Hydrogen monitor and fan", "Warning
# signs"): wall pieces facing north (the wall behind them at z 0, their front +z). Their lettering is drawn at 512 px a
# block so it reads (SHARP).

def _oct_prism_z(cx, cy, a, z0, z1, tex):
    """A regular octagon across x-y (a: its inradius), from z0 to z1: two crossing boxes and the same pair turned 45
    degrees about z (a hair inside them front and back, so no faces share a plane)."""
    s = a * math.tan(math.radians(22.5))
    rot = {"rotation": {"angle": 45, "axis": "z", "origin": [cx, cy, z0]}}
    return [(cx - a, cy - s, z0, cx + a, cy + s, z1, tex), (cx - s, cy - a, z0 + 0.03, cx + s, cy + a, z1 - 0.03, tex),
            (cx - a, cy - s, z0 + 0.06, cx + a, cy + s, z1 - 0.06, tex, rot), (cx - s, cy - a, z0 + 0.09, cx + s, cy + a, z1 - 0.09, tex, rot)]


def _oct_ring_z(cx, cy, a_in, a_out, z0, z1, tex):
    """A ring of eight flat bars round an octagon (inradius a_in to a_out) across x-y, from z0 to z1."""
    half = a_out * math.tan(math.radians(22.5)) + 0.04
    out = [(cx + a_in, cy - half, z0, cx + a_out, cy + half, z1, tex), (cx - a_out, cy - half, z0, cx - a_in, cy + half, z1, tex),
           (cx - half, cy + a_in, z0, cx + half, cy + a_out, z1, tex), (cx - half, cy - a_out, z0, cx + half, cy - a_in, z1, tex)]
    for ang in (45, -45):
        rot = {"rotation": {"angle": ang, "axis": "z", "origin": [cx, cy, z0]}}
        out += [(cx + a_in, cy - half, z0 + 0.03, cx + a_out, cy + half, z1 - 0.03, tex, rot),
                (cx - a_out, cy - half, z0 + 0.03, cx - a_in, cy + half, z1 - 0.03, tex, rot)]
    return out


# ---- the signs: a plate on four stand-off screws, its picture a region of a 512 px drawing (SIGN_UV: 11 by 13).
SIGN_PLATE = (2.5, 1.5, 13.5, 14.5)
SIGN_UV = [0, 0, round(16 * 11 / 13, 4), 16]
SIGNS = {"sign_no_smoking": "No Smoking Sign", "sign_battery_acid": "Battery Acid Danger Sign", "sign_hydrogen": "Hydrogen Warning Sign",
         "sign_eyewash": "Emergency Eyewash Sign"}


def _sign_boxes(tex):
    x0, y0, x1, y1 = SIGN_PLATE
    out = [(x0, y0, 0.15, x1, y1, 0.35, {"*": "pi_pnl_white", "south": (tex, SIGN_UV)})]
    for x, y in ((x0 + 0.6, y0 + 0.6), (x1 - 0.6, y0 + 0.6), (x0 + 0.6, y1 - 0.6), (x1 - 0.6, y1 - 0.6)):
        out += [(x - 0.25, y - 0.25, 0.0, x + 0.25, y + 0.25, 0.15, "pi_steel"), (x - 0.2, y - 0.2, 0.35, x + 0.2, y + 0.2, 0.45, "pi_steel")]
    return out


def _fit_line(d, xy, text, size, max_w, fill):
    """A line of bold condensed lettering centred at xy, its size brought down until it fits in max_w px."""
    while size > 12 and d.textlength(text, font=rfont(size, True)) > max_w:
        size -= 2
    d.text(xy, text, font=rfont(size, True), fill=fill, anchor="mm")


def _sign_image(kind):
    """A sign's picture: 433 by 512 px in a 512 px square (the rest is never shown)."""
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    F = lambda s: rfont(s, True)
    M = W - 56
    BLACK, WHITE = (20, 20, 22, 255), (250, 250, 246, 255)
    if kind == "sign_no_smoking":
        RED = (200, 30, 30, 255)
        d.rectangle([0, 0, W - 1, H - 1], fill=WHITE, outline=RED, width=14)
        cx, cy, r = W // 2, 168, 118
        # The cigarette and its smoke, the red ring and bar over them.
        d.rectangle([cx - 95, cy - 12, cx + 70, cy + 14], fill=BLACK)
        d.rectangle([cx + 70, cy - 12, cx + 95, cy + 14], fill=(230, 120, 40, 255))
        for k in range(3):
            d.arc([cx - 120 + k * 22, cy - 82, cx - 70 + k * 22, cy - 20], 200, 340, fill=BLACK, width=7)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=RED, width=26)
        d.line([(cx - 82, cy - 82), (cx + 82, cy + 82)], fill=RED, width=26)
        _fit_line(d, (cx, 352), "NO SMOKING", 66, M, BLACK)
        _fit_line(d, (cx, 420), "NO OPEN FLAMES", 42, M, RED)
        _fit_line(d, (cx, 466), "NO SPARKS", 42, M, RED)
    elif kind == "sign_battery_acid":
        d.rectangle([0, 0, W - 1, H - 1], fill=WHITE, outline=BLACK, width=10)
        d.rectangle([10, 10, W - 11, 132], fill=BLACK)
        d.ellipse([46, 26, W - 47, 116], fill=(200, 30, 30, 255), outline=WHITE, width=5)
        _fit_line(d, (W // 2, 71), "DANGER", 70, W - 140, WHITE)
        for y, line, size in ((196, "BATTERY ACID", 60), (270, "CORROSIVE", 52), (350, "WEAR FACE SHIELD,", 40), (408, "APRON AND GLOVES", 40)):
            _fit_line(d, (W // 2, y), line, size, M, BLACK)
    elif kind == "sign_hydrogen":
        ORANGE = (244, 120, 26, 255)
        d.rectangle([0, 0, W - 1, H - 1], fill=WHITE, outline=BLACK, width=10)
        d.rectangle([10, 10, W - 11, 132], fill=ORANGE)
        d.polygon([(34, 108), (74, 34), (114, 108)], fill=BLACK)
        d.polygon([(50, 99), (74, 55), (98, 99)], fill=ORANGE)
        d.text((74, 88), "!", font=F(44), fill=BLACK, anchor="mm")
        _fit_line(d, (W // 2 + 50, 71), "WARNING", 64, W - 150, BLACK)
        for y, line, size in ((196, "HYDROGEN GAS", 58), (270, "FLAMMABLE", 52), (350, "NO SPARKS OR", 44), (414, "OPEN FLAMES", 44)):
            _fit_line(d, (W // 2, y), line, size, M, BLACK)
    elif kind == "sign_eyewash":
        GREEN = (0, 132, 72, 255)
        d.rectangle([0, 0, W - 1, H - 1], fill=GREEN, outline=WHITE, width=12)
        cx, cy = W // 2, 112
        # An eye, and the two jets of water sprayed up into it from the nozzles below, arcing in.
        d.ellipse([cx - 110, cy - 54, cx + 110, cy + 54], outline=WHITE, width=12)
        d.ellipse([cx - 36, cy - 36, cx + 36, cy + 36], fill=WHITE)
        for k in (-1, 1):
            pts = [(cx + k * (96 - 80 * t * t), cy + 188 - 120 * t) for t in [i / 12 for i in range(13)]]
            d.line(pts, fill=WHITE, width=10)
            d.rectangle([cx + k * 96 - 18, cy + 188, cx + k * 96 + 18, cy + 212], fill=WHITE)
        _fit_line(d, (cx, 382), "EMERGENCY", 66, M, WHITE)
        _fit_line(d, (cx, 452), "EYE WASH", 66, M, WHITE)
    return img


# ---- the eyewash: a plumbed wall-mounted eyewash station (ANSI Z358.1), its stainless bowl at waist height with two
# spray heads under yellow dust covers, fed from the wall through the stay-open valve its green push flag works, the
# bowl's drain back into the wall. Use it and it runs (nuclear/EyewashBlock.java).
EW_C, EW_A = (8.0, 7.4), 5.0                # the bowl's centre (x, z) and its inradius
EW_BOWL_Y = (11.0, 13.4)
EW_HEADS = (6.4, 9.6)                       # the spray heads (x)


def _oct_prism_y(cx, cz, a, y0, y1, tex):
    s = a * math.tan(math.radians(22.5))
    rot = {"rotation": {"angle": 45, "axis": "y", "origin": [cx, y0, cz]}}
    return [(cx - a, y0, cz - s, cx + a, y1, cz + s, tex), (cx - s, y0 + 0.03, cz - a, cx + s, y1 - 0.03, cz + a, tex),
            (cx - a, y0 + 0.06, cz - s, cx + a, y1 - 0.06, cz + s, tex, rot), (cx - s, y0 + 0.09, cz - a, cx + s, y1 - 0.09, cz + a, tex, rot)]


def _oct_ring_y(cx, cz, a_in, a_out, y0, y1, tex):
    half = a_out * math.tan(math.radians(22.5)) + 0.15
    out = [(cx + a_in, y0, cz - half, cx + a_out, y1, cz + half, tex), (cx - a_out, y0, cz - half, cx - a_in, y1, cz + half, tex),
           (cx - half, y0, cz + a_in, cx + half, y1, cz + a_out, tex), (cx - half, y0, cz - a_out, cx + half, y1, cz - a_in, tex)]
    for ang in (45, -45):
        rot = {"rotation": {"angle": ang, "axis": "y", "origin": [cx, y0, cz]}}
        out += [(cx + a_in, y0 + 0.03, cz - half, cx + a_out, y1 - 0.03, cz + half, tex, rot),
                (cx - a_out, y0 + 0.03, cz - half, cx - a_in, y1 - 0.03, cz + half, tex, rot)]
    return out


# Round parts standing up (round 26, #10) whose faces facing the same way are never within tools/zfight.py's reach of
# each other (0.1 px apart), so it never has to move them: an octagon (two crossing boxes, the same pair turned 45
# degrees, each a step of 0.1 shorter at both ends: taller than 0.7), an eight-pointed star (a square and the same
# turned, the second 0.1 shorter at both ends: a thin round part, a cable), and a ring of eight bars.
def round_y(cx, cz, a, y0, y1, tex):
    s = a * math.tan(math.radians(22.5))
    rot = {"rotation": {"angle": 45, "axis": "y", "origin": [cx, y0, cz]}}
    return [(cx - a, y0, cz - s, cx + a, y1, cz + s, tex), (cx - s, y0 + 0.1, cz - a, cx + s, y1 - 0.1, cz + a, tex),
            (cx - a, y0 + 0.2, cz - s, cx + a, y1 - 0.2, cz + s, tex, rot), (cx - s, y0 + 0.3, cz - a, cx + s, y1 - 0.3, cz + a, tex, rot)]


def star_y(cx, cz, r, y0, y1, tex, inset=0.1):
    rot = {"rotation": {"angle": 45, "axis": "y", "origin": [cx, y0, cz]}}
    return [(cx - r, y0, cz - r, cx + r, y1, cz + r, tex), (cx - r, y0 + inset, cz - r, cx + r, y1 - inset, cz + r, tex, rot)]


def ring_y(cx, cz, a_in, a_out, y0, y1, tex):
    half = a_out * math.tan(math.radians(22.5)) + 0.05
    out = [(cx + a_in, y0, cz - half, cx + a_out, y1, cz + half, tex), (cx - a_out, y0, cz - half, cx - a_in, y1, cz + half, tex),
           (cx - half, y0, cz + a_in, cx + half, y1, cz + a_out, tex), (cx - half, y0, cz - a_out, cx + half, y1, cz - a_in, tex)]
    rot = {"rotation": {"angle": 45, "axis": "y", "origin": [cx, y0, cz]}}
    out += [(cx + a_in, y0 + 0.1, cz - half, cx + a_out, y1 - 0.1, cz + half, tex, rot),
            (cx - a_out, y0 + 0.1, cz - half, cx - a_in, y1 - 0.1, cz + half, tex, rot),
            (cx - half, y0 + 0.1, cz + a_in, cx + half, y1 - 0.1, cz + a_out, tex, rot),
            (cx - half, y0 + 0.1, cz - a_out, cx + half, y1 - 0.1, cz - a_in, tex, rot)]
    return out


def eyewash_boxes():
    cx, cz = EW_C
    y0, y1 = EW_BOWL_Y
    SS = "pi_chrome"
    out = [(5.5, 4.6, 0.12, 10.5, 12.6, 0.6, SS),                                  # the wall plate
           (7.0, 9.6, 0.6, 9.0, 11.0, cz - EW_A + 0.4, SS)]                       # the bracket arm to the bowl
    # The bowl: its floor, its wall round it, the rolled rim; the drain in the middle.
    out += _oct_prism_y(cx, cz, EW_A - 0.3, y0, y0 + 0.5, SS)
    out += _oct_ring_y(cx, cz, EW_A - 0.4, EW_A, y0 + 0.5, y1, SS)
    out += _oct_ring_y(cx, cz, EW_A - 0.5, EW_A + 0.2, y1, y1 + 0.3, "pi_steel")
    out += [(cx - 0.5, y0 + 0.5, cz - 0.5, cx + 0.5, y0 + 0.55, cz + 0.5, "pi_pnl_black")]
    # The spray heads: a stem, the head, the yellow dust cover on it.
    for hx in EW_HEADS:
        out += [(hx - 0.3, y0 + 0.5, cz - 0.3, hx + 0.3, y0 + 1.6, cz + 0.3, SS),
                (hx - 0.6, y0 + 1.6, cz - 0.6, hx + 0.6, y0 + 2.0, cz + 0.6, SS),
                (hx - 0.7, y0 + 2.0, cz - 0.7, hx + 0.7, y0 + 2.35, cz + 0.7, "pi_pnl_yellow")]
    # The feed: from the wall (z 0) out to the valve under the bowl, up into the bowl; the valve body.
    out += [(9.0, 6.5, 0.6, 10.0, 7.5, 6.6, "pi_brass"), (6.9, 6.0, 6.4, 9.9, 8.2, 8.6, "pi_brass"),
            (cx - 0.45, 8.2, cz - 0.45, cx + 0.45, y0, cz + 0.45, SS)]
    # The push flag: its arm out of the valve, the green paddle (PUSH).
    out += [(7.8, 6.9, 8.6, 8.2, 7.3, 12.6, "pi_steel"),
            (6.2, 5.4, 12.6, 9.8, 9.0, 12.85, {"*": "pi_pnl_green", "south": "pi_eyewash_flag!"})]
    # The drain: down out of the bowl's floor at the back, along to the wall.
    out += [(5.2, 5.0, 3.8, 6.0, y0 + 0.05, 4.6, SS), (5.2, 5.0, 0.6, 6.0, 5.8, 3.8, SS)]
    return out


def _eyewash_flag():
    img = Image.new("RGBA", (256, 256), (0, 140, 76, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 251, 251], outline=(0, 100, 54, 255), width=8)
    d.text((128, 128), "PUSH", font=rfont(92, True), fill=(250, 250, 246, 255), anchor="mm")
    return img


# ---- the acid spill kit: a yellow steel cabinet on the wall (neutralizer, absorbent pillows, gloves, apron, goggles).
def spill_kit_boxes():
    out = [(2.0, 1.5, 0.2, 14.0, 14.5, 5.8, {"*": "pi_spill_yellow", "south": ("pi_spill_front", [0, 0, round(16 * 12 / 13, 4), 16])})]
    # Hinges on the left, the latch handle on the right, the base rail.
    for y in (3.0, 11.5):
        out.append((1.7, y, 5.2, 2.1, y + 1.4, 6.0, "pi_steel"))
    out += [(12.7, 6.4, 5.8, 13.3, 9.6, 6.15, "pi_pnl_black"), (12.8, 7.4, 6.15, 13.2, 9.2, 6.6, "pi_steel"),
            (2.0, 1.0, 0.2, 14.0, 1.5, 5.2, "pi_cabinet_dark")]
    return out


def _spill_front():
    W, H = 473, 512
    img = Image.new("RGBA", (512, 512), (246, 200, 26, 255))
    d = ImageDraw.Draw(img)
    F = lambda s: rfont(s, True)
    BLACK = (20, 20, 22, 255)
    d.rectangle([12, 12, W - 13, H - 13], outline=(170, 136, 14, 255), width=6)
    d.rectangle([40, 52, W - 41, 172], fill=BLACK)
    _fit_line(d, (W // 2, 112), "SPILL KIT", 84, W - 110, (246, 200, 26, 255))
    _fit_line(d, (W // 2, 226), "BATTERY ACID", 56, W - 130, BLACK)
    for k, line in enumerate(("NEUTRALIZER", "ABSORBENT PILLOWS", "GLOVES  APRON  GOGGLES")):
        _fit_line(d, (W // 2, 306 + k * 52), line, 40, W - 70, BLACK)
    _fit_line(d, (W // 2, 466), "BREAK SEAL TO OPEN", 30, W - 70, BLACK)
    return img


# ---- the hydrogen sensor head: high on the wall by the ceiling (hydrogen rises), its conduit down to the monitor; a
# conduit body with the sensor hanging under it, its sintered head.
def h2_sensor_boxes():
    z = 2.6
    out = [(7.3, 0, z - 0.7, 8.7, 6.4, z + 0.7, "pi_chrome"),
           (6.4, 6.4, 0.2, 12.4, 10.2, 3.6, {"*": "pi_cabinet", "south": "pi_h2_tag!"}),
           (6.6, 10.2, 0.4, 12.2, 10.5, 3.4, "pi_cabinet_dark")]
    for x in (6.9, 11.9):
        out.append((x - 0.2, 9.7, 3.6, x + 0.2, 10.0, 3.75, "pi_steel"))
    # The sensor under the box: its yellow housing, the sintered head at the bottom (round, as a square and the same
    # square turned).
    rot = lambda y: {"rotation": {"angle": 45, "axis": "y", "origin": [10.2, y, 2.0]}}
    out += [(9.4, 3.4, 1.2, 11.0, 6.4, 2.8, "pi_spill_yellow"), (9.4, 3.45, 1.2, 11.0, 6.35, 2.8, "pi_spill_yellow", rot(3.4)),
            (9.6, 2.4, 1.4, 10.8, 3.4, 2.6, "pi_steel_dark"), (9.6, 2.45, 1.4, 10.8, 3.35, 2.6, "pi_steel_dark", rot(2.4))]
    return out


def _h2_tag():
    img = Image.new("RGBA", (256, 256), (170, 174, 176, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([40, 70, 215, 186], fill=(236, 236, 230, 255), outline=(40, 40, 42, 255), width=3)
    d.text((128, 104), "H2", font=rfont(48, True), fill=(20, 20, 22, 255), anchor="mm")
    d.text((128, 154), "SENSOR", font=rfont(30, True), fill=(20, 20, 22, 255), anchor="mm")
    return img


RISERS.update({k: {"display": v, "boxes": _sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})
RISERS.update({
    "eyewash_station": {"display": "Emergency Eyewash Station", "boxes": eyewash_boxes()},
    "spill_kit": {"display": "Acid Spill Kit", "boxes": spill_kit_boxes()},
    "h2_sensor": {"display": "Hydrogen Sensor Head", "boxes": h2_sensor_boxes()},
})
CRAFTING.update({
    "sign_no_smoking": (["PRP", "PIP"], {"P": "minecraft:paper", "R": "minecraft:red_dye", "I": "minecraft:iron_nugget"}, 2),
    "sign_battery_acid": (["PBP", "PIP"], {"P": "minecraft:paper", "B": "minecraft:black_dye", "I": "minecraft:iron_nugget"}, 2),
    "sign_hydrogen": (["POP", "PIP"], {"P": "minecraft:paper", "O": "minecraft:orange_dye", "I": "minecraft:iron_nugget"}, 2),
    "sign_eyewash": (["PGP", "PIP"], {"P": "minecraft:paper", "G": "minecraft:green_dye", "I": "minecraft:iron_nugget"}, 2),
    "eyewash_station": (["IWI", " B ", " I "], {"I": "#c:plates/steel", "W": "minecraft:water_bucket", "B": "minecraft:copper_ingot"}, 1),
    "spill_kit": (["YPY", "YSY", "YYY"], {"Y": "minecraft:yellow_dye", "P": "#c:plates/steel", "S": "minecraft:sand"}, 1),
    "h2_sensor": (["P", "C", "R"], {"P": "#c:plates/steel", "C": "jugcraft:basic_circuit", "R": "minecraft:redstone"}, 1),
})


# ---- the hydrogen monitor: its transmitter on the wall at eye level, the conduit up from its top to the sensor head.
H2_KEY = "h2_monitor"
H2_BOX = (3.0, 2.0, 13.0, 14.6)             # the transmitter's enclosure (x0, y0, x1, y1)
H2_FRONT = 3.4
H2_DISPLAY = (4.4, 8.6, 11.6, 11.6)         # the display's bezel
H2_LAMPS = [(5.0, "NORMAL", (40, 180, 70), "h2_ok", "pi_lens_green"), (8.0, "WARN", (240, 170, 30), "h2_warn", "pi_lens_amber"),
            (11.0, "ALARM", (230, 40, 30), "h2_alarm", "pi_lens_red")]
H2_LAMP_Y = 6.4


def h2_monitor(label="1A"):
    f = Front(1, 590)
    T = f.T
    WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)
    x0, y0, x1, y1 = H2_BOX
    D = H2_FRONT
    f.R(x0, y1 - 1.9, x1, y1, fill=(30, 70, 140, 255))
    T((x0 + x1) / 2, y1 - 0.65, "HYDROGEN MONITOR", 9, fill=WHITE, bold=True)
    T((x0 + x1) / 2, y1 - 1.45, f"BATTERY ROOM {label}", 7, fill=WHITE, bold=True)
    bx0, by0, bx1, by1 = H2_DISPLAY
    f.R(bx0, by0, bx1, by1, fill=(30, 30, 32, 255))
    f.R(bx0 + 0.3, by0 + 0.3, bx1 - 0.3, by1 - 0.3, fill=(28, 6, 6, 255))
    T(bx1 - 0.55, (by0 + by1) / 2 - 0.45, "% LEL", 7, fill=(240, 90, 70, 255), bold=True, anchor="rm")
    f.text(bx1 - 2.5, (by0 + by1) / 2 + 0.2, D + 0.2, 1.2, (255, 70, 50), "h2_lel", "right")
    T((bx0 + bx1) / 2, by0 - 0.55, "H2 IN AIR", 7, bold=True)
    for x, label_, colour, ch, _ in H2_LAMPS:
        T(x, H2_LAMP_Y - 1.1, label_, 6, bold=True)
        f.live.append({"t": "lamp", "x": x, "y": H2_LAMP_Y, "r": 0.45, "z": D + 0.56, "colour": list(colour), "ch": ch})
    T((x0 + x1) / 2, 3.2, "WARN 10 %LEL   ALARM 25 %LEL", 6, bold=True)
    T((x0 + x1) / 2, 2.55, "FAN TO HIGH AT WARN", 6, bold=True)
    return f


def h2_monitor_boxes():
    F = f"pi_{H2_KEY}_f"
    x0, y0, x1, y1 = H2_BOX
    D = H2_FRONT
    face = {"*": "pi_cabinet", "south": F}
    b = [(x0, y0, 0.2, x1, y1, D, face), (x0 + 0.3, y1, 0.4, x1 - 0.3, y1 + 0.3, D - 0.2, "pi_cabinet_dark")]
    # The display's bezel round its window.
    bx0, by0, bx1, by1 = H2_DISPLAY
    blk = "pi_pnl_black"
    b += [(bx0, by0, D, bx0 + 0.3, by1, D + 0.3, blk), (bx1 - 0.3, by0, D, bx1, by1, D + 0.3, blk),
          (bx0 + 0.3, by0, D, bx1 - 0.3, by0 + 0.3, D + 0.3, blk), (bx0 + 0.3, by1 - 0.3, D, bx1 - 0.3, by1, D + 0.3, blk)]
    for x, _, _, _, lens in H2_LAMPS:
        b += _sc_round(x, H2_LAMP_Y, D, D + 0.3, 0.65, "pi_chrome") + _sc_round(x, H2_LAMP_Y, D + 0.3, D + 0.5, 0.45, lens)
    # Its mounting screws; the conduit up out of its top to the sensor head (as plant_office2's wall conduit).
    for x, y in ((x0 + 0.5, y0 + 0.5), (x1 - 0.5, y0 + 0.5), (x0 + 0.5, y1 - 2.4), (x1 - 0.5, y1 - 2.4)):
        b.append((x - 0.2, y - 0.2, D, x + 0.2, y + 0.2, D + 0.12, "pi_steel"))
    b += [(7.3, y1 + 0.3, 1.9, 8.7, 32, 3.3, "pi_chrome"), (7.0, y1 + 0.3, 1.6, 9.0, y1 + 1.0, 3.6, "pi_steel_dark")]
    return b


# ---- the exhaust fan: a wall fan over the room's hydrogen exhaust louvre, high up (its upper block on the louvre), its
# HAND-OFF-AUTO station under it. The blades turn (live): slowly on AUTO while the battery floats, fast while it
# charges hard or the hydrogen is up, fast on HAND, stopped on OFF; RUN and HIGH SPEED lamps.
EF_KEY = "exhaust_fan"
EF_C, EF_SHROUD = (8.0, 24.0), (5.25, 5.7)   # the fan's centre (x, y), the shroud's inradius in and out
EF_FRONT = 5.0
EF_BOX = (4.0, 1.6, 12.0, 13.4)             # the control station's enclosure
EF_SEL = (8.0, 8.2)                         # the selector
EF_LAMPS = [(5.6, "RUN", (40, 180, 70), "ef_run", "pi_lens_green"), (10.4, "HIGH", (240, 170, 30), "ef_high", "pi_lens_amber")]
EF_LAMP_Y = 4.6
EF_STATION_FRONT = 3.0


def exhaust_fan(label="1A"):
    f = Front(1, 595)
    T = f.T
    WHITE, BLACK = (238, 238, 232, 255), (24, 24, 26, 255)
    x0, y0, x1, y1 = EF_BOX
    D = EF_STATION_FRONT
    f.R(x0 + 0.4, y1 - 1.9, x1 - 0.4, y1 - 0.4, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    T((x0 + x1) / 2, y1 - 0.85, f"EXHAUST FAN EF-{label}", 8, fill=WHITE, bold=True)
    T((x0 + x1) / 2, y1 - 1.5, "BATTERY ROOM", 7, fill=WHITE, bold=True)
    sx, sy = EF_SEL
    f.E(sx, sy, 1.0, fill=(46, 46, 48, 255))
    for name, (dx, dy) in (("HAND", (-1.2, 0.0)), ("OFF", (0.0, 1.5)), ("AUTO", (1.2, 0.0))):
        T(sx + dx, sy + dy, name, 8, bold=True, anchor="rm" if dx < 0 else "lm" if dx > 0 else "mm")
    T(sx, sy - 1.6, "SELECTOR", 7, bold=True)
    for x, label_, colour, ch, _ in EF_LAMPS:
        T(x, EF_LAMP_Y - 1.1, label_, 8, bold=True)
        f.live.append({"t": "lamp", "x": x, "y": EF_LAMP_Y, "r": 0.42, "z": D + 0.56, "colour": list(colour), "ch": ch})
    # The selector's pointer, live: one of three white bars on the knob (left HAND, up OFF, right AUTO).
    for k, (bx0, by0, bx1, by1) in enumerate(((sx - 0.62, sy - 0.1, sx, sy + 0.1), (sx - 0.1, sy, sx + 0.1, sy + 0.62),
                                              (sx, sy - 0.1, sx + 0.62, sy + 0.1))):
        f.live.append({"t": "toggle", "on": [round(bx0, 3), round(by0, 3), D + 0.45, round(bx1, 3), round(by1, 3), D + 0.55], "off": [],
                       "colour": [236, 236, 230], "ch": f"ef_mode_{k}"})
    # The blades (live): four, turning in the shroud.
    f.live.append({"t": "fan", "x": EF_C[0], "y": EF_C[1], "z": 2.0, "r": 5.0, "blades": 4, "width": 2.2, "colour": [176, 180, 184],
                   "ch": "ef_spin"})
    return f


def exhaust_fan_boxes():
    F = f"pi_{EF_KEY}_f"
    cx, cy = EF_C
    a_in, a_out = EF_SHROUD
    D = EF_FRONT
    H = "pi_cabinet_dark"
    b = [(0.5, 30.9, 0.2, 15.5, 31.5, D, H), (0.5, 16.5, 0.2, 15.5, 17.1, D, H),
         (0.5, 17.1, 0.2, 1.1, 30.9, D, H), (14.9, 17.1, 0.2, 15.5, 30.9, D, H)]
    # The front plate round the shroud: four bands and the corners in to it.
    b += [(1.1, cy + a_out, D - 0.4, 14.9, 30.9, D, H), (1.1, 17.1, D - 0.4, 14.9, cy - a_out, D, H),
          (1.1, cy - a_out, D - 0.4, cx - a_out, cy + a_out, D, H), (cx + a_out, cy - a_out, D - 0.4, 14.9, cy + a_out, D, H)]
    # (Each corner in to the octagon: a box turned 45 degrees, from the octagon's face out to the square's corner.)
    reach, half = a_out * math.sqrt(2) + 0.05, a_out * math.tan(math.radians(22.5)) + 0.05
    for ang in (45, -45):
        rot = {"rotation": {"angle": ang, "axis": "z", "origin": [cx, cy, D - 0.45]}}
        b += [(cx + a_out - 0.2, cy - half, D - 0.45, cx + reach, cy + half, D - 0.05, H, rot),
              (cx - reach, cy - half, D - 0.45, cx - a_out + 0.2, cy + half, D - 0.05, H, rot)]
    # The shroud, the dark behind it (the louvre), the motor and the cross bars it hangs on.
    b += _oct_ring_z(cx, cy, a_in, a_out, 0.6, D + 0.25, "pi_steel")
    b += _oct_prism_z(cx, cy, a_in, 0.25, 0.5, "pi_pnl_black")
    b += _oct_prism_z(cx, cy, 1.7, 2.5, 4.2, "pi_cap_black")
    b += [(cx - a_in, cy - 0.25, 3.0, cx - 1.7, cy + 0.25, 3.4, "pi_steel_dark"), (cx + 1.7, cy - 0.25, 3.0, cx + a_in, cy + 0.25, 3.4, "pi_steel_dark")]
    # The wire guard over it: three rings and the spokes.
    for r in (1.9, 3.5, 5.1):
        b += _oct_ring_z(cx, cy, r, r + 0.18, D + 0.35, D + 0.55, "pi_chrome")
    b += [(cx - 5.4, cy - 0.09, D + 0.3, cx + 5.4, cy + 0.09, D + 0.6, "pi_chrome"), (cx - 0.09, cy - 5.4, D + 0.3, cx + 0.09, cy + 5.4, D + 0.6, "pi_chrome")]
    # The control station, its selector knob, lamps, the conduit up from it into the fan.
    x0, y0, x1, y1 = EF_BOX
    S = EF_STATION_FRONT
    b.append((x0, y0, 0.2, x1, y1, S, {"*": "pi_cabinet", "south": F}))
    sx, sy = EF_SEL
    b += _oct_prism_z(sx, sy, 0.62, S, S + 0.45, "pi_cap_black")
    for x, _, _, _, lens in EF_LAMPS:
        b += _sc_round(x, EF_LAMP_Y, S, S + 0.3, 0.62, "pi_chrome") + _sc_round(x, EF_LAMP_Y, S + 0.3, S + 0.5, 0.42, lens)
    b += [(9.6, y1, 1.1, 10.4, 16.6, 1.9, "pi_chrome"), (9.4, y1, 0.9, 10.6, y1 + 0.5, 2.1, "pi_steel_dark")]
    return b


CRAFTING.update({
    "h2_monitor": (["PGP", "CRC", "PPP"], {"P": "#c:plates/steel", "G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit",
                                           "R": "minecraft:redstone"}, 1),
    "exhaust_fan": (["PIP", "IRI", "PIP"], {"P": "#c:plates/steel", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, 1),
})


# The drawings defined after the table (DRAWINGS holds their keys from the start, so SHARP has them).
DRAWINGS["station_charger"] = station_charger
DRAWINGS["vital_inverter"] = vital_inverter
DRAWINGS["reg_transformer"] = reg_transformer
DRAWINGS["vital_panel"] = vital_panel
DRAWINGS["dc_ground_detector"] = ground_detector
DRAWINGS["h2_monitor"] = h2_monitor
DRAWINGS["exhaust_fan"] = exhaust_fan


# The relay room's cabinets and pieces (round 26, #10): they register themselves here.
import relay_room  # noqa: E402,F401
import hvac_room  # noqa: E402,F401  (round 26, #11: the control room HVAC equipment room's pieces)
import tsc_room  # noqa: E402,F401  (round 26, #12: the Technical Support Center's consoles, displays and boards)
