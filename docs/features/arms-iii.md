# Arms III: two-handed weapons

Status: implemented on `claude/arms-iii` (batch 46), awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 4 October 2026: "start on the next batch of weapons", then, with the Fiery Combat add-on attached: "look into how this mod works, now this is bedrock so you can't actually copy the code but I want something similar with how their two handed weapons work".
Owner: jimbozoomer-byte
Target milestone and tier: bronze (tin) and steel (machines), beside the arms of batches 42 ([arms.md](arms.md)) and 45 ([arms-ii.md](arms-ii.md)).
Primary specialty and supported player role: fighting.

## Player experience
The heavy arms now fight like two-handed weapons. This covers seven earlier kinds (greatsword, war hammer, glaive, battle axe, scythe, quarterstaff and pike) and four new ones.

- **A committed swing.** A click starts the swing at once, but the blow does not land on the click as vanilla's does. It lands as the swing comes round: on the frame where the animation lands it, a third of the way in.
  - Swings are longer to match: the greatsword's takes 20 ticks (was 11), the maul's 24.
  - While the swing is in the air you move at 40% of your speed and cannot sprint, so the swing is a commitment.
- **A cleave.** The blow strikes every foe in its arc (nearest first, up to a number per kind):
  - each foe must be within the arm's reach and in sight;
  - each is struck through vanilla's own attack, so enchantments, knockback, wear and the kind's trait all count;
  - your pets, allies and your own mount are never struck.
- **Combos with a finishing blow.** Clicks within 1.5 s carry a combo on, and the last attack of each kind's combo is the finishing blow, 25% stronger.
  - The combos follow each kind's animations (batch 43). The war hammer and battle axe now swing sideways first and finish with the overhead blow.
  - A click in the last 4 ticks of a swing follows straight on; earlier clicks are let go, as Fiery Combat queues its next attack.
- **Both hands.** A shield (anything that blocks) in the off hand leaves no hand for the grip: the swing does not start, and the action bar says why. A torch or anything else in the off hand is fine.
- Looking at a block still mines it as before; clicking a boat, minecart or item frame still hits it.
- Each two-handed arm's tooltip says it is two-handed. Handbook: "Arms: Two Hands" (how the swings work) and "Arms: The Two-Handers" (the new kinds), in the gear chapter. Advancement: **Two-Hander** (forge any of the four new kinds), after Bronze Age.

### The new kinds

| Arm | Hit (bronze / steel) | Attacks a second | Reach | Swing | Blow lands | Arc, most foes | Trait |
|---|---|---|---|---|---|---|---|
| Zweihander | 10.5 / 11 | 0.8 | 4 | 22 ticks | tick 7 | 140°, 5 | **Guard:** hold use to block 50% of a blow from in front. Sweeps. |
| Maul | 13 / 13.5 | 0.55 | 3.25 | 24 ticks | tick 9 | 90°, 3 | **Quake:** the finishing blow shakes the ground. Foes within 2.5 blocks (and a block of your footing) that the cleave missed take half the blow and are thrown back, and every foe there is slowed (Slowness II, 2 s). Stops a shield for 5 s; wears 2 a hit. |
| Executioner's Sword | 11 / 11.5 | 0.7 | 3.5 | 22 ticks | tick 8 | 90°, 2 | **Execute:** half again as much damage to a foe at or below 30% of its health. |
| Bill | 9 / 9.5 | 0.9 | 4.5 | 18 ticks | tick 6 | 90°, 3 | **Hook:** a hit pulls the foe towards you (0.6 blocks a tick, less its knockback resistance) and drags a rider from the saddle. |

### The earlier two-handed kinds

| Arm | Swing (was) | Blow lands | Arc, most foes | Combo (finishing blow last) |
|---|---|---|---|---|
| Greatsword | 20 ticks (11) | tick 7 | 120°, 4 | sweep, cleave |
| War Hammer | 22 (12) | tick 8 | 70°, 2 | side swing, slam |
| Glaive | 18 (10) | tick 6 | 120°, 4 | sweep, rising cut |
| Battle Axe | 22 (12) | tick 8 | 90°, 3 | side swing, chop |
| Scythe | 18 (10) | tick 6 | 150°, 5 | reap, return |
| Quarterstaff | 12 (7) | tick 4 | 100°, 3 | strike, jab, sweep |
| Pike | 16 (10) | tick 5 | 20° (a thrust), 3 | thrust, high thrust |

The halberd, spear and lance keep vanilla's thrusts and charges: those already strike every foe in line.

## What was learned from Fiery Combat
Fiery Combat is a Bedrock add-on: a behavior pack (items, recipes, camera presets and a 208 KB script on the Bedrock script API, `@minecraft/server` 2.10) and a resource pack (animations, attachables, models, particles, sounds). The pack carries no license file. It was studied for its approach only. Nothing of it can run in Java Edition, and none of its code, animations, models, textures, sounds or numbers is used.

