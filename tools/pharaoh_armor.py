"""Pharaoh: the owner's golden design (a render of a player in the full set from the front, titled "PHARAOH") as 3D worn
models for jugcraft:pharaoh_helmet, pharaoh_chestplate, pharaoh_leggings and pharaoh_boots, on the toolkit in
tools/armor_models.py (shapes) and tools/armor_paint.py (the atlas).

What the owner drew, and where it is here:
    helmet      a nemes headdress: a cap striped upright in gold and teal over the brow, a gold brow band across it, and
                on the brow the uraeus, a red gem on a gold diamond; on each side a tall flap in level gold and teal
                stripes from above the crown down to the shoulder, its top a rounded teal hump rising above the crown's
                outer corner, bulging at the temples and coming back in toward the jaw; a lappet hanging beside the face
                in front of each shoulder, gold over a dark teal end beside the chin; the tan face below the brow,
                narrowing in steps to the chin, and a short tan beard. Here: the cap (its top 1.35 above the head, the
                outer texel columns of its front striped level as the flaps, so the upright stripes span the crown
                between them), the brow band round it, the uraeus diamond and its gem; each flap one stack of boxes, the
                hump's three steps rounding off (each higher one narrower and shorter front to back, the lowest sunk
                into the cap's top corner, so the humps, not the crown, are the highest points), then five steps
                bulging at the temples and stepping in to the jaw, the stack turned 6.4 degrees so that its back flares
                out behind the ears, as a nemes does; the lappets in front of the face plate's sides, their ends beside
                the chin; the face plate in four steps, an inverted trapezoid with a short chin step
    chestplate  a broad collar (usekh) of gold and teal with a red gem at its front; a breastplate, a teal square framed
                in gold; teal sides banded gold, tan and gold, dark at the waist; the arms tan, a gold band at the
                shoulder, on each forearm a big teal panel framed in gold with two black bands round the wrist inside it.
                Here: the corselet, the collar box (its front painted in the owner's rings), the gem, the breastplate;
                on each arm the tan sleeve, the gold armlet, the bracer reaching 1.4 past the forearm's outer side and
                the two black bands round the sleeve, which show on its inner side and front between bracer and body
    leggings    a long skirt (shendyt) in level bands, gold, teal, gold, tan, gold, teal, dark gold, darkening to a dark
                teal foot with a near-black block on each leg, each band dipping to a shallow V at the centre line and
                each gold band standing a little proud as the skirt flares to the ground. Here: the gold belt on the
                body; on each leg a teal lame under the belt, then four lames, each wider than the last and rolled 4.5
                degrees about its inner top corner (the V), each a gold band over teal or tan, the hem's foot dark;
                the inner side closed at the centre line by a lining, as the knight's skirt
    boots       not drawn (the skirt reaches the ground), so designed to match: sandal-greaves of tan wraps under a
                gold-rimmed teal cuff, a gold-framed teal plate on the shin like the bracers, a gold ankle strap and a
                gold sandal sole with a raised lip at the toe
Colours: armor_paint.PHARAOH, sampled from the render (it is unlit, so its tones are texture colours as they stand): the
gold ramp from a bright yellow gold to a dark gold-brown, the teal enamel from a light green teal to the near-black of
the bands, the tan linen and the two reds, each part in flat bands as drawn: the gold bands lit toward the outside and
shaded at the centre line, the teal panels lit at the top.

The face and arms of the render are tan. They may be the figure under the armor, as White Diamond's charcoal may be;
here they are armor: a tan face plate (a mask, shaded as drawn: a light band under the brow, darker cheeks and chin) and
tan linen sleeves. So the helm is closed, and the set looks as drawn whatever skin wears it and on an armor stand.

The render shows only the front, seen a little from above, the arms 12 degrees out and the head turned 13 degrees (the
head's front features sit right of the body's centre line and its flaps and humps left of it, as a turned head shows
flaps that reach back behind the ears). Its chest gem, breastplate and uraeus sit about a texel right of centre; here
they are centred. The sides and back are drawn in the design's own words: the flaps' stripes run round them; behind, the
headcloth gathers at the nape into the classic tail, a broad knot over a short queue in gold wraps; the collar's gold
ring runs round the neck; a framed back plate mirrors the breastplate; the skirt's bands and their V repeat behind;
the bracers are framed on every face. Every box is closed: a face is left out only where another box of the same piece
and bone covers it (the face plate's back on the cap, a flap step's top or foot where the step above or below covers it,
the knot's front on the cap's back, the breastplate's back on the corselet, each lame's foot inside the next), so the
set shows no holes alone, on an armor stand's thin limbs (tools/art_check.py, H1).

The measures are in G, fitted to the owner's front view from a camera fitted to it (scratch tools, not kept): the cap,
the flaps and their humps, the uraeus, the face, the lappets, the bracers; the collar, breastplate and skirt bands were
read off it with that camera. A box is its own plate; the stripes, rings, frames and bands are paint, one texel per
model pixel.

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left.
Preview: python3 tools/armor_preview.py --set pharaoh (--compare owner_design.png: their front view beside ours, from a
camera fitted to it and in their pose)
"""
import math
from dataclasses import replace

