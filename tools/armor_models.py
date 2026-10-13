"""3D worn armor: boxes of any size, at any angle, on the body's bones, exported to worn_models.json.

This generalizes the exosuit's worn parts (tools/exosuit.py, drawn by the client's worn-model layer) so an armor piece
is not limited to vanilla's flat armor shape: a helm wider than the head with a tilted crest and fins, layered
pauldrons, flared cuffs, a skirt of lames. A model is a dict of bones; each bone holds parts (boxes). Every part has an
origin, a size, a pivot, a rotation in degrees (any angle), an inflation, a mirror flag and a paint spec. The exporter
bakes the rotation into textured quads (the QuadModel format the client already reads) and lays every face out in one
atlas texture per set, one texel per model pixel, which tools/armor_paint.py paints.

Space (as tools/exosuit.py): each bone's own space, in model pixels, before the bone moves. x points to the model's
LEFT (the right arm and leg are at -x), y points DOWN from the bone's pivot, z points to the BACK (the face is at -z).

    bone        pivot (body space)   its own box in bone space
    head        (0, 0, 0)            x -4..4,  y -8..0,  z -4..4
    body        (0, 0, 0)            x -4..4,  y 0..12,  z -2..2
    right_arm   (-5, 2, 0)           x -3..1,  y -2..10, z -2..2
    left_arm    (5, 2, 0)            x -1..3,  y -2..10, z -2..2
    right_leg   (-1.9, 12, 0)        x -2..2,  y 0..12,  z -2..2
    left_leg    (1.9, 12, 0)         x -2..2,  y 0..12,  z -2..2

Faces are named for where they face on an unturned box: top (-y), bottom (+y), right (-x, the model's right), front
(-z), left (+x) and back (+z). Vanilla's names are accepted too: down = top, up = bottom, west = right, north = front,
east = left, south = back. On a mirrored part a face name names the original's face it shows, so "right" on a mirrored
copy of a right-arm plate is its outer face, now on the left.

Rotation: `rotation=(x, y, z)` degrees turn the part about `pivot`, x first, then y, then z, right-handed in bone space:
the order and sense of a ModelPart's xRot, yRot, zRot. So:
  - a positive x turn swings a hanging plate's bottom BACK (+z) (and tips a helm's face down);
  - a positive y turn swings the front (-z) toward the model's RIGHT (-x);
  - a positive z turn swings the bottom (+y) toward the model's RIGHT (-x).
`turns` adds more turns after that, each (axis, degrees, pivot), with the axis "x", "y", "z" or any vector: helpers such
as hinge() append these, so a plate can be rolled, then pitched about one of its own edges.

Use:
    import armor_models as am
    from armor_paint import P, STEEL
    helm = am.around("helm", "head", 1.25, paint=P("plate"))
    crest = am.diamond("crest", centre=(0, -7, -5.25), side=8, thickness=1.5, pitch=35, paint=P("chevron", corner="bl"))
    knight = am.ArmorSet("steel_knight", STEEL, {"steel_helmet": {"head": [helm, crest]}, ...})
    am.set_quads(knight)  # {"steel_helmet_head": [quads...]}, for worn_models.json
    am.problems(knight)   # z-fighting, skin clearance, budgets
Register a set by adding its module to SET_MODULES (the module's SETS list); generate_material_data.py then writes its
quads into worn_models.json and generate_textures.py paints its atlas (tools/armor_paint.py). Preview it with
tools/armor_preview.py; tools/armor_smoke.py is the toolkit's own test.

worn_models.json: each set adds one entry per worn item and bone, keyed "<item>_<bone>" (the item's id path, e.g.
"steel_chestplate_right_arm"), a list of quads {"texture", "normal", "vertices": [[x, y, z, u, v] x 4]}. As for every
entry in that file, positions are stored half-turned about z ((x, y, z) -> (-x, -y, z)): the client puts the pose in the
bone's space (ModelPart.translateAndRotate), turns it 180 degrees about z and draws the quads.
"""
import math
from dataclasses import dataclass, field, replace

MOD = "jugcraft"
BONES = ("head", "body", "right_arm", "left_arm", "right_leg", "left_leg")
# Each bone's own box (wide arms), bone space: the skin every worn part must clear.
BASE = {"head": ((-4, -8, -4), (4, 0, 4)), "body": ((-4, 0, -2), (4, 12, 2)),
        "right_arm": ((-3, -2, -2), (1, 10, 2)), "left_arm": ((-1, -2, -2), (3, 10, 2)),
        "right_leg": ((-2, 0, -2), (2, 12, 2)), "left_leg": ((-2, 0, -2), (2, 12, 2))}
# Where each bone's pivot sits in body space, standing (HumanoidModel).
PIVOTS = {"head": (0, 0, 0), "body": (0, 0, 0), "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0),
          "right_leg": (-1.9, 12, 0), "left_leg": (1.9, 12, 0)}
FACES = ("top", "bottom", "right", "front", "left", "back")
ALIASES = {"down": "top", "up": "bottom", "west": "right", "north": "front", "east": "left", "south": "back"}
GROUPS = {"all": FACES, "sides": ("right", "front", "left", "back"), "ends": ("top", "bottom")}
NORMALS = {"top": (0, -1, 0), "bottom": (0, 1, 0), "right": (-1, 0, 0), "front": (0, 0, -1), "left": (1, 0, 0),
           "back": (0, 0, 1)}
AXES = {"x": (1, 0, 0), "y": (0, 1, 0), "z": (0, 0, 1)}
TEXTURE_DIR = "entity/equipment/3d"   # a set's atlas: textures/entity/equipment/3d/<set>.png (in no atlas)

