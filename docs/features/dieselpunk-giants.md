# Dieselpunk giants (batch 44)

Status: implemented (pending CI and review)
Proposal issue: none; the owner asked in chat for much bigger, very detailed workshops (2x2x1 up to 5x5x5, some long, not all square), in the weathered dieselpunk look of their reference pictures.
Owner: jimbozoomer-byte
Target milestone and tier: every tier that had one-block machines
Primary specialty and supported player role: industry and building (visual scale; no balance change)

## Player experience
Sixteen machines that used to be one block are now big multi-blocks in a weathered, patina-green dieselpunk style. They stand on skid feet, with riveted steel plate, perforated green covers, ribbed coil stacks, banded domes, copper and steel pipe runs with hex-nut fittings, red handwheels, blue caps, gauges and glowing amber windows. Placing the item builds the whole machine if the space is free (like the existing multi-blocks); breaking any block removes all of it and drops the one item.

| Machine | Size (wide x tall x deep) | What it looks like |
| --- | --- | --- |
| Coal Generator | 2 x 2 x 3 | Firebox and hopper, boiler barrel with steam dome, perforated dynamo, smokestack |
| Steam Generator | 3 x 3 x 2 | Boiler under a ribbed dome, steam main into a flanged turbine with an intake fan |
| Electric Furnace | 2 x 2 x 2 | Red crucible between two green induction coil stacks, hood fan, control cabinet |
| Crusher | 2 x 3 x 2 | Jaw crusher in a red frame, big hopper, flywheels, belted motor |
| Metal Press | 2 x 3 x 2 | Four-column hydraulic press, twin cylinders, red ram, pump |
| Wire Drawer | 4 x 1 x 2 | Long bench: pay-off reel, three die boxes, capstan |
| Circuit Assembler | 3 x 2 x 2 | Pick-and-place gantry over a belt, amber CRT console, reel cabinet |
| Pulverizer | 3 x 2 x 2 | Ball mill drum with ring gear, pinion motor, hopper and chute |
| Ore Washer | 2 x 2 x 4 | Rotary drum over a long sluice of tanks, spray bars, settling tank |
| Sieve | 2 x 2 x 3 | Vibrating screen tilted on coil springs: feed hopper, grizzly bars, woven-wire deck, vibrator motor with spinning weights, totes of fines, flint and nuggets |
| Sawmill | 2 x 2 x 5 | Log carriage on rails into a big toothed blade that turns while it runs, under a hood, belted from a motor; dust duct |
| Fuel Cell | 2 x 2 x 2 | Clamped cell stack, gas manifolds, inverter cabinet |
| Hydroponic Bay | 3 x 2 x 3 | Open grow house: three crop trays under amber lamps, nutrient tank |
| Electroplating Bath | 4 x 2 x 2 | Three tanks under a gantry hoist, busbars, rectifier |
| Ammonia Chiller | 2 x 3 x 2 | Frosted condenser tower with fan, compressor, receiver drum |
| Rocket Workshop | 5 x 3 x 3 | Assembly hall: gantry crane over a rocket in cradles, workbench, console, propellant tank |

The battery box, solar panel, cobblestone generator, auto-crafter, tree farm and arc furnace controller stay one block: they work on what is right next to them (sky, water and lava, the planting area, the arc furnace casing), or are meant to be packed tightly.

## Connections
- Existing input producer / output consumer: unchanged; every recipe, slot, tank, side setting and upgrade is the same.
- Technology connection: the same machines. Cables, pipes, conveyors, hoppers and comparators reach the machine through any of its blocks (shared multi-block code).
- Magic connection: none.
- Reachable entry path: unchanged recipes.
- For infrastructure/cosmetics: this is a model and footprint change; resource links do not apply.

## Balance and automation
No recipe, speed, energy or capacity changes. The steam generator and ore washer still refill from a water source under their master block (the front left bottom block). Nothing new is produced, so no loop is possible.

## Multiplayer and persistence
Placement and forming run on the server (`LargeMachineBlock`). **Old worlds:** each enlarged block has a new `compact` property, default `true`. A machine saved before this change has no such property, so it loads as `compact=true`: one block, the old model, and exactly the old behaviour (`EnlargedMachineBlock.footprint` returns a single block). Placing the item always builds the full machine (`compact=false`). Commands and structures that place the default state also get the compact block, as before. A compact copy can still be turned with the wrench; the full machine cannot.

## Dependencies and assets
No dependencies. All textures are original and drawn by code (`tools/dieselrust_textures.py`, names `dr_*`); the models are in `tools/giant_models.py`. The classic style pack gets simple plinth-and-body versions (`large_machines.classic_giant`).