import armor_models as am
from armor_paint import PHARAOH, P


def rows(*tones):
    """One tone per texel row, cycling, each row broken into runs of the next tone darker."""
    return P("lames", rows=1, tones=tuple((t, t) for t in tones), centre=None)


def marks(*rects):
    return P("marks", rects=list(rects))


def solid(tone):
    return P("solid", tone=tone)


# The palette's names (armor_paint.PHARAOH): the gold is its metal, the teal enamel its leather, the tan linen its
# under-layer, the two reds its gold.
Y, G_, g, b = "light", "mid_light", "mid", "dark"                                         # bright gold to dark gold-brown
A, a, d, e, K = "leather_light", "leather_mid_light", "leather_mid", "leather_dark", "leather_darkest"   # teal to black
T, t, s, n = "under_light", "under_mid", "under_dark", "under_darkest"                    # tan
R, q = "gold_light", "gold_dark"                                                          # the gems' red

# A band's three tones, outer to inner: the owner's skirt bands are lit toward the outside and shaded at the centre line.
BANDS = {"gold": (Y, G_, g), "dark_gold": (g, g, b), "teal": (d, d, e), "tan": (s, n, n)}


def band_face(names, back=False, extra=()):
    """A lame's front (or `back`): one band a texel row (BANDS), in the band's first tone, its second two texels from
    the centre line and its third on the two texels at the centre line, as the owner's bands darken toward the middle.
    Texture-left is the outer side on the front of the right leg's lame (the left leg's mirrors it) and the inner side
    on its back."""
    rects = []
    for y, name in enumerate(names):
        outer, mid, inner = BANDS[name]
        if back:
            rects += [(0, y, 2, 1, inner), (2, y, 2, 1, mid), (4, y, 99, 1, outer)]
        else:
            rects += [(0, y, 99, 1, outer), (-4, y, 2, 1, mid), (-2, y, 2, 1, inner)]
    return [solid(BANDS[names[0]][0]), marks(*rects, *extra)]


def lame(*names, extra=(), extra_back=()):
    """A skirt lame: its bands on the front and back, its top (the ledge where it steps out under the lame above) in
    its first band's middle tone, its sides in plain rows."""
    return {"front": band_face(names, extra=extra), "back": band_face(names, back=True, extra=extra_back),
            "top": solid(BANDS[names[0]][1]), "bottom": solid(e), "*": rows(*(BANDS[n][0] for n in names))}


