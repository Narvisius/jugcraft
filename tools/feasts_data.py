"""JSON resources for feasts and food displays (tools/feasts.py), in the owner's own textures: the placed pies' models (a
pie in quarters, as tools/pie_data.py draws them, in the owner's crust) and blockstates; each feast's model at every
serving left (the roast on its platter carved down to the carcass, the shepherd's pie and the stuffed pumpkin eaten
down, the salad bowl emptied) and its leftovers; the displays' models; items, names, loot, recipes and tags.

The models are fitted to the owner's textures: each face's UV window is where that part is drawn in their texture.

Called from agriculture_data.py (assets, loot, tags); the recipes are in tools/agriculture.py SHAPED and SHAPELESS. Formats follow vanilla Minecraft 26.3's own files.
"""
import feasts
from feasts import FEASTS, DISPLAYS, PLACED_PIES
from decor_data import MOD, rid, box, block_model, turned, flat_item, self_drop
import pie_data


def unwrap(u, v, w, h, d):
    """The UVs of a box w (x) by h (y) by d (z) whose faces are laid out from (u, v) as a box unwrap: the top and bottom
    side by side above, then west, north, east and south in a row."""
    return {"up": [u + d, v, u + d + w, v + d], "down": [u + d + w, v, u + d + 2 * w, v + d],
            "west": [u, v + d, u + d, v + d + h], "north": [u + d, v + d, u + d + w, v + d + h],
            "east": [u + d + w, v + d, u + 2 * d + w, v + d + h], "south": [u + d, v + d, u + d + w, v + d + h]}


# ---------------------------------------------------------------- feasts

def platter(top):
    """The wooden platter a roast sits on, its top `top` (the platter's wood, a garnish ring, or the leftovers)."""
    return box((1, 0, 1), (15, 1, 15), "#platter", textures={"up": top}, uvs={"up": [1, 1, 15, 15]})


def roast_chicken(servings):
    """A roast chicken on its platter, a ring of roast vegetables round it. The drumsticks are eaten first, then the breast;
    the leftovers are the carcass on a platter of crumbs."""
    if servings == 0:
        return [platter("#leftovers"),
                box((5, 1, 6), (11, 3, 10), "#details", uvs={"up": [0, 0, 6, 4], "north": [0, 9, 6, 11], "south": [0, 9, 6, 11],
                                                             "east": [0, 11, 4, 13], "west": [0, 11, 4, 13]})]
    out = [platter("#platter"),
           # the roast vegetables, front and back
           box((2, 1, 2), (14, 2.5, 4), "#side_dish", uvs={"up": [0, 0, 12, 2], "north": [0, 12, 12, 14], "south": [0, 12, 12, 14],
                                                           "east": [10, 12, 11, 14], "west": [0, 12, 1, 14]}),
           box((2, 1, 12), (14, 2.5, 14), "#side_dish", uvs={"up": [0, 2, 10, 4], "north": [0, 14, 11, 16], "south": [0, 14, 11, 16],
                                                             "east": [10, 14, 11, 16], "west": [0, 14, 1, 16]})]
    if servings >= 2:
        # the breast: a body with a rounder back on top
        out += [box((3, 1, 4), (13, 6, 12), "#chicken", uvs={"up": [0, 0, 10, 8], "north": [3, 10, 13, 15], "south": [3, 10, 13, 15],
                                                              "east": [8, 10, 16, 15], "west": [0, 10, 8, 15]}),
                box((4, 6, 5), (12, 7, 11), "#chicken", uvs={"up": [2, 1, 10, 7], "north": [2, 9, 10, 10], "south": [2, 9, 10, 10],
                                                              "east": [9, 9, 15, 10], "west": [1, 9, 7, 10]})]
    else:
        # half the breast left
        out.append(box((3, 1, 8), (13, 5, 12), "#chicken", uvs={"up": [0, 4, 10, 8], "north": [3, 11, 13, 15], "south": [3, 11, 13, 15],
                                                                  "east": [8, 11, 12, 15], "west": [4, 11, 8, 15]}))
    meat = unwrap(8, 0, 4, 3, 2)
    bone = {"up": [2, 1, 4, 2], "north": [2, 1, 3, 2], "south": [2, 1, 3, 2], "east": [2, 1, 4, 2], "west": [2, 1, 4, 2]}
    for i, x in enumerate((1, 13)):
        if servings > 2 + i:  # the left drumstick goes at three servings, the right at two
            out += [box((x, 1, 6), (x + 2, 4, 10), "#details", uvs={**meat, "east": meat["north"], "west": meat["north"],
                                                                      "north": meat["west"], "south": meat["east"]}),
                    box((x + 0.5, 2, 4), (x + 1.5, 3, 6), "#details", uvs=bone)]
    return out


