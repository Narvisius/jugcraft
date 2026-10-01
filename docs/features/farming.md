# Farming: harvesters, sprinklers and cotton

Status: implemented (batch 9, #58)
Proposal issue: owner request, 1 October 2026 ("merge it and start the next batch"), following suggestion 6 from batch 5 ("Farming: greenhouses, a powered harvester and sprinklers that use fertilizer, plus new crops")
Owner: jimbozoomer-byte
Target milestone and tier: steel tier, beside the chemistry line's fertilizer
Primary specialty and supported player role: farming and automation

## Plan

| # | Commit | What it adds |
| --- | --- | --- |
| 46 | Crop harvester | A two-block gantry that harvests and replants ripe crops in the 9×9 field in front of it. |
| 47 | Sprinkler | Pipe-fed water that speeds the crops around it, and spreads fertilizer from its slot. |
| 48 | Cotton | A new crop: seeds from sieving coarse dirt, cotton into string. |
| 49 | Docs and PR | Advancements, handbook, docs. |

Greenhouses and rubber trees are left for later: rubber trees need worldgen, and a greenhouse needs its own climate rules.

## Player experience

### Crop harvester (commit 46)
- **Two blocks tall** (dieselpunk look): an olive engine cabinet with a grain hopper, a caged lamp and gauge, and a mast carrying a red reel of bats over a cutter bar, with a beacon on top.
- **Works through the 9×9 field in front of it**, starting the block in front, at its own height (where crops on farmland sit). Each ripe crop takes 20 powered ticks at 24 JE/t (480 JE).
- It keeps the drops in three result slots, **less one seed, which it plants again**. When the drops would not all fit, it waits. Unripe crops are left alone.
- Any `CropBlock` counts: wheat, carrots, potatoes, beetroot, and cotton (commit 48).
- Side configuration, eject, redstone control and upgrades work as on other machines.
- Recipe: steel gears, shears, two hoppers, a machine casing, steel plates and a basic circuit.

### Sprinkler (commit 47)
- **A sprinkler on a post** (dieselpunk look): flanged inlets on every side for pipes, an olive water tank, a fertilizer hopper and a rotor head with two spray arms. It shows spray while it holds water.
- **Water:** pipes (or a water bucket) fill its 4-bucket tank. Every 5 seconds it uses 50 mB and gives each growing crop within 4 blocks (at its height and one below) one extra growth tick, as if the game had picked it. Crops still need light to grow, as in vanilla.
- **Fertilizer:** load up to 16 by hand or by hopper; every 30 seconds it spreads one over the 5×5 crops around it (`FertilizerItem.fertilize`), and only uses it if something grows.
- Right-click with an empty hand to read its water and fertilizer.
- Code: `farming/SprinklerBlock`, `SprinklerBlockEntity`, `JugcraftFarming`.
- Recipe: a bronze fluid pipe, steel plates, a tinplate tank and a hopper.

### Cotton (commit 48)
- **A new crop:** plant cotton seeds on farmland; it grows through eight ages, shown in four original stages (sprouts, leafy plants, green bolls, open white bolls).
- **Seeds:** the sieve sometimes finds them in coarse dirt (15%, with wheat seeds 10%); the coarse dirt becomes dirt.
- **A ripe plant** drops one to three cotton and more seeds (Fortune adds seeds), like wheat. The harvester, sprinkler and fertilizer all work on it (`minecraft:crops`, `minecraft:maintains_farmland`).
- **One cotton spins into one string** (crafting).
- Code: `farming/CottonCropBlock`; `JugcraftFarming` registers the crop, its seeds (which place it) and cotton. Textures: `tools/crop_textures.py`. `check_mod_data` now knows crops have no item of their own and that item tags may hold machine and chemistry items.

### Advancements
King Cotton (cotton), Make It Rain (sprinkler) and Reaping What You Sow (crop harvester).

## Connections
- Existing input producer: vanilla crops on farmland; fertilizer (batch 5) ripens them faster.
- Existing output consumer: food and seeds for players, the auto-crafter, and anything that takes wheat; cotton makes string (and so wool, bows, leads and the leather belt's string).
- Technology connection: power, item logistics.
- Magic connection: none.
- Reachable entry path: steel tier; no circular unlock.
- Required vs optional: optional automation.

## Balance and automation
- The harvester only gathers what vanilla crops grow on their own; it makes nothing from power. 480 JE a crop.
- Cotton is like wheat: it grows only as vanilla crops do, and one cotton makes one string. Coarse dirt is renewable (gravel and dirt), so the seed source never runs out.
- A sprinkler's extra growth tick every 5 seconds is about thirteen times the vanilla average for a crop (a random tick roughly every 68 seconds), for 600 mB of water a minute. Water is renewable but has to be pumped and piped there.

## Multiplayer and persistence
Server-side. The harvester's scan position saves with its block entity like the ore drill's, and it only touches crop blocks in its loaded field. The sprinkler's water, fertilizer and pulse count save with its block entity; its spray is a client-side effect of its `wet` block state.

## Dependencies and assets
No new dependencies. Textures and models are original (`tools/dieselpunk_models.py`, `tools/farming_models.py`, `tools/crop_textures.py`).

## Verification
- Game test `cropHarvesterHarvestsAndReplants` (JugcraftGameTests): a ripe wheat crop in its field is harvested, replanted at age 0, and the wheat kept.
- Game test `cottonGrowsFromSeedsAndDropsCotton` (JugcraftGameTests): the seeds plant the crop, it is in `minecraft:crops`, a ripe one drops cotton and seeds, and the sieve's coarse dirt recipe can give seeds.
- Game test `sprinklerWatersAndFertilizes` (PetroGameTests): with water and three fertilizer, it uses at least six pulses of water and one fertilizer, and the wheat beside it grows.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.
