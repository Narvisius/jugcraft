"""The control building's battery rooms brought up to the detail standard (older rooms round 4; Justin, 16 October 2026:
"Battery rooms (Recommended)", the rest left to me: the racks' spill containment, cell numbers and pilot cell,
terminal covers and labels; a disconnect switch for each string that works and cells you can read; the room's
vapor-tight lights, emergency light and smoke detector, the acid-proof floor and drain, PPE at the door, the signs and
the cell log).

- The station battery (plant_panels station_battery, its boxes added to here): labelled by train, string and rack
  (LABELS: 1A string 1 cells 1-30 ...), its drawing (pi_station_battery_f) lettering the nameplate on its lower rail,
  each cell's number on its cover, the pilot cell's tag, the spill pan's lip; absorbent pillows in the pan, the pilot
  cell's float thermometer, insulating boots over the string's end terminals.
- The battery disconnect (battery_disconnect): a fused 125 VDC disconnect switch on a strut frame, one for each string
  (1A-1 .. 1B-2), its handle live (ON up, OFF left: nuclear/BatteryDisconnectBlock.java), its ON / OFF flag, the fuses
  behind its window; its conduit pair up into the DC tray (battery_disconnect_riser).
- The room: the vapor-tight light (in place of the ceiling panels), the PPE cabinet, the cell log board, the acid
  neutralization sump, the room's sign.

Model pixels for the piece facing north (its back to the north wall, z 0; its front out at +z), x across to the right
as you face it, y up from the floor.
"""
from PIL import Image, ImageDraw

import plant_panels as PP
import relay_room as RR
import switchgear_room as SGR
import dc_room as DR

U = PP.U
WHITE, BLACK, INK = (238, 238, 232, 255), (24, 24, 26, 255), (24, 24, 26, 255)
RED = (190, 28, 24, 255)

# ------------------------------------------------------------------------------------------------ the station battery

SB_KEY = "station_battery"
# Train, string, rack: the first rack of a string holds cells 1-30, the second 31-60.
LABELS = [f"1{t} STR {s} CELLS {h * 30 + 1}-{h * 30 + 30}" for t in "AB" for s in (1, 2) for h in (0, 1)]
PILOT = 7                     # the pilot cell: the upper step's eighth from the left (cell 8, or 38)
LABEL_X = (1.55, 0.2)         # a cell's number label on its cover's front: from its left side, short of its right


def label_parts(label):
    """(train letter, string, first cell) of a rack's label."""
    t = label[1]
    s = int(label.split("STR ")[1].split()[0])
    first = int(label.split("CELLS ")[1].split("-")[0])
    return t, s, first


def cell_number(step, k, first):
    """A cell's number (first: 1 or 31): the string runs from the positive terminal, the upper step's right end, along
    the upper step to the left, down the jumper and along the lower step to the right (the negative terminal)."""
    return first + (14 - k if step == "high" else 15 + k)


