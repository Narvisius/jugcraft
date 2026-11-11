"""The training room (work list item 22 part B, room 10): the tower's flight school, to the standard Justin set on the
lab: everything at its real size (a pixel is 6.25 cm), modelled rather than painted on, the screens live in game,
varied.

Pieces: training_table (2 x 1, two students; what is on it a variant), instructor_desk (2 x 1: the teaching desk with
its document camera, laptop, AV touch panel and microphones), touch_display_cart (2 x 2: the interactive display on
its mobile stand), parts_board (3 x 2, on a wall: the parts of a drone mounted and labelled), build_table (2 x 1: a
student's build bench, its job a variant), controller_rack (2 x 1, on a wall: the training radios charging),
bag_cubbies (2 x 1: the students' bags at the door), classroom_poster (on a wall; which a variant), study_carrel (a
computer-based-training booth; its screen a variant). With them the briefing room's training chairs, the lab's stools,
LiPo cabinet and flight cases, the offices' whiteboard and notice board, the wall screen, the clock.
"""
from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import model_writer as MW
import training_kit as TK
import manager_kit as MK
import lab_kit as LK
import rest_kit as RK
import planning_kit as PK
import office_kit as OK
from training_kit import box, mat, faces, round_part, round_ring, cut_parts, swap_yz, TX, FONT, FONT_R, FACINGS
from lab_kit import swap_xy

ROOT = Path(__file__).resolve().parent.parent
A = SCR = PRT = None
_fit = MK._text_fit
rod_x = LK.rod_x
cable, cut_y = RK.cable, RK.cut_y

MATERIALS = {
    "cr_lam_white": lambda: PK.laminate((232, 232, 228), 4001), "cr_lam_grey": lambda: PK.laminate((150, 153, 158), 4002),
    "cr_lam_edge": lambda: TK.solid((96, 98, 102), 4003, 1.2), "cr_powder": lambda: TK.brushed((118, 122, 128), 4004, 1.6),
    "cr_fab_blue": lambda: MK.weave((52, 82, 122), 4005, 1, 4), "cr_fab_grey": lambda: MK.weave((104, 108, 114), 4006, 1, 4),
    "cr_ply": lambda: MK.wood((206, 176, 128), 4007, 30, (0.03, 0.07)), "cr_meter": lambda: TK.solid((236, 186, 36), 4008, 1.4),
    "cr_hivis": lambda: TK.solid((206, 236, 48), 4009, 2.0), "cr_reflect": lambda: TK.brushed((196, 198, 200), 4010, 2.0),
    "cr_bag_navy": lambda: MK.weave((36, 46, 70), 4011, 1, 3), "cr_bag_green": lambda: MK.weave((62, 86, 58), 4012, 1, 3),
    "cr_bag_red": lambda: MK.weave((150, 40, 40), 4013, 1, 3), "cr_jacket": lambda: MK.weave((44, 58, 92), 4014, 2, 4),
    "cr_lunch": lambda: MK.weave((40, 130, 128), 4015, 1, 3), "cr_cup": lambda: TK.solid((240, 240, 236), 4016, 1.0),
    "cr_sleeve": lambda: TK.solid((150, 112, 72), 4017, 2.0), "cr_led_green": lambda: TK.solid((90, 255, 120), 4018, 0.5),
    "cr_led_amber": lambda: TK.solid((255, 180, 40), 4019, 0.5), "cr_led_off": lambda: TK.solid((40, 44, 40), 4020, 0.5),
    "cr_tx_white": lambda: TK.solid((222, 224, 228), 4021, 1.2), "cr_tx_grey": lambda: TK.solid((96, 100, 106), 4022, 1.2),
    "cr_binder_navy": lambda: TK.solid((34, 52, 96), 4023, 1.2), "cr_binder_teal": lambda: TK.solid((28, 120, 120), 4024, 1.2),
    "cr_pcb_blue": lambda: TK.solid((40, 70, 150), 4025, 1.2), "cr_shrink": lambda: TK.solid((60, 120, 200), 4026, 1.0),
    "cr_chute": lambda: MK.weave((236, 120, 30), 4027, 1, 3), "cr_lens": lambda: TK.solid((30, 40, 60), 4028, 0.6),
    "cr_nav_red": lambda: TK.solid((255, 60, 50), 4029, 0.5), "cr_nav_green": lambda: TK.solid((60, 255, 110), 4030, 0.5),
    "cr_ring": lambda: TK.solid((250, 250, 240), 4031, 0.5), "cr_sponge": lambda: TK.solid((222, 190, 70), 4032, 3.0),
    "cr_anod_black": lambda: TK.brushed((34, 34, 38), 4033, 1.4), "cr_anod_red": lambda: TK.brushed((176, 34, 34), 4034, 2.0),
    "cr_anod_purple": lambda: TK.brushed((112, 62, 168), 4035, 2.0), "cr_batt_shell": lambda: TK.solid((112, 116, 122), 4036, 1.2),
    "cr_batt_cap": lambda: TK.solid((40, 42, 46), 4037, 1.0), "cr_gold": lambda: TK.brushed((212, 172, 70), 4038, 2.0),
    "cr_xt": lambda: TK.solid((232, 186, 36), 4039, 1.2), "cr_sil_red": lambda: TK.solid((176, 32, 30), 4040, 1.2),
    "cr_sil_black": lambda: TK.solid((28, 28, 30), 4041, 1.0), "cr_strap": lambda: MK.weave((26, 26, 28), 4042, 1, 3),
    "cr_tpu": lambda: TK.solid((236, 116, 30), 4043, 1.4), "cr_felt": lambda: MK.weave((198, 202, 208), 4044, 1, 3),
    "cr_balance": lambda: stripes(((200, 40, 40), (240, 240, 240), (40, 40, 40), (240, 200, 40), (40, 120, 220), (60, 170, 70), (230, 120, 30))),
    "cr_lipo": lambda: TK.solid((30, 32, 36), 4045, 1.0), "cr_lipo_cap": lambda: TK.solid((70, 74, 80), 4046, 1.0),
    "cr_magnet": lambda: TK.brushed((150, 152, 158), 4047, 2.5), "cr_white_mat": lambda: TK.solid((236, 238, 240), 4048, 1.0),
    "cr_heatsink": lambda: TK.brushed((170, 174, 180), 4049, 3.0),
}


def stripes(cols):
    """The balance lead's ribbon: its wires side by side."""
    img = Image.new("RGB", (16, 16))
    d = ImageDraw.Draw(img)
    w = 16 / len(cols)
    for k, c in enumerate(cols):
        d.rectangle((round(k * w), 0, round((k + 1) * w) - 1, 15), fill=c)
    return img


# ------------------------------------------------------------------ pictures
SCHOOL = "TRI-PILLAR FLIGHT SCHOOL"
NAVY = (26, 44, 84)


def _page(title, module, band=NAVY):
    """A training page's frame: the school's bar, the module, the title; returns (img, draw, y) to draw the rest from."""
    def frame(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(248, 248, 246))
        d.rectangle((0, 0, W, H * 0.11), fill=band)
        d.text((W * 0.02, H * 0.055), SCHOOL, font=_fit(d, SCHOOL, FONT, round(H * 0.06), W * 0.5), fill=(255, 255, 255), anchor="lm")
        d.text((W * 0.98, H * 0.055), module, font=_fit(d, module, FONT_R, round(H * 0.055), W * 0.4), fill=(220, 226, 236), anchor="rm")
        d.text((W * 0.04, H * 0.2), title, font=_fit(d, title, FONT, round(H * 0.085), W * 0.92), fill=(24, 30, 44), anchor="lm")
        return d
    return frame


def cbt(kind):
    """A student laptop's course page (live in game: the course software)."""
    def draw(img):
        W, H = img.size
        d = None
        if kind == "airspace":
            d = _page("AIRSPACE: WHERE YOU MAY FLY", "MODULE 2  /  LESSON 3")(img)
            for k, (lab, col) in enumerate((("CLASS C  CONTROLLED", (120, 150, 210)), ("CLASS D  TOWER ZONE", (150, 190, 230)),
                                            ("CLASS G  OPEN, UNDER 120 M", (190, 226, 196)))):
                y0 = H * (0.3 + k * 0.17)
                d.rectangle((W * 0.08 + k * W * 0.06, y0, W * 0.92 - k * W * 0.06, y0 + H * 0.14), fill=col, outline=(60, 70, 90))
                d.text((W * 0.5, y0 + H * 0.07), lab, font=_fit(d, lab, FONT, round(H * 0.06), W * 0.6), fill=(24, 30, 44), anchor="mm")
            d.polygon([(W * 0.46, H * 0.86), (W * 0.54, H * 0.86), (W * 0.5, H * 0.8)], fill=(200, 60, 40))
        elif kind == "quiz":
            d = _page("QUESTION 4 OF 10", "MODULE 3  /  QUIZ")(img)
            q = "A METAR reads 27015G25KT. The wind is:"
            d.text((W * 0.05, H * 0.32), q, font=_fit(d, q, FONT_R, round(H * 0.06), W * 0.9), fill=(24, 30, 44), anchor="lm")
            for k, t in enumerate(("from 270, 15 kt, gusting 25", "to 270, 15 kt", "from 150, 27 kt", "calm")):
                y = H * (0.45 + k * 0.11)
                sel = k == 0
                d.rectangle((W * 0.06, y - H * 0.04, W * 0.94, y + H * 0.04), fill=(214, 232, 255) if sel else (236, 236, 234), outline=(120, 140, 180))
                d.ellipse((W * 0.08, y - H * 0.02, W * 0.08 + H * 0.04, y + H * 0.02), fill=(30, 90, 200) if sel else (255, 255, 255), outline=(80, 90, 110))
                d.text((W * 0.16, y), t, font=_fit(d, t, FONT_R, round(H * 0.055), W * 0.7), fill=(24, 30, 44), anchor="lm")
        elif kind == "metar":
            d = _page("READING A METAR", "MODULE 3  /  LESSON 2")(img)
            m = "METAR TPT 141350Z 27015G25KT 9999 SCT030 14/06 Q1012"
            d.rectangle((W * 0.04, H * 0.27, W * 0.96, H * 0.37), fill=(30, 34, 40))
            d.text((W * 0.5, H * 0.32), m, font=_fit(d, m, FONT, round(H * 0.05), W * 0.88), fill=(140, 255, 160), anchor="mm")
            for k, (a, b) in enumerate((("27015G25KT", "wind 270, 15 kt, gusts 25"), ("9999", "visibility 10 km or more"),
                                        ("SCT030", "scattered cloud, 3000 ft"), ("14/06", "temperature 14, dew point 6"), ("Q1012", "pressure 1012 hPa"))):
                y = H * (0.45 + k * 0.1)
                d.text((W * 0.06, y), a, font=_fit(d, a, FONT, round(H * 0.05), W * 0.3), fill=(30, 90, 200), anchor="lm")
                d.text((W * 0.4, y), b, font=_fit(d, b, FONT_R, round(H * 0.05), W * 0.56), fill=(24, 30, 44), anchor="lm")
        elif kind == "preflight":
            d = _page("PRE-FLIGHT CHECKS", "MODULE 1  /  LESSON 5")(img)
            for k, (t, done) in enumerate((("Airframe: cracks, loose arms", True), ("Props: chips, tight", True), ("Battery: charged, no swelling", True),
                                           ("Radio: link, failsafe set", False), ("Weather: within limits", False), ("Airspace: clear, NOTAMs", False))):
                y = H * (0.32 + k * 0.1)
                d.rectangle((W * 0.06, y - H * 0.03, W * 0.06 + H * 0.06, y + H * 0.03), outline=(60, 70, 90), width=2)
                if done:
                    d.line((W * 0.06 + H * 0.01, y, W * 0.06 + H * 0.025, y + H * 0.02, W * 0.06 + H * 0.055, y - H * 0.025), fill=(30, 150, 70), width=3)
                d.text((W * 0.06 + H * 0.1, y), t, font=_fit(d, t, FONT_R, round(H * 0.055), W * 0.75), fill=(24, 30, 44), anchor="lm")
        elif kind == "lipo":
            d = _page("LIPO BATTERIES", "MODULE 4  /  LESSON 1")(img)
            for k in range(6):
                x0 = W * (0.08 + k * 0.14)
                d.rectangle((x0, H * 0.32, x0 + W * 0.12, H * 0.62), fill=(60, 64, 70), outline=(30, 30, 34))
                v = ("4.18", "4.17", "4.18", "4.16", "4.18", "4.17")[k]
                d.text((x0 + W * 0.06, H * 0.47), v + "V", font=_fit(d, "4.18V", FONT, round(H * 0.05), W * 0.1), fill=(140, 255, 160), anchor="mm")
            for k, t in enumerate(("6S: six cells, 22.2 V nominal", "Charge in a LiPo bag, never unattended", "Store at 3.8 V a cell")):
                d.text((W * 0.06, H * (0.72 + k * 0.08)), t, font=_fit(d, t, FONT_R, round(H * 0.05), W * 0.88), fill=(24, 30, 44), anchor="lm")
        elif kind == "radio":
            d = _page("RADIO CALLS", "MODULE 5  /  LESSON 2")(img)
            for k, t in enumerate(("TOWER, DRONE TP412, AT PAD 1,", "READY FOR DEPARTURE.", "", "TP412, CLEARED TO GO,", "CLIMB TO 120 METRES.")):
                d.text((W * 0.08, H * (0.34 + k * 0.08)), t, font=_fit(d, t or " ", FONT, round(H * 0.05), W * 0.84), fill=(30, 90, 200) if k < 2 else (150, 60, 30), anchor="lm")
            d.rectangle((W * 0.06, H * 0.8, W * 0.94, H * 0.9), fill=(232, 240, 250), outline=(120, 140, 180))
            d.text((W * 0.5, H * 0.85), "PRESS SPACE TO TALK", font=_fit(d, "PRESS SPACE TO TALK", FONT, round(H * 0.05), W * 0.6), fill=(30, 60, 120), anchor="mm")
        else:
            d = _page("SIGN IN", "STUDENT LOGIN", (40, 60, 70))(img)
            for k, t in enumerate(("Student ID", "Password")):
                y = H * (0.4 + k * 0.18)
                d.text((W * 0.2, y - H * 0.06), t, font=_fit(d, t, FONT_R, round(H * 0.05), W * 0.6), fill=(60, 70, 90), anchor="lm")
                d.rectangle((W * 0.2, y - H * 0.02, W * 0.8, y + H * 0.06), fill=(255, 255, 255), outline=(120, 130, 150))
            d.rectangle((W * 0.35, H * 0.8, W * 0.65, H * 0.9), fill=(30, 90, 200))
            d.text((W * 0.5, H * 0.85), "SIGN IN", font=_fit(d, "SIGN IN", FONT, round(H * 0.05), W * 0.25), fill=(255, 255, 255), anchor="mm")
        d.rectangle((0, H * 0.95, W * {"airspace": 0.4, "quiz": 0.45, "metar": 0.3, "preflight": 0.6, "lipo": 0.2, "radio": 0.7}.get(kind, 0), H), fill=(60, 160, 90))
        return img
    return draw


def lesson_slide(img):
    """The display cart's lesson (live in game): wind round buildings, the instructor's red pen over it."""
    W, H = img.size
    d = _page("LESSON 4: WIND ROUND BUILDINGS", "MODULE 3  /  WEATHER")(img)
    gy = H * 0.86
    d.line((W * 0.04, gy, W * 0.96, gy), fill=(90, 90, 96), width=3)
    for x0, x1, top in ((0.36, 0.5, 0.42), (0.58, 0.66, 0.6)):
        d.rectangle((W * x0, H * top, W * x1, gy), fill=(150, 156, 166), outline=(80, 84, 92), width=2)
    for k in range(4):
        y = H * (0.36 + k * 0.08)
        d.line((W * 0.06, y, W * 0.32, y), fill=(40, 110, 200), width=4)
        d.polygon([(W * 0.32, y - 7), (W * 0.34, y), (W * 0.32, y + 7)], fill=(40, 110, 200))
    d.arc((W * 0.5, H * 0.46, W * 0.6, H * 0.8), 200, 520, fill=(40, 110, 200), width=4)
    d.arc((W * 0.66, H * 0.56, W * 0.78, H * 0.84), 200, 520, fill=(40, 110, 200), width=4)
    d.text((W * 0.07, H * 0.3), "WIND", font=ImageFont.truetype(FONT, round(H * 0.045)), fill=(40, 110, 200))
    red = (220, 40, 40)
    d.ellipse((W * 0.49, H * 0.44, W * 0.62, H * 0.82), outline=red, width=5)
    d.line((W * 0.62, H * 0.5, W * 0.74, H * 0.36), fill=red, width=5)
    d.text((W * 0.75, H * 0.31), "TURBULENT!", font=ImageFont.truetype(FONT, round(H * 0.05)), fill=red)
    d.text((W * 0.75, H * 0.38), "fly upwind side", font=ImageFont.truetype(FONT_R, round(H * 0.04)), fill=red)
    for t, y in (("1. Wind speeds up over roofs", 0.62), ("2. Eddies form downwind", 0.68), ("3. Keep 3x the height away", 0.74)):
        d.text((W * 0.04, H * y), t, font=ImageFont.truetype(FONT_R, round(H * 0.035)), fill=(24, 30, 44))
    d.rectangle((W * 0.9, H * 0.9, W * 0.98, H * 0.97), fill=(230, 232, 236))
    d.text((W * 0.94, H * 0.935), "7 / 18", font=ImageFont.truetype(FONT_R, round(H * 0.03)), fill=(60, 64, 70), anchor="mm")
    return img