def honey_glazed_ham(servings):
    """A honey-glazed ham on a platter ringed with garnish, its cut face (pink) to the front and the bone out of the back.
    It is carved down slice by slice; the leftovers are the bone on a platter of crumbs."""
    if servings == 0:
        return [platter("#leftovers"),
                box((7, 1, 4), (9, 3, 11), "#details", uvs={"up": [7, 0, 9, 7], "north": [7, 0, 9, 2], "south": [7, 0, 9, 2],
                                                            "east": [8, 0, 15, 2], "west": [8, 0, 15, 2]})]
    depth = {4: 8, 3: 6, 2: 4, 1: 2}[servings]  # the ham is carved from the front
    z0 = 12 - depth
    out = [platter("#side_dish"),
           box((4, 1, z0), (12, 7, 12), "#ham", uvs={"up": [8, 0, 16, depth], "south": [8, 9, 16, 15], "north": [0, 1, 8, 7],
                                                      "east": [8, 1, 8 + depth, 7], "west": [8, 1, 8 + depth, 7]}),
           # the bone, out of the back
           box((7, 4, 12), (9, 6, 14.5), "#details", uvs={"up": [7, 0, 9, 2], "north": [7, 2, 9, 4], "south": [7, 0, 9, 2],
                                                           "east": [7, 2, 9, 4], "west": [7, 2, 9, 4]})]
    if servings >= 3:
        # a sprig of berries and leaves on top
        out.append(box((9, 7, 5 + 8 - depth), (12, 8, 8 + 8 - depth), "#details",
                       uvs={"up": [0, 11, 3, 14], "north": [0, 11, 3, 12], "south": [0, 13, 3, 14], "east": [1, 11, 4, 12], "west": [1, 11, 4, 12]}))
    return out


def shepherds_pie(servings):
    """A shepherd's pie in its dish on a board, served a quarter at a time (as pie_data.pie draws a pie); the leftovers
    are the board of crumbs."""
    if servings == 0:
        return [platter("#leftovers")]
    out = [platter("#platter")]
    for quarter in range(4 - servings, 4):
        lo, hi = pie_data.QUARTERS[quarter]
        faces = {}
        for side, inner in (("west", lo[0] == 8), ("east", hi[0] == 8), ("north", lo[2] == 8), ("south", hi[2] == 8)):
            if inner:
                faces[side] = "#inside"
        out.append(box((lo[0], 1, lo[2]), (hi[0], 8, hi[2]), "#side", textures={"up": "#top", "down": "#side", **faces},
                       uvs={side: [lo[2] if side in ("east", "west") else lo[0], 9, hi[2] if side in ("east", "west") else hi[0], 16]
                            for side in ("north", "south", "east", "west")}))
    return out


def stuffed_pumpkin(servings):
    """A stuffed pumpkin, heaped with stuffing and its stem on top. The heap goes down a serving at a time to the stuffing at
    the rim; the leftovers are the hollow pumpkin."""
    out = [box((2, 0, 2), (14, 8, 14), "#side", textures={"up": "#top" if servings == 0 else "#top_eaten", "down": "#bottom"},
               uvs={"up": [2, 2, 14, 14], "down": [2, 2, 14, 14], "north": [2, 8, 14, 16], "south": [2, 8, 14, 16],
                    "east": [2, 8, 14, 16], "west": [2, 8, 14, 16]})]
    heap = {4: 3, 3: 2, 2: 1}.get(servings, 0)
    if heap:
        out.append(box((4, 8, 4), (12, 8 + heap, 12), "#details", uvs={"up": [0, 0, 8, 8], "north": [0, 8 - heap, 8, 8],
                                                                         "south": [0, 8 - heap, 8, 8], "east": [0, 8 - heap, 8, 8],
                                                                         "west": [0, 8 - heap, 8, 8]}))
    if servings == 4:
        out += [box((7.5, 11, 7.5), (8.5, 14, 8.5), "#details", uvs={"up": [0, 8, 1, 9], "north": [0, 8, 1, 11], "south": [1, 8, 2, 11],
                                                                      "east": [0, 8, 1, 11], "west": [1, 8, 2, 11]}),
                box((5, 11, 8), (11, 11.01, 11), "#details", faces=("up", "down"), uvs={"up": [2, 13, 8, 16], "down": [2, 13, 8, 16]})]
    return out


