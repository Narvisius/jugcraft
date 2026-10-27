# Arcane Concordance: ritual structures and execution states

Status: merged to main ([#244](https://github.com/jimbozoomer-byte/jugcraft/pull/244), from branch
`claude/awesome-davinci-iwv3b9`, integrated by [#263](https://github.com/jimbozoomer-byte/jugcraft/pull/263) on
8 October 2026); see Verification for what has run. Concordance Chunk C's unit 2 (ritual readiness, fault positions,
spent and held; Root and Rivet) is on `claude/chunk-c-living-workshop`, not merged ([contract](concordance-chunk-c.md));
its tests passed in CI (run 38693262154, and again in run 38774859294).
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 12) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 5, Practitioner stage (Jugcraft Discovery). Roadmap step 12.
Primary specialty and supported player role: Circlewrights (the Tether Principle). A ritual is slow, built, and
worked in one place; it does what a cast spell cannot: changes an item permanently, or blesses a party for minutes.

Builds on [the shared effect system and composition](arcane-concordance-composition.md),
[typed resources](arcane-concordance-sharing.md) and [the baselines](arcane-concordance-baselines.md). Contract and
checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

**Circle Lore** (new research; needs First Light understood). Examine a compass, a lead, calcite or chiseled stone
bricks to encounter it; examine two of them to observe it; study one at a Lampwright's Bench, or read someone's
Research Notes, to understand it. Understanding it teaches its rituals: the Adept's Attunement, the Lumen Vigil, Root
and Rivet, and the Kindling that finishes a Concord Spire's Crown. Completing rituals in **three different chunks**
masters it.

**The Lesser Circle.** A ritual is worked at a **Circle Anchor** with a circle built round it on the same level:

- four **Ley Pylons**, two blocks from the anchor on each side (the channels);
- eight **Warding Stones** at the corners and three blocks out (the boundary);
- the two blocks above the anchor left open (the clearance).

With nothing offered, use the anchor with an empty hand to hear what is wrong: a part **missing**, the **wrong block**, a space **obstructed**,
a pylon **without Ley Charge**, a pylon that **belongs to someone else**, or a part **out of reach** (in an unloaded
chunk). Each fault says where it is and where that is from the anchor: "x, y, z (dx, dy, dz from the anchor)".
`/jugcraft concordance circle <pos>` gives the same report for an anchor within 16 blocks. Since Concordance Chunk C
(C-03), both readings also **mark every faulted part where it stands**, at most once a second a player, with the sign
the interruption those faults would cause shows: danger at every faulted part while the boundary is broken, a shortage
sign otherwise. While a rite runs, or by the command with offerings in the anchor (an empty-hand use would begin the
rite), each pylon short of a step's charge or not lent to the reader is marked with a shortage sign too. A refused start
marks the parts that refused it the same way.

Looking at the anchor, the encyclopedia's **Inspect** page (Chunk C, C-03) lists everything the rite the offerings
answer still needs before it can begin, all at once, for the reader as its leader: each offering short; each pylon's
charge against one step and against the whole rite; pylons that will not lend; whether the reader stands near enough;
their Focus; the light; and how many faults the circle has (the status lines above list each). A pylon short only of the
whole run is a warning, not a refusal: the start still asks one step's charge (C-P10), and the line names the step the
rite would break off at unless the pylon is refilled meanwhile (on JE a pylon gains about 2.56 Ley Charge a step, more
than any rite draws from it). While a rite runs, the page shows what it has spent (Focus, Ley Charge, steps drawn) apart
from the offerings it still holds; after an interruption, how that attempt ended, until the next start or a load. Then
the rite's stages and what each pays, worded from its data, the backlash included. A rite the reader has not learned is
never named there: the page says only that the offerings answer a rite they have not learned.

With Jade, looking at the anchor shows its phase, its step, the participants a running rite needs ("Gathering: 1 of 2")
and how many are channelling, the first three offerings in it (and "+n" for the rest), while a rite holds it what it has
spent and how many offerings it holds, and how many faults the circle has with the first three (as "x,y,z"). Jade never
names the rite. Looking at a pylon shows its charge.

**Ley Pylons** hold up to 64 Ley Charge. Two ways in, neither of which comes back out:

- pour a Kindled Lantern into one (use it on the pylon): Radiance turns into Ley Charge through the data conversion
  `jugcraft:radiance_to_ley`, 3 Radiance for 2 Ley Charge, in whole batches, at most 15 Radiance a pour;
- feed it Jugcraft Energy through any cable or machine side: 1000 JE for one Ley Charge, up to 64 JE a tick.

A pylon belongs to the player who placed it; a ritual draws only from pylons owned by a participant (or by no one).
Broken, a pylon keeps its charge on the item.

