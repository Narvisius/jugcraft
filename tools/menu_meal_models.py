"""Sculpted models of eight placed menu meals (tools/menu.py), replacing tools/feast_meal_models.py's flat-colour
templates for them (owner request, Group C placed-food remodels): Cornbread Stuffing, Vegetable Noodles, Squid Ink
Pasta, Bacon and Eggs, Steak and Potatoes, Roasted Mutton Chops, Pasta with Meatballs and Tomato Sauce. Every other
placed dish keeps its template (tools/menu_data.py dish_model).

Each meal is its food built piece by piece on closed dishware:
- dishware: a wooden or glazed plate (a foot, a floor and a rim one pixel high with stepped corners), a wooden bowl
  (a foot, a body, walls lined inside, the contents filling it below the rim as its cap) or the owner's Skillet;
- food: PIECES below, each a box of one material (tools/menu_meal_textures.py MEALS). The meal's <dish>_top texture
  is the dish seen from above, drawn from these very pieces, so every upward face reads it by position (u = x, v = z,
  one texel a model pixel) and pieces at one height that overlap draw the same texels; a piece's sides read its
  material's rows in <dish>_side, top-aligned. A turned piece (bones, strands, the spoon) reads its material's rows on
  its sides and its material's swatch (or the top drawing) on top.

A model faces south (tools/menu_data.py: the blockstate turns it to face whoever set it down). Its pieces stay inside
the dish's outline (PlacedDishBlock.DishShape: PLATE 1..15 across and 5.5 high, BOWL 3..13 and 6.5). The writer closes
the models (model_writer.finish_closed), leaves out faces buried inside other boxes (drop_buried) and keeps faces off
each other's planes (finish_elements).
"""
import model_writer
import menu_meal_textures as tex

SIDES = ("north", "south", "east", "west")
FACES = SIDES + ("up", "down")


def P(lo, hi, mat, **options):
    """A piece of food: a box from lo to hi of material `mat`. Options: rot=(axis, angle, origin) turns it (its sides
    then read its material's rows, and its top its material's swatch, or the top drawing by position with
    top="pos"); down=True draws its under face (it overhangs or arches); skip=faces left out (resting on or buried
    in something); faces_mat={face: material} for a side of another material."""
    return {"lo": lo, "hi": hi, "mat": mat, **options}


# ---------------------------------------------------------------- dishware

# Rows of the dishware side textures (tools/menu_meal_textures.py meal_wood_side and meal_glazed_side).
RIM_TOP, LIP, LINING, FOOT, UNDER = 0, 1, 6, 10, 12


def band(texture, row, lo, hi, face, rows=None):
    """The uv of a side or flat face reading `rows` (default: the face's height) of `texture` from `row`, along its
    position."""
    (x0, y0, z0), (x1, y1, z1) = lo, hi
    if face in ("north", "south"):
        return {"texture": texture, "uv": [x0, row, x1, row + (rows if rows is not None else y1 - y0)]}
    if face in ("east", "west"):
        return {"texture": texture, "uv": [z0, row, z1, row + (rows if rows is not None else y1 - y0)]}
    # up / down: a strip along the longer side; a strip running along z is turned to read the row along x
    if (z1 - z0) > (x1 - x0):
        return {"texture": texture, "uv": [z0, row, z1, row + min(rows or 1, x1 - x0)], "rotation": 90}
    return {"texture": texture, "uv": [x0, row, x1, row + min(rows or 1, z1 - z0)]}


def dish_box(lo, hi, side, faces, top=None, row=LIP, inner=()):
    """A box of dishware: its upward face by position on `top` (or the rim-top row), its sides `row` and on (the lip
    and wall rows, or the foot's), its `inner` faces (towards the food) the lining rows, its under face the
    underside rows."""
    e = {"from": list(lo), "to": list(hi), "faces": {}}
    for face in faces:
        if face == "up":
            e["faces"]["up"] = ({"texture": top, "uv": [lo[0], lo[2], hi[0], hi[2]]} if top
                                else band(side, RIM_TOP, lo, hi, "up"))
        elif face == "down":
            e["faces"]["down"] = band(side, UNDER, lo, hi, "down", rows=min(4, max(hi[0] - lo[0], hi[2] - lo[2])))
        else:
            e["faces"][face] = band(side, LINING if face in inner else row, lo, hi, face)
    return e


