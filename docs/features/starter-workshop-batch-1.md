# Starter workshop — Batch 1

Status: owner-approved preview; PR #442 ready for review.
AI assistance: OpenAI Codex (GPT-6).

## Scope

Implement the first slice of the [industrial starter workshop plan](industrial-starter-workshop-plan.md): a two-block-wide Workshop Bench, a one-block Small Ceramic Kiln and a two-block-tall Small Crucible Furnace. The bench uses interchangeable, permanent hammer, wire cutters and fuel-heated soldering tools. No tool wear or installed-part maintenance.

Construction follows the accepted provisional costs: bench = crafting table, four planks, two cobblestone; hammer = two iron and a stick; cutters = two iron and two sticks; soldering tool = copper and a stick; kiln and furnace body each = four brick blocks and a furnace. Shape a lining from four clay balls and a crucible from seven, then fire them and install both in the furnace. One clay ball makes two insulator blanks, fired into the existing ceramic insulators.

## Processing contract

- Manual plates: iron, copper, tin, zinc, lead, bronze and brass, one ingot to one plate, 100 active-work ticks.
- Manual copper wire: one copper ingot to three wires, 100 active-work ticks.
- Manual basic circuit: existing silicon, three copper wires and solder to one circuit, 200 active-work ticks with ordinary heating fuel.
- Kiln: two insulator blanks in 200 ticks; lining in 400 ticks; crucible in 600 ticks.
- Furnace: three copper and one tin to four bronze, or three copper and one zinc to four brass, in 200 ticks; one tin and one lead to two solder in 120 ticks.
- Preserve machine yields and existing powered recipes, bronze-blend fallback, current copper cable and silicon routes. Add six wires, one tin and two insulators to eight copper cables as an alternate starter recipe.

Select a recipe, insert ingredients and click Start Work for manual production. Work stays enabled until Stop Work is clicked or the player leaves the bench menu. Server-validated presence still prevents work after closing, disconnecting or moving out of reach. Fuel credit, committed ingredients and partial progress survive saves; blocked output or missing requirements spend no fuel. Inventory automation never supplies player work or removes a tool during an active batch. Installed lining and crucible are reusable.

Registration remains stable with features disabled; acquisition uses the machines feature without requiring drones for early insulators. Use shared recipes, inventory, fuel and permission systems. New stations must handle interruption, multiple viewers and breaking either multiblock part without duplicate drops.

## Boundaries and overlap check

Current main, open pull requests and public fork branches were reviewed before implementation. The upstream starter-workshop branch contains planning documentation only. Advanced ceramics and powered-tool work are adjacent, not this implementation; retain their identities and recipes.

Batch 2 is excluded: no quartz world generation, silicon heat upgrade, bellows, reusable gear molds, mechanical waterwheel or existing machine cost changes. No terrain generation changes.

## Integration and assets

The workshop shares distinct-slot ingredient matching with powered machines and persists its reserved inputs/results in the existing `WorkLane`. Vanilla cooking-fuel components supply heat; unused credit is retained while idle. Vanilla menu buttons carry only recipe selection and enabled-work presence, with server-side distance, menu, feature and structure-permission checks. Hoppers can access either station part but cannot supply labor or remove installed tools/ceramics. Recipe reload invalidates open picker snapshots.

Zinc had no plate item on main. This batch adds it to the shared component registry, common plate tags and normal powered press generator, preserving the same one-ingot yield as the new manual route. Existing cable recipes remain; the alternate ceramic cable has a distinct recipe filename so the tower-cost audit continues to use the established canonical cable cost.

The owner asset library and complete catalog were inspected for hammer/cutter references. New native closed-cuboid models use existing Jugcraft `sp_wood`, `sp_iron`, `sp_copper`, firebrick and ceramic surfaces, plus vanilla clay. The zinc icon uses the existing plate renderer and zinc palette. No new dependency, copied external model or new animation runtime is introduced. Shared JEI recipe-view data and the Engineer's Handbook explain the recipes and reusable parts.

