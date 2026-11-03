# November 3 batch: raider crews, companion stable, placed-food remodels, Thanksgiving decor

Status: implemented locally, not yet compiled or run. Owner request, 3 November 2026; implemented with Claude Code, to be reviewed, integrated and tested by Codex. Base: `jimbozoomer-byte/jugcraft` main `88c2379ba2331f712c3a9e3064c00046845b3227`. Nothing is committed, pushed, released or deployed. No client channel was changed, and no live server was touched.

This is the batch's ledger: what was done, what remains, and the evidence. Each group's feature record has the details.

## Environment limits (read first)

The Claude environment cannot reach Fabric's or Mojang's maven (both fail to connect), so:

- **Not run:** compilation, the server game tests, the rendered client tests and in-game screenshots.
- **Run:**
  - the Python checks (`scripts/check_repository.py`, `tools/check_mod_data.py` and others named per group);
  - tree-sitter syntax checks of every changed Java file;
  - offline textured model renders: a z-buffered software renderer of the model JSON and quad files, written for this review. Vanilla textures are not in the repo, so they are drawn as flat approximate colours and labelled;
  - layout mocks of the stable screen.

Neither the renders nor the mocks are game screenshots.

Whole-repo asset generation needs `tools/newest_shapes.py`'s pinned vanilla `block/cube` files, which are unavailable here; that is the known CI blocker. Only the specific generators each group changed were run, and their outputs were compared with the committed files.

Java APIs this work uses that are not used elsewhere in the repo (please check with javap):

- `Entity.startRiding(Entity, boolean, boolean)` (the repo uses it elsewhere);
- `ServerLevelAccessor.getLevel()`;
- `GameTestHelper.makeMockServerPlayerInLevel` (used elsewhere);
- `ScreenRectangle.left/top/right/bottom`;
- `TestInput.pressKey(int)`;
- `CompoundTag.getFloatOr`, which the repo already uses.

## A — Raider crews (`features/raiders.md`, "Crews")

**Done:**
- `raiders/RaiderCrew` (new): seats, boarding, facing, failing without a crew, bail-out, and the `Afoot` goal wrapper.
- The walker's open roof hatch: `tools/raiders.py` `walker_hatch()` and `raider_quads.json` `raider_walker_hatch`, drawn by `RaiderWalkerRenderer`.
- Crews drawn from the party (`RaiderRaids.start`).
- Crew withdrawal with the machine (`RaidMember.withdraw`).
- Walker standoff and reach-checked melee (`RaiderWalker`).
- Blimp climb when stuck (`RaiderBlimp`).
- Marcher sidestep/skip recovery (`MarchGoal`).
- Crew cover and suffocation immunity (`RaiderInfantry`).
- Tests: `RaiderCrewGameTests` (6), and the client showroom crews plus the close-up `jugcraft_raider_crews`.

**Evidence (run here):**
- `check_repository` PASS.
- `check_mod_data`: exit 1, with 48 O1 findings, all in the owner's imported `models/newest/` files, which this batch does not touch. The same 48 come up on the merged tree and on an untouched base (measured by both workers). An earlier note here said exit 0; that was the exit code of the `tail` the output was piped through, and is corrected.
- The raider quad export was byte-identical before the change, and afterwards adds only the hatch.
- Tree-sitter: no syntax errors.
- Offline renders of the hatch with a stand-in driver, and the blimp seat with a stand-in pilot.

**Remaining:**
- Compile.
- Run the tests and the client screenshot.
- A full city raid, observing the standoff and the stuck recovery.
- Guard repositioning (TODO, October 29 item).

## B — Companion stable screen (`features/pet-inventory-nov2.md`)

**Done:**
- `client/PetInventoryScreen` rewritten. `pets/Pets.Row` gains display fields (health, max health, KO seconds, own orders); `Pets.store` records health.
- Tests: `PetInventoryClientTests` (extended) and `PetStableGameTests` (new).

**Evidence (run here):**
- Tree-sitter: no syntax errors.
- The layout mock fits at 320×240, 427×240, 480×270 and 640×360 with no overlaps.

**Remaining:**
- Compile.
- Run both tests.
- A human look at the screenshots at GUI scales 2–4.

## C — Placed-food remodels (`features/placed-food-remodels.md`)

