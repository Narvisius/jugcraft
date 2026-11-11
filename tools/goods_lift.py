"""The Tri-Pillar's goods lift and the dispatch room's pieces (work list item 20, take 1 approved by Justin 26 Oct 2026):
room kit pieces (tower/RoomKit; tools/room_kit.py runs write() as the "dispatch" kit).

- goods_lift_door: a landing door 3 wide and 4 high, placed whole from one item facing the player (the top block its
  housing): jambs, header and housing in anthracite steel (as the wall panels), the leaves' guides, the aluminium sill,
  the call station on the jamb at the walker's right (up lit, down, stop, the key switch), the floor indicator's bezel in
  the header and the GOODS LIFT placard on the housing. Its block is tower/GoodsLiftDoorBlock (display "goods_lift");
  client/GoodsLiftRenderer draws the rest live from the door's own part models past its 12 cells (MESH): the vertical
  bi-parting leaves (the upper rises into the housing, the lower drops into the sill; a wired-glass vision panel, the
  hazard edge, the rubber astragal), the indicator's floor and arrow, and, at the lowest door of a shaft, the car: a
  cantilevered ("rucksack") 3 x 3 car on two T-rails on the shaft's back wall, chequer plate, toe guards, a lifting mesh
  gate on its front and on either side, a car light, and two pallets of drone crates while it goes up loaded (where a
  cargo hall serves the lift, client/warehouse/CargoHallRenderer's pallets ride in it instead). With no door over the
  lowest, the car runs up to the top of the well (the Tri-Pillar's house, the supply pickup on its top, layout 40).
- lift_rail: one block of a T-rail on a wall (the car's guides; one in each of the shaft's back corners).
- energy_port_plate, cargo_port_plate: the yellow plate over an exchange's port on the outside wall.
- crate_pallet: a 1200 x 1200 pallet of drone crates (2 x 2 blocks; the Tri-Pillar's dispatch had two until layout 40).
- cargo_case_packer: the cargo hall's case packer (the cargo area rework, Justin 27 October 2026): the warehouse's Intake
  Case Packer (tools/warehouse.py intake: 3 wide, 2 deep, 2 high) as a look-only piece, its models the warehouse
  piece's own; the cargo exchange feeds it through the wall at its back, client/warehouse/CargoHallRenderer draws its
  cases, HMI and stack light, the stacker drone and the pallet drone live.

Structure space, each piece facing north (its front at z 0), model pixels (1 px = 6.25 cm). The door's live parts are in
its own frame: the door x 0..48, y 0..64, z 0..16; the car's well behind it, x 0..48, z 16..64, its floor at y 0 at a
landing (the pit below, y -16..0). The plain materials are 64 x 64; the pictures (the placard, the port plates) are
packed into gl_atlas, each inside one block.

Run with the room kits: python3 tools/room_kit.py
"""
from __future__ import annotations

import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import model_writer as MW
from roof_plant_art import B, _fit, painted, plate, round_axis, rubber

DOOR, RAIL, PALLET = "goods_lift_door", "lift_rail", "crate_pallet"
PACKER = "cargo_case_packer"
# Its cells, the piece facing north (its front north), anchor first: part (row * 2 + depth) * 3 + column as the
# warehouse's pieces number them (tools/warehouse_bp.piece), the anchor its back row's east end. A room kit facing F
# shows the warehouse piece facing the other way (the warehouse's "the way its user looks"): its models turned 180 more.
PACKER_CELLS = [(-c, r, -k) for r in range(2) for k in range(2) for c in range(3)]
INFEED = "cargo_infeed"
# Its cells, facing north: the spout over the packer's hopper (the anchor), the conveyor's head over it, its run through the
# wall and over the aisle behind, back to the cargo exchange (+z).
INFEED_CELLS = [(0, 0, 0), (0, 1, 0), (0, 1, 1), (0, 1, 2), (0, 1, 3)]
INFEED_BELT_Y = 22.9            # the belt's top (px over the anchor's floor), where the goods ride (CargoHallRenderer)
PLATES = {"energy": "energy_port_plate", "cargo": "cargo_port_plate"}
ATLAS = 512
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
PICS = {}