def framed(inner="right"):
    """A framed panel, as the owner's bracers: teal, lit toward its top and shaded toward its foot, under a gold top and
    over a gold foot, with a gold bar along its `inner` edge (texture "left" or "right"; None: along both)."""
    bars = {"right": [(-1, 0, 1, 99, G_)], "left": [(0, 0, 1, 99, G_)], None: [(0, 0, 1, 99, G_), (-1, 0, 1, 99, G_)]}
    return [solid(A), marks((0, 2, 99, 1, a), (0, 3, 99, 99, d), (0, 0, 99, 1, Y), (0, -1, 99, 1, G_), *bars[inner])]


# ---------------------------------------------------------------- paint
def paints():
    """Role -> paint spec, the owner's patterns read off their render: upright stripes on the crown, level ones on the
    side flaps, lappets and skirt, rings on the collar, gold frames round teal panels, the tan face shaded as drawn."""
    return {
        # the cap: the crown's upright stripes, gold and teal (the widest teal in the middle), over the top too, its
        # front's and top's outer texel columns striped as the flaps beside them; the sides and back in level stripes
        "cap": {"front": [solid(Y), marks((2, 0, 1, 99, a), (4, 0, 2, 99, A), (7, 0, 1, 99, a), (0, 1, 1, 99, A),
                                          (-1, 1, 1, 99, A), (0, -3, 99, 2, G_), (0, -1, 99, 1, g))],
                "top": [solid(Y), marks((2, 0, 1, 99, a), (4, 0, 2, 99, A), (7, 0, 1, 99, a), (0, 0, 1, 99, A),
                                        (-1, 0, 1, 99, A))],
                "bottom": solid(d),
                "*": rows(Y, A, a, Y, G_, A, a, d, Y, a, d)},
        # the brow band: lit along its top, darker gold below
        "brow": {"top": solid(Y), "bottom": solid(g), "*": rows(Y, g)},
        # the uraeus: a gold diamond lit on its upper edges, shaded on its lower ones; its gem red, lit at the top
        "uraeus": {"front": [solid(G_), marks((0, 0, 99, 1, Y), (0, 0, 1, 99, Y), (0, -1, 99, 1, g), (-1, 0, 1, 99, g))],
                   "*": solid(g)},
        "gem": {"front": [solid(q), marks((0, 0, 2, 1, R))], "*": solid(q)},
        # the face plate: tan, a light band under the brow, darker cheeks below it, the steps toward the chin darker
        # still; the beard tan with a light foot
        "face_0": {"front": [solid(t), marks((1, 1, 7, 1, T), (0, 2, 2, 1, s), (-2, 2, 2, 1, s))], "*": solid(s)},
        "face_1": {"front": [solid(t), marks((0, -1, 99, 1, s), (0, 0, 1, 99, s), (-1, 0, 1, 99, s))],
                   "bottom": solid(n), "*": solid(s)},
        "face_2": {"front": solid(s), "bottom": solid(n), "*": solid(s)},
        "face_3": {"front": solid(s), "*": solid(n)},
        "beard": {"front": [solid(s), marks((0, -1, 99, 1, T))], "*": solid(n)},
        # the side flaps in level stripes, as drawn, a texel row each: the hump teal, lit on its top and its steps'
        # ledges, darker toward its foot; below it a gold stripe, teal, the gold at the brow over dark gold, teal, gold,
        # and teal over dark gold at the jaw; each step's ledge in its first stripe's tone
        "hump_0": {"top": solid(A), "sides": solid(A), "bottom": solid(d)},
        "hump_1": {"top": solid(A), "sides": solid(a), "bottom": solid(d)},
        "hump_2": {"top": solid(A), "sides": rows(a, d), "bottom": solid(e)},
        "flap_0": {"top": solid(Y), "sides": rows(Y, A), "bottom": solid(g)},
        "flap_1": {"top": solid(A), "sides": solid(a), "bottom": solid(e)},
        "flap_2": {"top": solid(Y), "sides": rows(Y, g, A), "bottom": solid(g)},
        "flap_3": {"top": solid(A), "sides": rows(a, Y), "bottom": solid(g)},
        "flap_4": {"top": solid(a), "sides": rows(A, g), "bottom": solid(e)},
        # the lappets: gold in level stripes under a light teal top; their ends dark teal, lit along the top
        "lappet": {"top": solid(A), "bottom": solid(b), "*": [rows(G_, Y, g), marks((0, 0, 1, 1, d))]},
        "lappet_end": {"top": solid(a), "bottom": solid(e),
                       "*": [solid(d), marks((0, 0, 99, 1, a), (0, -1, 99, 1, e), (1, 1, 2, 1, e))]},
        # the tail: the knot a teal panel framed gold, the queue wrapped gold over teal, a dark gold tip
        "knot": {"back": [solid(A), marks((0, 0, 99, 1, Y), (0, -1, 99, 1, G_), (1, 1, 1, 99, a), (-2, 1, 1, 99, a))],
                 "top": solid(Y), "bottom": solid(g), "*": rows(Y, A, a, G_)},
        "tail": {"top": solid(g), "bottom": solid(g), "sides": [solid(a), marks((0, 0, 99, 1, Y))]},
        "tail_tip": {"top": solid(g), "bottom": solid(b), "sides": solid(G_)},
        # chest: the corselet teal, its sides banded gold, tan and gold, dark at the waist, as drawn round the
        # breastplate
        "corselet": rows(a, a, a, a, a, a, d, g, t, G_, d, e),
        # the usekh: teal, a gold ring round the neck and the gem, a dark gold rim along its foot and over the shoulders
        "collar": {"front": [solid(a), marks((3, 1, 4, 1, G_), (2, 2, 6, 1, Y), (0, 3, 1, 1, d), (-1, 3, 1, 1, d),
                                             (3, 3, 1, 1, Y), (-4, 3, 1, 1, G_), (0, 4, 2, 1, g), (-2, 4, 2, 1, g),
                                             (2, 4, 2, 1, A), (-4, 4, 2, 1, A))],
                   "back": [solid(a), marks((2, 1, 6, 1, Y), (0, -1, 99, 1, g))],
                   "top": rows(a, A, Y, a, A, Y, a), "bottom": solid(g),
                   "*": [solid(a), marks((0, 2, 99, 1, Y), (0, -1, 99, 1, g))]},
        "chest_gem": {"front": [solid(q), marks((0, 0, 1, 1, R))], "*": solid(q)},
        # the breastplate: a teal square framed gold, lit along the top, a darker row across its middle, the frame's
        # foot shaded; the back plate the same, seen from behind
        "breast": {"front": [solid(Y), marks((1, 1, 4, 4, A), (1, 1, 1, 4, a), (-2, 1, 1, 4, a), (1, 3, 4, 1, d),
                                             (0, -1, 99, 1, G_), (-1, 1, 1, 4, G_))],
                   "*": solid(G_)},
        "back_plate": {"back": [solid(Y), marks((1, 1, 4, 4, A), (1, 1, 1, 4, a), (-2, 1, 1, 4, a), (1, 3, 4, 1, d),
                                                (0, -1, 99, 1, G_), (0, 1, 1, 4, G_))],
                       "*": solid(G_)},
        # arms: the tan linen sleeve in wraps; the gold armlet; the bracer framed on every face, its gold bar broadest
        # on the inner side as drawn; the black bands
        "sleeve": rows(t, T, t, s),
        "armlet": rows(Y, G_, g),
        "bracer": {"front": framed(inner="right"), "back": framed(inner="left"), "right": framed(inner=None),
                   "top": solid(Y), "bottom": solid(g), "*": rows(Y, A, a, A, a, G_)},
        "band": solid(K),
        # waist and legs: the gold belt, darker at its ends; the lining dark teal, its thin edges too; the lames'
        # bands (BANDS)
        "belt": [rows(G_, G_), marks((0, 0, 1, 99, g), (-1, 0, 1, 99, g))],
        "lining": {"sides": rows(d, e), "ends": solid(e)},
        "lame_0": {"front": band_face(("teal", "teal", "teal")), "back": band_face(("teal", "teal", "teal"), back=True),
                   "top": solid(d), "*": rows(d, d, e)},
        "lame_1": lame("gold", "teal", "teal"),
        "lame_2": lame("gold", "tan", "tan"),
        "lame_3": lame("gold", "teal", "teal"),
        # the hem: the dark gold band over the dark teal foot, a near-black block on each leg as drawn
        "hem": lame("dark_gold", "teal", "teal", "teal", extra=((3, 1, 2, 3, K),), extra_back=((-5, 1, 2, 3, K),)),
        # boots: the greave's tan wraps, the teal cuff with its gold rim on top, the framed shin plate, the gold strap
        # and sole. The cuff's and the strap's sides are a texel tall, so they are named: a patterned paint there (as
        # through "*") would turn into armor_paint's plain mid-gold edge
        "greave": {"top": solid(s), "bottom": solid(n), "*": rows(t, T, t, s, t, s)},
        "cuff": {"top": solid(Y), "bottom": solid(g), "sides": solid(A)},
        "shin": {"front": framed(inner=None), "*": solid(G_)},
        "strap": {"top": solid(Y), "bottom": solid(g), "sides": solid(Y)},
        "sole": {"top": solid(Y), "bottom": solid(b), "*": rows(G_, g)},
        "toe": solid(Y),
    }


