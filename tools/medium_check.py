"""Walks the Medium Nuclear Plant (tools/medium_plant.py) the way a visitor would and reports what they would notice:

- furniture or decor equipment standing in an outer wall or a window, or with no room in front of it;
- doors with something in the way on either side;
- blocks floating in the air (not joined to the ground through other blocks);
- ceiling lights with nothing above them;
- floors you can't reach on foot from the building's doors (broken stairs, blocked doorways);
- dark places indoors (block light below 8 where you can stand).

Run: python3 tools/medium_check.py [--quiet]. Exits 1 if anything is found (dark spots only warn under a threshold).
"""
import sys
from collections import deque

import numpy as np

import nuclear_plants as P
import medium_plant as MP

B, O = P.BELOW, P.OVER
# Seats (you sit facing the way they face, so the room you need is in front), things hung on walls (nothing under
# them), and what may stand on a piece's user side (its chair, a coffee table in front of a sofa).
SEATED = {"sofa", "locker_bench", "stretcher"}
WALL_HUNG = {"tb_crane_compass", "chem_status_board", "sign_chem_lab", "sign_doc_control", "mgr_license", "mgr_awards", "mgr_hotline", "mgr_ppe_hooks", "ops_oos_board", "ops_schedule_board", "ops_wall_clock", "ops_radio_rack", "ops_ppe_shelf", "ops_key_locker", "ops_tag_rack",
             "ops_lockbox", "whiteboard", "framed_print", "large_print", "wall_display", "coat_rack", "aed_cabinet", "emergency_light", "emergency_light_conduit", "medicine_cabinet", "blueprint_sheet",
             "fire_extinguisher", "exhaust_fan", "sign_vital_area", "sign_prohibited", "rsr_key_cabinet", "rsr_phone_station", "sign_rsr_area", "sign_protected_area",
             "admin_evac_plan", "exit_sign", "fire_alarm_pull", "h2_monitor", "h2_sensor", "sign_no_smoking", "sign_battery_acid", "sign_hydrogen", "sign_eyewash",
             "sign_relay_row_a", "sign_relay_row_b", "sign_relay_row_c", "sign_relay_row_d", "hv_rad_monitor", "hv_tox_monitor", "hv_refrig_monitor",
             "tsc_video_wall", "tsc_class_board", "tsc_status_board", "tsc_area_monitor", "tsc_card_reader",
             # The diesel rooms' fittings (round 31 part 3: tools/diesel_fittings.py).
             "edg_horn_strobe", "edg_room_fan", "sign_co2", "sign_hearing",
             # The battery rooms' (older rooms round 4: tools/battery_rooms.py), the smoke detector under the ceiling.
             "battery_ppe_cabinet", "battery_log_board", "sign_battery_1a", "sign_battery_1b", "smoke_detector",
             # The plant computer room's (older rooms round 2: tools/server_room.py).
             "sign_srv_row_a", "sign_srv_row_b", "sign_srv_row_c", "sign_srv_row_d", "agent_abort_station", "agent_manual_release",
             "agent_horn_strobe", "sign_agent_vacate", "sign_no_food", "sign_clean_agent"}
# Signs hung on a row's end in the aisle you read them from (the computer room's row signs on the racks' ends): the
# reader stands in the sign's own cell, under it.
ROW_END_SIGNS = {"sign_srv_row_a", "sign_srv_row_b", "sign_srv_row_c", "sign_srv_row_d"}
# (A rubber mat, the black carpet, before electrical cabinets: older rooms round 3, the DC rooms.)
USER_SIDE_OK = ("jugcraft:operator_chair", "jugcraft:cafe_chair", "jugcraft:coffee_table", "minecraft:black_carpet")


def state_of(m):
    return P.MATERIALS[int(m)][0] if m else "minecraft:air"


def build_grids(site):
    """The site's blocks (materials), and a mask of everything solid in game (blocks, equipment bodies, pipes)."""
    g = P.assemble(site, render=False)
    solid = g != 0
    water = np.zeros_like(solid)
    for k, (st, _) in P.MATERIALS.items():
        if st.startswith("minecraft:water"):
            water |= g == k
    solid &= ~water
    # The invisible light blocks under high-bay lights are air you can walk through.
    for k, (st, _) in P.MATERIALS.items():
        if st.startswith("minecraft:light["):
            solid &= g != k
    occupied = np.zeros_like(solid)
    for kind, ax, ay, az, facing in site.equipment:
        for (x, y, z) in P.equipment_cells(kind, ax, ay, az, facing):
            occupied[x, y + B, z + O] = True
    for (x, y, z) in site.lines:
        occupied[x, y + B, z + O] = True
    for part, px, pz, rot in site.parts:
        for kind, ax, ay, az, facing in part.props:
            for (x, y, z) in P.equipment_cells(kind, ax, ay, az, facing):
                occupied[x + px, y - part.sink + B, z + pz + O] = True
    for x, y, z, kind in site.towers:
        occupied[x, y + B, z + O] = True
    return g, solid | occupied, water, occupied


PASSABLE_PREFIX = ("minecraft:air", "jugcraft:steel_door", "jugcraft:glass_door", "jugcraft:lead_door", "minecraft:ladder", "jugcraft:caged_ladder",
                   "jugcraft:railing_gate", "jugcraft:room_sign", "jugcraft:speed_gate",
                   "jugcraft:building_sign", "minecraft:rail", "jugcraft:ceiling_light_panel", "minecraft:iron_chain",
                   "jugcraft:cable_tray", "jugcraft:dc_cable_tray", "jugcraft:light_hanger", "jugcraft:high_bay_light", "minecraft:light[",
                   "jugcraft:roller_door_station", "jugcraft:battery_room_sump", "jugcraft:restroom_floor_drain",
                   "jugcraft:cr_atc_line", "jugcraft:tb_floor_mark")
SEE_THROUGH = PASSABLE_PREFIX + ("minecraft:glass", "jugcraft:framed_glass", "jugcraft:blast_glass", "jugcraft:lead_glass", "minecraft:water", "jugcraft:guard_rail", "jugcraft:railing",
                                 "jugcraft:storage_tank_fill_pipe", "jugcraft:storage_tank_inlet", "jugcraft:storage_tank_outlet",
                                 "jugcraft:storage_tank_manway", "jugcraft:storage_tank_vent", "jugcraft:storage_tank_hatch",
                                 "jugcraft:operator_chair", "jugcraft:office_desk", "jugcraft:console_desk", "jugcraft:operator_console", "jugcraft:speed_gate_pedestal",
                                 "jugcraft:cr_console", "jugcraft:ss_desk", "jugcraft:rsr_desk",
                                 "minecraft:iron_trapdoor", "jugcraft:water_cooler", "jugcraft:filing_cabinet",
                                 "jugcraft:whiteboard", "jugcraft:pallet_rack", "jugcraft:roller_door", "jugcraft:storage_rack",
                                 "jugcraft:dc_cable_riser", "jugcraft:station_battery")
LIGHTS = {"jugcraft:ceiling_light_panel": 15, "jugcraft:vapor_tight_light": 15, "jugcraft:high_bay_light": 15, "minecraft:light": 15, "minecraft:sea_lantern": 15, "jugcraft:warning_light": 13, "jugcraft:warning_beacon": 10,
          "minecraft:campfire": 15, "jugcraft:server_rack": 3, "jugcraft:relay_cabinet": 2, "jugcraft:office_desk": 4,
          "jugcraft:console_desk": 4, "jugcraft:operator_console": 4, "jugcraft:cr_console": 4, "jugcraft:ss_desk": 4, "jugcraft:rsr_desk": 4}


def table(fn):
    """A lookup array over material ids."""
    t = np.zeros(max(P.MATERIALS) + 2, bool)
    for k, (st, _) in P.MATERIALS.items():
        t[k] = fn(st)
    t[0] = fn("minecraft:air")
    return t


