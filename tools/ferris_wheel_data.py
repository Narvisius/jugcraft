"""JSON resources for the Ferris wheel (fall addition 27), from tools/ferris_wheel.py: the booth's block and item models,
its blockstate, names and messages, loot and tags, and the quads the client draws the wheel from
(assets/jugcraft/ferris_wheel_quads.json): the frame (two A-frames of lattice steel on their footings, braced, with the
axle through their bearings), the wheel (a sixteenth of its two trussed rims with their spokes, drawn sixteen times round,
the hub plates, and a car's pivot bar, drawn at each car), its lights (a sixteenth's worth), and a car in each of the four
colours.

Called from agriculture_data.py. The advancements' and recipe's data come through HALLOWEEN_ADVANCEMENTS and SHAPED in
tools/agriculture.py. Every shape is in model pixels (16 to a block) in the wheel's own frame: x along the wheel, y up,
z along the axle, the front at the low z side (north), as block models face; the client turns it to its facing.
  - The frame's origin is the middle of the booth's foot.
  - The wheel's parts' and the lights' origin is the hub; the client turns them about z.
  - A car's origin is its pivot; the client hangs it there, upright.
"""
import json
import math

from decor_data import MOD, rid, box, block_model, flat_item, self_drop, turned
from decor6_data import quads
from ferris_wheel import FERRIS_WHEEL, CAR_COLOURS
from ferris_wheel_textures import METAL_ACROSS_U, METAL_ACROSS_V, METAL_PLATE_SMALL, METAL_PLATE_BIG, METAL_FILL
import ferris_wheel

HORIZONTAL = ("north", "east", "south", "west")
FULL = (0, 0, 16, 16)
ALL = ("north", "south", "east", "west", "up", "down")
LONG = ("north", "south", "east", "west")

HUB = FERRIS_WHEEL["hub"] * 16
R = FERRIS_WHEEL["radius"] * 16
FEET = FERRIS_WHEEL["frame_feet"] * 16
R_IN = R - 12
RIMS = (-12.0, 12.0)          # the rims' middles along the axle
FRAMES = (-20.0, 20.0)        # the A-frames' middles along the axle
SEGMENTS = 16
SPOKES = 16
HALF_SEGMENT = 180.0 / SEGMENTS
# Each part of a rim's truss lies in its own planes along the axle (5 October 2026): how far each is set in from the
# outer ring's faces, a rim's middle +-1 pixel. Nothing shares a face plane with anything else, so nothing z-fights.
INSET = {"inner": 0.25, "spoke": 0.35, "tie_low": 0.45, "tie_high": 0.55}

FRAME_TEXTURES = {"lattice": "ferris_wheel_lattice", "steel": "ferris_wheel_steel", "axle": "ferris_wheel_axle",
                  "footing": "ferris_wheel_footing", "brass": "ferris_wheel_brass"}
WHEEL_TEXTURES = {"rim": "ferris_wheel_rim", "steel": "ferris_wheel_steel", "hub": "ferris_wheel_hub", "brass": "ferris_wheel_brass",
                  "bulb": "ferris_wheel_bulb"}
CUTOUT = {"ferris_wheel_lattice", "ferris_wheel_valance_pumpkin", "ferris_wheel_valance_cranberry", "ferris_wheel_valance_mustard",
          "ferris_wheel_valance_spruce"}

# Where things are painted on their 64 x 64 textures (tools/ferris_wheel_textures.py), in uv units (0 to 16, four
# texels each): every face samples about four texels to a model pixel, or fewer on faces longer than 16 pixels (a flat
# fill, stretched), never the whole texture squeezed onto a thin member (which shimmers as the camera moves: the client
# draws these textures without mipmaps).
LATTICE_FRONT = (0.0, 0.0, 5.0, 16.0)        # the legs' 5-pixel faces
LATTICE_SIDE = (6.0, 0.0, 10.0, 16.0)        # their 4-pixel faces
LATTICE_CAP = (11.0, 0.0, 16.0, 4.0)
RIM_FACE = 0.0           # the outer ring's front and back, 3 pixels deep, from v 0
RIM_OUT = 3.0            # its outer face, 2 pixels
RIM_IN = 5.0             # its inner face
INNER_FACE = 7.0         # the inner ring's front and back, 2 pixels
INNER_EDGE = 9.0         # its outer and inner faces, 1.5 pixels
CAR_PANEL = 0.0          # a car's big painted panels, 8 pixels tall
CAR_FRONT = 8.0          # its low front panel, 5 pixels tall
CAR_PLAIN = 13.0         # plain paint, for insides and edges
FOOTING_TOP = (0.0, 0.0)
FOOTING_SIDE = (0.0, 13.0)