# Quad budget (each box is 6 quads before skipped faces): per worn entry, per piece and per set.
ENTRY_CAP = 200
PIECE_CAPS = {"helmet": 220, "chestplate": 320, "leggings": 260, "boots": 100}
SET_CAP = 900
SET_TARGET = 600
# The skin and its outer layer on each bone (the hat +0.5 on the head; jacket, sleeves and pants +0.25) and vanilla
# armor (1.0 helmet/chest/boots, 0.5 leggings): no part face over the bone may lie this close to those shells, or the
# two flicker (z-fight), nor cross one (the wearer shows through). Coplanar faces in a set need this gap too.
SKIN_SHELLS = {bone: (0.0, 0.5) if bone == "head" else (0.0, 0.25) for bone in BONES}
SKIN_GAP = 0.15
COPLANAR_GAP = 0.1
EPS = 1e-9          # float slack: a gap of exactly COPLANAR_GAP, read back from a Blockbench project, still passes
VANILLA_SHELLS = {"chestplate": {"body": 0.5}, "leggings": {b: 1.0 for b in ("body", "right_leg", "left_leg")},
                  "boots": {"right_leg": 0.5, "left_leg": 0.5}}
# Parts beyond these (body space, standing) may pop out at the screen edge (the entity's cull box): warned, not refused.
REACH_X, REACH_UP, FLOOR = 12.0, -14.0, 25.0   # FLOOR: 1 px into the ground, as vanilla boots

# Modules that define armor sets, each with a SETS list.
SET_MODULES = ("knight_armor",         # the knight armor: the owner's steel design and its bronze variant
               "bloodthorn_armor",     # Bloodthorn Armor: the owner's crimson design
               "white_diamond_armor",  # Reforged White Diamond: the owner's icy design
               "hades_armor",          # Hades Armor: the owner's underworld design
               "sunset_gem_armor",     # Sunset Gem: the owner's sunset design
               "pharaoh_armor",        # Pharaoh: the owner's golden design
               "dread_knight_armor",   # Dread Knight: the owner's dark crowned knight
               "valkyrie_armor",       # Valkyrie: the owner's white and gold winged design
               "wayfarer_armor",       # Wayfarer: the owner's blue hooded cloak
               "spartan_armor",        # Spartan: the owner's gold plumed design
               "berserker_armor",      # Berserker: the owner's horned red and white design
               "crusader_armor",       # Paladin and Templar: the owner's two crusader knights
               "sentinel_armor",       # Sentinel: the owner's gold-and-black knight
               "frost_knight_armor",   # Frost Knight: the owner's white knight crowned with ice
               "wight_king_armor",     # Wight King: the owner's slate knight crowned with icicles and antlers
               "reaper_armor",         # Reaper: the owner's hooded reaper
               "banana_armor",         # Banana: the owner's banana costume
               "scarab_armor")         # Scarab: the owner's gold-and-lapis Egyptian set


def face_name(name):
    name = ALIASES.get(name, name)
    if name not in FACES:
        raise ValueError(f"unknown face {name!r}: use one of {FACES} (or {tuple(ALIASES)})")
    return name


def faces_of(names):
    """A face name, group ("all", "sides", "ends") or several of them -> a frozenset of face names."""
    if isinstance(names, str):
        names = [names]
    out = set()
    for name in names:
        out.update(GROUPS[name] if name in GROUPS else [face_name(name)])
    return frozenset(out)


# ---------------------------------------------------------------- small linear algebra (pure Python: deterministic)
def _vec(v):
    return tuple(float(c) for c in (AXES[v] if isinstance(v, str) else v))


def _add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def _sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


def _scale(a, k):
    return tuple(x * k for x in a)


def _dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def _unit(a):
    n = math.sqrt(_dot(a, a))
    if n < 1e-12:
        raise ValueError("zero-length axis")
    return _scale(a, 1 / n)


def _mul(p, q):
    return tuple(tuple(sum(p[i][k] * q[k][j] for k in range(3)) for j in range(3)) for i in range(3))


def _apply(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))


