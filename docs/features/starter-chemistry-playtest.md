# Starter chemistry machines — local playtest

Owner request, 12 October 2026: implement the next listed machines in the selected visual style and let the owner play locally **before merging to main**. Work by OpenAI Codex (GPT-6 family).

## Scope and player outcome

Four entry forms extend the existing IndustrialForms controller, JE energy, pipe, tank, upgrade and renderer systems. Coal, coke or charcoal becomes dirty synthesis gas. A combined cleaner separates usable CO and hydrogen and captures one mixed residue. A reusable nickel bed lets the Infuser turn CO/CO2 and hydrogen into methane. The existing Gas Burning Generator burns methane for 448 JE/mB at 256 JE/t. Sulfur oxidation, contact conversion and hydrogen chloride synthesis are also included. The Engineer's Handbook describes ports, recipes and tools.

This is the next **machine playtest slice**, not a claim that all of roadmap package 2 is finished. The later local [acid factory follow-up](acid-factory-and-encyclopedia.md) implements wet absorption of HCl/SO3 and the retained reactor charge. Map-wide pollution remains open. The original sulfur + water acid recipe remains available. HCl and SO3 also feed the aqueous-acid routes in that follow-up. No new world generation or changes to the other roadmap packages.

## Construction and appearance

| Machine | Dimensions, width × depth × height | Construction bill | Visible working part |
|---|---|---|---|
| Chemical Infuser | 2 × 3 × 3 | 4 steel plates, 2 steel pipes, Tinplate Tank, copper cable, basic circuit | Rear cooling fan, contained process strip |
| Chemical Oxidizer | 2 × 2 × 3 | 4 steel plates, electric motor, basic circuit, Machine Casing, lined burner | Guarded feed coupling, inspection slit |
| Coal Gasifier | 4 × 4 × 6 | 4 steel plates, 2 bricks, furnace, steel pipe, Machine Casing | Charging-drive coupling, lower heat slit |
| Gas Cleaning and Separation Rack | 3 × 5 × 4 | 4 steel plates, Steel Tank, 2 Tinplate Tanks, electric motor, basic circuit | Pump coupling |

The lined burner combines a furnace and glass before the final craft, fitting the selected ten-ingredient bill into a normal crafting grid without omitting an ingredient. Nickel bed: one nickel ingot and two bricks. Vanadium contact bed: one oxide and two bricks. Four raw iron craft one vanadium-bearing concentrate; the existing Ore Washer extracts one oxide using its normal 500 mB water and 200 ticks. This deliberately spends the iron feed rather than creating both iron and a free catalyst. It is an independent, acid-free starter route; better ore recovery can follow later.

Geometry follows `industrial-machine-models-and-textures.md`: refractory stepped columns, closed flanges, working drawers, service rails, separate ports and clean gunmetal/olive/ceramic surfaces. Existing authored native textures and the shared MachineRotors renderer are reused. The owner library was inspected, including `originals/Blocks/factory/infusing/` and the tile-sound inventory; no library program is executed and no new copied library asset is shipped in this slice. These are code-native models, not generated concept pictures.

## Batch ledger

Amounts are mB. Tool beds are installed once and locked while a batch runs. The first five rows are the owner's selected chemistry baseline; gasification, washing, charcoal oxidation, emissions and the independent vanadium route are explicit playtest defaults.

