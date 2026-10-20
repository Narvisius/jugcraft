"""Tools, armor and paxels (batch 25, docs/features/tools-and-armor.md), inspired by Mekanism: Tools (MIT; no code or
art taken). Keep GEAR_TIERS, PIECES, PAXEL_TIERS, ARMOR_STYLES and ARMOR_TIERS in sync with gear/JugcraftGear.java
(ARMOR_STYLES also with STYLE_TEMPLATES); tools/check_mod_data.py checks it.
"""

MOD = "jugcraft"

# Jugcraft metals with a full set: display name, repair/crafting tag, feature switch, and their stats.
# Tool stats: (durability, mining speed, attack damage bonus, enchantability); vanilla iron is (250, 6, 2, 14) and
# diamond (1561, 8, 3, 10). Armor: durability multiplier and defense per piece (boots, leggings, chestplate,
# helmet), enchantability, toughness, knockback resistance; vanilla iron is 15, (2, 5, 6, 2), 9, 0, 0.
GEAR_TIERS = {
    "bronze": {"display": "Bronze", "ingot": "#c:ingots/bronze", "feature": "tin",
               "tool": (320, 6.5, 2.0, 14), "drops": "iron",
               "armor": (15, (2, 5, 6, 2), 12, 0.5, 0.0)},
    "steel": {"display": "Steel", "ingot": "#c:ingots/steel", "feature": "machines",
              "tool": (900, 7.0, 2.5, 12), "drops": "diamond",
              "armor": (25, (3, 6, 7, 3), 10, 1.5, 0.05)},
    # Thallite (docs/features/thallite.md): iron's drops, speed and defense, fewer uses, and the best enchantability.
    # Its gear regrows on living soil (REGROWTH). "arms": False keeps it out of tools/arms.py METALS: its arms are only
    # the kinds the owner drew, in a later slice.
    "thallite": {"display": "Thallite", "ingot": "#c:ingots/thallite", "feature": "thallite",
                 "tool": (200, 6.0, 2.0, 18), "drops": "iron",
                 "armor": (13, (2, 5, 6, 2), 18, 0.0, 0.0), "arms": False},
}
TOOLS = ["sword", "pickaxe", "axe", "shovel", "hoe"]
ARMOR = ["helmet", "chestplate", "leggings", "boots"]
PIECES = TOOLS + ARMOR

# Paxels: pickaxe, axe and shovel in one, three times the durability of the tier's tools.
PAXEL_TIERS = {"wood": "Wooden", "stone": "Stone", "iron": "Iron", "gold": "Golden", "diamond": "Diamond",
               "netherite": "Netherite", "bronze": "Bronze", "steel": "Steel"}
PAXEL_DURABILITY = 3
# The vanilla tools a vanilla-tier paxel is made from.
VANILLA_TOOL_PREFIX = {"wood": "wooden", "stone": "stone", "iron": "iron", "gold": "golden", "diamond": "diamond",
                       "netherite": "netherite"}

# Crafting patterns (S stick, # the metal).
PATTERNS = {
    "sword": ["#", "#", "S"], "pickaxe": ["###", " S ", " S "], "axe": ["##", "#S", " S"],
    "shovel": ["#", "S", "S"], "hoe": ["##", " S", " S"],
    "helmet": ["###", "# #"], "chestplate": ["# #", "###", "###"], "leggings": ["###", "# #", "# #"],
    "boots": ["# #", "# #"],
}
ITEM_TAGS = {"sword": "swords", "pickaxe": "pickaxes", "axe": "axes", "shovel": "shovels", "hoe": "hoes",
             "helmet": "head_armor", "chestplate": "chest_armor", "leggings": "leg_armor", "boots": "foot_armor"}

