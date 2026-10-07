"""The flail's swinging head (docs/features/arms-restyle.md, "The flail's head swings"). On 5 October 2026 the owner
asked that "flails should have an animated ball that actually flails around". So in the hand a flail's model is its
handle alone (tools/arms_art.py flail_handle), and its chain and spiked ball are two small box models, a link and a
ball, that client/arms/FlailHeads.java draws one by one where a little chain simulation puts them each frame: hanging
under gravity, trailing the arm as it moves and whipping round after a blow.

What this writes:
- models/item/arms_<kind>_link.json and arms_<kind>_ball.json: the shapes, built from boxes as the house style asks
  (docs/ART_DIRECTION.md): closed boxes, no two faces facing the same way in one plane, flat tones from a short ramp.
  They have no parent and no display transforms, so FlailHeads places them exactly;
- models/item/<item>_link.json and <item>_ball.json: each metal's, on that flail's own _model texture, whose free
  corner holds the swatches the boxes are coloured from (paint_swatches);
- the flail's item definition picks them by the custom_model_data string FlailHeads puts on its render-only copies of
  the stack (LINK_CASE, BALL_CASE), and otherwise shows the flail as before (definition());
- arms_heads.json, per item: where the chain hangs from the handle and where the hand holds it (model pixels), the
  links and the ball, the haft's radius (the ball swings clear of it), a link's half width and the ball's reach (they
  keep clear of the holder's body), and the hand poses the handle is drawn with, so FlailHeads can put the chain where
  the handle's eye is drawn.

The measures are in the arms' design units (tools/arms_art.py) and scaled by the handle's own layout, so the head is
always in proportion to the handle it hangs from. All original.
"""
import itertools
import math

import arms_pixel as px
from arms_pixel import DARK, HIGHLIGHT, LIGHT, MID

MOD = "jugcraft"
# The custom_model_data strings that pick a flail's head parts (FlailHeads.LINK and BALL in Java).
LINK_CASE = "flail_link"
BALL_CASE = "flail_ball"
# The kinds whose head swings free, and its measures in design units: its style ("spiked": oval chain links and a
# spiked ball; "skull": vertebrae and a horned skull), how many links and their pitch (centre to centre), how far each
# link is turned about its length from the last (degrees: a chain's interlock), each link's outer width and the
# thickness of its bars, and the ball's core radius, how far its spikes stand out of it and how thick they are (a share
# of the core's radius).
HEADS = {
    "flail": {"style": "spiked", "links": 4, "pitch": 2.4, "twist": 90, "link_width": 1.9, "link_bar": 0.62, "ball": 3.4,
              "spike": 3.0, "spike_width": 0.3},
}
# Arms VII variants (tools/arms_variants.py) whose head swings free, by name: the Bonecarved Flail's spine of vertebrae
# and horned skull ("ball": the skull's half width; "horn": its horns' length).
VARIANT_HEADS = {
    "bonecarved_flail": {"style": "skull", "links": 5, "pitch": 1.55, "twist": 0, "vertebra": 1.5, "ball": 3.9, "horn": 2.6},
}
# The swatches: a 16-texel square in the bottom right corner of the 64x64 _model texture (the handle's upright image
# only fills the top left), as rows of four 4-texel squares, highlight to dark: the chain, the ball's metal and its
# fittings. A skull's face and jaw are painted just above them (FACE, JAW).
SWATCH = (48, 48)
FACE = (48, 32, 12, 11)
JAW = (48, 43, 10, 5)
ROWS = ("chain", "blade", "fitting")
TONES = (HIGHLIGHT, LIGHT, MID, DARK)
# Which tone each face of a box takes, lit from above and the front left as the icons are (an index into TONES), for
# a part's body and for its points, which are a step brighter so they read against it (a horn's tip, a vertebra's
# processes). A ball is in two tones only (BALL_TONE: the game shades its faces too, and a third, dark tone striped its
# rounded core's steps), its spikes the same with the end that points out in the light tone (SPIKE_TIP): no highlight,
# no stepped cap.
FACE_TONE = {"up": 1, "north": 1, "west": 1, "south": 2, "east": 2, "down": 3}
POINT_TONE = {"up": 0, "north": 0, "west": 0, "south": 1, "east": 1, "down": 2}
BALL_TONE = {"up": 1, "north": 1, "west": 1, "south": 2, "east": 2, "down": 2}
SPIKE_TIP = 1


