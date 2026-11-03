# November 2 structure waterlogging hotfix

## Report and cause
The live 20261102.1 server crashed during placement of `jugcraft:twigworks_bloodstone_obelisk`. The report timestamp is November 3 00:51:26 UTC (November 2 in the owner's time zone). `StructureTemplate.placeInWorld` called `SimpleWaterloggedBlock.placeLiquid` on `jugcraft:twigworks_lamp`, whose supplied state definition does not contain `waterlogged`. The resulting chunk-generation failure preceded the watchdog shutdown.

## Fix
A bounded optional-waterlogging adapter guards all three native liquid entry points (eligibility, direct placement, bucket pickup). Six imported household families use this adapter because their supplied definitions mix dry-only and waterloggable blocks. Dry-only definitions reject water without inventing states; existing waterloggable definitions still use native behavior. No registry names, state schemas, models, saved worlds or source-library assets are changed.

## Verification
Added a real structure-template lamp-over-water reproduction and a registered-block sweep of all Jugcraft SimpleWaterloggedBlock implementations without the native property. Existing bucket fill/drain, native-property serialization, natural decoration and biome tests run alongside these regressions. All 36 focused server tests passed locally in the combined TwigworksWorldGameTests, BiomeGameTests, OwnerEcologyGameTests and NewestHouseholdGameTests run. Compilation and the repository/documentation check passed. Command: `gradlew runGameTest -I build/worldgen-hotfix.init.gradle`. The two new regressions passed alongside the existing 34 tests; no live-player join test is claimed.

## Deployment
Local fix in progress. The live server is currently stopped after its crash. No restart, world rollback, chunk deletion or client-channel change has been performed for this hotfix. Obtain the owner's maintenance confirmation before starting the fixed server.

Implemented with OpenAI Codex.
