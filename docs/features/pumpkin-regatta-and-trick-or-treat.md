# Pumpkin regatta and trick-or-treating

Status: implemented in source, not yet played. The Build workflow compiles it, and CI's game tests are recorded below.
Proposal issue: none; requested directly by the owner on 1 October 2026 ("What is the Pumpkin Regetta and Trick or Treating sell me on the idea of it and lets refine it!"). The owner chose both boat sizes, buoys with a timed board, treats only around Halloween, harmless villager pranks, a racer hollowed from a 2×2×2 giant, pumpkins plus three costumes, and one batch in one PR.
Owner: @jimbozoomer-byte
Target milestone and tier: Milestone 3 (first homestead); Discovery tier (a Carving Knife, wool, dye, paper, string, planks, wheat; no machines).
Primary specialty and supported player role: farming and play; supports groups (races against the clock, trick-or-treating round a village) and builders (regatta courses, porch lights, costumes).

## Player experience
### The pumpkin regatta (all year)
1. **Hollow a giant pumpkin into a boat.** Sneak and use the Carving Knife on top of a giant pumpkin 2 or 3 blocks wide (without sneaking, the knife only says how, so a prize pumpkin is never hollowed by accident).
   - A full-grown 3×3×3 giant becomes a **Pumpkin Barge**: four seats, two abreast. It keeps the giant's weight, its 48×48 carvings and its torch, so a carved, lit barge glows on the water at night.
   - A 2×2×2 giant (stopped there by cutting its vine or giving it no room) becomes a **Pumpkin Racer**: one seat, uncarved. It weighs 30 kg plus 2 kg a growth point (62–126 kg).
   - The pumpkin is gone, without its usual nine pumpkins. Instead come out its guts (2–4 from a racer, 4–8 from a barge) and, from a full-grown one, its 1–3 giant seeds. The boat goes to your inventory.
2. **Paddle it.** A pumpkin boat goes on water, steers and carries like a boat. Its weight sets its top speed in water against a vanilla boat: a racer from 1.30× (lightest) to 1.15× (heaviest), a barge from 0.95× (100 kg) to 0.70× (1000 kg). Its floor sits above the water line, so no water shows inside. Break it to get it back as an item, weight, carving and torch included; the item's tooltip shows its weight.
3. **Race.** Put a **Regatta Flag** (a chequered flag on a pole) on the shore and **Regatta Buoys** (red and white, four per craft) on still water. Use a buoy to count its number up (1–16), sneaking to count down.
   - Use the flag on foot: it finds its course (the numbered buoys within 64 blocks across and 16 up or down, one per number, nearest first), and shows the board of the three best times.
   - Use it from the driver's seat of a pumpkin boat: after a 3-second countdown the clock runs. Pass within 5 blocks of each buoy in number order, then come back within 5 blocks of the flag. Each buoy and the finish show your time.
   - The board keeps each racer's best time. The first time you place on a flag's board you get that place's **First, Second or Third Prize Ribbon**, the Harvest Scale's trophies, once per flag. Finishing a course earns the **Pumpkin Regatta** advancement.
   - Leaving the boat, taking over 10 minutes, or moving faster than 2 blocks in a tick (no paddle can; this stops boats on ice and teleports from setting records) ends the run without a time.

