# Blueprint System (Blueprint Table, import, Survey Stake hologram, drone building)

Status: first slice implemented on the drone branch; the full design is proposal #23
Owner: Narvisius

## Player experience
- **Craft a Blueprint Table** (3 paper, 4 planks, 1 copper ingot) and right-click it.
  - It is a drafting station two blocks wide (an MBS placed by one item, like a bed; it needs the block to your right free): steel trestle legs, a tilted drawing board with a blueprint taped on, a drafting arm and scales, a pencil ledge, a shelf of drawings and rolled plans, and a swing-arm lamp. Breaking either half breaks the whole table and drops it once.
  - The screen is a drafting sheet on a steel board, with folder tabs (below). The left is the drawing register; the selected blueprint turns slowly in the middle (drawn in blueprint blue with white ink, one turn every 14 seconds) over a dimension line, with its bill of materials and a title block (title, type, block count, source) on the right. The red **PRINT** stamp gives you the blueprint for free.
  - The board is 440 wide where the screen has room (410 on a narrow one, where the tabs take short names: SETS,
    INDIVIDUAL, PARTIAL, IMPORT), so all four tabs sit on the board. In a window too small for it (the smallest GUI
    is 320 x 240) the whole board scales down to fit, the paste box, turning preview and mouse with it
    ([blueprint-table-fit.md](blueprint-table-fit.md)). Names too long for their space (in the drawing
    register, the bill of materials and the title block) slide slowly to the left until their end shows, wait, and
    start again, so the whole name can be read.
  - **Importing:** paste the text of a blueprint (`.jugbp.json`, for example one an AI designed for you) under IMPORT, **+ NEW IMPORT**, and press IMPORT.
    - The server checks it and explains any problem in plain words: unknown block, too big, forbidden block, bad JSON.
    - If it passes, it is saved with the world (so stakes of it keep working) and in your own import list.
- **Tabs at the table:**
  - **STRUCTURE SET:** several parts making a whole, such as a complete power plant (`"kind": "set"`).
  - **INDIVIDUAL STRUCTURES:** one structure, not a part and not divided, such as the Small Church. This is the default (`"kind": "individual"`).
  - **PARTIAL STRUCTURES:** one part of a set, such as a single cooling tower (`"kind": "part"`).
  - **IMPORT:**
    - Every blueprint you have imported, kept on your own computer in `.minecraft/jugcraft/imported_blueprints/`, so the list follows you to any server.
    - Select one to see it, import it again (IMPORT), print it when the server has it (PRINT), or remove it from your list (REMOVE).
    - **+ NEW IMPORT** shows a sheet of tracing paper to paste on, with PASTE, CLEAR and IMPORT stamps.
    - **KIT** on that sheet writes the **design kit** to `.minecraft/jugcraft/blueprint_kit/`, for designing a blueprint by hand or with an AI:
      - `ai_guide.md`: the format, limits, forbidden blocks, "any material" slots, which way the front faces, and tips;
      - `blocks.txt`: every block this game allows, its mods' included, with each state property and its values;
      - `tags.txt`: every block tag a slot may name, with how many blocks it holds;
      - `example.jugbp.json`: a garden shed with a planks slot, ready to import.
- **Item colour in the inventory** (`Blueprint.Kind`; the holograms are unchanged):
  - **blue:** a complete build, either an individual structure or a whole set;
  - **green:** a partial structure, one part of a set such as a single cooling tower;
  - **red:** a player's import.
- **Placing:** hold a blueprint to preview the structure where you look, as far away as any block can be placed plus twice the structure's longest side (the Small Church, 19 blocks tall: 38 blocks further; the large EPR plant, 440 blocks long: 880 further). Every MBS placed whole from one item works the same (see "Placing reach" below).
  - Faint blue means clear; red means something is in the way. The stake's spot is marked amber.
  - Right-click to set the **Survey Stake** there. The build extends away from you, facing you.
