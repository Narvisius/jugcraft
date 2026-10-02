# Blueprint System (Blueprint Table, import, Survey Stake hologram, drone building)

Status: first slice implemented on the drone branch; the full design is proposal #23
Owner: Narvisius

## Player experience
- **Craft a Blueprint Table** (3 paper, 4 planks, 1 copper ingot) and right-click it.
  - It is a drafting station two blocks wide (an MBS placed by one item, like a bed; it needs the block to your right free): steel trestle legs, a tilted drawing board with a blueprint taped on, a drafting arm and scales, a pencil ledge, a shelf of drawings and rolled plans, and a swing-arm lamp. Breaking either half breaks the whole table and drops it once.
  - The screen is a drafting sheet on a steel board, with folder tabs (below). The left is the drawing register; the selected blueprint turns slowly in the middle (drawn in blueprint blue with white ink, one turn every 14 seconds) over a dimension line, with its bill of materials and a title block (title, type, block count, source) on the right. The red **PRINT** stamp gives you the blueprint for free.
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
- **Item colour in the inventory** (`Blueprint.Kind`; the holograms are unchanged):
  - **blue:** a complete build, either an individual structure or a whole set;
  - **green:** a partial structure, one part of a set such as a single cooling tower;
  - **red:** a player's import.
- **Placing:** hold a blueprint to preview the structure where you look, up to 80 blocks (five chunks) away.
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
- **A blueprint is used up when it is placed:** when every block is in place the stake pops off and nothing drops, and breaking the stake drops nothing either. Print another at the table (it is free).

## How it works
- **Format:** format 1 `*.jugbp.json` files. Each holds:
  - `name`;
  - `palette`: one character → a block state;
  - `layers`: bottom first, each a list of text rows, with `.` or space meaning nothing;
  - optional `anchor`: where the stake stands, in front of the build.
- **Built-in blueprints** live in `data/jugcraft/blueprint/`, written by `tools/blueprints.py`.
- **Imported blueprints** are saved in `<world>/jugcraft/blueprints/`.
- **Limits:**
  - at most 48 blocks each way;
  - at most 8,192 blocks;
  - at most 262,144 characters;
  - forbidden blocks: command, structure, jigsaw, barrier, light, bedrock, spawners and portals.
- **Syncing:** the server sends the whole library to each player on join, and new imports to everyone.
  - A stake syncs only its blueprint id, rotation, owner and mode.
  - Pasted text goes to the server in 8,000-character parts.
- **Block checks:** a block counts when it is the same block as the one wanted. Stairs and other directional blocks are turned with the blueprint.
- **The table block:** `BlueprintTableBlock` has `facing` and `part` (`main`/`side`). The side half goes to the player's right; each half removes itself when its partner is gone, and only the main half drops the item (loot `match_block` on `part=main`). Models are generated per half by `tools/blueprints.py` (`table_elements`), with the board tilted 22.5 degrees and its drawing split across the two halves; the item model shows both halves.
- **The turning preview:** `TurntableRaster` (plain Java) fills each block face that shows, shaded by direction and tinted by the block's map colour, then inks the outline and depth steps in white. `BlueprintTurntable` runs it on a worker thread about twelve times a second into a `DynamicTexture` the screen draws.
- **Drone jobs:** the stake is a `BuildJobs.Source`, the interface the Drone Depot already uses.
  - It offers missing positions bottom-up, only to depots that pass `JugcraftParties.mayServe`.
  - It reserves each position for one depot at a time.
  - It never offers positions where a wrong block is in the way.

## Testing aids (development runs only)
- `/dronetest supplies [off]`: endless power and building blocks for every depot within 96 blocks.
- `/dronetest modules`: fills nearby Tower Cores with 1024 of each module.

## Verification
- **Server game tests (`BlueprintGameTests`):**
  - built-in blueprints load (225-block foundation with one core; the church; rotation keeps every block);
  - a stake offers only its floor layer first, and nothing to a stranger's depot;
  - a finished foundation pops the stake and returns the blueprint.
- **Server game tests:** import checks (a valid paste is saved; unknown block, forbidden block, missing palette key, bad JSON and wrong format are each refused with the right message).
- **Client game test (`BlueprintClientGameTests`):** preview and placement from 20 blocks away, hologram, stake screen, table tabs and IMPORT (a pasted blueprint arrives in the client library and the import list), Item Index, chair, Creative Energy Cell.

## Not yet done (from #23)
- "Any material" slots and tags.
- Comparator output.
- The design kit and in-JAR AI guide.

## World height

If a staked-out blueprint (or, for one that grows like the Drone Tower, its final stage) would reach above the
world height limit, the placement preview marks the clipped cells red and shows a red warning on screen; the
stake-out message repeats it. Nothing above the limit is built.
