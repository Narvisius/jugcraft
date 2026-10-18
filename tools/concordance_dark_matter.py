"""Owner-art alchemical fuel, covalence and Dark Matter tools. No additional machine or currency."""
MOD = "jugcraft"
def rid(name): return f"{MOD}:{name}"

BURN_TICKS = {"alchemical_coal": 6400, "mobius_fuel": 12800, "aeternalis_fuel": 25600}
TOOLS = {"dark_matter_pickaxe": "pickaxe", "dark_matter_hammer": "hammer", "dark_matter_axe": "axe"}
NAMES = {"low_covalence_dust": "Low Covalence Dust", "medium_covalence_dust": "Medium Covalence Dust",
         "high_covalence_dust": "High Covalence Dust", "alchemical_coal": "Alchemical Coal", "mobius_fuel": "Mobius Fuel",
         "aeternalis_fuel": "Aeternalis Fuel", "dark_matter": "Dark Matter", "dark_matter_pickaxe": "Dark Matter Pickaxe",
         "dark_matter_hammer": "Dark Matter Hammer", "dark_matter_axe": "Dark Matter Axe"}
TOOLTIPS = {
    "low_covalence_dust": "Binds four coal into one Alchemical Coal. Made from cobblestone and charcoal.",
    "medium_covalence_dust": "Binds two Alchemical Coal into Mobius Fuel. Made from iron and redstone.",
    "high_covalence_dust": "Binds Mobius into Aeternalis Fuel. Repairs Dark Matter tools in an anvil: up to 1,024 durability per dust.",
    "alchemical_coal": "Furnace fuel: 32 ordinary smelts. Also a material for Mobius Fuel.",
    "mobius_fuel": "Furnace fuel: 64 ordinary smelts. Also a material for Aeternalis Fuel.",
    "aeternalis_fuel": "Furnace fuel: 128 ordinary smelts. Combine eight around a diamond block to make Dark Matter.",
    "dark_matter": "A dense alchemical material for durable tools. Crafted at an ordinary crafting table.",
    "dark_matter_pickaxe": "Modes: single block, connected ore vein (up to 8 extra), or tunnel (2 extra). Netherite-tier mining; 4,096 durability.",
    "dark_matter_hammer": "Modes: single block or a 3x3 mining face. Netherite-tier mining; 4,096 durability.",
    "dark_matter_axe": "Modes: single block or fell (up to 16 connected logs of the same kind, upward). Netherite-tier tool; 4,096 durability.",
}
ITEMS = {key: {"name": name, "tooltip": TOOLTIPS[key]} for key, name in NAMES.items()}
OWNER_FILES = {
    **{f"textures/item/{tier}_covalence_dust.png": f"textures/item/covalence_dust/{tier}.png" for tier in ("low", "medium", "high")},
    "textures/item/alchemical_coal.png": "textures/item/fuels/alchemical_coal.png",
    "textures/item/mobius_fuel.png": "textures/item/fuels/mobius.png",
    "textures/item/aeternalis_fuel.png": "textures/item/fuels/aeternalis.png",
    "textures/item/dark_matter.png": "textures/item/matter/dark.png",
    **{f"textures/item/{name}.png": f"textures/item/dm_tools/{tool}.png" for name, tool in TOOLS.items()},
}

# Deliberately no reverse recipes and no equivalence values for magical fuels or tools.
SHAPELESS = {
    "low_covalence_dust": (["minecraft:cobblestone", "minecraft:charcoal"], 4),
    "medium_covalence_dust": (["minecraft:iron_ingot", "minecraft:redstone"], 4),
    "high_covalence_dust": (["minecraft:diamond", "minecraft:lapis_lazuli"], 4),
    "alchemical_coal": (["minecraft:coal"] * 4 + [rid("low_covalence_dust")], 1),
    "mobius_fuel": ([rid("alchemical_coal")] * 2 + [rid("medium_covalence_dust")], 1),
    "aeternalis_fuel": ([rid("mobius_fuel")] * 2 + [rid("high_covalence_dust")], 1),
}
SHAPED = {
    "dark_matter": (["FFF", "FDF", "FFF"], {"F": rid("aeternalis_fuel"), "D": "minecraft:diamond_block"}),
    "dark_matter_pickaxe": (["MMM", " S ", " S "], {"M": rid("dark_matter"), "S": "minecraft:stick"}),
    "dark_matter_hammer": (["MMM", "MSM", " S "], {"M": rid("dark_matter"), "S": "minecraft:stick"}),
    "dark_matter_axe": (["MM ", "MS ", " S "], {"M": rid("dark_matter"), "S": "minecraft:stick"}),
}
LANG = {
    "message.jugcraft.dark_matter.mode": "%s: %s. Extra blocks cost %s essence each.",
    "message.jugcraft.dark_matter.mode.single": "Single block",
    "message.jugcraft.dark_matter.mode.vein": "Ore vein",
    "message.jugcraft.dark_matter.mode.tunnel": "Short tunnel",
    "message.jugcraft.dark_matter.mode.face": "3x3 face",
    "message.jugcraft.dark_matter.mode.fell": "Fell logs",
    "tooltip.jugcraft.dark_matter.mode": "Selected: %s",
    "tooltip.jugcraft.dark_matter.controls": "Use in air (or sneak-use a block): next mode. Sneak while mining: one block. Extra breaks cost %s essence each and spend essence; ordinary mining is free.",
}

