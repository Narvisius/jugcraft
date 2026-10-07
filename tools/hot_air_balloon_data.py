"""JSON resources for the hot-air balloon fiesta (fall addition 29), from tools/hot_air_balloon.py: the items' models, the
Mooring Post's block model, words, loot and tags, and the quads the client draws the balloons and pibals from
(assets/jugcraft/balloon_quads.json):
  - balloon_basket: the wicker basket with its padded leather rim and floor, a fuel tank in a leather pocket on either
    side, the instrument panel, the four uprights and the load frame over the riders' heads, the twin-coil burner on
    its frame, and the eight load cables up to the envelope's throat;
  - balloon_flame: the burner's two flames (crossed sheets, drawn while it fires);
  - balloon_envelope_<kind>: the envelope, a surface of 24 gores turned about its axis from the throat to the crown,
    drawn both sides so it shows from inside the basket (flagged "cutout": 26.3's entityCutout draws both sides in the
    opaque pass; "nocull" would put this opaque shell through the translucent pass, where Improved Transparency's
    order-independent blending can let its far side show through, the owner's "tons of transparency"); the Jack-o'-Lantern is ribbed and squat, with a stem, and
    balloon_glow_pumpkin is its carved face, drawn lit while the burner fires;
  - pibal: a small latex balloon on a string with a light at its foot;
  - balloon_mooring_rope: a block's length of rope, stretched by the client from a moored basket to its post.

Called from agriculture_data.py; the advancements' and recipes' data come through HALLOWEEN_ADVANCEMENTS, SHAPED and
SHAPELESS in tools/agriculture.py. Shapes are in model pixels (16 to a block); a balloon's origin is the middle of its
basket's floor, a pibal's the middle of its balloon. The front is north (low z). The envelopes' quads are written
directly (a turned surface isn't boxes); the rest are boxes turned into quads (decor6_data.quads).
"""
import json
import math

from decor_data import MOD, rid, box, block_model, flat_item, self_drop, turned
from decor6_data import quads
from hot_air_balloon import HOT_AIR_BALLOON, KINDS, MOORING, PIBAL, BURNER, profile, pumpkin_profile

HORIZONTAL = ("north", "east", "south", "west")
FULL = (0, 0, 16, 16)
ALL = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

GORES = 24
# Where the rings between the envelope's bands are, from the throat (0) to the crown (1): closer together where it
# curves most, at the throat and over the crown.
RINGS = (0.0, 0.04, 0.1, 0.18, 0.27, 0.37, 0.47, 0.57, 0.66, 0.74, 0.81, 0.87, 0.92, 0.96, 0.99, 1.0)
THROAT = HOT_AIR_BALLOON["throat"] * 16
HEIGHT = HOT_AIR_BALLOON["envelope_height"] * 16
HALF = HOT_AIR_BALLOON["basket"] * 8                  # half the basket's width
WALL = HOT_AIR_BALLOON["basket_height"] * 16          # the basket's height
FRAME_Y = HOT_AIR_BALLOON["burner"] * 16              # the load frame's top
POST = HALF - 1.25                                    # the uprights' middles, in from the basket's corners

BASKET_TEXTURES = {"wicker": "balloon_wicker", "leather": "balloon_leather", "floor": "balloon_floor", "steel": "balloon_steel",
                   "brass": "balloon_brass", "coil": "balloon_coil", "tank": "balloon_tank", "gauge": "balloon_gauge",
                   "rope": "balloon_rope", "suede": "balloon_suede"}


def envelope_texture(kind):
    return f"entity/hot_air_balloon/envelope_{kind}"


GLOW_TEXTURE = "entity/hot_air_balloon/glow_pumpkin"
FLAME_TEXTURE = "entity/hot_air_balloon/flame"
STEM_TEXTURE = "entity/hot_air_balloon/stem"
PIBAL_TEXTURE = "entity/hot_air_balloon/pibal"


# ---------------------------------------------------------------- raw quads

def sub(a, b):
    return [a[i] - b[i] for i in range(3)]


def cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


def unit(v):
    n = math.sqrt(sum(c * c for c in v)) or 1.0
    return [c / n for c in v]


