# Arcane Concordance: experimental alchemy

Status: merged to main ([#244](https://github.com/jimbozoomer-byte/jugcraft/pull/244), from branch
`claude/awesome-davinci-iwv3b9`, integrated by [#263](https://github.com/jimbozoomer-byte/jugcraft/pull/263) on
8 October 2026); see Verification for what has run. Extended by Concordance Chunk C, unit 1 (C-01: the crucible says
why it waits and keeps a formula honestly; C-02: the Stillroot Poultice and the Kiln Draught; [the chunk's
contract](concordance-chunk-c.md)), and unit 3 (C-05: a [Crucible Stand](crucible-stand.md) heats a crucible with
JE; C-06: a crucible asks the couriers for its formula's shortage), not merged; their tests passed in CI, most
recently with unit 5 in run 38774859294 on `bb856f873` (see Verification).
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 13) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 5, Practitioner stage (Jugcraft Discovery). Roadmap step 13.
Primary specialty and supported player role: Alembists. Alchemy is the Concordance's patient, repeatable craft: a
player who works out a good process can write it down and have a crucible repeat it, and can explain exactly why a
draught does what it does.

Builds on [the shared effect system](arcane-concordance-composition.md) and [research and notes](arcane-concordance-sharing.md).
Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

**The Alembic Arts** (new research; needs First Light understood). Examine sugar, sweet berries, dried kelp, a spider
eye, bone meal or honeycomb to encounter it; examine three of them to observe it; study one at a Lampwright's Bench, or
read someone's Research Notes, to understand it. Understanding it lets you work an **Alembic Crucible**. Bottling
**three different outcomes** masters it.

**Ingredients are property vectors.** Every ingredient carries amounts on six axes, Radiance, Verdance, Ember, Rime,
Tide and Hollow, and some carry a contaminant:

| Ingredient | Properties | Contaminant |
|---|---|---|
| Glowstone dust | Radiance 1.50 | |
| Glow berries | Radiance 0.60, Verdance 0.60 | |
| Sweet berries | Verdance 1.00 | |
| Bone meal | Verdance 0.80, Hollow 0.30 | 0.20 |
| Magma cream | Ember 1.60 | 0.20 |
| Sugar | Ember 0.90, Tide 0.20 | |
| Snowball | Rime 1.00 | |
| Amethyst shard | Rime 0.80, Radiance 0.40 | |
| Dried kelp | Tide 1.20 | |
| Honeycomb | Tide 0.60, Verdance 0.40 | |
| Spider eye | Hollow 1.20 | 0.30 |
| Sunpetal | Radiance 1.00, Verdance 0.60 | |
| Dewmoss | Tide 1.40, Verdance 0.30 | |
| Gloamcap | Hollow 0.90, Rime 0.40 | 0.10 |
| Mendvetch | Verdance 1.40 | |

The last four are the garden's crops ([the ecology record](arcane-concordance-ecology.md)); this table lacked them until
Concordance Chunk C.

**Preparations are explicit transformations.** As it comes (`raw`), an ingredient goes in undissolved: all of it waits
to be stirred out. **Ground** in a Mortar and Pestle (`ground`: hold the mortar, the ingredient in the other hand) it
keeps 90% of its properties, and 60% of that dissolves the moment it goes in. The kitchen uses the same mortar: a Dried
Chili in the other hand grinds into two Paprika ([vegetables-herbs-and-spices.md](vegetables-herbs-and-spices.md)).

**The crucible.** Place it over a heat source. Its temperature moves one degree a tick towards the source's: nothing
20, magma 60, soul campfire 100, campfire 120, soul fire 150, fire 175, lava 220 (a campfire only while lit). Four
bands decide what a **stir** (a Stirring Rod or a stick, at most one every half second) does:

| Band | From | A stir dissolves of what is pending | Rime kept | Also |
|---|---|---|---|---|
| Cold | | 10% | all | |
| Warm | 40 | 30% | 60% | |
| Hot | 90 | 55% | 30% | |
| Searing | 160 | 80% | none | keeps only 85% of the dissolved Radiance and Verdance, and adds 0.50 contaminant |

**A Crucible Stand beneath** (since Concordance Chunk C, C-05; not merged) heats with JE: following a formula,
for its next stir (warm 65, hot 120, searing 175, for 8, 16 or 32 JE a tick), only while the crucible waits for nothing
or for heat alone; with no formula, to the stand's own setting. See [crucible-stand.md](crucible-stand.md). An Electric
Pump piped into the tank fills it for the formula's water steps.

**Rime is cold's own** (since Concordance Chunk B, unit 4, B-08; [the winter record](arcane-concordance-winter.md)):
every stir above the cold band keeps only that share of the Rime already dissolved. A ground ingredient's ready share
stays until a stir, since an addition has no band.

Put in water first (a water bottle is one part, a water bucket three; at most six parts), then ingredients, then stir.
**Bottling** draws one part's share of everything dissolved: that is the dose. A glass bottle makes a **draught**
(drink it), a bowl a **salve** (use it on a creature, or on yourself while sneaking).

**What a dose does** depends only on how much of each property a part holds:

| Property | Effect | Threshold | Stronger by | Highest level | Duration per unit (cap) |
|---|---|---|---|---|---|
| Radiance | Night Vision | 0.50 | | I | 60 s (120 s) |
| Verdance | Regeneration | 0.60 | 1.50 | II | 15 s (22.5 s) |
| Ember | Fire Resistance | 0.60 | | I | 60 s (120 s) |
| Rime | Frostward | 0.80 | | I | 60 s (120 s) |
| Tide | Water Breathing | 0.60 | | I | 60 s (120 s) |
| Hollow | Poison | 0.40 | 1.00 | II | 10 s (21.6 s) |
| Contaminant | Nausea | 0.50 | | I | 8 s (20 s) |

**Contaminant fouls Verdance** (since Concordance Chunk C, C-02; C-P7, a starting value). Before a dose is judged, each
unit of contaminant a part takes 4.00 units of Verdance a part, and the reason names what fouled it (the ingredients or
searing stirs that brought the contaminant). No other property is fouled.

Below its threshold a property does nothing, and the explanation says so. **Frostward** (since Chunk B, unit 4) is a
Jugcraft status: while it lasts no frost gathers on its holder and the Yeti King's frost does not take; milk ends it.
Three snowballs in one part of water, stirred cold three times, make **Winter Brine**: 0.813 Rime a part, Frostward I
for 48.75 seconds. Stirred warm, the same keep 0.685 and do nothing. Before unit 4, Rime gave Resistance (30 s a unit,
at most 60 s); a Rime draught bottled then still gives Resistance.

**Products** (since Concordance Chunk C, C-02). A brew of the right form carrying a product's status and nothing harmful
is named on its stack, with vanilla's item name (an anvil can still rename it; the name is never how it is known).
Figures from the generator's model, not yet measured in a game:
- **Stillroot Poultice**, a salve of Regeneration: two Mendvetch as they come in one part of water, stirred warm four
  times, hold 2.127 Verdance a part: Regeneration II for 22.5 s. Stirred in with them, one, two and three bone meal
  leave 1.935, 1.743 and 1.551 after fouling: Regeneration I, then Poison too, then Nausea too. A searing stir in place
  of the last warm one leaves 0.215 (2.215 less 2.000): Nausea only. In two parts of water the batch makes two doses
  of 1.063, Regeneration I for 318 ticks each: every dose of a batch is the same, so they stack. Use it on a friend, or
  on yourself while sneaking; the bowl comes back. A poultice another player puts on you in a lair fight earns them healing credit
  ([lair-encounter-credit.md](lair-encounter-credit.md)).
- **Kiln Draught**, a draught of Fire Resistance: one magma cream as it comes in one part of water, stirred hot twice,
  holds 1.276 Ember a part (and 0.200 contaminant, under Nausea's 0.500): 76.55 s. Two sugar stirred the same way hold
  1.435: 86.1 s. Under it the Cinder Kiln's slag does not burn and his flame does not take
  ([cinder-kiln.md](cinder-kiln.md)).
- **Winter Brine**, a draught of Frostward (Chunk B's), now named on its bottle too.

**A worked example** (the one the game tests use). One water bottle, one glowstone dust as it comes (1.50 Radiance
pending), the crucible over a campfire (hot). The first stir dissolves 55%: 0.825. The second dissolves 55% of the 0.675
left: 0.371 (amounts are kept in thousandths and rounded down). Bottled, the draught carries 1.196 Radiance, over the
0.50 threshold: Night Vision I for 1.196 × 60 = 71.75 seconds. Stirred once more, or with a second glowstone dust, it
would last longer; stirred over fire (searing), it would lose 15% of its Radiance and gain contaminant.

**Sampling gives increasing information.**

- **Sampling Spoon** (anyone): the band, the volume, the strongest property and whether it is murky.
- **Assay Glass** (the Alembic Arts understood): the temperature, every property dissolved and still pending, a part's
  share and the contaminant, and (since Chunk C) what the contaminant takes from the Verdance a part. Without the
  research it reads like a spoon.
- **Assay Glass, mastered**: what a dose bottled now would do, and why: each effect with its property, how much a part
  holds against the threshold, and which ingredients (or searing stirs) put it there; for Rime, also "stirs above the
  cold, which thaw Rime" when a warmer stir thawed some; and (since Chunk C) what fouled the Verdance and by how much.
- **The encyclopedia's Inspect page** (since Chunk C, C-01): looking at a crucible within your block reach, it shows
  to anyone its heat and the block heating it (or why that block does not), Preserve's hold, its volume and tank, the
  formula's step in words, why it waits, the risk line and its slots. With an Assay Glass carried it adds what the glass
  would tell you, at your level; without one it says that a glass reads more.

**Formulas.** Use a blank formula on a crucible whose mixture is fresh (nothing bottled yet) and would do something: it
writes down the whole process, for example `water 1; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot`.
Use the written formula on any crucible to set it (sneak with an empty hand to clear it). Fed water through a pipe and
ingredients (or reagents) through a hopper into its five buffer slots, the crucible follows the formula step by step,
once a second: it takes exactly the ingredient and preparation the step names, waits until its temperature is in the
band the step names before stirring, then bottles into its output while it has bottles or bowls. It never guesses or
substitutes; it waits. The same process gives the same result, so the repeat makes exactly the same draught.

Since Concordance Chunk C (C-01):
- **It says what it waits for:** used with an empty hand, its status line ends with the reason: water, an ingredient
  (or the same one prepared otherwise), the heat, Preserve's hold, a bottle or bowl, an output that is full or holds
  another brew, a batch to empty before a repeat. Jade and the Inspect page show the same reason. A shortage or a
  blockage shows a want sign once when it begins; a formula it cannot follow shows danger with the breaking sound;
  waiting for heat or a hold shows nothing. Preserve's hold is named only while it is what keeps the heat from the
  stir's band; over a block that cannot heat it that far (a magma block for a hot stir), it waits for the heat, held
  or not.
- **A formula is kept honestly:** a formula the rules in force can no longer make (after a reload or a data pack) stays
  set at its step. It names the step and why, and does nothing, not even bottling a finished batch, until the rules can
  follow it again; then it carries on. A saved formula this build cannot read is kept as written and reported. Setting a
  written formula the rules refuse names the step and why, and setting one that would harm says so: the risk line names
  the harmful statuses a dose would carry and the first searing stir. When the mixture in the crucible is not the
  formula's own batch (it was there before the formula was set, or the rules refuse the formula), the risk line also
  names that mixture's harm, and that it was stirred searing, since a dose of it can still be bottled by hand. Harm is
  judged by the server (a harmful or unknown status) as well as by the property's data.
- **Setting is a plain use;** the formula's tooltip said "sneak" until Chunk C.
- **It asks the couriers for what it lacks** (since Concordance Chunk C, C-06; not merged): waiting for an
  ingredient, a crucible files one courier request, as whoever set its formula and while they are here (or the server
  lets absent owners' devices act), at the nearest Courier Post within 16 blocks they may use, for exactly what its
  formula lacks first (a ground reagent stays a ground reagent). A Peepo at its port brings only what no courier holding
  that request is bringing. Clearing, replacing or losing the formula, setting it as another player, or breaking the
  crucible turns the request back; with its setter's sixteen requests already open it says so and asks again later
  (200 ticks, doubling to 1,200). See [logistics](arcane-concordance-logistics.md).
- **Only someone who could break the crucible by hand** may pour, add, stir, bottle, or set or clear its formula
  (claims, protected towns, adventure mode and spawn protection refuse). Tasting, the glass and recording onto a blank
  formula stay open.

## How it works

**Data** (`data/<ns>/concordance/ingredient/`, `/preparation/`, `/property/`, read by `AlchemyCatalog` in
`concordance/alchemy`): an ingredient names its item, its properties and contaminant; a preparation its tool, scale and
the share ready at once (exactly one has no tool: `raw`); a property its axis, status effect, intent, threshold, level
step, highest level, ticks per unit and duration cap, within the shared effect limits. The loader refuses anything
unknown or out of range, with a reason.

**The simulation is pure and deterministic** (`Mixture`, `Operation`, `Band`, `Heat`, `Outcome`, `Formula`, `Assay`).
A mixture is its volume in parts, its pending and dissolved vectors, its contaminant, the doses drawn and its history
of operations (at most 32). All amounts are whole thousandths (`Vector`, longs) with floor division, so identical
operations always give identical mixtures and doses on every machine. Every dose of a batch is the same share: what is
left after a bottle holds exactly that share for each part still in it, and what did not divide evenly (under a
thousandth a part) is lost, never given to a later dose (since Chunk C's unit review; before, the last dose took it, and
an automated batch's doses could differ and not stack). `Operation` is one of `water <n>`,
`add <item> <preparation>` and `stir <band>`, each with one canonical text; a `Formula` is the history as text,
parsed back and replayed against the catalog before it is accepted.

**The crucible** (`CrucibleBlockEntity`) holds a `Mixture`, its temperature, a water tank (Fabric Transfer API, water
only, one bucket), five buffer slots, a bottle slot, an output slot and an optional formula. Temperature is computed on
the server every tick from the block beneath. Every hand operation goes through `Mixture.apply`, which refuses with a
reason rather than half-applying. Hoppers reach the buffer (ingredients and reagents) and the bottle slot (bottles and
bowls) from above and the sides. Brews come out from the sides or below: the heat source sits beneath, so a Pneumatic
Extractor at a side is what empties an automated crucible.

**Brews** carry their effects as the item component `jugcraft:brew` (status, level, ticks, harmful), and a ground
ingredient is a Reagent item with `jugcraft:reagent` (item and preparation). A brew is applied through the shared effect
executor (`ConcordanceEffects`, kind `STATUS`, cause `POTION`), so friendly fire, protection and tolerance apply as for
any spell. A harmful salve on another player follows the same rule as a harmful spell.

**Practice.** Bottling by hand records practice (`Evidence.Practiced("jugcraft:alchemy", <outcome>)`) keyed by the
outcome (for example `minecraft:night_vision@0`): three distinct outcomes master the Alembic Arts; the same draught again
counts once.

**Since Concordance Chunk C, unit 1:**
- *Diagnostics* (C-01). The pure package `concordance/diagnostic` holds the shared codes (`Code`: 03:9's eleven, then
  Chunk C's eight, each with a kind) and `Diagnostic`, a code with at most four parameters, worded under
  `compose.jugcraft.diagnostic.<id>` (table: `tools/concordance_diagnostics.py`).
  - Each automation pulse (`automate`) returns why it waits, and the crucible keeps that reason (not saved).
    `waiting()` gives the formula's incompatibility if it has one, otherwise that reason. The status line, Jade, the
    Inspect page and the Peepo port all read `waiting()`.
  - The kind of a new reason sets its sign.
- *The honest reload* (C-01). `incompatibility()` replays the formula (at most 32 operations) once whenever
  `ConcordanceData.revision()` differs from the one it last checked, and after a load. The refusal carries its step
  (`Mixture.Refused.step`), and `Formula.incompatible` words it with the step's description (`Operation.describe`).
  `follow(Formula)` sets a formula from its first step without the item's checks.
- *Products* (C-02) are data, `data/<ns>/concordance/product/` (form, status, "nothing harmful", a name key and a codex
  entry), read by `RulesParser.product` into `AlchemyCatalog`. `Alchemy.product` finds the one product a stack is, by
  its form and what its brew carries, with harm judged by the server; a brew matching two stays unnamed.
- *Fouling* (C-02): a property may declare `fouling`. In `Outcome.of` the fouling is taken, subtracted, then judged
  (`Property.fouled`).

## Connections

- Input producer: First Light (the Alembic Arts needs it understood); vanilla ingredients from farming, the Nether, the
  ocean and caves; heat from any campfire, fire, magma or lava.
- Output consumer: players and creatures (vanilla status effects through the shared executor).
- Technology connection: water through the Fabric Transfer API (Jugcraft pipes or any mod's), hoppers for ingredients and
  bottles, the output for any extractor. A formula is canonical process data any automation can carry.
- Magic connection: the six axes are the Principles' own names; the same research, notes and practice rules as the rest
  of the Concordance; the shared effect boundary. Since Concordance Chunk B, unit 3
  ([Rime and Strata](arcane-concordance-rime-strata.md)): a brew that carries Rime, bottled by hand, is Rimekeeping's
  workshop study, and Preserve holds a crucible's heat in its band for a minute while you stir (two with a Focus of
  Water worn, unit 4). Since unit 4 ([the winter record](arcane-concordance-winter.md)): Winter Brine is the winter
  workshop's brew, and the Yeti King's frost respects its Frostward.
- Reachable entry path: First Light understood → the Alembic Arts by examining vanilla items → craft the crucible
  (four copper, a cauldron, three bricks) and tools from vanilla materials.
- Since Concordance Chunk C (not merged): input producers also Mendvetch from Verdant Beds or crafted, a Crucible
  Stand's JE, and a courier's or a Peepo's deliveries; output consumers also a poultice on another player in a lair
  fight (encounter credit) and the Kiln Draught at the Cinder Kiln's slag. Unlocks: nothing; no product is an
  ingredient, a recipe input or a research gate (`check_workshop_products`).
- Solo, trade and cooperative routes: entirely solo; Research Notes and written formulas can be traded.
- Specialty use without other branches: needs only First Light.

## Balance

- Every effect is a vanilla status effect at level I or II, and **no brew outlasts the vanilla potion of the same effect
  at the highest level it can reach**: night vision, fire resistance and water breathing at most 2 minutes (vanilla: 3);
  Regeneration (which reaches II) at most 22.5 seconds and Poison (which reaches II) at most 21.6 seconds, vanilla's
  strong-potion durations, even at level I. Nausea has no vanilla potion, and nor has Frostward (since Chunk B, unit 4),
  which is held to the Fire Resistance brew's numbers (at most 2 minutes). No brew gives Resistance any more: Rime gave
  Resistance I for at most a minute until unit 4.
  `tools/check_mod_data.py` enforces this. Brewing is cheaper than vanilla brewing (no blaze powder, nether wart or
  brewing stand), so it is held to vanilla's ceilings rather than allowed to exceed them.
- Since Concordance Chunk C (C-02), two things alchemy makes are consumed elsewhere, and neither feeds a conversion:
  - the Stillroot Poultice heals a friend and, in a lair fight, earns its helper a support share bounded by the fight's
    hurt (health is no modelled resource, so the Vigil's Vitae bound holds whatever heals);
  - the Kiln Draught's Fire Resistance stops the Cinder Kiln's slag and the Cinder Tyrant's flame.
  No brew is an ingredient, a Scale material or anything a recipe, trade or the Scale takes. A draught gives its bottle
  back and a salve its bowl. Diluting a product gives no more in all: one poultice heals 18, the same batch in two parts
  12. A second dose never adds a level. The Tyrant Scale (two magma cream) and Grave Dust (bone meal, for Mendvetch) are
  declared reward routes. `check_workshop_products` holds all of this.
- Searing heat trades quality for speed (85% of Radiance and Verdance kept and contaminant added), so the fastest fire
  is not always the best; since Chunk C its contaminant also fouls Verdance. Any heat above cold thaws Rime (warm keeps
  60%, hot 30%, searing none), so a Rime brew wants the cold band, per recipe: four raw snowballs stirred warm twice
  still reach Frostward, and five stirred hot once; searing never does.
- Grinding loses a tenth for speed: a ground ingredient needs fewer stirs.
- A crucible holds at most six parts: six doses per fill.

## Multiplayer and persistence

- Server authority: the crucible simulates on the server; clients receive the temperature and volume only (for the
  vessel's animation), never the makeup, which takes sampling. Hand operations are rate-limited and check research; a
  stir's band is the server's.
- Since Chunk C, C-06: a crucible files courier requests only as its formula's setter, and only while
  `Authority.answering` finds them (here, that is online, alive and in the crucible's dimension, or the server's
  absent-owner option). That stops new filings only: an unclaimed request open when they leave holds the post's queue
  for every request behind it (`OWNER_OFFLINE`) until they return or someone who may change the crucible clears its
  formula.
- Who may change a crucible (since Chunk C, C-01): every change by hand asks `Authority.mayChange` as the player (water,
  adds, stirs, bottling, setting or clearing a formula). Tasting, the glass and recording onto a blank formula stay
  open. The Inspect page's position is the client's claim: the server reads only a loaded crucible within the reader's
  block reach (plus its 1.0 use allowance).
- Persistence: the crucible saves its mixture exactly (to the thousandth), history, formula, step, tank and slots, and
  Preserve's hold, `held_until` (game time), only while held; broken, its items drop (vanilla's container behaviour) and
  the mixture spills. Items carry `jugcraft:brew`,
  `jugcraft:reagent` and `jugcraft:formula`.
- Since Chunk C, C-06 (not merged): the crucible also saves who set its formula (`formula_owner`, a UUID) and its open
  courier request (`request`, a number), each only when set, and reads both with defaults: a save from before reads as
  nobody's formula with no request, and asks no courier until the formula is set again. The stamp stays 1. The filing
  backoff is never saved. Broken, its request is turned back (its cargo goes home) and its slots drop once, as before.
- Since Chunk C: a saved formula this build cannot read is kept as written under `program` and saved back. The waiting
  reason and the rules revision last checked are never saved; they are worked out again after a load. Product names
  reach new stacks only, so a named brew and an older unnamed one of the same outcome do not stack (an automated
  crucible pauses on one in its output and says so). Verdance brews that carry contaminant heal less from now on;
  bottles already made keep what they carry.
- Disable behaviour: with `concordance.enabled=false`, crucibles refuse work and stop following formulas; everything stays
  registered.

## Dependencies and assets

No new dependency. Framework use:

- **GeckoLib**: the crucible is a GeckoLib block entity; its controller plays `empty`, `still` or `simmer` (the liquid
  swells and the ladle turns) from the synced volume and band.
- **Texture animation**: the liquid surface is a four-frame animated sheet (`crucible.png.mcmeta`), which GeckoLib uses
  for the geo model.
- **Jade** (optional): temperature and band, volume, and when following a formula its step and the next operation it
  waits for ("Next step: ..." since Chunk C). Since Chunk C (C-01) also the tank in whole parts and the crucible's own
  reason. The server sends the reason as its code and parameters (`Diagnostic.encode`), and the client words it. Never
  the makeup: `check_signs` locks the snapshot's keys.
- **JEI** (optional): each ingredient's properties and the mortar's preparation as ingredient information, and the tools'
  crafting recipes. JEI never shows outcomes: those are for the player to work out (or the mastered Assay Glass). Since
  Chunk B, unit 4, the Rime ingredients' pages (snowball, amethyst shard, gloamcap) add "stirred cold, its Rime brews
  Frostward (Winter Brine)": a fact about the property, never a formula's outcome. Since Chunk C, the Mendvetch page
  names the Stillroot Poultice, and the magma cream and sugar pages the Kiln Draught, the same way.
- **Modonomicon**: a new **Alembic** codex category (the Alembic Arts, the crucible, properties, sampling and formulas),
  generated from the same tables as the data. Since Chunk B, unit 4, it has a **Winter Brine** entry (the Alembic Arts
  observed), and the Stirring page names the Rime each warmer band keeps. Since Chunk C: **Stillroot Poultice** and
  **Kiln Draught** entries (the Alembic Arts observed). The Properties page names Verdance's fouling, and the Sampling
  page the Inspect page. The Formulas page says the crucible says what it waits for, keeps a refused formula, and asks
  who may change it.
- **Fabric Transfer API**: the water tank (`FluidStorage.SIDED`).

Sounds `jugcraft:concordance.crucible_add`, `.crucible_stir`, `.crucible_bottle` are synthesised by
`tools/concordance_sounds.py`; the crucible model, its sheet and the item textures are drawn by
`tools/concordance_alchemy_art.py`. Everything is original.

## Verification

- `python3 tools/check_mod_data.py` passes. New step 13 checks (`check_alchemy`): the Java axes, bands, heat sources and
  limits equal the generator's; every ingredient, preparation and property parses and is reachable; exactly one
  preparation has no tool; the contaminant property is harmful; every line the simulation can say has its words; the
  crucible's GeckoLib model, animations and animated sheet agree; no property outlasts the vanilla potion of its effect
  at its highest level (setting Poison's cap back to 600 ticks fails the check). A new rule (found while testing this
  step): every sound a spell plays must be registered as a SoundEvent (see below).
- `python3 scripts/check_repository.py` passes.
- The pure core compiles with JDK 21. The step 13 harness passes **61 checks** against the generated data (that harness
  is not in Git: `src/test` holds only `VanillaReferences.java`), including:
  the worked example to the thousandth; two independent replays of the same operations giving equal mixtures and doses;
  bands, searing loss and contaminant; ground preparation; bottling shares; thresholds, levels and duration caps; the
  formula's canonical text parsing back to the same operations; refusals (no water, overflow, unknown ingredient, too
  long) changing nothing; each assay level's content; and three distinct outcomes mastering the research while a repeat
  counts once. The step 12 mutation script also covers alchemy data and code.
- Game tests added: `ConcordanceAlchemyGameTests` (seven): heat comes from beneath (about 60 after 40 ticks over a
  campfire, settling at 120; cooling to 20 with nothing beneath); the same process in two crucibles makes the same
  mixture and the same draught, Night Vision I for 1435 ticks, recorded as practice; only an alembist works the crucible
  (a novice can still taste it), the mortar grinds, a bowl makes a salve that takes on a pig; a formula recorded by hand
  is repeated by another crucible fed by tank and buffer, making the same draught and using exactly its inputs, with
  hoppers reaching only the right slots from the right sides; **real machinery runs a formula twice** (a campfire
  beneath, a vanilla hopper above dropping in glowstone and bottles, water in the tank through the Transfer API, and a
  Pneumatic Extractor at the side moving each draught into a chest: two identical draughts, the inputs used exactly); a
  broken crucible drops its items; a crucible saves its mixture and formula exactly.
- CI, run 37567742360 (Build workflow, run manually on this branch, commit `e17a6efb`, with the side output, the
  machinery test and the vanilla duration ceilings): `mod` passed with **"All 881 required tests passed"** (the thirteen
  ritual tests and the seven alchemy tests included) and `optional integrations absent` passed. The client test shards
  fail before any test starts, with the same OpenGL startup crash (`Couldn't find matching GLX visual`) as on the
  framework foundation branch. Earlier runs: 37554993916 (commit `6caef7eb`) passed 880 of 880 and 37554056736 (commit
  `6d07d0dd`) 879 of 879, as tests were added. No log shows Spell Engine failing to play a sound.
- Earlier runs on this branch found and fixed: a compile error (the crucible compared the held item with the formula
  data component instead of the formula item; run 37553902529), and, for step 12, run 37552117833 passed 873 of 874,
  failing only the pylon's drop check (see the rituals record).

**Found and fixed in step 10's work.** The server log of an earlier run showed Spell Engine failing to play
`jugcraft:concordance.lance`: the five invocation release sounds had sound files and `sounds.json` entries but were never
registered as SoundEvents, which Spell Engine looks up. They are registered now, and `tools/check_mod_data.py` fails the
build for any spell sound that is not.

**After `main` was merged in (7 October 2026),** run 37582029152 on commit `862e2c37` passed every job: all 941 server
game tests, the build without optional integrations, and for the first time on this branch all 91 client game test
classes. They had crashed because the framework foundation put Iris on the development client, which CI's renderer
could not start; a workaround on this branch let them run, and `main`'s own fix (#239: an OpenGL context through SDL's
EGL in CI) has since replaced it, so this branch uses `main`'s build unchanged. The client tests load
the Concordance's client code, but none of them looks at a Concordance block, item or screen yet.

Not yet run: any client (the crucible's GeckoLib animation and animated surface, the codex pages and JEI information in
game), a two-client dedicated server, and a fluid pipe network filling the tank (the machinery test fills it through
the Transfer API, the interface pipes use).

**Concordance Chunk B, unit 4** ([the winter record](arcane-concordance-winter.md); not yet merged) added four game
tests to `ConcordanceAlchemyGameTests`: `winterBrineNeedsTheColdBand` (the Rime each band keeps, the generator's
numbers; a cold and a warm crucible by hand; Frostward for 975 ticks), `oldRimeBottlesStillGiveResistance` (a save
test), `aRimeFormulaWrittenBeforeBrewsFrostward` and `frostwardIsNeverCheaperThanItsSnow` (economy loops that must
fail). They passed in CI (run 38549876709 on `b012875ab`, by workflow_dispatch; not merged), among its 1676 required
server tests. The existing alchemy tests are unchanged, and passed there too: none of them brews Rime.
`check_alchemy` now mirrors `RIME_KEEP_*` and accepts a property's status only if vanilla or Jugcraft registers it; the
new `check_winter_brine` holds the rest. `check_mod_data` passes, locally and in that run's `mod` job.

**Concordance Chunk C, unit 1** ([the chunk's contract](concordance-chunk-c.md); not merged). **Passed in CI:** every
job of run 38684732031 (through `0b1039312`, after one re-run of a client shard for main's Peepo cow-breeding flake),
and again in run 38774859294 on `bb856f873`. The tests were written where no Minecraft jar is available; CI was their
first compile and run.
- **New server game tests:**
  - `ConcordanceAlchemyGameTests` +20 (33 in all). C-01's 13:
    - `aChangedRuleKeepsTheFormulaAndSaysWhy`, `aRestoredRuleResumesTheFormula`,
      `aWrittenFormulaThatNoLongerReplaysSaysWhichStep` and `aSavedFormulaThatNoLongerReadsIsKeptAndReported`;
    - `anIncompatibleFormulaAsksForNothing`, which also shows that an incompatible formula bottles nothing;
    - `aFullOutputPausesAndVoidsNothing`, `twoHoppersRaceForTheBuffer`, `partialWaterWaitsThenDrawsExactly`,
      `aRestartJustBeforeOutputBottlesOnce` and `theCompanionAndTheCrucibleAgreeOnFull`;
    - `aHoldIsTheReasonOnlyWhileItKeepsTheHeat` (added by the unit review's fixes);
    - `onlyThoseWhoMayChangeACrucibleWorkIt` (a claim and adventure mode);
    - `jadeIsToldTheTankAndWhyNeverTheMakeup`, whose Jade checks run only when Jade is loaded.
  - C-02's 7: `aStillrootPoulticeRepeatsAndADirtyBatchHealsLess`, `aKilnDraughtCarriesItsDrinkerAcrossTheSlag`,
    `aPoulticeReturnsItsBowlToAFullInventory`, `productsNeverFeedTheCrucible`, `twoPoulticesDoNotAdd`,
    `dilutionNeverHealsMore` and `anOldUnnamedBrewInTheOutputPausesAndSaysSo`.
  - `ConcordanceAssayGameTests.workshopProductsAreNeverWeighed`.
  - `EncyclopediaGameTests.theInspectPageReadsACrucibleByResearch`.
  - The credit and dossier tests are in [lair-encounter-credit.md](lair-encounter-credit.md) and
    [cinder-kiln.md](cinder-kiln.md).
  The rules are swapped only through the tests' `under()`, which restores them in a `finally` in the same callback.
- **Client game test:** `EncyclopediaClientGameTests.crucibleInspection`. A crucible follows the Kiln Draught's formula
  and waits for its magma cream. The test checks that the reason the server encodes reads back in words, with no key
  or `jugcraft:` id showing, in the reason and in the Inspect page's rows. A dirty poultice's preview names what fouled
  it. Shots: `jugcraft_encyclopedia_inspect_crucible`, `_scale4` and `_poultice`, and
  `jugcraft_concordance_crucible_jade` when Jade is loaded.
- **New and changed audits in `tools/check_mod_data.py`:**
  - `check_diagnostics` (new): codes, kinds and words; the crucible keeps a refused formula, shows a sign once and asks
    Authority first; the Peepo mapping; no client reads the rules; tests swap the rules only through `under()`;
  - `check_workshop_products` (new): products are the generator's; each worked process brews its product alone, with
    nothing harmful, from ingredients crafted from vanilla things; each has a consumer; none is an ingredient, a Scale
    material or a recipe input; containers come back; dilution gives no more; brews stack as the strongest; only the
    declared boss routes;
  - `check_alchemy`: the fouling mirror and its order; preparation names; a written formula set by a plain use;
  - `check_signs`: Jade's crucible snapshot and its client;
  - `check_encyclopedia`: the crucible page's reach and glass, and that it only reads;
  - the crucible in `AUTHORITY_ROUTES`, and `diagnostic` in the pure-package list.
- **Fixes from the unit review** (a sixth commit, `d593ae61d`; in CI with the unit, run 38684732031):
  - every dose of a batch is the same share (`Mixture.bottle`, `Vector.times`, and the generator's `bottle`), so a
    poultice in two parts bottles 1.063 twice and an automated batch's doses stack. Before, the last dose took the
    remainder (1.063 then 1.064), the two salves differed, and an automated crucible paused on its own second dose
    ("holds something else"). `dilutionNeverHealsMore` now runs the two-part poultice as a formula and expects one
    stack of two;
  - the risk line (`Assay.risks`) also names the harm of a mixture that is not the formula's own batch: one there
    before the formula was set, or under a formula the rules refuse. Harm is judged by the server as well
    (`Alchemy.harmfulStatus`), on the Inspect page and when a formula is set;
  - the formula item's refusal is built by `CrucibleBlockEntity.refusal`, which the test asserts against step 2's
    refusal;
  - Preserve's hold is the reason only while it keeps the heat from the stir's band
    (`aHoldIsTheReasonOnlyWhileItKeepsTheHeat`);
  - the dossiers name the poultice's credit only while a salve of Regeneration alone is the Stillroot Poultice;
  - the searing danger words also say a searing stir thaws all Rime;
  - the Peepo status is asserted where each wait already happens: `NEEDS_WATER`, `NEEDS_BOTTLES`, `NO_INPUT`, `OCCUPIED`
    and `WRONG_TEMPERATURE`, beside `FULL` and `RECIPE_MISSING`;
  - the client test puts the Assay Glass back after its crucible shots;
  - audits: `canPlaceItem`'s two returns and the mortar's whole refusal condition; equal doses in the generator and in
    `Mixture.bottle`; `formula()`'s refusal and risk line; the Inspect page's risk-line inputs; the dossier's product
    match.
- **Run locally for each of the unit's commits, and passed:** `python3 tools/check_mod_data.py`,
  `python3 scripts/check_repository.py`, `python3 tools/owner_art.py --check`,
  `python3 tools/concordance_journey.py --check`, `python -m unittest discover -s scripts/tests` and `git diff --check`.
  The audit mutations tried on a scratch copy were each reported (30 for the products' commit, 22 for the credit's).
  The first two commits' listed mutations were not run when they were made. They were run with the unit review's fixes,
  on a scratch copy against `check_mod_data`'s Concordance checks: commit 1's twelve (20 variants) and commit 2's seven
  were each reported, but for dropping `ground` from `PREPARATION_NAMES`, which is reported only once the generator has
  rewritten the lang (the stale key hides it from the audit alone). The fixes' own twelve mutations were each reported.
  The pure alchemy packages also compiled with `javac` against a stub of jspecify's `@Nullable`, and a scratch harness
  replayed the new assertions' alchemy against the real data: two doses of 1.063 for 318 ticks, the Kiln Draught's 765
  and 765, equal doses in 2 to 6 parts, and the risk lines (with a stand-in for the registry's harm test).
- **In CI only:** `./gradlew build` with and without the optional integrations and `./gradlew runClientGameTest`
  (above). **Not run:** any play by hand, and two clients.

**Concordance Chunk C, unit 3** ([the chunk's contract](concordance-chunk-c.md); not merged). Its server tests passed
in CI run 38720814268 (`mod` and `optional integrations absent`), where client shard 0 failed on a defect in the Peepo
unloaded-crucible check, fixed in `9098c06b1`. Every job passed in run 38726242149 (the merge with unit 4, before the
fix) and, with the fix, in runs 38731825181 and 38774859294.
- `ConcordanceAlchemyGameTests` +8 (41 in all), the Crucible Stand's: `aStandFollowsTheFormulasBands`,
  `anUnpoweredStandLetsTheCrucibleCool`, `aStandIdlesForAWaitingOrIncompatibleFormula`,
  `aStandIsSetOnlyByThoseWhoMayChangeIt`, `theFullChainRunsFromAPumpAndACable` (a pump and a pipe fill the tank, which
  three water steps draw exactly), `aCampfireCrucibleStillWorks`, `aSwitchedOffConcordanceStandDrawsNothing` and
  `aCableMeetsTheStandWhereItTakesPower` ([crucible-stand.md](crucible-stand.md)).
- `anIncompatibleFormulaAsksForNothing` now also asserts that a stand beneath gives no heat (C-05), and that a formula
  the rules refuse turns its courier request back and files no other, asks again once the rules are restored, and
  loses the request when the crucible is broken (C-06).
- C-06's tests are in `ConcordanceLogisticsGameTests` (nine new) and `ConcordancePersistenceGameTests`
  (`theCrucibleKeepsItsRequestThroughASave`), and the Peepo side in `PeepoIntegrationClientTests` and
  `CompanionAutomationChecks` ([logistics](arcane-concordance-logistics.md)).
- Audits: `check_crucible_stand` and `check_crucible_supply` (new); `check_alchemy` mirrors the stand's targets and
  rates. The checks run for the unit, and its audit mutations, are in the stand's and the logistics records.
- **Not run:** play by hand and two clients.

**Concordance Chunk C, unit 5** ([C-10](concordance-chunk-c.md#unit-5-evidence-and-handoff-c-10); not merged).
`ConcordanceWorkshopClientGameTests` shows, in one real client on a dedicated server, a crucible on a Crucible Stand
following the Stillroot Poultice's formula with water from an Electric Pump, its Inspect page naming the stand, and the
batch bottled as two poultices in two bowls; it passed in client shard 4 of run 38774859294. The viewed Inspect shot
reads "Crucible Stand beneath heats it towards 65 degrees" and "Formula: step 3 of 9: Mendvetch, as it comes". The
load card measures a crucible replaying the poultice on a stand (21 poultices while measured, every item accounted
for) and the journey test brews both products by hand and the poultice powered; both passed in that run.

## World and event applicability

No worldgen, creatures, loot or seasons.

## Rollout and open questions

- New stable ids: block and item `jugcraft:crucible` and its block entity; items `jugcraft:mortar`,
  `jugcraft:stirring_rod`, `jugcraft:sampling_spoon`, `jugcraft:assay_glass`, `jugcraft:formula`, `jugcraft:reagent`,
  `jugcraft:draught`, `jugcraft:salve`; item components `jugcraft:reagent`, `jugcraft:brew`, `jugcraft:formula`; research
  `jugcraft:alembic_arts`; preparations `jugcraft:raw`, `jugcraft:ground`; eleven ingredients (fifteen since step 14
  added the garden's four) and seven properties; item
  tag `jugcraft:alchemy_specimens`; the three sounds above.
- `jugcraft:concordance_specimens` (added in step 12) now also includes `#jugcraft:alchemy_specimens`.
- Save compatibility: additive. The formula text format (`water`, `add`, `stir`) is part of the saved data and must
  stay readable.
- Since Chunk B, unit 4: the Rime property's status changed from `minecraft:resistance` to `jugcraft:frostward` (a new
  stable id). Bottles keep the status they were bottled with, so old Rime draughts still give Resistance; formulas are
  operations only and replay under the new rule (a warm, hot or searing Rime stir now brews weaker or nothing). A
  crucible whose output still holds old Resistance draughts waits for them to be taken, because a Frostward draught
  does not stack with them.
- Since Concordance Chunk C, unit 1 (not merged): the products `jugcraft:stillroot_poultice`, `jugcraft:kiln_draught`
  and `jugcraft:winter_brine` (data under `concordance/product/`, ids stable once released); a property's `fouling`
  field; the diagnostic codes' ids (`Code`, stable: never renamed, reused or removed; new ones are appended); lang keys
  `preparation.jugcraft.*`, `compose.jugcraft.diagnostic.*`, `compose.jugcraft.alchemy.inspect.*`, `.risk.*`,
  `.op.*`, `.fouled`, `.assay.fouls`, and the products' item names (`item.jugcraft.salve.stillroot_poultice`,
  `item.jugcraft.draught.kiln_draught`, `item.jugcraft.draught.winter_brine`). No new saved key and no new item.
- Since Concordance Chunk C, unit 3 (not merged): the Crucible Stand's ids ([crucible-stand.md](crucible-stand.md));
  the crucible's optional saved keys `formula_owner` and `request` (C-06), read with defaults, the stamp still 1.
- Open: more preparations (distilling, calcining) and ingredients arrive with later Principles; a GuiLib crucible
  screen. For now the encyclopedia's Inspect page stands in for a crucible screen (plan 08:55-59).
