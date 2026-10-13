# Jugcraft: current state and integration map

**Audit date:** 10 October 2026. **Reviewed main:** `4b36d2254fd76e7db2a4cfa6ec488d746a999f1f`. **Author:** OpenAI Codex. This is a planning artifact, not an implementation or a new gameplay test report.

[Read the owner overview](START-HERE.md) · [Delivery chunks](09-claude-delivery-chunks.md) · [Sources](11-research-notes-and-sources.md)

## 1. Evidence rules for every future agent

Use these labels in tickets, PRs and check-ins:

- **MAIN:** present in the reviewed main source. This says nothing by itself about human playability.
- **OPEN CONTRIBUTION:** a named PR or branch, not yet part of the reviewed main.
- **OWNER DIRECTION:** requested or accepted in repository notes; implementation can still be absent.
- **PROPOSAL:** new design in this pack. Numbers are starting tuning values, not accepted balance.
- **UNVERIFIED:** requires a real test or unresolved source evidence.

Refresh main and relevant open contributions before implementation. Do not apply these October 10 status statements indefinitely. Keep an exact commit in each checkpoint. Code and registrations outrank an old status paragraph; owner choices outrank a new agent's preference.

The local editor checkout used in the earlier conversation was behind main. This review instead used an immutable snapshot of the current main. Do not resume from the old world-designer branch and recreate merged features.

## 2. What already exists

| Area | Audited state | Expand from here |
|---|---|---|
| Concordance foundation | MAIN: rules, research, instruments, resources, protections and persistence | Add content and repair gaps; do not replace the foundation |
| Casting | MAIN: Spell Engine bridge, Spell Power school mapping, composed spell compiler | Visual composer; more distinct operations; explicitly designed delayed delivery |
| Rituals | MAIN: lesser circle, pylons, phased payment, attunement, cooperative rite | Diagnostics, alternative patterns, workshop rituals |
| Alchemy | MAIN: assay, ingredient vectors, preparation, heat, contamination, formula replay | Readable workbench, more ingredients and products, bounded automation |
| Ecology | MAIN: four magical crops, beds, habitat and disturbance rules | Garden design, grafting and useful material chains |
| Sky | MAIN: six occurrences, observatory and attunement, claim anti-repeat ledger | Forecasts, long projects and planned astronomical encounters |
| Crimson practices | MAIN: chalice, exhaustion, Focus surge, growing blade | Distinct support/ritual choices; optional transformation remains separate |
| Spirits and constructs | MAIN: Hearthling, gathering shade, porter, agreements and limited jobs | Stolas pilot, richer contracts and shared worker jobs |
| Artifice and relics | MAIN: substrates, affixes, runes, gems, bonds, resonance and relic contexts | Survival acquisition, exclusions and utility sidegrades |
| Equivalence | MAIN: restricted 45-material catalog and exact-value arithmetic | Audit integrations; do not expand by blanket item tags |
| Hexes and dreams | MAIN: three curses, links, wards and dream escrow | Readable consent, dispelling, bounded dream adventures |
| Conclaves and wonders | MAIN: renown, commissions, projects and Concord Spire | Deeper projects without compulsory boss gates |
| Ember | MAIN: Hearthbinding content, combat/utility spells, armor and jewelry | Establish the quality bar for additional schools |
| Thallite | MAIN: earth metal, equipment and living/earthen ground interactions | Use for Strata and Verdance; avoid inventing another equivalent ore |
| Encounters | MAIN: Vesperine and Madame Tatterlace | Counterplay, support credit and dossier presentation |
| Yeti | OPEN CONTRIBUTION: PR300 supplies Glacier Hall and Frost Horn | Preserve its geometry and planned part-two encounter |
| World Designer | MAIN: offline terrain/biome editor and world preset pipeline | Magic layers, exact settlement planning and entrance placement |
| Encyclopedia | OWNER DIRECTION: inventory button and configurable key, technology and magic pathways | Unified navigation over real server state, no required book item |
| Stolas and 72 spirits | OWNER DIRECTION: Stolas first, connected to Styx, astronomy, plants and minerals | Shared pact definitions, then distinct cohorts |
| Industry expansion | OWNER DIRECTION plus extensive existing machines | Shared materials, selected interfaces and optional automation |

The old boss branch page saying nothing is built is stale. Some Concordance delivery notes also describe PRs as pending even though their code has merged. Correct those summaries in the first implementation chunk; preserve historical test evidence with dates.

## 3. Canonical magical language

Principles describe affinity and understanding; they are not ten new currencies.