# Styled armor (docs/features/steampunk-and-kaiser-armor.md): another look for a metal's armor, as its own items.
# The key is the styled items' prefix (<key>_<piece>) and their equipment asset. Armor only (not in GEAR_TIERS:
# no tools, paxels or arms). Every number is the metal's: Java derives the material with restyle(...). A smithing
# template plus the addition dresses a plain piece; if "reversible", the template plus the metal's ingot undresses
# it. "perk" names behaviour a style adds outside the material (Earthbound's Rooted, gear/ThalliteGear.java). Java:
# JugcraftGear.ARMOR_STYLES and STYLE_TEMPLATES.
ARMOR_STYLES = {
    "steampunk": {"display": "Steampunk", "metal": "bronze", "template": "steampunk_pattern",
                  "template_name": "Steampunk Pattern", "template_count": 4, "addition": "minecraft:copper_ingot",
                  "reversible": True, "perk": None,
                  "template_recipe": (["CLC", "GPG", "CLC"], {"C": "minecraft:copper_ingot", "L": "minecraft:leather",
                                                              "G": "minecraft:glass_pane", "P": "minecraft:paper"}),
                  "lore": "Steampunk: bronze and copper, with goggles and a boiler on the back. "
                          "Protects as bronze armor does.",
                  "template_tooltip": "A smithing template. With a copper ingot it turns a bronze helmet, chestplate, "
                                      "leggings or boots into Steampunk armor; with a bronze ingot it turns "
                                      "Steampunk armor back. Keeps enchantments and wear."},
    "kaiser": {"display": "Kaiser", "metal": "steel", "template": "kaiser_pattern",
               "template_name": "Kaiser Pattern", "template_count": 4, "addition": "minecraft:gold_ingot",
               "reversible": True, "perk": None,
               "template_recipe": (["NCN", "BPB", "NRN"], {"N": "minecraft:gold_nugget", "C": "jugcraft:imperial_crest",
                                                           "B": "minecraft:black_dye", "P": "minecraft:paper",
                                                           "R": "minecraft:red_dye"}),
               "lore": "Kaiser: field grey and gilt, the parade dress of the Winged Cog. Protects as steel armor does.",
               "template_tooltip": "A smithing template, made with an Imperial Crest. With a gold ingot it turns a "
                                   "steel helmet, chestplate, leggings or boots into Kaiser armor; with a steel "
                                   "ingot it turns Kaiser armor back. Keeps enchantments and wear."},
    # Earthbound thallite (docs/features/thallite.md): one-way, with the Rooted perk. One template a piece: a full set
    # costs 4 gold ingots and 16 gold nuggets, about 5.8 ingots.
    "earthbound_thallite": {"display": "Earthbound Thallite", "metal": "thallite", "template": "earthbinding_template",
                            "template_name": "Earthbinding Template", "template_count": 1,
                            "addition": "minecraft:gold_ingot", "reversible": False, "perk": "rooted",
                            "template_recipe": (["GTG", "TRT", "GTG"], {"G": "minecraft:gold_nugget",
                                                                        "T": "#c:nuggets/thallite",
                                                                        "R": "minecraft:rooted_dirt"}),
                            "lore": "Earthbound: thallite bound with gold. Protects as thallite armor does.",
                            "template_tooltip": "A smithing template. With a gold ingot it binds a thallite helmet, "
                                                "chestplate, leggings or boots into Earthbound armor, for good. Keeps "
                                                "enchantments and wear."},
}

# Thallite's traits (gear/ThalliteGear.java, docs/features/thallite.md). Regrowth: every REGROWTH_SECONDS, each item
# in jugcraft:thallite_gear worn or held gets back one use while its holder stands on jugcraft:living_ground, up to
# REGROWTH_CAP_PERCENT of full; with EARTHBOUND_FOR_STONE or more Earthbound pieces worn, on any jugcraft:earthen_ground.
# Rooted: each Earthbound piece worn (jugcraft:earthbound_armor) adds ROOTED_PER_PIECE knockback resistance while its
# wearer stands on jugcraft:earthen_ground, refreshed every ROOTED_TICKS.
REGROWTH_SECONDS = 5
REGROWTH_CAP_PERCENT = 75
EARTHBOUND_FOR_STONE = 2
ROOTED_PER_PIECE = 0.075
ROOTED_TICKS = 10
# Living soil: vanilla 26.3's dirt, grass and mud tags (grass is not in #minecraft:dirt; tools/agriculture.py BOG_SOIL
# names the three apart too), and by name each soil the record lists, so none depends on what a vanilla tag holds.
LIVING_GROUND = ["#minecraft:dirt", "#minecraft:grass_blocks", "#minecraft:mud", "minecraft:grass_block", "minecraft:dirt",
                 "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:mud", "minecraft:moss_block", "minecraft:rooted_dirt",
                 "minecraft:farmland"]
