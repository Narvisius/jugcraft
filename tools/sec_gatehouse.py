"""The security gatehouse brought up to the detail standard (Justin: "rework the security post next, the whole building
everything inside of it up to new standards"). The round 18 gatehouse had a reception desk at a pane-glass window,
two one-block CCTV desks, a steel door through a plain wall. As a real plant's primary access point has it:

- the search train in the visitors' hall: the walk-through metal detector (sec_metal_detector, live: it looks at
  what each player walking through carries; anything of metal and it alarms, its zone lights red and it beeps,
  else the green PASS light), the X-ray baggage scanner beside it (sec_xray, live: right-click with an item to put it
  through the tunnel; its operator's screen lists the bags it has seen, each CLEAR or SEARCH, and counts them), then
  the hand geometry reader and the speed gates into the protected area (the control building's: tools/speed_gate.py,
  tools/entrance_hall.py), framed glass from the gates to the wall;
- the bullet-resisting enclosure: the guard room's window onto the hall is the control building's security post
  (ac_station: the gates' access log, the duress button);
- the guard room as the secondary alarm station: the alarm station console (sec_alarm_console, three screens, live:
  the perimeter's eight zones, alarming while a hostile mob is in one; the access log and who is inside, from the
  security post in reach; a duress at the post flashes on every screen), the weapons locker (sec_weapons_locker),
  the officers' lockers, the kitchenette, radios, a clock;
- dome cameras over the hall, the PROTECTED AREA sign, the prohibited items sign, smoke detectors, emergency lights,
  extinguishers, an AED; every window framed glass (Justin, 2026-10-16: all windows from now on).

Models in model pixels for the piece facing north: x across, y up, z from its back (z 0) out to its front, where its user
is (+z)."""
from PIL import Image, ImageDraw

import cr_room as CR
import plant_panels as PP
import relay_room as RR
import switchgear_room as SGR
import tsc_room as TR

U = PP.U
INK = (24, 24, 26, 255)
WHITE = (246, 246, 240, 255)
BLACK = "pi_cap_black"
DESK_Y = CR.DESK_Y
GREEN, AMBER, RED = (90, 230, 120), (255, 190, 60), (255, 70, 50)

# ------------------------------------------------------------------------------------------------ the alarm console
SAS_KEY = "sec_alarm_console"
# Its screens' pages (nuclear/SecurityGatehouse.java SAS_PAGES): a click steps a screen.
SAS_PAGES = ["ALARM ZONES", "ACCESS LOG", "PERSONNEL INSIDE"]
PLATE = CR.PLATE


def alarm_console_boxes(open_=False):
    W = 48
    out = CR._console_body(W)
    for cx in CR.CON_MON:
        out += CR._monitor(cx)
    x0, x1, y0, y1, z0, z1 = PLATE
    out += [(x0, y0, z0, x1, y1, z1, {"*": CR.ALUM, "south": f"pi_{SAS_KEY}_f"}),
            (x0 + 3.6, CR.MON_Y[1], z0 + 0.1, x0 + 4.1, y0, z1 - 0.1, CR.ALUM), (x1 - 4.1, CR.MON_Y[1], z0 + 0.1, x1 - 3.6, y0, z1 - 0.1, CR.ALUM)]
    out += TR._keyboard(19.6, 10.4)
    out += TR._phone(1.6, 5.6)
    out += TR._phone(6.0, 5.8, "pi_cap_red")          # the hot line to the local law enforcement agency
    out += CR._page_mic(31.0, 7.0)                     # the security radio's base station microphone
    out += CR._logbook(36.4, 9.4, "pi_sec_log")
    out += [(33.6, 0.3, 4.4, 37.0, 8.6, 12.6, {"*": "pi_tsc_body", "south": "pi_tsc_pc!"})]
    return out


def alarm_console(label=None):
    f = PP.Front(3, 1010)
    x0, x1, y0, y1, _, _ = PLATE
    f.R(x0, y0, x1, y1, fill=(214, 216, 214, 255), outline=(150, 152, 150, 255), width=3)
    RR._fit(f, (x0 + x1) / 2, (y0 + y1) / 2, "SECONDARY ALARM STATION", 9, x1 - x0 - 1.2, fill=INK)
    for m, cx in enumerate(CR.CON_MON):
        CR._screen_live(f, cx, f"sas_m{m}")
    return f


