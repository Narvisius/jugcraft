"""The pilot simulator room (work list item 22 part B, room 9), to the standard Justin set on the lab: everything at its
real size (a pixel is 6.25 cm), modelled rather than painted on, the screens live in game, varied.

Pieces: instructor_station (3 x 1: the instructor's desk, its six repeaters of the simulators and its control panel),
headset_rack (2 x 1, on a wall), acoustic_panel (on a wall; its colour a variant), waiting_bench (3 x 1). With them the
tower's Pilot Simulators and wall screen, the crew rest's lockers and water cooler, the offices' team table and the
planning office's filing cabinets, the clock.
"""
from __future__ import annotations

import json
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import model_writer as MW
import training_kit as TK
import manager_kit as MK
import lab_kit as LK
import rest_kit as RK
import planning_kit as PK
import office_kit as OK
from training_kit import box, mat, faces, round_part, round_ring, cut_parts, swap_yz, TX, FONT, FONT_R, FACINGS

ROOT = Path(__file__).resolve().parent.parent
A = None
SCR = None
MONO = MK.MONO
swap_xy, rod_x = LK.swap_xy, LK.rod_x
cable, cut_y, label_plate = RK.cable, RK.cut_y, RK.label_plate
_fit = MK._text_fit

MATERIALS = {
    "sk_console": lambda: PK.laminate((52, 56, 62), 3801), "sk_panel": lambda: PK.laminate((40, 44, 50), 3802),
    "sk_acoustic_0": lambda: MK.weave((58, 62, 68), 3803, 1, 4), "sk_acoustic_1": lambda: MK.weave((52, 78, 110), 3804, 1, 4),
    "sk_acoustic_2": lambda: MK.weave((46, 100, 96), 3805, 1, 4), "sk_seat": lambda: MK.weave((40, 70, 120), 3806, 1, 4),
    "sk_estop": lambda: TK.solid((200, 30, 30), 3807, 1.2), "sk_estop_base": lambda: TK.solid((236, 196, 30), 3808, 1.2),
    "sk_btn_green": lambda: TK.solid((50, 170, 80), 3809, 1.2), "sk_btn_amber": lambda: TK.solid((236, 160, 40), 3810, 1.2),
    "sk_btn_blue": lambda: TK.solid((50, 110, 210), 3811, 1.2), "sk_btn_white": lambda: TK.solid((230, 230, 230), 3812, 1.0),
}


# ------------------------------------------------------------------ pictures
def sim_feed(n):
    """A repeater of simulator n (live in game: what that pilot sees): its centre screen's view of the simulators'
    Minecraft world (tools/sim_world.py) with the HUD's main lines, the sim's number and its state."""
    import sim_world as SW

    def draw(img):
        W, H = img.size
        view, marks = SW.repeater(n, W, H, live=True)
        img.paste(view, (0, 0))
        for mk in marks:
            mk = dict(mk)
            TK.live(img, mk.pop("kind"), mk.pop("rect"), **mk)
        return img
    return draw


def panel_print(img):
    """The instructor's control panel: its buttons' legends and the panel's sections."""
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(40, 44, 50))
    f = ImageFont.truetype(FONT, max(7, round(H * 0.09)))
    for k, t in enumerate(("SIM 1", "SIM 2", "SIM 3", "SIM 4", "SIM 5", "SIM 6")):
        x = W * (0.06 + k * 0.1)
        d.text((x + W * 0.035, H * 0.08), t, font=f, fill=(200, 200, 206), anchor="mm")
    for r, t in enumerate(("FREEZE", "FAULT", "RESET")):
        d.text((W * 0.005, H * (0.36 + r * 0.24)), t, font=ImageFont.truetype(FONT, max(7, round(H * 0.07))), fill=(160, 160, 170), anchor="lm")
    d.line((W * 0.68, H * 0.05, W * 0.68, H * 0.95), fill=(90, 94, 100), width=2)
    for r, t in enumerate(("WIND +10", "GUSTS", "RAIN", "NIGHT")):
        d.text((W * 0.855, H * (0.2 + r * 0.2)), t, font=ImageFont.truetype(FONT, max(7, round(H * 0.07))), fill=(200, 200, 206), anchor="rm")
    d.text((W * 0.97, H * 0.92), "E-STOP", font=ImageFont.truetype(FONT, max(7, round(H * 0.07))), fill=(240, 200, 60), anchor="rm")
    return img


