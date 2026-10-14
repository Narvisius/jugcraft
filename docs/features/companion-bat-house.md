# Companion Bat House collection

Owner-requested implementation on 12 October 2026 with OpenAI Codex (GPT-6). Uses existing Planner assignments, CompanionJob scheduling, physical eight-slot cargo, harvest provenance and linked Output. No new assets, dependencies, packets, registries, production ticker or collection inventory.

## Player behavior and progression

Assign a Bat House in normal Planner mode. Each house takes one work slot. Provide reachable safe ground in front of or beside it (same level or one below), link Output, and put the companion in Work mode. Supply is marked unsupported; no tool is needed. The existing two-arm interaction clip plays for forty ticks at four reserve JE per tick (160 JE per completed uninterrupted scoop), plus normal load-based delivery effort.

The entire currently available batch, at most sixteen native guano, goes directly into companion cargo. Output exports only marked collected goods, never unrelated player-given stacks. Full cargo leaves the ledge unchanged. Full or missing Output retains goods in cargo. Empty houses report Waiting for guano and yield to other jobs. Collection does not release bats, change residents, summon bats, accelerate dawn/dusk production, grant player achievements or fertilize crops.

This is early farming logistics: planks/stick and resident bats supply the house; guano feeds native fertilizer, composting and phosphate recipes. The existing collection and growth economy is unchanged. Maintenance remains deferred.

## Implementation and multiplayer

The block entity owns one ephemeral claimed job. A worker UUID and hundred-tick renewable lease prevent concurrent companion actions; expired/dead workers and removal clear the claim. Positions use at most six candidates (front and sides, on two levels), with one-second planning refreshes, hazard/collision checks and a short unobstructed reach. Failed path approaches are excluded through the existing navigation memory. No new entity or area scans are added; the house's existing bat simulation is unchanged.

Claims, completion and delivery recheck loaded/current blocks, assignments and existing companion permissions. Feature disablement and mob griefing prevent new collection. Town protection, work priorities, energy recovery, meals and settings-menu pause remain native companion behavior. This nonmagical job does not inherit Concordance's owner-presence/research rules.

Cargo room planning is cached for at most twenty ticks per companion and count; completion always probes live capacity. A transactional guano extraction snapshots only the real guano count; native block/client updates happen on final commit. Cargo, provenance, final effort and guano extraction commit together. A failed or rolled-back scoop creates no loose drops and loses no guano. Manual harvests/dawn changes are read again before taking the current batch. Native manual scooping and block-break drops continue to operate on the same count.

No new saved fields are needed. The house already saves its guano and resident bats; existing companion cargo/provenance saves preserve collected goods and transport manifests. Interrupted partial work restarts and prior effort is not refunded. Removing an assignment leaves physical cargo intact, following existing harvest behavior.

## Validation

Automated tests and gameplay were not run, following the owner's latest instruction. Common/client compilation and `assemble` passed in 52 seconds (`build/bat-house-build.log`), with existing deprecation/annotation warnings. The complete Modrinth distribution was rebuilt using the same eighteen locked libraries and platform pins. Its embedded JAR matches the final build and contains the new job class; its SHA-512 sidecar matches the pack. `git diff --check` passed. No game launch, pack import or multiplayer playtest was performed.

Manual checks remaining: Peepo and Jughead; front/side/one-block-below access; blocked or high houses; no guano, partial and full batches; full cargo and Output; two companions and simultaneous manual scooping; house removal; save/reload while hauling; stopped work, sleeping and low energy; disabled mob griefing/agriculture and protected locations. Neither compilation nor pack generation confirms these gameplay cases.
