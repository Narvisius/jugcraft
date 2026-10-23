JUGCRAFT town maintenance and Windows updater reliability update.

- Visitors can use Spawn City doors, trapdoors, gates, controls, seats and public workstations. Shared containers and stored resources remain protected; the bank Ender Chest remains personal and usable. Building and breaking protections remain active.
- Sleeping townsfolk stop walking and working until they wake.
- City flak selects reachable direct shots instead of firing near-vertical fallback shells at nearby ground targets.
- Guard walkers animate their movement and show a seated uniformed guard in the cockpit.
- The updater waits for Windows to release Minecraft's file lock after shutdown, with bounded retries, diagnostics, second-game protection and existing rollback/recovery.

This release preserves existing towns and worlds. No biome reset, new registry entries, platform change or dependency update is included. The separate aircraft, graves, furniture and Nether repair work is not in this release.

Existing players: accept the update in JUGCRAFT's title-menu updater, apply and quit, wait for it to finish, then relaunch the same Modrinth profile. Personal settings, saves and extra mods stay in place. Old clients stuck in the lock-error loop need the accompanying recovery ZIP once; follow READ-ME-FIRST.txt and do not import another profile.

The isolated candidate compiled and passed 17 town/defense server tests. The merged updater helper passed 79 Windows/Java 25 assertions. The release workflow requires Build and Foundation success on this exact main commit. Actual Modrinth apply/quit/relaunch was blocked by desktop control failures; the owner explicitly authorized rollout without it and will perform that acceptance check. Automated checks are not described as a completed launcher playtest.

Server files are updated directly after a fresh backup, preserving world/config/player data. Only after startup verification is the matching client channel promoted. Publication alone does not activate the update.

AI assistance: OpenAI Codex (GPT-6).