### Trick-or-treating (only around Halloween)
4. **Knock.** Use a **Candy Bag** (paper, string, orange dye) on a villager's wooden door. It knocks instead of opening it, and 1.5 seconds later the door is answered if:
   - the Halloween event is on (by default 20 October to 3 November, UTC; the server operator sets the dates, time zone and an on/off override);
   - it is between dusk and midnight;
   - you wear a costume on your head: a carved pumpkin (vanilla's or any hand-carved one) or one of the costume hats;
   - a **porch light** burns within 4 blocks of the door: a jack o'lantern, a turnip lantern, or any lit hand-carved or giant pumpkin;
   - someone lives there: a villager whose bed is within 12 blocks of the door.
5. **Treat.** The villager opens the door and hands you a treat: candy corn, caramel, cookies, a popcorn ball, a caramel apple or, rarely, the **King-Size Candy Bar** (8 hunger), only from villagers. In a costume hat, one time in four you get a second treat. Each villager's home gives each player **one treat a night**.
6. **Trick.** Knock at a home that has already given you a treat tonight and you get a harmless prank: a witch's cackle and a little hop, a flurry of bats and a moment of darkness, or the door slammed with a "no". Nothing is taken. Ten homes in one night earns the **Full Bag** advancement.
7. **Costumes.** The **Witch Hat** (black wool and purple dye), the **Ghost Sheet** (white wool and black dye; it drapes over the whole wearer, a hood with eye holes over the head and the sheet over the body and arms to below the knees, moving with them; you look out through its eye holes) and the **Scarecrow Hat** (wheat) go in the head slot and show on players and armor stands. Hand-carved pumpkins can now be worn too, like vanilla's carved pumpkin: you look out through the carving, and endermen take them as a disguise.

## Connections
- Existing input producer: the giant pumpkin and Carving Knife (boats), the Harvest Scale's ribbons (race prizes), Jugcraft candy corn, caramel, popcorn balls and caramel apples (treats), the turnip lantern, hand-carved pumpkins and lit giant pumpkins (porch lights); vanilla villages, wool, dye, wheat, paper, string, planks.
- Existing output consumer: food (treats), trophies (ribbons), advancements, decoration (costumes, flags, buoys).
- Technology connection: none needed.
- Magic connection: none yet.
- Reachable entry path: giant pumpkin seeds come from any pumpkin carved for the first time and from short grass (docs/branches/AGRICULTURE.md); wool, dye, wheat, paper and string are vanilla. Villages generate naturally; a player can also make a home for a villager.
- Required vs optional: all optional; nothing in progression needs a boat, a race or a treat. Every treat except the King-Size Candy Bar is also made by recipe all year; the bar is an optional seasonal treat.
- How this stays useful without other branches: a reason to grow a giant pumpkin past the weigh-off, a race any group can run, and a night out round a village.

## Balance and automation
- A boat costs a whole giant pumpkin: the barge gives up the 9 pumpkins a full-grown giant would drop (keeping its seeds), the racer the 4 a 2×2×2 one would. A boat never turns back into pumpkins, so there is no loop.
- Speed: each tick in water a boat's speed is multiplied by f, chosen so its top speed reaches the ratio for its weight (a boat's top speed is its push over 1 − 0.9, vanilla's water friction, so (1 − 0.9) / (1 − 0.9 f) = ratio). Weights: racer 62–126 kg, barge 100–1000 kg (its giant's weight).
- Ribbons have no use or value; each racer gets at most one per flag (the flag remembers the last 64 racers). Surveying a different course clears the board.
- Treats: one roll per treat of candy corn 1–3 (30), caramel 1–2 (20), cookies 1–3 (15), a popcorn ball (15), a caramel apple (10), a king-size bar (2) (`data/jugcraft/loot_table/gameplay/trick_or_treat.json`, a data-driven table servers can change). A costume hat adds a second roll 25 % of the time. At most one treat per villager's home per player per night: a village of ten homes gives about twelve treats a night, and only during the event, between dusk and midnight.
- No automation: knocking needs a player, a costume and a Candy Bag; one knock waits per player; the bag has a 2-second cooldown.

## Multiplayer and persistence
- **Server authority.** Clients only draw and send block uses.
  - Hollowing is checked on the server: the player holds a Carving Knife and may build there; the weight, carving and torch are read from the giant on the server.
  - Race timing, marks, the board and ribbons are all on the server, one distance a tick for each racing boat. The positions come from vanilla's own checked boat movement, and a run moving more than 2 blocks a tick is voided.
  - Trick-or-treating: the season comes from the server's clock and config (never the client's), the hour from the overworld clock, the costume from the server's copy of the head slot, the porch light and villagers from the world. Vanilla checks the reach of the block use.
