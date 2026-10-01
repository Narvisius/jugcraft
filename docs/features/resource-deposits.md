# Surface resource deposits and the deposit drill

Status: implemented on `feature/deposits-11` (batch 11), awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: owner request, 1 October 2026: "add some surface deposits of resources like coal and iron which have different blocks that aren't mineable directly but that the players must put big drills on and they keep putting out resources until like 1000 are depleted per block the mining drills would mine like 4 at a time and output them into a chest which the player could then send on a conveyer belt or item pipes to furnaces and industrial machines to process, lets start with these showing up on the surface in like stoney hill biomes and maybe make new resource rich biome? We can eventually put them underground." Rate changed the same day at the owner's request: "Should produce like 1 of each resource every 15 seconds".
Owner: jimbozoomer-byte
Target milestone and tier: workshop tier (right after the first coal generator and a basic circuit)
Primary specialty and supported player role: mining and logistics

## Player experience
- **Deposits.** On stony hills you find flat patches, 2–4 blocks in radius, of **Coal, Iron, Copper or Tin Deposit** blocks in the top layer of the ground.
  - They look like rubble packed with big lumps of ore, unlike any ore block.
  - A pickaxe only breaks one, slowly (as slow as obsidian), and gets nothing for it.
  - Right-click one to read how much is left, e.g. "Iron Deposit: 734 of 1,000 left".
- **Deposit drill.** A 3×3 dieselpunk rig, two blocks tall, built on top of a patch.
  - Every 15 seconds it takes **one coal or raw ore from each kind of deposit** under it, or one block round it, down to 3 blocks deep. A drill over coal and iron gives one coal and one raw iron every 15 seconds.
  - A drill covers up to 25 deposit blocks of a patch, 25,000 items; at one item of a kind every 15 seconds, a single-kind patch keeps one drill busy for a long time (about 100 hours of play), so it is a steady trickle rather than a flood.
  - It pushes what it mines out of every side into whatever is beside it: a chest, item pipe, conveyor, or a furnace or machine directly. Item pipes and conveyors then carry it to furnaces and the ore-processing machines.
  - Every face and the eject toggle can be changed on its screen, like any machine.
- **Depletion.** Each deposit block holds 1,000 units. When one is empty it turns to stone and the drill moves on to the next block. When none is left in reach the drill stops, and you move it to the next patch.

## Connections
- Input producer: power from any generator (16 JE/t); a coal generator runs two.
- Output consumer:
  - Coal goes to generators, the coke oven and furnaces.
  - Raw iron, copper and tin go to furnaces, the electric furnace, crusher, pulverizer and ore washer.
  - The drill fills chests directly; item pipes and conveyors take it from there.
- Technology connection: machine framework (upgrades, side configuration, eject, redstone modes); item logistics.
- Magic connection: none.
- Reachable entry path:
  - The drill needs iron plates, bronze gears, two iron pickaxes, a machine casing and a basic circuit, all workshop-tier.
  - Deposits generate in vanilla biomes, so no other Jugcraft content is needed.
- Required vs optional: optional. Ores still generate and can be mined by hand or with the ore drill. Deposits are a bulk, automated source.
- Trade and solo: solo-reachable. A patch can also be a shared resource on a server.

## Balance and automation
- **Rate.** One item of each kind of deposit in reach per 300 ticks (15 s) at 16 JE/t: 4,800 JE a cycle. Speed and efficiency upgrades apply as on other machines.
- **Amount.** 1,000 units per block, patches of about 13–50 blocks: roughly 13,000–50,000 items per patch, all finite.
- **No loop.** Deposits are finite and never regrow. Turning coal from a coal deposit into power (51,200 JE a coal in the coal generator) gains energy the way any mined fuel does, but each block ends.
- **Yield parity.** A deposit gives raw ore (or coal), the same as pick-mining an ore block without silk touch, so ore processing (crusher ×2, washing ×3) keeps its value. There is no doubling at the drill.
- **Rarity.** Each deposit type appears in about one chunk in 8 (coal), 10 (iron) and 12 (copper, tin) of a matching biome.

## Multiplayer and persistence
- Server-authoritative. The drill and deposits are server-side; clients only see blocks and the menu.
- **What is saved.** Only touched deposits are recorded: how much each deposit block has given is saved per dimension in `data/jugcraft_deposits.dat`, keyed by block position.
- **Removal.** Removing a deposit block (by hand or when it runs out) deletes its record.
- **Restart.** The drill finds its target block again after a restart (not saved); its items, energy, sides and upgrades are saved as for any machine.
- **Disable.** `deposits.enabled=false` stops new patches generating. Existing deposit blocks and drills stay and keep working. `tin.enabled=false` also stops tin patches.
- **Protection.** The drill does not check claims or spawn protection, like the ore drill.

## Dependencies and assets
No new dependencies. Uses Fabric API biome modifications and the `c:is_windswept` / `c:is_stony_shores` convention tags. All textures and models are original:
- `tools/deposits.py` for the deposit textures and data;
- `tools/dieselpunk_models.py` (`deposit_drill`) for the drill model.

## Verification
- `tools/check_mod_data.py` checks:
  - Java vs `tools/deposits.py`: deposits, yields, capacity and the worldgen calls with their feature switches;
  - the drill's constants vs `tools/machines.py`;
  - that every deposit has its assets, loot table and placed feature.
- The worldgen JSON follows vanilla 26.3's own `disk_gravel` and placed-feature files, read from the game jar with an API probe.
- Game tests (CI):
  - `depositDrillEmptiesDepositsIntoAChest`: an iron and a coal deposit in reach, each with 2 left, are worked together (one of each per cycle) into a chest beside the drill and turn to stone; a deposit out of reach stays full; a deposit block drops nothing.
  - `brokenDepositIsForgotten`: a deposit placed where one was broken starts full.
- Not run: client play, worldgen in a real world (finding patches on actual hills), two players, performance with many drills.

## World and event applicability
- **Biomes:** windswept hills, windswept gravelly hills, windswept forest, windswept savanna, stony peaks and stony shores, where bare rock shows.
- **Placement:** patches replace stone, andesite, diorite, granite, tuff, calcite, gravel, grass, dirt and coarse dirt in the top layer only, never under water.
- **New chunks only:** there is no retrogeneration.
- **Not done yet:** a new resource-rich biome. Fabric API can add biomes to the Nether and End but not to the Overworld; doing that needs either a biome-placement library (TerraBlender or similar, a new dependency) or replacing the Overworld's biome layout in a datapack, which other world-generation mods conflict with. This needs the owner's choice.
- **Later:** underground deposits (the drill already reaches 3 deep).

## Rollout and open questions
- Owner choice: how to add a resource-rich biome (see above).
- Underground deposits: thicker veins in caves, with a deeper drill tier.
- More deposit types (gold, zinc, lead, nickel, salt) once the first four are played.
- A visible drilling animation (spinning augers) is later polish.
