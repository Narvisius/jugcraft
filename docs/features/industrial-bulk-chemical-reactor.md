# Bulk Chemical Reactor: hydrochloric acid and chloride leaching (factory package 2)

Status: implemented (CI green on the contributor's fork; pending review).
Stacked on the [Industrial Fermenter](industrial-fermenter.md), which is stacked on [methane power](industrial-methane-power.md), the [Chemical Oxidizer](industrial-chemical-oxidizer.md) and the [Chemical Infuser](industrial-chemical-infuser.md). The Infuser makes the hydrogen chloride this machine uses.
Proposal issue: none. It takes up two TODO items:
- "hydrogen chloride absorption into aqueous acid, and its consumers" (package 2);
- the art brief's [Bulk Chemical Reactor](industrial-machine-models-and-textures.md#bulk-chemical-reactor): 4×4×4, "shared acid absorption … use installed recipe capabilities".

The absorption amounts, time and energy are the plan's [aqueous HCl preparation](industrial-starter-gas-and-acid-factory.md) row. The bill, the operating profile and the acid's use were not specified anywhere. They are this change's choices, made at the contributor's request ("just pick reasonable numbers") and listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the acid line of the starter gas and acid factory
Primary specialty and supported player role: chemical processing and ore refining

## Player experience

The **Bulk Chemical Reactor** is a 4×4×4 multi-block: the Chemical Reactor's jacketed vessel, enlarged.

- It runs **two batches at once** from 8,000 mB tanks, at 90% of the reactor's draw: 87 JE/t a batch instead of 96. This is the foundation's expanded profile.
- **Hydrochloric acid:**
  - Pipe water into the inlet on its left side, and hydrogen chloride from the Chemical Infuser into the short absorption inlet on its lid.
  - 500 mB of the gas dissolves in 1,000 mB of water into 1,000 mB of **hydrochloric acid**. A batch takes 100 ticks.
  - The acid leaves at the outlet low at the front.
- **Chloride leaching:**
  - Drop an ore into the hatch on its lid and give it 250 mB of hydrochloric acid at the side inlet.
  - It gives **four washed ores** in 80 ticks, from the chute at the front. That is the same yield as sulfuric-acid leaching in the Chemical Reactor, at four times its rate with both lanes (raised in [factory payoffs](industrial-factory-payoffs.md)).
- Hydrochloric acid is a new liquid with a bucket. Like every liquid, it can be piped, carried and stored in tanks.
- **Looks:**
  - Lit while it works: the amber activity strip under the access-plate window, and the control panel's amber and green lamps.
  - The stirring drive on the lid turns.
- **Guides:** its Engineer's Handbook page describes it, and JEI lists its recipes.

## Connections

- **Existing input producers:**
  - hydrogen chloride from the Chemical Infuser (hydrogen and chlorine from the Electrolytic Separator);
  - water;
  - ores.
- **Existing output consumers:** washed ores go to the furnace. Hydrochloric acid feeds this machine's own leaching. Later plan routes can use it too (titanium chloride, magnesium chloride, starch hydrolysis, PVC).
- **Technology connection:**
  - the shared form foundation, including its expanded two-lane profile, first used here by a real machine;
  - the chemical reactor family;
  - two new capabilities, `jugcraft:acid_absorption` and `jugcraft:chloride_leaching`.
- **Magic connection:** none.
- **Reachable entry path:** everything needed comes before the Infuser and the Chemical Reactor, which is part of the bill. Nothing it makes is needed to build it.
- **Required vs optional:** optional. Sulfuric-acid leaching in the Chemical Reactor is unchanged.
- **How the specialty stays useful alone:** a salt-and-electricity base without sulfur can still reach the best ore yield.

## Balance and automation

- **Absorption.** It uses exactly the plan's row: 500 mB of gas + 1,000 mB of water → 1,000 mB of acid, 100 ticks at the family's 96 JE/t, or 9,600 JE. The expanded profile's 90% makes that 8,700 JE a batch. The chlorine and hydrogen are accounted once: the acid volume holds the gas's solute and the water.
- **Leaching.** It matches sulfuric-acid leaching's yield, 1 ore + 250 mB of acid → 4 washed ores, in half the time (80 ticks). The 4× ore ceiling is unchanged, so there is no gain loop; the big machine wins on speed.
- **The acid is not interchangeable with gas.** Nothing turns the acid back into hydrogen chloride, and the Chemical Reactor neither makes nor uses it.
- **Automation.** Pipes reach the three fluid ports, and conveyors reach the hatch and the chute. The power socket is on the junction box at the back right.

## Choices made here for review

- **The bill:** a Chemical Reactor, a Steel Tank, a steel fluid pipe, an electric motor, a basic circuit and 4 steel plates (`PIP / MRC / PTP`).
- **The expanded profile** (2 lanes, 8,000 mB, 90% energy) for a 4×4×4 "bulk" machine.
- **Hydrochloric acid's first use** is ore leaching at sulfuric acid's yield.
- **The acid's colour:** a faint greenish yellow.

## Multiplayer and persistence

- The server decides everything. The ports check what goes in, and nothing can be put in at the outlet.
- New registrations, stable from now on:
  - the block and item `jugcraft:bulk_chemical_reactor`;
  - the liquid `jugcraft:hydrochloric_acid`, with its bucket and fluid block;
  - the capabilities `jugcraft:acid_absorption` and `jugcraft:chloride_leaching`;
  - the recipes `jugcraft:chemical_reaction/bulk_hydrochloric_acid` and `jugcraft:chemical_reaction/bulk_chloride_<ore>`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The model is authored in `tools/industrial_forms.py` from shared Jugcraft textures:
- steel plate, olive bands, the cream ceramic lining edge, gunmetal and hazard stripes;
- the porthole window, the gauge, the amber and green lamps;
- the hydrochloric acid and hydrogen chloride stills on the pipe bands;
- the pulley on the turning stirring drive (`bulk_chemical_reactor_drive` in `machine_rotor_quads.json`).

The acid's still, flow and bucket textures are drawn from its colours by `tools/petro_textures.py`, like every liquid. It is listed last among the liquids, so no other liquid's texture changes.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`: regenerated the 64 part models, the item model, the rotor, the recipes, the fluid's data and the handbook page.
- `python tools/check_mod_data.py` (with the art check), `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the part models was inspected from six directions.
- `javac` reported no syntax errors in the changed Java files. It could not compile them without Minecraft's libraries.

For CI, server game tests in `BulkChemicalReactorGameTests`:
- `itStandsWholeAndOnlyItsPortsAnswer`: 64 blocks in every orientation, and only the six declared ports answer.
  - The hatch takes iron ore, not cobblestone.
  - The side inlet takes water, not hydrogen.
  - The lid inlet takes hydrogen chloride, not water.
  - The outlet takes nothing.
- `hydrogenChlorideDissolvesIntoHydrochloricAcid`: 1,000 mB of water and 500 mB of gas give 1,000 mB of acid at the outlet, for exactly 8,700 JE.
- `twoBatchesRunSideBySide`: both lanes start together and make 2,000 mB, for twice the energy.
- `hydrochloricAcidLeachesOre`: an iron ore and 250 mB of acid give four washed iron ores at the chute, done within 100 ticks.
- `everyPartLightsWhileItWorks`: lit while working and dark when paused; only the controller gives off light.
- `itRunsItsOwnRecipesAndIsBuiltFromItsBill`:
  - its absorption and leaching recipes run;
  - the Chemical Reactor neither makes nor uses the acid;
  - the bill crafts it.

Client game test: `BulkChemicalReactorClientGameTests` takes four views (front left, front, back right, night), plus its screen while working and its screen when stopped by a full acid tank.

### Results

- **Run [38423386376](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38423386376) on the contributor's fork (commit d2d426b8): green.** It compiled; the data checks passed; every server game test passed, the six `BulkChemicalReactorGameTests` among them; the build without optional integrations passed; and all five client shards passed, `BulkChemicalReactorClientGameTests` included.
- The client screenshots have not yet been looked at by a person.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The bill, the profile and the acid's use await the owner's review.
- **Sulfuric acid.** The plan's SO3 absorption (with its retained carrier acid) can join this machine as another capability once the vanadium contact bed is specified.