def car_textures(colour):
    return {"car": f"ferris_wheel_car_{colour}", "canopy": f"ferris_wheel_canopy_{colour}", "valance": f"ferris_wheel_valance_{colour}",
            "seat": "ferris_wheel_seat", "floor": "ferris_wheel_floor", "brass": "ferris_wheel_brass", "steel": "ferris_wheel_steel"}


def with_uvs(element, uvs):
    for side, uv in uvs.items():
        if side in element["faces"]:
            element["faces"][side]["uv"] = list(uv)
    return element


def face_size(element, side):
    """A box face's size (pixels) along its texture's u and v, as vanilla lays a texture on each side."""
    (x0, y0, z0), (x1, y1, z1) = element["from"], element["to"]
    dx, dy, dz = x1 - x0, y1 - y0, z1 - z0
    return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}[side]


def sized(element, region=FULL, sides=None):
    """Pins each face's (or each of `sides`') uv to a box its own size, a pixel to a uv unit (four texels to a pixel on
    a 64 x 64 texture), from the corner of `region` (u0, v0, u1, v1), and no bigger than the region: a face longer than
    it is squeezed into it."""
    u0, v0, u1, v1 = region
    for side, face in element["faces"].items():
        if sides is None or side in sides:
            w, h = face_size(element, side)
            face["uv"] = [round(u0, 4), round(v0, 4), round(min(u0 + w, u1), 4), round(min(v0 + h, v1), 4)]
    return element


METALS = ("#steel", "#brass", "#axle")


def metal_width(size):
    """Which of the metals' strips (tools/ferris_wheel_textures.py) suits a member `size` pixels across."""
    return 1 if size <= 1.25 else 2 if size <= 2.3 else 3 if size <= 3.5 else 4 if size <= 5 else 6


def metal(elements):
    """Pins every steel, brass and axle face of `elements` to the part of the metals' texture that suits its shape, at
    about four texels to a pixel across it: a long face (a bar, a rail, a post) to a strip whose bands run along its
    length, lit along one edge; a squarer one at least 3.5 pixels each way to a plate, lit along its top and left and
    shaded along its bottom and right."""
    for element in elements:
        for side, face in element["faces"].items():
            if face["texture"] not in METALS:
                continue
            w, h = face_size(element, side)
            if min(w, h) >= 3.5 and max(w, h) < 2.5 * min(w, h):
                face["uv"] = list(METAL_PLATE_SMALL if max(w, h) <= 6 else METAL_PLATE_BIG)
            elif h >= w:
                u0, u1 = METAL_ACROSS_U[metal_width(w)]
                face["uv"] = [u0, 0, u1, round(min(h, 4.0), 4)]
            else:
                u0, v0, u1, v1 = METAL_ACROSS_V[metal_width(h)]
                face["uv"] = [u0, v0, round(u0 + min(w, u1 - u0), 4), v1]
    return elements


def bar(a, b, width, z0, z1, texture, pieces=1, faces=ALL, regions=None):
    """A straight bar in the wheel's plane from point `a` to point `b` (x, y), `width` across, from z0 to z1 along the
    axle, in `pieces` lengths. `regions` maps a face to its texture region (default: the whole texture, sized)."""
    dx, dy = b[0] - a[0], b[1] - a[1]
    length = math.hypot(dx, dy)
    angle = math.degrees(math.atan2(-dx, dy))
    out = []
    step = length / pieces
    for i in range(pieces):
        # Only the bar's two ends are capped: the joints between its lengths are hidden.
        sides = [f for f in faces if f in LONG or (f == "down" and i == 0) or (f == "up" and i == pieces - 1)]
        e = box((a[0] - width / 2, a[1] + i * step, z0), (a[0] + width / 2, a[1] + (i + 1) * step, z1), texture, faces=sides)
        for side in sides:
            sized(e, (regions or {}).get(side, FULL), sides=(side,))
        if abs(angle) > 1e-6:
            e["rotations"] = [{"origin": [a[0], a[1], 0.0], "axis": "z", "angle": round(angle, 4)}]
        out.append(e)
    return out


