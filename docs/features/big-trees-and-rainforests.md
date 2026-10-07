# Big trees and rainforests (biomes batch 5)

Status: in progress on branch `claude/biomes`, with the region engine ([biome-regions.md](biome-regions.md)). Green in CI (server and client game tests). **Not yet played.**
Proposal issue: none. The owner asked on 2 October 2026 to remake the Biomes O' Plenty catalog's biomes ([branches/BIOMES.md](../branches/BIOMES.md)) and to carry on through every batch. Everything here is original: the catalog guided the concepts only.
Owner: @jimbozoomer-byte
Target milestone and tier: world generation and building (Discovery).
Primary specialty and supported player role: exploration; building (redwood, eucalyptus and mahogany wood).

## Player experience
Eight forests, mostly in the **woodland** layout of Jugcraft regions. The two giant forests of the old-growth taigas also grow in the wild layout:

| Biome | Layout: replaces | What grows |
| --- | --- | --- |
| **Rainforest** | woodland: the wettest jungles | tall mahoganies and giant ones, jungle trees and bushes over ferns, orange cosmos and puddles; only parrots |
| **Eucalyptus Forest** | woodland: other jungles, savanna plateaus | tall eucalyptus with rainbow-streaked bark, oak scrub, melons and wildflowers; parrots among the farm animals |
| **Tropics** | woodland: sparse jungles | bright green land of palms, small palms, flowering azaleas and jungle bushes; hibiscus, hydrangeas and bamboo; turquoise water; parrots; jungle temples |
| **Subtropics** | woodland: savannas, warm plains | green, plains-like country with flowering azaleas, oaks, birches, small palms and vine-hung oaks; hydrangeas, sugar cane; villages |
| **Dense Forest** | woodland: dark forests | big spreading oaks packed close, dark oaks and a rare great oak among them, leaf litter and ferns; woodland mansions |
| **Redwood Forest** | woodland and wild: old-growth pine taigas | giant redwoods two blocks wide and tall single ones, a few firs and mossy maples, on podzol broken by moss; ferns and tall ferns |
| **Temperate Rainforest** | woodland and wild: old-growth spruce taigas | firs and redwoods, moss-hung bigleaf maples and willows, thick with ferns and berry bushes |
| **Woodland** | woodland: flower forests, sunflower plains | plain oak woodland: oaks big and small, fallen logs, leaf litter, poppies, daisies, berry bushes; villages and woodland mansions |

- Biomes O' Plenty's Redwood Forest Edge merges into the Redwood Forest, the Temperate Rainforest Hills into the Temperate Rainforest, the old Rainforest into the Rainforest, the Tropic Beach into the Tropics, and the Deciduous Forest and Timber into the Woodland.
- Seasons: the Dense Forest, Redwood Forest, Temperate Rainforest and Woodland change colour with the seasons. Only the Dense Forest and Woodland take winter snow when `seasons.snow` is on; the redwood and temperate rainforests have mild, wet winters. The four tropical forests do not change.
- **New trees:**
  - Redwood: a tall, straight trunk under a narrow spire of needles. **Four redwood saplings in a square grow a giant redwood**: a trunk two blocks wide, its crown high up, podzol spread round its foot.
  - Eucalyptus: a tall, clean trunk with a loose, airy crown; its bark is streaked green, orange, purple and blue where it peels. Big ones branch high up.
  - Mahogany: a tall rainforest hardwood that forks into a broad, flat canopy hung with vines. **Four mahogany saplings in a square grow a giant mahogany**, two blocks wide.
  - All three have full wood sets and saplings and are evergreen. A small palm grows in the tropics and subtropics.
- **New flowers:** hibiscus (pink; dye and suspicious stew, water breathing) and hydrangea (a tall flower with blue mopheads; light blue dye).
- **Ground:** the Redwood Forest lays podzol with patches of moss and coarse dirt.

## Connections
- Input producer: world generation.
- Output consumer: building (redwood, eucalyptus and mahogany wood), dye (hibiscus, hydrangea), farming (melons in the Eucalyptus Forest, sugar cane in the Subtropics).
- Technology connection: the sawmill and tree farm take the three new woods like the others.
- Magic connection: none yet.
- Reachable entry path: jungles, savannas, dark forests and flower forests are common; old-growth taigas less so. The woodland layout is a quarter of Jugcraft regions; the giant forests also grow in the wild layout.
- Required vs optional: nothing here is required.

## Balance and automation
- The new woods are ordinary wood (4 planks by hand, 6 in the sawmill). A giant tree gives more logs from four saplings, as vanilla's giant spruce and jungle trees do. No new conversions, no loops.

## Multiplayer and persistence
- Server authority: world generation and tree growth are the server's.
- `biomes.enabled=false` stops the biomes in new chunks and the new woods' hand recipes. Every block, item and biome stays registered.
- Existing worlds: new chunks only.

