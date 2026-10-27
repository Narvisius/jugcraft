# The benches in the workshop look

Status: merged (PR #471). Follows [the other machine screens in their looks](other-machine-screens.md) and [the screen fit check](screen-fit-check.md), whose PR it is stacked on.
Proposal issue: none. The owner, 23 October 2026, picked suggestion 4: "now do 4, restyle the crafting benches". The suggestion was the cooking pot, carving, lampwright bench, cave workshop, farm stand and blueprint table in the workshop look.

Owner: jimbozoomer-byte
Target milestone and tier: the bench screens, from the first workshop onwards
Primary specialty and supported player role: cooks, carvers, lampwrights, farm stand keepers and cave surveyors

## Player experience

Five bench screens, each in its own style before (vanilla grey, a shop's wooden board, a teal map, a dark pumpkin panel), now share the workshop machines' look. They have the brass casing, the orange glass, the status lamp by the title, and keys in the look that light up when they are on:

| Screen | What changed |
| --- | --- |
| Cooking Pot | The ingredients, arrow, flame and meals sit on the glass, over the inventory tray. The lamp is lit while the pot has heat, and green while a helper speeds it up. "Needs heat below" or "+50% speed" shows under the meals. |
| Lampwright's Bench | The specimen and work slots, with the study and Radiance bars, sit on the glass. Study, Kindle, Infuse and Channel are keys; Study is lit while it cancels your own running study. A dimmed key still says on hover what it needs. The lamp is lit while a study runs. |
| Farm Stand | The crates, with their chalked slates, are on the glass. Whose stand it is shows in a chip by the title. Buy, Set ("Price this crate" on hover; it was Price, which does not fit its key) and the price steps are keys; the crate being priced has its key lit and its crate framed. Your Jugs (and the price being set) show at the foot of the glass. |
| Cave survey desk | The six expeditions are keys in a "Choose an expedition" pane. The survey's state, progress bar and result (the coordinates, in green) are on a map glass under it, with the help in a strip. The lamp is lit while it surveys, green when it found a cave and red when it could not. A chip shows the progress or "Found". Its words, written in English in the code before, are now translatable. |
| Carving Knife | The face sits in its rind on the glass, and the actual-size preview has its own pane. The tool in use, Mirror and the candle are lit keys (the tool was marked with an outline). The hint, or what went wrong, is in a help strip. The lamp is lit while there are strokes for Done to send. |

Every screen keeps its functions: the same slots, buttons and button ids, the same labels (the carving tests click them by name), and the same data.

The **blueprint table** keeps its own look: a blueprint sheet clipped to a steel drawing board, with folder tabs, rubber stamps and a turning preview. It is a design tool with a finished look of its own, and the workshop look would only flatten it. See open questions on its size.

## Connections

- `ScreenKit`: two additions, both used by the benches: an `arrow` that shows on any glass (its unlit part a faint copy of its colour), and a `help` strip in a given colour (for warnings).
- `tools/gui_textures.py` `SCREENS`: the five benches map to the workshop look. Their new words are in its screen strings (`screen.jugcraft.cooking_pot.assisted`, `screen.jugcraft.cave_survey.*`).
- Each bench implements `ScreenKit.Fitted`, and the [fit check](screen-fit-check.md) now opens all five at the smallest GUI (320 × 240) too.

## Balance and automation

None. Display only.

## Multiplayer and persistence

Unchanged. The menus, slots, synced data, button ids and payloads are as before.

## Dependencies and assets

No new dependencies or assets.

## Verification

Client game tests that show the benches:
- `ScreenFitClientGameTests`: all five at 320 × 240, with screenshots (`jugcraft_fit_cooking_pot` and so on);
- `CarvingClientGameTests` and `HalloweenClientGameTests`: carving by hand, the templates and the candle, by clicking the keys by name;
- `PeepoCompanionClientTests`: the cooking pot with a helper.

### Results

Local, without Gradle or a game:
- `python tools/generate_material_data.py` added the new strings and the five benches to the screens map, and nothing else of this change's.
- The other checks and the Java syntax parse: see the PR.

CI, the [run on 6f9b99bca](https://github.com/jimbozoomer-byte/jugcraft/pull/471): the mod, the repository checks and 15 of the 16 client shards passed, among them `ScreenFitClientGameTests` (all five benches fit 320 × 240), `CarvingClientGameTests`, `HalloweenClientGameTests` and the Peepo cooking tests. The 16th (the nuclear tests, which these screens do not touch) was cancelled when the next commit was pushed, so the run's summary check reads failed. The shots showed two labels cut short, fixed in the next commit:
- the farm stand owner's Price keys read "Pr…" (Price is 26 pixels on a 24-pixel key); they now read Set, with "Price this crate" on hover;
- the carving knife's "Mirror: Off" was cut; Brush now takes 46 pixels of its row and Mirror the rest.

The re-run: see the PR.

Not run:
- a human using each bench;
- a second player at the farm stand.

## World and event applicability

The carving knife is seasonal (Halloween); its screen looks the same in any season.

## Rollout and open questions

- The blueprint table is 410 to 440 wide and 250 high: taller than the 240-high GUI of a 16:9 window at the smallest size, and wider than a 4:3 window's 320. It is not in the fit check. Fitting it means scaling it with its text box and turning preview, or laying it out again. That is its own change. Done since: it scales to fit and is in the fit check ([blueprint-table-fit.md](blueprint-table-fit.md)).
- The other non-machine screens (shops, bank, exchange, alchemy, journals) keep their own looks.

## History

Before this:
- the cooking pot and the lampwright's bench were vanilla grey panels with grey slots;
- the farm stand used the shops' wooden board;
- the cave survey desk was a teal map with vanilla buttons;
- the carving knife was a dark pumpkin panel with vanilla buttons.