# ---------------------------------------------------------------- measures, fitted to the owner's front view
G = dict(
    # the cap (the headcloth over the head): its top, half width and back
    cap_top=-9.35, cap_hw=4.7, cap_back=5.0,
    # the brow band, its top and foot
    brow_top=-7.25, brow_bot=-5.5,
    # the face plate, narrowing in steps to the chin: (half width, foot) of each step from the brow down
    face=((4.4, -2.5), (3.4, -0.75), (2.2, 0.25), (1.25, 0.6)),
    # the uraeus on the brow: the diamond's centre and side; the gem's top and foot
    ur_y=-6.3, ur_side=3.55, gem_top=-7.4, gem_bot=-5.6,
    # the side flaps, each one mass from its hump down to the shoulder, in its own frame before it turns: front and back
    # z, the turn (degrees) that swings its back out about its inner front corner, so it flares behind the ears, and
    # its inner x (inside the cap, so that turned it still meets the cap's side at the cap's back); the hump's top, then
    # its steps rounding off, (inner x, outer x, foot, inset at front and back) each, the lowest sunk into the cap's top
    # corner; then the flap's steps, (outer x, foot) each, bulging at the temples and coming back in toward the jaw
    flap_front=-2.0, flap_back=6.5, flap_turn=6.4, flap_in=3.9, hump_top=-11.6,
    hump=((3.85, 6.0, -10.75, 2.25), (3.45, 6.3, -10.05, 0.65), (2.1, 6.8, -9.0, 0.0)),
    flap=((6.8, -7.4), (7.35, -6.4), (7.65, -4.25), (7.15, -2.5), (6.85, -1.25)),
    # the lappets in front of the face plate's sides: outer and inner x, top, front and back z; their ends beside the
    # chin: outer and inner x, top, foot, front z
    lap_out=6.4, lap_in=3.65, lap_top=-5.0, lap_front=-5.75, lap_back=-1.0,
    end_out=6.0, end_in=2.4, end_top=-2.0, end_bot=0.8, end_front=-6.25,
    # the bracers (arm space, the right arm): outer and inner x, top and foot
    br_out=4.4, br_in=0.25, br_top=3.0, br_bot=9.5,
)


