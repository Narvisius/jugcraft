"""Bloodthorn Armor: the owner's crimson design (a render of a player in crimson plate in two views, a front three-quarter
and the back) as 3D worn models for jugcraft:bloodthorn_helmet, bloodthorn_chestplate, bloodthorn_leggings and
bloodthorn_boots, on the toolkit in tools/armor_models.py (shapes) and tools/armor_paint.py (the atlas).

What the owner drew, and where it is here:
    helmet      a tall boxy great helm, its flat front cut by three black slits (two wide, a narrower one below), its foot
                above the collar with a dark band between; behind the head the upper half of a big rounded back plate,
                from which a fan of square spikes radiates like a crown of thorns: a tall thick one at the centre,
                rising straight, then a long pair and a lower pair splayed wider, all leaning back
    chestplate  a high collar; a breastplate; the lower half of the rounded back plate, narrowing in steps to a lip; tall
                pauldrons tilted up toward the outside, each a main block, a top tier and a flared lame; dark
                under-layer at the shoulders and elbows; vambraces with a diamond plate standing on the outer forearm;
                dark gloves; and a V over the waist, magenta with a near-black stripe round a dark notch
    leggings    a dark belt; a dark under-layer on the thighs; tassets on each thigh (front, outer side and back), rolled
                so their tops dip to a V at the centre line as drawn behind, painted in nested L's pointing down to the
                centre; a front plate on each leg crossed by a dark strap; diamond knee plates; dark under-layer at the
                knees
    boots       chunky plated boots, notched dark at the top edge
Colours: armor_paint.BLOODTHORN, sampled from the render: orange and coral highlights at the top, red and crimson
through the chest, magenta at the arms and thighs, plum and dark plum at the feet, in broken horizontal strips (each
part painted in rows from its own window of the ramp), over a near-black purple under-layer.

What the render does not show is drawn to match it: the sides, closed inner sides of the thighs and boots, so a walking
leg shows dark plate, not the wearer, and the undersides. Every box is closed: a face is left out only where another
box of the same piece and bone covers it (the helm's foot opens into the neck band, a step of the back plate or the
pauldron's cap sits on the box below, the sleeve ends inside the vambrace, the glove under it), so the set shows no
holes alone, on an armor stand's thin limbs (tools/art_check.py, H1).

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left.
Preview: python3 tools/armor_preview.py --set bloodthorn (--compare owner_design.png: its two views beside ours)
"""
from dataclasses import replace

import armor_models as am
from armor_paint import BLOODTHORN, P


def rows(*tones, breaks=False):
    """One tone per texel row, cycling, each row broken into runs of the next tone darker (the owner's strips); with
    `breaks`, a hammer pass also breaks long runs a tone lighter or darker."""
    spec = P("lames", rows=1, tones=tuple((t, t) for t in tones), centre=None)
    return [spec, P("hammer", keep_border=False, every=4)] if breaks else spec


def ells(*bands, corner="in", outside="seam"):
    """Nested L's one texel wide about a lower corner (toward the centre line by default), broken into hammered strips:
    the owner's tassets, whose strips bend into V's at the centre line."""
    return [P("chevron", corner=corner, bands=bands, border=None, core=bands[-2:], outside=outside),
            P("hammer", keep_border=False, every=4)]