**Working a ritual.** Put the offerings in the anchor (use it with each item). Then use it with an empty hand: the anchor
finds the ritual the offerings answer, checks the circle, and takes the leader's Focus. A ritual for more than one waits
30 seconds for the others, who join the same way and pay their own Focus. Every two seconds the ritual checks the whole
circle again and draws its Ley Charge from every pylon. The participants hold a channelling gesture and motes run along
the channels the server last validated. The leader can call it off by sneaking with an empty hand.

| Ritual | Participants | Focus each | Steps (2 s) | Ley Charge a step, from each pylon | Light | Offerings | Result | Backlash |
|---|---|---|---|---|---|---|---|---|
| Adept's Attunement | 1 | 6 | 5 | 2 | any | an Initiate's Wand, 4 amethyst shards, 2 gold ingots, 4 glowstone dust | the wand becomes an **Adept's Wand**, keeping its inscription and tunings | 4 |
| Lumen Vigil | 2 | 4 | 3 | 1 | 7 or darker | 4 glow berries | every participant: protection absorbing 8 damage for 2 minutes; up to 16 creatures within 16 blocks glow for 1 minute | 2 |
| Root and Rivet | 1 | 6 | 4 | 2 | any | a Thallite plate, 4 copper ingots, 4 Mendvetch | the plate becomes a **Rooted Thallite Foundation** | 4 |

The **Adept's Wand** is the second instrument: a capacity of 12 (the Initiate's Wand has 8), 8 targets (6), 72 work
(48), two branches (one) and twice the duration. It is how a player reaches spells too large for the first wand.

**Root and Rivet** (Concordance Chunk C, C-04; the owner's choice C-D2, with C-P11's numbers as starting values) is the
first workshop rite, at the same Practitioner stage and taught by the same Circle Lore understood. A Thallite plate,
riveted with copper and bound with Mendvetch, becomes a **Rooted Thallite Foundation**: the base of the
[Crucible Stand](crucible-stand.md) (C-05, unit 3; not merged), its one consumer. Nothing turns the foundation back into
a plate, Ley Charge or Focus, and the Assayer's Scale never forms it: it is no tablet output. Since the merge with main
`f904df3fd`, whose Scale values every default item, the foundation can be weighed and dissolved at its catalogued worth.
The plate comes from a Metal Press or, by hand, from two thallite ingots; every route to it needs the server's
`machines` and `thallite` feature switches. With either off the rite stays known, but its plate cannot be made (its
codex entry says so). Completing it lays a Stratawright's `foundation` for each participant present, as any completed
ritual does. Its stages and what each pays, as its codex entry and the Inspect page state them:

| Stage | When | Paid at this stage | Still held | Spent so far | If broken off |
|---|---|---|---|---|---|
| Offer | the anchor idle | — | nothing: a sneaking empty-hand use takes the offerings back | — | — |
| Begin | the leader's empty-hand use | 6 Focus | the plate, 4 copper ingots and 4 Mendvetch, locked in the anchor | 6 Focus | the offerings kept and unlocked; the Focus lost |
| Step k of 4 | every 40 ticks (8 s in all) | 2 Ley Charge from each of the 4 pylons (8 a step) | the offerings | 6 Focus and k × 8 Ley Charge | the offerings kept; the Focus and Ley Charge spent lost; a lost Warding Stone also deals each participant 4 backlash |
| Commit | at step 4 | the offerings consumed and the foundation made, in one tick | — | 6 Focus, 32 Ley Charge and the offerings | — |
| Complete | until a participant takes it | — | the foundation, in the anchor | — | the anchor broken drops it |

**How a ritual ends.**

- **Completed**: the offerings are used and the result is made, both in the same server tick, once. A transformed item
  waits in the anchor for a participant to take it.
- **Broken off**: nothing is made; the offerings stay in the anchor, unlocked, to be taken back or tried again. Focus and
  Ley Charge already spent stay spent: they are what an attempt costs.
- **Broken off at the boundary** (a Warding Stone removed or replaced while it runs): the same, and the working lashes out,
  dealing its backlash as damage to every participant present.
- **The anchor broken**: the offerings drop where it stood, once.

Since Concordance Chunk C (C-03) each way a ritual breaks off is told to its participants in **one line**: why it broke
off, where the offerings are, and the Focus and Ley Charge the attempt spent and lost, for example "Adept's Attunement
broke off: a pylon ran dry. The offerings are still in the anchor; the attempt lost 6 Focus and 8 Ley Charge". The Focus
is the whole attempt's, every participant's together. Before, the reason and "the offerings are still in the anchor"
went to the action bar in the same tick, so the second probably replaced the first.

## How it works