# ---------------------------------------------------------------- helmet
def helmet(r, g=G):
    """The nemes. The cap 0.7 clear of the head's sides and 0.75 of its front, closed 0.65 below it (clear of the hat
    layer), so a turned head never shows the wearer from below; the face plate on its front in steps, its back against
    the cap."""
    hw = g["cap_hw"]
    parts = [
        am.span("cap", (-hw, g["cap_top"], -4.75), (hw, 0.65, g["cap_back"]), paint=r["cap"]),
        am.span("brow", (-hw - 0.25, g["brow_top"], -5.0), (hw + 0.25, g["brow_bot"], g["cap_back"] + 0.25),
                paint=r["brow"]),
        am.span("beard", (-0.85, 0.25, -5.35), (0.85, 1.25, -4.6), paint=r["beard"]),
        am.diamond("uraeus", (0.0, g["ur_y"], -5.3), g["ur_side"], 0.85, paint=r["uraeus"]),
        am.span("gem", (-1.0, g["gem_top"], -6.25), (1.0, g["gem_bot"], -5.725), skip="back", paint=r["gem"]),
    ]
    # the face plate, an inverted trapezoid: full width under the brow, then narrower steps to a short chin; each
    # step's top lies under the one above
    top = g["brow_bot"]
    for i, (half, foot) in enumerate(g["face"]):
        parts.append(am.span(f"face_{i}", (-half, top, -5.5), (half, foot, -4.75), skip=("back", "top") if i else "back",
                             paint=r[f"face_{i}"]))
        top = foot
    # a side flap, one mass from the hump down to the shoulder: the hump's steps rounding off from its top, the lowest
    # sunk into the cap's top corner (so the hump, not the crown, is the highest point), then the flap bulging at the
    # temples and stepping in toward the jaw; a step's top or foot is left out where the step above or below covers
    # it. All of it turns together about its inner front corner, flaring the back out; the flap's inner side starts
    # inside the cap so that it still meets the cap's side at the back
    front, back, turn, inner = g["flap_front"], g["flap_back"], g["flap_turn"], g["flap_in"]
    steps, top = [], g["hump_top"]       # (name, inner x, outer x, top, foot, front z, back z), top to bottom
    for i, (hin, out, foot, inset) in enumerate(g["hump"]):
        steps.append((f"hump_{i}", hin, out, top, foot, front + inset, back - inset))
        top = foot
    for i, (out, foot) in enumerate(g["flap"]):
        steps.append((f"flap_{i}", inner, out, top, foot, front, back))
        top = foot

    def covers(a, b):       # whether step a's footprint holds step b's (so a hides b's face against it)
        return a[1] <= b[1] and a[2] >= b[2] and a[5] <= b[5] and a[6] >= b[6]

    flap = []
    for i, (name, hin, out, top, foot, z0, z1) in enumerate(steps):
        skip = [face for face, other in (("top", steps[i - 1] if i else None),
                                         ("bottom", steps[i + 1] if i + 1 < len(steps) else None))
                if other is not None and covers(other, steps[i])]
        flap.append(am.span(f"{name}_right", (-out, top, z0), (-hin, foot, z1), skip=tuple(skip), paint=r[name],
                            pivot=(-inner, 0.0, front), rotation=(0.0, -turn, 0.0)))
    # the lappet in front of the face plate's side, and its end beside the chin, standing on the shoulder in front of
    # the collar (0.25 clear of it)
    lappet = am.span("lappet_right", (-g["lap_out"], g["lap_top"], g["lap_front"]),
                     (-g["lap_in"], g["end_top"] + 0.25, g["lap_back"]), paint=r["lappet"])
    end = am.span("lappet_end_right", (-g["end_out"], g["end_top"], g["end_front"]),
                  (-g["end_in"], g["end_bot"], -3.85), paint=r["lappet_end"])
    for part in flap + [lappet, end]:
        parts += am.pair(part)
    # behind, the headcloth gathered at the nape into the tail: a broad knot on the cap's back, then the queue in gold
    # wraps, short, so a head looking up swings it only a little way toward the back
    parts += [am.span("knot", (-2.5, -3.0, 5.0), (2.5, 0.5, 6.35), skip="front", paint=r["knot"]),
              am.span("tail", (-1.5, 0.5, 5.15), (1.5, 2.25, 6.15), paint=r["tail"]),
              am.span("tail_tip", (-1.0, 2.25, 5.3), (1.0, 3.25, 6.0), paint=r["tail_tip"])]
    return parts


