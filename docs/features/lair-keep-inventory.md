# Keeping your inventory in boss fights

Status: implemented (draft PR)
Proposal issue: none. This is an owner instruction from 22 October 2026: "players should not lose their inventory on death in boss fights".
Owner: jimbozoomer-byte
Target milestone and tier: the shared lair framework ([hollow-acre.md](hollow-acre.md#lairs-the-shared-rules)), for every lair and its boss; no tier of its own.
Primary specialty and supported player role: combat. It supports every player who fights a boss.

## Player experience

A boss fight is meant to be a fight you can try again, not one that costs your kit. Death in a lair works like vanilla's keepInventory rule, but only in a lair:
- **Nothing is dropped**: no items and no experience orbs. Curse of Vanishing items stay too, as keepInventory keeps them.
- **You respawn outside the lair** as before, with:
  - every inventory slot as it was;
  - armour still worn and the offhand still held;
  - Trinkets slots (rings, charms, spell foci, a backpack on your back) still worn;
  - the same hotbar slot selected;
  - all your experience, its level and progress.
  
  A chat line tells you: "You fell in the lair, but kept everything you carried and all your experience".
- **Your visit ends**, as before. Your party can go on, and you can rejoin through the gate while it is open. When the fight ends, your share of the spoils is kept for you until you respawn, as before (`LairRewards`).
- **Dying anywhere else is unchanged**: you still drop everything.

**No grave is ever placed in a lair.** The death graves ([owner-graves-aircraft-furniture.md](owner-graves-aircraft-furniture.md#death-graves), #475) skip lairs.

**Grave Goods** remain as a safety net. Anything still dropped when you die in a lair is caught as it drops and handed back when you respawn. Such drops would come from another mod's death drops, since the game itself now drops nothing. The graves' exact death-stack capture (`DeathGraves`) catches them.

## Connections

- Existing input producer: the lairs and their bosses (`lair/Lairs`, `LairInstance`, every boss's arena).
- Existing output consumer: the player's own inventory and experience. Grave Goods (`GraveGoods`, `Lairs.handBack`) still catch stray drops.
- Technology connection: none needed. It is a rule of the shared framework, so every lair, present and future, gets it.
- Magic connection: none needed, for the same reason.
- Reachable entry path: unchanged. Every lair's own rite or opening is the way in.
- Required vs optional: the rule applies in every lair, with no setting to switch it off.
- Usefulness without other branches: any player who reaches a boss benefits.
- Infrastructure note: this is a framework rule. It adds no items, recipes or progression.

## Balance and automation

- **No duplication:** nothing is dropped, and the respawned player is given copies of the dead player's slots. The dead player is discarded when they respawn, so each item exists exactly once.
- **Experience** works the same way: the dead player drops no orbs, because their experience reward is 0 in a lair. The new player gets the same level, progress and total, so nothing is gained or lost.
- **A cheaper death in a boss fight**: the cost of dying is now:
  - the walk back to the grave or altar;
  - the gate's time limit;
  - losing your place in the fight.
  
  The boss still heals and resets when everyone leaves, and every reward still needs your part in the fight (`Encounter`).

## Multiplayer and persistence

- **Server authority:** everything happens on the server, through the shared per-player keep-inventory rule (`server/PlayerRules`, from #450). No separate system is added.
  - `PlayerRules.keeps(player)` is now also true when the player is in a lair (`Lairs.keepsBelongings`). Its existing hooks then treat the death as keepInventory:
    - `PlayerKeepInventoryMixin` covers `Player.dropEquipment` and the experience reward;
    - `TrinketKeepInventoryMixin` covers the Trinkets drop rule;
    - `ServerPlayerKeepInventoryMixin` covers vanilla's own respawn restore, `ServerPlayer.restoreFrom`.
  - The respawn restore now asks about the player who died rather than the respawned one. They share a UUID, so the personal rule answers the same, but only the one who died is in the lair.
  - The world's keepInventory game rule is never changed.
- **Graves (#475):** `DeathGraves.duringDeath` still turns its death-stack capture on in a lair, although the lair keeps everything. Anything still dropped therefore becomes Grave Goods (`Lairs.retainDeathItem`) and never lies in an instance that will close. Everywhere else, keep-inventory deaths still skip the capture as before.
- **Logging out on the death screen:** a player who leaves while dead in a lair, or whose server restarts, comes back still dead where they fell.
  - `Lairs.joined` now leaves dead players alone, as `Lairs.check` already did. They are not sent home while dead, so their respawn still sees that they fell in a lair and keeps everything.
  - Their items are saved with them as with any player.
- **Save compatibility:** no new saved data, IDs or registrations. Grave Goods already held by a player (from before this change) are still handed back on their next respawn.
- **Disable behaviour:** none. The rule needs no configuration.

## Dependencies and assets

This adds no dependencies, assets or mixins. It reuses the shared keep-inventory mixins from #450, and Trinkets is the already-required library.

## Verification

| Check | Command | Result | When |
| --- | --- | --- | --- |
| Structure and links | `python scripts/check_repository.py` | passed | locally, before pushing |
| Material data and recipe audit | `python tools/check_mod_data.py` | passed | locally, before pushing |
| Syntax of the changed Java files | the session's parse helper (javac's parser, no Minecraft classes) | passed | locally |
| Compilation and headless server game tests (including `PlayerRulesGameTests`) | `./gradlew build` (CI job `mod`) | passed before the graves merge | CI run 39821173207, commit f836db854 |
| Core without optional integrations | CI job `optional integrations absent` | passed before the graves merge | same run |
| Rendered-client game tests | `./gradlew runClientGameTest` (CI job `client`, 16 shards) | passed: all 236 client test classes ran, because a shared mixin changed; `LairClientGameTests` ran in shard 10 | same run |
| Rendered-client game tests, with main's death graves (#475) merged in | CI job `client`, 16 shards | passed: every client test class ran; `OwnerBatchClientGameTests` (its lair death now kept in place) ran in shard 3, `LairClientGameTests` in shard 10 | CI run 40365916464, commit 912c9cc84 |
| Compilation and server game tests, with the graves merged in | CI jobs `mod` and `optional integrations absent` | **not completed**: both were cancelled at the workflow's 90-minute limit | same run |

The `mod` and `optional integrations absent` cancellations are not from this change:
- The game compiled, since the client jobs build the same code.
- The server tests ran: 2,049 of 2,050 passed, and none failed.
- One test in the last batch never finished, while the test server fell about 10,000 ticks behind.
- Main's own Build for #475 (commit 7317e5d44, run 40223676835) was cancelled the same way. So was #477, which only changes a Python tool.
- This change only acts in lair dimensions, which the server test world does not have.

Until that stall on main is fixed, the server game tests that touch this change (`DeathGraveGameTests`, `PlayerRulesGameTests`) have no recorded pass on the merged code. Before the merge (f836db854), `PlayerRulesGameTests` passed.

The game is not compiled locally (its toolchain downloads are blocked there), so CI is the evidence for the three rows above. The first push, 182d143ee, failed to compile the test: in 26.3 the open screen is `client.gui.screen()`, not a field of `Minecraft`. f836db854 fixed that.

The rendered-client test (`LairClientGameTests`, step 4) covers a real death in the Hollow Acre in survival:
1. Setup: a helmet worn, a shield in the offhand, an amethyst shard in the Trinkets spell-focus slot, 9 emeralds in hotbar slot 5 (selected), 5 bread in slot 21, and about 500 experience.
2. The player is killed with `genericKill`. No item entities or experience orbs are left within 8 blocks, there are no Grave Goods, and the visit has ended.
3. The client presses respawn. The respawned player is outside the lair, with:
   - the helmet, shield and spell-focus slot still worn;
   - the emeralds and bread in their own slots;
   - slot 5 still selected;
   - the same experience level, progress and total;
   - no Grave Goods left over.
4. `Lairs.keepsBelongings` is false outside a lair.
5. The player goes back in through `Lairs.enter`. The old Grave Goods check (3 diamonds and 7 experience dropped by hand) still passes as the safety net.

Not run: a two-client dedicated-server playtest, and play by hand.

## World and event applicability

This applies in every lair dimension and nowhere else. It does not depend on season or event.

## Rollout and open questions

- Known limit: a mod that moves items out of the inventory at death by some route other than `dropEquipment` (a grave mod, say) works as that mod decides.
- Rollout is reversible: removing the lair line from `PlayerRules.keeps` restores #475's Grave Goods behaviour, with no saved data to migrate. Under that behaviour items come back into the inventory after respawn, armour unworn and most experience lost.