def plate(top="#plate", side="#plate_side"):
    """A plate 12 across: a foot, a floor with stepped corners and a rim one pixel high. The well is x and z 3..13 less
    its corner steps (rows 3 and 12 span x 4..12); food stands on the floor at y 1.5; the rim's top is at 2.5."""
    out = [dish_box((5, 0, 5), (11, 0.5, 11), side, SIDES + ("down",), row=FOOT),
           dish_box((3, 0.5, 3), (13, 1.5, 13), side, FACES, top=top, row=LIP + 1)]
    # the rim walls stand from the floor's foot to the rim's top: outer face lip over body, inner face lined
    for lo, hi, inner in (((4, 0.5, 2), (12, 2.5, 3), "south"), ((4, 0.5, 13), (12, 2.5, 14), "north"),
                          ((2, 0.5, 4), (3, 2.5, 12), "east"), ((13, 0.5, 4), (14, 2.5, 12), "west")):
        e = dish_box(lo, hi, side, FACES, inner=(inner,))
        face = e["faces"][inner]
        face["uv"][1], face["uv"][3] = LINING - 1, LINING + 1  # the floor hides its lower half
        out.append(e)
    # the stepped corners of the rim, on the floor's corners
    for x, z in ((3, 3), (12, 3), (3, 12), (12, 12)):
        out.append(dish_box((x, 1.5, z), (x + 1, 2.5, z + 1), side, ("up",) + SIDES))
    return out


def bowl(a, b, wall_top, fill_top, side="#bowl_side", contents=None):
    """A wooden bowl from a to b across that flares as it rises: a foot, a body two pixels in, a lower ring of walls
    one pixel in and the upper ring out to its full width with corners stepped twice, each ring overhanging the one
    below; the walls are lined inside. `contents` (a material) fills it to `fill_top` (below the rim), closing it.
    Returns the dish's elements and the contents' pieces."""
    m, mid = (a + b) / 2, round(1.5 + (wall_top - 1.5) * 0.4, 2)
    # the body is solid wood up to the lower ring's top (the contents start there); its sides show below that ring
    out = [dish_box((m - 3, 0, m - 3), (m + 3, 0.5, m + 3), side, SIDES + ("down",), row=FOOT),
           dish_box((a + 2, 0.5, a + 2), (b - 2, mid, b - 2), side, SIDES + ("down",), row=FOOT - (mid - 0.5))]
    lo_row = LIP + round(wall_top - mid)
    for y0, y1, k, row in ((1.5, mid, 1, lo_row), (mid, wall_top, 0, LIP)):
        i0, i1 = a + 2, b - 2  # the walls run between the stepped corners
        for lo, hi, inner in (((i0, y0, a + k), (i1, y1, a + k + 1), "south"), ((i0, y0, b - k - 1), (i1, y1, b - k), "north"),
                              ((a + k, y0, i0), (a + k + 1, y1, i1), "east"), ((b - k - 1, y0, i0), (b - k, y1, i1), "west")):
            out.append(dish_box(lo, hi, side, FACES, inner=(inner,), row=row))
    for x, z in ((a + 1, a + 1), (b - 2, a + 1), (a + 1, b - 2), (b - 2, b - 2)):
        out.append(dish_box((x, mid, z), (x + 1, wall_top, z + 1), side, FACES, row=LIP,
                            inner=("east" if x == a + 1 else "west", "south" if z == a + 1 else "north")))
    fill = []
    if contents:
        fill = [P((a + 1, mid, a + 2), (b - 1, fill_top, b - 2), contents), P((a + 2, mid, a + 1), (b - 2, fill_top, b - 1), contents)]
    return out, fill


