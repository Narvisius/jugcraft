# Companion crafting-table work

Owner-requested implementation using OpenAI Codex (GPT-6), 25 October 2026. Adds manual crafting at the existing vanilla Crafting Table for Peepo and Jughead, using their current Planner, Filter, Supply/Output, stock targets and energy reserve, with an assemble-and-inspect animation. No new block/item, recipe, dependency or power system is added.

## Player outcome and progression

Assign a normal Crafting Table with the Planner. Link ingredient Supplies and finished-product Outputs, put desired finished items into the job's nine-item Filter, and optionally enable Maintain stock. An empty Filter allows available supported recipes. The table chooses a complete ordinary shaped/shapeless crafting recipe from the supplied ingredients; modded/data-pack recipes using those native recipe types participate. Custom dynamic recipes without a predictable ingredient layout remain manual.

One companion works at the table at a time. A craft requires 60 active work ticks (three seconds) at 4 JE of companion effort per tick: 240 JE before the Mechanic uniform discount, or 180 JE with it. Travel, breaks, disabled work orders, settings editing and blocked output do not advance production. Normal exhaustion/rest behavior applies. Whole recipe yields may exceed a small stock target.

This is an early Workshop-stage route from gathered or supplied materials to ordinary crafted goods. It needs a tamed companion, a normal obtainable crafting table and the recipe's actual ingredients. Later powered Crafters retain their own throughput, redstone/power controls and exact configured patterns. The manual table adds no ingredient or output bonuses.

## Shared systems and safeguards

The existing server-side assignment/menu permissions, claims, companion work scheduling, route links, physical eight-slot carrying inventory and bounded transfer planner remain authoritative. Only a loaded table with a present worker performs work; there is no autonomous block ticker or offline crafting. The shared stock implementation counts completed results, staged ingredients and registered physical deliveries, and controls new supply pickups while allowing committed batches to finish.

The native recipe assembles the actual chosen ingredient stacks. Crafted products and every native container remainder must fit the bounded table output storage before ingredients are consumed. Supplies fill complete recipe layers, avoiding duplicate ingredient starvation. Simulation rolls back inventory changes; real items never originate from Filter ghosts. Interrupted work keeps its saved ingredients/progress, and breaking the table returns its real contents once. Changing the Filter to exclude the current recipe returns its staged inputs through Output.

An additive companion block entity stores each table's filter, stock settings, nine ingredient slots, twelve output slots, selected recipe and paid progress. Existing placed tables acquire this state through normal block-entity lookup. Normal right-click still opens vanilla's player Crafting Table menu, whose grid remains separate. Shift-right-click with an empty hand recovers buffered goods when access is permitted; overflow remains in the table. Because vanilla pistons do not move blocks with block entities, crafting tables become immovable by pistons with this integration. No piston bypass or block replacement is introduced.

The recipe catalog is shared through a weak cache keyed by the native server RecipeManager, which is replaced on data reload. Output indexing and bounded selection avoid per-tick catalog scans. Reloaded/removed recipes invalidate incompatible plans and make actual staged ingredients recoverable; there is no background search for workers or containers.

## Crafting animation

Both characters use an **assemble and inspect** clip synchronized to the three-second paid work cycle:

1. Stand fully on the tabletop, with the feet planted beneath the hips and the jug unobstructed.
2. Over the first 24 ticks, bring up to two actual recipe ingredient previews together with alternating hands and a gentle torso lean.
3. During ticks 24–48, bend forward at the hips and press the pieces using both hands. The ingredients sit near the table center; short hand reach stays bounded.
4. During the final 12 ticks, lift a preview of the result for inspection. Only the server's normal completion event creates the actual output at tick 60.

The server sends one-item recipe snapshots and a clock anchored to paid progress. Continuous crafting does not resend the same preview payload every tick, and resuming a paused craft restores its phase. Clients use native item models without inventory or recipe searches. Previews are render-only; they do not spawn item entities, consume ingredients or alter energy costs. Interruptions and switching work clips clear them.

The standing position is 0.24 blocks from the table center toward the selected approach, keeping the companion fully above the tabletop. The feet retain the rig's normal planted position; the upper body rotates about the hip joints without an independent body or leg translation. Mounting checks an outside vertical lift and a horizontal path over the tabletop, including Jughead's full height, loaded chunks and permissions. Blocked headroom prevents mounting. Release restores gravity and uses a checked exit; saved exit recovery covers interrupted work and world reloads. Clear approach and overhead space must remain available.

The animation extends the existing Peepo model bones and render layers, including costume attachments; no second animation controller or dependency is introduced. The shared owner-library index/catalog was inspected. Its workshop/table textures do not supply a matching companion clip, so this procedural pose reuses the existing companion rig and Minecraft's runtime recipe item models. No library originals or textures are changed.

## Validation

Validated on 25 October 2026 using the pinned Minecraft 26.3 / Fabric 0.19.5 / Java 25 local toolchain and Complete-pack dependencies. Implementation used OpenAI Codex (GPT-6).