When the optional Diorama map is installed, its existing Fabric HUD element is wrapped after client startup to suppress it only while a workshop menu is open. This prevents overlap at small window sizes without mutating map preferences or linking optional Diorama classes into the workshop.

The inspection save is a new normal world with seed `jugcraft-starter-workshop-1`, with a five-station gallery near `512 240 512`. It is separate from existing owner worlds. Plates, wire, circuits, ceramics and alloys each have a prepared example and a supplies chest.

## Validation

Initial validation before the owner-requested speed/toggle revision: 20 October 2026, Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25 and the unchanged pinned Complete-pack libraries:

- `runGameTest` filtered to `jugcraft-test:*starter*workshop*`: all 10 passed. Includes real crafting-grid selection (and preserved vanilla clay blocks), all nine manual receipts, three kiln receipts, three alloys, fuel remainders/credit, saved partial work, cancellation/backpressure, concurrent mock-player requests, permissions/feature disabling, Fabric inventory transfers and both multiblock halves' placement/removal/drops.
- `runGameTest` filtered to `jugcraft-test:machine_consolidation_game_tests*`: all 8 passed, covering the existing machines sharing the durable work ledger.
- `test_starter_workshop.py`: 2 passed; `test_survival_recipes.py`: 5 passed; `test_agriculture_recipe_loops.py`: 8 passed. The actual registry export adds exactly the 11 workshop entries and zinc plate, with no removals.
- `tools/check_mod_data.py`: passed, including 6,182 material IDs, recipes, tags, the tower-cost audit and model geometry. No new art exceptions. Generated resources refreshed with `tools/generate_material_data.py`.
- `scripts/check_repository.py`, `scripts/package_modrinth.py check` (21 locked libraries) and `git diff --check`: passed.

- `runClientGameTest -PclientTests=StarterWorkshopClientGameTests` and `jar`: passed. Actual mouse-held work completed an iron plate, releasing paused work, all three station menus rendered, and saving/reopening preserved a new partial plate, leftover fuel credit and four bronze ingots. Six screenshots were inspected; the optional minimap no longer obscures the workshop menus.
- Export cleanup verified 92 save files and scanned 2,591 saved region records. Only the known unused test datapack selections and five test registry-presence entries were removed from a separate delivery copy; all other saved data was preserved. The original export is retained.

Delivery packaging records the source commit, matching mod hash, locked library list and archive verification separately with the inspection world. This is not evidence of two independently authenticated clients, a clean external-launcher import, a production-size performance benchmark or the full game-test suite. Batch 2 remains excluded.

## Owner revision: toggle and shorter batches

The owner requested click-to-toggle manual work: Start Work remains active through mouse release and successive batches; Stop Work, Cancel batch, or leaving the menu disables it. Reopening starts disabled. Hammering and wire cutting take 100 ticks (5 seconds); circuits take 200 ticks (10 seconds). Bronze and brass take 200 ticks (10 seconds); solder takes 120 ticks (6 seconds). Kiln firing times are unchanged. This supersedes the original four-times-powered-speed proposal. Powered recipes and material yields are unchanged.

Revision validation: all 10 focused workshop server tests and both workshop Python tests passed. Recipe, handbook, JEI timing and toggle-label generator comparisons passed. The focused real-client test and JAR build passed: clicking enabled work after mouse release, clicking again paused it, closing the menu stopped it, reopening left it disabled, and save/reopen retained partial progress, fuel and completed bronze. The refreshed inspection export preserves 91 files, with 2,563 saved region records checked during removal of unused test metadata from its separate delivery copy. The full test suite was not rerun for this revision.

## Initial sound revision (superseded below)