def paint_swatches(texture, materials, skull=False):
    """Paints the head's swatches into a flail's _model texture (its free corner; texture is the 64x64 image):
    materials gives the chain's, the ball's and the fittings' (keys "chain", "blade", "fitting"); a skull's face and
    jaw too, if it has one."""
    if skull:
        paint_face(texture, materials["blade"])
    x0, y0 = SWATCH
    for row, name in enumerate(ROWS):
        tones = materials[name].tones()
        for k, tone in enumerate(TONES):
            for y in range(4):
                for x in range(4):
                    texture.putpixel((x0 + k * 4 + x, y0 + row * 4 + y), tuple(tones[tone]) + (255,))
    return texture


def _uv(row, tone):
    """The middle of a swatch square, in model UV (0 to 16 over the 64-texel texture): flat, and mipmaps stay its colour."""
    u = (SWATCH[0] + TONES.index(tone) * 4) / 4.0
    v = (SWATCH[1] + ROWS.index(row) * 4) / 4.0
    return [u + 0.25, v + 0.25, u + 0.75, v + 0.75]


def _box(frm, to, row, point=False, rotation=None, tip=None, tones=None):
    """A box from frm to to (model pixels), its faces in `row`'s tones by which way they face (`tones`, FACE_TONE if
    not given; a point: a step brighter; `tip`: the face that points out, in SPIKE_TIP's tone)."""
    faces = {}
    for face, tone in (tones or (POINT_TONE if point else FACE_TONE)).items():
        faces[face] = {"uv": _uv(row, TONES[SPIKE_TIP if face == tip else tone]), "texture": "#tex"}
    element = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}
    if rotation is not None:
        element["rotation"] = rotation
    return element


C = 8.0   # the parts are built about the middle of the model, their chain axis along y


def link_elements(head, unit):
    """One chain link, its long axis along y and centred on the model: two side bars and two end bars nested between
    them (nothing overlaps). It is `pitch` long between its bends, plus a bar at each end, so the links interlock."""
    half_len = (head["pitch"] + head["link_bar"]) * unit / 2.0
    half_w = head["link_width"] * unit / 2.0
    bar = head["link_bar"] * unit
    thick = bar / 2.0
    out = []
    for side in (-1, 1):
        x0, x1 = sorted((C + side * half_w, C + side * (half_w - bar)))
        out.append(_box((x0, C - half_len, C - thick), (x1, C + half_len, C + thick), "chain"))
    # End bars a little thinner than the sides, so their front and back faces lie in planes of their own.
    for end in (-1, 1):
        y0, y1 = sorted((C + end * half_len, C + end * (half_len - bar)))
        out.append(_box((C - half_w + bar, y0, C - thick * 0.75), (C + half_w - bar, y1, C + thick * 0.75), "chain"))
    return out


