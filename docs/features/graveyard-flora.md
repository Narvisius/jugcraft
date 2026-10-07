# The Graveyard Flora

Status: implemented in source. Not yet played by hand. The Build workflow compiles it, and CI's game tests and client screenshots are recorded below.
Proposal issue: none. The owner asked for it directly on 4 October 2026 ("lets return to agriculture, make a bunch of plants and foliage that would help decorate a haunted graveyard"), with a sheet of chunky hand-built plants as the reference for the look.
Owner: @jimbozoomer-byte
Target milestone and tier: Discovery tier. Every plant is found growing wild, and nothing is crafted except dyes.
Primary specialty and supported player role: building and gardening. A builder can plant a churchyard to look haunted, with shrouded oaks, ivied walls, black roses and funeral lilies, withered grass between the graves and ghost pipes glowing in the dark. A farmer can grow mandrakes, keeping their ears covered. A witch can brew their roots into Flying Ointment.

## Player experience
Seventeen plants and the mandrake. Each one is sculpted in the chunky, hand-built style of the reference sheet rather than drawn as a crossed picture: a few bent stems of small boxes carry flat, cut-out leaves and petals set at angles, and bells, berries, trumpets, cups and roots are little boxes. Each has its own 64 × 64 texture. Small plants turn at random where they stand, so a bed of them doesn't repeat.

### Flowers
Small flowers each give one dye and a suspicious stew effect, and each can be potted:
- **Spider Lily** (red dye; Weakness): a bare stem topped by a ring of narrow, wavy, recurved red petals, with long stamens sweeping up past them.
- **Snowdrop** (white; Absorption): three stems arching over, each hanging a white bell from a green ovary.
- **Deadly Nightshade** (purple; Poison): a dark, leafy, branching plant with dull purple bells and glossy black berries in green star calyces.
- **Bleeding Heart** (pink; Health Boost): deeply cut leaves under two arching stems hung with rows of pink hearts, each with its white drop.
- **Ghost Pipe** (light grey; Invisibility): a clump of waxy white stems, each nodding at the top into a speckled white bell. **It glows** (light 6), and its stems and bells are drawn lit.

Tall flowers stand two blocks high, give two dyes, and bone meal drops a copy of them:
- **Black Rose** (black): a thorny bush of dark stems and leaves with black-red roses, each a cup of packed petals with four outer petals curling away and green sepals beneath, plus a bud.
- **Foxglove** (magenta): a rosette of soft grey-green leaves and a tall spike of drooping pink bells, their mouths spotted, with green buds at the tip.
- **Funeral Lily** (white): a tall stem with lance leaves up it and three white trumpets, each opening into a six-tepalled flower with a green throat and orange anthers, plus a bud.
- **Asphodel** (white): arching strap leaves and a tall stalk with pale six-pointed flowers along it, each tepal with a brown midvein, and pink buds above.

### Foliage
- **Withered Grass**: a tuft of dry grey and straw-coloured blades, some bent over or snapped. **Tall Withered Grass** is the same, two blocks tall with seed heads. Like vanilla's grass, both are replaced when built over and drop only to shears. Bone meal grows withered grass tall.
- **Ghost Fern**: a silvery painted fern with wine-red midribs, its fronds arching out on every side. **Large Ghost Fern** is the two-block form. They follow the same rules as the grasses.
- **Dead Man's Fingers**: black, knobbly, finger-like fungus, pale at the tips, rising from a dark mound. It stands on any sturdy floor, stone included.
- **Grave Moss**: low cushions of dark moss with tiny red stalks and brown capsules, one to four clumps on a block (placed like pink petals).
- **Shroud Moss**: grey-green lichen hanging in strands. It hangs from leaves, from the sturdy underside of a block, or from more of itself, and the lowest block tapers. Bone meal lengthens a strand a block at a time, up to 8. It falls when what holds it goes, and only shears take it.
- **Creeping Ivy**: dark ivy with pale veins and black berries, on any faces of a block, with a few leaves standing out from the wall. Bone meal spreads it to nearby faces, as with glow lichen. Shears take one for each face it covers.