def battery_drawing(label=LABELS[0]):
    """The rack's lettering (pi_station_battery_f, 4 blocks by 2): the nameplate on the lower step's front rail, each
    cell's number, the pilot cell's tag, the spill pan's lip."""
    f = PP.Front(4, 811)
    # Only the lettering is ever shown, so the rest is left flat (a mottled sheet would be eight times the file).
    f.R(0, 0, f.W, 32, fill=PP.PAINT + (255,))
    t, s, first = label_parts(label)
    # The nameplate (where plant_panels SB_TAG is): engraved white, the battery, string and cells; the warning in red.
    x0, y0, x1, y1 = PP.SB_TAG
    f.R(x0, y0, x1, y1, fill=(238, 238, 232, 255), outline=(60, 60, 60, 255), width=3)
    RR._fit(f, (x0 + x1) / 2, y1 - 0.42, f"125 VDC STATION BATTERY 1{t}  STRING {s}", 7, x1 - x0 - 0.4, fill=INK)
    RR._fit(f, (x0 + x1) / 2, y0 + 0.4, f"CELLS {first}-{first + 29} OF 60   NO SMOKING", 7, x1 - x0 - 0.4, fill=RED)
    # Each cell's number on its cover.
    for step, (z0, z1, cy0, cy1) in (("low", PP.SB_LOW), ("high", PP.SB_HIGH)):
        for k in range(PP.SB_CELLS):
            cx0 = PP.SB_X0 + k * PP.SB_PITCH
            lx0, lx1 = cx0 + LABEL_X[0], cx0 + PP.SB_W - LABEL_X[1]
            ly0, ly1 = cy1 + 0.1, cy1 + PP.SB_COVER - 0.1
            f.R(lx0, ly0, lx1, ly1, fill=(240, 240, 234, 255))
            f.T((lx0 + lx1) / 2, (ly0 + ly1) / 2, str(cell_number(step, k, first)), 7, fill=INK, bold=True)
    # The pilot cell's tag, hung from the upper step's front restraint rail.
    px0, py0, px1, py1 = pilot_tag()
    f.R(px0, py0, px1, py1, fill=(246, 200, 36, 255), outline=(40, 40, 42, 255), width=2)
    f.T((px0 + px1) / 2, py1 - 0.5, "PILOT", 7, fill=INK, bold=True)
    f.T((px0 + px1) / 2, py1 - 1.15, "CELL", 7, fill=INK, bold=True)
    f.T((px0 + px1) / 2, py0 + 0.45, f"#{cell_number('high', PILOT, first)}", 6.5, fill=INK, bold=True)
    # The spill pan's lip (its front, y 1 to 2): its legend at the left, the shock warning at the right.
    f.R(0.3, 1.0, 63.7, 2.0, fill=(52, 54, 56, 255))
    f.R(4.0, 1.12, 26.0, 1.88, fill=(246, 200, 36, 255))
    RR._fit(f, 15.0, 1.5, "ACID SPILL CONTAINMENT - KEEP ABSORBENT IN PLACE", 6.5, 21.6, fill=INK)
    f.R(40.0, 1.12, 60.0, 1.88, fill=(250, 250, 246, 255))
    f.R(40.0, 1.12, 45.4, 1.88, fill=(206, 30, 32, 255))
    f.T(42.7, 1.5, "DANGER", 6.5, fill=WHITE, bold=True)
    RR._fit(f, 52.7, 1.5, "125 VDC - SHOCK AND ARC FLASH", 6.5, 14.2, fill=INK)
    return f


def pilot_tag():
    """The pilot cell's tag (x0, y0, x1, y1 on the front), under the upper step's front restraint rail."""
    z0, z1, y0, y1 = PP.SB_HIGH
    cx = PP.SB_X0 + PILOT * PP.SB_PITCH + PP.SB_W / 2
    ry = y0 + 6.4
    return cx - 1.1, ry - 2.6, cx + 1.1, ry - 0.2


def station_battery_boxes():
    """The rack as plant_panels draws it, its nameplate lettered from the drawing, with what this round adds."""
    F = f"pi_{SB_KEY}_f"
    face = {"*": "pi_pnl_white", "south": F}
    out = []
    for b in PP.station_battery():
        t = b[6]
        if isinstance(t, dict) and isinstance(t.get("south"), tuple) and t["south"][0] == "pi_sb_tag":
            b = b[:6] + (face,) + tuple(b[7:])
        elif b[:6] == (0.3, 1.0, 15.3, 63.7, 2.0, 15.7):
            # The pan's front lip, lettered.
            b = b[:6] + ({"*": "pi_sb_pan", "south": F},)
        out.append(b)
    # Each cell's number label on its cover's front.
    for z0, z1, y0, y1 in (PP.SB_LOW, PP.SB_HIGH):
        for k in range(PP.SB_CELLS):
            cx0 = PP.SB_X0 + k * PP.SB_PITCH
            out.append((cx0 + LABEL_X[0], y1 + 0.1, z1 + 0.05, cx0 + PP.SB_W - LABEL_X[1], y1 + PP.SB_COVER - 0.1, z1 + 0.13, face))
    # The absorbent pillows lying in the pan along its front, either side of the middle upright.
    for x0, x1 in ((3.0, 30.4), (33.6, 61.0)):
        out += [(x0, 1.0, 13.95, x1, 1.85, 15.1, "pi_br_pillow"), (x0 + 0.4, 1.85, 14.2, x1 - 0.4, 2.0, 14.85, "pi_br_pillow")]
    # The pilot cell: a float thermometer down through its vent, its tag on the upper step's front rail.
    z0, z1, y0, y1 = PP.SB_HIGH
    cx0 = PP.SB_X0 + PILOT * PP.SB_PITCH
    xm, zc, top = cx0 + PP.SB_W / 2, (z0 + z1) / 2, y1 + PP.SB_COVER
    out += [(xm - 0.15, top + 1.15, zc - 1.35, xm + 0.15, top + 4.2, zc - 1.05, "pi_br_thermo"),
            (xm - 0.25, top + 4.2, zc - 1.45, xm + 0.25, top + 4.6, zc - 0.95, "pi_cap_black")]
    px0, py0, px1, py1 = pilot_tag()
    ry = y0 + 6.4
    out += [((px0 + px1) / 2 - 0.08, py1, z1 + 0.62, (px0 + px1) / 2 + 0.08, ry + 0.3, z1 + 0.7, "pi_steel"),
            (px0, py0, z1 + 0.62, px1, py1, z1 + 0.72, {"*": "pi_pnl_yellow", "south": F})]
    # The string's end terminals: insulating boots over the lugs, red on the positive, black on the negative.
    last = PP.SB_X0 + (PP.SB_CELLS - 1) * PP.SB_PITCH + PP.SB_W - 0.95
    lz0, lz1, ly0, ly1 = PP.SB_LOW
    ht, lt = y1 + PP.SB_COVER, ly1 + PP.SB_COVER
    for yb, zc_, tex in ((ht, (z0 + z1) / 2, "pi_br_boot_red"), (lt, (lz0 + lz1) / 2, "pi_br_boot_black")):
        out.append((last - 0.75, yb + 0.15, zc_ - 0.75, last + 0.75, yb + 1.4, zc_ + 0.75, tex))
    return out


