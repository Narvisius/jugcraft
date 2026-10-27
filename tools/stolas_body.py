"""Stolas's body (Concordance Chunk D, D-06, decision D-S5): the owner library's Flashjack rig
(art/owner-library/originals/Magic/assets/ars_jymbaumental/: geo/flashjack.geo.json, animations/flashjack.animation.json,
textures/entity/flashjack.png), adapted into his raven form. The geometry is copied unchanged (tools/concordance_pacts.py
OWNER_FILES); what is adapted is made here, from the library files' bytes, and written and checked by tools/owner_art.py
(concordance_pacts.ADAPTED). No I/O here, no randomness: the same library files always give the same runtime files.

- skin(read): Flashjack's 64x64 skin recoloured, texel for texel, into a raven's: the golden plumage black, its navy
  markings a blue-black sheen, the beak, legs and toes a dark horn, the eyes amber, the crest's few texels gold (a small
  crown) and the breast's glyph gold (his sigil). Which part a texel belongs to is read from the geometry's box-UV
  faces, so the eye's and the sigil's cyan are told apart from the wings' streaks. Every texel keeps its place and its
  alpha; the owner's shading is kept, its brightness carried onto each new ramp. Nothing is drawn that the owner did not
  draw.
- glowmask(read): the amber eye texels alone, for GeckoLib's AutoGlowingGeoLayer (his eyes catch the light).
- animations(read): Flashjack's five clips unchanged, and Stolas's own: his loops (idle and waiting, from idle.sitting's
  and idle.ground's poses; listen; suspended) and his gestures (arrive, inspect, assent, refuse, depart), keyframed on
  Flashjack's bones. Rotations in degrees, Bedrock's sense (see tools/concordance_worker_models.py): +x pitches a bone's
  top forward; a left wing (wing_left, +x) opens with -z, a right wing with +z.

`read(path)` returns a library file's bytes, `path` relative to art/owner-library/originals/Magic/.
"""
import colorsys
import io
import json
import math

from PIL import Image

import concordance_pacts as pacts

FLASHJACK = pacts.FLASHJACK
GEO = FLASHJACK + "geo/flashjack.geo.json"
SKIN = FLASHJACK + "textures/entity/flashjack.png"
ANIMATIONS = FLASHJACK + "animations/flashjack.animation.json"

# ------------------------------------------------------------------------------------------------- the skin
# Ramps, darkest first; a texel's place on its ramp is its brightness within its kind (the owner's shading, kept).
PLUMAGE = ((14, 14, 20), (56, 58, 76))      # raven black, cool in its lights
SHEEN = ((10, 14, 34), (50, 74, 132))       # the blue-black of a raven's wing
HORN = ((28, 26, 30), (86, 82, 88))         # beak, legs and toes: dark horn
EYE = ((214, 122, 18), (255, 214, 104))     # amber
GOLD = ((118, 80, 22), (238, 194, 84))      # the crown and the sigil: old gold
# Which cubes are which (bone, cube index in the geometry); every other cube is plumage.
HORN_CUBES = {("head", 1), ("legs", 0), ("legs", 1), ("legs", 2), ("legs", 3), ("feets", 0), ("feets", 1)}
CROWN_CUBES = {("head", 2), ("head", 3)}
EYE_CUBE = ("head", 0)
SIGIL_CUBE = ("upperBody", 0)


def _faces(u, v, w, h, d):
    """The texel rectangles a box-UV cube draws (GeckoLib's GeometryQuadUvs.ofBoxUv), those with area only."""
    faces = ((u + d, v, u + d + w, v + d), (u + d + w, v, u + d + 2 * w, v + d),
             (u, v + d, u + d, v + d + h), (u + d, v + d, u + d + w, v + d + h),
             (u + d + w, v + d, u + 2 * d + w, v + d + h), (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h))
    return [f for f in faces if f[2] > f[0] and f[3] > f[1]]


