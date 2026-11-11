"""Tri-Pillar Drone Tower geometry (the approved October 2026 design, docs: claude/jugcraft-drone-tower-redesign.md).

Pure geometry in block cells, independent of which blocks fill them: tools/tower_tripillar.py maps the material
keys used here to real block states and writes the layout. Everything is relative to the Tower Core at (0, 0, 0)
on the hall floor; +y up.

Design (as approved):
- Foundation / tier 1: the control building, a hexagon of circumradius 52 (104 across), two storeys to y 20:
  a glass ground-floor hall (to y 10) with mullions, an upper floor with a glass band, a metal parapet and an amber
  roof line. A round atrium of radius 15 opens the centre from the hall floor through the roof, closed above by a
  glass lantern between the pillar feet up to y 34.6. Six landing pads (7x7) on the roof for the 24 tier-1 drones.
- Tiers 2-9: three chamfered square pillars at 120 degrees round the centre (centres ~18 out, shrinking each tier),
  hangar bays on their outer faces (small 3x3 T2-T3, medium 6x4 T4-T6, large 7x5 T7-T9), a triangular tie deck at
  every tier line, a glass lift core from the tier-2 deck up. Tier lines TB = 0, 20, 36, 55, 76, 100, 130, 162, 198, 238.
"""
import math

TB = [0, 20, 36, 55, 76, 100, 130, 162, 198, 238]
PLAN = {1: ("pad", 24), 2: ("small", 20), 3: ("small", 18), 4: ("medium", 16), 5: ("medium", 14), 6: ("medium", 12),
        7: ("large", 10), 8: ("large", 8), 9: ("large", 6)}
# bay opening width, height, depth into the pillar, row pitch
BAY = {"small": (3, 3, 4, 5), "medium": (6, 4, 5, 6), "large": (7, 5, 8, 7)}
CHAMFER = 3
HEX_R = 52
ATRIUM_R = 15
ROOF_Y = 20
LANTERN_TOP = 34
PAD_RING = 37
PAD_SIZE = 7


def pillar_distance(t):
    return round(18 - .3 * t)


def pillar_centres(t):
    """Centres of pillars A (south, +z), B (north-east) and C (north-west) for tier t."""
    d = pillar_distance(t)
    return {"A": (0, d), "B": (round(.866 * d), -round(.5 * d)), "C": (-round(.866 * d), -round(.5 * d))}


def half_width(t):
    return int(round(2 * (9.6 - .15 * t)) // 2)


# outward faces of each pillar that carry bays (the faces that do not look at the other pillars)
FACES = {"A": [(0, 1), (1, 0), (-1, 0)], "B": [(1, 0), (0, -1)], "C": [(-1, 0), (0, -1)]}


def inside_pillar(x, z, cx, cz, hw, ch=CHAMFER):
    dx, dz = abs(x - cx), abs(z - cz)
    return dx <= hw and dz <= hw and dx + dz <= 2 * hw - ch


def inside_hex(x, z, r=HEX_R):
    """Pointy-side hexagon with flat sides north and south? No: vertices at 30 degrees (flat sides east/west)."""
    ax, az = abs(x), abs(z)
    return az <= r * math.sqrt(3) / 2 and ax * math.sqrt(3) / 2 + az / 2 <= r * math.sqrt(3) / 2


def lay_bays(t):
    """The bays of tier t as dicts {pillar, face (fx, fz), along s, y, w, h, depth, size}, rows bottom up."""
    size, n = PLAN[t]
    w, h, depth, pitch = BAY[size]
    y0, y1 = TB[t - 1], TB[t]
    hw = half_width(t)
    length = 2 * hw - 2 * CHAMFER
    rows = max(1, (y1 - y0 - 2) // pitch)
    left, got = n, []
    for r in range(rows):
        if left <= 0:
            break
        y = y0 + 1 + r * pitch
        cands = []
        for k in "ABC":
            for fi, (fx, fz) in enumerate(FACES[k]):
                m = max(0, int((length - 1) // (w + 2)))
                for i in range(m):
                    s = round((i - (m - 1) / 2) * (w + 2))
                    cands.append((i, fi, k, fx, fz, s))
        cands.sort()
        want = min(left, math.ceil(left / (rows - r)))
        used = {"A": [], "B": [], "C": []}
        took = 0
        for i, fi, k, fx, fz, s in cands:
            if took >= want:
                break
            # footprint in pillar-local coords: the room runs `depth` in from the face
            if fx:
                bx = sorted([fx * hw, fx * (hw - depth)])
                bz = [s - w // 2 - 1, s + w // 2 + 1]
            else:
                bz = sorted([fz * hw, fz * (hw - depth)])
                bx = [s - w // 2 - 1, s + w // 2 + 1]
            if any(bx[0] < u[1] and bx[1] > u[0] and bz[0] < u[3] and bz[1] > u[2] for u in used[k]):
                continue
            used[k].append((bx[0], bx[1], bz[0], bz[1]))
            got.append({"pillar": k, "face": (fx, fz), "s": s, "y": y, "w": w, "h": h, "depth": depth, "size": size, "row": r})
            took += 1
        left -= took
    return got


def row_floors(t):
    size, _ = PLAN[t]
    pitch = BAY[size][3]
    y0, y1 = TB[t - 1], TB[t]
    rows = max(1, (y1 - y0 - 2) // pitch)
    return [y0 + r * pitch for r in range(rows)]
