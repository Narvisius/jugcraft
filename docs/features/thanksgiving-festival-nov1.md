# Thanksgiving festival — November 1 owner request

Prepared with OpenAI Codex (GPT-6). Local development only; no publication or server maintenance authorized in this task.

## Full requested scope

- Pilgrim costumes for players and November spawn-city citizens. Preserve citizen identities, equipment, jobs and saved appearances; seasonal presentation must revert without deleting earned costumes.
- Temporary Native American-inspired raider outfits using the owner's November 1 visual brief: geometric face/torso paint, feathered headwear or beaded headbands, loincloths, leather leggings/moccasins, bead/bone chokers and armbands. Owner also referenced bareback horses, bows, tomahawks, spears and repeating rifles. This is a costume/design brief, not a claim of one historically uniform clothing tradition. Keep existing city targeting and protected guards.
- Thanksgiving Day raid truce using the server's existing season calendar/timezone. Interpret as the holiday itself (US 2026: November 26), not a permanent raid shutdown. Prevent new raids and withdraw existing raids without victory rewards; cover unloaded members and raid horns.
- Seamless indefinitely extensible tablecloth tables, reusing FeastTableBlock's existing single/start/middle/end neighbor connections. No scan of the full table each tick. Preserve stored dishes when changing cloth.
- Repair placed Pumpkin Soup, Vegetable Soup, Ratatouille, Onion Soup, all pasta variants, Bacon and Eggs, Grilled Salmon, Roasted Mutton Chops, Steak and Potatoes, and Mushroom Rice.
- Audit every edible item for placement coverage, then give the remaining foods appropriate dish/loaf/fruit/serving models and place/pickup/save/drop behavior. Do not label an inventory sprite copied onto a box as a completed meal model.
- Thanksgiving dishes not already present. Inventory existing corn, turkey, pies, stuffing and feast recipes before adding duplicates. Cranberry sauce and cornbread stuffing already exist. Candidate gaps: mashed potatoes and gravy, green-bean casserole, candied sweet potatoes, dinner rolls; recipes must use reachable ingredients and conserve portions.
- A giant multiblock turkey feast with serving interactions, safe assembly/removal, one drop and no duplicate food.
- A larger cornucopia centerpiece for the town square until December 1. Use TownDecor's saved compare-and-set ownership protection; retain owner changes, including church slot machines. Do not regenerate town chunks.
- Playable two-person chess: legal moves/check, castling, en passant, promotion choice, checkmate/stalemate, draw handling, resignation and reconnect behavior.
- Playable two-person checkers: explicit American/English rules, compulsory captures, chained jumps, kings and game-over/draw handling.
- Optional agreed Jugs wagers through existing Exchange escrow. Both players accept the same stake; no real-money payments, automatic stake escalation or duplicate payout. Cover restart, disconnect, block breaking, cancellation, draws and inventory/finance failures. Free games must work without a wager.

## Existing systems found

`MenuDishes` already registers the named placed foods; `tools/menu_data.py` projects regions of the owner's icons onto generic dish boxes. The reported problem is presentation, not missing registration. The existing FeastTableBlock already joins neighboring segments along either horizontal axis. `giant_cornucopia` is currently 3x2x2. TownDecor owns a 7x7x13 centerpiece volume and already preserves manual overrides. The season calendar already calculates US/Canadian Thanksgiving, while raiders currently ignore that date. Poker provides established Jugs escrow behavior to reuse, not a second currency.

## November joke-event ideas (proposals, not implemented)

Keep the theme literal, opt-in and non-explicit:
- **No Nut November:** optional nut-free feast recipe challenge, with a tiny squirrel referee and a cosmetic walnut trophy. Normal foods remain usable; no policing private behavior.
- **The Great Acorn Heist:** mischievous squirrels steal event acorn tokens from public event props, never player inventories or builds. Return the haul for banners/cosmetics.
- **Nutcracker Training:** target-practice minigame with wooden walnuts and a November scoreboard; cosmetic prizes rather than money printing.
- **Squirrel-Proof Pantry:** cooperative town delivery goal for nut-free dishes, with capped daily rewards and a shared feast decoration milestone.
- **A Tough Nut to Crack:** a harmless puzzle chest that rewards a walnut hat, title or emote.

