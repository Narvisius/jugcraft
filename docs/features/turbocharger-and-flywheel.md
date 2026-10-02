# Turbocharger and flywheel

Status: merged in #75 (batch 19). Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 1 October 2026: "Then lets do 7-9", item 7 of the options list ("turbocharger and flywheel"), from the saved backlog in [MACHINE_ROADMAP.md](../MACHINE_ROADMAP.md).
Owner: jimbozoomer-byte
Target milestone and tier: late kinetic power, with the advanced engine (batch 10)
Primary specialty and supported player role: power

## Player experience
- **Turbocharger:** an item made from steel plates, gaskets, steel gears, a titanium ingot and a steel fluid pipe.
  - It goes in the advanced engine's new slot. The engine also gains a second, 4-bucket tank for **coolant water**, which its intercooler needs.
  - With a turbocharger fitted and water in, the engine gives up to **1,536 KE/t** instead of 1,024. It also gets **10% more** out of each mB of fuel, and uses 2 mB of water a tick while it runs.
  - Without water, or without the turbocharger, it runs as before.
- **Flywheel:** a heavy steel wheel between two bearings, one block.
  - Shafts into any face but its front spin it up, 2,048 KE/t at most. It holds up to **2,000,000 KE**.
  - Its front shaft, pointing the way you looked when placing it, drives what it faces from that store, up to 2,048 KE/t. A line keeps turning through gaps in a bursty source, such as a hand crank, an engine between fuel deliveries or a steam engine refuelling.
  - Bearing friction takes a ten-thousandth of what it holds each tick, so a full wheel left alone runs down over several minutes.
  - Right-click it to read how much it holds. The wheel turns while it holds anything.

## Connections
- Input producer:
  - turbocharger: gaskets (batch 14), titanium, steel;
  - flywheel: any kinetic source.
- Output consumer: any kinetic consumer (machines, dynamos, conveyors).
- Technology connection: kinetic power, petrochemistry (fuel) and water.
- Magic connection: none.
- Reachable entry path:
  - the flywheel needs only steel and iron shafts;
  - the turbocharger needs the advanced engine.
- Required vs optional: optional.

## Balance and automation
- **Turbocharger:** half as much power again from one engine, at the cost of a fitted part and a water supply. The 10% fuel gain is the reward for tuning; water is not used up for nothing (it is the coolant).
- **Flywheel:** it stores and returns rotation and never makes it. Friction (at least 1 KE a tick while it spins) means a loop through a flywheel always loses.
- At 2,048 KE/t, one flywheel smooths the output of one turbocharged engine.

## Multiplayer and persistence
- Server-side. The flywheel saves what it holds. The engine saves its slot and tanks like every machine.
- **Old saves:** an advanced engine placed before this change gets an empty slot and an empty coolant tank, and keeps its fuel.

## Dependencies and assets
None new. The flywheel model (`tools/kinetic_models.py`, which spins with the kinetic rotor renderer) and the turbocharger texture (`tools/petro_textures.py`) are original.

## Verification
- `tools/check_mod_data.py` passes.
- Game test `turbochargerAndFlywheel`:
  - the engine's slot takes only a turbocharger;
  - a turbocharged engine with water gives a flywheel more than an untuned engine's maximum in 20 ticks, and uses coolant;
  - a charged flywheel drives a magnet dynamo;
  - an idle flywheel loses a little to friction.
- Not run: client play (the spinning wheel, the engine screen with its new slot and tank), two players.

## World and event applicability
Not applicable.

## Rollout and open questions
- A visible turbocharger on the engine model when one is fitted (with the machine screen redesign).
- Bigger flywheels (multi-block) if one wheel's 2,000,000 KE is too little.
