"""The zeppelin (batch 46, docs/features/zeppelin.md): a rideable dieselpunk airship.

A rigid airship held up by hydrogen lift cells: a banded canvas envelope with tail fins, hung on struts over a riveted
rust-and-patina gondola with porthole windows, two diesel engine pods with propellers, and a cargo hold. The pilot
flies it with the movement keys (forward and back, turn left and right), jump to climb and sprint to sink; it burns
diesel or kerosene, poured in from buckets, and floats where it is left.

Java: airship/ (Zeppelin, ZeppelinItem, ZeppelinInputPayload, JugcraftAirships); client/ZeppelinRenderer draws the
quads exported here (assets/jugcraft/zeppelin_quads.json). tools/check_mod_data.py keeps the numbers the same.
"""
import json
import math

from PIL import Image

from steampunk_models import box, cyl, wheel
from kinetic_rotors import FACE_CORNERS, face_texture, quads as rotated_quads, turn_for_separation
from model_writer import separate_boxes, unpack

MOD = "jugcraft"

# Flight, in blocks a tick and degrees a tick: top speed forward (half that backward), how fast it gets there, climb
# and sink rates, turning, and how fast it sinks with an empty tank.
MAX_SPEED = 0.35
ACCELERATION = 0.01
CLIMB = 0.12
TURN = 1.5
DRIFT_SINK = 0.03
# Fuel in mB: the tank, what a bucket adds, and mB burned each second the engines work.
FUEL_TANK = 8000
FUEL_PER_BUCKET = 1000
FUEL_PER_SECOND = 5
SEATS = 4
CARGO_SLOTS = 27
# Damage a player must deal (with no break in between) to knock it down into an item.
HEALTH = 40
# The hitbox, in blocks (the envelope is longer than this; the box covers the gondola and the envelope's middle).
WIDTH = 5.0
HEIGHT = 7.5
FUELS = {"diesel_bucket": "Diesel", "premium_diesel_bucket": "Premium Diesel", "kerosene_bucket": "RP-1 Kerosene"}

ITEMS = {"zeppelin": "Zeppelin"}
TOOLTIPS = {"zeppelin": "A diesel airship for four, with a cargo hold. Fly: movement keys, jump to climb, sprint to sink. "
                        "Use a diesel or kerosene bucket on it to refuel; sneak-use for the cargo hold."}

CANVAS, STRIPE, NOSE = "dz_canvas", "dz_canvas_stripe", "dz_canvas_nose"
RUST, RUST_BARE, PATINA, PERFORATED = "dr_rust", "dr_rust_bare", "dr_patina", "dr_perforated"
BAND, COPPER, RED, BLUE, AMBER = "dr_band", "dr_copper_pipe", "dr_red", "dr_blue", "dr_amber_on"
RIB_PATINA, SKID, GRATE, BLADE = "dr_ribbed_patina", "dr_skid", "dr_grate", "dr_blade"
EXHAUST = "dp_exhaust"

# The envelope: (z, radius) stations from tail (-z) to nose (+z), in pixels; centre line 84 px up.
ENVELOPE_Y = 84
STATIONS = [(-104, 10), (-92, 22), (-76, 32), (-56, 38), (-28, 40), (16, 40), (44, 38), (64, 32), (80, 24), (92, 14),
            (100, 6)]
# Engine pods: centre (x, y, z) of each propeller hub, in pixels; the propellers face backwards (-z).
PROPS = [(-34, 26, -18), (34, 26, -18)]


def round_section(cy, r, z0, z1, texture, steps=6):
    """A round section of the envelope along z: horizontal slabs, each as wide as the circle at its height, so the
    outline is smoother than steampunk_models.cyl's few bands."""
    m = []
    for i in range(steps):
        h0, h1 = r * i / steps, r * (i + 1) / steps
        half = math.sqrt(max(0.0, r * r - h0 * h0))
        half = round(half * 4) / 4
        for lo, hi in ((cy + h0, cy + h1), (cy - h1, cy - h0)):
            m.append(box((-half, round(lo * 4) / 4, z0), (half, round(hi * 4) / 4, z1), texture))
    return m


