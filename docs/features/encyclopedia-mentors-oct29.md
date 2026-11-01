# Unified Encyclopedia and starter mentors — October 29

Owner-directed local work, prepared with OpenAI Codex (GPT-6). Existing dirty
`feature/living-city-local` checkout; all prior asset, aircraft and raid fixes preserved.
No commits, publication, launcher changes or live-server operations.

## Player outcome

One Encyclopedia, available with its remappable key and inventory button, contains
separate Technology and Magic route directories and every delivered topic in its
scrolling contents. Technology covers all existing handbook chapters plus digital
workshops and imported manuals. Magic now includes Concordance, Jug Nouveau,
transmutation/equipment, imported spellbook manuals and discovered expeditions.
The previous 11-page transport limit could omit late registered guides; it is
now bounded at 24 pages for the 17 current server contributors.

Shared starter lessons explain materials, a first powered furnace, sockets and
troubleshooting; optional factory specialties; independent magic entry paths;
casting versus workshop charge versus essence; and convenience-only transmutation.
Recipes are read from the server via PageWriter, including unavailable-recipe
messages. Inventory counts describe current carried items, never fabricated
permanent quest completion. No rewards, new research requirements or currency
changes. Detailed handbook/reference screens remain reusable children of the
Encyclopedia, with return navigation.

Existing residents are mentors: Kester/General Store for technology, Linnea/Florist
for magic, Ivo/Curios for transmutation, and Mayor Osric for the directory. Shops
have Ask mentor; sneak-interaction opens the same lesson while normal shop access
is preserved. Academy consultation remains unchanged unless sneaking for the
shared starter lesson. Sleeping NPCs cannot be consulted; the book remains usable.
Mentor requests validate proximity, living/awake state, line of sight and rate;
reading spends no inventory or money. No town blueprint or resident IDs change.

## Validation

Implementation complete. Local native checks passed: all four
`StarterGuidanceGameTests` and all 24 existing `EncyclopediaGameTests`.
These cover full-library transport, live recipes, missing-recipe behavior,
personal inventory isolation, and free and distance/rate-limited mentoring.
Repository checks and the targeted generated Encyclopedia/route resource audit
also passed. The combined rendered run passed all three selected classes:
`EncyclopediaClientGameTests`, `EncyclopediaRoutesClientGameTests`, and
`StarterGuidanceClientGameTests`. This exercised actual shop-button network flow
for all three shop mentors, both routes and reference/back navigation, the
existing dedicated-server guide walkthrough, key/inventory access, and different
GUI sizes. Home, starter routes and mentor lesson/shop screenshots were inspected.
The additional focused recipe-grid scrolling/render check also passed; the
actual Bronze Blend grid and Machine Casing recipe were inspected in the book.

Commands used (pinned Java 25, local offline Gradle cache):
- `runGameTest` filtered to `jugcraft-test:*guidance_game_tests*`: 4 passed.
- `runGameTest` filtered to `jugcraft-test:encyclopedia_game_tests*`: 24 passed.
- `runClientGameTest -PclientTests=StarterGuidanceClientGameTests,EncyclopediaRoutesClientGameTests,EncyclopediaClientGameTests`: passed, 3m15s.
- `runClientGameTest -PclientTests=StarterGuidanceClientGameTests`: passed, 2m36s,
  including the added assertion that the recipe is reachable in the scrolling
  lesson and its rendered screenshot.
- `python scripts/check_repository.py`: passed.
- Targeted `tools/check_mod_data.py` Encyclopedia and routes audit: 0 errors.

Logs are under `build/encyclopedia-mentors*.log` and
`build/encyclopedia-regression-server.log`; the combined client log and 44
screenshots are retained under `build/encyclopedia-mentors/`, alongside the final
13 focused screenshots in `recipe-screenshots/` (ignored build output).
Human multiplayer playtesting and the full release suite were not run; no server
or personal Modrinth profile was touched.

## Integration note

Open PRs #503 (first-join welcome) and #506 (contents/topic/navigation repairs)
were read for overlap. They have not been merged or imported as part of this
local task. This batch touches the same Encyclopedia files; retain the complete
scrolling directory, delayed-page opening and mentor lessons when reconciling
those PRs later. Their remaining welcome/keyboard/topic fixes should be reviewed
alongside this batch rather than replacing these files wholesale.

## Limits

This is a unified directory over current implemented guides, not a claim that
every registered item has an exhaustive bespoke article. Deep research reference
and journal retain their existing screens/discovery rules. Full quest rewards,
pan/zoom dependency graphs and progression balancing remain separate TODO work.
Existing source art/UI is reused without new textures or dependencies.
