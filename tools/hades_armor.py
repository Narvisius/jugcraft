"""Hades Armor: the owner's underworld design (a render of a player in the full set from the front, beside small renders
of each piece on its own) as 3D worn models for jugcraft:hades_helmet, hades_chestplate, hades_leggings and hades_boots,
on the toolkit in tools/armor_models.py (shapes) and tools/armor_paint.py (the atlas).

What the owner drew, and where it is here:
    helmet      a helm with a pointed visor like a beak: two faces at 45 degrees meeting in a ridge in front of the
                crown, a V brow across them from their outer top corners down to a point on the ridge, slits on each
                face stepping down toward the ridge, the beak's foot cut into a chin and two cheeks; two big horns from
                the helm's front top corners, each a block leaning out and forward and a second one leaning in from its
                top, so the tips curve toward each other, a light band round the foot of each upper block. Here: the
                crown (9.7 wide, over the hat layer), the beak (one square block turned 45 degrees, its back half inside
                the crown) painted with the V's blue band, the dark above it and three slits each side, a raised V brow
                bar on each face, an angled cheek plate on the foot of each face and a chin bar under the ridge, the
                dark collar showing between them; the horns' two blocks
    chestplate  angular layered pauldrons: a plate rising toward the outside from beside the neck, a fin under it
                reaching further out, both edged; a steep blue V over the chest, its point low on the breastplate; the
                breastplate in slate strips inside a light frame; dark under-layer at the arms; the blood-red cloth
                hanging below the breastplate. Here, standing as the owner's chestplate on its own: on each arm the
                mantle and the fin rising toward the outside (14 and 10 degrees), the dark sleeve showing under them, a
                framed vambrace with a guard flaring out toward the elbow; the V of two bars lapped at the point under
                a small cap; the tabard's upper strip, from under the V's point down to the waist, below the breastplate
    leggings    a layered plate skirt flaring out: a tasset over each thigh, its long edges sloping down toward the
                outside, its inner edge one light arm of a V at the centre with a small dark diamond in the V's notch;
                a lower plate on each side flaring out to the hem in vertical strips; the red cloth hanging from under
                the V over the dark under-layer, long and narrow, ragged at its end. Here: the dark belt and the diamond
                (on the body), and per leg the tasset, the lower plate, the dark under-skirt and the tabard's lower
                strip, in two columns of different lengths on each leg, almost to the hem
    boots       not drawn (the render cuts them off under a lit rim), but the striped plates between the skirt's lower
                plates in the full view are their fronts: tall boots in light and slate strips under a lit rim, a toe
                cap with the set's V and a heel plate
Colours: armor_paint.HADES, sampled from the render (it is unlit, so its tones are texture colours as they stand): cool
greys from a light grey edge to the darkest slate, the blue slate of the V's and the strips, the blue black of the
under-layer and the cloth's three reds; mostly the dark slates, as the owner's, light grey only on the plates' edges.

The full render shows only the front, from a little above and to the model's right, the arms raised 25 degrees and the
head straight; the pieces on their own stand. The pauldrons follow the pieces on their own, rising standing; with the
arms raised they rise 25 degrees more, where the full render's fins stand level. The sides and back are drawn in the
design's own words: the brow's V repeats as nested V's on the helm's back, over a nape guard flared back from the
collar; a second V of bars on the back of the breastplate; tassets behind the thighs too, so the skirt's V repeats on
the back, over a striped apron; the lower plates wrap the sides of the legs; a heel plate on each boot. Every box is
closed: a face is left out only where another box of the same piece and bone covers it (the collar's top inside the
crown), so the set shows no holes alone, on an armor stand's thin limbs (tools/art_check.py, H1).

The measures are in G, fitted to the owner's renders from cameras fitted to them (scratch tools, not kept): the horns,
the helm and its beak, the pauldron plates, the vambraces and the skirt. A box is its own plate; the V's, slits, frames
and strips are paint, one texel per model pixel.

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left.
Preview: python3 tools/armor_preview.py --set hades (--compare owner_design.png: their front view beside ours, from a
camera fitted to it and in their pose)
"""
import math
from dataclasses import replace

import armor_models as am
from armor_paint import HADES, P


