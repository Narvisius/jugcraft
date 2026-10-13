"""JSON resources for the owner's pies and tarts (tools/pies_and_tarts.py): each bake's models (whole, then with one, two
and three quarters cut away), blockstates (turned to face whoever set it down), items (the bakes as their blocks; the raw
bakes and slices flat), names, loot (a bake only while whole) and tags. The textures are tools/pie_tart_art.py's; the
recipes are in tools/pies_and_tarts.py SHAPELESS.

The model is made in the drawing's own coordinates (x east, z south, seen from the south-east) and turned half round
(cakes.turn), so a model faces north, as Jugcraft's turned models do, and the quarter the page cuts away first is the
model's front right. Each quarter is boxes of its own, so a cut shows the bake's inside:
- a **pie**: its quarter of the lid (the full width, from PIE["lid"] up) over its quarter of the body (inset a texel at
  the bake's outer sides);
- a **tart**: its quarter of the base (inset), of the wall up to the filling, and of the rim a texel wide along the
  bake's outer sides, standing a texel above the filling.
A face between two quarters is drawn only once the quarter beyond it is gone. The toppings stand on the quarters they lie
on; one across a cut is cut with them.

Every face reads its texture as drawn, the way the art module lays the textures out (LAYOUT): the top as the drawing's
top (turned with the model); the outer sides as the drawing's south face (`front`, on the north and south) and east face
(`side`, on the east and west), each texel row of the bake at its row of those textures; the cuts as the drawing's cut
(`inside`).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files.
"""
import pies_and_tarts as pt
from decor_data import MOD, rid, block_model, self_drop, flat_item, FACING_Y

# The quarters, in the drawing's coordinates, in the order slices are taken: the south-east (the model's front right),
# the south-west, the north-west and the north-east (CakeBlock.QUARTERS turned half round).
QUARTERS = [((8, 8), (16, 16)), ((0, 8), (8, 16)), ((0, 0), (8, 8)), ((8, 0), (16, 8))]

# Where each part's texel rows are in the front, side and inside textures: (top row, bottom row), the bake's top first.
# A part a half texel tall shows half of its last (or first) row.
LAYOUT = {
    "pie": {"lid": (0, 2.5), "body": (3.5, 8)},
    "tart": {"wall": (0, 2.5), "base": (3.5, 5), "rim_inner": (6, 7)},
}
# A tart's wall: the rim (its first row) and the wall below it up to the filling.
TART_RIM_ROWS = (0, 1)
TART_BAND_ROWS = (1, 2.5)
# The toppings' texture: a topping's top at u 0 to its width, v 0 to its depth; its south face below it, its east face
# beside that (each its height tall).


def quarter_of(x, z):
    """The quarter (index in QUARTERS) a point in the drawing's coordinates is in."""
    for i, ((x0, z0), (x1, z1)) in enumerate(QUARTERS):
        if x0 <= x < x1 and z0 <= z < z1:
            return i
    raise ValueError(f"({x}, {z}) is off the bake")


def gone(bites):
    return set(range(bites))


def side_uv(side, lo, hi, rows):
    """A side face's uv: across the face as seen from outside (as Minecraft's default uv runs), and down `rows`."""
    u0, u1 = {"south": (lo[0], hi[0]), "north": (16 - hi[0], 16 - lo[0]), "east": (16 - hi[2], 16 - lo[2]),
              "west": (lo[2], hi[2])}[side]
    return [u0, rows[0], u1, rows[1]]


def neighbour(side, lo, hi):
    """The quarter just beyond a box's side, or None beyond the bake's edge."""
    x, z = (lo[0] + hi[0]) / 2, (lo[2] + hi[2]) / 2
    x, z = {"north": (x, lo[2] - 0.5), "south": (x, hi[2] + 0.5), "west": (lo[0] - 0.5, z), "east": (hi[0] + 0.5, z)}[side]
    return quarter_of(x, z) if 0 <= x < 16 and 0 <= z < 16 else None


def texture_for(side):
    return "#front" if side in ("north", "south") else "#side"


