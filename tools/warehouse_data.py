"""The automated warehouse's game data, part W1 (inbound; claude/jugcraft-warehouse-plan.md): the blocks' models,
blockstates, items, names, loot, recipes and textures, from the pieces drawn in tools/warehouse.py. Java:
io.github.jimbozoomer.jugcraft.warehouse (blocks, the terminal's books, the drones' jobs) and client/warehouse (the
drones and everything that moves, drawn at their pose; the live screens).

What the game draws itself (so the static models leave it out): the drones (their parts are block models of the
hidden warehouse_part block, posed by client/warehouse/DroneRenderer.java), the cases on the intake's conveyor and in
a drone's gripper, the pallet being built on a station and the stack of empty pallets in its magazine (models of
warehouse_part and warehouse_load), every live screen's page, lit lamp and charger bar, and the racks' location labels'
writing. A rack's pallet is part of its block model (its LOAD state), so a full warehouse costs nothing to draw.

assets/jugcraft/warehouse.json carries what the Java needs from here: each piece's hitboxes, the drones' and the
pieces' dimensions (where a case sits on the conveyor, where the screens are), and the parts' and loads' numbers."""
import json
import math

import nuclear_materials as M
import plant_interior as PI
import warehouse as W
import warehouse_w3 as X
import warehouse_w4 as X4
import warehouse_w5 as X5

MOD = "jugcraft"
# Lit for good (light_emission on the element): the drones' status strips, the beacons, the wall light. The lamps
# that tell something (stack lights, flow lamps) are drawn lit by the game over their dark models.
GLOW = [W.LIGHT, "pi_wh_beacon", "pi_wh_wallpack", "pi_wh_lamp_green", "pi_wh_lamp_red_on", "pi_wh_lamp_amber_on", "pi_wh_lamp_blue_on", X5.WHITE]

# ------------------------------------------------------------------------------------------------ the pieces
# A multi-block piece: (display name, (width, height, depth) in blocks, its boxes). Its blocks are parts of one block
# (facing, part = (r * d + k) * w + c: c across to the user's right, r up, k from the back (0) to the front); the
# block clicked is the front row's middle column at the bottom.
PIECES = {
    "warehouse_intake": ("Intake Case Packer", (3, 2, 2)),
    "warehouse_station": ("Palletizing Station", (1, 2, 2)),
    "warehouse_terminal": ("Warehouse Control Terminal", (3, 2, 1)),
    "warehouse_goods_in": ("Goods In Panel", (3, 2, 1)),
    "warehouse_ready_storage": ("Ready Storage", (3, 4, 2)),
    "warehouse_ready_storage_ext": ("Ready Storage Extension", (3, 4, 2)),
    "warehouse_goods_out": ("Goods Out Panel", (3, 2, 1)),
    # Part W3: the Pallet Lift joining the floors (a landing on each, shaft sections between, the drive on top).
    "warehouse_lift_landing": ("Pallet Lift Landing", (3, 3, 2)),
    "warehouse_lift_shaft": ("Pallet Lift Shaft", (3, 1, 2)),
    "warehouse_lift_drive": ("Pallet Lift Drive", (3, 2, 2)),
    # Part W4: liquids in drums (tools/warehouse_w4.py).
    **X4.PIECES_W4,
    # Part W5: fleet and control (tools/warehouse_w5.py).
    **X5.PIECES_W5,
}
SINGLE = {
    "warehouse_lane": "Warehouse Lane Tile",
    "warehouse_pack_spot": "Pallet Build Spot Floor",
    "warehouse_rack": "Pallet Rack",
    "warehouse_dock": "Drone Charging Dock",
    "warehouse_conduit": "Charger Conduit",
    "warehouse_trunking": "Cable Trunking",
    # Part W3: the floors' fittings.
    "warehouse_walkway": "Pedestrian Walkway",
    "warehouse_crossing": "Drone Crossing",
    "warehouse_crossing_beacon": "Crossing Beacon",
    "warehouse_aisle_sign": "Aisle Sign",
    "warehouse_end_barrier": "Rack End Barrier",
    "warehouse_bollard": "Bollard",
    "warehouse_slab": "Warehouse Floor Slab",
}
HIDDEN = {"warehouse_part": "Warehouse Drone Part", "warehouse_load": "Warehouse Pallet Load"}
ITEMS = {"warehouse_pallet": "Pallet", "stacker_drone": "Stacker Drone", "pallet_drone": "Pallet Drone", "rack_sprinkler_kit": "In-Rack Sprinkler Kit",
         **{item: name for item, name in X4.DRUM_ITEMS.values()}}
# The blocks with items (machines.machine_blocks: their loot drops themselves, a pickaxe breaks them).
BLOCKS = list(PIECES) + list(SINGLE)

# The lamps and screens the game lights (their dark versions in the models).
DARK = {
    "pi_wh_lamp_green": "pi_wh_lamp_green_off",
    "pi_wh_hmi_intake!": "pi_wh_scr_off!",
    "pi_wh_scr_stock!": "pi_wh_scr_off!", "pi_wh_scr_fleet!": "pi_wh_scr_off!", "pi_wh_scr_orders!": "pi_wh_scr_off!",
    "pi_wh_scr_dropoff!": "pi_wh_scr_off!",
    "pi_wh_charger_front!": "pi_wh_charger_dark!",
    "pi_wh_pd_screen!": "pi_wh_scr_off!",
    "pi_wh_scr_vlm!": "pi_wh_scr_off!", "pi_wh_scr_pickup!": "pi_wh_scr_off!",
    "pi_wh_scr_lift!": "pi_wh_scr_off!",
    **X4.W4_DARK,
    **X5.W5_DARK,
}
# The lift's call station beacon flashes while it moves (the game lights it): dark in the model.
LIFT_DARK = {"pi_wh_beacon": "pi_wh_lamp_amber"}


SEE_THROUGH_FACES = {W.FILM, W.GUARD, W.DECK, "pi_wh_vlm_glass"}


def _darken(boxes):
    """A piece's boxes with its live lamps and screens dark (and no holes: closed())."""
    out = []
    for b in boxes:
        t = b[6]
        if isinstance(t, str):
            t = DARK.get(t, t)
        elif isinstance(t, dict):
            t = {k: (DARK.get(v, v) if isinstance(v, str) else v) for k, v in t.items()}
        out.append(b[:6] + (t,) + tuple(b[7:]))
    return closed(out)


def closed(boxes):
    """Boxes that show only some faces (a label plate's front, a decal, a screen's glass) get their other faces too
    (dark), so no solid box has a hole in it; see-through sheets (film, guard windows) stay as they are."""
    out = []
    for b in boxes:
        t = b[6]
        if isinstance(t, dict) and "*" not in t and len(t) < 6:
            names = {(v[0] if isinstance(v, tuple) else v).split("@")[0].rstrip("!") for v in t.values()}
            if not names & SEE_THROUGH_FACES:
                t = dict(t, **{"*": "pi_cap_black"})
        out.append(b[:6] + (t,) + tuple(b[7:]))
    return out


def piece_boxes(name):
    if name == "warehouse_intake":
        return _darken(W.intake())
    if name == "warehouse_station":
        return _darken(W.pallet_station(0))       # the magazine's pallets are drawn by the game (how many there are)
    if name == "warehouse_terminal":
        return _darken(W.terminal())
    if name == "warehouse_goods_in":
        return _darken(W.inlet_panel())
    if name == "warehouse_ready_storage":
        return _darken(W.ready_storage())
    if name == "warehouse_ready_storage_ext":
        return _darken(W.ready_storage_ext())
    if name == "warehouse_goods_out":
        return _darken(W.pickup_window(True))
    if name == "warehouse_lift_landing":
        return _lift_dark(X.lift_landing(gate=None, level="pi_wh_lift_plate"))
    if name == "warehouse_lift_shaft":
        return _lift_dark(X.lift_shaft())
    if name == "warehouse_lift_drive":
        return _lift_dark(X.lift_drive())
    if name in X4.PIECES_W4:
        return _darken(X4.piece_boxes_w4(name))
    if name in X5.PIECES_W5:
        return _darken(X5.piece_boxes_w5(name))
    raise KeyError(name)


def _lift_dark(boxes):
    out = []
    for b in boxes:
        t = b[6]
        t = LIFT_DARK.get(t, t) if isinstance(t, str) else {k: LIFT_DARK.get(v, v) if isinstance(v, str) else v for k, v in t.items()}
        out.append(b[:6] + (t,) + tuple(b[7:]))
    return _darken(out)


def part_index(name, c, r, k):
    w, h, d = PIECES[name][1]
    return (r * d + k) * w + c


# ------------------------------------------------------------------------------------------------ racks and loads
LEVELS = ("ground", "first", "upper")      # a place on the floor; the one over it (both labels); any higher
KINDS = W.LOADS                            # carton, carton_w, sack, crate, drum, ingot, mixed
MAX_CASES = 8


