# Electrolytic Separator (factory package 2, first slice)

Status: implemented (CI green; pending review). Stacked on the [shared machine foundation](industrial-machine-foundation.md) (PR #312).
Proposal issue: none. The first slice of package 2, "Starter steel chemistry", of the owner's [factory implementation plan in PR #302](https://github.com/jimbozoomer-byte/jugcraft/pull/302); the owner asked on 10 October 2026 to begin the next part once package 1 was done. Its construction, quantities, times and output policies are the owner's selections in the [starter gas and acid factory](industrial-starter-gas-and-acid-factory.md) (eighth planning batch, questions 1, 2, 3, 9 and 10).
Owner: jimbozoomer-byte
Target milestone and tier: steel-era chemistry, the first machine of the starter gas and acid factory
Primary specialty and supported player role: chemical processing and factory operations

## Player experience

The **Electrolytic Separator** is the first of the big industrial machines: two wide, two deep and three tall. It splits water and ordinary brine, and it needs no aluminum or advanced circuit, so hydrogen and chlorine come before the Electrolytic Cell.

**Building and placing.** It is crafted from 4 steel plates, 2 Steel Tanks, copper cable, a basic circuit and a Machine Casing. With the Steel Tanks' own plates, that is the owner's selected 20 steel plates. Placed from the front, it fills twelve blocks with the control box at the front left, where it was placed. If any of those blocks is taken, nothing is placed and the player is told what is in the way and where. Breaking any block removes the whole machine and drops it once.

**What it looks like** (the [art brief](industrial-machine-models-and-textures.md#electrolytic-separator)):
- a tall olive cell housing with a stepped door, banded in gunmetal, with four electrode feedthroughs on ceramic insulators on its crown;
- either side, a narrower round collection tower in the white tank family, with a checker shoulder and an amber level strip;
- at the front left, the small control box with two gauges and the amber running lamp;
- at the foot of the right tower, the insulated bus connection: a junction box with a hazard band, the power socket, two insulators and a copper bus bar;
- above it, the recessed lye return;
- high on the front, an outlet collar for each tower, banded in its gas's colours (hydrogen on the left; oxygen or chlorine on the right);
- low at the back, the feed inlet, banded in brine colour.

The lamp and level strips light while it works, on every block of the machine; only the control box gives off light, like a furnace.

**Ports.** Pipes and cables meet it only at the middle of these faces:

| Port | Where (seen from the front) | What |
| --- | --- | --- |
| Feed in | back of the bottom left block | water or brine; nothing else goes in |
| Hydrogen out | front of the top left block | the hydrogen tank |
| Oxygen/chlorine out | front of the top right block | the oxygen/chlorine tank |
| Lye out | front of the middle right block | the lye tank |
| Power | front of the bottom right block | 256 JE/t while it works, up to 1,024 JE/t in |

Outlets push into the pipe or tank beyond, like other machines. Buckets work on any block.

**Working.**

| Feed | Gives | Time and energy |
| --- | --- | --- |
| 1,000 mB water | 500 mB hydrogen and 250 mB oxygen | 40 s (800 ticks), 204,800 JE |
| 1,000 mB brine | 250 mB hydrogen, 250 mB chlorine and 500 mB lye | 20 s (400 ticks), 102,400 JE |

Its screen is the shared formed-machine screen in the laboratory theme. It shows the state ("Processing", "Waiting for power", "Output blocked") and the one thing to fix, such as "Lye tank is full" or "Oxygen/chlorine tank holds another fluid (Oxygen)". For two seconds after a cold start it reads "Warming up", inside the paid work, as the owner chose (heating included in the processing power). Pause and Cancel work as on every form. Two upgrade slots take speed and efficiency cards.

**Output policies, as selected.**
- **Lye is kept.** When the lye tank is full, brine stops until the lye is piped away or used. No lye is vented, and there is no disposal button yet.
- **Water makes no lye**, so water still runs when the lye tank is full.
- **Oxygen and chlorine share one destination, one gas at a time.** Oxygen left from water makes brine wait, and the screen names the oxygen, until it is drawn off.

JEI shows its two recipes in their own category, with the Separator as the station. The Engineer's Handbook has a page for it after the Electrolytic Cell's.

## Connections

- Existing input producer: water anywhere; brine from the Chemical Reactor (two salt and a bucket of water).
- Existing output consumer:
  - hydrogen for the Fuel Cell, premium diesel (Hydrotreater), kerosene (Catalytic Cracker) and ammonia (Synthesis Converter);
  - oxygen for oxygen-blown steel, nitric acid and liquid oxygen;
  - chlorine for vinyl chloride (PVC), titanium sponge (the Kroll process) and chlorine grenades;
  - lye for alumina (the Bayer process), soap and antidote.

  Gas tanks, pipes and gas cylinders carry them, as they do the Electrolytic Cell's.
- Technology connection: the Electrolytic Cell family (`MachineKind.ELECTROLYTIC_CELL`), whose energy figures it shares. It runs only the recipes that ask for its capability, `jugcraft:aqueous_electrolysis`. The Cell keeps its own recipes; only its brine time changes, to match (below).
- Magic connection: none.
- Reachable entry path: steel plates, Steel Tanks, copper cable, the basic circuit and the Machine Casing all come before electrolysis. None needs hydrogen, chlorine, aluminum or an advanced circuit, so there is no circular unlock.
- Which connections are required vs optional: power and a feed are required; every output can be stored, piped or carried in tanks. Trade can supply brine, salt or the finished gases.
- How the specialty stays useful alone: the Separator is a complete first gas source; HCl, methane and acid stations come later as their own purchases.

## Balance and automation

- **Quantities** are the owner's selected starter baseline. Water keeps the existing anchor: 2 liquid mB of water are one reaction amount, so 1,000 mB give 500 mB of hydrogen and 250 mB of oxygen. Brine keeps its existing products, at the selected 400 ticks.
- **Energy per batch** follows the existing upgrade arithmetic (`MachineUpgrades`):
  - with no cards: water 204,800 JE (409.6 JE per mB of hydrogen); brine 102,400 JE (409.6 JE/mB);
  - with four efficiency cards, the cheapest: 84,000 and 42,000 JE (168 JE/mB);
  - speed cards shorten a batch but cost more energy per batch.
- **No gain loop.** The Fuel Cell returns 128 JE per mB of hydrogen, at most 76% of the cheapest batch's cost, before counting salt or pumping.
  - The plan's proposed floor of 256 JE per mB of hydrogen for electrolysis is not needed by this entry form, which has no profile saving. It must be enforced before an expanded or bulk Separator, or a generator bonus, could close the gap.
- **The Cell's brine now matches**: 400 ticks (102,400 JE) instead of 200 (51,200 JE). The plan proposed this and marked it for review; the owner chose it on 11 October 2026. It is a parity change, not a loop fix: the Cell takes no upgrade cards, so its brine already cost more than its hydrogen returns (32,000 JE). Its products and its water and aluminum recipes are unchanged.
- **Automation**: outlets push every 4 ticks, at most 1,000 mB per port; an idle Separator does nothing. Normal repeat processing needs no automation chip.

## Multiplayer and persistence

- The server decides everything: placement, recipes, ports and the screen's buttons. The client only draws the synced state.
- Saving uses the foundation's role-named tanks (`feed`, `anode_gas`, `hydrogen`, `lye`), its batch escrow and its form version (1), so a later version can add tanks without moving contents.
- **Foundation change in this slice**: each form block now numbers only its own parts. The Separator's `part` property runs 0–11, giving 96 block states instead of 1,728. Nothing saved used the old range: no form was registered in the game before this.
- **Foundation change in this slice**: a working form lights every part, not just its controller, so lamps and strips anywhere on it show the work. It stays lit for 20 ticks after it last worked, so a moment's pause does not relight every block; the other parts change only on the client, without neighbour updates, and only the controller gives off light.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The model is built from the mod's own textures: the dieselpunk olive, gunmetal, chrome, hazard, rubber, gauge and lamp sets; the tank family's body, top and checker; the ceramic insulator, copper bus bar and copper; the amber strip; the power socket; and the hydrogen, oxygen, chlorine, lye and brine stills for the port bands. `tools/industrial_forms.py` authors it as one model and slices it into twelve part models with the shared `model_writer.split_model`. The classic resource pack has no other look for it.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`: regenerated. It added only the Separator's blockstate, 24 part models, item model, loot table and three recipes; five lang strings; its handbook page; and its JEI category.
- `python tools/check_mod_data.py`: passed, including the art check of every block and item model. Its new form check:
  - compares `IndustrialForms.java` with `tools/industrial_forms.py`: layers, profile, tanks, capabilities, upgrades, warm-up and ports;
  - checks the blockstate covers 4 facings × 2 lights × 12 parts;
  - checks each recipe asks for a capability the form offers, fits its 2,000 mB tanks, sends its results to distinct real outlets and makes no fluid from nothing;
  - checks the construction comes to 20 steel plates.

  Breaking each of these on purpose made it fail.
- `python scripts/check_repository.py`: passed.
- A tree-sitter Java syntax parse of the 8 changed or new Java files: no errors. This is not a compile.
- The model was drawn outside the game from all four corners, lit and unlit, to check the parts line up and every face is finished.

For CI:
- **Server game tests** (`ElectrolyticSeparatorGameTests`), 7 tests:
  - in all four orientations it fills its twelve blocks and only its five ports answer; the feed takes water and brine but not lava, and nothing goes in at an outlet;
  - water gives 500 mB of hydrogen and 250 mB of oxygen, leaving at their collars, for exactly 204,800 JE;
  - brine gives 250 mB each of chlorine and hydrogen and 500 mB of lye, for exactly 102,400 JE;
  - a full lye tank stops brine, naming the tank and spending nothing, while water still runs; brine goes on once the lye is gone;
  - oxygen left in the shared tank makes brine wait, naming oxygen, until it is drawn off;
  - while it works all twelve parts are lit and only the controller gives off light; paused, every part goes dark after the 20-tick hold;
  - it runs exactly its two recipes while the Cell keeps its own (brine now 400 ticks, water 800), and its crafting recipe and the Steel Tank's together come to 20 plates.
- `IndustrialFoundationGameTests` now checks that each form block numbers only its own parts.
- `PetroGameTests.cellSplitsBrine` now allows 600 ticks and keeps the Cell charged: its brine needs 102,400 JE, more than the Cell's 60,000 JE store.
- **Client game test** (`ElectrolyticSeparatorClientGameTests`): `jugcraft_electrolytic_separator` (front left, working), `_front` (straight on), `_back`, `_night`, `_screen` (working) and `_lye_full` (brine stopped by a full lye tank).

### Results

- **Run [38089455548](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38089455548) (commit 1474cecd): green.**
  - Everything compiled. Both server game test jobs passed, "All 1298 required tests passed": the 1,292 before this change and the 6 new ones.
  - The client test selector chose this test and the foundation's; both passed with no model or loading errors.
  - Screenshots:
    - `_screen`: the Separator's screen reads "Processing", 22%, power 53k/60k, batches 1/1, with the feed in the batch and gas in two outlet tanks. The power line now fits.
    - `_lye_full`: "Output blocked" wraps onto two lines over "Lye tank is full", with brine waiting in the feed tank and the lye tank full.
    - The three world shots show the model working, its lamp lit at night. But they looked up and cut off its base: a teleport's "facing" aims from the feet, not the eyes. The test now aims from the eyes and adds the straight-on `_front`.
- **Run [38090159225](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38090159225) (commit 4119897f): green.**
  - "All 1298 required tests passed"; both client tests passed.
  - The four world shots now frame the whole machine on its plinth:
    - `jugcraft_electrolytic_separator` (front left): the control box's two gauges and lamp, the door, the junction box with its socket and hazard band, the lye return, the white left tower with its checker shoulder and level strip, and the collars;
    - `_front`: the front straight on, the towers either side of the housing;
    - `_back`: the banded housing's access panel and the feed inlet low at the back;
    - `_night`: the same front-left view, with the lamp lit. The strips, though, could light only where they pass through the controller's block: the foundation lit the controller alone. The commit after the brine change lights every part, and its run retakes this shot.
- **Run [38105664707](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38105664707) (commit 7d6ef403, the Cell's brine at 400 ticks): green.** "All 1298 required tests passed", among them the Cell's own brine test at its new time.
- **Run [38106303119](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38106303119) (commit cfce0f3d, every part lit): green.** "All 1299 required tests passed", the new `everyPartLightsWhileItWorks` among them. The retaken `_night` shot looks the same as before: in the open at midnight the test client's view is not dark enough to show any glow, so the server test, not the picture, is the evidence that every part now lights.

No survival playtest, two-client test or performance measurement has been done.

## World and event applicability

Not applicable: a placed machine, with no world generation, creatures, loot or seasons.

## Rollout and open questions

- **Next in package 2**:
  - the Chemical Infuser (HCl and, with its nickel bed, methanation, including the selected methanation-water drain);
  - the Chemical Oxidizer and contact conversion for the sulfuric acid chain;
  - the shared Gas Generator (128/256 JE/t);
  - the Coal Gasifier with Gas Cleanup and Separation;
  - portable handling.
- **Not done here**:
  - the 4×4×6 bulk Separator;
  - the electrolysis energy floor, needed before any larger form;
  - a lye disposal route and its pollution record;
  - the peroxide recipe the art brief mentions;
  - a Jade readout for forms;
  - companions working forms.
- Reversible: removing the Separator would need its blocks broken first. Its saves use only the foundation's form data.
