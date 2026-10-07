# Biomes branch

Status: **in progress** on branch `claude/biomes`. Batch 1 (seasonal forests, with the region engine) is green in CI; batch 2 (fields and meadows, with region layouts) is green in CI; batches 3 (wetlands) and 4 (warm and dry, with Jugcraft surfaces) are green in CI; batches 5 (big trees and rainforests) and 6 (mountains, coasts and volcanoes) are green in CI; batches 7 (wonders and caves), 8 (the Nether) and 9 (the End) are green in CI, every Nether and End biome found in a real world. Every biome on the roster is built. The owner asked to carry on through every batch. Everything below is a design proposal, not a promise; each batch records its own feature document and test evidence under `docs/features/` when it is built. Nothing here has been played.

On 2 October 2026 the owner shared a catalog of the Biomes O' Plenty mod's biomes (compiled from its fan wiki: 143 pages) and asked to "remake all these within our mod". They chose:
- **Placement:** Jugcraft regions (below).
- **Scope:** every biome in the catalog except those that are now vanilla Minecraft biomes, Nether and End included.
- **First batch:** the seasonal forests.

## Rules for the remake

- **Original work only** ([LICENSE_POLICY.md](../../LICENSE_POLICY.md)). Every biome, tree, plant, block, texture and line of code is Jugcraft's own. The catalog's descriptions and screenshots guide the concept only, as the references in [ART_DIRECTION.md](../ART_DIRECTION.md) do; no other mod's code, data, textures or wiki text is copied, and the catalog itself is not committed.
- **Names.** Biomes named after real ecosystems keep their real names (Bayou, Bog, Redwood Forest, Lavender Field, Tundra). Names that Biomes O' Plenty invented (for example Origin Valley, Mystic Grove, Visceral Heap) get original Jugcraft names, listed below. Distinctive invented woods and blocks get original equivalents too.
- **Seasons.** Every biome with four seasons joins `#jugcraft:has_seasons` and `#jugcraft:has_winter_snow` ([features/seasons.md](../features/seasons.md)). Biomes O' Plenty's separate snowy variants of temperate biomes (Snowy Maple Woods, Snowy Dead Forest, Snowy Tundra) come from Jugcraft's winter instead; permanently snowy biomes exist only where the climate is frozen.
- **Deciduous trees follow the seasons** like Alpine Spawn's larch ([features/alpine-spawn.md](../features/alpine-spawn.md)): green in spring and summer, autumn colours, bare in winter, from the server's season clock.
- **Connections.** Each batch connects its biomes to existing content (the agriculture branch's crops, the sawmill and tree farm, the shared wood-set code) and records tier, producers, consumers and failure behaviour in its feature document, as [CONTENT_BRANCHES.md](../CONTENT_BRANCHES.md) asks of cozy surface biomes.
- **Duplicates are skipped**: biomes that became vanilla Minecraft biomes (Cherry Grove, Mangrove Swamp, Badlands, Bamboo Jungle and others), and two that were cancelled before release. Alpine Spawn already covers the Alps.

## Placement: Jugcraft regions

Fifty-odd new Overworld biomes cannot all take slices of vanilla's climate table without shrinking vanilla's biomes away, so the Overworld is divided into large regions:
- About 1 km across (`biomes.region_size`), shaped as irregular cells. A share of them (`biomes.region_share`, half by default) are **Jugcraft regions**; the rest stay pure vanilla.
- A Jugcraft region uses one of four **layouts**: vanilla's climate table with that layout's replacements (for example, taiga becomes Coniferous Forest). Their characters: woodland (seasonal forests, big trees), meadow (fields; warm and dry lands), wetland (wetlands, coasts) and wild (mountains, volcanoes, wonders). A biome may grow in several layouts. Vanilla biomes keep their full size in vanilla regions, and the replacements leave the rest of a layout alone.
- Terrain does not depend on biomes, so a region border shows as a change of biome, not a cliff.
- The region of a place comes from the world seed, so a seed always makes the same world.
- Alpine Spawn (the start) and the Pixel Hollows are in every region.
- Nether and End biomes use Fabric API's Nether and End biome placement instead.

The engine and its settings are recorded in [features/biome-regions.md](../features/biome-regions.md).

## Batches

| Batch | Biomes | Count |
| --- | --- | --- |
| 1. Seasonal forests | Aspen Glade, Coniferous Forest, Dead Forest, Maple Woods, Muskeg, Seasonal Forest, Snowy Forest, Tundra | 8 |
| 2. Fields and meadows | Field, Flower Meadow, Grassland, Heathland, Lavender Field, Lush Grassland, Prairie, Shrubland, Steppe | 9 |
| 3. Wetlands | Bayou, Bog, Dead Swamp, Fen, Floodplain, Ghost Forest, Lake District, Lush River, Lush Swamp, Marsh, Quagmire, Sludge Mire, Swamp Woods, Wetland | 14 |
| 4. Warm and dry | Bone Flats, Burnt Forest, Cold Desert, Dry River, Dryland, Jacaranda Glade, Lush Desert, Lush Savanna, Mediterranean Forest, Oasis, Orchard, Outback, Scrubland, Wasteland, Xeric Shrubland | 15 |
| 5. Big trees and rainforests | Dense Forest, Eucalyptus Forest, Rainforest, Redwood Forest, Subtropics, Temperate Rainforest, Tropics, Woodland | 8 |
| 6. Mountains, coasts and volcanoes | Basin, Canyon, Dune Beach, Flower Isle, Gravel Beach, Highland, Hot Springs, Ice Sheet, Karst Pinnacles, Ocean Trench, Overgrown Beach, Shield, Volcano | 13 |
| 7. Wonders and caves | Cinder Barrens, Elder Vale, Frostlight Garden, Gilded Shrubland, Glimmer Grove, Gloomweald, Glowcap Grotto, Hallowed Bog, Highsun Meadow, Mycelial Jungle, Shrine Springs, Snowpetal Grove, Spider Nest, Starlit Wood, Toadstool Field, Webwood, Wild Greens | 17 |
| 8. Nether | Ashfall Wastes, Blighted Sands, Frost Rift, Fungal Thicket, Magma Fields, Marrow Heap, Netherbrush, Quartz Rift, Withered Hollow | 9 |
| 9. End | Chorus Reef, Ender Wilds, Outer Flats, Phantom Garden, Rotted Expanse | 5 |

98 biomes in all. Large batches may be split when they are built.

**New trees.** Many of these biomes still borrow vanilla trees as stand-ins. The survey of what each biome needs, a
roster of 57 trees in 14 batches, is [TREES.md](TREES.md). It is a plan; none of it is built.

## The catalog, entry by entry

Every page in the catalog, what Jugcraft does with it, and in which batch. "Removed" means the catalog marks it removed or outdated in Biomes O' Plenty; removed biomes are remade too unless they are vanilla duplicates.

| Catalog entry | Realm | In the catalog | Jugcraft | Batch | Note |
| --- | --- | --- | --- | --- | --- |
| Alps | Overworld | removed | Alpine Spawn | done | done: Alpine Spawn takes this role |
| Alps Foothills | Overworld | removed | Alpine Spawn | done | done: merged into Alpine Spawn |
| Alps Forest | Overworld | current | Alpine Spawn | done | done: merged into Alpine Spawn |
| Arctic | Overworld | removed | Ice Sheet | 6 | merged into Ice Sheet |
| Ashen Inferno | Nether | removed | Ashfall Wastes | 8 | removed; renamed |
| Aspen Glade | Overworld | current | Aspen Glade | 1 |  |
| Auroral Garden | Overworld | current | Frostlight Garden | 7 | renamed |
| Autumn Hills | Overworld | removed | Seasonal Forest | 1 | merged: firs, persimmons and pumpkins in the Seasonal Forest's hills |
| Badlands | Nether | removed | — | skipped | skip: Nether badlands became vanilla |
| Bamboo Forest | Overworld | removed | — | skipped | skip: vanilla Bamboo Jungle |
| Basin | Overworld | removed | Basin | 6 | removed |
| Bayou | Overworld | current | Bayou | 3 |  |
| Birch Forest | Overworld | removed | — | skipped | skip: vanilla Birch Forest |
| Blessed Bog | Overworld | removed | Hallowed Bog | 7 | removed; renamed |
| Bog | Overworld | current | Bog | 3 | with the agriculture branch's cranberries |
| Boneyard | Nether | removed | — | skipped | skip: became vanilla |
| Burnt Forest | Overworld | current | Burnt Forest | 4 |  |
| Canyon | Overworld | removed | Canyon | 6 | removed |
| Canyon Ravine | Overworld | current | Canyon | 6 | merged into Canyon |
| Cherry Blossom Grove | Overworld | current | — | skipped | skip: vanilla Cherry Grove |
| Coast | Overworld | removed | — | skipped | skip: vanilla Stony Shore |
| Cold Desert | Overworld | current | Cold Desert | 4 |  |
| Coniferous Forest | Overworld | current | Coniferous Forest | 1 | with its clearing and snowy variants |
| Coral Reef | Overworld | removed | — | skipped | skip: vanilla Warm Ocean |
| Corrupted Sands | Nether | removed | Blighted Sands | 8 | removed; renamed |
| Crystalline Chasm | Nether | current | Quartz Rift | 8 | renamed |
| Dead Forest | Overworld | current | Dead Forest | 1 | with its old-growth variant |
| Dead Swamp | Overworld | current | Dead Swamp | 3 |  |
| Deadlands | Overworld | removed | Cinder Barrens | 7 | removed; renamed |
| Deciduous Forest | Overworld | removed | Woodland | 5 | merged into Woodland |
| Deep Bayou | Overworld | removed | Bayou | 3 | merged into Bayou |
| Dense Forest | Overworld | removed | Dense Forest | 5 | removed |
| Dry Boneyard | Overworld | current | Bone Flats | 4 | renamed |
| Dry River | Overworld | removed | Dry River | 4 | removed |
| Dryland | Overworld | current | Dryland | 4 |  |
| Dune Beach | Overworld | current | Dune Beach | 6 |  |
| End Corruption | End | current | Rotted Expanse | 9 | renamed |
| End Flats | End | current | Outer Flats | 9 | renamed |
| End Reef | End | current | Chorus Reef | 9 | renamed |
| End Wilds | End | current | Ender Wilds | 9 | renamed |
| Erupting Inferno | Nether | current | Magma Fields | 8 | renamed |
| Eucalyptus Forest | Overworld | removed | Eucalyptus Forest | 5 | removed |
| Fen | Overworld | removed | Fen | 3 | removed |
| Field | Overworld | current | Field | 2 | with its forested variant |
| Field (old) | Overworld | removed | Field | 2 | merged into Field |
| Floodplain | Overworld | current | Floodplain | 3 |  |
| Flower Field | Overworld | removed | — | skipped | skip: vanilla Flower Forest and Sunflower Plains |
| Flower Island | Overworld | removed | Flower Isle | 6 | removed; renamed |
| Flower Meadow | Overworld | current | Flower Meadow | 2 |  |
| Fungal Field | Overworld | current | Toadstool Field | 7 | renamed |
| Fungal Jungle | Overworld | current | Mycelial Jungle | 7 | renamed |
| Fungi Forest (Nether) | Nether | removed | Fungal Thicket | 8 | removed; renamed |
| Fungi Forest (Overworld) | Overworld | removed | Toadstool Field | 7 | merged into Toadstool Field |
| Ghost Forest | Overworld | current | Ghost Forest | 3 |  |
| Glowing Grotto | Nether | current | Glowcap Grotto | 7 | cave biome; renamed |
| Grassland | Overworld | current | Grassland | 2 | with its clover patches |
| Gravel Beach | Overworld | current | Gravel Beach | 6 |  |
| Heathland | Overworld | removed | Heathland | 2 | removed |
| Highland | Overworld | current | Highland | 6 | with its crag and moor |
| Hot Springs | Overworld | current | Hot Springs | 6 |  |
| Ice Sheet | Overworld | removed | Ice Sheet | 6 | removed |
| Icy Hills | Overworld | removed | Ice Sheet | 6 | merged into Ice Sheet |
| Jacaranda Glade | Overworld | current | Jacaranda Glade | 4 |  |
| Jade Cliffs | Overworld | current | Karst Pinnacles | 6 | renamed |
| Kelp Forest | Overworld | removed | — | skipped | skip: vanilla kelp in oceans |
| Knoll | Overworld | removed | Highland | 6 | merged into Highland |
| Land of Lakes | Overworld | removed | Lake District | 3 | removed; renamed |
| Land of Lakes Marsh | Overworld | removed | Marsh | 3 | merged into Marsh |
| Lavender Field | Overworld | current | Lavender Field | 2 |  |
| Lush Desert | Overworld | current | Lush Desert | 4 |  |
| Lush Grassland | Overworld | current | Lush Grassland | 2 |  |
| Lush River | Overworld | removed | Lush River | 3 | removed |
| Lush Savanna | Overworld | current | Lush Savanna | 4 |  |
| Lush Swamp | Overworld | current | Lush Swamp | 3 |  |
| Majestic Meadow | Overworld | removed | Highsun Meadow | 7 | removed; renamed |
| Mangrove | Overworld | removed | — | skipped | skip: vanilla Mangrove Swamp |
| Maple Woods | Overworld | current | Maple Woods | 1 | with its snowy variant |
| Marsh | Overworld | current | Marsh | 3 |  |
| Mediterranean Forest | Overworld | current | Mediterranean Forest | 4 |  |
| Mesa | Overworld | removed | — | skipped | skip: vanilla Badlands |
| Mountain | Overworld | removed | Highland | 6 | merged into Highland |
| Mountain Foothills | Overworld | removed | Highland | 6 | merged into Highland |
| Mud | Overworld | removed | — | skipped | skip: a block, now vanilla mud |
| Muskeg | Overworld | current | Muskeg | 1 |  |
| Mystic Grove | Overworld | current | Glimmer Grove | 7 | renamed |
| Oasis | Overworld | current | Oasis | 4 |  |
| Oceanic Abyss | Overworld | removed | Ocean Trench | 6 | removed; renamed |
| Ochre Acres | Overworld | removed | — | skipped | skip: cancelled, never released |
| Ominous Woods | Overworld | current | Gloomweald | 7 | renamed |
| Orchard | Overworld | current | Orchard | 4 | with the agriculture branch's fruit |
| Origin Valley | Overworld | current | Elder Vale | 7 | renamed |
| Outback | Overworld | current | Outback | 4 |  |
| Overgrown Beach | Overworld | removed | Overgrown Beach | 6 | removed |
| Overgrown Greens | Overworld | current | Wild Greens | 7 | renamed |
| Parasitic Heap | Nether | removed | Marrow Heap | 8 | merged into Marrow Heap |
| Polar | Overworld | removed | — | skipped | skip: vanilla frozen oceans |
| Polar Chasm | Nether | removed | Frost Rift | 8 | removed; renamed |
| Prairie | Overworld | current | Prairie | 2 | with its pasture variant |
| Quagmire | Overworld | removed | Quagmire | 3 | removed |
| Rainforest | Overworld | current | Rainforest | 5 | with its rocky variant |
| Rainforest (old) | Overworld | current | Rainforest | 5 | merged into Rainforest |
| Redwood Forest | Overworld | current | Redwood Forest | 5 | with its edge |
| Redwood Forest Edge | Overworld | current | Redwood Forest | 5 | merged into Redwood Forest |
| Sacred Springs | Overworld | removed | Shrine Springs | 7 | removed; renamed |
| Savanna | Overworld | removed | — | skipped | skip: vanilla Savanna |
| Scrubland | Overworld | current | Scrubland | 4 | with its wooded variant |
| Sea | Overworld | removed | — | skipped | skip: vanilla oceans |
| Seasonal Forest | Overworld | current | Seasonal Forest | 1 | with pumpkin patches |
| Seasonal Forest Clearing | Overworld | removed | Seasonal Forest | 1 | merged into Seasonal Forest |
| Seasonal Orchard | Overworld | current | Orchard | 4 | merged into Orchard |
| Shield | Overworld | current | Shield | 6 |  |
| Shrubland | Overworld | removed | Shrubland | 2 | removed |
| Silkglade | Overworld | removed | Webwood | 7 | removed; renamed |
| Sludgepit | Overworld | removed | Sludge Mire | 3 | removed; renamed |
| Snowblossom Grove | Overworld | current | Snowpetal Grove | 7 | renamed |
| Snowy Dead Forest | Overworld | removed | Dead Forest | 1 | merged: the Dead Forest's snowy variant |
| Snowy Forest | Overworld | current | Snowy Forest | 1 |  |
| Snowy Tundra | Overworld | removed | Tundra | 1 | merged: the Tundra in winter snow |
| Spectral Garden | End | removed | Phantom Garden | 9 | removed; renamed |
| Spider Nest | Overworld | current | Spider Nest | 7 | cave biome |
| Spruce Woods | Overworld | removed | Coniferous Forest | 1 | merged into Coniferous Forest |
| Steppe | Overworld | current | Steppe | 2 |  |
| Sublime Shrubland | Overworld | removed | Gilded Shrubland | 7 | removed; renamed |
| Subtropics | Overworld | current | Subtropics | 5 |  |
| Swamp Woods | Overworld | current | Swamp Woods | 3 |  |
| Tar Pit | Overworld | removed | — | skipped | skip: cancelled, never released |
| Temperate Rainforest | Overworld | removed | Temperate Rainforest | 5 | removed |
| Temperate Rainforest Hills | Overworld | current | Temperate Rainforest | 5 | merged into Temperate Rainforest |
| Thicket | Overworld | removed | Shrubland | 2 | merged into Shrubland |
| Timber | Overworld | removed | Woodland | 5 | merged into Woodland |
| Tropic Beach | Overworld | current | Tropics | 5 | merged into Tropics |
| Tropics | Overworld | current | Tropics | 5 |  |
| Tundra | Overworld | current | Tundra | 1 |  |
| Undergrowth | Nether | current | Netherbrush | 8 | renamed |
| Visceral Heap | Nether | current | Marrow Heap | 8 | renamed |
| Volcano | Overworld | current | Volcano | 6 | with its volcanic plains |
| Volcano Edge | Overworld | current | Volcano | 6 | merged into Volcano |
| Wasteland | Overworld | current | Wasteland | 4 | with its steppe |
| Wetland | Overworld | current | Wetland | 3 |  |
| Withered Abyss | Nether | current | Withered Hollow | 8 | renamed |
| Wondrous Woods | Overworld | removed | Starlit Wood | 7 | removed; renamed |
| Woodland | Overworld | current | Woodland | 5 |  |
| Xeric Shrubland | Overworld | current | Xeric Shrubland | 4 |  |

## Open questions
- Region size and share are first guesses; the batch 1 client test logs how far the first world has to go to find each new biome.
- Marrow Heap (the catalog's Visceral Heap and Parasitic Heap) should be bone and gristle rather than gore, to suit Jugcraft's cozy tone.
- The Promised Land biomes (Hallowed Bog, Highsun Meadow, Gilded Shrubland, Starlit Wood, Lush River) came from a removed sky dimension. They are planned as Overworld wonders for now; the realms branch may want them instead.
