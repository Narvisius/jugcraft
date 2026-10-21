# COPY INTO CHATGPT WORK | COLLEGE OF WINTERHOLD GRAND ACADEMY

You are BUILDING a feature in https://github.com/jimbozoomer-byte/jugcraft, not just discussing a concept. Treat the attached `winterhold_grand_academy_plan.zip` as approved **design intent**, not proof that code is already implemented. Inspect `00_REPOSITORY_BASELINE.md`, the layout data, room schedule and all documents.

## Recover before rebuilding
My Oct 20, 2026 Work session repeatedly failed and the conversation vanished. It previously *reported* an academy prototype, five passing offline tests, an inspected layout preview, and unrun Java-dependent tests. These are **unverified reports**. Search main, ALL relevant branches, commits, draft/open/closed PRs, current runtime and accessible artifacts for `winterhold`, `college`, `academy`, tower/structure and test files. Do not assert recovery without locating actual commits/files. Do not lose work by waiting until the end to push.

## Inspect the real codebase
Read `CLAUDE.md`, `AGENTS.md`, `CONTRIBUTING.md`, `README.md`, `docs/WHAT_EXISTS.md`, `docs/PLATFORM.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN.md`, `docs/TESTING.md`, `docs/FRAMEWORKS.md`, `docs/ARCANE_CONCORDANCE.md`, `docs/WORLD_DESIGNER.md`, `docs/CONTENT_BRANCHES.md`, the Concordance expansion plan and actual source/tests/assets. Check current open PRs and note APIs unavailable on main. Respect repo AI-contribution requirements, no surprise version changes, no merges to main, no release. Honor asset provenance and technical constraints.

## Creative objective (locked)
Build a **grand, believable, functional magical academy** inspired by Skyrim's College of Winterhold, not a strict 1:1 copy and not a generic Hogwarts-style castle. Keep the recognizably narrow cliff bridge, stone entrance, circular open courtyard, dominant Hall of the Elements, ring of towers, Arcanaeum library, student and faculty residences, elevated Arch-Mage suite, and concealed Midden dungeon. Increase internal scale and variety while retaining the exterior silhouette and readable circulation. Snow and faint volumetric-looking fog should be a **localized magical microclimate** even in desert, jungle and plains, without globally changing weather or neighboring biomes. Every named room must have a meaningful interaction or discovery. Design supports multiplayer and does not require every player to master all magic branches.

## Geometry/placement
Use `data/layout.json`, `data/rooms.csv`, `data/connections.csv` as a coordinate-independent planning graph plus proposed dimensions. Establish a sensible Minecraft coordinate frame and translate the design into reproducible templates/pieces and supported generation code. Reconcile structure height, cliff foundations, overhangs, entry bridge alignment, terrain fitting and vanilla limitations. No inaccessible doors, blank tower shells, impossible stairs or rooms with no routes. Prefer geometry built of existing Jugcraft/vanilla assets; add a focused palette only if needed. Test walkable connectivity and build collision. Give screenshots with overhead, entrance and interior perspectives.

## Magic integration
Reuse the Arcane Concordance's Knowledge, Focus, research, spell instruments, Modonomicon and Spell Engine contracts. Do NOT invent a mana bar or parallel spell progression. Integrate instructors as discoverable presentation and legitimate research/workstation interactions. Where a tradition is not implemented, design a credible instructive NPC/room with truthful placeholder function rather than faking a working spell. Use exact actual classes and data formats found in code. The Academy must not replace ordinary survival research routes.

## NPCs and mage outfits
Use existing humanoid/villager/other appropriate person models where technically possible. Add persistent title, name, dialogue, schedule/workstation and role-specific fabric robe overlay/outfit textures or supported equipment. Do not construct costly custom biped rigs unless evidence says necessary. Roles and spaces in `04_TEACHERS_NPCS.md`; uniqueness should arise from names, costume, voice/text, profession, pedagogy and personality. Safe spawn and save/reload; no duplicating NPCs or converting normal villagers unexpectedly; clients see the same identity/outfit. Check art library before creating visuals.

## Winter climate aura
Implement the server's definition of an academy-region envelope, but render snow and mist **client-side** under opt-in/reduced-particle settings. Weather should look continuous to a player **inside** the influence zone, with gradual boundary fade and winter sound/lighting. Outside the boundary, original world weather/biome appears unchanged. Avoid global setWeather and permanent biome replacement. Ensure a player at the bridge experiences a magical change; test clients in different biomes. See `06_PERPETUAL_WINTER.md` for fallback when fog shader APIs aren't available.

## Secrets and play
Build actual discoverable clues, unique interactable rooms and progression-safe puzzle/reward state from `05_SECRETS_EVENTS.md`. Design for reset-safe puzzles, multiplayer claims and save/restart. Give clues before locks. Respect survival balancing; no duplicateable high-tier equipment or mandatory exclusive upgrades.

## Work schedule and safety
1. Audit and recover. Create `feature/winterhold-grand-academy` from current main (or an appropriate unique name) and **push a first checkpoint early**.
2. Implement **one coherent vertical slice**: site, bridge, courtyard, three primary towers, Hall of the Elements, one teaching NPC, one usable existing magical workstation/research interaction, localized snowfall/mist proof, one real secret. A working and testable small slice is better than 50 empty rooms.
3. Run focused data checks, unit logic, `./gradlew build` with JDK 25 if available, server GameTests and selected client screenshot tests. Honestly report skipped runs. Save an overhead map and in-game images when available.
4. Commit, push, open **draft PR** and report direct links. Then expand in ordered milestones from `07_PROGRESS_AND_BUILD_ORDER.md`, with checkpoint summaries after major chunks. Never leave hours of work only in a transient workspace. Do not claim a push/PR until the tool confirms it.
5. Do not touch main, merge, publish, or install unapproved dependencies.

## Requirements for each checkpoint
State (a) exact branch/commit/PR, (b) implemented paths and named areas with functional status, (c) what works and what is only planned, (d) tests actually run and results, (e) screenshots/preview evidence, (f) next bounded slice. Any implementation architecture deviations must update the design-data source of truth.

START NOW: inspect the repository and recent Winterhold work; create and push an early, useful checkpoint; then start the actual first vertical slice. Do not finish at a plan or an issue.
