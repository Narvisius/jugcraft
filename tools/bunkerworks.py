"""Bunker and trench interiors (batch 59, docs/features/bunker-interiors.md): what goes inside a dugout.

- Trench Periscope: two blocks tall; its mirror head pokes over the parapet. It counts the hostile mobs in view for a
  comparator, and looking through it marks the nearest one as a Range Finder would.
- Map Table: a campaign map on a table. It lists the target marks plotted near it and hands the next one to a fire
  control table beside it.
- Gas Curtain: a wet blanket hung across a doorway. Let down, it keeps chlorine and smoke out; rolled up, it is open.
- Field Kitchen: an iron stove that burns fuel only while a Cooking Pot stands on it, and heats that pot. Trench Stew is
  its dish.
- Corrugated Iron (with slab and stairs), Timber Shoring, the Bunker Lamp and the Bunker Bunk.

Every texture is drawn here in the clean style (tools/clean_metal.py): flat fills in a few tones, lit and shaded edges,
no noise. Java: building/Bunkerworks.java (the blocks), building/TrenchPeriscopeBlock, MapTableBlock, GasCurtainBlock,
FieldKitchenBlock and BunkerBunkBlock, artillery/Spotting (Spotting.near), weapons/ChemicalCloud (the curtain) and
agriculture/JugcraftAgriculture (Trench Stew). tools/check_mod_data.py checks that the numbers match.
"""
import copy

from PIL import Image

import clean_metal
from model_writer import element, fit_uvs, separate_coplanar
from steampunk_models import box, cyl

MOD = "jugcraft"

# id: (display name, kind, hardness, blast resistance).
BLOCKS = {
    "trench_periscope": ("Trench Periscope", "periscope", 2.5, 6.0),
    "map_table": ("Map Table", "map_table", 2.5, 3.0),
    "gas_curtain": ("Gas Curtain", "curtain", 0.8, 0.8),
    "field_kitchen": ("Field Kitchen", "kitchen", 3.5, 6.0),
    "corrugated_iron": ("Corrugated Iron", "family", 3.0, 6.0),
    "timber_shoring": ("Timber Shoring", "pillar", 2.0, 3.0),
    "bunker_lamp": ("Bunker Lamp", "lamp", 3.5, 3.5),
    "bunker_bunk": ("Bunker Bunk", "bunk", 2.0, 2.0),
}
TALL = ("trench_periscope", "gas_curtain", "bunker_bunk")
TOOLTIPS = {
    "trench_periscope": "Two blocks tall: its mirror head looks over the parapet the way you face when you place it. A "
                        "comparator reads how many hostile mobs it sees; use it to mark the nearest for your guns (sneak: "
                        "clear your mark).",
    "map_table": "Use it to read the target marks plotted within 256 blocks. Sneak and use it to give the next one to a fire "
                 "control table within 4 blocks.",
    "gas_curtain": "A wet blanket for a dugout's doorway. Let down, it keeps chlorine and smoke out; use it to roll it up or "
                   "let it down. You walk through it either way.",
    "field_kitchen": "A stove for a Cooking Pot on top. Burns logs, coal, charcoal or coke, and only starts a new piece "
                     "while a pot stands on it.",
    "corrugated_iron": "Galvanised sheet for bunker roofs and trench walls.",
    "timber_shoring": "Squared timber with an iron strap, to hold up a dugout's roof.",
    "bunker_lamp": "A caged lamp: hang it from a ceiling or stand it on a floor.",
    "bunker_bunk": "Two bunks one above the other. Use it to sit on the lower bunk. It sets no spawn point.",
}
# The trench periscope: ticks between its scans, how far it sees (blocks) and its cone's half-angle (degrees).
PERISCOPE_INTERVAL = 20
PERISCOPE_RANGE = 64
PERISCOPE_CONE = 45
# The map table: how far from it (blocks) the marks it lists may point, how near a fire control table must stand, and
# how many marks it lists at most.
MAP_RANGE = 256
MAP_LINK = 4
MAP_LINES = 5
# Light: the field kitchen while it burns, and the bunker lamp.
KITCHEN_LIGHT = 13
LAMP_LIGHT = 14
# Trench Stew (registered with the other foods, tools/agriculture.py ITEMS and POT_RECIPES): its food, its effect and
# its Cooking Pot recipe. In line with chili and forager's stew (10 and 0.8), plus five seconds of Regeneration I.
TRENCH_STEW = {"food": [10, 0.8], "effect": ["REGENERATION", 5],
               "inputs": {"minecraft:bowl": 1, "minecraft:beef": 1, "minecraft:potato": 1, "minecraft:carrot": 1}, "time": 300}

