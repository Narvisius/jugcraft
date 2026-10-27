# Lair encounter credit: support earns a boss's reward

Status: implemented for Concordance Chunk A (unit 3: tickets A-05 and A-06); its tests pass in CI (run [38435014953](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38435014953) on
`aa4d06c4e`). Extended to the Yeti King and the Cinder Tyrant in Concordance Chunk B, unit 1 (ticket B-01; [the chunk's
contract](concordance-chunk-b.md)), not merged; its tests pass in CI (runs 38514144531 and 38519234934). Unit 5 (B-09)
adds the Yeti King's second objective, a Brace put on another player that meets his push; its test passes in CI (run
38591108210). Extended in Concordance Chunk C, unit 1 (C-02, C-P8 and C-P30: a Stillroot Poultice put on a friend;
[the chunk's contract](concordance-chunk-c.md)), not merged; its tests pass in CI (run 38684732031, and again in
38774859294). See Verification for what has run.
Proposal issue: none. The owner chose Chunk A on 10 October 2026; [the chunk's contract](concordance-chunk-a.md) holds
the scope.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: the Witching Season lairs ([Vesperine](vesperine.md) and [Madame Tatterlace](tatterlace.md),
reached with Discovery-tier things) and, since Concordance Chunk B, the Glacier Hall's [Yeti King](yeti-king.md) and
the Cinder Kiln's [Cinder Tyrant](cinder-tyrant.md), with the Concordance's support spells: **Ward** (a composable word at First
Light understood) and **Lanternward** (an invocation at First Light mastered, which mends and wards a party), and, since
Concordance Chunk C, the **Stillroot Poultice**, a salve brewed with the Alembic Arts understood
([the alchemy record](arcane-concordance-alchemy.md)).
Primary specialty and supported player role: adventuring, for the party's healer, warder and helper as well as its
fighters.

## Player experience

Before this, only a player who struck a boss, or a creature that counts for it (Vesperine's skulls and harvest souls;
Madame Tatterlace's egg sacs and spiderlings; the Yeti King's whelps; the Cinder Tyrant's Cinderlings), shared its reward. A healer who kept the party standing,
or a player who relit the wards, got nothing unless they also landed a token blow. Now a player qualifies for a lair
boss's reward by any one of these, within the fight:

| Way to qualify | What counts | Threshold |
|---|---|---|
| **Damage** | Any blow to the boss, Vesperine's skulls or a harvest soul, Madame Tatterlace's egg sacs or spiderlings, the Yeti King's whelps or the Cinder Tyrant's Cinderlings, as before (not Vesperine's Grave Thralls); a tamed creature's blow counts for its owner | Any |
| **Healing** | Health the Concordance gives back to *another* player or, since Concordance Chunk C, the Regeneration of a Stillroot Poultice you put on them in the fight gives back (one health a heal, as it comes), counting only hurt creatures did to them in the fight (not a player's or a tamed creature's blow) that they still lack | 4 (two hearts; a poultice's fourth heal, 76 of its 450 ticks) |
| **Warding** | Damage from the fight's creatures soaked by a Concordance ward put on *another* player, up to what the ward gave | 4 (two hearts) |
| **An objective** | Vesperine: relighting one of her wards during Death's Harvest. Madame Tatterlace: cutting another player free of her Binding Thread. The Yeti King: his Avalanche Charge aimed at you ending in an ice column, if you had already taken part (a blow, healing or a ward) as he set off; and (since unit 5, B-09) a Brace you put on another player that takes one of his throws, with no part needed before, if you are in his fight and can fight him. The Cinder Tyrant: none (proposed, B-P4) | One, each counted once a fight (the column once for each player; a Brace once for each braced player and tick in which it meets his push) |

One **Mend** (4 health) of a friend her blows hurt, or one **Ward** (absorbs 4) that soaks one of her blows, is
enough; since Concordance Chunk C, so are the first four heals of a Stillroot Poultice on such a friend, and each
keeper's dossier says so while the Concordance brews the poultice. Past the threshold, more adds nothing: a share of the
loot is a share.

**What counts for nothing.** Healing yourself; healing a player already whole (overhealing); healing hurt the fight did
not do (a fall, another player's blow) or hurt that rest, food or a potion has healed since; a ward on yourself; a ward
no blow reaches; striking an already snapped thread; relighting a ward outside her Harvest, or relighting it again; a
blow to one of Vesperine's Grave Thralls; a pet's blow for anyone but its owner; the Yeti King's charge into a column
aimed at one who had no part yet, even if his fight had struck them; a Brace on yourself, the Water Bangle's (nobody's
cast), or one whose caster a relog forgot (who cast it is not saved), and being braced; healing what freezing, or the
Cinder Tyrant's slag's heat or burning, took (they have no attacker; his blows and his own fire count). Since
Concordance Chunk C, also: a Stillroot Poultice on yourself; one put on before the keeper wakes or outside his fight;
its heals while the patient is out of the fight, and after; a second poultice that changes nothing; its heals after
another Regeneration replaced it or it was cured; a Frostward salve, a Regeneration salve carrying anything harmful, a
Regeneration draught, or Regeneration as a spell's status; food or rest between its heals.

**Who is paid, and when.** When the boss falls, each qualified player is paid once:

- alive within 64 blocks of the arena's centre: at once, as before;
- one who **died** in the fight (within its reach while it was on): at once if they have respawned, wherever they are;
  if they are still on the death screen, as soon as they respawn ("Your share of the spoils is kept for you until you
  are back on your feet", then "You are handed your share of the spoils");
- one who **left the game** in the fight and is still away when it falls: when they next join, once out of any lair
  that has closed (a player who logs in there is sent home first); one who came back before it fell is paid like anyone
  else;
- anyone else who walked away alive: nothing, as before.

The loot is the boss's own table, rolled when it is handed over (so a first kill and the Halloween extras are judged
then), into the inventory, with what does not fit at the player's feet, as before. The 300 experience is shared among everyone
paid, now or later. The advancement comes with the loot.

## How it works

**The fight's record** (`lair/Encounter`). Each boss keeps one, begun when it wakes (and again if it is loaded awake
from a save) and forgotten when it is left alone and returns to its seat. Per player it keeps four numbers (damage,
healing, warding, deeds), plus each player's unhealed hurt and the ward on them; at most 32 players a fight. It is in
memory only: nothing about a fight in progress is saved, so a fight does not resume its record after a restart, as
before.

- *Blows to players.* Fabric's damage events give each player's health and absorption before and after every blow
  while a fight is on. A blow whose source is a creature (not a player, a tamed creature or a fall) adds what it took to that player's
  unhealed hurt (up to their whole health) and credits whoever's ward soaked it. Before any blow, of any kind, the
  unhealed hurt is cut to the health the player lacks, so what rest, food or a potion healed is no longer there to heal.
- *Healing and wards.* The Concordance's effect boundary (`ConcordanceEffects`) now tells observers of each restoration
  and protection that took: who cast it, on whom, and how much (the health actually given back, or the most the ward
  may soak). The lairs observe it: healing is credited up to the patient's unhealed hurt, which it then reduces; a
  ward is remembered for its duration and credited for what blows take from it. The boundary knows nothing of lairs.
- *Objectives.* Lighting a lair brazier now calls `Lairs.relit` (only on its unlit-to-lit turn, so clicking a lit one
  does nothing). Vesperine counts it as a deed if her Harvest is on, the player is in her fight and the brazier is one
  of her wards; the key is the fight and the ward, and her Harvest comes once a fight. Her Binding Thread, struck by a
  player other than the one it holds, counts a deed for the striker, keyed by the thread. The Yeti King's Avalanche
  Charge, on the last tick of its wind-up (where its line is fixed), notes whom it is aimed at if they already have a
  part in his fight (a share: a blow, healing that counted or a ward on another player, even short of qualifying; being
  struck by his fight is not a part). If the charge ends in an ice column and that player is still in his fight, they
  earn a deed keyed `column <player>`, so each baiter earns one a fight and several may each earn theirs; his stun any
  other way credits no one. Since unit 5 (B-09) every push of his goes through `YetiKingEntity.push`, which asks
  `Brace.spend`: when a Brace takes some of it off a player, `braced` reads who cast that Brace (`Brace.caster`, kept in
  the non-saved attachment `jugcraft:brace_caster`), and a caster who is not that player, is online, can fight him and
  is in his fight earns a deed keyed `brace <braced player> <game time>`. Each key counts once a fight, so two pushes in
  one tick are one deed and each Brace that meets his push is its own; no earlier part is needed, as a ward's soak needs
  none. The dossier's share line shows the deed count, which Brace deeds raise; qualifying is a yes or no.
- *A Stillroot Poultice* (since Concordance Chunk C, C-02). `BrewItem.applySalve` tells its observers when a poultice
  (`Alchemy.product`: a salve carrying Regeneration and nothing harmful) that one player put on another takes: the
  patient's Regeneration began, or grew stronger or longer. It compares the level and time left it read before the dose
  with those after, since a stronger dose updates the same instance in place. The lairs observe it (`Lairs` registers
  `Encounter.mended`). In each fight round the patient that the helper is in, the encounter keeps a note: the helper,
  the Regeneration's level and time left, and the patient's health. A later poultice that takes replaces the note, as a
  later ward does.
  - The note is read at the end of each server tick (`ServerTickEvents.END_SERVER_TICK`; that it fires after the
    players' own ticks, where Regeneration heals, is inferred, not confirmed in Fabric's source, and the count does not
    depend on it), at each blow before the hurt is settled, and at each Concordance heal.
  - Each read counts the heals vanilla gave since the last, from the Regeneration's own time left: a heal on each tick
    whose time left, before it counts down, is a multiple of `50 >> amplifier` (26.3's `RegenerationMobEffect`, read in
    `mc263src`).
  - It credits the helper with what the patient's health rose, at most one health a heal, through `creditHealing`
    (`healed`'s old body), so the fight's hurt not yet healed and full health bound it as for any healing.
  - The note ends when the Regeneration is gone, at another level or longer than it was (no longer the poultice's), when
    the patient leaves the fight, or when the fight is forgotten. The helper is credited wherever they stand once it is
    on, as a ward's caster is.
  - No mixin, and nothing is saved. Each read does a few comparisons for each note, and nothing runs while no fight
    is on.
- *Who is in the fight.* A player in the boss's level within 64 blocks of the arena's centre, alive or not, while the
  fight is on. A player who dies there is excused (paid wherever they are, as above); one who leaves the game there is
  marked away (on the server thread, as they leave), which counts only if they are still away when the boss falls.
- *The killing blow.* The blow that kills a boss is credited to its striker before the fall is paid, so a player whose
  only part is the last blow is paid too.

**The reward** (`lair/LairRewards`). A boss's fall is claimed once (`Encounter.claim`), however often it is reported,
so a second death callback pays nothing. The receipt is the fight's id and the player. What is owed is kept:

- with the player, in the `jugcraft:lair_rewards` attachment (saved in their player file, kept through death), for one
  still on the death screen; it is handed over on respawn, or within a second of their being alive;
- in the world, in the overworld's saved data `jugcraft:lair_rewards`
  (`<world>/dimensions/minecraft/overworld/data/jugcraft/lair_rewards.dat`), for one away from the game; on joining it
  moves onto them and is handed over once they are alive and not in a lair that has closed.

At most 16 rewards wait for one player (the oldest go first), and one fight's reward is kept once. A reward is taken
off the record before its loot is given.

**Crash limits.** The attachment, the inventory and the experience are saved together in the player's file, so what a
reward puts into the inventory from the attachment is either still owed or given. What does not fit is dropped at the
player's feet as items saved with that chunk, not in the player's file, so a crash between those saves can lose that
part or pay it twice (on the ground and still owed). Joining moves all of a player's kept rewards (up to 16) off the
world's record at once and crosses two files (the world's record and the player's file); a crash between their saves
can lose all of them, or pay them all twice. Nothing restores a lost reward automatically; a server admin can give the
boss's loot by hand.

## Connections

- Existing input producer: the lairs' fights (the Last Rites, the Cursed Spindle and, since Chunk B, the Frost Horn and
  the Kiln Seal); the Concordance's Ward and Lanternward.
- Existing output consumer: the bosses' loot, experience and advancements, unchanged.
- Technology connection: none required.
- Magic connection: the Concordance's support spells now earn a boss's reward, so a First Light caster has a party
  role in the lairs. Since Concordance Chunk C, so does an alembist: a Stillroot Poultice brewed before the fight and
  put on a friend in it.
- Reachable entry path: the lairs need Discovery-tier things; Ward needs First Light understood; neither needs a boss.
- Required vs optional; trade and solo routes: none is required. A solo player qualifies by damage as before.
- How this specialty stays useful: a party can bring a healer or warder who does not fight, and the rescues and wards
  give a player without either a way to help.

## Balance and automation

- No change to any loot table, chance, first-kill guarantee, Halloween extra or the experience total.
- Thresholds (proposed, for the owner): 4 healing or 4 warding, the size of one Mend or one Ward. Any damage still
  qualifies, as before. A Stillroot Poultice (Regeneration II for 450 ticks) heals at most 18 health, so it credits at
  most 18; it qualifies its helper at its fourth heal, 76 ticks after it goes on, if the patient still lacks that much
  of the fight's hurt. One note per patient, so two poultices on one friend earn no faster than one.
- No leeching: healing and wards count only the fight's own hurt and blows, on someone else, so two players cannot
  farm credit off each other; a bystander who does nothing still gets nothing. A Brace deed needs the Yeti King's own
  push to meet a Brace on someone else, at most one key a braced player and tick, bounded by Brace's 8-second cooldown
  and its Focus; one is enough, and more adds nothing.
- Units: health in half hearts; times in ticks; distances in blocks.

## Multiplayer and persistence

- Server authority: the server measures every blow and heal; nothing is claimed by a client.
- Bounds: 32 players a fight; 16 rewards a player; the record is per boss and dropped when it ends. Since Chunk C, at
  most 32 poultice notes a fight, one a patient, in memory only.
- Concurrent fights: each boss has its own record; a player near two fights is counted in each they are in.
- Chunk unload and restart: a fight's record is not saved; a boss that is loaded awake begins a fresh one. Rewards
  owed are saved (above).
- Disable behaviour: with the lairs switched off there are no fights; rewards already owed are still handed over.
- Leaving: a player leaving the game is marked on the server thread, as they leave (Fabric's leave event), not on the
  network thread that reports a disconnect.
- New saved data: the attachment `jugcraft:lair_rewards` and the world record `jugcraft:lair_rewards`, both versioned.
  Removing this feature would leave owed rewards unpaid; neither holds items. In both, each reward is kept under its
  boss's saved name: `vesperine` and `tatterlace`, and since Chunk B `yeti_king` and `cinder_tyrant`. These must not
  change once released; an older build that has no payer for a name drops a reward kept under it unpaid.

## Dependencies and assets

None new. Two new messages (`message.jugcraft.lair.reward.kept` and `.returned`) from `tools/lair_data.py`.

## Verification

- `python3 tools/check_mod_data.py` and `python3 scripts/check_repository.py` pass locally.
- Game tests, passed in CI (run [38435014953](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38435014953), with the optional integrations and without): `LairEncounterGameTests` (healing counts only the fight's hurt; what rest healed
  earns nothing; a ward is credited for what it soaks; a tamed creature's blow is its owner's; a left fight pays no
  one; one who came back is paid like anyone else; a kept reward is paid once; kept rewards survive a save: a player saved
  owing one and loaded again as the same player joining is owed it once, the world's record reads back the same, and
  both read their saves from before versions), `VesperineGameTests`
  `supportSharesHerHarvestOnce` and `aWardRelitInHerHarvestCounts`, and `TatterlaceGameTests.cuttingAFriendFreeCounts`.
  They use mock players: a death or a departure is stood in for by marking the player (`Encounter.excuse`,
  `Encounter.left`), and a reward kept for one is handed over by calling the join and hand-over code directly; none of
  Chunk A's tests kills, disconnects, rejoins or respawns a player. The existing loot tests still use the bosses'
  `took`, `participants` and `reward`, which keep their signatures.
- Concordance Chunk B, unit 1 (the Yeti King and the Cinder Tyrant), not merged; these **pass in CI** (runs
  38514144531 on `579ab5f6f` and 38519234934 on `06dc5ea5f`, each with every job green; the server logs name no test,
  but each of these is a required test, and every required test passed). In the first run, 38509414542, only the two
  `fourPlayersAreEachPaidOnce` failed: they expected more health than the game's cap of 1024, and now expect the cap.
  `YetiKingGameTests` `supportSharesHisLootOnce`, `aKillingBlowAloneIsPaid`, `aPetsBlowIsItsOwners` (on him and on a
  whelp), `twoBaitersEachEarnTheirDeed`, `anIdleLateArrivalEarnsNothing`, `fourPlayersAreEachPaidOnce` and
  `aFightLoadedAwakeIsRecordedAfresh`; `CinderTyrantGameTests` `supportSharesHisHoardOnce`, `aKillingBlowAloneIsPaid`,
  `aPetsBlowIsItsOwners` (on a Cinderling and the felling bite on him), `healingHisSlagAndBurningEarnsNothing`,
  `fourPlayersAreEachPaidOnce` and `aFightLoadedAwakeIsRecordedAfresh`; and `LairEncounterGameTests`
  `aKeptYetiKingRewardIsSavedAndPaid` and `aKeptCinderTyrantRewardIsSavedAndPaid`. How each lifecycle event is
  produced:
  - *death:* the Yeti King's support test makes one real `ServerPlayer.die` on a mock player, so Fabric's `AFTER_DEATH`
    reaches `Encounter`'s listener and `Lairs.died`; the blow that would kill is not real (its health is set to nothing
    first). The Cinder Tyrant's marks the death with `Encounter.excuse`, as Chunk A's tests do: the death path is
    shared code, so proving it once is enough;
  - *leaving:* marked with `Encounter.left` on a Fabric `FakePlayer` the server's player list never held; the real
    leave path is not run;
  - *respawn and join:* `LairRewards.handOver` and `LairRewards.welcome`, which those events call, are called directly;
  - *save:* in-process round trips of the boss entity and of the player's attachment, not a server restart;
  - *party:* four mock players, not four people.
  As in Chunk A, there is no real disconnect and no second client. `LairClientGameTests`, the lairs' own client test,
  passed in run 38519234934 (shard 2).
- Concordance Chunk B, unit 2, not merged: `LairClientGameTests` keeps a `yeti_king` reward across a restart. In step
  7b a Glacier Hall is closed by `Lairs.reset()` (a restart simulated) with the player inside: the reward kept from
  before is not paid in the closed hall, and is paid once at home. In step 9 the world is saved with the player inside a
  Glacier Hall and the reward kept in the world's record, and opened again: the game's own server stops and starts, and
  on the real join the player is sent home first and then paid once. It passed in CI run 38543250503 by its shard's
  success (shard 0). No disconnect or leave event is checked.
- Concordance Chunk B, unit 5 (B-09), not merged, passed in CI (run 38591108210): `YetiKingGameTests.aBraceOnAFriendIsTheCastersDeed`
  (a Brace one test player put on another that meets his push is the caster's deed, one for each push it meets, two in
  one tick one; a Brace on oneself, the Water Bangle's and one whose caster was forgotten credit no one, and being
  braced earns nothing), and `check_yeti` holds `braced` to the caster, never the braced player, and to the key.
- Concordance Chunk C, unit 1 (C-02), not merged. **Passed in CI:** every job of run 38684732031 (through `0b1039312`,
  after one re-run of a client shard for main's Peepo cow-breeding flake), and again in run 38774859294 on
  `bb856f873`, among its 1836 required server game tests. They were written where no Minecraft jar is available; CI
  was their first run.
  - `LairEncounterGameTests` +8 (18 in all):
    - `aPoulticeOnAFriendIsCreditedForWhatItHeals`: credit as it comes, with food between heals, within the hurt;
    - `aPoulticeOnOneselfEarnsNothing`, by a use and by sneaking;
    - `aPoulticeOnAnUnhurtFriendEarnsNothing`, for a friend unhurt and one fallen;
    - `aSecondPoulticeAddsNoCredit`;
    - `aPoulticeOutsideTheFightEarnsNothing`: one put on outside it, and one whose patient leaves it;
    - `onlyAPoulticeMends`: a Frostward salve, a Regeneration salve carrying something harmful and a spell's
      Regeneration earn nothing; a poultice on the same friend then does;
    - `blowsAndSpellsBetweenItsHealsAreNotThePoultices`;
    - `regenerationHealsOnItsVanillaInterval`, which measures vanilla's interval tick by tick: 18 heals at II over 450
      ticks, 6 at I over 318, and 10 at III over 120.
  - `EncyclopediaGameTests.theDossiersNameThePoulticesCredit`, and `theExpeditionsPageHoldsEveryKeeper` now expects the
    poultice's fact while the Concordance is on.
  - How the tests stand in for a fight: the patient's own tick, where effects tick, is run by hand (`baseTick`) on a
    mock player, so a real client's `doTick` (with `FoodData.tick`) is not run. Their players are mock players. Two
    tests (on oneself, and blows and spells between heals) check that none of their own players qualifies, rather than
    that nobody does, since a fight in a neighbouring test within 64 blocks may count theirs.
  - `check_poultice_credit` (new) holds:
    - the status to Verdance's, and the poultice to a salve of it;
    - the salve's guards and its single call to the observers;
    - the three reading points, the blow read before the hurt is settled;
    - the one-health cap and `creditHealing`'s bounds;
    - the note's end and its forgetting;
    - no codec and no mixin on effects;
    - the dossier fact and its words.
    Its `50 >> amplifier` needle, with `check_workshop_products`' check of `REGENERATION_INTERVAL`, ties the Java and
    the generator to the same vanilla interval.
  - Run locally for the unit's commits, and passed: `tools/check_mod_data.py`, `scripts/check_repository.py`,
    `tools/owner_art.py --check`, `tools/concordance_journey.py --check`, `scripts/tests` and `git diff --check`; 22
    audit mutations on a scratch copy for the credit's commit, each reported.

Not run: a real fight with a healer in a lair; a real death in a fight (the Yeti King's test above makes one real
`ServerPlayer.die` on a test player, without a real blow); a real disconnect or respawn; a rejoin other than the one
after a whole world's save and reopen (unit 2); two clients; a crash between saves.

## World and event applicability

Only the lair bosses are affected; their containment, loot rarity and dimension return are unchanged. Pet ownership:
a tamed creature's blow counts for its owner. Seasonal: who qualifies and who is paid do not depend on the event, and
owed rewards are still paid after it ends. A reward is rolled when it is handed over, so one owed from a fight during
the Halloween event and handed over after the event has ended comes without its candy and extra hood or headdress
chance (and one owed from before the event and handed over during it comes with them).

## Rollout and open questions

- Healing and wards count only through the Concordance: its effect boundary and, since Chunk C, a Stillroot Poultice's
  Regeneration. A splash potion of healing, food, a beacon or another mod's healing is not credited: vanilla's healing
  carries no healer, and finding one would need a mixin. Recorded as a known limit, with two residuals of the
  poultice's:
  - food, a vanilla potion or a beacon healing in the same tick as one of its heals can be credited for that heal, at
    most one health, and never past the fight's hurt (Concordance heals and blows are read out exactly);
  - a poultice replaced by a stronger or longer Regeneration (a potion, a beacon, milk) ends its note, so heals
    vanilla's hidden instance gives afterwards are not credited. This under-credits, which is safe.
- A poultice's helper is credited wherever they stand once it is on, as a ward's caster is; only its patient must stay
  in the fight.
- A second ward on a player replaces the first's record, so the newer caster is credited for what is soaked.
- The thresholds and the experience share are proposals for the owner (see the chunk's decisions).
- The Yeti King ([#304](https://github.com/jimbozoomer-byte/jugcraft/pull/304)) and the Cinder Tyrant
  ([#317](https://github.com/jimbozoomer-byte/jugcraft/pull/317)) have both adopted `Encounter` and `LairRewards`
  (Concordance Chunk B, unit 1). The Mire Hag ([#336](https://github.com/jimbozoomer-byte/jugcraft/pull/336)) and the
  Crypt Lich ([#350](https://github.com/jimbozoomer-byte/jugcraft/pull/350) and
  [#353](https://github.com/jimbozoomer-byte/jugcraft/pull/353)) are on main through
  [#352](https://github.com/jimbozoomer-byte/jugcraft/pull/352) with damage-only rewards (a private `damageBy`,
  `lair/hag/MireHagEntity.java:249` and `lair/lich/CryptLichEntity.java:258`); this record first said "in their own pull
  requests". Moving them onto `Encounter` is outside Chunk B, and outside Chunk C: a poultice earns nothing in their
  fights (deferred in [the Chunk C contract](concordance-chunk-c.md)).
- Harm with no attacking creature, such as freezing, or the Cinder Tyrant's slag's heat and burning, is not the
  fight's hurt, so healing it earns nothing (proposed, B-P5 in [the Chunk B contract](concordance-chunk-b.md)). The
  Cinder Tyrant names no objective (proposed, B-P4): a turned sluice records no one who turned it.