def owners(geo):
    """{(x, y): (bone, cube index)} for every texel some cube's face draws (the first cube wins; none share one)."""
    out = {}
    for bone in geo["minecraft:geometry"][0]["bones"]:
        for index, cube in enumerate(bone.get("cubes", [])):
            (w, h, d), (u, v) = cube["size"], cube["uv"]
            for x0, y0, x1, y1 in _faces(u, v, w, h, d):
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        out.setdefault((x, y), (bone["name"], index))
    return out


def kind(rgb):
    """A Flashjack texel's colour: 'gold' (its yellow plumage), 'pale' (the glyph's pale yellow), 'navy' (its dark blue
    markings) or 'cyan' (its bright markings: the eyes, the glyph, the wings' streaks)."""
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    if 0.40 <= h <= 0.56 and s > 0.3 and v > 0.45:
        return "cyan"
    if 0.45 <= h <= 0.75:
        return "navy"
    if s < 0.86 and v > 0.85:
        return "pale"
    return "gold"


def _luma(rgb):
    return (0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]) / 255


# The brightness each kind spans in Flashjack's skin (measured): a texel's place on its new ramp.
SPAN = {"gold": (0.50, 0.80), "pale": (0.69, 0.89), "navy": (0.15, 0.40), "cyan": (0.23, 0.73)}


def _on(ramp, colour, which):
    lo, hi = SPAN[which]
    t = min(1.0, max(0.0, (_luma(colour) - lo) / (hi - lo)))
    return tuple(round(a + (b - a) * t) for a, b in zip(*ramp))


def part(owner, which):
    """The ramp a texel of kind `which` on cube `owner` (None: no cube draws it) takes."""
    if owner in HORN_CUBES:
        return HORN
    if owner in CROWN_CUBES:
        return GOLD
    if owner == EYE_CUBE and which == "cyan":
        return EYE
    if owner == SIGIL_CUBE and which != "gold":
        return GOLD
    if which in ("navy", "cyan"):
        return SHEEN
    return PLUMAGE


def skin(read):
    source = Image.open(io.BytesIO(read(SKIN))).convert("RGBA")
    where = owners(json.loads(read(GEO)))
    out = source.copy()
    for y in range(source.height):
        for x in range(source.width):
            p = source.getpixel((x, y))
            if p[3]:
                which = kind(p[:3])
                out.putpixel((x, y), _on(part(where.get((x, y)), which), p[:3], which) + (p[3],))
    return out


def glowmask(read):
    """His eyes alone, as drawn on the skin; everything else clear."""
    source = Image.open(io.BytesIO(read(SKIN))).convert("RGBA")
    where = owners(json.loads(read(GEO)))
    painted = skin(read)
    out = Image.new("RGBA", source.size, (0, 0, 0, 0))
    for (x, y), owner in where.items():
        p = source.getpixel((x, y))
        if p[3] and owner == EYE_CUBE and kind(p[:3]) == "cyan":
            out.putpixel((x, y), painted.getpixel((x, y)))
    return out


# ------------------------------------------------------------------------------------------------- the animations
# His clips' names, lengths and poses are tools/concordance_pacts.py's (LOOPS, GESTURES, GESTURE_TIMES, LOOP_LENGTHS,
# ATTEND): the Java plays them by name (spirits/StolasEntity) and check_stolas_body holds the two together.

def _key(seconds):
    return str(round(float(seconds), 4))


def _vec(values):
    return [round(value, 3) + 0.0 for value in values]  # + 0.0 turns -0.0 into 0.0


def _keys(*frames, easing=None):
    """{time: [x, y, z]} from (time, (x, y, z)) pairs; with `easing`, every key after the first eases in and out."""
    out = {}
    for i, (time, values) in enumerate(frames):
        out[_key(time)] = {"vector": _vec(values), "easing": easing} if easing and i else _vec(values)
    return out


