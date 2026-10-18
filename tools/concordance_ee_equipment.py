"""Owner-library Dark/Red Matter equipment, Gem armor, and lossless material storage."""
import json
from pathlib import Path

MOD = "jugcraft"
def rid(name): return f"{MOD}:{name}"

TOOLS = {
    **{f"dark_matter_{kind}": ("dm", kind) for kind in ("sword", "shovel", "hoe", "shears")},
    **{f"red_matter_{kind}": ("rm", kind) for kind in ("pickaxe", "axe", "shovel", "sword", "hoe", "shears", "hammer", "katar", "morning_star")},
}
ARMOR = {f"{tier}_{piece}": (tier, piece) for tier in ("dark_matter", "red_matter", "gem") for piece in ("helmet", "chestplate", "leggings", "boots")}
BLOCKS = {f"{material}_block": {"name":f"{material}_block".replace("_", " ").title()}
          for material in ("dark_matter", "red_matter", "alchemical_coal", "mobius_fuel", "aeternalis_fuel")}
NAMES = {name: name.replace("_", " ").title() for name in (*TOOLS, *ARMOR, "red_matter", "iron_band", *BLOCKS)}
TOOLTIPS = {
    "red_matter": "Six Aeternalis Fuel and three Dark Matter form a stronger alchemical crafting material.",
    "iron_band": "An iron setting for alchemical rings and devices. Made at an ordinary crafting table.",
    **{name: "Nine units stored losslessly. Craft back into nine materials." for name in BLOCKS},
}
for name, (tier, kind) in TOOLS.items():
    ability = {
        "pickaxe": "Ore vein (16 extra) or a five-block tunnel.", "axe": "Fell up to 24 connected logs, never below the first block.",
        "shovel": "Mine a 3x3 face of soil; ordinary use makes paths.", "hoe": "Cultivate a 3x3 patch with ordinary hoe rules.",
        "shears": f"Shear up to six nearby sheep, or {16 if tier == 'rm' else 8} connected leaves.",
        "sword": "Sweep hits up to four nearby hostile creatures; never pets or players.",
        "hammer": "Mine a 3x3 face of stone or ore.", "katar": "Axe, hoe and shears mining; modes fell logs or sweep hostile creatures. Strips, tills and shears sheep.",
        "morning_star": "Pickaxe and shovel mining; modes 3x3 face or five-block tunnel.",
    }[kind]
    TOOLTIPS[name] = f"{ability} {'8,192' if tier == 'rm' else '4,096'} durability. Repairs with High Covalence Dust."
for name, (tier, piece) in ARMOR.items():
    text = {"dark_matter": "Full enabled suit: Resistance I, 8 essence per renewal.",
            "red_matter": "Full enabled suit: Resistance I (12 essence); Fire Resistance while burning (8 essence).",
            "gem": {"helmet": "Water Breathing while submerged: 4 essence per renewal.",
                    "chestplate": "Fire Resistance while burning: 8 essence per renewal.",
                    "leggings": "Speed I while sprinting: 4 essence per renewal.",
                    "boots": "Slow Falling after a two-block fall: 4 essence per renewal."}[piece] + " Full enabled suit: Resistance I (12 essence)."}[tier]
    TOOLTIPS[name] = text + " Enable wards to spend essence; ordinary protection is always available."

