# Custom NPCs and guards

Status: implemented locally; six NPC server tests and combined rendered client walkthrough passed. Owner-requested, November 2026, as the NPC half of a collaboration. Implemented with Claude Code (Claude Opus 5.5); OpenAI Codex integrates and runs it.
Proposal issue: none (owner request).
Owner: server operators (permission level 2).
Target milestone and tier: server administration and world-building; no progression tier.
Primary specialty and supported player role: builders and operators dressing their own builds with residents and watchmen.

## Player experience

Operators place named residents and guards anywhere, give them a town skin or any Minecraft player's skin, write what they say, and set where guards stand and patrol.

- **NPCs** stand at their post, wander up to the distance an operator sets (0–16 blocks), and say the operator's lines in turn. With no lines, they say the town's seasonal lines.
- **Guards** reuse the spawn town's guard: its sword, skin and behaviour.
  - They fight monsters only, never players or owned pets.
  - With no patrol they stand at their post (wander 0 by default) and return to it after a fight.
  - With a patrol they walk up to 16 waypoints in order, pausing 3 seconds at each. Waypoints in unloaded chunks are skipped, and nothing is loaded.

Players talk to them exactly as they talk to the town's residents.

Commands (`/npc`, `/guard` and `/jugcraft npc` are the same tree; `/guard create` makes a guard):

```text
/npc create <name>            /guard create <name>     (where you stand, facing your way; selects it)
/npc select [id]              (the one you look at, else the nearest within 16 blocks)
/npc list | info [id] | remove [id]
/npc name <name>
/npc skin player <name> | skin town <skin>
/npc say <text> | say add <text> | say list | say remove <n> | quiet
/npc wander <0-16>
/npc post [pos]
/npc patrol add [pos] | list | remove [n] | clear
```

**Choosing the NPC to edit.** Editing commands take no number. They act on the selected NPC; with none selected (or the selected one not loaded), on the NPC you are looking at, within 8 blocks and in sight. Any of these selects an NPC:

- making it;
- `select`;
- `select <id>`;
- talking to it while sneaking, as an operator.

**Where.** Posts and waypoints are where you stand unless you give a position. Waypoints must be within 64 blocks of the post. Moving the post drops any that are out of reach.

## Connections

- Existing input producer: operator commands. There are no recipes or items.
- Existing output consumer: none. These NPCs are decoration and guards, with no trades.
- Technology and magic connections: none. This is administration and world-building infrastructure, so resource links do not apply.
- Reuse:
  - They are the town's own `Townsfolk` entity, renderer, model, guard combat goals and seasonal dialogue keys.
  - Player skins use the server's own profile resolver and the client's player skin cache, the same pair that player heads and owner statues use.

## Balance and automation

Nothing is produced or consumed. Guards are invulnerable town guards, as in the spawn town; operators can remove them with `/npc remove`. They never attack players or owned pets.

**Limits:**

| What | Limit |
|---|---|
| Name | 32 characters |
| Dialogue | 8 lines of 256 characters; formatting codes and control characters removed |
| Wander | 16 blocks |
| Patrol | 16 waypoints within 64 blocks |
| `list` output | 40 rows |
| Skin lookups | 4 names in flight, up to 128 waiting NPCs per name |
| Index tombstones | kept: a removed NPC never returns |

## Multiplayer and persistence

**Server authority.**

- Every command node requires permission level 2.
- Every command checks again on the server before it changes anything.
- Clients send nothing new.

**Not the spawn town's.**

- An NPC's town spot is -1, so the town's builder, upgrades, resident roster, beds, flak crews and raid gate defence never touch it.
- Custom guards are also kept out of the gate-raid pull (`GateDefenseGoal`), and custom NPCs out of Halloween costumes and the client-side harvest overlay. A synced custom-NPC flag keeps operator-chosen appearances visible, including after reload.
- Placing or editing one never changes a block. Creation refuses a spot where its body would not fit.

**Saving.**