def pic(key, wpx, hpx, draw):
    PICS.setdefault(key, (draw, wpx, hpx))
    return f"gl_pic_{key}!"


# ------------------------------------------------------------------ materials
TS = 64


def _rng(seed):
    return np.random.default_rng(seed)


def _img(a, mode="RGB"):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), mode)


def tread():
    """Aluminium chequer plate: raised bars in a herringbone."""
    a = np.zeros((TS, TS, 3)) + np.array([170, 172, 174], float)
    a += _rng(31).normal(0, 3, (TS, TS, 1))
    for cy in range(0, TS, 16):
        for cx in range(0, TS, 16):
            for t in range(-5, 6):
                for w in (-1, 0, 1):
                    for (x, y) in ((cx + 4 + t * 0.5 + w, cy + 4 - t * 0.5), (cx + 12 + t * 0.5 + w, cy + 12 + t * 0.5)):
                        a[int(y) % TS, int(x) % TS] *= 1.18
    return _img(a)


def mesh():
    """Expanded steel mesh: diamonds, open between (alpha)."""
    im = np.zeros((TS, TS, 4))
    for y in range(TS):
        for x in range(TS):
            u, v = (x % 16) - 8, (y % 10) - 5
            if abs(abs(u) / 8 + abs(v) / 5 - 1) < 0.22:
                im[y, x] = [92, 96, 100, 255]
    return _img(im, "RGBA")


def crate():
    """The drone crate's wood (as drone_depot.png's crate): boards and a frame of darker battens, a brace across."""
    a = np.zeros((TS, TS, 3)) + np.array([150, 104, 62], float)
    for y in range(TS):
        a[y] *= 0.92 + 0.08 * math.sin(y * 0.9)
    for x in range(0, TS, 16):
        a[:, x] *= 0.6
    a += _rng(41).normal(0, 5, (TS, TS, 1))
    e = 7
    a[:e] *= 0.62
    a[-e:] *= 0.62
    a[:, :e] *= 0.62
    a[:, -e:] *= 0.62
    for k in range(TS):
        for w in range(-3, 4):
            if 0 <= k + w < TS:
                a[k, k + w] *= 0.8
    return _img(a)


def pallet_wood():
    a = np.zeros((TS, TS, 3)) + np.array([184, 150, 104], float)
    for y in range(TS):
        a[y] *= 0.94 + 0.06 * math.sin(y * 0.7 + 1)
    a += _rng(42).normal(0, 5, (TS, TS, 1))
    return _img(a)


def hazard():
    """Yellow and black diagonal stripes (they run on from block to block)."""
    im = Image.new("RGB", (TS, TS), (245, 196, 22))
    d = ImageDraw.Draw(im)
    for x in range(-TS, 2 * TS, 32):
        d.polygon([(x, TS), (x + 16, TS), (x + 16 + TS, 0), (x + TS, 0)], fill=(24, 24, 26))
    return im


MATERIALS = {
    "gl_frame": lambda: painted((56, 60, 64), 51, 1.6, 0.4),       # RAL 7016 anthracite, as the wall panels
    "gl_frame_dark": lambda: painted((36, 38, 41), 52, 1.4, 0.3),
    "gl_leaf": lambda: painted((150, 156, 160), 53, 1.8, 0.6),     # the leaves: galvanized, painted
    "gl_leaf_dark": lambda: painted((120, 126, 130), 54, 1.6, 0.4),
    "gl_car": lambda: painted((228, 182, 34), 55, 2.0, 0.4),       # the car's frame: safety yellow
    "gl_tread": tread,
    "gl_mesh": mesh,
    "gl_crate": crate,
    "gl_pallet": pallet_wood,
    "gl_hazard": hazard,
    "gl_rail": lambda: painted((70, 74, 78), 56, 1.2, 0.2),
    "gl_rail_face": lambda: painted((190, 194, 198), 57, 1.0, 0.0),  # the rail's machined blade
    "gl_cab": lambda: painted((214, 216, 212), 58, 1.6, 0.4),
    "gl_led": lambda: painted((255, 246, 226), 59, 1.0, 0.0),
    "gl_green": lambda: painted((60, 255, 90), 60, 1.0, 0.0),
    "gl_red": lambda: painted((178, 34, 30), 61, 2.0, 0.4),
    "gl_black": lambda: painted((26, 27, 29), 62, 1.5, 0.3),
    "gl_glass": lambda: painted((70, 90, 100), 63, 1.0, 0.0),
    "gl_alu": lambda: painted((186, 190, 194), 64, 2.0, 2.0),
    "gl_galv": lambda: painted((132, 136, 138), 65, 3.0, 0.5),
    "gl_rubber": rubber,
}
EMISSIVE = {"gl_led", "gl_green"}


