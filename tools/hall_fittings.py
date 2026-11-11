"""The turbine hall's fittings (turbine hall part 3): the overhead crane's pendant station. Built as the diesel rooms'
fittings (tools/diesel_fittings.py): a front drawn at 512 px a block, every part that stands out of it a real box, what
moves the game's (client/LivePanelRenderer.java, live_panels.json; nuclear/HallCrane.java).

Justin's answers (turbine hall, 2026-10-18): the crane "Works from a pendant" (like the diesel monorail's: the bridge
along the hall, the trolley across, the hook up and down).

Frames as the panels': x right as you face it, y up, z out of the wall toward you (model pixels; the front "south")."""
import plant_panels as PP
from plant_panels import Front, INK, _sc_round
import diesel_panels as DP
import diesel_fittings as DF
from diesel_fittings import conduit_up

LABELS = DP.LABELS
WHITE, BLACK, RED = (238, 238, 232, 255), (24, 24, 26, 255), (196, 36, 30, 255)
YELLOW = (236, 190, 30, 255)
BLK = "pi_pnl_black"

PENDANT = "tb_crane_pendant"

# ------------------------------------------------------------------------------------------------ the crane pendant

# The crane's pendant station on the wall (its controls, parked in their holder): a yellow pendant, its cable up into a
# junction box and the conduit on up the wall, the crane's tag on the wall beside it. On the pendant, top to bottom: the
# red STOP mushroom; START (green) and the POWER lamp (live); the main hoist's UP and DOWN, the auxiliary hoist's UP and
# DOWN; the travel buttons NORTH, SOUTH, WEST and EAST (whichever of the bridge and the trolley runs that way); the HORN.
# Holding a button runs the crane that way (a real pendant's push buttons: let go and it stops); STOP stops everything and
# takes the crane's power off until START (nuclear/HallCrane.java).
CP_BODY = (3.4, 2.4, 12.6, 24.6)
CP_D = 4.6                                     # its face (model px out of the wall)
CP_STOP = (8.0, 22.7)
CP_COLS = (5.6, 10.4)                          # the buttons' two columns
# Every button the player can press: its id (HallCrane), middle and reach (model px).
CP_BUTTONS = [("stop", 8.0, 22.7, 1.5), ("start", 5.6, 19.6, 1.1), ("main_up", 5.6, 15.5, 1.2), ("main_down", 10.4, 15.5, 1.2),
              ("aux_up", 5.6, 12.2, 1.2), ("aux_down", 10.4, 12.2, 1.2), ("north", 5.6, 8.9, 1.2), ("south", 10.4, 8.9, 1.2),
              ("west", 5.6, 6.0, 1.2), ("east", 10.4, 6.0, 1.2), ("horn", 8.0, 3.8, 1.0)]
CP_POWER = (10.4, 19.6)
CP_HEADS = {"main_up": "up", "main_down": "down", "aux_up": "up", "aux_down": "down", "north": "N", "south": "S", "west": "W", "east": "E",
            "horn": "horn"}


def _arrow(f, x, y, up):
    import math
    a = 90 if up else -90
    pts = []
    for da, r in ((0, 0.52), (140, 0.48), (220, 0.48)):
        t = math.radians(a + da)
        pts.append((x + math.cos(t) * r, y + math.sin(t) * r))
    f.d.polygon([f.px(px, py) for px, py in pts], fill=(240, 240, 234, 255))