def rows(*tones, breaks=False):
    """One tone per texel row, cycling, each row broken into runs of the next tone darker (the owner's strips); with
    `breaks`, a hammer pass also breaks long runs a tone lighter or darker."""
    spec = P("lames", rows=1, tones=tuple((t, t) for t in tones), centre=None)
    return [spec, P("hammer", keep_border=False, every=4)] if breaks else spec


def ells(*bands, corner="bl", core=None, outside=None):
    """Nested L's one texel wide about a corner, broken into hammered strips."""
    return [P("chevron", corner=corner, bands=bands, border=None, core=core or bands[-2:], outside=outside or bands[0]),
            P("hammer", keep_border=False, every=4)]


def edged(corner, light="mid_light", inner="mid", core=("seam", "gold_light", "dark", "seam")):
    """A plate in the owner's manner: one light L along two edges (about `corner`), a pinkish grey line inside it,
    then slate and blue, in hammered strips."""
    return ells(light, inner, *core, corner=corner, core=core[-2:])


def marks(*rects):
    return P("marks", rects=list(rects))


SLATE = ("seam", "dark", "gold_light", "seam", "dark", "void", "gold_light", "gold_dark")   # the owner's slate strips


def beak_face(w=7, h=8, ridge="right"):
    """A face of the beak, its ridge at texture-right (or left), in the owner's dark slate: per column, from the outer
    edge to the ridge, the V's blue band (under the brow) stepping down toward the ridge, dark slate above it; below
    it three slits in the darkest slate stepping down with it (the outer one an L), the ridge's edge blue from the
    band down, a dark foot."""
    def at(x, sw=1):
        return x if ridge == "right" else w - x - sw
    rects = []
    for c in range(w):
        b = round(c * 3 / (w - 1))
        for r in range(b):
            rects.append((at(c), r, 1, 1, "seam" if r < b - 1 else "void"))
        rects.append((at(c), b, 1, 1, "gold_light"))
    rects += [(at(w - 1), 4, 1, h - 5, "gold_dark"), (0, h - 1, w, 1, "seam")]
    for x, y, sw, sh in ((0, 2, 2, 1), (1, 3, 1, 2), (3, 4, 1, 2), (5, 5, 1, 2)):
        rects.append((at(x, sw), y, sw, sh, "void"))
    rects += [(at(2), 4, 1, 2, "seam"), (at(4), 5, 1, 2, "seam")]
    return [P("solid", tone="dark"), marks(*rects)]