# ------------------------------------------------------------------------------------------------ the X-ray scanner
XR_KEY = "sec_xray"
XR_BODY = (12.0, 36.0)          # the tunnel housing across the piece (the belt runs from x 48 to x 0)
XR_MON = (0.6, 10.6, 21.0, 29.0)  # the operator's monitor: x0, x1, y0, y1 (its face at z 15.3)
XR_SCREEN_Z = 15.3
XR_LINES = 5
XR = "pi_sec_xray"


def xray_boxes(open_=False):
    """The X-ray baggage scanner: the infeed belt (a tote tray on it), the tunnel housing (lead curtains at both
    mouths, its belt through it, the operator's panel and the radiation label on its side), the roller bed out, the
    operator's monitor on its post at the out end."""
    F = f"pi_{XR_KEY}_f"
    a, b = XR_BODY
    out = [(a, 0.0, 1.0, b, 6.0, 15.0, {"*": XR, "south": F}),
           (a, 6.0, 1.0, b, 18.0, 3.0, XR), (a, 6.0, 13.0, b, 18.0, 15.0, {"*": XR, "south": F}),
           (a, 18.0, 1.0, b, 22.0, 15.0, {"*": XR, "up": "pi_sec_xray_top", "south": F}),
           (a, 6.0, 3.0, b, 6.6, 13.0, {"*": BLACK, "up": "pi_sec_belt"})]
    # The lead curtains at both mouths: rubber strips hanging from the top.
    for x in (a + 0.2, b - 0.5):
        for k in range(5):
            z0 = 3.1 + k * 2.0
            out.append((x, 7.0, z0, x + 0.3, 18.0, z0 + 1.85, "pi_sec_curtain"))
    # The infeed belt, its rails and legs, a grey tote tray on it.
    out += [(b, 5.4, 2.0, 47.8, 6.6, 14.0, {"*": XR, "up": "pi_sec_belt"}),
            (b, 6.6, 2.0, 47.8, 7.4, 2.6, "pi_steel"), (b, 6.6, 13.4, 47.8, 7.4, 14.0, "pi_steel"),
            (39.0, 6.6, 4.0, 46.0, 8.4, 12.0, {"*": "pi_sec_tray", "up": "pi_sec_tray_in"})]
    for x in (37.0, 46.0):
        for z in (2.6, 12.4):
            out.append((x, 0.0, z, x + 1.0, 5.4, z + 1.0, "pi_steel"))
    # The roller bed out: its rails, the rollers, the end stop, its legs.
    out += [(0.2, 5.4, 2.0, a, 7.4, 2.6, "pi_steel"), (0.2, 5.4, 13.4, a, 7.4, 14.0, "pi_steel"),
            (0.2, 7.4, 2.0, 0.8, 8.4, 14.0, "pi_steel")]
    for k in range(6):
        x = 1.2 + k * 1.75
        out.append((x, 6.0, 2.6, x + 1.1, 7.0, 13.4, "pi_chrome"))
    for x in (1.0, 10.0):
        for z in (2.6, 12.4):
            out.append((x, 0.0, z, x + 1.0, 5.4, z + 1.0, "pi_steel"))
    # The operator's monitor on its post at the out end.
    x0, x1, y0, y1 = XR_MON
    out += [(5.2, 7.4, 13.6, 6.0, y0, 14.4, "pi_steel_dark"),
            (x0, y0, 13.2, x1, y1, XR_SCREEN_Z - 0.1, "pi_tsc_case"),
            (x0 + 0.3, y0 + 0.3, XR_SCREEN_Z - 0.1, x1 - 0.3, y1 - 0.3, XR_SCREEN_Z, {"*": "pi_tsc_case", "south": "pi_cr_screen!"})]
    # The operator's panel on the housing's side: the E-stop, the key switch (proud of the face).
    out += [(29.4, 10.0, 15.0, 31.2, 11.8, 15.6, "pi_cap_red"), (32.4, 10.4, 15.0, 33.4, 11.4, 15.4, "pi_chrome")]
    return out