## Dependencies and assets
- Giant trees use vanilla's giant and mega jungle trunk shapes and mega pine and jungle crowns. Four saplings in a square are found by `agriculture/GiantSaplingBlock`, which grows the giant from the square's north-west corner and leaves the saplings if there is no room. The other trees use vanilla's straight, fancy, forking and bending trunks.
- Leaf litter, melons, bamboo, fallen oak logs and vanilla's oaks with leaf litter come from vanilla's own features, by reference.
- Textures drawn by code in `tools/wild_textures.py`. No Mojang file is copied. Since the [wood repaint](wood-repaint.md), their woods and leaves are drawn by `tools/wood_style.py`.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py`: trees (each giant is a giant shape of its tree's wood, with its grower in Java), biomes and their features, and non-overlapping rules.
- Server game tests (`BiomeGameTests`):
  - `bigTreesGrow`: a redwood, a eucalyptus and a mahogany grow from saplings, and four redwood or mahogany saplings in a square grow a giant whose trunk is two blocks wide.
  - `tropicalPlantsWork`: hibiscus is a small flower with a potted form; a hydrangea takes two blocks and drops one.
  - `regionLayoutFollowsItsRules`: every batch 5 rule places its biome in each of its layouts.
- Client game test (`BiomeClientGameTests`): distances from the start of a real world (seed `jugcraft`), and screenshots of each biome found.
- Not run: play, a dedicated server, two clients.

### Results
- **Run [37040036527](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37040036527) (commit 5cc96597): server green, client failed.**
  - Server game tests: all 313 passed. A redwood of 13 logs and only 25 needles (its crown was then widened), a eucalyptus of 11 logs and 38 leaves, a mahogany of 10 and 57. Four saplings grew a giant redwood of 117 logs and a giant mahogany of 92, both two blocks wide.
  - Client game test: failed in the seasons test, not here: grass under seasonal snow had turned to dirt ([seasons.md](seasons.md); fixed in commit 44f70069).
- **Run [37041750692](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37041750692) (commit 44f70069): green.**
  - Server game tests: all 314 passed.
    - `bigTreesGrow`: a redwood of 14 logs and 165 needles, a eucalyptus of 12 and 42, a mahogany of 9 and 85; a giant redwood of 125 logs and a giant mahogany of 49, both two blocks wide.
    - `tropicalPlantsWork`: a broken hydrangea dropped 1.
    - The woodland layout placed every batch 5 biome: Subtropics 448 entries, Eucalyptus Forest 220, Dense Forest 174, Woodland 158, Rainforest 156, Redwood Forest 114, Temperate Rainforest 100, Tropics 88. The wild layout also placed the Redwood Forest (114) and Temperate Rainforest (100).
  - Client game test, a real world with seed `jugcraft`: all eight were within 6,400 blocks of the start.
    - Temperate Rainforest 1,104 blocks away, Subtropics 1,319, Dense Forest 1,345, Tropics 1,384.
    - Rainforest 1,431, Eucalyptus Forest 1,438, Redwood Forest 1,498, Woodland 2,307.
  - Screenshots (2 October, autumn):
    - Rainforest: a dark green canopy of mahoganies and jungle trees hung with vines.
    - Eucalyptus Forest: tall, streaked trunks under blue-green crowns.
    - Tropics: a turquoise lagoon below green hills with palms. Subtropics: green hills with azaleas and oaks.
    - Dense Forest: big oaks packed close, in autumn colours. Temperate Rainforest: firs and redwoods with a yellow willow.
    - Woodland: the camera stood inside an oak's crown.
    - Redwood Forest: the camera looked down into a lake. The client test now prefers dry ground.
- **Fixed later (3 October 2026): `bigTreesGrow` asked too much of a giant mahogany, then too little of a giant redwood.**
  - It wanted at least 40 logs from each giant. A trunk two blocks wide has four logs a level and one at its top, 4h − 3 for a trunk h tall. A giant mahogany's mega jungle trunk is at least 10 tall (`tools/trees.py`) and may by chance have no branches, so it can have as few as 37. CI grew 37 once and 38 once, and failed.
  - #134 lowered the floor to 30 logs for both giants. That passes, but a giant redwood's trunk is at least 22 tall, 85 logs, so 30 no longer told a giant redwood from a stunted one.
  - Each giant is now held to its own fewest: a redwood 85, a mahogany 37. Both must still be two blocks wide. The same change passed in both attempts of Build workflow run [37129429165](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37129429165) (on `edceeff`, before #134), with a giant redwood of 133 logs and giant mahoganies of 72, then 101 and 85. On `main` at `44c894e9`, run [37158942993](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37158942993) on `7e4c3822` passed: the build, all 593 server game tests (a giant redwood of 125 logs, a giant mahogany of 53) and the three client shards.

## World and event applicability
- Biome fit: each biome takes the climate of the vanilla biome it replaces.
- Seasons: see above.