- **Saved state.**
  - Boats keep their item (with `jugcraft:pumpkin_boat`: weight, torch, four 48×48 faces) in the entity and send it to clients. A run in progress is not saved; a restart ends it.
  - The flag keeps its course, board and ribbon list; a buoy's number is its block state.
  - Tonight's trick-or-treat record (the night and, per player, the homes that gave) is saved with the dimension (`data/jugcraft_trick_or_treat.dat`), so a restart never gives a home's treat twice. A new night replaces it.
- **Bounded work.**
  - A racing boat checks one distance a tick.
  - A flag's survey reads the block entities of at most 81 loaded chunks, only when used; it never loads chunks.
  - A knock reads at most 486 blocks for a porch light and lists villagers within 24 blocks; the record keeps at most 256 homes for at most 1024 players a night.
  - Each player has at most one knock waiting and one door waiting to close; both are dropped when the player leaves or the server stops.
- **Ending the event.** With the event off (out of its dates, or `halloween.mode=off`), doors are not answered. Every treat, costume, advancement, boat and block stays, and the record means starting the event again the same night gives no second treat from a home.
- New IDs only: entities `pumpkin_barge` and `pumpkin_racer`; items `pumpkin_barge`, `pumpkin_racer`, `candy_bag`, `king_size_candy_bar`, `witch_hat`, `ghost_sheet`, `scarecrow_hat`; blocks `regatta_flag` and `regatta_buoy`; the `jugcraft:pumpkin_boat` component. Hand-carved pumpkins gain the `equippable` component (head), so they can be worn; nothing about placed ones changes. The `agriculture` switch turns off the new recipes; placed blocks, boats and items stay.