PLATE = "#c:plates/steel"
RECIPES = [
    ("trench_periscope", "trench_periscope", ["PG", "P ", "P "], {"P": PLATE, "G": "minecraft:glass_pane"}, 1),
    ("map_table", "map_table", ["MI", "LL"], {"M": "minecraft:map", "I": "minecraft:iron_nugget", "L": "#minecraft:planks"}, 1),
    ("gas_curtain", "gas_curtain", ["S", "W", "W"], {"S": "minecraft:stick", "W": "#minecraft:wool"}, 1),
    ("field_kitchen", "field_kitchen", ["PPP", "P P", "PFP"], {"P": PLATE, "F": "minecraft:furnace"}, 1),
    ("corrugated_iron", "corrugated_iron", ["PP", "PP"], {"P": PLATE}, 8),
    ("timber_shoring", "timber_shoring", ["L", "I", "L"], {"L": "#minecraft:logs", "I": "minecraft:iron_ingot"}, 3),
    ("bunker_lamp", "bunker_lamp", [" N ", "NTN", " N "], {"N": "minecraft:iron_nugget", "T": "minecraft:torch"}, 1),
    ("bunker_bunk", "bunker_bunk", ["WW", "LL", "WW"], {"W": "#minecraft:wool", "L": "#minecraft:planks"}, 1),
]
TEXTURES = {"corrugated_iron": "bk_corrugated", "timber_shoring": ("bk_shoring_side", "bk_shoring_end")}
LAMP_TEXTURES = {}


def blocks():
    out = []
    for block, (_, kind, _, _) in BLOCKS.items():
        out.append(block)
        if kind == "family":
            out += [f"{block}_slab", f"{block}_stairs"]
    return out


def tool(block):
    """The tool that mines a block quickest (its minecraft:mineable tag)."""
    if block in ("timber_shoring", "map_table", "bunker_bunk"):
        return "axe"
    if block == "gas_curtain":
        return "hoe"
    return "pickaxe"


# ------------------------------------------------------------------ models

# Textures shared with the other building sets.
SKID, GUNMETAL, CHROME, WOOD = "dr_skid", "dp_gunmetal", "dp_chrome", "ts_wood"
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def _elements(boxes):
    """Block model elements from box tuples, separated so no two differently drawn faces share a plane."""
    els = [element(list(f), list(t), tx, rotation=o.get("rotation")) for f, t, tx, *rest in boxes
           for o in [rest[0] if rest else {}]]
    separate_coplanar(els)
    return els


def _textures(els, particle):
    names = sorted({spec["texture"][1:] for e in els for spec in e["faces"].values()})
    textures = {name: f"{MOD}:block/{name}" for name in names}
    textures["particle"] = f"{MOD}:block/{particle}"
    return textures


def _glow(els):
    """The lamp globe lights itself at full brightness in the dark."""
    for e in els:
        if e["faces"] and all(spec["texture"] == "#bk_globe" for spec in e["faces"].values()):
            e["light_emission"] = 15
    return els


def model(boxes, particle):
    els = _glow(_elements(boxes))
    fit_uvs(els)
    return {"textures": _textures(els, particle), "elements": els}


def halves(boxes, particle):
    """A two-block-tall thing drawn whole (0..32 pixels high), separated whole, then cut at 16 into its lower and upper
    block models. A box crossing the cut is split there: the two cut faces would only face each other inside it, so both
    are left out (art_check's X2 finds each covered by the other half)."""
    whole = _elements(boxes)
    lower, upper = [], []
    for e in whole:
        y0, y1 = e["from"][1], e["to"][1]
        if y0 < 16 - 1e-6:
            piece = copy.deepcopy(e)
            if y1 > 16 + 1e-6:
                piece["to"][1] = 16.0
                piece["faces"].pop("up", None)
            lower.append(piece)
        if y1 > 16 + 1e-6:
            piece = copy.deepcopy(e)
            piece["from"][1] = max(0.0, y0 - 16)
            piece["to"][1] = y1 - 16
            if y0 < 16 - 1e-6:
                piece["faces"].pop("down", None)
            upper.append(piece)
    out = []
    for els in (lower, upper):
        fit_uvs(els)
        out.append({"textures": _textures(els, particle), "elements": els} if els else
                   {"textures": {"particle": f"{MOD}:block/{particle}"}, "elements": []})
    return out


# The periscope's textures.
OLIVE_T, BOX_T, WINDOW_T, RUBBER_T = "bk_periscope", "bk_periscope_box", "bk_window", "bk_rubber"


def periscope():
    """The trench periscope facing north (the way it looks), 32 pixels high: a stand on a foot plate, a slim box-section
    tube, the eyepiece box with its rubber eyecup (facing south, to the user) and two handles at about head height, a
    clamp band, and the mirror head on top with its dark glass window looking north over the parapet."""
    head = {"*": BOX_T}
    m = [
        box((5, 0, 6), (11, 1.5, 12), SKID),
        box((7, 1.5, 8), (9, 10, 10), SKID),
        box((6.5, 10, 7), (9.5, 26, 10), OLIVE_T),
        box((6, 10.5, 8.5), (10, 15.5, 12.5), head),
        box((6.75, 11.5, 12.5), (9.25, 14.5, 13.5), RUBBER_T),
        box((4, 12, 9.5), (6, 13.5, 11), RUBBER_T),
        box((10, 12, 9.5), (12, 13.5, 11), RUBBER_T),
        box((6.25, 19, 6.75), (9.75, 20.5, 10.25), SKID),
        box((5.5, 25, 5), (10.5, 30.5, 10), head),
        box((6.25, 26, 4.5), (9.75, 29.5, 5), {"*": BOX_T, "north": f"{WINDOW_T}!"}),
        box((5, 30.5, 4.5), (11, 31.5, 10.5), {"*": BOX_T}),
    ]
    return m


MAP_T, TABLE_T, LEG_T = "bk_map", "ts_wood", "bk_frame"


