# Joined tanks, glass tanks and tank gauges

Status: merged in #76 (batch 20). Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 1 October 2026: "Then lets do 7-9". This is item 8 of the options list ("tank gauges and joined tanks"), from the backlog's "Fluid gauge and tank walls" and the glass tanks promised with glass chemistry (batch 16).
Owner: jimbozoomer-byte
Target milestone and tier: fluid logistics, after borosilicate glass (batch 16)
Primary specialty and supported player role: logistics

## Player experience
- **Joined tanks:** tinplate tanks and glass tanks touching face to face join into **one tank of one fluid**, up to 64 of them.
  - The group fills from its lowest tanks and drains from its highest, like one tall tank.
  - Pipes, pumps, buckets, right-click readouts and comparators all see the whole group.
  - A group only takes the fluid it already holds.
- **Glass tank:** four borosilicate glass in a steel frame. It holds 16 buckets like the tinplate tank, joins with it, keeps its fluid when broken, and **shows the fluid inside**: a column tinted the fluid's colour, filled to its level.
- **Tank gauge:** a sight-glass panel, two from a recipe, hung on the side of anything that holds fluid. That includes a tank group, a steel tank, a gas holder, a flow battery or any machine with tanks.
  - Its glass shows how full the block behind it is, in eighths, updated twice a second.
  - Right-click it to read the fluid and amount. A comparator next to it gives a signal like the tank's.

## Connections
- Input producer: borosilicate glass (batch 16), steel plates, a comparator.
- Output consumer: any fluid store or machine.
- Technology connection: fluid logistics.
- Magic connection: none.
- Reachable entry path: alloy smelter (borosilicate glass), steel.
- Required vs optional: optional; separate tanks work as before.

## Balance and automation
- Joining adds no capacity: a group holds exactly what its tanks hold.
- The gauge reads only; it holds and moves nothing.

## Multiplayer and persistence
- Server-authoritative. Each tank still saves its own share, so old worlds and broken tanks need no migration. A broken tank drops with what was in it, and the group re-forms around the rest.
- Glass tanks send their contents to clients when they change, so the fluid can be drawn. Tinplate tanks do not.
- Tanks that held different fluids before they were joined keep them; the group then takes only the lowest tank's fluid.

## Dependencies and assets
- No new dependencies.
- Textures and models are original (`tools/tank_display.py`).
- The glass tank's fluid is drawn by `client/GlassTankRenderer`, using fluid gauge colours rather than the fluids' own textures.

## Verification
- `tools/check_mod_data.py` passes.
- Game test `joinedTanksAndGauge`:
  - two stacked tinplate tanks and a glass tank take 40 buckets of water, filling the lowest first, and refuse lava;
  - 10 buckets drain from the top first;
  - a gauge on the group reads five eighths.
- Not run: client play (the glass rendering and its translucency, the gauge model in the world), two players.

## World and event applicability
Not applicable.

## Rollout and open questions
- The glass tank's fluid is a tinted column, not the fluid's animated texture. 26.3's fluid sprite lookup changed, and the texture can follow once a client is available to test it.
- Bigger single-block tank walls (multi-block "tank walls" from the backlog) are not built; joined tanks cover the same need.