def xy_ring(x0, x1, y0, y1, ix0, ix1, iy0, iy1, z0, z1, tex):
    """A frame in the x-y plane round a hole (four boxes that do not overlap)."""
    return [B((x0, y0, z0), (x1, iy0, z1), tex), B((x0, iy1, z0), (x1, y1, z1), tex),
            B((x0, iy0, z0), (ix0, iy1, z1), tex), B((ix1, iy0, z0), (x1, iy1, z1), tex)]


# ------------------------------------------------------------------ the landing door (3 x 4 blocks) and its live parts
DOOR_CELLS = [[x, y, 0] for y in range(4) for x in range(3)]
# the live parts (client/GoodsLiftRenderer draws them): each cut into blocks, every block one of the door's part values
# past its 12 cells; jugcraft/goods_lift.json lists each part's blocks as [part, dx, dy, dz] in the door's frame
MESH = ("leaf_lower", "leaf_upper", "car", "gate_front", "gate_left", "gate_right", "load")
MESH_FILE = "jugcraft/goods_lift.json"
LEAF_DROP, LEAF_RISE = 19.5, 20.0      # how far the leaves move open (px)
GATE_RISE = 24.0                       # how far a car gate lifts at a landing (px)
INDICATOR = (17.0, 41.8, 31.0, 46.4, 0.2)   # the indicator's glass: x0, y0, x1, y1, its face z (px)


def door_frame():
    fr, frd = "gl_frame", "gl_frame_dark"
    e = [B((0, 0, 0.5), (4, 48, 6), fr), B((44, 0, 0.5), (48, 48, 6), fr), B((4, 40, 0.5), (44, 48, 6), fr)]
    e.append(B((0, 48, 0.5), (48, 64, 16), fr))                             # the housing (the upper leaf goes up in it)
    e.append(B((0.4, 63.4, 0.2), (47.6, 64, 0.5), frd))                     # its top trim
    e += [B((4, 0, 6), (5.5, 40, 10), frd), B((42.5, 0, 6), (44, 40, 10), frd), B((4, 40, 6), (44, 48, 10), frd)]
    e.append(B((4, 0, 0.5), (44, 0.5, 10), "gl_alu"))                       # the sill
    # the indicator's bezel (its figures are drawn live)
    x0, y0, x1, y1, z = INDICATOR
    e.append(B((x0, y0, z), (x1, y1, 0.5), "gl_black"))
    # the call station on the jamb at the walker's right: up (lit), down, stop, the key switch
    e.append(B((0.5, 16.5, 0.15), (3.5, 31.5, 0.5), "gl_cab"))
    e.append(B((1.3, 27.6, 0.0), (2.7, 29.0, 0.15), "gl_green"))
    e.append(B((1.3, 25.4, 0.0), (2.7, 26.8, 0.15), "gl_black"))
    e.append(B((1.1, 20.6, 0.0), (2.9, 22.4, 0.15), "gl_red"))
    e.append(B((1.6, 18.0, 0.0), (2.4, 18.8, 0.15), "gl_galv"))
    # the placard on the housing (inside the middle block)
    e.append(B((16.6, 51, 0.3), (31.4, 61, 0.5), {"*": fr, "north": pic("placard", 14.8, 10, plate(
        ["GOODS LIFT", "1000 kg", "NO PASSENGERS"], bg=(240, 240, 234)))}))
    return e


