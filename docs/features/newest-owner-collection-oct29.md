# Owner's NEWEST ADDITION — October 29 local integration

Owner explicitly requested all supplied content from
`C:/Users/james/Downloads/NEWEST ADDITION/`, describing heavily edited files with
legacy names retained from editing/testing. Work by OpenAI Codex (GPT-6).
Existing local visual/raid, Encyclopedia/mentor and crafting-choice work is
preserved. No commit, push, release, launcher-profile or live-server change.

## Current checkpoint — October 31 local validation

Read the dated continuation entries at the end for current evidence; earlier
"not yet validated" paragraphs describe their own implementation checkpoint.

- The supplied-content implementation pass is in place, including recovered
  models/animations and functioning native systems; missing original inputs
  remain explicitly listed below and are not replaced by registration counts.
- Data-only audit: **PASS, 15,300 material IDs, data files and recipes**.
- Client: **PASS**, `build/newest-client-tests-statue-fix.log` (4m04s).
  **837,276 block states checked**, **14 accessory slot groups equipped and
  removed without research**, plus representative held items, creatures and
  placed blocks. The clock, bathtub and repaired statue screenshots were inspected.
- Server: full sweep completed **3,263 tests (3,258 passed, five old expectations failed)**.
  All five were corrected; **58 follow-up tests passed without optional integrations**,
  including all **19 NEWEST regressions**. The earlier thirteen failures were also
  resolved in the full sweep. This is coverage across runs, not one green full run.
- Full static geometry audit is **not clean**: 708 supplied-art findings remain
  documented separately from native resource/recipe correctness. No blanket
  suppression or destructive rewriting of original models was used.
- Everything is local. No commits, PRs, publishing, personal launcher changes,
  client updates or live-server operations occurred.

## Intake and source boundaries

`tools/import_owner_oct29.py` preserved and SHA-256 verified **98,589 files across
38 folders** in `art/owner-library/originals/Edited-2026-10-29/`. Exact file hashes,
sizes, namespaces, models, animations, recipes and prior byte-identical matches
are in `art/owner-library/catalog/edited-2026-10-29/`. Originals are not modified.
Compiled classes are static behavior/model references, never executed or bundled.
Supplied scripts are not executed. Existing notices remain preserved.

`fast paintings` is empty. Of Artifacts' 1,053 files, 1,023 match an earlier
catalog byte-for-byte; reconcile the differences with the existing accessory
system rather than duplicate all the equipment. Other collections still need
behavior-level overlap checks: different bytes are not proof of new gameplay.

## Implemented locally, not yet validated

`OwnerBuildingCollection` and `tools/newest_building.py` currently supply:

- 572 Stoneworks blocks: native full blocks, pillars, slabs, stairs and walls.
- 317 Paths entries: low/directional paths, full blocks, stairs/slabs and
  pickaxe-engraved/shovel-flattened surfaces. Tools pay durability and edits
  consult Jugcraft's claims. Original reference classes determine interactions.
- 13 consumed door blueprint items with supplied icons and paper/vanilla-door recipes.
- 294 ordinary Doors entries: native double-height doors, hinges, manual/iron
  behavior and redstone. Original models, item presentation and textures remain.
- Six garage/portcullis entries: reserved hanging columns, manual adjoining groups,
  root-only drops, redstone, bounded placement and all-member permission checks.
  Open panels retain their occupied cells instead of erasing later player builds.
  Sliding/Japanese and stable doors have open collision handling.
- 49 factory finishes including redstone-controlled fans and the original 2x2
  fan occupying four cells. Native stonecutting replaces the unavailable external
  workbench hook; large fans cost four base blocks.
- 198 fences/hedges/walls/gates, including stacked two/three-part meshes,
  selectable hinges, bounded grouped gate opening and wired-fence contact damage.
- 1,293 Chisel material finishes and the supplied chisel tool: a native finish
  picker and permitted in-place cycling, with conserved 1:1 material families.
  439 original connected/continuous tile models are adapted to the already
  approved optional Fusion pack, including the supplied animated tiles. Original
  pixels are packed losslessly; normal models remain the absence fallback.
- 157 bridges, piers, stairs, rail bridges and lights, plus pliers. Connections,
  saved manual overrides, waterlogged supports, paid extension and attachable
  redstone-torch rail signals are implemented with original meshes.
- 346 current window designs plus key and hammer: opening/locking, shutters,
  connected frames, vertical groups, curtain poses, three-position blinds and
  flower-pot parapets. Eighteen stale crimson/warped aliases without source item
  definitions are excluded in favor of their supplied registered stem/planks versions.
- 240 stair/platform/balcony designs with original selection/collision profiles,
  selectable railing styles and separately consumed/dropped balcony sides.
- 145 lights: manual switches, redstone edges, top-only stacked lighting,
  dyeable wall shades, native hanging/waterlogged lanterns, chains and luminous slabs.
- 776 furniture blocks plus cabinet-door/drawer ingredients: connected tables,
  desks, wardrobes and couches, shared 27-slot persistent cabinets, seating and
  conserved bucket/bottle sink interactions. Original source coordinates govern
  furniture orientation. Bookshelves provide native enchanting power.
- 334 holiday blocks and an edible candy cane: original seasonal geometry,
  connected garlands, switchable twinkling lights, bells, stacked canes,
  persistent snow and native plants/leaves. Decorations remain after the season.
- 256 Twigs blocks and three ingredients/novelty items: original meshes/skins,
  connected table corners, layered bamboo leaves, waterlogged floor decorations,
  columns, lamps, paper lanterns and copper oxidation/waxing/scraping. Nine-slot
  silt pots retain cargo through falling and failed landings; filled pots support
  flowers. Shell instruments and recoverable throwable pebbles use original audio.
  Recipes, natural sources and the supplied bloodstone obelisk are being adapted
  to native 26.3 codecs. These changes have not been compiled or tested.
- 90 supplied paintings: original dimensions, art and author/title metadata,
  available through vanilla painting placement.
- Supplied crafting/stonecutting recipes, block drops, tags and translations;
  a dedicated building tab. Stable Jugcraft IDs use stoneworks_, pathworks_,
  doorworks_ and gallery_ prefixes. No existing registry IDs are replaced.

Current generated subtotal: **5,296 building blocks, 27 nonblock items and 90
paintings**. These are implementation/resource counts, not a completion or test
coverage claim. Only the earlier 1,183-block subset loaded during the equipment
tests. None of the subsequent additions have been compiled, tested or launched:
the owner requested one combined validation after the entire collection is implemented.
Resource closure, native behavior, survival access, new conflicts, claims,
rendering and persistence must be validated after implementation.

Implementation bounds to cover in combined validation: garage columns/groups
are limited to 16 blocks/16 columns; grouped windows reject more than 64 members;
bridge extensions do not load new chunks; architectural collision uses the
supplied cuboids' rotated bounding boxes, with rescale handled. Angled-geometry
collision and furniture seat alignment still need visual playtesting. Original
source classes were inspected only as static references, never executed.
Twigs' archive has data for `noise_strip` and `azalea_flower_patch` but no supplied
implementation classes for those codecs. The native adapters use bounded silt
ribbons and attached flower patches; exact equivalence to the absent generators
is not claimed. Original ore sizes, biome tags, placement frequencies, shell
weights and supplied structure geometry are retained. No existing chunks change.

## Remaining collection work (not completed by intake)

- Buildings: finish Twigs resource/API integration; Handcrafted/Home
  differences and original interactions.
- Functional household/utility: Supplementaries, Amendments, loot containers,
  backpacks, enchanting changes and enchantments.
- Magic/progression: Jugtania, forbidden arcane, Ars additions and Icarus.
- Creatures/fishing: Aquaculture, Ribbits, Mowzie creatures and Moobloom variants;
  original rigs, animation controllers, encounter state and acquisition.
- Artifacts resource reconciliation is complete; runtime acceptance remains part
  of the combined validation described below.
- Client integration: chat animation, connected textures, tooltip frames,
  third-person camera, zoom and no-chat-report collection. Inspect exact behavior,
  pinned-platform compatibility and existing renderer/network responsibilities;
  do not automatically add external mods or replace protocol behavior.
- Empty `fast paintings`: no supplied implementation files exist in that folder.

Keep all work local. Registration, copied art, unchanged-file overlap and a
catalog entry do not count as functioning gameplay. Record actual native limits
and tests, and retain the owner's control of their personal playtest.

### October 30 continuation — still local and untested

- Handcrafted reconciliation compared all 14,891 supplied resource files with the
  earlier integrated owner folder: byte-identical. Its existing 267 blocks and
  18 items are retained, not duplicated. Exact hashes and paths are recorded in
  `handcrafted-reconciliation.json`; the generator now names the latest source.
- Home now has 313 generated blocks and four items. Eleven original declarative
  model rigs were recovered statically with original pivots, rotations, UVs and
  mirror/inflation data; native chair/sofa/clock/telescope rendering uses them.
  Runtime work includes cushions, dye/paint-brush colors, persistent 24-slot
  counters and nine-slot desks/drawers, seating, linked three-cell clocks and
  two-cell baths, clock hands/pendulums/audio, telescope aiming/zoom, lamp groups,
  original connected meshes, paid fluid transfers and claim checks. No runtime
  result is claimed. Remaining Home integration details include source edge-case
  interactions, exact joins/collision/animation acceptance, resource closure and
  all native API checks. The supplied saddle block and mangrove zaisu lack their
  referenced model geometry. The dye vat's missing fluid planes are filled
  by native water surfaces using the original complete vat mesh and skins; this
  resource change is included in the regenerated subtotal.
- Source helper programs remain unexecuted. `newest_home_models.py` parses static
  decompiler output; the model-recovery catalog distinguishes recovery from
  runtime binding. Unused original rigs are preserved without inventing behavior.
- Easy Magic native adaptation is now in implementation: persistent vanilla-table
  inventory, shared concurrent menus, one-lapis/five-experience-point rerolls,
  lenient shelf obstruction and filled chiseled-bookshelf power. Native animated
  book rendering is retained with an actual stored-item display and explicit cost
  button. Inventory serialization, hopper behavior, multiple viewers, permissions,
  resource rendering and mixin signatures still require the final combined checks.
- Reforged's 49 supplied enchantment definitions now have local native data and
  effect integrations: combat reactions, paid bounded mining chains, smelting and
  crop drops, projectile effects, movement/armor utilities, fishing, durability,
  lightmap lift and movement vibration suppression. The source acquisition tags
  are retained for enchanting, loot and trades. The 43 supplied vanilla reference
  entries are integrated into tooltips. Lumberjack suppresses a second TreeFelling
  job; every chained block uses ordinary permission and tool-wear paths. Effects
  remain uncompiled and untested, including mixin targets, animation/prediction,
  concurrent interactions and compatibility with the existing magical weapons.
  These local implementations are not a source-parity or release claim.
- Artifacts reconciliation now distinguishes the 992 byte-identical runtime
  files, 46 conditional overlays and 15 generator-cache files. No unresolved
  gameplay-resource differences remain against the already integrated October 23
  supply. Optional overlays that disable accessory slots are intentionally not
  activated. Both native generators now read the newest supplied source folder.
  See `artifacts-reconciliation.json`; this is not a new gameplay-test result.
- Personal loot: twelve native container blocks now store independent persistent
  player inventories, use the original chest/shulker/barrel/pot appearances, and
  prevent hopper access to personal rewards. Unopened generated vanilla
  containers convert on use; ordinary player storage does not. Decorated-pot
  patterns are retained, and failed replacement restores the complete original
  block-entity data. Suspicious sand/gravel require timed brushing. An operator
  command assigns a loot table once without rerolling existing inventories.
  Minecart/item-frame variants, configurable refresh/decay/team behavior and
  complete brush progress presentation still remain. Two referenced trophy
  skins are absent from the supplied source; no substitute textures were made.
- Home chair seating retains 16-way headings; the chair collision now follows
  its rotation/tucked pose. Bridge connection states rotate/mirror with
  structures, including handed rope ends and mirrored stairs. These changes
  require the deferred gameplay/visual checks.
- Backpacks now have a local physical storage implementation for all six source
  tiers (27/45/54/81/108/120 slots and 1/1/2/3/5/7 upgrade sockets), a paged native
  menu with ghost filters, placement/pickup and component-preserving tier
  recipes. Original base/strap/pocket geometry and skins are adapted from the
  supplied model loader. The resource generator includes 71 supplied item
  presentations and 27 component-preserving recipe patterns; neither count is
  a claim that every upgrade behavior is complete.
- Backpack behaviors currently implemented locally: bounded claim-aware pickup,
  magnet, feeding, refill, tool swapping, deposit/restock and explicitly filtered
  voiding; native crafting/stonecutting/anvil/smithing menus; persistent paid
  smelting/smoking/blasting modules and their automatic input/output variants.
  The carrier is locked while its menus are open. Generated loot and player-
  reserved dropped items are excluded from unattended acquisition. Shared placed
  pack access is serialized. Storage updates, native menu/cooking signatures,
  source capacities, colors and recipe resources remain uncompiled/untested.
- Backpack utility work now also includes paid 2×2/3×3 compacting with atomic
  output/remainder fit checks, a 100,000-JE battery charging existing Jugcraft
  equipment, explicit 100-point XP deposit/withdraw controls, and a transactional
  four-bucket tank. Tank items and installed pump modules transfer actual fluid
  to/from sided storage with claim checks. These are bounded native capacities;
  full supplied configuration behavior and runtime acceptance remain open.
- Further backpack implementation now includes the supplied stack multiplier and
  downgrade families, three-module limit, count-preserving conversion items,
  bounded normal-stack inventory windows and separate serialized large counts.
  Capacity reductions are rejected while contents would no longer fit. Native
  container packets never carry the extended counts as oversized ItemStacks.
- Backpack filters now belong to individual installed upgrades: nine basic or
  sixteen advanced ghost samples, include/exclude, item/exact-component matching,
  and pipe input/output direction. The leather interface has workshop/utility
  pages, real count readouts, XP controls, music controls and pump settings.
- Added original-intent alchemy (paid consumption of useful potions/effect foods
  and cures), everlasting dropped-pack protection, explicit basic/advanced
  jukebox playback with bounded disc playlists, and mob catching with saved
  entity state and reserved rectangular inventory space. Source health/tier
  costs are retained; bosses, town residents, other players' pets and encounter
  lifecycle entities are excluded. Creature payloads are bounded to 16 entries
  and 64 KiB of textual NBT. Creature-slot preview rendering still needs work;
  the current UI marks occupied space and supports release rather than claiming
  the original entity preview is complete.