**Data** (`data/<ns>/concordance/structure/` and `/ritual/`, read by `StructurePattern` and `RitualDefinition` in
`concordance/ritual`). A structure names its anchor block and its parts: each a role (`channel`, `boundary`,
`clearance`), the block or block tag it needs (none for clearance) and its offsets from the anchor (at most 64 parts,
16 channels, reach 8). A ritual names its structure, the research and state that teach it, its participants (at most
4), Focus, steps (at most 30), Ley Charge a step, optional conditions (`max_light`), up to six offerings (an item or a
tag, and a count), its result (`transform` one offered item into another, or `effects` through the shared effect
system on the participants or on creatures in a radius) and its backlash. The loader refuses a ritual whose structure,
research or items are unknown, or whose numbers are outside the limits, with a reason (`/jugcraft concordance
diagnose`).

**The validator** (`StructureValidator.check`) visits every part once and reports each fault: `MISSING`,
`INCOMPATIBLE`, `OBSTRUCTED`, `UNPOWERED`, `FOREIGN` (another player's pylon) and `UNLOADED`. It reads only loaded
chunks, so a check never loads one. A running ritual checks at every step (40 ticks) and nowhere else; an idle anchor
answers queries from a cached report, reused for 100 ticks unless a circle part within reach of it is placed or broken
(`Rituals.changed`: anchors are indexed as their block entities load, so nothing scans the world). The client is sent
the channels the last check linked, and draws motes only along those.

**The state machine** (`RitualMachine`, `RitualRun`): `IDLE → GATHERING → CHANNELING → COMPLETE → IDLE`, and any
interruption returns to IDLE. `RitualRun` is an immutable state; every event (start, join, tick, interrupt, take)
returns the next state and the ordered actions the anchor carries out: `Pay` (Focus), `Reserve` (lock the slots),
`Draw` (Ley Charge, all channels or none), `Consume`, `Commit`, `Release` (keep or drop) and `Backlash`. The anchor
applies them in one server tick (`CircleAnchorBlockEntity.apply`).

**When offerings are reserved, consumed and committed.** Reserved when the leader starts: the slots lock and the anchor
exposes no container, so no hopper, pipe or player can add or take. Consumed and committed together at the last step,
in one tick, and nowhere else; before consuming, the anchor checks the slots still hold exactly the planned items
(`TAMPERED` otherwise). The plan itself is deterministic: specific items before tags, lowest slot first.

**Every interruption point has one defined outcome** (`RitualMachine.Interruption`):

| Interruption | When | Offerings | Result | Extra |
|---|---|---|---|---|
| `STRUCTURE` | a channel or clearance missing, wrong, obstructed or foreign at a step | kept | none | |
| `CONTAINMENT` | a boundary part missing or wrong at a step | kept | none | backlash to participants present |
| `POWER` | a pylon holds less than the step draws | kept | none | nothing drawn that step |
| `PARTICIPANTS` | a participant disconnected, died, moved out of range or to another dimension, or not all joined within 30 s | kept | none | |
| `CONDITIONS` | the light above the anchor rose above the limit | kept | none | |
| `UNLOADED` | part of the circle is in an unloaded chunk | kept | none | no backlash risked on a guess |
| `LAPSED` | the anchor was saved mid-run (chunk unloaded, server stopped or crashed) or missed a whole step | kept | none | found when it loads; it never catches up |
| `CANCELLED` | the leader sneaks with an empty hand | kept | none | |
| `REMOVED` | the anchor is broken or removed | dropped once | none | |
| `DISABLED` | `concordance.enabled=false` | kept | none | |
| `FORGOTTEN` | a data pack no longer defines the ritual | kept | none | |
| `TAMPERED` | the reserved slots changed (a guard: the lock should make this impossible) | kept | none | |

When several faults hold at once the first of `UNLOADED`, `CONTAINMENT`, `STRUCTURE`, `POWER`, `PARTICIPANTS`,
`CONDITIONS` is given. Everything a ritual holds is saved with the anchor, in the anchor's own chunk, so a save or a
crash can never keep both the result and the offerings. Since Concordance Chunk C (C-03) each is told in one line
(`message.jugcraft.concordance.circle.ended`, or `.ended.spent` with the Focus and Ley Charge lost when the rules still
know the rite); `circle.kept` gave way to `circle.release.kept` and `.release.dropped`. `TAMPERED`'s line under-counts
its last step's Ley Charge: that step is drawn before the failed consume, and the run's count of steps does not include
it.

**Readiness** (Concordance Chunk C, C-03; pure, in `concordance/ritual`):
- `Choice.of` chooses the rite an anchor's offerings answer for one player: the known rite fully matched with the most
  items offered (ties to the first by id); failing that, the first fully matched rite the player has not learned (a
  start says so, as before; readiness never names it); failing both, the known rite fewest items short, with every
  shortfall. Nothing offered chooses nothing (`Choice.NONE`). The anchor's `choose` is the one call a leader's start and
  readiness share, and `check_rituals` holds that `begin` uses it.
- `Readiness.before` lists every shortfall before a start in the shared diagnostic vocabulary (`missing_ingredient`,
  `not_lent`, `unavailable_power` for one step or, as a warning that never refuses, for the whole run, `too_far`,
  `needs_focus`, `too_bright`, `incomplete_pattern`), each marked as refusing a start or not. `Readiness.running` gives
  what a rite holding the anchor has spent (`Spent`: each participant's Focus, each channel's Ley Charge for every step
  drawn, and what is to come) apart from what it holds (`Held`), with what may stop it at its next step: a channel short
  or unlent, the light above its limit, and, while it gathers, who has still to join. `Readiness.stages` words the stage
  and payment table from the rite's data alone. `shapeFaults` keeps the report's own shape faults and leaves charge and
  ownership to one reading of each channel.
- The anchor's `readiness` builds it from the cached report (no second check), one reading of each channel (at most 16),
  the reader's Focus and place, and the light above the anchor. It, `choose`, `spent` and the status lines the Inspect
  page and Jade read change nothing and show no sign (`check_rituals` and `check_encyclopedia` hold this).
- An interruption keeps its `Ending` (the reason, and what the attempt spent) in memory for the Inspect page until the
  next start or a load; it is never saved. `endingLine` words it.
- Signs: `markReadiness` for a reading in chat (an empty-hand use that neither begins, joins, calls off nor takes, and
  the command), gated by `RateGate` every `MARK_TICKS` (20) ticks a player, and `markFaults` for a refused start and an
  interruption.

**Effects** go through the shared executor (`ConcordanceEffects`) with the cause `RITUAL`, under a ledger sized to the
ritual's targets, so protection, friendly fire, spawn protection and tolerance apply as for any spell. Backlash is
`DAMAGE` through the same boundary.

**Practice.** A completed ritual records practice (`Evidence.Practiced("jugcraft:ritual", <chunk>)`) for each
participant present: one per chunk counts towards mastering Circle Lore. Practice before Circle Lore is encountered is
kept, as notes are, and counts once it is.

## Connections

- Input producer: First Light (Circle Lore needs it understood); Kindled Lanterns (Radiance) or any Jugcraft Energy
  generator (Ley Charge); Focus; vanilla offerings (amethyst, gold, glowstone, glow berries). Since Concordance Chunk C
  (C-04), Root and Rivet's: a Thallite plate (a Metal Press, or two thallite ingots by hand; the `machines` and
  `thallite` switches), copper ingots and Mendvetch (a Verdant Bed crop, or crafted).
