# Hydrogen from zinc and hydrochloric acid

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "hydrogen chloride absorption into aqueous acid, and its consumers (… later consumers remain)". This change adds a consumer of hydrochloric acid.

**This change's choices:** the amounts (one zinc ingot + 500 mB of acid → 500 mB of hydrogen, 80 ticks). The owner can review them.

Owner: jimbozoomer-byte
Target milestone and tier: once the player has hydrochloric acid (the Chemical Reactor absorbing hydrogen chloride)
Primary specialty and supported player role: chemistry; players who want a little hydrogen without an electrolysis line

## Player experience

The Chemical Reactor dissolves **one zinc ingot** in **500 mB of hydrochloric acid** and gives **500 mB of hydrogen** (Zn + 2 HCl → ZnCl₂ + H₂). It takes 80 ticks at the reactor's usual power. The zinc and the acid are spent; the zinc chloride is not kept.

This makes a small, power-cheap hydrogen source for early fuel cells, the Gas Burning Generator, or chemistry that needs only a little hydrogen. Electrolysis stays the bulk route.

## Connections

- **Input producers:** zinc ingots (the zinc ore), and hydrochloric acid (hydrogen chloride absorbed in water, or the Bulk Chemical Reactor).
- **Output consumers:** everything that takes hydrogen.
- **Machines:** the existing Chemical Reactor.

## Balance

- The hydrogen costs a mined ingot as well as the acid, which itself was made from hydrogen and chlorine. So there is no power loop.
- The power-loop audit does not count recipes that use up items as electricity routes.

## Multiplayer and persistence

- **New recipe ID:** `jugcraft:chemical_reaction/hydrogen_from_zinc`. It loads with the `machines` and `zinc` switches.
- No new items, blocks or saved data.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py` and `python scripts/check_repository.py` passed.

**Server game test for CI:** `ZincHydrogenGameTests.reactorDissolvesZincIntoHydrogen` checks the recipe takes a zinc ingot and 500 mB of hydrochloric acid and gives 500 mB of hydrogen.

**Results:** the fork's Build workflow passed on commit 84244fc2 ([run 39168464393](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39168464393)): compilation, the headless server game tests and the rendered-client shards.

**Not run:** an in-game reactor batch.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
