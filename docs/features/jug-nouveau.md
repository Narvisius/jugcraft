# Jug Nouveau integration

Owner request: 18 October 2026. Implementation: OpenAI Codex. Local branch:
`local/jug-nouveau-oct18`, with base commit `b418263b1c0bb3c88aafc63ae1c4d0ce08214d3e` and uncommitted local changes. No publication is authorized by
this implementation record. Existing local technology and town work is preserved.

## Source and scope

The owner's latest October 18 instruction expands the active implementation to
**every part of the core `ars_jimbaux` collection**, with one validation phase
after all implementation. It overrides the earlier incremental checkpoint plan.
The implementation pass is complete. Validation began only after all core
families were implemented. Historical results below apply only to the two earlier
helper integrations; the full-port validation record is separate.

The supplied name in the archive is `ars_jimbaux`. Its complete source is already
preserved in the owner library: **2,210 asset files and 1,397 data files**. The
[full manifest](jug-nouveau-source-manifest.json) records all 3,607 paths and their
catalogued SHA-256 hashes, with the runtime destinations used by this port.
Earlier imports have their own feature records and owner-art manifest entries.

The collection has textures, geometry, animations, audio, models, recipes, loot,
tags, world generation and other data. It does **not** contain Java/Kotlin gameplay
source. Two compiled owner archives were subsequently found in Downloads and
inspected without running them. The [archive cross-check](../../art/owner-library/ports/jug-nouveau/README.md)
accounts for all **4,847 resource entries** from core and elemental together:
4,819 already matched the library byte-for-byte, and 28 conflicting elemental
variants were preserved separately without overwriting the original upload.
Original metadata/notices are also preserved. The archives contain compiled
NeoForge classes, including embedded dependencies, not a Fabric 26.3 source
project. Old names, recipe serializers, shaders and NeoForge
data are not a working Fabric implementation. Preserve them in the library;
port their behavior and references into Jugcraft when their feature is wired.

Use the supplied art directly. Do not redraw, recolor or rename it just to make
it different. Preserve originals and notices. Technical runtime adaptations
(Jugcraft IDs, supported data schemas, renderer paths) are expected. Do not run
programs from the source library. See [owner authorization](../../art/owner-library/MAGIC_ASSETS.md).

## Full core implementation pass (local)

The following code and adapters implement the complete core collection locally.
The table records scope; validation evidence is listed separately below.
The end-of-pass audit found generated definitions for all 244 standalone items,
194 block definitions and converted outputs for all 709 supplied recipes, with
no unclassified core source paths. These counts do not by themselves establish
exact legacy behavior or a completed human playtest.
Original resources remain preserved. Fabric 26.3 code replaces the old loader;
there is no embedded legacy mod or separate mana/inventory authority system.

| Content | Implementation |
| --- | --- |
| Storage | Searchable Book Wyrm catalog, real crafting grid, withdrawals/deposits, named inventory groups, 30-block links, remote lecterns with bounded chain resolution, redstone-controlled adjacent imports, repositories/controllers, native single/double Archwood chests |
| Workers | Starbuncle spawning/token exchange, filters, multiple routes, gathering/berries, beds, path preferences, saved multi-stack cargo and stacked animated models, accessories and special appearances; Drygmy, Whirlisprig, Wixie, Amethyst Golem and Alakarkinos jobs |
| Familiars and encounters | All supplied familiar types and effects, binding, Wilden families and Chimera, original spike models, four Weald Walker/Waddler families and awakening |
| Workshops | Passive starter imbuement accelerated by Ley Charge, enchanting, glyph creation, fifty supplied preset tomes, alteration/armor/thread upgrades; matching source recipes with previews, recipe selection/lock, charge and output reservation |
| Source and devices | Five Sourcelinks, jars, collector/deposit/splitter/warp relays, six-direction prisms, sensors/detectors, redstone relays, turrets/timers, runes and brazier relays |
| Spellcraft | Complete supplied glyph/augment vocabulary in the constellation composer; projectiles/orbits/fields, sequence delay/reset/rewind, summons, physical tools, elemental effects, infusion, bubble/pop artwork, real fireworks and scrying markers |
| Equipment | Three armor families, upgrades and threads, books, sword/shield/bows/rod/gauntlet/mirror/eye, trinkets, arrows, utility items, original models and clips |
| Potions and rituals | Native potion/brewing families, jars, melding/diffusion, refillable flasks/cannons/crown; all ritual tablets, augments, world changes, creature containment and safe release |
| Travel and building | Warp scrolls/portals, remote scrying, named Planarium cells/miniatures, illusion blocks and sky windows |
| Natural/decorative content | Four Archwood families and fruit, Sourceberries/Magebloom and five native potted plants, masonry/decorations, editable signs, lamps/sconces/torches, beds/plushes, paintings, banners, discs and original structure templates |
| Data and guidance | Native recipe/loot/worldgen adapters, advancement event hooks, shared tags and Trinkets slots, translated language keys, supplied audio/particles, overview topics and searchable source references |

