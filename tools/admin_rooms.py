"""The administration building brought up to the detail standard, part 2 (Justin: "Admin building part 2"): its four
rooms with a job of their own, as a real plant's have them.

- The water chemistry laboratory: the plant chemistry status board on the wall (chem_status_board, live: the reactor
  coolant's boron, lithium, hydrogen, chloride and dose equivalent iodine, the secondary side's cation conductivity,
  sodium, oxygen, hydrazine and pH, each against its limit, worked out from the reactor: its power, its fuel's burnup,
  a trip's iodine spike, damaged fuel), the gamma spectroscopy station (chem_gamma_spec, live: the germanium detector
  in its lead cave over its liquid nitrogen dewar, the NIM bin, the spectrum building on its screen as it counts a
  sample and the nuclides it finds; a click counts a new sample), the emergency shower and eyewash (lab_safety_shower:
  pull it and it runs), the flammables and corrosives safety cabinets (chem_flam_cabinet, chem_acid_cabinet: their
  doors open on a click), the lab's door sign with its NFPA 704 diamond.
- Document control: high-density mobile shelving (doc_mobile_shelving: carriages on floor rails, each with its
  handwheel and its contents' label; turn a handwheel and the carriages roll to open the aisle there, never shut on
  anyone), fixed end shelving, the plan file for the drawings (doc_plan_file: its drawers pull out), the issue counter
  where controlled copies are handed out (doc_issue_counter, its computer's screen live), the room's sign.
- The board room: the boardroom table (board_table: walnut, cable cubbies, a delegate microphone at every seat, the
  conference phone), the AV credenza under the plant overview with the video conference camera (board_av_credenza),
  the unit performance screen (perf_screen, live: today's and the month's generation, the capacity factor, days on
  line and since the last trip), the lectern, the master clock.
- The plant manager's office: the performance screen, the direct line to the control room (mgr_hotline: lift it and
  the shift manager tells you how the unit is), the framed operating license, the awards wall, his hard hat and vest.

Models in model pixels for the piece facing north: x across, y up, z from its back (z 0, the wall) out to its front,
where its user is (+z)."""
import math

from PIL import Image, ImageDraw

import plant_panels as PP
import relay_room as RR
import switchgear_room as SGR
import tsc_room as TR

U = PP.U
INK = (24, 24, 26, 255)
WHITE = (246, 246, 240, 255)
BLACK = "pi_cap_black"
NAVY, PANEL_BG = TR.NAVY, TR.PANEL_BG
CYAN, WHITE_T, GREY_T = TR.CYAN, TR.WHITE_T, TR.GREY_T
GREEN, AMBER, RED = (90, 230, 120), (255, 190, 60), (255, 70, 50)
BENCH_Y = 14.5       # a lab bench's top (tools/plant_office.py LAB_BODY)


def _rot_z(cx, cy, z, angle):
    return {"rotation": {"angle": angle, "axis": "z", "origin": [cx, cy, z]}}


def ring_z(cx, cy, a_in, a_out, z0, z1, tex):
    """An octagonal ring standing up, facing +z (a handwheel's rim): four straight bars and the same four turned 45
    degrees about z, each 0.1 thinner at both ends so no two faces share a plane."""
    h = a_out * math.tan(math.radians(22.5)) + 0.05
    bars = [(cx + a_in, cy - h, z0, cx + a_out, cy + h, z1), (cx - a_out, cy - h, z0, cx - a_in, cy + h, z1),
            (cx - h, cy + a_in, z0, cx + h, cy + a_out, z1), (cx - h, cy - a_out, z0, cx + h, cy - a_in, z1)]
    out = [b + (tex,) for b in bars]
    out += [(b[0], b[1], z0 + 0.1, b[3], b[4], z1 - 0.1, tex, _rot_z(cx, cy, z0, 45)) for b in bars]
    return out


def round_y_front(cx, cz, a, y0, y1, tex, front):
    """plant_panels.round_y with the drawing (front) on its front face only (the face square to +z), not its
    diagonals'."""
    out = PP.round_y(cx, cz, a, y0, y1, tex)
    out[1] = out[1][:6] + ({"*": tex, "south": front},) + tuple(out[1][7:])
    return out


def round_z(cx, cy, a, z0, z1, tex):
    """A round part facing +z (a knob, a lens, a hub): an octagon of two crossing boxes and the pair turned 45 degrees."""
    assert z1 - z0 >= 0.7 - 1e-9, "round_z needs 0.7 px depth for its four steps"
    s = a * math.tan(math.radians(22.5))
    rot = _rot_z(cx, cy, z0, 45)
    return [(cx - a, cy - s, z0, cx + a, cy + s, z1, tex), (cx - s, cy - a, z0 + 0.1, cx + s, cy + a, z1 - 0.1, tex),
            (cx - a, cy - s, z0 + 0.2, cx + a, cy + s, z1 - 0.2, tex, rot), (cx - s, cy - a, z0 + 0.3, cx + s, cy + a, z1 - 0.3, tex, rot)]


# ================================================================================================ the chemistry lab
# ------------------------------------------------------------------------------------------------ the status board
CSB_KEY = "chem_status_board"
CSB_SCREEN = (1.0, 2.6, 47.0, 29.6)
CSB_Z = 2.4
# Its rows (nuclear/ChemLab.java PARAMS in this order): what, units, its limit as the chemistry manual writes it.
CSB_RCS = [("BORON", "ppm", "PER COLR"), ("LITHIUM", "ppm", "0.50 - 3.50"), ("DISSOLVED HYDROGEN", "cc/kg", "25 - 50"),
           ("CHLORIDE", "ppb", "< 150"), ("DOSE EQUIVALENT I-131", "µCi/g", "< 1.0"), ("GROSS ACTIVITY", "µCi/g", "< 100/E")]
CSB_SEC = [("SG CATION CONDUCTIVITY", "µS/cm", "< 0.80"), ("SG SODIUM", "ppb", "< 5.0"), ("FW DISSOLVED OXYGEN", "ppb", "< 5.0"),
           ("FW HYDRAZINE", "ppb", "> 50"), ("CONDENSATE pH", "", "9.6 - 10.0")]
CSB_STEP = 1.55
# The columns: the parameter's x, its result's (right), units, the limit (centred), the status (centred), sampled (right).
CSB_X = {"name": 2.0, "value": 25.0, "units": 25.8, "limit": 33.0, "status": 39.6, "time": 46.4}


def _csb_rows():
    """Each row's y (the RCS rows under their heading, then the secondary's under theirs), and the two headings' y."""
    h1 = CSB_SCREEN[3] - 6.6
    ys = [h1 - 1.1 - i * CSB_STEP for i in range(len(CSB_RCS))]
    h2 = ys[-1] - 1.25
    ys += [h2 - 1.1 - i * CSB_STEP for i in range(len(CSB_SEC))]
    return ys, h1, h2


def chem_status_board(label=None):
    """The plant chemistry status board: a wall LCD. Its title bar (the unit, the clock live), the lab's line (the plant's
    mode live), the column headings, the reactor coolant's six results and the secondary side's five (each its value,
    its status in its colour and when it was sampled, live; its name, units and limit printed), the footer's counts."""
    f = PP.Front(3, 1101)
    x0, y0, x1, y1 = CSB_SCREEN
    f.R(0, 0, 48, 32, fill=(30, 30, 32, 255))
    f.R(x0, y0, x1, y1, fill=NAVY)
    f.R(x0, y1 - 2.2, x1, y1, fill=(16, 110, 104, 255))
    f.T(x0 + 0.6, y1 - 1.1, "UNIT 1", 11, fill=WHITE_T, bold=True, anchor="lm")
    RR._fit(f, 22.0, y1 - 1.1, "PLANT CHEMISTRY STATUS", 15, 26.0, fill=WHITE_T)
    f.text(x1 - 0.6, y1 - 1.1, CSB_Z + 0.02, 1.0, (238, 240, 244), "csb_clock", "right")
    f.R(x0, y1 - 3.8, x1, y1 - 2.2, fill=(20, 30, 52, 255))
    f.T(x0 + 0.6, y1 - 3.0, "CHEMISTRY LAB  -  RESULTS AGAINST LIMITS", 9, fill=GREY_T, bold=True, anchor="lm")
    f.T(34.0, y1 - 3.0, "PLANT MODE", 9, fill=GREY_T, bold=True, anchor="rm")
    f.text(34.6, y1 - 3.0, CSB_Z + 0.02, 0.78, (120, 255, 150), "csb_mode")
    hy = y1 - 5.0
    for name, key, anchor in (("PARAMETER", "name", "lm"), ("RESULT", "value", "rm"), ("UNITS", "units", "lm"), ("LIMIT", "limit", "mm"),
                              ("STATUS", "status", "mm"), ("SAMPLED", "time", "rm")):
        f.T(CSB_X[key], hy, name, 9, fill=CYAN, bold=True, anchor=anchor)
    f.line([(x0 + 0.5, hy - 0.9), (x1 - 0.5, hy - 0.9)], (60, 80, 120, 255), 2)
    ys, h1, h2 = _csb_rows()
    for hy_, title in ((h1, "REACTOR COOLANT SYSTEM"), (h2, "SECONDARY SYSTEM")):
        f.T(x0 + 0.8, hy_, title, 8, fill=(255, 210, 80, 255), bold=True, anchor="lm")
    for i, ((name, units, limit), y) in enumerate(zip(CSB_RCS + CSB_SEC, ys)):
        f.R(x0 + 0.5, y - CSB_STEP / 2 + 0.12, x1 - 0.5, y + CSB_STEP / 2 - 0.12, fill=PANEL_BG)
        f.T(CSB_X["name"], y, name, 8.5, fill=WHITE_T, bold=True, anchor="lm")
        f.T(CSB_X["units"], y, units, 8, fill=GREY_T, bold=True, anchor="lm")
        f.T(CSB_X["limit"], y, limit, 8, fill=GREY_T, bold=True, anchor="mm")
        f.text(CSB_X["value"], y, CSB_Z + 0.02, 0.72, (238, 240, 244), f"csb_v{i}", "right")
        f.text(CSB_X["status"], y, CSB_Z + 0.02, 0.64, (110, 230, 140), f"csb_s{i}", "center")
        f.text(CSB_X["time"], y, CSB_Z + 0.02, 0.64, (150, 160, 178), f"csb_t{i}", "right")
    f.R(x0, y0, x1, y0 + 2.0, fill=(16, 24, 44, 255))
    for k, (name, x) in enumerate((("IN SPECIFICATION", 2.0), ("ACTION LEVEL", 19.0), ("LAST GAMMA COUNT", 33.0))):
        f.T(x, y0 + 1.0, name, 9, fill=GREY_T, bold=True, anchor="lm")
        f.text(x + (9.8 if k == 0 else 7.6 if k == 1 else 9.6), y0 + 1.0, CSB_Z + 0.02, 0.85, (255, 210, 80), f"csb_count{k}")
    return f


def chem_status_board_boxes(open_=False):
    x0, y0, x1, y1 = CSB_SCREEN
    F = f"pi_{CSB_KEY}_f"
    return [(8.0, 8.0, 0.1, 40.0, 24.0, 0.9, "pi_steel_dark"), (14.0, 4.0, 0.1, 18.0, 28.0, 0.9, "pi_steel_dark"),
            (30.0, 4.0, 0.1, 34.0, 28.0, 0.9, "pi_steel_dark"),
            (0.3, 1.8, 0.9, 47.7, 30.4, CSB_Z - 0.1, {"*": "pi_tsc_case", "south": "pi_tsc_case"}),
            (x0, y0, CSB_Z - 0.1, x1, y1, CSB_Z, {"*": "pi_tsc_case", "south": F}),
            (23.0, 1.9, CSB_Z - 0.1, 25.0, 2.3, CSB_Z - 0.02, "pi_chrome")]


# ------------------------------------------------------------------------------------------------ gamma spectroscopy
GS_KEY = "chem_gamma_spec"
GS_CAVE = (8.0, 8.4, 6.2, 16.0, 27.6)       # the lead cave: centre x, z, apothem, y0, y1 (on its stand's top, all in the upper block)
GS_DEWAR = (8.0, 8.4, 3.6, 1.0, 11.0)       # the liquid nitrogen dewar under it
GS_STAND = 15.2                             # the stand's top plate (to 16)
GS_NIM = (17.4, 26.6, 14.5, 19.0, 10.6)     # the NIM bin: x0, x1, y0, y1, its front's z
GS_MON = [(27.0, 37.0), (37.6, 47.6)]       # the two monitors' x (the spectrum, the analysis report)
GS_MON_Y = (16.6, 23.4)
GS_SCREEN_Z = 6.6
GS_BARS = 64
GS_PLOT = (27.7, 36.3, 18.1, 21.3)          # the spectrum's plot on the left screen: x0, x1, y0, y1
GS_KEV = 2048.0                             # the plot's energy across (keV)
# The peaks it may find (nuclear/ChemLab.java NUCLIDES): name, energy (keV), drawn over the plot.
GS_PEAKS = [("Xe-133", 81), ("I-131", 364), ("F-18", 511), ("Cs-137", 662), ("Co-58", 811), ("Co-60", 1173), ("Co-60", 1332),
            ("Na-24", 1369), ("K-40", 1461)]
GS_LABELLED = [("I-131", 364), ("Cs-137", 662), ("Co-60", 1173), ("K-40", 1461)]
GS_NUCLIDES = ["I-131", "I-133", "Cs-137", "Co-58", "Co-60", "Xe-133"]
GS_REPORT_TOP = 21.1
GS_REPORT_STEP = 0.62