# ---------------------------------------------------------------- paint
def paints():
    """Role -> paint spec. Each part paints rows from its own window of the ramp, light at the top of the armor and
    dark at the feet, as the owner's render runs: helm and spikes orange and coral; chest red and crimson; arms
    crimson and magenta; thighs magenta and plum; boots plum to dark plum. "gold" is the design's red."""
    under = P("under")
    slit = "under_darkest"
    return {
        # helm: the flat front plate with three slits, two wide and a narrower one below
        # (eight rows, the slits on the third, fifth and seventh, as on the owner's eight-texel face)
        "helm": {"front": [rows("light", "mid_light", "light", "mid_light", "gold_light", "mid_light", "gold_light",
                                "mid"),
                           P("marks", rects=[(1, 2, 8, 1, slit), (1, 4, 8, 1, slit), (2, 6, 6, 1, slit)])],
                 "top": rows("light", "light", "mid_light"),
                 "*": rows("light", "mid_light", "light", "mid_light", "gold_light", "mid_light", "mid", "mid")},
        # the band round the neck between the helm and the collar: the collar's dark plum inside, as drawn
        "neck": rows("seam", "void"),
        # the back plate: crimson with red strips at the top, magenta and plum toward its foot, a seam down the middle
        "crown": {"back": [rows("gold_light", "mid", "gold_light", "mid", "dark", "mid", "dark"),
                           P("marks", rects=[(5, 1, 1, 9, "seam")])],
                  "top": rows("gold_light"), "*": rows("gold_light", "mid", "mid", "dark")},
        "back": {"back": [rows("dark", "mid", "dark", "seam", "dark", "seam", "void", "seam"),
                          P("marks", rects=[(6, 0, 1, 9, "void")])],
                 "*": rows("mid", "dark", "seam", "dark", "seam")},
        "back_low": {"back": [rows("seam", "void", "seam", "void"), P("marks", rects=[(4, 0, 1, 9, "void")])],
                     "*": rows("seam", "void")},
        # the lip at the foot of the back plate: a magenta strip, as drawn (named faces, or a strip one texel tall would
        # get the painter's coral edge)
        "back_lip": {"back": rows("dark"), "bottom": P("solid", tone="void"), "*": P("solid", tone="dark")},
        # spikes: orange tips, coral and red down the shaft, crimson at the root
        "spike": {"top": P("solid", tone="light"),
                  "*": rows("light", "mid_light", "light", "gold_light", "mid_light", "gold_light", "mid", "gold_light",
                            "mid", "dark", "mid", "dark", "seam")},
        # chest
        # the collar: a coral band over crimson, so it reads apart from the helm above it, and a magenta rim on top
        "collar": {"top": P("solid", tone="dark"), "bottom": P("solid", tone="seam"),
                   "*": rows("mid_light", "mid", "mid")},
        "breast": {"*": rows("mid_light", "gold_light", "mid", "gold_light", "mid", "mid", "dark", "mid", "dark",
                             "dark", "seam")},
        "waist": under,
        "vee": {"front": P("chevron", corner="bl", bands=("dark", "dark", "under_dark", "dark", "dark"), border=None,
                           core=("under_dark", "under_mid"), outside="dark"),
                "bottom": P("solid", tone="seam"), "right": P("solid", tone="seam"), "*": P("solid", tone="dark")},
        # arms
        "pauldron": {"top": rows("light", "light", "mid_light"),
                     "*": rows("light", "mid_light", "light", "mid_light", "gold_light", "mid_light", "mid", "gold_light",
                               "mid", "dark")},
        "cap": {"top": P("solid", tone="light"), "*": rows("light", "mid_light")},
        # the lame's top is a thin ledge where it flares out under the pauldron: crimson, as its top row
        "paul_lame": {"top": P("solid", tone="mid"), "*": rows("mid", "dark", "mid")},
        "sleeve": under,
        "vambrace": {"*": rows("mid", "dark", "gold_light", "dark", "mid", "dark", "seam")},
        "diamond": {"front": [P("solid", tone="gold_light"), P("marks", rects=[(1, 1, 2, 2, "void")])],
                    "*": P("solid", tone="mid")},
        "glove": P("under", light="under_mid"),
        # waist and legs
        "belt": under,
        "breeches": under,
        "tasset": {"front": ells("seam", "void", "dark", "seam", "void", "seam"),
                   "back": ells("seam", "void", "dark", "seam", "void", "seam"),
                   "*": rows("dark", "seam", "void", "seam", "void", "seam", "void")},
        "front_plate": {"front": [rows("dark", "seam", "dark", "seam", "dark", "seam"),
                                  P("marks", rects=[(0, 2, 9, 1, "under_dark")])],
                        "*": P("solid", tone="seam")},
        "knee": {"front": [P("solid", tone="dark"), P("marks", rects=[(1, 1, 1, 1, "void")]),
                           P("marks", rects=[(0, 0, 3, 1, "mid")])],
                 "*": P("solid", tone="seam")},
        # boots
        # the owner's boot tops are notched dark: a V cut into the top edge of the front and back
        "boot": {"front": [rows("dark", "seam", "dark", "seam", "void", "seam", "dark"),
                           P("marks", rects=[(1, 0, 3, 1, "under_dark"), (2, 1, 1, 1, "under_dark"),
                                             (-1, 0, 1, 9, "void")])],
                 "back": [rows("dark", "seam", "dark", "seam", "void", "seam", "dark"),
                          P("marks", rects=[(1, 0, 3, 1, "under_dark"), (2, 1, 1, 1, "under_dark"),
                                            (0, 0, 1, 9, "void")])],
                 "top": P("solid", tone="under_dark"), "bottom": P("solid", tone="void"),
                 # the inner side, seen when the legs part: dark plum
                 "left": rows("seam", "void", "seam", "void"),
                 "*": rows("dark", "seam", "dark", "seam", "void", "seam", "dark")},
    }