Implementation bounds are deliberate: loaded-container access only, eight routes
and four carried stacks per Starbuncle, 16 lectern hops with cycle detection,
128 containers per terminal, and shared spell ledgers/permissions. Teleportation
can load the explicit destination; helpers and storage do not load chunks.
Source costs use existing Ley Charge at 100:1; personal spells use existing Focus.
Imbuement works without charge at one tenth of its powered speed so Source Gems
do not depend on a generator that already needs those gems. All fifty supplied
tome presets are selectable at the Scribe’s Table for a book, paper and a Source
Gem. Their server-owned sequences retain repeated augments and nested delivery
shapes within 64 distinct targets, 384 work, twelve stages, six-block effect radii,
and two-minute individual effect limits. They do not change the player composer’s
global limits. Supplied archaeology weights, scrying tag variants, bat summoning
and Blaze/Witch dispels are connected to their native gameplay handlers.

The source crystallizer blockstate has no matching supplied model or recipe; its
legacy reference maps to the Imbuement Chamber, rather than registering an inert
extra machine. Original files are unchanged.
Owning equipment is sufficient to use it. The original English field reference
is retained alongside current controls; legacy shaders and loader hooks have
native render/event equivalents rather than being executed unchanged.

## Full-port validation and local playtest

Verified on 18 October 2026 with Minecraft 26.3, Fabric Loader 0.19.5, Java
25.0.4.1 and the unchanged 18-library lock, including GeckoLib 5.5.7:

- Full build: **all 1,960 required server game tests passed**, including fifteen
  expansion tests and the earlier Book Wyrm/Starbuncle regression suites.
- The three client suites, `NouveauExpansionClientGameTests`,
  `JugNouveauClientGameTests` and `StarbuncleClientGameTests`, passed in disposable
  automated worlds with optional integrations present and again with them absent.
  The final package was assembled with the final resource fixes before the second
  client run. Production common code was unchanged from the passing server run.
- Complete resource generation passed. The final data/recipe/art audit passed:
  3,444 material IDs, 8,608 block/item models, 3,709 quad parts and 171 models
  drawn together. The focused Nouveau check validates 424 model sprite bindings,
  particle atlas entries, animation definitions and Gecko 5 logical IDs.
- Owner-art byte comparison, repository/documentation checks and pack-lock
  validation passed. Original supplied textures, audio and models remain in the
  owner library; runtime schema/reference adaptations are recorded separately.
- Inspected rendered workshop galleries, inventory pages, creature models,
  recipe selection/preview/lock and the synchronized Book Wyrm storage catalog.
  Both new menus fit the tested 854×480 display at GUI scale 2.

Combined validation caught and fixed recipe parsing of entity-shaped Source
inputs, empty tome sound objects, guide-page schemas, generated armor tags, XP
gem stack conservation and stale test expectations. Rendering fixes corrected
Gecko 5 IDs, atlas inclusion, legacy texture references and animation variables;
Archwood diagonal fence connections were restored after model generation.