def _cos_sin(deg):
    """Exact values on multiples of 90 degrees, so square turns leave square boxes."""
    if float(deg) % 90 == 0:
        return {0: (1.0, 0.0), 1: (0.0, 1.0), 2: (-1.0, 0.0), 3: (0.0, -1.0)}[int(float(deg) // 90) % 4]
    r = math.radians(deg)
    return math.cos(r), math.sin(r)


def axis_matrix(axis, deg):
    """Right-handed turn of `deg` degrees about `axis` ("x", "y", "z" or a vector)."""
    x, y, z = _unit(_vec(axis))
    c, s = _cos_sin(deg)
    k = 1 - c
    return ((c + x * x * k, x * y * k - z * s, x * z * k + y * s),
            (y * x * k + z * s, c + y * y * k, y * z * k - x * s),
            (z * x * k - y * s, z * y * k + x * s, c + z * z * k))


def euler_matrix(rotation):
    """ModelPart order: x first, then y, then z (R = Rz Ry Rx)."""
    rx, ry, rz = rotation
    return _mul(axis_matrix("z", rz), _mul(axis_matrix("y", ry), axis_matrix("x", rx)))


# ---------------------------------------------------------------- parts
@dataclass(frozen=True)
class Part:
    """One box on a bone. origin is its min corner and size its extent, in bone-space pixels, before inflation and
    rotation; it may be any size and stick out anywhere. paint says what tools/armor_paint.py draws on it: one spec for
    the whole box or a dict per face (see armor_paint). skip lists faces not drawn (pressed against another plate,
    inside the body). net names another part whose texture region this one shows (mirrored copies share it); mirror
    draws the region mirrored, as vanilla's mirror flag. cutout lets the paint leave see-through texels. uvs, when
    given, are the part's own texels instead of a net: ((face, ((u, v) x 4)), ...) in atlas texels on the face's
    corners in part_faces' order, faces left out not drawn (a part read from a Blockbench project, tools/bbmodel.py)."""
    name: str
    origin: tuple
    size: tuple
    paint: object = "plate"
    pivot: tuple = (0.0, 0.0, 0.0)
    rotation: tuple = (0.0, 0.0, 0.0)
    turns: tuple = ()
    inflate: float = 0.0
    mirror: bool = False
    skip: frozenset = frozenset()
    net: str = ""
    cutout: bool = False
    uvs: tuple = ()

    def __post_init__(self):
        if not self.name or "/" in self.name:
            raise ValueError(f"bad part name {self.name!r}")
        for key in ("origin", "size", "pivot", "rotation"):
            value = getattr(self, key)
            if len(value) != 3:
                raise ValueError(f"{self.name}: {key} needs 3 numbers")
            object.__setattr__(self, key, tuple(float(c) for c in value))
        if min(self.size) <= 0:
            raise ValueError(f"{self.name}: size must be positive, got {self.size}")
        object.__setattr__(self, "skip", faces_of(self.skip) if self.skip else frozenset())
        object.__setattr__(self, "turns", tuple((_vec(a), float(d), tuple(float(c) for c in p)) for a, d, p in self.turns))

    @property
    def key(self):
        """The atlas region (net) this part reads."""
        return self.net or self.name

    @property
    def to(self):
        return _add(self.origin, self.size)

    @property
    def centre(self):
        return _add(self.origin, _scale(self.size, 0.5))


def box(name, origin, size, **options):
    """A part from its min corner and size (Blockbench-style)."""
    return Part(name, origin, size, **options)


def span(name, frm, to, **options):
    """A part between two opposite corners."""
    lo = tuple(min(a, b) for a, b in zip(frm, to))
    hi = tuple(max(a, b) for a, b in zip(frm, to))
    return Part(name, lo, _sub(hi, lo), **options)


def around(name, bone, gap, x=None, y=None, z=None, **options):
    """A shell box round a bone's own box, `gap` pixels clear of it on every side; x, y or z = (lo, hi) bounds that
    axis instead (a band round the arm: y=(4, 10.5)). The net is the shell's real size, so its texels stay one pixel."""
    lo, hi = BASE[bone]
    bounds = []
    for i, given in enumerate((x, y, z)):
        bounds.append(tuple(given) if given is not None else (lo[i] - gap, hi[i] + gap))
    return span(name, tuple(b[0] for b in bounds), tuple(b[1] for b in bounds), **options)


def transform(part):
    """(R, t) with p' = R p + t: the part's rotation about its pivot, then its turns, in bone space."""
    r = euler_matrix(part.rotation)
    t = _sub(part.pivot, _apply(r, part.pivot))
    for axis, deg, pivot in part.turns:
        m = axis_matrix(axis, deg)
        r = _mul(m, r)
        t = _add(_apply(m, t), _sub(pivot, _apply(m, pivot)))
    return r, t


def place(part, point):
    r, t = transform(part)
    return _add(_apply(r, point), t)


def corners(part):
    """The 8 corners of the placed (inflated, turned) box in bone space."""
    g = part.inflate
    lo, hi = _sub(part.origin, (g, g, g)), _add(part.to, (g, g, g))
    return [place(part, (x, y, z)) for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])]


def _swap_sides(name):
    out = name.replace("right", "\0").replace("left", "right").replace("\0", "left")
    return out if out != name else f"{name}_mirror"


def mirror(part, name=None):
    """The same part on the other side of x = 0 (left from right): position, pivot and turns reflected, its texture
    region shared and drawn mirrored, so a plate's pattern points the same way on both sides (toward the centre line, or
    outward). Mirrors right-arm parts onto the left arm and right-leg parts onto the left leg too: their bone spaces are
    reflections of each other."""
    ox = -(part.origin[0] + part.size[0])
    turns = tuple(((-a[0], a[1], a[2]), -d, (-p[0], p[1], p[2])) for a, d, p in part.turns)
    return replace(part, name=name or _swap_sides(part.name), origin=(ox, part.origin[1], part.origin[2]),
                   pivot=(-part.pivot[0], part.pivot[1], part.pivot[2]),
                   rotation=(part.rotation[0], -part.rotation[1], -part.rotation[2]), turns=turns,
                   mirror=not part.mirror, net=part.key)


def pair(part, name=None):
    """[part, its mirror image]: a symmetric pair on one bone (helm fins, cheek plates)."""
    return [part, mirror(part, name)]


def mirror_all(parts):
    """Every part mirrored: the left arm's or leg's parts from the right one's."""
    return [mirror(p) for p in parts]


def turned(part, axis, degrees, pivot):
    """The part with one more turn about `pivot` (bone space), after its current placement."""
    return replace(part, turns=part.turns + ((axis, degrees, pivot),))


