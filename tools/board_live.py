"""The control room's boards made live (round 27; Justin, 14 October 2026: "Control room boards live", the reactor
coolant, turbine and electrical boards first, "Real alarms with horn", "Key controls work", values "worked out from the
plant"). Each of these boards is laid out here again to the detail standard (claude/jugcraft-detail-standard.md):
every annunciator window has its real legend and lights from the plant's state, every meter, dial, recorder,
controller, digital indicator, lamp and screen reads the plant, every part has its engraved nameplate, and the key
controls work (the coolant pumps, the turbine trip and latch, the generator breaker, the diesel generators).

The legends, scales and nameplates are drawn into their own texture, textures/entity/control_board_legends.png, at
512 px a block (LEGEND_PX), packed by the Atlas here; a board's operations use them as the material ["@", key], swapped
for ["@", u0, v0, u1, v1] when the boards are written. The parts the game draws by state are listed in the board's
"live" section (client/ControlBoards.java, client/NuclearRenderer.java draw them; nuclear/ControlBoard.java computes
the alarms and works the controls):

    windows   annunciator windows: id, quad (x0, y0, x1, y1 at z), legend uv, k (0 an alarm, 1 a status light)
    lamps     lamp lenses: signal, quad corners, lit and unlit materials
    needles   edgewise ("edge") and round ("dial") meters' pointers: value, scale lo..hi, where
    bars      controllers' bar graphs: value, lo..hi, the bar's rectangle
    digits    digital indicators: value, digits, decimals, the window's rectangle (seven-segment figures)
    pens      strip chart recorders: their chart's rectangle and pens (value, lo..hi, colour)
    screens   screens the game writes on: id, rectangle
    handles   control switch handles drawn by state: id, operations by state
    controls  where each working control is (desk: x0..x1, s0..s1)
"""
import math

from PIL import Image, ImageDraw, ImageFont

import control_boards as C

LEGEND_PX = 512                 # texels a block
ATLAS_W = 2048
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed.ttf"

# Annunciator windows (blocks): five across and three down in a box, six boxes across a board.
WIN_W, WIN_H = 0.18, 0.105
WIN_FONT = 12
# A window's legend is drawn once, black on white; the renderer colours it (client/NuclearRenderer.java WINDOW_TINTS):
# lit, the lamp's colour behind the frosted glass; unlit, the glass frosted white in the room's light.
TINTS = {"white": (1.0, 0.98, 0.93), "amber": (1.0, 0.74, 0.24), "red": (0.98, 0.28, 0.2)}
UNLIT_TINT = (0.80, 0.80, 0.78)


# ---------------------------------------------------------------- the legend texture

class Atlas:
    """The legend texture: drawings requested by key (pixel size and a function drawing into an image of that size),
    packed in rows when the boards are written. Identical keys are drawn once."""

    def __init__(self):
        self.items = {}
        self.uv = None
        self.image = None

    def add(self, key, w, h, draw):
        if key not in self.items:
            self.items[key] = (int(w), int(h), draw)
        return ["@", key]

    def pack(self):
        order = sorted(self.items, key=lambda k: (-self.items[k][1], k))
        x = y = row = 0
        place = {}
        for k in order:
            w, h, _ = self.items[k]
            if x + w + 2 > ATLAS_W:
                x, y, row = 0, y + row + 2, 0
            place[k] = (x + 1, y + 1)
            x += w + 2
            row = max(row, h)
        height = 1
        while height < y + row + 2:
            height *= 2
        img = Image.new("RGBA", (ATLAS_W, height), (0, 0, 0, 0))
        self.uv = {}
        for k, (px, py) in place.items():
            w, h, draw = self.items[k]
            tile = Image.new("RGBA", (w, h), (0, 0, 0, 255))
            draw(tile)
            img.paste(tile, (px, py))
            # Half a texel in from the edges, so neighbours never bleed in.
            self.uv[k] = [(px + 0.5) / ATLAS_W, (py + 0.5) / height, (px + w - 0.5) / ATLAS_W, (py + h - 0.5) / height]
        self.image = img
        return img

    def resolve(self, obj):
        """Swaps every ["@", key] in a board's operations for ["@", u0, v0, u1, v1]; ["@", key, f0, g0, f1, g1] takes
        part of the drawing (fractions of it)."""
        if isinstance(obj, list):
            if len(obj) >= 2 and obj[0] == "@" and isinstance(obj[1], str):
                u0, v0, u1, v1 = self.uv[obj[1]]
                if len(obj) == 6:
                    f0, g0, f1, g1 = obj[2:]
                    return ["@", round(u0 + (u1 - u0) * f0, 6), round(v0 + (v1 - v0) * g0, 6), round(u0 + (u1 - u0) * f1, 6),
                            round(v0 + (v1 - v0) * g1, 6)]
                return ["@", round(u0, 6), round(v0, 6), round(u1, 6), round(v1, 6)]
            return [self.resolve(o) for o in obj]
        if isinstance(obj, dict):
            return {k: self.resolve(v) for k, v in obj.items()}
        return obj


ATLAS = Atlas()


def px(blocks, density=LEGEND_PX):
    return max(4, int(round(blocks * density)))


def _font(size, bold=True):
    return ImageFont.truetype(FONT if bold else FONT_REG, size)


def _fit(d, lines, w, h, size, bold=True, gap=1):
    """The largest font up to size in which every line fits w and all of them h."""
    while size > 6:
        f = _font(size, bold)
        if all(d.textlength(line, font=f) <= w for line in lines) and (size + gap) * len(lines) <= h + gap:
            return f, size
        size -= 1
    return _font(6, bold), 6


def _lines(d, lines, x0, y0, w, h, ink, size, bold=True, gap=1):
    f, s = _fit(d, lines, w, h, size, bold, gap)
    lh = s + gap
    top = y0 + h / 2 - lh * len(lines) / 2 + lh / 2
    for i, line in enumerate(lines):
        d.text((x0 + w / 2, top + i * lh), line, font=f, fill=ink, anchor="mm")


def window_tex(legend, w=WIN_W - 0.012, h=WIN_H - 0.012, size=WIN_FONT):
    """An annunciator window's engraved legend, black on white (coloured when drawn)."""
    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, img.width - 1, img.height - 1], fill=(255, 255, 255, 255))
        _lines(d, legend.split("|"), 3, 2, img.width - 6, img.height - 4, (14, 14, 14, 255), size, gap=2)
    return ATLAS.add(f"win:{px(w)}x{px(h)}:{size}:{legend}", px(w), px(h), draw)


PLATE_STYLES = {"white": ((238, 238, 232), (18, 18, 20)), "black": ((24, 24, 26), (236, 236, 230)),
                "red": ((188, 32, 28), (250, 244, 236)), "yellow": ((236, 196, 36), (20, 20, 22)),
                "orange": ((226, 116, 30), (20, 20, 22)), "green": ((40, 120, 64), (244, 244, 240)),
                "blue": ((34, 80, 170), (244, 244, 240))}


def plate_tex(text, w, h, style="white", size=None, density=LEGEND_PX):
    """An engraved nameplate w x h blocks: its legend (lines split by |) in bold letters, as large as fits."""
    bg, ink = PLATE_STYLES[style]
    lines = text.split("|")
    pw, ph = px(w, density), px(h, density)
    size = size or max(7, min(40, int(ph / len(lines)) - 2))

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, img.width - 1, img.height - 1], fill=bg + (255,))
        edge = tuple(max(0, c - 40) for c in bg)
        d.rectangle([0, 0, img.width - 1, img.height - 1], outline=edge + (255,))
        _lines(d, lines, 2, 1, img.width - 4, img.height - 2, ink + (255,), size)
    return ATLAS.add(f"plate:{style}:{pw}x{ph}:{size}:{text}", pw, ph, draw)


def _nice(v):
    return f"{v:g}" if abs(v) < 1e5 else f"{v:.0e}"


EDGE_TOP, EDGE_BOTTOM = 19, 6     # an edgewise scale's margins (texels): the unit above it


def _decade(d, x, y, k, size, ink):
    """A decade's figure right-aligned at (x, y): 1, 10, or 10 with its power raised (10², 10⁻¹)."""
    if k == 0 or k == 1:
        d.text((x, y), "1" if k == 0 else "10", font=_font(size), fill=ink, anchor="rm")
        return
    sup = _font(max(6, size - 2))
    power = str(k).replace("-", "−") if k < 0 else str(k)
    pw = d.textlength(power, font=sup)
    d.text((x, y - size * 0.35), power, font=sup, fill=ink, anchor="rm")
    d.text((x - pw, y), "10", font=_font(size), fill=ink, anchor="rm")


def edge_face_tex(unit, lo, hi, majors, w, h, band=None, log=False):
    """An edgewise meter's scale (vertical): white, the graduations on the right, figures on the left, the unit at the
    top. band: (from, to, colour) a coloured zone (the normal range green, a limit red). log: a logarithmic scale of
    decades lo..hi (the pointer reads the value's log), each decade's figure a power of ten and its 2 to 9 marks."""
    pw, ph = px(w), px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(240, 240, 232, 255))
        top, bottom = EDGE_TOP, ph - EDGE_BOTTOM
        f = _font(9)
        ink = (20, 20, 22, 255)
        d.text((pw / 2, 6), unit, font=f, fill=ink, anchor="mm")
        d.line([(pw - 8, top), (pw - 8, bottom)], fill=(30, 30, 32, 255), width=1)
        if band:
            b0, b1, col = band
            ya = bottom - (b0 - lo) / (hi - lo) * (bottom - top)
            yb = bottom - (b1 - lo) / (hi - lo) * (bottom - top)
            d.rectangle([pw - 6, min(ya, yb), pw - 3, max(ya, yb)], fill=C.SWATCH_RGB[col] + (255,))
        if log:
            span = hi - lo
            for k in range(lo, hi + 1):
                y = bottom - (k - lo) / span * (bottom - top)
                d.line([(pw - 15, y), (pw - 9, y)], fill=ink, width=1)
                _decade(d, pw - 17, y, k, 8, ink)
                if k < hi:
                    for m in range(2, 10):
                        ym = bottom - (k - lo + math.log10(m)) / span * (bottom - top)
                        d.line([(pw - (12 if m == 5 else 11), ym), (pw - 9, ym)], fill=ink, width=1)
            return
        n = (majors - 1) * 5
        for k in range(n + 1):
            y = bottom - k / n * (bottom - top)
            major = k % 5 == 0
            d.line([(pw - (15 if major else 12), y), (pw - 9, y)], fill=ink, width=1)
            if major:
                v = lo + (hi - lo) * k / n
                d.text((pw - 17, y), _nice(round(v, 3)), font=_font(8), fill=ink, anchor="rm")
    return ATLAS.add(f"edge:{unit}:{lo}:{hi}:{majors}:{pw}x{ph}:{band}:{log}", pw, ph, draw)


def dial_face_tex(unit, lo, hi, majors, size, sweep=250, labels=None, black=False, sync=False):
    """A round meter's face (square case, recessed): the scale arc over sweep degrees from lower left to lower right,
    figures, the unit; or the synchroscope (SLOW / FAST, the 12 o'clock window)."""
    p = px(size)

    def draw(img):
        d = ImageDraw.Draw(img)
        face, ink = ((26, 28, 32), (236, 236, 228)) if black else ((240, 240, 232), (20, 20, 22))
        d.rectangle([0, 0, p - 1, p - 1], fill=(36, 38, 42, 255))
        d.ellipse([3, 3, p - 4, p - 4], fill=face + (255,))
        cx = cy = p / 2
        r = p / 2 - 6
        if sync:
            # The synchroscope: a full circle of marks, the in-phase window at 12 o'clock, SLOW and FAST.
            for k in range(24):
                a = math.radians(-90 + k * 15)
                r0 = r - (9 if k % 6 == 0 else 5)
                d.line([(cx + r0 * math.cos(a), cy + r0 * math.sin(a)), (cx + r * math.cos(a), cy + r * math.sin(a))], fill=ink + (255,), width=2)
            d.pieslice([cx - r, cy - r, cx + r, cy + r], -100, -80, fill=(40, 150, 70, 255))
            d.ellipse([cx - r + 10, cy - r + 10, cx + r - 10, cy + r - 10], fill=face + (255,))
            f = _font(max(8, int(p * 0.09)))
            d.text((cx - r * 0.5, cy + r * 0.35), "SLOW", font=f, fill=ink + (255,), anchor="mm")
            d.text((cx + r * 0.5, cy + r * 0.35), "FAST", font=f, fill=ink + (255,), anchor="mm")
            d.text((cx, cy + r * 0.62), unit, font=_font(max(7, int(p * 0.07))), fill=ink + (255,), anchor="mm")
            return
        a0 = 90 + (360 - sweep) / 2          # lower left (degrees, PIL's: clockwise from +x)
        n = (majors - 1) * 5
        for k in range(n + 1):
            a = math.radians(a0 + sweep * k / n)
            major = k % 5 == 0
            r0 = r - (9 if major else 5)
            d.line([(cx + r0 * math.cos(a), cy + r0 * math.sin(a)), (cx + r * math.cos(a), cy + r * math.sin(a))], fill=ink + (255,),
                   width=2 if major else 1)
            if major:
                v = lo + (hi - lo) * k / n
                label = labels[k // 5] if labels else _nice(round(v, 3))
                rt = r - 19
                d.text((cx + rt * math.cos(a), cy + rt * math.sin(a)), label, font=_font(max(8, int(p * 0.075))), fill=ink + (255,), anchor="mm")
        d.text((cx, cy + r * 0.45), unit, font=_font(max(8, int(p * 0.08))), fill=ink + (255,), anchor="mm")
    return ATLAS.add(f"dial:{unit}:{lo}:{hi}:{majors}:{p}:{sweep}:{labels}:{black}:{sync}", p, p, draw)


def chart_tex(w, h, scales):
    """A strip chart's paper behind its window: pale green, ruled, each pen's scale across the top in its colour."""
    pw, ph = px(w), px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(228, 238, 222, 255))
        for k in range(11):
            x = 3 + k * (pw - 7) / 10
            d.line([(x, 14), (x, ph)], fill=(150, 190, 160, 255) if k % 5 else (110, 160, 124, 255), width=1)
        for y in range(18, ph, 8):
            d.line([(0, y), (pw, y)], fill=(176, 204, 180, 255), width=1)
        f = _font(8)
        for i, (lo, hi, colour, name) in enumerate(scales):
            ink = C.SWATCH_RGB[colour] + (255,) if colour != "black" else (20, 20, 22, 255)
            y = 5 + 0
            if i == 0:
                d.text((2, y), name, font=f, fill=ink, anchor="lm")
            else:
                d.text((pw - 2, y), name, font=f, fill=ink, anchor="rm")
    return ATLAS.add(f"chart:{pw}x{ph}:{scales}", pw, ph, draw)


def bargraph_tex(w, h):
    """A controller's bar graph window: dark, its 0 to 100 scale ticked down the left."""
    pw, ph = px(w), px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(18, 20, 22, 255))
        for k in range(11):
            y = ph - 3 - k * (ph - 6) / 10
            d.line([(1, y), (5 if k % 5 else 8, y)], fill=(200, 200, 196, 255), width=1)
    return ATLAS.add(f"bar:{pw}x{ph}", pw, ph, draw)


def screen_tex(w, h):
    pw, ph = px(w), px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(8, 14, 12, 255))
    return ATLAS.add(f"screen:{pw}x{ph}", pw, ph, draw)


def button_tex(legend, size):
    """An illuminated push button's lens with its engraved legend, black on white (coloured when drawn, as a
    window)."""
    p = px(size)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, p - 1, p - 1], fill=(255, 255, 255, 255))
        _lines(d, legend.split("|"), 1, 1, p - 2, p - 2, (14, 14, 14, 255), 11, gap=1)
    return ATLAS.add(f"btn:{p}:{legend}", p, p, draw)


def arrow_tex(legend, size):
    """A controller's raise or lower button's face: a black arrow on white plastic."""
    p = px(size)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, p - 1, p - 1], fill=(214, 214, 208, 255))
        _lines(d, [legend], 1, 1, p - 2, p - 2, (14, 14, 14, 255), 12)
    return ATLAS.add(f"arrow:{p}:{legend}", p, p, draw)


def digit_tag_tex(tag, w, h):
    pw, ph = px(w), px(h)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(22, 22, 24, 255))
        _lines(d, tag.split("|"), 1, 1, pw - 2, ph - 2, (232, 232, 226, 255), 11)
    return ATLAS.add(f"dtag:{pw}x{ph}:{tag}", pw, ph, draw)


# ---------------------------------------------------------------- a live board's parts

def sw(name):
    return C.sw(name)


def solid_tex(rgb):
    """A plain patch of colour on the legend texture (an unlit lens)."""
    def draw(img):
        ImageDraw.Draw(img).rectangle([0, 0, img.width - 1, img.height - 1], fill=tuple(rgb) + (255,))
    return ATLAS.add("solid:%d,%d,%d" % tuple(rgb), 6, 6, draw)


LIT_LENS = {"red": C.pic("lamp_red"), "green": C.pic("lamp_green"), "white": C.pic("lamp_white"), "amber": C.pic("lamp_amber")}
# Unlit lenses: the coloured glass dull in the room's light.
OFF_RGB = {"red": (112, 34, 30), "green": (34, 82, 44), "white": (150, 150, 144), "amber": (128, 86, 30)}
OFF_LENS = {k: solid_tex(v) for k, v in OFF_RGB.items()}
DESK_DENSITY = 768              # desk nameplates are read closer: drawn finer


def _flat(*pts):
    return [round(float(c), 4) for p in pts for c in p]