def check(site, quiet=False):
    g, solid, water, occupied = build_grids(site)
    problems = []
    passable = table(lambda st: st.startswith(PASSABLE_PREFIX))
    see = table(lambda st: st.startswith(SEE_THROUGH))
    high_bay = table(lambda st: st.startswith("jugcraft:high_bay_light"))
    # Full blocks (a high-bay light's light passes pipes, rails, railings, chains and furniture to the floor).
    full = table(lambda st: st != "minecraft:air" and not st.startswith(SEE_THROUGH) and not any(
        k in st for k in ("pipe", "rail", "chain", "hanger", "high_bay", "conduit", "tray", "ladder", "trapdoor", "pane", "bars")))
    light_of = np.zeros(max(P.MATERIALS) + 2, np.int8)
    for k, (st, _) in P.MATERIALS.items():
        base = st.split("[")[0]
        light_of[k] = LIGHTS.get(base, 0)
    W, H, D = g.shape
    floor_t = table(lambda st: st.startswith(("jugcraft:vinyl_floor", "jugcraft:epoxy_floor", "jugcraft:access_floor_tile",
                                              "jugcraft:carpet_tile", "minecraft:deepslate_tiles", "minecraft:smooth_stone",
                                              "minecraft:stone_brick_stairs", "minecraft:iron_trapdoor")))

    def at(x, y, z):
        return g[x, y + B, z + O]

    # 1. Furniture: inside the walls, with room in front (the user's side).
    for part, px, pz, rot in site.parts:
        if not isinstance(part, MP.Building):
            continue
        w, _, d = part.size
        for name, x, y, z, facing in part.seats:
            sx, sy, sz = x + px, y + part.base - part.sink, z + pz
            if x <= 0 or z <= 0 or x >= w - 1 or z >= d - 1:
                problems.append(f"{part.key}: {name} at {(x, y, z)} is in the outer wall")
                continue
            # The user's side: behind a desk or cabinet (they look the way it faces), but in front of a seat.
            dx, dz = P.DIRS[facing if name in SEATED else MP.OPPOSITE[facing]]
            # (An intake plenum's outlet opens into its duct.)
            if name in ("whiteboard", "large_print", "office_plant", "coffee_table", "cafe_table", "cubicle_panel", "vitals_monitor", "stretcher", "glass_coffee_table",
                        "hv_plenum_outlet") or name in ROW_END_SIGNS:
                continue
            front = g[sx + dx, sy + B, sz + dz + O]
            fst = state_of(front)
            if occupied[sx + dx, sy + B, sz + dz + O] or (front and not passable[front] and not fst.startswith(USER_SIDE_OK)):
                problems.append(f"{part.key}: {name} at {(x, y, z)} facing {facing} has {state_of(front)} in front of it")
            below = solid[sx, sy + B - 1, sz + O]
            if not below and name not in WALL_HUNG:
                problems.append(f"{part.key}: {name} at {(x, y, z)} stands on nothing")
        # Decor equipment must not stand in an outer wall either.
        for kind, ax, ay, az, facing in part.props:
            for (x, y, z) in P.equipment_cells(kind, ax, ay, az, facing):
                if (x in (0, w - 1) or z in (0, d - 1)) and part.key not in ("pump_house",):
                    problems.append(f"{part.key}: {kind} at {(ax, ay, az)} reaches into the outer wall at {(x, y, z)}")
                    break
        # 2. Doors: both sides clear.
        for x, y, z, facing in part.doors:
            dx, dz = P.DIRS[facing]
            for s_ in (1, -1):
                for yy in (y, y + 1):
                    cx, cy, cz = x + px + dx * s_, yy - part.sink, z + pz + dz * s_
                    m = g[cx, cy + B, cz + O]
                    if occupied[cx, cy + B, cz + O] or (m and not passable[m]):
                        problems.append(f"{part.key}: door at {(x, y, z)} ({facing}) is blocked by {state_of(m) if m else 'equipment'} at {(cx - px, cy, cz - pz)}")
                        break
        # Doors round: a door's badge reader hangs on the wall of the side you come from, by its latch jamb (a pair's,
        # by the first leaf's hinge jamb, outside the pair): nothing may stand in front of it.
        for x, y, z, facing in part.doors:
            m = g[x + px, y - part.sink + B, z + pz + O]
            st = state_of(m) if m else ""
            if "reader=true" not in st:
                continue
            hinge_right, paired = "hinge=right" in st, "paired=true" in st
            if paired and not hinge_right:
                continue
            dx, dz = P.DIRS[facing]
            cx_, cz_ = P.DIRS[P.CLOCKWISE[facing]]
            lx, lz = (cx_, cz_) if not hinge_right else (-cx_, -cz_)
            if paired:
                lx, lz = -lx, -lz
            for yy in (y, y + 1):
                cx, cy, cz = x + px + lx - dx, yy - part.sink, z + pz + lz - dz
                m2 = g[cx, cy + B, cz + O]
                if occupied[cx, cy + B, cz + O] or (m2 and not passable[m2]):
                    problems.append(f"{part.key}: the badge reader by the door at {(x, y, z)} is blocked by "
                                    f"{state_of(m2) if m2 else 'equipment'} at {(cx - px, cy, cz - pz)}")
                    break

    # 3. Floating: everything solid must join the ground (y <= 0) through face-touching solids.
    grounded = np.zeros_like(solid)
    q = deque()
    xs, ys, zs = np.nonzero(solid[:, :B + 1, :])
    for x, y, z in zip(xs.tolist(), ys.tolist(), zs.tolist()):
        grounded[x, y, z] = True
        q.append((x, y, z))
    while q:
        x, y, z = q.popleft()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            nx, ny, nz = x + dx, y + dy, z + dz
            if 0 <= nx < W and 0 <= ny < H and 0 <= nz < D and solid[nx, ny, nz] and not grounded[nx, ny, nz]:
                grounded[nx, ny, nz] = True
                q.append((nx, ny, nz))
    floating = solid & ~grounded
    fx, fy, fz = np.nonzero(floating)
    if len(fx):
        seen = {}
        for x, y, z in zip(fx.tolist(), fy.tolist(), fz.tolist()):
            st = state_of(g[x, y, z]) if g[x, y, z] else "equipment/pipe"
            seen.setdefault(st, []).append((x, y - B, z - O))
        for st, cells in sorted(seen.items(), key=lambda kv: -len(kv[1])):
            problems.append(f"floating: {len(cells)} x {st}, e.g. {cells[:4]}")

    # 4. Lights must hang from something.
    for k, (st, _) in P.MATERIALS.items():
        if st.startswith("jugcraft:ceiling_light_panel"):
            xs, ys, zs = np.nonzero(g == k)
            for x, y, z in zip(xs.tolist(), ys.tolist(), zs.tolist()):
                if y + 1 < H and not solid[x, y + 1, z]:
                    problems.append(f"light at {(x, y - B, z - O)} has nothing above it")

    # 5 and 6. Per building: reach every floor on foot from the doors; light where you can stand.
    dark_total = 0
    for part, px, pz, rot in site.parts:
        if not isinstance(part, MP.Building) or part.key in ("rwst_tank", "condensate_tank"):
            continue
        w, h, d = part.size
        x0, z0 = px, pz
        y0 = -part.sink
        sub_solid = solid[x0:x0 + w, y0 + B:y0 + B + h, z0 + O:z0 + O + d]
        sub_g = g[x0:x0 + w, y0 + B:y0 + B + h, z0 + O:z0 + O + d]
        sub_occ = occupied[x0:x0 + w, y0 + B:y0 + B + h, z0 + O:z0 + O + d]
        sub_water = water[x0:x0 + w, y0 + B:y0 + B + h, z0 + O:z0 + O + d]
        free = (~sub_solid | passable[sub_g]) & ~sub_occ & ~sub_water
        free[sub_solid & ~passable[sub_g]] = False
        stand = np.zeros_like(free)
        stand[:, 1:-1, :] = free[:, 1:-1, :] & free[:, 2:, :] & (sub_solid[:, :-2, :] & ~passable[sub_g[:, :-2, :]] | sub_occ[:, :-2, :]
                                                                  | np.isin(sub_g[:, :-2, :], [k for k, (st, _) in P.MATERIALS.items() if st.startswith(("minecraft:ladder", "jugcraft:caged_ladder"))]))
        # Ladders are climbable: a ladder cell counts as standable.
        ladder = np.isin(sub_g, [k for k, (st, _) in P.MATERIALS.items() if st.startswith(("minecraft:ladder", "jugcraft:caged_ladder"))])
        stand |= ladder & free
        # Start from every door and the cells just outside the building's outline at ground level.
        start = []
        for x, y, z, facing in part.doors:
            start.append((x, y + part.base, z))
        # A part without doors (an outdoor tank on its pad) is walked onto from its edge.
        if not part.doors:
            for x in range(w):
                for z in range(d):
                    if x in (0, w - 1) or z in (0, d - 1):
                        start.append((x, part.base + 1, z))
        reach = np.zeros_like(stand)
        q = deque()
        for c in start:
            if 0 <= c[1] < h and stand[c] or (0 <= c[1] < h and free[c]):
                reach[c] = True
                q.append(c)
        while q:
            x, y, z = q.popleft()
            nbrs = []
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                for dy in (1, 0, -1, -2, -3):
                    nbrs.append((x + dx, y + dy, z + dz))
            if ladder[x, y, z]:
                nbrs += [(x, y + 1, z), (x, y - 1, z)]
            for nx, ny, nz in nbrs:
                if 0 <= nx < w and 1 <= ny < h - 1 and 0 <= nz < d and stand[nx, ny, nz] and not reach[nx, ny, nz]:
                    # Climbing a step up needs headroom above where you are.
                    if ny > y and not free[x, min(h - 1, y + 2), z]:
                        continue
                    # Going down: the cells in between must be free (you drop through them).
                    if ny < y and not all(free[nx, yy, nz] for yy in range(ny, y + 1)):
                        continue
                    reach[nx, ny, nz] = True
                    q.append((nx, ny, nz))
        # Only floors count (standing on a slab, a stair, a landing or grating; not on furniture, equipment or roofs).
        on_floor = np.zeros_like(stand)
        on_floor[:, 1:, :] = floor_t[sub_g[:, :-1, :]] & ~sub_occ[:, :-1, :]
        inside = stand & on_floor
        inside[:, part.base + part.h + 1:, :] = False
        inside[0, :, :] = inside[-1, :, :] = inside[:, :, 0] = inside[:, :, -1] = False
        unreached = inside & ~reach
        # Count unreached floor area per level (ignore little pockets: under equipment, in pits).
        ux, uy, uz = np.nonzero(unreached)
        levels = {}
        for x, y, z in zip(ux.tolist(), uy.tolist(), uz.tolist()):
            levels.setdefault(y - part.base, []).append((x, z))
        for y, cells in sorted(levels.items()):
            if len(cells) >= 40:
                problems.append(f"{part.key}: {len(cells)} cells of floor at y {y} can't be reached on foot, e.g. {cells[:3]}")
        # Light: from every source through see-through cells, falling one a block.
        lvl = np.zeros(sub_g.shape, np.int8)
        src = light_of[sub_g]
        q = deque()
        for x, y, z in zip(*np.nonzero(src)):
            lvl[x, y, z] = src[x, y, z]
            q.append((int(x), int(y), int(z)))
        # A high-bay light also puts light (an invisible light block, nuclear/HighBayLightBlock.java) four over the
        # floor or whatever stands under it.
        for x, y, z in zip(*np.nonzero(high_bay[sub_g])):
            fy = int(y) - 1
            while fy > 0 and not (full[sub_g[x, fy, z]] or sub_occ[x, fy, z]):
                fy -= 1
            for up in (4, 3, 5, 2, 6):
                gy = fy + up
                if gy < int(y) - 1 and sub_g[x, gy, z] == 0 and not sub_occ[x, gy, z]:
                    if lvl[x, gy, z] < 15:
                        lvl[x, gy, z] = 15
                        q.append((int(x), gy, int(z)))
                    break
        trans = (~sub_solid | see[sub_g]) & ~(sub_occ & ~sub_solid)
        trans |= see[sub_g]
        while q:
            x, y, z = q.popleft()
            v = lvl[x, y, z] - 1
            if v <= 0:
                continue
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                nx, ny, nz = x + dx, y + dy, z + dz
                if 0 <= nx < w and 0 <= ny < h and 0 <= nz < d and trans[nx, ny, nz] and lvl[nx, ny, nz] < v:
                    lvl[nx, ny, nz] = v
                    q.append((nx, ny, nz))
        dark = reach & inside & (lvl < 6)
        dark[0, :, :] = dark[-1, :, :] = dark[:, :, 0] = dark[:, :, -1] = False
        n = int(dark.sum())
        dark_total += n
        if n > 30:
            dx_, dy_, dz_ = np.nonzero(dark)
            problems.append(f"{part.key}: {n} dark places you can stand (block light < 6), e.g. "
                            f"{[(int(a), int(b_) - part.base, int(c)) for a, b_, c in list(zip(dx_, dy_, dz_))[:4]]}")
    # 7. Power cables (round 25): each network joins only what it is meant to.
    problems += cable_networks(site, g, quiet)
    # 8. The switchgear room (older rooms round 1; round 25b's feeder conduits replaced by its overhead power trays):
    # each load centre's cables down from its tray, each run up through the ceiling into its own train's raceway.
    problems += feeder_conduits(site, g)
    problems += switchgear_room(site, g)
    # 9. Regulating transformers (round 26, 8d): each beside the inverter of its label, fed from its train's MCC, the
    # conduits up from its top and from the MCC's REG XF section reaching the same ladder tray; and (8e) each inverter's
    # vital panel, their conduits into the same tray.
    problems += regulating_transformers(site, g)
    # 10. The battery rooms (round 26, #9): their second strings, sense cable risers and fittings.
    problems += battery_rooms(site, g)
    problems += dc_rooms(site, g)
    problems += control_room(site, g)
    problems += entrance_hall(site, g)
    problems += remote_shutdown_room(site, g)
    problems += gatehouse(site, g)
    problems += relay_room(site, g)
    problems += hvac_room(site, g)
    problems += tsc_room(site, g)
    # 11. The cable spreading room (punch list #15).
    problems += spreading_room(site, g, occupied)
    problems += overview_screens(g, site)
    if not quiet:
        for p in problems:
            print(p)
    return problems