# Natural ground: living soil, the Overworld's base stone (stone, granite, diorite, andesite, tuff, deepslate), sand
# and gravel; stone, deepslate and sand by name too.
EARTHEN_GROUND = [f"#{MOD}:living_ground", "#minecraft:base_stone_overworld", "minecraft:stone", "minecraft:deepslate",
                  "#minecraft:sand", "minecraft:sand", "minecraft:gravel"]
TRAITS = {
    "regrowth": ("Regrowth", "While you stand on living soil, it mends one use every 5 s, up to 75% of full."),
    "rooted": ("Rooted", "On natural ground, each Earthbound piece takes 7.5% off knockback (30% for a full set). "
                         "With two or more worn, Regrowth works on stone, sand and gravel too."),
}


def thallite_gear():
    """Every item with Regrowth (jugcraft:thallite_gear): thallite's tools and armor, plain and Earthbound."""
    return ([f"{MOD}:thallite_{piece}" for piece in PIECES]
            + [f"{MOD}:{style}_{piece}" for style, info in ARMOR_STYLES.items() if info["metal"] == "thallite"
               for piece in ARMOR])


def earthbound_armor():
    """Every armor piece with Rooted (jugcraft:earthbound_armor)."""
    return [f"{MOD}:{style}_{piece}" for style, info in ARMOR_STYLES.items() if info["perk"] == "rooted"
            for piece in ARMOR]


