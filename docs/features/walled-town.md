# The walled town

Status: implemented, green in CI (server and real-world client tests); **not yet played** by a person. Results are under Verification.
Proposal issue: none. The owner asked for the town on 2 October 2026 and chose its size, place and currency (see "Owner approval" below).
Owner: @jimbozoomer-byte
Target milestone and tier: the start of every new world (Discovery). Nothing in it needs anything but walking there.
Primary specialty and supported player role: a shared hub. It serves traders and decorators, and gives every player a safe place to meet near the start.

## Owner approval
- **The request:** "a new NPC village / city that ALWAYS generates near spawn". It should be "walled, medieval", "have NPC's that look like players in it", be "a place where players can't break blocks or kill the NPC's", have NPCs that "seasonally update the décor and stuff around town", and offer "basic shops for seasonal and fun items". The owner's four reference pictures of walled medieval towns guided the look; nothing was copied from them.
- **Place:** near the start and visible from it.
- **Size:** about 160 blocks across, with a curtain wall, towers, gatehouses, a church with spires, a market square, and 40–60 houses and shops.
- **The new currency:** MAINTAINERS.md requires design review for a new currency, and this records the owner's approval. Asked what the shops should take, the owner answered: "Jugs (which is just the name of the currency they should actually be credits tied to the player accessible when at stores or tradable via an ATM in the city.)" Jugs are therefore credits the server keeps per player. There is no item, and players trade them only through the town's ATMs.

## Player experience
- **A walled medieval town stands 170 to 300 blocks from the start of every new world**, on the flattest dry ground the server finds there, so it is in sight of the start without covering it.
  - **The wall:** about 160 blocks across. It is stone with a crenellated wall walk, 13 round towers (alternately with conical roofs and flag-topped battlements) and three gatehouses (east, south and west). Each gatehouse has a drawn-up portcullis, and a path leads out of each gate.
  - **The streets:** a paved main street runs east to west through the market square, a paved street leads south, and lanes and closes reach the rest of the town. A lane runs round inside the wall.
  - **The market square:** a round fountain, benches, six market stalls with striped awnings, and a seasonal centrepiece.
  - **The landmarks:**
    - the church faces the square, with two spired towers, a rose window over the portal, aisles, a transept, an apse, stained glass, pews and an altar;
    - the Jugs bank has columns and four Jug Tellers (ATMs);
    - the town hall has a clock tower.
  - **The houses:** 44 half-timbered houses and shops, including a General Store, a bakery, two taverns and a tailor. They have stone or timber ground floors, jettied upper floors, steep roofs in seven materials, chimneys with smoke, shutters, window boxes, lit doors and furnished rooms.
  - **Gardens:** trees and flowers fill the gaps. Lamp posts line the streets and lanterns line the wall walk.
  - Every block is vanilla's, set by the generator's own design (tools/town.py). It is original: the reference pictures guided the idea only.
- **Townsfolk:** 32 named townspeople, player-shaped, in 18 skins drawn by code. No one can hurt them; only `/kill` removes one, and the town brings a new one to that place.
  - Four shopkeepers keep the General Store and the seasonal, curiosities and flower stalls.
  - Three stall vendors, a banker, the mayor, the priest, two innkeepers and a baker keep their places and chat.
  - Ten townsfolk stroll the streets.
  - Six guards walk near the gates and fight monsters that come in.
  - Three decorators change the town's decor when the theme changes.
- **Seasonal decor:** the town's theme is the running event (December, then Halloween, then the Harvest Feast), or else the season, from the server's calendar.
  - When the theme changes, the decorators walk round and change every decor site: 163 window boxes, 49 banners, 6 striped awnings, 16 planters, 38 lamp posts, 6 gate banners, 6 tower flags and the square's centrepiece.
  - **The centrepiece** is a maypole in spring, a flower bed with a young oak in summer, a harvest stack in autumn, a snowman in winter, a heap of jack o'lanterns under a cobwebbed dead tree at Halloween, a feast table at the Harvest Feast, and a tall tree hung with glowing baubles over wrapped presents in December.
  - **Lamps** burn with soul fire at Halloween. Townsfolk and decorators wear carved pumpkins at Halloween.
  - A site no decorator has reached after 5 minutes changes on its own, so the town is never left half-decorated.
