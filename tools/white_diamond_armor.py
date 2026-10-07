"""Reforged White Diamond: the owner's icy design (a render of a player in the full set from the front, beside small
renders of each piece on its own) as 3D worn models for jugcraft:reforged_white_diamond_helmet, _chestplate,
_leggings and _boots, on the toolkit in tools/armor_models.py (shapes) and tools/armor_paint.py (the atlas).

What the owner drew, and where it is here:
    helmet      a big V crest of two white bars over the brow, leaning back, its point dipping into the face, with a
                light blue peak (a square on its corner) standing behind it; a helm with a T-shaped opening, the
                charcoal face behind it and cheek guards beside the T's stem; two swept wings, square bars rising from
                the cheeks up, out and back past the top of the helm, an outer V round the crest's
    chestplate  wing-like pauldrons, a long plate rolled up toward the outside over a lame; a breastplate in strips
                under a big V, white at its outer edge, with a small gem at the collar in the V's notch; charcoal
                sleeves to the wrist, lit by a grey L
    leggings    a belt; a long skirt of two tassets in an A, rolled out from the centre line, framed white and lined
                lavender, then blue, along their inner edges, over a striped under-skirt that shows between and under
                them
    boots       chunky boots in strips, each with a diamond plate (a square on its corner) at the front: a blue rim
                round pale, white toward the top
Colours: armor_paint.WHITE_DIAMOND, sampled from the render (it is unlit, so its tones are texture colours as they
stand): icy white, pale cyan and light cyan plates in broken strips, nested L's and nested V's, shaded light blue and
blue along lower edges and seams and lavender inside the tassets, over a charcoal under-layer.

The render shows only the front. The sides and back are drawn in its own words: the wings sweep back into a V behind
the head, over a smaller V at the nape; the breastplate's back carries a V and strips; the tassets wrap the legs, so
the A repeats behind; a blue V is notched into the top of each boot's back. The owner's helmet and chestplate
rendered on their own show the face open and no sleeves, so the charcoal face and arms of the full render may be the
figure under the armor; here they are a charcoal plate behind the T and charcoal sleeves, so the set looks as drawn
whatever skin wears it. Every box is closed: a face is left out only where another box of the same piece and bone
covers it (the face frame against the helm, a post between brow and cheek, a V's bars against the square at its
point), so the set shows no holes alone, on an armor stand's thin limbs (tools/art_check.py, H1).

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left.
Preview: python3 tools/armor_preview.py --set reforged_white_diamond (--compare owner_design.png: their front view
beside ours, from a camera fitted to it and in their pose)
"""
from dataclasses import replace

import armor_models as am
from armor_paint import WHITE_DIAMOND, P


def rows(*tones, breaks=False):
    """One tone per texel row, cycling, each row broken into runs of the next tone darker (the owner's strips); with
    `breaks`, a hammer pass also breaks long runs a tone lighter or darker."""
    spec = P("lames", rows=1, tones=tuple((t, t) for t in tones), centre=None)
    return [spec, P("hammer", keep_border=False, every=4)] if breaks else spec


def ells(*bands, corner="bl", core=None):
    """Nested L's one texel wide about a corner, broken into hammered strips: the owner's framed plates (a white L on
    the outer edges, cyan and pale inside). On a tall thin face the L's run its length, as a bar's stripes do."""
    return [P("chevron", corner=corner, bands=bands, border=None, core=core or bands[-2:], outside=bands[0]),
            P("hammer", keep_border=False, every=4)]


def marks(*rects):
    return P("marks", rects=list(rects))


def vee_paint(*tones, under="dark", back=True):
    """(bars, point) paints for vee(): each bar in strips along its length, `tones` from its inner edge to its outer
    edge, the square at its point in nested V's of the same tones, so the strips bend round the point. `back`: the V
    is seen from behind (its back face carries the pattern), else from the front."""
    face, other = ("back", "front") if back else ("front", "back")
    bands = tuple(reversed(tones))
    point = P("chevron", corner="bl" if back else "br", bands=bands, border=None, core=bands[-1:], outside=bands[0])
    bars = {face: rows(*tones), "bottom": P("solid", tone=under), "*": rows(*tones[:2])}
    return bars, {face: point, other: P("solid", tone=tones[0]), "*": P("solid", tone=under)}