def load_index(kind, n):
    """A rack place's LOAD state for a pallet of n cases of a kind (0: no pallet)."""
    return 1 + KINDS.index(kind) * MAX_CASES + (n - 1)


# Part W4: a pallet of 1 to 4 drums of a liquid (PalletLoad.look: after the case loads).
DRUM_LOAD0 = 1 + len(KINDS) * MAX_CASES


def drum_load_index(kind, n):
    return DRUM_LOAD0 + X4.DRUM_KINDS.index(kind) * 4 + (n - 1)


def rack_boxes(left, right, level, top):
    return closed(W.rack(left, right, level == "ground", top, {"ground": 0, "first": 2, "upper": 1}[level]))


def load_boxes(kind, n, y=0.0):
    """A pallet and its load standing at y (0: on the floor, W.BEAM_TOP: on a rack place's beams)."""
    return W.shifted(W.pallet() + W.load(kind, n), 0, y, 0)


# ------------------------------------------------------------------------------------------------ hidden part models
CASE_KINDS = ("carton", "carton_w", "sack", "crate", "drum", "ingot")


def held_case(kind):
    """One case on its own (on the conveyor, in a gripper): its corner at (0, 0, 0), CASE x CASE_H x CASE."""
    if kind in ("carton", "carton_w", "crate"):
        return [W._case(0.0, 0.0, 0.0, kind, True)]
    if kind == "sack":
        return [W.box(0.0, 0.0, 0.2, W.CASE, 3.2, W.CASE - 0.2, {"*": "pi_wh_sack", "south": "pi_wh_sack_print"}),
                W.box(0.4, 3.2, 0.6, W.CASE - 0.4, 3.25, W.CASE - 0.6, "pi_wh_sack")]
    if kind == "drum":
        c = W.CASE / 2
        return W.drum_y(c, c, 3.3, 0.0, W.CASE_H + 1.0, "pi_wh_drum", "pi_wh_drum_top", "pi_wh_drum_label") + \
            W.round_y(c, c, 3.45, 2.0, 2.4, "pi_wh_drum")
    if kind == "ingot":
        return [W.box(0.2, 0.0, 0.2, W.CASE - 0.2, 3.6, W.CASE - 0.2, {"*": "pi_wh_ingot_side", "north": "pi_wh_ingot_end", "south": "pi_wh_ingot_end"}),
                W.box(1.8, 3.6, 0.0, 2.6, 3.72, W.CASE, "pi_wh_band"), W.box(4.2, 3.6, 0.0, 5.0, 3.72, W.CASE, "pi_wh_band"),
                W.box(1.8, 0.0, 0.0, 2.6, 3.6, 0.2, "pi_wh_band"), W.box(4.2, 0.0, 0.0, 5.0, 3.6, 0.2, "pi_wh_band"),
                W.box(1.8, 0.0, W.CASE - 0.2, 2.6, 3.6, W.CASE, "pi_wh_band"), W.box(4.2, 0.0, W.CASE - 0.2, 5.0, 3.6, W.CASE, "pi_wh_band")]
    raise KeyError(kind)


def film_web():
    """The stretch film running from the drone's film roll to the turning pallet while it wraps (a strip, 1 px wide
    here, its length along z from 0 to 16; the game stretches and turns it)."""
    return [W.box(-0.5, 0.0, 0.0, 0.5, 5.0, 16.0, {"east": W.FILM, "west": W.FILM})]


def lamp_on(tex):
    """A stack light's lamp lit (drawn by the game over the dark one: a hair bigger), its middle at (8, 8), from y 0."""
    import plant_panels as PP
    return PP.round_y(8.0, 8.0, 0.8, 0.0, 1.26, tex)


def flow_lamp_on():
    """A flange's flow lamp lit (over the dark one), facing +z, its middle at (8, 8), from z 0."""
    return W.round_z(8.0, 8.0, 0.6, 0.0, 0.9, "pi_wh_lamp_green")


PARTS = {
    "pd_chassis": lambda: _darken(W.pd_chassis()), "pd_mast_outer": W.pd_mast_outer, "pd_mast_mid": W.pd_mast_mid, "pd_mast_inner": W.pd_mast_inner,
    "pd_carriage": W.pd_carriage, "pd_head": lambda: W.shifted(W.pd_head(), 0, 0, -16.0),
    "sd_base": W.sd_base, "sd_j1": W.sd_j1, "sd_upper": W.sd_upper, "sd_fore": W.sd_fore, "sd_wrist": W.sd_wrist, "sd_film": W.sd_film,
    **{f"case_{k}": (lambda k=k: held_case(k)) for k in CASE_KINDS},
    "film_web": film_web,
    "lamp_red": lambda: lamp_on("pi_wh_lamp_red_on"), "lamp_amber": lambda: lamp_on("pi_wh_lamp_amber_on"),
    "lamp_green": lambda: lamp_on("pi_wh_lamp_green"), "lamp_blue": lambda: lamp_on("pi_wh_lamp_blue_on"),
    "flow_lamp": flow_lamp_on,
    # Part W2: Ready Storage's shutter (a slat, the rubber edge), its pick cell's gantry (bridge, carriage, tube, vacuum
    # head), the drive on its top module; the Goods Out window's tray and what is on it.
    "rs_slat": W.rs_slat, "rs_edge": W.rs_edge, "rs_bridge": W.rs_bridge, "rs_carriage": W.rs_carriage, "rs_tube": W.rs_tube,
    "rs_head": W.rs_head, "rs_drive": W.rs_drive, "out_tray": W.out_tray, "out_tray_load": W.out_tray_load,
    # Part W3: the Pallet Lift's moving parts (the carriage, the counterweight, a chain stretched to its length, the
    # gate's two panels shut, the call station's beacon lit, the position indicator's arrows lit).
    "lift_carriage": X.lift_carriage, "lift_cw": X.lift_counterweight, "lift_chain": X.lift_chain,
    "lift_gate_lo": lambda: _gate_panel(0), "lift_gate_hi": lambda: _gate_panel(1),
    "lift_beacon": lambda: __import__("plant_panels").round_y(8.0, 8.0, 1.15, 0.0, 2.3, "pi_wh_lamp_amber_on"),
    "lift_arrow_up": lambda: [W.box(0.0, 0.0, 0.0, 1.4, 1.2, 0.2, "pi_wh_lamp_green")],
    "lift_arrow_dn": lambda: [W.box(0.0, 0.0, 0.0, 1.4, 1.2, 0.2, "pi_wh_lamp_amber_on")],
    # Part W4: the Drum Filling Station's gantry robot (bridge, trolley, Z mast with its vacuum tool, lance), the
    # Drum Emptying Station's jib (arm, balancer, cable) and drum pump, the drums on their own, a level gauge's liquid,
    # the sight flow glass's, the earthing reel's lamp lit.
    **X4.parts_w4(),
    # Part W5: a breaker's toggle, the main switch's handle, a pilot lamp's lit lens, a lit square (tinted, stretched).
    **X5.parts_w5(),
}
PART_NAMES = list(PARTS)
PART_STATES = 64         # the hidden part block's PART values (HiddenBlock.PART: 0..63)
# Parts written moved (a model's elements must stay within -16..32): the game moves them back when it draws them.
# (The pallet drone's head is written 16 back along z: Kinematics.HEAD_SHIFT.)
PART_SHIFT = {"sd_wrist": (0.0, -16.0, 0.0), "pd_mast_outer": (0.0, -16.0, 0.0), "rs_slat": (-16.0, 0.0, -16.0), "rs_edge": (-16.0, 0.0, -16.0),
              "rs_drive": (-16.0, 0.0, 0.0), "out_tray": (-16.0, 0.0, 0.0), "out_tray_load": (-16.0, 0.0, 0.0), "rs_bridge": (-16.0, 0.0, 0.0),
              "lift_carriage": (-16.0, 0.0, 0.0), "lift_gate_lo": (-16.0, 0.0, -16.0), "lift_gate_hi": (-16.0, 0.0, -16.0),
              # W4: the filling station's gantry bridge runs over the piece's top block (y 41.6..46.4).
              "lq_bridge": (0.0, -16.0, 0.0)}


def _gate_panel(which):
    """One panel of a landing's telescoping gate, shut (its bottom at y 0; the game raises it): 0 the lower (it
    carries the rubber edge and rides up behind the upper), 1 the upper."""
    boxes = X.lift_gate(0.0)
    per = (len(boxes) - 1) // 2
    if which == 0:
        part = boxes[:per] + boxes[-1:]
        y = 0.4
    else:
        part = boxes[per:2 * per]
        y = X.LIFT_TOP / 2 - 0.4
    return W.shifted(part, 0.0, -y, 0.0)


def part_boxes(name):
    boxes = closed(PARTS[name]())
    dx, dy, dz = PART_SHIFT.get(name, (0.0, 0.0, 0.0))
    return W.shifted(boxes, dx, dy, dz) if (dx or dy or dz) else boxes


