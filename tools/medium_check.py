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
WALL_HUNG = {"whiteboard", "framed_print", "large_print", "wall_display", "coat_rack", "aed_cabinet", "emergency_light", "medicine_cabinet", "blueprint_sheet",
             "fire_extinguisher"}
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
    if not quiet:
        for p in problems:
            print(p)
    return problems


if __name__ == "__main__":
    site = MP.medium_site()
    found = check(site, quiet="--quiet" in sys.argv)
    print(f"{len(found)} problem(s)")
    sys.exit(1 if found else 0)