def xray(label=None):
    """The scanner's drawing: the housing's side (its body colour, the CAUTION X-RAY label, the operator's panel with
    its lamps), the screen's live lines."""
    f = PP.Front(3, 1011)
    f.R(0, 0, 48, 32, fill=(196, 204, 212, 255))
    a, b = XR_BODY
    f.R(a, 5.8, b, 6.2, fill=(80, 86, 96, 255))
    # The radiation label: yellow, the trefoil, CAUTION X-RAY.
    f.R(14.0, 8.4, 23.0, 17.0, fill=(246, 206, 40, 255), outline=(30, 30, 32, 255), width=3)
    _trefoil(f, 16.4, 12.7, 1.6)
    f.T(20.4, 14.2, "CAUTION", 8, bold=True)
    f.T(20.4, 12.6, "X-RAY", 8, bold=True)
    f.T(20.4, 10.4, "KEEP HANDS", 6.5, bold=True)
    f.T(20.4, 9.6, "OUT", 6.5, bold=True)
    # The operator's panel: X-RAY ON (amber, lit while a bag is in the tunnel), BELT (green), the E-stop, the key.
    f.R(24.6, 8.6, 35.0, 17.0, fill=(60, 64, 70, 255))
    f.lamp(26.4, 14.6, AMBER, False, r=0.6, live="xr_on")
    f.T(26.4, 13.2, "X-RAY ON", 6.5, bold=True, fill=WHITE)
    f.lamp(29.6, 14.6, GREEN, False, r=0.6, live="xr_belt")
    f.T(29.6, 13.2, "BELT", 6.5, bold=True, fill=WHITE)
    f.T(30.3, 9.4, "E-STOP", 6.5, bold=True, fill=WHITE)
    f.T(32.9, 9.4, "KEY", 6.5, bold=True, fill=WHITE)
    # The screen's live lines.
    x0, x1, y0, y1 = XR_MON
    top = y1 - 0.3 - 0.5
    xl, xr = x0 + 0.75, x1 - 0.75
    f.text(xl, top, XR_SCREEN_Z + 0.01, 0.5, (236, 240, 246), "xr_t")
    f.text(xr, top, XR_SCREEN_Z + 0.01, 0.42, (120, 198, 238), "xr_c", "right")
    for i in range(XR_LINES):
        y = top - 1.0 - i * 1.0
        f.text(xl, y, XR_SCREEN_Z + 0.01, 0.46, (168, 176, 188), f"xr_l{i}")
        f.text(xr, y, XR_SCREEN_Z + 0.01, 0.46, (236, 240, 246), f"xr_r{i}", "right")
    return f


def _trefoil(f, cx, cy, r):
    import math
    f.E(cx, cy, r * 1.05, fill=(30, 30, 32, 255))
    f.E(cx, cy, r * 0.98, fill=(246, 206, 40, 255))
    pts = 24
    for k in range(3):
        a0 = math.radians(90 + k * 120 - 30)
        a1 = math.radians(90 + k * 120 + 30)
        poly = [(cx + math.cos(a0 + (a1 - a0) * i / pts) * r * 0.9, cy + math.sin(a0 + (a1 - a0) * i / pts) * r * 0.9) for i in range(pts + 1)]
        poly += [(cx + math.cos(a1 - (a1 - a0) * i / pts) * r * 0.28, cy + math.sin(a1 - (a1 - a0) * i / pts) * r * 0.28) for i in range(pts + 1)]
        f.d.polygon([f.px(x, y) for x, y in poly], fill=(30, 30, 32, 255))
    f.E(cx, cy, r * 0.16, fill=(30, 30, 32, 255))


# ------------------------------------------------------------------------------------------------ the metal detector
WD_KEY = "sec_metal_detector"
WD_POSTS = ((0.4, 2.4), (13.6, 15.6))
WD_Z = (3.5, 12.5)              # the posts' depth; their front faces at z 12.5
WD_HEAD = (29.0, 32.0, 3.0, 13.0)  # the head: y0, y1, z0, z1 (its face at z 13)
WD_LEDS = [3.0 + k * 3.2 for k in range(8)]   # the zone lights up each post's front


def detector_boxes(open_=False):
    """The walk-through metal detector: two slim posts (their zone lights up their fronts), the head across their tops
    with the PASS and ALARM lights, the feet; nothing between the posts (you walk through)."""
    F = f"pi_{WD_KEY}_f"
    z0, z1 = WD_Z
    hy0, hy1, hz0, hz1 = WD_HEAD
    out = []
    for x0, x1 in WD_POSTS:
        out += [(x0, 0.5, z0, x1, hy0, z1, {"*": "pi_sec_wtmd", "south": F, "north": "pi_sec_wtmd_back"}),
                (x0 - 0.3 if x0 > 1 else 0.1, 0.0, z0 - 1.0, x1 + 0.3 if x1 < 15 else 15.9, 0.5, z1 + 1.0, "pi_cap_black")]
    out.append((0.4, hy0, hz0, 15.6, hy1 - 0.1, hz1, {"*": "pi_sec_wtmd", "south": F, "north": "pi_sec_wtmd_back"}))
    return out