# ------------------------------------------------------------------------------------------------ hitboxes
def voxel_shape(boxes, step=2.0):
    """A block's hitbox from its boxes: which 2 px cells they fill (a cell counts when a box covers a fair part of
    it), merged into a few boxes."""
    n = int(16 / step)
    grid = [[[False] * n for _ in range(n)] for _ in range(n)]
    for b in boxes:
        x0, y0, z0, x1, y1, z1 = (max(0.0, min(16.0, v)) for v in b[:6])
        if min(x1 - x0, y1 - y0, z1 - z0) < 0.3:
            continue
        for i in range(n):
            if min(x1, (i + 1) * step) - max(x0, i * step) < step * 0.35:
                continue
            for j in range(n):
                if min(y1, (j + 1) * step) - max(y0, j * step) < step * 0.35:
                    continue
                for k in range(n):
                    if min(z1, (k + 1) * step) - max(z0, k * step) >= step * 0.35:
                        grid[i][j][k] = True
    out = []
    used = [[[False] * n for _ in range(n)] for _ in range(n)]
    for j in range(n):
        for k in range(n):
            for i in range(n):
                if not grid[i][j][k] or used[i][j][k]:
                    continue
                i1 = i
                while i1 + 1 < n and grid[i1 + 1][j][k] and not used[i1 + 1][j][k]:
                    i1 += 1
                k1 = k
                while k1 + 1 < n and all(grid[a][j][k1 + 1] and not used[a][j][k1 + 1] for a in range(i, i1 + 1)):
                    k1 += 1
                j1 = j
                while j1 + 1 < n and all(grid[a][j1 + 1][c] and not used[a][j1 + 1][c] for a in range(i, i1 + 1) for c in range(k, k1 + 1)):
                    j1 += 1
                for a in range(i, i1 + 1):
                    for bb in range(j, j1 + 1):
                        for c in range(k, k1 + 1):
                            used[a][bb][c] = True
                out.append([i * step, j * step, k * step, (i1 + 1) * step, (j1 + 1) * step, (k1 + 1) * step])
    return out or [[0, 0, 0, 16, 1, 16]]


# ------------------------------------------------------------------------------------------------ what the Java reads
def layout():
    """assets/jugcraft/warehouse.json: hitboxes, the parts' and loads' numbers, the dimensions the game draws by."""
    shapes = {}
    for name, (_, (w, h, d)) in PIECES.items():
        split = W.split_blocks(piece_boxes(name), w, h, d)
        parts = []
        for r in range(h):
            for k in range(d):
                for c in range(w):
                    parts.append(voxel_shape(split.get((c, r, k), [])))
        shapes[name] = {"size": [w, h, d], "parts": parts}
    racks = {f"{int(l)}{int(r)}_{lv}{int(t)}": voxel_shape(rack_boxes(l, r, lv, t))
             for l in (False, True) for r in (False, True) for lv in LEVELS for t in (False, True)}
    term = []
    for tex, x0, _ in (("fleet", 3.6, 0), ("stock", 17.2, 0), ("orders", 30.8, 0)):
        turn = 22.5 if x0 < 10 else (-22.5 if x0 > 30 else 0.0)
        pivot = [x0 + 13.6, 15.0, 4.0] if x0 < 10 else [x0, 15.0, 4.0]
        term.append({"page": tex, "x0": x0 + 0.3, "x1": x0 + 13.3, "y0": 15.3, "y1": 23.7, "z": 4.9, "turn": turn, "pivot": pivot})
    return {
        "pieces": shapes,
        "rack": racks,
        "dock": voxel_shape(W.charge_dock()),
        "conduit": voxel_shape(W.charge_dock_upper()),
        "trunking": voxel_shape(W.trunking()),
        "end_barrier": voxel_shape(X.end_barrier()),
        "crossing_beacon": voxel_shape(X.crossing_beacon(False)),
        "aisle_sign": voxel_shape(X.aisle_sign()),
        "bollard": voxel_shape(W.bollard()),
        "parts": PART_NAMES,
        "part_shift": {k: list(v) for k, v in PART_SHIFT.items()},
        "load_kinds": list(KINDS),
        "case_kinds": list(CASE_KINDS),
        "max_cases": MAX_CASES,
        "pallet": {"height": W.PALLET_H, "case": W.CASE, "case_height": W.CASE_H, "beam_top": W.BEAM_TOP,
                   "spots": [[1.1, 1.1], [8.1, 1.1], [1.1, 8.1], [8.1, 8.1]]},
        "pallet_drone": {"free": W.PD_FREE, "stage": W.PD_STAGE, "lift": W.PD_LIFT, "head_x": W.HEAD_X, "head_z": W.HEAD_Z,
                         "carried": [0.0, -0.2, 19.6], "head_shift": 16.0},
        "stacker_drone": {"base": list(W.SD_BASE), "shoulder": W.SD_SHOULDER, "upper": W.SD_UPPER, "fore": W.SD_FORE,
                          "cups": [-0.5, 3.6, 0.0], "film_base": 6.0, "film_top": 13.0, "film_roll": [-1.3, 3.7]},
        # The intake's conveyor (piece pixels, front row): cases run along x at this height from the pusher to the stop.
        "intake": {"conveyor_y": W.CONV_Y, "conveyor_z": 24.0, "start_x": 2.0, "stop_x": 47.0,
                   "hmi": {"x0": 1.4, "x1": 8.8, "y0": 18.2, "y1": 23.8, "z": 20.6},
                   "stack": {"x": 44.6, "z": 13.0, "y0": 26.77, "step": 1.3, "radius": 0.75}},
        # The station's turntable (the pallet built on it) and its magazine's stack.
        "station": {"table_y": 1.3, "table_x": 8.0, "table_z": 24.0, "magazine_y": 1.3, "magazine_max": 12},
        "terminal": term,
        "goods_in": {"screen": {"x0": 19.6, "x1": 28.4, "y0": 21.2, "y1": 25.2, "z": W.WALL_FACE + 2.8},
                     "inlets": [[8.0, 8.0], [40.0, 8.0], [8.0, 22.0], [40.0, 22.0]], "lamp_dx": 4.3, "lamp_z": W.WALL_FACE + 1.35,
                     "hatch_lamp": [30.8, 23.4, W.WALL_FACE + 1.55]},
        "charger": {"x0": 9.2, "x1": 15.4, "y0": 7.2, "y1": 15.6, "z": 2.8},
        # Ready Storage (its game frame: the hatch at its front, +z): the shutter, the bed and pick cell (the pallet's
        # middle), the gantry (its rails' ends, where it parks and drops cases into the lift), the operator screen,
        # the drive's height on the top module, the slots a module adds.
        "ready_storage": {"hatch_top": W.HATCH[2], "slat_step": 1.36, "edge": 0.9, "bed_y": 2.15, "pallet_x": 24.0, "bed_z": 40.0,
                          "cell_z": 32.0 - 0.8 - 8.0, "cell_back": 32.0 - W.HATCH[3], "park": [24.0, 13.4, 24.0], "drop": [24.0, 13.0, 24.0],
                          "carry_y": 24.0, "roll_speed": 0.6,
                          "screen": {"x0": 36.2, "x1": 44.2, "y0": 11.2, "y1": 17.4, "z": 33.8}, "drive_y": 64.0, "slots": 108},
        # The Goods Out panel: the kiosk's screen, the beacon, the OUT flange's lamp, the tray (presented at z 0; in, back by
        # tray_in) and the Output Port's flange (its block: c 2, r 0).
        "goods_out": {"screen": {"x0": 3.4, "x1": 9.6, "y0": 14.8, "y1": 22.8, "z": W.WALL_FACE + 5.8},
                      "beacon": [24.0, 24.8, 2.6 + W.WALL_FACE], "out_lamp": [44.3, 10.0, W.WALL_FACE + 1.4], "tray_in": -12.0},
        "rack_labels": {"single": [[5.9, 10.1]], "double": [[8.4, 12.6], [3.4, 7.6]], "y0": 0.25, "y1": 1.35, "z": 15.7},
        # Part W3. The Pallet Lift (a landing's frame, its front +z): the opening and the gate's travel, the carriage's
        # deck and the pallet's middle on it and on the transfer conveyor out front, the counterweight's line, the
        # chains' lines, the drive's sprockets, the call station's screen and beacon, the name plate, the arrows.
        "lift": {"x0": X.LIFT_X0, "x1": X.LIFT_X1, "top": X.LIFT_TOP, "landing_h": X.LANDING_H, "deck_y": X.DECK_Y, "pallet_x": 24.0,
                 "carriage_z": X.CARRIAGE_Z, "bed_z": X.BED_Z, "bed_y": 2.35, "gate_lo": 0.4, "gate_hi": X.LIFT_TOP / 2 - 0.4,
                 "cw_x": 0.0, "chains": [[14.0, 16.0], [34.0, 16.0]], "cw_chain": [6.0, 16.0], "yoke_y": 11.0, "cw_eye": 18.0,
                 "sprocket_y": 8.0 - 3.4, "screen": {"x0": 39.8, "x1": 45.2, "y0": 28.6, "y1": 34.4, "z": 33.85},
                 "beacon": [42.5, 36.8, 31.5], "plate": {"x0": 13.0, "x1": 35.0, "y0": 36.6, "y1": 44.6, "z": 32.45, "field": 6.9},
                 "arrow_up": [29.6, 45.3, 32.62], "arrow_dn": [31.4, 45.3, 32.62], "roll_speed": 0.6},
        "aisle_sign_face": {"x0": 0.8, "x1": 15.2, "y0": 3.4, "y1": 11.6, "z_front": 8.75, "z_back": 7.25},
        # Part W4 (tools/warehouse_w4.py layout_w4): the drums, the stations' gantry and jib, their gauges and pages.
        "drum_load0": DRUM_LOAD0,
        **X4.layout_w4(),
        # Part W5 (tools/warehouse_w5.py layout_w5): the switchboard's toggles, lamps, handle and pages; the status
        # board's screen; the signal box's LEDs and screen.
        **X5.layout_w5(),
    }