# ---------------------------------------------------------------- paint
def paints():
    """Role -> paint spec, the owner's patterns read off their render: slate and blue strips; plates edged light along
    two edges with a pinkish grey line inside; blue on the tops of the plates; the V's blue."""
    edge = P("solid", tone="mid_light")
    vee = [rows("gold_light", "dark", "gold_light"), P("hammer", keep_border=False, every=5)]
    cap = [P("solid", tone="gold_light"), marks((0, -1, 99, 1, "mid_light"))]
    lower = [P("plate", tone="dark", strips="v", bevel=False), P("hammer", keep_border=False, every=4)]
    # the under-layer as the owner's: blue blacks, its lighter two in strips, the darkest only in the breaks
    dusk = rows("under_mid", "under_light", "under_mid", "under_dark")
    return {
        # helmet: the crown's front shows only above the beak; its sides in slate strips, the brow's nested V on its
        # back; the beak dark slate, its top dark with blue edges; the cheeks and chin with pinkish grey feet
        "helm": {"front": rows("void", "seam", "void"),
                 "top": [rows("void", "void", "seam", "void"), P("hammer", keep_border=False, every=4)],
                 "bottom": P("solid", tone="under_dark"),
                 "back": [P("chevron", corner="v", bands=("seam", "gold_light", "dark", "seam", "void"), border=None,
                            core=("void", "seam"), outside="seam"), P("hammer", keep_border=False, every=4)],
                 "*": rows(*SLATE, breaks=True)},
        "beak": {"front": beak_face(ridge="right"), "left": beak_face(ridge="left"),
                 "top": [rows("void", "seam"), marks((0, -1, 99, 1, "dark"), (-1, 0, 1, 99, "dark"))],
                 "bottom": P("solid", tone="void"), "*": P("solid", tone="seam")},
        "brow": {"top": P("solid", tone="gold_light"), "bottom": P("solid", tone="dark"),
                 "*": P("solid", tone="gold_light")},
        "cheek": {"front": [rows("dark", "seam", "void", "seam", breaks=True), marks((0, -1, 99, 1, "mid"),
                                                                                   (0, 0, 1, 99, "gold_light"))],
                  "bottom": P("solid", tone="mid"), "top": P("solid", tone="gold_light"), "*": P("solid", tone="seam")},
        "chin": {"bottom": P("solid", tone="mid"), "*": [rows("dark", "seam"), marks((0, -1, 99, 1, "mid"))]},
        # the collar under the helm: the dark of the neck, a shade darker at its foot
        "collar": {"bottom": P("solid", tone="under_dark"), "*": [dusk, marks((0, -1, 99, 1, "under_dark"))]},
        "nape": {"back": [rows("mid_light", "seam", "dark", "gold_light", "seam", breaks=True),
                          marks((0, -1, 99, 1, "mid_light"))],
                 "top": P("solid", tone="seam"), "bottom": edge, "*": P("solid", tone="seam")},
        # horns: a blue stripe down each face between slate, a light band round the foot of the upper block and round
        # the lower block where it meets the helm
        "horn_low": {"top": P("solid", tone="dark"), "bottom": P("solid", tone="void"),
                     "*": [P("solid", tone="seam"), marks((1, 0, 1, 99, "gold_light"), (2, 0, 1, 99, "dark"),
                                                          (0, -1, 99, 1, "mid_light"))]},
        "horn_up": {"top": P("solid", tone="dark"), "bottom": edge,
                    "*": [P("solid", tone="seam"), marks((1, 0, 1, 99, "gold_light"), (2, 0, 1, 99, "dark"),
                                                         (0, -2, 99, 1, "light"), (0, -1, 99, 1, "mid_light"),
                                                         (0, 0, 99, 1, "dark"))]},
        # chest: the breastplate in slate strips inside a pinkish grey frame, dark under the frame's inner edges, its
        # hem lit, the waist dark between pinkish grey corners; the V blue round a slate line, lit along its outer
        # edge, its cap blue; the back plain slate in nested V's
        "breast": {"front": [rows("seam", "dark", "gold_dark", "seam", "gold_light", "dark", "seam", "gold_dark",
                                  breaks=True),
                             marks((0, 3, 1, 99, "mid"), (-1, 3, 1, 99, "dark"), (0, -2, 99, 1, "mid_light"),
                                   (1, 4, 1, 5, "under_mid"), (-2, 4, 1, 5, "under_mid"),
                                   (3, -2, 4, 1, "under_mid"), (3, -1, 4, 1, "under_dark"), (0, -1, 3, 1, "mid"),
                                   (-3, -1, 3, 1, "mid"))],
                   "top": rows("seam", "dark"), "bottom": P("solid", tone="seam"),
                   "back": [P("chevron", corner="v", bands=("seam", "gold_light", "dark", "seam"), border=None,
                              core=("void", "seam"), outside="seam"), P("hammer", keep_border=False, every=4)],
                   "*": rows(*SLATE, breaks=True)},
        "chev": {"front": vee, "bottom": P("solid", tone="light"), "top": P("solid", tone="seam"),
                 "*": P("solid", tone="dark")},
        "chev_cap": {"front": cap, "*": edge},
        "back_chev": {"back": vee, "bottom": P("solid", tone="light"), "top": P("solid", tone="seam"),
                      "*": P("solid", tone="dark")},
        "back_chev_cap": {"back": cap, "*": edge},
        # the blood-red cloth: dark red with bright patches, as drawn, each column a little different, its ragged foot
        # a darker red with a lit texel
        "cloth_a": {"front": [P("solid", tone="leather_mid"),
                              marks((0, 1, 1, 2, "leather_light"), (0, 5, 1, 1, "leather_mid_light"),
                                    (0, -1, 1, 1, "leather_dark"))],
                    "*": P("solid", tone="leather_dark")},
        "cloth_b": {"front": [P("solid", tone="leather_light"),
                              marks((0, 3, 1, 2, "leather_mid"), (0, 7, 1, 1, "leather_mid_light"),
                                    (0, -2, 1, 1, "leather_mid"), (0, -1, 1, 1, "leather_mid_light"))],
                    "*": P("solid", tone="leather_dark")},
        "cloth_c": {"front": [P("solid", tone="leather_mid"),
                              marks((0, 0, 1, 1, "leather_mid_light"), (0, 4, 1, 2, "leather_light"),
                                    (0, -1, 1, 1, "leather_dark"))],
                    "*": P("solid", tone="leather_dark")},
        # arms: the mantle edged pinkish grey along its outer end and lower edge, its top blue; the fin's lower edge
        # pinkish grey and its outer end lit, its top blue; the framed vambrace and its guard
        "pauldron": {"front": edged("bl", light="mid", inner="dark"), "back": edged("br", light="mid", inner="dark"),
                     "top": rows("gold_light", "dark", "gold_light", "seam"),
                     "bottom": P("solid", tone="void"), "*": rows("mid_light", "seam")},
        "fin": {"front": [rows("gold_light", "dark", "seam"),
                          marks((0, -1, 99, 1, "mid"), (0, 0, 1, 99, "mid_light"))],
                "back": [rows("gold_light", "dark", "seam"),
                         marks((0, -1, 99, 1, "mid"), (-1, 0, 1, 99, "mid_light"))],
                "top": rows("gold_light", "dark"), "bottom": edge, "*": P("solid", tone="mid")},
        "sleeve": dusk,
        "vambrace": {"front": edged("bl"), "back": edged("br"), "right": edged("bl"),
                     "left": rows("seam", "dark", "void", "seam"), "top": P("solid", tone="void"), "bottom": edge},
        "guard": {"right": edged("bl"), "left": rows("seam", "dark"), "top": edge, "bottom": edge,
                  "*": rows("mid_light", "seam")},
        # waist and legs
        "belt": dusk,
        "crest": {"front": [P("solid", tone="seam"), marks((1, 1, 1, 1, "gold_light"), (0, 0, 3, 1, "dark"))],
                  "*": P("solid", tone="seam")},
        "skirt": dusk,
        # the tasset: lit along its top edge; its inner edge one arm of the skirt's light V, set off by a dark line
        "tasset": {"front": [*edged("tr", inner="dark"), marks((-1, 0, 1, 99, "light"), (-2, 1, 1, 99, "void"))],
                   "back": edged("tl", inner="dark"), "top": P("solid", tone="mid_light"), "*": edge},
        "tasset_back": {"back": [*edged("tl", inner="dark"), marks((0, 0, 1, 99, "mid_light"), (1, 1, 1, 99, "void"))],
                        "front": edged("tr", inner="dark"), "top": P("solid", tone="mid_light"), "*": edge},
        "apron_back": {"back": rows("mid_light", "seam", "dark", "void", "gold_light", "seam", "dark", "gold_light",
                                    "seam", breaks=True),
                       "top": P("solid", tone="light"), "bottom": P("solid", tone="void"),
                       "*": rows("mid_light", "seam")},
        # the lower plates: vertical slate strips, framed pale grey along the outer edge, pinkish grey at the hem
        "flare": {"front": [*lower, marks((0, 0, 1, 99, "mid_light"), (0, -1, 99, 1, "mid"),
                                          (-1, 0, 1, 99, "gold_light"))],
                  "back": [*lower, marks((-1, 0, 1, 99, "mid_light"), (0, -1, 99, 1, "mid"),
                                         (0, 0, 1, 99, "gold_light"))],
                  "right": rows(*SLATE, breaks=True), "left": P("solid", tone="seam"), "top": P("solid", tone="void"),
                  "bottom": edge},
        # boots: light and slate strips under a lit rim; the toe cap's V; the heel plate
        "boot": {"front": rows("seam", "gold_light", "dark", "void", "mid", "seam", "gold_light", breaks=True),
                 "top": P("solid", tone="void"), "bottom": P("solid", tone="void"), "*": rows(*SLATE, breaks=True)},
        "cuff": {"top": P("solid", tone="seam"), "*": rows("light", "mid_light")},
        "toe": {"front": P("chevron", corner="v", bands=("mid_light", "seam", "gold_light"), border=None,
                           core=("seam",), outside="seam"), "*": P("solid", tone="seam")},
        "heel": {"back": [rows("seam", "dark", "gold_light", "seam"), marks((0, -1, 99, 1, "mid_light"))],
                 "*": P("solid", tone="seam")},
    }