def detector(label=None):
    """Its drawing (1 block by 2): the posts' fronts with their zone lights (red, lit live while it alarms), the head's
    face with PASS (green) and ALARM (red), lit live."""
    f = PP.Front(1, 1012)
    f.R(0, 0, 16, 32, fill=(214, 210, 198, 255))
    z1 = WD_Z[1]
    for x0, x1 in WD_POSTS:
        cx = (x0 + x1) / 2
        f.R(x0 + 0.3, 1.6, x1 - 0.3, 27.6, fill=(40, 40, 44, 255))
        for y in WD_LEDS:
            f.E(cx, y, 0.42, fill=(70, 20, 18, 255))
            f.live.append({"t": "lamp", "x": cx, "y": y, "r": 0.42, "z": round(z1 + 0.03, 3), "colour": list(RED), "ch": "wd_alarm"})
    hy0, hy1, hz0, hz1 = WD_HEAD
    cy = (hy0 + hy1 - 0.1) / 2
    f.R(0.6, hy0 + 0.25, 15.4, hy1 - 0.35, fill=(36, 36, 40, 255))
    for cx, col, ch, word in ((3.2, GREEN, "wd_pass", "PASS"), (9.4, RED, "wd_alarm", "ALARM")):
        f.E(cx, cy, 0.95, fill=(150, 150, 152, 255))
        f.E(cx, cy, 0.75, fill=tuple(int(c * 0.35) for c in col) + (255,))
        f.live.append({"t": "lamp", "x": cx, "y": cy, "r": 0.75, "z": round(hz1 + 0.03, 3), "colour": list(col), "ch": ch})
        f.T(cx + 2.4, cy, word, 6.5, bold=True, fill=WHITE)
    return f


# ------------------------------------------------------------------------------------------------ the room's pieces
def locker_part(c, r):
    """The weapons locker (two blocks high, against the wall): its steel case, the expanded-metal door, behind it the
    duty rifles in their rack and the ammunition cans on the shelf, its stencilled sign, the handle and padlock."""
    L = "pi_sec_locker"
    # (Its top kept under the piece's: the model writer's nudges would take a face at the top out past the item model's
    # limits.)
    top = 16.0 if r == 0 else 15.6
    out = [(1.0, 0.0, 1.0, 15.0, top, 1.6, L), (1.0, 0.0, 1.6, 1.8, top, 12.0, L), (14.2, 0.0, 1.6, 15.0, top, 12.0, L),
           (1.8, 0.0, 11.4, 14.2, top, 11.8, {"*": "pi_sec_mesh", "south": "pi_sec_mesh", "north": "pi_sec_mesh"})]
    if r == 0:
        out += [(1.8, 0.0, 1.6, 14.2, 1.2, 11.4, L),
                # The rifle rack's foot (a slotted block) and the rifles standing in it: butt, stock, receiver.
                (2.4, 1.2, 3.0, 13.6, 2.4, 8.0, L)]
        for cx in (4.0, 7.0, 10.0, 13.0):
            out += [(cx - 0.6, 1.6, 4.2, cx + 0.6, 6.0, 7.6, "pi_sec_rifle"), (cx - 0.35, 6.0, 4.8, cx + 0.35, 16.0, 6.6, "pi_sec_rifle")]
        out.append((12.6, 6.0, 11.8, 13.2, 9.0, 12.4, "pi_chrome"))
    else:
        for cx in (4.0, 7.0, 10.0, 13.0):
            out += [(cx - 0.35, 0.0, 4.8, cx + 0.35, 4.0, 6.6, "pi_sec_rifle"), (cx - 0.15, 4.0, 5.4, cx + 0.15, 7.6, 5.8, "pi_sec_rifle")]
        # The rifles' top rack bar, the shelf, the ammunition cans on it.
        out += [(1.8, 5.0, 3.6, 14.2, 5.6, 4.4, "pi_chrome"), (1.8, 8.0, 1.6, 14.2, 8.5, 11.4, L),
                (2.6, 8.5, 3.0, 7.4, 11.8, 9.0, {"*": "pi_sec_ammo", "up": "pi_sec_ammo_lid"}),
                (8.4, 8.5, 3.0, 13.2, 11.8, 9.0, {"*": "pi_sec_ammo", "up": "pi_sec_ammo_lid"}),
                (1.0, 14.8, 1.6, 15.0, 15.6, 12.0, L),
                # Its sign over the door; the hasp and the padlock.
                (2.0, 12.2, 11.8, 14.0, 14.8, 12.0, {"*": L, "south": ("pi_sec_weapons_sign", [0, 0, 16, 3.47])})]
    return out


