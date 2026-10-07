"""JSON resources for full-moon werewolves (fall addition 23), from tools/agriculture.py: wolfsbane (vanilla's flower and
potted-flower shapes) and its wild patch; each kind's rug (brown, snow and shadow), a pelt laid flat with its head at one
end; the silver dagger, silver arrow and pelt items; names (each kind of werewolf's too); loot (the flower, the potted
flower, the rugs, the werewolf's bones and each kind's pelt); tags (the silver weapons, the dagger's repair metal
and arrows, the werewolf's haunts and its kinds', its prey, the flower tags); and worldgen. The recipes are in SHAPED and
SHAPELESS, the advancements in HALLOWEEN_ADVANCEMENTS.

Called from agriculture_data.py. Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import WEREWOLF, WOLFSBANE, potted
from decor_data import MOD, rid, box, block_model, flat_item, self_drop, turned

HORIZONTAL = ("north", "east", "south", "west")


def rug(name):
    """The pelt laid flat, a little ragged at the edges, the legs splayed at the corners, the head at the north end."""
    p, h = "#pelt", "#head"
    elements = [box((2, 0, 3), (14, 0.6, 15), p),
                # Legs splayed out at the corners, and the tail.
                box((0, 0, 4), (2, 0.5, 7), p), box((14, 0, 4), (16, 0.5, 7), p),
                box((0, 0, 11), (2, 0.5, 14), p), box((14, 0, 11), (16, 0.5, 14), p),
                box((7, 0, 15), (9, 0.5, 16), p),
                # The head: skull, snout and ears, snarling at the north end.
                box((5, 0, 0.5), (11, 2.5, 4), h), box((6.5, 0, -1.5), (9.5, 1.6, 0.5), h),
                box((5.2, 2.5, 2.5), (6.6, 3.8, 3.5), h), box((9.4, 2.5, 2.5), (10.8, 3.8, 3.5), h)]
    return block_model({"pelt": name, "head": f"{name}_head"}, elements, name)


def assets(root, write, lang):
    models = root / "models" / "block"
    flower = WOLFSBANE["block"]
    write(models / f"{flower}.json", {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{flower}")}})
    write(models / f"{potted(flower)}.json", {"parent": "minecraft:block/flower_pot_cross", "textures": {"plant": rid(f"block/{flower}")}})
    write(root / "blockstates" / f"{flower}.json", {"variants": {"": {"model": rid(f"block/{flower}")}}})
    write(root / "blockstates" / f"{potted(flower)}.json", {"variants": {"": {"model": rid(f"block/{potted(flower)}")}}})
    write(root / "models" / "item" / f"{flower}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"block/{flower}")}})
    write(root / "items" / f"{flower}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{flower}")}})
    lang[f"block.{MOD}.{flower}"] = WOLFSBANE["display"]
    lang[f"block.{MOD}.{potted(flower)}"] = f"Potted {WOLFSBANE['display']}"

    for kind, spec in WEREWOLF["kinds"].items():
        name = spec["rug"]
        write(models / f"{name}.json", rug(name))
        write(root / "blockstates" / f"{name}.json", {"variants": {f"facing={f}": turned(rid(f"block/{name}"), f) for f in HORIZONTAL}})
        write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
        lang[f"block.{MOD}.{name}"] = WEREWOLF["displays"][name]
        flat_item(root, write, spec["pelt"])
        lang[f"item.{MOD}.{spec['pelt']}"] = WEREWOLF["displays"][spec["pelt"]]
        lang[f"entity.{MOD}.{WEREWOLF['entity']}.{kind}"] = spec["display"]

    write(root / "models" / "item" / f"{WEREWOLF['dagger']}.json", {"parent": "minecraft:item/handheld",
                                                                  "textures": {"layer0": rid(f"item/{WEREWOLF['dagger']}")}})
    write(root / "items" / f"{WEREWOLF['dagger']}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{WEREWOLF['dagger']}")}})
    flat_item(root, write, WEREWOLF["arrow"])
    for item in (WEREWOLF["dagger"], WEREWOLF["arrow"]):
        lang[f"item.{MOD}.{item}"] = WEREWOLF["displays"][item]
    lang[f"entity.{MOD}.{WEREWOLF['entity']}"] = WEREWOLF["display"]


def loot(out, write):
    """The flower, the potted flower and the rugs drop themselves (out = loot_table/blocks); a werewolf drops a bone or two
    (its entity table) and its kind's pelt (entities/werewolf/<kind>, rolled by Werewolf.dropPelt)."""
    entities = out.parent / "entities"
    flower = WOLFSBANE["block"]
    write(out / f"{flower}.json", self_drop(flower))
    write(out / f"{potted(flower)}.json", {"type": "minecraft:block", "pools": [
        {"condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": "minecraft:flower_pot"}], "rolls": 1},
        {"condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": rid(flower)}], "rolls": 1}],
        "random_sequence": rid(f"blocks/{potted(flower)}")})
    # The werewolf, whatever its kind: a bone or two.
    write(entities / f"{WEREWOLF['entity']}.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:bone", "modifier": {
            "type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}}]}],
        "random_sequence": rid(f"entities/{WEREWOLF['entity']}")})
    for kind, spec in WEREWOLF["kinds"].items():
        write(out / f"{spec['rug']}.json", self_drop(spec["rug"]))
        write(entities / WEREWOLF["entity"] / f"{kind}.json", {"type": "minecraft:entity", "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid(spec["pelt"])}]}],
            "random_sequence": rid(f"entities/{WEREWOLF['entity']}/{kind}")})


