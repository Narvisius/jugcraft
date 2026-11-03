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

## Companion stable screen (3 November 2026)

Owner request, 3 November 2026: make the pet inventory feel like a polished Minecraft companion stable or roster. Implemented with Claude Code in `client/PetInventoryScreen`. Not yet compiled or run (see Verification).

**The look.**
- A framed stable: a dark wood border with grain, a bevel and brass corner studs round a stitched leather panel.
- The title sits on a wooden plaque between two paw marks, with the companion count beside it.
- Faint paw prints mark the companion's parchment page.
- The palette is warm wood, leather and parchment. Text is cream on leather or dark ink on parchment, for contrast.

**The roster.**
- A scrolling column of cards. Each card has a small portrait (the companion's own model), its name (cut with an ellipsis when long; the full name is in the tooltip), its level, and a status pill:
  - green: **Deployed**;
  - amber: **In the stable**, or **Recovered**;
  - red: **Knocked out 0:42**, counting down, with a gentle pulse.
- **Scrolling:** the mouse wheel, eased; a brass scroll knob shows when the cards do not all fit.
- **More than 200 companions:** the server still sends 200 at a time. "Earlier companions…" and "More companions…" rows at the ends fetch the next 200.

**The companion's page.**
- A large portrait that turns to the mouse.
- Its name, its kind (the entity type's translated name) and its status.
- A **bond bar**: level and points toward the next level (`xp % 120 / 120`), "bond full" at level 20.
- A **health bar** with a heart: the live health of a deployed companion, or its health when it entered the stable.
- A line explaining what its status means.
- Four behaviour keys (Follow, Stay, Defend, Passive), the current one lit, each with a tooltip. They are disabled for Peepo companions, which keep their own Orders menu.
- One large key:
  - **Deploy**;
  - **Recall to stable**;
  - while knocked out, a disabled **Recovering 0:42** that counts down and becomes Deploy when the minute is up.

**Keys.**
- Up and Down, Page Up and Page Down, Home and End pick a companion.
- Enter deploys or recalls it.
- 1 to 4 set its behaviour.
- R refreshes; Esc closes.
- Tab and Space move to and press the other keys: the companion's keys first, then Refresh and Done. Choosing a companion updates the keys in place, so keyboard focus never jumps.
- Double-click deploys or recalls.
- Nothing needs typing.

**Fits small screens.** The panel shrinks to the smallest GUI (320 × 240, a 640 × 480 window at scale 2). All text is fitted to its space. The screen reports its panel through `ScreenKit.Fitted`.
- Tooltips wrap to the window's width.
- A status pill too narrow for its words shortens them: **Out**, **KO 0:42** (or **Ready**), **Stabled**.
- On a very short page the portrait shrinks first. If the bars still have no room, they are left out rather than drawn over the name or the portrait.

**Server unchanged in authority.**
- Every action is still a `Pets.Action` the server checks: ownership, rate, the knockout's minute, the exact stored entity, and safe deploy ground.
- The screen only shows what the server sent. A refused action comes back as the server's message on the strip above the bottom keys.
- New in `Pets.Row` (display only):
  - `health` and `maxHealth`: the live pet's, or recorded as it is stored. Older stored entries fall back to the saved entity's `Health`.
  - `recovery`: seconds left of a knockout.
  - `ownOrders`.

**Verification.**
- Not yet run: this environment cannot compile Minecraft 26.3.
- The Java was syntax-checked with tree-sitter.
- An offline layout mock (the same rectangles, with an approximate font) checked fit and no overlaps at 320 × 240, 427 × 240, 480 × 270 and 640 × 360. It is not a game screenshot.
- `PetInventoryClientTests` was updated to cover:
  - the empty stable;
  - a roster of two;
  - recall and deploy of the exact pet with the new keys;
  - setting Stay;
  - keyboard Down and Enter recalling the chosen pet;
  - a knocked-out pet whose key reads "Recovering…" and is disabled;
  - the panel fitting a 640 × 480 window;
  - screenshots at each step.
- Still to run: compilation, these client tests, and a human look at the screenshots, at GUI scales 2–4.
