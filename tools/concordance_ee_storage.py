"""Physical bags/chest, the existing tablet's portable and placed forms, and controlled utility artifacts."""
import json
from pathlib import Path
MOD = "jugcraft"
def rid(name): return f"{MOD}:{name}"
COLORS = ("white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black")
NAMES = {"alchemical_chest":"Alchemical Chest", "transmutation_table":"Transmutation Table", "transmutation_tablet":"Transmutation Tablet", "alchemy_manual":"Alchemical Manual", "alchemy_tome":"Tome of Material Equivalence", "gem_of_eternal_density":"Gem of Eternal Density", "void_ring":"Void Ring", "mind_stone":"Mind Stone", **{f"{color}_alchemical_bag":f"{color.replace('_',' ').title()} Alchemical Bag" for color in COLORS}}
BLOCKS = {key:{"name":NAMES[key]} for key in ("alchemical_chest","transmutation_table")}
ITEMS = {key:{"name":name} for key,name in NAMES.items() if key not in BLOCKS}
TOOLTIPS = {
 "alchemical_chest":"54 physical slots, shared with your party. Mining drops its contents. No hopper access.",
 "transmutation_table":"A placed essence tablet. Use within eight blocks where building is permitted. No research required.",
 "transmutation_tablet":"Use to dissolve materials into essence and form a retained sample. Same audited costs as the Philosopher's Stone.",
 "alchemy_manual":"Read six short alchemical reference sections. No research or resources are granted.",
 "alchemy_tome":"Browse the server's material catalogue and exact dissolve/form prices. Samples are read-only.",
 "gem_of_eternal_density":"Open deliberate material consolidation: dissolve eligible inputs, retain a target, and form it with your essence.",
 "void_ring":"Hold a plain material in your other hand. Use to preview; use again within ten seconds to dissolve it. Never runs automatically.",
 "mind_stone":"Use to store up to 100 experience points. Sneak-use to retrieve up to 100. Exact points, not levels.",
 "alchemical_bag":"54 physical slots in this bag only. No nested containers. Its inventory slot stays locked while open.",
}
for _color in COLORS: TOOLTIPS[f"{_color}_alchemical_bag"] = TOOLTIPS["alchemical_bag"]
_ITEM_SOURCES = {"transmutation_tablet":"transmutation_tablet", "alchemy_manual":"book", "alchemy_tome":"tome", "gem_of_eternal_density":"dense_gem_off", "void_ring":"rings/void_off", "mind_stone":"rings/mind_stone_off", **{f"{c}_alchemical_bag":f"alchemy_bags/{c}" for c in COLORS}}
OWNER_FILES = {f"textures/item/alchemy/{key}.png":f"textures/item/{source}.png" for key,source in _ITEM_SOURCES.items()}
OWNER_FILES.update({"textures/block/alchemy/alchemical_chest.png":"textures/block/alchemical_chest.png", **{f"textures/block/alchemy/transmutation_stone/{face}.png":f"textures/block/transmutation_stone/{face}.png" for face in ("top","bottom","side")}})
MANUAL = (
 ("Starting out","Craft, find or trade for alchemical equipment and use it immediately. Use a plain material sample to see its value. The table, tablet and Philosopher's Stone all share your existing essence balance."),
 ("Conservation","Dissolving consumes the input and teaches its default item. Withdraw learned items for their essence value, with no surcharge; batches round down on dissolution and up on withdrawal. Named, charged, filled and other modified stacks must be emptied/reset first."),
 ("Workshops","Daylight collectors supply condensers and Klein Stars. A condenser keeps a target and consumes only its marked inputs. Pause or redstone stops it. Loaded authorized factories use their own buffers while the owner is away."),
 ("Storage","Each colored bag carries its own 54 slots. Colors do not link inventories. Containers cannot nest. Your open bag stays locked in its slot. The chest grants access to its owner's party; mining spills contents."),
 ("Utilities","The density gem opens deliberate consolidation. The Void Ring previews the other hand's material and dissolves it only after a second use. Nothing automatically consumes your inventory."),
 ("Mind and study","Mind Stone: use stores 100 XP points; sneak-use retrieves 100, limited by your actual XP and its capacity. The Tome browses live catalogue prices. Neither book unlocks research or gives free samples."),
)
LANG = {
 "tooltip.jugcraft.alchemy_storage.xp":"Experience: %s / %s points",
 "tooltip.jugcraft.alchemy_storage.slots":"Occupied slots: %s / %s",
 "message.jugcraft.alchemy_storage.xp":"Moved %s XP points. Mind Stone: %s / %s.",
 "message.jugcraft.alchemy_storage.void_help":"Hold the material to dissolve in your other hand; use twice to confirm.",
 **{f"screen.jugcraft.alchemy_storage.{k}":v for k,v in {"no_nesting":"54 slots / no nested containers", "inventory":"Inventory / gold = locked bag", "previous":"Previous", "next":"Next", "catalogue_hint":"Select an item / Page %s of %s", "value":"Dissolve: %s   Form: %s", "read_only":"Reference only / no free items"}.items()},
 **{f"screen.jugcraft.alchemy_storage.manual.title_{i}":v[0] for i,v in enumerate(MANUAL)},
 **{f"screen.jugcraft.alchemy_storage.manual.body_{i}":v[1] for i,v in enumerate(MANUAL)},
 "screen.jugcraft.alchemy_storage.manual.close":"Close",
}
SHAPED = {
 "alchemical_chest":(["DHD","ICI","DLD"],{"D":rid("low_covalence_dust"),"H":rid("high_covalence_dust"),"I":"minecraft:iron_ingot","C":"minecraft:chest","L":rid("medium_covalence_dust")}),
 "transmutation_table":(["SSS","ODO","OOO"],{"S":"minecraft:stone","O":"minecraft:obsidian","D":rid("dark_matter")}),
 "transmutation_tablet":(["ODO","DTD","ODO"],{"O":"minecraft:obsidian","D":rid("dark_matter"),"T":rid("transmutation_table")}),
 "alchemy_manual":([" P "," B "," C "],{"P":"minecraft:paper","B":"minecraft:book","C":rid("low_covalence_dust")}),
 "alchemy_tome":(["LHL","BMB","LHL"],{"L":rid("low_covalence_dust"),"H":rid("high_covalence_dust"),"B":"minecraft:book","M":rid("alchemy_manual")}),
 "gem_of_eternal_density":(["ODO","DGD","ODO"],{"O":"minecraft:obsidian","D":rid("dark_matter"),"G":"minecraft:diamond"}),
 "void_ring":(["OGO","DBD","ORO"],{"O":"minecraft:obsidian","G":rid("gem_of_eternal_density"),"D":rid("dark_matter"),"B":rid("iron_band"),"R":rid("red_matter")}),
 "mind_stone":(["LGL","DND","LGL"],{"L":"minecraft:lapis_lazuli","G":"minecraft:gold_ingot","D":rid("dark_matter"),"N":"minecraft:amethyst_shard"}),
}
def write_all(write,assets,data,lang,condition,self_drop):
    lang.update(LANG)
    for key,name in NAMES.items():
        lang[f"{'block' if key in BLOCKS else 'item'}.{MOD}.{key}"]=name
        lang[f"tooltip.{MOD}.{key}"]=TOOLTIPS[key]
    lang["tooltip.jugcraft.alchemical_bag"]=TOOLTIPS["alchemical_bag"]
    for key in ITEMS:
        write(assets/f"models/item/{key}.json",{"parent":"minecraft:item/generated","textures":{"layer0":rid(f"item/alchemy/{key}")}})
        write(assets/f"items/{key}.json",{"model":{"type":"minecraft:model","model":rid(f"item/{key}")}})
    # Existing collectors module owns block/alchemical_chest.json as the common untextured chest geometry.
    # Keep that parent intact: the newly registered chest uses its own textured child.
    chest={"parent":rid("block/alchemical_chest"),"textures":{"chest":rid("block/alchemy/alchemical_chest"),"particle":rid("block/alchemy/alchemical_chest")}}
    write(assets/"models/block/alchemy_storage_chest.json",chest)
    source=Path(__file__).resolve().parents[1]/"art/owner-library/originals/Magic/models/block/transmutation_table.json"
    table=json.loads(source.read_text())
    table["textures"]={k:v.replace("projecte:block/transmutation_stone/","jugcraft:block/alchemy/transmutation_stone/") for k,v in table["textures"].items()}
    write(assets/"models/block/transmutation_table.json",table)
    for key in BLOCKS:
        model="alchemy_storage_chest" if key=="alchemical_chest" else key
        write(assets/f"blockstates/{key}.json",{"variants":{f"facing={face}":{"model":rid(f"block/{model}"),"y":angle} for face,angle in zip(("north","east","south","west"),(0,90,180,270))}})
        write(assets/f"models/item/{key}.json",{"parent":rid(f"block/{model}")})
        write(assets/f"items/{key}.json",{"model":{"type":"minecraft:model","model":rid(f"item/{key}")}})
        write(data/f"loot_table/blocks/{key}.json",self_drop(key))
    for color in COLORS:
        SHAPED[f"{color}_alchemical_bag"] = (["WWW","DCD","WWW"],{"W":f"minecraft:{color}_wool","D":rid("low_covalence_dust"),"C":rid("alchemical_chest")})
    for key,(pattern,ingredients) in SHAPED.items():
        write(data/f"recipe/{key}.json",{"fabric:load_conditions":condition("concordance"),"type":"minecraft:crafting_shaped","category":"misc","pattern":pattern,"key":ingredients,"result":{"id":rid(key),"count":1}})