def skillet():
    """The owner's Skillet (tools/kitchen_data.py skillet_model, its textures unchanged): a cast-iron pan 10 by 10 with
    walls two pixels high and its wooden handle to the back (north), here shortened to stay inside the dish's outline.
    The pan's floor has no top face: the stuffing's bed covers it."""
    def face(texture, uv):
        return {"texture": texture, "uv": list(uv)}
    s, b = "#pan_side", "#pan_bottom"
    out = [{"from": [3, 0, 4], "to": [13, 1, 14], "faces": {
        "down": face(b, (1, 1, 15, 15)), "north": face(s, (0, 2, 10, 3)), "south": face(s, (0, 2, 10, 3)),
        "east": face(s, (0, 2, 10, 3)), "west": face(s, (0, 2, 10, 3))}}]
    for lo, hi, sides in (((3, 1, 4), (13, 3, 5), (10, 1)), ((3, 1, 13), (13, 3, 14), (10, 1)),
                          ((3, 1, 5), (4, 3, 13), (1, 8)), ((12, 1, 5), (13, 3, 13), (1, 8))):
        wide, deep = sides
        out.append({"from": list(lo), "to": list(hi), "faces": {
            "north": face(s, (0, 0, wide, 2)), "south": face(s, (0, 0, wide, 2)),
            "east": face(s, (0, 0, deep, 2)), "west": face(s, (0, 0, deep, 2)), "up": face(s, (0, 0, 1, 1))}})
    out.append({"from": [7, 1.5, 1], "to": [9, 2.5, 4], "faces": {
        "north": face(s, (7, 7, 9, 8)), "east": face(s, (6, 7, 9, 8)), "west": face(s, (6, 7, 9, 8)),
        "up": face(s, (7, 6, 9, 9)), "down": face(s, (7, 6, 9, 9))}})
    return out


# ---------------------------------------------------------------- the meals' food

def meatball(x, z, y, mat="meatball"):
    """A meatball two and a half pixels across and under two high, rounded: two crossed boxes and a cap."""
    return [P((x, y, z + 0.5), (x + 2.5, y + 1.3, z + 2), mat), P((x + 0.5, y, z), (x + 2, y + 1.3, z + 2.5), mat),
            P((x + 0.5, y + 1.3, z + 0.5), (x + 2, y + 1.75, z + 2), mat)]


def strand(x0, x1, z, y, angle, mat, width=0.6, height=0.3):
    """A loose strand (a noodle, a julienned vegetable) lying across x0..x1 at z, turned `angle` about its middle."""
    return P((x0, y, z), (x1, y + height, z + width), mat, rot=("y", angle, ((x0 + x1) / 2, y, z + width / 2)),
             skip=("down",))


def bacon_and_eggs():
    """Two strips of bacon across the back, each in three pieces that rise and dip (a raised piece arches clear of the
    plate), and two fried eggs in front: round whites (two crossed thin boxes) and raised yolks."""
    out = []
    for z0, start, xs in ((3, 0, (4, 6, 9, 12)), (5, 1, (3, 6, 9, 13))):
        for i in range(3):
            high = (i + start) % 2 == 1
            y0 = 1.8 if high else 1.5
            out.append(P((xs[i], y0, z0), (xs[i + 1], y0 + 0.5, z0 + 2), "bacon_high" if high else "bacon", down=high))
    for x0, (yx, yz) in ((3, (5, 9)), (8, (9, 8))):
        out += [P((x0, 1.5, 8), (x0 + 5, 1.9, 11), "white"), P((x0 + 1, 1.5, 7), (x0 + 4, 1.9, 12), "white"),
                P((yx, 1.9, yz), (yx + 2, 2.5, yz + 2), "yolk")]
    return out


def steak_and_potatoes():
    """A thick steak at the back with its fat cap, crowned with grill marks and cut across its front (the cut shows
    pink), a slice of it lying in front; a mound of mashed potato with a pat of butter, two golden roast potatoes and a
    sprig of parsley (the sides of the owner's icon)."""
    return [
        P((5, 1.5, 4), (12, 2.6, 7), "steak", faces_mat={"south": "steak_cut"}),
        P((6, 1.5, 3), (11, 2.6, 8), "steak", faces_mat={"south": "steak_cut"}),
        P((6, 2.6, 4), (11, 2.9, 7), "steak_crown"),
        P((7, 1.5, 8), (11, 1.9, 10), "slice"),
        P((3, 1.5, 9), (7, 2.3, 11), "mash"), P((4, 1.5, 8), (6, 2.3, 12), "mash"), P((4, 2.3, 9), (6, 2.8, 11), "mash"),
        P((4, 2.8, 9), (5, 3.1, 10), "butter"),
        P((7, 1.5, 10), (9, 2.6, 12), "potato"), P((9, 1.5, 10), (11, 2.4, 12), "potato"),
        P((11, 1.5, 9), (12, 1.8, 11), "parsley"), P((12, 1.5, 8), (13, 1.9, 10), "parsley"),
    ]


