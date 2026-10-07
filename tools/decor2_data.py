"""JSON resources for the second batch of Halloween decorations, from tools/agriculture.py: the Luminaria, Floating
Candles, the Skeleton Hand Sconce and Bat Bunting (soul-flame carvings are carving_data.py's); their names, loot and
tags.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files: block model
element rotation and light emission, a select on a block state property for an item (the light block's), copy_state
in loot (the bee nest's) and a candle's count by its state (match_block). Model rotations are right-handed: about x,
a positive angle turns +y toward +z, so it lifts the north end of an element.
"""
from agriculture import LUMINARIA, FLOATING_CANDLE, SCONCE, BAT_BUNTING, DYE_COLORS
from decor_data import MOD, HORIZONTAL, SIDES, rid, turned, box, block_model, flat_item, self_drop

# The bag's four paper walls, each a plane seen from outside (the cut face) and from inside (plain paper lit by the
# candle, so the face shines when seen through its holes); zero thickness.
BAG_WALLS = [((4, 0, 4), (12, 10, 4), ("north", "south")), ((4, 0, 12), (12, 10, 12), ("south", "north")),
             ((4, 0, 4), (4, 10, 12), ("west", "east")), ((12, 0, 4), (12, 10, 12), ("east", "west"))]


def luminaria_model(color, lit):
    """A paper bag 8 pixels square and 10 tall, open at the top, its walls cut with a jack-o'-lantern face; sand in
    the bottom and a candle standing in it. Lit, the paper glows and a flame burns on the wick."""
    paper = f"luminaria_{color}_lit" if lit else f"luminaria_{color}"
    elements = [box(lo, hi, "#paper", faces=faces, textures={faces[1]: "#inside"}, light=12 if lit else None) for lo, hi, faces in BAG_WALLS]
    elements += [
        box((4, 0, 4), (12, 1.5, 12), "#sand", faces=("up",)),
        box((7, 1.5, 7), (9, 6, 9), "#candle", faces=SIDES + ("up",)),
    ]
    if lit:
        elements.append(box((7.5, 6, 7.5), (8.5, 7.5, 8.5), "#flame", faces=SIDES + ("up",), light=15))
    return block_model({"paper": paper, "inside": "luminaria_inside_lit" if lit else "luminaria_inside", "sand": "luminaria_sand",
                        "candle": "luminaria_candle", "flame": "luminaria_flame"},
                       elements, paper)


def sconce_model(lit):
    """The sconce reaching north from a wall on its south side: an iron plate, two forearm bones angled up to the
    hand, the hand gripping a torch (palm behind, fingers round the front, a thumb) and the torch's head."""
    arm = {"origin": [8, 5, 15], "axis": "x", "angle": 22.5}
    return block_model({"bone": "skeleton_hand_sconce_bone", "iron": "skeleton_hand_sconce_iron", "wood": "skeleton_hand_sconce_wood",
                        "head": "skeleton_hand_sconce_flame" if lit else "skeleton_hand_sconce_coal"}, [
        box((5.5, 3, 15), (10.5, 10, 16), "#iron"),
        box((6.5, 4.5, 10), (7.5, 5.5, 15), "#bone", rotation=arm),
        box((8.5, 4.5, 10), (9.5, 5.5, 15), "#bone", rotation=arm),
        box((6.5, 6, 9.5), (9.5, 9.5, 10.5), "#bone"),
        box((6.5, 7, 8), (9.5, 7.75, 8.5), "#bone"),
        box((6.5, 8.25, 8), (9.5, 9, 8.5), "#bone"),
        box((6.5, 7, 8.5), (7.5, 9, 9.5), "#bone"),
        box((8.5, 7, 8.5), (9.5, 9, 9.5), "#bone"),
        box((9.5, 8, 8.5), (10.25, 9.5, 9.5), "#bone"),
        box((7.5, 5, 8.5), (8.5, 13.5, 9.5), "#wood"),
        box((7.25, 13.5, 8.25), (8.75, 15, 9.75), "#head", light=15 if lit else None),
    ], "skeleton_hand_sconce_bone")


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"

    bag = LUMINARIA["block"]
    for color in DYE_COLORS:
        write(models / f"{bag}_{color}.json", luminaria_model(color, False))
        write(models / f"{bag}_{color}_lit.json", luminaria_model(color, True))
    write(states / f"{bag}.json", {"variants": {f"color={color},lit={str(lit).lower()}": {"model": rid(f"block/{bag}_{color}{'_lit' if lit else ''}")}
                                                for color in DYE_COLORS for lit in (False, True)}})
    # The item shows the bag in the colour it was broken in (the loot table copies the colour onto it).
    write(root / "items" / f"{bag}.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "color",
        "cases": [{"when": color, "model": {"type": "minecraft:model", "model": rid(f"block/{bag}_{color}")}} for color in DYE_COLORS if color != "white"],
        "fallback": {"type": "minecraft:model", "model": rid(f"block/{bag}_white")}}})
    lang[f"block.{MOD}.{bag}"] = LUMINARIA["display"]

    candle = FLOATING_CANDLE["block"]
    # The candles are drawn by the client (client/FloatingCandleRenderer.java); the block model is only its particle.
    write(models / f"{candle}.json", {"textures": {"particle": rid(f"block/{candle}")}})
    write(states / f"{candle}.json", {"variants": {f"candles={n},lit={str(lit).lower()}": {"model": rid(f"block/{candle}")}
                                                   for n in range(1, FLOATING_CANDLE["max"] + 1) for lit in (False, True)}})
    flat_item(root, write, candle)
    lang[f"block.{MOD}.{candle}"] = FLOATING_CANDLE["display"]

    sconce = SCONCE["block"]
    write(models / f"{sconce}.json", sconce_model(True))
    write(models / f"{sconce}_unlit.json", sconce_model(False))
    write(states / f"{sconce}.json", {"variants": {f"facing={f},lit={str(lit).lower()}": turned(rid(f"block/{sconce}{'' if lit else '_unlit'}"), f)
                                                   for f in HORIZONTAL for lit in (False, True)}})
    flat_item(root, write, sconce)
    lang[f"block.{MOD}.{sconce}"] = SCONCE["display"]

    flat_item(root, write, BAT_BUNTING["item"])
    lang[f"item.{MOD}.{BAT_BUNTING['item']}"] = BAT_BUNTING["display"]


def loot(out, write):
    """The luminaria keeps its colour; floating candles drop one candle each; the sconce drops itself."""
    bag = LUMINARIA["block"]
    write(out / f"{bag}.json", {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": rid(bag),
                     "modifier": {"type": "minecraft:copy_state", "block": rid(bag), "properties": ["color"]}}],
        "rolls": 1}], "random_sequence": rid(f"blocks/{bag}")})
    candle = FLOATING_CANDLE["block"]
    write(out / f"{candle}.json", {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": rid(candle), "modifier": [
            {"type": "minecraft:set_count", "count": n, "condition": {"type": "minecraft:match_block", "blocks": rid(candle),
                                                                     "state": {"candles": str(n)}}}
            for n in range(2, FLOATING_CANDLE["max"] + 1)]}],
        "rolls": 1}], "random_sequence": rid(f"blocks/{candle}")})
    write(out / f"{SCONCE['block']}.json", self_drop(SCONCE["block"]))


def tags(tags):
    tags.add("block", "minecraft:mineable/pickaxe", rid(SCONCE["block"]))
