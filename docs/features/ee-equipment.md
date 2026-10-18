# Matter equipment and material storage

Owner-directed local implementation, 17 October 2026. Implemented with **OpenAI Codex (GPT-6)** as part of the remaining Equivalent Exchange-inspired owner assets. It remains on the local feature branch pending the owner's playtest and publication decision.

## Player outcome

The extension supplies all four missing Dark Matter tools; all nine Red Matter tools including Katar and Morning Star; twelve Dark Matter, Red Matter and Gem armor pieces; Red Matter and Iron Band; and five reversible storage blocks. The existing Dark Matter Pickaxe, Axe and Hammer retain their IDs and implementation.

All equipment has reachable crafting or smithing recipes. Three Dark Matter and six Aeternalis Fuel make Red Matter. Eight iron ingots make an Iron Band. Dark Matter armor follows the ordinary armor crafting shapes. High Covalence Dust is the smithing template for upgrading a Dark Matter piece with Red Matter, and then Red Matter armor with a diamond block into Gem armor. Smithing preserves the base item's name, enchantments, damage and mode. High Covalence Dust repairs the equipment in a normal anvil.

Katar combines the four Red Matter sword/axe/hoe/shears, four Red Matter and High Covalence Dust. Morning Star combines the Red Matter pickaxe/shovel/hammer, four Red Matter, Iron Band and High Covalence Dust. These two ordinary crafting combinations make new equipment; ingredient names and enchantments are not transferred. They do not consume or create essence.

## Tool interactions

Tools start in single-target mode. Use in air or sneak-use a block to select the extra mode; sneaking while mining always limits the operation to the normal block. The actual player break path supplies normal drops, enchantments, durability and protection callbacks. Extra work requires understood Assay and the existing personal essence balance. No new energy or casting system is introduced.

| Tool | Extra operation | Cost and bound |
| --- | --- | --- |
| Red Pickaxe | Same-ore vein or tunnel | 8 essence per extra; 16 connected ore or four extra tunnel blocks |
| Red Axe | Upward log felling | 8 essence per extra; at most 24 same-kind logs |
| Dark/Red Shovel | Soil mining face | 8 essence per extra; eight extra blocks |
| Dark/Red Hoe | Ordinary tilling over 3x3 | 8 essence per successful extra tile; eight extra tiles |
| Dark/Red Shears | Flock shearing or connected leaves | 8 essence per extra; six sheep or eight/sixteen leaves |
| Dark/Red Sword | Nearby hostile sweep | 16 essence per affected creature; at most four, for two/four damage |
| Red Hammer | Mining face | 12 essence per extra; eight extra blocks |
| Red Katar | Axe/hoe/leaf mining, ordinary stripping, tilling and sheep shearing; felling or hostile sweep modes | 16 essence per extra log or swept hostile creature |
| Red Morning Star | Pickaxe/shovel mining and ordinary path making; face or tunnel modes | 12 essence per extra; eight face blocks or four tunnel blocks |

Extra mining excludes block entities, fluids, harder materials and protected or unloaded blocks. A per-player recursion guard prevents the extra breaks triggering a second expansion. Connected searches visit at most 192 candidates. Balance is reserved before a change and refunded on refusal. Sheep checks protect the animal and its location. Hostile sweeps exclude players, pets and villagers entirely and go through `ConcordanceEffects` for shared damage tolerance, wards and permissions. Extra actions stop when the held tool breaks.

## Armor

All suits offer 20 armor points with ordinary durability, damage, enchantments and repair. Dark/Red/Gem toughness is 3/4/5; durability multipliers are 64/80/96. A complete enabled Dark Matter suit provides Resistance I for 8 essence per renewal. Red Matter or Gem suits cost 12 for that same Resistance I. A complete Red Matter suit supplies Fire Resistance while burning for 8 essence.

Gem pieces also work independently: the helmet grants Water Breathing underwater for 4 essence; chestplate Fire Resistance while burning for 8; leggings Speed I while sprinting for 4; boots Slow Falling once fall distance exceeds two blocks for 4. No armor flight permission is changed. Wards last 60 ticks and renew only at 40 ticks or less. Ordinary wards are checked every 20 ticks; Gem boots check each tick so a short fall can receive protection before landing, without charging again for a fresh ward. Existing stronger/longer effects are preserved without charging. They require understood Assay and available essence; ordinary armor protection remains functional without either. Sneak-use a piece while held to toggle its paid wards. The saved active component survives upgrades and item storage.

## Materials, integration and assets

Dark Matter, Red Matter, Alchemical Coal, Mobius Fuel and Aeternalis Fuel compress nine-to-one and unpack to nine. Fuel blocks burn for exactly nine times their constituent fuel's duration. All 32 additions are excluded from equivalence to avoid transmuting special equipment or generating conversion cycles. Recipes are conditional on Concordance; registrations remain stable when disabled.

The canonical source is `tools/concordance_ee_equipment.py`, which exports all IDs, models, recipes, translations, shared equipment/repair tags, equivalence exclusions and codex pages. Global integration merges its equipment tags through the existing sole writer and includes the tool IDs in Spell Engine's opt-out list. No library versions or optional APIs change.

Art comes directly from `art/owner-library/originals/Magic/`: `textures/item/dm_tools`, `rm_tools`, `dm_armor`, `rm_armor`, `gem_armor`; `textures/item/matter/red.png`; `textures/item/rings/iron_band.png`; the five corresponding `textures/block/*_block.png` files; and `textures/models/armor/{dark_matter,red_matter,gem_armor}_layer_{1,2}.png`. `OWNER_FILES` records every runtime texture mapping. PNG pixels are unchanged. Worn layers use the native 26.3 equipment asset schema. The supplied ten trim item models per piece are adapted only by replacing the base texture namespace and expressing their old predicates as the current `minecraft:trim_material` select model. Originals and provenance notices remain untouched. New recipe, gameplay and guide text belongs to Jugcraft.

## Validation

The combined local validation passed: **1,886 required server tests**, 67 Python tests, full data/model/recipe and unchanged-owner-art audits, and rendered `CompleteAlchemyClientGameTests`. The client verified menus, inventory persistence, connected processing, pedestal display and worn armor. A final layout-only adjustment was rebuilt and checked again in the real client. See [complete alchemy](complete-alchemy.md#validation-and-local-playtest) for the exact sequence, fixes and evidence. Owner playtest and a two-client multiplayer check remain unperformed. Nothing was pushed.