def chop(x, z, h, bone_x):
    """A mutton chop: the round eye of meat (two crossed boxes), its tail curling forward, and its bone running back
    and up off the plate, knuckle at the end."""
    origin = (bone_x + 0.5, 1.9, z + 1)
    tilt = ("x", 22.5, origin)
    return [P((x, 1.5, z + 0.5), (x + 3, 1.5 + h, z + 2.5), "chop"), P((x + 0.5, 1.5, z), (x + 2.5, 1.5 + h, z + 3), "chop"),
            P((x + 1, 1.5, z + 3), (x + 3, 1.5 + h * 0.6, z + 4), "chop_tail"),
            P((bone_x, 1.6, z - 2.5), (bone_x + 1, 2.4, z + 1), "bone", rot=tilt),
            P((bone_x - 0.25, 1.5, z - 3.5), (bone_x + 1.25, 2.5, z - 2.5), "knuckle", rot=tilt)]


def roasted_mutton_chops():
    """Three roast chops side by side, the middle one standing proud, their bones rising over the back of the plate;
    roast potatoes, red onion and a sprig of rosemary in front."""
    return (chop(3, 6, 1.1, 4) + chop(6, 5, 1.4, 7) + chop(9, 6, 1.1, 10) + [
        P((3, 1.5, 10), (5, 2.6, 12), "roast_potato"), P((5, 1.5, 10), (7, 2.4, 12), "roast_potato"),
        P((7.5, 1.5, 10), (9, 2.5, 12), "roast_potato"),
        P((10, 1.5, 10), (12, 2.0, 11), "onion"), P((9, 1.5, 11), (11, 2.2, 12), "onion"),
        P((4, 1.5, 9), (10, 1.75, 10), "rosemary", down=False),
    ])


def nest(steps, mat):
    """A nest of pasta: stacked rounds, each `inset` in from the one below and `height` high (a stack of crossed
    boxes)."""
    out = []
    for inset, top in steps:
        lo, hi = 3 + inset, 13 - inset
        if inset == 0:
            out += [P((3, 1.5, 5), (13, top, 11), mat), P((4, 1.5, 4), (12, top, 12), mat)]
        else:
            out.append(P((lo, 1.5, lo), (hi, top, hi), mat))
    return out


def pasta_with_meatballs():
    """A mound of spaghetti with loose strands draped over it, sauce pooled on its top and three meatballs nestled in
    it; a leaf of basil."""
    return (nest([(0, 2.2), (2, 2.9)], "spaghetti") + [
        P((6, 2.9, 6), (10, 3.3, 10), "spaghetti"),
        strand(3.5, 12.5, 6.7, 2.2, 22.5, "strand"), strand(3.5, 12.5, 9.3, 2.2, -22.5, "strand"),
        strand(4, 9, 11.4, 1.5, 22.5, "strand"),
        P((6, 3.3, 7), (10, 3.5, 9), "sauce"), P((7, 3.3, 6), (9, 3.5, 10), "sauce"),
    ] + meatball(4.5, 4.5, 2.4) + meatball(9, 5, 2.4) + meatball(6.5, 9, 2.3) + [
        P((7, 3.5, 7), (8, 3.7, 8), "basil"), P((8, 3.5, 8), (9, 3.7, 9), "basil"),
    ])


