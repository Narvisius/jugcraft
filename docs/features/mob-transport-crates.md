# Mob Transport Crates

Owner-requested feature, implemented with OpenAI Codex (GPT-6) on `peepo-companion`, 8 October 2026. Uses Minecraft saved data, item components, vanilla menus and item models with Fabric interaction hooks. No new dependencies.

## Using a crate

- Wooden **Mob Transport Crate** holds four friendly mobs. Craft it from five planks and two sticks: `S S / P P / PPP`.
- **Iron Mob Transport Crate** holds eight. Surround the wooden crate with eight iron ingots in a crafting table. The custom upgrade preserves occupants, custom names and other item components, including when the crate is full.
- Right-click a calm friendly mob to capture it. Right-click the top of a block to release one occupant at the clicked surface. Nothing is placed as a block.
- Sneak-right-click in air, on a block or on a mob to open the selection menu. Each occupant has a miniature preview, name and Release button. Release searches a bounded set of clear positions immediately beside the player. If none fits, the mob stays in the crate.
- Crates are unstackable. Hover text lists the occupants and controls. The open slatted item model displays miniature occupants in the hand, inventory and dropped-item rendering; the iron version is larger with metal straps.

## Eligibility and preservation

Passive animal categories and calm neutral creatures are accepted, together with villagers, wandering traders, allays and friendly golems. Hostile `Enemy` mobs, monster-category mobs, angry/attacking mobs, dead mobs, riders, mounts with passengers and leashed mobs are refused. Other players' owned animals cannot be captured or released; Peepo and Jughead respect their existing owner/party setting. Players are never eligible.

`peepo_companion:transport_allowed` lets data packs include additional friendly miscellaneous mob types. `transport_denied` takes precedence. Allowing a type never bypasses hostile, anger, ownership or passenger checks.

Full server entity snapshots preserve identity, owner, name, variant, health, equipment and inventories. Companion energy, cargo, costumes and assignments persist; active jobs and rest poses end cleanly before capture. Stored mobs do not tick or consume server AI time. Their original dimension-scoped assignments remain saved when traveling to another dimension.

Release requires loaded space, world-border/build-height compliance, permission to interact, clear collision volume and supporting ground. Existing town protection is checked on capture and release. Mobs remain stored when a release cannot complete. Water releases are not currently supported; use suitable clear ground.

## Authority and costs

The overworld's `peepo_companion:transport_mobs` saved-data ledger owns the snapshots. Items contain random claim IDs and small visual snapshots, with no copied inventories or brain data sent to clients. A successful release consumes its ledger entry; a duplicated item cannot release the same claim again. Stale copied entries clear when used. This protects normal item-copy attempts, not manual world-file edits or partial backup restoration. Back up item inventories and world saved data together. Lost/destroyed crates leave their dormant entries in the ledger; they do not tick. There is no automatic deletion that could silently lose a recoverable mob.

Interactions are server-authoritative, reach-limited and rate-limited. Menus remain tied to the original held stack and cannot move their display slot into inventories. There are no active server scans or forced chunk loads. The client keeps at most 64 non-ticking visual mobs and clears them on disconnect/world change; rendering extracts current model states using the existing mob renderers.

## Assets

The rectangular open slat geometry follows the owner's supplied crate photograph as a shape reference; no pixels from it are shipped. `tools/transport_crates.py` generates both item models and the original 16px iron texture. `tools/generate_textures.py` includes this generator.

Wood: byte-for-byte owner texture `art/owner-library/originals/Blocks/biomes and tree blocks/origin_oak_planks.png`, imported by `tools/owner_art.py` into `assets/jugcraft/textures/block/transport_crate_wood.png`. Original library files remain untouched. Mob previews use each installed mob's existing renderer and textures. No Mojang texture files are copied.

The owner's second reference informed the iron revision: folded plates wrap all four corners, with top shoes, three rows of fastening tabs and raised rivets on both faces, plus lower perimeter braces. The existing wood and iron textures are reused. Separate flanges meet at edges rather than overlapping coplanar faces. Both crates have explicit left/right first-person and third-person transforms, and small outside handholds (wood or iron); the third-person grip stays outside the storage cavity. The shell and occupant layer share the transforms. This is a baked-model change, with no new animation hooks or server work.

## Validation

Focused runtime class: `TransportCrateClientTests` (only this class selected, unrelated feature tests not run): **42 assertions passed**, including an actual world save/reopen and release afterward. Covers captures, friendly/hostile eligibility, capacity, other-owner protection, filled upgrade, entity/item/ledger persistence, invalid and blocked releases, identity and companion inventory/settings preservation, duplicate claims, actual client menu button and held-stack invalidation. Explicitly checks that Peepo and Jughead client previews can be created; display-only companion loading skips the server-only transport goal.

Runtime screenshots of both crates and their menus were inspected. Local evidence: `build/crate-final.log` and `build/transport-crate-evidence/`. Compilation/assembly, repository documentation checks and the owner-art import check passed. The final JAR and complete launcher-managed pack are under `build/libs/` and `build/distributions/`.

Two independent clients and a populated multiplayer server remain manual validation; no existing player worlds are modified by the automated checks.

Carry/corner revision: the focused test also photographs both variants in the right hand, left hand and first-person offhand. Evidence is in `build/crate-corner-grip-final.log` and `build/transport-crate-carry-evidence/`.