def gamma_spec_boxes(open_=False):
    """The gamma spectroscopy station (three blocks across, two high, against the wall): the lead cave on its steel stand
    over the dewar of liquid nitrogen (the detector's cryostat up out of it into the cave), the bench beside it with
    the NIM bin, the dual monitors, the keyboard; the sample in its Marinelli beaker waiting on the stand."""
    F = f"pi_{GS_KEY}_f"
    st = "pi_steel_dark"
    out = []
    # The stand: four square legs, a lower shelf for the dewar, the thick top plate.
    for x in (1.2, 13.8):
        for z in (2.0, 13.8):
            out.append((x, 0.0, z, x + 1.0, GS_STAND, z + 1.0, st))
    out += [(1.2, 1.0, 2.0, 14.8, 1.6, 14.8, st), (0.8, GS_STAND, 1.6, 15.2, 15.9, 15.2, {"*": st, "up": "pi_gs_plate"})]
    # The dewar: its round body (stainless, its label to the front), the neck, the cryostat up into the cave.
    cx, cz, a, y0, y1 = GS_DEWAR
    out += round_y_front(cx, cz, a, 1.6, y1, "pi_gs_dewar", F)
    out += PP.round_y(cx, cz, 1.6, y1, y1 + 0.8, "pi_gs_dewar")
    out += PP.round_y(cx, cz, 0.9, y1 + 0.8, GS_STAND, "pi_chrome")
    # Its fill port and vent at the back, frosted white.
    out += [(cx + 1.6, y1, cz - 2.4, cx + 2.4, y1 + 1.4, cz - 1.6, "pi_gs_frost")]
    # The lead cave: its painted steel shell (round, its label to the front), the hinged lid on top with its handle, the
    # hinge at the back; the HV and signal cables out of its base to the NIM bin.
    cx, cz, a, y0, y1 = GS_CAVE
    out += round_y_front(cx, cz, a, y0, y1, "pi_gs_cave", F)
    out += PP.round_y(cx, cz, a + 0.3, y1, y1 + 1.6, {"*": "pi_gs_cave_lid", "up": "pi_gs_lid_top"})
    out += [(cx - 2.0, y1 + 1.6, cz + a - 1.4, cx + 2.0, y1 + 2.2, cz + a - 0.8, "pi_chrome"),
            (cx - 2.4, y1 + 0.6, cz - a - 0.6, cx + 2.4, y1 + 1.4, cz - a + 0.2, "pi_steel_dark")]
    out += [(cx + 4.4, 16.0, cz - 0.3, 16.0, 16.4, cz + 0.3, "pi_gs_cable"), (cx + 4.4, 16.0, cz - 1.3, 16.0, 16.4, cz - 0.7, "pi_gs_cable")]
    # The sample in its Marinelli beaker on the stand's corner, its lid on.
    out += PP.round_y(13.4, 14.0, 1.0, 16.0, 18.2, "pi_gs_beaker")
    # The bench (two blocks): toe kick, the cabinet with its drawers and doors (the drawing), the epoxy top.
    out += [(16.0, 0.0, 2.0, 48.0, 1.0, 14.0, "pi_toe_kick"),
            (16.0, 1.0, 1.5, 47.8, 13.0, 14.5, {"*": "pi_lab_cabinet", "south": F}),
            (16.0, 13.0, 0.5, 47.9, BENCH_Y, 16.0, {"*": "pi_epoxy_black", "up": "pi_epoxy_black!"})]
    # The cables along the bench's back into the bin.
    out += [(16.0, BENCH_Y, 8.1, 17.4, BENCH_Y + 0.4, 8.7, "pi_gs_cable"), (16.0, BENCH_Y, 7.1, 17.4, BENCH_Y + 0.4, 7.7, "pi_gs_cable")]
    # The NIM bin: its case, the modules' front panels (the drawing), its feet.
    x0, x1, y0, y1, z1 = GS_NIM
    out += [(x0, y0 + 0.3, 3.0, x1, y1, z1 - 0.3, "pi_gs_nim"),
            (x0, y0 + 0.3, z1 - 0.3, x1, y1, z1, {"*": "pi_gs_nim", "south": F}),
            (x0 + 0.4, y0, 3.6, x0 + 1.4, y0 + 0.3, z1 - 0.6, BLACK), (x1 - 1.4, y0, 3.6, x1 - 0.4, y0 + 0.3, z1 - 0.6, BLACK)]
    # The NIM modules' knobs and connectors standing out of their panels.
    for k, mx in enumerate(_nim_modules()):
        kx = (mx[0] + mx[1]) / 2
        out += round_z(kx, y0 + 3.2, 0.32, z1, z1 + 0.7, "pi_cap_black")
        out += round_z(kx, y0 + 1.0, 0.24, z1, z1 + 0.8, "pi_chrome")
    # The monitors on their dual stand: the spectrum (left), the analysis report (right).
    my0, my1 = GS_MON_Y
    out += [(31.0, BENCH_Y, 3.0, 43.6, BENCH_Y + 0.25, 6.0, "pi_steel_dark"), (36.95, BENCH_Y + 0.25, 3.8, 37.65, 20.0, 4.4, "pi_chrome"),
            (29.0, 19.6, 3.8, 45.6, 20.2, 4.4, "pi_chrome")]
    for mx0, mx1 in GS_MON:
        out += [(mx0, my0, 4.4, mx1, my1, GS_SCREEN_Z - 0.1, {"*": "pi_tsc_case", "south": "pi_tsc_case"}),
                (mx0 + 0.25, my0 + 0.25, GS_SCREEN_Z - 0.1, mx1 - 0.25, my1 - 0.25, GS_SCREEN_Z, {"*": "pi_tsc_case", "south": F})]
    out += TR._keyboard(33.0, 10.6)
    return out


def _nim_modules():
    """The NIM bin's modules across it: HV supply, preamp power, spectroscopy amplifier, ADC, MCA, LN2 monitor."""
    x0, x1 = GS_NIM[0] + 0.4, GS_NIM[1] - 0.4
    w = (x1 - x0) / 6
    return [(x0 + k * w + 0.05, x0 + (k + 1) * w - 0.05) for k in range(6)]


def gamma_spec(label=None):
    """The station's drawing: the cave's and the dewar's labels, the bench's drawers and doors, the NIM modules' front
    panels (their lamps live), the spectrum screen (its plot's bars live, the energy axis and the peaks named) and the
    analysis report (its lines live)."""
    f = PP.Front(3, 1102)
    f.R(0, 0, 48, 32, fill=(150, 156, 162, 255))
    # The cave's front: the detector's label plate, the radiation sticker.
    cx, _, a, y0, y1 = GS_CAVE
    f.R(cx - 2.5, y0, cx + 2.5, y1, fill=(92, 104, 120, 255))
    f.R(cx - 2.2, y0 + 7.8, cx + 2.2, y0 + 11.0, fill=(236, 236, 230, 255), outline=(40, 40, 44, 255), width=2)
    f.T(cx, y0 + 10.3, "HPGe DET 1", 7, fill=INK, bold=True)
    f.T(cx, y0 + 9.4, "COAXIAL 40%", 6.5, fill=INK, bold=True)
    f.T(cx, y0 + 8.5, "S/N 46-P41188A", 6, fill=INK, bold=True)
    f.R(cx - 2.0, y0 + 2.8, cx + 2.0, y0 + 7.0, fill=(246, 206, 40, 255), outline=(30, 30, 32, 255), width=2)
    _trefoil(f, cx, y0 + 5.7, 1.0)
    f.T(cx, y0 + 4.2, "CAUTION", 6.5, fill=INK, bold=True)
    f.T(cx, y0 + 3.4, "RADIOACTIVE", 6, fill=INK, bold=True)
    # The dewar's label: LN2, its size, CRYOGENIC.
    cx, _, a, y0, y1 = GS_DEWAR
    f.R(cx - 1.5, 1.6, cx + 1.5, y1, fill=(200, 204, 208, 255))
    f.R(cx - 1.4, 5.0, cx + 1.4, 9.0, fill=(246, 246, 240, 255), outline=(20, 70, 160, 255), width=2)
    f.T(cx, 8.2, "LN2", 8, fill=(20, 70, 160, 255), bold=True)
    f.T(cx, 7.2, "30 L", 7, fill=INK, bold=True)
    f.T(cx, 6.1, "CRYOGENIC", 6, fill=INK, bold=True)
    # The bench: a drawer bank, doors.
    f.R(16.0, 1.0, 48.0, 13.0, fill=(214, 216, 212, 255))
    for x0, x1 in ((16.3, 26.9), (27.1, 37.5), (37.7, 47.6)):
        if x0 < 20:
            for k, (dy0, dy1) in enumerate(((9.4, 12.7), (5.4, 9.2), (1.3, 5.2))):
                f.R(x0, dy0, x1, dy1, fill=(226, 228, 224, 255), outline=(130, 132, 128, 255), width=2)
                f.R((x0 + x1) / 2 - 1.6, dy1 - 0.9, (x0 + x1) / 2 + 1.6, dy1 - 0.55, fill=(150, 154, 156, 255))
        else:
            f.R(x0, 1.3, x1, 12.7, fill=(226, 228, 224, 255), outline=(130, 132, 128, 255), width=2)
            f.R(x1 - 1.0, 9.0, x1 - 0.65, 11.6, fill=(150, 154, 156, 255))
    # The NIM bin's frame and its modules' panels: each its name at the top, a lamp (live where it shows something).
    x0, x1, y0, y1, _ = GS_NIM
    f.R(x0, y0 + 0.3, x1, y1, fill=(60, 64, 70, 255))
    names = [("HV", "gs_hv", RED), ("PRE", None, GREEN), ("AMP", None, GREEN), ("ADC", "gs_cnt", GREEN), ("MCA", "gs_cnt", AMBER),
             ("LN2", "gs_ln2", GREEN)]
    for (mx0, mx1), (name, ch, col) in zip(_nim_modules(), names):
        f.R(mx0, y0 + 0.45, mx1, y1 - 0.15, fill=(178, 182, 186, 255), outline=(90, 94, 100, 255), width=1)
        f.T((mx0 + mx1) / 2, y1 - 0.55, name, 6, fill=INK, bold=True)
        lx, ly = (mx0 + mx1) / 2, y0 + 2.2
        f.E(lx, ly, 0.28, fill=tuple(int(c * 0.35) for c in col) + (255,))
        if ch:
            f.live.append({"t": "lamp", "x": lx, "y": ly, "r": 0.28, "z": round(GS_NIM[4] + 0.03, 3), "colour": list(col), "ch": ch})
    # The spectrum screen: the plot's frame, the energy axis and its ticks, the peaks named, the bars (live).
    (mx0, mx1), _ = GS_MON
    my0, my1 = GS_MON_Y
    f.R(mx0 + 0.25, my0 + 0.25, mx1 - 0.25, my1 - 0.25, fill=(10, 14, 24, 255))
    f.R(mx0 + 0.25, my1 - 1.05, mx1 - 0.25, my1 - 0.25, fill=(30, 50, 90, 255))
    f.T(mx0 + 0.5, my1 - 0.65, "MCA 1", 6, fill=WHITE_T, bold=True, anchor="lm")
    f.text(mx1 - 0.5, my1 - 0.65, GS_SCREEN_Z + 0.01, 0.36, (120, 210, 255), "gs_lt", "right")
    px0, px1, py0, py1 = GS_PLOT
    f.R(px0, py0, px1, py1, fill=(16, 22, 36, 255))
    f.line([(px0, py0), (px1, py0)], (120, 130, 150, 255), 1)
    f.line([(px0, py0), (px0, py1)], (120, 130, 150, 255), 1)
    for kev in (0, 500, 1000, 1500, 2000):
        x = px0 + (px1 - px0) * kev / GS_KEV
        f.line([(x, py0 - 0.2), (x, py0)], (120, 130, 150, 255), 1)
        if kev in (0, 1000, 2000):
            f.T(x, py0 - 0.62, str(kev), 6, fill=GREY_T, bold=True)
    f.T(px1, py0 - 1.25, "keV", 6, fill=GREY_T, bold=True, anchor="rm")
    for name, kev in GS_LABELLED:
        x = px0 + (px1 - px0) * kev / GS_KEV
        f.line([(x, py1 - 0.35), (x, py1 + 0.05)], (90, 100, 120, 255), 1)
        f.T(x, py1 + 0.42, name, 5.5, fill=(170, 180, 196, 255), bold=True)
    pitch = (px1 - px0) / GS_BARS
    for k in range(GS_BARS):
        bx0 = px0 + k * pitch + 0.01
        f.live.append({"t": "bar", "x0": round(bx0, 4), "y0": round(py0 + 0.02, 3), "x1": round(bx0 + pitch - 0.02, 4), "y1": round(py1 - 0.1, 3),
                       "z": round(GS_SCREEN_Z + 0.01, 3), "lo": 0.0, "hi": 1.0, "warn": 2.0, "colour": [110, 210, 255], "colour2": [110, 210, 255],
                       "ch": f"gs_b{k}"})
    # The analysis report: the sample, each nuclide's activity (live), the dose equivalent iodine against its limit.
    (mx0, mx1) = GS_MON[1]
    f.R(mx0 + 0.25, my0 + 0.25, mx1 - 0.25, my1 - 0.25, fill=(242, 244, 246, 255))
    f.R(mx0 + 0.25, my1 - 1.05, mx1 - 0.25, my1 - 0.25, fill=(30, 50, 90, 255))
    f.T(mx0 + 0.5, my1 - 0.65, "ANALYSIS REPORT", 6, fill=WHITE_T, bold=True, anchor="lm")
    f.text(mx1 - 0.5, my1 - 0.65, GS_SCREEN_Z + 0.01, 0.42, (238, 240, 244), "gs_sample", "right")
    f.T(mx0 + 0.6, GS_REPORT_TOP, "NUCLIDE", 5.5, fill=(60, 64, 72, 255), bold=True, anchor="lm")
    f.T(mx1 - 0.6, GS_REPORT_TOP, "µCi/g", 5.5, fill=(60, 64, 72, 255), bold=True, anchor="rm")
    for i, name in enumerate(GS_NUCLIDES):
        y = GS_REPORT_TOP - (i + 1) * GS_REPORT_STEP
        f.T(mx0 + 0.6, y, name, 6, fill=INK, bold=True, anchor="lm")
        f.text(mx1 - 0.6, y, GS_SCREEN_Z + 0.01, 0.4, (24, 26, 30), f"gs_v{i}", "right")
    y = GS_REPORT_TOP - (len(GS_NUCLIDES) + 1) * GS_REPORT_STEP - 0.1
    f.line([(mx0 + 0.5, y + 0.35), (mx1 - 0.5, y + 0.35)], (120, 124, 130, 255), 1)
    f.T(mx0 + 0.6, y, "DEI-131", 6, fill=INK, bold=True, anchor="lm")
    f.text(mx1 - 0.6, y, GS_SCREEN_Z + 0.01, 0.42, (20, 120, 60), "gs_dei", "right")
    return f


def _trefoil(f, cx, cy, r):
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


# ------------------------------------------------------------------------------------------------ the safety shower
def shower_part(c, r):
    """The combination emergency shower and eyewash (two blocks high, against the wall): its floor flange and the
    yellow-coated riser up the wall; at waist height the eyewash bowl on its arm with its two spray heads under
    their dust caps and the push flag; at the top the arm out to the shower head, the valve and its pull rod down to
    the triangle handle; the green EMERGENCY SHOWER plate on the riser."""
    Y = "pi_lab_shower"
    out = []
    rx, rz = 8.0, 3.0              # the riser
    if r == 0:
        out += PP.round_y(rx, rz, 2.0, 0.0, 0.7, "pi_chrome")
        out += PP.round_y(rx, rz, 0.9, 0.7, 16.0, Y)
        # The eyewash: the arm out to the bowl, the bowl (its rim, its floor), the heads, the push flag.
        out += [(rx - 0.5, 13.2, rz + 0.8, rx + 0.5, 14.0, 7.0, Y)]
        out += PP.round_y(rx, 10.0, 3.4, 13.1, 13.8, Y)
        out += PP.ring_y(rx, 10.0, 2.7, 3.4, 13.8, 15.0, Y)
        out += PP.round_y(rx - 1.1, 10.0, 0.55, 13.8, 14.8, "pi_chrome") + PP.round_y(rx + 1.1, 10.0, 0.55, 13.8, 14.8, "pi_chrome")
        out += PP.round_y(rx - 1.1, 10.0, 0.75, 14.8, 15.6, Y) + PP.round_y(rx + 1.1, 10.0, 0.75, 14.8, 15.6, Y)
        out += [(rx - 2.2, 12.6, 13.3, rx + 2.2, 15.4, 13.7, {"*": Y, "south": "pi_lab_push!"})]
    else:
        out += PP.round_y(rx, rz, 0.9, 0.0, 13.0, Y)
        # The arm out at the top, the shower head (a shallow bowl) under its end, the valve and its pull rod.
        out += [(rx - 0.6, 12.6, rz - 0.6, rx + 0.6, 14.0, 10.6, Y)]
        out += PP.round_y(rx, 10.0, 3.6, 11.2, 12.0, Y) + PP.round_y(rx, 10.0, 2.6, 12.0, 12.8, Y)
        out += PP.round_y(rx, 10.0, 0.8, 12.8, 13.6, Y)
        out += [(rx - 0.15, 3.2, 6.4, rx + 0.15, 12.6, 6.7, "pi_chrome"), (rx - 0.5, 11.8, 6.0, rx + 0.5, 12.6, 7.1, "pi_chrome"),
                # The triangle handle at the bottom of the rod.
                (rx - 1.6, 2.6, 6.4, rx + 1.6, 3.2, 6.7, Y),
                (rx - 1.6, 2.6, 6.4, rx - 1.0, 4.4, 6.7, Y, _rot_z(rx - 1.3, 2.9, 6.4, -22.5)),
                (rx + 1.0, 2.6, 6.4, rx + 1.6, 4.4, 6.7, Y, _rot_z(rx + 1.3, 2.9, 6.4, 22.5))]
        # The sign on the riser.
        out += [(rx - 3.0, 4.0, rz + 0.95, rx + 3.0, 10.0, rz + 1.15, {"*": "pi_pnl_white", "south": ("pi_lab_shower_sign", [0, 0, 16, 16])}),
                (rx - 0.9, 6.6, rz + 0.6, rx + 0.9, 7.4, rz + 0.95, "pi_chrome")]
    return out