def squid_ink_pasta():
    """Squid-ink spaghetti twirled into a tall nest (each round turned further than the one below), loose strands on the
    plate; cherry tomato halves, curls of squid and parsley on top."""
    twirl = []
    for half, y0, top, angle in ((3, 2.0, 2.9, 22.5), (2.25, 2.7, 3.5, 45), (1.5, 3.3, 4.0, -22.5)):
        twirl.append(P((8 - half, y0, 8 - half), (8 + half, top, 8 + half), "ink", rot=("y", angle, (8, y0, 8)),
                       top="pos", skip=("down",)))
    return ([P((4, 1.5, 5), (12, 2.2, 11), "ink"), P((5, 1.5, 4), (11, 2.2, 12), "ink")] + twirl + [
        strand(3.2, 7, 11.8, 1.5, -22.5, "ink_strand"), strand(9.5, 12.8, 3.5, 1.5, 22.5, "ink_strand"),
        P((3, 1.5, 6), (3.6, 1.75, 9), "ink_strand"),
        strand(4.4, 11.6, 7.7, 2.2, 45, "ink_strand", height=0.35), strand(5.2, 10.8, 8.3, 2.9, -45, "ink_strand", height=0.35),
        P((4, 2.2, 8), (6, 2.9, 10), "tomato"), P((10, 2.2, 5), (12, 2.9, 7), "tomato"),
        P((9, 2.9, 9), (11, 3.3, 10), "squid"), P((5, 2.9, 6), (6, 3.3, 8), "squid"), P((10, 2.2, 10), (12, 2.6, 11), "squid"),
        P((7, 4.0, 7), (8, 4.2, 8), "parsley"), P((8, 4.0, 8), (9, 4.2, 9), "parsley"),
    ])


def vegetable_noodles():
    """A wooden bowl filled with noodles, heaped in the middle with strands draped over the heap; carrot batons, strips of
    red pepper, cabbage, onion and rings of green onion on top."""
    return [
        P((4, 3.4, 4), (12, 3.8, 12), "noodles"), P((5, 3.8, 5), (11, 4.2, 11), "noodles"),
        strand(3.5, 12.5, 7, 3.8, 22.5, "noodle_strand", height=0.4), strand(4, 12, 9.5, 3.8, -22.5, "noodle_strand", height=0.4),
        strand(4.5, 11.5, 5.2, 3.8, 45, "noodle_strand", height=0.5), strand(4.5, 11.5, 10.6, 3.8, -45, "noodle_strand", height=0.5),
        strand(5, 9, 5.5, 4.2, -22.5, "carrot", width=0.8, height=0.6), strand(8, 12, 9, 4.2, 45, "carrot", width=0.8, height=0.6),
        strand(4, 7.5, 9.6, 3.8, 22.5, "carrot", width=0.8, height=0.6),
        strand(6, 10, 7.6, 4.2, 45, "pepper", width=0.8, height=0.5), strand(9, 12, 5.3, 4.0, -45, "pepper", width=0.8, height=0.5),
        P((7, 4.2, 9), (9, 4.5, 11), "cabbage"), P((8, 3.8, 10), (10, 4.3, 12), "cabbage"),
        P((5, 4.2, 7), (6, 4.5, 8), "onion"), P((10, 4.2, 10), (11, 4.5, 11), "scallion"), P((6, 4.2, 10), (7, 4.5, 11), "scallion"),
        P((9, 4.2, 6), (10, 4.5, 7), "scallion"),
    ]


def tomato_sauce():
    """A wooden bowl of tomato sauce, two leaves of basil on it and a wooden spoon standing in it, its handle leaning on
    the rim (tilted 22.5 degrees from under the sauce so its underside meets the rim's inner edge, y 5 at z 4)."""
    lean = ("x", 22.5, (8.5, 1.69, 12))
    return [
        P((5, 4.3, 5), (7, 4.5, 6), "basil"), P((6, 4.3, 6), (8, 4.5, 7), "basil"),
        P((9, 4.3, 10), (10, 4.5, 12), "basil"), P((10, 4.3, 9), (11, 4.5, 11), "basil"),
        P((8, 1.69, 2.5), (9, 2.19, 10.5), "spoon", rot=lean),
    ]


