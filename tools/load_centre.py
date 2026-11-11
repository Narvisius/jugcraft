"""The 480 V load centre rebuilt to the detail standard (older rooms round 1, the switchgear room; Justin, 15 October
2026: "Real 480 V switchgear", working "Control switches", "Rack breakers out", "Overload trips", "Transformer fans").

A real plant's 480 V load centre (a unit substation's low voltage switchgear): a lineup of steel sections bolted
together on a sill, eight blocks long, two deep, as the old one (the same frame, so nuclear/Switchgear.java's
terminals and the plant's cables stay where they were). From the left as you face it:

- the dry-type station service transformer (4160 V to 480Y/277 V, 1500 kVA, fan cooled), two blocks: its bolted
  covers, the lineup's nameplate and the train's plate, the DANGER sign, the transformer's nameplate, the winding
  temperature monitor (three phases' temperatures, the FANS ON, HIGH TEMP and TRIP lamps, the fans' AUTO / MANUAL
  switch: all the game's), and low down a grille with the two cooling fans behind it (they turn while they run);
- the main breaker's section: the instrument door (ammeter and voltmeter with their selector switches, the power
  monitor's display), the main breaker's door;
- five feeder sections, each with an instrument door (the two feeders' ammeters) over two breaker doors.

Every breaker's door has its engraved nameplate (tag and load, 52-1A-03 / MCC 1A2), its pistol-grip control switch
between the green (open) and red (closed) lamps, an amber TRIP lamp, hinges and a quarter-turn latch; through the
cut-out in it, the draw-out breaker's own front: PUSH CLOSE and PUSH OPEN buttons, the contacts' OPEN / CLOSED and
the closing spring's CHARGED / DISCHARGED indicators, the trip unit (its display reading the breaker's amps, the
pickup and status lights, its settings, the TRIPPED target and RESET), the charging handle, the racking position
indicator (CONNECT / TEST / DISCONNECT) and the racking crank's port, the rating plate. The breaker's front comes out
toward you as it is racked out. Each section's arc-flash warning label is on its lower door. Along the top the
train's colour, the breaker lifting device's rails and trolley, and over each feeder section the gland plate where
its cables come down from the tray. At the back, bolted rear covers with their warnings.

Labelled by the anchor's variant (v0 LC 1A Train A orange, v1 LC 1B Train B blue, v2..v4 LC 1C..1E non-safety).
The game draws (live, client/LoadCentreParts.java): lamps, needles, the indicators (flags by state), the trip units'
and power monitor's and temperature monitor's figures (texts), the control switches' handles (by state), the
breakers' fronts (moved out by their racking position), the fans; the controls a player works are listed with
where they are on the face (controls)."""
import contextlib
import math

from PIL import ImageDraw

import control_boards as C  # (first: board_live imports it)
import board_live as L
from control_boards import sw, _r

W, D, T = 8, 2, 3
FACE = 0.12                     # the doors' backs, the sections' fronts (z; the old LC_FACE)
DZ = FACE - 0.012               # the doors' faces: what stands on a door stands out of this
BODY_Z = 0.3                    # the sections' dark insides behind the doors and the breakers' cut-outs
TOP = 2.36                      # the sections' tops, under the cap
CAP = 2.4
BF = 0.135                      # a racked-in breaker's front (behind its door's face)
RACK_DZ = [0.0, -0.035, -0.07]  # its front moves out toward you: CONNECT, TEST, DISCONNECT
XF = (0.04, 1.98)
MAIN = (2.0, 2.96)
SEC_W = (7.96 - 2.98) / 5
FEEDER_X = [(2.98 + k * SEC_W + 0.02, 2.98 + (k + 1) * SEC_W - 0.02) for k in range(5)]
UPPER, LOWER = (1.16, 1.93), (0.14, 1.13)
FEEDER_INST = (1.96, 2.33)
MAIN_INST, MAIN_DOOR = (1.5, 2.33), (0.14, 1.47)
GLAND_Z = 1.5                   # the gland plates' middle (the plant's tray drops come down to them)
GLAND_R = 0.11

TAGS = ("1A", "1B", "1C", "1D", "1E")
TRAIN = ("a", "b", "n", "n", "n")
TRAIN_RGB = {"a": (226, 116, 30), "b": (40, 92, 196), "n": (96, 98, 102)}
PAINT = (176, 180, 178)         # ANSI 61 light grey
PAINT_DARK = (120, 124, 124)
CHARCOAL = (34, 35, 38)

ATLAS = L.Atlas()


@contextlib.contextmanager
def own_atlas():
    keep = L.ATLAS
    L.ATLAS = ATLAS
    try:
        yield
    finally:
        L.ATLAS = keep


def feeders(tag):
    """The ten feeders' loads, upper then lower breaker of each section from the left (nuclear/Switchgear.java NAMES)."""
    return [f"MCC {tag}1", f"MCC {tag}2", f"MCC {tag}3", f"MCC {tag}4", f"INVERTER {tag}", f"DC MCC {tag}", f"CR HVAC {tag}",
            f"LIGHTING {tag}", "SPARE", "SPARE"]


def breaker_tag(tag, i):
    return f"52-{tag}-{i + 1:02d}"


def breaker_door(i):
    """Breaker i's door (x0, x1, y0, y1): 0 the main, 1..10 the feeders (the upper and lower of each section)."""
    if i == 0:
        return MAIN[0], MAIN[1], MAIN_DOOR[0], MAIN_DOOR[1]
    k, lower = (i - 1) // 2, (i - 1) % 2
    x0, x1 = FEEDER_X[k]
    y0, y1 = LOWER if lower else UPPER
    return x0, x1, y0, y1


# ------------------------------------------------------------------------------------------------ the drawings

