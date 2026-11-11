"""The Tri-Pillar's crew (Justin approved the take-2 mockup sheet on 5 November 2026: Minecraft-style characters, the
nine uniforms of four departments, all white with blue or green eyes, the janitors women and everyone else men; then
four more jobs, drone pilots, engineers, a medic and the manager; then the night shift and breaks sheet, eyelids
shut and a blanket for the sleepers): their skins, the gear boxes the skins dress, the props they carry, and the hiring
pool.

Each crew skin is 128 x 64: the left half is the player skin layout (head, body, arms, legs and their outer layers; the
janitors' arms three pixels wide, everyone else's four), the right half that person's gear (headset, cap peak, hard
hat, bun, ponytail, and the eyelids they show asleep), painted in their own colours. The props (tablet, clipboard,
radio, toolbox, mug, tray, the facilities cart and the sleepers' blanket) share one 128 x 128 texture. client/TowerCrewModel and client/TowerCrewPropsModel build their boxes from the
JSON written here, so the boxes and the paint can't drift apart.

Writes:
  assets/jugcraft/textures/entity/tower_crew/<skin>.png   the skins
  assets/jugcraft/textures/entity/tower_crew_props.png    the props' texture
  assets/jugcraft/crew/gear.json, props.json              the boxes (model pixels, the game's model space: y down,
                                                          the front at -z), their texture offsets and when they show
  data/jugcraft/drone_tower/tower_crew.json               the hiring pool: each job's skins (gear bits, arms) and the
                                                          names the tower hires under

No coplanar faces: tower/TowerCrew's model stands each outer layer off its base box and off the layers it can meet by
at least 0.1 px (client/TowerCrewModel), and the gear and props here keep 0.1 px or more off the faces they touch."""
from __future__ import annotations

import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src/main/resources"
ASSETS = RESOURCES / "assets/jugcraft"
SKIN_DIR = ASSETS / "textures/entity/tower_crew"
PROPS_PNG = ASSETS / "textures/entity/tower_crew_props.png"
PROPS_GLOW_PNG = ASSETS / "textures/entity/tower_crew_props_glow.png"
GEAR_JSON = ASSETS / "crew/gear.json"
PROPS_JSON = ASSETS / "crew/props.json"
PROPS_H = 256         # the props texture's height (128 x 128 when the blanket came in; 256 for security's armour, helmet, rifle)
POOL_JSON = RESOURCES / "data/jugcraft/drone_tower/tower_crew.json"

AMBER = (245, 166, 35)
SKINS = [(242, 212, 190), (236, 198, 172), (230, 190, 160), (240, 204, 176), (226, 184, 156), (234, 194, 168)]
HAIRS = [(38, 30, 24), (74, 50, 32), (122, 84, 48), (190, 150, 92), (52, 44, 40), (160, 158, 152), (150, 62, 30)]
EYES = [(58, 96, 150), (88, 140, 196), (70, 116, 72), (98, 150, 92)]          # blue or green only
WHITE = (244, 244, 240)
BLACK = (28, 28, 30)

SIDES = ("right", "front", "left", "back")
FACES = ("up", "down") + SIDES
WIDE = {"head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
        "rarm": (40, 16, 4, 12, 4), "rsleeve": (40, 32, 4, 12, 4), "larm": (32, 48, 4, 12, 4), "lsleeve": (48, 48, 4, 12, 4),
        "rleg": (0, 16, 4, 12, 4), "rpants": (0, 32, 4, 12, 4), "lleg": (16, 48, 4, 12, 4), "lpants": (0, 48, 4, 12, 4)}

# gear bits (tower/TowerCrew.GEAR_*)
HEADSET, CAP, HARDHAT, BUN, PONYTAIL = 1, 2, 4, 8, 16
EYELIDS = 32          # not worn: client/TowerCrewModel shows the eyelids while they sleep
EXO = 128             # security's exoskeleton (Justin, 6 November 2026: "make the security guy wear an exoskeleton suit as
                      # well"): on the props layer (props.json, its groups with "bit"), off to sleep
BATON = 256           # the gate guards' plasma baton (Justin, 6 November 2026: "give the gate guards plasma batons ... when
                      # not in use they holster it on their side"): the holster on the right thigh, with the baton in it
HELMET = 64           # security's full helmet (Justin, 6 November 2026: "make security look more like a soldier, with a
                      # full helmet on", then "bigger helmet to match" the power armour): on the props layer, in place of
                      # the head (client/TowerCrewModel hides the head while it is on); off to sleep
CRESTS = (512, 1024, 2048)  # a drop squad leader's rank on the helmet (sheet 4's ranks; tower/squad/Legionary): decanus,
                      # tesserarius, optio
# props (tower/TowerCrew.PROP_*)
PROPS = {"tablet": 1, "clipboard": 2, "radio": 3, "toolbox": 4, "mug": 5, "tray": 6, "cart": 7, "blanket": 8, "rifle": 9, "baton": 10}


def rects(u, v, w, h, d):
    """Where a box's faces sit in its texture (the game's box unwrap): x, y, width, height. "front" is the face at the
    model's -z (the way the figure looks), "right" the one at -x (the figure's right)."""
    return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def tone(rgb, k):
    return tuple(min(255.0, c * k) for c in rgb)


def mix(a, b, t):
    return tuple(x * (1 - t) + y * t for x, y in zip(a, b))


# ------------------------------------------------------------------ the skin painter

class Skin:
    """A crew skin: the player layout in the left 64 x 64. Face-local pixels: x across the face as seen from outside, y
    down from its top edge (a limb's side faces: row 0 at the shoulder or hip)."""

    def __init__(self, slim=False, seed=0, width=128, height=64):
        self.rgb = np.zeros((height, width, 3), np.float32)
        self.alpha = np.zeros((height, width), np.float32)
        self.grain = np.zeros((height, width), np.float32)
        self.glow = np.zeros((height, width), bool)                 # lit: drawn again full bright (the glow layer)
        self.glow_rgb = np.zeros((height, width, 3), np.float32)
        self.box = dict(WIDE)
        if slim:
            for k in ("rarm", "rsleeve", "larm", "lsleeve"):
                u, v, w, h, d = self.box[k]
                self.box[k] = (u, v, 3, h, d)
        self.rng = np.random.default_rng(seed)

    def face(self, part, f):
        return rects(*self.box[part])[f]

    def size(self, part, f):
        return self.face(part, f)[2:]

    def rect(self, part, f, x0, y0, x1, y1, rgb, grain=0.035, a=1.0):
        X, Yv, W, H = self.face(part, f)
        x0, y0, x1, y1 = max(0, x0), max(0, y0), min(W, x1), min(H, y1)
        if x1 <= x0 or y1 <= y0:
            return
        sl = (slice(Yv + y0, Yv + y1), slice(X + x0, X + x1))
        self.rgb[sl] = np.asarray(rgb, np.float32)
        self.alpha[sl] = a
        self.grain[sl] = grain
        self.glow[sl] = False

    def lit(self, part, f, x0, y0, x1, y1, rgb, glow=None):
        """A lit strip: painted rgb, and glowing (glow, or a brighter rgb) on the glow layer."""
        self.rect(part, f, x0, y0, x1, y1, rgb, 0.0)
        X, Yv, W, H = self.face(part, f)
        x0, y0, x1, y1 = max(0, x0), max(0, y0), min(W, x1), min(H, y1)
        if x1 <= x0 or y1 <= y0:
            return
        sl = (slice(Yv + y0, Yv + y1), slice(X + x0, X + x1))
        self.glow[sl] = True
        self.glow_rgb[sl] = np.asarray(glow or rgb, np.float32)

    def litband(self, part, y0, y1, rgb, glow=None, sides=SIDES):
        for f in sides:
            W, H = self.size(part, f)
            self.lit(part, f, 0, y0, W, y1, rgb, glow)

    def glow_image(self):
        out = np.zeros(self.alpha.shape + (4,), np.uint8)
        on = self.glow & ((self.alpha > 0) | getattr(self, "glow_force", np.zeros_like(self.glow)))
        out[..., :3] = np.where(on[..., None], np.clip(self.glow_rgb, 0, 255), 0).astype(np.uint8)
        out[..., 3] = np.where(on, 255, 0).astype(np.uint8)
        return Image.fromarray(out, "RGBA")

    def dot(self, part, f, x, y, rgb, grain=0.0):
        self.rect(part, f, x, y, x + 1, y + 1, rgb, grain)

    def dots(self, part, f, pts, rgb, grain=0.0):
        for x, y in pts:
            self.dot(part, f, x, y, rgb, grain)

    def fill(self, part, rgb, faces=FACES, grain=0.035):
        for f in faces:
            W, H = self.size(part, f)
            self.rect(part, f, 0, 0, W, H, rgb, grain)

    def band(self, part, y0, y1, rgb, grain=0.035, sides=SIDES):
        for f in sides:
            W, H = self.size(part, f)
            self.rect(part, f, 0, y0, W, y1, rgb, grain)

    def clear(self, part, f, x0, y0, x1, y1):
        X, Yv, W, H = self.face(part, f)
        self.alpha[Yv + max(0, y0):Yv + min(H, y1), X + max(0, x0):X + min(W, x1)] = 0

    def shade(self, part, f, x0, y0, x1, y1, k):
        X, Yv, W, H = self.face(part, f)
        self.rgb[Yv + max(0, y0):Yv + min(H, y1), X + max(0, x0):X + min(W, x1)] *= k

    def gradient(self, part, top=1.04, bottom=0.92, sides=SIDES):
        """Clothes a touch lighter at the top of a part than at its bottom, as skin artists shade them."""
        for f in sides:
            X, Yv, W, H = self.face(part, f)
            self.rgb[Yv:Yv + H, X:X + W] *= np.linspace(top, bottom, H)[:, None, None]

    def ptext(self, part, f, x0, y, n, rgb, seed=1):
        """Lettering too small to read: letter-shaped runs of pixels with gaps between the words."""
        r = np.random.default_rng(seed)
        x = 0
        while x < n:
            run = int(r.integers(1, 4))
            for k in range(run):
                if x + k < n and r.random() < 0.85:
                    self.dot(part, f, x0 + x + k, y, rgb)
            x += run + 1

    def paint_box(self, u, v, size, colours, grain=0.04):
        """Paints a gear box's unwrap at (u, v): colours face -> rgb ("*" for the rest)."""
        w, h, d = size
        for f, (x, y, ww, hh) in rects(u, v, w, h, d).items():
            rgb = colours.get(f, colours.get("*"))
            x0, y0 = int(math.floor(x)), int(math.floor(y))
            x1, y1 = int(math.ceil(x + ww)), int(math.ceil(y + hh))
            sl = (slice(y0, max(y0 + 1, y1)), slice(x0, max(x0 + 1, x1)))
            self.rgb[sl] = np.asarray(rgb, np.float32)
            self.alpha[sl] = 1.0
            self.grain[sl] = grain

    def image(self):
        n = self.rng.normal(0, 1, self.alpha.shape).astype(np.float32)
        rgb = np.clip(self.rgb * (1 + self.grain * n)[..., None], 0, 255)
        out = np.zeros(self.alpha.shape + (4,), np.uint8)
        out[..., :3] = rgb.astype(np.uint8)
        out[..., 3] = (self.alpha * 255).astype(np.uint8)
        return Image.fromarray(out, "RGBA")


