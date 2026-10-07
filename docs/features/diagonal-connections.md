# Diagonal connections

Status: fences, panes and bars merged in #138; walls merged in #142; tall sides on diagonal walls in review. Green in CI with screenshots; **not yet played** by a person. Results are under Verification.
Proposal issue: none. The owner asked for it on 3 October 2026 (see "Owner request" below).
Owner: @jimbozoomer-byte
Target milestone and tier: building, from the first day (no tier).
Primary specialty and supported player role: builders and decorators of every branch.

## Owner request
- "begin making the framework for fences and similar objects to connect diagonally similarly to how this mod does it but I want it to work with any blocks that connect like iron bars, graveyard fence etc etc", with a link to the Diagonal Fences mod and its screenshot of fences running at 45 degrees.
- Then, once the framework was merged: "merge it, then start on walls".
- Then, sharing the Diagonal Fences jar: "make it more optimized copy how they do it in this mod". Its design was studied and its state-saving approach for walls, its pathfinding fix and its break-particle fix are followed here in Jugcraft's own code. The jar is MPL-2.0 with all-rights-reserved assets, so no code, model or texture was copied into this MIT project.
- Nothing is copied from that mod. The rule, the code, the models and the tests here are Jugcraft's own; the mod gave the idea and, for walls, the approach.

## Player experience
- **Fences, glass panes, bars and walls join diagonally.** Place two of a kind a diagonal step apart and a rail (or a pane, bars or a wall) runs between them at 45 degrees, so fences and walls can follow a slope, turn a corner at an angle or draw a diamond.
- **Which blocks:**
  - all of vanilla's fences (13 wooden and the nether brick fence);
  - glass and the 16 stained glass panes;
  - iron bars and the 8 copper bars, waxed or not;
  - all 32 of vanilla's walls (cobblestone, the stone, brick, deepslate, blackstone, tuff, cinnabar and sulfur families, and the rest);
  - all 15 of Jugcraft's fences: the 14 wood-set fences (the cedar's since the tree roster's batch 1, [trees-batch-1.md](trees-batch-1.md)) and the wrought-iron cemetery fence (the graveyard fence).
  - Jugcraft's own walls: the bastion concrete wall (batch 55, [fortifications](fortifications.md)). It gets a diagonal wall exactly as vanilla's walls do (`JUGCRAFT_WALLS` in `tools/diagonal_connections.py`).
- **The rule, the same from either end.** Two blocks a diagonal step apart join when they would join side by side, and neither joins straight into the two blocks between them:
  - a wooden fence joins any wooden fence; the nether brick fence and the cemetery fence join each other but not wooden fences, as they do straight; panes and bars all join each other; any wall joins any wall;
  - fences, panes and bars, and walls, never join each other's kind diagonally (a wall joins bars straight, but not on the diagonal);
  - a straight corner comes first. Fences that already meet round a corner, or a solid block in the corner, keep the diagonal from forming, so two diagonals never cross.
- **Walls: low arms, and posts as vanilla raises them.**
  - A wall that joins a wall diagonally becomes that wall's **diagonal wall** while it does, and goes back to the vanilla wall when its last diagonal goes. Players never see the difference: it is named, picked, mined and dropped as the vanilla wall, and walls, fence gates, bars and mobs treat it as a wall.
  - A wall's diagonal arm is a low wall side (14 pixels high), turned 45 degrees.
  - A wall with no straight sides and two opposite diagonals runs straight on, so it has no post, as a straight wall has none. Something above calls for a post as it does on a straight low wall: a torch or other post-raising block, a block over the middle, or a wall above with a post.
  - Any other wall with diagonals is an end, a corner or a junction, and keeps its post.
  - **Tall sides.** A diagonal wall's sides and arms rise to the top of the block to meet a wall or block above, as a vanilla wall's sides do: when the face of the block above covers every joined side and arm, all of them are tall, and a tall straight run has no post, as a tall straight wall has none. One uncovered arm keeps them all low rather than raise an arm into the air, so a diagonal wall under the end of a shorter wall above stays low, with a 2-pixel slot under the part that is covered. Walls without diagonals are vanilla's own and keep their per-side tall sides.
