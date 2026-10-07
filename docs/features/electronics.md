# Electronics: silicon, chips and the cyan look

Status: implemented (batch 7, #56)
**Batch 24:** the crystal grower was folded into the arc furnace, which now pulls the silicon boules; see [machine-consolidation.md](machine-consolidation.md). Its section below is kept as history.
Proposal issue: owner request, 1 October 2026 ("merge it and start the next batch"), following suggestion 3 from batch 5 ("Electronics tier in the cyan look")
Owner: jimbozoomer-byte
Target milestone and tier: the high-tech tier after oil and chemistry
Primary specialty and supported player role: technology; the player who builds factories and automates them

## Plan

| # | Commit | What it adds |
| --- | --- | --- |
| 31 | The cyan look and the crystal grower | High-tech textures (near-black casings with cyan seams, cyan glass and screens, violet conduits); the crystal grower; silicon boules and wafers. |
| 32 | Lithography | A cleanroom lithography station: wafers etched with sulfuric acid into microchips. |
| 33 | Processors | Processors from microchips, gold wire and an advanced circuit in the circuit assembler. |
| 34 | Network terminal | A beige retro computer that reads out the power network it is cabled to. |
| 35 | Docs and PR | Advancements, handbook, docs, screenshots. |

## Player experience

### The cyan look (commit 31)
The owner's references for the tier after oil: dark sci-fi casings with cyan glass panels and screens, a dark multi-block with violet and cyan conduits and a monitor bank with a keyboard, and a beige retro computer. New original textures (`tools/electric_textures.py`): `el_dark` (near-black panels with a cyan trace in the groove), `el_glass` / `el_glass_on` (cyan glass, drawn opaque, glowing inside while the machine runs), `el_glow_violet` (emissive), `el_conduit` (a violet-lit conduit), `el_boule` (silicon crystal), `el_screen_cyan` / `_on`, and the beige `rt_` textures for the retro computer. Models: `tools/hightech_models.py`.

### Crystal grower (commit 31)
- **Two blocks tall:** a dark control cabinet with a cyan screen, a status lamp and power ports, under a glass growth chamber where a silicon boule hangs on its pull rod over a glowing crucible ring, with the pull head on top and violet conduits up the back.
- **4 silicon + 1 phosphate → 1 silicon boule**, 400 ticks at 128 JE/t (51,200 JE). The phosphate is the dopant (phosphorus makes n-type silicon).
- The **sawmill** cuts a boule into **8 silicon wafers** (200 ticks).
- Recipe: glass, titanium ingot, an arc furnace casing, aluminum plates and an advanced circuit.

### Lithography (commit 32)
- **Lithography station** (three wide, two tall, two deep): a cleanroom with a long cyan glass window that glows while the stepper inside works, a pass-through hatch and a filter unit on the roof; beside it the operator's desk with a keyboard and a bank of four cyan monitors; violet and cyan conduits along the top.
- **1 silicon wafer + 2 copper wire + 100 mB sulfuric acid → 4 microchips**, 200 ticks at 192 JE/t (38,400 JE). Its 4-bucket tank takes the acid by pipe.
- A fluid processor like the chemical reactor (`FluidMachineSpec([4000], [], 2, 1)`, recipe type `jugcraft:lithography`), so pipes, side config, the recipe viewer and the metal audit all work as they do there.
- Recipe: glass, a redstone lamp, titanium ingots, an advanced circuit, aluminum plates and a machine casing.

### Processors (commit 33)
- **4 microchips + 1 advanced circuit + 1 gold ingot → 1 processor** in the circuit assembler (400 ticks at 32 JE/t). The third circuit tier, after basic and advanced; tagged with the other circuits in `JugcraftComponents.CIRCUITS`.
- Used by the network terminal (commit 34) and kept for the tiers above this one.

### Network terminal (commit 34)
- **A beige retro computer** (one block, one of the owner's references): a desktop case with drive bays and a power lamp, a CRT monitor showing a cyan readout, a keyboard in front, and power ports on its sides.
- **Cable it into a power network and right-click it:** it shows the network's cables, the rate its slowest cable sets, how many devices the network reaches, and the energy they hold (with a percentage).
  - Each device counts once, even a multi-block machine touching the cables with several blocks.
- It uses no power, stores none, and is not a device on the network; cables connect to it on every side.
- Recipe: glass panes, a processor, plastic sheets, redstone, copper cables and a button.
- Code: `electronics/NetworkTerminalBlock`, `JugcraftElectronics`; `EnergyNetworks.view` exposes a network's cables, rate and storage faces.

### Advancements
Pulling Strings (silicon boule), Etched in Light (microchips), Central Processing (processor) and Hello, World (network terminal), after Kroll Call.

## Connections
- Existing input producer: silicon (arc furnace from quartz), phosphate (mined), titanium (batch 6), the sawmill.
- Existing output consumer: wafers go to lithography, microchips to processors, processors to the network terminal (commit 34).
- Technology connection: power network, processing machines, recipe viewer.
- Magic connection: none.
- Reachable entry path: silicon, phosphate and titanium are all reachable before this tier; no circular unlock.
- Required vs optional: optional; existing circuits keep their recipes.

## Balance and automation
- A boule costs 4 silicon and 51,200 JE and gives 8 wafers: each wafer costs half a silicon and 6,400 JE before the sawmill.
- A microchip costs a quarter wafer, half a copper wire, 25 mB of sulfuric acid and 9,600 JE of etching: about 11,200 JE a chip in all, plus the acid's own cost.
- The metal audit allows the lithography recipe: copper wire goes in and no metal comes out.
- A processor holds four chips (about 45,000 JE), an advanced circuit and a gold ingot. The gold is used up.
- No item turns back into silicon, and no machine here makes power, so there is no loop.

## Multiplayer and persistence
Server-side machines like the others; the crystal grower and lithography station save with their block entities. The terminal reads the network on the server when used and sends the result to that player only. It has no block entity and no saved state.

## Dependencies and assets
No new dependencies. Textures and models are original (`tools/electric_textures.py`, `tools/hightech_models.py`, `tools/petro_textures.py`).

## Verification
- `tools/check_mod_data.py` checks the new IDs, recipes and models.
- Game tests `crystalGrowerPullsABoule` (JugcraftGameTests), which also checks the sawmill's boule recipe, and `lithographyMakesMicrochips` (PetroGameTests) `circuitAssemblerMakesAProcessor` and `networkTerminalReadsItsNetwork` (JugcraftGameTests).

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.
