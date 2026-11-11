"""The Tri-Pillar's roof plant (roof plant and walkways, take 1, approved by Justin 26 Oct): fifteen room-kit pieces
for the control building's roof, real blocks placed whole from one item (tower/RoomKit; tools/room_kit.py runs write()
as one of its kits, the "roof" room).

- rooftop_ahu: a packaged rooftop air handling unit, 3 x 8 blocks and 3 high, on steel W-beam dunnage on concrete pier
  pads: double-wall RAL 7035 casing with panel joints and a standing-seam roof cap, four hinged access doors with cam
  latches (fan, coil, filters, mixing box), the fan door's warning label, the filters' differential pressure gauge, a
  control panel (RUN lit) with its isolator and conduit to a pitch pocket, the coil's insulated pipes down into a pipe
  portal, its condensate trap to a splash block, the outside-air hood with its bird screen and louvre, canvas
  connectors where the supply (discharge end) and return (mixing box) ducts join (the HVAC ducts, then a curb).
- duct_roof_curb: an HVAC duct riser standing in its roof curb (counter-flashing, storm collar).
- roof_exhaust_fan (spun aluminium downblast hood), roof_upblast_fan, kitchen_exhaust_fan (upblast with the grease
  box): centrifugal roof fans on insulated curbs, each with its isolator; tagged EF-1..EF-6 (variant).
- dry_cooler: a flatbed dry cooler, 2 x 6 blocks and 2 high: legs on rubber feet, the finned coil, 12 EC fans in
  nozzle rings under wire guards (the rotors turn: ROTORS, client/RoomKitRotorRenderer), the header end's insulated
  drops, butterfly valves with red levers and the fan controller.
- roof_pipe_support: the insulated chilled-water pipe (as hv_chw_pipe) on a roof pipe support.
- pipe_doghouse: a pipe penetration box, 1 x 2, a pipe in through a rubber boot in each block.
- pipe_crossover: a steel crossover over pipes on the roof (1 wide, 7 long, 2 high): a 45-degree stair each end, five
  blocks of grating deck, yellow handrails, toe boards, posts at the block joints clear of the pipes.
- comms_mast_base, comms_mast, comms_mast_tie, comms_mast_top: a square galvanized lattice mast (0.44 m between leg
  centres) built up a block at a time: its base plate on a plinth, sections (step bolts, the feeder ladder and coax
  on its south face), a section with wall ties east, the top (three sector panels with radio units, two whips, the
  steady red obstruction light, the GPS puck, the finial).
- comms_dish: a microwave dish with its radome and outdoor unit on a side arm back to the mast block behind it.
- rf_cabinet: an outdoor telecom cabinet (door north: leaf, hinges, swing handle, cooling grille) and its cable bridge
  east over the next block to the mast, on a post.

Everything only looks live (the server load rule): nothing ticks; the dry coolers' fans turn on the client.
Run with the room kits: python3 tools/room_kit.py
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image

import model_writer as MW
from roof_plant_art import (B, EMISSIVE, MATERIALS, PICS, door, gauge, guard, label_x, label_z, octagon_axis, octagon_prism,
                            octagon_ring, pic, plate, round_axis, round_y, stencil, warning)

ROOT = Path(__file__).resolve().parent.parent
ATLAS = 512                   # the pictures' atlas (16 texels a pixel; the guards' at 8)
# textures of the mod's own the pieces share (the HVAC ducts' galvanized steel, the chilled-water pipes' jacket)
REUSE = {"hv_jacket": "jugcraft:block/pi_hv_jacket", "hv_jacket_band": "jugcraft:block/pi_hv_jacket_band",
         "hv_galv": "jugcraft:block/pi_hv_galv", "hv_galv_dark": "jugcraft:block/pi_hv_galv_dark"}
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}


# ------------------------------------------------------------------ the air handler
AHU_TOP = 44.0            # the casing's top
AHU_SECTIONS = (18, 50, 74, 94)          # section joints along z (discharge | fan | coil | filters | mixing box)


def ahu(tag="AHU-R1"):
    """The rooftop air handler, structure space facing north: its discharge end at z 0 (the supply duct off its middle
    block, row 1), the outside-air hood at the south end, service doors on the east, the return in on the west of the
    mixing box (block z 6, row 1)."""
    e = []
    # concrete pier pads with neoprene, the two W-beam dunnage rails on them
    for cx in (8, 40):
        for cz in (10, 44, 84, 118):
            e.append(B((cx - 3, 0, cz - 3), (cx + 3, 2, cz + 3), "rp_concrete"))
            e.append(B((cx - 2.5, 2, cz - 2.5), (cx + 2.5, 2.6, cz + 2.5), "rp_rubber"))
        e.append(B((cx - 2.5, 2.6, 4), (cx + 2.5, 3.4, 124), "rp_dunnage"))
        e.append(B((cx - 0.4, 3.4, 4), (cx + 0.4, 7.6, 124), "rp_dunnage"))
        e.append(B((cx - 2.5, 7.6, 4), (cx + 2.5, 8.4, 124), "rp_dunnage"))
    # the unit's galvanized base rail, lifting lugs on its corners
    e.append(B((1, 8.4, 2), (47, 12.6, 126), "rp_galv"))
    for x0, x1 in ((47, 47.6), (0.4, 1)):
        for z in (5, 121):
            e.append(B((x0, 9.2, z), (x1, 12.0, z + 2.4), "rp_galv_dark"))
    # the casing (double-wall panels, RAL 7035) and its roof cap with standing seams
    e.append(B((1.5, 12.6, 2.5), (46.5, AHU_TOP, 121.5), "rp_ahu"))
    e.append(B((0.9, AHU_TOP, 1.9), (47.1, AHU_TOP + 1.2, 122.1), "rp_ahu_roof"))
    for z in range(10, 121, 16):
        e.append(B((0.9, AHU_TOP + 1.2, z - 0.25), (47.1, AHU_TOP + 1.6, z + 0.25), "rp_ahu_roof"))
    # panel joints: the section joints on both sides, the panel joints between on the west (the east has the doors)
    for z in AHU_SECTIONS:
        e.append(B((46.5, 12.6, z - 0.4), (46.85, AHU_TOP, z + 0.4), "rp_ahu"))
        e.append(B((1.15, 12.6, z - 0.4), (1.5, AHU_TOP, z + 0.4), "rp_ahu"))
    for z in (34, 62, 84, 108):
        e.append(B((1.15, 12.6, z - 0.4), (1.5, AHU_TOP, z + 0.4), "rp_ahu"))
    # horizontal joint strip along both sides at the panel line under the roof cap
    e.append(B((46.5, AHU_TOP - 1.0, 2.5), (46.8, AHU_TOP - 0.4, 121.5), "rp_ahu"))
    e.append(B((1.2, AHU_TOP - 1.0, 2.5), (1.5, AHU_TOP - 0.4, 121.5), "rp_ahu"))
    # service doors on the east side: fan, coil, filters, mixing box
    e += door(46.5, 26, 42, 15, 40.5, 1, "rp_ahu", latches=(20, 35.5))
    e += door(46.5, 54, 70, 15, 40.5, 1, "rp_ahu", latches=(20, 35.5))
    e += door(46.5, 77, 91, 15, 40.5, 1, "rp_ahu", latches=(20, 35.5))
    e += door(46.5, 99, 117, 15, 40.5, 1, "rp_ahu", latches=(20, 35.5))
    # the fan door's warning label and the filter section's differential pressure gauge
    e.append(label_x(47.25, 1, 33.0, 40.6, 22.5, 26.0, pic("ahu_fan_warn", 7.6, 3.5,
                                                            warning(["ROTATING FAN", "ISOLATE FIRST"]))))
    e.append(B((46.5, 29.6, 91.9), (47.3, 33.4, 93.5), "rp_black"))
    e.append((list((47.3, 29.9, 92.05)), list((47.42, 33.1, 93.35)), {"*": "rp_black", "east": pic("ahu_gauge", 1.3, 3.2, gauge())}))
    # unit tag on both sides by the discharge end, the nameplate under it
    e.append(label_x(46.5, 1, 4, 16, 37.5, 41.5, pic("ahu_tag_" + tag, 12, 4, stencil(tag, bg=(206, 207, 201)))))
    e.append(label_x(1.5, -1, 4, 16, 37.5, 41.5, pic("ahu_tag_" + tag, 12, 4, stencil(tag, bg=(206, 207, 201)))))
    e.append(label_x(1.5, -1, 98, 110, 37.5, 41.5, pic("ahu_tag_" + tag, 12, 4, stencil(tag, bg=(206, 207, 201)))))
    # the control panel (door proud, hinges, handle, RUN lamp lit, FAULT lamp dark), the disconnect under it
    e.append(B((46.5, 20, 4), (49, 36, 15.5), "rp_cab"))
    e.append(B((49, 20.4, 4.4), (49.4, 35.6, 15.1), "rp_cab"))
    for hy in (23, 33):
        e.append(B((49.4, hy - 1.2, 4.0), (49.9, hy + 1.2, 5.0), "rp_galv_dark"))
    e.append(B((49.4, 26.5, 13.6), (49.8, 29.5, 14.4), "rp_black"))
    e.append(B((49.4, 32.2, 7.0), (49.9, 33.2, 8.0), "rp_led_green"))
    e.append(B((49.4, 32.2, 9.2), (49.9, 33.2, 10.2), "rp_lamp_off"))
    e.append(label_x(49.4, 1, 6.4, 12.6, 28.0, 30.6, pic("ahu_cp", 6.2, 2.6, plate([tag + " CONTROL", "LOCAL / AUTO"]))))
    e.append(B((46.5, 13.4, 6.0), (48.6, 19.4, 11.0), "rp_cab"))
    e.append(B((48.6, 14.6, 7.0), (48.8, 18.2, 10.0), "rp_yellow"))
    e.append(B((48.8, 16.0, 7.6), (49.7, 16.9, 10.6), "rp_red"))
    # its conduit down to a pitch pocket on the roof
    e.append(B((47.3, 1.6, 8.0), (48.3, 13.4, 9.0), "rp_galv_dark"))
    e.append(B((46.8, 0, 7.0), (48.8, 1.6, 10.0), "rp_rubber"))
    # the coil's condensate drain: out of the coil section, down through its trap to a splash block
    e.append(B((46.5, 13.2, 61.0), (49.2, 14.2, 62.0), "rp_pvc"))
    e.append(B((48.2, 8.0, 61.0), (49.2, 13.2, 62.0), "rp_pvc"))
    e.append(B((48.2, 7.0, 61.0), (51.4, 8.0, 62.0), "rp_pvc"))
    e.append(B((50.4, 8.0, 61.0), (51.4, 10.6, 62.0), "rp_pvc"))
    e.append(B((50.4, 0.8, 61.0), (51.4, 7.0, 62.0), "rp_pvc"))
    e.append(B((49.4, 0, 59.4), (52.4, 0.8, 64.0), "rp_concrete"))
    # the coil's chilled-water pipes on the west side, insulated, down into a pipe portal on the roof
    for cz, cy in ((58.0, 30.0), (66.0, 24.0)):
        e += round_axis("x", (cz, cy), 1.6, -2.4, 1.5, "hv_jacket")
        e += round_y(-2.4, cz, 1.6, 3.4, cy + 1.6, "hv_jacket")
        for by in (10.0, 18.0):
            if by < cy - 2:
                e += round_y(-2.4, cz, 1.85, by, by + 0.5, "hv_jacket_band")
    e.append(B((-6.0, 0, 54.0), (1.0, 3.4, 70.0), "rp_galv"))
    e.append(B((-6.4, 3.4, 53.6), (1.4, 4.0, 70.4), "rp_galv_dark"))
    # the supply opening (discharge end, middle block, row 1): flange and canvas connector to the duct
    e.append(B((16.3, 16.3, 2.0), (31.7, 31.7, 2.5), "rp_galv_dark"))
    e.append(B((16.9, 16.9, 0.25), (31.1, 31.1, 2.0), "rp_canvas"))
    # the return opening (west side of the mixing box, block z 6, row 1)
    e.append(B((1.0, 16.3, 96.3), (1.5, 31.7, 111.7), "rp_galv_dark"))
    e.append(B((0.25, 16.9, 96.9), (1.0, 31.1, 111.1), "rp_canvas"))
    # the outside-air hood on the south end: top, sides, front skirt, bird screen under it, the louvre behind
    e.append(B((5.0, 41.6, 121.5), (43.0, 42.4, 127.8), "rp_galv"))
    e.append(B((5.0, 30.0, 127.0), (43.0, 41.6, 127.8), "rp_galv"))
    e.append(B((5.0, 30.0, 121.5), (5.8, 41.6, 127.0), "rp_galv"))
    e.append(B((42.2, 30.0, 121.5), (43.0, 41.6, 127.0), "rp_galv"))
    e.append(B((5.8, 30.0, 121.5), (42.2, 30.3, 127.0), {"*": "rp_screen"}))
    e.append(B((6.5, 18.0, 121.5), (41.5, 41.0, 121.7), {"*": "rp_galv_dark", "south": "rp_louvre"}))
    # the end's own tag and a drain pan nipple at the hood's foot
    e.append(label_z(2.5, -1, 34.0, 44.0, 36.0, 40.0, pic("ahu_tag_" + tag, 12, 4, stencil(tag, bg=(206, 207, 201)))))
    return e


AHU_CELLS = [(x, y, z) for x in range(3) for y in range(3) for z in range(8)]
AHU_SUPPLY = (1, 1, -1)       # the duct cell off the discharge (an elbow down, the curb under it)
AHU_RETURN = (-1, 1, 6)       # the duct cell off the mixing box's west side


# ------------------------------------------------------------------ the dry cooler
DC_Z0 = 6.0                       # the casing's north end (the header bay in front of it)
DC_DECK = 29.0                    # the fan deck's underside
DC_FX = (8.75, 23.25)
DC_FZ = [DC_Z0 + 1.0 + (94.5 - DC_Z0 - 1.0) / 6 * (i + 0.5) for i in range(6)]
DC_R_IN = 6.2                     # the nozzle's inner apothem
DC_HOLE = 6.6                     # half the square hole in the deck


def dc_fans():
    return [(x, z) for z in DC_FZ for x in DC_FX]


def cooler(tag="DC-1"):
    """The flatbed dry cooler, structure space facing north: its header end at z 0 (the pipes leave it at x 8 and 24,
    y 8, into the blocks north of it), 2 x 6 EC fans on its deck (their rotors: ROTORS, drawn turning by the game)."""
    e = []
    # legs on rubber feet, side rails and cross ties
    for x in (1.0, 29.5):
        for z in (DC_Z0 + 1.0, 34.0, 62.0, 93.0):
            e.append(B((x - 0.4, 0, z - 0.4), (x + 1.9, 0.6, z + 1.9), "rp_rubber"))
            e.append(B((x, 0.6, z), (x + 1.5, 14.0, z + 1.5), "rp_galv"))
        e.append(B((x, 4.0, DC_Z0 + 2.5), (x + 1.5, 5.0, 93.0), "rp_galv"))
    for z in (DC_Z0 + 1.0, 34.0, 62.0, 93.0):
        e.append(B((2.5, 4.0, z), (29.5, 5.0, z + 1.5), "rp_galv"))
    # casing: bottom frame, side and end panels, the coil inside (seen from below), the fan deck
    e.append(B((0.5, 14.0, DC_Z0), (31.5, 15.0, DC_Z0 + 1.0), "rp_galv"))
    e.append(B((0.5, 14.0, 94.5), (31.5, 15.0, 95.5), "rp_galv"))
    e.append(B((0.5, 14.0, DC_Z0 + 1.0), (1.5, 15.0, 94.5), "rp_galv"))
    e.append(B((30.5, 14.0, DC_Z0 + 1.0), (31.5, 15.0, 94.5), "rp_galv"))
    e.append(B((1.5, 15.0, DC_Z0 + 1.0), (30.5, 21.0, 94.5), {"*": "rp_coil", "down": "rp_coil_top", "up": "rp_coil_top"}))
    for x0, x1 in ((0.5, 1.5), (30.5, 31.5)):
        e.append(B((x0, 15.0, DC_Z0), (x1, DC_DECK + 1.0, 95.5), "rp_cooler"))
    for z0, z1 in ((DC_Z0, DC_Z0 + 1.0), (94.5, 95.5)):
        e.append(B((1.5, 15.0, z0), (30.5, DC_DECK + 1.0, z1), "rp_cooler"))
    # the corner posts proud of the panels, the deck's edge angle
    for x0, x1 in ((0.2, 1.2), (30.8, 31.8)):
        for z0, z1 in ((DC_Z0 - 0.3, DC_Z0 + 0.7), (94.8, 95.8)):
            e.append(B((x0, 14.0, z0), (x1, DC_DECK + 1.6, z1), "rp_galv"))
    # the deck: strips round the square holes under the nozzles
    xs = sorted({1.5, 30.5} | {fx - DC_HOLE for fx in DC_FX} | {fx + DC_HOLE for fx in DC_FX})
    zs = sorted({DC_Z0 + 1.0, 94.5} | {fz - DC_HOLE for fz in DC_FZ} | {fz + DC_HOLE for fz in DC_FZ})
    holes = {(fx, fz) for fx in DC_FX for fz in DC_FZ}
    for i in range(len(xs) - 1):
        for j in range(len(zs) - 1):
            mx, mz = (xs[i] + xs[i + 1]) / 2, (zs[j] + zs[j + 1]) / 2
            if any(abs(mx - fx) < DC_HOLE and abs(mz - fz) < DC_HOLE for fx, fz in holes):
                continue
            if xs[i + 1] - xs[i] < 1e-6 or zs[j + 1] - zs[j] < 1e-6:
                continue
            e.append(B((xs[i], DC_DECK, zs[j]), (xs[i + 1], DC_DECK + 1.0, zs[j + 1]), "rp_cooler"))
    # each fan: the nozzle ring standing in its hole, corner plates closing the hole round it, the motor, the guard
    s = DC_R_IN * math.tan(math.radians(22.5))
    for (fx, fz) in dc_fans():
        e += octagon_ring(fx, fz, DC_R_IN, 0.4, DC_DECK - 0.4, DC_DECK + 3.6, "rp_cooler")
        for sx in (-1, 1):
            for sz in (-1, 1):
                px, pz = fx + sx * DC_HOLE, fz + sz * DC_HOLE
                h = (DC_HOLE - (DC_R_IN + 0.2) / math.sqrt(2) * 0 - s) if False else 2.9
                e.append(B((px - h, DC_DECK + 0.55, pz - h), (px + h, DC_DECK + 0.95, pz + h), "rp_cooler", ("y", 45, (px, DC_DECK + 0.75, pz))))
        e += octagon_prism(fx, fz, 2.0, DC_DECK + 2.0, DC_DECK + 3.4, "rp_black")          # the EC motor
        e += octagon_prism(fx, fz, 1.0, DC_DECK + 3.4, DC_DECK + 4.0, "rp_galv_dark")      # its mount on the guard
        e.append(B((fx - DC_R_IN - 0.4, DC_DECK + 4.0, fz - DC_R_IN - 0.4), (fx + DC_R_IN + 0.4, DC_DECK + 4.15, fz + DC_R_IN + 0.4),
                   {"up": "rp_pic_guard!", "down": "rp_pic_guard!"}))
    # the header bay: insulated drops from the headers, butterfly valves with red levers, flanged nozzles
    for px in (8.0, 24.0):
        e += octagon_axis("z", (px, 8.0), 1.76, 0.0, DC_Z0 - 1.2, "hv_jacket")
        e += octagon_prism(px, DC_Z0 - 2.96, 1.76, 9.76, 22.0, "hv_jacket")
        e += octagon_axis("z", (px, 20.0), 1.4, DC_Z0 - 2.96, DC_Z0, "rp_copper")
        for y in (12.0, 13.6):
            e += octagon_prism(px, DC_Z0 - 2.96, 2.3, y, y + 0.6, "rp_galv_dark")
        e += octagon_prism(px, DC_Z0 - 2.96, 2.0, 12.6, 13.6, "rp_black")
        e.append(B((px + 2.0, 12.8, DC_Z0 - 3.4), (px + 3.2, 13.4, DC_Z0 - 2.5), "rp_galv_dark"))
        e.append(B((px + 3.2, 13.0, DC_Z0 - 3.5), (px + 7.4, 13.5, DC_Z0 - 2.4), "rp_red"))
        e += octagon_axis("z", (px, 8.0), 2.0, 0.0, 0.6, "rp_galv_dark")
    # the controller between the drops: enclosure, door, isolator, lamp; its conduit down
    e.append(B((12.4, 15.0, 1.6), (19.6, 27.0, DC_Z0), "rp_cab"))
    e.append(B((12.7, 15.3, 1.2), (19.3, 26.7, 1.6), "rp_cab"))
    e.append(B((17.4, 19.2, 0.8), (18.4, 22.0, 1.2), "rp_black"))
    e.append(B((13.6, 23.4, 0.6), (16.4, 25.8, 1.2), "rp_yellow"))
    e.append(B((14.6, 24.2, 0.0), (15.4, 25.0, 0.6), "rp_red"))
    e.append(B((13.6, 20.6, 0.7), (14.6, 21.6, 1.2), "rp_led_green"))
    e.append(B((15.2, 0.6, 3.0), (16.2, 15.0, 4.0), "rp_galv_dark"))
    e.append(B((14.6, 0, 2.4), (16.8, 0.6, 4.6), "rp_rubber"))
    # tags on the long sides and the end
    for zc in (23.0, 72.0):
        e.append(label_x(31.5, 1, zc - 6, zc + 6, 20.0, 24.0, pic("dc_tag_" + tag, 12, 4, stencil(tag, bg=(204, 206, 202)))))
        e.append(label_x(0.5, -1, zc - 6, zc + 6, 20.0, 24.0, pic("dc_tag_" + tag, 12, 4, stencil(tag, bg=(204, 206, 202)))))
    e.append(label_z(DC_Z0, -1, 20.4, 28.0, 24.0, 27.0, pic("dc_plate", 7.6, 3.0, plate(["DRY COOLER  " + tag, "GLYCOL 30 %  2 x 6 EC FANS"]))))
    return e


DC_CELLS = [(x, y, z) for x in range(2) for y in range(2) for z in range(6)]
DC_NOZZLES = ((0, 0, -1), (1, 0, -1))        # the pipe cells off the header end (hv_chw_pipe along z)


# ------------------------------------------------------------------ the comms mast
LEGS = ((4.5, 4.5), (11.5, 4.5), (4.5, 11.5), (11.5, 11.5))
GA = 0.6          # angle size


def _legs(y0, y1):
    return [B((x - 0.5, y0, z - 0.5), (x + 0.5, y1, z + 0.5), "rp_galv") for x, z in LEGS]


def _girts(y):
    e = []
    for z in (4.5, 11.5):
        e.append(B((5.0, y, z - GA / 2), (11.0, y + GA, z + GA / 2), "rp_galv"))
    for x in (4.5, 11.5):
        e.append(B((x - GA / 2, y + 0.05, 5.0), (x + GA / 2, y + GA + 0.05, 11.0), "rp_galv"))
    return e


def _diagonals(y0, flip):
    """One panel's four diagonals (45 degrees), from y0 to y0 + 7."""
    e = []
    yc = y0 + 3.8
    L = 7.0 * math.sqrt(2) / 2
    for z in (4.5, 11.5):
        e.append(B((8 - L, yc - 0.25, z - 0.25), (8 + L, yc + 0.25, z + 0.25), "rp_galv", ("z", 45 if flip else -45, (8, yc, z))))
    for x in (4.5, 11.5):
        e.append(B((x - 0.25, yc - 0.25, 8 - L), (x + 0.25, yc + 0.25, 8 + L), "rp_galv", ("x", -45 if flip else 45, (x, yc, 8))))
    return e


