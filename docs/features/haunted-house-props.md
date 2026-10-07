# The Haunted House's Props

Status: implemented in source. Not yet played by hand. The Build workflow compiles it, and CI's game tests and client screenshots are recorded below.
Proposal issue: none. The owner asked for it directly on 4 October 2026 ("also make a bunch more halloween and graveyard decoration use the attached images for references"), with pictures of Halloween props: a flying eyeball on bat wings, a cluster of pillar candles, a spider web, a stitched monster's head, and a sheet of fall creatures (an owl, a hedgehog, an acorn, an ear of corn and a maple leaf among them). This is the second of two batches of them (Halloween decorations batch 16). It is stacked on the churchyard's ornaments.
Owner: @jimbozoomer-byte
Target milestone and tier: Discovery tier. The props are made from phantom membrane, a spider eye, honeycomb, string, black dye, black wool, iron nuggets, rotten flesh and a slime ball. The plushes are won at the fall fair midway.
Primary specialty and supported player role: building. These props dress a haunted house's rooms: a parlour, a laboratory, an attic.

## Player experience
1. **Flying Eyeball** (two phantom membranes and a spider eye): a bloodshot eye on red bat wings, its iris green and glowing. It hovers in its block with nothing under it, bobbing gently up and down, its wings beating, and **it turns to stare at the nearest player within 10 blocks**, following them about the room (at most 9 degrees a tick, so it turns rather than snaps). With nobody near it looks idly about. Now and then its wings are heard. Nothing collides with it.
2. **Pillar Candles**, ivory (string over two honeycombs) and black (an ivory one and black dye): church candles of different heights, dripping wax, with a puddle of it at their feet. Like vanilla's candles, one to four go in a block, clustered tallest at the back; they are lit with flint and steel (or a fire charge or a burning arrow), put out by hand or by a splash, and give light 3 a candle. A flame burns over each wick. Breaking the block gives back every candle.
3. **Spider Web** (three string make three): a whole web strung over the face of a block, radial threads from a hub, the spiral sagging between them, a broken strand or two, and a small spider waiting by the hub. It goes on walls, ceilings and floors, any number of faces of a block (as glow lichen does), and gives one back for each face. Nothing collides with it.
4. **Monster's Head** (black wool, iron nuggets, rotten flesh and a slime ball): a stitched monster's great green head on a stub of neck and collar, flat-topped, its black hair combed down in a jagged fringe, with a heavy brow over deep-set eyes, a broad nose, stitches across its forehead and a bolt each side of its neck. It faces whoever places it. **Give it a redstone signal and it wakes with a groan**: its jaw drops open on its teeth, its eyes glow (light 6) and sparks crackle at its bolts. It sleeps again when the signal goes.
5. **Harvest plushes**: five more prizes at the fall fair midway, sculpted on 64 × 64 felt textures with stitched seams and embroidered faces:
   - **Owl Plush**: brown felt feathers, a yellow breast, big yellow eyes, a beak, orange ear tufts, folded wings and feet;
   - **Hedgehog Plush**: a back of brown quills with pale tips standing up along its edges, a cream face and snout and a black nose;
   - **Acorn Plush**: a brown acorn with a worried face, little arms and feet, under a scaly cap and stalk;
   - **Corn Plush**: a grinning ear of corn in its husk, the husk's leaves flaring out like arms, silk on top;
   - **Maple Leaf Plush**: a red maple leaf with a grin, stuffed thick and leaning back on its stem, with little mittens.
   They squeak when squeezed, like the midway's other plushes.

## Connections
- Existing input producer: phantoms (membrane), spiders (eye, string), bees (honeycomb), dyes, sheep (wool), iron, zombies (rotten flesh), slimes. The fall fair midway gives the plushes.
- Existing output consumer: building. The plushes join the midway's prize table (`jugcraft:gameplay/midway_prize`) and the item tag `jugcraft:plushes`. Redstone drives the monster's head.
- Technology connection: redstone for the monster's head. Magic connection: none (decoration).
- Reachable entry path: early-game mob drops and a crafting table. The plushes need a High Striker or Ring Toss, both Discovery tier.
- Required vs optional: optional decoration.

## Balance and automation
- Recipes take materials in and give props, and breaking gives the prop back: one eyeball, one head, a candle for each candle, a web for each face. Nothing turns back into what made it, so there is no loop.
- The plushes' weights in the prize table: Owl 12, Hedgehog 12, Acorn 16, Corn 16, Maple Leaf 16, beside the seven existing plushes (102 between them), 174 in all. The Jumbo Pumpkin Plush stays the rarest (1 in 174).
- No ticking on the server. The eyeball's hovering and staring are drawn by the client from the game time; the head changes only on a neighbour update; the candles are vanilla's.

