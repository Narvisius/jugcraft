# New trees for Jugcraft's biomes

On 5 October 2026 the owner asked us to "look into each biome and determine what other new trees we need added". This
document answers that. It covers all 99 biomes in `tools/biomes.py`, plus Alpine Spawn and Pixel Hollows. For each
biome it records what grows there now and which vanilla trees are only standing in for the right ones. It then
proposes the trees that would fit, with how each should look and the order to build them in.

It builds on the biome roster ([BIOMES.md](BIOMES.md)) and follows the look in
[NATURAL_TEXTURES.md](../NATURAL_TEXTURES.md), from the wood repaint ([features/wood-repaint.md](../features/wood-repaint.md)).

**Batch 1 is built** on branch `claude/trees-batch-1` ([features/trees-batch-1.md](../features/trees-batch-1.md)), tested in
CI and awaiting review, and not yet played; "Batch 1 as built" under Decisions says where it differs from this plan. The rest is
a plan. Every later tree still needs its own PR, its own feature record in `docs/features/`, in-game screenshots and the
owner's approval of its colours.

**Revised for the owner's own-wood update.** On 5 October the owner also said: "We are ok for making new wood and logs
for a bunch as well, if we need them just alter the ones we have but make sure they are made the same way so they fit
the world!!! Unless they are supposed to be the same wood." So a tree that is botanically a different wood from the one
it borrowed now gets its own wood set, and a tree of the same wood keeps it (rule 9). Sixteen new woods follow from
that, besides batch 1's cedar: fourteen new to the roster, and the yew's and the olive's, which were already planned.
One of them, the fruitwood, is shared by three related fruit trees, the rowan, hawthorn and pear. "Owner's update: own
woods" under Decisions lists every tree's verdict and what changed.

**Finalized after review.** Two more reviews checked that revision on 6 October 2026, one for buildability and one for
art and fit, and every colour figure was recomputed by script. Their points and answers are under "Review of the
own-wood revision" in Decisions, which ends with the choices still left to the owner. The owner has since approved the
cedar as batch 1 built it ("Cedar looks sooo good!!"), so every check here is made against that cedar, #725543.

**The new woods' colours are approved.** On 6 October 2026 the owner saw the sixteen new woods as previewed (each wood's
bark, log end, stripped side and planks, with this roster's defaults: the yew as painted and one fruitwood) and said:
"The new wood looks so so so good". Those colours are the ones to build. The other calls at the end of Decisions (wood
names, stand-ins, sets to cut) keep this roster's defaults unless the owner says otherwise, and each wood is still
checked in game in its own batch.

How it was made: survey groups proposed trees biome by biome, and their proposals were merged into one roster. Three
reviewers then checked the roster: one for whether it can be built and keeps the project's rules, one for art and fit
with vanilla, and one for biome coverage. Their findings were applied, and the last section, "Decisions", says what
changed and why. Leaf and bark colours were worked out by script from the ramps in `tools/wood_style.py`. Each wood was
checked against every other wood, vanilla's and Jugcraft's, by CIE76 ΔE and by CIEDE2000, and each leaf and bark against
the trees it grows beside.

## How to read this

- **Shares.** A biome's trees are given as target shares of its tree tries, adding up to 100. Vanilla's
  `random_selector` tries its picks in order, so the build converts the shares into chances in that order:
  chance_i = share_i / (1 - the sum of the earlier shares). Whatever is left goes to the default.
- **(v)** marks a vanilla wood or a vanilla feature. The mod's `oak_bush` and `tall_vine_oak` are made of vanilla oak,
  so they carry it too.
- **Trunks.** "Straight trunk 6 (+3)" means `base_height` 6 and `height_rand_a` 3. "Forking trunk 3 (+1, +1)" also
  gives `height_rand_b`. Foliage is given by vanilla's placer name and fields: blob, spruce, pine, acacia, bush,
  fancy, cherry, random_spread, jungle, dark_oak.
- **Reuses X.** The tree's trunk is wood X's existing log, because the tree is that wood (rule 9). "Shape only" means
  it needs only a new entry in `trees.SHAPES` and its placed feature, and no new blocks or textures.
- **Own wood X.** The tree has a new wood set of its own, "X": log, wood, stripped log and wood, planks, stairs, slab,
  fence and fence gate, as every current Jugcraft wood has. Its colour comes from one of the owner's paintings, named
  as "painting N" (their first set, from 0) or "2:N" (a row of their second set, `OWNER_BANK`), and "altered" when it
  was recoloured to fit.
- **Stand-in on X.** The trunk is wood X's log, a wood the tree is not. Each stand-in is the owner's call (rule 9).
- **Colours** are six tones, dark to light, as `LEAVES` needs them. They are proposals. Rule 3 of
  `docs/NATURAL_TEXTURES.md` still applies: the owner approves each one beside the vanilla woods before it ships.
- **Names.** A new tree's key is its common name in snake case. Its leaves are `<key>_leaves`, or `<key>_needles` or
  `<key>_fronds` for those kinds, its sapling is `<key>_sapling`, and the shape its sapling grows is `<key>` too, as
  every current tree's is (batch 1 renamed `swamp_cedar` to `cedar` for this). A fruit is named for itself only when no
  other tree could bear it. IDs are permanent after release, so these rules are fixed before the first new tree ships,
  and do not depend on which batch a tree lands in.
  - A wood is named for what its timber is called: its genus's common name (`pine`, `yucca`, `fig`), an invented tree's
    own name (`dragonblood`), or, for one wood shared by several genera, their trade name (`fruitwood`). The current
    woods follow this already (larch, fir, maple, willow, chestnut, eucalyptus), the aspen being the one older
    exception.
  - A tree whose key is its wood's name is that wood's tree, with no tree-and-wood split: the tree `pine` has
    `pine_needles`, `pine_sapling` and the shape `pine`, the windswept pine. A tree with a name of its own keeps it on a
    wood named otherwise, through batch 4's `wood` field: `stone_pine_needles` on pine logs, `joshua_tree_fronds` on
    yucca logs, `banyan_leaves` on fig logs and `rowan_leaves` on fruitwood logs.

## Rules every new tree follows

These come from `docs/NATURAL_TEXTURES.md` and `CLAUDE.md`, and from the reviews.

1. **The look.** Each tree is drawn by code in vanilla's manner, never copied or recoloured from Mojang.
   - Everything is 16×16.
   - Each material is one colour stepped into a ramp.
   - Bark and grain run vertically, with no bands across the trunk.
   - Leaves are a speckle of small clumps with a few dark-backed gaps.
   - Each tree has one trait of its own.
   - Silhouettes come from vanilla's own trunk and foliage placers, so a new tree reads as more of the same world.
2. **Leaves.**
   - Six hand-set tones for every look, the darkest about 0.3 times the lightness of the lightest.
   - The same seed draws every seasonal look.
   - Gaps follow the kind's default: about 5% for leaves, 2.4% for needles, 3.5% for blossom and 6.4% for fronds. A
     new optional per-entry threshold covers the few trees that differ.
   - Fruit and flower clusters are placed from each tree's own seed, at least 4 pixels apart and wrapping at the
     edges, so species do not share a dot pattern.
   - Petals and blossom keep their main tones at lightness 0.86 or below. Only single-pixel highlights go above 0.9.
3. **New drawing options in `wood_style.py`** (batch 3, with the yew, whose dense needles need the gap threshold):
   - blossom entries get an edge-green ramp, an under-gap colour and a flower coverage;
   - fronds get a midrib count, length and angle;
   - `LEAVES` takes a per-entry gap threshold.

   Bare twigs are drawn in each tree's own bark colours, as `BARE` does now. The `oak_twigs` and `dark_oak_twigs` ramps
   once planned for trees on vanilla logs are dropped: the persimmon and walnut now have woods and barks of their own,
   and the hawthorn has the fruitwood's (rule 9). The elder, whose stem is hidden, gets a small twig palette of its own.
4. **Feature switches.** A wood's planks, wood and sawmill recipes are on when *any* feature that grows its logs is
   on, and so are a tree's leaves and sapling. `WOOD_SWITCHES` becomes a list, and `check_mod_data` checks that every
   shape's log wood and leaves cover every feature that places the shape. This lands in batch 1, before any borrowed
   larch ships. Built: `jugcraft:feature_enabled` takes an `"or"` list; larch is alpine_spawn or biomes, and the
   chestnut agriculture or biomes (the Orchard places it). The check covers the biomes, Alpine Spawn and agriculture's
   wild patches, and each wood's hand and sawmill recipes. Each new wood's section states its switch list.
5. **Placement.**
   - Picks are shares (above).
   - A pick that fails its survival check places nothing, so a tree is never picked where its sapling cannot stand.
   - Trees that grow on sand take `survives_as: minecraft:dead_bush`, as the palm does.
   - Rare lone trees are their own extras (a rarity filter, then the tree's survival check), not trees picks.
   - Cave trees use the cave extras' `environment_scan` placement.
   - Nether trees use `count_on_every_layer`.
   - Vanilla has no absolute-height filter, so there is no "above y 90".
6. **Fruit and ticking.**
   - Fruit ripens on random ticks whatever the season, so `seasons.mode=off` gates nothing.
   - Only tree-grown leaves with air below fruit, as the chestnut's do.
   - Evergreen fruiting leaves tick only while unripe.
   - Each record states its ticking budget.
7. **No gain loops.** Anything that bone meal grows returns less than one bone meal's worth in compost. Each record
   writes out the arithmetic.
8. **Records and tests.**
   - Each batch updates the biome records it changes (the files are named in "Biomes by biome").
   - Each new tree gets its own feature record: tier, producer, consumer, costs, unlocks, failure behaviour and test
     evidence.
   - `WoodClientGameTests` grows each new tree in CI beside vanilla's trees and its neighbours, and shoots it.
9. **Own wood or the same wood** (the owner's update of 5 October 2026: "We are ok for making new wood and logs for a
   bunch as well, if we need them just alter the ones we have but make sure they are made the same way so they fit the
   world!!! Unless they are supposed to be the same wood.").
   - **A different wood gets its own wood set.** A tree that is botanically a different wood from every existing one,
     and is a real tree whose timber a player would use, gets the full set: log, wood, stripped log and wood, planks,
     stairs, slab, fence and fence gate, plus its own leaves and sapling. A pine is not a larch, a magnolia is not a
     willow, and a rowan is not a maple.
   - **The same wood is kept.** A tree of the same genus as an existing wood keeps that wood's logs: a tamarack is a
     larch, a mallee is a eucalyptus, a wattle is an acacia, and a peach or a snowpetal cherry is a cherry. The palm
     wood stands for the true palms as a family (Arecaceae), so the date palm, another true palm, keeps it too.
   - **One timber, one wood.** Trees of one genus share one new wood: the windswept pine, the stone pine and the
     bristlecone all grow pine wood. Three related fruit trees of the tribe Maleae, the rowan, hawthorn and pear, share
     one fruitwood: their pale, pinkish, fine timbers are used alike, and the rowan's genus is even sold as "Swiss
     pear". Drawn as three woods they read as one dusty-pink family. This one is the owner's call (see the rowan).
   - **No wood set** for shrubs on a hidden stem (gorse, sagebrush, sea grape, elder, bramble bush), for plants with
     special trunk blocks of their own (banana, tree fern, rubber tree), or for fungi.
   - **A stand-in is the owner's call.** A tree whose visible trunk is a wood it is not stands on its nearest relative
     in the game, and each is put to the owner: the flame tree on acacia (the same subfamily), the osmanthus on olive
     (the same tribe), the pandanus on palm (another order of monocots), the orange on oak, and the rubber tree, whose
     own `rubber_log` saws into jungle planks.
   - **Made the same way.** A new wood's colour comes from the owner's own paintings: an unused one where it fits,
     otherwise one of theirs altered (recoloured) by the least change that fits. It is drawn by `tools/wood_style.py`'s
     code exactly as the current woods are (`docs/NATURAL_TEXTURES.md`).
   - **Fits beside vanilla.** `NATURAL_TEXTURES.md` rule 3 holds: no brighter, more saturated or more contrasting than
     vanilla's woods, and different in hue from the nearest of them. This roster reads it by script (CIE76 ΔE on Lab,
     HLS hue). Each new wood is ΔE 12 or more from every vanilla wood; a vanilla wood within ΔE 15 is at least 8 degrees
     of hue away when both have saturation 0.15 or more; and it is ΔE 10 or more from every other Jugcraft wood. The one
     exception is the owner's own yew (see the yew).
   - **A second gate, by eye.** CIE76 overweights chroma, which hides closeness among the low-chroma tans, greys and
     pinks where the new woods sit, so CIEDE2000 is checked too. A pair closer than vanilla's own closest pair (oak and
     jungle, CIEDE2000 9.3) is not refused, but is shown to the owner as a wall of both planks, lit and at Minecraft's
     side shade, to judge by eye. Builders put planks side by side, so never sharing a biome is no reason for close
     planks to pass; it can excuse only close barks, which are seen in the wild.
   - **The owner approves the reading.** This reading of rule 3 is the owner's to approve. Batch 2, the first batch to
     use it, adds it to `NATURAL_TEXTURES.md`.
   - **At most one new wood set to a PR,** as batches 1, 2, 3 and 7 have.

## The roster

57 trees. Priority is the value for the owner's world against the cost. The batch is its place in the build order.

"New wood" in the Trunk column is a tree's own wood set (rule 9): 17 in all, batch 1's cedar and 16 more. "A stand-in"
marks a trunk of a wood the tree is not, which is the owner's call (rule 9). A batch with a letter (4b, 5c) split off
its parent so that each PR brings at most one new wood set, or stays small.

| Tree | Species | Trunk | New blocks | Priority | Batch |
|---|---|---|---|---|---|
| stunted_fir (and bog_fir) | young balsam fir; black spruce | fir | none | high | 1 |
| subalpine_fir | subalpine fir | fir | none | medium | 1 |
| fir_bush | krummholz fir | fir | none | medium | 1 |
| tamarack | tamarack | larch | none | high | 1 |
| dead_snag | standing dead snag | dead | none | high | 1 |
| willow_bush | shrub willow | willow | none | medium | 1 |
| young_aspen | aspen suckers, bog birch | aspen | none | low | 1 |
| cedar (the swamp cedar) | Atlantic and northern white cedar | cedar (new wood) | cedar wood set, leaves, sapling | medium | 1 |
| mossy_maple | bigleaf maple | maple | none | high | 1 |
| cottonwood | plains cottonwood, white poplar | aspen | none | high | 2 |
| japanese_maple | Japanese maple | maple | none | medium | 2 |
| live_oak | southern live oak | oak (v) | none | high | 2 |
| pine (the windswept pine) | windswept and cliff pines | pine (new wood) | pine wood set, needles, sapling | high | 2 |
| big_jacaranda | mature jacaranda | jacaranda | none | medium | 2 |
| mallee | mallee eucalypts | eucalyptus | none | medium | 2 |
| giant_eucalyptus | mountain ash | eucalyptus | none | medium | 2 |
| yew | English yew | yew (new wood) | yew wood set, needles, sapling, arils | high | 3 |
| snowpetal_cherry | Yoshino cherry | cherry (v) | leaves, sapling | high | 4 |
| shimmer_birch | invented aurora birch | birch (v) | leaves, sapling | high | 4 |
| bald_cypress | bald cypress | bald cypress (new wood) | bald cypress wood set, needles, sapling | high | 4b |
| magnolia | southern magnolia | magnolia (new wood) | magnolia wood set, leaves, sapling | medium | 4c |
| rowan | rowan | fruitwood (new wood) | fruitwood set, leaves, sapling, berries | high | 5 |
| elder | elder | hidden oak log (v) | leaves, sapling, elderberries | medium | 5 |
| hawthorn | common hawthorn | fruitwood (batch 5) | leaves, sapling, haws | high | 5b |
| persimmon | American persimmon | persimmon (new wood) | persimmon wood set, leaves, sapling, persimmons | high | 5c |
| juniper | junipers | juniper (new wood) | juniper wood set, needles, sapling, berries | high | 6 |
| gorse | gorse | hidden oak log (v) | leaves, sapling | high | 6 |
| sagebrush | big sagebrush | hidden dead log | leaves, sapling | medium | 6 |
| holly | European holly | holly (new wood) | holly wood set, leaves, sapling, sprigs, Holly Wreath | medium | 6b |
| olive | olive | olive (new wood) | olive wood set, leaves, sapling, olives | high | 7 |
| stone_pine | the nut pines | pine (batch 2) | needles, sapling, pine nuts | high | 8 |
| wattle | golden wattle | acacia (v) | leaves, sapling | high | 8 |
| tamarisk | tamarisk | tamarisk (new wood) | tamarisk wood set, leaves, sapling | low; a cut candidate | 8b |
| joshua_tree | Joshua tree | yucca (new wood) | yucca wood set, fronds, sapling | high | 8c |
| huge_glowcap | invented giant glowcap | mushroom stem (v) | cap block, light block | high | 9 |
| blight_fungus | invented dead huge fungus | dead | wart block, small fungus | high | 9 |
| marrow_fungus | invented bone fungus | bone block (v) | none | medium | 9 |
| bramble_bush | invented bramble thicket | hidden crimson stem (v) | leaves | high | 9 |
| dragonblood | after Socotra's dragon tree | dragonblood (new wood) | dragonblood wood set, fronds, sapling, resin | medium | 9b |
| baobab | baobab and boab | baobab (new wood) | baobab wood set, leaves, sapling, pod, fruit | high | 10 |
| date_palm | date palm | palm | date cluster, dates | medium | 10 |
| flame_tree | royal poinciana | acacia (v), a stand-in | leaves, sapling | medium | 11 |
| sea_grape | sea grape | hidden oak log (v) | leaves, sapling, sea grapes | medium | 11 |
| pandanus | screw pine | palm, a stand-in | none | low; dropping it is recommended | 11 |
| osmanthus | sweet osmanthus | olive (batch 7), a stand-in | leaves, sapling, flowers | low | 11 |
| driftwood | bleached driftwood | stripped dead log | none (one small feature) | low | 11 |
| banyan | banyan | fig (new wood) | fig wood set, leaves, sapling | medium; the first cut candidate | 11b |
| mango | mango | mango (new wood) | mango wood set, leaves, sapling, mangoes | medium; a cut candidate | 11c |
| rubber_tree | Pará rubber tree | rubber log on jungle planks, a stand-in | built by agriculture slice 5 | high | 12 |
| banana | banana | banana stem | stem, leaves, sapling, bunch, bananas | high | 12 |
| tree_fern | tree fern | tree fern trunk | trunk, fronds, sapling | medium | 12 |
| buttressed_mahogany | big-leaf mahogany on buttresses | mahogany | none | medium | 12 |
| walnut | walnut | walnut (new wood) | walnut wood set, leaves, sapling, walnuts | medium | 13 |
| bristlecone | Great Basin bristlecone pine | pine (batch 2), mostly stripped | none | medium | 13 |
| pear | pear | fruitwood (batch 5) | leaves, sapling, pears | medium | 14 |
| peach | peach | cherry (v) | leaves, sapling, peaches | low | 14 |
| orange | sweet orange | oak (v), a stand-in | leaves, sapling, oranges | medium; pending the owner's call | 14 |

### Batch 1: northern, wet and cool-forest shapes

Built on `claude/trees-batch-1` ([features/trees-batch-1.md](../features/trees-batch-1.md)), awaiting review and CI; not yet played.

#### stunted_fir and bog_fir
Young or suppressed balsam fir, and black spruce drawn on fir. **High.** It makes the Muskeg and the Fen match their
own records, and gives the conifer forests an understory. Two shapes cost nothing in textures.
- **Grows in:**
  - stunted_fir: Coniferous Forest 8%, replacing vanilla spruce rather than standing beside it; Snowy Coniferous
    Forest 10%.
  - bog_fir: Muskeg 30%, replacing the full-size fir; Fen 50%, as its default ("short firs"); Bog 15%.
- **Shape:**
  - stunted_fir: straight trunk 4 (+3) under spruce foliage (radius [1, 2], offset [0, 1], trunk_height [0, 1]). A
    squat cone 5-8 blocks tall with needles nearly to the ground.
  - bog_fir: the same trunk under spruce foliage (radius [0, 1], offset [0, 1], trunk_height [2, 3]). A pencil-thin,
    ragged spire with a tuft at the top, like black spruce's "club top".
- **Bark, wood and leaves:** fir's plated dark-brown bark, straw wood #ceb678 and needles (#0d261c-#457d5c).
  Evergreen.
- **Wood:** reuses fir, shape only.

#### subalpine_fir
Subalpine fir (*Abies lasiocarpa*), in its spire form. **Medium.** Narrow "snow ghost" spires are the silhouette of
snowy high forests, and a few dark spires among gold aspens are the Rocky Mountain view.
- **Grows in:** Snowy Coniferous Forest 20%, from the fir's share; Aspen Glade 5%, where it keeps the glade green in
  winter.
- **Shape:** straight trunk 10 (+4) under spruce foliage (radius [1, 2], offset [1, 2], trunk_height [3, 4]). A short
  bare bole shows, and the spire flares slightly at its foot, so it does not read as the cypress's column. 12-15
  blocks.
- **Bark, wood and leaves:** fir's. Snow settles on each tier.
- **Wood:** reuses fir, shape only.

#### fir_bush
Krummholz: wind-stunted fir. **Medium.** It keeps some green in the tundra in winter, when the maple and willow scrub
there is bare.
- **Grows in:** Tundra 20%.
- **Shape:** one log under bush foliage (radius 2, offset 1, height 2): a low, dark, wind-pressed mound.
- **Wood:** reuses fir, shape only.

#### tamarack
Tamarack (*Larix laricina*). **High.** The defining tree of muskegs and fens: gold spikes over a frozen bog in
autumn, at the cost of one shape.
- **Grows in:** Muskeg 35%, from the dead tree's share; Bog 30%, over its red-orange grass; Fen 35%, replacing
  vanilla dark oak.
- **Shape:** straight trunk 6 (+3) under pine foliage (radius 1, offset 1, height 3; built with 3, not 4, see
  Decisions). A thin pole 7-10 blocks tall
  with a narrow, sparse tuft in its top third, taller and thinner than Alpine Spawn's larch cone.
- **Bark, wood and leaves:** larch's red-brown plated bark and amber wood #c98634. The larch needles' three looks are
  already drawn: green; gold from about day 268; bare from about day 318.
- **Wood:** reuses larch, shape only.
  - Larch joins `FALLEN` (length 5-8), so tamaracks lie fallen too: fallen larch is 2% of the Muskeg's trees.
  - The larch sapling still grows the alpine cone.
  - Larch's switch list becomes alpine_spawn and biomes (rule 4).

#### dead_snag
A standing dead snag, killed by fire, beetles or flood. **High.** Real dead stands are mostly straight snags. A
second dead silhouette makes the dead biomes read as killed forests, not a scatter of identical props, and gives the
Ghost Forest its colonnade.
- **Grows in:**

  | Biome | Share | Note |
  |---|---|---|
  | Ghost Forest | 55% | the default; the dead tree stays at 30% |
  | Dead Forest | 35% | |
  | Dead Swamp | 50% | standing in its ponds |
  | Muskeg | 20% | |
  | Coniferous Forest | 3% | |
  | Burnt Forest | 45% | |
  | Wasteland | 40% | |
  | Cinder Barrens | 1-2 tries a chunk | on its coarse-dirt patches, about one chunk in eight |
  | Gloomweald | 10% | with its new canopy, in batch 3 |
  | Rotted Expanse | 30% | |

  The existing fallen dead log also joins the Dead Swamp, Burnt Forest and Rotted Expanse.
- **Shape:** two forms, both on the dead tree's no-foliage path (air foliage).
  - dead_snag: straight trunk 3 (+4, +3). A straight grey spar 3-10 blocks tall, broken off at the top.
  - dead_snag_bent, about a quarter of snags: bending trunk 4 (+3), bend_length 1, giving one crooked top. As built,
    `min_height_for_leaves` 8 (above the trunk), so only the bend carries the air crown and no trunk log's crown can
    open a hole in 2-deep swamp water.
  - The forking trunk is not used, because with no leaves it reads as a bare acacia.
- **Bark and wood:** dead wood's grey furrowed bark (#33302d-#908a81) and grey wood #7a7a7a.
- **Wood:** reuses dead wood, shape only. It survives as an oak sapling would, as the dead tree does, so it keeps to
  soil and coarse dirt.

#### willow_bush
Shrub willow (*Salix*): arctic, bog and riverbank scrub. **Medium.** It replaces vanilla-oak scrub with the shrub that
really grows where floods reach, and it changes with the seasons where the oak bush does not.
- **Grows in:**
  - Floodplain 70%, as its default, replacing the oak bush.
  - Lush River 70%, replacing the oak bush; the existing willow also joins at 20%.
  - Tundra 30%, turning yellow beside the red maple scrub.
  - Fen 15%, as carr scrub.
- **Shape:** straight trunk 1 (+1) under bush foliage (radius 2, offset 1, height 2). A low, round mound of willow
  leaves, with no hanging curtains.