def _wave(length, cycles, base, amplitude, phase=0.0, steps=8):
    """base + amplitude * sin(2 pi (cycles t / length + phase)), sampled `steps` times a cycle: the first and last keys
    match, so a loop is seamless (tools/concordance_worker_models.py's _wave)."""
    keys = {}
    total = steps * cycles
    for i in range(total + 1):
        wave = math.sin(2 * math.pi * (i / steps + phase))
        keys[_key(length * i / total)] = _vec([b + a * wave for b, a in zip(base, amplitude)])
    return keys


def _final(clip, bone, channel):
    """A bone channel's last keyframe in one of the owner's clips (a pose it ends in), as [x, y, z]."""
    track = clip["bones"][bone][channel]
    if isinstance(track, list):
        return [float(v) for v in track]
    last = track[max(track, key=float)]
    return [float(v) for v in (last["vector"] if isinstance(last, dict) else last)]


def poses(owner):
    """His two resting poses, read from the owner's clips: perched (idle.sitting's last frame) and standing
    (idle.ground's still channels). {bone: {"rotation": [...], "position": [...]}}."""
    sitting, ground = owner["animations"]["idle.sitting"], owner["animations"]["idle.ground"]
    perched = {bone: {channel: _final(sitting, bone, channel) for channel in channels}
               for bone, channels in sitting["bones"].items()}
    standing = {bone: {channel: [float(v) for v in values] for channel, values in channels.items() if isinstance(values, list)
                       and all(not isinstance(v, str) for v in values)}
                for bone, channels in ground["bones"].items()}
    return perched, {bone: channels for bone, channels in standing.items() if channels}


def _at(pose, bone, channel="rotation"):
    return pose.get(bone, {}).get(channel, [0.0, 0.0, 0.0])


def _plus(a, b):
    return [x + y for x, y in zip(a, b)]