# ---------------------------------------------------------------- measures, fitted to the owner's front view
G = dict(
    # helm: the crown's top, half width, front and back (it reaches a little further back than forward) and bottom,
    # the collar's foot below the head; the beak's half width at the crown's front corners (it reaches as far in front
    # of the crown), its top and foot, and how far the V brow drops from the beak's outer corners to the ridge
    top=-10.0, hw=4.85, hd=4.8, hb=5.8, cbot=-1.75, foot=0.75, bw=4.5, btop=-9.25, bbot=-1.25, vdrop=2.75,
    # the cheek plates: width along the beak face from its outer end, top and foot
    cw=2.0, ctop=-3.0, cbot2=0.25,
    # horns: the lower block's centre (lx, lz) on the helm's top, its size, its lean out and forward; the upper
    # block from its top (shifted ux inward), its width, length, lean in and forward
    lx=5.3, lz=-5.0, lw=3.7, lh=3.0, ld=4.75, lo=11.0, lf=12.0, ux=0.15, uw=3.0, ul=5.45, ui=31.5, uf=9.0,
    # pauldron (arm space): each plate by its inner top corner (x, y), length, thickness, depth and roll (positive
    # lifts the outer end), standing: mantle, fin; the vambrace guard's flare
    mx=2.75, my=-4.75, ml=8.5, mt=2.25, md=4.75, mroll=14.0,
    fx=-0.75, fy=-1.5, fl=6.75, ft=2.0, fd=4.5, froll=10.0,
    gflare=25.0,
    # chest V: the point's height, the bars' angle above horizontal, their width, their tops' height (just over the
    # breastplate's top, between the mantles), the cap's side; the back V's point, angle and tops
    cvy=6.0, cva=58.0, cvw=2.5, cvtop=-0.5, cvcap=2.2, bvy=6.3, bva=60.0, bvtop=0.25,
    # the tabard: where each column of its strips ends (body space for the chest's three; leg space, outer column
    # first, for each leg's two)
    tabard=(11.5, 12.25, 11.0), tabard_right=(10.25, 11.5), tabard_left=(11.25, 10.0),
    # skirt (leg space): the tasset's length, height, slope and the V's point; the back apron's top; the lower
    # plate's inner top corner (ox, oy0), width, height and flare
    tl=8.9, ts=5.9, trot=31.0, tyv=1.65, ay0=4.5, ox=-1.0, oy0=3.0, ow=5.3, oh=9.25, oroll=12.0,
)


