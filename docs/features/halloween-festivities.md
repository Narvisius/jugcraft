# Halloween festivities

Status: implemented in source, not yet played. The Build workflow compiles it, and CI's game tests are recorded below.
Proposal issue: none; requested directly by the owner on 1 October 2026 ("What else could we add for the Halloween Seasonal Content come up with 10 ideas then lets do 5 at a time"). Of the ten ideas, the owner chose the first five (batch A) in one pull request: a carving contest with a judging stand, costumed mobs, the Halloween Peddler, spooky decorations for all year, and spooky sweets from the Cooking Pot. Batch B (will-o'-wisps, a pumpkin-chunking trebuchet, a bigger candy bag, the Harvest Moon and the Headless Horseman) comes later, separately.
Owner: @jimbozoomer-byte
Target milestone and tier: Milestone 3 (first homestead); Discovery tier (planks, wool, string, stone, bone, a candle, sugar and a Cooking Pot; no machines).
Primary specialty and supported player role: farming and play; supports groups (a contest everyone votes in), builders (graveyards, haunted arches) and cooks (sweets).

## Player experience
### The carving contest (entering any time, voting during the Halloween event)
1. **Put up a Judging Stand** (purple wool over planks): a draped pedestal with a gold rosette. Put a hand-carved pumpkin on top (a block in your hand is placed as usual).
2. **Enter it.** Use the stand with an empty hand. Only the pumpkin's carver can enter it; a pumpkin that was broken and placed again has forgotten its carver, so then whoever uses it first enters it. Taking the pumpkin off (or someone else recarving it) withdraws the entry. A player may enter several pumpkins, but votes count for the carver, not the pumpkin.
3. **Vote.** While the Halloween event runs, use someone else's entered stand with an empty hand to vote for its carver. Each player has **one vote per Halloween**; using another stand moves it. Nobody can vote for themselves. Sneak-use any stand to see the standings.
4. **Prizes.** When the event ends, the server announces the result, and the three carvers with the most votes get the Harvest Scale's **First, Second and Third Prize Ribbons** (ties go by name). A winner who is offline gets theirs on their next visit. Each Halloween is its own contest, decided once.

### Costumed mobs and the Halloween Peddler (only during the event)
5. **Costumed mobs.** During the event, each zombie, husk, skeleton, stray and zombie villager rolls once when it first appears: 15 % of them wear a costume: a Witch Hat, Ghost Sheet, Scarecrow Hat or carved pumpkin (never over a helmet). Like a helmet, it keeps undead from burning in daylight. The costume drops like any mob's gear (8.5 %), and a costumed mob killed by a player drops a sweet: candy corn (1–2), caramel, or one of the four spooky sweets.
6. **The Halloween Peddler.** During the event, every wandering trader arrives as the Peddler, in a Witch Hat. Besides its usual wares it sells four Halloween goods picked from thirteen: giant pumpkin seeds, the three heirloom pumpkin seeds, the three costumes, a Candle Skull, Hanging Ghosts and each spooky sweet. It only takes emeralds and buys none of these.

### Decorations (all year)
7. **Gravestones**: Rounded, Cross and Obelisk, cut from stone in a stonecutter. Use a Name Tag named in an anvil on one to engrave that name on its face (the tag is kept), or rename the gravestone itself in an anvil before placing it. Breaking it keeps the engraving as its name. Up to 50 characters, word-wrapped like a sign.
8. **Spun Cobweb** (five string make two): looks like a cobweb, but nothing sticks in it.
9. **Hanging Ghost** (white wool, string, black dye; two at a time): a little sheet ghost on a string under any block a lantern could hang from. It falls if that block goes.
10. **Candle Skull** (a bone block and a candle): a skull with a candle on its crown. Light it with flint and steel or a fire charge (light 12, a flickering flame); use it with an empty hand to snuff it.

### Spooky sweets (all year, from the Cooking Pot)
11. Four pieces a batch, eaten even on a full stomach (1 hunger each):

| Sweet | Cooking Pot | Effect |
| --- | --- | --- |
| **Glow Gum** | 2 sugar, glow berries, slime ball | Glowing, 30 s |
| **Ghost Taffy** | 2 sugar, phantom membrane | Invisibility, 3 s |
| **Fizz Rocks** | 2 sugar, gunpowder | Jump Boost, 20 s |
| **Witch's Licorice** | 2 sugar, wheat, ink sac | Night Vision, 45 s |

## Connections
- Existing input producer: hand-carved pumpkins and the Carving Knife (contest entries), the Harvest Scale's ribbons (prizes), the costume hats (mobs, the Peddler), the Cooking Pot (sweets), Halloween event timing (`HalloweenSeason`), Jugcraft candy corn and caramel (mob candy); vanilla wandering traders, undead mobs, stone, wool, string, dye, bone, candles, sugar and the sweets' odd ingredients.
- Existing output consumer: trophies (ribbons), food with effects (sweets), decoration, a second route to giant and heirloom pumpkin seeds and the costumes (the Peddler).
- Technology connection: none needed.
- Magic connection: none yet; the sweets are the first small "magic" foods, an easy hook for the magic branch later.
- Reachable entry path: every block and sweet has an all-year recipe from vanilla materials and the Cooking Pot; giant pumpkin seeds and costumes have their own routes (docs/branches/AGRICULTURE.md). Wandering traders and undead spawn naturally.
- Required vs optional: all optional. Nothing in progression needs a ribbon, a costume or a sweet; the Peddler and costumed mobs are extra routes, never the only one.
- How this stays useful without other branches: a reason for every player to carve and judge, decorations for any build, and sweets worth cooking.

## Balance and automation
- Contest: one vote per player per Halloween; at most 4,096 voters counted per contest and the latest 8 contests kept; ribbons have no use or value. Entering more pumpkins earns no more votes.
- Costumed mobs: 15 % of the five undead kinds during the event; one sweet roll per costumed mob a player kills (`data/jugcraft/loot_table/entities/costumed_mob_candy.json`: candy corn 1–2 (40), caramel (20), each spooky sweet (10)). A mob only rolls once, so a farm can't re-roll it.
- Peddler: 4 of 13 offers (`data/jugcraft/trade_set/halloween_peddler.json` and its `villager_trade` files; servers can change them), 1–4 emeralds each, 2–8 uses. It buys nothing, so no trade loop; every ware has another route.
- Sweets: four from 2 sugar and one or two cheap extras, 10 seconds a batch on heat; short effects (Ghost Taffy's invisibility lasts 3 seconds). No sweet converts back into anything.
- Decorations: one gravestone per stone; spun cobwebs from string (no way back); nothing here makes or breaks down into a currency.

## Multiplayer and persistence
- **Server authority.**
  - Entries, votes, standings and prizes are decided on the server. The client only sends the block use (vanilla checks its reach and spawn protection). The carver is read from the pumpkin on the server; the season and year from the server's clock and config, never the client's.
  - Engraving needs build rights (`mayBuild`, like editing a sign); the text is cut to 50 characters and control characters dropped on the server.
  - Costumes, candy and the Peddler's wares are rolled on the server.
- **Saved state.**
  - The contests (votes, entrants' names, decided or not) and prizes still owed are saved with the world (`data/jugcraft_carving_contest.dat`), so a restart keeps every vote and never awards twice.
  - A stand's entry is in its block entity. A gravestone's engraving is in its block entity and goes with the item.
  - A mob's roll and costume, and a trader's Peddler mark, are vanilla entity tags (`jugcraft.costume_rolled`, `jugcraft.costumed`, `jugcraft.halloween_peddler`) and equipment.
- **Bounded work.**
  - The contest check runs every 200 ticks (10 seconds): it walks at most 8 contests and the prizes owed.
  - A mob or trader is looked at once, when it enters the world; a death looks at one tag.
  - A vote touches one map entry.
- **Ending the event.** With the event off (out of its dates, or `halloween.mode=off`): no votes, no new costumes, no new Peddlers; the contest is decided and its ribbons given. Every ribbon, costume, sweet, decoration and engraving stays; a dressed mob keeps its costume and a Peddler its wares until it leaves. Switching the event on again in the same Halloween takes no new votes for the decided contest.
- New IDs only: blocks with items `judging_stand`, `rounded_gravestone`, `cross_gravestone`, `obelisk_gravestone`, `spun_cobweb`, `hanging_ghost`, `candle_skull`; items `glow_gum`, `ghost_taffy`, `fizz_rocks`, `witchs_licorice`; block entities `judging_stand` and `gravestone`. The `agriculture` switch turns off the new recipes, costumed mobs, their candy and the Peddler; placed blocks and items stay.

## Dependencies and assets
No new dependencies. Every texture is drawn by code (`tools/festivity_textures.py`): the stand's wood, velvet, hem and rosette, the gravestones' stone, the cobweb, the ghost, the skull and its candle, and the four sweets. The engraving is drawn by the client with the game's own font (`GravestoneRenderer`).

## Verification
Actual results (1 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares the contest, costumed mobs, Peddler, gravestones, candle skull and sweets with Java, checks each gravestone's shape is its model, that the Peddler only takes emeralds for things with another route, and that `villager_trade` tags name real trades) | Pass, 435 IDs |
| `./gradlew build` on `340bbb4` (the code of this pull request; later commits only change docs, screenshots and remove a temporary probe workflow) | Pass |
| Game tests on the headless server, same commit: 184 in total, 10 of them new here (`FestivityGameTests`) | **All 184 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `340bbb4`; no model or texture errors in the log; `[festivities test] engraving on the client: Here lies Jack O'Lantern, carved too deep` |

The 10 new game tests (`FestivityGameTests`):
1. through the stand itself: only the carver enters (anyone else is told whose it is), one entry per stand, nobody votes for themselves, voting again is "same", voting elsewhere moves the one vote, votes count per carver; taking the pumpkin off withdraws the entry; out of season no votes are taken and those cast stay;
2. **activation, restart, deactivation, duplicates and earned content:** entering works out of season but voting waits; in season votes count and nothing is awarded while it runs; the saved contest keeps every vote through a save and load; when the event ends the first and second carvers get their ribbons; checking twice more gives nothing; switching the event on again the same Halloween takes no more votes; the ribbons stay;
3. out of season no mob dresses up (and no roll is used); in season a lucky roll dresses a zombie in one of the four costumes, never twice, never over a helmet; cows don't; the costume stays after the event;
4. a costumed zombie killed by a player drops candy and an undressed one doesn't; the candy table loads and 50 rolls give only candy;
5. in season a wandering trader arrives in a Witch Hat, named the Halloween Peddler, with exactly four Halloween wares, all for emeralds, added once; out of season a trader is just a trader, and the Peddler keeps its hat;
6. an unnamed Name Tag engraves nothing; a player without build rights (adventure mode) engraves nothing; a named tag engraves its name and is kept; engravings are cut to 50 characters; a broken gravestone keeps its engraving as its name, and placing it again brings the engraving back;
7. flint and steel lights a Candle Skull (light 12) and wears by one; an empty hand snuffs it; a fire charge lights it and is used up;
8. a Hanging Ghost hangs under planks and goes when they do; a Spun Cobweb has no collision and is not a vanilla cobweb;
9. each sweet is always edible and gives exactly its effect for its time (Glowing 30 s, Invisibility 3 s, Jump Boost 20 s, Night Vision 45 s);
10. the eleven recipes and all thirteen Peddler trades load.

The client game test (`FestivityClientGameTests`) builds the three engraved gravestones with lit Candle Skulls, a spruce arch with Hanging Ghosts and Spun Cobwebs, a Judging Stand with a lit hand-carved pumpkin, the four sweets on a table, three costumed mobs and the Peddler, and photographs them by day, up close and at midnight. It also prints the engraving the client received.

Found by CI and fixed before this record:
- 26.3's `NestedLootTable.lootTableReference` takes a registry holder, not a key, and `ServerPlayer.drop` needs a prediction (compile errors). The candy now comes from rolling the data-driven table when a costumed mob dies, rather than from a pool added to vanilla's tables.
- The first screenshots showed no engraving. The client had the text (now logged), but small dark lettering on dark speckled stone was lost at preview size. The stone is now pale and the lettering bigger and darker.
- Voting after a contest was decided (the event switched off and on again the same Halloween) would have counted votes that are never awarded; it is now refused.

**Not run:**
- a person playing it in a client;
- a dedicated server with two players: two players voting, an offline winner getting their ribbon on their next visit (the code path is the same as an online winner's, checked every 10 seconds);
- the event ending on its own on a real calendar date (the tests switch the clock and the mode);
- costumed mobs spawning naturally in a real night (the tests roll the costume directly, with a seeded random source), and a wandering trader arriving on its own (the tests spawn one).

## World and event applicability
- **Seasonal rules** (docs/CONTENT_BRANCHES.md): the same operator settings as trick-or-treating (`halloween.start`, `halloween.end`, `halloween.timezone`, `halloween.mode` in `config/jugcraft.properties`); clients' clocks never count; votes and prizes are recorded on the server; ending the event only stops votes, costumes and Peddlers; nothing in progression needs it, and everything the Peddler or a costumed mob gives has an all-year route.
- Decorations and sweets work all year, anywhere.