| Machine / process | Consumed inputs | Outputs | Tool | Work / base draw |
|---|---|---|---|---|
| Infuser, HCl | 250 H2 + 250 Cl2 | 500 HCl gas | None | 100 ticks × 96 JE/t |
| Infuser, CO methanation | 100 CO + 300 H2 | 100 methane + 200 water | Nickel bed | 200 × 128 |
| Infuser, CO2 methanation | 100 CO2 + 400 H2 | 100 methane + 400 water | Nickel bed | 200 × 128 |
| Oxidizer, sulfur | 1 sulfur dust + 250 O2 | 250 SO2 | None | 100 × 96 |
| Infuser, contact | 250 SO2 + 125 O2 | 250 SO3 | Vanadium bed | 100 × 128 |
| Oxidizer, charcoal | 1 charcoal + 250 O2 | 250 CO2 | None | 100 × 96 |
| Gasifier, coal | 1 coal + 1000 water | 1000 dirty gas | None | 200 × 64; 4 emissions units |
| Gasifier, charcoal | 1 charcoal + 500 water | 500 dirty gas | None | 200 × 64; 1 unit |
| Gasifier, coke | 1 coke + 1500 water | 1500 dirty gas | None | 200 × 64; 3 units |
| Cleaning rack | 500 dirty gas + 125 water | 250 CO + 250 H2 + 1 wet mixed residue | None | 100 × 48 |

The cleanup water is bound in the wet residue. One residue + one clay ball + two gravel gives two cobblestone road-fill blocks. No extra fuel or recovered metal is awarded. Gasifier dirty gas cannot itself be burned.

All entry tanks hold 2000 mB. Every solid and fluid output must fit before input escrow starts. Infuser **Drain excess** defaults on: collect plain-water output that fits, drain only the overflow. **Keep water** reserves all water and stops for a full tank. The setting is snapshotted per batch and applies to subsequent starts. Neither mode can discard product gas, acid, lye or a different fluid occupying the water tank. Catalysts are permanent and excluded from automation access.

Energy rate, output disposition and emissions travel with the saved work lane. Older lanes default to their former family energy rate, retained outputs and zero emissions. Completion records the gasifier's numeric emissions once; cancellation returns untransformed inputs and never credits outputs or emissions. The counter is local balancing information, not a simulated pollution map or terrain damage.

No expanded/bulk Separator or free recovered energy is introduced. At four efficiency cards electrolysis still costs at least 168 JE per produced mB H2. CO methanation's cheapest inputs plus process cost exceed the 44,800 JE returned by 100 mB methane, even before assigning a cost to CO. Recovered water is not a fuel. Methane's partial-mB energy credit uses the existing saved generator balance.

## Validation and owner checkpoint

Validated locally on 12 October 2026, based on main `b73f51c8a4d4b6bbb03a7e04974dddc7e796f057`, branch `feature/starter-chemistry-playtest`, Minecraft 26.3, Fabric Loader 0.19.5 and the unchanged framework lock.

- `./gradlew build`: passed; all **1,386 required server tests**, including seven new chemistry scenarios. A final `./gradlew build -PjugcraftOptionalIntegrations=false` also passed all 1,386 tests after the last source correction. These are automated server game tests, not human multiplayer playtests.
- `./gradlew runClientGameTest -PclientTests=StarterChemistryClientGameTests`: passed. Twenty-six actual game screenshots cover four sides of each working machine, paused states and the Infuser menu. Assertions verify that working rotors advance and paused rotors settle. Related IndustrialFoundation, ElectrolyticSeparator and GasBurningGenerator client classes completed successfully in the initial combined run; that run subsequently failed on the new fixture's menu camera distance. The fixture was corrected and the new class rerun. Visual inspection then caught overfilling by the fixture's repeated stocking; bounded top-ups were fixed and the final rerun passed with clean gauges.
- Server coverage: all ten recipe ledgers, paid energy, permanent catalysts, recovered-water policy and wrong-fluid blocking, full gas/residue outputs, saved progress and policy, cancellation without unearned products/emissions, port routing in all four orientations, and methane's fractional energy credit. The shared energy audit checks every speed/efficiency combination.
- `python tools/check_mod_data.py`: passed, including 5,110 block/item models, 442 moving parts, 154 combined models and 2,199 material IDs. `python scripts/check_repository.py`, `python scripts/package_modrinth.py check`, and all 14 `scripts/tests` tests passed. `git diff --check` passed.
- A separate creative world named **Jugcraft Starter Chemistry** is saved with paused, supplied machines and real charged batteries. The local-only Modrinth pack includes that world, the built Jugcraft JAR and the unchanged 18-library manifest; its embedded JAR bytes and world entry were verified. Packaging is not a claim that a Modrinth import was tested. An independent world copy is available to the normal local development client, which is not the automatically closing test client.