def dome_camera():
    """A ceiling dome camera: its white base ring on the ceiling, the smoked dome under it."""
    # (Each step at least 0.7 px high: plant_panels.round_y's own steps.)
    out = PP.round_y(8.0, 8.0, 3.2, 15.0, 16.0, "pi_pnl_white")
    out += PP.round_y(8.0, 8.0, 2.6, 13.8, 15.0, "pi_sec_dome")
    out += PP.round_y(8.0, 8.0, 1.7, 12.9, 13.8, "pi_sec_dome")
    return out


SIGNS = {"sign_protected_area": "Protected Area Sign"}


def sign_tex(kind):
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, H - 1], fill=(250, 250, 246))
    d.rectangle([0, 0, W - 1, 150], fill=(176, 30, 30))
    SGR._text(d, (W / 2, 52), "PROTECTED", 60, (250, 250, 246), width=W - 40)
    SGR._text(d, (W / 2, 112), "AREA", 60, (250, 250, 246), width=W - 40)
    SGR._text(d, (W / 2, 196), "AUTHORIZED", 44, (24, 24, 26), width=W - 50)
    SGR._text(d, (W / 2, 246), "PERSONNEL ONLY", 44, (24, 24, 26), width=W - 50)
    d.rectangle([24, 290, W - 25, 300], fill=(176, 30, 30))
    SGR._text(d, (W / 2, 338), "ALL PERSONS, PACKAGES", 24, (24, 24, 26), width=W - 50)
    SGR._text(d, (W / 2, 370), "AND VEHICLES ARE", 24, (24, 24, 26), width=W - 50)
    SGR._text(d, (W / 2, 402), "SUBJECT TO SEARCH", 24, (24, 24, 26), width=W - 50)
    SGR._text(d, (W / 2, 458), "10 CFR 73", 22, (176, 30, 30), width=W - 50)
    d.rectangle([10, 10, W - 11, H - 11], outline=(176, 30, 30), width=8)
    return img


PIECES = {"sec_dome_camera": {"display": "Dome Camera", "boxes": dome_camera()}}
MULTI = {"sec_weapons_locker": {"display": "Weapons Locker", "multi": (1, 2, "frame"), "parts": locker_part, "item3d": True}}


# ------------------------------------------------------------------------------------------------ textures
def _f(size, bold=True):
    return PP.rfont(size, bold)


def _belt():
    img = Image.new("RGBA", (64, 64), (34, 34, 36, 255))
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 8):
        d.line([(0, y), (63, y)], fill=(22, 22, 24), width=2)
    return img


def _curtain():
    img = Image.new("RGBA", (64, 64), (52, 54, 58, 255))
    d = ImageDraw.Draw(img)
    for x in range(0, 64, 16):
        d.line([(x, 0), (x, 63)], fill=(38, 40, 44), width=2)
    return img


def _tray_in():
    img = Image.new("RGBA", (64, 64), (120, 124, 128, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 59, 59], fill=(96, 100, 104))
    return img


def _mesh():
    """Expanded metal: a diamond lattice of grey steel, open between."""
    S = 128
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    step = 16
    for k in range(-S, 2 * S, step):
        d.line([(k, 0), (k + S, S)], fill=(150, 154, 158, 255), width=3)
        d.line([(k, S), (k + S, 0)], fill=(150, 154, 158, 255), width=3)
    return img


