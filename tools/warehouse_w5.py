"""The automated warehouse, part W5 (fleet and control; claude/jugcraft-warehouse-plan.md).

How it works: the warehouse runs on power brought in by cable. The Warehouse Switchboard stands inside an outer wall,
its back in the wall: outside, the service entrance (the meter cabinet with its live meter, the main switch, the
terminal box the incoming cable is glanded into, its conduit down into the ground); inside, the main distribution
board (the incomer with its air circuit breaker and power analyzer, six outgoing breakers with their lamps: DOCKS,
LIFTS, STATIONS, READY STORAGE, LIQUIDS, LIGHTS & CONTROL, and the UPS section that keeps the terminal and the screens
up). The Warehouse Status Board is a big display on a wall showing the live floor map, and the Warehouse Signal Box is
a small remote I/O box that turns the warehouse's state into redstone (or redstone into an action).

Every piece is drawn as tools/warehouse.py draws them: front to +z (toward its user), x across, y up."""
from PIL import Image, ImageDraw

import plant_panels as PP
import warehouse as W
from warehouse import box, round_y, round_z, rot_x

GRAPHITE, STEEL, CHROME, BLACK, YELLOW = W.GRAPHITE, W.STEEL, W.CHROME, W.BLACK, W.YELLOW
PRECAST, PRECAST_LOW, FLASH_D = "ex_precast", "ex_precast_low", "ex_flash_anthracite"
CAB = "pi_wh5_cab"            # the switchboard's light grey powder-coated steel (RAL 7035)
CAB_DARK = "pi_wh5_cab_dark"  # its plinth and roof trim
GRP = "pi_wh5_grp"            # the meter cabinet's grey glass-fibre
CONDUIT = "pi_wh5_conduit"
RED = "pi_wh_spk_red"
LAMP = {"on": "pi_wh5_led_green", "shed": "pi_wh5_led_amber", "off": "pi_wh5_led_red"}
BREAKERS = ("DOCKS", "LIFTS", "STATIONS", "READY STR", "LIQUIDS", "LIGHTS CTRL")


# ================================================================================================ textures
def _t(d, xy, s, size, fill, anchor="mm", bold=True):
    W._t(d, xy, s, size, fill, anchor=anchor, bold=bold)


def _cab():
    img = PP._solid((200, 202, 198), 5101, 0.035).convert("RGBA").resize((64, 64))
    return img


def _cab_dark():
    return PP._solid((70, 74, 78), 5103, 0.04).convert("RGBA").resize((64, 64))


def _grp():
    img = PP._solid((150, 154, 150), 5105, 0.05).convert("RGBA").resize((64, 64))
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 4):
        d.line([(0, y), (63, y)], fill=(144, 148, 144, 255))
    return img


def _conduit():
    img = PP._solid((52, 56, 60), 5107, 0.05).convert("RGBA").resize((32, 32))
    return img


def _danger(d, cx, cy, s):
    """The electrical danger sticker: a yellow triangle edged black with the lightning flash."""
    d.polygon([(cx, cy - s), (cx + s * 1.1, cy + s * 0.8), (cx - s * 1.1, cy + s * 0.8)], fill=(250, 210, 20), outline=(20, 20, 22))
    d.polygon([(cx, cy - s * 0.78), (cx + s * 0.86, cy + s * 0.66), (cx - s * 0.86, cy + s * 0.66)], outline=(20, 20, 22))
    d.polygon([(cx + s * 0.1, cy - s * 0.45), (cx - s * 0.22, cy + s * 0.12), (cx, cy + s * 0.1), (cx - s * 0.12, cy + s * 0.55),
               (cx + s * 0.24, cy - s * 0.05), (cx + s * 0.02, cy - s * 0.04)], fill=(20, 20, 22))


def _door(kind):
    """A switchboard section's door (256 x 384 for 16 x 28.4 px): its edge and hinge side, the danger sticker, the
    section's label, the louvres (control) or the cut-out round the breaker (incomer)."""
    img = PP._solid((200, 202, 198), 5111 + len(kind), 0.03).convert("RGBA").resize((256, 448))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 253, 445], outline=(150, 152, 150, 255), width=4)
    d.rectangle([10, 10, 245, 437], outline=(184, 186, 184, 255), width=2)
    for y in (40, 400):
        d.rectangle([2, y, 10, y + 26], fill=(120, 122, 124, 255))
    _danger(d, 128, 46, 22)
    if kind == "ctl":
        for y in range(318, 396, 12):
            d.rectangle([60, y, 196, y + 6], fill=(150, 152, 150, 255))
            d.line([(60, y + 6), (196, y + 6)], fill=(230, 232, 230, 255), width=2)
    return img