**IDs:** `cornbread_stuffing`, `vegetable_noodles` (Veggie Noodles), `squid_ink_pasta`, `bacon_and_eggs`, `steak_and_potatoes`, `roasted_mutton_chops` (the plated Mutton Chops; the `mutton_chops` and `cooked_mutton_chops` items and `pasta_with_mutton_chop` are untouched), `pasta_with_meatballs`, `tomato_sauce`.

**Done:**
- All eight are sculpted food on closed dishware, 21–33 elements each, at 16×16 density in the owner's palettes:
  - stuffing: the Skillet with toasted cubes, cranberries and sage;
  - noodles: a lined bowl with carrot and pepper strips, cabbage and onions;
  - squid ink pasta: dark twirled pasta on a glazed plate with tomato and squid;
  - bacon and eggs: two bacon strips and two eggs with raised yolks;
  - steak and potatoes: a grill-marked steak with a cut face, mash, roast potatoes;
  - mutton chops: three chops with bones rising over the rim, potatoes, onion, rosemary;
  - pasta with meatballs: spaghetti, sauce, three meatballs, basil, cheese;
  - tomato sauce: a lined, flared bowl filled to below the rim, with a spoon.
- **Source of truth:** `tools/menu_data.py` sends these eight to the new `tools/menu_meal_models.py`, and `tools/menu_meal_textures.py` draws their 20 textures. `generate_textures.py` calls it, and the branches of `feast_meal_models.py` these eight no longer use were removed.
- `food_servings.py` skips menu dishes and its catalog has none of the eight, so it is unchanged.
- Blockstates (facing only, no portion property), recipes, eating, placement, drops and nutrition are unchanged. Item models (flat icons) are unchanged.

**Evidence (run here, by the food worker):**
- Targeted regeneration through the real writers changes only the 8 models; the other 65 dish models and all blockstates are byte-identical, and a rerun is identical.
- The full `generate_material_data.py` stops at the known `newest_shapes` blocker, after the menu models are written; it rewrote 0 of these files.
- Art-check rules on the 8 models: 0 findings.
- `check_repository` PASS.
- `check_mod_data`: the same 48 findings as the base. Its earlier run caught one faceless element, which was fixed.
- Renders: before and after at normal game scale (ppu 6) from several angles, close-ups, undersides, all four rotations, and the reference sheet.

**Choices to confirm:**
- Squid ink pasta and pasta with meatballs sit on a glazed cream-and-blue plate, not wood, so the dark pasta reads. Switching back is one value per dish.
- "Capped vessel" for the tomato sauce was read as a lined bowl whose sauce surface caps it below the rim. A lidded jar would be a small follow-up if that was meant.
- None of the eight blocks has a portion property, so there are no serving states to model. Adding them would mean Java and blockstate changes, outside this art pass.

**Remaining:**
- In-game look.
- Rerun the full generators where the pinned vanilla files are available.

## D–G — Thanksgiving decorations (`features/thanksgiving-decor.md`)

**Already in the game (13 of the 32 ideas), so not duplicated:**
- corn bundle and braided corn: `ornamental_corn_bundle`, `corn_shock`, `corn_dolly`;
- wheat sheaf: `wheat_stook`;
- produce crate: the crop crates;
- cranberry bowl: the `cranberry_sauce` placed serving;
- wreath: `autumn_wreath`, `bittersweet_wreath`;
- `harvest_bunting`;
- `hay_bale_seat`;
- `scarecrow`;
- wheelbarrow: `pumpkin_wheelbarrow`;
- `cider_barrel`;
- squash cart: `pumpkin_wagon`, `orchard_handcart`.

All four large pieces were already covered, so no new multi-block was needed.

**Done: 19 new blocks and 1 item, generated by `tools/thanksgiving_decor.py` and `tools/thanksgiving_decor_data.py`.**
- **D:** `pumpkin_and_gourd_stack`, `wicker_apple_basket`, `pear_basket`, `root_vegetable_hamper`.
- **E:** `harvest_place_setting`, `napkin_and_cutlery_caddy`, `bread_basket`, `gravy_boat`, `covered_butter_dish`, `pie_stand`.
  - `autumn_leaf_table_runner` (the item) is a fourth Feast Table cloth (`FeastTableBlock.Runner.LEAVES`, saved by name), so it works on tables of any length.
