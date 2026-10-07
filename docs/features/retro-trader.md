# Retro Game Shop and Retro Trader

Status: implemented in source; **not yet played**. Builds and passes its game tests in CI (see Verification); the client test screenshots the trader, his cabinet and the shop.
Proposal issue: none. The owner supplied the proposal ("Retro Game Shop & Retro Trader (Pixel Hollows companion)") with a reference picture of the trader and asked for it to be implemented on 1 October 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: Discovery. Emeralds and a compass are all a player needs.
Primary specialty and supported player role: exploration and trade, supporting the [Pixel Hollows](pixel-hollows.md).

## Player experience
Every new village has one small **Retro Game Shop**: an oak storefront with a false front and a lit "Retro Games" sign, display windows, a chequered floor, shelves of chunky cartridges, a counter, pixel lamps and a glowing **arcade cabinet**. The villager who takes that cabinet becomes the **Retro Trader**: a bearded villager with brown hair, black glasses and green eyes, in a red-and-black flannel shirt over a white tee with a silver cross, backpack straps, navy plaid trousers and black-and-white sneakers (drawn from the owner's reference picture).

He:
- **always offers a Pixel Hollows map**, which is how most players will first find the rare cave;
- **sells a little of the cave's palette** (circuitstone, pixel lamps, a few shards), so builders can try it before the trip;
- **buys pixel shards back**, which gives explorers a reason to return.

Everything is original: no real shop names, logos, colours, uniforms, games or franchise characters.

## Connections
- Existing input producer: vanilla villages, villagers, emeralds and compasses; pixel shards from the Pixel Hollows.
- Existing output consumer: Pixel Hollows exploration (the map), cozy building (decor), the player economy (the buyback).
- Technology connection: none directly; the cave he points to holds the extra copper, redstone and tin.
- Magic connection: none.
- Reachable entry path (prove no circular unlock):
  1. Find a generated shop (its cabinet needs no cave items), pay 12 emeralds and a compass for the map, and travel to the cave.
  2. After visiting the cave, craft an **arcade cabinet** (2 pixel shards, planks, a glass pane and redstone). Any unemployed villager takes the profession at it: the solo, anywhere route.
  The generated shop never needs cave items, and the cabinet recipe is a later convenience.
- Which connections are required vs optional; trade and solo routes:

| Connection | Status |
| --- | --- |
| Pixel Hollows biome | Required (the map points to it) |
| Vanilla villages, emeralds, compass | Required, already exist |
| Finding a shop | Optional: the cave can be found by exploring, and the cabinet makes a trader anywhere |
| Trade between players | Optional: shards and maps can be traded |
| Solo route | Fully soloable |

- How this specialty stays useful without mastering every other branch: a map seller and a small decor shop; nothing requires him.

## Balance and automation
- **One shop in every new village** (owner request, replacing the proposal's about 1 in 6): the shop joins the houses pool of all five village types (plains, desert, savanna, snowy, taiga) with weight 1, and `world/RetroShopPlacement` makes it the first choice for each of a new village's house slots until it is placed, then removes it, so there is exactly one. Village houses are built inside their street's own plot, and many plots are too small for the 9 by 8 shop (in a desert village only 12 of the 38 house slots can hold it), so now and then a village's layout has no slot with room. Then the village is laid out again from its start piece (`mixin/JigsawPlacementMixin` runs the placer again), up to 32 layouts, drawing on the same seeded random source, so a world seed still makes the same village. Zombie villages (about 2% of villages, abandoned, with zombie villagers) get none. The storefront is the same oak shop in every village type.
- **Trades** are data (26.1+): `data/jugcraft/villager_trade/retro_trader/*.json`, one tag per level (`#jugcraft:retro_trader/level_1` …) and a trade set per level (`data/jugcraft/trade_set/retro_trader/level_<n>.json`) that the profession names. All are generated from `TRADES` in `tools/pixel_hollows.py`. The last column is each trade's `reputation_discount` (the old price multiplier):

| Level | Trade | Uses per restock | XP | Price multiplier |
| --- | --- | --- | --- | --- |
| Novice | 12 emeralds + compass → Pixel Hollows Map (**always offered**) | 1 | 5 | 0.2 |
| Novice | 1 emerald → 8 circuitstone | 12 | 1 | 0.05 |
| Apprentice | 6 pixel shards → 1 emerald (buyback) | 12 | 5 | **0** |
| Apprentice | 3 emeralds → 1 pixel lamp | 4 | 5 | 0.05 |
| Journeyman | 4 emeralds → 2 pixel shards | 3 | 10 | 0.05 |

  The novice and apprentice trade sets each draw two trades from a tag of exactly two, so all of them always appear. Expert and master add nothing.
- **No profit loop.** The proposal assumed discounts only lower what the trader charges; in fact reputation (curing, trading) and Hero of the Village also lower what he asks for in his *buy* trades. With ordinary discounts the buyback could fall to 1 shard → 1 emerald while shards cost 1 emerald for 2: buy 2, sell 2, gain an emerald. So the buyback's `reputation_discount` is 0 (reputation never discounts it), and Hero of the Village V (the strongest in survival) lowers it only to 3 shards → 1 emerald. The cheapest a shard can be bought is 1 emerald for 2 (0.5 each); the most selling one can pay is 1/3. The data checker and the game test `retroTraderHasNoProfitLoop` both check this.
- **Stock stays small and restock-limited,** so the cave remains the main source of the palette and ores. Automation is limited to vanilla trading.

## The Pixel Hollows map
- The trader sells a **Pixel Hollows Map**: an unmarked map. Use it and the server looks for the nearest Pixel Hollows from where you stand; the map becomes a normal explorer map (scale 2, tracking) centred on the cave, with an original Pixel Hollows marker (`jugcraft:pixel_hollows` map decoration). Because the biome is underground, the map's name gives the depth: "Pixel Hollows, around Y −16".
- **Server-only, bounded search** (`PixelHollowsMaps`): the game's own biome search (the one behind `/locate biome`), which samples the biome noise and loads or generates no chunks, in columns 64 blocks apart out to 2,048 blocks, every 32 blocks of height, starting at Y −16. At most 65 × 65 columns of 12 samples. It runs only when a player uses the map, off the server thread so a long search does not stall the game; the marked map is then made on the server thread from one of the player's unmarked maps. A 5-second cooldown stops repeated searches. It returns at once in a dimension or world where the biome cannot generate.
- **No cave in reach:** the player is told ("No Pixel Hollows within 2,048 blocks of here") and the map stays unmarked, to be used somewhere else. It never becomes a blank or wrong map.
- **Change from the proposal:** the proposal searched once when the trade was created, from the trader's village, and showed a sold-out trade when nothing was found. In 26.1+ trades are data files and their items are made by item modifiers, so a search at trade time would need a new item-modifier type; searching when the map is used is simpler, works anywhere the buyer takes it, and still never sells a dud. Open question answered: there is nothing to retry on restock.
- Purchases go through vanilla trading, validated by the server.

## Multiplayer and persistence
- Server authority: vanilla trading and villager AI, no custom packets. The map search runs on the server, on a background thread.
- The arcade cabinet cannot be pushed by pistons: moving one half of a two-block cabinet would break the other and drop a free cabinet. Breaking either half in creative drops nothing.
- Ownership: vanilla villager rules; restock and prices per villager.
- **Stable IDs:** `jugcraft:retro_trader` (profession), `jugcraft:arcade_cabinet` (block, item and job site point of interest), `jugcraft:pixel_hollows_map` (item), `jugcraft:pixel_hollows` (map decoration), the trades `jugcraft:retro_trader/*` and trade sets `jugcraft:retro_trader/level_1`–`3`, the shop template `jugcraft:village/plains/retro_game_shop`, the sound `jugcraft:entity.villager.work_retro_trader`.
- A marked map is an ordinary vanilla filled map; nothing about it depends on Jugcraft after it is made.
- **Existing worlds:** villages already generated are unchanged; shops appear only in new ones.
- **Disable switch:** `retro_trader.enabled=false` stops new shops, the map trade (for trades made from then on) and the cabinet recipe. The profession, the cabinet, the map item, existing traders and their saved offers stay. Trades that sell cave goods follow the `pixel_hollows` switch, and with `pixel_hollows` off no new shops are placed either, so no village gets a trader with nothing to sell.

## Dependencies and assets
- Fabric API's `PoiHelper` registers the cabinet as a job site; trades are vanilla data. Fabric API has no village pool API, so a mixin accessor (`mixin/StructureTemplatePoolAccessor`) adds the shop to the five houses pools when the server starts, and three small mixins make it one per village: `StructureTemplatePoolMixin` reorders a houses pool's shuffled candidates, `JigsawPlacementMixin` wraps the jigsaw placer's run (marking where each structure begins and ends, and laying a village out again when it has no shop) and `PoolElementStructurePieceMixin` notes when the shop is placed.
- Original assets: the trader's 64×64 villager overlay (also used for his zombie form) and the cabinet's textures are drawn by `tools/pixel_hollows_textures.py`; the cabinet model is in `tools/retro_models.py`; the shop is written by `tools/retro_game_shop.py` from vanilla blocks and Jugcraft's own; the work sound reuses the note block's "bit" sound by name. MIT.
- The shop template is written in 26.3's format with 26.3's DataVersion (5023), so the data fixer leaves it alone. A DataVersion above the game's made the whole template load as air; the game test `retroGameShopTemplateLoads` compares the two numbers, so after a platform bump `DATA_VERSION` in `tools/retro_game_shop.py` must follow.

## Verification
Build workflow run 36815372169 on commit 568409c (1 October 2026, merged with `main` after chemistry batch 5): Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub-hosted Ubuntu; game tests on the headless test server, client tests with Mesa software rendering.

**Passed:**
- `python3 tools/check_mod_data.py`: the Java trade table against `tools/pixel_hollows.py`, the search bounds, and the buyback against the cheapest sale.
- `./gradlew build`: all 103 game tests, including:
  - `retroTraderTrades`: novice, apprentice and journeyman traders get exactly their trades; the map costs 12 emeralds and a compass, once per restock.
  - `retroTraderHasNoProfitLoop`: the buyback's multiplier is 0; logged "cheapest shard 0.5 emeralds, best buyback 0.333 emeralds per shard".
  - `pixelHollowsMapNeedsACaveInReach`: in the superflat test world, using the map leaves it unmarked.
  - `pixelHollowsMapIsMarked`: a marked map carries the marker and the depth in its name.
  - `arcadeCabinetIsAJobSite`: the lower half is an acquirable job site for the profession; the upper half is not.
  - `villagerClaimsTheArcadeCabinet`: an unemployed villager walks to a cabinet and becomes a Retro Trader.
  - `retroGameShopTemplateLoads`: the template's DataVersion is 5023, the same as the game's, and its palette reads as its own blocks.
  - `retroGameShopTemplate`: placed as the test structure, the shop has the cabinet, door, sign, street jigsaw and a villager, and it is in the houses pool of all five village types.
  - `everyVillageHasOneShop`, now (Build run [37083349557](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37083349557) on 584c1bd6, 507 game tests passing): four rounds of the five village types, 20 real villages generated as `/place structure` does. Every one had exactly one shop and villagers (5 to 16). In the last round the shop was withheld from each village's first layout (`RetroShopPlacement.withholdShopForTests`), and the log shows exactly those 5 villages laid out again ("20 villages, 5 new layouts (5 in the last round …)"), each then with one shop.
  - `everyVillageHasOneShop` (first version: Build run 36902595139 on 7238b9e, 125 game tests passing): one real village of each type generated as `/place structure` does; logged "plains: 1 shop(s), 11 villagers; desert: 1 shop(s), 5 villagers; savanna: 1 shop(s), 10 villagers; snowy: 1 shop(s), 12 villagers; taiga: 1 shop(s), 16 villagers" (no zombie villages that run).
- Dedicated server (client job, `JugcraftServerClientGameTests`, one client): the game's own dedicated server started in-process on port 25565 and the client joined. The Retro Trader's novice offers reached the client after a real right-click (`[1 jugcraft:pixel_hollows_map, 8 jugcraft:circuitstone]`, screenshot `jugcraft_dedicated_server_trader`), a machine's screen showed the server's items, and after leaving and joining again the player still had the 7 pixel shards given before leaving and the world was unchanged. A singleplayer world holding a trader, a machine and an arcade cabinet was saved, closed and reopened with identical contents (the trader kept his offers).
- In a generated plains village (client screenshot `jugcraft_retro_game_shop_village`, Build run 36903438804 on aae25ba): the shop stands at ground level like the village's own houses, with one grass block at its door and then the village's dirt path (logged: doorstep `grass_block`, beyond it `dirt_path`).
- Client screenshots, looked at: `jugcraft_retro_trader` (the trader beside his cabinet, matching the reference picture), `jugcraft_retro_game_shop` (the storefront with its door, windows, shelves, lamps and sign) and the cabinet in `jugcraft_pixel_hollows_blocks`.

**Failed, then fixed on this branch:** the shop first placed as air: 26.3 templates name palette entries `id`/`properties`, and the template had used the older `Name`/`Properties` with a DataVersion the data fixer would not upgrade. It now matches vanilla 26.3's own village templates.

**Shop frequency:**
- **The gap.** Village houses are built inside their street's plot. From vanilla 26.3's templates, the shop fits an empty plot at 16 of plains' 39 house slots, 12 of the desert's 38, 20 of savanna's 26, 19 of snowy's 45 and 23 of taiga's 49.
- **How often it bit.** A village with no room happened in CI: "the desert village has 0 shops" in run 37076304887, one failure in about seven runs of five villages.
- **A model, not the game.** A scratch Python model of the placer on flat ground put it at about 2–3.5% of villages per type. With new layouts, every one of 5,000 modelled villages got a shop within 3 layouts. Its random numbers are not the game's.
- **In the game.** Since the fix, every one of the 20 villages in each test run has had exactly one shop, including the 5 that were made to need a new layout.

**Not run:** shop frequency in real, hilly worlds and many seeds (the tests use 20 villages on flat ground); buying a map in a client and following it to a cave; the map search's time in a real world; two clients trading with one trader on a dedicated server (the checklist is in docs/TESTING.md); restock; a marked map across a restart; old-world upgrade; disable-switch behaviour in a running world.

## World and event applicability
- Village fit: every village type except zombie villages. The shop is built from oak and cobblestone (plains style) with a few Jugcraft blocks inside, in every village type; styled variants for desert, savanna, snowy and taiga villages are a later change.
- Not seasonal; no pets, bosses, dimensions or rare loot.

## Rollout and open questions
- The villager in the shop usually takes its cabinet, but any unemployed villager may claim it first.
- Village-styled shops, wandering-trader stock and cosmetic outfits are later proposals.


### October 7 integration: rare exhausted layouts

Full CI found normal desert and snowy villages that still had no shop after the old eight-layout limit. The bounded retry budget is now 32 layouts; generation still stops on success, excludes zombie villages and places at most one shop. The village regression now deliberately withholds the shop for the first eight layouts in its final round, requiring recovery beyond the old limit. This improves the rare-case fallback; a finite budget does not promise a suitable site for every possible world seed. Results of the combined validation are recorded in the integration PR.