def lesson_plan(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(250, 250, 248))
    d.text((W / 2, H * 0.06), "LESSON 7: GPS LOSS", font=_fit(d, "LESSON 7: GPS LOSS", FONT, round(H * 0.06), W * 0.9), fill=(30, 30, 34), anchor="mm")
    fm = ImageFont.truetype(FONT_R, max(7, round(H * 0.035)))
    for k, t in enumerate(("1. take off, climb to 120 m", "2. route TWR - BRAVO", "3. at BRAVO: GPS fails", "4. hold, switch to vision nav",
                           "5. return to the tower", "6. land on pad 2", "", "pass: on pad within 3 m", "time: 25 min")):
        d.text((W * 0.06, H * (0.16 + k * 0.08)), t, font=fm, fill=(30, 30, 34))
    for k in range(3):
        y = H * (0.88 + k * 0.03)
        d.line((W * 0.06, y, W * 0.94, y), fill=(190, 190, 194), width=1)
    return img


def hook_labels(img):
    W, H = img.size
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, H), fill=(30, 32, 36))
    for k in range(6):
        x = W * (0.09 + k * 0.164)
        d.text((x, H / 2), f"SIM {k + 1}", font=_fit(d, "SIM 6", FONT, round(H * 0.6), W * 0.12), fill=(236, 236, 236), anchor="mm")
    return img


# ------------------------------------------------------------------ the instructor's station
def instructor_station():
    """The instructor's station (3 x 1, built along x 0..48: its user at the south looking north over the simulators):
    the dark console desk on its panel legs with its modesty panel at the front; at the back the frame of six repeaters
    facing the instructor, one a simulator (live in game: what each pilot sees); the sloped control panel (each sim's
    freeze, fault and reset buttons, the weather switches, the emergency stop); keyboard, mouse, the headset, the desk
    microphone on its gooseneck, the lesson plan, a mug; the computer under it."""
    c, pn = "console", "panel"
    t = 11.8
    out = [box((0.2, t - 0.6, 1.2), (47.8, t, 14.6), mat(c)), box((0.2, t - 1.2, 14.2), (47.8, t - 0.6, 14.6), mat(c)),
           box((0.6, 0, 1.6), (1.6, t - 0.6, 14.2), mat(pn)), box((46.4, 0, 1.6), (47.4, t - 0.6, 14.2), mat(pn)),
           box((1.6, 3.0, 1.6), (46.4, t - 0.6, 2.2), mat(pn)), box((23.6, 0, 1.6), (24.4, t - 0.6, 14.2), mat(pn))]
    # the repeaters' frame at the back and the six screens facing the instructor (two rows of three)
    out += [box((9.6, t, 1.4), (38.4, t + 0.4, 3.4), mat("graphite")), box((23.6, t + 0.4, 1.8), (24.4, t + 12.2, 2.4), mat("graphite")),
            box((9.6, t + 12.2, 1.8), (38.4, t + 12.6, 2.4), mat("graphite"))]
    for r in range(2):
        for k in range(3):
            n = r * 3 + k
            x0 = 10.0 + k * 9.4
            y0 = t + 1.4 + (1 - r) * 5.4
            out += [box((x0, y0, 2.4), (x0 + 9.0, y0 + 5.2, 3.0), mat("black")),
                    {**box((x0 + 0.2, y0 + 0.2, 3.0), (x0 + 8.8, y0 + 5.0, 3.05), faces("black", south=SCR.face(f"sk_feed_{n}", 8.6, 4.8, sim_feed(n)))), "shade": False}]
    # the control panel, sloped up away from the instructor; its buttons; the switches; the emergency stop
    rot = {"angle": 22.5, "axis": "x", "origin": [24.0, t, 8.4]}
    out += [box((8.0, t, 5.2), (40.0, t + 1.8, 8.4), mat(pn), dict(rot)),
            box((8.4, t + 1.8, 5.4), (39.6, t + 1.85, 8.2), faces(pn, up=SCR.face("sk_panel", 31.2, 2.8, panel_print)), dict(rot))]
    for k in range(6):
        bx = 8.4 + 31.2 * (0.06 + k * 0.1)
        for r, col in enumerate(("btn_blue", "btn_amber", "btn_white")):
            bz = 5.4 + 2.8 * (0.36 + r * 0.24) - 0.25
            out.append(box((bx + 0.3, t + 1.85, bz), (bx + 1.5, t + 2.15, bz + 0.5), mat(col), dict(rot)))
    for r in range(4):
        bz = 5.4 + 2.8 * (0.2 + r * 0.2) - 0.2
        out += [box((35.4, t + 1.85, bz), (36.2, t + 2.05, bz + 0.4), mat("chrome"), dict(rot)), box((35.65, t + 2.05, bz + 0.1), (35.95, t + 2.6, bz + 0.3), mat("chrome"), dict(rot))]
    stop = round_part(38.4, 6.6, 0.9, t + 1.85, t + 2.1, "estop_base", n=3) + round_part(38.4, 6.6, 0.6, t + 2.1, t + 2.8, "estop", n=3)
    for e in stop:
        e["rotation"] = dict(rot)
    out += stop
    # keyboard, mouse, headset, microphone, the lesson plan, a mug, the computer
    out += [box((14.0, t, 10.4), (22.0, t + 0.35, 13.0), faces("black", up=A.region("sk_kbd", 8.0, 2.6, MK.keyboard_top))),
            box((22.6, t, 10.2), (26.6, t + 0.05, 13.4), mat("grip_black")), box((24.0, t + 0.05, 11.0), (25.0, t + 0.45, 12.4), mat("black"))]
    out += [box((28.0, t, 11.0), (30.0, t + 0.6, 12.6), mat("black")), box((28.2, t + 0.6, 11.6), (29.8, t + 2.2, 12.0), mat("black")),
            box((27.6, t + 0.2, 11.2), (28.0, t + 1.6, 12.4), mat("black")), box((30.0, t + 0.2, 11.2), (30.4, t + 1.6, 12.4), mat("black"))]
    out += round_part(6.0, 9.6, 0.8, t, t + 0.3, "black", n=3)
    out += cable([(6.0, t + 0.3, 9.6), (6.0, t + 2.6, 9.6), (6.0, t + 3.4, 10.4), (6.0, t + 3.4, 11.2)], 0.12, "graphite")
    out += [box((5.7, t + 3.1, 11.2), (6.3, t + 3.7, 12.0), mat("black")), {**box((5.6, t + 0.3, 9.0), (5.8, t + 0.4, 9.2), mat("light")), "shade": False}]
    out += [box((40.0, t, 9.4), (43.4, t + 0.1, 13.8), faces("paper", up=SCR.face("sk_lesson", 3.4, 4.4, lesson_plan))),
            box((43.8, t, 11.0), (46.2, t + 0.2, 11.2), mat("navy"))]
    out += MK.round_ring(3.4, 11.6, 0.55, 0.1, t, t + 1.4, "mug_grey", 1.0, "coffee", n=4)
    out += [box((40.0, 0, 4.0), (43.0, 7.6, 11.0), mat("black")), box((40.0, 6.4, 11.0), (43.0, 7.0, 11.02), mat("graphite")),
            {**box((41.3, 5.8, 11.0), (41.7, 6.0, 11.04), mat("light")), "shade": False}]
    return out