def _feeders(y0, y1):
    """The feeder ladder on the south face: two rails on stand-offs, three coax feeders, cable hangers."""
    e = []
    for x in (5.6, 10.4):
        e.append(B((x - 0.3, y0, 13.2), (x + 0.3, y1, 13.8), "rp_galv"))
    for y in range(int(y0) + 2, int(y1), 8):
        e.append(B((5.0, y, 12.0), (11.0, y + 0.5, 13.2), "rp_galv_dark"))
        e.append(B((5.3, y, 13.2), (10.7, y + 0.5, 14.6), "rp_galv_dark"))
    for x in (6.6, 8.0, 9.4):
        e += octagon_prism(x, 14.0, 0.4, y0, y1, "rp_cable")
    return e


def mast_section(part="mid", ties=False, cables=True, bolts=True):
    e = []
    if part == "base":
        e.append(B((1.0, 0, 1.0), (15.0, 3.0, 15.0), "rp_concrete"))
        e.append(B((2.5, 3.0, 2.5), (13.5, 3.6, 13.5), "rp_galv"))
        for x, z in ((3.2, 3.2), (12.8, 3.2), (3.2, 12.8), (12.8, 12.8)):
            e += octagon_prism(x, z, 0.45, 3.6, 4.6, "rp_galv_dark")
            e += octagon_prism(x, z, 0.65, 3.6, 4.0, "rp_galv_dark")
        for x, z in LEGS:
            e.append(B((x - 1.1, 3.6, z - 0.15), (x + 1.1, 5.4, z + 0.15), "rp_galv_dark"))
        e += _legs(3.6, 16.0)
        e += _girts(3.6)
        e += _diagonals(4.2, False)
        e += _girts(12.0)
    else:
        e += _legs(0.0, 16.0)
        e += _girts(0.0)
        e += _diagonals(0.6, False)
        e += _girts(8.0)
        e += _diagonals(8.6, True)
    if bolts:
        # step bolts up the north-west leg, alternately west and north
        for k, y in enumerate(range(2 if part == "mid" else 6, 16, 4)):
            if k % 2:
                e.append(B((2.8, y, 4.3), (4.0, y + 0.4, 4.7), "rp_galv_dark"))
            else:
                e.append(B((4.3, y, 2.8), (4.7, y + 0.4, 4.0), "rp_galv_dark"))
    if cables:
        e += _feeders(0.0 if part == "mid" else 3.6, 16.0)
    if ties:
        # wall ties east to the wall face at x 16: two arms, a brace, the wall plate with its anchors
        for z in (4.5, 11.5):
            e.append(B((12.0, 7.0, z - 0.3), (15.4, 7.6, z + 0.3), "rp_galv"))
        e.append(B((12.0, 7.6, 7.7), (15.4, 8.2, 8.3), "rp_galv"))
        e.append(B((15.4, 4.5, 3.5), (16.0, 10.5, 12.5), "rp_galv_dark"))
        for z in (4.6, 11.4):
            for y in (5.6, 9.4):
                e += octagon_axis("x", (z, y), 0.4, 14.9, 15.4, "rp_galv_dark")
    return e


