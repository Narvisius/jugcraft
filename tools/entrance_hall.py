"""The control building's entrance hall brought up to the detail standard (older rooms round 6; Justin: "Lobby and
entrance"). Round 18 made it an airlock vestibule, the security officer's desk and the speed gates (#2, #3: the gates,
the counter, the framed glass), from the office pieces of the time. As a real plant's vital area entrance has it:

- the security post (ac_station, three blocks wide, two high): a bullet-resisting counter facing the hall (its front
  panel, the transaction ledge, ballistic glass to the ceiling in a steel frame, the voice port, the stainless deal
  tray through the counter), and on the officer's side his desk: two screens of the security computer (live: the
  gates' access log, who is inside: nuclear/AccessControl.java), the gate status panel (the entry and exit lanes open,
  the duress alarm), the duress button, the radio in its charger, the phone, the post's log;
- the hand geometry reader on its stand at the entry lane (a click verifies whoever uses it);
- the signs over the vestibule's inner doors (VITAL AREA, prohibited items), the evacuation plan by the gates;
- the room's fittings (pieces the plant has): the card reader at the corridor doors, the AED, the master clock, smoke
  detectors, emergency lights.

Models in model pixels for the piece facing north: x across, y up, z from its back (z 0) out to its front, where its user
is (+z): here the officer, the counter's visitor side at its back."""
from PIL import Image, ImageDraw

import nuclear_materials as M
import plant_panels as PP
import relay_room as RR
import switchgear_room as SGR
import tsc_room as TR

U = PP.U
INK = (24, 24, 26, 255)
WHITE = (246, 246, 240, 255)
BLACK = "pi_cap_black"

ST_KEY = "ac_station"
DESK_Y = 12.2                 # the officer's desk
LEDGE = (15.6, 16.4, 0.0, 4.6)  # the transaction ledge: y0, y1, z0, z1 (the visitors' side)
GLASS_Z = (1.4, 2.2)          # the ballistic glass's thickness
FRAME = "pi_ac_frame"
MON = [(13.0, 4.6), (25.0, 4.6)]   # the security computer's screens: middle, half width
MON_Y = (13.2, 19.4)
SCREEN_Z = 7.3
PANEL = (33.0, 46.6, 12.2, 18.4, 6.2, 7.0)   # the gate status panel: x0, x1, y0, y1, z0, z1 (its face at z1)
LAMPS = [("ENTRY LANE", 35.6, (90, 230, 120), "ac_entry"), ("EXIT LANE", 39.4, (90, 230, 120), "ac_exit"),
         ("DURESS", 43.6, (255, 70, 50), "ac_duress")]
DURESS = (43.6, 13.4)          # the duress button's middle on the panel (x, y)
LAMP_Y, LABEL_Y = 16.4, 15.2
LINES = 6


def _screen(cx, half, tex="pi_ac_screen!"):
    z0 = SCREEN_Z - 0.9
    return [(cx - half, MON_Y[0], z0, cx + half, MON_Y[1], SCREEN_Z - 0.1, "pi_tsc_case"),
            (cx - half + 0.3, MON_Y[0] + 0.3, SCREEN_Z - 0.1, cx + half - 0.3, MON_Y[1] - 0.3, SCREEN_Z, {"*": "pi_tsc_case", "south": tex}),
            (cx - 1.6, DESK_Y, z0 - 1.4, cx + 1.6, DESK_Y + 0.25, z0 + 0.6, "pi_steel_dark"),
            (cx - 0.45, DESK_Y + 0.25, z0 - 0.6, cx + 0.45, MON_Y[0] + 0.8, z0, "pi_steel_dark")]


