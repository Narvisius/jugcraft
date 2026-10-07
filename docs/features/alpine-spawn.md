# Alpine Spawn

Status: in progress on branch `claude/alpine-spawn`, which is stacked on the agriculture pull requests, #53 and #80.
- Part 1 (the biome, its placement, villages and the world start) passed CI.
- The start search was then changed to prefer alpine villages (see Results).
- Part 2 (larch trees, and the start beside an alpine village) passed CI.
- On 2 October the owner asked for a larger biome: every meadow and the cool plateau's forest and taiga now become Alpine Spawn. That passed CI too.
- Later parts are planned (see Rollout). **Not yet played.**

Proposal issue: none. On 2 October 2026 the owner asked for an "Alpine Spawn" biome:
- a seasonal alpine biome where the world starts;
- "pretty large";
- one that should "encourage or if possible always have villages";
- one that "from the get go offers extra seasonal support in as many ways as we can".

They chose to build it after the seasons upgrade (#80).
Owner: @jimbozoomer-byte
Target milestone and tier: the start of every new world (Discovery).
Primary specialty and supported player role: exploration and settling; every player starts here.

## Player experience
- **New worlds start at an alpine village in Alpine Spawn**: a large, cool alpine meadow on mountain plateaus, with scattered larches and spruces. It takes the place of every vanilla meadow and of the cool plateau's forest and taiga, so vanilla meadows no longer generate in new chunks. Only when there is no alpine village within reach does the world start elsewhere in the biome.
- **Villages are common.** Alpine villages use vanilla's taiga village pieces (spruce houses, and with #53 a Retro Game Shop) on their own grid of 16 chunks; vanilla's grid is 34.
- **Larches (part 2)** are conifers that change with the seasons:
  - green in spring and summer, gold in autumn, then bare twigs in winter, budding green again in spring;
  - each block turns within a week either side of the date, so a crown changes gradually and neighbouring trees differ;
  - they drop larch saplings and come with a full wood set: log, wood, stripped log and wood, planks, stairs, slab, fence and fence gate, in reddish larch colours.
- **Seasons from day one.** Its grass and leaves follow the season colours (#80), and winter snow covers it when `seasons.snow` is on.
- Like vanilla's meadow it has mineshafts, strongholds nearby, trial chambers, abandoned camps and mountain ruined portals. It has donkeys, rabbits and sheep, and foxes as well.

## Connections
- Input producer: world generation. Vanilla's meadows, and the cool plateau's forest and taiga, become Alpine Spawn.
- Output consumer: settling. It is the world's starting area, with villages to trade with (vanilla trades, and the Retro Trader with #53). Everything vanilla's meadow gives is here too: ores, stone and wood.
- Technology connection:
  - Larch logs saw into planks in the sawmill, 6 per log with sawdust, like every other wood.
  - Larch saplings grow 6 logs in the tree farm, like vanilla saplings.
  - Larch wood joins vanilla's wood tags (planks, logs that burn, wooden stairs, slabs, fences and gates), so every vanilla wood recipe and fuel use accepts it.
- Magic connection: none yet.
- Reachable entry path: it is where the world starts, so nothing is needed to reach it. Larch saplings drop from the larches there.
- Required vs optional connections; trade and solo routes: villages and larch wood are optional. Nothing in it is required for progression.
- How this stays useful without other branches: it needs none.

## Balance and automation
- Larch wood is ordinary wood: 4 planks per log by hand, 6 in the sawmill (as for every wood). The tree farm turns 1 sapling into 6 logs, returning the sapling, as for vanilla saplings. There is no new conversion and no loop.
- The needles' look is cosmetic: drops, decay and fire behave as vanilla spruce leaves in every season. Bare needles still drop saplings and sticks.
- Villages are more common inside the biome only. Villagers already exist everywhere, so trading gains no new route.

## Multiplayer and persistence
- **Server authority.** The server places the biome (world generation) and moves the world spawn, once, on a new world's first start (game time 0), before anyone joins. It uses vanilla's `/setworldspawn`, so saving and respawning are vanilla's.
- **The start search.**
  - It looks for the alpine village nearest the origin, up to 25 cells of the alpine village grid away (6,400 blocks). Alpine villages only generate where their start piece stands in Alpine Spawn, but a village at the biome's edge can reach outside it. The start is the nearest point within 96 blocks of the village that has Alpine Spawn under it and 16 blocks around it (or at least under it). The start may be thousands of blocks from the origin; nothing depends on the origin.
  - With no alpine village in reach, it finds the nearest Alpine Spawn within 6,400 blocks, sampled every 64 blocks, and moves inward until the biome reaches 48 blocks on all four sides.
  - This runs once, at world creation, and only when the Overworld uses vanilla's multi-noise biomes (not superflat or single-biome worlds).
  - If there is no Alpine Spawn in reach, the spawn stays vanilla's (logged).
- **Larch needles.**
  - The server decides the look from its own season day (`JugcraftSeasons.today()`, #80), never a client's clock. Clients see block states, as for any block.
  - Needles placed by a player or grown from a sapling take today's look at once; since the biomes branch, so do the needles of generated larches (the `jugcraft:seasonal_leaves` tree decorator, [seasonal-forests.md](seasonal-forests.md)). After that they catch up on random ticks. A needle that changes on its tick also brings the needles it touches up to date (at most 128, in loaded chunks only), so a whole tree turns within moments of its first tick near a player. Chunks nobody is near keep their old look until someone comes.
  - Before the biomes branch, world generation placed green needles and they caught up as above: a new world made in autumn showed its larches turn gold within seconds of loading near the player.
  - The look is saved as a block state (`season`) and survives restarts.
  - The cost is one date lookup per random tick of a needle block, which is bounded by vanilla's random tick rate. The catch-up runs only when a needle changes and visits at most 128 needles.
  - With seasons off (`seasons.mode=off`) every needle turns green.
- **Settings:**
  - `alpine_spawn.enabled=false` stops new Alpine Spawn generation. The larch's hand and sawmill recipes stop only when `biomes.enabled` is off too, since larches, tamaracks and fallen larches also grow in Jugcraft's biomes ([trees-batch-1.md](trees-batch-1.md), TREES.md rule 4). Meadows and the cool plateau's forest and taiga are vanilla's again in new chunks, and the spawn move is skipped. The biome and every larch block and item stay registered, so old chunks and inventories keep them.
  - `alpine_spawn.start=off` keeps vanilla's spawn but still generates the biome.
- **Existing worlds:**
  - Old chunks are never rewritten. New chunks where meadows or cool plateau forest and taiga would have generated become Alpine Spawn, which may leave seams next to old chunks.
  - An existing world's spawn never moves (its game time is past 0).

## Dependencies and assets
- **Hook.** The biome is placed through #53's `mixin/OverworldBiomeBuilderMixin`, which now also wraps the builder's output, because Fabric API cannot place new Overworld biomes.
- **Copied layouts.** The biome's layout, features and mobs follow vanilla 26.3's meadow; the village uses vanilla's taiga pieces by name; the larch uses vanilla's spruce foliage shape and spruce-leaf drop chances. No Mojang file is copied.
- **Larch textures.** These are drawn by code: the sapling in `tools/larch_textures.py`; since the [wood repaint](wood-repaint.md), the bark, log ends, stripped wood, planks and the three needle looks in `tools/wood_style.py`. No texture is read, traced or recoloured.
- **Shared code.** The larch wood set reuses the chestnut's wood-set data and code. `WOOD_SETS` in `tools/agriculture.py` and `JugcraftAgriculture.registerWoodSet` now generate and register both woods. The chestnut's generated files are unchanged.
- No new dependencies.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py` checks:
  - Java's placement and spawn numbers match `tools/alpine.py`;
  - the biome file's climate and its own placed features;
  - that no biome attribute has an argument without a modifier (the part 1 failure);
  - the village structure's biome tag and its grid;
  - that the village search reaches as far as the biome search;
  - that the trees pick larches and spruces;
  - the feature switch;
  - the start option;
  - the biome's name;
  - that it has seasons.
- `python3 tools/check_mod_data.py` also checks the larch:
  - the larch's season days in Java (`JugcraftAgriculture.LARCH_LEAVES`, read by the shared `SeasonalLeavesBlock`) and the jitter match `TREES` in `tools/agriculture.py`;
  - each fixed season mode shows only its own look on every block, whatever the jitter (spring and summer green, autumn gold, winter bare);
  - the needles' blockstate covers every look;
  - the wood set is registered.
- Server game tests (`AlpineGameTests`):
  - no meadow is left in the Overworld climate table, Alpine Spawn has more entries than vanilla had meadows (so the cool plateau's forest and taiga were taken), and lowland forest and taiga remain (the numbers are logged);
  - the meadow rule and the plateau rule on their own;
  - the replacement rule on its own;
  - the biome has seasonal colours, winter snow and the mountain tags, and is cooler than a meadow;
  - the alpine village is in `#minecraft:village` and `#jugcraft:alpine_villages`, generates in Alpine Spawn, and has a grid tighter than vanilla's;
  - larch needles, natural and placed, follow autumn, winter, spring, summer and off on random ticks, and needles placed in winter start bare;
  - out-of-date needles catch up together: one random tick turns a whole 4 by 4 crown;
  - the turn is gradual: a 16 by 16 patch turns gold over at least a week (logged);
  - a sapling grows a larch of at least 7 logs and 10 needles, all bare when grown in winter (logged);
  - an axe strips a larch log; the wood is in vanilla's tags and burns.
- `PixelHollowsGameTests.overworldFeatureOrderHasNoCycle` now includes Alpine Spawn's features too.
- Client game test (`AlpineClientGameTests`):
  - creates a real, normally generated world (seed `jugcraft`, structures on);
  - checks that the player starts in Alpine Spawn, at an alpine village (within 128 blocks of its start chunk);
  - logs the biome's share of the 2 km square around the start and of a 16 km square around the origin;
  - takes screenshots of the start, of the view from 40 blocks above it, and of three larches grown in spring, autumn and winter.
- Not run:
  - other seeds;
  - how often the search finds no alpine village;
  - a dedicated server's first start;
  - a two-client test;
  - needles turning over real days (only the season modes);
  - performance of the one-off start search.

### Results
- **Part 1, run 36950970247 (base) and run [36952055544](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36952055544) (part 1 fixed, commit 50fb78a3):** green.
  - The first part 1 run failed to load the biome: its copied mob spawn list lacked vanilla's `"modifier": "overlay"`. Fixed, and the checker now catches it.
  - All 296 required server game tests passed.
  - The Overworld climate table has 7,595 entries: 100 are now Alpine Spawn, 80 meadows remain, and no cool meadow is left.
  - The client test's world (seed `jugcraft`) started at 288, 120, 1200 in Alpine Spawn.
  - **But the nearest village of any kind was 781 blocks away** (-352, 768). The start search looked for the nearest village of any kind and rejected it when it was not in the biome, so the world started without one.
  - The start screenshot shows an alpine meadow with spruces, flowers and the autumn grass colours. The overview screenshot showed the ground, because the creative player fell back down before it was taken.
- **Changes after part 1:**
  - the start search now looks for alpine villages only, from the origin outward;
  - the client test now fails if the world does not start at one;
  - the overview is taken standing on a barrier.
- **Part 2, run [36953457067](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36953457067) (commit e1081030): server tests green, client test failed.**
  - All 300 required server game tests passed, including the larch tests. A 16 by 16 patch of needles turns gold between season days 261 and 275. A larch grown in winter had an 8-block trunk and 58 needles, all bare.
  - The client test's larch scene: grown in spring, 21 needles, all green; in autumn, 30, all gold; in winter, 33, all bare. The screenshot shows the three side by side.
  - **The start found an alpine village** at 3216, 2912 (the world started there; the start screenshot shows its spruce houses on terraced ground). **But the player stood in `minecraft:forest`**: the village's locate position, a corner of its start chunk, lay just outside the biome. The test failed on that. The start now moves to the nearest Alpine Spawn ground beside the village (awaiting CI).
  - **The biome's size:** 2.5% of the 2 km square around the start (107 of 4,225 samples) and 0.8% of a 16 km square around the origin (32 of 4,225). That is smaller than "pretty large"; see the open questions.
  - The larches in the start screenshot were still green on 2 October, as world generation places them, which prompted the catch-up above.
- **Start fix and catch-up, run [36954872048](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36954872048) (commit 4db60823): green.**
  - All 301 required server game tests passed, including the catch-up test. A larch grown in winter had a 7-block trunk and 18 needles, all bare.
  - The world (seed `jugcraft`) started at 3224, 97, 2928 in Alpine Spawn, 17 blocks from the alpine village at 3216, 2912.
  - Alpine Spawn covered 2.2% of the 2 km square around the start and 0.8% of a 16 km square around the origin.
  - The larch scene: spring 58 needles, all green; autumn 34 gold (and 6 green from the spring tree's crown, which reached into its counting box; the trees are now further apart); winter 34, all bare.
  - The overview screenshot shows autumn-coloured woods with a gold larch. The start screenshot faced a terrace wall; it is now taken from a little higher.
- **Larger biome, run [36956117976](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36956117976) (commit 134190fb): green.**
  - All 301 required server game tests passed.
  - The plateau hook applied: the climate table's 7,595 entries now have 220 Alpine Spawn (part 1 had 100) and no meadow. 568 forest and 332 taiga entries remain elsewhere.
  - **Alpine Spawn now covers 1.9% of a 16 km square around the origin, about 2.4 times its 0.8% before.** It covers 2.8% of the 2 km square around the start.
  - The world (seed `jugcraft`) now started much nearer the origin: at an alpine village at 320, 106, 512, in Alpine Spawn.
  - The start screenshot shows an alpine meadow with spruces and poppies, and **gold larches**: the catch-up turned them on a freshly made world in October. The overview shows grassy alpine slopes with gold larches below snowy spruce peaks.
  - The larch scene: spring 30 needles, all green; autumn 17, all gold; winter 53, all bare. A village house and a villager stand behind them.

## World and event applicability
- **Biome fit.** A cool, wet mountain meadow takes vanilla's meadow climate and the cool plateau beside it, so it borders the mountain slopes, the cool plateau's old-growth taiga and lowland forest and taiga.
- **Existing worlds.** Old chunks are untouched; new chunks may form seams with old chunks.
- **Seasons.** The biome is in `#jugcraft:has_seasons` and `#jugcraft:has_winter_snow`, and follows #80's rules: activation, deactivation and nothing earned is lost. The larch's look follows the same clock; switching seasons off turns every needle green, and no item or block is lost or changed in kind.
- **Hemispheres.** The needles follow the season day, so `seasons.hemisphere=south` turns them gold in the southern autumn (April).

## Rollout and open questions
- **Part 1:** the biome, its placement, alpine villages and the world start.
- **Part 2:** larch trees and the larch wood set; the start at an alpine village.
- **Larger biome (this change):** every meadow, and the cool plateau's forest and taiga.
- **Planned parts, all seasonal:**
  - **Seasonal flowers:** crocuses that bloom in spring, edelweiss and gentians in summer. Each is green the rest of the year.
  - **Bilberries:** bushes that ripen in late summer. When seasons are off, they ripen on a timer instead.
  - **Alpine winter:**
    - snow on by default in this biome only;
    - a snow line that comes down in autumn and melts back up the slopes through spring;
    - lakes that freeze in winter and thaw back to water.
- **Open questions:**
  - Should other mods' world presets or data packs that change the Overworld also get the spawn move? Today only the vanilla multi-noise Overworld does.
  - Would a pillager outpost near the start be too harsh? Vanilla's meadows allow outposts, and Alpine Spawn joins `#minecraft:is_mountain`. Villages exclude outposts nearby.
  - Is the biome large enough now? It covers about 1.9% of the land (seed `jugcraft`), up from 0.8%. It could grow further by taking more of the cool plateau (its old-growth taiga) or the windswept hills, at the cost of more vanilla terrain.
  - Should larches also grow outside Alpine Spawn (for example in taiga)? Today they are unique to it.
