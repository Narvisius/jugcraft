# Alchemical storage, study and deliberate conversion

Owner-selected remaining Equivalent Exchange asset integration, implemented with **OpenAI Codex (GPT-6)**. This is local development. No publication or merge is authorized by this package. The owner requested implementation first and one combined validation phase afterward; that combined phase has now passed, with evidence below.

## What the player can do

This slice registers **24 entries**: Alchemical Chest; sixteen individually colored Alchemical Bags; Transmutation Table; Transmutation Tablet; Alchemical Manual; Tome of Material Equivalence; Gem of Eternal Density; Void Ring; and Mind Stone.

- **Chest and bags:** 54 real slots each. Color is cosmetic, never a shared inventory key. Chest access checks the owner's party, distance and shared building permission. It offers no hopper or Fabric transfer bypass. Ordinary removal drops the actual inventory once. Each physical bag saves its inventory to Minecraft's persistent container component after every transaction. The exact held inventory slot is locked against pickup, shift-click, number-key and offhand swaps while its menu is open. A stale menu loses access if another system removes that stack. Closing never copies the bag contents into the player inventory. Storage contents remain accessible when magic is disabled or research changes.
- **No nested containers:** bags, bundles, shulker boxes, filled container blocks, arbitrary portable container payloads, block-entity or entity payloads, and items declaring that they cannot fit in containers are refused. Ordinary empty chest/barrel/furnace blocks remain valid building supplies, including vanilla's default empty container component. Ordinary named/damaged gear is preserved exactly. There is deliberately no bag recoloring recipe that could destroy the stored component.
- **Table and tablet:** open the existing native essence tablet interface. A placed table checks its block identity, same level, eight-block distance and Authority permissions throughout use. The portable tablet must remain held. Both use existing Assay progression, the same personal essence, the same conservative rounding and 125% formation price. Temporary real slots are returned by the established menu on closing.
- **Study:** the Manual has six short selectable sections explaining the current systems. The Tome shows the live server catalogue in pages of 36 samples and the selected item's dissolve/form price. It never grants research or a sample. Every click form, including clone, throw and drag, is read-only. Catalogue lists refresh after datapack changes rather than sorting every server tick.
- **Gem of Eternal Density:** deliberate consolidation through the established real-input/retained-target/output interface, with full preview and confirmation. Its ordinary item texture represents the manual tool; it has no passive inventory-scan state.
- **Void Ring:** weigh the plain material in the opposite hand; using it again within ten seconds confirms dissolution through the existing Assay implementation. Named, enchanted, charged, excluded and uncatalogued materials are refused. It has no automatic mode and never consumes the rest of the inventory.
- **Mind Stone:** use stores at most 100 actual XP points; sneak-use retrieves at most 100. Capacity is 1,000,000 points on the physical item. Current experience is calculated from the actual level/progress bar because Minecraft's lifetime counter can be stale after enchanting. Transfers normalize the bar through vanilla point addition, limit both ends, and conserve integer points. This is not a level exchange or a second currency.

## Progression and contracts

The existing covalence dust chain, vanilla iron/glass/obsidian/book/wool, Dark Matter and the companion package's Iron Band/Red Matter supply crafting. All 24 entries have canonical survival recipes. Each bag uses the corresponding wool color, Low Covalence Dust and an empty Alchemical Chest block item. The tablet consumes a placed-table item and Dark Matter, not a filled container. The Manual is reachable with paper, a book and Low Covalence Dust. Utility conversions require existing understood Assay and a balanced server catalogue. Bags/chests are physical storage and cannot lose access to existing contents because research or feature settings change.

All entries are excluded from the equivalence catalogue. No new material value, generic permission framework, global storage network, research auto-unlock or currency is introduced. The interfaces use the existing native tablet widgets and Minecraft inventory synchronization. Block shapes are static supplied art; the chest lid does not animate. Color variants share behavior intentionally.

The only existing gameplay-menu change is an explicit access predicate in `TransmutationMenu`. Its original constructor preserves the Philosopher's Stone requirement. Client mirrors do not consult server-only knowledge or catalogue data to authorize slots; the authoritative server still checks the predicate and Assay state.

## Asset provenance

Canonical source: `art/owner-library/originals/Magic/`. `tools/concordance_ee_storage.py` maps 26 PNGs byte-for-byte, with namespace/path changes only:

- `textures/item/alchemy_bags/{color}.png` for all sixteen bags.
- `textures/item/{transmutation_tablet,book,tome,dense_gem_off}.png` for tablet, manual, tome and gem.
- `textures/item/rings/{void_off,mind_stone_off}.png` for the two manual utility items.
- `textures/block/alchemical_chest.png` and `textures/block/transmutation_stone/{top,bottom,side}.png`.

The supplied `models/block/transmutation_table.json` preserves its four-pixel slab geometry and UVs with remapped texture paths. The chest uses the already imported `base_chest.json` geometry through the existing shared parent `block/alchemical_chest`; its textured child is named `block/alchemy_storage_chest` to avoid replacing the parent used by condensers. Originals, metadata and supplied notices are unchanged. No library code, programs or external mod runtime is imported. The Manual's supplied `book.png` is 32×32. Its narrowly documented owner-size exception checks the exact runtime path and byte identity to that source; the general 16×16 icon rule and frozen legacy list remain intact.

## Validation plan and current evidence

The combined local validation passed: **1,886 required server tests**, 67 Python tests, full data/model/recipe and unchanged-owner-art audits, and rendered `CompleteAlchemyClientGameTests`. The client verified menus, inventory persistence, connected processing, pedestal display and worn armor. A final layout-only adjustment was rebuilt and checked again in the real client. See [complete alchemy](complete-alchemy.md#validation-and-local-playtest) for the exact sequence, fixes and evidence. Owner playtest and a two-client multiplayer check remain unperformed. Nothing was pushed.