def mast_top():
    """The mast's top: leg stubs and a cap plate, a mount frame carrying three sector panels (with their radio units)
    north, west and south, two whips, the steady red obstruction light, the GPS puck, the finial."""
    e = _legs(0.0, 6.0) + _girts(0.0) + _girts(5.4)
    e.append(B((3.6, 6.0, 3.6), (12.4, 6.6, 12.4), "rp_galv"))
    # mount pipes (vertical) out from the faces on brackets, a panel and a radio unit on each
    for side in ("north", "west", "south"):
        for y in (2.0, 11.0):
            if side == "north":
                e.append(B((7.6, y, 0.6), (8.4, y + 0.6, 4.0), "rp_galv_dark"))
            elif side == "south":
                e.append(B((7.6, y, 12.0), (8.4, y + 0.6, 15.4), "rp_galv_dark"))
            else:
                e.append(B((0.6, y, 7.6), (4.0, y + 0.6, 8.4), "rp_galv_dark"))
        if side == "north":
            e += octagon_prism(8.0, 0.2, 0.45, 0.0, 22.0, "rp_galv")
            e.append(B((5.7, 1.5, -2.4), (10.3, 22.5, -0.8), "rp_radome"))
        elif side == "south":
            e += octagon_prism(8.0, 15.8, 0.45, 0.0, 22.0, "rp_galv")
            e.append(B((5.7, 1.5, 16.8), (10.3, 22.5, 18.4), "rp_radome"))
        else:
            e += octagon_prism(0.2, 8.0, 0.45, 0.0, 22.0, "rp_galv")
            e.append(B((-2.4, 1.5, 5.7), (-0.8, 22.5, 10.3), "rp_radome"))
    # remote radio units under the panels on the mount pipes (north and south), cables to them
    e.append(B((5.8, -6.0, -2.2), (10.2, -0.6, -0.4), "rp_galv_dark"))
    e.append(B((5.8, -6.0, 16.4), (10.2, -0.6, 18.2), "rp_galv_dark"))
    e.append(B((-2.2, -6.0, 5.8), (-0.4, -0.6, 10.2), "rp_galv_dark"))
    # whips on arms off the north-east and south-west legs
    for (ax, az, wx, wz) in ((12.0, 4.0, 14.6, 1.4), (4.0, 12.0, 1.4, 14.6)):
        e.append(B((min(ax, wx) - 0.3, 6.6, min(az, wz) - 0.3), (max(ax, wx) + 0.3, 7.2, max(az, wz) + 0.3), "rp_galv_dark"))
        e += octagon_prism(wx, wz, 0.35, 7.2, 30.0, "rp_radome")
    # finial (the air terminal) up the middle, the obstruction light and the GPS puck on short arms
    e += octagon_prism(8.0, 8.0, 0.35, 6.6, 31.0, "rp_galv")
    e.append(B((12.4, 5.6, 7.6), (14.4, 6.2, 8.4), "rp_galv_dark"))
    e += octagon_prism(13.6, 8.0, 0.9, 6.2, 6.8, "rp_black")
    e += octagon_prism(13.6, 8.0, 0.75, 6.8, 8.0, "rp_led_red")
    e.append(B((7.6, 5.6, 12.4), (8.4, 6.2, 14.2), "rp_galv_dark"))
    e += octagon_prism(8.0, 13.6, 0.8, 6.2, 6.8, "rp_radome")
    return e