def station_boxes(open_=False):
    """The security post: the counter's front panel to the hall and its ledge; the glass in its frame over it to the
    top, its voice port and the deal tray; the officer's desk on its pedestal and legs; his screens, the gate panel with
    the duress button, the radio in its charger, the phone, the keyboard, the post log."""
    W = 48
    y0, y1, z0, z1 = LEDGE
    # (Kept a tenth in from the piece's ends and top: the model writer's nudges would take a face at the edge out past
    # the item model's limits.)
    E = 0.1
    out = [(E, 0.0, 0.0, W - E, y0, 1.4, {"*": "pi_ac_panel", "north": "pi_ac_panel"}),
           (E, y0, z0, W - E, y1, z1, {"*": "pi_ac_edge", "up": "pi_ac_ledge", "down": "pi_ac_panel"}),
           # Its stainless kick plate and the badge sign on its face to the hall.
           (E, 0.0, -0.2, W - E, 1.6, 0.0, "pi_steel"),
           (17.0, 8.0, -0.15, 31.0, 12.6, 0.0, {"*": "pi_ac_edge", "north": ("pi_ac_counter_sign", [0, 0, 16, 5.26])})]
    # The glass in its frame: posts at the ends and the blocks' joints, the head rail at the top.
    g0, g1 = GLASS_Z
    out.append((1.4, y1, g0, W - 1.4, 31.0, g1, {"*": "pi_ac_glass", "north": "pi_ac_glass", "south": "pi_ac_glass"}))
    for x in (0.8, 15.6, 31.6, W - 1.4):
        out.append((x, y1, g0 - 0.3, x + (0.8 if 1 < x < W - 2 else 0.6), 31.0, g1 + 0.3, FRAME))
    out.append((0.8, 31.0, g0 - 0.3, W - 0.8, 31.5, g1 + 0.3, FRAME))
    # The voice port in the glass (a round grille) over the deal tray, in the middle block.
    out.append((22.6, 21.0, g0 - 0.15, 25.4, 23.8, g1 + 0.15, {"*": "pi_steel", "north": "pi_ac_port!", "south": "pi_ac_port!"}))
    # The deal tray: a stainless trough down through the ledge under the glass, open to both sides.
    out += [(20.4, y0 - 1.2, -0.4, 27.6, y0 - 1.0, 4.0, "pi_steel"), (20.4, y0 - 1.2, -0.4, 20.6, y1 + 0.2, 4.0, "pi_steel"),
            (27.4, y0 - 1.2, -0.4, 27.6, y1 + 0.2, 4.0, "pi_steel")]
    # The officer's desk: its top, a pedestal of drawers, a leg, the modesty panel joining the counter's front.
    out += [(0.4, DESK_Y - 0.8, 1.4, W - 0.4, DESK_Y, 15.4, {"*": "pi_ac_edge", "up": "pi_ac_desk", "down": BLACK}),
            (0.6, 0.6, 1.4, 10.6, DESK_Y - 0.8, 14.8, "pi_ac_body"), (W - 1.6, 0.6, 12.0, W - 0.6, DESK_Y - 0.8, 14.8, "pi_ac_body")]
    for k, (a, b) in enumerate(((0.9, 4.2), (4.4, 7.7), (7.9, DESK_Y - 1.1))):
        out.append((1.0, a, 14.8, 10.2, b, 15.0, "pi_ac_body"))
        out.append((4.0, b - 0.9, 15.0, 7.2, b - 0.5, 15.35, "pi_chrome"))
    for cx, half in MON:
        out += _screen(cx, half)
    # The gate status panel: a sloped box on the desk, its face (lamps and their legends) the drawing's.
    x0, x1, py0, py1, pz0, pz1 = PANEL
    out.append((x0, py0, pz0 - 1.6, x1, py1, pz1, {"*": "pi_ac_body", "south": f"pi_{ST_KEY}_f"}))
    # The duress button: a red mushroom under a hinged clear guard.
    dx, dy = DURESS
    out += [(dx - 1.0, dy - 1.0, pz1, dx + 1.0, dy + 1.0, pz1 + 0.3, "pi_steel_dark"),
            (dx - 0.7, dy - 0.7, pz1 + 0.3, dx + 0.7, dy + 0.7, pz1 + 1.0, "pi_cap_red"),
            (dx - 1.2, dy - 1.2, pz1 + 1.0, dx + 1.2, dy + 1.2, pz1 + 1.3, {"*": "pi_ac_guard", "south": "pi_ac_guard"})]
    out += TR._keyboard(14.6, 10.6)
    out += TR._phone(2.0, 9.6)
    # The radio in its charging base, its antenna up.
    out += [(29.0, DESK_Y, 9.6, 31.6, DESK_Y + 0.8, 12.0, BLACK), (29.4, DESK_Y + 0.8, 10.2, 31.2, DESK_Y + 5.2, 11.4, {"*": BLACK, "south": "pi_tsc_radio!"}),
            (30.6, DESK_Y + 5.2, 10.5, 30.9, DESK_Y + 7.4, 10.8, BLACK)]
    # The post's log, its pen.
    out += [(36.0, DESK_Y, 9.8, 41.6, DESK_Y + 0.4, 13.8, {"*": "pi_cr_logcover", "up": ("pi_ac_log", [0, 0, 16, 11.43])}),
            (40.4, DESK_Y + 0.4, 10.4, 40.55, DESK_Y + 0.55, 13.0, "pi_pen_blue")]
    return out