# Armor-only tiers with numbers of their own (docs/features/bloodthorn-armor.md): the owner's own armor designs, each a
# new tier stronger than vanilla's, worn as a 3D model (tools/<tier>_armor.py; every piece must have one, so no flat
# layer is drawn or needed). Not in GEAR_TIERS: no tools, paxels or arms. "armor" is as in GEAR_TIERS: durability
# multiplier, defense (boots, leggings, chestplate, helmet), enchantability, toughness, knockback resistance; netherite
# is 37, (3, 6, 8, 3), 15, 3.0, 0.1. "repair" is what mends it at an anvil, "fire_resistant" as netherite. No recipe
# or drop yet: the owner, 6 October 2026, "for now just make the armor we can figure that out later" (they might be
# dropped by bosses or craftable), so for now they are creative-only. Java: JugcraftGear.ARMOR_TIERS.
ARMOR_TIERS = {
    "bloodthorn": {"display": "Bloodthorn", "armor": (40, (3, 7, 9, 3), 15, 3.5, 0.15),
                   "repair": "minecraft:netherite_ingot", "fire_resistant": True},
    # Reforged White Diamond: beside Bloodthorn rather than above it, in other strengths: the heavier helm, the longest
    # wear and the best enchanting, but netherite's toughness and no fire resistance; mended with diamonds.
    "reforged_white_diamond": {"display": "Reforged White Diamond", "armor": (45, (3, 7, 8, 4), 20, 3.0, 0.1),
                               "repair": "minecraft:diamond", "fire_resistant": False},
    # Hades: the underworld's plate, beside the other two in other strengths: the toughest and steadiest, but a point
    # less defense and the poorest enchanting; fire resistant, mended with netherite.
    "hades": {"display": "Hades", "armor": (42, (3, 7, 8, 3), 12, 4.0, 0.2),
              "repair": "minecraft:netherite_ingot", "fire_resistant": True},
    # Sunset Gem: beside the others in other strengths: the longest wear and the best enchanting of all, but a point
    # less defense than Bloodthorn's and netherite's toughness; mended with amethyst shards.
    "sunset_gem": {"display": "Sunset Gem", "armor": (48, (3, 7, 8, 3), 25, 3.0, 0.1),
                   "repair": "minecraft:amethyst_shard", "fire_resistant": False},
    # Pharaoh: beside the others in other strengths: the heaviest helm and chest (but leggings like netherite's),
    # good enchanting, netherite's toughness; fire resistant, as the desert sun asks; mended with gold.
    "pharaoh": {"display": "Pharaoh", "armor": (41, (3, 6, 9, 4), 22, 3.0, 0.1),
                "repair": "minecraft:gold_ingot", "fire_resistant": True},
    # The four designs the owner sent on 7 October 2026 ("just create the armors... we can figure that out later"),
    # beside the others in other strengths, each after its look (docs/features/four-armor-designs.md).
    # Dread Knight: the heaviest plate of all and among the steadiest, but the poorest enchanting; fire resistant, as
    # its dark forge asks; mended with netherite.
    "dread_knight": {"display": "Dread Knight", "armor": (44, (3, 7, 9, 4), 10, 3.5, 0.2),
                     "repair": "minecraft:netherite_ingot", "fire_resistant": True},
    # Valkyrie: the light plate of a winged rider: netherite's toughness, long wear and good enchanting, a point of
    # defense over netherite; mended with phantom membranes, as wings are.
    "valkyrie": {"display": "Valkyrie", "armor": (46, (3, 7, 8, 3), 24, 3.0, 0.1),
                 "repair": "minecraft:phantom_membrane", "fire_resistant": False},
    # Wayfarer: a traveller's cloak and leathers: netherite's defense, the longest wear and the best enchanting of all,
    # but less toughness and no knockback resistance; mended with leather.
    "wayfarer": {"display": "Wayfarer", "armor": (50, (3, 6, 8, 3), 30, 2.5, 0.0),
                 "repair": "minecraft:leather", "fire_resistant": False},
    # Spartan: a hoplite's bronze: the heavier helm, tough and steady, middling enchanting; mended with bronze.
    "spartan": {"display": "Spartan", "armor": (43, (3, 7, 8, 4), 18, 3.5, 0.15),
                "repair": "jugcraft:bronze_ingot", "fire_resistant": False},
    # The designs the owner sent on 8 October 2026, likewise (docs/features/armor-designs-8-october.md).
    # Berserker: horned white plate: a point of defense over netherite, steady, middling enchanting; mended with quartz,
    # white as its plate.
    "berserker": {"display": "Berserker", "armor": (45, (3, 7, 8, 3), 16, 3.0, 0.15),
                  "repair": "minecraft:quartz", "fire_resistant": False},
    # Paladin: the white knight: the heavier helm, long wear and good enchanting, netherite's toughness; mended with
    # amethyst shards, purple as its cloth.
    "paladin": {"display": "Paladin", "armor": (47, (3, 6, 8, 4), 22, 3.0, 0.1),
                "repair": "minecraft:amethyst_shard", "fire_resistant": False},
    # Templar: the dark knight of the pair: the heavier chest, tough and steady, poorer enchanting; fire resistant,
    # mended with netherite.
    "templar": {"display": "Templar", "armor": (43, (3, 7, 9, 3), 14, 3.5, 0.15),
                "repair": "minecraft:netherite_ingot", "fire_resistant": True},
    # Sentinel: the gold-and-black guard: the heavier helm, tough and steady, good enchanting; mended with gold.
    "sentinel": {"display": "Sentinel", "armor": (46, (3, 7, 8, 4), 20, 3.5, 0.15),
                 "repair": "minecraft:gold_ingot", "fire_resistant": False},
    # Frost Knight: the white knight crowned with ice: a point of defense over netherite, steady; mended with blue ice.
    "frost_knight": {"display": "Frost Knight", "armor": (44, (3, 7, 8, 3), 18, 3.0, 0.15),
                     "repair": "minecraft:blue_ice", "fire_resistant": False},
    # Wight King: the slate knight crowned with icicles and antlers: the heavier helm and the steadiest of the cold
    # sets, middling enchanting, netherite's toughness; mended with packed ice.
    "wight_king": {"display": "Wight King", "armor": (45, (3, 7, 8, 4), 15, 3.0, 0.2),
                   "repair": "minecraft:packed_ice", "fire_resistant": False},
    # Reaper: a robe over bone-white plates: netherite's defense, good enchanting, but less toughness and barely any
    # knockback resistance; mended with bone.
    "reaper": {"display": "Reaper", "armor": (42, (3, 6, 8, 3), 24, 2.5, 0.05),
               "repair": "minecraft:bone", "fire_resistant": False},
    # Banana: a costume, not plate: iron's defense, no toughness or knockback resistance, but long wear and the best
    # enchanting of all; mended with yellow wool.
    "banana": {"display": "Banana", "armor": (25, (2, 5, 6, 2), 30, 0.0, 0.0),
               "repair": "minecraft:yellow_wool", "fire_resistant": False},
    # Scarab: gold and lapis: netherite's defense with gold's enchanting, less toughness; mended with lapis lazuli.
    "scarab": {"display": "Scarab", "armor": (40, (3, 6, 8, 3), 25, 2.0, 0.1),
               "repair": "minecraft:lapis_lazuli", "fire_resistant": False},
}