def doc_view(img):
    """The display cart (live): the document camera's view of the sheet on the desk, the instructor's pen at it."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(214, 216, 220))
    sw, sh = round(H * 0.82 * 0.71), round(H * 0.82)
    sheet = doc_sheet(Image.new("RGB", (sw, sh)))
    sd = ImageDraw.Draw(sheet)
    red = (210, 40, 40)
    sd.ellipse((sw * 0.06, sh * 0.12, sw * 0.42, sh * 0.36), outline=red, width=max(2, sw // 90))
    sd.text((sw * 0.44, sh * 0.16), "spins CW", font=ImageFont.truetype(FONT, max(8, round(sh * 0.035))), fill=red)
    img.paste(sheet.rotate(2.5, expand=False, fillcolor=(214, 216, 220)), (round((W - sw) / 2), round(H * 0.09)))
    d.polygon([(W * 0.66, H * 0.62), (W * 0.9, H * 0.98), (W * 0.86, H), (W * 0.64, H * 0.66)], fill=(40, 40, 46))
    d.rectangle((W * 0.02, H * 0.03, W * 0.16, H * 0.1), fill=(30, 34, 40))
    d.text((W * 0.09, H * 0.065), "DOC CAM", font=_fit(d, "DOC CAM", FONT, round(H * 0.045), W * 0.12), fill=(255, 255, 255), anchor="mm")
    return img


def presenter(img):
    """The instructor's laptop (live): the presenter view, this slide and the next, the notes, the clock."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(34, 36, 42))
    cur = lesson_slide(Image.new("RGB", (round(W * 0.6), round(H * 0.6))))
    img.paste(cur, (round(W * 0.03), round(H * 0.08)))
    nxt = Image.new("RGB", (round(W * 0.31), round(H * 0.3)), (248, 248, 246))
    dn = ImageDraw.Draw(nxt)
    dn.rectangle((0, 0, nxt.size[0], nxt.size[1] * 0.12), fill=NAVY)
    dn.text((4, nxt.size[1] * 0.3), "8: GUSTS", font=ImageFont.truetype(FONT, max(7, round(nxt.size[1] * 0.12))), fill=(24, 30, 44))
    img.paste(nxt, (round(W * 0.66), round(H * 0.08)))
    f = ImageFont.truetype(FONT_R, max(7, round(H * 0.05)))
    for k, t in enumerate(("Ask: who has flown near", "the hangar on a windy day?", "Show the clip after this.")):
        d.text((W * 0.66, H * (0.46 + k * 0.07)), t, font=f, fill=(220, 222, 226))
    d.text((W * 0.03, H * 0.76), "NOTES: eddies, rotor, wind shear", font=f, fill=(200, 202, 206))
    # live: the lesson's clock
    TK.live_text(img, (W * 0.97, H * 0.92), "00:24:16", ImageFont.truetype(FONT, max(7, round(H * 0.08))), (255, 255, 255), "rm",
                 bg=(34, 36, 42), src="timer", fmt="hms", period=3600)
    return img


def av_panel(img):
    """The AV touch panel (live): the display, its source, the volume, the microphones."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(18, 22, 30))
    d.text((W * 0.05, H * 0.1), "TRAINING ROOM", font=_fit(d, "TRAINING ROOM", FONT, round(H * 0.1), W * 0.6), fill=(230, 232, 236), anchor="lm")
    for k, (t, on) in enumerate((("LAPTOP", True), ("DOC CAM", False), ("CART", True))):
        x0 = W * (0.05 + k * 0.31)
        d.rectangle((x0, H * 0.24, x0 + W * 0.28, H * 0.48), fill=(40, 120, 220) if on else (46, 52, 64))
        d.text((x0 + W * 0.14, H * 0.36), t, font=_fit(d, t, FONT, round(H * 0.09), W * 0.24), fill=(255, 255, 255), anchor="mm")
    d.text((W * 0.05, H * 0.6), "VOLUME", font=_fit(d, "VOLUME", FONT_R, round(H * 0.08), W * 0.3), fill=(200, 204, 210), anchor="lm")
    d.rectangle((W * 0.35, H * 0.57, W * 0.95, H * 0.63), fill=(46, 52, 64))
    d.rectangle((W * 0.35, H * 0.57, W * 0.7, H * 0.63), fill=(90, 200, 120))
    for k, t in enumerate(("MIC 1", "MIC 2")):
        x0 = W * (0.05 + k * 0.46)
        d.rectangle((x0, H * 0.74, x0 + W * 0.42, H * 0.92), fill=(30, 110, 60) if k == 0 else (46, 52, 64))
        d.text((x0 + W * 0.21, H * 0.83), t + ("  ON" if k == 0 else "  OFF"), font=_fit(d, "MIC 1  OFF", FONT, round(H * 0.08), W * 0.38), fill=(255, 255, 255), anchor="mm")
    return img


def doc_sheet(img):
    """The page under the document camera: a drone's top view with its parts labelled, a pencil note."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(250, 250, 246))
    d.text((W / 2, H * 0.06), "THE QUADCOPTER", font=_fit(d, "THE QUADCOPTER", FONT, round(H * 0.05), W * 0.8), fill=(24, 30, 44), anchor="mm")
    cx, cy = W / 2, H * 0.45
    for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        d.line((cx, cy, cx + dx * W * 0.28, cy + dy * W * 0.28), fill=(40, 40, 44), width=4)
        d.ellipse((cx + dx * W * 0.28 - W * 0.13, cy + dy * W * 0.28 - W * 0.13, cx + dx * W * 0.28 + W * 0.13, cy + dy * W * 0.28 + W * 0.13), outline=(120, 120, 128), width=2)
    d.rectangle((cx - W * 0.08, cy - W * 0.1, cx + W * 0.08, cy + W * 0.1), fill=(60, 64, 70))
    f = ImageFont.truetype(FONT_R, max(7, round(H * 0.03)))
    for t, (x, y) in (("motor", (0.08, 0.2)), ("prop", (0.72, 0.18)), ("battery", (0.62, 0.5)), ("arm", (0.1, 0.62))):
        d.text((W * x, H * y), t, font=f, fill=(30, 60, 160))
    for k in range(6):
        y = H * (0.76 + k * 0.035)
        d.line((W * 0.08, y, W * 0.92, y), fill=(200, 200, 204), width=1)
    d.text((W * 0.1, H * 0.77), "CW / CCW: props turn opposite ways", font=f, fill=(90, 90, 96))
    return img


def exam(kind):
    """A carrel's screen (live in game): the theory exam, its result, the sign-in, a practice quiz."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(10, 10, 12))
        b = round(W * 0.02)
        inner = Image.new("RGB", (W - 2 * b, H - 2 * b))
        if kind == "exam":
            f = _page("QUESTION 12 OF 40", "UAS THEORY EXAM  /  31:20 LEFT", (110, 40, 40))
            dd = f(inner)
            IW, IH = inner.size
            q = "Your drone loses its GPS fix. You should first:"
            dd.text((IW * 0.05, IH * 0.32), q, font=_fit(dd, q, FONT_R, round(IH * 0.06), IW * 0.9), fill=(24, 30, 44), anchor="lm")
            for k, t in enumerate(("A  land at once, wherever it is", "B  hold height and fly by sight", "C  climb to find the satellites", "D  switch the radio off")):
                y = IH * (0.46 + k * 0.11)
                dd.rectangle((IW * 0.06, y - IH * 0.04, IW * 0.94, y + IH * 0.04), fill=(214, 232, 255) if k == 1 else (236, 236, 234), outline=(140, 150, 170))
                dd.text((IW * 0.09, y), t, font=_fit(dd, t, FONT_R, round(IH * 0.055), IW * 0.8), fill=(24, 30, 44), anchor="lm")
        elif kind == "pass":
            f = _page("EXAM COMPLETE", "UAS THEORY EXAM", (30, 110, 60))
            dd = f(inner)
            IW, IH = inner.size
            dd.text((IW / 2, IH * 0.48), "36 / 40", font=_fit(dd, "36 / 40", FONT, round(IH * 0.2), IW * 0.6), fill=(30, 110, 60), anchor="mm")
            dd.text((IW / 2, IH * 0.68), "PASS  (pass mark 30)", font=_fit(dd, "PASS  (pass mark 30)", FONT_R, round(IH * 0.07), IW * 0.7), fill=(24, 30, 44), anchor="mm")
            dd.text((IW / 2, IH * 0.82), "Please see your instructor", font=_fit(dd, "Please see your instructor", FONT_R, round(IH * 0.055), IW * 0.7), fill=(90, 96, 104), anchor="mm")
        elif kind == "practice":
            cbt("quiz")(inner)
        else:
            cbt("login")(inner)
        img.paste(inner, (b, b))
        if kind == "exam":
            # live: the time left (a 45 minute exam)
            IW, IH = inner.size
            module, part = "UAS THEORY EXAM  /  31:20 LEFT", "31:20"
            dd = ImageDraw.Draw(inner)
            fm = _fit(dd, module, FONT_R, round(IH * 0.055), IW * 0.4)
            x = dd.textbbox((IW * 0.98, IH * 0.055), module, font=fm, anchor="rm")[0] + dd.textlength(module[:module.index(part)], font=fm)
            TK.live_text(img, (b + x, b + IH * 0.055), part, fm, (220, 226, 236), "lm", bg=(110, 40, 40), bake=False, src="timer", down=True, period=2700)
        return img
    return draw


POSTERS = ("airspace", "phonetic", "preflight", "lipo", "metar")


def poster_print(kind):
    """A classroom poster (A1, printed): airspace classes, the phonetic alphabet, the pre-flight check, LiPo safety,
    how to read a METAR."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(250, 250, 246))
        band = {"airspace": (30, 70, 140), "phonetic": (40, 40, 48), "preflight": (30, 110, 60), "lipo": (190, 50, 30), "metar": (20, 110, 140)}[kind]
        title = {"airspace": "AIRSPACE", "phonetic": "PHONETIC ALPHABET", "preflight": "PRE-FLIGHT CHECK", "lipo": "LIPO SAFETY", "metar": "READING A METAR"}[kind]
        d.rectangle((0, 0, W, H * 0.11), fill=band)
        d.text((W / 2, H * 0.055), title, font=_fit(d, title, FONT, round(H * 0.06), W * 0.9), fill=(255, 255, 255), anchor="mm")
        f = ImageFont.truetype(FONT_R, max(7, round(H * 0.028)))
        fb = ImageFont.truetype(FONT, max(7, round(H * 0.03)))
        if kind == "airspace":
            for k, (lab, h, col) in enumerate((("CLASS C", "above 1500 m", (120, 150, 210)), ("CLASS D", "tower zone, 0 to 1500 m", (150, 190, 230)),
                                               ("CLASS G", "open, drones under 120 m", (190, 226, 196)))):
                y0 = H * (0.16 + k * 0.2)
                d.rectangle((W * 0.08, y0, W * 0.92, y0 + H * 0.17), fill=col, outline=(60, 70, 90), width=2)
                d.text((W * 0.12, y0 + H * 0.05), lab, font=fb, fill=(24, 30, 44))
                d.text((W * 0.12, y0 + H * 0.1), h, font=f, fill=(24, 30, 44))
            d.text((W * 0.08, H * 0.8), "Ask the tower before you enter D.", font=f, fill=(24, 30, 44))
            d.text((W * 0.08, H * 0.85), "Never above 120 m without a permit.", font=f, fill=(180, 40, 30))
        elif kind == "phonetic":
            words = ("ALFA BRAVO CHARLIE DELTA ECHO FOXTROT GOLF HOTEL INDIA JULIETT KILO LIMA MIKE NOVEMBER OSCAR PAPA QUEBEC "
                     "ROMEO SIERRA TANGO UNIFORM VICTOR WHISKEY XRAY YANKEE ZULU").split()
            for k, w in enumerate(words):
                col, row = k // 13, k % 13
                x, y = W * (0.08 + col * 0.46), H * (0.15 + row * 0.064)
                d.text((x, y), w[0], font=ImageFont.truetype(FONT, max(7, round(H * 0.045))), fill=band)
                d.text((x + W * 0.1, y + H * 0.008), w, font=fb, fill=(24, 30, 44))
        elif kind == "preflight":
            for k, t in enumerate(("AIRFRAME", "PROPS", "BATTERY", "RADIO LINK", "FAILSAFE", "COMPASS", "WEATHER", "AIRSPACE", "NOTAMS", "BRIEFING")):
                y = H * (0.16 + k * 0.075)
                d.rectangle((W * 0.08, y, W * 0.08 + H * 0.045, y + H * 0.045), outline=(40, 40, 48), width=2)
                d.text((W * 0.08 + H * 0.07, y), t, font=fb, fill=(24, 30, 44))
            d.text((W * 0.08, H * 0.92), "If in doubt, don't fly.", font=fb, fill=band)
        elif kind == "lipo":
            for k, t in enumerate(("Charge in a LiPo bag", "Never leave it charging", "Puffy? Stop. Isolate it.", "Store at 3.8 V a cell",
                                   "No bare leads touching", "Fire: sand bucket, then call")):
                y = H * (0.16 + k * 0.13)
                d.ellipse((W * 0.08, y, W * 0.08 + H * 0.09, y + H * 0.09), fill=band)
                d.text((W * 0.08 + H * 0.045, y + H * 0.045), str(k + 1), font=fb, fill=(255, 255, 255), anchor="mm")
                d.text((W * 0.08 + H * 0.12, y + H * 0.045), t, font=fb, fill=(24, 30, 44), anchor="lm")
        else:
            m = "TPT 141350Z 27015G25KT 9999 SCT030 14/06 Q1012"
            d.rectangle((W * 0.05, H * 0.15, W * 0.95, H * 0.22), fill=(30, 34, 40))
            d.text((W / 2, H * 0.185), m, font=_fit(d, m, FONT, round(H * 0.03), W * 0.86), fill=(140, 255, 160), anchor="mm")
            for k, (a, b) in enumerate((("TPT", "the station"), ("141350Z", "14th, 13:50 UTC"), ("27015G25KT", "wind 270, 15 gust 25 kt"),
                                        ("9999", "10 km or more"), ("SCT030", "scattered, 3000 ft"), ("14/06", "temp / dew point"), ("Q1012", "pressure, hPa"))):
                y = H * (0.28 + k * 0.09)
                d.text((W * 0.06, y), a, font=fb, fill=band)
                d.text((W * 0.06, y + H * 0.035), b, font=f, fill=(24, 30, 44))
        return img
    return draw


def header_print(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=NAVY)
    d.text((W * 0.03, H / 2), "PARTS OF A DRONE", font=_fit(d, "PARTS OF A DRONE", FONT, round(H * 0.56), W * 0.5), fill=(255, 255, 255), anchor="lm")
    t = "the T4 trainer, taken apart"
    d.text((W * 0.97, H / 2), t, font=_fit(d, t, FONT_R, round(H * 0.4), W * 0.4), fill=(200, 210, 230), anchor="rm")
    return img


def plate(text, n=None):
    """A part's label on the board: its number in a navy circle, its name."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(246, 244, 236))
        x = 4
        if n is not None:
            d.ellipse((2, 2, H - 2, H - 2), fill=NAVY)
            d.text((H / 2, H / 2), str(n), font=_fit(d, "12", FONT, round(H * 0.6), H * 0.7), fill=(255, 255, 255), anchor="mm")
            x = H + 2
        d.text((x, H / 2), text, font=_fit(d, text, FONT, round(H * 0.56), W - x - 3), fill=(24, 30, 44), anchor="lm")
        return img
    return draw


NAMES = ("AVA", "LIAM", "NOOR", "MATEO", "PRIYA", "JONAS", "ZOE", "KAI", "IMANI", "FINN", "LUCIA", "OMAR", "HANA", "LEO", "SOFIA", "RAFE",
         "MEI", "TOBY", "ELLA", "SAM", "NIA", "OSCAR", "IVY", "ARJUN")


def tent(name):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(250, 250, 246))
        d.rectangle((0, H * 0.82, W, H), fill=NAVY)
        d.text((W / 2, H * 0.42), name, font=_fit(d, name, FONT, round(H * 0.56), W * 0.86), fill=(24, 30, 44), anchor="mm")
        return img
    return draw


def binder_cover(module, colour):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=colour)
        d.rectangle((W * 0.08, H * 0.1, W * 0.92, H * 0.5), fill=(250, 250, 246))
        d.text((W / 2, H * 0.22), "GROUND SCHOOL", font=_fit(d, "GROUND SCHOOL", FONT, round(H * 0.1), W * 0.76), fill=(24, 30, 44), anchor="mm")
        d.text((W / 2, H * 0.38), module, font=_fit(d, module, FONT_R, round(H * 0.08), W * 0.7), fill=(24, 30, 44), anchor="mm")
        d.text((W / 2, H * 0.86), SCHOOL, font=_fit(d, SCHOOL, FONT, round(H * 0.05), W * 0.84), fill=(230, 234, 240), anchor="mm")
        return img
    return draw


def workbook(seed):
    """An open workbook: notes in pencil on the left page, a diagram on the right."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(250, 250, 244))
        d.line((W / 2, 0, W / 2, H), fill=(200, 200, 196), width=2)
        rng = random.Random(seed)
        for k in range(10):
            y = H * (0.1 + k * 0.08)
            d.line((W * 0.05, y, W * 0.45, y), fill=(206, 214, 230), width=1)
            d.line((W * 0.06, y - 2, W * (0.1 + rng.uniform(0.15, 0.33)), y - 2), fill=(110, 110, 116), width=1)
        cx, cy = W * 0.75, H * 0.45
        d.ellipse((cx - W * 0.15, cy - H * 0.2, cx + W * 0.15, cy + H * 0.2), outline=(60, 60, 66), width=2)
        d.line((cx - W * 0.15, cy, cx + W * 0.15, cy), fill=(60, 60, 66), width=1)
        d.text((cx, H * 0.85), "fig. %d" % (seed % 9 + 1), font=ImageFont.truetype(FONT_R, max(7, round(H * 0.06))), fill=(90, 90, 96), anchor="mm")
        return img
    return draw