- **The hologram:** every player sees the build as a hologram.
  - **blue:** a block is still missing;
  - **amber:** place this by hand (the Tower Core and the depot terminal);
  - **red shell:** a wrong block is in the way.
- **Building:** build it by hand, or let a drone depot build it from the bottom up, a few layers at once, each column bottom first.
- **The stake screen** (right-click the stake) shows:
  - progress and what is still needed, with icons;
  - blocks in the way;
  - **PERSONAL/PARTY**, **ROTATE** and **REMOVE** buttons for the player who placed it (or the party leader). REMOVE takes the stake down without giving the blueprint back.

  Sneak-right-click also switches Personal/Party.
- **Comparator output:** a comparator beside the stake reads how much of the build is in place, like a container's fill: 0 before any block, then 1 to 15 as it fills. It changes at the stake's regular checks (every 2 seconds; every 30 seconds for the largest built-in blueprints), so a lamp or a door can show when a drone build is half or nearly done.
- **A blueprint is used up when it is placed:** when every block is in place the stake pops off and nothing drops, and breaking the stake drops nothing either. Print another at the table (it is free).

## How it works
- **Format:** format 1 `*.jugbp.json` files. Each holds:
  - `name`;
  - `palette`: one character → a block state, or an "any material" slot: a block tag such as `"#minecraft:planks"`;
  - `layers`: bottom first, each a list of text rows, with `.` or space meaning nothing;
  - optional `anchor`: where the stake stands, in front of the build;
  - optional `size`: the build's full size (empty edges count).