def dish(d=14.0):
    """A microwave dish facing north with its radome, the outdoor unit behind, the side arm back to the mast (whose
    north legs are 4.5 px into the next block south)."""
    r = d / 2
    e = []
    e += octagon_axis("z", (8.0, 8.0), r, 2.4, 3.0, "rp_radome")
    e += octagon_axis("z", (8.0, 8.0), r - 0.2, 3.0, 4.4, "rp_galv")
    e += octagon_axis("z", (8.0, 8.0), r * 0.78, 4.4, 5.8, "rp_galv")
    e += octagon_axis("z", (8.0, 8.0), r * 0.5, 5.8, 7.0, "rp_galv")
    e.append(B((6.0, 6.0, 7.0), (10.0, 10.0, 10.4), "rp_galv_dark"))          # the outdoor unit
    e.append(B((6.6, 10.0, 8.0), (7.2, 11.6, 9.0), "rp_black"))
    e += octagon_axis("z", (8.0, 8.0), 0.6, 10.4, 20.5, "rp_galv")           # the arm to the mast
    e.append(B((7.0, 5.6, 19.6), (9.0, 10.4, 20.9), "rp_galv_dark"))         # the clamp on the leg
    e += octagon_axis("z", (8.0, 10.6), 0.3, 9.0, 20.0, "rp_cable")
    return e