- **F:**
  - `autumn_leaf_wall_garland`: neighbours join, with end variants.
  - `amber_jar_lantern`: light 13, stands or hangs.
  - `pumpkin_candle_centerpiece`: flint and steel lights it, an empty hand snuffs it.
  - `harvest_market_sign`, `copper_pan_rack`.
- **G:** `turkey_statuette`, `acorn_leaf_mobile`, `squirrel_acorn_figurine`, `table_cornucopia`.

Every piece:
- is a shapeless recipe from reachable items, with no research or calendar gate;
- faces the player when placed, rotates and mirrors;
- has a sized outline and drops itself;
- has its name, tool tag and creative-tab entry, and appears on the encyclopedia's Furniture & Kitchens page.

Models use 8–39 elements. Nothing in the city, the Halloween schedule or anyone's placements was touched. The opt-in town arrangement is left for the city phase.

**Evidence (run here, by the decor worker):**
- `check_repository` PASS.
- `check_icon_maps` PASS.
- `check_mod_data`: the same 48 O1 findings as a clean checkout, and the new `check_thanksgiving_decor()` passes.
- Art-check rules on the 40 new or changed models: 0 findings.
- Python unit tests (feast presentation, harvest festival, resource cleanup, recipe conflicts) pass.
- Recipe-overlap audit: no overlaps involving the new recipes.
- Tree-sitter: no syntax errors.
- Offline renders of every piece, all rotations, and scale against existing decor.
- The real generator functions reproduce every committed file. They were run through a scratch wrapper, because the whole-repo generator stops at `newest_shapes.py` here.

**Review notes (coordinator):**
- Fixed a Javadoc the insertion had displaced in `JugcraftAgriculture`.
- The gravy boat read more like a jug than a boat at game scale. It was remodelled in the review round below.

**Remaining:**
- Compile.
- Run `ThanksgivingDecorGameTests`.
- An in-game look.
- After merging, rerun `generate_material_data.py`, `generate_textures.py` and `build_alchemy_values.py`. The new items use the one-essence fallback until then.
- Floor props, like the existing ones, stay put when the block under them is removed.

## Combined checks on the merged tree (all groups)

Run here on the tree with every group merged:

- `scripts/check_repository.py`: PASS.
- `tools/check_icon_maps.py`: PASS (286 maps; the warnings are on the existing legacy-icon list).
- Tree-sitter syntax check of all 21 changed or new Java files: 0 errors.
- `git diff --check`: no whitespace errors.
- Python unit tests:
  - Pass: `test_client_test_selection` (11), `test_asset_png`, `test_encyclopedia`, `test_food_recipe_conflicts`, `test_generated_resource_cleanup`, `test_harvest_festival`, `test_recipe_conflicts`, `test_feast_presentation` (see the next item).
  - **`test_feast_presentation` was failing on the eight remodelled meals and was updated deliberately.** Its meal check required every box to keep all six faces and to sample single texels of the dish icon: the old flat style. The remodels intentionally leave buried faces out (closed geometry, ART_DIRECTION) and use their own textures.
    - The old check still runs, unchanged, for every other bowl and plate meal.
    - A new test, `test_sculpted_meals_fit_their_collision_and_draw_only_opaque_texels`, holds the eight to what still matters: real boxes inside the block, no higher than the dish's collision height, at least one face each, only opaque texels of textures that exist, and the saved model equal to the generator's.
    - Measured heights are at most 5.0 against the 5.5 and 6.5 limits.
  - **`test_food_servings`: 48 errors, all from the environment.** Each is `FileNotFoundError: /root/.gradle/caches/fabric-loom/26.3/minecraft-client.jar`; the Minecraft client jar cannot be downloaded here. Not a code finding, and not run.
- `tools/check_mod_data.py` on the merged tree: **exit 1**, with exactly 48 O1 findings ("left-out faces of opaque boxes are exposed").
  - All 48 are in the owner's imported `models/newest/` files. None of the batch's 227 files is under `newest/`.
  - The list is identical to the run with only group A, and to the untouched base (both workers).
  - No new finding. The new `check_thanksgiving_decor()` and the Feast Table runner checks pass.
  - These pre-existing findings are not hidden or allow-listed by this batch: they remain for the original-model geometry review (TODO P1).

## Delivery

Grouped patches: A against the base; each later group against the base plus the earlier groups. New and binary files are inside the patches (`--binary`) and also in the archive. The archive also carries checksums, a list of modified files, instructions and renders.