def _paint(rgb, key, w=48, h=48, grain=True):
    """Paint on steel: the colour with a faint brushed grain."""
    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, img.width - 1, img.height - 1], fill=tuple(rgb) + (255,))
        if grain:
            for y in range(0, img.height, 3):
                c = tuple(max(0, min(255, v + (3 if (y // 3) % 2 else -2))) for v in rgb)
                d.line([(0, y), (img.width - 1, y)], fill=c + (255,))
    return ATLAS.add(f"lc:paint:{key}", w, h, draw)


def _text(d, xy, s, size, ink, anchor="mm", bold=True, width=None):
    f = L._font(size, bold)
    if width:
        while size > 6 and d.textlength(s, font=f) > width:
            size -= 1
            f = L._font(size, bold)
    d.text(xy, s, font=f, fill=ink, anchor=anchor)


def cs_plate_tex():
    """The control switch's escutcheon plate: TRIP to the left, CLOSE to the right, the arrows, black on brushed
    steel."""
    p = L.px(0.15)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, p - 1, p - 1], fill=(196, 198, 196, 255), outline=(120, 122, 122, 255))
        ink = (16, 16, 18, 255)
        _text(d, (p * 0.22, p * 0.16), "TRIP", 11, ink)
        _text(d, (p * 0.78, p * 0.16), "CLOSE", 11, ink)
        _text(d, (p * 0.5, p * 0.92), "NORMAL", 9, ink)
        for sx in (-1, 1):
            cx = p / 2 + sx * p * 0.3
            d.line([(p / 2 + sx * p * 0.12, p * 0.3), (cx, p * 0.36)], fill=ink, width=2)
            d.polygon([(cx, p * 0.36), (cx - sx * 5, p * 0.29), (cx - sx * 1, p * 0.42)], fill=ink)
    return ATLAS.add(f"lc:csplate:{p}", p, p, draw)


def window_tex(word, bg, ink, w, h):
    """An indicator's flag: its word on its colour."""
    pw, ph = L.px(w), L.px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=tuple(bg) + (255,))
        _text(d, (pw / 2, ph / 2), word, max(7, int(ph * 0.62)), tuple(ink) + (255,), width=pw - 4)
    return ATLAS.add(f"lc:flag:{word}:{bg}:{pw}x{ph}", pw, ph, draw)


def arc_flash_tex(tag, section, cal, boundary, ppe):
    """The arc-flash warning label (NFPA 70E / ANSI Z535): the orange WARNING header, the hazard, the incident energy
    and boundaries, the PPE category, the shock hazard and approach boundaries, the equipment and the date."""
    w, h = 0.3, 0.21
    pw, ph = L.px(w), L.px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(250, 250, 246, 255), outline=(30, 30, 30, 255))
        d.rectangle([2, 2, pw - 3, 22], fill=(244, 128, 28, 255))
        d.polygon([(10, 19), (18, 5), (26, 19)], fill=(16, 16, 16, 255))
        d.text((18, 15), "!", font=L._font(10), fill=(244, 128, 28, 255), anchor="mm")
        _text(d, (pw / 2 + 10, 12), "WARNING", 17, (16, 16, 16, 255))
        ink = (16, 16, 18, 255)
        _text(d, (pw / 2, 32), "Arc Flash and Shock Hazard", 10, ink, width=pw - 8)
        _text(d, (pw / 2, 44), "Appropriate PPE Required", 9, ink, bold=False, width=pw - 8)
        d.line([(4, 52), (pw - 5, 52)], fill=(30, 30, 30, 255), width=1)
        rows = [("Incident energy", f"{cal} cal/cm² @ 18 in"), ("Arc flash boundary", f"{boundary} in"),
                ("PPE category", f"{ppe}"), ("Shock hazard", "480 VAC"), ("Limited approach", "42 in"), ("Restricted approach", "12 in")]
        for k, (a, b) in enumerate(rows):
            y = 61 + k * 11
            _text(d, (6, y), a, 8, ink, anchor="lm", bold=False)
            _text(d, (pw - 6, y), b, 8, ink, anchor="rm")
        d.line([(4, 128), (pw - 5, 128)], fill=(30, 30, 30, 255), width=1)
        _text(d, (pw / 2, 137), f"Equipment: LC {tag} SECTION {section}", 8, ink, width=pw - 8)
        _text(d, (pw / 2, 148), "Study 2026-03  Do not remove", 7, ink, bold=False, width=pw - 8)
    return ATLAS.add(f"lc:arc:{tag}:{section}:{cal}", pw, ph, draw)


def danger_tex(lines, w, h, key):
    """An ANSI Z535 DANGER sign: the red oval DANGER header, the message in black on white."""
    pw, ph = L.px(w), L.px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(250, 250, 246, 255), outline=(20, 20, 20, 255))
        hh = int(ph * 0.34)
        d.rectangle([2, 2, pw - 3, hh], fill=(20, 20, 20, 255))
        d.rounded_rectangle([pw * 0.18, 5, pw * 0.82, hh - 3], radius=(hh - 8) // 2, fill=(206, 30, 32, 255))
        _text(d, (pw / 2, hh / 2 + 2), "DANGER", int(hh * 0.62), (250, 250, 246, 255), width=pw * 0.58)
        n = len(lines)
        for k, s in enumerate(lines):
            y = hh + (ph - hh) * (k + 0.5) / n
            _text(d, (pw / 2, y), s, int((ph - hh) / n * 0.62), (16, 16, 18, 255), width=pw - 10)
    return ATLAS.add(f"lc:danger:{key}:{pw}x{ph}", pw, ph, draw)


def rating_tex(main):
    """The breaker's rating plate."""
    w, h = (0.22, 0.06) if main else (0.18, 0.05)
    pw, ph = L.px(w), L.px(h)
    lines = (["POWER CIRCUIT BREAKER", "3200 AF  2000 AT  480 VAC", "65 kA INT  ANSI C37.13"] if main else
             ["POWER CIRCUIT BREAKER", "800 AF  225 AT  480 VAC", "65 kA INT  ANSI C37.13"])

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(186, 188, 186, 255), outline=(110, 110, 110, 255))
        for k, s in enumerate(lines):
            _text(d, (pw / 2, ph * (k + 0.5) / 3), s, 8 if k else 7, (20, 20, 22, 255), bold=k == 0, width=pw - 4)
    return ATLAS.add(f"lc:rating:{main}", pw, ph, draw)


TU_LEDS, TU_KNOBS, TU_RESET, TU_TARGET = 0.45, 0.8, (0.82, 0.6), 0.36   # the trip unit's layout (fractions down it)


def trip_unit_tex(main, w, h):
    """The trip unit's face: its maker's line, the display's place (the game writes in it), the lights' and settings'
    legends, the test port."""
    pw, ph = L.px(w), L.px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(92, 96, 100, 255), outline=(40, 42, 44, 255))
        ink = (238, 238, 232, 255)
        _text(d, (pw / 2, 7), "DIGITAL TRIP UNIT  LSI" if not main else "DIGITAL TRIP UNIT  LSIG", 8, ink, width=pw - 6)
        _text(d, (pw * 0.2, ph * (TU_LEDS - 0.07)), "STATUS", 7, ink)
        _text(d, (pw * 0.5, ph * (TU_LEDS - 0.07)), "LT PU", 7, ink)
        _text(d, (pw * 0.8, ph * (TU_LEDS - 0.07)), "TRIP", 7, ink)
        for k, s in enumerate(("LTPU", "LTD", "STPU", "INST")):
            _text(d, (pw * (0.14 + k * 0.24), ph * (TU_KNOBS + 0.115)), s, 7, ink)
        _text(d, (pw * TU_RESET[0], ph * (TU_RESET[1] + 0.085)), "RESET", 7, ink)
        _text(d, (pw * TU_TARGET, ph * (TU_RESET[1] + 0.085)), "TARGET", 7, ink)
    return ATLAS.add(f"lc:tripunit:{main}:{pw}x{ph}", pw, ph, draw)


def xf_nameplate_tex(tag):
    w, h = 0.42, 0.27
    pw, ph = L.px(w), L.px(h)
    lines = [("DRY TYPE TRANSFORMER", 11, True), (f"LC {tag} STATION SERVICE", 9, True), ("1500 / 2000 kVA  AA / FA", 9, False),
             ("3 PHASE  60 Hz", 9, False), ("HV 4160 V DELTA   LV 480Y/277 V", 9, False), ("IMPEDANCE 5.75 %", 9, False),
             ("CLASS H  150 °C RISE  BIL 60 kV", 9, False), ("TAPS 2 x 2.5 % FCAN  2 x 2.5 % FCBN", 8, False), ("SERIAL 26-04417", 8, False)]

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(206, 208, 206, 255), outline=(100, 100, 100, 255))
        for k, (s, size, bold) in enumerate(lines):
            _text(d, (pw / 2, 11 + k * (ph - 18) / (len(lines) - 1)), s, size, (18, 18, 20, 255), bold=bold, width=pw - 10)
        for x, y in ((5, 5), (pw - 6, 5), (5, ph - 6), (pw - 6, ph - 6)):
            d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(120, 120, 120, 255))
    return ATLAS.add(f"lc:xfplate:{tag}", pw, ph, draw)


