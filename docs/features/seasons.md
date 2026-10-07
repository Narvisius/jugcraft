# Seasons: colours, events and winter snow

Status: implemented on branch `claude/seasons` (PR #80). Compiles and passes its server and client game tests in CI (see Results). **Not yet played.**
Proposal issue: none.
- On 1 October 2026 the owner asked for seasonal biomes that change colour with the real date, and chose "Seasonal colours first" on a new branch.
- On 2 October they chose:
  - one season clock with a Thanksgiving Harvest Feast;
  - opt-in real snow cover;
  - seasons in every biome that has four seasons;
  - this upgrade before the Alpine Spawn biome.

Owner: @jimbozoomer-byte
Target milestone and tier: none; world atmosphere and events for every tier.
Primary specialty and supported player role: exploration and building (looks), seasonal events

## Player experience
- **Colours.** Grass and tree leaves in four-season biomes change colour through the year, following the **server's** date:
  - **Winter** (around January): dull, olive-brown and dormant.
  - **Spring** (March to May): first green, then a fresh, bright green.
  - **Summer** (July): the vanilla colours.
  - **Autumn** (September to November):
    - Leaves yellow from September.
    - In late October they turn gold, orange and red in patches about 12 blocks across, with smaller speckles, so neighbouring trees turn different colours.
    - By late November they are russet. Grass goes straw-coloured.
- Colours change a little each day (no sudden jumps) and blend smoothly into neighbouring biomes that have no seasons.
- **Winter snow (opt-in, `seasons.snow=on`).** From 1 December to 28 February (northern dates):
  - Rain falls as snow in the winter-snow biomes.
  - Snow settles on the ground, roofs and tree crowns, up to `seasons.snow_depth` layers (default 2).
  - From March it melts away layer by layer.
  - It never freezes water and never lies on farmland or paths.
  - It never touches snow the player placed, or the snow vanilla already has on cold mountains.
- **Events**, all on the same clock. An event is announced in chat when it begins and when a player joins during it (not while an operator previews a date).
  - **Harvest Feast** (Thanksgiving). By default it follows the US date: from the fourth Thursday of November over the weekend (26–29 November 2026). Operators can use Canada's date instead (the weekend up to the second Monday of October: 9–12 October 2026) or turn it off.
  - **December**: 1 December to 6 January by default.
  - The Halloween event, in the agriculture pull requests, joins this clock once both are merged.
- **`/jugcraft season`**:
  - Anyone can see the season, the day, the events running and the snow.
  - Operators can use:
    - `set <auto|spring|summer|autumn|winter|off>`;
    - `date <MM-DD>` to preview a date (season and events), and `date today` to end the preview and go back to the mode held before it;
    - `snow <on|off>`.
  - These changes last until the server stops; the config file is unchanged. A server starting, including the next world opened in the same game, begins from the file again.
  - A preview is not announced in chat, but seasonal content follows it as it would the real date (`JugcraftSeasons.isActive`): preview on a test server.
- **Biomes with seasons** (`#jugcraft:has_seasons`, listed in `tools/seasons.py`):
  - plains, sunflower plains and meadow;
  - forest, flower forest, birch forest, old growth birch forest, dark forest, dappled forest and cherry grove;
  - taiga, old growth pine taiga and old growth spruce taiga;
  - windswept forest, windswept hills and windswept gravelly hills;
  - swamp.
  - Not rivers: a river runs through jungles and deserts too, and its banks would turn autumn-gold there.
- **Winter snow biomes** (`#jugcraft:has_winter_snow`): the same biomes, plus the pale garden.
- **Unchanged all year:**
  - the tropics (jungles, mangrove swamp);
  - dry lands (deserts, savannas, badlands);
  - always-frozen biomes (snowy plains, ice spikes, the peaks, grove);
  - oceans, beaches and caves;
  - the pale garden's grey.

## Connections
- Input producer: the server's clock. Nothing in the game feeds it.
- Output consumer: looks only. Alpine Spawn's larch needles (`docs/features/alpine-spawn.md`) turn gold and bare with `today()`. No item, recipe, drop, spawn or progression step depends on seasons.
  - The seasonal snow block drops snowballs, as vanilla snow does, so snow adds nothing new.
  - `JugcraftSeasons.isActive(event)` and `today()` are the shared clock that seasonal content reads, such as the Halloween and Harvest Feast content in the agriculture branch.
- Technology connection: none.
- Magic connection: none yet.
- Reachable entry path: nothing to unlock.
- Required vs optional connections; trade and solo routes: not applicable.
- How this stays useful without other branches: it needs none.
- Cosmetics and data packs:
  - It works in every Overworld biome in the tags.
  - Other dimensions and modded biomes are unchanged unless a data pack adds them to the tags.

## Balance and automation
- No resources, units, conversions or rewards.
- Seasonal snow gives snowballs (1 per layer) when dug, exactly like vanilla snow layers. Snow is already unlimited in vanilla (snow golems), so this is no new source.

## Multiplayer and persistence
- **Server authority.**
  - The server works out the season day, the events and whether snow is falling.
  - It sends the day and the snow flag to each player when they join (`SeasonPayload`: a number and a flag).
  - It checks the date once a minute (`JugcraftSeasons.CHECK_TICKS` = 1,200) and tells everyone again when anything changes.
  - The client never reads its own clock.
  - Commands are checked on the server. Changing the season needs permission level 2.
- **Operator settings** in `config/jugcraft.properties`, read at startup:

  | Setting | Values | Default |
  | --- | --- | --- |
  | `seasons.mode` | `auto` follows the date; `spring`, `summer`, `autumn` or `winter` hold that season (for testing and off-season worlds); `off` gives vanilla colours | `auto` |
  | `seasons.hemisphere` | `north` or `south`. The south is half a year on: October there is spring, and its snow falls in June to August. | `north` |
  | `seasons.timezone` | an IANA zone such as `Europe/London`. It decides when the day turns over, for colours and events alike. | `UTC` |
  | `seasons.snow` | `off` or `on` (opt-in winter snow) | `off` |
  | `seasons.snow_depth` | the most layers winter lays, 1–8 | 2 |
  | `harvest_feast` | `us`, `canada` or `off` | `us` |
  | `harvest_feast.days` | how long the feast lasts, 1–7 | 4 |
  | `december` | `MM-DD..MM-DD` or `off` | `12-01..01-06` |

  An unreadable value falls back to its default and logs a warning.
- **Client.** A client mixin (`mixin/client/ClientLevelSeasonMixin`) adjusts each biome sample of grass and foliage tints.
  - The level's tint cache keeps the results, so this costs nothing per frame.
  - When the season day changes, the client clears the tint cache and rebuilds the chunk meshes once (about once a day).
- **Snow.**
  - A common mixin (`mixin/BiomeSeasonMixin`) turns rain into snow in winter-snow biomes while it is snowing. Rendering and weather rules then match vanilla's snowy biomes: rain does not water farmland, lightning does not strike, fires are not put out by the weather, Riptide tridents do not work, endermen are not hurt, and empty cauldrons fill with powder snow, as in vanilla's snowy biomes.
  - Biome temperature is untouched, so world generation, ice and vanilla snow are unchanged.
  - The season lays its own block, `jugcraft:seasonal_snow`. It looks like vanilla snow and is in `#minecraft:snow`, so grass under it turns snowy. Its light is reckoned by block, not by its shape (`useShapeForLightOcclusion` is false): vanilla kills grass under anything whose shape shuts out its light except one layer of vanilla snow, so grass under seasonal snow would otherwise turn to dirt over winter and have no grass to spread back from in spring. Below a full block of 8 layers grass lives; the layers still hide the faces they cover from rendering. It is placed by `SeasonalSnow`: 2 spots per player per tick, within 48 blocks, only in loaded chunks, only while it rains and not while the game is frozen (`/tick freeze`).
  - Once it is no longer snowing, the block's random ticks melt it.
- A client without the mod, or a server without Jugcraft, shows vanilla colours and rain. Leaving a server resets both.
- **Saved data.** Nothing is saved for colours or events. Seasonal snow is ordinary block data in loaded chunks:
  - It melts away in spring wherever it is ticked.
  - It also melts if snow is switched off.
  - Turning seasons off never unregisters the block, so old saves still load it.

## Dependencies and assets
- Fabric API only: networking, lifecycle events, the command API, and MixinExtras (bundled with Fabric Loader).
- No textures:
  - Colours are code (`season/SeasonPalette`).
  - Seasonal snow uses vanilla snow's models by name; no Mojang file is copied.
- No new dependencies.

## Verification
Results are recorded under "Results" below after CI runs.
- `python3 tools/check_mod_data.py` (offline) checks:
  - both biome tags match `tools/seasons.py`, and Java reads them;
  - every winter-snow biome but the pale garden has seasons;
  - the seasonal snow block's ID and its tags;
  - the palette keyframe and mode days lie within the year;
  - every `seasons.*`, `harvest_feast*` and `december` option exists.
- Server game tests (`SeasonGameTests`):
  - Calendar days: northern and southern hemispheres, leap years, New Year; every day of a leap year is within 1–365.
  - Mode overrides, and the time zone deciding the day at midnight.
  - A runtime override is not written to the config file, and a server starting goes back to the file (it runs what `SERVER_STARTED` runs).
  - Every test that changes the season puts the file's settings back even when it fails.
  - The palette:
    - summer and off are vanilla;
    - autumn leaves are redder, with alpha kept;
    - no day-to-day change is bigger than 4 levels per colour channel;
    - autumn patches include red and gold.
  - The biome tags and flags: four-season biomes in, the tropics, dry lands, frozen biomes, oceans and the pale garden's colours out; rivers have no winter snow.
  - Events:
    - US and Canadian Thanksgiving dates and windows;
    - December across New Year;
    - events off;
    - events in the southern hemisphere;
    - the snow season by hemisphere;
    - a preview date.
  - The command: `set winter`, `date 11-26` (Harvest Feast), `snow on` (none in November, falling on 10 January), `snow off`, `date today`.
  - Winter snow, in a patch of plains:
    - rain becomes snow in winter;
    - two layers lie, and no third past the depth;
    - no snow on farmland, on water (which stays water) or over vanilla snow;
    - grass under two layers stays grass (snowy) through random ticks;
    - in spring rain falls as rain again;
    - each random tick melts a layer, while vanilla snow stays.
- Client game test (`SeasonClientGameTests`):
  - A real client joins a flat world, receives today's day, and sees a grove of oaks in each mode, with checks on the tints.
  - Then winter snow with rain:
    - the client sees snow falling;
    - the grass under the season's snow is snowy;
    - a screenshot `jugcraft_season_winter_snow`.
- Not run:
  - a dedicated server with two clients;
  - a server restart across midnight;
  - real biomes in a normal world;
  - a whole winter of snowfall and a spring thaw in real time;
  - performance under many players.

### Results
Seasonal colours, from Build run [36915807158](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36915807158) on 86eae42 (Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25, GitHub-hosted Ubuntu, Mesa for the client), run on 1 October 2026:
- `./gradlew build`: all 125 game tests pass, including the five `SeasonGameTests` of that version.
- `./gradlew runClientGameTest`: pass.
  - The client had the server's season day as soon as it joined.
  - After each mode change, the client's day matched the server's.
  - Logged tints at one leaf and one grass block (vanilla plains foliage #77ab2f, grass #91bd59):

    | Mode | Day | Foliage | Grass |
    | --- | --- | --- | --- |
    | summer | 196 | #77ab2f | #91bd59 |
    | spring | 105 | #7aba35 | #8ec458 |
    | autumn | 293 | #e2aa27 | #a9b053 |
    | winter | 15 | #828243 | #98a468 |
    | off | 0 | #77ab2f | #91bd59 |
    | auto (1 October) | 274 | #ccab2a | #a4b354 |

- I looked at the screenshots (`jugcraft_season_<mode>`):
  - summer and off are identical and vanilla;
  - spring is a brighter green;
  - autumn shows orange-red and gold oaks side by side, with straw-coloured grass;
  - winter is dull olive-brown;
  - today's date (1 October) shows the oaks partly turned.
- An earlier screenshot round (run 36914731990) showed autumn as a flat olive-gold, because leaf textures darken the tint. The palette was strengthened, and the autumn patches made smaller, before these results.

Upgrade (events, command, more biomes, winter snow), from Build run [36950495928](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36950495928) on e9a0f11, run on 2 October 2026:
- `./gradlew build`: all 128 game tests pass. That includes the eight `SeasonGameTests`:
  - events;
  - the command;
  - winter snow lying, then melting in spring;
  - the new biome tags.
- `./gradlew runClientGameTest`: pass.
  - The tints are as above. Auto on 2 October is day 275: foliage #ceab2a, grass #a4b354.
  - Winter snow was logged as "client sees snow falling true, grass under the season's snow is snowy true".
  - I looked at the screenshot `jugcraft_season_winter_snow`: snow is falling, two layers lie on the ground and on the oak crowns, the grass is white under it, and the dormant winter leaves show beneath.

Later, on a branch built on this one (run [36968803297](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36968803297)), `winterSnowLiesAndMeltsInSpring` failed once ("Winter rain does not fall as snow"). Its plains covered only its own blocks, and a biome lookup blends the biome cells up to 5 blocks around, so near its edge it could read the natural biome of wherever the test happened to be placed (placement changes from run to run). The test now fills plains 5 blocks past every block it reads and checks that the fill took.

## World and event applicability
- **Seasonal rules** ([CONTENT_BRANCHES.md](../CONTENT_BRANCHES.md)):
  - **Activation** and **deactivation**: tested by switching modes (including `off`), previewing dates, and switching snow on and off.
  - **Timezone and manual override**: tested; one zone serves the colours and every event.
  - **Restart across the boundary**:
    - The season and events are worked out from the date on every check, and nothing about them is stored.
    - Overrides do not persist; this is tested.
    - Seasonal snow left at a restart melts if the new date is past winter.
  - **Duplicate rewards**: none exist.
  - **Earned content**: none is created.
    - Seasonal snow only lies on top of blocks and melts away, leaving them as they were.
    - Player-placed snow is vanilla snow and never melts this way.
- Old chunks are not rewritten. Colours change while chunks are drawn. Snow lies only in loaded chunks near players, and melts the same way.

## Rollout and open questions
- **Colours default on** (`seasons.mode=auto`), as the owner asked for colours that follow the real date. **Snow defaults off** (opt-in), because it changes the world while it lies: for example, it covers mob-farm floors in winter.
- **Fixed leaf colours.** Birch, spruce, cherry, poplar, mangrove and azalea leaves have fixed colours in vanilla, so they do not change. Their biomes still change grass, and oak and dark oak leaves change.
- **The Halloween event** (agriculture pull requests) has its own dates and time zone settings for now. Once both are merged, a small follow-up moves it onto this clock, as a third event window, and drops the duplicate time zone setting.
- **Content for the Harvest Feast and December**, such as a feast table or gifts, belongs to the branches that own the food and decorations. This change only provides the dated windows and the announcements.
- **Data packs that change tags** (`/reload`) are picked up the next time tags load.
- **Autumn colour work** is done for each biome sample the client blends, so at large biome blend radii a chunk rebuild costs more, and the blend softens the smallest autumn speckles. It happens only when chunks are drawn or the season day changes.
- **The Alpine Spawn biome** (next) will use all of this:
  - a large alpine valley where new worlds start, with villages;
  - its own seasonal features: larches that turn gold, then bare;
  - seasonal flowers and berries;
  - a moving snow line.
