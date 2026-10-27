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
