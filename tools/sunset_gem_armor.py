"""Sunset Gem: the owner's sunset design (a render of a player in the full set from the front, beside small renders of
each piece on its own) as 3D worn models for jugcraft:sunset_gem_helmet, _chestplate, _leggings and _boots, on the
toolkit in tools/armor_models.py (shapes) and tools/armor_paint.py (the atlas).

What the owner drew, and where it is here:
    helmet      a crown of gem shards radiating round the top of the helm: a broad cream spike over the brow, a smaller
                one at each front corner, shards splayed out at the sides, nearly level by the ears, with sparkles off
                their tips; a crown band of cream and yellow blocks; under it a cream brow over the face opening, framed
                by posts and peach cheek guards, a yellow nose bar down from the brow, the chin open between the cheeks
    chestplate  wings of gem shards: a crescent from each shoulder, low at the collar and bending up to a big cream
                sparkle, with a steeper shard inside it and two small sparkles; layered pauldrons tilted up toward the
                outside under the wings; a breastplate in strips, its collar a cream U round a coral inside; olive
                forearms
    leggings    a long striped skirt: the belt's red V; flaps hanging from it, fanning out to clusters of sparkles at
                the hips, lined red, then crimson, along their inner edges (the owner's frames); the centre panel
                between them in strips; a hem with the crimson band and its mauve middle low on the skirt
    boots       hidden by the skirt in the full view and cut off in the piece render, so drawn from what that shows: a
                crimson cuff with a mauve middle, a peach strip, coral below with crimson blocks, and a shard flaring up
                from the outer side to a sparkle, as the leggings' flaps do
Colours: armor_paint.SUNSET_GEM, sampled from the render (it is unlit, so its tones are texture colours as they stand):
the sunset runs from cream and pale yellow on the crown and wings, through apricot and peach on the chest and pauldrons,
to coral and red on the skirt, in broken horizontal strips, with crimson and mauve low on the skirt and boots, over an
olive under-layer.

The olive face and arms: the owner's helmet and chestplate rendered on their own show no olive (the helm's face opening
shows its peach inside; the chestplate has no forearms), so the olive is most likely the figure under the armor. A worn
helm must be closed (tools/art_check.py, H1), so something has to fill the opening: here it is an olive plate behind
it, and the forearms get olive sleeves and bracers (wider below the elbow, as the full view's arms are), as White
Diamond's charcoal is drawn, so the set looks as the owner's full view does whatever skin wears it.

The render shows only the front. The sides and back are drawn in its own words: the crown's shards lean back behind
the head round a back spike; the wings fan out behind the shoulders, meeting at a small cream gem between the shoulder
blades; the breastplate's back carries a cream collar band over its strips; the belt's V, the flaps with their red
and crimson inner edges and the hem's mauve band repeat behind. Every box is closed: a face is left out only where
another box of the same piece and bone covers it (the face frame against the helm, the spikes' feet inside the crown
band, the gem against the breastplate), so the set shows no holes alone, on an armor stand's thin limbs
(tools/art_check.py, H1). Each sparkle is a cube of its own on the bone of the shard it lights, clear of every other
face's plane.

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left.
Preview: python3 tools/armor_preview.py --set sunset_gem (--compare owner_design.png: their front view beside ours,
from a camera fitted to it, in their pose)
"""
import math
from dataclasses import replace

import armor_models as am
from armor_paint import SUNSET_GEM, P


def rows(*tones, breaks=False):
    """One tone per texel row, cycling, each row broken into runs of the next tone darker (the owner's strips); with
    `breaks`, a hammer pass also breaks long runs a tone lighter or darker."""
    spec = P("lames", rows=1, tones=tuple((t, t) for t in tones), centre=None)
    return [spec, P("hammer", keep_border=False, every=4)] if breaks else spec


def solid(tone):
    return P("solid", tone=tone)


def marks(*rects):
    return P("marks", rects=list(rects))


def cols(*tones, width=8, side="left"):
    """Strips down a face, one tone per texel column from its `side` edge, then a hammer pass to break them up: the
    owner's strips along a long plate."""
    x = (lambda i: i) if side == "left" else (lambda i: -1 - i)
    return [P("marks", rects=[(x(i), 0, 1, 99, tones[i % len(tones)]) for i in range(width)]),
            P("hammer", keep_border=False, every=4)]