def rf_cabinet():
    """The outdoor RF cabinet (two blocks high, door north): plinth, body, roof cap with drip, a 3D door (leaf, hinges,
    swing handle) with its cooling unit's grille; the cable bridge off its top east over the next block to the mast,
    on a post."""
    e = [B((1.0, 0, 1.0), (15.0, 2.0, 15.0), "rp_galv"),
         B((1.6, 2.0, 1.6), (14.4, 28.0, 14.4), "rp_cab"),
         B((1.0, 28.0, 1.0), (15.0, 29.0, 15.0), "rp_cab"),
         B((1.2, 29.0, 1.2), (14.8, 29.6, 14.8), "rp_cab")]
    # the door on the north face: gasket, leaf, hinges (west), swing handle (east), the grille of its cooling unit
    e.append(B((2.0, 2.6, 1.35), (14.0, 27.4, 1.6), "rp_gasket"))
    e.append(B((2.25, 2.85, 0.85), (13.75, 27.15, 1.35), "rp_cab"))
    for y in (6.0, 23.0):
        e.append(B((1.4, y - 1.5, 0.4), (2.6, y + 1.5, 0.85), "rp_galv_dark"))
    e.append(B((11.6, 12.0, 0.6), (12.8, 17.0, 0.85), "rp_black"))
    e.append(B((12.0, 12.4, 0.2), (12.4, 16.0, 0.6), "rp_black"))
    e.append(B((4.0, 16.0, 0.7), (10.4, 25.0, 0.85), {"*": "rp_galv_dark", "north": "rp_louvre"}))
    e.append(B((4.0, 5.0, 0.7), (10.4, 9.0, 0.85), {"*": "rp_galv_dark", "north": "rp_louvre"}))
    e.append(label_z(0.85, -1, 4.4, 10.0, 11.0, 14.4, pic("rf_cab", 5.6, 3.4, plate(["RF CABINET", "DRONE LINK", "48 V DC"]))))
    # the cable bridge: a ladder tray off its top going east over the next block to the mast, the feeders on it
    for z in (5.2, 10.8):
        e.append(B((10.0, 29.6, z - 0.3), (31.7, 31.0, z + 0.3), "rp_galv"))
    for x in range(12, 31, 4):
        e.append(B((x, 29.6, 5.5), (x + 0.6, 30.0, 10.5), "rp_galv"))
    for z in (6.6, 8.0, 9.4):
        e += octagon_axis("x", (z, 30.4), 0.4, 7.0, 31.7, "rp_cable")
    # the bridge's post beside the cabinet
    e.append(B((22.0, 0.0, 7.4), (23.2, 29.6, 8.6), "rp_galv"))
    e.append(B((21.2, 0.0, 6.6), (24.0, 0.6, 9.4), "rp_galv_dark"))
    return e