### The mandrake
- **Wild Mandrake**: a rosette of broad, crinkled leaves round a cluster of violet bells, with the top of a root and its two eyes just out of the soil. Pulled by hand it gives one or two **Mandrake Roots**. Shears take the whole plant.
- **Mandrake Root**: a little root figure with a screaming face, root arms raised and root legs, and a tuft of leaves on its head. Plant it on farmland and it grows through four looks: seedling, young rosette, rosette with its shoulder showing, then ripe, flowering, and peering out of the soil. It is also found now and then in grass, like the other crops' seeds.
- **The scream**: pull up a ripe mandrake (or a wild one by hand) and it shrieks. **Every player within 8 blocks with nothing on their head is sickened** (Nausea for 8 seconds). Anything worn on the head covers the ears: a helmet, a hat, a carved pumpkin. Pull one with your own ears covered and you earn **Mind Your Ears**. An unripe mandrake comes up quietly.
- **Flying Ointment**: a mandrake root stirred into a purple brew in the Bubbling Cauldron makes Flying Ointment, as a phantom membrane does. Witches' flying ointments were said to be made of mandrake and nightshade.

### Grave vases
The pack's grave vases take the flora's small flowers by colour: spider lilies make red bouquets, snowdrops and ghost pipes white, deadly nightshade purple, and bleeding hearts mixed.

## Where it grows
In Jugcraft's haunted and dying biomes (through `tools/biomes.py` EXTRAS):

| Biome | Flora |
| --- | --- |
| Ghost Forest | withered grass (and tall), ghost pipes, dead man's fingers, spider lilies, grave moss, black roses, shroud moss |
| Gloomweald | ghost ferns (and large), ghost pipes, deadly nightshade, foxgloves, wild mandrakes, creeping ivy |
| Hallowed Bog | snowdrops, funeral lilies, asphodel |
| Dead Swamp | spider lilies, withered grass, shroud moss |
| Sludge Mire | deadly nightshade, bleeding hearts, shroud moss |
| Dead Forest | withered grass (and tall) |
| Bayou | shroud moss |

In vanilla's biomes too (a plant's "patch", by conventional biome tag): ghost pipes, ghost ferns, deadly nightshade, black roses, dead man's fingers, grave moss and wild mandrakes in spooky biomes (the dark forest and the pale garden); spider lilies in swamps; snowdrops in snowy biomes and taiga; foxgloves in forests. Bleeding hearts, funeral lilies, asphodel, shroud moss, creeping ivy and withered grass grow only in Jugcraft's biomes. Every plant spreads by bone meal or grows from what it drops, as noted above, so a gardener can grow more once one is found. Mandrake roots also come from grass anywhere.

## Connections
- Existing input producer: the biomes branch's biomes and vanilla's spooky, swamp, snowy and forest biomes. Bone meal and shears. Farmland for the mandrake. Grass, for mandrake roots, as with every crop.
- Existing output consumer: dyes (vanilla's dye recipes), suspicious stew, flower pots, composters and bees (vanilla's flower tags), and the graveyard pack's grave vases (`jugcraft:grave_flowers/<colour>`). The Bubbling Cauldron's Flying Ointment, through the mandrake root (item tag `jugcraft:hex/flying`), and so the flying broomstick.
- Technology connection: none needed.
- Magic connection: the mandrake root in Flying Ointment.
- Reachable entry path: every plant is found wild, and bone meal and shears are vanilla's. Mandrake roots also drop from grass anywhere. Nothing is locked behind anything else.
- Required vs optional: all optional. The flora is decoration, and the mandrake root is a second way to Flying Ointment, not the only one.
- How this specialty stays useful without the others: plants are plants. A builder needs nothing but shears and bone meal.

## Balance and automation
- Dyes: one from a small flower, two from a tall one, as vanilla's.
- Bone meal does what it does on vanilla's plants. Tall flowers drop a copy. Grass and ferns grow tall. Shroud moss lengthens to at most 8 blocks. Ivy spreads to a face. The mandrake ripens.
- Mandrake: a crop like the carrot. The root always drops, with a bonus when ripe. A wild one gives 1–2 roots by hand.
- Flying Ointment from a mandrake root: one root in a purple brew makes the brew's three doses, as one phantom membrane does. Each further ointment needs another root.
- The scream is a nuisance with a cure. It does no damage.
- No conversion loops: nothing turns back into what made it.

