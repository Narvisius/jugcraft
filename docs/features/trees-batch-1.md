# Trees batch 1: northern, wet and cool-forest trees

Status: implemented on branch `claude/trees-batch-1`, awaiting review. Built and tested in CI (PR #206); **not yet played**. See Verification for exactly what has run.
Proposal issue: none. The owner, 5 October 2026: "start batch 1 of the new trees", from the tree roster ([branches/TREES.md](../branches/TREES.md)). After the roster was written they added: "We are ok for making new wood and logs for a bunch as well, if we need them just alter the ones we have but make sure they are made the same way so they fit the world!!! Unless they are supposed to be the same wood."
Owner: @jimbozoomer-byte
Target milestone and tier: world generation and building (Discovery), as the rest of the biomes branch ([seasonal-forests.md](seasonal-forests.md)).
Primary specialty and supported player role: exploration and building; every player meets them.

## Player experience
Twenty-nine of Jugcraft's biomes change their trees: twenty gain new trees, and nine only swap trees the mod already has. Nine of the ten new trees are new shapes of woods the mod already has, so their logs and planks are their parent's wood. The tenth, the swamp cedar, is a new wood: **cedar**.

| Tree | What it is | Where it grows (share of the biome's tree tries) |
|---|---|---|
| **Stunted fir** | a young balsam fir: a squat cone with needles nearly to the ground | Snowy Coniferous Forest 10%, Coniferous Forest 8% |
| **Bog fir** | black spruce drawn on fir: a pencil-thin, ragged spire with a tuft at its top | Fen 50%, Muskeg 30%, Bog 15% |
| **Subalpine fir** | a tall, narrow spire over a short bare bole | Snowy Coniferous Forest 20%, Aspen Glade 5% |
| **Fir bush** | krummholz: one fir log in a low, dark mound of needles | Tundra 20% |
| **Tamarack** | a thin larch pole with a small tuft at its top: gold in autumn, bare in winter | Muskeg 35%, Fen 35%, Bog 30% |
| **Dead snag** and **bent snag** | straight grey spars broken off at the top; one in four has a crooked top | Ghost Forest 55%, Dead Swamp 50%, Burnt Forest 45%, Wasteland 40%, Dead Forest 35%, Rotted Expanse 30%, Muskeg 20%, Coniferous Forest 3%; the Cinder Barrens' only trees |
| **Willow bush** | shrub willow: a low, round mound of willow leaves, yellow in autumn | Floodplain 70%, Lush River 70%, Tundra 30%, Fen 15% |
| **Young aspen** | a thin, slightly leaning white pole with a small crown at its bend | Hallowed Bog 30% (standing in its water), Aspen Glade 15% |
| **Cedar** | the swamp cedar: a clear, stringy red-brown bole under a dense, narrow cone | Wetland 50%, Ghost Forest 15% |
| **Mossy maple** | the bigleaf maple of temperate rainforests: a big spreading maple with moss on its limbs, hung with vines | Temperate Rainforest 11%, Redwood Forest 5% |

**Existing trees move too** (the roster's free swaps):
- firs replace vanilla spruces in the Maple Woods, Redwood Forest and Lake District, and join the Seasonal Forest;
- larches join the Coniferous Forest, Shield and Lake District;
- aspens join the Snowy Forest and replace the Shield's oaks and the Snowpetal Grove's birches;
- fallen dead logs lie in the Dead Swamp, Burnt Forest and Rotted Expanse, and fallen larch logs in the Muskeg;
- willows join the Lush River, maples the Lush Swamp, vanilla's azalea trees the Swamp Woods, and a rare great oak the Dense Forest;
- the Wasteland gets a tree try in every chunk (it had one in ten chunks).

Every biome's full list, before and after, is in "World and event applicability" below.

**The cedar wood.** A full wood set like the others: cedar log, wood, stripped log and wood, planks, stairs, slab, fence and fence gate, with cedar leaves and a cedar sapling, which grows a cedar. Its colours are the owner's own painting of western red cedar (their second set, row 0): the wood a muted red-brown, darkened a little so that it does not sit on vanilla spruce or the cypress, and the reddish-brown bark darkened with it, so the log keeps the painting's contrast between bark and wood; the bark is drawn stringy. Its leaves are flat sprays of sage-green scale-leaves, yellower than the cypress's blue-green, and stay green all year.

## The trees
Every tree below is world generation only, except the cedar, which also grows from its sapling. None has a tier, costs anything or unlocks anything: they are scenery and wood. Each is a `trees.SHAPES` entry in `tools/trees.py`, generated to `worldgen/feature/<shape>.json` and `worldgen/placed_feature/<shape>_checked.json`, and each pick checks that the tree's sapling could stand there. Shares are of the biome's tree tries. "Trunk 4 (+3)" is `base_height` 4 and `height_rand_a` 3.

### Stunted fir and bog fir (`stunted_fir`, `bog_fir`)
- **Species:** young or suppressed balsam fir (*Abies balsamea*), and black spruce (*Picea mariana*) drawn on fir.
- **Biomes and shares:**
  - stunted fir: Coniferous Forest 8%, replacing vanilla spruce; Snowy Coniferous Forest 10%;
  - bog fir: Fen 50%, as its default ("short firs"), replacing the full-size fir; Muskeg 30%, replacing the full-size fir; Bog 15%.
- **Shape:** both a straight trunk 4 (+3), so the top log is 4-7 blocks up, under spruce foliage.
  - Stunted: radius 1-2, offset 0-1, trunk height 0-1: a squat cone with needles nearly to the ground.
  - Bog: radius 0-1, offset 0-1, trunk height 2-3: a pencil-thin, ragged spire with a tuft at its top, like black spruce's "club top".
- **Wood:** fir's logs and needles; no new blocks.
- **Why:** the Muskeg and the Fen get the short, thin firs of real muskegs and fens, and the conifer forests get an understory, for two shapes and no textures.
- **Producer and consumer:** world generation in Jugcraft regions; fir logs and needles, as the fir's.
- **Failure:** a pick where a fir sapling could not stand places nothing.
- **Test evidence:** `BiomeGameTests.batchOneShapesGrow` and `batchOneShapesNeedTheirSoil`; `WoodClientGameTests`' shape shots. Written for CI; not run (see Verification).

### Subalpine fir (`subalpine_fir`)
- **Species:** subalpine fir (*Abies lasiocarpa*), in its spire form.
- **Biomes and shares:** Snowy Coniferous Forest 20%, from the fir's share; Aspen Glade 5%, where it keeps the glade green in winter.
- **Shape:** straight trunk 10 (+4), so the top log is 10-14 blocks up, under spruce foliage (radius 1-2, offset 1-2, trunk height 3-4). A short bare bole shows, and the spire flares a little at its foot, so it does not read as the cypress's column.
- **Wood:** fir's.
- **Why:** narrow "snow ghost" spires are the silhouette of snowy high forests, and a few dark spires among gold aspens are the Rocky Mountain view.
- **Producer and consumer:** world generation; fir logs and needles.
- **Failure:** as the stunted fir. **Test evidence:** as the stunted fir.

### Fir bush (`fir_bush`)
- **Species:** krummholz, wind-stunted fir.
- **Biomes and shares:** Tundra 20%.
- **Shape:** one fir log under bush foliage (radius 2, offset 1, height 2): a low, dark, wind-pressed mound.
- **Wood:** fir's.
- **Why:** it keeps some green in the Tundra in winter, when its maple and willow scrub is bare.
- **Producer and consumer:** world generation; fir logs and needles.
- **Failure:** as the stunted fir. **Test evidence:** as the stunted fir.

### Tamarack (`tamarack`)
- **Species:** tamarack (*Larix laricina*).
- **Biomes and shares:** Muskeg 35%, as its default, from the dead tree's share; Fen 35%, replacing vanilla dark oak; Bog 30%, over its red-orange grass.
- **Shape:** straight trunk 6 (+3), so the top log is 6-9 blocks up, under vanilla's pine foliage: radius 1, offset 1, **height 3**.
  - The roster asked for height 4. By our reading of vanilla's pine placer, which widens the crown with the trunk's bare length, height 4 would make about half the tamaracks 7 blocks wide, wider than the larch cone.
  - Height 3 keeps every tamarack 5 wide or less, and about one in five 3 wide. This reading is from memory of vanilla's code, not checked against 26.3; the CI screenshots will show it.
- **Wood:** larch's logs and needles (green, then gold from about day 268, bare from about day 318). The larch sapling still grows Alpine Spawn's larch cone.
  - Larch joins the fallen logs (`FALLEN`, length 5-8): fallen larches are 2% of the Muskeg's trees.
  - Larch now grows in Jugcraft's biomes as well as Alpine Spawn, so its recipes follow either switch (see Multiplayer and persistence).
- **Why:** the defining tree of muskegs and fens, gold spikes over a frozen bog in autumn, for one shape.
- **Producer and consumer:** world generation; larch logs and needles.
- **Failure:** a pick where a larch sapling could not stand places nothing.
- **Test evidence:** as the stunted fir, and `BiomeGameTests.fallenLarchesLie`.

### Dead snag and bent snag (`dead_snag`, `dead_snag_bent`)
- **Species:** standing dead snags, killed by fire, beetles or flood.
- **Biomes and shares:**

  | Biome | Snags (straight, bent) |
  |---|---|
  | Ghost Forest | 55% (41.25, 13.75), its default |
  | Dead Swamp | 50% (37.5, 12.5), standing in its ponds |
  | Burnt Forest | 45% (33.75, 11.25) |
  | Wasteland | 40% (30, 10) |
  | Dead Forest | 35% (26.25, 8.75) |
  | Rotted Expanse | 30% (22.5, 7.5) |
  | Muskeg | 20% (15, 5) |
  | Coniferous Forest | 3% (2.25, 0.75) |
  | Cinder Barrens | all its trees (75, 25), on its coarse dirt |

  The roster's Gloomweald snag (10%) waits for batch 3, which gives the Gloomweald its own canopy.
- **Shape:** both have no leaves, through the dead tree's leafless path (air foliage).
  - Straight: trunk 3 (+4, +3), a grey spar 3-10 blocks tall, broken off at the top.
  - Bent: vanilla's bending trunk 4 (+3), bend length 1, giving one crooked top. Every snag share is split three straight to one bent, as the roster's "about a quarter".
  - The bent snag's air "leaves" start at height 8 (`min_height_for_leaves`), above its tallest trunk, as the young aspen's do. By default the bending trunk gives every trunk log from height 1 a crown, and by our reading of vanilla's placers that crown's air could replace a water block beside a low bend standing in the Dead Swamp's 2-deep water, leaving a hole in the pond. Only the bend keeps its crown, at height 4 or more. Not seen in game.
- **Wood:** dead wood's grey logs. Dead wood has no sapling, so both stand where an oak sapling could, as the dead tree does: on soil and coarse dirt, never on stone, calcite or tuff.
- **Why:** real dead stands are mostly straight snags. A second dead silhouette makes the dead biomes read as killed forests, not a scatter of identical props, and gives the Ghost Forest its colonnade.
- **Producer and consumer:** world generation; dead logs, which have no other source than the dead biomes' trees and fallen logs.
- **Failure:** a pick on other ground places nothing.
- **Test evidence:** as the stunted fir; `batchOneShapesNeedTheirSoil` also places the snags on coarse dirt (grows) and on calcite and tuff (nothing).

### Willow bush (`willow_bush`)
- **Species:** shrub willow (*Salix*): arctic, bog and riverbank scrub.
- **Biomes and shares:** Floodplain 70%, as its default, replacing the oak bush; Lush River 70%, as its default, replacing the oak bush; Tundra 30%; Fen 15%, as carr scrub.
- **Shape:** straight trunk 1 (+1) under bush foliage (radius 2, offset 1, height 2): a low, round mound of willow leaves with no hanging curtains.
- **Wood:** willow's logs and leaves (green, then yellow from about day 283, bare from about day 328).
- **Why:** it replaces vanilla-oak scrub with the shrub that really grows where floods reach, and it turns with the seasons where the oak bush does not.
- **Producer and consumer:** world generation; willow logs and leaves.
- **Failure:** a pick where a willow sapling could not stand places nothing. **Test evidence:** as the stunted fir.

### Young aspen (`young_aspen`)
- **Species:** quaking aspen suckers (*Populus tremuloides*), and the bog birch form.
- **Biomes and shares:** Hallowed Bog 30%, replacing the vine-hung oak, in water up to 2 deep; Aspen Glade 15%.
- **Shape:** vanilla's bending trunk 4 (+2), bend length 1, under a small blob crown (radius 2, offset 0, height 3). Leaves start only from height 7 (`min_height_for_leaves`), above its tallest straight trunk, so only the bend carries a crown, not a stack of blobs. A thin, slightly leaning white pole, about 6-7 blocks tall.
- **Wood:** aspen's logs and leaves, with their seasons.
- **Why:** trees of mixed ages make the glade read as one living clone, and white trunks make the Hallowed Bog pale.
- **Producer and consumer:** world generation; aspen logs and leaves.
- **Failure:** a pick where an aspen sapling could not stand places nothing. **Test evidence:** as the stunted fir.

### Cedar (`cedar`): a new wood from the owner's painting
- **Species:** the swamp cedar: Atlantic white cedar (*Chamaecyparis thyoides*) and northern white cedar (*Thuja occidentalis*).
- **Biomes and shares:** Wetland 50%, as its default, replacing vanilla spruce; Ghost Forest 15%, replacing vanilla dark oak as the living fringe beside its dead trunks.
- **Shape:** straight trunk 7 (+3), so the top log is 7-10 blocks up, under spruce foliage (radius 1-2, offset 0-1, trunk height 3-5): a clear, stringy red-brown bole under a dense, narrow cone, so it reads as a cedar, not another small fir.
  - The roster called the shape `swamp_cedar`. It is named `cedar` because a tree's sapling grows the shape of its own name (`JugcraftAgriculture.grower`), and the tree farm and giant saplings key on that name too. No `swamp_cedar` ID exists.
- **Wood:** its own, **cedar**, a new wood set. The roster had put the swamp cedar on cypress logs; the owner's update allows a new wood for a tree that is not the same wood, and a cedar is not a cypress. Its colours come from the owner's banked painting of western red cedar (second set, row 0):
  - wood from the painting's `#84654d`, darkened to `#725543` so that it stands apart from vanilla spruce and our cypress;
  - bark from the painting, darkened by the same lightness ratio so that bark and wood keep the painting's contrast, and drawn stringy, as cedar bark peels in long fibrous strips;
  - leaves in a new sage-green ramp of their own, evergreen.

  The full colour record, with every distance, is under Dependencies and assets.
- **Why:** the real conifer of cedar swamps, replacing vanilla spruce and dark oak, and keeping the wetland green beside its bare winter willows.
- **Producer:** world generation; the cedar sapling, which cedar leaves drop as spruce leaves drop theirs (1 in 20, more with Fortune, and sticks).
- **Consumer:** building (the cedar wood set); the sawmill; the tree farm; vanilla's wood recipes and fuel through vanilla's tags (`logs_that_burn`, `planks`, `wooden_stairs`, `wooden_slabs`, `wooden_fences`, `fence_gates`).
- **Costs:** as any wood: 4 planks a log by hand, 6 in the sawmill. **Unlocks:** none.
- **Failure:** a pick where a cedar sapling could not stand places nothing. A sapling grows only with room, as vanilla's do.
- **Test evidence:** `BiomeGameTests.cedarSaplingsGrow`, `cedarWoodWorksLikeWood` and `woodRecipesFollowAnyOfTheirSwitches`; the shape tests above; `WoodClientGameTests`, which grows the cedar with the other trees and again beside vanilla's spruce, and puts its wood on the sample wall beside vanilla's spruce wood. Written for CI; not run.

### Mossy maple (`mossy_maple`)
- **Species:** bigleaf maple (*Acer macrophyllum*).
- **Biomes and shares:** Temperate Rainforest 11%, replacing the vine-hung oak; Redwood Forest 5%.
- **Shape:** fancy trunk 9 (+4) under fancy foliage (radius 2, offset 4, height 4), as the big maple, about 10-14 blocks tall.
  - Vanilla's `attached_to_logs` decorator lays moss carpet on the upper faces of its limbs (probability 0.35).
  - Vanilla's vine decorators hang trunk vines, and leaf vines at 0.3.
- **Wood:** maple's logs and leaves (green; red, orange and gold; bare). The moss carpet and vines are vanilla's blocks.
- **Why:** the temperate rainforest's own moss-draped broadleaf in place of a vanilla oak, so the "dripping" rainforest finally has moss on its trees.
- **Producer and consumer:** world generation; maple logs and leaves, moss carpet and vines.
- **Failure:** a pick where a maple sapling could not stand places nothing.
- **Test evidence:** as the stunted fir; `batchOneShapesGrow` also counts the moss carpet and vines of four mossy maples.

## Connections
- Existing input producer: world generation, in Jugcraft regions and the End's Rotted Expanse; the cedar sapling.
- Existing output consumer: building; the sawmill and tree farm; vanilla's wood recipes and fuels through vanilla's tags.
- Technology connection: the sawmill saws cedar logs and the tree farm grows cedar saplings, like every wood.
- Magic connection: none.
- Reachable entry path (no circular unlock): every tree generates in a common Jugcraft biome with nothing to unlock; the cedar sapling drops from cedar leaves, so cedar is renewable.
- Required vs optional; trade and solo routes: nothing is required. Every wood here can be cut by any player, or traded.
- How it stays useful alone: scenery and building wood, independent of every other branch.
- Resource links: nine of the trees reuse existing woods, so they need no link beyond their parent wood's. Only the cedar adds a wood, linked as every wood is.

## Balance and automation
- **The cedar is ordinary wood:** 4 planks a log by hand; in the sawmill, 6 planks a log and sawdust half the time; in the tree farm, 6 cedar logs a sapling, the sapling back, and a stick one time in ten, as for every tree. Its leaves drop saplings and sticks as spruce leaves do, and its sapling composts like every sapling. No conversion loop: the tree farm is a renewable growth recipe, like every tree's (400 ticks and the machine's energy for 6 logs and the sapling back; `tools/machines.py` marks it renewable), and no recipe turns cedar wood back into saplings or logs.
- **The nine shapes add no blocks:** their logs and leaves are the existing woods', so they change no wood's numbers. The only recipe change for existing woods is the switch list (below).
- **Ticking:** no new ticking block. The tamarack, willow bush, young aspen and mossy maple bring more of the existing seasonal leaves (random ticks only, catching up at most 128 touching leaves when one changes, as before). Cedar leaves are evergreen leaves like the fir's.
- **World generation work:** the Cinder Barrens gains one tree try a chunk (two in one chunk in ten), and the Wasteland one a chunk instead of one in ten chunks. The other biomes keep their numbers of tries.

## Multiplayer and persistence
- **Server authority:** world generation, sapling growth and the season clock are the server's. There is no new block entity, packet, screen or client code. The cedar's blocks are registered by the same `JugcraftAgriculture.registerTree` and `registerWoodSet` as every other Jugcraft wood, and its leaves are evergreen leaves like the fir's.
- **Feature switches (TREES.md rule 4).** A wood's hand and sawmill recipes (and any recipe making its tree's sapling or leaves) are on whenever any switch that grows its logs is on.
  - `agriculture.WOOD_SWITCHES` is now a list for every wood, and `jugcraft:feature_enabled` takes an optional `"or"` list: `{"condition": "jugcraft:feature_enabled", "feature": "alpine_spawn", "or": ["biomes"]}` loads when either is on. A one-switch list writes exactly the JSON it wrote before.
  - Larch: `alpine_spawn` or `biomes` (the larch, the tamarack and the fallen larch grow in Jugcraft's biomes too). Its recipes stop only when both are off.
  - Chestnut: `agriculture` or `biomes` (the Orchard places chestnut trees under the biomes switch).
  - Cedar and every other wood: `biomes`.
  - `check_mod_data` now checks:
    - that every switch placing a shape, fallen log or placed tree of a wood is in that wood's list: the biomes' trees and extras, Alpine Spawn's selector, and agriculture's wild patches (`JugcraftAgriculture.registerWorldgen`, which places the wild chestnut trees);
    - that every hand recipe's switches match its result's;
    - that every Jugcraft wood's sawmill recipe carries `machines` and its wood's list.

    The tree farm's recipes follow `machines` alone, by design, and are not checked against the list.
- **`biomes.enabled=false`** stops every one of these trees in new chunks (the Rotted Expanse's too, with the End biomes), and the cedar's hand and sawmill recipes. The tree farm's cedar recipe follows `machines` alone, as every tree farm recipe does. Every block and item stays registered, so old chunks and inventories keep them.
- **New IDs, permanent once released:**
  - blocks and items `jugcraft:cedar_log`, `cedar_wood`, `stripped_cedar_log`, `stripped_cedar_wood`, `cedar_planks`, `cedar_stairs`, `cedar_slab`, `cedar_fence`, `cedar_fence_gate`, `cedar_leaves` and `cedar_sapling`;
  - the block and item tag `jugcraft:cedar_logs`;
  - features `jugcraft:<shape>` and placed features `jugcraft:<shape>_checked` for `stunted_fir`, `bog_fir`, `subalpine_fir`, `fir_bush`, `tamarack`, `dead_snag`, `dead_snag_bent`, `willow_bush`, `young_aspen`, `cedar` and `mossy_maple`;
  - the feature and placed feature `jugcraft:fallen_larch_tree`, and `jugcraft:trees_cinder_barrens`;
  - the recipes `cedar_planks`, `cedar_wood`, `stripped_cedar_wood`, `cedar_stairs`, `cedar_slab`, `cedar_fence`, `cedar_fence_gate`, `sawing/cedar_logs` and `tree_growing/cedar_sapling`.
- **Existing worlds:** new chunks only. Old chunks keep their trees, so a biome's trees can change at the border of chunks generated before the update. No saved ID is renamed or removed, so no migration or backup step is needed beyond the usual backup before any update.

## Dependencies and assets
- **No new dependencies.**
- **Textures**, drawn by code from fixed seeds by `tools/wood_style.py` (`WOOD`, `BARK`, `LEAVES`) and `tools/wild_textures.py` (the sapling), in the manner of [NATURAL_TEXTURES.md](../NATURAL_TEXTURES.md): `cedar_log`, `cedar_log_top`, `stripped_cedar_log`, `stripped_cedar_log_top`, `cedar_planks`, `cedar_leaves` and `cedar_sapling`. The cedar was appended last to `WOOD` and `LEAVES`, so every other texture keeps its seed; no existing texture changed. Nothing of Mojang's is read, traced or recoloured. The nine other trees need no textures.
- **The cedar's colours**, checked by script against rule 3 of NATURAL_TEXTURES.md: CIE76 ΔE on sRGB (D65) Lab, and HLS hue. Vanilla's woods are compared by their published average planks colours, not read from any Mojang file: oak #a2834f, spruce #735532, birch #c0af79, jungle #a07351, acacia #a85a32, dark oak #432b14, mangrove #763631, cherry #e3b3ad, pale oak #e4dbd9, bamboo #c2ad51, crimson #653046, warped #2b6963.
  - **The painting:** western red cedar, second set row 0, moved from `OWNER_BANK` into `WOOD` (as `"2:0"`) and `BARK`. Wood #84654d (hue 26.2, lightness 0.410, saturation 0.263); bark #483229 #513a2f #563d31 #65493a #6b4f40.
  - **It sat on two woods:** ΔE 8.2 from our cypress (12.5° of hue apart) and 9.6 from vanilla spruce (6.1° apart), with vanilla jungle at 11.1 (0.4° apart).
  - **The rule used:** at least ΔE 10 from all 12 vanilla and 13 Jugcraft woods, and at least 8° of hue from any vanilla wood within ΔE 13. Only hue and lightness move, by the smallest change (ΔE from the painting) on a grid of ±40° of hue in 0.25° steps by ±0.12 of lightness in 0.001 steps. ΔE 10 is about where two flat colours read apart at a glance.
  - **The change:** lightness 0.410 to 0.355 and hue 26.2 to 23.0, saturation kept (0.260 after rounding): **#725543**, ΔE 7.4 from the painting. It stays a muted red-brown, a little darker, as weathered cedar is.
  - **After:** vanilla spruce ΔE 10.0 (9.3° apart), our cypress 10.6 (9.3°), our jacaranda 13.4 (30.5°), vanilla jungle 17.7, dark oak 19.2, mangrove 19.7.
  - **Rejected:** a yellower shift (about #876f4f) sat on spruce's hue (2° apart) and lost the red; a redder one became a rose-brown near the jacaranda and mangrove; keeping clear of the planned yew as well forced an olive.
  - **Within vanilla's range:** lightness 0.355 (vanilla's planks 0.17-0.87), saturation 0.26 (vanilla's up to 0.54; oak 0.34, spruce 0.39). Its ramp has the same six tones as every wood: #433126 #533d2f #644938 #715544 #7f5f4c #8c6a54.
  - **Bark:** the painted ramp, darkened with the wood: each tone's lightness times 0.866 (0.355 / 0.410), hue and saturation kept, giving **#3e2b24 #463229 #4a352a #573f32 #5d4437** (mean ΔE 4.6 a tone from the painting). Drawn `stringy` (seven narrow furrows and a few flecks) where the bank had suggested furrowed.
    - **Why:** with the wood darkened alone, the painted bark sat ΔE 9.7 from the wood (the bark ramp's mean against the wood's colour), the least of the 14 woods (jacaranda 11.0, dead 12.4, cypress 13.7, aspen 14.4; the others 20 or more), and the cedar log looked almost the same as its stripped log. The painting has 17.0; the darkened pair has 14.2. On the drawn textures, the cedar log's average colour is now ΔE 14.3 from its stripped log's (9.9 with the painted bark; cypress 19.0, jacaranda 12.7, aspen 4.1).
    - **Contrast:** its darkest-to-lightest lightness ratio stays 0.66 (darkest 0.19), the softest bark (the others 0.33-0.48, the aspen 0.17), so it is no more contrasting than vanilla's.
    - **Other barks:** mean ΔE a tone from fir 9.3, mahogany 9.9, cypress 12.0, willow 12.5, chestnut and larch 13.4, the rest 15 or more (as painted: fir 8.8, cypress and mahogany 9.4). The fir's and mahogany's are drawn plated; the cypress's is stringy but much redder and lighter.
    - **Sapling:** the cedar sapling's stem takes the bark's fourth tone, so it is redrawn too (#573f32, was #65493a).
  - **Leaves:** `cedar_leaves` #27301a #343f22 #43522c #536539 #657a46 #7d9657: hue 84, saturation 0.27-0.30, the darkest 0.31 of the lightest's lightness (rule 2: about 0.3). Drawn with the `needles` kind (short slanting strokes), as the cypress's are. Mean ΔE per tone from the nearest leaf looks: larch green 13.7, mahogany 14.0, redwood 14.3, cypress 14.8, fir 16.1, chestnut 18.7, willow 21.5 (the Wetland's other tree); every other look is further. The cypress's leaves are blue-green at hue 140 and the fir's at 147.
  - **Map colours:** bark `TERRACOTTA_BROWN` (ΔE 4.2 from the bark's middle tone; 5.4 from the painted one), wood `COLOR_BROWN` (6.1 from the planks).
  - **For later batches:** the yew planned for batch 3 (painting row 7, #654135) was ΔE 15.5 from the painting and is 8.8 from the cedar's wood, so batch 3 must check the yew against the cedar. The hawthorn green planned for batch 5 is 10.7 from the cedar's leaves; they share no biome.
- The owner's other banked rows stay in `OWNER_BANK` ([wood-repaint.md](wood-repaint.md#the-owners-second-set)).
- **Generated by:** `python3 tools/generate_textures.py` and `python3 tools/generate_material_data.py`: 100 new and 66 changed files under `src/main/resources`, none edited by hand.

## Verification
- **Run locally on 5 October 2026, on the branch's worktree** (the build of this change):
  - `python3 tools/generate_textures.py`: exit 0. Seven new textures, all `cedar*`; no existing PNG changed.
  - `python3 tools/generate_material_data.py`: exit 0. 100 new and 66 changed generated files, the same set as the build spec's prototype, byte for byte.
  - `python3 tools/check_mod_data.py`: exit 0, `PASS: 1448 material IDs, data files and recipe audit. No Minecraft build or game test performed.` (1437 before this change; the 11 new IDs are the cedar's.)
  - `python3 scripts/check_repository.py`: exit 0, `PASS: repository structure (bootstrap) and local documentation links. No gameplay/build tests performed.`
  - Negative runs of the new checks, with the data changed in memory only:
    - `WOOD_SWITCHES` unchanged: 0 errors; larch without `biomes`: 3 errors (larch, tamarack, fallen larch); chestnut without `biomes`: 1 error; the cedar's list as a string: 1 error; the cedar with an unknown switch: 2 errors; fir without `biomes`: 7 errors (all six fir shapes and the fallen fir).
    - `larch_planks` without its `"or"`: `larch_planks: gated by [['alpine_spawn']] but its result belongs to ['alpine_spawn', 'biomes'] (any of them)`; `chestnut_fence` without its `"or"`: the same kind of error; `cedar_planks` with an extra `"or"`: flagged.
  - A parse-only `javac` pass over the two changed main Java files (`FeatureEnabledCondition`, `JugcraftAgriculture`): exit 0. Syntax only: nothing was compiled and no symbol was resolved.
  - Each changed biome's shares, rebuilt by script from the chances in `tools/biomes.py`, are within 0.01 of the targets in "World and event applicability".
- **Run again on 6 October 2026, after these docs were written:** `python3 tools/check_mod_data.py` printed the same PASS line (1448 IDs), and `python3 scripts/check_repository.py` its PASS line.
- **Run again on 6 October 2026, after the review fixes** (the cedar's bark, the bent snag's crown, two more checks, the client test's rows and walls, the fallen larch test's box):
  - `python3 tools/generate_textures.py`: exit 0. Only `cedar_log`, `cedar_log_top` and `cedar_sapling` changed.
  - `python3 tools/generate_material_data.py`: exit 0. Only `worldgen/feature/dead_snag_bent.json` changed (`min_height_for_leaves` 8).
  - `python3 tools/check_mod_data.py`: exit 0, the same PASS line (1448 IDs). `python3 scripts/check_repository.py`: exit 0, its PASS line.
  - Negative runs of the two new checks, in a scratch copy of the worktree (each a failure, exit 1, with the error shown):
    - `sawing/larch_logs.json` without its `"or"`: `recipe/sawing/larch_logs.json: gated by [['alpine_spawn'], ['machines']], not by the machines switch and the larch wood's switches ['alpine_spawn', 'biomes'] (any of them)`;
    - `sawing/cedar_logs.json` with only its `machines` condition, `sawing/chestnut_logs.json` with an extra `seasons` condition, and `sawing/maple_logs.json` without its `machines` condition: the same kind of error;
    - the chestnut's list cut to `["biomes"]` and the data regenerated: `chestnut is placed by ['agriculture'], which the chestnut wood's switches ['biomes'] lack (agriculture.WOOD_SWITCHES, TREES.md rule 4)`. Before this fix, that edit passed every check;
    - `wildPatch("chestnut_tree", ...)` moved out of `registerWorldgen`: `chestnut is placed by ['(patch_chestnut_tree, outside registerWorldgen)'] ...`; `registerWorldgen` without its agriculture gate: `JugcraftAgriculture.registerWorldgen() not found, or not gated by the agriculture switch first`.
  - A parse-only `javac` pass over the two changed test files (`WoodClientGameTests`, `BiomeGameTests`): exit 0, with a deliberate syntax error caught as a control. Syntax only: nothing was compiled and no symbol was resolved.
- **Game tests** (results under CI results below):
  - `BiomeGameTests.batchOneShapesGrow`: each shape, placed through its `_checked` feature on dirt from four seeds in autumn, reaches the height its trunk gives (exact for the straight trunks, loose for the bending and fancy ones, to be narrowed from CI's logged sizes); the leafy shapes have at least 4 leaves, all in autumn colours where seasonal and none where evergreen; the snags have no leaves; four mossy maples lay moss carpet and vines.
  - `batchOneShapesNeedTheirSoil`: on stone no shape places a log; the snags grow on coarse dirt and not on calcite or tuff.
  - `fallenLarchesLie`: a larch stump and at least three lying larch logs.
  - `cedarSaplingsGrow`: a cedar sapling grows 7-10 cedar logs and at least 10 cedar leaves.
  - `cedarWoodWorksLikeWood`: an axe strips a cedar log along its axis; cedar logs burn, cedar planks are planks, the sapling is a sapling and the leaves are evergreen leaves.
  - `woodRecipesFollowAnyOfTheirSwitches`: the `"or"` loads a recipe when any switch is on and not when none is; the larch's, chestnut's and cedar's recipes load.
  - `batchOneBiomesPlaceTheirTrees`: each of the 29 biomes places its own `jugcraft:trees_<biome>` in the vegetation step and no other (the Cinder Barrens' is new), and is placed in its dimension: Jugcraft's regions, or the End for the Rotted Expanse.
  - `WoodClientGameTests`:
    - grows the cedar with the other trees, and puts its wood on the sample wall beside a wall of vanilla spruce, the wood its colour was moved off;
    - grows the batch-1 shapes in a row of their own between vanilla's spruce and oak, each beside its parent: the cedar beside the vanilla spruce it replaces, and the willow bush, young aspen and mossy maple beside a willow, an aspen and a big maple;
    - grows the tamarack, mossy maple, young aspen and willow bush again in autumn beside the larch, maple, aspen and willow;
    - logs each tree's logs and leaves.
  - `BiomeClientGameTests` photographs the Muskeg too.
- **CI results (PR #206):**
  - **4511d46c:** the game tests failed to compile. 26.3's `BlockTags` has no `LOGS_THAT_BURN` constant, so `cedarWoodWorksLikeWood` now looks up `#minecraft:logs_that_burn` by name (8b60c43f).
  - **8b60c43f:** the client shards, `client` and `repository` passed. `mod` failed one server test, `DiagonalConnectionsGameTests.everyFenceBarsBlockAndWallHasDiagonals`. It expected exactly 86 diagonally joining blocks, and the cedar's fence makes 87. The count was updated (47c96576).
  - **47c96576** (run 37491799326): `mod` (build and every server game test, the six above among them), `client (shard 0, 1 and 2 of 3)`, `client` and `repository` all passed.
  - **WoodClientGameTests logged:**
    - cedar 9-10 logs and 30-58 leaves;
    - stunted fir 5 and 34; bog fir 7 and 26; subalpine fir 10 and 79; fir bush 1 and 29;
    - tamarack 8 and 30;
    - dead snag and bent snag 6 logs each, no leaves;
    - willow bush 1 and 30; young aspen 6 and 69;
    - mossy maple 16 and 267, against the big maple's 14 and 211.
  - **Screenshots:** they show every shape beside its parent and the cedar's wood on the sample wall. The Muskeg biome shot framed a close-up of a snow block rather than the biome, which is a framing limit of `BiomeClientGameTests` and not of the trees.
- **Not run:** play, a dedicated server, two clients. The owner has seen the CI screenshots in chat; the cedar's colours await their word.

## World and event applicability
Shares are of each biome's tree tries, turned into in-order chances (TREES.md, "How to read this"). (v) is vanilla's wood; the mod's oak bush, vine-hung oak and azalea tree are vanilla's wood too. Snag shares are three straight to one bent. Where the roster's final shares include a tree from a later batch, its share stays with the tree it will replace until its batch.

| Biome | Before | Batch 1 | Held for a later batch |
|---|---|---|---|
| Coniferous Forest | fir 68, tall fir 25, spruce (v) 6, fallen fir 1 | fir 57, tall fir 20, larch 10, stunted fir 8, dead snag 3, fallen fir 2 | |
| Snowy Coniferous Forest | fir 78, tall fir 20, fallen fir 2 | fir 48, tall fir 20, subalpine fir 20, stunted fir 10, fallen fir 2 | |
| Maple Woods | maple 76, big maple 12, spruce (v) 11, fallen maple 1 | maple 75, big maple 12, fir 12, fallen maple 1 | |
| Seasonal Forest | oak (v) 49, maple 35, aspen 13, big maple 3, fallen maple 1 | oak (v) 44, maple 35, aspen 13, fir 4, big maple 3, fallen maple 1 | the persimmon's 10, in the oak (batch 5) |
| Aspen Glade | aspen 89, maple 10, fallen aspen 1 | aspen 69, young aspen 15, maple 10, subalpine fir 5, fallen aspen 1 | |
| Dead Forest | dead tree 73, spruce (v) 15, oak (v) 8, fallen dead 4 | dead tree 40, dead snag 35, spruce (v) 10, oak (v) 10, fallen dead 5 | the juniper's 10, as the oak (batch 6) |
| Tundra | maple bush 100 | maple bush 50, willow bush 30, fir bush 20 | the juniper bush's 10, in the maple bush (batch 6) |
| Snowy Forest | oak (v) 56, fir 30, maple 14 | oak (v) 36, fir 30, aspen 20, maple 14 | the rowan's 36, as the oak (batch 5) |
| Muskeg | dead tree 71, fir 25, fallen dead 4 | tamarack 35, bog fir 30, dead snag 20, dead tree 10, fallen dead 3, fallen larch 2 | |
| Bog | maple bush 90, dead tree 10 | maple bush 45, tamarack 30, bog fir 15, dead tree 10 | |
| Dead Swamp | dead tree 100 | dead snag 50, dead tree 45, fallen dead 5 | |
| Lush Swamp | vine-hung oak 70, willow 30 | vine-hung oak 60, willow 30, maple 10 | the bald cypress's 25, in the vine-hung oak (batch 4) |
| Swamp Woods | willow 70, vine-hung oak 30 | willow 70, vine-hung oak 20, azalea (v) 10 | the bald cypress's 25, in the willow (batch 4) |
| Floodplain | oak bush 80, oak (v) 20 | willow bush 70, oak (v) 30 | the cottonwood's 30, as the oak (batch 2) |
| Ghost Forest | dead tree 85, dark oak (v) 15 | dead snag 55, dead tree 30, cedar 15 | |
| Lush River | oak bush 100 | willow bush 70, willow 20, oak bush 10 | the cottonwood's 10, as the oak bush (batch 2) |
| Fen | fir 70, dark oak (v) 30 | bog fir 50, tamarack 35, willow bush 15 | |
| Lake District | oak (v) 54, spruce (v) 40, fancy oak (v) 6 | oak (v) 54, fir 30, larch 10, fancy oak (v) 6 | the rowan's 10, in the oak (batch 5) |
| Wetland | spruce (v) 60, willow 40 | cedar 50, willow 40, spruce (v) 10 | the elder's 10, as the spruce (batch 5) |
| Wasteland | dead tree 100, one try in ten chunks | dead tree 60, dead snag 40, one try a chunk | the tamarisk's 15, in the dead tree (batch 8) |
| Burnt Forest | dead tree 90, oak bush 10 | dead snag 45, dead tree 40, oak bush 10, fallen dead 5 | |
| Dense Forest | fancy oak (v) 57, oak (v) 30, dark oak (v) 7, oak bush 6 | fancy oak (v) 54, oak (v) 30, dark oak (v) 7, oak bush 7, great oak 2 | the walnut's 10, in the fancy oak (batch 13); the holly's 5, in the oak bush (batch 6) |
| Redwood Forest | redwood 57, giant redwood 35, spruce (v) 6, fallen redwood 1 | redwood 52, giant redwood 35, fir 6, mossy maple 5, fallen redwood 2 | |
| Temperate Rainforest | fir 40, redwood 30, tall fir 14, vine-hung oak 11, willow 4 | fir 41, redwood 30, tall fir 14, mossy maple 11, willow 4 | the tree fern's 3, in the fir (batch 12) |
| Shield | fir 50, pine (v) 30, spruce (v) 14, oak (v) 6 | fir 45, pine (v) 25, spruce (v) 14, aspen 8, larch 8 | the windswept pine's 25, as the pine (batch 2) |
| Cinder Barrens | none | dead snag 100, one try a chunk (two in one chunk in ten) | |
| Hallowed Bog | willow 70, vine-hung oak 30 | willow 70, young aspen 30 | |
| Snowpetal Grove | cherry (v) 75, birch (v) 25 | cherry (v) 75, aspen 25 | the snowpetal cherry's 75, as the cherry (batch 4) |
| Rotted Expanse | dead tree 100 | dead tree 60, dead snag 30, fallen dead 10 | |

- **Biome fit.** The Muskeg and Fen get the tamaracks, thin bog firs and snags of real muskegs and fens. Dead stands read as killed forests. The Temperate Rainforest finally has moss on its trees, and the Wetland and Ghost Forest a living green conifer of their own.
- **Biome records updated** to these trees: [seasonal-forests.md](seasonal-forests.md), [wetlands.md](wetlands.md), [warm-and-dry.md](warm-and-dry.md), [big-trees-and-rainforests.md](big-trees-and-rainforests.md), [mountains-coasts-and-volcanoes.md](mountains-coasts-and-volcanoes.md), [wonders-and-caves.md](wonders-and-caves.md) and [end-biomes.md](end-biomes.md). Their Results sections are history and are left as they were.
- **Special placements.**
  - **Cinder Barrens:** it had no trees. Its floor is tuff with patches of magma, gravel and coarse dirt; a snag stands only where an oak sapling could, so only on the coarse dirt. The roster's "about one tree in eight chunks" is its estimate, not measured. Its badlands base has no tree feature, so `jugcraft:trees_cinder_barrens` joins its vegetation step, after vanilla's firefly bushes and before its dead bushes; it is the biome's own, so no feature order changes elsewhere.
  - **Wasteland:** its floor is calcite with coarse-dirt patches; its dead trees and snags stand only on the coarse dirt, for the same reason.
  - **Rotted Expanse** (the End): its ground is end stone with coarse dirt and soul soil; snags and fallen logs stand only on the coarse dirt, as its dead trees do.
  - **Hallowed Bog:** young aspens stand in its water up to 2 deep, the biome's existing water depth for trees; in 2-deep water a young aspen shows about 3-5 of its 5-7 blocks.
  - Water depth is a biome's, not a tree's: the Dead Swamp's snags and fallen logs, the Swamp Woods' azaleas, the Lush Swamp's maples and the Floodplain's and Wetland's new trees may also stand in shallow water, as their other trees already do. Not checked in game.
- **Seasons.** The tamarack, willow bush, young aspen and mossy maple, like every seasonal tree, follow the server's season day in every biome; `#jugcraft:has_seasons` tints only grass and vanilla foliage. So the aspens newly in the Snowpetal Grove and Snowy Forest go gold in autumn and bare in winter beside the cherries and firs. With `seasons.mode=off` they stay green. The cedar and the firs stay green all year.
- **The Gloomweald** waits for batch 3, which gives it its own canopy, the yew and its snags.
- Not applicable: caves, dungeons, pets, bosses, loot and events. No seasonal content gates anything here.

## Rollout and open questions
- **The cedar's colours are approved.** On 6 October 2026 the owner saw them beside their painting and beside vanilla spruce, in the CI screenshots, and said: "Cedar looks sooo good!!". Each is one line in `tools/wood_style.py`:
  - the darkened wood `#725543` against the painting's `#84654d`;
  - the bark darkened with it (`#3e2b24`-`#5d4437` against the painted `#483229`-`#6b4f40`);
  - the sage leaves.
- **Shapes to check in the screenshots:**
  - the tamarack's tuft (height 3);
  - the young aspen's crown, whose bend logs may show at its top (blob offset 1 would cover them);
  - the willow bush's bare second log;
  - the mossy maple's moss coverage;
  - the bending and fancy heights, to narrow `batchOneShapesGrow`'s bounds.
- **Vanilla placer behaviour** was read from memory of 1.21.x code, with no 26.3 sources at hand: the pine crown's width, the bending trunk's top, the fancy limbs and the fallen log's length. The tests use loose bounds where unsure.
- **CI time:** eight more wood shots (17, from 9), a wider field and one more biome shot in client shard 2, which has run 18-24 minutes against a 30-minute limit. Watch the first run.
- **Chosen where the roster was silent:** the Muskeg's fallen larch 2 (out of its fallen 5); the Ghost Forest's and Muskeg's defaults; which tree holds a later batch's share in the Swamp Woods, Lush Swamp, Tundra, Lake District and Temperate Rainforest; the Floodplain's oak 30, the Lush River's oak bush 10 and the Wetland's spruce 10.
- **The tree farm** recipes stay gated by `machines` alone for every tree, as before this change.
- **The `"or"` field** is Jugcraft's own, not Fabric's `fabric:or`, whose 26.3 form could not be checked here. If maintainers prefer Fabric's, the generator, the check and `FeatureEnabledCondition` change back.
- **The yew** (batch 3) must be checked against the cedar's wood (ΔE 8.8).
- **Reversible:** the shapes and lists are data; reverting restores the old trees in new chunks. The cedar's IDs must stay once released; reverting after a release would need a migration for placed cedar blocks.

---

Built and documented on 5 and 6 October 2026 with Claude Opus 5.5, from the build spec checked against the roster. Colour distances and shares were computed by script; nothing has been played.