def part(lo, hi, rows, bites, bounds=(0, 16), up=True, down=False, cut_rows=None, inner=None):
    """A box of a quarter. Its outer sides (at `bounds`: the bake's edge, or the inset body's) take the front or side
    texture at `rows`; its sides on a cut (beyond them a quarter that is gone) the inside texture at `cut_rows` (or
    `rows`); its top the top texture; its underside, with `down`, the inside's last row of `rows`. `inner`: the sides that
    face into the bake (a tart's rim over its filling), with the rows of the rim's inner wall."""
    faces = {}
    if up:
        faces["up"] = {"texture": "#top"}
    if down:
        faces["down"] = {"texture": "#inside", "uv": [lo[0], rows[1] - 1, hi[0], rows[1]]}
        if lo[1] == 0:
            faces["down"]["cullface"] = "down"
    for side in ("north", "south", "east", "west"):
        if inner and side in inner:
            faces[side] = {"texture": texture_for(side), "uv": side_uv(side, lo, hi, inner[side])}
            continue
        at = {"north": lo[2], "south": hi[2], "west": lo[0], "east": hi[0]}[side]
        if at == (bounds[0] if side in ("north", "west") else bounds[1]):
            faces[side] = {"texture": texture_for(side), "uv": side_uv(side, lo, hi, rows)}
            if at in (0, 16):
                faces[side]["cullface"] = side
        elif at == 8 and neighbour(side, lo, hi) in gone(bites):
            faces[side] = {"texture": "#inside", "uv": side_uv(side, lo, hi, cut_rows or rows)}
    return {"from": list(lo), "to": list(hi), "faces": faces}


def pie_quarter(q, bites):
    (x0, z0), (x1, z1) = QUARTERS[q]
    h, lid = pt.PIE["height"], pt.PIE["lid"]
    i = pt.INSET
    rows = LAYOUT["pie"]
    body = ([max(x0, i), 0, max(z0, i)], [min(x1, 16 - i), lid, min(z1, 16 - i)])
    return [part([x0, lid, z0], [x1, h, z1], rows["lid"], bites, down=True),
            part(body[0], body[1], rows["body"], bites, bounds=(i, 16 - i), up=False, down=True)]


def tart_quarter(q, bites):
    (x0, z0), (x1, z1) = QUARTERS[q]
    h, base, fill = pt.TART["height"], pt.TART["base"], pt.TART["filling"]
    i = pt.INSET
    rows = LAYOUT["tart"]
    elements = [part([max(x0, i), 0, max(z0, i)], [min(x1, 16 - i), base, min(z1, 16 - i)], rows["base"], bites, bounds=(i, 16 - i),
                     up=False, down=True),
                part([x0, base, z0], [x1, fill, z1], TART_BAND_ROWS, bites, down=True)]
    # The rim: along each outer side of the quarter, a texel wide; the north and south strips run the quarter's width,
    # the west and east ones between them.
    strips = []
    if z0 == 0:
        strips.append(([x0, fill, 0], [x1, h, 1], "south"))
    if z1 == 16:
        strips.append(([x0, fill, 15], [x1, h, 16], "north"))
    za, zb = max(z0, 1), min(z1, 15)
    if x0 == 0:
        strips.append(([0, fill, za], [1, h, zb], "east"))
    if x1 == 16:
        strips.append(([15, fill, za], [16, h, zb], "west"))
    for lo, hi, facing_in in strips:
        elements.append(part(lo, hi, TART_RIM_ROWS, bites, inner={facing_in: rows["rim_inner"]}, cut_rows=TART_RIM_ROWS))
    return elements


