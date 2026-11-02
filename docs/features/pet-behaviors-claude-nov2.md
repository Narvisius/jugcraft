> Historical Claude handoff, retained for provenance. Local integration and validation are recorded in [pet-inventory-nov2.md](pet-inventory-nov2.md). Current code uses persistent stable-ID modifiers and detach on storage/unload; older transient/clear-before-storage guidance below is superseded.

# PetBehaviors: handoff

Base: jugcraft main `dcf6b249fae20c3244cd7f98613edc03d6ec8a5e`. Two new files, nothing else changed:

| File | SHA-256 |
|---|---|
| `src/main/java/io/github/jimbozoomer/jugcraft/pets/PetBehaviors.java` | `24eb55584960ee5cbfdb58c859f89b40bed9845341e0635c2c08846dfee914c6` |
| `src/gametest/java/io/github/jimbozoomer/jugcraft/test/PetBehaviorsGameTests.java` | `d3d59a93d88b6a287769dfa774c5ed208a6a7308dc42c004d0840294145089e1` |

Nothing is committed, pushed or published. Both files are syntax-checked only (tree-sitter). They have **not been compiled**: this cloud machine cannot reach Fabric's or Mojang's servers. Every Minecraft method used is one the repo already calls with the same spelling.

## Contract

`PetBehaviors.tick(ServerPlayer owner, Mob pet, String mode, int level)` is called once a second for each active registered pet.

- **Mode:** `FOLLOW`, `STAY`, `DEFEND` or `PASSIVE`, in any case. Anything else counts as FOLLOW.
- **Level:** clamped to 1–20, which matches `1 + xp/120`.

`PetBehaviors.clear(Mob pet)` undoes everything it set:

- the level bonuses, clamping health to the species' own maximum;
- the sit that STAY caused;
- the target that DEFEND gave;
- its two command goals;
- its per-pet state.

**Call `clear` before serialising a pet for recall**, and when it is knocked out, released, unloaded or leaves the loop.

**Do not `tick` a knocked-out pet.**

## What it does

**Movement** is read from the pet's own navigation, so it needs no list of species:

| Navigation | Movement |
|---|---|
| `WaterBoundPathNavigation` (including lava swimmers) | `FLUID` |
| `FlyingPathNavigation` | `AIR` |
| anything else | `GROUND` |

**Following** (FOLLOW, DEFEND, PASSIVE):

- **Start distance:** 6 blocks for ground pets, 8 for flyers, 5 for water pets, and never less than the pet's width + 3.
- **Speed:** the pet's own navigation at `pace(level)` (1.00–1.19), ×1.2 beyond 12 blocks.
- **Water pets:** their navigation cannot leave the fluid, so they come to the nearest shore and wait there.
- **No interference:** it does not drag a pet out of a fight unless the owner is more than 24 blocks away.
- **It does nothing while** the pet is ridden, riding, on a lead, sitting by its owner's hand, or in a different level from its owner.

**Catching up** happens after 3 seconds more than 24 blocks away, at most once every 10 seconds. It moves the pet 2–4 blocks from the owner, under the same rules as the menagerie pet flute:

- loaded chunks only; nothing is ever loaded;
- within the world border;
- only cells the owner may change (`Authority.mayChange`), with no fire;
- land pets onto a solid floor, out of any liquid;
- flyers into clear air;
- **water pets only into the fluid they are in now.** A stranded water pet is not moved at all, so it is never put on land;
- never pets more than 4 blocks wide or tall, ridden or riding pets, or pets on a lead.

**STAY:**

- Pets with a native sit use it, so the species' own sitting animation plays:
  - `MenagerieAnimal.sit`;
  - `Chima.setSitting`;
  - `Pumpkling.setSitting`;
  - tamed `TamableAnimal`: `setOrderedToSit` plus `setInSittingPose`.
- The entity tag `jugcraft.pet_stay` marks "STAY sat this pet down". It is saved with the entity, so a pet recalled or summoned in another mode is always stood up again.
- A pet its owner sat down by hand is never stood up.
- Pets that cannot sit (horses, for example) are held at their anchor by the **Hold** command goal and walked back if pushed more than 1.5 blocks away.

**PASSIVE:**

- Every second: clears the target, sets `setAggressive(false)`, and for pets that can fight also clears `lastHurtByMob` and calls `NeutralMob.stopBeingAngry()`.
- Between seconds: the **Calm** goal drops any target the moment a native goal sets one, before the pet can strike.
- Panic and fleeing are untouched.

**DEFEND:**

- **What it targets:** the nearest of:
  - monsters (`Enemy`) targeting the owner within `guardRadius` (8–12 blocks);
  - whatever hurt the owner or the pet in the last 5 seconds;
  - whatever the owner just struck.
- **What it never targets** (`mayFight`):
  - a player, so there is no PvP through pets;
  - the owner;
  - anyone's pet, mount, companion, spirit or summon (`owned`);
  - anything away from the owner and the pet.
- **Claims:** a target is fought only where `Authority.mayStrike(owner, target)` allows, which covers claims and town residents. `MenagerieAnimal.doHurtTarget` also re-checks `TwoHanded.allowed`.
- **Pets that can't fight:** they get no target (passive menagerie species, or no attack damage).
- **From level 5:** monsters stalking the owner glow for 2 seconds, refreshed while it lasts, at most 8 of them.
- Leaving DEFEND drops the target DEFEND gave.

