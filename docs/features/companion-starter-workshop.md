# Companion claims, fertilizer blends and starter workshops

Owner-requested implementation on 21 October 2026 using OpenAI Codex (GPT-6).
Upstream main `38f1f809` was merged into `peepo-companion` first, retaining the
local task animations and upstream companion movement fixes. No new dependency,
recipe, registry, asset or animation framework is introduced.

## Claims

Companion jobs, garden/animal actions, gates, rest stations, loose food pickups,
Supply/Output and lunch sources now consult the Exchange land registry using the
companion owner's UUID. Personal ownership, current company control and the land
owner/controller's trust list apply while the owner is offline. Untamed companions
cannot act inside claims. Party command access does not independently grant land
access, and automated workers do not inherit an operator bypass.

The land registry keeps its existing Overworld-only scope. Town protection and
container locks remain enforced. Both halves of double chests are checked, including
chests accessed through lunch covers. Planner/filter edits also check the actual
player's permission. The transport crate's menu-based release checks claims too.
Checks read current ownership at use/transfer time, without caching permission
grants or loading chunks. Revoking trust stops work and transfers; carried items
remain real inventory under the existing return/delivery lifecycle.

## Fertilizers

Existing Farmland zones with Fertilizing set to Auto accept Nitrogen Field
Fertilizer, Triple Superphosphate and Mixed-farm Fertilizer alongside bone meal,
bat guano and ordinary Fertilizer. Blend strengths and crop tags come from the
native fertilizer implementation: nitrogen/phosphate give four attempts on their
matching crops and one elsewhere; mixed gives three. Among carried usable doses,
the worker prefers the highest native strength for the selected crop.

The existing four-item supply target, 20-second zone cooldown, shared 60-second
crop cooldown, permitted-zone clipping, harvest/replant priority and 320-JE
application effort remain. At most 75 cells and four growth attempts per eligible
crop are examined. Tall plants are handled at their base and their potential upper
sections must be permitted; magic crops and maintenance-excluded plants remain
excluded. An item is spent only after successful native growth.

Sprinkler Supply accepts all native fertilizer blends, stocking at most four total
items. It preserves the loaded blend/components and cannot mix a new blend into a
partially filled hopper. Water buckets, empty-bucket Output and the sprinkler's
native pulse/growth rules are unchanged.

## Workshop setup and progression

Assign either half of the Workshop Bench or Small Crucible Furnace, or the Small
Ceramic Kiln, using the existing Planner. Each station uses one work assignment.
Link the existing Supply/Output containers and open the station's funnel button in
the companion GUI to choose up to nine finished products. An empty Filter allows
any supported product with available ingredients. Filters are saved on the station
and shared by its assigned companions, like the existing kitchen filters.

- **Workshop Bench:** one companion operates it at the normal recipe speed using
  the existing mallet/general interaction clips. It spends 4 reserve JE per
  productive tick: 400 JE for a 100-tick plate/wire batch, 800 JE for a 200-tick
  circuit batch, plus ordinary hauling effort. Real installed hammer, cutters or
  soldering tool requirements remain; heated recipes still consume normal fuel.
- **Small Ceramic Kiln:** supplies blanks and ordinary furnace fuel, collects fired
  ceramics. The station cooks by itself; no helper speed multiplier or stationed
  work effort is added.
- **Small Crucible Furnace:** supplies alloy ingredients, fuel, a Basic Ceramic
  Lining and Ceramic Crucible, and collects products. The ceramic parts are real,
  reusable installed items. Only ordinary hauling effort is charged.

Supply batches are bounded to eight crafts' ingredients, available cargo capacity,
eight fuel items and one of each required reusable part. Fuel/ingredient insertions
and output extraction use native transactional inventory slots. Multiple chests
can collectively satisfy a recipe; actual pickups still travel to real containers.
If a new recipe needs a different tool, the idle bench exposes the obsolete tool
to Output before receiving its replacement from Supply. Obsolete ingredient types
can also return through Output after a filter/recipe change. Installed ceramics and
tools used by an active batch are never extracted. Keep room in Output for returns.

