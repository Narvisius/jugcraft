# Balance

Energy is in **JE** (Jugcraft Energy) and rotation in **KE**, both per tick (20 ticks a second). The source of truth for these numbers is `tools/machines.py` and the Java constants it mirrors. This page was last reviewed on 1 October 2026 (PR #46; the oil line added in #47–#51).

## Generators

| Generator | Output | Per fuel | Notes |
| --- | --- | --- | --- |
| Coal Generator | 32 JE/t | coal 51,200 · charcoal 38,400 · coke 102,400 · coal block 512,000 | stops burning when full |
| Steam Generator | 64 JE/t | coal 102,400 · charcoal 76,800 · coke 204,800 | 10 mB water/t |
| Geothermal Generator | 64 JE/t | 64,000 per lava bucket | 1 mB lava/t |
| Solar Panel | 8 JE/t | free | full sun only; half in rain |
| Water Wheel | up to 24 JE/t | free | flowing water at the wheel |
| Wind Turbine | 12–72 JE/t | free | height and weather |
| Steam Engine | 64 KE/t | coal 102,400 KE | burns only while something takes the power |
| Large Steam Engine | 256 KE/t | coal 102,400 KE | four times the small engine, at the same fuel efficiency |
| Hand Crank | 16 KE/t | a little hunger | 5 s per click |
| Diesel Generator | 256 JE/t | diesel 256,000 · heavy fuel oil 128,000 per bucket | stops burning when full |
| Gas Turbine | 512 JE/t | gasoline 384,000 · refinery gas 192,000 per bucket | plus 1 mB lubricant per 20 ticks running (a bucket per 10,240,000 JE) |
| Diesel Engine | up to 512 KE/t | diesel 256,000 KE · heavy fuel oil 128,000 KE per bucket | burns only for what the line takes |
| Fuel Cell | 128 JE/t | hydrogen 128,000 per bucket | a bucket of hydrogen costs 204,800 JE of electrolysis |
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
| Copper / Silver / Aluminum Cable | — | 256 / 1,024 / 4,096 |
| Charging Station | 50,000 JE | 1,024 in, 512 into the tool |

A charging station on copper cable fills at 256 JE/t; silver cable or better lets it reach its full 512 JE/t into the tool.

## Users

| Thing | Cost |
| --- | --- |
| Most processing machines | 8–32 JE/t while working |
| Arc Furnace | 64 JE/t |
| Ore Drill (machine) | 32 JE/t, 40 ticks per ore: 1,280 JE an ore |
| Conveyor | 1 KE per conveyor per tick, for a whole run of up to 64 |
| Mining Drill | 60 JE a block (×2 / ×3 with overclock modules); 100,000 JE full |
| Chainsaw | 40 JE a block; 100,000 JE full |
| Rocket Pack | 50 JE a tick of thrust; 200,000 JE full (about 200 s) |

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
- KE: the diesel engine gives the same per mB as the generator; through a dynamo that is 192 JE/mB, so the generator stays the better JE source. Even through a magnet dynamo it is 243 JE/mB, under the generator's 256.

## Loops and renewables checked

- **Motor ↔ dynamo:** loses 44% per round trip; magnet motor ↔ magnet dynamo loses 9.75% (game test `magnetMotorAndDynamoLoopLosesPower`).
- **Electrolysis and the fuel cell:** a bucket of brine costs 51,200 JE and gives 250 mB of hydrogen, worth 32,000 JE in the fuel cell: a 37.5% return, never a loop.
- **Aluminum:** the Bayer route gets two ingots per bauxite for about 35,000 JE an ingot; the arc furnace gets one for 12,800 JE. More metal for more power, not more of both.
- **Oil:** no loop. Fracking water returns at 75% (flowback treatment), so a fracking rig needs a water supply; the water is not counted as gain. Diesel engine → dynamo → electric motor loses at every step.
- **Coke:** a coke oven turns 1 coal into 1 coke, which burns twice as long. That doubles the power from coal, but uses the coal up: an upgrade path, not a loop.
- **Sieve:** cobblestone → gravel → sieve gives a small trickle of iron and tin nuggets (12% and 8%). This is a deliberate renewable, marked `renewable` in the recipe data and exempt from the metal-conservation audit.
- **Tree farm charcoal (owner-approved exception):** a tree farm uses 6,400 JE to grow 6 logs. The logs become 6 charcoal for about 6,000 JE in the electric furnace, and burn for 230,400 JE in a coal generator (460,800 in a steam generator). That is a net gain of about +545 JE/t per tree farm (it was +737 before charcoal was cut to 1,200 ticks). The owner chose to keep wood power, made slightly weaker, rather than remove it. It is the one positive loop allowed, and any change to the tree farm, charcoal or generators should keep it in mind.

## Rules

- No new positive-gain loops (CLAUDE.md). An exception needs the owner's decision and a line in the list above.
- Conversion losses are documented here and in each feature record.
