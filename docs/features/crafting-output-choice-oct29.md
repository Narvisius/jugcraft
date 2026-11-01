# Crafting output selection — October 29, local

Owner-requested work with OpenAI Codex (GPT-6), on the existing dirty
`feature/living-city-local` checkout. Earlier visual/raid and Encyclopedia/mentor
changes are preserved. No commits, pushes, releases or live-server changes.

## Player behavior

When the actual ingredients match multiple different outputs, a small count/arrow
button appears immediately below the result slot. It opens a paged list of item
icons, names and quantities. Selecting a result changes the ordinary preview;
take it normally or shift-craft. Choosing does not spend ingredients or grant an
item. The button is hidden for zero/one output and disabled while holding a
cursor stack. Escape/Back returns to the same crafting grid.

Covered menus: vanilla crafting table, survival inventory 2x2 grid, Jugcraft's ME
crafting terminal and Jug Nouveau storage crafting grid. The screen position
follows the actual container origin when the recipe book moves it. Stonecutting
already has a result selector; industrial form machines retain their native
recipe picker/lock. This is not a new selector for every arbitrary third-party
machine or automated crafter.

The server alone resolves `CraftingRecipe.matches`, assembles the chosen recipe,
checks enabled output items and limited-crafting permissions, and records the
recipe used for advancement and remainder handling. Bucket/tool remainders come
from that selected recipe. Each request is bounded, rate-limited and checked
against the player's current valid menu and current input revision. A changed
grid, stale menu, invalid recipe ID, spectator, distant player or held cursor
cannot select a stale/unmatched output. Closing the menu discards its choice.
Recipe-manager replacement invalidates match caches; taking a result also checks
for a reload/changed limited-crafting rule. Catalogs are cached per recipe manager
and inputs per result container; there is no per-tick full recipe scan.

New ME patterns save the chosen crafting recipe ID in their existing compatible
pattern component. Actual autocrafting validates that ID against the current
ingredients and uses its output/remainders. Old patterns without IDs retain their
old automatic selection. Missing/mismatched pinned recipes fail rather than
silently producing a different item.

## Audit

`tools/audit_recipe_overlaps.py` reads local recipe resources plus explicitly
supplied vanilla/dependency jars, including nested Fabric data jars. It resolves
item tags and tests mirrored shaped layouts, shapeless permutations, and
shaped/shapeless intersections using concrete ingredient witnesses. All files
are inventoried by type. Resource conditions are candidates, not executed by
the Python tool; specialized/custom recipe matching remains runtime-owned.

Local snapshot: 10,155 recipe files inventoried; all 6,893 standard shaped and
shapeless recipes checked; zero unresolved tags or skipped ingredients; 38
different-output crafting pairs. No same-type cooking conflicts were found,
including tag intersections. Shared-input stonecutting outputs are intentional
and already selectable. Report: `build/crafting-choice-review/overlaps.json`.

Concrete witnesses are saved in the test resources and verified against actual
loaded server recipes, including vanilla/mod and cross-mod overlaps. They are
an audit snapshot, not a runtime allowlist: newly added/datapack recipes use the
same live selector without editing this list. Test-only milk-bucket recipes are
absent from the production mod.

## Validation

Common/client compilation passed. All six native `CraftingChoicesGameTests`
passed, including 76 actual loaded witness outputs from the 38 conflict pairs,
normal/shift consumption, inventory bucket remainders, stale/forged/remote
requests, limited crafting, recipe-book hints, both storage grids and encoded
ME choice preservation. The first run exposed a pre-existing literal `54` catalog
boundary in the magical terminal despite its catalog having 36 slots; it is now
`GRID`, allowing normal crafting-slot clicks. Repository/document-link and
targeted whitespace checks passed.

The rendered client test caught a double-background-blur call in the new
popup and a query/selection rate-limit collision when clicking immediately after
opening the inventory. The popup now draws its background once, and opening a
screen uses a separate query rate budget so it cannot discard the first choice.
The final rendered client run passed: real crafting-table and survival-inventory
screens opened the picker, selected alternate results, returned to the same
grid, and preserved ingredients. The fast inventory click is covered without an
artificial wait. Screenshots were reviewed and saved under
`build/crafting-choice-review/screenshots/`; logs and audit evidence are in its
parent directory. No personal profile/gameplay or live server
has been controlled. The full release suite and human multiplayer tests were
not run.

## Bounds

The choice payload is limited to 1,024 distinct outputs per concrete grid, with
bounded recipe identifiers; unusually larger catalogs log a warning instead of
emitting an unbounded packet. This audit does not certify the progression balance
or equivalence of all custom machine recipes. No recipes or costs were changed.
The popup reuses vanilla controls and Jugcraft's existing frame; no new art or
dependency was imported.
