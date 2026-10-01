# Chemistry branch

Status: **in progress.** The petrochemistry (oil) line below is being built in batches; the rest of the branch is still planned. This document reserves the branch's scope so mechanical and fluid work stays out of it. See [../TECH_TREE.md](../TECH_TREE.md) for how it fits with the rest of Jugcraft.

## What belongs here

Anything that changes what a substance *is* through a reaction, as opposed to its shape or mix (mechanical) or where it is (fluids):
- electrolysis;
- acids and bases;
- fertilizers;
- oil refining;
- rare-earth separation;
- battery chemistry.

## Existing items waiting for chemistry

These materials already exist and are obtainable. Chemistry will give them their real uses and replace the temporary stand-ins without changing any item IDs.

| Item | Current source | Current stand-in use | Chemistry plan |
| --- | --- | --- | --- |
| Salt | Rock salt ore | None | **Done (batch 5):** brine electrolysis → lye, chlorine and hydrogen |
| Bauxite | Surface rock | Blast furnace → 1 aluminum nugget; arc furnace → 1 aluminum ingot | **Done (batch 5):** lye digestion → 2 alumina, electrolysis with a coke anode → 2 aluminum ingots |
| Sulfur dust | Crushed vanilla sulfur | None | **Done (batch 5):** sulfuric acid |
| Phosphate | Phosphorite ore | None | **Done (batch 5):** phosphate + sulfuric acid → fertilizer |
| Lepidolite / lithium carbonate | Lepidolite ore | Blast or arc furnace → lithium carbonate | Leaching and precipitation; battery compounds |
| Monazite / rare earth oxide | Monazite ore | Blast or arc furnace → rare earth oxide | Acid digestion and solvent extraction → separated rare earths (magnets) |
| Bitumen | Oil sand | Steam generator fuel | Upgrading and refining alongside liquid crude oil |

## Planned machines (proposals)

| Machine | Reaction | Needs first |
| --- | --- | --- |
| Electrolytic Cell | Brine → lye + chlorine; alumina → aluminum | Fluid system (water, brine) |
| Chemical Reactor | Sulfur + water → sulfuric acid; phosphate + acid → fertilizer | Fluid system |
| Leaching Vat | Ores and oxides + acid → dissolved salts → purified products | Chemical reactor, fluid system |
| Refinery (multiblock) | Crude oil → fuel, lubricant, plastic, asphalt | Fluid system, liquid crude oil. Now planned in detail below |

## Petrochemistry: the dieselpunk oil line

The owner asked on 1 October 2026 for "the Diesel Punk Chemistry branch of the science tree": crude oil processed into more advanced, usable fuels in big machines, with oil fracking, opening "a new path for more advance power generation and other techs that will be useful for industry later on". It is planned as 20 commits, built five at a time (one PR per batch). Each batch lists what it adds; the status column is updated as batches merge.

Every machine here is steel tier or later and follows [ART_DIRECTION.md](../ART_DIRECTION.md): dieselpunk, detailed, and as big as the real thing (pumpjacks, towers and rigs are multi-block). Fluids move in the existing pipes and tanks. Amounts are millibuckets (mB).

### Batch 1: oil in the world (done, #47)

| # | Commit | What it adds |
| --- | --- | --- |
| 1 | Crude oil fluid | Liquid crude oil: a real fluid with a bucket, placeable and flowing, that works in every Jugcraft and Fabric tank and pipe. |
| 2 | Fluid processing machines | Machines with several input and output tanks, data-driven fluid recipes (`jugcraft:<machine>` recipe types with fluids in and out), tank gauges on the machine screen, and output tanks that push into pipes. The base for every machine below. |
| 3 | Oil reservoirs | Hidden underground oil fields fixed by the world seed: some chunks hold a conventional reservoir (pumpable), more hold tight shale oil (needs fracking). Each is finite and its depletion is saved. The prospector reports oil in its survey. |
| 4 | Pumpjack | A 3-block-long, 3-tall dieselpunk pumpjack (nodding donkey) that pumps a chunk's conventional reservoir dry, slowly, on JE. |
| 5 | Oil sand extractor | Hot-water extraction: oil sand + water → crude oil + sand. A route to crude anywhere oil sand is found, for players with no reservoir nearby. |

### Batch 2: refining (done, #49)