# ---------------------------------------------------------------- paint
def paints():
    """Role -> paint spec, the owner's patterns read off their render: each part paints its strips from its own window
    of the sunset, cream and yellow at the top of the armor to coral, red, crimson and mauve at the feet."""
    # the belt's V: its arms from the top corners to the point at the foot, red over coral, peach inside the V
    belt_v = [solid("seam"), marks((0, 0, 2, 1, "void"), (-2, 0, 2, 1, "void"), (2, 0, 6, 1, "dark"),
                                   (0, 1, 4, 1, "void"), (-4, 1, 4, 1, "void"), (0, 2, 99, 1, "void"))]
    return {
        # the helm: its front is the olive face behind the opening (a lighter band across, darker at the sides and the
        # chin, as drawn); its sides and back run yellow at the top to coral at the foot
        "helm": {"front": [solid("under_mid"), marks((0, 4, 99, 1, "under_light"), (2, 5, 1, 2, "under_dark"),
                                                     (-3, 5, 1, 2, "under_dark"), (0, 8, 99, 2, "under_dark"))],
                 "bottom": solid("seam"),
                 "*": rows("mid_light", "mid_light", "mid", "mid_light", "mid", "dark", "mid", "dark", "seam", "dark",
                           breaks=True)},
        # the crown band: cream blocks under the spikes, yellow between
        "ring": {"top": rows("light", "mid_light"), "bottom": solid("mid"),
                 "*": [solid("light"), marks((2, 0, 2, 9, "mid_light"), (-4, 0, 2, 9, "mid_light"))]},
        "brow": {"*": [rows("light", "light", "mid"), marks((0, 0, 1, 99, "mid_light"), (-1, 0, 1, 99, "mid_light"))]},
        "post": {"*": rows("mid", "dark", "mid", "dark")},
        "nose": {"*": solid("mid_light")},
        "cheek": {"front": [rows("dark", "mid", "dark", "seam"), marks((0, 0, 1, 3, "seam"))],
                  "bottom": solid("void"), "*": rows("dark", "mid", "dark", "seam")},
        # the spikes: cream down the middle, yellow edges (the centre spike), cream and yellow strips (the others)
        "spike": {"front": [solid("light"), marks((0, 0, 1, 99, "mid_light"), (-1, 0, 1, 99, "mid_light"),
                                                 (1, 2, 2, 1, "mid_light"))],
                  "*": rows("mid_light", "light", "mid_light", "mid")},
        "shard": {"*": rows("light", "mid_light", "light", "mid", "mid_light", "dark")},
        # the wings' blades: strips along their length, cream on the upper edge, then yellow and apricot
        "blade": {"*": rows("light", "light", "mid_light", "mid_light", breaks=True)},
        # the gem between the shoulder blades, where the wings meet: cream round a yellow heart
        "gem": {"back": [solid("light"), marks((1, 1, 1, 1, "mid_light"))], "*": solid("mid_light")},
        "spark": solid("light"), "spark_y": solid("mid_light"), "spark_p": solid("mid"), "spark_o": solid("dark"),
        "spark_c": solid("seam"),
        # the breastplate: the cream U of the collar round its coral inside, then strips yellow and apricot to peach
        # and coral, darker at the edges
        "breast": {"front": [rows("mid", "mid", "light", "light", "mid_light", "mid", "mid_light", "mid", "dark",
                                  "mid", "mid_light", breaks=True),
                             marks((3, 0, 4, 2, "seam"), (5, 0, 2, 2, "dark"), (2, 0, 1, 2, "light"),
                                   (-3, 0, 1, 2, "light"), (2, 2, 6, 2, "light"), (0, 2, 1, 2, "mid_light"),
                                   (-1, 2, 1, 2, "mid_light")),
                             marks((0, 4, 1, 99, "dark"), (-1, 4, 1, 99, "dark"))],
                   "back": [rows("light", "mid_light", "mid_light", "mid", "mid_light", "mid", "dark", "mid", "dark",
                                 "seam", "dark", breaks=True),
                            marks((0, 0, 99, 1, "light"), (0, 2, 1, 99, "dark"), (-1, 2, 1, 99, "dark"))],
                   "top": rows("mid_light", "mid"),
                   "bottom": solid("seam"),
                   "*": rows("mid_light", "mid", "mid_light", "mid", "dark", "mid", "seam", "dark", "mid", "dark",
                             "seam", breaks=True)},
        # the pauldrons' lames: yellow and apricot on top, peach, then coral and red on the lowest
        "pauldron": {"top": rows("mid_light", "mid"), "*": rows("mid_light", "mid", "dark", "mid", breaks=True)},
        "pauldron_b": {"*": rows("mid", "dark", "mid", "mid_light", "dark", breaks=True)},
        "pauldron_c": {"*": rows("dark", "mid", "dark", "seam", "dark", breaks=True)},
        # olive sleeves and bracers: a light outer edge and a dark L at the wrist, as drawn
        "sleeve": P("under", light="under_light"),
        "bracer": {"front": [solid("under_mid"),
                             marks((0, 0, 99, 1, "under_light"), (0, 1, 1, 99, "under_light"),
                                   (2, 2, 99, 1, "under_dark"), (0, 4, 3, 1, "under_dark"),
                                   (-3, -2, 2, 1, "under_darkest"), (-2, -2, 1, 2, "under_darkest"))],
                   "back": [solid("under_mid"),
                            marks((0, 0, 99, 1, "under_light"), (-1, 1, 1, 99, "under_light"),
                                  (0, 2, 4, 1, "under_dark"), (-3, 4, 3, 1, "under_dark"),
                                  (1, -2, 2, 1, "under_darkest"), (1, -2, 1, 2, "under_darkest"))],
                   "top": solid("under_light"), "bottom": solid("under_dark"),
                   "*": [solid("under_mid"), marks((0, 0, 99, 1, "under_light"), (0, 2, 99, 1, "under_dark"),
                                                   (0, -2, 99, 2, "under_dark"))]},
        "belt": {"front": belt_v, "back": belt_v, "*": rows("void", "seam", "void")},
        # the under-skirt, the centre panel between the flaps: apricot and peach at the top, coral, a red strip, then
        # the crimson band at its foot
        "skirt": {"*": rows("void", "mid", "dark", "seam", "void", "dark", "gold_light", "seam", "seam", "seam")},
        # the flaps: strips along their length, apricot at the outer edge to peach and coral, lined red (crimson
        # lower down) along the inner edge, as the owner's frames
        "flap": {"front": [*cols("mid", "dark", "mid", "dark", "seam"), marks((-1, 0, 1, 3, "void"),
                                                                             (-1, 3, 1, 99, "gold_light"))],
                 "back": [*cols("mid", "dark", "mid", "dark", "seam", side="right"),
                          marks((0, 0, 1, 3, "void"), (0, 3, 1, 99, "gold_light"))],
                 "top": solid("void"), "bottom": solid("seam"), "left": solid("gold_light"),
                 "*": rows("mid", "dark", "mid", "seam", "dark", "seam", breaks=True)},
        "shard_low": {"*": rows("mid", "dark", "seam", breaks=True)},
        # the hem: a peach strip, then coral, then the crimson band with its mauve middle low on the skirt
        "hem": {"front": [rows("dark", "gold_light", "gold_light", "seam"), marks((-3, 1, 3, 2, "gold_dark"))],
                "back": [rows("dark", "gold_light", "gold_light", "seam"), marks((0, 1, 3, 2, "gold_dark"))],
                "*": rows("dark", "gold_light", "gold_light", "seam")},
        # boots: the crimson cuff with its mauve middle, a peach strip, then coral with crimson blocks
        "boot": {"front": [rows("gold_light", "dark", "seam", "seam", "void"),
                           marks((-3, 0, 3, 1, "gold_dark"), (1, 2, 2, 2, "gold_light"), (-3, 2, 2, 2, "gold_light"))],
                 "back": [rows("gold_light", "dark", "seam", "seam", "void"),
                          marks((0, 0, 3, 1, "gold_dark"), (-3, 2, 2, 2, "gold_light"), (1, 2, 2, 2, "gold_light"))],
                 "top": solid("gold_light"), "bottom": solid("void"),
                 "*": rows("gold_light", "dark", "seam", "seam", "void", breaks=True)},
    }


