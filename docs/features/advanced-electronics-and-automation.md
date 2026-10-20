# Advanced electronics and local automation — package 4

Technology package 4, implemented locally with OpenAI Codex (GPT-6). On 16 October
2026 the owner authorized integration with main and local validation of all seven
packages. The earlier testing hold is superseded. **Keep this work local until the
owner has played it and approves publication.**

Current build, test evidence, draft reconciliation and remaining release checks:
[seven-package integration record](tech-seven-local-integration.md). The detailed
verification lists below are release checklists; only results explicitly recorded
there are claimed complete.

## Player progression

This is an optional advanced specialty. Existing silicon, boule, wafer, microchip,
basic/advanced circuit, processor, speed and efficiency recipes remain available.
There are no new ores or world-generation dependencies in this package.

| Stage | Equipment | Batch |
|---|---|---|
| Purification | Mineral Conditioner | 4 Silicon + 500 mB HCl + 500 mB water → 2 High-purity Silicon + 1000 mB spent mixture |
| Crystal pulling | Existing Arc Furnace | 4 High-purity Silicon + 1 Phosphate → 1 High-purity Boule |
| Slicing | Precision Finisher, retained Abrasive Head | Boule + 250 mB water → 8 Sliced Process Wafers + same water returned |
| Polishing | Precision Finisher, retained Polishing Head | Sliced wafer + 125 mB water → Polished wafer + same water returned |
| Cleaning | New Wet-processing Bench | Polished wafer + 125 mB electronic sulfuric acid + 125 mB peroxide → Clean wafer + 250 mB spent mixture |
| Coating | Same Wet-processing Bench | Clean wafer + 125 mB photoresist → Coated wafer + 125 mB spent mixture |
| Exposure | Existing Lithography Station | Coated wafer + retained General-chip Pattern Plate → Exposed wafer |
| Development | Same Wet-processing Bench | Exposed wafer + 125 mB TMAH + 125 mB water → Developed wafer + 250 mB spent mixture |
| Selective etching | Same Wet-processing Bench | Developed wafer + 125 mB HF + 125 mB water → Etched wafer + 250 mB spent mixture |
| Bonding | Existing Circuit Assembler | Etched wafer + 2 copper wire + solder ingot → 4 General-purpose Advanced Chips |

The first bench uses ordinary steel, tanks, motors, basic circuits, cable and
casing. Its 3×3×3 cabinet has one input/output drawer, two 2000 mB input tanks,
one 2000 mB waste tank and shared upgrade slots. A supported work tray, pump
coupling animation and connected service runs use the current olive/silver kit.
The two Finisher tools, early lenses, gold ingot and basic circuit provide the
reusable Pattern Plate before any advanced chip exists. No wear or consumable
replacement schedule is introduced.

The bench's shared recipe picker, exact previews, lock and output reservation
explain the job and its blockages. Unused reagent tanks must be empty when
switching wet stages. Store the spent mixture in existing tanks; a blocked waste
outlet stops new batches. Existing paid batches keep their inputs and outputs.

## Reagent integration

All quantities below are simplified game receipts. They are not a real chemical
composition model; the single spent mixture abstracts acids, rinse and resist waste.

- **Electronic sulfuric acid:** Conditioner takes 1000 mB concentrated sulfuric acid
  and 250 mB water, returning 750 mB electronic-grade acid and 500 mB spent mixture.
  Purity remains distinct from concentrated acid. Conditioning water is internal
  to this job; no new purified-water currency is required.
- **Peroxide:** Bulk Chemical Reactor takes 750 mB water and 250 mB oxygen into
  1000 mB Oxygenated Process Water. A distinct powered Separator capability makes
  250 mB peroxide and 750 mB returned water. Separator outlet 0 carries peroxide,
  outlet 1 water, outlet 2 is unused. No old outlet moves or changes ownership.
- **Novolac:** Bulk Reactor takes one Coke and 250 mB bioethanol into two resin.
- **DNQ:** Bulk Reactor takes one Coke and 250 mB nitric acid into two additive.
- **Photoresist:** The existing compact two-solid Chemical Reactor blends one
  resin, one additive and 500 mB bioethanol into 500 mB photoresist.
- **TMAH:** Bulk Reactor takes one ammonium nitrate, 250 mB bioethanol and 250 mB
  lye into two Developer Charges; each dissolves with 1000 mB water into 1000 mB
  developer. These are abstract prepared doses, not literal buckets of solid.

