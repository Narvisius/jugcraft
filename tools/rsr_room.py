"""The remote shutdown room brought up to the detail standard (older rooms round 7; Justin: "Remote shutdown room"). The
round 24 room kept its panel (live since #5a) but its furniture was the first the plant had: a generic black operator
console with painted screens, nothing on the walls but an extinguisher. As a real Westinghouse plant's alternate
shutdown room has it, the place the crew comes to when the control room has to be evacuated (AOP-8):

- the remote shutdown station (rsr_desk, three blocks wide, against the east wall): the same black steel console as the
  control room's, with two of the plant process computer's screens (live, nuclear/ControlRoomConsoles.java: the left
  works through AOP-8's checks against the plant, each step DONE or still open; the right the hot standby readings; a
  click steps a screen on through the RCS, steam generator and aux feed pages), its name on the plate over them, the
  sound-powered phone set and its headset, the radio in its charger, a flashlight, the RSP log, AOP-8 open in its
  holder;
- the transfer key cabinet by the door (rsr_key_cabinet): a red break-glass box, the transfer keys on their hooks
  (train A, train B, the spare), its hammer on a chain beside it;
- the communications station (rsr_phone_station): the plant page handset station and the sound-powered phone jack box
  with the headset hung under it;
- the room's sign (sign_rsr_area: the Appendix R safe shutdown area, keep clear);
- pieces the plant has: the AOP procedure rack and an SCBA cabinet (the control room's), the radio charging rack and the
  master clock (the operations office's), a smoke detector.

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

# ------------------------------------------------------------------------------------------------ the station
KEY = "rsr_desk"
MON = CR.SS_MON                 # its two screens' middles (the shift manager's desk's layout)
PLATE = (16.0, 32.0, 22.4, 24.0, 2.7, 3.3)
# The pages its screens step through (nuclear/ControlRoomConsoles.java RSR_PAGES): screen m starts on the m-th.
PAGES = ["AOP-8 EVAC", "HOT STANDBY", "RCS", "SECONDARY", "AUX FEED"]
PAGE_UV = CR.PAGE_UV
LOG_UV = CR.LOG_UV


def _sp_phone(x0, z0):
    """The sound-powered phone set: its grey case (the circuit selector and the two jacks on its sloped top), the
    handset lying in its cradle, the cord's loop to the case."""
    return [(x0, DESK_Y, z0, x0 + 5.2, DESK_Y + 1.2, z0 + 4.0, {"*": "pi_rsr_spp_side", "up": "pi_rsr_spp!"}),
            (x0 + 0.4, DESK_Y + 1.2, z0 + 0.3, x0 + 4.8, DESK_Y + 1.5, z0 + 1.3, "pi_rsr_spp_side"),
            # The handset: its bar, the earpiece and mouthpiece cups.
            (x0 + 1.4, DESK_Y + 1.6, z0 + 0.5, x0 + 3.8, DESK_Y + 2.0, z0 + 1.1, BLACK),
            (x0 + 0.5, DESK_Y + 1.5, z0 + 0.35, x0 + 1.4, DESK_Y + 2.3, z0 + 1.25, BLACK),
            (x0 + 3.8, DESK_Y + 1.5, z0 + 0.35, x0 + 4.7, DESK_Y + 2.3, z0 + 1.25, BLACK),
            # The press-to-talk bar on the handset's side, the cord down into the case.
            (x0 + 2.2, DESK_Y + 1.5, z0 + 1.1, x0 + 3.0, DESK_Y + 1.8, z0 + 1.3, "pi_cap_red"),
            (x0 + 4.9, DESK_Y + 0.4, z0 + 0.6, x0 + 5.3, DESK_Y + 1.6, z0 + 1.0, BLACK)]


def _radio(x0, z0):
    """The radio in its charging base, its antenna up."""
    return [(x0, DESK_Y, z0, x0 + 2.6, DESK_Y + 0.8, z0 + 2.4, BLACK),
            (x0 + 0.4, DESK_Y + 0.8, z0 + 0.6, x0 + 2.2, DESK_Y + 5.2, z0 + 1.8, {"*": BLACK, "south": "pi_tsc_radio!"}),
            (x0 + 1.6, DESK_Y + 5.2, z0 + 0.9, x0 + 1.9, DESK_Y + 7.4, z0 + 1.2, BLACK)]


