"""JSON resources for the fall fair midway (fall addition 26), from tools/midway.py: the High Striker's five parts (its
base and strike pad, the tower with its lamps lit and unlit and the puck where it is, and the bell, rung or not), Ring
Toss (the crate of nine bottles, and a ring over each in turn), the twelve plushes (the five harvest plushes' models
from tools/decor16_data.py); the mallet, ring and striker item models; names; loot (each block drops itself, the
striker from its base only; the prize table); and the advancements' and recipes' data come through
HALLOWEEN_ADVANCEMENTS, SHAPED and SHAPELESS in tools/agriculture.py.

Called from agriculture_data.py. Models face north (the front at the low z side) and are turned by the blockstates.
"""
from decor_data import MOD, rid, box, block_model, flat_item, self_drop, turned
import midway
from midway import HIGH_STRIKER, RING_TOSS, PLUSHES, LEVELS

HORIZONTAL = ("north", "east", "south", "west")
RUNG = LEVELS + 1
PARTS = 5
# Where each tower part's two rows of lamps sit (bottom of the lamp, in pixels); the top part's are lower, under the bell.
LAMP_ROWS = {1: (3, 11), 2: (3, 11), 3: (3, 11), 4: (1.5, 5.5)}
BOTTLES = ("green", "amber", "milk")


def _lamps(y, lit):
    t = "#lamp_on" if lit else "#lamp_off"
    light = 15 if lit else None
    return [box((3.5, y, 5.5), (5, y + 3, 7), t, light=light), box((11, y, 5.5), (12.5, y + 3, 7), t, light=light),
            # The lamps' brass collars against the post.
            box((4.5, y + 3, 5.75), (5.25, y + 3.5, 6.75), "#brass"), box((10.75, y + 3, 5.75), (11.5, y + 3.5, 6.75), "#brass")]


def _puck(y):
    return [box((6.25, y, 2.75), (9.75, y + 2, 4.25), "#puck"), box((7, y + 0.5, 2.5), (9, y + 1.5, 2.75), "#brass")]


def striker_part(part, lit, puck):
    """Part `part` of the striker, `lit` of its two lamp rows lit, the puck at row `puck` (0 or 1), at the bell
    ("bell") or nowhere (None); the base (part 0) shows the puck at rest when `puck` is 0."""
    textures = {"post": "high_striker_post", "red": "high_striker_red", "brass": "high_striker_brass", "lamp_on": "high_striker_lamp_on",
                "lamp_off": "high_striker_lamp_off", "puck": "high_striker_puck", "pad": "high_striker_pad", "deck": "high_striker_deck",
                "scale": f"high_striker_scale_{max(part, 1)}", "bell": "high_striker_bell", "bell_lit": "high_striker_bell_lit",
                "sign": "high_striker_sign"}
    if part == 0:
        elements = [box((0, 0, 0), (16, 3, 16), "#red", textures={"up": "#deck"}),
                    box((0.5, 3, 0.5), (15.5, 3.5, 15.5), "#brass", faces=("north", "south", "east", "west", "up")),
                    # The foot of the post and its rail.
                    box((5, 3.5, 5), (11, 16, 11), "#post", textures={"north": "#red"}),
                    box((7, 3.5, 4), (9, 16, 5), "#brass"),
                    # The strike pad on its lever, and the lever's pivot.
                    box((3.5, 3.5, 0.5), (12.5, 5, 4.5), "#red", textures={"up": "#pad"}),
                    box((6, 3.5, 4.5), (10, 4.5, 5), "#brass"),
                    # Painted stars on the base's sides.
                    box((2, 0.5, -0.01), (14, 2.5, -0.01), "#sign", faces=("north",), uvs={"north": (0, 0, 16, 4)})]
        if puck == 0:
            elements += _puck(5)
        return block_model(textures, elements, "high_striker_red")
    rows = LAMP_ROWS[part]
    top = 9 if part == PARTS - 1 else 16
    elements = [box((5, 0, 5), (11, top, 11), "#post", textures={"north": "#scale"}, uvs={"north": (5, 16 - top, 11, 16)}),
                box((7, 0, 4), (9, top, 5), "#brass")]
    for row, y in enumerate(rows):
        elements += _lamps(y, row < lit)
    if puck in (0, 1):
        elements += _puck(rows[puck] + 0.5)
    if part == PARTS - 1:
        rung = puck == "bell"
        bell = "#bell_lit" if rung else "#bell"
        light = 15 if rung else None
        elements += [box((4, 9, 4), (12, 10, 12), "#red"),
                     # The bell's arch, and a crown sign on its top.
                     box((4.5, 10, 7), (5.5, 15, 8), "#brass"), box((10.5, 10, 7), (11.5, 15, 8), "#brass"),
                     box((4.5, 14, 7), (11.5, 15, 8), "#brass"),
                     box((5.5, 15, 7.25), (10.5, 16, 7.75), "#sign", uvs={"north": (0, 4, 16, 8), "south": (0, 4, 16, 8)}),
                     # The bell hanging in the arch.
                     box((7.25, 13, 6.75), (8.75, 14, 8.25), bell, light=light),
                     box((6.5, 11.5, 6), (9.5, 13, 9), bell, light=light),
                     box((5.75, 10.25, 5.25), (10.25, 11.5, 9.75), bell, light=light)]
        if rung:
            elements += _puck(9.25)
    return block_model(textures, elements, "high_striker_red")


