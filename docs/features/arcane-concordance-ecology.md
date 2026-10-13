# Arcane Concordance: ecological cultivation and living devices

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
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
and with it every dormant bed joined to it (up to 64). A bed holds water like farmland (water within four blocks, rain,
a wet sprinkler or a canteen keep it at 7) and up to **32 nutrients**. With Fusion, rows of beds join into one long bed.

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
and fixes, and two organisms on one block or item.

**The habitat is read within fixed bounds** (`Sampler`): one sample reads exactly 100 positions (the 5x5 at the plant's
height for kinds, and the 5x5x3 round it for disturbance), never in a chunk that is not loaded. A bed keeps its sample
for 200 ticks; a level takes at most **16 samples a tick** (`SampleBudget`), and a bed refused one keeps its last and
asks again. Moisture and nutrients are the bed's own and light is read at the plant each time (one read).

**Growth uses Jugcraft's farming rules** (`OrganismCropBlock`, a vanilla `CropBlock`): on a random tick (vanilla's, or a
sprinkler's, which gives every crop in reach one) the crop reads its habitat, and if the niche allows, takes a step with
the chance `CropGrowth` gives any Jugcraft crop (the soil round it, a legume beside it, the Harvest Moon) scaled by its
pace. The step takes its cost from the bed exactly (`VerdantBedBlockEntity.take`) or, for a fixer, gives its nutrients
one at a time to the poorest awake bed of its own and its four neighbours (`Nourishment.poorest`, first on a tie).
Bone meal and fertilizer on a crop feed its bed (`performBonemeal`) instead of forcing a step.

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
- **Simulation budget**: crops cost only on random ticks; a sample is 100 reads, reused for 10 seconds, at most 16 a
  level a tick; devices pulse every 2 or 10 seconds, staggered; the Maw and Gleaner scan at most 147 positions a pulse.
  No device ticks work every tick, loads a chunk or reads outside its reach.

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

## Dependencies and assets

No new dependency. Framework use:

- **GeckoLib**: the Verdant Heart (beats, faster while thriving), Mulch Maw (chews while eating) and Gleaner (reaches while
  harvesting) are GeckoLib block entities animated from the status the server sends.
- **Fusion** (optional): the built-in pack joins neighbouring Verdant Beds' rims (dry and wet).
- **Jade** (optional): a bed's nutrients, habitat and its crop's verdict; a crop's step and the factors holding it back
  (from its bed's synced verdict); each device's status and store. Without Jade, an empty hand says the same in chat.
- **SmartBrainLib**: not used. None of these organisms moves or decides anything: they are sessile crops and devices,
  so a brain would add nothing. Spirits, familiars and constructs (step 17) are where it applies.
- **Modonomicon**: a new **Garden** codex category (Verdant Husbandry, habitats, organisms, living devices).
- **JEI** (optional): the four crops' alchemy properties appear with the other ingredients.

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
- The pure core compiles with JDK 21. The step 14 harness passes **52 checks** against the generated data: range edges;
  each crop's verdicts (a sunpetal thrives in sun and company, tolerates alone, stalls in shade and says light 9 against
  11; dewmoss stalls in sun; a gloamcap needs magic near it; the vetch grows on an empty bed but not beside loud magic);
  a sample reads exactly 100 positions and caps diversity and disturbance; the budget gives 16 a tick and renews;
  nourishment's tie rule and full beds; the Maw's holding limit; the Heart's four beat outcomes; the parser's refusals;
  and the reference-garden simulation above.
- Game tests added: `ConcordanceGardenGameTests` (nine): a Greenwarden wakes the beds (a novice's bed is dormant and
  grows nothing; one touch wakes the three joined beds and not a fourth); a crop grows only within its niche (lit by
  glowstone it grows a step, paying a nutrient and drying its bed; shut in stone it stalls, and its bed records "too
  little light, needs 11"); an empty bed stops growth (bone meal feeds it; fertilizer feeds the bed two doses and never
  forces a step); a fixer feeds the poorest bed; the Heart beats 1 nutrient into 2 Verdance among three kinds of crop,
  pours 3 for 2 into a pylon, and stops on a starved bed; the Maw feeds the poorest bed and stops eating when every bed
  is full; the Gauge reads moisture and judges an attuned crop; the Gleaner harvests a ripe sunpetal for a Verdance from
  a shared Heart, and a stranger's Gleaner may not draw; twenty beds in one tick get at most 16 samples and the rest are
  read later; a bed and a Heart keep what they hold through a save.
- CI: the first pushes failed to compile (`yield` is a restricted name; 26.3 has no `PushReaction.BLOCK`/`DESTROY` and
  no four-argument `withinManhattan`), then the server failed to start (the crops copied wheat's properties, whose map
  colour reads wheat's own age); each was fixed. Build run 37605176553 (commit `038153ea`) passed: compilation, the
  data checks, every required server game test (including the nine garden tests) with and without the optional
  integrations, and the three client test shards.

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
- Save compatibility: additive.
- Owner review: the crop colours, the icons and the adapted soil and mushroom textures were approved as built on
  7 October 2026.
- Open: a GuiLib garden overview; SmartBrainLib pollinators if a mobile organism is wanted later.
