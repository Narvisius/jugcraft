# Arcane Concordance: spirits, familiars and constructs

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
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
every ten seconds you are near it, at most 24 a day) and fades by 4 for each day you are online but away from it. At 20
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
  last mending and the last visit. Growth and fading are worked out from game time, so turning the world clock changes
  nothing.
- **Agreement** (`worker/Agreement.java`, on the Spirit Anchor, the one authoritative copy): its Bound Will record's id,
  its holder, the work, dimension, centre, radius, hours, quota, the day's tasks and whether it is suspended.
  `permits` names the reason a task is refused: suspended, outside the agreement (work, dimension, area or hours) or
  finished (the quota). The Bound Will itself lives in the world's `BoundWills` ledger (one record a sealed agreement,
  never merged or split).
- **Body** (`worker/Body.java`, on the porter): integrity and Ley Charge, with the trips, repairs and charging its
  definition allows. `ready` names the reason it cannot go: needs repair or no energy.

**Decisions** (`spirits/`): every second each worker `think`s once on the server and leaves one `Status`
(`worker/Status.java`), an intent or a reason. The roster (`WorkerRoster`, world data) records each worker's kind, last
status, dimension and place when its status changes, so the `workers` command answers for unloaded workers without
loading them. A familiar its person released while it was unloaded strikes itself off when it next loads.

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
- The bond needs days: at most 24 a day, 20 to start mending and 60 for the faster mending, fading by 4 a day away.
- Roster and Bound Will ledgers keep at most 32 entries a player; brains run two light behaviours and one sensor.

## Multiplayer and persistence

- Server authority: binding, sealing, routing, repair and every decision are the server's; the client draws the synced
  status. Uses of the charm and the anchor are rate-limited.
- Permissions: only a familiar's person commands it; only a shade's holder suspends it (anyone may read its terms;
  breaking the anchor needs the right to break it); only a porter's keeper's party routes or repairs it.
- Persistence: the bond, the porter's body, route and load, the shade's anchor and load and each worker's person or
  keeper on the entity; the agreement, the day's tasks and the delivered items on the anchor; Bound Will records
  (`jugcraft:bound_wills`) and the roster (`jugcraft:worker_roster`) in the world's data; a key's route in its
  `jugcraft:porter_route` component.
- PvP: no worker attacks anyone.
- Disable behaviour: with `concordance.enabled=false` every worker stops and says it is switched off; nothing is
  unregistered or lost.

## Dependencies and assets

No new dependency. Framework use:

- **SmartBrainLib**: the workers' brains (a players sensor, look and walk behaviours, the walk-target and can't-reach
  memories). Decisions stay in Jugcraft's own models; the brain only moves the body.
- **GeckoLib**: three original entity models with their animations, one looping clip per status group.
- **Jade (optional)**: worker status, bond, load, integrity and charge.
- **Shared effect system**: the familiar's mending goes through `ConcordanceEffects` like any spell, so protections,
  stacking and attribution apply once.
- **Modonomicon**: a new **Binding** codex category (the Binding Arts, familiars, spirits, constructs, and every
  reason a worker can give).
- **GuiLib**: not used. The management controls are the charm, the anchor, the key, an empty hand on a worker and the
  `workers` command; an optional management screen is an open item.

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

## World and event applicability

Works in every dimension; a shade's agreement and a porter's route belong to the dimension they were made in. No
seasonal content.

## Rollout and open questions

New registrations only: entities `jugcraft:hearthling`, `jugcraft:gathering_shade`, `jugcraft:clockwork_porter`; block
and item `jugcraft:spirit_anchor`; items `jugcraft:bonding_charm`, `jugcraft:clockwork_porter`, `jugcraft:porter_key`;
data component `jugcraft:porter_route`; saved data `jugcraft:worker_roster` and `jugcraft:bound_wills`; research
`jugcraft:binding_arts`; practice `jugcraft:worker_service`; item tag `jugcraft:binding_specimens`; data folder
`concordance/worker`. Removing them needs a migration.

Open items:

- A spirit agrees to one kind of work (gathering); more kinds each need their own server behaviour.
- An agreement's terms come from the definition; letting a holder choose narrower hours or a smaller area needs a
  management screen (GuiLib, step 26) or more anchor controls.
- A familiar waits rather than following through a portal, and the charm recalls it only within the same dimension.
- A shade whose holder is offline still keeps its agreement, but can then check only world bounds and town protection
  for the items it takes.
- Workers have not walked real terrain in a test: navigation failure is tested in the harness, not in game.