## Progress and validation

Planning/integration inventory recorded. Implementation and tests must be recorded below as completed; the list above is not a claim that the content exists.

### Local implementation started

- 27 bowl/plate models now use solid vessels and individually modeled ingredients, with opaque material texels sampled from the unchanged owner icons. Named soups, pasta variants, eggs/bacon, salmon, mutton, steak/potatoes, ratatouille and mushroom rice covered. Source: originals/Blocks/farming and food textures. No raster images edited. Rendered review passed November 1; see phase 1 evidence below.
- A linen tablecloth variant extends the existing unlimited neighbor-connected Feast Table. Apply white carpet, swap with bat runners or remove with shears; previous cloth is returned and stored dishes retained. Joined internal ends have no drapes. New drop/state/model resources generated. Phase 1 tests passed; duplicate-carpet fallback repaired.
- Calendar computes a one-day truce independently of the longer feast event; both raid-start overloads/horns are blocked and active raids withdraw without winning rewards. Calendar and live raid-transition tests passed in phase 1.
- The remaining full-scope items above are still unimplemented. Do not describe this first batch as the finished festival.

### November 1 clarification and checks

Owner supplied the outfit visual direction directly: geometric paint on face/torso, feathered headwear or beaded headbands, leather leggings/moccasins, loincloths, and bead/bone accessories. Use multiple variants and retain each NPC's identity. Horseback riding and listed weapon types are reference details; new mounts/combat behavior are not implemented by this costume brief. Clothing art and runtime seasonal overlays remain outstanding.

Three focused Python presentation tests pass (`scripts/tests/test_feast_presentation.py`): all 27 generated bowl/plate models match their generator, use fully opaque material texels and stay within their collision bounds; linen end drapes appear only at exposed ends; every axis/connection variant resolves its model. This is resource validation, not an in-game visual approval.

Compilation exposed the old `Items.WHITE_CARPET` constant, corrected to Minecraft 26.3's `Blocks.CARPET.white().asItem()`. Main, client and game-test Java sources now compile successfully (`compileJava compileClientJava compileGametestJava -x processResources`, build/nov1-feast-compile-only.log; 2m24s). Resource processing was excluded for this source-only check. At that initial compile checkpoint, game tests and rendered review were pending; phase 1 results below supersede that status. Nothing published or deployed.

## Phase 1 completed locally — November 1, 2026

Scope: validate the existing food-model corrections, linen Feast Tables and one-day Thanksgiving raid truce. This does **not** complete the rest of the festival.

### Repair found during testing

Repeatedly applying white carpet to a linen table returned `FAIL`, allowing the carpet BlockItem's normal placement to run afterward and consume another carpet. The table now handles that click with `SUCCESS` and consumes nothing. The regression test exercises this through the actual server player interaction path, not a direct call to the private dressing method.

### Final evidence