## Review round (after delivery)

Codex applied the delivery and asked for a second look at A's lifecycle, save/reload and stuck recovery, and at B's keyboard and small-window behaviour, plus a gravy boat that reads as a boat. Exported as one incremental patch against the delivered tree. No compile or API fixes were made: Codex owns those.

**A, bugs found and fixed:**
- **A walker that lost its driver kept fighting.** For its 2 seconds of smoking it still marched, strolled, took up its standoff and punched, because its goals run before the stall check. Every goal, and the punch itself, now stands down while it is stalled (`RaiderWalker.stalled()`).
- **A stalled machine dropped loot when its raid ended.** A crew member checks its raid on its own clock, so the crew could withdraw first and leave the machine to go down with its drops. Now a stalled machine whose raid has ended withdraws with it, with no drops (`RaiderCrew.abandoned`).
- **The blimp could climb for no reason.** Its "no nearer for 5 seconds" count carried over from one target to the next, so a fresh target could set off a climb at once. The count now restarts when its heading changes. The climb counter no longer runs below zero.
- **A marcher that gave up its last waypoint stood for good.** It now plans a fresh route 30 seconds later (`MarchGoal.REPLAN_TICKS`).

**A, checked and unchanged:**
- **Save and reload:** crew save inside their machine, as passengers. Nothing creates crew on load.
- **Crew persistence:** crew are not made persistent on their own.
- **Seating:** the `/summon` path seats the crew only, so it is not added twice.
- **Damage:** the hatch cover and the bail-out.

**B, bugs found and fixed:**
- **Tooltips ran off small windows.** The long behaviour, bond and health tooltips were single lines that could run past the edge of a small window. All four of the screen's own tooltips now wrap to the window, at line breaks and between words. They use the `setTooltipForNextFrame(font, List<Component>, Optional, x, y)` overload, which the repo already uses in `local/peepo/client/CompanionScreen.java`.
- **The countdown was cut off.** On a narrow roster the "Knocked out 0:42" pill was cut with an ellipsis and lost the time. A pill too narrow for its words now shortens them: Out, KO 0:42 (or Ready), Stabled.
- **The bars overlapped on short pages.** At the shortest GUI heights the bond and health bars were drawn over the portrait or the name block. The portrait now shrinks first; if there is still no room, the bars are left out.
- **Tab order.** Tab reached Refresh and Done before the companion's own keys. The keys are now added first.

**The gravy boat.** It was a deep square bowl on a foot, which read as a jug. It is now a long, low boat, using the same generator, palette and painters:
- **Shape:** narrower at the bow and stern than in the middle, with a stepped rim round gravy just under it.
- **Spout:** at the bow the lip draws out into a pointed spout tilted up, with a drip down the bow and a small spill on the saucer.
- **Handle:** at the stern the rim rises into a looped handle.
- **Saucer:** an oval saucer underneath.
- **Unchanged:** the block outline, ID, recipe and behaviour. The new model fits the existing outline.

**Checks for this round (Claude environment; no compile here):**
- Ran:
  - tree-sitter on the five changed Java files: 0 errors;
  - `py_compile` of the generator, and a JSON parse of both regenerated models;
  - `git diff --check` of this round's changes.
- **The generator** (`thanksgiving_decor_data.gravy_boat`, through the decor wrapper) changed only the gravy boat's block model, item model and texture. Every other decor output was rewritten byte for byte the same.
- **The model's bounds** `[4.0, 0.0, 2.23, 12.0, 5.02, 13.25]` lie within the unchanged outline `[4.0, 0.0, 2.5, 12.0, 5.0, 13.5]`, inside the half-pixel tolerance `check_thanksgiving_decor()` allows.
- **Offline renders:** before and after at ppu 6, side views, and all four facings from the blockstate.
- **Small-window layout mocks** of the stable at 320×240, 427×240, 480×270, 640×360, 300×200, and 320×180 (forced, below the minimum GUI).
- **Pending (still running when this patch was exported; the result follows separately):** `tools/check_mod_data.py`, then `scripts/check_repository.py` and `tools/check_icon_maps.py`.
- **Codex reported locally:** `compileJava`, `compileClientJava` and `compileGametestJava` passed on the delivered tree. Server game tests and the combined runtime run with this round are Codex's.

