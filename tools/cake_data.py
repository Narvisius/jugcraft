"""JSON resources for the cakes (tools/cakes.py): each cake's models (whole, then with one, two and three quarters cut
away), blockstates (turned to face whoever set the cake down), items (the cakes as their blocks; the raw cakes, slices
and Cake Batter flat), names, loot (a cake only while whole) and tags. The textures are tools/cake_art.py's; the recipes
are in tools/cakes.py SHAPELESS.

A model faces north, as Jugcraft's turned models do: the drawing (cakes.turn) turned half round, so the quarter the
owner's INTERIOR drawing cuts away first is the model's north-west, the front right as seen from its front. Each quarter
is a box of its own; a face between two quarters is drawn only once the quarter beyond it is gone, with the cake's inside.
The toppings stand on the quarters they lie on (the cheesecake's jam is cut with them).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files.
"""
import cakes
from decor_data import MOD, rid, block_model, self_drop, flat_item, FACING_Y

# The model's quarters, in the order slices are taken (CakeBlock.QUARTERS, facing north): the front right (north-west),
# the front left, the back left, the back right.
QUARTERS = [((0, 0), (8, 8)), ((8, 0), (16, 8)), ((8, 8), (16, 16)), ((0, 8), (8, 16))]


def quarter_of(lo, hi):
    """The quarter (index in QUARTERS) a box (model coordinates) stands on, by its middle."""
    x, z = (lo[0] + hi[0]) / 2, (lo[2] + hi[2]) / 2
    for i, ((x0, z0), (x1, z1)) in enumerate(QUARTERS):
        if x0 <= x < x1 and z0 <= z < z1:
            return i
    raise ValueError(f"a topping at {lo}-{hi} is off the cake")


def gone(bites):
    return set(range(bites))


def body(bites, name):
    """The cake's quarters left after `bites` slices: their tops, outer sides (the front texture north and south, the side
    east and west), undersides, and the inside where a quarter beside them is gone."""
    h = cakes.HEIGHT
    elements = []
    for q in range(bites, len(QUARTERS)):
        (x0, z0), (x1, z1) = QUARTERS[q]
        faces = {"up": {"texture": "#top"}, "down": {"texture": "#inside", "uv": [x0, 15, x1, 16], "cullface": "down"}}
        neighbours = {"west": (x0 - 4, z0 + 4), "east": (x1 + 4, z0 + 4), "north": (x0 + 4, z0 - 4), "south": (x0 + 4, z1 + 4)}
        for side, (nx, nz) in neighbours.items():
            if not (0 <= nx < 16 and 0 <= nz < 16):
                faces[side] = {"texture": "#front" if side in ("north", "south") else "#side", "cullface": side}
            elif quarter_of((nx, 0, nz), (nx, 0, nz)) in gone(bites):
                faces[side] = {"texture": "#inside"}
        elements.append({"from": [x0, 0, z0], "to": [x1, h, z1], "faces": faces})
    return elements


def bar_faces(axis, length, width, leaves):
    """A bar's faces on its toppings texture (tools/cake_art.py toppings): lying along z, its top is the region u 0 to
    `width`, v 0 to `length` (leaves at v 0); along x, u 8 to 8 + `length`, v 0 to `width` (leaves at u 8); its long sides
    the row u 3 to 3 + `length` (leaves at u 3, one row stretched up the side); its ends u 3-5 (the tip) and 5-7 (the
    leaves) on row 2. A side's uv runs left to right as the side is seen from outside."""
    lo_first = leaves == "lo"
    forward, backward = [3, 0, 3 + length, 1], [3 + length, 0, 3, 1]
    tip, leaf = [3, 2, 3 + width, 3], [5, 2, 5 + width, 3]
    if axis == "z":
        return {"up": [0, 0, width, length] if lo_first else [0, length, width, 0],
                "west": forward if lo_first else backward, "east": backward if lo_first else forward,
                "north": leaf if lo_first else tip, "south": tip if lo_first else leaf}
    return {"up": [8, 0, 8 + length, width] if lo_first else [8 + length, 0, 8, width],
            "south": forward if lo_first else backward, "north": backward if lo_first else forward,
            "west": leaf if lo_first else tip, "east": tip if lo_first else leaf}