def envelope():
    m = []
    for (z0, r0), (z1, r1) in zip(STATIONS, STATIONS[1:]):
        r = (r0 + r1) / 2
        texture = NOSE if z1 >= 92 or z0 <= -104 else CANVAS
        m += round_section(ENVELOPE_Y, r, z0, z1, texture, 6 if r > 20 else 3)
    for z in (-60, -20, 20, 56):  # Painted bands round the envelope.
        r = next(rr for (za, rr), (zb, _) in zip(STATIONS, STATIONS[1:]) if za <= z < zb)
        m += round_section(ENVELOPE_Y, r + 0.6, z, z + 3, STRIPE)
    # Tail fins: top, bottom and both sides, with red-and-white rudders.
    m.append(box((-1, ENVELOPE_Y + 18, -108), (1, ENVELOPE_Y + 44, -76), {"*": CANVAS, "east": STRIPE, "west": STRIPE}))
    m.append(box((-1, ENVELOPE_Y - 40, -108), (1, ENVELOPE_Y - 18, -78), {"*": CANVAS, "east": STRIPE, "west": STRIPE}))
    m.append(box((18, ENVELOPE_Y - 1, -108), (44, ENVELOPE_Y + 1, -76), {"*": CANVAS, "up": STRIPE, "down": STRIPE}))
    m.append(box((-44, ENVELOPE_Y - 1, -108), (-18, ENVELOPE_Y + 1, -76), {"*": CANVAS, "up": STRIPE, "down": STRIPE}))
    m.append(box((-1.5, ENVELOPE_Y + 18, -112), (1.5, ENVELOPE_Y + 40, -108), RED))
    m.append(box((-1.5, ENVELOPE_Y - 36, -112), (1.5, ENVELOPE_Y - 18, -108), RED))
    # Nose cap and mooring ring.
    m += cyl("z", 0, ENVELOPE_Y, 3, 100, 106, PATINA, BLUE)
    return m


def gondola():
    """The gondola under the envelope: a riveted hull with portholes, a glazed bridge at the front, a railed deck
    with the pilot's wheel, engine pods on outriggers, and struts up to the envelope."""
    m = []
    # Hull: keel, sides and floor.
    m.append(box((-14, 0, -40), (14, 4, 40), {"*": SKID, "up": GRATE}))
    m.append(box((-16, 4, -42), (16, 20, -36), {"*": RUST, "south": RUST_BARE}))
    m.append(box((-16, 4, 36), (16, 14, 44), {"*": RUST, "north": RUST_BARE}))
    for x0, x1, face in ((-16, -14, "west"), (14, 16, "east")):
        m.append(box((x0, 4, -36), (x1, 14, 36), {"*": RUST, face: RUST}))
        m.append(box((x0, 14, -36), (x1, 16, 36), BAND))
        for z in (-26, -10, 6, 22):  # Portholes.
            m.append(box((x0 - 0.25, 7, z), (x1 + 0.25, 13, z + 6), {"*": PATINA, face: "dw_porthole!"}))
    # A cabin at the back: perforated green walls, a roof and a lit window.
    m.append(box((-13, 4, -34), (13, 26, -16), {"*": PERFORATED, "up": PATINA}))
    m.append(box((-14, 26, -35), (14, 28, -15), RUST_BARE))
    m.append(box((-8, 12, -15.75), (8, 20, -15.25), {"*": RUST_BARE, "south": f"{AMBER}!"}))
    # The bridge at the front: a red console with gauges and the wheel.
    m.append(box((-10, 4, 30), (10, 14, 36), {"*": RED, "up": RUST_BARE}))
    m.append(box((-6, 14, 33), (6, 15, 35.5), {"*": RUST_BARE, "south": f"{AMBER}!", "north": "sp_gauge!"}))
    m += wheel("z", 0, 16, 4, 26, 27, "dr_red", "dr_nut")
    m.append(box((-0.5, 12, 27), (0.5, 16, 30), BAND))
    # Deck railings along the open middle.
    for x0 in (-15.5, 14.5):
        m.append(box((x0, 20, -16), (x0 + 1, 21, 30), COPPER))
        for z in (-14, -2, 10, 22):
            m.append(box((x0, 14, z), (x0 + 1, 20, z + 1), COPPER))
    # Struts and cables up to the envelope.
    for x in (-12, 11):
        for z in (-30, 0, 28):
            m.append(box((x, 16 if z else 20, z), (x + 1, ENVELOPE_Y - 36, z + 1), BAND))
    # Engine pods on outriggers, each with a ribbed green cowling, an exhaust and a propeller hub.
    for px, py, pz in PROPS:
        side = 1 if px > 0 else -1
        m.append(box((min(16 * side, px - 6 * side), py - 2, pz - 2), (max(16 * side, px - 6 * side), py + 2, pz + 2), SKID))
        m += cyl("z", px, py, 7, pz + 2, pz + 22, RIB_PATINA, RUST_BARE)
        m += cyl("z", px, py, 7.6, pz + 8, pz + 10, BAND)
        m += cyl("z", px, py, 4, pz + 22, pz + 26, RUST_BARE, BLUE)
        m += cyl("z", px, py, 2, pz - 2, pz + 2, "dr_nut")
        m.append(box((px - 1, py + 7, pz + 12), (px + 1, py + 13, pz + 14), {"*": EXHAUST, "up": "dr_soot"}))
    return m