def tx_screen(n, batt):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(14, 22, 30))
        d.text((W * 0.06, H * 0.28), f"TX {n:02d}", font=_fit(d, "TX 06", FONT, round(H * 0.32), W * 0.5), fill=(230, 236, 240), anchor="lm")
        fb = _fit(d, "100%", FONT, round(H * 0.3), W * 0.4)
        colour = (120, 255, 150) if batt > 50 else (255, 190, 60)
        if batt:
            # live: the battery running down through the lessons (two hours a charge)
            TK.live_text(img, (W * 0.94, H * 0.28), f"{batt}%", fb, colour, "rm", bg=(14, 22, 30), widest="100%", src="value", base=batt,
                         rate=-0.003, cycle=7200, min=5, max=100, unit="%")
        else:
            d.text((W * 0.94, H * 0.28), f"{batt}%", font=fb, fill=colour, anchor="rm")
        d.text((W * 0.06, H * 0.72), "T1 TRAINER", font=_fit(d, "T1 TRAINER", FONT_R, round(H * 0.28), W * 0.88), fill=(170, 186, 200), anchor="lm")
        return img
    return draw


def meter_face(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(40, 40, 44))
    d.rectangle((W * 0.1, H * 0.06, W * 0.9, H * 0.34), fill=(170, 186, 150))
    TK.live_text(img, (W * 0.5, H * 0.2), "24.81", _fit(d, "24.81", FONT, round(H * 0.2), W * 0.7), (30, 34, 30), "mm", bg=(170, 186, 150),
                 src="value", base=24.81, spread=0.03, dp=2, period=6)
    d.ellipse((W * 0.2, H * 0.42, W * 0.8, H * 0.82), fill=(30, 30, 34), outline=(120, 120, 126), width=2)
    d.line((W * 0.5, H * 0.62, W * 0.68, H * 0.48), fill=(230, 230, 230), width=2)
    for k, t in enumerate(("V", "A", "OHM")):
        d.text((W * (0.2 + k * 0.3), H * 0.9), t, font=_fit(d, "OHM", FONT, round(H * 0.07), W * 0.25), fill=(230, 230, 230), anchor="mm")
    return img


def fc_config(img):
    """The build bench laptop (live): the flight controller set-up, its motors' test."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(36, 38, 44))
    d.rectangle((0, 0, W, H * 0.1), fill=(240, 180, 40))
    d.text((W * 0.03, H * 0.05), "FC SETUP  /  MOTORS", font=_fit(d, "FC SETUP  /  MOTORS", FONT, round(H * 0.06), W * 0.6), fill=(30, 30, 34), anchor="lm")
    for k in range(4):
        x0 = W * (0.1 + k * 0.2)
        d.rectangle((x0, H * 0.22, x0 + W * 0.1, H * 0.78), fill=(56, 60, 68))
        lvl = (0.3, 0.3, 0.55, 0.3)[k]
        d.rectangle((x0, H * (0.78 - 0.56 * lvl), x0 + W * 0.1, H * 0.78), fill=(240, 180, 40))
        # live: the motor test's throttles
        TK.live(img, "bar", (x0, H * 0.22, x0 + W * 0.1, H * 0.78), bg=[56, 60, 68], fg=[240, 180, 40], vertical=True, src="value",
                base=lvl * 100, spread=12, period=3 + k, min=0, max=100)
        d.text((x0 + W * 0.05, H * 0.86), f"M{k + 1}", font=_fit(d, "M4", FONT, round(H * 0.06), W * 0.1), fill=(220, 220, 226), anchor="mm")
    d.text((W * 0.97, H * 0.2), "PROPS OFF!", font=_fit(d, "PROPS OFF!", FONT, round(H * 0.06), W * 0.3), fill=(255, 90, 80), anchor="rm")
    return img


def build_sheet(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(250, 250, 246))
    d.rectangle((0, 0, W, H * 0.12), fill=(240, 180, 40))
    d.text((W / 2, H * 0.06), "BUILD SHEET: T1 TRAINER", font=_fit(d, "BUILD SHEET: T1 TRAINER", FONT, round(H * 0.06), W * 0.9), fill=(30, 30, 34), anchor="mm")
    d.text((W / 2, H * 0.18), "STEP 4 OF 9: MOTORS", font=_fit(d, "STEP 4 OF 9: MOTORS", FONT, round(H * 0.05), W * 0.8), fill=(24, 30, 44), anchor="mm")
    cx, cy = W / 2, H * 0.48
    d.line((cx - W * 0.3, cy - W * 0.3, cx + W * 0.3, cy + W * 0.3), fill=(40, 40, 44), width=4)
    d.line((cx - W * 0.3, cy + W * 0.3, cx + W * 0.3, cy - W * 0.3), fill=(40, 40, 44), width=4)
    for dx, dy, t in ((-1, -1, "CCW"), (1, -1, "CW"), (-1, 1, "CW"), (1, 1, "CCW")):
        x, y = cx + dx * W * 0.3, cy + dy * W * 0.3
        d.ellipse((x - W * 0.07, y - W * 0.07, x + W * 0.07, y + W * 0.07), outline=(30, 90, 200), width=3)
        d.text((x, y + W * 0.12), t, font=ImageFont.truetype(FONT, max(7, round(H * 0.035))), fill=(30, 90, 200), anchor="mm")
    for k, t in enumerate(("M3 x 8 screws, blue threadlock", "Wires out toward the arm tip", "Check: shaft spins freely")):
        d.text((W * 0.06, H * (0.8 + k * 0.06)), t, font=ImageFont.truetype(FONT_R, max(7, round(H * 0.035))), fill=(24, 30, 44))
    return img


def cubby_label(name):
    return PK.name_plate(name, (250, 250, 246), (24, 30, 44))


def rack_strip(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(30, 32, 36))
    for k in range(6):
        x = W * (k + 0.5) / 6
        d.text((x, H / 2), f"TX {k + 1:02d}", font=_fit(d, "TX 06", FONT, round(H * 0.62), W / 6 * 0.6), fill=(236, 236, 236), anchor="mm")
    return img


def decal(img):
    """The school's mark on the teaching desk's front: three pillars, the name."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(52, 56, 62))
    cx, cy = W * 0.5, H * 0.36
    for dx in (-1, 0, 1):
        d.rectangle((cx + dx * H * 0.12 - H * 0.04, cy - H * 0.18, cx + dx * H * 0.12 + H * 0.04, cy + H * 0.18), fill=(236, 236, 236))
    d.text((W / 2, H * 0.74), SCHOOL, font=_fit(d, SCHOOL, FONT, round(H * 0.14), W * 0.8), fill=(236, 236, 236), anchor="mm")
    return img


def handout(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(250, 250, 246))
    d.text((W / 2, H * 0.1), "HANDOUT: MODULE 3", font=_fit(d, "HANDOUT: MODULE 3", FONT, round(H * 0.07), W * 0.86), fill=(24, 30, 44), anchor="mm")
    for k in range(9):
        y = H * (0.22 + k * 0.08)
        d.line((W * 0.08, y, W * (0.5 + 0.4 * ((k * 7) % 5) / 5), y), fill=(150, 150, 156), width=1)
    return img


def carrel_tag(n):
    return PK.name_plate(f"C{n}", (40, 44, 50), (240, 240, 240))


# ------------------------------------------------------------------ small things
def castor(x0, z0):
    """A small castor (5 cm wheel) under a foot's end: its fork, the wheel."""
    return [box((x0 + 0.15, 0.45, z0 + 0.1), (x0 + 0.65, 0.85, z0 + 0.7), mat("chrome"))] + rod_x(x0 + 0.2, x0 + 0.6, 0.4, z0 + 0.4, 0.4, "grip_black", n=3)


def laptop(x0, z0, y0, key, draw, kind="graphite", w=5.4, d=3.6, lid=3.4):
    return LK.laptop_at(x0, z0, y0, key, draw, w, d, lid, kind)


def bottle(cx, cz, y0, colour):
    return (round_part(cx, cz, 0.56, y0, y0 + 3.6, colour) + round_part(cx, cz, 0.4, y0 + 3.6, y0 + 4.2, "black")
            + [box((cx - 0.1, y0 + 4.2, cz - 0.45), (cx + 0.1, y0 + 4.45, cz + 0.45), mat("black"))])


def cup(cx, cz, y0):
    """A takeaway coffee: the paper cup, its card sleeve, the lid."""
    return (round_part(cx, cz, 0.6, y0, y0 + 1.75, "cup") + round_part(cx, cz, 0.66, y0 + 0.5, y0 + 1.2, "sleeve")
            + round_part(cx, cz, 0.7, y0 + 1.75, y0 + 1.95, "white"))


def pen(x0, z0, y0, colour="navy", ln=2.2):
    return [box((x0, y0, z0), (x0 + 0.18, y0 + 0.18, z0 + ln), mat(colour)), box((x0, y0, z0 + ln), (x0 + 0.18, y0 + 0.18, z0 + ln + 0.25), mat("chrome"))]


def binder(x0, z0, y0, module, colour, rgb):
    """A ring binder lying shut: its spine (west), the boards, the pages' edges between them, the cover."""
    w, d = 4.6, 5.0
    cover = PRT.face(f"cr_binder_{module}_{colour}", w - 0.25, d, binder_cover(module, rgb))
    return [box((x0, y0, z0), (x0 + 0.3, y0 + 0.8, z0 + d), mat(colour)),
            box((x0 + 0.3, y0, z0), (x0 + w, y0 + 0.1, z0 + d), mat(colour)),
            box((x0 + 0.5, y0 + 0.1, z0 + 0.2), (x0 + w - 0.2, y0 + 0.68, z0 + d - 0.2), mat("paper")),
            box((x0 + 0.3, y0 + 0.68, z0), (x0 + w, y0 + 0.8, z0 + d), faces(colour, up=cover))]


def open_book(x0, z0, y0, seed):
    """An open workbook: its pages (notes left, a figure right) on its card covers."""
    w, d = 4.8, 3.4
    return [box((x0, y0, z0), (x0 + w, y0 + 0.08, z0 + d), mat("navy")),
            box((x0 + 0.1, y0 + 0.08, z0 + 0.08), (x0 + w - 0.1, y0 + 0.28, z0 + d - 0.08), faces("paper", up=PRT.face(f"cr_book_{seed}", w - 0.2, d - 0.16, workbook(seed))))]


def name_tent(x0, zf, y0, name):
    """A folded name card standing on the table: the name on both faces, toward the front and toward its owner."""
    pic = PRT.face(f"cr_tent_{name}", 3.2, 1.2, tent(name))
    zb = zf + 0.92
    return [box((x0, y0, zf), (x0 + 3.2, y0 + 1.2, zf + 0.06), {**mat("paper"), "north": pic}, {"angle": 22.5, "axis": "x", "origin": [x0 + 1.6, y0, zf]}),
            box((x0, y0, zb - 0.06), (x0 + 3.2, y0 + 1.2, zb), {**mat("paper"), "south": pic}, {"angle": -22.5, "axis": "x", "origin": [x0 + 1.6, y0, zb]})]


def phone(x0, z0, y0):
    return [box((x0, y0, z0), (x0 + 1.1, y0 + 0.14, z0 + 2.3), mat("black")), box((x0 + 0.08, y0 + 0.14, z0 + 0.1), (x0 + 1.02, y0 + 0.16, z0 + 2.2), mat("glassblack"))]


def _tx_local(colour, key, draw):
    """A training radio in its own frame: u across (-1.45..1.45), v up its face (0..2.9 with the aerial), w out of its
    face (0 its back): the body, the grips, the face plate and its screen, the two sticks on their gimbals, the
    switches, the aerial, the handle. (name, from, to, texture, picture on its front)"""
    out = [((-1.0, 0.2, 0.0), (1.0, 2.6, 0.8), colour, None), ((-1.45, 0.0, 0.0), (-1.0, 2.3, 0.9), "grip_black", None),
           ((1.0, 0.0, 0.0), (1.45, 2.3, 0.9), "grip_black", None), ((-1.0, 0.25, 0.8), (1.0, 2.55, 0.86), "black", None),
           ((-0.55, 1.65, 0.86), (0.55, 2.35, 0.88), "black", (key, draw))]
    for c in (-0.62, 0.62):
        out += [((c - 0.36, 0.6, 0.86), (c + 0.36, 1.32, 0.92), "graphite", None), ((c - 0.06, 0.9, 0.92), (c + 0.06, 1.02, 1.45), "chrome", None),
                ((c - 0.13, 0.83, 1.45), (c + 0.13, 1.09, 1.62), "black", None)]
    for c in (-0.75, -0.4, 0.4, 0.75):
        out.append(((c - 0.05, 2.6, 0.35), (c + 0.05, 2.82, 0.45), "chrome", None))
    out += [((-0.07, 2.6, 0.15), (0.07, 3.7, 0.29), "black", None), ((-0.55, 2.6, 0.0), (0.55, 2.75, 0.12), "black", None)]
    return out


def radio_lying(cx, cz, y0, colour, key, draw):
    """A radio lying face up on a table, its top away from the student (north)."""
    out = []
    for (u0, v0, w0), (u1, v1, w1), tex, pic in _tx_local(colour, key, draw):
        fc = mat(tex)
        if pic:
            fc["up"] = SCR.face(pic[0], u1 - u0, v1 - v0, pic[1])
        out.append(box((cx + u0, y0 + w0, cz + 1.4 - v1), (cx + u1, y0 + w1, cz + 1.4 - v0), fc))
    return out


def radio_standing(cx, zb, y0, colour, key, draw):
    """A radio standing on a shelf leaning back on its back's bottom edge (its face north)."""
    out = []
    rot = {"angle": 22.5, "axis": "x", "origin": [cx, y0, zb]}
    for (u0, v0, w0), (u1, v1, w1), tex, pic in _tx_local(colour, key, draw):
        fc = mat(tex)
        if pic:
            fc["north"] = SCR.face(pic[0], u1 - u0, v1 - v0, pic[1])
        out.append(box((cx + u0, y0 + v0, zb - w1), (cx + u1, y0 + v1, zb - w0), fc, dict(rot)))
    return out


# ------------------------------------------------------------------ the students' tables
TT = 11.84            # a table's top (74 cm)


def table_frame():
    """A flip-top training table (160 by 60 cm, built along x 0..32, its students to the south looking north): the
    laminate top with its grey edge, the two T-legs on castors with the flip brackets and their red release tabs, the
    rail between, the perforated modesty panel at the front, the pop-up power box."""
    top = {**mat("lam_edge"), "up": {"texture": "#lam_white"}, "down": {"texture": "#lam_grey"}}
    out = [box((3.2, TT - 0.4, 6.0), (28.8, TT, 15.6), top)]
    for x in (5.2, 25.8):
        out += [box((x, 1.4, 10.0), (x + 1.0, TT - 0.9, 11.0), mat("powder")),
                box((x - 0.1, 0.85, 6.6), (x + 1.1, 1.4, 15.0), mat("powder")),
                box((x - 0.2, TT - 0.9, 7.0), (x + 1.2, TT - 0.4, 15.0), mat("powder")),
                box((x + 0.2, TT - 1.3, 14.6), (x + 0.8, TT - 0.9, 15.4), mat("red"))]
        out += castor(x + 0.1, 6.6) + castor(x + 0.1, 14.2)
    out += [box((6.2, TT - 1.5, 10.2), (25.8, TT - 0.9, 10.8), mat("powder")),
            box((6.2, 5.4, 6.2), (25.8, 10.6, 6.5), mat("perf")),
            box((5.6, 8.0, 6.5), (6.2, 8.6, 10.0), mat("powder")), box((25.8, 8.0, 6.5), (26.4, 8.6, 10.0), mat("powder")),
            box((14.6, TT, 12.8), (17.4, TT + 0.18, 14.2), mat("graphite")), box((14.8, TT + 0.18, 13.0), (17.2, TT + 0.22, 14.0), mat("black"))]
    return out


TABLE_PLACES = (
    # (left place, right place): each student's (main thing, its page, a notepad?, one more thing)
    (("laptop", "airspace", "pad", "bottle"), ("laptop", "quiz", None, "cup")),
    (("book", None, None, "radio"), ("laptop", "metar", "pad", "bottle")),
    (("laptop", "preflight", "pad", "phone"), ("laptop", "lipo", None, "cup")),
    (("laptop", "radio", None, "bottle"), ("shut", None, "pad", "radio")),
    (("laptop", "login", "pad", "cup"), ("laptop", "airspace", None, "phone")),
    (("binder", None, "pad", "bottle"), ("laptop", "preflight", "pad", "radio")),
)


def training_table(v):
    """The training table with its two students' things (variant v): a laptop open on the course (live) or shut, the
    course binder or an open workbook, a notepad and pen, a bottle, a coffee, a phone or a training radio; each a
    folded name card at the front."""
    out = table_frame()
    for side, (x0, place) in enumerate(((3.6, TABLE_PLACES[v][0]), (16.4, TABLE_PLACES[v][1]))):
        main, page, pad, extra = place
        if main == "laptop":
            out += laptop(x0 + 3.4, 9.2, TT, f"cr_cbt_{page}", cbt(page), ("graphite", "silver", "black")[(v + side) % 3])
            if (v + side) % 2 == 0:
                # its charger's lead along the table to the power box
                if side == 0:
                    out += cable([(x0 + 8.8, TT + 0.12, 11.0), (14.8, TT + 0.12, 11.0), (14.8, TT + 0.12, 12.8)], 0.06, "black")
                else:
                    out += cable([(x0 + 3.4, TT + 0.12, 9.6), (16.0, TT + 0.12, 9.6), (16.0, TT + 0.12, 12.8)], 0.06, "black")
        elif main == "shut":
            out += [box((x0 + 3.4, TT, 9.4), (x0 + 8.8, TT + 0.55, 13.0), faces("alu", up=A.region("cr_shut", 5.4, 3.6, TK.lid_back("silver"))))]
        elif main == "book":
            out += open_book(x0 + 3.6, 10.2, TT, 4300 + v * 2 + side)
        else:
            out += binder(x0 + 3.8, 9.0, TT, "MODULE %d" % (1 + (v + side) % 5), ("binder_navy", "binder_teal")[side], ((34, 52, 96), (28, 120, 120))[side])
        if pad:
            out += [box((x0 + 0.2, TT, 10.6), (x0 + 2.6, TT + 0.22, 14.0), faces("paper", up=PRT.face(f"cr_pad_{v}_{side}", 2.4, 3.4, workbook(4340 + v * 2 + side))))]
            out += pen(x0 + 2.75, 11.2, TT, ("navy", "black", "red")[(v + side) % 3])
        if extra == "bottle":
            out += bottle(x0 + 11.4, 7.6, TT, ("teal", "navy", "orange", "red")[(v + side) % 4])
        elif extra == "cup":
            out += cup(x0 + 11.4, 7.8, TT)
        elif extra == "phone":
            out += phone(x0 + 9.8, 11.4, TT)
        elif extra == "radio":
            out += radio_lying(x0 + 10.9, 11.4, TT, ("tx_grey", "tx_white")[side], f"cr_txs_{v}{side}", tx_screen(3 + side + v % 3, 60 + 7 * v))
        out += name_tent(x0 + 0.4, 6.3, TT, NAMES[(v * 2 + side) % len(NAMES)])
    return out