- **3 Python resource tests passed:** `python -m unittest discover -s scripts/tests -p test_feast_presentation.py -v`. All 27 meal models match their generator, have opaque material UVs and fit their bounds; linen geometry has exterior end drapes only and both-axis state/model coverage.
- **5 server game tests passed:** `FestivalRegressionGameTests`, filter `jugcraft-test:festival_regression_game_tests*`. Covers US calendar dates 2026–2032, Canadian date, disabled holiday/date preview; an active raid withdrawing when the holiday begins, both start overloads refusing new raids, horn preservation and no victory-level increase; cloth swapping/repeated clicks/shearing/break drops with food retained; 15-segment linen tables joining and splitting on X and Z; existing dish placement/pickup/drop regression. This is a disposable server with mock players, not a human multiplayer session. Unloaded-chunk reload behavior still uses the existing RaidMember inactivity check; that specific unload/reload path was not newly exercised.
- **2 rendered client classes passed:** `runClientGameTest -PclientTests=MenuClientGameTests,PartyTableClientGameTests -x processResources --offline --no-daemon --max-workers=2`, run separately without the server filter environment. Successful in 4m10s. All 27 revised meals were reviewed in nine three-dish screenshots, plus connected linen tables on both axes and after removing a center segment. Solid vessels/food surfaces, no missing-texture checkerboards in these assets, continuous cloth joins and correct new exterior ends observed. This is automated in-game visual review, not owner artistic acceptance.
- **22 screenshots preserved:** `build/nov1-festival-review/screenshots/`. Representative views: `0004_jugcraft_linen_table_connected.png`, `0006_jugcraft_linen_table_split.png`, and `0013`–`0021_jugcraft_revised_meals_*.png`.
- Logs: `build/nov1-festival-review/server-passed-excerpt.log`, `nov1-festival-combined.log`, `nov1-festival-client.log`. The combined command passed all server tests but its client harness stalled before running the gallery. Its test process alone was stopped; the independent visual rerun passed. The first launch also found incomplete local processed resources; a full resource rebuild repaired the test output before the passing gameplay run. These were local test-run issues, not changes to a deployed pack.

The 27 models are deliberately simple Minecraft-scale solid vessels and ingredient shapes using the original icons' palettes. No source textures were overwritten. Table length is visually extensible by neighbor connections; the existing bounded feast-bonus scan is retained.

No commits, pushes, release artifacts, personal Modrinth profile changes or live-server actions. The next phase is the remaining edible-item placement audit/coverage.

## Phase 2 — Remaining food placement (completed locally)

Implemented with OpenAI Codex. Runtime food-component audit found 471 edible items: 397 now have dedicated serving blocks; 70 existing menu dishes, three native Household food blocks and the existing pumpkin pie retain their placement. This covers the current Minecraft/Jugcraft registries, not arbitrary future third-party items. The runtime coverage test fails when a new edible is added without a placement route.

- Sneak-use a food on a supported surface to set one serving down; use an empty hand to pick it back up. Ordinary eating/planting remains the default. No research unlock required.
- Full item components are stored in the existing Showcase serialization system. Names and food data are retained; block breaking/support loss yields one stored item, with empty default loot tables to prevent duplication. No block entity tick work or per-tick registry scans.
- `tools/food_servings_catalog.json` records all 397 original item-model/texture references and explicitly assigned model families. The resolved item models were flat icons. New closed cuboid geometry provides bowls, plates, produce, loaves, cakes, slices, pies, pancakes, sushi, fish/fillets/shellfish, sweets, bottles and other servings using unchanged source-icon material texels. Existing native food-block models are preserved. No PNG/source-library edits.
- Stable block IDs: `jugcraft:food_serving/<item namespace>/<item path>`. Display blocks do not create duplicate creative inventory items. Outlines follow modeled bounds and horizontal facing.
- Common resource generator includes the serving catalog and resources; generated-resource pruning retains them. Existing land/town protection checks apply to placement/pickup.

Validation so far: two resource tests passed across all 397 models; first two server tests passed, covering registry coverage and all 397 survival placement/pickup round trips plus single-drop break/support-loss behavior. The expanded three-test server run passed in 3m15s (`build/nov1-food-servings-final-server.log`), including creative counts, unsupported placement and serialized component retention. The corrected full-catalog client gallery passed in 4m51s (`build/nov1-food-servings-final-client.log`), including Feast Tables. Representative screenshots were reviewed for geometry and textures. The final fixture-only rerun also passed and clears dropped items between gallery batches. Nothing published or deployed.


