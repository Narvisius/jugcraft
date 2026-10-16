# Trees batch 4: snowpetal cherry and shimmer birch

Implementation record, 15 October 2026. Owner-authorized batch 4; actual tool: OpenAI Codex (GPT-6).

**Implemented; awaiting owner visual approval.** Public branch `codex/tree-generation-batch-4`, draft PR #389, builds on approved batch 3 (PR #388, which includes batch 2). Scope is snowpetal cherry, normal/tall shimmer birch and their four biome mixes. Later batches, including bald cypress and magnolia, are not started. Actual tool: OpenAI Codex (GPT-6).

Approved refinements: Glimmer Grove explicitly uses 55% normal jacaranda, 15% big jacaranda, 20% normal shimmer birch and 10% red mushroom; its previous oak share was 18%. Shimmer saplings choose normal/tall shapes at 80%/20%. Existing biome placement counts remain unchanged.

## Separate the tree from its timber

`agriculture.TREES` accepts an optional `wood` key and a separate display name. Existing entries default to their own name, preserving their blocks, recipes and seasonal behavior. `trees.SHAPES.tree` selects foliage independently of its log wood. Its default remains the existing wood key. Explicit `log` support is reserved for later special stems; this batch needs only vanilla cherry and birch logs.

Java registers foliage/saplings independently of wood sets. Existing trees register both through the existing wrapper. Snowpetal cherry and shimmer birch register only their leaves and saplings, with no invented cherry or birch wood-set IDs. Their timber uses vanilla recipes, tags and processing. Feature switches follow the real timber where Jugcraft owns it; trees on vanilla timber carry their own `biomes` switch. The data audit checks both log and foliage ownership for every placement source and rejects accidental wood-set blocks for trees without a wood set.

The tree farm uses each tree's actual log output and an explicit optional yield, defaulting to the existing six logs. Later one-log shrubs can specify one or two logs. No existing tree's farm output or cost changes. Sapling survival uses the tree's own sapling unless a shape explicitly asks for another ground rule.

## Player outcome

- Snowpetal cherry: native cherry shape and cherry logs, dense near-white blossom with lilac shadows and white falling particles, evergreen. Replaces the 75% vanilla-cherry share in Snowpetal Grove; aspen remains 25%.
- Shimmer birch: vanilla birch logs under mint foliage with sparse pink-lilac clusters. Normal straight trunk 5 (+2), blob radius 2 / height 3; tall trunk 10 (+4), blob radius 2 / height 5. Evergreen. Frostlight Garden 40%, Starlit Wood 45% tall form alongside the retained 30% vanilla super-birch bee trees, Glimmer Grove 20%. Target percentages are converted to conditional selector chances.

Both are Discovery-stage exploration/building content. Natural leaves yield their own saplings; replanted trees yield established vanilla timber. There is no new crafting gate, required specialty dependency or fruit tick loop. Failed growth must preserve saplings. Existing chunks remain untouched.

## Implementation and assets

The native cherry trunk/foliage settings and minimum clearance are used for snowpetal. Shimmer uses straight 5 (+2) / 10 (+4) trunks and blob radius 2, foliage heights 3 / 5. One shimmer sapling selects the normal/tall feature with native weighted entries 4:1; it does not require a square of saplings. Both leaf blocks are evergreen, drop their own saplings, support shears/Silk Touch and decay without supporting logs. The tree-farm route costs the established 400 ticks, yields six vanilla logs and returns one matching sapling. Vanilla crafting and existing sawmill recipes process that timber.

The owner library's biome/tree and magic catalogs were searched for snowpetal, shimmer, cherry and birch. No matching snowpetal or shimmer foliage was found. The five new 16x16 textures are authored in `tools/tree_roster_textures.py` using the established Canvas and seeded leaf painter, with the approved roster palettes. Timber is placed directly as vanilla blocks; no Minecraft textures are bundled. Snowpetal adds a sixth deeper lilac shadow to the roster's five blossom tones, green pockets and a dark green under-gap colour. Shimmer has three spaced pink-lilac clusters with a species-specific seed. The saplings reflect the branching cherry and slender white birch trunks. Existing owner pine art is untouched.

White falling petals use a registered simple particle and Minecraft's existing cherry-petal provider on the client, with an authored white sprite. The common leaf block uses the native particle-leaves behavior; no new tick loop, custom particle engine or dependency is introduced.

Final biome attempt shares: Snowpetal Grove 75% snowpetal / 25% aspen; Frostlight Garden 40% shimmer / 60% fir; Starlit Wood 45% tall shimmer / 30% vanilla super birch with bees / 25% tall fir; Glimmer Grove 20% shimmer / 55% jacaranda / 15% big jacaranda / 10% red mushroom. Counts remain 3/4, 3/4, 8/9 and 6/7 respectively, with a 90%/10% choice between the two counts. These are attempts, not guaranteed standing-tree proportions. Existing ground decoration and climate rules are unchanged.