# Batch 27 gear (docs/features/gear-and-plastic.md), after Mekanism's scuba gear, free runners, Meka-Tana and
# Meka-Bow (MIT; no code or art taken): display name, crafting pattern and key, and the item model it uses.
EXTRAS = {
    "scuba_mask": {"display": "Scuba Mask", "pattern": ["SRS", "RGR"], "model": "generated",
                   "key": {"S": "#c:plates/steel", "R": "jugcraft:rubber", "G": "minecraft:glass_pane"}},
    "scuba_tank": {"display": "Scuba Tank", "pattern": ["R R", "PTP", "PPP"], "model": "generated",
                   "key": {"R": "jugcraft:rubber", "P": "#c:plates/steel", "T": "jugcraft:fluid_tank"}},
    "free_runners": {"display": "Free Runners", "pattern": ["L L", "S S", "R R"], "model": "generated",
                     "key": {"L": "minecraft:leather", "S": "#c:plates/steel", "R": "jugcraft:rubber"}},
    "power_katana": {"display": "Power Katana", "pattern": ["  T", "LT ", "AS "], "model": "handheld",
                     "key": {"T": "#c:plates/tungsten", "L": "jugcraft:lithium_cell", "A": "jugcraft:advanced_circuit",
                             "S": "#c:ingots/steel"}},
    "power_bow": {"display": "Power Bow", "pattern": [" PW", "LAW", " PW"], "model": "bow",
                  "key": {"P": "#c:plates/steel", "W": "minecraft:string", "L": "jugcraft:lithium_cell",
                          "A": "jugcraft:advanced_circuit"}},
}
# What the scuba tank holds and uses (Java: ScubaTankItem).
SCUBA_OXYGEN = 8_000
SCUBA_OXYGEN_PER_TICK = 1


def style_items():
    """The styled armor pieces, <style>_<piece>, in ARMOR_STYLES order."""
    return [f"{style}_{piece}" for style in ARMOR_STYLES for piece in ARMOR]


def style_templates():
    """The styled armor's smithing templates, in ARMOR_STYLES order."""
    return [info["template"] for info in ARMOR_STYLES.values()]


def base_piece(item):
    """The plain piece a styled piece is smithed from: steampunk_helmet -> bronze_helmet."""
    style, piece = item.rsplit("_", 1)
    return f"{ARMOR_STYLES[style]['metal']}_{piece}"


def style_of_template(template):
    """The style whose smithing template this is."""
    return next(style for style, info in ARMOR_STYLES.items() if info["template"] == template)


def feature(item):
    """The feature switch that gates an item's recipes: its metal's (a styled piece and its template go with the metal
    they are made from); vanilla-tier paxels and the extras go with the machines."""
    if item in style_templates():
        return GEAR_TIERS[ARMOR_STYLES[style_of_template(item)]["metal"]]["feature"]
    prefix = item.rsplit("_", 1)[0]
    if prefix in GEAR_TIERS:
        return GEAR_TIERS[prefix]["feature"]
    if prefix in ARMOR_STYLES:
        return GEAR_TIERS[ARMOR_STYLES[prefix]["metal"]]["feature"]
    return "machines"