def leaf_lower():
    lf, lfd = "gl_leaf", "gl_leaf_dark"
    e = [B((5.5, 0.5, 6.5), (42.5, 20, 8.5), lf)]
    e.append(B((5.5, 15.4, 6.3), (42.5, 19.6, 6.5), {"*": lf, "north": "gl_hazard"}))
    for y in (5.0, 10.0):
        e.append(B((6.5, y, 6.2), (41.5, y + 1.0, 6.5), lfd))
    e.append(B((5.5, 19.6, 6.2), (42.5, 20.4, 8.8), "gl_rubber"))
    return e


def leaf_upper():
    lf, lfd = "gl_leaf", "gl_leaf_dark"
    e = xy_ring(5.5, 42.5, 20.4, 40, 18, 30, 27, 34, 6.5, 8.5, lf)
    e += xy_ring(17.4, 30.6, 26.4, 34.6, 18, 30, 27, 34, 6.2, 6.5, "gl_black")
    e.append(B((18, 27, 7.4), (30, 34, 7.6), "gl_glass"))
    e.append(B((6.5, 37.0, 6.2), (41.5, 38.0, 6.5), lfd))
    e.append(B((6.5, 22.5, 6.2), (16.5, 23.5, 6.5), lfd))
    e.append(B((31.5, 22.5, 6.2), (41.5, 23.5, 6.5), lfd))
    return e


def car():
    """The car in the door's frame: its well x 0..48, z 16..64 (the rails on the back wall at z 64), floor top y 0."""
    y = "gl_car"
    e = [B((1, -3, 17), (47, 0, 58.0), {"*": y, "up": "gl_tread"})]
    e.append(B((1, -9, 17), (47, -3, 17.6), y))                                       # toe guard under the front
    e.append(B((1, -9, 17.6), (1.6, -3, 58.0), y))                                    # and the two sides
    e.append(B((46.4, -9, 17.6), (47, -3, 58.0), y))
    for sx in (8, 40):                                                                # the back frame on the rails
        e.append(B((sx - 1.5, -3, 58.0), (sx + 1.5, 42, 61.0), y))
        for gy in (-2.5, 38.5):
            e.append(B((sx - 2.2, gy, 61.0), (sx + 2.2, gy + 3.2, 63.2), "gl_black"))  # guide shoes round the blades
        e.append(B((sx - 1, -6.5, 18.0), (sx + 1, -3, 58.0), y))                     # the brace under the floor
    e.append(B((6.5, 42, 58.0), (41.5, 45, 61.0), y))                                 # crosshead
    e.append(B((9.5, 18, 58.4), (38.5, 19.5, 61.0), y))
    e.append(B((18, 41.2, 56.6), (30, 42, 58.0), "gl_led"))                           # the car light
    for (x0, z0) in ((1.0, 17.0), (45.6, 17.0), (1.0, 56.6), (45.6, 56.6)):
        e.append(B((x0, 0, z0), (x0 + 1.4, 42, z0 + 1.4), y))                         # the gates' corner posts
    return e


def gate(side):
    y = "gl_car"
    if side == "front":
        return [B((2.4, 0, 17.2), (45.6, 18, 17.6), "gl_mesh"), B((2.4, 18, 17.0), (45.6, 19.4, 18.4), y)]
    x0 = 1.2 if side == "left" else 46.4
    xr = 1.0 if side == "left" else 45.6
    return [B((x0, 0, 18.4), (x0 + 0.4, 18, 56.6), "gl_mesh"), B((xr, 18, 18.4), (xr + 1.4, 19.4, 56.6), y)]