def ball_elements(head, unit):
    """The spiked ball, centred on the model with its lug up (+y, towards the chain): a core of three crossing slabs
    of different sizes (so no two faces share a plane) rounding a cube, a crown of eight spikes round its middle (four
    along x and z, four between them turned 45 degrees about y: every spike turns about one axis only) and one below,
    each a single box standing out of the core, its sides in the ball's tones and its end in the light one. Few spikes
    in two tones read as a spiked ball from every side, not as speckle."""
    r = head["ball"] * unit
    out_by = head["spike"] * unit
    out = []
    # The core, rounded: a cube, and through it one slab long in x, one in y and one in z, each a different size across
    # (so no two faces share a plane), stepping the outline in towards the poles.
    half = ((r, 0.52 * r, 0.52 * r), (0.46 * r, r + 0.12, 0.46 * r), (0.4 * r, 0.4 * r, r + 0.24))
    out.append(_box((C - 0.72 * r,) * 3, (C + 0.72 * r,) * 3, "blade", tones=BALL_TONE))
    for hx, hy, hz in half:
        out.append(_box((C - hx, C - hy, C - hz), (C + hx, C + hy, C + hz), "blade", tones=BALL_TONE))
    # The spikes: from inside the core out past it. The turned ones a little thinner, so no face of theirs shares a
    # plane with an upright one's.
    w = head["spike_width"] * r / 2.0
    for axis, sign, tip in ((0, 1, "east"), (0, -1, "west"), (2, 1, "south"), (2, -1, "north"), (1, -1, "down")):
        reach = half[axis][axis]
        lo, hi = [C - w] * 3, [C + w] * 3
        lo[axis], hi[axis] = (C + 0.6 * r, C + reach + out_by) if sign > 0 else (C - reach - out_by, C - 0.6 * r)
        out.append(_box(lo, hi, "blade", tip=tip, tones=BALL_TONE))
    turned = 0.82 * w
    for sign, tip in ((1, "east"), (-1, "west")):
        for angle in (45.0, -45.0):
            lo, hi = [C - turned] * 3, [C + turned] * 3
            lo[0], hi[0] = (C + 0.6 * r, C + 0.86 * r + out_by) if sign > 0 else (C - 0.86 * r - out_by, C - 0.6 * r)
            out.append(_box(lo, hi, "blade", rotation={"angle": angle, "axis": "y", "origin": [C, C, C]}, tip=tip,
                            tones=BALL_TONE))
    # The lug the chain's last link hangs from: a fitting collar on the core's top and an eye above it.
    top = half[1][1]
    collar, eye = 0.34 * r, 0.16 * r
    out.append(_box((C - collar, C + top - 0.1, C - collar), (C + collar, C + top + 0.3 * r, C + collar), "fitting"))
    out.append(_box((C - eye, C + top + 0.3 * r - 0.1, C - eye * 1.6), (C + eye, C + top + 0.62 * r, C + eye * 1.6), "fitting"))
    return out


def paint_face(texture, bone):
    """A skull's face and its jaw, in the house's clean creature style (docs/ART_DIRECTION.md): flat bone lit along its
    top and left and shaded along its bottom and right, square eye sockets in a soft dark plum as Minecraft's skulls
    have, no nose holes; the jaw a neat row of square teeth with dark gaps."""
    tones = bone.tones()
    plum = (58, 34, 52)
    x0, y0, w, h = FACE
    for y in range(h):
        for x in range(w):
            tone = LIGHT
            if y == 0 or x == 0:
                tone = HIGHLIGHT
            elif y == h - 1 or x == w - 1:
                tone = MID
            texture.putpixel((x0 + x, y0 + y), tuple(tones[tone]) + (255,))
    for ex in (2, w - 5):
        for y in range(4, 7):
            for x in range(ex, ex + 3):
                texture.putpixel((x0 + x, y0 + y), plum + (255,))
        # A soft lower lid, a shade into the bone, keeps the socket from reading as a hole cut through.
        for x in range(ex, ex + 3):
            texture.putpixel((x0 + x, y0 + 7), tuple(tones[MID]) + (255,))
    x0, y0, w, h = JAW
    for y in range(h):
        for x in range(w):
            if y == 0 or y == h - 1:
                colour = tones[MID] if y == 0 else tones[DARK]
            else:
                colour = plum if x % 2 == 1 or x in (0, w - 1) else tones[HIGHLIGHT if y == 1 else LIGHT]
            texture.putpixel((x0 + x, y0 + y), tuple(colour) + (255,))
    return texture


def _face_uv(rect):
    x0, y0, w, h = rect
    return [x0 / 4.0, y0 / 4.0, (x0 + w) / 4.0, (y0 + h) / 4.0]


def vertebra_elements(head, unit):
    """One vertebra, its spine along y and centred: a round body, a cord through it to the next (exactly a pitch long,
    so neighbours meet end to end), side processes and a spine at the back."""
    w = head["vertebra"] * unit / 2.0
    half_pitch = head["pitch"] * unit / 2.0
    out = [_box((C - w, C - 0.32 * w * 2, C - w), (C + w, C + 0.32 * w * 2, C + w), "chain"),
           _box((C - 0.3 * w, C - half_pitch + 0.01, C - 0.3 * w), (C + 0.3 * w, C + half_pitch - 0.01, C + 0.3 * w), "chain"),
           _box((C - 1.7 * w, C - 0.3 * w, C - 0.62 * w), (C + 1.7 * w, C + 0.3 * w, C + 0.62 * w), "chain", True),
           _box((C - 0.24 * w, C - 0.2 * w, C - 1.75 * w), (C + 0.24 * w, C + 0.2 * w, C - 0.9 * w), "chain", True)]
    return out