# ------------------------------------------------------------------------------------------------ the disconnect

BD_KEY = "battery_disconnect"
BD_LABELS = ["1A-1", "1A-2", "1B-1", "1B-2"]
BD_D = 7.8                                     # the door's face
BD_BOX = (2.2, 9.0, 13.8, 27.0)                # the enclosure: x0, y0, x1, y1
BD_DOOR = (2.4, 9.2, 13.6, 26.8)
BD_WIN = (3.2, 18.4, 8.2, 23.6)                # the fuse window in the door
BD_HUB = (9.6, 14.4)                           # the handle's hub (live: ON up, OFF turned left)
BD_FLAG = (10.3, 18.55, 11.5, 19.25)           # the ON / OFF flag in its window
BD_CONDUITS = ((5.7, 7.1), (8.9, 10.3))        # the conduit pair out of its top (x), at z ...
BD_CZ = (5.0, 6.4)


def disconnect(label="1A-1"):
    """The disconnect's door, drawn: its nameplate, the fuse window's frame, the ratings, ON and OFF, the DANGER label."""
    f = PP.Front(1, 812)
    t, n = label[1], label[-1]
    x0, y0, x1, y1 = BD_DOOR
    f.R(x0, y0, x1, y1, fill=(170, 174, 176, 255), outline=(110, 112, 114, 255), width=3)
    # The nameplate: black, engraved white.
    f.R(3.0, 24.6, 13.0, 26.3, fill=BLACK, outline=(170, 172, 174, 255), width=2)
    RR._fit(f, 8.0, 25.85, f"BATTERY 1{t}  STRING {n}", 8, 9.6, fill=WHITE)
    RR._fit(f, 8.0, 25.05, "DISCONNECT SWITCH", 7, 9.6, fill=WHITE)
    # The fuse window's frame (the glass and the fuses are real).
    wx0, wy0, wx1, wy1 = BD_WIN
    f.R(wx0 - 0.3, wy0 - 0.3, wx1 + 0.3, wy1 + 0.3, fill=(40, 40, 42, 255))
    f.T((wx0 + wx1) / 2, wy0 - 0.75, "FUSES", 6.5, fill=INK, bold=True)
    # The ratings.
    for k, s in enumerate(("125 VDC", "400 A", "2 POLE", "FUSED")):
        f.T(10.9, 23.0 - k * 0.9, s, 7 if k < 2 else 6.5, fill=INK, bold=True)
    # The flag's window frame, the handle's legends.
    fx0, fy0, fx1, fy1 = BD_FLAG
    f.R(fx0 - 0.2, fy0 - 0.2, fx1 + 0.2, fy1 + 0.2, fill=(30, 30, 32, 255))
    hx, hy = BD_HUB
    f.T(11.6, 17.0, "ON", 7.5, fill=INK, bold=True)
    f.T(6.4, 13.0, "OFF", 7.5, fill=INK, bold=True)
    f.E(hx, hy, 1.3, fill=(236, 196, 30, 255), outline=(40, 40, 42, 255), width=2)
    # The DANGER label at the foot of the door.
    DR._label_danger(f, 3.0, 9.6, 13.0, 12.0, "125 VOLTS DC", "OPEN ONLY WITH CHARGERS OFF")
    # Its live parts: the handle (turned about its hub), the ON (red) and OFF (green) flags.
    import diesel_panels
    D = BD_D
    lever = [(hx - 0.4, hy, D + 0.25, hx + 0.4, hy + 3.2, D + 0.65, "pi_pnl_red"),
             (hx - 0.6, hy + 2.4, D + 0.25, hx + 0.6, hy + 3.5, D + 0.85, "pi_pnl_red")]
    hub = [b[:6] + ("pi_pnl_black",) for b in PP._sc_round(hx, hy, D + 0.15, D + 0.6, 0.6, None)]
    diesel_panels.part(f, "bd_handle", hx, hy, hub + lever, angles=[0, 90])
    flag = [fx0, fy0, D + 0.05, fx1, fy1, D + 0.12]
    f.live.append({"t": "toggle", "on": flag, "off": [], "colour": [200, 40, 34], "ch": "bd_on"})
    f.live.append({"t": "toggle", "on": flag, "off": [], "colour": [40, 170, 70], "ch": "bd_off"})
    return f