## Verification
The sawmill's and sieve's turning parts (5 October 2026) have their own evidence below (see [Turning parts](#turning-parts-sawmill-and-sieve-5-october-2026)). Planned (CI): game tests `enlargedCrusherFormsAndOldCopiesKeepWorking` (all 12 blocks form, the top back block reaches the slots, the big crusher and a compact one both crush) and `breakingAnEnlargedMachineRemovesItAll` (breaking the far end of a sawmill removes every block). Every existing machine test still places the default (compact) state, so the compact path is covered by them. The client screenshot showroom builds every multi-block, now including the sixteen giants. Local: `check_mod_data.py` and offline model renders. Not done: survival play-test, dedicated server with two clients, performance measurement.

## World and event applicability
Not applicable: machines are placed by players only.

## Turning parts: sawmill and sieve (5 October 2026)

The owner's review asked for the sawmill's saw to spin and for the sieve to be better detailed and more interesting.

- **Tier, inputs, outputs, costs:** unchanged (the same machines, recipes, energy and slots). Visual only; nothing is produced, so no loop.
- **Sawmill:** the blade is no longer part of the block models. `client/MachineRotors` draws it through the machines' block entity renderer (`WindTurbineRenderer`, the only one on `MACHINE_ENTITY`) from `assets/jugcraft/machine_rotor_quads.json`, written by `tools/machine_rotors.py` from `giant_models.ROTORS`. It is a true 24-tooth disc (the quads are polygons, not stepped boxes, so it stays round turning) with flanges, arbor nut, arbor and drive pulley; the motor's pulley turns with it. Speed −7° a tick (the front teeth cut down into the log; 7° a tick stays under half the 15° tooth pitch a frame down to 20 frames a second, so the teeth never seem to run backwards on a slow machine or a recording). The bed has a slot for its lower arc with sawdust in it; a hood (cheek, top and back plates) covers its top back quarter and leaves the front teeth bare; pillow blocks (a foot plate, a housing with its bearing face centred on the arbor, and a cap) on red pedestals carry the arbor, each housing wholly in the block above the y 16 seam so its bearing face is drawn once; the duct runs from the hood's back down to the bed inside the machine's blocks, with plain red flanged joints (it runs along the x 0 seam, where a hex-nut face would be cut in two); the running lamp and gauge stand on a control box on the master block. Textures `dr_saw_disc` (32×32, mapped once across the disc), `dr_saw_edge`, `dr_flange`, `dr_pulley`, `dr_sawdust`, `dr_red_paint` (`tools/mill_textures.py`); the old checkered `dr_blade` is no longer used by the sawmill.
- **Sieve:** a red screen box tilted 22.5° (high at the back under the feed hopper's chute, low at the front over the totes, so its deck faces you from the front) on four blue coil springs (one-pixel coils with one-pixel gaps: no sub-pixel rings, which shimmer) on red pedestals. On the deck: a grizzly of real steel bars with gravel riding on it, then a fine woven-wire deck (`dr_screen_mesh`, 32×32, period 4) with steel cross bars every 8 pixels, and a hazard-striped discharge lip. A vibrator motor on a beam across the middle spins two yellow eccentric weights (`MachineRotors`, 24° a tick). Totes of fines, flint and nuggets at the front, a pan of fines under the deck, and the control box with the gauge and running lamp on the master block. The tilted box is cut into pieces of at most 16 pixels (`giant_models.tilted`), so no texture is stretched; the 24-pixel-wide pieces are cut 8 + 16 (`WIDE_CUT`), multiples of the mesh's, bolts' and stripes' repeats, so the weave and the hazard stripes run on across the cut, and the motor beam spans the full width. Textures `dr_screen_frame`, `dr_screen_mesh`, `dr_spring`, `dr_gravel`, `dr_flint_heap`, `dr_nugget_heap`, `dr_fines`, `dr_weight`.
- **How the parts turn:** each rotor is drawn standing still while the machine is idle and turning while its master block is `lit`, only when `compact=false` (a one-block copy from before batch 44 keeps its old model). `MachineRotors` keeps each machine's angle on the client and eases every speed change over 8–12 ticks, so a machine stopping and starting on weak power slows and speeds up rather than jumping. Parts are turned with the machine's facing about the master block's centre, as its block models are.
- **Both styles:** the rotors are drawn in the classic pack too (the file is cached for the session, like `kinetic_rotors.json`), so the classic sawmill (`large_machines.classic_sawmill`) leaves a slot between two tanks for the blade and the classic sieve's body is tall enough to hide the weights. The item icons add the blade and pulleys (`giant_models.ITEM_EXTRAS`).
- **Failure behaviour:** without `machine_rotor_quads.json` (a broken resource pack) the parts are not drawn and a warning is logged; the machines work as before. Past the renderer's view distance (160 blocks) the turning parts are not drawn, like the wind turbine's rotor: from that far the blade slot under the hood looks empty and the sieve's motor has no weights. Accepted: the renderer has no frustum test (`shouldRenderOffScreen`), so a longer distance would draw every machine's rotors in a wider ring every frame.
- **Lamps on the master block:** only part 0's `lit` state changes, so both machines' amber lamps now sit on part 0, where they light up while running (they were on parts that never light).
- **Checks:** `check_mod_data.check_machine_rotors` checks every rotor's machine, keys and `compact=false` condition, opaque still textures and UVs inside 0..1, that the renderer reads the file, that no static element of either style cuts into the blade's slab (`clear` in `giant_models.ROTORS`), that every unrotated sawmill and sieve element stays inside its footprint, that no stretched-texture face of either machine (in either style) is cut by a block seam, and that only part 0 carries the amber lamp.
- **Test evidence (planned for CI, not run here):** server game test `MachineMotionGameTests.giantSawmillAndSieveLightTheirMasterWhileRunning` (a formed, powered sawmill and sieve light their master block while they make planks and flint); client game test `MachineMotionClientGameTests` with screenshots `jugcraft_sawmill_sieve_running`, `jugcraft_sawmill_blade_a` and `_b` (three ticks apart, so the blade and pulleys have turned 21 degrees), `jugcraft_sieve_running` and `jugcraft_sieve_running_corner`. The existing `sawmillCutsLogs` and `sieveSiftsGravel` still cover the compact copies. Offline: generators, `check_mod_data.py`, `check_repository.py`, a javac parse, and renders of both styles, the rotors at several angles and the test's camera shots.

## Rollout and open questions
- A compact copy is not upgraded automatically; break it and place it again to get the big machine (that needs the free space).
- Sizes and looks are first passes for the owner to review.