# ---------------------------------------------------------------- helmet
def horns(r, g):
    """The right horn: a block on the helm's front top corner leaning out and forward, and from its top a second one
    leaning in and forward, so the two horns' tips curve toward each other."""
    lx, lz = -g["lx"], g["lz"]
    low = am.box("horn_low_right", (lx - g["lw"] / 2, g["top"] - g["lh"], lz - g["ld"] / 2),
                 (g["lw"], g["lh"] + 0.5, g["ld"]), pivot=(lx, g["top"], lz), rotation=(-g["lf"], 0, -g["lo"]),
                 paint=r["horn_low"])
    top = am.place(low, (lx + g["ux"], g["top"] - g["lh"], lz))
    up = am.box("horn_up_right", (top[0] - g["uw"] / 2, top[1] - g["ul"], top[2] - g["uw"] / 2),
                (g["uw"], g["ul"] + 0.75, g["uw"]), pivot=(top[0], top[1] + 0.75, top[2]),
                rotation=(g["uf"], 0, g["ui"]), paint=r["horn_up"])
    return [low, up]


def helmet(r, g=G):
    hw, hd = g["hw"], g["hd"]
    # the crown, narrower than the knight's helm, its top over the hat layer; under it the dark collar, set in from the
    # crown's sides, closes the neck 0.75 below the head (its sides, front and back 0.65 off the head, clear of the hat
    # layer); the collar's top is inside the crown
    parts = [am.span("helm", (-hw, g["top"], -hd), (hw, g["cbot"], g["hb"]), paint=r["helm"]),
             am.span("collar", (-4.65, g["cbot"] - 0.5, -4.65), (4.65, g["foot"], 4.65), skip="top",
                     paint=r["collar"])]
    # the beak: one square block turned 45 degrees, so two of its faces meet in the ridge on the centre line in front
    # of the crown and run back to the crown's front corners, as tall as the crown's front; its back half lies inside
    # the crown
    bw, zr = g["bw"], -hd - g["bw"]
    a = bw * math.sqrt(2)
    parts.append(am.box("beak", (-a / 2, g["btop"], -hd - a / 2), (a, g["bbot"] - g["btop"], a),
                        pivot=(0.0, 0.0, -hd), rotation=(0, 45, 0), paint=r["beak"]))
    # the V brow: a bar along each beak face from its outer top corner down to the ridge, standing proud of it
    drop = g["vdrop"]
    brow = am.box("brow_right", (-math.hypot(a, drop), g["btop"] + drop, zr - 0.5), (math.hypot(a, drop), 1.0, 0.5),
                  pivot=(0.0, g["btop"] + drop, zr), rotation=(0, 0, math.degrees(math.atan2(drop, a))),
                  paint=r["brow"])
    parts += am.pair(am.turned(brow, "y", 45.0, (0.0, 0.0, zr)))
    # the cheek plates: one on the outer foot of each beak face, standing proud of it and hanging below it to the jaw
    t = 0.75
    cheek = am.box("cheek_right", (-bw, g["ctop"], -hd - 0.1 - t), (g["cw"], g["cbot2"] - g["ctop"], t),
                   pivot=(-bw, 0.0, -hd), rotation=(0, 45, 0), paint=r["cheek"])
    parts += am.pair(cheek)
    # the chin: a square bar on its edge under the ridge, down to the jaw, so the beak's foot is cut into the chin and
    # the two cheeks with the dark collar showing between
    c = 1.5
    zc = zr + c / math.sqrt(2) + 0.25   # its faces 0.18 behind the beak's
    parts.append(am.box("chin", (-c / 2, g["bbot"] - 0.5, zc - c / 2), (c, g["cbot2"] - g["bbot"] + 0.5, c),
                        pivot=(0.0, 0.0, zc), rotation=(0, 45, 0), paint=r["chin"]))
    low, up = horns(r, g)
    parts += am.pair(low) + am.pair(up)
    # the nape guard: a plate across the back of the neck under the crown, flared back a little at its foot
    parts.append(am.hinge(am.span("nape", (-hw + 0.5, g["cbot"] - 1.0, g["hb"]), (hw - 0.5, g["foot"] + 0.75,
                                                                                    g["hb"] + 0.75),
                                  paint=r["nape"]), "top", 12.0, toward="back"))
    return parts


