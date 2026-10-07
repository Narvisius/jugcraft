# Bunker and trench interiors (batch 59)

Status: implemented (pending CI and review)
Proposal issue: none. The owner's ideas list after the tower guns (4 October 2026, "do those 1-6 ... one at a time with lots of depth") led to fortifications (batch 55) and fire control (batch 56); this batch furnishes the trenches and dugouts those guns are fought from.
Owner: jimbozoomer-byte
Target milestone and tier: steel tier, beside the trench works (batch 50), fortifications (batch 55) and fire control (batch 56)
Primary specialty and supported player role: building and base defence; spotting for the guns; feeding a garrison

## Player experience
Eight new blocks and a new dish:

![A dugout under corrugated iron on timber shoring: a bunk, a map table, a lit field kitchen with a cooking pot, bunker lamps, a gas curtain let down in the back doorway and one rolled up in the side doorway, and a trench periscope looking over a sandbag parapet (offline render)](../images/bunkerworks_preview.png)

| Block | What it is |
| --- | --- |
| **Trench Periscope** | Two blocks tall: a slim olive box-section periscope on a stand, the eyepiece and handles at about head height and the mirror head on top, so it pokes over a parapet. It looks the way you face when you place it.<br>**Every second it counts the hostile mobs it can see**: within 64 blocks, inside 45 degrees either side of the way it looks, with a clear line of sight from the mirror head. A **comparator** reads the count (up to 15).<br>**Use it (either half) to look through it:** the nearest hostile mob in view becomes your **target mark**, just as a Range Finder marks one, so your guns, other gunners near you and fire control tables can use it. You hear the spyglass and see the mob's name, distance and compass bearing ("Zombie, 42 blocks, NE"). **Sneak and use it to clear your mark.** |
| **Map Table** | A waist-high table with a campaign map pinned on it (sea, fields, a river, a front line, a grid, pins and a compass rose), a pair of dividers and an inkwell.<br>**Use it** to read the plot: the target marks in this dimension pointing within 256 blocks of the table, nearest first, at most five lines, each with who marked it, how far and which way it is from the table, and how many seconds ago. With none: "No targets plotted".<br>**Sneak and use it** with a Fire Control Table within 4 blocks: that table is laid on the next mark on the plot (the one after its present target, round to the first after the last). |
| **Gas Curtain** | Two blocks tall: a heavy wet khaki blanket hung from a timber batten across a doorway. **You walk through it** (no collision).<br>**Let down, it keeps gas out:** a chlorine or smoke cloud does not reach anyone with a hanging curtain on the straight line between the cloud's middle and their eyes. Thermite, which burns on the floor, is not stopped.<br>**Use either half** to roll it up under its batten (open, no use against gas) or let it down again. |
| **Field Kitchen** | A riveted black iron stove with a firebox door at the front (its vents glow when lit), a towel rail, a hob plate on top and its stove pipe running up the back to the top edge.<br>It holds **one stack of fuel**: logs, coal, charcoal, coal blocks or coke (the Hearth Oven's fuels). Use it with fuel to load it, empty-handed to see its fuel and seconds of fire left.<br>**It only puts a new piece on the fire while a Cooking Pot stands on top**, so an empty stove never wastes fuel; a piece already burning burns down. While burning it is **lit** (light 13), smokes from its pipe and **heats the Cooking Pot on it**. |
| **Corrugated Iron**, with slab and stairs | Galvanised grey-blue sheet with ribs every four pixels, for bunker roofs and trench walls. A wall or roof reads as one ribbed sheet. Pickaxe. |
| **Timber Shoring** | A squared timber with a bolted iron strap, placed like a log (upright or on its side), to hold up a dugout roof. Axe. |
| **Bunker Lamp** | A warm glass globe in a dark wire cage, like a lantern: it **hangs from a ceiling** (with a short hanger) or **stands on a floor** (with a carrying loop). Light 14. |
| **Bunker Bunk** | Two blocks tall: two timber bunks one above the other, each with a mattress, a folded khaki blanket and a pillow. **Use it to sit on the lower bunk** (sneak to get up). It is furniture, not a bed: it sets no spawn point and skips no night. |

| Food | What it is |
| --- | --- |
| **Trench Stew** | Beef, a potato and a carrot stewed in a bowl in the Cooking Pot (300 ticks with heat; any heat source works, the Field Kitchen is the one that belongs in a dugout). 10 food and 0.8 saturation, like chili and forager's stew, and **Regeneration I for 5 seconds**. Leaves the bowl. |

## Connections
- Recipes (shaped, the "machines" feature switch like the other building sets):
  - Trench Periscope: `PG / P_ / P_`: 3 steel plates and a glass pane (1).
  - Map Table: `MI / LL`: an empty map, an iron nugget and 2 planks (1).
  - Gas Curtain: `S / W / W`: a stick over 2 wool (1).
  - Field Kitchen: `PPP / P_P / PFP`: 7 steel plates and a furnace (1).
  - Corrugated Iron: 2 × 2 steel plates (8). Slab: 3 in a row (6). Stairs: the usual pattern (4).
  - Timber Shoring: `L / I / L`: 2 logs and an iron ingot (3).
  - Bunker Lamp: `_N_ / NTN / _N_`: 4 iron nuggets round a torch (1).
  - Bunker Bunk: `WW / LL / WW`: 4 wool and 2 planks (1).
  - Trench Stew (Cooking Pot, `jugcraft:pot_cooking`): a bowl, raw beef, a potato and a carrot.
- Input producers:
  - The metal press's steel plates (steel tier).
  - Vanilla planks, logs, wool, glass panes, maps, furnaces, torches; beef, potatoes and carrots from farms and herds.
  - Fuel: logs, coal, charcoal and the coke oven's coke.
- Output consumers:
  - **Artillery and fire control:** periscope marks are ordinary target marks (`artillery/Spotting`), so a crewed gun fires at its gunner's own mark or a spotter's mark nearby, a Fire Control Table can take one with a Range Finder, and the Map Table hands them to a Fire Control Table.
  - **Redstone:** the periscope's comparator count can sound an alarm, light lamps or trigger a fire control salvo.
  - **Field chemistry:** gas curtains keep chlorine and smoke grenades (batch 31) out of dugouts.
  - **Cooking:** the Field Kitchen is a heat source (`#jugcraft:heat_sources`) for the Cooking Pot and every pot recipe.
- Reachable entry path: every input is vanilla or steel-tier; none of these blocks is needed to make another, so nothing is circular.
- Optional: all of it. The periscope and map table make the fire-control specialty easier but guns work without them; a dugout can be built from vanilla blocks.

## Balance and automation
- **No positive-gain loops.** The Field Kitchen only burns fuel and makes no heat for anything but the pot on top of it, which still needs its ingredients; it starts no new fuel without a pot, and a piece already lit burns down whether or not the pot is cooking (like a furnace). Fuel values are the Hearth Oven's: coal 1600 ticks, charcoal 1200, coke 3200, a coal block 16000, a log 300.
- **Trench Stew** costs raw beef (3 food), a potato (1) and a carrot (3), a bowl back, and 300 ticks of fuel, for 10 food and 8 saturation plus 5 seconds of Regeneration I (about 2 hearts). It is in line with the other pot stews and cheaper per effect than a Regeneration potion; it is a meal, not a loop: nothing turns it back into its inputs.
- **Corrugated iron** turns 4 steel plates into 8 sheets; nothing turns sheets back into plates, so it is a sink for steel like the other building sets.
- **Periscope cost per second:** one entity search in a 128-block box and a line-of-sight ray per hostile mob in its cone, once every 20 ticks; it writes its block state only when the count changes.
- **Gas curtain cost:** only for chlorine and smoke clouds, only for targets already inside the cloud, once a pulse (every second): a walk of at most 4 steps a block along a line at most the cloud's radius (3 or 4 blocks) long, one block read per step.
- **Map table:** a search of the 9 × 9 × 9 blocks round it for a Fire Control Table, only when someone sneak-uses it.
- **Automation:** pipes, hoppers and conveyors can load a Field Kitchen with fuel from any side not covered by the pot, but nothing can take fuel out of it. Breaking it drops its fuel.

## Multiplayer and persistence
- **Server authority:** every scan, mark, toggle, fuel change and cooking step happens on the server; clients see block states, messages and the smoke particles (client only, drawn from the `lit` state). Using a block goes through vanilla's reach check.
- **Marks are not saved:** periscope and range finder marks are fire orders kept in memory for 5 minutes (`MARK_TTL`), not part of the world. The map table lists them and adds no state of its own. A Fire Control Table saves the target the map table gave it, as it saves a range finder target.
- **Saved:** the periscope's count (block state), the curtain's `rolled` state, the field kitchen's fuel, fire and `lit` state (block entity and state). The periscope's scheduled tick is saved with the chunk.
- **Names:** the map table names the player who made each mark when they are online, and says "Someone" otherwise.
- **Two-block-tall blocks** (periscope, curtain, bunk) place and break as one, like doors, and drop one item from the lower half.

## Dependencies and assets
- No dependencies. All art is original, drawn in the clean style (`tools/clean_metal.py`) by `tools/bunkerworks.py`: corrugated iron ribs, timber shoring's side and sawn end, post timber, the curtain blanket, its roll and cord, the periscope's olive paint, window and rubber, the campaign map and its paper, the stove's panels, hob, firebox door (cold and lit) and pipe, the lamp's amber globe, the bunk's mattress, blanket and pillow; item icons for the periscope, curtain and bunk; Trench Stew's bowl (`kitchen_textures.bowl_item`). It reuses `dr_skid`, `dp_chrome`, `ik_brass` and the trench works' `ts_wood`.
- Models are built from boxes. The two-block-tall things are drawn whole, separated so no two faces share a plane, then cut into their `_lower` and `_upper` models (`bunkerworks.halves`), which `tools/art_check.py` checks together.
- Code:
  - `building/Bunkerworks` (registration), `TrenchPeriscopeBlock`, `MapTableBlock`, `GasCurtainBlock`, `FieldKitchenBlock` (with its block entity), `BunkerBunkBlock`; the lamp is vanilla's `LanternBlock` and the shoring a `RotatedPillarBlock`.
  - `artillery/Spotting.near` (the marks near a point, nearest first, as a copy).
  - `weapons/ChemicalCloud` asks `GasCurtainBlock.shields` before chlorine or smoke works on a target.
  - `agriculture/JugcraftAgriculture` registers Trench Stew (a stew with an effect); `CookingPotBlockEntity.isHeated` already counts a heat source with a `lit` property only while lit, which covers the Field Kitchen.
  - Tags: the field kitchen in `#jugcraft:heat_sources`, mining tags (the gas curtain under `#minecraft:mineable/hoe`, like other soft blocks; it breaks quickly by hand anyway), Trench Stew in `#c:foods/soup`.

## Verification
- Planned in CI (server game tests, `JugcraftGameTests`):
  - `bunkerBlocksPlace`: every block places; placing the lower half of the periscope, curtain and bunk puts up the upper half; the lamp hangs from a ceiling and shines at 14; the shoring lies along an axis; the field kitchen has its block entity.
  - `periscopeCountsAndMarks`: a periscope looking south with three zombies in front and one behind reads 3 on a comparator after its scan; a player using it gets a mark on the nearest zombie, and sneak-using it clears the mark.
  - `gasCurtainStopsChlorine`: a chlorine cloud on one side of a hanging curtain does not hurt a pig on the other side; using the curtain rolls up both halves, and the next pulse hurts the pig.
  - `fieldKitchenHeatsThePot`: a field kitchen with two coal and a Cooking Pot on top cooks Trench Stew from a bowl, beef, a potato and a carrot, burning one coal; one without a pot stays unlit with both coal; dirt is refused.
  - `mapTablePlotsTargetsToFireControl`: with a mark beside a map table, sneak-using it lays the fire control table 3 blocks away on that mark.
  - Client screenshot `jugcraft_bunker` (in `JugcraftClientGameTests`, after `jugcraft_fortifications`): the dugout in the picture above, with a lit field kitchen cooking and lamps hanging from the roof.
- Done locally:
  - `python3 tools/check_mod_data.py` passes, including the art check (no new allow-list entries) and the new `check_bunkerworks` (the block list, kinds, strengths and numbers against the tool, Trench Stew's food, effect and pot recipe against `tools/agriculture.py` and the Java, the heat-source tag and the lower-half-only loot of the tall blocks).
  - `python3 scripts/check_repository.py` passes.
  - I reviewed offline renders of every model from two sides and a sheet of the new textures and icons, and the preview above.
- Not done: compiling (no Minecraft on the classpath locally; CI compiles), running the game tests, a two-player server, a curtain against a smoke grenade in play.

## World and event applicability
Not applicable: everything is crafted and placed by players. The periscope sees any `Enemy` mob, including the raiders (batch 57).

## Rollout and open questions
- The Field Kitchen takes the Hearth Oven's fuels, not every furnace fuel (no planks, sticks or lava buckets), so nothing can leave a bucket behind in it.
- The periscope does not see players, even in PvP; it counts mobs only.
- A gas curtain checks the blocks on the line between a cloud and a target, so a cloud that bursts inside the curtain's own doorway block is shielded on both sides; a curtain is no use against a grenade thrown into the dugout itself.