def station(label=None):
    """The post's drawing: the gate status panel's face (its lamps' rings and legends; the lamps and the duress lamp
    lit live), the screens' live lines."""
    f = PP.Front(3, 960)
    x0, x1, py0, py1, pz0, pz1 = PANEL
    f.R(x0, py0, x1, py1, fill=(52, 54, 58, 255))
    f.R(x0 + 0.3, py1 - 1.1, x1 - 0.3, py1 - 0.3, fill=WHITE)
    RR._fit(f, (x0 + x1) / 2, py1 - 0.7, "GATE STATUS  /  ALARMS", 7, x1 - x0 - 0.8, fill=INK)
    for name, cx, rgb, ch in LAMPS:
        # Each lamp: its chrome ring, the lens dark (lit live over it).
        f.E(cx, LAMP_Y, 0.78, fill=(196, 198, 200, 255))
        f.E(cx, LAMP_Y, 0.55, fill=tuple(int(c * 0.35) for c in rgb) + (255,))
        f.live.append({"t": "lamp", "x": cx, "y": LAMP_Y, "r": 0.55, "z": round(pz1 + 0.03, 3), "colour": list(rgb), "ch": ch})
        RR._fit(f, cx, LABEL_Y, name, 6.5, 3.6, fill=WHITE)
    dx, dy = DURESS
    RR._fit(f, dx - 3.4, dy, "DURESS", 6.5, 3.0, fill=(255, 200, 190, 255))
    RR._fit(f, dx - 3.4, dy - 0.7, "PUSH", 6.5, 3.0, fill=(255, 200, 190, 255))
    for m, (cx, half) in enumerate(MON):
        top = MON_Y[1] - 0.3 - 0.45
        x, xr = cx - half + 0.7, cx + half - 0.7
        stem = f"ac_m{m}"
        f.text(x, top, SCREEN_Z + 0.01, 0.48, (236, 240, 246), f"{stem}_t")
        f.text(xr, top, SCREEN_Z + 0.01, 0.4, (120, 198, 238), f"{stem}_c", "right")
        for i in range(LINES):
            y = top - 0.95 - i * 0.82
            f.text(x, y, SCREEN_Z + 0.01, 0.44, (168, 176, 188), f"{stem}_l{i}")
            f.text(xr, y, SCREEN_Z + 0.01, 0.44, (236, 240, 246), f"{stem}_r{i}", "right")
    return f


# ------------------------------------------------------------------------------------------------ the room's pieces
def hand_reader():
    """The hand geometry reader on its stand: the weighted base, the post, the reader (its sloped platen with the finger
    pegs, the keypad and display over it), the LED."""
    return [(4.4, 0.0, 4.4, 11.6, 0.6, 11.6, BLACK), (7.2, 0.6, 7.2, 8.8, 15.0, 8.8, "pi_steel"),
            (3.6, 15.0, 3.2, 12.4, 17.4, 12.8, {"*": "pi_ac_reader", "up": "pi_ac_reader_top!"}),
            (4.0, 17.4, 3.4, 12.0, 21.0, 5.0, {"*": "pi_ac_reader", "south": "pi_ac_reader_face!"})]


def evac_part(c, r):
    """The evacuation plan by the gates (two blocks across): its frame and the plan."""
    tex = (f"pi_ac_evac_{c}", [0, 0, 16, 14])
    x0, x1 = (0.4, 16.0) if c == 0 else (0.0, 15.6)
    out = [(x0, 1.0, 0.0, x1, 15.0, 0.6, {"*": "pi_ac_evac_frame", "south": tex}),
           (x0, 0.6, 0.0, x1, 1.0, 0.9, "pi_ac_evac_frame"), (x0, 15.0, 0.0, x1, 15.4, 0.9, "pi_ac_evac_frame")]
    out.append((x0, 0.6, 0.0, x0 + 0.4, 15.4, 0.9, "pi_ac_evac_frame") if c == 0 else (x1 - 0.4, 0.6, 0.0, x1, 15.4, 0.9, "pi_ac_evac_frame"))
    return out