# ---------------------------------------------------------------- chestplate
def body(r, g=G):
    """The corselet, 0.85 off the body (clear of vanilla leggings' 0.5 shell), ending above the belt; the collar box
    round the neck and over the shoulders; the gem at the collar's front; the breastplate under the collar and, behind,
    its twin."""
    return [
        am.span("corselet", (-4.85, -0.75, -2.85), (4.85, 11.1, 2.85), paint=r["corselet"]),
        am.span("collar", (-5.0, -1.0, -3.6), (5.0, 4.0, 3.6), paint=r["collar"]),
        am.span("chest_gem", (-1.0, 2.0, -4.1), (1.0, 3.85, -3.6), skip="back", paint=r["chest_gem"]),
        am.span("breast", (-2.75, 4.0, -3.45), (2.75, 10.15, -2.85), skip=("back", "top"), paint=r["breast"]),
        am.span("back_plate", (-2.75, 4.0, 2.85), (2.75, 10.15, 3.45), skip=("front", "top"), paint=r["back_plate"]),
    ]


def arm(r, g=G):
    """The right arm: the tan sleeve, closed all round from 0.45 above the arm to the wrist; the gold armlet at the
    shoulder; the bracer on the forearm's outer side, from its inner bar 0.25 inside the arm's middle to 1.4 past its
    outer side; the two black bands round the sleeve, 0.25 proud of it, showing on its inner side and its front inside
    the bracer."""
    return [
        am.span("sleeve_right", (-3.45, -2.45, -2.45), (1.45, 10.45, 2.45), paint=r["sleeve"]),
        am.span("armlet_right", (-4.0, -2.85, -3.0), (2.0, -0.6, 3.0), paint=r["armlet"]),
        am.span("bracer_right", (-g["br_out"], g["br_top"], -3.0), (-g["br_in"], g["br_bot"], 3.0), paint=r["bracer"]),
        am.span("band_right", (-3.7, 5.1, -2.7), (1.7, 6.3, 2.7), paint=r["band"]),
        am.span("band_low_right", (-3.7, 7.0, -2.7), (1.7, 8.2, 2.7), paint=r["band"]),
    ]