## Validation and inspection world

The focused build/server/client run passed all four `TreeBatchFourGameTests` and `TreeBatchFourClientGameTests` on Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and Java 25.0.4.1. Twelve separated seeds placed twice each reproduced exactly while producing different crowns: snowpetal 10-11 blocks high / 7-15 wide; normal shimmer 6-8 high / 5 wide; tall shimmer 11-15 high / 7 wide. Log and leaf identities matched every sample. Forty single saplings per species grew successfully; shimmer produced 34 normal and six tall trees in that deterministic sample (configured selection weights remain exactly 4:1).

The tests check obstacles without consuming/changing saplings, invalid stone soil, snow clearance against actual vanilla cherry/birch, growth surrounded by snow, evergreen states through all season modes, unsupported-leaf decay, persistent-leaf survival, natural sapling drops, shears/Silk Touch, tree-farm output/returned sapling, vanilla plank crafting and sawmill routes, and absence of phantom timber registrations. The first broad run exposed an incorrect test expectation that tree placement should overwrite an occupied snow layer. The corrected test checks vanilla parity and successful planting in surrounding snow; production clearance behavior was unchanged.

The initial full server suite completed 1,658 tests: 1,656 passed; that snow-test expectation and the existing `CrowGameTests.aCrowFliesDownToPeck` timeout failed. The corrected four tree tests then passed in the focused build/client run. The crow test passed without a code change on an isolated rerun with `-PjugcraftOptionalIntegrations=false`, also verifying server startup without the optional integrations (`build/tree4-crow-recheck.log`). The full suite is not represented as entirely green. Logs are `build/tree4-full.log` and `build/tree4-focused-client.log`.

All 43 Python tests passed; the three batch-4 checks passed again after the sapling art refinement. The complete data audit passed 2,675 material IDs, 8,376 audited block/item models, 3,707 quad parts and 171 combined models. Latest biome/timber/switch checks also passed separately. All 114 previous generated tree texture outputs retained identical pixels, and all 100 previous tree/fallen-tree feature files retained identical data. Unrelated PNG compression and floating-point regeneration noise were discarded. Repository structure and documentation links passed.

The normal inspection world uses seed `jugcraft`. Nine comparison trees (three seeds per shape), a snow-backed foliage/sapling wall, solid/cutout leaf views, a night view and the white-petal provider were checked in the rendered client. The world was saved and reopened with all nine trees unchanged. Natural surveys found the new leaves at all four stops:

| Biome | Teleport | New leaf blocks counted |
| --- | --- | ---: |
| Snowpetal Grove | `/tp @s -1280 139 3968 135 25` | 4,569 snowpetal |
| Frostlight Garden | `/tp @s -1864 90 0 135 25` | 1,473 shimmer |
| Starlit Wood | `/tp @s 1368 95 -712 135 25` | 11,993 shimmer |
| Glimmer Grove | `/tp @s 1552 81 696 135 25` | 368 shimmer |

These are block counts in bounded biome surveys, not tree percentages. Starlit Wood also retained vanilla birch foliage and bee nests. The garden starts at `/tp @s 1080 180 -988 180 0`; walk west for the other forms and north for more seeds. Preview time, weather, mob spawning and random ticks are frozen; bone meal works, and `/gamerule minecraft:random_tick_speed 3` restores ordinary growth/decay.

The delivery copy was scanned through 8,486 region records and all other saved files. Only the two development datapack selections and five unused test registry presence names were removed, changing three metadata files and preserving every other saved byte. The source export remains untouched. The package supplies the matching JAR, standalone world ZIP, Modrinth inspection pack, tour and screenshots.

The cleaned runtime copy also opened in the ordinary Minecraft client with the complete locked integrations and without the test mod. The player joined the comparison garden; `build/tree4-ordinary-client.log` records that launch. The delivered world is a separate copy, so the live inspection session cannot alter the archives.

Screenshots: [snowpetal](../images/trees-batch-4-snowpetal.png), [normal shimmer](../images/trees-batch-4-shimmer.png), [tall shimmer](../images/trees-batch-4-tall-shimmer.png), [Snowpetal Grove](../images/trees-batch-4-snowpetal-grove.png), [Starlit Wood](../images/trees-batch-4-starlit.png), [leaf/sapling display](../images/trees-batch-4-leaves.png).

Commands: `./build-local.ps1 -Tasks @('build','runGameTest')`; focused completion adds `-I build/pine-focused.gradle -PgameTestFilter=jugcraft-test:tree_batch_four_game_tests_* runClientGameTest -PclientTests=TreeBatchFourClientGameTests`. Python checks: `python -m unittest discover -s scripts/tests -v`, `python tools/check_mod_data.py`, `python scripts/check_repository.py`.

Not claimed: owner visual approval, external launcher import, two-client multiplayer, long-duration performance or every world seed. No merge, release, server deployment or later tree batch is included.
