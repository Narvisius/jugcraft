"""The control building's plant computer room brought up to the detail standard (older rooms round 2; Justin, 15 October
2026: racks "Real 3D IT racks", working "Rack lights show traffic", "Plant computer console", "UPS and power", "Room
cooling"; fittings "Clean-agent fire system", "Overhead cable ladder", "Cooled floor and CRACs", "Signs and labels";
the admin building's server closet gets the same racks).

The racks (this module's first part) are 42U, 600 mm cabinets a block wide and two high: a black frame (posts, side
panels, a top with its cable entries, a plinth), a perforated front door you see the equipment through (hinges, a
swing handle and lock, the rack's number on a plate), a perforated rear door, the 19-inch rails, and the equipment
on them, each unit a real box with its bezel drawn on a shared sheet of faces (pi_srv_faces) and its parts standing
out of it: drive carriers, handles, plugs and patch cords, card ejectors. Their lights are live
(client/LivePanelRenderer "leds" strips: power, activity, links and status, from the room's state:
client/EdgTelemetry.java srv, nuclear/ServerRoom.java). Four racks: servers, network, storage, the plant process
computer.

Models in model pixels for the piece facing north: x across, y up, z from its back (north, z 0) to its front (south)."""
import math
import random

from PIL import Image, ImageDraw

import plant_panels as PP

FRAME, BEZEL = "pi_srv_frame", "pi_srv_bezel"
SHEET = "pi_srv_faces"
SHEET_PX = 1024
CHROME = "pi_chrome"

# ------------------------------------------------------------------------------------------------ the rack's frame
X0, X1 = 0.15, 15.85            # the cabinet's sides
ZB, ZF = 0.55, 15.45            # its back and front (the doors' faces)
TOP = 31.9                      # the top cover's top
UH = 0.7                        # one rack unit (44.45 mm at 16 px a metre and a bit)
RAIL_Y0 = 16 - 21 * UH          # U1's bottom (a unit boundary lies on the blocks' seam, y 16)
NU = 42
RAIL_Y1 = RAIL_Y0 + NU * UH
SEAM_U = 21                     # the unit boundary on the seam
EQ_X0, EQ_X1 = 1.6, 14.4        # a unit's bezel with its ears (in front of the rails, 0.2 inside their sides)
BODY_X0, BODY_X1 = 2.3, 13.7    # its chassis between the rails
FACE_Z = 14.15                  # the bezels' face
LED_Z = FACE_Z + 0.04
CARRIER_Z = FACE_Z + 0.1        # a drive carrier's face (0.1 out of its bezel: never closer, or the faces fight)
ON_PART = FACE_Z + 0.15         # a light on a latch or a button (their fronts FACE_Z + 0.12)
CARD_Z = FACE_Z + 0.15          # an I/O card's front
CORD_Z = FACE_Z + 0.3           # the patch cords dressed down over the cable managers (their fingers 0.2 out)
SHELF_Y0, SHELF_STEP, SHELF_H = 0.12, 0.4, 0.34
DOOR_Z = (15.1, 15.45)
MESH_Z = (15.2, 15.35)


def frame(open_=False):
    """The cabinet: plinth, corner posts, side panels, top cover; the front and rear doors (the front one swung open
    when open_); the rails."""
    b = [(0.5, 0.0, 0.9, 15.5, 0.6, 15.1, {"*": FRAME, "south": "pi_srv_kick!", "north": "pi_srv_kick!"})]
    for x0, x1 in ((X0, 0.95), (15.05, X1)):
        b += [(x0, 0.6, 0.9, x1, 31.4, 1.7, FRAME), (x0, 0.6, 14.3, x1, 31.4, 15.1, FRAME)]
    b += [(X0, 0.6, 1.7, 0.3, 31.4, 14.3, FRAME), (15.7, 0.6, 1.7, X1, 31.4, 14.3, FRAME)]
    # Each side panel's lift-off latch.
    b += [(X0 - 0.1, 15.0, 7.4, X0, 17.0, 8.6, "pi_srv_handle"), (X1, 15.0, 7.4, X1 + 0.1, 17.0, 8.6, "pi_srv_handle")]
    b += [(X0, 31.4, ZB, X1, TOP, ZF, {"*": FRAME, "up": "pi_srv_top!"})]
    b += [swing(d) for d in door_front()] if open_ else door_front()
    b += door_rear()
    # The 19-inch rails, front and rear, their square holes and unit marks (texture).
    for x0, x1 in ((1.4, 2.2), (13.8, 14.6)):
        b += [(x0, RAIL_Y0 - 0.1, 13.5, x1, RAIL_Y1 + 0.1, 13.9, {"*": "pi_srv_rail_edge", "south": "pi_srv_rail!"}),
              (x0, RAIL_Y0 - 0.1, 2.2, x1, RAIL_Y1 + 0.1, 2.6, "pi_srv_rail_edge")]
    # The side rails the rails hang on, front to back at the top and the bottom.
    for y0 in (RAIL_Y0 - 0.5, RAIL_Y1 + 0.1):
        for x0, x1 in ((0.95, 1.4), (14.6, 15.05)):
            b += [(x0, y0, 1.7, x1, y0 + 0.4, 14.3, FRAME)]
    return b


def door_front():
    """The perforated front door: its frame (stiles and rails), the mesh you see the equipment through, three hinges on
    the left, the swing handle and its lock on the right, the rack's number plate (its number live) on the top rail."""
    z0, z1 = DOOR_Z
    b = [(0.45, 0.8, z0, 1.25, 31.2, z1, FRAME), (14.75, 0.8, z0, 15.55, 31.2, z1, FRAME),
         (1.25, 0.8, z0, 14.75, 1.6, z1, FRAME), (1.25, 30.0, z0, 14.75, 31.2, z1, FRAME),
         (1.25, 1.6, MESH_Z[0], 14.75, 30.0, MESH_Z[1], MESH)]
    for y0 in (3.0, 14.2, 27.4):
        b += [(0.2, y0, z0 + 0.05, 0.45, y0 + 1.4, z1 - 0.05, CHROME)]
    # The swing handle: its escutcheon, the handle laid flat, the key cylinder above it.
    b += [(14.9, 13.6, z1, 15.4, 18.4, z1 + 0.1, FRAME), (14.98, 14.0, z1 + 0.1, 15.32, 17.6, z1 + 0.42, "pi_srv_handle"),
          (15.03, 17.9, z1 + 0.1, 15.27, 18.2, z1 + 0.22, CHROME)]
    b += [(5.4, 30.25, z1, 10.6, 30.95, z1 + 0.15, "pi_srv_label")]
    return b


# The perforated panels: their faces only (the frame round them hides their edges), the holes laid out a block at a time
# so they run on across the blocks' seam.
MESH = {"south": "pi_srv_mesh", "north": "pi_srv_mesh"}


HINGE = (0.45, DOOR_Z[0])          # the front door's hinge line (x, z on the piece facing north)
TURNED = {"south": "west", "north": "east", "east": "south", "west": "north", "up": "up", "down": "down"}


def swing(box):
    """A part of the front door swung open a right angle about its hinges (its outside face then faces west): x along
    the door becomes z out into the aisle, its thickness x."""
    x0, y0, z0, x1, y1, z1, t = box[:7]
    hx, hz = HINGE
    nx0, nx1 = hx - (z1 - hz), hx - (z0 - hz)
    nz0, nz1 = hz + (x0 - hx), hz + (x1 - hx)
    if isinstance(t, dict):
        t = {("*" if k == "*" else TURNED[k]): v for k, v in t.items()}
    return (round(nx0, 4), y0, round(nz0, 4), round(nx1, 4), y1, round(nz1, 4), t) + tuple(box[7:])


def door_rear():
    """The rear door, perforated, in two leaves (their frames, their handles)."""
    z0, z1 = ZB, ZB + 0.35
    b = []
    for x0, x1 in ((0.45, 7.95), (8.05, 15.55)):
        b += [(x0, 0.8, z0, x0 + 0.6, 31.2, z1, FRAME), (x1 - 0.6, 0.8, z0, x1, 31.2, z1, FRAME),
              (x0 + 0.6, 0.8, z0, x1 - 0.6, 1.6, z1, FRAME), (x0 + 0.6, 30.0, z0, x1 - 0.6, 31.2, z1, FRAME),
              (x0 + 0.6, 1.6, z0 + 0.1, x1 - 0.6, 30.0, z1 - 0.1, MESH)]
    for x in (7.5, 8.5):
        b += [(x - 0.15, 14.2, z0 - 0.3, x + 0.15, 17.8, z0, "pi_srv_handle")]
    return b


# ------------------------------------------------------------------------------------------------ the equipment
# Each kind: its height in units, the drawing of its face, what stands out of it, its lights. The faces share one
# sheet: a slot each, a column of units at SLOT_PX a model pixel.
SLOT_PX = 39                   # the sheet's pixels a model pixel (a face 13.1 px wide: 511 px, two columns of a 1024 sheet)
FACE_W = EQ_X1 - EQ_X0


class Unit:
    def __init__(self, kind, u, draw, parts=None, leds=None, texts=None):
        self.kind, self.u, self.draw, self.parts, self.leds, self.texts = kind, u, draw, parts, leds, texts


UNITS = {}
SLOTS = {}


def unit(kind, u):
    def wrap(fn):
        UNITS[kind] = Unit(kind, u, fn)
        return fn
    return wrap


def parts_of(kind):
    def wrap(fn):
        UNITS[kind].parts = fn
        return fn
    return wrap


def leds_of(kind):
    def wrap(fn):
        UNITS[kind].leds = fn
        return fn
    return wrap


def texts_of(kind):
    def wrap(fn):
        UNITS[kind].texts = fn
        return fn
    return wrap


# Colours of the faces.
BLACK = (24, 25, 28)
DARK = (38, 40, 44)
STEEL = (70, 73, 78)
SILVER = (150, 154, 160)
PORT = (12, 12, 14)
WHITE = (232, 232, 228)
LABEL = (236, 234, 220)


class Face:
    """A face's drawing at SLOT_PX a model pixel, in model pixels across the bezel from its left (0) and up from its
    bottom (0)."""

    def __init__(self, img, ox, oy, h_px):
        self.d = ImageDraw.Draw(img)
        self.ox, self.oy, self.h = ox, oy, h_px

    def px(self, x, y):
        return self.ox + x * SLOT_PX, self.oy + self.h - y * SLOT_PX

    def R(self, x0, y0, x1, y1, fill, outline=None, width=1):
        a, b_ = self.px(x0, y1)
        c, d = self.px(x1, y0)
        self.d.rectangle([a, b_, c - 1, d - 1], fill=fill, outline=outline, width=width)

    def T(self, x, y, s, size, fill=WHITE, anchor="mm", bold=True):
        # size: the capitals' height in model pixels.
        self.d.text(self.px(x, y), s, font=PP.rfont(size * SLOT_PX * 1.45, bold), fill=fill, anchor=anchor)

    def vents(self, x0, y0, x1, y1, step=0.22, fill=PORT):
        x = x0
        while x + step * 0.55 <= x1:
            self.R(x, y0, x + step * 0.55, y1, fill)
            x += step

    def holes(self, x0, y0, x1, y1, r=0.07, step=0.2, fill=PORT):
        y = y0 + r
        k = 0
        while y + r <= y1:
            x = x0 + r + (step / 2 if k % 2 else 0)
            while x + r <= x1:
                cx, cy = self.px(x, y)
                rr = r * SLOT_PX
                self.d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=fill)
                x += step
            y += step * 0.87
            k += 1

    def ears(self, h, fill=BLACK):
        """The mounting ears at each end with their two screws."""
        for x0 in (0.0, FACE_W - 0.85):
            self.R(x0, 0, x0 + 0.85, h, fill)
            for y in ([h / 2] if h < 1 else [h * 0.25, h * 0.75]):
                cx, cy = self.px(x0 + 0.42, y)
                rr = 0.12 * SLOT_PX
                self.d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=SILVER)
                self.d.line([cx - rr * 0.7, cy, cx + rr * 0.7, cy], fill=DARK, width=3)


def _port(f, x, y, w=0.36, h=0.3):
    f.R(x - w / 2, y - h / 2, x + w / 2, y + h / 2, (90, 92, 96))
    f.R(x - w / 2 + 0.04, y - h / 2 + 0.04, x + w / 2 - 0.04, y + h / 2 - 0.06, PORT)


