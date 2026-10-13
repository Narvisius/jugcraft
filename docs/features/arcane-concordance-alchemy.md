# Arcane Concordance: experimental alchemy

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
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

**Preparations are explicit transformations.** As it comes (`raw`), an ingredient goes in undissolved: all of it waits
to be stirred out. **Ground** in a Mortar and Pestle (`ground`: hold the mortar, the ingredient in the other hand) it
keeps 90% of its properties, and 60% of that dissolves the moment it goes in. The kitchen uses the same mortar: a Dried
Chili in the other hand grinds into two Paprika ([vegetables-herbs-and-spices.md](vegetables-herbs-and-spices.md)).

**The crucible.** Place it over a heat source. Its temperature moves one degree a tick towards the source's: nothing
20, magma 60, soul campfire 100, campfire 120, soul fire 150, fire 175, lava 220 (a campfire only while lit). Four
bands decide what a **stir** (a Stirring Rod or a stick, at most one every half second) does:

| Band | From | A stir dissolves of what is pending | Also |
|---|---|---|---|
| Cold | | 10% | |
| Warm | 40 | 30% | |
| Hot | 90 | 55% | |
| Searing | 160 | 80% | keeps only 85% of the dissolved Radiance and Verdance, and adds 0.50 contaminant |

Put in water first (a water bottle is one part, a water bucket three; at most six parts), then ingredients, then stir.
**Bottling** draws one part's share of everything dissolved: that is the dose. A glass bottle makes a **draught**
(drink it), a bowl a **salve** (use it on a creature, or on yourself while sneaking).

**What a dose does** depends only on how much of each property a part holds:

| Property | Effect | Threshold | Stronger by | Highest level | Duration per unit (cap) |
|---|---|---|---|---|---|
| Radiance | Night Vision | 0.50 | | I | 60 s (120 s) |
| Verdance | Regeneration | 0.60 | 1.50 | II | 15 s (22.5 s) |
| Ember | Fire Resistance | 0.60 | | I | 60 s (120 s) |
| Rime | Resistance | 0.80 | | I | 30 s (60 s) |
| Tide | Water Breathing | 0.60 | | I | 60 s (120 s) |
| Hollow | Poison | 0.40 | 1.00 | II | 10 s (21.6 s) |
| Contaminant | Nausea | 0.50 | | I | 8 s (20 s) |

Below its threshold a property does nothing, and the explanation says so.

**A worked example** (the one the game tests use). One water bottle, one glowstone dust as it comes (1.50 Radiance
pending), the crucible over a campfire (hot). The first stir dissolves 55%: 0.825. The second dissolves 55% of the 0.675
left: 0.371 (amounts are kept in thousandths and rounded down). Bottled, the draught carries 1.196 Radiance, over the
0.50 threshold: Night Vision I for 1.196 × 60 = 71.75 seconds. Stirred once more, or with a second glowstone dust, it
would last longer; stirred over fire (searing), it would lose 15% of its Radiance and gain contaminant.

**Sampling gives increasing information.**

- **Sampling Spoon** (anyone): the band, the volume, the strongest property and whether it is murky.
- **Assay Glass** (the Alembic Arts understood): the temperature, every property dissolved and still pending, a part's
  share and the contaminant. Without the research it reads like a spoon.
- **Assay Glass, mastered**: what a dose bottled now would do, and why: each effect with its property, how much a part
  holds against the threshold, and which ingredients (or searing stirs) put it there.

**Formulas.** Use a blank formula on a crucible whose mixture is fresh (nothing bottled yet) and would do something: it
writes down the whole process, for example `water 1; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot`.
Use the written formula on any crucible to set it (sneak with an empty hand to clear it). Fed water through a pipe and
ingredients (or reagents) through a hopper into its five buffer slots, the crucible follows the formula step by step,
once a second: it takes exactly the ingredient and preparation the step names, waits until its temperature is in the
band the step names before stirring, then bottles into its output while it has bottles or bowls. It never guesses or
substitutes; it waits. The same process gives the same result, so the repeat makes exactly the same draught.

## How it works

**Data** (`data/<ns>/concordance/ingredient/`, `/preparation/`, `/property/`, read by `AlchemyCatalog` in
`concordance/alchemy`): an ingredient names its item, its properties and contaminant; a preparation its tool, scale and
the share ready at once (exactly one has no tool: `raw`); a property its axis, status effect, intent, threshold, level
step, highest level, ticks per unit and duration cap, within the shared effect limits. The loader refuses anything
unknown or out of range, with a reason.

**The simulation is pure and deterministic** (`Mixture`, `Operation`, `Band`, `Heat`, `Outcome`, `Formula`, `Assay`).
A mixture is its volume in parts, its pending and dissolved vectors, its contaminant, the doses drawn and its history
of operations (at most 32). All amounts are whole thousandths (`Vector`, longs) with floor division, so identical
operations always give identical mixtures and doses on every machine. `Operation` is one of `water <n>`,
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

