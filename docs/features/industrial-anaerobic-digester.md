# Anaerobic Digester: biogas from organic waste (factory package 2)

Status: implemented (CI green on the contributor's fork; pending review).
Stacked on the [Bulk Chemical Reactor](industrial-bulk-chemical-reactor.md) and the branches below it, down to the [Chemical Infuser](industrial-chemical-infuser.md), whose nickel bed upgrades the biogas.
Proposal issue: none. TODO item: "Design the substantial first bulk Anaerobic Digester … using the selected 60/40 methane/CO2 game mixture, usable digestate … and optional hydrogen upgrading into separated methane."
- **From the owner's plan:**
  - the 6×6×6 size and the model ([art brief](industrial-machine-models-and-textures.md#anaerobic-digester));
  - the 60/40 mixture and the upgrading mapping ([starter gas and acid factory](industrial-starter-gas-and-acid-factory.md));
  - its port roles ([construction and operation](industrial-machine-construction-and-operation.md): wet feed, raw biogas and digestate).
- **This change's choices:** which waste it takes, the rates, the digestate and the bill. The plan left these open, the contributor asked for reasonable numbers, and they are listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the bio line of the starter gas and acid factory
Primary specialty and supported player role: farming and waste into fuel

## Player experience

The **Anaerobic Digester** is a 6×6×6 factory landmark: a huge sealed tank on four foundation shoes, ringed with compression rings, under a faceted dome.

- It runs **four batches at once** from 16,000 mB tanks, at 80% of the reactor's draw: 77 JE/t a batch. This is the foundation's bulk profile.
- **What goes in:** feed organic waste at the auger on its left side and water at the inlet beside it.
  - Accepted waste is the new tag `#jugcraft:digestible`: the fermentable crops, rotten flesh, poisonous potatoes, kelp, pumpkins and seeds.
- **What comes out:** sixteen items and 1,000 mB of water digest over 400 ticks (20 seconds) into two products:
  - **1,000 mB of raw biogas**, at the header flange high on the right;
  - **two bone meal** as digestate, at the chute low on the right.
- **Upgrading biogas:** the Chemical Infuser, with its Nickel Catalyst Bed, turns 250 mB of biogas and 400 mB of hydrogen into **250 mB of methane** and 400 mB of water. That methane runs the Gas Burning Generator.
- **Looks:**
  - Lit while it works: the amber sight strip, the green gas-status lamp and the control panel's lamps.
  - The mixer coupling on the back of its gearbox turns slowly.
- **Guides:** its Engineer's Handbook page describes it, and JEI lists its recipe.

## Connections

- **Existing input producers:** farms, mob drops (rotten flesh), the sea (kelp), water.
- **Existing output consumers:**
  - biogas, through the Infuser's upgrading, into methane for the Gas Burning Generator;
  - bone meal for farming.
  - The Bio Generator, on its own branch, is the plan's direct consumer of cleaned biogas. It can add biogas as a fuel once both are merged.
- **Technology connection:**
  - the shared form foundation's bulk four-lane profile, first used here by a real machine;
  - the `jugcraft:anaerobic_digestion` capability;
  - the Infuser's nickel-bed capability, which gains its second recipe.
- **Magic connection:** none.
- **Reachable entry path:** Steel Tanks, motors, a plate, a basic circuit and a casing all come before it. Nothing it makes is needed to build it. It is optional: first methane already comes from coal-fired carbon dioxide or the Fermenter.
- **How the specialty stays useful alone:** a farm and mob base can turn its surplus into fuel and fertilizer.

## Balance and automation

- **Biogas.** 1,000 mB for 16 items is 62.5 mB of gas per item. Biogas is the plan's 60/40 mixture, so the upgrading recipe gets exactly 150 mB of methane and 100 mB of carbon dioxide from 250 mB of biogas.
- **Upgrading.** It matches methanation: the 100 mB of carbon dioxide takes 400 mB of hydrogen and gives 100 mB more methane and 400 mB of water. The batch takes 200 ticks at 128 JE/t, methanation's 25,600 JE. The biogas's own 150 mB of methane passes through, accounted once.
- **Digestate.** Two bone meal per 16 items is about what a composter gives for as many items. It is not a cheaper fertilizer route.
- **Energy per batch:**
  - 30,800 JE in the bulk profile;
  - 38,400 JE at the family's full 96 JE/t;
  - with efficiency cards, at most 59% less.
- **The power-loop audit, refined.** `tools/check_mod_data.py`'s `cheapest_electrolysis` now costs only routes that **electricity alone** can run.
  - A recipe that uses up items (coal, crops, waste) draws energy from them, so it is not an electricity loop. Neither is any route through a fluid that only such recipes make.
  - Without this, methane upgraded from waste-made biogas would have looked like a loop. Waste, like coal or crops, is a primary fuel.
  - Hydrogen still costs 168 JE per mB, from water electrolysis.
  - Methane now has no electricity-only route, because all of its carbon comes from coal, crops or waste. So the generator check no longer prices it. The figures in [methane power](industrial-methane-power.md) describe the earlier, stricter audit.
  - This same rule covers the Bio Generator branch's primary-fuel exemption for bioethanol.
- **Automation.**
  - Conveyors reach the auger and the chute, and pipes reach the inlet and the header.
  - The power socket is on the junction box at the back.
  - Four lanes share one 16,000 mB water tank and one biogas tank. A full biogas tank stops it, and its screen says so.

## Choices made here for review

- **The waste list** (`#jugcraft:digestible`): the fermentable crops, rotten flesh, poisonous potatoes, kelp, pumpkins and four kinds of seeds.
- **The rates:** 16 items + 1,000 mB of water → 1,000 mB of biogas + 2 bone meal, 400 ticks.
- **Digestate as bone meal**, rather than a new item.
- **The bill:** four Steel Tanks, two electric motors, a steel plate, a basic circuit and a Machine Casing (`TPT / MCM / TKT`).
- **The bulk profile** (4 lanes, 16,000 mB, 80% energy).
- **The audit refinement** described above.
- **Not built:** biogas cleanup, recovered water and larger digesters.

## Multiplayer and persistence

- The server decides everything. The ports check what goes in, and nothing can be put in at the gas header.
- New registrations, stable from now on:
  - the block and item `jugcraft:anaerobic_digester`;
  - the gas `jugcraft:biogas`;
  - the item tag `jugcraft:digestible`;
  - the capability `jugcraft:anaerobic_digestion`;
  - the recipes `jugcraft:chemical_reaction/digester_biogas` and `jugcraft:gas_synthesis/infuser_biogas_upgrading`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The model is authored in `tools/industrial_forms.py` from shared Jugcraft textures:
- the owner's white tank panels (`tk_body`, `tk_top`);
- gunmetal compression rings and shoes, olive auger and motor, hazard edges;
- portholes, the amber strip and the green lamps;
- the biogas still on the header band;
- the turning mixer coupling (`anaerobic_digester_coupling` in `machine_rotor_quads.json`).

The biogas texture is drawn from its colours by `tools/petro_textures.py`, like every gas. It is listed last among the gases, so no other gas's texture changes.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`: regenerated the 216 part models, the item model, the rotor, both recipes, the tag, the gas's data and the handbook page.
- `python tools/check_mod_data.py` (with the art check and the refined audit) and `python scripts/check_repository.py`: passed.
- An offline render of the part models was inspected from six directions.
- `javac` reported no syntax errors in the changed Java files. It could not compile them without Minecraft's libraries.

For CI, server game tests:
- `AnaerobicDigesterGameTests.itStandsWholeAndOnlyItsPortsAnswer`: 216 blocks in every orientation, and only the five declared ports answer.
  - The auger takes rotten flesh, not cobblestone.
  - The inlet takes water, not hydrogen.
  - The header takes nothing.
- `AnaerobicDigesterGameTests.wasteDigestsIntoBiogasAndBoneMeal`: 16 rotten flesh and 1,000 mB of water give 1,000 mB of biogas at the header and two bone meal at the chute, for exactly 30,800 JE.
- `AnaerobicDigesterGameTests.fourBatchesRunSideBySide`: 64 wheat and 4,000 mB of water start all four lanes.
- `AnaerobicDigesterGameTests.everyPartLightsWhileItWorks`.
- `AnaerobicDigesterGameTests.itRunsItsOwnRecipeAndIsBuiltFromItsBill`: its one recipe; the Chemical Reactor makes no biogas; the bill crafts it.
- `ChemicalInfuserGameTests.theNickelBedUpgradesBiogasIntoMethane`: 250 mB of biogas and 400 mB of hydrogen give 250 mB of methane and 400 mB of water, for 25,600 JE.
- `ChemicalInfuserGameTests.itRunsItsOwnRecipeAndIsBuiltFromTheStarterBill`: now three recipes.

Client game test: `AnaerobicDigesterClientGameTests` takes four views (front left, front, back right, night), plus its screen while working and its screen when stopped by a full biogas tank.

### Results

- **The first run, [38425068211](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38425068211), failed only in `AnaerobicDigesterClientGameTests`.** Its full-tank check found the Digester still processing: the test's top-up kept a full stack of waste in the slot, so cancelling had no room to return a batch's waste. The build, the data checks and every server game test passed.
- The test now empties the slot and the water tank before cancelling (commit 57e3a77f).
- **Run [38432374427](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38432374427) (commit 57e3a77f): green.** Everything compiled; every server game test passed, including the Digester's tests and the Infuser's biogas upgrading; the build without optional integrations and all five client shards passed.
- The client screenshots have not yet been looked at by a person.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The waste list, rates, digestate, bill and audit refinement await the owner's review.
- Still to come: direct cleaned-biogas burning in the Bio Generator, gas cleanup and larger digesters.
