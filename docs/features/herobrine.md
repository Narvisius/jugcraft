# Herobrine

Status: parts 1–3 (encounters, wilderness marks, shoreline pyramids and the trail), the Jugcraft connections and OpenAI Codex's schedule are implemented on `claude/herobrine-haunting`, pending review. All 40 native Herobrine game tests, the 393 scheduler assertions, and the rendered and two-client tests pass. CI and a human playtest have not run yet (see "Verification" under Tests). Not merged, not deployed. The schedule, installed at server start, decides when he appears. Operators can also show him on command.

Owner: jimbozoomer-byte
Target milestone and tier: the Halloween event, any tier
Primary specialty and supported player role: exploration and atmosphere

## The owner's brief (27 October 2026)

- **Behaviour:** a watcher with harmless scares. He appears at a distance, stares, and vanishes when approached, stared at or hit. Now and then he jump-scares from behind with a sound and a moment of darkness. He never damages anyone.
- **Every player sees him, in rotation:** mostly at a distance, gone if a player gets too close.
- **Schedule:** extremely active now. High activity lasts through **3 November inclusive**, then becomes rarer from **4 November 2026, 00:00 America/New_York** (time zone and cutoff configurable). It does not taper straight after Halloween, and he still exists all year.
- **No chat:** he never speaks, and there is no chat impersonation.
- **World signs (later parts):** in unclaimed wilderness only he leaves redstone-torch shrines, small mossy and sand pyramids, cryptic signs, leafless groves and 2×2 tunnels, and rare massive brick pyramids near shorelines.
- **Look:** the classic Herobrine, which is the default player with glowing white eyes. The owner supplied their Planet Minecraft upload as the reference and allowed its pixels to be copied (see "His look" for what was done instead).

## Who builds what

The owner approved a plan shared between this branch and OpenAI Codex/Astra (coordination relayed by the owner on 27 October 2026):

- **Codex: the schedule and per-player progress.** This covers `io.github.jimbozoomer.jugcraft.herobrine.HerobrineSchedule` and `HerobrineScheduleDirector`, with versioned Overworld saved data. It owns the deterministic calendar (event mode through 3 November, quiet mode after), active-time accumulation, priority to unseen and oldest-overdue players, persistence across reconnect and restart, short retries after failed placement, reservations, rate caps, and crediting only confirmed sightings. Proposed pacing: in event mode, a first sighting within 5–10 active minutes, then roughly every 10–20; in quiet mode, a first sighting within 45–90 active minutes, then every 1–3 active hours; close encounters on their own long cooldown. These are targets, not guarantees: when there is nowhere visible to stand, nothing is shown.
- **This branch: everything else.** That is the entity and renderer, the live tick hook, placement, encounter content, terrain marks, structures and the links into the rest of Jugcraft.
- **The boundary is `creatures/herobrine/HerobrineDirector`.** Neither side builds a competing schedule. `HerobrineScheduleDirector` installs the schedule with `HerobrineEncounters.setDirector(...)` when the server starts, and removes it when the server stops.

## Part 1, implemented here

### The encounter

- **Distant.** Placement picks one of four variants, falling back to a watcher. Every variant stands on solid ground in a loaded chunk, with a clear line from the player's eyes to his, and turns to face the player.
  - **Watcher:** 28–52 blocks away under the open sky (14–28 underground), 20°–75° off where the player faces. Of 16 tries the best place wins: higher than the player (a ridge), open to the sky (a clearing), and in clear view of the other attentive players within 48 blocks, so friends together share one sighting. Under the open sky each try also weighs a cave mouth near the player's height: room to stand under a solid roof of natural ground, at least 3 blocks thick over his head, with a clear line to the player. A cave mouth beats a ridge in a clearing, so where a hillside opening faces the player he waits just inside its dark. A roof a player built (planks, say) is not a cave mouth.
  - **Follower** (a quarter of the time when the player is alone): he starts 18–22 blocks behind and walks the player's own path, keeping that distance. He freezes while on their screen. Every 6 ticks he walks, a faint step sounds for that player only, from 8 blocks behind them towards him (quiet sounds carry 16 blocks). When they stop, he and his steps stop.
  - **Second look** (a quarter of the time): once the player has seen him and then looked away for half a second, he is gone, or half the time standing somewhere else they could see.
  - **Tunnel** (tried first 60% of the time when the player is not under the open sky): he stands at the far end of the longest straight, covered, walkable run, 14–48 blocks, in a direction the player is not facing. The eyes wait down the tunnel behind them.

  He goes when anyone comes within 12 blocks, after the player has looked straight at him for 2 seconds (long enough to know what they saw), after 30 seconds, if he is hit, or if his player leaves. Operators can name a variant: `/jugcraft herobrine sighting <players> [watcher|follower|second_look|tunnel]`.