def _flashlight(x0, z0):
    """A flashlight lying on the desk: its body, the switch, the head with its lens to the right."""
    return [(x0, DESK_Y, z0, x0 + 4.4, DESK_Y + 0.8, z0 + 0.8, "pi_rsr_flash"),
            (x0 + 1.4, DESK_Y + 0.8, z0 + 0.2, x0 + 2.0, DESK_Y + 0.95, z0 + 0.6, BLACK),
            (x0 + 4.4, DESK_Y, z0 - 0.15, x0 + 5.4, DESK_Y + 1.1, z0 + 0.95, {"*": "pi_rsr_flash", "east": "pi_rsr_lens"})]


def desk_boxes(open_=False):
    W = 48
    out = CR._console_body(W)
    for cx in MON:
        out += CR._monitor(cx)
    x0, x1, y0, y1, z0, z1 = PLATE
    out += [(x0, y0, z0, x1, y1, z1, {"*": CR.ALUM, "south": f"pi_{KEY}_f"}),
            (x0 + 3.6, CR.MON_Y[1], z0 + 0.1, x0 + 4.1, y0, z1 - 0.1, CR.ALUM), (x1 - 4.1, CR.MON_Y[1], z0 + 0.1, x1 - 3.6, y0, z1 - 0.1, CR.ALUM)]
    out += TR._keyboard(19.6, 10.4)
    out += _sp_phone(1.4, 5.0)
    out += TR._headset(8.4, 6.2)
    out += CR._logbook(2.0, 10.4, "pi_rsr_log")
    out += _radio(45.0, 7.0)
    out += _flashlight(30.6, 13.0)
    out += CR._procedure_holder(35.4, 6.2, "pi_rsr_aop8")
    # The process computer's workstation under the desk, beside the right pedestal.
    out += [(33.6, 0.3, 4.4, 37.0, 8.6, 12.6, {"*": "pi_tsc_body", "south": "pi_tsc_pc!"})]
    return out


def rsr_desk(label=None):
    """The station's drawing: its name on the plate, the screens' live lines."""
    f = PP.Front(3, 990)
    x0, x1, y0, y1, _, _ = PLATE
    f.R(x0, y0, x1, y1, fill=(214, 216, 214, 255), outline=(150, 152, 150, 255), width=3)
    RR._fit(f, (x0 + x1) / 2, (y0 + y1) / 2, "REMOTE SHUTDOWN STATION", 9, x1 - x0 - 1.2, fill=INK)
    for m, cx in enumerate(MON):
        CR._screen_live(f, cx, f"rsd_m{m}")
    return f


# ------------------------------------------------------------------------------------------------ the room's pieces
KEYS = [("XFER A", (232, 132, 40)), ("XFER B", (60, 150, 80)), ("SPARE", (236, 236, 230))]
KEY_X = [4.6, 7.2, 9.8]
SIGN_UV = [0, 0, 16, 4.95]


def key_cabinet():
    """The transfer key cabinet by the door: a red steel box with a break-glass front, its sign across the top, the
    three keys on their hooks (each its coloured tag), the hammer on its chain beside it."""
    R = "pi_rsr_keybox"
    out = [(3.0, 2.0, 0.0, 13.0, 14.0, 0.6, R),
           (3.0, 2.0, 0.6, 3.8, 14.0, 2.6, R), (12.2, 2.0, 0.6, 13.0, 14.0, 2.6, R),
           (3.8, 2.0, 0.6, 12.2, 2.8, 2.6, R),
           (3.8, 11.4, 0.6, 12.2, 14.0, 2.6, {"*": R, "south": ("pi_rsr_keybox_sign", SIGN_UV)}),
           (3.8, 2.8, 2.2, 12.2, 11.4, 2.4, {"*": "pi_rsr_glass", "south": "pi_rsr_glass", "north": "pi_rsr_glass"}),
           # The hook rail inside.
           (4.2, 10.0, 0.6, 11.8, 10.5, 1.2, "pi_chrome")]
    for k, x in enumerate(KEY_X):
        out += [(x + 0.8, 9.3, 0.8, x + 1.2, 10.0, 1.1, "pi_chrome"),                      # the hook
                (x + 0.55, 8.7, 0.85, x + 1.45, 9.4, 1.05, "pi_rsr_brass"),               # the ring
                (x, 5.6, 0.8, x + 2.0, 8.7, 1.0, {"*": f"pi_rsr_tag_{k}", "south": f"pi_rsr_tag_{k}!"}),
                (x + 0.75, 3.8, 0.9, x + 1.25, 5.6, 1.05, "pi_rsr_brass")]                # the key's blade below its tag
    # The hammer on its chain at the box's right side.
    out += [(13.0, 11.6, 0.4, 13.5, 12.1, 1.0, "pi_chrome"),
            (13.2, 8.6, 0.6, 13.4, 11.6, 0.8, "pi_chrome"),
            (13.6, 5.0, 0.8, 14.0, 8.4, 1.2, "pi_cap_red"),
            (13.2, 8.4, 0.5, 14.6, 9.2, 1.5, "pi_cap_red")]
    return out