def propeller():
    """One four-bladed propeller about its own hub at the origin, turning about z."""
    m = cyl("z", 0, 0, 1.5, -1.5, 1.5, "dr_nut", BLUE)
    m.append(box((-0.75, 2, -0.25), (0.75, 13, 0.25), BLADE))
    m.append(box((-0.75, -13, -0.25), (0.75, -2, 0.25), BLADE))
    m.append(box((2, -0.75, -0.25), (13, 0.75, 0.25), BLADE))
    m.append(box((-13, -0.75, -0.25), (-2, 0.75, 0.25), BLADE))
    return m


def body():
    return envelope() + gondola()


# ------------------------------------------------------------------ export

_AXIS = {"north": 2, "south": 2, "west": 0, "east": 0, "up": 1, "down": 1}
_EPS = 1e-6


def _merge(cells):
    """Greedy merge of kept grid cells {(i, j)} into rectangles of index ranges [(i0, i1, j0, j1)]."""
    left = set(cells)
    out = []
    for i, j in sorted(cells):
        if (i, j) not in left:
            continue
        j1 = j
        while (i, j1 + 1) in left:
            j1 += 1
        i1 = i
        while all((i1 + 1, jj) in left for jj in range(j, j1 + 1)):
            i1 += 1
        for ii in range(i, i1 + 1):
            for jj in range(j, j1 + 1):
                left.discard((ii, jj))
        out.append((i, i1, j, j1))
    return out