# ---------------------------------------------------------------- shape helpers
def rod(name, base, length, section, out, back, depth=None, **options):
    """A bar standing on `base` (its bottom centre), `section` wide and `depth` deep (default: square), tipped `out`
    degrees toward the model's right (-x) and leaning `back` degrees backward; build it on the model's right and mirror
    it for the left."""
    bx, by, bz = base
    depth = depth or section
    return am.box(name, (bx - section / 2, by - length, bz - depth / 2), (section, length, depth), pivot=base,
                  rotation=(-back, 0, -out), **options)


def blade(name, root, length, width, depth, angle, back=0.0, **options):
    """A flat bar from `root` (the middle of its inner end) running `length` px out toward the model's right (-x),
    `width` tall and `depth` thick, lifted `angle` degrees (outer end up) and swung `back` degrees (outer end back), so
    painted rows run along it; build it on the model's right and mirror it for the left."""
    rx, ry, rz = root
    return am.box(name, (rx - length, ry - width / 2, rz - depth / 2), (length, width, depth), pivot=root,
                  rotation=(0, back, angle), **options)


def end(part, beyond=0.0):
    """The middle of a blade's outer end, plus `beyond` px further along it (bone space)."""
    return am.place(part, (part.origin[0] - beyond, part.origin[1] + part.size[1] / 2,
                           part.origin[2] + part.size[2] / 2))