# ------------------------------------------------------------------ the instructor's desk
DT = 12.2             # the teaching desk's top (76 cm)


def instructor_desk():
    """The teaching desk (170 by 75 cm, built along x 0..32, the instructor to the south facing the class to the
    north): its white top on two grey pedestals (the computer's cupboard with its vented door, three drawers), the front
    panel with the school's mark toward the class; on it the document camera over a lesson sheet (its light on), the
    instructor's laptop (live: the presenter view), the AV touch panel (live), the microphones in their charger, the
    clicker, markers, handouts, a mug; the cables down through the grommet."""
    top = {**mat("lam_edge"), "up": {"texture": "#lam_white"}, "down": {"texture": "#lam_grey"}}
    out = [box((2.4, DT - 0.4, 3.2), (29.6, DT, 15.2), top)]
    # the pedestals on their plinths
    for x0, x1 in ((2.6, 10.4), (21.6, 29.4)):
        out += [box((x0 + 0.3, 0, 3.9), (x1 - 0.3, 0.6, 14.6), mat("black")), box((x0, 0.6, 3.6), (x1, DT - 0.4, 15.0), mat("lam_grey"))]
    # the cupboard's door (vented) and its pull; the drawers and theirs
    out += [box((2.8, 1.0, 15.0), (10.2, DT - 0.8, 15.3), faces("lam_grey", south={"texture": "#perf"})),
            box((9.2, 5.0, 15.3), (9.5, 8.6, 15.6), mat("chrome"))]
    for k, (y0, y1) in enumerate(((8.4, DT - 0.8), (4.8, 8.2), (1.0, 4.6))):
        out += [box((21.8, y0, 15.0), (29.2, y1, 15.3), mat("lam_grey")), box((23.6, y1 - 1.0, 15.3), (27.4, y1 - 0.7, 15.6), mat("chrome"))]
    # the front panel toward the class, the school's mark on it
    out += [box((2.4, 2.0, 3.2), (29.6, DT - 0.4, 3.6), faces("graphite", north=PRT.face("cr_decal", 27.2, DT - 2.4, decal)))]
    # the document camera: its base and buttons, the post, the knuckle, the arm out over the sheet, the camera head
    # with its lens looking down and its two lamp pads lit, the sheet under it
    out += [box((3.2, DT, 4.6), (6.8, DT + 0.35, 8.2), mat("anod_black"))]
    for k in range(4):
        out.append(box((3.6 + k * 0.7, DT + 0.35, 7.6), (4.0 + k * 0.7, DT + 0.42, 7.9), mat("graphite")))
    out += round_part(5.0, 5.4, 0.2, DT + 0.35, DT + 3.4, "anod_black", n=3)
    out += [box((4.7, DT + 3.3, 5.1), (5.3, DT + 3.8, 5.7), mat("graphite")),
            box((4.82, DT + 3.42, 5.7), (5.18, DT + 3.72, 10.0), mat("anod_black"), {"angle": 22.5, "axis": "x", "origin": [5.0, DT + 3.57, 5.7]})]
    hy, hz = DT + 3.57 - 4.3 * 0.3827, 5.7 + 4.3 * 0.9239
    out += [box((4.3, hy - 0.62, hz - 0.9), (5.7, hy - 0.02, hz + 0.9), mat("anod_black")),
            box((4.86, hy - 0.02, hz - 0.2), (5.14, hy + 0.3, hz + 0.2), mat("graphite"))]
    out += round_part(5.0, hz, 0.26, hy - 0.72, hy - 0.62, "lens", n=3)
    for x0, x1 in ((3.7, 4.3), (5.7, 6.3)):
        out += [box((x0, hy - 0.5, hz - 0.7), (x1, hy - 0.3, hz + 0.7), mat("anod_black")),
                {**box((x0 + 0.05, hy - 0.54, hz - 0.6), (x1 - 0.05, hy - 0.5, hz + 0.6), mat("ring")), "shade": False}]
    out += [box((3.2, DT, hz - 2.4), (6.6, DT + 0.04, hz + 2.4), faces("paper", up=PRT.face("cr_docsheet", 3.4, 4.8, doc_sheet)))]
    # the laptop, the AV panel on its stand, the microphones in their charger
    out += laptop(11.6, 8.0, DT, "cr_presenter", presenter, "silver", 6.0, 4.0, 3.8)
    out += [box((19.0, DT, 4.6), (22.4, DT + 0.6, 6.8), mat("black"))]
    out += [box((19.1, DT + 0.6, 6.0), (22.3, DT + 2.8, 6.3), {**mat("black"), "south": SCR.face("cr_av", 3.2, 2.2, av_panel)},
                {"angle": -22.5, "axis": "x", "origin": [20.7, DT + 0.6, 6.3]})]
    out += [box((23.4, DT, 4.6), (27.0, DT + 0.7, 7.0), mat("black")), {**box((26.2, DT + 0.7, 6.4), (26.6, DT + 0.76, 6.8), mat("led_green")), "shade": False}]
    out += round_part(24.4, 5.8, 0.36, DT + 0.7, DT + 3.4, "black") + round_part(24.4, 5.8, 0.48, DT + 3.4, DT + 4.3, "grille")
    out += [box((25.3, DT + 0.7, 5.3), (26.2, DT + 2.0, 5.7), mat("black")), box((25.6, DT + 2.0, 5.4), (25.7, DT + 2.6, 5.5), mat("black"))]
    # the clicker, the markers, the handouts, the mug, the grommet and the cables
    out += [box((23.4, DT, 10.2), (24.0, DT + 0.28, 11.8), mat("black")), {**box((23.55, DT + 0.28, 10.5), (23.85, DT + 0.32, 10.8), mat("red")), "shade": False}]
    for k, c in enumerate(("black", "navy", "red", "green")):
        out += [box((19.0 + k * 0.5, DT, 10.4), (19.3 + k * 0.5, DT + 0.3, 12.4), mat("white")), box((19.0 + k * 0.5, DT, 12.4), (19.3 + k * 0.5, DT + 0.3, 13.0), mat(c))]
    out += [box((24.6, DT, 9.6), (28.0, DT + 0.9, 14.4), faces("paper", up=PRT.face("cr_handout", 3.4, 4.8, handout)))]
    out += round_ring(28.2, 6.0, 1.1, 0.2, DT, DT + 2.4, "mug_grey", 1.9, "coffee", None)
    out += [box((28.4, DT + 0.6, 5.75), (29.0, DT + 1.9, 6.25), mat("mug_grey"))]
    out += [box((16.4, DT - 0.02, 4.0), (18.0, DT + 0.06, 5.6), mat("black"))]
    out += cable([(17.2, DT - 0.4, 4.8), (17.2, 2.0, 4.8), (17.2, 2.0, 6.4)], 0.18, "grip_black")
    return out


# ------------------------------------------------------------------ the display cart
def touch_display_cart():
    """The interactive display on its mobile stand (2 x 2, built along x 0..32, its screen to the north): the H base
    on four castors, the twin uprights, the 75 inch touch screen (live: the lesson, the instructor's red pen on it),
    its camera on top, the pen tray and its two pens, the shelf for a laptop."""
    out = []
    for x in (5.6, 24.4):
        out += [box((x, 0.85, 1.6), (x + 2.0, 1.6, 14.4), mat("graphite"))]
        out += castor(x + 0.6, 1.8) + castor(x + 0.6, 13.6)
    out += [box((7.6, 1.0, 9.0), (24.4, 1.6, 9.6), mat("graphite"))]
    for x in (6.0, 24.7):
        out.append(box((x, 1.6, 8.8), (x + 1.3, 27.0, 9.5), mat("graphite")))
    out += [box((5.0, 14.6, 8.2), (27.0, 15.2, 8.8), mat("graphite")), box((5.0, 26.2, 8.2), (27.0, 26.8, 8.8), mat("graphite"))]
    out += [box((2.6, 13.2, 7.0), (29.4, 29.0, 8.2), mat("black")),
            {**box((3.1, 13.7, 6.94), (28.9, 28.5, 7.0), {**mat("black"), "north": SCR.face("cr_docview", 25.8, 14.8, doc_view)}), "shade": False}]
    out += [box((14.6, 29.0, 7.2), (17.4, 29.8, 8.0), mat("black")), box((15.7, 29.2, 7.16), (16.3, 29.6, 7.2), mat("lens"))]
    out += [box((10.0, 12.6, 6.2), (22.0, 13.2, 7.0), mat("black"))]
    for k in range(2):
        out.append(box((11.0 + k * 5.0, 13.2, 6.4), (14.6 + k * 5.0, 13.45, 6.65), mat(("white", "graphite")[k])))
    out += [box((7.3, 8.6, 5.2), (24.7, 9.0, 11.0), mat("graphite")), box((7.3, 9.0, 5.2), (24.7, 9.6, 5.5), mat("graphite"))]
    return out


def ring(cx, cz, r, wall, y0, y1, tex, floor_h=0.0, floor_tex=None, n=5):
    """training_kit.round_ring without a floor when floor_h is 0 (no flat, zero-height element)."""
    return [e for e in round_ring(cx, cz, r, wall, y0, y1, tex, floor_h, floor_tex or tex, None, n=n) if e["to"][1] - e["from"][1] > 1e-6]


# ------------------------------------------------------------------ drone parts, in detail (take 2)
def stand_up(elements, cy, zb):
    """Parts built lying (their front up, their +z end what is to be their top) stood on a board whose face is at
    z = zb and faces north: a turn of -90 degrees about x (y' = cy + z, z' = zb - y)."""
    names = {"up": "north", "down": "south", "south": "up", "north": "down", "east": "east", "west": "west"}
    out = []
    for e in elements:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        q = {"from": [x0, round(cy + z0, 4), round(zb - y1, 4)], "to": [x1, round(cy + z1, 4), round(zb - y0, 4)],
             "faces": {names[k]: {kk: vv for kk, vv in v.items() if kk != "uv"} for k, v in e["faces"].items()}}
        if "shade" in e:
            q["shade"] = e["shade"]
        if "rotation" in e:
            r = e["rotation"]
            ox, oy, oz = r["origin"]
            ax, ang = r["axis"], r["angle"]
            if ax == "y":
                ax, ang = "z", -ang
            elif ax == "z":
                ax = "y"
            q["rotation"] = {"angle": ang, "axis": ax, "origin": [ox, round(cy + oz, 4), round(zb - oy, 4)]}
        out.append(q)
    return out


def motor(cx, cz, y0, r=0.64, leads=1.6):
    """A brushless outrunner standing on its base: the steel base, (if big enough) its windings showing in the gap, the
    black bell, the red ring and the cap on top, the shaft; its three leads out to one side in heat-shrink, with gold
    bullet connectors. (Every layer at least 0.09 px thick: none of their faces fight.)"""
    big = r >= 0.4
    layers = ([("steel", 0.95, 0.1), ("copper", 0.86, 0.1), ("anod_black", 1.0, 0.3), ("anod_red", 0.9, 0.1), ("anod_black", 0.42, 0.1)] if big
              else [("steel", 0.95, 0.09), ("anod_black", 1.0, 0.14), ("steel", 0.9, 0.09)])
    out, y = [], y0
    for tex, rf, h in layers:
        out += round_part(cx, cz, r * rf, y, y + h, tex, n=4 if big else 3)
        y += h
    out += round_part(cx, cz, max(0.04, r * 0.11), y, y + 0.12 if not big else y + 0.22, "chrome", n=3)
    if leads:
        w = max(0.05, r * 0.08)
        for k in (-1, 0, 1):
            z = cz + k * w * 2.6
            x0 = cx + r
            out += [box((x0, y0 + 0.02, z - w), (x0 + leads, y0 + 0.02 + 2 * w, z + w), mat("sil_black")),
                    box((x0 + leads, y0 + 0.02 + w * 0.2, z - w * 0.8), (x0 + leads + w * 4, y0 + 0.02 + w * 1.8, z + w * 0.8), mat("gold"))]
    return out


def prop(cx, cz, y0, ln, colour="pla_black", cw=True):
    """A two-bladed prop lying flat, ln tip to tip along x: the hub and its centre; each blade in five pieces, widest a
    third of the way out and rounded off to the tip, the inner three pitched 22.5 degrees and the tip flat (its twist),
    the two blades turned the opposite ways."""
    R = ln / 2
    c = ln * 0.1
    hr = max(0.1, ln * 0.04)
    hh = max(0.08, ln * 0.03)
    hh = max(hh, 0.1)
    out = round_part(cx, cz, hr, y0, y0 + hh, "black", n=3) + round_part(cx, cz, hr * 0.5, y0 + hh, y0 + hh + 0.1, "black", n=3)
    t = max(0.03, ln * 0.007)
    ym = y0 + max(hh * 0.5, 0.1)
    segs = ((0.0, 0.16, 0.5, 22.5), (0.16, 0.42, 1.0, 22.5), (0.42, 0.66, 0.82, 22.5), (0.66, 0.88, 0.58, 0.0), (0.88, 1.0, 0.3, 0.0))
    for sg in (1, -1):
        for a, b, wf, pitch in segs:
            xa, xb = sorted((cx + sg * (hr + (R - hr) * a), cx + sg * (hr + (R - hr) * b)))
            w = c * wf
            ang = pitch * sg * (1 if cw else -1)
            out.append(box((xa, ym - t / 2, cz - w / 2), (xb, ym + t / 2, cz + w / 2), mat(colour),
                           {"angle": ang, "axis": "x", "origin": [round((xa + xb) / 2, 4), ym, cz]} if ang else None))
    return out


def esc(cx, cz, y0):
    """A heavy-lift ESC in its finned case: the aluminium heatsink, its fins, the black end caps; the motor's three leads
    out of one end, the power pair and the signal lead out of the other."""
    L, W, H = 1.8, 0.9, 0.36
    out = [box((cx - L / 2, y0, cz - W / 2), (cx + L / 2, y0 + H * 0.5, cz + W / 2), mat("heatsink"))]
    for k in range(6):
        z = cz - W / 2 + 0.06 + k * 0.152
        out.append(box((cx - L / 2 + 0.05, y0 + H * 0.5, z), (cx + L / 2 - 0.05, y0 + H, z + 0.06), mat("heatsink")))
    out += [box((cx - L / 2 - 0.1, y0, cz - W / 2), (cx - L / 2, y0 + H * 0.8, cz + W / 2), mat("black")),
            box((cx + L / 2, y0, cz - W / 2), (cx + L / 2 + 0.1, y0 + H * 0.8, cz + W / 2), mat("black"))]
    for k in (-1, 0, 1):
        z = cz + k * 0.22
        out += [box((cx + L / 2 + 0.1, y0 + 0.1, z - 0.05), (cx + L / 2 + 1.1, y0 + 0.2, z + 0.05), mat("sil_black")),
                box((cx + L / 2 + 1.1, y0 + 0.11, z - 0.04), (cx + L / 2 + 1.3, y0 + 0.19, z + 0.04), mat("gold"))]
    for z, c in ((cz - 0.2, "sil_red"), (cz + 0.05, "sil_black")):
        out.append(box((cx - L / 2 - 1.2, y0 + 0.08, z - 0.07), (cx - L / 2 - 0.1, y0 + 0.22, z + 0.07), mat(c)))
    out.append(box((cx - L / 2 - 0.8, y0 + 0.1, cz + 0.25), (cx - L / 2 - 0.1, y0 + 0.14, cz + 0.29), mat("white")))
    return out


def flight_controller(cx, cz, y0):
    """The autopilot: the grey base, the white top shell and its dark vent strip, the status light, the rows of ports
    along both sides, the buzzer, the USB port at one end."""
    L, W = 1.4, 0.9
    out = [box((cx - L / 2, y0, cz - W / 2), (cx + L / 2, y0 + 0.12, cz + W / 2), mat("tx_grey")),
           box((cx - L / 2 + 0.04, y0 + 0.12, cz - W / 2 + 0.04), (cx + L / 2 - 0.04, y0 + 0.3, cz + W / 2 - 0.04), mat("white_mat")),
           box((cx - L / 2 + 0.2, y0 + 0.3, cz - 0.08), (cx + L / 2 - 0.2, y0 + 0.4, cz + 0.08), mat("anod_black")),
           {**box((cx + L / 2 - 0.04, y0 + 0.18, cz + 0.2), (cx + L / 2 + 0.06, y0 + 0.26, cz + 0.3), mat("led_green")), "shade": False}]
    for side in (-1, 1):
        for k in range(5):
            x = cx - L / 2 + 0.16 + k * 0.24
            z = cz + side * (W / 2 + 0.02)
            z = cz + side * W / 2
            out.append(box((x, y0 + 0.06, min(z, z + side * 0.1)), (x + 0.14, y0 + 0.18, max(z, z + side * 0.1)), mat("white")))
    out += round_part(cx - 0.3, cz - 0.24, 0.1, y0 + 0.3, y0 + 0.4, "black", n=3)
    out.append(box((cx - L / 2 - 0.1, y0 + 0.03, cz - 0.1), (cx - L / 2, y0 + 0.11, cz + 0.1), mat("chrome")))
    return out


