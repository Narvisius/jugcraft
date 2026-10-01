# Blueprint System (Blueprint Table, import, Survey Stake hologram, drone building)

Status: first slice implemented on the drone branch; the full design is proposal #23
Owner: Narvisius

## Player experience
- **Craft a Blueprint Table** (3 paper, 4 planks, 1 copper ingot) and right-click it.
  - **LIBRARY** lists every blueprint the server knows:
    - the mod's own structures (Drone Tower Foundation, Arc Furnace, and the Small Church as a test);
    - then everything players have imported.

    Each blueprint shows a front view, its size, its block count and the main materials. **PRINT** gives you the blueprint for free.
  - **IMPORT:** paste the text of a blueprint (`.jugbp.json`, for example one an AI designed for you) and press IMPORT.
    - The server checks it and explains any problem in plain words: unknown block, too big, forbidden block, bad JSON.
    - If it passes, it is saved with the world and appears in every player's LIBRARY.
- **Placing:** hold a blueprint to preview the structure where you look, up to 80 blocks (five chunks) away.
  - Faint blue means clear; red means something is in the way. The stake's spot is marked amber.
  - Right-click to set the **Survey Stake** there. The build extends away from you, facing you.
- **The hologram:** every player sees the build as a hologram.
  - **blue:** a block is still missing;
  - **amber:** place this by hand (the Tower Core and the depot terminal);
  - **red shell:** a wrong block is in the way.
- **Building:** build it by hand, or let a drone depot build it, one layer at a time from the bottom.
- **The stake screen** (right-click the stake) shows:
  - progress and what is still needed, with icons;
  - blocks in the way;
  - **PERSONAL/PARTY**, **ROTATE** and **REMOVE** buttons for the player who placed it (or the party leader).

  Sneak-right-click also switches Personal/Party.
- **When every block is in place,** the stake pops off and gives the blueprint back.

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
- **Client game test (`BlueprintClientGameTests`):** preview and placement from 20 blocks away, hologram, stake screen, table LIBRARY and IMPORT (a pasted blueprint arrives in the client library), Item Index, chair, Creative Energy Cell.

## Not yet done (from #23)
- "Any material" slots and tags.
- Comparator output.
- The design kit and in-JAR AI guide.

## World height

If a staked-out blueprint (or, for one that grows like the Drone Tower, its final stage) would reach above the
world height limit, the placement preview marks the clipped cells red and shows a red warning on screen; the
stake-out message repeats it. Nothing above the limit is built.