def quad(corners, uvs, texture, **flags):
    """A quad from four corners (pixels), facing the way (corners[2] - corners[0]) x (corners[3] - corners[1]) points
    (the way a box face's corners turn), with UVs from 0 to 1."""
    normal = unit(cross(sub(corners[2], corners[0]), sub(corners[3], corners[1])))
    out = {"texture": texture, "normal": [round(c, 4) for c in normal],
           "vertices": [[round(p[0], 3), round(p[1], 3), round(p[2], 3), round(uv[0], 5), round(uv[1], 5)] for p, uv in zip(corners, uvs)]}
    out.update(flags)
    return out


def prism(a, b, width, texture, ends=False, **flags):
    """A thin square bar from point `a` to point `b` (pixels), `width` across, its texture along it."""
    d = unit(sub(b, a))
    side = unit(cross(d, [0.0, 1.0, 0.0] if abs(d[1]) < 0.95 else [1.0, 0.0, 0.0]))
    up = cross(side, d)
    h = width / 2
    offsets = [[(side[k] * sx + up[k] * sy) * h for k in range(3)] for sx, sy in ((1, 1), (-1, 1), (-1, -1), (1, -1))]
    out = []
    for i in range(4):
        o0, o1 = offsets[i], offsets[(i + 1) % 4]
        corners = [[a[k] + o1[k] for k in range(3)], [a[k] + o0[k] for k in range(3)], [b[k] + o0[k] for k in range(3)],
                   [b[k] + o1[k] for k in range(3)]]
        out.append(quad(corners, [(0, 1), (1, 1), (1, 0), (0, 0)], texture, **flags))
    return out


# ---------------------------------------------------------------- the envelope

def surface_point(kind, s, k):
    """Where ring `s`, gore line `k` of an envelope is (pixels): the Jack-o'-Lantern's ribs bulge between its creases."""
    theta = 2 * math.pi * k / GORES
    if kind == "pumpkin":
        r = pumpkin_profile(s) * (1.0 if k % 2 else 0.92)
    else:
        r = profile(s)
    return [r * 16 * math.cos(theta), THROAT + HEIGHT * s, r * 16 * math.sin(theta)]


def vertex_normal(kind, s, k):
    """The envelope's outward normal at ring `s`, gore line `k`, from its neighbours, so it is lit smoothly across its
    gores (and across a pumpkin's ribs and creases)."""
    eps = 0.01
    a, b = surface_point(kind, max(0.0, s - eps), k), surface_point(kind, min(1.0, s + eps), k)
    along = sub(b, a)
    across = sub(surface_point(kind, s, k + 1), surface_point(kind, s, k - 1))
    n = cross(along, across)
    if math.sqrt(sum(c * c for c in n)) < 1e-6:
        return [0.0, 1.0, 0.0]
    return unit(n)


def envelope(kind, texture, ring_range=None, gore_filter=None, scale=1.0, **flags):
    """The envelope's quads, both sides (drawn without culling), its texture wrapped once round it from the crown (top of
    the texture) to the throat, reading the right way round from outside."""
    out = []
    lo, hi = ring_range or (0, len(RINGS) - 1)
    for j in range(lo, hi):
        s0, s1 = RINGS[j], RINGS[j + 1]
        for k in range(GORES):
            if gore_filter and not gore_filter(k):
                continue
            pts = [surface_point(kind, s0, k + 1), surface_point(kind, s0, k), surface_point(kind, s1, k), surface_point(kind, s1, k + 1)]
            if scale != 1.0:
                pts = [[p[0] * scale, THROAT + (p[1] - THROAT) * scale, p[2] * scale] for p in pts]
            u0, u1 = 1 - k / GORES, 1 - (k + 1) / GORES
            uvs = [(u1, 1 - s0), (u0, 1 - s0), (u0, 1 - s1), (u1, 1 - s1)]
            normals = [vertex_normal(kind, s0, k + 1), vertex_normal(kind, s0, k), vertex_normal(kind, s1, k), vertex_normal(kind, s1, k + 1)]
            out.append(quad(pts, uvs, texture, normals=[[round(c, 3) for c in n] for n in normals], **flags))
    return out


def face_gores(k):
    """The gores the Jack-o'-Lantern's faces are carved on: the front (north, -z) and back, five gores each."""
    middle = (k + 0.5) / GORES * 360.0
    return any(abs((middle - centre + 180) % 360 - 180) <= 38 for centre in (270.0, 90.0))


