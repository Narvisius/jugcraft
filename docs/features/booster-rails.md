# Booster rails: rocket-boosted minecarts

Status: batch 42 is implemented on `feature/booster-rails-42`, stacked on `feature/rocket-launcher-41`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, approved "booster rails" in the rocketry list, and asked for them to be started while CI ran.
Owner: jimbozoomer-byte
Target milestone and tier: electronics tier, after the batch 38 rocket workshop.
Primary specialty and supported player role: transport (minecart lines).

## Player experience
- **Booster rail** (six from six steel ingots, a rocket nozzle and redstone): a straight rail with hazard-striped thruster blocks between the ties.
  - It is powered by redstone like a powered rail, and passes power along a line of booster rails the same way. Its nozzles glow while powered.
  - Unpowered, it is an ordinary rail. It does not brake carts as an unpowered powered rail does.
- **Fuel:** solid propellant. Use it on the rail, or feed it from a hopper. Each propellant gives 8 boosts, and a rail holds up to 64.
- **Boost:** a cart that rolls on to a powered, loaded booster rail, or stands on one, is kicked to full speed and held there for 10 seconds (about 80 blocks).
  - The boost works up slopes too, and the cart trails flame and smoke.
  - A cart standing still is sent uphill on a slope, or else away from a solid block at one end, as a powered rail would. Failing both, it goes south or east.
- **Speed limit:** minecarts keep their own maximum speed (8 blocks a second on land). A booster rail is not a way to go faster than that. Its value is that a long line, and especially a long climb, needs one booster rail where it would need many powered rails.

## Connections
- Input producer: the rocket workshop (rocket nozzles, solid propellant), steel.
- Output consumer: minecart transport, including the hopper and chest carts used by item lines.
- Technology connection: batch 38 propellant, vanilla rails, powered-rail power. Magic connection: none.
- Required vs optional: optional.

## Balance and automation
- One solid propellant is 8 boosts, about 640 blocks of full-speed travel.
- Powered rails stay the cheap choice for short flat stretches; boosters win on long runs and climbs.
- Nothing is produced, so there is no positive-gain loop.

## Multiplayer and persistence
- Server-authoritative: boosts are applied by the server to minecart entities.
- Fuel and charges are saved in the rail. Boosts in progress are not saved: one running when the server stops just ends.

## Dependencies and assets
- No new dependencies.
- Original 16x16 rail textures (off and on) are drawn in `tools/rocketry.py`. The models use vanilla's rail templates.
- It uses vanilla firework launch sounds and flame and smoke particles.

## Verification
- `tools/check_mod_data.py` checks against `tools/rocketry.py`: the boost length, the boosts per propellant and the maximum charges.
- Game test `boosterRailLaunchesACart` (CI): a powered booster rail fed one propellant through its slot starts a standing minecart off at speed, uses one boost and keeps the cart boosted.
- Not tested in CI:
  - climbing a long slope;
  - loading the rail by hand;
  - power passing along a line of booster rails;
  - the textures in game.

## World and event applicability
Not applicable.

## Rollout and open questions
- A faster rail line (above the vanilla minecart limit) would need minecart physics changes and is not attempted.
- Next rocketry batch: liquid fuels (batch 43, [liquid-fuels.md](liquid-fuels.md)).