# ------------------------------------------------------------------------------------------------ item models
def _rotated_box(b, R, t):
    """A box turned by R (a quarter-turn matrix: each row one +-1) and moved by t; its own turn turned with it."""
    import numpy as np
    p0 = R @ np.array(b[:3], float) + t
    p1 = R @ np.array(b[3:6], float) + t
    lo, hi = np.minimum(p0, p1), np.maximum(p0, p1)
    tex = b[6]
    if isinstance(tex, dict):
        # The faces go where the turn takes them.
        names = {(0, 0, -1): "north", (0, 0, 1): "south", (1, 0, 0): "east", (-1, 0, 0): "west", (0, 1, 0): "up", (0, -1, 0): "down"}
        normals = {v: k for k, v in names.items()}
        new = {}
        for face, v in tex.items():
            if face == "*":
                new["*"] = v
                continue
            n = tuple(int(round(x)) for x in R @ np.array(normals[face], float))
            new[names[n]] = v.rstrip("!") if isinstance(v, str) else v
        tex = new
    out = (round(lo[0], 4), round(lo[1], 4), round(lo[2], 4), round(hi[0], 4), round(hi[1], 4), round(hi[2], 4), tex)
    if len(b) > 7 and b[7]:
        r = b[7]["rotation"]
        axis = {"x": (1, 0, 0), "y": (0, 1, 0), "z": (0, 0, 1)}[r["axis"]]
        na = R @ np.array(axis, float)
        i = int(np.argmax(np.abs(na)))
        angle = r["angle"] * (1 if na[i] > 0 else -1)
        o = R @ np.array(r["origin"], float) + t
        out += ({"rotation": {"angle": angle, "axis": "xyz"[i], "origin": [round(float(v), 4) for v in o]}},)
    return out


def _item_model(groups, rid, gui=0.625, fit=44.0):
    """An item model of groups of boxes, each (boxes, (dx, dy, dz)): a group's elements get their textures' areas
    where they stand in their own block, then go to their place; the whole is fitted inside the game's -16..32
    limits (moved, and shrunk if it spans more than fit px; a tall piece fits in less, as the passes against z-fighting
    push faces out from there)."""
    elements, used = [], set()
    for boxes, (dx, dy, dz) in groups:
        els, names = PI._elements(PI.separate(closed(boxes)), GLOW)
        used |= names
        for e in els:
            for fname, f in e["faces"].items():
                f.setdefault("uv", PI._auto_uv(fname, *e["from"], *e["to"]))
            e["from"] = [e["from"][0] + dx, e["from"][1] + dy, e["from"][2] + dz]
            e["to"] = [e["to"][0] + dx, e["to"][1] + dy, e["to"][2] + dz]
            if "rotation" in e:
                o = e["rotation"]["origin"]
                e["rotation"] = dict(e["rotation"], origin=[o[0] + dx, o[1] + dy, o[2] + dz])
            elements.append(e)
    lo = [min(min(e["from"][i], e["to"][i]) for e in elements) for i in range(3)]
    hi = [max(max(e["from"][i], e["to"][i]) for e in elements) for i in range(3)]
    span = max(hi[i] - lo[i] for i in range(3))
    s = min(1.0, fit / span)
    centre = [(lo[i] + hi[i]) / 2 for i in range(3)]
    target = [8.0, 8.0, 8.0]
    f = lambda v, i: round(target[i] + (v - centre[i]) * s, 4)
    for e in elements:
        e["from"] = [f(e["from"][i], i) for i in range(3)]
        e["to"] = [f(e["to"][i], i) for i in range(3)]
        if "rotation" in e:
            o = e["rotation"]["origin"]
            e["rotation"] = dict(e["rotation"], origin=[f(o[i], i) for i in range(3)])
    textures = {t: ("minecraft:block/" + t[3:] if t.startswith("mc_") else rid(f"block/{t}")) for t in sorted(used)}
    textures["particle"] = textures[sorted(used)[0]]
    k = round(gui * 16.0 / max(16.0, span * s), 4)
    sc = lambda m: [round(k * m, 4)] * 3
    return {"textures": textures, "elements": elements, "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": sc(1)},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": sc(0.4)},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": sc(0.8)},
        "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": sc(1)},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": sc(0.6)},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": sc(0.6)},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": sc(0.65)},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": sc(0.65)}}}


def _piece_item_groups(name):
    """A piece's blocks for its item, cut as in the world (so each face shows what it does there)."""
    w, h, d = PIECES[name][1]
    return [(bx, (16 * c, 16 * r, 16 * k)) for (c, r, k), bx in sorted(W.split_blocks(piece_boxes(name), w, h, d).items())]


def _quarter(axis, turns):
    import numpy as np
    a = math.radians(90 * turns)
    c, s = round(math.cos(a)), round(math.sin(a))
    if axis == "x":
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]], float)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]], float)


def _about(b, R, pivot):
    import numpy as np
    p = np.array(pivot, float)
    return _rotated_box(b, R, p - R @ p)


def stacker_item_boxes():
    """The stacker drone parked for its item: arm up, forearm folded forward, the cups pointing down."""
    import numpy as np
    x, _, z = W.SD_BASE
    out = list(W.sd_base()) + list(W.sd_j1()) + list(W.sd_upper())
    elbow = [x, W.SD_SHOULDER + W.SD_UPPER, z]
    R1 = _quarter("x", 1)                       # the forearm forward (toward +z)
    out += [_about(b, R1, elbow) for b in W.sd_fore()]
    # The wrist: the forearm's turn and its own (cups down): 180 in all about the elbow then the wrist's spot.
    wrist = R1 @ (np.array([x, W.SD_SHOULDER + W.SD_UPPER + W.SD_FORE, z]) - np.array(elbow)) + np.array(elbow)
    for b in W.sd_wrist():
        b1 = _about(b, R1, elbow)
        out.append(_about(b1, _quarter("x", 1), list(wrist)))
    out += W.shifted(W.sd_film(), 0, 6.0, 0)
    return out


def pallet_drone_item_boxes():
    return list(W.pd_chassis()) + list(W.pd_mast_outer()) + list(W.pd_mast_mid()) + list(W.pd_mast_inner()) + \
        list(W.pd_carriage()) + list(W.pd_head())


# ------------------------------------------------------------------------------------------------ writing
TURN = PI.TURN        # (("north", 0), ("east", 90), ("south", 180), ("west", 270)): facing (the way its user looks)


# A floor over another (part W3): what its slab shows underneath, the metal deck with its ribs along x or z, or the
# deck and a steel girder (along the other way) on a column line. none: the ground floor.
SOFFITS = ("none", "deck_x", "deck_z", "girder_z", "girder_x")


def topping(boxes):
    """A floor tile's boxes cut to its topping (y 12.6 up), for a slab with a soffit under it."""
    out = []
    for b in boxes:
        if b[4] <= 12.6:
            continue
        out.append((b[0], max(b[1], 12.6)) + tuple(b[2:]))
    return out


def soffit_parts(model):
    """The multipart entries drawing a slab's soffit under its topping."""
    import exterior_kit as E
    deck = model("warehouse_soffit_deck", E.soffit(False))
    girder = model("warehouse_soffit_girder", E.soffit(True))
    return [{"when": {"soffit": "deck_x"}, "apply": {"model": deck}}, {"when": {"soffit": "deck_z"}, "apply": {"model": deck, "y": 90}},
            {"when": {"soffit": "girder_z"}, "apply": {"model": girder}}, {"when": {"soffit": "girder_x"}, "apply": {"model": girder, "y": 90}}]


