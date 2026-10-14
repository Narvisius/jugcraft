# JEI integration

Status: implemented in source (PR #35); **not yet tried in a client with JEI installed**. The plugin as merged compiled against the JEI API in CI; Concordance Chunk A's information pages (`addAcquisitionInfo`) have not been compiled yet. JEI is loaded in the default development and test runs (below), but no test opens JEI or checks its pages, so nothing verifies the plugin at runtime yet.
Proposal issue: none. The owner asked for "JEI/EMI recipes" on 30 September 2026. EMI has no build for Minecraft 26.3 yet (checked on its maven), so this is JEI only.
Owner: @jimbozoomer-byte
Target milestone and tier: all tiers (it only displays recipes).
Primary specialty and supported player role: information and quality of life

## Player experience
With [JEI](https://github.com/mezz/JustEnoughItems) installed:
- every Jugcraft processing machine has its own recipe page, with inputs (tags show every matching item), the result, byproducts with their chances, and the time;
- the machine is listed as the "catalyst", so looking up a machine shows what it makes.

Machines covered: crusher, arc furnace, alloy smelter, metal press, wire drawer, circuit assembler, pulverizer, ore washer, sieve, sawmill, coke oven, steel foundry and tree farm.

Crafting-table recipes show in JEI's own crafting page when JEI's recipe sync reaches the client: always in a single-player world, and on a server only if JEI is installed there too. A server without JEI sends none, and JEI falls back to vanilla's own recipes (read in JEI's 31.9 source; the pin is 31.8.0.49).

**Information pages** (JEI's Information tab) cover what no recipe shows: the alchemy ingredients' properties and the mortar, how a Resonant Ring is forged, and, since Concordance Chunk A, where the things only a ritual or a lair boss gives come from (the Adept's Wand, and Vesperine's and Madame Tatterlace's loot), with the default data's chances ([concordance-first-loadout.md](concordance-first-loadout.md)).

## How it works
- **Optional:** the plugin compiles against JEI's API only (`compileOnly`, maven.blamejared.com). The JEI mod itself is an optional entry of the framework lock, which `build.gradle` loads at runtime in development and test runs unless `-PjugcraftOptionalIntegrations=false` (as CI's "optional integrations absent" job runs). The plugin class is reached only through JEI's `jei_mod_plugin` entrypoint, so Jugcraft runs normally without JEI.
- **Recipes:** these are server data in 26.x and are not sent to clients. The plugin reads `assets/jugcraft/recipe_view.json` instead, which `tools/recipe_view.py` generates from the same tables as the recipe data.
  - A data pack that changes machine recipes on a server is not reflected in JEI; nor is one that changes a lair boss's loot table or the Adept's Attunement, which is why those information pages say "with the default data".

## Dependencies and assets
- JEI 31.8.0.49 for 26.3 (MIT): its API at compile time; the mod optional (`distribution/frameworks.lock.json`).
- No JEI code or assets are shipped.

## Verification
- `tools/check_mod_data.py` passes.
- CI compiled the plugin against the JEI API up to the merged main; Concordance Chunk A's `addAcquisitionInfo` is not compiled yet (no build here can; CI will).
- `check_acquisition` (`tools/check_mod_data.py`) ties the information pages to `tools/acquisition.py` and the data on disk. It reads data only, so a drop coded in Java is not seen ([concordance-first-loadout.md](concordance-first-loadout.md), Known limits).
- Not run: a client with JEI installed. Nothing is play-tested.

## Rollout and open questions
- Add EMI when it publishes a 26.3 build.
- Syncing server recipes to JEI (so data packs show) would need a small packet.