def hinge(part, edge, degrees, toward=None, axis=None):
    """The part tilted `degrees` about one edge of its face `edge` (top, bottom, front, back, right, left), like a plate
    on a hinge: the far side swings `toward` (a face name or vector, bone space) for positive degrees. By default a plate
    swings outward: along its thinnest axis, away from the bone's origin. `axis` ("x", "y", "z") picks the hinge line's
    direction when the part is not a thin plate. Works on a part already turned: the hinge follows it."""
    edge = face_name(edge)
    n = NORMALS[edge]
    edge_axis = [i for i in range(3) if n[i]][0]
    if axis is not None:
        hinge_axis = "xyz".index(axis)
        if hinge_axis == edge_axis:
            raise ValueError(f"{part.name}: the hinge line cannot run across the {edge} face")
        thin = 3 - edge_axis - hinge_axis
    else:
        others = [i for i in range(3) if i != edge_axis]
        thin = min(others, key=lambda i: (part.size[i], i))
        if abs(part.size[others[0]] - part.size[others[1]]) < 1e-9:
            raise ValueError(f"{part.name}: not a thin plate; pass axis= for the hinge line")
        hinge_axis = 3 - edge_axis - thin
    c = list(part.centre)
    c[edge_axis] = part.origin[edge_axis] + (part.size[edge_axis] if n[edge_axis] > 0 else 0)
    far = list(part.centre)
    far[edge_axis] = part.origin[edge_axis] + (0 if n[edge_axis] > 0 else part.size[edge_axis])
    r, t = transform(part)
    pivot = _add(_apply(r, tuple(c)), t)
    line = _apply(r, AXES["xyz"[hinge_axis]])
    if toward is None:
        thin_dir = _apply(r, AXES["xyz"[thin]])
        side = _dot(_add(_apply(r, part.centre), t), thin_dir)
        if abs(side) < 1e-6:
            raise ValueError(f"{part.name}: plate centred on the bone's origin; pass toward=")
        direction = _scale(thin_dir, 1 if side > 0 else -1)
    else:
        direction = NORMALS[face_name(toward)] if isinstance(toward, str) else _vec(toward)
    far_now = _add(_apply(r, tuple(far)), t)
    swung = _add(_apply(axis_matrix(line, degrees), _sub(far_now, pivot)), pivot)
    sign = 1 if _dot(_sub(swung, far_now), direction) >= 0 else -1
    return turned(part, line, sign * degrees, pivot)


def lames(name, origin, size, count, step, grow=(0.0, 0.0, 0.0), flare=0.0, flare_step=0.0, edge="top", toward=None,
          paint="lames", **options):
    """A stack of `count` overlapping plates (lames): plate i starts at origin + i * step and is size + i * grow, grown
    evenly in x and z about the same centre line. Each is hinged at `edge` and swung by flare + i * flare_step degrees,
    outward or `toward` a face (see hinge). paint may be one spec, a list (one per lame) or a function of the lame's
    index. Named name_0, name_1..."""
    out = []
    for i in range(count):
        s = _add(size, _scale(grow, i))
        o = _add(origin, _scale(step, i))
        o = (o[0] - grow[0] * i / 2, o[1], o[2] - grow[2] * i / 2)
        spec = paint(i) if callable(paint) else paint[i] if isinstance(paint, list) else paint
        part = Part(f"{name}_{i}", o, s, paint=spec, **options)
        angle = flare + flare_step * i
        out.append(hinge(part, edge, angle, toward) if angle else part)
    return out


def diamond(name, centre, side, thickness, pitch=0.0, roll=-45.0, paint="chevron", **options):
    """A square plate stood on one corner (rolled `roll` degrees) facing front, centred at `centre`, then pitched about
    its horizontal diagonal: positive pitch juts the bottom corner forward and leans the top one back (a crest or a
    visor peak, a couter, a toe cap). The front face's bottom-left texel corner becomes the bottom corner."""
    cx, cy, cz = centre
    part = Part(name, (cx - side / 2, cy - side / 2, cz - thickness / 2), (side, side, thickness), paint=paint,
                **options)
    part = turned(part, "z", roll, centre)
    return turned(part, "x", -pitch, centre) if pitch else part


def chevron(name, apex, length, width, thickness, angle=45.0, pitch=0.0, paint="chevron", **options):
    """A V of two bars meeting at `apex` (the V's bottom point, on the bars' front face plane z = apex z), each `length`
    long and `width` tall, rising at `angle` degrees above horizontal; positive pitch tilts the V forward about the bars'
    top ends so the point juts out (-z). Returns [right bar, left bar]: the left mirrors the right. The two bars lie in
    one plane where they cross above the apex: move one a little nearer or further (knight_armor.vee does), or
    problems() reports them."""
    ax, ay, az = apex
    bar = Part(f"{name}_right", (ax - length, ay - width, az), (length, width, thickness), paint=paint, **options)
    bar = turned(bar, "z", angle, (ax, ay, az))
    if pitch:
        top = ay - length * math.sin(math.radians(angle)) - width * math.cos(math.radians(angle))
        bar = turned(bar, "x", -pitch, (ax, top, az))
    return pair(bar, f"{name}_left")


def rivets(name, start, step, count, size=(0.75, 0.75, 0.5), face="front", paint=("solid", {"tone": "light"}),
           **options):
    """A row of `count` small boxes (rivets or bolts) from `start` (the first one's centre) every `step`, proud of a
    plate's `face`: the face they sit on is left out (it is against the plate)."""
    n = NORMALS[face_name(face)]
    opposite = {"top": "bottom", "bottom": "top", "right": "left", "left": "right", "front": "back",
                "back": "front"}[face_name(face)]
    skip = faces_of(opposite) | faces_of(options.pop("skip", ()))
    out = []
    for i in range(count):
        c = _add(start, _scale(step, i))
        origin = tuple(c[k] - size[k] / 2 if not n[k] else (c[k] if n[k] > 0 else c[k] - size[k]) for k in range(3))
        out.append(Part(f"{name}_{i}", origin, size, paint=paint, skip=skip, **options))
    return out


# ---------------------------------------------------------------- sets
@dataclass
class ArmorSet:
    """One look: the worn models of its items, one atlas texture and one palette. pieces maps an item id path
    ("steel_helmet") to {bone: [Part]}; any piece may put parts on any bone. Part names are unique in the set.
    image: an atlas of its own (a PIL image, as a Blockbench project carries it) instead of one armor_paint paints;
    its parts then give their texels (Part.uvs) and atlas_size is the image's size."""
    name: str
    palette: dict
    pieces: dict
    width: int = 128
    density: int = 1
    pad: int = 1
    texture: str = field(default="")
    image: object = None
    atlas_size: tuple = ()

    def __post_init__(self):
        if not self.texture:
            self.texture = f"{TEXTURE_DIR}/{self.name}"

    def parts(self):
        """(item, bone, part) for every part, in definition order."""
        for item, bones in self.pieces.items():
            for bone, parts in bones.items():
                for part in parts:
                    yield item, bone, part