def write_w3(write, model, item, rid, states, assets, lang):
    """Part W3's floor fittings: walkways, crossings, their beacons, aisle signs, end barriers, bollards, the slab."""
    for name in ("warehouse_walkway", "warehouse_crossing", "warehouse_crossing_beacon", "warehouse_aisle_sign", "warehouse_end_barrier",
                 "warehouse_bollard", "warehouse_slab"):
        lang[f"block.{MOD}.{name}"] = SINGLE[name]
    parts = []
    for m in range(16):
        for st in (0, 1):
            full = model(f"warehouse_walkway_{m}_{st}", X.walkway_model(m, st))
            top = model(f"warehouse_walkway_{m}_{st}_top", topping(X.walkway_model(m, st)))
            key = {"mask": str(m), "stencil": "true" if st else "false"}
            parts.append({"when": {**key, "soffit": "none"}, "apply": {"model": full}})
            parts.append({"when": {**key, "soffit": "|".join(SOFFITS[1:])}, "apply": {"model": top}})
    write(states / "warehouse_walkway.json", {"multipart": parts + soffit_parts(model)})
    item("warehouse_walkway", rid("block/warehouse_walkway_15_1"))
    parts = []
    for axis, along in (("x", 1), ("z", 0)):
        full = model(f"warehouse_crossing_{axis}", X.crossing_model(along))
        top = model(f"warehouse_crossing_{axis}_top", topping(X.crossing_model(along)))
        parts.append({"when": {"axis": axis, "soffit": "none"}, "apply": {"model": full}})
        parts.append({"when": {"axis": axis, "soffit": "|".join(SOFFITS[1:])}, "apply": {"model": top}})
    write(states / "warehouse_crossing.json", {"multipart": parts + soffit_parts(model)})
    item("warehouse_crossing", rid("block/warehouse_crossing_x"))
    slab_full = model("warehouse_slab", [W.box(0, 0, 0, 16, 16, 16, {"*": "pi_wh_lane_side", "up": "epoxy_floor"})])
    slab_top = model("warehouse_slab_top", [W.box(0, 12.6, 0, 16, 16, 16, {"*": "pi_wh_lane_side", "up": "epoxy_floor"})])
    write(states / "warehouse_slab.json", {"multipart": [{"when": {"soffit": "none"}, "apply": {"model": slab_full}},
                                                         {"when": {"soffit": "|".join(SOFFITS[1:])}, "apply": {"model": slab_top}}] + soffit_parts(model)})
    item("warehouse_slab", rid("block/warehouse_slab"))
    on, off = model("warehouse_crossing_beacon_on", X.crossing_beacon(True)), model("warehouse_crossing_beacon_off", X.crossing_beacon(False))
    write(states / "warehouse_crossing_beacon.json", {"variants": {f"facing={f},lit={l}": {"model": on if l == "true" else off, **({"y": y} if y else {})}
                                                                   for f, y in TURN for l in ("true", "false")}})
    item("warehouse_crossing_beacon", rid("block/warehouse_crossing_beacon_off"))
    for name, boxes in (("warehouse_aisle_sign", aisle_sign_boxes()), ("warehouse_end_barrier", X.end_barrier())):
        ref = model(name, boxes)
        write(states / f"{name}.json", {"variants": {f"facing={f}": {"model": ref, **({"y": y} if y else {})} for f, y in TURN}})
        item(name, ref)
    ref = model("warehouse_bollard", W.bollard())
    write(states / "warehouse_bollard.json", {"variants": {"": {"model": ref}}})
    item("warehouse_bollard", ref)


def aisle_sign_boxes():
    """The aisle sign with its faces blank (the game writes the aisle, its floor and bays on them)."""
    return [b[:6] + ({k: ("pi_wh_aisle_plate!" if v == "pi_wh_aisle_sign!" else v) for k, v in b[6].items()} if isinstance(b[6], dict) else b[6],)
            + tuple(b[7:]) for b in X.aisle_sign()]


def write_assets(write, rid, assets, lang):
    import zfight
    write = zfight.unfighting(write)
    models = assets / "models" / "block"
    states = assets / "blockstates"
    items = assets / "items"
    write(assets / "warehouse.json", layout())

    def model(name, boxes):
        write(models / f"{name}.json", PI.model(boxes, GLOW, rid))
        return rid(f"block/{name}")

    def item(name, model_id):
        write(items / f"{name}.json", {"model": {"type": "minecraft:model", "model": model_id}})

    # The pieces: each block of the piece its own model, turned by facing.
    for name, (display, (w, h, d)) in PIECES.items():
        lang[f"block.{MOD}.{name}"] = display
        split = W.split_blocks(piece_boxes(name), w, h, d)
        variants = {}
        for r in range(h):
            for k in range(d):
                for c in range(w):
                    p = part_index(name, c, r, k)
                    ref = model(f"{name}_{p}", split.get((c, r, k)) or [W.box(7.9, 0, 7.9, 8.1, 0.1, 8.1, W.GRAPHITE)])
                    for face, y in TURN:
                        variants[f"facing={face},part={p}"] = {"model": ref, **({"y": y} if y else {})}
        write(states / f"{name}.json", {"variants": variants})
        write(assets / "models" / "item" / f"{name}.json", _item_model(_piece_item_groups(name), rid, fit=38.0 if PIECES[name][1][1] >= 4 else 44.0))
        item(name, rid(f"item/{name}"))

    # Lane tiles: the floor block with the yellow line on each edge with no lane beyond (mask), one of four codes.
    lang[f"block.{MOD}.warehouse_lane"] = SINGLE["warehouse_lane"]
    parts = []
    for m in range(16):
        for c in range(4):
            full = model(f"warehouse_lane_{m}_{c}", W.lane_model(m, c))
            top = model(f"warehouse_lane_{m}_{c}_top", topping(W.lane_model(m, c)))
            parts.append({"when": {"code": str(c), "mask": str(m), "soffit": "none"}, "apply": {"model": full}})
            parts.append({"when": {"code": str(c), "mask": str(m), "soffit": "|".join(SOFFITS[1:])}, "apply": {"model": top}})
    write(states / "warehouse_lane.json", {"multipart": parts + soffit_parts(model)})
    item("warehouse_lane", rid("block/warehouse_lane_0_1"))
    lang[f"block.{MOD}.warehouse_pack_spot"] = SINGLE["warehouse_pack_spot"]
    write(states / "warehouse_pack_spot.json", {"variants": {"": {"model": model("warehouse_pack_spot", W.pack_spot_model())}}})
    item("warehouse_pack_spot", rid("block/warehouse_pack_spot"))

    # Loads: the pallet with n cases of a kind, on the floor and on a place's beams.
    write(models / "warehouse_pallet.json", PI.model(W.pallet(), GLOW, rid))
    for kind in KINDS:
        for n in range(1, MAX_CASES + 1):
            model(f"warehouse_load_{kind}_{n}", load_boxes(kind, n))
            model(f"warehouse_load_{kind}_{n}_beam", load_boxes(kind, n, W.BEAM_TOP))
    for kind in X4.DRUM_KINDS:
        for n in range(1, 5):
            model(f"warehouse_load_drum_{kind}_{n}", X4.drum_load_boxes(kind, n))
            model(f"warehouse_load_drum_{kind}_{n}_beam", X4.drum_load_boxes(kind, n, W.BEAM_TOP))

    # The rack: its frame by where it stands in the run, plus its pallet (LOAD).
    lang[f"block.{MOD}.warehouse_rack"] = SINGLE["warehouse_rack"]
    multipart = []
    for l in (False, True):
        for r in (False, True):
            for lv in LEVELS:
                for t in (False, True):
                    ref = model(f"warehouse_rack_{int(l)}{int(r)}_{lv}{int(t)}", rack_boxes(l, r, lv, t))
                    for face, y in TURN:
                        multipart.append({"when": {"facing": face, "left": str(l).lower(), "right": str(r).lower(), "level": lv, "top": str(t).lower()},
                                          "apply": {"model": ref, **({"y": y} if y else {})}})
    for kind in KINDS:
        for n in range(1, MAX_CASES + 1):
            idx = str(load_index(kind, n))
            for face, y in TURN:
                multipart.append({"when": {"facing": face, "load": idx, "level": "ground"},
                                  "apply": {"model": rid(f"block/warehouse_load_{kind}_{n}"), **({"y": y} if y else {})}})
                multipart.append({"when": {"facing": face, "load": idx, "level": "first|upper"},
                                  "apply": {"model": rid(f"block/warehouse_load_{kind}_{n}_beam"), **({"y": y} if y else {})}})
    for kind in X4.DRUM_KINDS:
        for n in range(1, 5):
            idx = str(drum_load_index(kind, n))
            for face, y in TURN:
                multipart.append({"when": {"facing": face, "load": idx, "level": "ground"},
                                  "apply": {"model": rid(f"block/warehouse_load_drum_{kind}_{n}"), **({"y": y} if y else {})}})
                multipart.append({"when": {"facing": face, "load": idx, "level": "first|upper"},
                                  "apply": {"model": rid(f"block/warehouse_load_drum_{kind}_{n}_beam"), **({"y": y} if y else {})}})
    spk = model("warehouse_rack_sprinkler", X.rack_sprinkler())
    for face, y in TURN:
        multipart.append({"when": {"facing": face, "sprinkler": "true"}, "apply": {"model": spk, **({"y": y} if y else {})}})
    write(states / "warehouse_rack.json", {"multipart": multipart})
    write(assets / "models" / "item" / "warehouse_rack.json",
          _item_model([(rack_boxes(True, True, "upper", True) + load_boxes("carton", 8, W.BEAM_TOP), (0, 0, 0))], rid))
    item("warehouse_rack", rid("item/warehouse_rack"))

    # The dock, its conduit, the trunking: turned by facing.
    for name, boxes in (("warehouse_dock", W.charge_dock()), ("warehouse_conduit", W.charge_dock_upper()), ("warehouse_trunking", W.trunking())):
        lang[f"block.{MOD}.{name}"] = SINGLE[name]
        ref = model(name, _darken(boxes))
        write(states / f"{name}.json", {"variants": {f"facing={face}": {"model": ref, **({"y": y} if y else {})} for face, y in TURN}})
        item(name, ref)

    write_w3(write, model, item, rid, states, assets, lang)

    # The hidden blocks the game draws moving things with: the drones' parts and the cases; the pallet loads.
    assert len(PART_NAMES) <= PART_STATES
    part_refs = [model(f"warehouse_part_{n}", part_boxes(n)) for n in PART_NAMES]
    write(states / "warehouse_part.json", {"variants": {f"part={i}": {"model": part_refs[i] if i < len(part_refs) else part_refs[0]}
                                                        for i in range(PART_STATES)}})
    loads = {"load=0": {"model": rid("block/warehouse_pallet")}}
    for kind in KINDS:
        for n in range(1, MAX_CASES + 1):
            loads[f"load={load_index(kind, n)}"] = {"model": rid(f"block/warehouse_load_{kind}_{n}")}
    for kind in X4.DRUM_KINDS:
        for n in range(1, 5):
            loads[f"load={drum_load_index(kind, n)}"] = {"model": rid(f"block/warehouse_load_drum_{kind}_{n}")}
    write(states / "warehouse_load.json", {"variants": loads})
    for name, display in HIDDEN.items():
        lang[f"block.{MOD}.{name}"] = display

    # Items: the pallet; the drones parked.
    write(assets / "models" / "item" / "warehouse_pallet.json", _item_model([(W.pallet(), (0, 0, 0))], rid, gui=0.7))
    item("warehouse_pallet", rid("item/warehouse_pallet"))
    write(assets / "models" / "item" / "stacker_drone.json", _item_model([(stacker_item_boxes(), (0, 0, 0))], rid, gui=0.7))
    item("stacker_drone", rid("item/stacker_drone"))
    write(assets / "models" / "item" / "pallet_drone.json", _item_model([(pallet_drone_item_boxes(), (0, 0, 0))], rid, gui=0.75))
    item("pallet_drone", rid("item/pallet_drone"))
    write(assets / "models" / "item" / "rack_sprinkler_kit.json", _item_model([(X.rack_sprinkler(), (0, 0, 0))], rid, gui=0.8))
    item("rack_sprinkler_kit", rid("item/rack_sprinkler_kit"))
    # Part W4: the drums (an empty one; a full one looks the same).
    for kind, (name, _) in X4.DRUM_ITEMS.items():
        write(assets / "models" / "item" / f"{name}.json", _item_model([(X4.drum_item(kind), (0, 0, 0))], rid, gui=1.0))
        item(name, rid(f"item/{name}"))
    for name, display in ITEMS.items():
        lang[f"item.{MOD}.{name}"] = display
    lang.update(LANG)
    lang.update(X5.LANG_W5)