def disconnect_boxes():
    """The whole disconnect (one block wide, two high): the strut frame from the floor, the enclosure, its door round the
    fuse window, the glass, the fuses behind it, the hinges, the handle's yellow plate, the conduit pair out of its top."""
    F = f"pi_{BD_KEY}_f"
    face = {"*": "pi_br_disc", "south": F}
    D = BD_D
    b = []
    # The strut frame: two channels from the floor up behind the box, their foot plates, a cross channel top and bottom.
    for x0 in (3.0, 11.4):
        b += [(x0, 0.0, 0.0, x0 + 1.6, 30.4, 1.6, "pi_br_strut"), (x0 - 0.6, 0.0, 0.0, x0 + 2.2, 0.4, 2.8, "pi_br_strut")]
    for y0 in (8.0, 27.6):
        b.append((2.0, y0, 0.0, 14.0, y0 + 0.8, 1.6, "pi_br_strut"))
    # The enclosure: its back pan, its sides out to the door, the door in four pieces round the window.
    bx0, by0, bx1, by1 = BD_BOX
    b += [(bx0, by0, 1.6, bx1, by1, 5.6, "pi_br_disc"),
          (bx0, by0, 5.6, bx0 + 0.3, by1, 7.4, "pi_br_disc"), (bx1 - 0.3, by0, 5.6, bx1, by1, 7.4, "pi_br_disc"),
          (bx0 + 0.3, by0, 5.6, bx1 - 0.3, by0 + 0.3, 7.4, "pi_br_disc"), (bx0 + 0.3, by1 - 0.3, 5.6, bx1 - 0.3, by1, 7.4, "pi_br_disc")]
    dx0, dy0, dx1, dy1 = BD_DOOR
    wx0, wy0, wx1, wy1 = BD_WIN
    b += [(dx0, dy0, 7.4, wx0, dy1, D, face), (wx1, dy0, 7.4, dx1, dy1, D, face),
          (wx0, dy0, 7.4, wx1, wy0, D, face), (wx0, wy1, 7.4, wx1, dy1, D, face)]
    # The window's frame proud of the door, the glass in it.
    b += [(wx0 - 0.3, wy0 - 0.3, D, wx1 + 0.3, wy0, D + 0.2, "pi_cap_black"), (wx0 - 0.3, wy1, D, wx1 + 0.3, wy1 + 0.3, D + 0.2, "pi_cap_black"),
          (wx0 - 0.3, wy0, D, wx0, wy1, D + 0.2, "pi_cap_black"), (wx1, wy0, D, wx1 + 0.3, wy1, D + 0.2, "pi_cap_black"),
          (wx0, wy0, 7.55, wx1, wy1, 7.6, {"*": "pi_dc_glass", "south": "pi_dc_glass", "north": "pi_dc_glass"})]
    # Behind the glass: the fuse base (black), two fuses (tan bodies, copper blades) in their clips.
    b.append((wx0 + 0.2, wy0 + 0.2, 5.6, wx1 - 0.2, wy1 - 0.2, 5.9, "pi_cap_black"))
    for fy in (wy0 + 1.4, wy1 - 1.4):
        b += [(wx0 + 1.1, fy - 0.6, 5.9, wx1 - 1.1, fy + 0.6, 7.1, "pi_br_fuse"),
              (wx0 + 0.5, fy - 0.35, 6.2, wx0 + 1.1, fy + 0.35, 6.8, "pi_sb_copper"),
              (wx1 - 1.1, fy - 0.35, 6.2, wx1 - 0.5, fy + 0.35, 6.8, "pi_sb_copper"),
              (wx0 + 0.4, fy - 0.5, 5.9, wx0 + 0.9, fy + 0.5, 6.4, "pi_steel"), (wx1 - 0.9, fy - 0.5, 5.9, wx1 - 0.4, fy + 0.5, 6.4, "pi_steel")]
    # The nameplate and the DANGER label a hair out of the door; the flag's window frame; the handle's plate.
    b += [(3.0, 24.6, D, 13.0, 26.3, D + 0.1, face), (3.0, 9.6, D, 13.0, 12.0, D + 0.1, face)]
    fx0, fy0, fx1, fy1 = BD_FLAG
    b.append((fx0 - 0.2, fy0 - 0.2, D - 0.05, fx1 + 0.2, fy1 + 0.2, D + 0.02, face))
    hx, hy = BD_HUB
    b += [bx[:6] + ("pi_pnl_yellow",) for bx in PP._sc_round(hx, hy, D, D + 0.15, 1.3, None)]
    # The hinges down its left edge, the latch screws on its right.
    for y in (11.0, 24.0):
        b.append((1.9, y, 6.8, 2.4, y + 1.8, D, "pi_chrome"))
    for y in (12.0, 22.0):
        b.append((13.0, y, D, 13.4, y + 0.4, D + 0.15, "pi_steel"))
    # The conduit pair out of its top (on into battery_disconnect_riser and the DC tray).
    for cx0, cx1 in BD_CONDUITS:
        b += [(cx0, by1, BD_CZ[0], cx1, 32.0, BD_CZ[1], "pi_steel"), (cx0 - 0.2, by1, BD_CZ[0] - 0.2, cx1 + 0.2, by1 + 0.8, BD_CZ[1] + 0.2, "pi_steel_dark")]
    return b


