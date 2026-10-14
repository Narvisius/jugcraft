# Biogas power: the Bio Generator burns the Digester's biogas (factory package 2)

Status: implemented (CI green on the contributor's fork; pending review).
This branch joins two stacks:
- the [Bio Generator](industrial-bio-generator.md) branch (`claude/bio-generator`);
- the [Anaerobic Digester](industrial-anaerobic-digester.md) stack, up to the [PVC route](industrial-pvc-hcl-recovery.md).

Proposal issue: none. TODO items:
- "separate lower-output Bio-Generator for cleaned biogas and bioethanol";
- the Digester's "direct cleaned-biogas Bio-Generator use".

**From the owner's plan:** the 64 JE/t output for biogas, which the Bio Generator's record already proposed.

**This change's choice, for review:** the burn value per millibucket.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, biological power
Primary specialty and supported player role: power generation from farms and waste

## Player experience

- The **Bio Generator** now burns the Anaerobic Digester's **biogas**, as well as bioethanol, through the same fuel inlet low on its right side.
- Biogas gives **268 JE a millibucket at 64 JE/t**, so a bucket lasts about 210 seconds and gives 268,000 JE.
- Bioethanol is unchanged: 192 JE a millibucket at 96 JE/t.
- **Methane:** pure methane, upgraded from biogas in the Infuser, still belongs to the Gas Burning Generator at 448 JE/mB and 256 JE/t. Raw biogas is the lower-output, no-upgrade option.
- The Engineer's Handbook page says so.

## Balance

- **Burn value.** Biogas is the plan's 60/40 methane/carbon dioxide mixture. 268 JE/mB is its 60% methane share of methane's 448 JE/mB, rounded down. The carbon dioxide burns for nothing.
  - Burning a bucket directly gives 268,000 JE.
  - Upgrading it first takes 1,600 mB of hydrogen and four Infuser batches, and gives 448,000 JE in the Gas Burning Generator. Upgrading pays only when hydrogen is cheap.
- **No loop.** Biogas is made only from consumed waste (the Digester), so the audit treats it as a primary fuel: electricity alone cannot make it.
  - The [refined audit](industrial-anaerobic-digester.md#balance-and-automation) now covers bioethanol as well.
  - The Bio Generator branch's separate primary-fuel exemption in `tools/check_mod_data.py` was removed as redundant.
- **Output.** 64 JE/t, below bioethanol's 96 and the gas generator's 128 and 256, as the plan proposes for a lower-output generator.

## Merging notes

This branch merges `claude/bio-generator` into the Digester stack:
- `IndustrialForms.java`, `tools/industrial_forms.py`, `FluidFuels.java`, the test registry and the docs were resolved to keep every form.
- `machine_rotor_quads.json` was regenerated.

If the Bio Generator's own PR merges first, this PR's diff shrinks to the biogas fuel.

## Multiplayer and persistence

- No new registrations: biogas and the Bio Generator already exist on their branches.
- The fuel is a form property, so saved generators keep their tanks.

## Verification

Locally:
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py` (the refined audit) and `python scripts/check_repository.py`: passed.
- `javac` reported no syntax errors in the changed Java files.

For CI, server game tests:
- `BioGeneratorGameTests.biogasBurnsAt64JEATick`: every millibucket burnt is 268 JE made or in hand, at 64 JE a tick.
- `BioGeneratorGameTests.itsFuelAndItsBill`: two fuels, biogas at 268 and 64.
- All the Bio Generator's and the Digester stack's earlier tests run together on this branch.

### Results

- **Run [38442417215](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38442417215) on the contributor's fork (commit 46a1b307): green.** Everything compiled; the data checks passed; every server game test passed, `BioGeneratorGameTests.biogasBurnsAt64JEATick` among them; the build without optional integrations and all five client shards passed. Later commits on the branch change only documentation.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The burn value awaits the owner's review.
- Still to come: a separate "cleaned biogas" (cleanup before burning) if the owner wants raw and cleaned biogas to differ.
