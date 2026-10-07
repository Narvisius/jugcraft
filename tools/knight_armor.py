"""The knight armor: the owner's steel design (a front render of a player in plate armor) as 3D worn models for
jugcraft:steel_helmet, steel_chestplate, steel_leggings and steel_boots, and a bronze variant of it for the bronze_
pieces, on the toolkit in tools/armor_models.py (shapes) and tools/armor_paint.py (the atlas). Same items, ids and
stats as before; only how they look when worn.

What the owner drew, and where it is here:
    helmet      a helm wider than the head; a big diamond crest pitched back with nested chevrons, a finial and two
                horn fins; splayed cheek fins; eye slits stepping down in a V, grille ribs and a nasal bar; a gold
                collar showing at the sides of a bevor, with a clasp under the nasal bar
    chestplate  a forward-angled diamond plate over the chest, its point jutting out over the waist with a keel tab;
                large layered pauldrons (cap, main plate, two lames) with nested L's toward the armpit; vambraces open
                on the inner side over a strapped sleeve, with cuffs flaring toward the elbow
    leggings    a brown leather belt of plates and a strap over a dark under-layer; a skirt of four plate lames a leg
                to the ground, each wider and further out, rolled so their tops dip to a V at the centre line, with a
                mail lining closing each leg's inner side
    boots       hidden under the skirt in the drawing (its hem is dark to the ground): a greave with a knee cop, a
                sabaton with overlapping instep lames and a diamond toe cap, seen as the legs swing
Added for the "more intricate" request, in the drawing's own vocabulary (plates, lames, nested chevrons, leather): a
crown comb and a three-lame neck guard on the helm, steps on the cheek fins; an upright flange (haute-piece) on each
pauldron; a lame round the breastplate's hem, a spine ridge and a V on the back; a glove with a knuckle plate and a
steel fist; mail under the skirt and inside the open vambrace, so a swinging limb shows dark steel, not a black hole.
Gold is only at the collar, as drawn.

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left.

The bronze variant (bronze_knight) is the same armor made earlier, in the steam age (docs/ART_DIRECTION.md: bronze is
"brass, copper and riveted iron", steel heavy bolts): the same build in armor_paint.BRONZE, a warm copper-bronze, with
    trim        brass where the steel has gold (the collar, now riveted, and the clasp), and where the steel's small
                fittings are steel: the breastplate's side buckles, the elbow-strap studs, horn tips, knuckle plates
    fittings    rows of small brass rivets where the steel has a few heavy bolts (cheek fins, cuffs) and along the
                breastplate's hem lame and the pauldron lames; a rivet at each skirt lame's outer end, so a column runs
                down each side of the skirt; the belt plates riveted at their outer top corners
    shapes      a round brass knob on the finial pin for the steel's diamond, and a brass buckle on the belt strap
                (the steel's belt has none); everything else is the steel's geometry, part for part
    leather     darker and redder; the under-layer stays the steel's slate
model(metal) builds the pieces for "steel" or "bronze". Preview: python3 tools/armor_preview.py --set steel_knight
(or bronze_knight).
"""
import math
from dataclasses import replace

import armor_models as am
from armor_paint import BRONZE, P, STEEL, shade

H = P("hammer")   # the owner's broken, hammered strips over a nested-L plate


def rows(*tones):
    """The lames painter with one tone per texel row, cycling (broken strips along each row)."""
    return P("lames", rows=1, tones=tuple((t, t) for t in tones), centre=None)