def sets():
    """Every registered ArmorSet (SET_MODULES)."""
    import importlib
    out = []
    for module in SET_MODULES:
        out.extend(importlib.import_module(module).SETS)
    return out


def net_size(part, density=1):
    """(w, h, d) of the part's net in texels: its uninflated size, rounded up."""
    return tuple(max(1, math.ceil(part.size[i] * density - 1e-6)) for i in range(3))


def face_rects(w, h, d):
    """Each face's texel rect (u0, v0, u1, v1) in a net of (w, h, d) at (0, 0): vanilla's box layout. The top and
    bottom faces sit in a row above the right side, front, left side and back."""
    return {"top": (d, 0, d + w, d), "bottom": (d + w, 0, d + 2 * w, d), "right": (0, d, d, d + h),
            "front": (d, d, d + w, d + h), "left": (d + w, d, 2 * d + w, d + h), "back": (2 * d + w, d, 2 * d + 2 * w, d + h)}


def check(s):
    """Raises ValueError for a set the exporter cannot write: an unknown bone, a repeated part name, a shared net of
    another size, a net wider than the atlas."""
    names, nets = set(), {}
    for item, bone, part in s.parts():
        if bone not in BONES:
            raise ValueError(f"{s.name}: {item} uses unknown bone {bone!r}")
        if part.name in names:
            raise ValueError(f"{s.name}: part name {part.name!r} is used twice")
        names.add(part.name)
        if part.uvs:
            if not s.atlas_size:
                raise ValueError(f"{s.name}: {part.name} gives its own texels, but the set has no atlas_size")
            continue
        size = net_size(part, s.density)
        if nets.setdefault(part.key, size) != size:
            raise ValueError(f"{s.name}: {part.name} shares net {part.key} but its size is {size}, not {nets[part.key]}")
        w, h, d = size
        if 2 * (d + w) + s.pad > s.width:
            raise ValueError(f"{s.name}: {part.name}'s net is {2 * (d + w)} texels wide; the atlas is {s.width}")
    return nets


def layout(s):
    """({net: (u, v, w, h, d)}, (width, height)): every distinct net shelf-packed into the atlas, tallest first (then
    widest, then by name), `pad` texels apart; the height is a power of two. Deterministic: the exporter and the painter
    both call it, so the quads' UVs always match the painted atlas."""
    sizes = check(s)
    order = sorted(sizes, key=lambda k: (-(sizes[k][2] + sizes[k][1]), -(sizes[k][2] + sizes[k][0]), k))
    out, x, y, row = {}, 0, 0, 0
    for key in order:
        w, h, d = sizes[key]
        nw, nh = 2 * (d + w) + s.pad, d + h + s.pad
        if x + nw > s.width:
            x, y, row = 0, y + row, 0
        out[key] = (x, y, w, h, d)
        x, row = x + nw, max(row, nh)
    if s.atlas_size:
        if out:
            raise ValueError(f"{s.name}: an atlas of its own (atlas_size) holds no packed nets")
        return out, tuple(s.atlas_size)
    height = 1
    while height < y + row:
        height *= 2
    return out, (s.width, height)


# Each face's corners (indices into the box's corners 1-8) and the UV corner each takes, as vanilla ModelPart.Cube:
# counter-clockwise seen from outside, texture top at the visual top, the front's texture-left at the model's right.
_CORNERS = {"top": (6, 5, 1, 2), "bottom": (3, 4, 8, 7), "right": (1, 5, 8, 4), "front": (2, 1, 4, 3),
            "left": (6, 2, 3, 7), "back": (5, 6, 7, 8)}