# ------------------------------------------------------------------ heads

def _hair(sk, part, f, pts, hc, r):
    for x, y in pts:
        sk.dot(part, f, x, y, tone(hc, r.choice([0.82, 0.92, 1.0, 1.0, 1.1, 1.18])), 0.0)


def _rows(xs, ys):
    return [(x, y) for y in ys for x in xs]


def _side(fside, f_min, f_max, ys):
    """Pixels on a side face of the head by distance from its front edge (0: the front column)."""
    return [((7 - fd) if fside == "right" else fd, y) for y in ys for fd in range(f_min, f_max + 1)]


HAIR_FRONT = {"crop": _rows(range(8), [0]),
              "short": _rows(range(8), [0, 1]) + [(0, 2), (7, 2), (5, 2), (6, 2)],
              "long": _rows(range(8), [0, 1]) + _rows([0, 7], [2, 3]),
              "bob": _rows(range(8), [0, 1]) + [(0, 2), (1, 2), (2, 2), (5, 2), (6, 2), (7, 2)] + _rows([0, 7], [3, 4, 5, 6]),
              "bald": []}
HAIR_SIDE = {"crop": lambda s: _side(s, 0, 7, [0, 1]) + _side(s, 4, 7, [2, 3]),
             "short": lambda s: _side(s, 0, 7, [0, 1, 2]) + _side(s, 5, 7, [3, 4]) + _side(s, 1, 2, [3]),
             "long": lambda s: _side(s, 0, 7, [0, 1, 2, 3]) + _side(s, 5, 7, [4, 5, 6, 7]) + _side(s, 0, 0, [4, 5]),
             "bob": lambda s: _side(s, 0, 7, [0, 1, 2, 3]) + _side(s, 0, 2, [4, 5, 6]) + _side(s, 5, 7, [4, 5, 6]),
             "bald": lambda s: _side(s, 5, 7, [3, 4])}
HAIR_BACK = {"crop": 5, "short": 6, "long": 8, "bob": 8, "bald": 0}


def head(sk, st, hc, style, eye, beard, seed, volume=True):
    r = np.random.default_rng(seed)
    sk.fill("head", st, grain=0.02)
    F = "front"
    sk.shade("head", F, 0, 2, 1, 8, 0.95); sk.shade("head", F, 7, 2, 8, 8, 0.95); sk.shade("head", F, 0, 7, 8, 8, 0.96)
    sk.dot("head", F, 1, 4, WHITE); sk.dot("head", F, 2, 4, EYES[eye]); sk.dot("head", F, 5, 4, EYES[eye]); sk.dot("head", F, 6, 4, WHITE)
    sk.dots("head", F, [(1, 3), (2, 3), (5, 3), (6, 3)], tone(hc, 0.85))
    sk.dots("head", F, [(3, 5), (4, 5)], tone(st, 0.86))
    sk.dots("head", F, [(3, 6), (4, 6)], mix(tone(st, 0.80), (150, 70, 62), 0.30))
    sk.dots("head", "right", [(3, 4), (3, 5)], tone(st, 0.84))
    sk.dots("head", "left", [(4, 4), (4, 5)], tone(st, 0.84))
    sk.shade("head", "down", 0, 0, 8, 8, 0.9)
    if style != "bald":
        _hair(sk, "head", "up", _rows(range(8), range(8)), hc, r)
    _hair(sk, "head", F, HAIR_FRONT[style], hc, r)
    for s in ("right", "left"):
        _hair(sk, "head", s, HAIR_SIDE[style](s), hc, r)
    nb = HAIR_BACK[style]
    _hair(sk, "head", "back", _rows(range(8), range(nb)), hc, r)
    if 0 < nb < 8:
        _hair(sk, "head", "back", [(x, nb) for x in range(8) if r.random() < 0.5], hc, r)
    if beard:
        bp = [(0, 5), (1, 5), (2, 5), (5, 5), (6, 5), (7, 5), (0, 6), (1, 6), (2, 6), (5, 6), (6, 6), (7, 6)] + _rows(range(8), [7])
        _hair(sk, "head", F, bp, tone(hc, 0.95), r)
        for s in ("right", "left"):
            _hair(sk, "head", s, _side(s, 0, 1, [3, 4, 5, 6, 7]), tone(hc, 0.95), r)
    if volume and style != "bald":
        # the outer layer: a ragged second layer of hair standing off the head
        _hair(sk, "hat", "up", [(x, y) for y in range(8) for x in range(8) if r.random() < 0.85], hc, r)
        _hair(sk, "hat", F, [(x, 0) for x in range(8) if r.random() < 0.6], hc, r)
        deep = 7 if style == "bob" else (4 if style == "long" else 2)
        for s in ("right", "left"):
            pts = [(x, y) for (x, y) in HAIR_SIDE[style](s) if y < deep and r.random() < (0.75 if y < deep - 1 else 0.4)]
            _hair(sk, "hat", s, pts, hc, r)
        _hair(sk, "hat", "back", [(x, y) for y in range(min(nb, 7)) for x in range(8) if r.random() < (0.7 if y < nb - 2 else 0.35)], hc, r)
        if style == "bob":
            _hair(sk, "hat", F, _rows([0, 7], range(1, 7)), hc, r)


# ------------------------------------------------------------------ clothes

def shirt(sk, rgb, st, sleeves="short", collar=None, open_neck=True, cuff=None):
    sk.fill("body", rgb)
    sk.gradient("body", 1.05, 0.93)
    for arm in ("rarm", "larm"):
        sk.fill(arm, rgb)
        if sleeves == "short":
            sk.band(arm, 4, 12, st, 0.02)
            sk.band(arm, 3, 4, tone(rgb, 0.85))
        else:
            sk.band(arm, 9, 10, cuff or tone(rgb, 0.85))
            sk.band(arm, 10, 12, st, 0.02)
        sk.fill(arm, st, faces=("down",), grain=0.02)
        sk.gradient(arm, 1.04, 0.94)
    if open_neck:
        sk.dots("body", "front", [(3, 0), (4, 0)], st)
        sk.dot("body", "front", 3, 1, tone(st, 0.92)); sk.dot("body", "front", 4, 1, tone(st, 0.92))
    c = collar or tone(rgb, 1.15)
    sk.dots("body", "front", [(2, 0), (5, 0), (2, 1), (5, 1)], c)
    sk.dots("body", "up", [(2, 2), (3, 2), (4, 2), (5, 2)], c)


def trousers(sk, rgb, shoes=BLACK, boots=False, sole=(18, 18, 18)):
    for leg in ("rleg", "lleg"):
        sk.fill(leg, rgb)
        sk.gradient(leg, 1.0, 0.9)
        sk.band(leg, 9 if boots else 10, 12, shoes, 0.03)
        if boots:
            sk.band(leg, 9, 10, tone(shoes, 1.25), 0.02, sides=("front",))
        sk.fill(leg, sole, faces=("down",), grain=0.02)
    sk.dot("rleg", "front", 3, 0, tone(rgb, 0.85)); sk.dot("lleg", "front", 0, 0, tone(rgb, 0.85))


def belt(sk, rgb=(24, 22, 22), buckle=(186, 186, 180), below=None):
    sk.band("body", 10, 11, rgb, 0.02)
    sk.dots("body", "front", [(3, 10), (4, 10)], buckle)
    if below is not None:
        sk.band("body", 11, 12, below)


def badge(sk, part="body", x=5, y=2):
    """The tower's amber badge on the left chest."""
    sk.dot(part, "front", x, y, tone(AMBER, 1.12)); sk.dot(part, "front", x + 1, y, AMBER)
    sk.dot(part, "front", x, y + 1, AMBER); sk.dot(part, "front", x + 1, y + 1, tone(AMBER, 0.72))


def lanyard(sk, cord=AMBER):
    """The crew card on its cord (the card carries the badge)."""
    sk.dots("body", "front", [(2, 2), (3, 3), (5, 2), (4, 3)], tone(cord, 0.9))
    sk.dots("body", "front", [(3, 4), (4, 4), (3, 5), (4, 5)], WHITE)
    sk.dot("body", "front", 4, 4, AMBER); sk.dot("body", "front", 3, 5, (70, 70, 74))


def hands(sk, rgb):
    for arm in ("rarm", "larm"):
        sk.band(arm, 10, 12, rgb, 0.03)
        sk.fill(arm, rgb, faces=("down",), grain=0.03)


def cap_skin(sk, rgb, rows=2):
    sk.fill("hat", rgb, faces=("up",), grain=0.03)
    for s in SIDES:
        sk.rect("hat", s, 0, 0, 8, rows, rgb, 0.03)
    sk.rect("hat", "back", 2, rows - 1, 6, rows, tone(rgb, 0.7))


def glasses_skin(sk, frame=(36, 38, 42), lens=(70, 90, 104)):
    sk.rect("hat", "front", 0, 4, 8, 5, frame, 0.02)
    sk.dots("hat", "front", [(1, 4), (2, 4), (5, 4), (6, 4)], lens)
    sk.dot("hat", "front", 1, 4, tone(lens, 1.5))
    for s in ("right", "left"):
        X = (6, 7) if s == "right" else (0, 1)
        sk.dots("hat", s, [(X[0], 4), (X[1], 4)], frame)


def reflective(sk, part, y, sides=SIDES):
    sk.band(part, y, y + 1, (196, 200, 202), 0.06, sides=sides)


# ------------------------------------------------------------------ the Narv-Tech uniforms (black and purple)
NB = (24, 22, 30)        # Narv black
NC = (44, 42, 56)        # charcoal
NG = (68, 66, 82)        # graphite: plates and panels
NP = (120, 58, 198)      # Narv purple
ND = (74, 36, 124)       # deep purple
NL = (110, 50, 150)      # the glow: the light the glow layer adds (it is drawn additively, full bright, over the paint)
NV = (212, 192, 248)     # lavender
NS = (196, 198, 212)     # silver
SOLE = (16, 14, 20)


def nt_base(sk, st, top, legs, sleeves="long", boots=NB, gloves=None):
    """A Narv-Tech suit: the top and the legs, long or short sleeves, the boots with a glowing welt."""
    sk.fill("body", top)
    sk.gradient("body", 1.06, 0.92)
    for arm in ("rarm", "larm"):
        sk.fill(arm, top)
        if sleeves == "short":
            sk.band(arm, 4, 12, st, 0.02)
            sk.band(arm, 3, 4, tone(top, 0.8))
        else:
            sk.band(arm, 10, 12, gloves or st, 0.02 if gloves is None else 0.03)
        sk.fill(arm, gloves or st, faces=("down",), grain=0.02)
        sk.gradient(arm, 1.05, 0.92)
    for leg in ("rleg", "lleg"):
        sk.fill(leg, legs)
        sk.gradient(leg, 1.0, 0.9)
        sk.band(leg, 9, 12, boots, 0.03)
        sk.band(leg, 9, 10, tone(boots, 1.4), 0.02, sides=("front",))
        sk.litband(leg, 11, 12, ND, NL)
        sk.fill(leg, SOLE, faces=("down",), grain=0.02)
    sk.dot("rleg", "front", 3, 0, tone(legs, 0.85)); sk.dot("lleg", "front", 0, 0, tone(legs, 0.85))


