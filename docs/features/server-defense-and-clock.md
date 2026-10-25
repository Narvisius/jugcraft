# Viewer server defenses and world rules

Status: local implementation; validation results recorded below. No hosted deployment or client-channel promotion.
Owner authorization: October 21 request to finish the two-player claim audit, lengthen days, shorten nights, protect villagers from mobs except pillagers, and make automatable defenses.
Implementation: OpenAI Codex (GPT-6).

## Day and night

The native Overworld clock runs at 0.5 during clock ticks 0–11999 and 1.5 during
12000–23999. At 20 simulation ticks per second this gives **20 minutes of daylight**
and **6 minutes 40 seconds of night**, including each half's twilight transition.
The calendar still advances one day per full cycle. This deliberately controls
the Overworld clock rate; manually setting a different clock rate is overwritten.
Other dimension clocks, simulation ticks, machine timers, combat and scheduled
functions are unchanged. Sleeping and `/time` continue to use native clock
markers. Pausing the clock or disabling `advance_time` remains effective. Native
time packets send the fractional rate to clients when it changes.

## Residents

Vanilla villagers and wandering traders cannot be targeted or injured by mobs
except the actual vanilla **Pillager** entity. This does not exempt every illager:
evokers, vindicators, ravagers, zombies and modded monsters are not exceptions.
Player damage, environmental damage and claim permissions retain their normal
rules. Target selection, explicit mob targets and final living-entity damage are
covered. The town's custom residents also reject non-pillager targets, while
retaining their pre-existing invulnerability; this update does not make town
merchants or guards killable.

## Automated defenses players can build

Use the existing **Bastion Autocannon**, tower guns or other crewed artillery with
a **Fire Control Table** and **Fire Control Wire**. No duplicate turret family or
new artwork is introduced. Existing recipes, animated models and ammunition
remain canonical.

1. Place the gun on its required solid platform and a Fire Control Table within
   64 blocks. Keep the table, guns and supply racks within the same claim owner's
   land (adjacent claims owned by that player work).
2. Use Fire Control Wire on the table, then on each gun. Up to eight guns can share
   one table. Empty-hand the table to select **Sentry**; sneak-use changes its
   90/180/270/360 degree sector.
3. Put the correct shells in a **Ready Rack** within two blocks of the gun.
   Hoppers, item pipes and Ammo Hoists can replenish racks. Each fired barrel
   consumes a real shell; there is no free ammunition or requirement for an
   online player to stand at the gun.
4. Leave the table unpowered for automatic defense. **Redstone power stops Sentry
   fire**, allowing a lever, timer or other circuit to control it. Fire-mission
   modes retain their existing rising-edge salvo behavior.
5. A comparator reports how many linked guns are ready. Use existing sandbags,
   bastion parapets, blast doors and gates to fortify the position.

Sentries scan at bounded intervals and aim only at hostile mobs, excluding owned
creatures. They hold fire with a player, villager, pet or other non-hostile living
bystander within eight blocks of the target. Every unattended shell, including
redstone fire-mission shells, **also spares friendly living creatures at impact**,
so someone entering the blast after launch is protected. Shells never destroy
blocks. Manual gunners retain existing combat/claim rules.

A table records its operator. It rechecks current land permission before acting;
a sale, reassignment or removed trust suspends an old operator. A new authorized
user can recommission it by using the table. Existing ownerless tables in claimed
land need one authorized use; unclaimed legacy tables continue to operate.
Unloaded tables/racks are not force-loaded. A player cannot use an old wire
selection to take control of another owner's table or gun.

## Claim audit fixes

- Actual player-caused living damage (including bullets and custom blast damage)
  to protected creatures checks current land permission, not only the initial
  attack click. Wild hostile mobs are exempt from this land check as described below.
- Display entities and the main vehicles/artillery additionally guard their
  overridden damage handlers.
- Bucket raycast pickup and placement check the affected position.
- Item, fluid and electrical networks partition discovery by ownership. Cached
  partitions expire when claims change. Each transfer rechecks its endpoint;
  discovery and transfers do not force-load chunks.