- **Leaves:** willow's three looks: green; yellow from about day 283; bare from about day 328.
- **Wood:** reuses willow, shape only.

#### young_aspen
Quaking aspen suckers, and the bog birch form. **Low.** Trees of mixed ages make the glade read as one living clone,
and white trunks make the Hallowed Bog pale.
- **Grows in:** Aspen Glade 15%; Hallowed Bog 30%, replacing the vine-hung oak (v), in water up to 2 deep.
- **Shape:** bending trunk 4 (+2), bend_length 1, `min_height_for_leaves` 7, under a small blob crown (radius 2,
  offset 0, height 3). The leaf height sits above the tallest trunk, so only the bend carries a crown rather than a
  stack of blobs. A thin, slightly leaning white pole 6-7 blocks tall.
- **Leaves:** aspen's, with its seasons.
- **Wood:** reuses aspen, shape only.

#### swamp_cedar (built as `cedar`)
Atlantic and northern white cedar (*Chamaecyparis thyoides*, *Thuja occidentalis*). **Medium.** The real conifer of
cedar swamps, replacing vanilla spruce and dark oak and keeping the wetland green beside its bare winter willows.
- **Grows in:** Wetland 50%, as its default, replacing vanilla spruce; Ghost Forest 15%, replacing vanilla dark oak as
  the living fringe beside its own dead trunks.
- **Shape:** straight trunk 7 (+3) under spruce foliage (radius [1, 2], offset [0, 1], trunk_height [3, 5]). A clear
  stringy red-brown bole shows under a dense, narrow cone 7-10 blocks tall, so it reads as a cedar, not another small
  fir.
- **Bark, wood and leaves (as built):** its own wood, **cedar**, after the owner's 5 October update ("We are ok for
  making new wood and logs for a bunch as well ... Unless they are supposed to be the same wood"): the swamp cedar is
  not a cypress.
  - Bark: the owner's painted western red cedar (their second set, row 0, moved from `OWNER_BANK` into `WOOD` as
    "2:0" and `BARK`), #483229 #513a2f #563d31 #65493a #6b4f40, darkened by the wood's lightness ratio (0.866) to
    #3e2b24 #463229 #4a352a #573f32 #5d4437, so the log keeps the painting's contrast with its stripped side. Stringy.
  - Wood: the painting's #84654d, darkened to #725543 so it stands off vanilla spruce and the cypress (ΔE 10.0 and 10.6).
  - Leaves: `cedar_leaves`, sage-green scale-leaves #27301a #343f22 #43522c #536539 #657a46 #7d9657, ΔE 14.8 from the
    cypress's and 16.1 from the fir's. Evergreen.
- **Wood:** the cedar wood set, leaves and sapling. The shape is named `cedar`, which the cedar sapling grows.

#### mossy_maple
Bigleaf maple (*Acer macrophyllum*). **High.** The temperate rainforest's own moss-draped broadleaf giant in place of
a vanilla oak, so the "dripping" rainforest finally has moss on its trees.
- **Grows in:** Temperate Rainforest 11%, replacing the vine-hung oak (v); Redwood Forest 5%.
- **Shape:** fancy trunk 9 (+4) under fancy foliage (radius 2, offset 4, height 4), 10-14 blocks.
  - Vanilla's `attached_to_logs` decorator lays moss carpet on the upper faces of its limbs (probability 0.35,
    direction up).
  - Vine decorators: trunk vines, and leaf vines at 0.3.
- **Leaves:** maple's: green; red, orange and gold; bare.
- **Wood:** reuses maple, worldgen only.

### Batch 2: warm and mountain shapes, the pine, and the Lush Desert fix

#### cottonwood
Plains cottonwood and white poplar (*Populus deltoides*, *P. alba*), the same genus as the aspen. **High.** It
replaces vanilla oaks with the giant of floodplains and prairie creeks, and gives a golden crown over golden grass in
autumn.
- **Grows in:** Floodplain 30%, replacing vanilla oak; Prairie 10%, replacing most of the vanilla fancy oak (5% stays
  as the odd bur oak); Lush River 10%.
- **Shape:** fancy trunk 10 (+6) under fancy foliage (radius 2, offset 4, height 4). A tall trunk 10-16 blocks high,
  forking into heavy limbs under a broad, billowing crown.
- **Bark and wood:** aspen's white bark with dark marks and near-white wood #d5d1ce, so it reads as the white poplar.
  The aspen is a poplar too, so this is the cottonwood's own wood, the same pale, soft poplar timber, and not a
  stand-in (rule 9). The move to willow logs once planned for batch 4 is dropped: willow is *Salix*, a different genus.
- **Leaves:** aspen's: green; gold from about day 258; bare from about day 302.
- **Wood:** reuses aspen, the same genus. Worldgen only, as big_maple is. Optionally an aspen sapling grows one a tenth
  of the time, as an oak sapling grows a fancy oak.

#### japanese_maple
Japanese maple (*Acer palmatum*). **Medium.** Red maples round the steaming pools. The Hot Springs' grass already
follows the seasons, but its trees never change; these do.
- **Grows in:** Hot Springs 30%, replacing vanilla spruce.
- **Shape:** forking trunk 2 (+1, +1) under blob foliage (radius 2, offset 0, height 2). Two low, overlapping domes
  4-5 blocks tall and wider than tall. Acacia pads are kept for the jacaranda family.
- **Leaves:** maple's, with its seasons.
- **Wood:** reuses maple, the same genus (*Acer*), worldgen only.

#### live_oak
Southern live oak (*Quercus virginiana*). **High.** The signature silhouette of the humid subtropics and the bayou,
made entirely from vanilla parts.
- **Grows in:** Subtropics 30%, replacing vanilla oak; Bayou 15%, replacing the vine-hung oak (v).
- **Shape:** vanilla's cherry trunk placer throws out long, nearly level limbs:
  - base 4 (+1), branch_count [2, 3], branch_horizontal_length [4, 6];
  - branch_start_offset_from_top [-3, -2], branch_end_offset_from_top [-1, 0].

  Each limb ends in a flat pad of blob foliage (radius 3, offset 0, height 2). The tree is 6-8 blocks high and up to
  about 15 across. Vanilla's `attached_to_leaves` decorator hangs the mod's existing **shroud moss** under the limbs
  (direction down, about 0.3), so one hanging moss stands for Spanish moss in all the warm swamps. Pale hanging moss
  stays with the pale oak.
- **Wood and leaves:** vanilla oak logs and leaves, placed and not copied, as great_oak and tall_vine_oak are. A live
  oak is an oak (*Quercus*), so this is its true wood, not a stand-in (rule 9).

#### pine (the windswept pine)
Windswept white and jack pine of the Canadian Shield, Huangshan pine and Japanese black pine. **High.** The iconic cliff
pine replaces taiga pines in three mountain biomes, and brings the mod's pine wood, which every pine on the roster
shares.
- **Grows in:**
  - Shield 25%, replacing vanilla pine. The Shield's vanilla oak also becomes aspen, and larch joins at 8%.
  - Karst Pinnacles 65%, as its default, replacing vanilla pine.
  - Hot Springs 55%, replacing most of the vanilla pine. Its placed feature, `jugcraft:pine_checked`, stands there
    beside vanilla's `minecraft:pine_checked`.
- **Shape:** the shape `pine`, which its sapling grows. Vanilla's cherry trunk placer, with acacia foliage (radius 1) at
  every tip, gives small flat pads at different heights: a leaning, layered cliff pine 6-9 blocks tall.
  - base 5 (+2), branch_count [2, 3], branch_horizontal_length [2, 3];
  - branch_start_offset_from_top [-4, -2], branch_end_offset_from_top [-1, 0].

  It does not use vanilla acacia's own forking and acacia placers, which would read as acacias in pine colours.
- **Bark, wood and leaves:** its own wood, **pine** (rule 9). Vanilla spruce logs and needles were a stand-in: vanilla
  reuses spruce for its own pines, but a pine (*Pinus*) is not a spruce (*Picea*).
  - Bark: plated, the orange-brown plates of an old pine: #2d2521 #43312a #5f4433 #805a3d #9c6c48. ΔE 9.2 from the fir's
    plated bark beside it in the Shield. The larch's plated orange-brown bark is ΔE 9.8 away, and the two share the
    Shield and Alpine Spawn; their needles and silhouettes tell them apart, which the CI screenshots check.
  - Wood: #a49a84, a warm grey-tan (hue 41, lightness 0.58, saturation 0.15), from the owner's painting 6 (#a5977e)
    altered by ΔE 2.6. ΔE 18.8 from vanilla birch, the nearest vanilla wood, 4.4 degrees of hue away, so it passes rule
    3's reading on ΔE; 10.4 from the eucalyptus and 13.0 from the chestnut; 10.1 from the mango and 10.4 from the
    baobab. By CIEDE2000 the chestnut (7.3) and the eucalyptus (7.8) are closer than vanilla's oak and jungle; by eye
    its greyness sets it apart, which the owner judges on the wall pairs.
  - Leaves: `pine_needles`, the soft blue-green of white pine: #193a2c #224f3d #2c654d #357b5e #45896d #53a583 (hue
    155). ΔE 13.1 from the fir's needles beside it in the Shield, about 14 from vanilla's spruce needles as tinted (an
    estimate), and 12.5 from the stone pine's. Evergreen, with the needles' default gaps.
  - Sapling: `pine_sapling`, a cross sprite in the needle tones on a pine-bark stem. It grows the shape `pine`.
- **Wood:** NEW wood set "pine": log, wood, stripped log and wood, planks, stairs, slab, fence and fence gate.
  - The tree is registered as `pine`: its wood, `pine_needles`, `pine_sapling`, and the shape `pine`, the windswept
    pine, which its sapling grows. That is the one-name pattern of every current tree, which batch 1 kept by renaming
    `swamp_cedar` to `cedar`, so batch 2 needs no batch-4 change.
  - The stone pine (batch 8) and the bristlecone (batch 13) grow on the same logs. The pine's switch list is biomes, and
    gains alpine_spawn when the stone pine lands (rule 4).

#### big_jacaranda
A mature jacaranda (*Jacaranda mimosifolia*). **Medium.** Old giants among the small umbrellas: a mix of sizes and
ages makes a grove feel real.
- **Grows in:** Jacaranda Glade 15%; Glimmer Grove 15%.
- **Shape:** fancy trunk 7 (+4) under fancy foliage (radius 2, offset 4, height 4). An open, branching crown 10-14
  blocks tall, standing to the jacaranda as big_maple stands to the maple.
- **Bark, wood and leaves:** jacaranda's grey-brown furrowed bark, the owner's mauve wood #7a5a5e and violet blossom,
  in bloom all year.
- **Wood:** reuses jacaranda, the same species, shape only.

#### mallee
Mallee eucalypts: small, multi-stemmed *Eucalyptus*. **Medium.** Small gums are the commonest tree of dry Australia.
With the baobab, they make the Outback read as Australian rather than a generic acacia desert.
- **Grows in:** Outback 30%.
- **Shape:** forking trunk 3 (+1, +1) under random_spread foliage (radius 2, offset 0, foliage_height 2, 40
  attempts). An airy, leaning gum 5-6 blocks tall. It survives as a dead bush would, so it stands on red sand.
- **Bark, wood and leaves:** eucalyptus's green bark with rainbow streaks, tan wood #bda281 and blue-green leaves.
- **Wood:** reuses eucalyptus, shape only. A mallee is a growth form of *Eucalyptus*, so it is the same wood (rule 9).

#### giant_eucalyptus
Mountain ash (*Eucalyptus regnans*). **Medium.** It gives the third big-tree wood the same four-sapling giant as the
redwood and mahogany have, and makes the biome live up to "tall eucalyptus".
- **Grows in:** Eucalyptus Forest 8%. Four eucalyptus saplings in a square also grow it.
- **Shape:** giant trunk 24 (+6, +8), two blocks wide, under a small, airy random_spread crown high up (radius 4,
  offset 0, foliage_height 4, 120 attempts).
- **Wood:** reuses eucalyptus. Needs a `"giant"` entry in `agriculture.TREES`, which makes the sapling a
  `GiantSaplingBlock`. No new blocks. Mountain ash is a eucalyptus, and one genus shares one wood (rule 9). Its real
  bark is smooth grey-white ribbons rather than the rainbow eucalyptus Jugcraft draws, but its timber (sold as
  "Tasmanian oak") is the same pale tan.

**Also in batch 2:**
- **The Lush Desert defect.** Its oak-bush pick (30%) checks an oak sapling, which can never stand on its red sand or
  sand, so the pick places nothing. The small palm replaces it now, since desert fan palms grow wild in the Mojave
  beside Joshua trees. Grass patches are added to its surface so that its `bushes_dense` and `meadow_wildflowers`
  extras have dirt to grow on.
- **`apple_tree_checked`** is added to `biomes.PLACED_TREES` (the apple tree on an apple-sapling check), for the
  Flower Isle now and the Orchard in batch 14.
- **Rule 3's reading.** Rule 9's bar, with CIEDE2000 shown to the owner by eye, goes into `NATURAL_TEXTURES.md` once the
  owner approves it. The pine is its first user.

### Batch 3: the yew, and the leaf drawing options

#### yew
English yew (*Taxus baccata*). **High.** The yew is the tree of churchyards and folklore, and the owner has already
painted a wood for it. It gives the Gloomweald its own ominous evergreen in place of vanilla spruce, and gives builders
a tree for the mod's Halloween graveyards and cemetery fences. Halloween opens on 20 October. The yew has its own wood
and leaves, the pattern every current tree uses, so it needs none of batch 4's shared change and can ship first. Its
dense needles need the per-entry gap threshold, so the leaf drawing options of rule 3 land in this batch, with it.
- **Grows in:**
  - Gloomweald, 20% of its own new canopy list.
  - Player churchyards, which is where its sapling is the point.
  - It is not placed in the Dense Forest, where it would stand beside the holly as a second dark evergreen with red
    berries.
- **Shape:**
  - yew: straight trunk 2 (+2) under blob foliage (radius 3, offset 0, height 4). A low, broad, very dark dome 5-7
    blocks tall, wider than tall, its crown almost to the ground.
  - ancient_yew, which four saplings in a square grow: vanilla's dark_oak trunk and foliage (two blocks wide,
    gnarled), with podzol beneath (`alter_ground`).
- **Bark:** the owner's painted yew bark (`OWNER_BANK` row 7: #473729 #564636 #614f3f #6e5d4a #766551), plated as
  painted, vertical only.
- **Wood:** the owner's yew, #654135, a dark red-brown.
  - It shares the cypress's and redwood's hue (about 15) but is much darker: lightness 0.30 against 0.46.
  - It is far less saturated than the mahogany.
  - Check it beside the cedar (batch 1, #725543: ΔE 8.8, the nearest wood), the cypress, mahogany and redwood, and
    vanilla's mangrove and dark oak, before it ships.
  - It is the one wood under rule 9's ΔE 10 between Jugcraft woods: ΔE 8.8 (CIEDE2000 7.6) from the cedar as batch 1
    built it, which the owner has since approved. It is kept as the owner painted it, and the exception is owned, not
    excused: builders will put yew and cedar planks side by side, so the two never sharing a biome is no reason.
  - **Owner's call, before batch 3 ships:** the yew as painted, beside the cedar; or a visible move to #553731, ΔE 6.9
    from the painting, darker and a little redder (hue 10, lightness 0.26), which is ΔE 14.0 from the cedar and
    CIEDE2000 9.5 or more from every wood. The wall-pair sheet shows both beside the cedar. The near-copy #633e32 once
    offered here (ΔE 1.3 from the painting) is dropped: it changed the figure, not the look.
  - Among the new woods it is ΔE 11.1 from the walnut and 17.8 from the persimmon; vanilla mangrove 13.8 (CIEDE2000 7.8)
    and dark oak 13.9.
- **Leaves:** `yew_needles`, the darkest and densest needles in the mod (about 1.5% gaps):
  #111a12 #182419 #1f3021 #2b3d2d #354c38 #425f45. Its one trait is a few red arils (#901a29 #b22e3e #d04859, each
  with a dark #3a1418 centre pixel), which ripen on random ticks as the chestnut's burs do. Evergreen.
- **Extras:** yew arils, picked when ripe. They are poisonous to eat, give red dye and serve as a witch's-cauldron
  floater (decor17 `FLOATERS`). Yew wood could later be an optional material for the Arms branch's longbows, never a
  required one.
- **Also in batch 3:**
  - **The leaf drawing options** of rule 3: blossom edge greens, under-gap colours and flower coverage; frond midribs; a
    per-entry gap threshold. The yew uses the threshold, and the wood batches that follow (4b, 4c, 8b, 9b, 10 and 11c)
    use the rest, so they need nothing from batch 4. They are tools-side and small.
  - **The Gloomweald fix.** Its base keeps vanilla's whole dark-forest vegetation, and its own 2-3 trees mostly land
    on that canopy, where their sapling checks fail. So the Gloomweald sets `base_trees` to
    `minecraft:dark_forest_vegetation` and gets its own list of about 16 a chunk instead: dark oak (v) 45, yew 20,
    dead tree 10, dead snag 10, huge red mushroom (v) 8, huge brown mushroom (v) 7.
  - Check the yew's frequency in the client survey.

### Batch 4: own leaves on borrowed logs (the shared change), with the snowpetal cherry and shimmer birch

The shared change comes first, with a short design note posted before building. Until now a tree's name has been its
wood's name. Under rule 9 most of the trees that borrowed logs now have woods of their own, so far fewer need that to
change. Those that still do keep their own name on a wood named otherwise, stand on a wood that is truly theirs, on a
hidden stem or on a trunk block of their own, or are stand-ins the owner may keep. They add only their own leaves and
sapling:
- `agriculture.TREES` gets a `wood` field (the log) and a `display` name;
- `trees.SHAPES` gets a `tree` field (whose leaves);
- Java's `registerTree` is split from `registerWoodSet`, so a tree on another wood's logs, or on a trunk block of its
  own, registers no phantom wood set;
- the switch, display names and tree-farm output key on the log wood;
- one-log shrubs give the tree farm 1-2 logs, not 6;
- `check_mod_data` refuses a wood-set block for any tree without its own `WOOD_SETS` entry;
- the naming rule under "How to read this".

The leaf drawing options of rule 3, once part of this batch, land in batch 3 with the yew.

**Who still needs it.** Twenty trees:

| Tree | Its logs | Batch |
|---|---|---|
| snowpetal_cherry | cherry (v) | 4 |
| shimmer_birch | birch (v) | 4 |
| rowan | fruitwood, its own new wood, under the tree name `rowan` | 5 |
| elder | a hidden oak log (v) | 5 |
| hawthorn | fruitwood (batch 5) | 5b |
| gorse | a hidden oak log (v) | 6 |
| sagebrush | a hidden dead log | 6 |
| stone_pine | pine, the tree `pine`'s (batch 2) | 8 |
| wattle | acacia (v) | 8 |
| joshua_tree | yucca, its own new wood, under the tree name `joshua_tree` | 8c |
| bramble_bush | a hidden crimson stem (v); leaves only | 9 |
| flame_tree | acacia (v), a stand-in | 11 |
| sea_grape | a hidden oak log (v) | 11 |
| osmanthus | olive (batch 7), a stand-in | 11 |
| banyan | fig, its own new wood, under the tree name `banyan` | 11b |
| banana, tree_fern | trunk blocks of their own, with no wood set | 12 |
| peach | cherry (v) | 14 |
| pear | fruitwood (batch 5) | 14 |
| orange | oak (v), a stand-in | 14 |

The trees whose key is their wood's name (the pine, yew, bald cypress, magnolia, persimmon, juniper, holly, olive,
tamarisk, dragonblood, baobab, mango and walnut) follow the pattern of every current tree and need none of the
tree-and-wood split. Nor does the bristlecone, a shape of the tree `pine` under its `pine_needles`. The cottonwood stays
on aspen, so its move to willow logs is gone. The root placer moves to batch 4b with the bald cypress, its first user.

**Where it stands.** Batch 4 still makes sense, and stays fourth. Its own two trees are high-value swaps on vanilla
logs, and batches 5, 5b, 6, 8, 8c, 9, 11, 11b, 12 and 14 each have a tree that needs it. It no longer carries a wood set
or the leaf drawing options, so it is the shared change and two leaf blocks. A wood batch waits for it only where the
tree keeps its own name on its wood (5, 8c and 11b).

#### snowpetal_cherry
Yoshino cherry (*Prunus × yedoensis*), the white-flowered cherry. **High.** White blossom over white petals in the
snow is the grove's whole picture, and vanilla's pink breaks it. It is the cheapest high-value swap on the roster.
- **Grows in:** Snowpetal Grove 75%, replacing vanilla cherry as its default. Its vanilla birch becomes the aspen
  (25%) in batch 1.
- **Shape:** exactly vanilla cherry's shape, with vanilla cherry logs.
- **Leaves:** `snowpetal_cherry_leaves`, blossom kind, among the densest (about 2% gaps).
  - Flowers: near-white clumps with a faint lilac-grey shadow, #8a7f94 #a79db0 #c2bbc8 #d6d1da, with single-pixel
    highlights of #e4e1e7.
  - Edge green: #2e5426 #3c6a2e.
  - Under the gaps: #2a3328, so fast graphics show dense foliage.
  - It sheds white petal particles, as vanilla's cherry sheds pink ones.
  - In bloom all year.
- **Wood:** reuses vanilla cherry logs, the same genus (*Prunus*, rule 9), with new leaves and a sapling, through
  this batch's `wood` field.

#### shimmer_birch
An invented aurora birch, shaped after the silver birch (*Betula pendula*). **High.** One leaf block gives three
wonder biomes a magic tree of their own in place of vanilla birch and oak.
- **Grows in:**
  - Frostlight Garden 40%, replacing vanilla birch.
  - Starlit Wood 45% as its tall form, replacing vanilla's super birch as the default. Vanilla's stays at 30% for its
    bees.
  - Glimmer Grove 20%, replacing vanilla oak.
- **Shape:**
  - shimmer_birch: straight trunk 5 (+2) under blob foliage (radius 2, height 3), vanilla birch's silhouette.
  - tall_shimmer_birch: straight trunk 10 (+4) under blob foliage (radius 2, height 5), after vanilla's super birch.
- **Bark and wood:** vanilla birch's, placed and not copied. It is drawn after the silver birch (*Betula*), so
  vanilla birch is its true wood (rule 9); the aspen (*Populus*) was a stand-in. Birch logs also keep the white
  trunks of the Frostlight Garden and the Starlit Wood, where it replaces vanilla birch and super birch.
