# Relays and matter workshops

Owner-directed addition to the local Equivalent Exchange-inspired package, implemented with **OpenAI Codex (GPT-6)**. This package stays local until the owner has played it. The owner requested implementation of all remaining supplied content before the combined test run; the tests described below are written, with execution evidence recorded by the package integration pass.

## Player outcome

Nine new craftable blocks use the supplied models and textures: three Essence Relay tiers, Dark/Red Matter Furnaces, a Dark Matter Pedestal, an Interdiction Torch with standing and wall placement, and Nova Catalyst/Cataclysm excavation charges. They extend the existing Assay ledger, Klein Star storage, collectors and condensers. No new currency, remote network, dependency or chunk loader is introduced.

| Block | Input and useful output | Limits and cost |
| --- | --- | --- |
| Relay Mk I / II / III | Plain materials dissolve into the existing essence buffer; the textured front face feeds an adjacent compatible workshop, then the docked Klein Star charges | 8 / 16 / 32 input items per second; 256 / 1,024 / 4,096 outgoing essence per second; buffers 16,384 / 65,536 / 262,144 |
| Dark Matter Furnace | Three input slots run existing vanilla/data-pack smelting recipes into three output slots | One input every two seconds, 16 essence each; buffer 65,536 |
| Red Matter Furnace | Same recipes and inventory, faster throughput | Up to four inputs each second, 16 essence each; buffer 262,144 |
| Dark Matter Pedestal | Retained artifact serves a garden or nearby owner/party players | Buffer 16,384; effects below; no passive free effect |
| Interdiction Torch | Standing or wall light, with a paid hostile-mob repulsion field | Light 12; radius 6; at most 16 candidates/8 pushes each second, 4 essence per actual push |
| Nova Catalyst | Owner explicitly consumes the placed charge to excavate eligible blocks | Radius-two cube, at most 64 blocks; 4,096 personal essence |
| Nova Cataclysm | Larger controlled excavation charge | Radius-three cube, at most 192 blocks; 16,384 personal essence |

Use the native workshop menu to insert supplies, see the current status, pause, and move essence between the personal ledger and machine buffer. Furnace and relay slots clearly distinguish **To smelt** and **To dissolve**. Relays charge their star; furnaces, pedestals and torches draw from theirs. Stars and pedestal artifacts remain manual-only. No typed target names or amounts are accepted.

Collectors retain their original priority: adjacent ready condensers first, other adjacent alchemy machines second, then their own star. Relays deliver through one front face; they do not traverse a network recursively. A relay can feed another relay, condenser, furnace, pedestal or torch. Every transfer debits exactly the accepted amount and respects current permission at both endpoints. All transfers stay within each block's per-second budget.

Furnace input never overwrites full output. Modified/named inputs are deliberately refused rather than destroying custom components. Output uses the recipe's complete result. Recipe XP accumulates in the block and is released on manual output collection or block removal; hoppers leave XP stored. The Watch of Flowing Time can request one additional ordinary furnace pulse through `AlchemyMachines.bonusWork`; that hook checks the live owner and permissions, still spends the normal furnace essence, and never recursively accelerates a relay or pedestal.

## Pedestal artifacts

The artifact stays in its slot. Effects require the usual shared authority checks:

The retained artifact is visibly displayed resting on the pedestal's supplied top plate. Its actual baked bounds are fitted to the plate; active pedestals rotate it slowly, with no bobbing and a static reduced-motion option. Only a sanitized count-one display item ID is sent to observers. The update payload contains no owner UUID, star charge, stored inventory or private item components. Insertion/removal updates the display and chunk loading includes the current visual.

- **Repair Talisman:** repair at most four durability on one eligible equipped item per nearby party member, one essence per repaired point. Up to eight players within six blocks. Unbreakable and non-repairable items are skipped.
- **Body Stone:** one food point for 32 essence per nearby party member who needs food. **Soul Stone:** one health point for 32 essence. **Life Stone:** both independently, paying for each successful restoration. These stationary rates intentionally cost less than the portable stones.
- **Harvest Goddess Band:** scan at most 49 positions per pulse through a persisted 7×3×7 rolling garden cursor; at most four successful age steps, 16 essence each. Standard crops obey survival conditions. Concordance organisms call their existing `hasten` path and retain nutrient, niche, bed and feature-enable checks; crop and bed permissions are checked before invoking it. No nutrient bypass or free fixer production.
- **Swiftwolf's Rending Gale:** the same paid ward as the Interdiction Torch. It does not grant flight to other players.

The ward moves only living hostile `Enemy` creatures. It checks sight and current permissions and routes movement through the shared Concordance effect guards, immunity, resistance and knockback rules. It pays only for an applied push, never harms a creature, and never moves players, pets or neutral wildlife. It does not intercept projectiles. The torch still provides ordinary light when paused, empty, disabled or its owner is away.

## Nova controls

Only the actual owner may detonate: sneak-use with an empty hand, or explicitly press **Detonate** in its menu. The player must be nearby and have enough personal essence before anything changes. No eligible targets means no charge or item consumption.