The first client gallery exposed Minecraft 26.3 rejecting item-atlas sprites on block models. Fixed with explicit block-atlas sprite aliases (`jugcraft:block/food_serving_material/...`) pointing to the original texture resources. No copied/modified PNGs. The asset test now requires those exact aliases, rather than assuming an item sprite is usable by a block. New servings on Feast Tables also submit their native block geometry in place of the flat inventory icon. Table inventory/eating mechanics are unchanged.


Phase 2 player controls: **sneak + right-click** a supported surface while holding food to place one serving; **empty-hand right-click** retrieves it. On a Feast Table, normal food serving/eating controls are unchanged. No research requirement. Only the 397 newly covered foods switch the table renderer to these solid serving models; existing native feast/menu presentations remain intact.

Generation: `python tools/food_servings.py` uses the committed reviewed catalog, unchanged Jugcraft runtime art, and the pinned 26.3 client JAR in the existing Fabric Loom cache to read vanilla material texels. It never runs source-library programs or rewrites PNGs. This is 29 reusable shape families with item-specific palettes, not 397 individually sculpted unique meshes. No human multiplayer or owner artistic acceptance is claimed. This phase adds placement/presentation; the later new feast recipes, giant turkey, costumes and games remain separate unfinished festival work.


Final clean gallery: **35 screenshots**, covering all 397 new servings and their Feast Table presentation, saved in `build/nov1-food-servings-review/screenshots/`. Representative vanilla, sweet/drink, cake/pie slice, imported fish/meat, candy and Feast Table shots visually inspected after the atlas repair: solid geometry, no missing-texture checkerboards in those shots. No new food-serving model rejections in the final client log. Logs for the two resource tests, three server tests and clean client gallery are preserved alongside the screenshots. These checks do not certify every artistic detail of every food; owner visual feedback remains welcome. All work remains local.


## Phases 3–5 — November 2 local implementation

Implemented with Codex (GPT-6), using native Java/JSON geometry and existing runtime materials; no source-library programs executed or original PNGs altered.

- Four previously absent dishes: mashed potatoes and gravy, harvest pea casserole, candied sweet potatoes, dinner rolls. The casserole uses the existing pea crop because the registry has no green beans. Three meals use the Cooking Pot and return a bowl when eaten; bread + sugar produces two rolls. All four have solid serving models and work with the existing placement/table system. Existing cranberry sauce, stuffing, corn and turkey recipes are reused, not duplicated.
- Giant Turkey Feast: 3 x 3 footprint, two blocks reserved vertically, 24 portions from four six-portion roasts. Four visible carving stages; knife carves the existing turkey slice, empty hand eats when hungry, sneak-empty-hand packs it. Master-only loot copies the remaining count, including zero. Breaking a secondary part removes the entire feast and drops one packed feast; blocked placement and protected cells refuse construction. Piston movement is disabled. A comparator reports the remaining fraction. No refill from pickup or re-placement.
- Two player head-slot costumes (Pilgrim coat and dress) are craftable and usable year-round without research. Automatic November citizen outfits and three Native American-inspired raider clothing variants are client-rendered layers attached to the existing animated body parts. Existing skin/face, name, role, gear, AI and saved identity are retained. This is a stylized response to the owner's clothing brief, not a claim of one historically uniform tradition. The brief's mounts/weapons do not change existing raid combat. Seasonal presentation uses the server's configured calendar/date preview and reverts December 1; earned costumes remain usable. No city blocks are modified.
- Season sync now includes the November-costume flag; a future release must update client and server together. Disconnect clears the client flag.

Validation in progress: five Python asset checks passed (401 food placements, all harvest materials, complete turkey state/facing models and portion-conserving source recipe). Common Java compile passed after correcting 26.3 API names; final full compile, gameplay tests and rendered review are still pending. Nothing committed, published, installed in personal profiles or deployed.