# ---------------------------------------------------------------- paint
def paints(fit="bolt"):
    """Role -> paint spec. `fit`: "bolt" (2x2 bolts lit at the top left) or "rivet" (1-texel rivets)."""
    bolts = dict(style=fit, tone="light")
    # the owner's helm sides: mid-light and mid strips, light only as the outer ring and one band
    shell_l = dict(bands=("mid_light", "mid", "light", "dark"), border="light", core=("mid_light", "mid"))
    # the far side of an open plate (inside the vambrace, under the skirt): dark mail rows, which light to grey where
    # the under-layer would light to black
    mail = P("lames", rows=1, tones=(("dark", "dark"), ("seam", "seam")), centre=None)
    # the belt plate: a light top, the strap across, mid leather below, darkest at the inner lower corner
    belt_plate = [P("leather", tone="leather_mid", rim="leather_light"),
                  P("marks", rects=[(0, 1, 4, 1, "leather_mid_light"), (1, 1, 2, 1, "leather_light")])]
    return {
        # helm
        "shell": {"right": [P("chevron", corner="bl", **shell_l), H], "left": [P("chevron", corner="br", **shell_l), H],
                  "back": [P("chevron", corner="v", bands=("mid_light", "light", "mid", "dark"), border="light",
                             core=("mid_light", "light"), thick=2), H],
                  "top": P("plate", tone="light"), "*": P("plate")},
        "brow": {"top": P("plate", tone="light"), "*": P("plate")},
        "visor": {"front": [P("plate", tone="mid", bevel=False),
                            P("marks", rects=[(0, 0, 1, 1, "void"), (2, 1, 1, 2, "void"), (4, 2, 1, 2, "seam")],
                              symmetric=True)],
                  "*": P("plate")},
        # the crest's light rim is its thickness (the bottom and right faces below); the face itself is a mid-light
        # ring, then a mid V at the point, a light V and a dark V round a mottled core, as drawn
        "crest": {"front": [P("chevron", corner="bl", bands=("mid", "light", "dark"), border="mid_light",
                              core=("light", "mid_light")), H],
                  "bottom": P("solid", tone="light"), "right": P("solid", tone="light"),
                  "top": P("edge", tone="mid_light"), "left": P("edge", tone="mid_light"),
                  "back": P("plate", tone="mid", border="seam")},
        "finial": {"top": P("solid", tone="light"), "*": [P("solid", tone="mid_light"),
                                                           P("marks", rects=[(0, 0, 9, 1, "light")])]},
        "pin": P("solid", tone="dark"),
        "comb": {"top": P("solid", tone="light"),
                 "*": [rows("light", "mid_light"), P("marks", rects=[(0, -1, 9, 1, "mid")])]},
        "horn": {"top": P("solid", tone="light"),
                 "*": [P("solid", tone="mid_light"), P("marks", rects=[(0, 0, 2, 1, "light")])]},
        "cheek": {"right": [P("chevron", corner="br", bands=("light", "mid", "dark"), border="mid_light"), H,
                            P("rivets", row="top", every=9, offset=0, **bolts)],
                  "left": [P("chevron", corner="bl", bands=("light", "mid", "dark"), border="mid_light"), H],
                  "*": P("edge", tone="mid_light")},
        "cheek_top": {"top": P("solid", tone="light"), "*": P("solid", tone="mid_light")},
        "nasal": {"front": [P("solid", tone="mid_light"), P("marks", rects=[(0, 0, 1, 9, "light")])],
                  "*": P("solid", tone="mid")},
        "rib": {"front": P("solid", tone="mid_light"), "right": P("solid", tone="mid_light"),
                "*": P("solid", tone="mid")},
        "bevor": {"front": P("plate", tone="mid"), "*": P("solid", tone="mid")},
        "collar": P("gold", rim=None),
        "clasp": {"front": [P("gold", tone="gold_light", rim=None), P("marks", rects=[(1, 0, 1, 1, "gold_dark")])],
                  "*": P("gold", rim=None)},
        "nape": {"*": rows("light", "mid"), "top": P("solid", tone="light")},
        # chest and back
        # a mid field, so the light chevron plate stands out against it as in the owner's; steel-buckled side straps
        "breast": {"front": P("plate", tone="mid"),
                   "back": P("plate", border="light"), "top": P("plate", tone="light"),
                   "*": [P("plate"), P("marks", rects=[(0, 2, 9, 1, "leather_dark"), (3, 2, 2, 1, "mid_light")])]},
        "chev": {"front": [P("chevron", corner="bl", bands=("mid", "light", "mid"), border="light",
                             core=("light", "mid_light")), H],
                 "bottom": P("solid", tone="light"), "right": P("solid", tone="light"), "*": P("edge", tone="mid")},
        "keel_tab": {"front": [P("solid", tone="mid_light"), P("marks", rects=[(0, 0, 1, 9, "light")])],
                     "*": P("solid", tone="mid")},
        "waist": P("under"),
        "rim": {"front": P("lames", rows=2, tones=(("light", "mid"),), centre="middle"),
                "back": P("lames", rows=2, tones=(("light", "mid"),), centre="middle"),
                "*": P("lames", rows=2, tones=(("light", "mid"),), centre=None)},
        "spine": {"back": [P("solid", tone="mid_light"), P("marks", rects=[(0, 0, 1, 9, "light")])],
                  "*": P("solid", tone="mid")},
        "back_chev": {"back": rows("light", "mid"), "*": P("edge", tone="mid_light")},
        # arms
        # the owner's pauldron: nested L's about the lower-inner corner (toward the armpit), with no ring: a mid
        # bottom row, a light strip, the dark L, then light and mid-light strips up to the light top row; no bolts
        # (the owner drew none there)
        "pauldron": {**{face: [P("chevron", corner=corner, border=None,
                                 bands=("mid", "light", "dark", "light", "mid_light", "light")), H]
                        for face, corner in (("front", "in"), ("back", "in"), ("right", "br"))},
                     "top": P("plate", tone="light", strips="v"), "*": P("plate")},
        "cap": {"top": P("plate", tone="light", strips="v"),
                "*": [P("solid", tone="light"), P("marks", rects=[(0, -1, 20, 1, "mid_light")])]},
        "haute": {"*": rows("light", "mid_light", "mid_light"), "top": P("solid", tone="light")},
        "paul_lame": {"top": P("solid", tone="light"), "*": rows("mid_light", "dark")},
        "sleeve": {"left": mail, "*": P("under")},   # its inner side shows through the open vambrace
        "strap": {"left": P("strap", holes=3, rim="leather_light"),
                  "front": [P("strap", holes=0, rim="leather_light"), P("marks", rects=[(-3, 0, 1, 1, "mid_light")])],
                  "back": P("strap", holes=0, rim="leather_light"), "*": P("solid", tone="leather_dark")},
        # the owner's forearms are mostly mid with dark L's, no light
        "vambrace": {"front": [P("chevron", corner="in", bands=("dark", "seam"), border="mid", outside="mid",
                                 core=("mid_light", "mid")), H],
                     "back": [P("chevron", corner="in", bands=("dark", "seam"), border="mid", outside="mid",
                                core=("mid_light", "mid")), H],
                     "*": P("plate", tone="mid")},
        "cuff": {"right": [P("chevron", corner="bl", bands=("mid", "dark"), border="mid_light",
                             core=("mid", "mid_light")), H,
                           P("rivets", row="top", every=9, offset=1, **bolts)],
                 "left": P("plate", tone="mid", border="dark"), "*": P("edge", tone="mid")},
        # the fist's underside (seen whenever an arm swings forward): steel with finger lines, not lit-black cloth
        "glove": {"bottom": [P("plate", tone="mid_light", bevel=False),
                             P("marks", rects=[(0, 1, 9, 1, "dark"), (0, 3, 9, 1, "dark")])],
                  "*": P("under", light=None)},
        "knuckle": {"right": [P("solid", tone="mid_light"), P("marks", rects=[(0, 1, 9, 1, "dark")])],
                    "*": P("solid", tone="mid")},
        # waist: the owner's belt has no buckle; the strap is lighter at the centre instead
        "belt": {"front": [P("under"), P("marks", rects=[(4, 1, 3, 3, "under_light"), (6, 2, 1, 2, "under_dark")])],
                 "back": [P("under"), P("marks", rects=[(4, 1, 3, 3, "under_light")])],
                 # the sides show whenever an arm swings away: leather, not under-layer (which lights to black there)
                 "sides": belt_plate, "*": P("under")},
        "plate_l": {"front": belt_plate, "back": belt_plate, "*": P("solid", tone="leather_dark")},
        "belt_strap": {"front": [P("strap", tone="leather_dark", rim=None, holes=4),
                                 P("marks", rects=[(4, 0, 3, 1, "leather_mid_light")])],
                       "back": P("strap", tone="leather_dark", rim=None, holes=4),
                       "*": P("strap", tone="leather_dark", rim=None, holes=0)},
        "hip": P("under"),
        "breeches": mail,
        # boots
        "greave": {"front": P("chevron", corner="v", bands=("mid_light", "dark"), border="mid_light"),
                   "*": P("plate", tone="mid")},
        "poleyn": {"front": P("chevron", corner="bl", bands=("light", "mid"), border=None),
                   "*": P("solid", tone="mid_light")},
        "foot": {"bottom": P("plate", tone="dark", bevel=False, border="mid"), "*": P("plate", tone="mid")},
        "instep": {"top": P("solid", tone="light"), "*": rows("light", "mid")},
        "toe": {"front": P("chevron", corner="bl", bands=("light", "mid"), border=None),
                "*": P("solid", tone="mid_light")},
    }


