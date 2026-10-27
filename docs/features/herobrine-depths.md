# Herobrine permanent underground discoveries

Owner approved on 27 October 2026; implementation by OpenAI Codex, paired with Claude's sky islands and surface ruins. This extends the existing event builder, not World Designer terrain generation. No live deployment is included.

## Player experience

- Nine cave layouts: redstone-torch trails, abandoned mining shelters with usable crafting table/furnace, watching rooms, stairs ending at sealed walls, geometric pillar grids, broken ravine bridges, balconies, buried shrines and empty columned halls. Rooms have entrances and navigable interiors.
- Four submerged glowstone patterns: rings, crosses, stepped pyramids and trails. Only source water is replaced; neither lava nor flowing water is accepted. The structures rest on the bottom rather than floating in the water.
- Selection varies by depth: upper caves favour shelters/trails; deep caves favour halls, watching rooms and spans; the bottom 32 blocks favour shrines and geometric chambers.
- All blocks persist indefinitely, through approach, logout and the November event cutoff. Only the apparition disappears. Players may explore, mine and adopt the structures. Operator undo remains available and preserves edited blocks and adopted containers; there is no automatic rebuild/reset.

## Discovery and protection

The existing per-player sighting scheduler remains unchanged. The world-bound exploration director additionally attempts a discovery after 180 active seconds underground or 900 active seconds on the surface. In quiet season these intervals are six times longer. AFK, unsafe, menu-open and offline time do not count. Placement is best-effort, not a guaranteed successful structure every interval. There is one search globally per ten seconds, at most six candidate columns per cave/water search, with failed searches waiting another minute. Surface selection can choose Claude's rarer sky islands or surface ruins. Existing records prevent same-depth repeats within 48 blocks underground or 64 blocks underwater, while allowing discoveries at least 20 blocks above/below. Reconnect/server restart resets the extra attempt clock conservatively; saved structure spacing persists.

The shared queue still limits all marks together to eight changed blocks per tick and two jobs. Site scans require all chunks already loaded, natural-looking surroundings, no containers, no claims and no town. Replacing/excavating rock requires operator-approved wilderness, excludes ores, and cannot open a liquid leak. Blocks are rechecked at placement. Additive cave rooms retain existing floor rather than replacing natural ground for appearance. The natural-block check remains a heuristic; unclaimed plain-stone structures are not provably natural, so excavation stays opt-in. No automatic wilderness approval is performed.

`/jugcraft herobrine mark <player> underground` and `underwater` search nearby; named cave/water variants place at the command's usual six-block offset if safe. Sky/surface group commands use the companion planners. Commands remain operator-only and Overworld-only.

## Economy / integration

Atmospheric exploration only, no research prerequisite, currency, loot chest, replenishing rewards or mandatory progression. The vanilla decorative materials and sparse glowstone are mineable once; placement spacing prevents repeated spawning at the same site. Built-in vanilla blocks reuse the installed game resources; no external texture assets or dependencies are introduced. Furnaces/crafting tables are usable normally. Existing saved marks remain compatible; new type names are strings in the same codec.

## Validation

Combined validation completed on 27 October 2026 at implementation commit `96097d3027`, including Claude's sky/surface planners:

- Compilation passed. `herobrineScheduleTest` passed all 393 assertions; the filtered native Herobrine suite passed all 70 required GameTests (49.97 seconds of server testing).
- Native coverage includes every layout's construction, SavedData round trips, 3D spacing, water/lava and container refusal, unloaded-chunk refusal, approval revocation/ore protection, approach permanence and adopted furnace inventory preservation. It also checks all six sky islands' full block states, signs and undo, all three surface ruins on grass, and claim/town protection.
- Both rendered client galleries passed together: `HerobrineDepthsClientTests` and `HerobrineLandmarksClientTests`. Their 32 screenshots cover all 22 new variants, cave interiors, ravine spans and island undersides. Reviewed the rendered layouts, including corrected chair direction, proper bridge/balcony gaps and surface placement on grass.
- Combined testing caught and fixed the 26.3 natural-soil tag mismatch, an undersized delayed-test timeout, and test assumptions about the arena heightmap. The final reruns are green.

Local evidence is retained under `build/reports/herobrine-coordination/`: `combined-native-2.log`, `combined-gallery-2.log`, and `gallery/`. Commands: `gradlew herobrineScheduleTest runGameTest --offline --no-daemon` with `JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=jugcraft-test:herobrine*`; then `gradlew runClientGameTest -PclientTests=HerobrineDepthsClientTests,HerobrineLandmarksClientTests -I scripts/owner-network.init.gradle --offline --no-daemon` without that environment variable.

The gallery uses controlled disposable terrain, not natural-world distribution or multiplayer load testing. Human traversal, live-server performance and deployment remain unverified. No personal Modrinth profile or live world was changed. All named variants are available through the operator mark command; a surface variant uses the player's height six blocks ahead and safely refuses unsuitable/sloping terrain.
