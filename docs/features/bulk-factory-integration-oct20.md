# Bulk-factory integration — 20 October 2026

The owner selected the next bulk-factory group for conflict resolution, combined tests and merge.
Base: c27afcd98417258beb32d73bd92acd2ec4ab3c90.
Integration and repairs: OpenAI Codex (GPT-6), under direct owner authorization.
Original contributor code, models, textures and attribution remain preserved.

## Included outcome

- Combined Cycle Power Plant and its gas-fuel ladder.
- Optional ruthenium recovery and a reusable methanation catalyst bed.
- Bulk Fermenter and Large Bio Generator.
- Bulk Electrolytic Separator, together with its stacked Large Gas Burning Generator,
  Bulk Chemical Infuser and Bulk Chemical Oxidizer support commits.
- Contributor improvements to fermentation, contact acid, leaching throughput,
  tank labels, milestones and bulk stations in the recipe viewer.

All five selected source heads are ancestors of the separator branch. Merging that
stack once preserves their history without replaying overlapping changes.

## Compatibility repairs

- Keep main's seven-package recipes, cobalt catalyst, vanadium-concentrate route,
  chemistry items, machine registrations and generated-resource ownership.
- Bulk processors retain the same current recipes and tank-role aliases as their
  small counterparts, including electronics peroxide, crop mash and synthetic fuel.
  The bulk Infuser drains only water, preserving other secondary products.
- Keep the separator's no-card policy. Its four lanes pay the bulk energy profile;
  audits include recipe-specific energy overrides and the highest generator fuel value.
- Set the Combined Cycle methane return to 528 JE/mB while preserving its 2,048 JE/t
  output. The proposed 560 return exceeded the conservative 535.5 JE/mB cost of fully
  carded bulk ruthenium CO methanation with CO treated as free. Phase conversion of
  existing liquid methane is audited separately from synthesizing methane.
- Preserve Clean Biogas as a Large Bio Generator fuel, alongside raw biogas and
  bioethanol, with a runtime fuel-accounting regression.
- Add all seven machine UI taglines to the existing interface generator. Reuse the
  current shared olive/gunmetal/silver palette and native recipe picker/lock/previews.
- Extend the shared runtime recipe ledger to all four bulk processors, checking
  exact paid inputs, outputs, energy, catalysts and emissions.

## Source heads

| PR | Integrated head |
| --- | --- |
| #361 | 7bc5e144698155c5b85f4a948643ce35b120a5c8 |
| #362 | 4ffd6da0823ab14aa2b1424a44c9ad2332ec0f61 |
| #364 | 47ee989948b81f7710c0b859bbc369e0f1616dec |
| #365 | dc03bef77bc2cc9878febc91591b9b3e54682233 |
| #374 | 8165f4d8ff9a02a725c0c00e108fffec151fdd14 |

## Validation

Combined validation is pending. Exact final commands, outcomes and CI run links will
be recorded on the integration PR before merge. Historical green contributor runs
are not evidence for this combined build. No human playtest, two-account multiplayer
certification or factory-scale performance measurement is claimed. Owner worlds and
launcher profiles are untouched; no platform or dependency upgrade is included.