def phone_station():
    """The communications station: the plant page handset station (its case, the handset in its cradle, the coiled
    cord, the faceplate's PAGE key and party line selector) and the sound-powered phone jack box beside it, the headset
    hung on its hook under the box."""
    out = [(1.6, 3.0, 0.0, 8.2, 13.6, 2.8, {"*": "pi_rsr_page_body", "south": "pi_rsr_page_face!"}),
           # The handset on the case's left: the bar, the earpiece and mouthpiece.
           (2.4, 6.0, 2.8, 3.4, 11.4, 3.6, BLACK),
           (2.1, 10.6, 2.8, 3.7, 12.6, 4.2, BLACK), (2.1, 4.8, 2.8, 3.7, 6.8, 4.2, BLACK),
           # The coiled cord down from the mouthpiece and back into the case's foot.
           (2.6, 3.4, 3.0, 3.2, 4.8, 3.6, BLACK), (2.6, 2.4, 2.0, 3.2, 3.4, 3.4, BLACK),
           # The jack box.
           (9.6, 7.0, 0.0, 14.6, 13.0, 1.8, {"*": "pi_rsr_jackbox", "south": "pi_rsr_jack_face!"}),
           # The headset's hook under it, the headset hung on it: the band, the ear cups, the boom.
           (11.8, 6.0, 0.0, 12.4, 6.6, 1.8, "pi_chrome"),
           (10.6, 5.6, 1.0, 13.6, 6.1, 1.6, BLACK), (10.2, 5.0, 1.0, 10.7, 5.8, 1.6, BLACK), (13.5, 5.0, 1.0, 14.0, 5.8, 1.6, BLACK),
           (9.8, 2.8, 0.7, 10.9, 5.0, 2.3, {"*": BLACK, "west": "pi_tsc_cushion", "east": "pi_tsc_cushion"}),
           (13.3, 2.8, 0.7, 14.4, 5.0, 2.3, {"*": BLACK, "west": "pi_tsc_cushion", "east": "pi_tsc_cushion"}),
           (13.6, 2.6, 2.1, 13.9, 3.4, 3.6, BLACK),
           # Its cord plugged into circuit 1's jack: the plug, the cord down to the left ear cup.
           (10.7, 8.4, 1.8, 11.3, 9.0, 2.6, BLACK), (10.85, 5.6, 2.3, 11.15, 8.4, 2.6, BLACK)]
    return out


SIGNS = {"sign_rsr_area": "Remote Shutdown Area Sign"}


def sign_tex(kind):
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, H - 1], fill=(250, 250, 246))
    d.rectangle([0, 0, W - 1, 132], fill=(20, 60, 140))
    SGR._text(d, (W / 2, 46), "REMOTE SHUTDOWN", 50, (250, 250, 246), width=W - 40)
    SGR._text(d, (W / 2, 100), "ROOM", 50, (250, 250, 246), width=W - 40)
    SGR._text(d, (W / 2, 180), "APPENDIX R", 40, (24, 24, 26), width=W - 50)
    SGR._text(d, (W / 2, 224), "SAFE SHUTDOWN AREA", 32, (24, 24, 26), width=W - 50)
    d.rectangle([24, 262, W - 25, 352], fill=(246, 200, 40))
    SGR._text(d, (W / 2, 290), "KEEP CLEAR", 40, (24, 24, 26), width=W - 70)
    SGR._text(d, (W / 2, 330), "NO STORAGE IN THIS ROOM", 22, (24, 24, 26), width=W - 70)
    SGR._text(d, (W / 2, 396), "CONTROL ROOM EVACUATION", 24, (24, 24, 26), width=W - 50)
    SGR._text(d, (W / 2, 428), "PER AOP-8 ONLY", 24, (24, 24, 26), width=W - 50)
    d.rectangle([10, 10, W - 11, H - 11], outline=(20, 60, 140), width=8)
    return img


PIECES = {"rsr_key_cabinet": {"display": "Transfer Key Cabinet", "boxes": key_cabinet()},
          "rsr_phone_station": {"display": "Plant Communications Station", "boxes": phone_station()}}


# ------------------------------------------------------------------------------------------------ textures
def _f(size, bold=True):
    return PP.rfont(size, bold)


