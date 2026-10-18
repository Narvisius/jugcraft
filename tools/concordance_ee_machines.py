"""Owner-library relays, matter furnaces, pedestal, interdiction torch and controlled Nova charges."""
import json
from pathlib import Path
MOD="jugcraft"
def rid(name): return f"{MOD}:{name}"
NAMES={**{f"relay_mk{i}":f"Essence Relay Mk {i}" for i in range(1,4)},"dark_matter_furnace":"Dark Matter Furnace","red_matter_furnace":"Red Matter Furnace","dark_matter_pedestal":"Dark Matter Pedestal","interdiction_torch":"Interdiction Torch","nova_catalyst":"Nova Catalyst","nova_cataclysm":"Nova Cataclysm"}
BLOCKS={name:{"name":label} for name,label in NAMES.items()}
TOOLTIPS={
    **{f"relay_mk{i}":f"Dissolves up to {8<<(i-1)} plain materials per second; routes up to {256<<(2*(i-1)):,} essence through its front face, then charges its star." for i in range(1,4)},
    "dark_matter_furnace":"Smelts one item every two seconds for 16 essence. Three inputs, three outputs; vanilla smelting recipes.",
    "red_matter_furnace":"Smelts up to four items per second for 16 essence each. Three inputs, three outputs; vanilla smelting recipes.",
    "dark_matter_pedestal":"A retained artifact repairs, nourishes, heals, grows crops or wards nearby monsters using stored essence.",
    "interdiction_torch":"Standing or wall-mounted light; spends 4 essence per hostile creature pushed away. No player or pet effects.",
    "nova_catalyst":"Sneak-use empty-handed, or use its menu, to consume this block and 4,096 personal essence for protected radius-two clearing (64 blocks maximum).",
    "nova_cataclysm":"Sneak-use empty-handed, or use its menu, to consume this block and 16,384 personal essence for protected radius-three clearing (192 blocks maximum).",
}
OWNER_FILES={
    **{f"textures/block/alchemy/relays/{name}.png":f"textures/block/relays/{name}.png" for name in ("front","other","top_1","top_2","top_3")},
    **{f"textures/block/alchemy/matter_furnace/{name}.png":f"textures/block/matter_furnace/{name}.png" for name in ("dm_off","dm_on","rm_off","rm_on")},
    "textures/block/alchemy/interdiction_torch.png":"textures/block/interdiction_torch.png",
    **{f"textures/block/alchemy/explosives/{name}.png":f"textures/block/explosives/{name}.png" for name in ("bottom","top","nova_side","nova1_side")},
    **{f"textures/gui/alchemy/{name}.png":f"textures/gui/{name}.png" for name in ("dmfurnace","rmfurnace","relay1","relay2","relay3")},
}
STATUSES=("Ready","Paused / redstone signal","Waiting for owner in this dimension","Protected location: work stopped","Ready to use","Concordance is disabled","Equivalence audit failed: work stopped","Working","Waiting for input / recipient","Buffer or output full","Needs essence","Insert a supported artifact","Ready for explicit detonation")
LANG={"message.jugcraft.nova.cleared":"Nova cleared %s blocks for %s essence.",**{f"screen.jugcraft.alchemy_machine.status_{i}":value for i,value in enumerate(STATUSES)},**{f"screen.jugcraft.alchemy_machine.{key}":value for key,value in {
    "buffer":"Essence  %s / %s","wallet":"Your essence: %s","star_charge":"Charge star","star_supply":"Star supply","smelt":"To smelt","dissolve":"To dissolve","output":"Output","artifact":"Artifact (kept)",
    "pause":"Pause","resume":"Resume","deposit":"Store essence","withdraw":"Take essence","detonate":"Detonate","nova_cost":"Consumes this block + %s personal essence",
    "pause_help":"Pause processing and hopper access. Redstone also pauses. Loaded furnaces and relays can work from their own buffers while the authorized owner is away. Pedestals and wards need the owner present.",
    "nova_help":"Consumes the charge. Clears eligible loaded blocks only; skips claims, inventories, fluids and very hard blocks. No fire, chain reactions or entity damage. Only the owner can activate it.",
    "hint_relay":"Output: front face first, then Klein Star",
    "hint_furnace":"16 essence / item; retained XP on taking output",
    "hint_pedestal":"Radius 6; owner and party only; artifact stays",
    "hint_torch":"Radius 6; at most 8 hostile creatures / second",
    "hint_nova":"Keeps normal drops; skips protected blocks",
}.items()}}
SHAPED={
    "relay_mk1":(["ODO","OFO","OOO"],{"O":"minecraft:obsidian","D":rid("dark_matter"),"F":"minecraft:furnace"}),
    "dark_matter_furnace":(["DDD","DFD","DDD"],{"D":rid("dark_matter"),"F":"minecraft:furnace"}),
    "dark_matter_pedestal":(["DDD"," O ","OOO"],{"D":rid("dark_matter"),"O":"minecraft:obsidian"}),
    "interdiction_torch":([" D "," G "," I "],{"D":rid("dark_matter"),"G":"minecraft:glowstone","I":rid("iron_band")}),
    "nova_catalyst":(["GCG","ODO","GCG"],{"G":"minecraft:gunpowder","C":rid("alchemical_coal"),"O":"minecraft:obsidian","D":rid("dark_matter")}),
}
def write_all(write,assets,data,lang,condition,self_drop):
    lang.update(LANG)
    library=Path(__file__).resolve().parents[1]/"art/owner-library/originals/Magic/models/block"
    def imported(source):
        model=json.loads((library/f"{source}.json").read_text())
        def remap(value):
            if isinstance(value,str) and value.startswith("projecte:block/"):
                path=value.removeprefix("projecte:block/")
                if path in ("dark_matter_block","red_matter_block"):return rid(f"block/{path}")
                if path in ("relay_mk1","relay_mk2","relay_mk3"):return rid(f"block/{path}")
                if path in ("dm_furnace","rm_furnace"):return rid(f"block/{'dark' if path.startswith('dm') else 'red'}_matter_furnace")
                if path=="nova_catalyst":return rid("block/nova_catalyst")
                return rid(f"block/alchemy/{path}")
            if isinstance(value,dict):return {key:remap(val) for key,val in value.items()}
            if isinstance(value,list):return [remap(v) for v in value]
            return value
        return remap(model)
    for name in BLOCKS:
        lang[f"block.{MOD}.{name}"]=NAMES[name];lang[f"tooltip.{MOD}.{name}"]=TOOLTIPS[name]
        source={"dark_matter_furnace":"dm_furnace","red_matter_furnace":"rm_furnace","dark_matter_pedestal":"dm_pedestal"}.get(name,name)
        model=imported(source);write(assets/f"models/block/{name}.json",model)
        if "furnace" in name:
            write(assets/f"models/block/{name}_on.json",imported(source+"_on"))
            variants={f"facing={facing},lit={str(lit).lower()}":{"model":rid(f"block/{name}{'_on' if lit else ''}"),"y":angle} for facing,angle in zip(("north","east","south","west"),(0,90,180,270)) for lit in (False,True)}
        elif name=="interdiction_torch":
            write(assets/"models/block/wall_interdiction_torch.json",imported("wall_interdiction_torch"))
            variants={"wall=false":{"model":rid("block/interdiction_torch")},**{f"wall=true,facing={facing}":{"model":rid("block/wall_interdiction_torch"),"y":angle} for facing,angle in zip(("east","south","west","north"),(0,90,180,270))}}
        else:variants={f"facing={facing}":{"model":rid(f"block/{name}"),"y":angle} for facing,angle in zip(("north","east","south","west"),(0,90,180,270))}
        write(assets/f"blockstates/{name}.json",{"variants":variants})
        itemmodel={"parent":"minecraft:item/generated","textures":{"layer0":rid("block/alchemy/interdiction_torch")}} if name=="interdiction_torch" else {"parent":rid(f"block/{name}")}
        write(assets/f"models/item/{name}.json",itemmodel);write(assets/f"items/{name}.json",{"model":{"type":"minecraft:model","model":rid(f"item/{name}")}})
        loot=self_drop(name);loot["pools"][0]["entries"][0]["modifier"]=[{"type":"minecraft:copy_components","source":"block_entity","include":[rid("workshop_essence")]}];write(data/f"loot_table/blocks/{name}.json",loot)
    for name,(pattern,ingredients) in SHAPED.items():write(data/f"recipe/{name}.json",{"fabric:load_conditions":condition("concordance"),"type":"minecraft:crafting_shaped","category":"misc","pattern":pattern,"key":ingredients,"result":{"id":rid(name),"count":1}})
    for name,base,addition in (("relay_mk2","relay_mk1","dark_matter"),("relay_mk3","relay_mk2","red_matter"),("red_matter_furnace","dark_matter_furnace","red_matter"),("nova_cataclysm","nova_catalyst","red_matter")):
        write(data/f"recipe/{name}.json",{"fabric:load_conditions":condition("concordance"),"type":"minecraft:smithing_transform","template":rid("high_covalence_dust"),"base":rid(base),"addition":rid(addition),"result":{"id":rid(name),"count":1}})