def tip(part, beyond=0.0):
    """Where a bar's axis leaves its top face, plus `beyond` px further along it (bone space)."""
    cx = part.origin[0] + part.size[0] / 2
    cz = part.origin[2] + part.size[2] / 2
    return am.place(part, (cx, part.origin[1] - beyond, cz))


def sparkle(name, centre, side, paint, turn=45.0):
    """A small cube standing on its corner, facing front: a sparkle detached from the shard it lights."""
    return am.diamond(name, centre, side, side, roll=-turn, paint=paint)


# ---------------------------------------------------------------- helmet
def helmet(r):
    """The helm, 0.9 clear of the head (0.4 off the hat layer) and closed underneath 0.65 below it, so a turned head
    never shows the wearer from below; its front is the olive face. Over it: the crown band, 0.5 proud all round; the
    brow below the band, the posts beside the face opening, the nose bar and the cheek guards, 0.5 proud of the face
    (the cheeks 0.65, so they cover the posts' feet). The crown's spikes rise from inside the band: the broad one at
    the front, a smaller one at each front corner tipped out a little, then pairs splayed wider round the sides (the
    lower pair nearly level, a sparkle off each tip) and leaning back behind, round a back spike."""
    parts = [
        am.span("helm", (-4.9, -9.15, -4.9), (4.9, 0.65, 4.9), skip="top", paint=r["helm"]),
        am.span("ring", (-5.4, -9.65, -5.4), (5.4, -8.4, 5.4), paint=r["ring"]),
        am.span("brow", (-4.9, -8.4, -5.4), (4.9, -6.3, -4.9), skip=("back", "top"), paint=r["brow"]),
        *am.pair(am.span("post_right", (-4.9, -6.3, -5.4), (-3.2, -2.5, -4.9), skip=("back", "top", "bottom"),
                         paint=r["post"])),
        am.span("nose", (-1.05, -6.3, -5.4), (1.05, -5.0, -4.9), skip=("back", "top"), paint=r["nose"]),
        *am.pair(am.span("cheek_right", (-4.9, -2.5, -5.55), (-1.2, 0.65, -4.9), skip="back", paint=r["cheek"])),
        am.span("spike", (-2.15, -13.3, -5.2), (2.15, -9.4, -3.0), skip="bottom", paint=r["spike"]),
    ]
    flank = rod("flank_right", (-4.1, -9.4, -4.3), 2.7, 1.8, out=6, back=0, skip="bottom", paint=r["spike"])
    side = rod("side_right", (-5.0, -9.4, -1.8), 3.4, 1.6, out=40, back=0, skip="bottom", paint=r["shard"])
    low = rod("low_right", (-5.1, -8.9, 1.0), 3.2, 1.5, out=80, back=6, skip="bottom", paint=r["shard"])
    rear = rod("rear_right", (-3.2, -9.4, 3.9), 3.0, 1.6, out=26, back=34, skip="bottom", paint=r["shard"])
    parts += am.pair(flank) + am.pair(side) + am.pair(low) + am.pair(rear)
    parts.append(rod("back_spike", (0.0, -9.4, 4.3), 3.6, 2.2, out=0, back=32, skip="bottom", paint=r["spike"]))
    parts += am.pair(sparkle("crown_spark_right", tip(low, 1.2), 1.5, r["spark"]))
    return parts