def feeder_conduits(site, g):
    """Every feeder conduit column runs unbroken from a load centre's in-use upper feeder hub to a cable tray."""
    import control_boards as CB
    kinds = {k: st.split("[")[0].split(":")[-1] for k, (st, _) in P.MATERIALS.items()}

    def at(x, y, z):
        if not (0 <= x < g.shape[0] and 0 <= y + B < g.shape[1] and 0 <= z + O < g.shape[2]):
            return ""
        m = int(g[x, y + B, z + O])
        return kinds.get(m, "") if m else ""
    problems = []
    hubs = set()
    for part, px, pz, rot in site.parts:
        for kind, ax, ay, az, facing in part.props:
            if kind != "switchgear_lineup":
                continue
            fx, fz = P.DIRS[facing]
            rx, rz = P.DIRS[P.CLOCKWISE[facing]]
            for k in range(5):
                if CB.LC_FEEDERS[2 * k] != "SPARE":
                    lx, lz = 3 + k, int(CB.LC_CONDUIT_Z)
                    hubs.add((ax + rx * lx + fx * lz + px, ay - part.sink + P.EQUIPMENT_SIZES[kind][3], az + rz * lx + fz * lz + pz))
    bases = [tuple(int(v) for v in c) for c in np.argwhere(np.isin(g, [k for k, v in kinds.items() if v == "feeder_conduit_base"]))]
    for x, y, z in bases:
        y, z = y - B, z - O
        if (x, y, z) not in hubs:
            problems.append(f"feeder conduit at {(x, y, z)} stands on no load centre's hub")
        k = y + 1
        while at(x, k, z).startswith("feeder_conduit") and at(x, k, z) != "feeder_conduit_ceiling":
            k += 1
        if at(x, k, z) != "feeder_conduit_ceiling":
            problems.append(f"feeder conduit from {(x, y, z)} does not reach the ceiling ({at(x, k, z)} at y {k})")
            continue
        k += 1
        while at(x, k, z) and not at(x, k, z).startswith("feeder_conduit"):
            k += 1
        if at(x, k, z) != "feeder_conduit_floor":
            problems.append(f"feeder conduit from {(x, y, z)} does not come up through the floor above ({at(x, k, z)} at y {k})")
            continue
        k += 1
        while at(x, k, z) == "feeder_conduit":
            k += 1
        if at(x, k, z) != "feeder_conduit_tray" or not at(x, k, z + 1).startswith("cable_tray"):
            problems.append(f"feeder conduit from {(x, y, z)} does not end in a cable tray ({at(x, k, z)} at y {k}, {at(x, k, z + 1)} beside it)")
    return problems


def turbine_building(site):
    """The turbine building's footprint in the grid (x0, z0, x1, z1): its plant rooms (turbine hall part 3) have a
    switchgear room, a lube oil room with its own pre-action sprinklers, a workshop with its eyewash and a turbine control
    room with its own overview screen, none of them the control building's, so the rules for those rooms leave it out."""
    for q, px, pz, _ in site.parts:
        if getattr(q, "key", "") == "pwr_turbine_hall":
            return (px, pz + O, px + q.size[0] - 1, pz + O + q.size[2] - 1)
    return (0, 0, -1, -1)


def switchgear_room(site, g):
    """The switchgear room (older rooms round 1; tools/switchgear_room.py, medium_plant.switchgear_room): every cable
    drop on a load centre's top under a tray drop, each lineup in the room four (its in-use feeder sections); each tray
    run's end up through the ceiling into its train's way up: LC 1B's Train B riser to its top, LC 1A's fire-wrapped
    raceway unbroken (rise, elbow, run, drop) down into a Train A tray; the log desk's laptop within its reach of a
    load centre (nuclear/SwitchgearRoom.java REACH)."""
    kinds = {k: st.split("[")[0].split(":")[-1] for k, (st, _) in P.MATERIALS.items()}
    problems = []

    tb = turbine_building(site)

    def blocks(*names, part=None):
        out = []
        for k, (st, _) in P.MATERIALS.items():
            if kinds[k] in names and (part is None or f"part={part}" in st):
                v = int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
                out += [(int(x), int(y) - B, int(z) - O, v) for x, y, z in np.argwhere(g == k)
                        if not (tb[0] <= x <= tb[2] and tb[1] <= z <= tb[3])]
        return out

    def at(x, y, z):
        if not (0 <= x < g.shape[0] and 0 <= y + B < g.shape[1] and 0 <= z + O < g.shape[2]):
            return ""
        m = int(g[x, y + B, z + O])
        return kinds.get(m, "") if m else ""

    def variant(x, y, z):
        st = P.MATERIALS.get(int(g[x, y + B, z + O]), ("", None))[0]
        return int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
    cells = {}
    for part, px, pz, rot in site.parts:
        for kind, ax, ay, az, facing in part.props:
            if kind == "switchgear_lineup":
                for cx, cy, cz in P.equipment_cells(kind, ax, ay, az, facing):
                    cells[(cx + px, cy - part.sink, cz + pz)] = (ax + px, ay - part.sink, az + pz)
    per = {}
    for x, y, z, v in blocks("lc_cable_drop"):
        lc = cells.get((x, y - 1, z))
        if lc is None:
            problems.append(f"a load centre cable drop at {(x, y, z)} is over no load centre")
        else:
            per[lc] = per.get(lc, 0) + 1
        if at(x, y + 1, z) != "cable_tray_power_down":
            problems.append(f"the cable drop at {(x, y, z)} hangs from {at(x, y + 1, z) or 'nothing'}, not a tray's drop")
    if len(per) < 2 or any(n != 4 for n in per.values()):
        problems.append(f"the switchgear room's load centres' cable drops: {sorted(per.values())}, not four on each of two")
    ends = blocks("cable_tray_power_ceiling")
    if len(ends) != 2:
        problems.append(f"{len(ends)} tray runs up through the switchgear room's ceiling, not 2")
    for x, y, z, v in ends:
        k = y + 1
        while k < y + 5 and at(x, k, z) not in ("cable_tray_riser_floor", "fire_wrap_floor"):
            k += 1
        up = at(x, k, z)
        if v % 3 == 1 and up == "cable_tray_riser_floor":
            k += 1
            while at(x, k, z) == "cable_tray_riser":
                k += 1
            if at(x, k, z) != "cable_tray_riser_top" or variant(x, k, z) % 3 != 1:
                problems.append(f"LC 1B's riser from {(x, y, z)} ends at {at(x, k, z) or 'nothing'}, not a Train B riser top")
        elif v % 3 == 0 and up == "fire_wrap_floor":
            k += 1
            while at(x, k, z) == "fire_wrap_rise":
                k += 1
            if at(x, k, z) != "fire_wrap_elbow":
                problems.append(f"LC 1A's fire wrap from {(x, y, z)} rises to {at(x, k, z) or 'nothing'}, not its elbow")
                continue
            j = x - 1
            while at(j, k, z) in ("fire_wrap", "fire_wrap_hanger"):
                j -= 1
            if at(j, k, z) != "fire_wrap_drop":
                problems.append(f"LC 1A's fire wrap breaks off at {(j, k, z)} ({at(j, k, z) or 'nothing'})")
                continue
            into = [(j + dx, k - 1, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1)]
            if not any(at(*c) in TRAIN_PIECES[:7] and variant(*c) % 3 == 0 for c in into):
                problems.append(f"LC 1A's fire wrap drops at {(j, k, z)} into no Train A tray")
        else:
            problems.append(f"the train {'ABN'[v % 3]} tray run up through the ceiling at {(x, y, z)} goes into {up or 'nothing'}")
    # The cooling: the fan coil unit's supply duct north off it to its elbow, then west along the aisle, its two
    # registers, closed at its end.
    fcus = blocks("sgr_fan_coil")
    if len(fcus) != 1:
        problems.append(f"{len(fcus)} switchgear room fan coil units, not 1")
    for x, y, z, v in fcus:
        if at(x + 1, y, z) != "sgr_fan_coil_end":
            problems.append(f"the fan coil unit at {(x, y, z)} has no coil end east of it")
        k = z - 1
        while at(x, y, k) in ("sgr_duct", "sgr_duct_hanger"):
            k -= 1
        if at(x, y, k) != "hv_duct_elbow":
            problems.append(f"the fan coil unit's duct north from {(x, y, z)} ends at {at(x, y, k) or 'nothing'}, not its elbow")
            continue
        j, registers = x - 1, 0
        while at(j, y, k) in ("sgr_duct", "sgr_duct_hanger", "sgr_duct_register"):
            registers += at(j, y, k) == "sgr_duct_register"
            j -= 1
        if at(j, y, k) != "sgr_duct_end" or registers != 2:
            problems.append(f"the switchgear room's supply duct west from {(x, y, k)}: {registers} registers, ends at {at(j, y, k) or 'nothing'}")
    laptops = blocks("sgr_desk_laptop", part=0)
    if len(laptops) != 1:
        problems.append(f"{len(laptops)} switchgear log desk laptops, not 1")
    lcs = set(cells.values())
    for x, y, z, v in laptops:
        if not any((x - a) ** 2 + (z - c) ** 2 <= 24 * 24 for a, b_, c in lcs):
            problems.append(f"the switchgear room's laptop at {(x, y, z)} has no load centre in reach")
    return problems