## Multiplayer and persistence
- Plants are plain blocks: placing, breaking, bone meal and shears are vanilla's server-side handling. Shroud moss falls by a scheduled tick when its holder goes.
- The scream happens on the server, after a player breaks the block (Fabric's after-break event). It looks only at players within 8 blocks of it, once.
- Nothing is saved beyond block states. New IDs only:
  - blocks and items `spider_lily`, `snowdrop`, `deadly_nightshade`, `bleeding_heart`, `ghost_pipe`, `black_rose`, `foxglove`, `funeral_lily`, `asphodel`, `withered_grass`, `tall_withered_grass`, `ghost_fern`, `large_ghost_fern`, `dead_mans_fingers`, `grave_moss`, `shroud_moss`, `creeping_ivy`, `wild_mandrake`;
  - blocks `potted_spider_lily`, `potted_snowdrop`, `potted_deadly_nightshade`, `potted_bleeding_heart`, `potted_ghost_pipe`, `mandrake_crop`;
  - item `mandrake_root`;
  - advancement `mind_your_ears`.
- The plants follow the "biomes" feature switch for their dye recipes, and the mandrake follows "agriculture".

## Dependencies and assets
No new dependencies. Models and textures are made by code from fixed seeds (`tools/flora_models.py` on the toolkit in `tools/flora_art.py`, written out by `tools/flora_data.py`). The flower pots use vanilla's pot and dirt textures by reference. The mandrake's scream plays vanilla's fox screech and ghast scream, pitched up. All original. The look follows the owner's reference sheet; nothing is traced from it.

New code:
- Plant kinds in `tools/plants.py`, read by `JugcraftAgriculture.registerWildPlants`: "grass" (`WildGrassBlock`), "tall_grass" (vanilla's `DoublePlantBlock`), "hanging" (`HangingPlantBlock`) and "vine" (vanilla's `GlowLichenBlock` on vine's properties, so without the glow).
- `Mandrakes`, for the scream.

## Verification
Automated checks run on CI's Build workflow, run 37176762103 on commit 45f11936 (all jobs passed; the screenshots added after it change only documentation):
- `python3 scripts/check_repository.py`: pass.
- `python3 tools/check_mod_data.py`: pass, 1266 IDs with main merged in (1177 at the CI run). Its new graveyard flora check compares `Mandrakes.java` with `MANDRAKE`. It checks that every sculpted model turns its elements only as block models may (one axis, 22.5 or 45 degrees) and stays within -16..32. It also checks each flora texture is 64 × 64, the vanilla-biome patches have their placed features, grasses grow into tall grasses, and Flying Ointment takes a mandrake root.
- `./gradlew build` with the game tests: pass, "All 697 required tests passed", among them:
- `GraveyardFloraGameTests` (eight tests, all pass):
  1. every plant has its block and item; flowers are small or tall flowers with pots and dyes; the ghost pipe glows; each stands on grass (dead man's fingers on stone);
  2. grasses and ferns: nothing by hand, themselves to shears, bone meal grows them tall;
  3. shroud moss: not in open air; hangs from leaves; bone meal lengthens it (the old tip no longer a tip); shears only; it falls when the leaves go;
  4. creeping ivy: two faces give two to shears, none by hand; bone meal spreads it;
  5. the root plants the crop, and bone meal ripens it; ripe screams, unripe doesn't;
  6. pulling a ripe mandrake: roots drop; the bare-headed (puller and a neighbour) are sickened, a helmeted neighbour isn't; a covered puller is spared and earns Mind Your Ears; nobody beyond 8 blocks hears it;
  7. a wild mandrake sheared comes whole and quietly; pulled by hand it gives roots and screams;
  8. the root is a Flying Ointment ingredient; vases take the flora by colour; loot, features, patches and the advancement load.
- `GraveyardFloraClientGameTests` (CI job `client`, all three shards pass): a planted churchyard at dusk and at night, and up close: the flowers, the tall flowers, the ivied wall, the shrouded oak, the mandrake row, and the ghost pipes glowing. Its eight screenshots are in [the agriculture branch](../branches/AGRICULTURE.md#the-graveyard-flora).

Not run:
- Planting and pulling by hand.
- Exploring the biomes for the plants, and looking at worldgen in a fresh world.
- A two-player dedicated server (one player pulling a mandrake by another).
- This environment can't run a game client interactively.

## World and event applicability
The flora grows in the haunted, dying and holy biomes it suits, and in vanilla's matching ones. None of it is seasonal: it grows and works all year, and nothing in it depends on the Halloween event. No caves, mobs, bosses or dimensions are involved.

## Rollout and open questions
- Placed in new chunks only. Worlds made before this find the plants in chunks they haven't generated yet, and mandrake roots in grass anywhere.
- Open: whether the mandrake root should be more than a seed and an ingredient (eaten, it might sicken).