def polar(radius, degrees):
    return radius * math.cos(math.radians(degrees)), radius * math.sin(math.radians(degrees))


def quad(texture, points, normal, uvs):
    """A quad through four points (x, y, z pixels) with uvs (0 to 16), its corners put in the order that faces it toward
    `normal`, as tools/decor6_data.py's quads face."""
    (ax, ay, az), (bx, by, bz), (cx, cy, cz) = points[0], points[1], points[2]
    cross = ((by - ay) * (cz - az) - (bz - az) * (cy - ay), (bz - az) * (cx - ax) - (bx - ax) * (cz - az),
             (bx - ax) * (cy - ay) - (by - ay) * (cx - ax))
    if sum(c * n for c, n in zip(cross, normal)) < 0:
        points, uvs = points[::-1], uvs[::-1]
    return {"texture": texture, "normal": [round(n, 4) + 0.0 for n in normal],
            "vertices": [[round(p[0], 4), round(p[1], 4), round(p[2], 4), round(u / 16, 5), round(v / 16, 5)]
                         for p, (u, v) in zip(points, uvs)]}


def ring_prism(texture, radius, thick, z0, z1, angle, face_v, out_v, in_v):
    """One length of a ring `radius` pixels round (to its middle, at the length's middle), `thick` deep radially, from z0
    to z1, centred at `angle` degrees, as four quads mitred to meet the next lengths exactly at its ends (which are left
    open, hidden in them). The texture runs along the ring: its front and back show rows from `face_v` (the outer edge)
    inward, its outer and inner faces rows from `out_v` and `in_v` across the axle."""
    r_out, r_in = radius + thick / 2, radius - thick / 2
    ends = (angle - HALF_SEGMENT, angle + HALF_SEGMENT)
    stretch = 1 / math.cos(math.radians(HALF_SEGMENT))

    def at(r, a):
        x, y = polar(r * stretch, a)
        return x, y

    o0, o1, i0, i1 = at(r_out, ends[0]), at(r_out, ends[1]), at(r_in, ends[0]), at(r_in, ends[1])
    n = polar(1.0, angle)
    t = (-n[1], n[0])
    half = r_out * math.tan(math.radians(HALF_SEGMENT))

    def u(p):
        return (p[0] * t[0] + p[1] * t[1] + half) / (2 * half) * 16

    def v(p):
        return face_v + (r_out - (p[0] * n[0] + p[1] * n[1]))

    out = []
    for z, nz in ((z0, -1.0), (z1, 1.0)):
        pts = [o0, o1, i1, i0]
        out.append(quad(texture, [(x, y, z) for x, y in pts], (0.0, 0.0, nz), [(u(p), v(p)) for p in pts]))
    for (p0, p1), normal, v0 in (((o0, o1), (n[0], n[1], 0.0), out_v), ((i0, i1), (-n[0], -n[1], 0.0), in_v)):
        pts = [(p0[0], p0[1], z0), (p1[0], p1[1], z0), (p1[0], p1[1], z1), (p0[0], p0[1], z1)]
        out.append(quad(texture, pts, normal, [(u(p0), v0), (u(p1), v0), (u(p1), v0 + z1 - z0), (u(p0), v0 + z1 - z0)]))
    return out


# ---------------------------------------------------------------- the frame

def leg_x(y):
    """Where an A-frame's leg is across the wheel at height y (its right-hand leg; the left mirrors it)."""
    top = HUB - 4
    return FEET + (4 - FEET) * y / top


