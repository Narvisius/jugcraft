"""The town's shops and their prices in Jugs (the town's credit, kept per player by the server: town/Jugs.java).

Each shop sells offers (the player pays Jugs) and may buy goods (the player is paid Jugs). An offer is
[item, count, price]; a seasonal offer also names the themes it is on sale in (tools/town_decor.py THEMES). Prices
are whole Jugs for the whole count.

Balance: the General Store is where Jugs come from: it buys a farm's and a mine's ordinary output at modest prices,
which turns surplus into Jugs at the player's own pace. Everything for sale is decoration, fun or convenience; nothing
sold is needed for progression. No shop sells anything any shop buys, so no item can be bought and sold back at a
profit (tools/check_mod_data.py checks it, and that per-item sale prices beat buy prices for anything in both lists).
Emeralds are bought, never sold, so villager trading cannot be looped through the town either.
"""

SHOPS = {
    "general": {
        "name": "General Store",
        "sells": [
            ["minecraft:torch", 16, 3], ["minecraft:lantern", 1, 3], ["minecraft:bread", 4, 3],
            ["minecraft:cooked_beef", 4, 4], ["minecraft:ladder", 8, 2], ["minecraft:glass", 8, 4],
            ["minecraft:bone_meal", 8, 3], ["minecraft:bucket", 1, 6], ["minecraft:arrow", 16, 4],
            ["minecraft:shield", 1, 10], ["minecraft:map", 1, 5], ["minecraft:compass", 1, 8],
            ["minecraft:white_bed", 1, 8], ["minecraft:oak_sapling", 2, 2], ["minecraft:flower_pot", 2, 2],
        ],
        "buys": [
            ["minecraft:wheat", 16, 3], ["minecraft:carrot", 16, 3], ["minecraft:potato", 16, 3],
            ["minecraft:beetroot", 16, 3], ["minecraft:pumpkin", 4, 3], ["minecraft:melon_slice", 32, 3],
            ["minecraft:sugar_cane", 16, 2], ["minecraft:cocoa_beans", 16, 3], ["minecraft:sweet_berries", 16, 2],
            ["minecraft:apple", 8, 3], ["minecraft:egg", 12, 2], ["minecraft:leather", 4, 3],
            ["minecraft:white_wool", 8, 3], ["minecraft:feather", 16, 2], ["minecraft:string", 16, 3],
            ["minecraft:bone", 16, 3], ["minecraft:rotten_flesh", 32, 1], ["minecraft:gunpowder", 8, 4],
            ["minecraft:honeycomb", 4, 3], ["minecraft:oak_log", 16, 3], ["minecraft:spruce_log", 16, 3],
            ["minecraft:birch_log", 16, 3], ["minecraft:cobblestone", 64, 1], ["minecraft:coal", 16, 4],
            ["minecraft:copper_ingot", 16, 5], ["minecraft:iron_ingot", 8, 8], ["minecraft:gold_ingot", 4, 8],
            ["minecraft:redstone", 32, 6], ["minecraft:lapis_lazuli", 16, 6], ["minecraft:emerald", 1, 3],
            ["minecraft:amethyst_shard", 8, 4], ["minecraft:cod", 8, 3], ["minecraft:salmon", 8, 3],
        ],
    },
    "seasonal": {
        "name": "Seasonal Stall",
        "sells": [
            ["minecraft:pink_petals", 8, 3, ["spring"]], ["minecraft:cherry_sapling", 1, 3, ["spring"]],
            ["minecraft:flowering_azalea", 1, 4, ["spring"]], ["minecraft:lilac", 2, 3, ["spring"]],
            ["minecraft:pink_candle", 2, 3, ["spring"]],
            ["minecraft:sunflower", 2, 3, ["summer"]], ["minecraft:sea_pickle", 4, 4, ["summer"]],
            ["minecraft:tropical_fish_bucket", 1, 12, ["summer"]], ["minecraft:yellow_candle", 2, 3, ["summer"]],
            ["minecraft:firework_rocket", 8, 5, ["summer", "december"]],
            ["minecraft:carved_pumpkin", 2, 3, ["autumn", "halloween", "harvest"]],
            ["minecraft:jack_o_lantern", 2, 4, ["autumn", "halloween"]],
            ["minecraft:hay_block", 2, 3, ["autumn", "harvest"]], ["minecraft:orange_candle", 2, 3, ["autumn", "halloween"]],
            ["minecraft:pumpkin_pie", 2, 3, ["autumn", "harvest"]],
            ["minecraft:snow_block", 8, 3, ["winter", "december"]], ["minecraft:packed_ice", 8, 4, ["winter"]],
            ["minecraft:powder_snow_bucket", 1, 6, ["winter"]], ["minecraft:cookie", 8, 3, ["winter", "december"]],
            ["minecraft:white_candle", 2, 3, ["winter", "december"]],
            ["minecraft:black_candle", 2, 3, ["halloween"]], ["minecraft:cobweb", 4, 6, ["halloween"]],
            ["minecraft:soul_lantern", 2, 4, ["halloween"]], ["minecraft:skeleton_skull", 1, 40, ["halloween"]],
            ["minecraft:zombie_head", 1, 40, ["halloween"]],
            ["minecraft:cake", 1, 6, ["harvest", "december"]], ["minecraft:baked_potato", 8, 4, ["harvest"]],
            ["minecraft:golden_carrot", 2, 6, ["harvest"]], ["minecraft:honey_bottle", 2, 4, ["harvest"]],
            ["minecraft:red_candle", 2, 3, ["december"]], ["minecraft:green_candle", 2, 3, ["december"]],
            ["minecraft:spruce_sapling", 2, 3, ["december", "winter"]], ["minecraft:red_wool", 4, 3, ["december"]],
        ],
        "buys": [],
    },
    "curios": {
        "name": "Curiosities",
        "sells": [
            ["minecraft:firework_rocket", 8, 5], ["minecraft:name_tag", 1, 15], ["minecraft:lead", 1, 6],
            ["minecraft:saddle", 1, 20], ["minecraft:spyglass", 1, 12], ["minecraft:bundle", 1, 8],
            ["minecraft:painting", 2, 4], ["minecraft:item_frame", 4, 4], ["minecraft:armor_stand", 1, 6],
            ["minecraft:jukebox", 1, 12], ["minecraft:note_block", 2, 4], ["minecraft:bell", 1, 25],
            ["minecraft:music_disc_cat", 1, 30], ["minecraft:music_disc_blocks", 1, 30],
            ["minecraft:music_disc_chirp", 1, 30], ["minecraft:music_disc_mall", 1, 30],
            ["minecraft:music_disc_mellohi", 1, 30], ["minecraft:music_disc_strad", 1, 30],
            ["minecraft:glow_ink_sac", 4, 4], ["minecraft:slime_ball", 4, 5], ["minecraft:candle", 4, 3],
            ["minecraft:glow_item_frame", 2, 5],
        ],
        "buys": [],
    },
    "florist": {
        "name": "Florist",
        "sells": [
            ["minecraft:poppy", 4, 1], ["minecraft:dandelion", 4, 1], ["minecraft:cornflower", 4, 2],
            ["minecraft:allium", 4, 2], ["minecraft:azure_bluet", 4, 2], ["minecraft:red_tulip", 4, 2],
            ["minecraft:orange_tulip", 4, 2], ["minecraft:white_tulip", 4, 2], ["minecraft:pink_tulip", 4, 2],
            ["minecraft:oxeye_daisy", 4, 2], ["minecraft:lily_of_the_valley", 4, 2], ["minecraft:blue_orchid", 4, 3],
            ["minecraft:rose_bush", 2, 3], ["minecraft:peony", 2, 3], ["minecraft:birch_sapling", 2, 2],
            ["minecraft:jungle_sapling", 2, 3], ["minecraft:acacia_sapling", 2, 2], ["minecraft:dark_oak_sapling", 4, 3],
            ["minecraft:azalea", 1, 3], ["minecraft:moss_block", 4, 3], ["minecraft:lily_pad", 4, 3],
        ],
        "buys": [],
    },
}

