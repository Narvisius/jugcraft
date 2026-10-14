# Companion integration regression — 12 October 2026

Owner-requested companion-only validation using OpenAI Codex (GPT-6). Tested the dirty `peepo-companion` working tree based on `91128a9153064a7c3185714902f763a13c0070b9`; this record does not describe a committed release. Minecraft 26.3, Fabric Loader 0.19.5, the project's pinned dependencies and local JDK 25 were used. Worlds are disposable flat worlds created by Fabric's client game-test framework.

## Scope and results

All ten companion suites passed: **2,691 gameplay assertions**, plus **five test-selector unit tests**. The final integration invocation completed at 12:26 local time with **17 groups, 235 assertions and zero failures** (3 minutes 31 seconds including startup). No unrelated gameplay suites were selected.

Passing assertions by suite:

| Suite | Assertions |
| --- | ---: |
| PeepoCompanionClientTests | 719 |
| PeepoHearthClientTests | 199 |
| PeepoKitchenAnimationClientTests | 1,096 |
| PeepoSuppliesClientTests | 34 |
| PeepoDeliveryClientTests | 60 |
| PeepoWorkSessionClientTests | 54 |
| PeepoHarvestClientTests | 231 |
| PeepoPopulationClientTests | 25 |
| PeepoGardenExpansionClientTests | 38 |
| PeepoIntegrationClientTests | 235 |

These include core NPC behavior, processor assistance and machine-based effort, hand cranks, kitchen automation and animation, supply routing, batch deliveries, continuous work sessions, garden harvesting, livestock management and garden extensions. The core suite includes its existing one-client dedicated-server reconnect check. This is representative integration coverage, not every recipe on every machine.

The new `PeepoIntegrationClientTests` adds real-world adapters and transaction rollback checks for sprinklers, serving stations, spinning and cider, candy kettles, vanilla stations, brewing chains, hives, compost, Farm Stands, crucibles, solid-fuel generators, Auto-Crafters, Electroplating Baths, garden zones and maintenance, industrial forms, magic devices and Bat Houses. Autonomous cases exercise water containers, sprinkler Supply/Output, spinning, flooded crops, industrial assistance, multi-step brewing, hive harvesting and magic crop harvesting.

## Reproduction and evidence

Select only the required companion classes, for example:

```powershell
./gradlew.bat runClientGameTest '-PclientTests=PeepoIntegrationClientTests'
```

Other companion classes can be selected with the same comma-separated property. Do not use an unfiltered test run for companion-only validation. Finish a running game process before recompiling test classes: replacing its class outputs during execution interrupts the runner.

Local, uncommitted logs: `build/companion-all-new-tests.log` (core and hearth), `build/companion-remaining-tests.log` (kitchen and supplies), `build/companion-zones-tests.log` (delivery, sessions, harvesting, population and expanded gardens), and `build/companion-integration-complete.log` (new integrations). Early logs also preserve failed exploratory runs; passing class results above are identified separately from their enclosing command's exit status.

## Build and packaging

`compileJava compileClientJava assemble` passed after the final game-test run (19 seconds; `build/companion-tested-build.log`). `python scripts/check_repository.py` and `git diff --check` passed. The complete Modrinth pack was rebuilt at `build/distributions/jugcraft-complete-0.1.0-alpha.mrpack`, retaining all 18 locked libraries and platform pins. Its embedded Jugcraft JAR was compared byte-for-byte with the final build, and the pack SHA-512 matched its sidecar. No commit or push was performed during the test run.

## Corrections exposed during validation

- Older garden fixtures still used the single-block assignment gesture. They now use the implemented two-corner garden-zone API; delivery conservation and work-session expectations are retained.
- New fixture corrections establish owners for private serving containers, load bottles before requesting brewing reagents, smoke/ripen a hive before requesting bottles, and wait for both industrial helpers to actually work before measuring speed. The industrial fixture supplies power during approach, then stops refilling for the measured energy window.
- Garden maintenance looked for `jugcraft:superphosphate`, while the registered chemical fertilizer is `jugcraft:fertilizer`. It now uses the actual item, retaining its radius and dose count.
- Flooded-crop reach checks aimed through a dry bank's edge. Rice and cranberry checks now aim near the water surface and recognize an upper rice section as part of that same plant; the test places crops in a contained paddy with dry footing.

- Water jobs used the ordinary 0.8-block station arrival radius, which could stop Peepo short of dry footing. They now require arrival within 0.2 blocks of the bank point, finishing the final short step after the vanilla path node completes; other stations retain their prior radius. A dedicated regression begins at the water edge with a canteen.

## Limits

This is not a large-server load benchmark or a two-player certification. Every recipe, long-term fermentation timing, external modded animals/furniture, optional-library absence, cross-dimensional factory loads and every GUI interaction remain outside this run. Transaction and persistence assertions are distinguished from autonomous delivery cases. Packaging does not establish launcher import or gameplay approval.
