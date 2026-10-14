# Shared concentrators, scrubbers, filter presses and ovens

Owner-directed local work, 13 October 2026, implemented with OpenAI Codex. Base checkpoint: `699ce6c39`. No platform or dependency change, publication or main push.

## Four recommended machines

| Player-facing machine | Preserved registry ID | Supported processes |
| --- | --- | --- |
| Concentrator | `brine_concentrator` | Coastal seawater concentration; sulfuric acid concentration |
| Gas Scrubber | `gas_cleaning_rack` | Synthesis-gas cleaning/separation; biogas cleaning; fermentation CO2 cleaning |
| Filter Press | `aluminate_filter_press` | Aluminate recovery; digestate separation |
| Industrial Oven | `alumina_calciner` | Alumina calcination; magnesium chloride drying; PTFE seal, fitting and liner sintering |

Extend the existing family/capability recipe lookup. Recipe IDs, ingredients, yields, processing times, per-tick energy and paid recycling remain unchanged. JEI lists the shared recipes at the recommended machines; Encyclopedia recipe pages recommend their consolidated equivalents. Each machine can be reused between jobs or duplicated for continuous production.

These are shared game-machine roles. This checkpoint does not add corrosion/temperature upgrade gates or new materials; existing inputs still gate the advanced processes. The four existing construction recipes are retained. No new resource framework or machine registry is needed.

## Existing factories and saves

All nine old block/item IDs, models, drops and construction recipes remain registered. The five now-optional specialists (Acid Concentrator, Biogas Cleaner, Digestate Separator, Chloride Dryer, PTFE Sintering Oven) continue their original recipes and retain their old layouts. They are omitted from the main creative machinery list, but remain craftable and documented for existing layouts. Nothing is removed from a world or automatically replaced. Consolidation reduces the types needed in a new factory, not the number of registered compatibility IDs.

The four recommended forms retain their dimensions, part ordering, inventories, upgrade slots, existing tank role names and existing port coordinates. The Oven adds one solution input tank and one lower rear inlet at column 1, row 3, layer 0, connected to the chamber by a modeled pipe. Its old `recovered_water` output remains saved under that role; adding an input does not reinterpret old contents. Item slot positions are unchanged. Running batches still reference the same recipe IDs and input/output-relative indices.

Tank labels for the four shared machines are now generic and specific to that form, so a Scrubber's second outlet is not always mislabeled Hydrogen. Save keys are unchanged. Digestate water leaves the Filter Press's first liquid outlet; aluminate recovery uses outlet 1 for lye and outlet 2 for water. The handbook explains this explicitly.

## Controls and required next UI

Follow-up: the [recipe interface playtest](machine-recipe-interface-playtest.md) implements the mandatory picker, previews and saved lock locally. The paragraphs below describe this earlier consolidation checkpoint.

This build uses the existing input-driven recipe matching, pause, cancellation, tank inspection and JEI/Encyclopedia. Before changing jobs, drain incompatible input/output liquids and clear incompatible feed items. A different fluid in an output blocks production rather than being replaced. Unused inputs also block an unrelated recipe. Costs are paid per actual batch.

The new mixed-feed regression test exposed that the old matching path ignored input tanks a candidate recipe did not use. Shared families (more than one capability) now require all present feeds to belong to the candidate before starting a batch; otherwise they show a mixed-jobs message. Single-capability specialists keep their earlier matching behavior. Existing escrowed work is not reselected or discarded when feeds change. This is a safe automatic-mode rule, not the planned persistent recipe lock.

The owner-required searchable product picker, full ingredient/output preview and persistent automation recipe lock are recorded as **MUST** items in [TODO](../TODO.md#must-consolidated-machine-recipe-interface). They are planned follow-up work, not a claimed feature of this first consolidation build.

## Local validation

Validation on 13 October 2026:

- `./gradlew.bat build runClientGameTest -PclientTests=MachineConsolidationClientGameTests -Pkotlin.compiler.execution.strategy=in-process --offline`: passed. All **1,426 required server tests** passed, including the four new consolidation tests covering every shared recipe, original saved batches/slots, incompatible job switching and old Oven storage.
- The rendered client completed all **17 stations**: 12 shared jobs and five original specialists. Working/stopped drive assertions, four model views, each menu and connected power were exercised; **119 screenshots** were captured. The Oven menu, new inlet and representative connections were visually inspected.
- `python tools/check_mod_data.py`: passed the full material/recipe and model audit. The Python test suite passed **34 tests**; repository structure/documentation checks and `git diff --check` passed.
- Compatibility audit: all nine existing construction recipes and blockstate/part mappings are unchanged; no processing recipe JSON changed. Shared tank labels and the mixed-input diagnostic are present. Save tests reconstruct the original schema with an absent Oven solution tank; they do not claim to have loaded the owner's real saves.

The first test run caught surplus fluid being ignored during recipe selection. The production matching fix described above passed the full rerun; the assertion was retained.

Delivery is a local `Jugcraft-Consolidated-Machines-Oct13.mrpack` with a separate **Jugcraft Consolidated Machine Test** world, stocked paused stations, batteries/cables and refill chests. The original manual worlds remain untouched. Import this as a new Modrinth instance and resume individual machines. Actual Modrinth-app import, owner playtest and a two-player session remain unverified. Nothing was pushed or merged to main.