def tags(tags):
    for key in BLOCKS:tags.add("block","minecraft:mineable/pickaxe",rid(key))
    for key in NAMES:tags.add("item",rid("equivalence/excluded"),rid(key))

def codex():
    return {("equivalence","storage_and_study"):{"name":"Storage and Study","x":14,"y":0,"icon":rid("alchemy_tome"),"condition":None,"description":"Physical storage and deliberate conversion",
      "pages":[
       ("crafting_recipe","A Physical Chest","The **Alchemical Chest** holds **54 slots** and opens for its owner and party. No hopper can bypass the party gate. Mining spills the real contents separately. Stored items remain physical; explicitly activated, filtered converters can dissolve the chosen plain material types while the chest is closed. No research check can strand existing chest contents.",rid("alchemical_chest")),
       ("crafting_recipe","Sixteen Bags","Craft any of sixteen wool colors around an Alchemical Chest and Low Covalence Dust. Each **bag owns its own 54 slots**, even when two bags share a color. Its inventory slot is locked while open and each transaction saves immediately to that bag. **No nested bags, bundles or container items**. Contents remain accessible if Concordance is disabled. There is no contents-losing recolor recipe.",rid("white_alchemical_bag")),
       ("crafting_recipe","Table and Tablet","Use the placed **Transmutation Table** nearby, or carry the **Transmutation Tablet**. Both are usable immediately, without research. Dissolve the input to teach its default item. Browse your learned collection, favorites and search; withdraw 1 / 16 / 64 / Max at its value, no surcharge. Moving away from a table or losing the held tablet closes access without losing its working slots.",rid("transmutation_table")),
       ("crafting_recipe","Read Before Spending","The **Alchemical Manual** has six short reference sections. The **Tome of Material Equivalence** browses the server's actual catalogue; click a read-only sample to see dissolution and formation prices. No free sample is delivered, and no research state is changed. Values are shared by all exchange devices.",rid("alchemy_tome")),
       ("crafting_recipe","Consolidation and Void","The **Void Ring** weighs the material held in your other hand; a second use within ten seconds confirms dissolution into your existing balance. Empty stored contents and reset modified stacks before dissolving. Density Gem now opens a nine-entry input filter and selected output tier. Sneak-use Void Ring to configure its optional automatic mode. Empty filters consume nothing. See Connected Alchemy for storage and carried automation.",rid("gem_of_eternal_density")),
       ("crafting_recipe","Mind Stone","Use to store **up to 100 actual XP points**; sneak-use returns up to 100. It holds **1,000,000 points** and stores them on the physical item. Transfer is limited by the real XP bar and capacity, never a whole-level exchange rate. Research and feature disable gates still apply. Like other possessions, a lost stone loses its stored XP.",rid("mind_stone")),
      ]}}

# Owner-requested connected behavior supersedes the original manual-only descriptions.
from concordance_ee_expansion import TOOLTIPS as _connected_tooltips
TOOLTIPS.update({k:v for k,v in _connected_tooltips.items() if k in TOOLTIPS})