def pallet_load(x0, z0, layers=3, skip=()):
    """A 1200 x 1200 pallet (19.2 px square) at (x0, z0), drone crates (0.34 m, the tier-1 size) 3 x 3 a layer."""
    P = 19.2
    e = [B((x0, 0, z0), (x0 + P, 0.9, z0 + P), "gl_pallet")]
    for k in range(3):
        e.append(B((x0, 0.9, z0 + k * (P - 2.4) / 2), (x0 + P, 2.1, z0 + k * (P - 2.4) / 2 + 2.4), "gl_pallet"))
    e.append(B((x0, 2.1, z0), (x0 + P, 2.8, z0 + P), "gl_pallet"))
    c, gap = 5.44, 0.6
    span = 3 * c + 2 * gap
    cx, cz = x0 + (P - span) / 2, z0 + (P - span) / 2
    for layer in range(layers):
        for i in range(3):
            for j in range(3):
                if (layer, i, j) in skip:
                    continue
                x, z, yy = cx + i * (c + gap), cz + j * (c + gap), 2.8 + layer * (c + 0.05)
                e.append(B((x, yy, z), (x + c, yy + c, z + c), "gl_crate"))
    return e


def load():
    """Two pallets of crates side by side in the car, two crates on their own by the front."""
    e = pallet_load(4.0, 33.0) + pallet_load(24.8, 33.0, skip={(2, 2, 0), (2, 2, 1), (2, 2, 2)})
    for (x, z) in ((6.0, 20.0), (12.2, 20.6)):
        e.append(B((x, 0, z), (x + 5.44, 5.44, z + 5.44), "gl_crate"))
    return e


# ------------------------------------------------------------------ the single pieces
def rail():
    """A T-rail on the wall behind (its foot against the block's back), centred across the block."""
    e = [B((5, 0, 15.4), (11, 16, 16), "gl_rail"), B((7.5, 0, 13.4), (8.5, 16, 15.4), "gl_rail_face")]
    for y in (2, 10):
        e.append(B((5.4, y, 15.0), (10.6, y + 1.6, 15.4), "gl_galv"))              # the clips
    return e


def port_plate(kind):
    t = {"energy": ["ENERGY EXCHANGE", "POWER IN  >  CONNECT HERE"], "cargo": ["CARGO EXCHANGE", "ITEMS IN  >  CONNECT HERE"]}[kind]
    return [B((1.0, 4.0, 15.6), (15.0, 12.0, 16.0), {"*": "gl_galv", "north": pic("port_" + kind, 14, 8, plate(t, bg=(250, 204, 20)))})]


PALLET_CELLS = [[0, 0, 0], [1, 0, 0], [0, 0, 1], [1, 0, 1]]


def pallet():
    return pallet_load(6.4, 6.4, skip={(2, 2, 2), (2, 1, 2)})


