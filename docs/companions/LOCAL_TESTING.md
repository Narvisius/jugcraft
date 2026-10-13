# Local companion integration

Branch: `peepo-companion`, updated with fork main `8ca8aee5` on 2026-10-07.

Build: `./build-local.ps1 -Tasks assemble` (compilation and packaging only; no tests run).
Jar: `build/libs/jugcraft-0.1.0-alpha.jar`.
Use Minecraft 26.3, Java 25, Fabric Loader 0.19.5 or later and the pinned dependencies from `distribution/frameworks.lock.json` (see `docs/DISTRIBUTION.md`).
This jar replaces the regular Jugcraft jar and includes Peepo/Jughead. Remove the separate Peepo Companion jar from the test instance to avoid duplicate registrations. Use a separate creative test world.

## Manual wheel check

1. Obtain `/give @s peepo_companion:generator_wheel` and `/give @s peepo_companion:peepo_summoner` (or `jughead_summoner`). The wheel is also in Functional Blocks. Craft one with rows `PBP / CMC / PBP`: P = any plank, B = bronze ingot, C = copper cable, M = Machine Casing (the frame ingredient used by the other generators).
2. Place the wheel with a clear 2-wide, 2-high area and clear ground in front for approach/dismount. Spawn a companion nearby, within 16 blocks.
3. Connect either outward side of the bottom two blocks to Jugcraft cable and a battery or a machine that accepts power. Front, back and top are not electrical outputs. Both side ports share one buffer and a total active push budget of 64 JE/t.
4. The companion should approach, enter, run, and transfer up to 64 JE/t from its reserve into the wheel. The rotor should turn. Right-click any wheel part with an empty hand to open the generator GUI and read its 32,000 JE buffer, export rate, companion and reserve; check the receiving battery to see delivered power. A buffer can remain nearly empty while power is immediately exported.
5. Disconnect the load: the wheel should fill and release the companion. Reconnect or drain the load to let work resume.
6. Feed the companion once to tame it, then Shift-right-click to inspect energy and commands. Choose Work to enable wheel work after taming. Off the wheel it gains 2 JE/t while walking or standing; food adds the existing bonus. Exhaustion blocks work until 80% energy. Dropped food and hand feeding remain supported.
7. Try both companion variants, each wheel orientation, both side ports, breaking an occupied wheel, and saving/reloading when convenient.

No in-game verification or automated test run was performed for this integration. Companion beds are now available; see BEDS.md. Stools and shared chair/perch support are available; see SEATING.md. Workstation assignment is not included yet.

Fork update performed locally with OpenAI Codex (GPT-6). Companion registrations and wheel rendering retained alongside upstream framework/Jade entries. No automated tests or gameplay checks were run for this update.

The Companion Planner now configures one home and four work targets; see `docs/features/companion-assignments.md`.
