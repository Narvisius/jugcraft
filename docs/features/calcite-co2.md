# Carbon dioxide from calcite and hydrochloric acid

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "hydrogen chloride absorption into aqueous acid, and its consumers (… later consumers remain)". This change adds a consumer of hydrochloric acid.

**This change's choices:** the amounts (one calcite + 500 mB of acid → 250 mB of carbon dioxide, 60 ticks). The owner can review them.

Owner: jimbozoomer-byte
Target milestone and tier: once the player has hydrochloric acid
Primary specialty and supported player role: chemistry; players who want carbon dioxide for methanation without burning coal

## Player experience

The Chemical Reactor dissolves **one calcite** in **500 mB of hydrochloric acid** and gives **250 mB of carbon dioxide** (CaCO₃ + 2 HCl → CaCl₂ + H₂O + CO₂). It takes 60 ticks at the reactor's usual power. The calcite and the acid are spent; the calcium chloride brine is not kept.

The CO₂ feeds the Chemical Infuser's methanation and any other carbon dioxide user, with no coal or kiln.

## Connections

- **Input producers:** calcite (mined, the cave and mountain stone) and hydrochloric acid.
- **Output consumers:** CO₂ methanation, and every carbon dioxide user.
- **Machines:** the existing Chemical Reactor.
- **Alternatives:** CO₂ from coal in the Oxidizer, and from fermentation, stay.

## Balance

- One calcite gives 250 mB of CO₂, counted once.
- Carbon dioxide is not a fuel, so it cannot form a power loop.

## Multiplayer and persistence

- **New recipe ID:** `jugcraft:chemical_reaction/carbon_dioxide_from_calcite`.
- No new items, blocks or saved data.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py` and `python scripts/check_repository.py` passed.

**Server game test for CI:** `CalciteCarbonDioxideGameTests.reactorDissolvesCalciteIntoCarbonDioxide` checks the recipe takes a calcite and 500 mB of hydrochloric acid and gives 250 mB of carbon dioxide.

**Results:** the fork's Build workflow passed on commit 934a7213 ([run 39174746870](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39174746870)): compilation, the headless server game tests (including the narrowed Fermenter check) and the rendered-client shards.

**Not run:** an in-game reactor batch.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