def frame():
    """Two A-frames either side of the wheel: lattice legs from footings on the ground to a bearing at the hub, two
    braces across, a cross of bracing between them (its two bars in different planes, so they never fight where they
    cross); beams along the axle tying the frames' feet; the axle."""
    e = []
    top = HUB - 4
    lattice = {"north": LATTICE_FRONT, "south": LATTICE_FRONT, "east": LATTICE_SIDE, "west": LATTICE_SIDE, "up": LATTICE_CAP,
               "down": LATTICE_CAP}
    for z in FRAMES:
        z0, z1 = z - 2, z + 2
        for side in (-1, 1):
            e += bar((side * FEET, 0), (side * 4, top), 5, z0, z1, "#lattice", pieces=10, regions=lattice)
            # The footing under each leg.
            footing = box((side * FEET - 7, 0, z - 6), (side * FEET + 7, 2, z + 6), "#footing")
            sized(footing, FOOTING_TOP + (14.0, 12.0), sides=("up", "down"))
            sized(footing, FOOTING_SIDE + (16.0, 15.0), sides=LONG)
            e.append(footing)
        for y in (44, 92):
            x = leg_x(y)
            e += bar((-x, y), (x, y), 3, z - 1.5, z + 1.5, "#steel")
        # A cross of bracing between the two braces.
        for side, depth in ((-1, 1.0), (1, 0.85)):
            e += bar((side * leg_x(44), 44), (-side * leg_x(92), 92), 2, z - depth, z + depth, "#steel")
        # The bearing at the top, and its brass cap (its back against the bearing left out).
        e.append(sized(box((-7, top - 4, z - 3.5), (7, HUB + 6, z + 3.5), "#steel")))
        cap = z - 4.5 if z < 0 else z + 3.5
        e.append(sized(box((-4, HUB - 4, cap), (4, HUB + 4, cap + 1), "#brass",
                           faces=[f for f in ALL if f != ("south" if z < 0 else "north")])))
    # Beams along the axle tying the two frames' feet, and the axle through the bearings.
    for side in (-1, 1):
        e.append(sized(box((side * FEET - 2, 0.5, FRAMES[0]), (side * FEET + 2, 4, FRAMES[1]), "#steel")))
    e.append(sized(box((-3, HUB - 3, FRAMES[0] - 4), (3, HUB + 3, FRAMES[1] + 4), "#axle")))
    return e


# ---------------------------------------------------------------- the wheel

def section_boxes():
    """The straight bars of one sixteenth of the wheel: for each rim the two ties trussing its rings together and a
    spoke from the hub, each in its own planes (INSET); their ends hidden inside the rings and the hub."""
    e = []
    a = 90.0
    joint = R_IN / math.cos(math.radians(HALF_SEGMENT))      # the inner ring's middle at its joints
    for z in RIMS:
        for lo, inset in ((a - HALF_SEGMENT, INSET["tie_low"]), (a + HALF_SEGMENT, INSET["tie_high"])):
            e += bar(polar(joint, lo), polar(R, a), 1.5, z - 1 + inset, z + 1 - inset, "#steel")
        spoke = a + 360.0 / SPOKES / 2
        e += bar(polar(10, spoke), polar(joint, spoke), 2, z - 1 + INSET["spoke"], z + 1 - INSET["spoke"], "#steel")
    return e


def section_quads():
    """One sixteenth of the turning wheel, at its top, for the client to draw sixteen times round: for each rim a length
    of the outer ring and of the inner one, mitred so the lengths meet exactly (they used to overlap a pixel at every
    joint), and the ties and spoke."""
    out = []
    for z in RIMS:
        out += ring_prism(WHEEL_TEXTURES["rim"], R, 3, z - 1, z + 1, 90.0, RIM_FACE, RIM_OUT, RIM_IN)
        out += ring_prism(WHEEL_TEXTURES["rim"], R_IN, 2, z - 1 + INSET["inner"], z + 1 - INSET["inner"], 90.0, INNER_FACE, INNER_EDGE,
                          INNER_EDGE)
    return out + drawn(metal(section_boxes()), WHEEL_TEXTURES)