def part_faces(part, density=1):
    """The part's drawn faces in bone space: [(face, 4 points, 4 (u, v) texel coordinates in its net, normal)]. A part
    with texels of its own (uvs) gives them, in atlas texels."""
    own = dict(part.uvs)
    w, h, d = net_size(part, density)
    rects = face_rects(w, h, d)
    g = part.inflate
    x0, y0, z0 = (c - g for c in part.origin)
    x1, y1, z1 = (c + g for c in part.to)
    if part.mirror:   # vanilla: swap the x sides, then reverse each face's corners (keeps the winding)
        x0, x1 = x1, x0
    c = {1: (x0, y0, z0), 2: (x1, y0, z0), 3: (x1, y1, z0), 4: (x0, y1, z0),
         5: (x0, y0, z1), 6: (x1, y0, z1), 7: (x1, y1, z1), 8: (x0, y1, z1)}
    r, t = transform(part)
    out = []
    for face in FACES:
        if face in part.skip or (part.uvs and face not in own):
            continue
        u1, v1, u2, v2 = rects[face]
        if face == "bottom":
            v1, v2 = v2, v1
        pts = [c[i] for i in _CORNERS[face]]
        uvs = list(own[face]) if part.uvs else [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
        normal = NORMALS[face]
        if part.mirror:
            pts, uvs = pts[::-1], uvs[::-1]
            normal = (-normal[0], normal[1], normal[2])
        out.append((face, [_add(_apply(r, p), t) for p in pts], uvs, _apply(r, normal)))
    return out


def _r(value, digits):
    return round(value, digits) + 0.0   # + 0.0: no "-0.0" in the file


def part_quads(part, net, atlas, texture, density=1):
    """The part as worn_models.json quads, in the stored (half-turned) frame, with UVs into the atlas."""
    u0, v0 = net[0], net[1]
    out = []
    for _, pts, uvs, normal in part_faces(part, density):
        quad = {"texture": texture, "normal": [_r(-normal[0], 4), _r(-normal[1], 4), _r(normal[2], 4)],
                "vertices": [[_r(-x, 4), _r(-y, 4), _r(z, 4), _r((u0 + u) / atlas[0], 5), _r((v0 + v) / atlas[1], 5)]
                             for (x, y, z), (u, v) in zip(pts, uvs)]}
        if part.cutout:
            quad["cutout"] = True
        out.append(quad)
    return out


def piece_kind(item):
    """helmet, chestplate, leggings or boots, from the item id's last word (None for anything else)."""
    kind = item.rsplit("_", 1)[-1]
    return kind if kind in PIECE_CAPS else None


def set_quads(s):
    """{"<item>_<bone>": [quads]} for one set, in definition order. Raises ValueError over the quad caps."""
    nets, atlas = layout(s)
    out = {}
    for item, bones in s.pieces.items():
        for bone, parts in bones.items():
            out[f"{item}_{bone}"] = [q for p in parts for q in part_quads(p, nets.get(p.key, (0, 0)), atlas, s.texture,
                                                                         s.density)]
    for message in budget(s, out):
        raise ValueError(message)
    return out


def _drawn_faces(part):
    """How many faces a part draws: six less those it skips, and for a part with its own texels (from a Blockbench
    project) only the faces it has texels for."""
    if part.uvs:
        return sum(1 for face in dict(part.uvs) if face not in part.skip)
    return 6 - len(part.skip)


def budget(s, quads=None):
    """Over-cap messages: per worn entry, per piece (by its kind) and per set."""
    quads = quads if quads is not None else {f"{i}_{b}": [None] * sum(_drawn_faces(p) for p in ps)
                                              for i, bs in s.pieces.items() for b, ps in bs.items()}
    out = []
    for key, qs in quads.items():
        if len(qs) > ENTRY_CAP:
            out.append(f"{s.name}: {key} has {len(qs)} quads (cap {ENTRY_CAP})")
    total = 0
    for item, bones in s.pieces.items():
        n = sum(len(quads[f"{item}_{b}"]) for b in bones)
        total += n
        cap = PIECE_CAPS.get(piece_kind(item))
        if cap and n > cap:
            out.append(f"{s.name}: {item} has {n} quads (cap {cap})")
    if total > SET_CAP:
        out.append(f"{s.name}: {total} quads in all (cap {SET_CAP})")
    return out


def entries(armor_sets=None, taken=()):
    """worn_models.json entries of every registered set (or of `armor_sets`). `taken` are keys already in the file;
    a set may not reuse one."""
    out = {}
    for s in sets() if armor_sets is None else armor_sets:
        for key, quads in set_quads(s).items():
            if key in out or key in taken:
                raise ValueError(f"{s.name}: worn_models.json already has {key}")
            out[key] = quads
    return out


# ---------------------------------------------------------------- checks (z-fighting, skin, reach)
@dataclass(frozen=True)
class _Face:
    """A drawn face for the checks: its plane (unit normal n, offset d = n . p), the axis its normal is nearest and
    that axis' sign, and its corners in the plane's own 2D basis (the other two axes for a face in an axis plane)."""
    item: str
    bone: str
    part: Part
    face: str
    pts: tuple
    n: tuple
    d: float
    axis: int
    sign: int
    basis: tuple
    poly: tuple

    @property
    def flat(self):
        return abs(abs(self.n[self.axis]) - 1) < 1e-9


def _basis(n, axis):
    """Two unit vectors spanning the plane with normal n: the other two axes for an axis plane (so its 2D corners are
    its coordinates on them), else any orthonormal pair."""
    if abs(abs(n[axis]) - 1) < 1e-9:
        return tuple(AXES["xyz"[i]] for i in range(3) if i != axis)
    a = AXES["y"] if axis != 1 else AXES["x"]
    u = _unit((n[1] * a[2] - n[2] * a[1], n[2] * a[0] - n[0] * a[2], n[0] * a[1] - n[1] * a[0]))
    w = (n[1] * u[2] - n[2] * u[1], n[2] * u[0] - n[0] * u[2], n[0] * u[1] - n[1] * u[0])
    return u, w


def _faces(s, offset=None):
    """Every drawn face of the set, at any angle, in bone space (or, with `offset` {bone: vector}, moved by it)."""
    out = []
    for item, bone, part in s.parts():
        shift = offset[bone] if offset else (0.0, 0.0, 0.0)
        for face, pts, _, normal in part_faces(part, s.density):
            n = _unit(normal)
            pts = tuple(_add(p, shift) for p in pts)
            axis = max(range(3), key=lambda i: abs(n[i]))
            basis = _basis(n, axis)
            poly = tuple((round(_dot(p, basis[0]), 6), round(_dot(p, basis[1]), 6)) for p in pts)
            out.append(_Face(item, bone, part, face, pts, n, _dot(n, pts[0]), axis, 1 if n[axis] > 0 else -1, basis,
                             poly))
    return out


def _rect(lo, hi):
    return [(lo[0], lo[1]), (hi[0], lo[1]), (hi[0], hi[1]), (lo[0], hi[1])]


def _overlap(a, b, shift=(0.0, 0.0)):
    """Whether two convex polygons overlap by more than a sliver (separating axis test; touching edges do not count)."""
    b = [(x + shift[0], y + shift[1]) for x, y in b]
    for poly in (a, b):
        for i in range(len(poly)):
            (x0, y0), (x1, y1) = poly[i], poly[(i + 1) % len(poly)]
            length = math.hypot(x1 - x0, y1 - y0)
            if length < 1e-9:
                continue
            nx, ny = (y1 - y0) / length, (x0 - x1) / length
            pa = [nx * x + ny * y for x, y in a]
            pb = [nx * x + ny * y for x, y in b]
            if max(pa) <= min(pb) + 1e-6 or max(pb) <= min(pa) + 1e-6:
                return False
    return True


def _area(poly):
    return 0.5 * sum(poly[i][0] * poly[i - 1][1] - poly[i - 1][0] * poly[i][1] for i in range(len(poly)))


def _cut(poly, g):
    """The part of a convex polygon where the affine function g(point) >= 0 (one Sutherland-Hodgman step)."""
    out = []
    for i in range(len(poly)):
        p, q = poly[i - 1], poly[i]
        gp, gq = g(p), g(q)
        if (gp >= -1e-9) != (gq >= -1e-9):
            t = gp / (gp - gq)
            out.append((p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1])))
        if gq >= -1e-9:
            out.append(q)
    return out