def nt_collar(sk, rgb=NP, lit=False):
    """A high collar trimmed in purple, round the neck."""
    for x in (1, 2, 5, 6):
        if lit:
            sk.lit("body", "front", x, 0, x + 1, 1, rgb, NL)
        else:
            sk.dot("body", "front", x, 0, rgb)
    sk.dots("body", "up", [(2, 2), (3, 2), (4, 2), (5, 2), (2, 1), (5, 1)], rgb)
    sk.rect("body", "back", 1, 0, 7, 1, rgb)


def nt_seams(sk, arms=True, legs=True, rgb=ND):
    """Glowing piping down the outer seams of the arms and legs."""
    if arms:
        for arm, f in (("rarm", "right"), ("larm", "left")):
            W, H = sk.size(arm, f)
            sk.lit(arm, f, W // 2, 2, W // 2 + 1, 9, rgb, NL)
    if legs:
        for leg, f in (("rleg", "right"), ("lleg", "left")):
            sk.lit(leg, f, 2, 0, 3, 9, rgb, NL)


def nt_cuffs(sk, y=9, rgb=ND):
    for arm in ("rarm", "larm"):
        sk.litband(arm, y, y + 1, rgb, NL)


def nt_plates(sk, rgb=NG, edge=NP, rows=2):
    """Shoulder plates: the arms' tops and their first rows in graphite, edged in purple."""
    for arm in ("rarm", "larm"):
        sk.fill(arm, rgb, faces=("up",), grain=0.03)
        sk.band(arm, 0, rows, rgb, 0.03)
        sk.band(arm, rows, rows + 1, edge, 0.02)


def nt_belt(sk, rgb=NG, buckle=NS, below=None):
    sk.band("body", 10, 11, rgb, 0.02)
    sk.dots("body", "front", [(3, 10), (4, 10)], buckle)
    if below is not None:
        sk.band("body", 11, 12, below)


def nt_cap(sk, rgb=NB, band=ND, rows=2):
    """A cap with a glowing band round its crown."""
    cap_skin(sk, rgb, rows)
    for f in SIDES:
        sk.lit("hat", f, 0, rows - 1, 8, rows, band, NL)


def nt_back(sk, top):
    """The back: a yoke seam over the shoulder blades (the NARV-TECH print goes on it: props.json "wordmark")."""
    sk.rect("body", "back", 0, 2, 8, 3, tone(top, 0.85))
    sk.rect("body", "back", 0, 5, 8, 6, tone(top, 0.85))


def camo(sk, parts, seed=7):
    """Dark digital camouflage over a part's paint: blocks of charcoal and a dark purple on the black."""
    r = np.random.default_rng(seed)
    for part in parts:
        for f in SIDES:
            W, H = sk.size(part, f)
            for y in range(0, H, 2):
                for x in range(0, W, 2):
                    k = r.random()
                    if k < 0.28:
                        sk.rect(part, f, x, y, x + 2 - (r.random() < 0.4), y + 1 + (r.random() < 0.6), NC, 0.03)
                    elif k < 0.40:
                        sk.rect(part, f, x, y, x + 1 + (r.random() < 0.5), y + 1, (44, 28, 64), 0.03)


def uniform(sk, job, st):
    """Paints the job's Narv-Tech uniform (black and purple, futuristic; the NARV-TECH print and the chest emblem are the
    props' "wordmark" and "emblem"); returns the gear bits it wears and the cap colour (if any)."""
    gear, cap = 0, None
    if job == "controller":
        nt_base(sk, st, NB, NB)
        nt_collar(sk)
        sk.rect("body", "front", 5, 1, 6, 10, NC)                       # the offset zip, its glowing pull
        sk.lit("body", "front", 5, 1, 6, 2, NP, NL)
        nt_plates(sk, NC, ND)
        nt_seams(sk)
        nt_cuffs(sk)
        sk.dots("body", "front", [(2, 2), (3, 3), (4, 3)], tone(NP, 0.9))   # the crew card on a purple cord
        sk.dots("body", "front", [(3, 4), (4, 4), (3, 5), (4, 5)], NS)
        sk.dot("body", "front", 4, 4, NP)
        nt_belt(sk, below=NB)
        nt_back(sk, NB)
        gear |= HEADSET
    elif job == "flight_director":
        nt_base(sk, st, NG, NB)
        nt_collar(sk, NP, lit=True)
        nt_belt(sk, below=NB)
        # the long command coat on the outer layer: open at the front, purple facings, to the knee
        for f in SIDES:
            W, H = sk.size("jacket", f)
            sk.rect("jacket", f, 0, 0, W, 12, NB, 0.03)
        sk.clear("jacket", "front", 3, 0, 5, 12)
        for x in (2, 5):
            sk.lit("jacket", "front", x, 1, x + 1, 12, NP, NL)
        sk.dots("jacket", "front", [(2, 0), (5, 0)], NP)
        sk.gradient("jacket", 1.03, 0.94)
        sk.fill("jacket", NB, faces=("up",)); sk.clear("jacket", "up", 3, 1, 5, 3)
        sk.rect("jacket", "back", 3, 6, 5, 12, NC)                      # the coat's back vent
        for sleeve in ("rsleeve", "lsleeve"):
            for f in SIDES:
                W, H = sk.size(sleeve, f)
                sk.rect(sleeve, f, 0, 0, W, 9, NB, 0.03)
            sk.fill(sleeve, NB, faces=("up",))
            sk.litband(sleeve, 8, 9, NP, NL)
        for sleeve, f in (("rsleeve", "right"), ("lsleeve", "left")):  # shoulder boards, three silver bars
            for f2 in SIDES:
                sk.rect(sleeve, f2, 0, 0, sk.size(sleeve, f2)[0], 1, NG, 0.02)
            sk.dots(sleeve, f, [(1, 1), (2, 1), (1, 2), (2, 2)], NS)
        for pants in ("rpants", "lpants"):
            for f in SIDES:
                sk.rect(pants, f, 0, 0, 4, 6, NB, 0.03)
            sk.rect(pants, "front", 1 if pants == "rpants" else 0, 0, 3 if pants == "rpants" else 2, 6, NB)
        sk.clear("rpants", "front", 3, 0, 4, 6); sk.clear("lpants", "front", 0, 0, 1, 6)
        nt_seams(sk, arms=False)
        nt_back(sk, NB)
        gear |= HEADSET
    elif job == "security":
        # the power armour's undersuit (the armour itself is a mesh on the bones: tools/power_armor.py): a black
        # bodyglove, ribbed where it flexes (neck, waist, elbows, knees), a deep violet ring at each elbow and knee
        UB, UR = (17, 16, 23), (31, 29, 41)
        nt_base(sk, st, UB, UB, gloves=UB)
        for part in ("body", "rarm", "larm", "rleg", "lleg"):
            sk.fill(part, UB, grain=0.02)
        UP = (50, 24, 90)
        for arm in ("rarm", "larm"):
            for y in (5, 7):
                sk.band(arm, y, y + 1, UR, 0.01)
            sk.band(arm, 6, 7, UP, 0.01)
            sk.band(arm, 10, 12, (26, 24, 34), 0.02)                     # the glove
            sk.fill(arm, (26, 24, 34), faces=("down",), grain=0.02)
        for leg in ("rleg", "lleg"):
            for y in (4, 6):
                sk.band(leg, y, y + 1, UR, 0.01)
            sk.band(leg, 5, 6, UP, 0.01)
            sk.band(leg, 9, 12, SOLE, 0.02)
            sk.fill(leg, SOLE, faces=("down",), grain=0.02)
        for y in (0, 8, 10):                                             # the neck's seal and the waist
            sk.band("body", y, y + 1, UR, 0.01)
        gear |= HELMET | EXO
    elif job == "gate_guard":
        nt_base(sk, st, NB, NB, gloves=NC)
        nt_collar(sk, ND)
        nt_plates(sk, NG, NP, rows=3)
        nt_belt(sk, NG, NS, below=NB)
        for f in ("right", "left"):                                     # the belt's pouches
            sk.dots("jacket", f, [(1, 10), (2, 10), (1, 11), (2, 11)], NC)
        # the chest plate on the outer layer: graphite, a glowing strip across it
        sk.rect("jacket", "front", 1, 1, 7, 8, NG, 0.03)
        sk.rect("jacket", "front", 1, 8, 7, 9, ND)
        sk.lit("jacket", "front", 2, 4, 6, 5, ND, NL)
        sk.rect("jacket", "back", 1, 1, 7, 7, NG, 0.03)
        sk.gradient("jacket", 1.04, 0.94)
        nt_seams(sk, arms=False)
        nt_cap(sk, NB, ND)
        sk.dots("hat", "front", [(3, 0), (4, 0)], NS)
        gear |= CAP | BATON; cap = (NB, SOLE)
        # a light vest in place of a hi-vis one: bands down the front over the shoulders, round the waist, and down
        # the back clear of the print
        for x in (1, 6):
            sk.lit("jacket", "front", x, 0, x + 1, 10, NP, NL)
            sk.lit("jacket", "back", x, 0, x + 1, 2, NP, NL)
            sk.lit("jacket", "back", x, 6, x + 1, 10, NP, NL)
        for f in SIDES:
            W, H = sk.size("jacket", f)
            sk.lit("jacket", f, 0, 7, W, 8, NP, NL)
        sk.dots("jacket", "up", [(1, 0), (1, 1), (1, 2), (1, 3), (6, 0), (6, 1), (6, 2), (6, 3)], NP)
    elif job == "mechanic":
        nt_base(sk, st, NC, NC, gloves=NG)
        nt_collar(sk, ND)
        sk.rect("body", "front", 4, 1, 5, 10, NB)                       # the coverall's zip
        nt_belt(sk, NG, NS, below=NC)
        for arm in ("rarm", "larm"):                                    # elbow pads and glowing bands
            sk.band(arm, 5, 7, ND, 0.03)
            sk.litband(arm, 3, 4, ND, NL)
        for leg in ("rleg", "lleg"):
            sk.band(leg, 5, 7, ND, 0.03)                                # knee pads
            sk.litband(leg, 3, 4, ND, NL)
        sk.litband("body", 6, 7, ND, NL)
        # the tool harness at the hips (outer layer)
        sk.rect("rpants", "right", 0, 0, 4, 3, NG); sk.rect("rpants", "front", 0, 0, 2, 3, NG)
        sk.rect("lpants", "left", 0, 0, 4, 3, NG); sk.rect("lpants", "front", 2, 0, 4, 3, NG)
        sk.dots("rpants", "right", [(1, 0), (2, 0)], NS); sk.dots("lpants", "left", [(1, 0), (2, 0)], NS)
        nt_back(sk, NC)
        glasses_skin(sk, frame=NB, lens=(150, 110, 210))
        gear |= HARDHAT
    elif job == "tech":
        nt_base(sk, st, NB, NB)
        for f in SIDES:                                                 # the purple shoulder yoke
            W, H = sk.size("body", f)
            sk.rect("body", f, 0, 0, W, 2, NP, 0.03)
        sk.dots("body", "front", [(3, 0), (4, 0)], st)
        sk.fill("body", NP, faces=("up",))
        for arm in ("rarm", "larm"):
            sk.band(arm, 0, 2, NP, 0.03); sk.fill(arm, NP, faces=("up",))
            sk.litband(arm, 5, 6, ND, NL)
        sk.lit("body", "front", 0, 2, 8, 3, ND, NL)
        nt_belt(sk, below=NB)
        nt_seams(sk, arms=False)
        nt_back(sk, NB)
        nt_cap(sk, NC, ND)
        gear |= CAP; cap = (NC, tone(NC, 0.85))
    elif job == "cook":
        nt_base(sk, st, NB, NB)
        sk.dots("body", "front", [(2, 3), (2, 5), (2, 7), (5, 3), (5, 5), (5, 7)], NV)   # double-breasted, lavender studs
        sk.rect("body", "front", 4, 1, 5, 9, NC)
        nt_collar(sk, NP)
        nt_cuffs(sk, 9, NP)
        for leg in ("rleg", "lleg"):                                    # black and purple check trousers
            for f in SIDES:
                W, H = sk.size(leg, f)
                for y in range(9):
                    for x in range(W):
                        sk.dot(leg, f, x, y, NB if (x + y) % 2 == 0 else ND, 0.02)
        sk.band("body", 10, 12, NB)
        ap = NG                                                         # the apron, its hem glowing
        sk.rect("jacket", "front", 0, 9, 8, 12, ap); sk.band("jacket", 9, 10, ap)
        for pants in ("rpants", "lpants"):
            sk.rect(pants, "front", 0, 0, 4, 7, ap)
            sk.lit(pants, "front", 0, 6, 4, 7, ND, NL)
        sk.rect("rpants", "right", 3, 0, 4, 7, ap); sk.rect("lpants", "left", 0, 0, 1, 7, ap)
        nt_back(sk, NB)
        nt_cap(sk, NB, NP, rows=2)
    elif job == "server":
        nt_base(sk, st, NB, NC, sleeves="short")
        nt_collar(sk, NP)
        nt_belt(sk, below=NC)
        ap = ND                                                         # the purple waistcoat-apron, silver seams
        sk.rect("jacket", "front", 1, 3, 7, 12, ap)
        sk.dots("jacket", "front", [(1, 0), (1, 1), (1, 2), (6, 0), (6, 1), (6, 2)], tone(ap, 0.85))
        sk.dots("jacket", "up", [(1, 2), (1, 3), (6, 2), (6, 3)], tone(ap, 0.85))
        sk.lit("jacket", "front", 1, 8, 7, 9, NP, NL)
        sk.rect("jacket", "back", 0, 8, 8, 9, tone(ap, 0.85))
        sk.rect("rpants", "front", 0, 0, 4, 8, ap); sk.rect("lpants", "front", 0, 0, 4, 8, ap)
        sk.dots("jacket", "front", [(2, 5), (5, 5)], NS)
        nt_back(sk, NB)
        nt_cap(sk, NB, ND)
        gear |= CAP; cap = (NB, SOLE)
    elif job == "pilot":
        nt_base(sk, st, NB, NB, gloves=NC)                              # the black flight suit, gloves
        nt_collar(sk, NP, lit=True)
        sk.rect("body", "front", 4, 1, 5, 11, NC)                       # the zip
        sk.dot("body", "front", 4, 1, NS)
        for f in ("right", "left"):                                     # graphite side panels, purple shoulders
            sk.rect("body", f, 0, 2, 4, 10, NG, 0.03)
        for arm, f in (("rarm", "right"), ("larm", "left")):
            sk.band(arm, 0, 3, NP, 0.03); sk.fill(arm, NP, faces=("up",))
            sk.rect(arm, f, 1, 4, 3, 6, NG)                             # the shoulder pocket
        sk.lit("body", "front", 1, 3, 2, 10, ND, NL)                    # glowing lines down the chest
        sk.lit("body", "front", 6, 3, 7, 10, ND, NL)
        nt_belt(sk, NG, NS, below=NB)
        for leg, f in (("rleg", "right"), ("lleg", "left")):
            sk.rect(leg, f, 1, 3, 4, 6, NG)                             # the thigh pockets
        nt_seams(sk, arms=False)
        nt_back(sk, NB)
        gear |= HEADSET
    elif job == "engineer":
        nt_base(sk, st, ND, NB)                                         # a deep purple shirt, a black tie
        sk.dots("body", "front", [(3, 1), (4, 1)], NB)
        sk.rect("body", "front", 3, 2, 5, 8, NB)
        sk.dot("body", "front", 4, 3, NP)
        nt_belt(sk, below=NB)
        # the black lab coat on the outer layer: open down the front on its purple lining, to the knee
        for f in SIDES:
            W, H = sk.size("jacket", f)
            sk.rect("jacket", f, 0, 0, W, 12, NB, 0.025)
        sk.clear("jacket", "front", 3, 0, 5, 12)
        for x in (2, 5):
            sk.rect("jacket", "front", x, 0, x + 1, 12, NP, 0.02)
        sk.gradient("jacket", 1.03, 0.94)
        sk.fill("jacket", NB, faces=("up",)); sk.clear("jacket", "up", 3, 1, 5, 3)
        for sleeve in ("rsleeve", "lsleeve"):
            for f in SIDES:
                W, H = sk.size(sleeve, f)
                sk.rect(sleeve, f, 0, 0, W, 9, NB, 0.025)
            sk.fill(sleeve, NB, faces=("up",))
            sk.litband(sleeve, 8, 9, ND, NL)
        for pants, outer in (("rpants", "right"), ("lpants", "left")):
            sk.rect(pants, outer, 0, 0, 4, 6, NB, 0.025)
            sk.rect(pants, "back", 0, 0, 4, 6, NB, 0.025)
            sk.rect(pants, "front", 0 if pants == "rpants" else 1, 0, 3 if pants == "rpants" else 4, 6, NB, 0.025)
            sk.rect(pants, "front", 0, 5, 4, 6, NC)
        sk.rect("rpants", "front", 2, 0, 3, 6, NP); sk.rect("lpants", "front", 1, 0, 2, 6, NP)
        nt_back(sk, ND)
    elif job == "medic":
        nt_base(sk, st, NB, NB, sleeves="short", boots=NC)              # black scrubs
        sk.dots("body", "front", [(3, 0), (4, 0), (3, 1), (4, 1)], st)  # the V neck, purple trimmed
        sk.dots("body", "front", [(2, 0), (5, 0), (2, 1), (5, 1)], NP)
        for arm, f in (("rarm", "right"), ("larm", "left")):           # a glowing cross on each sleeve
            sk.rect(arm, f, 0, 0, 3, 3, NP)
            for x, y in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
                sk.lit(arm, f, x, y, x + 1, y + 1, NV, (120, 110, 140))
        nt_seams(sk, arms=False)
        sk.dots("body", "front", [(2, 2), (2, 3), (2, 4)], NS)           # the stethoscope
        sk.dot("body", "front", 2, 5, NS)
        sk.rect("body", "front", 0, 6, 2, 8, NC)
        for arm in ("rarm", "larm"):
            sk.litband(arm, 3, 4, ND, NL)
        nt_back(sk, NB)
    elif job == "manager":
        suit = (30, 28, 38)
        nt_base(sk, st, NV, suit, boots=SOLE)                           # a lavender shirt
        sk.dots("body", "front", [(3, 1), (4, 1)], ND)
        sk.rect("body", "front", 3, 2, 5, 9, NP)                         # the purple tie
        sk.dot("body", "front", 4, 4, ND)
        sk.band("body", 10, 12, suit)
        for leg in ("rleg", "lleg"):
            sk.band(leg, 11, 12, SOLE, 0.02)                            # (shoes, no glowing welt)
        # the suit jacket on the outer layer: collarless, a purple edge down the opening and round the hem
        for f in SIDES:
            W, H = sk.size("jacket", f)
            sk.rect("jacket", f, 0, 0, W, 12, suit, 0.03)
        sk.clear("jacket", "front", 3, 0, 5, 7)
        sk.clear("jacket", "front", 2, 0, 3, 3); sk.clear("jacket", "front", 5, 0, 6, 3)
        sk.dots("jacket", "front", [(2, 3), (2, 4), (2, 5), (5, 3), (5, 4), (5, 5), (3, 6), (4, 6)], NP)
        sk.dots("jacket", "front", [(4, 8), (4, 10)], NS)
        sk.dot("jacket", "front", 6, 3, NP)                             # the pocket square
        sk.gradient("jacket", 1.03, 0.95)
        sk.fill("jacket", suit, faces=("up",)); sk.clear("jacket", "up", 3, 1, 5, 3)
        for sleeve in ("rsleeve", "lsleeve"):
            for f in SIDES:
                W, H = sk.size(sleeve, f)
                sk.rect(sleeve, f, 0, 0, W, 9, suit, 0.03)
            sk.fill(sleeve, suit, faces=("up",))
            sk.band(sleeve, 8, 9, ND, 0.02)
        for pants in ("rpants", "lpants"):
            for f in SIDES:
                sk.rect(pants, f, 0, 0, 4, 1, suit, 0.03)
        sk.rect("body", "back", 0, 2, 8, 3, tone(NV, 0.9))
    elif job == "janitor":
        nt_base(sk, st, NC, NC, sleeves="short", gloves=None)
        nt_collar(sk, ND)
        sk.rect("body", "front", 4, 2, 5, 10, NB)
        nt_belt(sk, NB, NS, below=NC)
        for arm in ("rarm", "larm"):                                    # purple gloves to the elbow
            sk.band(arm, 7, 12, NP, 0.03)
            sk.band(arm, 7, 8, ND, 0.02)
            sk.fill(arm, NP, faces=("down",))
        sk.litband("body", 8, 9, ND, NL)
        for leg in ("rleg", "lleg"):
            sk.litband(leg, 5, 6, ND, NL)
        nt_back(sk, NC)
    else:
        raise ValueError(job)
    return gear, cap


# ------------------------------------------------------------------ gear (per skin) and props (shared)
#
# Boxes are given here as they were drawn for the mockup (y up from the feet, the front at +z, model pixels) and turned
# into the game's model space (y down from the neck, the front at -z) by mc_box(). Head and body gear hangs off the
# neck (the head's and body's pivot); props off the fist (their own pivot inside the arm) or, the cart, off the root.

def mc_box(lo, hi, pivot_mc=(0.0, 0.0, 0.0)):
    """A box (lo, hi) in the mockup's frame to (from, size) in model space relative to a part's pivot."""
    (x0, y0, z0), (x1, y1, z1) = lo, hi
    frm = (x0 - pivot_mc[0], (24.0 - y1) - pivot_mc[1], -z1 - pivot_mc[2])
    return [round(v, 4) for v in frm], [round(x1 - x0, 4), round(y1 - y0, 4), round(z1 - z0, 4)]


def hand_box(lo, hi):
    """A box in a fist's frame (x across the arm, y up the arm, z forward) to (from, size) relative to the fist."""
    (x0, y0, z0), (x1, y1, z1) = lo, hi
    return [round(x0, 4), round(-y1, 4), round(-z1, 4)], [round(x1 - x0, 4), round(y1 - y0, 4), round(z1 - z0, 4)]


GEAR = [  # (group, bit, [(name, lo, hi, colour key)])
    ("headset", HEADSET, [("cup_r", (-4.95, 25.0, -1.3), (-4.25, 28.0, 1.3), "cup"), ("cup_l", (4.25, 25.0, -1.3), (4.95, 28.0, 1.3), "cup"),
                          ("band_r", (-4.8, 28.0, -0.45), (-4.6, 32.6, 0.45), "band"), ("band_l", (4.6, 28.0, -0.45), (4.8, 32.6, 0.45), "band"),
                          ("band", (-4.8, 32.6, -0.45), (4.8, 32.8, 0.45), "band"),
                          ("boom", (-4.9, 25.7, 1.3), (-4.6, 26.0, 4.6), "pad"), ("mic", (-4.9, 25.4, 4.6), (-3.2, 26.2, 5.1), "mic")]),
    ("cap_peak", CAP, [("peak", (-4.5, 29.8, 4.4), (4.5, 30.4, 7.4), "peak")]),
    ("hardhat", HARDHAT, [("dome", (-4.8, 29.4, -4.8), (4.8, 32.8, 4.8), "hat"), ("ridge", (-4.6, 32.8, -1.0), (4.6, 33.2, 1.0), "hat2"),
                          ("brim", (-5.4, 29.0, -5.4), (5.4, 29.4, 6.6), "brim")]),
    ("bun", BUN, [("bun", (-2.0, 27.6, -6.6), (2.0, 30.8, -4.45), "hair")]),
    ("ponytail", PONYTAIL, [("tie", (-1.0, 27.4, -5.1), (1.0, 28.4, -4.45), "tie"), ("tail", (-1.0, 22.6, -5.8), (1.0, 27.6, -4.6), "hair")]),
    # shut eyes: a lid over each eye (the face's row 4, its columns 1-2 and 5-6), 0.1 px off the face, a lash line along
    # its foot 0.1 px off the lid (under the hat layer's 4.5 and its glasses)
    ("eyelids", EYELIDS, [("lid_r", (-3.0, 27.0, 4.1), (-1.0, 28.0, 4.2), "lid"), ("lid_l", (1.0, 27.0, 4.1), (3.0, 28.0, 4.2), "lid"),
                          ("lash_r", (-3.0, 27.0, 4.3), (-1.0, 27.3, 4.4), "lash"), ("lash_l", (1.0, 27.0, 4.3), (3.0, 27.3, 4.4), "lash")]),
]


GEAR_GLOW = {"mic": ("front",), "hat2": FACES}      # what glows on the gear: the mic's tip, the hard hat's ridge


def gear_colours(cap, hc, st):
    peak = cap[1] if cap else (30, 30, 30)
    return {"lid": {"*": tone(st, 0.93), "front": tone(st, 0.95)}, "lash": {"*": mix(tone(hc, 0.55), (40, 30, 26), 0.5)},
            "cup": {"*": (40, 40, 44)}, "band": {"*": (30, 30, 32)}, "pad": {"*": (22, 22, 24)},
            "mic": {"*": (20, 20, 22), "front": (120, 58, 198)},
            "peak": {"*": peak, "down": tone(peak, 0.7)},
            "hat": {"*": (64, 62, 78), "front": (64, 62, 78)}, "hat2": {"*": (120, 58, 198)},
            "brim": {"*": (52, 50, 64), "down": (30, 28, 38)},
            "hair": {"*": hc, "up": tone(hc, 1.1), "back": tone(hc, 0.92)}, "tie": {"*": (40, 40, 44)}}


class Packer:
    """Shelf packing of box unwraps into a region of a texture."""

    def __init__(self, x0, y0, w, h):
        self.x0, self.y0, self.w, self.h = x0, y0, w, h
        self.x, self.y, self.row = x0, y0, 0

    def place(self, size):
        w, h, d = size
        uw, uh = 2 * math.ceil(d) + 2 * math.ceil(w), math.ceil(d) + math.ceil(h)
        if self.x + uw > self.x0 + self.w:
            self.x, self.y, self.row = self.x0, self.y + self.row, 0
        if self.y + uh > self.y0 + self.h:
            raise ValueError("the texture is full")
        at = (self.x, self.y)
        self.x += uw
        self.row = max(self.row, uh)
        return at


def gear_layout():
    """The gear groups with each box's model-space box and texture offset (the same for every skin)."""
    pk = Packer(64, 0, 64, 64)
    groups = []
    for group, bit, boxes in GEAR:
        out = []
        for name, lo, hi, key in boxes:
            frm, size = mc_box(lo, hi)
            out.append({"name": name, "from": frm, "size": size, "uv": list(pk.place(size)), "paint": key})
        groups.append({"name": group, "parent": "head", "pivot": [0, 0, 0], "bit": bit, "boxes": out})
    return groups


# the props, in a fist's frame (tablet and clipboard in the left hand, the rest in the right) or, the cart, the root's
PROP_BOXES = {
    "tablet": ("left_arm", [("slate", (-2.5, -0.3, -1.0), (2.5, 0.3, 6.0), "tablet")]),
    "clipboard": ("left_arm", [("board", (-2.5, -0.25, -1.0), (2.5, 0.25, 6.0), "clipboard")]),
    "radio": ("right_arm", [("body", (-0.9, -2.5, -1.1), (0.9, 3.0, 1.1), "radio"), ("aerial", (-0.25, 3.0, 0.3), (0.25, 5.5, 0.8), "aerial")]),
    "toolbox": ("right_arm", [("box", (-1.5, -5.6, -3.4), (1.5, -1.8, 3.4), "toolbox"), ("post_b", (-0.4, -1.8, -1.6), (0.4, -1.2, -1.2), "dark"),
                              ("post_f", (-0.4, -1.8, 1.2), (0.4, -1.2, 1.6), "dark"), ("grip", (-0.4, -1.2, -1.6), (0.4, -0.6, 1.6), "dark")]),
    "mug": ("right_arm", [("cup", (-1.1, -0.6, 0.2), (1.1, 2.0, 2.4), "mug"), ("handle", (1.1, 0.0, 0.9), (1.7, 1.4, 1.7), "mug_h")]),
    "tray": ("right_arm", [("plate", (-3.0, 0.6, -1.0), (6.0, 1.1, 6.0), "plate"),
                           ("cup_a", (-2.0, 1.2, 0.1), (-0.2, 3.0, 1.9), "cup"), ("cup_b", (1.0, 1.2, 2.7), (2.8, 3.0, 4.5), "cup"),
                           ("cup_c", (3.2, 1.2, -0.1), (5.0, 3.0, 1.7), "cup")]),
}
# the blanket a sleeper lies under on a bunk or a sofa (tower/crew/TowerCrew ACT_LIE; the renderer lays the body
# down): in the standing frame (y up from the feet, z forward), from the collar to past the hips' end of the legs, 12
# px across, 1 px over the body's front and 0.1 px off its back; a turned-down sheet at the top, 0.1 px proud of it
BLANKET_BOXES = [("wool", (-6.0, 3.0, -1.9), (6.0, 21.7, 3.0), "wool"), ("sheet", (-6.1, 21.6, -1.8), (6.1, 23.0, 3.1), "sheet")]
CART_FORWARD = 12.3   # the cart's middle ahead of the janitor (0.72 blocks at the crew's 0.9375 scale)
CART_BOXES = [  # the mockup's cart: y up from the floor, its handle at -z
    ("base", (-4.0, 2.0, -6.0), (4.0, 3.0, 8.0), "grey"), ("shelf", (-4.0, 12.0, -6.0), (4.0, 12.6, 1.0), "grey"),
    ("leg_a", (-4.0, 3.0, -6.0), (-3.4, 12.0, -5.4), "dark"), ("leg_b", (3.4, 3.0, -6.0), (4.0, 12.0, -5.4), "dark"),
    ("leg_c", (-4.0, 3.0, 0.4), (-3.4, 12.0, 1.0), "dark"), ("leg_d", (3.4, 3.0, 0.4), (4.0, 12.0, 1.0), "dark"),
    ("bar", (-4.4, 15.0, -7.0), (4.4, 15.6, -6.4), "dark"), ("bar_r", (-4.4, 12.6, -7.0), (-3.8, 15.0, -6.4), "dark"),
    ("bar_l", (3.8, 12.6, -7.0), (4.4, 15.0, -6.4), "dark"),
    ("bin", (-2.8, 3.0, -4.6), (2.8, 11.6, -0.4), "grey"), ("bag", (-3.0, 11.6, -4.8), (3.0, 12.4, -0.2), "bag"),
    ("bucket", (-2.8, 3.0, 2.0), (2.8, 7.6, 7.6), "yellow"), ("wringer", (-2.0, 7.6, 2.6), (2.0, 9.4, 4.6), "yellow"),
    ("mop", (0.6, 9.4, 3.4), (1.2, 24.0, 4.0), "blue"), ("spray_a", (-2.0, 12.6, -3.8), (-1.0, 15.0, -2.8), "blue"),
    ("spray_b", (-0.4, 12.6, -3.8), (0.6, 15.6, -2.8), "green"),
    ("wheel_a", (-4.1, 0.0, -6.2), (-3.1, 2.0, -4.2), "tyre"), ("wheel_b", (3.1, 0.0, -6.2), (4.1, 2.0, -4.2), "tyre"),
    ("wheel_c", (-4.1, 0.0, 6.2), (-3.1, 2.0, 8.2), "tyre"), ("wheel_d", (3.1, 0.0, 6.2), (4.1, 2.0, 8.2), "tyre"),
]
PROP_PAINT = {"tablet": {"*": (32, 32, 34), "up": "screen"}, "clipboard": {"*": (120, 82, 48), "up": "paper"},
              "radio": {"*": (30, 30, 32), "front": (52, 52, 56)}, "aerial": {"*": (50, 50, 52)},
              "toolbox": {"*": (196, 42, 32), "up": (170, 36, 28)}, "dark": {"*": (40, 40, 42)},
              "mug": {"*": (238, 238, 234), "up": (70, 44, 26)}, "mug_h": {"*": (238, 238, 234)},
              "plate": {"*": (44, 44, 46)}, "cup": {"*": (238, 238, 234), "up": (82, 52, 30)},
              "wool": {"*": (46, 62, 96), "front": "knit", "down": (40, 54, 84)}, "sheet": {"*": (232, 232, 226), "up": (240, 240, 236)},
              "grey": {"*": (78, 82, 86)}, "bag": {"*": (26, 26, 28)}, "yellow": {"*": (236, 196, 30)},
              "blue": {"*": (40, 120, 200)}, "green": {"*": (60, 170, 90)}, "tyre": {"*": (24, 24, 26)}}
FIST = {"right_arm": (-1.0, 8.5, 0.0), "left_arm": (1.0, 8.5, 0.0)}


# the Narv-Tech print (Justin, 6 November 2026: "all uniforms to say Narv-Tech somewhere on it"): flat decals on the
# props layer, hung off the body and always shown (prop 0), finer than the skin's pixels (their boxes are in texels, the
# part scaled down): NARV-TECH across the shoulder blades, 0.1 px off the outer layer's back (2.25), and the emblem on the
# left breast, 0.1 px off its front
FONT = {  # 5 x 7, squared off
    "N": ["X...X", "XX..X", "XX..X", "X.X.X", "X..XX", "X..XX", "X...X"],
    "A": ["XXXXX", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"],
    "R": ["XXXX.", "X...X", "X...X", "XXXX.", "X..X.", "X...X", "X...X"],
    "V": ["X...X", "X...X", "X...X", "X...X", "X...X", ".X.X.", "..X.."],
    "-": ["...", "...", "...", "XXX", "...", "...", "..."],
    "T": ["XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X.."],
    "E": ["XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "XXXXX"],
    "C": ["XXXXX", "X....", "X....", "X....", "X....", "X....", "XXXXX"],
    "H": ["X...X", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"],
}
WORDMARK = "NARV-TECH"
PRINT_INK, PRINT_GLOW = (232, 224, 255), (150, 100, 190)       # the letters: near white, glowing lavender (added light)
PRINT_LINE, PRINT_LINE_GLOW = (120, 58, 198), (110, 50, 150)   # the purple rule under them
BADGE_EDGE, BADGE_FIELD = (120, 58, 198), (24, 22, 30)


def text_mask(text):
    cols = []
    for i, ch in enumerate(text):
        g = FONT[ch]
        if i:
            cols.append([False] * 7)
        for x in range(len(g[0])):
            cols.append([g[y][x] == "X" for y in range(7)])
    return np.array(cols, bool).T                                # (7, width)


PRINT_W = text_mask(WORDMARK).shape[1] + 2                         # a texel either side
PRINT_H = 10                                                       # a texel over, 7 of letters, a gap, the rule
PRINT_SCALE = 0.148                                                # 53 texels: 7.84 px across the back (8)
BADGE = 12
BADGE_SCALE = 0.18                                                 # 2.16 px
DECALS = [  # (name, pivot (body part, model space), size in texels, scale, the face that shows)
    ("wordmark", (0.0, 3.75, 2.35), (PRINT_W, PRINT_H), PRINT_SCALE, "back"),
    ("emblem", (2.05, 2.6, -2.35), (BADGE, BADGE), BADGE_SCALE, "front"),
]


def decal_art(name):
    """A decal's texels: rgb (h, w, 3), alpha (h, w), glow (h, w) bool, as seen from outside."""
    if name == "wordmark":
        h, w = PRINT_H, PRINT_W
        rgb = np.zeros((h, w, 3), np.float32); a = np.zeros((h, w), np.float32); glow = np.zeros((h, w, 3), np.float32)
        m = text_mask(WORDMARK)
        rgb[1:8, 1:w - 1][m] = PRINT_INK; a[1:8, 1:w - 1][m] = 1; glow[1:8, 1:w - 1][m] = PRINT_GLOW
        rgb[9, 1:w - 1] = PRINT_LINE; a[9, 1:w - 1] = 1; glow[9, 1:w - 1] = PRINT_LINE_GLOW
        return rgb, a, glow
    # the emblem: a hexagon edged in glowing purple, an N on its black field
    n = BADGE
    from PIL import ImageDraw
    big = Image.new("L", (n * 8, n * 8), 0)
    d = ImageDraw.Draw(big)
    c, r = n * 4, n * 4 - 2
    hexagon = [(c + r * math.sin(math.radians(60 * k)), c - r * math.cos(math.radians(60 * k))) for k in range(6)]
    d.polygon(hexagon, fill=255)
    outer = np.asarray(big.resize((n, n), Image.BOX)) > 110
    inner = np.zeros_like(outer)
    for y in range(n):
        for x in range(n):
            inner[y, x] = outer[y, x] and all(0 <= y + dy < n and 0 <= x + dx < n and outer[y + dy, x + dx]
                                              for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    rgb = np.zeros((n, n, 3), np.float32); a = outer.astype(np.float32); glow = np.zeros((n, n, 3), np.float32)
    rgb[outer] = BADGE_EDGE; glow[outer & ~inner] = PRINT_LINE_GLOW
    rgb[inner] = BADGE_FIELD
    m = text_mask("N")
    y0, x0 = (n - 7) // 2, (n - 5) // 2
    sub = np.zeros((n, n), bool); sub[y0:y0 + 7, x0:x0 + 5] = m
    rgb[sub] = PRINT_INK; glow[sub] = PRINT_GLOW
    return rgb, a, glow


# the exoskeleton: a frame on the props layer, its groups hung off the body, the arms and the legs (each box in its
# part's own space, as the Java adds it), shown for a skin that wears it (gear bit EXO) and taken off to sleep. Each box
# 0.1 px or more off the outer layer (the jacket 4.25 x 2.25, the sleeves 0.35 out, the trousers 0.25 / 0.15 out).
# Body: a yoke over the shoulder blades and the power pack and spine below the NARV-TECH print (y 3.0-4.5, kept clear),
# a frame round the waist. Arms (right; the left mirrored): a lit actuator at the shoulder and the elbow on the outer
# side, struts between, a cuff round the forearm. Legs: a lit actuator at the hip and the knee on the outer side, struts,
# a guard over the knee, a brace round the ankle.
# The power armour, its helmet and the squad leaders' crests are a mesh on the bones now (tools/power_armor.py,
# client/PowerArmor: Justin, 9 November 2026, "make the full set of Narv-Tech power armor ... redesign it"), not boxes
# here. Their groups stay, empty, so the gear's bits keep their meaning to the models: EXO still takes the NARV-TECH print
# and the emblem off the uniform (props.json "hide": the suit carries its own), HELMET still hides the head.
EXO_GROUPS = [("exo_back", "body", []), ("exo_body", "body", []), ("exo_right_arm", "right_arm", []), ("exo_left_arm", "left_arm", []),
              ("exo_right_leg", "right_leg", []), ("exo_left_leg", "left_leg", [])]      # (group, parent, boxes)
EXO_DECALS = []       # (group, art, pivot, scale)
HELMET_BOXES = []
HELMET_PAINT = {"hm_shell": {"*": (52, 50, 64), "up": (62, 60, 76), "down": (28, 26, 34), "back": "stripe"},
                "hm_neck": {"*": (40, 38, 50), "front": "ribs", "back": "ribs", "right": "ribs", "left": "ribs"},
                "hm_frame": {"*": (74, 72, 88), "up": (90, 88, 104), "down": (44, 42, 54)},
                "hm_visor": {"*": (60, 28, 104), "front": "glass"}, "hm_chin": {"*": (64, 62, 78), "front": "grille"},
                "hm_crest": {"*": (74, 36, 124), "up": "lit"}, "hm_pod": {"*": (34, 32, 42), "right": "ring", "left": "ring"},
                "hm_aerial": {"*": (30, 28, 36), "up": "lit"}}


# the plasma rifle (Justin, 6 November 2026: "make a plasma rifle for them to have"): security carry it at the ready and
# aim it at what they shoot. Since the NT-7 (P6, 9 November 2026) the rifle in their hands is the item itself
# (tools/plasma_rifle_nt7.py), drawn by client/TowerCrewRenderer at this group's pose: no boxes here, just where it hangs.
# Its frame (x across, y up, z to the muzzle; px, the origin at the top of the grip, under the body) is laid along the
# top of the right forearm with the grip in the fist, the part scaled by a half, a crew pixel to the rifle's
# (TowerCrewRenderer.RIFLE_K).
RIFLE_SCALE = 0.5
RIFLE_AT = (-1.0, 9.2, -2.9)         # the grip's top in the right arm's space: the fist, 0.1 off the gauntlet's top (2.8)


# the plasma baton (rebuilt 10 November 2026, r203: Justin picked A, "Patrol", from the rebuild sheets): the baton itself
# is the item (tools/plasma_baton.py: round moulded parts), drawn by client/TowerCrewRenderer at these two places, a crew
# pixel to the item's: in the right fist, the middle of its grip there, pointing ahead of the fist (the arm's -z); and in
# the holster, stowed (its rod telescoped into the handle), its guard resting on the mouth, the handle up. Its plasma is
# drawn by the client as it lights (client/BatonPlasma). No boxes here but the holster's.
BATON_AT = (-1.0, 9.6, 0.0)          # the grip's middle in the right arm's space: the fist
# the holster down the back of the right thigh (the leg's space; the hip joint at y 0, the foot at 12): a cup that takes
# the stowed baton from its guard to its end cap (the item's 4.8 px), 0.35 off the trousers' back (2.25) and clear of
# the arm (its inner face 4.1 out from the middle: the leg's -2.1), a steel clip round its mouth, a strap round the
# thigh and one under it, each joined to the cup by a hanger, and a drop strap up to the belt. The mouth is low enough
# that the whole handle stays below the hip joint but its butt stud (1.1 px over it): the leg turns about that joint, so
# anything standing higher would swing through the guard's back at every stride (as the full-length baton's grip would:
# why it is carried stowed). (Before r203 the holstered baton's grip was drawn the wrong way up, inside the sheath.)
HOLSTER = [("cup", (-2.1, 4.9, 2.6), (0.0, 10.0, 4.7), "hs_sheath"), ("clip", (-2.2, 4.8, 2.5), (0.1, 5.4, 4.8), "hs_clip"),
           ("strap", (-2.35, 5.6, -2.35), (2.2, 6.3, 2.35), "hs_strap"), ("hanger", (-1.5, 5.7, 2.35), (-0.6, 6.2, 2.6), "hs_drop"),
           ("strap_low", (-2.35, 8.7, -2.35), (2.2, 9.4, 2.35), "hs_strap"), ("hanger_low", (-1.5, 8.8, 2.35), (-0.6, 9.3, 2.6), "hs_drop"),
           ("drop", (-1.5, 0.3, 2.3), (-0.6, 5.3, 2.5), "hs_drop")]
HOLSTERED_AT = (-1.05, 4.8, 3.65)    # the holstered baton on the middle of the clip's mouth (the leg's space): its guard's
                                     # front face 0.1 inside it (client/TowerCrewRenderer.BatonLayer: HOLSTER_SEAT)
BATON_PAINT = {"hs_sheath": {"*": (24, 22, 30), "right": "hs_side", "left": "hs_side", "back": "hs_side", "up": (16, 14, 20)},
               "hs_strap": {"*": (30, 28, 36), "front": "litrow", "back": "litrow", "right": "litrow", "left": "litrow"},
               "hs_clip": {"*": (150, 152, 164)}, "hs_drop": {"*": (30, 28, 36)}}


EXO_PAINT = {"exo_frame": {"*": (74, 72, 88), "up": (90, 88, 104), "down": (44, 42, 54)},
             "exo_strut": {"*": (118, 116, 134), "up": (140, 138, 156), "down": (78, 76, 90)},      # steel struts
             "exo_hub": {"*": (40, 38, 50), "right": "ring", "left": "ring"},
             "exo_pack": {"*": (70, 68, 84), "back": "core", "right": "vents", "left": "vents", "up": (84, 82, 98)},
             "exo_band": {"*": (74, 72, 88), "front": "litrow", "back": "litrow", "right": "litrow", "left": "litrow", "down": (32, 30, 40)},
             "exo_guard": {"*": (78, 76, 92), "front": "plate"},
             "exo_plate": {"*": (64, 62, 78), "front": "plate", "up": (84, 82, 98), "down": (44, 42, 54)},
             "exo_chest": {"*": (64, 62, 78), "front": "chest", "up": (84, 82, 98), "down": (44, 42, 54)}}


def props_layout():
    pk = Packer(0, 0, 128, PROPS_H)
    groups = []
    for prop, (arm, boxes) in PROP_BOXES.items():
        out = []
        for name, lo, hi, key in boxes:
            frm, size = hand_box(lo, hi)
            out.append({"name": name, "from": frm, "size": size, "uv": list(pk.place(size)), "paint": key})
        groups.append({"name": prop, "parent": arm, "pivot": list(FIST[arm]), "prop": PROPS[prop], "level": prop in ("mug", "tray"), "boxes": out})
    out = []
    for name, (x0, y0, z0), (x1, y1, z1), key in CART_BOXES:
        frm, size = mc_box((x0, y0, z0 + CART_FORWARD), (x1, y1, z1 + CART_FORWARD))
        out.append({"name": name, "from": frm, "size": size, "uv": list(pk.place(size)), "paint": key})
    groups.append({"name": "cart", "parent": "root", "pivot": [0, 0, 0], "prop": PROPS["cart"], "level": False, "boxes": out})
    out = []
    for name, lo, hi, key in BLANKET_BOXES:
        frm, size = mc_box(lo, hi)
        out.append({"name": name, "from": frm, "size": size, "uv": list(pk.place(size)), "paint": key})
    groups.append({"name": "blanket", "parent": "root", "pivot": [0, 0, 0], "prop": PROPS["blanket"], "level": False, "boxes": out})
    for group, parent, boxes in EXO_GROUPS:
        out = [{"name": n, "from": list(f), "size": list(sz), "uv": list(pk.place(sz)), "paint": k} for n, f, sz, k in boxes]
        groups.append({"name": group, "parent": parent, "pivot": [0, 0, 0], "bit": EXO, "prop": 0, "level": False, "boxes": out}
                      | ({"standing": True} if group == "exo_back" else {}))
    # the rifle: just where it hangs (the NT-7 item is drawn there, client/TowerCrewRenderer)
    groups.append({"name": "rifle", "parent": "right_arm", "pivot": list(RIFLE_AT), "scale": RIFLE_SCALE, "prop": PROPS["rifle"], "level": False,
                   "boxes": []})
    # the baton: just where it is held and where it hangs (the item is drawn there, client/TowerCrewRenderer.BatonLayer)
    groups.append({"name": "baton", "parent": "right_arm", "pivot": list(BATON_AT), "scale": 1.0, "prop": PROPS["baton"], "level": False,
                   "boxes": []})
    out = [{"name": n, "from": list(f), "size": [round(h - l, 4) for l, h in zip(f, t)], "uv": list(pk.place([h - l for l, h in zip(f, t)])), "paint": k}
           for n, f, t, k in HOLSTER]
    groups.append({"name": "holster", "parent": "right_leg", "pivot": [0, 0, 0], "bit": BATON, "prop": 0, "level": False, "boxes": out})
    groups.append({"name": "baton_holstered", "parent": "right_leg", "pivot": list(HOLSTERED_AT), "scale": 1.0, "bit": BATON,
                   "holstered": True, "prop": 0, "level": False, "boxes": []})
    out = []
    for name, lo, hi, key in HELMET_BOXES:
        frm, size = mc_box(lo, hi)
        out.append({"name": name, "from": frm, "size": size, "uv": list(pk.place(size)), "paint": key})
    groups.append({"name": "helmet", "parent": "head", "pivot": [0, 0, 0], "bit": HELMET, "prop": 0, "level": False, "boxes": out})
    for name, pivot, (w, h), scale, face in DECALS:
        box = {"name": name, "from": [-w / 2, -h / 2, 0.0], "size": [w, h, 0], "uv": list(pk.place((w, h, 0))), "paint": "decal:" + name}
        groups.append({"name": name, "parent": "body", "pivot": list(pivot), "scale": scale, "prop": 0, "hide": EXO, "level": False,
                       "boxes": [box]})
    for group, art, pivot, scale in EXO_DECALS:
        w, h = next(d[2] for d in DECALS if d[0] == art)
        box = {"name": group, "from": [-w / 2, -h / 2, 0.0], "size": [w, h, 0], "uv": list(pk.place((w, h, 0))), "paint": "decal:" + art}
        groups.append({"name": group, "parent": "body", "pivot": list(pivot), "scale": scale, "bit": EXO, "prop": 0, "level": False,
                       "boxes": [box]} | ({"standing": True} if group == "exo_wordmark" else {}))
    return groups


def paint_props(groups):
    sk = Skin(width=128, height=PROPS_H, seed=99)
    for g in groups:
        for b in g["boxes"]:
            if b["paint"].startswith("decal:"):
                name = b["paint"][6:]
                face = next(d[4] for d in DECALS if d[0] == name)
                rgb, a, glow = decal_art(name)
                x, y, ww, hh = rects(*b["uv"], *b["size"])[face]
                sl = (slice(int(y), int(y) + rgb.shape[0]), slice(int(x), int(x) + rgb.shape[1]))
                sk.rgb[sl] = rgb; sk.alpha[sl] = a; sk.grain[sl] = 0
                sk.glow[sl] = glow.any(-1) & (a > 0); sk.glow_rgb[sl] = glow
                continue
            key = b["paint"]
            spec = dict(PROP_PAINT.get(key) or EXO_PAINT.get(key) or HELMET_PAINT.get(key) or BATON_PAINT[key])
            if isinstance(spec["*"], str):                  # a special "*": every face not named gets it
                star = spec.pop("*")
                spec = {f: spec.get(f, star) for f in FACES} | {"*": (40, 38, 50)}
            special = {f: v for f, v in spec.items() if isinstance(v, str)}
            sk.paint_box(*b["uv"], b["size"], {f: v for f, v in spec.items() if not isinstance(v, str)} | {f: spec["*"] for f in special})
            u, v = b["uv"]
            w, h, d = b["size"]
            for f, kind in special.items():
                x, y, ww, hh = rects(u, v, w, h, d)[f]
                x0, y0, x1, y1 = int(x), int(y), int(math.ceil(x + ww)), int(math.ceil(y + hh))
                if kind == "screen":
                    sk.rgb[y0 + 1:y1 - 1, x0 + 1:x1 - 1] = (64, 146, 206)
                    sk.rgb[y0 + 1, x0 + 1:x1 - 1] = AMBER
                    for yy in range(y0 + 3, y1 - 1, 2):
                        sk.rgb[yy, x0 + 1:x1 - 2] = tone((64, 146, 206), 1.45)
                    sk.grain[y0:y1, x0:x1] = 0
                elif kind == "knit":
                    # a wool blanket's ribs across it, a satin band at the foot
                    base = np.asarray((46, 62, 96), np.float32)
                    for yy in range(y0, y1):
                        sk.rgb[yy, x0:x1] = base * (0.90 if (yy - y0) % 2 else 1.0)
                    sk.rgb[y1 - 2:y1, x0:x1] = (64, 80, 116)
                    sk.grain[y0:y1, x0:x1] = 0.05
                elif kind == "ring":                                    # a lit actuator: a glowing ring, a dark hub
                    sk.rgb[y0:y1, x0:x1] = (74, 36, 124)
                    sk.glow[y0:y1, x0:x1] = True
                    sk.glow_rgb[y0:y1, x0:x1] = NL
                    if x1 - x0 > 2 and y1 - y0 > 2:
                        sk.rgb[y0 + 1:y1 - 1, x0 + 1:x1 - 1] = (24, 22, 30)
                        sk.glow[y0 + 1:y1 - 1, x0 + 1:x1 - 1] = False
                    sk.grain[y0:y1, x0:x1] = 0
                elif kind == "vents":                                   # the power pack's vents, lit from inside
                    for yy in range(y0 + 1, y1 - 1, 2):
                        sk.rgb[yy, x0 + 1:x1 - 1] = (74, 36, 124)
                        sk.glow[yy, x0 + 1:x1 - 1] = True
                        sk.glow_rgb[yy, x0 + 1:x1 - 1] = NL
                elif kind == "core":                                    # the power pack's back: a lit core, vents under
                    cx, cy = (x0 + x1) // 2, y0 + max(2, (y1 - y0) // 2)        # (below the print on the tall pack)
                    sk.rgb[cy - 1:cy + 1, cx - 2:cx + 2] = (74, 36, 124)
                    sk.glow[cy - 1:cy + 1, cx - 2:cx + 2] = True
                    sk.glow_rgb[cy - 1:cy + 1, cx - 2:cx + 2] = (140, 80, 190)
                    sk.rgb[cy - 2, cx - 2:cx + 2] = (40, 38, 50); sk.rgb[cy + 1, cx - 2:cx + 2] = (40, 38, 50)
                    sk.rgb[cy - 1:cy + 1, cx - 3] = (40, 38, 50); sk.rgb[cy - 1:cy + 1, cx + 2] = (40, 38, 50)
                    for yy in range(cy + 3, y1 - 1, 2):
                        sk.rgb[yy, x0 + 1:x1 - 1] = (74, 36, 124)
                        sk.glow[yy, x0 + 1:x1 - 1] = True
                        sk.glow_rgb[yy, x0 + 1:x1 - 1] = NL
                    sk.rgb[y0:y1, x0] = (50, 48, 62); sk.rgb[y0:y1, x1 - 1] = (50, 48, 62)
                elif kind.startswith("glow_only:"):                    # plasma: added light only (alpha 0 on the texture)
                    sk.alpha[y0:y1, x0:x1] = 0
                    core = kind.endswith("core")
                    sk.glow[y0:y1, x0:x1] = True
                    sk.glow_rgb[y0:y1, x0:x1] = (66, 40, 110) if core else (44, 16, 84)
                    if not core:                                        # the sheath's edges fainter: a soft envelope
                        sk.glow_rgb[y0:y1, x0] = (26, 8, 54); sk.glow_rgb[y0:y1, x1 - 1] = (26, 8, 54)
                    sk.glow_force = getattr(sk, "glow_force", np.zeros_like(sk.glow))
                    sk.glow_force[y0:y1, x0:x1] = True
                elif kind == "bt_ribs":                                 # the grip's rubber rings, across it
                    for yy in range(y0, y1, 2):
                        sk.rgb[yy, x0:x1] = (44, 40, 52)
                elif kind == "bt_plasma":                               # the plasma rod: a white core along it in purple light
                    sk.rgb[y0:y1, x0:x1] = (150, 80, 230)
                    sk.glow[y0:y1, x0:x1] = True
                    sk.glow_rgb[y0:y1, x0:x1] = (130, 70, 200)
                    if x1 - x0 >= 2:
                        cx = (x0 + x1) // 2
                        sk.rgb[y0:y1, cx - 1:cx + 1] = (236, 220, 255)
                        sk.glow_rgb[y0:y1, cx - 1:cx + 1] = (170, 140, 220)
                    sk.grain[y0:y1, x0:x1] = 0
                elif kind == "hs_side":                                 # the sheath's side: a lit stripe down it
                    sk.rgb[y0:y1, (x0 + x1) // 2] = (74, 36, 124)
                    sk.glow[y0:y1, (x0 + x1) // 2] = True
                    sk.glow_rgb[y0:y1, (x0 + x1) // 2] = NL
                elif kind == "glass":                                   # the visor: lit glass, a brighter line along its top
                    sk.rgb[y0:y1, x0:x1] = (52, 24, 92)
                    sk.glow[y0:y1, x0:x1] = True
                    sk.glow_rgb[y0:y1, x0:x1] = (70, 28, 120)
                    sk.glow_rgb[y0, x0:x1] = (110, 60, 160)
                    sk.grain[y0:y1, x0:x1] = 0
                elif kind == "lit":
                    sk.rgb[y0:y1, x0:x1] = (74, 36, 124)
                    sk.glow[y0:y1, x0:x1] = True
                    sk.glow_rgb[y0:y1, x0:x1] = NL
                elif kind == "stripe":                                  # the shell's back: a purple stripe down the middle
                    cx = (x0 + x1) // 2
                    sk.rgb[y0:y1, cx - 1:cx + 1] = (74, 36, 124)
                    sk.rgb[y0 + (y1 - y0) * 2 // 3, x0:x1] = (30, 28, 38)
                elif kind == "ribs":                                    # the gorget's rings
                    for yy in range(y0, y1, 2):
                        sk.rgb[yy, x0:x1] = (60, 58, 72)
                elif kind == "grille":                                  # the respirator: dark slots, a lit dot each side
                    for yy in range(y0 + 1, y1, 2):
                        sk.rgb[yy, x0 + 1:x1 - 1] = (18, 16, 24)
                    for xx in (x0, x1 - 1):
                        sk.rgb[y0 + 1, xx] = (74, 36, 124); sk.glow[y0 + 1, xx] = True; sk.glow_rgb[y0 + 1, xx] = NL
                elif kind == "litrow":                                  # a band's lit line along its foot
                    sk.rgb[y1 - 1, x0:x1] = (74, 36, 124)
                    sk.glow[y1 - 1, x0:x1] = True
                    sk.glow_rgb[y1 - 1, x0:x1] = NL
                elif kind == "chest":                                   # the chest plate: a lit vent pair on the right breast
                    sk.rgb[y0, x0:x1] = (112, 110, 126)
                    sk.rgb[y0:y1, x0 + (x1 - x0) // 2] = (40, 38, 50)
                    for yy in (y0 + 2, y0 + 4):
                        sk.rgb[yy, x0 + 1:x0 + 3] = (74, 36, 124)
                        sk.glow[yy, x0 + 1:x0 + 3] = True
                        sk.glow_rgb[yy, x0 + 1:x0 + 3] = NL
                    sk.rgb[y1 - 1, x0:x1] = (74, 36, 124)
                    sk.glow[y1 - 1, x0:x1] = True
                    sk.glow_rgb[y1 - 1, x0:x1] = NL
                elif kind == "plate":                                   # a knee guard: a lighter edge, a dark centre line
                    sk.rgb[y0, x0:x1] = (112, 110, 126)
                    sk.rgb[y0:y1, x0 + (x1 - x0) // 2] = (40, 38, 50)
                elif kind == "paper":
                    sk.rgb[y0 + 1:y1, x0 + 1:x1 - 1] = (236, 236, 230)
                    for yy in range(y0 + 2, y1 - 1, 2):
                        sk.rgb[yy, x0 + 1:x1 - 2] = (150, 150, 156)
                    sk.rgb[y0, x0 + (x1 - x0) // 2 - 1:x0 + (x1 - x0) // 2 + 1] = (180, 180, 176)
                    sk.grain[y0:y1, x0:x1] = 0.01
    return sk.image(), sk.glow_image()


# ------------------------------------------------------------------ the hiring pool

# Each job's pool: (hair, style, eye, beard). The janitors are women (slim arms, long hair); everyone else is a man.
POOL = {
    "flight_director": [(0, "crop", 0, False), (5, "short", 1, True), (2, "crop", 2, False)],
    "controller": [(2, "short", 1, False), (1, "crop", 0, False), (6, "short", 3, False), (4, "short", 2, True),
                   (3, "crop", 1, False), (0, "short", 0, False), (5, "crop", 2, True), (1, "short", 3, False)],
    # security: eight posts since the detail outside and on the roof (Justin, 6 November 2026: "add in a couple more
    # security detail"), so nine faces; fourteen since the crew list (C2, 9 November 2026), so eighteen
    "security": [(0, "short", 1, True), (4, "crop", 0, False), (2, "short", 2, False), (1, "bald", 1, True), (3, "crop", 3, False),
                 (5, "short", 0, False), (6, "crop", 2, True), (0, "bald", 3, False), (2, "crop", 1, True),
                 (1, "short", 0, False), (3, "short", 2, True), (4, "bald", 1, False), (5, "crop", 3, False), (6, "short", 1, False),
                 (2, "bald", 0, True), (0, "crop", 2, False), (4, "short", 3, True), (1, "crop", 2, False)],
    "gate_guard": [(0, "crop", 1, False), (2, "short", 0, True), (6, "crop", 2, False)],
    "mechanic": [(1, "short", 0, False), (0, "crop", 1, True), (4, "short", 3, False), (2, "bald", 2, True), (3, "short", 1, False)],
    "tech": [(1, "short", 1, False), (6, "crop", 0, False), (0, "short", 2, True), (5, "short", 3, False), (2, "crop", 1, False)],
    "cook": [(0, "crop", 1, False), (2, "short", 2, True)],
    "server": [(4, "short", 0, False), (3, "crop", 1, False), (1, "short", 3, False)],
    "janitor": [(5, "bun", 1, False), (3, "ponytail", 2, False), (0, "bob", 0, False), (6, "bun", 3, False)],
    # the new jobs (Justin, 5 November 2026: "add in an additional 10 people, spread them between jobs appropriately"):
    # pilots 4, engineers 4, a medic and the manager, each pool a face or two more than its posts
    "pilot": [(1, "crop", 1, False), (0, "short", 2, False), (3, "crop", 0, True), (2, "short", 3, False), (4, "crop", 1, False),
              (6, "short", 0, False)],
    "engineer": [(2, "short", 0, False), (0, "crop", 1, True), (5, "bald", 2, True), (1, "short", 3, False), (4, "short", 1, False),
                 (3, "crop", 2, False)],
    "medic": [(1, "short", 2, False), (0, "crop", 0, False)],
    "manager": [(5, "short", 1, True), (5, "crop", 2, False)],
}
GLASSES = {("engineer", 1), ("engineer", 2), ("engineer", 4), ("manager", 0)}    # who wears glasses (not safety glasses)
WOMEN = {"janitor"}
NAMES_MEN = ["Adam", "Ben", "Callum", "Daniel", "Ethan", "Finn", "George", "Harry", "Isaac", "Jack", "Kyle", "Liam", "Mason",
             "Nathan", "Oliver", "Owen", "Peter", "Ryan", "Sam", "Thomas", "Tyler", "Will", "Lewis", "Jamie"]
NAMES_WOMEN = ["Amy", "Chloe", "Emma", "Grace", "Hannah", "Lucy", "Megan", "Rachel", "Sophie", "Zoe"]
SURNAMES = ["Archer", "Bennett", "Carter", "Dawson", "Ellis", "Fletcher", "Grant", "Hayes", "Irving", "Jensen", "Keller",
            "Lawson", "Mercer", "Nolan", "Parker", "Quinn", "Reed", "Shaw", "Turner", "Walsh", "Young", "Hart", "Brooks"]


def skin_name(job, i):
    return f"{job}_{i + 1}"


def paint_skin(job, i, gear_groups):
    hair, style, eye, beard = POOL[job][i]
    slim = job in WOMEN
    seed = 1000 + sum(map(ord, job)) * 7 + i * 31
    st = SKINS[(i * 5 + len(job)) % len(SKINS)]
    hc = HAIRS[hair]
    sk = Skin(slim, seed)
    base = {"bun": "long", "ponytail": "long"}.get(style, style)
    head(sk, st, hc, base, eye, beard, seed, volume=job != "mechanic")
    gear, cap = uniform(sk, job, st)
    if (job, i) in GLASSES:
        glasses_skin(sk, frame=(30, 30, 34), lens=(170, 196, 214))
    gear |= BUN if style == "bun" else PONYTAIL if style == "ponytail" else 0
    colours = gear_colours(cap, hc, st)
    for g in gear_groups:
        for b in g["boxes"]:
            spec = colours[b["paint"]]
            sk.paint_box(*b["uv"], b["size"], {f: (spec["*"] if isinstance(c, str) else c) for f, c in spec.items()})
            for f in GEAR_GLOW.get(b["paint"], ()):
                x, y, ww, hh = rects(*b["uv"], *b["size"])[f]
                sl = (slice(int(y), max(int(y) + 1, int(math.ceil(y + hh)))), slice(int(x), max(int(x) + 1, int(math.ceil(x + ww)))))
                sk.glow[sl] = True
                sk.glow_rgb[sl] = NL
    return sk.image(), sk.glow_image(), {"skin": skin_name(job, i), "gear": gear, "slim": slim}


def build():
    gear_groups = gear_layout()
    prop_groups = props_layout()
    skins, pool = {}, {}
    for job in POOL:
        pool[job] = []
        for i in range(len(POOL[job])):
            img, glow, entry = paint_skin(job, i, gear_groups)
            skins[entry["skin"]] = (img, glow)
            pool[job].append(entry)
    data = {"jobs": pool, "names": {"men": NAMES_MEN, "women": NAMES_WOMEN, "surnames": SURNAMES}, "women": sorted(WOMEN)}
    strip = lambda gs: [{k: v for k, v in g.items()} | {"boxes": [{k: v for k, v in b.items() if k != "paint"} for b in g["boxes"]]} for g in gs]
    return skins, paint_props(prop_groups), {"texture": [128, 64], "groups": strip(gear_groups)}, \
        {"texture": [128, PROPS_H], "groups": strip(prop_groups)}, data


def check(gear_json, props_json):
    """No two boxes of one texture share texels, and every box lies inside its texture."""
    for name, js in (("gear", gear_json), ("props", props_json)):
        H = js["texture"][1]
        taken = np.zeros((H, 128), bool)
        for g in js["groups"]:
            for b in g["boxes"]:
                w, h, d = b["size"]
                u, v = b["uv"]
                uw, uh = 2 * math.ceil(d) + 2 * math.ceil(w), math.ceil(d) + math.ceil(h)
                assert u + uw <= 128 and v + uh <= H, (name, g["name"], b["name"])
                if name == "gear":
                    assert u >= 64, ("gear must keep off the skin half", b["name"])
                assert not taken[v:v + uh, u:u + uw].any(), (name, b["name"])
                taken[v:v + uh, u:u + uw] = True


def write_all():
    skins, props, gear_json, props_json, pool = build()
    check(gear_json, props_json)
    SKIN_DIR.mkdir(parents=True, exist_ok=True)
    for old in SKIN_DIR.glob("*.png"):
        if old.stem.removesuffix("_glow") not in skins:
            old.unlink()
    for name, (img, glow) in skins.items():
        img.save(SKIN_DIR / f"{name}.png", optimize=True)
        glow.save(SKIN_DIR / f"{name}_glow.png", optimize=True)        # the glow layer (client/TowerCrewRenderer)
    props[0].save(PROPS_PNG, optimize=True)
    props[1].save(PROPS_GLOW_PNG, optimize=True)
    GEAR_JSON.parent.mkdir(parents=True, exist_ok=True)
    GEAR_JSON.write_text(json.dumps(gear_json, indent=1) + "\n")
    PROPS_JSON.write_text(json.dumps(props_json, indent=1) + "\n")
    POOL_JSON.write_text(json.dumps(pool, indent=1) + "\n")
    return len(skins)


if __name__ == "__main__":
    print(f"{write_all()} crew skins")