- `./build-local.ps1 -Tasks @('compileJava','compileClientJava')`: **PASS**, including normal resource preparation. This took 10 minutes 27 seconds; the long resource step was verified to be copying/hashing files, not a stalled game test. Evidence: `build/crafting-table-compile.log`.
- `./build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoCraftingTableClientTests','-x','processResources')`: **PASS, 104 assertions**, build successful in 2 minutes 51 seconds. The latest common/client/test source was compiled; unchanged main resources had just been prepared above. Evidence: `build/crafting-table-focused.log`.
- Coverage includes ordinary/existing-table block-entity creation, the unchanged native player crafting menu, output Filter validation, repeated ingredients, mixed tag alternatives, whole yields, bounded automatic selection, simulation rollback, shared incoming reservations, saved plans/stock/contents, missing-recipe ingredient recovery, single-worker claims, exact base/Mechanic energy costs, low-reserve recovery, duplicate-call protection, mid-craft saved progress, unattended pauses, full-output refusal and break/drop conservation.
- Real Supply → general work animation state → native craft → Output cycles passed for Peepo and Jughead. Stock stops further pickup after the target; consuming the stock restarts it. The Jughead honey-block cycle delivers all four native glass-bottle remainders.
- Repository structure/documentation-link checks and `git diff --check`: **PASS**.

The results above are the crafting-table implementation baseline, before the dedicated animation. These are automated integrated-server checks, including save/load of the station's data; a full dedicated-server restart, two-client play and representative large-server load remain unverified. The distribution is not rebuilt by this implementation.

### Dedicated animation validation

- `./build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoCraftingTableClientTests,PeepoCraftingAnimationClientTests','-x','processResources')`: **PASS**, 104 crafting-table assertions and 324 animation assertions. Build successful in 4 minutes 11 seconds, including common/client/test compilation; unchanged main resources were reused. Evidence: `build/crafting-animation-focused.log`.
- The focused animation checks cover both companions' mount/headroom/overhang/release/break/settings behavior, actual paid progress and recipe preview snapshots, unchanged metadata, pause/resume timing, absence of premature outputs, finite bounded arm poses, the pumpkin costume's wider body, and reset to idle. Both real crafting cycles complete normally after their gather/press/inspection phases.
- Initial real in-game captures for both characters were inspected. They confirmed native ingredient/result switching and led to moving the crafting shoulder positions outward to make the small arm movements more visible.
- After that visual adjustment, `./build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoCraftingAnimationClientTests','-x','processResources')`: **PASS, 324 assertions**, on 26 October 2026, build successful in 1 minute 37 seconds. Evidence: `build/crafting-animation-visual.log`. The final gather/press/inspect captures for both companions were inspected individually, plus a rear-quarter perch view. Evidence is preserved under `build/companion-crafting-evidence/final/`; native props, short arm reach and Jughead's attached jug were checked. The rear-quarter framing crops the feet at the lower image edge, so full foot placement is additionally covered by the deterministic rig/clearance checks.
- Repository structure/documentation-link checks and whitespace validation passed. No unrelated gameplay suites were run.

## Local distribution refresh

Owner-requested refresh, 26 October 2026, using OpenAI Codex (GPT-6): `./build-local.ps1 -Tasks @('jar')` passed in 1 minute 29 seconds. `python scripts/package_modrinth.py check` and `build --jar build/libs/jugcraft-0.1.0-alpha.jar` passed with 26 pinned libraries. The refreshed `build/distributions/jugcraft-complete-0.1.0-alpha.mrpack` includes the crafting-table job and final animation. Both archives passed integrity checks; all 297 compiled companion/client classes, the crafting-table mixin and its registration, embedded current JAR, updater inventory and pack checksum were verified. Evidence: `build/crafting-table-distribution-build.log` and `build/crafting-table-distribution-verification.json`. No gameplay tests were repeated for this packaging-only refresh. The importable pack is ready for manual testing; no existing launcher profile was modified or public release published.

## Standing correction after manual testing

On 26 October 2026, the owner reported disconnected legs in the edge-perch pose. Removed its independent backward/downward leg offsets and body translation. Both characters now stand fully on the table with their normal planted feet; the upper body bends about the connected hips. Moving the worker inward and positioning ingredient previews relative to the same standing offset puts the ingredients near the center without increasing arm reach. Implemented using OpenAI Codex (GPT-6).

`./build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoCraftingAnimationClientTests','jar','-x','processResources')` passed in 4 minutes 44 seconds, including **470 focused assertions**. New checks cover actual leg-to-transformed-hip contact, stationary foot height, the whole companion fitting over the tabletop and centered ingredients; existing work/clearance/interrupt checks also pass. Front and uncropped rear-quarter screenshots for Peepo and Jughead were inspected, with both feet visibly supported. Evidence: `build/crafting-standing-fix.log` and `build/companion-crafting-evidence/standing-fix/`. Unchanged main resources were already prepared; no unrelated gameplay suite ran.

The Complete pack was rebuilt with this correction. Archive integrity, the current 297 companion/client classes, crafting mixin registration, embedded JAR, updater inventory and SHA-512 checksum passed again; `build/crafting-table-distribution-verification.json` records the corrected package. No installed launcher profile or public release was changed.
