"""The rooms behind the pillars' maintenance doors (work list item 21's last part; take 1 approved by Justin, 26 October
2026): the bottom row of each pillar off the roof (tier 2) and off every tie deck (tiers 3-9) as a drone service floor,
laid out from the room's own bays, of pieces already in the game:
- a work station along the back of each of the row's bays, its user facing the bay: the LiPo charging cabinet, the
  workbenches under their pegboards, the roll cab, the parts racks (tools/lab_kit.py), the longer the wider the bay
  (a room with no bay on its row, tiers 8 and 9's pillar C, has one along the wall across from its doors);
- a green pedestrian walkway with yellow edge lines (the warehouse's Walkway tiles) from the doors in, to a strip in front
  of each station and to the ladder's foot, by the shortest ways over the free floor, never over a station;
- a cable tray under the ceiling over each station, its two ends capped;
- a fire extinguisher on the wall beside the doors, inside;
- on the small and medium rows a drone of the row's tier on a service stand near the first station (a large drone on a
  stand would fill the room).
The rows above (up the ladder) and the riser rooms in the pillars' feet stay as they were. tools/tower_tripillar.py
pillar_tier runs it last, on the generator's materials.
"""
from __future__ import annotations

import collections

import tripillar_geometry as G

DIRS4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
FACING = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}
OPPF = {"east": "west", "west": "east", "south": "north", "north": "south"}
BENCH, PEG, CAB, RACK, LIPO = ("jugcraft:lab_workbench", "jugcraft:lab_pegboard", "jugcraft:lab_tool_cabinet",
                               "jugcraft:parts_rack", "jugcraft:lipo_cabinet")
# the station along a bay's back, left to right as you face it: by the bay's width (its back wall is w + 2 wide)
STATION = {3: "L B B B C", 6: "L R B B B B C L", 7: "L R B B B B B C R"}


def rooms(T, b, t):
    """Each pillar's door-level room: {k: (cells, door inside cells, bays at its row, ladder cells, y0, height)}."""
    y0 = G.TB[t - 1]
    size, _ = G.PLAN[t]
    w, h, depth, pitch = G.BAY[size]
    floors = G.row_floors(t)
    top = floors[1] if len(floors) > 1 else G.TB[t]
    out = {}
    for k in "ABC":
        cells = T.pillar_cells(t, k)
        edge = T.edge_of(cells)
        inner = set(cells) - set(edge)
        door = [(x, z) for (x, z) in edge if "maintenance_door" in b.get((x, y0 + 1, z), "")]
        inside = []
        for (x, z) in door:
            for dx, dz in DIRS4:
                if (x + dx, z + dz) in inner:
                    inside.append((x + dx, z + dz))
        bays = [bay for bay in G.lay_bays(t) if bay["pillar"] == k and bay["row"] == 0]
        ladder = {(x, z) for (x, z) in inner if any(n in b.get((x, y0 + 1, z), "") for n in ("ladder", "steel_armor"))}
        out[k] = (inner, inside, bays, ladder, y0, top - y0 - 1)
    return out