def stem():
    """The pumpkin's stem on its crown: a curling green stalk in three turned lengths, and a leaf."""
    top = THROAT + HEIGHT * RINGS[-2] - 2
    t = {"stem": STEM_TEXTURE}
    parts = [box((-5, top, -5), (5, top + 10, 5), "#stem"),
             box((-4, top + 10, -4), (4, top + 18, 4), "#stem", rotation={"origin": [0, top + 10, 0], "axis": "z", "angle": 22.5}),
             box((-3, top + 16, -3), (3, top + 24, 3), "#stem", rotation={"origin": [0, top + 16, 0], "axis": "x", "angle": -22.5})]
    for e in parts:
        for face in e["faces"].values():
            face["uv"] = list(FULL)
    return quads(parts, t)


# ---------------------------------------------------------------- the basket, burner and rigging

def full(e):
    for face in e["faces"].values():
        face["uv"] = list(FULL)
    return e


def b(lo, hi, texture, faces=ALL, textures=None, rotation=None):
    return full(box(lo, hi, texture, faces=faces, textures=textures, rotation=rotation))


def basket():
    h, w = HALF, WALL
    inner = h - 1.5
    e = [
        # The wicker walls, the floor of planks inside, and a leather skid round the foot.
        b((-h, 0, -h), (h, w, -inner), "#wicker"), b((-h, 0, inner), (h, w, h), "#wicker"),
        b((-h, 0, -inner), (-inner, w, inner), "#wicker"), b((inner, 0, -inner), (h, w, inner), "#wicker"),
        b((-inner, 0, -inner), (inner, 1.5, inner), "#steel", textures={"up": "#floor"}),
        b((-h - 0.25, 0, -h - 0.25), (h + 0.25, 2, h + 0.25), "#leather", faces=SIDES),
        # The padded leather rim round its top.
        b((-h - 0.5, w - 1, -h - 0.5), (h + 0.5, w + 1.5, -inner + 0.5), "#leather"),
        b((-h - 0.5, w - 1, inner - 0.5), (h + 0.5, w + 1.5, h + 0.5), "#leather"),
        b((-h - 0.5, w - 1, -inner + 0.5), (-inner + 0.5, w + 1.5, inner - 0.5), "#leather"),
        b((inner - 0.5, w - 1, -inner + 0.5), (h + 0.5, w + 1.5, inner - 0.5), "#leather"),
        # The instrument panel on the front wall's inside: altimeter, variometer and envelope thermometer.
        b((-4.5, w - 6, -inner), (4.5, w - 1, -inner + 1), "#steel", textures={"south": "#gauge"}),
    ]
    # A fuel tank in a leather pocket on each side wall, its valve and a hose up to the burner.
    for side in (-1, 1):
        x0, x1 = sorted((side * h, side * (h + 5)))
        e += [b((x0, 2, -3), (x1, 15, 3), "#tank", textures={"up": "#steel", "down": "#steel"}),
              b((x0 - 0.25, 2, -3.25), (x1 + 0.25, 9, 3.25), "#leather"),
              b((side * (h + 1.5) - 1, 15, -1), (side * (h + 1.5) + 1, 17, 1), "#brass")]
    # Four uprights in suede sleeves from the rim's corners to the load frame, and the frame.
    p, top = POST, FRAME_Y
    for x in (-p, p):
        for z in (-p, p):
            e.append(b((x - 0.75, w + 1.5, z - 0.75), (x + 0.75, top, z + 0.75), "#suede", faces=SIDES))
    e += [b((-p - 1, top - 2, -p - 1), (p + 1, top, -p + 1), "#steel"), b((-p - 1, top - 2, p - 1), (p + 1, top, p + 1), "#steel"),
          b((-p - 1, top - 2, -p + 1), (-p + 1, top, p - 1), "#steel"), b((p - 1, top - 2, -p + 1), (p + 1, top, p - 1), "#steel"),
          # The burner's cradle across the frame.
          b((-p + 1, top - 1.5, -1), (p - 1, top - 0.5, 1), "#steel")]
    # The twin-coil burner: two coils side by side, their brass jets on top, a blast valve's lever on each and the
    # pilot light between them.
    for x in (-4, 4):
        e += [b((x - 3.5, top, -3.5), (x + 3.5, top + 6, 3.5), "#coil", textures={"up": "#steel", "down": "#steel"}),
              b((x - 2, top + 6, -2), (x + 2, top + 7.5, 2), "#brass"),
              b((x - 0.5, top + 1, -5.5), (x + 0.5, top + 2, -3.5), "#brass"),
              b((x - 0.5, top - 3, -6), (x + 0.5, top + 2, -5), "#brass")]
    e.append(b((-1, top + 2, -1), (1, top + 5, 1), "#brass"))
    out = quads(e, BASKET_TEXTURES)
    # The hoses, from each tank's valve to the burner.
    for side in (-1, 1):
        out += prism([side * (h + 1.5), 17, 0], [side * 6.5, top + 1, 0], 1.0, BASKET_TEXTURES["rope"])
    # Eight load cables from the frame up to the throat.
    throat = profile(0.0) * 16
    for k in range(8):
        a = math.radians(45 * k)
        corner = k % 2 == 1
        x, z = math.cos(a), math.sin(a)
        start = [p * (1 if x > 0.1 else -1 if x < -0.1 else 0), top, p * (1 if z > 0.1 else -1 if z < -0.1 else 0)]
        if not corner:
            start = [p * x, top, p * z]
        out += prism(start, [throat * x, THROAT + 1, throat * z], 0.6, BASKET_TEXTURES["rope"])
    return out