| Fiery Combat's greatsword | What Jugcraft does |
|---|---|
| A weapon type with data: guard reduction 0.65, can parry, an "impact" for poise. Tagged items join the type. | A table of two-handed kinds (`TWO_HANDED` in `tools/arms.py`): strike tick, arc, most foes and combo length. |
| An attack is a small state machine: the click starts it, the damage comes at its `damagingDuration` (0.75 s for the greatsword, against 0.4 s for its sword), and a `recoveryDuration` follows. The player is slowed hard throughout (movement 0.01 against 0.02 for a sword). | The click starts the swing, and the blow lands at the strike tick, which the data check holds to the animation's blow key. The wielder is slowed by 60% until the swing ends. The timings are Jugcraft's own: shorter, for Java's faster combat. |
| A forward hit box (2.6 wide, 3 high, 3 deep; up to 5 targets, against the sword's 2 by 2 by 3 and 3). | An arc across the view, within the arm's own reach (26.3's attack range component), in sight, nearest first, up to the kind's number. |
| A combo cycle (attack 1, 2, 1, 3) whose last blow is a slam, multiplied 1.0, 1.2, 1.0 and 1.3. | Each kind's combo, as its animations; the last attack is the finishing blow (×1.25). The maul's shakes the ground. |
| "Cannot attack with a two-handed weapon while holding a shield". | The same rule, for anything in the off hand that blocks. |
| Guard and parry by sneaking, a poise meter that stuns at zero, attack clashes, a plunging slam from 5 blocks up, sprint attacks, its own camera. | **Not taken:** guard is 26.3's own blocking (hold use) on the zweihander, as on the longsword. Poise, clashes, slams and sprint attacks would each be a combat system of their own. Vanilla's mace already slams. |

## How it works
- **Client** (`client/arms/TwoHandedInput`, on Fabric's `ClientPreAttackCallback`):
  - a click with a two-handed arm in the main hand, at a foe or at the air, is taken from vanilla;
  - the client sends `jugcraft:two_handed_swing` (no data) and swings the arm at once, so the motion starts without waiting for the server;
  - clicks at blocks and at things that are not alive go to vanilla as before.
- **Server** (`weapons/TwoHanded`):
  - **Checks:** the request is checked against the server's own state: a two-handed arm in hand, the off hand, use, death, spectating, and whether a swing is already in the air.
  - **The swing:** the server swings the arm for everyone else and slows the wielder. It remembers the attack charge at the click (vanilla's charge rule) and starts the charge again, as a click does.
  - **The blow:** at the strike tick each foe is struck through `Player.stabAttack`, vanilla's thrust attack, with the charge put back to its value at the click (one accessor mixin, `AttackStrengthAccessor`), and the item's trait bonus added.
  - **Protection:** foes that other code protects from the player (`AttackEntityCallback`, such as the town's townsfolk) are skipped.
  - **Instant hits refused:** vanilla's instant hit with a two-handed arm is refused on the server, so a modified client cannot skip the swing.
- **Timing:** the strike tick is when the kind's motion (`tools/arms_moves.py`, `"blow"`) lands the blow, so what is seen and what is dealt agree. Other players see the swing begin from vanilla's swing packet, as before.

## Connections
- Existing input producer: bronze ingots (tin) and steel ingots (the steel foundry), sticks and leather.
- Existing output consumer: the player, against mobs and, where PvP is on, players.
- Recipes (shaped, `#` the metal's ingot):

  | Arm | Pattern | Cost |
  |---|---|---|
  | Zweihander | `  #/## /L# ` | 4 ingots, leather |
  | Maul | `###/###/ S ` | 6 ingots, stick |
  | Executioner's Sword | `## /## / L#` | 5 ingots, leather |
  | Bill | ` ##/ S /S  ` | 2 ingots, 2 sticks |

  None repeats another Jugcraft recipe (every shaped recipe was compared, mirrored too) or a vanilla tool's shape.
- Technology connection: the metal tiers of batch 25, as the other arms; electroplating repairs and plates them. Magic connection: none.
- Reachable entry path: as batch 42 (bronze early, steel from the foundry); no arm unlocks anything. Required vs optional: optional; each can be crafted solo and traded.
- Specialty: a two-handed arm is the crowd fighter's choice: slower and committed, but it strikes several foes at once.

## Balance and automation
- Every kind still deals less a second than its metal's sword (bronze 9.6, steel 10.4) to one foe. The check counts the finishing blow over the combo:
  - zweihander 9.1 / 9.5;
  - maul 8.0 / 8.4;
  - executioner's sword 8.7 / 9.1;
  - bill 9.1 / 9.6;
  - greatsword now 9.0 / 9.45.
  
  `check_arms` fails if one reaches the sword. The cleave is the reward: several foes for one swing, paid for with the wind-up, the slow and the wear (each foe struck wears the arm).
- The charge rule is vanilla's: a click before the charge is full lands a weaker blow, however long the wind-up.
- The quake's share, the execute and the hook are first values for the owner to tune.
- No conversion, no recycling.

## Multiplayer and persistence
- **Server authority:** the client only asks for a swing. Everything is decided on the server from its own entities:
  - whether a swing starts;
  - when it lands, and whom it strikes (reach, arc, sight, protection);
  - how hard.
  
  The request has no data to forge, and one swing at a time is the rate limit.
- **Other players:** they see the swing from vanilla's swing packet, as before.
- **Saved state:** none. A swing in the air lives only in memory; leaving, dying or switching away cancels it, and the slow is a transient modifier.
- Saved as ordinary items with stable ids (`jugcraft:<bronze|steel>_<zweihander|maul|executioner|bill>`). Recipes follow the `tin` and `machines` switches; the items stay registered when a switch is off.

## Dependencies and assets
- No new dependencies. One new mixin, `AttackStrengthAccessor` (an accessor on `LivingEntity.attackStrengthTicker`). Vanilla reads the attack charge only from that protected field and can only reset it, and the blow must be as strong as the charge was at the click.
- One new packet: `jugcraft:two_handed_swing` (client to server, empty).
- Art: four 64x64 sprites a metal, by `tools/arms_art.py`:
  - a zweihander with curled quillons, a leather-wrapped ricasso and parrying lugs;
  - a banded maul;
  - a broad, blunt-ended executioner's sword;
  - a billhook.
  
  Motion: guards, combos and first-person strokes in `tools/arms_moves.py`. All original.

## Verification
- `python3 tools/check_mod_data.py` (`check_arms`):
  - Java and `tools/arms.py` agree on the kinds, traits, constants and the two-handed table;
  - every two-handed kind's strike is within half a tick of its animation's blow, and every one of its attacks has a key there;
  - its combo has as many attacks as its motion, and its swing fits between its blows;
  - damage a second with the finishing blow stays below the sword;
  - the mixin is registered.
  
  A wrong strike tick or finisher was put in on purpose and the check failed on each.
- Game tests (CI job `gametest`, `ArmsIIIGameTests`, a mock player with a full charge and still husks with no armor):
  - `aTwoHandedBlowLandsLateOnEveryFoeInItsArc`: a greatsword's blow has not landed a tick before its strike tick. After it, it has struck the three husks in its arc for exactly the player's attack damage, and not the one behind or the one aside; the wielder is slowed until the swing ends;
  - `aComboEndsInAFinishingBlow`: the second greatsword blow within the window deals 1.25 times the first;
  - `aShieldInTheOffHandStopsATwoHandedSwing`: a shield stops a war hammer's swing; a torch does not;
  - `switchingAwayCancelsTheSwing`: putting the glaive away before the blow cancels it and the slow;
  - `aMaulsFinishingBlowShakesTheGround`: the cleave strikes the husk ahead. A husk 1 block behind takes half the finishing blow and Slowness II; one 4 blocks aside is untouched;
  - `anExecutionerFinishesTheWounded`: +50% of a blow against a husk at 30% health, nothing against a hale one;
  - `aBillHooksRidersOutOfTheSaddle`: a husk riding a horse is dragged off and pulled towards the wielder.
- Client game tests (CI job `client`):
  - `TwoHandedClientGameTests`: the real attack key, pressed with a greatsword at a husk. Two ticks later the husk is unhurt, the server has the swing and the player is slowed; after the strike it is hurt. With a shield in the off hand the same press does nothing. Screenshots `jugcraft_two_handed_blow` and `_shield`;
  - `ArmsMotionClientGameTests`: the new kinds' guards, and each two-handed kind caught at its blow; first person for the zweihander and maul too;
  - `ArmsClientGameTests`: the racks in three parts (`jugcraft_arms_bronze_rack`, `_ii`, `_iii` and the steel ones) and the frames.
- **Not run:**
  - play: how the wind-up, the slow and the cleave feel, and whether the timings are right;
  - two players or PvP;
  - latency (the blow lands on the server's clock, about one round trip after the animation shows it);
  - a swing inside a town.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions. Mobs holding these arms (none spawn with them) use vanilla's attack.

## Rollout and open questions
- All timings and numbers are first values for the owner to tune after play: the strike ticks, arcs, slow, finisher and queue window.
- Possible later batches, from what Fiery Combat does and this does not: a guard and perfect-parry window by sneaking, a poise or stagger meter, a plunging slam, sprint attacks, hit-stop and trails.
- Turning the system off would mean one switch in `TwoHanded` and the client input. None is added: the behavior is part of what these arms are.
