# Seasonal biome audit

Status: implemented on `claude/seasonal-audit` ([#454](https://github.com/jimbozoomer-byte/jugcraft/pull/454)), awaiting review. Its CI passed (below). **Not yet played.** A follow-up, stacked on it, takes the fall's wild patches, the graveyard flowers and the Hag Stone to the landscape biomes through the same kinds ([below](#follow-up-the-falls-wild-patches-the-graveyard-flowers-and-the-hag-stone)).
Proposal issue: none. This is the TODO list's "MUST: audit seasonal content against every newer biome collection" ([TODO](../TODO.md#living-spawn-city--october-21-local-work)). On 22 October 2026, offered it as the next work, the owner answered "Lets do the seasonal audit then lets do Guns Slice 14D".
Owner: @jimbozoomer-byte
Target milestone and tier: seasonal delivery alongside world generation; no tier.
Primary specialty and supported player role: exploration and the Halloween and autumn content; every player meets them.

## What the audit found
Every seasonal or date-dependent system was checked against the biome collections that came after the seasons:
- **The landscape collection** (the owner's 18 October collections, `tools/landscape_content.py`): 159 biomes.
  - Its overworld biomes take part of every Jugcraft region (`menagerie/LandscapeBiomes`).
  - Its Nether and End biomes join those dimensions.
  - Its caves lie underground.
- **The caverns:** six cave biomes, the Candy Cavity among them, and the Pixel Hollows.
- **The lairs' and pocket dimensions' own biomes:** the Cinder Kiln, Crypt Archive, Cydonian Sanctum, Foundry Trench, Glacier Hall, Hollow Acre, Lantern Fen, Moonlit Ring, Spindle Loft and the Nouveau jar.
- **The newer biomes of `tools/biomes.py`**, from the tree batches, the wonders batch and the Adam-ondi-Ahman valley.

| System | When | Where it looks | Newer collections, before this change | Now |
|---|---|---|---|---|
| Grass and leaf colours | the season day | `#jugcraft:has_seasons` | None of the 159 landscape biomes had seasons, though about a third are temperate woods, fields and swamps (the Landscape Seasonal Forest, Maple Woods and Pumpkin Patch among them) | 54 landscape biomes have seasons (below) |
| Winter snow (`seasons.snow=on`) | winter, while it rains | `#jugcraft:has_winter_snow` | as above | 47 of them; the 7 mild ones keep their colours' turn but get no snow |
| Will-o'-wisps | Halloween nights | `#c:is_swamp`, or within 2 blocks of corn | The landscape swamps (Bayou, Bog, Marsh, Wetland and others) carry no common tags, so wisps came out there only by corn | Wisps read `#jugcraft:wisp_haunts`: the common swamps and the 7 landscape swamps |
| Werewolves, and the Alpha Werewolf's Howling Totem rite | full-moon nights (not the date) | `#jugcraft:werewolf_haunts`, `#jugcraft:snow_werewolf_haunts`, `#jugcraft:shadow_werewolf_haunts` | The landscape woods carry no `#minecraft:is_forest` or `#minecraft:is_taiga`, so no werewolf came there | Their haunts add the 31 landscape woods, the 23 snowy landscape biomes (snow werewolves) and the Ominous Woods (shadow werewolves) |
| Squirrels | by day | `#jugcraft:squirrel_habitat` | as werewolves | The 31 landscape woods |
| Wild turkeys | by day | `#jugcraft:turkey_habitat` | as werewolves; landscape fields too | The landscape woods and its 18 open grasslands (not the snowy ones) |
| Scary creatures | Halloween nights | every overworld biome but mushroom fields, outdoors | Included already (the open-sky rule keeps them out of caverns) | Unchanged |
| Harvest Moon (crops grow twice as fast) | the Harvest Moon night | everywhere | Included already | Unchanged |
| Trick-or-treating, costumes, the peddler, the carving contest | Halloween | no biome check | Included already | Unchanged |
| Headless Horseman, witching-season lairs | Halloween midnight; any night by default | the overworld, open sky | Included already | Unchanged |
| Town decorations, shop stock, townsfolk | December, then Halloween, then the Harvest Feast, then the season | the town only | Biome-independent | Unchanged |
| Concordance sky and celestial patterns | the season and the Harvest Moon | the overworld, open sky | Biome-independent | Unchanged |
| Seasonal tree leaves (maple, aspen, larch, willow, rowan, elder, bald cypress, walnut and the rest) | the season day | the species, anywhere | The Snowy Forest, Muskeg, Snowpetal Grove and Webwood grow these trees without having seasons: their leaves turn and their grass does not | Unchanged, deliberately: these species are deciduous wherever they stand, as a planted sapling's tree is |
| Lair event loot (Vesperine, Tatterlace) | Halloween, with `lairs.event_loot=on` | the lairs | Biome-independent | Unchanged |
| The fall's wild patches: the festival gourds and cranberries, chestnut trees, the heirloom pumpkins, mums, the autumn mushrooms, wild mandrake and wolfsbane (follow-up) | always (worldgen, new chunks) | the common kind tags (`#c:is_forest`, `#c:is_swamp` and the like) | None in the landscape biomes | Each also grows in the landscape biomes of its kind: woods, grasslands, swamps, snowy land and the dark woods (`tools/landscape_kinds.py` BESIDE) |
| The graveyard flowers' wild patches: spider lilies, ghost pipes, snowdrops and the rest (follow-up) | always (worldgen, new chunks) | the common kind tags | as above | as above |
| The Hag Stone, the Mire Hag's rite (follow-up) | any night | `#jugcraft:hag_stone_lands`: the common swamps, the Floodplain and the Muskeg | The landscape swamps were not wetland to it | It takes the 7 landscape swamps (`#jugcraft:landscape_kind/swamps`) |

**No date dependence** was found in fishing, the menagerie's animal spawns or any loot-table condition. The Christmas pocket pet and the Harvest Feast meal score are not date-gated.

**The seasons do not need a larger rewrite.** One calendar drives the colours, snow and town events; one Halloween window drives the Halloween content; placement is by biome tags. The gaps came from newer biomes that the tags never named. The data check (below) now fails on any new biome that has no seasons decision, so the next collection cannot miss them silently.

## Player experience
- **In about a quarter of each Jugcraft region** (where the landscape biomes are), grass and leaves now turn with the year, as they already did elsewhere:
  - fresh green in spring;
  - gold, orange and red in autumn;
  - grey-brown in winter, under snow where the operator has it on.
- **The 54 landscape biomes with seasons:** the temperate woods (forest and taiga kinds), open country, hills and swamps where it rains and is neither frozen nor tropical (temperature 0.15 to 0.9, the range of the seasonal vanilla and Jugcraft biomes). These include:
  - the Seasonal Forest, Maple Woods, Pumpkin Patch, Birch Taiga, Coniferous Forest, Redwood Forest;
  - the Field, Prairie, Pasture, Steppe and Tundra;
  - the Highlands, Wetland, Marsh and Bog.
- **The 7 mild ones, no winter snow:** the Mediterranean Forest, the three lavender biomes, the Redwood Forest, the Hot Shrubland and the Orchid Swamp, as their Jugcraft namesakes.
- **No seasons, and why** (each kind's biomes are listed in `tools/landscape_kinds.py`):
  - **Frozen land**, where snow falls all year (below 0.15): the wintry, snowy and Siberian biomes, the alpine grove, the ice marsh, the muskeg and the Scarlet Mountains.
  - **Tropics**, at 0.95 or warmer: the jungles, rainforests, tropics, subtropics, Floodplain, Jade Cliffs and the Bayou.
  - **Arid land:** deserts, badlands, savannas and dry shrubland.
  - **Magic, with fixed colours**, as the Elder Vale and the Glimmer Grove: the Mirage Isles, Moonlight Grove and Valley, Mystic Grove, Amethyst biomes, Auroral Garden, the Alpha Islands and the Origin Valleys.
  - **The Skylands:** one biome for each season, which keep their own.
  - **The Ominous Woods**, gloomy all year as the Gloomweald.
  - **The rest:** volcanoes, coastal cliffs, beaches, the warm ocean and river, caves, and the Nether and End biomes.
- **Halloween nights** (20 October to 3 November by default): wisps rise over the landscape swamps.
- **Full moons:** werewolves stalk the landscape woods; snow werewolves stalk its snowy land, shadow werewolves the Ominous Woods. The Howling Totem rite works there too.
- **By day:** squirrels come to the landscape woods and turkeys to its woods and open country.

## Connections
- **Input producer:** the season clock and the Halloween window, both already present.
- **Output consumer:** the biome tags that the client colours, the winter snow and the creature spawners read.
- **Technology and magic:** none new. The Alpha Werewolf's Howling Totem rite reads the werewolves' haunts, so it gains the landscape woods.
- **Shared tags:** `tools/landscape_kinds.py` gives each landscape biome a kind. From the kinds it writes four Jugcraft tags for other content to use beside the vanilla and common biome-kind tags:
  - `#jugcraft:landscape_kind/woods`
  - `#jugcraft:landscape_kind/grasslands`
  - `#jugcraft:landscape_kind/swamps`
  - `#jugcraft:landscape_kind/snowy`

## Balance and automation
No item, recipe, cost or reward changes. More places host the same creatures, with the same per-player and per-world caps (werewolves 2 near a player and 8 in the world; squirrels 6 and 30; turkeys 10 and 40; wisps 4 and 64), so no more of them are about at once than before.

## Multiplayer and persistence
- Nothing new is saved and nothing is sent between client and server. Biome tags reach clients with the server's tags, as before.
- Seasonal snow in the newly tagged biomes falls only with `seasons.snow=on`, and melts in spring, as everywhere.
- Existing builds, items and earned seasonal rewards are untouched. Generated terrain does not change; colours and spawns follow the tags at once, in old chunks too.

## Dependencies and assets
No new dependencies or art.
- **Data:** `tools/landscape_kinds.py` (new) and `tools/seasons.py` (`BIOMES`, `WINTER_SNOW` and `OTHER`), from which `tools/generate_material_data.py` writes the seasons and kind tags.
- **Creature lists:** `tools/agriculture.py` adds the kind tags to the werewolf, squirrel and turkey lists, and gives `WISPS` its haunts. `tools/night_data.py` writes `#jugcraft:wisp_haunts`.
- **Java:** `agriculture/Wisps.java` reads `#jugcraft:wisp_haunts` instead of `#c:is_swamp`.

## Verification
- **Run locally:**
  - `python3 tools/generate_material_data.py`: regenerated the tags.
  - `python3 tools/check_mod_data.py`: its new `check_seasonal_biomes` checks that:
    - every Jugcraft biome has a seasons decision: a `"seasons"` flag in `tools/biomes.py`, a landscape kind, or an entry in `seasons.OTHER`;
    - the landscape `SEASONS` and `SNOWY` lists follow their rules against each biome file's own climate;
    - every seasonal Jugcraft biome rains and lies between 0.15 and 0.9;
    - winter snow falls only where it rains;
    - the kind tags and `#jugcraft:wisp_haunts` match `tools/`, and `Wisps.java` reads the tag.
  - `python3 scripts/check_repository.py`.
  - **Java:** a syntax parse only. Compilation and the game tests run in CI.
- **Game tests** (`SeasonalBiomeGameTests`, all passed in CI, below):
  - `landscapeBiomesFollowTheSeasons`:
    - temperate landscape biomes have seasons and winter snow;
    - mild ones have seasons without snow;
    - one of every other kind has neither, as do the caverns and the lairs' and pocket dimensions' biomes;
    - across the whole biome registry, every seasonal biome rains and lies between 0.15 and 0.9, and every winter-snow biome rains.
  - `landscapeCreaturesFindTheirHaunts`: the werewolves', snow and shadow werewolves', squirrels', turkeys' and wisps' tags hold the landscape biomes they should, and not the others (prairie, tropics, steppe, the snowy lowlands, desert, fields).
  - `wispsComeOutOverLandscapeSwamps`: over a landscape bayou (made with `/fillbiome`), under the open sky and away from corn, a wisp may appear; over a landscape field it may not.
- **In CI** ([run 39845833481](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/39845833481), on d9741317a): every check passed, the `mod` job on its second attempt.
  - **First attempt:** one server game test failed, `LaboratoryLarderDiningGameTests.clockKeepsTheHour` ("The pulse ends on tick 12"). That test reads the grandfather clock 12 ticks after it starts. The clock pulses whenever the shared world clock's hour changes, so it fails when the hour rolls over in its last few ticks. It touches nothing this change does, and it passed on `main`. The [PR comment](https://github.com/jimbozoomer-byte/jugcraft/pull/454#issuecomment-6350982008) gives the cause and a proposed fix for that test.
  - **Re-run of the failed jobs:** all 2263 required server game tests passed, `SeasonalBiomeGameTests` among them. The job without the optional integrations passed on the first attempt.
  - **Client game tests:** the one class chosen for the changed files, `NightClientGameTests` (the wisps' and the other night creatures'), passed.
  - **The repository check:** passed.
- **Not run:** the client by hand (the colours in a landscape biome through the year), a dedicated server and play.

## World and event applicability
- Seasonal and Halloween content; nothing generates differently.
- Earned items and builds are kept after events end, as before.
- Not covered here, and why:
  - **The wild Halloween harvest patches and wolfsbane** were left out at first: permanent worldgen, reading the vanilla and common kind tags, which the landscape biomes lack. The follow-up below reaches them through the landscape kinds instead, without those tags, so wildlife and the other wild crops are as they were.

## Follow-up: the fall's wild patches, the graveyard flowers and the Hag Stone

On 23 October 2026 the owner closed the duplicate audit, [#464](https://github.com/jimbozoomer-byte/jugcraft/pull/464), and asked for what it added beyond this one as a follow-up on this audit's kinds ("close 464 and do the follow-up").

- **The fall's wild patches** (the 19 in `tools/landscape_kinds.py` FALL_PATCHES) also grow in the landscape biomes of their kinds. A landscape maple wood grows chestnut trees, chanterelles and acorn squash; a landscape bog grows cranberries; a landscape pasture grows butternut squash, Cinderella pumpkins and puffballs; the Ominous Woods grow wild mandrake and jack-o'-lantern mushrooms.
  - `JugcraftAgriculture.fallPatch` places them; beside each common kind tag a patch reads it also selects that kind's landscape tag (`LANDSCAPE_KINDS`, from `BESIDE`):

    | Common tag | Landscape tag |
    | --- | --- |
    | `#c:is_forest`, `#c:is_taiga`, `#c:is_birch_forest` | `#jugcraft:landscape_kind/woods` |
    | `#c:is_plains` | `#jugcraft:landscape_kind/grasslands` |
    | `#c:is_swamp` | `#jugcraft:landscape_kind/swamps` |
    | `#c:is_snowy` | `#jugcraft:landscape_kind/snowy` |
    | `#c:is_spooky` | `#jugcraft:landscape_kind/dark_woods` (new: the Ominous Woods) |

  - The savanna, jungle, hills, floral, windswept, river and badlands tags have no landscape counterpart. The landscape kinds there (arid, tropical, hills) are broader than any one common kind, and would put a savanna's pumpkins in a desert. So the bottle gourd (jungle and savanna) and the Jarrahdale pumpkin (savanna and windswept hills) still grow only in vanilla's and Jugcraft's own biomes.
  - Every other wild crop keeps to the common tags (`wildPatch`), so the landscape biomes' farming is as it was.
- **The graveyard flowers' wild patches** (`tools/plants.py` "patch") read the same kinds.
- **The Hag Stone** opens the Lantern Fen in the landscape swamps too: `#jugcraft:hag_stone_lands` takes `#jugcraft:landscape_kind/swamps`.
- **New chunks only** for the patches, as for any worldgen; the Hag Stone and the tags apply at once.
- **Checks:**
  - `check_landscape_patches` (from `check_seasons`) holds the Java's `LANDSCAPE_KINDS` to `BESIDE`; checks that every `FALL_PATCHES` name, and only they, is placed with `fallPatch`, which reads both tags; that the graveyard flowers' loop reads both; that every flower's patch kind has a landscape counterpart; and that the Hag Stone's lands hold the landscape swamps.
  - The older patch checks (each patch's biomes against `tools/agriculture.py`, and the agriculture switch's gating) parse `fallPatch` as well as `wildPatch`.
  - 12 deliberate breaks, each in a scratch copy of the tree, all caught.
- **Game test** `SeasonalBiomeGameTests.landscapeBiomesTakeTheFallsPatches`: whenever a vanilla forest, taiga, plains or swamp carries one of these patches in the test world, its landscape counterpart (maple woods, woodland, Siberian taiga, pasture, bog) carries it too, and the bog has no wild rice (no fall patch). It logs how many of the vanilla biomes carried theirs. The landscape swamps are in the Hag Stone's lands and a landscape field is not; the Ominous Woods are in the dark woods tag.
- **Run locally** on the follow-up: `python3 tools/generate_material_data.py`, `python3 tools/check_mod_data.py`, `python3 scripts/check_repository.py` and `python3 -m unittest discover -s scripts/tests` passed; the Java was parsed only (syntax). Compilation and the game tests run in CI on its pull request.
- **Not run:** a world. Nobody has seen a chestnut tree in a landscape maple wood.

## Rollout and open questions
- **Owner choices to confirm,** each one line in `tools/landscape_kinds.py`:
  - which landscape biomes count as magic or one-season (no seasons);
  - the Pumpkin Patch having seasons (it is a field);
  - the Scarlet Mountains going without (snow falls there all year).
- **Follow-up, not seasonal:** the landscape biomes have none of the vanilla or common biome-kind tags (`#minecraft:is_forest`, `#c:is_swamp` and the like). Wild crops and other mods' content therefore skip them. Giving them those tags is a separate change, because it alters spawns and new-chunk generation across those biomes.
  - **Wildlife:** the "13 species" counted here were tag files nothing reads. The menagerie's creatures in fact reached none of the landscape biomes, for want of `#minecraft:is_overworld`; they now do by their own habitat tags ([landscape-wildlife.md](landscape-wildlife.md)).
- **Owner visual check:** a landscape wood in autumn and in winter.