# The owner's four skirt lames, three texel rows each (a lit top row, the plate, its shadowed lower edge), from the hip.
# As drawn (DESIGN_NOTES' skirt grid), no lame below the first has a light top, and the lower two end in void and seam.
SKIRT_ROWS = (("mid_light", "mid_light", "dark"), ("mid", "mid", "dark"), ("mid_light", "mid_light", "void"),
              ("mid", "mid", "seam"))


def skirt_paint(i):
    tones = tuple((t, t) for t in SKIRT_ROWS[i])
    inner = tuple((shade(t, 1), shade(t, 1)) for t in SKIRT_ROWS[i])
    return {"front": P("lames", rows=1, tones=tones, centre="in"), "back": P("lames", rows=1, tones=tones, centre="in"),
            "right": P("lames", rows=1, tones=tones, centre=None),
            # the inner side, seen in the slit at the centre line and on the far leg when walking: the same rows a
            # shade darker, in shadow, so the slit reads as a seam and the far leg still shows banded lames
            "left": P("lames", rows=1, tones=inner, centre=None),
            "top": P("solid", tone="seam"), "bottom": P("solid", tone="under_dark")}


# ---------------------------------------------------------------- the bronze variant's paint
# Bronze is the same armor made earlier, in the steam age (docs/ART_DIRECTION.md: "brass, copper and riveted iron"):
# riveted where the steel is bolted. In BRONZE "gold" is brass, so the steel's gold collar and clasp come out brass.
RIVET = dict(style="rivet", tone="gold_light")   # one-texel brass rivets


