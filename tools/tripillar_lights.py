"""Keeps mobs from spawning on the Tri-Pillar (tools/tower_tripillar.py calls light_pass after building each step).

Monsters spawn in the overworld only where the block light is 0, on a solid floor with two clear blocks over it. This
works out the block light the tower's own lamps give (as the game spreads it: a level less a step, through air and
see-through blocks only) and, wherever a floor of the tower is still dark, sets a flush amber light into that floor
(jugcraft:amber_light_strip, light 12): on the lattice of deck lights where one of its points is near, else near
the middle of the dark patch, so they look laid out and few are needed. The lights go in
the step being built, so each step leaves the tower as a whole lit.
"""
from collections import deque

import numpy as np

import room_kit_recipes

AMBER = "jugcraft:amber_light_strip"
AMBER_LIGHT = 12
# the light a floor must have here: 2, one more than the game needs, for what this model doesn't see (a slab or stair
# letting light in on one side only, a part-block dimming it by one)
NEED = 2
# the deck lights' lattice: points with x a multiple of 9 and z - x a multiple of 18 (a diamond of reach 9 round each:
# 12 less the step up, less NEED)
LATTICE = 18
# light levels (tower/JugcraftTower, the hall fixtures, vanilla)
EMISSION = {
    "jugcraft:ceiling_light_panel": 15, "jugcraft:linear_ceiling_light": 15, "jugcraft:wall_task_light": 14,
    "minecraft:end_rod": 14, "jugcraft:warning_light": 13, AMBER: 12, "jugcraft:red_light_strip": 8,
    "jugcraft:hangar_pad": 6, "jugcraft:superconducting_conduit": 5, "jugcraft:landing_platform": 4, "jugcraft:equipment_rack": 3,
    "jugcraft:touch_console": 6, "jugcraft:tower_info_totem": 7, "jugcraft:holo_emitter": 9, "jugcraft:drone_display_stand": 4,
    "jugcraft:holo_projector": 11, "jugcraft:holo_projector_part": 7,
    "jugcraft:curved_video_wall": 7, "jugcraft:curved_video_wall_part": 7,
    "jugcraft:aisle_containment_roof": 12, "jugcraft:data_hall_sign": 10, "jugcraft:map_room_sign": 10,
    # the neon light lines (layout 21; tools/neon_lines.py): the amber boxes' and the end rods' light
    "jugcraft:neon_line_concrete": 12, "jugcraft:neon_line_steel": 12, "jugcraft:neon_line_carbon": 12,
    "jugcraft:neon_floor_concrete": 12, "jugcraft:neon_floor_steel": 12, "jugcraft:neon_floor_epoxy": 12,
    "jugcraft:neon_floor_dock": 12, "jugcraft:neon_tube": 14,
    # the inset floor lights where a floor line stood on its own (layout 28)
    "jugcraft:inset_light_concrete": 12, "jugcraft:inset_light_steel": 12, "jugcraft:inset_light_epoxy": 12,
    "jugcraft:inset_light_dock": 12,
}
EMISSION.update({f"jugcraft:{piece}": level for piece, level in room_kit_recipes.LIGHT.items()})
EMISSION["minecraft:light"] = 15       # the atrium dome's hidden lights where the cut's downlights hung (atrium_dome.fit)   # the rooms' lamps, screens
# a Maintenance Door: a hooded door's upper half (its downlight; tower/MaintenanceDoorBlock HOOD_LIGHT)
DOOR = "jugcraft:maintenance_door"
DOOR_HOOD_LIGHT = 9
# a Drone Display Case's cells: only its light hood's (the top layer) give light
CASE_PART = "jugcraft:drone_display_case_part"
CASE_HOOD_LIGHT = 12
# a Crystal Tree: its master (the trunk's foot) and its trunk's cells over it give light; the planter's cells do not
TREE, TREE_PART = "jugcraft:crystal_tree", "jugcraft:crystal_tree_part"
TREE_LIGHT, TREE_TRUNK_LIGHT = 9, 12
# the flight-ops trees (layout 45; tower/JugcraftTower): the Ember Oak's master and trunk cells, the Drift Isle's master
# and island cells give light
OPS_TREES = {"jugcraft:ember_oak": 10, "jugcraft:drift_isle": 9}
OPS_PARTS = {"jugcraft:ember_oak_part": 13, "jugcraft:drift_isle_part": 6}   # (the trunk's cells; the island's)
# blocks light passes through (glass, furniture and other part-blocks, ladders, slabs, stairs, leaves)
SEE_THROUGH = {
    "jugcraft:framed_glass", "jugcraft:blast_glass", "jugcraft:glass_door", "jugcraft:maintenance_door", "minecraft:end_rod",
    "jugcraft:neon_tube", "jugcraft:tower_ladder", "jugcraft:dome_shell", "minecraft:light",
    "jugcraft:steel_armor_plate_slab", "jugcraft:reinforced_concrete_stairs", "minecraft:azalea_leaves",
    "minecraft:flowering_azalea_leaves", "jugcraft:console_desk", "jugcraft:operator_chair", "jugcraft:sofa",
    "jugcraft:storage_rack", "jugcraft:equipment_rack", "jugcraft:holo_table", "jugcraft:control_screen",
    "jugcraft:speed_gate", "jugcraft:speed_gate_pedestal", "jugcraft:touch_console", "jugcraft:tower_info_totem",
    "jugcraft:holo_emitter", "jugcraft:drone_display_stand", "jugcraft:drone_service_arm", "jugcraft:cable_tray",
    "jugcraft:glass_coffee_table", "jugcraft:ceiling_light_panel", "jugcraft:linear_ceiling_light",
    "jugcraft:wall_task_light", "jugcraft:tower_master_console", "jugcraft:supply_pickup", "jugcraft:landing_pad",
    "jugcraft:tower_control_console", "jugcraft:console_chair", "jugcraft:raised_floor",
    "jugcraft:drone_depot_terminal", "jugcraft:cargo_packager", "jugcraft:curved_video_wall", "jugcraft:curved_video_wall_part",
    "jugcraft:drone_display_case", CASE_PART, TREE, TREE_PART, *OPS_TREES, *OPS_PARTS, "jugcraft:long_coffee_table", "jugcraft:long_coffee_table_part",
    "jugcraft:drone_parts_shelving", "jugcraft:drone_parts_shelving_part", "jugcraft:holo_projector", "jugcraft:holo_projector_part",
    # the joined girders and the cradles' struts (layout 22: no occlusion, no floor a mob stands on)
    "jugcraft:steel_girder", "jugcraft:cradle_struts", "jugcraft:cradle_struts_part",
    # the exchanges (tower/ExchangeStations: no occlusion)
    "jugcraft:energy_exchange", "jugcraft:cargo_exchange",
    # the data hall (layout 17): the plant's server-room pieces (no occlusion) and the containment
    "jugcraft:server_rack", "jugcraft:network_rack", "jugcraft:san_rack", "jugcraft:crac_unit", "jugcraft:ups_battery",
    "jugcraft:ups_module", "jugcraft:ups_bypass", "jugcraft:pdu_cabinet", "jugcraft:agent_cylinder", "jugcraft:agent_manifold",
    "jugcraft:agent_pipe", "jugcraft:agent_pipe_nozzle", "jugcraft:agent_pipe_elbow", "jugcraft:agent_pipe_tee",
    "jugcraft:agent_pipe_riser_top", "jugcraft:agent_release_panel", "jugcraft:agent_horn_strobe", "jugcraft:agent_abort_station",
    "jugcraft:agent_manual_release", "jugcraft:sign_agent_vacate", "jugcraft:sign_clean_agent", "jugcraft:sign_no_food",
    "jugcraft:smoke_detector", "jugcraft:srv_ladder", "jugcraft:srv_ladder_cross", "jugcraft:srv_ladder_riser",
    "jugcraft:fire_extinguisher", "jugcraft:emergency_light", "jugcraft:aisle_containment_door",
    "jugcraft:aisle_containment_door_part", "jugcraft:aisle_containment_roof", "jugcraft:data_hall_sign",
    # the automatic sliding doors (layout 47, tools/sliding_doors.py: no occlusion)
    "jugcraft:automatic_entrance_door", "jugcraft:automatic_sliding_door_glass", "jugcraft:automatic_sliding_door_steel",
    "jugcraft:automatic_sliding_door_single_glass", "jugcraft:automatic_sliding_door_single_steel", "jugcraft:sliding_door_part",
    # the map room (layout 23): its Big Map Table (no occlusion) and the sign over its doors
    "jugcraft:big_map_table", "jugcraft:big_map_table_part", "jugcraft:map_room_sign",
    # the ground floor's rooms (layout 24, tools/tripillar_rooms.py): the plant's interior kit and the shower stalls
    "jugcraft:cafe_chair", "jugcraft:locker", "jugcraft:station_battery", "jugcraft:cafe_table",
    "jugcraft:steel_door", "jugcraft:locker_bench", "jugcraft:office_plant", "jugcraft:locker_open",
    "jugcraft:crew_locker_open", "jugcraft:long_glass_table",
    "jugcraft:room_sign", "jugcraft:fridge", "jugcraft:restroom_sink", "jugcraft:kitchen_counter",
    "jugcraft:restroom_mirror", "jugcraft:restroom_stall_partition", "jugcraft:battery_disconnect",
    "jugcraft:restroom_toilet", "jugcraft:restroom_trash_can", "jugcraft:sec_alarm_console",
    "jugcraft:coffee_counter", "jugcraft:water_cooler", "jugcraft:shower_stall", "jugcraft:restroom_towel_dispenser",
    "jugcraft:restroom_hand_dryer", "jugcraft:restroom_stall_headrail", "jugcraft:restroom_exhaust_grille",
    "jugcraft:restroom_changing_station", "jugcraft:restroom_stall_door_accessible", "jugcraft:restroom_stall_door",
    "jugcraft:cctv_desk", "jugcraft:locker_security", "jugcraft:sec_weapons_locker", "jugcraft:glass_desk",
    "jugcraft:kitchen_sink", "jugcraft:sign_wash_hands", "jugcraft:sign_no_smoking", "jugcraft:exhaust_fan",
    "jugcraft:h2_monitor", "jugcraft:restroom_stall_panel", "jugcraft:restroom_urinal_screen",
    "jugcraft:restroom_urinal", "jugcraft:office_desk", "jugcraft:filing_cabinet", "jugcraft:eyewash_station",
    "jugcraft:spill_kit", "jugcraft:battery_room_sump", "jugcraft:coat_rack", "jugcraft:microwave_counter",
    "jugcraft:kitchen_stove", "jugcraft:aed_cabinet", "jugcraft:ops_radio_rack", "jugcraft:whiteboard",
    "jugcraft:sign_eyewash", "jugcraft:battery_log_board", "jugcraft:battery_ppe_cabinet", "jugcraft:ops_wall_clock",
    "jugcraft:sign_battery_acid",
    # the upper floor's rooms (layout 25, tools/tripillar_rooms_upper.py): the plant's signs, grilles and diffusers, and
    # every room kit piece (no occlusion: tower/RoomKit)
    "jugcraft:exit_sign", "jugcraft:hv_return_grille", "jugcraft:hv_supply_diffuser", "jugcraft:large_print",
    "jugcraft:sign_eye_protection",
} | {f"jugcraft:{piece}" for piece in room_kit_recipes.PIECES}
# plain floors a light may be set into (not pads, pickups, machines or glass)
PLAIN = {
    "jugcraft:reinforced_concrete", "jugcraft:carbon_composite_panel", "jugcraft:steel_armor_plate",
    "jugcraft:access_floor_tile", "jugcraft:dock_plating", "jugcraft:epoxy_floor", "jugcraft:hazard_plating",
}


