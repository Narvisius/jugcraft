"""JSON resources for the Halloween festivities, from tools/agriculture.py: the carving contest's Judging Stand,
costumed mobs' candy, the Halloween Peddler's trades, gravestones, Spun Cobwebs, Hanging Ghosts, Candle Skulls and
the spooky sweets' pot recipes (their items come with ITEMS).

Called from agriculture_data.py (assets, loot, trades, recipes, tags). Formats follow vanilla Minecraft 26.3's own
files, read from the game jar: the wandering trader's villager_trade, tag and trade_set files, the zombie's and the
bee nest's loot tables, and the stonecutter recipes.
"""
from agriculture import CONTEST, COSTUMED_MOBS, PEDDLER, GRAVESTONES, ENGRAVING, FEST_DECOR

MOD = "jugcraft"
HORIZONTAL = ("north", "east", "south", "west")
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
SIDES = ("north", "south", "east", "west")
ALL = SIDES + ("up", "down")


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def turned(model, facing):
    variant = {"model": model}
    if FACING_Y[facing]:
        variant["y"] = FACING_Y[facing]
    return variant


def box(lo, hi, texture, faces=ALL, front=None):
    """A box whose faces take their UVs from its position (so a texture lines up across boxes); `front` is the north face's."""
    return {"from": list(lo), "to": list(hi),
            "faces": {side: {"texture": front if side == "north" and front else texture} for side in faces}}


def block_model(textures, elements, particle):
    return {"parent": "minecraft:block/block", "textures": {"particle": rid(f"block/{particle}"), **{
        key: rid(f"block/{name}") for key, name in textures.items()}}, "elements": elements}


# ---------------------------------------------------------------- models

def judging_stand_model():
    """A wooden foot and post under a purple cloth top whose hem hangs down, with a gold rosette at the front."""
    return block_model({"wood": "judging_stand_wood", "cloth": "judging_stand_cloth", "hem": "judging_stand_hem",
                        "rosette": "judging_stand_rosette"}, [
        box((1, 0, 1), (15, 2, 15), "#wood"),
        box((4, 2, 4), (12, 10, 12), "#wood", faces=SIDES),
        box((1, 10, 1), (15, 13, 15), "#hem", faces=SIDES),
        box((1, 10, 1), (15, 10, 15), "#cloth", faces=("down",)),
        box((0, 13, 0), (16, 16, 16), "#cloth"),
        box((6, 8.5, -0.5), (10, 14.5, 0), "#rosette", faces=("north", "south")),
    ], "judging_stand_cloth")


def gravestone_model(info):
    return block_model({"stone": "gravestone"}, [box(b[:3], b[3:], "#stone") for b in info["boxes"]], "gravestone")


def hanging_ghost_model():
    """A sheet ghost: a round head with a face, a skirt hanging below it, and the string it hangs by."""
    return block_model({"sheet": "hanging_ghost", "face": "hanging_ghost_face", "string": "hanging_ghost_string"}, [
        box((7.5, 13, 7.5), (8.5, 16, 8.5), "#string", faces=SIDES),
        box((5, 8, 5), (11, 13, 11), "#sheet", front="#face"),
        box((4, 1, 4), (12, 8, 12), "#sheet", faces=SIDES + ("down",), front="#face"),
    ], "hanging_ghost")


def candle_skull_model(lit):
    """A bone skull with a white candle melted onto its crown; lit, the wick glows."""
    return block_model({"bone": "candle_skull_bone", "face": "candle_skull_face", "wax": "candle_skull_wax",
                        "wick": "candle_skull_wick_lit" if lit else "candle_skull_wick"}, [
        box((4, 0, 4), (12, 7, 12), "#bone", front="#face"),
        box((5.5, 7, 5.5), (10.5, 8, 10.5), "#wax", faces=SIDES + ("up",)),
        box((6.5, 8, 6.5), (9.5, 13, 9.5), "#wax", faces=SIDES + ("up",)),
        box((7.5, 13, 7.5), (8.5, 14, 8.5), "#wick", faces=SIDES + ("up",)),
    ], "candle_skull_bone")


TEXT = {
    "entity.jugcraft.halloween_peddler": PEDDLER["display"],
    "message.jugcraft.judging_stand.entered": "Entered in this Halloween's carving contest!",
    "message.jugcraft.judging_stand.not_yours": "Only %s, who carved it, can enter this pumpkin",
    "message.jugcraft.judging_stand.no_pumpkin": "Put a hand-carved pumpkin on the stand first",
    "message.jugcraft.judging_stand.voted": "You voted for %s's carving",
    "message.jugcraft.judging_stand.moved": "You moved your vote to %s's carving",
    "message.jugcraft.judging_stand.same": "Your vote is already for %s",
    "message.jugcraft.judging_stand.own_entry": "That's your own carving: everyone else votes for it",
    "message.jugcraft.judging_stand.closed": "%s's carving is entered: voting opens at Halloween",
    "message.jugcraft.judging_stand.full": "This contest has counted all the voters it can",
    "message.jugcraft.judging_stand.no_entry": "Nobody has entered this carving yet",
    "message.jugcraft.judging_stand.standings": "Carving contest %s: %s entrants with votes",
    "message.jugcraft.judging_stand.place": "%s. %s: %s votes",
    "message.jugcraft.carving_contest.over": "The %s carving contest is over! The winners:",
    "message.jugcraft.carving_contest.place": "%s. %s, with %s votes",
    "message.jugcraft.carving_contest.prize": "Your carving contest prize: %s",
    "message.jugcraft.gravestone.unnamed_tag": "Name the tag in an anvil first: its name is what gets engraved",
}