- **Close.** He stands 1.6 blocks behind the player. Only that player hears a heartbeat and a soul's sigh and gets 3 seconds of Darkness, which dims the screen and does nothing else. He goes once they have turned and looked at him for 0.6 seconds, or after 3 seconds.
- **Never harmful.** No attack, no damage, no loot, no world change. A blow or an arrow meets nothing and he is gone. He has no name tag or boss bar, is never saved with the world (`noSave`), and removes himself.

### Who counts as having seen him

- **Witnesses must be attentive.** A player counts as a witness only when he has been on their screen, unobstructed, within 96 blocks, for 10 ticks (or they hit him), and they are calm, awake and not idle. A camera that merely faces him is not enough. Any player can be a witness, not only the one he came for, so a shared sighting credits everyone who really saw him.
- **Calm** means no menu open, not dying or below 6 health, not hurt by a mob in the last 10 seconds, and not burning, in lava, falling or short of air.
- **Idle** means not having moved or looked round for 90 seconds.
- **Active players.** The tick hook calls a player active when they are in the Overworld, alive, not spectating or asleep, a minute past joining or respawning, and not idle.
- **Close encounters are limited.** Unless forced by an operator, one needs a calm player who has already seen him at a distance since the server started, so nobody's first sight of him is a jump-scare.
- **One encounter per player at a time.**

### The seam (`HerobrineDirector`)

Once a second, with `herobrine.haunting=on`, the hook calls `due(server, candidates)`. Each candidate is `(player, active, calm)`. The hook attempts each returned `Request(player, kind, reservation)` and reports back:

- `placed(request, encounter)`
- `failed(request, failure)`, with `TARGET_GONE`, `TARGET_UNSAFE`, `ALREADY_HAUNTED` or `NO_VISIBLE_SPOT`. Nothing was shown, so nothing should be consumed.
- `seen(request, encounter, player)`, once per player per encounter.
- `ended(request, encounter, outcome, witnesses)`, with `STARED_DOWN`, `APPROACHED`, `HIT`, `EXPIRED`, `TARGET_LOST` or `REMOVED` (including chunk unload).

`HerobrineEncounters.attempt` (any variant of the kind), `attemptVariant` and `attemptAt` (a chosen place) are the placement calls. An operator's forced encounter uses reservation `FORCED` (-1).

### Operators and settings

- `/jugcraft herobrine status` shows whether the haunting is on, encounters under way and players online. It also says when no schedule is installed.
- `/jugcraft herobrine sighting <players> [variant]` and `/jugcraft herobrine scare <players>` force an encounter, reporting where none could be placed.
- `herobrine.haunting` in `config/jugcraft.properties` (`on` by default; `off` stops scheduled encounters, and the commands still work).

### His look

The owner's upload is the default player's skin with white eyes: its palette is the default skin's colour for colour. The repository's licence policy forbids copying or recolouring game textures, so no skin was copied. `HerobrineRenderer` draws the game's own default player texture (`DefaultPlayerSkin.getDefaultTexture()`), read from each player's installed game at run time. Over it goes `textures/entity/herobrine_eyes.png`, drawn by `tools/herobrine.py`: clear except the four eye pixels, white and full-bright, so the eyes glow in the dark. The result looks like the upload. `tools/check_mod_data.py` fails if a `herobrine.png` skin is ever added.

### Marks he leaves (part 2)

After a distant encounter that his player really saw (any ending but losing the player or being removed), there is a 35% chance he leaves a mark where he stood. The kind suits the place:

- **Underground:** a straight 2×2 tunnel dug away from the player.
- **On sand:** a little sandstone pyramid with a chiseled cap.
- **Under trees:** a leafless grove, where every leaf the world grew within 8 blocks is taken and the trunks stand bare.
- **Open ground:** a redstone-torch shrine (a 3×3 mossy base, a netherrack heart and four redstone torches), a little mossy pyramid (5×5, 3×3 and 1), or a tunnel into a hillside.
- **Otherwise:** a lone sign.