def gleaming_salad(servings):
    """A wooden bowl of glow-berry salad, heaped with leaves and an orange slice of melon on top; the heap goes down a
    serving at a time; the leftovers are the bowl of crumbs."""
    out = [box((3, 0, 3), (13, 1, 13), "#bowl", uvs={side: [3, 14, 13, 15] for side in ("north", "south", "east", "west")}
               | {"up": [3, 3, 13, 13], "down": [3, 3, 13, 13]}),
           box((2, 1, 2), (14, 5, 14), "#bowl", textures={"up": "#leftovers" if servings == 0 else "#top"},
               uvs={side: [2, 10, 14, 14] for side in ("north", "south", "east", "west")} | {"up": [2, 2, 14, 14], "down": [2, 2, 14, 14]})]
    heap = {4: 3, 3: 2, 2: 1}.get(servings, 0)
    if heap:
        out.append(box((4, 5, 4), (12, 5 + heap, 12), "#details", uvs={"up": [0, 9, 8, 15], "north": [0, 15 - heap, 8, 15],
                                                                        "south": [1, 15 - heap, 9, 15], "east": [2, 15 - heap, 10, 15],
                                                                        "west": [0, 15 - heap, 8, 15]}))
    if servings == 4:
        out.append(box((7, 8, 6), (10, 9, 9), "#details", uvs={"up": [1, 1, 4, 4], "north": [0, 1, 3, 2], "south": [1, 3, 4, 4],
                                                                "east": [1, 1, 4, 2], "west": [1, 2, 4, 3]}))
    return out


def nachos(servings):
    """A tray of nachos in quarters (as pie_data.pie draws a pie), chips standing in each quarter left; the leftovers are
    the last bitten chip."""
    if servings == 0:
        return [box((4.5, 0, 8), (11.5, 7, 8), "#eaten", faces=("north", "south"), uvs={"north": [11, 9, 4, 16], "south": [4, 9, 11, 16]})]
    out = []
    for quarter in range(4 - servings, 4):
        lo, hi = pie_data.QUARTERS[quarter]
        faces = {}
        for side, inner in (("west", lo[0] == 8), ("east", hi[0] == 8), ("north", lo[2] == 8), ("south", hi[2] == 8)):
            if inner:
                faces[side] = "#inside"
        out.append(box((lo[0], 0, lo[2]), (hi[0], 2, hi[2]), "#side", textures={"up": "#top", "down": "#side", **faces}))
        mz, mx = (lo[2] + hi[2]) / 2, (lo[0] + hi[0]) / 2
        out.append(box((lo[0], 2, mz), (hi[0], 7, mz), "#chip", faces=("north", "south"),
                       uvs={"north": [hi[0], 11, lo[0], 16], "south": [lo[0], 11, hi[0], 16]}))
        out.append(box((mx, 2, lo[2]), (mx, 7, hi[2]), "#chip", faces=("east", "west"),
                       uvs={"east": [16 - hi[2], 11, 16 - lo[2], 16], "west": [lo[2], 11, hi[2], 16]}))
    return out


MODELS = {"nachos": nachos, "roast_chicken": roast_chicken, "honey_glazed_ham": honey_glazed_ham, "shepherds_pie": shepherds_pie,
          "stuffed_pumpkin": stuffed_pumpkin, "gleaming_salad": gleaming_salad}


def feast_model(name, servings):
    info = FEASTS[name]
    return block_model(info["textures"], MODELS[name](servings), info["particle"])


# ---------------------------------------------------------------- displays

