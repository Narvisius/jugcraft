# Arcane Concordance: ecological cultivation and living devices

Status: merged to main ([#244](https://github.com/jimbozoomer-byte/jugcraft/pull/244), from branch
`claude/awesome-davinci-iwv3b9`, integrated by [#263](https://github.com/jimbozoomer-byte/jugcraft/pull/263) on
8 October 2026); see Verification for what has run. Concordance Chunk C's unit 4 (habitat inspection, the canteen on
beds, the compost page, the mixed bed, harvest strain and the Dewlit graft) is built for
`claude/chunk-c-living-workshop`, not merged; its tests passed in CI (run 38726242149, with unit 3, and again in run
38774859294; [contract](concordance-chunk-c.md)).
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 14) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 6, Practitioner stage (Jugcraft Workshops). Roadmap step 14.
Primary specialty and supported player role: Greenwardens. A garden is something to understand and tend: each crop
has a niche, a bed says what it lacks, and a working garden runs on the sun, water and its own declared sources.

Builds on [research and notes](arcane-concordance-sharing.md), [typed resources](arcane-concordance-sharing.md) and
[rituals](arcane-concordance-rituals.md) (the Ley Pylon). Extends Jugcraft's own farming: the same random ticks, the
sprinkler, fertilizer and the legume rule. Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

**Verdant Husbandry** (new research; needs First Light understood). Examine a moss block, a sunflower, a brown mushroom,
a lily pad or a fern to encounter it; three of them to observe it; study one at a Lampwright's Bench, or read someone's
Research Notes, to understand it. Harvesting each of the four crops by hand masters it.

**Verdant Beds** are soil for the Greenwardens' crops (crafted from dirt, moss and bone meal). A bed placed by someone
who has not understood Verdant Husbandry is **dormant**: nothing grows in it until a Greenwarden's empty hand wakes it,
and with it every dormant bed joined to it (up to 64). A bed holds water like farmland (water within four blocks, rain
or a wet sprinkler keep it at 7, and since Concordance Chunk C a Gourd Canteen's sip wets a dry one to 7 where you may
build) and up to **32 nutrients**. With Fusion, rows of beds join into one long bed. Since Chunk C an item in your main
hand is that item's own use on a bed (a seed plants, and fertilizer works on the crops round it, without sneaking); an
empty hand reads it.

**Five factors** decide how a crop grows:

| Factor | Range | Where it comes from |
|---|---|---|
| Moisture | 0 to 7 | the bed's own wetness; each step of growth dries it by one |
| Light | 0 to 15 | the server's light at the plant (sun or lamps), never the time of day, a dynamic light or a shader |
| Nutrients | 0 to 32 | what the bed holds; growth spends them |
| Diversity | 1 to 8 | the different plants (crops, flowers, saplings) in the 5x5 round the plant, itself included |
| Disturbance | 0 to 15 | working magic round it: circle anchor 3; Ley Pylon, crucible and Verdant Heart 2; Lumen Sconce, Kindled light, Warding Stone and amethyst 1 |

**Four crops** (each crafted from vanilla plants and an amethyst shard; then they propagate):

| Crop | Its niche | A step | Harvest |
|---|---|---|---|
| Sunpetal | sun 11+ (best 13+), moisture 2+ (best 4+), nutrients 1+ (best 3+), best among 3+ kinds, disturbance at most 5 (best 2) | costs 1 nutrient | 2 sunpetals, 1 chaff |
| Dewmoss | moisture 5+ (best 7), shade at most 12 (best 9 or less), nutrients 1+ (best 2+) | costs 1 | 2 dewmoss, 1 chaff |
| Gloamcap | dark at most 9 (best 6 or less), moisture 2+, nutrients 2+ (best 4+), disturbance 1+ (best 3 to 10) | costs 2 | 1 gloamcap, 1 chaff |
| Mendvetch | light 8+ (best 11+), moisture 1+, disturbance at most 6 (best 3) | gives 1 to the poorest bed round it | 1 mendvetch, 1 chaff |

A crop grows at full pace while every factor is in its best range, at **half pace** while some are only tolerable, and
**not at all** while any is outside. Use an empty hand on a crop (or look at it with Jade) to see its step, its pace and
each factor holding it back: "Light 6: too little (needs at least 11)". A ripe crop (step 3) is harvested with an empty
hand: its produce and Verdant Chaff, and it falls back to step 1 to grow again. A Mendvetch beside other crops also
speeds them, as Jugcraft's legumes do.

