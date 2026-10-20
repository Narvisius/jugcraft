# Advanced ceramics and precision components

Technology package 1, implemented locally with OpenAI Codex (GPT-6). On 16 October
2026 the owner authorized integration with main and local validation of all seven
packages. The earlier testing hold is superseded. **Keep this work local until the
owner has played it and approves publication.**

Current build, test evidence, draft reconciliation and remaining release checks:
[seven-package integration record](tech-seven-local-integration.md). The detailed
verification lists below are release checklists; only results explicitly recorded
there are claimed complete.

## Player outcome

Make insulating ceramics, ceramic bearings, precision shafts, reusable cutting
inserts and lenses. This is an optional steel/electrical workshop specialty feeding
motors, upgrades, wiring, optical equipment and polymer tooling. Players can trade
any intermediate; there are no personal research or multiplayer-only requirements.

One new machine, the **Precision Finishing Station**, serves ceramics, metal and
optical work. The Pulverizer, Mineral Conditioning Station, Metal Press and shared
Industrial Oven perform preparation, shaping and firing. No parallel ceramic
crusher, press, kiln or polishing machine is introduced.

## Production routes

| Stage | Input | Station | Output and purpose |
|---|---|---|---|
| Clay foundation | Clay ball | Pulverizer | One clay powder; vanilla bricks and the existing clay/steel path remain unchanged |
| Porcelain blend | Four clay powder, two quartz, one borax | Crafting | Four powder blends |
| Preparation | One blend, alumina or zirconia powder; 125 mB water | Mineral Conditioner | One corresponding prepared charge |
| Shaping | One prepared charge | Metal Press | One corresponding unfired blank |
| Firing | One unfired blank | Industrial Oven | One fired blank; 240 ticks / 48 JE/t for porcelain, 320 ticks / 80 JE/t for alumina or zirconia |
| Insulation | One fired porcelain blank | Precision Finisher + abrasive head | Two porcelain insulators |
| Alumina products | One fired alumina blank | Precision Finisher + abrasive head | One finished plate OR one set of bearing balls, selected with the recipe lock |
| Regional zircon supply | Two zircon sand, 500 mB water | Mineral Conditioner | One zircon concentrate, 200 ticks / 48 JE/t |
| Trace zircon supply | 16 sand, 1000 mB water | Mineral Conditioner | One zircon concentrate, 400 ticks / 48 JE/t |
| Zirconia preparation | Two zircon concentrate, 500 mB lye | Mineral Conditioner | One precursor, 240 ticks / 64 JE/t |
| Zirconia powder | One precursor | Industrial Oven | One powder, 320 ticks / 80 JE/t; continue through preparation/shaping/firing |
| Cutting inserts | One fired zirconia blank | Precision Finisher + abrasive head | Two zirconia inserts |
| Shafts | Two steel plates | Crafting | Two shaft blanks; finishing makes one precision shaft per blank |
| Bearings | One precision shaft, steel plate and bearing-ball set | Crafting | One precision bearing |
| Optics | Two borosilicate glass | Crafting | Two optical blanks; finish with polishing head for one lens each |

All finishing jobs take **200 ticks at 48 JE/t**, consume one blank and reserve
125 mB water, then return 125 mB to a separate outlet. Costs are before upgrades.
This water is a cooling loop, not a generated resource. Preparation consumes its
water/reagent without claiming a recoverable product. Quantities are provisional
game balance, not real-world chemistry instructions.

The trace-sand zircon route is intentionally expensive and works in existing worlds.
It neither adds world generation nor depends on the unmerged regional mineral-sand
PR. A later integration may add a better regional producer under the same IDs.

## Immediate consumers

