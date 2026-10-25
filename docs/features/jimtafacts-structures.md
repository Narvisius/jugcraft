# Jimtafacts original structures

OpenAI Codex authored the local 26.3 integration on 23 October 2026. Combined runtime generation and gameplay tests remain deferred until the complete Jimtafacts pass is implemented. This is not a claim that the structures have already been playtested.

## Imported layouts

The supplied 236 templates retain their dimensions, block positions, palette indices, entity positions, jigsaw connections and selection weights. The nine source structures are the Ancient Battleground, Catacombs, Citadel, Evoker Fort, Ice Spider Den, Impaled Icebreaker, Mangrove Hut, Mountain Tower and Pyromancer Tower. Source generation heights, spacing, separation, salts and biome restrictions are carried over. Priest houses join the plains and taiga village house pools with a native weight of one. Pocket-room templates are retained; the player spell continues to use its separately managed private room.

New templates use native Jugcraft blocks, original source creatures and actual registered items. Old item stacks, armor/hand inventories, scroll contents and component names are migrated to the pinned version. Template mob identities, author-world memories, old spell debt and vault reward histories are cleared. Loot references point to the separately adapted source loot collection, including trial-spawner rewards. No source script or executable is run.

Original pool elements can request terrain adjustment per room. `JimtaStructures.TerrainElement` preserves those requests, and the Beardifier adapter changes only the corresponding density pieces while keeping vanilla junctions and neighboring structures intact. Source foundation, lit-block, copper-weathering and vault-reset processors are ported. Foundation work is restricted to the currently generating chunk and bounded by the world's height; weather variation uses the placement seed instead of a global random generator.

## Explicit source gaps and adaptations

The archive references `irons_lib:player_statue` and two old statue block-entity names, but supplies neither that library nor its original model/skin data. Those sixteen decorative statue cells become air, with every affected template and palette index recorded in `docs/audits/jimtafacts-spellbook-structures.json`. The rest of each original building remains. The missing statue randomizer is omitted rather than represented by invented replacement art.

The source Ice Spider Den biome tag uses NeoForge tag subtraction. Its supplied vanilla result is represented explicitly as snowy plains, snowy taiga and ice spikes; additional modded snowy biome subtraction needs a native biome predicate before expansion. Original source per-level spell scrolls become the corresponding implemented native level-one spell scroll. Generated vaults use native per-player reward accounting and no author-world reward ledger.

## Save and release behavior

Only normal new-chunk generation and explicitly requested structure placement use these templates. There is no automatic retrogen, regeneration, city rewrite or existing-world deletion. Existing structure definitions remain registered for saved worlds; new structure-set placement follows the Concordance feature switch. World Designer sees the registered structures in its ordinary exported catalog and can pin their start chunks under its existing terrain/biome constraints.

Original files and notices remain under `art/owner-library/originals/Oct23/Jimtafacts/`. `tools/jimtafacts_spellbook_structures.py` produces the copies and source-hash inventory. Register `JimtaStructures` after spellbook content, and enable `JimtaBeardifierAccess` plus `JimtaTerrainMixin` in the common mixin configuration. Layout/component contract tests are authored in `scripts/tests/test_jimta_structure_assets.py` for the final combined run.
