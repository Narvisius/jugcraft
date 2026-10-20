# Aged wood (nitric-acid-treated pine and maple)

Status: implemented on the contributor's fork, pending the owner's review.

Proposal issue: none. TODO item: "Extend nitric/ammonia chemistry … review owner-requested aged pine/maple finishes separately." The chemical catalog records it as "an owner-requested decorative woodworking branch; exact color variants/process applicability need review". This change is a first version for that review.

**This change's choices:** the two colours, the treatment (8 planks + 100 mB nitric acid → 8 aged planks in the Chemical Reactor), and planks, stairs and slabs only (no fences or doors). The owner can review all three.

Owner: jimbozoomer-byte
Target milestone and tier: after nitric acid (Synthesis Converter) and the biomes branch's pine and maple
Primary specialty and supported player role: building; players who want darker, warmer wood from the chemistry line

## Player experience

- **Aged Pine Planks:** pine's olive planks turned a honey amber.
- **Aged Maple Planks:** maple's pale tan planks turned a deep red-brown.
- Each wood also gets **Aged … Stairs** (6 planks → 4) and an **Aged … Slab** (3 planks → 6), crafted like vanilla's.
- **Making them:** the Chemical Reactor takes **8 pine or maple planks and 100 mB of nitric acid** and gives **8 aged planks** in 100 ticks. There is no way back to plain planks; the acid is spent.
- They behave like oak planks in every way but the look: the same hardness, axe mining, fire spread and furnace fuel. They are in `#minecraft:planks`, `#minecraft:wooden_stairs` and `#minecraft:wooden_slabs`, so they make sticks, crafting tables and anything else that takes planks.

## Connections

- **Input producers:** pine and maple planks (biomes branch trees) and nitric acid (Synthesis Converter: ammonia and oxygen).
- **Output consumers:** building, and every planks recipe.
- **Machines:** the existing Chemical Reactor. Nothing new to build.
- **Optional:** purely decorative; nothing requires it.

## Balance

Aging costs acid and gains nothing but the look. Aged planks made into sticks give the same sticks as plain planks, so there is no loop.

## Multiplayer and persistence

- **New registrations,** stable from now on:
  - the blocks and items `jugcraft:aged_pine_planks`, `aged_pine_stairs`, `aged_pine_slab`, `aged_maple_planks`, `aged_maple_stairs` and `aged_maple_slab`;
  - the recipes `chemical_reaction/aged_pine_planks`, `chemical_reaction/aged_maple_planks` and the four stairs and slab recipes.
- They load with the `biomes` switch, like the woods they come from. The reactor recipe also needs `machines`.

## Dependencies and assets

There are no new dependencies. `tools/aged_wood.py` draws the textures from Jugcraft's own pine and maple plank textures, keeping their grain. Each tone moves by brightness rank onto an aged ramp (seven tones for pine, four for maple). No Mojang texture is read, traced or recoloured.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py` and `python scripts/check_repository.py` passed. The textures were inspected tiled 3×3 beside the plain planks.

**Server game tests for CI** (`AgedWoodGameTests`):

| Test | What it checks |
| --- | --- |
| `reactorAgesPineAndMaple` | For pine and maple, the reactor recipe takes 8 planks and 100 mB of nitric acid and gives 8 aged planks. |
| `agedPlanksCraftStairsSlabsAndCountAsPlanks` | Aged planks are `#minecraft:planks`; six craft four stairs and three craft six slabs. |

**Results:** the fork's Build workflow passed on commit 62d1a3c5 ([run 39162549690](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39162549690)): compilation, the headless server game tests including `AgedWoodGameTests`, and the rendered-client shards.

**Not run:** the blocks have not been placed in a running game client, and there is no multiplayer playtest.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