Phases 3–5 gameplay checkpoint: common/client/test compilation passed. Seven focused `HarvestFestivalGameTests` passed in a disposable local server, including the three existing food-serving regression tests across all 401 servings, actual ingredient matching for all three new Cooking Pot meals, November boundaries, 24-slice exhaustion, packing a zero-portion carcass, all four facing/secondary-break cases, 11-portion re-placement, and obstructed placement without consumption. Six Python resource checks passed (two food, four harvest). Existing unrelated imported-loot warnings remain; the custom Cooking Pot recipes emit the same vanilla recipe-book placement warning as existing pot recipes, but the actual Cooking Pot matcher passed. Rendered client review is pending. Test resources were explicitly synchronized to the existing complete output before `-x processResources`; no tests used a stale harvest resource set.


Rendered review: the first client run passed and showed both player costumes, the automatic citizen/raider overlay, the four dishes and full/partly carved/empty turkey. Refined the roll to reuse the existing baked-crust material instead of the overly bright cornbread material. Expanded the gallery to all three raider roles. Their uniform cap/goggles belong to the base model's outer head shell; the November model now temporarily hides that shell while the costume supplies its own hair/headwear, restoring it outside the season. Equipped armor and actual entity equipment remain untouched. The final clean screenshot rerun is pending at this checkpoint.


### Phases 3–5 complete locally

Final `runClientGameTest -PclientTests=HarvestFestivalClientGameTests -x processResources --offline --no-daemon --max-workers=2` passed (3m44s). Five clean screenshots are saved in `build/nov2-harvest-review/final/screenshots/`: four dishes; whole/partly carved/empty giant turkey; player and citizen costumes; all three raider infantry variants; normal raider appearances restored. Reviewed the final costume/variant/reversion images directly, plus the earlier unchanged turkey geometry and the refined dishes. No missing-texture checkerboards observed in these assets. The uniform outer head shell is absent only during the seasonal overlay and visibly restored afterwards. Player costumes remain independently wearable. The test fixture uses discarded test mannequins/NPCs, not death effects or a personal save.

Final validation: six Python resource checks; seven focused server tests; common/client/game-test Java compilation; rendered client review. Logs and initial/refinement/final screenshots are preserved under `build/nov2-harvest-review/`. This is automated local validation, not a human multiplayer session or the owner's artistic acceptance. No commits, pushes, PRs, releases, client updater changes, personal Modrinth profile changes, town regeneration or live-server actions. Remaining festival phases (town cornucopia, board games/wagers and optional November activities) are not claimed complete here.


## Phase 6 — Prepared, deliberately held (owner instruction November 2)

Do NOT implement or activate the new city decoration replacement yet. November 3–4 is a tentative target, not an automatic switch or deployment authorization. Wait until the Halloween decoration period has actually finished and the owner resumes this step. No scheduled automation, town template edits or date-triggered migration is being added now.

Prepared design: a larger woven cornucopia centered on the existing square site, with a grounded horn, sculpted squash/corn/fruit and warm lantern accents. Target an approximately 5×3×5 central composition inside the existing owned centerpiece volume; confirm exact slot bounds before modeling. Place connected linen Feast Tables and the new dishes at existing decoration sites without blocking paths, market stalls, doors or guard sightlines. The giant turkey can be the communal serving prop, with protected packing/breaking and public eating. Do not replenish serving inventory automatically.

Future implementation checklist: inspect the actual Halloween calendar end and current saved city; snapshot the existing scene; use TownDecor's saved compare-and-set ownership ledger; skip and report every owner-modified cell rather than overwrite it; preserve church slot machines and all unrelated placed blocks; stage a dry-run report before mutation; test interrupted/repeated upgrades, loaded/unloaded chunks and owner overrides. Switch only after Halloween is done, retain the harvest scene through November, and hand back to the December scene on December 1. No chunk regeneration. Any later live restart still requires the owner's separate confirmation and 30-second warning.

## Phases 7–8 — Completed and validated locally