# ---------------------------------------------------------------- leggings
LAME_IN = 2.1        # the lames' inner edge, leg x: past the centre line (1.9), so the two legs' lames close the V
ROLL = 4.5           # degrees, bottom outward about the inner top corner: the tops dip toward the centre line
LEFT_OUT = 0.12      # the left leg's lames (but the first) and its boot stand this much further out, so the two halves
                     # never share a plane where they overlap at the centre line
LAMES = [  # (top y, outer x, depth out from the leg's front and back, height, paint), the owner's gold bands: each lame
    # steps out far enough that the roll leaves its outer face 0.1 clear of the one above's, and its top is inside it
    (1.9, -3.5, 1.25, 2.8, "lame_1"), (4.4, -3.85, 1.55, 2.3, "lame_2"), (6.4, -4.35, 1.8, 2.7, "lame_3"),
    (8.8, -4.7, 2.1, 3.75, "hem")]


def waist(r, g=G):
    """The gold belt, 0.7 off the body (between the corselet's 0.85 and the sleeves' 0.45, clear of vanilla armor's
    shells), showing below the corselet; closed underneath 0.65 below the body, so a sneaking body's tipped underside
    shows gold, not the wearer."""
    return [am.span("belt", (-4.7, 10.75, -2.7), (4.7, 12.65, 2.7), paint=r["belt"])]


