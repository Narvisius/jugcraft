"""The administration building brought up to the detail standard, part 1 (Justin: "Administration building"). The
round 18 building was furnished room by room, but its computers showed a painted chart, its windows were plain panes
and it had none of the fire and life safety fittings a real office building has. Here:

- every office computer's screen is live (nuclear/DeskScreenBlockEntity.java, client/DeskScreenRenderer.java): the
  plant's intranet home page, read from the nearest reactor (the unit's state in its colour, its power and output,
  its alarms, the time); the desks keep what else is on them. The meeting tables' laptops show their lock screens;
- the windows are framed glass (Justin, 2026-10-16: every window from now on);
- the life safety fittings: a lit EXIT sign over every stair door and the front doors (exit_sign), a fire alarm pull
  station by each (fire_alarm_pull: pulled, the building's bells ring and everyone near is told), the evacuation plan
  of each floor by the stairs (admin_evac_plan: the floor's rooms, the way out, YOU ARE HERE, the assembly point), a
  smoke detector in every room and along the corridors, emergency lights in the corridors, an AED on each floor.

The building's plan (its rooms, floor by floor) is here (PLAN) so the evacuation plans and tools/medium_admin.py lay out
the same building. Models in model pixels for the piece facing north: x across, y up, z from its back (z 0, the wall)
out to its front."""
from PIL import Image, ImageDraw

import plant_panels as PP
import relay_room as RR
import switchgear_room as SGR

U = PP.U
INK = (24, 24, 26, 255)
WHITE = (246, 246, 240, 255)

# ------------------------------------------------------------------------------------------------ the building's plan
W, D = 60, 26
CORRIDOR = (13, 15)            # the corridor's z (walls at 12 and 16)
NORTH = [(9, 23), (25, 35), (37, 58)]
NORTH_TOP = [(9, 23), (25, 35), (37, 47), (49, 58)]
SOUTH = [(1, 11), (13, 29), (31, 41), (43, 58)]
STAIRS = (1, 1, 8, 12)         # the stairwell: x0, z0, x1, z1 (its doors at x 3..4 in its south wall, z 12)
PLAN = {
    0: {"n": [("lockers", "locker_room"), ("lobby", "lobby"), ("cafeteria", "cafeteria")],
        "s": [("first_aid", "first_aid"), ("meeting", "meeting_room"), ("operations", "operations"), ("hr", "human_resources")]},
    5: {"n": [("lab", "chemistry_lab"), ("open", "open_office"), ("board", "board_room")],
        "s": [("engineering", "engineering"), ("docs", "document_control"), ("shared", "maintenance_planning"), ("break", "break_room")]},
    10: {"n": [("it", "it_support"), ("training", "training_room"), ("manager", "plant_manager"), ("private", "office")],
         "s": [("private", "office"), ("meeting", "meeting_room"), ("private", "office"), ("interns", "intern_area")]},
}
FLOORS = ["GROUND FLOOR", "SECOND FLOOR", "THIRD FLOOR"]
NAMES = {"locker_room": "LOCKERS", "lobby": "LOBBY", "cafeteria": "CAFETERIA", "first_aid": "FIRST AID", "meeting_room": "MEETING",
         "operations": "OPERATIONS", "human_resources": "HUMAN RES.", "chemistry_lab": "CHEM LAB", "open_office": "OPEN OFFICE",
         "board_room": "BOARD ROOM", "engineering": "ENGINEERING", "document_control": "DOC CONTROL", "maintenance_planning": "MAINT PLAN",
         "break_room": "BREAK ROOM", "it_support": "IT", "training_room": "TRAINING", "plant_manager": "MANAGER", "office": "OFFICE",
         "intern_area": "INTERNS"}