def gps(cx, cz, y0, mast=2.4):
    """The GPS on its folding mast (the mast along +z): the clamp and hinge at its foot, the carbon mast, the puck at its
    top: the white dome, the red arrow on top showing the way forward."""
    out = [box((cx - 0.25, y0, cz - 0.2), (cx + 0.25, y0 + 0.3, cz + 0.25), mat("anod_black")),
           box((cx - 0.12, y0 + 0.08, cz + 0.25), (cx + 0.12, y0 + 0.26, cz + 0.45), mat("alu")),
           box((cx - 0.07, y0 + 0.1, cz + 0.45), (cx + 0.07, y0 + 0.24, cz + mast - 0.12), mat("carbon"))]
    pz = cz + mast + 0.7
    out += round_part(cx, pz, 0.8, y0, y0 + 0.22, "white_mat", n=4) + round_part(cx, pz, 0.62, y0 + 0.22, y0 + 0.32, "white_mat", n=4)
    out += [box((cx - 0.05, y0 + 0.32, pz - 0.05), (cx + 0.05, y0 + 0.42, pz + 0.35), mat("anod_red")),
            box((cx - 0.05, y0 + 0.32, pz + 0.3), (cx + 0.2, y0 + 0.42, pz + 0.4), mat("anod_red"), {"angle": -45, "axis": "y", "origin": [cx, y0 + 0.37, pz + 0.35]}),
            box((cx - 0.2, y0 + 0.32, pz + 0.3), (cx + 0.05, y0 + 0.42, pz + 0.4), mat("anod_red"), {"angle": 45, "axis": "y", "origin": [cx, y0 + 0.37, pz + 0.35]})]
    return out


def receiver(cx, cz, y0):
    """The control link's receiver: its case and light, two aerial leads out at 45 degrees each ending in its white
    element."""
    out = [box((cx - 0.4, y0, cz - 0.25), (cx + 0.4, y0 + 0.22, cz + 0.25), mat("black")),
           {**box((cx + 0.2, y0 + 0.22, cz), (cx + 0.3, y0 + 0.32, cz + 0.1), mat("led_green")), "shade": False}]
    for sx, ang in ((-1, 45), (1, -45)):
        ox = cx + sx * 0.15
        out.append(box((ox - 0.03, y0 + 0.08, cz - 0.25 - 1.2), (ox + 0.03, y0 + 0.14, cz - 0.25), mat("black"), {"angle": ang, "axis": "y", "origin": [ox, y0 + 0.11, cz - 0.25]}))
        tx = ox - 1.2 * math.sin(math.radians(ang))
        tz = cz - 0.25 - 1.2 * math.cos(math.radians(ang))
        out.append(box((tx - 0.05, y0 + 0.06, tz - 0.55), (tx + 0.05, y0 + 0.16, tz), mat("white")))
    return out


def nav_light(cx, cz, y0, col="nav_red"):
    return [box((cx - 0.45, y0, cz - 0.35), (cx + 0.45, y0 + 0.2, cz + 0.35), mat("black"))] + \
        [{**e, "shade": False} for e in round_part(cx, cz, 0.3, y0 + 0.2, y0 + 0.42, col, n=3)]


def gimbal(cx, cz, y0):
    """The camera gimbal hung from its plate (the camera looking up, out of the board): the carbon plate on its four
    rubber dampers, the yaw motor, the arms, the pitch motors either side, the camera and its lens."""
    out = [box((cx - 0.8, y0, cz - 0.6), (cx + 0.8, y0 + 0.1, cz + 0.6), mat("carbon"))]
    for dx in (-0.6, 0.6):
        for dz in (-0.4, 0.4):
            out += round_part(cx + dx, cz + dz, 0.13, y0 + 0.1, y0 + 0.24, "rubber", n=3)
    out += [box((cx - 0.7, y0 + 0.24, cz - 0.5), (cx + 0.7, y0 + 0.34, cz + 0.5), mat("carbon"))]
    out += round_part(cx, cz, 0.34, y0 + 0.34, y0 + 0.56, "anod_black", n=3)
    out += [box((cx - 0.72, y0 + 0.56, cz - 0.07), (cx + 0.72, y0 + 0.66, cz + 0.07), mat("alu")),
            box((cx - 0.72, y0 + 0.66, cz - 0.07), (cx - 0.62, y0 + 1.12, cz + 0.07), mat("alu")),
            box((cx + 0.62, y0 + 0.66, cz - 0.07), (cx + 0.72, y0 + 1.12, cz + 0.07), mat("alu")),
            box((cx - 0.62, y0 + 0.8, cz - 0.2), (cx - 0.46, y0 + 1.1, cz + 0.2), mat("anod_black")),
            box((cx + 0.46, y0 + 0.8, cz - 0.2), (cx + 0.62, y0 + 1.1, cz + 0.2), mat("anod_black")),
            box((cx - 0.46, y0 + 0.7, cz - 0.34), (cx + 0.46, y0 + 1.2, cz + 0.34), mat("black"))]
    out += round_part(cx, cz, 0.24, y0 + 1.2, y0 + 1.34, "anod_black", n=3) + round_part(cx, cz, 0.17, y0 + 1.34, y0 + 1.44, "glass", n=3)
    return out


def folding_arm(x0, x1, cz, y0, r=0.3):
    """A carbon arm with its folding joint (the two clamp halves, the red locking lever) and the motor plate at its end."""
    cy = y0 + r
    out = LK.rod_x(x0, x1 - 0.5, cy, cz, r, "carbon", n=4)
    j = x0 + (x1 - x0) * 0.32
    out += [box((j - 0.45, y0 - 0.1, cz - r - 0.1), (j - 0.02, y0 + 2 * r + 0.1, cz + r + 0.1), mat("alu")),
            box((j + 0.02, y0 - 0.1, cz - r - 0.1), (j + 0.45, y0 + 2 * r + 0.1, cz + r + 0.1), mat("alu")),
            box((j - 0.3, y0 + 2 * r + 0.1, cz - 0.1), (j + 0.7, y0 + 2 * r + 0.22, cz + 0.1), mat("anod_red"))]
    out += [box((x1 - 0.5, y0 - 0.02, cz - 0.55), (x1, y0 + 0.1, cz + 0.55), mat("alu")),
            box((x1 - 0.5, y0 + 0.1, cz - r - 0.06), (x1 - 0.2, y0 + 2 * r + 0.06, cz + r + 0.06), mat("alu"))]
    for dz in (-0.35, 0.35):
        out += round_part(x1 - 0.1, cz + dz, 0.05, y0 + 0.1, y0 + 0.2, "chrome", n=3)
    return out


def parachute(cx, cz, y0):
    """The parachute's canister (it fires the chute out of its lid): the black tube, its white band, the orange lid."""
    return (round_part(cx, cz, 1.1, y0, y0 + 0.5, "anod_black", n=5) + round_part(cx, cz, 1.12, y0 + 0.5, y0 + 0.7, "white_mat", n=5)
            + round_part(cx, cz, 1.1, y0 + 0.7, y0 + 1.25, "anod_black", n=5) + round_part(cx, cz, 1.16, y0 + 1.25, y0 + 1.5, "tpu", n=5)
            + round_part(cx, cz, 0.3, y0 + 1.5, y0 + 1.6, "anod_black", n=3))


def altimeter(cx, cz, y0):
    out = [box((cx - 0.45, y0, cz - 0.28), (cx + 0.45, y0 + 0.32, cz + 0.28), mat("anod_black"))]
    for dx in (-0.2, 0.2):
        out += round_part(cx + dx, cz, 0.12, y0 + 0.32, y0 + 0.42, "lens", n=3)
    out.append(box((cx + 0.45, y0 + 0.1, cz - 0.04), (cx + 1.4, y0 + 0.16, cz + 0.04), mat("black")))
    return out


def servo_hook(cx, cz, y0):
    """The cargo hook: its servo (the case, the mounting ears, the white horn), the pushrod to the hook's latch, the
    steel J hook and its spring gate."""
    out = [box((cx - 0.32, y0, cz - 0.6), (cx + 0.32, y0 + 0.45, cz + 0.6), mat("black")),
           box((cx - 0.32, y0 + 0.3, cz - 0.82), (cx + 0.32, y0 + 0.36, cz + 0.82), mat("black"))]
    out += round_part(cx, cz + 0.3, 0.14, y0 + 0.45, y0 + 0.55, "white", n=3) + [box((cx - 0.06, y0 + 0.55, cz + 0.05), (cx + 0.06, y0 + 0.65, cz + 0.65), mat("white"))]
    hz = cz - 1.6
    out += [box((cx - 0.04, y0 + 0.2, hz + 0.2), (cx + 0.04, y0 + 0.28, cz - 0.6), mat("chrome")),
            box((cx - 0.12, y0 + 0.05, hz - 0.6), (cx + 0.12, y0 + 0.4, hz + 0.25), mat("steel")),
            box((cx - 0.12, y0 + 0.05, hz - 0.9), (cx + 0.55, y0 + 0.4, hz - 0.6), mat("steel")),
            box((cx + 0.3, y0 + 0.05, hz - 0.6), (cx + 0.55, y0 + 0.4, hz - 0.3), mat("steel")),
            box((cx + 0.12, y0 + 0.18, hz - 0.3), (cx + 0.3, y0 + 0.26, hz + 0.1), mat("chrome"))]
    return out


def xt(cx, cz, y0, big=True):
    """An XT90 (or XT60) plug: the yellow housing, its two gold sockets showing at the open end."""
    w, d, h = (0.42, 0.62, 0.3) if big else (0.26, 0.4, 0.18)
    out = [box((cx - w / 2, y0, cz - d / 2), (cx + w / 2, y0 + h, cz + d / 2), mat("xt"))]
    for dx in (-w * 0.22, w * 0.22):
        out.append(box((cx + dx - h * 0.18, y0 + h * 0.3, cz + d / 2), (cx + dx + h * 0.18, y0 + h * 0.66, cz + d / 2 + 0.1), mat("gold")))
    return out


def lipo(cx, cz, y0, key, L=1.76, W=0.58, H=0.72, label=("6S", "2200")):
    """A LiPo pack lying along z: the shrink-wrapped cells under their printed wrap, the hard end caps, the main leads
    (red and black silicone) out of the +z end to an XT60, the balance lead (seven wires) to its white plug."""
    pic = PRT.face(key, W, L, lipo_wrap(*label))
    out = [box((cx - W / 2, y0, cz - L / 2), (cx + W / 2, y0 + H, cz + L / 2), {**mat("lipo"), "up": pic}),
           box((cx - W / 2 - 0.02, y0, cz + L / 2), (cx + W / 2 + 0.02, y0 + H + 0.02, cz + L / 2 + 0.1), mat("lipo_cap")),
           box((cx - W / 2 - 0.02, y0, cz - L / 2 - 0.1), (cx + W / 2 + 0.02, y0 + H + 0.02, cz - L / 2), mat("lipo_cap"))]
    for dx, c in ((-0.09, "sil_red"), (0.09, "sil_black")):
        out.append(box((cx + dx - 0.06, y0 + 0.24, cz + L / 2 + 0.1), (cx + dx + 0.06, y0 + 0.36, cz + L / 2 + 0.9), mat(c)))
    out += xt(cx, cz + L / 2 + 1.1, y0 + 0.15, big=False)
    out += [box((cx + 0.12, y0 + H - 0.12, cz + L / 2 + 0.1), (cx + 0.26, y0 + H - 0.08, cz + L / 2 + 0.5), mat("balance")),
            box((cx + 0.08, y0 + H - 0.2, cz + L / 2 + 0.5), (cx + 0.3, y0 + H - 0.02, cz + L / 2 + 0.66), mat("white"))]
    return out


def smart_battery(x0, y0, zb, key):
    """The T4's smart battery (12S, 22000 mAh; 30 by 17 by 9 cm) on its side on the board, its label to the front: the
    grey shell, the black end caps, the carry handle on top, the power button and its four charge lights (three lit),
    the gold-bladed connector at the far end, the latch buttons."""
    L, H, D = 6.4, 3.6, 2.0
    z0 = zb - D
    out = [box((x0 + 0.3, y0, z0), (x0 + L - 0.3, y0 + H, zb), {**mat("batt_shell"), "north": PRT.face(key, L - 0.6, H, battery_label)}),
           box((x0, y0 + 0.08, z0 + 0.08), (x0 + 0.3, y0 + H - 0.08, zb), mat("batt_cap")),
           box((x0 + L - 0.3, y0 + 0.08, z0 + 0.08), (x0 + L, y0 + H - 0.08, zb), mat("batt_cap"))]
    out += [box((x0 + 1.8, y0 + H, z0 + 0.6), (x0 + 2.1, y0 + H + 0.4, z0 + 1.4), mat("batt_cap")),
            box((x0 + 4.3, y0 + H, z0 + 0.6), (x0 + 4.6, y0 + H + 0.4, z0 + 1.4), mat("batt_cap")),
            box((x0 + 1.8, y0 + H + 0.4, z0 + 0.6), (x0 + 4.6, y0 + H + 0.6, z0 + 1.4), mat("batt_cap"))]
    out += [box((x0 + 0.05, y0 + H - 0.6, z0 - 0.02), (x0 + 0.25, y0 + H - 0.4, z0 + 0.08), mat("tx_grey"))]
    for k in range(4):
        y = y0 + H - 0.95 - k * 0.3
        out.append({**box((x0 + 0.1, y, z0 - 0.02), (x0 + 0.2, y + 0.16, z0 + 0.08), mat("led_green" if k < 3 else "led_off")), "shade": False})
    out += [box((x0 + L, y0 + 0.9, z0 + 0.4), (x0 + L + 0.1, y0 + 2.7, z0 + 1.5), mat("black"))]
    for k in range(4):
        y = y0 + 1.1 + k * 0.4
        out.append(box((x0 + L + 0.1, y, z0 + 0.6), (x0 + L + 0.2, y + 0.16, z0 + 1.3), mat("gold")))
    out += [box((x0 + L - 0.25, y0 + 1.4, z0 - 0.02), (x0 + L - 0.05, y0 + 2.2, z0 + 0.08), mat("tx_grey"))]
    return out


def battery_label(img):
    """The smart battery's label: the maker's band, its numbers in large type, the safety marks, the QR code."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(112, 116, 122))
    d.rectangle((W * 0.03, H * 0.07, W * 0.97, H * 0.93), fill=(236, 236, 232))
    d.rectangle((W * 0.03, H * 0.07, W * 0.97, H * 0.27), fill=(30, 34, 40))
    d.text((W * 0.06, H * 0.17), "TRI-PILLAR  TB-12", font=_fit(d, "TRI-PILLAR  TB-12", FONT, round(H * 0.12), W * 0.56), fill=(255, 255, 255), anchor="lm")
    d.text((W * 0.94, H * 0.17), "SMART", font=_fit(d, "SMART", FONT, round(H * 0.11), W * 0.22), fill=(240, 170, 40), anchor="rm")
    for k, t in enumerate(("44.4 V  12S", "22000 mAh", "977 Wh")):
        d.text((W * 0.06, H * (0.38 + k * 0.15)), t, font=_fit(d, "22000 mAh", FONT, round(H * 0.12), W * 0.5), fill=(24, 28, 34), anchor="lm")
    for k in range(3):
        cx = W * (0.08 + k * 0.1)
        d.polygon([(cx, H * 0.76), (cx + H * 0.06, H * 0.88), (cx - H * 0.06, H * 0.88)], outline=(30, 30, 34), width=2)
        d.text((cx, H * 0.845), "!", font=ImageFont.truetype(FONT, max(7, round(H * 0.07))), fill=(30, 30, 34), anchor="mm")
    q = H * 0.5
    qx, qy = W * 0.94 - q, H * 0.34
    d.rectangle((qx - 2, qy - 2, qx + q + 2, qy + q + 2), fill=(255, 255, 255))
    rng = random.Random(77)
    n = 11
    for i in range(n):
        for j in range(n):
            if (i < 3 and j < 3) or (i < 3 and j > n - 4) or (i > n - 4 and j < 3) or rng.random() < 0.45:
                d.rectangle((qx + i * q / n, qy + j * q / n, qx + (i + 1) * q / n - 1, qy + (j + 1) * q / n - 1), fill=(20, 20, 24))
    return img


def lipo_wrap(cells, mah):
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(30, 32, 36))
        d.rectangle((0, H * 0.3, W, H * 0.5), fill=(230, 60, 40))
        d.text((W / 2, H * 0.4), "VOLT", font=_fit(d, "VOLT", FONT, round(W * 0.3), W * 0.8), fill=(255, 255, 255), anchor="mm")
        d.text((W / 2, H * 0.6), cells, font=_fit(d, "6S", FONT, round(W * 0.34), W * 0.7), fill=(240, 240, 240), anchor="mm")
        d.text((W / 2, H * 0.75), mah, font=_fit(d, "2200", FONT, round(W * 0.26), W * 0.8), fill=(200, 204, 210), anchor="mm")
        return img
    return draw


def plate_dark(text, n):
    """A part's engraved plate: black anodised, its number and name in white."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(26, 28, 32))
        d.rectangle((1, 1, W - 2, H - 2), outline=(110, 114, 120))
        d.text((H * 0.55, H / 2), f"{n:02d}", font=_fit(d, "16", FONT, round(H * 0.56), H * 0.9), fill=(240, 180, 50), anchor="mm")
        d.text((H * 1.15, H / 2), text, font=_fit(d, text, FONT, round(H * 0.5), W - H * 1.3), fill=(236, 236, 236), anchor="lm")
        return img
    return draw


def tag_print(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(200, 30, 30))
    for k, t in enumerate(("REMOVE", "BEFORE", "FLIGHT")):
        d.text((W / 2, H * (0.22 + k * 0.28)), t, font=_fit(d, "BEFORE", FONT, round(H * 0.22), W * 0.84), fill=(255, 255, 255), anchor="mm")
    return img