def topping_parts(bake, bites):
    """The toppings on the quarters left, each cut where it crosses into another quarter."""
    spec = pt.BAKES[bake].get("toppings")
    if not spec:
        return []
    w, d, h = spec["size"]
    out = []
    for lo, hi in pt.topping_boxes(bake):
        for q in range(bites, len(QUARTERS)):
            (qx0, qz0), (qx1, qz1) = QUARTERS[q]
            a = [max(lo[0], qx0), lo[1], max(lo[2], qz0)]
            b = [min(hi[0], qx1), hi[1], min(hi[2], qz1)]
            if a[0] >= b[0] or a[2] >= b[2]:
                continue
            # Its place on the topping's own texture.
            ua, ub = a[0] - lo[0], b[0] - lo[0]
            va, vb = a[2] - lo[2], b[2] - lo[2]
            faces = {"up": {"texture": "#toppings", "uv": [ua, va, ub, vb]}}
            south = [ua, d, ub, d + h]
            east = [w + (d - vb), d, w + (d - va), d + h]
            for side, edge, outer in (("south", b[2], hi[2]), ("north", a[2], lo[2]), ("east", b[0], hi[0]), ("west", a[0], lo[0])):
                if edge != outer:
                    beyond = neighbour(side, a, b)
                    if beyond not in gone(bites):
                        continue
                uv = south if side in ("south", "north") else east
                if side == "north":
                    uv = [w - uv[2], uv[1], w - uv[0], uv[3]]
                if side == "west":
                    uv = [2 * w + d - uv[2], uv[1], 2 * w + d - uv[0], uv[3]]
                faces[side] = {"texture": "#toppings", "uv": uv}
            out.append({"from": a, "to": b, "faces": faces})
    return out


def turned(element):
    """An element in the drawing's coordinates turned half round, as the model faces (cakes.turn): its sides change
    places and its top and underside turn with it."""
    lo, hi = pt.turn(element["from"], element["to"])
    rename = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "up", "down": "down"}
    faces = {}
    for side, face in element["faces"].items():
        face = dict(face)
        if side in ("up", "down") and "uv" in face:
            u0, v0, u1, v1 = face["uv"]
            face["uv"] = [u1, v1, u0, v0]
        if "cullface" in face:
            face["cullface"] = rename[face["cullface"]]
        faces[rename[side]] = face
    return {"from": lo, "to": hi, "faces": faces}


def model(bake, bites):
    quarter = pie_quarter if pt.BAKES[bake]["shape"] == "pie" else tart_quarter
    elements = [e for q in range(bites, len(QUARTERS)) for e in quarter(q, bites)] + topping_parts(bake, bites)
    textures = {"top": f"{bake}_top", "front": f"{bake}_front", "side": f"{bake}_side", "inside": f"{bake}_inside"}
    if pt.BAKES[bake].get("toppings"):
        textures["toppings"] = f"{bake}_toppings"
    return block_model(textures, [turned(e) for e in elements], f"{bake}_front")


def model_name(bake, bites):
    return bake if bites == 0 else f"{bake}_slice{bites}"


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    for bake, info in pt.BAKES.items():
        for bites in range(len(QUARTERS)):
            write(models / f"{model_name(bake, bites)}.json", model(bake, bites))
        write(states / f"{bake}.json", {"variants": {
            f"bites={b},facing={f}": {"model": rid(f"block/{model_name(bake, b)}"), **({"y": FACING_Y[f]} if FACING_Y[f] else {})}
            for b in range(len(QUARTERS)) for f in ("north", "east", "south", "west")}})
        write(root / "items" / f"{bake}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{bake}")}})
        lang[f"block.{MOD}.{bake}"] = info["display"]
        flat_item(root, write, pt.raw(bake))
        lang[f"item.{MOD}.{pt.raw(bake)}"] = f"Raw {info['display']}"
        flat_item(root, write, pt.slice_item(bake))
        lang[f"item.{MOD}.{pt.slice_item(bake)}"] = f"Slice of {info['display']}"


def loot(out, write):
    for bake in pt.BAKES:
        write(out / f"{bake}.json", self_drop(bake, {"type": "minecraft:match_block", "blocks": rid(bake), "state": {"bites": "0"}}))


def tags(tags):
    for bake in pt.BAKES:
        tags.add("item", "c:foods", rid(pt.slice_item(bake)))
        tags.add("item", "c:foods/pie", rid(pt.slice_item(bake)))