The normal station tick exclusively owns progress, heat and product creation. A
leased companion supplies a short-lived presence heartbeat and pays for one native
step; a player working at the bench takes precedence, so work does not double.
Pauses, missing resources, full output, changed permissions, editing, rest and
unloads preserve the native saved partial batch. A filter edit does not discard a
committed batch. Idle kiln/crucible processing still follows native player-selected
recipes; the companion Filter controls its recipe planning and deliveries.

Workshop access checks every loaded footprint cell. Front standing positions use
bounded clearance checks and the existing navigation/server budgets. Catalogs reuse
the native reload-aware cache; source inspection happens only during budgeted route
planning. No global worker searches or additional station tickers are added.

## Validation

### Focused regression run (21 October 2026)

At the owner's subsequent request, OpenAI Codex (GPT-6) added and ran only
`PeepoWorkshopClientTests` with
`./build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoWorkshopClientTests')`.
The final run passed **216 assertions** in **4 minutes 33 seconds**; evidence is
`build/companion-workshop-focused-tests-2.log`. No unrelated test classes ran.

Coverage: owned/foreign claims, protection of the second bench half, trust grant
and revocation, offline companion owners and untamed access; all three fertilizer
blends, zone boundaries, consumption, cooldown, four-item sprinkler stock and
mixing rejection; workshop Filters, native readiness, input simulation rollback,
tool retention and output extraction; real Peepo and Jughead bench work approached
from behind in all four orientations, alternate/blocked front positions, paths
avoiding bench cells/tops, and energy consumption; complete physical Supply to
production to Output cycles at the Bench, Ceramic Kiln and Crucible Furnace.

The initial run stopped because the movement fixture omitted fuel for its heated
bench recipe. Adding that required fuel fixed the fixture; no production-code
changes were needed during this test pass. These are automated checks in one
client's disposable integrated-server world, not a multiplayer load test.

### Bench approach correction (21 October 2026)

OpenAI Codex (GPT-6) corrected the owner-reported trapping under/against the bench.
Ground navigation treats both bench parts and their tops as non-walkable, as it
already does the spinning wheel. Work and Supply/Output share the two exterior
front cells, rotated with the footprint and checked against the companion's actual
size, floor support, claims and hazards. Deliveries cannot target the adjacent
bench half. The operator must get within 0.4 blocks of the front position before
working; short final approaches now check the whole passage before moving directly.
The checks use existing path budgets and bounded clearance checks, with no new
world scans. No automated tests were run during the original fix; the subsequent
focused regression results are recorded above.
The final `assemble` passed in 1 minute 34 seconds
(`build/companion-bench-navigation-final-build.log`). The Complete distribution was
rebuilt, and all six affected class files were compared with the compiled output;
its embedded JAR and SHA-512 also matched. This does not verify in-game navigation.

Automated tests and in-game testing were not run, at the owner's request.
Final `compileJava` and `compileClientJava` passed (1 minute 8 seconds;
`build/companion-workshop-final-compile.log`) on the pinned Java 25 / Minecraft
26.3 toolchain. The compiler reported existing JEI deprecation and missing optional
annotation warnings, with no errors. The initial build also compiled successfully
but took 11 minutes 20 seconds to process/index the newly merged resources.
`git diff --check` passed. Compilation is not gameplay or multiplayer validation;
the initial implementation did not rebuild the distribution (rebuilt subsequently
as recorded above). Manual verification remaining: all station orientations and both multiblock halves; filtered/blank
recipes; split supplies, fuel and tool replacement; full Output; concurrent player
and companion work; interrupted/saved batches; claim transfer/trust revocation,
offline owners and cross-claim double chests; all fertilizer blends, tall crops,
cooldowns and a sprinkler with a different blend already loaded.