| Principle | Domain | Existing attribute bridge |
|---|---|---|
| Radiance | Light, disclosure, protection through understanding | Arcane |
| Ember | Heat, combustion, controlled transformation | Fire |
| Rime | Cold, preservation, restraint | Frost |
| Tempest | Motion, charge, storms | Lightning |
| Strata | Stone, weight, architecture | No mapped school |
| Verdance | Growth, habitat, restoration | Healing |
| Tide | Flow, dissolution, mixtures | No mapped school |
| Tether | Binding, agreements, boundaries | No mapped school |
| Echo | Memory, resonance, dreams | Arcane |
| Hollow | Decay, endings, departed things | Soul |

Do not invent an attribute just because a Principle lacks a direct Spell Power school. A structural or utility effect can use its own bounded, server-defined parameters. Radiance and Echo sharing an attribute does not make them the same tradition.

Seven resources already have different jobs:

1. **Focus:** personal casting. Current base pool 20, one point regenerates per 40 ticks. Never pipe or bottle it.
2. **Ley Charge:** stored power for devices and rituals.
3. **Principle Essence:** explicitly typed amounts; Radiance is not interchangeable with another essence.
4. **Vitae:** ritual resource with health/exhaustion acquisition rules, distinct from health itself.
5. **Astral Resonance:** occurrence-aligned resource with per-owner claim protection.
6. **Bound Will:** an agreement identity and record. Never turn it into a fungible fluid.
7. **Prima Materia:** restricted material-accounting value with exact arithmetic.

Stages are Initiate, Practitioner, Adept, Master and Architect. Research progresses Encountered → Observed → Understood → Mastered. Sharing notes can help another player reach Understood; it does not impersonate their personal practice.

## 4. Source entry points

Paths below are relative to the repository. New file names elsewhere in this pack are proposals unless marked existing.

| Responsibility | Existing entry points |
|---|---|
| Catalogs and progression | `concordance/ConcordanceData.java`, `concordance/ConcordanceProgress.java`, `concordance/rules/` |
| Casting and composed grammar | `concordance/ConcordanceSpells.java`, `concordance/compose/` |
| Effects and authority | `concordance/ConcordanceEffects.java`, `concordance/Authority.java`, `concordance/effect/` |
| Resource ledgers | `concordance/resource/` |
| Ritual execution | `concordance/ritual/`, `concordance/CircleAnchorBlockEntity.java` |
| Workers and spirits | `concordance/worker/`, `concordance/spirits/`; locate `CourierLedger` before adding delivery work |
| Machines | `machine/MachineBlockEntity.java`, `MachineKind.java`, `MachineRecipe.java`, `MultiMachineRecipe.java` |
| Fluid processing | `chemistry/FluidRecipe.java`, `FluidMachineSpec.java`, `FluidTank.java`, `FluidTanks.java` |
| Power and shafts | `energy/EnergyStorage.java`, `kinetic/KineticNetworks.java` |
| Lair lifecycle | `lair/Lair.java`, `LairInstance.java`, `Lairs.java`, `LairVisit.java`, `GraveGoods.java` |
| Existing boss behavior | `lair/vesperine/`, `lair/tatterlace/`, `LairBosses.java` |
| World design | `world/design/`, `tools/world-designer/` |

Java paths start at `src/main/java/io/github/jimbozoomer/jugcraft/`. Client implementations live in the client source set; locate their current names with `rg` before editing. A pack author should not create a second class because a design document used a conceptual name.

Read `AGENTS.md`, `CLAUDE.md`, `CONTRIBUTING.md`, `docs/TESTING.md`, `docs/FRAMEWORKS.md` and the relevant feature record first. Generator-owned JSON must be changed through its generator and regenerated.

## 5. Contracts that new work must preserve

### Casting

The existing Spell Engine listener refuses invalid casts, runs the Jugcraft custom impact and settles Focus/cooldown once through COST_CONSUME. A pending settlement currently associates a player with spell, Focus, cooldown and exact cost. Before adding concurrent projectiles or sustained casts, examine that lifetime: a per-player pending record is not automatically a multi-cast transaction system.

All branches and pulses in a composition share its effect ledger. No applied effect means no successful-cast charge; a later empty pulse does not refund earlier valid work. Authority checks and target tolerance belong at actual application, not just preview.

Current grammar has here/touch/ray deliveries; struck/creatures/spread selection; light/reveal/ward/sear/dazzle operations; bounded modifiers, branches and pulses. It is not an unrestricted visual scripting language. Current composed delivery uses Jugcraft traces, not a generic moving projectile engine.

### Rituals and energy

Existing pylons store 64 Ley and accept 64 JE/t. The conversion is 1,000 JE → 1 Ley. Radiance conversion is 3 essence → 2 Ley. Neither has a reverse route. Preserve these baseline economics when adding hardware.

A ritual reserves ingredients, channels resources in bounded steps, then commits output once. Interruption can lose already spent energy but keeps reserved ingredients available according to the existing machine contract. Anchor breaking must return items once. Do not consume an input again in an animation callback.