## Multiplayer and persistence
- Placing, breaking, lighting, redstone and squeezing are server-side, through vanilla's paths. The eyeball's look is worked out on each client for that client's world, so each player sees it stare at whoever is nearest.
- Nothing is saved but block states. The eyeball has a block entity that holds nothing (it lets the client draw it).
- New IDs:
  - blocks and items `flying_eyeball`, `ivory_pillar_candle`, `black_pillar_candle`, `spider_web`, `monster_head`, `owl_plush`, `hedgehog_plush`, `acorn_plush`, `corn_plush`, `maple_leaf_plush`;
  - block entity type `flying_eyeball`;
  - their recipes (not the plushes': plushes are won, not made).
- The recipes follow the agriculture feature switch. The pillar candles are in the block tag `minecraft:candles` (so vanilla's lighting finds them), not the item tag, which vanilla's candle cakes use.

## Dependencies and assets
No new dependencies. The models are built on `tools/flora_art.py` by `tools/decor16_data.py`, which paints their 64 × 64 textures by code and writes the eyeball's moving parts to `assets/jugcraft/decor16_quads.json` for its renderer. The pillar candles are vanilla's `CandleBlock` with these models and their own flames' places; the spider web is vanilla's `MultifaceBlock`. All original. The look follows the owner's reference pictures, and nothing is traced from them. Since 5 October 2026 the creature props are painted with the clean, cute painters in `tools/cute_art.py` ([ART_DIRECTION.md](../ART_DIRECTION.md#creatures-and-faces-cute-and-clean)).

## Verification
Automated checks run on CI's Build workflow:
- run 37178906733: compile failure (26.3 has no coloured candle constants), fixed;
- run 37179138062: compiled, 715 of 716 game tests passed; the failure was this record's own test asking a flame to stand below the block's top when the tallest candle's flame stands exactly at it, fixed;
- run 37179323948 on e0095799: every job passed;
- run 37180140539 on 97692652 (the churchyard's ornaments and the flora merged in, and two close-ups reframed): every job passed.

In them:
- `python3 scripts/check_repository.py` and `python3 tools/check_mod_data.py`: pass. The audit's new haunted house check compares `FlyingEyeballBlock`, `PillarCandleBlock` (its layout and flames) and `MonsterHeadBlock` with `tools/decor16.py`. It checks each prop is registered, named, drops and has its recipe and 64 × 64 texture, that the candles are in `minecraft:candles`, and that the eyeball's quads, `DecorQuads` and its renderer are wired up. The midway check covers the five plushes' footprints and weights.
- `./gradlew build` with the game tests: pass in the last two runs, among them:
- `HauntedHousePropsGameTests` (six tests, all pass):
  1. the eyeball is placed, nothing collides with it, it has its block entity, it stays when its floor goes, it bobs within bounds and drops itself;
  2. pillar candles cluster to four and no more, light from flint and steel at 3 a candle, have a flame over each wick, give back four, and give no light unlit;
  3. a spider web covers the wall it was hung on, a second covers the ceiling in the same block, nothing collides with it and it gives two back;
  4. the monster's head faces its builder, wakes and glows while powered, sleeps when the signal goes, and drops itself;
  5. the five harvest plushes are midway prizes and plush blocks;
  6. recipes and loot load.
- `MidwayGameTests` squeezes every plush, the new ones included, and checks the prize table gives only plushes.
- `HauntedHousePropsClientGameTests` (CI job `client`, pass): a parlour open to the south, by day and by candlelight: the whole room, the eyeballs close up, the candles, the monster's heads (one awake), the webs in a corner and the plushes on their shelf. Its seven screenshots are in [the agriculture branch](../branches/AGRICULTURE.md#the-haunted-houses-props).

Not run:
- Building with them by hand.
- A two-player dedicated server (two players watched by one eyeball).
- This environment can't run a game client interactively.

### Look fix, 5 October 2026: the Flying Eyeball's eye and wings
The owner: "the eye on the flying eye has a texture that is like bright white behind the green eye looks weird". Fixed on 5 October 2026 (art fixes); tier, inputs, outputs, costs, IDs and behaviour are unchanged.
- **Cause:** the iris is drawn glowing (full brightness), and its square picture had near-white corners round the green disc and a pure white glint, so in any light below full a glowing white square sat on the world-lit eye. The thin sides of the cornea's cap also squeezed a whole 16-texel picture into half a pixel (32 texels to a pixel), which shimmered as a frame round it. And each wing was two back-to-back quads in one plane: 26.3's cut-out entity type draws both sides of a quad, so the two fought, flickering between two shadings.
- **Fix (`tools/decor16_data.py`):** the iris's corners are now see-through (`iris(corner=None)`), with one small white glint inside the opaque disc (docs/ART_DIRECTION.md, "Creatures and faces"), and the iris sits 0.1 pixel proud of a new white front on the cap, lit like the rest of the eye; the cap's sides sample a thin strip, four texels to a pixel. The white of both eyes is a warmer ivory (`WARM_SCLERA`). The client's wings are one face each, repainted clean by `membrane_wing()`: flat membrane panels between the fingers, alternately lit and shaded, a lit band under the arm, a darker rim on the scallops and a little pale claw. The item's wings keep a face on each side, but the two now lie 0.1 pixel apart (each lifted 0.05 pixel off the wing's plane), so they cannot fight whether or not 26.3's item renderer culls back faces; that it does is not verified. `sclera()` and `iris()` keep their old defaults, so the Jar of Eyeballs (batch 17) is unchanged.
- **Checked offline** (not in a game client): renders of the renderer's quads by day and in the dark from five sides show no white square in the dark (before and after in the branch's previews); no back-to-back pair is left, and the only coplanar pair is two bulge faces hidden inside the eye's body; every face samples about four texels to a pixel. `HauntedHousePropsClientGameTests` takes three new screenshots for CI: `jugcraft_haunted_house_eyeballs_night` (the eyeballs close up by candlelight), `jugcraft_haunted_house_eyeball_back` (one seen from behind, its wings' backs, from the open block between the monster's head and the eyeball, eye level with it) and, last, `jugcraft_haunted_house_eyeball_item` (the item held in the hand and in the hotbar, by day, the HUD shown), to check the item's wings in a real client. CI has not run them yet.

## World and event applicability
Placed by players only; no worldgen. They work all year, and nothing depends on the Halloween event.

## Rollout and open questions
- Open: whether the eyeball should blink, or the monster's head speak a line when it wakes.