def brass_rivets(row, every=3, offset=1):
    return P("rivets", row=row, every=every, offset=offset, **RIVET)


def bronze_paints():
    """The steel's roles, re-accented for bronze: rows of small brass rivets where the steel has a few heavy bolts
    (cheek fins, cuffs) and along the breastplate's hem lame and the pauldron lames; brass side buckles, elbow-strap
    studs, horn tips and knuckle plates where the steel's are steel; a riveted brass collar; the belt plates riveted at
    their outer top corners; the brass knob and belt buckle (bronze-only parts)."""
    r = paints("rivet")
    knob = {"top": P("solid", tone="gold_light"), "*": [P("solid", tone="gold_dark"),
                                                         P("marks", rects=[(0, 0, 9, 1, "gold_light")])]}
    r.update({
        "horn": {"top": P("solid", tone="gold_light"),
                 "*": [P("solid", tone="mid_light"), P("marks", rects=[(0, 0, 2, 1, "gold_light")])]},
        "cheek": {**r["cheek"], "right": [P("chevron", corner="br", bands=("light", "mid", "dark"), border="mid_light"), H,
                                          brass_rivets("top", every=2, offset=0)]},
        "collar": [P("gold", rim=None), P("rivets", row=0, every=2, offset=0, style="rivet", tone="gold_light")],
        "knob": knob,
        "breast": {**r["breast"], "*": [P("plate"), P("marks", rects=[(0, 2, 9, 1, "leather_dark"),
                                                                      (3, 2, 2, 1, "gold_light")])]},
        "rim": {face: [spec, brass_rivets(1)] for face, spec in r["rim"].items()},
        "paul_lame": {"top": P("solid", tone="light"), "*": [rows("mid_light", "dark"), brass_rivets(1)]},
        "strap": {**r["strap"], "front": [P("strap", holes=0, rim="leather_light"),
                                          P("marks", rects=[(-3, 0, 1, 1, "gold_light")])]},
        "cuff": {**r["cuff"], "right": [P("chevron", corner="bl", bands=("mid", "dark"), border="mid_light",
                                          core=("mid", "mid_light")), H, brass_rivets("top")]},
        # the belt plates riveted at their outer top corners (at the inner ones the rivets ran into the buckle); the
        # front's texture-left and the back's texture-right are the outer side
        "plate_l": {**r["plate_l"], "front": r["plate_l"]["front"] + [P("marks", rects=[(0, 0, 1, 1, "gold_light")])],
                    "back": r["plate_l"]["back"] + [P("marks", rects=[(-1, 0, 1, 1, "gold_light")])]},
        "knuckle": {"right": [P("solid", tone="gold_light"), P("marks", rects=[(0, 1, 9, 1, "gold_dark")])],
                    "*": P("solid", tone="gold_dark")},
        "buckle": {"front": [P("solid", tone="gold_light"),
                             P("marks", rects=[(0, -1, 9, 1, "gold_dark"), (1, -1, 1, 1, "leather_darkest")])],
                   "*": P("solid", tone="gold_dark")},
    })
    return r


def bronze_skirt_paint(i):
    """The steel's lames, each riveted near its outer end front and back, as lames are riveted to the leathers inside
    them: a column of brass rivets down each side of the skirt. Not across the plates: a rivet every few texels turned
    the skirt into polka dots."""
    out = skirt_paint(i)
    for face in ("front", "back"):
        out[face] = [out[face], P("marks", rects=[(1, 1, 1, 1, "gold_light")])]
    out["right"] = [out["right"], P("marks", rects=[(1, 1, 1, 1, "gold_light"), (-2, 1, 1, 1, "gold_light")])]
    return out


# ---------------------------------------------------------------- shape helpers
def pitched(name, origin, size, centre, pitch, **options):
    """A box in a pitched plate's frame (as if the plate stood upright), pitched with it about `centre`."""
    return am.turned(am.box(name, origin, size, **options), "x", -pitch, centre)


def rod(name, base, length, section, out, back, **options):
    """A square bar standing on `base` (its bottom centre), tipped `out` degrees toward the model's right (-x) and
    leaning `back` degrees backward; build it on the model's right and mirror it for the left."""
    bx, by, bz = base
    s = section / 2
    return am.box(name, (bx - s, by - length, bz - s), (section, length, section), pivot=base,
                  rotation=(-back, 0, -out), **options)