# ---------------------------------------------------------------- chestplate
def chain(name, root, links, overlap=0.5, **options):
    """Blades end to end, each (length, width, depth, angle, back, paint) starting `overlap` px inside the end of the
    one before: a band that bends up as the angles grow. Named name_0, name_1..."""
    out = []
    for i, (length, width, depth, angle, back, paint) in enumerate(links):
        if out:
            root = end(out[-1], -overlap)
        out.append(blade(f"{name}_{i}_right", root, length, width, depth, angle, back=back, paint=paint, **options))
    return out


def wing(r):
    """The right wing (body bone): a crescent of three blades from the shoulder above the pauldron, flat at first and
    bending up to the big sparkle at its tip, two small sparkles inside it; a thinner shard rising from its middle."""
    band = chain("wing", (-3.4, -2.4, 1.0), [(3.8, 3.2, 1.6, 18.0, 4.0, r["blade"]),
                                              (3.8, 3.0, 1.5, 34.0, 6.0, r["blade"]),
                                              (2.8, 2.6, 1.4, 48.0, 8.0, r["blade"])])
    rise = blade("wing_rise_right", end(band[0], -1.0), 4.2, 1.8, 1.1, 62.0, back=10.0, paint=r["blade"])
    tip_x, tip_y, tip_z = end(band[-1], 1.3)
    return band + [rise,
                   sparkle("wing_spark_right", (tip_x, tip_y, tip_z), 2.3, r["spark"]),
                   sparkle("wing_spark_b_right", end(rise, 0.9), 1.2, r["spark_y"]),
                   # the third of the tip's cluster, between the other two and clear of the blades
                   sparkle("wing_spark_c_right", (tip_x + 2.0, tip_y - 1.5, tip_z - 0.2), 1.1, r["spark_p"])]


def body(r):
    """The breastplate, its sides 0.65 off the body (clear of the jacket and of vanilla leggings' 0.5 shell); the gem
    between the shoulder blades, its face against the breastplate's back left out; the two wings."""
    out = [am.span("breast", (-4.65, -0.6, -3.1), (4.65, 9.6, 3.1), paint=r["breast"]),
           am.diamond("back_gem", (0.0, 2.6, 3.4), 2.6, 0.6, paint=r["gem"], skip="front")]
    w = wing(r)
    return out + w + am.mirror_all(w)


def arm(r):
    """The right arm: three lames of the pauldron, each shorter than the one above, tilted up toward the outside under
    the wing (the owner's pose adds the arm's 12 degrees out); the olive sleeve, closed all round the arm from 0.45
    above it, under the lames; the olive bracer over the forearm, wider than the sleeve as drawn, its front and back
    0.7 off the arm (clear of the belt's plane at 0.85 and the left skirt's at 0.57 standing)."""
    roll = dict(pivot=(-0.1, -2.3, 0.0), rotation=(0, 0, 6.0))
    return [
        am.span("pauldron_right", (-6.3, -2.3, -3.4), (-0.1, 0.2, 3.4), paint=r["pauldron"], **roll),
        am.span("pauldron_b_right", (-5.6, 0.0, -3.2), (-0.3, 2.5, 3.2), paint=r["pauldron_b"], **roll),
        am.span("pauldron_c_right", (-4.8, 2.3, -3.0), (-0.5, 5.4, 3.0), paint=r["pauldron_c"], **roll),
        am.span("sleeve_right", (-3.45, -2.45, -2.45), (1.45, 4.8, 2.45), paint=r["sleeve"]),
        am.span("bracer_right", (-3.7, 4.5, -2.7), (1.7, 10.6, 2.7), paint=r["bracer"]),
    ]