## Local integration checkpoint — 3 November 2026

Integrated by OpenAI Codex on `feature/nov3-harvest-polish-local`, based on `88c2379ba2331f712c3a9e3064c00046845b3227`. Claude web session supplied the implementation and incremental review patch. Everything remains uncommitted and local.

- Original delivery archive SHA-256: `1c1ff94a4e69e1e7468aa18a1fe244b02c966ec3fe79add5ff8c99b74973ab61`.
- Review archive SHA-256: `2823075430a788788f8571a8cb2bdbb1435184f2b596e09b60a3c6ee933c27c1`; all 13 files applied cleanly.
- Codex additionally checks the sidestep column is loaded **before** calling its heightmap in MarchGoal; the height query must not create a chunk load.
- Generated and added the 20 new decoration essence entries only, preserving existing item values. The leaf runner retains the generator's fallback value; this is not a claim that every value was derived from a recipe.
- Added `HolidayPolishClientTests` for rendered evidence of all eight meals, new decor, leaf runner and seated walker/blimp crews.
- Compilation of main/client/game-test sources passed before the review; the updated pet screen also compiled during the first server run.
- First combined local server run: **all 52 required tests passed** (`build/nov3-holiday-server.log`). The final review's main-source changes arrived after that run's compile phase, so this is NOT final review validation.
- **29 targeted Python tests pass:** 10 feast/food-serving/harvest tests, 6 recipe-conflict tests, 13 test-selection/resource-cleanup tests. Initial recipe import failures were resolved by setting `JUGCRAFT_FONT_DIR` to the existing workspace DejaVu directory; no assertions were weakened.
- Repository structure/link check and `git diff --check` pass.
- Claude reports the final data audit still matches exactly the 48 pre-existing `models/newest/` geometry findings; not fixed or hidden by this batch.
- **Final combined server/client runtime validation is still in progress.** Do not treat this checkpoint as release approval.
- No live server, client updater, owner world, or personal launcher profile was changed.

## Final local validation — 3 November 2026

The checkpoint above is superseded by these completed results:

- Final combined server run after all review/source fixes: **52/52 required tests pass** (`build/nov3-holiday-final-runtime.log`, 03:07:54).
- `PetInventoryClientTests` and `HolidayPolishClientTests`: **both pass** (`build/nov3-holiday-client-retry.log`). The first visual-test attempt used the wrong food registry; the test was corrected to use the existing `PlacedDishBlock` registrations. No gameplay registry change was needed.
- The pet client checks exercised empty/active/stored rosters, exact-UUID recall/deploy, Stay, keyboard selection/action, KO deploy lock and the 640×480 window. Screenshots reviewed: controls, portraits, status and bars remain inside the frame; long names shorten with tooltips.
- A further HUD-hidden 1280×720 visual pass **passes** (`build/nov3-holiday-clean-visual.log`). Reviewed all eight meals, floor/wall decorations, the leaf runner, the walker driver in its hatch and the blimp pilot in its gondola. No missing textures seen on these additions. Crew pose evidence uses stationary no-AI test vehicles; it is not a full populated-city raid soak test.
- Evidence preserved under `build/nov3-holiday-evidence/client-first-pass/` and `build/nov3-holiday-evidence/clean-visuals/` so subsequent test cleanup does not erase it.
- The 29 targeted Python checks and repository/link check pass. `git diff --check` passes.
- Claude's final data audit remains exit 1 with exactly the same 48 existing imported geometry findings; icon checks pass with 28 existing warnings. This is NOT a clean full-repository audit.
- Added Claude's docs-only food modelling guide/reference sheet, with links from CLAUDE.md and ART_DIRECTION.md. Patch SHA-256: `173728fb79f2d0bb6b86a53238c0f297c910b1eb8c36713f2b00fdb8449acadc`.
- Implementation/review/targeted local validation are complete for groups A–G. Owner visual playtest and any later release/deployment remain separate. No commits, pushes, releases, live operations, updater or personal Modrinth changes were made.

## Release preparation — 3 November 2026

Owner authorized review/merge of ready PRs and preparation of a client/server update. Local batch committed as 5bc6383ac1 and pushed in PR 606. PR 597 was reviewed; GitHub requires its branch to be current with main, so update-branch was requested and fresh checks started. Normal merge was refused as out of date; no protection bypass was used. Auto-merge is disabled on this repository. PR 600 remains conflicting with an unsuccessful client run; other unfinished drafts are not release-ready. Deployment and player-channel promotion have not occurred. A new live restart confirmation is still required once files are ready.