The blast is controlled excavation, not a vanilla damaging explosion. It checks loaded chunks and permission per target and skips inventories/block entities, fluids, other Nova charges, blocks with negative hardness, and blocks harder than 20. Harvestability and normal drops use an unenchanted netherite pickaxe. It creates no fire, entity damage, chain reactions, remote trigger or ore multiplication. The supplied Nova sound, bounded mining particles and an exact cleared-block/cost message confirm activation. The placed charge is consumed once; stale requests cannot repeat it.

## Progression and automation

Understand existing Assay and make Dark Matter first. A relay uses obsidian, Dark Matter and a furnace. A Dark Matter Furnace wraps an ordinary furnace in Dark Matter. The pedestal uses Dark Matter and obsidian; the torch uses Dark Matter, glowstone and the new Iron Band. A Nova Catalyst combines gunpowder, Alchemical Coal, obsidian and Dark Matter. High Covalence Dust smithing upgrades the relay, furnace and Nova tiers, using Dark Matter or Red Matter as appropriate. These are reachable shared recipes, not creative-only content. The encyclopedia includes recipes, rates and control instructions.

Hoppers insert relay dissolution materials or furnace inputs from the top/sides and extract furnace output from below. Unsided Fabric storage returns empty, so automation cannot bypass manual-only star/artifact slots. Redstone or **Pause** stops processing and hopper access. Feature disable, a failed equivalence audit, protected terrain, an absent owner, and missing research stop work while preserving contents. Party members can use ordinary menus; the owner must still be alive and online in the same dimension for automation. Every loaded block has one position-staggered pulse per second; no catch-up after unload.

## Saves and owner assets

Stable registrations remain present when the feature is disabled. The block saves its inventory, owner UUID, essence, pause flag, smelting progress, retained XP and garden cursor. Mining copies essence onto the dropped block through the existing `jugcraft:workshop_essence` component, drops the actual inventory separately and releases stored XP. Smithing preserves carried essence and names. Placement rebinds to the placer. Buffers clamp to their actual tier. No old IDs or saves are renamed.

`tools/concordance_ee_machines.py` owns the data, recipe, model, language, tooltip and codex definitions. `OWNER_FILES` records source-to-runtime mappings and is consumed by the shared owner-art checker. Original library files are unchanged:

| Supplied source beneath `art/owner-library/originals/Magic/` | Runtime use |
| --- | --- |
| `textures/block/relays/{front,other,top_1,top_2,top_3}.png` | Byte-identical relay faces under `textures/block/alchemy/relays/` |
| `textures/block/matter_furnace/{dm_off,dm_on,rm_off,rm_on}.png` | Byte-identical off/on furnace faces |
| `models/block/{relay_mk1,relay_mk2,relay_mk3,dm_furnace,dm_furnace_on,rm_furnace,rm_furnace_on,dm_pedestal}.json` | Supplied geometry, face assignments and UVs; namespace/parent adaptation only, plus shared canonical compatibility finishing |
| `textures/block/{dark_matter_block,red_matter_block}.png` | Shared matter-block textures registered by the equipment package; used by furnace shells and pedestal |
| `textures/block/interdiction_torch.png`, `models/block/{interdiction_torch,wall_interdiction_torch}.json` | Both placement forms of one stable torch block; shared state drives supplied models |
| `textures/block/explosives/{bottom,top,nova_side,nova1_side}.png`, `models/block/{nova_catalyst,nova_cataclysm}.json` | Nova charges, copied textures and technically remapped models |
| `textures/gui/{dmfurnace,rmfurnace,relay1,relay2,relay3}.png` | Preserved runtime GUI art for the package; current native panels use the established Jugcraft tablet controls rather than the incompatible original slot layout |
| `sounds/item/nova.ogg` | Shared package sound registry; plays on a successful charge |

## Validation

`AlchemyMachinesGameTests` contains twenty targeted server cases: exact furnace costs and throughput; blocked output and modified inputs; conservative relay dissolution and transfer limits; front-face routing and claims; collector→relay→condenser conservation; pause/redstone/owner absence; guarded crop growth; retained artifacts; standing/wall support; hostile-only ward effects and shared guard/immunity rejection; Nova ownership, cost, block exclusions and single activation; real save/load; loot and smithing component preservation; ledger/menu authority; unsided automation denial; survival recipes/exclusions; and private save data versus sanitized public pedestal display updates.

The combined local validation passed: **1,886 required server tests**, 67 Python tests, full data/model/recipe and unchanged-owner-art audits, and rendered `CompleteAlchemyClientGameTests`. The client verified menus, inventory persistence, connected processing, pedestal display and worn armor. A final layout-only adjustment was rebuilt and checked again in the real client. See [complete alchemy](complete-alchemy.md#validation-and-local-playtest) for the exact sequence, fixes and evidence. Owner playtest and a two-client multiplayer check remain unperformed. Nothing was pushed.