def rooms(lv):
    """A floor's rooms: (x0, z0, x1, z1, name)."""
    out = []
    spans_n = NORTH_TOP if lv == 10 else NORTH
    for (x0, x1), job in zip(spans_n, PLAN[lv]["n"]):
        out.append((x0, 1, x1, CORRIDOR[0] - 2, job[1]))
    for (x0, x1), job in zip(SOUTH, PLAN[lv]["s"]):
        out.append((x0, CORRIDOR[1] + 2, x1, D - 2, job[1]))
    return out


# ------------------------------------------------------------------------------------------------ the evacuation plan
EP_KEY = "admin_evac_plan"


def evac_plan(label=FLOORS[0]):
    """A floor's evacuation plan (two blocks across, one high, framed on the wall): the floor drawn north up, its rooms
    named, the corridor and the stairs the way out in green with arrows, YOU ARE HERE by the stairs; the title, the
    legend and the assembly point."""
    lv = (0, 5, 10)[FLOORS.index(label)]
    f = PP.Front(2, 1030 + lv)
    f.R(0, 0, 32, 32, fill=(40, 40, 44, 255))
    f.R(0.9, 1.4, 31.1, 14.6, fill=(250, 250, 246, 255))
    f.R(0.9, 13.2, 31.1, 14.6, fill=(30, 120, 70, 255))
    RR._fit(f, 16.0, 13.9, f"EVACUATION PLAN  -  ADMINISTRATION BUILDING  -  {label}", 8, 29.4, fill=WHITE)
    # The plan: 60 x 26 blocks into x 1.4..25.4, y 2.0..12.6 (north at the top).
    s = 24.0 / W
    X = lambda x: 1.4 + x * s
    Y = lambda z: 12.6 - z * (10.6 / D)
    ink = (40, 40, 44, 255)
    f.R(X(0), Y(D), X(W), Y(0), fill=(232, 232, 226, 255), outline=ink, width=3)
    # The corridor, the way out, in green, with arrows toward the stairs.
    f.R(X(1), Y(CORRIDOR[1] + 1), X(W - 1), Y(CORRIDOR[0]), fill=(200, 236, 206, 255))
    for x in range(14, W - 2, 8):
        cx, cy = X(x), (Y(CORRIDOR[0]) + Y(CORRIDOR[1] + 1)) / 2
        f.d.polygon([f.px(cx - 0.5, cy), f.px(cx + 0.2, cy + 0.32), f.px(cx + 0.2, cy - 0.32)], fill=(30, 140, 70, 255))
    sx0, sz0, sx1, sz1 = STAIRS
    f.R(X(sx0), Y(sz1 + 1), X(sx1 + 1), Y(sz0), fill=(60, 170, 90, 255), outline=ink, width=2)
    RR._fit(f, (X(sx0) + X(sx1 + 1)) / 2, (Y(sz1 + 1) + Y(sz0)) / 2 + 0.3, "STAIRS", 6.5, X(sx1 + 1) - X(sx0) - 0.4, fill=WHITE)
    RR._fit(f, (X(sx0) + X(sx1 + 1)) / 2, (Y(sz1 + 1) + Y(sz0)) / 2 - 0.4, "EXIT", 6.5, X(sx1 + 1) - X(sx0) - 0.4, fill=WHITE)
    for x0, z0, x1, z1, name in rooms(lv):
        f.R(X(x0), Y(z1 + 1), X(x1 + 1), Y(z0), fill=(250, 250, 246, 255), outline=ink, width=2)
        RR._fit(f, (X(x0) + X(x1 + 1)) / 2, (Y(z1 + 1) + Y(z0)) / 2, NAMES[name], 6.5, X(x1 + 1) - X(x0) - 0.3, fill=INK)
    if lv == 0:
        # The front doors out of the lobby to the road.
        f.R(X(29), Y(0) - 0.15, X(31), Y(0) + 0.35, fill=(30, 140, 70, 255))
    # The pull stations by the stairs' doors (and the front doors), the extinguishers along the corridor, the AED.
    marks = [((196, 40, 32), 6, CORRIDOR[0]), ((220, 60, 40), 20, CORRIDOR[1]), ((220, 60, 40), 44, CORRIDOR[1])]
    if lv:
        marks.append(((40, 120, 200), 9, CORRIDOR[1]))
    else:
        marks.append(((196, 40, 32), 29, 1))
    for rgb, x, z in marks:
        cx, cy = X(x + 0.5), Y(z + 0.5)
        f.R(cx - 0.22, cy - 0.22, cx + 0.22, cy + 0.22, fill=rgb + (255,))
    # YOU ARE HERE: in the corridor by the stairs' doors.
    yx, yy = X(5.5), (Y(CORRIDOR[0]) + Y(CORRIDOR[1] + 1)) / 2
    f.E(yx, yy, 0.42, fill=(210, 30, 30, 255))
    # The legend and the assembly point.
    lx = 26.0
    f.R(lx, 2.0, 30.6, 12.6, fill=(244, 244, 238, 255), outline=ink, width=2)
    RR._fit(f, (lx + 30.6) / 2, 11.8, "LEGEND", 7.5, 4.2, fill=INK)
    for k, (rgb, word) in enumerate((((30, 140, 70), "WAY OUT"), ((210, 30, 30), "YOU ARE HERE"), ((196, 40, 32), "PULL STATION"),
                                     ((220, 60, 40), "EXTINGUISHER"), ((40, 120, 200), "AED"))):
        y = 10.6 - k * 1.2
        f.R(lx + 0.4, y - 0.35, lx + 1.1, y + 0.35, fill=rgb + (255,))
        RR._fit(f, lx + 1.3, y, word, 6.5, 3.0, fill=INK, anchor="lm")
    RR._fit(f, (lx + 30.6) / 2, 3.6, "ASSEMBLY POINT:", 6.5, 4.2, fill=INK)
    RR._fit(f, (lx + 30.6) / 2, 2.8, "NORTH CAR PARK", 6.5, 4.2, fill=(176, 30, 30, 255))
    return f