def _acb():
    """The incomer's air circuit breaker seen from the front: its frame, the ON (green) and OFF (red) buttons, the
    charged and closed flags, the trip unit with its little display, the rating plate."""
    img = Image.new("RGBA", (160, 160), (34, 36, 40, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 155, 155], outline=(70, 74, 80, 255), width=4)
    d.rectangle([16, 18, 90, 70], fill=(22, 24, 26, 255), outline=(90, 94, 100, 255))
    d.rectangle([24, 28, 82, 48], fill=(40, 70, 52, 255))
    _t(d, (53, 38), "I 412A", 12, (130, 250, 160))
    _t(d, (53, 60), "TRIP UNIT", 8, (170, 176, 182), bold=False)
    d.ellipse([104, 22, 132, 50], fill=(30, 150, 70, 255), outline=(10, 10, 10, 255), width=2)
    _t(d, (118, 36), "I", 14, (240, 250, 240))
    d.ellipse([104, 58, 132, 86], fill=(190, 40, 34, 255), outline=(10, 10, 10, 255), width=2)
    _t(d, (118, 72), "O", 14, (250, 240, 240))
    for k, (s, col) in enumerate((("CLOSED", (30, 150, 70)), ("CHARGED", (240, 200, 40)))):
        y = 96 + k * 22
        d.rectangle([20, y, 72, y + 16], fill=col + (255,))
        _t(d, (46, y + 8), s, 8, (20, 20, 22))
    d.rectangle([90, 100, 146, 140], fill=(200, 202, 198, 255))
    _t(d, (118, 112), "1250 A", 10, (20, 20, 22))
    _t(d, (118, 128), "400 V AC", 8, (20, 20, 22), bold=False)
    return img


def _page(title, tag, rows, accent=(30, 90, 150), size=(256, 160)):
    img = Image.new("RGB", size, (8, 14, 22))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, size[0] - 1, 26], fill=accent)
    _t(d, (10, 13), title, 14, (240, 244, 250), anchor="lm")
    _t(d, (size[0] - 8, 13), tag, 10, (220, 236, 246), anchor="rm")
    for k, (a, b, col) in enumerate(rows):
        y = 44 + k * 24
        _t(d, (10, y), a, 12, (160, 176, 190), anchor="lm", bold=False)
        _t(d, (size[0] - 10, y), b, 12, col, anchor="rm")
    return img.convert("RGBA")


def _analyzer():
    return _page("POWER", "MDB-1", (("Supply", "96 JE/t", (110, 210, 240)), ("Draw", "71 JE/t", (236, 240, 246)),
                                    ("Stored", "84 %", (100, 230, 140)), ("Charging", "2 drones", (255, 190, 60)),
                                    ("Peak today", "188 JE/t", (160, 176, 190))))


def _ups():
    return _page("UPS", "CONTROL", (("Mains", "OK", (100, 230, 140)), ("Battery", "100 %", (100, 230, 140)),
                                    ("Runs", "terminal, screens", (236, 240, 246)), ("Panels", "Goods In/Out", (236, 240, 246))),
                 accent=(36, 110, 80))


def _meter():
    """The utility meter behind its little window: the LCD (what the warehouse draws now, the total), the pulse LED."""
    img = Image.new("RGBA", (128, 128), (150, 154, 150, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([14, 16, 113, 112], fill=(24, 26, 28, 255), outline=(60, 62, 64, 255), width=3)
    d.rectangle([22, 26, 106, 60], fill=(150, 176, 120, 255))
    _t(d, (64, 37), "71 JE/t", 14, (20, 30, 20))
    _t(d, (64, 53), "0 482 913", 10, (30, 40, 30), bold=False)
    d.ellipse([28, 74, 38, 84], fill=(250, 60, 40, 255))
    _t(d, (76, 79), "1000 imp/kJE", 7, (190, 194, 190), bold=False)
    d.rectangle([28, 92, 100, 104], fill=(200, 202, 198, 255))
    _t(d, (64, 98), "SERVICE METER", 7, (20, 20, 22))
    return img


def _meter_door():
    """The meter cabinet's door: grey glass-fibre, its viewing window (the meter behind it is the live part), the
    triangular-key lock, the danger sticker, ELECTRICITY."""
    img = PP._solid((150, 154, 150), 5121, 0.05).convert("RGBA").resize((224, 320))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 221, 317], outline=(120, 124, 120, 255), width=4)
    d.rectangle([40, 50, 184, 180], fill=(30, 34, 36, 255), outline=(110, 114, 110, 255), width=4)
    _danger(d, 112, 232, 26)
    _t(d, (112, 286), "ELECTRICITY", 15, (20, 20, 22))
    d.ellipse([190, 150, 208, 168], fill=(70, 74, 72, 255))
    return img