The local package is `work/playtests/jug-nouveau-complete-oct18/` in the task
workspace. Its README gives import and exploration instructions. BUILD-INFO.json
records the base commit, uncommitted status, tested JAR hash, dependency pins and
validation evidence. The .mrpack embeds that exact JAR. Screenshots and a Gradle
sources artifact are alongside it. No commit, push or merge was made.

Still unverified: importing this final package in Modrinth, owner-controlled
survival/visual feedback, independent multiplayer, restart/chunk-boundary field
testing, and measured large-workshop performance. The automated suites exercise
representative integration and conservation behavior; they do not establish exact
legacy parity or exhaustive human playtesting of every combination.

Separate elemental/companion collections remain outside this core port. The
historical results below apply only to the earlier helper slices.

## Historical helper-only checkpoints

### First player outcome: Book Wyrm storage

This first integration adds a craftable Storage Lectern, Book Wyrm Charm and
Dominion Wand. Book Wyrms use the supplied geometry, flight animation and six
textures. The lectern uses the supplied geometry, texture and floating-ledger
animation for its placed and item renderers. They belong to the existing
Concordance feature switch and authority/party system; there is no second mod,
currency or mandatory research gate.

1. Craft and place the Storage Lectern (book, two amethyst shards, vanilla
   lectern and gold ingot).
2. Craft a Book Wyrm Charm (book, two amethyst shards and two gold ingots) and use
   it on the lectern. Each wyrm allows eight links, up to four wyrms / 32 links.
3. Craft a Dominion Wand (amethyst shard, stick and gold nugget). Use it on the
   lectern, then on ordinary chests or barrels within 16 blocks.
4. Use the lectern empty-handed. Previous/Next selects a chest; ordinary clicks
   and shift-click transfer its real items. Put down cursor-held items before
   changing pages. Unlink removes the connection, not the contents.
5. Dye a wyrm black, blue, green, purple, red or white. Its color persists.

This is a first storage integration, not complete parity with the older mod's
storage/crafting system or its Book Wyrm familiar. Aggregated search, crafting
requests, physical item-delivery jobs and familiar effects remain port work.

### Second player outcome: Starbuncle delivery

The owner confirmed: **use the files already in the repository**. This chunk,
implemented with OpenAI Codex, adds a complete chest-to-chest helper locally on
top of `4768ff667`. An October 18 read of all 34 open PR titles/branches found no
Jug Nouveau/Starbuncle contribution. Existing local packages remain intact.

- **Acquisition and setup:** craft a Starbuncle Charm with three gold ingots,
  amethyst and sugar; use it on clear ground. Use the Dominion Wand on the
  Starbuncle, then a pickup chest/barrel, then a delivery chest/barrel within
  32 blocks of the first. No research prerequisite. Selecting a lectern switches
  the wand back to Book Wyrm storage; sneak-use a chest restarts route selection.
- **Connected workshop:** transports up to 16 items of one kind per trip between
  real vanilla containers. Technology or magic workshops may feed those chests
  with their existing automation. No new energy, currency or upkeep. This is a
  short-range creature alternative, not a replacement for item pipes or the
  powered Clockwork Porter's courier-post request service.
- **Shared systems:** extends `WorkerEntity` for SmartBrainLib navigation,
  once-per-second staggered decisions, failure statuses, signs and worker roster;
  reuses the existing route codec/component, Authority, parties and RateGate.
  `NouveauStorage` provides the same locked-chest/vanilla-storage boundary for
  both Book Wyrms and Starbuncles. No arbitrary mod-private inventory access.
- **Presentation:** original Starbuncle geometry, idle/run animation file,
  sixteen color textures and charm icon (19 additional unchanged source files).
  GeckoLib owns the posing; a display-only synced item appears on the model's
  existing item bone over its basket. Dyeing and packing retain the color.
  Runtime paths, registration and rendering code are technical adaptations;
  source designs and originals remain unchanged.
- **Failure/recovery:** full, locked, removed, newly protected or unloaded
  destinations stop transfers and keep cargo. Owner absence pauses work by
  default, using the existing server option if enabled. Cargo is removed only
  at pickup, stored once on the entity, and inserted only upon physical arrival.
  Assignment is refused while carrying. Sneak-use empty-handed returns the
  color-preserving charm and cargo as drops; destruction also refunds them once.
  Unloading does not drop anything. No chunk tickets or world-wide searches.
