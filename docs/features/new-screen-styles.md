# The workshop, aircraft and furniture screens in the looks

Status: implemented (pending CI and review). Follows [the benches in the workshop look](bench-screens.md) and [the screen fit check](screen-fit-check.md).
Proposal issue: none. The owner, 26 October 2026, picked the first of the suggested polish items: "do 1, restyle the new screens". The suggestion was the screens the Jimtafacts workshops, spellbooks and aircraft/furniture batch (PR #475) brought in, put into the machine looks and added to the fit check.

Owner: jimbozoomer-byte
Target milestone and tier: the Jimtafacts workshop machines, the aircraft and the artisan's workstation
Primary specialty and supported player role: engineers, pilots and furniture makers

## Player experience

The machine screens PR #475 brought in each had a look of their own: the source mod's grey GUI images, a dark aircraft panel, a green terminal, a grey reactor grid, a cream drafting board. They now use the machine looks the other machine screens use. Each has its look's casing, the glass panes, a status lamp by the title and keys in the look that light up when they are on:

| Screen | Look | What changed |
| --- | --- | --- |
| Lathe | industrial | The blank, tool and shavings slots and the blank's five-segment profile are on the glass, with a cut key over each segment. The KE, the state and Finish are in a strip under the inventory. The lamp is green when it is ready or done, amber when it waits on something. |
| Transformer | electric | Its readings on the glass, and the three modes as keys, the current one lit. The lamp is lit while it holds energy. |
| Fluid service (pumps, routers and distillers) | industrial | The fluid, its tank gauges (bars) and the power are on the glass. Under the inventory are the rate and mode keys (pumps and routers) or the state (distillers). The lamp is green while it works, amber when something stops it. |
| Workshop engines (stirling, combustion, electric, dynamo and the rest) | workshop | The stored JE and what the engine burns or turns are on the glass; its state is in a strip under the inventory. The lamp is green while it runs, amber while it cools after overheating, unlit with no redstone. |
| Workshop chute | workshop | A key for each face (the input face dimmed and marked IN, open outputs lit) and for collection; the stored JE on the glass. |
| Robot programming table and equipment charger | workshop | The programs are keys showing their cards, the chosen one lit. The state and stored JE are in a strip under the inventory, with a JE gauge (programming) or a charge bar (charging). A "?" key holds the help. |
| Compact (component) reactor | advanced | The component grid on the glass, the columns not yet unlocked shaded, a heat gauge beside it (green below half, red from half up), and the start/stop key, lit while it runs. The lamp is green while it runs, red on a fault. |
| Computer | electric | The program and the output each on a pane of glass, the actions as keys under them, a RUNNING/READY chip by the title. The device's own storage shows only when it has some. |
| Gate parameters | electric | Its keys take the look, lit while a condition or an output is on. |
| Heat and kinetic generators, civil workshops, matter workshops | electric (already) | Their vanilla buttons are keys in the look (the steam generator's superheated mode and a running civil workshop are lit). |
| Aircraft | industrial | The source layout's sockets on the glass, an empty socket marked with what it takes, and the fuel and engine in a line under them, with the socket under the mouse named at its right. The lamp is green while the engine runs and amber with no fuel. |
| Artisan's workstation | workshop | The name and presets, the previews and the part's measurements each on their glass, the cost in a chip by the title, and the features that are on (seat, storage, light, particles, sound) lit. |

Every screen keeps its functions: the same slots, button ids, payloads, tooltips and data.

These are unchanged:
- the **scribing desk** keeps the source's scroll forge (a spellbook forge in a purple atlas);
- the **personal safe** and the **shops** (barter station, energy vendor) keep their own panels for now: they take the trade look, and join the fit check, in [trade-look.md](trade-look.md);
- the source GUI images the restyled screens drew (`textures/gui/jimtafacts/lathe.png` and the like) stay in the mod; the lathe still draws the workpiece's profile from its images.

## Size

Every one of the 15 screens fits the smallest GUI (320 × 240). Four were bigger: the computer (360 × 288), the compact reactor (232 × 244), the gate parameters (320 × 246) and the larger aircraft (the cargo airship and warship are wider than 320). They lay themselves out at their own size and scale down to fit a smaller window, slots and keys with them, through a new base class, `FittedContainerScreen`. The aircraft screen did this already, by hand; it now uses the class. The artisan's workstation scales itself already.

## Connections

- `ScreenKit`: `frame` draws a container screen's casing and inventory tray from its menu (where its player inventory starts); `slots` frames every slot in use; `inventoryTop` finds where the inventory starts.
- `FittedContainerScreen`: a container screen laid out at its own size and scaled to fit, mouse included. It draws its panel, slots, widgets and tooltip in one scaled pass.
- `tools/gui_textures.py` `SCREENS`: the new screens map to their looks (`workshop_engine`, `workshop_chute`, `workshop_table` and `artisan` to workshop; `industrial_lathe`, `industrial_fluid_service` and `aircraft` to industrial; `industrial_transformer` and `computer` to electric; `compact_reactor` to advanced). The heat, civil, matter and gate screens keep the logic controller's electric look.
- Each screen implements `ScreenKit.Fitted`, and the [fit check](screen-fit-check.md) opens all 15 at the smallest GUI. That includes the programming table, whose menu chooses its layout from the block it stands on: the test places that block above the player first. It opens every aircraft layout.

## Balance and automation

None. Display only.

## Multiplayer and persistence

Unchanged. The menus, slots, synced data, button ids and payloads are as before.

## Dependencies and assets

No new dependencies or assets.

## Verification

Client game tests that show these screens:
- `ScreenFitClientGameTests`: all 15 at 320 × 240, every aircraft layout and both table kinds, with screenshots (`jugcraft_fit_lathe`, `jugcraft_fit_workshop_engine`, `jugcraft_fit_programming_table`, `jugcraft_fit_compact_reactor`, `jugcraft_fit_computer`, `jugcraft_fit_gate_parameters`, `jugcraft_fit_aircraft_cargo_airship`, `jugcraft_fit_artisan`);
- the Jimtafacts and aircraft tests PR #475 brought in, which open these screens and use them.

### Results

Local, without Gradle or a game:
- `python tools/generate_material_data.py` added the new screens to the themes file's screens map, and nothing else of this change's.
- The other checks and the Java syntax parse: see the PR.

CI: see the PR.

Not run:
- a human using each screen.

## World and event applicability

Everywhere these machines and vehicles are.

## Rollout and open questions

- The personal safe and shops take the trade look, and join the fit check, in [trade-look.md](trade-look.md).
- The blueprint table (410 to 440 × 250) is still not in the fit check: the polish item after this one.

## History

Before this, the Jimtafacts workshop screens drew the source mod's GUI images with vanilla buttons; the aircraft screen was a dark slate panel; the computer a green terminal; the compact reactor a grey grid; the artisan's workstation a cream board with vanilla buttons.
