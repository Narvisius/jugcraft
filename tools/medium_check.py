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
WALL_HUNG = {"whiteboard", "framed_print", "large_print", "wall_display", "coat_rack", "aed_cabinet", "emergency_light", "emergency_light_conduit", "medicine_cabinet", "blueprint_sheet",
             "fire_extinguisher", "exhaust_fan", "h2_monitor", "h2_sensor", "sign_no_smoking", "sign_battery_acid", "sign_hydrogen", "sign_eyewash"}
USER_SIDE_OK = ("jugcraft:operator_chair", "jugcraft:cafe_chair", "jugcraft:coffee_table")


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
                   "jugcraft:cable_tray", "jugcraft:dc_cable_tray", "jugcraft:light_hanger", "jugcraft:high_bay_light", "minecraft:light[")
SEE_THROUGH = PASSABLE_PREFIX + ("minecraft:glass", "jugcraft:framed_glass", "jugcraft:blast_glass", "jugcraft:lead_glass", "minecraft:water", "jugcraft:guard_rail", "jugcraft:railing",
                                 "jugcraft:storage_tank_fill_pipe", "jugcraft:storage_tank_inlet", "jugcraft:storage_tank_outlet",
                                 "jugcraft:storage_tank_manway", "jugcraft:storage_tank_vent", "jugcraft:storage_tank_hatch",
                                 "jugcraft:operator_chair", "jugcraft:office_desk", "jugcraft:console_desk", "jugcraft:operator_console", "jugcraft:speed_gate_pedestal",
                                 "minecraft:iron_trapdoor", "jugcraft:water_cooler", "jugcraft:filing_cabinet",
                                 "jugcraft:whiteboard", "jugcraft:pallet_rack", "jugcraft:roller_door", "jugcraft:storage_rack",
                                 "jugcraft:dc_cable_riser", "jugcraft:station_battery")
LIGHTS = {"jugcraft:ceiling_light_panel": 15, "jugcraft:high_bay_light": 15, "minecraft:light": 15, "minecraft:sea_lantern": 15, "jugcraft:warning_light": 13, "jugcraft:warning_beacon": 10,
          "minecraft:campfire": 15, "jugcraft:server_rack": 3, "jugcraft:relay_cabinet": 2, "jugcraft:office_desk": 4,
          "jugcraft:console_desk": 4, "jugcraft:operator_console": 4}


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
            if name in ("whiteboard", "large_print", "office_plant", "coffee_table", "cafe_table", "cubicle_panel", "vitals_monitor", "stretcher", "glass_coffee_table"):
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
    # 8. Feeder conduits (round 25b): none stops in mid-air. Each column stands on a load centre's hub and reaches a
    # cable tray: base over the lineup, up to the ceiling, through the slab, up through the floor, into the tray.
    problems += feeder_conduits(site, g)
    # 9. Regulating transformers (round 26, 8d): each beside the inverter of its label, fed from its train's MCC, the
    # conduits up from its top and from the MCC's REG XF section reaching the same ladder tray; and (8e) each inverter's
    # vital panel, their conduits into the same tray.
    problems += regulating_transformers(site, g)
    # 10. The battery rooms (round 26, #9): their second strings, sense cable risers and fittings.
    problems += battery_rooms(site, g)
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
    for name in ("h2_monitor", "eyewash_station", "spill_kit", "sign_no_smoking", "sign_battery_acid", "sign_hydrogen", "sign_eyewash"):
        n = sum(int((g == k).sum()) for k, (st, _) in P.MATERIALS.items() if kinds[k] == name and "part=1" not in st)
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
    for start in sorted(cells):
        if start in seen:
            continue
        net, touches, q = [], set(), deque([start])
        seen.add(start)
        while q:
            c = q.popleft()
            net.append(c)
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
        problems.append("the main transformer does not feed both of the control building's load centres")
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