**Inspecting a garden** (Concordance Chunk C, C-07; not merged, like the three paragraphs after it). Open the
encyclopedia while looking at a Verdant Bed, or a crop on one, within your reach, and its Inspect page shows the bed's
moisture and nutrients, the crop's step and pace, the verdict growth would reach on this reading, and each factor
against the plant's range (least, best from, best to, most), naming how far its reading reaches: moisture is the bed's
own (water within 4 blocks, rain, a wet sprinkler or a canteen wets it); light is shown as sky and lamps, the greater
counting; diversity counts the kinds of plant within 2 blocks at the plant's height; disturbance is the working magic
in the 5 by 5 round the plant, a block above and below, plus the strain recent harvests count against its limit. A bed
with nothing on it gives its readings unjudged; a dormant bed says so and is not read; the page changes nothing. Jade
on a ripe crop shows its bed's last verdict ("As it grew: ..."), and a bed forgets its verdict on its next random tick
once neither a crop nor a Verdant Heart stands on it, and at once when another crop is set on it or it is grafted.

**A mixed bed** (C-08). A Dewmoss with a Gloamcap growing in the 5 by 5 round it at plant height, or a Gloamcap with a
Dewmoss, steps 25% more often. Only the other kind counts, never copies. It is speed only: a step costs what it always
did, so a mixed bed makes no nutrient, and a Mendvetch (a fixer) neither takes nor gives the bonus. Reports, Jade and
the Inspect page say "Mixed bed: +25%".

**Harvest strain** (C-08).
- Every harvest a bed sees strains it by one, whatever took the ripe crop: an empty hand, a Gleaner, a sickle, a reap
  (an arm weapon's or the harvesting spell's), the Crop Harvester, or breaking it (each in a game test, passed in CI),
  and, counted the same way but not tested, a Peepo, a Hay Golem or a crow raid. A crop placed ripe never strains its
  bed.
- Up to 3, counted only against a plant's disturbance limit, never as the magic it needs: a Sunpetal on a quiet bed
  still thrives at 1 or 2 and grows at half pace at 3 (sooner where the area already disturbs it); a Mendvetch thrives
  up to 3; a Dewmoss does not care; a Gloamcap still needs its magic.
- It eases by rest (one a bed random tick while no crop grows on the bed: fallow, ripe, or stalled or dormant), by
  compost (each dose of bone meal on the bed, bone meal or fertilizer on its growing crop, or a Mulch Maw's nutrient)
  and by a Greenwarden's empty hand on a bed they may change, or on its growing crop, once every 30 seconds a bed. A
  Mendvetch's gift is not compost.
- It never kills (a crop stalled by strain lets its bed rest, and grows again) and never reads industrial work or
  pollution. Reports and Jade say "Recently harvested: strain n of 3", a harvest not yet counted included.

**Grafting** (C-09). A Greenwarden (Verdant Husbandry understood) who uses a Dewmoss on a Sunpetal crop, ripe or
growing, in the main hand and not sneaking, spends the Dewmoss and grafts **Dewlit** onto the bed beneath. There a
Sunpetal bears shade, light 9+ (best 11+) for 11+ (best 13+), but wants a wetter bed, moisture 4+ (best 5+) for 2+
(best 4+). The graft lasts through every harvest, replanting, save and restart, until a crop of another organism grows
there. A bed carries one: a second is refused, as is a graft where the gardener may not change the crop or the bed, and
a refusal spends nothing. Produce, cost, chaff and alchemy are unchanged, so a graft makes nothing. The reports, the
Inspect page and Jade name it, on a crop only when the crop is its host.

**Four living devices:**

- **Verdant Heart** (the producer), set on a bed: every 10 seconds, in its niche (light, a fed bed, at least two other
  kinds of plant round it, quiet magic), it spends 1 nutrient and makes **2 Verdance** (1 while a factor is only
  tolerable), up to 64. Beside a Ley Pylon it pours whole batches into it, 3 Verdance for 2 Ley Charge, so a garden can
  power a circle. It beats faster on screen while the garden thrives.
- **Mulch Maw** (the consumer): feed it plant matter by hand or hopper. Every 2 seconds it eats one and, for every 4
  quarters of a nutrient, feeds the poorest bed within 3 blocks. Seeds, leaves and the crops are a quarter; chaff, vines
  and cane a half; fruit and roots three quarters; bread and hay a whole nutrient. With every bed full it stops eating.
- **Habitat Gauge** (the sensor), set on a bed: a comparator reads it. An empty hand chooses what it reads (a factor,
  scaled to 0 to 15, or suitability); a crop used on it makes it judge the habitat for that crop: 15 thriving, 8 slow,
  0 not at all. Its bulb opens while it gives a signal.
- **Gleaner** (the collector): every 2 seconds it harvests the nearest ripe crop within 3 blocks into its nine slots
  (hoppers take them out), for 1 Verdance a harvest, drawn up to 8 at a time from a Verdant Heart within 4 blocks that
  its keeper's party may draw from.

Each device placed by a Greenwarden is awake at once; otherwise a Greenwarden's empty hand wakes it. Each says what it
is doing: working, idle, dormant, stalled (and why), starved, full, or waiting for Verdance.

## How it works

**Data** (`data/<ns>/concordance/organism/`, `/disturbance/`, read by `EcologyCatalog` in `concordance/ecology`): an
organism's role (`crop` or `producer`), block, item, growth time, cost or fix, replant step, yields and niche (a range
`[least, ideal from, ideal to, most]` per factor); a disturbance source's blocks (ids or `#tags`) and value. The loader
refuses an unknown factor, a range out of order, a niche whose nutrient floor is below its cost, a crop that both costs
and fixes, and two organisms on one block or item. Since Concordance Chunk C a crop may declare `neighbours`
(`{"organisms": [ids], "percent": n}`, 1 to 50: other crops that cost nutrients, never itself, and never on or naming a
fixer; the rules report a bad neighbour), and `concordance/graft/<id>.json` holds a graft: `schema`, `host`, `donor`
and `niche` (only the ranges it changes) and nothing else. The rules refuse a graft that does not join two different
loaded crops, puts its host's nutrient floor below its cost, or repeats a host and donor.

