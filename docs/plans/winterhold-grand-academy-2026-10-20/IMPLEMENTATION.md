# Grand Academy implementation ledger

The contributor supplied this planning package on 20 October 2026. It is design intent; proposed rooms and mechanics are not implementation claims.

The saved circular-campus prototype is commit e03c341 on feature/frostward-academy, draft PR #441. Continue that branch rather than discarding recovered work. Current upstream main c27afcd adds drilling/Chima; open factory/economy PRs are not academy dependencies.

M0 implementation checkpoint: `9d7cc28` adds localized client snow/mist, one persistent Radiance instructor using the existing townsfolk model, existing Concordance journal/benches, and courtyard secret S01 with per-player lore credit. The saved geometry now includes an actual ward and four controls. No extra rooms, progression engine, global weather change or natural generation. This is a prototype pending the runtime acceptance in the [feature record](../../features/frostward-academy.md#grand-academy-m0-checkpoint), not a completed M0 gameplay certification.

The runtime layout remains tools/winterhold_layout.json: 145 x 76 x 161, origin northwest bottom, courtyard walking Y=25, monument X/Z=68/85. Proposed sizes/room graph in data/layout.json are long-term targets, not placed dimensions. The first-stage climate geometry must match the runtime footprint.

Room mapping: EXT-01 bridge; EXT-02 gate frame (full vestibule pending); EXT-03 courtyard/cloister; ELM-01 main teaching floor. Existing additional rooms remain prototypes, not completed M1-M5 milestones.

AI assistance: OpenAI Codex (GPT-6); the existing PR records the model-policy exception for maintainer review.

21 October continuation: `gpt-6.1-sol-wm` corrected pinned-client API names and validated the prototype. Nine Python structure tests, independent Node validation and pure Java puzzle/climate checks pass. Common, client and game-test Java compile successfully. All 2,111 required Minecraft server GameTests pass, including the four academy methods (task exit 0). The selected `AcademyClientGameTests` also passes (exit 0), with nine captured/inspected Minecraft screenshots in the feature record. Earlier client fixture/resource failures are recorded there. Full `build` certification, two-client play, persistence/transition/performance checks and richer room dressing remain open. Review this coherent M0 prototype checkpoint before expanding M1.