AOP8_STEPS = [("TRIP THE REACTOR", "BEFORE LEAVING THE CR"), ("TRIP THE TURBINE", "STOP VALVES CLOSED"),
              ("TAKE THE TRANSFER KEYS", "KEY CABINET BY THE DOOR"), ("TRANSFER CONTROL", "XFER SWITCHES TO LOCAL"),
              ("VERIFY REACTOR TRIPPED", "RX TRIPPED LAMP LIT"), ("START AFW PUMPS", "A, B AND TD AS NEEDED"),
              ("OPEN AFW FCVS", "FLOW TO EVERY SG"), ("HOLD SG LEVELS", "30 - 50 % NARROW RANGE"),
              ("CONTROL SG PRESSURE", "ARVS TO 1005 PSIG"), ("CHECK TAVG", "547 F HOT STANDBY"),
              ("CHECK PZR LEVEL", "ABOVE 17 %"), ("MONITOR SR FLUX", "STARTUP RATE NEGATIVE"),
              ("CALL THE CR / TSC", "SOUND POWERED CKT 1"), ("TRANSFER BACK", "WHEN THE CR IS HABITABLE")]


def _spp_top():
    """The sound-powered phone set's top: the circuit selector (its knob's pointer at 1, the circuits round it) and the
    two jacks."""
    img = Image.new("RGBA", (256, 256), (120, 124, 120, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 255], outline=(80, 82, 80), width=6)
    cx, cy = 96, 140
    d.ellipse([cx - 62, cy - 62, cx + 62, cy + 62], fill=(236, 236, 230))
    for k in range(6):
        import math
        a = math.radians(210 - k * 48)
        x, y = cx + math.cos(a) * 48, cy - math.sin(a) * 48
        d.text((x, y), str(k + 1), font=_f(22), fill=(24, 24, 26), anchor="mm")
    d.ellipse([cx - 24, cy - 24, cx + 24, cy + 24], fill=(30, 30, 32))
    d.line([(cx, cy), (cx - 18, cy + 12)], fill=(240, 240, 236), width=6)
    SGR._text(d, (cx, 40), "CIRCUIT", 24, (250, 250, 246))
    for k, y in enumerate((96, 186)):
        d.ellipse([196 - 24, y - 24, 196 + 24, y + 24], fill=(200, 202, 204))
        d.ellipse([196 - 11, y - 11, 196 + 11, y + 11], fill=(20, 20, 22))
        SGR._text(d, (196, y + 40), f"JACK {k + 1}", 16, (250, 250, 246))
    return img