def hub_quads():
    """The hub on each rim: a sixteen-sided disc (it used to be two squares turned an eighth from each other in the
    same planes, which fought), its sunburst face on the outside, deep enough to hold the spokes' ends."""
    out = []
    radius, sides = 11.0, 16
    corners = [polar(radius, 360.0 * k / sides) for k in range(sides)]
    for z in RIMS:
        outer, inner = (z - 2.5, z + 0.8) if z < 0 else (z + 2.5, z - 0.8)
        facing = -1.0 if z < 0 else 1.0
        for zf, nz, texture, (u0, v0, u1, v1) in ((outer, facing, WHEEL_TEXTURES["hub"], FULL),
                                                   (inner, -facing, WHEEL_TEXTURES["steel"], METAL_FILL)):
            for k in range(0, sides, 2):
                pts = [(0.0, 0.0), corners[k], corners[(k + 1) % sides], corners[(k + 2) % sides]]
                out.append(quad(texture, [(x, y, zf) for x, y in pts], (0.0, 0.0, nz),
                                [(u0 + (x + radius) / (2 * radius) * (u1 - u0), v0 + (radius - y) / (2 * radius) * (v1 - v0))
                                 for x, y in pts]))
        # Its rim, banded round it: lit along its outer edge, shaded along its inner.
        z0, z1 = min(outer, inner), max(outer, inner)
        su0, sv0, su1, sv1 = METAL_ACROSS_V[metal_width(z1 - z0)]
        for k in range(sides):
            p0, p1 = corners[k], corners[(k + 1) % sides]
            n = polar(1.0, 360.0 * (k + 0.5) / sides)
            edge = [(su0, sv0), (su1, sv0), (su1, sv1), (su0, sv1)] if z < 0 else [(su0, sv1), (su1, sv1), (su1, sv0), (su0, sv0)]
            out.append(quad(WHEEL_TEXTURES["steel"], [(p0[0], p0[1], z0), (p1[0], p1[1], z0), (p1[0], p1[1], z1), (p0[0], p0[1], z1)],
                            (n[0], n[1], 0.0), edge))
    return out


def pivot():
    """A car's pivot bar across both rims at the top of the wheel, a closed box whose capped ends are hidden 0.1 pixel
    inside the outer rings (it used to share their faces), with a brass collar either side of where the car hangs, sunk
    a little into the car's yoke; the client turns it to each car's place."""
    e = [box((-1.3, R - 1.3, RIMS[0] - 0.9), (1.3, R + 1.3, RIMS[1] + 0.9), "#steel")]
    for z0, z1 in ((-7.5, -6.4), (6.4, 7.5)):
        e.append(sized(box((-2, R - 2, z0), (2, R + 2, z1), "#brass")))
    return e


def lights():
    """The bulbs of one sixteenth of the wheel, as the section: on the outer face of each rim one on its outer ring, one
    on its inner ring and two along its spoke, each a closed box sunk a little into the steel it sits on."""
    e = []
    a = 90.0
    spoke = a + 360.0 / SPOKES / 2
    joint = R_IN / math.cos(math.radians(HALF_SEGMENT))
    for z in RIMS:
        z0, z1 = (z - 2, z - 0.6) if z < 0 else (z + 0.6, z + 2)
        # The inner ring's bulb is a little smaller, so its top and bottom stay clear of the ring's faces; the outer
        # ring's, where a car's pivot bar runs through it, keeps its sides 0.1 pixel inside the bar's.
        for (x, y), r in ((polar(R, a), 1.2), (polar(joint, spoke), 0.85), (polar(36, spoke), 1.0), (polar(60, spoke), 1.0)):
            e.append(with_uvs(box((x - r, y - r, z0), (x + r, y + r, z1), "#bulb"), {s: FULL for s in ALL}))
    return e


# ---------------------------------------------------------------- the cars

