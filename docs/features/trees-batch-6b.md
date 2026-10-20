# Tree generation batch 6b: holly

Status: implemented, owner-approved and ready for review in [PR #404](https://github.com/jimbozoomer-byte/jugcraft/pull/404), 16 October 2026. Public branch: `codex/tree-generation-batch-6b` in `speedygroyper/jugcraft`. Actual tool/model: OpenAI Codex (GPT-6). Based on the approved batch 6 branch (PR #399), already reconciled with main after #398. Batch 7 is excluded.

## Scope

Add a renewable holly tree, its nine-block ivory wood set, glossy evergreen leaves, sapling, decorative holly sprigs and Holly Wreath. Use the existing tree, seasonal foliage, wood, recipe, tree-farm and wall-wreath systems. No new dependency or progression gate.

Woodland receives 4% holly from its ordinary oak default, preserving hawthorn, elder, fallen oaks and the remaining 3.5% oak bushes. Dense Forest receives 5% holly from its roughly 7% oak-bush share, preserving the other trees. Counts remain 9–10 and 14–16 attempts respectively (the upper count in 10% of chunks). Soil and space checks can reject attempts.

The tree uses a straight trunk of four plus zero to two logs and native fancy foliage (radius 2, offset 0, height 4). This produces the intended broadleaf oval; the originally suggested height-four blob would spread too far sideways. Across twelve seeds in each of summer, winter and seasons-off, the measured trees were 5–7 blocks high and 5 wide, with 4–6 logs and 69 leaves. All leaves connected by faces to the trunk, and three distinct height/shape hashes occurred. These are sampled bounds, not worldwide population statistics.

The wood uses the approved grey-green ivory palette `#b6baa1`, smooth grey-green bark, deep glossy green foliage and small scarlet berry clusters. Follow the existing deterministic 16×16 art tools and runtime native wood references where appropriate; preserve previous species and the owner library. The owner-library catalog contains no holly-specific assets.

## Sprigs and seasons

The leaves stay green all year. They reuse the shared seasonal state and [100, 300, 335] schedule for appearance: autumn/winter foliage has scarlet berries, while one in three green-state model choices carries berries, including seasons-off. These berries are decorative, not a new food item.

Right-click supported natural leaves with shears to clip one sprig, damaging the shears once and visibly removing berries from that block. Clipped leaves have a one-in-sixteen regrowth chance on each random tick in every calendar mode; repeated immediate use and player-placed leaves yield nothing. Ordinary shearing/Silk Touch still obtains the leaf block without bonus sprigs. Natural decay and renewable sapling drops remain intact.

Craft one Holly Wreath from eight sprigs around a central string. Reuse the Autumn Wreath's wall/door placement, facing and support-loss behavior, with a dark green ring and scarlet clusters. It is permanent decoration, available year-round; mums do not recolor it. Both wreaths stay on the same physical face of the door as it opens or closes, including either hinge and redstone-operated doors. Crouch and right-click either broad face to place one on an open or closed door. Clicking the wreath operates its wooden door; punching removes the wreath. An obstruction drops the wreath once without replacing another block, and the Autumn Wreath keeps its flower colour.

The inspection's flat foliage panels are player-placed decorative leaves and cannot yield sprigs. Use shears on an actual holly tree (including one grown from a sapling) or the marked natural demonstration leaf. An empty hand cannot clip sprigs. The panels are now labelled "Display only".

## Verification and delivery

Passed locally: the full build and all 1,764 required server game tests; all 28 tree Python regression tests; four focused server game tests; repository checks; 826 generated tree/biome/loot/model/recipe-view JSON comparisons; and eleven deterministic texture comparisons. All previously committed texture files are byte-identical. The server tests exercise 36 seeded trees, calendar initialization, leaf connectivity, sapling growth and obstruction protection, all calendar modes of clipping/regrowth, duplicate prevention, placed-leaf restrictions, decay, leaf loot, stripping, crafting/sawmilling/tree farming, wreath crafting, all four wall facings, floor rejection, door placement and exactly one dropped wreath per removed support. The full data/recipe/art audit passed after teaching its plain-item parser to recognize a decorative ingredient registered directly in the building tab: 2,806 material IDs, 8,409 block/item models, 3,707 quad parts and 171 combined models.

The rendered `HollyClientGameTests` passed, saving and reopening all three gallery trees, seasonal/clipped foliage states, and wreaths. Ten screenshots were inspected, including cutout and solid leaves. Two untouched natural samples contained holly: Woodland at -1664, 93, 1728 had 32 holly logs and 523 leaves; Dense Forest at 832, 67, 1744 had 74 logs and 922 leaves. These local samples establish presence, not a measured global frequency.

The `Holly Inspection` world uses seed `jugcraft`. Its authored garden is at 1000, 180, -988; `TOUR.txt` gives the natural stops. Inventory supplies include shears, sprigs, wreaths and saplings. Random ticks are frozen for inspection; restore `minecraft:random_tick_speed` to 3 to observe natural regrowth. A narrowly cleaned copy preserves all 90 save files and every gameplay value, removing only unused development metadata from `level.dat`, `level.dat_old`, and `data/fabric/registry_entries.dat`. The scan covered 6,210 saved region records and found no actual test content. The ordinary Minecraft client opened this cleaned save and joined at 1000.5, 180, -987.5. External launcher import, two-client multiplayer and server deployment are not claimed.

Initial delivery files are `Jugcraft-Tree-Batch-6b.mrpack`, `Tree-Batch-6b-World.zip` and the matching `jugcraft-tree-batch-6b.jar`, with a readable tour, screenshots and SHA-256 checksums. Archive integrity, every saved-world byte, the exact JAR and all 18 pinned dependency entries were verified. Initial gameplay/art source is `00211cb936f9dfe2291cb5ed9e51cedcbc0d046c`; the following commit records the completed full audit only. GitHub's eight client shards and combined client check also passed for that gameplay commit.

### Door attachment and clipping clarification, 16 October

The owner reported trying to harvest the decorative panels and a wreath left floating when its door opened. Sprig acquisition rules are unchanged; the gallery labels and instructions explain the distinction. The first correction (`e4dca698`) followed the panel's outside block edge, but the owner's follow-up showed that this changed the physical face when opening. Its five focused server checks and rendered client check passed because they checked contact with the panel, not which face was retained.

The corrected door attachment stores the door half and physical front/back face in a native `BlockAttachedEntity` (`jugcraft:door_wreath`), retaining the normal block model, textures and Autumn Wreath flower state. This lets the ring occupy the inner face inside the door's block cell, with its real picking box, rather than forcing it into an adjacent block. Wall wreaths retain their block representation. The attachment is saved and synchronized normally and has a bounded local support/obstruction check. Existing door-mounted wreath blocks convert on their scheduled placement tick or when their door is operated; they do not replace another block. A wreath that had already flipped in an older build uses its current visible face as the migration starting point. Remove/re-hang it to choose the other face.

Door decorations add a saved entity registration; retain this version or a later compatible version when reopening saves containing them. The gallery includes a left-hinged door and a right-hinged door with Holly on the front and Autumn on the back, making both faces easy to check. The right-hinged sample is saved open.

Focused server validation: all five wreath tests passed (`jugcraft-test:*wreath*`). They cover 128 placements across both wreath types, four door orientations, both hinges and halves, both physical faces, and initially open/closed doors. Checks include manual oak and powered iron opening/closing, serialized face/appearance/anchor round-trips, duplicate-placement refusal, obstruction/support-loss drops, mum recolouring, survival/creative removal, wall behavior, and migration of an older block attachment. The full suite and full data/art audit were not rerun for this correction, as requested.

The single `HollyClientGameTests` class passed with the pinned Sodium/Iris setup and with optional integrations absent. Actual mouse/key input placed a wreath on an open door's inner face, picked it, closed the door through it, and punched it off. Both configurations saved and reopened the gallery, loading the door display before asserting all three saved wreaths and their faces/colours. Open/closed and front/back screenshots were inspected. The refreshed save opens beside the wreaths at 1050.5, 180, -865.5; earlier inspection saves are preserved. The ordinary Minecraft client joined this cleaned save at that position. Its delivery copy preserves all 90 save files; a scan of 6,254 region records found no test content, and only the same three unused development-metadata files were changed.

### Generated language ordering correction, 16 October

The owner approved the corrected wreaths and requested review readiness, then reported the failing PR check. GitHub run `38966841324`, job `117195303739`, failed its generated-resource comparison because `entity.jugcraft.door_wreath` had been inserted after `chair_seat` instead of its alphabetical position after `dirge`. All eight client shards, their combined check, the optional-integrations-absent build/server tests and repository check had passed on `2dc2353fe`.

Rewrote the language JSON using the generator's existing sorted-key writer. Verified every translation key/value is unchanged and the complete file matches that ordering and serialization. This changes only generated JSON ordering; the previously approved tree/wreath behavior and inspection world remain the same. Local gameplay suites were not rerun for this data-order-only correction. Actual tool/model: OpenAI Codex (GPT-6).

### In-game screenshots

![Small holly tree](../images/trees-batch-6b/tree.png)
![Holly and birch wood comparison](../images/trees-batch-6b/wood.png)
![Seasonal and clipped foliage](../images/trees-batch-6b/foliage.png)
![Wall and door Holly Wreaths](../images/trees-batch-6b/wreaths.png)
![Holly Wreath retains the front of the opened door](../images/trees-batch-6b/wreath-open-door.png)
![Holly on the front of the right-hinged door](../images/trees-batch-6b/wreath-right-front.png)
![Autumn on the back of the same right-hinged door](../images/trees-batch-6b/wreath-right-back.png)
![Evergreen winter berries](../images/trees-batch-6b/winter.png)
![Natural Woodland sample](../images/trees-batch-6b/woodland.png)
![Natural Dense Forest sample](../images/trees-batch-6b/dense-forest.png)

Open PR titles and public branches in upstream and all four listed forks were checked before starting. No separate holly implementation was identified; the upstream tree-roster branch is the existing plan. This is not approval to merge or deploy a server world.

## Compatibility and asset provenance

New chunks gain the trees; existing terrain is not rewritten. Disabling biome generation preserves registered blocks, items and saved decorations. Sprigs and wreaths are optional early-game decoration, with no food, event-only unlock or core progression dependency. No new framework, seasonal state or external dependency is introduced.

`tools/holly_textures.py` extends the existing original pixel-art generators. Wood colour follows the approved nominal recolour of owner painting 10; bark uses the agreed smooth grey-green palette. `tools/wood_style.py` appends the new wood without shifting prior seeds. Bark and planks use the generated holly images; cut and stripped faces reference the player's native pale-oak textures with a fixed ivory tint through the existing wood renderer. No Mojang image is redistributed. The wreath reuses the Autumn Wreath's ring geometry and attachment class while supplying its own foliage and raised berry clusters; the existing wreath retains its original recipes and mum recolouring.