def _keybox_sign():
    """The key cabinet's sign (its top 512 by 158: the header's shape)."""
    img = Image.new("RGBA", (512, 512), (190, 30, 28, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([6, 6, 505, 151], outline=(250, 250, 246), width=5)
    SGR._text(d, (256, 46), "RSP TRANSFER KEYS", 50, (250, 250, 246), width=470)
    SGR._text(d, (256, 108), "BREAK GLASS - AOP-8", 40, (250, 250, 246), width=470)
    return img


def _tag(k):
    """A key's tag (2 by 3.1: its 128 by 198 shape squeezed into the square): the colour, the key's name."""
    name, rgb = KEYS[k]
    img = Image.new("RGBA", (128, 198), rgb + (255,))
    d = ImageDraw.Draw(img)
    d.ellipse([52, 10, 76, 34], fill=(40, 40, 42))
    dark = sum(rgb) > 500
    ink = (24, 24, 26) if dark else (250, 250, 246)
    for i, word in enumerate(name.split()):
        SGR._text(d, (64, 82 + i * 52), word, 44, ink, width=116)
    d.rectangle([0, 0, 127, 197], outline=(30, 30, 32), width=4)
    return img.resize((128, 128), Image.LANCZOS)


def _page_face():
    """The page station's faceplate: PLANT PAGE across its top, the speaker grille, the PAGE key, the party line
    selector, the handset's cradle on its left."""
    img = Image.new("RGBA", (256, 256), (226, 210, 120, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 255], outline=(150, 136, 70), width=6)
    d.rectangle([92, 14, 244, 52], fill=(30, 30, 32))
    SGR._text(d, (168, 33), "PLANT PAGE", 24, (250, 250, 246), width=146)
    for y in range(72, 140, 12):
        d.line([(110, y), (226, y)], fill=(70, 66, 40), width=5)
    d.rectangle([116, 156, 220, 196], fill=(200, 40, 34))
    SGR._text(d, (168, 176), "PAGE", 24, (250, 250, 246))
    SGR._text(d, (168, 216), "PARTY  1  2  3  4  5", 16, (30, 30, 32), width=140)
    d.rectangle([20, 40, 78, 236], fill=(196, 182, 100))
    return img


def _jack_face():
    """The sound-powered phone jack box's face: its name plate, the two jacks with their circuits."""
    img = Image.new("RGBA", (256, 256), (230, 196, 40, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 255], outline=(150, 120, 20), width=6)
    d.rectangle([16, 14, 239, 92], fill=(250, 250, 246))
    SGR._text(d, (128, 36), "SOUND POWERED", 28, (24, 24, 26), width=210)
    SGR._text(d, (128, 70), "PHONE JACKS", 28, (24, 24, 26), width=210)
    for k, x in enumerate((72, 184)):
        SGR._text(d, (x, 122), f"CKT {1 + k * 2}", 24, (24, 24, 26))
        d.ellipse([x - 30, 152, x + 30, 212], fill=(200, 202, 204))
        d.ellipse([x - 13, 169, x + 13, 195], fill=(20, 20, 22))
    return img


def makers():
    solid = PP._solid
    return {
        "pi_rsr_spp_side": lambda: solid((110, 114, 110), 991, 0.05), "pi_rsr_spp": _spp_top,
        "pi_rsr_flash": lambda: solid((230, 190, 40), 992, 0.05), "pi_rsr_lens": lambda: solid((250, 248, 228), 993, 0.0),
        "pi_rsr_keybox": lambda: solid((180, 32, 30), 994, 0.06), "pi_rsr_brass": lambda: solid((196, 160, 70), 995, 0.04),
        "pi_rsr_glass": lambda: PP._see_through((214, 228, 234), 44, 996), "pi_rsr_keybox_sign": _keybox_sign,
        "pi_rsr_page_body": lambda: solid((210, 194, 108), 997, 0.06), "pi_rsr_page_face": _page_face,
        "pi_rsr_jackbox": lambda: solid((220, 186, 36), 998, 0.06), "pi_rsr_jack_face": _jack_face,
        "pi_rsr_aop8": lambda: CR._page_tex("AOP-8", "CONTROL ROOM EVACUATION", AOP8_STEPS, accent=(20, 60, 140)),
        "pi_rsr_log": lambda: CR._log_tex("RSP LOG"),
        **{f"pi_rsr_tag_{k}": (lambda k=k: _tag(k)) for k in range(len(KEYS))},
    }


SHARP = {"pi_rsr_spp", "pi_rsr_keybox_sign", "pi_rsr_page_face", "pi_rsr_jack_face", "pi_rsr_aop8", "pi_rsr_log",
         *{f"pi_rsr_tag_{k}" for k in range(len(KEYS))}}
SEE_THROUGH = {"pi_rsr_glass"}


# ------------------------------------------------------------------------------------------------ registration
# (Two rows, as the control room's consoles: the screens' upper halves in the upper row, so a click anywhere on a screen
# lands: nuclear/CrConsoleBlock.)
PANELS = {KEY: {"display": "Remote Shutdown Station", "width": 3, "rows": 2, "item3d": True}}
DRAWINGS = {KEY: rsr_desk}
BOXES = {KEY: desk_boxes}
CRAFTING = {
    KEY: (["G G", "PCP", "PRP"], {"G": "minecraft:black_stained_glass_pane", "P": "#c:plates/steel", "C": "jugcraft:basic_circuit",
                                   "R": "minecraft:redstone"}, 1),
    "rsr_key_cabinet": (["RGR", "RTR", "RRR"], {"R": "minecraft:red_dye", "G": "minecraft:glass_pane", "T": "minecraft:tripwire_hook"}, 1),
    "rsr_phone_station": (["YC", "YB"], {"Y": "minecraft:yellow_dye", "C": "jugcraft:basic_circuit", "B": "minecraft:stone_button"}, 1),
    **{k: (["PCP", "PIP"], {"P": "minecraft:paper", "C": "minecraft:blue_dye", "I": "minecraft:iron_nugget"}, 2) for k in SIGNS},
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
    PP.SEE_THROUGH |= SEE_THROUGH
    PP.SHARP |= SHARP
    PP.SHARP |= {f"pi_{KEY}_f_{p}" for p in range(6)}
    old = PP._sign_image

    def draw(kind):
        return sign_tex(kind) if kind in SIGNS else old(kind)
    PP._sign_image = draw
    PP.SIGNS.update(SIGNS)
    PP.SHARP |= {f"pi_{k}" for k in SIGNS}
    PP.RISERS.update({k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})


register()