LANG = {
    # Tooltips.
    "tooltip.jugcraft.warehouse.lane": "Drones drive on these. Lay them as aisles and loops; yellow lines show their edges.",
    "tooltip.jugcraft.warehouse.rack": "Pallet places, one per block, up to 5 high. The aisle side faces the way you look.",
    "tooltip.jugcraft.warehouse.intake": "Packs what is piped or dropped in into cases for the stacker drones.",
    "tooltip.jugcraft.warehouse.station": "Holds empty pallets (use pallets on it) and builds them up with cases.",
    "tooltip.jugcraft.warehouse.terminal": "The warehouse's books: everything stored, and the drones.",
    "tooltip.jugcraft.warehouse.goods_in": "Set in an outside wall behind an Intake Case Packer: pipe inlets and a drop-off hatch.",
    "tooltip.jugcraft.warehouse.dock": "Use a drone on it to park it here. It charges and waits for work.",
    "tooltip.jugcraft.warehouse.trunking": "Connects a terminal to the warehouse floor.",
    "tooltip.jugcraft.warehouse.pallet": "Load a Palletizing Station's magazine with these.",
    "tooltip.jugcraft.warehouse.drone": "Use on a Drone Charging Dock in a warehouse.",
    # Messages.
    "message.jugcraft.warehouse.no_terminal": "Not connected to a Warehouse Control Terminal",
    "message.jugcraft.warehouse.no_intake": "No Intake Case Packer against the wall behind this panel",
    "message.jugcraft.warehouse.docked": "%s parked at the dock",
    "message.jugcraft.warehouse.dock_taken": "There is already a drone at this dock",
    "message.jugcraft.warehouse.pallets": "Magazine: %s pallets",
    "message.jugcraft.warehouse.magazine_full": "The magazine is full",
    "message.jugcraft.warehouse.pallet_contents": "Pallet at %s: %s",
    "message.jugcraft.warehouse.empty_place": "Place %s: empty",
    # Screens.
    "container.jugcraft.goods_in": "Goods In",
    "container.jugcraft.warehouse_terminal": "Warehouse Management",
    "gui.jugcraft.warehouse.insert_all": "INSERT ALL",
    "gui.jugcraft.warehouse.insert_matching": "INSERT MATCHING",
    "gui.jugcraft.warehouse.take_all": "TAKE ALL BACK",
    # Part W2.
    "tooltip.jugcraft.warehouse.ready_storage": "Orders come out here: its hatch takes pallets in, its pick cell takes what was ordered. 108 slots.",
    "tooltip.jugcraft.warehouse.ready_storage_ext": "Stand it on Ready Storage (the same way round): 108 more slots.",
    "tooltip.jugcraft.warehouse.goods_out": "Set in an outside wall in front of Ready Storage: the pickup window, the order kiosk, the Output Port.",
    "message.jugcraft.warehouse.no_ready_storage": "No Ready Storage against the wall behind this panel",
    "message.jugcraft.warehouse.ordered": "Order #%s placed: %s x%s",
    "message.jugcraft.warehouse.not_stocked": "The warehouse has none of that",
    "message.jugcraft.warehouse.too_many": "You already have %s orders open: wait for some to come out",
    "message.jugcraft.warehouse.cancelled": "Order #%s cancelled",
    "message.jugcraft.warehouse.ready_storage": "Ready Storage: %s of %s slots used",
    "container.jugcraft.goods_out": "Goods Out",
    "container.jugcraft.goods_out_kiosk": "Goods Out Kiosk",
    "container.jugcraft.output_port": "Output Port",
    # Part W3.
    "tooltip.jugcraft.warehouse.lift_landing": "One on each floor, its front to an aisle; shaft sections between them, the drive on top. Carries pallets between the floors.",
    "tooltip.jugcraft.warehouse.lift_shaft": "Stand these between two Pallet Lift Landings, as many as the floor is high.",
    "tooltip.jugcraft.warehouse.lift_drive": "Goes on the top landing (or shaft section) of a Pallet Lift.",
    "tooltip.jugcraft.warehouse.walkway": "For people: drones keep off it. Cross it with Drone Crossing tiles.",
    "tooltip.jugcraft.warehouse.crossing": "Where drones may cross a walkway (they drive on it like a lane tile).",
    "tooltip.jugcraft.warehouse.crossing_beacon": "Stand one each side of a crossing: they flash while a drone is coming through.",
    "tooltip.jugcraft.warehouse.aisle_sign": "Hang it over an aisle's end: it shows the aisle, its floor, its bays and levels.",
    "tooltip.jugcraft.warehouse.end_barrier": "Protects a rack's end from the drones.",
    "tooltip.jugcraft.warehouse.slab": "The floor under racks on an upper floor.",
    "tooltip.jugcraft.warehouse.rack_sprinkler_kit": "Use on a pallet place to fit an in-rack sprinkler line (it works with the building's sprinkler system).",
    "message.jugcraft.warehouse.lift": "%s: carriage at floor %s, %s",
    "gui.jugcraft.warehouse.lift_idle": "idle",
    "gui.jugcraft.warehouse.lift_moving": "moving",
    # The pre-made warehouses (blueprint/BlueprintStacks.java) and the terminal's BUILDING page (ADD A FLOOR).
    "blueprint.jugcraft.small_warehouse": "Small Warehouse",
    "blueprint.jugcraft.medium_warehouse": "Medium Warehouse",
    "blueprint.jugcraft.large_warehouse": "Large Warehouse",
    "item.jugcraft.blueprint.floors": "Blueprint: %s, %s floors",
    "message.jugcraft.blueprint.floors": "%s: %s floors   (shift + scroll: %s ... %s floors)",
    "message.jugcraft.warehouse.floor_staked": "Floor %s staked out over the roof: Drone Towers in range build it (%s); anyone can place blocks by hand",
    "message.jugcraft.warehouse.floor_cancelled": "Floor %s cancelled: the stake is pulled up",
    "message.jugcraft.warehouse.floor_built": "Floor %s of %s is built: its racks, docks and lift landing join the warehouse",
    "message.jugcraft.warehouse.floor_refund": "Taken off the old top floor and given back: %s",
    "message.jugcraft.warehouse.not_premade": "This warehouse isn't one of the pre-made ones: floors are added by hand",
    "message.jugcraft.warehouse.floor_too_tall": "No room for another floor under the build limit (y %s)",
    "message.jugcraft.warehouse.floor_no_room": "Something is in the way of the stake over the roof",
    "message.jugcraft.warehouse.floor_busy": "A floor is already being added",
    # Part W4: liquids in drums.
    "tooltip.jugcraft.warehouse.drum_filler": "Against an outside wall behind a Liquids In panel: fills drums with what is piped in, four to a pallet, for the racks. Load it with empty drums and pallets.",
    "tooltip.jugcraft.warehouse.drum_emptier": "By Ready Storage, against an outside wall behind a Liquids Out panel: pumps ordered liquids out of drums into its holding tank.",
    "tooltip.jugcraft.warehouse.liquids_in": "Set in an outside wall behind a Drum Filling Station: four pipe inlets, a tanker coupling and a funnel to tip buckets into.",
    "tooltip.jugcraft.warehouse.liquids_out": "Set in an outside wall behind a Drum Emptying Station: the OUT flange for pipes and a fill point for buckets.",
    "tooltip.jugcraft.warehouse.drum": "Holds 20 buckets of one liquid. Load empty ones into a Drum Filling Station.",
    "tooltip.jugcraft.warehouse.drum_for": "For %s liquids",
    "tooltip.jugcraft.warehouse.drum_kind.steel": "general",
    "tooltip.jugcraft.warehouse.drum_kind.red": "flammable",
    "tooltip.jugcraft.warehouse.drum_kind.poly": "corrosive",
    "tooltip.jugcraft.warehouse.drum_kind.dewar": "cryogenic",
    "message.jugcraft.warehouse.no_filler": "No Drum Filling Station against the wall behind this panel",
    "message.jugcraft.warehouse.no_emptier": "No Drum Emptying Station against the wall behind this panel",
    "message.jugcraft.warehouse.drum_magazine": "Drums: %s steel, %s flammables, %s HDPE, %s dewars (%s places free); pallets: %s",
    "message.jugcraft.warehouse.drum_magazine_full": "No room in the drum magazine",
    "message.jugcraft.warehouse.drum_full": "Only empty drums go in the magazine",
    "message.jugcraft.warehouse.filled_bucket": "Filled: %s",
    "message.jugcraft.warehouse.nothing_to_fill": "Nothing ready for you at the fill point",
    "message.jugcraft.warehouse.refused_liquid": "The warehouse doesn't store %s",
    "message.jugcraft.warehouse.liquid_ordered": "Order #%s placed: %s B of %s",
    "message.jugcraft.warehouse.tipped": "Tipped %s B into the filling station",
    "container.jugcraft.liquids_in": "Liquids In",
    "container.jugcraft.liquids_out": "Liquids Out",
}