# ---------------------------------------------------------------- shape helpers
def rod(name, base, length, section, out, back, **options):
    """A square bar standing on `base` (its bottom centre), tipped `out` degrees toward the model's right (-x) and
    leaning `back` degrees backward; build it on the model's right and mirror it for the left."""
    bx, by, bz = base
    s = section / 2
    return am.box(name, (bx - s, by - length, bz - s), (section, length, section), pivot=base,
                  rotation=(-back, 0, -out), **options)


# ---------------------------------------------------------------- helmet
def helmet(r):
    parts = [
        # the tall boxy great helm, its foot above the chestplate's collar, as drawn: between them a dark band
        # round the neck, the collar's inside (0.15 clear of the hat layer, closed underneath 0.65 below the head, so
        # a turned head never shows the wearer from below)
        am.span("helm", (-4.75, -10.25, -4.75), (4.75, -2.5, 4.75), skip="bottom", paint=r["helm"]),
        am.span("neck", (-4.75, -2.5, -4.75), (4.75, 0.65, 4.75), skip="top", paint=r["neck"]),
        # the upper half of the rounded back plate, its top stepped round under the spikes; the helm's back shows
        # above it between the spikes, as drawn
        am.span("crown", (-5.75, -3.25, 4.9), (5.75, 0.65, 6.4), paint=r["crown"]),
        am.span("crown_mid", (-4.75, -5.0, 4.9), (4.75, -3.25, 6.4), skip="bottom", paint=r["crown"]),
        am.span("crown_top", (-3.25, -6.25, 4.9), (3.25, -5.0, 6.4), skip="bottom", paint=r["crown"]),
        # the spikes: a tall thick one at the centre, rising straight; a long pair; a lower pair splayed wider
        rod("spike_centre", (0.0, -5.5, 5.65), 12.5, 2.75, out=0, back=7, paint=r["spike"]),
    ]
    parts += am.pair(rod("spike_long_right", (-3.75, -3.0, 5.65), 11.0, 2.4, out=19, back=12, paint=r["spike"]))
    parts += am.pair(rod("spike_low_right", (-5.25, -1.0, 5.65), 9.5, 2.4, out=44, back=14, paint=r["spike"]))
    return parts


# ---------------------------------------------------------------- chestplate
def body(r):
    return [
        # the high collar round the helm's foot
        am.span("collar", (-5.0, -1.5, -5.0), (5.0, 0.5, 4.6), paint=r["collar"]),
        am.span("breast", (-4.8, -0.75, -4.4), (4.8, 7.5, 3.5), paint=r["breast"]),
        # the lower half of the rounded back plate, widest at the shoulder blades, narrowing in steps to a lip (each
        # step's top is the step above's bottom face)
        am.span("back", (-6.25, -0.5, 3.6), (6.25, 3.75, 5.9), paint=r["back"]),
        am.span("back_mid", (-5.25, 3.75, 3.6), (5.25, 5.75, 5.65), skip="top", paint=r["back"]),
        am.span("back_low", (-3.75, 5.75, 3.6), (3.75, 7.25, 5.4), skip="top", paint=r["back_low"]),
        am.span("back_lip", (-1.75, 7.25, 3.6), (1.75, 8.0, 5.15), skip="top", paint=r["back_lip"]),
        am.span("waist", (-4.6, 7.0, -2.6), (4.6, 11.0, 2.6), paint=r["waist"]),
        # the V over the waist: a square on its corner, its upper half behind the breastplate, painted in nested
        # V's (magenta, a near-black stripe, magenta) round the dark notch above its point, which meets the top of the
        # leggings' front plates
        am.diamond("vee", (0.0, 7.9, -3.85), 6.5, 0.75, paint=r["vee"]),
    ]


PAULDRON_ROLL = 16.0   # degrees, the outer end up


