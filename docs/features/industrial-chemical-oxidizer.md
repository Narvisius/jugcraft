# Chemical Oxidizer (factory package 2, sulfur oxidation)

Status: implemented (CI green on the contributor's fork; pending review). Built on the [shared machine foundation](industrial-machine-foundation.md), beside the [Electrolytic Separator](industrial-electrolytic-separator.md) and [Gas Burning Generator](industrial-gas-burning-generator.md).
Proposal issue: none. An unchecked item of package 2, "Starter steel chemistry", in the owner's [factory implementation plan (PR #302)](https://github.com/jimbozoomer-byte/jugcraft/pull/302) and the [TODO list](../TODO.md#industrial-machine-models-textures-and-dimensions). Its construction bill, the sulfur oxidation quantities and the 9,600 JE batch budget are the proposals in the [starter gas and acid factory](industrial-starter-gas-and-acid-factory.md); its look is the [art brief](industrial-machine-models-and-textures.md#chemical-oxidizer).
Owner: jimbozoomer-byte
Target milestone and tier: steel-era chemistry, the first step of the starter factory's contact acid chain
Primary specialty and supported player role: chemical processing and factory operations

## Player experience

The **Chemical Oxidizer** burns a solid feed in a metered gas. It is two wide, two deep and three tall, every position a block. This slice gives it sulfur oxidation.

**Building and placing.** It is crafted from 4 steel plates, a furnace, an electric motor, glass, a basic circuit and a Machine Casing. Placed from the front, it fills twelve blocks with the control panel at the front left. If any of them is taken, nothing is placed and the player is told what is in the way and where. Breaking any block removes the whole machine and drops it once.

**What it looks like** (the art brief):
- a squat dark-steel reaction throat on a plinth, a cream refractory ring round its circular bolted hatch, the orange reaction slit above it and the deep residue drawer below;
- a short gas column with a copper band and a ringed collector hood on top, its outlet rising to a collar banded in sulfur dioxide;
- at the back, a broad olive hopper with a hazard-striped feed lip on two legs, a chute and a guarded trough where the copper feed screw carries the dust into the throat;
- the capped oxidant inlet low on the right, banded in oxygen, with a copper valve;
- the junction box with the power socket low on the left at the back;
- the control panel with a gauge and the amber running lamp at the front left.

While it works the lamp and the reaction slit light on every block and the feed screw turns; only the control panel gives off light.

**Ports.** Conveyors, pipes and cables meet it only at the middle of these faces:

| Port | Where (seen from the front) | What |
| --- | --- | --- |
| Feed in | top of the hopper, back left | sulfur dust; nothing else goes in |
| Oxidant in | right side of the front bottom block | oxygen; nothing else goes in |
| Product out | top of the front right block | the product gas tank |
| Power | left side of the back bottom block | 96 JE/t while it works, up to 512 JE/t in |

**Working.**

| Feed | Gives | Time and energy |
| --- | --- | --- |
| 1 sulfur dust + 250 mB oxygen | 250 mB sulfur dioxide | 5 s (100 ticks), 9,600 JE |

Its screen is the shared formed-machine screen. It names what it waits for (oxygen in the "Oxidant tank") or what blocks it ("Product gas tank is full"), reads "Warming up" for the first two seconds after a cold start, and takes speed and efficiency cards. JEI shows its recipe in its own category. The Engineer's Handbook has a page for it after the Chemical Reactor's.

## Connections

- Existing input producer: sulfur dust (mined sulfur and the Chemical Reactor's Claus recovery from hydrogen sulfide) and oxygen from the Electrolytic Separator, the Electrolytic Cell or the Air Separation Unit.
- Existing output consumer: none yet. Sulfur dioxide is the first step of the selected contact acid chain: SO2 + O2 -> SO3 in the Chemical Infuser family over its vanadium contact bed, then absorption into ordinary sulfuric acid. The existing simple sulfuric acid recipe stays as it is meanwhile, as the plan requires. Sulfur dioxide can be stored in gas tanks and cylinders.
- Technology connection: the chemical reactor family (`MachineKind.CHEMICAL_REACTOR`), whose energy figures it shares. It runs only recipes asking for its capability, `jugcraft:solid_oxidation`; the Chemical Reactor keeps its own recipes.
- Magic connection: none.
- Reachable entry path: steel plates, a furnace, an electric motor, glass, a basic circuit and a Machine Casing all come before it; oxygen comes from the Separator or the Air Separation Unit; no circular unlock.
- Which connections are required vs optional: power, sulfur and oxygen are required.
- How the specialty stays useful alone: it is the starter factory's sulfur burner; its other compatible solid/gas recipes can follow.

## Balance and automation

- **Quantities** are the plan's proposal: a dust counts as 250 reaction amounts of sulfur, so 250 mB of oxygen makes 250 mB of sulfur dioxide. No fluid is made from nothing.
- **Energy**: the plan's 100 ticks at 96 JE/t, 9,600 JE, which is exactly the reactor family's draw. Four efficiency cards bring a batch to 3,900 JE.
- **No gain loop**: sulfur dioxide burns in no generator.
- **Automation**: the product pushes out of its collar every 4 ticks, at most 1,000 mB at a time; the hopper takes sulfur from conveyors and item pipes; an idle Oxidizer does nothing.
- **Residue**: none for pure sulfur. The residue drawer in the model is for later feeds that leave one; this form does not promise every dust becomes useful gas.

## Multiplayer and persistence

- The server decides placement, recipes, ports and the screen's buttons; the client draws the synced state and turns the screw.
- Saving uses the foundation's role-named tanks (`oxidant`, `product`), its one input slot, its batch escrow and its form version (1).
- New registrations: the block and item `jugcraft:chemical_oxidizer`, and the gas `jugcraft:sulfur_dioxide`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The model is built from the mod's own textures: the dieselpunk olive, stencil, gunmetal, chrome, rubber, hazard, gauge and lamp; the cream ceramic panel; copper; the flange plate; the hopper mouth; the amber strip; the power socket; and the oxygen and sulfur dioxide stills for the bands. `tools/industrial_forms.py` authors it and slices it into 12 part models; the feed screw is quads in `machine_rotor_quads.json`. The sulfur dioxide texture is drawn by `tools/petro_textures.py` from its colours, like every gas.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`: regenerated the Oxidizer's blockstate, 24 part models, item model, loot table, crafting recipe, sulfur dioxide recipe and fluid tag, the feed screw quads, the lang strings, its handbook page and its JEI category.
- `python tools/check_mod_data.py`: passed, including the form check and the art check of every model and turning part.
- `python scripts/check_repository.py`: passed.
- `javac` reported no syntax errors in the changed Java files; it could not compile them without Minecraft's libraries.
- The sliced part models were drawn together outside the game from six sides with a small z-buffer script, to check the slices line up and every side is finished.

For CI:
- **Server game tests** (`ChemicalOxidizerGameTests`), 5 tests:
  - in all four orientations it fills its twelve blocks and only its four ports answer; the hopper takes only sulfur dust and the inlet only oxygen; nothing goes in at the outlet;
  - a dust and 250 mB of oxygen give 250 mB of sulfur dioxide at the hood's collar, for exactly 9,600 JE;
  - sulfur alone waits and names oxygen; a full product tank stops it and is named; it goes on once emptied;
  - every part lights while it works and only the controller gives off light;
  - it runs exactly its one recipe, and the starter bill crafts it.
- **Client game test** (`ChemicalOxidizerClientGameTests`): `jugcraft_chemical_oxidizer` (front left, working), `_front`, `_back`, `_night`, `_screen` and `_product_full`.

### Results

- **Run [38297565660](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38297565660) on the contributor's fork (commit 8de837c8): green after one re-run.**
  - Everything compiled. "All 1384 required tests passed", the five new Oxidizer tests among them; the build without optional integrations passed too.
  - Every client test class ran. On the first attempt one client job failed in `PeepoSuppliesClientTests` (a companion's meal timed out), which this change does not touch and which passed on the Infuser's run of every class. Re-running that job once passed.
  - Screenshots, looked at in the job's previews:
    - `jugcraft_chemical_oxidizer`, `_front`, `_back`: the whole machine on its plinth; the hopper on its legs with the hazard lip, the chute and screw trough, the column and ringed hood with its outlet collar, the bolted hatch in its cream ring, the drawer and the control panel all render, matching the offline drawing;
    - `_night`: the same view at midnight, the machine working;
    - `_screen`: "Processing", 71%, power 27k/30k, batches 1/1, with sulfur dust in the hopper slot and oxygen in its tank;
    - `_product_full`: "Output blocked" over "Product gas tank is full".

No survival playtest, two-client test or performance measurement has been done.

## World and event applicability

Not applicable: a placed machine, with no world generation, creatures, loot or seasons.

## Rollout and open questions

- **Next in the acid chain**: contact conversion (SO2 + O2 -> SO3) in the Chemical Infuser family with the selected vanadium contact bed, once its independent mineral supply is specified, then absorption into ordinary sulfuric acid.
- **Not done here**: other oxidation recipes; a residue output; the 4×4×6 larger variant; a Jade readout for forms; companions working forms.
- Reversible: removing the Oxidizer would need its blocks broken first. Its saves use only the foundation's form data.