Each explicit Start Work click arms at most three quiet tool cues, spaced 16 active client ticks apart. The screen waits for server-confirmed active work; missing ingredients, tools or heat and blocked output stay quiet. Stop, cancellation or leaving the screen prevents further cues. Successive batches do not restart the phrase. Hammering uses vanilla anvil hits, wire cutting uses shearing snips, and soldering uses a higher-pitched extinguishing hiss, all at 0.12 volume on the Blocks sound slider and audible only to the operating client. The shared owner-library catalog was searched for suitable workshop audio; this change references existing vanilla sound events and adds no audio files or dependencies. Cable recipe quantities are unchanged pending owner balance selection.

Validation: client compilation and the existing focused `StarterWorkshopClientGameTests` run passed with the sound calls active, covering manual start/stop, menu closure, completion and save/reopen. No server behavior or recipe data changed, so the server suite was not rerun. Perceived loudness and sound preference remain for owner listening review.

## Owner revision: cable balance and one sound per batch

The direct recipe now yields four copper cables from two copper ingots and one tin ingot. The workshop route now yields eight cables from six copper wires, one tin and two ceramic insulators; the cable-insulation route yields sixteen. This explicitly supersedes the earlier promise to preserve the direct recipe quantity. The tower recipe-cost audit passes without changing its tolerances or tower costs.

Manual tool sounds now play once when the server commits a new batch, including successive batches while Start Work remains enabled. Volume rises from 0.12 to 0.5 on the Blocks slider. Only the operating player receives the cue. A paused or saved partial batch retains its work and does not replay the sound on resumption. Failed starts, missing ingredients and blocked output remain silent. The earlier three-cue client schedule is removed. No new audio assets or dependencies are added; the same vanilla anvil, shearing and extinguishing events remain in use.

Revision validation: all 10 workshop server tests passed, including actual crafting-grid selection for all three cable recipes. All three workshop Python tests and eight recipe-loop regressions passed. The cable recipes and handbook match their generators; the tower cost audit and whitespace checks pass. The focused workshop client run exercised the sound packet path, start/stop, completion and save/reopen successfully. Listening preference remains for owner review; no full game-test suite was run.

## PR #442 CI corrections (20 October 2026)

Foundation failed its authored-item alchemy coverage check because the committed catalog predated the 12 new workshop item definitions. Regenerated `data/jugcraft/alchemy/values.json` with `tools/build_alchemy_values.py` and the pinned Minecraft 26.3 extracted server JAR. All 12 additions have recipe-derived records; the same generator also refreshes 511 existing records against the current recipes, including the approved cable outputs. No seeds, valuation rules or conservation assertions were changed.

Build run 39520358008 exhausted the mod job's 30-minute wall-clock budget: generation and data checks consumed about 11 minutes before compilation/testing, and the server suite reached its final 24-test batch with three tests still waiting at cancellation. Those were the Rainforest tap scheduling, partial-progress and stopped-tap restart tests, whose original callbacks waited 1,205 or 2,410 ticks. The log reported a 216-second tick backlog; it did not report a failed gameplay assertion. Client checks and the optional-integrations-absent job passed.

The tap tests now inspect the actual chunk scheduler for exactly one tick at the production deadline, check that upgrades and bottling retain that exact deadline, then reschedule only their own verified fixture tick eight ticks ahead. Normal server dispatch still performs filling and restart, with early-fill, exact quantity, bottle consumption, partial progress and full-tap stopping assertions retained. No global clock, gameplay interval, CI timeout or test selection was changed. Batch 2 remains excluded.

Focused validation: `python -m unittest discover -s scripts/tests -p test_alchemy_values.py -v` passed all 10 tests. `runGameTest` filtered to `jugcraft-test:rainforest_game_tests*` passed all 13 tests in 4.889 seconds of game-test execution, using the pinned Complete-pack dependencies and regenerated catalog. The full server suite and rendered client suite were not rerun locally; the fresh CI run must confirm the full job finishes within its existing budget.