def vee(name, apex, length, width, thickness, angle, pitch=0.0, stagger=0.15, paint="chevron", skip=()):
    """am.chevron, with the left bar `stagger` px nearer the viewer (negative: behind, for a vee on the back), so the
    two bars never share a plane where they cross above the apex."""
    right, left = am.chevron(name, apex, length, width, thickness, angle=angle, pitch=pitch, paint=paint, skip=skip)
    left = replace(left, origin=(left.origin[0], left.origin[1], left.origin[2] - stagger))
    return [right, left]


def tipped(part, pivot, degrees):
    """A plate hinged about the horizontal (x) line through `pivot`: positive degrees swing its lower part forward."""
    return am.turned(part, "x", -degrees, pivot)


# ---------------------------------------------------------------- helmet
# The crest: an 8 px square on its corner, pitched back 50 degrees so it reads as wide and squat from the front. The
# owner's render, from a low camera, shows it about 0.65 as tall as wide. At 42 degrees ours matched that under the
# render's camera (0.69) but stood taller from the game's own cameras (0.80 at eye level, 0.94 from 15 degrees above);
# at 50 they read 0.57, 0.70 and 0.88, nearest the drawing where the owner will look at it. Under 35 the point hangs
# over the eye slits.
CREST = dict(centre=(0.0, -7.5, -4.9), side=8.0, thickness=1.5, pitch=50.0)


def helmet(r, knob=False):
    """The helm; `knob` (bronze) tops the finial pin with a round brass knob instead of the steel's diamond."""
    c, s, t, pitch = CREST["centre"], CREST["side"], CREST["thickness"], CREST["pitch"]
    cx, cy, cz = c
    half = s / math.sqrt(2)            # half the crest's diagonal
    fin_c = (cx, cy - half - 0.1 - 0.5 * math.sqrt(2), cz)   # a 1 px cube on its corner, just clear of the crest
    if knob:   # a cross of two boxes (a squat one and a tall one), round in silhouette, sitting on the pin's top
        ky = cy - half - 0.6 - 0.7
        finial = [pitched("knob", (cx - 0.7, ky - 0.45, cz - 0.7), (1.4, 0.9, 1.4), c, pitch, paint=r["knob"]),
                  pitched("knob_cap", (cx - 0.45, ky - 0.7, cz - 0.45), (0.9, 1.4, 0.9), c, pitch, paint=r["knob"])]
    else:
        finial = [am.turned(am.turned(am.box("finial", (fin_c[0] - 0.5, fin_c[1] - 0.5, fin_c[2] - 0.5), (1.0, 1.0, 1.0),
                                             paint=r["finial"]), "z", 45, fin_c), "x", -pitch, c)]
    parts = [
        # the shell: a back box and a visor box; the brow above the visor is under the crest
        am.span("shell", (-5.5, -9, -3.5), (5.5, -1, 5), paint=r["shell"]),
        am.span("visor", (-5.5, -6, -5), (5.5, -1, -3.5), skip="back", paint=r["visor"]),
        # the brow over the visor, mostly under the crest: without it the hat layer showed at the crest's sides
        am.span("brow", (-5.5, -9, -4.75), (5.5, -6, -3.5), skip=("back", "bottom"), paint=r["brow"]),
        # the crest, a square stood on its corner and pitched back
        am.diamond("crest", c, s, t, pitch=pitch, paint=r["crest"]),
        # the finial: a cube on its corner (bronze: a knob) on a short pin above the crest's top corner
        pitched("finial_pin", (cx - 0.3, cy - half - 0.6, cz - 0.3), (0.6, 0.8, 0.6), c, pitch, skip="ends",
                paint=r["pin"]),
        *finial,
        # crown comb: a ridge from behind the crest's top over the crown to the nape, stepped down to the back
        am.span("comb", (-0.5, -10.5, -1.0), (0.5, -9, 2.5), skip="bottom", paint=r["comb"]),
        am.span("comb_back", (-0.4, -9.9, 2.5), (0.4, -9, 4.75), skip="bottom", paint=r["comb"]),
        # nasal bar down to the clasp; grille ribs between the slits, ending at the visor's edge
        am.span("nasal", (-0.5, -3.0, -5.75), (0.5, -0.3, -5), skip="back", paint=r["nasal"]),
        *am.pair(am.span("rib_inner_right", (-2.4, -5.0, -5.5), (-1.6, -1.0, -5), skip="back", paint=r["rib"])),
        *am.pair(am.span("rib_outer_right", (-4.4, -6.0, -5.5), (-3.6, -1.0, -5), skip="back", paint=r["rib"])),
        # the gold collar ring; a bevor plate in front of it, so the gold shows at the sides as drawn, and the clasp.
        # Both reach 0.65 below the head, past the hat layer's bottom (+0.5), or that showed as a line round the neck.
        # The collar is closed underneath: open, the helm showed the sky through it from below (and on an armor stand)
        am.span("collar", (-4.75, -1, -4.75), (4.75, 0.65, 4.75), skip="top", paint=r["collar"]),
        am.span("bevor", (-3.75, -1, -5), (3.75, 0.65, -4.75), skip=("back", "top"), paint=r["bevor"]),
        am.span("clasp", (-0.7, -0.75, -5.4), (0.7, 0.4, -4.75), skip="back", paint=r["clasp"]),
        # neck guard: three lames at the back, each flaring further out
        *am.lames("nape", origin=(-5.5, -4.25, 5.1), size=(11, 1.5, 0.75), count=3, step=(0, 1.15, 0.2),
                  grow=(0.5, 0, 0), flare=8, flare_step=5, edge="top", toward="back", paint=r["nape"]),
    ]
    # horn fins: one plain bar rising up and out from each of the crest's side corners, its tip below the crest's top
    base = (-half + 0.35, cy - 0.3, cz + 0.5)
    parts += am.pair(rod("horn_right", base, 3.0, 1.25, out=39, back=18, paint=r["horn"]))
    # cheek fins, hinged at their back edge and splayed out, a step at the top front
    fin = am.hinge(am.span("cheek_right", (-6.5, -7, -5.25), (-5.5, -3.5, -1.25), paint=r["cheek"]), "back", 15)
    fin_top = am.hinge(am.span("cheek_top_right", (-6.5, -8.25, -4.0), (-5.5, -7, -1.25), paint=r["cheek_top"]),
                       "back", 15)
    parts += am.pair(fin) + am.pair(fin_top)
    return parts