def skull_elements(head, unit):
    """A horned skull, centred, its crown up (+y, towards the chain) and its face to the front (+z): a broad cranium
    and a deeper, narrower one through it (the face painted on its front), a jaw with its teeth, two horns from the
    temples in two tiers, and a horn lug on the crown."""
    r = head["ball"] * unit
    face, jaw = _face_uv(FACE), _face_uv(JAW)
    a = _box((C - r, C - 0.3 * r, C - 0.86 * r), (C + r, C + 0.95 * r, C + 0.86 * r), "blade")
    b = _box((C - 0.72 * r, C - 0.18 * r, C - r), (C + 0.72 * r, C + 1.1 * r, C + r), "blade")
    b["faces"]["south"] = {"uv": face, "texture": "#tex"}
    j = _box((C - 0.62 * r, C - 0.82 * r, C - 0.35 * r), (C + 0.62 * r, C - 0.24 * r, C + 0.97 * r), "blade")
    j["faces"]["south"] = {"uv": jaw, "texture": "#tex"}
    out = [a, b, j]
    horn = head["horn"] * unit
    for side, angle in ((1, 40.0), (-1, -40.0)):
        for w, a0, a1, point in ((0.2 * r, 0.8 * r, r + horn * 0.55, False), (0.09 * r, r + horn * 0.5, r + horn, True)):
            lo, hi = [C - w, C - w + 0.35 * r, C - w], [C + w, C + w + 0.35 * r, C + w]
            lo[0], hi[0] = (C + a0, C + a1) if side > 0 else (C - a1, C - a0)
            out.append(_box(lo, hi, "fitting", point, {"angle": 45.0 if side > 0 else -45.0, "axis": "z", "origin": [C, C + 0.35 * r, C]}))
    top = 1.1 * r
    out.append(_box((C - 0.28 * r, C + top - 0.1, C - 0.28 * r), (C + 0.28 * r, C + top + 0.32 * r, C + 0.28 * r), "fitting"))
    out.append(_box((C - 0.12 * r, C + top + 0.32 * r - 0.1, C - 0.14 * r), (C + 0.12 * r, C + top + 0.6 * r, C + 0.14 * r), "fitting"))
    return out


def ball_reach(head, unit):
    """How far the ball reaches from its centre (model pixels): the core and spikes, or the skull and its horns, for the
    haft it swings clear of."""
    if head["style"] == "skull":
        return (head["ball"] + head["horn"] * 0.8) * unit
    return (head["ball"] + head["spike"]) * unit


def chain_radius(head, unit):
    """A link's half width (model pixels): how far a link keeps clear of the holder's body."""
    return (head["vertebra"] if head["style"] == "skull" else head["link_width"]) * unit / 2.0


def lug(head, unit):
    """From the ball's centre to where the last link hooks its lug (model pixels)."""
    if head["style"] == "skull":
        return (1.1 + 0.6) * head["ball"] * unit - 0.25 * head["vertebra"] * unit
    return (head["ball"] + 0.62 * head["ball"]) * unit - 0.5 * head["link_bar"] * unit


# ---------------------------------------------------------------- the models and the table


def models(head, unit):
    """{"link": model, "ball": model}: a head's models (HEADS or VARIANT_HEADS), textured #tex, with no parent."""
    skull = head["style"] == "skull"
    return {"link": {"textures": {"particle": "#tex"}, "elements": (vertebra_elements if skull else link_elements)(head, unit)},
            "ball": {"textures": {"particle": "#tex"}, "elements": (skull_elements if skull else ball_elements)(head, unit)}}


def definition(item, fallback):
    """A flail's item definition model: its head parts by FlailHeads' custom_model_data string, else `fallback`."""
    return {"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 0,
            "cases": [{"when": LINK_CASE, "model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}_link"}},
                      {"when": BALL_CASE, "model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}_ball"}}],
            "fallback": fallback}