- **Bounds and saves:** eight Starbuncles per owner, including unloaded entries,
  within the shared 32-worker roster. One stack up to 16 items, two endpoints,
  32-block endpoint separation and at most 27 slots examined per decision.
  Owner, color, route and full item components persist with the entity; synced
  cargo is rendering data only. Existing IDs/saves keep their meaning.

The original charm recipe uses an enchanting apparatus and Starbuncle shards.
That source recipe is preserved unchanged; the reachable crafting recipe above
is an explicit bridge until its workshop and acquisition are ported. Natural
spawning/taming, loose-item gathering, filters/multiple routes, accessories,
special cosmetic models, familiar effects and courier-post jobs remain separate
port work. This is not full original-mod parity.

Validation on October 18:

- `gradlew.bat build runClientGameTest
  -PclientTests=JugNouveauClientGameTests,StarbuncleClientGameTests --offline`:
  **all 1,945 required server game tests passed**, and both client suites passed.
  The eight new server cases cover ordinary charm/wand clicks without research,
  real concurrent cargo, full/locked storage, save/load, invalid routes/owners,
  recovery and roster caps. The client actually navigated and delivered two
  16-item loads, and rendered all sixteen colors and the existing storage UI.
- `tools/check_mod_data.py`: passed (3,025 material IDs, recipe/data checks and
  the model/owner-art audits). All **34** Jug Nouveau runtime resource files also
  match their source bytes in a direct comparison.
- `scripts/check_repository.py`, `scripts/package_modrinth.py check` and
  `git diff --check`: passed. Player-facing codex text matches its generator.
- Final targeted `StarbuncleClientGameTests` rerun and JAR/source-JAR rebuild
  passed after the status/codex wording cleanup. The additional close-up asserts
  real cargo synchronization; its screenshot shows the diamond on the supplied
  basket item bone. No gameplay logic changed after the full server run.
- Packaged locally as `jugcraft-complete-nouveau-couriers-oct18.mrpack`, alongside
  instructions, screenshots and a source/hash record. The pack retains the
  existing Book Wyrm feature and other local branch content. Nothing pushed.

These are independent disposable automated worlds; no owner's Modrinth session
was operated. Human survival balance, independent multiplayer, full restart and
unloaded-boundary field testing, and measured large-workshop performance remain
unverified. The first port's broader release requirements below still apply.
The menu shows one real linked container at a time. Double-chest halves are
separate links. Wyrms fly between their home and loaded links; they are not a
second item-transport inventory. Existing delivery workers remain separate.
The Binding section of the Concordance encyclopedia includes the complete
crafting and use guide without a research condition.

## Authority, persistence and bounds

- The owner and their current party may use the lectern, subject to the existing
  change-permission checks on the lectern and each chest.
- Locked containers must be unlocked directly before linking/access; Ender
  chests, furnaces and arbitrary mod inventories are excluded.
- No chest stacks are copied into a persistent network inventory. Every access
  resolves the real container, so a removed, unloaded or newly protected chest
  cannot leave a withdrawable cache behind.
- Opening, linking and page actions are rate limited. Menu access ends when the
  player walks away, dies, becomes a spectator or loses authorization.
- No chunk tickets or world searches. Home/link checks occur once per second
  per wyrm; each network has at most four wyrms and 32 links within 16 blocks.
- Save schema 1 keeps owner, linked positions, bound entity UUIDs and colors.
  Load deduplicates and bounds lists. Losing a wyrm disables excess links without
  deleting storage. Breaking the lectern returns bound charms and removes loaded
  wyrms; an unloaded orphan discards itself when it sees its home is gone.
- Disabling Concordance stops access/flight; registrations and saved inventories
  remain. Do not remove the mod from a world containing its entities and blocks.

## Historical helper-only planning (superseded by the full pass above)

This table records the earlier helper-only checkpoint. Its core families have
since been implemented and exercised by combined validation in the full pass above.
Separate companion collections remain outside this core-only request.

