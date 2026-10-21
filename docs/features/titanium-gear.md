# Titanium tools and armor

Status: combined integration of #390 and #419 against main; final validation and merge evidence are recorded on the integration PR.

## October 20 integration decisions

- Use #390's 3D knight armor, ten equipment items including the paxel, and 1,300-use / 8.0-speed tool balance. The armor gives 20 defense and 2.0 toughness. Titanium ingots feed the existing recipes and repair tag; this does not add a parallel titanium processing chain.
- Retain #419's all-piece durability/slot assertions, alongside #390's mining, armor, repair and crafting tests. Extend crafting coverage to the paxel and include it in the rendered icon gallery.
- Keep current main's shared tool silhouettes for bronze, steel and thallite. Titanium uses those same maps in its own palette; the older draft's global silhouette replacement is not applied.
- Keep one titanium item set and one worn model. #419's alternate flat armor and lower tool numbers are superseded, not registered as duplicate equipment. Both contributor branches are retained; no owner-library originals are modified.
- Regenerate tags, names, recipes, essence values and acquisition reports from the combined sources. Preserve all existing imported and generated content.

Integration and repairs: OpenAI Codex (GPT-6), under the owner's direct instruction. The original feature work was contributed using Claude Opus 5.5. No human playtest or two-client certification is claimed.

Proposal issue: none. TODO item: "Add aluminum's affordable bulk electrical/lightweight structural/panel/vehicle roles; titanium advanced process/precision equipment and vehicle components; and stronger titanium tools/armor above steel with balanced statistics." This change does the last part: titanium tools and armor.

**This change's choices:** the statistics and the look, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: after the titanium line (Kroll process), the step above steel
Primary specialty and supported player role: metals; any player who reaches titanium

## Player experience

Titanium ingots make a full set, crafted like vanilla's (three ingots and two sticks a pickaxe, eight a chestplate):