**Every mode:**

- A pet never keeps its owner as its target.
- Level bonuses are applied as transient attribute modifiers, which are never saved and are re-applied on the first tick after a summon:

  | Modifier id | What it adds | At level 20 |
  |---|---|---|
  | `jugcraft:pet_vitality` | max health | +38% |
  | `jugcraft:pet_strength` | attack damage | +47.5% |

  A level change replaces them and never stacks them. Health is never raised by a bonus being added, so recall and summon can't farm it.
- **Recovery:** once a pet has lost no health for 5 seconds, has no target and is not on fire, it recovers 0.05–0.29 health a second. Any loss, including fire or drying out, restarts the calm.

**What it never does:**

- set blocks, drop or create items, or load chunks;
- add goals other than its own two (see below);
- touch Peepo movement or targets. Peepo keeps its native `CompanionOrders` and gets only the level bonuses.

## STAY and PASSIVE between the 1-second ticks: no mixin needed

`Mob.getGoalSelector()` is public in 26.3; Peepo's `CompanionLivestock` and `CompanionHusbandry` already add and remove goals on other mobs this way. So `PetBehaviors` installs **two** goals per pet, once, at priority **-2** (the same as Peepo's holds):

| Goal | Flags | Runs while | Effect |
|---|---|---|---|
| `Hold` | MOVE | STAY and the pet cannot sit (not ridden, not on a lead) | Holds the pet and walks it back to its anchor |
| `Calm` | MOVE, LOOK, one tick | PASSIVE, or STAY while not natively sitting, when the pet has a target | Drops the target, beating `MeleeAttackGoal` in the same goal pass |

**Safety properties:**

- **No double install.** The goals are installed only when the entity instance changes; a summoned pet is a new instance.
- **Self-expiring.** Each goal reads the loop's state and goes inert if the pet has not been ticked for 40 ticks, or if `clear` was called (which also removes them). A pet the roster forgets is never held forever.
- **Swimming still works.** Neither goal takes JUMP, so `FloatGoal` keeps working.
- **No pose flicker.** Calm skips natively sitting pets. Interrupting vanilla's `SitWhenOrderedToGoal` would reset its sitting pose.

**One limitation:** `WorkerEntity` pets use SmartBrainLib brains, not goal selectors. Your audit already excludes them.

## Integration notes (shared files I did not edit)

- **Register the tests.** In `src/gametest/resources/fabric.mod.json`, add `"io.github.jimbozoomer.jugcraft.test.PetBehaviorsGameTests"` to `fabric-gametest`.
- **Call order in your loop:**
  1. Skip knocked-out pets.
  2. `tick` each active pet once a second.
  3. On recall, call `clear(pet)`, then serialise.
  4. On summon, add the entity, then let the next `tick` re-apply bonuses and goals.
- **Mode sync.** A pet sat down by hand (shift-use) stays sitting in FOLLOW mode. If the UI should show STAY for it, set the stored mode from the native sit state (`PetBehaviors.sitting(pet)`).
- **Level.** Pass `1 + xp/120` (max 20). The bonuses are in `vitality`, `strength`, `recovery`, `pace` and `guardRadius` for UI display.
- **Optional helper.** `PetBehaviors.owned(LivingEntity)` is a broad "belongs to someone" test (tame/horse/menagerie/Chima/Pumpkling/Ownable/Peepo/WorkerEntity/`NouveauSummons.owned`) that your enrollment or exclusions could reuse.
- **Dedicated-server memory.** Per-pet state is in a server-thread `HashMap`. It is swept 5 minutes after a pet's last tick, if `clear` was never called.

## Tests (`PetBehaviorsGameTests`, not run)

| Test | What it checks |
|---|---|
| `levelBonusesAreModestBoundedAndNeverStack` | Bounds, monotonicity, a single modifier after repeated ticks, replacement on level change, and that `clear` removes the bonuses and clamps health. |
| `stayUsesTheNativeSitAndOtherModesOnlyStandUpWhatStaySat` | Wolf sit and stand. A hand-sat pet is respected. Clear before recall. A saved STAY tag stands up a pet summoned in DEFEND. |
| `stayHoldsAPetThatCannotSitBetweenTicksAndClearRemovesTheGoals` | Exactly 2 goals after 3 ticks. A horse pushed 4 blocks is walked back. `clear` leaves 0 goals. |
| `passivePetsNeverStrike` | A target set between ticks is dropped and the zombie is unhurt. |
| `defendersTurnOnMonstersButNeverOnPlayersOrPets` | `mayFight` refuses a player, another player's wolf and the owner. A player who hit the owner is ignored. A stalking zombie is targeted. Leaving DEFEND drops it. |
| `landPetsCatchUpButWaterPetsNeverLandOnLand` | A wolf 30 blocks away catches up. An aquatic menagerie pet in a walled pool stays in water. |
| `petsRecoverOnlyOnceCalm` | No recovery right after a hit; `recovery(20)` after 5 seconds calm. |

**Assumption to watch:** the catch-up test needs the mock owner to pass `Authority.mayChange` at the test site (no spawn protection, no town or claim there).
