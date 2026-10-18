# Dark Matter workshop — local alchemy follow-up

> Publication update, 18 October 2026: the owner accepted the combined alchemy presentation and authorized publishing the alchemy additions and UI fixes. Historical local-only checkpoints below are superseded. Current scope and validation are recorded in [Alchemy interfaces and access](alchemy-interface-and-access.md).

Owner accepted this package on 17 October 2026. Implementation: OpenAI Codex (GPT-6). Built on the local alchemy tablet branch; publication and owner playtest are pending.

## Player outcome and integration

Turn ordinary materials into dense furnace fuels and durable utility tools. Use the existing crafting table, furnace and anvil, with the existing personal essence ledger for extra mining. No new machine, currency, research tree or spellcasting requirement is introduced. Crafting and ordinary single-block mining work without research; paid extra blocks require understood Assay, just like the existing alchemical equipment. The encyclopedia route is **Magic → Equivalence → Dark Matter Workshop**.

| Output | Crafting inputs | Immediate use |
|---|---|---|
| 4 Low Covalence Dust | Cobblestone + charcoal | Alchemical Coal |
| 4 Medium Covalence Dust | Iron ingot + redstone | Mobius Fuel |
| 4 High Covalence Dust | Diamond + lapis | Aeternalis Fuel and Dark Matter tool repair |
| Alchemical Coal | 4 coal + Low Covalence Dust | 32 normal furnace smelts |
| Mobius Fuel | 2 Alchemical Coal + Medium Covalence Dust | 64 normal furnace smelts |
| Aeternalis Fuel | 2 Mobius Fuel + High Covalence Dust | 128 normal furnace smelts and Dark Matter |
| Dark Matter | 8 Aeternalis Fuel surrounding a diamond block | Three tools |
| Dark Matter Pickaxe | 3 Dark Matter + 2 sticks in the usual pickaxe shape | Single block, ore vein, short tunnel |
| Dark Matter Hammer | 5 Dark Matter across the top and beside the central stick, 2 sticks down the centre | Single block, 3×3 face |
| Dark Matter Axe | 3 Dark Matter + 2 sticks in the usual axe shape | Single block, fell logs |

Fuel durations use Minecraft 26.3's cooking-fuel component at normal cooking speed. Dense fuels preserve the coal's total burn time rather than multiplying energy. These ten items are deliberately excluded from the ordinary equivalence catalogue, so advanced material progression cannot be bypassed through the tablet. They can be traded normally. No existing recipes are replaced.

## Controls, costs and safeguards

Use in air to cycle modes, or sneak-use a block. The action bar reports the selected mode and extra-block price; the tooltip lists controls. Modes persist on the tool. **Sneak while mining** temporarily limits it to a single block. Tools default to single block.

- Pickaxe: up to eight extra face-connected blocks of the same ore, or two extra blocks straight ahead, at **8 essence each**. A tunnel stops at the first obstacle.
- Hammer: up to eight extra blocks in one 3×3 face perpendicular to view direction, at **16 essence each**. Looking steeply up/down gives a horizontal face. Start at the centre.
- Axe: up to sixteen extra connected logs of the same type, sideways/upward including diagonals, at **8 essence each**. Leaves and roots below the first log stay. Placed logs are eligible too; use precision mode around timber buildings.

All have 4,096 durability and netherite-level mining capability. Ordinary enchantments and loot apply to each real break, including Silk Touch. Each block consumes normal durability; the first costs no essence. High Covalence Dust repairs up to 1,024 durability per unit through the ordinary anvil and XP rules, preserving names, enchantments and modes. Existing Repair Talismans can also repair these tools.

Server authority owns modes, ledger spending and mining. Extras call the player's normal block-break path with shared claim checks, normal protection callbacks and refund on a refused break. No recursion, chunk loading, container breaking, fluid-bearing extra blocks, harder-neighbour shortcut or overdraft. Extra work stops on tool breakage. Empty essence, missing Assay or a disabled Concordance leaves ordinary mining usable. Connected searches have fixed bounds and a 192-position scan cap; creative mining stays ordinary. Terrain and dropped items use normal world persistence; only the tool's bounded integer mode is new saved state.

The existing `gear.py` writer owns shared vanilla equipment tags. Dark Matter contributes entries there; a generator replay audit protects all pre-existing equipment membership from accidental overwrite. Spell Engine opt-outs follow the existing Jugcraft weapon/tool convention.

## Owner art

All ten icons are byte-for-byte imports from `art/owner-library/originals/Magic/`:

- `textures/item/covalence_dust/{low,medium,high}.png`
- `textures/item/fuels/{alchemical_coal,mobius,aeternalis}.png`
- `textures/item/matter/dark.png`
- `textures/item/dm_tools/{pickaxe,hammer,axe}.png`

Runtime paths are `assets/jugcraft/textures/item/<item_id>.png`. `tools/concordance_dark_matter.py` maps the files; `tools/owner_art.py` verifies the imports. Runtime item models use Minecraft's generated/handheld parents. Originals and notices are retained. No asset-library program was executed. Gameplay code is Jugcraft's own implementation of the selected alchemy ideas; no external mod is added.

## Validation and delivery

The server suite exercises actual crafting, furnace ticks, bounded mining/costs, scarce essence, precision, protected/container/harder blocks, tunnel barriers, connected ore/tree limits, breakage, anvil repair, mode persistence, disablement/research, normal enchanted loot and transmutation exclusion. A separate disposable rendered client test exercises real mode-switch input, icons/held models and server mining, then restores a prepared workshop for independent owner play.

Results on 17 October 2026:

- All **1,787 required server game tests passed**, including **16 new Dark Matter cases**. The initial run caught an incomplete anvil test fixture: unlike the real client, it left the name field empty and therefore requested name removal. The corrected fixture sends the displayed name; the full rerun passed.
- Full data/recipe/art audit passed: **2,926 material IDs**, 8,516 block/item models, 3,709 quad parts and 171 combined models. It also checks the shared equipment tag writer against committed output.
- Scoped canonical replay matched 30 JSON outputs and 18 feature language entries. All ten icons match the owner originals byte for byte; the full owner import check passed. The built JAR matched all 45 Dark Matter resource files.
- Repository documentation/structure and platform-lock checks passed, along with all nine packaging unit checks.
- The rendered client test passed, including real input selecting modes, client/server mode synchronization, a real nine-block hammer break and the exact 128-essence charge. Inventory icons and all three held models were inspected. A second client pass allowed the equip animation to settle for clean pickaxe/axe captures; it passed too.
- The independent **Jugcraft Dark Matter Workshop** save supplies all ten items, understood Assay, 100,000 essence, XP, recipes' ingredients, a damaged tool and labeled mining/furnace/repair stations. The local Modrinth package includes this save and instructions. Automated tests operate only their disposable world; the owner controls manual play. Previous manual saves remain intact.

No owner playtest or multi-person manual multiplayer session is claimed. Red Matter, armor, stars, collectors and condensers are not part of this package.