def stolas_clips(owner):
    """Stolas's own clips, keyed on Flashjack's bones. His gestures are made at the perch's screen, while he listens, so
    each starts and ends on his listening pose (ATTEND: perched, leaning in, his head cocked) for every bone it moves:
    it takes over from his listening loop, and hands back to it, without a jump (from his perched loop the step is a
    small one: the lean and the cock of his head)."""
    P, G = poses(owner)
    h, hp, b, t, l, lp, f = (_at(P, "head"), _at(P, "head", "position"), _at(P, "body"), _at(P, "tail"), _at(P, "legs"),
                             _at(P, "legs", "position"), _at(P, "feets"))
    zero = [0.0, 0.0, 0.0]
    # Listening (ATTEND): perched, leaning in a little, the head cocked to one eye, the wings eased from the body.
    attend = pacts.ATTEND
    ha, hpa, ba, wa = _plus(h, attend["head"]), _plus(hp, attend["head_position"]), _plus(b, attend["body"]), attend["wing"]

    def loop(length, bones):
        return {"loop": True, "animation_length": length, "bones": bones}

    def once(length, bones, hold=False):
        return {"loop": "hold_on_last_frame" if hold else False, "animation_length": length, "bones": bones}

    def wings(*frames, easing=None):
        """Both wings from the left wing's (x, y, z) keys: the right mirrors y and z."""
        return {"wing_left": {"rotation": _keys(*frames, easing=easing)},
                "wing_right": {"rotation": _keys(*((time, (x, -y, -z)) for time, (x, y, z) in frames), easing=easing)}}

    perched_still = {"head": {"rotation": _keys((0, h)), "position": _keys((0, hp))}, "body": {"rotation": _keys((0, b))},
                     "tail": {"rotation": _keys((0, t))}, "legs": {"rotation": _keys((0, l)), "position": _keys((0, lp))},
                     "feets": {"rotation": _keys((0, f))}}
    gb, gl, gf = _at(G, "body"), _at(G, "legs"), _at(G, "feets")
    standing_still = {"body": {"rotation": _keys((0, gb))}, "legs": {"rotation": _keys((0, gl))},
                      "feets": {"rotation": _keys((0, gf))}}
    E = "easeInOutSine"
    L = pacts.LOOP_LENGTHS
    T = {name: length for name, (length, _) in pacts.GESTURE_TIMES.items()}
    clips = {
        # Perched (idle.sitting's pose): breathing, looking left and right, now and then a flick of the tail.
        pacts.LOOPS["idle"]: loop(L["idle"], dict(perched_still, **{
            "upperBody": {"position": _wave(L["idle"], 2, zero, (0, 0.2, 0))},
            "head": {"rotation": _keys((0, h), (0.8, h), (1.3, _plus(h, (0, 22, 0))), (2.0, _plus(h, (0, 22, 0))), (2.4, h),
                                       (3.0, _plus(h, (0, -14, 4))), (3.5, _plus(h, (0, -14, 4))), (4.0, h), easing=E),
                     "position": _keys((0, hp))},
            "tail": {"rotation": _keys((0, t), (3.1, t), (3.25, _plus(t, (-12, 0, 0))), (3.45, t), (4.0, t))}})),
        # Listening: ATTEND held, breathing, the cocked head tilting a little further and back.
        pacts.LOOPS["listen"]: loop(L["listen"], dict(perched_still, **{
            "body": {"rotation": _keys((0, ba))},
            "upperBody": {"position": _wave(L["listen"], 2, zero, (0, 0.15, 0))},
            "head": {"rotation": _keys((0, ha), (1.5, _plus(ha, (3, 0, -5))), (3.0, ha), easing=E), "position": _keys((0, hpa))},
            **wings((0, wa))})),
        # Waiting (idle.ground's stance): standing, breathing as the owner's clip does, looking about.
        pacts.LOOPS["waiting"]: loop(L["waiting"], dict(standing_still, **{
            "upperBody": {"position": _wave(L["waiting"], 2, zero, (0, 0.25, 0))},
            "head": {"rotation": _keys((0, zero), (0.6, zero), (1.1, (0, 30, 0)), (1.7, (0, 30, 0)), (2.2, zero), (2.6, zero),
                                       (3.1, (0, -30, 0)), (3.6, (0, -30, 0)), (4.0, zero), easing=E),
                     "position": _wave(L["waiting"], 2, zero, (0, 0.15, 0))}})),
        # Suspended: standing hunched and puffed up, his head tucked down, his wings slack; slow breaths.
        pacts.LOOPS["suspended"]: loop(L["suspended"], dict(standing_still, **{
            "body": {"rotation": _keys((0, _plus(gb, (14, 0, 0))))},
            "upperBody": {"position": _wave(L["suspended"], 2, (0, -0.3, 0), (0, 0.2, 0)), "scale": _keys((0, (1.1, 1.06, 1.1)))},
            "head": {"rotation": _keys((0, (45, 0, 4))), "position": _keys((0, (0, -2, 0.6)))},
            "tail": {"rotation": _keys((0, (-8, 0, 0)))},
            **wings((0, (8, 0, -6)))})),
        # Arrive: he comes down onto the perch from above, wings beating, flares (wings high, feet reaching: the telling
        # pose), grips, folds his wings and settles, listening.
        pacts.GESTURES["arrive"]: once(T["arrive"], {
            "master": {"position": _keys((0, (0, 14, 6)), (0.7, (0, 3, 0.5)), (1.2, (0, 2, 0)), (1.4, zero), (2.0, zero), easing=E),
                       "rotation": _keys((0, (12, 0, 0)), (0.7, (-8, 0, 0)), (1.2, (-10, 0, 0)), (1.4, zero), (2.0, zero), easing=E)},
            **wings((0, (-10, 0, -100)), (0.15, (-10, 0, -45)), (0.3, (-10, 0, -105)), (0.45, (-10, 0, -50)),
                    (0.7, (-20, 0, -115)), (1.2, (-24, 0, -110)), (1.4, (-10, 0, -60)), (1.7, (0, 0, -12)), (2.0, wa)),
            "legs": {"rotation": _keys((0, (35, 0, 0)), (0.7, (-5, 0, 0)), (1.2, (-12, 0, 0)), (1.4, l), (2.0, l), easing=E),
                     "position": _keys((0, zero), (1.2, zero), (1.4, lp), (2.0, lp))},
            "feets": {"rotation": _keys((0, (60, 0, 0)), (0.7, (-10, 0, 0)), (1.2, (-10, 0, 0)), (1.4, f), (2.0, f), easing=E)},
            "tail": {"rotation": _keys((0, (-15, 0, 0)), (0.7, (30, 0, 0)), (1.2, (32, 0, 0)), (1.6, t), (2.0, t), easing=E)},
            "body": {"rotation": _keys((0, zero), (0.7, (-6, 0, 0)), (1.2, (-6, 0, 0)), (1.45, _plus(b, (10, 0, 0))), (1.8, ba),
                                       (2.0, ba), easing=E)},
            "head": {"rotation": _keys((0, (-12, 0, 0)), (0.7, (-4, 0, 0)), (1.2, (-4, 0, 0)), (1.7, ha), (2.0, ha), easing=E),
                     "position": _keys((0, zero), (1.2, zero), (1.7, hpa), (2.0, hpa))}}),
        # Inspect: he leans down to look with one eye (the telling pose), then the other, taps once with his beak and
        # straightens.
        pacts.GESTURES["inspect"]: once(T["inspect"], {
            "body": {"rotation": _keys((0, ba), (0.4, _plus(b, (26, 0, 0))), (1.9, _plus(b, (26, 0, 0))), (2.5, ba), easing=E)},
            "head": {"rotation": _keys((0, ha), (0.4, _plus(h, (18, 0, -24))), (1.1, _plus(h, (18, 0, -24))), (1.3, _plus(h, (18, 0, 24))),
                                       (1.55, _plus(h, (18, 0, 24))), (1.7, _plus(h, (38, 0, 0))), (1.85, _plus(h, (18, 0, 0))),
                                       (2.5, ha), easing=E),
                     "position": _keys((0, hpa), (0.4, _plus(hp, (0, 0, -0.6))), (1.9, _plus(hp, (0, 0, -0.6))), (2.5, hpa), easing=E)},
            "tail": {"rotation": _keys((0, t), (0.4, _plus(t, (-22, 0, 0))), (1.9, _plus(t, (-22, 0, 0))), (2.5, t), easing=E)},
            **wings((0, wa), (0.4, (0, 0, -10)), (1.9, (0, 0, -10)), (2.5, wa), easing=E)}),
        # Assent: a deep, slow bow, his wings half open (the telling pose), and back.
        pacts.GESTURES["assent"]: once(T["assent"], {
            "body": {"rotation": _keys((0, ba), (0.45, _plus(b, (16, 0, 0))), (1.25, _plus(b, (16, 0, 0))), (1.7, ba), (2.0, ba), easing=E)},
            "head": {"rotation": _keys((0, ha), (0.45, _plus(h, (28, 0, 0))), (1.25, _plus(h, (28, 0, 0))), (1.7, ha), (2.0, ha), easing=E),
                     "position": _keys((0, hpa), (0.45, _plus(hp, (0, -0.5, -0.5))), (1.25, _plus(hp, (0, -0.5, -0.5))), (1.7, hpa),
                                       (2.0, hpa), easing=E)},
            "tail": {"rotation": _keys((0, t), (0.45, _plus(t, (-15, 0, 0))), (1.25, _plus(t, (-15, 0, 0))), (1.7, t), (2.0, t), easing=E)},
            **wings((0, wa), (0.45, (-20, 0, -60)), (1.25, (-20, 0, -60)), (1.6, (0, 0, -10)), (2.0, wa), easing=E)}),
        # Refuse: he draws himself up, beak raised, turns his head away (the telling pose) with a flick of both wings,
        # shakes it once and settles.
        pacts.GESTURES["refuse"]: once(T["refuse"], {
            "body": {"rotation": _keys((0, ba), (0.3, (-10, 0, 0)), (1.5, (-10, 0, 0)), (2.0, ba), easing=E)},
            "head": {"rotation": _keys((0, ha), (0.3, (-20, 0, 0)), (0.5, (-20, 50, 0)), (1.3, (-20, 50, 0)), (1.4, (-20, 30, 0)),
                                       (1.5, (-20, 50, 0)), (2.0, ha), easing=E),
                     "position": _keys((0, hpa), (0.3, zero), (1.5, zero), (2.0, hpa), easing=E)},
            "tail": {"rotation": _keys((0, t), (0.3, (-12, 0, 0)), (1.5, (-12, 0, 0)), (2.0, t), easing=E)},
            **wings((0, wa), (0.25, (0, 0, -38)), (0.5, (0, 0, -12)), (1.5, (0, 0, -12)), (2.0, wa), easing=E)}),
        # Depart: he crouches and spreads his wings high (the telling pose), springs up and beats away, dwindling to
        # nothing; the last frame holds until the server removes him.
        pacts.GESTURES["depart"]: once(T["depart"], {
            "master": {"position": _keys((0, zero), (0.8, zero), (1.0, (0, 3, 0)), (2.0, (0, 22, 8)), easing=E),
                       "scale": _keys((0, (1, 1, 1)), (1.2, (1, 1, 1)), (2.0, zero))},
            "legs": {"rotation": _keys((0, l), (0.3, _plus(l, (-10, 0, 0))), (0.8, _plus(l, (-10, 0, 0))), (1.0, (30, 0, 0)),
                                       (2.0, (35, 0, 0)), easing=E),
                     "position": _keys((0, lp), (0.3, _plus(lp, (0, -0.5, 0))), (0.8, _plus(lp, (0, -0.5, 0))), (1.0, zero),
                                       (2.0, zero), easing=E)},
            "feets": {"rotation": _keys((0, f), (0.8, f), (1.0, (10, 0, 0)), (1.3, (60, 0, 0)), (2.0, (60, 0, 0)), easing=E)},
            "body": {"rotation": _keys((0, ba), (0.3, _plus(b, (10, 0, 0))), (0.8, _plus(b, (10, 0, 0))), (1.0, (-8, 0, 0)),
                                       (2.0, (6, 0, 0)), easing=E)},
            "head": {"rotation": _keys((0, ha), (0.3, (-10, 0, 0)), (0.8, (-10, 0, 0)), (2.0, (-12, 0, 0)), easing=E),
                     "position": _keys((0, hpa), (0.3, zero), (2.0, zero), easing=E)},
            "tail": {"rotation": _keys((0, t), (0.3, _plus(t, (10, 0, 0))), (0.8, _plus(t, (10, 0, 0))), (1.0, (-15, 0, 0)),
                                       (2.0, (-15, 0, 0)), easing=E)},
            **wings((0, wa), (0.3, (-15, 0, -118)), (0.8, (-15, 0, -122)), (0.95, (-10, 0, -45)), (1.1, (-10, 0, -120)),
                    (1.25, (-10, 0, -45)), (1.4, (-10, 0, -120)), (1.55, (-10, 0, -45)), (1.7, (-10, 0, -120)), (2.0, (-10, 0, -90)))},
            hold=True),
    }
    return clips


def animations(read):
    """The runtime animation file: the owner's five clips as they are, then Stolas's own (sorted by name)."""
    owner = json.loads(read(ANIMATIONS))
    clips = dict(owner["animations"])
    for name, clip in sorted(stolas_clips(owner).items()):
        if name in clips:
            raise SystemExit(f"tools/stolas_body.py: {name} is already one of Flashjack's clips")
        clips[name] = clip
    return {"format_version": owner["format_version"], "animations": clips}