def striker_model(part, level):
    """The model name for `part` at puck `level`."""
    if part == 0:
        return "high_striker_base" if level == 0 else "high_striker_base_struck"
    lo, hi = 2 * part - 1, 2 * part
    if level < lo:
        state = "off"
    elif level == lo:
        state = "one"
    elif level == hi:
        state = "two_puck"
    elif part == PARTS - 1 and level == RUNG:
        state = "rung"
    else:
        state = "two"
    return f"high_striker_{part}_{state}"


STRIKER_MODELS = {"high_striker_base": (0, 0, 0), "high_striker_base_struck": (0, 0, None)}
for _p in range(1, PARTS):
    STRIKER_MODELS.update({f"high_striker_{_p}_off": (_p, 0, None), f"high_striker_{_p}_one": (_p, 1, 0),
                           f"high_striker_{_p}_two_puck": (_p, 2, 1), f"high_striker_{_p}_two": (_p, 2, None)})
STRIKER_MODELS[f"high_striker_{PARTS - 1}_rung"] = (PARTS - 1, 2, "bell")


def ring_toss(ringed):
    """The crate and its nine bottles, with a ring over bottle `ringed` - 1 (none for 0)."""
    textures = {"crate": "ring_toss_crate", "top": "ring_toss_crate_top", "ring": "ring_toss_ring"}
    textures.update({b: f"ring_toss_bottle_{b}" for b in BOTTLES})
    elements = [box((0, 0, 0), (16, 8, 16), "#crate", textures={"up": "#top"})]
    for index in range(9):
        x, z = midway.neck(index)
        glass = "#" + BOTTLES[(index + index // 3) % 3]
        elements += [box((x - 1.75, 8, z - 1.75), (x + 1.75, 12.5, z + 1.75), glass),
                     box((x - 1, 12.5, z - 1), (x + 1, 13.5, z + 1), glass),
                     box((x - 0.5, 13.5, z - 0.5), (x + 0.5, 16, z + 0.5), glass),
                     box((x - 0.75, 15.5, z - 0.75), (x + 0.75, 16, z + 0.75), glass)]
    if ringed:
        x, z = midway.neck(ringed - 1)
        y = 13.75
        elements += [box((x - 2, y, z - 2), (x + 2, y + 0.75, z - 1), "#ring"), box((x - 2, y, z + 1), (x + 2, y + 0.75, z + 2), "#ring"),
                     box((x - 2, y, z - 1), (x - 1, y + 0.75, z + 1), "#ring"), box((x + 1, y, z - 1), (x + 2, y + 0.75, z + 1), "#ring")]
    return block_model(textures, elements, "ring_toss_crate")


def _face(name):
    return {"north": f"#face"}, {"north": (0, 0, 16, 16)}


def plush(name):
    """Each plush, stuffed felt, sat facing north with its face on the front. The harvest plushes are sculpted in
    tools/decor16_data.py."""
    if PLUSHES[name].get("sculpted"):
        import decor16_data
        return decor16_data.plush_model(name)
    t = {"felt": name, "face": f"{name}_face"}
    face, full = _face(name)
    if name in ("pumpkin_plush", "jumbo_pumpkin_plush"):
        t.update({"stem": "plush_stem", "leaf": "plush_leaf", "rosette": "plush_rosette"})
        if name == "pumpkin_plush":
            e = [box((4, 0, 4), (12, 7, 12), "#felt"),
                 box((3, 0.5, 5), (4, 6.5, 11), "#felt"), box((12, 0.5, 5), (13, 6.5, 11), "#felt"),
                 box((5, 0.5, 3), (11, 6.5, 4), "#felt", textures=face, uvs=full), box((5, 0.5, 12), (11, 6.5, 13), "#felt"),
                 box((7.25, 7, 7.25), (8.75, 9, 8.75), "#stem"), box((8.75, 7, 7.5), (11, 7.5, 9), "#leaf")]
        else:
            e = [box((3, 0, 3), (13, 12, 13), "#felt"),
                 box((1, 1, 4), (3, 11, 12), "#felt"), box((13, 1, 4), (15, 11, 12), "#felt"),
                 box((4, 1, 1), (12, 11, 3), "#felt", textures=face, uvs=full), box((4, 1, 13), (12, 11, 15), "#felt"),
                 box((7, 12, 7), (9, 14.5, 9), "#stem"), box((9, 12, 6.5), (13, 12.5, 9.5), "#leaf"),
                 box((15, 5, 5.5), (15.5, 10, 10.5), "#rosette", faces=("east", "west", "north", "south", "up", "down"),
                     uvs={"east": (0, 0, 16, 16), "west": (0, 0, 16, 16)})]
    elif name == "ghost_plush":
        e = [box((5, 0, 5), (11, 8, 11), "#felt", textures=face, uvs={"north": (0, 0, 16, 16)}),
             box((5.5, 8, 5.5), (10.5, 9.5, 10.5), "#felt"), box((6.5, 9.5, 6.5), (9.5, 10, 9.5), "#felt"),
             box((4.5, 0, 4.5), (11.5, 1.5, 11.5), "#felt"),
             box((3, 4, 7), (5, 5.5, 9), "#felt"), box((11, 4, 7), (13, 5.5, 9), "#felt")]
    elif name == "bat_plush":
        t["wing"] = "bat_plush_wing"
        e = [box((6, 0, 6.5), (10, 5, 9.5), "#felt"),
             box((6, 5, 6), (10, 8.5, 10), "#felt", textures=face, uvs=full),
             box((6, 8.5, 7.5), (7.5, 10, 8.5), "#felt"), box((8.5, 8.5, 7.5), (10, 10, 8.5), "#felt"),
             box((1, 2, 7.5), (6, 8, 8.5), "#wing", uvs={"north": (0, 0, 16, 16), "south": (16, 0, 0, 16)}),
             box((10, 2, 7.5), (15, 8, 8.5), "#wing", uvs={"north": (16, 0, 0, 16), "south": (0, 0, 16, 16)})]
    elif name == "black_cat_plush":
        t["bow"] = "plush_bow"
        e = [box((5, 0, 6), (11, 6, 11), "#felt"),
             box((5.5, 0, 4.5), (7.5, 1.5, 6), "#felt"), box((8.5, 0, 4.5), (10.5, 1.5, 6), "#felt"),
             box((5, 6, 5), (11, 11, 10), "#felt", textures=face, uvs=full),
             box((5.5, 11, 7), (7, 12.5, 8.5), "#felt"), box((9, 11, 7), (10.5, 12.5, 8.5), "#felt"),
             box((7.5, 1, 11), (8.5, 8, 12), "#felt"), box((7.5, 7, 10.5), (8.5, 9, 11.5), "#felt"),
             box((6.5, 5.5, 4.75), (9.5, 6.5, 5.25), "#bow")]
    elif name == "squirrel_plush":
        t.update({"tail": "squirrel_plush_tail", "acorn": "plush_acorn", "cap": "plush_acorn_cap"})
        e = [box((6, 0, 6), (10, 5, 9.5), "#felt"),
             box((6, 5, 5), (10, 8.5, 8.5), "#felt", textures=face, uvs=full),
             box((6.25, 8.5, 6.5), (7.25, 10, 7.5), "#felt"), box((8.75, 8.5, 6.5), (9.75, 10, 7.5), "#felt"),
             box((6, 0.5, 9.5), (10, 9, 12.5), "#tail"), box((6.5, 9, 8), (9.5, 11.5, 12), "#tail"),
             box((7, 2.5, 5), (9, 4.5, 6), "#acorn"), box((6.75, 4.5, 4.75), (9.25, 5, 6.25), "#cap")]
    elif name == "werewolf_plush":
        t.update({"muzzle": "werewolf_plush_muzzle", "snout": "werewolf_plush_snout"})
        e = [box((5.5, 0, 6), (10.5, 6, 10), "#felt"),
             box((5.5, 0, 5), (7.5, 2, 6), "#felt"), box((8.5, 0, 5), (10.5, 2, 6), "#felt"),
             box((4, 2.5, 6.5), (5.5, 6, 8.5), "#felt"), box((10.5, 2.5, 6.5), (12, 6, 8.5), "#felt"),
             box((5, 6, 5), (11, 11, 10), "#felt", textures=face, uvs=full),
             box((6.5, 6.5, 3.5), (9.5, 8.5, 5), "#muzzle", textures={"north": "#snout"}, uvs={"north": (0, 0, 16, 16)}),
             box((5, 11, 7), (6.5, 13, 8.5), "#felt"), box((9.5, 11, 7), (11, 13, 8.5), "#felt"),
             box((7, 1, 10), (9, 4, 11.5), "#felt")]
    else:
        raise ValueError(name)
    return block_model(t, e, name)


def assets(root, write, lang):
    models = root / "models" / "block"
    states = root / "blockstates"
    for name, (part, lit, puck) in STRIKER_MODELS.items():
        write(models / f"{name}.json", striker_part(part, lit, puck))
    write(states / f"{HIGH_STRIKER['block']}.json", {"variants": {
        f"facing={f},level={level},part={part}": turned(rid(f"block/{striker_model(part, level)}"), f)
        for f in HORIZONTAL for part in range(PARTS) for level in range(RUNG + 1)}})
    flat_item(root, write, HIGH_STRIKER["block"])
    write(root / "models" / "item" / f"{HIGH_STRIKER['mallet']}.json",
          {"parent": "minecraft:item/handheld", "textures": {"layer0": rid(f"item/{HIGH_STRIKER['mallet']}")}})
    write(root / "items" / f"{HIGH_STRIKER['mallet']}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{HIGH_STRIKER['mallet']}")}})
    flat_item(root, write, RING_TOSS["ring"])

    name = RING_TOSS["block"]
    for ringed in range(10):
        write(models / (f"{name}.json" if ringed == 0 else f"{name}_ringed_{ringed}.json"), ring_toss(ringed))
    write(states / f"{name}.json", {"variants": {f"ringed={r}": {"model": rid(f"block/{name}" + (f"_ringed_{r}" if r else ""))}
                                                for r in range(10)}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})

    for plush_name in PLUSHES:
        write(models / f"{plush_name}.json", plush(plush_name))
        write(states / f"{plush_name}.json", {"variants": {f"facing={f}": turned(rid(f"block/{plush_name}"), f) for f in HORIZONTAL}})
        write(root / "items" / f"{plush_name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{plush_name}")}})
    for item, display in midway.displays().items():
        kind = "item" if item in (HIGH_STRIKER["mallet"], RING_TOSS["ring"]) else "block"
        lang[f"{kind}.{MOD}.{item}"] = display
    lang[f"entity.{MOD}.{RING_TOSS['ring']}"] = RING_TOSS["ring_display"]


def loot(out, write):
    """Each block drops itself, the striker from its base (part 0) only; the prize table (out = loot_table/blocks)."""
    striker = HIGH_STRIKER["block"]
    write(out / f"{striker}.json", self_drop(striker, {"type": "minecraft:match_block", "blocks": rid(striker), "state": {"part": "0"}}))
    for block in [RING_TOSS["block"]] + list(PLUSHES):
        write(out / f"{block}.json", self_drop(block))
    write(out.parent / f"{midway.PRIZE_TABLE}.json", {"type": "minecraft:gift", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": rid(name), "weight": spec["weight"]} for name, spec in PLUSHES.items()]}],
        "random_sequence": rid(midway.PRIZE_TABLE)})


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(HIGH_STRIKER["block"]))
    tags.add("block", "minecraft:mineable/axe", rid(RING_TOSS["block"]))
    for name in PLUSHES:
        tags.add("item", f"{MOD}:plushes", rid(name))