def _plate(n, text, x0, y0, w):
    return [box((x0, y0, 14.9), (x0 + w, y0 + 0.9, 15.0), faces("anod_black", north=PRT.face(f"cr_plate2_{n}", w, 0.9, plate_dark(text, n))))]


def scaled(elements, s, cx, cz, y0=0.0):
    """Elements grown s times about (cx, y0, cz) (a bigger part from a smaller one's build)."""
    out = []
    for e in elements:
        q = json.loads(json.dumps(e))
        for k in ("from", "to"):
            x, y, z = q[k]
            q[k] = [round(cx + (x - cx) * s, 4), round(y0 + (y - y0) * s, 4), round(cz + (z - cz) * s, 4)]
        if "rotation" in q:
            x, y, z = q["rotation"]["origin"]
            q["rotation"]["origin"] = [round(cx + (x - cx) * s, 4), round(y0 + (y - y0) * s, 4), round(cz + (z - cz) * s, 4)]
        out.append(q)
    return out


def parts_board():
    """PARTS OF A DRONE (3 x 2 on a wall, 2.8 by 1.75 m, its front to the north): the navy felt board in its aluminium
    frame, the header, and sixteen real parts of the T4 (a heavy-lift delivery drone) fixed to it, each over its
    engraved plate: the 32 inch prop, a motor (its windings showing, the bell, the cap, the leads), the ESC in its
    finned case, the autopilot, the GPS on its folding mast, the smart battery (its charge lights, its label), the
    camera gimbal on its dampers, an arm with its folding joint, the parachute (its REMOVE BEFORE FLIGHT tag), the
    receiver and its two aerials, a landing skid, the cargo hook and its servo, the power leads and their XT90s, the
    navigation lights, the laser altimeter, a pair of small props (one each way)."""
    board = [box((2.0, 2.0, 15.0), (46.0, 30.0, 15.36), mat("felt")), box((2.0, 2.0, 15.36), (46.0, 30.0, 15.9), mat("lam_grey")),
             box((1.6, 30.0, 14.8), (46.4, 30.6, 16.0), mat("alu")), box((1.6, 1.4, 14.8), (46.4, 2.0, 16.0), mat("alu")),
             box((1.6, 2.0, 14.8), (2.0, 30.0, 16.0), mat("alu")), box((46.0, 2.0, 14.8), (46.4, 30.0, 16.0), mat("alu")),
             box((2.6, 27.0, 14.94), (45.4, 29.6, 15.0), faces("navy", north=PRT.face("cr_header", 42.8, 2.6, header_print)))]
    ZB = 15.0
    out = []
    # (laid out as the viewer sees them, x from their left; mirrored into the model at the end)
    # row A: the prop, the motor, the ESC, the autopilot, the GPS
    out += stand_up(prop(10.0, 0, 0.14, 13.0, "pla_black") + round_part(10.0, 0, 0.2, 0.0, 0.14, "alu", n=3), 24.0, ZB)
    out += _plate(1, "PROPELLER 32 IN", 6.6, 20.2, 6.8)
    out += stand_up(motor(20.0, 0, 0.0, 0.9, 1.6), 23.6, ZB)
    out += _plate(2, "MOTOR", 18.4, 20.2, 4.2)
    out += stand_up(scaled(esc(27.6, 0, 0.0), 1.3, 27.6, 0), 23.6, ZB)
    out += _plate(3, "ESC", 26.2, 20.2, 3.2)
    out += stand_up(scaled(flight_controller(35.0, 0, 0.0), 1.25, 35.0, 0), 23.6, ZB)
    out += _plate(4, "AUTOPILOT", 32.6, 20.2, 4.8)
    out += stand_up(scaled(gps(41.4, -2.2, 0.0, 2.0), 1.15, 41.4, 0), 23.0, ZB)
    out += _plate(5, "GPS", 40.0, 19.6, 3.0)
    # row B: the battery, the gimbal, the arm, the parachute, the receiver
    out += smart_battery(3.4, 12.0, ZB, "cr_batt_label")
    out += _plate(6, "SMART BATTERY", 3.6, 9.8, 5.8)
    out += stand_up(scaled(gimbal(14.0, 0, 0.0), 1.3, 14.0, 0), 14.4, ZB)
    out += _plate(7, "CAMERA GIMBAL", 11.6, 9.8, 5.4)
    out += stand_up(folding_arm(18.8, 30.0, 0, 0.0, 0.36), 14.4, ZB)
    out += [box((20.0, 13.6, 14.16), (20.4, 15.2, 15.0), mat("alu")), box((27.6, 13.6, 14.16), (28.0, 15.2, 15.0), mat("alu"))]
    out += _plate(8, "ARM AND FOLDING JOINT", 20.6, 9.8, 7.4)
    out += stand_up(scaled(parachute(35.0, 0, 0.0), 1.15, 35.0, 0), 14.4, ZB)
    out += [box((36.1, 12.0, 13.1), (36.18, 13.6, 13.18), mat("black")),
            box((35.7, 11.1, 13.06), (36.7, 12.0, 13.12), faces("red", north=PRT.face("cr_rbf", 1.0, 0.9, tag_print)))]
    out += _plate(9, "PARACHUTE", 32.8, 9.8, 4.4)
    out += stand_up(scaled(receiver(41.6, 0.6, 0.0), 1.2, 41.6, 0.6), 14.0, ZB)
    out += _plate(10, "RECEIVER", 39.8, 9.8, 4.0)
    # row C: the skid, the hook, the leads, the lights, the altimeter, the small props
    out += stand_up(LK.rod_x(4.2, 11.6, 0.28, 0, 0.28, "carbon", n=4) + [box((7.6, -0.1, -0.4), (8.2, 0.66, 0.4), mat("alu"))]
                    + [box((7.72, 0.14, 0.4), (8.08, 0.42, 3.0), mat("carbon"))]
                    + LK.rod_x(3.6, 4.2, 0.28, 0, 0.38, "rubber", n=4) + LK.rod_x(11.6, 12.2, 0.28, 0, 0.38, "rubber", n=4), 4.8, ZB)
    out += _plate(11, "LANDING SKID", 5.4, 2.6, 4.8)
    out += stand_up(scaled(servo_hook(16.0, 0.6, 0.0), 1.2, 16.0, 0.6), 6.2, ZB)
    out += _plate(12, "CARGO HOOK", 13.8, 2.6, 4.6)
    for zz, c in ((0.18, "sil_red"), (-0.18, "sil_black")):
        out += stand_up([box((20.6, 0.06, zz - 0.09), (25.8, 0.24, zz + 0.09), mat(c))], 6.0, ZB)
    out += stand_up([box((19.8, 0.0, -0.32), (20.6, 0.32, 0.32), mat("xt")), box((25.8, 0.0, -0.32), (26.6, 0.32, 0.32), mat("xt")),
                     box((19.7, 0.08, -0.22), (19.8, 0.24, -0.06), mat("gold")), box((19.7, 0.08, 0.06), (19.8, 0.24, 0.22), mat("gold")),
                     box((26.6, 0.08, -0.22), (26.7, 0.24, -0.06), mat("gold")), box((26.6, 0.08, 0.06), (26.7, 0.24, 0.22), mat("gold"))], 6.0, ZB)
    out += _plate(13, "POWER LEADS XT90", 19.8, 2.6, 6.8)
    out += stand_up(scaled(nav_light(29.6, 0, 0.0, "nav_red") + nav_light(31.0, 0, 0.0, "nav_green"), 1.2, 30.3, 0), 6.0, ZB)
    out += _plate(14, "NAV LIGHTS", 28.0, 2.6, 4.6)
    out += stand_up(scaled(altimeter(35.0, 0, 0.0), 1.2, 35.0, 0), 6.0, ZB)
    out += _plate(15, "ALTIMETER", 33.4, 2.6, 4.0)
    out += stand_up(prop(39.6, 0, 0.1, 2.85, "pla_black", True) + prop(43.0, 0, 0.1, 2.85, "pla_orange", False)
                    + round_part(39.6, 0, 0.1, 0.0, 0.1, "alu", n=3) + round_part(43.0, 0, 0.1, 0.0, 0.1, "alu", n=3), 6.0, ZB)
    out += _plate(16, "PROPS: CW AND CCW", 38.0, 2.6, 6.8)
    return board + mirror_x(out, 48.0)


def mirror_x(elements, w):
    """Elements laid out as seen from a north-facing piece's front (x from the viewer's left) moved into the model, where
    the viewer's left is +x: x mirrored about the piece's middle, east and west faces swapped, turns about y and z
    reversed."""
    out = []
    for e in elements:
        q = json.loads(json.dumps(e))
        x0, x1 = q["from"][0], q["to"][0]
        q["from"][0], q["to"][0] = round(w - x1, 4), round(w - x0, 4)
        fc = q["faces"]
        if "east" in fc or "west" in fc:
            east, west = fc.pop("east", None), fc.pop("west", None)
            if west is not None:
                fc["east"] = west
            if east is not None:
                fc["west"] = east
        if "rotation" in q:
            r = q["rotation"]
            r["origin"][0] = round(w - r["origin"][0], 4)
            if r["axis"] in ("y", "z"):
                r["angle"] = -r["angle"]
        out.append(q)
    return out


# ------------------------------------------------------------------ the build benches (take 2)
BT = 14.4             # a build bench's top (90 cm)


def quad(cx, cz, y0, motors=4, props=False, battery=None, usb=False):
    """A 7 inch training quad lying on the bench, its camera to the north: the carbon bottom plate and its four arms out
    to the motor pads, the anodised standoffs, the top plate, the flight stack between them (the ESC board, the flight
    controller, its light and USB port), the capacitor, the camera in its orange mount, the video aerial at the back,
    the receiver's two aerial tubes, the battery strap and its buckle; motors on `motors` arms (their leads along the
    arms), props on them if `props`, a LiPo strapped on top if `battery`. (Every layer 0.1 px from the next.)"""
    f = 0.1                                    # the frame's plates (6 mm)
    out = [box((cx - 0.55, y0, cz - 0.9), (cx + 0.55, y0 + f, cz + 0.9), mat("carbon"))]
    pads = []
    for sx in (-1, 1):
        for sz in (-1, 1):
            ang = -45 * sx * sz
            xa, xb = sorted((cx + sx * 0.5, cx + sx * 1.95))
            out.append(box((xa, y0 + f, cz - 0.17), (xb, y0 + 2 * f, cz + 0.17), mat("carbon"), {"angle": ang, "axis": "y", "origin": [cx, y0, cz]}))
            mx, mz = cx + sx * 2.4 * 0.7071, cz + sz * 2.4 * 0.7071
            pads.append((mx, mz, sx, sz))
            out += round_part(mx, mz, 0.3, y0 + f, y0 + 2 * f, "carbon", n=3)
    t = y0 + 0.75                              # the top plate's underside
    for dx in (-0.45, 0.45):
        for dz in (-0.75, 0.75):
            out += round_part(cx + dx, cz + dz, 0.06, y0 + f, t, "anod_purple", n=3)
    out += [box((cx - 0.55, t, cz - 0.85), (cx + 0.55, t + f, cz + 0.85), mat("carbon")),
            box((cx - 0.4, y0 + 0.3, cz - 0.4), (cx + 0.4, y0 + 0.4, cz + 0.4), mat("black")),
            box((cx - 0.36, y0 + 0.5, cz - 0.36), (cx + 0.36, y0 + 0.6, cz + 0.36), mat("pcb_blue")),
            box((cx + 0.36, y0 + 0.5, cz - 0.07), (cx + 0.46, y0 + 0.6, cz + 0.07), mat("chrome")),
            {**box((cx - 0.3, y0 + 0.6, cz - 0.3), (cx - 0.22, y0 + 0.7, cz - 0.22), mat("led_green")), "shade": False}]
    out += round_part(cx, cz + 0.6, 0.12, y0 + f, y0 + 0.45, "black", n=3)
    out += [box((cx - 0.3, y0 + f, cz - 0.98), (cx - 0.25, y0 + 0.6, cz - 0.62), mat("tpu")),
            box((cx + 0.25, y0 + f, cz - 0.98), (cx + 0.3, y0 + 0.6, cz - 0.62), mat("tpu")),
            box((cx - 0.2, y0 + 0.2, cz - 0.95), (cx + 0.2, y0 + 0.55, cz - 0.62), mat("black")),
            box((cx - 0.1, y0 + 0.27, cz - 1.05), (cx + 0.1, y0 + 0.47, cz - 0.95), mat("lens"))]
    top = t + f
    out += [box((cx + 0.38, top, cz + 0.74), (cx + 0.46, top + 0.55, cz + 0.82), mat("black"))]
    out += round_part(cx + 0.42, cz + 0.78, 0.13, top + 0.55, top + 0.8, "black", n=3)
    for dx in (-0.42, -0.24):
        out.append(box((cx + dx - 0.025, top, cz + 0.78), (cx + dx + 0.025, top + 0.65, cz + 0.83), mat("white"),
                       {"angle": 45, "axis": "x", "origin": [cx + dx, top, cz + 0.8]}))
    out += [box((cx - 0.65, top, cz - 0.12), (cx + 0.5, top + 0.1, cz + 0.12), mat("strap")),
            box((cx + 0.5, top, cz - 0.14), (cx + 0.7, top + 0.2, cz + 0.14), mat("red"))]
    for k, (mx, mz, sx, sz) in enumerate(pads):
        ang = -45 * sx * sz
        xa, xb = sorted((cx + sx * 0.6, cx + sx * 2.05))
        out.append(box((xa, y0 + 2 * f, cz - 0.05), (xb, y0 + 3 * f, cz + 0.05), mat("sil_black"), {"angle": ang, "axis": "y", "origin": [cx, y0, cz]}))
        if k < motors:
            out += motor(mx, mz, y0 + 2 * f, 0.22, 0)
            if props:
                out += prop(mx, mz, y0 + 2 * f + 0.44, 2.85, ("pla_black", "pla_orange")[(k + (1 if sz > 0 else 0)) % 2], cw=(sx * sz > 0))
    if battery:
        out += lipo(cx, cz - 0.25, top + 0.1, battery, 1.76, 0.58, 0.62)
    if usb:
        out.append(box((cx + 0.46, y0 + 0.52, cz - 0.05), (cx + 1.6, y0 + 0.58, cz + 0.05), mat("black")))
    return out


def solder_station(x0, z0, y0):
    """The soldering station: its unit (the set temperature on its display, the knob, the switch), the iron resting in
    its stand's sloped holder over the brass wool and the wet sponge, the reel of solder on its stand."""
    disp = SCR.face("cr_solder2", 1.4, 0.6, LK.label("350°C", (14, 20, 26), (255, 120, 60), live={"src": "value", "base": 350, "spread": 3, "period": 8, "unit": "°C"}))
    out = [box((x0, y0, z0), (x0 + 2.4, y0 + 1.5, z0 + 1.8), mat("anod_black")),
           box((x0 + 0.3, y0 + 0.7, z0 + 1.8), (x0 + 1.7, y0 + 1.3, z0 + 1.9), {**mat("black"), "south": disp}),
           box((x0 + 1.9, y0 + 0.2, z0 + 1.8), (x0 + 2.2, y0 + 0.5, z0 + 1.9), mat("red"))]
    out += swap_yz(round_part(x0 + 2.0, y0 + 1.0, 0.22, z0 + 1.8, z0 + 2.05, "graphite", n=3))
    out += [box((x0 + 2.8, y0, z0), (x0 + 5.6, y0 + 0.3, z0 + 2.0), mat("anod_black"))]
    out += ring(x0 + 3.5, z0 + 1.0, 0.45, 0.1, y0 + 0.3, y0 + 0.7, "brass", 0.25, "brass")
    out += [box((x0 + 4.1, y0 + 0.3, z0 + 0.3), (x0 + 5.4, y0 + 0.45, z0 + 0.7), mat("sponge"))]
    rot = {"angle": 45, "axis": "z", "origin": [x0 + 4.7, y0 + 0.3, z0 + 1.0]}
    out += [box((x0 + 4.5, y0 + 0.3, z0 + 0.8), (x0 + 4.9, y0 + 2.0, z0 + 1.2), mat("chrome"), dict(rot)),
            box((x0 + 4.64, y0 + 1.2, z0 + 0.94), (x0 + 4.76, y0 + 2.8, z0 + 1.06), mat("graphite"), dict(rot)),
            box((x0 + 4.54, y0 + 2.8, z0 + 0.84), (x0 + 4.86, y0 + 4.2, z0 + 1.16), mat("anod_red"), dict(rot))]
    out += LK.rod_x(x0 + 6.0, x0 + 6.6, y0 + 0.55, z0 + 1.0, 0.45, "chrome", n=4)
    out += LK.rod_x(x0 + 5.85, x0 + 6.0, y0 + 0.55, z0 + 1.0, 0.12, "black", n=3) + LK.rod_x(x0 + 6.6, x0 + 6.75, y0 + 0.55, z0 + 1.0, 0.12, "black", n=3)
    out += [box((x0 + 5.75, y0, z0 + 0.5), (x0 + 6.85, y0 + 0.1, z0 + 1.5), mat("anod_black")), box((x0 + 5.75, y0 + 0.1, z0 + 0.75), (x0 + 5.85, y0 + 0.8, z0 + 1.25), mat("anod_black")),
            box((x0 + 6.75, y0 + 0.1, z0 + 0.75), (x0 + 6.85, y0 + 0.8, z0 + 1.25), mat("anod_black"))]
    return out


