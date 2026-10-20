# Titanium rocket casings

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "Add aluminum's affordable bulk … roles; titanium advanced process/precision equipment and **vehicle components**; …" This change gives titanium its first vehicle component.

**This change's choices:** the yield (two titanium plates → two rocket casings, against three steel plates → one). The owner can review it.

Owner: jimbozoomer-byte
Target milestone and tier: rocketry, after titanium (the Kroll process)
Primary specialty and supported player role: metals and rocketry; players who want more casings from less metal

## Player experience

The Rocket Workshop now also makes **two rocket casings from two titanium plates** (160 ticks). The steel recipe (three steel plates → one casing) stays.

Casings go into every rocket motor, propellant tank (kerosene, liquid oxygen and the others), the delivery rocket and the HE rocket, so titanium makes those cheaper in metal.

## Connections

- **Input producer:** titanium plates (Kroll-process ingots pressed in the Metal Press).
- **Output consumers:** everything that takes a rocket casing.
- **Machines:** the existing Rocket Workshop.
- **Optional:** steel casings stay.

## Balance

- One casing is a plate of titanium against three of steel. Titanium costs a long chlorine and arc-furnace chain, so steel stays the cheap early casing.
- No metal is created: a casing is not taken apart again.

## Multiplayer and persistence

- **New recipe ID:** `jugcraft:rocket_assembly/rocket_casing_from_titanium`. It loads with the `machines` and `titanium` switches.
- No new items, blocks or saved data.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py` (including the metal-conservation audit) and `python scripts/check_repository.py` passed.

**Server game test for CI:** `TitaniumCasingGameTests.workshopMakesCasingsFromTitanium` checks that the Rocket Workshop makes two rocket casings from two titanium plates.

**Results:** the fork's Build workflow passed on commit f485bac3 ([run 39171276025](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39171276025)): compilation, the headless server game tests and the rendered-client shards.

**Not run:** an in-game workshop batch.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