def map_table():
    """The map table facing north: a timber top at waist height on four legs with stretchers, and the campaign map
    pinned on it (a plate a quarter of a pixel thick, drawn whole)."""
    m = [box((0, 12, 0), (16, 14, 16), TABLE_T)]
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        m.append(box((x, 0, z), (x + 2, 12, z + 2), LEG_T))
    m.append(box((3, 3, 1.5), (13, 4.5, 2.5), TABLE_T))
    m.append(box((3, 3, 13.5), (13, 4.5, 14.5), TABLE_T))
    m.append(box((1.5, 3, 3), (2.5, 4.5, 13), TABLE_T))
    m.append(box((13.5, 3, 3), (14.5, 4.5, 13), TABLE_T))
    m.append(box((1, 14, 1), (15, 14.25, 15), {"*": "bk_paper", "up": f"{MAP_T}!"}))
    # A brass pair of dividers lying on the map, and an inkwell.
    m.append(box((10, 14.25, 3), (10.75, 14.75, 8), "ik_brass"))
    m.append(box((11.25, 14.25, 3.5), (12, 14.75, 8), "ik_brass"))
    m.append(box((2.5, 14.25, 11), (4.5, 15.75, 13), SKID))
    return m


CURTAIN_T, ROLL_T, ROLL_END_T, CORD_T = "bk_curtain", "bk_roll", "bk_roll_end", "bk_cord"


def gas_curtain(rolled):
    """The gas curtain facing north, 32 pixels high: a timber batten across the doorway's top on two iron hooks, and the
    blanket hung from it, one and a half pixels thick, down to half a pixel off the floor; rolled up, the blanket is a
    roll under the batten, tied with two cords."""
    m = [box((0, 29, 6.5), (16, 31, 9.5), WOOD)]
    for x in (2, 13):
        m.append(box((x, 27.5 if rolled else 28, 7.5), (x + 1, 29, 8.5), SKID))
    if rolled:
        m += cyl("x", 25.5, 8, 2.4, 0.5, 15.5, ROLL_T, ROLL_END_T)
        for x in (3, 12):
            m += cyl("x", 25.5, 8, 2.7, x, x + 1, CORD_T)
    else:
        m.append(box((0.5, 0.5, 7.25), (15.5, 28, 8.75), CURTAIN_T))
        m.append(box((0.25, 26.5, 7), (15.75, 28, 9), CURTAIN_T))
    return m


STOVE_T, STOVE_TOP_T, DOOR_T, DOOR_LIT_T, PIPE_T = "bk_stove", "bk_stove_top", "bk_firebox", "bk_firebox_lit", "bk_pipe"


def field_kitchen(lit):
    """The field kitchen facing north: a riveted iron stove on four feet, a hob plate on top for the Cooking Pot, the
    firebox door (glowing through its vents when lit) and a towel rail at the front, and the stove pipe up the back from an
    elbow to the top edge, not above it."""
    body = {"*": f"{STOVE_T}!", "up": STOVE_TOP_T, "down": SKID}
    m = []
    for x, z in ((1, 1), (13, 1), (1, 11.5), (13, 11.5)):
        m.append(box((x, 0, z), (x + 2, 1, z + 2), SKID))
    m.append(box((1, 1, 1), (15, 15, 13.5), body))
    m.append(box((0.5, 15, 0.5), (15.5, 16, 14), {"*": SKID, "up": STOVE_TOP_T}))
    m.append(box((4, 2, 0.5), (12, 10, 1), {"*": SKID, "north": f"{DOOR_LIT_T if lit else DOOR_T}!"}))
    m.append(box((2.5, 12, 0), (13.5, 12.75, 0.5), CHROME))
    m.append(box((2.5, 12, 0.5), (3.25, 12.75, 1), CHROME))
    m.append(box((12.75, 12, 0.5), (13.5, 12.75, 1), CHROME))
    m.append(box((10.75, 4, 13.5), (13.25, 6.5, 14.25), PIPE_T))
    m.append(box((10.75, 4, 14.25), (13.25, 15.5, 16), {"*": PIPE_T, "up": SKID}))
    m.append(box((10.5, 15.5, 14), (13.5, 16, 16), SKID))
    return m


CAGE_T, GLOBE_T = "dr_skid", "bk_globe"


def lamp(hanging):
    """The bunker lamp: a warm glass globe in a wire cage between a base and a cap. Standing, it sits on the floor with a
    carrying loop on top; hanging, it is lifted by six pixels on a short hanger rod from the ceiling."""
    dy = 6 if hanging else 0
    m = [box((5, dy, 5), (11, dy + 1, 11), CAGE_T),
         box((6.5, dy + 1, 6.5), (9.5, dy + 7, 9.5), GLOBE_T),
         box((5, dy + 7, 5), (11, dy + 8, 11), CAGE_T)]
    for x, z in ((5.5, 5.5), (9.5, 5.5), (5.5, 9.5), (9.5, 9.5)):
        m.append(box((x, dy + 1, z), (x + 1, dy + 7, z + 1), CAGE_T))
    for (x0, z0, x1, z1) in ((6.5, 5.5, 9.5, 6.5), (6.5, 9.5, 9.5, 10.5), (5.5, 6.5, 6.5, 9.5), (9.5, 6.5, 10.5, 9.5)):
        m.append(box((x0, dy + 3.5, z0), (x1, dy + 4.5, z1), CAGE_T))
    if hanging:
        m.append(box((7.5, dy + 8, 7.5), (8.5, 16, 8.5), CAGE_T))
        m.append(box((6.5, 15, 6.5), (9.5, 16, 9.5), CAGE_T))
    else:
        m.append(box((7, dy + 8, 7.5), (9, dy + 8.75, 8.5), CAGE_T))
        m.append(box((6.25, dy + 8.75, 7.5), (7, dy + 10, 8.5), CAGE_T))
        m.append(box((9, dy + 8.75, 7.5), (9.75, dy + 10, 8.5), CAGE_T))
        m.append(box((7, dy + 10, 7.5), (9, dy + 10.75, 8.5), CAGE_T))
    return m