def _tag(f, x0, y0, x1, y1, lines):
    """An engraved aluminium tag on the wall: brushed metal, its letters cut in dark, a rivet at each end."""
    import random
    r = random.Random(7)
    for k in range(int((y1 - y0) * PP.U)):
        v = 196 + r.randint(-8, 8)
        f.d.line([(x0 * PP.U, (32 - y1) * PP.U + k), (x1 * PP.U - 1, (32 - y1) * PP.U + k)], fill=(v, v + 3, v + 5, 255))
    f.R(x0, y0, x1, y1, outline=(120, 124, 128, 255), width=2)
    for x in (x0 + 0.45, x1 - 0.45):
        f.E(x, (y0 + y1) / 2, 0.16, fill=(70, 72, 76, 255))
    n = len(lines)
    for i, text in enumerate(lines):
        y = (y0 + y1) / 2 + (n - 1) * 0.36 - i * 0.72
        f.T((x0 + x1) / 2, y, text, 8 if i == 0 else 6.5, fill=(36, 38, 42, 255), bold=True)
    f.proud(x0, y0, x1, y1, 0.12, "pi_chrome")


def crane_pendant():
    f = Front(1, 641)
    x0, y0, x1, y1 = CP_BODY
    f.R(x0, y0, x1, y1, fill=YELLOW)
    # The legends on the pendant's face, the arrows and letters on the buttons' heads.
    T = lambda x, y, s, size=5.2: f.T(x, y, s, size, fill=INK, bold=True)
    T(CP_COLS[0], 18.25, "START")
    T(CP_COLS[1], 18.25, "POWER")
    T(8.0, 16.95, "MAIN HOIST 160 T")
    T(8.0, 13.65, "AUX HOIST 25 T")
    T(8.0, 10.35, "TRAVEL")
    f.T(x0 + 0.5, 3.8, "HORN", 5.2, fill=INK, bold=True, anchor="lm")
    for key, bx, by, _ in CP_BUTTONS:
        head = CP_HEADS.get(key)
        if key in ("stop", "start"):
            continue
        f.E(bx, by, 0.85, fill=(28, 28, 30, 255))
        if head in ("up", "down"):
            _arrow(f, bx, by, head == "up")
        elif head == "horn":
            # A horn's bell, its sound coming out of it.
            c = (240, 240, 234, 255)
            f.d.polygon([f.px(bx - 0.55, by - 0.12), f.px(bx - 0.55, by + 0.12), f.px(bx - 0.05, by + 0.38), f.px(bx - 0.05, by - 0.38)], fill=c)
            for r in (0.28, 0.5):
                f.d.arc([f.px(bx - 0.05 - r, by + r), f.px(bx - 0.05 + r, by - r)], -45, 45, fill=c, width=3)
        else:
            f.T(bx, by, head, 7.5, fill=(240, 240, 234, 255), bold=True)
    f.E(CP_COLS[0], 19.6, 0.85, fill=(40, 150, 60, 255))
    # The crane's tag on the wall beside the junction box.
    _tag(f, 0.9, 27.0, 6.3, 29.2, ["TB CRANE 1", "PENDANT STATION"])
    f.live.append({"t": "lamp", "x": CP_POWER[0], "y": CP_POWER[1], "r": 0.5, "z": CP_D + 0.5, "colour": [240, 240, 230], "ch": "crane_power"})
    return f