def entry(head, layout, display):
    """An item's arms_heads.json entry for a head (HEADS or VARIANT_HEADS): `layout` is (grip_model, unit, grip, eye):
    the hand's point in model pixels, model pixels a design unit, and the grip and eye along the haft in design units;
    `display` the handle's hand poses. The eye and the grip are given in model pixels, on the handle's 45-degree lean."""
    (gx, gy), unit, grip, eye = layout
    lean = math.sqrt(0.5)
    along = (eye - grip) * unit
    anchor = [round(gx + along * lean, 4), round(gy + along * lean, 4), 8.0]
    return {"anchor": anchor, "grip": [round(gx, 4), round(gy, 4), 8.0], "links": head["links"],
            "pitch": round(head["pitch"] * unit, 4), "twist": head["twist"], "lug": round(lug(head, unit), 4),
            "reach": round(ball_reach(head, unit), 4), "haft": round(0.85 * unit, 4),
            "chain_radius": round(chain_radius(head, unit), 4),
            "display": {context: display[context] for context in sorted(display)}}


# ---------------------------------------------------------------- checks (tools/check_mod_data.py)

_NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0), "down": (0, -1, 0),
            "up": (0, 1, 0)}


def _turn(rotation, p):
    if not rotation:
        return p
    a = math.radians(rotation["angle"])
    c, s = math.cos(a), math.sin(a)
    o = rotation["origin"]
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    if rotation["axis"] == "x":
        x, y, z = x, y * c - z * s, y * s + z * c
    elif rotation["axis"] == "y":
        x, y, z = x * c + z * s, y, -x * s + z * c
    else:
        x, y, z = x * c - y * s, x * s + y * c, z
    return (x + o[0], y + o[1], z + o[2])


def _faces(element):
    """Each face of an element as (normal, plane offset, its four corners), after the element's rotation."""
    f, t = element["from"], element["to"]
    rot = element.get("rotation")
    o = rot["origin"] if rot else (0, 0, 0)
    out = []
    for face, n in _NORMALS.items():
        axis = [i for i in range(3) if n[i]][0]
        others = [i for i in range(3) if i != axis]
        corners = []
        for a, b in ((0, 0), (1, 0), (1, 1), (0, 1)):
            p = [0.0, 0.0, 0.0]
            p[axis] = t[axis] if n[axis] > 0 else f[axis]
            p[others[0]] = (f, t)[a][others[0]]
            p[others[1]] = (f, t)[b][others[1]]
            corners.append(_turn(rot, p))
        normal = _turn({"angle": rot["angle"], "axis": rot["axis"], "origin": (0, 0, 0)} if rot else None, n)
        out.append((normal, sum(c * k for c, k in zip(corners[0], normal)), corners))
    return out


def _overlap(a, b, normal):
    """Whether two convex quads in one plane overlap in area (separating axes in the plane)."""
    def axes(quad):
        for i in range(4):
            e = [quad[(i + 1) % 4][k] - quad[i][k] for k in range(3)]
            yield (normal[1] * e[2] - normal[2] * e[1], normal[2] * e[0] - normal[0] * e[2], normal[0] * e[1] - normal[1] * e[0])
    for axis in itertools.chain(axes(a), axes(b)):
        pa = [sum(p[k] * axis[k] for k in range(3)) for p in a]
        pb = [sum(p[k] * axis[k] for k in range(3)) for p in b]
        if min(max(pa), max(pb)) - max(min(pa), min(pb)) <= 1e-4:
            return False
    return True


def coplanar(elements, gap=0.1):
    """Pairs of faces (element indices, face normal) that face the same way in planes closer than `gap` pixels and
    overlap: they would flicker. Rotated boxes are compared too (after their turns)."""
    faces = [(i, normal, offset, corners) for i, e in enumerate(elements) for normal, offset, corners in _faces(e)]
    bad = []
    for (i, n1, o1, q1), (j, n2, o2, q2) in itertools.combinations(faces, 2):
        if i == j or sum(a * b for a, b in zip(n1, n2)) < 0.9999 or abs(o1 - o2) >= gap - 1e-6:
            continue
        if _overlap(q1, q2, n1):
            bad.append((i, j, tuple(round(v, 3) for v in n1)))
    return bad