# ------------------------------------------------------------------ on the walls
def headset_rack():
    """The pilots' headsets on the wall (2 x 1; their front to the north): the steel rail with its labels (SIM 1 to 6),
    six hooks, a headset on five of them (the band, the two ear cups, the boom microphone, the coiled lead hanging)."""
    out = [box((1.0, 8.0, 15.0), (31.0, 9.6, 16.0), mat("steel_black")),
           box((1.2, 8.3, 14.96), (30.8, 9.3, 15.0), faces("steel_black", north=SCR.face("sk_hooks", 29.6, 1.0, hook_labels)))]
    for k in range(6):
        hx = 31.0 - 30.0 * (0.09 + k * 0.164) - 0.25
        out += [box((hx, 7.4, 13.6), (hx + 0.5, 8.0, 15.0), mat("chrome")), box((hx, 7.4, 13.4), (hx + 0.5, 8.4, 13.6), mat("chrome"))]
        if k == 3:
            continue
        cx = hx + 0.25
        out += [box((cx - 1.4, 7.9, 13.0), (cx + 1.4, 8.3, 14.2), mat("black")),
                box((cx - 1.8, 5.2, 12.6), (cx - 1.4, 8.0, 14.6), mat("black")), box((cx + 1.4, 5.2, 12.6), (cx + 1.8, 8.0, 14.6), mat("black")),
                box((cx - 2.3, 4.8, 12.4), (cx - 1.8, 7.0, 14.8), mat("grip_black")), box((cx + 1.8, 4.8, 12.4), (cx + 2.3, 7.0, 14.8), mat("grip_black")),
                box((cx - 2.4, 5.4, 11.0), (cx - 2.2, 5.6, 12.4), mat("graphite")), box((cx - 2.5, 5.3, 10.6), (cx - 2.1, 5.7, 11.0), mat("black"))]
        out += cable([(cx + 2.05, 4.8, 13.6), (cx + 2.05, 1.2, 13.6)], 0.15, "coil")
    return out


def acoustic_panel(v):
    """An acoustic panel on the wall (90 cm square, 5 cm thick; its face to the north): the fabric front, its rounded
    edge stepped back, the shadow gap to the wall. v: its colour (charcoal, slate blue, teal)."""
    f = f"acoustic_{v}"
    return [box((1.2, 1.2, 15.6), (14.8, 14.8, 16.0), mat("black")),
            box((0.8, 0.8, 15.0), (15.2, 15.2, 15.6), mat(f)), box((1.0, 1.0, 14.8), (15.0, 15.0, 15.0), mat(f))]