def riser_part(c, r):
    """The disconnect's conduit pair up from its top into the DC tray over it (two blocks: the lower straight, the upper
    on up into the tray's bottom), a strap on the wall at each block, a coupling."""
    out = []
    top = 16.0 if r == 0 else 28.2
    for cx0, cx1 in BD_CONDUITS:
        out.append((cx0, 0.0, BD_CZ[0], cx1, top, BD_CZ[1], "pi_steel"))
        out.append((cx0 - 0.2, 7.0, BD_CZ[0] - 0.2, cx1 + 0.2, 8.0, BD_CZ[1] + 0.2, "pi_steel_dark"))
    out.append((4.8, 10.0, 0.0, 11.2, 10.8, BD_CZ[0] - 0.2, "pi_br_strut"))
    return out


# ------------------------------------------------------------------------------------------------ the room's pieces

def vapor_tight_light():
    """A 4 ft enclosed and gasketed light under the ceiling: its fibreglass housing, the frosted lens (lit), the
    stainless clips along both sides, the conduit hub at one end."""
    out = [(1.0, 13.4, 5.0, 15.0, 16.0, 11.0, "pi_br_vt_housing"),
           (1.4, 12.2, 5.4, 14.6, 13.4, 10.6, "pi_br_vt_lens")]
    for x in (2.6, 8.0, 13.4):
        out += [(x - 0.4, 12.4, 4.7, x + 0.4, 14.2, 5.0, "pi_steel"), (x - 0.4, 12.4, 11.0, x + 0.4, 14.2, 11.3, "pi_steel")]
    out += [(14.2, 14.0, 7.3, 15.8, 15.6, 8.7, "pi_steel_dark")]
    return out