- Added a wearable back slot and original tier-model rendering attached to the
  animated body, plus an inventory Pack button and optional unbound hotkey (the
  existing N binding for Traveler's Backpack is preserved). Portable workstation
  validity and removal guards include the worn carrier.
- Placed item/fluid ports now use transactional snapshots, current owner/claim
  checks and view exclusion. Manual world pumping is restricted to ordinary
  water/lava source blocks; advanced automatic item-container filling/draining
  is opt-in, bounded and simulates output fit before updating the real bag.
- Backpack infinity is now implemented with operator-only admin upgrades and a
  disabled-by-default survival option. Placed cooking, compacting, internal
  charging and tracked music now run with owner/claim checks and view exclusion.
  Carrier identity is preserved during inventory refresh; placed batteries do
  not remotely charge their owner. Workstation sessions are cleaned on ticks
  and shutdown. Full source advanced settings, creature preview, resource/API
  closure, source color/fit acceptance and all runtime validation remain open. New code above has not been compiled, tested or launched.
  Other remaining collection families listed above are still open. Registration
  and copied art are not completion evidence.
- Corrected the architectural loot generator to emit 26.3 condition/modifier
  schemas consistently. A generator naming collision was fixed; subsequent
  resource generation completed. Later Home changes still require regeneration.

No compilation, tests, game launches, commits, pushes, profile edits or server
actions were performed for this continuation. Full collection completion remains
open; these additions are not a release or an assertion of full source parity.

The equipment regression launch exposed 13 missing door-print ingredient registrations.
Their native items, supplied item models/textures and crafting recipes are now included;
registry loading now passes in the equipment-focused server/client runs. The broader collection remains unfinished.

A later model-closure audit also found missing supplied door variant geometry
(e.g. `mcwdoors:block/waffle/cherry_waffle_door_bot_rh`, with no corresponding
cherry waffle block skins) and an unreferenced `snow_path` experimental model
without its skin. The importer now traverses reachable models and excludes the
incomplete cherry waffle door. The six formerly pending garage/portcullis entries
now have dedicated mechanics described above. It also excludes the unreachable snow-path model. Runtime
resource generation and model-atlas generation completed after those fixes.
Original files remain preserved. The equipment-focused tests do not certify
this content. Japanese/sliding and stable-door open collision has since been
refined and remains awaiting the combined validation.

## October 30 continuation — additional local implementation, not tested

- Icarus/Skyweave: 83 wearable wing items, supplied six model families and
  layered skins, native gliding/forward boost, synchronized stamina, three
  effects, no-flight dimension-type tags, crafting and wearable/HUD rendering.
  Exact loop/camera/configuration parity and runtime/model fit remain open.
- Flowery Mooblooms: 16 supplied adult/baby skin variants and biome rules,
  native cow behavior, inherited variants, milk/breeding, claim-aware flower
  shearing/regrowth and visible flowers. The supplied add-on has no base
  Friends & Foes model/gameplay; native cow geometry is an explicit adaptation,
  not a claim that a missing original entity model was recovered.
- Aquatic collection: 30 fish entities with supplied meshes, animations, skins,
  habitats, bucket forms and nutrition. Species-specific schooling avoids
  mixed schools caused by the shared Java class. Jellyfish retain contact damage.
  Six knives now use source fillet yields and return worn knives; four rods
  have persistent tackle sockets and a native menu. Source fish-biome weights,
  bait consumption, hook cast weights/luck/wear, double catches and redstone
  catch window are being connected to native fishing. These changes are still
  in progress; source loot/rewards, other equipment, workstations, turtles,
  fishing presentation/audio and API closure are not yet complete.
- Removed a duplicate backpack conversion `use` method encountered during
  source reading. No compilation or test was run to claim acceptance.

All of this remains local and uncompiled/untested. The rest of the collection
listed above remains required; registration and retained art are not completion.

### Continuing October 30 — aquatic and personal-loot follow-through (unvalidated)

- Aquatic fishing now appends the supplied weighted fish/junk/bounty entries to
  the native fishing pool; the rare bounty retains its open-water requirement.
  Original reward-box tables, message bottles, 30 fish and three turtle habitats,
  native tool/armor adaptations, permanent moist soil and paid worm composting
  are connected. The tackle box has 17 persistent storage slots plus four rod
  sockets; the bounty is a normal persistent 27-slot chest. Both use supplied
  geometry/skins and opening animation; contents use native container components.
- Added fish trophies with original plaque JSON and actual fish meshes,
  native hanging-entity persistence/conserved drops and claim checks. The source
  bobber/hook layers and dye colors render with native fishing-line hand tracking.
  Fish-weight competition rules are opt-in (`aquatic.fish_weights=false` by
  default); recorded weights survive in item components and determine fillets.
  Spawn eggs, tool/enchantment/food/dye tags and crafting sources are generated.
  Water-arrow behavior preserves native arrows rather than registering duplicate
  projectile types. Leggings use native buoyancy reduction and boots native water
  movement efficiency instead of unavailable NeoForge attributes. These are
  explicit adaptations, not an assertion of exact platform behavior parity.
- Personal loot now shares a server-only reward journal across block containers,
  minecarts and frames. Generated unopened vanilla loot minecarts convert on use;
  ordinary storage carts and player frames do not. Optional `loot.party_shared`
  binds the first-visit party permanently per player/cache to prevent party-hop
  rerolls. Leaving that party closes access to its shared cache. Defaults remain
  individual, once-only rewards. `loot.refresh_ticks` only refills empty, closed
  caches, and `loot.decay_ticks` stops new rolls while preserving existing rewards
  and the physical container; both default to zero. Minecart hopper inventories
  stay empty. Structure/admin frames accept assigned tables via
  `/jugcraftlootentity <entity> <table>`, not survival duplication of frame items.
- Personal archaeology has owner-only particles and progress feedback; stale
  brush progress is discarded. Original chest/cart/frame appearances are wired
  with per-player visited state. Existing saved block inventories are migrated.

All changes above remain local, uncompiled and untested. Combined API/resource,
claims, conservation, persistence, multiplayer and rendered checks are still
required after the remaining collection implementation. No publish/server action.

### Continuing October 30 — Ars additions (implementation in progress)

Implemented locally by OpenAI Codex GPT-6; none of the following has been compiled,
launched or tested yet:

- Twelve supplied charge-based charms now have native protection/effect hooks,
  inventory and accessory-slot use without research, charge tooltips and paid
  Source Jar/Ley Pylon recharging. Fractional Source credit is retained rather
  than giving free charges. Enderman stare, piglin armor checks, snow walking,
  wither/dispel protection, night vision, falling, drowning, fire, sonic damage,
  void rescue and death rescue are wired. Original icons and apparatus recipes
  are generated. Exact source recharge apparatus behavior remains to reconcile.
- Thirty-one decorative blocks use supplied models/skins and native doors,
  trapdoors, buttons, walls, chains, carpet and lantern mechanics. Magelight
  lanterns accept light/dispel spells and manual brightness/color state.
- The XP jar filters opted-in hotbar pickups and yields one XP for two consumed
  items, preserving the odd-item remainder. The Haversack deposits real stacks
  and filtered pickups into loaded authorized storage. It does not load chunks
  or expose loot/locked machine inventories. Exact animated load-state item
  presentation and additional safe transfer-capability adapters remain open.
- The Memory Crystal has ten selectable, lockable/clearable configurations with
  server-owned icon buttons. It copies detector/sensor/rune/turret settings and
  a Starbuncle route; never inventories, energy, ownership or job progress.
  Turret spell memories persist separately from physical input items. Full
  multi-route source-handler equivalence still needs comparison.
- Ender Source Jars share one persistent per-owner Ley balance across dimensions;
  donor accounting de-duplicates shared balances and prevents self-transfers.
  Enchanting Wixies can consume apparatus ingredients from authorized linked
  inventories, preserving remainders and paying recipe plus worker energy. They
  reuse the apparatus mesh/animations exactly as the supplied renderer does.
- Source Spawners use loaded, authorized nearby specimen jars, population caps,
  source costs, redstone disable and saved delays. Spawned creatures keep only
  appearance data, not inventories/owners/equipment; denied bosses and worker
  NPCs remain excluded. These are native adaptations requiring combined checks.

Still open in this family: advanced Dominion tools; warp indexes/nexus/scrolls;
Mark/Recall/Retaliate and reliquary; codex/scribing/parchment, reach thread and
locating/chunk rituals; complete resource/API closure and acceptance. Other
collection families above remain unfinished. No commits, publication, personal
profile changes, live server actions or tests were performed in this continuation.

### Continuing October 30 — Nexus, spell memories and scribing (unvalidated)

Local implementation by OpenAI Codex GPT-6, still not compiled or tested:

- Warp indexes open the existing storage/crafting terminal with live held-item,
  loaded-destination and owner/party checks. Stabilized indexes permit another
  dimension. Advanced Dominion wands have explicit direction/group controls and
  bounded, permission-checked block linking; helper selection uses existing logic.
- The two-cell Warp Nexus uses the supplied geometry and open/spin/close clips,
  a persistent private 27-scroll destination inventory, safe landing checks and
  10 Ley per constructed-nexus trip. Generated free nexuses remain public. The
  native crafting route supplements source structure placement. It never copies
  private inventory into the public block. Shared Source Jars no longer export
  their cached shared balance as portable charge.
- Mark records a location/creature in a held Reliquary; Recall casts at that
  loaded same-dimension target; Retaliate targets the recent attacker. Effects
  retain the existing claim/PvP/work-ledger checks. Recall retains supplied
  50/250/1000 durability costs for location/creature/player and a removable
  five-minute player mark. Recall/Retaliate cannot directly contain Mark.
- Imbuement chambers copy a permitted pedestal's master spell into actual input
  equipment/parchment. Imbued parchments pay Ley during inscription and retain
  the exact prepaid inscription; they consume one on successful casting. Editing
  an inscription cannot keep its prepaid status. Codex entries use the supplied
  already-known XP fallback (55/160/480), since owner instructions forbid putting
  equipment behind research. Reach thread uses existing armor sockets and native
  interaction-range attributes.
- Locating rituals match supplied augment recipes and produce a saved Wayfinder;
  failures refund the tablet/augments without partial-output duplication. Search
  is bounded to 32 structure regions and one attempt per player/minute. Native
  compass dispatch uses the supplied direction frames. Expedition scrolls bind
  from an offhand located Wayfinder and consume on safe arrival. Source loot's
  automatic exploratory destination and thrown-portal behavior remain to finish.
- Arcane Permanence uses expiring tickets for the station's chunk, source-paid
  active time, online/access rechecks and limits of one per owner/sixteen per
  server. It does not retain permanent tickets after a stop/restart. Native limits
  intentionally differ from the source's effectively unlimited configured count.

Still implementing: supplied Ars structures/loot/stored block data and natural
encounter behavior; exact source recharge/copy/reliquary-device edges; Haversack
loaded presentation, additional transfer ports and collection-wide closure.
The previous broad remaining-family list is still open, not replaced by these
additions. All new content remains unvalidated; no compilation, test, game launch,
publication, launcher/profile or live-server operation has occurred.


### Continuing October 30 — source mechanics, client tools and Ribbits

Implementation by OpenAI Codex (GPT-6), still local and **not compiled or tested**.

- Spellweave now gives ordinary equipped armor one thread socket, with strength
  from the enchantment level. Its original enchantment metadata/apparatus recipes
  and Snoozebuncle painting are adapted. Equipment still has no research gate.
- Device Mark/Recall uses a physical accessible Reliquary in linked/local storage,
  not the owner's held relic. Written spell scrolls can be source-paid into
  prepaid scrolls. Ars structure translation now handles original stored items,
  potions, equipment and spell presets; separate supplied loot injections are
  consumed by the existing native loot integration.
- Native optional-key zoom has smooth FOV transitions, scroll strength, secondary
  zoom, sensitivity scaling and persistent settings. Tooltip presentation uses the
  supplied frame atlas, centered titles and item previews. These are partial
  adaptations: advanced source zoom camera/overlay policies, tooltip resource
  selectors and rotating previews remain. Neither has runtime acceptance yet.
- Ribbits now have original profession/instrument/umbrella rigs and shared clips,
  positional music/maraca loops, bounded crop watering, fishing poses/splashes,
  sorcerer buffs, finite merchant trades, home assignment and saved profession.
  Original village templates, household models, textures, sounds and crafting data
  are translated. Native jigsaw placement preserves the layout pool and spacing.
- Ribbit village processors were NOT supplied as implementations. Native adapters
  use the supplied replacement palettes/probabilities and bounded foundation
  columns. They do not claim exact YUNG terrain or missing processor parity.
  Fishing-trade source mapping, musical phase synchronization and processor marker
  details remain to close, along with all resource/API and gameplay checks.

Open scope remains substantial: Supplementaries/Amendments, Jugtania, forbidden
arcane, Mowzie creatures, remaining client families and the earlier recorded
family edge cases. No tests/builds/game launches/publication/profile/server edits
were performed in this continuation. Registration or adapted files do not mean
these families are complete.


### Further October 30 local continuation — interactive household and presentation

- Added source-style chat message/input transitions with configurable timings,
  monotonic animation clocks and reduced-motion support. Native signing, trust
  indicators and transport are retained; this does not complete No Chat Reports.
- Added native third-person free orbit, camera-relative walking and interaction
  aim alignment, excluding mounted/swimming/gliding/sleeping/spectator cases.
  An unbound toggle persists the player's preference. Runtime camera acceptance,
  exact source configuration options and special-input edge cases remain.
- Tooltip item context now follows the actual deferred tooltip callback rather
  than a stale last-hovered item. Custom tooltip styles are left intact.
- Began Supplementaries native household integration with the supplied meshes:
  storage/safe/lunch/display/posted-book, crank, clock, redstone crystal display,
  hourglass and rotation table families plus ordinary building pieces. Source
  sack/lunch capacities are 9/6; safe is 27. Hourglass materials/durations come
  from the supplied data. Rotation checks the owner's authority and avoids block
  entities/multiblocks. Menus, saved contents and display rendering are local.
- Supplementaries is **not complete**: remaining functional families, dynamic
  renderer parts, source-only interactions, portable storage components, exact
  analog rotation, map display, survival-source closure and API acceptance still
  need work. The catalog `supplementaries-local.json` lists current scope and
  remaining block IDs; it is not completion evidence.

All changes remain uncompiled/untested pending completion of the collection.
No commits, pushes, profile modifications or live-server operations occurred.

Ribbit fishing trades now retain the source seven-entry pool (four selected offers),
including axolotl/tropical-fish buckets, cooked fish, enchanted fishing rods and
raw-fish purchases. Old intermediary IDs were resolved with the official Fabric
[Yarn item mappings](https://maven.fabricmc.net/docs/yarn-1.21.1+build.3/net/minecraft/item/Items.html).
The supplied June pride model and rain-dependent holding clips are connected;
server-synchronized June selection avoids client/server calendar disagreement.
Still no compilation or tests.

### Continuing local integration — household gameplay and connected surfaces

OpenAI Codex (GPT-6). No compilation, tests, game launch, publication, profile
change or server operation has run during this continuation.

- Repaired Continuity tile import for native sprite-object texture references
  and the source `matchBlock` spelling. The original full-CTM/horizontal pixels
  are repacked losslessly for the approved Fusion renderer. A separate **optional**
  built-in resource pack exposes these surfaces without replacing a player's
  selected art automatically. The two conditional top-overlay rules and programmer
  art selection are still outstanding; this is not full Continuity parity.
- Portable sacks, presents and lunch baskets now serialize actual cargo into the
  dropped item and restore it on placement, with recursive container nesting
  refused. Creative breaking preserves nonempty cargo. Private storage contents
  are excluded from ordinary block-entity visual update packets.
- Corrected the crank to the supplied persistent 0–15 forward/reverse control.
  Added directional relayers, analog illuminators, rune lamps and extinguishable
  floor/wall sconces with a separate lever variant. These are native implementations
  awaiting validation, not evidence that every original control edge case works.
- Three-slot flower boxes render the inserted plants; planters support native
  vegetation, crops and stems. Timber frames accept/display a real full building
  block. Fixed original custom model-loader wrappers to use their supplied base
  geometry instead of unresolved loader IDs.
- Faucets transfer actual fluid or items between adjacent storages, with online
  owner authority, loaded-cell checks and checks on the source/destination. Whole
  world-source buckets can fill a tank transactionally. Full source parity for
  cauldrons, entity targets, flowing visuals and loose-item output remains open.
- Original clock-hand and wind-vane meshes now animate. Hourglasses show changing
  sand, use original material durations/light values, and restrict automated
  insertion/extraction to the original timing rules. Sand is a native chamber
  rendering adaptation; exact alignment and all materials need visual validation.
- Jars hold finite fluid (native three-bucket capacity), twelve individual cookies,
  or a captured small mob; cages carry small mobs. Modes are mutually exclusive.
  Capture checks claims/ownership and rejects mounted/leashed entities. Release
  preserves UUID, refuses duplicates/obstructions, and clears saved data only
  after successful spawn. Placed contents use native creature/item/fluid rendering.
  Boat jars, explicit tagged exceptions, bucket-fish interactions, source soft-fluid
  food/drink effects and occupied held-item rendering remain to finish.
- Noticeboards paginate long text rather than truncating it and display stored
  maps. Town visitors can read a public notice but cannot change its contents.

`jar_tinted` is an orphan supplied blockstate: the inspected original registry
has no registration and its referenced model/texture/item are absent. It has not
been replaced with an invented skin or counted as a working registered variant.

Remaining collection scope is still substantial, including Supplementaries and
Amendments edge cases, Jugtania, forbidden arcane, Mowzie, remaining client families
and previously recorded creature/equipment/Ars/backpack details. The catalog is
an implementation inventory, **not** a completion or test report.


### Continuing local implementation — drawing, crops and household machinery

OpenAI Codex (GPT-6), local and **uncompiled/untested**. No commits, pushes,
profiles, updater channels or server state changed. Combined validation remains
pending the complete collection implementation, as requested.

- Blackboards retain the supplied frame and textured chalk surface, with a native
  16x16 drawing screen, continuous strokes, undo, clear, explicit save/cancel and
  copy/paste of bounded drawings. Native color selection extends the source's
  black/white GUI using its original color mapping. Drawings survive block drops;
  glow ink, waxing, axe removal of wax, direct dye drawing and soap clearing work
  through server-side edits. UUID edit sessions, expiry, distance, claims and board
  revision checks prevent stale sessions from overwriting a replacement/other edit.
  Connected multi-board GUI layout and held-picture previews remain to close.
- Supplied flax now has both crop heights and a real fiber/seed connection to the
  existing farming chain. Wild flax uses the source sand requirement and bounded
  bonemeal spread, refusing town/claim boundary crossings. Its original referenced
  biome tag was absent; native river, plains and flower-forest defaults are explicit.
- Pancake stacks have eight layers, toppings and the source food/speed effect.
  Ash falls/merges, softens falls and erodes in rain; fodder stacks can be shaved
  with a hoe; gravel can be raked into connected patterns. Soap blocks slip under
  moving feet. Metal gates retain waterlogging: iron follows redstone; powered gold
  locks its current position. Adjacent gold-gate use checks every affected cell.
- Soap uses the supplied art and animal-washing actions and integrates the existing
  Jugcraft status-cleansing behavior; dyed held gear and unwaxed boards can be washed.
  Ash bricks and soap recipes are available, with a native charcoal-to-ash furnace
  route where the supplied alternative depended on an absent soot mod. Original
  fire-consumption ash hooks/complete wash-recipe parity remain outstanding.
- Bellows use the original declarative mesh, compression motion, texture and audio.
  Directional airflow is blocked by solid/protected cells; online-owner authority
  gates entity movement and furnace acceleration. Extra furnace ticks use native
  fuel/recipe accounting. Wet-sponge soap particles and some original interactions
  remain outstanding.
- Spring launchers reserve a physical front part, animate the supplied head during
  their two-tick stroke and launch permitted entities at the supplied velocity.
  Retraction checks the original base; occupied cells are not overwritten. Moving
  parts have no collectible item/loot. Reload/collision/visual behavior needs the
  deferred combined tests.
- Candle holders use all supplied colors and floor/wall/ceiling geometry, native
  stacking/water/extinguishing behavior and the original flame offsets. Fire pits
  use the supplied model, lighting, smoke, contact damage and comparator output.
- Corrected native bonemeal signatures and the renamed farmland model parent.
  Replaced the supplied impossible fodder `layers=16` loot rule with actual layer
  counts; preserved shared tag contents while adding required source item tags.
  Resource generators ran to produce assets; that is **not** compilation or testing.

This is additional implementation, not completion of the whole collection. The
large magic/creature families and earlier listed parity work are still open.


## Continuing local implementation — furnishings, traps and workshop controls

Work by OpenAI Codex (GPT-6), local only. No compilation, automated tests or game
launch has occurred during this continuation. Generators produced runtime copies;
static source/API inspection is not validation.

- Corrected the bellows manual stroke so standing on it cannot continuously reset
  the compression start. Moving spring heads now expose their translated head
  collision. Both devices use bounded entity collection (32), with direct shared
  attack permission checks before affecting other living entities.
- Preserved the source awnings' extra model rotation, reversible slant, supported
  placement, bounce/crouch behavior and bounce sounds. Added the original cog's
  attenuating redstone behavior. Placed and held vessels/blackboards share content
  rendering; held rendering initializes only when item extraction is available.
- Added six crossing-rod families with source meshes/textures, placement on three
  axes, correct rotation and one drop per constituent rod. Vanilla stick/blaze/
  breeze rods and prismarine shards have reversible placeable selection recipes.
  Blaze rods have environmental contact heat outside claims/towns; breeze rods
  use a native gust particle in place of the unavailable source particle codec.
  Edelwood/propelplant survival inputs still depend on their collection work.
- Bamboo spikes support six attachment directions, original tipped appearance,
  lingering-potion coating, 16 saved charges, washing, portable coating data and
  source-strength contact damage/effect duration. Native adaptation uses finite
  charges for harmful effects too. Owner presence, claims and shared combat rules
  apply; no fake attacker or offline damage. Potion/effect cooldown is persisted.
  Original piston interaction/fluid-potion automation remain to close.
- Book piles hold four real saved books, retaining text and enchantments. Crouch
  placement, standing/stacked orientation, targeted retrieval, supplied cover
  variants/glint and fractional enchanting power are implemented. No empty pile
  item or duplicate inventory drops. Vanilla book variants are supported; optional
  source book types resolve only if their item IDs actually exist. Visual layout,
  enchantment updates and hopper/reload behavior await combined validation.
- Groupers compare adjacent block states or bounded nearby entity sets. Source
  none/partial/full signals are 0/7/15. Item component, falling-block state, animal
  variant, villager profession and named-entity comparisons use native 26.3 data.
- Ropes retain multipart connections, waterlogging, climb/standing distinction,
  shearing and bounded downward extension/retrieval, checking changed positions.
  Missing/unloaded neighbors are not force-loaded. Pulley, leash-knot, rope-arrow,
  chandelier, bunting and fire-propagation integration remain open.
- Speakers/doormats have server-validated, revision-checked text editors with save
  acknowledgments, retained drafts on conflicts, wax/dye/glow controls and bounded
  plain text. Doormats hide/retrieve a real item. Speakers support chat/action-bar/
  title/narrator delivery, range selection, rising-edge activation, per-block and
  per-listener rate limits. Narrator respects the client's accessibility setting.
  No commands, click actions or rich JSON are accepted. Original antique font,
  custom display names and platform text-filter integration are not complete.
- Statues use the source's native player-model basis and supplied stone skin,
  standing/holding/tool/sword/candle poses, redstone waving, stored displayed item
  and candle light. Named-player skins/capes, seasonal accessories and special
  held-globe rendering remain open; no remote skin fetch was added.
- Goblets retain a real potion/milk/honey serving, original cup mesh and colored
  liquid surface. Pouring returns the original empty vessel; withdrawal requires
  that vessel; drinking discards its internal remainder to avoid duplicate bottles.
  Filled goblets retain contents when collected; automation cannot extract a free
  full bottle. Full source soft-fluid compatibility remains open.
- Added player-facing instructions to the existing Household Encyclopedia section.

The whole collection is NOT finished. Supplementaries still has functional
families and compatibility work; Amendments, Jugtania, forbidden arcane, Mowzie,
remaining client systems and the earlier recorded integration gaps are still
open. Source files with no corresponding source registration (for example drawers,
diode and tinted-jar leftovers) are not evidence of implemented gameplay and are
not silently registered as broken/inert content. The final combined checks have
not started, in accordance with the owner's instruction.


## Continuing local implementation — instruments, rotation and party tools

OpenAI Codex (GPT-6). Local implementation only; no compilation, tests, game
launch, publication or deployment during this continuation.

- Flutes now bind/unbind up to six owned tame animals/horses, recall loaded pets
  within 128 blocks, and call a bounded nearby standing-pet group when unbound.
  Ownership is rechecked at recall; source/destination claims, border, collision,
  liquid, passenger/leash state and loaded chunks are checked. Giant pets over
  four blocks are excluded from this small-companion recall route. Bamboo repairs
  the 64-use instrument. Original songs, named-song selection, source pitch/chords,
  first/third-person playing poses and original held/inventory models are adapted.
  Trusted-fox binding, parrot dance synchronization and exact hand/head alignment
  remain to close; the pose is not visually validated.
- Altimeters display numeric elevation and the original moving dimension-specific
  strip (Overworld/Nether/End), using the supplied frame and texture heights.
- Turntables now place toward six directions, use source power-to-speed periods,
  show rotating states, rotate ordinary container states without removing their
  block entities, and handle directional blocks/pillars/stairs/slabs/face mounts.
  Linked beds/doors/double chests/extended pistons are deliberately rejected until
  their multi-cell rotation is implemented. Entity/minecart rotation remains open.
- Confetti poppers retain original held/head meshes, sounds and particle sprites.
  Server-issued bounded bursts, one-popper/up-to-eight-dyes crafting, crouch-placed
  four-part litter and wearable hats are implemented. Converted party creepers
  retain a synchronized/saved flag and original hat; their detonation is replaced
  with a harmless burst and removal, including suppressing an effect cloud.
  Particle drift uses a native lightweight flutter adaptation rather than the
  source multi-axis noise renderer. Rendering and multiplayer acceptance pending.
- Fixed an accidentally duplicated goblet empty-hand branch and the unfilled
  statue's initial light state during source review.

This does not close the collection. Earlier outstanding families and limitations
still apply. The owner was given a rough **40–50% implementation planning estimate**,
not a counted completion/test percentage; large magic/creature families remain.


## Continuing local implementation — flags and Amendments placement/cauldrons

OpenAI Codex (GPT-6). This continuation remains local and uncompiled/untested.
No game launch, commits, publication, profile changes or server actions occurred.
Asset generators were run to write runtime resources; that is not validation.

- Supplementaries' sixteen flag colors now retain banner patterns/custom names,
  copy patterns with the source returned, and raise/lower along bounded real stick
  poles. Original pole/item presentation is used; animated cloth uses native
  banner pattern layers. Wrenches rotate permitted blocks/armor stands, with
  repair/durability/sounds. Turntables reject linked machine footprints and named
  segment/part states. Flag/extended-head renderer distance remains bounded.
  Map-marker integration, linked rotation and visual acceptance remain open.
- Started Amendments native implementation using the supplied static behavior and
  model references. Carpets cover bottom slabs/stairs while retaining the real
  underlying block, native stair joins, waterlogging and emitted light. Dyes
  recolor; shears restore the underlying block and return the carpet. Original
  overlay geometry is reused with the selected wool material.
- Tool hooks retain the actual tool, components and durability, restoring an
  unused tripwire hook on retrieval; attached/powered tripwire is not converted.
  Hanging flower pots accept actual pottable plants. Wall lanterns use original
  brackets and native lantern skins. Bell chains scan at most sixteen loaded cells
  in each vertical direction and respect permissions at the bell.
- Directional cake eating and double cakes are implemented (seven servings per
  cake; the upper cake is eaten first), with native food/stat/comparator behavior.
- Skull piles and up-to-four-candle skulls retain original head components,
  including player profiles and note sounds. Lit candles can be extinguished;
  crouch-removal restores the head and returns candles. Ceiling banners retain
  colors/patterns with source placement proportions and native animated cloth.
- Dye/potion cauldrons, stored dye bottles, component-preserving dye recipes,
  sheep dyeing and manual tipped-arrow batches have local implementations.
  Three bottle portions per cauldron; four individual recolors or two eight-arrow
  batches per layer. Bottle extraction consumes a full layer, including a partly
  used layer, preventing fraction refunds. Different potion effects are diluted,
  limited to eight, and instantaneous effects cannot mix with different potions.
  Dye color mixing uses linear RGB (an explicit adaptation of source HCL mixing).
  In-world dropped-item crafting, broader dynamic recipe/soft-fluid compatibility,
  boiling, enhanced projectiles, hanging-sign displays, falling lanterns, water-lily
  support and remaining source interaction/render details are still open.
- Restored the main branch's fish-tag entries alongside the dirty aquatic additions;
  the aquatic generator already merges tags rather than replacing them.

This does NOT complete Amendments or the collection. Supplementaries gaps,
Jugtania, forbidden arcane, Mowzie and earlier recorded gaps remain. None of these
new native API, resource, preservation or render paths have run in Minecraft yet.


### Further local implementation — charge combat and household interactions

OpenAI Codex (GPT-6). Still local, uncompiled and untested, with no game launch,
publication, profile changes or server actions. Asset generation is not validation.

- Added the supplied fire/dragon charges, original trail/explosion sprites and impact
  audio, tumbling held-item presentation, water extinguishing, direct/splash hits,
  and a bounded two-second expanding dragon cloud. Each target uses current shared
  combat permission and line-of-sight checks; terrain is not ignited or destroyed.
  Striking a flying charge redirects it and transfers authority to the deflector.
  Sneak-use binds charges for dispensers using the existing owner-binding component.
  Bound shots require a present, authorized owner and consume only after spawning;
  unbound vanilla fire charges keep their existing dispenser behavior. Saved expired
  clouds do not reset their per-target damage budget on chunk reload.
- Snowball hits accumulate freezing; direct successful torch hits ignite briefly.
  Added one dropped-item dye/arrow batch per cauldron per second, checking the live
  thrower's permission and creating output before consuming input. Heat-source tags
  produce original boiling bubbles and native bubbling sound for water/dye/potion
  cauldrons. Ambient boiling damage is suppressed in claims and protected town land.
- Lily pads support small decorations using a water-cell support, restoring the
  surface pad when the decoration is removed. Fixed a compound `var` declaration
  encountered during implementation review. No compile has run to validate APIs.
- Hanging signs store separate front/back item stacks, retaining their components
  and original text. Sneak-use mounts an item; empty-hand use retrieves it. Wax,
  permissions and break drops apply. Native sign rendering remains the base.
- Unsupported lanterns visibly fall through native falling-block physics, including
  the new wall brackets. Landing respects claim boundaries and town protection;
  rejected landings use native item drops. Anonymous lantern fire/explosions are
  deliberately excluded. Dynamic lantern/sign sway remains a presentation gap.
- Writable lecterns open the native edit/sign screens with sneak-use. Bounded,
  expiring nonces, exact book identity/component snapshots, distance/claim checks,
  server text filtering and a second validation after filtering protect saves.
  Normal reading/taking and held-book editing remain native paths. Editing and
  signing are implemented; extra source font controls are not yet implemented.
- Added fence/wall/bar/material connection adapters and bubble-column traversal
  through tagged wet grates, preserving the actual grate blocks. Downward support
  lookups are bounded to sixteen grates. Neighbor-change propagation still needs
  implementation review alongside the combined runtime tests.

Remaining scope is still substantial. Amendments still needs broader soft-fluid/
cauldron recipe adapters, lava layering, source presentation/compatibility edges,
pet teleport behavior and final resource/API closure. The earlier Supplementaries,
Jugtania, forbidden arcane, Mowzie and other family gaps remain open. Nothing in
this entry is a claim that the whole collection is finished or validated.

### Further local implementation — botanical progression

OpenAI Codex (GPT-6). Local implementation only; no compilation, tests, game
launch, commit, publication, profile changes or server actions. Generation of
runtime resources and static recovery of supplied geometry are not validation.

- Added a native Jugtania resource/recipe adapter with stable `jugtania_` IDs,
  original flower, wood/stone/quartz, workshop and material artwork. Only selected
  implemented families are registered; copying the remaining source assets does
  not mean their gameplay has been implemented. Corrected the supplied elven
  quartz identifier and translated the legacy dye/quartz ingredient tags.
- Added natural mystical flowers, floral fertilizer, bonemeal growth, water-filled
  petal apothecaries, seed completion, Pure Daisy transformation of eight adjacent
  blocks, Endoflame fuel consumption, physical ingredients and visible outputs.
  Source recipe quantities, mana costs and the Pure Daisy's 1,200-tick default
  transformation duration are retained. Native recipe/data compatibility still
  needs the deferred combined validation.
- Mana spreaders bind with the twig wand, reserve mana while a visible burst is
  travelling, check loaded paths and current permissions, and return undelivered
  mana to their source. Pools support infusion catalysts and saved mana-tablet
  transfers. Altars retain rune catalysts; the terra plate checks the original
  livingrock/lapis foundation. Inventories, owners, cooldowns and in-flight mana
  are persisted. Hopper input/output sides use shared permission checks.
- Added the five-by-five elven gate frame, three glimmering frame blocks, a
  three-second opening, two Natura pylons over powered pools within five blocks,
  200,000 mana opening cost and 500 mana per trade. Ingredients/results remain
  recoverable. It never rebuilds the player's frame or deletes rejected items.
  Dreamwood, elementium, pixie dust and elven quartz now have local trade paths.
- Recovered the original Mana and Natura pylon cuboids, UVs and pivots statically.
  Native placed/item renderers use the supplied textures, rotating rings and
  counter-rotating/bobbing crystals. Their enchanting strengths are eight/fifteen.
  The tablet has an explicit two-way ley-pylon bridge at 100 mana per ley unit;
  whole-unit transfers preserve remainders and require permission.
- Added Hydroangeas water cycles/decay, Thermalily lava/cooldowns, Gourmaryllis
  saved food-variety/digestion, owner-only Rosa Arcana XP consumption, Kekimurus
  cake slices, bounded Munchdew canopy harvesting and Spectrolus wool sequencing.
  These are native adaptations, not complete source parity: food/wool consumes
  one offering without deleting unrelated stacks; Spectrolus sheep consumption,
  Rosa Arcana loose-orb handling and original flower presentation edges remain.

Jugtania is still incomplete: remaining generating/functional flowers, equipment,
sparks/corporea, other machines/rituals, Gaia encounters, lexicon/encyclopedia
integration and additional source systems remain. Pure Daisy progress persistence, plant support and aimed spreader shells/animated
cores have now been implemented locally. Transaction edge cases, client reloads and
all resource/API closure still need implementation review before final validation.
The previously recorded household, forbidden arcane, Mowzie and other gaps remain.


### Continued local implementation — functional botanical workshops

OpenAI Codex (GPT-6); implementation only, still uncompiled and untested.

- Added Agricarnation crop acceleration, Clayconia sand processing, Fallen Kanade
  ally regeneration, Jaded Amaranthus flower planting, both Orechids and both
  Marimorphoses. Original costs, ingredients, weighted ores and biome bonuses
  drive real world changes. Scans are bounded and check current permissions.
- Added Hopperhock and its miniature variant with transactional item collection,
  item-frame allow/deny/ignore modes, neighboring sided inventories and shared
  claim-boundary checks. Fresh drops and pickup-delayed items are left alone.
- Added Bellethorn/miniature and Dreadthorn attacks, Hyacidus poison, Medumone
  slowing, Tangleberrie containment and Jiyuulia repulsion (including miniatures).
  Automatic effects exclude players and tamed pets and check owner authority.
- Added Daffomill directional item wind: placement chooses direction, sneak-empty
  interaction rotates it, redstone suspends it, and paid wind time is saved.
- Added Tigerseye pacification with a persisted entity tag and native player-avoid
  AI. Heisei Dream buys bounded thirty-second monster rivalries with current
  authority checks; it does not permanently rewrite mobs' combat goal lists.
  These AI adaptations and their interactions with other goals need final tests.
- Added vertical neighbor-update propagation through bubble-column grates.
- Pylon rendering now submits separate crystal/ring parts rather than mutating
  visibility on a shared queued root. Original geometry/textures are retained.

This is additional functioning implementation, not a claim of collection closure.
Remaining botanical equipment, machines, flowers, rituals, encounters and the
other recorded source-family gaps are still open. No owner profile or live files
have changed.


### Continued local implementation — remaining flower systems

OpenAI Codex (GPT-6). All additions below remain uncompiled and untested; runtime
resource generation and reading supplied/native bytecode are not validation.

- Narslimmus consumes tagged natural-spawn slimes; eggs/spawners/split offspring
  do not receive that provenance. Shulk Me Not requires a shulker and its nearby
  levitating enemy target. Rafflowsia retains a saved variety/streak history and
  source output curve, and will not eat workshops containing mana or offerings.
- Dandelifeon now runs a redstone-controlled 25×25 cellular simulation with real
  cell blocks, saved generations, bounded area checks, neighboring-flower leases,
  capture rewards and rollback on failed placement. Source boundary punishment
  and exact multi-flower/source-timing parity still require closure review.
- Entropinnyum intercepts dry near-expiry TNT before world damage, with a saved
  first-tick check for duplicated/moving TNT. Its anti-duplication heuristic needs
  adversarial validation; it is not a proven comprehensive TNT-duper detector.
- Rosa Arcana also accepts merged XP orbs and disenchants dropped equipment while
  retaining curses. Pending mana is saved instead of discarding overflow. Spectrolus
  now also accepts correctly colored unshorn sheep; unrelated colors remain alive.
- Exoflame heats working vanilla furnaces/smokers/blast furnaces with paid fuel
  time and cooking acceleration. Pollidisiac consumes actual animal food for
  adult breeding or young-animal growth, with saved feeding mode.
- Rannuncarpus/miniature use native block placement, an underlying sample, optional
  exact-state matching and frame filters. Spectranthemum binds a separate loaded
  item destination and pays distance/stack costs. Labellia consumes named tags to
  rename permitted nearby creatures and items. No research unlock is required.
- Solegnolia/miniature now suppress the implemented enchantment, backpack,
  alchemical band/pedestal/storage and pocket-pet item magnets. Bergamute attenuates
  positional sound and intercepts nearby vibration paths. Vinculotus redirects
  enderman teleports to checked ground and refunds mana when teleportation fails.
- Bubbell/miniature create reversible air cells from plain water in bounded scan
  slices, retaining each original water state. Cells restore after power loss,
  source removal or unloading; they never overwrite player replacement blocks.
- Loonium now resolves supplied inherited structure configurations, selects the
  matching loot/equipment tables, applies mob attributes/effects and carries one
  saved reward that replaces ordinary death loot. Legacy trim/dye/potion NBT is
  converted to native item components. Encounter resource/API and gameplay behavior
  still require combined validation, including spawn caps and death/reload paths.

Generated botanical descriptor: 365 blocks and 59 nonblock items. These include
an internal non-item air-cell block. This is a registration subtotal, not a
completion or test percentage. The collection remains incomplete: botanical gear,
sparks/corporea, machines/rituals, Gaia encounters, floating-flower/source behavior
edges, guide integration, and the other recorded source-family gaps remain open.


### Continued local implementation — botanical tools, floating islands and guidance

OpenAI Codex (GPT-6). Implementation continues; no compilation, tests, Minecraft,
profile edits, commits, publishing or live-server operations were performed.

- Added native Manasteel mining/farming/combat tools and shears, Soulscribe,
  Livingwood Bow and Crystal Bow. Carried mana tablets pay durability costs and
  repair existing damage; no equipment research is required. The crystal bow
  materializes ordinary arrows, consumes special ammunition and draws faster.
- Added Elementium pickaxe rubble filtering, bounded vertical gravity-block
  shovel harvesting, farmland-moistening hoe and held-use area shearing. Mining
  uses native player break handling and rechecks claims; area shearing requires
  visibility and permission. Elementium axe/sword and remaining advanced gear
  are still open, rather than registered as inert variants.
- Converted the supplied floating-flower custom loader into native composite
  island/flower models, preserving original cuboids, UVs, ten island materials
  and the source half-size flower transform. Placed islands bob/turn, support
  normal generating/functional flower behavior and need no soil. Added all
  sixteen shiny flowers and their source recipes, nine pasture seeds, six soil
  variants, and authority-checked bounded spreading. Snowballs/seeds customize
  floating island appearance. Growth jobs end on unload/restart; they are short
  cosmetic landscaping operations, not persisted resource-producing jobs.
- Added Manastar's three-second adjacent-pool trend indication and wand readout.
- Added Botanical Workshops and Working Flowers to the shared encyclopedia,
  including native recipe data and recipes involving the held item. This is not
  yet a complete JEI category for every botanical recipe.
- Fixed pylon geometry cache invalidation on resource reload, including when the
  ResourceManager instance itself does not change.

Latest generated subtotal: 456 botanical blocks and 81 nonblock items. Generation
is implementation output, not validation or a completion percentage. The entire
collection remains incomplete. Remaining botanical equipment, sparks/corporea,
machines/rituals, Gaia encounter, original behavior edges, remaining Supplementaries
and Amendments edges, forbidden arcane and jugziesmobs systems still need work.
Final combined validation remains deferred until the implementation scope closes.


Further local continuation: mana distributors now share incoming bursts among
permitted adjacent pools while retaining unaccepted mana; detectors pass bursts
and generate redstone pulses, and explicit void blocks destroy mana/allow pool
overflow. Added the source Black/Blacker Lotus pool charges, Overgrowth Seed and
Enchanted Soil flower acceleration (excluding floating flowers and Hydroangeas),
and source weighted treasure injection into four complete supported chest tables.
The supplied Lexicon item opens the unified botanical encyclopedia chapters.
Native loot schema conversion now runs on botanical loot, including the earlier
block and encounter resources. The current generation subtotal is 460 blocks and
85 nonblock items. No compilation or tests have run; implementation is ongoing.


### Continued local implementation — botanical mechanisms and brews

OpenAI Codex (GPT-6), local only. Added functional hourglass timers and burst
counters, saved contents locks, four-tick pulses, bellows furnace acceleration
and pool/tablet charging acceleration, and turntables that sweep actual spreader
receiver targeting. Redstone spreaders now queue a rising-edge shot instead of
firing continuously while powered. Physical dropped tablets exchange mana with
pools without being collected as infusion ingredients. Claim/loaded-chunk checks
remain in the routing and machine paths. Hourglass pulses have a scheduled reset
so an operator going offline cannot leave the redstone signal stuck on.

Recovered the supplied Bellows, Hourglass and Botanical Brewery model cuboids,
UVs and authored movement transforms from static references; added native held
renderers and runtime animation. Original sound files and subtitles are imported,
with bellows and brewery/incense completion/ignition hooked to their sounds.
This does not claim every imported sound has a gameplay caller.

Implemented the supplied brewing formulas and source costs, four-dose Vials,
six-dose Flasks at twice the mana cost, returned empty containers, formula
matching, physical ingredients and preserved ingredient remainders. Added the
special effect mechanics: accelerated fishing countdowns, Feather Feet fall
protection, Soul Cross kill healing, Emptiness natural hostile-spawn suppression,
and Bloodthirst's light-rule exception while retaining ordinary spawn/collision
rules and sanctuaries. Incense supports eligible single non-instant brews at ten
times the cost, sixty-times-duration area effects, owner/claim boundaries,
water extinguishing, persistent burn time and comparator states. Unified guide
chapters describe the controls and formulas.

Resource generation currently covers 465 botanical blocks and 90 nonblock
items; this is a subtotal, not collection completion. No compile, test or game
launch has run. Combined validation remains deferred by owner instruction.
Remaining botanical scope still includes advanced equipment, sparks/corporea,
lenses, other machines/rituals, Gaia encounters and source-parity/presentation
edges. Other collection families recorded above are still outstanding. Incense
held-content presentation and brewed creative/recipe-browser variants still
need closure; actual rendering and multiplayer behavior remain unverified.


### Continued local implementation — sparks, rituals, crafting and control devices

OpenAI Codex (GPT-6), local implementation only, no compile/test/game launch.
- Mana Sparks now transfer between permitted loaded pools and active Terra Plate /
  Mana Enchanter recipes. Source dye channels, four augments, reversible installs
  and Dispersive tablet charging are implemented. Searches cap at 128 neighbors;
  this is not Corporea, and advanced mana-item charging is still open.
- Added Horns of the Wild/Canopy/Covering and three mana-triggered drums, with
  bounded clearing, animal shearing, chicken egg timing, milk/stew conversion,
  brown-mooshroom effect preservation and Bergamute/claim protections.
- Added the source six-pylon Mana Enchanter formation, compatible book-enchantment
  copying, source rarity/level/count costs, retained books, saved in-progress
  state and structure-loss cleanup. Pylon beam presentation is still open.
- Open Crates deliver below; Crafty Crates perform real 3x3 recipes, preserve
  remainders, support reusable placeholders and nine pattern masks, and require
  a wand-confirmed choice for ambiguous recipes. Failed output insertion keeps
  inputs. Open Crate's redstone pickup delay currently also delays player pickup
  (the original delays only flower collection); this parity detail remains open.
- Animated Torch has three burst-driven routing modes, manual rotation, a short
  disconnected transition and original redstone-torch animation. Forest Eye and
  Ender Overseer provide animal-count / line-of-sight redstone sensing. Teru Teru
  Bozu uses the recovered original model and weather faces, bobbing/swaying,
  Halloween skin and supplied flower-controlled weather behavior. Life Imbuer
  pays mana to satisfy a spawner's proximity check without replacing its spawn
  limits. Spark Tinkerers exchange physical augments on redstone rising edges.
- Cocoons implement two-minute hatching, emerald/chorus probabilities, rare
  spirit feed, aquatic/source entity tags, baby animals, loaded/claim checks and
  saved blocked-hatch choice rather than rerolling every retry. Original cocoon
  mesh/UVs and breathing motion are retained.

Current generated botanical subtotal: 478 blocks, 108 nonblock items, six recovered
model rigs. These are implementation counts, not completion or validation. Added
unified encyclopedia instructions. Incense held presentation and brew creative
variants are now implemented; complete JEI categories and runtime acceptance
remain outstanding. The wider scope still includes advanced botanical gear,
Corporea, lenses/rods, remaining machinery/rituals, Gaia, source behavior edges,
Supplementaries/Amendments gaps, forbidden arcane and jugziesmobs. No claim that
all NEWEST ADDITION content is done. Combined checks remain deferred.


### Continued local implementation — portable mana, advanced tools and armor

OpenAI Codex (GPT-6). No compilation, tests or game launch; no publication.
- Added real mana rings, greater rings, Aura rings and loaded/permission-checked
  pool mirrors. Dispersive Sparks and pool charging share portable/equipped mana
  accounting; upgrades preserve stored mana. Terra Shatterer charging is separate
  from spendable portable mana.
- Added bounded Terra Shatterer area mining, connected Terra Truncator felling,
  Temperance controls, Vitreous Pick glass harvesting, Elementium axe head drops,
  Terra/Star blade projectiles and Thundercaller chains. Native mining/claims and
  loaded-chunk limits remain authoritative. Empty-air left-swing firing and some
  source head-drop cases still need closure.
- Implemented four armor sets with original statistics, mana repair/discounts,
  Terrasteel passive healing/charging, Manaweave snow walking, Phantom Ink and
  Ancient Will attachment/critical-hit effects. Eight original model layers were
  recovered from static source references with original UVs and textures; native
  humanoid animation poses them. Added original-model animated Elementium pixies,
  held-sword bonuses and full-set retaliatory effects with claim protections.
- Corrected queued mining cancellation and cocoon habitat persistence; hatching
  waits for the original aquatic habitat instead of turning aquatic rolls into
  land spawns. Finalized spawn dimensions are checked before spawning.

Generated botanical subtotal is 478 blocks / 146 nonblock items, seven mechanism
and creature rigs plus eight armor layers. These are implementation counts only.
Entire collection remains incomplete: rods/lenses, Corporea, remaining botanical
rituals/machinery/Gaia and source edges, plus other families listed above. All new
API signatures, model rendering, recipes and multiplayer behavior await the
owner-requested combined validation at the end. No percentage/completion claim.

### Continued local implementation — rods, Corporea and accessories

OpenAI Codex (GPT-6); still no compilation, tests, game launch or publication.
- All thirteen supplied rod item paths now have native placement, water, flight,
  smelting, bounded terrain replacement, temporary bridges, ore highlighting,
  fire circles, homing missiles and gravity-grip behavior. Empty-air sword swings
  now fire their projectiles. Rod-avatar integration and thrown-item impact damage
  are still outstanding; their registration is not being counted as completion.
- Corporea now has physical colored Spark endpoints with exactly one master,
  bounded loaded networks, claim checks, atomic real-inventory transfers, paged
  Index retrieval, redstone/item-frame Funnels, counted/locked Crystal Cubes,
  Interceptor pulses and Retainer retries. Full output storage retains items and
  does not falsely signal a stock shortage. Both halves of locked/unopened loot
  chests are excluded. Original Index geometry/rotation, cube glass/counts and
  Spark sprites are bound; native inventories and multiplayer await validation.
- Twenty-six equipped botanical accessories now implement utility rings,
  defensive cloaks, movement sashes, pendants, extra jumps, dodging and finite
  Tiara flight. Brewing can infuse Blood Pendants. Existing flight leasing now
  honors external paid providers between pulses instead of revoking them every
  tick. Original cloak geometry, belt textures, pendant icons and eight selectable
  wing styles are wired to worn rendering. Original no-wings Tiara style retained.
- Goddess protection pays once per explosion; entity damage remains. Diva's
  lethal interception occurs before drops/XP and cannot repeatedly revive the
  same enemy or revive high-health bosses. All retaliatory effects check claims.
- Fixed replacement-rod placement context, removed invalid empty native armor
  equipment maps, and kept the owner's other equipment and source files intact.

Generated botanical subtotal: 491 blocks / 188 nonblock items, nine recovered
mechanism/creature/cloak rigs and eight armor layers. Counts are implementation
inventory only. Source parity edges still include Tiara halo/special wing tint and
relic infinite flight, pendant temperature details, flower/machine issues noted
above, and remaining lenses, accessories (Spectator/Monocle/cosmetics/Tiny Planet),
rituals/machinery/Gaia. Supplementaries/Amendments, forbidden arcane, jugziesmobs
and the previously recorded other-family gaps still need implementation. Do not
publish or represent the collection as complete or tested.

### Continued local implementation — inspection, cosmetics, Avatars and Luminizers

OpenAI Codex (GPT-6). No compilation, tests, game launch or publication.
- Spectator searches permitted loaded inventories and public item/trade entities
  in bounded slices. Locked/unopened loot and other players' private inventories
  are excluded. Markers are private and separate from ore-divining markers.
- Monocle works alone or attached to an accessory; it shows redstone/repeater/
  comparator readouts, public frame-container contents and spreader aim markers.
- All 33 supplied cosmetic variants have original item models and recovered
  source wear transforms, including paired parts. Crafting attaches, replaces or
  removes cosmetics while returning the old cosmetic and preserving host data.
  Phantom Ink also hides functional botanical accessories.
- Avatars accept the six source-supported rods: dirt placement, flight boosting,
  ore divination, flame circles, homing missiles and short-lived walking bridges.
  They consume real mana, pause under redstone, check claims and retain boost
  cooldowns. Original six-part Avatar geometry, rod placement and pulsing overlays
  are bound; this adds the tenth recovered botanical rig.
- Four Luminizers now bind within 20 blocks, carry players/items on native ride
  entities, route via animated torches, pause from redstone, pulse detectors and
  relay ender pearls to bounded endpoints. Paths do not force-load chunks or pass
  through protected/obstructed space; loops stop. Original rotating sprites and
  optional hidden path particles are used.

Generated botanical subtotal: 496 blocks / 223 nonblock items. These are only
implementation inventory counts. All above still needs combined API, data,
client/server and gameplay validation. Spectator's bounded search differs from
instant source scanning and intentionally does not inspect private players.
Monocle's public-container preview does not yet expand every custom filter type.
Cosmetic orange-shades alpha currently uses ordinary item material (RGB tint is
preserved); accessory tooltip explanation is still pending. Luminizer route
radius hints and original spiral particles still need visual closure. Remaining
lenses/Tiny Planet, Red String, other botanical devices/rituals/Gaia, prior source
behavior edges and other collection families remain open. No completion claim.


### Continued local implementation — Red String, platforms, lenses and portable tools

OpenAI Codex (GPT-6). Local-only; no compilation, automated tests or game launch.
- Six Red String endpoints now implement bounded remote storage/dispenser access,
  comparator/interceptor signals, paid fertilizer and redirected flower effects.
  Effects use a separate origin; real block-entity identity and mana links stay
  unchanged. Live permissions, transaction paths and unloaded targets are checked.
- Three platforms have source collision rules, copied-block camouflage, Phantom
  Ink hiding and bounded connected wand copying/clearing. Tiny Potato has six
  item displays, naming, hopping and the source birthday interaction; 55 named
  model variants plus default are bound to supplied meshes. Cacophonium records
  supported mob sounds and plays them by item or redstone-driven block.
- Spreaders now launch real saved mana projectiles. The 25 lens paths include
  composites/dyes, velocity/range/loss changes, gravity, bouncing, damage, phasing,
  homing, attraction, aiming, detectors, bore/weight/paint/fire/light/explosive
  effects and Force Relay piston/warp behavior. Tiny Planet block and accessory
  bend passing bursts. World-changing effects check loaded chunks and claims.
  Missing burst entities do not refund mana and create duplication.
- Mana Blaster consumes carried mana and supports lens installation/removal,
  six-slot clip crafting/cycling, original meshes/audio, recoil, Haste cooldowns
  and the recoil belt. No durability/ammunition bar. Composite lenses retain the
  secondary lens's actual item components when split.
- Slime Bottle detects native slime chunks; Open Bucket discards picked-up fluid;
  Obedience Stick binds nearby compatible flowers or clears one flower's link;
  World Seed returns to a bounded safe arrival near world spawn; Ender Hand opens
  the player's own Ender Chest for mana. Spellbinding Cloth removes enchantments
  and repair cost with 35 conserved uses. Dreamwood Wand shares working wand use.
- Flower Pouch has 32 color-specific storage slots and eligible contact pickup;
  Bauble Box has 24 accessory slots. Both use saved components, exclude nested
  storage, lock the open carrier against menu swaps and preserve item contents.
- Black Hole Talisman stores one ordinary block type with a bounded conserved
  count, inventory collection, cursor transfers, withdrawal and native placement.
  Custom item components are not flattened into a count; the exchange rod can
  consume its stored blocks. Life Aggregator captures the complete native spawner
  state and consumes its filled tool only after a successful placement.

Generated subtotal now 510 botanical blocks / 266 nonblock items. This is not a
runtime result, source-parity claim or completion percentage. Source edges still
include platform biome tint, some potato named attachment/pose details, original
Luminizer particles, full lens painted-block compatibility and Bore/Warp drop
routing. Firework lens currently uses harmless firework particles/audio instead
of a damaging rocket; Force Relay pushes instantly rather than animating a moving
piston. The later continuation below replaces the bag layouts and adds pouch dumping.
Box inventory shortcuts remain. Ender Hand deliberately does not open other
players' private chests; its Astrolabe supply adapter is now implemented. Remaining botanical relics/Gaia, devices and prior flower
edges, plus the other collection families above, are still open. Preserve all
prior dirty work; continue implementation before the combined test pass.


### Continued local implementation — supplies, crafting and remaining portable tools

OpenAI Codex (GPT-6). All changes remain local. No compiler, test runner or game
has been run during this continuation; the complete collection is not finished.
- Flower Pouch and Bauble Box now use their supplied GUI backgrounds and source
  slot positions; the box includes a player preview. Pouch sneak-use can deposit
  permitted contents into supported inventories, without crossing chest claims.
- Ender Air can be bottled in the End and thrown for bounded stone conversion or
  its supplied Overworld ghast interaction. Six-swig Mana Bottles, Mana Cookies,
  both original music discs and their sounds are implemented. Bottle explosions
  affect the drinker only; inventory scatter deliberately preserves the in-use
  bottle. Friendly bottle Pixies have bounded follow/orbit behavior.
- Two horse viruses convert an owned, tamed, unoccupied horse to the source undead
  variants with increased attributes, retained ownership/saddle and returned body
  armor. This is still pending native conversion/API and visual validation.
- Astrolabe selects a block and places 3x3 through 11x11 areas using native
  placement, mana, claim checks and real materials. It uses inventory blocks,
  Black Hole reserves, then the player's own Ender Chest while carrying an Ender
  Hand. Multi-block blocks and block-entity blocks are intentionally excluded.
- Worldshaper's Sextant saves circle/sphere guides up to radius 256. Client-only
  overlays show nearby missing shell blocks with a bounded render budget. No
  blocks are generated or modified by the guide.
- Resolute Ivy binds one unstackable item for one ordinary death. Actual death
  drops are intercepted before graves, then restored from a persistent attachment
  after respawn; overflow stays retained. Existing keep-inventory/boss protection
  leaves the binding intact. Vanilla vanishing still precedes this interception.
- Vine Balls and Livingwood Slingshot place up to nine climbable, non-growing
  solid vines using supplied block/item resources. Thorn/Flare Chakrams bounce,
  return and preserve custody of their actual thrown item, including components.
  They apply permission-checked damage/poison/fire without block destruction.
- Assembly and Manufactory Halos have native portable crafting, eleven recipe
  bookmarks, saved output selection and preserved recipe remainders. Automatic
  crafting is opt-in and checks one bookmark per second, round-robin, while the
  ordinary inventory is open. Bookmarks currently require the exact saved item
  components, not interchangeable members of ingredient tags. Recipes and their
  outputs are recomputed on the server; no saved output grants are trusted.
- Pinkinator converts an eligible visible Wither into the supplied friendly Pink
  Wither with original skin and native three-head geometry. It can be ridden,
  heals gradually and has no skull attacks, terrain destruction, star drops or
  boss bar. Its wandering is bounded by loaded space and the owner's permissions.

Implementation still needs API/resource/runtime closure. None of the above is a
passed gameplay result. Remaining collection work includes botanical Gaia/relics,
Laputa and remaining devices/source parity edges, Supplementaries/Amendments,
forbidden arcane and jugziesmobs details recorded earlier. No completion claim.


### Continued local implementation — Gaia, relics and botanical devices

OpenAI Codex (GPT-6). This is implementation progress only: no compilation,
tests or game launch have run. The entire NEWEST ADDITION collection is still
unfinished; combined validation remains deferred under the owner's instruction.

- Gaia ritual now uses a beacon, four original Gaia pylons, an arena-clearance
  check, participant-bound phases, teleports, delayed mines, original-model
  pixies and bounded summoned waves. Normal/hard loot is adapted from supplied
  tables, and hard rewards bind relic dice per participant. Ritual adds have no
  ordinary loot/XP; damage and potion effects cannot affect nonparticipants.
  The body mirrors the viewer's skin. Original distortion shaders and hard-mode
  missile details are still outstanding; no runtime fight result is claimed.
- Six bound relic outcomes now have native behavior: Infinite Fruit, King's Key,
  Eye of the Flugel and Thor/Odin/Loki rings. King's Key uses the twelve supplied
  weapon meshes; Loki repeats permitted placements/mining with real materials,
  native tool wear/drops and bounded offsets. The Loki selection overlay remains.
- Laputa shards have persistent, incremental island lifting and tier upgrades.
  Terrain moves 42 blocks upward without force-loading chunks; permissions are
  checked at both ends. Occupied destinations, inventories, fluid/multipart and
  other unsafe block forms are preserved. This conservative movement excludes
  those forms instead of reproducing the source's block-entity/flying-block lift.
- Mana Pool Minecart uses native rails, saved mana and original pool/item models.
  Pump loads/unloads across rail → pump → pool, with reversible direction,
  redstone pause, comparator feedback and the original moving pump head.
  Dispenser placement still needs an adapter.
- Fluxfield converts accepted mana at the supplied Fabric ratio (3 JE/mana),
  exposes transactional energy extraction, retains its buffer and pushes power
  to permitted neighbors. There is no energy-to-mana conversion path.
- Prism installs/swaps actual lenses and changes crossing bursts once per prism,
  with saved traversal bounds. Fabulous/Creative pools and remaining supplied
  decorative stone/wood/glass/potted variants are connected to native behavior.
  Fabulous world tint is currently chunk-baked, not continuously animated.
- Petals can now be planted as invisible, colored-sparkle buried petals and
  bonemealed into their actual matching tall flowers. Original root block is
  included. Fel Pumpkin atop two iron bars summons a persistent Fel Blaze;
  ritual blocks are consumed only after spawn acceptance. It drops powder rather
  than rods (6 ordinary/10 player kill); the original looting bonus remains.
- Luminizer Launcher has a rising-edge trigger, waterlogging, original mesh and
  bounded passenger/item admission to neighboring linked relays. Player riders
  authorize their own route; foreign tamed pets are excluded.
- Water Bowls contain one actual bucket, return the bowl on transactional drain
  or crafting, fill apothecaries and support permission-checked water pickup.
- Manastorm Charge now starts a persistent finite 250-burst sequence followed by
  a permission-checked finale; nearby concurrent storms are capped. The existing
  storm-lens creative-only terrain-damage restriction is retained pending review.
- Gaia trophies support floor/wall placement and viewer-dependent native skull
  rendering. Starfield uses its supplied block and a native portal-surface effect
  plus nighttime stars; exact source shader/particle parity is not claimed.

Resources were last generated before the newly added oddities/trophies; rerun
`tools/newest_jugtania.py` after the remaining edits. Ghost rail, the device/source
edges above, other recorded botanical gaps and Supplementaries/Amendments,
forbidden arcane, jugziesmobs and client-family gaps still require implementation.
Nothing was committed, pushed, published, installed in a player profile or deployed.

### Continued local implementation — arcane forge and botanical follow-through

Implemented with OpenAI Codex (GPT-6). Still local and unvalidated; no compile,
test suite or game launch has been run during this implementation pass.

- Botanical resource generation now includes 678 block definitions and 294 items.
  Counts describe generated coverage, not verified gameplay completeness. Added
  ghost-rail ownership/limited collision-free cart travel, bag inventory shortcuts,
  original open-box item display, spinning chakrams, Loki selection outlines,
  source Ender Dagger support, cart dispenser ownership and crafting statistics.
  Ghost-rail collision/rider transitions and existing source-behavior limitations
  still need the final runtime pass.
- Forbidden-arcane now has native material/building/gear implementations and
  original models/textures, including doors, trapdoors, hollow-workshop shapes,
  farmland, soulless sand, tools and the three supplied armor sets. Mortem armor
  retains the source's absence of durability. Ordinary ingredients remain ordinary
  ingredients; custom devices are not counted as implemented from asset copies.
- Added source-layout Hephaestus Forge creation using Mundabitur Dust on a smithing
  table above the 9-by-9 patterned darkstone base. Eight physical pedestal positions,
  bounded ingredient matching, tier requirements, four essence stores, timed rituals,
  component-preserving armor transmutation and forge upgrades are implemented.
  Ingredients are escrowed before crafting; breaking a running forge returns those
  ingredients rather than paying the pending result twice. Stored essence is spent
  at ritual start. Claim/transfer checks run before intake and during crafting.
- Added the supplied forge GUI texture/slot layout, synchronized essence bars,
  inventory controls and visible rotating pedestal offerings. Enhancer tier slots
  and source essence discounts/time conditions are implemented. Clibano-specific
  enhancer effects are not yet implemented.
- Added 35-tick Soul Extractor use, conversion to soulless sand, recoverable souls,
  91-XP orbs, Aureal bottles and persistent player Aureal, and offhand blood tubes
  filled by actual permitted melee damage, capped at the source's 3,000 units.
  The Aureal recipe retains its strong-regeneration potion requirement through a
  native component-sensitive crafting recipe. Ore placements are adapted to the
  pinned worldgen schema for future chunks, without regenerating existing terrain.

This arcane work is not complete. Remaining scope includes nature/tree behavior,
Clibano, jars, obelisks, skulls, quantum capture, custom arrows/wands, modifier
effects, desks and related source systems. Jugziesmobs and the previously recorded
Supplementaries/Amendments, botanical and client-family gaps also remain. Final
validation must check native API compatibility, resource closure, survival sources,
multiplayer permissions, forge interruption/save behavior and original visuals.

### Further local arcane implementation (OpenAI Codex, GPT-6)

Still implementation-only: no compilation, tests, game launch, commit, push,
profile installation or live-server action in this continuation.

- Added six equipment modifiers (Eternal, Aquatic, Demolishing, Fiery,
  Magnetized and Soulbound), native smithing recipes, applicable-item checks,
  claim-aware mining/pickup and grave-aware permanent soulbinding. Modifier
  tooltip text is wired. Exact native API compatibility awaits combined checks.
- Added source treasure-table injections, gavel ore bonuses, magical farmland
  yields, Stella mining explosions with permissions, Aurum/nugget-leaf trees and
  yellow orchids. These affect future generation only.
- Added hollow Edelwood logs, oil harvesting, carving, leaf/shear decoration,
  rainwater collection, branch placement and original block models. Added
  Corrupt Soul sapling conversion, growing Edelwood, ordinary/2x2 Fungyss growth,
  and bounded Arcane Bone Meal jobs. Supplied custom tree feature classes were
  absent; Edelwood/Fungyss footprints are new native implementations, not claimed
  exact source tree silhouettes. Added sparse underground Fungyss acquisition
  because no natural placement hook was supplied.
- Added single-essence 10,000-unit Utrem jars, component-preserving drops and
  pickup, partial draining into forge fuel slots, original glass models and
  animated liquid textures. Recovered original soul-jar geometry and complete
  keyframes into native client rendering. Inventory soul animation remains to
  be added; liquid inventory levels are wired.
- Added both three-block obelisks and the source dust formation pattern. The
  gilded base supplies one Aureal per 100 ticks to the closest eligible forge,
  with bounded loaded-position scanning and claim-transfer permission checks.
- Added Quantum/Boss Catchers and sixteen colors, persisted creature identity,
  Aureal cost, taming ownership and clear-space release. Town/managed encounter
  lifecycles cannot be bypassed through capture; those entities remain excluded.
- Added original bat/tentacle foods, effects, soup bowl return and their source
  loot/recipe routes. Added original sound files/events and charged magic-wand /
  Draco-scepter projectile implementations with original textures, authority
  checks and bounded lifetime. Scepter lightning is visual with directed target
  damage, preventing unrelated fire/area damage across claims.

Generated arcane coverage at this checkpoint: 118 blocks / 101 nonblock items.
This is not a collection completion or runtime-validation claim. Continue
Clibano, skulls, advanced jars/item views, desks, quantum devices, capacity
buckets, arrows, lost souls/aureal conversions, special blocks and the other
previously recorded collection families. Source-disabled preview items (e.g.
desk/book and Aureal tank) must be distinguished from finished source systems.

### Continued local device implementation (OpenAI Codex, GPT-6)

Still implementation-only. No compilation, tests, game launch, publication,
profile edits or server actions have been performed in this continuation.

- Clibano: 3×3×3 dust formation, source frame states/models, shared inventory
  through all parts, two independent smelting lanes and paired combustion
  recipes, fuel remainder handling, ordinary/soul/enchanted fire, enhancer soul
  duration, original GUI, persisted progress/experience and source residue
  recipes with automatic output recovery. Formation preflights all positions;
  dismantling restores surviving original blocks and drops inventory once.
  Orphaned frames restore when their central chunk becomes available. Combined
  checks must cover chunk borders, partial dismantling, hoppers, residue output
  congestion, recipe reloads and restart continuity.
- Six obsidian skull stages have source geometry/textures and native item/block
  renderers, fire protection, timed degradation and Aurealic consumption.
- Aurum/Edelwood boats and chest boats use native rowing/passenger/cargo behavior
  with the original textures. Carried soul jars now render the recovered soul
  geometry/keyframes, alongside the existing world renderer.
- Edelwood buckets implement water/milk capacity 4 and lava/powder-snow capacity
  3, same-fluid collection, placement, milk clearing, and whole-bucket Fabric
  tank transactions. The source's random lava-bucket destruction/spill and
  stack-dependent crafting remainder integration remain outstanding; do not
  describe these as exact full bucket parity yet.
- Quantum injector: toggled original model with recovered transform/rotation
  keyframes and emissive layer, loaded-only bounded discovery, nearby jars and
  forge, 5-unit transfer every 5 ticks, receiver capacity/claim checks. Source
  ordinary quantum core and mortar contain no machine logic; their original
  decorative behavior and mortar shape are retained. The arcane dragon egg
  retains source shape and falling behavior.
- Spectral Eye amulet toggles viewer-local entity outlines, with the supplied
  exclusion tag and no research requirement.

Generated arcane checkpoint: 139 blocks / 111 nonblock items, not a collection
completion percentage. Remaining arcane work includes original lost-soul
creatures and conversions, specialized arrows and splash Aureal bottles,
whirlwind/upwind, dark-matter black holes, source-disabled desk/book/tank status,
and the bucket gaps above. The broader botanical, creature, supplement and
compatibility gaps recorded earlier are still outstanding. Preserve that scope;
do not start the combined test phase or publish this checkpoint.


### Arcane completion pass and owner creature work — October 31 (OpenAI Codex, GPT-6)

Implementation continues locally. No compilation, automated tests, game launch,
Git publication, launcher/profile changes or server actions in this continuation.
The entire collection is **not complete**. These are implementation checkpoints,
not evidence of runtime correctness or a completion percentage.

- Arcane Lost Souls: original recovered model/idle/fear animation, natural normal
  souls, corrupt/enchanted conversions, source soul drops and catcher support.
- Boom and Draco arrows: original projectile textures, bounded blast and growing
  dragon-breath cloud with separate cloud lifecycle and per-target PvP/claim checks.
- Splash Aureal bottles: Aureal replenishment, soul conversions and the supplied
  persistent creature mark. The mark does not invent extra source gameplay.
- Whirlwind Prism: preflighted ten-block lift column, short lifetime, orphan cleanup,
  cooldown and claim/PvP-aware lift. Native cloud particles currently stand in for
  the source custom particle presentation; original particle parity is still open.
- Dark matter + corrupt dust: owner-bound dropped-item black hole, bounded item/
  projectile/XP attraction, persisted stored XP and original rotating cube/aura.
  Orb conversion costs 91 XP rather than source 60 to avoid the source 60-to-91 XP
  duplication loop. XP-removal lifecycle and rollback edge cases await final checks.
- Capacity buckets now include unit-preserving default crafting remainders and
  source random lava spill/destruction, with placement preflight. Recipe overrides
  of default remainder behavior and fluid spread/claim boundaries need final checks.
- Source desk/book/Aureal-tank classes explicitly disable their previews; they are
  not complete source machines. Dark Trader emergence/model exists but native
  integration is outstanding; no supplied trade table or merchant logic was found.
- Arcane generator now covers 143 blocks / 116 nonblock items. This measures
  bindings, not validated collection completion. Wand/scepter and lift PvP checks
  now use the shared TwoHanded.allowed permission path.

Owner `jugziesmobs` now has a native `wilds` module and source-preserving generator:

- Painted acacia/slab, thatch, clawed log and raked/red-raked sand. Rake rotates
  patterns and updates adjacent connections; falling sand restores its correct
  original sand color. Rake and gong receive explicit Jugcraft survival recipes
  because the supplied source omitted them.
- Gong: 3-wide × 3-tall occupied multiblock, one inventory item, original runtime
  mesh/UVs, 148-tick damped swing, manual/projectile/redstone strikes, shared root
  interaction and single-drop dismantling. Cross-chunk orphan/rotation edge cases
  still require completion/validation.
- Foliaath: three feeding milestones, growth to adulthood, original infant/
  juvenile/adult meshes, eating/bite/hurt/death keyframes, stationary emergence,
  owner-aware targeting and seeds. Local spawn balance is 20 weight / one per group
  rather than source 70 / 1–4; original intermittent mouth/flail transitions are
  not all ported. Planted plants and seedlings survive peaceful mode.
- Grottol: original stone/deepslate models/textures and six-legged gait, idle/death/
  burrow keyframes, fleeing and empty-minecart escape, pickaxe-only player damage,
  one diamond (two with Fortune), Silk Touch capture/release retaining variant,
  name and health. Source rail-pattern Blackpink trigger/music remains outstanding;
  its retained variant flag alone is not completion. Natural source spawn weight 2.
- Lantern: original mesh, separate transparent gel and solid stem, original scale
  controller/puff/death keyframes, ground-distance-driven puff flight, original
  sound effects and jelly drops. Jelly provides the original food and Glowing.
  Custom source particle glow is not yet exact; native particles are temporary.
- Blowgun/darts: five-tick charge, dart consumption, Infinity, Power/Flame, poison,
  source held/using models and projectile texture; spear adds 1.5 melee reach;
  Naga-fang dagger adds source poison/backstab and fang repair. Source Punch and
  dispenser handling still need binding. Fang survival source awaits the Naga.
- Wroughtnaut, Frostmaw and Naga original geometry and authored clips have been
  recovered as data, but native encounters/equipment/structures remain unfinished.
  Umvuthana/Umvuthi, Sculptor, Bluff, Elokosa and related original systems also
  remain. Do not count copied resources or recovered clips as finished creatures.

Continue all previously recorded botanical, supplement, compatibility and other
collection gaps as well. Combined tests remain deferred until implementation is
complete at the owner's request. Never publish this checkpoint as a full port.


### Original Naga, Wroughtnaut and Frostmaw continuation — October 31 (OpenAI Codex, GPT-6)

Local implementation only. No compilation, tests or gameplay launch, commits,
pushes, PRs, releases, launcher changes or live operations. Collection remains
unfinished; this section supersedes the earlier statement that all three had
only recovered geometry. It is not evidence that these additions work in-game.

- Naga: original 97-part geometry, authored attack/death clips, jaw/wing folding
  and membrane controls; native territorial flight, acid spit, swoop, projectile
  dodge and interrupted-attack fall/stun/recovery. Original poison-ball geometry
  and textures are bound. Original dynamic tail physics, custom particles, full
  spawn exclusions and one unbound source animation call remain open.
- Wroughtnaut: original 74-part mesh, nine clips, original resting/active pose,
  walking coefficients and emissive eyes. Native sleeping/activation behavior,
  swing combinations, embedded-axe back vulnerability, stomp, boss bar, original
  sounds, death delay, peaceful preservation and helmet/axe reward drops.
- Wrought helm uses original 64x32 shell, horns and tusks on the vanilla animated
  wearer. Helm and axe retain source unbreakability. The axe has timed sweep and
  crouching slam/ground wave, bounded victims and shared PvP/claim protection.
  Three source player gestures are sampled/retargeted onto Player Animation
  Library, with CastPoses ownership. Custom joint translations/held-item bones
  and the dedicated original first-person rig still need refinement; retargeting
  is not pixel-exact source-rig parity. Full enchantment effects on special blows,
  original ceiling disturbance and respawn-heal triggers remain open.
- Original Wrought chamber NBT includes its authored boss entity. Added native
  jigsaw placement at Y20–50 using source spacing 15/separation 5/salt; land-biome
  tag is a native adaptation. No existing generated chunk is rewritten.
- Frostmaw: native 250-health persistent sleeping guardian, crystal theft by
  invisible/creative players near the original crystal position, waking with or
  without the crystal, swipes, low-health slam, dodge, ice breath and predictive
  ice projectile. Retains source cooldowns and action lengths, heals while asleep,
  returns to sleep after losing targets, exposes boss bar and 94-tick death.
- Original Frostmaw 85-part mesh and twelve clips, sleeping/breathing pose,
  quadruped/biped gait, finger/roar/crystal controls, original texture/sounds,
  animated item sockets and three-part translucent ice ball are bound. The
  dynamic swing-controller corrections inside individual source attack methods
  are not all translated yet; source camera shakes/custom particle effects and
  original player ice-breath animation still remain.
- Ice crystal: 600-charge channelled ice breath, five-charge startup, one-charge
  per tick use and inventory recharge; source-default nonbreaking behavior.
  Shared accumulated freeze immobilizes and prevents attacks/item use for 50 ticks,
  respects Frostward/freeze immunity/fire, and checks per-target permissions.
  Water conversion uses bounded claim-safe native frosted ice for player casts;
  boss breath does not mutate claimed terrain. Source custom frozen shell and
  full head/camera locking are not yet ported. Native snowflake particles are
  temporary presentation, not original custom-particle parity.
- Original Frostmaw spawn template uses source spacing 25/separation 8 and village
  exclusion, with native snowy-land biome selection. Source height 50–100 and
  additional outpost exclusion are not yet enforced. Crystal is awarded only if
  still held; defeated crystal-less Frostmaw can drop the original Petiole disc
  while raining. Petiole has original audio and 140-second jukebox binding.
- Native dart/bow/axe/armor enchantability tags added; Punch/dispenser edges and
  other previously listed wilds gaps remain open. All generated counts are asset
  bindings, not validation or percentage completion.

Next unfinished source families still include Umvuthi/Umvuthana, Sculptor, Bluff,
Elokosa and their equipment, abilities and original structures, alongside the
previously recorded botanical/arcane/supplement/compatibility work. Preserve the
other dirty edits. Run combined validation only after the implementation pass.


### Bluff and Elokosa continuation — October 31 (OpenAI Codex, GPT-6)

Local implementation, not compiled or tested. No Git publication, launcher/profile,
client update or live-server changes. The entire collection is still unfinished.

- Bluff now has native 45-health/18-damage combat, original 11-tick windup,
  downward slam, 34-tick recovery, ranged strafing and a ground-navigation/flight
  fallback. Source spawn weight 10/group 2–3 applies to dark, covered Overworld
  locations. Original model, idle/hurt/attack/death clips, orbit equations, sound
  events and rod drop are bound. Rod is a material in the supplied source, not an
  omitted ability. Sculptor retaliation/exclusion awaits Sculptor implementation.
- Fissures follow loaded ground at 0.4 blocks/tick for at most 60 ticks; a hit or
  terminal obstruction raises a temporary stone spike. Source six-block spike
  sockets, emergence clip and sampled terrain material render through GeckoLib's
  block layer. Original growing crack sprites project onto terrain tops. They
  expire without modifying any world blocks. Player-cast permission handling is
  included for later geomancy tools. Original crack side-wall decals and source
  looping rumble/custom debris controllers still need work; this is not full
  presentation parity.
- Elokosa has native howler/follower entities, persisted pack links, source health
  and movement differences between timid day and hostile lunar forms, visible
  transformations, stalking/screaming, timed single/combo/scythe attacks, leaps,
  backflips and wall/ceiling attachment. Pack generation is bounded and does not
  repeat on save reload. Villagers remain protected by targeting players only.
- Original Elokosa model and clips, elite skin, glow masks and seven authored
  directional/day/night gait routines are translated. No stand-in mesh/texture.
  Source dynamic tail solver, transforming overlay, hand symbols, drool and exact
  ballistic destination search remain open. Natural spawn currently uses jungle
  weight 5 at Y60+, source surfaces, but source daytime/structure/nearby-entity
  exclusions are not yet all enforced. Ground animation timing and clinging
  transitions need the deferred visual pass. Do not claim source AI parity.
- All five moon-phase paw rewards use source three charges, shared 1,800-tick
  cooldown, 600-tick effects and 10-block radius; affected entities are bounded,
  require line of sight and respect PvP/claims and allied/tamed exclusions.
  Full/gibbous/half apply Weakness/Slowness/Mining Fatigue, crescent adds Fragility
  (+20% incoming damage per effect level), new moon applies Eclipse. Eclipse saves
  suppressed effects as a persistent attachment, blocks new effects and restores
  saved effects after ending/removal, including across unloads. It does not retain
  that attachment across death. Closed paw models follow cooldown.
- Source moon/pixel/snowflake/glow sprites now have native particle providers;
  ice breath/projectiles use supplied snowflakes and paws show supplied moons.
  Native particle motion is an adaptation, not every original component engine.

Remaining: Umvuthi/Umvuthana/Sculptor and their equipment/structures/abilities;
all explicitly recorded source gaps above and in earlier sections; remaining
botanical, arcane, supplements and compatibility systems. Combined validation
remains deferred until the implementation pass is finished, as instructed.


### Suntribe, Sculptor and monastery continuation — October 31 (OpenAI Codex, GPT-6)

Local implementation only. Nothing in this section has been compiled, tested or
played; combined validation remains deferred by owner instruction until the full
implementation pass finishes. The entire collection is NOT complete.

- Suntribe now has native Umvuthi/Umvuthana encounters, owned mask followers,
  healer/raptor behavior, persisted follower caps, source mask loot and the gold
  offering route to Sun's Blessing. Source rigs/clips and pose equations are used.
  Solar abilities validate server-side targets, claims and PvP; owned followers
  cannot damage across an offline owner's dimension. Client-only input interception
  no longer blocks the server's shared attack-permission checks.
- The original Umvuthana grove templates now have native saved placement, support
  markers, boss/resident population, masks and loot. Precise resident trade offers,
  some terrain decorations and source spawn exclusions still need implementation.
- Sculptor has a persisted 400-second climbing challenge, paid Bluff Rod entry only
  after space/course creation succeeds, success/failure recovery, and a single
  Earthrend Gauntlet reward. The current bounded two-path course is a native
  adaptation, not the original dynamic branching algorithm. Provoked combat,
  healing, original rig/clips, staff hand attachments and gauntlet assembly are
  connected; combat pillar ascent and detailed procedural pose physics remain.
- Gauntlet supports charged terrain boulders, punching them to launch, permission-
  checked temporary tunneling with occupancy-aware restoration, and sneak/jump
  pillars. Sculptor staff fires staggered stone projectiles. Geomancer equipment
  has source attribute/conditional abilities and uses the supplied art. Equipment
  never needs research. Boulder launch age and crumbling-platform regrowth now
  have separate, persisted timing; Sculptor descent is synchronized for animation.
- Monastery conversion includes all 94 supplied exterior/interior NBT templates,
  source pool metadata, native item-stack/equipment format conversion, gong state
  conversion and loot. Native saved layout assembly follows weighted rotations,
  source depth/height/ancestry/path-count conditions, two-way connectors, tagged
  room furnishings, interior space, special overlap bounds and generation order.
  Its test-zone template reserves room before subsequent pieces. Material variation
  uses the source weights; bounded deepslate supports descend into terrain.
  Remaining monastery work: guaranteed closure of path loops, exact terrain stair/
  base-decoration processors, roots and review of converted legacy display data.
  Generation is new-chunk-only; no existing world is rewritten.
- Source geomancy models, original staff/armor/gauntlet textures and clips, boulder
  tiers and repeating pillar geometry are wired. The original prayer-bead armor
  segment and all source spell/creature presentation details still need the final
  implementation pass; do not describe this as complete visual parity.
- Earlier unrecorded repairs retained: native dart dispenser/claim-boundary handling,
  Grottol rail/music route, corrected Black Pink sound identifier, and bounded
  arcane Stella explosion permission/PvP handling. These also remain unvalidated.

Still open: the explicit wilds encounter/visual/spawn gaps in this and previous
sections, remaining botanical/arcane/supplement/compatibility behaviors, and then
combined compilation, automated integration and visual checks. Asset counts and
registered objects are not completion or test evidence.


### Continued source integration — October 31 (OpenAI Codex, GPT-6)

Local implementation, **not compiled, tested or played**. Earlier open bullets are
historical checkpoints; the following supersedes the corresponding entries only.
The full collection is still in progress; no publication or live changes occurred.

- Grove residents now use the 21 supplied weighted trade offers, saved refresh
  state, original material substitutions and settlement-aware spawn exclusions.
  Grove supports/decorations and monastery base/deepslate/root processors are
  ported. The supplied stairs processor is not referenced by its own pools;
  guaranteed monastery path-loop closure still needs work.
- Monastery runtime templates now convert original banner patterns, legacy sign
  text and item-frame block positions into native data. Item frames retain the
  native uppercase Item field. Original sources remain intact. All 94 templates
  were regenerated as data, without starting the game or running tests.
- Frostmaw has native saved spawn-site placement with source surface-height and
  fluid restrictions. Source structure helper does not implement its advertised
  outpost avoidance; do not describe that flag as implemented behavior.
- Geomancer prayer beads use their proper armor segment. Robes use the source
  50% retaliation chance/search with a bounded nearby boulder budget. Bluff can
  retaliate against Sculptor. These remain unvalidated.
- Gaia hard mode now launches source-timed, finite-lived, locked-position magic
  missiles. Player-owned friendly missiles, ring attacks and pixies now route
  damage through shared attack permissions. Source distortion remains open.
- Crafting halos resolve tag/alternative ingredients from current inventory using
  bounded backtracking, verify the actual recipe/output, and preserve remainders.
  Special recipes retain exact recorded components. Nothing is paid until the
  complete result fits. Arcane Black Hole removal returns stored XP once through
  the native pre-removal callback; source pull motion is restored.
- Dark Trader now has the three original skins, source actor/portal models and
  all 12 recovered actor clips, with entrance/greeting/escape presentation. The
  provided entity has no trade offers or Merchant implementation and no natural
  placement. Jugcraft therefore does not invent or claim a finished merchant:
  currently it is a summonable NPC with an honest no-wares greeting.
- Ghost rails consider rider collision boxes, restore passengers along with the
  cart and search permitted loaded exit space if a checkpoint becomes obstructed.
  If no safe exit exists, the cart holds still and retries once per second without
  deleting blocks/cargo. Passengers are protected against in-wall damage only
  during the active spectral ride; this still needs multiplayer validation.
- Frozen entities now retain their captured facing, body/head angles and cannot
  jump or supply movement/turn input while frozen. Native living renderers receive
  the supplied ice overlay on the actual model. Remaining presentation coverage:
  custom/Gecko renderers and first-person frozen arms, plus original ice-breath
  player gestures. No visual result has yet been observed in-game.

Continue the remaining explicitly recorded creature behavior/presentation,
botanical, supplement and compatibility systems, then combined validation.


### Continued local source and presentation closure — October 31 (OpenAI Codex, GPT-6)

**Implementation only; still no compilation, automated tests or game launch.**
The owner's local-only and test-at-the-end instructions remain in force. These
entries supersede only the matching earlier gaps; the collection is not finished.

- Sculptor combat now raises an owned, persistent pillar toward the source height,
  carries riders, checks loaded clearance and lowers after combat. Ground targets
  remain eligible while the Sculptor is elevated. The original dynamically
  branching challenge course and detailed pose physics remain open.
- Gong parts reconcile loaded controllers/partners, clean orphan parts without
  duplicate drops and implement facing/mirror state transforms. Unloaded partners
  are preserved. Fabulous mana pools now use live tint rather than a chunk-baked
  rainbow. Flutes recall trusted foxes and make nearby parrots dance while played.
- Wroughtnaut axe hits now apply native enchantment damage/post-hit effects and
  cancel ongoing swings on freezing. Embedded strikes create the source ceiling
  dust/audio without destroying terrain; nearby bosses heal on an actual player
  death/respawn as configured in the supplied default behavior.
- Turntables rotate matching vertical double-block pairs together with rollback
  if the second placement fails. Beds, double chests, extended pistons and unknown
  machine assemblies still stay intact instead of being rotated unsafely.
- Firework lenses launch real, bounded cosmetic rockets with trail/twinkle/explosion
  presentation; tagged lens rockets cannot deal native explosion damage. Missile
  clipping now skips foliage within traversal, so foliage cannot hide a wall.
- Ten source player gestures (axe, solar and ice breath) are retargeted to the
  approved animation library. Source first-person arm tracks are sampled as data
  and bound to the existing hand renderer, with single ownership of first-person
  posing. All native API and visual fit checks remain. Custom third-person rig
  translations and item-bone transforms remain a fidelity limitation.
- Solar beams use the supplied start/end flare atlas and block-face impact flare,
  with a clear first-person caster view. Fissures now carry crack decals over
  exposed one-block ledges and play a positional loop which ends with the entity.
- Fixed recovered animation root lookup: authored root tracks affect the original
  root bone, while sibling controller bones remain accessible. Naga tails now use
  the supplied six-point bend-constraint algorithm and coefficients, retargeted
  onto the original hierarchical native mesh; long frame gaps/teleports reset
  the simulation. The source's tail-demo clip has no ordinary encounter trigger;
  dodge is not an omitted keyframed clip in the supplied model.
- Frozen overlays now cover native living models, recovered Naga/Foliaath/small
  creature meshes, Gecko entity meshes through its layer event, and first-person
  arms. Native facing/walk freeze is connected. Full custom/Gecko animation-pose
  freezing still needs closure; do not claim all model motions pause correctly.
- Cauldrons now hold mushroom/rabbit/suspicious stew and beetroot soup using the
  original animated surfaces. Pour/take preserves exact item components and
  consumes one paid serving; distinct suspicious-stew effects cannot be mixed.
  Native three-level capacity is retained. Food cannot become potions/arrows.
  Broader soft-fluid adapters/recipe reload integration remain open.
- Lava cauldrons regain source bubbles and ambient sounds, correcting the source
  sound-height bug. The source baked model concerns fluid rendering, not a new
  layered-lava crafting mechanic. Whirlwind now shows its actual supplied animated
  translucent mesh and uses native spell particles/audio, which is what its source
  UpwindBlock actually uses; the previous custom-particle TODO was inaccurate.
- Same-world player teleports bring up to sixteen nearby, already-following owned
  pets to safe loaded destinations. Sitting/leashed/riding pets stay put. Existing
  recall permission, collision, fluid and border checks apply; no forced chunk
  loading or cross-dimension pet migration. The supplied empty extra-pet tag is
  retained for explicit compatibility additions.

Still open: source creature pose/encounter/layout details, botanical shader and
other recorded behavior/presentation edges, remaining household/compatibility
features and any other earlier gaps not explicitly closed above. Combined build,
resource, server/client tests and the owner's local visual playtest remain pending.
No commits, pushes, PRs, release/update changes or live operations were performed.


### Continued original effects and rope workshops — October 31 (OpenAI Codex, GPT-6)

Local implementation only. No compile, tests, game launch or publishing. This
entry supersedes matching earlier gaps, not the whole remaining collection list.

- Elokosa now binds the original transformation texture's animation-controller
  opacity and all four moon-phase paw overlays. Droplets use six actual mouth
  bones, twenty ticks attached and eighty-tick lifetimes with the supplied pixel
  sprite. Emission is intentionally limited to rendered, visible actors; no
  off-screen particle simulation. Dynamic Elokosa tail and exact leap search
  remain open. Deferred layer color/pose and droplet alignment need visual checks.
- Turntables rotate grounded entities while powered, with crouch opt-out, real
  owner permissions, loaded destination/border/collision checks and a native
  relative player-facing packet. Rotating straight rails reorient up to sixteen
  unoccupied carts and their velocity; occupied carts remain untouched. Existing
  atomic tall-pair and unsupported-assembly behavior is retained.
- Rope arrows now have a native ArrowItem/projectile, original item/projectile
  artwork, 32 paid rope capacity, crafting and inventory-click refilling, saved
  unwinding endpoints, native component-preserving pickup, and one placed rope
  per tick after an authorized hit. Native intangible/free multishot copies have
  zero rope charges. Anonymous dispenser shots cannot place blocks. Post/fence
  conversion into the source rope-knot block remains separate from this route.
- Ropes accept up to sixteen currently player-leashed animals using native leash
  knots. The knot stays attached to the actual rope and uses native release when
  the rope disappears. Support traversal no longer treats a small floating loop
  of ropes as an anchor; unloaded or over-budget networks remain intact. Failed
  rope placement/retrieval does not consume or refund an item.
- Added the sixteen supplied bunting colors in both wall and ceiling placements,
  original models/textures/recipes and a four-slot rope bunting assembly. Each
  connected branch holds a separately paid/retrievable pennant. Breaking the
  assembly drops the rope and stored pennants once; lost branches eject their
  decorations. Native rendering uses the exact supplied flag/clip geometry, UVs
  and directional sway. Bunting inventories are not exposed to hoppers. No
  registration-only claim: placement, support, use, storage and rendering are
  connected, but not yet validated in Minecraft.
- Updated the in-game Household Encyclopedia with these actual interactions.

The whole collection remains unfinished. In addition to earlier source behavior
and presentation gaps, the supplied Supplementaries catalog still lists pulley /
sliding machinery, cannon, globes, labels/way signs and other families requiring
implementation or explicit source-registration reconciliation. Do not mark these
complete merely because their artwork was already copied. Final combined
validation remains deferred until the implementation pass is complete.


## Owner-requested checkpoint — October 31

Saved by OpenAI Codex (GPT-6) on request for a progress estimate. All current
collection work remains local. This checkpoint records implementation status,
not working-in-game acceptance or release readiness.

**Rough implementation estimate: about 80%, with a 75–85% uncertainty range.**
This is a judgment across functional families and their outstanding systems,
not a measured percentage of files, registry entries, code lines or tests.
Copied resources and duplicate source collections are not counted as completed
gameplay. Integration failures discovered by the final checks may increase the
remaining work. Final combined validation has not started; the earlier small
subset/equipment tests do not validate this whole collection.

### Current stopping point / next action

Elokosa presentation now has original transformation/moon overlays and mouth
drool. Next, finish its original ballistic leap calculation, safe landing and
surface candidate search using the supplied reference classes. Replace the
current approximate leap without introducing unbounded world scans or chunk
loading. Original dynamic tail motion is also still open. Reference reads and
decompilation were started; the replacement leap implementation was NOT made
before this checkpoint.

### Remaining implementation groups

1. Creature fidelity and encounters: Elokosa leap/tail; Sculptor branching
   challenge course and pose behavior; monastery path-loop/terrain closure;
   recorded creature particle/pose details and freezing custom animation poses.
2. Household machinery and tools: pulley/sliding assemblies, cannon, globes,
   labels/way signs, rope post/knot conversion and other catalog families such
   as heads, lumisene and gifts. Reconcile original registrations first: dormant
   or disabled source assets are not automatically missing gameplay.
3. Household compatibility and presentation: broader soft-fluid/cauldron recipe
   adapters, remaining sign/lantern motion and display controls, source interaction
   edges and original Home furniture details.
4. Magic and other family closure: Gaia distortion presentation and the recorded
   botanical/arcane behavior, renderer and compatibility gaps; audit earlier
   ledger entries for gaps not explicitly closed by later entries.
5. Source limitations: empty Fast Paintings folder and missing original Home
   trophy/saddle/mangrove-zaisu assets remain documented, not silently invented.
6. After implementation: combined compile/resource checks, relevant server and
   client tests, repair failures, and prepare the owner's own local playtest.
   Original models, animations, recipes, permissions, persistence and rendering
   need runtime acceptance; none is implied by this estimate.

Latest implemented local batch: original Elokosa overlays/drool, grounded
turntable entity/cart motion, paid/refillable rope arrows, rope support and leash
knots, and sixteen colors of wall/ceiling/rope buntings with saved attachments,
original rendering and Encyclopedia instructions. These additions are uncompiled
and untested. No commit, publication, profile edit or server action was made.


### Continued encounters, pose physics and globes — October 31 (OpenAI Codex, GPT-6)

Local implementation only; no compile, tests, game launch or publishing yet.
These entries supersede the corresponding earlier implementation gaps, not the
remaining whole-collection checklist or runtime acceptance requirements.

- Elokosa now selects original launch-angle candidates with a bounded ballistic
  solver, safe loaded floor/wall/ceiling landings and per-request collision work
  limits. Failed searches retain the current attachment. Source release timing,
  friction handling and grounded/wall/ceiling clips are connected. Runtime motion
  and collision acceptance remain pending.
- Original six-joint Elokosa tail and four-joint Sculptor beard use the supplied
  spring coefficients on their original bone hierarchies. Long frame gaps and
  teleports reset simulation; frozen actors hold it, and the beard holds while
  its pillar moves. Sculptor beads and skirt use original head/knee/hem locators
  and correction equations. Hierarchical retargeting and cloth fit are unverified.
- Gecko meshes retain a captured visible bone pose while frozen. Animation clocks
  still advance underneath; this is visible pose holding, not clock suspension.
- Sculptor trials now plan a randomized main path and optional reconnecting
  branches incrementally before taking payment. Safe main-path platforms never
  crumble; optional upper-course platforms may. Platforms appear as the pillar
  reaches them, and entity insertion failure rolls the course back. Native
  ordinary-player jump heights replace source jumps that require its movement
  assumptions. Final path/terrain race and course reachability require further
  review and runtime validation; this is not a proved playable course yet.
- Both globes use original stands, globe artwork and special flat/sheared meshes;
  saved names select the supplied named textures, shears toggle cutaway views,
  spinning responds to interaction/redstone and outputs comparator strength.
  Wandering traders offer them for the source 10/12 emerald prices. Original
  procedural dimension maps, round shader and special animated-name effects
  remain open; the ordinary supplied Earth appearance is implemented.

Remaining: household machinery/tools and source-registration reconciliation,
monastery layout closure, recorded botanical/arcane and compatibility edges,
source limitations and final combined validation. Nothing has been published.


### Coastal life, direction signs and layout follow-through — October 31

Implemented locally by OpenAI Codex (GPT-6); still uncompiled and untested.

- Barnacles retain the six-face original artwork, waterlogging, bone-meal spread,
  Silk Touch per-face harvesting, ambient bubbles and bounded source-shaped
  ocean/beach generation in new chunks. Each player/automation growth destination
  checks claims and loaded chunks. Flax has its supplied flower-pot variant.
- The original boat-in-a-jar now has its crafting route, vessel model and rocking
  ship mesh in-world and as an item. Globe held models now share the world globe
  renderer rather than showing only the stand.
- Eleven original wooden direction signs are crafted from their corresponding
  vanilla signs. Upper/lower boards can attach to an existing fence/wall post,
  preserving its material, or mount on a solid wall. Each has separately saved
  lettering and rotation; editor tokens are tied to the exact board and revision.
  Wrench turns, shared dye/glow/wax styling, shears recovery, hopper exclusion,
  original arrow meshes and post restoration are wired. Native neighbor fence
  connection equivalence and placement/render/drop behavior need final acceptance.
  Styling is shared between the two boards in this adapter.
- Sculptor routes recheck loaded/protected terrain, headroom and jump corridors
  immediately before payment. New platforms cannot intersect an accepted jump;
  branch landings cannot invalidate the required main path. Physical reachability,
  course-generation success rate and work cost remain unvalidated.
- Monastery assembly now honors source root-level as well as nested conditions,
  evaluates self-referencing fallback pools at the depth limit, and joins directly
  matching open path ends instead of capping both. Two referenced source templates
  (`monastery_wall_straight_deadend` and `monastery_wall_corner_deadend`) are absent
  from the owner library. They have not been invented or called implemented.
  Full layout/terrain closure remains to verify and complete.

Remaining collection work has not disappeared: pulley/sliding machinery, cannon
and its related equipment, remaining active Supplementaries families, botanical
shader/recorded compatibility edges, source model/presentation gaps and combined
validation. Catalog entries without matching source registrations still require
reconciliation, rather than automatic registration as nonfunctional items.
No commits, pushes, PRs, packs, personal profiles or live-server changes.


### Cannon, original heads and key-lock continuation — October 31

Local implementation by OpenAI Codex (GPT-6), not compiled or tested yet.

- Cannon: shared two-slot inventory, gunpowder power selection, aim controls,
  forty-tick fuse, cooldown, water cancellation and owner-attributed redstone
  firing; original cannon mesh, lit/loaded textures and firing sounds. Native
  projectiles/cargo, bounded cannonball damage/ricochets, stackable ball piles,
  voluntary rider boarding and launch/fall protection are wired. Fuel/payload
  consumption follows successful launch. Claims and loaded-chunk limits apply.
  Remaining source parity: boat mounts, other mounting faces, trajectory preview,
  anonymous pearl-marker behavior, specialized confetti/cargo behavior, exact
  projectile tuning and rider pose/gliding. Runtime trajectory and rendering
  acceptance are pending, not claimed complete.
- Enderman/spider heads: original meshes, emissive eyes, wearable equipment,
  note-block voices and native charged-creeper loot routes. Enderman heads use
  front-facing player gaze for jaw animation and redstone strength. Floor-head
  rotation/mirroring now handles all sixteen rotations. Vision shaders and
  expanding-jaw collision remain open; visuals/equipment unverified.
- Named keys and key-lock block are being integrated with netherite doors,
  trapdoors and existing safes. Server-side key matching preserves claim checks;
  lock names are saved but excluded from client update packets. Lock blocks emit
  a twenty-tick pulse. Keyed netherite doors ignore redstone bypasses. Resource
  generation, source lava-trapdoor handling and full acceptance remain pending.
- Direction-sign wrench turns consume durability; failed final-board removal
  restores its board/text instead of losing it.

A source-registration audit also found active non-block tools missing from the
older block-only catalog, including bubble blower, keys, gunpowder fuses,
slingshot/quiver/bombs and other families. Those need actual gameplay, not just
catalog aliases. The collection is still incomplete. Existing pulley/sliding,
monastery, botanical/compatibility and other recorded gaps remain. All work stays
local; combined compilation/tests are deferred until implementation is complete.


### Household machinery and remaining handheld families — October 31 continuation

Implemented locally by OpenAI Codex (GPT-6). This entire continuation is still
**uncompiled and untested** under the owner's defer-validation instruction.
Nothing was committed, pushed, published or installed in a launcher/server.

- Named keys/locks now have runtime resources, recipes, private saved key names,
  netherite-door redstone bypass prevention and water/lava-loggable trapdoors.
  Authorized bucket interactions pass through to the native bucket handler.
- Bubble blowers consume stored soap; their Stasis enchantment creates temporary
  bubbles. Original bubble meshes, palette, suds/pop sprites and sounds are wired.
  Soap support and Feather Falling affect platform survival. Source dispenser
  handling and the exact two-hand pose remain open.
- Vanilla gunpowder places the original connected fuse mesh, burns in timed stages,
  and lights permitted adjacent TNT/candles/campfires/cannons. Actor attribution,
  claims, rotations, jumping extinguish and burning-projectile ignition are wired.
  Source miniature explosion behavior is not reproduced.
- Pulleys have rope/chain storage, cooperating adjacent shafts, bounded sticky-load
  resolution and carried single-block vanilla inventories. Movement uses saved
  piston carriers, native collision pushing and a rollback snapshot. Assemblies
  deliberately stay in one chunk and reject linked blocks/unsupported machine
  inventories. Sliding blocks can be pushed, fall and continue across ice.
  Carried special block-entity rendering, pressure-plate/piston slide initiation,
  wider cooperation and horizontal rope handling remain open. Transaction/save
  behavior is implementation only, not validated safety evidence.
- Crumbly/suspicious gravel bricks and paid-item archaeology crafting are wired;
  hidden items retain metadata, and nested containers are excluded. Original
  models are used. All drop/brush/save paths still require combined validation.
- Six-slot quiver: inventory loading/unloading, arrow-type selection and feeding
  native bows when loose arrows run out. Draw-time consumption commits the native
  ammo delta back to the stored stack. Original held/GUI models are wired. Worn
  back rendering, dye variants and accessory-slot discovery remain open.
- Slingshot: original draw stages/sounds, 20-tick charge, 192 durability, offhand
  ammunition selection, native projectiles and paid block placement with bounded
  protection checks. Buckets, source special enchantments, parrot launching and
  exact custom hand animation remain open. The adapter currently enables a wider
  projectile selection than the supplied default config.
- Normal/blue bombs: impact detonation, water extinguishing of normal bombs,
  five-child blue clusters, original item/projectile models and explosion audio.
  Per-cell/per-target claim/PvP checks replace unrestricted native explosions;
  containers are excluded. Anonymous dispenser projectiles currently do no damage.
  Source fireball supercharging and exact explosion/particle parity remain open.
- Cave/slice maps use native filled-map display and save data, with the creation
  height stored on the stack. Held maps survey only loaded chunks, one 128-pixel
  stripe per tick with a nine-block vertical bound; unknown areas stay unknown.
  Scaling/cloning/locking behavior and client rendering remain unvalidated.
- Antique ink applies original font glyphs to signs and written books; soap/ink
  cleaning restores normal lettering. Globes can convert between ordinary/sepia.
  Antique map colors and shared household-sign font styling remain open.
- Hat stand uses the original short wooden mesh and native helmet/skull layers,
  head-only equipment, wrench rotation, removable base and two-hit recovery.
  Source gaze/jumpscare and special cauldron animation remain open.
- Piñata uses the original pig mesh and ears, nine slots of player-supplied contents,
  ceiling attachment, leads, hit swing and bounded loot scattering on destruction.
  It does not manufacture loot. Source moving-anchor follow-through, exact spring
  simulation, confetti variants and loot-table integration remain open.

Next: dispenser minecart and remaining actual source registrations; complete the
specific missing behaviors above plus the earlier cannon, botanical/arcane,
monastery and compatibility gaps. Do not count internal aliases as separate
playable features, or copied assets as finished gameplay. Entire collection is
still incomplete; combined validation must follow the implementation pass.


### Original galleon encounters and remaining transports — October 31

Local implementation by OpenAI Codex (GPT-6); **not compiled or tested yet**.
This closes the earlier "next: dispenser minecart" implementation entry, not the
whole-collection acceptance gate.

- Dispenser minecart now has nine persisted slots, native rail motion, powered
  activator-rail edge firing and owner-attributed projectiles. Native dispenser
  actions use a private inventory proxy with bounded nearby protection checks.
- Original dragon banner, blast armor trim and Heave Ho disc have native item,
  banner/shield, trim and jukebox resources, plus shipwreck/desert/end-city reward
  routes. Datapack-replaced loot tables are not silently modified.
- Plunderer has supplied geometry/UVs and sounds, sword combat, spyglass pose,
  finite unowned encounter-cannon use, boat boarding/steering/abandoning and saved
  supplies. It does not join automatic raids/patrols or target villagers.
- Nine cannon-boat wood variants use native boat motion/rowing, the original
  cannon mesh, nine saved supply slots, aim/fuse/recoil and finite shots. Sneak-use
  opens supplies; flint-and-steel/fire charge ignites; mounted look controls aim.
  Player shots retain the shared owner/claim/PvP checks. NPC cannonballs damage
  targets but cannot destroy terrain. Original item artwork is retained.
- The 17 supplied galleon templates and weighted connector pools are adapted to
  native new-chunk generation, with deep-water checks, crew/boat markers, original
  loot, keyed safe, urns, flag patterns and captured allay data. Source spawn-box
  helpers are removed; encounters use finite crews rather than repeated respawns.
  Sea-level placement, hull joins, marker offsets, loot schemas and renders still
  require the combined runtime pass. No existing world chunks were changed.
- Small correctness fixes: hat-stand first hit cannot instantly break it near
  world tick zero; failed globe recoloring restores its original state; quiver
  source rarity restored.

Remaining source details in this family: shoulder parrots/advanced captain AI,
boat-cannon interpolation/tilt and source trajectory preview, exact cannon
projectile/rider behavior, and the separately listed household/creature/magic
compatibility details. Registering these families does not certify full parity.
Nothing was committed, pushed, published, deployed or launched.


### Equipment, moving attachments and source animations — October 31 continuation

Implemented locally by OpenAI Codex (GPT-6), still **uncompiled and untested**.
These close the matching earlier implementation gaps, not runtime acceptance.

- Quivers now feed bows from a dedicated Trinkets slot, retain their six real
  ammunition slots, use the original dyed/undyed held and GUI models, and render
  on the back, hip or thigh. Sneak-use cycles display/hidden modes; dye and soap
  use native components. They require no research. Inventory-only quivers still
  supply ammo but do not draw another remote-player accessory.
- Bubble blowers now work from dispensers with paid soap charges and protected,
  loaded-space checks for Stasis platforms. Shared arm hooks provide blower and
  slingshot two-hand poses without competing with gun poses.
- Carried chest blocks use detached content-free native chest rendering on the
  assembly carrier. Failed final block replacement retains its carried data.
  Pulley placement on a top/bottom face now chooses a working horizontal shaft.
- Cannon boats use the correct bamboo raft model, interpolated aim and the
  native hull's rocking pivot. Nonfinite saved aim values are rejected.
- Hat stands use the original supplied DEFAULT keyframe track and articulated
  neck/head pivots. Stands in cauldrons/composters/hoppers/pots animate periodically;
  sneak-empty-hand interaction triggers the animation with gaze tracking.
  The original optional jumpscare track is retained but not automatically used.
- Piñatas follow native piston/assembly anchors, use the supplied spring-motion
  constants, original hit/burst sounds and shared confetti particles. Authored
  loot-table rewards unpack once and retain unspilled rewards after insertion
  failure. Player-stuffed contents remain real items; no automatic free loot.
- Antique lettering now applies to household direction signs/doormats and speaker
  announcements, retaining server editing permissions and wax checks.
- Slingshots launch filled buckets through permission-checked native placement,
  return the empty bucket after success, and leave full payloads recoverable on
  failure. Stasis removes projectile gravity; Quick Charge affects charging;
  multi-projectile shots consume one real payload each (deliberate economy-safe
  difference from free duplicate building payloads). Shoulder parrots retain
  variant/ownership and leave the shoulder only after successful insertion.
  Shared arm poses follow actual enchantment-adjusted charging time.

Still required: remaining source/compatibility details recorded below and earlier,
then the combined compile/resource/server/client checks and visual acceptance.
No commits, pushes, releases, launcher edits or server actions occurred.


### Vessel, globe and cannon closure — October 31 continuation

OpenAI Codex (GPT-6). Local implementation only; **not compiled or tested**.
This entry supersedes the matching earlier gaps, not the whole collection.

- Jars/goblets preserve exact drink components in a separate portion store.
  Pouring returns the original empty container; bottling requires one, and drinking
  cannot return a second container. Goblet saves from the earlier local inventory
  representation migrate. Milk, honey, potions and component-defined drinks work;
  item inventories, captive mobs and native fluid tanks exclude drink insertion.
  Filled vessels retain contents on pickup and render their liquid while held.
- Faucets transfer real drink portions between vessels and native fluids between
  tanks and cauldrons, including partially filled water cauldrons. Extraction and
  insertion must reserve a full serving before changing either side. Claims,
  loaded-cell checks and unopened loot-container restrictions remain in force.
- Gaia has a port of the supplied distortion/grain shader on the current native
  pipeline, including invulnerable/hurt intensities. Actual shader compilation,
  skin rendering and optional Iris compatibility have not yet been exercised.
- Cannons now mount on all six faces, with corresponding base collision, muzzle
  and local aim controls. A wrench/spyglass shows a bounded loaded-terrain preview
  for supported projectile types; unsupported or arbitrary entity trajectories
  are omitted. It stops at the first block/fluid hit and does not predict ricochets.
  Voluntary riders render at the barrel with its aim; valid glider equipment can
  enter native gliding after launch. Bomb and dyed-confetti payloads use their
  actual projectile behavior. Cannon safety/visual acceptance is still pending.
- Bombs regain the supplied large-fireball supercharge: normal bombs gain a
  bounded six-block blast; blue bombs produce ten paid-shot child fragments.
  Per-block/per-target permission checks remain; no native unbounded explosions.
- Globes now use the supplied 32x16 procedural generator and normal/sepia palettes,
  with bounded placement retries. Artwork is saved/synced without exposing the
  world seed. Named Earth/other supplied textures remain selectable. Round globes
  use a spherified cube with original UVs through native entity rendering rather
  than a separate obsolete shader. December/Earth Day variants use the configured
  calendar date. The generator depicts fictional terrain, not surveyed chunks.
- Sliding blocks preserve owner identity across native piston movement, including
  saved moving carriers, and resume their bounded ice motion afterward. Online
  owner permission, town/claim boundaries and the single-chunk limit remain.
- The supplied enderman/spider wearable heads request native invert/spider effects
  in first person, composing with other effects. Removing the head or changing
  camera mode removes the request; the local JVM head-vision opt-out is available.
  Enderman jaw collision expands with its rendered opening.
- Antique ink/soap crafting now changes map appearance as well as books. Held and
  framed antique maps use the supplied light/dark palette without overwriting
  native map colors or changing ordinary copies. Original antique item artwork is
  used; dynamic map textures have bounded caches and reload/disconnect cleanup.

Still open: source registration reconciliation, remaining household sway/display
and machine/interaction edges, recorded botanical/creature/source fidelity details,
missing original-input exceptions, then the combined validation and fixes. None
of the changes above is a claim of successful runtime verification. No commit,
push, release, profile modification or live-server action occurred.

### Botanical recipe access and original presentation — October 31 continuation

OpenAI Codex (GPT-6). Local source implementation; **not compiled or tested**.
This supersedes the matching earlier gaps only.

- Hanging signs now render their board, text and displayed items under the same
  pendulum transform. Wall brackets remain fixed; attached ceiling signs rotate
  around their suspension. Wall lanterns swing below a fixed bracket. Motion uses
  the supplied damping/frequency with bounded collision impulses and weak caches.
  Custom resource-pack bracket geometry and renderer compatibility need acceptance.
- Redstone-powered Open Crates give dropped items a saved, merge-aware 200-tick
  botanical consumption delay. This is separate from vanilla player pickup delay;
  players and ordinary hoppers can still collect them.
- Luminizers render the supplied blue wisp sprite as the original ten-point spiral
  and sync their resolved fork route. Wand/Monocle inspection shows a bounded
  twenty-block radius and route. Filter-scroll and factory-list previews and
  attached-cosmetic tooltips are implemented.
- Starfield blocks use a native-pipeline port of the supplied animated shader.
  Camouflaged platforms now collect the displayed block's biome tint sources.
  Enderman head collision is dynamic instead of using a stale shape cache.
- Dandelifeon reads a one-cell ownership-checked boundary halo with quarter-age
  penalties. Its rollback restores complete saved cells, including ownership and
  simulation leases; missing replacement block entities abort the transaction.
- Corrected botanical recipe parsing for the supplied primitive Livingwood output
  and block-predicate catalyst form. Previously those formats would throw during
  conversion/infusion. Invalid catalyst and Pure Daisy outputs are rejected during
  recipe loading rather than during a world tick.
- Optional JEI now has botanical apothecary, Daisy, infusion, altar, plate, elven
  trade, brewery and ore/stone conversion categories. Displays include costs,
  setup conditions, weighted outputs, reusable runes, consumed reagents and the
  four supported brew vessels. The display manifest is generated from the same
  supplied formulas as server recipes. It is explicitly a bundled reference;
  server datapack changes remain authoritative in the encyclopedia.

No compilation, tests, game launch, publication, profile change or live-server
operation occurred. Remaining: source-registration reconciliation, outstanding
active source interaction/renderer edges, then combined validation and repairs.
Known missing-original input exceptions remain open, not silently replaced.


### Collection closure and combined validation entry — October 31

OpenAI Codex (GPT-6). All changes remain local. This entry supersedes the older
"current stopping point" and matching unfinished-family checkpoints; it does not
claim runtime success. The implementation pass now covers the available active
families with the native adaptations below. Combined validation begins next.

Additional implementation since the botanical checkpoint:
- Original road-sign structure anchors/spacing and settlement arrows, with saved
  pending state and rate-limited server-thread locating. The native worker replaces
  the source temporary block generator. Generated posts do not rewrite settlements.
- Cannon pearls relocate their firing cannon/boat, not their operator. Stationary
  inventory-bearing cannons retain the existing single-chunk transaction boundary;
  boats check loaded destination and passenger clearance. Movement is permission-
  checked, and cannon replacement has rollback. Runtime acceptance is pending.
- Bore/Warp drops return behind the authorized spreader, and paint lenses have
  explicit plain/colored glass, terracotta, wool, candle and building-family maps
  plus a compatibility registration hook. Arbitrary machine names are not recolored.
- Named statues use native profile skin/cape resolution, slim rigs, clothing poses,
  seasonal head/party-hat presentation and preserved item/name cargo on breaking.
- Backpack creature spaces now display isolated original entity/model portraits,
  synchronized only to the authorized open menu. Preview entities never enter or
  tick in the world; capture/release continues through the existing server checks.
- Default and Programmer Art connected-surface packs are separately selectable.
  Sandstone side transitions depend on the upper neighbor. Unjoined sandstone
  retains Minecraft 26.3 base art; these packs should be below custom artwork.
- Tooltip resource rules support item IDs, item tags, namespaces, display-name
  substrings and rarity, with priorities, original atlas selection, reload cleanup,
  colors and rotating item previews. Reduced motion stops preview rotation.
- Zoom adds keyboard strength steps, configurable step size/limit, cinematic
  mouse smoothing, linear/eased transitions and optional secondary-HUD/spyglass
  frame policies. These do not overwrite native persistent FOV/sensitivity options.
- Lectern books have per-page ink and quill styles using supplied art. Text filtering,
  session identity and compare-before-write remain. Styling is retained on unsigned
  pages and applied to signed components. This is page-level formatting, not a
  rich-text span editor; no executable click actions or arbitrary component JSON.
- Optional `chat.system_delivery=false` is a server-controlled native adaptation
  of the supplied no-report delivery: when enabled, filtered player text is sent
  as disguised/server chat after recipient routing. Login authentication, incoming
  signatures, moderation and ordinary chat on other servers are unchanged. It does
  not fake trust icons or promise protection from server logs/screenshots. Joining
  players receive an explicit notice when this mode is enabled.

Scope reconciliation:
- Supplementaries' active block-generator behavior is now bound. Other catalog-only
  assets have no registration in the supplied source; `structure_temp` only removes
  itself. The source-disabled Forbidden desk/research desk/book/Aureal tank remain
  classified separately. Empty registration lists are audit evidence, not tests.
- Earlier Elokosa/Sculptor/monastery, frozen-pose, vessel, globe, sign-sway, botanical
  recipe and particle entries supersede their corresponding older TODO paragraphs.
- Native adaptations intentionally retain bounded scans, claims, transactional
  movement and online-owner authority; they are not every old mod's configuration
  switch or optional-mod compatibility module. Exact custom rigs/materials and
  API signatures still need the combined checks and visual acceptance.
- Missing source inputs remain: two monastery dead-end templates, Home saddle and
  mangrove-zaisu geometry, two trophy skins, four cherry-waffle door variants, and
  the empty Fast Paintings supply. No invented replacements are counted as original.

No compile/test outcome is recorded yet at this entry. No commits, pushes, PRs,
releases, updater/profile changes or live-server operations have occurred.


## October 31 combined validation — in progress

OpenAI Codex (GPT-6) began the deferred combined checks after the implementation
pass. This entry supersedes earlier statements that no compilation was attempted.

- Repository structure and relative documentation links: passed
  (`python -X utf8 scripts/check_repository.py`).
- Common Java compilation: passed after repairing the accumulated 26.3 API
  mismatches. Diagnostics reduced from 725 to 247, 93, 7 and then zero common
  errors. This is compiler evidence, not gameplay approval.
- Native adaptations include immutable item-container copies, prediction-aware
  inventory transfers, current entity/particle types, server punch handling,
  scoped pet ownership, weather saved data, component-preserving statue names,
  farmland/tool components and bounded block/entity movement.
- Corrected a statue name-tag branch duplicated into empty-hand interaction,
  moving-assembly block-entity type shadowing, and statue holiday checks that
  would otherwise run every server tick.
- The resource audit found malformed nested model parents in the Amendments
  custom-loader conversion. Corrected 85 generated hook/lantern adaptations and
  their generator to retain inline model objects rather than wrap them as IDs.
  The checker now reports invalid parent types instead of crashing.
- Added five game-test regressions for statue cargo/name persistence, foreign
  cannon permissions, invalid saved cannon values, moving-container conservation
  and blocked-movement preservation. These tests are not yet run.
- Client compilation, resource audit and combined gameplay validation are still
  in progress. No playable-build, visual-acceptance or release claim is made.

All work remains local. No commits, pushes, server actions, updater promotion or
personal launcher/profile changes.

- Follow-up resource audit resolved inherited texture overrides before reporting
  missing files. Four actual compatibility rod sprites are absent: prismarine
  rod/cross and edelwood stick/cross. Their supplied geometry now uses native
  prismarine and the supplied Edelwood bark respectively. These are material
  fallbacks, not recovered original rod-specific textures; source archives stay
  untouched. Other apparent Home template misses are overridden by child models.
- Native target-member audit checked 327 mixin/accessor declarations. The new
  magnet-drop hook targeted a removed Player overload; it now targets the actual
  ServerPlayer prediction-aware drop method. Runtime mixin application remains
  to be tested; name existence alone does not prove full signature compatibility.

### Combined validation follow-up — still local, not release-ready

- Client compilation initially reported 183 API errors. Current fixes update the
  render submission, model visibility, block tint, screen sizing and GUI APIs;
  the rerun is in progress. The large resource snapshot/copy stage was inspected
  and was doing filesystem work, rather than waiting for owner input.
- Added six more unrun game tests covering exact mana debits/refusals/capacity,
  mining progression isolation, backpack tier-upgrade component preservation and
  upgraded stack limits. Together with the five household tests, eleven new
  regressions are queued; no passing result is claimed yet.
- Repaired 30 aquatic spawn-egg inventory definitions using native spawn-egg art,
  seven missing decorative/rod inventory model links, zero-area placeholder faces
  in supplied Home templates, and the dye-vat faucet underside's texture binding.
- Converted 275 new-collection loot tables to 26.3 condition/modifier syntax and
  updated generators to preserve this conversion on regeneration.
- The full static data audit failed. Most diagnostics were unrecognized new
  descriptor-driven registrations, but it also exposed the genuine resource/loot
  defects above. Added explicit registration recognition for eight content families;
  remaining audit diagnostics and runtime validation are still being resolved.
  No audit failure is silently waived or reported as a passing test.

- Common and client Java compilation have both passed. The test compile exposed
  one new test API mismatch (`ItemContainerContents.getSlots`); corrected to
  inspect the actual copied module items. The combined server run is queued in
  the repository's disposable GameTest world.
- Focused full model-reference audit (`check_mod_data.check_model_textures`) now
  reports zero diagnostics. This checks resource linkage, not rendered fidelity.
- Added an isolated rendered-client smoke suite resolving new item render states
  and capturing eleven representative held-item views. It has not run yet.

### Lumisene source-audit correction and validation continuation

- The separate supplied `ModFluids` registrar was missed by the earlier block
  registration audit. This corrects the previous closure claim: Lumisene was a
  real missing system, not a stale asset or an audit-only unknown item.
- Added native finite 16-layer fuel with four bottles per bucket, glow-berry
  recipes, the original bottle/bucket/fire/fluid textures, drinking effects,
  ignition, finite burning, claim-boundary-aware flow, container transfer API and
  filled-container dispensing. Burning deliberately does not start wildfires;
  Flammable extends existing burning as in the source instead of inventing damage
  multipliers. Spawn protection applies to unattended placement and flow.
- Added five regressions for conservation, blocked/overfull placement, foreign
  claims, finite burning and bottle effects. Together with earlier additions,
  sixteen new server regressions await execution.
- The first combined server invocation stopped at client compilation because
  Lumisene was added after that invocation's common compile stage. It did not
  reach game startup. A fresh common/client/test compile is in progress.
- Full model-link audit passed before the Lumisene resources. The broader static
  data checker remains failing (registration/tag recognition and recipe/metal
  accounting diagnostics); it is not reported as passed.

Work remains local; no owner profile or live server changes.

- Repeated full model-reference audit after Lumisene: zero diagnostics.
- Focused loot/recipe/tag audit reduced to two remaining obsolete Ribbit entity
  references; corrected those native Minecraft tags and their generator, and
  changed the removed music-disc tag to Fabric's verified `c:music_discs`.
  The checker now recognizes verified native registrations and pinned dependency
  tags instead of treating the imported households as switch-gated industrial
  ingot forms. Finished household devices follow the existing no-salvage policy;
  shared metal inputs/outputs remain subject to the mass-balance check.
- First actual server startup failed on seven Gothic window schemas containing
  only `open=true`. Multipart rendering implicitly supports the false branch too.
  Corrected boolean-schema extraction and regenerated both states' original-mesh
  collision boxes. Retesting; this is not a passing startup/game-test result.

- Runtime profiling found architectural shape unions repeatedly optimized per
  cuboid, and Home block construction scanned every shape variant for every
  state. Added immutable shape reuse, one final union optimization and indexed
  exact-state lookup (retaining wildcard fallback). Geometry is unchanged. Removed
  an unused retained descriptor and avoided capturing whole meshes in light
  lambdas, allowing large parsed descriptors to be collected after construction.
- A subsequent startup reached personal-loot registration and failed because the
  old global entity-data serializer registration is forbidden by pinned Fabric.
  Changed it to namespaced `FabricEntityDataRegistry` registration. Retesting.
- Found a reachable old custom-loader wrapper on barnacles: retained its original
  inline mesh in a native model. Current reachable item/block model graph has no
  remaining legacy-loader wrappers. This still requires the rendered-client run.

### Native startup and resource closure follow-up — October 31

- The local combined server run found additional copied vanilla-property hazards:
  the botanical invisible barrier inherited a waterlogged map-color callback,
  and the supplied single-state clawed log inherited an axis-dependent callback.
  Both now use the appropriate constant map color without changing their geometry.
  Botanical registration errors now identify the exact block and kind.
- Dynamic spring, mob-head, sign and bubble collision shapes are not frozen into
  an inappropriate startup cache. Spring-arm registration tolerates its head not
  being registered until later in the same registrar.
- Supplied Omega Arcoin and five reinforced Deorum tool sprites now have native
  generated/handheld inventory wrappers. Temporary Bifrost and Mana Flame world
  effects no longer create unintended blank inventory items (local, unreleased IDs).
- Added 30 aquatic spawn-egg names, five aquatic block names and 18 omitted
  collection display names. Chisel resource paths use Minecraft's slash-to-dot
  translation-key convention in the audit.
- Corrected four out-of-atlas face UVs in supplied wheelbarrow/broom/torch models;
  original texture pixels and geometry remain unchanged. Generators retain repairs.
- The focused assets/worldgen/tags/loot audit now reports **zero diagnostics**
  (`build/newest-focused-assets.log`). This is resource linkage evidence, not
  visual acceptance or full static-suite success.
- Shared equipment regeneration now retains 21 reviewed new tag groups through
  `tools/newest_equipment_tags.py`; all earlier tag memberships remain. Updated
  obsolete audit expectations that still required research before equipping gear,
  and recognized the implemented accessory slots and native special renderers.
- Combined server gameplay checks and the isolated rendered-client suite remain
  in progress. Earlier server attempts failed during registration, before tests
  executed. Full static art/legacy assertions still have unresolved diagnostics;
  no passing full-suite or release claim is made.

AI assistance: OpenAI Codex (GPT-6). Local work only; no commits, publication,
launcher-profile edits, or live-server operations.

### Native data reload corrections — October 31

All common registrars and worldgen definitions now pass native loading. The next
server run exposed 251 recipe/loot failures; 197 were recipes referencing three
unregistered window-frame consumables. Added those original ingredients, models,
sprites and five recipes, and preserved the change in the building generator.
Other repairs cover explicit loot uniform providers, nested loot-table values,
entity-type predicates, shears conditions, optional Twilight biome matching,
Deorum material tags, glass/chest/dye tags and renamed native grass/chain items.
Backpack dyeing now uses six native component-preserving recipes. Generating-flower
drops use a bounded flower-memory component for cooldowns, age and feeding history;
they never copy workshop inventories or linked/owner state. Regression tests added
for both inventory preservation and cooldown persistence. Common/client compilation
passed; a test-only stale RED_DYE reference was corrected to Items.DYE.get(RED).
The server rerun and rendered-client checks remain pending; no pass claimed yet.
The shared Concordance generator now includes the three implemented reliquary
words (Mark, Recall, Retaliate), with actual labels and descriptions.

Full static audit follow-up produced 1,397 lines, predominantly supplied-art face
warnings and diagonal connectivity expectations. It did not pass. Focused linkage
was clean before the newly corrected native codec failures: static linkage alone
is explicitly not sufficient validation.

OpenAI Codex (GPT-6); all changes local, no publication or owner-profile changes.

- Native data reload now succeeds. The first executed gameplay batch ran 18 tests:
  16 passed; two household fixtures were incorrect (a statue displays one item,
  and cannon editing is claim-controlled rather than private on unclaimed land).
  Fixtures now use one exact named item and an actual foreign land claim; rerun
  pending. No product permission checks were relaxed to make tests pass.
- The alchemy graph falsely treated installed botanical catalysts as consumable
  feed. It now excludes only that nonconsumed field for mana-infusion recipes.
  Both reported paths ran through a catalyst into snowball duplication, which
  actually requires an existing snowball and mana. Rechecked: Yeti Fur reaches no
  Rime ingredient through these recipe edges.
- Frostmaw's original cutout was retained: its 512x256 skin has just three partial
  alpha texels (194/205/214). A narrowly documented alpha-audit exception preserves
  the supplied skin and original opaque mob rendering; no texture was redrawn.

### Executed combined checks — October 31, 19:08 local

- **PASS:** common/client/test Java compilation and all **18 required server game
  tests**, in one disposable server world with the full collection registered.
  Evidence: `build/newest-server-tests-gameplay.log`, `BUILD SUCCESSFUL`,
  `All 18 required tests passed`. Covers household inventory/claim/movement
  behavior, portable mana accounting, six-tier backpack dye and upgrade cargo,
  generating-flower drop memory, and finite Lumisene transfer/burning/drinking.
- **PASS:** focused Winter Brine/workshop-product audit after excluding installed
  catalysts from consumed-feed edges (`build/newest-alchemy-audit.log`, zero
  diagnostics). This removes the two false alchemy reachability findings.
- **RUNNING:** isolated `NewestCollectionClientTests` and
  `EquipmentAccessClientGameTests` in `build/run/clientGameTest`. These do not use
  or alter the owner's launcher profile. Client/visual success is not claimed yet.
- These are representative regression checks, not proof every encounter, recipe,
  model pose or multiplayer combination has been manually played. Full original-
  artwork/diagonal compatibility diagnostics remain separately recorded.


### Rendered-import repairs — October 31 continuation

OpenAI Codex (GPT-6); all work remains local.

The first rendered run failed during atlas upload of the 400-frame Lumisene
strip. Runtime animation metadata now explicitly declares its frame dimensions
and sequence; the supplied PNG pixels are unchanged. This loading crash no
longer appeared in the next run, which reached an isolated world but timed out
waiting for chunk rendering. That run is **not** reported as a pass.

Additional repairs from actual client diagnostics:
- Native state-bearing decorative classes for directional Flax blocks, reversible
  daub braces, oriented fine-wood slabs, framed botanical wood and directional
  metamorphic bricks. Arcane polished pillars select bottom/middle/top meshes as
  matching adjacent pillars are placed or removed. Wicker uses the wall state
  scheme its supplied low/tall meshes require.
- Shared diagonal presentations for 213 newly imported native wall families,
  plus compatible native fences/panes/bars. Original weighted wall alternatives
  and supplied material/mesh inheritance are retained. Zero-angle source element
  rotations are handled instead of producing empty diagonal meshes.
- Authored complete-junction grass-capped walls, angled wire/spike fences and
  custom architectural assemblies retain their original cardinal joins. They
  are not replaced with generic diagonal rails. These are explicit native
  presentation limits, not claims of supplied diagonal artwork.
- Gecko 5 timeline arrays are joined into equivalent scripts; numeric keyframes
  are ordered. Seven undefined additive NaN offsets in the supplied Umvuthi and
  Elokosa clips are set to zero while retaining authored oscillation and angles.
  Static armor/staff meshes receive a valid empty-pose clip, without invented
  movement. Original library files remain untouched.
- Removed invalid texture-dictionary comment values; migrated legacy chain and
  farmland parents and native special-renderer base parents.
- Atlas generation resolves child overrides before requiring texture files, so
  unused template defaults do not block stitching the actual drawn materials.
  Real missing materials in block/item roots still fail generation.
- Restored the original globe spin sound, distinguished duplicate item labels,
  supplied required Concordance component metadata, corrected native entity and
  climbable tags, excluded no-item botanical blocks from creative-tab entries,
  and added Gaia shader overlay/lightmap bind groups.

`build/newest-diagonal-audit.log`: **zero diagnostics** after adapting the audit
for weighted original models and explicitly cardinal architectural classes.
The newer rendered-client rerun and full static follow-up are in progress.
No all-green, whole-collection visual-acceptance or release claim is made.


### Final rendered-import follow-up — October 31, 19:38 local

OpenAI Codex (GPT-6), local only. No publication or personal launcher changes.

- The atlas run exposed a native hat-stand renderer construction error: Minecraft
  26.3 expects `hat` below `head`, not beside it. The native hierarchy is repaired;
  its child pose now remains local instead of applying the head transform twice.
- A byte comparison refreshed 42 stale build resources before the next client run.
  A further targeted refresh includes the latest runtime compatibility changes.
- The 439 optional Fusion models now take particles from their installed ordinary
  source-tile texture instead of referring to unshipped CTM subpaths. Supplied
  connected texture sheets and animations are preserved. Both qualified and
  unqualified old vanilla chain material names resolve to `iron_chain`.
- Native sink honey/chocolate planes and streams referenced absent optional
  Create sheets. They now use vanilla honey and the existing owner chocolate
  surface. This is a bounded native material adaptation, not recovered Create art.
- Four cross-mod candle holder families (cupric, ender, soul, spectacle) had no
  dependency candle sheets supplied. Holder geometry remains original; installed
  cyan/purple/light-blue/white candle textures provide matching wax/wick UVs.
  Supplied skull-candle sheets are not substituted because their UV layout differs.
- The rendered regression now reports multiple broken block presentations together
  rather than requiring a full client restart per first missing face.

Repository structure/document-link check passed. The full static audit and
rendered client checks are still in progress at this entry; no pass is claimed.


### Accessory pass and inventory-presentation repairs — October 31

- The combined client run reached a disposable world and **passed real equip and
  unequip packets for all 14 populated slot groups** (back, backpack, necklace,
  spell focus, wings, boots, bracelet, ring, hat, belt, charm, quiver, glove and
  spell book), with no research fixture. Evidence:
  `build/newest-client-tests-final.log`, `Equipment inventory packets passed`.
- That overall run still failed the subsequent block audit on the optional cupric
  candle material; its prepared adaptation must be included in the next run.
- A second audit found 35 ordinary Home inventory definitions with empty meshes.
  Wall clocks now use the existing recovered native rig renderer. Bathtub item
  models combine both original block halves, with their material bindings and UVs
  preserved, centered into the original inventory display template. Supplied
  chair/sofa/clock particle materials are explicitly inherited from their block.
- The marker glyph now respects Concordance's existing 2,400-tick duration ceiling.
  The Hephaestus rune label no longer duplicates the existing Arcane Rune.
- The static registry audit now recognizes six explicitly registered botanical
  entity types instead of reporting their valid blacklist entries as unknown.

The full data audit completed with 1,013 geometry diagnostics (457 O1, 280 Z1,
252 N1, 24 I1) plus the eight data/catalog diagnostics corrected above. Geometry
findings remain unresolved rather than globally allow-listed: some are deliberate
open interiors or inverted interior shells; others need visual review for actual
holes/flicker. This is not a passing full static suite or release certification.


### Rendered evidence and statue repair — October 31, 19:47 local

The repaired client run passed its baked-block presentation audit: **837,276
states / 742,135 distinct state models**, with no rejected model or missing quad
sprite found in the checked choices. Randomized alternatives are sampled with a
fixed seed, so this is not an exhaustive screenshot of every weighted alternative.
All 14 accessory slot groups passed again. Original wild creatures and the
hat-stand entity reached their rendered screenshot steps. Clock and bathtub
screenshots were inspected: both are visible with their supplied geometry/skins.

The overall run then **failed** on the placed statue: its renderer still looked
for the old root-level player clothing parts. Native 26.3 clothing is parented
under the corresponding body part. Removed the obsolete pose-copy loop and
root-level hat lookup; clothing now follows its parent once, and hiding the head
also hides its child hat. This actual crash is fixed locally and being rerun.
The botanical luminizer wisp also now has an explicit particle-atlas binding.

Evidence: `build/newest-client-tests-verified.log` and the corresponding disposable
client crash report. The succeeding run is `build/newest-client-tests-statue-fix.log`;
no whole-client success is claimed before its completion.


### Combined client acceptance — October 31, 19:51 local

**PASS** `build/newest-client-tests-statue-fix.log`, `BUILD SUCCESSFUL in 4m 4s`.
Both `EquipmentAccessClientGameTests` and `NewestCollectionClientTests` completed
in one disposable run. This includes the baked-state audit, real inventory slot
packets, native item-model resolution, held samples (including restored clock and
bathtub), five creature/entity previews, and seven placed-block previews including
the previously crashing statue. The particle-atlas correction was included.
Representative screenshots were inspected; no owner profile/game was controlled.

The full unfiltered local server game-test suite and local assembly are running
in `build/newest-combined-server-final.log`. Original-art geometry findings and
missing originals remain separate explicit limitations, not silently cleared.


### Whole-server compatibility follow-up — October 31

The unfiltered suite runs **3,263 tests**, substantially beyond the new collection's
focused checks. Its first pass exposed older expectations affected by intentional
new behavior and one real march regression. Corrections prepared for the rerun:

- Five tree fixtures now permit the specifically registered `twigworks_twig`
  alongside sticks/that tree's sapling. They continue rejecting unrelated drops;
  leaf fruit/silk-touch/count assertions remain. The supplied Twigs loot adapter
  deliberately converts stick drops into equal-count twigs.
- The survival recipe fixture uses the actual crafting output picker when a
  completion recipe shares a layout. It checks the selected server result rather
  than assuming the vanilla first match must be that recipe.
- The Halloween peddler test counts the seasonal trade pool, excluding the newly
  added ordinary globe offers. Four seasonal wares remain required.
- The cemetery-fence expectation reflects the implemented supplied fence/wall
  connection adapter, including wooden neighbours, while retaining tag/gate tests.
- Non-city siege marches now accept a partial path to an enclosing wall. The old
  complete-path-only check left infantry idle at the gathering point and prevented
  ladder placement. The protected city approach retains its complete-path rule.

These changes are not yet reported as passing at this entry. The first complete
suite is still running; its results will be preserved before the repaired rerun.


Additional full-suite corrections prepared locally:
- Public town enchanting retains vanilla per-visitor temporary slots, while private
  tables retain the new persistent inventory. Stored town gear/lapis never enters
  a visitor's menu; closure returns the visitor's own items. Public rerolls still
  pay normal lapis/experience and recheck public/claim access. The town regression
  now preloads protected gear and checks that public use leaves it untouched.
- Diagonal tests retain exact coverage of eligible native families while explicitly
  recognizing the documented authored cardinal-only stacked-metal, wire/spike and
  complete-junction grass-capped families. Cross-material fence expectations match
  the supplied adapter; this does not allow arbitrary untagged future fences.
- The beginner Encyclopedia test now includes the implemented, ungated botanical
  and flower reference sections alongside technology/magic starter routes.


### Full-suite memory limit and repaired rerun — October 31, 20:08 local

The first 3,263-test unfiltered run ended with an explicit Java heap exhaustion
at the 4 GB test limit while building large nuclear-plant fixtures. The last
progress snapshot recorded 2,535 passes and 13 failures; later fixture activity
occurred before exhaustion, so that snapshot is not a final test total. It is
**not a passing whole-suite result**. Evidence is retained in
`build/newest-combined-server-final.log`.

The compatibility fixes listed above are now included in a fresh unfiltered run,
with a local-only 6 GB heap override (`build/newest-full-server.init.gradle`).
The machine had approximately 10 GB free before the run. Repository production
memory settings and owner launcher profiles are unchanged. The town regression
also verifies the visitor receives exactly their own three lapis back on close.

The 1,013 original-art diagnostics are now preserved in the committed-document
area (still uncommitted locally): [original-art review](../reports/newest-original-art-oct31.json).
These are heuristic geometry findings, not 1,013 confirmed visible failures.
Intentional contents surfaces/open vessels must be distinguished from real
flickering or holes before any model rewrite or narrowly reasoned exemption.
The full static art check remains failing; native data and rendered smoke passes
do not override that limitation.


### Source-aware art review — October 31

Reviewed all 24 I1 (reversed bounds) diagnostics against the preserved source.
They are intentional two-sided paper-lantern, lunch-basket handle and whirlwind
surfaces, plus Dinnerbone's two-axis 180-degree orientation. The art checker now
recognizes only those named models with **exact source element/face/UV equality**.
All 24 originals were accepted and all 24 deliberately mutated copies rejected
in the focused verification. No runtime model or source asset changed.

The initial 1,013 findings are retained in the review report, with these 24 marked
reviewed and **989 other diagnostics unresolved**. This is not a clean full-art
check or a claim that every remaining finding is a visible defect.


### Complete server sweep and final compatibility retest — October 31, 20:24 local

The 6 GB unfiltered run completed all **3,263 tests: 3,258 passed, five failed**.
All thirteen failures from the first run were resolved. The five late failures
were old expectations: 50 tomes instead of the now supplied 62; 85 glyphs instead
of 85 base plus three Ars Additions glyphs; and Holly, juniper/scrub and rainforest
leaf fixtures rejecting the registered twig drop. Corrected those exact counts
and allowed only `jugcraft:twigworks_twig` alongside the existing permitted drops.
Compilation of every operation, actual tome input reservation and leaf/sapling/
shears/fruit-conservation assertions are retained.

A focused combined rerun of those four test classes plus all four NEWEST classes
is now running **without optional integrations**, followed by local assembly.
Its test-class filter modifies only the disposable build manifest and restores
that manifest in a finalizer, including on failure. Evidence:
`build/newest-final-followup.log`. This entry does not predeclare its result.

The art audit also exposed a checker bug: explicit zero-angle Blockbench rotations
were ignored as occluders and overlay identity transforms. The checker now treats
these as unrotated. Focused negative controls still reject truly uncovered faces,
rotated occluders and differently sized overlay bounds. No runtime/source art was
changed. The first corrected full audit reported 740 remaining findings; a final
block-only audit includes the corresponding identity-overlay correction. These
source-aware improvements are not blanket exceptions for imported art.


Final art-review result: **708 unresolved block-model diagnostics** across the
38,254 distinct models checked (`build/newest-art-block-final.log`). The report
retains the original findings and the final list for comparison. Full quad and
assembly checks passed in `build/newest-art-final.log`; they were not changed by
the later block-overlay correction. No full static-art pass is claimed.


### Final focused server acceptance — October 31, 20:27 local

**PASS: all 58 required tests**, `build/newest-final-followup.log`, with
`-PjugcraftOptionalIntegrations=false`. This includes all five repaired late
expectations, the surrounding Nouveau/Holly/juniper/rainforest class tests and
all **19 NEWEST** household, fuel, mana and backpack regressions. The temporary
manifest was restored and verified against the source's complete server-class
list. Native implementation code did not change after the completed full sweep;
only these five test expectations and the offline art checker changed.

Local assembly follows in the same Gradle invocation. This is not an authorized
publication, update-channel promotion, profile installation or server restart.
Still outstanding: owner visual/playability acceptance; the explicitly absent
original source files; source-aware review of 708 static model diagnostics.
The full static art check is failing, even though runtime references, data,
combined rendered smoke tests and the server follow-up passed. Do not mark this
candidate release-certified or describe the missing originals as implemented.


### Local assembly and packaged-resource verification — October 31, final checkpoint

**BUILD SUCCESSFUL in 8m14s** for the focused absence-path server run plus
`assemble`. Local candidate: `build/libs/jugcraft-0.1.0-alpha.jar`, **346,215,956 bytes**.
SHA-256: `7a9a6ea9c3599c77638ad23b26bfa378aa332cb0ee2236a8b091378e7ec6bf23`.
This is an unreleased development artifact, not a channel/version promotion.

After Gradle fully exited, the archive passed CRC verification; all **49,612**
selected NEWEST/Ars Additions resources and descriptors matched source bytes.
All required checked implementation classes were present, test classes absent,
and before/after archive hashes matched. The earlier verification overlapped
Gradle's final ZIP rewrite and used one incorrect renderer package; its result
was rejected, corrected and repeated after build completion. Final evidence is
`build/newest-artifact-final.json`, with a stable archive and zero errors.

[Machine-readable final evidence](../reports/newest-local-validation-oct31.json)
separates passing gameplay/data checks from the still-failing static-art review
and missing original inputs. No publication, launcher installation or server
operation occurred. Owner playtesting remains the next acceptance step.

## October 31 publication preparation — owner-directed update

The owner requested publishing this batch and moving unfinished work into priority
fixes. That authorization supersedes this record's earlier local-only hold. It
is not confirmation to start/restart the hosted server. Preparation by OpenAI
Codex (GPT-6); no claim of clean CI or completed deployment is made here.

The source base was reconciled with `origin/main` f82f88c5ac5290c603f72a914b9fd933c45c2410:
the prior local HEAD had the identical tree, despite the earlier squash history.
Existing work was preserved on `feature/newest-addition-oct31` based on that main.

Publishing repair: the 152,631,297-byte building descriptor exceeded GitHub's
per-file limit. Both generation paths now write the identical JSON data compactly
(39,409,921 bytes). The prepared candidate JAR replaces only that descriptor;
188,948 other archive entries are byte-identical to the locally tested artifact.
JSON equality and ZIP CRC were verified. Candidate JAR is 341,061,226 bytes,
SHA-256 `65af2d8bced3b9c5f3004a5343c76a52f0de8af3be313ec72b143e8c58d09b2d`.
The original tested JAR is retained; this is a packaging-only resource change.
All four mixin configs and 239 declared mixin classes passed packaged validation.

Additional pre-publication Python sweep: **264 tests, 12 failures, 14 errors**,
`build/oct31-package-tests.log`. Release/updater/server-patch packaging tests
passed within that run. Six shell tests could not start Git Bash inside the
Windows sandbox; thirteen import errors require the DejaVu font directory in
the test environment. The remaining aircraft import path error was repaired by
pinning the aircraft adapter to its own original collection, independently of
the furniture collection's updated source. All three focused survival/aircraft
source tests then passed.

The other failures are recorded as priority follow-up: new explicit essence
values and conversion-cost coverage, JEI station catalog/serializer coverage,
duplicate shapeless recipes and the survival-audit registry snapshot. They have
not been suppressed. Prior passing Java/runtime and data checks do not imply
these broader Python checks passed. Required Foundation CI is not presumed green.

`docs/TODO.md` now leads with these issues, the 708 remaining static-art findings,
missing originals, owner/player acceptance and full-suite memory reliability.
Release notes describe the available content and these limitations. No new
frameworks, platform upgrades, personal Modrinth profiles or world regeneration
are part of this publication.

Host inspection found the server already offline on 20261029.1, with all ten
manual backup slots occupied. It was not stopped by this preparation. Preserve
world/configuration/player files; request final maintenance and backup handling
confirmation only after the candidate is ready. Keep the client channel on the
currently deployed version until server verification and explicit promotion.


Configured Python rerun: **290 tests, 282 passed, 8 failed, 0 errors**
(`build/oct31-python-configured.log`). Installed DejaVu fonts and running the
mocked Bash harness outside the Windows sandbox resolved the environment
failures; the aircraft source test also passes. The eight remaining failures
cover the recorded economy/catalog/survival issues plus the Encyclopedia
auditor's old `page` expectation for the new `handbook` link and two tests
reporting the same stale recipe-view catalog. No additional gameplay changes
were made to hide these findings. Repository structure/documentation links pass.