**The habitat is read within fixed bounds** (`Sampler`): one sample reads exactly 100 positions (the 5x5 at the plant's
height for kinds, and the 5x5x3 round it for disturbance), never in a chunk that is not loaded. A bed keeps its sample
for 200 ticks; a level takes at most **16 samples a tick** (`SampleBudget`), and a bed refused one keeps its last and
asks again. Moisture and nutrients are the bed's own and light is read at the plant each time (one read).

**Growth uses Jugcraft's farming rules** (`OrganismCropBlock`, a vanilla `CropBlock`): on a random tick (vanilla's, or a
sprinkler's, which gives every crop in reach one) the crop reads its habitat, and if the niche allows, takes a step with
the chance `CropGrowth` gives any Jugcraft crop (the soil round it, a legume beside it, the Harvest Moon) scaled by its
pace. The step takes its cost from the bed exactly (`VerdantBedBlockEntity.take`) or, for a fixer, gives its nutrients
one at a time to the poorest awake bed of its own and its four neighbours (`Nourishment.poorest`, first on a tie).
Bone meal and fertilizer on a crop feed its bed (`performBonemeal`) instead of forcing a step. Since Concordance Chunk C
(C-08) a plain use of either on a growing crop does so (before, the crop reported instead): fertilizer by its own area
rule, bone meal by the bed's own rule (rate-limited, for someone who may change the bed); each dose that feeds a bed is
compost.