def write_loot(write, rid, out, self_drop):
    """The hidden blocks drop nothing (the others' loot comes with the machine blocks')."""
    for name in HIDDEN:
        write(out / f"{name}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{name}")})


CRAFTING = {
    "warehouse_lane": (["CYC", "CCC", "CYC"], {"C": "minecraft:light_gray_concrete", "Y": "minecraft:yellow_dye"}, 8),
    "warehouse_pack_spot": (["YWY", "WCW", "YWY"], {"C": "minecraft:light_gray_concrete", "Y": "minecraft:yellow_dye", "W": "minecraft:white_dye"}, 1),
    "warehouse_rack": (["B B", "IOI", "B B"], {"B": "minecraft:blue_dye", "I": "#c:ingots/iron", "O": "minecraft:orange_dye"}, 4),
    "warehouse_pallet": (["PPP", "S S", "PPP"], {"P": "#minecraft:planks", "S": "minecraft:stick"}, 4),
    "warehouse_trunking": (["III", "C C", "III"], {"I": "#c:ingots/iron", "C": "jugcraft:copper_cable"}, 8),
    "warehouse_conduit": ([" I ", " C ", " I "], {"I": "#c:ingots/iron", "C": "jugcraft:copper_cable"}, 4),
    "warehouse_dock": (["SCS", "PRP", "SSS"], {"S": "#c:plates/steel", "C": "jugcraft:copper_cable", "P": "minecraft:copper_ingot",
                                              "R": "minecraft:redstone_block"}, 1),
    "warehouse_station": (["Y Y", "YMY", "SCS"], {"Y": "minecraft:yellow_dye", "M": "jugcraft:electric_motor", "S": "#c:plates/steel",
                                                 "C": "jugcraft:basic_circuit"}, 1),
    "warehouse_intake": (["SHS", "MCM", "SPS"], {"S": "#c:plates/steel", "H": "minecraft:hopper", "M": "jugcraft:electric_motor",
                                                "C": "jugcraft:basic_circuit", "P": "minecraft:piston"}, 1),
    "warehouse_goods_in": (["SHS", "PCP", "SSS"], {"S": "#c:plates/steel", "H": "minecraft:hopper", "P": "jugcraft:brass_item_pipe",
                                                  "C": "jugcraft:basic_circuit"}, 1),
    "warehouse_terminal": (["GGG", "CPC", "SSS"], {"G": "minecraft:glass_pane", "C": "jugcraft:advanced_circuit", "P": "jugcraft:processor",
                                                  "S": "#c:plates/steel"}, 1),
    "stacker_drone": (["YAY", "CMC", "SBS"], {"Y": "minecraft:yellow_dye", "A": "minecraft:piston", "C": "jugcraft:advanced_circuit",
                                             "M": "jugcraft:electric_motor", "S": "#c:plates/steel", "B": "minecraft:redstone_block"}, 1),
    "warehouse_ready_storage": (["SGS", "MPM", "SCS"], {"S": "#c:plates/steel", "G": "minecraft:glass_pane", "M": "jugcraft:electric_motor",
                                                       "P": "minecraft:piston", "C": "jugcraft:advanced_circuit"}, 1),
    "warehouse_ready_storage_ext": (["SGS", "S S", "SMS"], {"S": "#c:plates/steel", "G": "minecraft:glass_pane", "M": "jugcraft:electric_motor"}, 1),
    "warehouse_goods_out": (["SGS", "PCP", "SSS"], {"S": "#c:plates/steel", "G": "minecraft:glass_pane", "P": "jugcraft:brass_item_pipe",
                                                   "C": "jugcraft:basic_circuit"}, 1),
    "warehouse_lift_landing": (["YGY", "MCM", "SSS"], {"Y": "minecraft:yellow_dye", "G": "minecraft:iron_bars", "M": "jugcraft:electric_motor",
                                                      "C": "jugcraft:basic_circuit", "S": "#c:plates/steel"}, 1),
    "warehouse_lift_shaft": (["Y Y", "BBB", "Y Y"], {"Y": "minecraft:yellow_dye", "B": "minecraft:iron_bars"}, 2),
    "warehouse_lift_drive": (["SCS", "MGM", "SSS"], {"S": "#c:plates/steel", "C": "jugcraft:basic_circuit", "M": "jugcraft:electric_motor",
                                                    "G": "minecraft:chain"}, 1),
    "warehouse_walkway": (["CGC", "CCC", "CYC"], {"C": "minecraft:light_gray_concrete", "G": "minecraft:green_dye", "Y": "minecraft:yellow_dye"}, 8),
    "warehouse_crossing": (["CWC", "CCC", "CWC"], {"C": "minecraft:light_gray_concrete", "W": "minecraft:white_dye"}, 8),
    "warehouse_crossing_beacon": ([" L ", " Y ", " I "], {"L": "minecraft:redstone_lamp", "Y": "minecraft:yellow_dye", "I": "#c:ingots/iron"}, 2),
    "warehouse_aisle_sign": (["NBN", "BWB"], {"N": "minecraft:iron_nugget", "B": "minecraft:blue_dye", "W": "minecraft:white_dye"}, 2),
    "warehouse_end_barrier": (["YIY", "III"], {"Y": "minecraft:yellow_dye", "I": "#c:ingots/iron"}, 2),
    "warehouse_bollard": (["Y", "I", "I"], {"Y": "minecraft:yellow_dye", "I": "#c:ingots/iron"}, 2),
    "rack_sprinkler_kit": (["RNR"], {"R": "minecraft:red_dye", "N": "minecraft:iron_nugget"}, 2),
    "warehouse_slab": (["CCC", "CIC", "CCC"], {"C": "minecraft:light_gray_concrete", "I": "#c:ingots/iron"}, 8),
    "pallet_drone": (["CMC", "SPS", "MBM"], {"C": "jugcraft:advanced_circuit", "M": "jugcraft:electric_motor", "S": "#c:plates/steel",
                                            "P": "minecraft:sticky_piston", "B": "minecraft:redstone_block"}, 1),
    # Part W4.
    "warehouse_drum_filler": (["YMY", "CTC", "SSS"], {"Y": "minecraft:yellow_dye", "M": "jugcraft:electric_motor", "C": "jugcraft:basic_circuit",
                                                     "T": "jugcraft:fluid_tank", "S": "#c:plates/steel"}, 1),
    "warehouse_drum_emptier": (["YPY", "CTC", "SSS"], {"Y": "minecraft:yellow_dye", "P": "jugcraft:electric_pump", "C": "jugcraft:basic_circuit",
                                                      "T": "jugcraft:fluid_tank", "S": "#c:plates/steel"}, 1),
    "warehouse_liquids_in": (["SBS", "PCP", "SSS"], {"S": "#c:plates/steel", "B": "minecraft:bucket", "P": "jugcraft:bronze_fluid_pipe",
                                                    "C": "jugcraft:basic_circuit"}, 1),
    "warehouse_liquids_out": (["SGS", "PCP", "SSS"], {"S": "#c:plates/steel", "G": "minecraft:glass_pane", "P": "jugcraft:bronze_fluid_pipe",
                                                     "C": "jugcraft:basic_circuit"}, 1),
    "steel_drum": (["SBS", "S S", "SSS"], {"S": "#c:plates/steel", "B": "minecraft:blue_dye"}, 2),
    "flammables_drum": (["SRS", "S S", "SSS"], {"S": "#c:plates/steel", "R": "minecraft:red_dye"}, 2),
    "hdpe_drum": (["P P", "P P", "PPP"], {"P": "jugcraft:plastic_sheet"}, 2),
    "liquid_dewar": (["SGS", "S S", "SSS"], {"S": "#c:plates/steel", "G": "minecraft:glass"}, 1),
    # Part W5.
    **X5.CRAFTING_W5,
}
FEATURE = "machines"
ITEM_FEATURES = {"processor": ["silicon"], "advanced_circuit": [], "basic_circuit": [], "electric_motor": [], "copper_cable": [],
                 "plastic_sheet": ["crude_oil"]}


