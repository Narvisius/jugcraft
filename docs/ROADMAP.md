# Roadmap

The [Concordance expansion planning pack](plans/concordance-expansion-2026-10-10/START-HERE.md) develops magic, bosses, workshops, pacts and authored worlds into researched designs and substantial Claude implementation chunks with owner checkpoints. It distinguishes current source, existing owner direction and new proposals.

The broad vision is approved as direction, not as a promise that all content is available or a fixed delivery schedule. See the feature documents and current PRs for implementation status.

Concrete owner-requested follow-ups are tracked in the [TODO list](TODO.md), including the independently documented [Jugcraft Encyclopedia](features/jugcraft-encyclopedia.md) and its ten UI reference images. The Encyclopedia's approved direction is a full UI with technology/magic route explanations, detailed pathway guidance and quests, accessed from the inventory or a keybind without an item.

| Milestone | Deliverable | Exit evidence |
| --- | --- | --- |
| 0 — Contribution foundation | Repository, proposals, shared design, checks and review protection | Published files and verified GitHub settings; established |
| 1 — Playable platform | Minecraft 26.3 + Fabric, license, pinned toolchain, minimal mod | Build checks, two-client dedicated-server join and save/restart |
| 2 — Shared systems | Materials/tags, recipes, bounded resource handling, unlocks, ownership and persistence | Tested contracts that multiple small features can use |
| 3 — First homestead and workshops | A useful crop, simple processing machine, starter magical craft, shared material and cozy building content | Independently reachable entry paths plus one useful cross-branch interaction |
| 4 — Specialty pilot | Small industrial/agricultural/magical branches and friendly creature content | Contributor PRs integrate without incompatible currencies/APIs; measured workload |
| 5 — Underground adventure | One dangerous cave theme, compact dungeon, enemy/boss encounter and curated rare loot | Readable danger, controlled spawning, loot balance and multiplayer evidence |
| 6 — First frontiers | One meaningful magical realm and one rocket/moon expedition in separate increments | Safe outward/return travel, bounded dimension cost, useful downstream rewards |
| 7 — Expanding worlds | More schools, farming revolutions, planets/moons, galactic exploration and community infrastructure | Distinct destinations/specialties, save compatibility and combined staging tests |

Seasonal delivery runs alongside suitable milestones: prioritize Halloween, then Christmas-esque December content. Scope events to available foundations; a small coherent crop/decor/encounter release is preferable to rushing new dimension, boss and magic frameworks at once. Events must not gate necessary progression or erase earned content after their dates.

Before a viewer release, verify permissions/claims, combined progression, realistic factory/player load, backups and restoration, installation/update instructions and a numbered release candidate on staging.

## Initial proposal board

### Owner-requested TODO: Ars Goetia summoning and pacts

- [ ] Add a future summoning and pact system covering **all 72 spirits of the Ars Goetia, including Stolas** (owner request, 5 October 2026).
- [ ] Give each spirit its own appearance, personality, summoning requirements, and clearly stated pact benefits, costs, limits, duration, and exit/breach rules. Players must knowingly accept pact terms; another player cannot bind them without consent.
- [ ] Start the first pilot with Stolas, connecting botanical knowledge, astronomy, and minerals to [Styxhexenhammer's conservatory](features/styxhexenhammer.md); expand through a shared, data-driven roster instead of 72 incompatible systems.
- [ ] Connect later flower uses to existing agriculture, materials, crafting and magic. Keep the current decorative flowers usable before pacts exist.
- [ ] Design server authority, ownership, protected-area rules, encounter bounds, concurrent summons, entity caps, restart/unload recovery, and save compatibility before implementation.

This is future work. Styxhexenhammer's initial release does not implement summoning or pacts.

These are suggested issue-sized briefs, not assigned or implementation-approved work:

- Shared material plan: one resource with distinct mechanical and magical uses.
- Starter crop/food/reagent loop feeding a workshop and a factory.
- Idle-safe processing machine using the shared recipe/resource interfaces.
- Starter magical instrument and one crafting or transport spell.
- Cozy regional biome brief with ecological transitions and useful building materials.
- Friendly companion brief with ownership, persistence and population limits.
- First cave/dungeon loop with telegraphed danger and a bounded reward pool.
- Halloween feature brief reusing permanent systems; December counterpart separately.
- One school brief chosen from Fire, Ice, Storm, Earth, Necromancy, Blood, Vampirism or Cursing.
- One realm brief and one moon/rocket brief, including safe return travel and material uses.

Use the feature proposal form for discussion, or start one focused prototype in your fork and open a draft PR. Prior issue approval is not required for that prototype. Coordinate major shared-API or platform changes first; maintainers decide acceptance during review. Do not submit a single PR implementing the entire roadmap.