# ------------------------------------------------------------------ the smaller pieces
def ring4(x0, x1, z0, z1, ix0, ix1, iz0, iz1, y0, y1, tex):
    """A square ring (outer x0..x1, z0..z1; hole ix0..ix1, iz0..iz1) as four boxes that do not overlap."""
    return [B((x0, y0, z0), (x1, y1, iz0), tex), B((x0, y0, iz1), (x1, y1, z1), tex),
            B((x0, y0, iz0), (ix0, y1, iz1), tex), B((ix1, y0, iz0), (x1, y1, iz1), tex)]


def duct_roof_curb():
    """The hv_duct_riser's duct standing in a roof curb: curb walls with counter-flashing, a storm collar."""
    e = [B((1.5, 0, 1.5), (14.5, 16, 14.5), {"*": "hv_galv", "up": None, "down": None})]
    # the riser's top flange (as hv_duct_riser), so a duct block above joins it
    e += ring4(0.9, 15.1, 0.9, 15.1, 1.5, 14.5, 1.5, 14.5, 15.75, 16, "hv_galv_dark")
    e += ring4(0.2, 15.8, 0.2, 15.8, 1.5, 14.5, 1.5, 14.5, 0, 6.4, "rp_galv")
    e += ring4(0.05, 15.95, 0.05, 15.95, 1.5, 14.5, 1.5, 14.5, 6.4, 6.9, "rp_galv_dark")
    e += ring4(1.2, 14.8, 1.2, 14.8, 1.5, 14.5, 1.5, 14.5, 6.9, 7.7, "rp_galv_dark")
    return e


def exhaust_fan(kind="down", tag="EF-1"):
    """A centrifugal roof exhaust fan on its curb. kind: down (spun hood), up, grease (up + grease box)."""
    e = ring4(0.5, 15.5, 0.5, 15.5, 2.0, 14.0, 2.0, 14.0, 0, 6.2, "rp_galv")         # insulated curb
    e.append(B((2.0, 5.0, 2.0), (14.0, 6.2, 14.0), "rp_black"))                        # the curb's throat (dark)
    e += ring4(0.2, 15.8, 0.2, 15.8, 1.4, 14.6, 1.4, 14.6, 6.2, 6.8, "rp_galv_dark")   # counter-flashing
    e.append(B((1.0, 6.8, 1.0), (15.0, 7.8, 15.0), "rp_alu"))                          # the fan's curb cap
    # its disconnect on the curb's east side, conduit up from the roof
    e.append(B((15.5, 2.0, 5.5), (16.6, 5.4, 8.5), "rp_cab"))
    e.append(B((16.6, 3.0, 6.4), (16.9, 4.4, 7.6), "rp_red"))
    e.append(B((15.6, 0, 9.4), (16.3, 2.6, 10.1), "rp_galv_dark"))
    if kind == "down":
        e += octagon_prism(8, 8, 5.6, 7.8, 10.0, "rp_screen")                            # bird screen ring
        e += octagon_prism(8, 8, 7.6, 10.0, 10.5, "rp_alu")                              # the hood's skirt
        ys = [10.5, 11.1, 11.7, 12.2, 12.7, 13.15, 13.55, 13.9, 14.2, 14.45, 14.8]
        for y0, y1 in zip(ys, ys[1:]):
            t = (y0 - 10.5) / 4.5
            e += octagon_prism(8, 8, max(1.4, 7.4 * math.sqrt(max(0.0, 1 - t * t))), y0, y1, "rp_alu")
        e += octagon_prism(8, 8, 0.7, 14.8, 15.5, "rp_galv_dark")                        # lifting eye
        e.append(label_x(15.5, 1, 9.0, 13.0, 3.2, 5.0, pic("ef_" + tag, 4, 1.8, stencil(tag, bg=(176, 180, 182)))))
    else:
        # upblast: a square base, the windband (a squat cylinder, hollow), the wheel's shroud and the motor cover
        # inside it, a bird screen over the top, a rolled rim
        e.append(B((1.6, 7.8, 1.6), (14.4, 9.0, 14.4), "rp_alu"))
        e += octagon_ring(8, 8, 6.2, 0.4, 9.0, 14.6, "rp_alu")
        e += octagon_ring(8, 8, 6.6, 0.3, 14.2, 14.9, "rp_galv_dark")
        e += octagon_prism(8, 8, 6.2, 9.0, 11.4, "rp_black")
        e += octagon_prism(8, 8, 2.6, 11.4, 13.2, "rp_alu")
        e += octagon_prism(8, 8, 1.6, 13.2, 13.8, "rp_galv_dark")
        e.append(B((1.4, 14.6, 1.4), (14.6, 14.75, 14.6), {"up": "rp_pic_guard!", "down": "rp_pic_guard!"}))
        # the motor's cooling tube and the wiring box on the windband's west side
        e.append(B((0.4, 10.0, 6.6), (1.4, 13.0, 9.4), "rp_cab"))
        if kind == "grease":
            e.append(B((14.0, 8.0, 6.4), (16.2, 10.4, 9.6), "rp_galv"))                 # the grease box
            e.append(B((15.6, 6.8, 7.6), (16.4, 8.0, 8.4), "rp_galv_dark"))
        e.append(label_x(14.4, 1, 2.4, 6.2, 7.9, 8.9, pic("ef_" + tag, 3.8, 1.0, stencil(tag, bg=(176, 180, 182)))))
    return e


def pipe_support():
    """The insulated pipe of hv_chw_pipe (along z, centre 8, 8) on a roof pipe support: a recycled-rubber base, a
    strut post and a pipe clamp."""
    e = [B((3.5, 0, 4.5), (12.5, 2.2, 11.5), "rp_rubber"),
         B((6.6, 2.2, 6.6), (9.4, 5.4, 9.4), "rp_galv"),
         B((4.2, 5.4, 6.8), (11.8, 5.9, 9.2), "rp_galv_dark"),
         B((5.9, 5.9, 7.4), (6.24, 9.9, 8.6), "rp_galv_dark"),
         B((9.76, 5.9, 7.4), (10.1, 9.9, 8.6), "rp_galv_dark"),
         B((6.24, 9.76, 7.4), (9.76, 10.1, 8.6), "rp_galv_dark")]
    return e