# ---------------------------------------------------------------- chestplate
def vee(r, g, name, z, back=False):
    """A V of two bars rising steeply from its point (on the centre line at g["cvy"]) to the shoulders, standing proud
    of the breastplate's face at `z` (its front, or with `back` its back). A V this steep cannot be mitred with boxes,
    so it is lapped: the left bar runs to the point, 0.15 px further out, and the right one starts far enough up its
    length that its end lies inside the left; a small diamond cap covers the joint. The bars are 1.1 thick, so their
    outer faces keep 0.15 px off the helmet's collar (4.65 in front and behind) where they rise past it."""
    if back:   # the back V keeps to the breastplate (no mantles to stand between)
        g = dict(g, cvy=g["bvy"], cva=g["bva"], cvtop=g["bvtop"], cvw=2.5)
    a, w, t = g["cva"], g["cvw"], 1.1
    length = (g["cvy"] - g["cvtop"]) / math.sin(math.radians(a))
    start = -w / math.tan(math.radians(2 * a))
    zb = (z, z + t) if back else (z - t, z)
    zl = (zb[0] + 0.15, zb[1] + 0.15) if back else (zb[0] - 0.15, zb[1] - 0.15)
    left = am.Part(f"{name}_left", (0.0, g["cvy"] - w, zl[0]), (length, w, t), paint=r[name], mirror=True)
    right = am.Part(f"{name}_right", (-length, g["cvy"] - w, zb[0]), (length - start, w, t), paint=r[name])
    side, centre = g["cvcap"], g["cvy"] - g["cvcap"] / math.sqrt(2)
    cap = am.Part(f"{name}_cap", (-side / 2, centre - side / 2, zl[1] if back else zl[0] - 0.4), (side, side, 0.5),
                  paint=r[f"{name}_cap"])
    return [am.turned(right, "z", a, (0.0, g["cvy"], 0.0)), am.turned(left, "z", -a, (0.0, g["cvy"], 0.0)),
            am.turned(cap, "z", 45.0, (0.0, centre, 0.0))]


