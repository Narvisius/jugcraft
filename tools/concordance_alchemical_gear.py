"""Owner's EE-inspired utility gear; original implementation over the existing bounded Assay ledger."""
MOD = "jugcraft"
def rid(path): return f"{MOD}:{path}"

ITEMS = {"philosophers_stone": {"name": "Philosopher's Stone"},
         "repair_talisman": {"name": "Repair Talisman"}, "black_hole_band": {"name": "Black Hole Band"}}
OWNER_FILES = {
    "textures/gui/alchemy/transmutation.png": "textures/gui/transmute.png",
    "textures/item/philosophers_stone.png": "textures/item/philosophers_stone.png",
    "textures/item/repair_talisman.png": "textures/item/repair_talisman.png",
    "textures/item/black_hole_band.png": "textures/item/rings/black_hole_off.png",
    "textures/item/black_hole_band_active.png": "textures/item/rings/black_hole_on.png",
}
TOOLTIPS = {
    "philosophers_stone": "Use: open the transmutation tablet. Dissolve plain matter; keep a sample to form more. No research required.",
    "repair_talisman": "Use: toggle. Hold or wear in a Charm slot. Mends up to 4 held-tool / worn-armor durability each second; 1 essence per point. No research required.",
    "black_hole_band": "Use: toggle. Hold or wear in a Ring slot. Attracts nearby loose items within 6 blocks; 1 essence per item entity moved. No research required.",
}
for _item, _info in ITEMS.items():
    _info["tooltip"] = TOOLTIPS[_item]

RECIPES = {
    "philosophers_stone": {"pattern": ["RGR", "GSG", "RGR"], "key": {"R": "minecraft:redstone", "G": "minecraft:gold_ingot", "S": rid("assayers_scale")}},
    "repair_talisman": {"pattern": [" L ", "ISI", " R "], "key": {"L": "minecraft:lapis_lazuli", "I": "minecraft:iron_ingot", "S": "minecraft:string", "R": "minecraft:redstone"}},
    "black_hole_band": {"pattern": ["GOG", "OEO", "GOG"], "key": {"G": "minecraft:gold_ingot", "O": "minecraft:obsidian", "E": "minecraft:ender_pearl"}},
}
UI = {
    "title": "Transmutation Tablet", "balance": "%s essence", "input": "Dissolve items", "sample": "Sample", "output": "Formed",
    "gain": "+%s essence", "cost": "%s essence / item", "inventory": "Your inventory", "sample_kept": "Sample (kept)",
    "dissolve": "Dissolve stack", "confirm": "Confirm dissolve", "form": "Form one", "on": "Active", "off": "Inactive",
    "dissolve_help": "Consumes the whole input stack for the displayed essence. Click again within 10 seconds to confirm. Changing the stack cancels confirmation.",
    "input_help": "Insert plain catalogued matter. Named or modified items are refused. Your ledger must have room for the whole stack (maximum 2,000,000,000 essence).",
    "form_help": "Keep one sample here. Creates one copy for the displayed price, paid from your personal ledger.",
    "status_0": "Form one copy. The sample is kept; take the result from the output slot.",
    "status_1": "Place one sample in the sample slot.", "status_2": "This sample cannot be formed. Only plain catalogued mundane materials qualify.",
    "status_3": "Not enough essence. Dissolve spare materials first.", "status_4": "Take the formed items out first; the output is full or holds another material.",
}

def codex():
    return {("equivalence", "alchemical_gear"): {
        "name": "Alchemical Gear", "x": 2, "y": 0, "icon": rid("philosophers_stone"), "condition": None,
        "description": "Transmute, repair and gather without casting spells",
        "pages": [
            ("text", "Equipment, Not Spells", "Craft, find or trade for the equipment and use it immediately. No research, wand or spell cast is needed. Dissolved matter becomes **essence** in your personal **Prima Materia** balance. These tools spend that same balance, also used by the Assayer's Scale. This alchemical essence is separate from Principle essences in vials. No Focus, mana or spell inscription is involved."),
            ("crafting_recipe", "Philosopher's Stone", "Use to open the combined transmutation tablet. Insert an item, then **Dissolve** and **Confirm**: the input is consumed and its default item is learned. Click a learned icon, choose **1 / 16 / 64 / Max**, then **Withdraw** at its essence value with no surcharge. Search and favorites help find learned items. Default magical gear is valued too; empty containers and reset charge, names or other custom state before dissolving. Real input/output items return when the tablet closes. Sneak-use air opens crafting; use terrain transforms it, and sneak-use terrain cycles the area.", rid("philosophers_stone")),
            ("crafting_recipe", "Repair Talisman", "Use to toggle **Active**, then hold it or equip it in a **Charm** slot. Every second it repairs up to **4 durability** on one held tool or worn armor piece, for **1 essence per point**. Main hand, offhand, then head-to-feet armor are checked in order. Only damaged items with normal repair ingredients qualify. Enchantments and names are preserved. An active talisman inside a carried Alchemical Bag or owned Alchemical Chest can also repair stored equipment while closed.", rid("repair_talisman")),
            ("crafting_recipe", "Black Hole Band", "Use to toggle **Active**, then hold it or equip it in a **Ring** slot. Every second it draws up to **8 item entities** within **6 blocks** toward you, costing **1 essence for each moved entity**. It stops at solid walls, respects loot ownership and claims, and pauses when your inventory cannot accept the item. Minecraft's ordinary pickup collects it. An active band inside Alchemical storage inserts drops matching a stored item; on a pedestal it inserts into adjacent storage. Mobs are unaffected.", rid("black_hole_band")),
            ("text", "Switches and Upkeep", "Both passive tools start off. Use again to switch off; the message shows your balance. Active artifacts inside an Alchemical Bag or Chest can repair or collect matching stored item types; ordinary backpacks do not supply that feature. Duplicates do not multiply the effect. Empty balance, missing Assay knowledge or disabled Concordance stops all work. Trade the crafted equipment or formed materials; the ledger stays personal."),
        ]}}

def write_all(write, assets, data, lang, condition, self_drop):
    for key, text in UI.items(): lang[f"screen.{MOD}.alchemy.{key}"] = text
    lang[f"message.{MOD}.alchemy.toggle"] = "%s: %s · %s essence"
    for name in [*ITEMS, "black_hole_band_active"]:
        write(assets / "models" / "item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
    for name in ITEMS:
        model = {"type": "minecraft:model", "model": rid(f"item/{name}")}
        if name == "black_hole_band": model = {"type": "minecraft:condition", "property": "minecraft:has_component", "component": rid("alchemy_active"), "on_false": model, "on_true": {"type": "minecraft:model", "model": rid("item/black_hole_band_active")}}
        write(assets / "items" / f"{name}.json", {"model": model})
    for name, recipe in RECIPES.items():
        write(data / "recipe" / f"{name}.json", {"fabric:load_conditions": condition("concordance"), "type": "minecraft:crafting_shaped", "category": "equipment", **recipe, "result": {"id": rid(name), "count": 1}})
    # The shared tags() writer merges old and new wearable items in one pass.

def tags(tags):
    for item in ITEMS: tags.add("item", rid("equivalence/excluded"), rid(item))
    tags.add("item", "trinkets:hand/ring", rid("black_hole_band"))
    tags.add("item", "trinkets:legs/charm", rid("repair_talisman"))

# Owner-requested connected behavior supersedes the original manual-only descriptions.
from concordance_ee_expansion import TOOLTIPS as _connected_tooltips
TOOLTIPS.update({k:v for k,v in _connected_tooltips.items() if k in TOOLTIPS})