SIGNS = {"sign_vital_area": "Vital Area Sign", "sign_prohibited": "Prohibited Items Sign"}


def sign_tex(kind):
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    if kind == "sign_vital_area":
        d.rectangle([0, 0, W - 1, H - 1], fill=(250, 250, 246))
        d.rectangle([0, 0, W - 1, 150], fill=(176, 30, 30))
        SGR._text(d, (W / 2, 52), "VITAL AREA", 62, (250, 250, 246), width=W - 40)
        SGR._text(d, (W / 2, 116), "CONTROL BUILDING", 36, (250, 250, 246), width=W - 40)
        SGR._text(d, (W / 2, 210), "AUTHORIZED", 50, (24, 24, 26), width=W - 50)
        SGR._text(d, (W / 2, 266), "ACCESS ONLY", 50, (24, 24, 26), width=W - 50)
        SGR._text(d, (W / 2, 340), "BADGE AND HAND READER", 26, (24, 24, 26), width=W - 50)
        SGR._text(d, (W / 2, 376), "AT THE GATE", 26, (24, 24, 26), width=W - 50)
        SGR._text(d, (W / 2, 450), "UNAUTHORIZED ENTRY IS A FEDERAL OFFENSE", 18, (176, 30, 30), width=W - 40)
        d.rectangle([10, 10, W - 11, H - 11], outline=(176, 30, 30), width=8)
    else:
        d.rectangle([0, 0, W - 1, H - 1], fill=(250, 250, 246))
        d.rectangle([0, 0, W - 1, 110], fill=(24, 24, 26))
        SGR._text(d, (W / 2, 56), "PROHIBITED ITEMS", 44, (246, 200, 40), width=W - 40)
        y = 150
        for k, word in enumerate(("FIREARMS", "EXPLOSIVES", "WEAPONS", "CAMERAS", "ALCOHOL / DRUGS")):
            cx, cy = 66, y + k * 70
            d.ellipse([cx - 26, cy - 26, cx + 26, cy + 26], outline=(200, 30, 30), width=8)
            d.line([(cx - 18, cy + 18), (cx + 18, cy - 18)], fill=(200, 30, 30), width=8)
            d.text((112, cy - 16), word, font=PP.rfont(30, True), fill=(24, 24, 26))
        d.rectangle([10, 10, W - 11, H - 11], outline=(24, 24, 26), width=6)
    return img


PIECES = {"ac_hand_reader": {"display": "Hand Geometry Reader", "boxes": hand_reader()}}
MULTI = {"ac_evac_plan": {"display": "Evacuation Plan", "multi": (2, 1, "frame"), "parts": evac_part, "item3d": True}}


# ------------------------------------------------------------------------------------------------ textures
def _f(size, bold=True):
    return PP.rfont(size, bold)