**Since Concordance Chunk C** a growth attempt first clears the bed's graft if it carries one for another organism
(before the dormant check), then counts a harvest the bed saw (`VerdantBedBlockEntity.settle`: the bed noted `ripened`
when growth took its crop to the last step, and the crop no longer stands ripe). The step is judged on the niche the bed
gives (`VerdantBedBlockEntity.niche`: the graft's ranges when it carries one for this organism); growth, both reports,
the bed's note, the Habitat Gauge and the Inspect page ask it, and the Verdant Heart reads its own. `Verdict.of` judges
disturbance as the worse of the area alone and the area with the strain. A mixed bed multiplies the chance of a step
(`OrganismCropBlock.speed`; the kinds come from the sample's own set, with no new read), never its cost.

**The devices** share `LivingDeviceBlockEntity` (keeper, awake, status word, a staggered pulse) and act only on their
pulse: the Heart every 200 ticks, the Maw, Gauge and Gleaner every 40. The Heart's Verdance is a typed
`Reservoir` of `essence/verdance`; the Gleaner draws it through `Transfers.move` with both devices' `Ownership` (the
keeper's party), so a stranger's Gleaner is refused. The Heart pours into a pylon through the data conversion
`jugcraft:verdance_to_ley`.

**Practice.** Harvesting a ripe crop by hand records `Evidence.Practiced("jugcraft:cultivation", <crop>)`; all four
crops master Verdant Husbandry.

## Connections

- Input producer: First Light (Verdant Husbandry needs it understood); vanilla plants for the first seeds; sun, water,
  rain, Jugcraft's sprinkler and canteen; bone meal; Jugcraft's superphosphate fertilizer and bat guano; any plant matter
  for the Maw.
- Output consumer: the four crops are **alchemy ingredients** (Sunpetal Radiance and Verdance; Dewmoss Tide; Gloamcap
  Hollow and Rime; Mendvetch Verdance); Verdance feeds Ley Pylons and so rituals; produce and chaff feed the Maw.
- Technology connection: hoppers and pipes feed the Maw and empty the Gleaner; the sprinkler's pulses and wetness reach
  the crops and beds; a comparator reads the gauge.
- Magic connection: Verdance is the Concordance's typed essence and moves only by the shared transfer rules; working
  magic (circles, pylons, crucibles, lights) disturbs the habitats round it.
- Reachable entry path: First Light understood → Verdant Husbandry by examining vanilla plants → craft beds and the
  first crops from vanilla materials.
- Solo, trade and cooperative routes: entirely solo; crops, beds and devices can be traded, and a non-Greenwarden can
  run a garden a Greenwarden woke; a party shares its Hearts with its Gleaners.
- Specialty use without other branches: needs only First Light.
- Unlocks: nothing new in Concordance Chunk C; the mixed bed, strain and the graft change how a garden grows, not what
  can be made.
- Since Concordance Chunk C (not merged): Dewmoss is an input of grafting (one spent a graft), whose output is the bed's
  trait, Dewlit; the Inspect page is a new reader of beds and crops; the canteen wets beds; compost (bone meal,
  fertilizer, the Maw's nutrient) also eases strain.

## Balance

- **Every nutrient has a declared source**: the Mendvetch (fixed from the air, one per step), the Maw (plant matter, at a
  loss), bone meal and fertilizer (2 a dose). Sun and water are external, as for any crop. Nothing else makes nutrients.
- **No loop through the Maw**: composting a harvest never returns what regrowing it cost (a Sunpetal's two steps cost 8
  quarters; its harvest composts to 4). `tools/check_mod_data.py` checks this for every crop.
- **Depleted inputs constrain output**: an empty bed stalls every crop with a cost, and a starved Heart makes nothing.
  The step 14 harness simulates a row of eight beds for 2000 rounds: with Mendvetch (and optionally a Maw) it keeps
  yielding; with neither, its starting nutrients run out and it stops.
- **Vanilla composters** take the crops as they take seeds (a 30% chance of a layer) and chaff at 50%: a harvest
  composts to about a sixth of a bone meal (a third of a nutrient), far less than the two it cost to regrow.
- Verdance into Ley Charge loses a third (3 for 2), and nothing turns back into Verdance.
- The Gleaner costs a Verdance a harvest, so automation draws on the garden's own producer.
- **Concordance Chunk C:** the mixed bed mints nothing: it changes the chance of a step, never its cost, and a fixer
  neither takes nor gives it (`check_ecology`). A graft is a sink of one Dewmoss and changes only the niche; a grafted
  Sunpetal and a Dewmoss share light 9 to 12 on a wet bed.
- **Strain, by arithmetic and not measured:** a bed's random tick comes about every 1,365 ticks (4,096 / 3), and a
  Sunpetal on a wet bed among wet beds regrows in about 16,400 (`CropGrowth`'s chance: about 6 random ticks a step).
  Resting one a random tick whatever grew would undo a strain long before the next harvest, which is why a bed rests
  only while no crop grows on it. A Gleaner-run Sunpetal bed with no compost settles at strain 3 (half pace); compost
  or tending keeps it lower.
- **Simulation budget**: crops cost only on random ticks; a sample is 100 reads, reused for 10 seconds, at most 16 a
  level a tick; devices pulse every 2 or 10 seconds, staggered; the Maw scans 147 positions a pulse (7 by 7, a block
  above and below), and the Gleaner 147 for ripe crops and, while short of Verdance, 729 for Verdant Hearts (9 by 9 by
  9, `GleanerBlockEntity.draw`). This record said 147 for both until Concordance Chunk C, which measured the Heart
  search (C-10, C-P21, C-P68): a starved pulse took 333 µs in run 38774859294's `mod` job (the position list alone 304
  µs), over the proposed 250 µs line, so a change is proposed and not applied (caching `Garden.nearest`'s offset
  order, or a vertical reach of ±1); `HEART_REACH` stays 4 until the owner decides. Since Chunk C a bed's random tick reads the block above it at most three
  times more (C-07's forgetting, C-08's rest); a sample still reads 100 positions; a graft adds catalogue lookups and
  no block read. No device ticks work every tick, loads a chunk or reads outside its reach.

## Multiplayer and persistence

- Server authority: growth, sampling, nutrients, Verdance and harvests are the server's. Clients receive a bed's
  nutrients, whether it is awake and its crop's last verdict (for the tooltip), and each device's status and store (for
  its animation and Jade); nothing a client sends changes them. Uses are rate-limited and check build permission (spawn
  protection, protected towns); a Gleaner never harvests in a protected town.
- Ownership: a bed and a device remember who placed them; a Gauge's settings change only for its keeper's party (or an
  operator); a Heart lends Verdance only to its keeper's party.
- Persistence: a bed saves its nutrients, whether it is awake and its keeper (a broken bed loses its nutrients); a Heart
  its Verdance; a Maw its food and digested quarters; a Gleaner its slots and Verdance; a Gauge its mode, crop and signal.
  Area samples are not saved (the first reading after a load samples again).
- Disable behaviour: with `concordance.enabled=false`, crops do not grow and devices stop (status "switched off"),
  and everything stays registered.
- Since Concordance Chunk C (not merged): clients are also sent a bed's strain and whether its crop has ripened since,
  and, only while it carries a graft, the graft and the organism it is for; never the keeper. A bed saves `strain`,
  `ripened` and `graft` only when set (never the graft's host); older saves read 0, false and none, and the bed's
  version stays 1. When a bed was last tended is not saved, so a restart allows one early tending. The Inspect page
  reads on the server, within the reader's block reach, and changes nothing. A canteen's sip, tending and grafting ask
  `Authority` (grafting at the crop and at its bed); bone meal on a crop asks what bone meal on its bed asks. Switched
  off, no strain changes and nothing grafts; a canteen still wets a bed, as water nearby does.

## Dependencies and assets

No new dependency. Framework use:

- **GeckoLib**: the Verdant Heart (beats, faster while thriving), Mulch Maw (chews while eating) and Gleaner (reaches while
  harvesting) are GeckoLib block entities animated from the status the server sends.
- **Fusion** (optional): the built-in pack joins neighbouring Verdant Beds' rims (dry and wet).
- **Jade** (optional): a bed's nutrients, habitat and its crop's verdict; a crop's step and the factors holding it back
  (from its bed's synced verdict); each device's status and store. Without Jade, an empty hand says the same in chat.
  Since Concordance Chunk C: a ripe crop's last verdict ("As it grew"), a bed's strain (on the bed and its crop), a
  mixed bed's line, and a graft (on the bed, and on its crop when the crop is the graft's host).
- **SmartBrainLib**: not used. None of these organisms moves or decides anything: they are sessile crops and devices,
  so a brain would add nothing. Spirits, familiars and constructs (step 17) are where it applies.
- **Modonomicon**: a new **Garden** codex category (Verdant Husbandry, habitats, organisms, living devices). Since
  Concordance Chunk C the habitats entry names the Inspect page, and the organisms entry adds "Mixed Beds and Strain"
  and, once Verdant Husbandry is understood, "Grafting".
- **JEI** (optional): the four crops' alchemy properties appear with the other ingredients; since Concordance Chunk C
  everything a Mulch Maw eats has an information page with its worth in quarters (`recipe_view.json`
  `garden_compost`; the default data's worths over the client's tags). Not yet seen in a client.

Art: nine 16x16 item icon maps in `tools/item_icons/` (sunpetal, dewmoss, gloamcap, mendvetch, verdant_chaff,
verdant_heart, mulch_maw, gleaner, habitat_gauge), with new icon materials (leaf, moss, gloam, stem, petal, straw,
terracotta, dew) in `tools/icon_materials.py`; block textures, crop steps and GeckoLib sheets from
`tools/concordance_ecology_art.py`. **Provenance:** the bed's soil adapts the owner's library
`art/owner-library/originals/Blocks/farming and food textures/rich_soil.png`, `rich_soil_farmland.png`,
`rich_soil_farmland_moist.png` and `rich_soil_farmland_moist_side.png` (hue shifted towards moss, moss flecks added, a
wooden rim drawn); the Gloamcap's four steps adapt `brown_mushroom_colony_stage0..3.png` from the same folder (caps
recoloured violet). The library files are read, never changed. Everything else is drawn from code. The crop colours
and icons have not been shown to the owner yet.

## Verification

- `python3 tools/check_mod_data.py` passes. New step 14 checks (`check_ecology`): the Java factors, sampling bounds,
  budget and device numbers equal the generator's; every organism and disturbance names registered blocks and items,
  every crop has a recipe, step textures and a loot table, and its niche covers its cost; composting a harvest never
  returns what regrowing it cost; every word the garden says has its text; the nine icons are their maps; the three
  GeckoLib models' animations, bones and UV regions agree with their sheets. The pure-package rule now covers
  `concordance/ecology`.
- `python3 scripts/check_repository.py` and `python3 tools/check_icon_maps.py` pass.
- The pure core compiles with JDK 21. The step 14 harness passes **52 checks** against the generated data (the harness
  is not in Git: `src/test` holds only `VanillaReferences.java`, so these checks cannot be run again from the
  repository): range edges;
  each crop's verdicts (a sunpetal thrives in sun and company, tolerates alone, stalls in shade and says light 9 against
  11; dewmoss stalls in sun; a gloamcap needs magic near it; the vetch grows on an empty bed but not beside loud magic);
  a sample reads exactly 100 positions and caps diversity and disturbance; the budget gives 16 a tick and renews;
  nourishment's tie rule and full beds; the Maw's holding limit; the Heart's four beat outcomes; the parser's refusals;
  and the reference-garden simulation above.
- Game tests: `ConcordanceGardenGameTests`, twelve before Concordance Chunk C: step 14 added ten (this record said
  nine), roadmap step 28 `theGleanerWaitsForItsKeeper` and step 29 `aHastenedStepPaysAndFixesNothing`. Step 14's: a
  Greenwarden wakes the beds (a novice's bed is dormant and grows nothing; one touch wakes the three joined beds and not
  a fourth); a crop grows only within its niche (lit by glowstone it grows a step, paying a nutrient and drying its bed;
  shut in stone it stalls, and its bed records "too little light, needs 11"); an empty bed stops growth (bone meal feeds
  it; fertilizer feeds the bed two doses and never forces a step); a fixer feeds the poorest bed; the Heart beats 1
  nutrient into 2 Verdance among three kinds of crop, pours 3 for 2 into a pylon, and stops on a starved bed; the Maw
  feeds the poorest bed and stops eating when every bed is full; the Gauge reads moisture and judges an attuned crop;
  the Gleaner harvests a ripe sunpetal for a Verdance from a shared Heart, and a stranger's Gleaner may not draw; twenty
  beds in one tick get at most 16 samples and the rest are read later; a bed and a Heart keep what they hold through a
  save.
- CI: the first pushes failed to compile (`yield` is a restricted name; 26.3 has no `PushReaction.BLOCK`/`DESTROY` and
  no four-argument `withinManhattan`), then the server failed to start (the crops copied wheat's properties, whose map
  colour reads wheat's own age); each was fixed. Build run 37605176553 (commit `038153ea`) passed: compilation, the
  data checks, every required server game test (including the ten garden tests) with and without the optional
  integrations, and the three client test shards.

**Concordance Chunk C, unit 4 (C-07 to C-09; commits `5ba973632`, `d32ac3a61` and `d6edee4f4`, records in the commit
after them, and the unit review's fixes in `ce04da475`).** Written with no Minecraft jar; its first CI run was with
unit 3, in the merge of units 3 and 4 (`1c7dc577c`): **every job of run 38726242149 passed**, the tests below among
them (the client test by its shard; its shots not viewed). Every job passed again in runs 38731825181 and 38774859294.
- `ConcordanceGardenGameTests`, 21 new (33 in all):
  - C-07: `aBedSaysWhatItsPlantNeedsAndHowFar`, `aVerdictClearsWhenThePlantGoes`, `aCanteenWatersABed`;
  - C-08: `aMixedBedGrowsFasterOnlyForAnotherKind`, `theBonusSpendsNoExtraNutrientAndMintsNone`,
    `everyHarvestPathStrainsTheBedOnce` (a hand, a Gleaner, a sneaking sickle, a reap, breaking and planting again),
    `theCropHarvesterStrainsTheBedOnce` (it passes at once when `JUGCRAFT_TEST_FOCUS` leaves out `machines`),
    `fixingDoesNotEaseStrain`, `restCompostAndTendingEaseStrain`, `strainNeverSuppliesDisturbance`,
    `strainNeverKillsAndNeverReadsPollution` and `strainSurvivesASave`; `samplingStaysWithinTheBudget` extended (still
    100 reads, and the 25 kinds it already read);
  - C-09: `aGraftTakesOnceAndConsumesItsDonor`, `aSneakingUseNeitherGraftsNorPlaces`,
    `aDonorOnARipeCropGraftsAndAnyOtherItemStillHarvests`, `anUnresearchedGardenerCannotGraft`,
    `aGraftIsRefusedWhereTheGardenerMayNotBuild`, `aGraftSurvivesEveryHarvestPathAndASave`,
    `everyNicheReaderSeesTheGraft`, `aGraftChangesOnlyTheNiche` and `anotherPlantClearsTheGraft`.
- `EncyclopediaGameTests.theInspectPageReadsABedAndACrop` and `theInspectPageNamesAGraft` (23 in all).
- `ConcordancePersistenceGameTests.anOlderBedLoadsWithoutItsNewKeys` (a bed saved before strain and grafts reads
  unstrained, unripened and ungrafted, at version 1).
- `EncyclopediaClientGameTests.gardenInspection`: random ticks held at 0 while a Sunpetal is grown ripe, the
  client told the bed's strain and graft, and the shots `jugcraft_concordance_ripe_crop_jade` (only when Jade is
  loaded) and `jugcraft_encyclopedia_inspect_bed`.
- Unchanged, to run again because they harvest and regrow or use the canteen: `theGleanerHarvestsForVerdance`,
  `ConcordanceSpireGameTests.aVerdantFieldHastensOnlyWhatItsBedsPay`, `PeepoIntegrationClientTests` (its crop is
  placed ripe, so it strains nothing), `ConcordanceJourneyGameTests`, `ConcordanceWorkloadGameTests` and
  `HalloweenGameTests`; each passed in run 38731825181, which ran every client class (171).
- Run and passed for the unit's code commits: `python3 -B tools/generate_material_data.py`, a second run changing
  nothing. Run and passed for those commits and again for these records: `python3 -B tools/check_mod_data.py` (among it
  `check_ecology`'s C-07 and C-08 blocks, `check_graft`, `check_encyclopedia`'s garden reach block and read-only list,
  the client-read rule and `AUTHORITY_ROUTES`), `python3 scripts/check_repository.py`,
  `python3 tools/owner_art.py --check`, `python3 tools/concordance_journey.py --check`,
  `python3 tools/check_icon_maps.py`, `node --test tools/world-designer/model.test.cjs`,
  `python3 -m unittest discover -s scripts/tests` and `git diff --check`.
- The audit mutations, each applied in place and restored, re-run on the final tree for these records: C-07's 23,
  C-08's 28 and C-09's 32, every one reported.
- The pure ecology classes compiled with JDK 21 against stubs, and two scratch harnesses outside Git passed against
  the generated data: 41 checks of the mixed bed and strain, 28 of the graft.
- In CI only: `./gradlew build` (with and without the optional integrations) and `./gradlew runClientGameTest`. Not run:
  any client by hand.

**Concordance Chunk C, unit 5 (C-10; [the contract](concordance-chunk-c.md#unit-5-evidence-and-handoff-c-10)).**
- `ConcordanceWorkloadGameTests.gleanersHeartSearch` times a Gleaner's pulse in every ordinary build. In run
  38774859294's `mod` job it logged: "Concordance workload: Gleaner pulse starved 333 us (its Heart search, 729
  positions, 729 of them loaded; the position list alone 304 us), fed 62 us (its crop search, 147 positions), each
  after 1000 untimed pulses; C-P21's line 250 us: over; runner: 4 processors, amd64, Java VM 25.0.4.1+1-LTS". The test
  passed (its bound is 5 ms a pulse).
- The load card's garden (nine planted beds, the Heart's bed, a Mulch Maw and a Gleaner) worked while measured in run
  38774859294: Gleaner harvests 2 (a Mendvetch and a Dewmoss), Heart searches that drew 0 of 150 pulses, the Maw ate
  150, every item accounted for.
- `ConcordanceWorkshopClientGameTests` shows nine Mendvetch beds with a Mulch Maw and a Gleaner in the workshop's one
  view (random ticks held at 0); it passed in client shard 4 of run 38774859294, and its overview,
  `jugcraft_concordance_workshop`, was viewed (the beds on the right, small at that distance).
- The journey test lays crafted beds and seeds with a mixed bed and a Dewlit graft; it passed in run 38774859294.

Not yet run: any client (the GeckoLib devices, the Fusion beds, the crop textures, the Jade lines and the codex pages
in game), a two-client dedicated server, and a long unattended garden on a real server.

## World and event applicability

No worldgen, creatures or seasons. The crops are not found in the world; they are crafted from vanilla plants.

## Rollout and open questions

- New stable ids: blocks `jugcraft:verdant_bed`, `jugcraft:sunpetal_crop`, `jugcraft:dewmoss_crop`,
  `jugcraft:gloamcap_crop`, `jugcraft:mendvetch_crop`, `jugcraft:verdant_heart`, `jugcraft:mulch_maw`,
  `jugcraft:habitat_gauge`, `jugcraft:gleaner` and their block entities; items `jugcraft:verdant_bed`,
  `jugcraft:sunpetal`, `jugcraft:dewmoss`, `jugcraft:gloamcap`, `jugcraft:mendvetch`, `jugcraft:verdant_chaff` and the
  device items; research `jugcraft:verdant_husbandry`; conversion `jugcraft:verdance_to_ley`; five organisms and seven
  disturbance sources; item tags `jugcraft:verdant_specimens`, `jugcraft:mulch/quarter`, `/half`, `/three_quarters`,
  `/whole`; the bed joins `#minecraft:supports_crops` and `#minecraft:grows_crops`, the crops `#minecraft:crops`, the
  Mendvetch `#jugcraft:nitrogen_fixing_crops`.
- Since Concordance Chunk C, unit 4 (not merged): the graft `jugcraft:dewlit` (data folder `concordance/graft`, lang
  `graft.jugcraft.dewlit`); the organism field `neighbours`; bed keys `strain`, `ripened` and `graft` (and, in the
  update tag only, `graft_host`); lang for the Inspect page (`compose.jugcraft.ecology.inspect.*`,
  `encyclopedia.jugcraft.inspect.garden.habitat` and `.here`), the mixed bed and strain
  (`compose.jugcraft.ecology.mixed`, `.strain`, `.strained.excess` and `.strained.tolerable`), grafts
  (`compose.jugcraft.ecology.graft` and `.graft.range`), the messages `message.jugcraft.concordance.garden.tended`,
  `.tended_lately`, `.grafted` and `.graft_held`, the Jade lines `tooltip.jugcraft.concordance.jade.as_it_grew` and
  `.graft`, and the codex's organisms pages p6 and p7; `recipe_view.json` `garden_compost`. No new block, item or
  payload.
- Save compatibility: additive; since Concordance Chunk C the bed's new keys are optional and its version stays 1.
- Owner review: the crop colours, the icons and the adapted soil and mushroom textures were approved as built on
  7 October 2026.
- Open: a GuiLib garden overview; SmartBrainLib pollinators if a mobile organism is wanted later; graft cuttings as
  items and a visible graft tint (deferred by Concordance Chunk C); whether the mixed bed, strain and Dewlit suit play
  (C-P58 to C-P65 are proposals, their numbers starting values).
- A gap since step 14, found by reading in Concordance Chunk C's unit review: bees step these crops, since
  `#minecraft:bee_growables` includes `#minecraft:crops` and a bee steps any `CropBlock` by its next age (mc263src
  `Bee.java`). Such a step skips `grow`: no niche check (a stalled crop advances), no nutrient cost, no drying, no
  strain on its harvest and no graft cleared. Whether to move the crops to a tag of their own, which fertilizer and
  garden maintenance would also accept, is the owner's choice.