def evac_plan_boxes(open_=False):
    """The plan in its frame on the wall: the board (the drawing on its face), the frame's moulding round it."""
    F = f"pi_{EP_KEY}_f"
    E = "pi_admin_frame"
    return [(0.6, 1.0, 0.0, 31.4, 15.0, 0.6, {"*": E, "south": F}),
            (0.6, 0.6, 0.0, 31.4, 1.0, 0.9, E), (0.6, 15.0, 0.0, 31.4, 15.4, 0.9, E),
            (0.6, 1.0, 0.0, 1.0, 15.0, 0.9, E), (31.0, 1.0, 0.0, 31.4, 15.0, 0.9, E)]


# ------------------------------------------------------------------------------------------------ the room's pieces
def exit_sign():
    """A lit EXIT sign on the wall over a door: its white housing, the face (red letters, the chevrons) lit, the
    mounting canopy against the wall."""
    return [(3.0, 9.0, 0.0, 13.0, 10.0, 1.2, "pi_admin_exit_case"),
            (2.0, 4.0, 0.6, 14.0, 9.0, 2.4, {"*": "pi_admin_exit_case", "south": "pi_admin_exit!", "north": "pi_admin_exit!"})]


def pull_station():
    """A fire alarm pull station: the red box, FIRE across its top, the white pull handle, PULL DOWN under it."""
    return [(5.0, 3.0, 0.0, 11.0, 12.0, 2.2, {"*": "pi_admin_pull", "south": "pi_admin_pull_face!"}),
            (6.4, 7.2, 2.2, 9.6, 8.6, 3.2, "pi_pnl_white"), (6.4, 8.6, 2.2, 7.0, 9.4, 2.8, "pi_pnl_white"),
            (9.0, 8.6, 2.2, 9.6, 9.4, 2.8, "pi_pnl_white")]


PIECES = {"exit_sign": {"display": "Exit Sign", "boxes": exit_sign(), "glow": ["pi_admin_exit"]},
          "fire_alarm_pull": {"display": "Fire Alarm Pull Station", "boxes": pull_station()}}