Not yet verified: the owner's visual/gameplay acceptance, human survival acquisition, two-player dedicated-server use, optional-libraries-absent client rendering, night lighting and representative factory performance. No remote branch, PR, merge, release or live-server deployment is part of this checkpoint. The owner must approve the local look and play experience before any merge to main.

## Owner feedback: machine connections, 12 October 2026

Implemented with OpenAI Codex (GPT-6 family). The owner liked the working animations and requested visible joins between batteries, cables and every compatible machine. This follow-up preserves those animations, existing power/fluid rules, machine IDs, footprints and saved inventories.

- Cables and fluid pipes now resolve their connection arms one tick after direct/structure placement and neighbour changes. This fixes the earlier showroom's default-state cable stubs. The refresh is scheduled by changes, not continuous ticking or a world scan. An already-saved stale cable needs one adjacent update or replacement to refresh.
- Native socket collars meet the exact block boundary and have a short sleeve into the actual machine casing. The six industrial forms use their declared power, fluid and item ports; no fictitious functional port is added. Batteries, pumps and tanks have flush fittings, and smaller powered devices receive the same treatment: dynamos, motors, solar equipment, chargers, network/drone terminals, the cell rack, fog machine, Ley Pylon and Tesla Coil's powered lower half. Full-cube creative cells and exchange ports already meet their cable faces.
- Large legacy machines retain their all-face compatibility. A permanent service socket marks their controller side. Other fittings appear only where a connected cable or fluid pipe is attached, so open frames are not covered in unused sockets. These use the existing quad-rendering infrastructure, cached for four client ticks with no additional block entities or server tick loop. Compact saved machines retain compact models. The alternate Classic Machines pack supplies its own matching fitting geometry; renderer recreation clears the cache during resource reload.
- Fittings reuse Jugcraft's existing brass, chrome, rubber and electrical materials. No new image textures, dependency changes or owner-library originals are involved. Generated fitting quads join the art audit's existing closed-geometry and overlap checks.

`MachineConnectionGameTests` covers every MachineBlock/form's exposed faces in four orientations, compact saved variants, real energy/fluid sided lookups, direct cable placement and battery removal/replacement. The expanded `StarterChemistryClientGameTests` verifies actual battery/cable chains, rotor stop/start behaviour, conditional fittings appearing/disappearing, smaller powered devices and tank pipe arms. Its separate **Jugcraft Connected Machines** world is intended for the next owner playtest; the previous manual save remains untouched.

Follow-up validation:

- Final `./gradlew build`: passed all **1,388 required server game tests**, including the expanded fluid-only tank/boiler/foundry cases. The preceding optional-integrations-absent build passed 1,388 tests too; the last expansion added test coverage and fluid-only fitting assets without changing server runtime code.
- Final `./gradlew runClientGameTest -PclientTests=StarterChemistryClientGameTests`: passed with **52 screenshots**. The full steel tank and gas holder are placed in the saved world, and assertions verify their additional pipe fittings. Visual inspection includes battery/Infuser joints, a large circuit assembler with conditional fittings, motors, terminals, fog equipment and the three tanks.
- Final data/art audit passed **5,110 block/item models, 3,484 quad parts** (including both styles' fittings), **154 combined assemblies** and 2,199 material IDs. The audit caught and the implementation corrected elbow UV overlaps and motor-shaft/sleeve overlap; no art exception was added.
- Repository/link checks, `git diff --check`, the 18-library distribution lock check and all 14 packaging/selection unit tests passed. The local pack's embedded JAR is byte-identical to the build; it includes all 45 large-machine fitting profiles and the separate saved test world.

Still unverified: owner acceptance, a Modrinth UI import, live switching to Classic Machines, optional-libraries-absent client rendering, two-player use, night lighting and representative factory performance. This follow-up remains local, with no remote push or merge.
