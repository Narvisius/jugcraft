# Arcane Concordance: ritual structures and execution states

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
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
Research Notes, to understand it. Understanding it teaches both rituals. Completing rituals in **three different
chunks** masters it.

**The Lesser Circle.** A ritual is worked at a **Circle Anchor** with a circle built round it on the same level:

- four **Ley Pylons**, two blocks from the anchor on each side (the channels);
- eight **Warding Stones** at the corners and three blocks out (the boundary);
- the two blocks above the anchor left open (the clearance).

With nothing offered, use the anchor with an empty hand to hear what is wrong: a part **missing**, the **wrong block**, a space **obstructed**,
a pylon **without Ley Charge**, a pylon that **belongs to someone else**, or a part **out of reach** (in an unloaded
chunk). `/jugcraft concordance circle <pos>` gives the same report for an anchor within 16 blocks. With Jade, looking at
the anchor shows its phase, its step, how many faults it has and the first three; looking at a pylon shows its charge.

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

The **Adept's Wand** is the second instrument: a capacity of 12 (the Initiate's Wand has 8), 8 targets (6), 72 work
(48), two branches (one) and twice the duration. It is how a player reaches spells too large for the first wand.

**How a ritual ends.**

- **Completed**: the offerings are used and the result is made, both in the same server tick, once. A transformed item
  waits in the anchor for a participant to take it.
- **Broken off**: nothing is made; the offerings stay in the anchor, unlocked, to be taken back or tried again. Focus and
  Ley Charge already spent stay spent: they are what an attempt costs.
- **Broken off at the boundary** (a Warding Stone removed or replaced while it runs): the same, and the working lashes out,
  dealing its backlash as damage to every participant present.
- **The anchor broken**: the offerings drop where it stood, once.

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
crash can never keep both the result and the offerings.

**Effects** go through the shared executor (`ConcordanceEffects`) with the cause `RITUAL`, under a ledger sized to the
ritual's targets, so protection, friendly fire, spawn protection and tolerance apply as for any spell. Backlash is
`DAMAGE` through the same boundary.

**Practice.** A completed ritual records practice (`Evidence.Practiced("jugcraft:ritual", <chunk>)`) for each
participant present: one per chunk counts towards mastering Circle Lore. Practice before Circle Lore is encountered is
kept, as notes are, and counts once it is.

## Connections

- Input producer: First Light (Circle Lore needs it understood); Kindled Lanterns (Radiance) or any Jugcraft Energy
  generator (Ley Charge); Focus; vanilla offerings (amethyst, gold, glowstone, glow berries).
- Output consumer: the Adept's Wand (larger composed spells, step 8); party protection and creature detection.
- Technology connection: Ley Pylons accept Jugcraft Energy through the shared energy interface (an alternative to
  Radiance, never a way back to energy).
- Magic connection: the shared grammar's instrument limits, the shared effect system, research and practice evidence,
  the typed-resource conversion `radiance_to_ley`.
- Reachable entry path: First Light understood → Circle Lore by examining and studying vanilla items → craft anchor,
  pylons and stones from vanilla materials → charge pylons from a Kindled Lantern (First Light) or energy.
- Solo, trade and cooperative routes: Adept's Attunement is solo; Lumen Vigil needs two players. Research Notes can be
  traded to understand Circle Lore. No ritual is the only way to anything a player needs for core progression.
- Specialty use without other branches: needs only First Light (and either Radiance or energy for the pylons).

## Balance

Units: Focus (the caster's pool, 20 on an Initiate), Ley Charge (whole units), Radiance (whole units), Jugcraft Energy
(JE).

- A Lesser Circle has four pylons, so Adept's Attunement draws 4 × 2 × 5 = **40 Ley Charge** (60 Radiance, or 40,000 JE)
  over 10 seconds; Lumen Vigil draws 4 × 1 × 3 = **12 Ley Charge** (18 Radiance, or 12,000 JE) over 6 seconds.
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

## Dependencies and assets

No new dependency. Framework use:

- **GeckoLib**: the Circle Anchor is a GeckoLib block entity with idle and channel animations chosen from the synced
  phase (original model and texture by `tools/concordance_ritual_art.py`).
- **Player Animation Library**: participants hold the `circle_channel` gesture on a Jugcraft layer below Spell Engine's
  cast gestures.
- **Fusion** (optional): a built-in resource pack `jugcraft:fusion_textures`, registered only when Fusion is installed,
  joins neighbouring Warding Stones with a connected texture; without Fusion they are plain cubes.
- **Jade** (optional): anchor phase, step, fault count and the first three faults; pylon charge.
- **JEI** (optional): a Circle Anchor category showing Adept's Attunement (its offerings and the Adept's Wand), with
  the anchor as its catalyst.
- **Modonomicon**: a new **Circles** codex category (Circle Lore, the Lesser Circle, the Ley Pylon, Working a Ritual, and
  one entry per ritual), generated from the same tables as the data.
- **Not built: the GuiLib schematic.** The circle report (anchor, command and Jade) lists every part that is wrong and
  where; a GuiLib schematic screen remains optional future work.

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
  the checker or harness caught all 12.
- Game tests: `ConcordanceRitualGameTests` (thirteen): the circle checked part by part; the attunement completing once
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
- Save compatibility: additive. Existing worlds keep knowledge, Focus and inscriptions.
- Open: larger circles and rituals for the other Principles arrive with their research; a GuiLib schematic.
