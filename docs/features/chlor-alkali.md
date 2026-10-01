# Chlorine and lye: PVC and soap

Status: implemented on `feature/chemistry-15` (batch 15). Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner's chemistry list, item 3 (1 October 2026), from the saved backlog in [MACHINE_ROADMAP.md](../MACHINE_ROADMAP.md).
Owner: jimbozoomer-byte
Target milestone and tier: industrial chemistry, after the electrolytic cell and batch 12's synthesis converter
Primary specialty and supported player role: chemistry

## Player experience
- **PVC:**
  - The synthesis converter joins 250 mB of refinery gas (its ethylene) and 250 mB of chlorine into 250 mB of **vinyl chloride**.
  - The polymerization reactor turns 500 mB of vinyl chloride into **four PVC resin**.
  - The metal press makes **two plastic sheets** from each resin, against one per plastic pellet.
- **Soap:** two rotten flesh (rendered fat) boiled in 250 mB of lye in the chemical reactor make **four soap**. Use a bar to wash every status effect off, good or bad, as a bucket of milk does.

## Connections
- Input producer: chlorine and lye (the electrolytic cell), refinery gas (the distillation tower), rotten flesh (zombies).
- Output consumer: plastic sheets (diesel engine, advanced machines); soap (players).
- The electrolytic cell's chlorine and lye now each have a steady use.
- Reachable entry path: every machine already exists.
- Required vs optional: optional.

## Balance and automation
- Volumes shrink: 500 mB of gas → 250 mB of vinyl chloride.
- A plastic sheet from PVC costs 125 mB of vinyl chloride, which is 125 mB of chlorine and 125 mB of refinery gas. From pellets it costs 250 mB of refinery gas.
- Soap equals a milk bucket's effect without the bucket, at two rotten flesh and some lye for four bars.

## Multiplayer and persistence
Items and fluids only. Soap is used server-side.

## Dependencies and assets
None new. Item textures are original (`tools/petro_textures.py`).

## Verification
- `tools/check_mod_data.py` passes.
- Game tests `chlorineBecomesPvc` and `lyeMakesSoapThatWashesEffectsOff` (CI).
- Not run: client play, multiplayer.

## World and event applicability
Not applicable.

## Rollout and open questions
Flue-gas scrubbing with lye needs emissions to scrub first. It is left for a pollution system, if there ever is one.