FRAME_T, MATTRESS_T, BLANKET_T, PILLOW_T = "bk_frame", "bk_mattress", "bk_blanket", "bk_pillow"


def bunk():
    """The bunk facing north (pillows at the north end), 32 pixels high: four timber posts, and two bunks, each a frame of
    rails round a mattress with a khaki blanket folded over its foot and a pillow at its head; the upper bunk has a guard
    rail along its east side."""
    m = []
    for x, z in ((0, 0), (14, 0), (0, 14), (14, 14)):
        m.append(box((x, 0, z), (x + 2, 31, z + 2), FRAME_T))
    for y in (3, 19):
        m.append(box((0.5, y, 2), (1.5, y + 2, 14), WOOD))
        m.append(box((14.5, y, 2), (15.5, y + 2, 14), WOOD))
        m.append(box((2, y, 0.5), (14, y + 2, 1.5), WOOD))
        m.append(box((2, y, 14.5), (14, y + 2, 15.5), WOOD))
        m.append(box((1.5, y + 1, 1.5), (14.5, y + 3.5, 14.5), {"*": MATTRESS_T}))
        m.append(box((1.25, y + 2, 6), (14.75, y + 4, 14.75), BLANKET_T))
        m.append(box((4, y + 3.5, 2), (12, y + 5, 5), PILLOW_T))
    m.append(box((14.5, 25, 2), (15.5, 26.5, 14), WOOD))
    return m


# ------------------------------------------------------------------ data

def _turned(ref, facing, extra=None):
    y = FACING_Y[facing]
    out = {"model": ref}
    if y:
        out["y"] = y
    if extra:
        out.update(extra)
    return out


def write_all(write, assets, data, lang, condition, self_drop):
    from dieselworks import shaped, write_blocks
    models = assets / "models" / "block"
    states = assets / "blockstates"

    class Standard:
        """Corrugated iron (with slab and stairs) and timber shoring, written by tools/dieselworks.write_blocks."""
        BLOCKS = {k: v for k, v in BLOCKS.items() if v[1] in ("family", "pillar")}
        TEXTURES = TEXTURES
        TOOLTIPS = {}
        RECIPES = [r for r in RECIPES if BLOCKS[r[1]][1] in ("family", "pillar")]
        LAMP_TEXTURES = {}

    write_blocks(write, assets, data, lang, condition, self_drop, Standard)

    def item(block, ref):
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": ref}})

    def flat_item(block):
        write(assets / "models" / "item" / f"{block}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{block}"}})
        item(block, f"{MOD}:item/{block}")

    def lower_drop(block):
        """A two-block-tall thing drops one item, from its lower half only (as the doors do)."""
        return {"type": "minecraft:block", "random_sequence": f"{MOD}:blocks/{block}",
                "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}"}],
                           "condition": {"type": "minecraft:all_of", "terms": [
                               {"type": "minecraft:survives_explosion"},
                               {"type": "minecraft:match_block", "blocks": f"{MOD}:{block}", "state": {"half": "lower"}}]}}]}

    for block, (name, kind, _, _) in BLOCKS.items():
        if kind in ("family", "pillar"):
            continue
        lang[f"block.{MOD}.{block}"] = name
        ref = f"{MOD}:block/{block}"
        if kind == "periscope":
            lower, upper = halves(periscope(), OLIVE_T)
            write(models / f"{block}_lower.json", lower)
            write(models / f"{block}_upper.json", upper)
            write(states / f"{block}.json", {"variants": {
                f"facing={f},half={h}": _turned(f"{ref}_{h}", f) for f in FACING_Y for h in ("lower", "upper")}})
            flat_item(block)
        elif kind == "curtain":
            for rolled in (False, True):
                lower, upper = halves(gas_curtain(rolled), CURTAIN_T)
                suffix = "_rolled" if rolled else ""
                write(models / f"{block}{suffix}_lower.json", lower)
                write(models / f"{block}{suffix}_upper.json", upper)
            write(states / f"{block}.json", {"variants": {
                f"facing={f},half={h},rolled={r}": _turned(f"{ref}{'_rolled' if r == 'true' else ''}_{h}", f)
                for f in FACING_Y for h in ("lower", "upper") for r in ("false", "true")}})
            flat_item(block)
        elif kind == "bunk":
            lower, upper = halves(bunk(), FRAME_T)
            write(models / f"{block}_lower.json", lower)
            write(models / f"{block}_upper.json", upper)
            write(states / f"{block}.json", {"variants": {
                f"facing={f},half={h}": _turned(f"{ref}_{h}", f) for f in FACING_Y for h in ("lower", "upper")}})
            flat_item(block)
        elif kind == "map_table":
            write(models / f"{block}.json", model(map_table(), TABLE_T))
            write(states / f"{block}.json", {"variants": {f"facing={f}": _turned(ref, f) for f in FACING_Y}})
            item(block, ref)
        elif kind == "kitchen":
            write(models / f"{block}.json", model(field_kitchen(False), STOVE_T))
            write(models / f"{block}_lit.json", model(field_kitchen(True), STOVE_T))
            write(states / f"{block}.json", {"variants": {
                f"facing={f},lit={lit}": _turned(ref + ("_lit" if lit == "true" else ""), f)
                for f in FACING_Y for lit in ("false", "true")}})
            item(block, ref)
        elif kind == "lamp":
            write(models / f"{block}.json", model(lamp(False), GLOBE_T))
            write(models / f"{block}_hanging.json", model(lamp(True), GLOBE_T))
            write(states / f"{block}.json", {"variants": {"hanging=false": {"model": ref},
                                                          "hanging=true": {"model": f"{ref}_hanging"}}})
            item(block, ref)
        write(data / "loot_table" / "blocks" / f"{block}.json", lower_drop(block) if block in TALL else self_drop(block))
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang.update({
        f"message.{MOD}.periscope.seen": "%s, %s blocks, %s: marked",
        f"message.{MOD}.periscope.none": "Nothing hostile in view within %s blocks",
        f"message.{MOD}.periscope.cleared": "Mark cleared",
        f"message.{MOD}.map_table.line": "%s's mark: %s blocks %s of the table, %s s ago",
        f"message.{MOD}.map_table.none": "No targets plotted within %s blocks",
        f"message.{MOD}.map_table.someone": "Someone",
        f"message.{MOD}.map_table.plotted": "Fire control table laid on %s's mark: %s blocks %s",
        f"message.{MOD}.map_table.no_fire_control": "No fire control table within %s blocks",
        f"message.{MOD}.field_kitchen.status": "Field kitchen: %s fuel, %s s of fire left",
    })
    for name, result, pattern, key, count in RECIPES:
        if BLOCKS[result][1] not in ("family", "pillar"):
            write(data / "recipe" / f"{name}.json", shaped(condition, pattern, key, result, count))