def _weapons_sign():
    """The weapons locker's sign (its top 512 by 111: the plate's shape)."""
    img = Image.new("RGBA", (512, 512), (232, 196, 40, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([6, 6, 505, 104], outline=(24, 24, 26), width=5)
    SGR._text(d, (256, 40), "WEAPONS", 50, (24, 24, 26), width=470)
    SGR._text(d, (256, 84), "AUTHORIZED OFFICERS ONLY", 26, (24, 24, 26), width=470)
    return img


def _ammo_lid():
    img = Image.new("RGBA", (64, 64), (86, 92, 60, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([24, 20, 40, 44], fill=(60, 64, 40))
    return img


def makers():
    solid = PP._solid
    return {
        "pi_sec_wtmd": lambda: solid((214, 210, 198), 1020, 0.04), "pi_sec_wtmd_back": lambda: solid((200, 196, 186), 1021, 0.04),
        "pi_sec_xray": lambda: solid((196, 204, 212), 1022, 0.04), "pi_sec_xray_top": lambda: solid((176, 184, 192), 1023, 0.05),
        "pi_sec_belt": _belt, "pi_sec_curtain": _curtain, "pi_sec_tray": lambda: solid((120, 124, 128), 1024, 0.04),
        "pi_sec_tray_in": _tray_in, "pi_sec_locker": lambda: solid((96, 104, 98), 1025, 0.06), "pi_sec_mesh": _mesh,
        "pi_sec_rifle": lambda: solid((34, 34, 36), 1026, 0.05), "pi_sec_ammo": lambda: solid((86, 92, 60), 1027, 0.08),
        "pi_sec_ammo_lid": _ammo_lid, "pi_sec_weapons_sign": _weapons_sign, "pi_sec_dome": lambda: solid((40, 42, 46), 1028, 0.02),
        "pi_sec_log": lambda: CR._log_tex("SECURITY LOG"),
    }


SHARP = {"pi_sec_weapons_sign", "pi_sec_log"}
SEE_THROUGH = {"pi_sec_mesh"}

# ------------------------------------------------------------------------------------------------ registration
PANELS = {SAS_KEY: {"display": "Alarm Station Console", "width": 3, "rows": 2, "item3d": True},
          XR_KEY: {"display": "X-Ray Baggage Scanner", "width": 3, "rows": 2, "item3d": True},
          WD_KEY: {"display": "Walk-Through Metal Detector", "width": 1, "rows": 2, "item3d": True}}
DRAWINGS = {SAS_KEY: alarm_console, XR_KEY: xray, WD_KEY: detector}
BOXES = {SAS_KEY: alarm_console_boxes, XR_KEY: xray_boxes, WD_KEY: detector_boxes}
STEEL = {"P": "#c:plates/steel", "C": "jugcraft:basic_circuit"}
CRAFTING = {
    # (item 22: a note block for its alarm, so it is not the control room console's recipe)
    SAS_KEY: (["GGG", "PCP", "PBP"], {**STEEL, "G": "minecraft:black_stained_glass_pane", "B": "minecraft:note_block"}, 1),
    XR_KEY: (["PGP", "CBC", "PPP"], {**STEEL, "B": "minecraft:iron_block", "G": "minecraft:black_stained_glass_pane"}, 1),
    WD_KEY: (["PCP", "P P", "P P"], {**STEEL, "C": "minecraft:copper_ingot"}, 1),
    "sec_weapons_locker": (["PIP", "PBP", "PIP"], {"P": "#c:plates/steel", "I": "minecraft:iron_bars", "B": "minecraft:barrel"}, 1),
    "sec_dome_camera": (["PGP"], {"P": "#c:plates/steel", "G": "minecraft:tinted_glass"}, 2),
    **{k: (["PCP", "PIP"], {"P": "minecraft:paper", "C": "minecraft:red_dye", "I": "minecraft:iron_nugget"}, 2) for k in SIGNS},
}


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], info.get("rows", 2), "panel")
        info["panel"] = key
    PP.PANELS.update(PANELS)
    PP.DRAWINGS.update(DRAWINGS)
    PP.CABINETS.update(BOXES)
    PP.RISERS.update(PIECES)
    PP.RISERS.update(MULTI)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.SEE_THROUGH |= SEE_THROUGH
    PP.SHARP |= SHARP
    PP.SHARP |= {f"pi_{key}_f_{p}" for key in PANELS for p in range(PANELS[key]["width"] * 2)}
    old = PP._sign_image

    def draw(kind):
        return sign_tex(kind) if kind in SIGNS else old(kind)
    PP._sign_image = draw
    PP.SIGNS.update(SIGNS)
    PP.SHARP |= {f"pi_{k}" for k in SIGNS}
    PP.RISERS.update({k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})


register()