| Family | Supplied examples | Integration work still required |
|---|---|---|
| Other helpers | Starbuncle, Drygmy, Whirlisprig, Wixie, Amethyst Golem | Ownership, jobs, bounded automation, animations, acquisition and interfaces; reuse existing worker authority and logistics |
| Enchanting and crafting | Enchanting/alteration apparatus, imbuement chamber, glyph press, scribes table | Supported recipe serializers, work inventories, previews, animation states and existing Concordance connections |
| Potions | Potion melder/stirrer/diffuser and flask cannons | Potion component preservation, recipes, costs, claims and usable controls |
| Source infrastructure | Jars, sourcelinks, relays, splitters, collectors/deposits/warps | Translate into existing magical power systems, transfer limits and diagnostics; avoid a duplicate currency |
| Spell devices | Turrets, timers, mirrors, prisms, runes and lenses | Connect to the existing spell compiler, payment, claims, PvP rules and bounded targets |
| Flora and building | Archwood sets, magebloom/manabloom, archfruit, sourcestone and decorations | Survival routes, crop/tree lifecycle, full block variants, loot and compatible world generation |
| Gear and familiars | Armor, tools, bows/crossbows, foci, familiar items and threads | Map existing gear first, then missing equipment behavior, animation, attachments and clear controls |
| Creatures and encounters | Alakarkinos, Weald Walkers/Waddlers, Wilden mobs and Chimera | AI, encounter spawning, attacks/telegraphs, reward eligibility, loot and persistence |
| Glyphs, rituals and realms | Glyph recipes, ritual data, structures and dimension metadata | Map to existing Concordance operations; implement missing effects and safe travel rather than load foreign serializers |
| Companion collections | `ars_jymbaumental`, `ars_nouveau` and cross-mod data | Audit dependencies against the core port; keep unrelated library collections distinct |

## Historical helper-only validation

Verified locally on 18 October 2026 with Minecraft 26.3, Fabric Loader 0.19.5,
Java 25.0.4.1 and the unchanged 18-library lock (including GeckoLib 5.5.7):

- `gradlew.bat build`: **all 1,937 required server game tests passed**, including
  eight new Book Wyrm tests; common, client and game-test compilation passed.
- `gradlew.bat runClientGameTest -PclientTests=JugNouveauClientGameTests`:
  passed in an independent disposable world. Inspected screenshots of the
  lectern/item renderer, animated wyrms in six colors, live linked contents,
  next-chest navigation and GUI scales 1/2. Keyboard focus is exercised.
- `python -X utf8 tools/generate_material_data.py`: completed. The generated
  sound-map key order was retained for CI consistency; sound definitions are
  semantically unchanged.
- `python -X utf8 tools/check_mod_data.py`: passed, including 3,024 material IDs,
  recipe/data checks, 8,522 block/item models and the owner-art checks.
- `python -X utf8 tools/owner_art.py --check`, `scripts/check_repository.py`,
  `scripts/package_modrinth.py check`, and `git diff --check`: passed.
- All **4,870** preserved archive entries (4,847 resources and 23 metadata,
  notice/logo/configuration files) match their recorded size and SHA-256.

During implementation, API and recipe-condition mismatches were corrected.
The locked-chest fixture was changed to preserve the actual inventory while
setting its lock. The gallery fixture was fixed to keep its camera on solid
ground. The final successful runs include those corrections. Test logs/worlds
are local build outputs, not committed player data.

`JugNouveauGameTests` covers ownership/capacity, real-container withdrawals,
unlinking and capacity loss, invalid targets, saved identities/colors and menu
invalidation, ordinary wand/charm clicks, survival-recipe loading and immediate
lock revocation. `JugNouveauClientGameTests` uses a separate disposable automated
test world for models, colors, live inventory sync, page switching and GUI scales.
It never operates an owner's Modrinth playtest.

Still required before release: owner visual/survival feedback, two independent
players exercising simultaneous access, restart/chunk-boundary field testing
and measured performance at a stated network count. Automated single-client
checks must not be described as independent multiplayer testing.