ITEMS = {name: {"name": NAMES[name], "tooltip": TOOLTIPS[name]} for name in NAMES if name not in BLOCKS}
OWNER_FILES = {
    **{f"textures/item/{name}.png": f"textures/item/{tier}_tools/{kind}.png" for name, (tier, kind) in TOOLS.items()},
    **{f"textures/item/{name}.png": f"textures/item/{dict(dark_matter='dm', red_matter='rm', gem='gem')[tier]}_armor/{dict(helmet='head', chestplate='chest', leggings='legs', boots='feet')[piece]}.png" for name, (tier, piece) in ARMOR.items()},
    **{f"textures/entity/equipment/{layer}/alchemy/{tier}.png": f"textures/models/armor/{'gem_armor' if tier == 'gem' else tier}_layer_{index}.png"
       for tier in ("dark_matter", "red_matter", "gem") for index, layer in ((1, "humanoid"), (2, "humanoid_leggings"))},
    "textures/item/red_matter.png": "textures/item/matter/red.png",
    "textures/item/iron_band.png": "textures/item/rings/iron_band.png",
    **{f"textures/block/{name}.png": f"textures/block/{name}.png" for name in BLOCKS},
}
LANG = {
    "message.jugcraft.extended_matter.mode": "%s: %s. Extra work costs %s essence per target.",
    "tooltip.jugcraft.extended_matter.controls": "%s | %s essence per extra. Use in air or sneak-use a block to change mode; sneak while mining for precision. No research required.",
    **{f"message.jugcraft.extended_matter.mode.{key}": value for key, value in {
        "single":"Single target", "vein":"Ore vein", "tunnel":"Five-block tunnel", "fell":"Fell logs", "face":"3x3 face",
        "cultivate":"3x3 cultivation", "shear":"Flock / leaves", "sweep":"Hostile sweep"}.items()},
    "message.jugcraft.alchemy_armor.toggle": "%s wards: %s",
    "tooltip.jugcraft.alchemy_armor.controls": "Wards: %s. Sneak-use this piece in hand to toggle. Short wards expire after unequipping; stronger existing effects are preserved.",
}