def ppe_cabinet():
    """The battery PPE cabinet by the door: yellow steel, a clear door, its header; inside the face shield, the acid
    apron, the rubber gloves, the goggles."""
    Y = "pi_dc_yellow"
    out = [(1.0, 1.0, 0.0, 15.0, 1.6, 5.0, Y), (1.0, 13.0, 0.0, 15.0, 15.0, 5.0, {"*": Y, "south": "pi_br_ppe_head!"}),
           (1.0, 1.6, 0.0, 1.6, 13.0, 5.0, Y), (14.4, 1.6, 0.0, 15.0, 13.0, 5.0, Y), (1.6, 1.6, 0.0, 14.4, 13.0, 0.4, "pi_pnl_white"),
           # The clear door in its frame, the latch.
           (1.0, 1.0, 5.0, 15.0, 1.4, 5.4, Y), (1.0, 12.6, 5.0, 15.0, 13.0, 5.4, Y), (1.0, 1.4, 5.0, 1.4, 12.6, 5.4, Y),
           (14.6, 1.4, 5.0, 15.0, 12.6, 5.4, Y), (1.4, 1.4, 5.1, 14.6, 12.6, 5.2, {"*": "pi_dc_glass", "south": "pi_dc_glass", "north": "pi_dc_glass"}),
           (13.6, 6.2, 5.4, 14.2, 8.2, 5.8, "pi_cap_black")]
    # The face shield on its hook: the headgear, the clear visor.
    out += [(2.6, 10.6, 0.4, 6.4, 11.6, 2.2, "pi_cap_black"), (2.4, 6.6, 1.6, 6.6, 10.6, 1.9, {"*": "pi_dc_glass", "south": "pi_dc_glass", "north": "pi_dc_glass"})]
    # The acid apron hung beside it.
    out += [(7.4, 3.0, 0.4, 11.0, 11.8, 0.8, {"*": Y, "south": "pi_dc_apron!"}), (8.9, 11.8, 0.4, 9.5, 12.4, 1.2, "pi_steel")]
    # The gloves (a pair, cuffs up) and the goggles on the shelf.
    for dx in (0.0, 1.3):
        out += [(11.8 + dx, 6.0, 0.6, 12.9 + dx, 9.6, 1.2, "pi_cap_black")]
    out += [(2.8, 2.0, 0.6, 6.2, 3.0, 2.6, "pi_cap_black"), (3.1, 2.2, 2.6, 5.9, 2.8, 2.7, {"*": "pi_dc_glass", "south": "pi_dc_glass", "north": "pi_dc_glass"})]
    return out


def log_board():
    """The cell maintenance log on its board: the board, the clipboard and its sheet, the pen on its cord."""
    return [(1.2, 1.0, 0.0, 14.8, 15.0, 0.4, "pi_br_board"),
            (3.0, 2.4, 0.4, 13.0, 13.6, 0.7, "pi_br_clipboard"),
            (3.4, 2.8, 0.7, 12.6, 12.4, 0.8, {"*": "pi_pnl_white", "south": "pi_br_log!"}),
            (6.4, 12.6, 0.7, 9.6, 14.0, 1.2, "pi_steel"),
            (13.4, 7.0, 0.4, 13.8, 11.0, 0.9, "pi_cap_black"), (13.5, 11.0, 0.4, 13.7, 13.0, 0.5, "pi_cap_black")]


def sump():
    """The acid neutralization sump's cover in the floor: its yellow and black rim, the grating, its stencil."""
    return [(0.6, 0.0, 0.6, 15.4, 0.35, 15.4, {"*": "pi_br_sump_rim", "up": "pi_br_sump_top!"})]


# ------------------------------------------------------------------------------------------------ the textures

def _img(w, h, rgb):
    return Image.new("RGBA", (w, h), tuple(rgb) + (255,))


def ppe_head_tex():
    img = _img(512, 64, (236, 196, 36))
    d = ImageDraw.Draw(img)
    d.rectangle([6, 6, 505, 57], fill=(24, 24, 26))
    SGR._text(d, (256, 22), "BATTERY ACID PPE", 26, (246, 200, 40), width=480)
    SGR._text(d, (256, 46), "FACE SHIELD  APRON  GLOVES  GOGGLES", 16, (236, 236, 230), width=480)
    return img


