# Machine screens: five looks by tech level

Status: implemented (pending CI and review). Replaces the batch 22 screens (#78, see History).
Proposal issue: none. The owner, 14 October 2026: "refining user interfaces to match the images I have provided ... Make them more stylistic depending on the tech level of the machine and increase the simplicity and user experience so that its more intuitive." The owner attached five reference images:
- a cyan holographic heads-up display;
- a chunky blue pixel device with icon buttons and a status dot;
- a grey monitor with green text, a preview and two green buttons;
- a dark blueprint skill tree with coloured tabs;
- an orange-on-brown pixel device with an icon toolbar and a side gauge.

The owner approved the offline mockups and the machines' grouping ("Looks right, go ahead and build it").

Owner: jimbozoomer-byte
Target milestone and tier: every machine and every industrial form
Primary specialty and supported player role: every player who uses a machine

## Player experience

**One layout for every machine.** The screen is still 268 by 166.
- **The machine's body:**
  - a title strip with a **status lamp**: green while it works, a dim lamp when it is idle, red when something is wrong;
  - the **machine pane**, holding the energy bar, slots, tanks and progress arrow where they always were. A machine with nothing else in it (a battery or solar panel) shows a **big charge gauge** with its percentage instead of the small energy bar;
  - the **status pane**.
- **The inventory** sits on its own tray under the body.
- **Beside the inventory:** the **help box**, as tall as the tray. It says in plain words what the machine is for. On machines with controls, a row of **icon buttons** sits along its top.

**The status pane says what is happening and what to do about it:**
- One word: RUNNING, IDLE, NO POWER, OUTPUT FULL, GENERATING, CHARGING, DISCHARGING or STORING.
- When the machine is stuck, the fix:
  - "Connect a cable or a generator.";
  - "Take the results from OUT, or turn on Eject.";
  - "Put something to work on in IN.";
  - "Put fuel in its slot."
- While it works: a progress meter with its percentage, and the time left. The time comes from the pace over the last second, so speed upgrades and helpers are counted.
- Power stored, its rate, and the machine's own condition where it has one (formed, oil or dry, and so on).

**Controls are icons with tooltips:**
- Redstone mode: the icon turns red with its mode.
- Eject: a small lit dot when on.
- Sides.

**The side settings stay out of the way until wanted.** The Sides button turns the status pane into the machine's six faces: front in the middle, back to the right, top, bottom, left and right. Each face is coloured by what it does; click one to change it. The help box shows the colour key (in, out, in and out, closed), and the button turns into a Back arrow.

**Empty slots explain themselves:**
- IN and OUT labels sit over a processor's slots where they fit.
- Upgrade slots and fuel slots show a faint hint icon while empty.
- Hovering an empty slot says what goes there ("Input: what the machine works on", "Upgrade: a speed or efficiency card").
- Hovering the energy bar, the charge gauge or a tank still gives exact numbers.

**Industrial forms** use the same layout:
- the status pane gives the form's state and the one thing to fix, with progress, the time left, power, and batches when there are several. The coal gasifier adds what it has emitted so far;
- icon buttons pause or resume it, cancel its batches, open its **recipe page** (lit while a recipe is locked), and choose whether excess byproduct is vented, or excess water drained (a droplet; the forms that vent excess gas by the same switch show the vent icon);
- its tool sockets and upgrade slots sit in the help box. A form without them says there what it is for. Every one of the 37 forms has a line.

**Five looks by tech level.** The references set the mood and colours; everything is drawn fresh.

| Look | After | Machines |
| --- | --- | --- |
| Workshop | the orange device | the first machines: coal and steam generators, water wheel, wind turbine, crusher, electric furnace, alloy smelter, metal press, wire drawer, pulverizer, ore washer, sieve, sawmill, cobblestone generator, tree farm, crop harvester, auto-crafter, large steam engine |
| Industrial | the green-text monitor | steel and oil: every other machine (foundry, coke oven, arc furnace, drills, pumpjack, refinery and cracking, generators and engines, tanks and the rest), and every industrial form |
| Electric | the blue device | power gear: battery box, capacitor bank, solar panels, fuel cell, lithium battery bank, flow battery, advanced engine |
| Lab | the blueprint screen | chemistry: chemical reactor, electrolytic cell, air separation unit, synthesis converter, polymerization reactor |
| Advanced | the holographic display | electronics and nuclear fuel: circuit assembler, lithography station, gas centrifuge cascade, fuel fabricator |

How each look is drawn:
- **Workshop**: a warm brass casing with soft blotches, an orange-on-brown screen with a faint grid and a scan band, orange-rimmed keys.
- **Industrial**: a brushed steel bezel, green phosphor panes and green keys.
- **Electric**: a chunky blue device with cyan glass, side vents and a blue tray.
- **Lab**: a dark blueprint grid in a pale frame, outlined slots and coloured tabs for the controls (red redstone, green eject, blue sides).
- **Advanced**: dark teal with cyan hologram frames and cut corners.

## Connections

Every machine and form screen. No gameplay change.

## Balance and automation

None. The screen shows only what the server already syncs: the machine's data, its slots and its tanks. The rate, the time left and the hints are worked out on the client from those.

## Multiplayer and persistence

- Client-only drawing.
- The menus, slots, synced data and buttons' messages to the server are unchanged, so servers and saves are unaffected.
- The sides view is a client-only toggle.

## Dependencies and assets

- No new dependencies.
- `tools/gui_textures.py` draws the five backgrounds (`textures/gui/machine_<look>.png`, replacing `machine_diesel.png` and redrawing `machine_electric.png` and `machine_lab.png`). It also holds:
  - which look each machine uses, and the forms' look;
  - the looks' colours (`gui/machine_themes.json`);
  - the taglines for machines and forms;
  - the status words, hints and slot help, as lang keys.
- `client/MachineSkin` draws the moving parts and the buttons: slots, the energy bar, the charge gauge, the arrow, the lamp, thirteen 8×8 pixel icons, the icon buttons and the face buttons.
- The companion wheel (`local/peepo/client/WheelScreen`) borrows the coal generator's background, so it takes the workshop look.

## Verification

Local checks:
- `tools/check_mod_data.py`, `scripts/check_repository.py` and the script unit tests: see Results.
- Every form has a tagline. `gui_textures.write_all` fails on a form without one.
- An overlay of every slot, tank, tool socket, button and face position on the new backgrounds showed each inside its pane, with no clipping at a pane's edge.

Client game test (`JugcraftClientGameTests`):
- one screen per look: the crusher (workshop), the battery box (electric), the circuit assembler (advanced), the chemical reactor (lab) and the distillation tower (industrial, the screen with the most outputs);
- the crusher's sides view;
- the auto-crafter's screen.

The industrial forms' own client tests photograph their screens in the industrial look.

### Results

Local, without Gradle or a game:
- `python tools/generate_material_data.py` regenerated `gui/machine_themes.json` (the five looks; 18 workshop, 23 industrial, 8 electric, 5 lab and 4 advanced machines; forms industrial) and added 33 lang strings (hints, slot help, form taglines, the Sides button).
- `python tools/check_mod_data.py`: **PASS**, including the art check of every model.
- `python scripts/check_repository.py`: **PASS**.
- `python -m unittest discover -s scripts/tests`: **OK**.
- A tree-sitter syntax parse of the five changed Java files: 0 errors. This is not a compile.

CI on 5c0a70c09 ([run 38506018399](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38506018399)), all **passed**:
- `mod`: the generated-JSON and material checks, then `./gradlew build` with the server game tests;
- `optional integrations absent`: the same build and server tests without the optional integrations;
- the five client shards, chosen by `tools/select_client_tests.py`:
  - `JugcraftClientGameTests`;
  - `JugcraftServerClientGameTests`;
  - the client tests of all ten industrial forms, plus `IndustrialFoundationClientGameTests`;
- `repository`: passed in its own workflow run.

The first CI run on the merge with main (5dabac78e) failed to compile: the new sides-view step read `client.screen`, which 26.x does not have. 5c0a70c09 reads `client.gui.screen()`, like the other client tests.

The CI screenshots, checked by eye:
- each of the five looks;
- the crusher's sides view, with its colour key;
- the form screens, including the waiting, output-blocked and idle generator states.

In them, the status text, warnings, power rows and taglines sit inside their panes. At the CI window size, the recipe viewer's item panel sits beside the screen without overlapping it. The screenshots also showed three faults, which the next commit fixes:
- In the sides view, the key's "input and output" ran past the help box's edge. The key now uses short words (in, out, in and out, closed), and the help line is shorter, so all four fit.
- On machines without controls (generators, batteries, fluid machines), the world showed through the empty strip between the status pane and the help box. The help box now starts under the status pane, level with the tray, and the buttons sit inside its top.
- The battery box's pane was empty but for the small energy bar. Batteries and solar panels now show the big charge gauge.

After those fixes:
- the five backgrounds were redrawn;
- an overlay of the buttons, tool sockets, help text area and charge gauge showed each inside its box;
- the local checks above were run again: `check_mod_data.py` **PASS**, `check_repository.py` **PASS**, the unit tests **OK**, a second generation changed nothing, and the Java syntax parse found 0 errors.

Main then gained the factory chains (#358, 4d38e9712), which added a recipe page, a drain or vent switch and an emissions count to the old form screen, and 27 more forms. The merge carries them into this layout:
- **Recipes** is an icon button. Its label stays "Recipes" (or "Locked recipe"), so the client tests that click it by name still find it.
- The drain or vent switch is the row's last button; no form has both it and the excess-vent choice.
- The emissions count is a status row, EMITTED.
- The Gas Scrubber keeps its own title.
- The 27 new forms got taglines, written from their tanks, capabilities and records. All but the Coastal Seawater Intake have upgrade slots, so the help box shows those instead.

After the merge:
- the generation, `check_mod_data.py` (**PASS**), `check_repository.py` (**PASS**) and the unit tests (**OK**, 34) were run again;
- the Java syntax parse of the client and game-test sources found 0 errors.

CI on the merge, 27a1c01fc ([run](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38512640407)), all **passed**:
- `mod` and `optional integrations absent`;
- the five client shards, which now include #358's form tests: StarterChemistry, Fluorochemistry, MachineConsolidation (which clicks Recipes, then Lock recipe, Automatic and Back to machine), Biorefinery, CoastalMetals, Anaerobic and Polymer;
- `repository`.

Its screenshots, checked by eye:
- The help box fills the strip under the status pane on every look, and the buttons sit in its top.
- The sides key shows all four entries inside the box.
- The battery box shows its charge gauge and percentage.
- On the Chemical Infuser, the drain droplet is lit as the fourth button. The coal gasifier shows EMITTED. The Recipes button sits on every processor form.

Not run:
- a human trying every machine's screen;
- other GUI scales and window sizes.

## World and event applicability

Not applicable.

## Rollout and open questions

- The other machine screens (prospector, rocket pad, storage tank, logic controller, reactor console, remote shutdown panel, drone depot terminal and drone tower) take their looks in [the other machine screens](other-machine-screens.md).
- A machine's look can be changed by moving it between the sets in `tools/gui_textures.py`.

## History

Batch 22 (#78, 1 October 2026) gave every machine a 268-wide screen with three themes (dieselpunk amber, electric green, lab teal), a CRT terminal with the machine's tagline and readouts, side controls under it, and a decorative keyboard. This version keeps its slot positions, gauges and taglines. It replaces:
- the three themes, with five looks by tech level;
- the terminal and keyboard, with the status pane, icon buttons and help box;
- the always-shown side buttons, with the sides view.