# ---------------------------------------------------------------- chestplate
# Where arm and body plates face front or back they sit on a 0.25 px grid (arms at 2.7, 2.95, 3.2, 3.45, 3.7 and
# 3.95; body plates on the midpoints), so no two share a plane while the arms hang. Keep this if sizes change.
CHEST = dict(centre=(0.0, 2.1, -3.475), side=8.0, thickness=0.75, pitch=12.0)


def body(r):
    c, s, t, pitch = CHEST["centre"], CHEST["side"], CHEST["thickness"], CHEST["pitch"]
    cx, cy, cz = c
    half = s / math.sqrt(2)
    # pitched about its side corners (tucked under the pauldrons): the point juts forward, the top sinks behind the
    # head's face, so the plate never shows above the shoulders even without the helm
    plate = am.diamond("chev", c, s, t, pitch=pitch, paint=r["chev"])
    # the keel: a ridge down the plate's lower point, ending at the point (body y 7.6; the owner's tab ends near 7.4)
    keel = tipped(am.span("keel_tab", (-0.625, cy + half - 2.75, cz - t / 2 - 0.6), (0.625, cy + half, cz + 0.2),
                          paint=r["keel_tab"]), c, pitch)
    return [
        am.span("breast", (-5, -0.75, -3.075), (5, 6, 3.075), paint=r["breast"]),
        plate,
        keel,
        am.span("breast_rim", (-5.3, 5, -3.575), (5.3, 6.5, 3.575), paint=r["rim"]),
        am.span("waist", (-4.825, 5.75, -2.825), (4.825, 7.75, 2.825), skip="top", paint=r["waist"]),
        am.span("spine", (-0.5, -0.25, 3.075), (0.5, 4.0, 3.5), skip="front", paint=r["spine"]),
        *vee("back_chev", apex=(0.0, 5.0, 3.15), length=5.5, width=2.0, thickness=0.5, angle=38, stagger=-0.15,
             skip="front", paint=r["back_chev"]),
    ]