- **Live updates.** Placing, breaking or changing a block updates its diagonal partners at once. Fill in the corner and the diagonal goes; clear it and the diagonal comes back.
- **Shapes.** The diagonal arm is part of the block's outline and collision. A fence's or wall's diagonal blocks players and mobs as its straight sides do, and is as high (one and a half blocks).
- **Mobs.** Vanilla lets a narrow mob (a chicken, say) step diagonally between two fence posts. Across a diagonal join that gap is closed, so mobs no longer try that step and walk round instead of pushing against the arm.
- **Breaking.** A block with diagonals breaks into the particles of its shape without the arms, as a plain fence or wall does. The arms are runs of small boxes and the game spawns at least eight particles for each, so they would otherwise burst into several hundred.
- **Structures.** Rotating or mirroring a structure (the structure block, templates) turns the diagonals with it.
- **Switch:** `diagonal_connections.enabled=false` stops new diagonal joins; existing ones go when their blocks next update. Vanilla shapes and models stay as they were for every block without a diagonal.

## Connections
- Existing input producer: none needed; the blocks are vanilla's and Jugcraft's own fences, panes, bars and walls.
- Existing output consumer: building. The walled town's fences and walls keep their exact layout: its builder places blocks without shape updates.
- Technology connection: none.
- Magic connection: none.
- Reachable entry path (prove no circular unlock): every block that joins is craftable without any Jugcraft progression.
- Which connections are required vs optional; trade and solo routes: optional and solo.
- How this specialty stays useful without mastering every other branch: it is a building aid for everyone.
- For infrastructure/cosmetics, supported systems and reason resource links do not apply: it changes no recipe, resource or progression; it is a block shape and model behaviour.