def doghouse(n=5, boots=None):
    """A pipe penetration box along z (n blocks), pipes coming in on its west side (one in every block) through
    rubber boots; a sloped cap, galvanized."""
    L = 16 * n
    e = [B((2.0, 0, 0.6), (14.0, 12.0, L - 0.6), "rp_galv"),
         B((1.4, 12.0, 0.0), (14.6, 13.0, L), "rp_galv_dark"),
         B((1.4, 13.0, 0.0), (8.0, 13.8, L), "rp_galv_dark")]
    for k in (boots if boots is not None else range(n)):
        cz = 16 * k + 8
        # boot: a stepped collar round the pipe where it enters (pipe along x at y 8, z cz)
        e += octagon_axis("x", (cz, 8.0), 2.7, 1.2, 2.0, "rp_rubber")
        e += octagon_axis("x", (cz, 8.0), 2.1, 0.4, 1.2, "rp_rubber")
        e += octagon_axis("x", (cz, 8.0), 1.76, 0.0, 0.4, "hv_jacket")
    e.append(label_z(0.6, -1, 4.0, 12.0, 4.0, 6.6, pic("doghouse", 8, 2.6, plate(["CHILLED WATER", "DATA HALL"]))))
    return e


def crossover(n_deck=5, h=16.7):
    """A steel crossover along z over pipes on the roof: a 45-degree stair up in block 0 (four treads, the deck the
    fifth step), the deck over the next n_deck blocks, a stair down in the last block. The deck is a grating between
    two 0.2 m channel stringers that span from stair to stair (their tops the toe boards), all of it in the blocks' upper
    row so the pipes keep the blocks under it; posts and stringers stand only in the stair blocks. Yellow handrails
    1.0 m over the nosings and the deck, a mid rail."""
    e = []
    L = 16.0 * (n_deck + 2)
    r = h / 5                       # rise = going
    d0, d1 = 0.75 + 4 * r, L - 0.75 - 4 * r        # the deck's ends (in the stair blocks)
    e.append(B((1.6, h - 0.6, d0), (14.4, h, d1), {"*": "rp_grating"}))
    for x0, x1 in ((0.4, 1.6), (14.4, 15.6)):
        e.append(B((x0, 16.05, d0 - 0.6), (x1, h + 2.6, d1 + 0.6), "rp_galv"))          # the channel stringers
    for end in (0, 1):
        for i in range(4):
            y = r * (i + 1)
            za = 0.75 + r * i if end == 0 else L - 0.75 - r * (i + 1)
            zb = za + r
            e.append(B((1.6, y - 0.6, za), (14.4, y, zb), {"*": "rp_grating"}))
            n0 = za if end == 0 else zb - 0.5
            e.append(B((1.6, y - 0.9, n0), (14.4, y - 0.6, n0 + 0.5), "rp_yellow"))     # nosing
        # the stair's stringers and handrails, turned 45 degrees about x round their middles
        ang = -45 if end == 0 else 45
        zs = 7.4 if end == 0 else L - 7.4                 # stringer: along the nosings, 1.8 px under them
        zr = 6.4 if end == 0 else L - 6.4
        ys, yr_top, yr_mid = 7.4, 7.4 + 1.8 + 16.4 + 0.05 - 0.4, 7.4 + 1.8 + 8.2
        for x0, x1 in ((0.4, 1.6), (14.4, 15.6)):
            e.append(B((x0, ys - 1.6, zs - 9.9), (x1, ys + 1.6, zs + 9.9), "rp_galv", ("x", ang, ((x0 + x1) / 2, ys, zs))))
            e.append(B((x0 + 0.2, yr_top - 0.4, zr - 8.6), (x1 - 0.2, yr_top + 0.4, zr + 8.6), "rp_yellow",
                       ("x", ang, ((x0 + x1) / 2, yr_top, zr))))
            e.append(B((x0 + 0.2, yr_mid - 0.4, zr - 8.6), (x1 - 0.2, yr_mid + 0.4, zr + 8.6), "rp_yellow",
                       ("x", ang, ((x0 + x1) / 2, yr_mid, zr))))
            # the post at the stair's foot and the post at the deck's end (from the roof up to the top rail)
            zf = 1.4 if end == 0 else L - 2.2
            e.append(B((x0 + 0.2, r, zf), (x1 - 0.2, r + 16.8, zf + 0.8), "rp_yellow"))
            zt = d0 - 0.8 if end == 0 else d1
            e.append(B((x0 + 0.2, 0.6, zt), (x1 - 0.2, 16.05, zt + 0.8), "rp_galv"))
            e.append(B((x0 - 0.2, 0, zt - 0.4), (x1 + 0.2, 0.6, zt + 1.2), "rp_rubber"))
            e.append(B((x0 + 0.2, h + 2.6, zt), (x1 - 0.2, h + 16.8, zt + 0.8), "rp_yellow"))
    top = h + 16.4
    for x0, x1 in ((0.4, 1.6), (14.4, 15.6)):
        for k in range(1, n_deck + 1):
            z = 16.0 * k + 8
            e.append(B((x0 + 0.2, h + 2.6, z - 0.4), (x1 - 0.2, top, z + 0.4), "rp_yellow"))
        # the top rail stands above the blocks' upper row: in a length to each block (each kept with its block)
        cuts = [d0 - 0.8] + [16.0 * k for k in range(1, n_deck + 2)] + [d1 + 0.8]
        for za, zb in zip(cuts, cuts[1:]):
            e.append(B((x0 + 0.1, top, za), (x1 - 0.1, top + 0.8, zb), "rp_yellow"))
        e.append(B((x0 + 0.1, h + 8.6, d0 - 0.8), (x1 - 0.1, h + 9.4, d1 + 0.8), "rp_yellow"))
    return e