- Output consumer: the Adept's Wand (larger composed spells, step 8); party protection and creature detection. Since
  Chunk C, the Rooted Thallite Foundation, whose consumer is the [Crucible Stand](crucible-stand.md)'s recipe (C-05,
  unit 3; the foundation's tooltip and codex page name it). Every completed
  ritual, Root and Rivet among them, is also Stratawrighting's `foundation` elective for each participant present
  ([Rime and Strata](arcane-concordance-rime-strata.md)); Root and Rivet's codex entry is linked from Stratawrighting's
  Understood page.
- Technology connection: Ley Pylons accept Jugcraft Energy through the shared energy interface (an alternative to
  Radiance, never a way back to energy).
- Magic connection: the shared grammar's instrument limits, the shared effect system, research and practice evidence,
  the typed-resource conversion `radiance_to_ley`.
- Reachable entry path: First Light understood → Circle Lore by examining and studying vanilla items → craft anchor,
  pylons and stones from vanilla materials → charge pylons from a Kindled Lantern (First Light) or energy.
- Solo, trade and cooperative routes: Adept's Attunement and Root and Rivet are solo; Lumen Vigil needs two players
  (the live progression graph holds Root and Rivet reachable by one player alone). Research Notes can be
  traded to understand Circle Lore. No ritual is the only way to anything a player needs for core progression.
- Specialty use without other branches: needs only First Light (and either Radiance or energy for the pylons).

## Balance

