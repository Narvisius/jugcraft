# Balance

Energy is in **JE** (Jugcraft Energy) and rotation in **KE**, both per tick (20 ticks a second). The source of truth for these numbers is `tools/machines.py` and the Java constants it mirrors. This page was last reviewed on 1 October 2026 (PR #46).

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

Charcoal burns three quarters as long as coal in Jugcraft's generators and engines. Vanilla furnaces are unchanged.

## Conversions

| Conversion | Rate | Loss |
| --- | --- | --- |
| KE → JE (Dynamo) | 128 JE/t max | 25% |
| JE → KE (Electric Motor) | 96 KE/t max (needs 128 JE/t; takes up to 256) | 25% |
| Machine on a shaft | 1 KE = 1 JE | none |

A motor driving a dynamo returns 56% of the JE: no loop.

## Storage and transfer

| Block | Holds | In/out per tick |
| --- | --- | --- |
| Battery Box | 400,000 JE | 256 |
| Capacitor Bank | 4,000,000 JE | 4,096 |
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

## Loops and renewables checked

- **Motor ↔ dynamo:** loses 44% per round trip.
- **Coke:** a coke oven turns 1 coal into 1 coke, which burns twice as long. That doubles the power from coal, but uses the coal up: an upgrade path, not a loop.
- **Sieve:** cobblestone → gravel → sieve gives a small trickle of iron and tin nuggets (12% and 8%). This is a deliberate renewable, marked `renewable` in the recipe data and exempt from the metal-conservation audit.
- **Tree farm charcoal (owner-approved exception):** a tree farm uses 6,400 JE to grow 6 logs. The logs become 6 charcoal for about 6,000 JE in the electric furnace, and burn for 230,400 JE in a coal generator (460,800 in a steam generator). That is a net gain of about +545 JE/t per tree farm (it was +737 before charcoal was cut to 1,200 ticks). The owner chose to keep wood power, made slightly weaker, rather than remove it. It is the one positive loop allowed, and any change to the tree farm, charcoal or generators should keep it in mind.

## Rules

- No new positive-gain loops (CLAUDE.md). An exception needs the owner's decision and a line in the list above.
- Conversion losses are documented here and in each feature record.
