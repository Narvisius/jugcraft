# Evidence-based repository baseline (inspected 2026-10-20)
Repo: https://github.com/jimbozoomer-byte/jugcraft | default branch `main`.

## Confirmed in current repository documentation
- `docs/PLATFORM.md`: Minecraft Java **26.3**, Fabric; Java **25**, pinned Gradle/Fabric dependencies. Do not change pins.
- `README.md`, `docs/WHAT_EXISTS.md`: Arcane Concordance with research/knowledge, spell composition, workstations, invocations; existing worldgen, lair, and towns; Minecraft server/client tests.
- `docs/ARCANE_CONCORDANCE.md`: server-owned Knowledge and Focus attachments; datapack rules under `data/<namespace>/concordance/`; Spell Engine casts and cooldowns; Spell Power schools; Modonomicon codex. There is **not** a new mana bar. Distinguish Principles such as Radiance, Ember, Rime, Tempest, Strata, Verdance, Echo, Hollow from Skyrim spell-school labels. Only use actually implemented functionality.
- `art/owner-library/MAGIC_ASSETS.md`: owner-supplied magic library (7,909 files, incl. textures, models, sounds and NBT); must inspect precise files before reusing; don't overwrite original library.
- `docs/WORLD_DESIGNER.md`: new-world World Designer can pin native **structure start chunks**, not guaranteed exact corners/rotations, no retrogen. Don't pretend this is an instant academy placement system.
- `docs/WHAT_EXISTS.md`: fog machine with `fog` particle exists. **Not proof** that region-locked world fog, localized snowfall or a Winterhold climate controller already exists.
- `README.md`, `docs/TESTING.md`: server game tests and rendered-client screenshot tests available; real two-human-client tests remain manual.
- `CLAUDE.md`, `CONTRIBUTING.md`: one focused branch per PR; preserve owner library provenance, avoid new dependency/version changes; check current implementation and open PRs before editing.
- `docs/plans/concordance-expansion-2026-10-10/START-HERE.md`: Concordance extension chunks exist; assess overlaps before implementation.

## Explicit limits of this audit
Docs were fetched from GitHub main on 2026-10-20. **No complete source-code audit, compile, full branch comparison, or live in-game verification was performed while preparing this pack.** Some docs can lag actual code and may refer to proposed or unmerged work. Agent must verify class names and methods before coding.

## Initial investigation targets
`CLAUDE.md`, `AGENTS.md`, `CONTRIBUTING.md`, `README.md`, `docs/WHAT_EXISTS.md`, `docs/PLATFORM.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN.md`, `docs/TESTING.md`, `docs/FRAMEWORKS.md`, `docs/ARCANE_CONCORDANCE.md`, `docs/ARCANE_CONCORDANCE_GUIDE.md`, `docs/WORLD_DESIGNER.md`, `docs/CONTENT_BRANCHES.md`, `docs/features/arcane-concordance-*.md`, `docs/plans/concordance-expansion-2026-10-10/` and `art/owner-library/catalog/magic/`.

## Design rules
- One shared source of truth for player knowledge, research and spell costs.
- No permanent global rain, snowfall, biome swapping or world time changes for the atmospheric effect.
- Cosmetic NPC professions/titles do not automatically grant commands, permissions or trades.
- Avoid cloning protected Skyrim game assets; reinterpret geometry and source visuals using owned/generated/approved Jugcraft assets.
- Preserve player save compatibility and deterministic structure placement. Avoid world-gen work per tick.
