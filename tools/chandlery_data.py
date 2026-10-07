"""JSON resources for the chandlery (fall additions 1), from tools/agriculture.py: the Wax Melting Pot and the Aura
Candle's models and blockstates, the candle item's tinted model, their names, the waxes' and scents' names, the
candle's tooltip and the pot's messages, loot (a candle keeps what it is made of and how long it has burned) and the
tags that say what melts into wax, what scents it and what brightens or extends it.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files. The wax in the
pot and the candle itself are drawn by the client's WaxPotRenderer and AuraCandleRenderer in their colours; the block
models are the pot and the candle's brass dish.
"""
from agriculture import CHANDLERY
from decor_data import MOD, rid, box, block_model, self_drop

POT_TEXTURES = {"copper": "wax_pot", "inside": "wax_pot_inside"}
DISH_TEXTURES = {"brass": "candle_dish"}


def pot():
    """A hammered copper pot on a thick base: walls a pixel thick, a rolled rim, a pouring lip and two handles."""
    c, i = "#copper", "#inside"
    elements = [box((2, 0, 2), (14, 1, 14), c, textures={"up": i}),
                box((2, 1, 2), (14, 11, 3), c, textures={"south": i}), box((2, 1, 13), (14, 11, 14), c, textures={"north": i}),
                box((2, 1, 3), (3, 11, 13), c, textures={"east": i}), box((13, 1, 3), (14, 11, 13), c, textures={"west": i}),
                box((1.5, 10, 1.5), (14.5, 11, 2.5), c), box((1.5, 10, 13.5), (14.5, 11, 14.5), c),
                box((1.5, 10, 2.5), (2.5, 11, 13.5), c), box((13.5, 10, 2.5), (14.5, 11, 13.5), c),
                box((7, 9.5, 0.5), (9, 10.5, 1.5), c),
                box((0.5, 6, 7), (2, 7, 9), c), box((0.5, 7, 7), (1.5, 9, 9), c),
                box((14, 6, 7), (15.5, 7, 9), c), box((14.5, 7, 7), (15.5, 9, 9), c)]
    return block_model(POT_TEXTURES, elements, POT_TEXTURES["copper"])


def dish():
    """A shallow brass dish with a finger ring at its side; the candle on it is drawn by the client."""
    b = "#brass"
    elements = [box((4, 0, 4), (12, 1, 12), b), box((3.5, 0.5, 3.5), (12.5, 1.25, 4), b), box((3.5, 0.5, 12), (12.5, 1.25, 12.5), b),
                box((3.5, 0.5, 4), (4, 1.25, 12), b), box((12, 0.5, 4), (12.5, 1.25, 12), b),
                box((12.5, 0.5, 7.5), (14, 1, 8.5), b), box((13.5, 1, 7.5), (14, 2.5, 8.5), b)]
    return block_model(DISH_TEXTURES, elements, DISH_TEXTURES["brass"])


