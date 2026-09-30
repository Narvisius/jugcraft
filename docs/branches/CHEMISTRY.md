# Chemistry branch

Status: **planned. Nothing in this branch is implemented yet.** This document reserves the branch's scope so mechanical and fluid work stays out of it. See [../TECH_TREE.md](../TECH_TREE.md) for how it fits with the rest of Jugcraft.

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
| Salt | Rock salt ore | None | Brine electrolysis → lye (sodium hydroxide) and chlorine |
| Bauxite | Surface rock | Blast furnace → 1 aluminum nugget; arc furnace → 1 aluminum ingot | Lye digestion → alumina, then electrolysis → aluminum (the real route) |
| Sulfur dust | Crushed vanilla sulfur | None | Sulfuric acid |
| Phosphate | Phosphorite ore | None | Phosphate + sulfuric acid → fertilizer (for the farming pillar) |
| Lepidolite / lithium carbonate | Lepidolite ore | Blast or arc furnace → lithium carbonate | Leaching and precipitation; battery compounds |
| Monazite / rare earth oxide | Monazite ore | Blast or arc furnace → rare earth oxide | Acid digestion and solvent extraction → separated rare earths (magnets) |
| Bitumen | Oil sand | Steam generator fuel | Upgrading and refining alongside liquid crude oil |

## Planned machines (proposals)

| Machine | Reaction | Needs first |
| --- | --- | --- |
| Electrolytic Cell | Brine → lye + chlorine; alumina → aluminum | Fluid system (water, brine) |
| Chemical Reactor | Sulfur + water → sulfuric acid; phosphate + acid → fertilizer | Fluid system |
| Leaching Vat | Ores and oxides + acid → dissolved salts → purified products | Chemical reactor, fluid system |
| Refinery (multiblock) | Crude oil → fuel, lubricant, plastic, asphalt | Fluid system, liquid crude oil |

## Boundaries

- **Fluids** (pipes, tanks, pumps) belong to the fluid system; chemistry only uses them.
- **Mechanical machines** (crusher, press, drawer, alloy smelter, assembler) stay mechanical. Alloying is physical mixing, so it stays in the mechanical branch.
- **No free resources.** Every reaction will be audited like the existing recipes: defined units, losses allowed, gains not.