def log_tex():
    """The log sheet: its heading, the columns, a few weeks' readings in pencil."""
    img = _img(256, 320, (246, 246, 240))
    d = ImageDraw.Draw(img)
    SGR._text(d, (128, 16), "STATION BATTERY", 18, (24, 24, 26))
    SGR._text(d, (128, 36), "MONTHLY CELL LOG", 16, (24, 24, 26))
    cols = (8, 56, 112, 168, 248)
    for k, s in enumerate(("DATE", "PILOT V", "SG", "TEMP F")):
        SGR._text(d, ((cols[k] + cols[k + 1]) / 2, 58), s, 11, (24, 24, 26))
    for y in range(68, 312, 18):
        d.line([(8, y), (248, y)], fill=(150, 170, 200), width=1)
    for x in cols:
        d.line([(x, 50), (x, 302)], fill=(150, 170, 200), width=1)
    rows = (("07/02", "2.23", "1.214", "76"), ("08/06", "2.22", "1.213", "77"), ("09/03", "2.23", "1.215", "75"),
            ("10/01", "2.23", "1.214", "76"))
    for r, row in enumerate(rows):
        for k, s in enumerate(row):
            SGR._text(d, ((cols[k] + cols[k + 1]) / 2, 77 + r * 18), s, 12, (70, 70, 80), bold=False)
    return img


def sump_tex():
    """The sump's cover from above: the hazard-striped rim, the grating, NEUTRALIZATION SUMP stencilled."""
    img = _img(256, 256, (236, 196, 36))
    d = ImageDraw.Draw(img)
    for k in range(-256, 512, 36):
        d.polygon([(k, 0), (k + 18, 0), (k + 18 - 256, 256), (k - 256, 256)], fill=(24, 24, 26))
    d.rectangle([28, 28, 227, 227], fill=(70, 72, 74))
    for x in range(36, 224, 12):
        d.rectangle([x, 36, x + 5, 219], fill=(34, 36, 38))
    d.rectangle([36, 104, 219, 152], fill=(236, 196, 36))
    SGR._text(d, (128, 118), "ACID NEUTRALIZATION", 15, (24, 24, 26), width=176)
    SGR._text(d, (128, 138), "SUMP - NO DRAIN TO SEWER", 12, (24, 24, 26), width=176)
    return img


def sign_tex(kind):
    """The room's sign: the battery, its train's band, its strings."""
    W, H = 433, 512
    img = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    t = kind[-1].upper()
    band = {"A": (226, 116, 30), "B": (40, 140, 70)}[t]
    d.rectangle([0, 0, W - 1, H - 1], fill=(24, 24, 26))
    d.rectangle([14, 14, W - 15, H - 15], outline=(236, 236, 230), width=6)
    SGR._text(d, (W / 2, 90), "STATION", 64, (250, 250, 246))
    SGR._text(d, (W / 2, 168), f"BATTERY 1{t}", 72, (250, 250, 246), width=W - 60)
    d.rectangle([30, 220, W - 31, 300], fill=band)
    SGR._text(d, (W / 2, 260), f"CLASS 1E  TRAIN {t}", 40, (20, 20, 22) if t == "A" else (250, 250, 246), width=W - 80)
    SGR._text(d, (W / 2, 350), "125 VDC  2 STRINGS", 34, (236, 236, 230), width=W - 60)
    SGR._text(d, (W / 2, 395), "60 CELLS EACH", 34, (236, 236, 230), width=W - 60)
    SGR._text(d, (W / 2, 455), "HYDROGEN - NO OPEN FLAMES", 24, (246, 200, 40), width=W - 60)
    return img


SIGNS = {"sign_battery_1a": "Station Battery 1A Sign", "sign_battery_1b": "Station Battery 1B Sign"}


def makers():
    solid = PP._solid
    out = {
        "pi_br_pillow": lambda: solid((214, 214, 206), 820, 0.08), "pi_br_thermo": lambda: solid((236, 236, 230), 821, 0.02),
        "pi_br_boot_red": lambda: solid((176, 32, 28), 822, 0.04), "pi_br_boot_black": lambda: solid((30, 30, 32), 823, 0.04),
        "pi_br_disc": lambda: solid((150, 154, 156), 824, 0.05), "pi_br_strut": lambda: solid((168, 172, 176), 825, 0.08),
        "pi_br_fuse": lambda: solid((196, 170, 120), 826, 0.04), "pi_br_vt_housing": lambda: solid((196, 198, 196), 827, 0.04),
        "pi_br_vt_lens": lambda: solid((250, 250, 244), 828, 0.01), "pi_br_board": lambda: solid((176, 140, 96), 829, 0.1),
        "pi_br_clipboard": lambda: solid((120, 84, 50), 830, 0.08), "pi_br_sump_rim": lambda: solid((236, 196, 36), 831, 0.06),
        "pi_br_ppe_head": ppe_head_tex, "pi_br_log": log_tex, "pi_br_sump_top": sump_tex,
    }
    return {k: SGR._square(v) for k, v in out.items()}