class LiveBoard(C.Board):
    """A benchboard section whose parts read the plant (the parts' live entries; see the module's description)."""

    def __init__(self, wide, seed, title, face=C.FACE):
        super().__init__(wide, seed, face)
        self.title = title
        self.live = {"windows": [], "lamps": [], "needles": [], "bars": [], "digits": [], "pens": [], "screens": [],
                     "handles": [], "controls": []}

    # -- the hood's title and the annunciator boxes

    def hood_title(self):
        w = self.w
        x0, x1 = w / 2 - 0.75, w / 2 + 0.75
        mat = plate_tex(self.title, x1 - x0, 0.034, "black", 15)
        self.box(x0, C.TOP + 0.008, 0.972, x1, 3.0 - 0.008, 0.98, [mat] + [sw("label")] * 5)

    def ann_boxes(self, boxes, x_first=0.1, y_top=None):
        """Annunciator boxes across the top: each five windows across and three down, in a black frame; returns the
        boxes' bottom. boxes: lists of 15 (id, legend, colour, kind) read across then down (one box: at x_first)."""
        n = len(boxes)
        bw = 5 * WIN_W + 0.02
        gap = (self.w - 0.2 - n * bw) / max(1, n - 1)
        y_top = C.TOP - 0.02 if y_top is None else y_top
        y_bot = y_top - 3 * WIN_H - 0.02
        z = self.face - 0.03
        black = sw("black")
        for b, windows in enumerate(boxes):
            assert len(windows) == 15, (self.title, b, len(windows))
            x0 = x_first + b * (bw + gap)
            x1 = x0 + bw
            self.box(x0, y_bot, z, x1, y_top, self.face, [None, None, black, black, black, black])
            for q in ((x0, y_top - 0.01, x1, y_top), (x0, y_bot, x1, y_bot + 0.01), (x0, y_bot + 0.01, x0 + 0.01, y_top - 0.01),
                      (x1 - 0.01, y_bot + 0.01, x1, y_top - 0.01)):
                self.front_quad(*q, z, black, None)
            for k, (wid, legend, colour, kind) in enumerate(windows):
                i, j = k % 5, k // 5
                ax0, ax1 = x0 + 0.01 + i * WIN_W, x0 + 0.01 + (i + 1) * WIN_W
                ay1 = y_top - 0.01 - j * WIN_H
                ay0 = ay1 - WIN_H
                e = 0.006
                self.front_quad(ax0, ay0, ax0 + e, ay1, z, black, None)
                self.front_quad(ax1 - e, ay0, ax1, ay1, z, black, None)
                self.front_quad(ax0 + e, ay1 - e, ax1 - e, ay1, z, black, None)
                self.front_quad(ax0 + e, ay0, ax1 - e, ay0 + e, z, black, None)
                self.live["windows"].append({"id": wid, "q": _flat((ax0 + e, ay0 + e, ax1 - e, ay1 - e, z)),
                                             "uv": window_tex(legend), "c": colour, "k": kind, "t": legend})
        return y_bot

    def status_windows(self, x0, y1, cols, items, w=0.15, h=0.07):
        """A box of status lights (white legend windows, lit while true, never flashing): items (signal, legend) read
        across then down from the top left."""
        rows = (len(items) + cols - 1) // cols
        z = self.face - 0.025
        black = sw("black")
        bx1, by0 = x0 + cols * w + 0.02, y1 - rows * h - 0.02
        self.box(x0, by0, z, bx1, y1, self.face, [None, None, black, black, black, black])
        for q in ((x0, y1 - 0.01, bx1, y1), (x0, by0, bx1, by0 + 0.01), (x0, by0 + 0.01, x0 + 0.01, y1 - 0.01), (bx1 - 0.01, by0 + 0.01, bx1, y1 - 0.01)):
            self.front_quad(*q, z, black, None)
        for k, (signal, legend) in enumerate(items):
            i, j = k % cols, k // cols
            ax0, ay1 = x0 + 0.01 + i * w, y1 - 0.01 - j * h
            e = 0.005
            self.front_quad(ax0, ay1 - h, ax0 + e, ay1, z, black, None)
            self.front_quad(ax0 + w - e, ay1 - h, ax0 + w, ay1, z, black, None)
            self.front_quad(ax0 + e, ay1 - e, ax0 + w - e, ay1, z, black, None)
            self.front_quad(ax0 + e, ay1 - h, ax0 + w - e, ay1 - h + e, z, black, None)
            self.live["windows"].append({"id": signal, "q": _flat((ax0 + e, ay1 - h + e, ax0 + w - e, ay1 - e, z)),
                                         "uv": window_tex(legend, w - 0.01, h - 0.01, 10), "c": "white", "k": 1, "t": legend})
        return by0

    # -- plates

    def plate(self, x, y, text, w, h=0.03, style="white", size=None, d=0.006):
        """An engraved nameplate on the face, its middle at (x, y)."""
        edge = sw("label_white") if style == "white" else sw("label")
        self.face_box(x, y, w, h, d, plate_tex(text, w, h, style, size), edge)

    def desk_plate(self, x, s, text, w=0.12, t=0.032, style="white", size=None):
        """A nameplate lying on the desk at (x, s)."""
        mat = plate_tex(text, w, t, style, size, DESK_DENSITY)
        edge = sw("label_white") if style == "white" else sw("label")
        self.desk_block(x, s, w, t, 0.006, [edge, edge, edge, edge, mat, None])

    # -- lamps

    def lamp(self, signal, quad, colour, two=False):
        """A lamp's lens drawn by state: lit in its colour while the signal is true, else unlit (two: a mimic's
        two-colour lamp, red while true and green lit while false)."""
        entry = {"s": signal, "q": _flat(*quad), "on": LIT_LENS[colour], "off": OFF_LENS[colour]}
        if two:
            entry["off"] = LIT_LENS["green"]
            entry["off_lit"] = True
        self.live["lamps"].append(entry)

    def legend_lamp(self, signal, quad, legend, colour, size):
        """An illuminated legend lens (a push button's): lit in its colour while the signal is true, frosted
        otherwise (drawn as a window is)."""
        self.live["lamps"].append({"s": signal, "q": _flat(*quad), "on": button_tex(legend, size), "c": colour})

    def face_lamp_live(self, x, y, colour, signal, size=0.03, z=None, d=0.012, two=False):
        z = self.face if z is None else z
        rim = sw("mgrey")
        h = size / 2 + 0.005
        self.box(x - h, y - h, z - d, x + h, y + h, z, [None, None, rim, rim, rim, rim])
        zz = z - d
        self.lamp(signal, ((x - h, y + h, zz), (x - h, y - h, zz), (x + h, y - h, zz), (x + h, y + h, zz)), colour, two)

    def desk_lens(self, x, s, size, h=0.012):
        """The corners of a lens lying on the desk at (x, s), h above it."""
        import numpy as np
        c, t, n = self.on_desk(x, s, h)
        u = np.array([1.0, 0, 0])
        k = size / 2
        return (tuple(c - u * k + t * k), tuple(c - u * k - t * k), tuple(c + u * k - t * k), tuple(c + u * k + t * k))

    def desk_lamp_live(self, x, s, colour, signal, size=0.026):
        c, t, n = self.on_desk(x, s, 0.012)
        rim = sw("mgrey")
        self.obox(tuple(c), (1, 0, 0), tuple(n), size / 2 + 0.006, 0.012, size / 2 + 0.006, [rim, rim, rim, rim, None, None])
        self.lamp(signal, self.desk_lens(x, s, size + 0.012, 0.024), colour)

    def desk_button(self, x, s, legend, colour, signal, size=0.07, control=None):
        """An illuminated push button on the desk: its dark collar and its legend lens, lit by state."""
        c, t, n = self.on_desk(x, s, 0.012)
        rim = sw("black")
        self.obox(tuple(c), (1, 0, 0), tuple(n), size / 2 + 0.008, 0.012, size / 2 + 0.008, [rim, rim, rim, rim, None, None])
        self.legend_lamp(signal, self.desk_lens(x, s, size, 0.025), legend, colour, size)
        if control:
            self.control(control, x - size / 2 - 0.01, x + size / 2 + 0.01, s - size / 2 / 1.116 - 0.01, s + size / 2 / 1.116 + 0.01)

    def control(self, cid, x0, x1, s0, s1):
        self.live["controls"].append({"id": cid, "x0": round(x0, 4), "x1": round(x1, 4), "s0": round(s0, 4), "s1": round(s1, 4)})

    # -- switches

    def _handle(self, x, s, kind, angle, lst):
        """A switch's handle turned angle degrees (clockwise seen from above) about the plate's middle."""
        import numpy as np
        p, t, n = self.on_desk(x, s, 0.012)
        a = math.radians(angle)
        u = np.array([math.cos(a), 0, 0]) - t * math.sin(a)
        u /= np.linalg.norm(u)
        if kind == "knob":
            self.cyl(tuple(p), tuple(p + n * 0.045), 0.03, sw("black"), sw("dgrey"), 10, lst)
            # The knob's white pointer across its top.
            top = p + n * 0.0455
            w = np.cross(n, u)
            self.obox(tuple(top + u * 0.013), tuple(u), tuple(n), 0.013, 0.0006, 0.0035, sw("white"), lst)
            return
        ln = 0.075 if kind == "j" else 0.06
        self.obox(tuple(p + n * 0.02), tuple(u), tuple(n), ln / 2, 0.02, 0.012, sw("black"), lst)
        # The white pointer line along the handle's top.
        self.obox(tuple(p + n * 0.0402), tuple(u), tuple(n), ln / 2 - 0.006, 0.0004, 0.003, sw("white"), lst)
        if kind == "j":
            self.obox(tuple(p + n * 0.0535 + u * (ln / 2 - 0.009)), tuple(u), tuple(n), 0.009, 0.0105, 0.012,
                      [sw("black")] * 5 + [None], lst)

    def switch_live(self, x, s, text, kind="j", lamps=(("red", None), ("green", None)), control=None, collar=None, plate_w=0.12):
        """A control switch: its plate, its handle (turned to where it was last left, for a working one), its lamps
        above (colour, signal) and its nameplate below."""
        plate = sw(collar) if collar else sw("black")
        self.desk_block(x, s, 0.11, 0.11, 0.012, [plate] * 5 + [None])
        if control:
            ops = {}
            for state, angle in (("0", -45), ("1", 45), ("c", 0)):
                lst = []
                self._handle(x, s, kind, angle, lst)
                ops[state] = lst
            self.live["handles"].append({"id": control, "ops": ops})
            self.control(control, x - 0.065, x + 0.065, s - 0.065, s + 0.065)
        else:
            self._handle(x, s, kind, 0, None)
        for k, (colour, signal) in enumerate(lamps):
            lx = x + (k - (len(lamps) - 1) / 2) * 0.042
            if signal is None:
                self.lamp_on_desk(lx, s + 0.085, "off_" + colour)
            else:
                self.desk_lamp_live(lx, s + 0.085, colour, signal)
        if text:
            self.desk_plate(x, s - 0.082, text, plate_w)

    # -- instruments

    def edge_meter(self, x, y, value, lo, hi, unit, majors=6, band=None, tall=0.3, log=False):
        """An edgewise meter: its black case standing out of the board, the scale recessed behind the bezel, the
        pointer (the game's), the zero-adjust screw under the scale (log: a scale of decades lo..hi, value its log)."""
        z0, zf = self.face - 0.07, self.face - 0.056
        black, dark = sw("black"), sw("dgrey")
        x0, x1, y0, y1 = x - 0.05, x + 0.05, y - tall / 2, y + tall / 2
        self.box(x0, y0, z0, x1, y1, self.face, [None, None, black, black, black, black])
        fx0, fx1, fy0, fy1 = x - 0.04, x + 0.04, y0 + 0.03, y1 - 0.01
        for q in ((x0, fy1, x1, y1), (x0, y0, x1, fy0), (x0, fy0, fx0, fy1), (fx1, fy0, x1, fy1)):
            self.front_quad(*q, z0, black, None)
        # The recess's walls, then the scale.
        self.quad((fx0, fy1, zf), (fx0, fy1, z0), (fx1, fy1, z0), (fx1, fy1, zf), dark)
        self.quad((fx0, fy0, z0), (fx0, fy0, zf), (fx1, fy0, zf), (fx1, fy0, z0), dark)
        self.quad((fx0, fy1, zf), (fx0, fy0, zf), (fx0, fy0, z0), (fx0, fy1, z0), dark)
        self.quad((fx1, fy1, z0), (fx1, fy0, z0), (fx1, fy0, zf), (fx1, fy1, zf), dark)
        face = edge_face_tex(unit, lo, hi, majors, fx1 - fx0, fy1 - fy0, band, log)
        self.front_quad(fx0, fy0, fx1, fy1, zf, face, None)
        self.cyl((x, y0 + 0.015, z0), (x, y0 + 0.015, z0 - 0.004), 0.007, black, sw("mgrey"), 8)
        # The pointer's travel: the scale's ends (the drawing's top and bottom margins, 13 and 6 texels).
        top, bottom = fy1 - EDGE_TOP / LEGEND_PX, fy0 + EDGE_BOTTOM / LEGEND_PX
        self.live["needles"].append({"t": "edge", "v": value, "lo": lo, "hi": hi, "x0": round(fx0 + 0.006, 4), "x1": round(fx1 - 8 / LEGEND_PX, 4),
                                     "y0": round(bottom, 4), "y1": round(top, 4), "z": round(zf - 0.003, 4)})

    def dial_meter(self, x, y, size, value, lo, hi, unit, majors=6, labels=None, sweep=250, black_face=False, sync=False):
        """A square-cased round meter: the case, the face recessed behind its bezel, the pointer the game's."""
        z0, zf = self.face - 0.05, self.face - 0.038
        black, dark = sw("black"), sw("dgrey")
        h = size / 2
        self.box(x - h, y - h, z0, x + h, y + h, self.face, [None, None, black, black, black, black])
        b = 0.012
        fx0, fx1, fy0, fy1 = x - h + b, x + h - b, y - h + b, y + h - b
        for q in ((x - h, fy1, x + h, y + h), (x - h, y - h, x + h, fy0), (x - h, fy0, fx0, fy1), (fx1, fy0, x + h, fy1)):
            self.front_quad(*q, z0, black, None)
        self.quad((fx0, fy1, zf), (fx0, fy1, z0), (fx1, fy1, z0), (fx1, fy1, zf), dark)
        self.quad((fx0, fy0, z0), (fx0, fy0, zf), (fx1, fy0, zf), (fx1, fy0, z0), dark)
        self.quad((fx0, fy1, zf), (fx0, fy0, zf), (fx0, fy0, z0), (fx0, fy1, z0), dark)
        self.quad((fx1, fy1, z0), (fx1, fy0, z0), (fx1, fy0, zf), (fx1, fy1, zf), dark)
        face = dial_face_tex(unit, lo, hi, majors, fx1 - fx0, sweep, labels, black_face, sync)
        self.front_quad(fx0, fy0, fx1, fy1, zf, face, None)
        r = (fx1 - fx0) / 2 - 6 / LEGEND_PX
        a0, a1 = -(90 + (360 - sweep) / 2), -(90 + (360 - sweep) / 2 + sweep)
        entry = {"t": "sync" if sync else "dial", "v": value, "lo": lo, "hi": hi, "cx": round(x, 4), "cy": round(y, 4), "z": round(zf - 0.003, 4),
                 "r": round(r * 0.92, 4), "a0": a0 % 360, "a1": a0 % 360 + (a1 - a0), "white": black_face}
        self.live["needles"].append(entry)

    def recorder_live(self, x, y, wide, pens, title=None):
        """A strip chart recorder: its door frame and the chart behind the window (lit), the pens' traces the game's
        (pens: value, lo, hi, colour, name)."""
        h = 0.3
        z = self.face - 0.12
        self.face_box(x, y, wide, h, 0.12, None, sw("dgrey"))
        dark = sw("dgrey")
        self.front_quad(x - wide / 2, y + h / 2 - 0.04, x + wide / 2, y + h / 2, z, dark, None)
        self.front_quad(x - wide / 2, y - h / 2, x + wide / 2, y - h / 2 + 0.03, z, dark, None)
        self.front_quad(x - wide / 2, y - h / 2 + 0.03, x - wide / 2 + 0.025, y + h / 2 - 0.04, z, dark, None)
        self.front_quad(x + wide / 2 - 0.025, y - h / 2 + 0.03, x + wide / 2, y + h / 2 - 0.04, z, dark, None)
        cx0, cx1, cy0, cy1 = x - wide / 2 + 0.025, x + wide / 2 - 0.025, y - h / 2 + 0.03, y + h / 2 - 0.04
        chart = chart_tex(cx1 - cx0, cy1 - cy0, [(lo, hi, c, name) for _, lo, hi, c, name in pens])
        self.front_quad(cx0, cy0, cx1, cy1, z, chart, self.lit)
        self.live["pens"].append({"x0": round(cx0 + 0.006, 4), "x1": round(cx1 - 0.006, 4), "y0": round(cy0 + 0.004, 4),
                                  "y1": round(cy1 - 18 / LEGEND_PX, 4), "z": round(z - 0.002, 4),
                                  "pens": [{"v": v, "lo": lo, "hi": hi, "c": sw(c)} for v, lo, hi, c, _ in pens]})
        if title:
            self.plate(x, y + h / 2 + 0.022, title, wide, 0.03)

    def controller_live(self, x, y, value, auto, tag):
        """A manual/automatic controller station: its bar graph (the output, the game's), its AUTO (green) and MAN
        (amber) lamps, the raise and lower buttons, its tag above."""
        z = self.face - 0.09
        self.face_box(x, y, 0.12, 0.32, 0.09, sw("dgrey"), sw("dgrey"))
        self.face_box(x, y + 0.07, 0.07, 0.13, 0.095, None)
        bx0, bx1, by0, by1 = x - 0.035, x + 0.035, y + 0.005, y + 0.135
        self.front_quad(bx0, by0, bx1, by1, self.face - 0.095, bargraph_tex(bx1 - bx0, by1 - by0), self.lit)
        self.live["bars"].append({"v": value, "lo": 0, "hi": 100, "x0": round(bx0 + 0.012, 4), "x1": round(bx1 - 0.008, 4),
                                  "y0": round(by0 + 3 / LEGEND_PX, 4), "y1": round(by1 - 3 / LEGEND_PX, 4), "z": round(self.face - 0.097, 4)})
        for k, (colour, signal) in enumerate((("green", auto), ("amber", "!" + auto))):
            self.face_lamp_live(x - 0.025 + k * 0.05, y - 0.055, colour, signal, 0.03, z)
        for k, legend in enumerate(("▲", "▼")):
            bx = x - 0.025 + k * 0.05
            self.box(bx - 0.0175, y - 0.13, self.face - 0.1, bx + 0.0175, y - 0.1, z, [arrow_tex(legend, 0.035)] + [sw("mgrey")] * 5)
        self.plate(x, y + 0.18, tag, 0.13, 0.03)

    def digital_live(self, x, y, value, n, dp, tag, wide=0.21, tw=0.05):
        """A digital indicator: its black case, its engraved tag at the left, the red seven-segment figures (the
        game's) in the window (dp -1: scientific, a figure, its decimal and the power of ten: 2.3E-1)."""
        self.face_box(x, y, wide, 0.07, 0.035, sw("black"), sw("dgrey"))
        z = self.face - 0.035
        self.front_quad(x - wide / 2 + 0.006, y - 0.025, x - wide / 2 + 0.006 + tw, y + 0.025, z - 0.001, digit_tag_tex(tag, tw, 0.05), None)
        wx0, wx1 = x - wide / 2 + tw + 0.012, x + wide / 2 - 0.008
        self.front_quad(wx0, y - 0.025, wx1, y + 0.025, z - 0.001, sw("black"), None)
        self.live["digits"].append({"v": value, "n": n, "dp": dp, "x0": round(wx0 + 0.004, 4), "x1": round(wx1 - 0.004, 4),
                                    "y0": round(y - 0.019, 4), "y1": round(y + 0.019, 4), "z": round(z - 0.003, 4)})

    def screen_live(self, x, y, wide, high, sid):
        """A plant computer screen the game writes on: its bezel and dark glass (lit)."""
        z = self.bezel(x - wide / 2, y - high / 2, x + wide / 2, y + high / 2, 0.05)
        self.front_quad(x - wide / 2, y - high / 2, x + wide / 2, y + high / 2, z, screen_tex(wide, high), self.lit)
        self.live["screens"].append({"id": sid, "x0": round(x - wide / 2, 4), "y0": round(y - high / 2, 4), "x1": round(x + wide / 2, 4),
                                     "y1": round(y + high / 2, 4), "z": round(z - 0.003, 4)})

    def face_switch_live(self, x, y, text, lamps):
        """A switch on the vertical board: its plate, a knob with its pointer, its lamps above (colour, signal), its
        nameplate below."""
        self.face_box(x, y, 0.1, 0.1, 0.012, sw("black"))
        self.cyl((x, y, self.face - 0.012), (x, y, self.face - 0.05), 0.025, sw("black"), sw("dgrey"), 8)
        self.face_box(x, y + 0.012, 0.006, 0.022, 0.0505, sw("white"), sw("white"))
        for k, (colour, signal) in enumerate(lamps):
            self.face_lamp_live(x + (k - (len(lamps) - 1) / 2) * 0.042, y + 0.085, colour, signal, 0.026)
        self.plate(x, y - 0.075, text, 0.16, 0.03)

    # -- the mimic's live symbols

    def pump_live(self, x, y, signal, r=0.045):
        """A pump on a mimic: a white ring standing off the line, its lamp in the middle (red running, green stopped)."""
        self.cyl((x, y, self.face - 0.008), (x, y, self.face - 0.02), r, sw("white"), sw("white"), 12)
        k = r * 0.5
        zz = self.face - 0.0215
        self.lamp(signal, ((x - k, y + k, zz), (x - k, y - k, zz), (x + k, y - k, zz), (x + k, y + k, zz)), "red", two=True)

    def valve_live(self, x, y, signal):
        """A valve (or a breaker) on a mimic: its black square and its lamp, red open (closed, for a breaker) and
        green shut (open)."""
        self.face_box(x, y, 0.05, 0.05, 0.02, sw("black"))
        k = 0.016
        zz = self.face - 0.021
        self.lamp(signal, ((x - k, y + k, zz), (x - k, y - k, zz), (x + k, y - k, zz), (x + k, y + k, zz)), "red", two=True)

    # -- the annunciator's push buttons

    def ann_buttons(self, x, s):
        """The annunciator's ACKNOWLEDGE, RESET and TEST push buttons and their plate: ACK lit while an alarm is
        unacknowledged, RESET while one has cleared and waits to be reset, TEST while testing."""
        for k, (legend, colour, signal, cid) in enumerate((("ACK", "amber", "ann.unacked", "ack"), ("RESET", "white", "ann.cleared", "reset"),
                                                           ("TEST", "white", "ann.test", "test"))):
            self.desk_button(x + (k - 1) * 0.11, s, legend, colour, signal, 0.075, cid)
        self.desk_plate(x, s + 0.105, "ANNUNCIATOR", 0.3, 0.032, "black")

    # -- round 28: the guarded switches, push buttons on the face, the radiation monitors' drawers

    def guarded_live(self, x, s, cid, gid, lamps, text, plate_style="red", plate_w=0.2):
        """A switch under a hinged red guard (it works): the handle turned to its action ("0") or back in NORMAL ("1"),
        the guard over it ("0") or lifted ("1": the first click lifts it, the second works the switch; it drops again
        after five seconds), its lamps above (colour, signal) and its nameplate."""
        import numpy as np
        self.desk_block(x, s, 0.16, 0.16, 0.014, [sw("red")] * 5 + [None])
        p, t, n = self.on_desk(x, s, 0.014)
        ops = {}
        for state, angle in (("1", 0), ("0", -45)):
            lst = []
            a = math.radians(angle)
            u = np.array([math.cos(a), 0, 0]) - t * math.sin(a)
            u /= np.linalg.norm(u)
            self.obox(tuple(p + n * 0.03), tuple(u), tuple(n), 0.05, 0.03, 0.016, [sw("black")] * 5 + [None], lst)
            self.obox(tuple(p + n * 0.0605), tuple(u), tuple(n), 0.044, 0.0006, 0.004, sw("white"), lst)
            ops[state] = lst
        self.live["handles"].append({"id": cid, "ops": ops})
        guard = {"0": [], "1": []}
        for dx in (-0.075, 0.075):
            self.obox(tuple(p + n * 0.04 + np.array([dx, 0, 0])), (1, 0, 0), tuple(n), 0.006, 0.04, 0.07, [sw("red")] * 5 + [None], guard["0"])
            self.obox(tuple(p + n * 0.07 + t * 0.105 + np.array([dx, 0, 0])), (1, 0, 0), tuple(n), 0.006, 0.07, 0.035, [sw("red")] * 5 + [None],
                      guard["1"])
        self.obox(tuple(p + n * 0.072 + t * 0.03), (1, 0, 0), tuple(n), 0.074, 0.006, 0.034, sw("red"), guard["0"])
        self.obox(tuple(p + n * 0.137 + t * 0.105), (1, 0, 0), tuple(n), 0.084, 0.006, 0.03, sw("red"), guard["1"])
        self.live["handles"].append({"id": gid, "ops": guard})
        for k, (colour, signal) in enumerate(lamps):
            self.desk_lamp_live(x + (k - (len(lamps) - 1) / 2) * 0.05, s + 0.115, colour, signal)
        if text:
            self.desk_plate(x, s - 0.12, text, plate_w, 0.036, plate_style)
        self.control(cid, x - 0.085, x + 0.085, s - 0.085, s + 0.085)

    def face_button(self, x, y, legend, w=0.08, h=0.04, colour="white", base=0.0):
        """A square push button on the vertical board (a drawer's or a cabinet's, its front base out from the board's
        face): its black bezel, its legend engraved on its face."""
        edge = sw("mgrey")
        self.face_box(x, y, w + 0.012, h + 0.012, base + 0.008, sw("black"), sw("black"))
        self.face_box(x, y, w, h, base + 0.02, plate_tex(legend, w, h, colour), edge)

    def rad_drawer(self, x, y, n, name, unit, value, sci):
        """A radiation monitor's drawer (a digital ratemeter): its tag, the reading in scientific figures with its unit,
        its HIGH (red), ALERT (amber), FAIL (white) and OPERATE (green) lamps and their plates, its range plate, and
        its CHECK SOURCE and RESET push buttons. value: the reading's name; its lamps rm<n>.high, .alert, .fail, .ok."""
        w, h = 0.66, 0.29
        self.face_box(x, y, w, h, 0.05, sw("dgrey"), sw("dgrey"))
        z = self.face - 0.05
        tag = f"RM-{n}  {name}"
        self.face_box(x, y + 0.115, w - 0.04, 0.036, 0.056, plate_tex(tag, w - 0.04, 0.036, "white"), sw("label_white"))
        self.face_box(x - 0.12, y + 0.035, 0.36, 0.072, 0.085, sw("black"), sw("black"))
        zz = self.face - 0.085
        tw = 0.075
        self.front_quad(x - 0.3 + 0.006, y + 0.035 - 0.026, x - 0.3 + 0.006 + tw, y + 0.035 + 0.026, zz - 0.001, digit_tag_tex(unit, tw, 0.052), None)
        wx0, wx1 = x - 0.3 + tw + 0.014, x + 0.06 - 0.008
        self.front_quad(wx0, y + 0.035 - 0.026, wx1, y + 0.035 + 0.026, zz - 0.001, sw("black"), None)
        self.live["digits"].append({"v": value, "n": 4, "dp": -1, "x0": round(wx0 + 0.004, 4), "x1": round(wx1 - 0.004, 4),
                                    "y0": round(y + 0.035 - 0.02, 4), "y1": round(y + 0.035 + 0.02, 4), "z": round(zz - 0.003, 4)})
        for k, (colour, sig, legend) in enumerate((("red", "high", "HIGH"), ("amber", "alert", "ALERT"), ("white", "fail", "FAIL"),
                                                   ("green", "ok", "OPER"))):
            lx = x + 0.12 + k * 0.06
            self.face_lamp_live(lx, y + 0.05, colour, f"rm{n}.{sig}", 0.03, z)
            self.face_box(lx, y + 0.005, 0.054, 0.022, 0.054, plate_tex(legend, 0.054, 0.022, "black"), sw("label"))
        self.face_button(x - 0.24, y - 0.085, "CHECK|SOURCE", 0.09, 0.05, base=0.05)
        self.face_button(x - 0.13, y - 0.085, "RESET", 0.09, 0.05, base=0.05)
        rng = {"MR/HR": "10⁻² TO 10⁴ MR/HR", "R/HR": "1 TO 10⁷ R/HR", "CPM": "10 TO 10⁶ CPM"}[unit]
        self.face_box(x + 0.15, y - 0.085, 0.3, 0.03, 0.054, plate_tex("RANGE " + rng, 0.3, 0.03, "black"), sw("label"))

    def ops(self):
        out = super().ops()
        out["live"] = self.live
        return out


# ---------------------------------------------------------------- annunciator legends

def R(wid, legend):
    """A red alarm window (a trip)."""
    return (wid, legend, "red", 0)


def A(wid, legend):
    """An amber alarm window."""
    return (wid, legend, "amber", 0)


def S(wid, legend):
    """A white status window (lit while true; it never flashes or sounds)."""
    return (wid, legend, "white", 1)