def add_tags(tags):
    """Mining tags, and the field kitchen as a heat source for the Cooking Pot (the pot counts it only while lit)."""
    for block in blocks():
        tags.add("block", f"minecraft:mineable/{tool(block)}", f"{MOD}:{block}")
    tags.add("block", f"{MOD}:heat_sources", f"{MOD}:field_kitchen")


# ------------------------------------------------------------------ art (the clean style, tools/clean_metal.py)

# Galvanised iron, cool blue-grey: 0 rib gap, 1 shaded flank, 2 fill, 3 lit crest, 4 lit lap edge.
GALV = [(78, 88, 98), (102, 112, 122), (124, 134, 144), (146, 156, 166), (166, 176, 186)]
# Timber (the trench works' wood), dark to light.
TIMBER = [(58, 40, 24), (84, 60, 36), (110, 80, 48), (136, 102, 64), (160, 124, 80)]
STEEL = [(40, 42, 46), (60, 62, 66), (82, 84, 88), (106, 108, 112), (136, 138, 142)]
# Khaki blanket and canvas.
KHAKI = [(76, 72, 46), (98, 93, 60), (120, 114, 76), (142, 136, 94), (162, 156, 112)]
# The periscope's olive drab paint.
OLIVE = [(50, 56, 32), (66, 74, 42), (84, 93, 54), (102, 112, 68), (122, 132, 86)]
# The stove's black iron.
IRON = [(34, 36, 40), (50, 53, 58), (68, 72, 78), (90, 95, 102), (116, 122, 130)]
FIRE = [(150, 48, 16), (214, 92, 24), (244, 150, 44), (252, 206, 104)]


def _canvas(fill):
    return clean_metal.canvas(fill)


def corrugated():
    """Corrugated iron: vertical ribs every four pixels (a lit crest, the fill, a shaded flank and a dark gap), so a wall
    or roof reads as one sheet of ribs; the sheet's lap is split across the block edge (the top row a shade lighter, the
    bottom row a shade darker), never a frame."""
    img = _canvas(GALV[2])
    ramp = (GALV[3], GALV[2], GALV[1], GALV[0])
    for x in range(16):
        for y in range(16):
            clean_metal.put(img, x, y, ramp[x % 4])
    for x in range(16):
        tone = 3 - x % 4
        clean_metal.put(img, x, 0, GALV[min(4, tone + 1)])
        clean_metal.put(img, x, 15, GALV[max(0, tone - 1)])
    return img


def shoring_side():
    """Squared timber standing on end: long grain lines, lit along the left edge and shaded along the right, with a dark
    iron strap round it bolted twice."""
    img = _canvas(TIMBER[2])
    for y in range(16):
        clean_metal.put(img, 0, y, TIMBER[3])
        clean_metal.put(img, 15, y, TIMBER[1])
        for x, (start, length) in ((4, (1, 9)), (9, (5, 11)), (12, (0, 6))):
            if start <= y < start + length:
                clean_metal.put(img, x, y, TIMBER[1])
    for x in range(16):
        clean_metal.put(img, x, 11, STEEL[3])
        clean_metal.put(img, x, 12, STEEL[2])
        clean_metal.put(img, x, 13, STEEL[1])
    for x in (3, 11):
        clean_metal.bolt(img, x, 11, STEEL)
    return img


