# Arcane Concordance: spirits, familiars and constructs

Status: merged to main ([#244](https://github.com/jimbozoomer-byte/jugcraft/pull/244), from branch
`claude/awesome-davinci-iwv3b9`, integrated by [#263](https://github.com/jimbozoomer-byte/jugcraft/pull/263) on
8 October 2026); see Verification for what has run. Extended by Concordance Chunk D, unit 1 (D-01: the agreements
keep their promises, and a familiar fades only for its person's own play time; D-02: one pact record; [the chunk's
contract](concordance-chunk-d.md)), not merged; its tests passed in CI with its unit review's fixes (run 40067977680 on
`da185dd0d`; see Verification). Extended by Concordance Chunk D, unit 4 (D-08: the Workshop Hand, a second spirit,
carrying a Courier Post's requests; [The Workshop Hand](#the-workshop-hand-concordance-chunk-d-d-08)), built beside unit
2 and not merged; compiled in CI, and its tests passed in run 40085012234 on `86c4e7127` (both server jobs and every
client class; see Verification). Extended by Concordance Chunk D, unit 2
(D-03 to D-05: [Stolas's pact](stolas-pact.md), a pact kept at a Pact Perch), not merged; its tests first ran in CI
run 40090825848, which failed two of them as defects of the tests (fixed in `8a3425f9e`), and passed in runs
40094972598 (on `8a3425f9e`) and 40133540741. Extended by Concordance Chunk D, unit 3 (D-06: Stolas's character;
D-07: [the roster screen](#the-roster-screen-concordance-chunk-d-d-07)), built on its own local branch and merged
into the pact hall at `554d71ab1`, not merged; its tests were first compiled and run in CI run 40121823509 on
`040d73ece`, where they passed (after run 40120355940 failed to compile the roster client test), and passed again in
run 40133540741.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 17) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 9, Practitioner stage (Jugcraft Workshops). Roadmap step 17.
Primary specialty and supported player role: the Spiritbinders. Helpers with their own terms: a familiar for one
person, a spirit for a place, a construct for a route.

Builds on [research and notes](arcane-concordance-sharing.md), [typed resources](arcane-concordance-sharing.md) (Bound
Will records, Ley Charge), the [shared effect system](arcane-concordance-composition.md) and the
[Ley Pylon](arcane-concordance-rituals.md). Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

Once First Light is understood, examining something that binds (a lead, a name tag, a soul lantern, a saddle, an echo
shard) begins the **Binding Arts**; studying one at a Lampwright's Bench (or reading notes) understands it. Three kinds
of helper then answer you, each with its own rules.

**A familiar: the Hearthling.** Use a **Bonding Charm** (string, a gold ingot, glow berries, an amethyst shard) to bind
one; a person keeps one familiar. It keeps within 8 blocks of you. Your **bond** grows by time spent together (a point
every ten seconds you are near it, at most 24 a day) and fades by 4 for each day of your own play time spent away from
it (here or in another dimension; time you are logged out never counts, and nor does play while the Concordance is
switched off with the familiar loaded). At 20
it mends you when you are at half health or below (Regeneration for five seconds, at most once a minute); at 60, twice
as often. Use the charm again to call it to your side; sneak and use it to release it. It never follows you through a
portal: while you are in another dimension or offline it waits where it is and says so.

**A spirit: the Gathering Shade.** Use a **Spirit Anchor** (polished deepslate, an amethyst shard, a soul lantern) to
seal an **agreement**, recorded as a Bound Will that you hold. Its terms: it gathers dropped items within 8 blocks of the
anchor, from 19:00 to 05:00 by the world clock, at most 64 times a day, carrying 16 at a time, and brings them to the
anchor's nine slots (a hopper below takes them out). Outside its hours, its area or its work it waits at the anchor and
says it is outside its agreement; when its day's work is done it is finished until tomorrow. It never takes what you
could not take yourself. Sneak and use the anchor to **suspend** or resume it; anyone may use the anchor to read its terms
and what the shade is doing. Break the anchor to **release** it: the Bound Will is released, the shade leaves what it
carried and departs.

Only the anchor's owner (who placed it) or their party may seal at it, and only where they may build; an anchor placed
before Chunk D, whoever may build there; never a stand-in. A refused seal says why (not yours, the anchor already keeps
one, you hold 32 agreements, your roster is full) and spends nothing. If the shade is killed, what it carried goes into
the anchor (or drops where it fell, whatever the block-drop rule says, if the anchor is not loaded or keeps someone
else's agreement), the anchor says it was lost, and its holder's next use calls another under the same agreement at no
cost. A copy of an anchor (its data at another place) does nothing, says another agreement covers it, and breaking it
releases nothing; nor does breaking an anchor while the Concordance is switched off.

**A construct: the Clockwork Porter.** Unfold one (copper, a barrel, redstone, iron) on a block; it is yours. Use a
**Porter Key** on a container (its source), then on another (its target), then on the porter. It carries up to 16 items
a trip. Each delivery spends 2 of the 64 Ley Charge it holds and wears 1 of its 64 integrity; it draws charge at its
source from a Ley Pylon within 3 blocks that your party may use, and each copper ingot mends 16 integrity. Your party
may route and repair it; no one else may.

**Why it waits.** Every worker always says what it is doing (following, mending, on its way, working, taking its load
home) or exactly why not: idle; waiting for resources; blocked by access; cannot find a way; outside its agreement;
finished; its person not here; another dimension; its destination not loaded; suspended; needs repair; out of Ley
Charge; full; switched off. Look at it (Jade), use it with an empty hand, or ask `/jugcraft concordance workers`, which
lists all your workers, loaded or not, with what each last said and where.

## How it works

**Definitions** (`data/<ns>/concordance/worker/`, read by `WorkerParser` into `WorkerCatalog`, `concordance/worker`,
pure Java): three kinds with their own fields, read strictly. A familiar's follow distance and support effect (amplifier
0 or 1, at most 10 seconds); a spirit's work (`gather`, the only kind with a server behaviour), radius (at most 12),
hours, daily quota and load; a construct's integrity, wear (no more than its integrity), repair item and amount, energy,
trip energy and load. Each kind keeps its own model, saved with it and never in its brain:

- **Bond** (`worker/Bond.java`, on the Hearthling): strength, the day's gain and the game-time day it belongs to, the
  last mending, the last visit, and the play time it counted to (`seen_played`). Growth is worked out from game time and
  fading from its person's own play time (the game's play-time statistic), so turning the clock changes neither and
  time logged out never fades it (Concordance Chunk D, D-S17).
- **Agreement** (`worker/Agreement.java`, on the Spirit Anchor, kept through its `AgreementHolder`, shared with Chunk
  D's perch): its Bound Will record's id, its holder, the work, dimension, centre, radius, hours, quota, the day's tasks
  and whether it is suspended. `permits` names the reason a task is refused: suspended, outside the agreement (work,
  dimension, area or hours) or finished (the quota). The Bound Will itself lives in the world's `BoundWills` ledger (one
  record a sealed agreement, never merged or split). Since Concordance Chunk D the Bound Will (`BoundWill.Pact`) is the
  one authority for whether it stands, where it is kept, whether it is suspended (by its holder, or for upkeep), the
  service terms and day's count it was sealed with, and its upkeep and escrow; an anchor's terms count only while its
  record keeps them for the same holder at that place, and the anchor's own `suspended` is kept only so that an older
  save reads (moved onto the record once, then written false).
- **Body** (`worker/Body.java`, on the porter): integrity and Ley Charge, with the trips, repairs and charging its
  definition allows. `ready` names the reason it cannot go: needs repair or no energy.

**Decisions** (`spirits/`): every second each worker `think`s once on the server and leaves one `Status`
(`worker/Status.java`), an intent or a reason. The roster (`WorkerRoster`, world data) records each worker's kind, last
status, dimension and place when its status changes, so the `workers` command answers for unloaded workers without
loading them. A familiar its person released while it was unloaded strikes itself off when it next loads. A spirit
sealed or called again is noted at once; one missing from the roster notes itself when it next thinks, if it is still
its anchor's spirit, and a change of status never notes one that is not on it, so a replaced or switched-off shade takes
no room; one killed is struck off, which is how its anchor knows it was lost.

**Movement** is SmartBrainLib's: `WorkerEntity` is a `SmartBrainOwner` with a nearby-players sensor and two core
behaviours (`LookAtTarget`, `MoveToWalkTarget`). A decision sets the walk-target memory; SmartBrainLib walks there. When
the brain records that it could not reach its target, the worker counts it (`worker/Navigation.java`): after three
failures it stops, says it cannot find a way, and tries again after thirty seconds. Only `WorkerEntity` touches the
brain, and the brain's memories hold nothing a worker owes or carries.

**No chunk loading, no portals.** A worker in an unloaded chunk does nothing; one whose destination (its anchor, its
person's place, a porter's source or target) is unloaded waits and says so; none uses a portal. A familiar may hop
beside its person in the same level when it is more than 24 blocks behind, and only to a loaded, open spot.

**Access.** A shade takes only items its holder could take, and a porter only uses containers its keeper could change
(`Illumination.mayChange`: world bounds, spawn protection, the player's build rights and town protection); otherwise
it says it is blocked by access. A porter uses plain containers only (not furnace-like ones that take items by side).

**Presentation.** GeckoLib draws each worker from its own original model (`geckolib/models/entity/`) and plays the
looping clip for its synced status: the Hearthling's idle, following, supporting and waiting; the shade's idle,
travelling, working and suspended; the porter's idle, walking, working and broken. Jade shows the status and the
model's own numbers (bond; carried; integrity, charge and load).

**Pacts** (Concordance Chunk D, unit 2; not merged; [Stolas's pact](stolas-pact.md)). The Pact Perch is a second holder
block on the same `AgreementHolder` as the Spirit Anchor, and Stolas a fourth worker entity (not a fourth definition
kind: his offer is the Concordance's `pact` data, `concordance/pact/stolas.json`):
- the roster's kind `pact` (`workers.kind.pact`), noted when the pact is sealed or its presence is called again;
- the perch's look once a second, spread by position (`PactKeeping.tick`): unit 1's upkeep look, and a lost presence
  called back at no cost while its holder is within 64 blocks;
- breaking a holder block now depends on the block: the anchor releases its agreement as before (`OnBreak.RELEASE`); the
  perch leaves its pact without its place (`OnBreak.UNSEAT`), and `AgreementHolder.seat` seats a pact without its place
  (or `stranded`: broken while switched off) at a new block of its owner's, with the same record, terms, day's count,
  upkeep clock and escrow, refused with one of the `workers.seat_*` outcomes (`SEAT_OUTCOMES`: seated, disabled,
  not_holder, nothing, not_permitted, taken, roster_full, unavailable);
- `StarReckoning`, Stolas's service: once a game day at the perch, a pattern its holder observed recalled through the
  one recall path (`AstrolabeItem.recall`), and the pact's first obligation (`practice jugcraft:pact_service`, never
  `worker_service`, so the Binding Arts' mastery keeps its three kinds);
- since unit 3 (D-06): Stolas's raven body, his loops and the gestures the server fires on a second GeckoLib controller,
  his sounds with subtitles and Jade's lines ([Stolas's character](stolas-pact.md#stolass-character-d-06)). His
  presence's departure lingers 40 ticks for its gesture: struck off the roster at once, never saved meanwhile, and never
  a perch's visitor or presence again (`gone()`, D-P97).

## Connections

- Input producer: First Light (the Binding Arts need it understood); Ley Charge from Ley Pylons (Radiance or
  electricity) for porters; copper for repairs; any dropped items for shades.
- Output consumer: the familiar's mending (through the shared effect boundary, with the familiar as its cause); items
  gathered into the anchor or carried between containers, for hoppers and later logistics (step 18).
- Technology connection: porters run on Ley Charge, which a pylon makes from Jugcraft electricity (1000 JE for 1).
- Magic connection: Bound Will records (typed resources, step 7); the familiar's support is an effect like any spell's.
- Reachable entry path: First Light understood → examine a lead → study at the bench → craft a charm, an anchor or a
  porter (vanilla materials only).
- Solo, trade and cooperative routes: entirely solo; a porter serves its keeper's whole party; a shade's agreement is
  its holder's alone.
- Specialty use without other branches: needs only First Light (and a pylon for a porter's charge).
- Mastery: being served once by each kind (a familiar's mending, a spirit's delivery, a construct's trip) masters the
  Binding Arts.

## Balance

- **No positive loop**: no worker makes items; shades and porters only move what exists. A porter's 64 charge is 32
  trips (energy runs out before wear), and its charge costs Radiance or electricity at a loss (3 Radiance for 2 Ley
  Charge, or 1000 JE for 1; step 12).
- A shade's day is bounded by its quota (64 tasks of at most 16 items) and its hours; a familiar mends at most once a
  minute (thirty seconds at full bond) with Regeneration I for five seconds.
- The bond needs days: at most 24 a day, 20 to start mending and 60 for the faster mending, fading by 4 a day of play
  time away.
- Roster and Bound Will ledgers keep at most 32 entries a player; brains run two light behaviours and one sensor.

## Multiplayer and persistence

- Server authority: binding, sealing, routing, repair and every decision are the server's; the client draws the synced
  status. Uses of the charm and the anchor are rate-limited.
- Permissions: only a familiar's person commands it; only the anchor's owner or their party seal at it, never a
  stand-in; only a shade's holder suspends it, calls another and ends it (`AgreementHolder.end`: since Chunk D unit 2
  the pact perch's screen calls it for Stolas's pact, and since unit 3 the roster (D-07) for every agreement and pact
  its reader holds; breaking the anchor still ends a shade's agreement in game); only a perch's owner, in person,
  seals a pact there, and only a pact's holder
  reckons, pays, suspends, ends or seats it; anyone may read its terms; breaking
  the anchor needs the right to break it; only a porter's keeper's party routes or repairs it.
- A shade whose holder is away (offline, dead or in another dimension) keeps its agreement but waits at its anchor and
  says its person is not here; it takes nothing for an absent holder unless the server's
  `concordance.absent_owner_authority` lets a stand-in answer protection questions for them.
- Persistence: the bond (with the play time it counted to), the porter's body, route and load, the shade's anchor and
  load and each worker's person or keeper on the entity; the anchor's owner, the agreement, the day's tasks and the
  delivered items on the anchor; Bound Will records with their optional pact and escrow (`jugcraft:bound_wills`), the
  pact history (`jugcraft:pact_history`) and the roster (`jugcraft:worker_roster`) in the world's data; a key's route
  in its `jugcraft:porter_route` component. The new keys are optional and every stamp stays at 1; a downgrade drops
  them on its next write (unsupported).
- PvP: no worker attacks anyone.
- Disable behaviour: with `concordance.enabled=false` every worker stops and says it is switched off; nothing is
  unregistered or lost. While switched off nothing of a pact falls due, is suspended, released or ended (breaking an
  anchor then releases nothing: its Bound Will stays kept at that place, and the next seal there leaves it without its
  place), but a killed shade still leaves its load. A loaded familiar counts none of its person's play meanwhile; one
  not loaded then counts that play at its first look after the switch is on (Concordance Chunk D, D-P13, proposed).

## Dependencies and assets

No new dependency. Framework use:

- **SmartBrainLib**: the workers' brains (a players sensor, look and walk behaviours, the walk-target and can't-reach
  memories). Decisions stay in Jugcraft's own models; the brain only moves the body.
- **GeckoLib**: three original entity models with their animations, one looping clip per status group.
- **Jade (optional)**: worker status, bond, load, integrity and charge; since Concordance Chunk D, unit 3, a pact
  perch's and a Spirit Anchor's agreement (`PactDataProvider`) and Stolas's pact, naming no player and nothing of the
  escrow.
- **Shared effect system**: the familiar's mending goes through `ConcordanceEffects` like any spell, so protections,
  stacking and attribution apply once.
- **Modonomicon**: a new **Binding** codex category (the Binding Arts, familiars, spirits, constructs, and every
  reason a worker can give).
- **GuiLib**: not used. The management controls are the charm, the anchor, the key, an empty hand on a worker and the
  `workers` command and the roster screen (Concordance Chunk D, D-07), a native screen.

Art: three original GeckoLib entity models (64x64 sheets, one looping clip per status group) by
`tools/concordance_worker_models.py`, and their sheets, the Spirit Anchor's faces and four 16x16 item icon maps by
`tools/concordance_worker_art.py` and `tools/item_icons/`. Provenance: every colour on the sheets and the anchor's faces
is one the owner painted, read from the owner library (`art/owner-library/originals/Blocks/`: the treated brass lamps
and orange niter glass for the Hearthling; umbran log and leaves, the lead block and jacaranda planks for the Gathering
Shade; the cog knight, copperplate, light-blue niter glass and the wooden basket for the Clockwork Porter; blackstone
spines and bulb and treated iron for the anchor); only colours were taken, no pixel, shape or layout, and nothing from
Mojang's files. The icons are drawn fresh as maps. The models' rotation directions (wings fold back, the shade's arms
reach forward, a broken porter slumps forward) were reasoned from the Bedrock conventions GeckoLib follows and have not
been seen in game. The art has not been shown to the owner yet.

## Verification

- `python3 tools/check_mod_data.py`: new step 17 checks (`check_workers`): the Java bond, navigation, parser, roster
  and visit numbers equal the generator's; the statuses are the generator's in order and include the five reasons the
  brief names, each with its text; the definitions are the generator's, one of each kind, each with its entity type,
  name, client renderer, and GeckoLib model, animations (every clip the Java names) and sheet; mastery needs all three
  kinds' service; no worker class loads a chunk or crosses dimensions, and only `WorkerEntity` touches brain memories;
  every message and Jade line has its text; the items and the anchor have recipes and icons that are their maps. The
  pure-package rule covers `concordance/worker`; `check_geckolib` now checks entity models as well as blocks.
- The pure core compiles with JDK 21. The step 17 harness passes **35 checks** against the generated data: the three
  kinds load separately; every reason the brief names exists and is not an intent; a day together grows the bond by its
  daily limit and days reach the full bond; three days away fade it by 12; an unbonded familiar does not mend and a
  bonded one mends once a cooldown, faster when stronger; an agreement permits gathering nearby at night and refuses
  noon, outside its area, other work and another dimension, is finished after its quota and works again the next day,
  waits when suspended, and keeps hours past midnight; a porter without charge says so, fills only to its capacity,
  makes as many trips as its energy and wear allow, needs repair when worn out and is mended up to its integrity;
  navigation gives up after three failures and retries later; and the parser refuses unknown work, wear beyond
  integrity, too strong a support and an unknown kind.
- Game tests added: `ConcordanceWorkerGameTests` (four): a familiar mends by its bond (bound by the charm and held by
  its person's roster; recalled, never doubled; no mending unbonded; bonded, it mends its badly hurt person once and not
  again within its cooldown; sneaking releases it; a familiar whose person is offline says so); a spirit keeps to its
  agreement (sealed as a Bound Will; at night it gathers and delivers to its anchor, one task of its quota; at noon it is
  outside its agreement; suspended, it says so; the anchor broken, the Bound Will is released and the shade departs); a
  porter says why it stops (no route: idle; an empty source: waiting for resources; it carries and delivers, spending
  energy and wear; a full target: full, keeping its load; worn out: needs repair, and copper mends it; unfuelled: no
  energy, and a pylon by its source fuels it); and the workers' models survive a save.
- CI: the first push (e52f3564) failed to compile: SmartBrainLib 2.0.3 has no `BrainActivityGroup` or
  `SmartBrainProvider`, takes its sensors from `getSensors(owner)` and builds and ticks the brain itself, and a
  `@Nullable` on a qualified type must sit on the simple name. Fixed in 86ff567d. Run 37619575937 (commit 8c20db46, which
  also carries step 18) passes the whole Build workflow: it builds, passes the data checks and all 990 required server
  game tests (the four above among them), and the client test shards pass.

Not yet run: any client (the models, animations, icons, Jade lines and codex pages in game), a two-client dedicated
server, real walking over real terrain (the tests stand workers within reach), and a night of a shade's work.

**Concordance Chunk D, unit 1: D-01 and D-02** (commits `56a74f4be` (D-01), `7477efbcd` (D-S17), `fa5360d0d` (D-02),
the records' commit `97e1facd4` and "Chunk D unit 1: fixes from the unit review";
[the chunk's contract](concordance-chunk-d.md), its As built notes and proposals D-P1 to D-P16; not merged). **CI:**
first compiled and run by CI; with the unit review's fixes and 26.3's white wool, run 40067977680 on `da185dd0d` passed
every job.
- New server game tests (29), passed in CI run 40067977680:
  - `ConcordanceWorkerGameTests` +17 (22 in all). D-01: `aKilledShadeLeavesItsLoadOnce` (into its loaded anchor once,
    struck off, a second removal gives nothing; 25 ticks), `aKilledShadeAwayFromItsAnchorDropsWhereItFell` (the anchor
    unloaded; switched off too), `aLostShadeIsCalledAgainUnderItsAgreement` (the same Bound Will, nothing spent; a full
    roster refuses; a Bound Will gone is forgotten), `anUnloadedShadeReplacedLeavesOnceAndDeliversOnce`,
    `aSealedShadeIsOnTheRosterAtOnce`, `aFullRosterRefusesTheSeal`, `aFullHolderIsToldWhy` (with a cost refused before
    anything is called and paid once, once sealed) and `theHolderEndsWithoutBreaking`. D-S17:
    `aFamiliarDoesNotFadeWhileItsPersonIsAway` (logged out three days, the familiar unloaded and loaded again; two days
    played while switched off) and `aFamiliarFadesWhileItsPersonPlaysElsewhere` (20 blocks away, the Nether, then back
    beside it). D-02: `upkeepWarnsThenSuspendsAfterGraceAndResumes`, `aReturnAfterAbsenceGetsTheFullGrace`,
    `absentAndUnloadedTimeIsNeverOwed` (logged out, beyond 64 blocks, in the Nether, and with the absent-owner option
    on: nothing looked at; with no ticker in unit 1, "unloaded" is a gap between looks), `onlyTheHolderEnds`,
    `twoPlayersCannotBothSealOnePlace`, `noOneBindsAnotherPlayer` and `theConcordanceOffHoldsEveryClock` (an anchor
    broken while switched off releases nothing). The upkeep tests use test terms on the vanilla `#minecraft:wool` tag:
    the anchor itself has no upkeep.
  - `ConcordanceAuthorityGameTests` +3 (8): `onlyTheOwnersSideSealsAnAnchor`, `aStandInNeverSeals`,
    `aChangeOfOwnerMovesNoPact`.
  - `ConcordancePersistenceGameTests` +5 (15): `anAnchorsAgreementRoundTrips`, `aBoundWillReadsItsOlderSave` (an
    unreadable record kept as written), `aPactRecordRoundTrips`, `thePactHistoryKeepsEight`,
    `aSuspendedAnchorMigratesItsHold`.
  - `ConcordanceSharingGameTests` +3 (5): `aCopiedAnchorGivesNothingAndReleasesNothing`,
    `aLegacyAnchorClaimsItsPlaceOnce`, `endingTwiceReturnsOnce`.
  - `ConcordanceResourceGameTests` +1: `thePactRulesKeepTheirShape`.
  - Extended by the unit review's fixes, passed in the same run: `aReturnAfterAbsenceGetsTheFullGrace` (an absence before
    the holder's own suspension still counts), `anUnloadedShadeReplacedLeavesOnceAndDeliversOnce` (switched off, the
    old shade takes no roster room), `aKilledShadeAwayFromItsAnchorDropsWhereItFell` (with the block-drop rule off),
    `upkeepWarnsThenSuspendsAfterGraceAndResumes` (a look in the offering's tick), `onlyTheHolderEnds` (a party
    member's offering refused), `aStandInNeverSeals` (a stand-in refused a recall) and `thePactRulesKeepTheirShape`.
  - Extended, passed in the same run: `aSpiritKeepsToItsAgreement` (suspended on its Bound Will, the anchor's flag false),
    `workersKeepTheirModelsThroughASave` (the bond's `seen_played`, an older, an unreadable and a forged one; a shade's
    anchor, holder and load, G10), `blockEntitiesStampTheirVersion` (the anchor) and
    `everyRecordReadsItsSaveFromBeforeVersions` (a Bound Will with a pact and escrow; the pact history).
  - `TestPlayers` gains `survivor` (an ordinary survival player), `leave`, `again` (the same identity with its saved
    data and statistics), `played` and `play`. The familiar and upkeep tests rely on the test world's player files
    being writable, and two of them on a test server with a Nether (the repository's first player teleport into the
    Nether in a game test).
- Audits in `tools/check_mod_data.py`: `check_workers` (a worker that carries things leaves them once and is struck off
  its roster when killed; the familiar fades by play time, never game time, restarts its count while switched off and
  reads `seen_played` leniently); `check_pacts` (new: the seal outcomes and their words, the refusal order, the cost
  asked before anything is called and paid once recorded, the stand-in and the owner, the cargo rule, the rules'
  numbers, holds and departure reasons, the Bound Will's optional keys, no item in a holder block, the copy check and
  the legacy claim, the day's count on the record, ending, breaking while switched off, suspending by record, the upkeep
  look, the unit's test names; since the unit review's fixes, `recall`'s stand-in refusal, `deposit`'s holder check, the
  shade's drop and roster rules and the resumed and same-tick upkeep looks); `AUTHORITY_ROUTES` (the holder's whole
  sealing check and `presentNear`); `ENTRY_BY_ENTRY` (`PactHistory`).
- Audit mutations, run in place on the final tree with every file restored after each and the tree verified unchanged:
  the unit specification's 30 (M1 to M12, M14 to M31), 16 more for the review's corrections and 7 for the unit review's
  fixes. 52 of the 53 are reported; the unchanged tree reports nothing. Not reported by the audits: counting the
  familiar's fade only beyond its follow distance (as before Chunk D); `aFamiliarFadesWhileItsPersonPlaysElsewhere`'s
  last step catches it, when it runs.
- Commands run and passed before each of the five commits: `python3 tools/owner_art.py --check`,
  `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
  `python3 tools/concordance_journey.py --check`, `python3 tools/check_icon_maps.py`,
  `node --test tools/world-designer/model.test.cjs`, `python3 -m unittest discover -s scripts/tests` and
  `git diff --check`; the generators were run one at a time (`python3 -B tools/generate_material_data.py`) after each
  table change. For these records: every changed Java file parse-checked (24, no syntax error); the pure classes
  (`worker/Pacts`, `Agreement`, `Status`, `Bond`, `resource/BoundWill`, `BoundWillLedger`, `logistics/Place`) compiled
  with JDK 21's `javac` against the jspecify jar; two pure harnesses replaying the upkeep tests' traces and
  `thePactRulesKeepTheirShape`'s rules passed. `python3 tools/select_client_tests.py --base a8c7cd770` chooses every
  client class (239), as `TestPlayers.java` is a shared file.
- **Not run where it was written:** `./gradlew build` and the game tests (first run by CI, below); any client by hand,
  two clients, a real world saved before Chunk D, a restart.
- **CI since:** with the unit review's fixes and 26.3's white wool (`Items.WOOL.white()`), run 40067977680 on
  `da185dd0d` passed every job (the build, the server game tests with and without the optional integrations, every
  client class). Still not run: any client by hand, two clients, a real world saved before Chunk D.

**Concordance Chunk D, unit 4: D-08** (commits `023b681e8`, `e51ef2b11`, the records' commit "Concordance Chunk D:
unit 4's records and the schema review" and the commit "Chunk D unit 4: fixes from the unit review"; [the chunk's
contract](concordance-chunk-d.md); not merged). **CI:** compiled in runs 40079227985 and 40082852322, which failed its
twenty hand tests at their first step: a test helper compared `ItemStack.useOn`'s answer with the SUCCESS constant,
which 26.3 never returns for an item's use (fixed in the helper, `WorkshopHands.use`); run 40085012234 on `86c4e7127`
passed them (both server jobs), and every job, after one re-run of a client shard for main's
`PeepoPopulationClientTests` cow-breeding flake.
- New server game tests (21), passed in CI run 40085012234: `ConcordanceLogisticsGameTests` +13 (28; listed in
  [logistics](arcane-concordance-logistics.md#verification)); `ConcordanceWorkerGameTests` +5 (27):
  `aRequestOutsideTheHoursOrAreaIsRefusedWithItsReason` (narrow test terms; a claim held past its hours is finished;
  suspended with a load claimed, it finishes it and claims nothing new; the day's two loads, then the next day),
  `theHandNeedsStolasRegardMastered` (never met or understood, nothing is sealed, recorded, called or noted; mastered,
  its own agreement and the roster at once; an unbound anchor still seals a shade), `theConcordanceOffHoldsTheHand`,
  `aHandsServiceIsASpiritsService` (exactly a familiar's and a spirit's service recorded; a spirit's again records
  nothing; a construct's masters the Binding Arts) and `anAnchorIsBoundOnlyWhileItKeepsNoAgreement` (the owner's posts
  13 blocks above the anchor refused and 12 bound; sealed, neither unbound nor bound anew; a copy binds nothing);
  `ConcordanceAuthorityGameTests` +1 (9): `aStrangerCannotBindAHandToAnotherPlayersPost`;
  `ConcordancePersistenceGameTests` +2 (17): `aHandsClaimSurvivesARestart`, `aBoundAnchorKeepsItsPost`. A
  `WorkshopHands` fixture (not a test class) seals hands under the definition's or narrower test terms through
  `anchor.handSealing`.
- Client, passed in CI run 40085012234 (client shard 1): `CompanionAutomationChecks.workshopHand`, through
  `PeepoCompanionClientTests`.
- Audits in `tools/check_mod_data.py`: `check_workers`' class map gains the hand (its definition, entity, name, GeckoLib
  renderer, model, clips and sheet are then checked as the others'); `check_hand` (new): the hand a spirit whose service
  is a spirit's, the Regard at MASTERED asked before the seal, its decision's order, its terms, refusal and releases, no
  item list, the anchor's optional `post`, `call` and party sealing, the key's order and binding, the sheet against the
  generator, and the tests; since the unit review also the hand's carry and take-back refusals (D-P82), why it waits
  with its post gone and the anchor asking only the definition of the worker it serves (D-P83); `AUTHORITY_ROUTES` (the
  hand's `answering`, the key's `maySeal`). Its audit mutations and the commands run are in the contract's D-08 As
  built; all passed. **Not run where it was written:** `./gradlew build` and the game tests (run by CI, above); any
  client by hand, two clients.

**Concordance Chunk D, unit 2: D-03 to D-05** (commits `9daf23ae1`, `832129b95`, `7da686ad4`, `ee82c1dc7`, the
records' commit and the unit review's fixes; [Stolas's pact](stolas-pact.md) and the [contract](concordance-chunk-d.md)'s As built notes, proposals
D-P17 to D-P50; not merged). **CI:** first compiled in run 40090825848, which failed
`aBrokenPactDefinitionIsReported` in both server jobs (a defect of the test, fixed in `8a3425f9e`); every job passed in
runs 40094972598 and 40133540741. In this record's suites: `ConcordanceWorkerGameTests` +22 (D-04's 12 and D-05's 10, with unit 1's four
upkeep tests now also on Stolas's own pact; 49 in all with unit 4's five),
`ConcordanceAuthorityGameTests` +1 (`aPerchIsPlacedOnlyWhereItsPlacerMayBuild`; `aChangeOfOwnerMovesNoPact` extended
to the perch and Star-reckoning), `ConcordancePersistenceGameTests` +4, `ConcordanceSharingGameTests` +4 and
`ConcordanceResourceGameTests` +2; the client class `ConcordancePactClientGameTests`. Audits: `check_pacts` (D-04's and
D-05's parts), `check_pact_screen`, and `check_workers` covering `StolasEntity` (in `concordance/spirits/`: no chunk
loading, no dimension crossing, no brain memories). The local checks, the parse check, the pure harnesses and the 80
audit mutations are reported in the contract's D-05 As built note.

**Concordance Chunk D, unit 3: D-06 and D-07** (commits `9805a9a7c`, `773b85aba`, `a3ee00822`, `66b741868` and the
records' commit "Concordance Chunk D: unit 3's records", on the local `wip/chunk-d-u3` from `92868ff82`, merged at `554d71ab1`;
[Stolas's pact](stolas-pact.md) and the [contract](concordance-chunk-d.md)'s D-06 and D-07 As built notes, proposals
D-P90 to D-P121; not merged). **CI:** run 40120355940 on `554d71ab1` failed to compile at the roster client test
(fixed in `040d73ece`); run 40121823509 on `040d73ece` passed every job (`optional integrations absent` after one
re-run), these tests among them, and so did run 40133540741. In this record's suites:
`ConcordanceWorkerGameTests` +14 (D-06's seven, D-07's six and `aReplacedHandLoadingWhileSwitchedOffStaysOffTheRoster`;
63 in all), with unit 2's tests that read Stolas leaving now reading `gone()`; the client classes
`ConcordancePactClientGameTests` (extended) and `ConcordanceRosterClientGameTests` (new). Audits: `check_stolas_body`,
`check_pacts` (D-06's block and the hand's roster rule), `check_roster` and `check_roster_screen`. Run and passed
locally: `python3 tools/owner_art.py --check`, `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
`python3 tools/concordance_journey.py --check`, `python3 tools/check_icon_maps.py`,
`node --test tools/world-designer/model.test.cjs`, `python3 -m unittest discover -s scripts/tests` and
`git diff --check`; 104 audit mutations, each reported. **Not run where it was written:** `./gradlew build` and the
game tests (run by CI, above); any client by hand, two people.

**Concordance Chunk D, unit 5 (D-09)** ([the contract](concordance-chunk-d.md#unit-5-evidence-and-handoff-d-09); not
merged): `ConcordanceRosterClientGameTests` also photographs a workshop hand carrying a request's three copper ingots
across the yard, frozen with its cargo aboard on its own claim, then delivering them once
(`jugcraft_workshop_hand_at_work`); `ConcordancePactNetworkTests` has a second client JVM refused at another player's
perch and roster; `ConcordancePactHallLoadGameTests` measures four workshop hands at work among eight pacts. In run
40133540741 the roster class passed in shard 14 (the hand: 3 carried, aboard on its own claim, 0 left at the source,
then 3 delivered), the network class in shard 3 and in the opt-in job without the optional integrations, and the load
card passed (the four hands carried 60 loads, 3,840 glowstone, while measured). Two automated clients on one machine,
not two people.

## The Workshop Hand (Concordance Chunk D, D-08)

Not merged; built beside Chunk D's unit 2 ([the chunk's contract](concordance-chunk-d.md), D-08's As built note and
proposals D-P70 to D-P83). A spirit of the Gathering Shade's kind, bound to carry: sealed at a Spirit Anchor that a
Porter Key has bound to its holder's Courier Post, it carries that post's exact requests through the courier ledger with
the Clockwork Porter's own decision ([logistics](arcane-concordance-logistics.md)).

- **Tier:** Practitioner, once its holder has mastered Stolas's Regard (Chunk D unit 2's research
  `jugcraft:stolas_regard`, asked by the anchor before it seals, and only then: a hand already sealed serves on).
- **Input producer:** the requests of its holder and their party at the bound post, an Alembic Crucible's own formula
  shortage among them (C-06); the plain containers within its area.
- **Output consumer:** the post's nine slots, a crucible's buffer, any destination a request names inside its area.
- **Costs:** a Spirit Anchor, a Porter Key and a Courier Post; one Bound Will (of 32) and one roster place (of 32). No
  fuel, repair or upkeep. At most 32 loads a day of at most 64 items each, from 06:00 to 18:00, within 12 blocks of its
  anchor (`jugcraft:workshop_hand`, kind `spirit`, work `courier`); cargo stranded at its post beyond its carry, or
  bound back to a container beyond its area, it leaves to porters or its requester's one recovery (D-P82).
- **Unlocks:** nothing.
- **Binding Arts:** each delivery records a spirit's service (`worker_service`, key `spirit`) for its holder, present;
  mastering the Binding Arts still needs a familiar's, a spirit's and a construct's (distinct 3), so a hand is no
  shortcut and no fourth kind.
- **Binding and sealing:** a Porter Key set on a Courier Post, used on an anchor that keeps no agreement, binds it
  (sneaking unbinds it): only with the Concordance on, never at a copied anchor (`CONFLICTING_AGREEMENT`), only by the
  anchor's owner or their party where they may build (never a stand-in; an anchor placed before Chunk D, with no owner
  recorded: whoever may build there), only to a post they may use, loaded, in the anchor's level and within 12 blocks;
  the key's use there is rate-limited. An anchor bound to a post seals a hand (for its holder and their party, recorded
  on the Bound Will); an unbound one seals a shade. Reading an anchor that keeps a hand's agreement names its post, or
  says the post is gone (the hand itself then says `outside_agreement`). The anchor asks only the definition of the
  worker it serves, so a data pack that drops the shade's leaves hands' anchors working, and the other way round
  (D-P83).
- **Saves:** the hand's `anchor` and `task` (the claim's id, never cargo; stamp 1); the anchor's optional `post` (an
  older or unreadable one reads as none, the rest kept); the Bound Will's pact (`beneficiaries` `party`, `service` work
  `courier`), keys there since unit 1.
- **Art provenance:** the Gathering Shade's model and sheet (`tools/concordance_worker_models.py`,
  `tools/concordance_worker_art.py`), its boxes and regions unchanged, with a wicker pack on a bone of its own on the
  body and its lid on a bone of its own on the pack, hinged along its edge against the body (working, the lid lifts at
  the back; the pack itself stays on the body), and five clips; its eyes lit like the owner's
  `Guns/block/treated_brass_lamp_lit.png`, its pack from `farming and food textures/wooden_basket_side.png` with a
  buckle from `Guns/entity/cog_knight.png` (colours only, read from `art/owner-library/originals/Blocks/`; nothing from
  Mojang's files). Generated; not seen in game.
- **Test evidence:** passed in CI run 40085012234 on `86c4e7127` (see Verification). 21 server game tests (Logistics +13, Worker +5, Authority +1, Persistence +2)
  and the client companion check `CompanionAutomationChecks.workshopHand`, listed in Verification above.

**Failure behaviour** (03:193's eight cases, as C-06's table (CC:1127-1136), and the hand's own terms):

| Case | What the hand does | Test |
|---|---|---|
| The destination fills | Delivers what fits; the rest stays aboard, noted `full`, and it says `FULL`; with room again it is delivered once | `aHandFillsACrucibleUntilItIsFull` |
| Access removed | A source its holder may no longer open releases the claim reserved there (`blocked`) and is not reserved again; a post its holder may no longer use is served by none of its hands; restored, the request is served once | `aHandTakesNothingWhereItsHolderMayNot` |
| The path fails three times | It gives up its claim (`CANNOT_NAVIGATE`); the cargo is stranded at the post, counted, never dropped; its requester recovers it once | `aHandThatCannotFindItsWayStrandsItsCargoOnce` |
| The destination unloads | `DESTINATION_UNLOADED`: no ledger step, the lease renewed; cancelled, the cargo goes back | `anUnloadedDestinationKeepsTheHandWaiting` |
| Killed | Its claim let go (`worker_removed`), nothing dropped, struck off the roster, the agreement standing; its holder calls another at no cost, which takes the cargo up and delivers it once | `aKilledHandStrandsItsCargoOnce` |
| Its holder logs out | `OWNER_OFFLINE`, nothing renewed; the lease runs out and the cargo waits at the post; its holder back, it is taken up and delivered once | `anAbsentHolderStopsTheHand` |
| The source is broken | Its claim at the source let go (`source_gone`); after its rest it reserves at another chest and delivers once | `aBrokenSourceReleasesTheHandsClaim` |
| The server restarts | The hand's `anchor` and `task`, the anchor's `post`, the Bound Will and the ledger survive a save; unloaded, it lets nothing go; read back, it delivers once | `ConcordancePersistenceGameTests.aHandsClaimSurvivesARestart` |
| Suspended, past its hours, or its day's loads done | It finishes a load it has claimed and claims nothing new (`SUSPENDED`, `OUTSIDE_AGREEMENT`, `FINISHED`); resumed, or the next day, it serves again | `aRequestOutsideTheHoursOrAreaIsRefusedWithItsReason` |
| A request not its beneficiaries', or bound beyond its area | Skipped (`IDLE`), or refused (`OUTSIDE_AGREEMENT`), and left open for other couriers; it takes the requests behind it | `aHandServesOnlyItsBeneficiaries`, `aRequestOutsideTheHoursOrAreaIsRefusedWithItsReason` |
| Cargo stranded at the post beyond its carry (a porter's), or bound back to a container beyond its area | Skipped (`IDLE`), left to porters or its requester's one recovery; a stranded load within its carry is taken up past it; cargo it already holds whose source is beyond its area goes back into the post, never to that source (D-P82) | `aHandLeavesStrandedCargoBeyondItsCarry`, `aHandTakesCargoBackOnlyInsideItsArea` |
| Its post broken | No ledger step; the claim lapses by itself and the cargo stays in the ledger; it says `OUTSIDE_AGREEMENT`, and the anchor's reading says the post is gone (D-P83) | not tested in game (the audit holds the status) |
| Its agreement ended with cargo aboard | It lets its claim go (`agreement_ended`) and departs; the cargo is stranded at the post and recovered once; ending again moves nothing | `endingWithCargoAboardStrandsItOnce` |
| Two hands, one request | One claims it; the other may neither claim (`CLAIMED`), reserve nor pick up for it (`NOT_YOURS`) | `twoHandsCannotHoldOneRequest` |
| The Concordance switched off | `DISABLED`: its claim, cargo and lease untouched; on again within its lease, it delivers once | `theConcordanceOffHoldsTheHand` |

## The roster screen (Concordance Chunk D, D-07)

Not merged; built in Chunk D's unit 3 on its own local branch (commits `a3ee00822`, the server side and the journal's
pacts, and `66b741868`, the screen; [the chunk's contract](concordance-chunk-d.md), D-07's As built note and proposals
D-P105 to D-P121). One native, narrated screen for everything a player keeps: their familiars, spirits and constructs,
their pacts and agreements, and their courier requests.

- **Tier:** none of its own; it shows what the player already has, from their first familiar on.
- **Input producer:** the worker roster (`WorkerRoster`), the player's Bound Wills (`BoundWills`) and the courier ledger
  (`CourierLedger`), read again each time it is asked, from what is loaded only.
- **Output consumer:** the existing authorities only: `AgreementHolder.toggle` and `end`, `CourierLedger.cancel` and
  `recover`.
- **Costs and unlocks:** none; a key, "Concordance Roster" (`key.jugcraft.concordance_roster`), not bound at first
  (Controls; listed on the encyclopedia's Keys page), opening it from the game only (D-P112).
- **Rows** (D-P105): the reader's own Bound Wills first, in the order sealed (a pact without its place, a record kept at
  a place that keeps it no longer, or one not yet claimed by its place, included; each with its presence in the same
  row), then the courier requests they filed, oldest first, then the other workers on their roster. At most 64 rows;
  the rest are counted, and the journal and the `workers` and `logistics` commands list everything. Each row shows its
  portrait, kind, service, place and whether it is loaded (a worker not loaded: as it last reported), its current task,
  its next obligation and why it waits, each state in words beside a drawn mark of its own (12 marks, and one for a
  loaded or unloaded place), never colour alone (D-P119).
- **Ars Nouveau's helpers** are named by their kind (Drygmy, Whirlisprig, Wixie, Amethyst Golem, Alakarkinos): G13 is
  closed, for the `workers` command too (D-P113).
- **Actions and their authorities** (D-P106 to D-P110, D-P114): Suspend and Resume (offered by the record's hold, so a
  stale screen never toggles the wrong way) and End through `AgreementHolder`, by record id, so they work while the place
  is unloaded; End returns the escrow once, into the living reader's inventory, and the presence departs. Cancel and
  Recover through `CourierLedger`: only the requester; a request reserved or aboard offers Cancel only; Recover hands
  over only cargo the ledger holds stranded, never anything a container holds, loaded or not. Each request names a row
  the server listed to this reader and that row's revision; the server checks both again, with the holder or requester,
  before acting. A stand-in's request is dropped and a dead reader refused. Suspend, Resume and End share the Spirit
  Anchor's rate gate, Cancel and Recover the logistics commands'. Switched off, the rows are listed, none offers an
  action, and an action is answered "disabled".
- **Keep it first** (D-P117): End asks first through `KeepFirstConfirmScreen`, with the server's own words and the
  keyboard starting on Keep it; Esc keeps. The pact perch's End uses it too. Cancel and Recover do not ask.
- **The screen:** eight rows a page (Page Up and Page Down turn; the page scrolls within; D-P116); each row a focusable
  card with its actions under it; each answer, a refusal included, shown with a tick or a cross and read aloud; it asks
  again every 40 client ticks while open (D-P118), fits the smallest GUI (320×240, D-P120) and draws subtitles over
  itself.
- **Portraits** (D-P115): the worker's own creature, drawn still and never added to a level or ticked (at most 16, all
  forgotten on leaving a world); a request's item; for an unloaded spirit (a Gathering Shade's and a Workshop Hand's
  kind are both `spirit`) or anything that cannot be made, the Spirit Anchor's icon. The icon is drawn only when the
  creature cannot be made; whether GeckoLib draws in the GUI is shown only by the client test's shots (its `drawn`
  records the attempt, not pixels).
- **No second inventory:** the screen keeps nothing and decides nothing; its requests carry four fields (sequence,
  action, row id, revision), no player id, text or total. Roster sessions are memory only, forgotten when their reader
  leaves or the server stops; no saved data.
- **The journal** (D-P111): the Pacts section lists every pact and agreement held, with where it is kept, its next
  obligation and why it waits, and the last eight departures.
- **Test evidence (first run in CI run 40121823509 on `040d73ece`, where it passed, and again in run 40133540741):**
  server game tests `theRosterEndsOnlyWhatTheReaderHolds`,
  `aRequestNamingAnotherPlayersRecordIsRefused`, `theRosterListsAPactWithoutItsPerch`,
  `endingAnOrphanReturnsItsEscrowOnce`, `aStaleRosterRevisionIsRefused`, `theRosterStopsAt64Rows`
  (`ConcordanceWorkerGameTests`), `recoveryNeverTakesCargoOutOfAnUnloadedContainer`,
  `theRosterEndsAHandAndRecoversItsCargoOnce` (`ConcordanceLogisticsGameTests`) and
  `thePactsSectionListsPactsOnlyAfterTheBindingArts` (`ConcordanceJournalGameTests`); the client test
  `ConcordanceRosterClientGameTests` (ten shots) and the roster in `ScreenFitClientGameTests`. Audits `check_roster`
  and `check_roster_screen`, run and passing.

## World and event applicability

Works in every dimension; a shade's agreement and a porter's route belong to the dimension they were made in. No
seasonal content.

## Rollout and open questions

New registrations only: entities `jugcraft:hearthling`, `jugcraft:gathering_shade`, `jugcraft:clockwork_porter`; block
and item `jugcraft:spirit_anchor`; items `jugcraft:bonding_charm`, `jugcraft:clockwork_porter`, `jugcraft:porter_key`;
data component `jugcraft:porter_route`; saved data `jugcraft:worker_roster` and `jugcraft:bound_wills`; research
`jugcraft:binding_arts`; practice `jugcraft:worker_service`; item tag `jugcraft:binding_specimens`; data folder
`concordance/worker`. Removing them needs a migration.

Since Concordance Chunk D, unit 1 (not merged): saved data `jugcraft:pact_history`; the worker status ids `upkeep_due`
and `upkeep_held` (appended); the seal outcomes (`SEAL_OUTCOMES`), the holds (`none`, `holder`, `upkeep`) and the
departure reasons (`ended`, `broken`), each saved or worded, so append only; optional saved keys (the anchor's `owner`,
the bond's `seen_played`, a Bound Will's `pact` and `escrow`). No new block, item or entity.

Since Concordance Chunk D, unit 4 (not merged): the entity `jugcraft:workshop_hand`, its definition
`concordance/worker/workshop_hand.json` and the work `courier` (saved in Bound Wills' service terms, so append only);
the codex entry `binding/workshop_hand`; optional saved keys (the anchor's `post`, the hand's `anchor` and `task`). No
new block or item.

Since Concordance Chunk D, unit 2 (not merged): entity `jugcraft:stolas`; block, item, block entity and menu
`jugcraft:pact_perch`; the roster kind `pact`; the seat outcomes (`SEAT_OUTCOMES`, worded, so append only); the perch's
saved `owner`, `agreement` and `spirit` and Stolas's `perch` and `record` (stamps 1). [Stolas's pact](stolas-pact.md)
lists the rest.

Since Concordance Chunk D, unit 3 (not merged): the payload types `jugcraft:concordance_roster_request` and
`jugcraft:concordance_roster_state` (the roster's actions and icons travel as indices, so append only), the key
`key.jugcraft.concordance_roster`, Stolas's sound events `jugcraft:stolas.*` and Jade's providers `jugcraft:pact_perch`
and `jugcraft:spirit_anchor`. No saved data, block, item or entity.

Open items:

- A spirit agrees to one kind of work (gathering, or since Chunk D's unit 4 a courier's); more kinds each need their own
  server behaviour.
- An agreement's terms come from the definition; letting a holder choose narrower hours or a smaller area needs a
  terms control added to the roster (D-07) or more anchor controls.
- A familiar waits rather than following through a portal, and the charm recalls it only within the same dimension.
- Ending an agreement without breaking its anchor (`AgreementHolder.end`): since unit 2 the pact perch's screen ends
  Stolas's pact, and a pact without its perch is ended from any perch keeping nothing (or from the roster, D-07); since
  unit 3 the roster ends any agreement or pact its reader holds, a shade's Bound Will left without its place included.
  Its tests passed in CI (runs 40121823509 and 40133540741); not played by a person.
- Workers have not walked real terrain in a test: navigation failure is tested in the harness, not in game.
