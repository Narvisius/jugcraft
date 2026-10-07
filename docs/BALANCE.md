# Balance

Energy is in **JE** (Jugcraft Energy) and rotation in **KE**, both per tick (20 ticks a second). The source of truth for these numbers is `tools/machines.py` and the Java constants it mirrors. This page was last reviewed on 1 October 2026 (PR #46; the oil line added in #47–#51).

## Generators

| Generator | Output | Per fuel | Notes |
| --- | --- | --- | --- |
| Coal Generator | 32 JE/t | coal 51,200 · charcoal 38,400 · coke 102,400 · coal block 512,000 | stops burning when full |
| Steam Generator | 64 JE/t | coal 102,400 · charcoal 76,800 · coke 204,800 | 10 mB water/t |
| Geothermal Generator | 64 JE/t | 64,000 per lava bucket | 1 mB lava/t |
| Solar Panel | 8 JE/t | free | full sun only; half in rain |
| Advanced Solar Panel | 64 JE/t | free | full sun only; half in rain; costs three solar panels and a processor |
| Water Wheel | up to 24 JE/t | free | flowing water at the wheel |
| Wind Turbine | 12–72 JE/t | free | height and weather |
| Steam Engine | 64 KE/t | coal 102,400 KE | burns only while something takes the power |
| Large Steam Engine | 256 KE/t | coal 102,400 KE | four times the small engine, at the same fuel efficiency |
| Hand Crank | 16 KE/t | a little hunger | 5 s per click |
| Diesel Generator | 256 JE/t | diesel 256,000 · heavy fuel oil 128,000 · premium diesel 320,000 per bucket | stops burning when full |
| Gas Turbine | 512 JE/t | gasoline 384,000 · premium gasoline 448,000 · refinery gas 192,000 per bucket | plus 1 mB lubricant per 20 ticks running (a bucket per 10,240,000 JE) |
| Diesel Engine | up to 512 KE/t | diesel 256,000 KE · premium diesel 320,000 KE · heavy fuel oil 128,000 KE per bucket | burns only for what the line takes |
| Advanced Combustion Engine | up to 1,024 KE/t | premium gasoline 512,000 KE · gasoline 448,000 KE · premium diesel 400,000 KE · diesel 320,000 KE per bucket | burns only for what the line takes |
| Fuel Cell | 128 JE/t | hydrogen 128,000 per bucket | a bucket of hydrogen costs 204,800 JE of electrolysis |
| Heat Recovery Unit (batch 29) | 30% of a touching diesel generator (77 JE/t) or gas turbine (154 JE/t) | the generator's own fuel | 1 mB water per 64 JE; 1 mB lubricant per 40 ticks; two units share one generator's heat |
| Steam Generator, bitumen | 64 JE/t | 51,200 per bitumen | unchanged by the oil line |

Charcoal burns three quarters as long as coal in Jugcraft's generators and engines. Vanilla furnaces are unchanged.

## Conversions

| Conversion | Rate | Loss |
| --- | --- | --- |
| KE → JE (Dynamo) | 128 JE/t max | 25% |
| JE → KE (Electric Motor) | 96 KE/t max (needs 128 JE/t; takes up to 256) | 25% |
| KE → JE (Magnet Dynamo) | 512 JE/t max | 5% |
| JE → KE (Magnet Motor) | 384 KE/t max (needs 405 JE/t; takes up to 1,024) | 5% |
| Machine on a shaft | 1 KE = 1 JE | none |

A motor driving a dynamo returns 56% of the JE: no loop. The magnet pair returns 90.25%: still no loop.

## Storage and transfer

| Block | Holds | In/out per tick |
| --- | --- | --- |
| Battery Box | 400,000 JE | 256 |
| Capacitor Bank | 4,000,000 JE | 4,096 |
| Lithium Battery Bank | 32,000,000 JE | 16,384 |
| Steel Tank | 128 buckets of one fluid | by pump |
| Gas Holder | 1,024 buckets of one gas | by pump |
| Copper / Silver / Aluminum Cable | — | 256 / 1,024 / 4,096 |
| Charging Station | 50,000 JE | 1,024 in, 512 into the tool |

A charging station on copper cable fills at 256 JE/t; silver cable or better lets it reach its full 512 JE/t into the tool.

## Users

| Thing | Cost |
| --- | --- |
| Most processing machines | 8–32 JE/t while working |
| Arc Furnace | 64 JE/t |
| Ore Drill (machine) | 32 JE/t, 40 ticks per ore: 1,280 JE an ore |
| Deposit Drill | 16 JE/t, one item of each deposit kind in reach per 300 ticks (15 s): 4,800 JE a cycle; each deposit block gives 1,000 then is stone |
| Conveyor | 1 KE per conveyor per tick, for a whole run of up to 64 |
| Mining Drill | 60 JE a block (×2 / ×3 with overclock modules); 100,000 JE full |
| Chainsaw | 40 JE a block; 100,000 JE full |
| Rocket Pack | 50 JE a tick of thrust; 200,000 JE full (about 200 s) |
| Power Katana | 1,000 JE a hit; 200,000 JE full |
| Power Bow | 500 JE a shot; 100,000 JE full; arrows cannot be picked up |
| Exosuit (each piece) | 400,000 JE full. Helmet 2 JE/t while dark; chestplate 4,000 JE per absorption point regrown and 50 JE/t of jetpack thrust; leggings and boots 1 JE/t |
| Scuba Tank | 1 mB oxygen a tick under water while air is short; 8,000 mB full (400 s; 256,000 JE of air separation) |

## Oil

Crude oil is finite: reservoirs run dry, oil sand is a worldgen block, and nothing turns power, water or items back into crude. Values per bucket (1,000 mB) of crude oil:

| Step | Costs | Gives |
| --- | --- | --- |
| Pumpjack | 16,000 JE | 1,000 mB crude |
| Oil sand extractor (instead) | 10,240 JE + 500 mB water | 1,000 mB crude, 2 sand (from 2 oil sand) |
| Fracking rig (instead) | ~42,700 JE, ~3,400 JE of mixing, ~1,900 JE of flowback treatment; 667 mB fracking fluid (sand, kelp, water, a quarter of the water lost) | 1,000 mB crude **and** 333 mB refinery gas |
| Distillation | 12,800 JE | 100 gas, 250 naphtha, 400 diesel, 250 heavy fuel oil |
| Cracking the heavy fuel oil | 6,400 JE + ¼ catalyst + 62.5 mB water | 125 diesel, 75 naphtha, 50 gas |
| Reforming all the naphtha | 4,680 JE | 292.5 gasoline, 32.5 gas |
| **Total (pumpjack route)** | **~39,900 JE** | **525 diesel, 292.5 gasoline, 182.5 gas** |

Burnt in the best generator for each (diesel generator, gas turbine), that is 134,400 + 112,320 + 35,040 = **about 281,800 JE**, a net of about **+242,000 JE per bucket of crude**: roughly what 2.4 coal give in a steam generator after costs. Oil is meant to be the strongest fuel, but it is limited by how much oil a world has (a conventional reservoir holds 50–250 buckets, shale 200–800).

- Instead of cracking, the vacuum unit turns 250 mB of heavy fuel oil into 100 mB lubricant and ½ asphalt binder (the turbine's upkeep and 4 asphalt).
- Refinery gas has two uses: 192,000 JE a bucket in the turbine, or 4 plastic pellets (4 sheets).
- KE: the diesel engine gives the same per mB as the generator; through a dynamo that is 192 JE/mB, so the generator stays the better JE source. Even through a magnet dynamo it is 243 JE/mB, under the generator's 256. The advanced combustion engine is the exception: through a magnet dynamo it gives 426 JE per mB of gasoline and 304 per mB of diesel, the best use of either fuel, paid for in titanium, magnets and a processor.

## Electronics

| Step | Cost |
| --- | --- |
| Silicon boule (arc furnace, batch 24) | 4 silicon, 1 phosphate, 25,600 JE |
| 8 wafers (sawmill) | 1 boule, 200 ticks |
| 4 microchips (lithography station) | 1 wafer, 2 copper wire, 100 mB sulfuric acid, 38,400 JE |
| Processor (circuit assembler) | 4 microchips, 1 advanced circuit, 1 gold ingot, 12,800 JE |

A processor costs about 64,000 JE of crystal growing and etching (two wafers' worth of chips), plus 2 silicon, 4 copper wire and 100 mB of acid. Nothing in this tier makes power or turns back into its inputs.

## Loops and renewables checked

- **Motor ↔ dynamo:** loses 44% per round trip; magnet motor ↔ magnet dynamo loses 9.75% (game test `magnetMotorAndDynamoLoopLosesPower`).
- **Electrolysis and the fuel cell:** a bucket of brine costs 51,200 JE and gives 250 mB of hydrogen, worth 32,000 JE in the fuel cell: a 37.5% return, never a loop.
- **Bioethanol (batch 26):** 8 crops and 9,600 JE of fermenting (100 ticks at 96 JE/t) give 250 mB, worth 48,000 JE in the gas turbine: 4,800 JE a crop after fermenting. One harvester field (81 crops, each regrowing in roughly 20–40 minutes) feeds about 10–20 JE/t on average: a renewable trickle, a few solar panels' worth, never a rival to oil. Crops are renewable, so this is a renewable power source like the sun, not a loop.
- **Refinery upgrades (batch 29):** hydrotreating a bucket of diesel costs 15,360 JE and 100 mB of hydrogen for +64,000 JE in the diesel generator; with water-electrolysis hydrogen (about 41,000 JE per 100 mB) that is only about +7,600, with the brine cell's by-product hydrogen about +48,600. Blending 900 mB gasoline + 100 mB bioethanol costs 5,120 JE for +83,200 JE in the gas turbine. Both use up oil (and crops), so they are upgrades, not loops. The heat recovery unit only turns a generator's fuel heat into 30% more power; it takes that heat from the generator, so it cannot recover from itself and two units cannot share the same heat twice (game test `heatRecoveryUnitsShareOneTurbine`). Sulfur recovery: 200 mB of hydrogen sulfide (from 2 buckets of diesel) gives a sulfur dust.
- **Acid leaching (batch 26):** an ore + 250 mB of sulfuric acid gives 4 washed ores, the 4× route (washer 3×, crusher 2×). The acid costs half a sulfur dust per ore, so the extra ingot is paid for in sulfur.
- **Water electrolysis (batch 24):** a bucket of water costs 204,800 JE (800 ticks at 256 JE/t) and gives 500 mB of hydrogen, worth 64,000 JE in the fuel cell: a 31% return. Fluid processors take no upgrade cards, so nothing makes it cheaper. Its 250 mB of oxygen is a by-product for the steel foundry or nitric acid.
- **Aluminum:** the Bayer route gets two ingots per bauxite for about 35,000 JE an ingot; the arc furnace gets one for 12,800 JE. More metal for more power, not more of both.
- **Oil:** no loop. Fracking water returns at 75% (flowback treatment), so a fracking rig needs a water supply; the water is not counted as gain. Diesel engine → dynamo → electric motor loses at every step.
- **Coke:** a coke oven turns 1 coal into 1 coke, which burns twice as long. That doubles the power from coal, but uses the coal up: an upgrade path, not a loop.
- **Thallite (slice 1):** no loop. Its ore takes the shared routes (smelt 1, crush or pulverize 2, wash 3, leach 4 ingots an ore), and ingot → dust → ingot is 1:1. The Pulverizer's iron dust byproduct averages 0.1 dust a roll: 5% of an ore's metal and 10% of a washed ore's, under the 25% limit. The hand-made plate takes 2 ingots for 1, losing half; the Metal Press is 1:1 ([thallite.md](features/thallite.md)).
- **Sieve:** cobblestone → gravel → sieve gives a small trickle of iron and tin nuggets (12% and 8%). This is a deliberate renewable, marked `renewable` in the recipe data and exempt from the metal-conservation audit.
- **Tree farm charcoal (owner-approved exception):** a tree farm uses 6,400 JE to grow 6 logs. The logs become 6 charcoal for about 6,000 JE in the electric furnace, and burn for 230,400 JE in a coal generator (460,800 in a steam generator). That is a net gain of about +545 JE/t per tree farm (it was +737 before charcoal was cut to 1,200 ticks). The owner chose to keep wood power, made slightly weaker, rather than remove it. It is the one positive loop allowed, and any change to the tree farm, charcoal or generators should keep it in mind.

## Rules

- No new positive-gain loops (CLAUDE.md). An exception needs the owner's decision and a line in the list above.
- Conversion losses are documented here and in each feature record.

## Nitrogen chemistry (batch 12)

| Step | Cost | Notes |
| --- | --- | --- |
| Air separation | 64 JE/t for 8 mB nitrogen + 2 mB oxygen + 0.5 mB argon | from the air; no input, like a pumpjack's reservoir |
| Oxygen-blown steel (batch 13) | 2 mB oxygen a tick | the steel foundry runs twice as fast; one air separation unit keeps one foundry blown |
| Argon-shielded crystals (batch 13) | 1 mB argon a tick | the arc furnace runs twice as fast on any recipe (batch 24; the crystal grower before it); one unit's argon keeps half a furnace shielded |
| Ammonia (Haber–Bosch) | 300 mB hydrogen + 100 mB nitrogen → 200 mB, 5,120 JE | the hydrogen is 1.2 buckets of brine of electrolysis |
| Nitric acid (Ostwald) | 100 mB ammonia + 200 mB oxygen + 100 mB water → 200 mB, 5,120 JE | |
| Ammonium phosphate | 2 phosphate + 250 mB ammonia → 6 fertilizer | against 4 with sulfuric acid; phosphate stays the limit |
| Microchips with nitric acid | 50 mB per 4 chips | against 100 mB of sulfuric acid |

No converter recipe gives out more fluid than it takes in (400 mB → 200 mB each), so there is no fluid loop.