def shoring_end():
    """The sawn end of the timber: square growth rings round the heart, lit along the top and left edge."""
    img = _canvas(TIMBER[3])
    for y in range(16):
        for x in range(16):
            ring = max(abs(x - 7.5), abs(y - 7.5))
            if round(ring) in (2, 5):
                clean_metal.put(img, x, y, TIMBER[2])
    clean_metal.rect(img, 7, 7, 8, 8, TIMBER[1])
    for i in range(16):
        clean_metal.put(img, i, 0, TIMBER[4])
        clean_metal.put(img, 0, i, TIMBER[4])
        clean_metal.put(img, i, 15, TIMBER[1])
        clean_metal.put(img, 15, i, TIMBER[1])
    return img


def frame():
    """Timber for posts and legs: grain running up it, a lit stripe and a shaded one every four pixels."""
    img = _canvas(TIMBER[2])
    for x in range(16):
        c = (TIMBER[3], TIMBER[2], TIMBER[2], TIMBER[1])[x % 4]
        for y in range(16):
            clean_metal.put(img, x, y, c)
    return img


def curtain():
    """The gas curtain's wet blanket: heavy folds hanging straight down, every four pixels a lit ridge and a shaded
    hollow, and a stitched hem line across it every eight rows."""
    img = _canvas(KHAKI[2])
    for x in range(16):
        c = (KHAKI[3], KHAKI[2], KHAKI[2], KHAKI[1])[x % 4]
        for y in range(16):
            clean_metal.put(img, x, y, c)
    for y in (3, 11):
        for x in range(0, 16, 2):
            clean_metal.put(img, x, y, KHAKI[0] if x % 4 == 2 else KHAKI[1])
    return img


def roll():
    """The rolled-up blanket's side: the turns of the roll as bands along it."""
    img = _canvas(KHAKI[2])
    for y in range(16):
        c = (KHAKI[3], KHAKI[2], KHAKI[2], KHAKI[1])[y % 4]
        for x in range(16):
            clean_metal.put(img, x, y, c)
    return img


def roll_end():
    """The end of the roll: rings of blanket round a dark core."""
    img = _canvas(KHAKI[2])
    for y in range(16):
        for x in range(16):
            r = max(abs(x - 7.5), abs(y - 7.5))
            clean_metal.put(img, x, y, KHAKI[3] if int(r) % 3 == 0 else KHAKI[1] if int(r) % 3 == 2 else KHAKI[2])
    clean_metal.rect(img, 7, 7, 8, 8, KHAKI[0])
    return img


def cord():
    """Twisted cord: even diagonal ridges."""
    img = _canvas((150, 120, 76))
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0:
                clean_metal.put(img, x, y, (182, 152, 102))
            elif (x + y) % 4 == 3:
                clean_metal.put(img, x, y, (118, 92, 56))
    return img


def periscope_tube():
    """The tube's olive paint: plain lengthwise, with a lit stripe and a shaded one every eight pixels."""
    img = _canvas(OLIVE[2])
    for x in range(16):
        c = OLIVE[3] if x % 8 == 1 else OLIVE[1] if x % 8 == 6 else OLIVE[2]
        for y in range(16):
            clean_metal.put(img, x, y, c)
    return img


def periscope_box():
    """The head boxes: olive paint with a one-pixel bevel (lit top and left, shaded bottom and right)."""
    img = _canvas(OLIVE[2])
    clean_metal.bevel(img, 0, 0, 15, 15, OLIVE[3], OLIVE[1], OLIVE[2])
    clean_metal.scuffs(img, OLIVE[3], [(4, 5, 3), (9, 10, 2)], only=OLIVE[2])
    return img


def window():
    """The mirror head's window: dark glass in an olive frame, with one diagonal glint."""
    img = _canvas(OLIVE[1])
    clean_metal.bevel(img, 0, 0, 15, 15, OLIVE[3], OLIVE[0], OLIVE[1])
    clean_metal.rect(img, 3, 3, 12, 12, (38, 50, 58))
    for x in range(3, 13):
        clean_metal.put(img, x, 3, (28, 36, 42))
        clean_metal.put(img, 3, x, (28, 36, 42))
    for i in range(4):
        clean_metal.put(img, 6 + i, 9 - i, (96, 124, 138))
        clean_metal.put(img, 8 + i, 11 - i, (70, 92, 104))
    return img


def rubber():
    """Black rubber: a flat fill with a one-pixel bevel."""
    img = _canvas((40, 40, 44))
    clean_metal.bevel(img, 0, 0, 15, 15, (58, 58, 62), (28, 28, 30), (40, 40, 44))
    return img