- Extractor intakes, sorter pass-throughs, conveyor pickup/output, pump intake,
  ammunition hoists and ready-rack withdrawal enforce the same boundary.
- Native hopper output/loose-item pickup and dispenser/dropper output are guarded.
- Machine ports must share ownership with their controller; automated access to
  double chests spanning different owners is refused.

Unattended machines have no trusted-player identity. Sharing a claim with a party
lets friends operate machinery inside it, but does not open transfer across two
different owners' boundaries. Build shared factories on one owner's shared land.
This is consistent with the existing fluid/piston boundary policy.

## Hostile combat on claimed land (23 October 2026)

Owner-reported bug: visitors could not fight hostile mobs on another player's
claim. Both the attack callback and final player-caused damage callback required
building permission for every non-player entity. The combat exception now covers
wild `Enemy` mobs in both paths, including arrows, bullets and player-attributed
magic. A hostile does not need to acquire a target first; naming it does not make
it a pet. Non-hostile animals, residents and decorations still require claim access.

Owned creatures remain protected, including Menagerie creatures whose hostile
class remains after taming, native owner references even while the keeper is
offline, and Nouveau player summons. An evoker's native vex owner is another mob,
so that reference does not turn the vex into player property. Player-summoned
vexes remain protected by their summon ownership. Multipart hostile hitboxes use
their parent's identity, ownership and claim position, including a body segment
that crosses the boundary while its parent remains inside a claim.

The exception grants combat access only. Entity use, building, containers, trust,
party sharing and existing PvP/town protection keep their own checks. Neutral
animals do not become attackable merely by acquiring a target. No save migration,
new registry, dependency, progression gate, recipe, loot or damage-value change
is introduced. Existing claims receive the correction when the server mod updates;
reverting restores the previous combat restriction without changing saved claims.

`ClaimCombatGameTests` exercises actual survival-player melee, an arrow crossing a
claim border, loaded gunfire, player-attributed magic, hostile multipart damage,
pet and summon protection, owner access, unclaimed animals, and retained use/build
permissions. These use simulated server-player identities, not two authenticated
clients. Implementation: OpenAI Codex (GPT-6), under direct owner authorization.

Validation on 23 October, based on upstream `f904df3fd`, with unchanged Minecraft
26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.0.4.1+1 and Gradle 9.6.0:

- The corrected baseline ran the five new tests against the compiled pre-fix
  production code. Four failed on blocked wild-hostile combat; livestock/pet
  protection passed. The arena explicitly keeps both sides of a chunk boundary
  active so projectile/entity lookups exercise real loaded entities.
- `runGameTest compileClientJava --offline --console=plain` with
  `fabric-api.gametest.filter=*:*claim*`: **all 21 required tests passed**, build
  successful in 1m08s. Common/client/test compilation passed. The runs reused
  unchanged production resources prepared by the first run (`-x processResources`).
- After making the multipart boundary assertion explicit, the five focused
  `ClaimCombatGameTests` passed again; build successful in 45s.
- Repository/document links and whitespace checks passed.
- Not run: the full GameTest suite, rendered-client play, two authenticated clients,
  production-world installation, save/restart or server-load benchmarks. The fix
  changes existing event checks only and introduces no per-tick scanning.

## Validation

The combined local server suite passed all **2,254 required GameTests** on October 21 (`final-validation.log` in the local review folder), including all seven new server-defense tests. A subsequent 2,256-test combined run also passed the defense tests; its two alchemy test-expectation failures were corrected and the affected 23-test group passed separately. Tests use separate **simulated server players**, not
two authenticated clients. `ServerDefenseGameTests` exercises real loaded gun
shots, blast damage, native buckets/hoppers, item and fluid transfers, ammunition
withdrawal, ownership revocation, mob targeting/damage, native fractional clock
accumulation and an unattended sentry firing actual shells after its redstone
stop is released. Existing fire-control and logistics tests remain enabled.

Before release: owner playtest and a hosted two-account acceptance run. This audit
is not an exhaustive security proof for every third-party world-changing machine
or every indirect attack in all imported content. Continue targeted regressions
when new automation and weapon paths are introduced.