RCS_WINDOWS = [
    # Reactor trip: the first out of the reactor protection system's trips, and its status.
    [R("trip_manual", "RX TRIP|MANUAL"), R("trip_otdt", "RX TRIP|OT DELTA-T"), R("trip_flow1", "RX TRIP|LO FLOW|1 LOOP"),
     R("trip_flow2", "RX TRIP|LO FLOW|2 LOOPS"), R("trip_turbine", "RX TRIP|TURBINE|TRIP"),
     R("trip_pzr_hi", "RX TRIP|PZR PRESS|HI"), R("trip_pzr_lo", "RX TRIP|PZR PRESS|LO"), R("trip_sg_lo", "RX TRIP|SG LEVEL|LO-LO"),
     R("trip_flux", "RX TRIP|PR FLUX|HI"), R("trip_si", "RX TRIP|SAFETY|INJECTION"),
     R("rx_tripped", "REACTOR|TRIPPED"), A("rtb_open", "RX TRIP|BREAKERS|OPEN"), A("rps_bypass", "REACTOR|PROTECTION|BYPASSED"),
     S("p8", "P-8|LO FLOW TRIP|ARMED"), S("p9", "P-9|TURB TRIP|ARMED")],
    # The reactor coolant pumps and loops.
    [A("rcp1_trip", "RCP 1A|TRIP"), A("rcp2_trip", "RCP 1B|TRIP"), A("rcp3_trip", "RCP 1C|TRIP"), A("rcp4_trip", "RCP 1D|TRIP"),
     A("rcp_ovld", "RCP|MOTOR|OVERLOAD"),
     A("loop1_flow", "LOOP 1|FLOW|LO"), A("loop2_flow", "LOOP 2|FLOW|LO"), A("loop3_flow", "LOOP 3|FLOW|LO"), A("loop4_flow", "LOOP 4|FLOW|LO"),
     A("loop_out", "RCS LOOP|NOT|AVAILABLE"),
     A("rcp1_seal", "RCP 1A|SEAL LEAKOFF|HI"), A("rcp2_seal", "RCP 1B|SEAL LEAKOFF|HI"), A("rcp3_seal", "RCP 1C|SEAL LEAKOFF|HI"),
     A("rcp4_seal", "RCP 1D|SEAL LEAKOFF|HI"), A("rcp_vib", "RCP|VIBRATION|HI")],
    # The core and the coolant's temperature.
    [A("tavg_hi", "TAVG|HI"), A("tavg_dev", "TAVG-TREF|DEVIATION"), A("otdt_alert", "OT DELTA-T|ROD STOP|RUNBACK"),
     A("cet_hi", "CORE EXIT|TEMP|HI"), A("subcool_lo", "RCS|SUBCOOLING|LO"),
     A("fuel_fail", "FUEL|FAILURE"), A("fuel_burnup", "FUEL|BURNUP|LIMIT"), A("core_empty", "CORE|NOT|LOADED"),
     A("refuel_open", "REFUELING|CAVITY|OPEN"), A("unit_incomplete", "REACTOR|UNIT|INCOMPLETE"),
     A("rod_limit", "ROD BANK|INSERTION|LIMIT"), A("rod_urgent", "ROD CONTROL|URGENT|FAILURE"), A("nis_dev", "NIS POWER|RANGE|DEVIATION"),
     S("sr_hv_off", "SOURCE|RANGE HV|OFF"), A("rcs_leak", "RCS|LEAK RATE|HI")],
    # The pressurizer and its relief tank.
    [A("pzr_p_hi", "PZR PRESS|HI"), A("pzr_p_lo", "PZR PRESS|LO"), A("porv_open", "PZR PORV|OPEN"), A("pzr_safety", "PZR SAFETY|VALVE|OPEN"),
     A("spray_open", "PZR SPRAY|VALVE|OPEN"),
     A("pzr_l_hi", "PZR LEVEL|HI"), A("pzr_l_lo", "PZR LEVEL|LO"), A("pzr_l_dev", "PZR LEVEL|DEVIATION"),
     S("backup_htrs", "PZR BACKUP|HEATERS|ON"), A("htr_trip", "PZR HEATER|GROUP|TRIP"),
     A("prt_p_hi", "PRT|PRESS|HI"), A("prt_t_hi", "PRT|TEMP|HI"), A("prt_l", "PRT|LEVEL|HI-LO"), S("ltop", "COLD|OVERPRESS|ARMED"),
     A("pzr_chan", "PZR PRESS|CHANNEL|FAILURE")],
    # Chemical and volume control.
    [A("vct_l_hi", "VCT|LEVEL|HI"), A("vct_l_lo", "VCT|LEVEL|LO"), A("chg_flow", "CHARGING|FLOW|HI-LO"), A("ltdn_flow_lo", "LETDOWN|FLOW|LO"),
     A("ltdn_t_hi", "LTDN HX|OUTLET|TEMP HI"),
     A("seal_inj_lo", "RCP SEAL|INJ FLOW|LO"), A("bast_l_lo", "BA TANK|LEVEL|LO"), A("ba_flow_dev", "BORIC ACID|FLOW|DEVIATION"),
     A("rmw_flow_dev", "RX M/U WTR|FLOW|DEVIATION"), A("chg_auto", "CHG PUMP|AUTO|START"),
     A("ltdn_iso", "LETDOWN|ISOLATED"), A("ltdn_p_hi", "LETDOWN|PRESS|HI"), A("demin_div", "DEMIN|FLOW|DIVERTED"),
     A("vct_p", "VCT|PRESS|HI-LO"), A("dilute_blk", "BORON|DILUTION|BLOCKED")],
    # Component cooling and the containment.
    [A("ccw_flow_lo", "CCW TO RCP|FLOW|LO"), A("ccw_surge", "CCW SURGE|TANK|LEVEL"), A("ccw_t_hi", "CCW HX|OUTLET|TEMP HI"),
     A("ccw_auto", "CCW PUMP|AUTO|START"), A("ccw_rad", "CCW|RADIATION|HI"),
     A("sump_hi", "CNTMT|SUMP LEVEL|HI"), A("rcdt_hi", "RCDT|LEVEL|HI"), A("cnt_p_hi", "CNTMT|PRESS|HI"), A("cnt_t_hi", "CNTMT|TEMP|HI"),
     A("cnt_rad", "CNTMT|RADIATION|HI"),
     A("accum_p", "ACCUM|PRESS|HI-LO"), A("accum_l", "ACCUM|LEVEL|HI-LO"), S("rhr_on", "RHR|COOLING|IN SERVICE"),
     A("cooldown", "RCS|COOLDOWN|RATE HI"), A("loop_dt_hi", "RCS LOOP|DELTA-T|HI")],
]


# ---------------------------------------------------------------- the reactor coolant board

ROW3 = 1.40                     # the instruments' row low on the board (round 27: down a little, the mimics above)
TAG_Y = 1.233                   # the tags under ROW3's meters


def rcs_board(seed=23):
    """Reactor coolant and chemical and volume control (round 27, live): the four loops round the vessel with their
    hot and cold leg temperatures and the coolant pumps (lit red running, green stopped) and the pressurizer; the
    loops' flows and the pumps' amps, the pressurizer's controllers and meters, the wide range pressure; on the right
    the charging and letdown mimic with the volume control tank, its recorders, meters, controllers, the plant
    computer's RCS page; on the desk the four coolant pump switches (they work), the pressurizer heaters, the relief
    valves and their blocks, the charging pumps and letdown valves, and the annunciator's buttons."""
    b = LiveBoard(6, seed, "REACTOR COOLANT SYSTEM")
    b.profile(False)
    b.hood_title()
    y_ann = b.ann_boxes(RCS_WINDOWS)
    b.plate(1.55, y_ann - 0.04, "REACTOR COOLANT LOOPS", 1.1, 0.04, "black")
    b.plate(4.45, y_ann - 0.04, "CHEMICAL AND VOLUME CONTROL", 1.3, 0.04, "black")
    # The loops round the vessel; the pressurizer on loop 2's hot leg.
    vx, vy = 1.5, 2.13
    b.vessel(vx, vy, 0.24, 0.52, "red")
    b.plate(vx, vy, "RX|VESSEL", 0.16, 0.06, "white", d=0.032)
    for k, (sx, sy) in enumerate(((0.45, 2.33), (2.55, 2.33), (0.45, 1.93), (2.55, 1.93))):
        n = k + 1
        left = sx < vx
        b.vessel(sx, sy, 0.2, 0.24, "lblue")
        b.plate(sx, sy, f"SG|{n}", 0.1, 0.07, "white", d=0.032)
        edge_v, edge_s = vx + (-0.12 if left else 0.12), sx + (0.1 if left else -0.1)
        mid = (edge_v + edge_s) / 2
        b.mimic([(edge_v, sy + 0.05), (edge_s, sy + 0.05)], "red")
        b.mimic([(edge_s, sy - 0.07), (edge_v, sy - 0.07)], "blue")
        b.pump_live(mid, sy - 0.07, f"rcp{n}.run", 0.04)
        b.plate(mid, sy + 0.105, f"LOOP {n}", 0.16, 0.032)
        b.digital_live(mid - 0.11, sy - 0.165, f"loop{n}.thot", 3, 0, "TH")
        b.digital_live(mid + 0.11, sy - 0.165, f"loop{n}.tcold", 3, 0, "TC")
    b.mimic([(2.28, 2.41), (2.28, 2.38)], "red")
    b.vessel(2.28, 2.47, 0.09, 0.12, "red")
    b.plate(2.28, 2.555, "PZR", 0.09, 0.03)
    # Under the loops: their flows, the pumps' amps, the pressurizer's controllers and meters, wide range pressure.
    for k in range(4):
        b.edge_meter(0.28 + k * 0.12, ROW3, f"loop{k + 1}.flow", 0, 120, "%", 7, (90, 120, "green"))
        b.plate(0.28 + k * 0.12, TAG_Y, str(k + 1), 0.05, 0.03)
        b.edge_meter(0.80 + k * 0.12, ROW3, f"rcp{k + 1}.amps", 0, 1000, "AMPS", 6)
        b.plate(0.80 + k * 0.12, TAG_Y, "1" + "ABCD"[k], 0.06, 0.03)
    b.plate(0.46, ROW3 + 0.185, "RCS LOOP FLOW", 0.46, 0.035, "black")
    b.plate(0.98, ROW3 + 0.185, "RCP MOTOR AMPS", 0.46, 0.035, "black")
    for k, (value, tag) in enumerate((("pzr.press_out", "PZR PRESS"), ("pzr.level_out", "PZR LEVEL"), ("pzr.spray_a", "SPRAY A"),
                                      ("pzr.spray_b", "SPRAY B"))):
        b.controller_live(1.38 + k * 0.17, ROW3, value, "auto", tag)
    b.edge_meter(2.12, ROW3, "pzr.press", 1700, 2500, "PSIG", 5, (2185, 2310, "green"))
    b.edge_meter(2.24, ROW3, "pzr.level", 0, 100, "%", 6)
    b.plate(2.18, ROW3 + 0.185, "PRESSURIZER", 0.24, 0.035, "black")
    b.plate(2.12, TAG_Y, "PRESS", 0.1, 0.03)
    b.plate(2.24, TAG_Y, "LEVEL", 0.1, 0.03)
    b.dial_meter(2.62, ROW3 + 0.02, 0.3, "rcs.press_wr", 0, 3000, "PSIG", 7)
    b.plate(2.62, ROW3 + 0.2, "RCS PRESS WR", 0.3, 0.035, "black")
    # Charging and letdown: the recorders, the meters, the mimic, the controllers, the screen, the tank's pressure.
    b.recorder_live(3.35, 2.33, 0.3, [("pzr.press", 1700, 2500, "red", "PZR P"), ("pzr.level", 0, 100, "blue", "PZR L")], "PZR PRESS / LEVEL")
    b.recorder_live(3.72, 2.33, 0.3, [("rcs.tavg", 530, 600, "red", "TAVG"), ("rcs.tref", 530, 600, "green", "TREF")], "TAVG / TREF")
    for k, (value, lo, hi, unit, majors, tag) in enumerate((
            ("cvcs.vct_level", 0, 100, "%", 6, "VCT|LEVEL"), ("cvcs.charging", 0, 150, "GPM", 4, "CHG|FLOW"),
            ("cvcs.letdown", 0, 150, "GPM", 4, "LTDN|FLOW"), ("cvcs.seal_inj", 0, 50, "GPM", 6, "SEAL INJ|FLOW"),
            ("cvcs.ltdn_press", 0, 600, "PSIG", 4, "LTDN|PRESS"), ("cvcs.ltdn_temp", 50, 350, "°F", 4, "LTDN|TEMP"),
            ("cvcs.bast_level", 0, 100, "%", 6, "BAST|LEVEL"))):
        x = 4.05 + k * 0.12
        b.edge_meter(x, 2.33, value, lo, hi, unit, majors)
        b.plate(x, 2.135, tag, 0.11, 0.045)
    b.recorder_live(5.48, 2.33, 0.36, [(f"loop{k + 1}.flow", 0, 120, c, f"L{k + 1}") for k, c in enumerate(("red", "blue", "green", "black"))],
                    "RCS LOOP FLOW")
    b.vessel(3.4, 1.95, 0.2, 0.26, "green")
    b.plate(3.4, 1.95, "VCT", 0.1, 0.035, d=0.032)
    b.mimic([(3.5, 2.02), (5.7, 2.02)], "green")
    b.mimic([(5.7, 1.88), (3.5, 1.88)], "lblue")
    b.mimic([(5.7, 2.02), (5.7, 1.88)], "green")
    for k in range(3):
        b.pump_live(3.85 + k * 0.25, 2.02, f"cvcs.chg_{'abc'[k]}", 0.04)
        b.plate(3.85 + k * 0.25, 1.94, "ABC"[k], 0.04, 0.03)
    b.vessel(4.85, 1.95, 0.3, 0.08, "dgrey")
    b.plate(4.85, 1.95, "REGEN HX", 0.2, 0.032, d=0.032)
    for x, signal, tag, y in ((5.3, "cvcs.chg_line", "CHG", 2.02), (5.5, "cvcs.aux_spray", "AUX SPR", 2.02), (4.1, "cvcs.ltdn_line", "LTDN ISO", 1.88),
                              (4.3, "cvcs.orifice", "ORIFICE", 1.88)):
        b.valve_live(x, y, signal)
        b.plate(x, y + (0.05 if y > 1.95 else -0.05), tag, 0.12, 0.03)
    for k, (value, tag) in enumerate((("cvcs.chg_out", "CHG FLOW"), ("cvcs.ltdn_p_out", "LTDN PRESS"), ("cvcs.ltdn_t_out", "LTDN TEMP"),
                                      ("cvcs.seal_out", "SEAL INJ"), ("cvcs.vct_out", "VCT M/U"))):
        b.controller_live(3.30 + k * 0.17, ROW3, value, "auto", tag)
    b.screen_live(4.6, ROW3 + 0.02, 0.56, 0.3, "rcs")
    b.plate(4.6, ROW3 + 0.205, "PLANT COMPUTER - RCS", 0.4, 0.035, "black")
    b.dial_meter(5.45, ROW3 + 0.02, 0.26, "cvcs.vct_press", 0, 100, "PSIG", 6)
    b.plate(5.45, ROW3 + 0.185, "VCT PRESS", 0.26, 0.035, "black")
    # The desk: the coolant pumps (they work), the heaters, relief valves and blocks, charging and letdown, the
    # annunciator.
    for k in range(4):
        n = k + 1
        b.switch_live(0.35 + k * 0.33, 0.58, f"RCP 1{'ABCD'[k]}", "j", (("red", f"rcp{n}.run"), ("green", f"!rcp{n}.run")), f"rcp{n}")
    b.mimic_desk([(0.2, 0.24), (1.45, 0.24)], "red")
    b.desk_plate(0.85, 0.36, "REACTOR COOLANT PUMPS", 0.5, 0.034, "black")
    for k, (tag, signal) in enumerate((("PZR HTR A", "pzr.htr_prop"), ("PZR HTR B", "pzr.htr_prop"), ("PZR HTR C", "pzr.htr_backup"),
                                       ("PZR HTR D", "pzr.htr_backup"), ("PZR HTR E", "pzr.htr_backup"))):
        b.switch_live(1.8 + k * 0.22, 0.74, tag, "knob", (("red", signal), ("green", "!" + signal)))
    for k in range(2):
        b.switch_live(1.9 + k * 0.4, 0.42, ("PORV 455A", "PORV 456")[k], "j", (("red", "pzr.porv"), ("green", "!pzr.porv")), collar="red")
        b.switch_live(2.1 + k * 0.4, 0.42, ("BLOCK 8000A", "BLOCK 8000B")[k], "knob", (("red", "plant.present"), ("green", "!plant.present")))
    for k, (tag, signal) in enumerate((("CHG PP A", "cvcs.chg_a"), ("CHG PP B", "cvcs.chg_b"), ("CHG PP C", "cvcs.chg_c"),
                                       ("LTDN LCV-459", "cvcs.ltdn_line"), ("ORIFICE 75", "cvcs.orifice"), ("ORIFICE 45", "cvcs.orifice45"),
                                       ("VCT LCV-112C", "plant.present"))):
        b.switch_live(3.3 + k * 0.3, 0.46 if k % 2 else 0.72, tag, "j", (("red", signal), ("green", "!" + signal)), plate_w=0.14)
    b.mimic_desk([(3.15, 0.22), (5.2, 0.22)], "green")
    b.ann_buttons(5.6, 0.5)
    return b


TURBINE_WINDOWS = [
    # Turbine trip: the first out, and the turbine's state.
    [R("tt_manual", "TURB TRIP|MANUAL"), R("tt_reactor", "TURB TRIP|REACTOR|TRIP"), R("tt_overspeed", "TURB TRIP|OVERSPEED"),
     R("tt_vacuum", "TURB TRIP|LO COND|VACUUM"), R("tt_thrust", "TURB TRIP|THRUST|BEARING"),
     R("tt_oil", "TURB TRIP|LO BRG|OIL PRESS"), R("tt_ehc", "TURB TRIP|LO EHC|PRESS"), R("tt_gen", "TURB TRIP|GENERATOR|LOCKOUT"),
     R("tt_vib", "TURB TRIP|HI|VIBRATION"), R("tt_sg", "TURB TRIP|SG LEVEL|HI-HI"),
     R("turb_tripped", "TURBINE|TRIPPED"), S("turb_latched", "TURBINE|LATCHED"), A("runback", "TURBINE|RUNBACK"),
     S("gen_online", "GENERATOR|ON LINE"), A("ost", "OVERSPEED|TEST|IN PROGRESS")],
    # The electro-hydraulic control and the valves.
    [A("ehc_p_lo", "EHC FLUID|PRESS|LO"), A("ehc_t_hi", "EHC FLUID|TEMP|HI"), A("ehc_auto", "EHC PUMP|AUTO|START"),
     A("ehc_filter", "EHC FILTER|DP|HI"), A("ehc_res", "EHC|RESERVOIR|LEVEL LO"),
     A("sv_closed", "STOP|VALVES|CLOSED"), A("cv_closed", "CONTROL|VALVES|CLOSED"), A("iv_closed", "INTERCEPT|VALVES|CLOSED"),
     A("vlv_test", "VALVE|TEST|IN PROGRESS"), A("speed_dev", "SPEED|NOT AT|1800 RPM"),
     A("load_limit", "LOAD|LIMITER|ACTIVE"), A("vpl", "VALVE POS|LIMITER|ACTIVE"), A("ehc_manual", "EHC|MANUAL|CONTROL"),
     A("accel_hi", "ACCEL|RATE|HI"), A("speed_chan", "SPEED|CHANNEL|FAILURE")],
    # Lubricating oil and the turbine's supervisory instruments.
    [A("brg_oil_p", "BRG OIL|PRESS|LO"), A("brg_oil_t", "BRG OIL|TEMP|HI"), A("lo_res", "LUBE OIL|RESERVOIR|LEVEL LO"),
     A("brg_metal", "BEARING|METAL|TEMP HI"), A("turb_vib", "TURBINE|VIBRATION|HI"),
     A("eccen_hi", "ECCENTRICITY|HI"), A("thrust_wear", "THRUST|BEARING|WEAR"), A("diff_exp", "DIFF|EXPANSION|HI"),
     A("shell_exp", "SHELL|EXPANSION|LIMIT"), A("tg_trip", "TURN GEAR|MOTOR|TRIP"),
     A("emerg_oil", "EMERG BRG|OIL PUMP|RUNNING"), A("seal_oil_p", "SEAL OIL|PRESS|LO"), A("seal_backup", "SEAL OIL|BACKUP PP|RUNNING"),
     S("lift_oil", "LIFT OIL|PUMPS|RUNNING"), S("turn_gear", "TURNING|GEAR|ENGAGED")],
    # The generator.
    [A("gen_bkr_open", "GEN BKR|OPEN"), A("gen_lockout", "GENERATOR|LOCKOUT|86G"), A("field_open", "GEN FIELD|BKR|OPEN"),
     A("loss_field", "GENERATOR|LOSS OF|FIELD"), A("rev_power", "GENERATOR|REVERSE|POWER"),
     A("h2_p_lo", "GEN H2|PRESS|LO"), A("h2_purity", "GEN H2|PURITY|LO"), A("stator_flow", "STATOR|COOLING|FLOW LO"),
     A("stator_t", "STATOR|WINDING|TEMP HI"), A("vr_manual", "VOLT REG|MANUAL"),
     A("over_exc", "GENERATOR|OVER|EXCITED"), A("neg_seq", "GEN NEG|SEQUENCE|CURRENT"), A("gen_freq", "GENERATOR|FREQUENCY|HI-LO"),
     A("gen_ground", "GEN STATOR|GROUND"), A("exc_trouble", "EXCITER|TROUBLE")],
    # The condenser and the steam dumps.
    [A("vac_lo", "COND|VACUUM|LO"), A("cw_flow_lo", "CIRC WATER|FLOW|LO"), A("hotwell", "HOTWELL|LEVEL|HI-LO"),
     S("dump_armed", "STEAM DUMP|ARMED"), A("dump_open", "STEAM DUMP|VALVES|OPEN"),
     A("sjae_rad", "AIR EJECTOR|RADIATION|HI"), A("hood_t_hi", "EXH HOOD|TEMP|HI"), A("cond_auto", "COND PUMP|AUTO|START"),
     A("cwp_trip", "CIRC WATER|PUMP|TRIP"), A("tower_basin", "CLG TOWER|BASIN|LEVEL LO"),
     A("msr_level", "MSR DRAIN|TANK|LEVEL HI"), A("msr_relief", "MSR|RELIEF|OPEN"), A("fwh_level", "FW HEATER|LEVEL|HI"),
     A("fwh_bypass", "FW HEATER|BYPASSED"), A("hdp_trip", "HTR DRAIN|PUMP|TRIP")],
    # The turbine's auxiliaries and its building.
    [A("gland_p", "GLAND|STEAM|PRESS LO"), A("gland_fan", "GLAND EXH|FAN|TRIP"), A("aux_steam", "AUX STEAM|HEADER|PRESS LO"),
     S("hp_drains", "HP TURBINE|DRAINS|OPEN"), A("hood_spray", "LP HOOD|SPRAY|ON"),
     A("tb_sump", "TURB BLDG|SUMP|LEVEL HI"), A("tb_fire", "TURB BLDG|FIRE|ALARM"), A("deluge", "BEARING|DELUGE|ACTUATED"),
     A("oil_fire", "LUBE OIL|FIRE|PROTECTION"), A("h2_seal_fire", "H2 SEAL|OIL|FIRE PROT"),
     A("isophase", "ISO PHASE|BUS COOLING|TROUBLE"), A("exc_cool", "EXCITER|COOLING|TROUBLE"), A("tg_ctrl_pwr", "T-G CONTROL|POWER|LOSS"),
     A("dehc_fail", "DEH|COMPUTER|FAILURE"), A("tsi_fail", "TSI|CHANNEL|FAILURE")],
]

TURBINE_STATUS = [("st.latched", "LATCHED"), ("st.tripped", "TRIPPED"), ("st.online", "ON LINE"), ("st.at_speed", "AT SPEED"),
                  ("st.turn_gear", "TURNING|GEAR"), ("st.lift_oil", "LIFT OIL|PUMPS"),
                  ("st.ehc_a", "EHC|PUMP A"), ("st.ehc_b", "EHC|PUMP B"), ("st.aux_oil", "AUX OIL|PUMP"), ("st.seal_oil", "SEAL OIL|PUMP"),
                  ("st.dump_armed", "STM DUMP|ARMED"), ("st.hood_spray", "HOOD|SPRAY"),
                  ("st.hp_drains", "HP DRAINS|OPEN"), ("st.load_limit", "LOAD|LIMITING"), ("st.oper_auto", "OPER|AUTO"),
                  ("st.turb_manual", "TURBINE|MANUAL"), ("st.valve_test", "VALVE|TEST"), ("st.ost", "OVERSPEED|TEST")]

EHC_BUTTONS = [
    [("OPER|AUTO", "ehc.oper_auto"), ("TURB|MANUAL", "ehc.turb_manual"), ("REF", "ehc.ref"), ("DEMAND", "ehc.demand"),
     ("GO", "ehc.go"), ("HOLD", "ehc.hold"), ("LATCH", "st.latched"), ("SINGLE|VALVE", "ehc.single")],
    [("SPEED|100", "ehc.s100"), ("SPEED|500", "ehc.s500"), ("SPEED|1800", "ehc.s1800"), ("ACCEL|SLOW", "ehc.slow"),
     ("ACCEL|MED", "ehc.med"), ("ACCEL|FAST", "ehc.fast"), ("OVER|SPEED", "ehc.ost"), ("VALVE|TEST", "ehc.vtest")],
    [("LOAD|RATE", "ehc.load_rate"), ("MW|IN", "ehc.mw_in"), ("MW|OUT", "ehc.mw_out"), ("IMP|IN", "ehc.imp_in"),
     ("IMP|OUT", "ehc.imp_out"), ("LOAD|LIMIT", "st.load_limit"), ("RUN|BACK", "ehc.runback"), ("CLEAR", "ehc.clear")],
]