# ------------------------------------------------------------------------------------------------ textures
def _exit():
    """The EXIT sign's face (its 512 by 213 shape squeezed into the square): red letters lit on white, a chevron each
    side."""
    img = Image.new("RGBA", (512, 213), (250, 248, 240, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 511, 212], outline=(180, 180, 176), width=8)
    SGR._text(d, (256, 108), "EXIT", 150, (220, 30, 24), width=320)
    for side in (-1, 1):
        cx = 256 + side * 200
        pts = [(cx - side * 22, 70), (cx + side * 22, 108), (cx - side * 22, 146)]
        d.line(pts, fill=(220, 30, 24), width=16)
    return img.resize((256, 256), Image.LANCZOS)


def _pull_face():
    img = Image.new("RGBA", (256, 384), (200, 32, 28, 255))
    d = ImageDraw.Draw(img)
    SGR._text(d, (128, 50), "FIRE", 72, (250, 250, 246))
    d.rectangle([40, 140, 216, 230], fill=(170, 24, 20))
    SGR._text(d, (128, 300), "PULL DOWN", 40, (250, 250, 246), width=220)
    return img.resize((256, 256), Image.LANCZOS)


def _office_screen():
    """The office screens' frame (tools/plant_interior.py draws it: pi_office_screen)."""
    import plant_interior
    return plant_interior._office_screen()


def _laptop_lock():
    """A meeting table laptop's lock screen: the plant's photo-blue background, the time, LOCKED and the prompt."""
    img = Image.new("RGBA", (256, 160), (20, 50, 90, 255))
    d = ImageDraw.Draw(img)
    for y in range(160):
        c = (20 + y // 8, 50 + y // 5, 90 + y // 4)
        d.line([(0, y), (255, y)], fill=c)
    SGR._text(d, (128, 52), "08:15", 44, (240, 244, 250))
    SGR._text(d, (128, 98), "LOCKED", 22, (220, 228, 240))
    SGR._text(d, (128, 128), "CTRL + ALT + DEL TO SIGN IN", 14, (200, 210, 226), width=220)
    return img.resize((128, 128), Image.LANCZOS)


def makers():
    solid = PP._solid
    return {"pi_admin_frame": lambda: solid((50, 50, 54), 1040, 0.05), "pi_admin_exit_case": lambda: solid((236, 236, 230), 1041, 0.03),
            "pi_admin_exit": _exit, "pi_admin_pull": lambda: solid((196, 30, 26), 1042, 0.05), "pi_admin_pull_face": _pull_face,
            "pi_laptop_lock": _laptop_lock}


SHARP = {"pi_admin_exit", "pi_admin_pull_face", "pi_laptop_lock"}

# ------------------------------------------------------------------------------------------------ registration
PANELS = {EP_KEY: {"display": "Evacuation Plan (Administration)", "width": 2, "rows": 1, "item3d": True, "labels": FLOORS}}
DRAWINGS = {EP_KEY: evac_plan}
BOXES = {EP_KEY: evac_plan_boxes}
CRAFTING = {
    EP_KEY: (["SPS"], {"S": "minecraft:stick", "P": "minecraft:paper"}, 1),
    "exit_sign": (["RGR"], {"R": "minecraft:red_dye", "G": "minecraft:glowstone_dust"}, 2),
    "fire_alarm_pull": (["RIR", "RBR"], {"R": "minecraft:red_dye", "I": "minecraft:iron_nugget", "B": "minecraft:stone_button"}, 1),
}


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], info.get("rows", 2), "panel")
        info["panel"] = key
    PP.PANELS.update(PANELS)
    PP.DRAWINGS.update(DRAWINGS)
    PP.CABINETS.update(BOXES)
    PP.RISERS.update(PIECES)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.NOT_LIVE.add(EP_KEY)
    PP.SHARP |= SHARP
    PP.SHARP |= {f"pi_{PP.texture_key(EP_KEY, v)}_f_{p}" for v in range(len(FLOORS)) for p in range(2)}


register()