def waiting_bench():
    """Beam seating for the pilots waiting their turn (3 x 1, built along x 0..48, its sitters looking north): the steel
    beam on two legs and feet, three upholstered seats with their backs, a gap and a little table at one end with a
    magazine and a cup."""
    s = "steel_black"
    out = [box((4.0, 4.2, 7.8), (44.0, 5.0, 8.6), mat(s))]
    for x in (6.0, 30.0):
        out += [box((x, 0, 4.0), (x + 0.8, 0.4, 12.6), mat(s)), box((x, 0.4, 7.8), (x + 0.8, 4.2, 8.6), mat(s))]
    for k in range(3):
        x0 = 4.4 + k * 9.4
        out += [box((x0 + 1.0, 5.0, 6.8), (x0 + 1.6, 6.8, 7.4), mat(s)), box((x0 + 7.0, 5.0, 6.8), (x0 + 7.6, 6.8, 7.4), mat(s))]
        out += RK.bevel_box((x0, 6.8, 3.6), (x0 + 8.6, 7.8, 10.4), "seat", b=0.3)
        out += [box((x0 + 1.0, 5.0, 10.2), (x0 + 1.6, 9.0, 10.8), mat(s)), box((x0 + 7.0, 5.0, 10.2), (x0 + 7.6, 9.0, 10.8), mat(s))]
        out += [box((x0, 9.0, 10.4), (x0 + 8.6, 14.1, 11.4), mat("seat")), box((x0 + 0.3, 14.1, 10.5), (x0 + 8.3, 14.4, 11.3), mat("seat"))]
    out += [box((34.0, 5.0, 5.0), (43.6, 5.6, 11.4), mat("oak")), box((38.4, 0.4, 7.8), (39.2, 5.0, 8.6), mat(s)), box((36.0, 0, 6.0), (41.6, 0.4, 10.4), mat(s))]
    out += [box((35.0, 5.6, 6.0), (39.6, 5.8, 10.4), faces("paper", up=A.region("sk_mag", 4.6, 4.4, MK.magazine("DRONE WORLD", (40, 80, 160)))))]
    out += MK.round_ring(41.8, 8.6, 0.5, 0.08, 5.6, 7.0, "white", 0.9, "coffee", n=4)
    return out


# ------------------------------------------------------------------ writing
def write(assets, ns="jugcraft", pages=None):
    global A, SCR
    TK.PAGES = pages
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    for d in (tex_dir, model_dir, bs_dir):
        d.mkdir(parents=True, exist_ok=True)
    mats = {**TK.MATERIALS, **MK.MATERIALS, **LK.MATERIALS, **RK.MATERIALS, **PK.MATERIALS, **OK.MATERIALS, **MATERIALS}
    import lounge_kit as LG
    mats["lk2_steel_black"] = LG.MATERIALS["lk2_steel_black"]
    for name, make in mats.items():
        make().save(tex_dir / f"{name}.png")
    A = PK.ShelfAtlas(256, TX, "atlas")
    SCR = PK.ShelfAtlas(2048, 4 * TX, "screens")
    TK.SCR = SCR
    for m in (MK, LK, RK, PK, OK):
        m.A, m.SCR = A, SCR
    ref = lambda n: f"{ns}:block/{n}"
    T = {(k[4:] if k.startswith("lk2_") else k[3:]): ref(k) for k in mats}
    T.update({"atlas": ref("sk_atlas"), "screens": ref("sk_screens"), "particle": ref("sk_console")})

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

    def wide(name, els, n):
        v = {}
        for p, part in enumerate(cut_parts(prep(els), n)):
            model(f"{name}_{p}", part, True)
            v.update(faced(f"{name}_{p}", f",part={p}"))
        return v

    states("instructor_station", wide("instructor_station", instructor_station(), 3))
    states("headset_rack", wide("headset_rack", headset_rack(), 2))
    v = {}
    for k in range(3):
        model(f"acoustic_panel_{k}", acoustic_panel(k))
        v.update(faced(f"acoustic_panel_{k}", f",variant={k}"))
    states("acoustic_panel", v)
    states("waiting_bench", wide("waiting_bench", waiting_bench(), 3))
    A.img.save(tex_dir / "sk_atlas.png")
    SCR.img.save(tex_dir / "sk_screens.png")
    return A.y + A.row, SCR.y + SCR.row


if __name__ == "__main__":
    print(write(ROOT / "src/main/resources/assets/jugcraft"))
