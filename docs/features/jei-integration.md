# JEI integration

Status: implemented in source (PR #35); **not yet tried in a client with JEI installed**. It compiles against the JEI API in CI. JEI is not in the test runs, so nothing verifies it at runtime yet.
Proposal issue: none. The owner asked for "JEI/EMI recipes" on 30 September 2026. EMI has no build for Minecraft 26.3 yet (checked on its maven), so this is JEI only.
Owner: @jimbozoomer-byte
Target milestone and tier: all tiers (it only displays recipes).
Primary specialty and supported player role: information and quality of life

## Player experience
With [JEI](https://github.com/mezz/JustEnoughItems) installed:
- every Jugcraft processing machine has its own recipe page, with inputs (tags show every matching item), the result, byproducts with their chances, and the time;
- the machine is listed as the "catalyst", so looking up a machine shows what it makes.

Machines covered: crusher, arc furnace, alloy smelter, metal press, wire drawer, circuit assembler, pulverizer, ore washer, sieve, sawmill, coke oven, steel foundry and tree farm. Crafting-table recipes already show in JEI's own crafting page.

## How it works
- **Optional:** JEI is a compile-time API dependency only (`compileOnly`, maven.blamejared.com). The plugin class is reached only through JEI's `jei_mod_plugin` entrypoint, so Jugcraft runs normally without JEI.
- **Recipes:** these are server data in 26.x and are not sent to clients. The plugin reads `assets/jugcraft/recipe_view.json` instead, which `tools/recipe_view.py` generates from the same tables as the recipe data.
  - A data pack that changes machine recipes on a server is not reflected in JEI.

## Dependencies and assets
- JEI API 31.8.0.49 for 26.3 (MIT), compile-time only.
- No JEI code or assets are shipped.

## Verification
- `tools/check_mod_data.py` passes.
- CI compiles the plugin against the JEI API.
- Not run: a client with JEI installed. Nothing is play-tested.

## Rollout and open questions
- Add EMI when it publishes a 26.3 build.
- Syncing server recipes to JEI (so data packs show) would need a small packet.