def tags(tags):
    for name in BLOCKS:
        tags.add("block","minecraft:mineable/pickaxe",rid(name));tags.add("item",rid("equivalence/excluded"),rid(name))
def codex():
    return {("equivalence","advanced_workshops"):{"name":"Relays and Matter Workshops","x":12,"y":0,"icon":rid("relay_mk1"),"condition":None,"description":"Route essence, smelt, tend gardens and secure a workshop","pages":[
        ("crafting_recipe","Essence Relay","Mk I / II / III hold **16,384 / 65,536 / 262,144** essence. The three **To dissolve** slots consume plain materials at the same conservative tablet values, at most **8 / 16 / 32 items each second**. The **front textured face** sends **256 / 1,024 / 4,096 essence per second** to a touching condenser, relay, furnace, pedestal or torch. The remaining budget charges its socketed star. Collectors may feed relays from any side. No remote networks or chunk loading.",rid("relay_mk1")),
        ("crafting_recipe","Matter Furnaces","Use ordinary **smelting recipes** in the three input slots. Dark Matter cooks **one item every two seconds**; Red Matter cooks **up to four per second**. Both cost **16 essence per input**. Three output slots never overwrite items. XP accumulates and is released on taking output or mining the furnace. A charged Klein Star, touching collector or relay, or **Store essence** supplies power. Hoppers insert above/sides and extract below.",rid("dark_matter_furnace")),
        ("crafting_recipe","Dark Matter Pedestal","The artifact is **kept**. Repair Talisman repairs up to **4 equipped durability points** per nearby party member at **1 essence per point**. Body / Soul / Life Stones restore **food / health / both**, one point for **32 essence**. Up to eight owner/party players within **6 blocks** are served. Harvest Goddess grows at most **4 permitted crop steps for 16 essence each** across a rolling 7x3x7 garden; Concordance organisms still pay nutrients and obey their ecology. Swiftwolf pushes hostile mobs as the torch does. Mature crops are harvested and replanted. Six additional artifact effects cover pickup, XP, defense and workshop acceleration; see **Connected Alchemy**.",rid("dark_matter_pedestal")),
        ("crafting_recipe","Interdiction Torch","Place on the ground or a wall. It lights the area and, while powered, pushes at most **8 visible hostile monsters per second** within **6 blocks**, for **4 essence per target**. Players, pets and neutral creatures are not targets. Dock a charged Klein Star or feed it from a collector or relay. A paused/empty torch remains an ordinary light.",rid("interdiction_torch")),
        ("crafting_recipe","Controlled Nova Charges","These are deliberate excavation charges: **sneak-use with an empty hand**, or press **Detonate** in the menu. The owner consumes the actual block plus **4,096 / 16,384 personal essence**. Catalyst / Cataclysm clear within radius **2 / 3**, at most **64 / 192 blocks**. They keep normal unenchanted netherite-pick drops and skip inventories, fluids, very hard/unbreakable blocks, other charges, unloaded terrain and protected locations. No fire, entity damage, chain reaction or remote detonation.",rid("nova_catalyst")),
        ("text","Safe Workshop Logistics","Place a block to own it; no research is required. Permission at each affected location still applies. Loaded relays and Matter furnaces may work from their own inventories and buffers while the owner is away. Pedestals, wards and Nova charges still need the real owner online in this dimension. Party members may use menus, but only owners detonate Nova charges. **Pause** or redstone stops work and hopper access. Stars and pedestal artifacts are manual-only. Mine to keep stored essence on the dropped block; inventories drop separately. High Covalence Dust smithing upgrades preserve charge and names. No offline catch-up."),
    ]}}

# Owner-requested connected behavior supersedes the original manual-only descriptions.
from concordance_ee_expansion import TOOLTIPS as _connected_tooltips
TOOLTIPS.update({k:v for k,v in _connected_tooltips.items() if k in TOOLTIPS})