## Connections

- Input producer: First Light (the Alembic Arts needs it understood); vanilla ingredients from farming, the Nether, the
  ocean and caves; heat from any campfire, fire, magma or lava.
- Output consumer: players and creatures (vanilla status effects through the shared executor).
- Technology connection: water through the Fabric Transfer API (Jugcraft pipes or any mod's), hoppers for ingredients and
  bottles, the output for any extractor. A formula is canonical process data any automation can carry.
- Magic connection: the six axes are the Principles' own names; the same research, notes and practice rules as the rest
  of the Concordance; the shared effect boundary.
- Reachable entry path: First Light understood → the Alembic Arts by examining vanilla items → craft the crucible
  (four copper, a cauldron, three bricks) and tools from vanilla materials.
- Solo, trade and cooperative routes: entirely solo; Research Notes and written formulas can be traded.
- Specialty use without other branches: needs only First Light.

## Balance

- Every effect is a vanilla status effect at level I or II, and **no brew outlasts the vanilla potion of the same effect
  at the highest level it can reach**: night vision, fire resistance and water breathing at most 2 minutes (vanilla: 3);
  Regeneration (which reaches II) at most 22.5 seconds and Poison (which reaches II) at most 21.6 seconds, vanilla's
  strong-potion durations, even at level I. Resistance I (at most 1 minute) and Nausea have no vanilla potion.
  `tools/check_mod_data.py` enforces this. Brewing is cheaper than vanilla brewing (no blaze powder, nether wart or
  brewing stand), so it is held to vanilla's ceilings rather than allowed to exceed them.
- Nothing alchemy makes is a resource another system consumes, so there is no conversion loop.
- Searing heat trades quality for speed (85% of Radiance and Verdance kept and contaminant added), so the fastest fire
  is not always the best.
- Grinding loses a tenth for speed: a ground ingredient needs fewer stirs.
- A crucible holds at most six parts: six doses per fill.

## Multiplayer and persistence

- Server authority: the crucible simulates on the server; clients receive the temperature and volume only (for the
  vessel's animation), never the makeup, which takes sampling. Hand operations are rate-limited and check research; a
  stir's band is the server's.
- Persistence: the crucible saves its mixture exactly (to the thousandth), history, formula, step, tank and slots;
  broken, its items drop (vanilla's container behaviour) and the mixture spills. Items carry `jugcraft:brew`,
  `jugcraft:reagent` and `jugcraft:formula`.
- Disable behaviour: with `concordance.enabled=false`, crucibles refuse work and stop following formulas; everything stays
  registered.

## Dependencies and assets

No new dependency. Framework use:

- **GeckoLib**: the crucible is a GeckoLib block entity; its controller plays `empty`, `still` or `simmer` (the liquid
  swells and the ladle turns) from the synced volume and band.
- **Texture animation**: the liquid surface is a four-frame animated sheet (`crucible.png.mcmeta`), which GeckoLib uses
  for the geo model.
- **Jade** (optional): temperature and band, volume, and when following a formula its step and the next operation it
  waits for.
- **JEI** (optional): each ingredient's properties and the mortar's preparation as ingredient information, and the tools'
  crafting recipes. JEI never shows outcomes: those are for the player to work out (or the mastered Assay Glass).
- **Modonomicon**: a new **Alembic** codex category (the Alembic Arts, the crucible, properties, sampling and formulas),
  generated from the same tables as the data.
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
- The pure core compiles with JDK 21. The step 13 harness passes **61 checks** against the generated data, including:
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

## World and event applicability

No worldgen, creatures, loot or seasons.

## Rollout and open questions

- New stable ids: block and item `jugcraft:crucible` and its block entity; items `jugcraft:mortar`,
  `jugcraft:stirring_rod`, `jugcraft:sampling_spoon`, `jugcraft:assay_glass`, `jugcraft:formula`, `jugcraft:reagent`,
  `jugcraft:draught`, `jugcraft:salve`; item components `jugcraft:reagent`, `jugcraft:brew`, `jugcraft:formula`; research
  `jugcraft:alembic_arts`; preparations `jugcraft:raw`, `jugcraft:ground`; eleven ingredients and seven properties; item
  tag `jugcraft:alchemy_specimens`; the three sounds above.
- `jugcraft:concordance_specimens` (added in step 12) now also includes `#jugcraft:alchemy_specimens`.
- Save compatibility: additive. The formula text format (`water`, `add`, `stir`) is part of the saved data and must
  stay readable.
- Open: more preparations (distilling, calcining) and ingredients arrive with later Principles; a GuiLib crucible
  screen.
