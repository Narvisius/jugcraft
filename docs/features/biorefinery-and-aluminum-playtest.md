# Expanded fermentation and first aluminum refinery — local playtest

Status: implemented locally on `feature/starter-chemistry-playtest`, 12 October 2026. Build, server, model/data and rendered-client validation passed; separate local playtest pack prepared. Owner requested expanded fermentation/biofuel followed by the beginning of expanded aluminum production. AI implementation: OpenAI Codex (GPT-6 family). Nothing published or merged by this task.

## Player routes

Farm crops → Heated Mashing Station → Industrial Fermenter → Chemical Distillation Station → existing Bioethanol consumers, including the Bio-Generator. Distillery stillage feeds the Anaerobic Digester. Optional wet fermentation CO2 cleanup reuses the Biogas Cleaner; its clean CO2 supplies existing methane synthesis. Specialty players can sell crops, mash, ethanol, gas or recovered materials without owning every branch.

Bauxite → Bauxite Digestion Vessel → Aluminate Recovery Press → Alumina Calciner → Aluminum Reduction Cell → existing aluminum wire, cable, plates and compatible structural/vehicle recipes. Obtain bauxite through its existing source or trade, lye from the existing copper-built Electrolytic Separator, and coke through the existing carbon route. All seven new machine bills use earlier steel, copper, motors, basic circuits, tanks and/or clay bricks; none needs aluminum or its own chemical output.

## Implemented stations and provisional batch balance

| Station | Footprint (width × depth × height) | One batch | Time / power |
|---|---|---|---|
| Heated Mashing Station | 3 × 3 × 3 | 16 existing fermentable crops + 1000 mB water → 1250 mB heated mash | 200 ticks × 24 JE/t |
| Industrial Fermenter | 3 × 3 × 4 | 1250 mB mash → 1000 mB broth + 250 mB wet CO2 | 400 × 8 |
| Chemical Distillation Station | 4 × 4 × 6 | 1000 mB broth → 500 mB existing bioethanol + 500 mB stillage | 200 × 32 |
| Bauxite Digestion Vessel | 3 × 4 × 3 | 1 bauxite + 250 mB lye + 750 mB water → 1000 mB slurry | 200 × 64 |
| Aluminate Recovery Press | 3 × 4 × 3 | 1000 mB slurry → 2 hydroxide + 1 residue + 200 mB lye + 500 mB water | 160 × 32 |
| Alumina Calciner | 3 × 4 × 3 | 2 hydroxide → 2 existing alumina + 250 mB water | 200 × 64 |
| Aluminum Reduction Cell | 3 × 3 × 4 | 2 alumina + 1 coke → 2 existing ingots + 250 mB CO2 | 400 × 96 |

New forms use the existing ENTRY profile: one lane, 2000 mB named tanks and existing speed/efficiency cards. Durations include paid automatic heating. Values are game balance for testing, not real chemical stoichiometry.

Supporting recipes:

- Existing Biogas Cleaner: 250 mB wet fermentation CO2 + 250 mB wash water → 250 mB usable CO2 + 250 mB recovered water; 100 ticks × 24 JE/t. Drain old biogas/wash contents before changing recipes; products are not mixed.
- Existing Anaerobic Digester: 1000 mB stillage → 250 mB raw biogas + 500 mB digestate; 300 ticks × 24 base JE/t, rounded to 22 JE/t per EXPANDED lane. Continue through existing cleaning and digestate separation. No solid-feed requirement for this fluid recipe.
- One bauxite residue + one clay ball + two gravel → two cobblestone as stabilized game filler. It is not interchangeable with gas-cleaning residue, fertilizer or edible/digestible crops.

Before upgrades, the ethanol route consumes 14,400 JE to make 500 mB ethanol from 16 crops. That ethanol gives 96,000 JE in the Bio-Generator, leaving 81,600 JE before transport, optional gas capture and residue treatment. The older direct reactor recipe remains available. Agriculture supplies the finite fuel source; water, fertilizer, hydroxide and residue are not fermentable. Recycling stillage recovers a smaller portion of that same finite crop batch and never regenerates crops or mash.

One bauxite consumes 69,120 JE through the new chain and still yields exactly two ingots, matching existing metal accounting. Recover 200/250 mB lye (50 mB makeup); press plus calciner recover the initial 750 mB water. The reduction cell uses the shared electrolysis family and its 60,000 JE buffer; distillation uses the shared tower family. Any machine safely pauses if its power supply runs out. Coke is consumed once, not repaired as equipment. These initial stages connect to existing aluminum products; this task does not add the broader future alloy/product catalog.

