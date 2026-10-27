# Crucible Stand: electric heat for the next stir

Status: implemented on branch `claude/chunk-c-living-workshop` (Concordance Chunk C, unit 3, C-05: commits `940f1f8a8`
for its art and `7b2dd6783` for the stand), not merged, no pull request. Its tests passed in CI: the server tests in
run 38720814268, and every job in runs 38726242149, 38731825181 and 38774859294 (see Verification).
Proposal issue: none; the owner's choice C-D3 of 14 October 2026 ("Crucible Stand (Recommended)") in
[Concordance Chunk C's contract](concordance-chunk-c.md) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance Chunk C (the living workshop), unit 3; the Practitioner stage, where first
electricity (plan 06 §2) meets the Alembic Arts.
Primary specialty and supported player role: Alembists who also run a little power. A crucible following a written
formula of mixed bands (warm, then hot) repeats it unattended, without the player swapping fires.

Builds on [experimental alchemy](arcane-concordance-alchemy.md) (the Alembic Crucible and its formulas), the
[rituals](arcane-concordance-rituals.md) (Root and Rivet's Rooted Thallite Foundation) and Jugcraft's one energy
interface (JE through cables). The shared courier job of the same unit (C-06) is in
[logistics](arcane-concordance-logistics.md).

## Player experience

**Making it.** Crafted `ICI / ICI / IFI`: six iron ingots, two copper ingots and a **Rooted Thallite Foundation**, which
Root and Rivet makes from a Thallite plate. The recipe loads only while the server's `concordance`, `machines` and
`thallite` feature switches are on (C-P47). The codex has an entry, **Alembic > Crucible Stand** (the Alembic Arts
understood), with the recipe, its heats and piped water.

**Using it.** Set an Alembic Crucible on it and run a cable to a side or underneath; its top, where the crucible sits,
takes no power (C-P44). Holding anything, a use is the item's own, so a crucible or a cable is placed as usual (C-P46).

- **Following a formula,** it heats the crucible for the formula's next stir: warm 65, hot 120 or searing 175 degrees
  (inside each band; starting values, C-P12), paying 8, 16 or 32 JE a tick (C-D3's starting rates). For a cold stir it
  gives nothing and lets the crucible cool. Its copper coil lights on each tick it pays.
- **Fed less than it pays** (a single solar panel on hot, say), it still brings the crucible to the band: on a tick it
  cannot pay it holds the crucible where it is instead of letting it cool, while the crucible is below the band (and,
  following a formula, at the band's lowest degree), so each degree costs what it costs at full power. Inside the band
  such a tick cools as usual, so with no formula a trickle at or below half its rate keeps the crucible at the band's
  edge rather than at its target. Its coil stays lit through up to 9 ticks in a row it cannot pay.
- **It idles, dark and paying nothing** (C-P43), while the crucible waits for anything but heat: water, an ingredient,
  a bottle or bowl, room in its output, a batch to bottle before a repeat, or a formula the rules in force refuse. It
  idles while Preserve holds the crucible (the hold wins), and while the crucible is already hotter than the band's
  target. The crucible then cools by the usual rule, one degree a tick.
- **With no formula,** a plain use with an empty hand cycles it off, warm, hot, searing and off again, by someone who
  may change the block (claims, protected towns, adventure mode and spawn protection refuse), at most once every 10
  ticks a player. Sneaking only reads it. While a formula is set, the formula decides and the setting is kept for later.
- **Reading it:** an empty-hand use shows one line, for example "Crucible Stand: following its crucible's formula, Hot
  (120 degrees); 320 of 640 JE. Heating: 16 JE a tick", or why it gives no heat: the crucible's own reason, "Nothing to
  heat: set an Alembic Crucible on it", or "The Crucible Stand holds 4 JE; it needs 8 JE a tick". Jade shows the same:
  its energy, what decides its heat and at what, its draw, and "Idle while its crucible waits:" before the crucible's
  reason. The crucible's own Inspect page names the stand as the block heating it.
- **Water by pipe:** an Electric Pump on a water source, piped into the crucible, fills its tank for the formula's
  water steps (existing code: the pump pays 8 JE a tick only while it moves water; the tank takes water only and gives
  none back).
- **The manual route stays:** a campfire, fire, magma or lava beneath a crucible works exactly as before.

## Connections

- Existing input producer: Root and Rivet's foundation (unit 2, [rituals](arcane-concordance-rituals.md)); iron and
  copper ingots; any JE generator through Jugcraft's cables; an Electric Pump for water.
- Existing output consumer: the Alembic Crucible's heat, and so every formula with a warm, hot or searing stir.
- Technology connection: JE in through `EnergyStorage.SIDED` on five faces, the same interface as every Jugcraft
  machine and the Ley Pylon; the Electric Pump and fluid pipes for water.
- Magic connection: the Alembic Arts' crucible and formulas; Circle Lore's Root and Rivet for the foundation.
- Reachable entry path (no circular unlock): First Light understood, then the Alembic Arts, and a crucible over a
  campfire; Circle Lore understood, then Root and Rivet on a Thallite plate (a Metal Press, or two thallite ingots by
  hand), then the stand. The stand unlocks nothing: research, ingredients and the formula still gate every brew, and the
  campfire route stays.
- Required or optional: optional. It is a convenience for a powered workshop; no recipe, research or rite needs it.
  Solo throughout.
- How this specialty stays useful without the other branches: a crucible over any fire does everything the stand does,
  more slowly and by hand.

## Balance and automation

- **Heat only for a stir it can take** (C-P43). On a tick it pays its band's rate only when the Concordance is on, a
  crucible stands directly above, Preserve does not hold it, the crucible waits for nothing or for heat alone
  (`WRONG_HEAT`, or a `HELD` left by a hold now over), there is a band, and the crucible is no hotter than the band's
  target. Coasting down from hot to warm is free, so a warm-after-hot formula pays nothing until the crucible reaches
  65.
- **A trickle** (C-P43 as amended by the unit review): when JE came in within the last 20 ticks but not a tick's worth,
  the stand holds the crucible's temperature on that tick while the crucible is below the band, and, following a
  formula, at the band's lowest degree (so a pulse, every 20 ticks, finds it in the band whatever the pattern of the
  ticks it pays). Inside the band such a tick cools, so holding a band still costs the band's rate a tick. A stand with
  no JE coming in lets the crucible cool. Short of JE it goes dark only after 10 such ticks in a row; for any other
  reason it goes dark at once. The crucible reads a stand as out exactly when it gave nothing.
- **Its store** (C-P45): 640 JE (20 searing ticks), at most 64 JE a tick in (the Ley Pylon's rate), nothing ever drawn
  out; switched off, it takes none. Broken, it drops itself and the JE it held is lost, never carried or duplicated.
- **One way only.** The economy model (`tools/concordance_economy.py`) has a `je_to_heat` edge, at the cheapest band's
  8 JE, into a node with no way out; `check_economy` holds that no cycle through it gains and that heat becomes nothing
  else. Its table row, from the tool: a stand heating searing all day pays 768,000 JE for 24,000 ticks of one crucible's
  heat (the generator's arithmetic, not a measurement). Heat is never stored, carried or turned back into JE, Ley Charge
  or anything else ([the economy record](arcane-concordance-economy.md)).
- **No wear and no upkeep** (D-004).
- **Work a tick:** one block-entity read (the crucible above) and no scan; a change of its light sends a client update
  only (`UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE`): no neighbour, cable-shape or light update. It emits no block light and
  shows no signs (the crucible signs its own waits).

## Multiplayer and persistence

- Server authority: the server works out the band and pays; a client sends only the use. Setting it asks
  `Authority.mayChange` as the player, then `RateGate` (`crucible_stand`, 10 ticks); reading is open
  (`AUTHORITY_ROUTES` lists the call).
- Concurrent use: the rate gate is a player's own, so two players may each step the setting once in the same moment;
  a formula above decides whatever the setting.
- Saved (`CrucibleStandBlockEntity`): `energy` and `setting`, stamped 1. A save without them, stamped or not, reads as an
  empty stand set off; a setting this build does not know reads as off; JE beyond the store is brought within it. What it
  paid for last (`given`) is never saved and is worked out at its next tick. Its light is the block state's `lit`.
- Chunk unload and restart: nothing is simulated while unloaded; it resumes from its saved JE and setting.
- Disable behaviour: with `concordance.enabled=false` it takes no JE from a cable, pays none, gives no heat, cannot be
  set and Jade shows only that it is off; everything stays registered.
- Removal or downgrade: an older build drops the unknown block (and its JE). Removing `jugcraft:crucible_stand` needs a
  migration.

## Dependencies and assets

No new dependency. Jade (optional) shows its lines through the existing Concordance provider; Modonomicon has its entry.

**Art** (C-P48): every texture is a byte-for-byte copy of the owner's file, copied and checked by
`tools/owner_art.py` (`block_asset_imports`, from `OWNER_BLOCKS_FILES` in `tools/concordance_alchemy.py`); the owner
supplied the library for Jugcraft, and "Big Cannons and Mounted Guns" is confirmed as the owner's own folder
([the library's README](../../art/owner-library/README.md)). As printed by
`python3 tools/owner_art.py --provenance alchemy` (paths under `art/owner-library/`):

| Runtime file | Library file | SHA-256 | Use |
|---|---|---|---|
| `textures/block/crucible_stand_plates.png` | `originals/Blocks/Guns/block/treated_iron_plates.png` | `75ed386c65cbc6691e7d333c6244ec64fea67453d240c046a07e952e7cefd000` | cladding |
| `textures/block/crucible_stand_frame.png` | `originals/Blocks/Guns/block/treated_iron_block.png` | `ae14010cb4bc05c55f9faee6718d06aa19be2085957ae39fa28f2878d68a35dd` | posts, deck and particle |
| `textures/block/crucible_stand_coil.png` | `originals/Blocks/Big Cannons and Mounted Guns/textures/block/coilside.png` | `e4cccb0a8e507f2286b9896d1802be636e40820bf9f9f3d02162bc3fa7723c33` | the copper coil, every face |

The slab is Jugcraft's own `thallite_block` texture. The model (`stand_model` in `tools/concordance_alchemy.py`) is a
thallite slab, four treated-iron posts carrying a treated-iron deck, cladding round its works and the coil recessed
between cladding and deck; `tools/machine_connections.py`'s `with_model_sockets` adds a power socket on each face that
takes JE (Jugcraft's own `power_port`, `dp_chrome` and `dp_rubber`). The lit model draws the coil full-bright
(`light_emission` 15). Nothing was redrawn or recoloured.

Searched first in `art/owner-library/catalog/files.csv` and not used (hashes taken again from the library for this
record; nothing in it was executed):

| Library file (`originals/Blocks/…`) | SHA-256 | Why not |
|---|---|---|
| `Big Cannons and Mounted Guns/textures/block/coilend.png` | `e56954aaaccc65e6b6170137902c3067df26855599b047e6ddd0c25263638068` | the coil seen end-on: in this model the coil's top lies under the deck and its bottom on the cladding, so it would never show |
| `farming and food textures/heating_tray_side.png` | `e1d0cd97627089d6b62df501e11addff1649d25e8b241202ba14d4bff4c0ec8f` | 182 of 256 texels transparent (an open tray table) |
| `farming and food textures/heating_tray_top.png` | `48baf0e2f5911116ac9f9d6cb67f8b57f1903b8efd673f5534efc720c6b016e2` | 70 texels transparent (a slotted grate) |
| `Guns/block/treated_iron_grate.png` | `2e3a4c2f249a5438bce381a3387f93bab5faafc4be372b55d211cef7d19b35e1` | 72 texels transparent |
| `Guns/block/treated_iron_lamp.png` | `4049b869e1c7290458370499fa0792cc254456178e1c0d905945f2bb6bde7904` | an animated 16×80 lamp |
| `Guns/block/cut_treated_iron.png` | `9d0a5dd171e8bb9f59324213b1bd6089effccf7c1f743e896e8fee1f85e398c7` | masonry |
| `Guns/block/chiseled_treated_iron_block.png` | `1f1055ed71668cc0a5cda34c6f06fba9da70766e4faf0f3110d789c765a86a9a` | masonry (an X brace) |
| `Big Cannons and Mounted Guns/textures/block/burner.png`, `burnerlit.png`, `blastheatingcoil.png` | `92a854f141a7e6e4aca1fb85464b13b7870ace6724eac595216742dd064aaa5b`, `64cf4f5098b391e91b5e27d765b0e38d76cbe6cca4ce63714facdd0586138369`, `8e4ee06fd5639f194c3cc8d71c56441901a4ba7bce31dff43878c529cdc27aba` | brickwork: a masonry hearth, not a metal stand |

No sound or animation. Lang (generated by `tools/concordance_alchemy.py`): `block.jugcraft.crucible_stand`,
`message.jugcraft.concordance.stand.*`, `compose.jugcraft.alchemy.stand.*`,
`tooltip.jugcraft.concordance.jade.stand_*`, `config.jade.plugin_jugcraft.crucible_stand`, the diagnostic
`compose.jugcraft.diagnostic.no_crucible` and the codex entry's keys.

## Verification

**In CI:** it was written where no Minecraft jar is available, and CI was its first compile and run. Run 38720814268 on
`2f13fed54` passed `mod` (every server game test) and `optional integrations absent`, and client shards 1 to 4; shard 0
failed on a Peepo check of an unloaded crucible (a test defect, fixed in `9098c06b1`). Every job passed in run
38726242149 (the merge of units 3 and 4), in run 38731825181 on `9098c06b1` (every client class, 171) and in run
38774859294 on `bb856f873`. Unit 3's client shots below (`crucibleStandWorkshop`): their shard passed, not viewed.

- Server game tests, new:
  - `ConcordanceAlchemyGameTests` (42 in all): `aStandFollowsTheFormulasBands` (a warm-then-hot formula repeated twice
    over a Creative Energy Cell, the draught the formula makes on paper both times, no heat while bottling or waiting
    for water); `anUnpoweredStandLetsTheCrucibleCool` (the crucible cools and waits for its band, the Peepo port says
    `WRONG_TEMPERATURE`, the stand says what it lacks, and with power the formula goes on); `aStandIdlesForAWaitingOrIncompatibleFormula`
    (water, an ingredient, a bottle, a hold, a refused formula, nothing on it; Jade told the same; since the unit
    review, the crucible's pulse names the hold, as over a campfire); `aTrickleFedStandStillReachesItsBand` (the unit
    review: fed 8 JE a tick, a stand set to hot brings its crucible into the band, its light staying on, and one
    following a formula takes both hot stirs);
    `aStandIsSetOnlyByThoseWhoMayChangeIt` (the cycle, the rate gate, a claim, adventure mode, sneaking, a formula
    deciding, and, holding a crucible, a use left to the item with no change of setting); `theFullChainRunsFromAPumpAndACable`
    (a creative cell through a copper cable; an Electric Pump on a spring through a bronze pipe filling the tank's
    bucket, which three water steps draw exactly; two draughts, then the stand idles for glowstone);
    `aCampfireCrucibleStillWorks`; `aSwitchedOffConcordanceStandDrawsNothing` (the switch toggled and restored in one
    tick); `aCableMeetsTheStandWhereItTakesPower` (a cable on each of the six faces: an arm on every face but the seat,
    none once the stand is gone, back once it is placed again). `anIncompatibleFormulaAsksForNothing` also asserts that
    a stand beneath gives no heat.
  - `ConcordanceEconomyGameTests.aStandTurnsElectricityIntoHeatOnly` (3 in all): set to each heat it pays exactly its
    rate a heated tick until its store runs short; off, over a hotter crucible or with nothing on it, nothing; nothing
    comes out as JE on any face, fluid or item, and the mixture is untouched.
  - `ConcordancePersistenceGameTests.theCrucibleStandKeepsItsEnergyAndSettingThroughASave`, and
    `blockEntitiesStampTheirVersion` now includes the stand.
- Client game test, new: `EncyclopediaClientGameTests.crucibleStandWorkshop` at the ordinary scale, a
  crucible following the Kiln formula on a stand fed through a copper cable with a pump piping its water. Shots
  `jugcraft_concordance_crucible_stand_workshop` and, when Jade is loaded, `jugcraft_concordance_crucible_stand_jade`;
  the crucible's Inspect page names the stand with no key or id.
- Audits in `tools/check_mod_data.py`: the new `check_crucible_stand` (targets inside their bands, rates rising, never a
  `HEAT_SOURCES` block, the ports, the owner's art and its models, `Heat.standBand`'s order and `waitsForHeat`, one
  payment and a client-only light update in `heat`, one block-entity read and no loop in `plan`, the store never drawn
  on and closed with the switch off, the seat and no `EnergyConnectable`, the empty-hand rule and `useItemOn`, the
  crucible's `below` asking the stand, the words, recipe, codex entry and Jade lines, a read-only Jade snapshot keyed
  `aim`; since the unit review, the trickle rule in `heat` and `holds`, and the crucible reading a stand as out when it
  gave nothing); `check_economy` also holds the economy's `STAND_JE` to `tools/concordance_alchemy.py`'s;
  `check_alchemy` mirrors `Heat.STAND_*` and the stand's `JE_*`, `CAPACITY`, `INTAKE` and `SET_TICKS`;
  `AUTHORITY_ROUTES` names the setting's call; `check_economy` holds the heat sink; `check_diagnostics` the new code.
- Commands for the unit's records (commit "Concordance Chunk C, unit 3: records"), run on the whole unit and passed:
  `python3 tools/owner_art.py --check`, `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
  `python3 tools/concordance_journey.py --check`, `python3 tools/check_icon_maps.py`,
  `node --test tools/world-designer/model.test.cjs`, `python3 -m unittest discover -s scripts/tests` and
  `git diff --check`.
- Audit mutations, run for the records in place with every file restored after each, against `check_mod_data`'s
  Concordance checks and `owner_art.errors()`: the spec's 22 for C-05 in 33 variants, and two from its review (the
  `useItemOn` override deleted; the `thallite` condition dropped): all 35 reported. One more variant, `jade.stand_waits`
  dropped from the generator's table alone, is reported only once the generator has rewritten the lang (the stale key
  hides it), as unit 1 found for another table.
- The unit review's fixes (commit "Chunk C unit 3: fixes from the unit review"): the same commands, run and passed
  before it was committed; their audit mutations are listed in [the contract](concordance-chunk-c.md) (all 21
  reported).
- Concordance Chunk C, unit 5 (C-10): `ConcordanceWorkshopClientGameTests` shows the stand under a crucible following
  the Stillroot Poultice's formula, fed by a Creative Energy Cell through a copper cable with an Electric Pump piping its
  water, lit while it heats and dark once the crucible waits for Mendvetch; it passed in client shard 4 of run
  38774859294. Viewed for the records: `jugcraft_concordance_stand_lit` (the crucible on the stand, its copper coil lit,
  the pump and the cell beside it) and `jugcraft_encyclopedia_inspect_crucible_stand` ("Crucible Stand beneath heats it
  towards 65 degrees"); `jugcraft_concordance_stand_dark` was not viewed. The load card measures a stand under a
  crucible replaying the poultice (lit at 29 of 60 window ends while measured, in run 38774859294), and the journey
  test powers one from a coal generator; both passed in that run.
- In CI only: `./gradlew build` with and without the optional integrations and `./gradlew runClientGameTest`. Not run:
  any play by hand, and two clients.

## World and event applicability

No worldgen places it, and no structure pre-connects a cable to it. It works in any dimension with power. No creatures,
loot or seasons.

## Rollout and open questions

- New stable ids: block, item and block entity `jugcraft:crucible_stand`; the diagnostic code `no_crucible` (appended
  after unit 2's codes; never renamed or reused); the lang keys above; the codex entry `alembic/crucible_stand`; the
  textures `crucible_stand_plates`, `_frame` and `_coil`; the models `crucible_stand` and `crucible_stand_lit`. No
  change to the crucible's saved keys or update tag for C-05.
- Decisions C-P43 to C-P49 are Proposed ([the contract](concordance-chunk-c.md#decisions)).
- For the owner's visual review (09:144): whether the coil drawn full-bright reads as lit in daylight; whether the
  owner's treated iron reads as the "muted silver-grey" cladding of `ART_DIRECTION.md`; whether a one-block stand under
  a one-block pot suits "real-life-sized stations".
- Known limits:
  - the stand and the crucible tick in no fixed order, so the crucible heats on the stand's last decision, at most a
    tick late; the JE paid is still exactly the rate times the ticks paid;
  - the crucible's reason can be up to 20 ticks old, so after a refill the stand may idle up to 20 ticks more (a hold
    that is over never idles it: `HELD` counts as a wait for heat, and the hold itself is judged from the crucible's
    clock);
  - with no formula, a stand fed at or below half its band's rate keeps the crucible at the band's lowest degree and
    the one below it (the unit review; before it, such a stand never reached the band and paid all it received, its
    light changing almost every tick);
  - no `EnergyConnectable` marker (C-P44), so a worldgen structure could not pre-connect cables to it (none places
    one);
  - with `machines` or `thallite` off its recipe does not load, and its codex recipe page shows the recipe missing
    (Modonomicon's behaviour read from its source for unit 2, not seen in a client); the campfire route is untouched;
  - while Preserve holds a crucible the stand idles, so a held crucible drifts within its band (by design);
  - the model's `light_emission` key and its automatic UVs follow the shipped charged Ley Pylon's model; 26.3's model
    loader was not read for them.