def arm(r):
    """The right arm: pauldron with cap, haute-piece and two lames; sleeve, straps, vambrace, flared cuff, glove."""
    return [
        am.span("pauldron_right", (-6, -3, -3.95), (2, 3, 3.95), paint=r["pauldron"]),
        am.box("cap_right", (-4.25, -4.1, -3.0), (3.75, 1.25, 6.0), pivot=(-4.25, -2.85, 0), rotation=(0, 0, -10),
               paint=r["cap"]),
        am.span("haute_right", (-1.6, -5.0, -2.75), (-0.6, -2.5, 2.75), paint=r["haute"]),
        *am.lames("paul_lame_right", origin=(-5.75, 2.6, -3.45), size=(7.25, 1.6, 6.9), count=2, step=(-0.25, 1.3, 0),
                  grow=(0.25, 0, 0.5), flare=4, flare_step=5, edge="left", toward="bottom", paint=r["paul_lame"]),
        am.span("sleeve_right", (-3.7, 3.0, -2.7), (1.7, 10.4, 2.7), skip="ends", paint=r["sleeve"]),
        am.span("strap_a_right", (-3.5, 5.5, -2.95), (1.95, 6.5, 2.95), skip="right", paint=r["strap"]),
        am.span("strap_b_right", (-3.5, 7.5, -2.95), (1.95, 8.5, 2.95), skip="right", paint=r["strap"]),
        am.span("vambrace_right", (-4.2, 4.75, -3.2), (0, 10.75, 3.2), skip="left", paint=r["vambrace"]),
        am.hinge(am.span("cuff_right", (-5.2, 5.25, -2.25), (-4.2, 10.75, 2.25), paint=r["cuff"]), "bottom", 18),
        am.span("glove_right", (-3.95, 10.25, -2.95), (1.95, 11.25, 2.95), skip="top", paint=r["glove"]),
        am.span("knuckle_right", (-4.45, 10.5, -2.0), (-3.95, 11.4, 2.0), skip="left", paint=r["knuckle"]),
    ]


# ---------------------------------------------------------------- leggings
def waist(r, buckle=False):
    """The belt on the body: under-layer base, two leather plates front and back, the strap right round; `buckle`
    (bronze) adds a brass buckle on the strap at the front, where the steel's strap only lightens."""
    # The belt runs 0.45 below the body and is closed at the bottom: when sneaking the body tips forward and its
    # underside turns toward a camera behind, which saw the wearer through an open bottom. 12.45 keeps the bottom face
    # clear of the jacket layer (12.25) and the front and back clear of the top lame's (its outer top corner: 12.485).
    out = [
        am.span("belt", (-5.2, 7.5, -3.325), (5.2, 12.45, 3.325), paint=r["belt"]),
        *am.pair(am.span("plate_right", (-5.0, 8, -3.575), (-1.0, 12, -3.325), skip="back", paint=r["plate_l"])),
        *am.pair(am.span("plate_back_right", (-5.0, 8, 3.325), (-1.0, 12, 3.575), skip="front", paint=r["plate_l"])),
        am.span("belt_strap", (-5.5, 9, -3.825), (5.5, 10, 3.825), skip="ends", paint=r["belt_strap"]),
    ]
    if buckle:
        out.append(am.span("buckle", (-1.5, 8.5, -4.1), (1.5, 10.5, -3.825), skip="back", paint=r["buckle"]))
    return out


SKIRT = [  # (top y, outer x, depth out from the leg's front and back), per leg, hip to ground; the top lame's front
    # (z -3.325) sits between the arms' planes, clear of the hanging vambrace's (3.2) at its outer top corner. Each lame
    # steps 0.5 further out: the roll lifts the next lame's outer face about 0.36 px (2.6 x sin 8), and the two must stay
    # COPLANAR_GAP apart where they overlap
    (1.25, -3.4, 1.325), (3.85, -3.9, 1.75), (6.45, -4.4, 2.25), (9.05, -4.9, 2.75)]
LAME_H = 2.85
LAME_IN = 2.1        # inner edge, leg x: past the centre line (1.9), so the two legs' lames close the V at the top
SKIRT_ROLL = 8.0     # bottom outward: the top edges dip toward the centre line
LEFT_OUT = 0.12      # the left leg's lames and breeches stand this much further out, so the halves never share a plane
HEM_DROP = 0.9       # the hem lame is this much taller: the roll lifts its outer corner about 1 px, and the owner's hem
                     # is dark down to the ground, with no sabaton showing under it


