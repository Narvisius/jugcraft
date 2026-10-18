# Klein Stars and elemental amulets

> Publication update, 18 October 2026: the owner accepted the combined alchemy presentation and authorized publishing the alchemy additions and UI fixes. Historical local-only checkpoints below are superseded. Current scope and validation are recorded in [Alchemy interfaces and access](alchemy-interface-and-access.md).

Owner requested continued use of the remaining Equivalent Exchange-style owner assets on 17 October 2026. Implementation: OpenAI Codex (GPT-6). Local branch only; owner playtest and publication pending.

## Outcome and connections

Six Klein Star tiers let players carry and trade the existing alchemical essence. Evertide and Volcanite amulets supply water/lava buckets and paid environmental protection without spellcasting. Dark Matter and High Covalence Dust from the previous package supply progression. Ordinary crafting and smithing tables suffice; no additional machine, currency, research tree or library is introduced. Use requires understood Assay and enabled Concordance.

Stars hold 4,096 / 16,384 / 65,536 / 262,144 / 1,048,576 / 4,194,304 essence. Right-click moves the real held star into a single-slot menu. Six buttons store/take 1,024, 16,384 or all available essence, clamped to capacity and the existing 1,000,000 personal-ledger cap. Closing returns the same item through vanilla container handling. Withdraw to the personal ledger before powering tools or wards; there is no automatic drain from carried stars. Smithing consumes High Covalence Dust and Dark Matter to advance one tier while preserving charge, name and other components.

Amulets use a plain empty bucket in the other hand: water costs 64 essence; lava costs 4,096. A stacked bucket requires an empty inventory slot; refusal consumes nothing. Sneak-use toggles a ward, held or worn in an existing Charm slot. Evertide grants water breathing while submerged for 4 essence per second; Volcanite grants fire resistance while burning/in lava for 8. Checks run once per second and refresh a three-second effect. Longer/stronger potions are left alone; duplicates do not stack. No danger means no spending. Empty balance, disabled feature or missing research stops refresh. Removing the amulet leaves only the short remaining buff; unrelated potion effects are never stripped.

All ledger/item changes are server-owned and menu/button inputs are bounded and rate-limited. Stars persist a bounded integer component; amulets reuse the existing activation component. These items follow normal inventory, trade and death rules and are explicitly excluded from ordinary equivalence. Bucket supply does not edit terrain, place fluids or bypass claims: filled buckets use normal vanilla placement afterward. Neither amulet grants flight or invulnerability.

## Art and interface

Eight unchanged owner icons come from `art/owner-library/originals/Magic/textures/item/stars/klein_star_{1..6}.png` and `textures/item/rings/{evertide_amulet,volcanite_amulet}.png`. Originals/notices remain intact. `tools/concordance_elemental_gear.py` records mappings; `tools/owner_art.py` verifies exact copies. The new storage screen reuses the existing stone/silver tablet's native buttons and owner sigil; subtle orbital animation honors reduced-motion settings. Balance, star charge, capacity and transfer choices are visible without typing.

## Validation

Results on 17 October 2026:

- Common/client compilation and all **1,801 required server game tests passed**, including **14 new alchemy cases**. These cover exact transfer conservation and caps, foreign/invalid/rate-limited requests, close/shift-move behavior, all five real smithing recipes with charge/name persistence, saved components, crafting bucket remainders, both hand orientations, full inventory and insufficient funds, passive costs/duplicate equipment, Charm equipment, real submerged-eye state, long potions, feature/research gates and equivalence exclusion.
- The first server run caught two test-fixture mistakes: the close test assumed the returned star occupied slot zero despite the research grant's existing inventory item; the underwater fixture called the server-management tick instead of the entity/movement tick. The corrected tests locate the returned star and advance `doTick()`. The full rerun passed; no gameplay implementation was changed to satisfy these fixtures.
- **ElementalAlchemyClientGameTests passed** using real client input: right-click opening, charge/withdraw buttons and exact networked balances, item return on close, UI scales 1/2 and keyboard focus, Evertide/Volcanite bucket creation and sneak-use activation. The tablet, inventory icons and both held amulets were visually inspected in captured game renders. This ran in a disposable automated world only.
- All **67 Python unit tests**, the repository/document checks and the 18-library platform/pack validation passed. All owner imports match their source files. A canonical replay matched **24 JSON outputs and 29 language entries** and verified the existing Charm/exclusion memberships were retained. The JAR contains all **36 feature/shared resources** byte-identically and the four new runtime classes.
- The broader data/art audit initially found its old expected Charm membership omitted the new amulets. The checker now derives those additions from the canonical feature module. The full rerun passed: **2,934 material IDs**, all data/recipes, 8,516 block/item models, 3,709 quad parts and 171 combined models.
- An independent **Jugcraft Elemental Alchemy** save has been preserved with all eight items, understood Assay, 100,000 essence, upgrade ingredients, prior equipment and labeled water/lava/crafting/smithing stations. Previous manual worlds are untouched. The local Modrinth package includes this save and instructions. Assembly checks ZIP integrity, embedded JAR equality and the saved-world entry.

Commands: `gradlew.bat runClientGameTest build -PclientTests=ElementalAlchemyClientGameTests --offline --no-daemon -Pkotlin.compiler.execution.strategy=in-process` (Java 25, Minecraft 26.3, Fabric Loader 0.19.5), `python tools/check_mod_data.py`, `python tools/owner_art.py --check`, `python scripts/check_repository.py`, `python scripts/package_modrinth.py check`, `python -m unittest discover -s scripts/tests -v`. Local environment points `JAVA_HOME` to the pinned JDK and `JUGCRAFT_FONT_DIR` to the installed DejaVu fonts.

No owner playtest, real multi-person multiplayer session, optional-library absence run or Modrinth import is claimed for this slice. Mobility rings, collectors/condensers and Red Matter gear remain separate TODO work. Nothing is pushed to main.

The source remains on `local/alchemical-gear-oct16`, extending local commit `2c833b57b9564b659c9a35b8bc36ac85e2bb8267`. The packaged build records its exact source commit, checksums and evidence in the accompanying `build-info.json`.
