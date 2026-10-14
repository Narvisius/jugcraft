# Machine recipe picker, preview and lock

Owner-directed local implementation, 13 October 2026, using OpenAI Codex (GPT-6). Extends the [consolidated machines](machine-consolidation-playtest.md); no main push, platform upgrade or dependency change.

## Player controls

Open a formed processor's controller and click **Recipes**. Search by process, product or ingredient. Selecting a row only browses it. The scrollable preview lists consumed items/fluids, reusable tool requirements, all products/byproducts, target output slots/tanks and physical port positions. Port coordinates are column, depth and height from the machine's front, using 1-based values. Time, JE/t and total batch energy include currently installed speed/efficiency cards and the operating profile. Costs are per lane; parallel lanes pay separately. Existing overflow/vent behavior is identified rather than concealed.

**Lock recipe** tells new batches to use that recipe exclusively. **Automatic** restores the original input-driven choice. The recipe lock is shared server state, not a player preference. A switch never empties tanks, deletes feed, cancels work or resets paid progress. Existing batches finish with their original reserved outputs. Remove incompatible leftovers manually when changing jobs. Pipes, hoppers and sided item storage only accept inputs used by the locked recipe; existing contents remain recoverable.

The shared screen applies to formed processors, including the four consolidated types and their old specialist equivalents. Generators do not use processing recipes and do not offer this picker. No new upgrade gate, ingredient, power system or recipe balance is introduced. The preview's live status describes the actual machine, not a simulated attempt to run a browsed recipe.

## Authority, saves and reloads

The opening catalog comes from the server's loaded `FluidRecipe` entries, including datapacks. It uses the existing recipe stream codecs and a bounded list, independent of optional JEI/GuiLib. The client sends only a button index into that open menu's fixed catalog. The server verifies the open menu, distance/validity, spectator/build permission, vanilla interaction permission, town protection, bounds, current recipe eligibility and one selection per tick. It synchronizes the choice to all viewers through container data. A changed recipe catalog invalidates the open picker; close/reopen the controller to refresh before selecting.

The block entity saves the stable `recipe_lock` identifier. Older saves without it remain Automatic. An unavailable saved recipe remains locked and reports the problem; it never silently starts another recipe. Active `WorkLane` escrow is unaffected by selection or recipe removal. Registry IDs, construction/processing recipes, storage layouts and existing tank roles remain unchanged.

The recipe page keeps the original menu open and synchronized, returns to it without inventory reinitialization, and closes if the server changes/closes that menu. Search and action buttons use vanilla keyboard/narration controls. Mouse wheel and Page Up/Page Down scroll the preview.

## Validation and delivery

Local validation so far:

- All **1,430 required server tests** passed in the final full build rerun. Four added tests cover stable locks and invalid IDs, preserved paid work plus automation filters, missing recipe/old-save behavior, and nearby/open-menu authority, rate limits, shared viewer data and recipe-cache invalidation.
- `python tools/check_mod_data.py` passed the full data/recipe/model audit (2,292 materials; 7,276 models). **34 Python tests** passed; all **18 locked dependencies/platform pins** passed packaging checks. Repository links/structure and whitespace checks passed.
- The first complete build and native client run passed. The keyboard-scroll follow-up caught a missing key-code declaration at compile time; it now uses the same codes as the existing Handbook screen. A screenshot review also found that searching retained the previous preview; search now previews a matching row without changing the saved lock, and empty searches report no matches. The client test explicitly selects and locks a filtered PTFE result.
- Final `jar runClientGameTest -PclientTests=MachineConsolidationClientGameTests -Pkotlin.compiler.execution.strategy=in-process --offline`: passed. **173 screenshots** cover 17 stations, each picker/lock/Automatic flow, input/output scrolling, the three PTFE search results, selecting and locking the seal, GUI scale settings 2/3, menus, working/stopped drives and power connections. The final seal preview and controller layout were visually inspected. Returning to the original inventory menu works. This is a native single-client test, not two human players.
- A resource-only JAR refresh incorporates the corrected overview guide paragraph. Compiled-class checksums are compared with the passing client-test JAR; no gameplay code is changed by packaging.

The local **Jugcraft-Recipe-Interface-Oct13.mrpack** includes the independent **Jugcraft Recipe Interface Test** world with 17 stocked consolidation stations, paused machines, charged batteries/cables and supply chests. This saved yard was preserved from the passing full build before the subsequent client-only search refinement; its contents and machine behavior are unchanged. Earlier owner worlds are preserved. The output folder includes a development-client launcher, screenshots, checksums and validation logs. Modrinth-app import and human multiplayer remain unverified. Nothing was pushed or merged to main.