# ------------------------------------------------------------------------------------------------ safety cabinets
CAB_KEYS = {"chem_flam_cabinet": ("Flammable Liquids Safety Cabinet", "flam"), "chem_acid_cabinet": ("Corrosives Safety Cabinet", "acid")}
CAB_FRONT = 9.0          # the cabinet's door face (z)
CAB_TOP = 26.4           # its top (1.65 m)
CAB_HINGES = (0.6, 15.4)


def cabinet_boxes(key, open_=False):
    """A safety cabinet for flammable liquids (yellow) or corrosives (blue), 1 block wide, 1.65 m tall: its double-walled
    body, the levelling feet, the leak-tight sill, the two doors (their legends the drawing; the three-point latch's
    paddle handle and lock in the middle), the vents either side with their flame arrester caps; open, the doors
    swung back square to the front and the shelves inside with their bottles and cans."""
    kind = CAB_KEYS[key][1]
    F = f"pi_{key}_f"
    body = f"pi_cab_{kind}"
    out = [(0.6, 0.0, 1.6, 1.6, 0.8, 2.6, "pi_steel_dark"), (14.4, 0.0, 1.6, 15.4, 0.8, 2.6, "pi_steel_dark"),
           (0.6, 0.0, 7.4, 1.6, 0.8, 8.4, "pi_steel_dark"), (14.4, 0.0, 7.4, 15.4, 0.8, 8.4, "pi_steel_dark"),
           (0.4, 0.8, 1.0, 15.6, CAB_TOP, 1.6, body),
           (0.4, 0.8, 1.6, 1.0, CAB_TOP, CAB_FRONT - 0.4, {"*": body, "west": f"pi_cab_{kind}_side!"}),
           (15.0, 0.8, 1.6, 15.6, CAB_TOP, CAB_FRONT - 0.4, {"*": body, "east": f"pi_cab_{kind}_side!"}),
           (1.0, 0.8, 1.6, 15.0, 2.6, CAB_FRONT - 0.4, body),
           (1.0, CAB_TOP - 0.6, 1.6, 15.0, CAB_TOP, CAB_FRONT - 0.4, body)]
    # The vents (2-inch bungs with flame arrester caps) high on both sides.
    for x0, x1 in ((-0.2, 0.4), (15.6, 16.2)):
        out += [(x0, 21.0, 4.0, x1, 23.0, 6.0, "pi_chrome")]
    if not open_:
        out += [(0.4, 0.8, CAB_FRONT - 0.4, 7.95, CAB_TOP, CAB_FRONT, {"*": body, "south": F}),
                (8.05, 0.8, CAB_FRONT - 0.4, 15.6, CAB_TOP, CAB_FRONT, {"*": body, "south": F}),
                # The paddle handle and its lock on the right-hand door.
                (8.4, 12.0, CAB_FRONT, 9.2, 15.4, CAB_FRONT + 0.5, "pi_chrome"),
                (8.5, 16.2, CAB_FRONT, 9.1, 16.8, CAB_FRONT + 0.3, "pi_cap_black")]
        for x in CAB_HINGES:
            out += [(x - 0.25, 4.0, CAB_FRONT - 0.2, x + 0.25, 6.0, CAB_FRONT + 0.15, "pi_chrome"),
                    (x - 0.25, 20.0, CAB_FRONT - 0.2, x + 0.25, 22.0, CAB_FRONT + 0.15, "pi_chrome")]
        return out
    # Open: the doors swung square to the front at the hinges, their insides to each other; the inside: its sill
    # (the leak-tight sump), two shelves and what is on them.
    out += [(0.4, 0.8, CAB_FRONT, 0.8, CAB_TOP, CAB_FRONT + 7.4, body), (15.2, 0.8, CAB_FRONT, 15.6, CAB_TOP, CAB_FRONT + 7.4, body),
            (0.8, 12.0, CAB_FRONT + 6.4, 1.3, 15.4, CAB_FRONT + 7.0, "pi_chrome"),
            (1.0, 2.6, 1.6, 15.0, 3.0, CAB_FRONT - 0.6, {"*": body, "up": "pi_cab_sump"}),
            (1.0, 9.9, 1.6, 15.0, 10.3, CAB_FRONT - 0.8, body), (1.0, 16.0, 1.6, 15.0, 16.4, CAB_FRONT - 0.8, body)]
    # (Nothing standing across the line between its two blocks: the middle row is the short bottles.)
    for y, short in ((3.0, False), (10.3, True), (16.4, False)):
        out += _cabinet_stock(kind, y, short)
    return out


def _cabinet_stock(kind, y, short=False):
    """What stands on a shelf: safety cans and solvent bottles (flammables), acid bottles in their tubs (corrosives); a
    short row has no can (flammables) and no tub (corrosives)."""
    out = []

    def bottle(x, z, a, y0, h, glass, label, cap):
        """A round bottle (its glass or plastic plain), its paper label a flat patch on its front, its cap."""
        b = PP.round_y(x, z, a, y0, y0 + h, glass) + PP.round_y(x, z, a * 0.4, y0 + h, y0 + h + 1.0, cap)
        b.append((x - a * 0.4, y0 + h * 0.25, z + a, x + a * 0.4, y0 + h * 0.75, z + a + 0.05, {"*": "pi_paper", "south": label + "!"}))
        return b
    if kind == "flam":
        if short:
            out += [(1.8, y, 2.6, 5.4, y + 3.6, 5.8, {"*": "pi_cab_jug_plain", "up": "pi_cab_jug_plain", "south": "pi_cab_jug!"})]
        else:
            out += [(1.8, y, 2.6, 5.4, y + 5.2, 6.2, {"*": "pi_cab_can_red_plain", "up": "pi_cab_can_top", "south": "pi_cab_can_red!"}),
                    (3.0, y + 5.2, 3.8, 4.2, y + 6.2, 5.0, "pi_chrome")]
        out += bottle(7.6, 4.6, 1.3, y, 4.6, "pi_cab_glass_brown", "pi_cab_label_acetone", "pi_cap_black")
        out += bottle(11.0, 4.6, 1.3, y, 4.6, "pi_cab_glass_clear", "pi_cab_label_methanol", "pi_cab_cap_red")
        out += [(12.8, y, 2.6, 14.6, y + 3.6, 5.8, {"*": "pi_cab_jug_plain", "up": "pi_cab_jug_plain", "south": "pi_cab_jug!"})]
    else:
        if not short:
            out += [(1.6, y, 2.4, 14.4, y + 1.2, 7.4, {"*": "pi_cab_tub", "up": "pi_cab_tub_in"})]
        rise = 0.0 if short else 1.2
        for x, glass, label, cap in ((4.0, "pi_cab_glass_brown", "pi_cab_label_hcl", "pi_cab_cap_red"),
                                     (8.0, "pi_cab_glass_brown", "pi_cab_label_hno3", "pi_cab_cap_blue"),
                                     (12.0, "pi_cab_hdpe", "pi_cab_label_naoh", "pi_cab_cap_white")):
            out += bottle(x, 4.8, 1.4, y + rise, 4.6, glass, label, cap)
    return out


def cabinet_drawing(key):
    """A safety cabinet's doors: yellow with FLAMMABLE - KEEP FIRE AWAY in red, or blue with CORROSIVE, the hazard
    pictogram, the capacity; the seam down the middle."""
    kind = CAB_KEYS[key][1]
    f = PP.Front(1, 1103 if kind == "flam" else 1104)
    if kind == "flam":
        bg, ink, word, sub = (236, 196, 32, 255), (196, 28, 24, 255), "FLAMMABLE", "KEEP FIRE AWAY"
    else:
        bg, ink, word, sub = (34, 84, 160, 255), (250, 250, 246, 255), "CORROSIVE", "ACIDS AND BASES"
    f.R(0, 0, 16, 32, fill=bg)
    f.line([(8.0, 0.8), (8.0, CAB_TOP)], (40, 40, 40, 255), 2)
    f.R(0.4, CAB_TOP - 0.1, 15.6, CAB_TOP + 0.4, fill=(40, 40, 42, 255))
    RR._fit(f, 8.0, 23.6, word, 13, 13.6, fill=ink)
    RR._fit(f, 8.0, 21.6, sub, 9, 13.6, fill=ink)
    # The hazard pictogram (a red-bordered diamond: the flame or the corrosion), the capacity under it.
    cx, cy, r = 8.0, 9.0, 3.2
    f.d.polygon([f.px(cx, cy + r), f.px(cx + r, cy), f.px(cx, cy - r), f.px(cx - r, cy)], fill=(250, 250, 246, 255), outline=(200, 30, 26, 255))
    f.d.polygon([f.px(cx, cy + r - 0.35), f.px(cx + r - 0.35, cy), f.px(cx, cy - r + 0.35), f.px(cx - r + 0.35, cy)], outline=(200, 30, 26, 255))
    if kind == "flam":
        flame = [(cx - 1.2, cy - 1.4), (cx - 1.4, cy - 0.2), (cx - 0.7, cy + 0.6), (cx - 0.5, cy + 0.1), (cx, cy + 1.7), (cx + 0.6, cy + 0.4),
                 (cx + 0.9, cy + 0.8), (cx + 1.3, cy - 0.3), (cx + 1.1, cy - 1.4)]
        f.d.polygon([f.px(x, y) for x, y in flame], fill=(24, 24, 26, 255))
        f.R(cx - 1.6, cy - 1.8, cx + 1.6, cy - 1.5, fill=(24, 24, 26, 255))
    else:
        ink = (24, 24, 26, 255)
        # Two test tubes tipped toward each other, their drops falling.
        for sx in (-1, 1):
            tube = [(cx + sx * 0.3, cy + 0.9), (cx + sx * 1.7, cy + 1.7), (cx + sx * 1.9, cy + 1.35), (cx + sx * 0.5, cy + 0.55)]
            f.d.polygon([f.px(x, y) for x, y in tube], fill=ink)
            f.E(cx + sx * 0.55, cy + 0.15, 0.13, fill=ink)
            f.E(cx + sx * 0.6, cy - 0.35, 0.11, fill=ink)
        # The bar being eaten (left) and the hand (right) under them, on the ground line.
        f.R(cx - 1.9, cy - 1.05, cx - 0.4, cy - 0.7, fill=ink)
        f.d.polygon([f.px(x, y) for x, y in ((cx - 0.9, cy - 0.7), (cx - 0.75, cy - 0.45), (cx - 0.55, cy - 0.7))], fill=(250, 250, 246, 255))
        f.d.polygon([f.px(x, y) for x, y in ((cx + 0.4, cy - 1.05), (cx + 1.9, cy - 1.05), (cx + 1.9, cy - 0.55), (cx + 1.2, cy - 0.3),
                                              (cx + 0.6, cy - 0.45), (cx + 0.4, cy - 0.7))], fill=ink)
        f.R(cx - 2.0, cy - 1.35, cx + 2.0, cy - 1.15, fill=ink)
    f.T(8.0, 4.4, "45 GAL  -  170 L", 7, fill=ink if kind == "flam" else (250, 250, 246, 255), bold=True)
    f.T(8.0, 3.2, "NFPA 30  -  OSHA 1910.106" if kind == "flam" else "KEEP DOORS CLOSED", 6, fill=(24, 24, 26, 255) if kind == "flam" else (250, 250, 246, 255), bold=True)
    return f


# ================================================================================================ document control
# ------------------------------------------------------------------------------------------------ mobile shelving
MS_KEY = "doc_mobile_shelving"
FS_KEY = "doc_fixed_shelving"
MS_SEGS = 4               # a carriage's length (blocks); seg 0 is the handwheel end (the model's front, +z)
MS_SLIDE = 3              # the slide state at rest; 0..6 moves it -3..+3 quarter blocks along its x (nuclear/MobileShelvingBlock.java)
MS_LABELS = ["EOP - AOP - ARP", "OPERATING PROCEDURES", "SURVEILLANCE PROCEDURES", "MAINTENANCE PROCEDURES", "TECH SPECS - FSAR",
             "DESIGN DRAWINGS", "VENDOR MANUALS", "QA RECORDS"]
MS_X = (0.6, 15.4)        # the carriage's sides
MS_SHELVES = {0: (1.6, 6.8, 12.0), 1: (1.2, 6.4, 11.6)}    # the shelves' tops in each half (lower, upper)


def _shelf_stock(side, y, seg, half, k):
    """What one shelf holds on one side of the carriage (side -1 west, +1 east): binders in runs (each run one box, its
    spines drawn), a pair of record storage boxes, the next run; varied by where it is."""
    out = []
    x0, x1 = (MS_X[0] + 0.3, 7.6) if side < 0 else (8.4, MS_X[1] - 0.3)
    face = "west" if side < 0 else "east"
    h = 4.0 if half == 0 else 3.8
    pick = (seg * 7 + half * 3 + k * 5 + (side > 0) * 11) % 4
    runs = [(0.8, 5.6), (5.9, 10.8), (11.0, 15.6)]
    for j, (z0, z1) in enumerate(runs):
        if (j + pick) % 3 == 1:
            # Two record boxes (their printed ends out), a hair apart.
            for b0, b1 in ((z0, (z0 + z1) / 2 - 0.05), ((z0 + z1) / 2 + 0.05, z1)):
                n = (seg * 5 + half * 2 + k + j + (side > 0)) % 4
                out.append((x0 + (0.3 if side > 0 else 0.0), y, b0, x1 - (0.0 if side > 0 else 0.3), y + h - 0.6, b1,
                            {"*": "pi_doc_box_side", face: f"pi_doc_box_{n}!", "up": "pi_doc_box_lid"}))
        else:
            n = (seg + half + k + j + (side > 0)) % 3
            out.append((x0, y, z0, x1, y + h - (0.3 if j % 2 else 0.0), z1, {"*": "pi_doc_binder_side", face: f"pi_doc_binders_{n}!",
                                                                             "up": "pi_doc_binder_top"}))
    return out