def regulating_transformers(site, g):
    """Each regulating transformer stands beside the vital inverter of its own label (each the other's nearest), the
    nearest motor control centre feeds one transformer of each number of one train, and a conduit column runs up from
    each transformer's top, and two from the MCC's REG XF section (its second section), to drops in one ladder tray;
    so does one from every other cabinet in the row (round 26, 8f), each into its room's tray."""
    import plant_panels as PN
    labels = PN.PANELS["reg_transformer"]["labels"]
    kinds = {k: st.split("[")[0].split(":")[-1] for k, (st, _) in P.MATERIALS.items()}

    def at(x, y, z):
        if not (0 <= x < g.shape[0] and 0 <= y + B < g.shape[1] and 0 <= z + O < g.shape[2]):
            return ""
        m = int(g[x, y + B, z + O])
        return kinds.get(m, "") if m else ""

    def panels(kind):
        """Each panel's bottom block: (x, y, z) -> its label index."""
        out = {}
        for k, (st, _) in P.MATERIALS.items():
            if st.startswith(f"jugcraft:{kind}[") and "part=0" in st:
                v = int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
                for x, y, z in np.argwhere(g == k):
                    out[(int(x), int(y) - B, int(z) - O)] = v
        return out

    def nearest(p, cells):
        return min(cells, key=lambda c: (c[0] - p[0]) ** 2 + (c[1] - p[1]) ** 2 + (c[2] - p[2]) ** 2, default=None)

    problems = []
    xfs, invs = panels("reg_transformer"), panels("vital_inverter")
    mccs = []
    for part, px, pz, rot in site.parts:
        for kind, ax, ay, az, facing in part.props:
            if kind == "mcc_lineup":
                rx, rz = P.DIRS[P.CLOCKWISE[facing]]
                mccs.append(((ax + px, ay - part.sink, az + pz), (ax + px + rx, ay - part.sink + P.EQUIPMENT_SIZES[kind][3], az + pz + rz)))
    fed = {}
    tray_drops = {}
    for xf, v in xfs.items():
        inv = nearest(xf, list(invs))
        if inv is None or abs(inv[0] - xf[0]) + abs(inv[2] - xf[2]) != 1 or inv[1] != xf[1] or invs[inv] != v or nearest(inv, list(xfs)) != xf:
            problems.append(f"regulating transformer {labels[v]} at {xf} is not beside the inverter of its label (nearest {inv})")
        mcc = nearest(xf, [m[0] for m in mccs])
        if mcc is None or sum((a - b) ** 2 for a, b in zip(mcc, xf)) > 24 ** 2:
            problems.append(f"regulating transformer {labels[v]} at {xf} has no MCC in reach")
            continue
        fed.setdefault(mcc, []).append(v)
        # Its conduit: up from its top (two blocks up) to a drop in the tray.
        x, y, z = xf
        k = y + 2
        while at(x, k, z) == "feeder_conduit":
            k += 1
        if k == y + 2 or at(x, k, z) not in DROPS:
            problems.append(f"regulating transformer {labels[v]} at {xf}: its conduit does not reach a tray drop ({at(x, k, z)} at y {k})")
        else:
            tray_drops.setdefault(mcc, []).append((x, k, z))
    for mcc, vs in fed.items():
        if sorted(v % 2 for v in vs) != [0, 1] or len({v // 2 for v in vs}) != 1:
            problems.append(f"the MCC at {mcc} feeds transformers {[labels[v] for v in vs]}, not one of each number of one train")
    for anchor, (x, y, z) in mccs:
        if anchor not in fed:
            continue
        if at(x, y, z) != "conduit_pair" or at(x, y + 1, z) not in PAIR_DROPS:
            problems.append(f"the MCC at {anchor}: no REG XF conduits from its second section's top to the tray ({at(x, y, z)}, {at(x, y + 1, z)})")
            continue
        # The tray from the MCC's drops reaches each of its transformers' drops.
        seen, todo = {(x, y + 1, z)}, deque([(x, y + 1, z)])
        while todo:
            cx, cy, cz = todo.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (cx + dx, cy, cz + dz)
                if n not in seen and at(*n).startswith("ladder_tray"):
                    seen.add(n)
                    todo.append(n)
        for d in tray_drops.get(anchor, []):
            if d not in seen:
                problems.append(f"the MCC at {anchor}: its tray does not reach the transformer drop at {d}")
    # The 120 VAC vital panels (round 26, 8e): one for each inverter, of its label; the inverter's output conduit up from
    # its top to a drop in the tray, the panel's up the wall to a wall drop in the same tray.
    vps = panels("vital_panel")
    for vp, v in vps.items():
        same = [i for i, iv in invs.items() if iv == v]
        inv = nearest(vp, same)
        if inv is None or sum((a - b) ** 2 for a, b in zip(inv, vp)) > 24 ** 2:
            problems.append(f"vital panel {labels[v]} at {vp} has no inverter of its label in reach")
            continue
        x, y, z = vp
        k = y + 2
        while at(x, k, z) == "wall_conduit":
            k += 1
        if k == y + 2 or at(x, k, z) != "ladder_tray_drop_wall":
            problems.append(f"vital panel {labels[v]} at {vp}: its conduit does not reach a tray drop ({at(x, k, z)} at y {k})")
            continue
        ix, iy, iz = inv
        j = iy + 2
        while at(ix, j, iz) == "conduit_pair_run":
            j += 1
        if j == iy + 2 or at(ix, j, iz) not in PAIR_DROPS:
            problems.append(f"inverter {labels[v]} at {inv}: its conduits do not reach a pair of tray drops ({at(ix, j, iz)} at y {j})")
            continue
        seen, todo = {(x, k, z)}, deque([(x, k, z)])
        while todo:
            cx, cy, cz = todo.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (cx + dx, cy, cz + dz)
                if n not in seen and at(*n).startswith("ladder_tray"):
                    seen.add(n)
                    todo.append(n)
        if (ix, j, iz) not in seen:
            problems.append(f"vital panel {labels[v]} at {vp}: the tray from its conduit does not reach its inverter's at {(ix, j, iz)}")
    if len(vps) != len(invs):
        problems.append(f"{len(invs)} vital inverters but {len(vps)} vital panels")
    # Every cabinet in the DC rooms' rows (round 26, 8f; Justin: "Chargers, switchboards, inverter DC"): a conduit up
    # from its top (the switchboard's from its second section; an inverter's two) to a drop in the tray of its room's
    # MCC; and each room has its ground detector, one of each train.
    gds = panels("dc_ground_detector")
    if sorted(gds.values()) != [0, 1]:
        problems.append(f"the DC rooms' ground detectors: {sorted(gds.values())} (one 1A, one 1B)")
    boards = {}
    for k, (st, _) in P.MATERIALS.items():
        if st.startswith("jugcraft:dc_switchboard[") and "part=1" in st:
            for x, y, z in np.argwhere(g == k):
                boards[(int(x), int(y) - B, int(z) - O)] = 0
    cabinets = ([(p, "station charger", "feeder_conduit", DROPS) for p in panels("station_charger")]
                + [(p, "ground detector", "feeder_conduit", DROPS) for p in gds]
                + [(p, "DC switchboard", "feeder_conduit", DROPS) for p in boards]
                + [(p, "vital inverter", "conduit_pair_run", PAIR_DROPS) for p in invs])
    components = {}
    for anchor, (x, y, z) in mccs:
        seen, todo = {(x, y + 1, z)}, deque([(x, y + 1, z)])
        while todo:
            cx, cy, cz = todo.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (cx + dx, cy, cz + dz)
                if n not in seen and at(*n).startswith("ladder_tray"):
                    seen.add(n)
                    todo.append(n)
        components[anchor] = seen
    for (x, y, z), name, conduit, drops in cabinets:
        k = y + 2
        while at(x, k, z) == conduit:
            k += 1
        if k == y + 2 or at(x, k, z) not in drops:
            problems.append(f"{name} at {(x, y, z)}: its conduit does not reach a tray drop ({at(x, k, z)} at y {k})")
            continue
        mcc = nearest((x, y, z), list(components))
        if mcc is None or (x, k, z) not in components[mcc]:
            problems.append(f"{name} at {(x, y, z)}: its drop is not in its room's tray (the MCC at {mcc})")
    return problems


def dc_rooms(site, g):
    """The DC equipment rooms (older rooms round 3; nuclear/DcRoom.java): each room's two 125 VDC distribution panels (one
    of each label: 1A-1, 1A-2 in Room 1A, 1B-1, 1B-2 in 1B) on the side wall, each with its train's switchboard in reach and
    its conduit up the wall to a tray drop; each room's log desk laptop with a switchboard in reach; the one-line
    diagram, the battery service board, the test cart, the train sign and the DANGER sign in each; no relay cabinet
    left in them."""
    import dc_room as DC
    kinds = {k: st.split("[")[0].split(":")[-1] for k, (st, _) in P.MATERIALS.items()}

    def at(x, y, z):
        if not (0 <= x < g.shape[0] and 0 <= y + B < g.shape[1] and 0 <= z + O < g.shape[2]):
            return ""
        m = int(g[x, y + B, z + O])
        return kinds.get(m, "") if m else ""

    def placed(kind, part=None):
        out = []
        for k, (st, _) in P.MATERIALS.items():
            if st.startswith(f"jugcraft:{kind}[") or st == f"jugcraft:{kind}":
                if part is not None and f"part={part}" not in st:
                    continue
                v = int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
                out += [(int(x), int(y) - B, int(z) - O, v) for x, y, z in np.argwhere(g == k)]
        return out

    problems = []
    boards = placed("dc_switchboard", part=1)
    if sorted(v for *_, v in boards) != [0, 1]:
        problems.append(f"the DC switchboards: {sorted(v for *_, v in boards)} (one 1A, one 1B)")
    panels = placed(DC.DP_KEY, part=0)
    if sorted(v for *_, v in panels) != [0, 1, 2, 3]:
        problems.append(f"the DC distribution panels' labels: {sorted(v for *_, v in panels)} (1A-1, 1A-2, 1B-1, 1B-2)")
    for x, y, z, v in panels:
        mine = [b for b in boards if b[3] == v // 2 and (b[0] - x) ** 2 + (b[2] - z) ** 2 <= 24 * 24]
        if not mine:
            problems.append(f"DC panel {DC.DP_LABELS[v]} at {(x, y, z)} has no switchboard of its train in reach")
        k = y + 2
        while at(x, k, z) == "wall_conduit":
            k += 1
        if k == y + 2 or at(x, k, z) != "ladder_tray_drop_wall":
            problems.append(f"DC panel {DC.DP_LABELS[v]} at {(x, y, z)}: its conduit does not reach a tray drop ({at(x, k, z)} at y {k})")
    laptops = placed(DC.LT_KEY, part=0)
    if len(laptops) != 2:
        problems.append(f"{len(laptops)} DC room log desk laptops, not 2")
    for x, y, z, v in laptops:
        if not any((b[0] - x) ** 2 + (b[2] - z) ** 2 <= 24 * 24 for b in boards):
            problems.append(f"the DC room laptop at {(x, y, z)} has no switchboard in reach")
    for kind, part, n in (("dc_one_line", 0, 2), ("battery_service_board", 0, 2), ("battery_test_cart", None, 2), ("sign_dc_train_a", None, 1),
                          ("sign_dc_train_b", None, 1), ("sign_dc_danger", None, 2)):
        got = len(placed(kind, part))
        if got != n:
            problems.append(f"{got} {kind} in the DC rooms, not {n}")
    for x, y, z, v in boards:
        cab = [c for c in placed("relay_cabinet") if (c[0] - x) ** 2 + (c[2] - z) ** 2 <= 10 * 10 and abs(c[1] - y) < 3]
        if cab:
            problems.append(f"a relay cabinet still in the DC room by the switchboard at {(x, y, z)}: {cab[:2]}")
    return problems


def control_room(site, g):
    """The main control room's furniture (older rooms round 5; tools/cr_room.py): the three operators' consoles, one of
    each position; the shift manager's desk; the eight procedure racks, one of each section; the SCBA cabinet, the EAL
    chart, the At The Controls signs at the ends of its line across the floor."""
    import cr_room as CR
    problems = []

    def count(name, part=0):
        out = {}
        for k, (st, _) in P.MATERIALS.items():
            if st.startswith(f"jugcraft:{name}[") and f"part={part}" in st:
                v = int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
                out[v] = out.get(v, 0) + int((g == k).sum())
        return out
    # (The remote shutdown room has an AOP rack and an SCBA cabinet of its own: older rooms round 7.)
    aop = [CR.PR_LABELS.index("AOP")]
    for name, n, more in ((CR.CC_KEY, len(CR.POSITIONS), []), (CR.PR_KEY, len(CR.PR_LABELS), aop), (CR.SS_KEY, 1, [])):
        c = count(name)
        if sorted(v for v, k in c.items() for _ in range(k)) != sorted(list(range(n)) + more):
            problems.append(f"{name}: {c}, not one of each of its {n} labels")
    for name, want in (("cr_scba_cabinet", 2), ("cr_eal_chart", 1)):
        if sum(count(name).values()) != want:
            problems.append(f"{sum(count(name).values())} {name}, not {want}")
    for name, lo in (("cr_atc_sign", 2), ("cr_atc_line", 20)):
        n = sum(int((g == k).sum()) for k, (st, _) in P.MATERIALS.items() if st.startswith(f"jugcraft:{name}["))
        if n < lo or (name == "cr_atc_sign" and n != 2):
            problems.append(f"{n} {name}")
    return problems


def entrance_hall(site, g):
    """The control building's entrance (older rooms round 6; tools/entrance_hall.py): the security post, the hand
    reader, the evacuation plan, the VITAL AREA and prohibited items signs, one of each (the post, the hand reader and
    the prohibited items sign twice: the security gatehouse has its own)."""
    problems = []
    for name, want in (("ac_station", 2), ("ac_hand_reader", 2), ("ac_evac_plan", 1), ("sign_vital_area", 1), ("sign_prohibited", 2)):
        n = sum(int((g == k).sum()) for k, (st, _) in P.MATERIALS.items()
                if st.startswith(f"jugcraft:{name}[") and ("part=" not in st or "part=0" in st))
        if n != want:
            problems.append(f"{n} {name}, not {want}")
    return problems


def gatehouse(site, g):
    """The security gatehouse (tools/sec_gatehouse.py): the alarm station console, the X-ray, the metal detector, the
    weapons locker and the PROTECTED AREA sign, one of each; two dome cameras."""
    problems = []
    for name, want in (("sec_alarm_console", 1), ("sec_xray", 1), ("sec_metal_detector", 1), ("sec_weapons_locker", 1),
                       ("sign_protected_area", 1), ("sec_dome_camera", 2)):
        n = sum(int((g == k).sum()) for k, (st, _) in P.MATERIALS.items()
                if st.startswith(f"jugcraft:{name}[") and ("part=" not in st or "part=0" in st))
        if n != want:
            problems.append(f"{n} {name}, not {want}")
    return problems


def remote_shutdown_room(site, g):
    """The remote shutdown room (older rooms round 7; tools/rsr_room.py): the station, the transfer key cabinet, the
    communications station and the room's sign, one of each."""
    problems = []
    for name in ("rsr_desk", "rsr_key_cabinet", "rsr_phone_station", "sign_rsr_area"):
        n = sum(int((g == k).sum()) for k, (st, _) in P.MATERIALS.items()
                if st.startswith(f"jugcraft:{name}[") and ("part=" not in st or "part=0" in st))
        if n != 1:
            problems.append(f"{n} {name}, not 1")
    return problems


def battery_rooms(site, g):
    """The battery rooms (round 26, #9): two strings of two racks in each, every rack's terminal end with its riser (and
    the sense cable) up to a riser top under the ceiling; each room's exhaust fan over a louvre, its hydrogen monitor's
    conduit up to the sensor head, its eyewash, spill kit and signs."""
    kinds = {k: st.split("[")[0].split(":")[-1] for k, (st, _) in P.MATERIALS.items()}

    def at(x, y, z):
        if not (0 <= x < g.shape[0] and 0 <= y + B < g.shape[1] and 0 <= z + O < g.shape[2]):
            return ""
        m = int(g[x, y + B, z + O])
        return kinds.get(m, "") if m else ""

    problems = []
    ends = []
    for k, (st, _) in P.MATERIALS.items():
        if st.startswith("jugcraft:station_battery[") and "part=3" in st:
            ends += [(int(x), int(y) - B, int(z) - O) for x, y, z in np.argwhere(g == k)]
    if len(ends) != 8:
        problems.append(f"{len(ends)} station battery racks, not 8 (two strings of two in each battery room)")
    for x, y, z in ends:
        column = [at(x, y + k, z) for k in (2, 3, 4)]
        if column != ["dc_cable_riser_sense", "dc_cable_riser_sense", "dc_cable_riser_top_sense"]:
            problems.append(f"station battery rack ending at {(x, y, z)}: no riser with the sense cable over its terminal end ({column})")
    fans = []
    for k, (st, _) in P.MATERIALS.items():
        if st.startswith("jugcraft:exhaust_fan[") and "part=1" in st:
            fans += [(int(x), int(y) - B, int(z) - O, st) for x, y, z in np.argwhere(g == k)]
    for x, y, z, st in fans:
        facing = st.split("facing=")[1].split(",")[0].rstrip("]")
        dx, dz = P.DIRS[facing]
        if at(x + dx, y, z + dz) != "intake_louvre":
            problems.append(f"exhaust fan at {(x, y, z)} is not over a louvre ({at(x + dx, y, z + dz)})")
    # (The diesel rooms have eyewashes of their own: round 31 part 3. Not counted here.)
    diesel = [(px, pz, part.size[0], part.size[2]) for part, px, pz, _ in site.parts if getattr(part, "key", "") == "diesel_building"]

    tb = turbine_building(site)

    def counted(c):
        # (Nor the turbine building's: its workshop has an eyewash, its lube oil room a NO SMOKING sign; part 3.)
        return c[1] - B < 10 and not any(px <= c[0] < px + w and pz <= c[2] - O < pz + d for px, pz, w, d in diesel) \
            and not (tb[0] <= c[0] <= tb[2] and tb[1] <= c[2] <= tb[3])
    for name in ("h2_monitor", "eyewash_station", "spill_kit", "sign_no_smoking", "sign_battery_acid", "sign_hydrogen", "sign_eyewash"):
        # On the ground floor (the restroom upstairs has a no smoking sign of its own).
        n = sum(sum(1 for c in np.argwhere(g == k).tolist() if counted(c)) for k, (st, _) in P.MATERIALS.items() if kinds[k] == name and "part=1" not in st)
        if n != 2:
            problems.append(f"{n} {name} in the battery rooms, not one in each")
    if len(fans) != 2:
        problems.append(f"{len(fans)} battery room exhaust fans, not 2")
    for k, (st, _) in P.MATERIALS.items():
        if st.startswith("jugcraft:h2_monitor[") and "part=1" in st:
            for x, y, z in np.argwhere(g == k):
                x, y, z = int(x), int(y) - B, int(z) - O
                k2 = y + 1
                while at(x, k2, z) == "wall_conduit":
                    k2 += 1
                if at(x, k2, z) != "h2_sensor":
                    problems.append(f"hydrogen monitor at {(x, y, z)}: its conduit does not reach a sensor head ({at(x, k2, z)} at y {k2})")
    # Older rooms round 4 (tools/battery_rooms.py): a disconnect for each string of each battery (1A-1 .. 1B-2), its
    # conduit pair up from its top into a DC tray; the racks labelled, each label once (a train's two strings of two
    # racks); the ground floor's battery rooms lit by vapor-tight lights; the PPE cabinet, cell log, sump and sign in each.
    discs = {}
    for k, (st, _) in P.MATERIALS.items():
        if st.startswith("jugcraft:battery_disconnect[") and "part=0" in st:
            v = int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
            for x, y, z in np.argwhere(g == k):
                discs[(int(x), int(y) - B, int(z) - O)] = v
    if sorted(discs.values()) != [0, 1, 2, 3]:
        problems.append(f"the battery disconnects' labels: {sorted(discs.values())} (1A-1, 1A-2, 1B-1, 1B-2)")
    for (x, y, z), v in discs.items():
        column = [at(x, y + 2, z), at(x, y + 3, z), at(x, y + 4, z)]
        if column[:2] != ["battery_disconnect_riser"] * 2 or column[2] not in ("dc_cable_tray", "dc_cable_tray_tee", "dc_cable_tray_bend"):
            problems.append(f"battery disconnect at {(x, y, z)}: no conduit pair up into a DC tray over it ({column})")
    labels = []
    for k, (st, _) in P.MATERIALS.items():
        if st.startswith("jugcraft:station_battery[") and ("part=0," in st or "part=0]" in st):
            v = int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
            labels += [v] * int((g == k).sum())
    if sorted(labels) != list(range(8)):
        problems.append(f"the station battery racks' labels: {sorted(labels)} (each of the eight once)")
    for name, n in (("battery_ppe_cabinet", 2), ("battery_log_board", 2), ("battery_room_sump", 2), ("sign_battery_1a", 1), ("sign_battery_1b", 1)):
        got = sum(int((g == k).sum()) for k, (st, _) in P.MATERIALS.items() if kinds[k] == name)
        if got != n:
            problems.append(f"{got} {name} in the battery rooms, not {n}")
    lights = sum(int((g == k).sum()) for k, (st, _) in P.MATERIALS.items() if kinds[k] == "vapor_tight_light")
    if lights < 6:
        problems.append(f"{lights} vapor-tight lights in the battery rooms")
    return problems


RELAY_KINDS = ("protection_rack", "ssps_cabinet", "nis_cabinet", "relay_panel")
RELAY_DROP_TRAYS = ("control_tray_waterfall", "control_tray_waterfall_hanger")


def relay_room(site, g):
    """The relay room (round 26, #10): its cabinets by kind (12 protection racks, 6 SSPS bays, 5 NIS racks, 29 relay
    panels, every relay panel's name once), each with its control cable bundle from a drop in the tray over it down
    into its top; the header from a tee in the cable spreading room's top tray, through both corridor walls in fire
    stops, to the rows' crosses."""
    kinds = {k: st.split("[")[0].split(":")[-1] for k, (st, _) in P.MATERIALS.items()}

    def at(x, y, z):
        if not (0 <= x < g.shape[0] and 0 <= y + B < g.shape[1] and 0 <= z + O < g.shape[2]):
            return ""
        m = int(g[x, y + B, z + O])
        return kinds.get(m, "") if m else ""

    problems = []
    found = {k: [] for k in RELAY_KINDS}
    for k, (st, _) in P.MATERIALS.items():
        name = kinds[k]
        if name in RELAY_KINDS and "part=1" in st:
            v = int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
            found[name] += [(int(x), int(y) - B, int(z) - O, v) for x, y, z in np.argwhere(g == k)]
    for name, n in zip(RELAY_KINDS, (12, 6, 5, 29)):
        if len(found[name]) != n:
            problems.append(f"{len(found[name])} {name} in the relay room, not {n}")
        if len({v for *_, v in found[name]}) != len(found[name]):
            problems.append(f"the relay room's {name}s don't each have their own label")
    for name in RELAY_KINDS:
        for x, y, z, v in found[name]:
            column = [at(x, y + k, z) for k in (1, 2, 3)]
            if column[0] != "control_cable_entry" or column[1] != "control_cable_drop" or column[2] not in RELAY_DROP_TRAYS:
                problems.append(f"{name} at {(x, y, z)}: no cable bundle down from a tray drop into its top ({column})")
    header = [(x, y, z) for k, (st, _) in P.MATERIALS.items() if kinds[k] == "control_tray_firestop" for x, y, z in np.argwhere(g == k)]
    if len(header) != 2:
        problems.append(f"{len(header)} fire-stopped tray penetrations, not 2 (the corridor's walls)")
    tees = sum(int((g == k).sum()) for k in P.MATERIALS if kinds[k] in ("control_tray_tee", "cable_tray_control_tee"))
    if tees != 2:
        problems.append(f"{tees} control tray tees, not 2 (out of the spreading room's tray, into row D's run)")
    # The room's fittings, counted in the room (the cabinets' extent, out to its walls).
    cells = [c for name in RELAY_KINDS for c in found[name]]
    x0, x1 = min(c[0] for c in cells) - 3, max(c[0] for c in cells) + 3
    z0, z1 = min(c[2] for c in cells) - 3, max(c[2] for c in cells) + 3
    y0, y1 = min(c[1] for c in cells) - 1, max(c[1] for c in cells) + 6
    room = g[x0:x1 + 1, y0 + B:y1 + B + 1, z0 + O:z1 + O + 1]
    for name, n in (("sign_relay_row_a", 1), ("sign_relay_row_b", 1), ("sign_relay_row_c", 1), ("sign_relay_row_d", 1), ("soe_printer", 1),
                    ("drawing_rack", 1), ("smoke_detector", 6)):
        m = sum(int((room == k).sum()) for k in P.MATERIALS if kinds[k] == name)
        if m != n:
            problems.append(f"{m} {name} in the relay room, not {n}")
    return problems


def hvac_room(site, g):
    """The Control Room HVAC Equipment Room (round 26, #11): its HVAC control panel, six dampers (each tag once, open as
    the panel has them in normal: only CR-D-1A), each intake's radiation monitor and toxic gas analyser (A and B), the
    refrigerant monitor, the room's sign; every duct and chilled water piece joined at each opening to what it faces."""
    kinds = {k: st.split("[")[0].split(":")[-1] for k, (st, _) in P.MATERIALS.items()}
    problems = []

    def blocks(name, part=None):
        out = []
        for k, (st, _) in P.MATERIALS.items():
            if kinds[k] == name and (part is None or f"part={part}" in st):
                v = int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
                out += [(int(x), int(y) - B, int(z) - O, v) for x, y, z in np.argwhere(g == k)]
        return out
    panels = blocks("hv_control_panel", 1)
    if len(panels) != 1:
        problems.append(f"{len(panels)} HVAC control panels, not 1")
    dampers = blocks("hv_damper")
    if len(dampers) != 6 or len({v // 2 for *_, v in dampers}) != 6:
        problems.append(f"{len(dampers)} HVAC dampers with tags {sorted(v // 2 for *_, v in dampers)}, not CR-D-1A to 3B once each")
    if sorted(v for *_, v in dampers) != [1, 2, 4, 6, 8, 10]:
        problems.append(f"the dampers aren't as the panel sets them in normal (only CR-D-1A open): {sorted(v for *_, v in dampers)}")
    for name, want in (("hv_rad_monitor", [0, 1]), ("hv_tox_monitor", [0, 1]), ("hv_refrig_monitor", [0])):
        got = sorted(v for *_, v in blocks(name, 0))
        if got != want:
            problems.append(f"{name}: labels {got}, not {want}")
    signs = sum(int((g == k).sum()) for k, (st, _) in P.MATERIALS.items() if "label=cr_hvac_room" in st)
    if signs != 1:
        problems.append(f"{signs} Control Room HVAC Equipment Room signs, not 1")
    # Every opening of a duct or chilled water piece meets a piece of its own kind opening back, a machine, or (a duct)
    # the intake plenum; a small pipe its tank, tap or feed pot.
    import hvac_room as H
    machines = set()
    for part, px, pz, rot in site.parts:
        if isinstance(part, MP.Building):
            for kind, ax, ay, az, facing in part.props:
                if kind in ("cr_filtration_unit", "cr_air_handling_unit", "cr_chiller", "chw_pump"):
                    machines |= {(x + px, y + part.base - part.sink, z + pz) for x, y, z in P.equipment_cells(kind, ax, ay, az, facing)}
    family = {}
    for k, (st, _) in P.MATERIALS.items():
        name = kinds[k]
        if name in H.PIECES and "conn" in H.PIECES[name]:
            facing = st.split("facing=")[1].split(",")[0].rstrip("]")
            fam = "pipe" if name.startswith("hv_chw") else "small" if name == "hv_small_pipe" else "duct"
            for x, y, z in np.argwhere(g == k):
                family[(int(x), int(y) - B, int(z) - O)] = (name, fam, H.turned(H.PIECES[name]["conn"], facing))
    opposite = {"n": "s", "s": "n", "e": "w", "w": "e", "u": "d", "d": "u"}
    small_ends = ("hv_chw_pipe_tap", "hv_expansion_tank", "hv_expansion_tank_top", "hv_chem_feeder", "hv_small_pipe")

    def name_at(c):
        x, y, z = c
        if not (0 <= x < g.shape[0] and 0 <= y + B < g.shape[1] and 0 <= z + O < g.shape[2]):
            return ""
        m = int(g[x, y + B, z + O])
        return kinds.get(m, "") if m else ""
    for c, (name, fam, opens) in family.items():
        for f in opens:
            d = H.DIRS[f]
            n = (c[0] + d[0], c[1] + d[1], c[2] + d[2])
            other = family.get(n)
            if other and other[1] == fam and opposite[f] in other[2]:
                continue
            if fam != "small" and n in machines:
                continue
            if fam == "duct" and name_at(n).startswith("hv_plenum"):
                continue
            if fam == "duct" and name_at(n).startswith("sgr_duct"):
                continue        # (the switchgear room's supply duct off its elbow: switchgear_room checks its run)
            if fam == "small" and name_at(n) in small_ends:
                continue
            problems.append(f"{name} at {c}: its {f} opening meets {name_at(n) or 'air'}")
    return problems


TRAIN_PIECES = ("cable_tray_power", "cable_tray_control", "cable_tray_instr", "cable_tray_power_drop", "cable_tray_control_drop",
                "cable_tray_instr_drop", "cable_tray_power_up", "cable_bundle", "cable_bundle_elbow", "cable_bundle_sleeve")


def overview_screens(g, site=None):
    """The plant overview screens (round 34): three of them (the control room's east wall, the board room, the plant
    manager's office) and the turbine control room's (turbine hall part 3), each with its power raceway on the wall
    under its middle (tools/medium_plant.py screen_raceway)."""
    kinds = {k: st.split("[")[0].split(":")[-1] for k, (st, _) in P.MATERIALS.items()}
    middles = [tuple(int(v) for v in c) for k, (st, _) in P.MATERIALS.items() if kinds[k] == "plant_map_screen" and "part=1" in st
               for c in np.argwhere(g == k)]
    problems = []
    want = 4 if site is not None and turbine_building(site)[2] >= 0 else 3
    if len(middles) != want:
        problems.append(f"{len(middles)} plant overview screens, not {want}")
    for x, y, z in middles:
        below = kinds.get(int(g[x, y - 1, z]), "") if g[x, y - 1, z] else ""
        # (The board room's overview stands over the AV credenza, which feeds it: the administration building part 2.)
        if below not in ("screen_raceway", "board_av_credenza"):
            problems.append(f"plant overview screen at {(x, y, z)}: no power raceway under its middle ({below or 'air'})")
    return problems


def spreading_room(site, g, occupied):
    """The cable spreading room (punch list #15; tools/spreading_room.py): one releasing panel, its valve station and a
    pull station; detectors and heads, all within the panel's reach (nuclear/SpreadingRoomFire.java REACH, HIGH); no
    two trays of different trains touching along a run; every bundle's sleeve under a board (the slab between), its
    bundle down to an elbow out of a tray or a tray it rises out of (the operator consoles' too)."""
    import spreading_room as SR
    kinds = {k: st.split("[")[0].split(":")[-1] for k, (st, _) in P.MATERIALS.items()}
    problems = []
    # The warehouse (part W3) has its own sprinkler system, valve station and releasing panel in its office: not the
    # spreading room's.
    own = [(px, pz + O, px + q.size[0] - 1, pz + O + q.size[2] - 1) for q, px, pz, _ in site.parts if q.key == "warehouse"]
    # (Nor is the turbine building's lube oil room's, turbine hall part 3.)
    own.append(turbine_building(site))

    def blocks(*names, part=None):
        out = []
        for k, (st, _) in P.MATERIALS.items():
            if kinds[k] in names and (part is None or f"part={part}" in st):
                v = int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
                out += [(int(x), int(y), int(z), v) for x, y, z in np.argwhere(g == k)
                        if not any(a <= x <= c and b <= z <= e for a, b, c, e in own)]
        return out

    def kind_at(x, y, z):
        return kinds.get(int(g[x, y, z]), "") if 0 <= x < g.shape[0] and 0 <= y < g.shape[1] and 0 <= z < g.shape[2] and g[x, y, z] else ""
    panels = blocks("csr_release_panel", part=0)
    for name, n in (("csr_release_panel", 1), ("csr_valve_station", 1), ("fire_pull_station", 1)):
        got = len(blocks(name, part=0 if name != "fire_pull_station" else None))
        if got != n:
            problems.append(f"{got} {name}, not {n}")
    if len(panels) != 1:
        return problems
    px, py, pz, _ = panels[0]
    heads = blocks("sprinkler_pipe_head", "sprinkler_pipe_end")
    detectors = blocks("smoke_detector", "heat_detector")
    near = lambda c: abs(c[0] - px) <= 24 and abs(c[2] - pz) <= 24 and abs(c[1] - py) <= 10
    if sum(1 for h in heads if near(h)) < 20:
        problems.append(f"only {sum(1 for h in heads if near(h))} sprinkler heads within the releasing panel's reach")
    if sum(1 for d in detectors if near(d)) < 9:
        problems.append(f"only {sum(1 for d in detectors if near(d))} detectors within the releasing panel's reach")
    if not any(near(c) for c in blocks("csr_valve_station", part=0)):
        problems.append("the valve station is out of the releasing panel's reach")
    # The trains kept apart: no two train pieces of different trains side by side.
    trays = {(x, y, z): v % 3 for x, y, z, v in blocks(*TRAIN_PIECES[:7])}
    for (x, y, z), t in trays.items():
        for dx, dz in ((1, 0), (0, 1)):
            o = trays.get((x + dx, y, z + dz))
            if o is not None and o != t:
                problems.append(f"trays of two trains touch at {(x, y - B, z - O)}")
    # The bundles: up from a tray to a sleeve under a board.
    sleeves = blocks("cable_bundle_sleeve") + blocks("fire_wrap_sleeve")
    if len(sleeves) < 40:
        problems.append(f"only {len(sleeves)} bundle sleeves up into the boards")
    for x, y, z, v in sleeves:
        if not occupied[x, y + 2, z] and kind_at(x, y + 2, z) not in ("operator_console", "cr_console"):
            problems.append(f"a sleeve at {(x, y - B, z - O)} goes up into no board or console")
        k = y - 1
        while kind_at(x, k, z) in ("cable_bundle", "fire_wrap_rise"):
            k -= 1
        if kind_at(x, k, z) not in ("cable_bundle_elbow", "cable_tray_power_up", "fire_wrap_tee_up", "fire_wrap_up"):
            problems.append(f"the bundle up to the sleeve at {(x, y - B, z - O)} starts at {kind_at(x, k, z) or 'nothing'}")
    return problems


def tsc_room(site, g):
    """The Technical Support Center (round 26, #12): its classification board, the video wall's three displays, the
    seven consoles (each pair of positions once), the communications console, the NRC desk, the four status boards,
    the area radiation monitor and the frisker, each within reach of the board (nuclear/TscRoom.REACH); its sign and the
    NRC room's."""
    kinds = {k: st.split("[")[0].split(":")[-1] for k, (st, _) in P.MATERIALS.items()}
    problems = []

    def blocks(name, part=0):
        out = []
        for k, (st, _) in P.MATERIALS.items():
            if kinds[k] == name and (f"part={part}," in st or f"part={part}]" in st):
                v = int(st.split("variant=")[1].split(",")[0].rstrip("]")) if "variant=" in st else 0
                out += [(int(x), int(y) - B, int(z) - O, v) for x, y, z in np.argwhere(g == k)]
        return out
    hubs = blocks("tsc_class_board")
    if len(hubs) != 1:
        problems.append(f"{len(hubs)} TSC classification boards, not 1")
        return problems
    hx, hy, hz, _ = hubs[0]
    for name, want in (("tsc_video_wall", [0, 1, 2]), ("tsc_console", list(range(7))), ("tsc_comms_console", [0]), ("tsc_nrc_desk", [0]),
                       ("tsc_status_board", [0, 1, 2, 3]), ("tsc_area_monitor", [0]), ("tsc_frisker", [0])):
        got = blocks(name)
        if sorted(v for *_, v in got) != want:
            problems.append(f"{name}: labels {sorted(v for *_, v in got)}, not {want}")
        for x, y, z, v in got:
            if (x - hx) ** 2 + (y - hy) ** 2 + (z - hz) ** 2 > 24 ** 2:
                problems.append(f"{name} {v} at {(x, y, z)} is out of its board's reach")
    for label, what in (("support_center", "Technical Support Center"), ("nrc_room", "NRC Site Team")):
        signs = sum(int((g == k).sum()) for k, (st, _) in P.MATERIALS.items() if f"label={label}" in st)
        if signs < 1:
            problems.append(f"no {what} sign")
    return problems


DROPS = ("ladder_tray_drop", "ladder_tray_drop_hanger", "ladder_tray_bend_drop")
PAIR_DROPS = ("ladder_tray_drop_pair", "ladder_tray_drop_pair_hanger")


def lineup_role(lx, ly, lz):
    """What a cable touching this cell of a load centre finds (nuclear/Switchgear.java storageAt): its incoming side
    (the back of its transformer section), a feeder's terminal (1..10), or nothing."""
    if lz == 1 and lx <= 1:
        return "incoming"
    for f in range(1, 11):
        if (lx, ly, lz) == (3 + (f - 1) // 2, 2 if (f - 1) % 2 == 0 else 0, 1):
            return f"feeder{f}"
    return None


def mcc_role(lx, ly, lz):
    """What a cable touching this cell of a motor control centre finds (nuclear/MotorControlCenter.java storageAt): its
    incoming side (the first section's bottom cell), a feeder's terminal, or nothing."""
    if (lx, ly, lz) == (0, 0, 0):
        return "incoming"
    for f, cell in MCC_TERMINALS.items():
        if (lx, ly, lz) == cell:
            return f"feeder{f}"
    return None


# The blocks that charge a station battery (nuclear/LivePanelBlockEntity.java isCharger).
CHARGERS = ("edg_battery_charger", "station_charger")
# nuclear/MotorControlCenter.java TERMINALS: each feeder's terminal cell (section, height, 0).
MCC_TERMINALS = {1: (1, 2, 0), 2: (1, 1, 0), 4: (2, 0, 0), 5: (2, 2, 0), 6: (3, 0, 0), 7: (3, 2, 0)}


def cable_networks(site, g, quiet=True):
    """The plant's power cables as the game joins them: every network found, with what it touches that holds or moves
    power. A network may join the main transformer to load centres' incoming sides, a load centre's feeder to a motor
    control centre's incoming side, or a board's feeder to a charger; never a board's feeder to its own incoming side
    (it would feed itself), the transformer to a feeder, or two feeders together. The control building must have the
    transformer's feed to both its load centres, each load centre's DC MCC breaker's run to its train's motor control
    centre, and each motor control centre's CHGR 1 and CHGR 2 breakers' runs to one charger each (round 26)."""
    cable = table(lambda st: st.split("[")[0].endswith("_cable") and "data_cable" not in st)
    cells = set(map(tuple, np.argwhere(cable[g])))
    if not cells:
        return []
    # Equipment cells by where they are: (kind, anchor, local cell).
    where = {}

    def add(kind, ax, ay, az, facing, px=0, py=0, pz=0):
        _, w, d, h = P.EQUIPMENT_SIZES[kind]
        fx, fz = P.DIRS[facing]
        rx, rz = P.DIRS[P.CLOCKWISE[facing]]
        for ly in range(h):
            for lz in range(d):
                for lx in range(w):
                    if P.equipment_solid(kind, lx, ly, lz) or (lx, ly, lz) == (0, 0, 0):
                        at = (ax + rx * lx + fx * lz + px, ay + ly + py + B, az + rz * lx + fz * lz + pz + O)
                        where[at] = (kind, (ax + px, ay + py, az + pz), (lx, ly, lz))
    for kind, ax, ay, az, facing in site.equipment:
        add(kind, ax, ay, az, facing)
    for part, px, pz, rot in site.parts:
        for kind, ax, ay, az, facing in part.props:
            add(kind, ax, ay, az, facing, px, -part.sink, pz)
    problems, seen, nets = [], set(), []
    # The transformer yard built as a site (transformer yard and switchyard build): its unit auxiliary transformers'
    # feed into the cable under UAT-1A's bus duct (tools/yard_sites.py) is the transformer's way out.
    yard_feeds = set()
    if any(part.key == "pwr_transformer_yard" and getattr(part, "kind", "") == "yard" for part, *_ in site.parts):
        import yard_sites
        hx, hy, hz = yard_sites._cached_meta("transformer_yard")["house_feed"]
        yard_feeds.add((hx, hy + B, hz + O))
    for start in sorted(cells):
        if start in seen:
            continue
        net, touches, q = [], set(), deque([start])
        seen.add(start)
        while q:
            c = q.popleft()
            net.append(c)
            if c in yard_feeds:
                touches.add(("transformer_yard", (c[0], c[1] - B, c[2] - O), "out"))
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (c[0] + dx, c[1] + dy, c[2] + dz)
                if n in cells:
                    if n not in seen:
                        seen.add(n)
                        q.append(n)
                    continue
                if n in where:
                    kind, anchor, local = where[n]
                    role = None
                    if kind == "main_transformer" and local[1] == 0:
                        role = "out"
                    elif kind == "switchgear_lineup":
                        role = lineup_role(*local)
                    elif kind == "mcc_lineup":
                        role = mcc_role(*local)
                    if role:
                        touches.add((kind, anchor, role))
                    continue
                if 0 <= n[0] < g.shape[0] and 0 <= n[1] < g.shape[1] and 0 <= n[2] < g.shape[2] and g[n]:
                    st = P.MATERIALS[int(g[n])][0].split("[")[0]
                    if st in ("jugcraft:edg_battery_charger", "jugcraft:station_charger", "jugcraft:station_battery"):
                        touches.add((st.split(":")[1], (n[0], n[1] - B, n[2] - O), "block"))
        nets.append((net, touches))
        feeders = {t for t in touches if t[2].startswith("feeder")}
        incomings = {t for t in touches if t[2] == "incoming"}
        cell = (int(net[0][0]), int(net[0][1]) - B, int(net[0][2]) - O)
        if any(f[1] == i[1] for f in feeders for i in incomings):
            problems.append(f"power cable at {cell}: a board's feeder and its own incoming side on one network (it would feed itself): {sorted(touches)}")
        if feeders and any(t[2] == "out" for t in touches):
            problems.append(f"power cable at {cell}: the transformer and a feeder on one network: {sorted(touches)}")
        if len(feeders) > 1:
            problems.append(f"power cable at {cell}: two feeders on one network: {sorted(feeders)}")
        if any(t[0] == "station_battery" for t in touches):
            problems.append(f"power cable at {cell}: touches a station battery (its chargers charge it): {sorted(touches)}")
        if not quiet:
            print(f"  power network of {len(net)} cables from {cell}: {sorted((t[0], t[2]) for t in touches)}")
    # What the control building must have.
    feed = [t for net, t in nets if any(x[2] == "out" for x in t)]
    if not feed or sum(1 for x in feed[0] if x[2] == "incoming") < 2:
        problems.append("the main transformer (or the transformer yard) does not feed both of the control building's load centres")
    if any(x[0] != "switchgear_lineup" for x in feed[0] if x[2] == "incoming") if feed else False:
        problems.append("the main transformer's feed reaches something besides the load centres' incoming sides")
    to_mcc = [t for net, t in nets if any(x[0] == "switchgear_lineup" and x[2] == "feeder6" for x in t)]
    if len(to_mcc) != 2 or any(sum(1 for x in t if x[0] == "mcc_lineup" and x[2] == "incoming") != 1 for t in to_mcc):
        problems.append(f"each load centre's DC MCC breaker must reach its train's motor control centre: {[sorted(t) for t in to_mcc]}")
    fed = []
    for f in (4, 6):
        runs = [t for net, t in nets if any(x[0] == "mcc_lineup" and x[2] == f"feeder{f}" for x in t)]
        if len(runs) != 2 or any(sum(1 for x in t if x[0] in CHARGERS) != 1 for t in runs):
            problems.append(f"each motor control centre's CHGR {f // 2 - 1} breaker must reach one charger: {[sorted(t) for t in runs]}")
        fed += [x[1] for t in runs for x in t if x[0] in CHARGERS]
    if len(set(fed)) != 4:
        problems.append(f"the four chargers must each have their own breaker: {sorted(fed)}")
    return problems


if __name__ == "__main__":
    site = MP.medium_site()
    found = check(site, quiet="--quiet" in sys.argv)
    print(f"{len(found)} problem(s)")
    sys.exit(1 if found else 0)
