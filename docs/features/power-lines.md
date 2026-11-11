# Power lines

Status: implemented (stage 1; round 2: the supports, turns and the switchyard's line gantry, then the superconducting line and the boxes, 20 October 2026), not yet played in survival
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
| Lattice towers | 345 kV Lattice Tower, 1,048,576 (31 m) | Lattice Tower (500 kV), 2,097,152 | Reconductored (carbon core), 3,145,728; tier IV Monitored, 3,670,016 | 43 | 128 |
| Top tiers | 765 kV UHV Tower, 4,194,304 (53 m) | UHVDC Tower, 5,242,880 (71 m) | Superconducting Pylon, 16,777,216 (20 m) | | 160 / 160 / 64 |

| Box | Size | Capacity (JE/t) | Takes |
| --- | --- | --- | --- |
| Service Pedestal | 1x1, 2 tall | 16,384 | wooden poles (a pole-mount transformer on the pole, the triplex drop to its weatherhead) |
| Pad-Mount Transformer | 2x2, 2 tall | 524,288 | wooden poles and monopoles (a riser down the pole and a buried cable, no overhead wire) |
| Unit Substation | 10x10 fenced yard (fence line to fence line) | 16,777,216 | every family but UHVDC, on its gantry; superconducting lines on its cryogenic terminations; holds nitrogen for them |
| HVDC Converter Station | 14x14 fenced yard | 5,242,880 | UHVDC only |
| Switchyard Line Gantry | part of the medium plant's switchyard (no item) | 16,777,216 | lattice, monopole and UHV lines: the plant's power goes out over the line strung from it |

A line's rate is its weakest structure's capacity. Scale: 1 JE/t is about 2.3 kW, so a lattice line carries about
5 GW, close to a real 500 kV line's limit.

**Building.** Place a structure from its item: it is built whole (the pole, a tower's four legs, a box's fence and
equipment) and is wired at once to the nearest structure it can be wired to (in reach, compatible, wire path clear,
fewer than four wires). While the item is held, a see-through copy of the structure shows where it would stand, turned along its line, with the
wire it would get (red if it won't fit; client/MbsPreview.java). Lattice
and bigger towers stand their legs on uneven ground (up to 12 blocks between the highest and lowest foot), lifted to
the highest foot, with leg extensions down to the rest. Leaves and solid blocks in the way stop a wire.

**Round 2 supports (claude/jugcraft-power-lines-redesign.md, approved 19 October 2026).** Every support but the
superconducting pylon is drawn from the approved generators (`tools/line_supports.py`, exported by
`tools/line_models.py` to `assets/jugcraft/power_lines`): a support turns to its line (one wire: toward it; two: the
mean travel direction, so its arms bisect the corner; the old rule ran a right-angle corner's arms along the line). On a
straight line or a turn of up to 15 degrees it is its tangent (suspension) body; at a line end, a sharper turn, a tap
(three or more wires) or, but for wooden poles, a span into a box, it is its family's angle (dead-end) body: each span
gets its own strain assembly (wooden poles: a double dead-end arm and a down guy) turned to it, and the phases are
jumpered across (a bundle's loops as a ribbon so its sub-conductors never cross; round the peak at the phase
clearance; on a pilot string when long; nested round a wooden pole). Wires hang every sub-conductor of the bundle
(twin, triple, six, eight) with spacers or spacer-dampers at the thirds; reconductored and monitored lines sag less;
shield wires run peak to peak. A tower's legs (its collision frame) turn with it and move when its wires change. Each
structure's plates carry its own number. Near a structure everything is drawn; from 48 blocks small parts are left out
and faces are flat colours; from 128 only the main members; off screen nothing.

**Part 2: the superconducting line and the boxes (approved 19 October 2026, built 20 October).** The superconducting
pylon is the new slim white design: two cables, each resting in a sleeve under its swept arm, hanging nearly straight
between pylons under a messenger on droppers every 4 m, a glowing inspection stripe along each cable while the line
carries power. The pylon always stands as its one body, turned to bisect its line's turn, and each cradle's sleeve is
bent round the turn (never tighter than a 2.2 m radius, longer on sharp turns) so the cable leaves each mouth along its
span. The boxes are drawn from their approved models and **face their first line in 90-degree steps** (the box facing
rule): the gantry of a Unit Substation or Converter Station toward it, a pedestal's weatherhead or a pad-mount's back
toward its pole; a Unit Substation whose first line is superconducting turns its front, where the cryogenic
terminations stand, to it. Their fences and equipment (frame blocks) turn with them. Each line on a gantry gets its own
strain assemblies turned to it (up to 60 degrees off straight out) with slack jumpers down to the disconnect (or, at a
converter station, gathered to the smoothing reactor). A superconducting line lands on the substation's two cryogenic
termination vessels, their cable-entry sleeves swivelled to it, the cables leaving the yard through fence penetrations.
A wooden pole or monopole wired to a pedestal or pad-mount treats that wire as its service, not part of its line (it
does not turn the pole or make it an angle pole): a wooden pole carries a pole-mount transformer on the box's side, its
lead off the nearest phase, and the drop to the pedestal is triplex; a pole wired to a pad-mount carries a riser with
three leads from the line. The pedestal's meter shows the kWh it has delivered into cables, its voltage and current.

**The plant's line 1.** The medium plant's switchyard has its outgoing dead-end gantry for line 1 (the approved
H-frame, its strain strings and slack jumpers down to the line traps); a Switchyard Line Gantry under its beam anchors
the line: build 345 kV lattice towers (or any lattice, monopole or UHV line) out from it, and the plant's power goes out
over the line (into cables at the line entry while no line is strung).

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
No new dependencies. All art is original. Round 2 supports: `tools/line_models.py` runs the approved generators
(`tools/line_supports.py`, `tools/transformer_yard.py`) and writes each body, footing and strain assembly in three levels
of detail (`assets/jugcraft/power_lines/models/*.bin`), their data (`supports.json`: conductor points by phase, shield
points, hinges, assembly ends and jumper pads, legs, plates, wire sizes) and one texture
(`textures/entity/line_models.png`); `client/LineModels.java`, `client/LineSupports.java` and `client/LineMesh.java`
draw them, `powerline/LineGeometry.java` (both sides) turns them and picks their bodies. Part 2 adds the
superconducting pylon (its sleeves drawn by the game), the four boxes (`supports.json` "boxes": strain assembly, hinges,
jumpers, cryogenic terminations, frame cells, the meter's display) and each pole's transformer and riser models, each
side. The switchyard's line gantry is drawn by its site; `tools/power_lines.py`'s stage 1 material sheet is no longer
drawn by the structures.

## Verification
- `tools/check_mod_data.py`: LineKind.java matches tools/power_lines.py (kinds, tiers, model keys); the renderer's
  material regions match the sheet; every support's models and data exist; the line gantry's attach points are where
  the switchyard draws line 1's dead-ends; names, loot, recipes (with feature switches) and the damage type exist.
- Game tests (`PowerLineGameTests`): energy crosses a line at no more than its rate; a half-made wire carries nothing;
  a pig on a live wire is shocked; breaking a lattice leg takes the whole tower and drops it once; poles upgrade in
  place keeping their wires; families do not mix and wires have a reach; towers turn to their line with their legs
  (twin-bundle phases, shield wires); corners take angle bodies with a strain assembly per span; the line gantry takes
  a 345 kV line; boxes face their first line (a substation turns, its frame blocks with it, its strain assemblies
  straight out); a superconducting line lands on the substation's terminations (no messenger on that span); a pole's
  service to a pedestal and a pad-mount leaves it a tangent pole with the transformer and the riser on opposite sides.
- Client game test (`PowerLineClientGameTests`): screenshots of every structure and the placement preview.
- Not yet done: a survival playthrough and a dedicated-server test with two players.

## World and event applicability
Not world-generated. Structures can be placed in any dimension.

## Rollout and open questions
- Are the wire reaches right (32 / 64 / 128 / 160)?
- Should mobs be electrocuted too (currently yes)?
- Later: more polish for towers on hills, and the nuclear plants that need this capacity.
- A cable from a cryogenic termination crosses the substation's front fence low: it goes through a penetration plate.

## Focused testing
`JUGCRAFT_TEST_FOCUS=powerline ./gradlew build runClientGameTest` runs only this feature's game tests and client
screenshots (every other feature's tests pass at once); several features can be listed with commas. Unset, everything
runs. On the test PC, `13-test-focus.bat powerline` does the same and installs the jar into the Modrinth instance.