def shelving_cell(seg, half, slide=MS_SLIDE, fixed=False):
    """One block of a carriage (seg along it, half lower or upper), shifted by its slide: the base on its wheels (the
    lower half), the uprights at the section's back end, the shelves either side of the steel spine with what they hold,
    the top cap (upper half); at the handwheel end the laminate end panel, the label holder (the upper half; its card
    the carriage's label, nuclear/MobileShelvingBlock.java variant) and the three-spoke handwheel with its spinner
    knob; at the far end a plain end panel. A fixed unit has no wheels or handwheel."""
    dx = (slide - MS_SLIDE) * 4.0
    a, b = MS_X
    out = []
    zlo = 0.7 if seg == MS_SEGS - 1 else 0.0       # what stands inside stops short of the end panels
    zhi = 15.3 if seg == 0 else 16.0
    if half == 0:
        out += [(a, 0.4, 0.0, b, 1.6, 16.0, {"*": "pi_doc_base", "up": "pi_doc_shelf"})]
        if not fixed:
            for x in (a + 1.0, b - 2.4):
                out += [(x, 0.0, 6.6, x + 1.4, 0.4, 9.4, BLACK)]
    out += [(7.6, 0.0 if half else 1.6, 0.0, 8.4, 16.0 if half == 0 else 15.2, 16.0, "pi_doc_spine")]
    if seg != MS_SEGS - 1:
        out += [(a, 0.0 if half else 1.6, 0.0, b, 16.0 if half == 0 else 15.2, 0.6, "pi_doc_upright")]
    for k, y in enumerate(MS_SHELVES[half]):
        if k > 0 or half == 1:
            out += [(a + 0.1, y - 0.4, 0.6, b - 0.1, y, 16.0, {"*": "pi_doc_shelf_edge", "up": "pi_doc_shelf"})]
        out += _shelf_stock(-1, y, seg, half, k) + _shelf_stock(1, y, seg, half, k)
    clipped = []
    for bx in out:
        z0, z1 = max(bx[2], zlo), min(bx[5], zhi)
        if z1 - z0 > 0.05:
            clipped.append(bx[:2] + (z0,) + bx[3:5] + (z1,) + tuple(bx[6:]))
    out = clipped
    if half == 1:
        out += [(a - 0.2, 15.2, 0.0, b + 0.2, 15.8, 16.0, {"*": "pi_doc_end", "up": "pi_doc_cap"})]
    if seg == 0:
        top = 16.0 if half == 0 else 15.8
        out += [(a - 0.2, 0.0, 15.3, b + 0.2, top, 16.0, {"*": "pi_doc_end", "south": "pi_doc_end_face"})]
        if half == 1:
            out += [(4.2, 8.4, 16.0, 11.8, 11.6, 16.3, {"*": "pi_chrome", "south": "pi_doc_label_fixed!" if fixed else "pi_doc_label!"})]
            if not fixed:
                out += _handwheel(8.0, 2.4)
    elif seg == MS_SEGS - 1:
        top = 16.0 if half == 0 else 15.8
        out += [(a - 0.2, 0.0, 0.0, b + 0.2, top, 0.7, {"*": "pi_doc_end", "north": "pi_doc_end_face"})]
    return [_shift_x(bx, dx) for bx in out]


def _shift_x(bx, dx):
    """A box moved dx along x (a turned box's origin with it)."""
    out = (bx[0] + dx, bx[1], bx[2], bx[3] + dx, bx[4], bx[5], bx[6])
    if len(bx) > 7:
        rot = dict(bx[7]["rotation"])
        rot["origin"] = [rot["origin"][0] + dx] + list(rot["origin"][1:])
        out += ({"rotation": rot},)
    return out


def _cut_x(bx, c):
    """Block c's share of a box across several blocks (x in the piece's own pixels), moved into the block; a front face
    with its whole texture across it ("name!") keeps its own share of that texture."""
    x0, x1 = max(bx[0], c * 16), min(bx[3], c * 16 + 16)
    if x1 - x0 <= 0.01:
        return None
    tex = bx[6]
    if isinstance(tex, dict) and (x0, x1) != (bx[0], bx[3]):
        tex = dict(tex)
        for face in ("south", "up", "down"):
            t = tex.get(face)
            if isinstance(t, str) and t.endswith("!"):
                u0 = round((x0 - bx[0]) / (bx[3] - bx[0]) * 16, 4)
                u1 = round((x1 - bx[0]) / (bx[3] - bx[0]) * 16, 4)
                tex[face] = (t[:-1], [u0, 0, u1, 16])
    return (x0 - c * 16, bx[1], bx[2], x1 - c * 16, bx[4], bx[5], tex) + _moved_rot(bx, -c * 16)


def _handwheel(cx, cy):
    """The carriage's handwheel on its end panel (the upper half's bottom, about 1.1 m up): the hub on its boss, the rim
    (an octagonal ring), four spokes, the spinner knob on the rim."""
    out = round_z(cx, cy, 0.9, 16.0, 16.9, "pi_chrome") + round_z(cx, cy, 0.6, 16.9, 17.6, "pi_cap_black")
    out += ring_z(cx, cy, 2.7, 3.2, 16.9, 17.3, "pi_doc_wheel")
    out += [(cx - 2.8, cy - 0.2, 17.0, cx - 0.8, cy + 0.2, 17.2, "pi_chrome"), (cx + 0.8, cy - 0.2, 17.0, cx + 2.8, cy + 0.2, 17.2, "pi_chrome"),
            (cx - 0.2, cy + 0.8, 17.0, cx + 0.2, cy + 2.8, 17.2, "pi_chrome"), (cx - 0.2, cy - 2.8, 17.0, cx + 0.2, cy - 0.8, 17.2, "pi_chrome")]
    out += round_z(cx + 2.95, cy, 0.42, 17.3, 18.6, "pi_cap_black")
    return out


# ------------------------------------------------------------------------------------------------ the plan file
PF_KEY = "doc_plan_file"
PF_DRAWERS = 5
PF_Y = [(1.6 + k * 2.4, 1.6 + (k + 1) * 2.4 - 0.15) for k in range(PF_DRAWERS)]   # each drawer front's y, the bottom first
PF_FRONT = 14.6
PF_PULL = 9.0             # how far an open drawer comes out


def plan_file_part(c, r, drawer=0):
    """The plan file (two blocks across, waist high, under the window): its steel case on a plinth, the laminate top;
    five wide shallow drawers, each with its two pulls and the label card between them; drawer k (1..5, 0 none) pulled
    out, the drawing sheets lying in it."""
    out = []
    W = 32.0
    whole = [(0.4, 0.0, 1.2, W - 0.4, 1.4, PF_FRONT - 0.6, "pi_steel_dark"),
             (0.4, 1.4, 1.0, W - 0.4, 13.8, PF_FRONT - 0.2, "pi_doc_planfile"),
             (0.2, 13.8, 0.8, W - 0.2, 14.4, PF_FRONT + 0.2, {"*": "pi_doc_planfile", "up": "pi_doc_planfile_top"})]
    for k, (y0, y1) in enumerate(PF_Y):
        z = PF_FRONT + (PF_PULL if drawer == k + 1 else 0.0)
        whole.append((0.6, y0, z - 0.2, W - 0.6, y1, z, {"*": "pi_doc_planfile", "south": "pi_doc_planfile_drawer!"}))
        for px in (5.0, W - 5.0):
            whole.append((px - 1.6, (y0 + y1) / 2 - 0.25, z, px + 1.6, (y0 + y1) / 2 + 0.25, z + 0.5, "pi_chrome"))
        whole.append((W / 2 - 2.4, (y0 + y1) / 2 - 0.6, z, W / 2 + 2.4, (y0 + y1) / 2 + 0.6, z + 0.15, {"*": "pi_chrome", "south": f"pi_doc_planfile_card_{k}!"}))
        if drawer == k + 1:
            # The open drawer: its sides and bottom running back into the case, the sheets in it.
            whole += [(0.8, y0, PF_FRONT - 12.0 + PF_PULL, 1.2, y1 - 0.3, z - 0.2, "pi_doc_planfile"),
                      (W - 1.2, y0, PF_FRONT - 12.0 + PF_PULL, W - 0.8, y1 - 0.3, z - 0.2, "pi_doc_planfile"),
                      (1.2, y0, PF_FRONT - 12.0 + PF_PULL, W - 1.2, y0 + 0.3, z - 0.2, "pi_doc_planfile"),
                      (1.6, y0 + 0.3, PF_FRONT - 11.6 + PF_PULL, W - 1.6, y0 + 0.7, z - 0.6, {"*": "pi_paper", "up": "pi_doc_drawing!"})]
    for bx in whole:
        cut = _cut_x(bx, c)
        if cut:
            out.append(cut)
    return out


PF_SERIES = ["M-100  P&ID  RCS", "M-200  P&ID  CVCS", "M-300  P&ID  SECONDARY", "E-100  ONE-LINES", "C-100  CIVIL / STRUCT"]


# ------------------------------------------------------------------------------------------------ the issue counter
def issue_counter():
    """The document control issue counter (a row piece, three across): the visitors' transaction top over its front
    (the drawing lettered DOCUMENT CONTROL), the clerk's work surface behind; on it the computer (its screen live), the
    stamps and the ink pad by a stack of controlled copies, the in and out trays, the sign-out log and the bell."""
    body = [(0, 0, 1, 16, 15, 3, {"*": "pi_veneer", "north": "pi_doc_counter_front!"}),
            (0, 15, 0, 16, 16, 5, {"*": "pi_quartz", "up": "pi_quartz!"}),
            (0, 10.5, 3, 16, 11.5, 15.5, {"*": "pi_laminate", "up": "pi_laminate!"}),
            (0, 0, 3, 16, 1, 6, "pi_veneer")]
    left = [(0, 0, 3, 1, 10.5, 15.5, "pi_veneer")]
    right = [(15, 0, 3, 16, 10.5, 15.5, "pi_veneer")]
    pc = [(7, 11.5, 6, 9, 11.8, 8, "pi_monitor"), (7.6, 11.8, 6.5, 8.4, 13.5, 7, "pi_monitor"),
          (3.5, 13, 6, 12.5, 19, 6.8, {"*": "pi_monitor", "south": "pi_office_screen!"}),
          (4, 11.5, 9, 12, 11.9, 11.5, {"*": "pi_monitor", "up": "pi_keyboard!"})]
    stamps = [(1.6, 11.5, 7.0, 5.6, 11.9, 10.2, {"*": "pi_cap_black", "up": "pi_doc_inkpad"})]
    for k, x in enumerate((2.2, 4.0)):
        stamps += [(x, 11.9, 8.0, x + 1.2, 12.5, 9.2, "pi_doc_stamp_base"), (x + 0.4, 12.5, 8.4, x + 0.8, 14.0, 8.8, "pi_veneer"),
                   (x + 0.2, 14.0, 8.2, x + 1.0, 14.6, 9.0, "pi_doc_stamp_red" if k == 0 else BLACK)]
    for k in range(6):
        stamps.append((8.0, 11.5 + k * 0.32, 6.6, 14.0, 11.5 + (k + 1) * 0.32 - 0.02, 12.0,
                       {"*": "pi_paper", "up": "pi_doc_controlled!"} if k == 5 else "pi_paper"))
    trays = []
    for k, (y0, tex) in enumerate(((11.5, "pi_doc_tray_out"), (13.0, "pi_doc_tray_in"))):
        trays += [(1.4, y0, 6.4, 7.4, y0 + 0.2, 13.4, "pi_steel_dark"), (1.4, y0, 6.4, 1.6, y0 + 1.2, 13.4, "pi_steel_dark"),
                  (7.2, y0, 6.4, 7.4, y0 + 1.2, 13.4, "pi_steel_dark"), (1.6, y0, 13.2, 7.2, y0 + 1.2, 13.4, {"*": "pi_steel_dark", "south": tex + "!"}),
                  (2.0, y0 + 0.2, 7.0, 6.8, y0 + 0.5, 12.6, {"*": "pi_paper", "up": "pi_doc_6!"})]
    trays += [(1.6, 11.5, 2.0, 1.8, 13.0, 2.2, "pi_steel_dark")]
    trays += [(9.0, 15.0, 1.0, 14.2, 16.4, 4.6, {"*": "pi_tsc_hardboard", "up": "pi_doc_signout!"})]
    trays += PP.round_y(11.0, 2.6, 0.9, 16.0, 16.8, "pi_chrome") + PP.star_y(11.0, 2.6, 0.25, 16.8, 17.3, "pi_chrome")
    return {"display": "Document Control Issue Counter", "row": "doccounter", "boxes": body, "left": left, "right": right,
            "tops": [pc, stamps, trays], "glow": ["pi_office_screen"]}


# ================================================================================================ the board room
# ------------------------------------------------------------------------------------------------ the boardroom table
BT_W, BT_D = 8, 3          # its blocks along and across
BT_TOP = (11.6, 12.6)      # the top's underside and top (0.79 m)
BT_SEATS = [c for c in range(1, BT_W - 1)]


def _table_outline():
    """The top's shape in the table's own pixels (x 0..128 along, z 0..48 across): a racetrack, its ends round; as
    columns of boxes (x0, x1, z0, z1)."""
    L, Wd = BT_W * 16, BT_D * 16
    inset = 1.0
    r = Wd / 2 - inset
    cols = []
    # Columns 2 px wide on even x, so none straddles a block's edge (the top is cut into its blocks).
    edges = [inset] + [float(e) for e in range(2, L) if e % 2 == 0 or e < inset + r or e > L - inset - r] + [L - inset]
    for x, x1 in zip(edges, edges[1:]):
        mid = (x + x1) / 2
        if mid < inset + r:
            dx = inset + r - mid
        elif mid > L - inset - r:
            dx = mid - (L - inset - r)
        else:
            dx = 0.0
        h = math.sqrt(max(0.0, r * r - dx * dx))
        cols.append((x, x1, round(Wd / 2 - h, 3), round(Wd / 2 + h, 3)))
    return cols


