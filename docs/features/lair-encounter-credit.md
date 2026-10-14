# Lair encounter credit: support earns a boss's reward

Status: implemented for Concordance Chunk A (unit 3: tickets A-05 and A-06); its tests pass in CI (run [38435014953](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38435014953) on
`aa4d06c4e`). See Verification for what has run.
Proposal issue: none. The owner chose Chunk A on 10 October 2026; [the chunk's contract](concordance-chunk-a.md) holds
the scope.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: the Witching Season lairs ([Vesperine](vesperine.md) and [Madame Tatterlace](tatterlace.md),
reached with Discovery-tier things), with the Concordance's support spells: **Ward** (a composable word at First
Light understood) and **Lanternward** (an invocation at First Light mastered, which mends and wards a party).
Primary specialty and supported player role: adventuring, for the party's healer, warder and helper as well as its
fighters.

## Player experience

Before this, only a player who struck a boss, or a creature that counts for it (Vesperine's skulls and harvest souls;
Madame Tatterlace's egg sacs and spiderlings), shared its reward. A healer who kept the party standing,
or a player who relit the wards, got nothing unless they also landed a token blow. Now a player qualifies for a lair
boss's reward by any one of these, within the fight:

| Way to qualify | What counts | Threshold |
|---|---|---|
| **Damage** | Any blow to the boss, Vesperine's skulls or a harvest soul, or Madame Tatterlace's egg sacs or spiderlings, as before (not Vesperine's Grave Thralls); a tamed creature's blow counts for its owner | Any |
| **Healing** | Health the Concordance gives back to *another* player, counting only hurt creatures did to them in the fight (not a player's or a tamed creature's blow) that they still lack | 4 (two hearts) |
| **Warding** | Damage from the fight's creatures soaked by a Concordance ward put on *another* player, up to what the ward gave | 4 (two hearts) |
| **An objective** | Vesperine: relighting one of her wards during Death's Harvest. Madame Tatterlace: cutting another player free of her Binding Thread | One, each counted once a fight |

One **Mend** (4 health) of a friend her blows hurt, or one **Ward** (absorbs 4) that soaks one of her blows, is
enough. Past the threshold, more adds nothing: a share of the loot is a share.

**What counts for nothing.** Healing yourself; healing a player already whole (overhealing); healing hurt the fight
did not do (a fall, another player's blow) or hurt that rest, food or a potion has healed since; a ward on yourself; a
ward no blow reaches; striking an already snapped thread; relighting a ward outside her Harvest, or relighting it again;
a blow to one of Vesperine's Grave Thralls; a pet's blow for anyone but its owner.

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
  player other than the one it holds, counts a deed for the striker, keyed by the thread.
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

- Existing input producer: the lairs' fights (the Last Rites and the Cursed Spindle); the Concordance's Ward and
  Lanternward.
- Existing output consumer: the bosses' loot, experience and advancements, unchanged.
- Technology connection: none required.
- Magic connection: the Concordance's support spells now earn a boss's reward, so a First Light caster has a party
  role in the lairs.
- Reachable entry path: the lairs need Discovery-tier things; Ward needs First Light understood; neither needs a boss.
- Required vs optional; trade and solo routes: none is required. A solo player qualifies by damage as before.
- How this specialty stays useful: a party can bring a healer or warder who does not fight, and the rescues and wards
  give a player without either a way to help.

## Balance and automation

- No change to any loot table, chance, first-kill guarantee, Halloween extra or the experience total.
- Thresholds (proposed, for the owner): 4 healing or 4 warding, the size of one Mend or one Ward. Any damage still
  qualifies, as before.
- No leeching: healing and wards count only the fight's own hurt and blows, on someone else, so two players cannot
  farm credit off each other; a bystander who does nothing still gets nothing.
- Units: health in half hearts; times in ticks; distances in blocks.

## Multiplayer and persistence

- Server authority: the server measures every blow and heal; nothing is claimed by a client.
- Bounds: 32 players a fight; 16 rewards a player; the record is per boss and dropped when it ends.
- Concurrent fights: each boss has its own record; a player near two fights is counted in each they are in.
- Chunk unload and restart: a fight's record is not saved; a boss that is loaded awake begins a fresh one. Rewards
  owed are saved (above).
- Disable behaviour: with the lairs switched off there are no fights; rewards already owed are still handed over.
- Leaving: a player leaving the game is marked on the server thread, as they leave (Fabric's leave event), not on the
  network thread that reports a disconnect.
- New saved data: the attachment `jugcraft:lair_rewards` and the world record `jugcraft:lair_rewards`, both versioned.
  Removing this feature would leave owed rewards unpaid; neither holds items.

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
  `Encounter.left`), and a reward kept for one is handed over by calling the join and hand-over code directly; no test
  kills, disconnects, rejoins or respawns a player. The existing loot tests still use the bosses' `took`,
  `participants` and `reward`, which keep their signatures.

Not run: a real fight with a healer in a lair; a real death, disconnect, rejoin or respawn; two clients; a crash between saves.

## World and event applicability

Only the lair bosses are affected; their containment, loot rarity and dimension return are unchanged. Pet ownership:
a tamed creature's blow counts for its owner. Seasonal: who qualifies and who is paid do not depend on the event, and
owed rewards are still paid after it ends. A reward is rolled when it is handed over, so one owed from a fight during
the Halloween event and handed over after the event has ended comes without its candy and extra hood or headdress
chance (and one owed from before the event and handed over during it comes with them).

## Rollout and open questions

- Healing and wards count only through the Concordance (its effect boundary). A splash potion of healing, food, a
  beacon or another mod's healing is not credited: vanilla's healing carries no healer, and finding one would need a
  mixin. Recorded as a known limit.
- A second ward on a player replaces the first's record, so the newer caster is credited for what is soaked.
- The thresholds and the experience share are proposals for the owner (see the chunk's decisions).
- The Yeti King ([#304](https://github.com/jimbozoomer-byte/jugcraft/pull/304)) and the Cinder Tyrant
  ([#317](https://github.com/jimbozoomer-byte/jugcraft/pull/317)), on main through #320 since 12 October, keep their own
  damage-only reward until they adopt `Encounter` and `LairRewards`: support does not earn their reward, and one who
  died or left in the fight is not paid later. Proposed as the next step at the chunk's checkpoint.
