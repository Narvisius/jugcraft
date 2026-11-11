"""No two faces of the overview screen's picture share a plane where they overlap (Justin: never any z-fighting): every
page drawn with the frame, its fixed quads and its live ones (lamps, flows, fills and the levels' bars at full),
checked pair by pair (convex quads clipped against each other). Used by tools/check_mod_data.py."""
EPS = 2e-7                      # an overlap this small (block squared) is two faces touching at an edge



def _quads(ops):
    out = []
    for op in ops["lit"] + ops["still"]:
        if op[0] == "q":
            out.append((op[1:13], "fixed"))
    live = ops.get("live", {})
    for key in ("lamps", "flows", "fills"):
        for e in live.get(key, []):
            out.append((e["q"], key))
    for e in live.get("levels", []):
        z = e["z"]
        out.append(([e["x0"], e["y1"], z, e["x0"], e["y0"], z, e["x1"], e["y0"], z, e["x1"], e["y1"], z], "level"))
    for e in live.get("alarms", []):
        for s in e["slots"]:
            x0, y0, x1, y1 = s
            out.append(([x0, y1, e["z"], x0, y0, e["z"], x1, y0, e["z"], x1, y1, e["z"]], "alarm"))
    return out


def _clip(subject, a, b):
    out = []
    n = len(subject)
    cross = lambda p: (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])
    for i in range(n):
        p, q = subject[i], subject[(i + 1) % n]
        cp, cq = cross(p), cross(q)
        if cp >= 0:
            out.append(p)
        if (cp >= 0) != (cq >= 0):
            t = cp / (cp - cq)
            out.append((p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t))
    return out


def _area(poly):
    return 0.5 * sum(poly[i][0] * poly[(i + 1) % len(poly)][1] - poly[(i + 1) % len(poly)][0] * poly[i][1] for i in range(len(poly)))


def _ccw(poly):
    return poly if _area(poly) >= 0 else poly[::-1]


def overlaps(page, eps=EPS):
    """The overlapping same-plane pairs of a page drawn over the frame: (z, kinds, area)."""
    import overview_screen as O
    O.ATLAS.items.clear()
    quads = _quads(O.section().ops()) + _quads(O.section(page).ops())
    by_z = {}
    for q, kind in quads:
        pts = [(q[i], q[i + 1]) for i in (0, 3, 6, 9)]
        if _area(_ccw(pts)) < 1e-10:
            continue
        by_z.setdefault(round(q[2], 4), []).append((_ccw(pts), kind))
    bad = []
    for z, items in by_z.items():
        boxes = [(min(p[0] for p in poly), min(p[1] for p in poly), max(p[0] for p in poly), max(p[1] for p in poly)) for poly, _ in items]
        for i in range(len(items)):
            for j in range(i + 1, len(items)):
                a, b = boxes[i], boxes[j]
                if a[2] <= b[0] + 1e-9 or b[2] <= a[0] + 1e-9 or a[3] <= b[1] + 1e-9 or b[3] <= a[1] + 1e-9:
                    continue
                poly = items[i][0]
                clip = items[j][0]
                for k in range(len(clip)):
                    poly = _clip(poly, clip[k], clip[(k + 1) % len(clip)])
                    if not poly:
                        break
                if poly and _area(poly) > eps:
                    bad.append((z, items[i][1], items[j][1], round(_area(poly), 8), boxes[i], boxes[j]))
    return bad


if __name__ == "__main__":
    import sys
    import overview_screen as O
    for p in (sys.argv[1:] or O.PAGES):
        bad = overlaps(p)
        print(p, len(bad))
        for b in bad[:8]:
            print("   ", b)