def flames():
    """Two flames over the burner's jets: crossed sheets of flame, cut out round their tongues."""
    out = []
    for x in (-4, 4):
        y0, y1 = FRAME_Y + 7.5, FRAME_Y + 26
        for a in (45, 135):
            c, s = math.cos(math.radians(a)) * 4.5, math.sin(math.radians(a)) * 4.5
            corners = [[x - c, y0, -s], [x + c, y0, s], [x + c, y1, s], [x - c, y1, -s]]
            out.append(quad(corners, [(0, 1), (1, 1), (1, 0), (0, 0)], FLAME_TEXTURE, cutout=True))
    return out


def pibal():
    """A pibal: a latex balloon a little over half a block across, a tied neck, its string, and a light at its foot."""
    out = []
    rings, gores, r = 8, 12, 4.5
    for j in range(rings):
        a0, a1 = math.pi * j / rings, math.pi * (j + 1) / rings
        for k in range(gores):
            t0, t1 = 2 * math.pi * k / gores, 2 * math.pi * (k + 1) / gores

            def at(a, t):
                # A little taller than wide, and pointed towards the neck.
                stretch = 1.12 + 0.12 * max(0.0, math.cos(a))
                return [r * math.sin(a) * math.cos(t), -r * stretch * math.cos(a), r * math.sin(a) * math.sin(t)]
            pts = [at(a0, t1), at(a0, t0), at(a1, t0), at(a1, t1)]
            uvs = [(1 - (k + 1) / gores, j / rings), (1 - k / gores, j / rings), (1 - k / gores, (j + 1) / rings),
                   (1 - (k + 1) / gores, (j + 1) / rings)]
            out.append(quad(pts, uvs, PIBAL_TEXTURE))
    out += prism([0, -r * 1.24, 0], [0, -r * 1.24 - 14, 0], 0.4, PIBAL_TEXTURE)
    knot = [b((-0.75, -r * 1.24 - 0.5, -0.75), (0.75, -r * 1.24 + 0.75, 0.75), "#latex"),
            b((-1, -r * 1.24 - 17, -1), (1, -r * 1.24 - 14, 1), "#latex")]
    out += quads(knot, {"latex": PIBAL_TEXTURE})
    return out


def balloon_quads():
    out = {"balloon_basket": basket(), "balloon_flame": flames(), "pibal": pibal(),
           # The mooring rope: a block of rope up the y axis, which the client turns and stretches from basket to post.
           "balloon_mooring_rope": prism([0, 0, 0], [0, 16, 0], 0.9, "mooring_post_rope", cutout=True)}
    for kind in KINDS:
        out[f"balloon_envelope_{kind}"] = envelope(kind, envelope_texture(kind), cutout=True)
    out["balloon_envelope_pumpkin"] += stem()
    out["balloon_glow_pumpkin"] = envelope("pumpkin", GLOW_TEXTURE, ring_range=(3, 12), gore_filter=face_gores, scale=1.006, cutout=True)
    return out


# ---------------------------------------------------------------- the mooring post

