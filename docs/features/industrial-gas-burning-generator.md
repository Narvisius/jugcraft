# Gas Burning Generator (factory package 2, second slice)

Status: implemented (pending CI and review). Stacked on the [Electrolytic Separator](industrial-electrolytic-separator.md) (PR #316), itself on the [shared machine foundation](industrial-machine-foundation.md) (PR #312).
Proposal issue: none. The second slice of package 2, "Starter steel chemistry", of the owner's [factory implementation plan in PR #302](https://github.com/jimbozoomer-byte/jugcraft/pull/302): the plan's shared hydrogen and methane generator, the slice the owner chose on 11 October 2026. Its output per tick is the owner's selection in the [starter gas and acid factory](industrial-starter-gas-and-acid-factory.md) (eighth planning batch, question 8: 128 JE/t on hydrogen, 256 JE/t on methane). Its construction bill and its burn value of 128 JE per mB of hydrogen are that record's proposals; the burn value is also the Fuel Cell's existing one.
Owner: jimbozoomer-byte
Target milestone and tier: steel-era power, the generator of the starter gas and acid factory
Primary specialty and supported player role: power generation and factory operations

## Player experience

The **Gas Burning Generator** turns hydrogen into electricity without the Fuel Cell's aluminum. It is four wide, five deep and four tall: a gas engine and generator on a steel skid.

**Building and placing.** It is crafted from 4 steel plates, a steel gear, an electric motor, a basic circuit, a Tinplate Tank and a Machine Casing. Placed from the front, it fills 50 of the 80 positions in its envelope, with its ignition box at the front left, where it was placed. Above the skid, the front three positions on either side are left open, two high, beside the heat shield, and so is the whole top layer except the top of the cabinet. Anything may stand in those open positions. If any block it needs is taken, nothing is placed and the player is told what is in the way and where. Breaking any block removes the whole machine and drops it once.

**What it looks like** (the [art brief](industrial-machine-models-and-textures.md#gas-burning-generator)):
- a gunmetal skid under the whole machine;
- at the front, the round intake: a chrome rim and a cross of guard bars before a six-bladed fan;
- at the front left, the stepped ignition box with two gauges and the amber running lamp;
- the olive combustion section with graphite ribs and amber sight glasses on either side of the burner, under an arched pale heat shield on four posts;
- behind it, the coupling, inside hazard-banded hoops joined by guard bars;
- the large olive generator housing, ribbed in steel, with a cooling grille on its back;
- at the rear right, the tall terminal cabinet: gunmetal with an olive door and stencil, a hazard band, two mint lamps, the power socket, and three ceramic insulators with copper caps and a bus bar on top, cabled over from the generator housing;
- the exhaust leaving the burner's belly, running back low along the skid on the left and turning out at the back;
- low on the right, the capped gas inlet, banded in hydrogen colour, running through a regulator into the burner.

While it runs, the amber lamp and sight glasses and the mint lamps light on every block, and only the ignition box gives off light. The coupling and the fan spin up over two seconds when it starts and run down as long when it stops; idle, they stand still.

**Ports.** Pipes and cables meet it only at the middle of these faces:

| Port | Where (seen from the front) | What |
| --- | --- | --- |
| Fuel in | right side of the bottom block, second from the front | hydrogen; nothing else goes in |
| Power out | back of the cabinet, second block up, third from the left | its power, up to 2,048 JE a tick |

Cables connect only at the socket; nothing can push power into it.

**Working.** It burns a millibucket of hydrogen a tick, 128 JE each, and makes 128 JE/t into its 120,000 JE store, which it pushes out of the socket. A bucket of hydrogen lasts 50 seconds and gives 128,000 JE. It makes only as much as its store has room for: when nothing draws its power it idles, keeping its fuel, and when something draws less than 128 JE/t it makes just that. Fuel already burnt and not yet made into power stays in hand, is saved with the machine, and is made first.

Its screen is the shared formed-machine screen in the diesel theme: the fuel tank on the left, an arrow lit while it makes power, and the store on the right. The terminal reads "Generating", "Idle" with "Fuel tank is empty" or "Power store is full", and gives the JE it made last tick and the store's level. Pause stops it; a generator runs no batches, so there is no Cancel. A comparator reads 0 when it makes nothing and up to 15 at its full rate.

## Connections

- Existing input producer: hydrogen from the Electrolytic Separator and the Electrolytic Cell (water or brine), carried in gas tanks, pipes and gas cylinders.
- Existing output consumer: every Jugcraft cable network and powered machine, through the socket.
- Technology connection: the combustion generators' family (`MachineKind.GAS_TURBINE`), for its energy figures (120,000 JE store, 2,048 JE/t out) and its screen. What it burns is listed on the form itself, so the Gas Turbine never burns hydrogen, and the Fuel Cell keeps its own fuel.
- Magic connection: none.
- Reachable entry path: steel plates, a steel gear, an electric motor, a basic circuit, a Tinplate Tank and a Machine Casing all come before electrolysis. It needs no aluminum or advanced circuit, unlike the Fuel Cell.
- Which connections are required vs optional: hydrogen is required. Methane, at the selected 256 JE/t, comes with the methane Infuser.
- How the specialty stays useful alone: it turns stored or traded hydrogen into power wherever the network reaches.

## Balance and automation

- **Burn value**: 128 JE per mB of hydrogen, the Fuel Cell's and the plan's proposed value. **Output**: 128 JE/t, the owner's selection, so a millibucket a tick.
- **No gain loop.** The cheapest electrolysis is a Separator with four efficiency cards: 168 JE per mB of hydrogen (the Cell, which takes no cards, spends 409.6). Burning hydrogen returns 128, at most 76%.
  - A Separator splitting water without stopping makes 0.625 mB of hydrogen a tick at 256 JE/t. The generator turns that into 80 JE/t on average: 31% of the electricity back, 76% with four efficiency cards.
  - The audit checks every generator fuel against the cheapest recipe making it.
- The plan's recovery ceiling (192 JE per mB of hydrogen) and electrolysis floor (256 JE per mB) stay proposals. This generator has no bonus or heat recovery for them to bound yet.
- **Demand-led**: fuel burns only as power is drawn. Nothing is vented or wasted, and a full store idles it with its fuel kept.
- **Methane**: not yet in the game. When the methane Infuser adds it, the form gains a second fuel: the plan proposes 448 JE/mB, at the selected 256 JE/t.
- **Pollution**: the plan has generator emissions follow fuel burnt; there is no pollution system yet, so nothing is recorded.

## Multiplayer and persistence

- The server decides everything: placement, burning, the ports and the screen's buttons. The client only draws the synced state and turns the coupling and fan.
- Saving uses the foundation's role-named tank (`fuel`), with the energy, the JE in hand and their rate, and its form version (1).
- **Foundation changes in this slice:**
  - **Generator forms.** A form can list the fuels it burns, each with its JE per mB and JE a tick. Such a form needs a generator family, one fuel tank and no product tanks, slots, sockets, upgrade slots or capabilities. It needs a fuel inlet and a power outlet, and it never takes power in. Building one any other way is refused.
  - **Power-out ports** (`FormPort.Kind.ENERGY_OUT`): cables connect at power ports of either direction. A generator's store gives power out and takes none in.
  - **The generator tick**: burn a millibucket at a time into JE in hand, make what the store has room for up to the fuel's rate, and push out of the power ports up to the family's output. Two new status reasons ("Fuel tank is empty", "Power store is full"), and the title "Generating".
  - **The screen's generator layout**, described above.
  - **Turning parts**: forms draw theirs through `MachineRotors` with `FormMachineRenderer`, lit as the inside of the machine, so they do not brighten with the ignition box's glow.
  - **No furnace smoke**: a form of a burning family would have smoked from its controller's column. Forms show their work in their models instead. No form of such a family existed before.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The model is built from the mod's own textures:
- the dieselpunk olive, gunmetal, chrome, hazard, rubber, grille, exhaust, gauge, lamp and stencil sets;
- the electric set's pale ribbed panel (the heat shield) and mint lamp;
- ribbed steel and the amber strip;
- ceramic insulators, the copper bus bar and copper;
- the flange plate and fan face (the item model's still fan);
- the power socket and the hydrogen still.

`tools/industrial_forms.py` authors the model and slices it into 50 part models with the shared `model_writer.split_model`. The turning parts are quads in `machine_rotor_quads.json`, written by `tools/machine_rotors.py`. The classic resource pack has no other look for it.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`: regenerated. It added the generator's blockstate, 100 part models, item model, loot table and crafting recipe; its turning parts in `machine_rotor_quads.json`; the block name, "Fuel tank", "Generating" and the two new reasons; and its handbook page after the Fuel Cell's.
- `python tools/check_mod_data.py`: passed, including the art check of every block and item model and the turning-part check. The form check now also:
  - compares each form's fuels in `IndustrialForms.java` and `tools/industrial_forms.py`;
  - checks a generator burns from one fuel tank, runs no recipes and only gives power out;
  - checks no fuel burns for as much as the cheapest recipe making it spends.

  Breaking it on purpose made it fail each time: a different output in Java; a different burn value in Python; 200 JE/mB in both (the loop rule alone); the power port made an input.
- `python scripts/check_repository.py`: passed.
- A tree-sitter Java syntax parse of the changed and new Java files: no errors. This is not a compile.
- The model was drawn outside the game from three sides, turning parts included, to check the slices line up.

For CI:
- **Server game tests** (`GasBurningGeneratorGameTests`), 7 tests:
  - in all four orientations it fills its fifty blocks and leaves its open positions free;
  - only the inlet takes fluid, and only hydrogen: not water, oxygen or the turbine's refinery gas;
  - only the socket answers cables, and it only gives power;
  - hydrogen burns a millibucket a tick into exactly 128 JE, reading "Generating" with the comparator at 15;
  - with room for 64 JE it burns one millibucket and keeps 64 JE in hand, through saving and loading. With 1,000 JE drawn it makes exactly those, the 64 first;
  - paused, it burns and makes nothing;
  - a battery box at the socket fills while one at the front gets nothing, and every JE is accounted for;
  - every part lights while it generates, and only the controller gives off light;
  - its fuel is hydrogen at 128 JE/mB and 128 JE/t; the turbine still burns none; electrolysis spends at least 168 JE/mB; and the plan's bill crafts it.
- `IndustrialFoundationGameTests.formDescriptorsCheckThemselves` now also builds a small generator form, and refuses:
  - a generator of a processing family;
  - one with a product tank or upgrades;
  - one taking power;
  - a fuel listed twice, or making more a tick than the store holds;
  - a generator whose power cannot leave;
  - a processing form giving power out.
- **Client game test** (`GasBurningGeneratorClientGameTests`):
  - world shots: `jugcraft_gas_burning_generator` (front left, working), `_front`, `_back` (back right), `_side` (the right side) and `_dark` (shut in a black box at midnight, so only its lamps and the ignition box's light show);
  - screen shots: `_screen` (generating) and `_store_full` (idle on a full store).

### Results

Pending: CI has not run on this branch yet.

No survival playtest, two-client test or performance measurement has been done.

## World and event applicability

Not applicable: a placed machine, with no world generation, creatures, loot or seasons.

## Rollout and open questions

- **Next in package 2**:
  - the Chemical Infuser: HCl, and methanation with its nickel bed and water drain, which makes methane for this generator;
  - the Coal Gasifier with Gas Cleanup and Separation;
  - the Chemical Oxidizer and contact conversion, for the sulfuric acid chain;
  - portable handling.
- **Not done here**:
  - methane as a fuel (no methane exists yet);
  - the 5×6×5 larger variant;
  - exhaust or heat recovery, and the recovery ceiling that would bound it;
  - a pollution record;
  - a Jade readout for forms;
  - companions working forms.
- Reversible: removing the generator would need its blocks broken first. Its saves use only the foundation's form data.
