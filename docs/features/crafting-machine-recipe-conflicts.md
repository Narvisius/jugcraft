# Crafting and machine recipe conflict repairs

Owner-directed follow-up to the 23 October 2026 recipe audit and the
[food and cutting-board repairs](food-recipe-conflicts.md). Implemented with OpenAI Codex (GPT-6).

## Player result and scope

The remaining 31 crafting conflict groups and compact-reactor citrus conflict have distinct
inputs. All existing item IDs, recipe IDs and output quantities remain available. Recipes
are updated in their generators/import adapters as well as shipped data. Original owner
library files are unchanged. The companion crate recipe is directly authored JSON.

| Previously competing items | Replacement |
| --- | --- |
| Stirring Rod / vanilla Copper Spear | Bent two-stick rod pattern, same copper and stick cost. |
| Copper Busbar / Lightning Rod | Three copper plates for three busbars; ingots still make the vanilla rod. |
| Tank Leg / Feeder Conduit | Three steel plates in an L for two legs. |
| Bookcase / vanilla Bookshelf | Bookshelf between two sticks makes two bookcase sections. |
| DC Switchboard / Generator Panel / Hydrogen Monitor | Lever, electric motor and Hydrogen Sensor Head distinguish the three panels. |
| Engine Panel / Relay Cabinet | Engine panel uses a piston in place of its redstone; cabinet retains its recipe. |
| Chilled-water Supply / Return labels | Green dye selects supply, lime dye selects return. |
| Four duct labels / Drawing Flat File | Duct labels use paper and white (outside), yellow (filtered), blue (supply) or red (return) dye. The file uses a paper center and iron-nugget handle. |
| Fixed Radiation Monitor / Area Monitor | The fixed ventilation monitor uses a duct fitting. |
| Plant Display Wall / Reactor Console | The wall uses three existing wall displays; console retains its processor recipe. |
| Five pocket pets | Retain diamond and emerald, replace one of seven nuggets with rabbit foot (cheetah), ender pearl (meta), cod (ocelot), spider eye (spider) or ink sac (squid). |
| Polymer Molding Press / Precision Finisher | Housing mold and grindstone distinguish the two machines; neither requires its own output. |
| Stonemason's Chisel / Carving Knife | Add flint beside the chisel head. |
| String-light Hook / Key Blank | Add string to the hook. |
| Soldering Tool / Sampling Spoon | Add a clay-ball insulating grip to the soldering tool. |
| Silk Spool Stack / all vanilla bed colors | Three yarn between two rows of wooden slabs. |
| Tall Filing Cabinet / Locker | Six plates with paper files and an iron-nugget handle distinguish the cabinet. |
| Haunted Dining Chair / Rocking Chair | Add a soul torch to the haunted chair. |
| Upward Duct Elbow / Cable Tray Bend | Four existing ducts or cable trays in an L produce four matching fittings. |
| Training Screen / Wall Display | Training screen adds a book below its circuit. |
| Supply / Return duct riser labels | Paper with blue (supply) or red (return) dye. |
| Wooden Basket / Mob Transport Crate | Crate adds a lead between its handles. |
| Corrugated Iron / Skid Iron / Steel Armor Plate | Corrugated sheets use a staggered four-plate pattern; skid keeps its square; armor uses a four-plate T pattern. Yields remain eight, four and two. |
| Filing Cabinet / Weathered Steel Plate | Cabinet adds paper and an iron-nugget handle; weathered plates retain their separate recipe. |
| Chilled-water Elbow / Tee | Four pipe segments in distinct L and T shapes. |
| Relay-row signs A / B / C / D | Blue, cyan, purple and light-blue dye respectively. |
| Lab Coat / Pyromaniac's Tunic | Tunic replaces its center wool with charcoal. |
| Storage Tank / Storage Tank Plate | Plates use an eight-plate stepped sheet pattern; complete tank retains its hollow ring. |
| Coffee Table / Locker Bench | Bench adds an oak-fence support between its feet. |
| Stone cross / upright gravestones | Upright uses a two-wide pillar with a foot, cross retains its five-stone cross. |
| Mossy cross / upright gravestones | Same distinct shapes in mossy cobblestone. |

## Processing and intentional choices

The compact Chemical Reactor uses the new `jugcraft:compact_fermentable` tag for direct
Bioethanol: the existing fermentable crops except lemons and oranges. Citrus therefore
always makes Citric Acid, even when supplied with enough fruit and water to satisfy the
old fermentation recipe. Other fermentable crops retain eight crops + 1,000 mB water
to 250 mB Bioethanol. Acid quantities and timers are unchanged.

The broad `jugcraft:fermentable` tag is unchanged. Citrus can still enter the Mashing
Station → Industrial Fermenter → Chemical Distillation Station ethanol chain and
other consumers of that tag. The new tag is generated from the broad authored list
after all contributions, excluding only the two acid-extraction fruits.