def mooring_post():
    """A cast-iron bollard on a stone plinth, a rope round its neck, and a winch drum on its front with a brass crank."""
    t = {"iron": "mooring_post_iron", "stone": "mooring_post_stone", "rope": "mooring_post_rope", "brass": "mooring_post_brass"}
    e = [b((1, 0, 1), (15, 3, 15), "#stone"), b((2.5, 3, 2.5), (13.5, 4, 13.5), "#stone"),
         b((5, 4, 5), (11, 11, 11), "#iron"), b((5.5, 11, 5.5), (10.5, 12, 10.5), "#iron"),
         b((3.5, 12, 3.5), (12.5, 14, 12.5), "#iron"), b((5, 14, 5), (11, 15, 11), "#iron"),
         b((4.5, 9, 4.5), (11.5, 10.5, 11.5), "#rope"),
         # The winch: its drum on two cheeks, wound with rope, and its crank.
         b((3, 5, 1.5), (4, 9, 4.5), "#iron"), b((12, 5, 1.5), (13, 9, 4.5), "#iron"),
         b((4, 5.5, 2), (12, 8.5, 4), "#rope"),
         b((13, 6.5, 2.5), (14.5, 7.5, 3.5), "#brass"), b((14, 6.5, 1), (15, 10, 2), "#brass")]
    return block_model(t, e, "mooring_post_iron")


def assets(root, write, lang):
    for spec in KINDS.values():
        flat_item(root, write, spec["item"])
    flat_item(root, write, BURNER["item"])
    flat_item(root, write, PIBAL["item"])
    name = MOORING["block"]
    write(root / "models" / "block" / f"{name}.json", mooring_post())
    write(root / "blockstates" / f"{name}.json", {"variants": {f"facing={f}": turned(rid(f"block/{name}"), f) for f in HORIZONTAL}})
    write(root / "models" / "item" / f"{name}.json", {"parent": rid(f"block/{name}")})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})
    # One quad to a line, as the Ferris wheel's.
    path = root / "balloon_quads.json"
    path.write_text("{\n" + ",\n".join(json.dumps(n) + ": [\n" + ",\n".join(json.dumps(q, separators=(",", ":")) for q in qs) + "\n]"
                                         for n, qs in balloon_quads().items()) + "\n}\n", encoding="utf-8")
    for spec in KINDS.values():
        lang[f"item.{MOD}.{spec['item']}"] = spec["display"]
    lang[f"item.{MOD}.{BURNER['item']}"] = BURNER["display"]
    lang[f"item.{MOD}.{PIBAL['item']}"] = PIBAL["display"]
    lang[f"block.{MOD}.{name}"] = MOORING["display"]
    lang[f"entity.{MOD}.{HOT_AIR_BALLOON['entity']}"] = HOT_AIR_BALLOON["entity_display"]
    lang[f"entity.{MOD}.{PIBAL['entity']}"] = PIBAL["entity_display"]
    lang[f"item.{MOD}.hot_air_balloon.fuel"] = "Fuel: %s of burner"
    lang[f"item.{MOD}.hot_air_balloon.tooltip"] = "Place it on open ground; fuel it with coal, charcoal or coke"
    lang[f"item.{MOD}.pibal.tooltip"] = "Let it go to see which way the winds blow aloft"
    lang[f"message.{MOD}.balloon.gauges"] = "Up %s · Envelope %s%% · Wind %s %s m/s · Fuel %s"
    lang[f"message.{MOD}.balloon.moored"] = "Moored · Envelope %s%% · Fuel %s"
    lang[f"message.{MOD}.balloon.controls"] = "Jump fires the burner · Back opens the vent · Sneak to get out"
    lang[f"message.{MOD}.balloon.fuelled"] = "Fuel: %s of burner"
    lang[f"message.{MOD}.balloon.fuel_full"] = "The tanks are full"
    lang[f"message.{MOD}.balloon.no_fuel"] = "The burner is out of fuel"
    lang[f"message.{MOD}.balloon.no_room"] = "A balloon needs open ground and room over it"
    lang[f"message.{MOD}.balloon.full"] = "The basket is full"
    lang[f"message.{MOD}.mooring.tied"] = "Moored to the post"
    lang[f"message.{MOD}.mooring.untied"] = "Cast off"
    lang[f"message.{MOD}.mooring.none"] = "No balloon within reach of the post"


def loot(out, write):
    write(out / f"{MOORING['block']}.json", self_drop(MOORING["block"]))


def tags(tags):
    tags.add("block", "minecraft:mineable/pickaxe", rid(MOORING["block"]))