- Everything an operator set is saved in the entity under `jugcraft_custom_npc` (`CustomNpcs.Data.CODEC`). That covers the kind, town skin, player name and resolved profile with textures (`ResolvableProfile.CODEC`), patrol waypoints and current index, lines, wander and creator.
- A small world index (`jugcraft:custom_npcs`, in the overworld's saved data) numbers NPCs for commands and records their last-seen place. It also keeps tombstones for removed NPCs (never evicted), so one removed while unloaded leaves when its chunk next loads.
- Nothing ever loads a chunk.

**Unloading, restarts and removal.**

- Unloading keeps the index entry; `/kill` or removal forgets it.
- Restarting restores everything from the entity.
- Selections are in memory, per operator, and reset when the server changes.

**Skin lookup.**

- `/npc skin player <name>` calls `ResolvableProfile.createUnresolved(name).resolveProfile(server.services().profileResolver())`.
- The lookup runs off the server thread. The reply is applied on the server thread, through `whenCompleteAsync(.., server)`.
- At most 4 different names are looked up at once; more are refused with a "try again" message. Requests for a name already being looked up join that lookup.
- Lookups time out after 15 seconds.
- Each request is bound to the exact entity and its request number, so a stale reply changes nothing. A reply is stale when the NPC has since been given another skin, removed, unloaded or reloaded, or when the server has stopped. At most 128 NPCs wait on lookups.
- Found profiles are cached in memory for 6 hours, up to 128 names; misses are cached for 1 minute.
- An online player with signed textures is used at once.

**Client.**

- The synced `ResolvableProfile` is drawn through `playerSkinRenderCache().createLookup` (at most 64 lookups are kept). The NPC wears `townsfolk_1` until the skin loads.
- Slim-armed skins use slim arms. The townsfolk renderer's new `townsfolk:skinnable` model layer carries classic and slim arms; Herobrine, the crook and the mecha pilot keep the original layer.

## Dependencies and assets

No new dependencies or assets. Player skins come from Mojang's services through the server's existing resolver, as player heads already do. That is the only external call, and it is made only on an operator's command.

## Verification

All six focused server game tests passed locally on November 2 after integrating with the utility/protection changes (build/npc-integration-tests.log; archived under build/server-utilities-review). Client compile and the rendered walkthrough passed after fixing test-player teleport synchronization. Screenshot review then found the harvest costume overlay covering custom appearances; that has been corrected and final verification passed (build/npc-final-validation.log), with screenshots reviewed. The Claude-side files were syntax-checked only (tree-sitter); that machine cannot compile Minecraft 26.3. Codex hardened the core during integration: stationary guards default to wander 0, patrols skip unloaded chunks, tombstones are never evicted, skin waiters are bounded and bound to the exact entity, and guards never attack players or owned pets. The passing checks are `CustomNpcGameTests`:

| Test | What it checks |
|---|---|
| `operatorsMakeAndEditNpcsByCommandAndOthersCannot` | A non-operator's `/npc`, `/guard` and `/jugcraft npc` make nothing. An operator makes, names, voices, reskins, re-posts and removes an NPC, and makes a guard (sword, skin) with ordered patrol points from where it stands. Out-of-reach and unknown skins are refused. Edits act only on the selection. |
| `everythingAnOperatorSetIsSavedWithTheNpc` | Save and load keep the id, lines, wander, patrol, post and player profile, and the skin is re-synced. |
| `guardsFightMonstersButNeverPlayers` | Player damage does nothing. The guard targets a zombie and never a player. |
| `patrolsAreWalkedInOrder` | The waypoint index goes 0, 1, 0 and each transition requires physically reaching the preceding destination. |
| `aRemovedNpcNeverComesBackFromDisk` | A saved copy of a removed NPC leaves when added back to the world. |
| `skinLookupsAreBoundedAndStaleRepliesChangeNothing` | Four lookups in flight, a fifth refused, same-name joining, a found skin applied to both waiting NPCs, a failed lookup and a stale reply changing nothing, cached hits and misses. |

Still to do:

- visual comparison using real classic and slim player skins (the rendered smoke check used a synthetic resolved profile/default skin);
- a dedicated-server lookup against Mojang's services.

## World and event applicability

- They work in any dimension. Posts and patrols stay in the NPC's own dimension.
- Like town residents, they can walk through portals; an operator then sets a new post.
- No loot, ecology or boss interaction.
- Seasonal: NPCs without their own lines use the town's seasonal lines.

## Rollout and open questions

**Known limitations:**

- Players see no in-game editor screen; editing is by commands.
- Items held in a slim-armed NPC's hand sit half a pixel off, unlike vanilla's slim player model.
- Any language other than English sees the commands' feedback in English.

**Reversible:** removing the feature leaves the NPCs as ordinary townsfolk outside the town's list. A world backup before mass placement is still advised.