- Porcelain insulators: alternate existing placed Ceramic Insulators (requires
  drones, matching that block's existing feature), Copper Cables and motor assemblies.
- Precision bearings/shafts: two motors per advanced assembly and an alternate Speed
  Upgrade, reducing gear/plate fabrication in exchange for ceramic processing.
- Alumina plates: an alternate existing Efficiency Upgrade, substituting ceramic
  insulation for part of its steel body.
- Lenses: alternate Range Finder and vanilla Spyglass recipes. Advanced electronics
  can add consumers later without introducing a second finishing station.
- Zirconia inserts: alternate recipes for all six existing polymer molds and five
  extrusion dies. These remain reusable tools; inserts are consumed during tool
  construction, not per machine batch.

All original crafting and processing recipes stay available. Finished parts cannot
be recycled back into more feedstock. No new power currency or passive bonus is added;
upgrades and motors retain their existing behavior.

## Machine, UI and persistence

The 3x3x3 Finisher uses the shared form's server-authoritative work lanes, escrow,
inventory/tank transfers, locks, upgrades, save data, recipe UI, JEI adapter and status
messages. One item input, one output, one water inlet, one return outlet and a manual
tool socket. A reusable Diamond Abrasive Head uses diamond, ordinary alumina and iron;
a Felt Polishing Head uses wool, ordinary alumina and iron. Neither tool requires
finished ceramics or the station's outputs. Construction uses earlier steel, motors,
basic circuitry and bricks.

The model uses the existing olive/dark-steel/silver factory kit: supported bench,
feed carriage, guarded rotating head, connected motor, splash tray, sump and coolant
plumbing terminating at the real ports. The existing rotor system spins the head
while working. This is a machining head depiction, not separate animated tool models.

Recipe previews include tool, items, water, outputs and costs. Locks select alumina
plates versus bearing balls and persist through the existing shared system. Automatic
mode remains available. Wrong head, missing feed, absent power or full outputs should
block through the shared lifecycle; this behavior is implemented by reuse but remains
unverified for the new station. Existing forms retain IDs, footprints, slots and ports;
the Oven only gains a capability and recipes.

## Art and reproducibility

Inspected the owner-library guide and catalog, including its technical-glass reference
entry and existing machinery collection. This slice extends the already integrated
Jugcraft machine material kit; no new owner-library file is copied or modified.
New component silhouettes are original editable 16x16 pixel maps in
`tools/item_icons/`, rendered by the existing palette/map system. Source: `tools/ceramics.py`,
`tools/ceramics_models.py`, `tools/ceramics_art.py`. Registry declarations are in
`PetroItems.java` and `IndustrialForms.java`.

Normal data/texture generators include the slice. `python tools/write_ceramics_resources.py`
emits its dependent resources without deleting unrelated generated trees; on Windows,
set the existing `JUGCRAFT_FONT_DIR` to the installed DejaVu TTF directory first.
Generation is not validation. Handbook and Encyclopedia gain a Ceramics and Precision
chapter describing reachable routes, tools, ports and consumers.

## Release verification checklist

Current automated results are in the integration record. These broader release
checks remain subject to manual verification. No remote PR, push or main merge.

- [ ] Build and audit generated data/icons, recipe identities, feature switches and
  full regeneration parity; inspect all new item icons and all machine facings.
- [ ] Exercise every route from survival-obtainable inputs and its immediate consumer;
  verify both alumina choices and mirror-distinct crafting patterns.
- [ ] Missing/wrong tool, low power, wrong fluid, full solid output and full coolant
  output must pause without spending/duplicating inputs or granting products.
- [ ] Lock a job, feed with pipes/hoppers, change the next lock mid-batch, unload/reload,
  save/restart, and break/rebuild during work; confirm refunds, outputs and tool retention.
- [ ] Verify recipe previews include the returned water and retained head, and old
  oven/press recipes and pre-existing factories remain functional.
- [ ] Check small/medium/stress factories, upgrades and full-route energy/material
  budgets with optional integrations both enabled and absent.
- [ ] Owner controls the local playtest; do not launch, drive or close it automatically.
  Two-player manual checks remain a separate release requirement.