def toppings(cake, bites):
    """The toppings on the quarters left, in model coordinates (cakes.turn)."""
    elements = []
    if cake in cakes.BARS:
        spec = cakes.BARS[cake]
        for bar in spec["bars"]:
            lo, hi = cakes.turn(*cakes.bar_box(cake, bar))
            if quarter_of(lo, hi) < bites:
                continue
            # Turned half round, a bar's leaves change ends.
            leaves = {"lo": "hi", "hi": "lo"}[bar[3]] if len(bar) > 3 else "lo"
            uvs = bar_faces(bar[0], spec["length"], spec["width"], leaves)
            if not spec["leaves"]:
                uvs["north" if bar[0] == "z" else "west"] = uvs["south" if bar[0] == "z" else "east"]
            elements.append({"from": lo, "to": hi, "faces": {side: {"texture": "#toppings", "uv": uv} for side, uv in uvs.items()}})
    for candle in cakes.CANDLES.get(cake, []):
        lo, hi = cakes.turn(*cakes.candle_box(candle))
        if quarter_of(lo, hi) < bites:
            continue
        u = cakes.CANDLE_ORDER.index(candle[2])
        faces = {side: {"texture": "#toppings", "uv": [u, 0, u + 1, cakes.CANDLE_HEIGHT]} for side in ("north", "south", "east", "west")}
        faces["up"] = {"texture": "#toppings", "uv": [u, cakes.CANDLE_HEIGHT, u + 1, cakes.CANDLE_HEIGHT + 1]}
        elements.append({"from": lo, "to": hi, "faces": faces})
    if cake in cakes.JAM:
        lo, hi = cakes.turn(*cakes.jam_box(cake))
        jx, jz = lo[0], lo[2]
        for q in range(bites, len(QUARTERS)):
            (x0, z0), (x1, z1) = QUARTERS[q]
            a, b = [max(lo[0], x0), lo[1], max(lo[2], z0)], [min(hi[0], x1), hi[1], min(hi[2], z1)]
            if a[0] >= b[0] or a[2] >= b[2]:
                continue
            faces = {"up": {"texture": "#toppings", "uv": [a[0] - jx, a[2] - jz, b[0] - jx, b[2] - jz]}}
            # Its sides on the jam's side row: the outer ones always, the cut ones once the quarter beside is gone.
            for side, edge, outer in (("north", a[2], lo[2]), ("south", b[2], hi[2]), ("west", a[0], lo[0]), ("east", b[0], hi[0])):
                if edge == outer or quarter_of(*neighbour_point(side, a, b)) in gone(bites):
                    span = (a[0] - jx, b[0] - jx) if side in ("north", "south") else (a[2] - jz, b[2] - jz)
                    faces[side] = {"texture": "#toppings", "uv": [span[0], 4, span[1], 5]}
            elements.append({"from": a, "to": b, "faces": faces})
    return elements


def neighbour_point(side, a, b):
    """A point just beyond a box's side, as (lo, hi) for quarter_of."""
    x, z = (a[0] + b[0]) / 2, (a[2] + b[2]) / 2
    x, z = {"north": (x, a[2] - 0.5), "south": (x, b[2] + 0.5), "west": (a[0] - 0.5, z), "east": (b[0] + 0.5, z)}[side]
    return (x, 0, z), (x, 0, z)


def model(cake, bites):
    textures = {"top": f"{cake}_top", "front": f"{cake}_front", "side": f"{cake}_side", "inside": f"{cake}_inside"}
    if cake in cakes.BARS or cake in cakes.CANDLES or cake in cakes.JAM:
        textures["toppings"] = f"{cake}_toppings"
    return block_model(textures, body(bites, cake) + toppings(cake, bites), f"{cake}_front")


def model_name(cake, bites):
    return cake if bites == 0 else f"{cake}_slice{bites}"


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    for cake in cakes.blocks():
        for bites in range(len(QUARTERS)):
            write(models / f"{model_name(cake, bites)}.json", model(cake, bites))
        write(states / f"{cake}.json", {"variants": {
            f"bites={b},facing={f}": {"model": rid(f"block/{model_name(cake, b)}"), **({"y": FACING_Y[f]} if FACING_Y[f] else {})}
            for b in range(len(QUARTERS)) for f in ("north", "east", "south", "west")}})
        write(root / "items" / f"{cake}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{cake}")}})
    for cake, info in cakes.CAKES.items():
        lang[f"block.{MOD}.{cake}"] = info["display"]
        flat_item(root, write, cakes.raw(cake))
        lang[f"item.{MOD}.{cakes.raw(cake)}"] = f"Raw {info['display']}"
        flat_item(root, write, cakes.slice_item(cake))
        lang[f"item.{MOD}.{cakes.slice_item(cake)}"] = f"Slice of {info['display']}"
    lang[f"block.{MOD}.{cakes.BURNT}"] = "Burnt Cake"


def loot(out, write):
    for cake in cakes.blocks():
        write(out / f"{cake}.json", self_drop(cake, {"type": "minecraft:match_block", "blocks": rid(cake), "state": {"bites": "0"}}))


def tags(tags):
    for cake in cakes.CAKES:
        tags.add("item", "c:foods", rid(cakes.slice_item(cake)))
        tags.add("item", "c:foods/cake", rid(cakes.slice_item(cake)))
