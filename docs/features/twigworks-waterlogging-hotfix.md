# Twigworks world-generation waterlogging hotfix

Owner requested a fix, related world-generation review, tests and coordinated
server/client deployment after the October 31 pebble crash. Work by OpenAI Codex
(GPT-6).

## Failure and repair

The hosted 20261031.1 server completed startup, then crashed during chunk
generation at 2026-11-01 02:46:32 UTC. `OwnerTwigsWorld.Natural.place` attempted
to set the native `WATERLOGGED` property on `jugcraft:twigworks_pebble`.
`HouseholdBlock` had constructed a separate BooleanProperty with the same name.
Minecraft 26.3's state access requires the registered property instance.

Shared household registration now uses `BlockStateProperties.WATERLOGGED` for
that property. This also repairs the matching native bucket/feature paths for
twigs, seashells and other descriptor-backed decorations. Serialized property
names, values, registry IDs, artwork and geometry remain unchanged. Existing
chunks are not regenerated or rewritten by this fix.

## Regression coverage and review

`TwigworksWorldGameTests` exercises all registered household waterlogging
properties and saved-state round trips, actual dry/underwater natural placement
for pebbles, twigs and all four shells, native bucket filling/draining, and dry/
underwater azalea attachment. It is registered in the ordinary server suite.

The feature-source review also examined property writes in landscape, candy,
Ribbit, archwood, fungal, ecology, driftwood and arcane growth generators.
Guarded property writes and native block properties were distinguished from
the dynamically constructed household property. No second instance of this
specific defect was found. This is a targeted review, not proof that every
possible world-generation defect is absent.

## Validation

October 31, 23:32 local: **all 34 focused server game tests passed**, with optional
integrations absent. The selection included the four new Twigworks regressions,
`BiomeGameTests` and `NewestHouseholdGameTests`; common, client and test code
compiled. The disposable test world saved and shut down successfully. No personal
launcher profile or owner playtest was controlled.

All ten release-packaging/server-patch Python tests and the 26-library platform
lock check passed. Evidence: `build/worldgen-hotfix-tests.log` and
`build/worldgen-hotfix-pack-tests.log`. The local Gradle selector changes only the
generated test manifest and restores it afterward; ordinary CI retains the full
test registry. This is focused validation, not a full-suite green claim.

PR #599's Foundation audit reproduced the eight already documented October 31
Python failures. Those and the 708 static geometry diagnostics remain priority
follow-ups; this repair does not clear them. Deployment status is recorded
separately once the release gates are resolved and the hosted server is verified.