def _counter_sign():
    """The sign on the counter's face to the hall (its top 512 by 168)."""
    img = Image.new("RGBA", (512, 512), (24, 36, 70, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([6, 6, 505, 161], outline=(250, 250, 246), width=4)
    SGR._text(d, (256, 46), "SECURITY", 54, (250, 250, 246), width=480)
    SGR._text(d, (256, 100), "PRESENT YOUR BADGE AT THE WINDOW", 24, (250, 250, 246), width=480)
    SGR._text(d, (256, 136), "VISITORS SIGN IN - ESCORT REQUIRED", 20, (246, 200, 40), width=480)
    return img


def _screen_tex():
    img = Image.new("RGBA", (320, 224), (8, 12, 20, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 319, 26], fill=(26, 52, 40, 255))
    d.rectangle([3, 30, 316, 220], outline=(30, 46, 40, 255), width=2)
    for y in range(54, 220, 26):
        d.line([(8, y), (311, y)], fill=(16, 24, 22, 255), width=1)
    return img.resize((128, 128), Image.LANCZOS)


def _port():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([4, 4, 123, 123], fill=(150, 154, 158, 255), outline=(90, 92, 96, 255), width=6)
    for y in range(24, 108, 14):
        for x in range(24, 108, 14):
            if (x - 64) ** 2 + (y - 64) ** 2 < 46 ** 2:
                d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(30, 30, 32, 255))
    return img


def _reader_top():
    img = Image.new("RGBA", (128, 128), (60, 62, 66, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([14, 30, 113, 120], fill=(40, 42, 46, 255))
    for x in (36, 54, 70, 86):
        d.ellipse([x - 4, 40, x + 4, 48], fill=(200, 200, 196, 255))
    d.ellipse([58, 90, 68, 100], fill=(200, 200, 196, 255))
    d.line([(20, 24), (108, 24)], fill=(30, 130, 210, 255), width=4)
    return img


def _reader_face():
    img = Image.new("RGBA", (128, 128), (70, 72, 76, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([12, 10, 116, 44], fill=(150, 190, 120, 255))
    d.text((18, 16), "PLACE HAND", font=_f(14), fill=(20, 40, 20))
    for k in range(12):
        x, y = 22 + (k % 4) * 22, 54 + (k // 4) * 22
        d.rectangle([x, y, x + 16, y + 16], fill=(200, 200, 196, 255))
    d.ellipse([104, 100, 116, 112], fill=(90, 230, 120, 255))
    return img


def _ac_log():
    """The post's log (its top 256 by 183): the date, the shift's rounds and entries."""
    img = Image.new("RGBA", (256, 256), (244, 242, 230, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([126, 0, 130, 182], fill=(190, 186, 170, 255))
    d.text((10, 6), "SECURITY POST LOG", font=_f(15), fill=(20, 40, 120))
    for page in (0, 1):
        x0 = 8 + page * 128
        for k in range(7):
            y = 28 + k * 21
            d.line([(x0, y + 18), (x0 + 112, y + 18)], fill=(170, 190, 210), width=1)
            if page == 0 or k < 3:
                d.text((x0, y), f"{(6 + k + page * 7):02d}00", font=_f(12, False), fill=(30, 30, 120))
                d.line([(x0 + 38, y + 10), (x0 + 50 + (k * 37 % 60), y + 10)], fill=(40, 40, 140), width=2)
    return img


ROOMS = [  # the ground floor's rooms on the plan (x0, z0, x1, z1, name): medium_plant control_building's
    (1, 1, 9, 8, "BATT 1A"), (11, 1, 19, 13, "DC EQUIP 1A"), (21, 1, 29, 12, "SWGR"), (31, 1, 38, 12, "REMOTE SD"),
    (1, 19, 12, 28, "ENTRANCE"), (14, 19, 17, 28, "LOCKERS"), (19, 19, 28, 28, "DC EQUIP 1B"), (30, 19, 38, 28, "BATT 1B"),
]


def _evac(c):
    """The evacuation plan (two blocks across, its top 1024 by 448): the ground floor's rooms and corridor, YOU ARE
    HERE, the ways out in green, the assembly point; its legend down the right."""
    W, H = 1024, 448
    img = Image.new("RGBA", (W, 512), (250, 250, 246, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, 46], fill=(30, 110, 60))
    SGR._text(d, (W // 2, 23), "EVACUATION PLAN  -  CONTROL BUILDING GRADE", 28, (250, 250, 246), width=W - 40)
    ox, oz, k, kz = 30, 64, 18, 12.2
    d.rectangle([ox, oz, ox + 40 * k, oz + 30 * kz], outline=(24, 24, 26), width=6)
    for x0, z0, x1, z1, name in ROOMS:
        zz0, zz1 = oz + z0 * kz, oz + (z1 + 1) * kz
        d.rectangle([ox + x0 * k, zz0, ox + (x1 + 1) * k, zz1], outline=(24, 24, 26), width=4, fill=(236, 236, 230))
        ly = zz0 + 18 if name == "ENTRANCE" else (zz0 + zz1) / 2
        SGR._text(d, (ox + (x0 + x1 + 1) * k / 2, ly), name, 17, (40, 40, 44), width=(x1 - x0) * k)
    # The corridor and the ways out (to the stair tower and out through the entrance).
    cz = oz + 16 * kz
    d.rectangle([ox + k, cz - 20, ox + 39 * k, cz + 20], fill=(220, 236, 222))
    for x in (ox + 10 * k, ox + 30 * k):
        d.polygon([(x - 26, cz), (x + 26, cz - 14), (x + 26, cz + 14)], fill=(30, 140, 70))
    hx, hz = ox + 6.5 * k, oz + 24.2 * kz
    d.ellipse([hx - 12, hz - 12, hx + 12, hz + 12], fill=(200, 30, 30))
    SGR._text(d, (hx, hz + 26), "YOU ARE HERE", 16, (200, 30, 30))
    d.polygon([(ox + 6.5 * k - 14, oz + 30 * kz - 4), (ox + 6.5 * k + 14, oz + 30 * kz - 4), (ox + 6.5 * k, oz + 30 * kz + 22)], fill=(30, 140, 70))
    # The legend.
    lx = ox + 40 * k + 30
    d.rectangle([lx - 10, oz, W - 14, oz + 30 * kz], outline=(24, 24, 26), width=3)
    SGR._text(d, ((lx + W - 14) / 2, oz + 26), "LEGEND", 24, (24, 24, 26))
    rows = [((30, 140, 70), "WAY OUT"), ((200, 30, 30), "YOU ARE HERE"), ((200, 30, 30), "EXTINGUISHER"), ((240, 200, 40), "PULL STATION")]
    for k2, (col, word) in enumerate(rows):
        y = oz + 70 + k2 * 44
        d.rectangle([lx, y - 10, lx + 22, y + 12], fill=col)
        d.text((lx + 34, y - 10), word, font=_f(17), fill=(24, 24, 26))
    SGR._text(d, ((lx + W - 14) / 2, oz + 30 * kz - 50), "ASSEMBLY POINT:", 16, (24, 24, 26), width=W - lx - 20)
    SGR._text(d, ((lx + W - 14) / 2, oz + 30 * kz - 26), "SECURITY BUILDING", 16, (24, 24, 26), width=W - lx - 20)
    return img.crop((c * 512, 0, c * 512 + 512, 512))


def makers():
    solid = PP._solid
    out = {
        "pi_ac_panel": lambda: solid((120, 124, 130), 970, 0.05), "pi_ac_edge": lambda: solid((60, 62, 66), 971, 0.03),
        "pi_ac_ledge": lambda: solid((200, 202, 204), 972, 0.04), "pi_ac_frame": lambda: solid((70, 74, 78), 973, 0.05),
        "pi_ac_glass": lambda: PP._see_through((190, 214, 222), 46, 974), "pi_ac_desk": lambda: solid((48, 50, 54), 975, 0.03),
        "pi_ac_body": lambda: solid((66, 70, 74), 976, 0.05), "pi_ac_guard": lambda: PP._see_through((230, 236, 240), 70, 977),
        "pi_ac_reader": lambda: solid((80, 84, 90), 978, 0.04), "pi_ac_evac_frame": lambda: solid((40, 40, 44), 979, 0.05),
        "pi_ac_screen": _screen_tex, "pi_ac_port": _port, "pi_ac_reader_top": _reader_top, "pi_ac_reader_face": _reader_face,
        "pi_ac_log": _ac_log, "pi_ac_counter_sign": _counter_sign, "pi_ac_evac_0": lambda: _evac(0), "pi_ac_evac_1": lambda: _evac(1),
    }
    return out


SHARP = {"pi_ac_counter_sign", "pi_ac_log", "pi_ac_evac_0", "pi_ac_evac_1", "pi_ac_reader_face", "pi_ac_screen"}
SEE_THROUGH = {"pi_ac_glass", "pi_ac_guard"}

PANELS = {ST_KEY: {"display": "Security Post", "width": 3, "rows": 2, "item3d": True}}
DRAWINGS = {ST_KEY: station}
BOXES = {ST_KEY: station_boxes}
CRAFTING = {
    ST_KEY: (["GGG", "PCP", "PBP"], {"G": "minecraft:glass", "P": "#c:plates/steel", "C": "jugcraft:basic_circuit", "B": "minecraft:black_dye"}, 1),
    "ac_hand_reader": (["GC", "I "], {"G": "minecraft:glass_pane", "C": "jugcraft:basic_circuit", "I": "minecraft:iron_ingot"}, 1),
    "ac_evac_plan": (["SPS"], {"S": "minecraft:stick", "P": "minecraft:green_dye"}, 1),
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
    PP.SHARP |= {f"pi_{ST_KEY}_f_{p}" for p in range(6)}
    old = PP._sign_image

    def draw(kind):
        return sign_tex(kind) if kind in SIGNS else old(kind)
    PP._sign_image = draw
    PP.SIGNS.update(SIGNS)
    PP.SHARP |= {f"pi_{k}" for k in SIGNS}
    PP.RISERS.update({k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})


register()