# Where on a 48-port switch the ports are (pairs of rows), in model pixels across its face.
SW_X = [2.4 + (k // 6) * 2.05 + (k % 6) * 0.32 for k in range(24)]
PATCH_X = [1.6 + (k // 6) * 2.6 + (k % 6) * 0.4 for k in range(24)]


@unit("blank", 1)
def _blank(f, h):
    f.R(0, 0, FACE_W, h, BLACK)
    f.R(0.9, 0.08, FACE_W - 0.9, h - 0.08, (30, 31, 34))
    f.ears(h)


@unit("blank2", 2)
def _blank2(f, h):
    f.R(0, 0, FACE_W, h, BLACK)
    f.R(0.9, 0.1, FACE_W - 0.9, h - 0.1, (30, 31, 34))
    f.vents(1.4, 0.25, FACE_W - 1.4, h - 0.25, 0.3, (14, 14, 16))
    f.ears(h)


@unit("patch", 1)
def _patch(f, h):
    f.R(0, 0, FACE_W, h, BLACK)
    f.ears(h)
    for k, x in enumerate(PATCH_X):
        _port(f, x, h * 0.42, 0.3, 0.28)
        f.T(x, h * 0.85, str(k + 1), 0.11, WHITE, bold=False)
    f.R(0.95, 0.05, 1.45, h - 0.05, LABEL)


@unit("cablemgr", 1)
def _cablemgr(f, h):
    f.R(0, 0, FACE_W, h, BLACK)
    f.R(1.0, 0.06, FACE_W - 1.0, h - 0.06, (16, 16, 18))
    f.ears(h)


@unit("switch", 1)
def _switch(f, h):
    f.R(0, 0, FACE_W, h, DARK)
    f.ears(h, BLACK)
    for k, x in enumerate(SW_X):
        _port(f, x, h * 0.72, 0.28, 0.24)
        _port(f, x, h * 0.3, 0.28, 0.24)
    for k in range(4):
        x = 10.8 + k * 0.42
        f.R(x - 0.17, h * 0.25, x + 0.17, h * 0.75, (120, 124, 130))
        f.R(x - 0.12, h * 0.32, x + 0.12, h * 0.68, PORT)
    f.T(1.6, h * 0.55, "SW", 0.2, SILVER)


@unit("server1", 1)
def _server1(f, h):
    f.R(0, 0, FACE_W, h, DARK)
    f.ears(h, BLACK)
    for k in range(10):
        x0 = 1.05 + k * 0.82
        f.R(x0, 0.06, x0 + 0.76, h - 0.06, (52, 54, 58), outline=(20, 20, 22), width=2)
        f.R(x0 + 0.05, 0.12, x0 + 0.5, h - 0.12, (30, 31, 34))
    f.vents(9.4, 0.12, 11.25, h - 0.12, 0.16)
    f.R(11.75, 0.1, 12.2, h - 0.1, (16, 16, 18))


@unit("server2", 2)
def _server2(f, h):
    f.R(0, 0, FACE_W, h, DARK)
    f.ears(h, BLACK)
    # The control panel at the left: the asset tag, the power button, the ID light.
    f.R(1.0, 0.1, 2.3, h - 0.1, (30, 31, 34))
    f.R(1.1, h - 0.45, 2.2, h - 0.15, LABEL)
    f.T(1.65, h - 0.3, "PPC-SRV", 0.14, (20, 20, 22))
    # Four 3.5-inch bays a row, two rows (the carriers stand out of them: parts).
    for r in range(2):
        for k in range(4):
            x0 = 2.5 + k * 2.1
            y0 = 0.1 + r * 0.62
            f.R(x0, y0, x0 + 2.0, y0 + 0.58, PORT)
    f.vents(10.95, 0.12, 12.1, h - 0.12, 0.18)


@unit("kvm", 1)
def _kvm(f, h):
    f.R(0, 0, FACE_W, h, BLACK)
    f.ears(h)
    f.R(1.2, 0.1, FACE_W - 1.2, h - 0.1, (34, 35, 38))
    f.T(FACE_W / 2, h * 0.5, "KVM CONSOLE", 0.22, SILVER)


@unit("shelf", 2)
def _shelf(f, h):
    f.R(0, 0, FACE_W, h, DARK)
    f.ears(h, (32, 34, 38))
    for r in range(3):
        for k in range(4):
            x0 = 1.0 + k * 2.78
            y0 = SHELF_Y0 + r * SHELF_STEP - 0.03
            f.R(x0, y0, x0 + 2.7, y0 + SHELF_H + 0.06, PORT)


@unit("ctrl", 2)
def _ctrl(f, h):
    f.R(0, 0, FACE_W, h, (46, 48, 54))
    f.ears(h, (32, 34, 38))
    f.R(1.2, 0.15, FACE_W - 1.2, h - 0.15, (58, 60, 66))
    f.vents(1.4, 0.3, 7.0, h - 0.3, 0.26, (24, 24, 28))
    f.R(7.6, 0.45, 10.6, 1.0, (20, 40, 30))
    f.T(9.1, 0.72, "SAN CTRL A", 0.2, (120, 230, 150))
    f.T(9.1, 1.2, "STORAGE", 0.18, SILVER)


@unit("fcsw", 1)
def _fcsw(f, h):
    f.R(0, 0, FACE_W, h, (40, 44, 60))
    f.ears(h, BLACK)
    for k in range(24):
        x = 2.0 + (k // 4) * 1.65 + (k % 4) * 0.36
        f.R(x - 0.15, h * 0.22, x + 0.15, h * 0.7, (120, 124, 130))
        f.R(x - 0.1, h * 0.3, x + 0.1, h * 0.62, PORT)
    f.T(1.5, h * 0.5, "FC", 0.22, SILVER)


@unit("gps", 1)
def _gps(f, h):
    f.R(0, 0, FACE_W, h, BLACK)
    f.ears(h)
    f.T(2.4, h * 0.55, "TIME SERVER", 0.2, SILVER)
    f.R(4.6, 0.1, 9.2, h - 0.1, (24, 6, 6))
    f.T(10.6, h * 0.55, "GPS", 0.2, SILVER)


@unit("firewall", 1)
def _firewall(f, h):
    f.R(0, 0, FACE_W, h, (120, 30, 28))
    f.ears(h, BLACK)
    for k in range(8):
        _port(f, 6.0 + k * 0.42, h * 0.5, 0.3, 0.3)
    f.R(1.3, 0.12, 4.4, h - 0.12, (20, 30, 24))
    f.T(2.85, h * 0.5, "FIREWALL", 0.2, (120, 230, 150))


@unit("fiber", 2)
def _fiber(f, h):
    f.R(0, 0, FACE_W, h, BLACK)
    f.ears(h)
    f.R(1.2, 0.15, FACE_W - 1.2, h - 0.15, (60, 66, 74))
    f.R(1.4, 0.3, FACE_W - 1.4, h - 0.3, (34, 44, 52))
    for k in range(12):
        x = 1.8 + k * 0.82
        f.R(x, 0.4, x + 0.6, h - 0.4, (240, 200, 40))
        f.R(x + 0.1, 0.55, x + 0.5, h - 0.55, (60, 140, 220))
    f.T(FACE_W / 2, h - 0.22, "FIBER ENCLOSURE", 0.16, SILVER)


@unit("io", 4)
def _io(f, h):
    f.R(0, 0, FACE_W, h, (176, 180, 182))
    f.ears(h, (150, 154, 158))
    f.R(1.0, 0.2, FACE_W - 1.0, h - 0.5, (40, 42, 46))
    f.T(FACE_W / 2, h - 0.25, "PLANT DATA ACQUISITION  I/O CHASSIS", 0.2, (24, 24, 26))
    for k in range(IO_CARDS):
        x0 = IO_X0 + k * IO_STEP
        f.R(x0, 0.3, x0 + IO_STEP - 0.06, h - 0.6, (120, 124, 128))
        f.R(x0 + 0.06, 0.6, x0 + IO_STEP - 0.12, h - 0.9, (196, 198, 194))
        f.T(x0 + IO_STEP / 2 - 0.03, h - 1.15, IO_LABELS[k % len(IO_LABELS)], 0.12, (24, 24, 26))
        f.T(x0 + IO_STEP / 2 - 0.03, 0.95, f"{k + 1:02d}", 0.13, (24, 24, 26))


IO_CARDS = 14
IO_X0 = 1.1
IO_STEP = (FACE_W - 2.2) / IO_CARDS
IO_LABELS = ["AI 16", "AI 16", "AI 16", "TC 16", "RTD 8", "DI 32", "DI 32", "DO 16", "AI 16", "PULSE", "DI 32", "AO 8", "CPU", "PSU"]


@unit("ppc", 2)
def _ppc(f, h):
    f.R(0, 0, FACE_W, h, (180, 184, 186))
    f.ears(h, (150, 154, 158))
    f.R(1.0, 0.12, 4.6, h - 0.12, (40, 42, 46))
    f.R(1.15, 0.7, 4.45, 1.25, (16, 30, 22))
    f.T(2.8, 0.42, "PLANT PROCESS COMPUTER", 0.12, SILVER)
    for r in range(2):
        for k in range(4):
            x0 = 4.9 + k * 1.75
            y0 = 0.12 + r * 0.6
            f.R(x0, y0, x0 + 1.65, y0 + 0.55, PORT)
    f.vents(12.0, 0.15, 12.25, h - 0.15, 0.2)


@unit("core", 7)
def _core(f, h):
    f.R(0, 0, FACE_W, h, (30, 32, 36))
    f.ears(h, BLACK)
    f.T(FACE_W / 2, h - 0.25, "CORE SWITCH", 0.2, SILVER)
    for k in range(CORE_CARDS):
        y0 = 0.25 + k * CORE_STEP
        f.R(1.0, y0, FACE_W - 1.0, y0 + CORE_STEP - 0.08, (44, 46, 52))
        if k in (2, 3):
            f.T(2.2, y0 + CORE_STEP / 2, "SUP " + "AB"[k - 2], 0.18, SILVER)
            f.R(4.0, y0 + 0.15, 7.0, y0 + CORE_STEP - 0.25, (16, 16, 18))
        else:
            for p in range(24):
                x = 2.2 + (p // 6) * 2.4 + (p % 6) * 0.34
                _port(f, x, y0 + CORE_STEP / 2 - 0.04, 0.28, 0.26)


CORE_CARDS = 6
CORE_STEP = (7 * UH - 0.6) / CORE_CARDS


def sheet():
    """Every kind's face on the shared sheet (two columns), and where each lies (SLOTS: kind -> uv on the sheet)."""
    img = Image.new("RGBA", (SHEET_PX, SHEET_PX), (20, 20, 22, 255))
    col_w = SHEET_PX // 2
    x, y = 0, 0
    for kind, un in UNITS.items():
        h_px = round(un.u * UH * SLOT_PX)
        if y + h_px > SHEET_PX:
            x, y = col_w, 0
        f = Face(img, x + 1, y, h_px)
        un.draw(f, un.u * UH)
        w_px = FACE_W * SLOT_PX
        k = 16 / SHEET_PX
        SLOTS[kind] = [round((x + 1) * k, 4), round(y * k, 4), round((x + 1 + w_px) * k, 4), round((y + h_px) * k, 4)]
        y += h_px + 2
    return img


# ---- what stands out of each kind's face (y0: its bottom in the rack)

def _ear_handles(y0, h, tex="pi_srv_handle"):
    return [(EQ_X0 + 0.2, y0 + h * 0.18, FACE_Z, EQ_X0 + 0.55, y0 + h * 0.82, FACE_Z + 0.25, tex),
            (EQ_X1 - 0.55, y0 + h * 0.18, FACE_Z, EQ_X1 - 0.2, y0 + h * 0.82, FACE_Z + 0.25, tex)]


CORDS = [(52, 120, 210), (52, 120, 210), (52, 120, 210), (230, 210, 60), (60, 170, 80), (52, 120, 210), (200, 60, 50)]


def _cord_colour(seed, k):
    return CORDS[(seed * 7 + k * 3) % len(CORDS)]


def _in_use(seed, k, share=0.8):
    return random.Random(seed * 131 + k).random() < share


@parts_of("patch")
def _patch_parts(y0, seed):
    """The patch cords: a plug in each port in use, its cord dropping into the cable manager below."""
    h = UH
    out = []
    for k, x in enumerate(PATCH_X):
        if not _in_use(seed, k):
            continue
        c = cord(_cord_colour(seed, k))
        px = EQ_X0 + x
        out += [(px - 0.13, y0 + h * 0.42 - 0.12, FACE_Z, px + 0.13, y0 + h * 0.42 + 0.12, FACE_Z + 0.35, c),
                (px - 0.06, y0 - 0.55, CORD_Z, px + 0.06, y0 + h * 0.42 - 0.12, CORD_Z + 0.1, c)]
    return out


@parts_of("switch")
def _switch_parts(y0, seed):
    h = UH
    out = []
    for row, yy in ((0, h * 0.72), (1, h * 0.3)):
        for k, x in enumerate(SW_X):
            if not _in_use(seed, k + 24 * row, 0.7):
                continue
            c = cord(_cord_colour(seed, k + row))
            px = EQ_X0 + x
            out += [(px - 0.11, y0 + yy - 0.1, FACE_Z, px + 0.11, y0 + yy + 0.1, FACE_Z + 0.3, c)]
            if row == 1:
                out += [(px - 0.05, y0 - 0.5, CORD_Z, px + 0.05, y0 + yy - 0.1, CORD_Z + 0.1, c)]
    return out


@parts_of("cablemgr")
def _cablemgr_parts(y0, seed):
    """The horizontal cable manager's fingers (the cords from the unit above dressed down over them)."""
    out = []
    for k in range(13):
        x = EQ_X0 + 1.4 + k * 0.85
        out += [(x, y0 + 0.08, FACE_Z, x + 0.2, y0 + UH - 0.08, FACE_Z + 0.2, FRAME)]
    return out


@parts_of("server1")
def _server1_parts(y0, seed):
    out = _ear_handles(y0, UH)
    for k in range(10):
        x0 = EQ_X0 + 1.05 + k * 0.82
        out += [(x0 + 0.52, y0 + 0.1, FACE_Z, x0 + 0.72, y0 + UH - 0.1, FACE_Z + 0.12, "pi_srv_latch")]
    out += [(EQ_X0 + 11.82, y0 + 0.2, FACE_Z, EQ_X0 + 12.13, y0 + UH - 0.2, FACE_Z + 0.12, "pi_srv_button")]
    return out


@parts_of("server2")
def _server2_parts(y0, seed):
    out = _ear_handles(y0, 2 * UH)
    for r in range(2):
        for k in range(4):
            x0 = EQ_X0 + 2.5 + k * 2.1
            yy = y0 + 0.1 + r * 0.62
            out += [(x0 + 0.05, yy + 0.04, FACE_Z - 0.1, x0 + 1.95, yy + 0.54, CARRIER_Z, {"*": "pi_srv_carrier", "south": "pi_srv_carrier_f!"}),
                    (x0 + 1.35, yy + 0.12, CARRIER_Z, x0 + 1.75, yy + 0.46, CARRIER_Z + 0.12, "pi_srv_latch")]
    out += [(EQ_X0 + 1.2, y0 + 0.2, FACE_Z, EQ_X0 + 1.5, y0 + 0.5, FACE_Z + 0.12, "pi_srv_button")]
    return out


@parts_of("shelf")
def _shelf_parts(y0, seed):
    out = _ear_handles(y0, 2 * UH, FRAME)
    for r in range(3):
        for k in range(4):
            x0 = EQ_X0 + 1.0 + k * 2.78
            yy = y0 + SHELF_Y0 + r * SHELF_STEP
            out += [(x0 + 0.04, yy, FACE_Z - 0.1, x0 + 2.66, yy + SHELF_H, CARRIER_Z, {"*": "pi_srv_carrier", "south": "pi_srv_carrier_f!"}),
                    (x0 + 2.0, yy + 0.06, CARRIER_Z, x0 + 2.55, yy + SHELF_H - 0.06, CARRIER_Z + 0.12, "pi_srv_latch")]
    return out


@parts_of("ppc")
def _ppc_parts(y0, seed):
    out = _ear_handles(y0, 2 * UH, "pi_srv_handle_grey")
    for r in range(2):
        for k in range(4):
            x0 = EQ_X0 + 4.9 + k * 1.75
            yy = y0 + 0.12 + r * 0.6
            out += [(x0 + 0.04, yy + 0.04, FACE_Z - 0.1, x0 + 1.61, yy + 0.51, CARRIER_Z, {"*": "pi_srv_carrier", "south": "pi_srv_carrier_f!"}),
                    (x0 + 1.1, yy + 0.12, CARRIER_Z, x0 + 1.5, yy + 0.43, CARRIER_Z + 0.12, "pi_srv_latch")]
    return out


@parts_of("io")
def _io_parts(y0, seed):
    """Each I/O card's front: its ejector handles at the top and the bottom."""
    h = 4 * UH
    out = _ear_handles(y0, h, "pi_srv_handle_grey")
    for k in range(IO_CARDS):
        x0 = EQ_X0 + IO_X0 + k * IO_STEP
        out += [(x0 + 0.06, y0 + 0.3, FACE_Z, x0 + IO_STEP - 0.12, y0 + h - 0.6, CARD_Z, {"*": "pi_srv_card_edge", "south": (SHEET, card_uv(k))}),
                (x0 + 0.18, y0 + h - 0.85, CARD_Z, x0 + IO_STEP - 0.24, y0 + h - 0.72, CARD_Z + 0.22, "pi_srv_ejector"),
                (x0 + 0.18, y0 + 0.42, CARD_Z, x0 + IO_STEP - 0.24, y0 + 0.55, CARD_Z + 0.22, "pi_srv_ejector")]
    return out


def card_uv(k):
    """A card's share of the I/O chassis face on the sheet (its front stands out of the chassis)."""
    u0, v0, u1, v1 = SLOTS["io"]
    w = u1 - u0
    h = v1 - v0
    x0 = (IO_X0 + k * IO_STEP + 0.06) / FACE_W
    x1 = (IO_X0 + (k + 1) * IO_STEP - 0.12) / FACE_W
    ya, yb = 0.3 / (4 * UH), (4 * UH - 0.6) / (4 * UH)
    return [round(u0 + x0 * w, 4), round(v0 + (1 - yb) * h, 4), round(u0 + x1 * w, 4), round(v0 + (1 - ya) * h, 4)]


@parts_of("core")
def _core_parts(y0, seed):
    out = _ear_handles(y0, 7 * UH)
    for k in range(CORE_CARDS):
        yy = y0 + 0.25 + k * CORE_STEP
        for x in (EQ_X0 + 1.05, EQ_X1 - 1.45):
            out += [(x, yy + 0.08, FACE_Z, x + 0.4, yy + CORE_STEP - 0.16, FACE_Z + 0.25, "pi_srv_ejector")]
    out += _switch_like_plugs(y0, seed)
    return out


def _switch_like_plugs(y0, seed):
    out = []
    for k in range(CORE_CARDS):
        if k in (2, 3):
            continue
        yy = y0 + 0.25 + k * CORE_STEP + CORE_STEP / 2 - 0.04
        for p in range(24):
            if not _in_use(seed + k, p, 0.6):
                continue
            x = EQ_X0 + 2.2 + (p // 6) * 2.4 + (p % 6) * 0.34
            out += [(x - 0.1, yy - 0.09, FACE_Z, x + 0.1, yy + 0.09, FACE_Z + 0.28, cord((240, 200, 40) if p % 5 else (52, 120, 210)))]
    return out


@parts_of("kvm")
def _kvm_parts(y0, seed):
    return [(EQ_X0 + 3.0, y0 + 0.12, FACE_Z, EQ_X0 + 10.1, y0 + 0.32, FACE_Z + 0.35, "pi_srv_handle")]


@parts_of("fiber")
def _fiber_parts(y0, seed):
    return [(EQ_X0 + 5.6, y0 + 2 * UH - 0.4, FACE_Z, EQ_X0 + 7.5, y0 + 2 * UH - 0.25, FACE_Z + 0.2, "pi_srv_handle")]


# ---- each kind's lights: (x0, y, dx, n, colour, colour2, mode, seed) on its face (x from the bezel's left, y up
# from its bottom). Modes: pwr (lit with power), act (blinks with the traffic), link (lit, flickering with traffic),
# stat (green, amber when the room is hot), drive (fast activity).
GREEN, AMBER, BLUE, RED = (90, 240, 120), (255, 180, 40), (80, 160, 255), (255, 60, 50)


@leds_of("switch")
def _switch_leds(seed):
    out = []
    for g in range(4):
        out += [(SW_X[g * 6] - 0.1, UH * 0.93, 0.32, 6, GREEN, None, "link", seed + g),
                (SW_X[g * 6] + 0.1, UH * 0.93, 0.32, 6, AMBER, None, "act", seed + 10 + g),
                (SW_X[g * 6] - 0.1, UH * 0.08, 0.32, 6, GREEN, None, "link", seed + 20 + g)]
    out += [(1.2, UH * 0.8, 0.0, 1, GREEN, None, "pwr", 0), (1.2, UH * 0.25, 0.0, 1, GREEN, AMBER, "stat", 0)]
    return out


@leds_of("server1")
def _server1_leds(seed):
    # Each drive's light on its latch, the power light in the power button, the status light left of it (clear of
    # the ear handle).
    out = [(1.05 + k * 0.82 + 0.62, UH * 0.72, 0.0, 1, GREEN, None, "drive", seed + k, ON_PART) for k in range(10)]
    out += [(11.97, UH * 0.5, 0.0, 1, GREEN, None, "pwr", 0, ON_PART), (11.5, UH * 0.5, 0.0, 1, BLUE, AMBER, "stat", 0)]
    return out


@leds_of("server2")
def _server2_leds(seed):
    out = []
    for r in range(2):
        for k in range(4):
            out += [(2.5 + k * 2.1 + 1.86, 0.1 + r * 0.62 + 0.36, 0.0, 1, GREEN, None, "drive", seed + r * 4 + k, CARRIER_Z + 0.03)]
    out += [(1.35, 0.7, 0.3, 2, GREEN, AMBER, "stat", 0), (1.35, 0.35, 0.0, 1, GREEN, None, "pwr", 0, ON_PART)]
    return out


@leds_of("shelf")
def _shelf_leds(seed):
    out = []
    for r in range(3):
        for k in range(4):
            x = 1.0 + k * 2.78 + 0.25
            y = SHELF_Y0 + r * SHELF_STEP + SHELF_H / 2
            out += [(x, y, 0.0, 1, GREEN, None, "drive", seed + r * 4 + k, CARRIER_Z + 0.03),
                    (x + 0.22, y, 0.0, 1, BLUE, None, "pwr", 0, CARRIER_Z + 0.03)]
    out += [(0.72, 1.0, 0.0, 1, GREEN, AMBER, "stat", 0), (0.72, 0.7, 0.0, 1, GREEN, None, "pwr", 0)]   # right of the ear handle
    return out


@leds_of("ctrl")
def _ctrl_leds(seed):
    return [(7.8, 0.3, 0.3, 4, GREEN, None, "act", seed), (10.2, 0.3, 0.0, 1, GREEN, AMBER, "stat", 0), (10.5, 0.3, 0.0, 1, GREEN, None, "pwr", 0)]


@leds_of("fcsw")
def _fcsw_leds(seed):
    out = []
    for g in range(6):
        out += [(2.0 + g * 1.65, UH * 0.85, 0.36, 4, GREEN, None, "link", seed + g)]
    out += [(12.4, UH * 0.5, 0.0, 1, GREEN, None, "pwr", 0)]
    return out


@leds_of("gps")
def _gps_leds(seed):
    return [(9.9, UH * 0.7, 0.0, 1, GREEN, None, "pwr", 0), (9.9, UH * 0.3, 0.0, 1, GREEN, AMBER, "stat", 0)]


@leds_of("firewall")
def _firewall_leds(seed):
    return [(6.0 + k * 0.42 - 0.1, UH * 0.88, 0.0, 1, GREEN, None, "link", seed + k) for k in range(8)] + [
        (5.2, UH * 0.5, 0.0, 1, GREEN, None, "pwr", 0), (5.5, UH * 0.5, 0.0, 1, GREEN, AMBER, "stat", 0)]


@leds_of("io")
def _io_leds(seed):
    out = []
    for k in range(IO_CARDS):
        x = IO_X0 + k * IO_STEP + IO_STEP / 2 - 0.15
        out += [(x, 4 * UH - 1.0, 0.0, 1, GREEN, None, "pwr", 0, CARD_Z + 0.03), (x + 0.24, 4 * UH - 1.0, 0.0, 1, AMBER, None, "act", seed + k, CARD_Z + 0.03)]
    return out


@leds_of("ppc")
def _ppc_leds(seed):
    out = [(4.9 + k * 1.75 + 0.3, 0.12 + r * 0.6 + 0.28, 0.0, 1, GREEN, None, "drive", seed + r * 4 + k, CARRIER_Z + 0.03) for r in range(2)
           for k in range(4)]
    out += [(1.25, 0.35, 0.3, 3, GREEN, AMBER, "stat", 0)]
    return out


@leds_of("core")
def _core_leds(seed):
    out = []
    for k in range(CORE_CARDS):
        y = 0.25 + k * CORE_STEP + CORE_STEP - 0.2
        if k in (2, 3):
            out += [(7.4, y - 0.1, 0.3, 4, GREEN, AMBER, "stat", 0)]
            continue
        for g in range(4):
            out += [(2.2 + g * 2.4 - 0.1, y, 0.34, 6, GREEN, None, "link", seed + k * 4 + g)]
    return out


@leds_of("kvm")
def _kvm_leds(seed):
    return [(11.6, UH * 0.5, 0.0, 1, GREEN, None, "pwr", 0)]


@leds_of("fiber")
def _fiber_leds(seed):
    return []


@texts_of("gps")
def _gps_texts():
    return [(6.9, UH * 0.5, 0.42, (255, 40, 30), "srv_time", "center")]


@texts_of("ppc")
def _ppc_texts():
    return [(1.3, 1.1, 0.2, (120, 230, 150), "srv_ppc0", "left"), (1.3, 0.85, 0.2, (120, 230, 150), "srv_ppc1", "left")]


@texts_of("ctrl")
def _ctrl_texts():
    return []


# ---- the racks: their units from the top down (U42 first), filled to 42 with blanks.
RACKS = {
    "server_rack": ("Server Rack", ["patch", "cablemgr", "switch", "cablemgr", "switch", "cablemgr", "kvm", "blank"]
                    + ["server1"] * 10 + ["blank2"] + ["server2"] * 6 + ["blank2", "blank", "blank"]),
    "network_rack": ("Network Rack", ["patch", "cablemgr", "switch", "cablemgr", "patch", "cablemgr", "switch", "cablemgr",
                                      "patch", "cablemgr", "switch", "cablemgr", "fiber", "fiber", "blank", "firewall", "firewall",
                                      "blank", "core", "blank", "gps", "blank2"]),
    "san_rack": ("Storage Array Rack", ["fcsw", "fcsw", "cablemgr", "blank", "ctrl", "ctrl"] + ["shelf"] * 12 + ["blank2"]),
    "ppc_rack": ("Plant Computer Rack", ["patch", "cablemgr", "switch", "cablemgr", "firewall", "gps", "kvm", "ppc", "ppc",
                                          "io", "io", "io", "io", "blank", "server1", "server1", "server1", "server1", "blank2"]),
}


def layout(key):
    """A rack's units from the bottom: (kind, U at its bottom (0-based), seed)."""
    kinds = RACKS[key][1]
    out = []
    top = NU
    for i, k in enumerate(kinds):
        if top - UNITS[k].u < SEAM_U < top:
            # It would lie across the blocks' seam (its face is a region of the sheet, not cut): a blank over it.
            top -= 1
            out.append(("blank", top, 0))
        top -= UNITS[k].u
        out.append((k, top, i * 17 + len(key)))
    assert top >= 0, (key, top)
    # The rest at the bottom, blanks.
    while top > 0:
        top -= 1
        out.append(("blank", top, 0))
    return out


def u_y(u):
    return RAIL_Y0 + u * UH


def rack_boxes(key, open_=False):
    if not SLOTS:
        sheet()
    b = frame(open_)
    for kind, u, seed in layout(key):
        un = UNITS[kind]
        y0, y1 = u_y(u), u_y(u + un.u)
        b += [(BODY_X0, y0, 3.0, BODY_X1, y1, 13.9, "pi_srv_chassis"),
              (EQ_X0, y0, 13.9, EQ_X1, y1, FACE_Z, {"*": BEZEL, "south": (SHEET, SLOTS[kind])})]
        if un.parts:
            b += un.parts(y0, seed)
    return b


def cord(rgb):
    """A patch cord's colour (its own little texture)."""
    return "pi_srv_cord_" + {(52, 120, 210): "blue", (230, 210, 60): "yellow", (60, 170, 80): "green", (200, 60, 50): "red",
                             (240, 200, 40): "yellow"}[tuple(rgb)]


def rack_front(key):
    """The rack's live parts: its lights (leds strips), its number, the time server's clock, the process computer's
    displays."""
    f = PP.Front(1, 820 + len(key))
    f.text(8.0, 30.6, ZF + 0.17, 0.42, (24, 24, 26), "lbl_srv", "center")
    for kind, u, seed in layout(key):
        un = UNITS[kind]
        y0 = u_y(u)
        if un.leds:
            for led in un.leds(seed):
                x, y, dx, n, c, c2, mode, s = led[:8]
                e = {"t": "leds", "x0": EQ_X0 + x, "y": y0 + y, "z": led[8] if len(led) > 8 else LED_Z, "r": 0.06, "width": dx, "blades": n,
                     "colour": list(c), "ch": "srv_act", "align": mode, "lo": float(s % 997), "hi": 1.0}
                if c2:
                    e["colour2"] = list(c2)
                f.live.append(e)
        if un.texts:
            for x, y, size, c, ch, align in un.texts():
                f.text(EQ_X0 + x, y0 + y, FACE_Z + 0.02, size, c, ch, align)
    return f


# ------------------------------------------------------------------------------------------------ textures

def _img(w, h, rgb):
    return Image.new("RGBA", (w, h), tuple(rgb) + (255,))


def mesh_tex():
    """The perforated steel, a block's face of it: round holes in staggered rows (cut out: you see in), laid out so the
    next block's carries straight on."""
    W = 512
    img = Image.new("RGBA", (W, W), (22, 23, 26, 255))
    d = ImageDraw.Draw(img)
    r, step = 5, 16
    for row in range(W // step):
        y = row * step + step // 2
        for col in range(W // step):
            x = col * step + (step // 2 if row % 2 else 0) + step // 4
            for xx in (x, x - W):
                d.ellipse([xx - r, y - r, xx + r, y + r], fill=(0, 0, 0, 0))
    return img


def top_tex():
    """The top cover: the cable entry slots with their brushes front and back, the vents."""
    img = _img(256, 256, (26, 27, 30))
    d = ImageDraw.Draw(img)
    for y0 in (18, 200):
        d.rectangle([40, y0, 216, y0 + 34], fill=(10, 10, 12))
        for x in range(42, 216, 3):
            d.line([(x, y0 + 2), (x + 1, y0 + 32)], fill=(34, 34, 38))
    for x in range(40, 216, 12):
        d.rectangle([x, 90, x + 6, 160], fill=(14, 14, 16))
    return img


def kick_tex():
    img = _img(256, 32, (24, 25, 28))
    d = ImageDraw.Draw(img)
    for x in range(20, 236, 10):
        d.rectangle([x, 8, x + 5, 24], fill=(10, 10, 12))
    return img


def rail_tex():
    """A rail's face: the square holes three to a unit, a unit's number every fifth."""
    img = _img(32, 1024, (150, 152, 156))
    d = ImageDraw.Draw(img)
    per = 1024 / (NU * UH + 0.2) * UH
    for u in range(NU):
        y = 1024 - (0.1 / UH + u + 0.5) * per
        for k in (-1, 0, 1):
            yy = y + k * per * 0.32
            d.rectangle([10, yy - 3, 20, yy + 3], fill=(30, 30, 32))
    return img


def label_tex():
    img = _img(256, 36, (240, 240, 236))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 35], outline=(30, 30, 32), width=2)
    return img


def carrier_front():
    """A drive carrier's front: its vented face (the latch stands out of it)."""
    img = _img(128, 32, (46, 48, 52))
    d = ImageDraw.Draw(img)
    for x in range(8, 80, 6):
        d.rectangle([x, 6, x + 3, 26], fill=(18, 18, 20))
    return img


def _square(make):
    """The game's block textures are square, a power of two on a side: a drawing made to its face's shape is stretched
    so (its face maps the whole of it)."""
    def made():
        img = make()
        side = 1 << (max(img.size) - 1).bit_length()
        return img if img.size == (side, side) else img.resize((side, side), Image.LANCZOS)
    return made


def makers():
    return {k: _square(v) for k, v in _makers().items()}


def _makers():
    solid = PP._solid
    return {
        FRAME: lambda: solid((26, 27, 30), 830, 0.02), BEZEL: lambda: solid((30, 31, 34), 831, 0.02),
        "pi_srv_chassis": lambda: solid((70, 72, 76), 832, 0.03), SHEET: sheet,
        "pi_srv_mesh": mesh_tex, "pi_srv_top": top_tex, "pi_srv_kick": kick_tex,
        "pi_srv_rail": rail_tex, "pi_srv_rail_edge": lambda: solid((150, 152, 156), 833, 0.03), "pi_srv_label": label_tex,
        "pi_srv_handle": lambda: solid((40, 42, 46), 834, 0.03), "pi_srv_handle_grey": lambda: solid((150, 154, 158), 835, 0.03),
        "pi_srv_latch": lambda: solid((120, 124, 130), 836, 0.03), "pi_srv_button": lambda: solid((60, 62, 66), 837, 0.02),
        "pi_srv_carrier": lambda: solid((40, 42, 46), 838, 0.02), "pi_srv_carrier_f": carrier_front,
        "pi_srv_card_edge": lambda: solid((120, 124, 128), 839, 0.02), "pi_srv_ejector": lambda: solid((196, 40, 36), 840, 0.03),
        "pi_srv_cord_blue": lambda: solid((52, 120, 210), 841, 0.03), "pi_srv_cord_yellow": lambda: solid((230, 200, 50), 842, 0.03),
        "pi_srv_cord_green": lambda: solid((60, 170, 80), 843, 0.03), "pi_srv_cord_red": lambda: solid((200, 60, 50), 844, 0.03),
    }


SHARP = {SHEET, "pi_srv_mesh", "pi_srv_top", "pi_srv_kick", "pi_srv_rail", "pi_srv_label", "pi_srv_carrier_f"}


# ------------------------------------------------------------------------------------------------ the cabinets
# The UPS lineup along the east wall (the UPS, its two battery cabinets, the maintenance bypass, the PDU that feeds the
# rows) and the two computer room air conditioners at the west end. Each is drawn at 512 px a block (plant_panels
# Front), its doors, handles, display bezels and buttons real boxes standing out of it; their displays and lamps live
# (nuclear/ServerRoom.java: the room's state, at the UPS).

UPS_KEY, BATT_KEY, MBP_KEY, PDU_KEY, CRAC_KEY = "ups_module", "ups_battery", "ups_bypass", "pdu_cabinet", "crac_unit"
UGREY = (206, 208, 204, 255)          # the UPS lineup's light grey
UDARK = (60, 64, 70, 255)
CGREY = (214, 216, 212, 255)          # the CRACs' panels
NAVY = (14, 22, 34, 255)
DOOR_Z0 = 15.0                        # the body's front (the doors' backs)
DOOR_Z1 = 15.3                        # the doors' faces


def _fitc(f, cx, cy, text, size, max_w, fill=PP.INK):
    while size > 5 and f.d.textlength(text, font=PP.cfont(size, True)) > max_w * PP.U:
        size -= 0.5
    f.T(cx, cy, text, size, fill=fill, bold=True)


def _seam(f, x0, y0, x1, y1, base):
    """A door's outline: the shadow of its gap, its lit edge."""
    f.R(x0, y0, x1, y1, fill=base, outline=(96, 98, 100, 255), width=3)
    f.R(x0 + 0.15, y0 + 0.15, x1 - 0.15, y1 - 0.15, outline=tuple(min(255, c + 18) for c in base[:3]) + (255,), width=2)


def _louvres(f, x0, y0, x1, y1, pitch=0.55):
    y = y0
    while y + pitch * 0.5 <= y1:
        f.R(x0, y, x1, y + pitch * 0.45, fill=(40, 42, 46, 255))
        f.R(x0, y + pitch * 0.45, x1, y + pitch * 0.55, fill=(150, 152, 150, 255))
        y += pitch


def _warning(f, cx, cy, w, lines, colour=(250, 200, 30, 255)):
    h = 0.55 + 0.6 * len(lines)
    f.R(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2, fill=colour, outline=(20, 20, 22, 255), width=2)
    for i, s in enumerate(lines):
        _fitc(f, cx, cy + (len(lines) - 1) * 0.3 - i * 0.6, s, 7 if i else 8, w - 0.3)


def _cab(W, F, f, body="pi_ups_body", top="pi_ups_top!", doors=(), handles=()):
    """A floor-standing cabinet W px wide: its plinth, body (its front the drawing), top; the doors standing out of
    it (their faces the drawing too), each door's handle; what the drawing stands proud."""
    b = [(0.6, 0, 2.4, W - 0.6, 1.0, 14.8, "pi_ups_plinth"),
         (0.2, 1.0, 1.0, W - 0.2, 31.0, DOOR_Z0, {"*": body, "south": F}),
         (0.1, 31.0, 0.9, W - 0.1, 31.6, DOOR_Z1 + 0.1, {"*": "pi_ups_trim", "up": top})]
    for x0, y0, x1, y1 in doors:
        b.append((x0, y0, DOOR_Z0, x1, y1, DOOR_Z1, {"*": body, "south": F}))
    for x, y0, y1 in handles:
        b += [(x - 0.25, y0, DOOR_Z1, x + 0.25, y1, DOOR_Z1 + 0.12, "pi_ups_trim"),
              (x - 0.15, y0 + 0.4, DOOR_Z1 + 0.12, x + 0.15, y1 - 0.4, DOOR_Z1 + 0.5, CHROME)]
    for x0, y0, x1, y1, depth, side in f.boxes:
        b.append((x0, y0, DOOR_Z1, x1, y1, DOOR_Z1 + depth, {"*": side, "south": F}))
    return b


# ---- the UPS module
UPS_SCREEN = (4.0, 21.0, 12.0, 26.2)
UPS_LAMPS = [("RECT", "ups_rect", (90, 230, 120)), ("INV", "ups_inv", (90, 230, 120)), ("BATT", "ups_batt", (255, 180, 40)),
             ("BYP", "ups_byp", (255, 180, 40)), ("LOAD", "ups_load", (90, 230, 120))]


def ups_module(label=None):
    f = PP.Front(1, 851)
    f.R(0, 0, 16, 32, fill=UGREY)
    _seam(f, 0.8, 1.6, 15.2, 15.4, UGREY)
    _seam(f, 0.8, 15.8, 15.2, 30.6, UGREY)
    _louvres(f, 2.2, 3.0, 13.8, 13.6)
    # The nameplate, the display in its bezel, the mimic's lamps, the EPO under its cover.
    f.R(2.0, 27.6, 14.0, 29.8, fill=(30, 32, 36, 255))
    _fitc(f, 8.0, 29.1, "UPS-1  PLANT COMPUTER", 9, 11.6, fill=(236, 236, 230, 255))
    _fitc(f, 8.0, 28.2, "80 kVA   480 V 3 PH   60 HZ", 7, 11.6, fill=(190, 192, 196, 255))
    x0, y0, x1, y1 = UPS_SCREEN
    f.R(x0 - 0.5, y0 - 0.5, x1 + 0.5, y1 + 0.5, fill=(30, 32, 36, 255))
    f.R(x0, y0, x1, y1, fill=NAVY)
    f.R(x0, y1 - 0.75, x1, y1, fill=(30, 70, 140, 255))
    f.proud(x0 - 0.5, y0 - 0.5, x1 + 0.5, y1 + 0.5, 0.3, "pi_ups_trim")
    for k, (name, ch, c) in enumerate(UPS_LAMPS):
        cx = 3.6 + k * 2.2
        f.lamp(cx, 19.4, c, False, r=0.38, live=ch)
        _fitc(f, cx, 18.4, name, 7, 2.0)
    f.R(12.9, 16.6, 14.6, 18.3, fill=(250, 200, 30, 255))
    f.E(13.75, 17.45, 0.6, fill=(200, 30, 26, 255))
    _fitc(f, 13.75, 16.2, "EPO", 7, 1.8)
    f.proud(12.9, 16.6, 14.6, 18.3, 0.12, "pi_ups_trim")
    f.proud(13.25, 16.95, 14.25, 17.95, 0.45, "pi_epo")
    _warning(f, 8.0, 14.4, 6.0, ["WARNING", "HAZARDOUS VOLTAGE INSIDE"])
    # The live display: the mode in the title bar, then volts in and out, the load, the battery.
    for i in range(5):
        y = y1 - 0.38 - i * 0.92
        f.text(x0 + 0.3, y, DOOR_Z1 + 0.32, 0.42 if i else 0.4, (236, 238, 242) if i else (250, 250, 250), f"ups_l{i}", "left")
    for i in range(1, 5):
        f.text(x1 - 0.3, y1 - 0.38 - i * 0.92, DOOR_Z1 + 0.32, 0.42, (120, 230, 150), f"ups_v{i}", "right")
    for d in f.live:
        if d["t"] == "lamp":
            d["z"] = DOOR_Z1 + 0.34
    return f


def ups_module_boxes():
    f = ups_module()
    return _cab(16, f"pi_{UPS_KEY}_f", f, handles=[(14.2, 7.0, 10.0), (14.2, 21.0, 24.0)])


# ---- its battery cabinets and the maintenance bypass
def ups_battery(label=None):
    f = PP.Front(1, 852)
    f.R(0, 0, 16, 32, fill=UGREY)
    _seam(f, 0.8, 1.6, 15.2, 30.6, UGREY)
    _louvres(f, 2.2, 3.0, 13.8, 7.0)
    _louvres(f, 2.2, 25.0, 13.8, 29.6)
    f.R(3.0, 21.0, 13.0, 23.4, fill=(30, 32, 36, 255))
    _fitc(f, 8.0, 22.7, "UPS-1 BATTERY CABINET", 8, 9.6, fill=(236, 236, 230, 255))
    _fitc(f, 8.0, 21.7, "VRLA  40 x 12 V BLOCKS", 7, 9.6, fill=(190, 192, 196, 255))
    _warning(f, 8.0, 18.4, 8.4, ["DANGER", "480 VDC  ARC FLASH HAZARD", "BATTERIES - NO OPEN FLAME"], colour=(240, 64, 52, 255))
    # The battery disconnect's handle in its escutcheon.
    f.R(6.2, 9.0, 9.8, 14.0, fill=(40, 42, 46, 255))
    _fitc(f, 8.0, 14.6, "BATTERY DISCONNECT", 7, 6.0)
    f.T(8.0, 9.5, "ON        OFF", 6, fill=(236, 236, 230, 255), bold=True)
    f.proud(6.2, 9.0, 9.8, 14.0, 0.15, "pi_ups_trim")
    return f


def ups_battery_boxes():
    f = ups_battery()
    b = _cab(16, f"pi_{BATT_KEY}_f", f, handles=[(14.2, 14.0, 18.0)])
    # The disconnect's handle, up (ON).
    b += [(7.6, 10.2, DOOR_Z1 + 0.15, 8.4, 13.4, DOOR_Z1 + 0.85, "pi_srv_bkr_handle")]
    return b


MBP_BREAKERS = [("MIB", "MODULE INPUT"), ("MBB", "MAINT BYPASS"), ("MIS", "MODULE OUTPUT")]


def ups_bypass(label=None):
    f = PP.Front(1, 853)
    f.R(0, 0, 16, 32, fill=UGREY)
    _seam(f, 0.8, 1.6, 15.2, 30.6, UGREY)
    f.R(2.0, 27.0, 14.0, 29.6, fill=(30, 32, 36, 255))
    _fitc(f, 8.0, 28.8, "MAINTENANCE BYPASS", 9, 11.6, fill=(236, 236, 230, 255))
    _fitc(f, 8.0, 27.7, "UPS-1   DO NOT OPERATE", 7, 11.6, fill=(250, 200, 30, 255))
    # The three breakers in a window, their handles real; the key interlock; the two lamps.
    f.R(2.0, 13.0, 14.0, 25.6, fill=(30, 32, 36, 255))
    f.R(2.3, 13.3, 13.7, 25.3, fill=(52, 56, 62, 255))
    for k, (tag, name) in enumerate(MBP_BREAKERS):
        cx = 4.2 + k * 3.8
        f.R(cx - 1.4, 16.0, cx + 1.4, 23.4, fill=(24, 24, 26, 255))
        f.R(cx - 1.2, 22.0, cx + 1.2, 23.0, fill=(240, 240, 234, 255))
        _fitc(f, cx, 22.5, tag, 8, 2.2)
        _fitc(f, cx, 15.3, name, 6, 3.4, fill=(236, 236, 230, 255))
        f.T(cx, 21.4, "ON", 6, fill=(236, 236, 230, 255), bold=True)
        f.T(cx, 16.6, "OFF", 6, fill=(236, 236, 230, 255), bold=True)
    for k, (name, ch, c) in enumerate((("ON UPS", "mbp_ups", (90, 230, 120)), ("ON BYPASS", "mbp_byp", (255, 180, 40)))):
        cx = 5.0 + k * 6.0
        f.lamp(cx, 10.6, c, False, r=0.45, live=ch)
        _fitc(f, cx, 9.4, name, 7, 4.0)
    f.R(6.9, 4.6, 9.1, 7.6, fill=(170, 150, 60, 255))
    _fitc(f, 8.0, 3.9, "KEY INTERLOCK", 6, 4.0)
    for d in f.live:
        d["z"] = DOOR_Z1 + 0.34
    return f


def ups_bypass_boxes():
    f = ups_bypass()
    b = _cab(16, f"pi_{MBP_KEY}_f", f, handles=[(14.4, 14.0, 18.0)])
    b += [(2.0, 13.0, DOOR_Z1, 14.0, 25.6, DOOR_Z1 + 0.15, {"*": "pi_ups_trim", "south": f"pi_{MBP_KEY}_f"})]
    for k in range(3):
        cx = 4.2 + k * 3.8
        up = k != 1           # the bypass breaker open, the module's two closed
        y0, y1 = (19.2, 21.2) if up else (17.0, 19.0)
        b += [(cx - 0.35, y0, DOOR_Z1 + 0.15, cx + 0.35, y1, DOOR_Z1 + 0.9, "pi_srv_bkr_handle")]
    b += [(7.5, 5.2, DOOR_Z1, 8.5, 7.0, DOOR_Z1 + 0.35, CHROME)]
    return b


# ---- the PDU that feeds the rows: its transformer and panelboards behind the doors, its meter live.
PDU_SCREEN = (4.2, 23.2, 11.8, 27.0)


def pdu_cabinet(label=None):
    f = PP.Front(1, 854)
    f.R(0, 0, 16, 32, fill=UGREY)
    _seam(f, 0.8, 15.8, 15.2, 30.6, UGREY)
    _seam(f, 0.8, 1.6, 7.9, 15.4, UGREY)
    _seam(f, 8.1, 1.6, 15.2, 15.4, UGREY)
    f.R(2.0, 28.0, 14.0, 30.0, fill=(30, 32, 36, 255))
    _fitc(f, 8.0, 29.3, "PDU-1  POWER DISTRIBUTION", 8, 11.6, fill=(236, 236, 230, 255))
    _fitc(f, 8.0, 28.5, "FED FROM UPS-1   ROWS A - D", 7, 11.6, fill=(190, 192, 196, 255))
    x0, y0, x1, y1 = PDU_SCREEN
    f.R(x0 - 0.4, y0 - 0.4, x1 + 0.4, y1 + 0.4, fill=(30, 32, 36, 255))
    f.R(x0, y0, x1, y1, fill=(16, 30, 22, 255))
    f.proud(x0 - 0.4, y0 - 0.4, x1 + 0.4, y1 + 0.4, 0.25, "pi_ups_trim")
    for i in range(3):
        f.text(x0 + 0.25, y1 - 0.6 - i * 1.15, DOOR_Z1 + 0.27, 0.5, (120, 230, 150), f"pdu_l{i}", "left")
    # The main breaker's handle; the two panelboards' circuit directories.
    f.R(6.0, 18.0, 10.0, 21.6, fill=(40, 42, 46, 255))
    _fitc(f, 8.0, 22.2, "MAIN 400 A", 7, 4.4)
    for p, px in enumerate((1.6, 8.9)):
        f.R(px, 4.0, px + 5.5, 13.6, fill=(244, 244, 238, 255), outline=(60, 60, 62, 255), width=2)
        _fitc(f, px + 2.75, 13.0, f"PANEL {'AB'[p]}  CIRCUITS", 7, 5.2)
        for k in range(12):
            y = 12.2 - k * 0.68
            row = "ABCD"[(k // 6) + 2 * p]
            f.T(px + 0.3, y, f"{k + 1 + 12 * p:02d}", 5, anchor="lm")
            f.T(px + 1.3, y, f"RACK {row}-{(k % 6) + 1 + (6 if k >= 6 else 0):02d}", 5, anchor="lm")
            f.d.line([f.px(px + 0.2, y - 0.34), f.px(px + 5.3, y - 0.34)], fill=(180, 180, 176, 255), width=1)
    return f


def pdu_cabinet_boxes():
    f = pdu_cabinet()
    b = _cab(16, f"pi_{PDU_KEY}_f", f, handles=[(14.3, 21.0, 24.4), (7.2, 7.0, 10.0), (14.5, 7.0, 10.0)])
    b += [(7.6, 18.8, DOOR_Z1, 8.4, 20.8, DOOR_Z1 + 0.8, "pi_srv_bkr_handle")]
    return b


# ---- the computer room air conditioners: two blocks wide, downflow (the return in its top, the supply under the
# floor), its controller live (CRAC-1 and CRAC-2: one drawing, the name lettered live).
CRAC_SCREEN = (3.0, 20.6, 13.0, 27.4)
CRAC_LABELS = ["CRAC-1", "CRAC-2"]


def crac_unit(label=None):
    f = PP.Front(2, 855)
    f.R(0, 0, 32, 32, fill=CGREY)
    for x0, x1 in ((0.8, 15.9), (16.1, 31.2)):
        _seam(f, x0, 1.6, x1, 15.4, CGREY)
        _seam(f, x0, 15.8, x1, 30.6, CGREY)
    # The blue band and the maker's badge along the top.
    f.R(0.8, 29.0, 31.2, 30.6, fill=(40, 90, 160, 255))
    _fitc(f, 24.0, 29.8, "PRECISION COOLING   DOWNFLOW", 8, 13.0, fill=(240, 240, 236, 255))
    # The controller: its display, the run and alarm lights, its keys.
    x0, y0, x1, y1 = CRAC_SCREEN
    f.R(x0 - 0.6, y0 - 2.4, x1 + 0.6, y1 + 0.6, fill=(40, 42, 46, 255))
    f.R(x0, y0, x1, y1, fill=(16, 26, 40, 255))
    f.proud(x0 - 0.6, y0 - 2.4, x1 + 0.6, y1 + 0.6, 0.3, "pi_ups_trim")
    for k, s in enumerate(("ON/OFF", "ALARM", "SET", "MENU")):
        cx = x0 + 1.2 + k * 2.55
        f.R(cx - 0.9, y0 - 1.8, cx + 0.9, y0 - 0.7, fill=(70, 74, 80, 255))
        f.T(cx, y0 - 1.25, s, 5, fill=(236, 236, 230, 255), bold=True)
    for i in range(6):
        f.text(x0 + 0.35, y1 - 0.55 - i * 1.08, DOOR_Z1 + 0.32, 0.5, (236, 238, 242) if i == 0 else (150, 220, 255), f"crac_l{i}", "left")
    for i in range(1, 6):
        f.text(x1 - 0.35, y1 - 0.55 - i * 1.08, DOOR_Z1 + 0.32, 0.5, (240, 240, 236), f"crac_v{i}", "right")
    f.lamp(x1 + 1.6, 26.0, (90, 230, 120), False, r=0.4, live="crac_run")
    f.lamp(x1 + 1.6, 24.2, (255, 60, 50), False, r=0.4, live="crac_alarm")
    _fitc(f, x1 + 1.6, 25.1, "RUN", 6, 1.6)
    _fitc(f, x1 + 1.6, 23.3, "ALARM", 6, 1.8)
    # The main isolator on the upper right door; the filter door's label; the electrical warning.
    f.R(21.4, 20.0, 26.6, 25.4, fill=(250, 200, 30, 255))
    f.E(24.0, 22.7, 1.6, fill=(200, 30, 26, 255))
    _fitc(f, 24.0, 25.9, "MAIN ISOLATOR", 7, 6.0)
    f.T(22.2, 20.5, "OFF", 6, bold=True)
    f.T(25.8, 20.5, "ON", 6, bold=True)
    f.proud(21.4, 20.0, 26.6, 25.4, 0.15, "pi_ups_trim")
    _warning(f, 8.4, 11.0, 7.0, ["FILTERS", "CHANGE EVERY 3 MONTHS"], colour=(236, 236, 230, 255))
    _warning(f, 23.6, 11.0, 7.0, ["WARNING", "480 V  ISOLATE BEFORE", "OPENING"])
    for d in f.live:
        if d["t"] == "lamp":
            d["z"] = DOOR_Z1 + 0.34
    return f


def crac_unit_boxes():
    f = crac_unit()
    F = f"pi_{CRAC_KEY}_f"
    b = _cab(32, F, f, top="pi_crac_grille!", doors=[(0.8, 1.6, 15.9, 15.4), (16.1, 1.6, 31.2, 15.4), (0.8, 15.8, 15.9, 30.6),
                                                       (16.1, 15.8, 31.2, 30.6)],
             handles=[(14.9, 7.0, 10.0), (17.1, 7.0, 10.0), (14.9, 17.0, 20.0), (17.1, 17.0, 20.0)])
    # The isolator's handle across its knob (ON: horizontal).
    b += [(22.2, 22.4, DOOR_Z1 + 0.15, 25.8, 23.0, DOOR_Z1 + 0.85, "pi_isolator")]
    # Its keys.
    x0, y0 = CRAC_SCREEN[:2]
    for k in range(4):
        cx = x0 + 1.2 + k * 2.55
        b += [(cx - 0.9, y0 - 1.8, DOOR_Z1 + 0.3, cx + 0.9, y0 - 0.7, DOOR_Z1 + 0.45, {"*": "pi_ups_trim", "south": F})]
    return b


# ------------------------------------------------------------------------------------------------ the clean-agent system
# FM-200 style (Justin: "Clean-agent fire system"): three agent cylinders strapped to the east wall, their valves'
# gauges live (the agent's pressure: empty after a discharge), their discharge hoses up into a manifold and a riser to
# the pipe under the ceiling, four 360-degree nozzles over the aisles; the releasing panel by the door (two-zone
# detection, a 30 second pre-discharge countdown, ABORT held off, RELEASED), the abort station and the manual release
# station beside it, a horn and strobe over the door (nuclear/ServerRoom.java: AgentSystem).

CYL_KEY, PANEL_KEY, HORN_KEY = "agent_cylinder", "agent_release_panel", "agent_horn_strobe"
RED = "pi_agent_red"
CYL_C = (8.0, 7.2)                  # the cylinder's axis (x, z) on the piece facing north, the wall at z 0
CYL_A = 4.6                         # its radius
CYL_TOP = 22.0
GAUGE = (8.0, 26.3, CYL_C[1] + 1.4, 0.95)    # the valve's gauge: middle x, y, its face z, radius


def agent_cylinder(label=None):
    f = PP.Front(1, 856)
    gx, gy, gz, gr = GAUGE
    f.R(0, 0, 16, 32, fill=(196, 40, 36, 255))
    f.E(gx, gy, gr, fill=(246, 246, 240, 255))
    for k in range(11):
        a = math.radians(225 - 27 * k)
        r0 = gr * (0.72 if k % 5 == 0 else 0.8)
        f.line([(gx + math.cos(a) * r0, gy + math.sin(a) * r0), (gx + math.cos(a) * gr * 0.92, gy + math.sin(a) * gr * 0.92)], PP.INK, 2)
    for k in range(6):
        a = math.radians(225 - 27 * (3.6 + 0.4 * k))
        f.E(gx + math.cos(a) * gr * 0.86, gy + math.sin(a) * gr * 0.86, 0.06, fill=(40, 170, 70, 255))
    f.T(gx, gy - 0.45, "PSI", 4, bold=True)
    f.live.append({"t": "dial", "x": gx, "y": gy, "r": gr * 0.95, "z": gz + 0.14,      # over the face (gz..gz + 0.1) "lo": 0, "hi": 600, "start": 225, "sweep": 270,
                   "ch": "agent_psi"})
    return f


def _cyl_label():
    img = Image.new("RGBA", (256, 512), (196, 40, 36, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([14, 40, 241, 470], fill=(244, 244, 238, 255), outline=(30, 30, 32, 255), width=4)
    PP._fit_line(d, (128, 92), "FM-200", 54, 200, (196, 40, 36, 255))
    PP._fit_line(d, (128, 150), "CLEAN AGENT", 34, 200, (24, 24, 26, 255))
    for k, s in enumerate(("HFC-227ea", "FIRE EXTINGUISHING", "SYSTEM CYLINDER", "", "AGENT 270 LB", "360 PSIG AT 70 F",
                           "", "DANGER: CONTENTS", "UNDER PRESSURE")):
        PP._fit_line(d, (128, 205 + k * 28), s, 22, 200, (24, 24, 26, 255))
    return img


def agent_cylinder_boxes():
    F = f"pi_{CYL_KEY}_f"
    cx, cz = CYL_C
    a = CYL_A
    s = a * math.tan(math.radians(22.5))
    out = PP.round_y(cx, cz, a, 0.4, 9.0, RED)
    lab = PP.round_y(cx, cz, a, 9.0, 17.0, RED)
    lab[1] = lab[1][:6] + ({"*": RED, "south": "pi_agent_label!"},)
    out += lab
    out += PP.round_y(cx, cz, a, 17.0, CYL_TOP, RED)
    # The shoulder and the neck.
    out += PP.round_y(cx, cz, a - 1.0, CYL_TOP, CYL_TOP + 0.9, RED)
    out += PP.round_y(cx, cz, a - 2.4, CYL_TOP + 0.9, CYL_TOP + 1.8, RED)
    # The valve: its brass body, the gauge on its front (its face the drawing, the needle live), the electric actuator
    # on top with its cable, the discharge outlet and the hose up to the manifold.
    gx, gy, gz, gr = GAUGE
    out += [(cx - 1.2, CYL_TOP + 1.8, cz - 1.2, cx + 1.2, CYL_TOP + 4.2, cz + 1.2, "pi_brass")]
    out += [(b[0], b[1], cz + 1.2, b[3], b[4], gz, "pi_brass") for b in PP._sc_round(gx, gy, 0, 0, gr + 0.15, "x")]
    out += [(b[0], b[1], gz, b[3], b[4], gz + 0.1, {"*": "pi_chrome", "south": F}) for b in PP._sc_round(gx, gy, 0, 0, gr, "x")]
    out += [(cx - 1.0, CYL_TOP + 4.2, cz - 1.0, cx + 1.0, CYL_TOP + 6.4, cz + 1.0, {"*": "pi_cap_black", "up": "pi_cap_black"}),
            (cx - 0.25, CYL_TOP + 6.4, cz - 0.25, cx + 0.25, 32.0, cz + 0.25, "pi_agent_cable")]
    out += [(cx + 1.2, CYL_TOP + 2.6, cz - 0.5, cx + 2.6, CYL_TOP + 3.6, cz + 0.5, "pi_brass"),
            (cx + 2.2, CYL_TOP + 3.6, cz - 0.6, cx + 3.4, 32.0, cz + 0.6, "pi_agent_hose")]
    # The wall bracket: a strut on the wall, the strap round the cylinder.
    out += [(cx - 5.4, 18.0, 0.2, cx + 5.4, 19.4, 1.2, "pi_srv_strut")]
    out += PP.ring_y(cx, cz, a, a + 0.25, 18.1, 19.3, "pi_steel_dark")
    out += [(cx - 0.6, 18.1, 1.2, cx + 0.6, 19.3, cz - a, "pi_steel_dark")]
    return out


# ---- the releasing panel by the door (on the wall at eye level, its window over the display and lamps)
RP_BOX = (2.6, 11.0, 13.4, 26.0)
RP_D = 2.6
RP_WIN = (3.4, 15.8, 12.6, 23.4)
RP_LCD = (4.0, 20.6, 12.0, 22.8)
RP_LAMPS = [(5.0, 19.4, (90, 230, 120), "agent_power", "AC POWER"), (5.0, 17.4, (255, 180, 40), "agent_trbl", "TROUBLE"),
            (8.0, 19.4, (255, 60, 50), "agent_alarm", "ALARM"), (8.0, 17.4, (255, 180, 40), "agent_abort", "ABORT"),
            (11.0, 19.4, (255, 60, 50), "agent_pre", "PRE-DISCH"), (11.0, 17.4, (255, 60, 50), "agent_rel", "RELEASED")]
RP_KEYS = [(4.6, 13.8, "pi_pnl_yellow", "ACK"), (6.8, 13.8, "pi_pnl_white", "SILENCE"), (9.0, 13.8, "pi_pnl_black", "RESET"),
           (11.2, 13.8, "pi_pnl_black", "DRILL")]
WHITE_A = (250, 250, 246, 255)


def agent_release_panel(label=None):
    f = PP.Front(1, 857)
    x0, y0, x1, y1 = RP_BOX
    f.R(x0, y0, x1, y1, fill=(190, 36, 32, 255))
    _fitc(f, 8.0, 25.2, "CLEAN AGENT RELEASING PANEL", 7, x1 - x0 - 0.8, fill=WHITE_A)
    _fitc(f, 8.0, 24.3, "SERVER ROOM   FM-200", 6, x1 - x0 - 0.8, fill=WHITE_A)
    f.R(*RP_WIN, fill=(36, 36, 38, 255))
    lx0, ly0, lx1, ly1 = RP_LCD
    f.R(lx0, ly0, lx1, ly1, fill=(20, 40, 28, 255))
    f.text(lx0 + 0.3, (ly0 + ly1) / 2 + 0.42, RP_D + 0.3, 0.55, (130, 240, 160), "agent_lcd1", "left")
    f.text(lx0 + 0.3, (ly0 + ly1) / 2 - 0.42, RP_D + 0.3, 0.55, (130, 240, 160), "agent_lcd2", "left")
    for x, y, c, ch, name in RP_LAMPS:
        f.E(x, y, 0.62, fill=(50, 50, 52, 255))
        f.E(x, y, 0.45, fill=tuple(int(v * 0.35) for v in c) + (255,))
        f.live.append({"t": "lamp", "x": x, "y": y, "r": 0.45, "z": RP_D + 0.52, "colour": list(c), "ch": ch})
        _fitc(f, x, y - 0.95, name, 5.5, 2.7, fill=WHITE_A)
    for x, y, tex, name in RP_KEYS:
        _fitc(f, x, y - 1.15, name, 5.5, 2.0, fill=WHITE_A)
    _fitc(f, 8.0, 11.7, "ZONE 1 + ZONE 2 DETECTION   30 S DELAY", 5.5, x1 - x0 - 0.6, fill=WHITE_A)
    return f


def agent_release_panel_boxes():
    F = f"pi_{PANEL_KEY}_f"
    x0, y0, x1, y1 = RP_BOX
    out = [(x0, y0, 0.0, x1, y1, RP_D, {"*": "pi_agent_red", "south": F})]
    wx0, wy0, wx1, wy1 = RP_WIN
    out += [(wx0, wy0, RP_D, wx1, wy1, RP_D + 0.15, {"*": "pi_pnl_black", "south": F})]
    for x, y, c, ch, name in RP_LAMPS:
        out += [(x - 0.62, y - 0.62, RP_D + 0.15, x + 0.62, y + 0.62, RP_D + 0.45, {"*": "pi_chrome", "south": F})]
    for x, y, tex, name in RP_KEYS:
        out += [(x - 0.75, y - 0.5, RP_D, x + 0.75, y + 0.5, RP_D + 0.4, tex)]
    # The key-lock, the conduit up out of its top.
    out += [(12.3, 24.6, RP_D, 12.9, 25.2, RP_D + 0.3, CHROME), (7.4, y1, 0.4, 8.6, 32.0, 1.6, "pi_steel")]
    return out


def horn_strobe(label=None):
    f = PP.Front(1, 858)
    f.R(0, 0, 16, 32, fill=(190, 36, 32, 255))
    f.T(8.0, 6.1, "FIRE  AGENT", 5.5, fill=WHITE_A, bold=True)
    f.T(8.0, 5.2, "DISCHARGE", 5.5, fill=WHITE_A, bold=True)
    f.live.append({"t": "window", "x0": 5.6, "y0": 11.6, "x1": 10.4, "y1": 13.6, "z": 3.15, "colour": [255, 255, 255], "alpha": 1.0,
                   "ch": "agent_strobe", "label": []})
    return f


def horn_strobe_boxes():
    """The horn and strobe high on the wall (one block): its red back box, the horn's grille, the strobe's lens."""
    F = f"pi_{HORN_KEY}_f"
    out = [(4.6, 4.0, 0.0, 11.4, 14.4, 2.0, {"*": "pi_agent_red", "south": F}),
           (5.6, 11.6, 2.0, 10.4, 13.6, 3.1, "pi_strobe_lens"),
           (7.4, 14.4, 0.4, 8.6, 16.0, 1.6, "pi_steel")]
    out += PP._sc_round(8.0, 8.4, 2.0, 2.6, 2.0, "pi_horn_grille")
    return out


def abort_station_boxes():
    """The abort station: a blue box, its yellow mushroom button (held, it holds the countdown), HOLD TO ABORT."""
    out = [(4.4, 2.0, 0.0, 11.6, 11.0, 1.8, {"*": "pi_agent_blue", "south": "pi_agent_abort!"})]
    out += PP._sc_round(8.0, 6.0, 1.8, 2.4, 1.3, "pi_pnl_yellow")
    out += PP._sc_round(8.0, 6.0, 2.4, 2.9, 1.0, "pi_pnl_yellow")
    return out


def manual_release_boxes():
    """The manual release station: a yellow pull box (PULL TO RELEASE AGENT), its handle, its clear cover's hinge."""
    out = [(4.4, 2.0, 0.0, 11.6, 11.6, 2.2, {"*": "pi_pnl_yellow", "south": "pi_agent_pull!"}),
           (6.0, 5.0, 2.2, 10.0, 6.4, 3.0, "pi_agent_red"),
           (4.6, 11.2, 2.2, 11.4, 11.6, 2.6, "pi_glass_edge")]
    return out


# ---- the pipe under the ceiling: along a run (facing north: east-west), a tee down to a nozzle, its elbows, the riser
PIPE_Y, PIPE_R = 13.0, 0.7          # the pipe's middle under the ceiling (the block under it), its radius
PIPE_Z = 8.0


def _pipe_x(x0, x1):
    return [(x0, PIPE_Y - PIPE_R, PIPE_Z - PIPE_R, x1, PIPE_Y + PIPE_R, PIPE_Z + PIPE_R, RED)]


def _pipe_hanger(x):
    return [(x - 0.25, PIPE_Y + PIPE_R + 0.3, PIPE_Z - 0.25, x + 0.25, 16.0, PIPE_Z + 0.25, "pi_steel"),
            (x - 0.4, PIPE_Y - PIPE_R - 0.3, PIPE_Z - PIPE_R - 0.3, x + 0.4, PIPE_Y + PIPE_R + 0.3, PIPE_Z + PIPE_R + 0.3,
             "pi_steel_dark")]                       # its ends drawn: a square collar round the pipe


def agent_pipe():
    return _pipe_x(0, 16) + _pipe_hanger(8.0)


def agent_pipe_nozzle(end=False):
    """A tee off the run, the drop and the 360-degree nozzle on it (its eight orifices round its brass body); at the
    run's end (end) the pipe from the east only, capped."""
    out = _pipe_x(8.0 - 0.9 if end else 0, 16)
    if end:
        out += [(PIPE_Z - 1.1, PIPE_Y - 0.9, PIPE_Z - 0.9, PIPE_Z - 0.9, PIPE_Y + 0.9, PIPE_Z + 0.9, "pi_steel_dark")]
    out += [(PIPE_Z - 0.9, PIPE_Y - 1.0, PIPE_Z - 0.9, PIPE_Z + 0.9, PIPE_Y + 1.0, PIPE_Z + 0.9, RED),
            (7.5, 5.6, 7.5, 8.5, PIPE_Y - 1.0, 8.5, RED),
            (7.2, 5.0, 7.2, 8.8, 5.6, 8.8, "pi_brass")]
    out += PP.round_y(8.0, 8.0, 1.1, 3.6, 5.0, "pi_brass")
    out += PP.round_y(8.0, 8.0, 0.6, 2.8, 3.6, "pi_brass")
    return out


def agent_pipe_elbow():
    """Along the run from the west (x 0), turning south (to +z)."""
    return [(0, PIPE_Y - PIPE_R, PIPE_Z - PIPE_R, PIPE_Z + PIPE_R, PIPE_Y + PIPE_R, PIPE_Z + PIPE_R, RED),
            (PIPE_Z - PIPE_R, PIPE_Y - PIPE_R, PIPE_Z + PIPE_R, PIPE_Z + PIPE_R, PIPE_Y + PIPE_R, 16, RED),
            (PIPE_Z - 0.9, PIPE_Y - 0.9, PIPE_Z - 0.9, PIPE_Z + 0.9, PIPE_Y + 0.9, PIPE_Z + 0.9, RED)]


def agent_pipe_tee():
    """The run east-west, a branch south."""
    return _pipe_x(0, 16) + [(PIPE_Z - PIPE_R, PIPE_Y - PIPE_R, PIPE_Z + 0.9, PIPE_Z + PIPE_R, PIPE_Y + PIPE_R, 16, RED),
                             (PIPE_Z - 0.9, PIPE_Y - 0.9, PIPE_Z - 0.9, PIPE_Z + 0.9, PIPE_Y + 0.9, PIPE_Z + 0.9, RED)]


def agent_pipe_riser(top=False):
    """The riser up the wall from the manifold (its back to the wall, z 0); at the top, its elbow out into the run
    (towards +z, away from the wall)."""
    x0, x1, z0, z1 = 8.0 - PIPE_R, 8.0 + PIPE_R, 3.0 - PIPE_R, 3.0 + PIPE_R
    if not top:
        return [(x0, 0, z0, x1, 16.0, z1, RED), (6.6, 6.0, 0.2, 9.4, 7.0, z0, "pi_steel_dark")]
    return [(x0, 0, z0, x1, PIPE_Y + PIPE_R, z1, RED), (x0, PIPE_Y - PIPE_R, z1, x1, PIPE_Y + PIPE_R, 16.0, RED),
            (x0 - 0.2, PIPE_Y - PIPE_R - 0.2, z0 - 0.2, x1 + 0.2, PIPE_Y + PIPE_R + 0.2, z1 + 0.2, RED),
            (6.6, 6.0, 0.2, 9.4, 7.0, z0, "pi_steel_dark")]


def agent_manifold(v=0):
    """The header over the cylinders (its back to the wall): its pipe along the row, a hose's coupling down to the
    cylinder under it; v 1 the riser's tee up out of it, v 2 its end capped (at the piece's x 16)."""
    out = [(0, 2.3, 3.0 - PIPE_R, 16 if v != 2 else 14.0, 3.7, 3.0 + PIPE_R, RED),
           (10.0, 0.0, 6.4, 11.6, 1.2, 8.0, "pi_agent_hose"), (10.0, 1.2, 3.0 + PIPE_R, 11.6, 3.4, 8.0, "pi_brass"),
           (4.2, 1.8, 0.2, 5.8, 4.2, 3.0 - PIPE_R, "pi_steel_dark")]
    if v == 1:
        out += [(8.0 - PIPE_R, 3.7, 3.0 - PIPE_R, 8.0 + PIPE_R, 16.0, 3.0 + PIPE_R, RED),
                (8.0 - 0.9, 2.1, 3.0 - 0.9, 8.0 + 0.9, 3.9, 3.0 + 0.9, RED)]
    if v == 2:
        out += [(14.0, 2.1, 3.0 - 0.9, 14.4, 3.9, 3.0 + 0.9, "pi_steel_dark")]
    return out


# ------------------------------------------------------------------------------------------------ the cable ladder
# Over each row (the block over the racks' tops: y 11 of the room) a black ladder rack over the racks' backs, the yellow
# fiber runway over their fronts; a drop of cables off the ladder into each rack's rear cable entry (and a fiber
# spill-out into the network and process computer racks' fronts); threaded rods up to the slab every few racks. At the
# rows' west end the cross ladder joins them, and the riser brings the plant's cables up through the floor into it.
LAD = "pi_ladder"
LAD_Y = 6.0                          # the ladder's rails' bottom
LAD_Z = (1.0, 6.6)                   # over the racks' backs
FIB_Z = (9.0, 14.2)                  # the runway, over their fronts


def _ladder_x(z0, z1, x0=0.0, x1=16.0, cables=True):
    out = [(x0, LAD_Y, z0, x1, LAD_Y + 1.6, z0 + 0.4, LAD), (x0, LAD_Y, z1 - 0.4, x1, LAD_Y + 1.6, z1, LAD)]
    for k in range(5):
        x = x0 + 1.0 + k * (x1 - x0 - 2.0) / 4
        out.append((x - 0.3, LAD_Y + 0.2, z0 + 0.4, x + 0.3, LAD_Y + 0.6, z1 - 0.4, LAD))
    if cables:
        out.append((x0, LAD_Y + 0.6, z0 + 0.6, x1, LAD_Y + 1.5, z1 - 0.6, {"*": "pi_srv_cables", "east": "pi_srv_cables_end",
                                                                          "west": "pi_srv_cables_end"}))
    return out


def _runway_x(z0, z1, x0=0.0, x1=16.0):
    return [(x0, LAD_Y, z0, x1, LAD_Y + 0.4, z1, "pi_fiber_duct"), (x0, LAD_Y + 0.4, z0, x1, LAD_Y + 2.2, z0 + 0.35, "pi_fiber_duct"),
            (x0, LAD_Y + 0.4, z1 - 0.35, x1, LAD_Y + 2.2, z1, "pi_fiber_duct"),
            (x0, LAD_Y + 0.4, z0 + 0.35, x1, LAD_Y + 1.2, z1 - 0.35, {"*": "pi_fiber_cables", "east": "pi_fiber_end", "west": "pi_fiber_end"})]


def _rods(x):
    """A trapeze under the ladder and the runway, its two threaded rods up to the slab (through the block over it)."""
    out = [(x - 0.4, LAD_Y - 0.7, LAD_Z[0] - 0.4, x + 0.4, LAD_Y, FIB_Z[1] + 0.4, "pi_srv_strut")]
    for z in (LAD_Z[0] - 0.3, FIB_Z[1] + 0.3):
        out.append((x - 0.15, LAD_Y - 0.9, z - 0.15, x + 0.15, 32.0, z + 0.15, "pi_srv_rod"))
    return out


def ladder(v):
    """The row's ladder and runway, variant v: bit 0 its hanger (rods), bit 1 a drop into the rack under it, bit 2 a
    fiber spill-out too."""
    out = _ladder_x(*LAD_Z) + _runway_x(*FIB_Z)
    if v & 1:
        out += _rods(8.0)
    if v & 2:
        # The drop: the cables leave down through the rungs, tied, into the rack's rear cable entry below.
        out += [(5.4, -0.1, 1.7, 10.6, LAD_Y + 0.6, 3.1, {"*": "pi_srv_cables", "down": "pi_srv_cables_end", "up": None}),
                (5.2, 2.6, 1.5, 10.8, 3.0, 3.3, "pi_cable_tie"), (5.2, 4.6, 1.5, 10.8, 5.0, 3.3, "pi_cable_tie")]
    if v & 4:
        out += [(7.4, -0.1, 12.9, 8.6, LAD_Y, 14.1, "pi_fiber_tube")]
    return out


LADDER_VARIANTS = 8


def ladder_cross(riser=False):
    """The cross ladder at the rows' west end (along x on the piece, its ladder on its back side against the rows'
    ends); where the riser comes up under it (riser) the cables rise into it from below."""
    out = _ladder_x(*LAD_Z)
    if riser:
        out += [(1.6, -0.1, 0.8, 6.0, LAD_Y + 1.2, 2.2, {"*": "pi_srv_cables", "down": None})]   # up into the bed
    return out


def ladder_riser(floor=False, top=False):
    """The riser up from the floor (fire-stopped where it comes through) to the cross ladder: a ladder on end against
    the room's west side (z 0 its back), the cables tied to its rungs; at the top it turns over into the cross ladder."""
    y1 = LAD_Y + 1.6 if top else 16.0
    out = [(1.0, 0, 0.6, 1.4, y1, 2.2, LAD), (6.2, 0, 0.6, 6.6, y1, 2.2, LAD)]
    for k in range(4):
        y = 1.6 + k * 3.6
        if y < y1 - 0.6:
            out.append((1.4, y, 0.8, 6.2, y + 0.4, 1.2, LAD))
    out.append((1.6, 0, 1.2, 6.0, y1 - (0.1 if top else 0), 2.2, {"*": "pi_srv_cables", "up": None if not top else "pi_srv_cables_end",
                                                                    "down": None}))
    if floor:
        out += [(0.4, 0, 0.2, 7.2, 1.4, 3.0, "pi_firestop")]
    return out


# ------------------------------------------------------------------------------------------------ the console
# The plant process computer's operator console: three blocks wide (the TSC's desk), its three screens live (the plant,
# the alarm summary, the computer room), a keyboard and mouse, the phone, a laser printer at its end.
CON_KEY = "ppc_console"


def ppc_console(label=None):
    import tsc_room as T
    f = PP.Front(3, 859)
    for k, cx in enumerate(CON_MON):
        T._screen_live(f, cx, f"ppc_m{k}")
    return f


CON_MON = (10.0, 24.0, 38.0)


def ppc_console_boxes():
    import tsc_room as T
    out = T._desk(48, [])
    for cx in CON_MON:
        out += T._monitor(cx)
        out += T._stand(cx, 0.5)
    out += T._keyboard(19.5, 8.6)
    out += T._phone(3.0, 9.0)
    out += T._clipboard(31.0, 9.4)
    # The laser printer at the right end: its body, the paper out tray, the control panel.
    out += [(41.0, T.DESK_Y, 8.2, 46.6, T.DESK_Y + 3.6, 14.0, {"*": "pi_printer_body", "up": "pi_srv_printer_top!", "south": "pi_srv_printer_front!"}),
            (42.0, T.DESK_Y + 3.6, 9.0, 45.6, T.DESK_Y + 3.75, 12.4, "pi_paper")]
    return out


# ------------------------------------------------------------------------------------------------ signs
SIGNS = {"sign_srv_row_a": "Row A Sign", "sign_srv_row_b": "Row B Sign", "sign_srv_row_c": "Row C Sign", "sign_srv_row_d": "Row D Sign",
         "sign_no_food": "No Food or Drink Sign", "sign_clean_agent": "Clean Agent Warning Sign", "sign_agent_vacate": "Agent Discharge Sign"}


def sign_tex(kind):
    """The room's signs (433 by 512 in a 512 square, as the plant's signs)."""
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    fit = PP._fit_line
    if kind.startswith("sign_srv_row_"):
        row = kind[-1].upper()
        d.rectangle([0, 0, W - 1, H - 1], fill=(24, 60, 120))
        d.rectangle([14, 14, W - 15, H - 15], outline=(240, 240, 236), width=6)
        fit(d, (W / 2, 120), "ROW", 110, W - 60, (240, 240, 236))
        fit(d, (W / 2, 290), row, 250, W - 60, (250, 250, 246))
        racks = {"A": "A-01 TO A-12", "B": "B-01 TO B-12", "C": "C-01 TO C-12", "D": "D-01 TO D-12"}[row]
        fit(d, (W / 2, 440), racks, 44, W - 60, (240, 240, 236))
    elif kind == "sign_no_food":
        RED = (200, 30, 30)
        d.rectangle([0, 0, W - 1, H - 1], fill=(250, 250, 246), outline=RED, width=14)
        cx, cy, r = W // 2, 180, 120
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=RED, width=22)
        d.rectangle([cx - 40, cy - 50, cx + 34, cy + 60], fill=(40, 40, 42))      # a cup
        d.rectangle([cx + 34, cy - 20, cx + 60, cy + 30], outline=(40, 40, 42), width=10)
        d.line([(cx - r * 0.7, cy - r * 0.7), (cx + r * 0.7, cy + r * 0.7)], fill=RED, width=22)
        fit(d, (W / 2, 360), "NO FOOD", 64, W - 50, (20, 20, 22))
        fit(d, (W / 2, 430), "OR DRINK", 64, W - 50, (20, 20, 22))
    elif kind == "sign_clean_agent":
        d.rectangle([0, 0, W - 1, H - 1], fill=(250, 250, 246), outline=(20, 20, 22), width=8)
        d.rectangle([8, 8, W - 9, 130], fill=(200, 30, 30))
        fit(d, (W / 2, 70), "CAUTION", 80, W - 60, (250, 250, 246))
        for k, s_ in enumerate(("THIS ROOM IS", "PROTECTED BY A", "CLEAN AGENT FIRE", "SUPPRESSION SYSTEM", "DO NOT ENTER", "WHEN ALARM SOUNDS")):
            fit(d, (W / 2, 175 + k * 58), s_, 42, W - 50, (200, 30, 30) if k >= 4 else (20, 20, 22))
    else:
        d.rectangle([0, 0, W - 1, H - 1], fill=(250, 200, 30), outline=(20, 20, 22), width=8)
        d.rectangle([8, 8, W - 9, 130], fill=(20, 20, 22))
        fit(d, (W / 2, 70), "WARNING", 80, W - 60, (250, 200, 30))
        for k, s_ in enumerate(("WHEN ALARM", "SOUNDS LEAVE", "THE ROOM AT ONCE", "AGENT DISCHARGE", "IN 30 SECONDS")):
            fit(d, (W / 2, 185 + k * 64), s_, 46, W - 50, (20, 20, 22))
    return img


# ------------------------------------------------------------------------------------------------ the rest of the textures

def grille_tex(size=256, pitch=10, rgb=(176, 180, 182)):
    img = _img(size, size, rgb)
    d = ImageDraw.Draw(img)
    for x in range(6, size - 6, pitch):
        d.rectangle([x, 6, x + pitch // 2, size - 7], fill=(30, 32, 36))
    d.rectangle([0, 0, size - 1, size - 1], outline=(120, 124, 128), width=6)
    return img


def perf_tile_tex():
    """A perforated access floor tile (25 % open): its grid of square holes, the dark plenum under them, its edge."""
    img = _img(256, 256, (176, 180, 182))
    d = ImageDraw.Draw(img)
    for y in range(20, 236, 10):
        for x in range(20, 236, 10):
            d.rectangle([x, y, x + 5, y + 5], fill=(26, 28, 30))
    d.rectangle([0, 0, 255, 255], outline=(110, 112, 114), width=6)
    return img


def cables_tex(end=False):
    """A bundle of data cables: mostly blue, some grey and white, a few red (along it: a line a cable; its end: their
    jackets' ends)."""
    img = _img(256, 256, (36, 64, 130))
    d = ImageDraw.Draw(img)
    rnd = random.Random(907)
    cols = [(40, 80, 170)] * 6 + [(32, 60, 120)] * 3 + [(120, 124, 132), (200, 202, 206), (170, 50, 44)]
    if end:
        for k in range(900):
            x, y = rnd.randrange(3, 253), rnd.randrange(3, 253)
            d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=rnd.choice(cols), outline=(16, 18, 24))
    else:
        y = 0
        while y < 256:
            h = rnd.randint(2, 4)
            c = rnd.choice(cols)
            d.rectangle([0, y, 255, y + h - 1], fill=c)
            d.line([(0, y + h - 1), (255, y + h - 1)], fill=tuple(int(v * 0.6) for v in c))
            y += h
    return img


def fiber_tex(end=False):
    img = _img(128, 128, (236, 206, 40))
    d = ImageDraw.Draw(img)
    rnd = random.Random(911)
    cols = [(236, 206, 40), (60, 200, 220), (236, 120, 40)]
    if end:
        for k in range(160):
            x, y = rnd.randrange(4, 124), rnd.randrange(4, 124)
            d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=rnd.choice(cols))
    else:
        y = 0
        while y < 128:
            h = rnd.randint(3, 5)
            d.rectangle([0, y, 127, y + h - 1], fill=rnd.choice(cols))
            y += h
    return img


def abort_tex():
    img = _img(256, 320, (30, 80, 170))
    d = ImageDraw.Draw(img)
    PP._fit_line(d, (128, 40), "AGENT ABORT", 40, 220, (250, 250, 246))
    PP._fit_line(d, (128, 285), "HOLD TO ABORT", 36, 220, (250, 250, 246))
    return img


def pull_tex():
    img = _img(256, 320, (240, 200, 30))
    d = ImageDraw.Draw(img)
    PP._fit_line(d, (128, 36), "MANUAL RELEASE", 36, 230, (20, 20, 22))
    PP._fit_line(d, (128, 76), "CLEAN AGENT", 30, 230, (20, 20, 22))
    PP._fit_line(d, (128, 270), "PULL", 56, 230, (200, 30, 30))
    return img


def printer_front():
    img = _img(256, 160, (226, 228, 224))
    d = ImageDraw.Draw(img)
    d.rectangle([20, 100, 236, 150], fill=(190, 192, 188), outline=(150, 150, 150), width=3)
    d.rectangle([150, 20, 230, 60], fill=(40, 44, 50))
    d.rectangle([158, 28, 222, 44], fill=(90, 140, 200))
    return img


def printer_top():
    img = _img(256, 256, (226, 228, 224))
    d = ImageDraw.Draw(img)
    d.rectangle([30, 30, 226, 150], fill=(200, 202, 198), outline=(160, 160, 160), width=3)
    return img


def makers2():
    return {k: _square(v) for k, v in _makers2().items()}


def _makers2():
    solid = PP._solid
    out = {
        "pi_ups_body": lambda: solid((206, 208, 204), 860, 0.02), "pi_ups_top": lambda: grille_tex(256, 12, (200, 202, 198)),
        "pi_ups_plinth": lambda: solid((50, 52, 56), 861, 0.03), "pi_ups_trim": lambda: solid((60, 64, 70), 862, 0.02),
        "pi_epo": lambda: solid((200, 30, 26), 863, 0.02), "pi_srv_bkr_handle": lambda: solid((30, 30, 32), 864, 0.02),
        "pi_isolator": lambda: solid((200, 30, 26), 865, 0.02), "pi_crac_grille": lambda: grille_tex(512, 14, (214, 216, 212)),
        "pi_agent_red": lambda: solid((196, 40, 36), 866, 0.03), "pi_agent_label": _cyl_label,
        "pi_agent_cable": lambda: solid((30, 30, 32), 867, 0.02), "pi_agent_hose": lambda: solid((28, 28, 30), 868, 0.03),
        "pi_srv_strut": lambda: solid((176, 180, 176), 869, 0.05), "pi_srv_rod": lambda: solid((150, 152, 150), 870, 0.05),
        "pi_strobe_lens": lambda: solid((236, 236, 236), 871, 0.01), "pi_horn_grille": lambda: grille_tex(128, 10, (190, 36, 32)),
        "pi_agent_blue": lambda: solid((30, 80, 170), 872, 0.02), "pi_agent_abort": abort_tex, "pi_agent_pull": pull_tex,
        "pi_ladder": lambda: solid((34, 35, 38), 873, 0.04), "pi_srv_cables": cables_tex, "pi_srv_cables_end": lambda: cables_tex(True),
        "pi_fiber_duct": lambda: solid((236, 196, 30), 874, 0.03), "pi_fiber_cables": fiber_tex, "pi_fiber_end": lambda: fiber_tex(True),
        "pi_fiber_tube": lambda: solid((236, 196, 30), 875, 0.05), "pi_srv_printer_top": printer_top, "pi_srv_printer_front": printer_front,
        "access_floor_perforated": perf_tile_tex,
    }
    out.update({f"pi_{k}": (lambda k=k: sign_tex(k)) for k in SIGNS})
    return out


SHARP |= {"pi_agent_label", "pi_agent_abort", "pi_agent_pull", "pi_srv_printer_top", "pi_srv_printer_front", "pi_crac_grille"} | {
    f"pi_{k}" for k in SIGNS}


# ------------------------------------------------------------------------------------------------ the pieces
# The racks' front doors swing open on a click ("swing": their open models, nuclear/ServerRoomPanelBlock.java OPEN).
PANELS = {key: {"display": display, "width": 1, "rows": 2, "item3d": True, "swing": True} for key, (display, _) in RACKS.items()}
PANELS.update({
    UPS_KEY: {"display": "UPS Module", "width": 1, "rows": 2, "item3d": True},
    BATT_KEY: {"display": "UPS Battery Cabinet", "width": 1, "rows": 2, "item3d": True},
    MBP_KEY: {"display": "UPS Maintenance Bypass Panel", "width": 1, "rows": 2, "item3d": True},
    PDU_KEY: {"display": "Power Distribution Unit", "width": 1, "rows": 2, "item3d": True},
    CRAC_KEY: {"display": "Computer Room Air Conditioner", "width": 2, "rows": 2, "item3d": True, "labels": CRAC_LABELS, "shared": True},
    CYL_KEY: {"display": "Clean Agent Cylinder", "width": 1, "rows": 2, "item3d": True},
    PANEL_KEY: {"display": "Clean Agent Releasing Panel", "width": 1, "rows": 2, "item3d": True},
    HORN_KEY: {"display": "Clean Agent Horn and Strobe", "width": 1, "rows": 1, "item3d": True},
    CON_KEY: {"display": "Plant Process Computer Console", "width": 3, "rows": 1, "item3d": True},
})
DRAWINGS = {key: (lambda label=None, key=key: rack_front(key)) for key in RACKS}
DRAWINGS.update({UPS_KEY: ups_module, BATT_KEY: ups_battery, MBP_KEY: ups_bypass, PDU_KEY: pdu_cabinet, CRAC_KEY: crac_unit,
                 CYL_KEY: agent_cylinder, PANEL_KEY: agent_release_panel, HORN_KEY: horn_strobe, CON_KEY: ppc_console})
BOXES = {key: (lambda open_=False, key=key: rack_boxes(key, open_)) for key in RACKS}
BOXES.update({UPS_KEY: ups_module_boxes, BATT_KEY: ups_battery_boxes, MBP_KEY: ups_bypass_boxes, PDU_KEY: pdu_cabinet_boxes,
              CRAC_KEY: crac_unit_boxes, CYL_KEY: agent_cylinder_boxes, PANEL_KEY: agent_release_panel_boxes, HORN_KEY: horn_strobe_boxes,
              CON_KEY: ppc_console_boxes})

PIECES = {
    "agent_abort_station": {"display": "Clean Agent Abort Station", "boxes": abort_station_boxes()},
    "agent_manual_release": {"display": "Clean Agent Manual Release Station", "boxes": manual_release_boxes()},
    "agent_pipe": {"display": "Clean Agent Pipe", "boxes": agent_pipe()},
    "agent_pipe_nozzle": {"display": "Clean Agent Pipe (Nozzle)", "boxes": [], "variants": 2, "vboxes": [agent_pipe_nozzle(), agent_pipe_nozzle(True)]},
    "agent_pipe_elbow": {"display": "Clean Agent Pipe (Elbow)", "boxes": agent_pipe_elbow()},
    "agent_pipe_tee": {"display": "Clean Agent Pipe (Tee)", "boxes": agent_pipe_tee()},
    "agent_pipe_riser": {"display": "Clean Agent Pipe Riser", "boxes": agent_pipe_riser()},
    "agent_pipe_riser_top": {"display": "Clean Agent Pipe Riser (Top)", "boxes": agent_pipe_riser(True)},
    "agent_manifold": {"display": "Clean Agent Manifold", "boxes": [], "variants": 3, "vboxes": [agent_manifold(v) for v in range(3)]},
    "srv_ladder": {"display": "Cable Ladder and Fiber Runway", "boxes": [], "variants": LADDER_VARIANTS,
                   "vboxes": [ladder(v) for v in range(LADDER_VARIANTS)]},
    "srv_ladder_cross": {"display": "Cable Ladder", "boxes": [], "variants": 2, "vboxes": [ladder_cross(), ladder_cross(True)]},
    "srv_ladder_riser": {"display": "Cable Ladder Riser", "boxes": [], "variants": 3,
                         "vboxes": [ladder_riser(), ladder_riser(floor=True), ladder_riser(top=True)]},
}


P_, G_ = "#c:plates/steel", "minecraft:glass_pane"
CRAFTING = {
    "network_rack": (["PCP", "PXP", "PCP"], {"P": P_, "C": "minecraft:copper_ingot", "X": "jugcraft:basic_circuit"}, 2),
    "san_rack": (["PCP", "PXP", "PCP"], {"P": P_, "C": "jugcraft:basic_circuit", "X": "minecraft:iron_ingot"}, 2),
    "ppc_rack": (["PCP", "PXP", "PCP"], {"P": P_, "C": "jugcraft:basic_circuit", "X": "minecraft:comparator"}, 1),
    UPS_KEY: (["PGP", "PCP", "PRP"], {"P": P_, "G": G_, "C": "jugcraft:basic_circuit", "R": "minecraft:redstone_block"}, 1),
    BATT_KEY: (["PRP", "PRP", "PRP"], {"P": P_, "R": "minecraft:redstone"}, 1),
    MBP_KEY: (["PLP", "PLP", "P P"], {"P": P_, "L": "minecraft:lever"}, 1),
    PDU_KEY: (["PCP", "PLP", "PCP"], {"P": P_, "C": "minecraft:copper_ingot", "L": "minecraft:lever"}, 1),
    CRAC_KEY: (["PBP", "PIP", "PCP"], {"P": P_, "B": "minecraft:iron_bars", "I": "minecraft:blue_ice", "C": "jugcraft:basic_circuit"}, 1),
    CYL_KEY: (["RBR", "R R", "RRR"], {"R": "minecraft:red_dye", "B": "minecraft:iron_ingot"}, 1),
    # (item 22: a lever, so it is not the CO2 and pre-action release panels' recipe)
    PANEL_KEY: (["PRP", "LCL", "PVP"], {"P": P_, "R": "minecraft:red_dye", "L": "minecraft:redstone_lamp", "C": "jugcraft:basic_circuit",
                                       "V": "minecraft:lever"}, 1),
    HORN_KEY: (["RLR", "RNR"], {"R": "minecraft:red_dye", "L": "minecraft:redstone_lamp", "N": "minecraft:note_block"}, 1),
    CON_KEY: (["GGG", "SCS", "S S"], {"G": "minecraft:black_stained_glass_pane", "S": "minecraft:smooth_stone_slab", "C": "jugcraft:basic_circuit"}, 1),
    "agent_abort_station": (["BUB"], {"B": "minecraft:blue_dye", "U": "minecraft:stone_button"}, 1),
    "agent_manual_release": (["YLY"], {"Y": "minecraft:yellow_dye", "L": "minecraft:lever"}, 1),
    "agent_pipe": (["RIR"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 4),
    "agent_pipe_nozzle": (["RIR", " N "], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot", "N": "minecraft:iron_nugget"}, 2),
    "agent_pipe_elbow": (["RI", " R"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 2),
    "agent_pipe_tee": (["RIR", " R "], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 2),
    "agent_pipe_riser": (["R", "I", "R"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 4),
    "agent_pipe_riser_top": (["RI", "R "], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}, 2),
    "agent_manifold": (["RIR", "BBB"], {"R": "minecraft:red_dye", "I": "minecraft:iron_ingot", "B": "minecraft:black_wool"}, 2),
    "srv_ladder": (["I I", "BYB", "I I"], {"I": "minecraft:iron_bars", "B": "minecraft:black_dye", "Y": "minecraft:yellow_dye"}, 4),
    "srv_ladder_cross": (["I I", "BBB", "I I"], {"I": "minecraft:iron_bars", "B": "minecraft:black_dye"}, 4),
    "srv_ladder_riser": (["IBI", "IBI", "IBI"], {"I": "minecraft:iron_bars", "B": "minecraft:black_dye"}, 4),
    **{k: (["PCP", "PIP"], {"P": "minecraft:paper", "C": "minecraft:blue_dye" if "row" in k else "minecraft:red_dye", "I": "minecraft:iron_nugget"}, 2)
       for k in SIGNS},
    # (item 22: the three the Drone Tower's data hall is built from, each a recipe of its own)
    "sign_agent_vacate": (["PRP", "PSP"], {"P": "minecraft:paper", "R": "minecraft:red_dye", "S": "minecraft:redstone"}, 2),
    "sign_clean_agent": (["PRP", "PGP"], {"P": "minecraft:paper", "R": "minecraft:red_dye", "G": "minecraft:glass_bottle"}, 2),
    "sign_no_food": (["PRP", "PBP"], {"P": "minecraft:paper", "R": "minecraft:red_dye", "B": "minecraft:bread"}, 2),
}


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], info["rows"], "panel")
        info["panel"] = key
    PP.PANELS.update(PANELS)
    PP.DRAWINGS.update(DRAWINGS)
    PP.CABINETS.update(BOXES)
    PP.RISERS.update(PIECES)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.EXTRA_MAKERS.append(makers2)
    PP.SHARP |= SHARP | {f"pi_{PP.texture_key(key, v)}_f_{p}" for key in PANELS for v in range(PP.variants(key))
                         for p in range(2 * PANELS[key]["width"])}
    old = PP._sign_image

    def draw(kind):
        return sign_tex(kind) if kind in SIGNS else old(kind)
    PP._sign_image = draw
    PP.SIGNS.update(SIGNS)
    PP.RISERS.update({k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})


register()