def arm(r):
    """The right arm: pauldron (main block, top tier and flared lame, all tilted up toward the outside), an
    under-layer sleeve over the shoulder and elbow, vambrace with a diamond plate on its outer face, glove. The pauldron
    starts just outside the collar (body x -5.35, the helm's side is at -4.75), as drawn."""
    tilt = dict(pivot=(-0.35, -5.75, 0.0), rotation=(0, 0, PAULDRON_ROLL))
    diamond = am.turned(am.diamond("diamond_right", (-5.15, 5.75, 0.0), 3.6, 0.75, paint=r["diamond"]), "y", 90,
                        (-5.15, 5.75, 0.0))
    return [
        am.span("pauldron_right", (-5.35, -5.75, -4.0), (-0.35, 0.75, 4.0), paint=r["pauldron"], **tilt),
        am.span("cap_right", (-4.35, -6.75, -3.25), (-0.35, -5.75, 3.25), skip="bottom", paint=r["cap"], **tilt),
        am.hinge(am.span("paul_lame_right", (-5.6, 0.5, -4.25), (-0.35, 2.0, 4.25), paint=r["paul_lame"], **tilt),
                 "top", 6, toward="right", axis="z"),
        am.span("sleeve_right", (-3.45, -2.45, -2.45), (1.45, 3.5, 2.45), skip="bottom", paint=r["sleeve"]),
        am.span("vambrace_right", (-4.75, 2.75, -3.25), (1.75, 8.75, 3.25), paint=r["vambrace"]),
        diamond,
        am.span("glove_right", (-3.6, 8.75, -2.7), (1.6, 10.45, 2.7), skip="top", paint=r["glove"]),
    ]


# ---------------------------------------------------------------- leggings
def waist(r):
    return [am.span("belt", (-4.85, 9.5, -2.85), (4.85, 12.45, 2.85), paint=r["belt"])]


TASSET_ROLL = 15.0   # bottom outward: the tops dip toward the centre line, the V of the owner's back view


LEFT_OUT = 0.12   # the left leg's under-layer and boot are inflated this much, so the two legs' faces never share a plane


def leg(r, side="right"):
    """One leg's leggings parts (the leg box is x -2..2, y 0..12, z -2..2), built for the right leg and mirrored."""
    out = [
        # dark under-layer over the thigh and knee, seen between the tassets, at the slit behind and at the knee; closed
        # all round, its inner side 0.45 off the leg: standing it is inside the other leg's, walking it closes the gap
        # between the legs, where the wearer showed
        am.span("breeches_right", (-2.45, -0.5, -2.45), (2.45, 7.0, 2.45), paint=r["breeches"]),
        am.span("tasset_right", (-3.6, -1.75, -3.5), (1.9, 5.25, 3.5), pivot=(1.9, -1.75, 0),
                rotation=(0, 0, TASSET_ROLL), paint=r["tasset"]),
        am.span("front_plate_right", (0.7, 0.25, -3.75), (1.9, 6.0, -3.25), paint=r["front_plate"]),
        am.diamond("knee_right", (-0.5, 3.75, -4.1), 2.8, 0.5, pitch=6, paint=r["knee"]),
    ]
    if side == "right":
        return out
    return [replace(p, inflate=LEFT_OUT) if p.name.startswith("breeches") else p for p in am.mirror_all(out)]


# ---------------------------------------------------------------- boots
def boot(r):
    """The boot, closed all round: its inner side 0.65 off the leg (clear of vanilla leggings' 0.5 shell) is inside the
    other boot while standing and closes the boot when the legs part. The left one is inflated LEFT_OUT (model())."""
    return [am.span("boot_right", (-3.1, 6.25, -3.4), (2.65, 12.6, 3.1), paint=r["boot"])]


# ---------------------------------------------------------------- the set
def model():
    """{item: {bone: [parts]}} for the four pieces."""
    r = paints()
    a, b = arm(r), boot(r)
    return {
        "bloodthorn_helmet": {"head": helmet(r)},
        "bloodthorn_chestplate": {"body": body(r), "right_arm": a, "left_arm": am.mirror_all(a)},
        "bloodthorn_leggings": {"body": waist(r), "right_leg": leg(r, "right"), "left_leg": leg(r, "left")},
        "bloodthorn_boots": {"right_leg": b, "left_leg": [replace(p, inflate=LEFT_OUT) for p in am.mirror_all(b)]},
    }


SETS = [am.ArmorSet("bloodthorn", BLOODTHORN, model())]
