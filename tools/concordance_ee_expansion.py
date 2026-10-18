"""Connected alchemy behavior and remaining owner UI/state art. No new item registry or currency."""
GUI = ('alchchest','book_texture','collector1','collector2','collector3','condenser','condenser_mk2','eternal_density','mercurial_eye','nei','portaldevice')
ACTIVE = {'gem_of_eternal_density':'dense_gem_on','void_ring':'rings/void_on','mind_stone':'rings/mind_stone_on'}
OWNER_FILES = {**{f'textures/gui/alchemy/{name}.png':f'textures/gui/{name}.png' for name in GUI},
               **{f'textures/item/alchemy/{name}_active.png':f'textures/item/{source}.png' for name,source in ACTIVE.items()}}
TOOLTIPS = {
 'philosophers_stone':'Use: essence tablet. Sneak-use air: crafting. Use terrain: transform. Sneak-use terrain: cycle 1x1 / 3x3 / 5x5. Costs at least 8 essence per block.',
 'gem_of_eternal_density':'Use to set nine allowed material identities, output tier and automation. Runs in inventory or a carried bag/chest. Empty filters consume nothing.',
 'void_ring':'Use: confirm dissolving the other-hand material. Sneak-use: configure filtered automatic dissolution. Empty filters consume nothing.',
 'mind_stone':'Use stores 100 XP; sneak-use retrieves 100. On a pedestal, collects nearby XP into this physical stone. The lit icon indicates stored XP.',
 'transmutation_tablet':'Dissolve materials into essence; open Learned patterns to learn, search, favorite and form items without retaining a sample.',
 'mercurial_eye':'Sneak-use or use in air: choose a learned building material. Use a surface: build the selected 1x1 / 3x3 / 5x5 face for formation price + 8 essence per placed block. Without a selected pattern, uses offhand blocks.',
 'dark_matter_pedestal':'Twelve artifacts: repair, support, grow/harvest, repel, gather into adjacent storage, defend, collect XP, or accelerate adjacent furnaces/condensers. Uses its buffer.',
 'alchemical_chest':'54 party-access slots with an opening lid. Activated artifacts can repair, consolidate and collect matching drops while the chest is closed. Redstone pauses.',
 'repair_talisman':'Activated while held/worn: repairs equipment. Activated inside a carried bag or owned chest: repairs stored gear. 1 essence per point, up to 4 each second.',
 'black_hole_band':'Activated while held/worn: attracts drops. Inside storage: collects matching stored item types. On a pedestal: inserts nearby drops into adjacent inventories.',
}
for i in range(1,7):TOOLTIPS[f'klein_star_{i}']='Stores essence. Equipment spends your wallet first, then carried stars in inventory order. Use to store/withdraw; nested stars are not accessed.'
for i in range(1,4):TOOLTIPS[f'collector_mk{i}']='Collects daylight essence. Charge mode exports to neighbors/stars. Fuel mode upgrades coal at the difference in item values to a chosen final alchemical fuel; inputs above/sides, output below.'
LANG={
 'key.jugcraft.gem_abilities':'Gem armor abilities',
 'message.jugcraft.stone.charge':'Stone area: %s x %s',
 'screen.jugcraft.alchemy.patterns':'Patterns',
 'screen.jugcraft.alchemy.gem_controls':'Gem armor',
 **{f'screen.jugcraft.pattern.{k}':v for k,v in {
 'title':'Learned transmutation','build':'Mercurial building palette','search':'Search patterns...',
 'learn':'Learn','make':'Form one','choose':'Use pattern','previous':'<','next':'>','favorite':'Favorite',
 'unfavorite':'Unfavorite','favorites':'Favorites','all':'All patterns','balance':'Essence: %s','sample':'Sample','target':'Target','preview':'Preview',
 'output':'Output','cost':'Price per item: %s','page':'%s / %s','learn_help':'Learn once; keep sample','area':'3 x 3 / up to nine blocks',
 'make_help':'Forms one learned item for the displayed price. Only your actual learned, eligible materials are available.',
 'build_help':'Select a learned building block. Choose an area and use the eye on a surface to place that face, each block at the displayed price. Protected or occupied positions are skipped.',
 }.items()},
 **{f'screen.jugcraft.automation.{k}':v for k,v in {
 'start':'Start automatic','stop':'Stop automatic','target_0':'Output: Iron','target_1':'Output: Gold','target_2':'Output: Diamond',
 'clear':'Clear filters','filters':'Allowed input materials','hint':'Copy a held stack into a filter.',
 'help':'Only these material types may be consumed. Click a filter with an empty cursor to remove it. Your stack is kept. Modified items and hotbar equipment are excluded.',
 'output_help':'Forms at most one output per pulse at its normal price, using your shared essence balance. Existing essence may be spent. A full output stops input consumption.',
 }.items()},
 **{f'screen.jugcraft.gem.{k}':v for k,v in {
 'title':'Gem armor abilities','action_0':'Helmet: toggle night vision','action_1':'Chest: repel nearby enemies','action_2':'Leggings: forward dash','action_3':'Boots: toggle flight',
 'help_0':'Wear an enabled Gem helmet. Night vision costs 4 essence each renewal. Toggle again to stop renewing it.',
 'help_1':'Wear an enabled Gem chestplate. Repels nearby hostile enemies for 16 essence each.',
 'help_2':'Wear enabled Gem leggings and stand on solid ground. Dash forward for 64 essence; blocked paths are refused.',
 'help_3':'Wear enabled Gem boots. Flight costs 16 essence per second. Double-jump to fly once enabled; toggle again to turn it off.',
 }.items()},
 **{f'screen.jugcraft.workshop.fuel_{i}':v for i,v in enumerate(('Mode: Charge','Stop: Alchemical Coal','Stop: Mobius Fuel','Stop: Aeternalis Fuel'))},
 'screen.jugcraft.workshop.fuel_input':'Fuel to upgrade',
 'screen.jugcraft.workshop.fuel_help':'One fuel step per second, paying the difference between input and output essence values: coal -> Alchemical -> Mobius -> Aeternalis. Target fuel is never upgraded further. Charge mode exports essence instead.',
}
def tags(tags): pass
def write_all(write,assets,data,lang,condition,self_drop):
    lang.update(LANG)
    for name,text in TOOLTIPS.items():lang[f'tooltip.jugcraft.{name}']=text
    for name in ACTIVE:
        write(assets/f'models/item/{name}_active.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'jugcraft:item/alchemy/{name}_active'}})
        write(assets/f'items/{name}.json',{'model':{'type':'minecraft:condition','property':'minecraft:has_component','component':'jugcraft:alchemy_active',
              'on_false':{'type':'minecraft:model','model':f'jugcraft:item/{name}'},'on_true':{'type':'minecraft:model','model':f'jugcraft:item/{name}_active'}}})
    # Actual player help is replaced together with mechanics; older feature records remain historical evidence.
    bodies=[
     'Craft, find or trade for a Stone or tablet. Use it immediately: no research or study is required. Dissolve one plain item to learn it permanently. Choose its icon to withdraw it for essence.',
     'All default item types have values. Dissolve rounds down; withdrawal rounds up, with no surcharge. Use 1 / 16 / 64 / Max. Search and favorites are optional. Empty charged or filled items before dissolving; their contents are never copied.',
     'Next craft a Klein Star, daylight collector and condenser. Charge feeds neighbors; Fuel upgrades coal for the difference in item values. Loaded factories work while you are away. Claims, pause and redstone still stop them.',
     'Bags have 54 independent slots. Sort and Stack merge identical items. Pause disables stored artifacts. Repair Talisman repairs gear; Black Hole Band gathers matching drops. Open menus pause automation. No nesting.',
     'Use Density Gem or sneak-use Void Ring. Set allowed inputs, input limit and wallet reserve, then Start. Empty filters do nothing. Density makes iron, gold or diamond, one per second. Last status explains waiting. Your hotbar is not feedstock.',
     'Add a pedestal for farming or defense. Its panel shows the effect, range and last cost. Gem controls show toggles and cooldown; bind direct powers in Controls. The HUD shows wallet, stars and last payment. Aim the Eye/tools to preview area and cost.',
    ]
    for i,text in enumerate(bodies):lang[f'screen.jugcraft.alchemy_storage.manual.body_{i}']=text
def codex():
    return {('equivalence','connected_alchemy'):{'name':'Connected Alchemy','x':18,'y':0,'icon':'jugcraft:transmutation_tablet','condition':None,'description':'Learn patterns; automate a living workshop',
        'pages':[
         ('text','Learn Once','The tablet opens **dissolving and learned items together**. Put an item into the input, inspect its value and confirm dissolution: the input is consumed and its default form is learned permanently. Choose an icon, then **1 / 16 / 64 / Max** and Withdraw. Pay the exact value, no surcharge, using wallet then carried stars. Max fills one affordable output stack. Empty containers and charge first; custom state is not copied. All registered types are valued; unfamiliar custom items may use provisional local values.'),
         ('text','Automatic Storage','Configure a **Density Gem** or sneak-use a **Void Ring**. Copy up to nine allowed input types into its filter and start it. Only plain matching material is consumed. Density forms its selected iron/gold/diamond target; Void deposits essence. A carried bag or owned chest also supports active **Repair Talisman** and **Black Hole Band**. Seed storage with a desired item to filter pickup. Storage menus pause their own background writer.'),
         ('text','Collector Fuels','Choose **Charge**, **Alchemical Coal**, **Mobius Fuel** or **Aeternalis Fuel** in a collector. Each step consumes one previous fuel and **the difference between input and output item values**. Intermediate fuels remain in output and can continue until the chosen stop. Clear output space is required. Charge mode retains the existing neighbor/star export behavior.'),
         ('text','Pedestal Workshop','Original six effects remain. **Harvest Goddess now replants ripe crops**. **Black Hole Band** transfers drops into adjacent inventories. **Mind Stone** stores nearby XP, preserving merged orb counts. **Archangel**, **Ignition**, **Zero** defend against monsters. **Watch** gives adjacent furnaces/condensers one extra paid work pulse per second, shared across all Watches. All effects use the pedestal buffer and owner authority.'),
         ('text','Stone and Eye','Stone: use terrain to transmute; sneak-use terrain changes area (1x1, 3x3, 5x5); sneak-use air opens crafting. Each changed block costs at least 8 essence. Eye: open its learned building palette, select a block, then use a surface for a selected **1x1 / 3x3 / 5x5 face**. Each placed block costs formation price plus 8. Claims, collision, reach and loaded terrain apply.'),
         ('text','Stars and Gem Armor','Equipment spends your **wallet first**, then carried Klein Stars. No nested-star access. Gem controls are in the Manual, with optional menu and direct-action keybinds in Controls. Helmet toggles night vision; chest repels enemies; leggings dash; boots toggle paid flight. Flight shares the existing lease and ends on removal or insufficient power. Ordinary wards remain.'),
        ]}}

# Universal tablet overrides, owner playability pass (17 October).
LANG.update({"screen.jugcraft.pattern."+k:v for k,v in {
 "title":"Transmutation tablet", "learn":"Dissolve", "confirm":"Confirm", "make":"Withdraw",
 "sample":"Dissolve", "sources":"Wallet %s / stars %s", "total":"Total price: %s essence",
 "learn_help":"Consumes the input stack and permanently teaches its default item. Click twice to confirm. Empty stored contents and reset charged/configured items first.",
 "make_help":"Withdraw learned items at their exact value, no surcharge. Wallet first, then carried stars. Max fills up to one affordable output stack.",
 "batch_0":"1", "batch_1":"16", "batch_2":"64", "batch_3":"Max",
 "input_0":"Dissolve for +%s essence", "input_1":"Put an item in the input.",
 "input_2":"Empty/reset item state first.", "input_3":"Wallet is full.", "input_4":"Batch is worth less than 1.",
 "form_0":"Ready to withdraw", "form_1":"Dissolve once to learn an item.",
 "form_2":"Not enough essence", "form_3":"Take the output first."
}.items()})
TOOLTIPS['transmutation_tablet']='Dissolve an item to learn it permanently. Browse learned items, choose 1/16/64/Max and spend their essence value with no surcharge. Stored contents and charge must be emptied first.'

LANG.update({"screen.jugcraft.automation."+k:v for k,v in {
 "budget":"Input limit: %s/s", "reserve":"Reserve: %s", "preview":"Price: %s essence",
 "reserve_help":"Density never spends below this wallet reserve. Input limit bounds how many filtered items are dissolved per second. Stars are not used by automatic density.",
 "menu_pause":"Automation pauses while a menu is open.", "status_0":"Last pulse: working", "status_1":"Last pulse: none", "status_2":"Add at least one input filter",
 "status_3":"Output space is full", "status_4":"Output has no value", "status_5":"Waiting for filtered inputs", "status_6":"Saving for output / reserve"
}.items()})
LANG.update({"screen.jugcraft.alchemy_storage."+k:v for k,v in {
 "sort":"Sort", "stack":"Stack", "pause":"Pause", "inventory":"Automation pauses while open",
 "controls_help":"Sort also merges identical stacks; Stack preserves first-seen order. Pause disables artifacts in this container. To configure or re-enable an artifact, take it out and use it, then return it."
}.items()})

LANG.update({"screen.jugcraft.pattern.area":"Area: %s x %s", "screen.jugcraft.pattern.area_help":"Aim to preview the area."})
LANG.update({"hud.jugcraft.alchemy."+k:v for k,v in {
 "power":"Essence: %s", "sources":"Wallet %s | carried stars %s", "low":"Low power: action needs more essence", "spent":"Last payment: %s essence",
 "preview":"Preview: up to %s blocks / %s essence; claims checked on use", "range":"Pedestal range: %s blocks (effect limits apply)"
}.items()})
LANG.update({"key.jugcraft.gem_"+key:value for key,value in zip(("night_vision","repel","dash","flight"),("Gem helmet: night vision","Gem chest: repel","Gem leggings: dash","Gem boots: flight"))})
LANG.update({"screen.jugcraft.gem."+k:v for k,v in {
 "short_0":"Night vision", "short_1":"Repel", "short_2":"Dash", "short_3":"Flight", "on":"On", "off":"Off", "ready":"Ready", "missing":"Equip this armor", "cooldown":"Cooling down", "power":"Available essence: %s (wallet + stars)"
}.items()})
LANG.update({"screen.jugcraft.alchemy_machine."+k:v for k,v in {
 "range":"Range: %s blocks", "range_help":"Select 2 / 4 / 6 blocks. Aim at the pedestal to see its footprint. Watch affects adjacent machines only; Harvest stays within its 7 x 3 x 7 farm scan. Claims and line of sight still apply.",
 "pulse":"Last pulse: %s targets/units, %s essence",
 "effect_air":"Insert an artifact to see its effect.", "effect_black_hole_band":"Gather drops to adjacent storage: 1 essence/stack",
 "effect_archangel_smite":"Strike hostile mobs: 64 essence/target, max 4/s",
 "effect_ignition_ring":"Smoulder hostile mobs: 16 essence/target, max 4/s",
 "effect_zero_ring":"Slow hostile mobs: 16 essence/target, max 4/s",
 "effect_watch_of_flowing_time":"Accelerate adjacent machines: 128 essence/step",
 "effect_mind_stone":"Capture XP in the stone: 1 essence/orb, max 100 XP/s",
 "effect_repair_talisman":"Repair party equipment: 1 essence/durability",
 "effect_harvest_goddess_band":"Grow/harvest crops: 16 essence/action, max 4/s",
 "effect_body_stone":"Feed nearby party members: 32 essence/food",
 "effect_soul_stone":"Heal nearby party members: 32 essence/health",
 "effect_life_stone":"Heal + feed party members: 32 essence/point",
 "effect_swiftwolf_rending_gale":"Repel hostile mobs: 4 essence/target, max 8/s"
}.items()})

LANG.update({"screen.jugcraft.pattern.guide":"Guide", "screen.jugcraft.alchemy_storage.source_0":"Reference value", "screen.jugcraft.alchemy_storage.source_1":"Derived from a local recipe", "screen.jugcraft.alchemy_storage.source_2":"Provisional local assignment"})


# Equipment access and engraved control panels, owner-directed playability revision.
LANG.update({
    "message.jugcraft.alchemy.unavailable": "Equipment unavailable: check that Concordance is enabled and its essence data is valid.",
    "screen.jugcraft.pattern.collection": "LEARNED ITEMS",
    "screen.jugcraft.pattern.target": "Selected",
    "screen.jugcraft.pattern.sample": "Input",
    "screen.jugcraft.pattern.sources": "Wallet %s / stars %s",
    "screen.jugcraft.pattern.form_1": "Dissolve an item, then select it.",
    "screen.jugcraft.automation.filters": "Allowed inputs (click with an item)",
    "screen.jugcraft.gem.missing": "Equip this armor",
    "screen.jugcraft.alchemy_storage.no_nesting": "54 slots / no bags inside",
    "screen.jugcraft.gem.help_0": "Wear the Gem Helmet and click to toggle night vision. 4 essence per renewal. No research or pre-activation needed.",
    "screen.jugcraft.gem.help_1": "Wear the Gem Chestplate and click to repel nearby hostile mobs. Costs essence per target; claims still apply.",
    "screen.jugcraft.gem.help_2": "Wear Gem Leggings and click to dash while grounded. Costs 64 essence; requires clear space and permission.",
    "screen.jugcraft.gem.help_3": "Wear Gem Boots and click to toggle flight. Uses the shared paid flight lease; ends without power or when removed.",
})