## Dependencies and assets
No new dependencies. Every texture is drawn by code (`tools/regatta_textures.py`): the boats' cut flesh, the ghost sheet's view and the sheet as worn (`entity/ghost_sheet`, drawn over the wearer by the client's `GhostSheetLayer`), the flag, buoys, hats, bag, candy bar and boat icons. The boats are drawn from the giant pumpkin's own side tiles. The hats are 3D item models.

## Verification
Actual results (1 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also compares every boat, regatta and trick-or-treat number, tag and table with Java, checks every buoy number has a model, and allows a vanilla tab's root as an advancement's parent) | Pass, 424 IDs |
| `./gradlew build` on `21a98ac` (with the Halloween harvest and everything under it, and `main` after #57, merged in) | Pass |
| Game tests on the headless server, same commit: 174 in total, 14 of them new here (`RegattaGameTests`) | **All 174 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`) | **Passes** on `21a98ac`; no model or texture errors in the log; `[regatta test] pumpkin boats afloat: 2` |

The 14 new game tests (`RegattaGameTests`):
1. hollowing a full-grown, carved, lit giant from the top gives a barge keeping its weight, carving and torch; all 27 blocks go, 4–8 guts and giant seeds come out but no pumpkins, and the knife wears by one;
2. the knife on top of a 2×2×2 giant only hints until the player sneaks, then makes a racer weighing 30 kg plus 2 kg a growth point, with guts and no giant seeds;
3. each kind's speed ratio runs from its lightest to its heaviest weight, and the tick factor reaches exactly that top speed;
4. a barge seats four (a fifth is refused), a racer one; a broken barge drops its item with the same data;
5. a buoy floats on water, not on land; using it counts up, sneaking counts down past 1 to 16; it goes when its water does;
6. a run: the flag finds buoys 1 and 2 in order; only the driver can start; no mark counts during the countdown; sitting within reach of both buoys and the flag, the run passes them on three ticks and the board records 2 ticks; the winner gets one first-prize ribbon, a better time replaces the old one without a second ribbon, and leaving the boat voids a new run;
7. the Halloween window: both ends included, wrapping past New Year, the operator's time zone (still on in New York at 02:00 UTC on 4 November, over in UTC), and `on`/`off` overriding the dates;
8. **activation and duplicates:** in season at dusk, in a carved pumpkin under a porch light, a home gives a treat; knocking again the same night gives a trick and no treat; the next night it gives again;
9. **conditions:** nothing out of season (`off`), at noon or after midnight, without a costume or without a porch light, or with nobody's bed near; a lit hand-carved pumpkin counts as a porch light;
10. **restart, deactivation and earned content:** the night's record survives a save and load and refuses the same home again; with the event off nobody answers but treats and the costume stay; on again the same night, no second treat;
11. ten villagers' homes behind one door give ten treats, then a trick, and Full Bag is earned;
12. a Candy Bag used on a door through the game's own block-use path knocks without opening it; one knock waits per player; only wooden doors take one; after the answer the player may knock again;
13. the three hats and all four hand-carved pumpkins are worn on the head (the pumpkins not swapped on by a right click, with the carved-pumpkin view), count as costumes, and the pumpkins disguise a gaze; the ghost sheet has its view;
14. the six recipes, the treat and hollowing tables and both advancements load, and 50 rolls of the treat table give only its sweets.

The client game test (`RegattaClientGameTests`) builds a pond with a carved, lit barge with two villagers aboard, a racer, four numbered buoys and the flag, and a door with a jack o'lantern porch light beside armor stands in a carved pumpkin, the three hats and a hand-carved pumpkin. It photographs them by day and at midnight. Screenshots are taken with the HUD hidden, so the ghost sheet's view (a camera overlay) is not photographed; test 13 checks the sheet has it.

Found by CI and fixed before this record:
- 26.3 has no `PushReaction.POPPED` (`POPPED`), its `PoseStack` turns with `rotateDegrees`, and vanilla entity types are looked up by ID in tests (compile errors).
- Test 10 first checked the saved record by the clock time instead of the night number it is kept by; the test was wrong, not the game.

**The Ghost Sheet after the owner's first look (1 October 2026).** It didn't fit over the character: as a head-slot item the game drew its small block model on the head only. A render layer now drapes the whole sheet over the wearer (every humanoid: players, costumed mobs, armor stands), fixed to the head, body, arms and thighs so it moves with them. Its equipment asset has no layers, which keeps the armor layer from drawing anything and the item from being drawn on the head, and is what puts it in the render state the layer reads. Results on `449f61b` (with the Halloween harvest's giant pumpkin and scarecrow fixes merged in):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py`, `python3 tools/check_mod_data.py` | Pass (453 IDs) |
| `./gradlew build` and the game tests on the headless server | **All 199 pass** |
| Client game test (`RegattaClientGameTests`), with a close-up of the sheet on its armor stand and a third-person shot of the player wearing it | **Passes**; screenshots `ingame_ghost_sheet.jpg`, `ingame_ghost_sheet_worn.jpg` |

Found by CI on the way: 26.3 has no `RenderTypes.entityCutoutNoCull` (its `entityCutout` already draws both sides), and the first working build drew nothing, because 26.3 only puts head equipment with an equipment asset in the render state (the screenshots showed a bare head); the empty asset fixed it.

**Not run:**
- a person playing it in a client, paddling a pumpkin boat (speed in water is only checked by its formula);
- a dedicated server with two players: racing each other, two players at the same door;
- trick-or-treating in a real village at night (the tests use villagers with homes set, not grown villages), and a restart of a real world across the event's end;
- performance with many boats or racers.

## World and event applicability
- **Seasonal rules** (docs/CONTENT_BRANCHES.md): the operator sets `halloween.start`, `halloween.end`, `halloween.timezone` and `halloween.mode` (`auto`, `on`, `off`) in `config/jugcraft.properties`; clients' clocks never count; timing and the record of who got what are on the server; ending the event only stops answers; nothing in progression needs it, and every treat but the optional King-Size Candy Bar has an all-year recipe.
- Villages: trick-or-treating uses villagers' own home beds (their `home` memory). It never moves, hurts or trades with a villager.
- The regatta works all year, in any water, in any dimension with water.