- **Format 2** is the same with run-length rows: `12.3A` is twelve empty cells then three of `A` (so palette keys
  can't be digits). A built-in format 2 file may also list `parts`, other built-in blueprints placed inside it
  (`{"id", "at": [x, y, z], "turn": quarter turns clockwise}`); its own layers go over the parts, and a palette entry
  of `minecraft:air` takes a part's block away. The nuclear plants are made this way (tools/nuclear_plants.py).
- **Built-in blueprints** live in `data/jugcraft/blueprint/`, written by `tools/blueprints.py`.
- **Imported blueprints** are saved in `<world>/jugcraft/blueprints/`.
- **Limits** (imports):
  - at most 48 blocks each way;
  - at most 8,192 blocks;
  - at most 262,144 characters;
  - built-in blueprints may be up to 512 blocks each way and 1,500,000 blocks (a whole power plant);
  - forbidden blocks: command, structure, jigsaw, barrier, light, bedrock, spawners and portals.
- **Syncing:** the server sends the whole library to each player on join, and new imports to everyone.
  - Except the large built-ins listed in `data/jugcraft/blueprint/large.txt` (the nuclear plants and their parts):
    each side reads those from its own copy of the jar the first time they are wanted, so they never cross the network.
  - A stake syncs only its blueprint id, rotation, owner and mode.
  - Pasted text goes to the server in 8,000-character parts.
- **Block checks:** a block counts when it is the same block as the one wanted. Stairs and other directional blocks are turned with the blueprint.
- **"Any material" slots:** a palette entry naming a block tag (`"#minecraft:planks"`, `"#minecraft:wool"`) lets any block of that tag count there.
  - The tag must exist and hold blocks, and none of them may be forbidden.
  - The tag's first block is the one shown: in the hologram, the turning preview and the bill of materials.
  - A drone's job for such a cell names the tag. The depot brings any block of it from the packager and sets it the way the drawn block is set: oak stairs drawn facing east become spruce stairs facing east (`BuildJobs.like`). With the endless test supply it brings the first block.
- **The table block:** `BlueprintTableBlock` has `facing` and `part` (`main`/`side`). The side half goes to the player's right; each half removes itself when its partner is gone, and only the main half drops the item (loot `match_block` on `part=main`). Models are generated per half by `tools/blueprints.py` (`table_elements`), with the board tilted 22.5 degrees and its drawing split across the two halves; the item model shows both halves.
- **The turning preview:** `TurntableRaster` (plain Java) fills each block face that shows, shaded by direction and tinted by the block's map colour, then inks the outline and depth steps in white. `BlueprintTurntable` runs it on a worker thread about twelve times a second into a `DynamicTexture` the screen draws.
- **Drone jobs:** the stake is a `BuildJobs.Source`, the interface the Drone Depot already uses.
  - It offers missing positions bottom-up, only to depots that pass `JugcraftParties.mayServe`.
  - It reserves each position for one depot at a time.
  - It never offers positions where a wrong block is in the way.
- **Large builds** (more than 8,192 blocks or 48 across: the plants):
  - Cells are kept compact (`BuildSite`: packed positions, bottom layer first, a sorted copy to find a position, and
    which cells are known done), a few megabytes for a whole plant.
  - Drones are offered work only from the lowest unfinished layer up a few layers, skipping cells known done, so a
    depot's request costs about the same as for a small blueprint. Every 30 seconds the stake checks every block in
    full (to notice broken ones and finish the build).
  - The hologram shows only the cells within 96 blocks of you, from the lowest unfinished layer there up a few layers.
  - The placement preview shows the build's outer skin (boxes of a few blocks) instead of every block.
- **Nuclear equipment in a blueprint** (an equipment anchor block with its facing): drones deliver the equipment's
  item and the stake places it whole, clearing any ground in its body first. The blueprint leaves the body empty.

- **Comparator:** `SurveyStakeBlockEntity` counts the placed blocks at each check and keeps the signal, which `SurveyStakeBlock` reports.
  - A small build (at most 8,192 blocks) is counted in full every 40 ticks. Before, the check stopped at the first missing block.
  - A large built-in build is counted at its 600-tick full check, which already looked at every block.
  - The signal is not saved: the first check after loading sets it.

- **The design kit:** `BlueprintKit` reads the guide and example from `assets/jugcraft/blueprint/` in the jar and makes the block and tag lists from the game's registry, so they match the mods installed. A tag is listed only when it holds an allowed block and nothing forbidden. `BlueprintKitExport` (client) writes them; nothing goes to the server.

## Testing aids (operators only: cheats on, or op level 2)
- `/nuclear build <blueprint> [fuelled]`: builds any blueprint at once where you stand, facing the way you face
  (equipment placed whole, pipes joined, consoles linked to the nearest reactor).
- `/dronetest supplies [off]`: endless power and building blocks for every depot within 96 blocks.
- `/dronetest modules`: fills nearby Tower Cores with 1024 of each module.

## Verification
- **Server game tests (`BlueprintGameTests`):**
  - built-in blueprints load (225-block foundation with one core; the church; rotation keeps every block);
  - a stake offers only its floor layer first, and nothing to a stranger's depot;
  - a finished foundation pops the stake and returns the blueprint.
- **Server game test (`BlueprintGameTests.aComparatorReadsTheBuildsProgress`):** a real comparator beside a foundation stake reads what the ground already holds, then 1 + 14 × placed / total once about half the rest is set. Added on the contributor's fork; passed in run [38631695156](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38631695156) (commit 85490fa9), with every other server and client test.
- **Server game test (`BlueprintGameTests.anyMaterialSlotsTakeAnyBlockOfTheirTag`):** an imported bench with a `#minecraft:planks` slot: birch and spruce planks set by hand count, a stone block does not, the drone job for it names the tag and dark oak planks complete it; stairs of another wood keep the drawn facing and half. Added on the contributor's fork; passed in run [38631695156](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38631695156) (commit 85490fa9), with every other server and client test.
- **Server game test (`BlueprintGameTests.theDesignKitDescribesWhatTheGameAllows`):** the guide in the jar shows the example, tag slots and limits; the example imports as a 5 × 4 × 4 shed with a planks slot; the block list holds stairs with their properties but no command block or air; the tag list holds planks but not the portals tag. Added on the contributor's fork; passed in run [38638745852](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38638745852) (commit e949ef54), with every other server and client test (its first attempt's server job without optional integrations failed in the nuclear plant's radiation test, which this change does not touch, and passed on the one re-run).
- **Server game tests:** import checks (a valid paste is saved; unknown block, forbidden block, unknown block tag, a tag holding forbidden blocks (`#minecraft:portals`), missing palette key, bad JSON and wrong format are each refused with the right message).
- **Client game test (`BlueprintClientGameTests`):** preview and placement from 20 blocks away, hologram, stake screen, table tabs and IMPORT (a pasted blueprint arrives in the client library and the import list), Item Index, chair, Creative Energy Cell.

## Not yet done (from #23)
Nothing listed in #23 remains. Comparator output, "any material" slots and the design kit with its in-JAR AI guide, listed here before, are built (see above).

## World height

If a staked-out blueprint (or, for one that grows like the Drone Tower, its final stage) would reach above the
world height limit, the placement preview marks the clipped cells red and shows a red warning on screen; the
stake-out message repeats it. Nothing above the limit is built.

## Placing reach (October 8 2026)
Justin: an MBS can be placed as far away as any block can, plus two times its longest side (placement/StructureReach.java).
- It replaces the blueprints' old fixed 80 blocks.
- It goes for every MBS placed whole from one item, held in the main hand: blueprints, storage tank kits, the nuclear plant's equipment, power line supports and boxes, the plant's multi-block furniture (wall screens, tables, racks, panels, the copier) and multi-block machines.
- While one is held, the player's block reach carries a modifier of twice its longest side (on the server and the client, so the crosshair outlines the far block and the server's reach check passes); the tooltip says how far.
- The game holds the block reach to 64 blocks, so the modifier stops there. Further out (a turbine-generator reaches 100 blocks further than a block, a lattice tower 112), using the item with nothing under the crosshair places it on the block the player looks at, up to its full reach: the server works the spot out itself (`StructureReach.farTarget`), as blueprints always have, and the preview shows it there.
- The longer reach is only for placing: beyond the ordinary reach a click places the item and never uses the block clicked (no far doors, levers or chests), and no block can be broken out there.