def crane_pendant_boxes():
    F = f"pi_{PENDANT}_f"
    x0, y0, x1, y1 = CP_BODY
    face = {"*": "pi_pnl_yellow", "south": F}
    b = [(x0, y0, 1.4, x1, y1, CP_D, face)]
    # The holder: a back plate on the wall, the cradle's two arms either side of the pendant; the strain relief on the
    # pendant's top and its cable up into the junction box, the conduit on up the wall.
    b += [(3.0, 18.6, 0.0, 13.0, 23.4, 0.4, "pi_steel_dark"), (x0 - 0.5, 19.4, 0.4, x0, 22.6, CP_D - 0.6, "pi_steel_dark"),
          (x1, 19.4, 0.4, x1 + 0.5, 22.6, CP_D - 0.6, "pi_steel_dark")]
    b += [(7.1, y1, 2.2, 8.9, y1 + 1.2, 3.8, BLK), (7.45, y1 + 1.2, 2.55, 8.55, 27.2, 3.45, BLK)]
    b += [(6.8, 27.2, 0.0, 9.6, 29.6, 3.8, "pi_steel"), (7.2, 26.9, 2.5, 8.8, 27.2, 3.5, "pi_steel_dark")]
    b.append((0.9, 27.0, 0.0, 6.3, 29.2, 0.12, {"*": "pi_chrome", "south": F}))
    conduit_up(b, 8.2, 29.6, 0.4, 1.6)
    b += [(6.9, 27.3, 3.8, 7.3, 27.7, 3.95, "pi_steel_dark"), (9.1, 27.3, 3.8, 9.5, 27.7, 3.95, "pi_steel_dark"),
          (6.9, 29.1, 3.8, 7.3, 29.5, 3.95, "pi_steel_dark"), (9.1, 29.1, 3.8, 9.5, 29.5, 3.95, "pi_steel_dark")]
    # STOP: the mushroom on its yellow guard collar; START: a green flush button in a black collar; the POWER lamp;
    # the motion buttons (black heads, their arrows or letters on them) and the HORN.
    sx, sy = CP_STOP
    b += _sc_round(sx, sy, CP_D, CP_D + 0.35, 1.25, "pi_pnl_yellow") + _sc_round(sx, sy, CP_D + 0.35, CP_D + 1.3, 0.55, BLK)
    b += _sc_round(sx, sy, CP_D + 1.3, CP_D + 1.95, 1.35, "pi_pnl_red")
    head = {"*": BLK, "south": F}
    for key, bx, by, _ in CP_BUTTONS:
        if key == "stop":
            continue
        if key == "start":
            b += _sc_round(bx, by, CP_D, CP_D + 0.4, 1.05, BLK) + _sc_round(bx, by, CP_D + 0.4, CP_D + 0.55, 0.8, {"*": "pi_pnl_green", "south": F})
            continue
        b += _sc_round(bx, by, CP_D, CP_D + 0.25, 1.05, BLK) + _sc_round(bx, by, CP_D + 0.25, CP_D + 0.6, 0.8, head)
    px_, py_ = CP_POWER
    b += _sc_round(px_, py_, CP_D, CP_D + 0.3, 0.75, "pi_chrome") + _sc_round(px_, py_, CP_D + 0.3, CP_D + 0.5, 0.5, "pi_lens_white")
    return b


# ------------------------------------------------------------------------------------------------ the oil spill kit