- **Jugs:** the town's credit, shown in every shop screen and at the Jug Tellers. A player's first visit inside the wall brings 20 Jugs.
  - **The General Store** buys a farm's and a mine's ordinary goods for Jugs (crops, wool, leather, logs, coal, ingots, redstone, lapis and emeralds). It sells useful basics.
  - **The Seasonal Stall** sells what fits the theme: blossoms in spring, fireworks in summer, pumpkins in autumn, snow and ice in winter, candles and skulls at Halloween, pies at the Harvest Feast, and red and green candles in December.
  - **Curiosities** sells music discs, name tags, saddles, bells, item frames and the like.
  - **The Florist** sells flowers and saplings.
- **Jug Tellers (ATMs):** using one shows your Jugs and lets you send Jugs to any other player online.
- **The town is protected.** Players can't break or place blocks inside the wall or within 8 blocks of it (from 12 blocks under its ground to 96 above it). They can't use items on its blocks, fill or empty buckets, or harm its decorations. They can still open doors and gates, press buttons, pull levers, use crafting tables, sleep in beds and use the Jug Tellers. Explosions, fire and pistons can't change its blocks, and hostile mobs don't spawn naturally inside the wall. Operators in creative mode can change it.
- **Commands:** `/jugcraft town` shows where the town is and today's theme; `/jugcraft jugs` shows your Jugs. Operators can also use:
  - `/jugcraft town place` builds the town round them in a world that has none;
  - `/jugcraft town theme <theme|auto>` holds a theme;
  - `/jugcraft jugs give|take <player> <amount>`.

