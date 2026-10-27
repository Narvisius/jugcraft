# Companion tree-tap collection and spawn balance

Owner-requested implementation, 23 October 2026, with OpenAI Codex (GPT-6).

## Behavior and progression

Peepo and Jughead use spawn weights 12 in main habitats and 8 in secondary habitats. Existing biome tags, groups of 1–4, natural-only Overworld placement, the passive-creature category and six-per-type/128-block local cap remain unchanged.

A placed Tree Tap is one Planner work assignment. Existing Supply brings glass bottles (eight unreserved empties total); existing Output receives marked latex bottles. The native tap has one bottle of capacity, or eight with its manually installed bucket upgrade. A 40-tick interaction at 4 reserve JE/t (160 JE per batch) collects as many complete bottles as available bottles and cargo allow. Normal delivery effort applies. Fractional fill remains. No bucket upgrades, production acceleration, new recipes or new assets are added. Rubber trees and glass manufacture supply this early-industrial job; latex feeds existing rubber processing and downstream machinery parts.

## Integration and server behavior

Uses CompanionJob, existing per-server search/path budgets, normal priorities/rest/orders, eight-slot cargo, provenance and Supply/Output modes. Each companion caches at most its four assigned tap jobs. At most nine front/side standing candidates are tested once per second; no tree scans, chunk loads or production ticker are introduced. Raised taps are reachable only from safe ground within the normal short reach. The existing companion path evaluator treats the tap and its overhead cell as obstacles, so delivery paths go around the partial collision shape instead of pressing into the tap or bucket. A bounded level-keyed, weak map holds UUID/100-tick leases; 64-entry rotating cleanup bounds stale-claim work at the 4096-entry ceiling. A lease stops two helpers animating on the same tap. Native player interactions remain available; completion rereads the actual fill and inventory.

Agriculture enablement, mob griefing, assignment identity and live owner/town permissions gate collection; native production remains independent. Bottle consumption, marked output insertion, final energy debit and tap fill share a transaction. Rollback restores the fill state; refill scheduling resumes only on final commit, preserving an existing pending fill tick. Full cargo leaves the tap untouched; collected goods survive blocked Output and use normal saved cargo/provenance. New status values are appended; no saved tap properties, IDs or inventory formats change. Interrupted work loses progress but never creates a free bottle or loses latex.

The branch was reconciled with upstream main. The land-permission merge retains upstream player permissions and the pre-existing companion-owner permission method separately; it does not extend automation access through party membership.

## Validation

Focused client game tests cover native extraction rollback/over-extraction, assignment recognition, bottle-only supply demand, live claim denial (including a cached supply port), bulk collection and exact per-tick energy/timing, competing helpers, paths around tap hardware, full cargo and freed bottle slots, tap removal, retained local spawn cap, and real Supply-to-tap-to-Output routes for Peepo and Jughead. `./build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoTreeTapClientTests','jar')` passed the focused client test with **76 assertions**. Only the tree-tap client test class was selected. Its route regression caught and now covers companions walking into the partial tap/bucket collision shape. Collection timing uses actual server ticks, respecting the existing per-tick energy-output cap. The focused test/build command completed successfully in 2m 37s and produced `build/libs/jugcraft-0.1.0-alpha.jar`. `python scripts/check_repository.py` and `git diff --check` passed. No full unrelated game suite or two-client multiplayer load test is claimed.

The local Complete distribution was rebuilt from that tested JAR. `python scripts/package_modrinth.py check` passed with 26 locked libraries, and `python -m unittest discover -s scripts/tests -p test_packaging.py -v` passed all 13 packaging checks. Archive integrity, the pack checksum, the embedded JAR hash and the updater inventory were verified. Launcher import and the owner's manual playtest remain separate checks; generated packages are excluded from Git.