def name(material):
    if material.startswith("fglass:"):
        return "jugcraft:framed_glass"
    return material.split("[")[0]


def emission(material):
    """The light a block gives (a display case's part by its layer)."""
    n = name(material)
    if n == CASE_PART:
        return CASE_HOOD_LIGHT if "oy=3" in material else 0
    if n == TREE:
        return TREE_LIGHT
    if n == TREE_PART:
        return 0 if "oy=0" in material else TREE_TRUNK_LIGHT
    if n in OPS_TREES:
        return OPS_TREES[n]
    if n == "jugcraft:ember_oak_part":
        return 0 if "oy=0" in material else OPS_PARTS[n]
    if n == "jugcraft:drift_isle_part":
        return OPS_PARTS[n] if "oy=2" in material else 0
    if n == DOOR:
        return DOOR_HOOD_LIGHT if "hood=true" in material and "half=upper" in material else 0
    return EMISSION.get(n, 0)


def spawnable_floor(material):
    """A floor a monster can stand on: a full solid block (not glass, a slab, leaves or a part-block)."""
    n = name(material)
    return n not in SEE_THROUGH


def light_pass(b, label=""):
    """Adds lights to ``b`` ({(x, y, z): material}) until no floor of it is dark; returns how many it added."""
    xs = [p[0] for p in b]
    ys = [p[1] for p in b]
    zs = [p[2] for p in b]
    x0, y0, z0 = min(xs) - 2, min(ys) - 2, min(zs) - 2
    shape = (max(xs) - x0 + 3, max(ys) - y0 + 4, max(zs) - z0 + 3)
    solid = np.zeros(shape, bool)        # blocks light
    emit = np.zeros(shape, np.int8)
    occupied = np.zeros(shape, bool)     # any block (not clear for a mob)
    floor = np.zeros(shape, bool)        # a block a mob could stand on
    plain = np.zeros(shape, bool)
    for (x, y, z), m in b.items():
        i = (x - x0, y - y0, z - z0)
        n = name(m)
        occupied[i] = True
        if n not in SEE_THROUGH:
            solid[i] = True
            floor[i] = True
        if n in PLAIN:
            plain[i] = True
        e = emission(m)
        if e:
            emit[i] = e
    light = propagate(emit, solid)

    def spawn_cells():
        # a mob's cell: clear, clear above, a floor below
        clear = ~occupied
        cell = np.zeros(shape, bool)
        cell[:, 1:-1, :] = clear[:, 1:-1, :] & clear[:, 2:, :] & floor[:, :-2, :]
        return cell

    cells = spawn_cells()
    dark = cells & (light < NEED)
    added = 0
    while dark.any():
        idx = np.argwhere(dark)
        # the first dark cell (lowest), then the middle of the dark cells on its floor level near it
        c = idx[np.lexsort((idx[:, 2], idx[:, 0], idx[:, 1]))[0]]
        near = idx[(idx[:, 1] == c[1]) & (np.abs(idx[:, 0] - c[0]) <= 6) & (np.abs(idx[:, 2] - c[2]) <= 6)]
        mid = near.mean(0)
        best = None
        # first choice: a point of the deck-light lattice (rows on the diagonals, 20 apart, so each light's reach
        # meets the next) within reach on this floor, the one that lights most of the dark cells
        level = idx[idx[:, 1] == c[1]]
        score = 0
        for dx in range(-LATTICE // 2, LATTICE // 2 + 1):
            for dz in range(-LATTICE // 2, LATTICE // 2 + 1):
                if abs(dx) + abs(dz) > LATTICE // 2:
                    continue
                wx, wz = int(c[0]) + x0 + dx, int(c[2]) + z0 + dz
                if wx % (LATTICE // 2) or (wz - wx) % LATTICE:
                    continue
                under = (wx - x0, int(c[1]) - 1, wz - z0)
                if not (0 <= under[0] < shape[0] and 0 <= under[2] < shape[2]) or not plain[under] or occupied[under[0], under[1] + 1, under[2]]:
                    continue
                lit = int((np.abs(level[:, 0] - under[0]) + np.abs(level[:, 2] - under[2]) <= AMBER_LIGHT - 1 - NEED).sum())
                if lit > score:
                    score, best = lit, under
        for cand in ([] if best is not None else near[np.argsort(((near - mid) ** 2).sum(1))]):
            under = (cand[0], cand[1] - 1, cand[2])
            if plain[under]:
                best = under
                break
        if best is None:
            # the patch is on something a light can't go into (a landing platform, a machine's top): the plain floor
            # near it, open above, that would light the most of the dark cells round it
            r = 8
            lo = np.maximum(c - r, 0)
            hi = np.minimum(c + r + 1, np.array(shape) - 1)
            box = plain[lo[0]:hi[0], lo[1]:hi[1], lo[2]:hi[2]] & ~occupied[lo[0]:hi[0], lo[1] + 1:hi[1] + 1, lo[2]:hi[2]]
            options = np.argwhere(box) + lo
            reach = idx[np.abs(idx - c).sum(1) <= 2 * r]
            score = -1
            for o in options:
                lit = int((np.abs(reach - o).sum(1) <= AMBER_LIGHT - NEED).sum())
                if lit > score:
                    score, best = lit, tuple(int(v) for v in o)
            if score <= 0:
                best = None
        if best is None:
            # (no floor to light it from: leave that patch, light the rest, and say so)
            print(f"  lights {label}: {len(near)} dark cells with no plain floor to light, e.g. {tuple(int(v) for v in c + (x0, y0, z0))}")
            for n in near:
                cells[tuple(n)] = False
            dark = cells & (light < NEED)
            continue
        b[(best[0] + x0, best[1] + y0, best[2] + z0)] = AMBER
        plain[best] = False
        emit[best] = AMBER_LIGHT
        spread(light, solid, best, AMBER_LIGHT)
        dark = cells & (light < NEED)
        added += 1
    return added


def propagate(emit, solid):
    """Block light everywhere: each block the brightest of its own light and its neighbours' less one (none into solid blocks)."""
    light = emit.astype(np.int8).copy()
    for _ in range(15):
        best = np.zeros_like(light)
        best[1:] = np.maximum(best[1:], light[:-1])
        best[:-1] = np.maximum(best[:-1], light[1:])
        best[:, 1:] = np.maximum(best[:, 1:], light[:, :-1])
        best[:, :-1] = np.maximum(best[:, :-1], light[:, 1:])
        best[:, :, 1:] = np.maximum(best[:, :, 1:], light[:, :, :-1])
        best[:, :, :-1] = np.maximum(best[:, :, :-1], light[:, :, 1:])
        new = np.maximum(emit, np.where(solid, 0, np.maximum(best - 1, 0))).astype(np.int8)
        if np.array_equal(new, light):
            break
        light = new
    return light


def spread(light, solid, at, level):
    """Adds one light of ``level`` at ``at`` to ``light`` (a breadth-first spread)."""
    light[at] = max(light[at], level)
    queue = deque([(at, level)])
    sx, sy, sz = light.shape
    while queue:
        (x, y, z), lv = queue.popleft()
        if lv <= 1:
            continue
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if not (0 <= n[0] < sx and 0 <= n[1] < sy and 0 <= n[2] < sz) or solid[n]:
                continue
            if light[n] < lv - 1:
                light[n] = lv - 1
                queue.append((n, lv - 1))