def cornbread_stuffing():
    """Cornbread stuffing baked in the skillet: a bed of crumbs with toasted cornbread cubes of every size heaped on it,
    some turned askew, cranberries, sage leaves and a slice of mushroom."""
    def askew(x, z, size, h, angle):
        c = (x + size / 2, 2.6, z + size / 2)
        return P((x, 2.6, z), (x + size, 2.6 + h, z + size), "crouton", rot=("y", angle, c), skip=("down",))
    return [
        P((4, 2.6, 5), (6, 3.8, 7), "cube"), P((7, 2.6, 5), (8, 3.4, 6), "cube"), P((9, 2.6, 6), (11, 3.9, 8), "cube"),
        P((5, 2.6, 8), (7, 3.6, 10), "cube"), P((8, 2.6, 9), (10, 4.0, 11), "cube"), P((10, 2.6, 11), (12, 3.4, 12), "cube"),
        P((4, 2.6, 11), (6, 3.5, 13), "cube"), P((7, 2.6, 12), (8, 3.2, 13), "cube"),
        askew(6.5, 6.5, 1.5, 1.1, 45), askew(10.5, 8.5, 1.5, 0.9, 22.5), askew(6.5, 10.5, 1.5, 0.9, -22.5),
        P((11, 2.6, 6), (12, 3.3, 7), "berry"), P((4, 2.6, 8), (5, 3.2, 9), "berry"), P((9, 2.6, 12), (10, 3.2, 13), "berry"),
        P((4, 2.6, 9), (5, 3.0, 11), "mushroom"),
        P((8, 4.0, 9), (9, 4.2, 10), "sage"), P((9, 4.0, 10), (10, 4.2, 11), "sage"), P((9, 3.9, 6), (11, 4.05, 7), "sage"),
    ]


MEALS = {
    "cornbread_stuffing": {"dish": "skillet", "food": cornbread_stuffing, "bed": ((4, 1, 5), (12, 2.6, 13), "crumb")},
    "vegetable_noodles": {"dish": "bowl", "food": vegetable_noodles, "bowl": (2, 14, 4.0, 3.4, "noodles")},
    "squid_ink_pasta": {"dish": "glazed_plate", "food": squid_ink_pasta},
    "bacon_and_eggs": {"dish": "wood_plate", "food": bacon_and_eggs},
    "steak_and_potatoes": {"dish": "wood_plate", "food": steak_and_potatoes},
    "roasted_mutton_chops": {"dish": "wood_plate", "food": roasted_mutton_chops},
    "pasta_with_meatballs": {"dish": "glazed_plate", "food": pasta_with_meatballs},
    "tomato_sauce": {"dish": "bowl", "food": tomato_sauce, "bowl": (3, 13, 5.0, 4.3, "sauce")},
}


# ---------------------------------------------------------------- writing

def side_rows(name):
    """The first row of each material in <name>_side (tools/menu_meal_textures.py draws them in this order)."""
    return tex.side_rows(name)


def food_elements(name, pieces):
    """The food's elements: upward faces read <name>_top by position, the rest their material's rows in <name>_side
    (a piece's `faces_mat` may give a side another material: a steak's cut face)."""
    rows = side_rows(name)
    swatches = tex.swatches(name)
    side_rows_of = {mat: len(tex.MEALS[name]["materials"][mat]["side"]) for mat in rows}
    out = []
    for p in pieces:
        (x0, y0, z0), (x1, y1, z1) = p["lo"], p["hi"]
        row, height = rows[p["mat"]], y1 - y0
        e = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": {}}
        turned = "rot" in p
        for face in FACES:
            if face in p.get("skip", ()):
                continue
            if face == "down" and not (p.get("down") or turned):
                continue
            if face == "up" and (not turned or p.get("top") == "pos"):
                e["faces"]["up"] = {"texture": "#food", "uv": [x0, z0, x1, z1]}
            elif face == "up" and swatches.get(p["mat"]):
                (u, v), (w, d) = swatches[p["mat"]]
                e["faces"]["up"] = {"texture": "#food_side", "uv": [u, v, u + min(w, x1 - x0), v + min(d, z1 - z0)]}
            elif face in ("up", "down"):
                e["faces"][face] = band("#food_side", row + (side_rows_of[p["mat"]] - 1 if face == "down" else 0),
                                        p["lo"], p["hi"], face)
            else:
                mat = p.get("faces_mat", {}).get(face, p["mat"])
                e["faces"][face] = band("#food_side", rows[mat], p["lo"], p["hi"], face)
                e["faces"][face]["uv"][3] = rows[mat] + min(height, side_rows_of[mat])
        if turned:
            axis, angle, origin = p["rot"]
            e["rotation"] = {"angle": angle, "axis": axis, "origin": list(origin)}
        out.append(e)
    return out


