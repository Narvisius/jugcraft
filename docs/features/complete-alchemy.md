# Complete owner-library alchemy — local integration, 17 October 2026

> Publication update, 18 October 2026: the owner accepted the combined alchemy presentation and authorized publishing the alchemy additions and UI fixes. Historical local-only checkpoints below are superseded. Current scope and validation are recorded in [Alchemy interfaces and access](alchemy-interface-and-access.md).

Owner request: implement all remaining entries in the audited Equivalent Exchange-style asset inventory, then test the combined result at the end. Keep this work local for an owner-controlled playtest before publication. Implementation and integration by **OpenAI Codex (GPT-6)**, including bounded delegated equipment, machine and storage work.

**Current extension:** [Connected Alchemy](connected-alchemy.md) supersedes the original manual-only converter, offhand-only Eye and six-artifact pedestal behavior described below. This record preserves the preceding slice and its actual validation.

## Scope and player outcome

The audited collection has **108 logical item/block entries**; texture states, armor trims and supporting GUI/audio images are not separate gameplay features. The preceding local slices implemented 26. This integration adds the remaining **82**, using the supplied assets and original Jugcraft behavior within the existing Assay/essence system.

| Family | New entries | Implemented content |
|---|---:|---|
| Equipment and materials | 32 | Four remaining Dark Matter tools; nine Red Matter tools including Katar and Morning Star; full Dark Matter, Red Matter and Gem armor; Red Matter and Iron Band; five material/fuel storage blocks |
| Workshops | 9 | Three relay tiers, two Matter furnaces, Dark Matter Pedestal, standing/wall Interdiction Torch, Nova Catalyst and Nova Cataclysm |
| Storage and study | 24 | Alchemical Chest, sixteen independent colored bags, placed Table and portable Tablet, Manual, catalogue Tome, Density Gem, Void Ring and Mind Stone |
| Utility artifacts | 17 | Swiftwolf, Harvest Goddess, Ignition, Zero, Arcana and Archangel artifacts; Body/Soul/Life Stones; Watch; three Divining Rods; Destruction Catalyst, Catalytic/Hyperkinetic Lenses and Mercurial Eye |

Related detailed records: [equipment](ee-equipment.md), [workshops](ee-machines.md), [storage](ee-storage.md), [preceding collectors and condensers](alchemical-workshops.md), [stars and amulets](elemental-alchemy.md), [original Dark Matter tools](dark-matter-workshop.md) and [tablet/starting gear](alchemical-gear.md).

## Integration and controls

- Existing essence balances, material catalogue, research, claims, party membership and item transfer APIs remain authoritative. No second currency, standalone mod or per-color shared bag inventory.
- Ordinary recipes produce all entries; High Covalence Dust smithing upgrades preserve supported item state. Every special item/block is excluded from dissolution, including containers and stored essence/XP. Material storage blocks craft back losslessly; fuel blocks have nine times the base fuel duration.
- Passive artifacts work only while **held or worn and activated**. Carrying one in an ordinary inventory slot does nothing. Use toggles; sneak-use changes Swiftwolf/Arcana mode. Tooltips and the existing Encyclopedia's equivalence entries describe each cost and action. Body restores food, Soul restores health, Life handles both. Duplicates do not multiply support.
- Swiftwolf grants paid vanilla flight (double-jump) in its flight modes. A short, server-owned lease is revoked on removal, loss of power, death, disconnect or disable. Its persistent marker clears stale grants on reconnect. Removal gives five seconds of slow falling. Creative/spectator and pre-existing unrelated flight are preserved. Gem armor does not create a competing flight owner.
- Archangel uses a paid spirit shot without recoverable ammunition. Rings and Watch affect bounded nearby hostile targets through existing friendly-fire/ward protections. Ignition never starts terrain fires. Zero temporarily freezes source water. Watch accelerates one looked-at permitted furnace/condenser step, charging both its own cost and the machine's normal operation costs; no global tick manipulation.
- Divining Rods inspect loaded, permitted ore cells only. Mining artifacts use ordinary player breaking/drops and skip inventories, fluids and very hard blocks. Mercurial Eye uses actual blocks from the offhand and native placement/collision rules. Harvest respects Concordance organisms' special ecology and harvest rules.
- Relays route essence through their visible front face into touching workshops or a socketed star. Matter furnaces use ordinary smelting recipes, existing essence and normal outputs/XP. Pedestals retain their artifact. Torch wards do not push players or pets. Paused, redstone-disabled, protected or owner-absent machines stop work and hopper access. No chunk loading or offline catch-up.
- Nova charges require deliberate owner activation, consume their placed block and personal essence, keep ordinary drops and skip inventories, fluids, claims and unloaded terrain. They cause no entity damage, fire or chain reactions.
- Density Gem opens deliberate conversion. Void Ring requires a preview and a second confirmation for the opposite-hand material. Mind Stone stores actual XP points with a fixed cap and exact transfer, not a level-based exchange. The Manual and Tome explain/browse; neither grants research or free resources.

## Assets and source preservation

`tools/concordance_ee_equipment.py`, `tools/concordance_ee_machines.py`, `tools/concordance_ee_storage.py` and `tools/concordance_ee_artifacts.py` are canonical data definitions. They join `tools/concordance.py`; `tools/gear.py` owns shared equipment tags; `tools/owner_art.py` contains their source mappings.

Every mapped image/sound comes from `art/owner-library/originals/Magic/`. Runtime PNG/OGG copies remain byte-identical. Native model definitions adapt namespaces, item state predicates, armor trim selection and equipment layers for 26.3; supplied textures are not redrawn. The existing condenser chest geometry remains the common parent, and the new chest has a separate textured child. Original sources and notices are preserved.

## Validation and local playtest

Implementation precedes the combined validation phase, as requested. Added server suites cover each of the four families, conservation, permissions, custody, persistence, crops, equipment and effects. `CompleteAlchemyClientGameTests` exercises real networked machine controls, bag transfers/reopening, catalogue/manual/tablet screens, worn armor and a collector → relay → furnace line. It produces a separate labeled showroom with all new entries and practice supplies. Its one-time datapack assigns workshop ownership to the actual first local player.

**Executed locally after all 82 entries were integrated:**

- Full Gradle build: **1,886 required server game tests passed**, including 66 new cases across the four families.
- Rendered `CompleteAlchemyClientGameTests`: passed real menu controls, transfer/persistence, pedestal synchronization, connected workshop output and showroom ownership binding. Screenshots inspected for native models, armor and readable interfaces.
- Full data/model/recipe audit: **3,021 material IDs**, 8,522 block/item models and supporting geometry checked successfully. Every imported owner asset matches its source.
- **67 Python tests passed**, repository structure/local links and locked Modrinth dependencies checked.
- The first combined run exposed a shared Charm tag overwrite, ordinary empty-container acceptance and creative-only test players in Survival flight fixtures. Those were corrected before the green server rerun. Two additional regressions cover Gem boots detecting short falls and fire wards preserving existing longer potion effects.
- Rendered inspection then found the 54-slot storage panel taller than the default 240-pixel GUI. Bag/chest coordinates were compacted to 216×232 and the furnace frame to 348×236. The final jar was rebuilt and the real client test repeated with panel/slot bounds assertions. These last changes affect layout only; the full server suite result above precedes them.

The separate **Jugcraft Complete Alchemy** save contains all 108 inventory entries, labeled workshops, equipment displays, crops, mining/combat supplies and XP bottles. The local Modrinth pack preserves older profiles/worlds. Owner playtest, two independent clients and a representative multiplayer load test remain unperformed. Nothing is published or pushed by this work.
