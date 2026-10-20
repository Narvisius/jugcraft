# Local recovery validation — 19 October 2026

This is an uncommitted local working-tree snapshot based on `b418263b1c0bb3c88aafc63ae1c4d0ce08214d3e`, not a published commit or release. Work and validation were performed with OpenAI Codex. Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and Java 25 use the repository's existing pins. The 19 companion libraries retain their locked versions.

## Completed checks

- The broad server run completed 2,005 tests: 2,004 passed, and the new Channelled test failed because it queried skylight immediately after placing a roof. The test now waits for lighting to update. The corrected focused build passed all seven required tests, including all six recovered-creature tests. The complete 2,005-test suite was not rerun after that fixture correction.
- The focused build completed compilation, resource processing, JAR assembly and Gradle checks successfully. Later client/resource-only corrections are validated by the final client build recorded in `local-validation.json`.
- Ten focused Python tests pass: model hierarchy, timelines, original texture hashes, overlay/base pairings, source variable scopes, legacy alias invalidation, sound-file resolution and current spawn-egg models.
- The full offline material/model audit passes: 5,990 material IDs, 11,687 block/item models, 3,713 quad parts and 173 composed assemblies. Subsequent changes were limited to recovered entity animation declarations, client rendering and the 48 sound references; focused checks cover these corrections.
- Atlas inspection found zero aspect-ratio mismatches across 780 supplied age bindings and two explicit vanilla delegates.
- Repository structure/documentation checks and the 19-library pack lock check pass.

## Rendered testing

Client tests use a freshly generated disposable flat development world. The gallery captures all 167 recovered renderer bindings with paired variants and adult/baby fixtures, selected later attack poses, Hunter Wolf, chest/barrel blocks and 13 held spawn eggs: 269 screenshots. The final run also resolves all 48 supplied Ogg files through Minecraft's resource manager, plus the vanilla delegated models/textures.

Review fixes include complete base skins beneath transparent overlays, the native creeper-minion model/atlas, explicit chest/barrel atlas registration, current egg models, baby ferrouslime scaling, and paired Murmur hair rotation. Screenshots are visual inspection evidence; they do not establish complete source-animation or AI parity.

Final run status, artifact hashes and local evidence locations are recorded in `local-validation.json` after packaging. The local output folder includes the logs, full gallery, contact sheets and owner playtest instructions.

## Not established

No existing owner world/profile was opened, modified or controlled. The pack has not been imported into Modrinth or accepted by the owner. Two-human-client testing, long multiplayer populations, survival balance and complete special-encounter parity remain unverified. Known missing systems and four unavailable original animal models remain listed in the catalog and TODO. No commit, push, merge or deployment was performed.