| # | Commit | What it adds |
| --- | --- | --- |
| 6 | Steel pipes and the heavy pump | Higher-throughput steel fluid pipes and a steel pump, sized for refinery flows. |
| 7 | Distillation tower | A tall multi-block column: crude oil → refinery gas, naphtha, diesel and heavy fuel oil, in fixed fractions that conserve volume. |
| 8 | Catalytic cracker | Heavy fuel oil + steam → more diesel, naphtha and gas, with a slowly used catalyst. |
| 9 | Vacuum distillation | Heavy fuel oil → lubricant and asphalt binder. |
| 10 | Reformer | Naphtha → high-octane gasoline. |

### Batch 3: fracking and diesel power (done, #50)

| # | Commit | What it adds |
| --- | --- | --- |
| 11 | Chemical mixer and fracking fluid | Water + sand + a gelling agent → fracking fluid. |
| 12 | Fracking rig | A big derrick that pumps fracking fluid into a shale reservoir and brings up crude oil, refinery gas and flowback water. |
| 13 | Flowback treatment | Flowback water → clean water (with losses) and salt, so fracking water is not free. |
| 14 | Diesel generator | A big engine that burns diesel (or heavy fuel oil, less well) for high JE output. |
| 15 | Gas turbine | Burns refinery gas or gasoline for the highest output, with lubricant upkeep. |

### Batch 4: industry (done, #51)

| # | Commit | What it adds |
| --- | --- | --- |
| 16 | Plastics | Refinery gas → plastic pellets → plastic sheets, for later machines and parts. |
| 17 | Asphalt | Asphalt binder + gravel → asphalt road blocks that are quicker to walk on. |
| 18 | Diesel engine | A kinetic engine that turns shafts on diesel. |
| 19 | Handbook, advancements and JEI | The oil chapter, advancements, recipe viewer categories and the oil energy audit in [BALANCE.md](../BALANCE.md). |
| 20 | Visual polish | In-game screenshots of the oil machines and a power-gear scene. The owner's restyle of the cables and power gear (the electric look, [ART_DIRECTION.md](../ART_DIRECTION.md#electric-power-gear-and-the-high-tech-tiers)) rode along in this batch. The animated pumpjack and rig and flare stacks were not done: they need a block-entity renderer like the kinetic rotors, and are left for a later polish pass. |

## Industrial chemistry: electrochemistry and acids

After the oil line (owner request, 1 October 2026: "start the next batch immediately"), the branch turns to the items that have been waiting for chemistry since the start: salt, sulfur, phosphate and bauxite. These machines are steel tier and dieselpunk like the oil line, and they use the same fluid machine system, tanks and pipes.

### Batch 5: electrochemistry and acids (#52)

| # | Commit | What it adds |
| --- | --- | --- |
| 21 | Brine and the electrolytic cell | Salt + water → brine (chemical mixer). A big electrolytic cell splits brine into chlorine gas, hydrogen gas and lye (sodium hydroxide solution). |
| 22 | Sulfuric acid | A chemical reactor: sulfur dust + water → sulfuric acid, the base of the acid chemistry. |
| 23 | Alumina and real aluminum | Bauxite digested in hot lye → alumina (the Bayer process); the electrolytic cell smelts alumina with a coke anode into aluminum, far better than the blast-furnace stand-in. |
| 24 | Fertilizer | Phosphate + sulfuric acid → fertilizer: a stronger bone meal that ripens crops around it, for the farming pillar. |
| 25 | Hydrogen fuel cell | Hydrogen → JE in a fuel cell (the first electric-look generator), so the cell's byproducts pay for some of its power; handbook, advancements and docs. |

### Rules for the oil line

- **Oil is finite.** Reservoirs run dry and oil sand is an ore; nothing turns power back into crude. Every fuel's JE per bucket is set so refining pays off over burning raw bitumen, and the full chain is audited in BALANCE.md (commit 19).
- **Reachable everywhere.** A player with no reservoir can still get crude from oil sand (commit 5), and shale (fracking) is common where conventional oil is not.
- **Volume is conserved** through refining: the fractions of one bucket of crude add up to one bucket or less.

## Boundaries

- **Fluids** (pipes, tanks, pumps) belong to the fluid system; chemistry only uses them.
- **Mechanical machines** (crusher, press, drawer, alloy smelter, assembler) stay mechanical. Alloying is physical mixing, so it stays in the mechanical branch.
- **No free resources.** Every reaction will be audited like the existing recipes: defined units, losses allowed, gains not.
