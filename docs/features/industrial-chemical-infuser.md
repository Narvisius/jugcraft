# Chemical Infuser (factory package 2, third slice)

Status: implemented (CI green on the contributor's fork; pending review). Built on the [shared machine foundation](industrial-machine-foundation.md), after the [Electrolytic Separator](industrial-electrolytic-separator.md) and [Gas Burning Generator](industrial-gas-burning-generator.md).
Proposal issue: none. The next unchecked item of package 2, "Starter steel chemistry", in the owner's [factory implementation plan (PR #302)](https://github.com/jimbozoomer-byte/jugcraft/pull/302) and the [TODO list](../TODO.md#industrial-machine-models-textures-and-dimensions). Its construction bill, the hydrogen chloride quantities and the 9,600 JE batch budget are the proposals in the [starter gas and acid factory](industrial-starter-gas-and-acid-factory.md); its look is the [art brief](industrial-machine-models-and-textures.md#chemical-infuser).
Owner: jimbozoomer-byte
Target milestone and tier: steel-era chemistry, the gas-reaction machine of the starter gas and acid factory
Primary specialty and supported player role: chemical processing and factory operations

## Player experience

The **Chemical Infuser** joins two gases. It is two wide, three deep and three tall, every position a block. This first slice gives it its hydrogen chloride reaction; methanation with its nickel bed follows.

**Building and placing.** It is crafted from 4 steel plates, 2 steel fluid pipes, a Tinplate Tank, copper cable and a basic circuit: no titanium or advanced circuit, unlike the Synthesis Converter. Placed from the front, it fills eighteen blocks with the control box at the front left. If any of them is taken, nothing is placed and the player is told what is in the way and where. Breaking any block removes the whole machine and drops it once.

**What it looks like** (the art brief):
- a central stepped reaction chamber on a gunmetal plinth: an olive lower body with stencilled service plates, a pale ceramic collar where it steps in, a gunmetal middle body and an olive-topped crown with a relief valve;
- an inlet branch on each shoulder: a charcoal pipe from a bolted flange, a valve with a red handwheel, a chrome collar into the chamber, banded hydrogen on the left and chlorine on the right;
- at the foot of the front, the thick catalyst-bed drawer with a raised handle and its small bed-face plate;
- above it, the ringed inspection port, the heat gauge and the amber process band;
- at the front left, the control box with a gauge and the amber running lamp;
- behind, a closed loop into the short ribbed cooling jacket, with a fan under two guard bars on top;
- at the back, the product collar (banded hydrogen chloride) high on the right and the water drain (banded water) low on the left;
- low on the right side at the back, the junction box with the power socket, a hazard band and two insulators.

While it works the lamp and the process band light on every block and the fan spins up; only the control box gives off light.

**Ports.** Pipes and cables meet it only at the middle of these faces:

| Port | Where (seen from the front) | What |
| --- | --- | --- |
| Hydrogen in | left side, middle block of the middle layer | hydrogen; nothing else goes in |
| Reactant in | right side, middle block of the middle layer | chlorine; nothing else goes in |
| Product out | back of the middle layer, right | the product gas tank |
| Water out | back of the bottom layer, left | the water tank (empty until methanation) |
| Power | right side of the back bottom block | 128 JE/t while it works, up to 1,024 JE/t in |

**Working.**

| Feed | Gives | Time and energy |
| --- | --- | --- |
| 250 mB hydrogen + 250 mB chlorine | 500 mB hydrogen chloride | 3.75 s (75 ticks), 9,600 JE |

Its screen is the shared formed-machine screen. It names what it waits for ("Reactant gas tank" needs chlorine) or what blocks it ("Product gas tank is full"), reads "Warming up" for the first two seconds after a cold start, and takes speed and efficiency cards. JEI shows its recipe in its own category. The Engineer's Handbook has a page for it after the Synthesis Converter's.

## Connections

- Existing input producer: hydrogen and chlorine from the Electrolytic Separator or the Electrolytic Cell splitting brine. A bucket of brine gives exactly 250 mB of each, one Infuser batch.
- Existing output consumer: none yet. Hydrogen chloride is the starter factory's supply for the consumers the plan lists: magnesium chloride preparation, corn-starch glucose syrup and PVC with HCl recovery. Absorption into aqueous hydrochloric acid is planned for the Chemical Reactor, whose single fluid input cannot take a gas and water together yet. It can be stored in gas tanks and cylinders until then.
- Technology connection: the synthesis family (`MachineKind.SYNTHESIS_CONVERTER`), whose energy figures it shares. It runs only recipes asking for its capability, `jugcraft:gas_combination`; the Synthesis Converter keeps its own three recipes.
- Magic connection: none.
- Reachable entry path: steel plates, steel fluid pipes, a Tinplate Tank, copper cable and a basic circuit all come before electrolysis; no circular unlock.
- Which connections are required vs optional: power and both gases are required; the product can be stored or carried.
- How the specialty stays useful alone: it is the starter factory's gas-reaction station; methanation for the Gas Burning Generator's second fuel is its next recipe.

## Balance and automation

- **Quantities** are the plan's proposal: equal gas amounts react, 250 + 250 mB making 500 mB, so no fluid is made from nothing.
- **Energy**: the plan's 9,600 JE a batch (100 ticks at 96 JE/t). The synthesis family draws 128 JE/t, so the batch takes 75 ticks for the same energy. Four efficiency cards bring a batch to 3,900 JE.
- **No gain loop**: hydrogen chloride burns in no generator.
- **Automation**: the product pushes out of its collar every 4 ticks, at most 1,000 mB at a time; an idle Infuser does nothing.

## Multiplayer and persistence

- The server decides placement, recipes, ports and the screen's buttons; the client draws the synced state and turns the fan.
- Saving uses the foundation's role-named tanks (`hydrogen`, `reactant`, `product`, `water`), its batch escrow and its form version (1). The water tank is part of the planned layout now, so methanation adds recipes and its bed socket, not a tank.
- New registrations: the block and item `jugcraft:chemical_infuser`, and the gas `jugcraft:hydrogen_chloride`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The model is built from the mod's own textures: the dieselpunk olive, stencil, gunmetal, chrome, rubber, hazard, gauge and lamp; the ceramic panel and insulators; copper; the flange plate, porthole, ribbed steel, red paint and fan face; the amber strip; the power socket; and the hydrogen, chlorine, hydrogen chloride and water stills for the bands. `tools/industrial_forms.py` authors it and slices it into 18 part models; the fan is quads in `machine_rotor_quads.json`. The hydrogen chloride texture is drawn by `tools/petro_textures.py` from its colours, like every gas.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`: regenerated the Infuser's blockstate, 36 part models, item model, loot table, crafting recipe, hydrogen chloride recipe and fluid tag, the fan quads, the lang strings, its handbook page and its JEI category.
- The hydrogen chloride texture: drawn with the gas texture code; redrawing hydrogen's the same way reproduced the committed file exactly.
- `python tools/check_mod_data.py`: passed, including the form check (Java and Python agree) and the art check of every model and turning part.
- `python scripts/check_repository.py`: passed.
- `javac` reported no syntax errors in the changed Java files; it could not compile them without Minecraft's libraries.
- The sliced part models were drawn together outside the game from six sides with a small z-buffer script (flat shading, first animation frame), to check the slices line up and every side is finished.

For CI:
- **Server game tests** (`ChemicalInfuserGameTests`), 5 tests:
  - in all four orientations it fills its eighteen blocks and only its five ports answer; each inlet takes only its own gas; nothing goes in at an outlet;
  - 250 mB each of hydrogen and chlorine give 500 mB of hydrogen chloride at the product collar, for exactly 9,600 JE;
  - hydrogen alone waits and names chlorine; a full product tank stops it and is named; it goes on once emptied;
  - every part lights while it works and only the controller gives off light;
  - it runs exactly its one recipe, and the starter bill crafts it.
- **Client game test** (`ChemicalInfuserClientGameTests`): `jugcraft_chemical_infuser` (front left, working), `_front`, `_back`, `_night`, `_screen` and `_product_full`.

### Results

- **Run [38294467085](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38294467085) on the contributor's fork (commit 618e3638): green.**
  - Everything compiled. "All 1384 required tests passed", the five new Infuser tests among them; the build without optional integrations passed too.
  - All five client test jobs passed, every client test class included, with no model or loading errors.
  - Screenshots, looked at in the job's previews:
    - `jugcraft_chemical_infuser`, `_front`, `_back`: the whole machine on its plinth from the front left, straight on and the back right. The olive lower body and drawer, the ceramic collar, the inspection port and gauge, both inlet branches with their handwheels, the ribbed cooling jacket with its fan shroud and the junction box all render, matching the offline drawing;
    - `_night`: the same view at midnight, the machine working;
    - `_screen`: "Processing", 28%, power 56k/60k, batches 1/1, with hydrogen and chlorine in their tanks;
    - `_product_full`: "Output blocked" over "Product gas tank is full".

No survival playtest, two-client test or performance measurement has been done.

## World and event applicability

Not applicable: a placed machine, with no world generation, creatures, loot or seasons.

## Rollout and open questions

- **Next for the Infuser**: methanation with its nickel bed and water drain, and methane as the Gas Burning Generator's second fuel, follow in [methane power](industrial-methane-power.md).
- **Open for review**: the 75-tick time keeps the plan's 9,600 JE at the synthesis family's 128 JE/t; the plan's 100 ticks at 96 JE/t would need a per-form draw.
- **Not done here**: aqueous hydrochloric acid and hydrogen chloride consumers; the 4×6×6 larger variant (since built: the [Bulk Chemical Infuser](industrial-bulk-chemical-infuser.md)); a Jade readout for forms; companions working forms.
- Reversible: removing the Infuser would need its blocks broken first. Its saves use only the foundation's form data.