def strips(r, name, edges, top, ends, z):
    """A hanging strip of cloth in columns (x from edges[i] to edges[i + 1], from `top` down to ends[i]), so its foot
    is ragged; z = (front, back)."""
    return [am.span(f"{name}_{i}", (edges[i], top, z[0]), (edges[i + 1], end, z[1]), paint=r[f"cloth_{'abc'[i % 3]}"])
            for i, end in enumerate(ends)]


def body(r, g=G):
    """The breastplate; the V over it, and its twin on the back; the tabard's upper strip, from under the V's point
    down to the waist, hanging below the breastplate (worn with the leggings, it hangs behind their diamond, and their
    strips carry it on to the hem)."""
    return [am.span("breast", (-5.0, -0.75, -3.25), (5.0, 9.75, 3.25), paint=r["breast"]),
            *vee(r, g, "chev", -3.25), *vee(r, g, "back_chev", 3.25, back=True),
            *strips(r, "tabard", (-1.4, -0.4, 0.4, 1.4), g["cvy"] + 0.25, g["tabard"], (-3.6, -3.35))]


def slab(name, inner, length, thick, depth, roll, paint):
    """A plate on the right arm reaching out toward -x from its inner top corner `inner` (x, y), `length` long,
    `thick` tall and `depth` deep, rolled `roll` degrees about that corner: positive lifts the outer end."""
    x, y = inner
    return am.box(name, (x - length, y, -depth / 2), (length, thick, depth), pivot=(x, y, 0.0), rotation=(0, 0, roll),
                  paint=paint)


def arm(r, g=G):
    """The right arm: the mantle high over the shoulder from beside the neck, and the fin under it reaching further
    out, both rising toward the outside standing, as the owner's chestplate on its own (raised, they rise the more);
    the dark sleeve, closed all round the arm, showing between the fin and the vambrace; the vambrace with its guard
    flaring out toward the elbow."""
    return [
        slab("pauldron_right", (g["mx"], g["my"]), g["ml"], g["mt"], g["md"], g["mroll"], r["pauldron"]),
        slab("fin_right", (g["fx"], g["fy"]), g["fl"], g["ft"], g["fd"], g["froll"], r["fin"]),
        am.span("sleeve_right", (-3.45, -2.45, -2.45), (1.45, 10.45, 2.45), paint=r["sleeve"]),
        am.span("vambrace_right", (-3.75, 4.0, -3.0), (1.75, 9.75, 3.0), paint=r["vambrace"]),
        am.hinge(am.span("guard_right", (-4.75, 3.5, -3.25), (-3.75, 9.75, 3.25), paint=r["guard"]), "bottom",
                 g["gflare"], toward="right"),
    ]


# ---------------------------------------------------------------- leggings
def waist(r, g=G):
    """The dark belt (its foot 0.15 below the sleeves' feet standing), and the small diamond in the skirt's V, in front
    of the tabard where its two strips meet."""
    return [am.span("belt", (-4.85, 9.5, -2.85), (4.85, 12.6, 2.85), paint=r["belt"]),
            am.turned(am.Part("crest", (-1.6, 10.0, -4.6), (3.2, 3.2, 0.45), paint=r["crest"]), "z", 45.0,
                      (0.0, 11.6, -4.375))]