def tier_items():
    """The armor-only tiers' pieces, <tier>_<piece>, in ARMOR_TIERS order."""
    return [f"{tier}_{piece}" for tier in ARMOR_TIERS for piece in ARMOR]


def items():
    """Every item this module registers, in registration order."""
    return ([f"{tier}_{piece}" for tier in GEAR_TIERS for piece in PIECES] + style_items() + tier_items()
            + style_templates() + [f"{tier}_paxel" for tier in PAXEL_TIERS] + list(EXTRAS))


def display(item):
    if item in EXTRAS:
        return EXTRAS[item]["display"]
    if item in style_templates():
        return ARMOR_STYLES[style_of_template(item)]["template_name"]
    tier, piece = item.rsplit("_", 1)
    if piece == "paxel":
        return f"{PAXEL_TIERS[tier]} Paxel"
    if tier in ARMOR_STYLES:
        return f"{ARMOR_STYLES[tier]['display']} {piece.capitalize()}"
    if tier in ARMOR_TIERS:
        return f"{ARMOR_TIERS[tier]['display']} {piece.capitalize()}"
    return f"{GEAR_TIERS[tier]['display']} {piece.capitalize()}"


def tool_ingredient(tier, tool):
    """The pickaxe/axe/shovel of a tier, for its paxel recipe."""
    if tier in GEAR_TIERS:
        return f"{MOD}:{tier}_{tool}"
    return f"minecraft:{VANILLA_TOOL_PREFIX[tier]}_{tool}"