def display_model(name):
    info = DISPLAYS[name]
    if name == "plate":
        elements = [box((3, 0, 3), (13, 1, 13), "#plate", uvs={"up": [3, 3, 13, 13], "down": [3, 3, 13, 13]}
                        | {side: [3, 13, 13, 14] for side in ("north", "south", "east", "west")})]
    elif name == "platter":
        elements = [box((1, 0, 1), (15, 1, 15), "#platter", uvs={"up": [1, 1, 15, 15], "down": [1, 1, 15, 15]})]
    else:  # the serving tray: a base and a rim
        r = "#rim"
        elements = [box((1, 0, 1), (15, 1, 15), r, textures={"up": "#base"}, uvs={"up": [1, 1, 15, 15], "down": [1, 1, 15, 15]}),
                    box((1, 1, 1), (15, 2, 2), r), box((1, 1, 14), (15, 2, 15), r),
                    box((1, 1, 2), (2, 2, 14), r), box((14, 1, 2), (15, 2, 14), r)]
    return block_model(info["textures"], elements, info["particle"])


# ---------------------------------------------------------------- writers

def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    for name, info in PLACED_PIES.items():
        for bites in range(4):
            write(models / (f"{name}.json" if bites == 0 else f"{name}_slice{bites}.json"), pie_data.pie(name, bites, owner=True))
        write(states / f"{name}.json", {"variants": {
            f"bites={b}": {"model": rid(f"block/{name}" + ("" if b == 0 else f"_slice{b}"))} for b in range(4)}})
        lang[f"block.{MOD}.{name}"] = info["display"]
    for name, info in FEASTS.items():
        for servings in range(info["servings"] + 1):
            write(models / f"{name}_{servings}.json", feast_model(name, servings))
        write(states / f"{name}.json", {"variants": {
            f"facing={f},servings={s}": turned(rid(f"block/{name}_{s}"), f)
            for f in ("north", "south", "east", "west") for s in range(info["servings"] + 1)}})
        flat_item(root, write, name)  # the owner's whole-feast icon
        lang[f"block.{MOD}.{name}"] = info["display"]
    for name, info in DISPLAYS.items():
        write(models / f"{name}.json", display_model(name))
        write(states / f"{name}.json", {"variants": {f"facing={f}": turned(rid(f"block/{name}"), f)
                                                     for f in ("north", "south", "east", "west")}})
        write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
        lang[f"block.{MOD}.{name}"] = info["display"]
    lang.update(feasts.TEXT)


def match(block, **state):
    return {"type": "minecraft:match_block", "blocks": rid(block), "state": {k: str(v) for k, v in state.items()}}


def whole(block, item, **state):
    """A pool dropping `item` while the block is whole (`state`) and the explosion spared it."""
    return {"rolls": 1, "entries": [{"type": "minecraft:item", "name": item}],
            "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"}, match(block, **state)]}}


def loot(out, write):
    """Loot in 26.3's own form (singular "condition" and "modifier", minecraft:match_block): a placed pie drops the vanilla
    pie while whole; a feast drops itself while whole and its leftovers once eaten; a display drops itself."""
    for name, info in PLACED_PIES.items():
        write(out / f"{name}.json", {"type": "minecraft:block", "pools": [whole(name, info["item"], bites=0)],
                                     "random_sequence": rid(f"blocks/{name}")})
    for name, info in FEASTS.items():
        pools = [whole(name, rid(name), servings=info["servings"])]
        for item, count in info["leftovers"]:
            entry = {"type": "minecraft:item", "name": item}
            if count != 1:
                entry["modifier"] = [{"type": "minecraft:set_count", "count": count, "add": False}]
            pools.append({"rolls": 1, "entries": [entry], "condition": match(name, servings=0)})
        write(out / f"{name}.json", {"type": "minecraft:block", "pools": pools, "random_sequence": rid(f"blocks/{name}")})
    for name in DISPLAYS:
        write(out / f"{name}.json", self_drop(name))


def tags(tags):
    for name, info in FEASTS.items():
        tags.add("item", "c:foods", rid(info["serving"]))
    for name in DISPLAYS:
        tags.add("block", "minecraft:mineable/axe" if name != "plate" else "minecraft:mineable/pickaxe", rid(name))