TEXT = {
    "item.jugcraft.aura_candle.plain": "%s Candle",
    "item.jugcraft.aura_candle.one": "%s Candle of %s",
    "item.jugcraft.aura_candle.two": "%s Candle of %s and %s",
    "item.jugcraft.aura_candle.muddled": "Muddled %s Candle",
    "tooltip.jugcraft.aura_candle.muddled": "Its scents clash: it gives light, but no aura",
    "tooltip.jugcraft.aura_candle.unscented": "Unscented: it gives light only",
    "tooltip.jugcraft.aura_candle.radius": "Aura: %s blocks around",
    "tooltip.jugcraft.aura_candle.burns": "Burns for %s:%s",
    "tooltip.jugcraft.aura_candle.layers": "%s of %s layers",
    "tooltip.jugcraft.aura_candle.bright": "Brightened: a stronger aura",
    "tooltip.jugcraft.aura_candle.lasting": "Long-burning",
    "message.jugcraft.wax_melting_pot.other_wax": "The pot holds another wax: use it up or pour it away first",
    "message.jugcraft.wax_melting_pot.full": "The pot is full",
    "message.jugcraft.wax_melting_pot.not_molten": "Melt the wax first: set the pot over a fire",
    "message.jugcraft.wax_melting_pot.already": "That's already in the wax",
    "message.jugcraft.wax_melting_pot.two_scents": "Two scents is all a candle can carry",
    "message.jugcraft.wax_melting_pot.dipped": "Dipped: %s of %s layers. Let it cool before the next dip",
    "message.jugcraft.wax_melting_pot.muddled": "Dipped: %s of %s layers, but its scents clash: it's muddled",
    "message.jugcraft.wax_melting_pot.too_warm": "Still warm! The new layer slid off",
    "message.jugcraft.wax_melting_pot.full_size": "This candle is as big as candles get",
    "message.jugcraft.wax_melting_pot.empty": "The pot is empty",
    "message.jugcraft.wax_melting_pot.poured": "You pour the wax away",
    "message.jugcraft.wax_melting_pot.status": "%s: %s of %s measures molten",
    "message.jugcraft.wax_melting_pot.bright": "brightened",
    "message.jugcraft.wax_melting_pot.lasting": "long-burning",
}


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"
    pot_id, candle = CHANDLERY["pot"], CHANDLERY["candle"]
    write(models / f"{pot_id}.json", pot())
    write(states / f"{pot_id}.json", {"variants": {"": {"model": rid(f"block/{pot_id}")}}})
    write(items / f"{pot_id}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{pot_id}")}})
    lang[f"block.{MOD}.{pot_id}"] = CHANDLERY["pot_display"]

    write(models / f"{candle}.json", dish())
    write(states / f"{candle}.json", {"variants": {f"dips={d},lit={str(lit).lower()}": {"model": rid(f"block/{candle}")}
                                                   for d in range(1, CHANDLERY["max_dips"] + 1) for lit in (False, True)}})
    # The candle's body is tinted by its dyed colour (layer 0); its wick and dish are not (layer 1).
    write(root / "models" / "item" / f"{candle}.json", {"parent": "minecraft:item/generated", "textures": {
        "layer0": rid(f"item/{candle}"), "layer1": rid(f"item/{candle}_wick")}})
    tallow = CHANDLERY["waxes"]["tallow"]["color"]
    write(items / f"{candle}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{candle}"),
                                               "tints": [{"type": "minecraft:dye", "default": (0xFF000000 | tallow) - (1 << 32)}]}})
    lang[f"block.{MOD}.{candle}"] = CHANDLERY["candle_display"]
    for wax, info in CHANDLERY["waxes"].items():
        lang[f"candle_wax.{MOD}.{wax}"] = info["display"]
    for scent, info in CHANDLERY["scents"].items():
        lang[f"candle_scent.{MOD}.{scent}"] = info["display"]
    lang.update(TEXT)


def loot(out, write):
    """The pot drops itself (its wax is lost); a candle drops itself as it is, part-burned (never once burned out)."""
    write(out / f"{CHANDLERY['pot']}.json", self_drop(CHANDLERY["pot"]))
    candle = CHANDLERY["candle"]
    table = self_drop(candle)
    table["pools"][0]["entries"][0]["modifier"] = [{"type": "minecraft:copy_components", "source": "block_entity",
                                                    "include": [rid("candle"), "minecraft:dyed_color", "minecraft:item_name"]}]
    write(out / f"{candle}.json", table)


def tags(tags):
    tags.add("block", "minecraft:mineable/pickaxe", rid(CHANDLERY["pot"]))
    for wax, info in CHANDLERY["waxes"].items():
        for item in info["items"]:
            tags.add("item", f"jugcraft:candle_wax/{wax}", item)
    for scent, info in CHANDLERY["scents"].items():
        for item in info["items"]:
            tags.add("item", f"jugcraft:candle_scents/{scent}", item)
    for kind in ("brightener", "extender"):
        for item in CHANDLERY[kind]["items"]:
            tags.add("item", CHANDLERY[kind]["tag"], item)