# ------------------------------------------------------------------ the pieces
EF_TAGS = [f"EF-{n}" for n in range(1, 7)]
CROSSOVER_CELLS = [(0, 0, 0), (0, 0, 6)] + [(0, 1, z) for z in range(7)]     # the stairs' blocks, the row over the pipes
# piece: (its cells, anchor first, as it faces north; its variants' tags; what makes a variant's elements)
PIECES = {
    "rooftop_ahu": (AHU_CELLS, ["AHU-R1", "AHU-R2"], ahu),
    "dry_cooler": (DC_CELLS, ["DC-1", "DC-2"], cooler),
    "duct_roof_curb": ([(0, 0, 0)], [None], lambda tag: duct_roof_curb()),
    "roof_exhaust_fan": ([(0, 0, 0)], EF_TAGS, lambda tag: exhaust_fan("down", tag)),
    "roof_upblast_fan": ([(0, 0, 0)], EF_TAGS, lambda tag: exhaust_fan("up", tag)),
    "kitchen_exhaust_fan": ([(0, 0, 0)], EF_TAGS, lambda tag: exhaust_fan("grease", tag)),
    "roof_pipe_support": ([(0, 0, 0)], [None], lambda tag: pipe_support()),
    "pipe_doghouse": ([(0, 0, 0), (0, 0, 1)], [None], lambda tag: doghouse(2)),
    "pipe_crossover": (CROSSOVER_CELLS, [None], lambda tag: crossover(5)),
    "comms_mast_base": ([(0, 0, 0)], [None], lambda tag: mast_section("base", cables=False)),
    "comms_mast": ([(0, 0, 0)], [None], lambda tag: mast_section("mid")),
    "comms_mast_tie": ([(0, 0, 0)], [None], lambda tag: mast_section("mid", ties=True)),
    "comms_mast_top": ([(0, 0, 0)], [None], lambda tag: mast_top()),
    "comms_dish": ([(0, 0, 0)], [None], lambda tag: dish()),
    "rf_cabinet": ([(0, 0, 0), (0, 1, 0), (1, 0, 0), (1, 1, 0)], [None], lambda tag: rf_cabinet()),
}
# what tools/room_kit.py reads: each multi-block piece's cells (its part n is cell n), and the dry cooler's fan rotors
# (pixels in its structure, facing north: centre x, y, z, hub and tip radius, blades) for client/RoomKitRotorRenderer
CELLS = {name: [list(c) for c in cells] for name, (cells, _, _) in PIECES.items() if len(cells) > 1}
ROTORS = {"dry_cooler": [[round(fx, 3), DC_DECK + 2.7, round(fz, 3), 2.1, 5.9, 5] for fx, fz in dc_fans()]}
DISPLAY = {"dry_cooler": "rotors"}
BLADE = "textures/entity/roof_fan_blade.png"      # the rotors' texture (a dark composite)


def _pack(assets):
    """The pictures into rp_atlas; their regions (uv, 16 = the atlas's side)."""
    img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    x = y = row = 0
    regions = {}
    for key, (draw, wpx, hpx) in sorted(PICS.items(), key=lambda kv: -kv[1][2]):
        tx = 8 if key == "guard" else 16
        w, h = max(1, round(wpx * tx)), max(1, round(hpx * tx))
        if x + w > ATLAS:
            x, y, row = 0, y + row + 2, 0
        if y + h > ATLAS:
            raise ValueError("roof_plant: the pictures do not fit the atlas")
        p = Image.new("RGBA", (w, h), (255, 255, 255, 255))
        draw(p)
        img.paste(p, (x, y))
        regions[key] = [round(v, 5) for v in (x * 16 / ATLAS, y * 16 / ATLAS, (x + w) * 16 / ATLAS, (y + h) * 16 / ATLAS)]
        x += w + 2
        row = max(row, h)
    img.save(assets / "textures/block/rp_atlas.png")
    return regions


def _finish(elements, regions, extra=()):
    """A part's elements as written: pictures pointed at their atlas regions, the lamps lit, the HVAC pipe's own
    elements (the pipe support's) added; a piece of a box with no face left (the inside of the air handler's casing,
    every face of it cut where the blocks meet) dropped."""
    elements = [e for e in elements if e.get("faces")]
    for e in elements:
        names = set()
        for spec in e.get("faces", {}).values():
            t = spec["texture"].lstrip("#")
            if t.startswith("rp_pic_"):
                spec["texture"] = "#rp_atlas"
                spec["uv"] = list(regions[t[len("rp_pic_"):]])
            names.add(spec["texture"].lstrip("#"))
        if names and names <= EMISSIVE:
            e["light_emission"] = 15
            e["shade"] = False
    out = list(elements) + [json.loads(json.dumps(x)) for x in extra]
    if extra:
        MW.finish_closed(out)          # the pipe's ends closed (its own model leaves them open, inside its flanges)
    return out


def _textures(elements, ns, extra_textures):
    names = sorted({spec["texture"].lstrip("#") for e in elements for spec in e.get("faces", {}).values()})
    out = {}
    for n in names:
        out[n] = extra_textures.get(n) or REUSE.get(n) or f"{ns}:block/{n}"
    out["particle"] = out.get("rp_galv") or out.get("rp_cab") or (out[names[0]] if names else f"{ns}:block/rp_galv")
    return out


def write(assets, ns="jugcraft"):
    """The pieces' textures, models (one per part, a variant's own only where its tag shows) and blockstates."""
    assets = Path(assets)
    for d in ("textures/block", "textures/entity", "models/block", "blockstates"):
        (assets / d).mkdir(parents=True, exist_ok=True)
    for name, make in MATERIALS.items():
        make().save(assets / f"textures/block/{name}.png")
    from roof_plant_art import blade
    blade().resize((16, 16)).save(assets / BLADE)
    PICS["guard"] = (guard, 13.6, 13.6)
    built = {}                    # piece -> variant -> part -> elements (tuples)
    for name, (cells, tags, make) in PIECES.items():
        built[name] = []
        for tag in tags:
            els = make(tag)
            built[name].append(els)
    regions = _pack(assets)
    pipe = json.loads((assets / "models/block/hv_chw_pipe.json").read_text())
    for name, (cells, tags, make) in PIECES.items():
        extra = pipe["elements"] if name == "roof_pipe_support" else ()
        extra_tex = {k: v for k, v in pipe["textures"].items() if k != "particle"} if name == "roof_pipe_support" else {}
        written = {}              # a model's JSON -> its name (a variant's parts the same as the first's share it)
        variants = {}
        for v, els in enumerate(built[name]):
            if len(cells) > 1:
                parts = MW.split_model(name, els, cells)
            else:
                part = [MW.element(*MW.unpack(t)[:3], rotation=MW.unpack(t)[3].get("rotation")) for t in els]
                MW.finish_elements(part)
                parts = [part]
            for i, part in enumerate(parts):
                part = _finish(part, regions, extra)
                model = {"textures": _textures(part, ns, extra_tex), "elements": part}
                key = json.dumps(model, sort_keys=True)
                if key not in written:
                    mname = name if len(cells) == 1 and v == 0 else (f"{name}_{i}" if v == 0 else f"{name}_v{v}_{i}" if len(cells) > 1 else f"{name}_v{v}")
                    written[key] = mname
                    (assets / f"models/block/{mname}.json").write_text(json.dumps(model, indent=1) + "\n")
                for f, y in FACINGS.items():
                    props = [f"facing={f}"] + ([f"part={i}"] if len(cells) > 1 else []) + ([f"variant={v}"] if len(tags) > 1 else [])
                    variants[",".join(props)] = {"model": f"{ns}:block/{written[key]}", **({"y": y} if y else {})}
        (assets / f"blockstates/{name}.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")
    return sorted(PIECES)


if __name__ == "__main__":
    import sys
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "src/main/resources/assets/jugcraft"
    print(write(out))
