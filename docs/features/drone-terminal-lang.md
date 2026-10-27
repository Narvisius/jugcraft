# The drone depot terminal in the language file

Status: implemented (pending CI and review). Follows [the other machine screens in their looks](other-machine-screens.md), which put the terminal in the drone look with its words still written in the code.
Proposal issue: none. The owner, 26 October 2026, asked for the suggested polish items in order ("then do 2-9 in order"). The fourth was "drone screens translatable".

Owner: jimbozoomer-byte
Target milestone and tier: the Drone Depot, from the first depot
Primary specialty and supported player role: builders running drone depots, and players in other languages

## Player experience

The Drone Depot Terminal's screen reads every word from the language file, so a translation can change all of it. That covers:
- the title and the tabs;
- every line on the OVERVIEW, FLEET, JOBS and POWER tabs, and the warnings;
- the mode key.

Before, only its TOWER key could be translated. The Drone Tower's screen already read its words from the language file.

In English it reads as before, in the drone look's capitals, with three improvements:
- **Tiers by name:** the FLEET tab names each tier by its drone's item name ("T1 Courier Quad Drone"), not the code's name ("T1 COURIER QUAD").
- **Materials by name:** the JOBS tab's missing materials go by their item names ("12× Stone"), not their ids ("12× stone").
- **The mode key** shows PERSONAL or PARTY from the same words the depot's chat messages use. Before the terminal first linked, it read MODE.

## Connections

- `tools/drones.py` `DRONE_LANG`: the `screen.jugcraft.terminal.*` strings, generated into `assets/jugcraft/lang/en_us.json`.
- `DroneTerminalScreen`: `text(key, values...)` fills in a string. `itemName(id)` names an item by its id, keeping the id if no such item is known.
- The tier names are the drones' item names (`item.jugcraft.drone_t1` to `drone_t9`, `creative_drone`). The mode names are `message.jugcraft.drone.mode_name.*`.

## Balance and automation

None. Display only.

## Multiplayer and persistence

Unchanged. The screen still shows the terminal's live depot view.

## Dependencies and assets

None. 47 new language strings.

## Verification

- `DroneClientGameTests`: the terminal's OVERVIEW and FLEET tabs, photographed.
- `ScreenFitClientGameTests`: the terminal at the smallest GUI.

### Results

Local, without Gradle or a game: the Java syntax parse, `tools/generate_material_data.py` (the new strings land in the language file) and the repository checks; see the PR.

CI: see the PR.

Not run: a translated language file.

## World and event applicability

Everywhere drone depots are.

## Rollout and open questions

None.

## History

Before this, the terminal's words were English literals in `DroneTerminalScreen`.