def _clip(poly, rect):
    """The convex polygon `poly` clipped to the axis-aligned rect ((x0, y0), (x1, y1))."""
    (x0, y0), (x1, y1) = rect
    for k, bound, keep in ((0, x0, 1), (0, x1, -1), (1, y0, 1), (1, y1, -1)):
        poly = _cut(poly, lambda p, k=k, bound=bound, keep=keep: keep * (p[k] - bound))
        if not poly:
            break
    return poly


def _samples(poly, step=0.25):
    """Points covering a convex polygon: its corners, edge midpoints and a grid `step` apart inside it."""
    out = list(poly) + [((poly[i - 1][0] + poly[i][0]) / 2, (poly[i - 1][1] + poly[i][1]) / 2) for i in range(len(poly))]
    xs, ys = [p[0] for p in poly], [p[1] for p in poly]
    nx, ny = int((max(xs) - min(xs)) / step), int((max(ys) - min(ys)) / step)
    for i in range(1, nx + 1):
        for j in range(1, ny + 1):
            point = (min(xs) + i * step, min(ys) + j * step)
            if _inside(point, poly):
                out.append(point)
    return out


def _inside(point, poly):
    """Whether a point lies in a convex polygon (edges included, either winding)."""
    signs = set()
    for i in range(len(poly)):
        (x0, y0), (x1, y1) = poly[i - 1], poly[i]
        c = (x1 - x0) * (point[1] - y0) - (y1 - y0) * (point[0] - x0)
        if abs(c) > 1e-6:
            signs.add(c > 0)
    return len(signs) < 2


def _depth(f, at):
    """The face's plane coordinate along its axis at a point given on the other two axes."""
    others = [i for i in range(3) if i != f.axis]
    return (f.d - f.n[others[0]] * at[0] - f.n[others[1]] * at[1]) / f.n[f.axis]


def _out(f):
    """How far out from its bone's box side a face lies, as a function of a point on the other two axes."""
    lo, hi = BASE[f.bone]
    side = hi[f.axis] if f.sign > 0 else lo[f.axis]
    return lambda at: f.sign * (_depth(f, at) - side)


def _over_bone(f, grow):
    """(region, out_lo, out_hi) for a face over its bone's box grown by `grow` (seen along the face's nearest axis):
    the face's part over the box, as corners on the other two axes, and how far out from the box's side that part lies
    (the least and most); None when the face is not over the box."""
    lo, hi = BASE[f.bone]
    others = [i for i in range(3) if i != f.axis]
    region = _clip([(p[others[0]], p[others[1]]) for p in f.pts],
                   ((lo[others[0]] - grow, lo[others[1]] - grow), (hi[others[0]] + grow, hi[others[1]] + grow)))
    if len(region) < 3 or abs(_area(region)) < 1e-6:
        return None
    out = [round(_out(f)(v), 6) + 0.0 for v in region]
    return region, min(out), max(out)


def _covered(f, region, lo_out, hi_out, by_bone):
    """Whether the part of `region` where f lies between lo_out and hi_out out from the bone (where it would flicker
    or let the wearer through) is hidden: every point of it under another part's face on the bone that faces the same
    way, lies further out and is clear of the bone's skin layers. A face crossing the layers may rise clear of them
    elsewhere (a crest out of the helm), so only the part in that band needs cover."""
    out = _out(f)
    band = _cut(_cut(region, lambda v: out(v) - lo_out), lambda v: hi_out - out(v))
    if len(band) < 3 or abs(_area(band)) < 1e-6:
        return True
    clear = max(SKIN_SHELLS[f.bone]) + SKIN_GAP
    others = [i for i in range(3) if i != f.axis]
    covers = [(c, [(p[others[0]], p[others[1]]) for p in c.pts], _out(c)) for c in by_bone[f.bone]
              if c.part is not f.part and not c.part.cutout and c.axis == f.axis and c.sign == f.sign]
    for point in _samples(band):
        if not any(_inside(point, flat) and c_out(point) >= clear - 1e-6 and c_out(point) > out(point) + 1e-6
                   for c, flat, c_out in covers):
            return False
    return True


def _where(f, out_lo, out_hi):
    if f.flat or abs(out_hi - out_lo) < 1e-6:
        return f"is {out_lo:.2f} px out"
    return f"runs {out_lo:.2f} to {out_hi:.2f} px out"


