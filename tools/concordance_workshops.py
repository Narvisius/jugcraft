"""Bounded essence collectors/condensers; owner collector faces and condenser chest atlas are unchanged."""
import json
from pathlib import Path
MOD = "jugcraft"
def rid(name): return f"{MOD}:{name}"
NAMES = {**{f"collector_mk{i}": f"Essence Collector Mk {i}" for i in range(1,4)}, **{f"condenser_mk{i}": f"Essence Condenser Mk {i}" for i in range(1,3)}}
BLOCKS = {key: {"name": value} for key,value in NAMES.items()}
TOOLTIPS = {
    **{f"collector_mk{i}": f"Collects {rate} essence per second in strong daylight. Feeds touching condensers first, then a docked Klein Star." for i,rate in enumerate((1,4,16),1)},
    **{f"condenser_mk{i}": f"Forms up to {rate} items per second from a retained sample. Costs the tablet's exact essence value. Three deliberate dissolve slots." for i,rate in enumerate((1,8),1)},
}
OWNER_FILES = {**{f"textures/block/alchemy/collectors/{name}.png": f"textures/block/collectors/{name}.png" for name in ("front","other","top_1","top_2","top_3")},
               **{f"textures/block/alchemy/condenser_mk{i}.png": f"textures/block/condenser_mk{i}.png" for i in (1,2)}}
STATUSES = ("Ready", "Paused / redstone signal", "Needs owner authorization", "Protected location: work stopped", "Ready to use", "Concordance is disabled", "Essence value data invalid: work stopped", "Needs strong daylight (sky light 12+)", "Essence buffer full", "Collecting / transferring essence", "Place one target sample", "Choose a plain approved building / convenience sample", "Output full: intake stopped", "Waiting for enough essence", "Forming items", "Target needs a larger condenser buffer")
LANG = {
    "tooltip.jugcraft.workshop.stored": "Stored essence: %s. Mining keeps this charge; inventories drop separately.",
    "message.jugcraft.workshop.denied": "Only the owner's party may use this workshop, where building is allowed.",
    **{f"screen.jugcraft.workshop.status_{i}": value for i,value in enumerate(STATUSES)},
    **{f"screen.jugcraft.workshop.{key}": value for key,value in {
        "buffer":"Essence  %s / %s", "wallet":"Your essence: %s", "star_charge":"Charge star", "star_supply":"Star supply", "sample":"Target (kept)", "output":"Output",
        "feed":"To dissolve", "cost":"Cost: %s essence / item", "speed":"Up to %s items / second", "sunlight":"Sky light: %s/15", "generation":"+%s / second", "neighbours":"Condensers first", "then_star":"Then charge star", "collector_hint":"Sunlight only",
        "pause":"Pause", "resume":"Resume", "deposit":"Store essence", "withdraw":"Take essence",
        "pause_help":"Pause all work and hopper access. A redstone signal also pauses. Loaded authorized factories can use their own buffers while the owner is away.",
        "deposit_help":"Store as much personal essence as the machine has room for. Collectors charge their star; condensers draw from theirs. No transfer fee.",
        "withdraw_help":"Take stored essence back into your personal ledger, up to its 2,000,000,000 limit. Inventory items and the target sample stay in the machine.",
    }.items()},
}
SHAPED = {
    "collector_mk1": (["GLG", "ODO", "OFO"], {"G":"minecraft:glass", "L":"minecraft:glowstone", "O":"minecraft:obsidian", "D":rid("dark_matter"), "F":"minecraft:furnace"}),
    "condenser_mk1": (["ODO", "DCD", "ODO"], {"O":"minecraft:obsidian", "D":rid("dark_matter"), "C":"minecraft:chest"}),
}
def write_all(write, assets, data, lang, condition, self_drop):
    lang.update(LANG)
    library = Path(__file__).resolve().parents[1] / "art/owner-library/originals/Magic/models/block"
    # The supplied condenser block JSON is a particle-only placeholder for an external renderer. Its supplied
    # base_chest item model already has complete chest geometry/UVs, so use it as the placed block as well.
    chest = json.loads((library / "base_chest.json").read_text())
    write(assets / "models/block/alchemical_chest.json", chest)
    for key in BLOCKS:
        lang[f"block.{MOD}.{key}"] = NAMES[key]; lang[f"tooltip.{MOD}.{key}"] = TOOLTIPS[key]
        if key.startswith("collector"):
            tier = key[-1]
            model = {"parent":"minecraft:block/orientable_with_bottom", "textures":{"bottom":rid("block/alchemy/collectors/other"), "side":rid("block/alchemy/collectors/other"), "front":rid("block/alchemy/collectors/front"), "top":rid(f"block/alchemy/collectors/top_{tier}")}}
        else:
            model = {"parent":rid("block/alchemical_chest"), "textures":{"chest":rid(f"block/alchemy/{key}"), "particle":rid(f"block/alchemy/{key}")}}
        write(assets / f"models/block/{key}.json", model)
        write(assets / f"blockstates/{key}.json", {"variants":{f"facing={facing}": {"model":rid(f"block/{key}"), "y":angle} for facing,angle in zip(("north","east","south","west"),(0,90,180,270))}})
        write(assets / f"models/item/{key}.json", {"parent":rid(f"block/{key}")})
        write(assets / f"items/{key}.json", {"model":{"type":"minecraft:model", "model":rid(f"item/{key}")}})
        loot = self_drop(key)
        loot["pools"][0]["entries"][0]["modifier"] = [{"type":"minecraft:copy_components", "source":"block_entity", "include":[rid("workshop_essence")]}]
        write(data / f"loot_table/blocks/{key}.json", loot)
    for key,(pattern,ingredients) in SHAPED.items():
        write(data / f"recipe/{key}.json", {"fabric:load_conditions":condition("concordance"), "type":"minecraft:crafting_shaped", "category":"misc", "pattern":pattern, "key":ingredients, "result":{"id":rid(key),"count":1}})
    for key,base in (("collector_mk2","collector_mk1"),("collector_mk3","collector_mk2"),("condenser_mk2","condenser_mk1")):
        write(data / f"recipe/{key}.json", {"fabric:load_conditions":condition("concordance"), "type":"minecraft:smithing_transform", "template":rid("high_covalence_dust"), "base":rid(base), "addition":rid("dark_matter"), "result":{"id":rid(key),"count":1}})
