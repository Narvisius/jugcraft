# Glass chemistry: borax, borosilicate glass and optical fibre

Status: implemented on `feature/chemistry-16` (batch 16). Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner's chemistry list, item 4 (1 October 2026), from the saved backlog in [MACHINE_ROADMAP.md](../MACHINE_ROADMAP.md).
Owner: jimbozoomer-byte
Target milestone and tier: electronics and advanced materials
Primary specialty and supported player role: chemistry and electronics

## Player experience
- **Tincal:** natural borax that crusts the sand of deserts and badlands (a surface rock, like oil sand). It needs a pickaxe and drops 1-3 **borax**.
- **Borosilicate glass:** 2 sand and a borax melt into 2 in the alloy smelter.
- **Optical fibre:** the wire drawer pulls each borosilicate glass into 4.
  - **Processor:** 4 microchips, an advanced circuit and 2 optical fibre make a processor, with **no gold ingot**.
- **Ferroboron:** an iron ingot and a borax in the alloy smelter.
  - **NdFeB magnets:** a rare earth oxide with ferroboron makes **two** neodymium magnets; with plain iron it makes one.
- Borosilicate glass is also meant for the flow batteries (next batch) and glass tanks (with the tank gauges).

## Connections
- Input producer: deserts and badlands (tincal), sand, iron, rare earths, microchips.
- Output consumer: processors (electronics), magnets (magnet dynamo and motor, the advanced engine), later flow batteries and glass tanks.
- Reachable entry path: the alloy smelter and wire drawer are early machines; tincal is found on the surface.
- Required vs optional: optional; the old recipes stay.

## Balance and automation
- A processor can trade one gold ingot for 2 optical fibre, which is half a borosilicate glass: one sand and half a borax.
- Ferroboron doubles magnet output per rare earth oxide, the scarce input; it costs an iron and a borax per two magnets.
- No metal is created: the audit passes.
- **Recipe order:** the alloy smelter and circuit assembler now try recipes with more ingredients first, so a specific recipe is never shadowed by a simpler one.

## Multiplayer and persistence
Items and a world block only.

## Dependencies and assets
None new. Textures are original (`tools/generate_textures.py`).

## Verification
- `tools/check_mod_data.py` passes.
- Game test `glassChemistryRecipes`: tincal drops, all five recipes resolve to the right results, and the magnet recipe picks ferroboron.
- Not run: client play, worldgen in a real world, multiplayer.

## World and event applicability
- Tincal is placed on sand at Y 55-100 in deserts and badlands, in newly generated chunks only.
- The `silicon` feature switch turns it off.

## Rollout and open questions
- Glass tanks that show their fluid come with the tank gauge work (owner list item 8).