SHARP = {"pi_br_ppe_head", "pi_br_log", "pi_br_sump_top"}


# ------------------------------------------------------------------------------------------------ registering

PANELS = {
    BD_KEY: {"display": "Battery Disconnect Switch", "width": 1, "item3d": True, "labels": BD_LABELS},
}
MULTI = {"battery_disconnect_riser": {"display": "Battery Disconnect Conduit", "multi": (1, 2, "frame"), "parts": riser_part, "item3d": True}}
PIECES = {"vapor_tight_light": {"display": "Vapor-Tight Light", "boxes": vapor_tight_light(), "glow": ["pi_br_vt_lens"]},
          "battery_ppe_cabinet": {"display": "Battery PPE Cabinet", "boxes": ppe_cabinet()},
          "battery_log_board": {"display": "Battery Cell Log Board", "boxes": log_board()},
          "battery_room_sump": {"display": "Acid Neutralization Sump", "boxes": sump()}}
CRAFTING = {
    BD_KEY: (["PFP", "CRC", "PFP"], {"P": "#c:plates/steel", "F": "minecraft:redstone", "C": "minecraft:copper_ingot", "R": "minecraft:lever"}, 1),
    "battery_disconnect_riser": (["I I", "I I"], {"I": "minecraft:iron_ingot"}, 2),
    "vapor_tight_light": (["PGP"], {"P": "#c:plates/iron", "G": "minecraft:glowstone"}, 2),
    "battery_ppe_cabinet": (["YGY", "LBL", "YYY"], {"Y": "minecraft:yellow_dye", "G": "minecraft:glass_pane", "L": "minecraft:leather",
                                                    "B": "minecraft:iron_ingot"}, 1),
    "battery_log_board": (["SPS", "SPS"], {"S": "minecraft:stick", "P": "minecraft:paper"}, 1),
    "battery_room_sump": (["YIY"], {"Y": "minecraft:yellow_dye", "I": "minecraft:iron_bars"}, 1),
    **{k: (["PCP", "PIP"], {"P": "minecraft:paper", "C": "minecraft:orange_dye" if k.endswith("a") else "minecraft:green_dye",
                             "I": "minecraft:iron_nugget"}, 2) for k in SIGNS},
}


def register():
    PP.PANELS[SB_KEY]["labels"] = LABELS
    PP.PANELS[SB_KEY]["display"] = "Station Battery (125 VDC, 30 Cells)"
    PP.DRAWINGS[SB_KEY] = battery_drawing
    PP.CABINETS[SB_KEY] = station_battery_boxes
    PP.NOT_LIVE.add(SB_KEY)
    for key, info in PANELS.items():
        info["multi"] = (info["width"], info.get("rows", 2), "panel")
        info["panel"] = key
    PP.PANELS.update(PANELS)
    PP.DRAWINGS[BD_KEY] = disconnect
    PP.CABINETS[BD_KEY] = disconnect_boxes
    PP.RISERS.update(PIECES)
    PP.RISERS.update(MULTI)
    PP.CRAFTING.update(CRAFTING)
    PP.EXTRA_MAKERS.append(makers)
    PP.SHARP |= SHARP
    PP.SHARP |= {f"pi_{PP.texture_key(SB_KEY, v)}_f_{p}" for v in range(len(LABELS)) for p in range(8)}
    PP.SHARP |= {f"pi_{PP.texture_key(BD_KEY, v)}_f_{p}" for v in range(len(BD_LABELS)) for p in range(2)}
    old = PP._sign_image

    def draw(kind):
        return sign_tex(kind) if kind in SIGNS else old(kind)
    PP._sign_image = draw
    PP.SIGNS.update(SIGNS)
    PP.SHARP |= {f"pi_{k}" for k in SIGNS}      # (kept at full size so the writing reads)
    PP.RISERS.update({k: {"display": v, "boxes": PP._sign_boxes(f"pi_{k}")} for k, v in SIGNS.items()})


register()
