# Cement with CO2 capture

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "Add optional CO2 capture/release to industrial carbonate cement processing and optional Coke Oven gas/condensate recovery. … account for material, energy and recovered/released amounts once." This change delivers the cement half.

**This change's choices:** the amounts (four cement mix → four cement + 250 mB of carbon dioxide, 200 ticks). The owner can review them.

Owner: jimbozoomer-byte
Target milestone and tier: once the player has a Chemical Reactor
Primary specialty and supported player role: construction and chemistry; builders who also want carbon dioxide for methanation or other chemistry

## Player experience

Cement mix (calcite, clay and sand) can now be burnt into cement in two ways:

- **Furnace or blast furnace:** as before. The carbon dioxide is lost.
- **Chemical Reactor:** four cement mix → four cement, plus 250 mB of carbon dioxide in its output tank (200 ticks, the reactor's usual power). When the tank is full, the reactor waits until it is emptied.

## Connections

- **Input producer:** cement mix (crafted from calcite, clay and sand, or from a bone block).
- **Output consumers:**
  - cement: concrete, and every existing cement use;
  - carbon dioxide: the Chemical Infuser's CO2 methanation and any other CO2 user.
- **Machines:** the existing Chemical Reactor.
- **Optional:** the furnace route stays.

## Balance and accounting

- One calcite makes four cement mix and releases one 250 mB of CO2, counted once.
- The recipe declares the gas as released from its items (`"source": 250`), so the fluid audit knows where it came from.
- Capture costs the reactor's power and time, while the furnace route is cheaper but loses the gas.

## Multiplayer and persistence

- **New recipe ID:** `jugcraft:chemical_reaction/cement_with_co2_capture`.
- No new items, blocks or saved data.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py` and `python scripts/check_repository.py` passed.

**Server game test for CI:** `CementCaptureGameTests.reactorBurnsCementAndKeepsTheCarbonDioxide` checks the recipe takes four cement mix and gives four cement and 250 mB of carbon dioxide.

**Results:** the fork's Build workflow passed on commit 4ad437f9 ([run 39174745063](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39174745063), second attempt): compilation, the headless server game tests (including the narrowed Fermenter check) and the rendered-client shards. In the first attempt client shard 2, whose classes do not touch this recipe, hit the 60-minute job limit; its re-run passed.

**Not run:** an in-game reactor batch.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
