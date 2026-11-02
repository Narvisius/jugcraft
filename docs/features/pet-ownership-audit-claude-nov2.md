> Historical Claude handoff, retained for provenance. Local integration and validation are recorded in [pet-inventory-nov2.md](pet-inventory-nov2.md). Current code uses persistent stable-ID modifiers and detach on storage/unload; older transient/clear-before-storage guidance below is superseded.

# Pet roster: ownership adapters, exclusions and edge cases

Repo `jimbozoomer-byte/jugcraft` at main `dcf6b249fae20c3244cd7f98613edc03d6ec8a5e`, read-only.

- **Paths:** relative to `src/main/java/io/github/jimbozoomer/jugcraft/`; Peepo classes are under `src/main/java/local/peepo/`.
- **Verified:** I checked these against the code myself.
- **Reported:** from a read-only audit pass that I spot-checked but did not re-read line by line.

## 1. Ownership adapters

### Already in Codex's core

**Vanilla `TamableAnimal`** (wolf, cat, parrot): verified.

- Owner: `isTame()`, then `getOwnerReference()` (`EntityReference`) and `.getUUID()`.
- Owner test: `isOwnedBy(LivingEntity)`.
- Sit: `isOrderedToSit()` / `setOrderedToSit(boolean)`. `setInSittingPose(boolean)` sets the pose.

**Vanilla `AbstractHorse`** (`net.minecraft.world.entity.animal.equine`): verified.

- Owner: `isTamed()`, then `getOwnerReference()` (it is an `OwnableEntity`).
- Sit: none.
- Notes: rideable. Saddle, armour and chest are in its NBT.

**`menagerie/MenagerieAnimal`** (extends `Animal`, not Tamable or Ownable): verified.

- Owner: `@Nullable UUID owner()`. Also `ownedBy(Player)` and `tame(UUID)`, which also sets persistence.
- Sit: `sitting()` / `sit(boolean)`.
- Notes:
  - It has its own follow goal (priority 5, owner within 6–32 blocks) and sit goal (priority 0).
  - Mounts are rideable. Cargo (27 slots), saddle, chest and BODY armour are in its NBT.
  - Aquatic, lava and flying species are distinguished by navigation type.
  - Multipart segments are rebuilt from the parent, so store only the parent.

**`creatures/chima/Chima`** (`PathfinderMob`): verified.

- Owner: `boolean tame()` (line 162), `@Nullable UUID owner()` (187), `tameTo(Player)` (205).
- Sit: `sitting()` (175) / `setSitting(boolean)` (179). Taming sits it.
- Notes: it has its own follow goal, which teleports via `comeToOwner`. It despawns only while wild (reported).

### Still to add

**`agriculture/Pumpkling`** (`PathfinderMob`, `CropGuard`): verified.

- Owner: `@Nullable UUID owner()` (134) / `setOwner` (138).
- Sit: `sitting()` (123) / `setSitting(boolean)` (127).
- Notes (reported): its head and spark ItemStacks are in its NBT. Sneak-bottling it calls `settle`, which discards it.

**Generic `OwnableEntity`** (any other vanilla or mod type): verified.

- Owner: `getOwnerReference() != null`, then `.getUUID()`.

**Trusted fox** (vanilla `Fox`): reported.

- Owner: trust comes through the mixin `mixin/OwnerFluteFoxAccess`; see `menagerie/OwnerSupplementFlute` lines 38–42.
- Sit: `Fox.setSitting`.

**Peepo companion** (`PeepoEntity`, `PathfinderMob`): verified.

- Owner:
  - `orders.tamed()` and `orders.owner(Player)` are public, as is `orders.allowed(Player)` (owner or party).
  - `CompanionOrders.ownerId()` is package-private, so reading the owner UUID needs a public accessor (a one-line change in `CompanionOrders`; your call).
- Notes:
  - It has its own modes FOLLOW / STAY / HOME / WORK / PORTER, so `PetBehaviors` leaves its movement and targets alone.
  - Before storing it, do what `MobTransportCrate` does (reported, line ~77): cancel social and routine, stop transport, leave its bed, clear rest.

## 2. Permanent versus auto-respawn familiars

**Real, permanent pets, even though they spawn as `MOB_SUMMONED`:**

- `menagerie/PocketPets`, line 167: the direwolf is a tamed `MenagerieAnimal` (`mount_direwolf`), capped at 4.
- `menagerie/PocketPets`, line 168: the siamese cat is a vanilla tame `Cat`, capped at 4.

So `EntitySpawnReason.MOB_SUMMONED` is **not** a temporariness signal.

**Auto-respawned. Exclude these; the player state owns their identity, not the entity:**

- **`nouveau/NouveauFamiliar`:**
  - `NouveauFamiliars.ensure(p)` runs every 100 server ticks (lines 37, 61–64). It recreates the familiar from the player's `ACTIVE` attachment whenever none is present.
  - The familiar discards itself when its owner is absent, when that species is no longer active, or when it is more than 32 blocks away (`NouveauFamiliar` lines 35–37).
  - Storing one would duplicate it, because `ensure` makes a new one.
  - Signal: `instanceof NouveauFamiliar`.
