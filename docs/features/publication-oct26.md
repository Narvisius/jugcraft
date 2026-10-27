# October 26 combined release preparation

Owner authorized publishing local Jimtafacts work, integrating all open pull requests,
and preparing matching server/client updates. Work performed with OpenAI Codex.

## Integrated scope

The integration branch preserves the original commit ancestry for PRs #448, #449,
#452-#455, #458-#459, #465-#468, #470, #472-#474, #476-#486, #489-#493 and the subsequently submitted #495.
This includes Jimtafacts/native computers and transport, aircraft repairs, Chompa,
Frostward College, starter quartz/silicon, companion jobs, seasonal wildlife,
Concordance winter/stone/workshops/pacts, crates, guns, trains, town traders,
Pumpkin King, and the Crown of Thunder arena/rite groundwork.

Crown of Thunder is part 1 only: the Storm Roc boss remains future work.
Jimtafacts adaptation limits remain in jimtafacts-remaining.md.

## Integration decisions

- Preserve starter heat upgrades and companion work together in the nine-slot workshop.
- Preserve full native computer controls while adopting the fitted machine screen style.
- Keep Crowfield/Pumpkin King and Crown of Thunder as separate lairs and generators.
- Merge shared registry, translation, tag and test manifests additively; regenerate data.
- Verify the embedded Cobalt runtime against its reviewed original content inventory;
  permit only Loom's exact generated metadata, not arbitrary nested libraries.

## Validation and deployment status

Combined local server validation: all 2,962 required tests passed after repairing
companion crafting-table state leaking onto smithing tables. A regression test
keeps ordinary crafting tables functional and smithing tables free of that state.
Main/client/test compilation passes. Python: 285/286 initially; the sole missing
Thunder Vane essence value was repaired and all 12 alchemy checks then passed.
World Designer: 12/12. Repository links and 26 dependency pins pass. The actual
built JAR passes four mixin-config/124-class checks and embedded-runtime integrity.
Full data/model audit found only two missing arena season classifications; both
were repaired and the focused season audit passed. The art audit covered 24,511
block/item models, 3,733 quad parts and 180 combined model layouts. Refreshed
survival graph: zero invalid ingredient diagnostics.

Linux CI exposed Pillow/zlib PNG encoding drift in twelve unchanged atlas crops.
Those two importers now preserve existing bytes when exact RGBA pixels match;
changed pixels and corrupt outputs still regenerate. The regression test passes.
Six combined rendered-client classes passed: ScreenFitClientGameTests,
JimtafactsComputersClientGameTests, AircraftFlightClientGameTests,
TrainClientGameTests, CrownOfThunderClientGameTests and RetroTraderClientGameTests.
63 screenshots are retained in build/release-client-evidence-oct26; the fitted
computer screen was inspected. These results precede the final three long guns
from #495; that addition receives separate focused checks and combined CI.
Required exact-commit GitHub checks remain in progress. Earlier
Jimtafacts-only results are separate. Local evidence: build/release-*-oct26.log.
No server restart or client channel promotion has occurred for this candidate.
The live world and player settings must be retained. Obtain owner confirmation,
warn connected players and wait 30 seconds before a live stop/restart. Promote
clients only after the matching server is verified healthy.

## Publication and release follow-up

PR #494 was squash-merged as `46bac6b349490801b3296df21ddcf2b19c778a2a`,
following the repository's required linear history. Its tree exactly matches
the locally tested integration. The 33 source PRs were closed as incorporated,
not individually marked merged. PRs #496–#499 arrived during validation and are
outside this release batch. The late #493 lightning-rod/value fixes match the
equivalent tested fixes already integrated.

The final local Python suite passes all 287 tests. All 13 diamond-steel gun tests
and 79 updater assertions pass. Local release preflight verifies all six artifact
hashes and identical client/server Jugcraft JARs; the server patch contains only
`mods/jugcraft.jar`. These are preparation results, not deployment evidence.

Exact-main CI passed the server suite without optional integrations, but its
VanadiumIron client test found no ore in the 64-chunk designed volcano sample.
The test now expands to a bounded 256-chunk sample wholly inside that painted
volcano if necessary, retaining both natural-ore presence and zero-outcrop
assertions. Local reproduction found 3,973 natural underground ore blocks and
zero surface outcrops in that expanded sample. Production rarity, placement and
terrain are unchanged. The full focused client test passed, including two natural-world surveys and
save/reopen persistence. Remaining release CI must still pass before deployment.

The same main run passed both server builds and release assembly. Its aircraft
client check hit an unresolved-sound NPE when the headless sound engine skipped
playback without an audio device. The test explicitly calls the 26.3
`getOrResolve` API before reading volume and rejects missing/empty resources;
the engine-loop, steering and both F5 camera assertions remain intact. The full
aircraft client test passes locally in 2m38s. Runtime aircraft code is unchanged.

The Pumpkin King client fixture also assumed its harvest and crowning could
interrupt an active attack. Bounded waits now cover the longest attack plus
the call/transition, and its artificial ripening clock crosses the actual
swelling boundary instead of skipping over it. The complete encounter passes
locally in 3m26s, including hand-picked gourds, all phases, loot, cleanup and a
fresh instance. Production encounter timing is unchanged. These three client
repairs are tracked in PR #500; no deployment has occurred.

The next exact-main run passed those repaired client groups and both server
builds, but exposed an intermittent Peepo crucible-output delivery stall.
The inset upper crucible shell inherited a walkable-passage classification;
the reported carrier position was against that shell's collision face.
Workshop blocks now explicitly reject pathfinding through their parts, as the
Hearth Oven already does. A regression checks every workshop footprint part;
the existing full stock/delivery test still checks physical deliveries, exact
output quantities and released reservations. The unchanged test passed locally
(173 assertions), so this is not a claim of a reliable local reproduction.
The repaired test and final release checks are pending. No server deployment
or client channel promotion has occurred.

PR #507 merged the workshop shell classification after all 178 local stock
station assertions passed. Its main run then caught the same class of delivery
stall at the Cider Press in the remaining-stock suite. Ground navigation now
extends its existing bench/wheel/tap exclusion to every workshop, Cider Press
and Canning Kettle, including the cell above each mechanism. Press and canning
shells also explicitly reject walk-through paths. Tests require a reachable
exterior route whose nodes do not cross or climb the equipment, followed by
the real production/delivery cycles. Combined validation is pending; the live
server and client channel remain unchanged.