def write_all(write, assets, data, lang, condition, self_drop):
    lang.update(LANG)
    for name in NAMES:
        lang[f"{'block' if name in BLOCKS else 'item'}.{MOD}.{name}"] = NAMES[name]
        lang[f"tooltip.{MOD}.{name}"] = TOOLTIPS[name]
    for name in ITEMS:
        model = {"parent": "minecraft:item/handheld" if name in TOOLS else "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}}
        write(assets / f"models/item/{name}.json", model)
        definition = {"type":"minecraft:model", "model":rid(f"item/{name}")}
        if name in ARMOR:
            tier, piece = ARMOR[name]
            prefix = {"dark_matter":"dm", "red_matter":"rm", "gem":"gem"}[tier]
            cases = []
            for trim in ("quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst"):
                source = Path(__file__).resolve().parents[1] / f"art/owner-library/originals/Magic/models/item/{prefix}_{piece}_{trim}_trim.json"
                imported = json.loads(source.read_text())
                imported.pop("overrides", None)
                imported["textures"]["layer0"] = rid(f"item/{name}")
                write(assets / f"models/item/{name}_{trim}_trim.json", imported)
                cases.append({"when":f"minecraft:{trim}", "model":{"type":"minecraft:model", "model":rid(f"item/{name}_{trim}_trim")}})
            definition = {"type":"minecraft:select", "property":"minecraft:trim_material", "cases":cases, "fallback":definition}
        write(assets / f"items/{name}.json", {"model":definition})
    for tier in ("dark_matter", "red_matter", "gem"):
        write(assets / f"equipment/alchemy/{tier}.json", {"layers":{layer:[{"texture":rid(f"alchemy/{tier}")}] for layer in ("humanoid", "humanoid_leggings")}})
    for name in BLOCKS:
        write(assets / f"models/block/{name}.json", {"parent":"minecraft:block/cube_all", "textures":{"all":rid(f"block/{name}")}})
        write(assets / f"blockstates/{name}.json", {"variants":{"":{"model":rid(f"block/{name}")}}})
        write(assets / f"models/item/{name}.json", {"parent":rid(f"block/{name}")})
        write(assets / f"items/{name}.json", {"model":{"type":"minecraft:model", "model":rid(f"item/{name}")}})
        write(data / f"loot_table/blocks/{name}.json", self_drop(name))
    def shaped(name, pattern, key, count=1):
        write(data / f"recipe/{name}.json", {"fabric:load_conditions":condition("concordance"), "type":"minecraft:crafting_shaped", "category":"equipment" if name in TOOLS or name in ARMOR else "misc", "pattern":pattern, "key":key, "result":{"id":rid(name),"count":count}})
    def shapeless(filename, result, ingredients, count=1):
        write(data / f"recipe/{filename}.json", {"fabric:load_conditions":condition("concordance"), "type":"minecraft:crafting_shapeless", "category":"misc", "ingredients":ingredients, "result":{"id":rid(result),"count":count}})
    def upgrade(name, base, addition):
        write(data / f"recipe/{name}.json", {"fabric:load_conditions":condition("concordance"), "type":"minecraft:smithing_transform", "template":rid("high_covalence_dust"), "base":rid(base), "addition":addition, "result":{"id":rid(name),"count":1}})
    shaped("red_matter", ["FFF", "DDD", "FFF"], {"F":rid("aeternalis_fuel"),"D":rid("dark_matter")})
    shaped("iron_band", ["III", "I I", "III"], {"I":"minecraft:iron_ingot"})
    for name in BLOCKS:
        material = name.removesuffix("_block")
        shaped(name, ["MMM", "MMM", "MMM"], {"M":rid(material)})
        shapeless(f"{material}_from_storage", material, [rid(name)], 9)
    for kind, pattern in {"sword":[" M ", " M ", " S "], "shovel":[" M ", " S ", " S "], "hoe":["MM ", " S ", " S "], "shears":[" M", "M "]}.items():
        ingredients = {"M":rid("dark_matter")}
        if kind != "shears": ingredients["S"] = "minecraft:stick"
        shaped(f"dark_matter_{kind}", pattern, ingredients)
    for kind in ("pickaxe", "axe", "shovel", "sword", "hoe", "shears", "hammer"):
        upgrade(f"red_matter_{kind}", f"dark_matter_{kind}", rid("red_matter"))
    shapeless("red_matter_katar", "red_matter_katar", [rid(f"red_matter_{kind}") for kind in ("axe", "sword", "hoe", "shears")] + [rid("red_matter")] * 4 + [rid("high_covalence_dust")])
    shapeless("red_matter_morning_star", "red_matter_morning_star", [rid(f"red_matter_{kind}") for kind in ("pickaxe", "shovel", "hammer")] + [rid("red_matter")] * 4 + [rid("iron_band"), rid("high_covalence_dust")])
    for piece, pattern in {"helmet":["MMM", "M M"], "chestplate":["M M", "MMM", "MMM"], "leggings":["MMM", "M M", "M M"], "boots":["M M", "M M"]}.items():
        shaped(f"dark_matter_{piece}", pattern, {"M":rid("dark_matter")})
        upgrade(f"red_matter_{piece}", f"dark_matter_{piece}", rid("red_matter"))
        upgrade(f"gem_{piece}", f"red_matter_{piece}", "minecraft:diamond_block")

def item_tags():
    result = {}
    def add(tag, names): result.setdefault(tag, []).extend(rid(name) for name in names)
    for tag in ("enchantable/durability", "enchantable/vanishing"): add(tag, (*TOOLS, *ARMOR))
    for tag in ("enchantable/mining", "enchantable/mining_loot"): add(tag, [name for name in TOOLS if not name.endswith("sword")])
    for kind, tag in (("sword","swords"), ("pickaxe","pickaxes"), ("axe","axes"), ("shovel","shovels"), ("hoe","hoes")):
        add(tag, [name for name, (_, exact) in TOOLS.items() if exact == kind])
    add("pickaxes", ("red_matter_hammer", "red_matter_morning_star")); add("shovels", ("red_matter_morning_star",))
    add("axes", ("red_matter_katar",)); add("swords", ("red_matter_katar",)); add("hoes", ("red_matter_katar",))
    for tag in ("enchantable/sharp_weapon", "enchantable/weapon", "enchantable/sword", "enchantable/fire_aspect"):
        add(tag, ("dark_matter_sword", "red_matter_sword", "red_matter_katar"))
    for tag in ("enchantable/armor", "enchantable/equippable", "trimmable_armor"): add(tag, ARMOR)
    for piece, tag in (("helmet", "head"), ("chestplate", "chest"), ("leggings", "leg"), ("boots", "foot")):
        names = [name for name, (_, part) in ARMOR.items() if part == piece]
        add(f"enchantable/{tag}_armor", names)
        add(f"{tag}_armor", names)
    return result

def tags(tags):
    for name in NAMES: tags.add("item", rid("equivalence/excluded"), rid(name))
    for name in TOOLS: tags.add("item", "c:tools", rid(name))
    for name in ("dark_matter_shears", "red_matter_shears", "red_matter_katar"): tags.add("item", "c:tools/shear", rid(name))
    for name in BLOCKS:
        tags.add("block", "minecraft:mineable/pickaxe", rid(name))
        tags.add("block", "minecraft:needs_iron_tool", rid(name))
    for group in ("red_matter_tools", "dark_matter_armor", "red_matter_armor", "gem_armor"):
        tags.add("item", rid(f"repairs_{group}"), rid("high_covalence_dust"))
    for source in ("axe", "hoe"): tags.add("block", rid("mineable/alchemical_katar"), f"#minecraft:mineable/{source}")
    tags.add("block", rid("mineable/alchemical_katar"), "#minecraft:leaves")
    for source in ("pickaxe", "shovel"): tags.add("block", rid("mineable/alchemical_morning_star"), f"#minecraft:mineable/{source}")

def codex():
    return {("equivalence", "matter_equipment"): {
        "name":"Matter Equipment", "x":10, "y":0, "icon":rid("red_matter_katar"), "condition":None,
        "description":"Durable tools, protective suits and compact alchemical materials",
        "pages":[
            ("crafting_recipe", "Red Matter", "Combine **three Dark Matter** and **six Aeternalis Fuel**. Upgrade each Dark Matter tool or armor piece at a **smithing table**: High Covalence Dust template, old equipment base, Red Matter addition. Names, damage and enchantments survive. Red tools have **8,192 durability** and netherite-tier harvest. Ordinary use costs no essence.", rid("red_matter")),
            ("text", "Tools and Modes", "Use in air, or sneak-use a block, to change mode. **Sneak while mining** for precision. Pickaxe: sixteen connected ores or four more blocks in a tunnel. Axe: twenty-four logs, upward only. Hammer: a 3x3 face. Shovel: 3x3 earth and ordinary path making. Hoe: 3x3 cultivation. Shears: six extra sheep or eight/sixteen connected leaves. Paid work uses the personal essence ledger or carried Klein Stars, loaded chunks and permission. No research is required."),
            ("crafting_recipe", "Red Katar", "Combine the four Red Matter sword/axe/hoe/shears with four Red Matter and High Covalence Dust. Mines axe/hoe blocks and leaves; strips logs, tills soil, shears sheep, and selects felling or hostile sweep. The combination is ordinary crafting: ingredient enchantments and names are not transferred. **16 essence** per extra log or hostile creature.", rid("red_matter_katar")),
            ("crafting_recipe", "Red Morning Star", "Combine Red Matter pickaxe/shovel/hammer, four Red Matter, an Iron Band and High Covalence Dust. Mines pickaxe and shovel materials; makes paths. Select a **3x3 face** or **five-block tunnel**, costing **12 essence** per extra block. Ingredient enchantments and names do not transfer through this crafting recipe.", rid("red_matter_morning_star")),
            ("text", "Safe Extra Work", "Single hits/breaks retain normal loot, durability and protection. Extra blocks never include block entities, fluid-bearing blocks, harder materials or protected terrain. An unaffordable operation stops before spending. Most tools cost **8 essence** per extra; hammer and Morning Star **12**; sword and Katar **16**. Sword sweep affects up to **four nearby hostile mobs** for two/four damage and uses the shared Concordance effect boundary. Pets, villagers and players are never swept."),
            ("crafting_recipe", "Dark Matter Armor", "Craft the ordinary armor shapes with Dark Matter. A full enabled suit supplies short **Resistance I** wards, costing **8 essence** each renewal. Red Matter suits cost **12** for resistance and **8** for Fire Resistance while burning. Protection remains ordinary damageable armor even with an empty ledger. Repair each piece in an anvil with High Covalence Dust.", rid("dark_matter_chestplate")),
            ("crafting_recipe", "Gem Armor", "Smith a Red Matter piece with a **diamond block** and High Covalence Dust. Helmet: Water Breathing underwater (**4 essence**). Chestplate: Fire Resistance when burning (**8**). Leggings: Speed I when sprinting (**4**). Boots: Slow Falling after falling two blocks (**4**). Full enabled suit: Resistance I (**12**). Wards last three seconds; checked once a second, they renew only at two seconds or less. Stronger effects are kept. Sneak-use a piece in hand to disable its ward before wearing it. Open **Gem abilities** in the tablet or Manual for paid night vision, repulsion, dash and flight; the boots share the existing flight lease.", rid("gem_chestplate")),
            ("crafting_recipe", "Material Storage", "Craft nine Dark Matter, Red Matter or any alchemical fuel into its storage block; craft back to exactly nine. Fuel blocks burn for exactly nine times the fuel's duration. These materials, tools and armor are excluded from transmutation: no charged equipment or magical matter can become an essence duplication loop.", rid("red_matter_block")),
        ]}}