- **`concordance/spirits/HearthlingEntity`, and the `WorkerEntity` family generally:**
  - It is permanent but managed by `WorkerRoster`. `WorkerRoster.familiar(owner)` (line 96) holds the bound UUID, and a Hearthling discards itself when that UUID no longer matches (`HearthlingEntity` lines 113–116).
  - It is created by `BondingCharmItem` (line 56).
  - Starbuncle, NouveauHelper, Porter, Shade, WorkshopHand and Stolas are also `WorkerEntity`s with homes and beds (reported).
  - Recommendation: exclude the whole `WorkerEntity` family; they already have their own roster. To include them anyway, the summon must keep the same UUID, and the roster must not see them as "away" while stored.
  - Signal: `instanceof io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity`.

## 3. Temporary summons and constructs to exclude

| Kind | Signal | Why |
|---|---|---|
| Spellbook and Nouveau summons, including summoned wolves and horses, which are tamed and look exactly like pets | `nouveau/NouveauSummons.owned(LivingEntity)` (line 34, verified) | They carry owner, expiry and spellbook attachments. The expiry attachment is private (reported), and `enemyWolf` has it with no owner. |
| Jimta spell entities: `JimtaApparition`, `JimtaScapegoat`, `JimtaSpellCreature`, `JimtaConstruct`, `JimtaSummonedWeapon` (under `jimtafacts/spellbooks/`) | Class | Each has its own life or age limit (reported) |
| `JimtaWardEntity` | Not a `Mob`, so it never enters the roster | It has a `lifetime` |
| `botany/BotanicalPixie` | Class | It has a `summoner` and lives 200–600 ticks (reported) |
| `nouveau/NouveauAnimatedBlock` | Class | It has `expires` (reported) |
| `nouveau/BookWyrm` | Class | It is bound to a lectern `home` (reported) |
| `jimtafacts/industry/WorkshopRobot` | Class | A machine with a dock, cargo and charge |
| Rotten lackeys | `limitedLife` (`RottenCreatures` line 157, reported) | Undead with a limited life |
| `RottenStorage` HunterWolf | A vanilla `Wolf` with a `hunter` UUID (reported) | If a player tames one, it enrolls still carrying a stale hunter reference. Decide whether to clear it. |
| `wilds/WildUmvuthana` followers | Class plus `owner` and mask fields | Bound to the Sol Visage, capped at 10 by `WildFollowerLedger` (reported) |
| `town/Townsfolk`, `ribbits/Ribbit`, `nouveau/NouveauBeast` | Class | Residents, a merchant keeper, and weald guardians bound to a post. None is a pet. |

**Do not use these as temporariness signals:**

- `MOB_SUMMONED`, because PocketPets are permanent.
- The persistence flag, because summons set it too (reported).

## 4. Edge cases for recall and summon

- **Passengers, vehicles and leads.**
  - Every existing capture path rejects ridden, riding and leashed mobs (`MobTransportCrate`, `OwnerSupplementVessel`, `ArcaneCapture`, `OwnerBackpackMobs`; reported). Do the same.
  - Rope fence knots tie leashes (reported).
- **Same UUID on summon.** Other systems hold pet UUIDs (reported):
  - flute `SuppsFlutePets`;
  - Caverns totem and controller `ControlledMob`;
  - `WorkerRoster`;
  - bed occupants;
  - `TransportLedger`.

  Restore with the same UUID, and check for duplicates across every level first, as the crate (line 99) and vessel (line 47) do.
- **Conversions change the UUID:**
  - `BotanicalEquestrian` (tamed horse into a zombie or skeleton horse, then re-owned);
  - `Mutations`;
  - waddler into weald walker;
  - `ArcaneLostSoul.convertTo` (reported).

  The roster must re-key on conversion, or it will lose the pet.
- **Aquatic and lava species dry out off their fluid.** `MenagerieAnimal` does this at line 260 (verified). Summon one only into its own fluid. `PetBehaviors.catchUp` follows the same rule.
- **Block-bound state:** Peepo beds, homes and seats; `NouveauBeast` guard post; `Ribbit` home.
- **Babies.** Age is in the NBT, and the menagerie switches to the baby model.
- **Friendly-fire helpers recognise only vanilla `TamableAnimal` pets.** `WildMoonEffects`, `WildCold`, `WildGeomancy` and `WildSolar` all work this way (reported). Menagerie, Chima and Pumpkling pets are not protected from their owner's area effects. This predates the roster.
- **Existing bug, reported, not fixed (outside my scope):** `MobTransportCrate.owned` returns true for any non-Ownable mob. Another player can therefore crate someone else's tamed `MenagerieAnimal` or `Chima`.