def assets(root, write, lang):
    models, states, items = root / "models", root / "blockstates", root / "items"

    def facing_block(block, model=None):
        write(states / f"{block}.json", {"variants": {f"facing={f}": turned(rid(f"block/{model or block}"), f) for f in HORIZONTAL}})
        write(items / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{model or block}")}})

    stand = CONTEST["stand"]
    write(models / "block" / f"{stand}.json", judging_stand_model())
    facing_block(stand)
    lang[f"block.{MOD}.{stand}"] = CONTEST["stand_display"]

    for stone, info in GRAVESTONES.items():
        write(models / "block" / f"{stone}.json", gravestone_model(info))
        facing_block(stone)
        lang[f"block.{MOD}.{stone}"] = info["display"]

    write(models / "block" / "spun_cobweb.json", {"parent": "minecraft:block/cross", "textures": {"cross": rid("block/spun_cobweb")}})
    write(states / "spun_cobweb.json", {"variants": {"": {"model": rid("block/spun_cobweb")}}})
    write(models / "item" / "spun_cobweb.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("block/spun_cobweb")}})
    write(items / "spun_cobweb.json", {"model": {"type": "minecraft:model", "model": rid("item/spun_cobweb")}})

    write(models / "block" / "hanging_ghost.json", hanging_ghost_model())
    facing_block("hanging_ghost")

    write(models / "block" / "candle_skull.json", candle_skull_model(False))
    write(models / "block" / "candle_skull_lit.json", candle_skull_model(True))
    write(states / "candle_skull.json", {"variants": {
        f"facing={f},lit={str(lit).lower()}": turned(rid("block/candle_skull_lit" if lit else "block/candle_skull"), f)
        for f in HORIZONTAL for lit in (False, True)}})
    write(items / "candle_skull.json", {"model": {"type": "minecraft:model", "model": rid("block/candle_skull")}})

    for block, display in FEST_DECOR.items():
        lang[f"block.{MOD}.{block}"] = display
    lang.update(TEXT)


# ---------------------------------------------------------------- loot tables

def self_drop(block, *modifiers):
    entry = {"type": "minecraft:item", "name": rid(block)}
    if modifiers:
        entry["modifier"] = list(modifiers)
    return {"type": "minecraft:block", "pools": [{"condition": {"type": "minecraft:survives_explosion"}, "entries": [entry], "rolls": 1}],
            "random_sequence": rid(f"blocks/{block}")}


def uniform(low, high):
    return {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}


def loot(out, write):
    """Every block drops itself (a gravestone keeps its engraving as its name); the costumed mobs' candy table."""
    for block in [CONTEST["stand"]] + list(FEST_DECOR):
        write(out / f"{block}.json", self_drop(block))
    for stone in GRAVESTONES:
        write(out / f"{stone}.json", self_drop(stone, {"type": "minecraft:copy_components", "include": ["minecraft:custom_name"],
                                                       "source": "block_entity"}))
    entries = []
    for item, weight, (low, high) in COSTUMED_MOBS["candy"]:
        entry = {"type": "minecraft:item", "name": item, "weight": weight}
        if high > 1:
            entry["modifier"] = uniform(low, high)
        entries.append(entry)
    table = COSTUMED_MOBS["table"]
    write(out.parent / f"{table}.json", {"type": "minecraft:gift", "pools": [{"entries": entries, "rolls": 1}],
                                         "random_sequence": rid(table)})


# ---------------------------------------------------------------- the Peddler's trades

def trades(data, write):
    """villager_trade/halloween_peddler/<name>, the villager_trade tag listing them, and the trade set picking from it."""
    name = PEDDLER["trade_set"]
    ids = []
    for trade, info in PEDDLER["trades"].items():
        item, count = info["gives"]
        gives = {"id": item} if count == 1 else {"count": count, "id": item}
        wants = {"id": "minecraft:emerald"} if info["wants"] == 1 else {"count": info["wants"], "id": "minecraft:emerald"}
        write(data / MOD / "villager_trade" / name / f"{trade}.json",
              {"gives": gives, "max_uses": info["max_uses"], "reputation_discount": 0.05, "wants": wants})
        ids.append(rid(f"{name}/{trade}"))
    write(data / MOD / "tags" / "villager_trade" / f"{name}.json", {"values": ids})
    write(data / MOD / "trade_set" / f"{name}.json", {"amount": PEDDLER["amount"], "random_sequence": rid(f"trade_set/{name}"),
                                                      "trades": f"#{rid(name)}"})


# ---------------------------------------------------------------- recipes and tags

def recipes(out, write, conditions):
    """Gravestones are cut from stone, one each, in a stonecutter."""
    for stone in GRAVESTONES:
        write(out / f"{stone}_from_stonecutting.json", {"fabric:load_conditions": conditions(), "type": "minecraft:stonecutting",
                                                        "ingredient": ENGRAVING["stone"], "result": {"id": rid(stone), "count": 1}})


def tags(tags):
    for stone in GRAVESTONES:
        tags.add("block", "minecraft:mineable/pickaxe", rid(stone))
    tags.add("block", "minecraft:mineable/pickaxe", rid("candle_skull"))
    tags.add("block", "minecraft:mineable/axe", rid(CONTEST["stand"]))
