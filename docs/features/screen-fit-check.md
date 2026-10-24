# Machine screens: the recipe page's look and a fit check

Status: implemented (pending CI and review). Follows [the other machine screens in their looks](other-machine-screens.md) and [machine screens: five looks by tech level](machine-screens.md).
Proposal issue: none. The owner, 23 October 2026, picked two of the suggested polish items ("do 1 and 2"):
1. the form machines' recipe page, still in its own teal, takes its machine screen's look;
2. a check, at a second GUI size, that the machine screens fit a small window, after the logic controller had been cut off at a 240-high GUI.

Owner: jimbozoomer-byte
Target milestone and tier: the industrial forms' recipe page; every machine screen
Primary specialty and supported player role: players running industrial forms; anyone playing in a small or 4:3 window

## Player experience

**The recipe page** (the RECIPES key on an industrial form's screen) now has the forms' industrial look, so it no longer changes colour when it opens:
- the steel body;
- the status lamp and "<machine>: recipes" along the top. The lamp is green while a recipe is locked, and amber when the recipes were reloaded or the locked recipe is gone;
- the job the machine is on, under the title;
- the recipe list and the chosen recipe's details in two panes;
- keys in the look. The chosen recipe's key is lit (it used to be marked "> "), Automatic is lit while the machine chooses its own jobs, and Lock is lit for the locked recipe. The page keys are dimmed at either end of the list.

It keeps every function: the search, the list, Lock, Automatic, priority, stock target, reset rules, Back, the scrolling details, and the same button ids.

**The storage tank screen** is 352 pixels wide, wider than the narrowest GUI Minecraft's automatic scale gives (320, in a 4:3 window). Like the reactor console and remote shutdown panel, it now scales itself down to fit when the window is narrower than it. At 427 or more (any 16:9 window) it is drawn as before.

## The fit check

Minecraft never gives a GUI smaller than 320 × 240 GUI pixels: whatever GUI scale is set, it caps the scale so the GUI keeps at least that size (only a window smaller than 320 × 240 goes below it). A new client game test, `ScreenFitClientGameTests`, resizes the test window to 640 × 480, which gives that GUI, and opens:
- every machine kind's screen;
- every industrial form's screen;
- the recipe page of the form with the most recipes;
- the prospector, rocket pad, logic controller, storage tank, reactor console, remote shutdown panel, drone depot terminal and drone tower.
- since [the benches in the workshop look](bench-screens.md): the cooking pot, lampwright's bench, farm stand, cave survey desk and carving knife.

For each screen it checks that the panel the screen reports (`ScreenKit.Fitted.panelArea`, after any scaling to fit) lies inside the window. It fails with a list of the screens that do not fit. It photographs one machine screen, the busiest form, the recipe page and the eight other screens at that size (`jugcraft_fit_*`), then puts the window and GUI scale back.

The screens are opened on stand-in menus, with no block and no server round trip. That is enough to judge their size and layout; the other client tests show them working.

## Connections

- `ScreenKit.Fitted`: each machine screen says where it draws its panel. A new machine screen should implement it and be added to the test.
- `ScreenKit.SMALLEST_GUI_WIDTH` and `SMALLEST_GUI_HEIGHT` (320, 240).
- The recipe page uses the theme of the form screen it opens from (`FormMachineScreen.theme()`), the industrial look, through `ScreenKit`.
- Two new strings in `tools/machine_consolidation.py`'s labels, generated into the language file: `container.jugcraft.form.recipes_of` ("%s: recipes") and `container.jugcraft.form.recipe_count` ("%s recipes", which was written in English in the code).

## Balance and automation

None. Display only.

## Multiplayer and persistence

Unchanged. The recipe page sends the same button ids through the same open menu. Nothing new is saved.

## Dependencies and assets

No new dependencies or assets. The test uses Fabric's client game test `TestInput.resizeWindow` (Fabric API 0.161.0).

## Verification

### Results

Local, without Gradle or a game:
- `python tools/generate_material_data.py` added the two strings and nothing else of this change's.
- The other checks and the Java syntax parse: see the PR.

CI, [run 40023529126](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/40023529126) on 14dfb9938: green, all 21 checks. The 34 client classes it chose include `ScreenFitClientGameTests`, which passed: every screen fits the 320 × 240 GUI. Its shots (`jugcraft_fit_*`) showed two faults on the recipe page, fixed in the next commit:
- the recipe's name and the INPUTS and OUTPUTS headings were in the look's label colour, which in the industrial look is the dark grey meant for titles on the casing, so they could not be seen on the glass. Headings (and the scroll bar) now use the look's bright accent;
- a lit key that cannot be pressed (Automatic while the machine already chooses its own jobs) had its label in the dim colour on the lit face, so it could not be read. A lit key now keeps its label whole; keys that cannot be pressed have it half faded and a darker face (all screens using `ScreenKit` keys).

CI, [run on 72a85ee98](https://github.com/jimbozoomer-byte/jugcraft/pull/469) with those fixes: green, all 21 checks. The headings could now be read. The lit Automatic key still could not: in the industrial look the keys are a light green, and lighting it made the face lighter still. A lit key now darkens a light face (and lightens a dark one, as before) and has an inner outline in the look's highlight. The job line under the title, on the steel casing, is now in the casing's title colour instead of the glass's green. The re-run with this: see the PR.

Not run:
- a human using the recipe page;
- other GUI scales and window sizes: every one gives a GUI at least 320 × 240, so a screen that fits this one fits them;
- the other mods' recipe-viewer panels beside the screens.

## World and event applicability

Not applicable.

## Rollout and open questions

- Only the machine screens are checked. The other custom screens (shops, cooking, journals and the like) are not machines and are not in the test yet.
- The search field is still the vanilla text box.

## History

The recipe page arrived with the industrial forms' recipe locking (#358) in its own dark teal, with vanilla buttons. The fit check follows the logic controller being cut off at a 240-high GUI in the CI shots of [the other machine screens](other-machine-screens.md).