- **Tools:** a titanium sword, pickaxe, axe, shovel and hoe. They mine everything diamond does (obsidian included), last half again as long as steel's, dig at diamond's speed and hit as hard as diamond's.
- **Armor:** a titanium helmet, chestplate, leggings and boots. Worn, it is the knight armor (the steel's 3D design, [knight armor](knight-armor.md)) in a cool periwinkle titanium palette, with the steel's gold trim and leather.
- **Icons:** the material-set tool maps and the knight armor's icon maps, coloured from the titanium ingot's ramp, so they sit beside steel's and read a shade bluer and lighter.
- A titanium ingot mends any piece at an anvil.

## Statistics (for review)

| | Steel | **Titanium** | Diamond | Netherite |
| --- | --- | --- | --- | --- |
| Tool durability | 900 | **1,300** | 1,561 | 2,031 |
| Mining speed | 7.0 | **8.0** | 8.0 | 9.0 |
| Attack damage bonus | 2.5 | **3.0** | 3.0 | 4.0 |
| Tool enchantability | 12 | **15** | 10 | 15 |
| Drops | diamond | **diamond** | diamond | netherite |
| Armor durability multiplier | 25 | **30** | 33 | 37 |
| Defense (boots, leggings, chest, helmet) | 3, 6, 7, 3 | **3, 6, 8, 3** | 3, 6, 8, 3 | 3, 6, 8, 3 |
| Toughness | 1.5 | **2.0** | 2.0 | 3.0 |
| Knockback resistance | 0.05 | **0.05** | 0 | 0.1 |
| Armor enchantability | 10 | **14** | 10 | 15 |

Titanium improves on steel's tool durability, mining speed, damage and enchanting, with stronger armor and the same knockback resistance. It sits around diamond: a little less durable, with better enchanting. It is a reachable, renewable route to diamond-grade gear through the titanium chemistry, not a replacement for netherite. The axe deals 5 + 3 damage at diamond's −3.0 speed; the hoe is diamond's.

## Connections

- **Input producer:** titanium ingots from the titanium line (raw titanium → crude titanium chloride → purification → magnesium reduction → titanium sponge → arc furnace).
- **Output consumer:** the player; nothing else consumes the gear.
- **Shared systems:** `JugcraftGear`'s tier list and `tools/gear.py`'s `GEAR_TIERS` (items, recipes, tags, repair tag, language), the knight armor's 3D model (`tools/knight_armor.py`, a third `ArmorSet`), `tools/armor_icons.py` and `tools/material_icons.py`.
- **Paxel:** a titanium paxel, from a titanium pickaxe, axe and shovel, lasts as long as the three (3,900 uses) and counts toward the "Jack of All Trades" advancement.
- **Not done:** titanium arms (the arms kinds; `"arms": False` as for thallite) and an advancement of its own.
- **Feature switch:** `titanium`, as the titanium line itself.

## Multiplayer and persistence

- **New registrations,** stable from now on: `jugcraft:titanium_sword`, `_pickaxe`, `_axe`, `_shovel`, `_hoe`, `_paxel`, `_helmet`, `_chestplate`, `_leggings`, `_boots`; the item tag `jugcraft:repairs_titanium_gear`; the worn-model atlas `textures/entity/equipment/3d/titanium_knight.png`.
- Plain vanilla items: the 26.3 item properties do the work. The server decides everything.

## Dependencies and assets

There are no new dependencies. All art is drawn by this repository's scripts: no texture is copied or recoloured from Mojang's files.
- Tool icons: `tools/material_icons.py` (`TOOL_METALS`), the titanium ramp `2a3144 5e73a4 95a4c5 c5cddd f3f5f7`.
- Armor icons and worn model: `tools/armor_icons.py` and `tools/knight_armor.py`, painted from `armor_paint.TITANIUM`.
- The flat worn layers, which no equipment asset draws (as the steel's), are the steel's Kaiser fallback.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py` passed. The icons were inspected beside steel's at 8x.

**Server game tests for CI** (`TitaniumGearGameTests`):

| Test | What it checks |
| --- | --- |
| `titaniumToolsBeatSteel` | The pickaxe gets obsidian, lasts 1,300 uses, more than steel and less than netherite, and mines stone faster than steel; titanium hits harder. |
| `titaniumArmorBeatsSteel` | A set gives 20 armor, more than steel's, and more toughness; a titanium ingot mends it; every piece goes in its slot. |
| `titaniumGearIsCraftedFromIngots` | Three ingots over two sticks make the pickaxe; eight make the chestplate. |

**Client game tests:** `TitaniumGearClientGameTests` (a steel and a titanium knight on armor stands, front and three-quarter, and the nine icons in frames) and `KnightArmorClientGameTests`, which now also checks the client read the titanium pieces' worn models.

### Results

Fork CI run 38774406665 on `1ee5f7a6`: the mod build, generated-data and material checks, all server game tests (these three included), the server tests without optional integrations, and all five client shards passed. Client shard 3, which holds none of these tests, hit the 60-minute job limit on the first attempt and passed on its one re-run.

**Historical contributor tool shapes (not adopted in the combined integration).** The contributor redrew the shared sword, pickaxe, axe and hoe maps in vanilla's layout (rule 9 of [ITEM_ICONS.md](../ITEM_ICONS.md)), affecting every metal. The October 20 integration instead retains current main's shared maps and existing metal textures; titanium uses those established shapes in its own palette.

Fork CI run 38797887571 on `842d6ef0` (the redrawn tool maps) passed every job after one re-run of a shard-3 time-out. Fork CI run [38817959827](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38817959827) on `2f9c9430` (the titanium paxel, checked in `TitaniumGearGameTests`) passed every job on the first attempt.

**Not yet done:** a survival playtest and a two-client test.

## Rollout and open questions

- The statistics await the owner's review.
- Titanium arms could follow.