## How it works (the framework)
- **Properties.** Every `FenceBlock` and `IronBarsBlock` (glass panes are bars too), including other mods', gets four more boolean properties: `north_east`, `south_east`, `south_west` and `north_west`, all false by default (`mixin/DiagonalStateMixin`, `mixin/DiagonalBlockMixin`).
- **Walls are swapped, not given properties** (the Diagonal Fences mod's approach). A wall has 324 states, ten times a fence's, so the properties would have added 155,520 states to vanilla's 32 walls. Instead vanilla's walls keep their own states, and `diagonal/DiagonalWalls` registers a `diagonal/DiagonalWallBlock` for each, `jugcraft:diagonal_<wall>`:
  - its sides are joined or not, not none, low or tall, and one more property, `tall`, raises them all with the arms, so a post, four sides, four diagonals, tall and waterlogged make 2,048 states;
  - `mixin/DiagonalWallMixin` hooks the vanilla wall's placement and shape updates: once vanilla has worked the wall out, `DiagonalWalls.settle` turns it into its diagonal wall if it joins a wall diagonally;
  - the diagonal wall hands each update to the vanilla wall it stands for, which works out its sides, tall sides and post, then turns back into the diagonal wall, or stays the vanilla wall if no diagonal is left;
  - it copies the vanilla wall's properties (`ofFullCopy`), loot table and name, returns the wall's item, and is in `#minecraft:walls`, which covers mining with a pickaxe, joins from walls, fence gates and bars, and mobs' pathfinding.
- **Who joins.** Only blocks in the block tag `#jugcraft:connects_diagonally` ever join. Every block in it has a diagonal arm model, so nothing joins without an arm to show. Today the tag has 88 blocks: vanilla's 72 (40 fences, panes and bars, 32 walls), Jugcraft's 15 fences (the cedar's since the tree roster's batch 1) and its bastion concrete wall (batch 55).
- **Adding a block.** A new Jugcraft fence, pane, bars block or wall joins diagonally once it is added to `tools/diagonal_connections.py`. The generator gives it an arm model and its four blockstate parts. The data checker fails if a Jugcraft blockstate shaped like a fence (a part for each straight side, none up or down) or like a wall (a low or tall part for each straight side) is missing from the tag. Another mod's block can join by adding itself to the tag and providing the models.
- **Updates.** `diagonal/DiagonalConnections` works out the diagonals wherever the block works out its straight connections: on placement and on each neighbour's shape update. Diagonal neighbours get no shape updates from the game, so a fence, bars block or wall also asks its four diagonal neighbours to look again whenever it is set. It does this through `updateIndirectNeighbourShapes`, the hook the game uses for redstone dust's diagonal neighbours. Blocks set with `UPDATE_KNOWN_SHAPE` (world generation, the town builder) are left as placed.
- **Wall posts.** A diagonal wall's post is worked out by vanilla's own rule (`WallBlock.shouldRaisePost`, reached through `mixin/WallBlockInvoker`). For a straight diagonal run, the rule is shown the wall as if it were a straight low wall running north to south. Every other wall with diagonals gets a post from the rule, as vanilla gives one to any wall that is not a straight run.
- **Shapes.** `mixin/DiagonalShapeMixin` adds each joined diagonal's arm to a fence's or bars block's outline and collision. It is a run of small overlapping boxes from the middle to the corner, as wide and high as the block's own straight arm: a fence's collision arm is 4 pixels wide and 24 high, its outline 2 wide. The same mixin turns the diagonals in `rotate` and `mirror`. A diagonal wall draws vanilla's wall shapes itself, with arms 6 wide, 24 high in collision and 14 in outline (16 when tall).
- **Shape memory.** Vanilla works out every fence's and bars block's shape for each of its states up front. `mixin/DiagonalBlockMixin` has it ignore the four diagonal properties, as it already ignores `waterlogged`, so it works out one shape for each set of straight sides instead of 16. The shapes with arms are kept in `DiagonalConnections.Arms`, by the shape without arms and the diagonals joined, and blocks whose shapes and arms are alike share them (every wooden fence, every pane). The diagonal walls share one table of 1,024 shapes, by post, sides, diagonals and height, for all 32.
- **Pathfinding and particles.** `mixin/DiagonalPathMixin` refuses a walking mob's diagonal step between two blocks joined to each other diagonally (`WalkNodeEvaluator.isDiagonalValid`). On the client, `mixin/client/DiagonalParticlesMixin` spawns a broken block's particles from its state without diagonals.
- **Models (`tools/diagonal_connections.py`).**
  - **Arm model:** each block's arm, `jugcraft:block/diagonal/<block>`, is a child of its own side model, so it keeps that block's textures, glass translucency and lighting. Its elements are turned 45 degrees about the post with Minecraft's `rescale`, which stretches them across the block to reach the corner; the arm is drawn narrower by the same factor, so it ends as wide as the straight arm.
  - **Hand-drawn arms:** fences (two rails at 6–9 and 12–15), the bamboo fence (from its own texture sheet's regions), panes (glass not stretched along the arm), bars (a plane of bars with edge strips) and walls (a low side, 6 wide and 14 high, its stone not stretched).
  - **Turned arms:** a side model with its own elements, as the cemetery fence has, is turned element by element. Rails reach the corner; pickets and finials keep their size and are spread along the arm.
  - **Blockstates:** each blockstate gets four more parts, the arm turned 0, 90, 180 and 270 degrees. Vanilla's 40 fence, pane and bars blockstates are rebuilt in `assets/minecraft/blockstates`: vanilla's own parts, naming vanilla's models by ID, plus the four diagonals; they were checked against vanilla 26.3's own, all identical before the diagonals were added. Vanilla's wall blockstates are not touched. Each diagonal wall has its own, `assets/jugcraft/blockstates/diagonal_<wall>.json`: the wall's post, its low side for each joined side and the four diagonals, all naming vanilla's models by ID.

## Balance and automation
None: no recipes, items or resources change.

## Multiplayer and persistence
- The server decides every join, as it decides straight ones. Clients get the new states with the usual block updates, and a client's placement prediction runs the same rule.
- Saved fences, panes and bars load with no diagonals, since the new properties default to false. They join when they next update, for example when a neighbour changes.
- Removing the mod drops the unknown properties, leaving vanilla's own blocks with their straight joins.
- Turning the switch off keeps the properties, so saved states still load.
- **Block states.** Vanilla 26.3 has 35,723 block states in all.
  - Fences, panes and bars: the properties multiply each one's states by 16 (any other mod's too), about 26,000 more for vanilla's 40 and Jugcraft's 14 (54 blocks × 32 straight and waterlogged states × 15 more each). A separate diagonal block for each, as the Diagonal Fences mod has, would take 512 states as well, so the properties stay.
  - Walls: the 32 diagonal walls add 65,536 states (32 × 2,048; half of them for tall sides). Giving vanilla's walls the properties instead added 155,520 (32 × 324 × 15), without tall diagonals. Measured numbers are under Verification.
- **Saves.** A diagonal wall is saved as `jugcraft:diagonal_<wall>`. Removing the mod loses those blocks, since the game no longer knows the ID; walls without diagonals are vanilla's and stay.
- Another mod that adds properties with the same names (`north_east` and the rest) to fences or bars would clash with this one. The Diagonal Fences mod itself is one such mod, so the two should not be installed together.

## Dependencies and assets
- No new dependencies.
- The arm models are written by `tools/diagonal_connections.py`. They name vanilla's side models and textures by ID and copy no Mojang file.
- Vanilla's blockstates for these 72 blocks are rebuilt from the same parts by the generator. A resource pack that replaces those blockstates wins over them and loses the diagonal arms; a pack that only retextures or reshapes the models keeps them.

## Verification
Checks:
- `python3 tools/check_mod_data.py` (`check_diagonal_connections`):
  - the tag matches the generator;
  - every tagged block has the four arm parts, turned correctly, and an arm model of turned elements with a parent;
  - vanilla's rebuilt blockstates keep vanilla's own parts;
  - vanilla's wall blockstates are not overridden, and each diagonal wall's blockstate has the wall's post and low sides and is in `#minecraft:walls`;
  - every Jugcraft fence-shaped or wall-shaped blockstate is tagged;
  - the Java names the four properties, the tag and the diagonal walls' IDs.
- Server game tests (`DiagonalConnectionsGameTests`):
  - fences placed by a player join diagonally, and their outline and collision reach that corner only;
  - a stone in the corner parts them and taking it away rejoins them;
  - a straight corner comes first;
  - breaking one lets the other go;
  - only kinds that join straight join diagonally (nine pairs, including the cemetery fence, the aspen fence, the bamboo fence, panes with copper bars, and bars with an oak fence);
  - rotation and mirroring, of a fence and of a diagonal wall;
  - walls placed by a player become diagonal walls and join, with posts at the ends; a third wall on along the diagonal drops the middle one's post; breaking the ends gives the post back and then turns the middle back into the vanilla wall;
  - a stone above the middle of a diagonal run makes it tall with no post; a wall with a post above raises its post and leaves it low; with nothing above it is low with no post; a stone in the corner keeps walls apart;
  - a diagonal run two high: the lower walls are tall, the lower middle has no post and its ends do, the upper walls are low, and the tall arm reaches the top of the block; taking the upper middle away lowers the lower walls;
  - a diagonal wall is joined by a vanilla wall beside it, is in `#minecraft:walls`, is named, picked and dropped as the vanilla wall, and turns back into it with its side tall under a stone;
  - alike blocks share their shapes with arms (two diagonal walls, two wooden fences);
  - every fence and bars block has the properties, starting false; no wall has them and each (vanilla's and, since batch 55, Jugcraft's) has a diagonal wall; exactly 88 blocks are tagged and 33 diagonal walls registered;
  - the game's block states and the heap after a full collection are logged.
- Client game test (`DiagonalClientGameTests`): photographs oak and bamboo fence zigzags, cemetery and aspen fence diamonds, slanted rows of panes, stained glass, and iron and copper bars, and walls (a cobblestone diamond, a stone brick run with a torch on its middle, an andesite zigzag and a mossy stone brick run two high), from above and from the side.
- Not covered by a test: the pathfinding change (mobs' diagonal steps) and the break particles.

### Results
**Walls (PR #142):**
- **Run [37144595581](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37144595581) on 6b033f47 (swapped diagonal walls): `mod` green; client shards 0 and 1 green, with the diagonal screenshots in shard 1.**
  - **Server:** all 582 required game tests passed. The logs showed:
    - walls placed by a player became `jugcraft:diagonal_cobblestone_wall[post, north_east]` and `[post, south_west]`; a third made the middle `[north_east, south_west]` with no post;
    - a vanilla cobblestone wall beside a diagonal wall joined it (`north=low`); with its diagonal gone under a stone, the diagonal wall was `minecraft:cobblestone_wall[south=tall, post]` again;
    - walls joined walls of any kind, and not bars, fences or panes; "86 blocks join diagonally, 32 diagonal walls; problems: []".
  - **Block states and heap** (heap after a full collection; it varies by about 20 MB from run to run):
    | Build | Block states | `BlockState` objects (shallow) | Heap |
    |---|---|---|---|
    | main, before walls (probe run 37142381337) | 90,121 | 9.4 MB | 835 MB |
    | walls with the properties on vanilla's walls (runs 37142353134, 37143200753) | 245,641 | 25.5 MB | 860–880 MB |
    | swapped diagonal walls (this run) | 122,889 | 12.8 MB | 837 MB |
  - **Client:** the walls joined as logged: the cobblestone diamond closed all round; in the stone brick run only the middle under the torch kept a post; the andesite zigzag's straight steps and diagonals joined; in the two-high mossy stone brick run the lower layer kept its posts under the upper walls. The three wall screenshots were looked at: the diamond, the run with its torch, the zigzag and the two-high run read as unbroken walls at 45 degrees, drawn with the walls' own stone and no missing models.
- **Failed, then fixed:** run 37142353134 on 8dfe69fc (properties on vanilla's walls) compiled, but eight diagonal tests failed: Mixin refused the shape mixin once it targeted both CrossCollisionBlock and WallBlock ("Found a remappable @Shadow annotation"), so no diagonal shapes, rotation or neighbour updates applied. Run 37143200753 on 46af105a, with the shadows not remapped, passed all 581 tests and every client shard. The swap design then replaced that approach.

**Fences, panes and bars (PR #138):**
- **Run [37137139804](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37137139804) on 6ad6205b: green**: `mod`, all three client shards and `client`; `repository` passed in run 37137139776.
  - **Server:** all 577 required game tests passed, including the seven diagonal tests. They logged:
    - placed oak fences as `oak_fence[north_east]` and `oak_fence[south_west]`;
    - every pair joining as expected: oak with spruce and with aspen, the cemetery fence with itself, glass with iron bars, red glass with copper bars, bamboo with bamboo; and oak not joining nether brick, the cemetery fence oak, or iron bars oak;
    - "54 blocks join diagonally; problems: []".
  - **Client:** the scene's joins were logged (each diamond's four corners and sides, each zigzag's steps, each slanted row), and the four screenshots were looked at:
    - from above, the zigzags and slanted rows read as unbroken 45-degree lines;
    - the oak and bamboo zigzags run post to post with diagonal rails;
    - the glass, light-blue glass, iron bars and copper bars rows run unbroken on the slant;
    - the cemetery fence and aspen fence diamonds close all the way round, the cemetery fence's pickets spread along its diagonal rails.
- **Failed, then fixed:** run 37137013971 on ca67de64 did not compile its tests: 26.3 has no `Blocks` field for each stained glass pane, and an `Identifier` does not concatenate with a list. The main code and mixins compiled in that run too.

Not run: play; two clients on a dedicated server; a save and reload of joined fences or diagonal walls; other mods' fences and walls; mobs walking along diagonal fences.

## World and event applicability
- Not seasonal and not tied to any dimension.
- World generation is unchanged: villages and structures place their fences and walls as before, without diagonals.

## Rollout and open questions
- **Walls** followed in the second stage, as swapped diagonal walls, and then their tall sides (the owner: "merge it, then add the tall sides").
- **Per-arm heights.** A diagonal wall has one height for all its sides and arms. Giving each its own, as vanilla gives each straight side, would multiply the diagonal walls' states many times over; the one `tall` property covers walls stacked two high and walls under blocks, which are the usual cases.
- **A diagonal wall is its own block ID.** Commands, data packs and other mods that look for `minecraft:cobblestone_wall` by ID do not find a wall while it joins diagonally (they find it by `#minecraft:walls`). The Diagonal Fences mod has the same trade-off.
- Diagonal joins are automatic. A way to stop one forming (for example, sneaking while placing) is a later choice.