Units: Focus (the caster's pool, 20 on an Initiate), Ley Charge (whole units), Radiance (whole units), Jugcraft Energy
(JE).

- A Lesser Circle has four pylons, so Adept's Attunement draws 4 × 2 × 5 = **40 Ley Charge** (60 Radiance, or 40,000 JE)
  over 10 seconds; Lumen Vigil draws 4 × 1 × 3 = **12 Ley Charge** (18 Radiance, or 12,000 JE) over 6 seconds. Root and
  Rivet (Concordance Chunk C, C-04) draws 4 × 2 × 4 = **32 Ley Charge** (48 Radiance, or as much Verdance poured into
  the pylons, or 32,000 JE) over 8 seconds, and takes 6 Focus: C-P11's starting values, by arithmetic, not measured in a
  game.
- Since Chunk C (C-03), `check_rituals` holds every rite's steps × Ley Charge a step to at most a full pylon's 64, so a
  full circle can pay a whole run. A start still asks each pylon for one step only (C-P10): a pylon fed by energy as it
  runs (64 JE a tick, about 2.56 Ley Charge a 40-tick step by arithmetic) can sustain a run it could not hold at once.
- The economy record models Root and Rivet as a sink: 32 Ley Charge and 6 Focus a run into a foundation that nothing
  turns back ([economy](arcane-concordance-economy.md)).
- `radiance_to_ley` always loses a third (3 → 2) and nothing turns Ley Charge back into Radiance or energy, so there is no
  positive loop.
- Construction: Circle Anchor (amethyst, two gold, compass, three chiseled stone bricks), Ley Pylon ×4 (each amethyst,
  two copper, lapis, three stone bricks), Warding Stone ×8 (crafted four at a time from stone bricks, amethyst and
  calcite).
- A failed attempt costs its Focus and the Ley Charge drawn so far, never the offerings (except when the anchor itself is
  broken, when they drop on the ground).

## Multiplayer and persistence

- Server authority: the anchor decides everything; clients receive only the phase, the participants, the step and the
  linked channel mask (not the items). Every player action is rate-limited (`RateGate`) and checks the feature switch,
  research, Focus and distance (within the structure's reach plus 4 blocks horizontally, 3 vertically).
- Persistence: the anchor saves its slots, output and the whole run (phase, ritual, plan, participants, step, timings);
  a run loaded mid-way lapses. Pylons save their charge and owner. New item component `jugcraft:ley_charge` on a broken
  pylon.
- Disconnects: a participant who leaves breaks the ritual off at the next step (`PARTICIPANTS`); offerings stay.
- Disable behaviour: with `concordance.enabled=false`, nothing can start and a running ritual breaks off (`DISABLED`);
  everything stays registered.
- Readiness, marks and endings (Concordance Chunk C, C-03): no new saved key. How the last attempt ended lives in
  memory only and is forgotten on a load. Readiness judges its reader as the rite's leader, as a start does, so a
  friend's pylon reads "not lent" until the friend joins. The signs reach every player tracking the part, within the
  Signs budget; a reading marks at most once a second a player, and an operator's command can mark any loaded circle it
  reads. A rite the reader has not learned is never named on the Inspect page or in Jade.

## Dependencies and assets

No new dependency. Framework use:

- **GeckoLib**: the Circle Anchor is a GeckoLib block entity with idle and channel animations chosen from the synced
  phase (original model and texture by `tools/concordance_ritual_art.py`).
- **Player Animation Library**: participants hold the `circle_channel` gesture on a Jugcraft layer below Spell Engine's
  cast gestures.
- **Fusion** (optional): a built-in resource pack `jugcraft:fusion_textures`, registered only when Fusion is installed,
  joins neighbouring Warding Stones with a connected texture; without Fusion they are plain cubes.
- **Jade** (optional): anchor phase, step, fault count and the first three faults; pylon charge. Since Concordance
  Chunk C (C-03), also the participants a running rite needs (the gathering line said "?"), how many are channelling,
  the first three offerings and "+n", and while a rite holds the anchor the Focus and Ley Charge spent and the offerings
  held (`ConcordanceDataProvider.SHOWN_OFFERINGS`); never the rite's name, anyone's Focus held or research.
- **JEI** (optional): a Circle Anchor category showing Adept's Attunement and, since Chunk C, Root and Rivet (their
  offerings and results), with the anchor as its catalyst; and information pages on where the Adept's Wand and the
  Rooted Thallite Foundation come from (`tools/acquisition.py`).
- **Modonomicon**: a new **Circles** codex category (Circle Lore, the Lesser Circle, the Ley Pylon, Working a Ritual, and
  one entry per ritual), generated from the same tables as the data. Since Chunk C (C-04), the Root and Rivet entry
  (`circles/root_and_rivet`): the rite, its stages and payment, the Thallite plate's recipe with the switches it needs,
  and the foundation. Stratawrighting's Understood page links it: the codex's first `entry://` link, which
  `check_concordance` holds, with every other such link, to an entry on disk (Modonomicon will not open a book with a
  broken link, as read from its source; the link has not been clicked in a client).
- **Not built: the GuiLib schematic.** The circle report (anchor, command and Jade) lists every part that is wrong and
  where; a GuiLib schematic screen remains optional future work. Since Chunk C (C-03), the encyclopedia's Inspect page
  stands in for it, with readiness and the stage table, and readings mark the faulted parts in the world.

Sounds `jugcraft:concordance.circle_start`, `.circle_step`, `.circle_complete`, `.circle_break` are synthesised by
`tools/concordance_sounds.py`; all textures and models are original.

## Verification

- `python3 tools/check_mod_data.py` passes. New step 12 checks (`check_rituals`): every structure and ritual parses
  with the Java limits; offsets are within reach and unique; the anchor block, boundary tag, pylon and offerings exist;
  each ritual's research unlocks it at the stated state; the Java constants equal the generator's; every message key the
  ritual code names exists; codex entries, advancements, models, GeckoLib assets and the Fusion pack exist.
- `python3 scripts/check_repository.py` passes.
- The pure core compiles with JDK 21. The step 12 harness passes **98 checks** against the generated data: the
  structure validator (each fault, the obstructed clearance, a foreign pylon, an unloaded part); offering plans
  (specific before tag, lowest slot first, shortfalls); the phase graph; fault priority; and **every interruption at
  every phase** (gathering, each channeling step, complete), each releasing the offerings once, never committing, and
  dealing backlash only for containment; the commit consuming exactly the plan once; practice per chunk; and the loader
  refusing bad structures and rituals. A mutation script made 12 deliberate faults in data and code (steps 12 and 13);
  the checker or harness caught all 12. That harness is not in Git: `src/test` holds only `VanillaReferences.java`.
- Game tests: `ConcordanceRitualGameTests` (thirteen when this step was built, and eighteen before Concordance Chunk C
  with the tests later steps added): the circle checked part by part; the attunement completing once
  (offerings consumed, wand transformed keeping its inscription, Focus and Ley Charge spent, practice recorded); a broken
  channel releasing the offerings; a lost boundary dealing backlash; a dry pylon; a participant leaving; the leader
  calling it off; a broken anchor dropping its offerings once; a ritual saved mid-run lapsing on load; the Vigil
  gathering two and shielding them; the Vigil refused in the light; a pylon filling from a lantern and from energy and
  keeping its charge when broken; refusals taking nothing.
- CI, run 37567742360 (commit `e17a6efb`, which also carries step 13): `mod` passed with **"All 881 required tests
  passed"**, the thirteen ritual tests included, and `optional integrations absent` passed. Earlier, run 37552117833 (commit `3d3f3634`) passed 873 of 874: the
  pylon's drop check counted item entities after the test helper's `destroyBlock`, which drops no loot; it now reads
  the block's drops with `Block.getDrops`. Runs before that found
  two compile errors against 26.3 (`PushReaction.IMMOVEABLE`; `Vec3.atCenterOf` for a block's centre). The client test
  shards fail before any test starts, with the OpenGL startup crash the framework foundation branch also has.

**After `main` was merged in (7 October 2026),** run 37582029152 on commit `862e2c37` passed every job: all 941 server
game tests, the build without optional integrations, and for the first time on this branch all 91 client game test
classes. They had crashed because the framework foundation put Iris on the development client, which CI's renderer
could not start; a workaround on this branch let them run, and `main`'s own fix (#239: an OpenGL context through SDL's
EGL in CI) has since replaced it, so this branch uses `main`'s build unchanged. The client tests load
the Concordance's client code, but none of them looks at a Concordance block, item or screen yet.

Not yet run: any client (the anchor's GeckoLib animation, the participants' gesture, the channel motes, Fusion's
connected texture and the codex pages in game), a two-client dedicated server (Lumen Vigil between two real players, a
participant disconnecting), and a real server restart mid-ritual (the lapse is tested by saving and loading the block
entity).

**Concordance Chunk C, unit 2 (C-03 and C-04; commits `a0e73881c` and `2c98d6d68`, records `4df687cc7`, merged with unit
1's review fixes in `2263f851b`, and the unit review's fixes in `23f28b90e`).** Written with no Minecraft jar; CI was
its first compile and run. **Every test below passed in CI:** every job of run 38693262154 on `23f28b90e` passed on its
first attempt, and every job passed again in run 38774859294 on `bb856f873` (the client shots: their shards passed;
which were viewed is below).
- `ConcordanceRitualGameTests`, 11 new (29 in all):
  - C-03: `anIdleCircleSaysEverythingARiteStillNeeds`, `aRefusedStartMarksEveryFault`,
    `aStatusReadingMarksFaultsButTheInspectPageDoesNot`, `spentAndReservedAreShownApart`, `theInterruptionReasonIsKept`
    (the ending kept in memory, never saved) and `unknownRitesStayUnnamed`;
  - C-04: `rootAndRivetAttunesOnePlateOnce`, `aDryPylonStopsRootAndRivetAndKeepsTheOfferings`,
    `rootAndRivetSavedMidRunLapses`, `theAttunementsOfferingsNeverChooseRootAndRivet` (combined offerings, the Kindling's
    11 items beating Root and Rivet's 9) and `rootAndRivetIsStrataFoundationWork` (a bystander who took no part lays no
    foundation).
- `EncyclopediaGameTests.theInspectPageShowsARitesReadinessAndPayments` (21 in all).
- `ConcordanceAssayGameTests.theFoundationIsNeverWeighed` (8 in all; renamed `theFoundationIsNeverFormed` at the merge
  with main `f904df3fd`, whose Scale weighs the foundation, and not yet run under that name).
- `ConcordanceProgressionGameTests.theLiveGraphIsWhole`: the node `ritual:jugcraft:root_and_rivet`, reachable by one
  player alone.
- Client shots in `EncyclopediaClientGameTests`: `jugcraft_encyclopedia_inspect_readiness` (the Inspect page on an
  anchor), `jugcraft_concordance_anchor_gathering_jade` (Jade on a gathering anchor, taken only when Jade is loaded) and
  `jugcraft_rooted_thallite_foundation` (the foundation in hand). Their shard passed; none of the three was viewed.
  The foundation's icon itself was viewed for unit 5's records: a green Thallite plate with copper rivets and roots
  beneath.
- The Jade checks in these tests run only when Jade is loaded, so a build with `-PjugcraftOptionalIntegrations=false`
  alone does not run them. No test reads the action bar's one line: a mock player's overlay is not readable.
- Run and passed locally for each of the unit's three commits: `python3 tools/check_mod_data.py`,
  `python3 scripts/check_repository.py`, `python3 tools/owner_art.py --check`,
  `python3 tools/concordance_journey.py --check`, `python3 tools/check_icon_maps.py`,
  `node --test tools/world-designer/model.test.cjs`, `python -m unittest discover -s scripts/tests` and `git diff --check`.
- Audit mutations on scratch copies, each reverted: C-03's 19 were all caught, and C-04's 23 (one of them a scratch rite
  offering a tag); deleting Mendvetch from `_item_name` surfaced as a crash (`KeyError`), not as a reported error. One
  control (C5', the spec's original switch loop) raised no switch error for the tag offering, so that loop would have
  missed it. A foundation-to-Ley conversion of 31 is, as intended, not a gaining cycle. No audit holds that
  running readiness reads the light, and no game test covers that line: only the scratch harness below does.
- A scratch harness outside Git compiled the pure Concordance packages at `2c98d6d68` with JDK 21 and ran 37 checks of
  `Choice` and `Readiness` with the game tests' figures (readiness lines and their order, warnings, spent and held, the
  stage tables of the attunement and the vigil); all passed. It is not a game test and it is not in Git.