# ---------------------------------------------------------------- paint
def paints():
    """Role -> paint spec, the owner's patterns read off their render: icy white, pale cyan and light cyan plates in
    broken strips and nested L's, light blue and blue along lower edges and seams, lavender lining the tassets."""
    recess = P("solid", tone="dark")   # a face turned into the face opening: shaded, so the opening reads deep
    crest, crest_point = vee_paint("light", "mid_light", "mid", back=False)
    chev, chev_point = vee_paint("mid", "mid_light", "light", "light", "void", back=False)
    return {
        # the helm: its front is the charcoal face behind the T opening (only the T shows): a dark square at each top
        # corner of the eye band and a darker stem, as drawn; nested V's down the back
        "helm": {"front": [P("solid", tone="under_mid"),
                           marks((1, 3, 2, 1, "under_dark"), (7, 3, 2, 1, "under_dark"), (4, 8, 2, 2, "under_dark"),
                                 (4, 10, 2, 1, "under_darkest"))],
                 "top": rows("light", "mid_light", "light", "mid_light", "mid"),
                 "back": [P("chevron", corner="v", bands=("light", "mid_light", "mid", "light", "mid_light", "dark"),
                            border=None, core=("mid", "dark"), outside="light"),
                          P("hammer", keep_border=False, every=4)],
                 "bottom": P("solid", tone="under_dark"),
                 "*": rows("light", "mid_light", "mid", "mid_light", "light", "mid", "dark", breaks=True)},
        # the frame round the T: brow, posts and cheek guards, shaded where they face into the opening
        "brow": {"bottom": recess, "*": rows("mid_light", "light", "mid", "light")},
        "post": {"left": recess, "*": rows("mid", "mid_light", "mid", "dark")},
        "cheek": {"left": recess, "top": recess, "bottom": P("solid", tone="seam"),
                  "*": rows("mid_light", "light", "mid_light", "mid")},
        # the V crest: each bar white on its inner edge, pale, then cyan on its outer edge over a light blue underside
        "crest": crest, "crest_point": crest_point,
        # the smaller V at the nape, seen from behind
        "nape": vee_paint("light", "mid_light", "mid")[0], "nape_point": vee_paint("light", "mid_light", "mid")[1],
        # the peak behind the crest: its top corner light blue, nested ^'s of cyan and pale below
        "peak": {"front": ells("dark", "mid", "mid_light", corner="tr", core=("mid_light", "light")),
                 "*": rows("mid", "dark")},
        # the swept wings: strips along their length, white on the outer edge
        "wing": {"ends": rows("mid_light", "mid"), "*": ells("light", "mid_light", "mid", "dark")},
        # chest
        "breast": {"top": rows("mid_light", "light"), "bottom": P("solid", tone="dark"),
                   "*": rows("mid_light", "light", "mid", "mid_light", "light", "mid", "mid_light", "seam", "mid",
                             "mid_light", "light", breaks=True)},
        # the chest V: cyan at its inner edge, pale, then white toward its outer edge, lined lavender, as drawn
        "chev": chev, "chev_point": chev_point,
        "back_chev": vee_paint("mid", "mid_light", "light", "void")[0],
        "back_chev_point": vee_paint("mid", "mid_light", "light", "void")[1],
        # the small diamond at the collar, in the V's notch: light blue round a blue heart
        "gem": {"front": [P("solid", tone="dark"), marks((1, 0, 1, 2, "seam"))], "*": P("solid", tone="mid")},
        # the pauldron wing: a white L along its top and outer end, pale and cyan inside, light blue under it
        "pauldron": {"front": [*ells("light", "light", "mid_light", "mid", corner="tl"), marks((0, -1, 99, 1, "dark"))],
                     "back": [*ells("light", "light", "mid_light", "mid", corner="tr"), marks((0, -1, 99, 1, "dark"))],
                     "top": rows("light", "mid_light", "light", "mid_light"), "bottom": P("solid", tone="seam"),
                     "*": rows("light", "mid_light", "mid", "mid_light", "dark")},
        "lame": {"top": P("solid", tone="seam"), "bottom": P("solid", tone="seam"),
                 "*": rows("light", "mid_light", "light", "mid", "dark", breaks=True)},
        # the charcoal sleeves, lit by a grey L as drawn
        "sleeve": {"front": P("chevron", corner="tl", bands=("under_light", "under_mid", "under_dark", "under_mid"),
                              border=None, core=("under_dark", "under_darkest"), outside="under_mid"),
                   "back": P("chevron", corner="tr", bands=("under_light", "under_mid", "under_dark", "under_mid"),
                             border=None, core=("under_dark", "under_darkest"), outside="under_mid"),
                   "*": P("under")},
        # waist and legs
        "belt": {"*": rows("dark", "light", "mid_light", "mid")},
        # the under-skirt between and under the tassets: pale, cyan and white strips with light blue and blue bands
        "skirt": {"*": rows("seam", "mid_light", "mid", "mid_light", "mid", "light", "mid", "dark", "mid_light",
                            "seam", "mid", "mid_light")},
        # a tasset: a white L along its outer edge and foot, nested cyan and pale inside, light blue along its top, its
        # inner edge lined lavender above and blue below
        "tasset": {"front": [*ells("light", "mid", "mid_light", "light", "mid", corner="bl"),
                             marks((0, 0, 99, 1, "dark"), (-2, 1, 2, 5, "void"), (-2, 6, 2, 99, "seam"))],
                   "back": [*ells("light", "mid", "mid_light", "light", "mid", corner="br"),
                            marks((0, 0, 99, 1, "dark"), (0, 1, 2, 5, "void"), (0, 6, 2, 99, "seam"))],
                   "top": P("solid", tone="dark"), "bottom": P("solid", tone="dark"), "left": P("solid", tone="seam"),
                   "*": rows("light", "mid_light", "mid", "mid_light", "light", "mid", "dark", breaks=True)},
        # boots: strips under a light blue top, a blue V notched into the top of the back
        "boot": {"top": P("solid", tone="dark"), "bottom": P("solid", tone="seam"),
                 "back": [*rows("dark", "mid_light", "light", "mid", "mid_light", "light", "mid", "dark", breaks=True),
                          marks((2, 0, 3, 1, "seam"), (3, 1, 1, 1, "seam"))],
                 "*": rows("dark", "mid_light", "light", "mid", "mid_light", "light", "mid", "dark", breaks=True)},
        # the diamond toe plates: a light blue rim round pale, white toward the top corner
        "toe": {"front": [P("solid", tone="dark"), marks((1, 1, 3, 3, "mid_light"), (2, 1, 2, 2, "light"))],
                "*": P("solid", tone="mid")},
    }


