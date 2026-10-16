# The other machine screens in their looks

Status: implemented (pending CI and review). Builds on [machine screens: five looks by tech level](machine-screens.md) (PR #366), which it extends.
Proposal issue: none. The owner, 14 October 2026, after the machine screens: "yes restyle the custom screens make them better". This followed the suggested next step: give the other custom screens the look of their tech level, with the same status lamp, plain status and help.

Owner: jimbozoomer-byte
Target milestone and tier: the eight machine screens that are not machine or form screens
Primary specialty and supported player role: players using prospectors, rocket pads, storage tanks, logic controllers, reactors and drones

## Player experience

Eight screens that each had their own style now share the machine screens' looks and parts:

| Screen | Look | What changed |
| --- | --- | --- |
| Geo-Resonance Prospector | workshop | Brass body and orange glass from the workshop machines. The signal count is in a chip, and each reading's depth is a tag (brighter nearer the surface). The resonance gauge sits beside a help strip: "Strongest first. More bars, more ore. Readings are approximate." The warm-up and scan line stay. |
| Rocket pad | industrial | The plain grey screen is gone. It now has the industrial body, pane and tray, an arrow from the cargo to the rocket (lit once a rocket is loaded), and faint rocket and plan hints in the empty slots. Hovering a slot says what goes there. Launch is a key with a tooltip. The last result sits in a chip by the title, and the lamp is green after a launch and red when one failed. |
| Storage tank | industrial | The cutaway tank picture stays, in its own pane. Its readouts are grouped into Contents (with a fill bar), Flow and Outlet (with a lamp). The rating is a chip, and the outlet hint is a help strip. |
| Logic controller | advanced | A heading row (#, SENSOR, LEVEL, TARGET, SET) and hologram keys with tooltips. Rules that are on stand out, and an ON action key is lit. The channel strip shows each reading under its swatch (hover one for its name), plus a help strip. |
| Reactor console | advanced | The owner-approved layout is unchanged. It now has the hologram body and panes, a status lamp by the title, and the status in a chip. |
| Remote shutdown panel | advanced | As the console: same layout, the advanced body and panes, a lamp and a status chip. |
| Drone depot terminal | drone | The advanced shapes in the drone tower's graphite and red. The tabs and the MODE and TOWER buttons are now keys in that look; the open tab is lit. A lamp shows the state: green with drones up, amber when something is missing, red on low power. |
| Drone tower | drone | The same look. The UPGRADE key is lit only when it can be pressed. The lamp is amber while building and green when the next tier is ready. Module counts that are short are amber; full ones are green. |

Every screen keeps its functions: the same buttons (button ids, payloads), the same slots at the same places, and the same data.

## Connections

- The looks come from `assets/jugcraft/gui/machine_themes.json`, which now also holds each look's frame colours and a "screens" map from screen to look. Moving a screen to another look means moving it in `tools/gui_textures.py` `SCREENS`.
- `client/ScreenKit` draws the parts: the body (any size), panes with headings, the title strip with its lamp, status chips, bars, rows, the inventory tray and slots, help strips, and `TextButton` keys.
- The drone look is the advanced look's shapes in the tower's palette. It exists only for screens; no machine uses it, and no machine background is drawn for it.

## Balance and automation

None. Display only.

## Multiplayer and persistence

Unchanged. The menus, slots, synced data, button ids and network payloads are exactly as before. The screens read the same synced block entities.

## Dependencies and assets

- No new dependencies.
- Two new tiles, drawn by `tools/gui_textures.py` `body_tile`: `textures/gui/screen_workshop.png`, the brass blotches on a wrapping grid, and `screen_industrial.png`, brushed steel. The other looks' bodies are drawn with fills.
- New lang strings for the headings, slot help, tooltips and help lines (`screen.jugcraft.prospector.*`, `rocket_pad.slot.*`, `logic_controller.*`, `tank.heading.*`).
- Two new 8×8 hint icons, ROCKET and PLAN.

## Verification

Client game tests take screenshots of:
- the prospector (`JugcraftClientGameTests`);
- the rocket pad and the logic controller (new, in `JugcraftClientGameTests`);
- the storage tank screens and the reactor console (`NuclearClientGameTests`);
- the remote shutdown panel with control transferred to it (new, in `NuclearClientGameTests`);
- the drone tower (`TowerClientGameTests`, `GuideScreenshotGameTests`);
- the depot terminal (`DroneClientGameTests`, `GuideScreenshotGameTests`).

### Results

Local, without Gradle or a game:
- `python tools/generate_material_data.py` regenerated `gui/machine_themes.json` (the drone theme, frame colours, the screens map) and the new lang strings. A second run changed nothing.
- The tiles were checked by tiling them 3×2: no visible seams.
- `python tools/check_mod_data.py`: PASS (2648 material IDs, data files and recipe audit). `python scripts/check_repository.py`: PASS. `python -m unittest discover -s scripts/tests`: OK. Tree-sitter Java syntax parse, 624 files: 0 errors.

CI, [run 38657990009](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38657990009) on 1e3b48489: green. The mod compiled, the repository checks passed, and all five client shards passed (JugcraftClientGameTests, GuideScreenshotGameTests, JugcraftServerClientGameTests, NuclearClientGameTests, DroneClientGameTests and TowerClientGameTests). Every screen above was in its shots (480×270 thumbnails of an 854×480 window, GUI 427×240), and they showed these faults, fixed in the next commit:
- The logic controller was 252 high, taller than the 240 window, so its title and help strip were cut off. It is now 236: rows 18 apart, a lower channel strip, and the help strip under it.
- The rocket pad's arrow from the cargo to the rocket was drawn in the slot shadow colour on the dark pane, so it could not be seen. It is now faint, and lit once a rocket is loaded.
- The remote shutdown panel's "Reset from the console" ran under the SCRAM button (a fault from before the restyle). The trip text now wraps to the left of the button.
- The depot terminal's FLEET tab with ten tiers drew the last row on the pane's edge. The rows are now 10 apart.
- The drone tower's hint and legend were cut short with an ellipsis (also from before). They now wrap.
- The workshop glass's band ran across the middle of the prospector's readings. It is now the rule under the heading.

CI, [run 38659890877](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38659890877) on 37b86deed with those fixes: green. The shots showed each fix working. They also showed one more fault from before the restyle: on the remote shutdown panel, "This panel (control room locked out)" ran past its pane. It now wraps above the transfer switch. The re-run with that fix: see the PR.

Not run:
- a human using each screen;
- other GUI scales.

## World and event applicability

Not applicable.

## Rollout and open questions

- The drone screens' strings are still written in English in the code, as they were.
- The remaining custom screens (shops, cooking, journals and the like) are not machines and keep their own looks.

## History

The eight screens each had their own style:
- the prospector: brass and amber;
- the rocket pad: vanilla grey;
- the storage tank, reactor console and remote shutdown panel: the nuclear teal;
- the logic controller: a teal bezel with vanilla buttons;
- the drone screens: graphite and red.