- The unit review's fixes ([the contract](concordance-chunk-c.md), Fixes from the unit review under C-04):
  `spentAndReservedAreShownApart` also asserts that a pylon gone since the last step is a blocking line (passed in CI); the
  same local checks run and passed; the fixes' audit mutations were each reported, their negative cases and controls
  were not, and C-03's and C-04's suites gave the results above again; the scratch harness passed with three new
  checks.
- In CI only: `./gradlew build` (with and without the optional integrations) and `./gradlew runClientGameTest`. Not
  run: any client by hand, and two clients.

**Concordance Chunk C, unit 5 (C-10; [the contract](concordance-chunk-c.md#unit-5-evidence-and-handoff-c-10)).**
`ConcordanceWorkshopClientGameTests` begins Root and Rivet beside a working crucible in one real client on a dedicated
server: the anchor's Inspect page shows what the rite has spent apart from what it holds and its stage table, with no
key or id (shots `jugcraft_encyclopedia_inspect_root_and_rivet` and `_scale4`, at GUI scale 4, not viewed), Jade's
snapshot is checked (`jugcraft_concordance_anchor_root_and_rivet_jade`, viewed: the circle with its anchor, no Jade box
visible at the 480×270 preview size), and the rite completes with one foundation from exactly its offerings, 2 Ley
Charge a step from each pylon, taken by its participant. It passed in client shard 4 of run 38774859294. The load card
runs Root and Rivet again and again while measured (37 completed, none interrupted, in run 38774859294), and the
journey test performs it from lantern-filled pylons; both passed in that run.

## World and event applicability

No worldgen, creatures, loot or seasons. Rituals obey spawn protection, protected towns and build permission through
the shared effect boundary.

## Rollout and open questions

- New stable ids: blocks and items `jugcraft:circle_anchor`, `jugcraft:ley_pylon`, `jugcraft:warding_stone`,
  `jugcraft:adept_wand`; block entities for the anchor and pylon; research `jugcraft:circle_lore`; structure
  `jugcraft:lesser_circle`; rituals `jugcraft:adept_attunement`, `jugcraft:lumen_vigil`; conversion
  `jugcraft:radiance_to_ley`; instrument `jugcraft:adept_wand`; item component `jugcraft:ley_charge`; block tag
  `jugcraft:concordance/ritual_boundary`; item tags `jugcraft:circle_specimens` and `jugcraft:concordance_specimens` (every
  Concordance specimen, which examination now reads); built-in pack
  `jugcraft:fusion_textures`; the four sounds above.
- New stable ids since Concordance Chunk C (C-04): the ritual `jugcraft:root_and_rivet` and the item
  `jugcraft:rooted_thallite_foundation`, with its codex entry `circles/root_and_rivet`. New lang, among them:
  `compose.jugcraft.ritual.*` (readiness, spent and the stage table), `encyclopedia.jugcraft.inspect.*` for the anchor,
  the C-03 diagnostic codes `not_lent`, `needs_focus`, `too_bright`, `too_far` and `participants`,
  `message.jugcraft.concordance.circle.ended`, `.ended.spent` and `.where`, `tooltip.jugcraft.concordance.jade.spent`
  and `.reserved`, `jei.jugcraft.acquire.rooted_thallite_foundation`, and the foundation's item, tooltip and codex keys.
  `message.jugcraft.concordance.circle.kept` was removed (a message, never saved) for `circle.release.kept` and
  `.release.dropped`.
- Save compatibility: additive. Existing worlds keep knowledge, Focus and inscriptions. Chunk C adds no saved key: the
  last attempt's ending is kept in memory only.
- Open: larger circles and rituals for the other Principles arrive with their research; a GuiLib schematic.

**Item-icon provenance (Concordance Chunk C, C-04).** The Rooted Thallite Foundation's 16×16 icon is drawn fresh to
[ITEM_ICONS.md](../ITEM_ICONS.md) from the map `tools/item_icons/rooted_thallite_foundation.txt`: a Thallite plate with
its diagonal glint, copper rivets at its corners and three roots below. Its plate is the new `thallite` icon material in
`tools/icon_materials.py`, the Thallite metal's own ramp (`tools/material_icons.py` `METAL_RAMPS["thallite"]`) with
only the outline darkened to stay within rule 4; `check_concordance` holds its four tones to the metal's and the PNG to
its map. Nothing is taken from Mojang's files. The owner's library was searched first (`art/owner-library/catalog/files.csv`,
26,965 files): no file is named for a foundation, a rivet or Thallite, and none of the plate-like candidates fitted.

| Candidate | SHA-256 (start) | Why not |
|---|---|---|
| `originals/Magic/assets/ars_nouveau/textures/item/ritual_flashing_base.png` | `e9e50d806f53` | a grey studded square tile filling the slot (ITEM_ICONS rule 2), soft and dithered; no Thallite, no roots |
| `originals/Blocks/Guns/item/armor_plate.png`, `heavy_armor_plate.png` | `359667a0c053`, `4df8b0c187a5` | copper gun armour plates; no rivets or roots |
| `originals/Blocks/Weapon Smithing/Items/materials/reinforcement/base.png`, `iron.png` | `fea48a7743d8`, `6530304cd6ca` | framed square tiles filling the slot |
| `originals/Blocks/Weapon Smithing/Items/cast/plate.png`, `originals/Blocks/Weapon Smithing/foundry/casting/table_plate.png` | `c6a65ce984c8`, `c74ae07f1feb` | casting moulds |
| `originals/Blocks/metal_plate_copper.png` | `a11a33069e1c` | a copper plate |
| `originals/Blocks/Big Cannons and Mounted Guns/textures/item/aluminumplate.png`, `brassplate.png`, `copperplate.png`, `steelplate.png` | `d285627c2fcd`, `822276c5d019`, `9f4cfa8879d0`, `69034e74d357` | plain plates of other metals, with no rivets or roots |
| `originals/Blocks/Weapon Smithing/slime/foliage/roots.png` (and the enderbark `roots` and `slimy_roots` block textures) | `b54c05377316` | block foliage and wood, not an item |
| `originals/Blocks/Airships and Planes/item/hull_reinforcement.png` | `9749022e0cfb` | hull planking |

The owner has not reviewed the icon (the roots may read as legs). Its look in game comes only from the client shot
`jugcraft_rooted_thallite_foundation`, whose shard passed in CI, not viewed; the icon itself was viewed for unit 5's
records (a green Thallite plate with copper rivets and roots beneath; the roots may read as legs).