def monitor_face_tex(w, h):
    """The winding temperature monitor's face: its title, the phases' rows (the figures the game's), the lamps'
    legends, the fan switch's."""
    pw, ph = L.px(w), L.px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(48, 52, 58, 255), outline=(20, 20, 22, 255))
        ink = (236, 236, 230, 255)
        _text(d, (pw / 2, 9), "WINDING TEMPERATURE °C", 11, ink, width=pw - 8)
        for k, s in enumerate(("PHASE A", "PHASE B", "PHASE C")):
            _text(d, (8, ph * (0.27 + k * 0.15)), s, 9, ink, anchor="lm")
        for k, s in enumerate(("FANS ON", "HIGH TEMP", "TRIP")):
            _text(d, (pw * (0.18 + k * 0.32), ph * 0.86), s, 8, ink)
        _text(d, (pw * 0.86, ph * 0.33), "FANS", 8, ink)
        _text(d, (pw * 0.78, ph * 0.56), "AUTO", 7, ink)
        _text(d, (pw * 0.95, ph * 0.56), "MAN", 7, ink, anchor="rm")
    return ATLAS.add(f"lc:monitor:{pw}x{ph}", pw, ph, draw)


def monitor_screen_tex(w, h):
    """The power monitor's dark glass (the game writes its lines)."""
    pw, ph = L.px(w), L.px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(10, 22, 18, 255))
    return ATLAS.add(f"lc:glass:{pw}x{ph}", pw, ph, draw)


def lcd_tex(w, h):
    pw, ph = L.px(w), L.px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(132, 156, 104, 255))
        d.rectangle([0, 0, pw - 1, ph - 1], outline=(70, 84, 54, 255))
    return ATLAS.add(f"lc:lcd:{pw}x{ph}", pw, ph, draw)