def driver_stand(x0, z0, y0):
    """The hex driver set in its aluminium stand: six drivers standing in it, each a grip with a coloured band."""
    out = [box((x0, y0, z0), (x0 + 3.0, y0 + 0.5, z0 + 1.0), mat("alu"))]
    for k, c in enumerate(("anod_red", "anod_black", "anod_purple", "anod_red", "anod_black", "anod_purple")):
        cx = x0 + 0.3 + k * 0.48
        out += round_part(cx, z0 + 0.5, 0.06, y0 + 0.5, y0 + 1.0, "chrome", n=3) + round_part(cx, z0 + 0.5, 0.17, y0 + 1.0, y0 + 2.0, "rubber", n=3)
        out += round_part(cx, z0 + 0.5, 0.17, y0 + 2.0, y0 + 2.2, c, n=3) + round_part(cx, z0 + 0.5, 0.17, y0 + 2.2, y0 + 2.6, "rubber", n=3)
    return out


def multimeter(x0, z0, y0):
    """The multimeter in its yellow holster: the display (reading the pack), the dial, the probe sockets, the leads to
    the probes."""
    return [box((x0, y0, z0), (x0 + 1.5, y0 + 0.55, z0 + 2.8), mat("meter")),
            box((x0 + 0.12, y0 + 0.55, z0 + 0.14), (x0 + 1.38, y0 + 0.65, z0 + 2.2), faces("anod_black", up=SCR.face("cr_meter", 1.26, 2.06, meter_face))),
            box((x0 + 0.3, y0 + 0.55, z0 + 2.3), (x0 + 0.5, y0 + 0.75, z0 + 2.5), mat("red")),
            box((x0 + 1.0, y0 + 0.55, z0 + 2.3), (x0 + 1.2, y0 + 0.75, z0 + 2.5), mat("black"))] + \
        cable([(x0 + 0.4, y0 + 0.7, z0 + 2.5), (x0 + 0.4, y0 + 0.7, z0 + 3.2), (x0 - 1.4, y0 + 0.7, z0 + 3.2)], 0.05, "red") + \
        cable([(x0 + 1.1, y0 + 0.7, z0 + 2.5), (x0 + 1.1, y0 + 0.7, z0 + 3.5), (x0 - 1.6, y0 + 0.7, z0 + 3.5)], 0.05, "black") + \
        [box((x0 - 2.4, y0, z0 + 3.1), (x0 - 1.4, y0 + 0.14, z0 + 3.3), mat("red")), box((x0 - 2.6, y0, z0 + 3.4), (x0 - 1.6, y0 + 0.14, z0 + 3.6), mat("black"))]