# The lube oil room's spill kit (turbine hall part 3): the battery rooms' yellow steel cabinet on the wall, lettered for
# oil (absorbent pads, socks and pillows that take up oil and not water, the disposal bags).
def _oil_spill_front():
    from PIL import Image, ImageDraw
    W, H = 473, 512
    img = Image.new("RGBA", (512, 512), (246, 200, 26, 255))
    d = ImageDraw.Draw(img)
    BLACK_ = (20, 20, 22, 255)
    d.rectangle([12, 12, W - 13, H - 13], outline=(170, 136, 14, 255), width=6)
    d.rectangle([40, 52, W - 41, 172], fill=BLACK_)
    PP._fit_line(d, (W // 2, 112), "SPILL KIT", 84, W - 110, (246, 200, 26, 255))
    PP._fit_line(d, (W // 2, 226), "OIL ONLY", 56, W - 130, BLACK_)
    for k, line in enumerate(("ABSORBENT PADS", "SOCKS  PILLOWS", "DISPOSAL BAGS  GLOVES")):
        PP._fit_line(d, (W // 2, 306 + k * 52), line, 40, W - 70, BLACK_)
    PP._fit_line(d, (W // 2, 466), "BREAK SEAL TO OPEN", 30, W - 70, BLACK_)
    return img


def oil_spill_kit_boxes():
    out = []
    for b in PP.spill_kit_boxes():
        tex = b[6]
        if isinstance(tex, dict) and isinstance(tex.get("south"), tuple) and tex["south"][0] == "pi_spill_front":
            tex = dict(tex, south=("pi_spill_front_oil", tex["south"][1]))
        out.append(b[:6] + (tex,) + tuple(b[7:]))
    return out



# ------------------------------------------------------------------------------------------------ the hall's fittings

# Turbine hall part 3, the fittings: the crane's travel directions on the walls (the pendant's NORTH, SOUTH, WEST and
# EAST, read from the floor and from the cab's height), the fire hose reels and the plant page stations (with their
# horns) along the walls on both floors, the laydown bay's floor markings, the hearing protection signs at the ways in.
import math
from PIL import Image, ImageDraw
from plant_panels import round_y
from spreading_room import oct_x, oct_y, oct_z
from admin_rooms import ring_z, round_z

COMPASS = "tb_crane_compass"
COMPASS_LABELS = ["N", "E", "S", "W"]
COMPASS_WORDS = {"N": "NORTH", "E": "EAST", "S": "SOUTH", "W": "WEST"}
CS_PLATE = (0.8, 0.8, 31.2, 31.2)              # the plate (two blocks by two), its face 0.6 off the wall


def crane_compass(label=COMPASS_LABELS[0]):
    """A crane travel direction sign: a yellow steel plate on the wall, its black border, the compass letter big with
    the arrow over it pointing up the wall (the way you face, looking at it), the word, CRANE TRAVEL across its top."""
    f = Front(2, 651 + COMPASS_LABELS.index(label))
    x0, y0, x1, y1 = CS_PLATE
    f.R(0, 0, 32, 32, fill=(120, 124, 128, 255))
    f.R(x0, y0, x1, y1, fill=YELLOW)
    f.R(x0 + 0.9, y0 + 0.9, x1 - 0.9, y1 - 0.9, outline=BLACK, width=int(0.7 * PP.U))
    f.R(x0 + 0.9, y1 - 4.4, x1 - 0.9, y1 - 0.9, fill=BLACK)
    f.T(16.0, y1 - 2.65, "CRANE TRAVEL", 30, fill=YELLOW, bold=True)
    # The arrow: its shaft and head; the letter under it, the word at the foot.
    f.R(14.7, 18.6, 17.3, 22.6, fill=BLACK)
    f.d.polygon([f.px(11.2, 22.4), f.px(20.8, 22.4), f.px(16.0, 26.0)], fill=BLACK)
    big = PP.rfont(int(10.5 * PP.U), True)
    f.d.text(f.px(16.0, 12.4), label, font=big, fill=BLACK, anchor="mm")
    f.T(16.0, 4.6, COMPASS_WORDS[label], 30, fill=BLACK, bold=True)
    for x, y in ((x0 + 0.5, y0 + 0.5), (x1 - 0.5, y0 + 0.5), (x0 + 0.5, y1 - 0.5), (x1 - 0.5, y1 - 0.5)):
        f.E(x, y, 0.32, fill=(90, 90, 92, 255))
    return f


def crane_compass_boxes():
    F = f"pi_{COMPASS}_f"
    x0, y0, x1, y1 = CS_PLATE
    b = [(x0, y0, 0.0, x1, y1, 0.6, {"*": "pi_pnl_yellow", "south": F})]
    for x, y in ((x0 + 0.5, y0 + 0.5), (x1 - 0.5, y0 + 0.5), (x0 + 0.5, y1 - 0.5), (x1 - 0.5, y1 - 0.5)):
        b += _sc_round(x, y, 0.6, 0.8, 0.42, "pi_steel_dark")
    return b


# The fire hose reel on its wall plate: the supply up the wall from the floor (red), its isolating valve (the handwheel
# toward you), the swinging reel on its hub (red flanges, the hose wound on its drum), the hose's end through the guide
# to the nozzle in its clip, the FIRE HOSE REEL sign over it.
HR_HUB = (8.0, 19.0)
HR_R = 6.2


def hose_reel():
    hx, hy = HR_HUB
    out = [(4.0, 13.0, 0.0, 12.0, 25.0, 0.5, "pi_pnl_red")]
    out += oct_z(hx, hy, 0.5, 3.0, 0.9, "pi_steel_dark")
    # The supply: up the wall at the left from the floor, into the hub's back.
    out += oct_y(2.4, 1.6, 0.0, hy, 0.75, "pi_pnl_red") + oct_x(1.6, hy, 2.4, hx - 0.9, 0.6, "pi_pnl_red")
    out += [(1.4, 1.2, 0.6, 3.4, 1.6, 2.6, "pi_steel_dark")]
    # Its gate valve at chest height: the body, the bonnet out toward you, the handwheel and its stem.
    vy = 10.0
    out += [(1.3, vy - 0.9, 0.6, 3.5, vy + 0.9, 2.6, "pi_pnl_red"), (1.9, vy - 0.5, 2.6, 2.9, vy + 0.5, 4.2, "pi_pnl_red")]
    out += oct_z(2.4, vy, 4.2, 5.0, 0.18, "pi_chrome") + ring_z(2.4, vy, 1.0, 1.35, 4.7, 5.1, "pi_pnl_red")
    out += [(2.25, vy - 0.15, 4.75, 2.55, vy + 1.0, 5.05, "pi_pnl_red")]
    # The reel: back flange, the drum with the hose wound on it, front flange; the spokes on the front.
    out += round_z(hx, hy, HR_R, 3.0, 3.7, "pi_pnl_red") + round_z(hx, hy, HR_R, 8.8, 9.5, "pi_pnl_red")
    out += round_z(hx, hy, 2.0, 3.7, 8.8, "pi_steel_dark")
    for k, (a_in, a_out) in enumerate(((2.0, 3.1), (3.1, 4.2), (4.2, 5.3))):
        out += ring_z(hx, hy, a_in, a_out, 3.8 + 0.05 * k, 8.7 - 0.05 * k, "pi_tb_hose")
    out += round_z(hx, hy, 1.0, 9.5, 10.3, "pi_steel_dark")
    out += [(hx - 0.3, hy - HR_R + 0.6, 9.5, hx + 0.3, hy + HR_R - 0.6, 9.8, "pi_pnl_red"),
            (hx - HR_R + 0.6, hy - 0.3, 9.52, hx + HR_R - 0.6, hy + 0.3, 9.78, "pi_pnl_red")]
    # The hose off the bottom of the coil, through the guide to the nozzle in its clip on the plate's right.
    out += [(hx + 2.0, hy - 5.6, 5.4, hx + 3.0, hy - 4.6, 7.2, "pi_tb_hose"), (hx + 3.0, 11.4, 5.6, hx + 4.0, hy - 4.6, 7.0, "pi_tb_hose")]
    out += [(hx + 2.6, 11.0, 4.6, hx + 4.4, 13.0, 5.6, "pi_chrome")]
    out += oct_y(hx + 3.5, 6.3, 6.4, 11.4, 0.55, "pi_tb_hose") + oct_y(hx + 3.5, 6.3, 3.6, 6.4, 0.65, "pi_chrome")
    out += oct_y(hx + 3.5, 6.3, 2.4, 3.6, 0.45, "pi_pnl_red")
    out += [(hx + 2.7, 7.6, 0.5, hx + 4.3, 8.4, 6.6, "pi_steel_dark")]
    # The sign over it.
    out.append((3.0, 27.6, 0.0, 13.0, 31.4, 0.4, {"*": "pi_pnl_red", "south": ("pi_tb_hose_sign", [0, 0, 16, 6.08])}))
    return out


def hose_sign():
    img = Image.new("RGBA", (256, 256), (196, 36, 30, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 251, 93], outline=(250, 250, 246, 255), width=4)
    PP._fit_line(d, (128, 34), "FIRE HOSE REEL", 40, 230, (250, 250, 246, 255))
    PP._fit_line(d, (128, 70), "OPEN VALVE  -  RUN OUT HOSE", 18, 230, (250, 250, 246, 255))
    return img


def hose_tex():
    """The hose: dark red rubber, its turns' ribs."""
    img = Image.new("RGBA", (16, 16), (112, 22, 20, 255))
    d = ImageDraw.Draw(img)
    for y in range(0, 16, 3):
        d.line([(0, y), (15, y)], fill=(70, 14, 12, 255))
    return img


# The plant page station (the party-line phone): a yellow cast enclosure on the wall, the handset in its cradle on the
# front (its PAGE switch), the coiled cord, the line selector and volume knobs, its conduit up the wall to the horn over it.
def page_station():
    out = [(4.0, 12.0, 0.0, 12.0, 25.0, 4.2, {"*": "pi_pnl_yellow", "south": ("pi_tb_page_face", [0, 0, 16, 16])})]
    out += [(4.4, 25.0, 0.4, 11.6, 25.4, 3.8, "pi_pnl_yellow")]
    # The handset: its cradle, the handle, the ear and mouth pieces, the PAGE bar on its grip; the coiled cord.
    out += [(4.6, 13.4, 4.2, 7.4, 14.4, 5.0, BLK), (4.6, 22.2, 4.2, 7.4, 23.2, 5.0, BLK)]
    out += [(5.2, 14.0, 5.0, 6.8, 23.0, 6.0, BLK), (4.9, 21.2, 5.4, 7.1, 23.6, 6.6, BLK), (4.9, 13.4, 5.4, 7.1, 15.8, 6.6, BLK)]
    out += [(5.6, 17.6, 6.0, 6.4, 19.0, 6.3, "pi_pnl_red")]
    for k in range(6):
        y = 8.2 + k * 0.8
        out.append((5.3, y, 4.6, 6.7, y + 0.5, 5.4, BLK))
    out += [(5.7, 7.8, 4.6, 6.3, 8.2, 5.2, BLK), (5.7, 12.9, 3.8, 6.3, 13.4, 4.6, BLK)]
    # The knobs: the line selector and the volume.
    out += round_z(9.8, 20.6, 0.85, 4.2, 5.0, BLK) + [(9.65, 20.7, 5.0, 9.95, 21.4, 5.1, "pi_pnl_white")]
    out += round_z(9.8, 16.4, 0.7, 4.2, 4.9, BLK)
    # The conduit up the wall from its top.
    out += oct_y(8.0, 1.2, 25.4, 32.0, 0.55, "pi_steel")
    out += [(7.0, 28.0, 0.0, 9.0, 28.6, 1.9, "pi_steel_dark")]
    return out


def page_face():
    """The station's face (8 by 13 px of the enclosure's front): its tag, the line selector's numbers, VOL, PAGE."""
    img = Image.new("RGBA", (256, 256), (236, 196, 30, 255))
    d = ImageDraw.Draw(img)
    py = lambda y: int((25.0 - y) * 256 / 13)   # model y to the drawing's rows (13 px tall over 256)
    # The tag across its top.
    d.rectangle([20, py(24.6), 236, py(23.6)], fill=(20, 20, 22, 255))
    PP._fit_line(d, (128, (py(24.6) + py(23.6)) // 2), "PLANT PAGE", 16, 200, (236, 196, 30, 255))
    cx, cy = int((9.8 - 4.0) * 32), py(20.6)
    for n in range(5):
        a = math.radians(150 - n * 30)
        d.text((cx + math.cos(a) * 42, cy - math.sin(a) * 42), str(n + 1), font=PP.rfont(14, True), fill=(20, 20, 22, 255), anchor="mm")
    d.text((cx, py(19.0)), "LINE", font=PP.rfont(12, True), fill=(20, 20, 22, 255), anchor="mm")
    d.text((cx, py(15.0)), "VOL", font=PP.rfont(12, True), fill=(20, 20, 22, 255), anchor="mm")
    return img


def page_horn():
    """The page horn high on the wall: its bracket, the arm out, the driver and its re-entrant bell facing the floor's
    way out from the wall, the conduit up from it."""
    out = [(6.0, 5.0, 0.0, 10.0, 11.0, 0.5, "pi_steel_dark"), (7.4, 7.4, 0.5, 8.6, 8.6, 2.2, "pi_steel_dark")]
    out += round_z(8.0, 8.0, 1.5, 2.2, 5.2, "pi_steel") + round_z(8.0, 8.0, 2.4, 5.2, 6.4, "pi_steel")
    out += round_z(8.0, 8.0, 3.4, 6.4, 7.6, "pi_steel") + round_z(8.0, 8.0, 1.2, 7.6, 8.4, "pi_steel_dark")
    out += oct_y(8.0, 0.8, 0.0, 5.0, 0.45, "pi_steel") + oct_y(8.0, 0.8, 11.0, 16.0, 0.45, "pi_steel")
    return out


# The laydown bay's floor paint (a thin coat on the floor, walked over): the yellow edge line (along the block's north
# side: turned to the edge it marks), the corner, the keep-clear hatching, and LAYDOWN AREA in big stencil letters
# across six blocks.
FM_LINE = 2.0                                   # the line's width (px)
FM_Y = 0.08                                     # the paint's thickness
FM_TEXT = ("LAYDOWN", "AREA")
FM_COLS, FM_ROWS = 6, 2
FM_TILES = FM_COLS * FM_ROWS                    # tile r * FM_COLS + c: row r from the top, column c from the left


def floor_marks():
    Y = "pi_tb_floor_yellow"
    line = [(0.0, 0.0, 0.6, 16.0, FM_Y, 0.6 + FM_LINE, Y)]
    corner = [(0.6, 0.0, 0.6, 16.0, FM_Y, 0.6 + FM_LINE, Y), (0.6, 0.0, 0.6 + FM_LINE, 0.6 + FM_LINE, FM_Y, 16.0, Y)]
    hatch = [(0.0, 0.0, 0.0, 16.0, FM_Y, 16.0, {"*": "pi_tb_floor_yellow", "up": "pi_tb_hatch"})]
    text = [[(0.0, 0.0, 0.0, 16.0, FM_Y, 16.0, {"*": None, "up": f"pi_tb_stencil_{k}"})] for k in range(FM_TILES)]
    return [line, corner, hatch] + text


def hatch_tex():
    img = Image.new("RGBA", (64, 64), (236, 190, 30, 255))
    d = ImageDraw.Draw(img)
    for k in range(-64, 128, 16):
        d.polygon([(k, 0), (k + 8, 0), (k + 8 - 64, 64), (k - 64, 64)], fill=(24, 24, 26, 255))
    return img


def stencil_tiles():
    """LAYDOWN over AREA in yellow stencil letters on nothing (the floor shows through), cut into tiles."""
    W, H = FM_COLS * 128, FM_ROWS * 128
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for r, line in enumerate(FM_TEXT):
        PP._fit_line(d, (W // 2, 128 * r + 66), line, 118, W - 60, (236, 190, 30, 255))
    return {f"pi_tb_stencil_{r * FM_COLS + c}": (lambda r=r, c=c: img.crop((c * 128, r * 128, (c + 1) * 128, (r + 1) * 128)))
            for r in range(FM_ROWS) for c in range(FM_COLS)}


HALL_PIECES = {
    "tb_hose_reel": {"display": "Fire Hose Reel", "boxes": hose_reel()},
    "tb_page_station": {"display": "Plant Page Station", "boxes": page_station()},
    "tb_page_horn": {"display": "Plant Page Horn", "boxes": page_horn()},
    "tb_floor_mark": {"display": "Floor Marking", "boxes": [], "variants": 3 + FM_TILES, "vboxes": floor_marks()},
}


def hall_makers():
    out = {"pi_tb_hose": hose_tex, "pi_tb_hose_sign": hose_sign, "pi_tb_page_face": page_face, "pi_tb_hatch": hatch_tex,
           "pi_tb_floor_yellow": lambda: PP._solid((236, 190, 30), 671, 0.06)}
    out.update(stencil_tiles())
    return out


# ------------------------------------------------------------------------------------------------ registering

PANELS = {PENDANT: {"display": "Crane Pendant Station", "width": 1, "item3d": True},
          COMPASS: {"display": "Crane Travel Direction Sign", "width": 2, "item3d": True, "labels": COMPASS_LABELS}}
BUILDERS = {PENDANT: (crane_pendant, crane_pendant_boxes), COMPASS: (crane_compass, crane_compass_boxes)}
CRAFTING = {
    "spill_kit_oil": (["YPY", "YWY", "YYY"], {"Y": "minecraft:yellow_dye", "P": "#c:plates/steel", "W": "minecraft:white_wool"}, 1),
    PENDANT: (["PYP", "BBB", "PCP"], {"P": "#c:plates/steel", "Y": "minecraft:yellow_dye", "B": "minecraft:stone_button", "C": "minecraft:redstone"}, 1),
    COMPASS: (["YPY", "PBP", "YPY"], {"Y": "minecraft:yellow_dye", "P": "#c:plates/steel", "B": "minecraft:black_dye"}, 1),
    "tb_hose_reel": (["RPR", "HHH", "RVR"], {"R": "minecraft:red_dye", "P": "#c:plates/steel", "H": "minecraft:dried_kelp", "V": "minecraft:copper_ingot"}, 1),
    "tb_page_station": (["YPY", "YRY", "YCY"], {"Y": "minecraft:yellow_dye", "P": "#c:plates/steel", "R": "minecraft:redstone", "C": "minecraft:copper_ingot"}, 1),
    "tb_page_horn": ([" P ", "PNP"], {"P": "#c:plates/steel", "N": "minecraft:note_block"}, 1),
    "tb_floor_mark": (["YYY"], {"Y": "minecraft:yellow_dye"}, 16),
}
_BUILT = {}


def built(key, label=None):
    labels = PANELS[key].get("labels")
    label = label or (labels[0] if labels else None)
    if (key, label) not in _BUILT:
        draw, boxes = BUILDERS[key]
        _BUILT[(key, label)] = (draw(label) if labels else draw(), boxes())
    return _BUILT[(key, label)]


def register():
    for key, info in PANELS.items():
        info["multi"] = (info["width"], 2, "panel")
        info["panel"] = key
        PP.PANELS[key] = info
        PP.DRAWINGS[key] = lambda label=None, key=key: built(key, label)[0]
        PP.CABINETS[key] = lambda key=key: built(key)[1]
    DP.BUILDERS.update({k: (lambda k=k: built(k)) for k in BUILDERS})
    PP.CRAFTING.update(CRAFTING)
    for key, info in PANELS.items():
        for v in range(len(info.get("labels", [None]))):
            PP.SHARP |= {f"pi_{PP.texture_key(key, v)}_f_{p}" for p in range(2 * info["width"])}
    PP.RISERS.update(HALL_PIECES)
    PP.EXTRA_MAKERS.append(hall_makers)
    PP.SHARP |= {"pi_tb_hose_sign", "pi_tb_page_face"} | {f"pi_tb_stencil_{k}" for k in range(FM_TILES)}
    PP.SEE_THROUGH |= {f"pi_tb_stencil_{k}" for k in range(FM_TILES)}
    PP.RISERS["spill_kit_oil"] = {"display": "Oil Spill Kit", "boxes": oil_spill_kit_boxes()}
    PP.EXTRA_MAKERS.append(lambda: {"pi_spill_front_oil": _oil_spill_front})
    PP.SHARP |= {"pi_spill_front_oil"}


register()