def campaign_map():
    """The campaign map: sea along the west, a coast, green fields and tan ground, a river and a front line drawn across
    it, a faint grid every four pixels, a red and a blue pin and a small compass rose, on paper lit along its top edge."""
    sea, sea_deep = (86, 132, 168), (72, 114, 150)
    field, field_dark = (136, 166, 96), (116, 146, 80)
    ground = (204, 184, 136)
    img = _canvas(ground)
    for y in range(16):
        coast = 3 + (1 if 5 <= y <= 9 else 0) - (1 if y >= 13 else 0)
        for x in range(16):
            if x < coast:
                c = sea_deep if x < coast - 2 else sea
            elif (x >= 8 and y < 7) or (x < 9 and y >= 10):
                c = field if (x + y) % 8 else field_dark
            else:
                c = ground
            clean_metal.put(img, x, y, c)
    # The grid: one shade darker every four pixels.
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 4 == 0:
                r, g, b, _a = img.getpixel((x, y))
                clean_metal.put(img, x, y, (r - 14, g - 14, b - 14))
    # A river from the sea to the north-east, and the front line in red across the middle.
    for x, y in ((3, 8), (4, 8), (5, 7), (6, 7), (7, 6), (8, 6), (9, 5), (10, 5), (11, 4), (12, 4), (13, 3)):
        clean_metal.put(img, x, y, sea)
    for x in range(4, 15):
        clean_metal.put(img, x, 10 if x % 3 else 9, (176, 52, 40))
    # Pins: a red one and a blue one, each a lit head over a shadow.
    for x, y, head, shade in ((6, 3, (222, 64, 52), (150, 36, 30)), (11, 12, (70, 104, 210), (40, 62, 140))):
        clean_metal.put(img, x, y, head)
        clean_metal.put(img, x + 1, y + 1, shade)
    # A compass rose in the south-west corner.
    for x, y in ((2, 12), (2, 14), (1, 13), (3, 13)):
        clean_metal.put(img, x, y, (60, 52, 40))
    clean_metal.put(img, 2, 13, (236, 228, 204))
    clean_metal.put(img, 2, 11, (176, 52, 40))
    for i in range(16):
        clean_metal.put(img, i, 0, (226, 212, 172))
        clean_metal.put(img, 0, i, (226, 212, 172))
        clean_metal.put(img, i, 15, (150, 134, 98))
        clean_metal.put(img, 15, i, (150, 134, 98))
    return img


def paper():
    """The map's edges: paper."""
    img = _canvas((214, 198, 156))
    for i in range(16):
        clean_metal.put(img, i, 0, (230, 218, 182))
        clean_metal.put(img, i, 15, (184, 166, 124))
    return img


def stove():
    """The stove's side panels: black iron, a bevelled panel with a seam round it and a row of rivets along the top and
    bottom."""
    img = _canvas(IRON[2])
    clean_metal.plate(img, IRON)
    for x in range(3, 13, 3):
        for y in (2, 12):
            clean_metal.bolt(img, x, y, IRON)
    return img


def stove_top():
    """The hob plate: dark iron, bevelled, with a square lid let into it (shaded inset) and a lifting bar across the lid;
    the Cooking Pot stands over it."""
    img = _canvas(IRON[1])
    clean_metal.bevel(img, 0, 0, 15, 15, IRON[2], IRON[0], IRON[1])
    clean_metal.inset(img, 3, 3, 12, 12, IRON[2], IRON[0])
    clean_metal.rect(img, 4, 4, 11, 11, IRON[1])
    clean_metal.rect(img, 6, 7, 9, 7, IRON[3])
    clean_metal.rect(img, 6, 8, 9, 8, IRON[0])
    return img


def firebox(lit):
    """The firebox door: an iron door in a frame with a row of vent slots and a latch; lit, the slots glow in bands of
    orange to yellow."""
    img = _canvas(IRON[2])
    clean_metal.bevel(img, 0, 0, 15, 15, IRON[3], IRON[0], IRON[2])
    clean_metal.bevel(img, 2, 2, 13, 13, IRON[3], IRON[1], IRON[2])
    for x in (4, 6, 8, 10):
        for y in range(4, 9):
            if lit:
                clean_metal.put(img, x, y, FIRE[3] if y <= 5 else FIRE[2] if y <= 7 else FIRE[1])
                clean_metal.put(img, x + 1, y, FIRE[2] if y <= 5 else FIRE[1] if y <= 7 else FIRE[0])
            else:
                clean_metal.put(img, x, y, IRON[0])
                clean_metal.put(img, x + 1, y, (24, 25, 28))
    for x in range(5, 11):
        clean_metal.put(img, x, 11, IRON[4])
        clean_metal.put(img, x, 12, IRON[1])
    return img


def pipe():
    """The stove pipe: plain black iron along its length, a lit stripe and a shaded one."""
    img = _canvas(IRON[2])
    for x in range(16):
        c = IRON[3] if x % 4 == 1 else IRON[1] if x % 4 == 3 else IRON[2]
        for y in range(16):
            clean_metal.put(img, x, y, c)
    return img


def globe():
    """The lamp's warm glass: amber, lit at the top left with a highlight stripe."""
    fill, lit, shade = (238, 184, 92), (250, 214, 140), (206, 146, 64)
    img = _canvas(fill)
    clean_metal.bevel(img, 0, 0, 15, 15, lit, shade, fill)
    for y in range(2, 14):
        clean_metal.put(img, 4, y, (252, 230, 176))
        clean_metal.put(img, 5, y, lit)
    return img