Checkers and chess share a server-authoritative table interface, UUID seats and persisted game state. This phase 7–8 checkpoint originally supplied free play; phase 9 below adds optional wagers. Closing the interface preserves a seat; leaving an active seat is resignation, while a disconnect simply preserves the game for reconnection. Breaking an unfunded table cancels the match and drops only the craftable table; funded-table protections/refunds are described in phase 9. Rematches need both seated players' agreement. Spectators cannot move pieces. Network actions require a nearby opened table, matching table identity and state revision.

Rules: American/English checkers with compulsory captures, same-piece chained jumps, short-range kings and crowning ending the move. Chess includes king safety, castling, en passant, all four promotion choices, checkmate/stalemate, resignation and agreed draws. For casual play, threefold repetition and the 50-move rule end chess automatically rather than require a formal claim; checkers automatically draws after threefold repetition or 40 moves per side without capture/man advance. No move clocks or automatic disconnect forfeits. Core engine tests currently pass 31 assertions, including initial chess perft 20/400/8902. In-game/network/persistence and rendered checks remain pending.


Board-game player controls: craft `jugcraft:checkers_table` or `jugcraft:chess_table`, place it, and empty-hand right-click. Choose **Take an open seat**; a second player does the same. Click a piece and a green highlighted destination. Promotion uses four buttons. The **Rules & help** panel explains the rule variant. Nearby players can open the table to watch; actual carved pieces also reflect the match in-world. Closing the UI or disconnecting retains the seat and position; **Leave seat** forfeits an active match, with confirmation. There are no bank-funded prizes, research locks or move clocks; optional wagers are described in phase 9. An abandoned table may be broken/replaced by someone authorized to build there; doing so ends its match.

Recipes: top row white dye / ink sac / white dye for checkers, or quartz / ink sac / quartz for chess; middle row three planks; bottom row stick / empty / stick. Each produces one table. Planks accept the vanilla planks tag. No separate consumable chess pieces are needed.

Validation checkpoint: all four new server tests passed, including identity/turn rejection, persistence and chained jumps, remote/stale/unopened requests, town public-interaction classification and a single-table break drop. Two resource checks passed. The first compile found only a test-helper signature mismatch, which was corrected. Client rendering/input validation is running in a disposable world; no owner playtest has been controlled.


Rendered/input checkpoint: the corrected client run passed in 3m04s. It exercised the real client UI/network path for taking a seat, e2–e4 and synchronization to the in-world pieces, choosing a knight promotion, and two compulsory checkers jumps; the opposing seat was a controlled test UUID, not a second human client. Eleven screenshots cover the world tables, empty/occupied seats, legal destinations, promotion and normal/compact interfaces. The first failed attempt used coordinate-based simulated mouse clicks which did not activate the seat button; switching to the Fabric button helper and direct GUI mouse events exercised the intended controls successfully. No production permission bypass was added.

Final presentation refinement: queen geometry is distinct from rook geometry; compact layout reserves enough vertical space for all controls; help hides board coordinate labels instead of leaving them behind the text. The focused client rerun for that last cleanup is pending.

Known unrelated resource warning observed during this run: the earlier phase 3–5 `harvest_costume` equipment JSON has an empty layers map rejected by the equipment resource parser. Custom costume rendering was already validated separately, but this metadata warning should be cleaned before publishing the festival batch; it does not concern the board-game assets.


### Phases 7–8 final validation — November 2

The final client run passed in 2m29s after the help-panel cleanup. Reviewed the clean in-world tables, chess selection/promotion, and compact checkers board/help screenshots. Eleven final screenshots and logs are preserved under `build/nov2-board-games-review/final/`; earlier failed input-fixture and passing intermediate evidence are retained separately.

Completed: craftable checkers/chess tables, native wood/stone models, distinct carved pieces with live world-state synchronization, shared click-only interface, legal-move highlights, promotion buttons, rules/help, two UUID seats, nearby spectators, server-checked actions, saved matches, confirmed resignation/leaving, agreed draws and consensual rematches. Town public-interaction classification allows these non-container tables; land-claim permissions still apply.