def tags(tags):
    for key in BLOCKS:
        tags.add("block", "minecraft:mineable/pickaxe", rid(key))
        tags.add("item", rid("equivalence/excluded"), rid(key))
def codex():
    return {("equivalence","workshops"): {
        "name":"Collectors and Condensers", "x":8,"y":0,"icon":rid("collector_mk1"),"condition":None,"description":"Daylight into essence; essence into chosen materials",
        "pages":[
            ("crafting_recipe","Essence Collector","Place outdoors in **strong daylight** (sky light minus sky darkness at least **12**). Mk I / II / III gather **1 / 4 / 16 essence per second**, holding **4,096 / 16,384 / 65,536**. Torches cannot power them. Adjacent condensers receive essence first, then the collector charges a **Klein Star** in its socket. Storage works even at night; generation waits for daylight.",rid("collector_mk1")),
            ("crafting_recipe","Essence Condenser","Put **one plain approved building or convenience material** into the gold **Target (kept)** slot. That real sample stays there. The condenser makes ordinary copies at the tablet's existing **essence value rounded up per item, without a surcharge**. Mk I / II make up to **1 / 8 items per second** and hold **1,048,576 / 16,777,216 essence**. See the price and pause reason in the interface. Six output slots may hold successive targets.",rid("condenser_mk1")),
            ("text","Three Ways to Supply","Touch a collector directly to any condenser face, insert a **charged Klein Star**, or use **Store essence** to move your personal balance into the buffer. The three **To dissolve** slots automatically consume eligible plain materials, at most **8 / 64 items per second**. Never put something there you want to keep. Named, charged, filled and otherwise modified stacks are refused; default magical item types are allowed. Only the **target** is retained; outputs are never fuel."),
            ("text","Hoppers and Control","Hoppers above or at the sides insert dissolution inputs; a hopper **below** extracts outputs. They cannot move the target sample or star. **Pause**, or apply a **redstone signal**, to stop processing and hopper access. Full outputs, missing/invalid targets or an unbalanced server catalogue stop intake before anything is spent. The owner's party may use the interface; automatic neighbour transfers also require related ownership and permission at both ends."),
            ("text","Upgrade and Recover","Mine a workshop to carry its essence **on the dropped block**; its sample, inputs, outputs and star drop separately. At a **smithing table**, combine the current block with **High Covalence Dust** in the template slot and **Dark Matter** in the addition slot to upgrade one tier. Stored essence and custom names survive. **Take essence** withdraws to your personal ledger with its normal **2,000,000,000** cap. Default empty workshops and uncharged stars may be transmuted; stored charge is never copied."),
            ("text","Loaded Workshops Only","Factories record the player who places them as their owner and may keep working while that owner is offline or in another dimension. They use their own buffers and check current protection for the owner each pulse; an absent player's wallet is never accessed. Unloaded chunks never run and time away never catches up. Paused or disabled systems retain contents. Collector transfer caps are **64 / 256 / 1,024 essence per second** total; condenser star draw caps **4,096 / 32,768**. No cables, remote networks or chunk loading are required."),
        ]}}

# Owner-requested connected behavior supersedes the original manual-only descriptions.
from concordance_ee_expansion import TOOLTIPS as _connected_tooltips
TOOLTIPS.update({k:v for k,v in _connected_tooltips.items() if k in TOOLTIPS})