def mattress():
    """The mattress: canvas ticking, stripes every four rows."""
    img = _canvas((176, 164, 128))
    for y in range(16):
        for x in range(16):
            if y % 4 == 1:
                clean_metal.put(img, x, y, (128, 132, 128))
            elif y % 4 == 3:
                clean_metal.put(img, x, y, (156, 144, 108))
    return img


def blanket():
    """The bunks' khaki army blanket: plain wool with a darker band near each end and a lit fold every eight pixels."""
    img = _canvas(KHAKI[2])
    for y in range(16):
        for x in range(16):
            if y % 8 == 0:
                clean_metal.put(img, x, y, KHAKI[3])
            elif y % 8 == 7:
                clean_metal.put(img, x, y, KHAKI[1])
            if x in (2, 3, 12, 13):
                r, g, b, _a = img.getpixel((x, y))
                clean_metal.put(img, x, y, (r - 20, g - 20, b - 18))
    return img


def pillow():
    """A pillow: off-white, lit top and left."""
    img = _canvas((206, 198, 176))
    clean_metal.bevel(img, 0, 0, 15, 15, (224, 218, 198), (174, 166, 144), (206, 198, 176))
    return img


# ------------------------------------------------------------------ item icons (16x16, outlined, lit from the top left)

OUTLINE = (24, 22, 20)


def _outline(img):
    """A one-pixel near-black outline round every drawn shape."""
    src = img.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3]:
                continue
            if any(0 <= x + dx < 16 and 0 <= y + dy < 16 and src.getpixel((x + dx, y + dy))[3]
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                clean_metal.put(img, x, y, OUTLINE)
    return img


def icon_periscope():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    clean_metal.rect(img, 5, 1, 10, 4, OLIVE[2])
    clean_metal.rect(img, 6, 2, 9, 3, (38, 50, 58))
    clean_metal.put(img, 7, 2, (96, 124, 138))
    clean_metal.rect(img, 7, 5, 8, 12, OLIVE[2])
    clean_metal.rect(img, 7, 5, 7, 12, OLIVE[3])
    clean_metal.rect(img, 6, 9, 10, 11, OLIVE[2])
    clean_metal.rect(img, 10, 10, 11, 10, (40, 40, 44))
    clean_metal.rect(img, 4, 10, 5, 10, (40, 40, 44))
    clean_metal.rect(img, 5, 13, 10, 14, STEEL[2])
    clean_metal.rect(img, 5, 1, 10, 1, OLIVE[3])
    return _outline(img)


def icon_curtain():
    """A blanket hanging in folds from a batten that sticks out past it on both sides, on two dark hooks."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    clean_metal.rect(img, 1, 2, 14, 2, TIMBER[3])
    clean_metal.rect(img, 1, 3, 14, 3, TIMBER[1])
    for x in range(4, 12):
        c = (KHAKI[4], KHAKI[3], KHAKI[2], KHAKI[1])[x % 4]
        clean_metal.rect(img, x, 5, x, 13 if x % 4 in (1, 2) else 12, c)
    for x in (5, 10):
        clean_metal.put(img, x, 4, STEEL[1])
    return _outline(img)


def icon_bunk():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in (2, 13):
        clean_metal.rect(img, x, 1, x, 14, TIMBER[3] if x == 2 else TIMBER[1])
    for y in (4, 10):
        clean_metal.rect(img, 3, y, 12, y + 1, KHAKI[2])
        clean_metal.rect(img, 3, y, 12, y, KHAKI[3])
        clean_metal.rect(img, 3, y - 1, 5, y - 1, (214, 206, 186))
        clean_metal.rect(img, 3, y + 2, 12, y + 2, TIMBER[2])
    return _outline(img)


def trench_stew():
    """Trench Stew in a wooden bowl: brown gravy with beef, potato and carrot."""
    from kitchen_textures import bowl_item
    broth = [(96, 60, 34), (120, 78, 44), (140, 94, 54), (160, 112, 68)]
    return bowl_item(broth, [((150, 54, 44), [(4, 6), (9, 7)]), ((222, 196, 120), [(6, 6), (11, 6)]),
                             ((230, 128, 40), [(8, 7), (5, 7)])])


def draw_all(save):
    for name, img in (("bk_corrugated", corrugated()), ("bk_shoring_side", shoring_side()), ("bk_shoring_end", shoring_end()),
                      ("bk_frame", frame()), ("bk_curtain", curtain()), ("bk_roll", roll()), ("bk_roll_end", roll_end()),
                      ("bk_cord", cord()), ("bk_periscope", periscope_tube()), ("bk_periscope_box", periscope_box()),
                      ("bk_window", window()), ("bk_rubber", rubber()), ("bk_map", campaign_map()), ("bk_paper", paper()),
                      ("bk_stove", stove()), ("bk_stove_top", stove_top()), ("bk_firebox", firebox(False)),
                      ("bk_firebox_lit", firebox(True)), ("bk_pipe", pipe()), ("bk_globe", globe()),
                      ("bk_mattress", mattress()), ("bk_blanket", blanket()), ("bk_pillow", pillow())):
        save(img, "block", name)
    save(icon_periscope(), "item", "trench_periscope")
    save(icon_curtain(), "item", "gas_curtain")
    save(icon_bunk(), "item", "bunker_bunk")
    save(trench_stew(), "item", "trench_stew")