Validation: **31 rules assertions** (including initial chess perft 20/400/8902); **4 server game tests**; **2 resource tests**; successful common/client/test compilation and the focused rendered/input client test for both games. `boardRulesTest` is hooked into Gradle `check`. Resources were regenerated and explicitly synchronized into the existing complete test output before using `-x processResources`. The test uses one real client and a controlled second-seat UUID; a two-human play session has not been performed. No broad full-project CI claim.

Phase 6 remains deliberately unimplemented and held until Halloween decoration is finished and the owner resumes that work. Wagers remain phase 9. No commits, pushes, PRs, releases, client-updater changes, personal Modrinth profile changes, town mutations or live-server actions were performed.


## Phase 9 — Optional board-game wagers (completed locally, November 2)

Implemented with OpenAI Codex. Free play remains the default. Before the first move, either seated player can propose 1, 5, 10, 25 or 50 Jugs each using the **Optional wager** panel. Both must explicitly confirm the current stake, and both must still be online, nearby and authorized to use the table when the pot is funded. Changing the proposal clears both confirmations. Either can cancel an unfunded proposal back to free play. An insufficient second wallet rolls back the first stake; no loan or bank subsidy is created.

The Exchange holds both stakes. The winner receives exactly the combined pot with no fee; draws refund both. Resigning/leaving forfeits to the opponent. Closing the interface or disconnecting retains the match rather than forfeiting it. Rematches return to free play with fresh consent. Pending, unfunded confirmations are cleared on reload/disconnect; funded matches retain their wager ID. No items, real-world payments or spectator bets.

Ledger receipts bind the latest round to the dimension, position and persistent table identity; settled receipts prevent repeat payouts or a stale chunk consuming a newer round's pot. Existing ledgers load with no board receipts. Receipts and Exchange escrow share the finance save; actual Jugs balances continue using the existing separate Jugs save. Normal save/reload and stale receipt handling are tested; this does not claim cross-file power-loss atomicity beyond the existing economy's persistence guarantees. Full wallets retain winnings through existing pending Exchange credits.

Ordinary players cannot break a funded table, and tables resist explosions/pistons. Forced block removal refunds through the ledger, independently of whether the block entity still exists. Chunk unload does not refund. Unfinished pots expire after seven calendar days; the server checks once per minute, including after restart, without loading table chunks. A loaded expired/recovered table ends as a draw. Latest receipts are retained per placed table to prevent replay.

Validation in progress: combined board/rules regressions, escrow and save compatibility, two-player mock consent/payouts, cancellation/removal/expiry, plus rendered wager controls. No publication, server action or player-profile changes. Phase 6 remains held. November joke events below remain proposals only.


Phase 9 validation checkpoint: all nine combined board/wager server tests passed. Both existing Exchange regressions also passed, including money conservation and ledger serialization. The rendered client walkthrough passed with 13 screenshots covering wager selection, explicit first-player confirmation without debit, cancellation to free play, normal chess/checkers moves and compact interfaces. Reviewed both wager-panel screenshots directly. The final label wording distinguishes free games from funded stakes; final compile/server rerun adds the explicit funded-table NBT round trip. Evidence: `build/nov2-board-wagers-review/`. No two-human playtest or deployment is claimed.


**Final phase 9 result:** final common/client/game-test compilation and all nine board/wager server tests passed (1m52s), including the funded table NBT round trip. The two shared Exchange regressions and rendered/input client walkthrough passed in the preceding combined run (3m13s). Final changes after the screenshots only clarified the disabled free/funded labels and added that save assertion; those changes compiled successfully. No new models/assets/dependencies or town changes. Optional wagers are complete locally. All commits, publication, client updates and live-server maintenance remain untouched.