### Fresh CI review

PR 606 Foundation found two new issues: bread_basket used nonexistent jugcraft:dinner_roll, and the runtime atlas fixture omitted 42 new texture references. Replaced the invalid ingredient with a second vanilla bread in both generator and recipe, and regenerated the resolved-reference fixture. Local full Python discovery now passes 310 of 316 tests, including both repaired tests; six graphics shell-harness tests fail in this Windows execution environment (see build/nov3-release-foundation-local.log). This is not a full green result.

PR 597's refreshed build fails before generation completes: newest_shapes searches only the remapped Loom archive and cannot find pinned vanilla block/cube on fresh CI. Its previous green run predates current main. The independent art audit confirms 48 existing imported-model findings. No merge override or live deployment has occurred. These release blockers must be resolved before normal merge/release.

### Release-blocker repairs (in progress)

OpenAI Codex backported the source-bounded model finishing and multipart proof approach from PR 600, scoped to 16 cap/handle models. Existing hash-guarded separation repairs are retained. Ten multipart tests, four model finishing tests, eight existing geometry tests, and a fresh-cache shape lookup regression pass. Full geometry audit now passes: 38,734 models, 3,778 quad parts, 182 assemblies. No blanket exemptions were added; negative tests remove covers, reverse caps and introduce transparency.

Canonical generation completes. Shape lookup now reads the prepared pinned vanilla archive when Loom has not yet created a remapped archive. Architectural state keys serialize deterministically; corrected the later chandelier writer's outdated chain ID. Generator-only ordering/formatting refreshes preserve board loot and names.

Full Python discovery passes all 327 tests outside the Windows sandbox. The six earlier shell failures were Git Bash being unable to create its signal pipe in the sandbox. Four additional hosted-runner disk-boundary tests and four model-finishing tests pass separately.

Runtime follow-up: isolate the Thanksgiving-truce/season-restart tests in separate test environments so temporary global dates cannot overlap ordinary raid tests. Verify owned Pumpklings enter the pet roster without pumpkin drops, while unowned Pumpklings retain their drop behavior. Backported bounded peer startup/log diagnostics from PR 600; normal network deadlines stay unchanged. Rendered clients get bounded 6 GiB heaps and CI uses swap plus guarded disposable-runner SDK cleanup; no tests or assertions removed. Compilation, full data audit and combined runtime validation are pending. No live actions or client-channel promotion.

### Combined release follow-up

PR 597 is integrated into this branch (ad556a94a7), not yet main. Full data audit passes. Foundation run 41693607607 passes. Full Build remains pending the following repairs and validation:

- Connected textures use the existing pixel-aware PNG writer. All 439 sheets regenerated with identical bytes and metadata; the PNG regression confirms changed pixels and corrupt files are replaced. This resolves cross-platform encoding drift without ignoring visual changes.
- Machine-test cleanup extended 40 blocks beyond its arena and removed neighboring food drops. Cleanup now intersects the fixture bounds. Two combined 95-test runs exposed this interference; rerun pending.
- Reviewed PR 600 third-person applied-pitch/external-view synchronization and regression checks backported. Crafting-planner tests now cover the actual catalog page count instead of an outdated 32-page limit. Client validation pending.
- Client selector checkout timeout increased from 5 to 15 minutes after the two-parent fetch timed out. Coverage remains unchanged.

No live maintenance, release publication, main merge or client-channel promotion occurred.

Combined server rerun passes **95/95 required tests** (`build/nov3-release-combined-server-3.log`). This includes machine recovery, raid approaches/crew, pets, seasonal isolation, foods and Lua readiness. Combined five-suite client validation is running.

Combined client run passes all five suites (`build/nov3-release-combined-client.log`): imported-model atlas, Peepo crafting, weapon arts/orbit, holiday visuals and pet inventory. Reviewed the original-art gallery and compact pet UI screenshots. Twelve world-designer tests pass. Evidence copied to `build/nov3-release-evidence`. Foundation run 41697495156 hit its five-minute whole-job timeout during passing contract tests, so the budget is raised to 15 minutes without changing coverage. Full CI remains pending.