Every mark carries a waxed sign facing the player, with a cryptic line: "YOU TOOK THE LONG WAY", "I WAS HERE FIRST", "COUNT THE WINDOWS", "STILL HERE", "THE WATER REMEMBERS" or "NOT YET". A sighting several players shared leaves "YOU BROUGHT SOMEONE". There is no fire, and nothing valuable is placed: no gold, no loot.

**Safeguards (`HerobrineMarkPlans`):**

- **Planning never loads a chunk.** Before any block is read, every chunk a plan could read (the site, its floor and roof, and 7 blocks round it) must already be loaded. At the edge of the loaded world the plan is simply refused.
- **Wild-looking land only.** A mark is planned only where the site and 6 blocks round it look like wild land:
  - unclaimed;
  - no column in the town;
  - every block natural-looking (ground, rock, ore, a plant, water);
  - no block entity, so no chest, machine, sign or spawner.

  A plank, a path, farmland, a placed leaf or a chest nearby keeps him away. This is a heuristic, not proof: unclaimed does not mean untouched, and a plain stone or log build, or a tree a player grew, looks natural.
- **Groves and tunnels fail closed.** The two marks that take blocks away are planned only where the whole site and margin lie inside land an operator has approved for them: `/jugcraft herobrine wilds approve <radius>` (16–256 blocks round where they stand, in the Overworld only), listed by `wilds` and withdrawn by `wilds revoke <number>`, saved with the world.
  - Each block taken away is checked against the approval again, so withdrawing it stops a grove or tunnel half built.
  - With nothing approved, there are no groves or tunnels at all. The trail begins with a shrine instead, and its signpost stands alone instead of at the end of a tunnel.
- **Building only adds.** Shrines, pyramids and signs take nothing away. They go only into air or plain ground cover (short grass, fern, dead bush, snow), never flowers or other plants a player may have set, never fluid. Each block rests on solid ground or on the mark itself.
- **The grove** takes only leaves that are not `persistent` (vanilla marks every leaf a player places persistent), and never one beside water. Logs stay.
- **The tunnel** digs only natural stone and soil. It stops before any ore (the common `c:ores` tag, modded ores included), any block touching a fluid, a roof of loose sand or gravel, or a missing floor. It runs 6–20 blocks, with a redstone torch every 5 blocks and a sign at the end.
- **Bounded work.** Building is spread over ticks: 8 blocks a tick, shared fairly among the marks building (at most 2), so a pyramid never starves a shrine. At most 8 sites are weighed per mark.
- **Rechecked before every block.** Just before each block changes it is checked again:
  - an unloaded chunk, or someone standing in the way, waits (up to 200 ticks);
  - land that became claimed or town, a removal whose approval was withdrawn, a block that is no longer what was planned on, or a block entity, stops the mark there.

  A mark never overwrites what a player did meanwhile.

**Records and undo (`HerobrineMarks`, saved with the world):**

- **Records.** Each mark has an id, its kind, where he stood and when, and every block it changed (before and after).
- **Undo.** `/jugcraft herobrine marks undo <id>` puts back, newest first, every block still exactly as the mark left it.
  - **Exact match.** It must be the same block in the same state, every property, and for a block that keeps contents, the same contents. A sign must hold the lines he wrote on both sides, filtered and not, plain and unstyled, in black, without glow ink, and still waxed. A lectern must hold the journal.
  - **What the world changes on its own** is allowed, and listed: a redstone torch's light, a glass pane's sides joining neighbours, and a lectern's pulse when a page turns.
  - **Anything else is a player's change**, and undo leaves it be and forgets it. That covers a block turned, waterlogged or replaced; a sign with any line, its back, a style, the dye, glow or wax changed; or a book taken or swapped.
  - **The journal goes with its pyramid.** A lectern still holding the pyramid's own journal is emptied just before it is taken away, so the book never drops loose. A lectern holding any other book stays, book and all.
  - **Waiting.** A block in an unloaded chunk, or where someone stands, waits in the record for a later undo.
- **Listing and forcing.** `/jugcraft herobrine marks` lists them. `/jugcraft herobrine mark <player> <kind>` leaves a mark of a given kind about 6 blocks in front of a player, for testing.
- **Switch.** `herobrine.marks=off` stops new marks; undo works either way.

### Shoreline pyramids and the trail (part 3)