# ---------------------------------------------------------------- shape helpers
def rod(name, base, length, section, out, back, **options):
    """A square bar standing on `base` (its bottom centre), tipped `out` degrees toward the model's right (-x) and
    leaning `back` degrees backward; build it on the model's right and mirror it for the left."""
    bx, by, bz = base
    s = section / 2
    return am.box(name, (bx - s, by - length, bz - s), (section, length, section), pivot=base,
                  rotation=(-back, 0, -out), **options)


def vee(name, apex, length, width, thickness, paint, point, lean=0.0):
    """A V of two bars `width` wide rising 45 degrees to each side of `apex` (the V's point, on the centre line, on
    their front face) to `length` from it, meeting in a square on its corner at the point: where two crossing bars
    overlap, that square is theirs, so here the bars butt against it instead, and the point and the nested V's painted
    on it stay clean (no bar's end crosses the other). The V leans `lean` degrees back about its point. The faces
    where the bars meet the square are left out: each lies on the other."""
    ax, ay, az = apex
    w = width
    bar = am.Part(f"{name}_right", (ax - length, ay - w, az), (length - w, w, thickness), paint=paint, skip="left")
    core = am.Part(f"{name}_point", (ax - w, ay - w, az), (w, w, thickness), paint=point, skip=("right", "top"))
    parts = [am.turned(p, "z", 45.0, apex) for p in (bar, core)]
    if lean:
        parts = [am.turned(p, "x", -lean, apex) for p in parts]
    bar, core = parts
    return [bar, am.mirror(bar, f"{name}_left"), core]


# ---------------------------------------------------------------- helmet
def helmet(r):
    parts = [
        # the helm, 0.75 clear of the head: its front is the charcoal face, framed by the brow, the posts beside the
        # eye band and the cheek guards beside the stem, which leave the T open over it, 1 px deep
        am.span("helm", (-4.75, -9.0, -4.75), (4.75, 1.25, 4.75), paint=r["helm"]),
        am.span("brow", (-4.75, -9.0, -5.75), (4.75, -6.0, -4.75), skip="back", paint=r["brow"]),
        *am.pair(am.span("post_right", (-4.75, -6.0, -5.75), (-3.65, -2.25, -4.75), skip=("back", "top", "bottom"),
                         paint=r["post"])),
        *am.pair(am.span("cheek_right", (-4.75, -2.25, -5.75), (-1.25, 1.25, -4.75), skip="back", paint=r["cheek"])),
        # the V crest, its point dipping into the eye band, leaning back; the peak stands behind it, its lower half
        # hidden by the crest and the brow
        *vee("crest", (0.0, -4.6, -7.35), 10.9, 3.0, 1.5, r["crest"], r["crest_point"], lean=7.5),
        am.diamond("peak", (0.0, -9.05, -5.6), 5.9, 1.0, paint=r["peak"]),
        # behind, a smaller V echoing the crest, its point at the nape
        *vee("nape", (0.0, 0.25, 4.6), 7.0, 2.5, 1.25, r["nape"], r["nape_point"]),
    ]
    # the wings: square bars from the cheeks, sweeping up, out and back past the helm's top corners, kept behind the
    # face plate where they pass the eye band, so the T stays clear
    parts += am.pair(rod("wing_right", (-3.55, -1.3, -3.2), 15.75, 4.0, out=24.0, back=39.9, paint=r["wing"]))
    return parts