def infeed():
    """The Case Packer's infeed (Justin, 27 October 2026: "connect the case packager to the cargo exchange"): goods out
    of the cargo exchange's back under a discharge hood onto a troughed belt conveyor, over the intake room's aisle, through
    the room's wall in a galvanized sleeve (the wall panel cut round it, flashed both sides), to the head over the packer:
    the drive on its side, the head under a stainless hood, a spout down into the packer's hopper. Its frame stands on a
    bracket on the exchange's back and in the wall. Facing north: the spout at z 0..16, the exchange's back at z 64."""
    PANEL, LINER = "ex_imp_anthracite", "ex_imp_liner"
    out = []
    # the spout into the hopper (its open end dark), its flange, the head hood, an inspection door on it
    out += [B((4.2, 0.4, 3.2), (11.8, 14.0, 10.8), {"*": "gl_alu", "down": "gl_black"}),
            B((3.6, 14.0, 2.6), (12.4, 15.0, 11.4), "gl_galv"),
            B((2.4, 15.0, 1.0), (13.6, 27.0, 10.0), "gl_alu"),
            B((2.2, 27.0, 0.8), (13.8, 27.6, 10.2), "gl_galv"),
            B((13.6, 18.0, 3.0), (13.9, 25.0, 8.0), "gl_galv"),
            B((13.9, 21.0, 5.0), (14.2, 22.0, 6.0), "gl_black")]
    # the drive: a gearbox and its motor on the side, at the head
    out += [B((14.2, 18.6, 1.6), (15.8, 22.6, 6.6), "gl_frame_dark")]
    out += round_axis("z", (15.0, 20.6), 0.8, 6.6, 11.4, "gl_frame_dark")
    # the conveyor: its two side channels, the slider bed, the belt over it and back under, the tail drum, the skirts
    out += [B((2.0, 18.0, 10.0), (3.0, 24.6, 63.0), "gl_rail"), B((13.0, 18.0, 10.0), (14.0, 24.6, 63.0), "gl_rail"),
            B((3.0, 21.6, 10.0), (13.0, 22.4, 57.5), "gl_galv"),
            B((3.2, 22.4, 10.0), (12.8, INFEED_BELT_Y, 61.0), "gl_rubber"),
            B((3.2, 18.4, 12.0), (12.8, 18.8, 58.5), "gl_rubber"),
            B((3.0, 22.9, 12.0), (3.6, 25.2, 54.5), "gl_galv"), B((12.4, 22.9, 12.0), (13.0, 25.2, 54.5), "gl_galv")]
    out += round_axis("x", (60.5, 20.6), 1.8, 3.2, 12.8, "gl_rubber")
    for z in (36.0, 46.0):                  # cross members under the bed over the aisle
        out.append(B((3.0, 19.0, z), (13.0, 19.8, z + 1.2), "gl_rail"))
    # the wall it goes through (z 16..32): the wall panel (as wall_panel: face, liner core, sealant) round the opening
    hole = (1.5, 14.5, 17.5, 27.0)          # x0, x1, y0, y1
    x0, x1, y0, y1 = hole
    out += [B((0.0, 16.0, 16.9), (16.0, y0, 32.0), LINER), B((0.0, y1, 16.9), (16.0, 32.0, 32.0), LINER),
            B((0.0, y0, 16.9), (x0, y1, 32.0), LINER), B((x1, y0, 16.9), (16.0, y1, 32.0), LINER)]
    out += [B((0.13, 16.5, 16.2), (15.87, y0, 17.0), PANEL), B((0.13, y1, 16.2), (15.87, 31.87, 17.0), PANEL),
            B((0.13, y0, 16.2), (x0, y1, 17.0), PANEL), B((x1, y0, 16.2), (15.87, y1, 17.0), PANEL),
            B((0.13, 16.13, 16.6), (15.87, 16.5, 17.0), "ex_sealant")]
    # its galvanized sleeve lining the opening, standing proud of both faces, and the flashing round it on both sides
    out += [B((x0, y0, 15.9), (x1, y0 + 0.3, 32.3), "gl_galv"), B((x0, y1 - 0.3, 15.9), (x1, y1, 32.3), "gl_galv"),
            B((x0, y0 + 0.3, 15.9), (x0 + 0.3, y1 - 0.3, 32.3), "gl_galv"), B((x1 - 0.3, y0 + 0.3, 15.9), (x1, y1 - 0.3, 32.3), "gl_galv")]
    for z0, z1 in ((15.8, 16.2), (32.0, 32.4)):
        out += [B((0.9, y0 - 0.6, z0), (15.1, y0, z1), "gl_galv"), B((0.9, y1, z0), (15.1, y1 + 0.6, z1), "gl_galv"),
                B((0.9, y0, z0), (x0, y1, z1), "gl_galv"), B((x1, y0, z0), (15.1, y1, z1), "gl_galv")]
    # at the exchange: the bracket bolted to its back with two braces under the frame, the discharge hood over the tail
    out += [B((1.0, 16.6, 63.5), (15.0, 31.0, 63.9), "gl_frame_dark"),
            B((2.2, 16.6, 58.0), (3.0, 18.0, 63.5), "gl_frame_dark"), B((13.0, 16.6, 58.0), (13.8, 18.0, 63.5), "gl_frame_dark"),
            B((2.4, 28.6, 54.0), (13.6, 31.6, 63.5), {"*": "gl_alu", "north": "gl_black"}),
            B((2.4, 25.2, 54.0), (3.0, 28.6, 63.5), "gl_alu"), B((13.0, 25.2, 54.0), (13.6, 28.6, 63.5), "gl_alu")]
    for x in (2.2, 13.0):
        for y in (19.0, 29.0):
            out.append(B((x + 0.1, y, 63.2), (x + 0.7, y + 0.6, 63.5), "gl_galv"))      # the bracket's bolts
    return out


