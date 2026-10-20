# Industrial Fermenter: bioethanol with collected carbon dioxide (factory package 2)

Status: implemented (CI green on the contributor's fork; pending review). Stacked on [methane power](industrial-methane-power.md), which is stacked on the [Chemical Infuser](industrial-chemical-infuser.md) and the [Chemical Oxidizer](industrial-chemical-oxidizer.md).
Proposal issue: none. The TODO list's "Develop the earlier milling/mashing -> fermentation -> distillation Bio-Generator fuel route, adding … brewery CO2 collection" (the fermentation and CO2 collection step) and "Full CO2 capture storage defaults to continued production/excess venting". The size, model and venting follow the art brief's Industrial Fermenter ([models and textures](industrial-machine-models-and-textures.md#industrial-fermenter)); the bill and the carbon dioxide amount are this change's choices, described below for review.
Owner: jimbozoomer-byte
Target milestone and tier: steel era, the bio line of the starter gas and acid factory
Primary specialty and supported player role: farming into fuel, chemical processing

## Player experience

The **Industrial Fermenter** is a 3×3×4 multi-block: a tall stepped steel vessel on four legs.

- Crops go in at the **feed hatch** on the crown, and water at the **water inlet** low at the back left.
- Eight crops from `#jugcraft:fermentable` (wheat, sugar cane, potato, carrot, beetroot, sweet berries, melon slices or apples) and 1,000 mB of water become two outputs:
  - **375 mB of bioethanol**, at the **broth outlet** low at the front: half again the Chemical Reactor's 250 (raised in [factory payoffs](industrial-factory-payoffs.md));
  - **250 mB of carbon dioxide**, at the **upper flange** at the back.
- A batch takes 100 ticks and 9,600 JE (96 JE/t). That is the Chemical Reactor's own fermentation, which stays unchanged, plus the gas the reactor cannot keep.

**Venting.** The carbon dioxide tank vents what it cannot hold, so the Fermenter carries on whether or not anything collects the gas. This is the plan's selected excess-vent default. A full alcohol tank does stop it, and the screen says so, like any machine.

**Looks.**
- The front: the level strip and the green batch lamp light, and the instrument box is the controller.
- The top: the agitator drive turns slowly while it works.

**Where to find it.** Its Engineer's Handbook page describes it, and JEI lists its recipe.

## Connections

- **Existing input producers:** farm crops; water from any pump or tank.
- **Existing output consumers:**
  - bioethanol for the Bio Generator (on its own branch), premium gasoline hydrotreating and the reactor's medical recipes;
  - carbon dioxide for the Chemical Infuser's methanation over the nickel bed. This gives methanation a farm-grown carbon source beside coal burnt in the Oxidizer.
- **Technology connection:**
  - the shared form foundation;
  - the new per-tank vent option (`MachineForm.Builder.vent`), which any later form can use for an optional byproduct;
  - the chemical reactor family's energy figures;
  - the `jugcraft:controlled_fermentation` capability.
- **Magic connection:** none.
- **Reachable entry path:** crops, water, steel plates, glass, a Steel Tank, a motor, a basic circuit and a Machine Casing all come before it. Nothing it makes is needed to build it.
- **Required vs optional:** the whole machine is optional. The Chemical Reactor still ferments, and the Oxidizer still makes carbon dioxide.
- **How the specialty stays useful alone:** a farm can supply both bioethanol and a methanation feed without burning coal.

## Balance and automation

- **Bioethanol.** The Fermenter takes the reactor's inputs, time and energy (8 crops + 1,000 mB of water, 100 ticks, 9,600 JE) but gives 375 mB instead of 250, so the dedicated machine is worth building (raised in [factory payoffs](industrial-factory-payoffs.md)). Its gain is the carbon dioxide, which the reactor discards.
- **Carbon dioxide.** It makes 250 mB per batch, the same amount of gas as alcohol. This is the plan's rough 1:1 molar ratio of ethanol to CO2, at the game's gas scale.
- **Power-loop checks.** Carbon dioxide made this way is cheaper than the Oxidizer's (which needs electrolysed oxygen), so methane gets cheaper to make. `tools/check_mod_data.py`'s loop audit still passes:
  - It costs every recipe a fluid comes from, including this one.
  - The Gas Burning Generator's 448 JE per mB of methane stays below what its hydrogen alone costs: 400 mB at 168 JE per mB.
  - The crops count as free, like every primary fuel's items.
- **Venting.** It destroys only the excess gas, never items or alcohol, and is counted once. Collection is always optional.
- **Automation.** Pipes reach the three fluid ports and conveyors the hatch. The power socket is at the back right.

## Choices made here for review

- **The bill.** The plan's bill is 8 steel plates, a Steel Tank, a motor, 2 glass, a basic circuit and a casing: 14 items, more than a crafting grid holds.
  - This change uses 4 steel plates, 1 glass, a Steel Tank, an electric motor, a basic circuit and a Machine Casing (`PGP / PTP / MCK`).
  - The Steel Tank and the casing already carry the plan's other 8 plates.
- **250 mB of carbon dioxide per batch**, as above.
- **The vent mechanism.** Excess venting is a property of the form's tank (`vent("co2")`), not of the recipe. The plan's "optional stop-instead" setting is now built separately ([vent or hold](industrial-vent-or-hold.md)).
- **Not built yet:** the separate milling/mashing and distillation steps. The larger 5×5×6 variant is now the [Bulk Fermenter](industrial-bulk-fermenter.md).

## Multiplayer and persistence

- The server decides everything. The ports check what goes in, and nothing can be put in at an outlet.
- New registrations, stable from now on:
  - the block and item `jugcraft:industrial_fermenter`;
  - the capability `jugcraft:controlled_fermentation`;
  - the recipe `jugcraft:chemical_reaction/fermenter_bioethanol`.
- The vent option adds no saved data. Saved forms without it behave exactly as before.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The model is authored in `tools/industrial_forms.py` from the shared Jugcraft textures:
- dull steel plate, the cream ceramic jacket, gunmetal bands, copper risers, the amber level strip and the green lamp;
- the bioethanol and carbon dioxide stills on the pipe bands;
- the pulley texture on the turning agitator drive (`industrial_fermenter_drive` in `machine_rotor_quads.json`).

Generated blocks wholly inside the vessel keep no faces. The writer now drops such elements, because Minecraft refuses a faceless element.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`: regenerated the 36 part models, the item model, the rotor, the recipe, the handbook page and the names.
- `python tools/check_mod_data.py` (with the art check), `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the part models was inspected from six directions.

For CI, server game tests in `IndustrialFermenterGameTests`:
- `itStandsWholeAndOnlyItsPortsAnswer`: 36 blocks in every orientation. Only the five declared ports answer. The hatch takes wheat, not cobblestone; the inlet takes water, not hydrogen; neither outlet takes anything.
- `wheatFermentsIntoBioethanolAndCarbonDioxide`: 8 wheat and 1,000 mB of water give 375 mB of bioethanol and 250 mB of carbon dioxide at their outlets, for exactly 9,600 JE.
- `aFullGasTankVentsButAFullAlcoholTankWaits`: with the gas tank full, a batch still runs and the tank stays exactly full. A full alcohol tank stops it with "output full", and it carries on once drained.
- `everyPartLightsWhileItWorks`: lit while working, dark when paused; only the controller gives off light.
- `itRunsItsOwnRecipeAndIsBuiltFromItsBill`: its one recipe; the Chemical Reactor makes no carbon dioxide; the bill crafts it.

Client game test: `IndustrialFermenterClientGameTests` takes four views (front left, front, back right, night), its screen while working, and its screen when stopped by a full alcohol tank.

### Results

- **Run [38338780045](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38338780045) on the contributor's fork (commit 6dc83b1a): green.** It compiled; the data checks passed; every server game test passed, the five `IndustrialFermenterGameTests` among them; the build without optional integrations passed; and all five client shards passed, `IndustrialFermenterClientGameTests` included.
- The first run (38335877059) failed to compile: the tests read bioethanol with `fluid()` instead of the liquid's `source()`. The fix is commit 6dc83b1a.
- The client screenshots have not yet been looked at by a person.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The bill and the carbon dioxide amount await the owner's review.
- Still to come: milling/mashing, distillation and dehydration. The 5×5×6 variant is the [Bulk Fermenter](industrial-bulk-fermenter.md).