# ---------------------------------------------------------------- chestplate
def body(r):
    """The breastplate in strips; the big V over it, its point near the belt, its arms running up under the
    pauldrons (its front 0.125 behind the pauldron wing's, so the wing covers its ends); the gem in the V's notch; a
    V on the back."""
    return [
        am.span("breast", (-5.075, -0.75, -3.075), (5.075, 9.5, 3.075), paint=r["breast"]),
        *vee("chev", (0.0, 9.4, -3.825), 8.5, 4.75, 1.0, r["chev"], r["chev_point"]),
        am.diamond("gem", (0.0, 2.2, -3.2), 2.1, 0.5, paint=r["gem"]),
        *vee("back_chev", (0.0, 9.0, 3.075), 8.0, 3.0, 1.0, r["back_chev"], r["back_chev_point"]),
    ]


WING_ROLL = 20.0   # degrees, the outer end up; the owner's pose adds the arm's 20 degrees out, so it reads as 40


def arm(r):
    """The right arm: the pauldron wing, rolled up toward the outside about its outer lower corner and ending over the
    shoulder; the lame under it; the charcoal sleeve, closed all round the arm from 0.5 above it to the wrist, so the
    arm never shows past the wing's inner end as the arm swings."""
    return [
        am.box("pauldron_right", (-8.4, -6.45, -3.95), (8.25, 4.95, 7.9), pivot=(-8.4, -1.5, 0.0),
               rotation=(0, 0, WING_ROLL), paint=r["pauldron"]),
        am.box("lame_right", (-5.7, 0.05, -3.45), (7.65, 4.7, 6.9), pivot=(1.95, 0.05, 0.0), rotation=(0, 0, 3.0),
               paint=r["lame"]),
        am.span("sleeve_right", (-3.7, -2.5, -2.7), (1.7, 10.7, 2.7), paint=r["sleeve"]),
    ]


# ---------------------------------------------------------------- leggings
TASSET_ROLL = 22.0   # bottom outward, about the inner top corner: the two tassets make the owner's A
LEFT_OUT = 0.12      # the left leg's parts are inflated this much, so the two legs' faces never share a plane


def waist(r):
    return [am.span("belt", (-4.85, 9.5, -2.85), (4.85, 12.45, 2.85), paint=r["belt"])]


def leg(r, side="right"):
    """One leg's leggings parts (the leg box is x -2..2, y 0..12, z -2..2), built for the right leg and mirrored: the
    striped under-skirt, closed all round, and the tasset wrapping the leg's front, outer side and back, its top 1.2
    px above the leg (clear of vanilla armor's 1 px shell as the roll lifts it), its inner top corner at the centre
    line."""
    out = [
        am.span("skirt_right", (-2.45, -0.5, -2.45), (2.45, 11.0, 2.45), paint=r["skirt"]),
        am.box("tasset_right", (-3.4, -1.2, -3.85), (5.4, 11.5, 7.7), pivot=(2.0, -1.2, 0.0),
               rotation=(0, 0, TASSET_ROLL), paint=r["tasset"]),
    ]
    if side == "right":
        return out
    return [replace(p, inflate=LEFT_OUT) for p in am.mirror_all(out)]


# ---------------------------------------------------------------- boots
def boot(r):
    """The boot, flared outward and closed all round, its inner side 0.65 past the leg (clear of vanilla leggings'
    0.5 shell), inside the other boot while standing; the diamond plate on its front. The left one is inflated
    LEFT_OUT (model())."""
    return [am.span("boot_right", (-4.2, 6.0, -3.6), (2.65, 12.75, 3.6), paint=r["boot"]),
            am.diamond("toe_right", (-1.15, 9.3, -3.925), 4.75, 0.75, paint=r["toe"])]


# ---------------------------------------------------------------- the set
def model():
    """{item: {bone: [parts]}} for the four pieces."""
    r = paints()
    a, b = arm(r), boot(r)
    return {
        "reforged_white_diamond_helmet": {"head": helmet(r)},
        "reforged_white_diamond_chestplate": {"body": body(r), "right_arm": a, "left_arm": am.mirror_all(a)},
        "reforged_white_diamond_leggings": {"body": waist(r), "right_leg": leg(r, "right"),
                                            "left_leg": leg(r, "left")},
        "reforged_white_diamond_boots": {"right_leg": b,
                                         "left_leg": [replace(p, inflate=LEFT_OUT) for p in am.mirror_all(b)]},
    }


SETS = [am.ArmorSet("reforged_white_diamond", WHITE_DIAMOND, model())]