def board_table_part(c, r):
    """Block (c across, r along its width) of the boardroom table: its slice of the walnut top (the racetrack, its inlay
    band in the texture), the two steel pedestals, the cable cubbies down its middle (brushed aluminium lids), a
    delegate microphone at every seat (its base, the gooseneck, the red talk ring), the conference phone in the middle,
    the water carafes and glasses, the notepads and pens."""
    whole = []
    y0, y1 = BT_TOP
    for x0, x1, z0, z1 in _table_outline():
        whole.append((x0, y0, z0, x1, y1, z1, {"*": "pi_walnut_edge", "up": ("pi_board_top", [x0 / 8, z0 / 8, x1 / 8, z1 / 8])}))
    # The pedestals (a steel column on a wide foot) under it.
    for px in (32.0, 96.0):
        whole += [(px - 6.0, 0.0, 16.0, px + 6.0, 0.6, 32.0, "pi_steel_dark"), (px - 1.6, 0.6, 22.4, px + 1.6, y0, 25.6, "pi_chrome")]
    # The cable cubbies: a lid (raised a little) over each middle block, its hinge line.
    for cx in range(1, BT_W - 1):
        x = cx * 16 + 8
        whole.append((x - 2.4, y1, 22.8, x + 2.4, y1 + 0.2, 25.2, {"*": "pi_chrome", "up": "pi_board_cubby!"}))
    # A delegate microphone at each seat, both sides, and one at each end: the base by the edge, the stalk, the head.
    for cx in BT_SEATS:
        x = cx * 16 + 8
        for zb, sgn in ((6.6, -1), (41.4, 1)):
            whole += _mic(x + 3.2, zb, sgn)
            whole += [(x - 4.6, y1, zb - 2.0 * sgn - 1.6, x - 0.6, y1 + 0.15, zb - 2.0 * sgn + 1.6, {"*": "pi_paper", "up": "pi_board_pad!"}),
                      (x - 0.3, y1 + 0.15, zb - 2.0 * sgn - 1.4, x - 0.0, y1 + 0.4, zb - 2.0 * sgn + 1.4, "pi_board_pen")]
    for xe, sgn in ((6.0, -1), (122.0, 1)):
        whole += _mic(xe, 24.0 - 3.0 * sgn, 0, along=sgn)
    # The conference phone: its round body, the display, the three satellite microphones' grilles.
    whole += PP.round_y(56.0, 24.0, 3.2, y1, y1 + 0.8, {"*": "pi_cap_black", "up": "pi_board_phone!"})
    # Water: a carafe and glasses on a tray at each end of the middle.
    for x in (40.0, 88.0):
        whole += [(x - 3.0, y1, 21.0, x + 3.0, y1 + 0.2, 27.0, "pi_steel_dark")]
        whole += PP.round_y(x, 24.0, 1.2, y1 + 0.2, y1 + 4.4, "pi_board_glass") + PP.round_y(x, 24.0, 0.5, y1 + 4.4, y1 + 5.2, "pi_board_glass")
        for gx, gz in ((x - 2.0, 22.4), (x + 2.0, 25.6)):
            whole += PP.round_y(gx, gz, 0.6, y1 + 0.2, y1 + 2.0, "pi_board_glass")
    out = []
    for bx in whole:
        x0, x1 = max(bx[0], c * 16), min(bx[3], c * 16 + 16)
        z0, z1 = max(bx[2], r * 16), min(bx[5], r * 16 + 16)
        if x1 - x0 > 0.01 and z1 - z0 > 0.01:
            tex = bx[6]
            if isinstance(tex, dict) and isinstance(tex.get("up"), tuple):
                # The top's slice of the whole top's picture (its texture across the table: 8 pixels a texture unit).
                tex = dict(tex, up=(tex["up"][0], [round(x0 / 8, 4), round(z0 / 8, 4), round(x1 / 8, 4), round(z1 / 8, 4)]))
            elif isinstance(tex, dict) and isinstance(tex.get("up"), str) and tex["up"].endswith("!") and \
                    (x0, x1, z0, z1) != (bx[0], bx[3], bx[2], bx[5]):
                # A top with its whole picture across it cut over blocks (the conference phone): each its share.
                fu = lambda v, a, b: round((v - a) / (b - a) * 16, 4)
                tex = dict(tex, up=(tex["up"][:-1], [fu(x0, bx[0], bx[3]), fu(z0, bx[2], bx[5]), fu(x1, bx[0], bx[3]), fu(z1, bx[2], bx[5])]))
            out.append((x0 - c * 16, bx[1], z0 - r * 16, x1 - c * 16, bx[4], z1 - r * 16, tex) + _moved_rot(bx, -c * 16, -r * 16))
    return out


def board_table_item():
    """The table's item model (it is eight blocks long, too long to be shown whole in a slot): a small racetrack table,
    the walnut top with its edge, the conference phone in the middle, its two pedestals on their plates."""
    top = {"*": "pi_walnut_edge", "up": "pi_board_top"}
    out = [(2.0, 8.0, 4.5, 14.0, 9.2, 11.5, top)]
    # Its rounded ends: two narrowing steps each end, butting the top (no face over another).
    for x0, x1, z0, z1 in ((1.0, 2.0, 5.4, 10.6), (0.4, 1.0, 6.6, 9.4), (14.0, 15.0, 5.4, 10.6), (15.0, 15.6, 6.6, 9.4)):
        out.append((x0, 8.0, z0, x1, 9.2, z1, top))
    out += PP.round_y(8.0, 8.0, 1.0, 9.2, 9.9, {"*": "pi_cap_black", "up": "pi_board_phone!"})
    for px in (4.6, 11.4):
        out += [(px - 2.0, 0.0, 6.0, px + 2.0, 0.4, 10.0, "pi_steel_dark"), (px - 0.6, 0.4, 7.4, px + 0.6, 8.0, 8.6, "pi_chrome")]
    return out


def _moved_rot(bx, dx, dz=0.0):
    """A turned box's rotation with its origin moved with the box (empty for a box not turned)."""
    if len(bx) <= 7:
        return ()
    rot = dict(bx[7]["rotation"])
    o = rot["origin"]
    rot["origin"] = [o[0] + dx, o[1], o[2] + dz]
    return ({"rotation": rot},)


def _mic(x, z, side, along=0):
    """A delegate microphone: its unit (the talk button, the red ring), the gooseneck up and toward the seat, the head
    with its red light ring."""
    y = BT_TOP[1]
    out = [(x - 1.2, y, z - 1.0, x + 1.2, y + 0.6, z + 1.0, {"*": "pi_cap_black", "up": "pi_board_mic_unit!"})]
    if along:
        out += [(x - 0.12, y + 0.6, z - 0.12, x + 0.12, y + 5.2, z + 0.12, "pi_cap_black"),
                (x - 0.2 + along * 0.4, y + 5.2, z - 0.2, x + 0.2 + along * 0.4, y + 6.4, z + 0.2, "pi_cap_black")]
    else:
        out += [(x - 0.12, y + 0.6, z - 0.12, x + 0.12, y + 5.2, z + 0.12, "pi_cap_black"),
                (x - 0.2, y + 5.2, z - 0.2 + side * 0.6, x + 0.2, y + 5.6, z + 0.2 + side * 0.6, "pi_board_mic_ring"),
                (x - 0.24, y + 5.6, z - 0.24 + side * 0.6, x + 0.24, y + 6.6, z + 0.24 + side * 0.6, "pi_cap_black")]
    return out


# ------------------------------------------------------------------------------------------------ the AV credenza
AV_KEY = "board_av_credenza"
AV_TOP = 12.0
AV_FRONT = 8.0


def av_credenza_boxes(open_=False):
    """The AV credenza under the overview screen (three blocks across): walnut, its plinth, the louvred doors (the
    drawing), the aluminium pulls; on top the video conference bar (its camera lens, the microphone grilles, the
    status light) and the room's touch controller on its stand."""
    F = f"pi_{AV_KEY}_f"
    out = [(1.0, 0.0, 1.4, 47.0, 1.0, AV_FRONT - 0.6, "pi_cap_black"),
           (0.4, 1.0, 0.8, 47.6, AV_TOP - 0.6, AV_FRONT, {"*": "pi_walnut", "south": F}),
           (0.2, AV_TOP - 0.6, 0.6, 47.8, AV_TOP, AV_FRONT + 0.3, {"*": "pi_walnut", "up": "pi_walnut"})]
    for x in (11.6, 23.6, 35.6):
        out += [(x - 0.25, 4.2, AV_FRONT, x + 0.25, 8.4, AV_FRONT + 0.6, "pi_chrome")]
    # The conference bar: its body, the lens (proud), the grilles in the drawing.
    out += [(14.0, AV_TOP, 3.6, 34.0, AV_TOP + 2.2, 6.6, {"*": "pi_cap_black", "south": F})]
    out += round_z(24.0, AV_TOP + 1.1, 0.8, 6.6, 7.3, "pi_board_lens")
    # The touch controller: its foot, the tilted screen.
    out += [(39.0, AV_TOP, 3.4, 43.0, AV_TOP + 0.3, 6.4, "pi_cap_black"),
            (39.2, AV_TOP + 0.3, 4.0, 42.8, AV_TOP + 3.2, 4.4, {"*": "pi_cap_black", "south": F})]
    return out


def av_credenza(label=None):
    f = PP.Front(3, 1110)
    f.R(0, 0, 48, 32, fill=(96, 64, 42, 255))
    # The doors: four across, louvred (a ventilation slot pattern for the equipment inside), their seams.
    for k in range(4):
        x0 = 0.6 + k * 11.7
        x1 = x0 + 11.5
        f.R(x0, 1.2, x1, AV_TOP - 0.8, fill=(104, 70, 46, 255), outline=(60, 40, 26, 255), width=2)
        for y in range(int(2.4 * 10), int((AV_TOP - 2.0) * 10), 6):
            f.R(x0 + 2.0, y / 10, x1 - 2.0, y / 10 + 0.22, fill=(46, 30, 20, 255))
    # The conference bar's front: the speaker and microphone grilles either side of the lens, the status light.
    for x0, x1 in ((14.4, 22.4), (25.6, 33.6)):
        f.R(x0, AV_TOP + 0.3, x1, AV_TOP + 1.9, fill=(40, 40, 44, 255))
        for k in range(int((x1 - x0) / 0.5)):
            f.R(x0 + 0.15 + k * 0.5, AV_TOP + 0.5, x0 + 0.35 + k * 0.5, AV_TOP + 1.7, fill=(20, 20, 22, 255))
    f.E(24.0, AV_TOP + 1.95, 0.12, fill=(80, 200, 255, 255))
    # The touch controller's screen: the room's controls.
    f.R(39.4, AV_TOP + 0.5, 42.6, AV_TOP + 3.0, fill=(18, 22, 30, 255))
    for k, (name, col) in enumerate((("DISPLAY", (40, 120, 200)), ("CALL", (40, 160, 90)), ("MUTE", (180, 50, 40)))):
        f.R(39.7, AV_TOP + 2.3 - k * 0.65, 42.3, AV_TOP + 2.8 - k * 0.65, fill=col + (255,))
        f.T(41.0, AV_TOP + 2.55 - k * 0.65, name, 5, fill=WHITE, bold=True)
    return f


# ------------------------------------------------------------------------------------------------ the lectern
def lectern():
    """The lectern by the screen (Justin: it faces the crowd): its plinth, the walnut body with the plant's emblem on its
    front to the room, the reading top sloping down toward the speaker behind it (its lip at the low edge to hold the
    papers, the overhang at the front), the gooseneck microphone on the front edge bent back to the speaker, the reading
    light. The speaker stands on its user's side (+z), looking at the room (-z)."""
    W = "pi_walnut"
    tilt = lambda: {"rotation": {"angle": 22.5, "axis": "x", "origin": [8, 16.0, 13.0]}}
    out = [(2.0, 0.0, 3.0, 14.0, 0.8, 13.6, "pi_cap_black"),
           (2.6, 0.8, 4.0, 13.4, 15.4, 12.6, {"*": W, "north": "pi_board_lectern_front!"}),
           # The wedge under the top (its lower part buried in the body), the top, the lip at its low edge.
           (2.8, 12.4, 3.8, 13.2, 16.0, 13.0, W, tilt()),
           (1.8, 16.0, 3.2, 14.2, 16.8, 13.4, {"*": W, "up": "pi_walnut"}, tilt()),
           (1.8, 16.8, 12.8, 14.2, 17.5, 13.4, W, tilt())]
    # The gooseneck microphone on the front edge (the speaker's left), bent back toward the speaker.
    out += [(3.0, 19.9, 4.4, 4.0, 20.5, 5.6, "pi_cap_black"), (3.35, 20.5, 4.85, 3.65, 24.2, 5.15, "pi_cap_black"),
            (3.3, 24.0, 5.15, 3.7, 24.4, 7.4, "pi_cap_black"), (3.25, 23.6, 7.4, 3.75, 24.6, 8.4, "pi_board_mic_ring")]
    # The reading light on the right: its foot, the stem, the shade over the top.
    out += [(11.8, 19.9, 4.4, 12.8, 20.5, 5.4, "pi_chrome"), (12.15, 20.5, 4.75, 12.45, 22.8, 5.05, "pi_chrome"),
            (10.6, 22.6, 4.8, 13.4, 23.2, 7.4, "pi_chrome")]
    return out


# ================================================================================================ the manager's office
# ------------------------------------------------------------------------------------------------ the direct line
HL_KEY = "mgr_hotline"


def hotline_boxes(open_=False):
    """The direct line to the control room (on the wall by the desk): its red steel box, the label plate, the handset on
    its hook (its cord coiled under it), the call lamp on top."""
    F = f"pi_{HL_KEY}_f"
    R = "pi_mgr_red"
    out = [(3.0, 1.2, 0.0, 13.0, 14.6, 2.6, {"*": R, "south": F}),
           # The handset: its earpiece and mouthpiece cups, the grip, on the hook.
           (5.6, 3.2, 2.6, 7.6, 12.6, 4.4, R), (5.2, 10.8, 2.6, 8.0, 13.2, 5.0, {"*": BLACK, "south": R}),
           (5.2, 2.6, 2.6, 8.0, 5.0, 5.0, {"*": BLACK, "south": R}),
           (8.0, 11.4, 2.6, 8.8, 12.0, 4.0, "pi_chrome"),
           # The cord: a coil under the handset to the box.
           (6.2, 1.2, 3.0, 7.0, 2.6, 3.8, BLACK), (6.6, 0.4, 2.4, 7.4, 1.2, 3.4, BLACK)]
    out += PP.round_y(8.0, 1.4, 1.0, 14.6, 15.8, "pi_mgr_lamp")
    return out


def hotline(label=None):
    f = PP.Front(1, 1120)
    f.R(0, 0, 16, 32, fill=(176, 28, 26, 255))
    f.R(9.0, 7.0, 12.6, 13.6, fill=(236, 236, 230, 255), outline=(30, 30, 32, 255), width=2)
    f.T(10.8, 12.7, "DIRECT", 6.5, fill=INK, bold=True)
    f.T(10.8, 11.9, "LINE", 6.5, fill=INK, bold=True)
    f.T(10.8, 10.6, "CONTROL", 6, fill=(176, 28, 26, 255), bold=True)
    f.T(10.8, 9.9, "ROOM", 6, fill=(176, 28, 26, 255), bold=True)
    f.T(10.8, 8.6, "SHIFT", 6, fill=INK, bold=True)
    f.T(10.8, 7.9, "MANAGER", 5.5, fill=INK, bold=True)
    f.R(9.2, 2.6, 12.4, 5.6, fill=(40, 40, 44, 255))
    for k in range(4):
        for j in range(3):
            f.E(9.8 + j * 1.0, 5.0 - k * 0.7, 0.22, fill=(200, 202, 204, 255))
    return f


# ------------------------------------------------------------------------------------------------ the license, the awards
LIC_KEY = "mgr_license"
AW_KEY = "mgr_awards"


def license_boxes(open_=False):
    """The framed operating license (two blocks across): the walnut frame standing off the wall round the certificate."""
    F = f"pi_{LIC_KEY}_f"
    fr = "pi_walnut"
    return [(3.0, 1.6, 0.1, 29.0, 14.4, 0.6, {"*": fr, "south": F}),
            (2.0, 0.6, 0.0, 30.0, 1.6, 1.4, fr), (2.0, 14.4, 0.0, 30.0, 15.4, 1.4, fr),
            (2.0, 1.6, 0.0, 3.0, 14.4, 1.4, fr), (29.0, 1.6, 0.0, 30.0, 14.4, 1.4, fr),
            (3.0, 1.6, 1.0, 29.0, 14.4, 1.1, {"south": "pi_mgr_glass", "north": "pi_mgr_glass"})]