def problems(s):
    """What would flicker or clip, as messages (empty when clean). Faces at any angle:
    - two faces of different parts on one bone (all pieces worn together), parallel, overlapping and less than
      COPLANAR_GAP apart, facing the same way, unless they show the same texels; or facing each other when one part is
      cutout (the armor draws both sides of every face, so the two fight wherever a see-through texel shows them);
    - a face over the bone's own box (seen along its nearest axis) within SKIN_GAP of the skin or its outer layer
      (SKIN_SHELLS: the hat +0.5 on the head, +0.25 elsewhere), or crossing one: a turned plate whose far side dips into
      the leg shows the wearer through it. Unless another part's face hides it: further out, clear of the layers;
    - a face on a vanilla armor shell another slot may draw there (VANILLA_SHELLS: mixed sets), likewise;
    - faces of two bones in one plane standing (body space): front or back faces of any two bones but the head (an
      idle arm sways about z, which keeps those planes), and faces of any direction between the body and the legs
      (they never sway). The legs' boxes overlap 0.2 px at the centre line, so a skirt split per leg can meet itself.
    Also the structure (check) and quad caps (budget)."""
    out = list(budget(s))
    check(s)
    faces = _faces(s)
    by_bone = {}
    for f in faces:
        by_bone.setdefault(f.bone, []).append(f)
    for bone, fs in by_bone.items():
        for i, a in enumerate(fs):
            for b in fs[i + 1:]:
                facing = _dot(a.n, b.n)
                if a.part is b.part or abs(facing) < 1 - 1e-5:
                    continue
                gap = abs(_dot(a.n, b.pts[0]) - a.d)
                if gap >= COPLANAR_GAP - EPS:
                    continue
                pb = [(_dot(p, a.basis[0]), _dot(p, a.basis[1])) for p in b.pts]
                if not _overlap(a.poly, pb):
                    continue
                if facing < 0:
                    if a.part.cutout or b.part.cutout:
                        out.append(f"{s.name} {bone}: {a.part.name} {a.face} and {b.part.name} {b.face} touch, and a "
                                   f"cutout's see-through texels show them fighting (skip the cutout part's face there)")
                elif not (a.part.key == b.part.key and a.face == b.face and a.poly == b.poly):
                    out.append(f"{s.name} {bone}: {a.part.name} {a.face} and {b.part.name} {b.face} are "
                               f"{gap:.2f} px apart (need {COPLANAR_GAP})")
    for f in faces:
        shells = SKIN_SHELLS[f.bone]
        over = _over_bone(f, max(shells))
        if over is not None:
            region, out_lo, out_hi = over
            hits = [shell for shell in shells if out_lo < shell + SKIN_GAP and out_hi > shell - SKIN_GAP]
            if hits and not _covered(f, region, -SKIN_GAP, max(shells) + SKIN_GAP, by_bone):
                out.append(f"{s.name} {f.bone}: {f.part.name} {f.face} {_where(f, out_lo, out_hi)} from the body, on "
                           f"the skin layer at {hits[0]} (keep {SKIN_GAP} px off {' and '.join(map(str, shells))})")
        shell = VANILLA_SHELLS.get(piece_kind(f.item), {}).get(f.bone)
        over = _over_bone(f, shell) if shell is not None else None
        if over is not None:
            region, out_lo, out_hi = over
            if out_lo < shell + COPLANAR_GAP and out_hi > shell - COPLANAR_GAP and not _covered(
                    f, region, shell - COPLANAR_GAP, shell + COPLANAR_GAP, by_bone):
                out.append(f"{s.name} {f.bone}: {f.part.name} {f.face} {_where(f, out_lo, out_hi)}, on vanilla "
                           f"armor's {shell} shell (another slot's flat armor would flicker through it)")
    still = ("body", "right_leg", "left_leg")
    standing = [f for f in _faces(s, PIVOTS) if f.bone != "head"]
    for i, a in enumerate(standing):
        for b in standing[i + 1:]:
            if a.bone == b.bone or _dot(a.n, b.n) < 1 - 1e-5:
                continue
            if not (a.bone in still and b.bone in still) and not (a.flat and a.axis == 2):
                continue
            if abs(_dot(a.n, b.pts[0]) - a.d) >= COPLANAR_GAP - EPS:
                continue
            if _overlap(a.poly, [(_dot(p, a.basis[0]), _dot(p, a.basis[1])) for p in b.pts]):
                hint = (f" (stop the right leg's at x <= 1.8 or move one out {COPLANAR_GAP} px)"
                        if {a.bone, b.bone} == {"right_leg", "left_leg"} else "")
                out.append(f"{s.name} {a.bone} and {b.bone}: {a.part.name} {a.face} and {b.part.name} {b.face} meet "
                           f"in one plane standing{hint}")
    return out


def warnings(s):
    """Parts far outside the body standing (body space): past REACH_X to the side, REACH_UP above the head top, or
    below the feet. They work, but may pop out at the screen edge or clip into the floor."""
    out = []
    for item, bone, part in s.parts():
        px, py, pz = PIVOTS[bone]
        pts = [(x + px, y + py, z + pz) for x, y, z in corners(part)]
        if max(abs(p[0]) for p in pts) > REACH_X:
            out.append(f"{s.name}: {part.name} reaches {max(abs(p[0]) for p in pts):.1f} px to the side")
        if min(p[1] for p in pts) < REACH_UP:
            out.append(f"{s.name}: {part.name} rises {-8 - min(p[1] for p in pts):.1f} px above the head")
        if max(p[1] for p in pts) > FLOOR:
            out.append(f"{s.name}: {part.name} reaches {max(p[1] for p in pts) - FLOOR:.1f} px below the feet")
    return out


def summary(s):
    """Lines describing a set: atlas size, parts and quads per entry."""
    nets, (w, h) = layout(s)
    quads = set_quads(s)
    lines = [f"{s.name}: texture {s.texture} {w}x{h}, {sum(1 for _ in s.parts())} parts, {len(nets)} nets, "
             f"{sum(len(q) for q in quads.values())} quads"]
    for key, qs in quads.items():
        lines.append(f"  {key}: {len(qs)} quads")
    return lines


if __name__ == "__main__":
    import sys
    failed = False
    for armor_set in sets():
        print("\n".join(summary(armor_set)))
        for line in problems(armor_set):
            failed = True
            print("  PROBLEM", line)
        for line in warnings(armor_set):
            print("  warning", line)
    if not SET_MODULES:
        print("No armor sets registered (armor_models.SET_MODULES is empty).")
    sys.exit(1 if failed else 0)