# Roles townsfolk can have, and the shop each shopkeeper keeps (see tools/town.py's spots).
WELCOME_JUGS = 20
MAX_BALANCE = 1_000_000_000
TRADE_RANGE = 8
ATM_RANGE = 6


def offers(shop, side):
    for offer in SHOPS[shop][side]:
        item, count, price = offer[:3]
        themes = offer[3] if len(offer) > 3 else None
        yield item, count, price, themes


def data():
    out = {}
    for shop, info in SHOPS.items():
        out[shop] = {"name": info["name"], "sells": [], "buys": []}
        for side in ("sells", "buys"):
            for item, count, price, themes in offers(shop, side):
                entry = {"item": item, "count": count, "price": price}
                if themes:
                    entry["themes"] = themes
                out[shop][side].append(entry)
    return out


def loop_errors():
    """No item is both sold and bought, and any per-item buy price stays under every per-item sale price."""
    errors = []
    sold, bought = {}, {}
    for shop in SHOPS:
        for item, count, price, _ in offers(shop, "sells"):
            sold.setdefault(item, []).append(price / count)
        for item, count, price, _ in offers(shop, "buys"):
            bought.setdefault(item, []).append(price / count)
    for item in sold.keys() & bought.keys():
        if max(bought[item]) >= min(sold[item]):
            errors.append(f"{item}: bought at {max(bought[item]):.3f} a piece, sold at {min(sold[item]):.3f}")
        errors.append(f"{item} is both sold and bought by the town")
    for shop in SHOPS:
        for side in ("sells", "buys"):
            for item, count, price, themes in offers(shop, side):
                if count < 1 or count > 64 or price < 1:
                    errors.append(f"{shop} {side} {item}: count {count}, price {price}")
    return errors