def tiled_quads(elements):
    """Quads for big models: every face cut into 16-pixel cells, each mapped as vanilla maps a block face, so the
    texture repeats over the face instead of stretching (the renderer draws each texture once, 0..1). Rotated
    elements and stretched ("!") faces go through kinetic_rotors.quads unchanged.

    Hidden faces are removed exactly, because QuadModel draws plain quads with a culling render type: a face area
    that is left out but exposed is a window straight through the model (docs/ART_DIRECTION.md, closed geometry).
    For each face of an unrotated box, the parts covered by another unrotated box (whose inside lies just outside
    the face) are cut away; where two boxes have faces on the same plane facing the same way, only the smaller face
    (the detail; the lower index on a tie) is kept in the overlap, so nothing z-fights. The face is cut at its own
    edges, every cover's edges and the 16-pixel grid; the uncovered pieces are merged back into rectangles inside
    each 16-pixel cell, so the cell-local UVs stay exactly as they were. Faces drawn the other way (rotated elements
    and stretched faces) cannot be cut like that, so where one of them shares a plane with another face, the smaller
    of the two boxes is first pushed out by model_writer.COPLANAR_NUDGE (separate_boxes, whole boxes only)."""
    def drawn_whole(item, face):
        frm, to, texture, options = unpack(item)
        return bool(options.get("rotation")) or str(face_texture(texture, face)).endswith("!")
    elements = separate_boxes(elements, turn=turn_for_separation,
                              pair_filter=lambda a, fa, b, fb: drawn_whole(a, fa) or drawn_whole(b, fb))
    out, rest, plain = [], [], []
    for index, item in enumerate(elements):
        frm, to, texture, options = unpack(item)
        if options.get("rotation"):
            rest.append(item)
        else:
            plain.append((index, frm, to, texture))

    def face_area(frm, to, axis):
        a, b = [k for k in range(3) if k != axis]
        return (to[a] - frm[a]) * (to[b] - frm[b])

    for index, frm, to, texture in plain:
        for face, (corners, normal) in FACE_CORNERS.items():
            name = face_texture(texture, face)
            if name is None:
                continue
            if name.endswith("!"):
                rest.append((frm, to, {face: name, "*": None}))
                continue
            axis = _AXIS[face]
            spans = [k for k in range(3) if k != axis]
            sign = normal[axis]
            fixed = to[axis] if sign > 0 else frm[axis]
            mine = face_area(frm, to, axis)
            if mine <= _EPS:
                continue
            covers = []
            for other, f2, t2, texture2 in plain:
                if other == index:
                    continue
                lo = [max(frm[k], f2[k]) for k in spans]
                hi = [min(to[k], t2[k]) for k in spans]
                if lo[0] >= hi[0] - _EPS or lo[1] >= hi[1] - _EPS:
                    continue
                # Another box whose inside lies just outside this face hides it there.
                if (f2[axis] - _EPS < fixed < t2[axis] - _EPS) if sign > 0 else (f2[axis] + _EPS < fixed < t2[axis] + _EPS):
                    covers.append((lo, hi))
                    continue
                # A face of another box on the same plane, facing the same way: the smaller one (the detail) is drawn.
                plane = t2[axis] if sign > 0 else f2[axis]
                if abs(plane - fixed) < _EPS and face_texture(texture2, face) is not None:
                    theirs = face_area(f2, t2, axis)
                    if theirs < mine - _EPS or (abs(theirs - mine) <= _EPS and other < index):
                        covers.append((lo, hi))
            # Cut lines: the face's edges, every cover's edges and the 16-pixel grid.
            cuts = []
            for n, k in enumerate(spans):
                edges = {frm[k], to[k]}
                edges.update(16 * i for i in range(math.floor(frm[k] / 16) + 1, math.ceil(to[k] / 16)))
                for lo, hi in covers:
                    edges.update((lo[n], hi[n]))
                cuts.append(sorted(e for e in edges if frm[k] - _EPS <= e <= to[k] + _EPS))
            kept = {}
            for i in range(len(cuts[0]) - 1):
                a0, a1 = cuts[0][i], cuts[0][i + 1]
                if a1 - a0 <= _EPS:
                    continue
                for j in range(len(cuts[1]) - 1):
                    b0, b1 = cuts[1][j], cuts[1][j + 1]
                    if b1 - b0 <= _EPS:
                        continue
                    ca, cb = (a0 + a1) / 2, (b0 + b1) / 2
                    if any(lo[0] < ca < hi[0] and lo[1] < cb < hi[1] for lo, hi in covers):
                        continue
                    kept.setdefault((math.floor(ca / 16), math.floor(cb / 16)), set()).add((i, j))
            for cell in sorted(kept):
                for i0, i1, j0, j1 in _merge(kept[cell]):
                    lo, hi = [0.0] * 3, [0.0] * 3
                    lo[axis] = hi[axis] = fixed
                    lo[spans[0]], hi[spans[0]] = cuts[0][i0], cuts[0][i1 + 1]
                    lo[spans[1]], hi[spans[1]] = cuts[1][j0], cuts[1][j1 + 1]
                    base = [math.floor(((lo[k] + hi[k]) / 2) / 16) * 16 for k in range(3)]
                    points = [[hi[k] if corner[k] else lo[k] for k in range(3)] for corner in corners]
                    vertices = []
                    for p in points:
                        local = [p[k] - base[k] for k in range(3)]
                        u, v = {"north": (16 - local[0], 16 - local[1]), "south": (local[0], 16 - local[1]),
                                "west": (local[2], 16 - local[1]), "east": (16 - local[2], 16 - local[1]),
                                "up": (local[0], local[2]), "down": (local[0], 16 - local[2])}[face]
                        vertices.append([round(p[0], 4), round(p[1], 4), round(p[2], 4), round(u / 16, 5), round(v / 16, 5)])
                    out.append({"texture": name, "normal": list(normal), "vertices": vertices})
    out += rotated_quads(rest, separate=False)
    for quad in out:
        if quad["texture"] == "dw_porthole":
            quad["cutout"] = True
    return out