# ---------------------------------------------------------------- leggings
LEFT_OUT = 0.12
FLAP_ROLL = 27.0


def waist(r):
    """The belt with the red V across its front and back, 0.85 off the body (clear of vanilla leggings' 1 px shell),
    tucked under the breastplate's foot."""
    return [am.span("belt", (-4.85, 9.4, -2.85), (4.85, 12.45, 2.85), paint=r["belt"])]


def leg(r, side="right"):
    """One leg's leggings parts (the leg box is x -2..2, y 0..12, z -2..2), built for the right leg and mirrored: the
    under-skirt, closed all round 0.45 off the leg (its front is the centre panel; it covers the flap's inner side where
    that crosses the leg); the flap; the hem, its inner side 0.65 past the leg; the hip sparkles. The left leg's
    under-skirt and hem are inflated LEFT_OUT, so the two legs' faces never share a plane."""
    # the flap hangs from the belt's V: rolled bottom outward about its top outer corner, so its top edge slopes down
    # toward the centre line as the V's arm does and both its edges splay out; its inner top corner at (-0.6, 0.4), so
    # its top face stays under vanilla armor's 1 px shell over the leg (the under-skirt's top covers it there)
    width, length = 4.5, 8.0
    c, s = math.cos(math.radians(FLAP_ROLL)), math.sin(math.radians(FLAP_ROLL))
    outer = (-0.6 - width * c, 0.4 - width * s)
    flap = am.box("flap_right", (outer[0], outer[1], -3.85), (width, length, 7.7), pivot=(outer[0], outer[1], 0.0),
                  rotation=(0, 0, FLAP_ROLL), paint=r["flap"])
    fx, fy, _ = am.place(flap, (outer[0], outer[1] + length, 0.0))   # its outer bottom corner
    out = [
        am.span("skirt_right", (-2.45, -0.5, -2.45), (2.45, 8.8, 2.45), paint=r["skirt"]),
        flap,
        am.span("hem_right", (-3.4, 6.0, -3.6), (2.65, 9.1, 3.6), paint=r["hem"]),
        # the sparkles flaring from the flap's outer foot, as drawn: a big one beyond it, two small ones by it
        sparkle("hip_spark_right", (fx - 1.6, fy + 0.2, -0.8), 1.9, r["spark_o"]),
        sparkle("hip_spark_b_right", (fx - 1.0, fy - 2.0, -1.3), 1.1, r["spark_p"]),
        sparkle("hip_spark_c_right", (fx + 0.2, fy + 1.9, -1.9), 1.0, r["spark_c"]),
    ]
    if side == "right":
        return out
    return [replace(p, inflate=LEFT_OUT) if p.name.startswith(("skirt", "hem")) else p for p in am.mirror_all(out)]


# ---------------------------------------------------------------- boots
def boot(r):
    """The boot, closed all round, its inner side 0.85 past the leg (clear of vanilla leggings' 0.5 shell and of the
    hem's side); a shard flaring up from its outer side to a sparkle, below the leggings' flap. The left boot is
    inflated LEFT_OUT (model())."""
    b = am.span("boot_right", (-3.5, 8.5, -3.95), (2.85, 12.75, 3.95), paint=r["boot"])
    shard = blade("boot_shard_right", (-3.2, 10.6, 0.0), 2.4, 1.2, 1.0, 30.0, paint=r["shard_low"])
    return [b, shard, sparkle("boot_spark_right", end(shard, 1.0), 1.2, r["spark_o"])]


# ---------------------------------------------------------------- the set
def model():
    """{item: {bone: [parts]}} for the four pieces."""
    r = paints()
    a, b = arm(r), boot(r)
    return {
        "sunset_gem_helmet": {"head": helmet(r)},
        "sunset_gem_chestplate": {"body": body(r), "right_arm": a, "left_arm": am.mirror_all(a)},
        "sunset_gem_leggings": {"body": waist(r), "right_leg": leg(r, "right"), "left_leg": leg(r, "left")},
        "sunset_gem_boots": {"right_leg": b, "left_leg": [replace(p, inflate=LEFT_OUT) if p.name == "boot_left" else p
                                                          for p in am.mirror_all(b)]},
    }


SETS = [am.ArmorSet("sunset_gem", SUNSET_GEM, model())]