def recipe_features(result, feature_of):
    found = set()
    for ref in CRAFTING[result][1].values():
        if ref.startswith("#c:") and ref.split("/")[0] in ("#c:plates", "#c:ingots", "#c:storage_blocks", "#c:gears"):
            metal = ref.split("/")[-1]
            if metal not in ("copper", "iron", "gold", "steel"):
                found.add(feature_of(f"{metal}_ingot"))
        elif ref.startswith(f"{MOD}:"):
            found.update(ITEM_FEATURES.get(ref.split(":")[1], []))
    return found - {FEATURE}


# ------------------------------------------------------------------------------------------------ textures
def _dark_screen():
    from PIL import ImageDraw
    img = W._img(64, 40, (8, 12, 18))
    d = ImageDraw.Draw(img)
    d.line([(0, 0), (63, 39)], fill=(20, 26, 34, 255))
    return W._sq(img)


def _charger_dark():
    """The charger's front with its display and bar dark (the game lights them)."""
    from PIL import ImageDraw
    img = W._img(256, 256, (196, 200, 204))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 255], outline=(150, 154, 158, 255), width=6)
    d.rectangle([28, 30, 228, 92], fill=(14, 18, 24, 255))
    d.rectangle([34, 106, 222, 130], fill=(14, 18, 24, 255))
    for y in range(150, 230, 12):
        d.rectangle([40, y, 216, y + 5], fill=(120, 124, 128, 255))
    return img


def _lift_plate():
    """A landing's name plate over its opening, blank (the game writes the lift's name and the floor on it): white, the
    floor's black field on its left, its border."""
    from PIL import ImageDraw
    img = W._img(512, 186, (246, 246, 240))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 511, 185], outline=(30, 30, 32, 255), width=6)
    d.rectangle([0, 0, 150, 185], fill=(30, 30, 32, 255))
    return W._sq(img)


def _aisle_plate():
    """An aisle sign's face, blank (the game writes the aisle, its floor, bays and levels)."""
    from PIL import ImageDraw
    img = W._img(256, 152, (24, 60, 120))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 151], outline=(246, 246, 240, 255), width=6)
    return W._sq(img)


def _loc_plate(arrow):
    """A rack location label without its writing (the game writes the place's code on it)."""
    return W._loc_label("", arrow)


def draw_block_textures():
    """Every texture the warehouse's models use that isn't the plant kit's already: name -> image."""
    makers = dict(W.makers())
    ext = W.exterior_makers()
    for name in ("pi_wh_panel_face", "pi_wh_panel_back", "pi_wh_sign_in", "pi_wh_lbl_in1", "pi_wh_lbl_in2", "pi_wh_lbl_in3",
                 "pi_wh_lbl_in4", "pi_wh_hatch_flap", "pi_wh_wallpack"):
        if name in ext:
            makers[name] = ext[name]
    used = textures_used()
    for name, make in list(ext.items()) + list(W.vlm_makers().items()):
        if name in used:
            makers.setdefault(name, make)
    makers["pi_wh_wallpack"] = lambda: W._stack((255, 244, 214), True)
    makers["pi_wh_lamp_green_off"] = lambda: W._stack((60, 220, 90), False)
    makers["pi_wh_lamp_red_on"] = lambda: W._stack((255, 60, 40), True)
    makers["pi_wh_lamp_amber_on"] = lambda: W._stack((255, 170, 30), True)
    makers["pi_wh_lamp_blue_on"] = lambda: W._stack((60, 120, 255), True)
    makers["pi_wh_scr_off"] = _dark_screen
    makers["pi_wh_charger_dark"] = _charger_dark
    makers["pi_wh_loc_label_up"] = lambda: _loc_plate("up")
    makers["pi_wh_loc_label_dn"] = lambda: _loc_plate("down")
    # Part W3.
    for name, make in X.w3_makers().items():
        makers.setdefault(name, make)
    makers["pi_wh_lift_plate"] = _lift_plate
    makers["pi_wh_aisle_plate"] = _aisle_plate
    # Part W4.
    for name, make in list(X4.w4_makers().items()) + list(X4.game_makers().items()) + list(X5.w5_makers().items()) + list(X5.game_makers().items()):
        if name in used:
            makers.setdefault(name, make)
    sharp = W.SHARP | W.EXTERIOR_SHARP | W.VLM_SHARP | X.W3_SHARP | X4.W4_SHARP | X4.GAME_SHARP | X5.W5_SHARP | X5.GAME_SHARP | {"pi_wh_lift_plate", "pi_wh_aisle_plate"} | {"pi_wh_lamp_red_on", "pi_wh_lamp_amber_on", "pi_wh_lamp_blue_on", "pi_wh_lamp_green", "pi_wh_scr_off", "pi_wh_charger_dark", "pi_wh_loc_label_up", "pi_wh_loc_label_dn"}
    see = W.SEE_THROUGH | W.VLM_SEE_THROUGH | X.W3_SEE_THROUGH
    out = {}
    from PIL import Image
    for name, make in makers.items():
        src = make()
        img = src if (name in sharp or name in see) else M.stylize(src, 18)
        if img.size[0] > 128 or img.size[1] > 128:
            img = img.convert("RGBA").resize((128, 128), Image.LANCZOS)
        out[name] = img
    return out


def textures_used():
    """Every texture name the written models point at (to check they are all drawn)."""
    names = set()
    boxes = []
    for name in PIECES:
        boxes += piece_boxes(name)
    boxes += W.lane_model(0, 0) + W.pack_spot_model() + W.charge_dock() + W.charge_dock_upper() + W.trunking()
    for l in (False, True):
        for r in (False, True):
            for lv in LEVELS:
                boxes += rack_boxes(l, r, lv, True) + rack_boxes(l, r, lv, False)
    for kind in KINDS:
        boxes += load_boxes(kind, 8)
    for kind in X4.DRUM_KINDS:
        boxes += X4.drum_load_boxes(kind, 4)
    for fn in PARTS.values():
        boxes += fn()
    boxes += aisle_sign_boxes() + X.end_barrier() + X.crossing_beacon(True) + X.crossing_beacon(False) + W.bollard() + X.rack_sprinkler()
    boxes += X.walkway_model(15, 1) + X.walkway_model(0, 0) + X.crossing_model(0) + X.crossing_model(1)
    for b in _darken(boxes):
        t = b[6]
        for v in (t.values() if isinstance(t, dict) else [t]):
            if v is None:
                continue
            n = v[0] if isinstance(v, tuple) else v
            names.add(n.split("@")[0].rstrip("!"))
    return names