def tasset(r, g, back=False):
    """The tasset: a plate `tl` long and `ts` tall over the thigh, its long edges sloping down toward the outside at
    `trot` degrees; its inner edge, one arm of the skirt's V, comes down to the centre line (leg x 1.9) at the V's
    point. `back`: its twin behind the thigh, so the V repeats on the back."""
    th = math.radians(g["trot"])
    tx, ty = 1.9 - g["ts"] * math.sin(th), g["tyv"] - g["ts"] * math.cos(th)
    part = am.Part("tasset_back_right" if back else "tasset_right", (tx - g["tl"], ty, 2.85 if back else -4.1),
                   (g["tl"], g["ts"], 1.25), paint=r["tasset_back" if back else "tasset"])
    return am.turned(part, "z", -g["trot"], (tx, ty, 0.0))


LEFT_OUT = 0.12   # the left leg's closed boxes (under-skirt, boot) are inflated this much, so the two legs' faces
                  # never share a plane where they overlap at the centre line; its other parts stay clear of the right's


def leg(r, g=G, side="right"):
    """One leg's leggings parts (the leg box is x -2..2, y 0..12, z -2..2; x 1.9 is the centre line), built for the
    right leg and mirrored: the dark under-skirt, closed all round, its inner side 0.45 off the leg (inside the other
    leg's standing, closing the gap between them walking); the tasset in front and its twin behind; the lower plate
    flaring out to the hem round the front, outer side and back; the striped apron behind; the red cloth, from under the
    belt behind the tassets down almost to the hem, ragged."""
    out = [
        am.span("skirt_right", (-2.6, -0.5, -2.6), (2.45, 12.5, 2.6), paint=r["skirt"]),
        tasset(r, g),
        tasset(r, g, back=True),
        am.span("apron_back_right", (-1.25, g["ay0"], 3.6), (1.7, 11.6, 4.6), paint=r["apron_back"]),
        am.box("flare_right", (g["ox"] - g["ow"], g["oy0"], -4.4), (g["ow"], g["oh"], 8.8),
               pivot=(g["ox"], g["oy0"], 0.0), rotation=(0, 0, g["oroll"]), paint=r["flare"]),
    ]
    # the tabard's lower strip, in two columns whose lengths differ on each leg, so the hem is ragged
    cloth = strips(r, f"tabard_{side}", (0.35, 1.1, 1.85), 0.5, g[f"tabard_{side}"], (-3.985, -3.735))
    if side == "right":
        return out + cloth
    return [replace(p, inflate=LEFT_OUT) if p.name.startswith("skirt") else p for p in am.mirror_all(out)] + [
        replace(am.mirror(p, p.name), net="") for p in cloth]


# ---------------------------------------------------------------- boots
def boot(r, g=G):
    """The boot, tall enough to show under the skirt as the owner's do: closed all round, its inner side 0.65 off the
    leg (clear of vanilla leggings' 0.5 shell), inside the other boot standing; a lit rim; a toe cap with the set's V;
    a heel plate behind. The left one is inflated LEFT_OUT (model())."""
    return [am.span("boot_right", (-2.9, 6.0, -3.25), (2.65, 12.75, 2.9), paint=r["boot"]),
            am.span("cuff_right", (-3.15, 5.5, -3.5), (2.9, 6.75, 3.15), paint=r["cuff"]),
            am.span("toe_right", (-2.25, 9.75, -3.75), (1.4, 12.5, -3.25), paint=r["toe"]),
            am.hinge(am.span("heel_right", (-2.4, 8.0, 2.9), (1.9, 12.25, 3.4), paint=r["heel"]), "top", 10.0,
                     toward="back")]


# ---------------------------------------------------------------- the set
def model(g=G):
    """{item: {bone: [parts]}} for the four pieces."""
    r = paints()
    a, b = arm(r, g), boot(r, g)
    return {
        "hades_helmet": {"head": helmet(r, g)},
        "hades_chestplate": {"body": body(r, g), "right_arm": a, "left_arm": am.mirror_all(a)},
        "hades_leggings": {"body": waist(r, g), "right_leg": leg(r, g, "right"), "left_leg": leg(r, g, "left")},
        "hades_boots": {"right_leg": b, "left_leg": [replace(p, inflate=LEFT_OUT) for p in am.mirror_all(b)]},
    }


SETS = [am.ArmorSet("hades", HADES, model())]