## Connections
- Existing input producer: the General Store takes vanilla crops, mob drops, logs and mined materials, and so turns surplus from farms (including the farming branch's machines) and mines into Jugs.
- Existing output consumer: the shops sell decoration and fun items, and Jugs pass between players at the Jug Tellers.
- Technology connection: none yet. Later shops can take the mod's own products; tools/town_shops.py is data.
- Magic connection: none yet.
- Reachable entry path: the town is built near the start of every new world, and nothing is needed to use it. The first visit brings 20 Jugs, and selling anything the General Store buys brings more. No circular unlock.
- Which connections are required vs optional; trade and solo routes: nothing in the town is required for progression. Every shop item is vanilla and obtainable elsewhere. Players can trade Jugs to each other or play solo.
- How this specialty stays useful without mastering every other branch: it needs none.
- For infrastructure/cosmetics, supported systems and reason resource links do not apply: the decor and the town's blocks are scenery, kept by protection.

## Balance and automation
- **Prices** are whole Jugs for a whole lot (tools/town_shops.py).
  - Buying: a stack of 16 torches costs 3 Jugs, a name tag 15 and a music disc 30.
  - Selling to the General Store: 16 wheat bring 3 Jugs, 8 iron ingots 8, and an emerald 3.
- **No profit loop.** No shop sells anything any shop buys. `tools/check_mod_data.py` and a game test check this, along with every item and price.
  - Emeralds are bought and never sold, so villager trading can't be looped through the town.
  - Farming for Jugs is production at the player's own pace, not a conversion.
- **Limits:**
  - A balance never goes below 0 or above 1,000,000,000.
  - Each player can make at most one shop trade every 2 ticks and send from a Jug Teller at most once every 10 ticks.
  - Shopkeepers trade within 8 blocks, and a Jug Teller works within 6.
- **The welcome gift** is 20 Jugs, once per player (`TownState` remembers who has had it).
- **Automation:** none. Shops trade only with players at the counter. Jug Tellers can't be crafted yet, so the town's four are the only ones.

## Multiplayer and persistence
- **Server authority:**
  - Placement, building, protection, decor, townsfolk, prices, balances and transfers are all the server's.
  - Shop and ATM screens send only vanilla menu-button clicks. The server checks the shopkeeper or Jug Teller is near, the offer is still on sale in today's theme, the price, the player's Jugs and items, the recipient is online, the maximum balance and the rate limits.
  - Clients are told only a townsperson's skin, a shop's id and theme, the names of players online at a Jug Teller, and the player's own Jugs.
- **Saved data:**
  - `data/jugcraft_town.dat` (Overworld) holds the town's corner, the chunks built, each site's theme, who stands at each place, the players welcomed and the current theme.
  - `data/jugcraft_jugs.dat` holds each player's Jugs.
  - Townsfolk save their role, shop, skin, place and home with their chunk.
- **Building:**
  - The town is built into chunks as they load, at most one chunk a server tick, and each chunk only once. The server never loads a chunk to build it.
  - Building a chunk levels the ground inside the wall to the town's height. It fills down to the land under it (up to 40 blocks), and clears land and trees above.
  - Outside the wall, the ground blends back to the land's own height over 12 blocks.
  - Blocks are set without neighbour updates or drops, as a structure template places them. Fences, panes, walls and stair corners are worked out by the generator.
- **The town's place** is chosen once, on a new world's first start (game time 0), after the world spawn is settled (after Alpine Spawn's move to the alpine village). The heights come from the generator's noise, so no chunk is generated to choose.
- **Switches:**
  - `town.enabled=false` places no town in new worlds.
  - `town.protection=off` lifts protection.
  - A town already built stays either way, with its townsfolk and shops. The Jug Teller, the townsfolk entity and the menus stay registered.
- **Existing worlds:** no town appears by itself. An operator can run `/jugcraft town place`, which builds it round them and replaces whatever stands there inside the wall.

## Dependencies and assets
- No new dependencies.
- **Data:**
  - tools/town.py writes `data/jugcraft/town/town.json.gz`: the blocks as palette indexes, the ground mask, decor sites, places and decor themes.
  - tools/town_shops.py writes `shops.json`.
  - The buildings come from tools/town_buildings.py, the themes from tools/town_decor.py and the text from tools/town_lang.py.
- **Textures:**
  - Townsfolk skins are drawn by tools/town_skins.py in the player skin layout.
  - The Jug Teller's textures and model come from tools/town_assets.py.
  - All are original; no Mojang file is copied. Vanilla blocks and items are used by ID.
- **Mixins** (four small ones, each asking `TownProtection.shieldsBlock`):
  - `TownExplosionCalculatorMixin` and `TownEntityExplosionMixin`: explosions skip protected blocks.
  - `TownFireBlockMixin`: fire in the town goes out on its first tick.
  - `TownPistonMixin`: protected blocks can't be pushed.
- **Fabric events:** block breaking, using blocks and items, attacking and using entities, entity loading (to stop natural hostile spawns inside the wall), chunk loading and server ticks.

## Verification
- **`python3 tools/check_mod_data.py`** checks that:
  - the town's data and shops are written and current;
  - the shops have no profit loop;
  - every townsperson's skin and every line they say exist;
  - every decor kind has every theme;
  - the feature switch and the shop's button numbering match Java;
  - the town's water stays put (`check_town_water`): no water block has air beside or under it, and no block that can hold water (a wall, a slab, stairs) touches two water blocks.
- **Server game tests (`TownGameTests`):**
  - `townDataReadsWhole`: every palette state is real, and every site kind shows something in every theme.
  - `shopsMakeSense`: every offer is a real item at a positive price, and nothing is both sold and bought.
  - `jugsStayInBounds`: no negative balances, no sending more than one has or to oneself, and the maximum holds.
  - `shopTradesCheckEverything`: buying and selling pay and take the right amounts, and refuse without the Jugs or the goods. A seasonal offer sells only in its theme.
  - `atmSendsJugsBetweenPlayers`.
  - `testTownIsBuiltAndKept`:
    - a test town far from other tests has its square's 12 chunks built and checked block by block against the data;
    - the town records one townsperson for each place;
    - a survival player can't break, place or pour there, an explosion leaves it as it was, and a lamp changes between Halloween and summer;
    - then, once the chunks tick their entities (the test waits for it), each recorded townsperson is in the world, one can't be hurt by a player, and a fire on the square goes out;
    - 80 ticks after that, no block of the town has become or stopped being air, or gained or lost water.
- **Client game test (`TownClientGameTests`):** a new world with seed `jugcraft` must have a town between 170 and 300 blocks from the start. The test waits over the middle of the town until every chunk is built, logs the chunks built and the townsfolk out, and takes screenshots:
  - the town from above (under the clouds);
  - the square in today's theme, at Halloween and in December;
  - the south gate, standing on the land outside it;
  - a shop screen and a Jug Teller's screen.
- Not run: play, two clients on a dedicated server, a restart, performance measurement.

### Results
- **Run [37080204071](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37080204071) (commit 6b904d4c, the latest): green** (`mod`, all three client shards, `client`; `repository` in run 37080204079).
  - Server game tests: all 507 required tests passed. `testTownIsBuiltAndKept`:
    - 171,148 blocks checked, 0 differ;
    - the square's chunks ticked their entities after 1,280 ticks;
    - all 10 townsfolk were in the world;
    - after ticking, 0 blocks had drifted.
  - Client game test, a real world with seed `jugcraft` (it starts in Alpine Spawn at 304 105 528):
    - the town's middle is at 432 97 656, **181 blocks from the start** (the test asserts 170 to 300);
    - 144 of 144 chunks were built;
    - the welcome message gave 20 Jugs (it shows in the overview);
    - 26 townsfolk were out round the camera, of every role;
    - the seasonal stall's shop screen opened, and the Jug Teller is in place.
  - Screenshots, looked at:
    - the overview, now from under the clouds (y 180, looking north over the south wall): the whole town's roofs, gatehouses and towers, hazy with distance;
    - the square, Halloween (soul lanterns, cobwebs, jack o'lanterns) and December (the tree of lights);
    - the gatehouse, from the ground 8 blocks below the town's floor.
- **Run [37078631445](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37078631445) (commit 8b8454a3, with the biomes and Alpine Spawn on main): green.**
  - Server game tests: all 507 required tests passed. `testTownIsBuiltAndKept`:
    - 171,148 blocks checked, 0 differ;
    - the square's chunks ticked their entities after 3,100 ticks;
    - all 10 townsfolk were in the world;
    - after ticking, 0 blocks had drifted.
  - Client game test, a real world with seed `jugcraft` (it starts in Alpine Spawn at 304 105 528):
    - the town's middle is at 384 106 672, 164 blocks from the start (see the fix below);
    - 144 of 144 chunks were built;
    - the welcome message gave 20 Jugs;
    - 25 townsfolk were out round the camera, of every role;
    - the seasonal stall's shop screen opened with its autumn offers, and the Jug Teller is in place.
  - Screenshots:
    - the square: the fountain's water stays in its basin and townsfolk stand about; the church faces it;
    - Halloween: soul lanterns, a dead tree hung with cobwebs and jack o'lanterns;
    - December: the tree of lights over its presents;
    - the gatehouse, seen from the land 10 blocks below the town's floor.
    - The overview was taken from above the clouds (the town's floor was at y 106), so it showed little. It is now taken from under them.
- **Fixed on the way, from earlier runs:**
  - **The fountain flooded the square** (runs 37077754832 and 37078159370: 247 and 219 blocks of water over the paving). The rim alternated walls and stone bricks. At its inside corners a wall touched two of the basin's water sources, and the game's infinite-water rule filled it (the test logged `stone_brick_wall[... waterlogged=true]` at 85 1 102), so it spilled outward. The rim is now full blocks, and the checker refuses such water.
  - **The town's distance.** The planner measured 170 to 300 blocks before setting the town's corner on a chunk border, which moved the middle up to 15 blocks. It now measures from the middle as built, and the client test asserts the range.
  - **Test timing, not the town:**
    - The test server races through ticks while the chunks round a forced chunk generate, so the test now waits until the square's chunks tick before looking for townsfolk or lighting the fire.
    - A render distance of 16 was too slow to render.
    - The gate camera stood inside the hill outside the wall.
- `PixelHollowsGameTests.everyVillageHasOneShop` (from #53) failed once on this branch, on a desert village with no room for the shop. It is not the town's; it passed on the re-run and in every other run. The fix (a village with no room is laid out again) is #122.

## World and event applicability
- **Biome fit:** the town takes the flattest dry ground near the start, whatever the biome. In Alpine Spawn worlds, that is near the alpine village the world starts beside.
- **Seasons and events:** the decor follows the server's calendar (seasons, the Harvest Feast and December from `season/`, Halloween from the agriculture branch).
  - Seasonal shop offers are on sale only in their theme. They are vanilla items, so nothing needed is seasonal.
  - Items bought in one season keep after it ends.
  - The town's blocks never change with the seasons; only its decor sites do.
- **Danger:** the town is a safe place. Hostile mobs don't spawn naturally inside the wall, and guards fight monsters that walk in.

## Rollout and open questions
- **Known limits:**
  - Endermen and zombies on hard can still take or break a block in the town; there is no repair yet.
  - The town's place in an existing world is wherever an operator stands.
- **Next:**
  - more shops taking the mod's own products;
  - a crafting recipe for Jug Tellers;
  - quests and more townsfolk lines.
- **Backup:** the town's blocks are ordinary blocks in the world. Removing the mod leaves them, and loses the townsfolk and the Jugs data.