def pieces(name):
    """The food's pieces (with a bowl's contents), for the meal's model and its top texture."""
    meal = MEALS[name]
    food = meal["food"]()
    if meal["dish"].startswith("bowl"):
        _, contents = dish_elements(name)
        food = contents + food
    if "bed" in meal:
        food = [P(*meal["bed"])] + food
    return food


def dish_elements(name):
    meal = MEALS[name]
    if meal["dish"] in ("wood_plate", "glazed_plate"):
        return plate(), None
    if meal["dish"] == "skillet":
        return skillet(), None
    a, b, wall_top, fill_top, contents = meal["bowl"]
    return bowl(a, b, wall_top, fill_top, contents=contents)


DISH_TEXTURES = {
    "wood_plate": {"plate": "meal_wood_plate_top", "plate_side": "meal_wood_side"},
    "glazed_plate": {"plate": "meal_glazed_plate_top", "plate_side": "meal_glazed_side"},
    "bowl": {"bowl_side": "meal_wood_side"},
    "skillet": {"pan_side": "skillet_side", "pan_bottom": "skillet_bottom"},
}


_OUT = {"north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1), "down": (1, -1), "up": (1, 1)}


def _buried(face, f, t, others):
    """Whether a box face lies wholly inside the `others` boxes: a point just outside it, on an 8 x 8 grid over it, is
    always strictly inside one of them."""
    axis, sign = _OUT[face]
    plane = t[axis] if sign > 0 else f[axis]
    a, b = [k for k in range(3) if k != axis]
    for u in range(8):
        for v in range(8):
            p = [0.0, 0.0, 0.0]
            p[axis] = plane + sign * 0.01
            p[a] = f[a] + (u + 0.5) / 8 * (t[a] - f[a])
            p[b] = f[b] + (v + 0.5) / 8 * (t[b] - f[b])
            if not any(all(F[k] + 1e-6 < p[k] < T[k] - 1e-6 for k in range(3)) for F, T in others):
                return False
    return True


def _filled(f, t, others):
    """Whether the inside of a box (a 4 x 4 x 4 grid of points) lies inside the `others` boxes."""
    for i in range(4):
        for j in range(4):
            for k in range(4):
                p = [f[n] + ((i, j, k)[n] + 0.5) / 4 * (t[n] - f[n]) for n in range(3)]
                if not any(all(F[n] + 1e-6 < p[n] < T[n] - 1e-6 for n in range(3)) for F, T in others):
                    return False
    return True


def drop_buried(elements):
    """Leaves out what nothing can see, so the model keeps no hidden waste: first every unrotated box lying wholly
    inside other boxes (its six sides and its inside, one box at a time, judged against the boxes still kept), then
    every face of a kept box that lies wholly inside other kept boxes. No hole opens: whatever buries a face is itself
    kept and closed. Returns (boxes removed, faces dropped)."""
    def boxes(skip=None):
        return [(e["from"], e["to"]) for j, e in enumerate(elements) if not e.get("rotation") and j != skip]
    removed = 0
    changed = True
    while changed:
        changed = False
        for i, e in enumerate(elements):
            if not e.get("rotation") and _filled(e["from"], e["to"], boxes(i)) \
                    and all(_buried(face, e["from"], e["to"], boxes(i)) for face in _OUT):
                del elements[i]
                removed += 1
                changed = True
                break
    dropped = 0
    for i, e in enumerate(elements):
        if e.get("rotation"):
            continue
        for face in list(e["faces"]):
            if _buried(face, e["from"], e["to"], boxes(i)):
                del e["faces"][face]
                dropped += 1
    assert all(e["faces"] for e in elements), "an element was left with no faces"
    return removed, dropped


def textures_and_elements(name):
    """(texture keys -> texture names under block/, the finished, closed elements) of a remodelled meal."""
    meal = MEALS[name]
    dish, _ = dish_elements(name)
    elements = dish + food_elements(name, pieces(name))
    model_writer.finish_closed(elements)
    drop_buried(elements)
    model_writer.finish_elements(elements)
    kind = "bowl" if meal["dish"].startswith("bowl") else meal["dish"]
    return {"food": f"{name}_top", "food_side": f"{name}_side", **DISH_TEXTURES[kind]}, elements
