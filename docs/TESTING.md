# Testing and release evidence

## Current foundation

Run `python scripts/check_repository.py` with Python 3.11+. The Foundation / repository job checks required files, relative Markdown links, and the phase declaration. It is not a Java compiler, mod test, security audit, or gameplay approval. In the `bootstrap` phase, Java/Gradle sources are allowed. The Build workflow compiles the mod (`./gradlew build`), checks generated JSON is current and runs `tools/check_mod_data.py`, which validates material data and audits recipes offline. None of these is a game test.

## Gameplay PR evidence after bootstrap

Record commit SHA, exact client/server/dependency versions, test world origin, commands, observed results, and test date. Do not commit worlds or logs with player information; attach redacted evidence when needed.

- Build and relevant unit/game tests.
- Dedicated server startup and two independent clients joining.
- Actual survival acquisition path, recipes, progression locks, and automation.
- Concurrent use by two players, permissions, disconnect/reconnect, save and restart.
- Existing systems interacting with the new feature; dependency present/absent for optional integrations.
- Invalid requests, repeated actions, full inventories, interrupted crafts, chunk unloads, and duplicate reward attempts where relevant.
- Before/after performance under a stated workload: machine count, players, hardware, median/p95 tick time and memory observations. Agree per-feature limits before merge; do not claim support for an untested player count.
- Upgrade from the prior released world version and safe feature-disable behavior where persisted content changes.

## Content-specific scenarios

Use the cases relevant to the feature; do not claim a scenario was run just because it appears here.

- Progression: start each affected specialty without advanced goods from itself; test required/optional connections, trade and documented solo routes.
- Agriculture: growth and yield loops, climate conditions, harvest automation, livestock population and outputs feeding both industry and magic.
- Biomes/dungeons: fresh-seed distribution, sensible transitions, old-chunk behavior, hazard signals, spawn caps and encounter containment.
- Pets/bosses: ownership, concurrent interaction, friendly fire, unload/reload, despawn/recovery, summon permissions and loot eligibility.
- Loot/spells: reward weights, duplicate handling, stacking/cooldowns, ability persistence, permission checks and conversion/healing feedback loops.
- Rockets/portals: outbound and return travel, invalid destinations, disconnect during transfer, destination access, stranded-player recovery and dimension/chunk budgets.
- Seasons: server timezone/manual override, activation/deactivation and restart at the boundary, reward replay prevention, and preservation of earned content and occupied destinations.

## Release process

Maintainers test the combined candidate, not just individual PRs, on an isolated staging server. Publish a numbered release with checksums, exact requirements, changelog, known issues, and migration notes. Promote only explicitly approved artifacts. Never automatically deploy arbitrary main or PR builds to the viewer server.

Back up world, player data, configs, and exact previous artifacts before upgrading. Test restoration. If migration is not reversible, restoring the matching backup and old artifacts is the rollback; merely reverting a source commit or replacing a JAR may not restore the world.
