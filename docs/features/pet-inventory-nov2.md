# Persistent companions — local implementation, 2 November 2026

Owner request: pause November joke events; add automatic tamed-pet registration, persistent pet inventory, knockout recovery, recall, following, abilities and leveling. Prior festival and board-game work is preserved. Nothing committed, published, installed into a personal profile or deployed.

## Implemented
- World SavedData roster with full entity snapshots. Recall/resummon preserves UUID, name, equipment, variant, ownership, health and custom entity data.
- Pets inventory button and `/pets`: portrait, status, bond level, recall/summon, mode selection, refresh and paginated roster.
- Permanent owned mobs enroll automatically: vanilla tamables/horses and custom Menagerie, Chima, Pumpkling and Peepo adapters.
- Lethal damage becomes knockout before normal death drops. Recovery takes 60 seconds of server game time and restores half health. Recall stores pets without killing them.
- Unloaded recall queues until the original entity loads. Never force-load chunks or create another pet from an active stale snapshot. Incarnation checks reject stale entity copies.
- Follow, Stay, Defend and Passive modes; native navigation, safe catch-up positions, full-body fluid checks for aquatic pets and claim-aware destinations. Peepo retains its native work orders.
- Bond levels 1–20 through traveling together. Persistent health, damage and pace bonuses; out-of-combat recovery and level-5 defender threat warnings. Stable modifier IDs prevent stacking.
- Server-side ownership, request throttling and attack permission checks. Temporary summons excluded. Custom pet transport crates enforce ownership. Known horse/mutation conversions transfer progression.
- Claude supplied a bounded movement helper and ownership audit; Codex integrated and reviewed these, corrected persistence/recovery behavior, and validated locally.

## Validation
- Common, client and test source compilation passed.
- Final focused server run: **all 84 required tests passed**, Gradle BUILD SUCCESSFUL. Evidence: `build/pets-review/server-final.log`.
- Disposable client walkthrough exercised actual roster UI, recall, same-UUID summon, Stay and compact-window layout. Screenshots: `build/pets-review/0000_pets_active_roster.png` through `0003_pets_compact.png`.
- Earlier combined run had a server fixture failure (mock players always report Creative, then test zombies burned in daylight). Corrected fixtures and reran the server suite successfully; the client walkthrough in that combined run completed. Do not describe its overall Gradle invocation as passing.
- Persistence checked with SavedData codec and entity NBT round-trips. No real crash/restart recovery or human multiplayer stress test claimed.

## Boundaries and follow-up
- Temporary summoned creatures, independently respawning bound familiars and workshop workers retain their existing lifecycles. Wild trust relationships without a single tame owner are not roster pets.
- Queued unloaded recalls require the real pet chunk to load. They are not instantaneous remote recovery.
- Storage currently closes nearby open containers conservatively to prevent stale entity-inventory interactions; narrow this to exact pet menus in a subsequent polish pass.
- Human visual/multiplayer playtest remains before publication. No personal playtest was controlled and no live server was touched.