def _isolator():
    """The main switch's face: yellow plate round the (red) rotary handle, ON (the lever up) and OFF (the lever turned
    to the right, as seen outside), MAIN SWITCH 400 V."""
    img = PP._solid((150, 154, 150), 5123, 0.05).convert("RGBA").resize((160, 224))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 157, 221], outline=(120, 124, 120, 255), width=4)
    d.rectangle([10, 46, 150, 146], fill=(250, 210, 20, 255), outline=(20, 20, 22, 255), width=2)
    _t(d, (80, 58), "ON", 12, (20, 20, 22))
    _t(d, (135, 112), "OFF", 11, (20, 20, 22))
    _t(d, (80, 172), "MAIN SWITCH", 13, (20, 20, 22))
    _t(d, (80, 192), "400 V  1250 A", 10, (20, 20, 22), bold=False)
    _danger(d, 80, 22, 13)
    return img


def _gland():
    """The terminal box the incoming cable is glanded into: its lid screws, CABLE IN, the conduit's gland underneath."""
    img = PP._solid((150, 154, 150), 5125, 0.05).convert("RGBA").resize((160, 160))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 157, 157], outline=(120, 124, 120, 255), width=4)
    for x, y in ((14, 14), (146, 14), (14, 146), (146, 146)):
        d.ellipse([x - 5, y - 5, x + 5, y + 5], fill=(110, 114, 110, 255))
    d.rectangle([24, 56, 136, 104], fill=(246, 246, 240, 255), outline=(20, 20, 22, 255))
    _t(d, (80, 72), "CABLE IN", 15, (20, 20, 22))
    _t(d, (80, 92), "connect cable here", 9, (20, 20, 22), bold=False)
    return img