## Shared behavior and compatibility

Existing form controllers own authoritative processing, paid work, output reservations, cancellation, save/reload, upgrades, inventory and Fabric-sided transport. Models are sliced native block geometry, with shared MachineRotors for working drives and existing physical cable/pipe sockets. No new power currency, renderer, pressure simulation, external dependency or chunk loader.

Fermenter and reduction-cell gas overflow reuses the form's saved per-batch overflow policy with a form-declared exact gas identity. Default collects what fits and vents only excess. The button is labelled Vent excess / Keep gas. Keep gas stops before spending inputs when storage is full. A running batch retains its original setting across toggles and reloads. Broth, ethanol, lye, water and solid products on these forms are never vented. Vented gas contributes once to the existing local process-emissions counter; captured gas does not. No new world pollution simulation is claimed.

Legacy `drain_water` / `drained_water` save keys and existing public water accessors are preserved for compatibility; on gas-enabled forms the latter counts vented mB. Existing water-drain forms still match only ordinary water. Old machine IDs, recipes, fluids and worlds remain intact; new content uses new stable IDs. Nothing migrates or rewrites existing chunks.

The Engineer's Handbook, Encyclopedia, JEI recipe data, personal acquisition goals, creative entries, crafting recipes, loot and transport ports include all seven machines. Existing tanks, pipes, cables, conveyors and gas cylinders handle their contents. No redundant new transport blocks are necessary.

## Art provenance

Seven new native models reuse Jugcraft's existing muted steel, olive, ceramic, copper, rubber and instrument textures. The supplied owner catalog was searched before modeling; broad machinery sheets are not stretched onto these new silhouettes. Geometry includes feed hatches, flanges, guarded drives, condensation columns, filter plates, kiln lining and copper busbars. Their motion uses the existing lit-state rotor system.

Following owner feedback, tanks, hoppers and pump housings have seated supports; hatches have throats into their machines. Every fluid socket has an explicit short route terminating inside a vessel, condenser, tray or housing. Working ports follow those positions, including low slurry drains and high fermentation-gas extraction. The bauxite vessel's decorative open pipe and unused upper block layer were removed. A layout regression test checks every fluid route starts at its registered socket, stays axis-aligned and ends inside the authored body before pipes are added.

Two owner-supplied 16×16 icons are copied unchanged from `art/owner-library/originals/Blocks/Big Cannons and Mounted Guns/textures/item/`: `filteredaluminate.png` → `aluminum_hydroxide.png`; `bauxitedust.png` → `bauxite_residue.png`. Originals and their supplied provenance remain intact. New process-fluid sprites use the established seeded fluid generator, with appended identities to preserve older seeds. No library programs or JARs are executed.

## Validation and local testing

- Full build and all **1,404 required server game tests passed**, including the five new refinery tests and extended digester/cleaner ledgers. Checks cover exact resource/energy costs, gas capture versus venting, save/reload, cancelled feed refunds, blocked solid outputs, reachable distinct crafting bills and rotated transport faces.
- The final rendered client pass checked all seven revised machines working/paused from four directions, opened their real menus, verified rotor motion/stopping and saved a stocked battery-connected yard. Forty-nine screenshots were captured. Front and rear views of every machine, the overflow menu and the reduction-cell cable connection were inspected. The final build plus server/client run completed successfully in 6m 30s. A separate manual save and local Modrinth pack include the stocked, paused stations; earlier manual worlds remain intact.
- Full material/data audit passed: **2,219 material IDs**. After the final pipe/support revision, the texture/model audit passed **6,106 block/item models**, 3,496 quad parts and 154 assembled models. Existing art allow-lists remain unchanged. Final shared-family recipe mappings, revised port positions, form dimensions and exact gas overflow declarations passed a separate targeted audit.
- Repository structure/links and **23 Python tests passed**, including the new physical pipe-route check. All **18 dependency locks** passed packaging configuration checks.
- Initial test development caught a misnamed helper and an underpowered one-buffer test fixture. Both were corrected; paid energy accounting now includes explicit fixture refills when needed. Distillation/reduction use their existing shared machine families. No required checks were disabled.
- Not performed: human two-client play, optional-library-absence testing for this addition, Modrinth application import or representative server-load performance measurements. Automated rendered tests are not an owner playtest.

## Later work

Larger fermenter/Bio-Generator variants; ethanol dehydration when a demanding consumer is implemented; broader renewable chemical/plastic uses; large molten-salt hall; aluminum-magnesium alloy with independent magnesium supply; new electrical, structural, panel and vehicle products; broader progression balance and human multiplayer playtesting.