def leg(r, g=G, side="right"):
    """One leg's leggings parts (the leg box is x -2..2, y 0..12, z -2..2), built for the right leg and mirrored: the
    lining closing the leg's inner side 0.5 off it, from inside the belt to the hem (it covers the rolled lames' inner
    sides, which pass close to the leg); the first lame under the belt, unrolled, its inner side on the centre line and
    its front between the belt's and the sleeves' planes; then the four rolled lames, the hem closed at its foot."""
    out = [am.span("lining_right", (1.9, -0.5, -2.45), (2.5, 12.4, 2.45), skip="right", paint=r["lining"]),
           am.span("lame_0_right", (-2.55, -0.65, -2.57), (1.9, 2.2, 2.57), skip="bottom", paint=r["lame_0"])]
    for i, (top, outer, depth, h, paint) in enumerate(LAMES):
        last = i == len(LAMES) - 1
        out.append(am.span(f"lame_{i + 1}_right", (outer, top, -2 - depth), (LAME_IN, top + h, 2 + depth),
                           pivot=(LAME_IN, top, 0), rotation=(0, 0, ROLL), paint=r[paint],
                           skip=() if last else "bottom"))
    if side == "right":
        return out
    left = []
    for p in am.mirror_all(out):
        if p.name.startswith("lame_") and not p.name.startswith("lame_0"):
            p = replace(p, origin=(p.origin[0], p.origin[1], p.origin[2] - LEFT_OUT),
                        size=(p.size[0], p.size[1], p.size[2] + 2 * LEFT_OUT))
        left.append(p)
    return left


# ---------------------------------------------------------------- boots
def boot(r, g=G):
    """The boot, hidden under the skirt standing (the owner's render shows none): a sandal-greave in the set's own
    words. The tan greave wrapped round the shin, closed all round, 0.65 off the leg (clear of vanilla leggings' 0.5
    shell); a gold-rimmed teal cuff at its top; a framed teal plate on the shin, as the bracers; a gold strap round the
    ankle; a gold sandal sole, longer at the toe, with a raised lip at its front. The left one is inflated LEFT_OUT
    (model())."""
    return [am.span("greave_right", (-2.65, 6.0, -2.65), (2.65, 11.25, 2.65), paint=r["greave"]),
            am.span("cuff_right", (-2.95, 5.5, -2.95), (2.95, 6.5, 2.95), paint=r["cuff"]),
            am.span("shin_right", (-1.75, 6.75, -3.15), (1.75, 10.0, -2.65), skip="back", paint=r["shin"]),
            am.span("strap_right", (-2.9, 10.0, -2.9), (2.9, 10.75, 2.9), paint=r["strap"]),
            am.span("sole_right", (-2.9, 11.25, -3.6), (2.9, 12.65, 2.9), paint=r["sole"]),
            am.span("toe_right", (-2.4, 10.75, -3.85), (2.4, 11.4, -3.1), paint=r["toe"])]


# ---------------------------------------------------------------- the set
def model(g=G):
    """{item: {bone: [parts]}} for the four pieces."""
    r = paints()
    a, b = arm(r, g), boot(r, g)
    return {
        "pharaoh_helmet": {"head": helmet(r, g)},
        "pharaoh_chestplate": {"body": body(r, g), "right_arm": a, "left_arm": am.mirror_all(a)},
        "pharaoh_leggings": {"body": waist(r, g), "right_leg": leg(r, g, "right"), "left_leg": leg(r, g, "left")},
        "pharaoh_boots": {"right_leg": b, "left_leg": [replace(p, inflate=LEFT_OUT) for p in am.mirror_all(b)]},
    }


SETS = [am.ArmorSet("pharaoh", PHARAOH, model())]
