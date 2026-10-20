# Coking with gas recovery

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "Add optional CO2 capture/release to industrial carbonate cement processing and optional Coke Oven gas/condensate recovery." This change delivers the gas half of the Coke Oven recovery. The condensate (coal tar) remains.

**This change's choices:**

- the gas is kept as methane, the fuel part of real coke-oven gas;
- the amounts: four coal → four coke + 200 mB of methane, 400 ticks.

The owner can review both.

Owner: jimbozoomer-byte
Target milestone and tier: once the player has a Chemical Reactor
Primary specialty and supported player role: metals and power; steelmakers who want fuel gas from their coke

## Player experience

Coal can now be baked into coke in two ways:

- **Coke Oven:** as before. Free and unpowered, 600 ticks a coal, and the gas is lost.
- **Chemical Reactor:** four coal → four coke, plus 200 mB of methane in its output tank. It takes 400 ticks at the reactor's usual 96 JE/t; when the tank is full the reactor waits.

The methane feeds the Gas Burning Generator and every other methane user.

## Connections

- **Input producer:** coal.
- **Output consumers:**
  - coke: the Steel Foundry and every other coke use;
  - methane: generators, rocket fuel and chemistry.
- **Machines:** the existing Chemical Reactor.
- **Optional:** the Coke Oven stays.

## Balance

- **Energy per batch:** the reactor spends 96 JE/t × 400 ticks = 38,400 JE, and keeps 200 mB of methane as fuel.
- **No power loop:** the coal is used up, so this is a primary-fuel route, not an electricity one, and the power-loop audit treats it that way.
- **Accounting:** the gas is declared as released from the coal (`"source": 200`) and counted once.

## Multiplayer and persistence

- **New recipe ID:** `jugcraft:chemical_reaction/coke_with_gas_recovery`.
- No new items, blocks or saved data.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py` and `python scripts/check_repository.py` passed.

**Server game test for CI:** `CokeGasGameTests.reactorCokesCoalAndKeepsTheGas` checks the recipe takes four coal and gives four coke and 200 mB of methane.

**Results:** the fork's Build workflow passed on commit cc090bad ([run 39169804033](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39169804033), second attempt; the first attempt's client shard 5 stopped during setup, 23 seconds in, before any test ran): compilation, the headless server game tests and the rendered-client shards.

**Not run:** an in-game reactor batch.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
