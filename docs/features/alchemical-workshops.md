# Essence collectors and condensers

> Publication update, 18 October 2026: the owner accepted the combined alchemy presentation and authorized publishing the alchemy additions and UI fixes. Historical local-only checkpoints below are superseded. Current scope and validation are recorded in [Alchemy interfaces and access](alchemy-interface-and-access.md).

Owner requested collectors and condensers with existing assets on 17 October 2026. Implemented locally with **OpenAI Codex (GPT-6)** on `local/alchemical-gear-oct16`, based on `a59edd31d`. Publication and owner playtest remain pending. The open-PR refresh found no overlapping alchemical workshop contribution.

## Player outcome

Three daylight collectors feed two material condensers, directly or through the previously implemented Klein Stars. This adds a deliberate, slow renewable source of existing essence. It does not add another currency or change catalogue values, tablet costs, saved player balances or paid equipment behavior.

| Block | Buffer | Work per second | Transfer limit |
|---|---:|---:|---:|
| Collector I | 4,096 | 1 essence in strong daylight | 64 essence |
| Collector II | 16,384 | 4 essence in strong daylight | 256 essence |
| Collector III | 65,536 | 16 essence in strong daylight | 1,024 essence |
| Condenser I | 65,536 | Up to 1 output; 8 dissolution inputs | 4,096 essence from a star |
| Condenser II | 262,144 | Up to 8 outputs; 64 dissolution inputs | 32,768 essence from a star |

Collecting requires a clear sky above and sky light minus sky darkness of at least 12; artificial light cannot substitute. Stored essence can still transfer at night. Collectors try only their six loaded adjacent blocks, sending to related-owner condensers first and charging their docked star afterward, within one shared transfer budget.

A condenser keeps one real target sample and makes ordinary copies at **125% of catalogue value, rounded up per item**, identical to the tablet. Six output slots permit mixed outputs after target changes. Three explicitly marked **To dissolve** slots consume plain catalogue materials, conservatively rounded down. The star and target are never dissolution inputs. Missing/invalid/unaffordable-by-capacity targets and full output stop intake before items or star charge are consumed. The UI shows buffer, personal balance, per-item cost, throughput and a concrete status. No typing is needed.

**Store essence** and **Take essence** transfer exactly between the block and existing personal ledger, bounded by both capacities. A pause button or redstone signal stops work and hopper access. Hoppers insert dissolution materials from above/sides and extract only finished output below; star and sample remain manually controlled. Shift-click sends ordinary materials to the marked dissolution slots, never silently selects a target.

## Progression and integration

Understand existing Assay. Craft the first collector from glass, glowstone, obsidian, Dark Matter and a furnace; the first condenser from obsidian, Dark Matter and a chest. At a vanilla smithing table, High Covalence Dust + the current block + Dark Matter upgrades one tier and preserves carried essence/custom names. No Red Matter prerequisite or new research gate is invented. Catalogue exclusions cover all five workshops. Existing technology can supply raw materials and hopper logistics; collectors/stars supply both these workshops and the earlier paid alchemical equipment.

One block-entity implementation and native server menus own all transactions. The existing native tablet widgets provide the client UI; no new framework or dependency. Static supplied models represent the blocks; a working block emits a small light and the interface animates collection/formation, respecting reduced-motion settings. The chest lid is a static model, not an opening animation.

## Authority, bounds and saves

Every loaded block pulses once per 20 ticks, staggered by position. No scanning, chunk tickets, offline production or elapsed-time catch-up. The owner must be alive, online in this dimension and know Assay. Owner/party menu access checks ordinary distance and the shared `Authority` permission gates. Automatic neighbour transfer checks related owners, destination readiness and both locations. Hopper access checks the workshop and adjacent location, with current owner authority. Feature disable and an invalid equivalence catalogue stop production.

The eleven-slot inventory, UUID owner, buffer and pause flag persist. Ordinary mining copies `jugcraft:workshop_essence` to the dropped block and drops the real inventory separately. Placement rebinds ownership to the placer. Buffers clamp to their actual tier on load/placement. Creative removal and explosions obey normal block-drop behavior; move valuable workshops by ordinary mining or withdraw first. No existing IDs or worldgen change.

## Owner assets

Sources are under `art/owner-library/originals/Magic/`. Seven images are copied byte-for-byte by `tools/concordance_workshops.py` / `tools/owner_art.py`:

- `textures/block/collectors/{front,other,top_1,top_2,top_3}.png` → the corresponding `textures/block/alchemy/collectors/` runtime path.
- `textures/block/condenser_mk{1,2}.png` → `textures/block/alchemy/condenser_mk{1,2}.png`.

The three supplied collector block definitions are expressed with their same orientable geometry and renamed texture paths. Supplied condenser block files are particle-only placeholders for an external renderer; the complete supplied `models/block/base_chest.json` geometry/UVs (also used by the supplied condenser item models) instead renders both placed and held condensers. Canonical model finishing handles shared face boundaries. Originals and metadata remain intact. No library programs or external mod code are executed or imported. The UI reuses the earlier imported owner tablet sigil with a muted render tint; its source pixels remain unchanged.

## Validation

Local compilation and the full **1,820-test server suite passed**, including **19 new workshop tests**. These cover conservation and throughput, blocked intake, invalid/modified items, owner/research/claim gates, exact menu transfers, dedicated-client slot prediction without server-only catalogue data, save/load, real loot, smithing retention and closed unsided automation. The regression run exposed an unrelated flaky Alpha Werewolf fixture: it arbitrarily selected a path cell while asserting moss restoration. A separate local commit selects a moss cell; the existing path-restoration assertions remain.

The disposable rendered client test passed with actual menu clicks, deposits/withdrawals/pause, both interface scales and keyboard focus, supplied-model inspection, daylight/night/roof behavior, live input/output hoppers and Fabric transaction rollback. The showroom's one-time first-player ownership binding was exercised after a real datapack reload. The subsequent dedicated-client prediction fix is covered by its focused server test; no manual two-player session is claimed.

The full data/art audit passed for **2,939 material IDs**, including model rendering checks. `python tools/owner_art.py --check` confirmed imported images match their supplied originals. Repository documentation checks, the **18-library** companion-pack lock check and **67 Python tests** passed. Optional-library absence was not rerun for this package; no dependency or optional integration changed.

An independent **Jugcraft Alchemical Workshops** save is prepared for the local Modrinth pack and the development client: all five workshops, a working hopper line, supplies, unlocked Assay and 200,000 personal essence. Its world-only datapack binds seven prepared machines to the first player once; the saved test-account marker is removed from these fresh copies. All earlier owner saves remain intact. Pack integrity and embedded-JAR equality are verified during packaging; this does not claim a manual launcher import or owner gameplay. Owner playtest remains required before publication. Nothing has been pushed, and no PR or merge is authorized for this package yet.