Precision Finisher plates/bearing balls, Gasifier gas routes, Gas Infuser variants and
Anaerobic Digester products remain intentional choices using existing recipe locks and
priorities. JEI information pages now explain their choices, automatic defaults and
relevant output-space requirements. No processing costs, capabilities or selection code
change for these machines. Furniture workbench and stonecutter selectors remain intact.

## Progression, costs and compatibility

Scope spans early tools, household decor and existing industrial tiers. Producers are
existing vanilla gathering/crafting, farming and shared metal/electronics workshops;
consumers remain the same tools, decor, processing stations and factory structures.
Added components are obtainable before their products; the survival acquisition graph
checks all active routes. No new research gate, dependency, saved registry entry, art,
network packet, currency or per-tick work is introduced.

Output counts are unchanged. Costs intentionally distinguish competing products: several
recipes add modest components, duct labels replace one plate with a dye, and the tunic
replaces one wool with charcoal. Fittings consume as many existing segments as they
produce, with no reverse conversion added. Pocket pets retain both expensive gems.
The haunted chair now requires a soul torch; the tunic requires charcoal. Alchemy
values were recalculated after both recipe repair batches: 46 numeric records change,
including dependent items and the earlier food routes. Reference seeds and valuation
rules are unchanged; equal-price provenance is kept stable. A regression check verifies
that the repaired crafting fixtures cannot produce more essence than their cheapest
accepted ingredients. Existing
items and placed machines remain valid. In-flight industrial batches keep their reserved
results under the existing machine persistence rules. Normal crafting inventories using
an old pattern need rearrangement or the newly listed ingredient after updating.

## Validation

- The full static scan covers 8,045 vanilla, Jugcraft and cached pinned dependency recipe
  definitions, including tag intersections, shaped mirrors and shaped/shapeless overlap.
  No different-output crafting conflicts or compact-reactor conflicts remain. Relative
  to the original audit, both repair batches remove 83 overlapping pairs and add none.
  Remaining raw overlaps are identical outputs or industrial routes distinguished by
  capabilities, tools, priorities and explicit selection.
- Six new Python checks cover generator/import parity, the 87 crafting fixtures,
  the narrow fermentation tag, refreshed JEI data and essence conservation. Earlier
  food, JEI-station, survival-graph and alchemy checks also pass: 25 checks in the
  final combined run, plus five progression checks from the earlier run.
- `runGameTest compileClientJava --offline --console=plain` with the focused filter:
  final run **PASS**, both required tests, build successful in 6m 4s after the
  early-game and tower-cost corrections. Existing deprecation and
  optional-annotation compiler warnings remain. This validates recipe matching and
  JEI compilation, not a rendered client interaction.
- `scripts/check_repository.py` and `git diff --check`: **PASS**.
- The full `tools/check_mod_data.py` run completed and found four integration
  failures: recognition of the new crop tag, armor plate cost accounting, and the
  tunic's Overworld and first-loadout requirements. These were corrected. The
  affected crafting, machine, fluid, tag, tower-cost, Ember, solo-journey and entry-gate
  checks were rerun and **PASS**. The full validator was not repeated end to end
  after these corrections; its unchanged art checks covered 13,187 block/item models,
  3,729 quad parts and 176 combined models. No validation rule was relaxed.
- Not claimed: full GameTest suite, rendered JEI interaction, two-client playtest,
  live-server installation or deployment. Custom server datapacks, unknown extra mods
  and dynamic/component-dependent serializers are outside the static audit's coverage.

Regression coverage is in `RecipeConflictGameTests`, `recipe_conflict_cases.json` and
`scripts/tests/test_recipe_conflicts.py`. Run the server test filter
`*:recipe_conflict_game_tests*` for these two tests. The crafting test compares every
fixture's output and quantity against every loaded crafting recipe, including mirrored
and translated inputs. The reactor test checks 4/8/64 citrus at 250/1,000/8,000 mB,
the actual compact lookup, continued citrus mash acceptance and the direct ethanol
minimum input/yield.

## PR #462 CI follow-up (23 October 2026)

OpenAI Codex (GPT-6) investigated the two failed checks at `086f44521`.
The generated-resource check found three stale Handbook crafting grids: the Polymer
Molding Press still displayed a steel gear instead of its housing mold, and both
Precision Finisher pages still displayed a steel gear instead of a grindstone.
Regenerating the resources updates those grids from the existing recipe sources.
The optional-integrations-absent build completed 2,280 server GameTests with one
failure: the coastal construction fixture still supplied a steel gear for the press.
That fixture now supplies the approved housing mold and retains its actual
RecipeManager/output assertion. No production recipe, assertion, test selection or
CI requirement is relaxed by this follow-up.

The full resource generator completed with only those three expected Handbook
ingredient changes. All 11 focused food, crafting/machine and coastal-layout Python
checks pass, as do the repository documentation and whitespace checks. The PR's
validation section records the subsequent complete build/data-check results.