def export():
    return {"zeppelin_body": tiled_quads(body()), "zeppelin_propeller": tiled_quads(propeller())}


# ------------------------------------------------------------------ data

def write_all(write, assets, data, lang, condition):
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"entity.{MOD}.zeppelin"] = "Zeppelin"
    lang[f"container.{MOD}.zeppelin"] = "Zeppelin Cargo Hold"
    lang[f"message.{MOD}.zeppelin.fuel"] = "Fuel: %s%%"
    lang[f"message.{MOD}.zeppelin.empty"] = "Out of fuel: the zeppelin is sinking"
    lang[f"message.{MOD}.zeppelin.full"] = "The fuel tank is full"
    lang[f"message.{MOD}.zeppelin.refuelled"] = "Refuelled: %s%%"
    lang[f"message.{MOD}.zeppelin.no_room"] = "Not enough room here for a zeppelin"
    # Written compact: it is thousands of quads.
    (assets / "zeppelin_quads.json").write_text(json.dumps(export(), separators=(",", ":")) + "\n", encoding="utf-8")
    write(data / "recipe" / "zeppelin.json", {
        "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["HHH", "HEH", "PCP"],
        "key": {"H": f"{MOD}:hydrogen_lift_cell", "E": f"{MOD}:diesel_engine", "P": "#c:plates/steel",
                "C": "minecraft:chest"},
        "result": {"id": f"{MOD}:zeppelin", "count": 1}})


# ------------------------------------------------------------------ art

CANVAS_COLORS = [(150, 138, 104), (170, 158, 122), (188, 176, 138), (204, 194, 156)]


def canvas(seed, stripe=False, nose=False):
    """Doped canvas: a smooth khaki panel lit along its top, a stitched seam across it and a faint rib line; the
    stripe band is red and cream, the nose a darker khaki."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = CANVAS_COLORS[2]
            if y == 0:
                c = CANVAS_COLORS[0]
            elif y == 1:
                c = CANVAS_COLORS[3]
            elif y == 8:
                c = CANVAS_COLORS[1]
            elif x == 0:
                c = CANVAS_COLORS[1]
            if nose:
                c = tuple(int(v * 0.72) for v in c)
            if stripe and 4 <= y < 12:
                c = (150, 36, 28) if (y < 6 or y >= 10) else (220, 210, 180)
            img.putpixel((x, y), c + (255,))
    for x in range(1, 16, 3):
        img.putpixel((x, 0), (120, 110, 82, 255))
    return img


def draw_all(save):
    import item_icons  # the 16x16 icon, a map in tools/item_icons/ like the big guns' and the other war machines'
    save(canvas(4601), "block", CANVAS)
    save(canvas(4602, stripe=True), "block", STRIPE)
    save(canvas(4603, nose=True), "block", NOSE)
    save(item_icons.draw("zeppelin"), "item", "zeppelin")