### Workers

Porters already spend Ley and integrity; this does not authorize new routine industrial-machine maintenance. Jobs must preserve exact item variants and claim ownership. A visible entity is not the authoritative copy of courier cargo. No worker should force-load chunks or cross dimensions automatically.

### Equivalence

The current whitelist excludes diamonds, emeralds, netherite and component-bearing/unique items. Dissolution rounds down; formation rounds up at a 5/4 cost. Iron's 256 units require 320 to form. New boss loot, pacts, research, charged tools and machine contents remain excluded. Audit recipe cycles, including hidden returned containers and salvage.

### Worlds

World Designer controls newly generated terrain. Existing chunks are not rewritten. Current structure pins select start chunks and are not exact, rotated buildings. Lair instances are runtime sessions; returns and grave goods persist, but active fights do not resume after restart. Neither capability should be advertised more broadly than implemented.

## 6. Approved dependencies and deliberate use

The authoritative versions are in `distribution/frameworks.lock.json`; do not duplicate pins in new build files.

| Library | Audited version | Implementation use and boundary |
|---|---|---|
| Fabric API | 0.161.0+26.3 | Common events, networking, transfers, attachments |
| GeckoLib | 5.5.7 | Authored models/clips for bosses, creatures, equipment and machines |
| Player Animation Library | 1.2.7+mc.26.3 beta | Player gestures with explicit ArmsMotion/gun ownership |
| Modonomicon | 2.16.0 | Codex and progression explanations; not authoritative unlock storage |
| SmartBrainLib | 2.0.3 | Sensors and decisions; not inventory/permission ownership |
| Spell Engine | 1.10.9+26.3 | Casting and supported delivery; retain Jugcraft settlement |
| Spell Power Attributes | 1.6.2+26.3 | Existing school attributes; avoid double scaling |
| Trinkets Updated | 4.2.1+26.3 | Existing accessory slots and equipment contexts |
| Cloth Config | 26.3.159 | Client configuration UI; required artifact |
| Jade | 26.3.5+fabric | Optional inspection, small server snapshots |
| JEI | 31.8.0.49 | Optional recipe/uses pages; audit live datapack refresh limitation |
| Fusion | 1.3.16 | Optional connected/scrolling surfaces with ordinary fallback models |
| LambDynamicLights | 4.13.0+26.3 | Optional visual moving light; no gameplay illumination authority |
| GuiLib | 0.12.4 | Optional rich client screens, keyboard/native fallback |
| Fabric Language Kotlin | 1.14.1+kotlin.2.4.20 | Client runtime for GuiLib path; not a declared common Kotlin runtime |
| Mod Menu | 21.0.0 | Optional settings entry |
| Iris | 1.11.7+mc26.3 | Optional shader support; no required shader pack |
| Sodium | 0.9.2+mc26.3 | Optional renderer in presentation stack |

Use the right library for the feature. Having eighteen libraries does not require every feature to depend on all eighteen. Research sources in this pack are not proposed runtime dependencies. The complete Modrinth pack handles installation; do not write a JAR downloader into startup code.

## 7. Existing owner constraints

- Build one coherent Jugcraft mod with internal packages, not a collection of incompatible mini-mods.
- Keep meaningful solo and trade routes. Bosses and real-world seasons cannot be mandatory for basic farming, electricity or magic.
- Preserve the approved industrial sequence: rich manual workshop; steel and first electricity in parallel; substantial chemistry after a steel/electrical foundation.
- Gas pressure is automatic. No manual compressor ladder or recurring lubricant/replacement-part chores.
- Pollution is local, bounded, naturally declining and attracts the existing raiders. It does not cause visible smog, recolor biomes, kill crops or impose sickness.
- New major industrial installations follow the approved 2–6 block dimensions and later dieselpunk art direction. Do not arbitrarily enlarge old tools or existing machines.
- Use the owner-supplied asset library as authorized, preserve originals and provenance, and adapt runtime copies technically. Do not redo approved art merely to make it different.
- Stolas begins a pact roster, not a mandatory enemy encounter. Vampirism is a separate, voluntary specialty, not another name for the Vitae economy.

## 8. What this review did not establish

Source inspection and prior repository evidence are not fresh runtime proof. This planning pass did not run a Java build, enter Minecraft, launch a server, fight a boss or import a pack. A comprehensive two-account playtest, fresh-world survival progression, exact current combat balance, late-game factory performance and every optional-renderer combination remain unverified here.

The known open contributions at audit were PR299 (launchers), PR300 (Yeti lair), PR301 (coil/plasma weapons). Refresh them before rebalance. A new high-damage gun can change encounter timings even if magic code is unchanged.

[Next: progression and spellcraft](02-magic-progression-and-spellcraft.md)