def tags(tags):
    tags.add("item", f"{MOD}:silver_weapons", rid(WEREWOLF["dagger"]))
    tags.add("item", f"{MOD}:repairs_silver_gear", "#c:ingots/silver")
    tags.add("item", "minecraft:arrows", rid(WEREWOLF["arrow"]))
    flower = WOLFSBANE["block"]
    for registry in ("block", "item"):
        tags.add(registry, "minecraft:small_flowers", rid(flower))
    tags.add("block", "minecraft:bee_attractive", rid(flower))
    tags.add("block", "minecraft:flower_pots", rid(potted(flower)))
    for haunt in WEREWOLF["haunts"]:
        tags.add("worldgen/biome", f"{MOD}:werewolf_haunts", haunt)
    for haunt in WEREWOLF["snow_haunts"]:
        tags.add("worldgen/biome", f"{MOD}:snow_werewolf_haunts", haunt)
    for haunt in WEREWOLF["shadow_haunts"]:
        tags.add("worldgen/biome", f"{MOD}:shadow_werewolf_haunts", haunt)
    for prey in WEREWOLF["prey"]:
        tags.add("entity_type", f"{MOD}:werewolf_prey", prey)


def worldgen(data, write):
    """Wolfsbane in patches on taiga and forest floors (new chunks only), like vanilla's flower patches."""
    folder = data / MOD / "worldgen"
    flower = WOLFSBANE["block"]
    patch = WOLFSBANE["patch"]
    write(folder / "feature" / f"{flower}.json", {"type": "minecraft:simple_block", "to_place": {
        "type": "minecraft:weighted", "entries": [{"data": {"id": rid(flower)}, "weight": 1}]}})
    spread = patch["spread_xz"]
    write(folder / "placed_feature" / f"patch_{flower}.json", {"feature": rid(flower), "placement": [
        {"type": "minecraft:rarity_filter", "chance": patch["rarity"]},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:count", "count": patch["tries"]},
        {"type": "minecraft:offset",
         "x": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0},
         "y": {"type": "minecraft:trapezoid", "max": patch["spread_y"], "min": -patch["spread_y"], "plateau": 0},
         "z": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0}},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
            {"type": "minecraft:matching_blocks", "blocks": ["minecraft:grass_block", "minecraft:podzol"], "offset": [0, -1, 0]}]}},
    ]})
