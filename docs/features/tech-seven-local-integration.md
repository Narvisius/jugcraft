# Seven technology packages: local integration, 16 October 2026

Owner request: finish the seven local packages against main and overlapping drafts,
then provide a separate local build for the owner to play. **Do not publish or merge
to main before owner review.** The owner controls the manual playtest.

Base: main `2804ba015b76e99ff341687801cb01fb908a9670`. The original local work is
preserved in `efc7307a4`; integration is on `local/tech-seven-oct16`.

## Included scope

1. Advanced ceramics, shared precision finishing, shafts, bearings and lenses.
2. Chromite/stainless steel and aluminum-magnesium structural components.
3. Lead-acid storage, expanded lithium chemistry, modular flow storage and controls.
4. Wafer processing, advanced chips, local recipe priorities and stock targets.
5. Phosphate processing, crop-specific fertilizers, gypsum and plaster products.
6. Cobalt-catalyzed synthetic fuel and paid process-water/waste recovery.
7. Methane/hydrogen cryogenics, insulated storage, storable propellants and motors
   for the existing rocket payloads. Orbital travel is still future work.

The existing machine IDs, original construction routes, older jobs and inventories
remain supported. Main's machine screen redesign, mineral deposits, trees, guns
and bosses are retained. Only the Finisher and Wet-processing Bench add new
processing machines; the other chains reuse shared machines.

## Reconciliation with overlapping drafts

These are local design reconciliations, **not claims that the draft PRs were merged**.
No contributor branch is rewritten or closed by this task.

| Draft | Decision for this seven-package build |
|---|---|
| #363 copper entry | Apply its copper-conductor entry to the existing Electrolytic Cell. |
| #402 porcelain | One `porcelain_insulator` product from the complete powder/press/oven/finisher chain. Add an optional Main Transformer recipe using it; preserve the old transformer recipe. Do not add a competing short furnace chain. |
| #405 alumina | Use the existing finished `alumina_plate` as the shared ceramic substrate for an additional processor assembly. Keep gold/fibre routes. Do not register a duplicate substrate/blank chain. |
| #401 lead-acid | Keep the complete local lead-electrode/separator/filled-cell chain and `lead_acid_battery_bank` (2,000,000 JE, 1,024 JE/t). Do not add a second 1,600,000-JE lead battery with a conflicting role. |
| #387 acid purity | Concentrate acid first, then purify it in the shared Conditioner; electronic-grade acid and measured spent mixture remain distinct products. No second purification machine or duplicate electronic-grade fluid. |
| #394 phosphates | Keep the complete dissolution/slurry/filter/acid/fertilizer/gypsum route and crop-group behavior. Cola and food-grade purification remain separate future work; industrial acid is not silently treated as food-grade. |
| #403 boron solar | Separate boric-acid/solar specialty, dependent on its own draft chain; not required by this wafer package. Keep the existing advanced solar construction route. |
| #361/#362/#364/#365/#374 | Separate stacked power-plant, ruthenium and large-factory variants. Their cumulative branches also contain unrelated work. They remain drafts for separate review rather than adding duplicate machines to this test. |

Main's regional zircon sand now supplies an efficient zircon concentrate recipe;
the expensive ordinary-sand route remains for established worlds. Zirconia powder
preparation and precursor preparation have distinct recipe IDs, so neither silently
overwrites the other. The phosphate pulverizer uses the single-input recipe codec.

## Validation and handoff

The final normal build passed all **1,758 required server game tests**, including
complete loaded shared-process ledgers, transactional battery limits, persisted
recipe automation and cryogenic retained-tool gating/removal/reload. This run
includes the final regional-zircon export and registration-order corrections.
The optional-integrations-absent run also passed all **1,758 required server game tests**. The final focused client run passed after the recipe-screen changes; inputs, tool requirements and outputs are visible ahead of detailed guidance.
All 58 Python tests passed, including new cyclic-fuel-cost and recipe export
regressions. Item-map lint passed 228 maps (27 existing warnings). The final full
resource audit passed 2,913 material IDs, 8,516 block/item models, 3,709 quad parts
and 171 combined models. Repository structure and local documentation links pass.
The focused client fixture loaded successfully, placed 19 stations, inspected six
machine menus and two recipe screens, and saved a separate owner-controlled world.
Screenshots prompted a final recipe-screen readability improvement: inputs and
outputs precede detailed cost/port/automation guidance; help remains on tooltips.
Shared feed labels and compact machine taglines now describe the expanded jobs. Historical green CI on main or a contributor fork does not validate this
combined branch. Human survival balance and two-player testing remain unclaimed.

Corrections during integration: phosphate pulverizer input schema; unique zirconia
recipe IDs and complete recipe exports; registration/catalog order; main's screen
APIs; automation card encyclopedia entry; retained-tool/energy ledger test setup;
old recipe tests now locate their original jobs explicitly and allow additional
shared jobs. Fuel auditing rejects circular candidate feeds rather than treating
them as free inputs, and accounts for the 4:1 gas/cryogenic-liquid volume ratio.
The base main commit was rechecked against GitHub before handoff.

AI assistance: OpenAI Codex (GPT-6), under direct owner authorization. No new
dependencies or platform upgrades. Minecraft 26.3 / Fabric Loader 0.19.5 / Java 25.


## Local delivery

`Jugcraft-Tech-Seven-Oct16.mrpack` contains the compiled mod, reviewed dependency
pins and **Jugcraft Seven Tech Packages**, a separate creative showroom with 19
stations, sample supplies, charged storage and an assembled flow battery. It is
intended for the owner to launch and control. No test harness is shipped in the
playable mod. Exact hashes and the source commit accompany the pack.

Automatic Modrinth import could not complete: the native window capture returned
`window capture timed out: timed out waiting on channel` after a refreshed-window
retry. The standard `.mrpack` is the direct import fallback. No existing profile or
world was changed and nothing was pushed, merged, or published remotely.

Still before release: owner visual/interface feedback, complete survival-chain
balance, representative factory load, and human two-player testing. The detailed
feature checklists include broader cases than these automated regressions and are
not all marked complete by this handoff.