def turbine_board(seed=41):
    """Turbine and generator (round 27, live): the generator's megawatts, megavars, volts and amps and the turbine's
    speed on big round meters, the recorders, the turbine's pressures, vacuum and oil; the steam path mimic (the stop
    valves lit open or shut), the turbine's status lights and digital readouts; the electro-hydraulic control's
    screen and controllers, the supervisory instruments, the generator's computer page; on the desk the EHC's lit
    push buttons (LATCH works), the oil pumps, the turbine trip under its guard (it works) and the auxiliaries."""
    b = LiveBoard(6, seed, "TURBINE GENERATOR")
    b.profile(False)
    b.hood_title()
    y_ann = b.ann_boxes(TURBINE_WINDOWS)
    b.plate(0.95, y_ann - 0.04, "GENERATOR OUTPUT", 0.9, 0.04, "black")
    b.plate(3.45, y_ann - 0.04, "TURBINE CONDITIONS", 0.9, 0.04, "black")
    b.plate(5.0, y_ann - 0.04, "GENERATOR AUXILIARIES", 1.0, 0.04, "black")
    # The generator's and the turbine's big meters, the recorders, the turbine's conditions.
    for k, (value, lo, hi, unit, majors, labels, tag) in enumerate((
            ("gen.mw", 0, 1500, "MW", 7, None, "GEN MW"), ("gen.mvar", -600, 600, "MVAR", 7, None, "GEN MVAR"),
            ("gen.kv", 0, 30, "KV", 7, None, "GEN KV"), ("gen.ka", 0, 40, "KA", 5, None, "GEN AMPS"))):
        x = 0.32 + k * 0.33
        b.dial_meter(x, 2.32, 0.28, value, lo, hi, unit, majors, labels)
        b.plate(x, 2.155, tag, 0.24, 0.03)
    b.dial_meter(1.78, 2.32, 0.34, "turb.speed", 0, 2000, "RPM", 5)
    b.plate(1.78, 2.125, "TURBINE SPEED", 0.32, 0.03)
    b.recorder_live(2.3, 2.3, 0.32, [("gen.mw", 0, 1500, "red", "MW"), ("gen.mvar", -600, 600, "blue", "MVAR")], "GEN MW / MVAR")
    b.recorder_live(2.68, 2.3, 0.32, [("turb.speed", 0, 2000, "red", "RPM"), ("turb.load_ref", 0, 1500, "green", "REF")], "SPEED / LOAD REF")
    for k, (value, lo, hi, unit, majors, tag) in enumerate((
            ("turb.throttle", 0, 1200, "PSIG", 4, "THROTTLE|PRESS"), ("turb.first_stage", 0, 1000, "PSIG", 6, "1ST STAGE|PRESS"),
            ("turb.vacuum", 0, 30, "IN HG", 4, "COND|VACUUM"), ("turb.hotwell", 0, 100, "%", 6, "HOTWELL|LEVEL"),
            ("turb.ehc_press", 0, 2000, "PSIG", 5, "EHC|PRESS"), ("turb.brg_oil", 0, 40, "PSIG", 5, "BRG OIL|PRESS"),
            ("turb.hood_temp", 0, 300, "°F", 4, "EXH HOOD|TEMP"), ("turb.gland", 0, 20, "PSIG", 5, "GLAND|STEAM"))):
        x = 3.05 + k * 0.12
        b.edge_meter(x, 2.32, value, lo, hi, unit, majors)
        b.plate(x, 2.125, tag, 0.11, 0.045)
    b.recorder_live(4.38, 2.3, 0.36, [("turb.vib1", 0, 15, "red", "BRG 1"), ("turb.vib2", 0, 15, "blue", "BRG 2")], "VIBRATION MILS")
    for k, (value, lo, hi, unit, majors, tag) in enumerate((
            ("gen.h2_press", 0, 80, "PSIG", 5, "H2|PRESS"), ("gen.stator_flow", 0, 800, "GPM", 5, "STATOR|CW FLOW"),
            ("gen.field_amps", 0, 6000, "AMPS", 4, "FIELD|AMPS"))):
        x = 4.75 + k * 0.12
        b.edge_meter(x, 2.32, value, lo, hi, unit, majors)
        b.plate(x, 2.125, tag, 0.11, 0.045)
    b.dial_meter(5.55, 2.32, 0.24, "gen.hz", 57, 63, "HZ", 7)
    b.plate(5.55, 2.155, "GEN FREQUENCY", 0.26, 0.03)
    # The steam path: main steam through the stop valves into the high pressure turbine, the three low pressure
    # turbines, the generator on the shaft.
    b.mimic([(0.18, 1.98), (0.8, 1.98)], "yellow")
    for k in range(4):
        b.valve_live(0.27 + k * 0.15, 1.98, "turb.valves")
        b.plate(0.27 + k * 0.15, 2.035, f"SV{k + 1}", 0.07, 0.028)
    b.vessel(0.95, 1.95, 0.3, 0.2, "dgrey")
    b.plate(0.95, 1.95, "HP", 0.1, 0.04, d=0.032)
    for k in range(3):
        b.vessel(1.45 + k * 0.45, 1.95, 0.34, 0.24, "dgrey")
        b.plate(1.45 + k * 0.45, 1.95, f"LP {'ABC'[k]}", 0.14, 0.04, d=0.032)
    b.vessel(2.85, 1.95, 0.42, 0.22, "purple")
    b.plate(2.85, 1.95, "GENERATOR", 0.3, 0.04, d=0.032)
    b.mimic([(1.1, 1.95), (1.28, 1.95)], "purple")
    b.mimic([(2.39, 1.95), (2.64, 1.95)], "purple")
    b.plate(1.5, 1.78, "MAIN STEAM AND TURBINE", 0.6, 0.032, "black")
    b.status_windows(3.22, 2.06, 6, TURBINE_STATUS)
    b.plate(3.7, 2.085, "TURBINE STATUS", 0.5, 0.03, "black")
    for k, (value, n, dp, tag) in enumerate((("gen.mw", 4, 0, "MW"), ("gen.mvar", 4, 0, "MVAR"), ("turb.speed", 4, 0, "RPM"),
                                            ("gen.hz", 4, 2, "HZ"))):
        b.digital_live(4.62 + (k % 2) * 0.62, 1.96 - (k // 2) * 0.1, value, n, dp, tag, 0.26)
    b.plate(4.93, 2.04, "GENERATOR DIGITAL", 0.5, 0.03, "black")
    # The EHC's screen and controllers, the supervisory instruments, the generator's page, exciter and hydrogen.
    b.screen_live(1.0, ROW3 + 0.02, 0.62, 0.3, "ehc")
    b.plate(1.0, ROW3 + 0.205, "DEH CONTROL", 0.4, 0.035, "black")
    for k, (value, tag) in enumerate((("ehc.load_ref", "LOAD REF"), ("ehc.load_limit", "LOAD LIMIT"), ("ehc.valve_pos", "VALVE POS"),
                                      ("ehc.speed_ref", "SPEED REF"))):
        b.controller_live(1.55 + k * 0.17, ROW3, value, "auto", tag)
    for k, (value, lo, hi, unit, majors, tag) in enumerate((
            ("tsi.eccen", 0, 10, "MILS", 6, "ECCEN"), ("tsi.diff_exp", -0.2, 0.8, "IN", 6, "DIFF|EXP"),
            ("tsi.shell_exp", 0, 2, "IN", 5, "SHELL|EXP"), ("tsi.thrust", -40, 40, "MILS", 5, "THRUST|POS"),
            ("tsi.brg_metal", 0, 300, "°F", 4, "BRG|METAL"), ("tsi.oil_temp", 50, 200, "°F", 4, "LUBE OIL|TEMP"))):
        x = 2.35 + k * 0.12
        b.edge_meter(x, ROW3, value, lo, hi, unit, majors)
        b.plate(x, TAG_Y - 0.008, tag, 0.11, 0.045)
    b.plate(2.65, ROW3 + 0.185, "TURBINE SUPERVISORY", 0.7, 0.035, "black")
    b.screen_live(3.75, ROW3 + 0.02, 0.56, 0.3, "gen")
    b.plate(3.75, ROW3 + 0.205, "PLANT COMPUTER - GEN", 0.4, 0.035, "black")
    b.dial_meter(4.35, ROW3 + 0.02, 0.22, "gen.exc_volts", 0, 600, "VOLTS", 4)
    b.plate(4.35, ROW3 + 0.165, "EXCITER", 0.22, 0.03)
    b.dial_meter(4.65, ROW3 + 0.02, 0.22, "gen.h2_purity", 80, 100, "%", 5)
    b.plate(4.65, ROW3 + 0.165, "H2 PURITY", 0.22, 0.03)
    for k, (text, signal) in enumerate((("TURN GEAR", "st.turn_gear"), ("EMERG OIL PP", "st.emerg_oil"), ("LIFT OIL PP", "st.lift_oil"))):
        b.face_switch_live(5.05 + k * 0.28, ROW3, text, (("red", signal), ("green", "!" + signal)))
    # The desk: the EHC's buttons, the oil pumps, the turbine trip, the auxiliaries, the annunciator.
    for j, row in enumerate(EHC_BUTTONS):
        for i, (legend, signal) in enumerate(row):
            colour = "amber" if legend in ("HOLD", "RUN|BACK", "OVER|SPEED", "VALVE|TEST") else "white"
            b.desk_button(0.3 + i * 0.105, 0.74 - j * 0.2, legend, colour, signal, 0.08, "latch" if legend == "LATCH" else None)
    b.desk_plate(0.67, 0.88, "DIGITAL ELECTRO-HYDRAULIC CONTROL", 0.7, 0.034, "black")
    for k, (tag, signal) in enumerate((("EHC PP A", "st.ehc_a"), ("EHC PP B", "st.ehc_b"), ("AUX OIL PP", "st.aux_oil"),
                                       ("SEAL OIL PP", "st.seal_oil"))):
        b.switch_live(1.5 + k * 0.28, 0.55, tag, "j", (("red", signal), ("green", "!" + signal)))
    trip_switch(b, 2.75, 0.5)
    for k, (tag, signal, kind) in enumerate((("LIFT OIL PP", "st.lift_oil", "j"), ("TURN GEAR", "st.turn_gear", "knob"),
                                             ("HOOD SPRAY", "st.hood_spray", "j"), ("GLAND EXH FAN", "plant.present", "j"),
                                             ("HP DRAINS", "st.hp_drains", "knob"), ("STEAM DUMP", "st.dump_armed", "knob"))):
        b.switch_live(3.3 + k * 0.36, 0.46 if k % 2 else 0.72, tag, kind, (("red", signal), ("green", "!" + signal)), plate_w=0.15)
    b.mimic_desk([(3.15, 0.22), (5.2, 0.22)], "purple")
    b.ann_buttons(5.6, 0.5)
    return b


def trip_switch(b, x, s):
    """The turbine trip switch under its hinged red guard (it works): the handle turned to TRIP or back in NORMAL,
    the guard over it, the lamps (red latched, green tripped) and its red nameplate."""
    b.guarded_live(x, s, "turbine_trip", "turbine_guard", (("red", "st.latched"), ("green", "!st.latched")), "TURBINE TRIP", plate_w=0.18)


ELECTRICAL_WINDOWS = [
    # The switchyard and the reserve auxiliary transformers.
    [A("swyd_v_lo", "345 KV|BUS|VOLT LO"), A("line1_trip", "345 KV|LINE 1|TRIP"), A("line2_trip", "345 KV|LINE 2|TRIP"),
     A("swyd_bkr", "SWYD BKR|TROUBLE"), A("swyd_batt", "SWYD|BATTERY|TROUBLE"),
     A("grid_freq", "GRID|FREQUENCY|HI-LO"), A("grid_volt", "GRID|VOLTAGE|HI-LO"), A("swyd_relay", "SWYD|RELAY|TROUBLE"),
     A("rat_a_trbl", "RES AUX|XFMR A|TROUBLE"), A("rat_b_trbl", "RES AUX|XFMR B|TROUBLE"),
     A("rat_lockout", "RES AUX|XFMR|LOCKOUT"), S("on_reserve", "AUX BUSES|ON RESERVE|XFMRS"), A("fast_xfer", "AUX BUS|FAST|TRANSFER"),
     A("rat_tap", "RES AUX|LTC|TROUBLE"), A("rat_cool", "RES AUX|COOLING|TROUBLE")],
    # The generator breaker and the main transformer.
    [A("gen_bkr_open", "GEN BKR|OPEN"), A("gen_bkr_trip", "GEN BKR|AUTO TRIP"), S("sync_perm", "SYNC CHECK|PERMISSIVE"),
     S("close_blocked", "GEN BKR|CLOSE|BLOCKED"), A("gen_limited", "GEN|OUTPUT|LIMITED"),
     A("mt_temp", "MAIN XFMR|OIL TEMP|HI"), A("mt_cool", "MAIN XFMR|COOLING|TROUBLE"), A("mt_lockout", "MAIN XFMR|LOCKOUT|86T"),
     A("mt_press", "MAIN XFMR|SUDDEN|PRESSURE"), A("mt_missing", "MAIN XFMR|NOT|CONNECTED"),
     A("uat_trbl", "UNIT AUX|XFMR|TROUBLE"), A("iso_cool", "ISO PHASE|BUS COOLING|TROUBLE"), A("gen_86", "GENERATOR|LOCKOUT|86G"),
     A("field_gnd", "EXCITER|FIELD|GROUND"), A("gen_bkr_air", "GEN BKR|AIR PRESS|LO")],
    # The 4160 V safety buses.
    [A("b1a_uv", "BUS 1A|UNDER|VOLTAGE"), A("b1b_uv", "BUS 1B|UNDER|VOLTAGE"), A("b1a_dv", "BUS 1A|DEGRADED|VOLTAGE"),
     A("b1b_dv", "BUS 1B|DEGRADED|VOLTAGE"), A("b1a_lock", "BUS 1A|LOCKOUT"),
     A("b1b_lock", "BUS 1B|LOCKOUT"), A("b1a_gnd", "BUS 1A|GROUND"), A("b1b_gnd", "BUS 1B|GROUND"), S("b1a_edg", "BUS 1A|ON EDG A"),
     S("b1b_edg", "BUS 1B|ON EDG B"),
     A("rhr_a_auto", "RHR PP A|AUTO START"), A("rhr_b_auto", "RHR PP B|AUTO START"), A("si_a_auto", "SI PP A|AUTO START"),
     A("si_b_auto", "SI PP B|AUTO START"), A("sequencer", "LOAD|SEQUENCER|ACTIVE")],
    # Diesel generator A.
    [S("edg_a_run", "EDG A|RUNNING"), A("edg_a_fail", "EDG A|START|FAILURE"), A("edg_a_day", "EDG A|DAY TANK|LEVEL LO"),
     A("edg_a_na", "EDG A|NOT|AVAILABLE"), S("edg_a_bkr", "EDG A|OUTPUT BKR|CLOSED"),
     A("edg_a_lo", "EDG A|LUBE OIL|PRESS LO"), A("edg_a_jw", "EDG A|JACKET WTR|TEMP HI"), A("edg_a_os", "EDG A|OVERSPEED"),
     A("edg_a_air", "EDG A|START AIR|PRESS LO"), A("edg_a_local", "EDG A|CONTROL|LOCAL"),
     A("edg_a_trbl", "EDG A|TROUBLE"), A("edg_a_fo", "EDG A|FUEL OIL|STORAGE LO"), A("edg_a_crank", "EDG A|CRANKCASE|PRESS HI"),
     A("edg_a_vib", "EDG A|VIBRATION|HI"), A("edg_a_gen", "EDG A|GEN|FAULT")],
    # Diesel generator B.
    [S("edg_b_run", "EDG B|RUNNING"), A("edg_b_fail", "EDG B|START|FAILURE"), A("edg_b_day", "EDG B|DAY TANK|LEVEL LO"),
     A("edg_b_na", "EDG B|NOT|AVAILABLE"), S("edg_b_bkr", "EDG B|OUTPUT BKR|CLOSED"),
     A("edg_b_lo", "EDG B|LUBE OIL|PRESS LO"), A("edg_b_jw", "EDG B|JACKET WTR|TEMP HI"), A("edg_b_os", "EDG B|OVERSPEED"),
     A("edg_b_air", "EDG B|START AIR|PRESS LO"), A("edg_b_local", "EDG B|CONTROL|LOCAL"),
     A("edg_b_trbl", "EDG B|TROUBLE"), A("edg_b_fo", "EDG B|FUEL OIL|STORAGE LO"), A("edg_b_crank", "EDG B|CRANKCASE|PRESS HI"),
     A("edg_b_vib", "EDG B|VIBRATION|HI"), A("edg_b_gen", "EDG B|GEN|FAULT")],
    # 480 V and DC.
    [A("lc_v_lo", "480V LC|BUS|VOLT LO"), A("lc_main_open", "480V LC|MAIN BKR|OPEN"), A("lc_fdr_open", "480V LC|FEEDER|BKR OPEN"),
     A("mcc_a_lo", "MCC 1A|BUS|VOLT LO"), A("mcc_b_lo", "MCC 1B|BUS|VOLT LO"),
     A("dc_a_lo", "125 VDC|BUS A|VOLT LO"), A("dc_b_lo", "125 VDC|BUS B|VOLT LO"), A("chg_a_trbl", "BATT CHGR|A|TROUBLE"),
     A("chg_b_trbl", "BATT CHGR|B|TROUBLE"), A("dc_gnd", "125 VDC|GROUND"),
     A("inv_a", "INVERTER|A|TROUBLE"), A("inv_b", "INVERTER|B|TROUBLE"), A("vital_lo", "VITAL BUS|VOLT LO"), A("ups_batt", "UPS|ON|BATTERY"),
     A("instr_lo", "120 VAC|INSTR BUS|VOLT LO")],
]

ELECTRICAL_STATUS = [("st.offsite", "OFFSITE|AVAILABLE"), ("st.gen_online", "GENERATOR|ON LINE"), ("st.uat", "UNIT AUX|XFMR"),
                     ("st.reserve", "RES AUX|XFMRS"), ("st.edg_a_ready", "EDG A|READY"), ("st.edg_b_ready", "EDG B|READY"),
                     ("st.lc", "480V LC|ENERGIZED"), ("st.lc_main", "LC MAIN|BKR CLOSED"), ("st.mcc_a", "MCC 1A|ENERGIZED"),
                     ("st.mcc_b", "MCC 1B|ENERGIZED"), ("st.dc_a", "125 VDC|BUS A"), ("st.dc_b", "125 VDC|BUS B")]

# The safety buses' breakers, left to right on each bus: (x, tag, signal, kind) kind "down" a load, "rat" the reserve
# transformer's feed from above, "edg" the diesel's from below.
BUS_1A = [(1.12, "RHR A", "bus.rhr_a", "down"), (1.32, "SI A", "bus.si_a", "down"), (1.55, "RES XFMR A", "bus.rat_a", "rat"),
          (1.8, "EDG A", "edg_a.bkr", "edg"), (2.05, "CCW A", "bus.ccw_a", "down"), (2.28, "SW A", "bus.sw_a", "down"),
          (2.51, "CHG A", "bus.chg_a", "down"), (2.74, "LC 1A", "bus.lc_1a", "down")]
BUS_1B = [(3.26, "LC 1B", "bus.lc_1b", "down"), (3.49, "CHG B", "bus.chg_b", "down"), (3.72, "SW B", "bus.sw_b", "down"),
          (3.95, "CCW B", "bus.ccw_b", "down"), (4.2, "EDG B", "edg_b.bkr", "edg"), (4.45, "RES XFMR B", "bus.rat_b", "rat"),
          (4.68, "SI B", "bus.si_b", "down"), (4.88, "RHR B", "bus.rhr_b", "down")]
SWYD_Y, BUS_Y = 2.47, 1.98


def electrical_board(seed=53):
    """Offsite power, the generator and the station's buses (round 27, live): the 345 kV switchyard over the main
    transformer and the generator (its breaker lit closed or open) and the two reserve auxiliary transformers; the
    4160 V safety buses below with their loads' breakers and the diesel generators; the status lights of the 480 V
    and DC systems; the buses' and diesels' meters, the switchyard's and generator's dials and the synchroscope; on
    the desk the generator breaker (it works: it closes through the synchroscope), every bus breaker in a mimic of its
    bus (the RES XFMR breakers work: the test switch), the diesels' start and stop buttons (they work) and their
    governor and voltage knobs."""
    b = LiveBoard(6, seed, "ELECTRICAL DISTRIBUTION")
    b.profile(False)
    b.hood_title()
    y_ann = b.ann_boxes(ELECTRICAL_WINDOWS)
    b.plate(1.2, y_ann - 0.04, "345 KV SWITCHYARD AND GENERATOR", 1.3, 0.04, "black")
    b.plate(4.0, y_ann - 0.04, "4160 V SAFETY BUSES", 1.0, 0.04, "black")
    b.mimic([(0.2, SWYD_Y), (5.0, SWYD_Y)], "black", 0.04)
    # The generator: its breaker off the switchyard, the main transformer, the generator.
    gx = 0.45
    b.mimic([(gx, SWYD_Y), (gx, 2.06)], "black")
    b.valve_live(gx, 2.38, "gen.bkr")
    b.plate(gx + 0.15, 2.38, "GEN BKR", 0.16, 0.03)
    b.vessel(gx, 2.24, 0.16, 0.1, "dgrey")
    b.plate(gx + 0.2, 2.24, "MAIN XFMR", 0.2, 0.03)
    b.pump_live(gx, 2.02, "st.gen_online", 0.06)
    b.plate(gx + 0.15, 2.02, "GEN", 0.08, 0.03)
    # The reserve auxiliary transformers down to the buses.
    for x, tag, sig in ((1.55, "RES AUX XFMR A", "swyd.rat_a"), (4.45, "RES AUX XFMR B", "swyd.rat_b")):
        b.mimic([(x, SWYD_Y), (x, BUS_Y)], "black")
        b.valve_live(x, 2.39, sig)
        b.vessel(x, 2.25, 0.16, 0.1, "dgrey")
        b.plate(x + (0.22 if x < 3 else -0.22), 2.25, tag, 0.26, 0.03)
    for x0, x1, bus, label in ((1.0, 2.85, BUS_1A, "BUS 1A"), (3.15, 5.0, BUS_1B, "BUS 1B")):
        b.mimic([(x0, BUS_Y), (x1, BUS_Y)], "black", 0.04)
        b.plate(x0 + 0.1 if x0 < 3 else x1 - 0.1, BUS_Y + 0.05, label, 0.13, 0.03, "black")
        for x, tag, sig, kind in bus:
            if kind == "rat":
                b.valve_live(x, BUS_Y + 0.09, sig)
            elif kind == "edg":
                b.mimic([(x, BUS_Y), (x, 1.76)], "black")
                b.valve_live(x, 1.89, sig)
                b.pump_live(x, 1.76, "edg_a.running" if x < 3 else "edg_b.running", 0.05)
                b.plate(x, 1.675, tag, 0.12, 0.03)
            else:
                b.mimic([(x, BUS_Y), (x, 1.80)], "black")
                b.valve_live(x, 1.89, sig)
                b.plate(x, 1.77, tag, 0.13, 0.028)
    b.status_windows(5.15, 2.46, 2, ELECTRICAL_STATUS, 0.33, 0.075)
    b.plate(5.49, 2.485, "480 V AND DC STATUS", 0.6, 0.03, "black")
    # The meters: bus 1A and diesel A, the switchyard and generator dials, the synchroscope, bus 1B and diesel B, DC.
    for side, (x0, bus, edg) in enumerate(((0.25, "1A", "a"), (3.35, "1B", "b"))):
        for k, (value, lo, hi, unit, majors, tag) in enumerate((
                (f"bus{bus.lower()}.kv", 0, 5, "KV", 6, "BUS|KV"), (f"bus{bus.lower()}.amps", 0, 3000, "AMPS", 4, "BUS|AMPS"),
                (f"edg_{edg}.kv", 0, 5, "KV", 6, "EDG|KV"), (f"edg_{edg}.amps", 0, 1000, "AMPS", 6, "EDG|AMPS"),
                (f"edg_{edg}.kw", 0, 6000, "KW", 4, "EDG|KW"), (f"edg_{edg}.kvar", -2000, 4000, "KVAR", 4, "EDG|KVAR"),
                (f"edg_{edg}.hz", 55, 65, "HZ", 6, "EDG|HZ"), (f"edg_{edg}.rpm", 0, 600, "RPM", 4, "EDG|RPM"))):
            x = x0 + k * 0.12
            b.edge_meter(x, ROW3, value, lo, hi, unit, majors)
            b.plate(x, TAG_Y - 0.008, tag, 0.11, 0.045)
        b.plate(x0 + 0.06, ROW3 + 0.185, f"BUS {bus}", 0.22, 0.035, "black")
        b.plate(x0 + 0.54, ROW3 + 0.185, f"DIESEL GENERATOR {edg.upper()}", 0.66, 0.035, "orange" if edg == "a" else "green")
    b.dial_meter(1.35, ROW3 + 0.02, 0.22, "swyd.kv", 0, 400, "KV", 5)
    b.plate(1.35, ROW3 + 0.165, "SWYD KV", 0.22, 0.03)
    b.dial_meter(1.65, ROW3 + 0.02, 0.22, "gen.mw", 0, 1500, "MW", 7)
    b.plate(1.65, ROW3 + 0.165, "GEN MW", 0.22, 0.03)
    b.dial_meter(3.0, ROW3 + 0.02, 0.32, "sync.phase", 0, 360, "SYNCHROSCOPE", 2, sync=True, black_face=True)
    b.plate(3.0, ROW3 + 0.235, "GEN BKR SYNC", 0.32, 0.035, "black")
    for k in range(2):
        b.face_lamp_live(2.78 + k * 0.44, ROW3 + 0.12, "white", "sync.lamp", 0.04)
    b.dial_meter(4.55, ROW3 + 0.02, 0.22, "gen.mvar", -600, 600, "MVAR", 7)
    b.plate(4.55, ROW3 + 0.165, "GEN MVAR", 0.22, 0.03)
    b.dial_meter(4.85, ROW3 + 0.02, 0.22, "gen.hz", 57, 63, "HZ", 7)
    b.plate(4.85, ROW3 + 0.165, "GEN HZ", 0.22, 0.03)
    for k, (value, lo, hi, unit, majors, tag) in enumerate((
            ("dc_a.volts", 0, 150, "VDC", 4, "DC A|VOLTS"), ("dc_a.amps", -200, 400, "AMPS", 4, "DC A|AMPS"),
            ("dc_b.volts", 0, 150, "VDC", 4, "DC B|VOLTS"), ("dc_b.amps", -200, 400, "AMPS", 4, "DC B|AMPS"))):
        x = 5.25 + k * 0.12
        b.edge_meter(x, ROW3, value, lo, hi, unit, majors)
        b.plate(x, TAG_Y - 0.008, tag, 0.11, 0.045)
    b.plate(5.43, ROW3 + 0.185, "125 VDC", 0.46, 0.035, "black")
    # The desk: the generator breaker and its companions, each bus's breakers in its mimic, the diesels, the annunciator.
    b.switch_live(0.3, 0.55, "GEN BKR", "j", (("red", "gen.bkr"), ("green", "!gen.bkr")), "gen_bkr", plate_w=0.13)
    b.switch_live(0.56, 0.55, "FIELD BKR", "j", (("red", "gen.field"), ("green", "!gen.field")))
    b.switch_live(0.82, 0.55, "VOLT REG", "knob", (("red", "plant.present"), ("green", "!plant.present")))
    b.desk_plate(0.56, 0.78, "GENERATOR", 0.36, 0.034, "black")
    for x0, x1, bus in ((1.0, 2.85, BUS_1A), (3.15, 5.0, BUS_1B)):
        b.mimic_desk([(x0, 0.86), (x1, 0.86)], "black", 0.03)
        for x, tag, sig, kind in bus:
            b.mimic_desk([(x, 0.86), (x, 0.66)], "black")
            # (diesels make real power, 27 October 2026: the RES XFMR breakers work, the test switch: res_a, res_b)
            control = ("res_a" if x < 3 else "res_b") if kind == "rat" else None
            b.switch_live(x, 0.53, tag, "j", (("red", sig), ("green", "!" + sig)), control, plate_w=0.18 if len(tag) > 6 else 0.13)
    for edg, x in (("a", 1.62), ("b", 4.02)):
        name = edg.upper()
        b.desk_button(x, 0.22, f"EDG {name}|START", "white", f"edg_{edg}.running", 0.075, f"edg_{edg}_start")
        b.desk_button(x + 0.11, 0.22, f"EDG {name}|STOP", "white", f"!edg_{edg}.running", 0.075, f"edg_{edg}_stop")
        b.switch_live(x + 0.27, 0.22, "GOVERNOR", "knob", (), plate_w=0.13)
        b.switch_live(x + 0.45, 0.22, "VOLTAGE", "knob", (), plate_w=0.13)
        b.desk_plate(x + 0.22, 0.345, f"DIESEL GENERATOR {name}", 0.62, 0.034, "orange" if edg == "a" else "green")
    b.ann_buttons(5.5, 0.5)
    return b


# ---------------------------------------------------------------- round 28: the safety systems board

ESF_WINDOWS = [
    # Safety injection: its actuation, its signals, the isolations it brings.
    [R("si_act", "SAFETY|INJECTION|ACTUATED"), A("si_man_a", "SI|MANUAL|TRAIN A"), A("si_man_b", "SI|MANUAL|TRAIN B"),
     A("si_pzr", "SI|PZR PRESS|LO"), A("si_steam", "SI|STEAMLINE|PRESS LO"),
     A("si_cnt", "SI|CNTMT PRESS|HI-1"), A("si_timer", "SI RESET|TIMER|RUNNING"), S("p11", "P-11|PZR PRESS|BELOW"),
     S("p4", "P-4|REACTOR|TRIPPED"), S("si_blocked", "LO PZR P|SI|BLOCKED"),
     A("fwi", "FEEDWATER|ISOLATION"), A("cia", "CNTMT ISOL|PHASE A"), A("cib", "CNTMT ISOL|PHASE B"), A("msi", "MAIN STEAM|ISOLATION"),
     A("cs_act", "CNTMT|SPRAY|ACTUATED")],
    # The emergency core cooling pumps.
    [A("ccp_a_as", "CCP A|AUTO START"), A("ccp_b_as", "CCP B|AUTO START"), A("sip_a_as", "SI PUMP A|AUTO START"),
     A("sip_b_as", "SI PUMP B|AUTO START"), A("rhr_a_as", "RHR PUMP A|AUTO START"),
     A("rhr_b_as", "RHR PUMP B|AUTO START"), A("si_mini", "SI PUMPS|ON|MINIFLOW"), A("rhr_mini", "RHR PUMPS|ON|MINIFLOW"),
     S("bit_flow", "BIT|INJECTION|FLOW"), A("cold_inj", "ECCS|COLD LEG|INJECTION"),
     A("sip_trip", "SI PUMP|MOTOR|TRIP"), A("rhr_trip", "RHR PUMP|MOTOR|TRIP"), A("ccp_trip", "CCP|MOTOR|TRIP"),
     A("eccs_vlv", "ECCS VALVE|NOT IN|SI POSITION"), A("eccs_ovld", "ECCS PUMP|MOTOR|OVERLOAD")],
    # The refuelling water storage tank, the accumulators, the boron injection tank.
    [A("rwst_lo", "RWST|LEVEL|LO"), A("rwst_lolo", "RWST|LEVEL|LO-LO"), A("rwst_empty", "RWST|EMPTY"), A("rwst_temp", "RWST|TEMP|HI-LO"),
     A("recirc", "SWAPOVER|TO COLD LEG|RECIRC"),
     A("acc1_dis", "ACCUM 1|DISCHARGING"), A("acc2_dis", "ACCUM 2|DISCHARGING"), A("acc3_dis", "ACCUM 3|DISCHARGING"),
     A("acc4_dis", "ACCUM 4|DISCHARGING"), S("acc_iso", "ACCUM|ISOL VALVES|CLOSED"),
     A("bit_lo", "BIT|LEVEL|LO"), A("bit_trace", "BIT|HEAT TRACE|TROUBLE"), A("sump_hi", "CNTMT|RECIRC SUMP|LEVEL HI"),
     S("chg_rwst", "CHG SUCTION|FROM|RWST"), A("acc_p_lo", "ACCUM|PRESS|LO")],
    # The containment.
    [A("cnt_p1", "CNTMT|PRESS|HI-1"), A("cnt_p3", "CNTMT|PRESS|HI-3"), A("cnt_temp", "CNTMT|AIR TEMP|HI"), S("cia_done", "PHASE A|ISOLATION|COMPLETE"),
     A("cia_fail", "PHASE A|VALVE NOT|CLOSED"),
     A("purge_iso", "CNTMT PURGE|ISOLATED"), A("cfc_slow", "CNTMT FAN|COOLERS|SLOW SPEED"), A("cfc_trip", "CNTMT FAN|COOLER|TRIP"),
     A("h2_hi", "CNTMT|HYDROGEN|HI"), A("h2_rec", "H2|RECOMBINER|ON"),
     A("airlock", "PERSONNEL|AIRLOCK|DOOR OPEN"), A("cnt_hirad", "CNTMT|HIGH RANGE|RAD HI"), A("cnt_sump", "CNTMT|NORMAL SUMP|LEVEL HI"),
     A("cs_a_as", "CS PUMP A|AUTO START"), A("cs_b_as", "CS PUMP B|AUTO START")],
    # Component cooling, service water, residual heat removal.
    [A("ccw_a_as", "CCW PUMP A|AUTO START"), A("ccw_b_as", "CCW PUMP B|AUTO START"), A("sw_a_as", "SW PUMP A|AUTO START"),
     A("sw_b_as", "SW PUMP B|AUTO START"), A("ccw_hx_t", "CCW HX|OUTLET|TEMP HI"),
     A("sw_p_lo", "SERVICE WTR|HEADER|PRESS LO"), A("ccw_surge", "CCW SURGE|TANK|LEVEL LO"), A("rhr_hx_flow", "RHR HX|CCW FLOW|LO"),
     S("rhr_sdc", "RHR|SHUTDOWN|COOLING"), S("rhr_suct", "RHR SUCTION|FROM|HOT LEGS"),
     A("ccw_nonsafe", "CCW|NON-ESS HDR|ISOLATED"), A("ccw_rad", "CCW|RADIATION|HI"), A("sw_strainer", "SW STRAINER|DIFF PRESS|HI"),
     A("uhs_temp", "ULTIMATE|HEAT SINK|TEMP HI"), A("ccw_rhr", "CCW TO|RHR HX|FLOW LO")],
    # The safeguards' power, sequencers and protection cabinets.
    [A("edg_a_si", "EDG A|AUTO START|ON SI"), A("edg_b_si", "EDG B|AUTO START|ON SI"), A("seq_a", "SEQUENCER A|LOADING"),
     A("seq_b", "SEQUENCER B|LOADING"), A("ssps_a", "SSPS|TRAIN A|GENERAL WARN"),
     A("ssps_b", "SSPS|TRAIN B|GENERAL WARN"), A("esf_byp_a", "ESF TRAIN A|BYPASSED"), A("esf_byp_b", "ESF TRAIN B|BYPASSED"),
     A("slave_test", "SLAVE RELAY|TEST|IN PROGRESS"), A("esf_dc_a", "ESF TRAIN A|DC POWER|LOSS"),
     A("esf_dc_b", "ESF TRAIN B|DC POWER|LOSS"), A("esfas_trbl", "ESFAS|CABINET|TROUBLE"), A("cr_emerg", "CR HVAC|EMERGENCY|MODE"),
     S("afw_signal", "AFW|AUTO START|SIGNAL"), A("load_shed", "LOAD SHED|SIGNAL")],
]

# The ESF status panel: each piece of equipment's window lit when it is where safety injection puts it (or running).
ESF_STATUS = [
    ("ccp_a.run", "CCP A|RUNNING"), ("ccp_b.run", "CCP B|RUNNING"), ("sip_a.run", "SI PP A|RUNNING"), ("sip_b.run", "SI PP B|RUNNING"),
    ("rhr_a.run", "RHR PP A|RUNNING"), ("rhr_b.run", "RHR PP B|RUNNING"), ("ccw_a.run", "CCW PP A|RUNNING"), ("ccw_b.run", "CCW PP B|RUNNING"),
    ("sw_a.run", "SW PP A|RUNNING"), ("sw_b.run", "SW PP B|RUNNING"), ("cfc_a.slow", "CFC 1A|SLOW"), ("cfc_b.slow", "CFC 1B|SLOW"),
    ("si.on", "BIT INLET|8803A OPEN"), ("si.on", "BIT INLET|8803B OPEN"), ("si.on", "BIT OUTLET|8801A OPEN"), ("si.on", "BIT OUTLET|8801B OPEN"),
    ("si.on", "RWST-CCP|112D OPEN"), ("si.on", "RWST-CCP|112E OPEN"), ("si.on", "VCT OUTLET|112B SHUT"), ("si.on", "VCT OUTLET|112C SHUT"),
    ("si.on", "CHG ISOL|8105 SHUT"), ("si.on", "CHG ISOL|8106 SHUT"), ("cia.on", "LTDN ISOL|459 SHUT"), ("cia.on", "LTDN ISOL|460 SHUT"),
    ("cia.on", "CNTMT ISOL|A COMPLETE"), ("cia.on", "CNTMT ISOL|B COMPLETE"), ("cia.on", "PURGE SUPPLY|SHUT"), ("cia.on", "PURGE EXH|SHUT"),
    ("fwi.sg1", "FW ISOL|SG 1"), ("fwi.sg2", "FW ISOL|SG 2"), ("fwi.sg3", "FW ISOL|SG 3"), ("fwi.sg4", "FW ISOL|SG 4"),
    ("afw_a.run", "AFW PP A|RUNNING"), ("afw_b.run", "AFW PP B|RUNNING"), ("afw_td.run", "TD AFW PP|RUNNING"), ("afw.valves", "AFW|VALVES OPEN"),
    ("edg_a.running", "EDG A|RUNNING"), ("edg_b.running", "EDG B|RUNNING"), ("seq_a.done", "SEQ A|COMPLETE"), ("seq_b.done", "SEQ B|COMPLETE"),
    ("hv.emergency", "CR HVAC|EMERG MODE"), ("hv.efu_0", "CREFS A|RUNNING"), ("hv.efu_1", "CREFS B|RUNNING"), ("si.on", "AUX BLDG|EXH FILTER"),
    ("bd.iso", "SG BLOWDN|ISOLATED"), ("bd.iso", "SG SAMPLE|ISOLATED"), ("cia.on", "RCP SEAL|RETURN SHUT"), ("cia.on", "MAIN STM|DRAINS SHUT"),
]


def esf_board(seed=11):
    """Engineered safety features (round 28, live): each safeguards train (A on the left in orange, B on the right in
    blue) with its pumps' flows and pressures, its recorder, the refuelling water storage tank's level and its
    accumulators; its mimic (the charging, safety injection and residual heat removal pumps from the tank, through the
    boron injection tank and the heat exchanger to the cold legs, every pump and valve lit as it stands); its
    controllers, readouts and the containment's fan coolers; between the trains the ESF status panel (each piece of
    equipment's window lit where safety injection puts it) and the containment's and the coolant's pressures; on the
    desk each train's pump and valve switches (lit as they stand), the two manual safety injection switches under
    their guards (they work: full actuation), the SI RESET buttons (after a minute) and the annunciator's buttons."""
    b = LiveBoard(6, seed, "ENGINEERED SAFETY FEATURES")
    b.profile(False)
    b.hood_title()
    y_ann = b.ann_boxes(ESF_WINDOWS)
    for x, colour, name, accums in ((0.1, "orange", "A", (1, 2)), (3.45, "blue", "B", (3, 4))):
        t = name.lower()
        b.plate(x + 1.22, y_ann - 0.04, f"SAFEGUARDS TRAIN {name}", 1.1, 0.04, colour)
        # The pumps' flows and pressures, the recorder, the tank, the accumulators.
        for k, (value, lo, hi, unit, majors, tag) in enumerate((
                (f"ccp_{t}.flow", 0, 200, "GPM", 5, "CCP|FLOW"), (f"sip_{t}.flow", 0, 800, "GPM", 5, "SI PUMP|FLOW"),
                (f"rhr_{t}.flow", 0, 5000, "GPM", 6, "RHR|FLOW"), (f"ccp_{t}.press", 0, 3000, "PSIG", 4, "CCP|DISCH"),
                (f"sip_{t}.press", 0, 2000, "PSIG", 5, "SI PUMP|DISCH"), (f"rhr_{t}.press", 0, 600, "PSIG", 4, "RHR PUMP|DISCH"),
                (f"rhr_{t}.hx_out", 50, 350, "°F", 4, "RHR HX|OUT TEMP"), (f"ccw_{t}.flow", 0, 10000, "GPM", 6, "CCW|FLOW"))):
            xm = x + 0.12 + k * 0.12
            b.edge_meter(xm, 2.32, value, lo, hi, unit, majors)
            b.plate(xm, 2.125, tag, 0.11, 0.045)
        b.recorder_live(x + 1.26, 2.3, 0.32, [(f"sip_{t}.flow", 0, 800, "red", "SI"), (f"rhr_{t}.flow", 0, 5000, "blue", "RHR")],
                        f"ECCS FLOW {name}")
        b.dial_meter(x + 1.66, 2.32, 0.26, "rwst.level", 0, 100, "%", 6)
        b.plate(x + 1.66, 2.155, "RWST LEVEL", 0.24, 0.03)
        for k, (value, lo, hi, unit, majors, tag) in enumerate((
                (f"acc{accums[0]}.press", 0, 800, "PSIG", 5, f"ACC {accums[0]}|PRESS"), (f"acc{accums[0]}.level", 0, 100, "%", 6, f"ACC {accums[0]}|LEVEL"),
                (f"acc{accums[1]}.press", 0, 800, "PSIG", 5, f"ACC {accums[1]}|PRESS"), (f"acc{accums[1]}.level", 0, 100, "%", 6, f"ACC {accums[1]}|LEVEL"))):
            xm = x + 1.94 + k * 0.12
            b.edge_meter(xm, 2.32, value, lo, hi, unit, majors)
            b.plate(xm, 2.125, tag, 0.11, 0.045)
        # The mimic: the tank, three pumps' lines through their valves, the boron injection tank and the heat
        # exchanger, the header to the cold legs.
        b.vessel(x + 0.2, 1.93, 0.2, 0.3, "lblue")
        b.plate(x + 0.2, 1.93, "RWST", 0.1, 0.034, d=0.032)
        rows = ((2.04, "ccp", "CCP", (f"112{'DE'[accums[0] // 3]}", "si.on"), (f"8803{name}", "si.on"), "BIT", (f"8801{name}", "si.on")),
                (1.93, "sip", "SI PP", (f"8923{name}", "plant.present"), (f"8821{name}", "plant.present"), None, (f"8835{name}", "plant.present")),
                (1.82, "rhr", "RHR PP", (f"8812{name}", "rhr.rwst"), (f"8809{name}", "plant.present"), "RHR HX", (f"8716{name}", "plant.present")))
        for yy, pump, tag, (v1, s1), (v2, s2), mid, (v3, s3) in rows:
            b.mimic([(x + 0.3, yy), (x + (2.4 if pump == "sip" else 2.15), yy)], colour)
            b.valve_live(x + 0.38, yy, s1)
            b.plate(x + 0.38, yy + 0.035, v1, 0.08, 0.024)
            b.plate(x + 0.52, yy + 0.035, tag, 0.1, 0.024, "black")
            b.pump_live(x + 0.66, yy, f"{pump}_{t}.run", 0.04)
            b.valve_live(x + 0.95, yy, s2)
            b.plate(x + 0.95, yy + 0.035, v2, 0.08, 0.024)
            if mid:
                b.vessel(x + 1.3, yy, 0.2, 0.06, "dgrey")
                b.plate(x + 1.3, yy, mid, 0.12, 0.028, d=0.032)
            b.valve_live(x + 1.65, yy, s3)
            b.plate(x + 1.65, yy + 0.035, v3, 0.08, 0.024)
        b.mimic([(x + 2.15, 2.04), (x + 2.15, 1.82)], colour)
        b.plate(x + 2.3, 1.975, "COLD LEGS", 0.16, 0.026)
        # Controllers, readouts, the containment's fan coolers and sprays.
        for k, (value, tag) in enumerate(((f"rhr_{t}.hx_ctl", "RHR HX FCV"), (f"rhr_{t}.byp_ctl", "RHR BYPASS"), (f"ccw_{t}.ctl", "CCW HX TCV"))):
            b.controller_live(x + 0.2 + k * 0.17, ROW3, value, "auto", tag)
        b.digital_live(x + 0.9, 1.48, f"rhr_{t}.hx_out", 3, 0, "RHR|°F", 0.26)
        b.digital_live(x + 0.9, 1.38, f"sw_{t}.press", 3, 0, "SW|PSIG", 0.26)
        b.digital_live(x + 0.9, 1.28, "rwst.level", 3, 1, "RWST|%", 0.26)
        b.plate(x + 0.9, 1.56, "READOUTS", 0.26, 0.03, "black")
        for k, (text, signal) in enumerate(((f"CFC 1{name} FAST", f"cfc_{t}.fast"), (f"CFC 1{name} SLOW", f"cfc_{t}.slow"),
                                            (f"H2 RECOMB {name}", f"h2r_{t}"), (f"CS PUMP {name}", f"cs_{t}.run"), (f"SPRAY ADD {name}", f"csa_{t}"))):
            b.face_switch_live(x + 1.25 + k * 0.25, ROW3, text, (("red", signal), ("green", "!" + signal)))
        b.plate(x + 1.75, ROW3 + 0.185, f"CONTAINMENT COOLING {name}", 0.9, 0.035, "black")
        b.plate(x + 0.37, ROW3 + 0.235, "RHR AND CCW", 0.46, 0.03, "black")
        # The desk: the pumps (back row) and valves (front row), lit as they stand.
        for k, (tag, signal) in enumerate(((f"CCP 1{name}", f"ccp_{t}.run"), (f"SI PP 1{name}", f"sip_{t}.run"), (f"RHR PP 1{name}", f"rhr_{t}.run"),
                                           (f"CS PP 1{name}", f"cs_{t}.run"), (f"CCW PP 1{name}", f"ccw_{t}.run"), (f"SW PP 1{name}", f"sw_{t}.run"),
                                           (f"CFC 1{name}", f"cfc_{t}.run"), (f"EBFS 1{name}", f"ebfs_{t}.run"))):
            b.switch_live(x + 0.2 + k * 0.29, 0.74, tag, "knob" if k % 3 == 1 else "j", (("red", signal), ("green", "!" + signal)), collar=colour,
                          plate_w=0.14)
        b.desk_plate(x + 1.22, 0.93, f"TRAIN {name} PUMPS AND FANS", 0.8, 0.032, colour)
        for k, (tag, signal) in enumerate(((f"112{'DE'[accums[0] // 3]} RWST", "si.on"), (f"8803{name} BIT IN", "si.on"),
                                           (f"8801{name} BIT OUT", "si.on"), (f"8835{name} SI", "plant.present"),
                                           (f"8809{name} RHR", "plant.present"), (f"8812{name} RWST", "rhr.rwst"),
                                           (f"CIV PHASE {name}", "cia.open"), (f"FWIV TRAIN {name}", "fwi.open"))):
            b.switch_live(x + 0.2 + k * 0.29, 0.44, tag, "j", (("red", signal), ("green", "!" + signal)), plate_w=0.17)
        b.mimic_desk([(x + 0.06, 0.24), (x + 2.38, 0.24)], colour)
        b.desk_plate(x + 1.22, 0.29, f"TRAIN {name} VALVES", 0.6, 0.032, colour)
    # Between the trains: the ESF status panel, the containment's and the coolant's pressures.
    b.plate(3.0, y_ann - 0.04, "ESF STATUS", 0.7, 0.04, "black")
    b.status_windows(2.585, 2.52, 4, ESF_STATUS, 0.205, 0.072)
    b.dial_meter(2.78, ROW3 - 0.02, 0.26, "cnt.press", -5, 55, "PSIG", 7)
    b.plate(2.78, ROW3 + 0.13, "CNTMT PRESS", 0.26, 0.03)
    b.dial_meter(3.22, ROW3 - 0.02, 0.26, "rcs.press_wr", 0, 3000, "PSIG", 7)
    b.plate(3.22, ROW3 + 0.13, "RCS PRESS WR", 0.26, 0.03)
    # The manual safety injection (it works, both trains), the resets, the annunciator.
    for x, name in ((2.78, "A"), (3.22, "B")):
        t = name.lower()
        b.guarded_live(x, 0.54, f"si_{t}", f"si_{t}_guard", (("red", "si.on"), ("green", "!si.on")), f"SI TRAIN {name}")
        b.desk_button(x, 0.86, f"SI RESET|TRAIN {name}", "white", "si.reset_ok", 0.08, f"si_reset_{t}")
    b.desk_plate(3.0, 0.97, "MANUAL SAFETY INJECTION", 0.62, 0.032, "red")
    b.ann_buttons(3.0, 0.17)
    return b


# ---------------------------------------------------------------- round 28: the steam and feedwater board

def _per_sg(make):
    return [make(n) for n in range(1, 5)]


SECONDARY_WINDOWS = [
    # The steam generators' levels.
    _per_sg(lambda n: A(f"sg{n}_lo", f"SG {n}|LEVEL|LO")) + [A("sg_hi", "SG|LEVEL|HI")]
    + _per_sg(lambda n: R(f"sg{n}_lolo", f"SG {n}|LEVEL|LO-LO")) + [A("sg_hihi", "SG LEVEL|HI-HI|TURB TRIP")]
    + _per_sg(lambda n: A(f"sg{n}_dev", f"SG {n}|LEVEL|DEVIATION")) + [A("sg_dry", "SG|WIDE RANGE|LEVEL LO")],
    # Steam: flow against feed, pressure, the relief valves.
    _per_sg(lambda n: A(f"sf{n}_mis", f"SG {n}|STM / FEED|MISMATCH")) + [A("stm_hdr_lo", "STEAM HDR|PRESS|LO")]
    + _per_sg(lambda n: A(f"sp{n}_lo", f"SG {n}|STEAM PRESS|LO")) + [A("stm_hdr_hi", "STEAM HDR|PRESS|HI")]
    + _per_sg(lambda n: A(f"arv{n}_open", f"SG {n}|ARV|OPEN")) + [A("mssv", "MAIN STEAM|SAFETY VLV|LIFTED")],
    # The main steam isolation valves and the steam dumps.
    _per_sg(lambda n: A(f"msiv{n}_shut", f"MSIV {n}|CLOSED")) + [A("msi2", "MAIN STEAM|ISOLATION")]
    + [A("msiv_air", "MSIV|AIR ACCUM|PRESS LO"), A("msiv_byp", "MSIV|BYPASS|OPEN"), A("stm_drain", "MAIN STEAM|DRAIN POT|LEVEL HI"),
       A("n16", "MAIN STEAM|N-16|RAD HIGH"), A("stm_break", "STEAM LINE|BREAK|DETECTED")]
    + [S("p12", "P-12|LO-LO TAVG|BELOW"), S("dump_armed2", "STEAM DUMP|ARMED"), A("dump_open2", "STEAM DUMP|VALVES|OPEN"),
       A("cond_na", "CONDENSER|NOT|AVAILABLE"), A("dump_blk", "STEAM DUMP|BLOCKED|MSIVS SHUT")],
    # Main feedwater and condensate.
    [A("mfp_a_trip", "MFP A|TRIP"), A("mfp_b_trip", "MFP B|TRIP"), A("fwi2", "FEEDWATER|ISOLATION"), A("frv_shut", "MAIN FRVs|CLOSED"),
     A("fw_hdr_lo", "FW HEADER|PRESS|LO"),
     A("mfp_a_man", "MFP A|SPEED CTRL|MANUAL"), A("mfp_b_man", "MFP B|SPEED CTRL|MANUAL"), A("frv_man", "FRV|CONTROL|MANUAL"),
     A("fw_temp_lo", "FEEDWATER|TEMP|LO"), A("fwh_iso", "FW HEATER|STRING|ISOLATED"),
     A("cond_p_lo", "CONDENSATE|HDR PRESS|LO"), A("cbp_trip", "COND BOOST|PUMP|TRIP"), A("hotwell2", "HOTWELL|LEVEL|HI-LO"),
     A("dea_lvl", "DEAERATOR|LEVEL|HI-LO"), A("fw_cond", "FEEDWATER|CONDUCTIVITY|HI")],
    # Auxiliary feedwater.
    [S("afw_a_run", "AFW PP A|RUNNING"), S("afw_b_run", "AFW PP B|RUNNING"), S("afw_td_run", "TD AFW PP|RUNNING"), A("afw_as", "AFW|AUTO START"),
     A("afw_flow_lo", "AFW|FLOW|LO"),
     A("afw_a_trip", "AFW PP A|TRIP"), A("afw_b_trip", "AFW PP B|TRIP"), A("afw_td_trip", "TD AFW PP|TRIP"), A("afw_td_os", "TD AFW PP|OVERSPEED"),
     A("afw_suct", "AFW PUMP|SUCTION|PRESS LO"),
     A("cst_lo", "CST|LEVEL|LO"), A("cst_lolo", "CST|LEVEL|LO-LO"), A("afw_sw", "AFW SUCTION|ON SERVICE|WATER"), A("afw_hdr_hi", "AFW|DISCH PRESS|HI"),
     S("td_steam", "TD AFW|STEAM SUPPLY|OPEN")],
    # Blowdown and the water's chemistry.
    [A("bd_iso", "SG BLOWDOWN|ISOLATED"), A("bd_rad", "BLOWDOWN|RADIATION|HIGH"), A("bd_flow", "BLOWDOWN|FLOW|HI-LO"), A("bd_hx", "BLOWDOWN HX|TEMP|HI"),
     A("bd_tank", "BD FLASH|TANK|LEVEL HI"),
     A("sg_cation", "SG|CATION COND|HI"), A("sg_na", "SG|SODIUM|HI"), A("cond_o2", "CONDENSATE|OXYGEN|HI"), A("hydrazine", "HYDRAZINE|FEED|TROUBLE"),
     A("amine", "AMINE|FEED|TROUBLE"),
     A("sample_iso", "SG SAMPLE|ISOLATED"), A("cpd_dp", "COND POLISH|DIFF PRESS|HI"), A("cpd_byp", "COND POLISH|BYPASSED"), S("cst_makeup", "CST|MAKEUP|ON"),
     A("dw_lo", "DEMIN WATER|STORAGE|LO")],
]


def secondary_board(seed=37):
    """Steam generators and feedwater (round 28, live): for each steam generator its narrow and wide range levels,
    steam and feed flows and a recorder of its level and feed; its mimic (steam out through the atmospheric relief
    and the main steam isolation valve, feed in through the regulating valve and the auxiliary feed valve, each lit
    open or shut); its controllers and readouts; on the right auxiliary feedwater (the condensate storage tank, the
    two motor and the turbine-driven pumps into the header to every generator) and the main feed pumps; on the desk
    each generator's MSIV (it works: closing it cuts the turbine off from that generator, its steam goes out the
    relief valves), relief, feed and auxiliary feed valve switches, the auxiliary feed pumps (they work) and the
    annunciator's buttons."""
    b = LiveBoard(6, seed, "STEAM GENERATORS AND FEEDWATER")
    b.profile(False)
    b.hood_title()
    y_ann = b.ann_boxes(SECONDARY_WINDOWS)
    for k in range(4):
        n = k + 1
        x = 0.1 + k * 1.1
        b.plate(x + 0.55, y_ann - 0.04, f"STEAM GENERATOR {n}", 0.9, 0.04, "black")
        for j, (value, lo, hi, unit, majors, tag, band) in enumerate((
                (f"sg{n}.nr", 0, 100, "%", 6, "NR|LEVEL", (40, 60, "green")), (f"sg{n}.wr", 0, 100, "%", 6, "WR|LEVEL", None),
                (f"sg{n}.steam", 0, 5, "MLB/HR", 6, "STEAM|FLOW", None), (f"sg{n}.feed", 0, 5, "MLB/HR", 6, "FEED|FLOW", None))):
            xm = x + 0.1 + j * 0.12
            b.edge_meter(xm, 2.32, value, lo, hi, unit, majors, band)
            b.plate(xm, 2.125, tag, 0.11, 0.045)
        b.recorder_live(x + 0.81, 2.3, 0.32, [(f"sg{n}.nr", 0, 100, "red", "NR LVL"), (f"sg{n}.feed", 0, 5, "blue", "FEED")], f"SG {n} LEVEL / FEED")
        # The mimic.
        b.vessel(x + 0.15, 1.92, 0.16, 0.3, "lblue")
        b.plate(x + 0.15, 1.92, f"SG|{n}", 0.09, 0.07, d=0.032)
        b.mimic([(x + 0.23, 2.04), (x + 1.06, 2.04)], "yellow")
        b.mimic([(x + 0.38, 2.04), (x + 0.38, 1.975)], "yellow")
        b.valve_live(x + 0.38, 1.975, f"sg{n}.arv")
        b.plate(x + 0.455, 1.975, "ARV", 0.07, 0.026)
        b.valve_live(x + 0.68, 2.04, f"msiv{n}.open")
        b.plate(x + 0.68, 1.99, "MSIV", 0.08, 0.026)
        b.plate(x + 0.97, 2.0, "TURB", 0.08, 0.026)
        b.mimic([(x + 1.06, 1.84), (x + 0.23, 1.84)], "green")
        b.valve_live(x + 0.85, 1.84, f"sg{n}.frv")
        b.plate(x + 0.85, 1.785, "FRV", 0.07, 0.026)
        b.plate(x + 0.99, 1.885, "MFW", 0.07, 0.026)
        b.mimic([(x + 0.5, 1.70), (x + 0.5, 1.84)], "green")
        b.valve_live(x + 0.5, 1.765, f"sg{n}.afwv")
        b.plate(x + 0.575, 1.765, "AFW", 0.07, 0.026)
        # Controllers and readouts.
        b.controller_live(x + 0.15, ROW3, f"sg{n}.frv_out", f"sg{n}.frv_auto", f"FRV {n}")
        b.controller_live(x + 0.32, ROW3, f"sg{n}.arv_out", "auto", f"ARV {n}")
        b.digital_live(x + 0.76, 1.48, f"sg{n}.press", 4, 0, "STM|PSIG", 0.36, 0.07)
        b.digital_live(x + 0.76, 1.38, f"sg{n}.afw", 4, 0, "AFW|GPM", 0.36, 0.07)
        b.digital_live(x + 0.76, 1.28, f"sg{n}.nr", 3, 1, "NR|%", 0.36, 0.07)
        b.plate(x + 0.76, 1.56, f"SG {n} READOUTS", 0.36, 0.03, "black")
        # The desk: the MSIV (it works), the relief, feed and auxiliary feed valves.
        b.switch_live(x + 0.18, 0.6, f"MSIV {n}", "j", (("red", f"msiv{n}.open"), ("green", f"!msiv{n}.open")), f"msiv{n}", collar="yellow")
        b.switch_live(x + 0.43, 0.6, f"ARV {n}", "knob", (("red", f"sg{n}.arv"), ("green", f"!sg{n}.arv")), f"arv{n}")
        b.switch_live(x + 0.68, 0.6, f"FRV {n}", "j", (("red", f"sg{n}.frv"), ("green", f"!sg{n}.frv")))
        b.switch_live(x + 0.93, 0.6, f"AFW VLV {n}", "j", (("red", f"sg{n}.afwv"), ("green", f"!sg{n}.afwv")), f"fcv{n}", plate_w=0.14)
        b.desk_plate(x + 0.55, 0.36, f"STEAM GENERATOR {n}", 0.5, 0.034, "black")
        b.mimic_desk([(x + 0.08, 0.24), (x + 1.02, 0.24)], "yellow")
    # Auxiliary and main feedwater.
    b.plate(5.2, y_ann - 0.04, "AUXILIARY AND MAIN FEEDWATER", 1.3, 0.04, "black")
    for j, (value, lo, hi, unit, majors, tag) in enumerate((
            ("cst.level", 0, 100, "%", 6, "CST|LEVEL"), ("afw.press", 0, 1600, "PSIG", 5, "AFW HDR|PRESS"),
            ("afw_a.flow", 0, 600, "GPM", 4, "AFW A|FLOW"), ("afw_b.flow", 0, 600, "GPM", 4, "AFW B|FLOW"),
            ("afw_td.flow", 0, 1200, "GPM", 5, "TD AFW|FLOW"), ("afw_td.speed", 0, 5000, "RPM", 6, "TD AFW|SPEED"))):
        xm = 4.6 + j * 0.12
        b.edge_meter(xm, 2.32, value, lo, hi, unit, majors)
        b.plate(xm, 2.125, tag, 0.11, 0.045)
    b.dial_meter(5.62, 2.32, 0.28, "fw.hdr_press", 0, 1600, "PSIG", 5)
    b.plate(5.62, 2.155, "FW HDR PRESS", 0.26, 0.03)
    b.vessel(4.62, 1.88, 0.16, 0.3, "green")
    b.plate(4.62, 1.88, "CST", 0.1, 0.034, d=0.032)
    for yy, signal, tag in ((2.02, "afw_a.run", "AFW A"), (1.9, "afw_td.run", "TD AFW"), (1.78, "afw_b.run", "AFW B")):
        b.mimic([(4.7, yy), (5.12, yy)], "green")
        b.pump_live(4.86, yy, signal, 0.04)
        b.plate(5.02, yy + 0.032, tag, 0.11, 0.024)
    b.mimic([(5.12, 2.02), (5.12, 1.70)], "green")
    b.mimic([(5.12, 1.70), (0.6, 1.70)], "green")
    b.plate(4.3, 1.665, "AFW HEADER", 0.24, 0.026)
    for yy, signal, tag in ((2.0, "mfp_a.run", "MFP A"), (1.84, "mfp_b.run", "MFP B")):
        b.mimic([(5.85, yy), (5.3, yy)], "green")
        b.pump_live(5.55, yy, signal, 0.045)
        b.plate(5.72, yy + 0.034, tag, 0.1, 0.024)
    b.mimic([(5.3, 2.0), (5.3, 1.79)], "green")
    b.plate(5.42, 1.765, "TO FRVs", 0.14, 0.026)
    b.controller_live(4.64, ROW3, "afw_td.spd_out", "auto", "TD AFW SPD")
    b.controller_live(4.81, ROW3, "mfp_a.spd_out", "auto", "MFP A SPD")
    b.controller_live(4.98, ROW3, "mfp_b.spd_out", "auto", "MFP B SPD")
    b.digital_live(5.55, 1.48, "afw.total", 4, 0, "AFW|GPM", 0.4, 0.075)
    b.digital_live(5.55, 1.38, "fw.total", 3, 1, "MFW|MLB/H", 0.4, 0.075)
    b.digital_live(5.55, 1.28, "stm.hdr", 4, 0, "HDR|PSIG", 0.4, 0.075)
    b.plate(5.55, 1.56, "FEED AND STEAM", 0.4, 0.03, "black")
    # The desk: the auxiliary feed pumps (they work), the main feed pumps, the steam dumps, the annunciator.
    for x, tag, signal, cid in ((4.65, "AFW PP A", "afw_a.run", "afw_a"), (4.9, "TD AFW PP", "afw_td.run", "afw_td"), (5.15, "AFW PP B", "afw_b.run", "afw_b")):
        b.switch_live(x, 0.62, tag, "j", (("red", signal), ("green", "!" + signal)), cid, collar="green", plate_w=0.14)
    b.desk_plate(4.9, 0.82, "AUXILIARY FEEDWATER", 0.6, 0.034, "green")
    # (Turbine hall part 2: they start and stop the hall's main feed pumps, where it has them.)
    b.switch_live(4.65, 0.3, "MFP A", "j", (("red", "mfp_a.run"), ("green", "!mfp_a.run")), "mfp_a")
    b.switch_live(4.9, 0.3, "MFP B", "j", (("red", "mfp_b.run"), ("green", "!mfp_b.run")), "mfp_b")
    b.switch_live(5.15, 0.3, "STM DUMP", "knob", (("red", "dump_armed2"), ("green", "!dump_armed2")), plate_w=0.14)
    b.ann_buttons(5.6, 0.5)
    return b


# ---------------------------------------------------------------- round 28: the radiation and ventilation board

# The radiation monitors: number, name, unit (MR/HR and R/HR area monitors, CPM process monitors), where they read
# (nuclear/BoardPlant.java RAD_SPOTS).
RAD_MONITORS = [(1, "CONTROL ROOM", "MR/HR"), (2, "CNTMT AREA", "MR/HR"), (3, "CNTMT HIGH RANGE", "R/HR"), (4, "SPENT FUEL POOL", "MR/HR"),
                (5, "FUEL HANDLING", "MR/HR"), (6, "AUX BUILDING", "MR/HR"), (7, "TURBINE BUILDING", "MR/HR"), (8, "CASK STORAGE", "MR/HR"),
                (9, "CR INTAKE A", "CPM"), (10, "CR INTAKE B", "CPM"), (11, "PLANT VENT", "CPM"), (12, "CNTMT ATMOSPHERE", "CPM")]
# Each scale's decades (the pointer reads the reading's log).
RAD_DECADES = {"MR/HR": (-2, 4), "R/HR": (0, 7), "CPM": (1, 6)}
SHORT = {1: "CR", 2: "CNTMT", 3: "CNT HR", 4: "SFP", 5: "FHB", 6: "AUX", 7: "TURB", 8: "CASK", 9: "INT A", 10: "INT B", 11: "VENT", 12: "ATMOS"}


# The monitors' names as their annunciator windows give them.
RAD_WINDOW_NAMES = {1: "CONTROL RM", 2: "CNTMT AREA", 3: "CNTMT HI RNG", 4: "SPENT FUEL", 5: "FUEL HDLG", 6: "AUX BLDG", 7: "TURB BLDG",
                    8: "CASK PAD", 9: "CR INTAKE A", 10: "CR INTAKE B", 11: "PLANT VENT", 12: "CNTMT ATMOS"}


def _rad_box(first):
    """An annunciator box for five monitors (first..first + 4): their HIGH (red) and ALERT windows, and their FAIL
    status lights (white and steady, no horn: Justin, "FAIL light, no horn")."""
    names = RAD_WINDOW_NAMES
    out = [R(f"rm{n}_high", f"RM-{n}|{names[n]}|HIGH") for n in range(first, first + 5)]
    out += [A(f"rm{n}_alert", f"RM-{n}|{names[n]}|ALERT") for n in range(first, first + 5)]
    out += [S(f"rm{n}_fail", f"RM-{n}|{names[n]}|FAIL") for n in range(first, first + 5)]
    return out


RADIATION_WINDOWS = [
    _rad_box(1),
    _rad_box(6),
    # Monitors 11 and 12 and the effluents.
    [R("rm11_high", "RM-11|PLANT VENT|HIGH"), R("rm12_high", "RM-12|CNTMT ATMOS|HIGH"), A("plume", "FUEL DAMAGE|RELEASE|DETECTED"),
     A("head_open", "RX VESSEL|HEAD OPEN|REFUELING"), A("rms_comp", "RMS|COMPUTER|TROUBLE"),
     A("rm11_alert", "RM-11|PLANT VENT|ALERT"), A("rm12_alert", "RM-12|CNTMT ATMOS|ALERT"), A("cr_hirad", "CR INTAKE|HIGH RAD|EMERG VENT"),
     A("vent_flow", "PLANT VENT|FLOW|LO"), A("rms_test", "RM CHECK|SOURCE|IN TEST"),
     S("rm11_fail", "RM-11|PLANT VENT|FAIL"), S("rm12_fail", "RM-12|CNTMT ATMOS|FAIL"), A("eff_limit", "EFFLUENT|RELEASE|LIMIT"),
     A("liq_rad", "LIQ WASTE|DISCH RAD|HIGH"), A("bd_rad2", "SG BLOWDN|RADIATION|HIGH")],
    # The control room's ventilation (its HVAC panel's alarms, nuclear/HvacRoom.java).
    [A("hv_rx_trip", "CR HVAC|REACTOR|TRIP SIGNAL"), A("hv_rx_temp", "CR HVAC|RX HIGH|TEMP"), A("hv_rad", "CR INTAKE|HIGH|RADIATION"),
     A("hv_tox", "CR INTAKE|TOXIC|GAS"), A("hv_emerg", "CR HVAC|EMERGENCY|MODE"),
     A("hv_lowflow", "CREFS|LOW|FLOW"), A("hv_chiller", "CR|CHILLER|TRIP"), A("hv_dp", "CR|DIFF PRESS|LO"), A("ahu_a_trip", "AHU-1A|TRIP"),
     A("ahu_b_trip", "AHU-1B|TRIP"),
     A("efu_a_dp", "CRF-1A|FILTER|DP HI"), A("efu_b_dp", "CRF-1B|FILTER|DP HI"), A("cr_temp_hi", "CONTROL RM|TEMP|HI"),
     S("hv_missing", "CR HVAC|PANEL|NOT FOUND"), S("hv_auto", "CR HVAC|SELECTOR|AUTO")],
    # The containment's and the buildings' ventilation.
    [A("purge_iso2", "CNTMT|MINI-PURGE|ISOLATED"), A("purge_rad", "PURGE EXH|RADIATION|HIGH"), A("cfc_slow2", "CNTMT FAN|COOLERS|SLOW"),
     A("cnt_cool", "CNTMT|AVG TEMP|HI"), A("cnt_dp", "CNTMT|PRESS|HI-LO"),
     A("aux_exh_a", "AUX EXH|FAN A|TRIP"), A("aux_exh_b", "AUX EXH|FAN B|TRIP"), A("aux_filt", "AUX EXH|FILTER|DP HI"),
     A("aux_dp", "AUX BLDG|DIFF PRESS|LO"), A("aux_rad", "AUX BLDG|AREA RAD|HIGH"),
     A("fb_exh", "FUEL BLDG|EXH FAN|TRIP"), A("fb_emerg", "FUEL BLDG|EMERG EXH|START"), A("fb_dp", "FUEL BLDG|DIFF PRESS|LO"),
     A("tb_vent", "TURB BLDG|ROOF VENT|TROUBLE"), S("vent_norm", "PLANT VENT|FLOW|NORMAL")],
    # Fire detection by zone.
    [A("fire_cr", "FIRE|CONTROL|ROOM"), A("fire_csr", "FIRE|CABLE|SPREADING"), A("fire_relay", "FIRE|RELAY|ROOM"), A("fire_swgr_a", "FIRE|SWGR|ROOM A"),
     A("fire_swgr_b", "FIRE|SWGR|ROOM B"),
     A("fire_edg_a", "FIRE|EDG|ROOM A"), A("fire_edg_b", "FIRE|EDG|ROOM B"), A("fire_aux", "FIRE|AUX|BUILDING"), A("fire_tb", "FIRE|TURB BLDG|LUBE OIL"),
     A("fire_rcp", "FIRE|CNTMT|RCP AREA"),
     A("fire_fb", "FIRE|FUEL|BUILDING"), A("fire_intake", "FIRE|INTAKE|STRUCTURE"), A("fire_trbl", "FIRE|DETECTION|TROUBLE"),
     A("fire_pump", "FIRE PUMP|RUNNING"), A("fire_halon", "CO2 / HALON|DISCHARGE")],
]


def radiation_board(seed=67):
    """Radiation monitoring and ventilation (round 28, live): the twelve radiation monitors' drawers (each reads the
    game's radiation where it stands: the control room, the containment, the spent fuel pool, the fuel handling
    bridge, the auxiliary and turbine buildings, the casks; the control room's intakes, the plant vent and the
    containment's air read counts a minute and the release from damaged fuel), each with its figures, its HIGH, ALERT,
    FAIL and OPERATE lamps, its range and its buttons; their log meters, a recorder and the plant computer's page;
    the control room's ventilation as a mimic of its two trains (from its HVAC panel: every damper, filter unit, air
    handler, chiller and pump lit as it stands), the containment's mini-purge and the buildings' exhausts to the plant
    vent; on the desk the monitors' select buttons (lit in alarm), the ventilation's switches and the annunciator's."""
    b = LiveBoard(6, seed, "RADIATION MONITORING AND VENTILATION")
    b.profile(False)
    b.hood_title()
    y_ann = b.ann_boxes(RADIATION_WINDOWS)
    b.plate(1.53, y_ann - 0.04, "RADIATION MONITORING SYSTEM", 1.4, 0.04, "black")
    for idx, (n, name, unit) in enumerate(RAD_MONITORS):
        i, j = idx % 4, idx // 4
        b.rad_drawer(0.48 + i * 0.7, 2.385 - j * 0.31, n, name, unit, f"rm{n}", True)
    # The monitors' log meters, a recorder, the plant computer's page.
    logged = (2, 3, 4, 5, 6, 8, 9, 10, 11, 12)
    units = {n: u for n, _, u in RAD_MONITORS}
    for k, n in enumerate(logged):
        lo, hi = RAD_DECADES[units[n]]
        xm = 0.2 + k * 0.12
        b.edge_meter(xm, ROW3, f"rm{n}.log", lo, hi, units[n], 0, log=True)
        b.plate(xm, TAG_Y, f"RM-{n}", 0.08, 0.03)
    b.plate(0.74, ROW3 + 0.185, "AREA AND PROCESS MONITORS", 1.1, 0.035, "black")
    b.recorder_live(1.64, ROW3, 0.32, [("rm11.log", 1, 6, "red", "VENT"), ("rm2.log", -2, 4, "blue", "CNTMT")], "VENT / CNTMT LOG")
    b.screen_live(2.45, ROW3 + 0.02, 0.62, 0.28, "rad")
    b.plate(2.45, ROW3 - 0.165, "PLANT COMPUTER - RMS", 0.4, 0.035, "black")
    # The control room's ventilation: each train's outside air through its normal damper to its air handler, its
    # emergency intake through the filtration unit; its chiller and chilled water pump; into the control room.
    b.plate(3.95, y_ann - 0.04, "CONTROL ROOM VENTILATION", 1.3, 0.04, "black")
    for t, (ym, ye) in enumerate(((2.4, 2.27), (2.02, 1.89))):
        tn = "AB"[t]
        b.mimic([(3.18, ym), (4.54, ym)], "lblue")
        b.plate(3.27, ym + 0.032, "OUTSIDE AIR", 0.18, 0.024)
        b.valve_live(3.45, ym, f"hv.d1_{t}")
        b.plate(3.45, ym - 0.042, f"CR-D-1{tn}", 0.1, 0.024)
        b.mimic([(3.18, ye), (3.92, ye), (3.92, ym)], "lblue")
        b.plate(3.24, ye - 0.035, "EMERG", 0.1, 0.024)
        b.valve_live(3.35, ye, f"hv.d2_{t}")
        b.plate(3.35, ye + 0.042, f"CR-D-2{tn}", 0.1, 0.024)
        b.vessel(3.56, ye, 0.18, 0.07, "dgrey")
        b.plate(3.56, ye, f"CRF-1{tn}", 0.12, 0.028, d=0.032)
        b.pump_live(3.71, ye, f"hv.efu_{t}", 0.03)
        b.valve_live(3.83, ye, f"hv.d3_{t}")
        b.plate(3.83, ye + 0.042, f"CR-D-3{tn}", 0.1, 0.024)
        b.pump_live(4.07, ym, f"hv.ahu_{t}", 0.045)
        b.plate(4.07, ym + 0.065, f"AHU-1{tn}", 0.1, 0.024)
        b.mimic([(4.07, ye), (4.46, ye)], "blue")
        b.mimic([(4.07, ye), (4.07, ym - 0.05)], "blue")
        b.pump_live(4.2, ye, f"hv.ch_{t}", 0.035)
        b.plate(4.2, ye - 0.05, f"CH-1{tn}", 0.09, 0.024)
        b.pump_live(4.36, ye, f"hv.pump_{t}", 0.03)
        b.plate(4.36, ye - 0.05, f"CHP-1{tn}", 0.1, 0.024)
    b.vessel(4.62, 2.15, 0.16, 0.58, "lblue")
    b.plate(4.62, 2.15, "CONTROL|ROOM", 0.12, 0.07, d=0.032)
    b.status_windows(3.15, 1.62, 2, [("hv.normal", "NORMAL|MODE"), ("hv.emergency", "EMERGENCY|MODE"), ("hv.auto", "SELECTOR|AUTO"),
                                     ("hv.found", "HVAC PANEL|IN SERVICE")], 0.2, 0.072)
    for k, (value, lo, hi, unit, majors, tag) in enumerate((("cr.dp", 0, 0.5, "IN WG", 6, "CR|DIFF P"), ("cr.temp", 50, 100, "°F", 6, "CR|TEMP"),
                                                            ("efu_a.flow", 0, 1500, "CFM", 4, "CRF-1A|FLOW"), ("efu_b.flow", 0, 1500, "CFM", 4, "CRF-1B|FLOW"))):
        xm = 3.75 + k * 0.12
        b.edge_meter(xm, ROW3, value, lo, hi, unit, majors)
        b.plate(xm, TAG_Y - 0.008, tag, 0.11, 0.045)
    b.plate(3.93, ROW3 + 0.185, "CONTROL ROOM AIR", 0.46, 0.035, "black")
    b.plate(3.36, 1.665, "CR HVAC MODE", 0.4, 0.03, "black")
    # The containment's mini-purge and the auxiliary and fuel buildings' exhausts, to the plant vent.
    b.plate(5.3, y_ann - 0.04, "CONTAINMENT AND EXHAUSTS", 1.1, 0.04, "black")
    b.mimic([(4.86, 2.4), (5.3, 2.4)], "lblue")
    b.pump_live(4.95, 2.4, "purge.fan", 0.035)
    b.plate(4.95, 2.45, "SUP FAN", 0.1, 0.024)
    b.valve_live(5.1, 2.4, "purge.open")
    b.valve_live(5.2, 2.4, "purge.open")
    b.plate(5.15, 2.355, "ISOL", 0.08, 0.024)
    b.vessel(5.38, 2.33, 0.16, 0.22, "red")
    b.plate(5.38, 2.33, "CNTMT", 0.12, 0.028, d=0.032)
    b.mimic([(5.46, 2.26), (5.82, 2.26)], "lblue")
    b.valve_live(5.53, 2.26, "purge.open")
    b.valve_live(5.62, 2.26, "purge.open")
    b.plate(5.575, 2.215, "ISOL", 0.08, 0.024)
    b.pump_live(5.73, 2.26, "purge.fan", 0.03)
    for yy, (signal, tag) in ((2.08, ("aux.fan_a", "AUX A")), (1.98, ("aux.fan_b", "AUX B"))):
        b.mimic([(4.86, yy), (5.22, yy)], "lblue")
        b.pump_live(5.0, yy, signal, 0.035)
        b.plate(4.9, yy + 0.034, tag, 0.09, 0.022)
    b.mimic([(5.22, 2.08), (5.22, 1.98)], "lblue")
    b.mimic([(5.22, 2.03), (5.28, 2.03)], "lblue")
    b.vessel(5.38, 2.03, 0.2, 0.08, "dgrey")
    b.plate(5.38, 2.03, "AUX FILTER", 0.16, 0.026, d=0.032)
    b.mimic([(5.48, 2.03), (5.82, 2.03)], "lblue")
    b.mimic([(4.86, 1.84), (5.28, 1.84)], "lblue")
    b.mimic([(5.48, 1.84), (5.82, 1.84)], "lblue")
    b.pump_live(5.0, 1.84, "fb.fan", 0.035)
    b.plate(4.95, 1.884, "FUEL BLDG", 0.13, 0.022)
    b.vessel(5.38, 1.84, 0.2, 0.08, "dgrey")
    b.plate(5.38, 1.84, "FB FILTER", 0.16, 0.026, d=0.032)
    b.mimic([(5.82, 1.84), (5.82, 2.36)], "lblue")
    b.vessel(5.82, 2.43, 0.1, 0.12, "dgrey")
    b.plate(5.7, 2.47, "VENT", 0.09, 0.024)
    for k, (value, lo, hi, unit, majors, tag) in enumerate((("vent.flow", 0, 200, "KCFM", 5, "VENT|FLOW"), ("purge.flow", 0, 2000, "CFM", 5, "PURGE|FLOW"),
                                                            ("aux.flow", 0, 100, "KCFM", 6, "AUX EXH|FLOW"), ("fb.flow", 0, 50, "KCFM", 6, "FB EXH|FLOW"))):
        xm = 4.95 + k * 0.12
        b.edge_meter(xm, ROW3, value, lo, hi, unit, majors)
        b.plate(xm, TAG_Y - 0.008, tag, 0.11, 0.045)
    b.plate(5.13, ROW3 + 0.185, "EXHAUST FLOWS", 0.46, 0.035, "black")
    b.digital_live(5.66, 1.47, "rm11", 4, -1, "VENT|CPM", 0.36, 0.08)
    b.digital_live(5.66, 1.37, "cnt.press", 4, 2, "CNTMT|PSIG", 0.36, 0.08)
    # The desk: the monitors' select buttons (lit in alarm), the ventilation's switches, the annunciator.
    for idx, (n, name, unit) in enumerate(RAD_MONITORS):
        i, j = idx % 6, idx // 6
        b.desk_button(0.3 + i * 0.18, 0.72 - j * 0.2, f"RM-{n}|{SHORT[n]}", "amber", f"rm{n}.alarm", 0.085)
    b.desk_plate(0.75, 0.88, "RADIATION MONITOR SELECT", 0.8, 0.034, "black")
    b.desk_button(1.55, 0.72, "CHECK|SOURCE|ALL", "white", "rms.check", 0.085)
    b.desk_button(1.55, 0.52, "RMS|ALARM|RESET", "white", "rms.check", 0.085)
    b.ann_buttons(2.45, 0.5)
    for k, (tag, signal, kind) in enumerate((("CR HVAC A", "hv.ahu_0", "j"), ("CR HVAC B", "hv.ahu_1", "j"), ("CREFS A", "hv.efu_0", "j"),
                                             ("CREFS B", "hv.efu_1", "j"), ("CR CHILLER A", "hv.ch_0", "knob"), ("CR CHILLER B", "hv.ch_1", "knob"))):
        b.switch_live(3.3 + k * 0.25, 0.6, tag, kind, (("red", signal), ("green", "!" + signal)), plate_w=0.15)
    b.desk_plate(3.93, 0.8, "CONTROL ROOM HVAC", 0.6, 0.034, "black")
    for k, (tag, signal, kind) in enumerate((("PURGE FAN", "purge.fan", "j"), ("PURGE ISOL", "purge.open", "j"), ("AUX EXH A", "aux.fan_a", "j"),
                                             ("AUX EXH B", "aux.fan_b", "j"), ("FB EXH FAN", "fb.fan", "j"))):
        b.switch_live(4.95 + k * 0.22, 0.46 if k % 2 else 0.68, tag, kind, (("red", signal), ("green", "!" + signal)), plate_w=0.14)
    b.desk_plate(5.39, 0.88, "BUILDING EXHAUST", 0.5, 0.034, "black")
    b.mimic_desk([(3.15, 0.24), (5.85, 0.24)], "lblue")
    return b


# ---------------------------------------------------------------- round 29: the remote shutdown panel live

def page_tex(lines, w, h, density=DESK_DENSITY):
    """A procedure's page: its title in bold and its steps, black on white paper."""
    pw, ph = px(w, density), px(h, density)

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(246, 244, 236, 255))
        n = len(lines)
        lh = (ph - 8) / n
        # One size for the steps (the largest every step fits at), the title's own.
        body = min(_fit(d, [line], pw - 10, lh, int(lh * 0.8), False)[1] for line in lines[1:]) if n > 1 else 0
        for i, line in enumerate(lines):
            bold = i == 0
            f = _fit(d, [line], pw - 10, lh, int(lh * 0.8), bold)[0] if bold else _font(body, False)
            d.text((6, 4 + lh * i + lh / 2), line, font=f, fill=(16, 16, 18, 255), anchor="lm")
            if i == 0:
                d.line([(6, 4 + lh - 1), (pw - 6, 4 + lh - 1)], fill=(16, 16, 18, 255), width=1)
    return ATLAS.add(f"page:{pw}x{ph}:{lines}", pw, ph, draw)


class FaceBoard(LiveBoard):
    """A live vertical panel (the remote shutdown panel): its working controls stand on the face, found where the
    player's look meets it (nuclear/ControlBoard.java face; a control's s0..s1 is its height y0..y1)."""

    def _face_handle(self, x, y, kind, angle, lst):
        """A switch's handle on the face turned angle degrees (clockwise) from upright."""
        a = math.radians(angle)
        u = (math.sin(a), math.cos(a), 0.0)
        n = (0.0, 0.0, -1.0)
        if kind == "knob":
            self.cyl((x, y, self.face - 0.03), (x, y, self.face - 0.052), 0.03, sw("black"), sw("dgrey"), 10, lst)
            self.obox((x + u[0] * 0.013, y + u[1] * 0.013, self.face - 0.0525), u, n, 0.013, 0.0006, 0.0035, sw("white"), lst)
            return
        self.obox((x, y, self.face - 0.042), u, n, 0.04, 0.012, 0.013, sw("black"), lst)
        self.obox((x + u[0] * 0.006, y + u[1] * 0.006, self.face - 0.0545), u, n, 0.03, 0.0006, 0.003, sw("white"), lst)

    def work_switch(self, x, y, text, kind="j", lamps=(("red", None), ("green", None)), control=None, plate_w=0.16):
        """A control switch on the face: its escutcheon and boss, its handle (turned to where it was last left, for a
        working one: "1" right, running or open; "0" left), its lamps above (colour, signal) and its nameplate below."""
        self.face_box(x, y, 0.1, 0.1, 0.012, sw("mgrey"), sw("dgrey"))
        self.cyl((x, y, self.face - 0.012), (x, y, self.face - 0.03), 0.022, sw("black"), sw("dgrey"), 8)
        if control:
            ops = {}
            for state, angle in (("0", -45), ("1", 45), ("c", 0)):
                lst = []
                self._face_handle(x, y, kind, angle, lst)
                ops[state] = lst
            self.live["handles"].append({"id": control, "ops": ops})
            self.control(control, x - 0.06, x + 0.06, y - 0.06, y + 0.06)
        else:
            self._face_handle(x, y, kind, 0, None)
        for k, (colour, signal) in enumerate(lamps):
            lx = x + (k - (len(lamps) - 1) / 2) * 0.042
            self.face_lamp_live(lx, y + 0.085, colour, signal or "rms.check", 0.026)
        if text:
            self.plate(x, y - 0.078, text, plate_w, 0.03)

    def transfer_key(self, x, y, text):
        """A control transfer switch: its plate, the key-lock barrel with the key upright (control at the control
        room, "0") or turned across (at this panel, "1"), the white and amber lamps above, its nameplate below. Any
        key turns the panel's control over (the game keeps one transfer for the unit)."""
        self.face_box(x, y, 0.1, 0.1, 0.012, sw("mgrey"), sw("dgrey"))
        self.cyl((x, y, self.face - 0.012), (x, y, self.face - 0.04), 0.022, sw("dgrey"), sw("black"), 8)
        ops = {}
        for state, local in (("0", False), ("1", True)):
            lst = []
            hx, hy = (0.022, 0.006) if local else (0.006, 0.022)
            self.box(x - hx, y - hy, self.face - 0.07, x + hx, y + hy, self.face - 0.04, [sw("yellow")] * 5 + [None], lst)
            # The key's bow.
            bx, by = (x + 0.03, y) if local else (x, y + 0.03)
            self.box(bx - 0.012, by - 0.012, self.face - 0.066, bx + 0.012, by + 0.012, self.face - 0.044, [sw("yellow")] * 5 + [None], lst)
            ops[state] = lst
        self.live["handles"].append({"id": "transfer", "ops": ops})
        self.control("transfer", x - 0.06, x + 0.06, y - 0.06, y + 0.06)
        for k, (colour, signal) in enumerate((("white", "rsp.mcr"), ("amber", "rsp.local"))):
            self.face_lamp_live(x + (k - 0.5) * 0.042, y + 0.085, colour, signal, 0.026)
        self.plate(x, y - 0.078, text, 0.16, 0.03)

    def guarded_face(self, x, y, cid, gid, text):
        """A reactor trip switch under its hinged red cover (it works): the cover down over it ("0") or flipped up
        ("1": the first click lifts it, the second trips; it drops again after five seconds), the black handle level
        ("1", normal) or turned to TRIP ("0"), its red plate and nameplate."""
        self.face_box(x, y, 0.16, 0.16, 0.014, sw("red"), sw("red"))
        for dx in (-0.075, 0.075):
            self.face_box(x + dx, y, 0.012, 0.14, 0.08, sw("red"), sw("red"))
        ops = {}
        for state, angle in (("1", 90), ("0", 45)):
            lst = []
            a = math.radians(angle)
            u = (math.sin(a), math.cos(a), 0.0)
            self.obox((x, y, self.face - 0.04), u, (0.0, 0.0, -1.0), 0.05, 0.026, 0.016, sw("black"), lst)
            self.obox((x, y, self.face - 0.0665), u, (0.0, 0.0, -1.0), 0.042, 0.0006, 0.004, sw("white"), lst)
            ops[state] = lst
        self.live["handles"].append({"id": cid, "ops": ops})
        guard = {"0": [], "1": []}
        self.box(x - 0.074, y - 0.074, self.face - 0.086, x + 0.074, y + 0.074, self.face - 0.08, [sw("red")] * 6, guard["0"])
        self.box(x - 0.074, y + 0.076, self.face - 0.235, x + 0.074, y + 0.082, self.face - 0.086, [sw("red")] * 6, guard["1"])
        self.live["handles"].append({"id": gid, "ops": guard})
        self.control(cid, x - 0.085, x + 0.085, y - 0.085, y + 0.085)
        self.plate(x, y - 0.115, text, 0.2, 0.034, "red")

    def face_push(self, x, y, legend, colour, signal, cid, size=0.07):
        """An illuminated push button on the face: its black bezel and body, its legend lens lit by state."""
        h = size / 2
        black = sw("black")
        self.face_box(x, y, size + 0.016, size + 0.016, 0.01, black, black)
        self.box(x - h, y - h, self.face - 0.024, x + h, y + h, self.face - 0.01, [None, None, black, black, black, black])
        z = self.face - 0.0245
        self.legend_lamp(signal, ((x - h, y + h, z), (x - h, y - h, z), (x + h, y - h, z), (x + h, y + h, z)), legend, colour, size)
        self.control(cid, x - h - 0.01, x + h + 0.01, y - h - 0.01, y + h + 0.01)

    def procedure(self, x0, x1, pages):
        """The procedure lying open on the ledge: its blue cover and its two pages (pages: two lists of lines)."""
        y = 0.95
        z0, z1 = self.face - 0.25, self.face - 0.03
        blue = sw("blue")
        self.box(x0, y, z0, x1, y + 0.012, z1, [blue, blue, blue, blue, blue, None])
        mid = (x0 + x1) / 2
        for (a, c), lines in zip(((x0 + 0.012, mid - 0.004), (mid + 0.004, x1 - 0.012)), pages):
            white = sw("white")
            self.box(a, y + 0.012, z0 + 0.012, c, y + 0.026, z1 - 0.012, [white, white, white, white, None, None])
            yt = y + 0.026
            self.quad((a, yt, z1 - 0.012), (a, yt, z0 + 0.012), (c, yt, z0 + 0.012), (c, yt, z1 - 0.012),
                      page_tex(lines, c - a, z1 - z0 - 0.024))
        self.box(mid - 0.003, y + 0.012, z0 + 0.03, mid + 0.003, y + 0.034, z1 - 0.03, sw("mgrey"))


RSP_WINDOWS = [
    R("rx_tripped", "REACTOR|TRIPPED"), A("rsp_local", "CONTROL|AT REMOTE|PANEL"), R("sg1_lolo", "SG 1|LEVEL|LO-LO"),
    R("sg2_lolo", "SG 2|LEVEL|LO-LO"), R("sg3_lolo", "SG 3|LEVEL|LO-LO"),
    R("sg4_lolo", "SG 4|LEVEL|LO-LO"), A("pzr_p_lo", "PZR PRESS|LO"), A("pzr_p_hi", "PZR PRESS|HI"), A("pzr_l_lo", "PZR LEVEL|LO"),
    A("pzr_l_hi", "PZR LEVEL|HI"),
    A("afw_flow_lo", "AFW|FLOW|LO"), A("cst_lo", "CST|LEVEL|LO"), A("cet_hi", "CORE EXIT|TEMP|HI"), A("subcool_lo", "RCS|SUBCOOLING|LO"),
    A("mssv", "MAIN STEAM|SAFETY VLV|LIFTED"),
]


def rsp_board(seed=89):
    """The remote shutdown panel (round 29, live; Justin: "Switches on panel + screen", aux feed and the steam
    generators' ARVs working, the boards locked out while it has control, a small annunciator). Three sections bolted
    together: Train A (orange), the reactor in the middle, Train B (blue). Each train: its indication (pressurizer
    pressure and level, a loop's hot and cold leg temperatures, its steam generators' levels, the boric acid tank, aux
    feed flow or the condensate storage tank, source range flux and startup rate, its generators' steam pressure on a
    recorder); its control transfer keys (any one turns control over to the panel: white lamp the control room has
    it, amber the panel); its aux feed mimic and switches (the pumps, the flow control valve to each generator, the
    generators' ARVs: they work, with control here); the reactor coolant system's switches (their lamps live). The
    middle: the panel's title, its annunciator, the unit's status screen, the control and trip lamps, the two reactor
    trip switches under their covers (they work), the annunciator's buttons, the telephones; on the ledge the
    evacuation procedure lies open; under it the doors and their placards."""
    b = FaceBoard(6, seed, "REMOTE SHUTDOWN PANEL", face=0.3)
    b.cabinet(((0.04, 2.5, 2), (2.5, 3.5, 1), (3.5, 5.96, 2)))
    trains = (
        (0.1, "orange", "A", 1, (1, 2), ("BAT|LVL %", "cvcs.bast_level", 3, 1), ("AFW|GPM", "afw.total", 4, 0),
         ("AFW PP A", "TDAFW PP", "CHG PP A", "PZR HT A", "PORV A", "SG 1 ARV"),
         ((0.55, 1.70, "afw_a", "AFW PP A"), (0.80, 1.58, "afw_td", "TDAFW PP")),
         (("CHG PP A", "cvcs.chg_a", "j"), ("BA PP A", "ba_a.run", "j"), ("PZR HT A", "pzr.htr_prop", "j"), ("PORV A", "pzr.porv", "knob"),
          ("BLOCK A", "plant.present", "knob"), ("RHR PP A", "rhr_a.run", "j"), ("CCW PP A", "ccw_a.run", "j"), ("SW PP A", "sw_a.run", "j"))),
        (3.56, "blue", "B", 3, (3, 4), ("BAT|LVL %", "cvcs.bast_level", 3, 1), ("CST|LVL %", "cst.level", 3, 1),
         ("AFW PP B", "CHG PP B", "PZR HT B", "PORV B", "SG 3 ARV", "SG 4 ARV"),
         ((0.55, 1.70, "afw_b", "AFW PP B"),),
         (("CHG PP B", "cvcs.chg_b", "j"), ("BA PP B", "ba_b.run", "j"), ("PZR HT B", "pzr.htr_backup", "j"), ("PORV B", "pzr.porv", "knob"),
          ("BLOCK B", "plant.present", "knob"), ("RHR PP B", "rhr_b.run", "j"), ("CCW PP B", "ccw_b.run", "j"), ("SW PP B", "sw_b.run", "j"))),
    )
    for x0, colour, name, loop, sgs, dig3, dig4, transfers, pumps, coolant in trains:
        x1 = x0 + 2.34
        mid = (x0 + x1) / 2
        b.plate(mid, 2.87, f"TRAIN {name}", 0.6, 0.07, colour)
        # Indication.
        b.zone([(x0, 2.26), (x1, 2.26), (x1, 2.8), (x0, 2.8), (x0, 2.26)])
        b.plate(mid, 2.8, "INDICATION", 0.4, 0.04, "black", d=0.016)
        for i, (value, lo, hi, unit, majors, tag, band) in enumerate((
                ("pzr.press", 1700, 2500, "PSIG", 5, "PZR|PRESS", (2185, 2310, "green")), ("pzr.level", 0, 100, "%", 6, "PZR|LEVEL", None),
                (f"loop{loop}.thot", 500, 650, "°F", 4, f"LOOP {loop}|TH", None), (f"loop{loop}.tcold", 500, 650, "°F", 4, f"LOOP {loop}|TC", None))):
            xm = x0 + 0.14 + i * 0.12
            b.edge_meter(xm, 2.53, value, lo, hi, unit, majors, band)
            b.plate(xm, 2.335, tag, 0.11, 0.045)
        for i, (tag, value, n, dp) in enumerate(((f"SG {sgs[0]}|LVL %", f"sg{sgs[0]}.nr", 3, 1), (f"SG {sgs[1]}|LVL %", f"sg{sgs[1]}.nr", 3, 1),
                                                 dig3, dig4)):
            b.digital_live(x0 + 0.82 + (i % 2) * 0.26, 2.62 - (i // 2) * 0.16, value, n, dp, tag, 0.24, 0.07)
        b.dial_meter(x0 + 1.43, 2.55, 0.22, "sr.log", 0, 6, "CPS", 7, ["1", "10", "10²", "10³", "10⁴", "10⁵", "10⁶"])
        b.plate(x0 + 1.43, 2.405, "SR FLUX", 0.2, 0.03)
        b.dial_meter(x0 + 1.69, 2.55, 0.22, "rsp.sur", -1, 5, "DPM", 7)
        b.plate(x0 + 1.69, 2.405, "STARTUP RATE", 0.22, 0.03)
        b.recorder_live(x0 + 2.07, 2.49, 0.3, [(f"sg{sgs[0]}.press", 0, 1200, "red", f"SG {sgs[0]}"), (f"sg{sgs[1]}.press", 0, 1200, "blue", f"SG {sgs[1]}")],
                        "SG PRESS")
        # The control transfer keys.
        b.zone([(x0, 1.88), (x1, 1.88), (x1, 2.2), (x0, 2.2), (x0, 1.88)], "yellow")
        b.plate(mid, 2.2, "CONTROL TRANSFER", 0.56, 0.04, "yellow", d=0.016)
        for i, legend in enumerate(transfers):
            b.transfer_key(x0 + 0.24 + i * 0.37, 2.02, legend)
        # Aux feed: the tank, the pumps, the header, the flow control valves to the generators and their ARVs.
        b.zone([(x0, 0.99), (x1, 0.99), (x1, 1.82), (x0, 1.82), (x0, 0.99)])
        b.zone([(x0, 1.27), (x1, 1.27)])
        b.plate(mid, 1.82, "AUXILIARY FEEDWATER", 0.6, 0.04, "black", d=0.016)
        b.vessel(x0 + 0.2, 1.64, 0.16, 0.22, "green")
        b.plate(x0 + 0.2, 1.64, "CST", 0.09, 0.03, d=0.032)
        hx = x0 + 0.98
        rows = {yy for (_, yy, _, _) in pumps}
        for (px_, yy, cid, tag) in pumps:
            b.pump_live(x0 + px_, yy, cid + ".run", 0.04)
        b.mimic([(hx, 1.70), (hx, 1.58)], "green")
        for k, (yy, sg) in enumerate(zip((1.70, 1.58), sgs)):
            # A pump's line runs on from the tank through the header to its generator; a row without a pump starts at
            # the header.
            b.mimic([(x0 + 0.28 if yy in rows else hx, yy), (x0 + 2.05, yy)], "green")
            b.valve_live(x0 + 1.35, yy, f"sg{sg}.afwv")
            b.plate(x0 + 1.35, yy + 0.04, f"FCV {sg}", 0.08, 0.024)
            b.vessel(x0 + 2.12, yy, 0.14, 0.1, "lblue")
            b.plate(x0 + 2.12, yy, f"SG {sg}", 0.09, 0.03, d=0.032)
            b.mimic([(x0 + 2.19, yy), (x0 + 2.31, yy)], "yellow")
            b.valve_live(x0 + 2.27, yy, f"sg{sg}.arv")
            b.plate(x0 + 2.27, yy + 0.04, "ARV", 0.06, 0.024)
        for (px_, yy, cid, tag) in pumps:
            b.work_switch(x0 + 0.45 + (0.3 if cid == "afw_td" else 0), 1.4, tag, "j", (("red", cid + ".run"), ("green", "!" + cid + ".run")), cid)
        for k, sg in enumerate(sgs):
            b.work_switch(x0 + 1.18 + k * 0.29, 1.4, f"FCV {sg}", "knob", (("red", f"sg{sg}.afwv"), ("green", f"!sg{sg}.afwv")), f"fcv{sg}")
            b.work_switch(x0 + 1.79 + k * 0.29, 1.4, f"SG {sg} ARV", "knob", (("red", f"sg{sg}.arv"), ("green", f"!sg{sg}.arv")), f"arv{sg}")
        # The reactor coolant system's switches (their lamps live).
        b.plate(mid, 1.27, "REACTOR COOLANT", 0.56, 0.04, "black", d=0.016)
        for i, (legend, signal, kind) in enumerate(coolant):
            b.work_switch(x0 + 0.16 + i * 0.29, 1.1, legend, kind, (("red", signal), ("green", "!" + signal)))
        # The caution placard on the section's first door.
        b.face_box(x0 + 0.42, 0.7, 0.26, 0.12, 0.004, plate_tex("CAUTION|TRANSFER CONTROL|PER AOP-8 ONLY", 0.26, 0.12, "yellow"), sw("label"))
    # The middle: the title, the annunciator, the unit's status screen, the lamps, the trip switches, the telephones.
    b.plate(3.0, 2.875, "REMOTE SHUTDOWN|PANEL - UNIT 1", 0.84, 0.1, "red")
    b.ann_boxes([RSP_WINDOWS], x_first=2.54, y_top=2.8)
    b.screen_live(3.0, 2.18, 0.7, 0.36, "rsp")
    for x, colour, signal, legend in ((2.75, "white", "rsp.mcr", "CONTROL|ROOM"), (3.0, "amber", "rsp.local", "THIS|PANEL"),
                                      (3.25, "red", "rx.tripped", "REACTOR|TRIPPED")):
        b.face_lamp_live(x, 1.89, colour, signal, 0.045)
        b.plate(x, 1.82, legend, 0.17, 0.05)
    b.guarded_face(2.76, 1.6, "rsp_trip_a", "rsp_trip_a_guard", "REACTOR TRIP A")
    b.guarded_face(3.24, 1.6, "rsp_trip_b", "rsp_trip_b_guard", "REACTOR TRIP B")
    for k, (legend, colour, signal, cid) in enumerate((("ACK", "amber", "ann.unacked", "ack"), ("RESET", "white", "ann.cleared", "reset"),
                                                       ("TEST", "white", "ann.test", "test"))):
        b.face_push(2.85 + k * 0.15, 1.32, legend, colour, signal, cid)
    b.plate(3.0, 1.395, "ANNUNCIATOR", 0.3, 0.03, "black")
    b.phone(3.0, 1.15)
    b.jack_box(2.68, 1.08)
    b.jack_box(3.32, 1.08)
    b.procedure(2.62, 3.38, (["AOP-8  CONTROL ROOM EVACUATION", "1. TRIP THE REACTOR AND THE TURBINE", "2. GO TO THE REMOTE SHUTDOWN PANEL",
                              "3. TURN THE TRANSFER KEYS TO LOCAL", "4. START AFW: PUMP A AND TD PUMP", "5. OPEN THE FCVs TO EVERY SG"],
                             ["6. HOLD SG LEVELS 40 TO 60 %", "7. HOLD STEAM PRESSURE ON THE ARVs", "8. WATCH PZR LEVEL AND PRESSURE",
                              "9. COOL DOWN ON THE ARVs WHEN TOLD", "10. HOLD HOT STANDBY", "11. TRANSFER BACK FROM THE CR ONLY"]))
    b.face_box(4.42, 0.7, 0.14, 0.1, 0.004, plate_tex("DANGER|125 VDC", 0.14, 0.1, "red"), sw("label"))
    b.face_box(3.0, 0.7, 0.12, 0.06, 0.004, plate_tex("RSP-1", 0.12, 0.06, "black"), sw("label"))
    return b


# ---------------------------------------------------------------- round 30: the reactor control console

# The rod banks (the game's names: shutdown banks A to D, control banks A to D), each rod's place in the core (row and
# column offsets from the middle, rows down) and the banks' names on the rod position display. 53 rod cluster control
# assemblies in a 193-assembly core, laid out with the core's eightfold symmetry.
ROD_BANKS = ["sa", "sb", "sc", "sd", "ca", "cb", "cc", "cd"]
BANK_NAMES = {"sa": "SA", "sb": "SB", "sc": "SC", "sd": "SD", "ca": "A", "cb": "B", "cc": "C", "cd": "D"}


def _octant(*offsets):
    """Every place an offset (row, column) lands under the core's symmetry (its eight reflections and turns)."""
    out = []
    for r, c in offsets:
        for a, b in ((r, c), (c, r)):
            for sr in (1, -1):
                for sc in (1, -1):
                    p = (a * sr, b * sc)
                    if p not in out:
                        out.append(p)
    return out


ROD_PATTERN = {"cd": _octant((0, 0), (4, 4)), "cc": _octant((0, 4), (2, 2)), "cb": _octant((2, 6)), "ca": _octant((0, 6), (5, 5)),
               "sa": _octant((2, 4)), "sb": _octant((4, 6)), "sc": _octant((0, 2)), "sd": _octant((0, 7))}
CORE_ROWS = [7, 11, 13, 13, 15, 15, 15, 15, 15, 15, 15, 13, 13, 11, 7]
CORE_COLUMNS = "RPNMLKJHGFEDCBA"         # the core's columns as the maps letter them (no I, O or Q)
assert sum(len(v) for v in ROD_PATTERN.values()) == 53
for _b, _places in ROD_PATTERN.items():
    for _r, _c in _places:
        assert abs(_c) <= (CORE_ROWS[_r + 7] - 1) // 2, (_b, _r, _c)

# The rod position display's panel on the face (x0, y0, x1, y1) and its grid inside it (fractions of the panel).
DRPI = (0.98, 1.2, 2.02, 2.5)
DRPI_GRID = (0.06, 0.05, 1.0, 0.985)     # left, top, right, bottom (fractions of the panel's width and height, from the top)
DRPI_DENSITY = 768


def drpi_tex():
    """The rod position display's panel: black, the core map (every assembly a dark square, every rod's place lighter
    with its bank's name), the columns lettered across the top and the rows numbered down the side."""
    x0, y0, x1, y1 = DRPI
    pw, ph = px(x1 - x0, DRPI_DENSITY), px(y1 - y0, DRPI_DENSITY)
    gl, gt, gr, gb = DRPI_GRID

    def draw(img):
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, pw - 1, ph - 1], fill=(16, 17, 19, 255))
        cw, chh = (gr - gl) * pw / 15, (gb - gt) * ph / 15
        rods = {p: b for b, places in ROD_PATTERN.items() for p in places}
        f = _font(int(chh * 0.26))
        small = _font(int(chh * 0.2))
        ink = (226, 228, 222, 255)
        for c in range(15):
            d.text((gl * pw + (c + 0.5) * cw, gt * ph / 2), CORE_COLUMNS[c], font=f, fill=ink, anchor="mm")
        for r in range(15):
            d.text((gl * pw / 2, gt * ph + (r + 0.5) * chh), str(r + 1), font=f, fill=ink, anchor="mm")
            n = CORE_ROWS[r]
            for c in range(15):
                dc = c - 7
                if abs(dc) > (n - 1) // 2:
                    continue
                ax, ay = gl * pw + c * cw, gt * ph + r * chh
                bank = rods.get((r - 7, dc))
                fill = (58, 62, 66, 255) if bank else (34, 36, 39, 255)
                d.rectangle([ax + 1.5, ay + 1.5, ax + cw - 2.5, ay + chh - 2.5], fill=fill)
                if bank:
                    d.text((ax + cw * 0.2, ay + chh * 0.32), BANK_NAMES[bank], font=small, fill=(150, 220, 160, 255), anchor="mm")
        # The core's outline.
        pts = []
        for r in range(15):
            half = (CORE_ROWS[r] - 1) // 2
            pts.append((r, 7 - half, 8 + half))
        for r, a, b in pts:
            for c in (a, b):
                x = gl * pw + c * cw
                d.line([(x, gt * ph + r * chh), (x, gt * ph + (r + 1) * chh)], fill=(150, 152, 150, 255), width=1)
        for r in range(16):
            above = pts[r - 1] if r > 0 else None
            below = pts[r] if r < 15 else None
            spans = [s for s in (above, below) if s]
            lo = min(s[1] for s in spans)
            hi = max(s[2] for s in spans)
            if above and below:
                # Only where the rows differ.
                for a, b in ((min(above[1], below[1]), max(above[1], below[1])), (min(above[2], below[2]), max(above[2], below[2]))):
                    if a != b:
                        y = gt * ph + r * chh
                        d.line([(gl * pw + a * cw, y), (gl * pw + b * cw, y)], fill=(150, 152, 150, 255), width=1)
            else:
                y = gt * ph + r * chh
                d.line([(gl * pw + lo * cw, y), (gl * pw + hi * cw, y)], fill=(150, 152, 150, 255), width=1)
    return ATLAS.add(f"drpi:{pw}x{ph}", pw, ph, draw)


def drpi_cell(r, c):
    """A core place's cell on the face (x0, y0, x1, y1): r, c its row and column offsets."""
    x0, y0, x1, y1 = DRPI
    gl, gt, gr, gb = DRPI_GRID
    cw, ch = (gr - gl) * (x1 - x0) / 15, (gb - gt) * (y1 - y0) / 15
    ax = x0 + gl * (x1 - x0) + (c + 7) * cw
    ay1 = y1 - gt * (y1 - y0) - (r + 7) * ch
    return ax, ay1 - ch, ax + cw, ay1


RX_WINDOWS = [
    # The reactor and its rod control.
    [R("rx_tripped", "REACTOR|TRIPPED"), A("rtb_open", "RX TRIP|BREAKERS|OPEN"), A("rod_bottom", "ROD|AT BOTTOM"),
     A("sd_banks", "SHUTDOWN|BANKS NOT|FULLY OUT"), A("rps_bypass", "REACTOR|PROTECTION|BYPASSED"),
     A("auto_dev", "AUTO ROD|CONTROL|DEVIATION"), A("rsp_local", "CONTROL AT|REMOTE|PANEL"), A("tavg_dev", "TAVG-TREF|DEVIATION"),
     A("otdt_alert", "OT DELTA-T|ROD STOP|RUNBACK"), A("tavg_hi", "TAVG|HI"),
     A("cet_hi", "CORE EXIT|TEMP|HI"), A("si_act", "SAFETY|INJECTION|ACTUATED"), A("turb_tripped", "TURBINE|TRIPPED"),
     A("loop_out", "RCS LOOP|NOT|AVAILABLE"), A("mt_missing", "TURBINE|NOT|CONNECTED")],
    # The nuclear instruments and the core.
    [A("sr_hi", "SR|HI FLUX|LEVEL"), A("ir_hi", "IR|HI FLUX|LEVEL"), A("pr_lo_hi", "PR LO SETPT|HI FLUX"),
     A("pr_pos", "PR POSITIVE|RATE|ALERT"), A("pr_neg", "PR NEGATIVE|RATE|ALERT"),
     A("sur_hi", "STARTUP|RATE|HI"), A("nis_dev", "NIS POWER|RANGE|DEVIATION"), A("qptr", "QUADRANT|POWER TILT|HI"),
     A("cw_flow_lo", "CIRC WATER|FLOW|LO"), A("fuel_fail", "FUEL|FAILURE"),
     A("fuel_burnup", "FUEL|BURNUP|LIMIT"), A("core_empty", "CORE|NOT|LOADED"), A("refuel_open", "REFUELING|CAVITY|OPEN"),
     A("unit_incomplete", "REACTOR|UNIT|INCOMPLETE"), A("cnt_rad", "CNTMT|RADIATION|HI")],
    # The bypass and permissive lights (white, lit while true).
    [S("p4", "P-4|RX TRIP|BKRS OPEN"), S("p6", "P-6|IR ABOVE|1E-10 AMP"), S("p7", "P-7|AT POWER|TRIPS ARMED"), S("p8", "P-8|LO FLOW TRIP|ARMED"),
     S("p9", "P-9|TURB TRIP|ARMED"),
     S("p10", "P-10|PR ABOVE|10%"), S("p13", "P-13|TURB IMP|ABOVE 10%"), S("sr_blk", "SR TRIP|BLOCKED"), S("ir_blk", "IR TRIP|BLOCKED"),
     S("pr_blk", "PR LO TRIP|BLOCKED"),
     S("sr_hv_off", "SOURCE|RANGE HV|OFF"), S("rod_auto", "RODS|IN AUTO"), S("rod_man", "RODS|IN MANUAL"),
     S("sd_out", "SHUTDOWN|BANKS|FULLY OUT"), S("rps_normal", "REACTOR|PROTECTION|NORMAL")],
]


class ConsoleBoard(LiveBoard):
    """The reactor control console (a benchboard section three wide): its own parts besides a board's."""

    def rod_lever(self, x, s):
        """The rods' IN-HOLD-OUT lever (it works: held to the left the rods drive in, to the right out; it springs back
        to HOLD): its black base plate, the lever by state ("0" in, "c" hold, "1" out), the white IN and OUT lamps (lit
        while the rods move) and the engraved plate under it."""
        import numpy as np
        self.desk_block(x, s, 0.2, 0.12, 0.012, [sw("black")] * 5 + [None])
        self.desk_block(x, s, 0.05, 0.05, 0.022, [sw("dgrey")] * 5 + [None], h0=0.012)
        p, t, n = self.on_desk(x, s, 0.022)
        ops = {}
        for state, angle in (("0", -28), ("c", 0), ("1", 28)):
            lst = []
            a = math.radians(angle)
            d = n * math.cos(a) + np.array([1.0, 0, 0]) * math.sin(a)
            d /= np.linalg.norm(d)
            u = np.array([1.0, 0, 0]) * math.cos(a) - n * math.sin(a)
            self.obox(tuple(p + d * 0.036), tuple(u), tuple(d), 0.007, 0.04, 0.007, sw("mgrey"), lst)
            self.cyl(tuple(p + d * 0.075), tuple(p + d * 0.105), 0.02, sw("black"), sw("black"), 10, lst)
            ops[state] = lst
        self.live["handles"].append({"id": "rod_lever", "ops": ops})
        self.control("rods_in", x - 0.1, x, s - 0.07, s + 0.07)
        self.control("rods_out", x, x + 0.1, s - 0.07, s + 0.07)
        for k, (colour, signal) in enumerate((("white", "rods.moving_in"), ("white", "rods.moving_out"))):
            self.desk_lamp_live(x + (k - 0.5) * 0.14, s + 0.095, colour, signal)
        self.desk_plate(x - 0.07, s + 0.15, "IN", 0.05, 0.026)
        self.desk_plate(x + 0.07, s + 0.15, "OUT", 0.05, 0.026)
        self.desk_plate(x, s - 0.095, "IN - HOLD - OUT", 0.2, 0.032)

    def desk_procedure(self, x0, x1, s0, s1, pages):
        """A procedure lying open on the desk: its blue cover, its two pages (two lists of lines, the first each page's
        title) and the rings down the middle."""
        blue, white = sw("blue"), sw("white")
        x, s, w, t = (x0 + x1) / 2, (s0 + s1) / 2, x1 - x0, (s1 - s0) * 1.116
        self.desk_block(x, s, w, t, 0.01, [blue] * 5 + [None])
        mid = x
        for (a, c), lines in zip(((x0 + 0.012, mid - 0.006), (mid + 0.006, x1 - 0.012)), pages):
            self.desk_block((a + c) / 2, s, c - a, t - 0.024, 0.022, [white] * 4 + [page_tex(lines, c - a, t - 0.024)] + [None], h0=0.01)
        for k in range(3):
            self.desk_block(mid, s0 + (s1 - s0) * (0.25 + k * 0.25), 0.01, 0.02, 0.03, [sw("mgrey")] * 5 + [None], h0=0.022)

    def rod_position_display(self):
        """The digital rod position indicator: its panel (the core map), each rod's column of green lights lit to its
        bank's position (the game's, in steps), its red rod bottom light under it."""
        x0, y0, x1, y1 = DRPI
        z = self.face - 0.014
        self.face_box((x0 + x1) / 2, (y0 + y1) / 2, x1 - x0 + 0.03, y1 - y0 + 0.03, 0.012, sw("dgrey"), sw("dgrey"))
        self.front_quad(x0, y0, x1, y1, z, drpi_tex(), None)
        for bank, places in ROD_PATTERN.items():
            for r, c in places:
                cx0, cy0, cx1, cy1 = drpi_cell(r, c)
                mx = (cx0 + cx1) / 2 + 0.008
                self.live["bars"].append({"v": f"bank.{bank}", "lo": 0, "hi": 228, "n": 12, "x0": round(mx - 0.007, 4), "x1": round(mx + 0.007, 4),
                                          "y0": round(cy0 + 0.022, 4), "y1": round(cy1 - 0.008, 4), "z": round(z - 0.002, 4)})
                k = 0.006
                by = cy0 + 0.012
                self.lamp(f"bank.{bank}.bottom", ((mx - k, by + k, z - 0.002), (mx - k, by - k, z - 0.002), (mx + k, by - k, z - 0.002),
                                                  (mx + k, by + k, z - 0.002)), "red")


def console_board(seed=7):
    """The reactor control console (round 30, live; Justin: rods driven by holding the lever, the control banks in
    their overlap with the shutdown banks out on the reset, AUTO holding power, the NIS blocks working without new
    trips). Across the top the annunciator: the reactor and rod control, the nuclear instruments and the core, the
    bypass and permissive lights. On the left the nuclear instruments: the source and intermediate ranges' log meters
    and startup rates, the four power range channels and their average, the NIS recorder. In the middle the digital
    rod position indicator, a core map with every rod's position lit. On the right the banks' step counters, the rod
    speed and AUTO's setpoint, the plant computer's reactor page. On the desk rod control (MAN/AUTO, the IN-HOLD-OUT
    lever), the NIS trip blocks, the reactor protection switch, the two reactor trip switches under their guards and
    the annunciator's buttons."""
    b = ConsoleBoard(3, seed, "REACTOR CONTROL")
    b.profile(False)
    b.hood_title()
    y_ann = b.ann_boxes(RX_WINDOWS)
    yp = y_ann - 0.04
    b.plate(0.52, yp, "NUCLEAR INSTRUMENTATION", 0.8, 0.04, "black")
    b.plate(1.5, yp, "DIGITAL ROD POSITION INDICATION", 1.0, 0.04, "black")
    b.plate(2.48, yp, "ROD CONTROL", 0.8, 0.04, "black")
    # The nuclear instruments: source and intermediate range, the power range, the recorder.
    for k, (value, lo, hi, unit, tag, log) in enumerate((
            ("sr1.log", 0, 6, "CPS", "N31", True), ("sr2.log", 0, 6, "CPS", "N32", True), ("sr.sur", -1, 5, "DPM", "SUR", False),
            ("ir1.log", -11, -3, "AMPS", "N35", True), ("ir2.log", -11, -3, "AMPS", "N36", True), ("ir.sur", -1, 5, "DPM", "SUR", False))):
        x = 0.19 + k * 0.12 + (0.05 if k >= 3 else 0)
        b.edge_meter(x, 2.30, value, lo, hi, unit, 7, log=log)
        b.plate(x, 2.115, tag, 0.07, 0.03)
    b.plate(0.31, 2.485, "SOURCE RANGE", 0.34, 0.03)
    b.plate(0.77, 2.485, "INTERMEDIATE RANGE", 0.36, 0.03)
    for k in range(4):
        x = 0.19 + k * 0.12
        b.edge_meter(x, 1.80, f"pr{k + 1}", 0, 120, "%", 7, (100, 120, "red"))
        b.plate(x, 1.615, f"N4{k + 1}", 0.07, 0.03)
    b.plate(0.37, 1.985, "POWER RANGE", 0.46, 0.03)
    b.dial_meter(0.78, 1.80, 0.25, "pr.avg", 0, 125, "%", 6)
    b.plate(0.78, 1.955, "REACTOR POWER", 0.25, 0.03)
    b.recorder_live(0.52, 1.36, 0.7, [("ir1.log", -11, -3, "red", "IR N35"), ("pr.avg", 0, 120, "blue", "PR AVG")], "NIS RECORDER")
    # The rod position indicator.
    b.rod_position_display()
    # Rod control: the banks' step counters, the rod speed and AUTO's setpoint, the plant computer.
    b.plate(2.48, 2.485, "BANK POSITION - STEPS", 0.6, 0.03)
    for k, bank in enumerate(ROD_BANKS):
        col, row = k // 4, k % 4
        b.digital_live(2.28 + col * 0.4, 2.415 - row * 0.09, f"bank.{bank}", 3, 0, BANK_NAMES[bank] if col == 0 else "C" + BANK_NAMES[bank],
                       wide=0.24, tw=0.06)
    b.digital_live(2.28, 2.02, "rod.speed", 3, 0, "SPM", wide=0.24, tw=0.06)
    b.digital_live(2.68, 2.02, "rod.sp", 4, 1, "SP %", wide=0.24, tw=0.06)
    b.plate(2.28, 1.95, "ROD SPEED", 0.24, 0.03)
    b.plate(2.68, 1.95, "AUTO POWER SETPT", 0.24, 0.03)
    b.screen_live(2.48, 1.55, 0.76, 0.42, "rx")
    b.plate(2.48, 1.795, "PLANT COMPUTER - REACTOR", 0.56, 0.035, "black")
    # The desk: rod control, the NIS blocks, the protection switch, the reactor trip switches, the annunciator.
    b.switch_live(0.3, 0.62, "MAN - AUTO", "knob", (("amber", "!rods.auto"), ("white", "rods.auto")), "rod_mode", plate_w=0.16)
    b.rod_lever(0.66, 0.6)
    b.desk_plate(0.48, 0.9, "ROD CONTROL", 0.5, 0.034, "black")
    for k, (legend, signal, cid) in enumerate((("SR|BLOCK", "sr.blocked", "sr_block"), ("IR|BLOCK", "ir.blocked", "ir_block"),
                                               ("PR LO|BLOCK", "prlo.blocked", "pr_block"))):
        b.desk_button(1.12 + k * 0.15, 0.62, legend, "amber", signal, 0.08, cid)
    b.desk_plate(1.27, 0.78, "NIS TRIP BLOCK", 0.42, 0.032, "black")
    b.switch_live(1.72, 0.62, "RX PROTECTION", "knob", (("green", "rps.normal"), ("amber", "!rps.normal")), "rps", collar="yellow",
                  plate_w=0.18)
    for x, name in ((2.2, "A"), (2.62, "B")):
        t = name.lower()
        b.guarded_live(x, 0.56, f"rx_trip_{t}", f"rx_trip_{t}_guard", (("red", "!rx.tripped"), ("green", "rx.tripped")), f"REACTOR TRIP {name}")
    b.desk_plate(2.41, 0.9, "TRIP - CLOSE", 0.4, 0.032, "red")
    b.ann_buttons(1.27, 0.22)
    # The startup procedure lying open on the desk (the game's way of starting up from this console, step by step).
    b.desk_procedure(1.92, 2.9, 0.05, 0.33, (
        ["GOP-3  REACTOR STARTUP", "1. ROD CONTROL TO MAN", "2. TURN A TRIP SWITCH TO CLOSE", "3. SD BANKS DRIVE OUT TO 228",
         "4. HOLD THE LEVER TO OUT", "5. AT P-6 BLOCK THE SOURCE RANGE"],
        ["GOP-3  (CONTINUED)", "6. AT P-10 BLOCK IR AND PR LO", "7. RAISE POWER, WATCH THE SUR", "8. AT THE POWER WANTED: AUTO",
         "9. TO SHUT DOWN: MAN, HOLD IN", "10. IN DOUBT: TRIP THE REACTOR"]))
    return b


LIVE_SECTIONS = {"rcs_control_board": rcs_board, "turbine_control_board": turbine_board, "electrical_control_board": electrical_board,
                 "esf_control_board": esf_board, "secondary_control_board": secondary_board, "radiation_control_board": radiation_board,
                 "remote_shutdown_panel": rsp_board, "reactor_control_console": console_board}
C.SECTIONS.update(LIVE_SECTIONS)


def legend_image():
    """The legend texture (textures/entity/control_board_legends.png): every live board built, its drawings packed."""
    for make in LIVE_SECTIONS.values():
        make()
    return ATLAS.pack()