def charger(x0, z0, y0):
    """The balance charger: its black case and fins, the screen (live: the pack charging), the buttons, the output
    jacks and the balance board, a LiPo on its leads."""
    out = [box((x0, y0, z0), (x0 + 2.8, y0 + 1.0, z0 + 2.0), mat("anod_black"))]
    for k in range(5):
        out.append(box((x0 + 0.2 + k * 0.52, y0 + 1.0, z0 + 0.2), (x0 + 0.36 + k * 0.52, y0 + 1.1, z0 + 1.0), mat("heatsink")))
    out += [box((x0 + 0.2, y0 + 0.25, z0 + 2.0), (x0 + 1.8, y0 + 0.85, z0 + 2.1), {**mat("black"), "south": SCR.face("cr_charger2", 1.6, 0.6, charge_screen)})]
    for k in range(4):
        out.append(box((x0 + 1.95 + (k % 2) * 0.35, y0 + 0.3 + (k // 2) * 0.3, z0 + 2.0), (x0 + 2.2 + (k % 2) * 0.35, y0 + 0.5 + (k // 2) * 0.3, z0 + 2.1), mat("graphite")))
    out += lipo(x0 + 4.2, z0 + 0.7, y0, "cr_wrap_chg", 1.76, 0.58, 0.62, ("6S", "1800"))
    out += cable([(x0 + 2.8, y0 + 0.24, z0 + 1.0), (x0 + 3.6, y0 + 0.24, z0 + 1.0), (x0 + 3.6, y0 + 0.24, z0 + 2.75), (x0 + 4.0, y0 + 0.24, z0 + 2.75)], 0.06, "sil_red")
    return out


def charge_screen(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(18, 40, 70))
    d.text((W * 0.05, H * 0.28), "LiPo 6S  CHG", font=_fit(d, "LiPo 6S  CHG", FONT, round(H * 0.32), W * 0.9), fill=(220, 236, 255), anchor="lm")
    f = _fit(d, "2.0A  24.61V  62%", FONT, round(H * 0.3), W * 0.9)
    # live: the pack's voltage and how full it is (a charge of 40 minutes, then it holds)
    TK.live_part(img, (W * 0.05, H * 0.72), "2.0A  24.61V  62%", "24.61V", f, (140, 255, 160), "lm", bg=(18, 40, 70), src="value", base=24.61,
                 spread=0.05, dp=2, period=40, unit="V")
    TK.live_part(img, (W * 0.05, H * 0.72), "2.0A  24.61V  62%", "62%", f, (140, 255, 160), "lm", bg=(18, 40, 70), widest="100%", again=True, src="value",
                 base=40, rate=0.025, cycle=3000, max=100, unit="%")
    return img


def magnifier(x0, z0, y0):
    """The magnifier lamp clamped to the bench's back edge (the edge at z0, the bench's top at y0): the clamp and its
    screw, the post, the spring-balanced arms (two tubes each, their springs, the knuckle between), the yoke, and the
    round head: its white housing, the glass lens, the ring of light under it, the handle."""
    w = "white_mat"
    out = [box((x0 - 0.5, y0, z0 - 0.2), (x0 + 0.5, y0 + 0.4, z0 + 0.9), mat(w)),
           box((x0 - 0.5, y0 - 1.4, z0 - 0.5), (x0 + 0.5, y0 + 0.4, z0 - 0.2), mat(w)),
           box((x0 - 0.5, y0 - 1.4, z0 - 0.2), (x0 + 0.5, y0 - 1.0, z0 + 0.5), mat(w)),
           box((x0 - 0.08, y0 - 1.0, z0 + 0.22), (x0 + 0.08, y0 - 0.5, z0 + 0.38), mat("chrome")),
           box((x0 - 0.4, y0 - 1.3, z0 + 0.25), (x0 + 0.4, y0 - 1.18, z0 + 0.35), mat("graphite"))]
    out += round_part(x0, z0 + 0.35, 0.14, y0 + 0.4, y0 + 2.2, w, n=3) + round_part(x0, z0 + 0.35, 0.2, y0 + 2.2, y0 + 2.6, "graphite", n=3)
    # the lower arm: up and forward at 45 degrees (top toward the student, +z), its two tubes and spring
    ky, kz, L1 = y0 + 2.4, z0 + 0.35, 4.0
    rot1 = {"angle": 45, "axis": "x", "origin": [x0, ky, kz]}
    tubes = ((-0.42, 0.1, w), (0.32, 0.1, w), (0.52, 0.07, "coil"))
    for dx, t, c in tubes:
        out.append(box((x0 + dx, ky, kz - t / 2), (x0 + dx + t, ky + L1, kz + t / 2), mat(c), dict(rot1)))
    ey, ez = ky + L1 * 0.7071, kz + L1 * 0.7071
    out += [box((x0 - 0.55, ey - 0.3, ez - 0.3), (x0 + 0.7, ey + 0.3, ez + 0.3), mat("graphite"))]
    # the upper arm: forward and a little down (22.5 degrees)
    L2 = 2.6
    rot2 = {"angle": 22.5, "axis": "x", "origin": [x0, ey, ez]}
    for dx, t, c in tubes:
        out.append(box((x0 + dx, ey - t / 2, ez), (x0 + dx + t, ey + t / 2, ez + L2), mat(c), dict(rot2)))
    hy, hz = ey - L2 * 0.3827, ez + L2 * 0.9239
    out += [box((x0 - 0.55, hy - 0.3, hz - 0.25), (x0 + 0.7, hy + 0.25, hz + 0.3), mat("graphite"))]
    # the yoke down to the head, the head
    cz = hz + 1.6
    cy = hy - 0.9
    out += [box((x0 - 0.08, cy + 0.3, hz - 0.05), (x0 + 0.08, hy - 0.25, hz + 0.1), mat(w)),
            box((x0 - 0.08, cy + 0.3, hz - 0.05), (x0 + 0.08, cy + 0.45, cz - 1.45), mat(w))]
    out += ring(x0, cz, 1.45, 0.38, cy, cy + 0.5, w, n=6)
    out += [{**e, "shade": False} for e in ring(x0, cz, 1.3, 0.22, cy - 0.1, cy, "ring", n=6)]
    out += round_part(x0, cz, 1.25, cy + 0.5, cy + 0.6, "glass", n=6)
    out += [box((x0 - 0.18, cy + 0.12, cz + 1.4), (x0 + 0.18, cy + 0.38, cz + 2.2), mat(w))]
    return out


def bin_open(x0, z0, y0, colour, w=2.4, d=3.4, h=1.6):
    """A louvred parts bin, its open front to the south: the floor, the sides, the back with its hanging lip, the low
    front with the white label window, a few parts in it."""
    out = [box((x0, y0, z0), (x0 + w, y0 + 0.15, z0 + d), mat(colour)), box((x0, y0 + 0.15, z0), (x0 + 0.12, y0 + h, z0 + d), mat(colour)),
           box((x0 + w - 0.12, y0 + 0.15, z0), (x0 + w, y0 + h, z0 + d), mat(colour)), box((x0 + 0.12, y0 + 0.15, z0), (x0 + w - 0.12, y0 + h, z0 + 0.12), mat(colour)),
           box((x0 - 0.05, y0 + h - 0.2, z0 - 0.2), (x0 + w + 0.05, y0 + h, z0), mat(colour)),
           box((x0 + 0.12, y0 + 0.15, z0 + d - 0.12), (x0 + w - 0.12, y0 + h * 0.55, z0 + d), mat(colour)),
           box((x0 + 0.5, y0 + h * 0.2, z0 + d), (x0 + w - 0.5, y0 + h * 0.45, z0 + d + 0.1), mat("paper"))]
    for k in range(3):
        out.append(box((x0 + 0.4 + k * 0.6, y0 + 0.15, z0 + 1.0 + k * 0.5), (x0 + 0.7 + k * 0.6, y0 + 0.4, z0 + 1.3 + k * 0.5), mat("chrome" if k % 2 else "black")))
    return out


def build_table(v):
    """A student's build bench (180 by 75 cm, 90 cm high; built along x 0..32, its student to the south): the grey
    steel frame and its lower shelf of louvred parts bins, the laminate top, the ESD mat and its earth lead; the job on
    it a variant: 0 a training quad getting its motors (two on, two waiting with their props, the screws' tray, the
    soldering station, the hex drivers); 1 one finished but for its props, on the set-up laptop by USB (live), its
    pack on the charger; 2 one complete with props and battery under the magnifier lamp, the multimeter on the pack,
    the build sheet."""
    out = [box((2.4, BT - 0.5, 3.6), (29.6, BT, 15.6), {**mat("lam_edge"), "up": {"texture": "#lam_grey"}})]
    for x in (2.8, 28.6):
        for z in (4.0, 14.6):
            out.append(box((x, 0, z), (x + 0.6, BT - 0.5, z + 0.6), mat("powder")))
    out += [box((3.4, BT - 1.3, 4.0), (28.6, BT - 0.5, 4.6), mat("powder")), box((3.4, BT - 1.3, 14.6), (28.6, BT - 0.5, 15.2), mat("powder")),
            box((3.4, 2.4, 4.0), (28.6, 2.8, 15.2), mat("powder"))]
    for k, c in enumerate(("bin_blue", "bin_red", "bin_yellow", "bin_blue", "bin_yellow", "bin_red", "bin_blue", "bin_yellow")):
        out += bin_open(4.0 + k * 3.0, 11.0, 2.8, c)
    out += [box((3.6, BT, 5.0), (22.0, BT + 0.1, 14.2), mat("esd")), box((21.0, BT + 0.1, 5.2), (21.6, BT + 0.22, 5.6), mat("chrome"))]
    out += cable([(21.3, BT + 0.15, 5.15), (21.3, BT + 0.15, 3.45), (21.3, BT - 3.0, 3.45)], 0.05, "graphite")
    y = BT + 0.1
    if v == 0:
        out += quad(11.0, 9.6, y, motors=2)
        out += motor(4.8, 6.6, y, 0.22, 0.7) + motor(7.6, 6.6, y, 0.22, 0.7)
        out += prop(16.8, 12.6, y, 2.85, "pla_black", True) + prop(17.2, 6.4, y, 2.85, "pla_orange", False)
        out += LK.tray(4.4, 11.0, 3.4, 2.6, y, "black", "screws")
        out += solder_station(22.4, 4.2, BT)
        out += driver_stand(22.8, 9.6, BT)
    elif v == 1:
        out += quad(10.0, 9.4, y, motors=4, usb=True)
        out += laptop(22.6, 8.4, BT, "cr_fc", fc_config, "black", 6.0, 4.0, 3.8)
        out += cable([(11.6, y + 0.32, 9.4), (21.0, y + 0.32, 9.4), (21.0, y + 0.32, 11.0), (22.6, y + 0.32, 11.0)], 0.06, "black")
        out += charger(14.2, 11.2, y)
        out += LK.tray(4.4, 11.6, 3.0, 2.4, y, "black", "parts")
    else:
        out += quad(9.0, 9.6, y, motors=4, props=True, battery="cr_wrap_q2")
        out += multimeter(14.8, 9.6, y)
        out += magnifier(9.0, 3.6, BT)
        out += [box((22.6, BT, 5.0), (26.4, BT + 0.04, 10.4), faces("paper", up=PRT.face("cr_buildsheet", 3.8, 5.4, build_sheet)))]
        out += LK.tray(18.6, 11.8, 3.0, 2.2, y, "black", "screws")
    return out


# ------------------------------------------------------------------ on the walls
def controller_rack():
    """The training radios' charging shelf (2 x 1 on a wall, its front to the north; hung a metre up): the perforated
    back with the power strip, the shelf and its lip with the slots' labels and charge lights, the dividers, five radios
    leaning back in their slots on their charging leads (one slot empty: that radio is out), the sign."""
    out = [box((1.6, 0.0, 15.6), (30.4, 7.0, 16.0), mat("perf")),
           box((1.6, 0.6, 11.6), (30.4, 0.9, 15.6), mat("steel_black")), box((1.6, 0.9, 11.6), (30.4, 1.3, 11.9), faces("steel_black", north=PRT.face("cr_rackstrip", 28.8, 0.4, rack_strip))),
           box((3.0, 0.0, 12.4), (3.4, 0.6, 15.6), mat("steel_black")), box((28.6, 0.0, 12.4), (29.0, 0.6, 15.6), mat("steel_black")),
           box((2.0, 3.6, 15.0), (30.0, 4.4, 15.6), mat("white"))]
    for k in range(7):
        x = 1.6 + k * 4.8 - (0.1 if k == 6 else 0)
        out.append(box((max(1.6, x - 0.1), 0.9, 12.2), (min(30.4, x + 0.1), 2.2, 15.6), mat("steel_black")))
    for k in range(6):
        cx = 30.4 - 4.8 * (k + 0.5)          # (TX 01 at the viewer's left: the model's +x)
        state = ("led_green", "led_green", "led_amber", "led_off", "led_green", "led_green")[k]
        out.append({**box((cx - 1.6, 0.95, 11.56), (cx - 1.2, 1.25, 11.6), mat(state)), "shade": False})
        if k == 3:
            out += cable([(cx, 1.05, 15.4), (cx, 1.05, 13.4)], 0.07, "graphite")
            continue
        out += radio_standing(cx, 14.0, 0.9, ("tx_grey", "tx_white", "tx_grey", None, "tx_white", "tx_grey")[k], f"cr_txr_{k}", tx_screen(k + 1, (100, 96, 64, 0, 100, 88)[k]))
        out += cable([(cx + 0.9, 1.05, 14.2), (cx + 0.9, 1.05, 15.3), (cx + 0.9, 3.6, 15.3)], 0.06, "graphite")
    out += [box((9.6, 9.0, 15.6), (22.4, 11.0, 16.0), faces("white", north=PRT.face("cr_racksign", 12.8, 2.0, LK.label("TRAINING RADIOS\nsign out with your instructor", (250, 250, 246), (24, 30, 44), (30, 90, 200)))))]
    return out


def class_board(kind):
    """The teaching whiteboards' writing, in marker: the wind lesson's sketch and notes; the METAR's parts, the exam
    dates."""
    def draw(img):
        W, H = img.size
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, W, H), fill=(246, 247, 248))
        fb = ImageFont.truetype(FONT, max(8, round(H * 0.075)))
        fr = ImageFont.truetype(FONT_R, max(8, round(H * 0.058)))
        blue, red, green, black = (30, 60, 160), (190, 40, 40), (30, 120, 60), (40, 40, 44)
        if kind == 0:
            d.text((W * 0.04, H * 0.06), "WIND + BUILDINGS", font=fb, fill=blue)
            gy = H * 0.8
            d.line((W * 0.04, gy, W * 0.6, gy), fill=black, width=3)
            d.rectangle((W * 0.26, H * 0.42, W * 0.36, gy), outline=black, width=3)
            for k in range(3):
                y = H * (0.3 + k * 0.08)
                d.line((W * 0.04, y, W * 0.22, y), fill=blue, width=3)
                d.polygon([(W * 0.22, y - 6), (W * 0.24, y), (W * 0.22, y + 6)], fill=blue)
            d.arc((W * 0.36, H * 0.48, W * 0.5, H * 0.78), 200, 520, fill=red, width=3)
            d.text((W * 0.44, H * 0.38), "ROTOR!", font=fb, fill=red)
            for k, (t, c) in enumerate((("- speeds up over the roof", black), ("- eddies downwind", black), ("- stay 3x height away", red),
                                        ("- gusts: + half the wind", black))):
                d.text((W * 0.64, H * (0.24 + k * 0.12)), t, font=fr, fill=c)
            d.text((W * 0.64, H * 0.8), "HOMEWORK: p. 42", font=fb, fill=green)
        else:
            d.text((W * 0.04, H * 0.06), "METAR", font=fb, fill=blue)
            m = "TPT 141350Z 27015G25KT 9999 SCT030 14/06 Q1012"
            d.text((W * 0.04, H * 0.2), m, font=_fit(d, m, FONT, round(H * 0.06), W * 0.6), fill=black)
            for k, (a, b) in enumerate((("wind", "270 15 G25"), ("vis", "10 km+"), ("cloud", "SCT 3000 ft"), ("temp", "14 / 6"), ("QNH", "1012"))):
                d.text((W * 0.06, H * (0.34 + k * 0.1)), a, font=fr, fill=red)
                d.text((W * 0.22, H * (0.34 + k * 0.1)), b, font=fr, fill=black)
            d.line((W * 0.62, H * 0.08, W * 0.62, H * 0.92), fill=(200, 200, 204), width=2)
            d.text((W * 0.66, H * 0.08), "EXAMS", font=fb, fill=green)
            for k, t in enumerate(("Theory: Tue 28", "Radio: Thu 30", "Flight test: Nov 4", "Bring ID + logbook")):
                d.text((W * 0.66, H * (0.24 + k * 0.12)), t, font=fr, fill=black if k < 3 else red)
        return img
    return draw


def class_whiteboard(v):
    """A teaching whiteboard (2 x 1, 1.8 by 0.9 m; its face to the north), as the offices' but with the class's work
    on it: the aluminium frame, the board, the pen tray and its markers and eraser, a sheet held up by a magnet."""
    out = [box((0.6, 1.4, 15.0), (31.4, 2.0, 16.0), mat("alu")), box((0.6, 15.6, 15.0), (31.4, 16.2, 16.0), mat("alu")),
           box((0.6, 2.0, 15.0), (1.2, 15.6, 16.0), mat("alu")), box((30.8, 2.0, 15.0), (31.4, 15.6, 16.0), mat("alu")),
           box((1.2, 2.0, 15.4), (30.8, 15.6, 16.0), faces("white_mat", north=PRT.face(f"cr_board_{v}", 29.6, 13.6, class_board(v))))]
    out += [box((4.0, 1.0, 14.0), (28.0, 1.4, 15.4), mat("alu")), box((4.0, 1.4, 14.0), (28.0, 1.8, 14.2), mat("alu"))]
    for k, c in enumerate(("navy", "red", "green", "black")[: 3 + v]):
        x = 6.0 + k * 2.6
        out += [box((x, 1.4, 14.4), (x + 2.0, 1.75, 14.75), mat("white")), box((x + 2.0, 1.4, 14.4), (x + 2.5, 1.75, 14.75), mat(c))]
    out += [box((19.0, 1.4, 14.3), (22.4, 2.2, 15.0), mat("felt")), box((19.0, 2.2, 14.3), (22.4, 2.6, 15.0), mat("black"))]
    sheet = ("TIMETABLE", ["MON  airspace", "TUE  weather", "WED  radio", "THU  build", "FRI  flying"]) if v == 0 else \
        ("SAFETY", ["props off on", "the bench", "", "LiPo: bag", "+ attended"])
    out += [box((26.4, 9.0, 15.35), (29.8, 13.8, 15.4), faces("paper", north=PRT.face(f"cr_bsheet_{v}", 3.4, 4.8, PK.notam_sheet([sheet[0], ""] + sheet[1]))))]
    out += swap_yz(round_part(28.1, 13.4, 0.4, 15.05, 15.35, ("red", "navy")[v], n=3))
    return out


def classroom_poster(v):
    """A poster in its aluminium snap frame (A1 portrait; its front to the north): the backing, the four profiles
    standing proud of the print. v: which poster (airspace, phonetic alphabet, pre-flight check, LiPo safety, METAR)."""
    pic = PRT.face(f"cr_poster_{v}", 9.2, 13.2, poster_print(POSTERS[v]))
    return [box((3.0, 1.0, 15.5), (13.0, 15.0, 16.0), mat("alu")),
            box((3.4, 1.4, 15.44), (12.6, 14.6, 15.5), faces("paper", north=pic)),
            box((3.0, 14.6, 15.3), (13.0, 15.0, 15.5), mat("alu")), box((3.0, 1.0, 15.3), (13.0, 1.4, 15.5), mat("alu")),
            box((3.0, 1.4, 15.3), (3.4, 14.6, 15.5), mat("alu")), box((12.6, 1.4, 15.3), (13.0, 14.6, 15.5), mat("alu"))]


# ------------------------------------------------------------------ by the door
def backpack(x0, z0, y0, colour, w=4.4, d=2.6, h=5.4):
    """A backpack standing up, its front to the north: the rounded body, the front pocket and its zip, the side pocket
    with a bottle, the grab handle, the shoulder straps down its back."""
    out = RK.bevel_box((x0, y0, z0), (x0 + w, y0 + h, z0 + d), colour, b=0.35)
    out += RK.bevel_box((x0 + 0.6, y0 + 0.4, z0 - 0.7), (x0 + w - 0.6, y0 + 2.8, z0), colour, b=0.25)
    out += [box((x0 + 0.8, y0 + 2.5, z0 - 0.74), (x0 + w - 0.8, y0 + 2.62, z0 - 0.7), mat("grip_black")),
            box((x0 + 0.5, y0 + h - 0.6, z0 - 0.04), (x0 + w - 0.5, y0 + h - 0.48, z0), mat("grip_black")),
            box((x0 + 1.6, y0 + h, z0 + 1.0), (x0 + w - 1.6, y0 + h + 0.5, z0 + 1.4), mat("grip_black"))]
    for x in (x0 + 0.8, x0 + w - 1.6):
        out.append(box((x, y0 + 0.6, z0 + d), (x + 0.8, y0 + h - 0.4, z0 + d + 0.3), mat("grip_black")))
    return out


def vest(x0, z0, y0):
    """A hi-vis vest folded: the fluorescent fabric, the two silver bands across it."""
    return [box((x0, y0, z0), (x0 + 4.4, y0 + 0.6, z0 + 3.6), mat("hivis")),
            box((x0 - 0.02, y0 + 0.6, z0 + 0.8), (x0 + 4.42, y0 + 0.64, z0 + 1.3), mat("reflect")),
            box((x0 - 0.02, y0 + 0.6, z0 + 2.3), (x0 + 4.42, y0 + 0.64, z0 + 2.8), mat("reflect"))]


def cap(cx, cz, y0, colour="navy"):
    return round_part(cx, cz, 1.25, y0, y0 + 0.9, colour) + round_part(cx, cz, 0.2, y0 + 0.9, y0 + 1.0, colour) + \
        [box((cx - 1.0, y0, cz - 2.4), (cx + 1.0, y0 + 0.15, cz - 1.0), mat(colour))]


def bag_cubbies():
    """The students' cubbies by the door (180 by 45 cm, 90 high; built along x 0..32, its open front to the north):
    the laminate carcass, eight cubbies with a name on each, and in them what people leave: backpacks, a vest and cap,
    a lunch bag and a bottle, boots, a kit bag, a folded jacket, one empty; two binders and a cap on top."""
    lw = "lam_white"
    out = [box((1.9, 0, 9.2), (30.1, 0.8, 15.6), mat("black")),
           box((1.6, 0, 8.8), (2.0, 14.4, 16.0), mat(lw)), box((30.0, 0, 8.8), (30.4, 14.4, 16.0), mat(lw)),
           box((2.0, 14.0, 8.8), (30.0, 14.4, 16.0), mat(lw)), box((2.0, 0.8, 8.8), (30.0, 1.2, 16.0), mat(lw)),
           box((2.0, 7.4, 8.8), (30.0, 7.8, 16.0), mat(lw)), box((2.0, 1.2, 15.6), (30.0, 14.0, 16.0), mat("lam_grey"))]
    for k in range(1, 4):
        x = 2.0 + k * 7.0
        out += [box((x - 0.2, 1.2, 8.8), (x + 0.2, 7.4, 15.6), mat(lw)), box((x - 0.2, 7.8, 8.8), (x + 0.2, 14.0, 15.6), mat(lw))]
    names = ("NOOR", "KAI", "SPARE", "ZOE", "LIAM", "PRIYA", "OMAR", "ELLA")
    for k in range(8):
        row, col = k // 4, k % 4
        x0 = 2.0 + col * 7.0 + 1.6
        y = (13.6 if row == 0 else 7.0)
        sh = 14.0 if row == 0 else 7.4
        out.append(box((x0, sh, 8.76), (x0 + 3.8, sh + 0.4, 8.8), faces(lw, north=PRT.face(f"cr_cubby_{names[k]}", 3.8, 0.4, cubby_label(names[k])))))
    up, lo = 7.8, 1.2
    out += backpack(3.2, 10.6, up, "bag_navy")
    out += vest(10.4, 10.8, up) + cap(12.6, 12.0, up + 0.6)
    out += RK.bevel_box((24.4, up, 11.0), (27.6, up + 3.0, 13.4), "lunch", b=0.25) + [box((25.6, up + 3.0, 12.0), (26.4, up + 3.5, 12.4), mat("grip_black"))]
    out += bottle(28.6, 12.0, up, "teal")
    out += RK.boot(3.4, 10.4, lo) + RK.boot(5.8, 10.6, lo)
    out += RK.bevel_box((10.0, lo, 10.4), (15.6, lo + 3.2, 14.8), "bag_red", b=0.35) + [box((11.6, lo + 3.2, 12.2), (14.0, lo + 3.7, 12.6), mat("grip_black"))]
    out += backpack(18.4, 11.0, lo, "bag_green", 4.2, 2.4, 5.0)
    for k in range(3):
        out.append(box((24.0 + k * 0.15, lo + k * 0.8, 10.4 + k * 0.2), (29.0 - k * 0.1, lo + 0.8 + k * 0.8, 15.0 - k * 0.15), mat("jacket")))
    out += binder(4.0, 10.0, 14.4, "MODULE 1", "binder_navy", (34, 52, 96)) + binder(4.4, 9.8, 15.2, "MODULE 2", "binder_teal", (28, 120, 120))
    out += cap(24.0, 12.8, 14.4, "red")
    return out


# ------------------------------------------------------------------ the carrels
CT = 11.84


def study_carrel(v):
    """A computer-based-training carrel (1 x 1, its user to the south looking north): the fabric side and front panels
    with their aluminium caps, the desk, the monitor (live: v 0 the theory exam mid-way, 1 its result, 2 the sign-in, 3
    a practice quiz), the keyboard and mouse, the headphones, the computer under the desk, its number on top."""
    out = [box((0.0, 0, 3.6), (0.6, 18.4, 15.6), mat("fab_grey")), box((15.4, 0, 3.6), (16.0, 18.4, 15.6), mat("fab_grey")),
           box((0.6, 0, 3.6), (15.4, 18.4, 4.2), mat("fab_blue")),
           box((0.0, 18.4, 3.6), (0.6, 18.7, 15.6), mat("alu")), box((15.4, 18.4, 3.6), (16.0, 18.7, 15.6), mat("alu")),
           box((0.6, 18.4, 3.6), (15.4, 18.7, 4.2), mat("alu")),
           box((0.6, CT - 0.4, 4.2), (15.4, CT, 15.2), {**mat("lam_edge"), "up": {"texture": "#lam_white"}, "down": {"texture": "#lam_grey"}})]
    tag = PRT.face(f"cr_carrel_{v}", 2.8, 1.2, carrel_tag(v + 1))
    out += [box((6.6, 18.7, 3.75), (9.4, 19.9, 4.05), {**mat("graphite"), "north": tag, "south": tag})]
    out += [box((6.4, CT, 5.0), (9.6, CT + 0.26, 7.4), mat("black")), box((7.6, CT + 0.26, 5.6), (8.4, 13.6, 6.2), mat("black")),
            box((4.15, 13.4, 6.0), (11.85, 17.9, 6.5), mat("black")),
            {**box((4.4, 13.65, 6.5), (11.6, 17.65, 6.56), {**mat("black"), "south": SCR.face(f"cr_exam_{v}", 7.2, 4.0, exam(("exam", "pass", "login", "practice")[v]))}), "shade": False}]
    out += [box((4.6, CT, 9.4), (11.4, CT + 0.3, 11.6), faces("black", up=A.region("cr_kbd", 6.8, 2.2, MK.keyboard_top))),
            box((12.4, CT, 10.0), (13.4, CT + 0.4, 11.6), mat("black"))]
    if v in (0, 3):
        out += [box((1.4, CT, 11.4), (4.0, CT + 0.04, 14.8), faces("paper", up=PRT.face(f"cr_scrap_{v}", 2.6, 3.4, workbook(4400 + v))))] + pen(3.0, 11.8, CT, "black")
    if v == 3:
        out += round_part(13.0, 7.2, 0.6, CT, CT + 0.4, "black") + round_part(13.0, 9.0, 0.6, CT, CT + 0.4, "black") + \
            [box((12.8, CT + 0.4, 7.0), (13.2, CT + 0.7, 9.2), mat("black"))]
    else:
        # hung on the hook on the side panel: the band over it, the two padded cups hanging
        out += [box((14.8, 16.0, 9.0), (15.4, 16.3, 9.4), mat("chrome")), box((14.4, 16.3, 7.9), (14.9, 16.6, 10.5), mat("black")),
                box((14.4, 15.2, 7.9), (14.8, 16.3, 8.3), mat("black")), box((14.4, 15.2, 10.1), (14.8, 16.3, 10.5), mat("black")),
                box((14.2, 13.9, 7.6), (14.8, 15.2, 8.7), mat("grip_black")), box((14.2, 13.9, 9.7), (14.8, 15.2, 10.8), mat("grip_black"))]
    out += [box((11.0, 7.6, 8.0), (15.0, CT - 0.4, 14.0), mat("black")), {**box((14.2, 10.6, 14.0), (14.5, 10.9, 14.04), mat("led_green")), "shade": False}]
    return out


# ------------------------------------------------------------------ writing
def write(assets, ns="jugcraft", pages=None):
    global A, SCR, PRT
    TK.PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    import lounge_kit as LG
    mats = {**TK.MATERIALS, **MK.MATERIALS, **LK.MATERIALS, **RK.MATERIALS, **PK.MATERIALS, **OK.MATERIALS, **MATERIALS}
    for k in ("lk2_steel_black", "lk2_glassblack"):
        mats[k] = LG.MATERIALS[k]
    for name, make in mats.items():
        make().save(tex_dir / f"{name}.png")
    A = PK.ShelfAtlas(512, TX, "atlas")
    SCR = PK.ShelfAtlas(2048, 3 * TX, "screens")
    PRT = PK.ShelfAtlas(2048, 2 * TX, "prints")
    TK.SCR = SCR
    for m in (MK, LK, RK, PK, OK):
        m.A, m.SCR = A, SCR
    ref = lambda n: f"{ns}:block/{n}"
    T = {(k[4:] if k.startswith("lk2_") else k[3:]): ref(k) for k in mats}
    T.update({"atlas": ref("cr_atlas"), "screens": ref("cr_screens"), "prints": ref("cr_prints"), "particle": ref("cr_lam_white")})

    def model(name, elements, prepared=False):
        if not prepared:
            prep(elements)
        MW.fit_uvs(elements)
        (model_dir / f"{name}.json").write_text(json.dumps({"textures": T, "elements": elements}, indent=1) + "\n")

    def states(name, variants):
        (bs_dir / f"{name}.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")

    def prep(elements):
        for e in elements:
            for i in range(3):
                if e["from"][i] > e["to"][i]:
                    e["from"][i], e["to"][i] = e["to"][i], e["from"][i]
        MW.close_open_faces(elements)
        MW.separate_coplanar(elements, pin_uv=False)
        return elements

    def faced(model_name, extra="", off=0):
        return {f"facing={f}{extra}": {"model": ref(model_name), **({"y": (y - off) % 360} if (y - off) % 360 else {})} for f, y in FACINGS.items()}

    def wide(name, els, n, extra=""):
        v = {}
        for p, part in enumerate(cut_parts(prep(els), n)):
            model(f"{name}_{p}", part, True)
            v.update(faced(f"{name}_{p}", f",part={p}{extra}"))
        return v

    def wide_variants(name, make, n, count):
        v = {}
        for k in range(count):
            for p, part in enumerate(cut_parts(prep(make(k)), n)):
                model(f"{name}_{k}_{p}", part, True)
                v.update(faced(f"{name}_{k}_{p}", f",part={p},variant={k}"))
        return v

    def big(name, els, n):
        v = {}
        for h, half in zip(("lower", "upper"), cut_y(prep(els), 2)):
            for p, part in enumerate(cut_parts(half, n)):
                model(f"{name}_{h}_{p}", part, True)
                v.update(faced(f"{name}_{h}_{p}", f",half={h},part={p}"))
        return v

    states("training_table", wide_variants("training_table", training_table, 2, len(TABLE_PLACES)))
    states("instructor_desk", wide("instructor_desk", instructor_desk(), 2))
    states("touch_display_cart", big("touch_display_cart", touch_display_cart(), 2))
    states("parts_board", big("parts_board", parts_board(), 3))
    states("build_table", wide_variants("build_table", build_table, 2, 3))
    states("controller_rack", wide("controller_rack", controller_rack(), 2))
    states("class_whiteboard", wide_variants("class_whiteboard", class_whiteboard, 2, 2))
    states("bag_cubbies", wide("bag_cubbies", bag_cubbies(), 2))
    v = {}
    for k in range(len(POSTERS)):
        model(f"classroom_poster_{k}", classroom_poster(k))
        v.update(faced(f"classroom_poster_{k}", f",variant={k}"))
    states("classroom_poster", v)
    v = {}
    for k in range(4):
        model(f"study_carrel_{k}", study_carrel(k))
        v.update(faced(f"study_carrel_{k}", f",variant={k}"))
    states("study_carrel", v)
    A.img.save(tex_dir / "cr_atlas.png")
    SCR.img.save(tex_dir / "cr_screens.png")
    PRT.img.save(tex_dir / "cr_prints.png")
    return [(a.y + a.row, a.size) for a in (A, SCR, PRT)]


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