CELLS = {DOOR: DOOR_CELLS, PALLET: PALLET_CELLS, PACKER: PACKER_CELLS, INFEED: INFEED_CELLS}
DISPLAY = {DOOR: "goods_lift"}


# ------------------------------------------------------------------ writing
def _one_block(elements, name):
    for frm, to, tex, *_ in elements:
        texs = tex.values() if isinstance(tex, dict) else [tex]
        if any(str(t).startswith("gl_pic_") for t in texs):
            for a in range(3):
                if math.floor(frm[a] / 16) != math.floor((to[a] - 1e-6) / 16):
                    raise ValueError(f"goods_lift {name}: the picture {sorted(map(str, texs))} at {frm}..{to} crosses a block")


def _pack(assets):
    img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    x = y = row = 0
    regions = {}
    for key, (draw, wpx, hpx) in sorted(PICS.items(), key=lambda kv: (-kv[1][2], kv[0])):
        w, h = max(1, round(wpx * 16)), max(1, round(hpx * 16))
        if x + w > ATLAS:
            x, y, row = 0, y + row + 2, 0
        if y + h > ATLAS:
            raise ValueError("goods_lift: the pictures do not fit the atlas")
        p = Image.new("RGBA", (w, h), (255, 255, 255, 255))
        draw(p)
        img.paste(p, (x, y))
        regions[key] = [round(v, 5) for v in (x * 16 / ATLAS, y * 16 / ATLAS, (x + w) * 16 / ATLAS, (y + h) * 16 / ATLAS)]
        x += w + 2
        row = max(row, h)
    img.save(assets / "textures/block/gl_atlas.png")
    return regions


def _finish(elements, regions):
    elements = [e for e in elements if e.get("faces")]
    for e in elements:
        names = set()
        for face, spec in e["faces"].items():
            t = spec["texture"].lstrip("#")
            if t.startswith("gl_pic_"):
                spec["texture"] = "#gl_atlas"
                spec["uv"] = list(regions[t[len("gl_pic_"):]])
            names.add(spec["texture"].lstrip("#"))
        if names and names <= EMISSIVE:
            e["light_emission"] = 15
            e["shade"] = False
    return elements


def _textures(elements, ns, particle):
    names = sorted({spec["texture"].lstrip("#") for e in elements for spec in e.get("faces", {}).values()})
    out = {n: f"{ns}:block/{n}" for n in names}
    out["particle"] = f"{ns}:block/{particle}"
    return out


def _cells_of(elements):
    """The blocks a structure-space model's boxes reach into (its footprint for split_model)."""
    out = set()
    for frm, to, *_ in elements:
        rng = [range(math.floor(frm[a] / 16), math.floor((to[a] - 1e-6) / 16) + 1) for a in range(3)]
        out |= {(x, y, z) for x in rng[0] for y in rng[1] for z in rng[2]}
    return sorted(out, key=lambda c: (c[1], c[2], c[0]))


def _model(assets, ns, name, part, particle="gl_frame"):
    (assets / f"models/block/{name}.json").write_text(json.dumps({"textures": _textures(part, ns, particle), "elements": part}, indent=1) + "\n")


def _single(elements):
    out = []
    for t in elements:
        frm, to, tex, opts = MW.unpack(t)
        out.append(MW.element(frm, to, tex, rotation=opts.get("rotation")))
    MW.finish_elements(out)
    return out