def car():
    """A car hanging from its pivot: a yoke and hanger down to a striped canopy with a scalloped valance, brass posts at
    its corners, and the car below, a painted tub with a tufted bench across its back for two, a grab bar across its
    front, on a plank floor. The front (low z) wall is lower, to see out over. Faces pressed against another box are
    left out, and every face samples its texture at about four texels to a pixel."""
    e = []
    plain = (0.0, CAR_PLAIN, 16.0, 16.0)
    # The yoke round the pivot bar and the hanger (set into the yoke and the canopy, in planes of its own).
    e.append(sized(box((-2.5, -2.5, -6.5), (2.5, 2.5, -5), "#brass")))
    e.append(sized(box((-2.5, -2.5, 5), (2.5, 2.5, 6.5), "#brass")))
    e.append(sized(box((-1, -5.5, -6.35), (1, -2, -5.15), "#steel", faces=LONG)))
    e.append(sized(box((-1, -5.5, 5.15), (1, -2, 6.35), "#steel", faces=LONG)))
    # The canopy: a striped roof, peaked in the middle (its underside on the roof left out).
    e.append(sized(box((-13, -7, -10.5), (13, -6, 10.5), "#canopy")))
    # The peak shows the same stripes as the roof under it, at the same scale, so they line up.
    across, along = (13 - 10) / 26 * 16, (10.5 - 8) / 21 * 16
    e.append(with_uvs(box((-10, -6, -8), (10, -5, 8), "#canopy", faces=("north", "south", "east", "west", "up")), {
        "up": (across, along, 16 - across, 16 - along), "north": (across, 0, 16 - across, 1), "south": (across, 0, 16 - across, 1),
        "east": (along, 0, 16 - along, 1), "west": (along, 0, 16 - along, 1)}))
    # The valance hanging round the canopy: one face each, its outward one (26.3 draws cut-out quads from both sides).
    e.append(sized(box((-13, -10, -10.5), (13, -7, -10.5), "#valance", faces=("north",)), (0.0, 0.0, 16.0, 3.0)))
    e.append(sized(box((-13, -10, 10.5), (13, -7, 10.5), "#valance", faces=("south",)), (0.0, 0.0, 16.0, 3.0)))
    e.append(sized(box((-13, -10, -10.5), (-13, -7, 10.5), "#valance", faces=("west",)), (0.0, 0.0, 16.0, 3.0)))
    e.append(sized(box((13, -10, -10.5), (13, -7, 10.5), "#valance", faces=("east",)), (0.0, 0.0, 16.0, 3.0)))
    # Brass posts at the corners, from the tub's rim to the canopy, tall enough for a rider sitting up under it.
    for x in (-11.5, 10.5):
        for z in (-8.5, 7.5):
            e.append(sized(box((x, -27, z), (x + 1, -7, z + 1), "#brass", faces=("north", "south", "east", "west", "down"))))
    # The tub, hung low so a rider sits with their head under the canopy: floor, back wall, side walls and the lower
    # front wall, the painted panels outside, plain paint inside.
    e.append(sized(box((-12, -36, -9), (12, -34, 9), "#steel", textures={"up": "#floor"})))
    back = box((-12, -34, 8), (12, -26, 9), "#car", faces=("north", "south", "east", "west"))
    sized(back, (0.0, CAR_PANEL, 16.0, CAR_FRONT), sides=("south",))
    e.append(sized(back, plain, sides=("north", "east", "west")))
    for x0, outside in ((-12, "west"), (11, "east")):
        wall = box((x0, -34, -9), (x0 + 1, -26, 8), "#car", faces=("north", "east", "west"))
        sized(wall, (0.0, CAR_PANEL, 16.0, CAR_FRONT), sides=(outside,))
        e.append(sized(wall, plain, sides=[f for f in ("north", "east", "west") if f != outside]))
    front = box((-11, -34, -9), (11, -29, -8), "#car", faces=("north", "south"))
    sized(front, (0.0, CAR_FRONT, 16.0, CAR_PLAIN), sides=("north",))
    e.append(sized(front, plain, sides=("south",)))
    # A brass rim round the tub's top edge, capping the walls (whose tops are left out under it).
    e.append(sized(box((-12.25, -26, 8), (12.25, -25.5, 9.25), "#brass")))
    e.append(sized(box((-12.25, -26, -9.25), (-11, -25.5, 8), "#brass", faces=("north", "east", "west", "up", "down"))))
    e.append(sized(box((11, -26, -9.25), (12.25, -25.5, 8), "#brass", faces=("north", "east", "west", "up", "down"))))
    e.append(sized(box((-11, -29, -9.25), (11, -28.5, -8), "#brass")))
    # The bench and its back, and the grab bar.
    e.append(sized(box((-11, -34, 2), (11, -28, 8), "#seat", faces=("north", "up"))))
    e.append(sized(box((-11, -28, 6.5), (11, -20, 8), "#seat", faces=("north", "south", "east", "west", "up"))))
    e.append(sized(box((-10, -25, -7.5), (10, -24, -6.5), "#brass")))
    for x in (-10, 9):
        e.append(sized(box((x, -29, -7.5), (x + 1, -25, -6.5), "#brass", faces=("north", "south", "east", "west", "down"))))
    return e