def write_all(write, assets, data, lang, condition):
    """Item models and definitions, names, worn-armor equipment assets, recipes and tags."""
    for item in items():
        lang[f"item.{MOD}.{item}"] = display(item)
        piece = item.rsplit("_", 1)[1]
        # Armor, styled armor and the styled armor's templates are flat icons; tools and paxels are held.
        if item in EXTRAS:
            kind = EXTRAS[item]["model"]
        else:
            kind = "generated" if piece in ARMOR or item in style_templates() else "handheld"
        model = {"parent": f"minecraft:item/{kind}", "textures": {"layer0": f"{MOD}:item/{item}"}}
        write(assets / "models" / "item" / f"{item}.json", model)
        definition = {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}
        if kind == "bow":
            # Drawn back in three steps while held, like the vanilla bow (minecraft:use_duration).
            for step in range(3):
                write(assets / "models" / "item" / f"{item}_pulling_{step}.json",
                      {"parent": "minecraft:item/bow", "textures": {"layer0": f"{MOD}:item/{item}_pulling_{step}"}})
            definition = {"type": "minecraft:condition", "property": "minecraft:using_item", "on_false": definition,
                          "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration",
                                      "scale": 0.05, "fallback": {"type": "minecraft:model",
                                                                  "model": f"{MOD}:item/{item}_pulling_0"},
                                      "entries": [{"threshold": t, "model": {"type": "minecraft:model",
                                                                             "model": f"{MOD}:item/{item}_pulling_{n}"}}
                                                  for n, t in ((1, 0.65), (2, 0.9))]}}
        write(assets / "items" / f"{item}.json", {"model": definition})
    # A flat layer is left out when every piece drawn with it has a 3D model (tools/armor_models.py, drawn by
    # client/WornModelLayer): helmet, chestplate and boots use "humanoid", leggings "humanoid_leggings". With no layer
    # left there is no asset file at all: 26.3 cannot read an empty layer map, and a missing asset draws nothing.
    import armor_models
    worn = {item for armor_set in armor_models.sets() for item in armor_set.pieces}
    for tier in list(GEAR_TIERS) + list(ARMOR_STYLES) + list(ARMOR_TIERS):
        layers = {layer: [{"texture": f"{MOD}:{tier}"}]
                  for layer, pieces in (("humanoid", ("helmet", "chestplate", "boots")), ("humanoid_leggings", ("leggings",)))
                  if not all(f"{tier}_{piece}" in worn for piece in pieces)}
        if layers:
            write(assets / "equipment" / f"{tier}.json", {"layers": layers})
    for asset in ("scuba", "free_runners"):
        write(assets / "equipment" / f"{asset}.json", {"layers": {"humanoid": [{"texture": f"{MOD}:{asset}"}]}})
    lang[f"tooltip.{MOD}.oxygen"] = "Oxygen: %s / %s mB"
    lang[f"tooltip.{MOD}.scuba_tank"] = "Use on a tank or gas holder of oxygen to fill. Wear it with the scuba mask."
    for style, info in ARMOR_STYLES.items():
        for piece in ARMOR:
            lang[f"tooltip.{MOD}.{style}_{piece}"] = info["lore"]
        lang[f"tooltip.{MOD}.{info['template']}"] = info["template_tooltip"]
    for trait, (name, text) in TRAITS.items():
        lang[f"tooltip.{MOD}.thallite.{trait}.trait"] = name
        lang[f"tooltip.{MOD}.thallite.{trait}"] = text

    recipes = data / "recipe"
    for tier, info in GEAR_TIERS.items():
        for piece in PIECES:
            key = {"#": info["ingot"]}
            if any("S" in row for row in PATTERNS[piece]):
                key["S"] = "minecraft:stick"
            category = "equipment"
            write(recipes / f"{tier}_{piece}.json", {
                "fabric:load_conditions": condition(info["feature"]), "type": "minecraft:crafting_shaped",
                "category": category, "pattern": PATTERNS[piece], "key": key,
                "result": {"id": f"{MOD}:{tier}_{piece}", "count": 1}})
    # Styled armor: the template at a crafting table; then at a smithing table the template, a plain piece and the
    # addition dress the piece, and (if reversible) the template, the styled piece and the metal's ingot undress it.
    # Smithing keeps the piece's enchantments, wear, name, trim and plating.
    for style, info in ARMOR_STYLES.items():
        metal = info["metal"]
        template = info["template"]
        pattern, key = info["template_recipe"]
        write(recipes / f"{template}.json", {
            "fabric:load_conditions": condition(feature(template)), "type": "minecraft:crafting_shaped",
            "category": "misc", "pattern": pattern, "key": key,
            "result": {"id": f"{MOD}:{template}", "count": info["template_count"]}})
        for piece in ARMOR:
            styled, plain = f"{style}_{piece}", f"{metal}_{piece}"
            write(recipes / f"{styled}.json", {
                "fabric:load_conditions": condition(feature(styled)), "type": "minecraft:smithing_transform",
                "template": f"{MOD}:{template}", "base": f"{MOD}:{plain}", "addition": info["addition"],
                "result": {"id": f"{MOD}:{styled}"}})
            if info["reversible"]:
                write(recipes / f"{plain}_from_{styled}.json", {
                    "fabric:load_conditions": condition(feature(plain)), "type": "minecraft:smithing_transform",
                    "template": f"{MOD}:{template}", "base": f"{MOD}:{styled}", "addition": GEAR_TIERS[metal]["ingot"],
                    "result": {"id": f"{MOD}:{plain}"}})
    for item, info in EXTRAS.items():
        write(recipes / f"{item}.json", {
            "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped",
            "category": "equipment", "pattern": info["pattern"], "key": info["key"],
            "result": {"id": f"{MOD}:{item}", "count": 1}})
    for tier in PAXEL_TIERS:
        write(recipes / f"{tier}_paxel.json", {
            "fabric:load_conditions": condition(feature(f"{tier}_paxel")), "type": "minecraft:crafting_shapeless",
            "category": "equipment",
            "ingredients": [tool_ingredient(tier, tool) for tool in ("pickaxe", "axe", "shovel")],
            "result": {"id": f"{MOD}:{tier}_paxel", "count": 1}})

    tags = data.parent / "minecraft" / "tags" / "item"
    by_tag = {}
    for tier in GEAR_TIERS:
        for piece in PIECES:
            by_tag.setdefault(ITEM_TAGS[piece], []).append(f"{MOD}:{tier}_{piece}")
    for style in ARMOR_STYLES:  # styled armor is armor of its slot, so it enchants, trims and equips as the plain piece
        for piece in ARMOR:
            by_tag[ITEM_TAGS[piece]].append(f"{MOD}:{style}_{piece}")
    for tier in ARMOR_TIERS:  # so the armor-only tiers enchant and equip as any armor of their slot
        for piece in ARMOR:
            by_tag[ITEM_TAGS[piece]].append(f"{MOD}:{tier}_{piece}")
    for tier in PAXEL_TIERS:
        for tag in ("pickaxes", "axes", "shovels"):
            by_tag.setdefault(tag, []).append(f"{MOD}:{tier}_paxel")
    by_tag["head_armor"].append(f"{MOD}:scuba_mask")
    by_tag["head_armor"].append(f"{MOD}:gas_mask")  # batch 31, tools/field_chemistry.py
    by_tag["chest_armor"].append(f"{MOD}:scuba_tank")
    by_tag["foot_armor"].append(f"{MOD}:free_runners")
    import nuclear  # the hazmat suit (tools/nuclear.py)
    for tag, item in nuclear.HAZMAT_TAGS.items():
        by_tag[tag].append(f"{MOD}:{item}")
    # The two fire sets (tools/concordance_ember.py ARMOR): armor of their slots, so they enchant and equip as any armor.
    import concordance_ember
    for item, piece in concordance_ember.ARMOR.items():
        by_tag[ITEM_TAGS[piece]].append(f"{MOD}:{item}")
    by_tag["swords"].append(f"{MOD}:power_katana")
    # Fall addition 23's silver dagger is a sword too. It is added here, not by tools/werewolf_data.py's tags, because
    # the shared tag writer replaces a whole file and would drop every sword above.
    from agriculture import WEREWOLF
    by_tag["swords"].append(f"{MOD}:{WEREWOLF['dagger']}")
    by_tag.setdefault("enchantable/bow", []).append(f"{MOD}:power_bow")
    import exosuit
    for tag, values in exosuit.item_tags().items():
        by_tag.setdefault(tag, []).extend(values)
    import arms  # batch 42: the arms join swords, spears and the enchantable tags
    for tag, values in arms.item_tags().items():
        by_tag.setdefault(tag, []).extend(values)
    import arms_variants  # batch 56: each variant joins its kind's tags
    for tag, values in arms_variants.item_tags().items():
        by_tag.setdefault(tag, []).extend(values)
    # One writer owns shared vanilla equipment tags. The later kitchen tag pass
    # must not replace every mace, shield and variant with only its knives.
    from kitchen import KNIVES
    for tag in ("enchantable/melee_weapon", "enchantable/durability"):
        by_tag.setdefault(tag, []).extend(f"{MOD}:{knife}" for knife in KNIVES)
    import concordance_dark_matter
    for tag, values in concordance_dark_matter.item_tags().items():
        by_tag.setdefault(tag, []).extend(values)
    import concordance_ee_equipment, concordance_ee_artifacts
    import nouveau_content
    for module in (concordance_ee_equipment, concordance_ee_artifacts, nouveau_content):
        for tag, values in module.item_tags().items():
            by_tag.setdefault(tag, []).extend(values)
    for tag, values in by_tag.items():
        write(tags / f"{tag}.json", {"replace": False, "values": values})
    for tier, info in GEAR_TIERS.items():
        write(data / "tags" / "item" / f"repairs_{tier}_gear.json", {"values": [info["ingot"]]})
    for tier, info in ARMOR_TIERS.items():
        write(data / "tags" / "item" / f"repairs_{tier}_gear.json", {"values": [info["repair"]]})
    write(data / "tags" / "item" / "repairs_rubber_gear.json", {"values": [f"{MOD}:rubber"]})
    write(data / "tags" / "block" / "mineable" / "paxel.json", {"values": [
        "#minecraft:mineable/pickaxe", "#minecraft:mineable/axe", "#minecraft:mineable/shovel"]})
    write(data / "tags" / "item" / "thallite_gear.json", {"values": thallite_gear()})
    write(data / "tags" / "item" / "earthbound_armor.json", {"values": earthbound_armor()})
    write(data / "tags" / "block" / "living_ground.json", {"values": LIVING_GROUND})
    write(data / "tags" / "block" / "earthen_ground.json", {"values": EARTHEN_GROUND})