def write(assets, ns="jugcraft"):
    """The textures, the part models and the blockstates of the four pieces."""
    assets = Path(assets)
    for d in ("textures/block", "models/block", "blockstates"):
        (assets / d).mkdir(parents=True, exist_ok=True)
    for name, make in MATERIALS.items():
        make().save(assets / f"textures/block/{name}.png")
    frame = door_frame()
    _one_block(frame, DOOR)
    plates = {k: port_plate(k) for k in ("energy", "cargo")}
    for k, els in plates.items():
        _one_block(els, PLATES[k])
    regions = _pack(assets)
    # the door: its 12 cells, then its live parts (each one model in the door's frame, drawn by the renderer)
    variants = {}
    cells = [tuple(c) for c in DOOR_CELLS]
    for i, part in enumerate(MW.split_model(DOOR, frame, cells)):
        _model(assets, ns, f"{DOOR}_{i}", _finish(part, regions))
        for f, yr in FACINGS.items():
            variants[f"facing={f},part={i}"] = {"model": f"{ns}:block/{DOOR}_{i}", **({"y": yr} if yr else {})}
    live = {"leaf_lower": leaf_lower(), "leaf_upper": leaf_upper(), "car": car(), "gate_front": gate("front"),
            "gate_left": gate("left"), "gate_right": gate("right"), "load": load()}
    index, table = len(cells), {}
    for key in MESH:
        els = live[key]
        foot = _cells_of(els)
        table[key] = []
        for cell, part in zip(foot, MW.split_model(f"{DOOR}_{key}", els, foot)):
            part = _finish(part, regions)
            if not part:
                continue
            _model(assets, ns, f"{DOOR}_{key}_{index}", part)
            for f in FACINGS:
                variants[f"facing={f},part={index}"] = {"model": f"{ns}:block/{DOOR}_{key}_{index}"}
            table[key].append([index, *cell])
            index += 1
    out = assets.parent.parent / MESH_FILE
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps({"parts": table, "leaf_drop": LEAF_DROP, "leaf_rise": LEAF_RISE, "gate_rise": GATE_RISE,
                               "indicator": list(INDICATOR)}, separators=(",", ":")) + "\n")
    (assets / f"blockstates/{DOOR}.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")
    # the rail
    _model(assets, ns, RAIL, _finish(_single(rail()), regions), "gl_rail")
    (assets / f"blockstates/{RAIL}.json").write_text(json.dumps({"variants": {
        f"facing={f}": {"model": f"{ns}:block/{RAIL}", **({"y": yr} if yr else {})} for f, yr in FACINGS.items()}}, indent=1) + "\n")
    # the port plates (one piece each)
    for k in ("energy", "cargo"):
        name = PLATES[k]
        _model(assets, ns, name, _finish(_single(plates[k]), regions), "gl_galv")
        (assets / f"blockstates/{name}.json").write_text(json.dumps({"variants": {
            f"facing={f}": {"model": f"{ns}:block/{name}", **({"y": yr} if yr else {})} for f, yr in FACINGS.items()}}, indent=1) + "\n")
    # the pallet of crates
    v = {}
    for i, part in enumerate(MW.split_model(PALLET, pallet(), [tuple(c) for c in PALLET_CELLS])):
        _model(assets, ns, f"{PALLET}_{i}", _finish(part, regions), "gl_pallet")
        for f, yr in FACINGS.items():
            v[f"facing={f},part={i}"] = {"model": f"{ns}:block/{PALLET}_{i}", **({"y": yr} if yr else {})}
    (assets / f"blockstates/{PALLET}.json").write_text(json.dumps({"variants": v}, indent=1) + "\n")
    # the case packer: the warehouse Intake Case Packer's own part models, turned
    v = {}
    for i in range(len(PACKER_CELLS)):
        for f, yr in FACINGS.items():
            turn = (180 + yr) % 360
            v[f"facing={f},part={i}"] = {"model": f"{ns}:block/warehouse_intake_{i}", **({"y": turn} if turn else {})}
    (assets / f"blockstates/{PACKER}.json").write_text(json.dumps({"variants": v}, indent=1) + "\n")
    # the packer's infeed conveyor
    v = {}
    for i, part in enumerate(MW.split_model(INFEED, infeed(), [tuple(c) for c in INFEED_CELLS])):
        _model(assets, ns, f"{INFEED}_{i}", _finish(part, regions), "gl_galv")
        for f, yr in FACINGS.items():
            v[f"facing={f},part={i}"] = {"model": f"{ns}:block/{INFEED}_{i}", **({"y": yr} if yr else {})}
    (assets / f"blockstates/{INFEED}.json").write_text(json.dumps({"variants": v}, indent=1) + "\n")
    return [DOOR, RAIL, *PLATES.values(), PALLET, PACKER, INFEED]
