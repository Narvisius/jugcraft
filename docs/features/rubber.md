# Rubber and polymers

Status: implemented on `feature/chemistry-14` (batch 14). Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner's chemistry list, item 2 ("lets do 1-5 of the chemistry list then merge all of it", 1 October 2026), from the saved backlog in [MACHINE_ROADMAP.md](../MACHINE_ROADMAP.md).
Owner: jimbozoomer-byte
Target milestone and tier: petrochemistry, after plastics
Primary specialty and supported player role: chemistry

## Player experience
- **Butadiene:** the chemical reactor cracks a bucket of naphtha into 500 mB of **butadiene**, a gas.
- **Synthetic rubber:** the polymerization reactor turns 500 mB of butadiene into **four rubber**.
- **Gaskets:** a steel plate faced with rubber (rubber, plate, rubber) cuts into **four gaskets**.
- **Uses:**
  - Rubber and string make **two belts**; leather and string make one.
  - Two steel plates and a gasket make **four steel fluid pipes**, with no bronze pipe needed.
  - Gaskets and rubber hoses are also parts for the turbocharger (a later batch).

## Connections
- Input producer: naphtha from the distillation tower; steel plates.
- Output consumer: belts (kinetic power), steel pipes (fluid logistics), later engine parts.
- Reachable entry path: the chemical reactor and polymerization reactor already exist, so there is no circular unlock.
- Required vs optional: optional; the old recipes stay.

## Balance and automation
- Cracking halves the volume: 1,000 mB of naphtha → 500 mB of butadiene → 4 rubber, so a bucket of naphtha is 4 rubber.
- A belt from rubber costs 2 rubber and a string, against a leather and a string for one.
- The pipe recipe uses no more steel than before per pipe (2 plates → 4 pipes, against 2 plates and a bronze pipe → 3).
- The data audit checks every recipe for metal created from nothing.

## Multiplayer and persistence
Items and fluids only; no new saved state.

## Dependencies and assets
None new. Item textures are original (`tools/petro_textures.py`).

## Verification
- `tools/check_mod_data.py` passes.
- Game test `naphthaBecomesRubber` (CI).
- Not run: client play, multiplayer.

## World and event applicability
Not applicable.

## Rollout and open questions
Natural rubber (rubber trees for the farming branch) and tires are possible later additions.
