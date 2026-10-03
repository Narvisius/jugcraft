# Power lines

Status: implemented (stage 1), not yet played in survival
Proposal issue: design in the project proposal "Power lines" (draft 5)
Owner: Narvisius
Target milestone and tier: machines tier and up (wooden poles early, superconducting lines at the end)
Primary specialty and supported player role: infrastructure; moving power from big plants (nuclear later) to bases far away

## Player experience
Power lines carry energy over long distances without laying cables block by block. A line runs between
**grid-connection boxes**: a box takes energy from the mod's cables on one end, the line carries it, and a box at the
other end pushes it back into cables. In between stand **supports** of one family, joined by real hanging wires.

| Family | Tier I | Tier II | Tier III | Height (m) | Wire reach (blocks) |
| --- | --- | --- | --- | --- | --- |
| Wooden poles | Wooden Pole, 4,096 JE/t | Double-Arm Pole, 8,192 | Spun-Concrete Pole, 16,384 | 12 / 13 / 14 | 32 |
| Steel monopoles | Steel Monopole, 131,072 | Double-Circuit Monopole, 262,144 | Compact Monopole, 524,288 | 30 / 32 / 36 | 64 |
| Lattice towers | Lattice Tower, 2,097,152 | Reconductored (carbon core), 3,145,728 | Monitored, 3,670,016 | 56 | 128 |
| Top tiers | 765 kV UHV Tower, 4,194,304 (65 m) | UHVDC Tower, 5,242,880 (70 m) | Superconducting Pylon, 16,777,216 (20 m) | | 160 / 160 / 64 |

| Box | Size | Capacity (JE/t) | Takes |
| --- | --- | --- | --- |
| Service Pedestal | 1x1, 2 tall | 16,384 | wooden poles (the service drop lands on its weatherhead) |
| Pad-Mount Transformer | 2x2, 2 tall | 524,288 | wooden poles and monopoles (a riser down the pole and a buried cable, no overhead wire) |
| Unit Substation | 10x10 fenced yard (fence line to fence line) | 16,777,216 | every family but UHVDC, on its gantry; holds nitrogen for superconducting lines |
| HVDC Converter Station | 14x14 fenced yard | 5,242,880 | UHVDC only |

A line's rate is its weakest structure's capacity. Scale: 1 JE/t is about 2.3 kW, so a lattice line carries about
5 GW, close to a real 500 kV line's limit.

**Building.** Place a structure from its item: it is built whole (the pole, a tower's four legs, a box's fence and
equipment) and is wired at once to the nearest structure it can be wired to (in reach, compatible, wire path clear,
fewer than four wires). While the item is held, a ghost shows where it would stand and the wire it would get. Lattice
and bigger towers stand their legs on uneven ground (up to 12 blocks between the highest and lowest foot), lifted to
the highest foot, with leg extensions down to the rest. Leaves and solid blocks in the way stop a wire.

**By hand.** Sneak-use one structure, then another, with an empty hand to wire them (or cut the wire if they are
already wired). Use one with an empty hand to read its line: rate, load, structures, wires.

**Upgrading.** Use a better support of the same family on a support: it is swapped in place, keeping its wires,
and the old one is given back.

**Breaking.** Breaking any part breaks the whole structure and drops its item once. Its wires go with it.

**Live wires.** Anything touching a wire while the line carries power is shocked (damage type
`jugcraft:electrocution`) every quarter second: half a heart on a lightly loaded wooden line, about 5 hearts on a fully loaded one, up to 16 hearts
on a fully loaded superconducting line, and set alight above about 80 % load. Creative players are not hurt.

**Superconducting lines** carry power only while a Unit Substation on the line has nitrogen (2 mB/t; the Air
Separation Unit makes 8 mB/t). Pipe nitrogen into the substation's terminal cabinet.

## Connections
- Existing input producer: any generator feeding cables into a box (Gas Turbine, solar, later nuclear).
- Existing output consumer: cables and machines touching the receiving box.
- Technology connection: steel, aluminum cable, plastic, processors, titanium, tungsten, superconducting tape, nitrogen.
- Magic connection: none.
- Reachable entry path: the Wooden Pole and Service Pedestal need only copper cable, iron and terracotta.
- Required vs optional: optional infrastructure; cables still work for short runs.

## Balance and automation
Each tier is crafted from the tier below plus pricier materials (`tools/power_lines.py` CRAFTING); each family's
first tier starts from the family below's top tier. Boxes buffer two ticks of their capacity each way, so energy
never loops back: what cables put in only goes out over the line, and what the line brings only goes out into
cables. A line moves at most its rate per tick in total, shared evenly among the receiving boxes. Networks are
capped at 4,096 structures and only run through loaded chunks.

## Multiplayer and persistence
All of it runs on the server: placing, wiring, upgrading, moving energy and shocks. Wires are saved on both
structures and only count when both ends list each other. A structure broken while the other end's chunk is unloaded
is forgotten by that end within five seconds of it loading. Clients only draw.

## Dependencies and assets
No new dependencies. All art is original, drawn by `tools/power_lines.py` (one 256x256 material sheet and 16x16
item icons); the structures are drawn in code by `client/LineRenderer.java` from the approved mock-ups.

## Verification
- `tools/check_mod_data.py`: LineKind.java matches tools/power_lines.py; the renderer's material regions match the
  sheet; names, loot, recipes (with feature switches) and the damage type exist.
- Game tests (`PowerLineGameTests`): energy crosses a line at no more than its rate; a half-made wire carries nothing;
  a pig on a live wire is shocked; breaking a lattice leg takes the whole tower and drops it once; poles upgrade in
  place keeping their wires; families do not mix and wires have a reach.
- Client game test (`PowerLineClientGameTests`): screenshots of every structure and the placement preview.
- Not yet done: a survival playthrough and a dedicated-server test with two players.

## World and event applicability
Not world-generated. Structures can be placed in any dimension.

## Rollout and open questions
- Are the wire reaches right (32 / 64 / 128 / 160)?
- Should mobs be electrocuted too (currently yes)?
- Later: converter station details, more polish for towers on hills, and the nuclear plants that need this capacity.

## Focused testing
`JUGCRAFT_TEST_FOCUS=powerline ./gradlew build runClientGameTest` runs only this feature's game tests and client
screenshots (every other feature's tests pass at once); several features can be listed with commas. Unset, everything
runs. On the test PC, `13-test-focus.bat powerline` does the same and installs the jar into the Modrinth instance.