## MBS placement preview and breaking (October 8 2026)
Justin: "I want the MBS to also show what it would look like when you put it down ... once you break an MBS, it just
drops the entire MBS for you." Asked, he chose a see-through copy of the real thing; for tanks, the kit back with its
liquid lost; hand-built structures left as they are; other contributors' decorations left out.
- **Preview** (client/MbsPreview.java): while an MBS item is in the main hand and aimed at a block, a faint copy of
  exactly what it would place stands where it would go, facing the way it would face: a tank kit's whole tank as it
  looks once formed, plant equipment and power line structures as their renderers draw them (a support turned along
  its line, with the wire it would hang), and multi-block furniture, the round table and multi-block machines from
  their own block models. Pale blue where it fits; red where something is in the way, each cell in the way marked with
  a red box. Blocks of the copy are drawn a hair larger than their cells so none of their faces lies in a face of the
  world (no z-fighting). Blueprints keep their own preview. The blocks' data comes from `StructureGhost.Source`
  (MultiFurnitureBlock, ConferenceTableBlock, LargeMachineBlock) and `TankKitItem.formed`/`blocked`.
- **Breaking:** plant equipment, power line structures, multi-block furniture, the round table and multi-block
  machines already came down whole with one item. Now:
  - A tank placed from a kit comes down whole when any block of it is broken (shell, nozzle, ladder, railing, fitting,
    foundation) and drops one empty kit of its size and rating (none in creative); the liquid is lost and the ground
    under the foundation comes back (nuclear/TankKitBreaking.java). The outlet remembers what its kit laid. A tank no
    longer as its kit laid it (a block blown off) forgets its kit and breaks block by block, so nothing comes back
    twice. Tanks a kit placed before this are recognised once by their layout.
  - Multi-block furniture and the round table also come down whole when a part goes by an explosion or a command
    (before, only a player's break did that), so no part is left on its own and only one item drops.
- Hand-built structures (tanks built from plates, drone pads, the drone control screen and holo table) still break a
  block at a time, as Justin chose.
