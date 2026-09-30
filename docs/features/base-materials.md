# Base materials

Status: implemented in source; **not yet played**. Compilation is checked by the Build workflow.
Proposal issue: #2 (appendix catalog); expanded to the full catalog at the owner's direction on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: shared materials; Discovery to Specialization
Primary specialty and supported player role: metallurgy and mining

## Player experience
The world now holds the raw materials later technology needs, found where they plausibly occur and drawn in vanilla style with real mineral colors. Tin and bronze are described in [tin-and-bronze.md](tin-and-bronze.md).

| Material | How the player gets it | What it gives now | Real-world basis |
| --- | --- | --- | --- |
| Tin | Tin ore, Y −32 to 96; stone pickaxe; furnace | Tin, bronze | Cassiterite |
| Zinc | Zinc ore, Y −16 to 80; stone pickaxe; furnace | Zinc ingots | Sphalerite (resinous amber specks) |
| Lead | Lead ore, Y −48 to 64; stone pickaxe; furnace | Lead ingots | Galena (lead-gray cubes) |
| Silver | Silver ore, rare, Y −64 to 32; iron pickaxe; furnace | Silver ingots | Native silver |
| Nickel | Nickel ore, Y −64 to 16; iron pickaxe; **blast furnace only** | Nickel ingots | Pentlandite (bronze-yellow) |
| Tungsten | Tungsten ore, rare, Y −64 to 0; iron pickaxe; **blast furnace only** | Tungsten ingots | Wolframite; too refractory for a plain furnace |
| Uranium | Uranium ore, very rare, Y −64 to −16; iron pickaxe; **blast furnace only** | Uranium ingots | Pitch-black uraninite with yellow-green crust |
| Bauxite / aluminum | Bauxite rock near the surface (Y 50–100) in jungle, savanna and badlands | Blast furnace: 1 bauxite → 1 aluminum nugget (**stand-in**) | Tropical weathering; real refining needs electrolysis |
| Salt | Rock salt ore, Y 0–64; drops 2–4 salt | Salt, salt blocks | Halite beds |
| Phosphate | Phosphorite ore, Y −16 to 48; drops 1–3 | Phosphate | Sedimentary phosphorite with apatite |
| Lithium | Lepidolite ore, Y −48 to 32; iron pickaxe; drops 1–2 | Blast furnace: lepidolite → lithium carbonate (**stand-in**) | Lithium mica (lilac) |
| Rare earths | Monazite ore, rare, Y −64 to 16; iron pickaxe | Blast furnace: monazite → rare earth oxide (**stand-in**) | Monazite; real separation is solvent extraction |
| Sulfur | **No new ore**: crafts from vanilla 26.2 sulfur (1 → 4 sulfur dust) | Sulfur dust | Reuses vanilla Sulfur Caves |
| Silicon | **No new ore**: blast vanilla quartz → silicon (**stand-in**) | Silicon | Carbothermic reduction of quartz |
| Brass, invar, solder | **Alloys only**, made in the alloy smelter (see machines-and-power.md) | Ingots, nuggets and blocks with `c:` tags | Brass = copper + zinc; invar = iron + nickel; solder = tin + lead |
| Crude oil | Oil sand in desert and badlands sand, Y 50–90; shovel; drops 1–2 bitumen | Bitumen | Oil sands. **Liquid crude oil is not added yet**: it needs a shared fluid system (pipes, tanks, refining) |

## Connections
- Existing input producer: Overworld generation, vanilla sulfur and quartz.
- Existing output consumer: none yet, apart from bronze. These are raw inputs for future machines, circuits (silicon, silver), batteries (lithium, lead, zinc), fertilizer (phosphate), fuels (crude oil) and power (uranium), and for magic (silver wards, salt circles, lead curse tablets).
- Stand-ins: aluminum, silicon, lithium carbonate and rare earth oxide are blast-furnace recipes that should move to real machines (electrolysis cell, arc furnace, separation) once those exist. Replacing a stand-in keeps the item IDs.
- Required vs optional: nothing requires another branch; every material is reachable with vanilla tools and furnaces.

## Balance and automation
All compaction is 9 ⇄ 1 and lossless. No recipe creates metal: `tools/check_mod_data.py` audits every recipe (the bauxite stand-in yields one nugget per block). Harder metals require an iron pickaxe and a blast furnace. Rarity decreases with usefulness: uranium, tungsten, monazite and silver are the rarest. Generation numbers are starting targets to tune after measurement.

Not added on purpose: radiation or other hazards (uranium needs its own design first), lead poisoning, new tool or armor tiers, vanilla recipe changes, and a sulfur-to-gunpowder recipe (a vanilla balance decision).

## Multiplayer and persistence
No block entities, screens, packets or tick logic. Every material has its own switch in `config/jugcraft.properties` (`tin`, `zinc`, `lead`, `silver`, `nickel`, `tungsten`, `uranium`, `aluminum`, `salt`, `phosphate`, `lithium`, `rare_earths`, `sulfur`, `silicon`, `crude_oil`). `false` stops that material's worldgen and recipes; registered blocks and items always remain. There is no retrogeneration, so existing chunks get none of the new ores.

## Dependencies and assets
Fabric API only. All 79 textures are original, drawn by `tools/generate_textures.py` from fixed seeds (MIT).

## Verification
Run in a sandbox without Minecraft (30 September 2026):
- `python3 tools/check_mod_data.py`: PASS for 79 IDs (models, loot, names, tags, worldgen, Java/config/worldgen sync, recipe audit). Negative test: a wrong worldgen switch in Java and a missing texture were both reported.
- `python3 scripts/check_repository.py`: PASS.
- `javac` without Minecraft: no syntax errors.
- A test class references `Items.SULFUR` and `Items.QUARTZ`, so the build fails if those vanilla IDs change.

Not run: client launch, dedicated server with two clients, worldgen sampling, performance. All of these need a machine with Minecraft.

## Rollout and open questions
- Liquid crude oil, refining and fuels need a shared fluid design.
- Uranium hazards need their own proposal.
- Stand-in recipes should be replaced by machines.
- Acquisition could stay disabled per material until consumers ship.