- **The pyramid** (`HerobrinePyramids`): a stepped brick pyramid 31 blocks across and 16 high, one block narrower each side each layer. It has a solid floor and walls two bricks thick, and is hollow inside.
  - **Doorway:** on the landward side, at the floor and the step above.
  - **Windows:** twelve, two panes deep. Eight are on the fifth layer and four on the ninth.
  - **The chamber:** a netherrack altar ringed with redstone torches and topped with a skeleton skull, torches in the corners, cobwebs, two waxed signs ("COUNT THE WINDOWS", "I WAS HERE FIRST"), and a lectern holding one written journal. The journal is the only thing to take, once: five original pages of a surveyor's notes ("From the beach I counted twelve windows. From inside I count eleven."). No chest, nothing valuable, nothing that refills.
- **Where one may stand:**
  - all the land 6 blocks round the base is loaded;
  - no other pyramid within 768 blocks;
  - dry ground under all of the base, rising and falling at most 3 blocks (brick fills the dip);
  - water at the surface within 12 blocks of its edge;
  - every block it places is air or a plant, never water;
  - the whole footprint, volume and margin are wild land.

  The site is weighed up to 4 times, 24–56 blocks from where it is sought. It is reserved as soon as building starts: while it rises, and once finished, its record counts for the spacing. A pyramid stopped part-built (by a claim, a player's block or an undo) stays recorded and undoable, but no longer counts, so another may be raised instead. It is built through the same rechecked, recorded, undoable work as any mark, bottom up, and a block a player sets in its way stops it there. Window panes are placed already joined to the wall round them, so they look right whichever block was laid first.
- **When:**
  - very rarely after a sighting (3%), near where he stood;
  - as the end of a trail;
  - on `/jugcraft herobrine mark <player> pyramid`.
- **The trail.** The trail sits ahead of ordinary marks, and its progress is saved per player. A step counts only once its mark is finished, built to its last block. A mark stopped part-built never moves the trail on, and while a trail mark is building no other is begun for that player.
  1. A sighting across water (water at the surface somewhere between player and him) may begin it, half the time. He leaves a bare grove where he stood, or a shrine away from trees, with "YOU TOOK THE LONG WAY".
  2. A later sighting may continue it, half the time. He leaves a torch-lit tunnel whose end sign reads "THE WATER / REMEMBERS / NORTH-WEST / 312 BLOCKS", the way and distance to the nearest finished pyramid within 1024 blocks.
     - **Only finished pyramids.** A signpost never points to a pyramid still rising, stopped part-built or partly undone.
     - **None finished.** The sighting leaves no signpost. He raises a pyramid by the shore, unless one is already rising, and the trail waits. A later sighting points to it once it is finished, or raises another if it was stopped.

  Other sightings may leave unrelated marks, and some clues stay unresolved.

### Connections to the rest of Jugcraft

- **Guards glance.** A town guard (any guard role, the mecha guard included) that sees him within 32 blocks turns to look at him for 2 seconds, once per encounter, then goes back to its rounds (`HerobrineGlanceGoal`, one line in `Townsfolk`). It is a look only. He is neither a monster nor an enemy, so no guard's attack goal and no automated gun ever takes him as a target.
- **Styxhexenhammer knows.** To anyone who has seen Herobrine, Styx adds one of four lines that show he recognises it and will not explain it ("White eyes at the edge of the trees? The observatory's oldest logs have a word for that. I am not going to say it."). Whether someone has seen him is this session's record, or the schedule's saved one through `HerobrineDirector.hasSeen`.
- **The Cursed Mirror's reflection.** `HerobrineEncounters.reflection(player)` stands him quietly right behind a player who looks into the mirror: a faint sigh and no Darkness. It keeps the close-encounter rules (calm, already introduced, one at a time) under reservation `MIRROR`. The mirror is on its own branch (https://github.com/jimbozoomer-byte/jugcraft/pull/498), so the one call from `CursedMirrorBlock` is left for whichever of the two merges second; it is not wired on this branch.

## Tests

Server game tests (`HerobrineGameTests`, in the 48-block arena with padding) cover:

- he vanishes when someone comes close, ending unseen;
- staring him down ends the sighting and reports the player once;
- a blow makes him vanish, hurts nobody and credits the attacker;
- a close encounter stands behind, darkens and never hurts, and goes unseen and uncredited if the player never turns;
- a first close encounter is refused and one encounter runs at a time, with both failures reported;
- only attentive witnesses are credited (a badly hurt player facing him is not);
- a sighting spot is ahead of the player, at the right distance, on solid ground;
- under the open sky a watcher waits in a cave mouth facing the player rather than on the hill above it; open ground and a plank roof are not cave mouths;
- a walled-in player gets `NO_VISIBLE_SPOT` and nothing is placed;
- he is never saved, and players under a minute in are not active;
- scattered glimpses (6 ticks looking, 3 away, three times) are not a sighting, and a steady look is;
- a line of sight into unloaded land is refused and loads nothing;
- a newcomer's natural distant encounter is a watcher, and seeing it introduces them;
- a guard looks his way and never targets him, and its glance lasts two seconds of game time however often the goal is ticked;
- a mirror reflection is refused to a newcomer, then placed behind them without Darkness;
- Styx says nothing of him before a sighting, and recognises it after;
- a follower walks the player's own path at its distance, and stops when they stop;
- a second look finds him gone or moved;
- a tunnel encounter stands at the far end of a tunnel behind the player;
- a shared sighting credits both players who saw him, and not the one facing away.

Mark game tests (`HerobrineMarksGameTests`, on a raised platform so each margin stays in the arena) cover:

- a shrine is built with its sign's message and undone cleanly, with no record left;
- undo leaves a block a player changed;
- planks nearby, a claim, or leaves a player placed keep a mark away;
- a grove strips a wild tree's leaves and leaves its trunk;
- a tunnel digs 2×2 through stone, stops short of ore, and fills back in on undo;
- undo keeps a replaced block and a merely turned sign, and a sign whose back, style, dye, glow or wax was changed; an untouched sign is taken back;
- a shoreline pyramid is built with its top brick, floor, 24 window panes and the journal on its lectern, and comes down completely, its journal not dropped loose;
- a block set in a pyramid's way after it was planned stops the building there and is never overwritten;
- groves and tunnels are refused without an operator's approval beside a plain stone and log hut and a grown tree, let through by an approval, and closed again when it is withdrawn;
- planning far away, and just inside the loaded world's edge, is refused and loads no chunk;
- on the real build queue, a block a player sets after building began stops the mark at that block, and undo leaves it;
- a trail moves on only when its mark is finished, never when it is queued or stopped by a claim;
- a pyramid stopped part-built by a player's block is never pointed to. While it rises and after it stops, a trail sighting leaves no signpost and the trail waits. Once a pyramid there is finished, the next sighting's signpost points to it (east, 18 blocks) and the trail moves on when the signpost is finished.

The small-mark tests run on the real build queue: they wait for a free slot and for their mark to finish. The pyramid tests build at once (`HerobrineMarks.buildNow`), through the same checks. Every marks test runs with sky access, because game tests otherwise roof each structure with barriers.

Verification (27 October 2026):

- **Native game tests: all pass.** OpenAI Codex ran all 40 Herobrine game tests and the 393 scheduler assertions with `herobrineScheduleTest runGameTest`, filtered to `jugcraft-test:herobrine*`: build successful in 2 min 2 s. That run covered this branch's content through `49ae6532d` plus Codex's fixture and API corrections (`2ead9bce18`, `9f46697bfd`), now on this branch. It covers every test listed above: encounters, terrain planning, approved wilderness, building and undo, sign edits, finished-only trails and the guard's timing.
- **Rendered clients: pass.** `HerobrineClientGameTests` and `HerobrineNetworkTests` passed together in 3 min 29 s: two client JVMs on loopback, disposable profiles. The inspected screenshots show:
  - the player silhouette in daylight;
  - the white eyes in a dark tunnel;
  - the second client seeing the shared apparition in the world.

  Commands, logs and evidence paths: [herobrine-scheduling.md](herobrine-scheduling.md).
- **One line differs from the tested copy.** `HerobrineMarkPlans` builds the `c:ores` identifier with `Identifier.fromNamespaceAndPath("c", "ores")`, where the tested copy used `Identifier.parse("c:ores")`. Both name the same tag, and `Jugcraft.id` uses the same call.
- **Not yet verified:**
  - CI on this branch;
  - a human playtest of the atmosphere and sound (whether the footsteps are audible);
  - the marks, pyramids and cave mouth seen in a rendered client;
  - a public-server load test.
- **Known limits:**
  - a finished pyramid is not rechecked if players later mine it;
  - the Cursed Mirror's call waits for https://github.com/jimbozoomer-byte/jugcraft/pull/498.

## Still to build (owner-approved plan, not implemented)

- **A human playtest:** whether the follower's footsteps are audible, and how the cave mouth, the marks and the pyramids look and feel in play. The automated client tests already show the eyes in a dark tunnel.
- **The Cursed Mirror call:** one line in the mirror calling `HerobrineEncounters.reflection` (when a player looks into it at night, rarely), once both branches are on main.