def _label(text, sub="", w=512, h=48, bg=(246, 246, 240), fg=(20, 20, 22), size=24):
    img = Image.new("RGBA", (w, h), bg + (255,))
    d = ImageDraw.Draw(img)
    _t(d, (w // 2, h // 2 - (6 if sub else 0)), text, size, fg)
    if sub:
        _t(d, (w // 2, h - 9), sub, 10, fg, bold=False)
    return img


def _plate(text, size=12, w=192, h=32):
    """A white engraved label plate's face (drawn in its plate's proportions, square for the atlas)."""
    img = Image.new("RGBA", (w, h), (246, 246, 240, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w - 1, h - 1], outline=(30, 30, 32, 255), width=2)
    _t(d, (w // 2, h // 2 + 1), text, size, (20, 20, 22))
    return img.resize((128, 128))


def _brk_label(text):
    img = Image.new("RGBA", (96, 32), (246, 246, 240, 255))
    d = ImageDraw.Draw(img)
    _t(d, (48, 16), text, 11 if len(text) < 9 else 9, (20, 20, 22))
    # (Square for the atlas: drawn whole onto its 4.4 x 1.2 px plate.)
    return img.resize((128, 128))


def _sig_face():
    """The signal box's front: its grey lid, the label strip, the four LED windows' legends."""
    img = PP._solid((200, 202, 198), 5131, 0.03).convert("RGBA").resize((160, 192))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 157, 189], outline=(150, 152, 150, 255), width=4)
    d.rectangle([12, 150, 148, 176], fill=(246, 246, 240, 255), outline=(20, 20, 22, 255))
    _t(d, (80, 163), "WAREHOUSE I/O", 11, (20, 20, 22))
    for k, s in enumerate(("PWR", "LINK", "IN", "OUT")):
        _t(d, (26 + k * 36, 132), s, 9, (40, 40, 44))
    return img


def _sig_scr():
    img = Image.new("RGBA", (128, 72), (6, 10, 14, 255))
    d = ImageDraw.Draw(img)
    _t(d, (64, 14), "OUT  STOCK", 11, (90, 220, 240))
    _t(d, (64, 34), "Coal < 512", 12, (236, 240, 246))
    _t(d, (64, 56), "ON  15", 13, (100, 230, 140))
    return img.resize((128, 128))


def _board_idle():
    """The status board's page as the model shows it (the game draws the live floor map over it)."""
    img = Image.new("RGB", (512, 320), (8, 12, 18))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 511, 30], fill=(14, 30, 44))
    _t(d, (14, 15), "WAREHOUSE STATUS", 16, (230, 238, 246), anchor="lm")
    _t(d, (256, 175), "NO TERMINAL", 22, (90, 104, 120))
    return img.convert("RGBA")


def _toggle(on):
    """A breaker's toggle seen end on: black, its white I (on) or O (off)."""
    img = Image.new("RGBA", (16, 16), (24, 24, 26, 255))
    d = ImageDraw.Draw(img)
    if on:
        d.rectangle([7, 3, 8, 12], fill=(240, 240, 240, 255))
    else:
        d.ellipse([4, 4, 11, 11], outline=(240, 240, 240, 255))
    return img


def _led(col):
    """A lit pilot lamp's lens: bright in the middle, the colour round it."""
    img = Image.new("RGBA", (16, 16), col + (255,))
    d = ImageDraw.Draw(img)
    d.ellipse([4, 4, 11, 11], fill=tuple(min(255, c + 90) for c in col) + (255,))
    return img


def w5_makers():
    out = {"pi_wh5_led_green": lambda: _led((40, 220, 90)), "pi_wh5_led_amber": lambda: _led((255, 170, 20)),
           "pi_wh5_led_red": lambda: _led((240, 40, 30)), CAB: _cab,
           "pi_wh5_toggle_on": lambda: _toggle(True), "pi_wh5_toggle_off": lambda: _toggle(False), CAB_DARK: _cab_dark, GRP: _grp, CONDUIT: _conduit,
           "pi_wh5_door_inc": lambda: _door("inc"), "pi_wh5_door_out": lambda: _door("out"), "pi_wh5_door_ctl": lambda: _door("ctl"),
           "pi_wh5_acb": _acb, "pi_wh5_analyzer": _analyzer, "pi_wh5_ups": _ups, "pi_wh5_meter": _meter,
           "pi_wh5_meter_door": _meter_door, "pi_wh5_isolator": _isolator, "pi_wh5_gland": _gland,
           "pi_wh5_lbl_service": lambda: _plate("SERVICE ENTRANCE  ·  400 V", 14, 500, 20),
           "pi_wh5_lbl_mdb1": lambda: _plate("MDB-1", 16, 220, 20),
           "pi_wh5_lbl_inc": lambda: _plate("INCOMER", 20), "pi_wh5_lbl_out": lambda: _plate("OUTGOING", 20),
           "pi_wh5_lbl_ctl": lambda: _plate("UPS · CONTROL", 17),
           "pi_wh5_sig_face": _sig_face, "pi_wh5_sig_scr": _sig_scr, "pi_wh5_board": _board_idle}
    for k, s in enumerate(BREAKERS):
        out[f"pi_wh5_brk_{k}"] = (lambda s=s: _brk_label(s))
    return out


W5_SHARP = {"pi_wh5_toggle_on", "pi_wh5_toggle_off", "pi_wh5_led_green", "pi_wh5_led_amber", "pi_wh5_led_red", "pi_wh5_door_inc", "pi_wh5_door_out", "pi_wh5_door_ctl", "pi_wh5_acb", "pi_wh5_analyzer", "pi_wh5_ups", "pi_wh5_meter",
            "pi_wh5_meter_door", "pi_wh5_isolator", "pi_wh5_gland", "pi_wh5_lbl_service", "pi_wh5_lbl_mdb1", "pi_wh5_lbl_inc",
            "pi_wh5_lbl_out", "pi_wh5_lbl_ctl", "pi_wh5_sig_face",
            "pi_wh5_sig_scr", "pi_wh5_board"} | {f"pi_wh5_brk_{k}" for k in range(len(BREAKERS))}


# ================================================================================================ the switchboard
WALL = 16.0       # the back row: the wall's cell (its outer face at z 0)
CAB_Z = 27.0      # the cabinets' doors
BRK_X = (18.4, 23.6, 28.8)
BRK_Y = (18.6, 9.8)


TOGGLE_ON, TOGGLE_OFF = 3.0, 1.2   # a breaker's toggle's bottom over its body's (up: on)


def breaker(cx, y0, on=True, lamp="on", live=True):
    """An outgoing moulded-case breaker on the board's door: its black body through the door's cut-out, the toggle
    (up when on), the lamp over it. (live False: the game's model, the toggle left for the game to draw where the
    breaker is, the lamp's lens dark for it to light.)"""
    out = [box(cx - 1.8, y0, CAB_Z, cx + 1.8, y0 + 5.4, CAB_Z + 1.4, BLACK), box(cx - 1.4, y0 + 0.6, CAB_Z + 1.4, cx + 1.4, y0 + 4.8, CAB_Z + 1.6, GRAPHITE)]
    if live:
        out += toggle(cx, y0 + (TOGGLE_ON if on else TOGGLE_OFF), CAB_Z + 1.6)
    out += round_z(cx, y0 + 6.4, 0.55, CAB_Z, CAB_Z + 0.8, LAMP[lamp] if live else LED_DARK)
    return out


def toggle(cx=0.0, y=0.0, z=0.0):
    """A breaker's toggle (its bottom at y, out from z): black, its white I."""
    return [box(cx - 0.8, y, z, cx + 0.8, y + 1.2, z + 1.4, {"*": BLACK, "south": "pi_wh5_toggle_on"})]


def handle(cx=0.0, cy=0.0, z=0.0, on=True):
    """The main switch's red rotary lever (out to -z from z, its pivot in the knob): pointing up to ON, or turned a
    quarter to OFF (to the right as seen outside: -x)."""
    return [box(cx - 0.6, cy - 0.8, z - 1.2, cx + 0.6, cy + 2.4, z, RED)] if on else [box(cx - 2.4, cy - 0.6, z - 1.2, cx + 0.8, cy + 0.6, z, RED)]


LED_DARK = "pi_wh5_led_dark"   # a pilot lamp's lens, unlit (the game lights it)
MAIN_LAMP = (12.4, 5.4, 0.6)   # the incomer's lamp: clear of the door's label


def switchboard(states=None, main_on=True, live=True):
    """The Warehouse Switchboard (three wide, two high, two deep; front, +z, to the hall; its back row in the outer
    wall). Outside on the wall (z under 0): the meter cabinet, the main switch, the cable terminal box with its
    conduit into the ground, the SERVICE ENTRANCE plate. Inside: the board's three sections on their plinth."""
    states = states or ["on"] * 6
    out = []
    # ---- the back row: the wall (precast plinth outside, its flashing at the top).
    out += [box(0.0, 0.0, 0.1, 48.0, 16.0, WALL, {"*": PRECAST_LOW, "south": CAB_DARK}),
            box(0.0, 16.0, 0.1, 48.0, 32.0, WALL, {"*": PRECAST, "south": CAB_DARK})]
    out += [box(0.0, 31.0, -0.7, 48.0, 32.0, 0.1, FLASH_D), box(0.0, 30.4, -0.7, 48.0, 31.0, -0.3, FLASH_D)]
    # The meter cabinet: its body, the door (its window over the meter, the live part).
    out += [box(3.0, 6.0, -5.0, 19.0, 28.0, 0.1, {"*": "pi_wh5_grp", "north": "pi_wh5_meter_door!"})]
    out += [box(5.9, 15.6, -5.1, 16.1, 24.6, -5.0, {"north": "pi_wh5_meter!"})]
    # The main switch: its box, the rotary handle (up: on).
    out += [box(21.0, 10.0, -4.0, 31.0, 24.0, 0.1, {"*": GRP, "north": "pi_wh5_isolator!"})]
    out += round_z(26.0, 17.0, 1.6, -5.2, -4.0, RED)
    if live:
        out += handle(26.0, 17.0, -5.2, main_on)
    # The cable terminal box (where the incoming cable connects), the conduit down into the ground, and the conduits
    # from it to the main switch and on into the wall.
    out += [box(34.0, 2.0, -4.0, 46.0, 14.0, 0.1, {"*": GRP, "north": "pi_wh5_gland!"})]
    out += round_y(40.0, -2.2, 1.3, -8.0, 2.0, CONDUIT) + round_y(40.0, -2.2, 1.8, 1.0, 2.0, BLACK)
    out += [box(31.0, 15.6, -2.6, 34.0, 17.4, -0.8, CONDUIT), box(32.2, 14.0, -2.6, 34.0, 15.6, -0.8, CONDUIT),
            box(19.0, 16.0, -2.6, 21.0, 17.8, -0.8, CONDUIT)]
    out += [box(4.0, 28.6, -0.6, 44.0, 30.2, 0.1, {"*": STEEL, "north": "pi_wh5_lbl_service!"})]
    # ---- the board: plinth, three sections (bodies, doors), their parts.
    out += [box(0.4, 0.0, WALL + 1.0, 47.6, 1.6, CAB_Z - 0.4, CAB_DARK)]
    for k, (x0, x1) in enumerate(((0.4, 15.8), (16.2, 31.8), (32.2, 47.6))):
        out += [box(x0, 1.6, WALL, x1, 30.0, CAB_Z, {"*": CAB, "south": ("pi_wh5_door_inc!", "pi_wh5_door_out!", "pi_wh5_door_ctl!")[k]})]
        out += [box(x1 - 1.6, 13.0, CAB_Z, x1 - 0.8, 17.6, CAB_Z + 1.0, CHROME), box(x1 - 1.8, 15.0, CAB_Z + 1.0, x1 - 0.6, 15.6, CAB_Z + 1.4, CHROME)]
        cx = (x0 + x1) / 2
        out += [box(cx - 4.2, 2.4, CAB_Z, cx + 4.2, 3.8, CAB_Z + 0.15, {"*": STEEL, "south": ("pi_wh5_lbl_inc!", "pi_wh5_lbl_out!", "pi_wh5_lbl_ctl!")[k]})]
    out += [box(0.4, 30.0, WALL, 47.6, 31.0, CAB_Z + 0.4, CAB_DARK)]
    out += [box(19.5, 30.15, CAB_Z + 0.4, 28.5, 30.95, CAB_Z + 0.6, {"*": STEEL, "south": "pi_wh5_lbl_mdb1!"})]
    # The cable tray over the board, into the wall above.
    out += [box(2.0, 31.0, WALL + 0.6, 46.0, 31.4, WALL + 8.0, STEEL), box(2.0, 31.4, WALL + 0.6, 46.0, 32.0, WALL + 1.0, STEEL),
            box(2.0, 31.4, WALL + 7.6, 46.0, 32.0, WALL + 8.0, STEEL)]
    for x in range(4, 46, 3):
        out.append(box(x, 31.4, WALL + 1.2, x + 1.6, 31.9, WALL + 7.4, ("pi_wh_graphite", "pi_wh_spk_red", BLACK)[x % 3]))
    # Incomer: the air circuit breaker (its face; the charging handle), the power analyzer (the live screen), the
    # main lamp.
    out += [box(2.6, 7.4, CAB_Z, 13.6, 18.4, CAB_Z + 1.8, {"*": BLACK, "south": "pi_wh5_acb!"}), box(6.6, 5.6, CAB_Z + 0.6, 9.6, 7.4, CAB_Z + 2.4, BLACK)]
    out += W.screen_unit(3.4, 20.6, 12.8, 26.4, CAB_Z, "pi_wh5_analyzer", hood=False)
    out += round_z(MAIN_LAMP[0], MAIN_LAMP[1], MAIN_LAMP[2], CAB_Z, CAB_Z + 0.9, (LAMP["on" if main_on else "off"]) if live else LED_DARK)
    # Outgoing: six breakers with their lamps and labels.
    for k in range(6):
        cx, y0 = BRK_X[k % 3], BRK_Y[k // 3]
        out += breaker(cx, y0, states[k] != "off", states[k], live)
        out += [box(cx - 2.2, y0 - 1.6, CAB_Z, cx + 2.2, y0 - 0.4, CAB_Z + 0.2, {"*": STEEL, "south": f"pi_wh5_brk_{k}!"})]
    # Control: the UPS screen, its key switch, the mains and battery lamps.
    out += W.screen_unit(35.6, 21.0, 44.2, 26.0, CAB_Z, "pi_wh5_ups", hood=False)
    out += round_z(37.0, 17.2, 1.0, CAB_Z, CAB_Z + 1.0, BLACK) + [box(36.7, 16.4, CAB_Z + 1.0, 37.3, 18.0, CAB_Z + 1.6, CHROME)]
    lamp = LAMP["on"] if live else LED_DARK
    out += round_z(41.0, 17.2, 0.55, CAB_Z, CAB_Z + 0.8, lamp) + round_z(43.4, 17.2, 0.55, CAB_Z, CAB_Z + 0.8, lamp)
    return out


# ================================================================================================ the status board
def status_board(tex="pi_wh5_board"):
    """The Warehouse Status Board: a large display on its wall bracket (three wide, two high, against a wall, its
    face +z): bezel, the glass with the live page."""
    out = [box(18.0, 8.0, 0.0, 30.0, 22.0, 0.8, STEEL), box(20.0, 10.0, 0.8, 28.0, 20.0, 1.6, GRAPHITE)]
    out += [box(0.6, 1.6, 1.6, 47.4, 30.4, 3.0, BLACK), box(1.2, 2.2, 3.0, 46.8, 29.8, 3.1, {"*": BLACK, "south": tex + "!"})]
    out += [box(22.0, 1.0, 1.6, 26.0, 1.6, 2.6, GRAPHITE), round_z(45.6, 1.9, 0.18, 3.0, 3.2, "pi_wh_lamp_green")[0]]
    return out


# ================================================================================================ the signal box
def signal_box(lit=(True, True, False, True), live=True):
    """The Warehouse Signal Box: a small remote I/O enclosure on a wall (its back at z 0): the grey box, its little
    screen, four LEDs (PWR, LINK, IN, OUT), the cable gland under it, the DIN-rail clips either side."""
    out = [box(2.4, 1.6, 0.0, 13.6, 14.4, 5.0, {"*": CAB, "south": "pi_wh5_sig_face!"})]
    out += [box(3.6, 8.2, 5.0, 12.4, 12.8, 5.3, {"*": BLACK, "south": "pi_wh5_sig_scr!"})]
    cols = ("pi_wh5_led_green", "pi_wh5_led_green", "pi_wh5_led_amber", "pi_wh5_led_red")
    for k in range(4):
        x = 4.6 + k * 2.25
        out += round_z(x, 6.4, 0.35, 5.0, 5.5, (cols[k] if lit[k] else GRAPHITE) if live else LED_DARK)
    out += round_y(8.0, 2.5, 0.9, 0.6, 1.6, BLACK) + round_y(8.0, 2.5, 0.5, 0.0, 0.6, CONDUIT)
    out += [box(1.6, 6.0, 0.0, 2.4, 10.0, 1.6, GRAPHITE), box(13.6, 6.0, 0.0, 14.4, 10.0, 1.6, GRAPHITE)]
    return out


# ================================================================================================ the game
# What tools/warehouse_data.py writes for part W5: the three pieces as their models (the toggles, the main switch's
# handle, the lamps lit, every page and the floor map the game draws: client/warehouse/SwitchboardRenderer,
# StatusBoardRenderer, SignalBoxRenderer), the hidden parts it draws them with, the numbers it draws them by.
PIECES_W5 = {
    "warehouse_switchboard": ("Warehouse Switchboard", (3, 2, 2)),
    "warehouse_status_board": ("Warehouse Status Board", (3, 2, 1)),
    "warehouse_signal_box": ("Warehouse Signal Box", (1, 1, 1)),
}
WHITE = "pi_wh5_white"   # lit white: the game tints it (a lamp's colour, a map cell's)
W5_DARK = {"pi_wh5_analyzer!": "pi_wh_scr_off!", "pi_wh5_ups!": "pi_wh_scr_off!", "pi_wh5_meter!": "pi_wh5_meter_game!",
           "pi_wh5_sig_scr!": "pi_wh_scr_off!", "pi_wh5_board!": "pi_wh5_board_dark!"}


def piece_boxes_w5(name):
    if name == "warehouse_switchboard":
        return switchboard(live=False)
    if name == "warehouse_status_board":
        return status_board()
    if name == "warehouse_signal_box":
        return signal_box(live=False)
    raise KeyError(name)


def lamp_disc():
    """A lit pilot lamp's lens (an octagon of radius 1, 0.7 deep, its back at z 0; the game scales it to the lamp and
    tints it)."""
    return round_z(0.0, 0.0, 1.0, 0.0, 0.7, WHITE)


def fill_quad():
    """A unit square of lit white facing +z (the game tints and stretches it: the status board's map)."""
    return [box(0.0, 0.0, 0.0, 1.0, 1.0, 0.05, {"south": WHITE + "!"})]


def parts_w5():
    """The hidden part models (warehouse_part) the game draws part W5's live things with."""
    return {"w5_toggle": toggle, "w5_handle": lambda: handle(0.0, 0.0, 0.0, True), "w5_led": lamp_disc, "w5_fill": fill_quad}


def _rect(x0, y0, x1, y1, z):
    return {"x0": x0, "x1": x1, "y0": y0, "y1": y1, "z": z}


def layout_w5():
    """assets/jugcraft/warehouse.json's part W5 sections (piece pixels; the pieces' fronts to +z)."""
    toggles, lamps = [], []
    for k in range(6):
        cx, y0 = BRK_X[k % 3], BRK_Y[k // 3]
        toggles.append([cx, y0 + TOGGLE_ON, y0 + TOGGLE_OFF, CAB_Z + 1.6])
        lamps.append([cx, y0 + 6.4, CAB_Z + 0.8, 0.55])
    # The meter's LCD and pulse LED on its north face (the texture's u runs from +x to -x there): 10.2 x 9 px.
    mx0, mx1, my0, my1 = 5.9, 16.1, 15.6, 24.6
    u = lambda f: mx1 - f * (mx1 - mx0)
    v = lambda f: my1 - f * (my1 - my0)
    return {
        "switchboard": {"toggles": toggles, "lamps": lamps, "main_lamp": [MAIN_LAMP[0], MAIN_LAMP[1], CAB_Z + 0.9, MAIN_LAMP[2]],
                        "ups_lamps": [[41.0, 17.2, CAB_Z + 0.8, 0.55], [43.4, 17.2, CAB_Z + 0.8, 0.55]], "handle": [26.0, 17.0, -5.2],
                        "analyzer": _rect(3.6, 20.8, 12.6, 26.2, CAB_Z + 1.8), "ups": _rect(35.8, 21.2, 44.0, 25.8, CAB_Z + 1.8),
                        "meter": _rect(u(106 / 128), v(60 / 128), u(22 / 128), v(26 / 128), -5.1), "pulse": [u(33 / 128), v(79 / 128), -5.1, 0.4]},
        "status_board": {"screen": _rect(1.2, 2.2, 46.8, 29.8, 3.1)},
        "signal_box": {"leds": [[4.6 + k * 2.25, 6.4, 5.5, 0.35] for k in range(4)], "screen": _rect(3.6, 8.2, 12.4, 12.8, 5.3)},
    }


def _white():
    return Image.new("RGBA", (16, 16), (255, 255, 255, 255))


def _led_dark():
    img = Image.new("RGBA", (16, 16), (44, 48, 50, 255))
    d = ImageDraw.Draw(img)
    d.ellipse([4, 4, 11, 11], fill=(62, 68, 70, 255))
    return img


def _meter_game():
    """The meter for the game: its LCD blank and the pulse LED dark (the game writes the reading and flashes it)."""
    img = Image.new("RGBA", (128, 128), (150, 154, 150, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([14, 16, 113, 112], fill=(24, 26, 28, 255), outline=(60, 62, 64, 255), width=3)
    d.rectangle([22, 26, 106, 60], fill=(70, 84, 60, 255))
    d.ellipse([28, 74, 38, 84], fill=(70, 30, 26, 255))
    _t(d, (76, 79), "1000 imp/kJE", 7, (190, 194, 190), bold=False)
    d.rectangle([28, 92, 100, 104], fill=(200, 202, 198, 255))
    _t(d, (64, 98), "SERVICE METER", 7, (20, 20, 22))
    return img


def _board_dark():
    return Image.new("RGBA", (64, 64), (8, 12, 18, 255))


def game_makers():
    """The textures the game's models need besides the mockups' (w5_makers)."""
    return {WHITE: _white, LED_DARK: _led_dark, "pi_wh5_meter_game": _meter_game, "pi_wh5_board_dark": _board_dark}


GAME_SHARP = {WHITE, LED_DARK, "pi_wh5_meter_game", "pi_wh5_board_dark"}

# The names, tooltips, messages and screens' titles; the recipes.
LANG_W5 = {
    "tooltip.jugcraft.warehouse.switchboard": "Its back in an outside wall: bring a cable to its CABLE IN box outside. Powers the warehouse; switch its six circuits here.",
    "tooltip.jugcraft.warehouse.status_board": "Hang it on a wall in a warehouse: the live floor map and the day's numbers. Use it to show another floor.",
    "tooltip.jugcraft.warehouse.signal_box": "On a wall by a warehouse: one rule, redstone out (stock below a number, an order ready, low power...) or in (pause the fleet...).",
    "container.jugcraft.warehouse_switchboard": "Switchboard",
    "container.jugcraft.warehouse_signal_box": "Signal Box",
    "message.jugcraft.warehouse.not_yours": "This warehouse is %s's",
    "message.jugcraft.warehouse.claimed": "This warehouse is yours now (PERSONAL: only you use it; PARTY on the terminal's CONTROL page)",
    "message.jugcraft.warehouse.access.personal": "PERSONAL: only you use this warehouse",
    "message.jugcraft.warehouse.access.party": "PARTY: your party uses this warehouse too",
    "message.jugcraft.warehouse.board_all": "Showing each floor in turn",
    "message.jugcraft.warehouse.board_floor": "Showing floor %s",
    "message.jugcraft.warehouse.signal_item": "Signal Box item: %s",
}
CRAFTING_W5 = {
    "warehouse_switchboard": (["SBS", "CRC", "SPS"], {"S": "#c:plates/steel", "B": "jugcraft:basic_circuit", "C": "jugcraft:copper_cable",
                                                     "R": "minecraft:redstone_block", "P": "minecraft:comparator"}, 1),
    "warehouse_status_board": (["GGG", "GCG", "SSS"], {"G": "minecraft:glass_pane", "C": "jugcraft:advanced_circuit", "S": "#c:plates/steel"}, 1),
    "warehouse_signal_box": (["SRS", "CPC", "SSS"], {"S": "#c:plates/steel", "R": "minecraft:redstone", "C": "jugcraft:basic_circuit",
                                                    "P": "minecraft:comparator"}, 1),
}