def codex():
    return {("equivalence", "dark_matter"): {
        "name": "Dark Matter Workshop", "x": 4, "y": 0, "icon": rid("dark_matter_pickaxe"), "condition": None,
        "description": "Alchemical fuels, covalence and tools powered by essence",
        "pages": [
            ("text", "A Working Alchemist", "Use an ordinary **crafting table**, furnace and anvil. These materials need no new machine and no spell casting. Crafting and single-block mining are available immediately; tool modes use the same essence balance as the tablet, without research. Dissolve surplus plain materials in the tablet to supply it."),
            ("crafting_recipe", "Low Covalence", "Cobblestone and charcoal make four dust. One dust binds four coal into one Alchemical Coal.", rid("low_covalence_dust")),
            ("crafting_recipe", "Medium Covalence", "Iron and redstone make four dust. One dust binds two Alchemical Coal into one Mobius Fuel.", rid("medium_covalence_dust")),
            ("crafting_recipe", "High Covalence", "Diamond and lapis make four dust. One dust binds two Mobius Fuel into one Aeternalis Fuel. In an **anvil**, one dust also repairs up to **1,024 durability** on a Dark Matter tool, with the usual XP cost. Existing repair enchantments and the Repair Talisman still work.", rid("high_covalence_dust")),
            ("crafting_recipe", "Alchemical Coal", "Burns for **32 ordinary smelts**, at normal speed. The four input coal have the same total burn time; condensation saves storage space, not fuel energy. Dust is consumed.", rid("alchemical_coal")),
            ("crafting_recipe", "Mobius Fuel", "Burns for **64 ordinary smelts**, at normal speed. Two Alchemical Coal plus Medium Covalence Dust yield one fuel.", rid("mobius_fuel")),
            ("crafting_recipe", "Aeternalis Fuel", "Burns for **128 ordinary smelts**, at normal speed. Two Mobius Fuel plus High Covalence Dust yield one fuel. It also feeds the Dark Matter recipe.", rid("aeternalis_fuel")),
            ("crafting_recipe", "Dark Matter", "Surround a diamond block with eight Aeternalis Fuel. The result is a crafting material, not spendable essence. Fuels, dust, Dark Matter and its tools cannot be dissolved or formed; the tablet explains that they are beyond ordinary equivalence.", rid("dark_matter")),
            ("crafting_recipe", "Pickaxe", "**Single block**, **Ore vein**, **Short tunnel**. Use in air to cycle, or sneak-use a block. Vein follows face-connected blocks of the same ore, at most **8 extra** within four blocks of the first. Tunnel mines the next **2 blocks** in the direction you face, stopping at an obstacle. Extra breaks cost **8 essence** each.", rid("dark_matter_pickaxe")),
            ("crafting_recipe", "Hammer", "**Single block** or **3x3 face**, perpendicular to the direction you face (horizontal when looking steeply up/down). At most **8 extra blocks**, **16 essence** each. Start at the centre of the intended face.", rid("dark_matter_hammer")),
            ("crafting_recipe", "Axe", "**Single block** or **Fell logs**. Follows connected logs of the same block type sideways/upward, including upward diagonals, at most **16 extra**, within four blocks sideways and sixteen upwards. Roots below the first block and leaves are kept. Extra breaks cost **8 essence** each. It can cut placed logs too: sneak for precision.", rid("dark_matter_axe")),
            ("text", "Control and Upkeep", "Tools start in **Single block**. **Sneak while mining** temporarily limits any mode to one block. Every broken block still uses durability and ordinary loot/enchantment rules. Extra blocks must suit the tool and be no harder than the starting block. Containers, block entities, fluid-bearing blocks, protected blocks and unloaded chunks are skipped. Insufficient essence stops extra work; ordinary mining still works. The first block costs no essence. Modes are saved on the tool."),
        ]}}

def write_all(write, assets, data, lang, condition, self_drop):
    lang.update(LANG)
    for name in ITEMS:
        lang[f"tooltip.{MOD}.{name}"] = TOOLTIPS[name]
        write(assets / "models" / "item" / f"{name}.json", {"parent": "minecraft:item/handheld" if name in TOOLS else "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})
    for name, (ingredients, count) in SHAPELESS.items():
        write(data / "recipe" / f"{name}.json", {"fabric:load_conditions": condition("concordance"), "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients, "result": {"id": rid(name), "count": count}})
    for name, (pattern, key) in SHAPED.items():
        write(data / "recipe" / f"{name}.json", {"fabric:load_conditions": condition("concordance"), "type": "minecraft:crafting_shaped", "category": "equipment" if name in TOOLS else "misc", "pattern": pattern, "key": key, "result": {"id": rid(name), "count": 1}})

def item_tags():
    """Shared vanilla equipment tags are merged by gear.write_all's sole writer."""
    result = {tag: [rid(name) for name in TOOLS] for tag in (
        "enchantable/durability", "enchantable/mining", "enchantable/mining_loot", "enchantable/vanishing")}
    result["axes"] = [rid("dark_matter_axe")]
    result["pickaxes"] = [rid("dark_matter_pickaxe"), rid("dark_matter_hammer")]
    return result

def tags(tags):
    for name in ITEMS: tags.add("item", rid("equivalence/excluded"), rid(name))
    tags.add("item", rid("repairs_dark_matter_tools"), rid("high_covalence_dust"))
    for name in TOOLS:
        tags.add("item", "c:tools", rid(name))