- **Leaves:** `shimmer_birch_leaves`, a light mint green at hue 155: #1b4433 #265f47 #327c5d #419773 #52b58b #7ac2a4.
  - That is ΔE 16 from the eucalyptus.
  - Its one trait is a few pink-lilac aurora clusters (#7a5288 #a070b0 #c49ad0), set as the chestnut's burs are and
    pinker than the jacaranda's violet.
  - Evergreen. Leaves follow the world's season, but this look has no seasonal states.
- **Wood:** reuses vanilla birch logs, with new leaves and a sapling, through this batch's `wood` field. Painting 4
  (#7078b1) stays reserved for a magic wood.

### Batch 4b: the bald cypress

#### bald_cypress
Bald cypress (*Taxodium distichum*). **High.** The Bayou's name promises bald cypress, so this is the clearest stand-in
in the wetlands. One leaf set serves four swamps, and its russet autumn sits beside the willows' yellow.
- **Grows in:**
  - Bayou 60%, as its default; the willow stays at 25%.
  - Lush Swamp 25%; the existing maple also joins at 10%, as red swamp maple.
  - Swamp Woods 25%; the vanilla azalea also joins at 10%, as swamp azalea.
  - Webwood 15%, replacing vanilla birch and half the vine-hung oak (v).
- **Shape:**
  - Trunk: vanilla mangrove's `upwards_branching` trunk, base 9 (+3, +2), place_branch_per_log_probability 0.15,
    extra_branch_steps [1, 2], extra_branch_length [0, 1]. One tall, straight trunk 9-14 blocks with one or two short
    limbs.
  - Foot: vanilla's `mangrove_root_placer` with bald cypress logs as the roots (trunk_offset_y 1, max_root_width 2,
    max_root_length 3), so it stands in water on a flared foot.
  - Crown: a flat, airy random_spread crown (radius 3, offset 0, foliage_height 2, 50 attempts), with the existing
    shroud moss hanging beneath it (`attached_to_leaves`, about 0.12).
  - Fallback if the root placer is not ready: a straight trunk 10 (+4).
  - In the Webwood it stands on dry ground, which bald cypress also does.
- **Bark and wood:** its own wood, **bald cypress** (rule 9). Jugcraft's cypress is the Mediterranean *Cupressus
  sempervirens*; bald cypress (*Taxodium*) is a different genus in a different subfamily, close to the redwoods, and
  shares only the common name and the family. It is a major timber of its own, the "wood eternal" of docks, boats and
  siding.
  - Bark: stringy, in fibrous red-grey strips: #392d28 #4e3e37 #624e46 #7a6157 #927468. ΔE 4.3 from the mahogany's
    plated bark, which grows only in rainforests.
  - Wood: #9c9391, a muted ashen grey with a faint pink cast (hue 11, lightness 0.59, saturation 0.05), greyed as
    `NATURAL_TEXTURES.md` asks of swamp blocks. Its true light yellowish-brown would land on oak, jungle and the cedar.
    It is the dead wood's painting 23 (#7a7a7a) recoloured by ΔE 11.1, and ends 11.1 from the dead wood. On review it
    moved ΔE 1.8 from #a09490, off the baobab and the pine. ΔE 21.8 from vanilla cherry, the nearest vanilla wood; 11.3
    from the pine and 12.3 from the baobab; CIEDE2000 9.8 or more from every wood.
- **Leaves:** `bald_cypress_needles`, soft and open (about 5% gaps). All three looks come from one seed, on a schedule
  of about [98, 292, 330].

  | Look | Colours |
  |---|---|
  | green | #333d19 #485523 #5e702d #73883a #8da647 #a2b866. A greyed lime, lighter and yellower than any needles in the mod. |
  | autumn | #392014 #502d1b #683b24 #7f492f #9b5a39 #b56e4a. A russet cinnamon at hue 19 and saturation 0.46, about ΔE 24 or more from every maple look. |
  | winter | bare twigs in the bald cypress bark's colours |
- **Sapling:** a cross sprite in the needle tones on a bald-cypress-bark stem.
- **Wood:** NEW wood set "bald_cypress" (shown as "Bald Cypress"), with new needles and a sapling.
- **Extras:** shroud moss, taken with shears. Bald cypress becomes the swamps' building wood for stilt houses and
  docks.
- **Also in batch 4b:** `trunk_placer` passes vanilla's `upwards_branching` probability and tag fields through, and
  vanilla's `mangrove_root_placer` is supported. This moved here from batch 4: the bald cypress is its first user,
  and the pandanus, banyan and buttressed mahogany use it later.

### Batch 4c: the magnolia

#### magnolia
Southern magnolia (*Magnolia grandiflora*). **Medium.** It gives the flower island the showy flowering tree its name
promises, without repeating vanilla's pink cherry or the violet jacaranda.
- **Grows in:** Flower Isle 55%, as its default. The oak bush stays at 30%, and the apple tree joins at 15% in
  batch 2. The wild apple patch already reaches the isle through `c:is_floral`.
- **Shape:** straight trunk 5 (+2) under a dense, broad oval of blob foliage (radius 3, offset 0, height 4), bigger and
  darker than an oak.
- **Bark and wood:** its own wood, **magnolia** (rule 9): a magnolia (Magnoliaceae) is not a willow (*Salix*).
  Southern magnolia is a commercial hardwood for furniture and veneer, light to medium brown with a greenish cast.
  - Bark: smooth grey, in low, flat plates: #3b3a30 #515043 #686755 #7e7d67 #95947e.
  - Wood: #807d55, an olive-brown for the heartwood's greenish cast (hue 56, lightness 0.42, saturation 0.20), from
    the owner's London plane (2:1, #9b8059) altered by ΔE 11.3. ΔE 15.5 from vanilla oak, 18 degrees of hue away;
    17.0 from the chestnut; 10.5 from the mango and 13.0 from the yucca. The London plane moved 20 degrees of hue, so
    the owner judges this drab olive by eye beside its painting.
- **Leaves:** `magnolia_leaves`, a glossy dark green in slightly larger two-pixel clumps, with about 3% gaps:
  #122c12 #1a3d19 #225020 #2b612a #357733 #438c40.
  - Its one trait is a few big cream flowers (2-3 px clumps of #c9b578 #ddd2b2 #e8e1cc), each with a darker #b8962e
    centre.
  - Evergreen, and in flower all year.
- **Wood:** NEW wood set "magnolia", with new leaves and a sapling.

### Batch 5: fruit on seasonal leaves

The shared change, with a short design note posted first:
- a `SeasonalFruitLeavesBlock` with both a season state and a fruit stage, the fruit drawn on every look from one
  seed;
- the rare "lone tree" extra (a rarity filter, then the tree's survival check), first used in the Highland by the rowan.

Fruit ripens on random ticks whatever the season, and only on tree-grown leaves with air below.

The bare-twig ramps once planned here for trees on vanilla logs are dropped: the persimmon and walnut now have their own
barks, the hawthorn has the fruitwood's, and their twigs are drawn in them (rule 3). Batch 5 carries one new wood set,
the fruitwood, which the rowan brings under its own name, so the batch follows batch 4. The hawthorn (5b) and the pear
(14) grow on it, and the persimmon follows as batch 5c with a wood of its own.

#### rowan
Rowan, or mountain ash (*Sorbus aucuparia*). **High.** A cold-hardy tree whose red berries on bare twigs make snow
look better, in place of the frozen forest's temperate vanilla oak. The rowan is the old charm against witches, so it
also gives the mod's woods their first magic tie.
- **Grows in:**
  - Snowy Forest 36%, as its default, replacing vanilla oak. The aspen also joins at 20% in batch 1.
  - Lake District 10%.
  - A rare lone tree in the Highland.
  - The Snowy Forest has no seasons of its own, but leaves follow the server's season, so the rowans still go bare
    with berries in winter.
- **Shape:** forking trunk 4 (+2, +1) under an airy random_spread crown (radius 2, offset 0, foliage_height 3, 40
  attempts). A slim tree 6-8 blocks tall, often with two leaders.
- **Bark and wood:** a new wood, the **fruitwood** (rule 9), shared with the hawthorn (batch 5b) and the pear (batch
  14). A rowan (*Sorbus*) is not a maple (*Acer*). The rowan, the hawthorn (*Crataegus*) and the pear (*Pyrus*) are
  three genera of one tribe, the Maleae, whose pale pinkish-brown, fine, hard timbers serve alike for tool handles,
  turnery, carving and instruments; *Sorbus* timber is even sold as "Swiss pear". Drawn as three woods they read as one
  dusty-pink family that differs mainly in lightness, which a block's side shade cancels. So one fruitwood serves all
  three, which also saves 18 block IDs.
  - **Owner's call:** one fruitwood (this roster's default), or a rowan wood and a pear wood (#ba8688 and #9f7e73,
    worked out under Decisions). The hawthorn's own set is cut either way.
  - Bark: smooth silver-grey with dark lenticel marks, the marked kind, as the aspen's is: #2e2928 #655a58 #7b6d6b
    #90817f #a39695. A rowan's horizontal lenticels are birch-like, which is rule 5's case for the marked kind, and the
    bark is far darker than the aspen's (ΔE 25.7). The hawthorn and the pear show the same bark.
  - Wood: #ab7d76, a dusty pinkish brown (hue 8, lightness 0.57, saturation 0.24), the owner's painting 7 (#a97b74)
    moved ΔE 0.8, so all but as painted. The nearest vanilla wood is jungle, ΔE 15.9 and 17.9 degrees of hue away;
    cherry lies on nearly the same hue but ΔE 20.4 away and far lighter (0.78). 10.4 from the cypress, 12.0 from the
    baobab and 13.1 from the tamarisk (CIEDE2000 10.0, 10.3 and 9.8).
  - Its switch list is biomes (the rowan and the hawthorn), and gains agriculture with the pear in batch 14 (rule 4).
- **Wood:** NEW wood set "fruitwood" (shown as "Fruitwood"): log, wood, stripped log and wood, planks, stairs, slab,
  fence and fence gate. The rowan keeps its own name for its leaves, sapling and shape (`rowan_leaves`, `rowan_sapling`,
  `rowan`) on fruitwood logs, through batch 4's `wood` field. "Fruitwood" is the trade's word for these timbers; the
  name is the owner's to approve, since IDs are permanent.
- **Leaves:** `rowan_leaves`. One seed on a schedule of about [100, 255, 305].

  | Look | Colours | Berries |
  |---|---|---|
  | green | #384619 #4f6123 #67802e #7e9b3c #99ba4d #aec577. A lighter, feathery yellow-green at hue 78, ΔE 12.5 from the maple's green. | scarlet clusters of 3-5 px (#8a1a0a #c4300f #e8521c, lit #f47a3a), from late summer |
  | autumn | #4f3810 #6e4e17 #90661e #ae7d29 #d09836 #d5ad67. A muted apricot, ΔE 17 or more from each maple look, so the scarlet berries stand out. | berries |
  | winter | bare twigs in the fruitwood bark's colours (the rowan's) | the berry clusters stay |
- **Extras:** rowan berries, picked like chestnuts. With seasons off, they ripen on a timer, so nothing is gated.
  - A small bitter food.
  - Rowan jelly in a Mason Jar.
  - The existing `jugcraft:candy_flavours/berry` tag.
  - The Aura Candle's existing Warding scent, through `jugcraft:candle_scents/warding`.

#### elder
Elder (*Sambucus nigra*). The tree key is `elder` and the fruit is elderberries. **Medium.** The shrub-tree of damp
woodland edges and fen carr, with flowers and then dark berries.
- **Grows in:** Woodland 4%, in its new understory; Wetland 10%. The Elder Vale is not changed: its name means the
  old world, and its point is plain vanilla oaks.
- **Shape:** a broad, flat-topped shrub round one hidden stem: one log under bush foliage (radius 3, offset 1, height
  2), 3 blocks tall and up to 7 across, broader than the other bushes, with the log hidden in its widest, lowest row.
- **Wood:** none (rule 9: a shrub on a hidden stem). Elder is a multi-stemmed shrub with pithy stems, not a timber tree,
  and its yellowish-white, boxwood-like wood has no free colour: it would sit within ΔE 5-8 of the palm and about 10 of
  birch and the fir. Its hidden stem is one vanilla oak log, as the gorse's is, through batch 4's `wood` field. The
  short visible trunk of chestnut logs once planned is dropped: an elder is not a chestnut.
- **Leaves:** `elder_leaves`, on about the willow's schedule.

  | Look | Colours | Trait |
  |---|---|---|
  | green | #26351e #364a2a #466137 #577645 #6a9055 #82a66e. A soft mid green, ΔE 9 from the hawthorn and 17 from the willow beside it. | flat clusters that ripen on random ticks: cream flower umbels (#caba6a #e0d9b6), then purple-black berries (#2d1932 #39233f #482c4e) |
  | autumn | #3d3d1b #565626 #707032 #898940 #a7a74f #b7b770. A dull olive-yellow, ΔE 28 from the willow's gold. | |
  | winter | bare twigs in a corky grey-brown of its own (#3d3832 #554e46 #6e655b), since its stem is hidden | |
- **Extras:** elderberries, picked when ripe.
  - A snack.
  - Purple dye.
  - The existing `jugcraft:candy_flavours/berry` tag.
  - A witch's-cauldron floater (decor17 `FLOATERS`).

### Batch 5b: the hawthorn

#### hawthorn
Common hawthorn (*Crataegus monogyna*). **High.** The real hedgerow and pasture shrub, with red haws in autumn, in
place of the vanilla oak bush the meadows default to.
- **Grows in:**
  - hawthorn_bush: Field 55%, as its default; Shrubland 60%, as its default; Woodland 4%, in place of the oak bush.
  - hawthorn: Field 10%; a rare lone tree in the Highland, beside the rowan's (batch 5).
  - The oak bush is cut to 10% or less, or removed, wherever the hawthorn bush grows.
- **Shape:**
  - hawthorn: bending trunk 3 (+1), bend_length 1, `min_height_for_leaves` 3, under a flat, dense blob (radius 2,
    offset 0, height 2). A lopsided, wind-bent thorn 4-5 blocks tall.
  - hawthorn_bush: one log under bush foliage (radius 2, offset 1, height 2).
- **Bark and wood:** the fruitwood from batch 5 (rule 9), shared with the rowan and the pear. A hawthorn (*Crataegus*)
  is not an oak: it is a real tree up to 10-15 m, whose very hard, fine, pale timber makes tool handles, mallets and
  turnery, the Maleae fruitwood its relative the rowan brings.
  - It shows the fruitwood's silver-grey marked bark and pinkish wood #ab7d76. hawthorn_bush's hidden stem is a
    fruitwood log too, so the Field and the Shrubland yield fruitwood.
  - Its own set, at #c2b2ad (the aspen's painting 9 recoloured), was cut on review. It passed rule 3's hue reading only
    on a saturation of 0.147 against the 0.15 cut-off, beside both cherry and pale oak, and no free slot lay near it.
    Its shaded side matched the bald cypress (CIEDE2000 2.0), and its bark was generic. Its fallback is its Maleae
    relative's fruitwood, not vanilla oak.
- **Wood:** no wood set. Its own leaves, sapling and shapes (`hawthorn_leaves`, `hawthorn_sapling`, `hawthorn`,
  `hawthorn_bush`) grow on fruitwood logs through batch 4's `wood` field.
- **Leaves:** `hawthorn_leaves`, in small, dense clumps (about 3% gaps). One seed on a schedule of about
  [85, 280, 325].

  | Look | Colours | Haws |
  |---|---|---|
  | green | #16301a #1f4022 #2a522c #366638 #447a44 #5c925a. A dark, faintly blue green at hue 122, ΔE 19 from the apple's leaves and 14 from the chestnut's. | a few unripe haws (#6a7a2a), so it reads in summer |
  | autumn | #3c2e1a #544124 #6e552f #86683c #a48049 #b69768. A dull ochre-brown, ΔE 30 or more from every maple look. | three or four 2-pixel clusters of deep red (#8e1c18 #b8302a #d04a36), lit on top, standing out against the ochre |
  | winter | bare twigs in the fruitwood bark's colours | a few haws hanging on |
- **Extras:** haws drop from the autumn and bare leaves, and at a low rate from green ones, so `seasons.mode=off`
  still yields them.
  - Haw Jelly: a preserve cooked into a Mason Jar in the Cooking Pot, beside Sweet Berry Jam.
  - Fox food (`#minecraft:fox_food`).
  - Compost.

  Haws are not a progression item. Players can plant hawthorn as hedges.
- **Note:** our leaves are coloured in their texture, so replacing tinted oak bushes drops the biomes' foliage tint,
  such as the Field's #6fa880, from those bushes.

### Batch 5c: the persimmon

#### persimmon
American persimmon (*Diospyros virginiana*). **High.** It fulfils the biome roster's written promise of "persimmons"
in the Seasonal Forest. Its orange fruit stays on the bare twigs until it is picked, a late-autumn sight it shares only
with the rowan and hawthorn.
- **Grows in:** Seasonal Forest 10%, from the vanilla oak's share. In batch 1 the fir also joins the Seasonal Forest
  at 4%, as a plain pick, for the roster's "firs in the hills", since vanilla has no absolute-height filter.
- **Shape:** straight trunk 5 (+3) under a tall oval blob (radius 2, offset 0, height 4): a slim, upright fruit tree,
  narrower and taller than an oak.
- **Bark and wood:** its own wood, **persimmon** (rule 9). *Diospyros* is the ebony genus, not an oak. The planks take
  the genus's dark heartwood look; American persimmon's own pale sapwood was the classic timber of golf-club "woods" and
  textile shuttles.
  - Bark: a cool slate-black in small square blocks (plated): #141416 #1b1c1f #232528 #2c2e32 #36383c. Its lightest tone
    is no lighter than the wood, so the bark is the separate, darker ramp rule 2 asks for, and its mid tone is ΔE 10.8
    from the wood. The revision's warm charcoal bark was 5.1 from it, so stripping a log barely showed.
  - Wood: #3d3733, a warm charcoal (hue 24, lightness 0.22, saturation 0.09), the owner's black painting 18 (#383838)
    warmed by ΔE 3.9: the set's only neutral charcoal. ΔE 17.4 from vanilla dark oak, 5.4 degrees of hue away, so it
    passes rule 3's reading on ΔE; 11.1 from the walnut (CIEDE2000 8.7) and 17.8 from the yew.
- **Wood:** NEW wood set "persimmon", with new leaves and a sapling.
- **Leaves:** `persimmon_leaves`. One seed on a schedule of about [110, 280, 315].

  | Look | Colours | Fruit |
  |---|---|---|
  | green | #133016 #1b431e #245828 #2e6b33 #38823e #47994e. A glossy deep green, a little bluer and darker than oak, leafing out late. | none |
  | autumn | #3a1018 #561a22 #74262c #8e3634 #a84a3e #c06450. A wine-maroon, ΔE 15 from the maple's red and 35 or more from its orange and gold. | orange persimmons as lit 2×2 clusters (#b8561a #de7a2a #f0a050), 4-6 to a face, clear against the maroon |
  | winter | bare twigs in the persimmon bark's colours | the fruit stays until picked |
- **Extras:** persimmons, picked by right-click; the tree is never cut. With seasons off, they ripen on a timer, as
  apples do.
  - Food.
  - A persimmon pie filling for the Hearth Oven, appended to the end of `PIES["fillings"]` and the `PieFilling` enum
    so existing pies keep their filling.
  - A persimmon preserve in a Mason Jar.
  - `#jugcraft:fermentable`.

### Batch 6: evergreen scrub

#### juniper
The junipers: common juniper (*Juniperus communis*), Utah juniper (*J. osteosperma*) and eastern red cedar
(*J. virginiana*). **High.** One leaf block and three shapes reach 13 biomes, more than any other tree on the roster.
It turns the canyon and dryland into pinyon-juniper country, and replaces the lush oak in the withered Dead Forest
with a drought-hardy survivor.
- **Grows in:**

  | Biome | Share |
  |---|---|
  | Canyon | juniper 35%, juniper_bush 25% |
  | Dryland | juniper 30%, juniper_bush 25% |
  | Xeric Shrubland | juniper 25% |
  | Dead Forest | juniper 10%, replacing vanilla oak |
  | Scrubland | juniper 15% |
  | Cold Desert | a rare lone juniper on its coarse-dirt patches, about one chunk in six |
  | Field | juniper_column 30%, replacing vanilla spruce as the record's "small spruces" |
  | Prairie | juniper_column 15%, as eastern red cedar |
  | Heathland | juniper_bush 15% |
  | Shrubland | juniper_bush 10% |
  | Steppe | juniper_bush 20% |
  | Tundra | juniper_bush 10% |
  | Alpine Spawn | juniper_bush 10%, in batch 8 with Alpine Spawn's picks list |
- **Shape:** all three forms survive as a dead bush would, so they stand on sand, red sand, coarse dirt and
  terracotta as well as grass.
  - juniper: forking trunk 2 (+2, +1) under random_spread foliage (radius 2, offset 0, foliage_height 3, 40
    attempts). Short, crooked and ragged, 4-6 blocks.
  - juniper_column: straight trunk 4 (+2) under spruce foliage (radius [1, 2], offset [0, 1], trunk_height [0, 1]). A
    shaggy flame, leafy from the ground up, 5-7 blocks.
  - juniper_bush: one log under bush foliage (radius 2, offset 0, height 1). A low mat 5 wide and 1-2 high.
- **Bark and wood:** its own wood, **juniper** (rule 9). The junipers (*Juniperus*) are one of about 30 genera of the
  cypress family, Cupressaceae, which also holds Jugcraft's *Cupressus* and this roster's cedar, bald cypress and
  redwood; a juniper is none of those. Juniper is a well-known timber: eastern red cedar's aromatic red-violet heartwood
  lines cedar chests and closets, and makes fence posts and pencils.
  - Bark: stringy, peeling in long red-grey strips: #372625 #4c3533 #624341 #7a5452 #936562. ΔE 3.8 from the
    tamarisk's furrowed bark; the two never share a biome.
  - Wood: #8b4654, a red-violet (hue 348, lightness 0.41, saturation 0.33), from the owner's painting 20 (#86434e)
    altered by ΔE 2.4. ΔE 14.0 from vanilla mangrove and 14.2 from crimson, 16.5 and 12.7 degrees of hue away; 17.4
    from the jacaranda, 4.7 degrees of hue away but twice as saturated; 17.2 from the tamarisk.
- **Leaves:** `juniper_needles`, among the densest. A dusty grey-blue-green at hue 187 and saturation 0.17:
  #1e2a2e #27373b #324548 #3f5558 #4f676a #688487.
  - That is greyer and bluer than the fir, much lighter than the cypress and well clear of the eucalyptus.
  - Its one trait is frosted indigo berries as single and paired pixels (#2e3a66 #3a4a6e), each with a pale #9aaac4
    bloom pixel.
  - Evergreen.
- **Wood:** NEW wood set "juniper", with new needles and a sapling, used by all three shapes. Its sapling takes sand.
  Its switch list is biomes, and gains alpine_spawn in batch 8, when the juniper bush joins Alpine Spawn (rule 4).
- **Extras:** juniper berries, picked like chestnuts or dropped from broken needles as apples drop from oak leaves.
  They are never required.
  - An alternative Mulling Spices recipe, with juniper berries in place of one ingredient.
  - Fox food.
  - Compost.

#### gorse
Common gorse, or furze (*Ulex europaeus*). **High.** The heath's signature shrub: yellow drifts over brown grass
beside the magenta heather the biome already has.
- **Grows in:** Heathland 60%, as its default; Shrubland 20%.
- **Shape:** tight, lumpy clumps round one hidden oak log: bush foliage (radius 2, offset 0, height 2), 3-5 wide and 2
  high.
- **Leaves:** `gorse_leaves`, blossom kind, evergreen and always in flower.
  - A dark spiny green field: #14200e #1c2c14 #253a1a #2e4721 #385629 #446633.
  - Lemon-yellow flower clumps on about 35% of the face: #927e1c #b09927 #d2b834, highlight #d7c465. Hue 50,
    saturation capped at 0.65, so the drifts do not read as yellow wool.
  - The wattle is egg-yolk gold at hue 42, so the two yellow shrubs differ.
- **Wood:** none (rule 9: a spiny legume shrub with no timber). One vanilla oak log hidden inside is the stem, as in
  the oak bush, through batch 4's `wood` field. It has its own leaves and a sapling, so players can plant gorse hedges.
- **Extras:** its leaves join `#minecraft:flowers`, so bees work them, as vanilla's flowering azalea leaves do.

#### sagebrush
Big sagebrush (*Artemisia tridentata*). **Medium.** It turns the bare steppe into a believable shrub-steppe while
keeping it open and low.
- **Grows in:** Steppe, which gets a trees slot of 1-2 a chunk: sagebrush 80%, juniper_bush 20%, nothing over two
  blocks. The wild apple patch already reaches the Steppe through `c:is_plains`. Remove the Steppe from that patch if
  it must stay low, or accept the apples.
- **Shape:** one dead-wood log as the stem, grey like sage's shreddy stems, under bush foliage (radius 1, offset 0,
  height 2). 3 wide and 1-2 tall, smaller than every other bush in the mod.
- **Leaves:** `sagebrush_leaves`, in fine one-pixel clumps of silvery sage: #3e463a #535c4e #697262 #80897a #9aa392
  #b4bca8. Evergreen. It never shares a biome with the olive bush, its near-twin in colour.
- **Wood:** none new (rule 9: a small aromatic shrub with no timber). Its sapling grows one dead-wood log, through
  batch 4's `wood` field, which makes dead wood renewable in the Overworld.
- **Extras:** compost. It could later become an Aura Candle scent.

### Batch 6b: the holly

Its berries use batch 5's season state, so it follows batch 5.

#### holly
European holly (*Ilex aquifolium*). **Medium.** It keeps the oak woods green under winter snow and ties world
generation to the planned December content.
- **Grows in:** Woodland 4%; Dense Forest 5%, from the oak bush's share.
- **Shape:** straight trunk 3 (+2) under blob foliage (radius 2, offset 0, height 4). An upright broadleaf oval 5-7
  blocks tall. It is not a spruce-placer cone, which would read as a small fir.
- **Bark and wood:** its own wood, **holly** (rule 9). Holly (*Ilex*) is not an oak; vanilla pale oak was a
  look-alike. Its timber is famous: the whitest wood, for inlay, chess pieces, turnery and engraving.
  - Bark: smooth grey with a faint green cast, in low, flat plates: #5e655a #717a6c #858e7f #99a094 #acb2a9 (plated, at
    a dark-to-light lightness ratio of 0.55, where most barks are about 0.35, so it reads smooth). Holly bark is smooth,
    not birch-like, so it does not take the marked kind, which rule 5 keeps for birch-like trees and which is the
    aspen's trait. Its nearest barks, the fig's and the olive's (ΔE 8.7 and 8.9), grow in none of its biomes.
  - Wood: #b6baa1, a grey-green ivory (hue 70, lightness 0.68, saturation 0.15), for the faint greenish-grey cast of
    holly's ivory. It is the palm's painting 10 (#e5d5b2) recoloured by ΔE 14.5 and 28 degrees of hue, so its source is
    nominal and the owner judges it by eye; it ends 14.5 from the palm. ΔE 18.4 from vanilla birch and 18.9 from pale
    oak; 15.2 from the eucalyptus; 12.2 from the pine.
- **Wood:** NEW wood set "holly", with new leaves and a sapling.
- **Leaves:** `holly_leaves`, dense and glossy: #17340f #204915 #29601c #347425 #408e2d #50a63b.
  - The hue is 109, a little yellower than the mahogany, with more light specks than other leaves.
  - Its one trait is small scarlet berry clusters (#8a1414 #c21f1f #e04a3a).
  - It is evergreen, but uses the season state for its berries, on a schedule of about [100, 300, 335]. The gold and
    bare states are the same leaves with berries.
  - The green state shows berries on about one block in three, picked by position, so a server with seasons off
    still sees them.
- **Extras:** holly sprigs, taken with shears. Holly grows all year, so nothing depends on the event window.
  - They make the **Holly Wreath**, a holly variant of the existing Autumn Wreath block, in the same PR, so the sprigs
    have a consumer from the start.
  - They are decoration only and never gate anything.

### Batch 7: the olive

#### olive
Olive (*Olea europaea*), wild and cultivated. **High.** The defining tree of the Mediterranean. It fills the biggest
stand-in gap in the warm, dry lands, where only the cypress reads as Mediterranean. It has its own wood and leaves,
the existing pattern, and so needs no shared change.
- **Grows in:**
  - Mediterranean Forest: olive 25% and olive_bush 15%, replacing vanilla dark oak, part of the vanilla oak, and the
    oak bush.
  - Lavender Field: olive_bees 30%, replacing most of vanilla's oak with bees, which keeps 10%. The cypress also
    joins at 10% in batch 2, and the biome's `winter_snow` goes off (Provence).
  - Scrubland: olive_bush 60%, as its default.
  - It is not placed in the snowy Orchard or the Mojave-like Xeric Shrubland.
- **Shape:**
  - olive: forking trunk 3 (+1, +2) under a wide, ragged, cloud-like random_spread crown (radius 3, offset 0,
    foliage_height 2, 45 attempts). A short, thick trunk that leans and splits low; 5-7 blocks, wider than tall.
  - olive_bees: the same, with vanilla's beehive decorator (0.05).
  - ancient_olive, which four saplings in a square grow: a squat giant trunk 3 (+1, +1) under random_spread (radius
    4, foliage_height 2, 90 attempts). It is not vanilla dark oak's placers, which would grow back the silhouette the
    olive replaces.
  - olive_bush: one log under bush foliage (radius 2, offset 1, height 2).
- **Bark:** silver-grey with a warm green cast: #474a3b #616451 #7b7f66 #93987e #aaad99 (hue 70, saturation 0.11).
  It is furrowed, with the standard one-pixel wander. That is ΔE 15 from the dead wood's bark and 10 from the palm's.
- **Wood:** the owner's first-set painting 3 (#ada358; painting 19 is the same colour, so only one is used), nudged
  darker and greener to **#9c9951** (ΔE 5.4). As painted it was only ΔE 10.6 from vanilla bamboo, 4 degrees of hue
  apart, as the check below foresaw.
  - A darker, greener yellow at hue 58 and lightness 0.47 than the willow (#c7c785) or the chestnut.
  - Its one trait is a figure of 2-3 straight, broken streaks drawn in the wood ramp's own two darkest tones,
    hue-shifted no more than 10 degrees toward brown, with a one-pixel jog.
  - **Checked by script beside vanilla's bamboo** (about hue 49): ΔE 15.3, 8.8 degrees of hue apart. Birch is ΔE
    14.7 at 12 degrees; the fir 16.2 and the chestnut 17.4. Still shoot it beside bamboo planks and stripped bamboo,
    crimson and warped planks, birch, oak, the willow and the chestnut, and record the distances.
  - If it still reads like bamboo in game, darken it further or move its hue toward 60-62, with the owner's approval.
    The old fallback, willow logs, is dropped: an olive is not a willow (rule 9).
  - This survey also proposes adding bamboo, crimson and warped to rule 3's list in `NATURAL_TEXTURES.md`.
  - The preview sheet shows the olive plain; it is previewed again once its figure is drawn.
- **Leaves:** `olive_leaves`, narrow and plainly silver: #424a37 #576249 #6d7b5b #83926f #9ba88b #b4bca9.
  - Middle tones at lightness 0.42-0.57 and saturation 0.14, the lightest tone silvery for the leaf undersides.
  - Its one trait is olive clusters drawn as the chestnut's burs are: green (#6a7a2a #8a9a3a), then ripe purple-black
    (#3a2a3a #5a3f55).
  - Evergreen.
- **Wood set:** NEW wood set "olive": log, wood, stripped log and wood, planks, stairs, slab, fence and fence gate.
  The osmanthus (batch 11) stands on it as a stand-in: the same tribe, not the same genus (an owner's call).
- **Extras:** olives, picked by right-click from ripe leaves; the tree is never cut.
  - A small food.
  - Pickled olives in a Mason Jar, with cider vinegar, as the pickled beets are.
  - When agriculture slice 5 adds the shared plant oil, olives press into that same fluid, not an olive-only oil.
  - olive_bees keeps the Lavender Field's bees.
  - Wild olive bushes bear no fruit, which keeps the harvest on the tree.

### Batch 8: dry country

The stone pine and the wattle, with Alpine Spawn's picks list. The Joshua tree and its yucca wood follow as batch 8c.

#### stone_pine
The nut pines: stone or umbrella pine (*Pinus pinea*), Swiss stone pine (*P. cembra*) and pinyon (*P. edulis*).
**High.** It merges four pine proposals into one needle block. It replaces vanilla's taiga pine in the dry and heath
biomes and vanilla oak in the Mediterranean, where flat parasols rise above dark cypress columns. It also gives the
start biome a second tree of its own, and a food.
- **Grows in:**
  - umbrella_pine: Mediterranean Forest 15%, replacing most of the vanilla oak; Heathland 25%, replacing vanilla pine
    as the flat-crowned Scots pine.
  - stone_pine: Alpine Spawn 20%. Its table becomes larch 50, stone pine 20, spruce (v) 20, juniper_bush 10, and
    `alpine.TREES` becomes a picks list, with `alpine_data.worldgen` changed to match. The juniper bush joins Alpine
    Spawn here, not in batch 6, since it needs the picks list, and the juniper's switch list gains alpine_spawn with the
    pine's (rule 4).
  - pinyon: Dryland 45%, as its default; Canyon 40%, as its default, both replacing vanilla pine.
  - It is not placed on the all-sand Dune Beach, which keeps its "no trees".
- **Shape:**
  - umbrella_pine: forking trunk 7 (+2, +1) under acacia foliage (radius 3, offset 0). A tall, clean trunk forking at
    the top into a wide, flat parasol 9-11 blocks tall and 7-9 across.
  - stone_pine: straight trunk 4 (+4) under a blob (radius 2, offset 0, height 4) that comes low down the trunk. A
    dense, round-crowned nut pine 6-10 blocks tall, the Alps' Zirbe.
  - pinyon: straight trunk 3 (+2) under a blob (radius 2, offset 0, height 3), 4-6 blocks. It survives as a dead
    bush would, so it stands on canyon terracotta.
- **Bark and wood:** the pine wood from batch 2 (rule 9): a pine is not a larch, and the pines share one wood. Pine's
  orange-brown plated bark (#2d2521 #43312a #5f4433 #805a3d #9c6c48) and grey-tan wood #a49a84. The pine's switch list
  gains alpine_spawn (rule 4). Larch's keeps alpine_spawn and biomes, for the larch and the tamarack.
- **Leaves:** `stone_pine_needles`, dense, evergreen, one look all year: #232f20 #31422d #40573b #4f6a4a #61825a
  #779970.
  - A dusty grey-green at hue 111 and saturation 0.18: ΔE 12 from the cypress beside it in the Mediterranean, and 17
    from the olive.
  - Its one trait is small 2-pixel cones lit on top (#5a3a20 #7a5230 #9a6e44).
  - The larch's own needles turn gold and fall, so they cannot be reused. The windswept pine's `pine_needles` (batch 2)
    are a softer blue-green with no cones, ΔE 12.5 away, so the nut pines keep needles of their own.
- **Wood:** reuses pine logs (the tree `pine`'s, batch 2), with its own needles and sapling, through batch 4's `wood`
  field.
- **Extras:** pine nuts from ripe cones, picked by right-click like chestnuts. They give an early food at Alpine
  Spawn, where every player starts, and ripening is not seasonal.
  - They roast at a campfire into roasted pine nuts.
  - They join `#jugcraft:squirrel_food`.

#### wattle
Golden wattle (*Acacia pycnantha*). **High.** It replaces vanilla-oak scrub with the real Australian understory, and
makes the Gilded Shrubland's gold its own instead of a tint on vanilla oak leaves.
- **Grows in:**
  - Eucalyptus Forest: wattle_bush 15%, replacing the oak bush.
  - Gilded Shrubland: wattle_bush 70% and wattle 30%. The oak bush is removed there; it is the same shape in the
    biome's same gold tint, and the two could not be told apart.
- **Shape:**
  - wattle: forking trunk 2 (+1, +1) under a blob (radius 2, offset 0, height 3). A low, rounded tree 4-6 blocks
    tall.
  - wattle_bush: one log under bush foliage (radius 2, offset 1, height 2).
- **Wood:** vanilla acacia logs, the wattle's own genus (*Acacia*, rule 9), through batch 4's `wood` field.
- **Leaves:** `wattle_leaves`, blossom kind, dense. In bloom all year.
  - Egg-yolk golden puffs: #997520 #b98f2b #d3a844, highlight #dabc77; hue 42.
  - Grey-green phyllodes (#546142 #6e7c5a) show on at least 35% of the face, so it reads as blossom over leaves.
- **Extras:** none needed. The blossom could give yellow dye if the owner wants a use.

### Batch 8b: the tamarisk

#### tamarisk
Tamarisk, or salt cedar (*Tamarix*). **Low**, and a candidate to cut if sets are trimmed, after the fig and the mango.
The one living tree that belongs on a salt flat, and the archetypal desert-river tree.
- **Grows in:**
  - Wasteland 15%, in the 1 try a chunk that batch 1 already gave it.
  - Dry River 40%, beside the date palm (batch 10). Whichever of batches 8b and 10 lands first gives the Dry River 1 try
    a chunk on its dry banks.
  - Oasis 10%.
- **Shape:** forking trunk 2 (+1, +1) under random_spread foliage (radius 2, offset 0, foliage_height 3, 40 attempts).
  A low, many-stemmed, feathery tree 4-6 blocks tall, loose rather than vanilla cherry's compact ball. It survives as
  a dead bush would, which keeps it to coarse dirt in the Wasteland and to sand elsewhere.
- **Bark and wood:** its own wood, **tamarisk** (rule 9): *Tamarix* is not a jacaranda (Bignoniaceae), whose mauve wood
  was only near its colour. It has a visible forking trunk, and its pinkish to light reddish-brown wood serves for fuel,
  tool handles and small timber. It is not the only wood in its biomes: the dead tree and the dead snag make up most of
  the Wasteland, and the date palm most of the Dry River.
  - Bark: dark red-brown, furrowed: #312022 #483032 #5e4043 #745153 #896165. ΔE 3.8 from the juniper's stringy bark;
    the two never share a biome.
  - Wood: #a06f7e, a dusty rose (hue 342, lightness 0.53, saturation 0.21), the jacaranda's painting (2:6, #7a5a5e)
    recoloured by ΔE 13.6; it ends 13.6 from the jacaranda. ΔE 25.2 from vanilla crimson, 6.5 degrees of hue away, so it
    passes rule 3's reading on ΔE; 13.1 from the fruitwood (CIEDE2000 9.8).
- **Leaves:** `tamarisk_leaves`, blossom kind, mostly leaf. Evergreen.
  - A feathery grey-green field: #222d24 #303f32 #3f5342 #4e6652 #607c64 #76937a.
  - Sparse, dusty mauve-pink plumes (#7e5a6a #a07888 #c09aa8) on no more than 25% of the face, so it never reads as a
    misplaced cherry.
- **Wood:** NEW wood set "tamarisk", with new leaves and a sapling.

### Batch 8c: the Joshua tree, and the yucca wood

#### joshua_tree
Joshua tree (*Yucca brevifolia*). **High.** The desert tree people picture, on the red dunes of the Lush Desert.
- **Grows in:** Lush Desert 35%, beside the desert acacia and the small palm from batch 2; Xeric Shrubland 30%, which
  becomes Mojave and Great Basin shrubland with juniper and acacia.
- **Shape:** vanilla's cherry trunk placer, with blob foliage (radius 1, offset 0, height 2) at each arm's tip. A short
  trunk throwing two or three upturned arms, each ending in a spiky rosette, 4-6 blocks.
  - base 3 (+1), branch_count [2, 3];
  - branch_horizontal_length 2: vanilla's codec does not allow less than 2, and `check_mod_data` gains a bounds check
    for every cherry-placer tree;
  - branch_start_offset_from_top [-2, -1], branch_end_offset_from_top [0, 1].

  It survives as a dead bush would, and its sapling takes sand, as the palm's does.
- **Bark and wood:** its own wood, **yucca**, named for the genus (rule 9): a yucca (Asparagaceae) is not a palm
  (Arecaceae). Its trunk is truly woody and thickens as it grows; settlers cut it for fence posts and corrals, and in
  the Lush Desert and the Xeric Shrubland it is the wood a player would chop.
  - Bark: stringy, a pale straw-grey for the shag of old dead leaves that covers its arms: #5f5a42 #7c7556 #98906b
    #aca688 #c0bca5. It is lighter than its planks, as the aspen's is, and its mid tone is ΔE 18.6 from the wood, so a
    stripped log shows the drab wood under the shag (the revision's shag was 5.1 from it). It is ΔE 15.4 from the palm's
    bark beside it in the Lush Desert (it was 4.0).
  - Wood: #68654e, a dusky khaki-grey (hue 53, lightness 0.36, saturation 0.14), from the owner's wenge (2:3, #544233)
    altered by ΔE 15.5, the most altered source: 26 degrees of hue, so the source is nominal (the wood now sits nearer
    the black walnut, 2:2, at 10.1), and the owner judges it by eye. ΔE 16.5 from vanilla spruce; 13.1 from the cedar
    and the walnut; 10.1 from the fig and 13.0 from the magnolia.
- **Leaves:** `joshua_tree_fronds`, fronds kind, drawn as short, steep 4-5 pixel spikes, among the most open:
  #33371f #484d2b #5e6439 #747a48 #8d9558 #a2aa74.
  - A fresher yucca green at hue 67 and saturation 0.26.
  - It is ΔE 31 from the juniper beside it.
  - Evergreen.
- **Wood:** NEW wood set "yucca". The tree keeps the key `joshua_tree` (`joshua_tree_fronds`, `joshua_tree_sapling`)
  on yucca logs through batch 4's `wood` field, so a felled Joshua tree gives Yucca Logs, and a later yucca of another
  species would share the wood.

### Batch 9: giant fungi and the Nether

Settle these together:
- `count_on_every_layer` placement under the Nether roof;
- one `huge_fungus` feature for each floor block;
- the no-gain-loop arithmetic for bone meal and composting.

#### huge_glowcap
An invented giant of the mod's own glowcap, after vanilla's huge mushrooms. **High.** With one block, five biomes
across the Overworld, the Nether and the End get a giant of the mod's own fungus. Its cold light reads well against
the Nether's red haze.
- **Grows in:**

  | Biome | How it is placed |
  |---|---|
  | Fungal Thicket | about 2 a layer on nylium and mycelium, among the crimson fungi, which stay as the canopy |
  | Glowcap Grotto | 2-4 tries a chunk as a **cave extra**, with the same `environment_scan` placement as `grotto_glowcaps`; the surface trees field could never place in a cave biome. It fails where there is less than about 10 blocks of headroom. |
  | Toadstool Field | about 1 a chunk on mycelium, beside vanilla's huge mushrooms, which stay |
  | Mycelial Jungle | 7%, from the jungle bush's share |
  | Ender Wilds | 5%, on moss |
- **Shape:** vanilla's huge mushroom features in two forms, a domed one (`huge_red_mushroom`, cap radius 2-3 on a stem
  5-9 tall) and a parasol (`huge_brown_mushroom`, radius 3). The cap is a new `glowcap_block`; the stem is vanilla's
  mushroom stem, placed as it is.
- **Cap:** a huge-mushroom block with six face states, opaque, with no outlines, tiling seamlessly.
  - Its outer face is drawn in vanilla's mushroom-block manner, as soft 2-4 pixel blotches in three tones, not the
    leaves' speckle: #1d5a6e and #2a8aa0 for the field, #3fbfd0 for small spots.
  - About ten #79e6ee and #c4fbff glints.
  - Its inner face shows pale grey-green pores (#8a9a8c #b0bcb0 #d6e2d2).
  - Light 8, below the plant's 10, so a cap reads as a soft lamp.
- **Extras:**
  - Cap blocks drop a glowcap at about 15% each, which is about 7 per giant of about 45 cap blocks.
  - `glowcap_block` itself does not compost. The glowcap keeps its existing compost value.
  - Arithmetic: 7 glowcaps at a 0.65 compost chance make about 0.65 bone meal, from 1 bone meal and 1 glowcap spent,
    so bone meal never comes back with a gain. This goes into the record.
  - Glowcaps become renewable light.
  - Silk touch gives the cap as a soft cyan light block for builders.
  - A glowcap grows into a giant with bone meal, in low light, on mycelium, moss, mud or nylium.

#### blight_fungus
An invented dead, blighted huge fungus, after the dead trees standing in Deadvlei's dune pan. **High.** The Nether has
no Jugcraft growth at all. This gives the Withered Hollow and the Blighted Sands the dead trees their names refer to,
in vanilla's own Nether-tree form, and links the End's rot to the Nether through the soul soil both share.
- **Grows in:**
  - Blighted Sands: about 1 a layer on soul soil. It is the commonest biome in this group, at 337 of 16,641 Nether
    samples.
  - Withered Hollow: about 3 a layer on blackstone.
  - Rotted Expanse: its own extra on the soul-soil patches. It is not a trees pick, which would fail on the end stone
    five times in six and remove dead trees for nothing.
- **Shape:** vanilla's `huge_fungus` feature, one configured feature per floor because the feature takes one
  `valid_base_block` (soul soil or blackstone).
  - A straight stem 4-13 blocks tall, now and then twice that, under a rounded cap that hangs 2-3 blocks down round
    the stem, with ragged gaps.
  - `decor_state` is the wart itself, so there are no shroomlights and the Hollow stays dark.
- **Stem and cap:**
  - Stem: dead wood's existing grey log. A fungus needs no wood set (rule 9), and the stalk is meant to be the same
    dead material.
  - Cap: a new `blight_wart_block`, opaque like vanilla's wart blocks, drawn as mottled blotches in four tones of dry
    charcoal-mauve (#1e1a1e #2c262c #3c343a #4e444c), with about ten pale ash specks (#9a9296).
- **Extras:**
  - Blight wart blocks drop a small blight fungus at about 5%, so the fungus reproduces. That is about 2-3 per huge
    fungus, roughly 0.25 bone meal's worth of compost, below the one bone meal that grew it.
  - The small fungus grows wild on soul soil, and bone meal there grows a huge one, so dead wood is renewable from
    the first Nether trip. The sagebrush already makes it renewable in the Overworld.
  - Wart blocks do not compost. Nothing requires them. A Cursing or Necromancy school could use them later.

#### marrow_fungus
An invented huge fungus with a bone stalk and a nether-wart cap. **Medium.** It turns the heap's own two floor
materials into its growth, a silhouette no other biome has, for the cost of one configured feature.
- **Grows in:** Marrow Heap, about 1 a layer on its nether wart block patches. The bone spires stay as its smaller
  verticals.
- **Shape:** vanilla's `huge_fungus`, built only of vanilla blocks placed by reference: stem `minecraft:bone_block`
  (axis y), hat `minecraft:nether_wart_block`, decor `minecraft:shroomlight`, base `minecraft:nether_wart_block`. A
  pale bone stalk 4-13 blocks tall under a domed red wart cap with a few shroomlights.
- **Extras:** none. It stays worldgen-only, with no sapling and no bone-meal growth, because a planted one would turn
  one bone meal into a stalk of bone blocks.

#### bramble_bush
An invented thicket of the mod's existing Nether bramble. **High.** It fills the empty head-height layer between the
one-block brambles and the tall warped fungi, which makes the "brush" in Netherbrush real.
- **Grows in:** Netherbrush, about 4 a layer on nylium; Blighted Sands, about 2 a layer on soul sand and soul soil.
- **Shape:** vanilla's bush foliage (radius 2, offset 1, height 2) round one hidden crimson stem: a low, thorny dome
  2-3 tall and about 5 wide.
- **Leaves:** `bramble_bush_leaves`, dense. Not flammable, and no seasonal looks.
  - The bramble plant's dark-red cane colours, stepped into six tones: #2f130e #421a14 #56221a #682c23 #7f362a
    #954437.
  - Its one trait is thorns drawn as 2-pixel slanted marks, with a mid step (#a8705a) under a pale tip (#c8a888). Only
    4-5 pixels take the palest tone, on clump tops.
- **Extras:** its leaves drop sticks and the odd bramble. There is no sapling. Bone meal on a bramble on nylium or soul
  soil grows a bush, as bone meal on a fern grows a large fern. The drops stay below one bone meal's worth.

### Batch 9b: the dragonblood, the End's tree

Settle these with it:
- End saplings that grow without sky light, on end stone;
- the block the tree leaves under its trunk on end stone.

#### dragonblood
Invented, after the dragon's blood tree of Socotra (*Dracaena cinnabari*). **Medium.** The End gets a tree of its own
in place of two Overworld stand-ins. Its teal umbrellas sit well among violet jacarandas, purple chorus and pale end
stone.
- **Grows in:**
  - Ender Wilds 45%, as its default; the jacaranda stays at 30%. dragonblood_shrub takes 20%, replacing vanilla's
    azalea tree.
  - Outer Flats: a lone landmark on end stone only, about one chunk in ten, as a rare extra.
- **Shape:**
  - dragonblood: vanilla's cherry trunk placer, base 4 (+1), branch_count [2, 3], branch_horizontal_length [2, 3],
    with branches leaving 2-3 below the top. Acacia foliage (radius 2, offset 0) at each tip, so the flat pads meet in
    one canopy: a short, stout trunk splitting into upturned branches under one dense, flat umbrella.
  - dragonblood_shrub: one log under bush foliage (radius 2, offset 1, height 2).
- **Bark and wood:** its own wood, **dragonblood** (rule 9): *Dracaena* is not a palm, and its trunk is truly woody and
  thickens as it grows. It gives the End its first building wood, where only chorus and purpur grow.
  - Bark: smooth silver-grey, the streaked kind: #2f3237 #444950 #5a6068 #6f7680 #878e96, its streaks dark
    dragon's-blood resin (#462a2a #583232 #683a36). `bark()` reads one streak list, the eucalyptus's rainbow, so the
    build gives each wood a streak list of its own (one line).
  - Wood: #689db2, an otherworld teal like vanilla's warped (hue 197, lightness 0.55, saturation 0.33), the owner's
    painting 12 as painted. ΔE 27.2 from warped, the nearest vanilla wood; 23.0 from the dead wood; 23.9 from the bald
    cypress.
  - It is the one plank that reads as pastel ice rather than wood: CIE lightness 62 against warped's 40, at about
    warped's chroma. For the owner's eye beside warped planks and purpur, in the End and in the overworld. If it is too
    pale, painting 12 darkened to #508295 (ΔE 10.3 from the painting, 19.7 from warped) keeps the idea.
- **Wood:** NEW wood set "dragonblood", with new fronds and a sapling; dragonblood_shrub grows on it too.
- **Leaves:** `dragonblood_fronds`, fronds kind, drawn as dense, stiff 6-pixel blades: #102a2a #173836 #1f4744
  #295752 #346861 #457c74.
  - A deep pearl-teal at hue 173: darker and bluer than the eucalyptus, duller and greener than warped wart.
  - Evergreen.
- **Sapling:** survives on end stone and moss, and grows without sky light, as chorus does. The tree keeps end stone
  under its trunk, so check what `minecraft:soil_beneath_tree` does there.
- **Extras:** dragon's blood, a rare red resin dropped by its fronds, usable as red dye. It can only be reached after
  the dragon, so nothing in core progression may require it.

### Batch 10: hanging fruit

#### baobab
African baobab and Australian boab (*Adansonia digitata*, *A. gregorii*). **High.** The outback's icon, a silhouette no
vanilla tree has. Today the Outback's only tree is a generic acacia.
- **Grows in:** Outback: baobab 10%, giant_baobab 10%. A lone giant in the Lush Savanna is offered as the owner's
  call, under "Biomes by biome". It is not placed in the Dryland, which becomes American pinyon-juniper country.
- **Shape:**
  - baobab, the young tree a sapling grows: straight trunk 5 (+2) under acacia foliage (radius 2, offset 0).
  - giant_baobab, which four saplings in a square grow: a fat bottle trunk two blocks wide from vanilla's giant trunk
    placer, 6 (+2, +1), 6-9 blocks, under a sparse, stubby random_spread crown (radius 3, foliage_height 2, 40
    attempts).
  - Shoot both forms beside the desert acacia and the small palm.
- **Bark and wood:** its own wood, **baobab** (rule 9): *Adansonia* (Malvaceae) is a broadleaf, not a palm, and its
  two-block giant trunk is a big source of logs that players will fell.
  - Bark: smooth, pale copper-grey, in broad flat plates: #61524c #7b6861 #937e77 #a69691 #baafab.
  - Wood: #bb9b8e, a pale pinkish buff (hue 17, lightness 0.65, saturation 0.25), the eucalyptus's painting 21 (#bda281)
    recoloured by ΔE 10.9; it ends 10.9 from the eucalyptus. ΔE 12.6 from vanilla cherry, 10.7 degrees of hue away; 10.4
    from the pine, 12.0 from the fruitwood and 12.3 from the bald cypress; CIEDE2000 9.4 or more from every wood.
- **Leaves:** `baobab_leaves`, a fresh green, among the most open (about 6.5% gaps), dark beneath: #243e18 #335721
  #43722c #548a38 #66a845 #80ba63. Evergreen.
- **Sapling:** grows on sand and red sand (it survives as a dead bush would).
- **Pods:** a new hanging pod block in velvety olive-grey-green (#6e7056 #8a8c6c #a6a886), two pixels wide and 4-5
  tall on a one-pixel stalk, lit on top. Vanilla's `attached_to_leaves` decorator hangs them under the crown (about
  0.1, direction down), as the mangrove hangs propagules.
- **Wood:** NEW wood set "baobab", with new leaves, a sapling and the pod block. giant_baobab is its `"giant"` entry in
  `agriculture.TREES`, as the redwood's and the mahogany's are, so the giant needs no batch-4 change.
- **Extras:** baobab fruit from ripe pods, a small food. Its seeds plant the sapling, as a chestnut does.

#### date_palm
Date palm (*Phoenix dactylifera*). **Medium.** Date palms define an oasis, and they give players a renewable desert
food and a reason to visit.
- **Grows in:** Oasis 35%, beside the palm and the small palm (15%, added in batch 2); Dry River 60%, with the
  tamarisk.
- **Shape:** straight trunk 8 (+3), ramrod-straight, under cherry foliage (radius 2, offset 0, height 2,
  wide_bottom_layer_hole_chance 0.5, corner_hole_chance 0.5, hanging_leaves_chance 0.6,
  hanging_leaves_extension_chance 0.3). A full, flattened, drooping crown, unlike the palm's bend and flat spray.
  Compare it with the palm and the tree fern in game before settling it.
- **Wood and leaves:** palm's wood and fronds. A date palm (*Phoenix*) is a true palm (Arecaceae), the family the palm
  wood stands for, so it is the same wood (rule 9). Shape only, plus the date cluster block. It grows on sand.
- **Date clusters:** a new hanging block under the crown, placed by `attached_to_leaves` (about 0.2, direction down).
  Unripe clusters are golden (#c89a2a #e0bc48); ripe ones are amber-brown (#7a3e18 #a45a24 #c87a38) on a #d0a040
  stalk, drawn as a plant sprite lit from above.
- **Extras:** dates ripen on random ticks. A right-click picks 2-4 and leaves the cluster unripe again, as the
  chestnut's burs do. Dates are a sweet food and join `#jugcraft:fermentable` at the existing bioethanol rate, so no
  new conversion is added. The palm sapling could grow a date palm some of the time, through vanilla's secondary tree
  in `TreeGrower`.

### Batch 11: tropics, subtropics and coasts

#### flame_tree
Flame tree, or royal poinciana (*Delonix regia*). **Medium.** A flowering tree of the tropics. In the Jacaranda Glade
its scarlet against the jacaranda's violet is the classic subtropical pairing.
- **Grows in:** Tropics 10%, replacing vanilla's azalea tree, since azaleas are not lowland tropical trees; Jacaranda
  Glade 8%, from the vanilla cherry's share.
- **Shape:** forking trunk 4 (+1, +1) under acacia foliage (radius 3, offset 0). A low, very wide, flat umbrella 6-8
  blocks tall and about 9 across, lower and wider than the jacaranda.
- **Bark and wood:** vanilla acacia's grey bark and orange wood, a stand-in on its nearest relative in the game (rule
  9). The royal poinciana is not a jacaranda (Bignoniaceae), and not an acacia either: it is a legume of the subfamily
  Caesalpinioideae, which since the 2017 reclassification of the legumes also holds the acacias, so the two share a
  subfamily, not a genus. Acacia gives the same smooth grey bark under a flat umbrella crown on a forking trunk.
  - Its own wood was tried and fails rule 3's purpose. Flame-tree timber is soft and mostly burnt, and its best colour,
    #ba974f (painting 19 altered, ΔE 11.3), is ΔE 12.6 from vanilla oak with under 3 degrees of hue between them: a
    brighter, more saturated oak.
  - **Owner's call:** acacia logs as a stand-in (this roster's default), or a "poinciana" wood at #ba974f beside oak.
- **Leaves:** `flame_tree_leaves`, blossom kind, dense. In bloom all year.
  - Scarlet-orange flower clumps: #4a1c0e #682814 #89341a #a54224 #c9512c #d37255, at hue 14 and saturation 0.64.
  - Feathery fern green at the edges (#3a6a2a #4c8236).
- **Wood:** vanilla acacia logs as a stand-in, with new leaves and a sapling, through batch 4's `wood` field.
- **Extras:** none. The blossom could give red or orange dye.

#### sea_grape
Sea grape (*Coccoloba uvifera*). **Medium.** The real shore shrub of warm dunes, in place of vanilla-oak scrub on the
warmest beach, and it gives fruit.
- **Grows in:** Overgrown Beach 40%, as its default. The palm and the small palm join at 20% each in batch 2, and the
  pandanus at 20% if the owner keeps it.
- **Shape:** a hidden log under bush foliage (radius 2, offset 1, height 2): a sprawling shore shrub 2-3 blocks tall. It
  survives as a dead bush would, so it stands on sand. The occasional small tree form once planned is dropped, since its
  two-log stem would show an oak trunk the sea grape is not (rule 9).
- **Leaves:** `sea_grape_leaves`, in larger two-pixel clumps for its round, leathery leaves, an olive green: #2a321a
  #3b4725 #4e5c30 #5f713d #748a4a #8ba35c.
  - Its one trait is hanging grape clusters, green (#6a8a3a #8aa34a) and then purple when ripe (#3e1e42 #5a2d5e, lit
    #7a4a7e).
  - Evergreen.
- **Wood:** none (rule 9: a shrub on a hidden stem); one hidden vanilla oak log, through batch 4's `wood` field. Its own
  fruiting leaves and a sapling.
- **Extras:** sea grapes, picked ripe while the bush stays.
  - Food.
  - Sea grape jelly in a Mason Jar.
  - `#jugcraft:fermentable`.

#### pandanus
Screw pine (*Pandanus tectorius*). **Low, and dropping it is recommended.** It would give the warm shores a striking
silhouette; its only merit was costing no blocks.
- **Grows in:** Overgrown Beach 20%; Tropics 10%. If it is dropped, its share goes to the sea grape in the Overgrown
  Beach and to the palm in the Tropics.
- **Shape:**
  - Trunk: vanilla's bending trunk 4 (+2), bend_length 1, with `min_height_for_leaves` above the trunk, so only the bend
    carries a tuft.
  - Roots: vanilla's `mangrove_root_placer` lifts it, with palm logs as the roots (trunk_offset_y 2, max_root_width 3,
    max_root_length 3), so stilt roots splay down to the sand.
  - Crown: a tuft of palm fronds (acacia foliage, radius 1).
  - A leaning shore tree 5-7 blocks tall that grows on sand.
- **Wood:** a stand-in on palm logs, worldgen only, with the root placer from batch 4b (rule 9). A screw pine is a
  monocot of another order (Pandanales), not a palm (Arecaceae); its ringed, fibrous trunk only looks like a palm's.
  **Owner's call:** drop it (recommended), or keep the stand-in.

#### osmanthus
Sweet osmanthus (*Osmanthus fragrans*). **Low.** Guilin, the archetypal karst landscape, means "forest of sweet
osmanthus". It gives the pinnacles a broadleaf of their own and ties them to the mod's Mid-Autumn foods.
- **Grows in:** Karst Pinnacles 15%, from the spruce bush's share.
- **Shape:** forking trunk 2 (+1, +1) under random_spread foliage (radius 2, foliage_height 3, 50 attempts). A
  multi-stemmed shrub-tree 4-6 blocks tall, a silhouette of its own rather than the orange and apple trees' ball.
- **Wood:** the olive's logs (batch 7) as a stand-in, through batch 4's `wood` field (rule 9). Osmanthus shares the
  olive's family and tribe (Oleaceae, tribe Oleeae), not its genus, though it is so close to *Olea* that Linnaeus named
  devilwood *Olea americana*, and it has the olive's hard, close-grained, yellowish wood and grey bark. It is a
  shrub-tree whose timber nobody uses, so it gets no wood set, but its forking trunk shows olive logs. **Owner's call:**
  olive logs as a stand-in (this roster's default), or one hidden stem under bush foliage, as the gorse has.
- **Leaves:** `osmanthus_leaves`, a dark glossy green: #162a14 #1e3a1b #284c24 #325d2e #3e7138 #4c8646.
  - Its one trait is tiny pale cream-gold flowers as single pixels (#e8d08a #f2e2a8), tucked among the leaves. They
    are sparse enough that the tree reads green from afar and gold up close, never orange.
  - Evergreen, in flower all year.
- **Extras:** osmanthus flowers.
  - A Candy Kettle flavour with **luck**, an effect no flavour uses yet.
  - An osmanthus mooncake filling, beside the existing mooncakes and lantern festival.

#### driftwood
Bleached driftwood logs washed ashore. **Low.** It gives the bare shores, which stay treeless, a natural detail and a
little wood.
- **Grows in:** Dune Beach, on the strand, where the sand gives contrast; Gravel Beach, more rarely.
- **Shape:** a small Jugcraft feature, written in Java, that lays 3-6 *stripped* dead logs flat on sand or gravel just
  above the waterline, with no stump. Vanilla's `fallen_tree` always places a stump, and bleached driftwood has lost
  its bark.
- **Wood:** reuses stripped dead logs, the mod's bark-less silver-grey wood. Driftwood is not a species, so it needs
  no wood of its own (rule 9). No new blocks.

### Batch 11b: the banyan, and the fig wood

#### banyan
Banyan (*Ficus benghalensis*). **Medium.** A rooted, many-trunked sacred fig beside the great oaks and the warm
calcite pools makes the springs read as a shrine of the tropics.
- **Grows in:** Shrine Springs 14%, replacing vanilla oak. The great oak stays the default.
- **Shape:** vanilla mangrove's `upwards_branching` trunk, 6-9 blocks, branching as it climbs.
  - Vanilla's `mangrove_root_placer` (batch 4b), with fig logs as the roots, flares its foot narrowly, with
    max_root_width 1-2, rather than arching like a mangrove.
  - A wide, low dome of blob foliage (radius 5, offset 0, height 3), with trunk vines.
  - Its defining look is vanilla's `hanging_roots`, hung under the crown by `attached_to_leaves` (direction down,
    about 0.15) as aerial roots.
- **Bark and wood:** a new wood, **fig**, named for its genus (rule 9): a banyan is a fig (*Ficus*, Moraceae), not
  jungle wood, and a vast many-trunked tree that players will fell for logs. A later common fig, the same genus, shares
  it under the same name, as a later yucca would share the Joshua tree's.
  - **Owner's call, since IDs are permanent:** a wood named `fig` with the tree `banyan` (this roster's default, by the
    naming rule under "How to read this"), or both woods named for their trees (`banyan`, and `joshua_tree` for the
    yucca).
  - Bark: pale grey, stringy for its aerial roots and fused stems: #4f4d4a #686561 #817d79 #989592 #b0adab. It is
    lighter than its planks.
  - Wood: #7f6f63, a taupe *Ficus* wood (hue 26, lightness 0.44, saturation 0.12), the owner's painting 8 (#7a7067)
    altered by ΔE 3.2. ΔE 19.1 from vanilla spruce; 10.4 from the dead wood and 12.3 from the cedar; 10.1 from the yucca
    and 10.5 from the mango (CIEDE2000 9.0 from the mango and 9.4 from the dead wood).
  - The banyan's identity is its pale bark and roots, not these generic taupe planks, and it grows in one biome. If wood
    sets are trimmed, the fig wood is the first to cut (9 IDs); the banyan would then wait, or stand on vanilla jungle
    logs as a stand-in.
- **Wood:** NEW wood set "fig" (shown as "Fig"): log, wood, stripped log and wood, planks, stairs, slab, fence and fence
  gate. The banyan keeps its own name for its leaves, sapling and shape (`banyan_leaves`, `banyan_sapling`, `banyan`) on
  fig logs, through batch 4's `wood` field. A fig fruit item, if a fig tree ever comes, would be `fig` and touch none of
  these IDs.
- **Leaves:** `banyan_leaves`, a glossy dark green with more two-pixel clumps and about 3% gaps: #173113 #20441a
  #2a5922 #356c2c #408436 #509c45. Its one trait is a few small red figs drawn on the leaves (#7d2929 #9c3e3e
  #bb5555), as decoration only. There is no fig item, so the IDs stay free for a real fig tree. Evergreen.
- **Extras:** none.

### Batch 11c: the mango

#### mango
Mango (*Mangifera indica*). **Medium.** Every tree in the Lush Grassland is a vanilla wood today. The mango gives it,
and its villages, a tree of their own and a warm-climate fruit.
- **Grows in:** Lush Grassland 20%, replacing vanilla's jungle tree. The small palm joins it at 10% in batch 2, and
  the banana becomes its default in batch 12.
- **Shape:** straight trunk 4 (+2) under a big, deep, dense dome (blob radius 3, offset 0, height 4). A stout fruit
  tree 7-9 blocks tall and 7 wide.
- **Bark and wood:** its own wood, **mango** (rule 9). Mango (*Mangifera*) has a well-known furniture timber of its own,
  a light golden or greyish tan with dark streaks; vanilla jungle is an unnamed tropical wood.
  - Bark: dark grey, furrowed: #312d2b #413c39 #514b48 #615a56 #716965.
  - Wood: #938167, a greyish golden tan, duller than oak, like a weathered oak (hue 36, lightness 0.49, saturation
    0.18), from the owner's elm (2:4, #866448) altered by ΔE 13.0; it now sits nearer painting 6 (8.4). ΔE 14.4 from
    vanilla jungle, 9.6 degrees of hue apart, and 16.1 from oak, 2.1 degrees apart; 14.1 from the eucalyptus; 10.1 from
    the pine and 10.5 from the fig and the magnolia.
  - It is the most crowded new wood: by CIEDE2000, vanilla oak 7.7, the pine 8.9 and the fig 9.0, and the eucalyptus's
    shaded side is 1.4 from it. It is offered as a weathered oak for the owner's eye, and is the next set to cut after
    the fig if sets are trimmed.
- **Leaves:** `mango_leaves`, dense (about 2% gaps), a dark glossy green: #0f2b11 #153d17 #1b4f1f #246028 #2b7530
  #388a3d.
  - It fruits in the apple's cycle (the `AppleLeavesBlock` pattern): plain leaves, then a few cream-pink flower sprays
    (#d8b8a8 #e4ccc0), then three or four 2×3-pixel mangoes.
  - The mangoes go from green-gold to orange-red: #5a8a2a, #e0a428, #d0582a.
  - Evergreen.
- **Wood:** NEW wood set "mango", with new fruiting leaves and a sapling.
- **Extras:** mangoes, picked 1-2 at a time from ripe leaves.
  - Food.
  - Mango chutney in a Mason Jar, with cider vinegar.
  - `#minecraft:parrot_food`.
  - `#jugcraft:fermentable`.

### Batch 12: rainforest

This batch follows agriculture slice 5, which already plans the rubber tree and its tapping.

#### rubber_tree
Pará rubber tree (*Hevea brasiliensis*). **High.** `docs/features/rubber.md` names natural rubber trees as a planned
addition, and `AGRICULTURE.md` slice 5 plans the tree with tapping. **Slice 5 builds it**: the log, the leaves, the
sapling and the tree tap. This roster only places it, as it does the slice-4 fruit trees.
- **Grows in:** Rainforest 12%, replacing vanilla's jungle tree.
- **Shape:** straight trunk 9 (+3) under blob foliage (radius 2, offset 0, height 4), with leaf vines at 0.1. A tall,
  clean trunk with a small oval crown high up.
- **Bark:** a new `rubber_log`, smooth grey-brown and furrowed with only three shallow furrows: #434137 #585548
  #6d695a #827e6b #95917e.
  - Its one trait is a few short cream latex drips (#e6dcc4, 2-3 px, vertical) on the ridges.
  - No bands. Its end rings are a pale pinkish tan.
- **Leaves:** `rubber_leaves`, a glossy mid green, dense (about 3% gaps): #1e3914 #29501b #366824 #447f2f #539b39
  #66b54a. Evergreen.
- **Wood:** no wood set, and a stand-in (rule 9): rubberwood is a real timber, but these planks are jungle's.
  - `rubber_log` strips to vanilla's stripped jungle log and saws into jungle planks. **Owner's call**, with slice 5:
    keep the jungle planks, or a rubberwood set later.
  - It is in `#minecraft:logs`, so its leaves do not decay, but not in `#minecraft:logs_that_burn`.
- **Extras (slice 5):** latex from a tree tap on a `rubber_log`.
  - The tap fills slowly on random or scheduled ticks, not a per-tick block entity, and is capped.
  - Latex set with cider vinegar makes `jugcraft:rubber`, the item the polymerization reactor already makes.
  - The route stays slower than the naphtha route, with its rate and losses written down.

#### banana
Banana (*Musa acuminata*). **High.** The iconic plant of tropical gardens and forest edges, in place of vanilla jungle
bushes, and it gives food.
- **Grows in:** Tropics 15%; Rainforest 10%; Lush Grassland 45%. It replaces vanilla's jungle bush, and is the Lush
  Grassland's default.
- **Shape:** straight trunk 2 (+1) of a new `banana_stem`, under a broad, drooping crown of cherry foliage (radius 2,
  offset 0, height 2, hanging_leaves_chance 0.5, hanging_leaves_extension_chance 0.2, corner_hole_chance 0.5). A
  soft-stemmed plant 4-5 blocks tall, clearly not the small palm's tuft.
- **Stem:** streaked, on a green-brown ramp (#474b25 #5e6331 #757b3d #8b9348 #a2ab54), with long vertical brown
  streaks (#5a4a2a) where old leaf bases dried.
- **Leaves:** `banana_fronds`, a broad fronds variant: two broad 2-pixel blades a tile, with tear gaps along the veins.
  A bright yellow-green: #334715 #47641d #5d8326 #739e32 #8cc13d #a3ca68. Evergreen.
- **Bunch:** a new `banana_bunch` hangs below on `attached_to_leaves` (about 0.3, direction down), green (#5a7a2a
  #7a9a34) and then yellow when ripe (#c8a428 #d8b832 #e6cc5a).
- **Wood:** none (rule 9: a giant herb whose "trunk" is a pseudostem of leaf sheaths).
  - `banana_stem` is an axis pillar that breaks by hand, drops itself, composts and makes no planks.
  - It is in `#minecraft:logs`, so the leaves do not decay, but not in `#minecraft:logs_that_burn`.
  - A banana sapling, the sucker, drops from the fronds.
- **Extras:** bananas, picked from a ripe bunch without felling the plant. Food, and `#jugcraft:fermentable`.

#### tree_fern
Tree fern (*Cyathea*; *Dicksonia antarctica*). **Medium.** Tree ferns fill the understory of three wet forests with
one plant. Mountain ash over tree ferns is the classic wet eucalyptus forest.
- **Grows in:** Rainforest 9%, replacing the rest of vanilla's jungle bush; Eucalyptus Forest 5%; Temperate
  Rainforest 3%.
- **Shape:** straight trunk 3 (+2) of a new `tree_fern_trunk`, under acacia foliage (radius 2, offset 0). A fern on a
  trunk, 4-6 blocks, with a flat, lacy rosette that differs from the banana's droop and the date palm's tall ball.
- **Trunk:** matted dark-brown fibres, stringy kind (#271d16 #392b20 #4b382a #5d4634 #6f533e), with a few pale
  frond-scar flecks (#7a6a4a) and no bands.
- **Leaves:** `tree_fern_fronds`, fronds kind, drawn as many 3-pixel pinnae on a dark rachis. A cool fern green:
  #17331f #20472c #2a5d39 #357147 #418a57 #52a36a. It is ΔE 11 from the mahogany and further from the rainforest's other leaves.
  Evergreen.
- **Wood:** none (rule 9: a fern, whose trunk is a mass of fibrous roots, not wood).
  - `tree_fern_trunk` is an axis pillar with no planks and no stripping, which burns and composts.
  - It is in `#minecraft:logs` for leaf distance.
  - A sapling drops from the fronds.

#### buttressed_mahogany
Big-leaf mahogany (*Swietenia macrophylla*) as an emergent on buttress roots. **Medium.** Buttress roots are what make
a rainforest giant read as one, and they remove the last vanilla emergent from the canopy at no block cost.
- **Grows in:** Rainforest 7%, replacing vanilla's mega jungle tree.
- **Shape:** vanilla's mega_jungle trunk, 18 (+4, +10), lifted by vanilla's `mangrove_root_placer` (batch 4b) with
  mahogany logs as the roots (trunk_offset_y 2-3, max_root_width 4, max_root_length 5). A wide, flat crown of jungle
  foliage (radius 3, height 2), with trunk vines and leaf vines at 0.25.
- **Prototype first.** Vanilla pairs the root placer only with one-block trunks, so under a 2×2 trunk the flare may
  sit on one corner and the other columns may float. Shoot it in `WoodClientGameTests` first. If it fails, fall back
  to a taller giant_mahogany, with no root placer.
- **Wood:** reuses mahogany, the same species, worldgen only. Four mahogany saplings still grow giant_mahogany.

### Batch 13: walnut and bristlecone

#### walnut
Walnut (*Juglans regia*; black walnut, *J. nigra*). **Medium.** It gives the dense oak forest a tree of its own and a
nut to forage. It is medium priority because the plain oak woods are already right.
- **Grows in:** Dense Forest 10%, from the vanilla fancy oak's share; Woodland 4%.
- **Shape:**
  - walnut: straight trunk 6 (+2) under a blob (radius 3, offset 0, height 4). A tall, open, round crown 8-10 blocks
    tall.
  - big_walnut: fancy trunk 9 (+5) under fancy foliage (radius 2, offset 4, height 4), as big_maple is.
- **Bark and wood:** its own wood, **walnut** (rule 9): *Juglans* is a premier timber, not an oak.
  - Bark: the owner's painted black-walnut bark (`OWNER_BANK` row 2), furrowed: #392e27 #3c322c #413730 #4d433d
    #594f49.
  - Wood: **#5a4b41**, a cool grey-brown (hue 24, lightness 0.30, saturation 0.16), from the owner's black walnut (2:2,
    #67533c) altered by ΔE 9.1.
  - As painted, the black walnut sat on vanilla spruce planks' hue (32) and lightness, which is why the roster first
    left it banked "until the owner nudges that colour at least 8 degrees away from spruce". This colour is 8.3 degrees
    and ΔE 18.0 from spruce.
  - The merged list of new woods had it at #665347, but that is only ΔE 6.5 from the cedar as batch 1 built it
    (#725543), at the same hue. It was darkened by ΔE 4.3 to clear 10: ΔE 10.1 from the cedar, 11.1 from the persimmon
    and the yew, and 17.5 from vanilla dark oak.
  - That cost the painting its warmth (saturation 0.26 to 0.16, hue 32 to 24), and it still sits on the cedar's hue:
    CIEDE2000 7.4 from the cedar, 8.7 from the persimmon and 8.9 from the yew. A search by script found no better slot
    near the painting. It now lies nearer the owner's wenge (2:3, ΔE 5.3) than the black walnut, but keeps the black
    walnut's credit: its bark is that painting's, painted for a walnut. The owner sees the cedar, walnut, yew and
    persimmon together on the wall pairs.
- **Wood:** NEW wood set "walnut", with new leaves and a sapling; big_walnut grows on it too.
- **Leaves:** `walnut_leaves`, a light, slightly yellow mid green with about 5% gaps. One seed on a schedule of about
  [105, 255, 290].

  | Look | Colours | Trait |
  |---|---|---|
  | green | #2b3c1a #3c5424 #4f6e2f #61863c #77a449 #8fb668 | smooth, round 2×2 pale lime husks (#8aa040 #a8c050, lit #c4d870), darkening to black-brown when ripe (#2e2418 #4a3a28). No split shells, which are the chestnut's trait. |
  | autumn | #504412 #705f19 #937c21 #b2972c #cfb240 #d6c271. A short yellow-gold. | |
  | winter | bare early, in the walnut bark's colours | |
- **Extras:** walnuts, picked ripe as chestnuts are. The husks give brown dye.
  - Food, raw or roasted at a campfire.
  - `#jugcraft:squirrel_food`.
  - A mooncake filling.

#### bristlecone
Great Basin bristlecone pine (*Pinus longaeva*). **Medium.** The oldest living trees, standing alone on bare ridges,
deepen the barren basin's ancient mood without filling it.
- **Grows in:** Basin, as a rare lone tree on gravel and stone, about one chunk in four.
- **Shape:** a fancy trunk like the dead tree's, made shorter, 4 (+2), with small tufts at a few branch ends (blob
  radius 1, offset 0, height 2). An ancient, twisted, mostly silver tree 4-7 blocks tall.
- **Bark and wood:** the pine wood from batch 2 (rule 9): *Pinus longaeva* is a pine, and the pines share one wood. Its
  silver look is bare, polished deadwood, so most of the trunk is stripped pine log (the grey-tan #a49a84), and bark-on
  pine logs (#2d2521-#9c6c48) stand for the living strip of bark. Vanilla's weighted state provider places them, about
  70% stripped and 30% bark-on. Check in game how the fancy placer's axis handling treats the mixed logs.
- **Leaves:** the tufts are the tree `pine`'s `pine_needles` (batch 2), with no cones: a bristlecone bears no pine nuts,
  and its tufts drop pine saplings, of its own genus.
- **Wood:** reuses pine logs, stripped and bark-on, under `pine_needles`: a shape of the tree `pine`, so it needs no
  batch-4 `tree` field and passes `check_mod_data` as it stands. Worldgen only, with no new blocks and no sapling of its
  own. Its placement checks for gravel, stone or andesite below in place of a sapling check.

### Batch 14: orchard fruit (agriculture slice 4)

`AGRICULTURE.md` already plans pear, peach, lemon and orange for slice 4, so they are built there with the
`AppleLeavesBlock` pattern and placed wild by this roster. The same PR:
- places the existing apple tree in the Orchard (`apple_tree_checked`, with an `apple_tree_bees` variant), in place of
  vanilla's oak with bees;
- first redraws `apple_leaves`, `apple_leaves_blossom` and `apple_leaves_ripe` in `wood_style.py`'s manner, since
  they are still drawn by `cider_textures.py`.

The pear grows on the fruitwood (batch 5), so slice 4 adds no wood set. The fruitwood's switch list gains agriculture
(rule 4), as the chestnut's has, since slice 4 builds the pear and the Orchard places it. `AGRICULTURE.md`'s slice-4 row
does not say which logs the pear grows, so this is agreed with the agriculture branch before the slice is built.

#### pear
European pear (*Pyrus communis*). **Medium.** Its upright crown adds shape to the Orchard beside the round chestnut
and apple.
- **Grows in:** Orchard 15%.
- **Shape:** straight trunk 5 (+2) under a blob (radius 2, offset 0, height 4). Upright, taller than wide, 7-8
  blocks.
- **Bark and wood:** the fruitwood from batch 5 (rule 9), shared with the rowan and the hawthorn. *Pyrus* is a classic
  fine timber, for carving, instruments and drawing tools, pale pinkish brown: the Maleae fruitwood. On maple logs, as
  first planned, a felled pear would have given "Maple Log"; on fruitwood it gives "Fruitwood Log".
  - It shows the fruitwood's silver-grey bark, smoother than a pear's own dark, blocky bark, and its pinkish wood
    #ab7d76.
  - Its own set, at #9f7e77 and credited to the cypress's painting 14, was cut on review: it sat ΔE 5.7 from painting 7,
    the rowan's, and CIEDE2000 8.2 from the rowan. If the owner keeps two sets instead of the fruitwood, the pear's
    would be #9f7e73 (see Decisions).
- **Wood:** no wood set. Its own leaves and sapling (`pear_leaves`, `pear_sapling`) on fruitwood logs, through batch 4's
  `wood` field.
- **Leaves:** `pear_leaves`, a glossy deep green, bluer than the apple's: #18341e #22492a #2d5f37 #397445 #468e54
  #58a768.
  - Fruit stages as the apple's leaves have: blossom (#b8b0b4 #d8d2c8, with single-pixel highlights of #e4dfd6), then
    green-yellow pears two pixels tall (#8a9030 #a8b040 #cdd060).
  - No autumn look, as the apple has none.
- **Extras:** pears; the tree is never cut down.
  - Food.
  - `#jugcraft:cider_apples`, so the Cider Press makes perry-style cider.
  - `#jugcraft:fermentable`.

#### peach
Peach (*Prunus persica*). **Low.** Spring blossom and a low vase shape fill out the Orchard's understory.
- **Grows in:** Orchard 10%.
- **Shape:** forking trunk 3 (+1, +1) under a blob (radius 2, offset 0, height 2). Low, open and vase-shaped, 4-5
  blocks.
- **Wood:** vanilla cherry logs, the same genus (*Prunus*, rule 9), through batch 4's `wood` field.
- **Leaves:** `peach_leaves`, a lighter yellow-green: #2e411a #405c25 #547830 #68923e #7fb24c #99bf73.
  - Fruit stages: deep rose-magenta blossom (#b84a6a #d4708c #e896aa), clearly not cherry pink.
  - Then blushing peaches (#d0603a #e08a4a #f0b070).
- **Extras:** peaches.
  - Food.
  - A peach pie filling for the Hearth Oven, appended to the end of `PIES["fillings"]` and the `PieFilling` enum.
  - `#jugcraft:fermentable`.

#### orange
Sweet orange (*Citrus × sinensis*). The lemon is the same tree with yellow fruit. **Medium.** A warm-climate fruit
tree that feeds the existing kitchen machines, in place of a temperate vanilla birch.
- **Grows in:** Subtropics 10%, replacing vanilla birch.
- **Shape:** straight trunk 3 (+1) under a blob (radius 2, offset 0, height 3). A small, dense, round tree 4-5 blocks
  tall, like the apple tree, which never shares its biome.
- **Wood:** vanilla oak logs, as the apple tree has, through batch 4's `wood` field: a stand-in, since citrus is not an
  oak. Orange and lemon are small fruit trees picked while they stand, and citrus timber serves only small turned items.
  - No citrus colour is worth having. The pale yellows sit within 2-3 degrees of the willow's hue, and the best cream,
    #cbba9f (an altered painting), is ΔE 8.5 from the holly. The holly's slot is not the only obstacle: by CIEDE2000
    that cream is also 7.1 from the palm, 7.3 from the eucalyptus, 8.5 from vanilla birch and 8.7 from the chestnut, and
    a script search finds no pale cream that clears every wood by vanilla's oak-jungle 9.3.
  - **Owner's call, pending:** keep the orange on vanilla oak as a stand-in, as the apple is; or move the holly and give
    its slot to a citrus wood, which would still sit close to the palm and the eucalyptus. If every fruit tree should
    have its own wood, a "citrus" wood shared with the lemon needs a new search, and the apple, a Maleae, would join the
    fruitwood.
- **Leaves:** `orange_leaves`, a glossy dark green: #193010 #234317 #2e581e #396a27 #468230 #56983e.
  - Fruit stages as the apple's leaves have: white blossom specks (#d8d2c4 #e4dfd2), then small green fruit
    (#5a7a2a), then oranges (#c86a14 #e0861c, lit #f0a040) in two-pixel clusters.
  - Evergreen.
- **Extras:** oranges, picked while the tree stays.
  - Food.
  - Marmalade in a Mason Jar.
  - A Candy Kettle flavour with an effect no flavour uses yet (proposed: jump boost).
  - `#jugcraft:fermentable`.

## Build order

Each batch is one focused PR, and brings at most one new wood set (rule 9). They are listed highest value first, and
each assumes the ones before it unless the table says otherwise. Batches 1, 2, 3 and 7 need nothing from batch 4 and can
run side by side; batch 3 also carries the leaf drawing options that the later wood batches use. A lettered batch (4b,
5c) split off its parent when the owner's update or the review gave its parent more than one new wood or too much else;
it can follow its parent directly or run beside the next batches. Each PR also:
- updates the records of the biomes it changes;
- adds its trees to `WoodClientGameTests`, shot beside vanilla's trees and their neighbours;
- runs `python3 tools/check_mod_data.py`, `python3 scripts/check_repository.py` and `./gradlew build`;
- states that it has not been play-tested until someone plays it.

| # | Batch | Trees | Shared work in it | Depends on |
|---|---|---|---|---|
| 1 | Northern, wet and cool-forest shapes | stunted_fir and bog_fir, subalpine_fir, fir_bush, tamarack, dead_snag, willow_bush, young_aspen, swamp cedar (built as `cedar`), mossy_maple | the any-of feature switches and their check; larch in `FALLEN`; the free swaps below; the cedar wood set (owner's row 0) | none |
| 2 | Warm and mountain shapes, and the pine | cottonwood, japanese_maple, live_oak, pine (the windswept pine), big_jacaranda, mallee, giant_eucalyptus | the pine wood set (painting 6), `pine_needles`, `pine_sapling` and the shape `pine`; the Lush Desert fix; `apple_tree_checked`; the eucalyptus `"giant"` entry; a cherry-placer bounds check; rule 3's reading in `NATURAL_TEXTURES.md` | none |
| 3 | The yew, and the leaf drawing options | yew | the yew wood set (owner's row 7) and its check against the cedar's wood; the leaf drawing options of rule 3; the Gloomweald canopy fix | 1 (for the dead snag and the cedar) |
| 4 | Own leaves on borrowed logs | snowpetal_cherry, shimmer_birch | `TREES` `wood`/`display` and `SHAPES` `tree` fields; the Java split of `registerTree`; switch, names and tree-farm output by log; the naming rule. No wood set. Design note first. | 1; 3 (blossom options) |
| 4b | The bald cypress | bald_cypress | the bald cypress wood set (painting 23, recoloured); `upwards_branching` and the `mangrove_root_placer` | 1; 3 (leaf drawing options) |
| 4c | The magnolia | magnolia | the magnolia wood set (London plane, 2:1, altered) | 3 (leaf drawing options) |
| 5 | Fruit on seasonal leaves | rowan, elder | `SeasonalFruitLeavesBlock`; the lone-tree extra; the fruitwood set (painting 7), brought by the rowan. Design note first. | 4 (the rowan on fruitwood, the elder on a hidden stem) |
| 5b | The hawthorn | hawthorn | none: its leaves, sapling and haws on the fruitwood | 5 |
| 5c | The persimmon | persimmon | the persimmon wood set (painting 18, warmed) | 5 |
| 6 | Evergreen scrub | juniper, gorse, sagebrush | the juniper wood set (painting 20, altered); the Steppe's trees slot | 4 (gorse and sagebrush on hidden stems) |
| 6b | The holly | holly | the holly wood set (painting 10, recoloured); the Holly Wreath | 5 (holly's berries) |
| 7 | The olive | olive | the olive wood set (painting 3, nudged off bamboo); Lavender Field `winter_snow` off | none |
| 8 | Dry country | stone_pine, wattle | Alpine Spawn's picks list, with the juniper bush there and alpine_spawn on the pine's and juniper's switch lists; sand saplings | 2 (pine wood), 4, 6 (the juniper) |
| 8b | The tamarisk | tamarisk | the tamarisk wood set (painting 2:6, recoloured); the Dry River's tries, unless batch 10 has given them | 3 (leaf drawing options) |
| 8c | The Joshua tree, and the yucca wood | joshua_tree | the yucca wood set (wenge, 2:3, altered) | 3 (fronds options), 4 (the Joshua tree on yucca logs) |
| 9 | Giant fungi and the Nether | huge_glowcap, blight_fungus, marrow_fungus, bramble_bush | the glowcap cap, blight wart, small blight fungus, bramble leaves; Nether and cave placements; the no-gain arithmetic | 4 (the bramble on a hidden crimson stem) |
| 9b | The dragonblood, the End's tree | dragonblood | the dragonblood wood set (painting 12); a streak list per wood in `bark()`; End saplings and the block under the trunk on end stone | 3 (fronds options) |
| 10 | Hanging fruit | baobab, date_palm | the baobab wood set (painting 21, recoloured) and its giant entry; the pod and date cluster blocks, hung by `attached_to_leaves`; the Dry River's tries, unless batch 8b has given them | 3 (leaf drawing options) |
| 11 | Tropics, subtropics and coasts | flame_tree, sea_grape, pandanus, osmanthus, driftwood | the driftwood feature; the osmanthus candy flavour. No wood set. | 4; 4b (the pandanus's roots); 7 (the osmanthus on olive logs) |
| 11b | The banyan, and the fig wood | banyan | the fig wood set (painting 8, altered) | 4 (the banyan on fig logs); 4b (its roots) |
| 11c | The mango | mango | the mango wood set (elm, 2:4, altered) | 3 (leaf drawing options) |
| 12 | Rainforest | banana, tree_fern, buttressed_mahogany, and the rubber tree's placement | three trunk blocks in `#minecraft:logs`; the buttress prototype gate | 4; 4b (the buttress roots); agriculture slice 5 (rubber tree) |
| 13 | Walnut and bristlecone | walnut, bristlecone | the walnut wood set (black walnut, 2:2, altered) | 2 (pine wood); 5 (the walnut's seasonal fruit leaves) |
| 14 | Orchard fruit (agriculture slice 4) | pear, peach, orange | no wood set: the pear on the fruitwood, whose switch list gains agriculture; the apple leaves redrawn; the apple in the Orchard | 4; 5 (the fruitwood) |

With the leaf drawing options in batch 3, a wood batch waits for batch 4's tree-and-wood split only where its tree keeps
its own name on its wood: batches 5, 8c and 11b.

**Free swaps of existing trees.** These need no new tree, and go in batch 1, or batch 2 for the warm ones:

| Swap | Where | Batch |
|---|---|---|
| larch joins | Coniferous Forest 10%, Shield 8%, Lake District 10% | 1 |
| fir replaces vanilla spruce | Maple Woods, Redwood Forest, Lake District (this covers the rejected hemlock) | 1 |
| fir joins as a plain pick | Seasonal Forest 4% | 1 |
| aspen joins or replaces | Snowy Forest 20%; replaces the Shield's vanilla oak and the Snowpetal Grove's vanilla birch | 1 |
| fallen dead log joins | Dead Swamp, Burnt Forest and Rotted Expanse | 1 |
| willow joins | Lush River | 1 |
| maple joins | Lush Swamp | 1 |
| vanilla azalea joins | Swamp Woods | 1 |
| great oak joins as a rare veteran | Dense Forest | 1 |
| more tries a chunk | Wasteland's count rises to 1; Cinder Barrens gets 1-2 snag tries | 1 |
| cypress joins | Lavender Field, with `winter_snow` off | 2 |
| small palm joins | Lush Grassland, Lush Desert and Oasis | 2 |
| palm and small palm join | Overgrown Beach | 2 |
| mahogany replaces vanilla oak | Mycelial Jungle | 2 |
| apple tree joins | Flower Isle | 2 |

## Biomes by biome

Every biome in `tools/biomes.py`, plus Alpine Spawn and Pixel Hollows. **Now** shows the shares before batch 1, worked out from
the in-order chances in `biomes.py`. **After** shows the target shares once every batch has landed. "Stays" means no
change, with the reason. Expected frequencies of rare trees are estimates, to be tuned in the client survey.

Trees are named here by shape. A tree that moved to a new wood keeps its name in these tables, and its wood is in the
roster. One shape was renamed on review: the windswept pine's shape is `pine`, after its tree, as batch 1's swamp cedar
became `cedar`. Two names to keep apart: "pine (v)" is vanilla's pine feature (`minecraft:pine_checked`, on spruce
logs), and "pine" is the new tree's windswept shape (`jugcraft:pine_checked`); both stand in the Hot Springs. The pine
wood's shapes here are the pine, umbrella pine, stone pine, pinyon and bristlecone. The Batches column gives lettered
batches (4b, 5c) where a tree's batch split off its parent.

### Seasonal forests (`docs/features/seasonal-forests.md`)

| Biome | Now | After | Batches |
|---|---|---|---|
| Coniferous Forest | fir 68, tall fir 25, spruce (v) 6, fallen fir 1 | fir 57, tall fir 20, larch 10, stunted fir 8 (for the spruce), dead snag 3, fallen fir 2 | 1 |
| Snowy Coniferous Forest | fir 78, tall fir 20, fallen fir 2 | fir 48, subalpine fir 20, tall fir 20, stunted fir 10, fallen fir 2 | 1 |
| Maple Woods | maple 76, big maple 12, spruce (v) 11, fallen maple 1 | maple 75, big maple 12, fir 12 (for the spruce), fallen maple 1 | 1 |
| Seasonal Forest | oak with bees and leaf litter (v) 49, maple 35, aspen 13, big maple 3, fallen maple 1 | maple 35, oak (v) 34, aspen 13, persimmon 10, fir 4, big maple 3, fallen maple 1 | 1, 5c |
| Aspen Glade | aspen 89, maple 10, fallen aspen 1 | aspen 69, young aspen 15, maple 10, subalpine fir 5, fallen aspen 1 | 1 |
| Dead Forest | dead tree 73, spruce (v) 15, oak (v) 8, fallen dead 4 | dead tree 40, dead snag 35, spruce (v) 10, juniper 10 (for the oak), fallen dead 5 | 1, 6 |
| Tundra | maple bush 100 | maple bush 40, willow bush 30, fir bush 20, juniper bush 10 | 1, 6 |
| Snowy Forest | oak (v) 56, fir 30, maple 14 | rowan 36 (for the oak), fir 30, aspen 20, maple 14 | 1, 5 |
| Muskeg | dead tree 71, fir 25, fallen dead 4 | tamarack 35, bog fir 30, dead snag 20, dead tree 10, fallen dead 3, fallen larch 2 | 1 |

### Fields and meadows (`docs/features/fields-and-meadows.md`)

| Biome | Now | After | Batches |
|---|---|---|---|
| Field | oak bush (v) 66, spruce (v) 30, oak with bees (v) 3 | hawthorn bush 55, juniper column 30 (for the spruce), hawthorn 10, oak with bees (v) 5, kept for its bees | 5b, 6 |
| Flower Meadow | none | stays: an unbroken carpet of flowers is its point. The wild apple patch reaches it about one chunk in twelve; the owner may keep or remove it. | none |
| Grassland | none | stays: the open clover pasture that sets it apart. If hedgerows are ever wanted, use the hawthorn bush. | none |
| Heathland | oak bush (v) 70, pine (v) 30 | gorse 60, umbrella pine 25 (as Scots pine, for the pine), juniper bush 15 | 6, 8 |
| Lavender Field | jacaranda 60, oak with bees (v) 40 | jacaranda 50, olive with bees 30, oak with bees (v) 10, cypress 10; `winter_snow` off | 2, 7 |
| Lush Grassland | jungle bush (v) 63, oak bush (v) 30, jungle tree (v) 7 | banana 45, oak bush (v) 25, mango 20, small palm 10 | 2, 11c, 12 |
| Prairie | oak bush (v) 85, fancy oak (v) 15 | oak bush (v) 70, juniper column 15 (eastern red cedar), cottonwood 10, fancy oak (v) 5 | 2, 6 |
| Shrubland | oak bush (v) 100 | hawthorn bush 60, gorse 20, juniper bush 10, oak bush (v) 10 | 5b, 6 |
| Steppe | none | a new trees slot of 1-2 a chunk: sagebrush 80, juniper bush 20; nothing over two blocks | 6 |

### Wetlands (`docs/features/wetlands.md`)

| Biome | Now | After | Batches |
|---|---|---|---|
| Bog | maple bush 90, dead tree 10 | maple bush 45, tamarack 30, bog fir 15, dead tree 10 | 1 |
| Dead Swamp | dead tree 100 | dead snag 50, dead tree 45, fallen dead 5 | 1 |
| Lush Swamp | vine-hung oak (v) 70, willow 30 | vine-hung oak (v) 35, willow 30, bald cypress 25, maple 10 | 1, 4b |
| Swamp Woods | willow 70, vine-hung oak (v) 30 | willow 45, bald cypress 25, vine-hung oak (v) 20, azalea (v) 10 | 1, 4b |
| Bayou | willow 85, vine-hung oak (v) 15 | bald cypress 60, willow 25, live oak 15 | 2, 4b |
| Floodplain | oak bush (v) 80, oak (v) 20 | willow bush 70, cottonwood 30 | 1, 2 |
| Ghost Forest | dead tree 85, dark oak (v) 15 | dead snag 55, dead tree 30, swamp cedar 15 | 1 |
| Sludge Mire | dark oak (v) 56, fancy oak (v) 30, vine-hung oak (v) 14 | stays: its record is "a dense canopy of big oaks and dark oaks" | none |
| Lush River | oak bush (v) 100 | willow bush 70, willow 20, cottonwood 10 | 1, 2 |
| Fen | fir 70, dark oak (v) 30 | bog fir 50 ("short firs"), tamarack 35, willow bush 15 | 1 |
| Lake District | oak (v) 54, spruce (v) 40, fancy oak (v) 6 | oak (v) 44, fir 30 (for the spruce), larch 10, rowan 10, fancy oak (v) 6 | 1, 5 |
| Quagmire | none | stays: bare mud. Trees would blur it with the Dead Forest, which the client test already mistook for it at a region edge. | none |
| Marsh | none | stays: a marsh is grasses and reeds; a wooded one is a swamp | none |
| Wetland | spruce (v) 60, willow 40 | swamp cedar 50 (for the spruce), willow 40, elder 10 | 1, 5 |

### Warm and dry (`docs/features/warm-and-dry.md`)

| Biome | Now | After | Batches |
|---|---|---|---|
| Dryland | pine (v) 49, oak bush (v) 26, oak (v) 25 | pinyon-juniper country: pinyon 45, juniper 30, juniper bush 25 | 6, 8 |
| Xeric Shrubland | desert acacia (v) 100 | Mojave shrubland: desert acacia (v) 45, joshua tree 30, juniper 25 | 6, 8c |
| Jacaranda Glade | jacaranda 61, oak with bees (v) 17, cherry (v) 15, azalea (v) 7 | jacaranda 45, oak with bees (v) 17, big jacaranda 15, flame tree 8, cherry (v) 8, azalea (v) 7 | 2, 11 |
| Lush Desert | desert acacia (v) 70, oak bush (v) 30, which never places | desert acacia (v) 45, joshua tree 35, small palm 20; grass patches on its red sand | 2, 8c |
| Bone Flats | none | stays: its bone pillars stand in for trees | none |
| Dry River | none | 1 try a chunk on its dry banks: date palm 60, tamarisk 40 | 8b, 10 |
| Cold Desert | none | a lone juniper on its coarse-dirt patches, about one chunk in six | 6 |
| Scrubland | oak bush (v) 100 | olive bush 60, oak bush (v) 25, juniper 15 | 6, 7 |
| Lush Savanna | none | stays: a treeless poppy field by its record. The owner's call: one lone giant baobab every 6-8 chunks would make it read as a savanna. | none |
| Outback | desert acacia (v) 100 | desert acacia (v) 50, mallee 30, baobab 10, giant baobab 10 | 2, 10 |
| Oasis | palm 100 | palm 40, date palm 35, small palm 15, tamarisk 10 | 2, 8b, 10 |
| Wasteland | dead tree 100, at 0-1 tries a chunk | 1 try a chunk on its coarse dirt: dead tree 45, dead snag 40, tamarisk 15 | 1, 8b |
| Burnt Forest | dead tree 90, oak bush (v) 10 | dead snag 45, dead tree 40, oak bush (v) 10, fallen dead 5. Its record's "charred" trunks become grey snags unless the owner takes up the charred log (see Decisions). | 1 |
| Mediterranean Forest | cypress 48, oak (v) 30, oak bush (v) 12, dark oak (v) 10 | cypress 45, olive 25, umbrella pine 15, olive bush 15 | 7, 8 |
| Orchard | chestnut 60, oak with bees (v) 30, azalea (v) 10 | chestnut 40, apple (with bees) 25, pear 15, peach 10, azalea (v) 10 | 14 |

### Big trees and rainforests (`docs/features/big-trees-and-rainforests.md`)

| Biome | Now | After | Batches |
|---|---|---|---|
| Rainforest | mahogany 52, jungle bush (v) 17, giant mahogany 12, jungle tree (v) 12, mega jungle tree (v) 7 | mahogany 50, giant mahogany 12, rubber tree 12, banana 10, tree fern 9, buttressed mahogany 7 | 12 |
| Eucalyptus Forest | eucalyptus 52, big eucalyptus 30, oak bush (v) 18 | eucalyptus 47, big eucalyptus 25, wattle bush 15, giant eucalyptus 8, tree fern 5 | 2, 8, 12 |
| Tropics | palm 51, small palm 25, jungle bush (v) 15, azalea (v) 9 | palm 45, small palm 20, banana 15, flame tree 10, pandanus 10 (if the owner keeps it; else to the palm) | 11, 12 |
| Subtropics | azalea (v) 45, oak (v) 30, small palm 13, birch (v) 7, vine-hung oak (v) 5 | azalea (v) 40, live oak 30, small palm 15, orange 10, vine-hung oak (v) 5 | 2, 14 |
| Dense Forest | fancy oak (v) 57, oak (v) 30, dark oak (v) 7, oak bush (v) 6 | fancy oak (v) 44, oak (v) 30, walnut 10, dark oak (v) 7, holly 5, great oak 2, oak bush (v) 2 | 1, 6b, 13 |
| Redwood Forest | redwood 57, giant redwood 35, spruce (v) 6, fallen redwood 1 | redwood 52, giant redwood 35, fir 6 (for the spruce), mossy maple 5, fallen redwood 2 | 1 |
| Temperate Rainforest | fir 40, redwood 30, tall fir 14, vine-hung oak (v) 11, willow 4 | fir 38, redwood 30, tall fir 14, mossy maple 11 (for the vine oak), willow 4, tree fern 3 | 1, 12 |
| Woodland | oak (v) 65, fancy oak (v) 25, oak bush (v) 8, fallen oak (v) 2 | oak (v) 57, fancy oak (v) 25, walnut 4, holly 4, elder 4, hawthorn bush 4 (for the oak bush), fallen oak (v) 2 | 5, 5b, 6b, 13 |

### Mountains, coasts and volcanoes (`docs/features/mountains-coasts-and-volcanoes.md`)

| Biome | Now | After | Batches |
|---|---|---|---|
| Volcano | none | stays: a live cone is bare black rock and ash | none |
| Canyon | pine (v) 70, spruce bush (v) 30 | pinyon 40, juniper 35, juniper bush 25 | 6, 8 |
| Highland | none | almost treeless: a lone rowan or wind-bent hawthorn, about one chunk in four (rare extra) | 5, 5b |
| Basin | none | a lone bristlecone on gravel and stone, about one chunk in four (rare extra) | 13 |
| Shield | fir 50, pine (v) 30, spruce (v) 14, oak (v) 6 | fir 45, pine 25 (the windswept pine), spruce (v) 14, larch 8, aspen 8 (for the oak) | 1, 2 |
| Karst Pinnacles | pine (v) 65, spruce bush (v) 35 | pine 65 (the windswept pine), spruce bush (v) 20, osmanthus 15 (a stand-in on olive logs). Bamboo for its pandas is a separate fix. | 2, 11 |
| Hot Springs | pine (v) 70, spruce (v) 30 | pine 55 (the windswept pine, `jugcraft:pine_checked`), Japanese maple 30, pine (v) 15 (`minecraft:pine_checked`) | 2 |
| Ice Sheet | none | stays: sea ice has no trees | none |
| Ocean Trench | none | stays: the deep sea floor, kelp only | none |
| Gravel Beach | none | still no trees; a little driftwood | 11 |
| Dune Beach | none | still no trees; driftwood on the strand | 11 |
| Overgrown Beach | oak bush (v) 100 | sea grape 40, palm 20, small palm 20, pandanus 20 (if the owner keeps it; else to the sea grape) | 2, 11 |
| Flower Isle | oak bush (v) 100 | magnolia 55, oak bush (v) 30, apple 15 | 2, 4c |

### Wonders and caves (`docs/features/wonders-and-caves.md`)

| Biome | Now | After | Batches |
|---|---|---|---|
| Cinder Barrens | none | dead snags on its coarse-dirt patches, 1-2 tries a chunk, about one tree in eight chunks | 1 |
| Elder Vale | oak (v) 90, fancy oak (v) 10 | stays: "a vale of the old world" of plain vanilla oaks, on purpose | none |
| Frostlight Garden | fir 60, birch (v) 40 | fir 60, shimmer birch 40 | 4 |
| Gilded Shrubland | oak bush (v) 100, tinted gold | wattle bush 70, wattle 30 | 8 |
| Glimmer Grove | jacaranda 72, oak (v) 18, huge red mushroom (v) 10 | jacaranda 55, shimmer birch 20, big jacaranda 15, huge red mushroom (v) 10 | 2, 4 |
| Gloomweald | vanilla's dark-forest canopy, then dead tree 70 and spruce (v) 30 at 2-3 a chunk, mostly failing on the canopy | its own list in place of vanilla's, about 16 a chunk: dark oak (v) 45, yew 20, dead tree 10, dead snag 10, huge red mushroom (v) 8, huge brown mushroom (v) 7 | 3 |
| Glowcap Grotto | none (a cave) | huge glowcaps, 2-4 tries a chunk on its floors, as a cave extra | 9 |
| Hallowed Bog | willow 70, vine-hung oak (v) 30 | willow 70, young aspen 30 | 1 |
| Highsun Meadow | oak bush (v) 100, at 0-1 a chunk | stays: "a few small oaks" | none |
| Mycelial Jungle | jungle bush (v) 51, huge red mushroom (v) 30, huge brown mushroom (v) 10, oak (v) 9 | jungle bush (v) 45, huge red mushroom (v) 30, huge brown mushroom (v) 10, mahogany 8 (for the oak), huge glowcap 7 | 2, 9 |
| Shrine Springs | great oak 56, jungle bush (v) 30, oak (v) 14 | great oak 56, jungle bush (v) 30, banyan 14 | 11b |
| Snowpetal Grove | cherry (v) 75, birch (v) 25 | snowpetal cherry 75, aspen 25 | 1, 4 |
| Spider Nest | none (a cave) | stays: a dark cave whose identity is its webs | none |
| Starlit Wood | super birch with bees (v) 75, tall fir 25 | tall shimmer birch 45, super birch with bees (v) 30, tall fir 25 | 4 |
| Toadstool Field | none (vanilla's mushroom-island huge mushrooms) | plus about one huge glowcap a chunk on mycelium | 9 |
| Webwood | willow 58, dead tree 20, vine-hung oak (v) 16, birch (v) 6 | willow 57, dead tree 20, bald cypress 15, vine-hung oak (v) 8 | 4b |
| Wild Greens | none | stays: treeless by its record | none |

### The Nether (`docs/features/nether-biomes.md`)

| Biome | Now | After | Batches |
|---|---|---|---|
| Ashfall Wastes | none | stays: open by design. Its fire patches would burn a dead-wood stem and leave floating caps. | none |
| Blighted Sands | brambles | huge blight fungus on soul soil (about 1 a layer), bramble bushes on soul sand and soul soil (about 2 a layer) | 9 |
| Frost Rift | none | stays: glacier chasms; its ice is its look | none |
| Fungal Thicket | huge red and brown mushrooms (v), crimson fungi (v), glowcaps | plus huge glowcaps on nylium and mycelium (about 2 a layer) | 9 |
| Magma Fields | none | stays: fumarole fields kill plants; sulfur spikes and basalt give it height | none |
| Marrow Heap | bone spires | marrow fungus on its nether wart blocks (about 1 a layer) | 9 |
| Netherbrush | brambles, warped fungi (v) | bramble bushes on nylium (about 4 a layer) | 9 |
| Quartz Rift | none | stays: its quartz spires are its trees | none |
| Withered Hollow | none | huge blight fungus on blackstone (about 3 a layer), with no shroomlights | 9 |

### The End (`docs/features/end-biomes.md`)

| Biome | Now | After | Batches |
|---|---|---|---|
| Chorus Reef | chorus (v) | stays: the chorus stands where coral trees would | none |
| Ender Wilds | jacaranda 70, azalea (v) 30 | dragonblood 45, jacaranda 30, dragonblood shrub 20, huge glowcap 5 | 9, 9b |
| Outer Flats | none | a lone dragonblood on end stone, about one chunk in ten (rare extra) | 9b |
| Phantom Garden | pale oak (v) 100 | stays: vanilla's pale oak, pale moss and eyeblossoms are one set, kept whole | none |
| Rotted Expanse | dead tree 100 | dead tree 60, dead snag 30, fallen dead 10; huge blight fungus on its soul-soil patches as its own extra | 1, 9 |

### Alpine Spawn and Pixel Hollows

| Biome | Now | After | Batches |
|---|---|---|---|
| Alpine Spawn (`docs/features/alpine-spawn.md`) | larch 60, spruce (v) 40 | larch 50, stone pine 20, spruce (v) 20, juniper bush 10 | 8 |
| Pixel Hollows | none (a cave) | stays: a cave biome with no light or soil for trees | none |

## Rejected ideas

| Idea | Why not |
|---|---|
| Hemlock | The existing fir covers it: the same spruce placer at nearly the same size. Fir replaces vanilla spruce in the Maple Woods, Redwood Forest and Lake District instead. A hemlock wood's buff planks would sit on oak's and chestnut's hue. |
| A walnut wood set (owner's black walnut, row 2) | **Reversed by the owner's update** (see Decisions). It was rejected because the painting's hue and lightness sat on vanilla spruce planks'. The walnut now has its own wood, the black walnut altered to #5a4b41: 8.3 degrees of hue and ΔE 18.0 from spruce, and 10.1 from the cedar. |
| A juniper wood set (painting 20, #86434e) | **Reversed by the owner's update.** Junipers are not Jugcraft's *Cupressus*, so the juniper has its own wood from painting 20, altered to #8b4654. It is 4.7 degrees of hue from the jacaranda's mauve but twice as saturated, and ΔE 17.4 away. |
| Glimmerwood | A new blue-violet wood for essentially one biome. The shimmer birch serves the Glimmer Grove. Painting 4 (#7078b1) stays reserved for when the magic branch needs a wood for wand hafts and workshop furniture. |
| A fire-proof Nether "blight" wood set | Dead-wood stems under a new wart cap serve three biomes, none of which spreads fire. A Nether set would need crimson-style registrations and IDs that become permanent. Revisit it with another of the owner's paintings if they want a third Nether building wood (painting 18, once suggested, is now the persimmon's); the Ashfall Wastes would wait for that. |
| Dragonblood and baobab wood sets | **Reversed by the owner's update**, which allows altering a painting. Neither is a palm, so each has its own wood: the dragonblood from painting 12 as painted (an otherworld teal), the baobab from painting 21 recoloured (a pale pinkish buff), with a smooth copper-grey bark. |
| Ebony or wenge | A near-black exotic for a rainforest that already gains four trees. The owner's wenge (row 3) is now the source of the yucca's wood, altered to a khaki-grey; an ebony would need another painting. |
| Elm, shagbark hickory, London plane (owner's banked rows 4, 5 and 1) | No surveyed biome needs these trees now. Rows 4 and 5 sit close to each other and near vanilla spruce; London plane's wood is close to oak planks. Their paintings are put to use, altered, for other trees: London plane (row 1) for the magnolia and elm (row 4) for the mango. Shagbark hickory (row 5) stays banked for a future hickory. Row 0, western red cedar, became the swamp cedar's own wood in batch 1, at the owner's word. |
| Osage orange | Serves essentially one biome, and its golden wood needs a painting the owner has not given. |
| Persimmon on black wood (painting 18) | **Reversed by the owner's update.** The persimmon is the ebony genus, not an oak, so it has its own wood from painting 18, warmed to #3d3733. A bark-only charred log could still take its bark from painting 18. |
| Bay laurel | A dense, glossy oval evergreen on oak logs, too close to the osmanthus and the orange tree. |
| Lehua | The Volcano stays treeless, and its scarlet blossom duplicates the flame tree. |
| A pale oak shrub | The Phantom Garden is vanilla's pale set kept whole; a bush of the same blocks adds little. |
| A charred wood set | It would duplicate dead wood. A single charred log block is offered to the owner instead (see Decisions). |
| Cork oak, mesquite, palo verde, beech, alder, water tupelo, river birch, sycamore, linden, almond | Their wood would land on an existing colour: cork oak on vanilla oak, mesquite between cypress and redwood, beech on maple, alder on maple and jacaranda, tupelo on chestnut and willow, and birch, sycamore and linden on fir, palm and aspen. Palo verde's green bark is the eucalyptus's, and almond's cycle is the apple's. River birch's peeling curls would also break the no-bands rule. A Scots pine wood was once on this list too; under rule 9 a Scots pine shares the pine wood, as the umbrella pine standing for it in the Heathland does. |
| Black spruce, paper or dwarf birch, sumac, hazel, saguaro, a separate lemon, full trees in the Tundra | Covered elsewhere or out of scope. The bog fir and tamarack fill black spruce's role; vanilla birch and the aspen give white bark; sumac's red autumn overlaps the maple bush; hazel belongs to a later woodland understory; saguaro is a cactus, a plant feature; the lemon is the orange with yellow fruit in slice 4; the Tundra stays treeless, with scrub only. |
| A hawthorn wood set (#c2b2ad, the aspen's painting 9 recoloured) | **Cut on review.** It passed rule 3's hue reading only on 0.003 of saturation, beside cherry and pale oak, no free slot lay near it, and its shaded side matched the bald cypress. The hawthorn grows the fruitwood. |
| Separate rowan and pear wood sets (#be8784 and #9f7e77) | **Replaced on review by the fruitwood**, the owner's call. The two sat CIEDE2000 8.2 apart, the pear 5.7 from the rowan's painting, and the rowan 3.6 degrees of hue from cherry. Two re-placed sets (#ba8688 and #9f7e73) remain the owner's alternative. |
| The yew at #633e32 | ΔE 1.3 from the painting: it cleared the cedar on the CIE76 figure without changing the look. The visible alternative is #553731. |
| The elder's short chestnut trunk, and the sea grape's small tree form | Each showed a trunk of a wood the tree is not. The elder is a shrub on a hidden stem; the sea grape keeps only its shrub. |
| Merged names: bog_fir, big_aspen, crooked_aspen, charred_snag, olive_bush, ancient_olive, juniper_bush, golden_wattle, Scots pine, pinyon, umbrella pine, hawthorn_bush | Shapes of trees above, built with them: stunted_fir, cottonwood, young_aspen, dead_snag, olive, juniper, wattle, stone_pine and hawthorn. |
| Bald cypress in the Sludge Mire | Its record is a dense canopy of big oaks and dark oaks, and lime needles would be the brightest thing in its murk. |
| Elder in the Elder Vale and the Highsun Meadow | The Vale's point is plain vanilla oaks. The Meadow's 0-1 tries a chunk would hardly show it. |
| Baobab in the Dryland | The Dryland becomes one coherent American pinyon-juniper country. |
| Umbrella pine on the Dune Beach | An all-sand beach with no sand survival check, and its record says "no trees". |
| Olive in the Orchard and the Xeric Shrubland | The Orchard has snowy winters; the Xeric Shrubland becomes Mojave shrubland. |
| Yew in the Dense Forest | It would stand beside the holly as a second dark evergreen with red berries. |
| Several species in one low-density biome | The Field, Lake District, Muskeg, Outback and Heathland were trimmed to three to five kinds, so a species reads as the biome's own. The rowan left the Field, swamp cedar and willow left the Lake District, the willow bush left the Muskeg, the wattle left the Outback, and the juniper column and most of the oak bush left the Heathland. |
| Banyan figs, and banana, fig, walnut and candied-peel candies | No item without a consumer, and no Candy Kettle flavour without an effect of its own. |

## Decisions

The first three reviews raised 98 points between them, and the two reviews of the own-wood revision 30 more. Where two
reviews raised the same point, it is answered once, in review order. "Accepted" means the roster above already includes
the change. Batch 1 as built, the owner's own-wood update and the review of that update come first; where a later
decision changed an earlier one, that row says so. The choices still left to the owner are gathered at the end of
"Review of the own-wood revision".

### Batch 1 as built

| Point | Decision |
|---|---|
| The swamp cedar was a cypress shape | **Changed by the owner.** It is its own wood, cedar, from their painted western red cedar, nudged off vanilla spruce and the cypress by the least change that separates it, with its bark darkened alike (see its record). The shape is `cedar`, which its sapling grows. |
| The tamarack's pine height 4 would crown half the tamaracks 7 wide | Built with height 3: 5 wide or less, by our reading of vanilla's pine placer. To check in the CI screenshots. |
| "Tamaracks lie fallen too", but no biome placed a fallen larch | The Muskeg's fallen 5 becomes fallen dead 3 and fallen larch 2. |
| The Orchard places chestnut trees under the biomes switch | The chestnut's switch list is agriculture or biomes, so rule 4's check passes for every placed tree, not only the shapes. |
| The Gloomweald's 10% snag | Left to batch 3, as its "Biomes by biome" row says: today's 2-3 tries mostly fail on vanilla's canopy. |
| A later batch's share before its batch | It stays with the tree it will replace: oak in the Seasonal Forest, Snowy Forest, Lake District, Dead Forest and Floodplain; the vine-hung oak in the Lush Swamp; the willow in the Swamp Woods; the oak bush in the Lush River and Dense Forest; the fancy oak in the Dense Forest; the maple bush in the Tundra; the spruce in the Wetland; the pine in the Shield; the fir in the Temperate Rainforest; the dead tree in the Wasteland; the cherry in the Snowpetal Grove. |
| The owner's verdict on the built cedar | "Cedar looks sooo good!!" (6 October 2026). The cedar stands as built, #725543, and every new wood is checked against it. |

### Owner's update: own woods

On 5 October 2026 the owner said: "We are ok for making new wood and logs for a bunch as well, if we need them just
alter the ones we have but make sure they are made the same way so they fit the world!!! Unless they are supposed to
be the same wood." Every tree on the roster was judged against that, in four groups by batch, and the groups' new
woods were then merged and checked together by script. Batch 1 was being built at the time and is kept as built: its
only new wood is the cedar.

| Point | Decision |
|---|---|
| The principle | Rule 9: a botanically different wood that players would fell gets its own wood set; the same wood is kept; one genus, or one timber, shares one wood; shrubs on hidden stems, special trunks and fungi get none; a trunk of a wood the tree is not is a stand-in and the owner's call. |
| Own wood (16 woods, 20 trees) | pine (the tree `pine`, the windswept pine; the stone pine with its umbrella and pinyon forms; the bristlecone), yew, bald cypress, magnolia, fruitwood (the rowan, the hawthorn and the pear), persimmon, juniper, holly, olive, yucca (the Joshua tree), tamarisk, dragonblood, baobab, mango, fig (the banyan) and walnut. The yew and the olive were already planned; the other 14 are new to the roster. Revised on review: the hawthorn and the pear grow the rowan's fruitwood, and the banyan's wood is named fig. |
| Same wood, kept | cottonwood on aspen (both *Populus*); japanese_maple on maple; live_oak on oak (v); big_jacaranda; mallee and giant_eucalyptus on eucalyptus; snowpetal_cherry and peach on cherry (v); wattle on acacia (v); date_palm on palm (both true palms, the family the palm wood stands for); buttressed_mahogany; driftwood on stripped dead logs; and batch 1's shapes. The pandanus, once listed here, is a stand-in (see the review). |
| Moved to their true wood | shimmer_birch from aspen to vanilla birch (*Betula*); stone_pine and bristlecone to pine. The flame tree (from jacaranda to acacia) and the osmanthus (from vanilla oak to olive) moved to their nearest relatives, which on review are stand-ins and the owner's call. The cottonwood's planned move to willow is dropped: willow is another genus. |
| No wood set | elder, gorse and sea_grape (hidden oak), sagebrush (hidden dead), blight_fungus (a dead stem), bramble_bush (hidden crimson), huge_glowcap, marrow_fungus, banana and tree_fern (their own trunk blocks), rubber_tree (slice 5's `rubber_log`, which saws into jungle planks: a stand-in). |
| Orange and lemon | Kept on vanilla oak as a stand-in, as the apple is. **Owner's call, pending** (see the orange). |
| How each colour was found | Each new wood starts from one of the owner's paintings: one not yet used where it fits, otherwise one altered by the least change that fits. The bar, this roster's reading of rule 3 for the owner to approve: ΔE 12 or more from every vanilla wood; a vanilla wood within ΔE 15 at least 8 degrees of HLS hue away when both have saturation 0.15 or more; ΔE 10 or more from every other Jugcraft wood, the cedar included. This reproduces both of the roster's earlier wood verdicts: the olive's painting 3 failed at ΔE 10.6 from bamboo on 4 degrees of hue, and the black walnut failed on spruce's hue (the 8 degrees is the roster's own walnut condition). Between modded woods, 10 is more than one step of `ramp()` at mid lightness (about ΔE 6.5-9), so no wood's base tone lands on another's neighbouring tone, and it is the bar batch 1 used for the cedar (ΔE 10.0 from vanilla spruce, 10.6 from the cypress). A flat 12 throughout was tried and fails: all the woods sit in a narrow band of low-saturation tans, greys and pinks, and holding 12 pushed the pine to a greenish khaki and the bald cypress to a near-neutral grey without converging. Added on review: CIEDE2000 as a second gate, for the owner's eye (rule 9). |
| Paintings | One new wood to a painting. Unused paintings: 3 olive, 6 pine, 7 fruitwood, 8 fig, 12 dragonblood, 18 persimmon, 20 juniper; second set 2:1 magnolia, 2:2 walnut, 2:3 yucca, 2:4 mango, 2:7 yew. Four recolour a current wood's painting, as the owner allowed ("just alter the ones we have"): the bald cypress from the dead wood's 23, the holly from the palm's 10, the baobab from the eucalyptus's 21 and the tamarisk from the jacaranda's 2:6, each ending ΔE 10.9 or more from that wood. Paintings 9 and 14, once recoloured for the hawthorn and the pear, are no longer used. Using only unused paintings still fails: of the 17 left once painting 4 is held for a magic wood, three are greens (5, 13 and 17) and one is painting 3's twin (19), which leaves 13 for 16 woods, and the greens would need recolouring by ΔE 26-33, which makes the source meaningless. Still free: 4 (held for a magic wood), 5, 13, 17, 19 and 2:5 (shagbark hickory, banked). Where a wood moved far, its credit is nominal and the owner judges it by eye: the yucca (26 degrees of hue from the wenge), the holly (28 from the palm's painting) and the magnolia (20 from the London plane). Some woods now sit nearer another painting than their own: the walnut (the wenge, 5.3), the yucca (the black walnut, 10.1), the mango, holly and baobab (painting 6) and the tamarisk (painting 7). The preview shows each wood beside its painting. |
| Batch 1 as built | The merged list was checked against the cedar's painting (#84654d), but batch 1 built the cedar darker, #725543, which the owner has since approved. Rechecked against that: the walnut, merged at #665347, was only ΔE 6.5 from the built cedar at the same hue, so it moves ΔE 4.3 to **#5a4b41** (10.1 from the cedar, and 8.3 degrees from spruce, the roster's own walnut condition). The yew, the owner's painting kept as painted, is ΔE 8.8 from the built cedar (see the yew). Every other wood is unchanged by it; after the walnut and the yew, the nearest to the cedar are the fig (12.3) and the yucca (13.1). |
| The result | Every new wood meets the CIE76 bar except the yew beside the cedar, which is the owner's call. The lowest ΔE to a vanilla wood is 12.6 (baobab and cherry). The lowest to a modded wood is 10.1 (pine and mango, fig and yucca, walnut and cedar). By CIEDE2000, 11 pairs with a new wood are closer than vanilla's oak and jungle (9.3), and none of them is pink: the tans (chestnut and pine 7.3, oak and mango 7.7, eucalyptus and pine 7.8, pine and mango 8.9, mango and fig 9.0), the browns squeezed by the darkened cedar (cedar and walnut 7.4, cedar and yew 7.6, mangrove and yew 7.8, persimmon and walnut 8.7, yew and walnut 8.9), and bamboo and olive (9.3). They go to the owner as wall pairs; by eye the pine and the mango read apart from the chestnut and oak by their greyness. Every lightness and saturation is within vanilla's (lightness 0.873, saturation 0.541), and contrast is `ramp()`'s fixed factors, the same for every wood. |
| One borderline | **Superseded on review.** The hawthorn passed rule 3's hue reading by 0.003 of saturation; its set is cut, and it grows the fruitwood. |
| Barks | As each group set them, from the owner's painted barks where there is one (yew, walnut), with three redrawn on review: the holly's (plated, not marked), the persimmon's (slate-black) and the yucca's (pale straw). The closest pairs are all of different kinds and share no biome: juniper and tamarisk ΔE 3.8, mahogany and bald cypress 4.3. The closest of one kind is fir and yew, 6.3. The yucca's shag is now ΔE 15.4 from the palm's bark in the Lush Desert, where it was 4.0. The pine's and the larch's plated orange-brown barks, ΔE 9.8 apart, share the Shield and Alpine Spawn; their needles and silhouettes differ, to be checked in the CI shots. Every new bark's mid tone is ΔE 8.5 or more from its wood: the lowest are the fig's pale bark (8.5, lighter than its planks, as the aspen's is at 7.0) and the owner's painted walnut (9.6) and yew (10.0). |
| New leaves | `pine_needles` for the tree `pine`, in place of vanilla's spruce needles: #193a2c #224f3d #2c654d #357b5e #45896d #53a583, a soft blue-green at hue 155, ΔE 13.1 from the fir's needles and 12.5 from the stone pine's. |
| Block IDs | 208 new: 16 wood sets × 9 = 144, plus 32 trees with their own leaves and sapling × 2 = 64. The roster had 80 before (2 wood sets and 31 leaf-and-sapling pairs); the update adds 14 wood sets and the pine's needles and sapling. The fruitwood saves 18 against three Maleae sets, and cutting the fig would save 9 more. Not counted: batch 1's cedar, the rubber tree (slice 5) and about 10 blocks that are not wood (pod, date cluster, glowcap cap, blight wart, small blight fungus, bramble leaves, banana stem and bunch, tree-fern trunk, Holly Wreath). |
| Batch 4 | Still needed, by 20 trees (listed under batch 4), and still fourth. It loses its two wood trees (to 4b and 4c), the root placer (to 4b) and the leaf drawing options (to batch 3), so it is the shared change and two leaf blocks. The bare-twig ramps for trees on vanilla logs are dropped: no deciduous tree is left on vanilla logs. |
| Build order | One new wood set to a PR, as batches 1, 2, 3 and 7 already are. Batches 4, 5, 6, 8, 9 and 11 split into 4b, 4c, 5b, 5c, 6b, 8b, 8c, 9b, 11b and 11c. Dependencies are in the build order table. |
| Names | The windswept pine is the tree `pine` (`pine_needles`, `pine_sapling` and the shape `pine`), so batch 2 needs no tree-and-wood split. A wood is named for its timber (its genus, or for a shared wood its trade name), so the Joshua tree grows yucca logs, the banyan fig logs, and the rowan, hawthorn and pear fruitwood logs, each keeping its own key through batch 4's `wood` field. **Owner's call**, since IDs are permanent; the alternative is to name each wood for the tree that brings it. |
| Earlier decisions | The Rejected ideas rows on the walnut, juniper, dragonblood and baobab wood sets and on the persimmon's painting 18 are reversed, and the decisions on the walnut, the olive's fallback, the cottonwood's move and the dark oak and pale oak route are marked superseded where they stand. |
| Preview | Two sheets, drawn by `wood_style.py`'s own `bark`, `log_top`, `stripped_side` and `planks`, with its tables extended in memory only, and not committed. The first shows every new wood beside the owner's painting it is credited to (bark, log end, stripped side and planks), then the 14 current woods' planks. The second shows every close pair as a wall of planks, lit and at the x0.8 side shade, then the browns, pinks, tans and End woods together, with the yew's alternative beside the cedar. The olive is shown plain until its figure is drawn. Nothing was built, run in game or play-tested, and every colour still needs the owner's approval beside vanilla's woods (rule 3). |

### Review of the own-wood revision

Two reviews checked the revision on 6 October 2026: one for buildability, against the code on `claude/trees-batch-1`,
and one for art and fit, by script and by eye. Every colour figure was recomputed by script with CIE76 and CIEDE2000,
reading `tools/wood_style.py` and writing nothing in the repository. The owner's latest message, "Cedar looks sooo
good!!", settles the built cedar, #725543, and every figure here is measured against it.

**The buildability review.**

| Point | Decision |
|---|---|
| The pine's sapling grew a shape named `windswept_pine`, against the one-name pattern for which batch 1 renamed `swamp_cedar` to `cedar` | Accepted. The shape is `pine`, the windswept pine, and the Biomes tables name it so. Its placed feature, `jugcraft:pine_checked`, stands beside vanilla's `minecraft:pine_checked` in the Hot Springs. |
| The pandanus (not a palm), the flame tree (the same subfamily as acacia) and the osmanthus (the same tribe as olive) were filed as the same or true wood | Accepted. All three are stand-ins on their nearest relatives, and each is an owner's call. Dropping the pandanus is recommended: its only merit was costing no blocks. Rule 9 now names stand-ins as a class. |
| The elder showed a chestnut trunk, and the sea grape's tree form an oak one | Accepted. The elder is a shrub round one hidden oak stem, as the gorse is, with twigs of its own; the sea grape's tree form is dropped. |
| Batch 6 could not place the juniper bush in Alpine Spawn, whose trees are a fixed larch-or-spruce selector until batch 8 | Accepted. The juniper bush joins Alpine Spawn in batch 8, with the picks list, and the juniper's switch list gains alpine_spawn there. Its section states its switch list. |
| The yew needs batch 4's gap threshold, so batch 3 was not independent | Accepted. The leaf drawing options land in batch 3, and batches 4b, 4c, 8b, 9b, 10 and 11c no longer wait for batch 4. |
| The bristlecone should use `pine_needles`, not the stone pine's | Accepted. No pine nuts on a pine that bears none, no stone pine saplings from it, and no batch-4 `tree` field. Batch 13 depends on 2 and 5 only. |
| Batch 10 should not wait for the tamarisk | Accepted. Whichever of 8b and 10 lands first gives the Dry River its tries. |
| Batches 8, 11 and 14 were too large, and batch 5 borderline | Accepted. The Joshua tree and the yucca wood are batch 8c, and the mango batch 11c. Batch 14 no longer adds a wood, since the pear grows the fruitwood, and the pear's logs are agreed with the agriculture branch. Batch 5 stays whole: it has one wood set, and the elder is now a plain shrub. |
| Wood naming was inconsistent (the yucca named for its genus, the banyan for its tree) and depended on build order | Accepted. One rule, under "How to read this": a wood is named for its timber (its genus, an invented tree's own name, or a shared trade name), and a tree keeps its own key on it through batch 4's `wood` field, whatever its batch. The banyan's wood is therefore `fig`. **Owner's call**, since IDs are permanent. |
| Resolve the yew now: adopt #633e32, or record the owner's acceptance | Accepted in part. It is settled before batch 3 by the owner's choice between the painting and a visible move, #553731; #633e32 is dropped (see the art review). |
| The hawthorn passed the hue clause by a hair: nudge it to hue 13 or below | **Declined.** Hue 13 moves it toward cherry (hue 6.7). Its set is cut instead (see the art review). |
| Rule 3 asks a new wood to differ in hue from the nearest vanilla wood; several pass only on ΔE above 15; seven of 18 woods were pinks; four modded pairs sat at 10.1 | Accepted. The bar is put to the owner as a reading of rule 3, with CIEDE2000 as a second gate, and batch 2 adds it to `NATURAL_TEXTURES.md`. Woods that pass on ΔE although near a vanilla hue: the mango, 2.1 degrees from oak (ΔE 16.1); the pine, 4.4 from birch (18.8); the walnut and persimmon, 5.4 from dark oak (17.5 and 17.4); the tamarisk, 6.5 from crimson (25.2); the fig, 6.6 from spruce (19.1); and the fruitwood, 1.3 from cherry (20.4), though its nearest vanilla wood is jungle, 17.9 degrees away. The pinks are now four (the fruitwood, baobab, tamarisk and juniper), with the bald cypress a grey of faint pink cast, where vanilla has cherry. Three modded pairs sit at 10.1. |
| The colour files and the preview were stale (the walnut at #665347) | Accepted (both reviews). The merged list and colour files are regenerated from this roster's colours, a script checks that the roster carries each final colour, and the preview is redrawn. |
| The tamarisk's "only wood a player finds" was false, and the Wasteland's tries were done in batch 1 | Accepted. Reworded; it is a candidate to cut, after the fig and the mango. Batch 8b no longer claims the Wasteland's tries. |
| The orange's case rested on the holly's slot; the rubber tree is a stand-in | Accepted. The orange row is pending, and the trade-off is put to the owner: by CIEDE2000 the citrus cream fails beside the palm, eucalyptus, birch and chestnut as well as the holly. The rubber tree is named a stand-in, for slice 5 to settle. |
| Wording and botany | Accepted. 16 new woods, 14 of them new to the roster; the junipers are one of about 30 genera of the cypress family; rule 9 keeps the palm as a family-level wood; the persimmon's golf clubs were its pale sapwood, and its charcoal planks take the ebony genus's heartwood look; the list of trees on borrowed woods is complete (under Buildability and rules); the juniper's switch list is stated. |

**The art and fit review.**

| Point | Decision |
|---|---|
| The hawthorn's borderline was misreported: the nudge went the wrong way, pale oak was left out, no free slot lay near it, and its shaded side matched the bald cypress | Accepted. Its set is cut, and it grows the fruitwood, its Maleae relative's, not vanilla oak. |
| The pink-grey group (bald cypress, baobab, hawthorn, pear, rowan) was tighter than vanilla's closest pair by CIEDE2000, and the rowan-pear clash was settled the wrong way, onto cherry's hue | Accepted, with option (a) as the default: one fruitwood on painting 7 (#ab7d76, ΔE 0.8 from it), brought by the rowan and grown by the hawthorn and the pear, with the bald cypress nudged ΔE 1.8 to #9c9391. No pink pair is now under CIEDE2000 9.8 (fruitwood and tamarisk 9.8, bald cypress and baobab 9.9), and 18 IDs are saved. Option (b), recomputed by script: a rowan wood at #ba8688 (painting 7 moved 7.3, 9.0 degrees off cherry) and a pear wood at #9f7e73 (painting 14 moved 10.6), CIEDE2000 9.5 apart and 9.4 or more from every wood, with the hawthorn on the rowan's logs. Both land nearest painting 7, though, so the pear would in truth be that painting's second wood. **Owner's call.** |
| The holly's marked bark is the aspen's trait, and rule 5 keeps it for birch-like trees | Accepted. Plated, a low-contrast grey with a faint green cast. The rowan keeps the marked kind, as the fruitwood's bark. |
| CIEDE2000 as a second gate | Accepted, as a gate for the owner's eye rather than a refusal. 11 pairs remain under vanilla's oak-jungle 9.3, in the tans, the browns and bamboo-olive, and each is shown as a wall pair. |
| The mango is the most crowded new wood | Accepted. Offered as a weathered oak, and the next set to cut after the fig. |
| The banyan's planks are generic, and it grows in one biome | Accepted. Its fig wood is the first to cut if sets are trimmed (9 IDs). |
| The yew's #633e32 only changed the figure; "never share a biome" excuses barks, not planks | Accepted. #633e32 is dropped, and the yew stays as painted with the exception owned, beside a visible alternative, #553731. The biome reason is dropped for planks everywhere (the yew and cedar, and the bald cypress's old 10.1 and 10.6, now gone); it stays only for barks. |
| The walnut is a cool grey-brown, not a dark chocolate | Accepted. Described so, and shown beside the cedar, yew and persimmon. |
| The persimmon's and yucca's barks barely change when stripped (mid tone ΔE 5.1) | Accepted. The persimmon's bark is a cool slate-black (10.8 from its wood), and the yucca's shag a pale straw-grey (18.6, and 15.4 from the palm's bark beside it). The walnut (9.6) and the yew (10.0) keep the owner's painted barks. |
| The dragonblood reads as pastel ice | Accepted as an owner's-eye check beside warped and purpur, with painting 12 darkened to #508295 as the alternative. |
| The owner's sheet should show each source painting and the close pairs as walls with a side face, and the olive's figure | Accepted. Both sheets are drawn (see Preview), and woods whose credit is nominal or crossed are marked. The olive is previewed again once its figure is drawn. |
| The pine's and larch's barks are close and share two biomes | Accepted. A check in the CI shots; their needles and silhouettes differ. |
| Keep the pine, yew, magnolia, yucca, persimmon, juniper, holly, olive, tamarisk and baobab | Agreed, with the barks above redrawn. |

**The owner's calls still open.** The colours, as previewed, were approved on 6 October 2026 ("The new wood looks so
so so good"), with the yew as painted and one fruitwood; the rest keep their defaults until the owner says otherwise.

| Call | Before | This roster's default | The alternative |
|---|---|---|---|
| Rule 3's reading: the CIE76 bar, with CIEDE2000 pairs judged by eye | batch 2 | as written in rule 9 | a bar of the owner's own |
| The yew's colour | batch 3, for Halloween (20 October) | as painted, #654135 (ΔE 8.8 from the cedar) | #553731, a visible move (ΔE 14.0 from the cedar) |
| Wood names | batch 4 | named for the timber: `yucca`, `fig`, `fruitwood` | named for the tree that brings each: `joshua_tree`, `banyan`, `rowan` |
| The fruitwood | batch 5 | one fruitwood for the rowan, hawthorn and pear (#ab7d76) | a rowan wood (#ba8688) and a pear wood (#9f7e73), with the hawthorn on the rowan's |
| The dragonblood's paleness | batch 9b | as painted, #689db2 | #508295, darker |
| Stand-ins | their batches | the flame tree on acacia, the osmanthus on olive, the orange on oak, the rubber tree on jungle planks; the pandanus dropped | own woods where a colour exists (the flame tree's #ba974f), hidden stems, or no tree |
| Sets to cut if the roster must shrink | any time | none cut | the fig first, then the mango, then the tamarisk |

### Buildability and rules

| Point | Decision |
|---|---|
| Borrowed woods break the per-wood feature switch (tamarack, elder, stone pine, pear) | Accepted. Any-of switch lists with a `check_mod_data` rule, settled in batch 1 before any borrowed larch ships. Since the review, every tree on a Jugcraft wood named otherwise is covered by the same check: the tamarack (larch), the stone pine and bristlecone (pine), the sagebrush, blight fungus and driftwood (dead), the rowan, hawthorn and pear (fruitwood), the Joshua tree (yucca), the osmanthus (olive) and the banyan (fig). The elder now stands on a hidden vanilla oak stem, which needs no switch. |
| Batch 2's shared change covered only the tools side | Accepted. Batch 4 now covers the Java split of `registerTree`, switch, names, tree-farm output and giant checks keyed on the log, shrub log counts and a phantom-wood-set check. A design note comes first. Since the owner's update no giant stands on another wood's logs (the baobab has its own), so the giant check by log is no longer needed. |
| Joshua tree branch length below the codec minimum | Accepted. Constant 2, and a bounds check for every cherry-placer tree. |
| Huge glowcap in the Glowcap Grotto through the trees field | Accepted. Placed as a cave extra. |
| Glowcap bone-meal loop | Accepted. Caps drop about 7 glowcaps per giant, the cap block does not compost, and the glowcap keeps its value. The arithmetic goes in the record. |
| Banana and tree-fern leaves would decay | Accepted. Their stems, and `rubber_log`, join `#minecraft:logs` but not `#minecraft:logs_that_burn`. |
| Rubber tree duplicates agriculture slice 5 | Accepted. Slice 5 builds the tree and tap; this roster only places it. |
| Holly sprigs have no consumer | Accepted. The Holly Wreath ships in the same PR. |
| Walnut fails rule 3 | Accepted then. Superseded by the owner's update: the walnut has its own wood, the black walnut altered to pass. |
| Olive not checked against bamboo | Accepted. Bamboo, crimson and warped join its check and are proposed for rule 3. Since the owner's update the olive is nudged to #9c9951, 8.8 degrees of hue from bamboo, and the willow fallback is dropped. |
| The Field loses its bees | Accepted. Vanilla's oak with bees stays at 5%, and the rowan is no longer in the Field. |
| No filter for "firs above y 90" | Accepted. A plain 4% fir pick. |
| The Flower Isle apple tree depends on batch 9 | Accepted. `apple_tree_checked` is created in batch 2. The record notes that the wild patch already reaches the isle. |
| The blight fungus cannot reproduce | Accepted. Wart blocks drop the small fungus at about 5%. The sagebrush is credited with making dead wood renewable first. |
| Driftwood cannot skip vanilla's stump | Accepted. A small custom feature with stripped logs and no stump; `FALLEN` is not restructured. |
| Too many new Candy Kettle flavours | Accepted. Rowan and elder join the berry tag, osmanthus takes luck, orange takes an unused effect, and the rest are dropped. |
| Always-ticking fruit leaves | Accepted. Tree-grown leaves with air below only; evergreens tick only while unripe; a budget in each record. |
| Batches too large for one focused PR | Accepted. 14 batches, with design notes before batches 4 and 5. |
| The Lush Desert defect waits for batch 4 | Accepted. Fixed in batch 2 with the small palm and grass patches. |
| Juniper "Cooking Pot seasoning" does not exist | Accepted. An alternative Mulling Spices recipe instead. |
| Peach "Cooking Pot pies" | Accepted. Hearth Oven fillings, appended to the end of the list and enum. |
| Persimmon's uniqueness claim is false | Accepted. Reworded. |
| Leaf IDs that take other trees' names | Accepted. The naming rule; `elder`, `banyan_leaves`, `snowpetal_cherry_leaves`; no fig item. |
| Yew's Halloween case sat in batch 6 | Accepted. Moved to batch 3; it needs no shared change. |
| Dragonblood on sand and gravel in the Outer Flats | Accepted. End stone only. |
| Yew as a cauldron reagent | Accepted. Dropped. |
| Two different "Spanish mosses" in the Bayou | Accepted. The live oak hangs shroud moss. |
| Root placer under a 2×2 trunk | Accepted. A prototype gate, with a plain giant as the fallback. |
| Olive texture breaks rules 2 and 4 | Accepted. The figure is drawn from the wood's own ramp, with a one-pixel wander. |
| Holly berries never show with seasons off | Accepted. A schedule, plus berries by position in the green state. |
| Persimmon and holly give a cheap route to dark oak and pale oak logs | **Rejected.** The tree farm already turns one dark oak or pale oak sapling into six logs, and dark oak is common. Moot since the owner's update: both now grow their own woods. |
| Biome records would contradict the new picks | Accepted. Each batch updates them; files are named in "Biomes by biome". |

### Art and fit with vanilla

| Point | Decision |
|---|---|
| Windswept pine copies vanilla acacia's placers | Accepted. Cherry trunk with radius-1 acacia pads. |
| Osmanthus is the orange and apple tree | Accepted. A multi-stemmed shape and cream-gold flowers. |
| Persimmon's autumn is maple orange and swallows its fruit | Accepted. Wine-maroon, ΔE 15 or more from every maple look. |
| The wattle bush is the Gilded Shrubland's tinted oak bush | Accepted. The oak bush is removed there, and phyllodes cover 35% or more. |
| The banana is the small palm's silhouette | Accepted. A drooping cherry crown. |
| Japanese maple is an acacia silhouette | Accepted. A low double dome. |
| One stunted fir cannot be both forms, and duplicates spruce | Accepted. Two shapes, and it replaces the spruce in the Coniferous Forest. |
| Subalpine fir is the cypress's column | Accepted. A visible bole and a flared foot. Proposed note for `NATURAL_TEXTURES.md`: fir and cypress never share a biome while their needles are this close. |
| Swamp cedar hides its bark | Accepted. trunk_height [3, 5]. |
| Holly is a small fir in summer | Accepted. An upright oval crown and a yellower glossy ramp; the yew is out of the Dense Forest. |
| Hawthorn's green is the apple's | Accepted. A shifted ramp (ΔE 19 from the apple), unripe haws, oak bush 10% or less. |
| Rowan reads as a small maple | Accepted. Hue 78, and an apricot autumn ΔE 17 or more from every maple look. |
| Juniper's hue is the eucalyptus's | Accepted. Hue 187, saturation 0.17. |
| Olive bark and wood break the manner rules | Accepted. |
| Olive wood is near bamboo | Accepted. See the olive's check. |
| Mediterranean greys are too alike | Accepted. A silver olive, stone pine ΔE 12 from the cypress, a fresher joshua green, and no olive bush with the sagebrush. |
| ancient_olive is vanilla dark oak's silhouette | Accepted. A squat giant trunk under random_spread. |
| Tamarisk reads as a vanilla cherry | Accepted. A random_spread crown, dustier plumes on 25% or less. |
| Gorse is over-saturated | Accepted. Saturation capped at 0.65, lemon versus the wattle's egg-yolk. |
| Glowcap cap too bright, drawn as leaves | Accepted. Darker blotches in the mushroom-block manner; the blight wart likewise. |
| Lime bald-cypress needles in murky biomes | Accepted. A greyed ramp and a browner russet; not placed in the Sludge Mire. |
| Walnut: wood, husks and bark | Accepted. Smooth lime husks. The wood and bark are now its own (owner's update). |
| Live oak's pale moss | Accepted. Shroud moss. |
| The banyan reads as a mangrove | Accepted. Hanging roots, a wider dome and narrower root flare. |
| The forking dead snag reads as an acacia skeleton | Accepted. Straight, plus a bent variant. |
| Stumpless driftwood is impossible with vanilla's feature | Accepted. See driftwood. |
| One fronds routine for four leaf forms | Accepted. Midrib count, length and angle become parameters. |
| Blossom edges use one global green | Accepted. Per-entry edge green, under-gap colour and coverage. |
| Leaf entries without six tones | Accepted. Every new look above has six tones, checked by ΔE. |
| Per-tree gap rates the code cannot draw | Accepted in part. An optional threshold; per-tree figures kept only for the few that differ. |
| Fruit at fixed coordinates | Accepted. Each species places clusters from its own seed. |
| Near-white petals | Accepted. Main tones at lightness 0.86 or below. |
| Peach blossom reads as cherry | Accepted. Rose-magenta. |
| Bramble thorns jump in contrast | Accepted. A mid step under 4-5 pale pixels. |
| Young aspen and pandanus grow stacked crowns | Accepted. Leaf height set above the trunk. |
| Date palm is a lollipop | Accepted. A flattened crown, compared in game. |
| Shimmer birch near the eucalyptus and jacaranda | Accepted. Hue 155, pinker clusters. |
| The Field's juniper column beside vanilla spruce | Accepted. It replaces the spruce. |
| Cottonwood is a giant birch | Accepted. Aspen logs (as white poplar). The later move to willow logs is dropped by the owner's update: the aspen is the cottonwood's own genus, and willow is not. |
| The baobab's crown is too dense | Accepted. A random_spread crown for the giant. |

### Biome coverage

| Point | Decision |
|---|---|
| Alpine Spawn's shares do not add up | Accepted. 50/20/20/10; `alpine.TREES` becomes a picks list. |
| Umbrella pine on the all-sand Dune Beach | Accepted. Dropped. |
| Chances in order are not shares | Accepted. This document gives shares, and the build converts them. |
| A failed pick places nothing | Accepted. The blight fungus is its own extra in the Rotted Expanse, and sand-capable survival checks are used where trees stand on sand. |
| Positions, rarities and a height filter the schema cannot express | Accepted. The wording is dropped, rare trees become extras, and the fir is a plain pick. |
| The Gloomweald's trees land on vanilla's canopy | Accepted. Its own canopy list (batch 3). |
| Elder works against the Elder Vale's theme | Accepted. The Vale stays; elder goes to the Woodland and Wetland. |
| The Dryland mixes continents and keeps vanilla oak | Accepted. Pinyon-juniper country; the baobab moves out. |
| The Xeric Shrubland mixes continents | Accepted. Mojave shrubland; the olive bush is dropped. |
| Olives in snowy winters | Accepted. No Orchard olive; the Lavender Field's `winter_snow` goes off. |
| The rubber tree belongs to slice 5 | Accepted. See the rubber tree. |
| The Burnt Forest's "charred" trunks | **Not built for now; offered to the owner.** One `charred_log` block, a log only, drawn from painting 18, could trunk the Burnt Forest's and Cinder Barrens' snags. Painting 18 is now the persimmon's wood; the charred log could still take its bark colours from it. Until the owner asks for it, the record says grey snags, to keep the roster lean. |
| The Lush Grassland is still mostly vanilla | Accepted. The banana becomes its default. |
| Too many kinds in low-density biomes | Accepted. Trimmed (see Rejected ideas). |
| Placements too rare to see | Accepted. Wasteland 1 a chunk, Dry River 1, Cinder Barrens 1-2, Cold Desert about one chunk in six; the Highsun elder is dropped. |
| Pixel Hollows missing | Accepted. Listed; it stays as it is. |
| Wrong claims (Hot Springs, Wild Greens, persimmon) | Accepted. Reworded: the Hot Springs' grass changes with the seasons but its trees do not; the Wild Greens are treeless by their record. |
| Record lines to update | Accepted. Each batch updates them. |
| The azalea is called a stand-in in one place and added in another | Accepted, keeping the azalea. It stays the Subtropics' flowering shrub and joins the Swamp Woods. The flame tree replaces it only in the Tropics, where azaleas do not grow. |
| The dead snag skips the Rotted Expanse and Gloomweald | Accepted. Added to both. |
| The Webwood has no water for the bald cypress | **Rejected.** Bald cypress also grows on dry ground. Setting a water depth would move the Webwood's willows and dead trees into water too. Its cypresses stand on dry ground. |

---

Prepared on 5 October 2026 with Claude Opus 5.5, revised on 6 October 2026 with Claude Opus 5.5 for the owner's own-wood
update, and finalized the same day with Claude Opus 5.5 after that revision's review. Colour ramps and distances were
computed by script from `tools/wood_style.py`, and current shares from `tools/biomes.py`. Batch 1 has since been built
(not played); nothing else in this plan has been built, run in game or play-tested.