def rear_tex(tag, section):
    w, h = 0.5, 0.3
    pw, ph = L.px(w), L.px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(250, 250, 246, 255), outline=(20, 20, 20, 255))
        hh = int(ph * 0.3)
        d.rectangle([2, 2, pw - 3, hh], fill=(20, 20, 20, 255))
        d.rounded_rectangle([pw * 0.22, 5, pw * 0.78, hh - 3], radius=(hh - 8) // 2, fill=(206, 30, 32, 255))
        _text(d, (pw / 2, hh / 2 + 2), "DANGER", int(hh * 0.6), (250, 250, 246, 255))
        ink = (16, 16, 18, 255)
        for k, s in enumerate(("HAZARDOUS VOLTAGE", "ENERGIZED BUS BEHIND THIS COVER", "DE-ENERGIZE AND LOCK OUT", f"LC {tag}  SECTION {section}  REAR")):
            _text(d, (pw / 2, hh + 14 + k * (ph - hh - 20) / 3), s, 13 if k == 0 else 10, ink, bold=k in (0, 3), width=pw - 12)
    return ATLAS.add(f"lc:rear:{tag}:{section}", pw, ph, draw)


# ------------------------------------------------------------------------------------------------ the board

class LoadCentre(L.LiveBoard):
    """The lineup (face: the doors' faces)."""

    def __init__(self, seed):
        super().__init__(W, seed, "", face=DZ)
        self.live.update({"flags": [], "texts": [], "fronts": [], "fans": []})
        self.paint = _paint(PAINT, "grey")
        self.paint_dark = _paint(PAINT_DARK, "dgrey", grain=False)
        self.charcoal = _paint(CHARCOAL, "charcoal", grain=False)
        self.black = sw("black")
        self.dark = sw("dgrey")
        self.steel = "stainless"
        self.off = {k: L.solid_tex(v) for k, v in L.OFF_RGB.items()}

    # -- helpers on a plane

    def at(self, x0, y0, x1, y1, zf, d, front, sides=None, lst=None):
        """A part whose face is at zf - d, standing out of a surface at zf."""
        sides = sides if sides is not None else self.dark
        self.box(x0, y0, zf - d, x1, y1, zf, [front, None, sides, sides, sides, sides], lst)

    def bolt(self, x, y, zf, r=0.011, lst=None):
        self.cyl((x, y, zf), (x, y, zf - 0.008), r, self.dark, sw("mgrey"), 6, lst)

    def lamp_at(self, x, y, zf, colour, signal, size=0.034, lst=None):
        """An indicating lamp: its chrome ring standing out of zf, its lens (the game's) in it."""
        h = size / 2 + 0.006
        rim = "stainless"
        self.box(x - h, y - h, zf - 0.014, x + h, y + h, zf, [None, None, rim, rim, rim, rim], lst)
        z = zf - 0.014
        q = ((x - h + 0.002, y + h - 0.002, z), (x - h + 0.002, y - h + 0.002, z), (x + h - 0.002, y - h + 0.002, z), (x + h - 0.002, y + h - 0.002, z))
        # The ring's front (round the lens).
        for a, b, c, e in ((x - h, y + h - 0.002, x + h, y + h), (x - h, y - h, x + h, y - h + 0.002), (x - h, y - h + 0.002, x - h + 0.002, y + h - 0.002),
                           (x + h - 0.002, y - h + 0.002, x + h, y + h - 0.002)):
            self.front_quad(a, b, c, e, z, rim, lst)
        self.live["lamps"].append({"s": signal, "q": L._flat(*q), "on": L.LIT_LENS[colour], "off": self.off[colour]})

    def flag(self, signal, x0, y0, x1, y1, z, mats, b=None):
        e = {"s": signal, "q": L._flat((x0, y1, z), (x0, y0, z), (x1, y0, z), (x1, y1, z)), "m": mats}
        if b is not None:
            e["b"] = b
        self.live["flags"].append(e)

    def text(self, tid, x, y, z, h, colour, anchor="l", w=None, b=None):
        e = {"id": tid, "x": round(x, 4), "y": round(y, 4), "z": round(z, 4), "h": round(h, 4), "a": anchor, "c": list(colour)}
        if w:
            e["w"] = round(w, 4)
        if b is not None:
            e["b"] = b
        self.live["texts"].append(e)

    def hit(self, cid, x, y, hw, hh):
        self.live["controls"].append({"id": cid, "x0": round(x - hw, 4), "x1": round(x + hw, 4), "y0": round(y - hh, 4), "y1": round(y + hh, 4)})

    def plate_at(self, x, y, w, h, text, zf, style="white", size=None, d=0.004, lst=None):
        edge = sw("label_white") if style == "white" else sw("label")
        self.at(x - w / 2, y - h / 2, x + w / 2, y + h / 2, zf, d, L.plate_tex(text, w, h, style, size), edge, lst)

    # -- doors

    def door(self, x0, x1, y0, y1, hole=None, hinge=True, latch=True):
        """A hinged door: its panel (round a cut-out, hole: x0, y0, x1, y1), its hinges and its quarter-turn latch."""
        z0, z1 = DZ, FACE
        faces = [self.paint, None, self.paint_dark, self.paint_dark, self.paint_dark, self.paint_dark]
        if hole:
            hx0, hy0, hx1, hy1 = hole
            for a, b, c, e in ((x0, hy1, x1, y1), (x0, y0, x1, hy0), (x0, hy0, hx0, hy1), (hx1, hy0, x1, hy1)):
                self.box(a, b, z0, c, e, z1, faces)
            # The cut-out's gasket: a black rubber rim on the door's face round it.
            g = 0.012
            for a, b, c, e in ((hx0 - g, hy1, hx1 + g, hy1 + g), (hx0 - g, hy0 - g, hx1 + g, hy0), (hx0 - g, hy0, hx0, hy1), (hx1, hy0, hx1 + g, hy1)):
                self.at(a, b, c, e, DZ, 0.004, self.black, self.black)
            # The compartment's dark inside round the breaker.
            self.front_quad(hx0, hy0, hx1, hy1, BODY_Z - 0.005, self.black, None)
        else:
            self.box(x0, y0, z0, x1, y1, z1, faces)
        if hinge:
            for hy in (y0 + 0.1, y1 - 0.1):
                self.cyl((x0 + 0.012, hy - 0.045, DZ - 0.006), (x0 + 0.012, hy + 0.045, DZ - 0.006), 0.009, self.dark, self.dark, 8)
                self.at(x0 + 0.004, hy - 0.04, x0 + 0.03, hy + 0.04, DZ, 0.003, self.paint_dark, self.paint_dark)
        if latch:
            ly = (y0 + y1) / 2 if not hole else min(y1 - 0.08, max(y0 + 0.08, (hole[1] + hole[3]) / 2))
            lx = x1 - 0.045
            self.at(lx - 0.022, ly - 0.05, lx + 0.022, ly + 0.05, DZ, 0.004, sw("mgrey"), self.dark)
            self.cyl((lx, ly, DZ - 0.004), (lx, ly, DZ - 0.016), 0.014, self.dark, self.dark, 8)
            self.obox((lx, ly, DZ - 0.022), (0, 1, 0), (0, 0, -1), 0.04, 0.007, 0.006, self.black)
            # The padlock hasp under it.
            self.at(lx - 0.008, ly - 0.085, lx + 0.008, ly - 0.06, DZ, 0.012, sw("mgrey"), sw("mgrey"))

    def louvres(self, cx, y0, n, w, zf=DZ):
        for k in range(n):
            y = y0 + k * 0.04
            self.at(cx - w / 2, y, cx + w / 2, y + 0.018, zf, 0.01, self.paint_dark, self.dark)

    # -- a breaker's door and its breaker

    def control_switch(self, i, x, y):
        """The pistol-grip control switch: its escutcheon plate (TRIP / CLOSE), the boss, the grip turned to TRIP ("t"),
        NORMAL ("n") or CLOSE ("c"): the game's."""
        self.at(x - 0.075, y - 0.075, x + 0.075, y + 0.075, DZ, 0.006, cs_plate_tex(), self.dark)
        zf = DZ - 0.006
        self.cyl((x, y, zf), (x, y, zf - 0.014), 0.024, self.black, self.dark, 10)
        ops = {}
        for state, angle in (("t", -45), ("n", 0), ("c", 45)):
            lst = []
            a = math.radians(angle)
            u = (math.sin(a), math.cos(a), 0.0)
            n = (0.0, 0.0, -1.0)
            # The grip: broad and black, its point (with the white mark) up the way it is turned.
            c = (x - u[0] * 0.004, y - u[1] * 0.004, zf - 0.026)
            self.obox(c, u, n, 0.04, 0.016, 0.012, self.black, lst)
            tip = (x + u[0] * 0.03, y + u[1] * 0.03, zf - 0.0385)
            self.obox(tip, u, n, 0.016, 0.0018, 0.0006, sw("white"), lst)
            ops[state] = lst
        self.live["handles"].append({"id": f"cs{i}", "ops": ops})
        self.hit(f"cs{i}", x, y, 0.07, 0.07)

    def breaker_front(self, i, ex, ey, ew, eh, main, tag):
        """The breaker's own front in its door's cut-out (its middle ex, ey; ew x eh), in its part's operations (moved
        out by its racking position)."""
        lst = []
        z = BF
        self.box(ex - ew / 2, ey - eh / 2, z, ex + ew / 2, ey + eh / 2, BODY_Z - 0.01, [self.charcoal, None, self.dark, self.dark, self.dark, self.dark], lst)
        x0, x1, y0, y1 = ex - ew / 2, ex + ew / 2, ey - eh / 2, ey + eh / 2
        k = 1.25 if main else 1.0
        # The close and open buttons (left), their legends.
        bx = x0 + 0.06 * k
        for by, colour, word in ((y1 - 0.07 * k, "red", "PUSH|CLOSE"), (y1 - 0.17 * k, "green", "PUSH|OPEN")):
            self.cyl((bx, by, z), (bx, by, z - 0.014), 0.026 * k, self.black, sw(colour), 12, lst)
            self.at(bx + 0.035 * k, by - 0.022 * k, bx + 0.115 * k, by + 0.022 * k, z, 0.002, L.plate_tex(word, 0.08 * k, 0.044 * k, "black"), self.black, lst)
        # The indicators: the main contacts (OPEN / CLOSED) and the closing spring (DISCHARGED / CHARGED).
        fx = x0 + 0.27 * k
        fw, fh = 0.1 * k, 0.034 * k
        for fy, sig, mats, legend in ((y1 - 0.06 * k, f"b{i}.contacts", [window_tex("OPEN", (36, 146, 66), (250, 250, 246), fw, fh),
                                                                          window_tex("CLOSED", (200, 36, 30), (250, 250, 246), fw, fh)], "MAIN CONTACTS"),
                                      (y1 - 0.15 * k, f"b{i}.spring", [window_tex("DISCHARGED", (236, 236, 230), (20, 20, 22), fw, fh),
                                                                       window_tex("CHARGED", (236, 196, 36), (20, 20, 22), fw, fh)], "CLOSING SPRING")):
            self.at(fx - fw / 2 - 0.008, fy - fh / 2 - 0.008, fx + fw / 2 + 0.008, fy + fh / 2 + 0.008, z, 0.003, self.black, self.black, lst)
            self.flag(sig, fx - fw / 2, fy - fh / 2, fx + fw / 2, fy + fh / 2, z - 0.004, mats, b=i)
            self.at(fx - 0.065 * k, fy + fh / 2 + 0.009, fx + 0.065 * k, fy + fh / 2 + 0.009 + 0.024 * k, z, 0.002, L.plate_tex(legend, 0.13 * k, 0.024 * k, "black"), self.black, lst)
        # The trip unit (right): its face, its display (the game's figures), its lights, its settings, the target and
        # the reset.
        tw, th = 0.17 * k, 0.26 * k
        tx, ty = x1 - tw / 2 - 0.03, y1 - th / 2 - 0.03
        tz = z - 0.022

        def ty_(f):
            return ty + th / 2 - f * th
        self.box(tx - tw / 2, ty - th / 2, tz, tx + tw / 2, ty + th / 2, z, [trip_unit_tex(main, tw, th), None, self.dark, self.dark, self.dark, self.dark], lst)
        lw, lh = tw - 0.03, 0.045 * k
        ly = ty_(0.22)
        self.at(tx - lw / 2 - 0.006, ly - lh / 2 - 0.006, tx + lw / 2 + 0.006, ly + lh / 2 + 0.006, tz, 0.002, self.black, self.black, lst)
        self.front_quad(tx - lw / 2, ly - lh / 2, tx + lw / 2, ly + lh / 2, tz - 0.003, lcd_tex(lw, lh), lst)
        self.text(f"b{i}.lcd", tx + lw / 2 - 0.008, ly, tz - 0.0035, lh * 0.55, (24, 34, 18), "r", lw - 0.016, b=i)
        led_y = ty_(TU_LEDS)
        for fr, sig, colour in ((0.2, f"b{i}.status", "green"), (0.5, f"b{i}.pickup", "amber"), (0.8, f"b{i}.trip", "red")):
            lx = tx - tw / 2 + tw * fr
            s = 0.008 * k
            self.live["lamps"].append({"s": sig, "q": L._flat((lx - s, led_y + s, tz - 0.001), (lx - s, led_y - s, tz - 0.001), (lx + s, led_y - s, tz - 0.001),
                                                               (lx + s, led_y + s, tz - 0.001)), "on": L.LIT_LENS[colour], "off": self.off[colour], "b": i})
        for kk in range(4):
            kx = tx - tw / 2 + tw * (0.14 + kk * 0.24)
            ky = ty_(TU_KNOBS)
            self.cyl((kx, ky, tz), (kx, ky, tz - 0.008), 0.013 * k, sw("black"), sw("dgrey"), 8, lst)
            a = math.radians(-60 + kk * 37)
            self.obox((kx + math.sin(a) * 0.006 * k, ky + math.cos(a) * 0.006 * k, tz - 0.0085), (math.sin(a), math.cos(a), 0), (0, 0, -1),
                      0.006 * k, 0.0012, 0.0005, sw("white"), lst)
        rx, ry = tx - tw / 2 + tw * TU_RESET[0], ty_(TU_RESET[1])
        self.cyl((rx, ry, tz), (rx, ry, tz - 0.01), 0.012 * k, self.black, sw("black"), 8, lst)
        self.hit(f"reset{i}", rx, ry, 0.025 * k, 0.025 * k)
        # The TRIPPED target: a window beside the reset showing the amber flag when the unit has tripped the breaker.
        gx, gy, gw, gh = tx - tw / 2 + tw * TU_TARGET, ty_(TU_RESET[1]), 0.07 * k, 0.026 * k
        self.at(gx - gw / 2 - 0.004, gy - gh / 2 - 0.004, gx + gw / 2 + 0.004, gy + gh / 2 + 0.004, tz, 0.0015, self.black, self.black, lst)
        self.flag(f"b{i}.target", gx - gw / 2, gy - gh / 2, gx + gw / 2, gy + gh / 2, tz - 0.0025,
                  [L.solid_tex((24, 24, 26)), window_tex("TRIPPED", (244, 150, 20), (16, 16, 18), gw, gh)], b=i)
        # The operations counter (its figures the game's: how many times the breaker has closed).
        cx_, cy_ = fx, y1 - 0.225 * k
        cw_, ch_ = 0.09 * k, 0.028 * k
        self.at(cx_ - cw_ / 2 - 0.006, cy_ - ch_ / 2 - 0.006, cx_ + cw_ / 2 + 0.006, cy_ + ch_ / 2 + 0.006, z, 0.003, self.black, self.black, lst)
        self.front_quad(cx_ - cw_ / 2, cy_ - ch_ / 2, cx_ + cw_ / 2, cy_ + ch_ / 2, z - 0.005, L.solid_tex((14, 14, 16)), lst)
        self.text(f"b{i}.ops", cx_ + cw_ / 2 - 0.006, cy_, z - 0.0055, ch_ * 0.6, (236, 236, 230), "r", cw_ - 0.01, b=i)
        self.at(cx_ - 0.06 * k, cy_ + ch_ / 2 + 0.008, cx_ + 0.06 * k, cy_ + ch_ / 2 + 0.028 * k, z, 0.002, L.plate_tex("OPERATIONS", 0.12 * k, 0.02 * k, "black"), self.black, lst)
        # The charging handle (bottom left), lying in its recess.
        hx, hy = x0 + 0.1 * k, y0 + 0.08 * k
        self.at(hx - 0.07 * k, hy - 0.025 * k, hx + 0.07 * k, hy + 0.025 * k, z, 0.002, self.black, self.black, lst)
        self.obox((hx, hy, z - 0.011), (1, 0, 0), (0, 0, -1), 0.06 * k, 0.012 * k, 0.009, sw("dgrey"), lst)
        self.at(hx - 0.07 * k, hy + 0.03 * k, hx + 0.07 * k, hy + 0.05 * k, z, 0.002, L.plate_tex("CHARGE", 0.14 * k, 0.02 * k, "black"), self.black, lst)
        # The racking position indicator and the crank's port (bottom middle).
        rx, ry = ex - 0.04 * k, y0 + 0.075 * k
        rw, rh = 0.12 * k, 0.03 * k
        self.at(rx - rw / 2 - 0.008, ry + 0.03 * k - rh / 2 - 0.008, rx + rw / 2 + 0.008, ry + 0.03 * k + rh / 2 + 0.008, z, 0.003, self.black, self.black, lst)
        self.flag(f"b{i}.rack", rx - rw / 2, ry + 0.03 * k - rh / 2, rx + rw / 2, ry + 0.03 * k + rh / 2, z - 0.004,
                  [window_tex("CONNECT", (36, 146, 66), (250, 250, 246), rw, rh), window_tex("TEST", (236, 196, 36), (20, 20, 22), rw, rh),
                   window_tex("DISCONNECT", (200, 36, 30), (250, 250, 246), rw, rh)], b=i)
        py = ry - 0.025 * k
        self.cyl((rx, py, z), (rx, py, z - 0.006), 0.026 * k, sw("mgrey"), sw("black"), 12, lst)
        self.cyl((rx, py, z - 0.006), (rx, py, z - 0.0075), 0.012 * k, sw("black"), sw("black"), 6, lst)
        self.at(rx + 0.035 * k, py - 0.014 * k, rx + 0.115 * k, py + 0.014 * k, z, 0.002, L.plate_tex("RACKING", 0.08 * k, 0.028 * k, "black"), self.black, lst)
        self.hit(f"rack{i}", rx, py, 0.04 * k, 0.04 * k)
        # The rating plate and the padlock tab (bottom right).
        rw2, rh2 = (0.22, 0.06) if main else (0.18, 0.05)
        self.at(x1 - 0.03 - rw2, y0 + 0.02, x1 - 0.03, y0 + 0.02 + rh2, z, 0.002, rating_tex(main), self.black, lst)
        self.live["fronts"].append({"b": i, "ops": lst, "dz": RACK_DZ})

    def breaker(self, i, tag, name, section):
        x0, x1, y0, y1 = breaker_door(i)
        main = i == 0
        cx = (x0 + x1) / 2
        ew, eh = (0.76, 0.62) if main else (0.64, 0.42)
        top = y1 - 0.06
        # The nameplate, the lamps and the control switch over the cut-out.
        ny = y1 - 0.055          # (the nameplate: by variant, named())
        sy = ny - 0.135
        self.control_switch(i, cx, sy)
        for lx, colour, signal, word in ((cx - 0.15, "green", f"b{i}.open", "OPEN"), (cx + 0.15, "red", f"b{i}.closed", "CLOSED"),
                                         (cx + 0.27, "amber", f"b{i}.trip", "TRIP")):
            self.lamp_at(lx, sy + 0.02, DZ, colour, signal)
            self.plate_at(lx, sy - 0.035, 0.085, 0.026, word, DZ, "white")
        ey = sy - 0.075 - 0.025 - eh / 2
        ex = cx - 0.02
        hole = (ex - ew / 2 - 0.012, ey - eh / 2 - 0.012, ex + ew / 2 + 0.012, ey + eh / 2 + 0.012)
        self.door(x0, x1, y0, y1, hole)
        self.breaker_front(i, ex, ey, ew, eh, main, tag)
        return ey - eh / 2 - 0.012

    # -- the sections

    def feeder_section(self, k, tag, names):
        x0, x1 = FEEDER_X[k]
        cx = (x0 + x1) / 2
        # The instrument door: the two feeders' ammeters, their names.
        self.door(x0, x1, FEEDER_INST[0], FEEDER_INST[1], latch=True)
        for j, dx in enumerate((-0.2, 0.2)):
            i = 1 + 2 * k + j
            self.dial_meter(cx + dx - 0.02, 2.2, 0.17, f"b{i}.amps", 0, 300, "AMPS", majors=4)
        for j in range(2):
            i = 1 + 2 * k + j
            bottom = self.breaker(i, tag, names[2 * k + j], k + 3)
            if j == 1:
                # The lower door: the section's arc-flash label and the vent louvres under the breaker.
                self.louvres(cx + 0.18, LOWER[0] + 0.04, 4, 0.36)

    def main_section(self, tag):
        x0, x1 = MAIN
        cx = (x0 + x1) / 2
        y0, y1 = MAIN_INST
        self.door(x0, x1, y0, y1)
        self.dial_meter(cx - 0.22, 2.13, 0.24, "main.amps", 0, 2500, "AC AMPS", majors=6)
        self.dial_meter(cx + 0.2, 2.13, 0.24, "main.volts", 0, 600, "AC VOLTS", majors=7)
        self.plate_at(cx - 0.22, 1.975, 0.24, 0.04, "AMMETER", DZ)
        self.plate_at(cx + 0.2, 1.975, 0.24, 0.04, "VOLTMETER", DZ)
        # The meters' selector switches (the ammeter's phase, the voltmeter's phase pair).
        for sx, words in ((cx - 0.22, "AM SW|1  2  3  OFF"), (cx + 0.2, "VM SW|1-2  2-3  3-1  OFF")):
            self.at(sx - 0.05, 1.85, sx + 0.05, 1.93, DZ, 0.004, sw("mgrey"), self.dark)
            self.cyl((sx, 1.89, DZ - 0.004), (sx, 1.89, DZ - 0.02), 0.022, self.black, self.dark, 10)
            self.obox((sx + 0.012, 1.89, DZ - 0.026), (1, 0, 0), (0, 0, -1), 0.02, 0.007, 0.006, self.black)
            self.plate_at(sx + 0.13, 1.89, 0.15, 0.05, words, DZ, "white")
        # The power monitor: its case, the dark glass (the game writes its four lines).
        mw, mh = 0.56, 0.18
        my = 1.635
        self.at(cx - mw / 2 - 0.02, my - mh / 2 - 0.02, cx + mw / 2 + 0.02, my + mh / 2 + 0.02, DZ, 0.03, self.black, self.dark)
        zf = DZ - 0.03
        self.front_quad(cx - mw / 2, my - mh / 2, cx + mw / 2, my + mh / 2, zf - 0.001, monitor_screen_tex(mw, mh), self.lit)
        for k in range(4):
            self.text(f"pm.{k}", cx - mw / 2 + 0.02, my + mh / 2 - 0.028 - k * 0.04, zf - 0.0015, 0.022, (120, 230, 150), "l", mw - 0.04)
        self.plate_at(cx, my + mh / 2 + 0.045, 0.3, 0.032, "POWER MONITOR", DZ, "black")
        bottom = self.breaker(0, tag, "MAIN BREAKER", 2)
        self.louvres(cx + 0.18, MAIN_DOOR[0] + 0.04, 4, 0.36)

    def transformer_section(self, tag):
        x0, x1 = XF
        cx = (x0 + x1) / 2
        # The upper cover: bolted, the lineup's nameplate, the DANGER sign, the transformer's nameplate, the winding
        # temperature monitor.
        uy0, uy1 = 1.24, 2.33
        self.box(x0, uy0, DZ, x1, uy1, FACE, [self.paint, None, self.paint_dark, self.paint_dark, self.paint_dark, self.paint_dark])
        for bx in (x0 + 0.04, x1 - 0.04):
            for by in (uy0 + 0.04, (uy0 + uy1) / 2, uy1 - 0.04):
                self.bolt(bx, by, DZ)
        for bx in (x0 + 0.5, cx, x1 - 0.5):
            for by in (uy0 + 0.04, uy1 - 0.04):
                self.bolt(bx, by, DZ)
        self.danger = danger_tex(["4160 VOLTS", "KEEP OUT"], 0.36, 0.24, "4160")
        self.at(x0 + 0.08, 1.86, x0 + 0.44, 2.1, DZ, 0.002, self.danger, self.black)
        # The temperature monitor: its case, its face, its three figures (the game's), the lamps, the fans' switch.
        mw, mh = 0.62, 0.4
        mx, my = cx, 1.56
        self.at(mx - mw / 2, my - mh / 2, mx + mw / 2, my + mh / 2, DZ, 0.04, monitor_face_tex(mw, mh), self.black)
        zf = DZ - 0.04
        for k in range(3):
            ry = my + mh / 2 - mh * (0.27 + k * 0.15)
            gx0, gx1 = mx - mw / 2 + 0.2, mx + mw / 2 - 0.22
            self.at(gx0 - 0.006, ry - 0.026, gx1 + 0.006, ry + 0.026, zf, 0.002, self.black, self.black)
            self.front_quad(gx0, ry - 0.02, gx1, ry + 0.02, zf - 0.003, lcd_tex(gx1 - gx0, 0.04), None)
            self.text(f"xf.t{k}", gx1 - 0.01, ry, zf - 0.0035, 0.022, (24, 34, 18), "r", gx1 - gx0 - 0.02)
        for k, (colour, sig) in enumerate((("green", "xf.fans"), ("amber", "xf.alarm"), ("red", "xf.trip"))):
            self.lamp_at(mx - mw / 2 + mw * (0.18 + k * 0.32), my - mh / 2 + mh * 0.27, zf, colour, sig, 0.03)
        fx, fy = mx + mw / 2 - mw * 0.14, my + mh / 2 - mh * 0.5
        self.cyl((fx, fy, zf), (fx, fy, zf - 0.012), 0.02, self.black, self.dark, 10)
        ops = {}
        for state, angle in (("0", -40), ("1", 40)):
            lst = []
            a = math.radians(angle)
            u = (math.sin(a), math.cos(a), 0.0)
            self.obox((fx, fy, zf - 0.02), u, (0, 0, -1), 0.026, 0.008, 0.008, self.black, lst)
            self.obox((fx + u[0] * 0.012, fy + u[1] * 0.012, zf - 0.0285), u, (0, 0, -1), 0.012, 0.0015, 0.0005, sw("white"), lst)
            ops[state] = lst
        self.live["handles"].append({"id": "fans", "ops": ops})
        self.hit("fans", fx, fy, 0.04, 0.04)
        self.louvres(cx - 0.6, 1.3, 2, 0.5)
        self.louvres(cx + 0.6, 1.3, 2, 0.5)
        # The lower cover: the fans' grille (the fans behind it, the game's), louvres, the caution label.
        ly0, ly1 = 0.14, 1.2
        gx0, gy0, gx1, gy1 = x0 + 0.18, 0.36, x1 - 0.18, 0.92
        faces = [self.paint, None, self.paint_dark, self.paint_dark, self.paint_dark, self.paint_dark]
        for a, b, c, e in ((x0, gy1, x1, ly1), (x0, ly0, x1, gy0), (x0, gy0, gx0, gy1), (gx1, gy0, x1, gy1)):
            self.box(a, b, DZ, c, e, FACE, faces)
        for bx in (x0 + 0.04, x1 - 0.04):
            for by in (ly0 + 0.04, (ly0 + ly1) / 2, ly1 - 0.04):
                self.bolt(bx, by, DZ)
        # The grille: a frame, expanded metal's strands (verticals in front of the horizontals).
        for a, b, c, e in ((gx0 - 0.02, gy1, gx1 + 0.02, gy1 + 0.02), (gx0 - 0.02, gy0 - 0.02, gx1 + 0.02, gy0), (gx0 - 0.02, gy0, gx0, gy1), (gx1, gy0, gx1 + 0.02, gy1)):
            self.at(a, b, c, e, DZ, 0.006, self.paint_dark, self.dark)
        n = int((gx1 - gx0) / 0.04)
        for k in range(1, n):
            x = gx0 + k * (gx1 - gx0) / n
            self.box(x - 0.003, gy0, DZ + 0.004, x + 0.003, gy1, DZ + 0.008, [self.dark] * 6)
        m = int((gy1 - gy0) / 0.04)
        for k in range(1, m):
            y = gy0 + k * (gy1 - gy0) / m
            self.box(gx0, y - 0.003, DZ + 0.009, gx1, y + 0.003, DZ + 0.012, [self.dark] * 6)
        # Behind it: the fans' housing (dark), each fan's motor and guard ring (the blades the game's).
        self.front_quad(gx0, gy0, gx1, gy1, BODY_Z - 0.005, self.black, None)
        for fxc in (gx0 + (gx1 - gx0) * 0.27, gx0 + (gx1 - gx0) * 0.73):
            fyc = (gy0 + gy1) / 2
            self.cyl((fxc, fyc, BODY_Z - 0.006), (fxc, fyc, 0.25), 0.06, self.dark, sw("mgrey"), 12)
            self.live["fans"].append({"c": [round(fxc, 4), round(fyc, 4), 0.235], "r": 0.22, "n": 5})
        self.at(cx - 0.32, 1.02, cx + 0.32, 1.1, DZ, 0.002, L.plate_tex("CAUTION  FANS START AUTOMATICALLY", 0.64, 0.08, "yellow"), self.black)
        self.louvres(cx, 0.18, 4, 1.4)
        # The lineup's nameplate and the train's plate (by variant: drawn in labels()).

    # -- the body, the top, the back, the ends

    def body(self):
        dark = self.black
        self.box(0.04, 0.1, BODY_Z, 7.96, TOP, 1.96, [dark, self.paint, self.paint, self.paint, None, None])
        # The sill, the cap.
        self.box(0.02, 0.0, 0.08, 7.98, 0.1, 1.98, [sw("black"), None, sw("black"), sw("black"), None, None])
        self.box(0.02, TOP, 0.08, 7.98, CAP, 1.98, [None, "gunmetal", "gunmetal", "gunmetal", "gunmetal", "gunmetal"])
        # The posts between the sections and the rails between their doors (seen in the doors' gaps).
        for x in [XF[1] + 0.01, MAIN[1] + 0.01] + [FEEDER_X[k][1] + 0.02 for k in range(4)]:
            self.box(x - 0.012, 0.1, FACE, x + 0.012, TOP, BODY_Z, [self.paint_dark, None] + [self.paint_dark] * 4)
        for x0, x1 in [MAIN] + FEEDER_X:
            for y in ((MAIN_INST[0] + MAIN_DOOR[1]) / 2,) if (x0, x1) == MAIN else ((FEEDER_INST[0] + UPPER[1]) / 2, (UPPER[0] + LOWER[1]) / 2):
                self.box(x0, y - 0.012, FACE + 0.004, x1, y + 0.012, BODY_Z, [self.paint_dark, None] + [self.paint_dark] * 4)
        self.box(XF[0], 1.208, FACE + 0.004, XF[1], 1.232, BODY_Z, [self.paint_dark, None] + [self.paint_dark] * 4)
        # The kick plate under the doors.
        self.box(0.04, 0.1, FACE + 0.008, 7.96, 0.14, BODY_Z, [self.paint_dark, None, None, None, None, None])
        self.box(0.04, 2.33, FACE + 0.008, 7.96, TOP, BODY_Z, [self.paint_dark, None, None, None, None, None])

    def top(self):
        dark, grey = self.dark, sw("mgrey")
        # The transformer's ventilation screen.
        self.box(0.22, CAP, 0.4, 1.78, CAP + 0.04, 1.6, ["gunmetal"] * 4 + [sw("black"), None])
        for k in range(12):
            bx = 0.31 + k * 0.12
            self.box(bx, CAP + 0.04, 0.46, bx + 0.06, CAP + 0.08, 1.54, [grey] * 5 + [None])
        # The gland plates over the feeder sections, where their cables come down from the tray, and over the main.
        for k in range(5):
            gx = 3.5 + k
            self.box(gx - 0.2, CAP, GLAND_Z - 0.2, gx + 0.2, CAP + 0.012, GLAND_Z + 0.2, ["gunmetal"] * 5 + [None])
            for bx in (gx - 0.16, gx + 0.16):
                for bz in (GLAND_Z - 0.16, GLAND_Z + 0.16):
                    self.box(bx - 0.012, CAP + 0.012, bz - 0.012, bx + 0.012, CAP + 0.02, bz + 0.012, [dark] * 5 + [None])
            self.cyl((gx, CAP + 0.012, GLAND_Z), (gx, CAP + 0.05, GLAND_Z), GLAND_R, dark, sw("black"), 12)
        # The breaker lifting device: rails along the top's front, the trolley parked at the right, its hoist, chain and
        # hook out over the front.
        for z0 in (0.22, 0.72):
            self.box(0.12, CAP, z0, 7.88, CAP + 0.05, z0 + 0.05, [dark] * 5 + [None])
        for x in (0.3, 2.5, 4.5, 6.5, 7.7):
            self.box(x - 0.03, CAP, 0.2, x + 0.03, CAP + 0.06, 0.79, [dark] * 5 + [None])
        yellow = "paint_yellow"
        self.box(6.55, CAP + 0.07, 0.16, 7.3, CAP + 0.2, 0.84, [yellow] * 6)
        for wx in (6.62, 7.18):
            for wz in (0.245, 0.745):
                self.box(wx - 0.035, CAP + 0.05, wz - 0.04, wx + 0.035, CAP + 0.07, wz + 0.04, [sw("black")] * 5 + [None])
        self.box(6.85, CAP + 0.1, -0.3, 7.0, CAP + 0.18, 0.16, [yellow, None, yellow, yellow, yellow, yellow])
        self.box(6.9, CAP - 0.02, -0.27, 6.95, CAP + 0.1, -0.22, [sw("black")] * 6)
        self.box(6.905, CAP - 0.32, -0.255, 6.945, CAP - 0.02, -0.235, [grey] * 6)
        self.box(6.89, CAP - 0.38, -0.26, 6.96, CAP - 0.32, -0.23, [dark] * 6)
        self.plate_at(7.16, CAP + 0.135, 0.24, 0.05, "SWL 1500 LB", 0.16, "black", d=0.002)

    def back(self, tag):
        """The rear covers (bolted, one a section) and their DANGER labels (the back faces +z)."""
        zb = 1.96
        sections = [(XF[0], XF[1], 1), (MAIN[0], MAIN[1], 2)] + [(x0, x1, k + 3) for k, (x0, x1) in enumerate(FEEDER_X)]
        for x0, x1, n in sections:
            self.box(x0 + 0.01, 0.16, zb, x1 - 0.01, 2.3, zb + 0.008, [None, self.paint, self.paint_dark, self.paint_dark, self.paint_dark, self.paint_dark])
            for bx in (x0 + 0.05, x1 - 0.05):
                for by in (0.22, 0.85, 1.5, 2.24):
                    self.cyl((bx, by, zb + 0.008), (bx, by, zb + 0.016), 0.011, self.dark, sw("mgrey"), 6)

    def ends(self):
        """The end covers: bolted, a lifting lug at each top corner."""
        for x, side in ((0.04, -1), (7.96, 1)):
            xa, xb = (x - 0.008, x) if side < 0 else (x, x + 0.008)
            faces = [self.paint_dark, self.paint_dark, self.paint if side < 0 else None, self.paint if side > 0 else None, self.paint_dark, self.paint_dark]
            self.box(xa, 0.12, 0.12, xb, 2.34, 1.94, faces)
            xo = xa if side < 0 else xb
            for by in (0.3, 1.2, 2.1):
                for bz in (0.3, 1.0, 1.7):
                    self.cyl((xo, by, bz), (xo + side * 0.008, by, bz), 0.011, self.dark, sw("mgrey"), 6)
            for bz in (0.2, 1.8):
                self.box(xo + (side - 1) * 0.02 if side < 0 else xo, 2.2, bz - 0.04, xo + (0 if side < 0 else 0.04), 2.32, bz + 0.04, [self.dark] * 6)

    # -- what differs by its label

    def named(self, v):
        """Variant v's names (its state "v<v>"): the stripe of its train's colour along the cap's front, the lineup's
        nameplate and train plate, the breakers' nameplates, the feeders' meters' plates, the arc-flash labels, the
        transformer's nameplate, the rear covers' labels."""
        tag, t = TAGS[v], TRAIN[v]
        names = feeders(tag)
        stripe = L.solid_tex(TRAIN_RGB[t])
        self.box(0.02, TOP + 0.008, 0.074, 7.98, CAP - 0.008, 0.08, [stripe, None, None, None, None, None])
        x0, x1 = XF
        cx = (x0 + x1) / 2
        self.plate_at(cx, 2.235, 0.92, 0.1, f"480 V LOAD CENTER  LC {tag}", DZ, "black", size=26)
        cls = {"a": "CLASS 1E   TRAIN A", "b": "CLASS 1E   TRAIN B", "n": "NON-CLASS 1E"}[t]
        self.plate_at(cx, 2.145, 0.5, 0.05, cls, DZ, {"a": "orange", "b": "blue", "n": "white"}[t], size=18)
        self.at(x1 - 0.5, 1.83, x1 - 0.08, 2.1, DZ, 0.002, xf_nameplate_tex(tag), sw("mgrey"))
        for i in range(11):
            bx0, bx1, by0, by1 = breaker_door(i)
            self.plate_at((bx0 + bx1) / 2, by1 - 0.055, 0.42, 0.075, f"{breaker_tag(tag, i)}|{'MAIN BREAKER' if i == 0 else names[i - 1]}", DZ, "white")
        for k in range(5):
            fx0, fx1 = FEEDER_X[k]
            fcx = (fx0 + fx1) / 2
            for j, dx in enumerate((-0.2, 0.2)):
                self.plate_at(fcx + dx - 0.02, 2.06, 0.25, 0.04, names[2 * k + j], DZ, "white")
            self.at(fx0 + 0.04, LOWER[0] + 0.03, fx0 + 0.34, LOWER[0] + 0.24, DZ, 0.002, arc_flash_tex(tag, k + 3, 8.4, 52, 2), self.black)
        self.at(MAIN[0] + 0.04, MAIN_DOOR[0] + 0.03, MAIN[0] + 0.34, MAIN_DOOR[0] + 0.24, DZ, 0.002, arc_flash_tex(tag, 2, 11.6, 64, 3), self.black)
        zb = 1.96
        sections = [(XF[0], XF[1], 1), (MAIN[0], MAIN[1], 2)] + [(a, b, k + 3) for k, (a, b) in enumerate(FEEDER_X)]
        for a, b, n in sections:
            mid = (a + b) / 2
            w = min(0.5, b - a - 0.14)
            self.box(mid - w / 2, 1.5, zb + 0.008, mid + w / 2, 1.5 + w * 0.6, zb + 0.01, [None, rear_tex(tag, n), self.black, self.black, self.black, self.black])


def load_centre(seed=97):
    with own_atlas():
        return _load_centre(seed)


def _load_centre(seed=97):
    b = LoadCentre(seed)
    b.body()
    tag = "1A"
    names = feeders(tag)
    b.transformer_section(tag)
    b.main_section(tag)
    for k in range(5):
        b.feeder_section(k, tag, names)
    b.top()
    b.back(tag)
    b.ends()
    b.core = len(b.still)        # (drawn whole at any distance: a far lineup keeps its doors)
    for v in range(len(TAGS)):
        st = b.state(f"v{v}")
        keep = b.still
        b.still = st["still"]
        try:
            b.named(v)
        finally:
            b.still = keep
    return b


def legend_image():
    load_centre()
    return ATLAS.pack()