Recipes retain finite time/power costs. Wet jobs use 160 ticks at base 64 JE/t;
exposure uses the existing station's 192 JE/t for 200 ticks. Other new form jobs
use base 64 JE/t except peroxide at 96 JE/t. Existing profiles/upgrades apply to
forms; the handbook/recipe preview derives their effective costs. Recovery of
spent mixture belongs to package 6; there is no vent, free disposal or recycling
receipt for it here. Silane/deposition and regional higher-yield silicon remain
future work.

## Chips and automation

The general chip has three immediate consumers:

- Chip + bearing + shaft + two steel plates → **two existing Speed Upgrades**.
- Chip + two alumina plates + copper wire → **two existing Efficiency Upgrades**.
- Chip + two basic circuits + polymer housing → **one Automation Card**.

Speed/efficiency retain their existing four-card caps and formulas. Speed shortens
time by `1 + 0.5 × cards` and raises draw and energy per item. Efficiency multiplies
draw by `0.8 ^ cards`, bounded at four. Old upgrade recipes remain available.
Automation uses an existing formed-machine upgrade slot; multiple cards do not
stack. Compact processors reject this card and keep their existing controls.

In a formed processor's Recipes screen, **Priority** cycles 0–3 and **Stock** cycles
Off/16/32/64 items, or Off/250/500/1000/2000 mB for fluid-only jobs. Up to eight
recipes per controller can hold non-default settings. The UI disables adding a
ninth. Reset both fields to free one slot, or Reset rules to clear all, including
preferences for recipes removed by a datapack. Recipe Lock is independent.

Priority applies to automatic selection only. Among equal preferences, existing
recipe priority/id ordering is preserved. A blocked high-priority job does not
prevent a lower-priority compatible job from running. A lock still allows only
its selected job.

Stock accounting uses the first item output, or the first fluid output when a
recipe has no item results. It counts matching products in this controller's
output buffers, matching products promised by every current lane, and the whole
next batch. It never scans external chests, adjacent tanks, other grids or chunks.
A target below one batch deliberately prevents starting. Exporting output can
allow another batch. This is a local buffer target, not a lifetime production
quota or network-wide warehouse inventory target.

Settings are saved by recipe ID, synced through the existing authoritative menu,
and checked against its server catalog, permissions and town protection. Stale
catalogs must be reopened. Buttons are rate limited to one accepted action per
tick. Changing rules, removing the card or clearing settings never cancels or
rewrites an escrowed batch. Removing the card disables rules but preserves them
for reinsertion; a fresh world/machine starts with no rules.

## Compatibility and assets

The Lithography Station appends one manual pattern slot after its three released
slots. Old input/output indexes, tanks and ports remain unchanged; old saves load
with that new slot empty. No face exposes the pattern to pipes. Only new exposure
requires the plate; old acid-etch recipes continue without one. The existing fluid
recipe codec carries the retained tool through synchronization and JEI displays
it. The plate is never consumed or damaged.

The owner library catalog was consulted, including `originals/Blocks/logic_circuit.png`,
`material_circuit_board.png` and the circuit fabricator reference. New code-native
icons extend Jugcraft's existing editable 16×16 material-map system; the bench
extends its current factory geometry/material kit. No owner-library originals
were modified or executed. Six fluids use the canonical animated-fluid writer.
The Handbook, Encyclopedia and recipe-view resources include this chain.

## Release verification checklist

- Compile both optional-integration configurations; resource, recipe, item-map,
  registration, feature-gating and port/model audits.
- Complete survival chain, recipe costs/yields and old electronics routes.
- Old lithography saves; pattern insertion/shift-click/removal, missing-tool pause,
  no consumption, pipe isolation and no progress reuse across different inputs.
- Recipe lock versus priorities, targets below batch size, exact thresholds,
  multi-lane reservations, duplicate output products, export/resume, blocked waste,
  cancel/refund, unloading, restarting, reload and stale menu handling.
- Two-player permission checks/synchronization; card removal/reinsertion and rule
  cap/reset; representative factory performance and absence of remote scans.
- Owner-controlled local playtest of visuals, screen layout at different scales,
  hover explanations, animations, sound inherited from existing machinery, ports
  and full progression. The owner controls the separate manual playtest.

Later extensions: remote/coordinated automation, external storage targets,
larger advanced equipment where justified, silane/deposition and spent-wafer
recovery (package 6). These are not claimed complete by this package.