def leg(r, side="right", skirt=skirt_paint):
    """One leg's leggings parts (the leg box is x -2..2, y 0..12, z -2..2): the hip band, mail breeches and four
    lames. The lames are shells open at the bottom (the upper ones end inside the next), so a swinging leg shows mail
    inside the hem, not a slab. Built for the right leg; the left mirrors it, LEFT_OUT further out.
    The roll swings the lower part of each lame's inner side back inside the leg (to x 1.7), so a mail lining closes the
    inner side 0.25 px outside the pants layer (x 2.25), from inside the belt down to the hem: on the far leg, seen
    when walking, it hides the wearer's pants and skin. It reaches past the centre line into the other leg's skirt
    (standing, the other leg's hip band and lames hide it), so the skirt keeps its V at the centre line."""
    # The roll opens the two halves' inner edges up to 0.4 px apart toward each lame's foot (the V at the top needs
    # it). The breeches reach the lames' inner edge, past the centre line, with no inner face, so the two legs' overlap
    # behind that slit and it shows a dark seam, as the owner drew, not the player's own legs. The hip bands just meet
    # at the centre line: to overlap, the left one would need a plane of its own, and none is free between the
    # sleeve's (2.7) and the glove's (2.95).
    # The hip band starts 0.75 above the hip, inside the belt, and is closed on top: a leg swung back tips the top of
    # the pants layer forward under the belt, and from the front that showed. (Not at 1.0: vanilla boots' shell.)
    # Closed at the centre line and the lining on top too, or the piece showed the sky through it from some angles
    # (tools/art_check.py H1); the lining starts 0.15 below the hip band, so its top is not in the other hip's plane.
    out = [am.span("hip_right", (-2.825, -0.75, -2.825), (1.9, 1.5, 2.825), skip="bottom", paint=r["hip"]),
           am.span("lining_right", (1.9, -0.6, -2.55), (2.5, 12.5, 2.55), skip="right", paint=r["breeches"]),
           # from where the top lame's slit opens (y 2.7) down; higher they would only swing through the belt
           am.span("breeches_right", (-2.7, 2.5, -2.7), (LAME_IN, 12, 2.7), skip=("ends", "left"),
                   paint=r["breeches"])]
    for i, (top, outer, depth) in enumerate(SKIRT):
        hem = i == len(SKIRT) - 1
        h = LAME_H + (HEM_DROP if hem else 0.0)
        # each lame's foot is under the next; the hem's is closed, or the skirt showed the sky through it from below
        out.append(am.span(f"skirt_{i}_right", (outer, top, -2 - depth), (LAME_IN, top + h, 2 + depth),
                           pivot=(LAME_IN, top, 0), rotation=(0, 0, SKIRT_ROLL), paint=skirt(i),
                           skip=() if hem else "bottom"))
    if side == "right":
        return out
    left = []
    for p in am.mirror_all(out):
        if p.name.startswith(("skirt_", "breeches_")):
            p = replace(p, origin=(p.origin[0], p.origin[1], p.origin[2] - LEFT_OUT),
                        size=(p.size[0], p.size[1], p.size[2] + 2 * LEFT_OUT))
        left.append(p)
    return left


# ---------------------------------------------------------------- boots
def boot(r):
    """Greave with a knee cop, sabaton with three instep lames overlapping toward the toe, a diamond toe cap on the
    sabaton's front (under the skirt only its point shows below the hem)."""
    out = [
        # closed all round (tools/art_check.py H1): open on top and inside, the boot showed the sky through it
        am.span("greave_right", (-3.0, 4.5, -3.0), (1.75, 10.25, 3.0), paint=r["greave"]),
        am.diamond("poleyn_right", (-0.55, 5.2, -3.2), 2.4, 0.6, pitch=10, paint=r["poleyn"]),
        # the sabaton reaches the centre line, where the leggings' lining starts: between them the pants showed
        am.span("foot_right", (-3.0, 10.25, -3.25), (1.9, 12.75, 3.0), paint=r["foot"]),
        am.diamond("toe_right", (-0.6, 11.35, -3.65), 2.3, 0.6, pitch=25, paint=r["toe"]),
    ]
    for i in range(3):   # instep lames, scales overlapping toward the toe, each drooping at its front
        x0 = -3.15 - 0.15 * i
        out.append(am.span(f"instep_right_{i}", (x0, 9.9 + 0.55 * i, -3.45 - 0.55 * i),
                           (1.75, 10.8 + 0.55 * i, -1.6 - 0.55 * i),
                           pivot=(0, 9.9 + 0.55 * i, -1.6 - 0.55 * i), rotation=(12, 0, 0), skip="left",
                           paint=r["instep"]))
    return out


# ---------------------------------------------------------------- the set
def model(metal="steel"):
    """{item: {bone: [parts]}} for one metal's four pieces: "steel" (the owner's design) or "bronze" (its steam-age
    make: bronze_paints(), the brass knob and buckle, riveted lames)."""
    bronze = metal == "bronze"
    r = bronze_paints() if bronze else paints("bolt")
    skirt = bronze_skirt_paint if bronze else skirt_paint
    a, b = arm(r), boot(r)
    return {
        f"{metal}_helmet": {"head": helmet(r, knob=bronze)},
        f"{metal}_chestplate": {"body": body(r), "right_arm": a, "left_arm": am.mirror_all(a)},
        f"{metal}_leggings": {"body": waist(r, buckle=bronze), "right_leg": leg(r, "right", skirt),
                              "left_leg": leg(r, "left", skirt)},
        f"{metal}_boots": {"right_leg": b, "left_leg": am.mirror_all(b)},
    }


SETS = [am.ArmorSet("steel_knight", STEEL, model("steel")), am.ArmorSet("bronze_knight", BRONZE, model("bronze"))]
