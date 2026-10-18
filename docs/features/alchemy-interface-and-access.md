# Alchemy interfaces and immediate equipment use

Owner-directed local work by **OpenAI Codex (GPT-6)**, 17 October 2026. This supersedes the equipment research requirement in [Alchemy Playability](alchemy-playability.md). The owner accepted the presentation and authorized publishing the alchemy suite and UI fixes on 18 October 2026.

## Player experience

Craft, find or trade for an alchemical item and use it immediately. First Light, Assay study and mastery are **not equipment-use requirements**. Research remains available as optional progression; using a device does not grant research automatically. The separate spellcasting/research system is unchanged.

The tablet, Eye, Klein Star, storage, material catalogue, converters, collector/condenser, relay/furnace/pedestal/ward/Nova controls, guide and Gem controls share an engraved dark-metal equipment style: silver bevels, restrained teal/violet inlays, recessed inventory trays, distinct selected controls and bounded animated energy indicators. Supplied owner icons and atlas fragments remain unchanged. The rendering extends `OwnerAlchemyArt` and the native narrated button widgets; no new UI library is required. Reduced-motion settings freeze decorative movement.

Input, output and selected-pattern sockets are visually distinct. The tablet retains the **dissolve an actual item to learn its pattern** rule, exact essence price and 1/16/64/Max withdrawal. Knowing an item pattern is separate from having permission to operate the tablet. The Eye hides irrelevant transaction slots. Guide tabs highlight their current topic. Gem powers can be clicked after equipping the correct piece, without first activating it in the hand; powers still use normal costs, cooldowns and safe movement rules.

## Progression, authority and saves

- Existing crafting, trading, loot and the shared essence economy remain the acquisition/input routes. Equipment feeds storage, resource production, farming, transport and combat specialties.
- Charms can be equipped by a new player. Active effects still require the actual item, their on/off setting where applicable, and available fuel. Feature switches, dead/spectator restrictions, loaded chunks, permission checks, inventory custody and output limits remain enforced by the server.
- Placing a factory records its owner immediately. Saved factories with an old false research-authorization flag migrate from their existing owner UUID, retaining inventory, charge, pause state and ownership. Missing owners are not invented; claim checks still run.
- No item, block, component or menu registration is removed. Balances, learned patterns and favorites are preserved. No world conversion or chunk loading is introduced.
- This scope covers the Equivalent Exchange-style alchemy suite. It does not redesign unrelated technology, settlement or spellcasting screens.

## Validation

Passed locally on 17 October 2026, Minecraft **26.3**, Fabric Loader **0.19.5**, Java **25.0.4.1**:

- `./gradlew.bat build --offline --console=plain -Pkotlin.compiler.execution.strategy=in-process`: all **1,929 required server tests passed**, including fresh survival equipment access, exact tablet payments, traded charms/tools, Gem activation, saved-factory migration, spectator denial and the Eye's hidden-slot boundary.
- `./gradlew.bat jar runClientGameTest -PclientTests=CompleteAlchemyClientGameTests --offline --console=plain -Pkotlin.compiler.execution.strategy=in-process`: passed using a fresh singleplayer test world without Assay research. Real menu packets verify item learning, withdrawal, converter settings, storage, machines, star transfers, equipped Gem activation and Eye shift-click inventory preservation. Reviewed rendered screenshots at GUI scales 1 and 2; corrected machine-wallet spacing and guide text fit.
- `python tools/check_mod_data.py`: **3,021 material IDs**, **8,522 block/item models**, **3,709 quad parts** and the recipe/data audit passed.
- `python scripts/check_repository.py`: structure and local documentation links passed. This is not a gameplay test.

The first server run exposed one obsolete research-gate expectation and a vanilla mock player that always reported Creative mode; the updated test uses a real embedded-network survival player and verifies spectator mode explicitly. The initial star UI test aimed at a relay and reopened that block; the corrected check uses the star in air. These test issues were corrected before the passing runs.

Local artifacts are in `outputs/Jugcraft-Alchemy-Refined-Oct17` outside the repository: verified JAR and Modrinth pack, screenshots, an independently playable showroom, validation record and exact source SHA in `build-info.json`. The existing owner playtest profile/worlds are preserved. The owner controls the manual playtest; the pack contains no scripted camera or player control.

The owner reported that the result looks good and authorized publication on 18 October 2026. This supersedes the earlier local-only publication checkpoints across the alchemy feature records. Independent two-client multiplayer and detailed survival/economy balancing remain unverified. The publication branch contains the alchemy suite and constellation/encyclopedia UI; the separate local seven-tech-package and town changes remain outside this merge.

## Main integration, 18 October 2026

[PR #415](https://github.com/jimbozoomer-byte/jugcraft/pull/415) extracts this scope onto main `2804ba015`. All 90 affected code/test files match the owner-tested local source after line-ending normalization. Shared test registrations, generated resources and the essence value table were rebuilt for main's existing content, without the unpublished tech and town additions. Compilation of common/client/game-test code, all 63 Python checks, the 18-library pin contract and repository documentation checks passed locally. The PR records the fresh combined server, resource and rendered-client results and GitHub check outcomes; the historical 1,929-test result above belongs to the broader local playtest build.

The isolated integration's full build passed all **1,925 required server tests**. Its resource audit passed **2,885 material IDs, 8,409 models and 3,707 quad parts**. GitHub also passed the full build/packaging and optional-integrations-absent build. The cloud workshop client check exposed an old 65,536-essence capacity expectation after the Mk I condenser had increased to 1,048,576. The fixture now supplies enough essence, checks the actual capacity and verifies exact wallet/buffer conservation through both deposit and withdrawal; no production behavior changed for this correction. The four focused Dark Matter, elemental, workshop and complete-alchemy client classes then passed together locally, with all 45 expected screenshots through the final Gem controls. The earlier local invocation exercised the encyclopedia, constellation editor and Stone tablet, but its early clean exit is not claimed as completion of the later four classes. Final cloud outcomes are recorded on the PR.