def license_drawing(label=None):
    f = PP.Front(2, 1121)
    f.R(0, 0, 32, 32, fill=(96, 64, 42, 255))
    f.R(3.0, 1.6, 29.0, 14.4, fill=(232, 226, 204, 255))
    f.R(3.6, 2.2, 28.4, 13.8, outline=(150, 120, 60, 255), width=3)
    f.R(4.0, 2.6, 28.0, 13.4, outline=(150, 120, 60, 255), width=1)
    ink = (40, 34, 30, 255)
    RR._fit(f, 16.0, 12.6, "NUCLEAR REGULATORY COMMISSION", 9, 22.0, fill=ink)
    RR._fit(f, 16.0, 11.4, "FACILITY OPERATING LICENSE", 12, 22.0, fill=(120, 30, 26, 255))
    f.T(16.0, 10.4, "LICENSE NO. NPF-99", 7, fill=ink, bold=True)
    f.T(16.0, 9.4, "JUGCRAFT NUCLEAR GENERATING STATION, UNIT 1", 6.5, fill=ink, bold=True)
    for k, line in enumerate(("The licensee is authorized to operate the facility at reactor core power",
                              "levels not in excess of 3,411 megawatts thermal (100% power) in accordance",
                              "with the Technical Specifications and the Environmental Protection Plan.")):
        f.T(16.0, 8.2 - k * 0.7, line, 5.5, fill=ink)
    # The seal (a gold rosette with its ribbons) and the signatures.
    f.d.polygon([f.px(7.4, 3.4), f.px(6.6, 1.9), f.px(7.4, 2.4), f.px(8.2, 1.9)], fill=(150, 30, 30, 255))
    f.d.polygon([f.px(8.4, 3.4), f.px(7.6, 1.9), f.px(8.4, 2.4), f.px(9.2, 1.9)], fill=(150, 30, 30, 255))
    for k in range(16):
        a = 2 * math.pi * k / 16
        f.E(7.9 + math.cos(a) * 1.15, 3.9 + math.sin(a) * 1.15, 0.42, fill=(212, 170, 60, 255))
    f.E(7.9, 3.9, 1.1, fill=(222, 182, 70, 255), outline=(170, 130, 40, 255))
    f.T(7.9, 3.9, "NRC", 5.5, fill=(120, 90, 30, 255), bold=True)
    for x, who in ((17.0, "FOR THE COMMISSION"), (24.4, "DIRECTOR, OFFICE OF NRR")):
        f.line([(x - 2.8, 3.6), (x - 1.6, 4.3), (x - 0.4, 3.7), (x + 0.6, 4.4), (x + 1.8, 3.8), (x + 2.6, 4.1)], (30, 40, 90, 255), 1)
        f.line([(x - 3.0, 3.3), (x + 3.0, 3.3)], ink, 1)
        f.T(x, 2.8, who, 5, fill=ink, bold=True)
    return f


AW_PLAQUES = [(1.2, 14.8), (16.8, 31.2), (34.0, 46.8)]


def awards_boxes(open_=False):
    """The awards wall (three blocks across): three walnut plaques standing off the wall, each with its brass plate."""
    F = f"pi_{AW_KEY}_f"
    out = []
    for x0, x1 in AW_PLAQUES:
        out += [(x0, 1.6, 0.0, x1, 14.6, 0.9, {"*": "pi_walnut", "south": F}),
                (x0 + 1.0, 2.6, 0.9, x1 - 1.0, 13.6, 1.05, {"*": "pi_mgr_brass", "south": F})]
    return out


def awards(label=None):
    f = PP.Front(3, 1122)
    f.R(0, 0, 48, 32, fill=(96, 64, 42, 255))
    ink = (40, 30, 16, 255)
    texts = [("SAFETY EXCELLENCE", "2,000,000 HOURS", "WITHOUT A LOST-TIME INJURY", "PRESENTED TO THE STAFF OF UNIT 1"),
             ("BREAKER TO BREAKER", "CYCLE 23", "512 DAYS ON LINE", "RFO-23 TO RFO-24"),
             ("INDUSTRY EXCELLENCE", "TOP DECILE", "CAPACITY FACTOR 97.4%", "OPERATING PERFORMANCE 2025")]
    for (x0, x1), (t1, t2, t3, t4) in zip(AW_PLAQUES, texts):
        f.R(x0 + 1.0, 2.6, x1 - 1.0, 13.6, fill=(200, 164, 80, 255))
        f.R(x0 + 1.4, 3.0, x1 - 1.4, 13.2, outline=(120, 90, 30, 255), width=2)
        cx = (x0 + x1) / 2
        # The emblem: a stylised atom over the words.
        for k, (rx, ry) in enumerate(((2.2, 0.8), (0.8, 2.2))):
            f.d.ellipse([f.px(cx - rx, 11.4 + ry), f.px(cx + rx, 11.4 - ry)], outline=ink, width=2)
        f.E(cx, 11.4, 0.3, fill=ink)
        RR._fit(f, cx, 8.9, t1, 10, x1 - x0 - 3.4, fill=ink)
        RR._fit(f, cx, 7.4, t2, 14, x1 - x0 - 3.4, fill=ink)
        RR._fit(f, cx, 5.9, t3, 9, x1 - x0 - 3.4, fill=ink)
        RR._fit(f, cx, 4.0, t4, 7, x1 - x0 - 3.4, fill=ink)
    return f


# ------------------------------------------------------------------------------------------------ the hard hat hooks
def ppe_hooks():
    """By the office door: a hook rail with the manager's white hard hat, his safety glasses and his high-visibility
    vest."""
    out = [(1.0, 10.6, 0.0, 15.0, 12.0, 0.6, "pi_walnut")]
    for x in (3.6, 12.4):
        out += [(x - 0.3, 10.9, 0.6, x + 0.3, 11.5, 2.2, "pi_chrome"), (x - 0.3, 11.5, 1.8, x + 0.3, 12.3, 2.2, "pi_chrome")]
    # The vest hanging from the first hook: its back panel hung flat, the front's two panels folded over it (the
    # stripes in the texture), the collar.
    out += [(1.0, 1.6, 1.0, 6.2, 11.0, 1.6, "pi_mgr_vest"),
            (1.0, 1.6, 1.6, 3.5, 10.4, 2.2, {"*": "pi_mgr_vest", "south": ("pi_mgr_vest_front", [0, 0, 8, 16])}),
            (3.7, 1.6, 1.6, 6.2, 10.4, 2.2, {"*": "pi_mgr_vest", "south": ("pi_mgr_vest_front", [8, 0, 16, 16])}),
            (2.6, 10.4, 1.4, 4.6, 11.4, 2.0, "pi_mgr_vest")]
    # The hard hat sitting on its little shelf over the middle hook (two brackets under it): its brim, the dome in
    # steps, the badge on its front.
    out += [(5.0, 6.0, 0.6, 11.0, 6.6, 7.6, "pi_walnut"), (5.6, 4.4, 0.6, 6.2, 6.0, 4.0, "pi_chrome"), (9.8, 4.4, 0.6, 10.4, 6.0, 4.0, "pi_chrome")]
    out += PP.round_y(8.0, 4.4, 3.2, 6.6, 7.3, "pi_mgr_hat")
    out += PP.round_y(8.0, 4.4, 2.7, 7.3, 9.4, "pi_mgr_hat") + PP.round_y(8.0, 4.4, 2.1, 9.4, 10.5, "pi_mgr_hat")
    out += PP.round_y(8.0, 4.4, 1.3, 10.5, 11.3, "pi_mgr_hat")
    out += [(7.1, 7.7, 7.1, 8.9, 9.1, 7.25, {"*": "pi_mgr_hat", "south": "pi_mgr_hat_badge!"})]
    # The safety glasses on the last hook: the lenses and the arms.
    out += [(10.6, 7.6, 2.2, 14.2, 9.0, 2.4, "pi_mgr_glasses"), (10.6, 8.6, 0.6, 10.8, 8.9, 2.2, "pi_cap_black"),
            (14.0, 8.6, 0.6, 14.2, 8.9, 2.2, "pi_cap_black")]
    return out


# ================================================================================================ signs
SIGNS = {"sign_chem_lab": "Chemistry Laboratory Sign", "sign_doc_control": "Document Control Sign"}


def sign_tex(kind):
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, H - 1], fill=(250, 250, 246))
    if kind == "sign_chem_lab":
        d.rectangle([0, 0, W - 1, 110], fill=(20, 90, 150))
        SGR._text(d, (W / 2, 38), "CHEMISTRY", 46, (250, 250, 246), width=W - 40)
        SGR._text(d, (W / 2, 84), "LABORATORY", 40, (250, 250, 246), width=W - 40)
        # The NFPA 704 diamond: health (blue) 3, flammability (red) 3, instability (yellow) 1, special (white) COR.
        cx, cy, r = W / 2, 236, 96
        quads = [((cx - r / 2, cy), (20, 90, 200), "3"), ((cx, cy - r / 2), (210, 30, 30), "3"), ((cx + r / 2, cy), (240, 210, 30), "1"),
                 ((cx, cy + r / 2), (250, 250, 246), "COR")]
        for (qx, qy), col, txt in quads:
            h = r / 2
            d.polygon([(qx, qy - h), (qx + h, qy), (qx, qy + h), (qx - h, qy)], fill=col, outline=(24, 24, 26), width=4)
            SGR._text(d, (qx, qy), txt, 44 if len(txt) == 1 else 26, (24, 24, 26))
        d.rectangle([24, 348, W - 25, 356], fill=(20, 90, 150))
        SGR._text(d, (W / 2, 386), "EYE PROTECTION", 30, (24, 24, 26), width=W - 50)
        SGR._text(d, (W / 2, 418), "AND LAB COAT REQUIRED", 26, (24, 24, 26), width=W - 50)
        SGR._text(d, (W / 2, 462), "NO FOOD OR DRINK", 28, (176, 30, 30), width=W - 50)
        d.rectangle([10, 10, W - 11, H - 11], outline=(20, 90, 150), width=8)
    else:
        d.rectangle([0, 0, W - 1, 150], fill=(30, 70, 140))
        SGR._text(d, (W / 2, 52), "DOCUMENT", 56, (250, 250, 246), width=W - 40)
        SGR._text(d, (W / 2, 112), "CONTROL", 56, (250, 250, 246), width=W - 40)
        SGR._text(d, (W / 2, 196), "CONTROLLED COPIES", 34, (24, 24, 26), width=W - 50)
        SGR._text(d, (W / 2, 238), "ISSUED HERE", 34, (24, 24, 26), width=W - 50)
        d.rectangle([24, 280, W - 25, 288], fill=(30, 70, 140))
        SGR._text(d, (W / 2, 326), "RETURN SUPERSEDED", 28, (24, 24, 26), width=W - 50)
        SGR._text(d, (W / 2, 360), "COPIES FOR DESTRUCTION", 26, (24, 24, 26), width=W - 50)
        SGR._text(d, (W / 2, 420), "QA RECORDS MAY NOT", 26, (176, 30, 30), width=W - 50)
        SGR._text(d, (W / 2, 452), "LEAVE THIS ROOM", 26, (176, 30, 30), width=W - 50)
        d.rectangle([10, 10, W - 11, H - 11], outline=(30, 70, 140), width=8)
    return img


# ================================================================================================ textures
def _canvas(size=256, colour=(255, 255, 255), h=None):
    """A texture's canvas: square, or size by h when the face it goes on is not square (then brought to a square,
    _square, so the face's own stretch puts it back in proportion)."""
    img = Image.new("RGBA", (size, h or size), colour + (255,))
    return img, ImageDraw.Draw(img)


def _square(img):
    w, h = img.size
    return img if w == h else img.resize((max(w, h), max(w, h)), Image.LANCZOS)


def _f(size, bold=True):
    return PP.rfont(size, bold)


def _yellow_push():
    img, d = _canvas(256, (236, 196, 32), 163)
    d.rectangle([0, 0, 255, 162], outline=(150, 120, 10), width=8)
    SGR._text(d, (128, 82), "PUSH", 80, (24, 24, 26), width=220)
    return _square(img)


def _shower_sign():
    """The green safe-condition sign: the shower over a figure, EMERGENCY SHOWER, EYE WASH."""
    img, d = _canvas(512, (0, 130, 70))
    d.rectangle([12, 12, 499, 499], outline=(250, 250, 246), width=10)
    # The pictogram: the head (a shower rose), its drops, a person under it, the eye and the drops.
    d.rectangle([150, 70, 290, 92], fill=(250, 250, 246))
    d.rectangle([270, 50, 290, 92], fill=(250, 250, 246))
    for k in range(6):
        x = 160 + k * 22
        d.line([(x, 104), (x - 6, 150)], fill=(250, 250, 246), width=7)
    d.ellipse([196, 160, 236, 200], fill=(250, 250, 246))
    d.rectangle([200, 204, 232, 290], fill=(250, 250, 246))
    d.rectangle([182, 214, 250, 230], fill=(250, 250, 246))
    d.ellipse([310, 170, 400, 220], outline=(250, 250, 246), width=8)
    d.ellipse([340, 180, 370, 210], fill=(250, 250, 246))
    for k in range(3):
        d.line([(330 + k * 26, 240), (326 + k * 26, 274)], fill=(250, 250, 246), width=7)
    SGR._text(d, (256, 352), "EMERGENCY", 64, (250, 250, 246), width=460)
    SGR._text(d, (256, 418), "SHOWER", 64, (250, 250, 246), width=460)
    SGR._text(d, (256, 474), "& EYE WASH", 44, (250, 250, 246), width=460)
    return img


def _cab_side(kind):
    col = (236, 196, 32) if kind == "flam" else (34, 84, 160)
    img = PP._solid(col, 1130 if kind == "flam" else 1131, 0.04).convert("RGBA").resize((256, 256))
    d = ImageDraw.Draw(img)
    ink = (196, 28, 24) if kind == "flam" else (250, 250, 246)
    SGR._text(d, (128, 90), "FLAMMABLE" if kind == "flam" else "CORROSIVE", 40, ink, width=220)
    SGR._text(d, (128, 130), "KEEP FIRE AWAY" if kind == "flam" else "STORE ACIDS LOW", 22, ink, width=220)
    return img


def _can_red():
    img = PP._solid((196, 30, 26), 1132, 0.06).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 50, 117, 96], fill=(236, 196, 32))
    SGR._text(d, (64, 64), "DANGER", 18, (24, 24, 26))
    SGR._text(d, (64, 84), "FLAMMABLE", 14, (24, 24, 26))
    return img


def _chem_label(name, sub, bg=(250, 250, 246)):
    """A reagent bottle's label (a patch about 1.1 by 2.3: taller than wide): its name, what it is, the hazard bar."""
    img, d = _canvas(128, bg, 256)
    d.rectangle([0, 0, 127, 34], fill=(30, 70, 140))
    SGR._text(d, (64, 17), "LAB GRADE", 16, (250, 250, 246), width=118)
    SGR._text(d, (64, 92), name, 34, (24, 24, 26), width=118)
    SGR._text(d, (64, 140), sub, 14, (60, 60, 64), width=118)
    d.rectangle([10, 186, 117, 236], outline=(196, 28, 24), width=5)
    SGR._text(d, (64, 211), "DANGER", 20, (196, 28, 24), width=100)
    return _square(img)


