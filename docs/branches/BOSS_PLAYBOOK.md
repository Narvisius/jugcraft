# How a boss is built (the boss playbook)

Status: the plan for building every boss, written on 10 October 2026 at the owner's request ("document the order in which you made that boss and save that as a plan for how bosses should be designed"). It is the order the Yeti King was built in: part 1 in [jimbozoomer-byte/jugcraft#300](https://github.com/jimbozoomer-byte/jugcraft/pull/300), part 2 in [jimbozoomer-byte/jugcraft#304](https://github.com/jimbozoomer-byte/jugcraft/pull/304). Madame Tatterlace and Vesperine were built the same way before him. Records: [glacier-hall.md](../features/glacier-hall.md) and [yeti-king.md](../features/yeti-king.md).

It adds no rules of its own. It puts in order the rules every boss already follows ([BOSSES.md](BOSSES.md#rules-every-boss-follows)), the project's instructions (CLAUDE.md, [CONTRIBUTING.md](../../CONTRIBUTING.md), [TESTING.md](../TESTING.md)), and the planning pack's shared lair contract and boss acceptance ([chapter 04](../plans/concordance-expansion-2026-10-10/04-existing-bosses-and-shared-encounters.md), [chapter 10](../plans/concordance-expansion-2026-10-10/10-validation-and-checkins.md)). Where they disagree, they win.

## The shape: one boss, two pull requests

1. **Part 1: the lair and the summoning that opens it.** On its own it is playable: a player can open an instance, walk the arena, use its fixtures and leave. The arena is fixed before the fight is tuned against it.
2. **Part 2: the boss, his adds and his loot**, stacked on part 1.

Each part:
- has its own feature record, game tests and a client game test that takes pictures;
- goes green in CI before its pictures go into its record.

Each is one reviewable scope.

## Step 0. Pick the boss and survey

- **Pick:** the owner picks the boss. Otherwise take the next in [BOSSES.md](BOSSES.md)'s list, whose trophies and loot table (`bosses/<boss>.json`) are already built.
- **Read the planning pack:** its [compendium](../plans/concordance-expansion-2026-10-10/05-new-boss-compendium.md) may already propose an arena, a loop and attacks for this boss.
- **Refresh:** fetch `main` and list the open pull requests. Nobody else should be building the same boss or lair, and nothing in flight should be about to collide with it.
- **Survey what the boss draws on:**
  - its home biomes;
  - the blocks, mobs, effects and items of its theme;
  - its two Arms VII trophies (`tools/arms_variants.py`);
  - any magic school it touches;
  - the lair framework (`lair/Lair.java`, `Lairs`, `LairInstance`, `LairRules`, `LairBosses`, `GraveGoods`, `MistGateEntity`);
  - the last lair added, as the model to copy.

  For the Yeti King this meant the cold biomes, powder snow and freezing, the Glacier Maul and the Rimeclaw, and the Spindle Loft's code.

## Step 1. Write the plan

Write the boss's plan as a section of [BOSSES.md](BOSSES.md), like "The Yeti King: the plan". It covers:

- **The lair:** its size, look and parts. These are an arrival ledge, the arena floor and its special ground, fixtures, safe places, the boss's waiting place and the Grey Mist's place.
- **The summoning:**
  - the item and its recipe, from things players already make;
  - where and when it works;
  - what refuses it;
  - what is spent, and when.
- **The fight:**
  - the waiting and waking;
  - the phases, with thresholds;
  - every attack, with its tell, timing, reach and damage;
  - the **arena rule**, the one mechanic only this boss has (the Yeti King bares the lake's snow to glare ice, and a charge into a column stuns him);
  - the adds and their cap;
  - the enrage;
  - what happens when he is left alone.
- **The loot, per participant:**
  - a material useful to non-fighters;
  - one of his two trophies, certain on a first kill;
  - a useful item and a costume;
  - shared experience and an advancement.
- **The shared lair contract** (below), answered for this lair.

Keep the numbers in the pack's envelope:
- 320 to 480 health solo, scaled 50% per extra player up to 2.5 times;
- armour 6 to 12;
- ordinary tells 0.6 to 1.2 seconds, arena actions 1.5 to 3.

## Part 1: the lair and the summoning

**2. Lay the lair out in Python.** `tools/<lair>.py` builds the template as named regions, and states the constants the Java will read: the arrival, the centre, the bounds, the floor and the places the fight looks for. Its lair-only blocks go in `tools/lairs.py`. They cannot be broken and drop nothing. Their textures come from the shared painters (`tools/lair_textures.py`, `tools/<lair>_textures.py`); no Mojang texture is read, traced or copied. Dimension type, biome and template data come from `tools/lair_data.py`. *Yeti King: `tools/glacier_hall.py`, `tools/glacier_hall_textures.py`.*

**3. Register the lair in Java.**
- `Lair.java`: a new entry with the size, arrival, centre, bounds, floor and moon, held to the Python by the audit.
- `JugcraftLairs`: the lair-only blocks.
- A class of its own for any block with behaviour, such as `TrampledSnowBlock` and `GiantIcicleBlock`.

**4. Make the summoning.** An item, and a rite that the server works through. The Yeti King's are `FrostHornItem` and `FrostHornRite`. The rite checks:
- the time and the place;
- the ground;
- the player's permission;
- that an instance slot is free.

It then opens an instance, opens the gate where it was used, and brings the player to the arrival. A refused summoning spends nothing. Test every refusal.

**5. Write the data.**
- The item's recipe from existing producers.
- Its icon map: `tools/item_icons/<item>.txt`, with materials from `tools/icon_materials.py` that pass `tools/check_icon_maps.py`.
- Lang and tags.

**6. Audit it.** Add a `check_<lair>` to `tools/check_mod_data.py`. It holds the Java constants to the Python tables and checks the template itself: clearances, the floor under every place a fight will use, headroom on ramps, and no block that melts or burns where it must not. Then break the Java on purpose, one change at a time, and see each change fail the audit. The hall took seven. Fix the audit wherever a break slips through.

**7. Test it.**
- **`<Lair>GameTests`:** the template loads at its size with its own blocks; the blocks keep their rules; each summoning check and refusal; a full lair refuses.
- **`<Lair>ClientGameTests`:** a real summoning in survival. It opens an instance, arrives, uses the gate both ways, takes the fall toll, leaves, and closes the instance. Then it takes a picture of each part of the lair.

**8. Write the record and run the local checks.**
- **Record:** `docs/features/<lair>.md` from [TEMPLATE.md](../features/TEMPLATE.md), giving tier, input producer, output consumer, costs, unlocks, failure behaviour and test evidence. Add a [CHANGELOG](../../CHANGELOG.md) entry and a link from the boss's plan.
- **Checks:**
  - `python3 scripts/check_repository.py`;
  - `python3 tools/check_mod_data.py`;
  - `python3 tools/check_icon_maps.py` on the new maps;
  - the generators (`tools/generate_material_data.py`, `tools/generate_textures.py`), then revert any PNG they re-encoded with identical pixels;
  - `javac` on the new Java files alone, for syntax only. The game compiles only in CI.

**9. Open the pull request and take it to green** ([CI](#ci-getting-to-green), [pictures](#pictures)).

## Part 2: the boss, his adds and his loot

**10. Numbers first.** `tools/<boss>.py` holds every number:
- health and armour;
- the phase thresholds;
- each attack's wind-up, active ticks, cooldown, reach and damage;
- the adds and their cap;
- the arena rule's timers;
- the loot chances;
- the messages and the advancement.

The Java reads the same numbers, and the audit checks they agree. *Yeti King: `tools/yeti_king.py`.*

**11. Model and animate.** `tools/<boss>_models.py` uses `tools/vesperine_models.py`: bones, poses, clips and controllers for the boss and every add or projectile. Preview every pose with `tools/geo_preview.py`, flat-shaded and painted, before any Java plays it. Learned on the Yeti King:
- two controllers never share a bone;
- a pose held in a loop needs its own sway, because the waves are ignored on a keyed channel;
- an arm raised past level swings the other way, so work out "out" from whether the arm is raised.

**12. Paint.** `tools/<boss>_art.py`:
- the sheets at the resolution the model needs (the King's is 1024 square);
- a glowmask for what burns or shines;
- the item icons;
- the clean style's painters.

The art checks report exposed faces (O1); fix them in the model.

**13. Write the data.** `tools/<boss>_data.py`:
- geo and animations for each entity;
- lang;
- the loot table, rolled per participant as a gift, so nothing in it reads the killer;
- the costume model and its tags;
- recipes for the material, with no loop that gains;
- the advancement.

His two trophies come through `tools/arms_variants.py`. Hook the module into the generators (`tools/agriculture_data.py`, `tools/crop_textures.py`).

**14. Write the Java.**
- **The boss:**
  - a phase machine: waiting, waking, the phases and the fall;
  - attacks as wind-up, active and cooldown;
  - the arena rule;
  - a leash, and a reset when he is left alone;
  - party health through `LairBosses`;
  - immunities;
  - saving only what must survive a reload. The King saves the cells he has bared, so the snow can drift back.
- **His small entities:** adds, projectiles and hazards, each capped, owned by him and cleared with him.
- **His loot:** per participant, with the trophy certain on a first kill.
- **His items, the registrations and the GeckoLib renderers** on the client side only.

The server decides every hit, every reward and every block the fight changes.

**15. Check every API against 26.3 before pushing.** The game only compiles in CI. List each import and member the new code uses, and confirm each is already used somewhere in the repository, or else check it. The Yeti King's first run failed on `hurtMarked`, which 26.3 does not have. Knockback is sent with `ClientboundSetEntityMotionPacket`, as the spider's spool does.

**16. Audit it.** Add a `check_<boss>` that checks:
- the Java numbers and attacks against the Python;
- that where the fight looks for the lair's parts is where the layout builds them;
- every clip the Java plays exists;
- UV regions, sheet sizes, the glowmask and controllers' bones;
- loot against the Arms VII trophies;
- recipes, tags, the advancement and messages;
- banned APIs (no chunk loading, no changing dimension).

Break the Java on purpose, one change at a time, and see each fail; the King took ten. Two first slipped through, and the audit was fixed.

**17. Test it.**
- **`<Boss>GameTests`:** put a boss down on his own in a lair-sized arena, a structure under `src/gametest/resources/data/jugcraft-test/gametest/structure/`, with the floor laid out from code. Write one test for each attack, rule, phase and loot path.
- **`<Boss>ClientGameTests`:** runs the whole fight in a real lair. Each attack is begun, held as it lands, and pictured; then the fall, the loot, the advancement and the reset or return.
- **Mock players** wait out their loading in creative before anything can hurt them.

**18. Write the record.**
- `docs/features/<boss>.md`.
- In CHANGELOG, BOSSES.md, the trophies' record ([arms-vii.md](../features/arms-vii.md)) and [TECH_TREE.md](../TECH_TREE.md): the trophies now drop.
- Run the local checks of step 8.

**19. Open the stacked pull request and take it to green.**

## CI: getting to green

- **Compile errors** show only in CI. Read the job's log, fix, re-check the API list, and push one validated fix.
- **A new test structure file** counts as a shared file to `tools/select_client_tests.py`. Every push to that pull request then runs every client test class, about 24 minutes a shard.
- **A push cancels the run in progress.** If the client shards are still taking pictures you want, hold the push until they finish, and fold any other fix into it.
- **A failure in code the pull request does not touch:**
  - Show it is not this pull request's, for example from:
    - the other server job on the same commit;
    - the batch order (the boss's tests are registered last);
    - an identical earlier commit that passed.
  - Look for an open pull request that fixes it, and port that fix unchanged. The javelin's came from #303.
  - Otherwise say once on the pull request what failed and why it is not this one's, then re-run the failed jobs once.
  - Never skip or disable a test.

## Pictures

The client test takes the pictures, and CI prints each one in its shard's log. Extract them, and look at every one before it goes in the record:

- **The camera follows the boss.** Aim it from where the boss is after the pose, not from a fixed offset. The Fury's camera was put by the lake's centre, and it ended up in his face.
- **Small things need a plain background.** Shoot them close and low against it. The icicles were lost among the vault's own until the camera moved in to seven blocks.
- **Re-aim, re-run, look again.** The spider's pictures took three runs, the King's two.
- **Captions say only what is visible,** and name the commit each picture came from.

Pass the owner a sheet of the fight.

## The shared lair contract

From the planning pack's [chapter 04](../plans/concordance-expansion-2026-10-10/04-existing-bosses-and-shared-encounters.md). Each lair's plan and record answer:

- the arrival, a safe staging space, the wake condition and the return gate;
- which blocks are fixtures, and who may work them;
- who owns the arena floor, and how it is restored;
- who may fight, and the party scaling;
- the reset when nobody eligible remains;
- the clean-up of projectiles, adds, temporary effects and reservations;
- death, disconnect, restart, and rewards given exactly once;
- how an ordinary melee or ranged player can solve every required mechanic.

A specialty may make a mechanic easier, never required.

## Done means

A boss is complete when it has:
- survival entry;
- reachable preparation;
- a readable fight;
- usable rewards;
- a safe reset and return;
- its records;
- real evidence.

A rendered model and a health bar are not an encounter. The evidence is:
- the local checks;
- CI green on the final commit;
- the pictures in the record.

Report it honestly, keeping run, failed and not run apart. Not yet run for any boss: the two-client dedicated-server playtest, and play by hand. Then give the owner a short checkpoint (chapter 10's template) before the next boss.
