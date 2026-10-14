# Coal gasification: the Coal Gasifier, gas cleanup and CO methanation (factory package 2)

Status: implemented (CI green on the contributor's fork; pending review).
Stacked on the [Anaerobic Digester](industrial-anaerobic-digester.md) and the branches below it, down to the [Chemical Infuser](industrial-chemical-infuser.md).
Proposal issue: none. TODO items:
- "the Coal Gasifier with Gas Cleanup and Separation" (package 2);
- "Add the earlier steel-era coal gasifier's CO/hydrogen mixture, cleanup and separation into useful gases";
- the CO + 3 H2 half of "Define distinct CO2 + 4 H2 and CO + 3 H2 methanation recipes".

**From the owner's plan:**
- both machines' bills ([starter gas and acid factory](industrial-starter-gas-and-acid-factory.md));
- their sizes and models ([art briefs](industrial-machine-models-and-textures.md#coal-gasifier));
- coal/coke first, with a lower-yield charcoal option;
- paid electrical heat, and no oxygen before first CO/H2;
- a combined cleanup and separation form.

**This change's choices:** the carbon, water and gas quantities, times and families. The plan left these pending its carbon ledger, and the contributor asked for reasonable numbers. They are listed below for the owner to review.

**Not built:** the plan's numeric pollution, because the mod has no pollution system yet.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the coal line of the starter gas and acid factory
Primary specialty and supported player role: fuel and chemical processing

## Player experience

- **Coal Gasifier** (4×4×6): a tall lined reaction column over a broad ash chamber.
  - Drop coal or coal coke into the charging chute on its left side, and pipe water into the inlet below.
  - Each piece and 500 mB of water (fed as steam) become **500 mB of raw syngas**, half carbon monoxide and half hydrogen, in 200 ticks. It leaves at the header flange high on the right.
  - Charcoal, the lower-yield option, gives 400 mB from 400 mB of water.
  - It runs two batches at once: the expanded profile, 87 JE/t a batch.
  - While it works, its fire door glows and its feed drive turns.
- **Gas Cleanup and Separation Rack** (3×5×4): a scrubber column and two towers on a skid.
  - Pipe raw syngas into the inlet low on its left.
  - 500 mB is cleaned and divided into **250 mB of carbon monoxide** and **250 mB of hydrogen** in 100 ticks at 128 JE/t. Each gas leaves at the top of its own tower at the back.
  - While it works, its blower fan turns.
- **CO methanation:** the Chemical Infuser with its Nickel Catalyst Bed turns 100 mB of carbon monoxide and 300 mB of hydrogen into **100 mB of methane** and 200 mB of water, in 150 ticks.
- **Guides:** the Engineer's Handbook has a page for each machine, and JEI lists their recipes.

## Connections

- **Existing input producers:**
  - coal (mined);
  - coke (the Coke Oven);
  - charcoal (logs);
  - water.
- **Existing output consumers:**
  - hydrogen in the Gas Burning Generator, the Fuel Cell, the Infuser's recipes and every existing hydrogen consumer;
  - carbon monoxide in the Infuser's CO methanation;
  - methane in the Gas Burning Generator.
- **Technology connection:**
  - two new forms, of the chemical reactor and synthesis families;
  - the capabilities `jugcraft:coal_gasification` and `jugcraft:gas_separation`;
  - the Infuser's nickel bed gains a third recipe.
- **Magic connection:** none.
- **Reachable entry path:** both bills come before the Separator's tier (steel, bricks, a furnace, tanks, a motor, a basic circuit). Nothing they make is needed to build them. Hydrogen from coal is a new route that needs no electrolysis.
- **Required vs optional:** optional. Electrolysis still supplies hydrogen, and coal-fired carbon dioxide, the Fermenter or the Digester still supply methanation's carbon.

## Balance and automation

- **Carbon ledger.**
  - A coal or coke counts as 250 reaction amounts of carbon, the same convention the Oxidizer's coal-to-carbon-dioxide recipe uses. Steam gasification C + H2O → CO + H2 gives 250 mB of each gas.
  - The rack recovers each gas once, never more: 250 + 250 mB from 500.
  - Charcoal counts as 200.
- **Water.** The steam is fed in excess: 500 mB for 250 reaction amounts. The surplus leaves as vapour. This keeps the data check's rule that a recipe makes no more fluid than it takes in.
- **Energy:**
  - Gasification takes 200 ticks: 19,200 JE at the family's 96 JE/t, or 17,400 JE in the expanded profile. This is the paid heat.
  - Cleanup takes 12,800 JE.
  - CO methanation takes 19,200 JE: CO + 3 H2 → CH4 + H2O, with the water at the plan's 2 mB per reaction amount.
- **Coal's own fuel value.** A coal burns for its furnace value or gasifies, never both. Hydrogen and methane from coal draw their energy from the coal, so the refined power-loop audit treats them as primary-fuel routes, not electricity loops (see the [Digester](industrial-anaerobic-digester.md#balance-and-automation)).
- **Automation.** Conveyors reach the chute, and pipes reach every fluid port. The Gasifier's power socket is at its back, and the rack's is on the junction box at its back.

## Choices made here for review

- **Quantities:** 250 reaction amounts of carbon per coal or coke, and 200 per charcoal.
- **Steam:** water fed in excess, 2:1.
- **Times:** 200, 100 and 150 ticks.
- **Families and profiles:**
  - the Gasifier in the chemical reactor family, expanded profile;
  - the rack in the synthesis family, entry profile.
- **Not built:**
  - numeric pollution, which has no system yet;
  - the plan's "one initial cleaning residue" and its road/filler use (since added in [gasification residue](industrial-gasification-residue.md));
  - optional oxygen assistance.

## Multiplayer and persistence

- The server decides everything. The ports check what goes in, and nothing can be put in at the outlets.
- New registrations, stable from now on:
  - the blocks and items `jugcraft:coal_gasifier` and `jugcraft:gas_cleanup_rack`;
  - the gases `jugcraft:raw_syngas` and `jugcraft:carbon_monoxide`;
  - the capabilities `jugcraft:coal_gasification` and `jugcraft:gas_separation`;
  - the recipes `chemical_reaction/gasifier_{coal,coke,charcoal}`, `gas_synthesis/rack_raw_syngas` and `gas_synthesis/infuser_carbon_monoxide_methanation`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. Both models are authored in `tools/industrial_forms.py` from shared Jugcraft textures:
- blackened gunmetal, cream refractory borders, copper pipe rings and olive guards;
- the fire door that glows while lit, soot seams, steel-plate towers and cream lining bands;
- the gases' stills on the pipe and tower bands.

The turning parts are the Gasifier's feed-drive pulley and the rack's blower fan, both in `machine_rotor_quads.json`. The two gas textures are drawn from their colours like every gas, listed last so no other texture changes.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`: regenerated both forms' parts, items, rotors, recipes and handbook pages, and the gases.
- `python tools/check_mod_data.py` and `python scripts/check_repository.py`: passed. The first gasifier figures, 250 mB of water to 500 mB of gas, failed the "no fluid from nothing" rule and were corrected to the excess-steam figures above.
- Offline renders of both forms were inspected from six directions.
- `javac` reported no syntax errors in the changed Java files. It could not compile them without Minecraft's libraries.

For CI, server game tests:
- `CoalGasificationGameTests.theGasifierStandsWholeAndOnlyItsPortsAnswer` and `theRackStandsWholeAndOnlyItsPortsAnswer`: every orientation, ports only.
- `coalGasifiesIntoRawSyngas`: a coal and 500 mB of water give 500 mB of raw syngas at the header, for exactly 17,400 JE.
- `charcoalGivesLessSyngas`: 400 mB.
- `everyGasifierPartLightsWhileItWorks`.
- `theGasifierRunsItsOwnRecipesAndIsBuiltFromItsBill`: its three recipes and the plan's bill.
- `theRackDividesSyngasIntoCarbonMonoxideAndHydrogen`: 250 mB of each gas at its tower, for exactly 12,800 JE.
- `theRackRunsItsOwnRecipeAndIsBuiltFromItsBill`: its one recipe and the plan's bill; the Chemical Reactor makes no raw syngas.
- `ChemicalInfuserGameTests.theNickelBedTurnsCarbonMonoxideIntoMethane`: 100 mB of methane and 200 mB of water, for 19,200 JE.
- `ChemicalInfuserGameTests.itRunsItsOwnRecipeAndIsBuiltFromTheStarterBill`: now four recipes.

Client game tests: `CoalGasifierClientGameTests` and `GasCleanupRackClientGameTests` each take four views and two screens.

### Results

- **Run [38432379438](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38432379438) on the contributor's fork (commit 709bde6e): green.** Everything compiled; every server game test passed, including the seven `CoalGasificationGameTests` and the Infuser's CO methanation; the build without optional integrations and all five client shards passed, both new client test classes among them.
- An earlier run on 6ea5a039 was cancelled before it finished, because it lacked the Digester's client-test fix.
- The client screenshots have not yet been looked at by a person.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The quantities, times and families await the owner's review, along with the carbon ledger.
- Still to come: numeric pollution and oxygen assistance. The residue and its filler use are in [gasification residue](industrial-gasification-residue.md).