def _binders(n):
    """A run of ring binders seen end on: their coloured spines, each with its white label window and finger hole."""
    import random as _r
    rng = _r.Random(1140 + n)
    img, d = _canvas(512, (30, 30, 32))
    x = 0
    cols = [(160, 30, 30), (30, 60, 140), (30, 110, 60), (24, 24, 26), (200, 160, 30), (90, 40, 120), (230, 230, 226)]
    titles = ["EOP", "AOP", "ARP", "OP", "OST", "MP", "TS", "QA", "DCN", "FSAR", "CAL", "SURV", "RP", "CHEM"]
    while x < 512:
        w = rng.choice((52, 60, 68, 76))
        c = rng.choice(cols)
        d.rectangle([x, 8, x + w - 3, 511], fill=c)
        d.rectangle([x + 6, 60, x + w - 9, 250], fill=(244, 244, 238))
        SGR._text(d, (x + (w - 3) / 2, 100), rng.choice(titles), 20, (24, 24, 26), width=w - 16)
        for k in range(4):
            d.line([(x + 10, 140 + k * 26), (x + w - 13, 140 + k * 26)], fill=(150, 150, 150), width=3)
        d.ellipse([x + (w - 3) / 2 - 10, 400, x + (w - 3) / 2 + 10, 420], fill=(14, 14, 16))
        x += w
    return img


def _record_box(n):
    """A record storage box's end: white board, the printed frame, the handhold, its label (what, which years, the box)."""
    labels = [("QA RECORDS", "2019", "BOX 112"), ("RCS SURV", "2022", "BOX 207"), ("CAL RECORDS", "2024", "BOX 318"), ("RP SURVEYS", "2021", "BOX 154")]
    what, year, box = labels[n % 4]
    img, d = _canvas(180, (238, 236, 228), 256)
    d.rectangle([6, 6, 173, 249], outline=(40, 70, 130), width=5)
    d.rounded_rectangle([54, 24, 126, 46], radius=11, fill=(60, 56, 50))
    d.rectangle([20, 92, 160, 220], fill=(250, 250, 246), outline=(120, 120, 120), width=3)
    SGR._text(d, (90, 120), what, 22, (24, 24, 26), width=132)
    SGR._text(d, (90, 156), year, 28, (176, 30, 30), width=132)
    SGR._text(d, (90, 194), box, 22, (24, 24, 26), width=132)
    return _square(img)


def _carriage_label(v):
    """A carriage's label card (its holder 7.6 by 3.2): its number, what it holds."""
    img, d = _canvas(512, (250, 250, 246), 216)
    d.rectangle([0, 0, 511, 215], outline=(60, 60, 64), width=8)
    SGR._text(d, (64, 108), str(v + 1), 150, (30, 70, 140))
    d.line([(128, 24), (128, 192)], fill=(60, 60, 64), width=5)
    words = MS_LABELS[v].split(" - ") if " - " in MS_LABELS[v] else MS_LABELS[v].split(" ")
    for k, w in enumerate(words):
        SGR._text(d, (320, 108 + (k - (len(words) - 1) / 2) * 58), w, 56, (24, 24, 26), width=360)
    return _square(img)


def _fixed_label():
    img, d = _canvas(512, (250, 250, 246), 216)
    d.rectangle([0, 0, 511, 215], outline=(60, 60, 64), width=8)
    SGR._text(d, (256, 74), "REFERENCE", 70, (30, 70, 140), width=470)
    SGR._text(d, (256, 148), "LIBRARY", 70, (24, 24, 26), width=470)
    return _square(img)


def _end_face():
    img = PP._solid((206, 206, 200), 1141, 0.03).convert("RGBA").resize((256, 256))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 255], outline=(170, 172, 174), width=12)
    return img


def _planfile_card(k):
    """A plan file drawer's label card (4.8 by 1.2): the drawing series and what it is."""
    img, d = _canvas(512, (250, 250, 246), 128)
    d.rectangle([0, 0, 511, 127], outline=(80, 80, 84), width=6)
    code, rest = PF_SERIES[k].split("  ", 1)
    SGR._text(d, (96, 64), code, 60, (30, 70, 140), width=160)
    SGR._text(d, (340, 64), rest.replace("  ", " "), 44, (24, 24, 26), width=320)
    return _square(img)


def _drawing():
    """A P&ID sheet in its drawer: the border and title block, piping lines, valves and a vessel."""
    img, d = _canvas(512, (244, 244, 238))
    d.rectangle([12, 12, 499, 499], outline=(30, 30, 34), width=4)
    d.rectangle([330, 420, 499, 499], outline=(30, 30, 34), width=3)
    SGR._text(d, (414, 446), "M-201 SH 1", 22, (24, 24, 26))
    SGR._text(d, (414, 476), "REV 14", 18, (176, 30, 30))
    d.rounded_rectangle([60, 80, 140, 300], radius=36, outline=(24, 24, 26), width=5)
    for y in (140, 220, 340):
        d.line([(140, y), (460, y)], fill=(24, 24, 26), width=4)
    d.line([(300, 140), (300, 380)], fill=(24, 24, 26), width=4)
    for x, y in ((220, 140), (360, 220), (300, 300), (420, 340)):
        d.polygon([(x - 14, y - 10), (x + 14, y + 10), (x + 14, y - 10), (x - 14, y + 10)], outline=(24, 24, 26), fill=(244, 244, 238))
    d.ellipse([380, 100, 420, 140], outline=(24, 24, 26), width=4)
    return img


def _rail_floor():
    """The carpet tile with the shelving's floor rail let into it across the middle."""
    from pathlib import Path
    src = Path(__file__).resolve().parent.parent / "src/main/resources/assets/jugcraft/textures/block/carpet_tile.png"
    img = Image.open(src).convert("RGBA").resize((256, 256), Image.NEAREST) if src.exists() else \
        PP._solid((90, 96, 110), 1142, 0.05).convert("RGBA").resize((256, 256))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 112, 255, 143], fill=(120, 124, 128))
    d.rectangle([0, 120, 255, 135], fill=(176, 180, 184))
    d.line([(0, 112), (255, 112)], fill=(50, 52, 56), width=3)
    d.line([(0, 143), (255, 143)], fill=(50, 52, 56), width=3)
    return img


def _board_top():
    """The boardroom table's whole top (8 x 3 blocks over the texture's top 3/8): walnut, the racetrack outline's
    inlay band of darker wood a little in from the edge."""
    import nuclear_materials as M
    S2 = 1024
    a = M.base((104, 70, 46), S2)
    M.shade(a, M.streaks(S2, 1150, "h", count=900, amount=12))
    img = M.to_img(a).convert("RGBA")
    d = ImageDraw.Draw(img)
    k = S2 / 128            # texture px per table px
    L, Wd = 128, 48
    inset = 3.2
    r = Wd / 2 - inset
    # The band: a rounded rectangle 0.7 px wide.
    box = [inset * k, inset * k, (L - inset) * k, (Wd - inset) * k]
    d.rounded_rectangle(box, radius=r * k, outline=(54, 34, 22), width=int(0.7 * k))
    return img


def _cubby():
    import nuclear_materials as M
    img = M.brushed((190, 194, 198), 1151, "h", dirt=0.03).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    d.line([(0, 20), (127, 20)], fill=(110, 112, 116), width=3)
    d.rounded_rectangle([46, 52, 82, 76], radius=6, outline=(120, 124, 128), width=3)
    return img


def _pad():
    img, d = _canvas(128, (250, 250, 244))
    d.rectangle([0, 0, 127, 22], fill=(30, 70, 140))
    for y in range(36, 128, 12):
        d.line([(8, y), (120, y)], fill=(170, 190, 220), width=1)
    return img


def _mic_unit():
    img, d = _canvas(128, (30, 30, 32))
    d.ellipse([70, 34, 118, 94], fill=(60, 60, 64), outline=(120, 30, 26), width=6)
    for k in range(5):
        d.line([(12, 30 + k * 16), (54, 30 + k * 16)], fill=(14, 14, 16), width=5)
    return img


def _phone_top():
    img, d = _canvas(256, (30, 30, 32))
    d.ellipse([6, 6, 249, 249], fill=(36, 36, 40))
    for k in range(9):
        y = 40 + k * 20
        d.line([(50, y), (206, y)], fill=(20, 20, 22), width=7)
    d.rectangle([92, 196, 164, 226], fill=(70, 150, 200))
    return img


def _lectern_front():
    """The lectern's front (10.8 by 16.2): walnut, the plant's brass emblem high on it, UNIT 1 under."""
    import nuclear_materials as M
    a = M.base((104, 70, 46), 256)
    M.shade(a, M.streaks(256, 1152, "v", count=200, amount=12))
    img = M.to_img(a).convert("RGBA").resize((170, 256))
    d = ImageDraw.Draw(img)
    d.ellipse([45, 30, 125, 110], fill=(200, 164, 80), outline=(120, 90, 30), width=5)
    for rx, ry in ((30, 11), (11, 30)):
        d.ellipse([85 - rx, 70 - ry, 85 + rx, 70 + ry], outline=(60, 40, 16), width=4)
    d.ellipse([80, 65, 90, 75], fill=(60, 40, 16))
    SGR._text(d, (85, 140), "UNIT 1", 30, (220, 190, 110))
    return _square(img)


def _vest_front():
    img, d = _canvas(146, (196, 230, 40), 256)
    for y in (70, 150):
        d.rectangle([0, y, 145, y + 20], fill=(210, 212, 214))
    d.line([(73, 0), (73, 255)], fill=(150, 170, 30), width=4)
    SGR._text(d, (73, 222), "PLANT MGR", 22, (24, 24, 26), width=136)
    return _square(img)


def _hat_badge():
    img, d = _canvas(256, (250, 250, 246), 160)
    d.ellipse([88, 10, 168, 90], fill=(30, 70, 140))
    for rx, ry in ((30, 11), (11, 30)):
        d.ellipse([128 - rx, 50 - ry, 128 + rx, 50 + ry], outline=(250, 250, 246), width=4)
    SGR._text(d, (128, 128), "UNIT 1", 34, (24, 24, 26))
    return _square(img)


def _glass(alpha=60):
    img = Image.new("RGBA", (64, 64), (200, 220, 230, alpha))
    d = ImageDraw.Draw(img)
    for k in range(3):
        d.line([(10 + k * 6, 63), (40 + k * 6, 0)], fill=(240, 248, 252, alpha + 40), width=2)
    return img


def _lens():
    img, d = _canvas(64, (10, 10, 14))
    d.ellipse([12, 12, 52, 52], fill=(20, 30, 50))
    d.ellipse([20, 18, 30, 28], fill=(90, 110, 150))
    return img


def _counter_front():
    import nuclear_materials as M
    a = M.base((104, 70, 46), 256)
    M.shade(a, M.streaks(256, 1153, "v", count=200, amount=12))
    img = M.to_img(a).convert("RGBA")
    d = ImageDraw.Draw(img)
    d.rectangle([0, 200, 255, 255], fill=(60, 40, 26))
    return img


def _controlled():
    img, d = _canvas(256, (250, 250, 244))
    for y in range(30, 256, 16):
        d.line([(20, y), (236, y)], fill=(160, 160, 160), width=2)
    d.rectangle([40, 96, 216, 160], outline=(196, 28, 24), width=6)
    SGR._text(d, (128, 116), "CONTROLLED", 30, (196, 28, 24), width=170)
    SGR._text(d, (128, 144), "COPY", 26, (196, 28, 24), width=170)
    return img


def _tray_label(word):
    img, d = _canvas(256, (60, 62, 66), 55)
    d.rectangle([78, 6, 178, 48], fill=(250, 250, 246))
    SGR._text(d, (128, 27), word, 34, (24, 24, 26))
    return _square(img)


def _signout():
    img, d = _canvas(256, (250, 250, 244), 177)
    SGR._text(d, (128, 18), "DOCUMENT SIGN-OUT LOG", 20, (24, 24, 26), width=230)
    for y in range(36, 177, 18):
        d.line([(10, y), (246, y)], fill=(120, 120, 120), width=2)
    for x in (70, 170):
        d.line([(x, 36), (x, 172)], fill=(120, 120, 120), width=2)
    for k in range(5):
        d.line([(16, 50 + k * 18), (60, 44 + k * 18)], fill=(30, 40, 90), width=3)
        d.line([(80, 50 + k * 18), (150, 46 + k * 18)], fill=(30, 40, 90), width=3)
    return _square(img)