def drawn(elements, textures):
    out = quads(elements, textures)
    for quad_ in out:
        if quad_["texture"] in CUTOUT:
            quad_["cutout"] = True
    return out


def wheel_quads():
    out = {"ferris_wheel_frame": drawn(metal(frame()), FRAME_TEXTURES), "ferris_wheel_section": section_quads(),
           "ferris_wheel_hub": hub_quads(), "ferris_wheel_pivot": drawn(metal(pivot()), WHEEL_TEXTURES),
           "ferris_wheel_lights": drawn(lights(), WHEEL_TEXTURES)}
    for colour in CAR_COLOURS:
        out[f"ferris_wheel_car_{colour}"] = drawn(metal(car()), car_textures(colour))
    return out


# ---------------------------------------------------------------- the booth

def booth():
    """The booth: a loading platform with a plank deck, its panelled sides painted, and the operator's controls on its
    front: a brass lever leaning in its slot, a speed gauge and two buttons. Its brass and steel are its own textures
    (the wheel's are laid out for the wheel's faces; a block model samples by position)."""
    t = {"side": "ferris_wheel_booth_side", "front": "ferris_wheel_booth_front", "deck": "ferris_wheel_booth_deck",
         "brass": "ferris_wheel_booth_brass", "steel": "ferris_wheel_booth_steel", "gauge": "ferris_wheel_gauge"}
    e = [box((0, 0, 0), (16, 13, 16), "#side", textures={"north": "#front", "up": "#deck"}),
         box((-0.5, 13, -0.5), (16.5, 16, 16.5), "#deck", textures={"down": "#steel"}),
         # The lever in its slot, leaning forward, and its knob.
         box((2.5, 6, -0.5), (5.5, 7, 0), "#steel"),
         box((3.5, 6.5, -1.5), (4.5, 11.5, -0.5), "#brass", rotation={"origin": [4, 6.5, -1], "axis": "x", "angle": -22.5}),
         box((3, 11, -2.5), (5, 13, -0.5), "#brass", rotation={"origin": [4, 6.5, -1], "axis": "x", "angle": -22.5}),
         # The gauge, and two buttons under it.
         box((9, 7, -0.75), (14, 12, 0), "#steel", textures={"north": "#gauge"}, uvs={"north": FULL}),
         box((9.5, 4, -0.75), (11, 5.5, 0), "#brass"), box((12, 4, -0.75), (13.5, 5.5, 0), "#brass")]
    return block_model(t, e, "ferris_wheel_booth_side")


def assets(root, write, lang):
    name = FERRIS_WHEEL["block"]
    write(root / "models" / "block" / f"{name}.json", booth())
    write(root / "blockstates" / f"{name}.json", {"variants": {f"facing={f}": turned(rid(f"block/{name}"), f) for f in HORIZONTAL}})
    flat_item(root, write, name)
    # One quad to a line: the file is large, and this keeps it a quarter the size of the indented form.
    path = root / "ferris_wheel_quads.json"
    path.write_text("{\n" + ",\n".join(json.dumps(name) + ": [\n" + ",\n".join(json.dumps(q, separators=(",", ":")) for q in qs) + "\n]"
                                         for name, qs in wheel_quads().items()) + "\n}\n", encoding="utf-8")
    lang[f"block.{MOD}.{name}"] = FERRIS_WHEEL["display"]
    lang[f"entity.{MOD}.{FERRIS_WHEEL['entity']}"] = FERRIS_WHEEL["entity_display"]
    lang[f"message.{MOD}.ferris_wheel.no_room"] = "The Ferris Wheel needs a clear space %s blocks across, %s high and %s deep"
    lang[f"message.{MOD}.ferris_wheel.full"] = "The car at the bottom is full: wait for the next"
    lang[f"message.{MOD}.ferris_wheel.jammed"] = "The Ferris Wheel is jammed: something is in a car's way"


def loot(out, write):
    write(out / f"{FERRIS_WHEEL['block']}.json", self_drop(FERRIS_WHEEL["block"]))


def tags(tags):
    tags.add("block", "minecraft:mineable/pickaxe", rid(FERRIS_WHEEL["block"]))