def fit(T, b, t):
    """Fits tier t's door-level pillar rooms into b (T: tools/tower_tripillar.py, its put, pillar_cells, edge_of)."""
    size, _ = G.PLAN[t]
    w, h, depth, pitch = G.BAY[size]
    pad_depth = {"small": 3, "medium": 4, "large": 7}[size]
    hw = G.half_width(t)
    for k, (inner, inside, bays, ladder, y0, H) in rooms(T, b, t).items():
        if not inside:
            continue
        cx, cz = G.pillar_centres(t)[k]
        foot = set()
        stations = []
        for bay in bays:
            (fx, fz), s = bay["face"], bay["s"]
            a0 = s - w // 2

            def cell(v, a):
                return (cx + fx * (hw - v), cz + a) if fx else (cx + a, cz + fz * (hw - v))
            for v in range(0, pad_depth + 2):
                for a in range(a0 - 1, a0 + w + 1):
                    foot.add(cell(v, a))
            # the station: in front of the back wall, the user facing it (out towards the face)
            row = [cell(pad_depth + 2, a) for a in range(a0 - 1, a0 + w + 1)]
            front = [cell(pad_depth + 3, a) for a in range(a0 - 1, a0 + w + 1)]
            stations.append((FACING[(fx, fz)], row, front, (fx, fz), STATION[w].split()))
        # the way in from the doors
        ddx, ddz = (0, 0)
        for dx, dz in DIRS4:
            if all((c[0] - dx, c[1] - dz) not in inner for c in inside):
                ddx, ddz = dx, dz
        if not stations:
            # no bay on this row: a parts and charging station along the far wall, facing the doors' way in
            wall = [c for c in inner - ladder if (c[0] + ddx, c[1] + ddz) not in inner and (c[0], y0 + 1, c[1]) not in b]
            far = max(c[0] * ddx + c[1] * ddz for c in wall)
            line = sorted((c for c in wall if c[0] * ddx + c[1] * ddz == far), key=lambda c: (c[0], c[1]))
            mid = len(line) // 2
            row = line[max(0, mid - 3):mid + 4]
            front = [(x - ddx, z - ddz) for (x, z) in row]
            plan = {7: "L R B B B C R", 6: "L R B B C R", 5: "L B B B C"}.get(len(row), "L " + "B " * max(0, len(row) - 2) + "C").split()
            stations.append((FACING[(ddx, ddz)], row, front, (ddx, ddz), plan))
        free = {c for c in inner - foot - ladder if (c[0], y0 + 1, c[1]) not in b}
        # (the walkway keeps off the stations' own cells)
        booked = {c for _f, row, _fr, _d, _p in stations for c in row}
        floor = free - booked
        # the walkway: the door's way in, two wide, to the room's middle; a strip in front of each station; the way to
        # the ladder; joined to the rest by the shortest ways over free floor
        walk = set()

        def path(a, goals):
            prev, todo = {a: None}, collections.deque([a])
            while todo:
                c = todo.popleft()
                if c in goals:
                    out = []
                    while c is not None:
                        out.append(c)
                        c = prev[c]
                    return out
                for dx, dz in DIRS4:
                    n = (c[0] + dx, c[1] + dz)
                    if n in floor and n not in prev:
                        prev[n] = c
                        todo.append(n)
            return []
        for c in inside:
            walk.add(c)
        # straight in from the door
        for c in inside:
            x, z = c
            for _ in range(2 * hw):
                x, z = x + ddx, z + ddz
                if (x, z) not in floor:
                    break
                walk.add((x, z))
                if abs(x - cx) + abs(z - cz) <= 1:
                    break
        for facing, row, front, _f, _p in stations:
            strip = [c for c in front if c in floor]
            walk |= set(strip)
            if strip:
                walk |= set(path(strip[len(strip) // 2], walk - set(strip)))
        # the ladder's foot: where its climber stands (in front of it, the way it faces)
        steps = {"east": (1, 0), "west": (-1, 0), "south": (0, 1), "north": (0, -1)}
        for (x, z) in ladder:
            st = b.get((x, y0 + 1, z), "")
            if "ladder" in st and "facing=" in st:
                dx, dz = steps[st.split("facing=")[1].split(",")[0].split("]")[0]]
                if (x + dx, z + dz) in floor:
                    walk |= set(path((x + dx, z + dz), walk))
                break
        for (x, z) in walk:
            mask = sum(bit for bit, (dx, dz) in zip((1, 2, 4, 8), ((0, -1), (1, 0), (0, 1), (-1, 0))) if (x + dx, z + dz) not in walk)
            T.put(b, x, y0, z, f"jugcraft:warehouse_walkway[mask={mask},soffit=none,stencil={'true' if (x * 5 + z * 3) % 13 == 0 else 'false'}]")
        # the stations
        for n, (facing, row, front, (fx, fz), plan) in enumerate(stations):
            # (left to right as you face the wall: the row runs along a; facing east the user's left is north (-z))
            lx, lz = {"east": (0, -1), "west": (0, 1), "south": (1, 0), "north": (-1, 0)}[facing]
            order = sorted(row, key=lambda c: c[0] * lx + c[1] * lz, reverse=True)
            for i, (c, p) in enumerate(zip(order, plan)):
                if c not in free or c in walk:
                    continue
                x, z = c
                if p == "B":
                    T.put(b, x, y0 + 1, z, f"{BENCH}[facing={facing},variant={(i + n) % 6}]")
                    T.put(b, x, y0 + 2, z, f"{PEG}[facing={OPPF[facing]},variant={(i + 2 * n) % 4}]")
                elif p == "C":
                    T.put(b, x, y0 + 1, z, f"{CAB}[colour={'red' if n % 2 == 0 else 'grey'},facing={facing}]")
                elif p == "L":
                    T.put(b, x, y0 + 1, z, f"{LIPO}[facing={facing},half=lower]")
                    T.put(b, x, y0 + 2, z, f"{LIPO}[facing={facing},half=upper]")
                elif p == "R":
                    v = (i + n) % 2
                    T.put(b, x, y0 + 1, z, f"{RACK}[facing={facing},half=lower,variant={v}]")
                    T.put(b, x, y0 + 2, z, f"{RACK}[facing={facing},half=upper,variant={v + 2}]")
                free.discard(c)
            # its cable tray under the ceiling, over the station, its two ends capped
            # (a straight tray facing north runs along x; an end facing north is capped at its east, so each end is
            # turned to cap the run's own end)
            run = sorted((c for c in order if (c[0], y0 + H, c[1]) not in b), key=lambda c: (c[0], c[1]))
            along_x = lz == 0
            for i, (x, z) in enumerate(run):
                if 0 < i < len(run) - 1:
                    st = f"facing={'north' if along_x else 'east'},shape=straight"
                elif along_x:
                    st = f"facing={'north' if i else 'south'},shape=end"
                else:
                    st = f"facing={'east' if i else 'west'},shape=end"
                T.put(b, x, y0 + H, z, f"jugcraft:cable_tray[{st}]")
        # a drone down for service: its stand in the open by the first station, a roll cab by it
        open_ = [c for c in free if c not in walk and all((c[0] + dx, c[1] + dz) in free for dx, dz in DIRS4)]
        # (on the small and medium rows only: a large drone on a stand would fill the room)
        if stations and open_ and size != "large":
            sx, sz = stations[0][2][len(stations[0][2]) // 2]
            c = min(open_, key=lambda c: (abs(c[0] - sx) + abs(c[1] - sz), c))
            T.put(b, c[0], y0 + 1, c[1], f"jugcraft:drone_display_stand[facing={stations[0][0]},style=service,tier={t}]")
            free.discard(c)
        # an extinguisher on the wall beside the door, inside
        for (x, z) in inside:
            for dx, dz in DIRS4:
                n = (x + dx, z + dz)
                if n in free and n not in walk and n not in inside:
                    back = [d for d in DIRS4 if (n[0] + d[0], n[1] + d[1]) not in inner]
                    if back:
                        T.put(b, n[0], y0 + 2, n[1], f"jugcraft:fire_extinguisher[facing={FACING[back[0]]}]")
                        break
            else:
                continue
            break