def makers():
    solid = PP._solid
    import nuclear_materials as M
    out = {
        "pi_gs_plate": lambda: M.brushed((96, 100, 104), 1160, "h", dirt=0.05), "pi_gs_dewar": lambda: M.brushed((200, 204, 208), 1161, "v", dirt=0.03),
        "pi_gs_frost": lambda: solid((236, 240, 244), 1162, 0.02), "pi_gs_cave": lambda: solid((92, 104, 120), 1163, 0.04),
        "pi_gs_cave_lid": lambda: solid((80, 90, 104), 1164, 0.04), "pi_gs_lid_top": lambda: solid((80, 90, 104), 1165, 0.06),
        "pi_gs_cable": lambda: solid((24, 24, 26), 1166, 0.02), "pi_gs_beaker": lambda: solid((236, 236, 230), 1167, 0.02),
        "pi_gs_nim": lambda: solid((60, 64, 70), 1168, 0.04),
        "pi_lab_shower": lambda: solid((236, 196, 32), 1169, 0.03), "pi_lab_push": _yellow_push, "pi_lab_shower_sign": _shower_sign,
        "pi_cab_flam": lambda: solid((236, 196, 32), 1170, 0.04), "pi_cab_acid": lambda: solid((34, 84, 160), 1171, 0.04),
        "pi_cab_flam_side": lambda: _cab_side("flam"), "pi_cab_acid_side": lambda: _cab_side("acid"),
        "pi_cab_sump": lambda: M.brushed((150, 154, 156), 1172, "h", dirt=0.08), "pi_cab_can_red": _can_red,
        "pi_cab_can_top": lambda: solid((176, 28, 24), 1173, 0.05), "pi_cab_cap_red": lambda: solid((196, 30, 26), 1174, 0.02),
        "pi_cab_cap_blue": lambda: solid((30, 70, 160), 1175, 0.02), "pi_cab_cap_white": lambda: solid((236, 236, 230), 1176, 0.02),
        "pi_cab_glass_brown": lambda: solid((110, 60, 20), 1133, 0.02), "pi_cab_glass_clear": lambda: _glass(150),
        "pi_cab_hdpe": lambda: solid((236, 236, 228), 1134, 0.03), "pi_cab_jug_plain": lambda: solid((236, 236, 228), 1135, 0.03),
        "pi_cab_can_red_plain": lambda: solid((196, 30, 26), 1136, 0.05),
        "pi_cab_jug": lambda: _chem_label("ISOPROPANOL", "IPA  4 L", (236, 236, 228)),
        "pi_cab_label_acetone": lambda: _chem_label("ACETONE", "HPLC GRADE"), "pi_cab_label_methanol": lambda: _chem_label("METHANOL", "HPLC GRADE"),
        "pi_cab_label_hcl": lambda: _chem_label("HCl", "HYDROCHLORIC 37%"), "pi_cab_label_hno3": lambda: _chem_label("HNO3", "NITRIC ACID 70%"),
        "pi_cab_label_naoh": lambda: _chem_label("NaOH", "SODIUM HYDROXIDE"),
        "pi_cab_tub": lambda: solid((200, 202, 196), 1177, 0.05), "pi_cab_tub_in": lambda: solid((180, 182, 178), 1178, 0.08),
        "pi_doc_base": lambda: solid((150, 152, 150), 1180, 0.05), "pi_doc_shelf": lambda: solid((214, 210, 198), 1181, 0.04),
        "pi_doc_shelf_edge": lambda: solid((190, 186, 176), 1182, 0.04), "pi_doc_spine": lambda: solid((200, 196, 186), 1183, 0.04),
        "pi_doc_upright": lambda: solid((196, 192, 182), 1184, 0.05), "pi_doc_end": lambda: solid((206, 206, 200), 1185, 0.03),
        "pi_doc_end_face": _end_face, "pi_doc_cap": lambda: solid((196, 196, 190), 1186, 0.03),
        "pi_doc_wheel": lambda: solid((34, 34, 36), 1187, 0.02),
        "pi_doc_binder_side": lambda: solid((40, 60, 120), 1188, 0.05), "pi_doc_binder_top": lambda: solid((236, 234, 226), 1189, 0.06),
        "pi_doc_box_side": lambda: solid((238, 236, 228), 1190, 0.04), "pi_doc_box_lid": lambda: solid((232, 230, 220), 1191, 0.05),
        "pi_doc_planfile": lambda: solid((196, 198, 196), 1192, 0.04), "pi_doc_planfile_top": lambda: solid((214, 212, 204), 1193, 0.03),
        "pi_doc_planfile_drawer": lambda: solid((204, 206, 204), 1194, 0.03), "pi_doc_drawing": _drawing, "pi_doc_rail_floor": _rail_floor,
        "pi_doc_counter_front": _counter_front, "pi_doc_inkpad": lambda: solid((40, 40, 60), 1195, 0.03),
        "pi_doc_stamp_base": lambda: solid((60, 60, 64), 1196, 0.03), "pi_doc_stamp_red": lambda: solid((176, 30, 26), 1197, 0.03),
        "pi_doc_controlled": _controlled, "pi_doc_tray_out": lambda: _tray_label("OUT"), "pi_doc_tray_in": lambda: _tray_label("IN"),
        "pi_doc_signout": _signout,
        "pi_board_top": _board_top, "pi_board_cubby": _cubby, "pi_board_pad": _pad, "pi_board_pen": lambda: M.brushed((190, 194, 198), 1154, "h", dirt=0.02),
        "pi_board_mic_unit": _mic_unit, "pi_board_mic_ring": lambda: solid((110, 26, 22), 1155, 0.02), "pi_board_phone": _phone_top,
        "pi_board_glass": lambda: _glass(70), "pi_board_lens": _lens, "pi_board_lectern_front": _lectern_front,
        "pi_mgr_red": lambda: solid((176, 28, 26), 1156, 0.04), "pi_mgr_lamp": lambda: solid((210, 40, 30), 1157, 0.02),
        "pi_mgr_glass": lambda: _glass(36), "pi_mgr_brass": lambda: M.brushed((200, 164, 80), 1158, "h", dirt=0.03),
        "pi_mgr_vest": lambda: solid((196, 230, 40), 1159, 0.04), "pi_mgr_vest_front": _vest_front,
        "pi_mgr_hat": lambda: solid((246, 246, 240), 1145, 0.02), "pi_mgr_hat_badge": _hat_badge,
        "pi_mgr_glasses": lambda: _glass(120),
    }
    for n in range(3):
        out[f"pi_doc_binders_{n}"] = lambda n=n: _binders(n)
    for n in range(4):
        out[f"pi_doc_box_{n}"] = lambda n=n: _record_box(n)
    for v in range(len(MS_LABELS)):
        out[f"pi_doc_label_{v}"] = lambda v=v: _carriage_label(v)
    out["pi_doc_label"] = lambda: _carriage_label(0)     # the carriages' base model's (each label's model gives its own)
    out["pi_doc_label_fixed"] = _fixed_label
    for k in range(PF_DRAWERS):
        out[f"pi_doc_planfile_card_{k}"] = lambda k=k: _planfile_card(k)
    return out


SHARP = {"pi_lab_push", "pi_lab_shower_sign", "pi_cab_flam_side", "pi_cab_acid_side", "pi_cab_can_red", "pi_cab_jug",
         "pi_cab_label_acetone", "pi_cab_label_methanol", "pi_cab_label_hcl", "pi_cab_label_hno3", "pi_cab_label_naoh", "pi_doc_drawing", "pi_doc_controlled",
         "pi_doc_tray_out", "pi_doc_tray_in", "pi_doc_signout", "pi_board_top", "pi_board_pad", "pi_board_phone", "pi_board_lectern_front",
         "pi_mgr_vest_front", "pi_mgr_hat_badge", "pi_doc_rail_floor"} | \
    {f"pi_doc_binders_{n}" for n in range(3)} | {f"pi_doc_box_{n}" for n in range(4)} | {f"pi_doc_label_{v}" for v in range(len(MS_LABELS))} | \
    {f"pi_doc_planfile_card_{k}" for k in range(PF_DRAWERS)} | {"pi_doc_label", "pi_doc_label_fixed"}
SEE_THROUGH = {"pi_board_glass", "pi_mgr_glass", "pi_mgr_glasses", "pi_cab_glass_clear"}


# ================================================================================================ registration
PANELS = {CSB_KEY: {"display": "Plant Chemistry Status Board", "width": 3, "item3d": True},
          GS_KEY: {"display": "Gamma Spectroscopy Station", "width": 3, "item3d": True},
          "chem_flam_cabinet": {"display": CAB_KEYS["chem_flam_cabinet"][0], "width": 1, "item3d": True, "swing": True},
          "chem_acid_cabinet": {"display": CAB_KEYS["chem_acid_cabinet"][0], "width": 1, "item3d": True, "swing": True},
          AV_KEY: {"display": "AV Credenza", "width": 3, "rows": 1, "item3d": True},
          HL_KEY: {"display": "Control Room Direct Line", "width": 1, "rows": 1, "item3d": True},
          LIC_KEY: {"display": "Framed Operating License", "width": 2, "rows": 1, "item3d": True},
          AW_KEY: {"display": "Awards Wall", "width": 3, "rows": 1, "item3d": True}}
DRAWINGS = {CSB_KEY: chem_status_board, GS_KEY: gamma_spec, "chem_flam_cabinet": lambda label=None: cabinet_drawing("chem_flam_cabinet"),
            "chem_acid_cabinet": lambda label=None: cabinet_drawing("chem_acid_cabinet"), AV_KEY: av_credenza, HL_KEY: hotline,
            LIC_KEY: license_drawing, AW_KEY: awards}
BOXES = {CSB_KEY: chem_status_board_boxes, GS_KEY: gamma_spec_boxes,
         "chem_flam_cabinet": lambda open_=False: cabinet_boxes("chem_flam_cabinet", open_),
         "chem_acid_cabinet": lambda open_=False: cabinet_boxes("chem_acid_cabinet", open_),
         AV_KEY: av_credenza_boxes, HL_KEY: hotline_boxes, LIC_KEY: license_boxes, AW_KEY: awards_boxes}
NOT_LIVE = {"chem_flam_cabinet", "chem_acid_cabinet", AV_KEY, HL_KEY, LIC_KEY, AW_KEY}
PIECES = {"board_lectern": {"display": "Lectern", "boxes": lectern()},
          "mgr_ppe_hooks": {"display": "Hard Hat Hooks", "boxes": ppe_hooks()}}
MULTI = {"lab_safety_shower": {"display": "Emergency Shower and Eyewash", "multi": (1, 2, "frame"), "parts": shower_part, "item3d": True},
         "board_table": {"display": "Boardroom Table", "multi": (BT_W, BT_D, False), "parts": board_table_part,
                         "item_boxes": board_table_item()},
         }
ROWS = {"doc_issue_counter": issue_counter()}
SCREENS = {"perf_screen": {"display": "Unit Performance Screen", "multi": (3, 2, True), "screen": "status", "item3d": True}}

STEEL = {"P": "#c:plates/steel", "C": "jugcraft:basic_circuit"}
CRAFTING = {
    CSB_KEY: (["GGG", "PCP"], {**STEEL, "G": "minecraft:black_stained_glass_pane"}, 1),
    GS_KEY: (["LCL", "PBP", "PPP"], {**STEEL, "L": "minecraft:iron_block", "B": "minecraft:blue_ice"}, 1),
    "chem_flam_cabinet": (["PYP", "P P", "PYP"], {"P": "#c:plates/steel", "Y": "minecraft:yellow_dye"}, 1),
    "chem_acid_cabinet": (["PBP", "P P", "PBP"], {"P": "#c:plates/steel", "B": "minecraft:blue_dye"}, 1),
    "lab_safety_shower": (["YYY", " P ", " P "], {"Y": "minecraft:yellow_dye", "P": "minecraft:copper_ingot"}, 1),
    "board_table": (["WWW", "WWW", "S S"], {"W": "minecraft:dark_oak_planks", "S": "#c:plates/steel"}, 1),
    AV_KEY: (["WCW", "WWW"], {"W": "minecraft:dark_oak_planks", "C": "jugcraft:basic_circuit"}, 1),
    HL_KEY: (["RCR", "R R"], {"R": "minecraft:red_dye", "C": "jugcraft:basic_circuit"}, 1),
    LIC_KEY: (["WPW"], {"W": "minecraft:dark_oak_planks", "P": "minecraft:paper"}, 1),
    AW_KEY: (["WGW"], {"W": "minecraft:dark_oak_planks", "G": "minecraft:gold_nugget"}, 1),
    "board_lectern": (["WWW", " W ", "WWW"], {"W": "minecraft:dark_oak_planks"}, 1),
    "mgr_ppe_hooks": (["WIW"], {"W": "minecraft:dark_oak_planks", "I": "minecraft:iron_nugget"}, 1),
    "doc_issue_counter": (["QQQ", "WWW"], {"Q": "minecraft:quartz_slab", "W": "minecraft:dark_oak_planks"}, 1),
    "perf_screen": (["GGG", "GCG", " R "], {"G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit", "R": "minecraft:comparator"}, 1),
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
    PP.RISERS.update(MULTI)
    PP.RISERS.update(ROWS)
    PP.RISERS.update(SCREENS)
    PP.CRAFTING.update(CRAFTING)
    PP.CRAFTING.update(CUSTOM_CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.NOT_LIVE |= NOT_LIVE
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



# ================================================================================================ the pieces with their own states
# The mobile shelving's carriages (nuclear/MobileShelvingBlock.java: facing, seg, half, slide, variant), the plan file
# (nuclear/PlanFileBlock.java: facing, part, drawer) and the floor rail (axis): their models and blockstates here.
RAIL_KEY = "doc_rail_floor"
BLOCKS = [MS_KEY, FS_KEY, PF_KEY, RAIL_KEY]
NAMES = {MS_KEY: "Mobile Shelving Carriage", FS_KEY: "Fixed Shelving", PF_KEY: "Plan File", RAIL_KEY: "Mobile Shelving Floor Rail"}
CUSTOM_CRAFTING = {
    FS_KEY: (["PBP", "PBP"], {"P": "#c:plates/steel", "B": "minecraft:book"}, 2),
    MS_KEY: (["PBP", "PBP", "IWI"], {"P": "#c:plates/steel", "B": "minecraft:book", "I": "minecraft:iron_ingot", "W": "minecraft:rail"}, 2),
    PF_KEY: (["PPP", "PCP", "PPP"], {"P": "#c:plates/steel", "C": "minecraft:paper"}, 1),
    RAIL_KEY: (["CRC"], {"C": "jugcraft:carpet_tile", "R": "minecraft:rail"}, 3),
}
TURN = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def _y(y):
    return {"y": y} if y else {}


def write_assets(write, rid, assets, lang):
    import plant_interior as PI
    models = assets / "models" / "block"

    def mdl(name, boxes):
        write(models / f"{name}.json", PI.model(boxes, [], rid))

    for block, name in NAMES.items():
        lang[f"block.jugcraft.{block}"] = name
    # The carriages: a model for each block of a carriage at each slide; the handwheel end's upper block once more for
    # each label (its card).
    parts = []
    for seg in range(MS_SEGS):
        for half in (0, 1):
            for slide in range(7):
                name = f"{MS_KEY}_{seg}_{half}_{slide}"
                mdl(name, shelving_cell(seg, half, slide))
                labelled = seg == 0 and half == 1
                if labelled:
                    for v in range(len(MS_LABELS)):
                        write(models / f"{name}_v{v}.json", {"parent": rid(f"block/{name}"), "textures": {"pi_doc_label": rid(f"block/pi_doc_label_{v}")}})
                for face, y in TURN:
                    when = {"facing": face, "seg": str(seg), "half": "lower" if half == 0 else "upper", "slide": str(slide)}
                    if labelled:
                        for v in range(len(MS_LABELS)):
                            parts.append({"when": dict(when, variant=str(v)), "apply": {"model": rid(f"block/{name}_v{v}"), **_y(y)}})
                    else:
                        parts.append({"when": when, "apply": {"model": rid(f"block/{name}"), **_y(y)}})
    write(assets / "blockstates" / f"{MS_KEY}.json", {"multipart": parts})
    mdl(f"{MS_KEY}_inventory", shelving_cell(1, 0))
    # The fixed units at the bank's ends: the carriages' build without wheels or handwheels.
    variants = {}
    for seg in range(MS_SEGS):
        for half in (0, 1):
            mdl(f"{FS_KEY}_{seg}_{half}", shelving_cell(seg, half, fixed=True))
            for face, y in TURN:
                variants[f"facing={face},half={'lower' if half == 0 else 'upper'},seg={seg}"] = {"model": rid(f"block/{FS_KEY}_{seg}_{half}"), **_y(y)}
    write(assets / "blockstates" / f"{FS_KEY}.json", {"variants": variants})
    write(assets / "items" / f"{FS_KEY}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{FS_KEY}_1_0")}})
    write(assets / "items" / f"{MS_KEY}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{MS_KEY}_inventory")}})
    # The plan file: each block with each drawer out (0: all shut).
    variants = {}
    for drawer in range(PF_DRAWERS + 1):
        for part in (0, 1):
            mdl(f"{PF_KEY}_{drawer}_{part}", plan_file_part(part, 0, drawer))
            for face, y in TURN:
                variants[f"drawer={drawer},facing={face},part={part}"] = {"model": rid(f"block/{PF_KEY}_{drawer}_{part}"), **_y(y)}
    write(assets / "blockstates" / f"{PF_KEY}.json", {"variants": variants})
    mdl(f"{PF_KEY}_inventory", [b for part in (0, 1) for b in
                                [(bx[0] + 16 * part,) + tuple(bx[1:3]) + (bx[3] + 16 * part,) + tuple(bx[4:]) for bx in plan_file_part(part, 0, 0)]])
    write(assets / "items" / f"{PF_KEY}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{PF_KEY}_inventory")}})
    # The floor rail: the carpet tile with the rail across it (along x, or turned along z).
    write(models / f"{RAIL_KEY}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": rid("block/pi_doc_rail_floor"), "side": rid("block/carpet_tile"), "bottom": rid("block/carpet_tile")}})
    write(assets / "blockstates" / f"{RAIL_KEY}.json", {"variants": {
        "axis=x": {"model": rid(f"block/{RAIL_KEY}")}, "axis=y": {"model": rid(f"block/{RAIL_KEY}")},
        "axis=z": {"model": rid(f"block/{RAIL_KEY}"), "y": 90}}})
    write(assets / "items" / f"{RAIL_KEY}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{RAIL_KEY}")}})


register()
