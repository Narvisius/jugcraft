# Arcane Concordance: the winter workshop and the Rimekeeper's travel kit (Winter Brine, Frostward and the water sets)

Status: implemented for Concordance Chunk B, unit 4 (tickets B-08 and B-10 of [the contract](concordance-chunk-b.md)),
on branch `claude/chunk-b-winter-stone`, based on main `feb6d04fb` (#352) with the contract, unit 1 and
[unit 3](arcane-concordance-rime-strata.md) beneath it; not merged. CI run 38549876709 on `b012875ab` (units 1 to 4)
built it and passed every job: its server game tests among the 1676 required, with and without the optional
integrations, and its client game test. Unit 5 renamed the Frost boon's key to the elective the Yeti King's column
shares (B-P56), made the bangle's Brace count against his push, and gave the client test its slots shot and its toasts
cleared; those changes pass in CI too (run 38591108210 on `25644502a`). Not played by hand or with two clients; see each part's Verification and
[Checks run for the unit](#checks-run-for-the-unit).
Proposal issue: none; the owner chose Chunk B on 13 October 2026 ("Ok lets begin Chunk B") and the same day answered
B-D2 ("Use your water sets as Rime") and B-D3 ("A timed brew"), from
[the expansion plan's delivery chunks](../plans/concordance-expansion-2026-10-10/09-claude-delivery-chunks.md) (B4).
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Practitioner stage (Jugcraft Discovery tier). Winter Brine needs only the Alembic Arts; the
travel kit is crafted from Overworld things before the Adept stage, and its foci and bangle work for a Rimekeeper.
Primary specialty and supported player role: the Alembists (the winter workshop) and the Rimekeepers (the travel kit);
anyone who travels in the cold or fights the Yeti King.

Three parts: Winter Brine and Frostward (B-08); the Rimekeeper's travel kit (B-10); Thallite and the trophies, linked
with no number changed (B-10). Their decisions are B-P40 to B-P53, all Proposed, B-P53 refined by unit 5's B-P56
([the contract's decisions](concordance-chunk-b.md#decisions)).

## 1. Winter Brine and Frostward (B-08)

### Player experience

- **Winter Brine** is what the codex, JEI and the encyclopedia call a Rime brew stirred cold (and, since Concordance
  Chunk C, its bottle: see Known limits). An alembist (the Alembic Arts understood) puts three snowballs in one part of
  water in an Alembic Crucible with nothing burning beneath it, stirs three times and bottles a draught of **Frostward I
  for 48.75 seconds**. Amethyst shards and gloamcap carry Rime too, so their brews can give Frostward as well
  (gloamcap's beside its Hollow's Poison).
- **Frostward** is a new helpful status, with the owner's frost icon. It lasts 60 seconds for each unit of Rime a part
  holds, at most 120 seconds, and milk ends it. While it lasts:
  - no frost gathers on its holder: powder snow, the Frost Horn's arrival chill and a snow werewolf's frostbite chill
    leave none (a creature standing in powder snow ends each tick at most one tick frozen: no damage, a slow too small
    to feel);
  - the Yeti King's blizzard and Frost Breath do not chill its holder;
  - his blows still land, a werewolf's Slowness stays, and powder snow still swallows whoever is not in leather boots.
- **Rime is cold's own.** Every stir above the cold band keeps only part of the Rime already dissolved: warm 60%, hot
  30%, searing none. Stirred warm, the same three snowballs keep 0.69 Rime a part, short of the 0.80 a draught needs,
  and the draught does nothing; a master's Assay Glass names "stirs above the cold, which thaw Rime" among its reasons.
  Searing stirs take all of the Rime and foul the brew into Nausea.
- **The Yeti Mitten stays the lasting answer:** held in the offhand, it keeps its holder warm for as long as it is held
  (B-D3).
- **Bottles already made keep what they were bottled with:** an old Rime draught still gives Resistance. A formula
  written before replays and now brews Frostward.
- **Where it is told:** JEI's snowball, amethyst shard and gloamcap pages end "stirred cold, its Rime brews Frostward
  (Winter Brine)" (a property fact, never a formula's outcome); the codex's Alembic category gains **Winter Brine** once
  the Alembic Arts are observed (a worked example, then Frostward), and its Stirring page names the warmer bands' loss.
- **The Glacier Hall's preparation fact** (B-P29, B-P45): carrying a Frost Horn, the rite on the encyclopedia's
  Expeditions page now reads "Against his frost: Winter Brine, snowballs stirred cold in an Alembic Crucible, gives
  Frostward for up to 120 seconds a draught; the Yeti Mitten from his loot keeps its holder warm for as long as it is
  held", with a snowball, the crucible, a draught and the Mitten as its icons, and a link to the codex's Winter Brine
  once the Alembic Arts are observed. With the Concordance switched off, or under a data pack whose Rime no longer
  brews Frostward, it names the Mitten only. It is text and icons, never a recipe grid: the page's twelve grids stay
  the four keepers'.

### How it works

- **Data,** from `tools/concordance_alchemy.py` (never hand-edited): `PROPERTIES["rime"]` gives `jugcraft:frostward`
  (helpful, threshold 0.80, 1200 ticks a unit, at most 2400: B-P19), written to
  `data/jugcraft/concordance/property/rime.json`; `RIME_KEEP` (cold 1000, warm 600, hot 300, searing 0 per thousand:
  B-P18); `simulate()` and `winter_brine()`, the process as Java's `Mixture` runs it, in thousandths with floor
  division, for the codex's worked example and the audit; the JEI lines; the codex entry `alembic/winter_brine` at
  (4, 2). `tools/concordance_rime.py` holds Frostward's colour, name and the owner's icon (`OWNER_FILES`).
- **The heat rule** (`concordance/alchemy/Mixture`): after each stir, the dissolved Rime is scaled by `rimeKeep(band)`
  (`RIME_KEEP_WARM`, `RIME_KEEP_HOT`, `RIME_KEEP_SEARING`), beside the searing rule. It acts on stirs only: an addition
  records no band, so the formula format is unchanged and a ground ingredient's ready share stays until a stir (B-P41).
  `Outcome` adds the reason `alchemy.thawed` to a Rime reason when a warmer stir came after something carrying Rime
  went in (a stir before that thawed nothing and is not named).
- **Frostward** (`concordance/rime/Frostward`): a helpful `MobEffect` that clears `ticksFrozen` in its own effect tick,
  every tick, on whatever creature carries it (the Mitten's check, done by the creature under it; B-P40). Effects tick
  in `baseTick` before blocks act in `aiStep` (mc263src `LivingEntity`), so powder snow adds at most one tick before the
  next clearing. It never touches Slowness or sinking. It is registered with Rime whatever the Concordance switch, so a
  saved Frostward keeps its id. Every Rime brew gives it, and no brew gives Resistance any more (B-P42).
- **The Yeti King** (`lair/yeti/YetiKingEntity.chill`, this part's only lair change): his frost returns early for
  `Frostward.warded(player)` as it does for the Mitten. His blows, push and footing are unchanged.
- **The dossier** (`lair/LairDossiers.prepare`, on unit 1's dossiers): it reads the catalogue's Rime property and
  `Frostward.STATUS`, takes the time from the data (`maxTicks / 20`) and writes one fact with icons and a codex link,
  whose entry, research and state are literals (the Alembic Arts observed, the entry's own unlock). It only reads.

### Connections

- Tier: Practitioner (Discovery): First Light understood, then the Alembic Arts. Neither Rimekeeping nor salt is
  needed (B-P17, B-P44).
- Input producers: snowballs (snow, snow golems), amethyst shards (geodes) and gloamcap; water (a bottle, a bucket, or a
  pipe through the Transfer API); an Alembic Crucible with nothing burning beneath; glass bottles, or bowls for a salve.
- Output consumers: anyone going into powder snow, arriving by the Frost Horn, bitten by a snow werewolf, or fighting
  the Yeti King. It is the plan's "one useful cold preparation route and a non-boss resistance option" (09:106) and
  makes "every required cold preparation has a non-boss route" (09:110) true for his frost.
- Costs: a draught takes three snowballs, one part of water, one bottle and three cold stirs (at most one stir every
  half second). A full crucible (two buckets for six parts, 18 snowballs and three stirs: 23 of its 32 steps) bottles
  six draughts.
- Unlocks: none. Bottling one by hand is alchemy practice (a new distinct outcome, `jugcraft:frostward@0`) and, from
  unit 3, Rimekeeping's workshop study (a brew that carries Rime).
- Failure behaviour:
  - warm or hot stirs thaw the Rime, and a draught short of 0.80 does nothing (the Assay Glass says why);
  - searing stirs remove the Rime and foul the brew into Nausea;
  - a data pack that changes Rime's status takes the dossier's Winter Brine words away (the Mitten's stay);
  - Frostward ends with its time, or with milk;
  - the Concordance switched off: crucibles refuse work, and the dossier names the Mitten only.
- With unit 3: Preserve can hold a cold crucible cold over a fire (`Band.keep`), the Rime workshop loop; a Preserve on
  a crucible holding a mixture is Rimekeeping's cold work.
- Solo and trade: entirely solo; draughts, salves and written formulas trade like any item.

### Balance

- There is no vanilla Frostward potion, so it is held to the Fire Resistance brew's numbers: 60 seconds a unit, at most
  120, as Ember's Fire Resistance brew (vanilla's Fire Resistance potion lasts 3 minutes). `check_winter_brine` holds
  Rime's timing to Ember's.
- The old Rime brew gave Resistance I for at most 60 seconds; the same cold brew of three snowballs gave 487 ticks. No
  brew gives Resistance any more (B-P42).
- **"Needs the cold band" holds per recipe, not absolutely:** four raw snowballs stirred warm twice still reach
  Frostward (0.936 a part), and five stirred hot once (0.825); searing never does. Two ground snowballs bottled with no stir make Frostward at any heat,
  because their ready share is dissolved as they go in and no stir has thawed it (B-P41).
- **No economy loop** (06:146-158): Yeti Fur reaches no Rime-bearing ingredient (snowball, amethyst shard, gloamcap).
  Its wool goes on through the Assayer's Scale to sugar and bone meal, which are ingredients but carry no Rime; no
  recipe, villager trade or Scale form makes snow, amethyst or gloamcap from it, so the boss makes Frostward no cheaper
  (a town's coin shops, where the General Store buys wool and the Seasonal Stall sells snow blocks in winter, are money,
  as for any item, and stay outside this check); a draught or a salve is neither an ingredient nor weighed at the
  Assayer's Scale; diluting makes more bottles but not more Frostward (two parts of the same brew carry 0.406 a
  part, below the threshold); two draughts drunk one after the other keep the longer time, never the sum.
- It costs no Focus: brewing never has.

### Multiplayer and persistence

- Server authority: the crucible simulates on the server; a draught or salve is applied through the shared effect
  executor (friendly fire, protection and tolerance as for any brew); the Yeti's chill asks the server's Frostward. No
  packet or client claim is added.
- No support credit: a Frostward salve put on an ally earns no lair support credit, since the lair observers see only
  restoration and protection (`ConcordanceEffects`).

| What | Effect | Test |
|---|---|---|
| Mob effect `jugcraft:frostward` | A new id, stable once released, saved in `active_effects` | `frostwardClearsFrostAndKeepsItsTime` (a round trip on a pig) |
| `jugcraft:brew` | Format unchanged. Rime bottles made before keep `minecraft:resistance` | `oldRimeBottlesStillGiveResistance` |
| Formulas (items and the crucible's program) | Operations only. They replay under the new rule and status: warm, hot or searing Rime stirs now brew weaker or nothing, and the formula is still accepted | `aRimeFormulaWrittenBeforeBrewsFrostward` |
| The crucible's mixture | No format change. A mixture saved mid-brew keeps its amounts; only later stirs follow the rule | `aCrucibleKeepsItsMixtureWhenSaved` (unchanged) |
| Practice outcome `jugcraft:frostward@0` | One more distinct Alembic outcome beside any old `minecraft:resistance@0` | none |

- Automation: a crucible following an old formula whose output still holds old Resistance draughts waits (its sign
  says it wants them taken) until they are taken, because a Frostward draught does not stack with them.

### Dependencies and assets

No new dependency. Framework use: **Modonomicon** (the Winter Brine entry), **JEI** (optional: the ingredient pages'
words), the shared effect boundary.

Frostward's icon is the owner's (`art/owner-library/originals/Magic`; the provenance under the travel kit applies):

| Runtime file (under `assets/jugcraft`) | Owner's file | SHA-256 of the owner's file | The owner's meaning |
|---|---|---|---|
| `textures/mob_effect/frostward.png` | `originals/Magic/assets/ars_jymbaumental/textures/mob_effect/frozen.png` | `370d286f55c7502a13ff47e18a9d61f336153863c2d7b7299403a5036e82a8ff` | "Frozen" (can't heal): used here for its opposite, the ward against frost |

It is 18×18, copied byte for byte by `tools/owner_art.py` from `tools/concordance_rime.py` `OWNER_FILES`. The
alternative offered at the checkpoint is `ars_jimbaux/textures/mob_effect/shielding.png` (B-P43).

### Verification

Game tests added; all passed in CI run 38549876709, their first run (see
[Checks run for the unit](#checks-run-for-the-unit)):

- `ConcordanceAlchemyGameTests` (+4): `winterBrineNeedsTheColdBand` (on paper, the Rime each band keeps of three
  snowballs, 813, 685, 211 and 0, the generator's own numbers, which the audit reads from the test; by hand, a cold and
  a warm crucible stirred three times, the cold draught drunk: Frostward for 975 ticks and no Resistance; the warm
  one does nothing; searing gives Nausea; a warm stir before any snowball is not named as thawing),
  `oldRimeBottlesStillGiveResistance` (a save test: a Rime draught as saved before, Resistance for 487 ticks, read,
  re-encoded and drunk), `aRimeFormulaWrittenBeforeBrewsFrostward` (an old
  formula's text set on a crucible fed by tank and buffer), `frostwardIsNeverCheaperThanItsSnow` (the must-fail loops
  above).
- `ConcordanceWinterGameTests.frostwardClearsFrostAndKeepsItsTime`: two pigs in powder snow, one warded, and a frozen
  third warded on the floor; a real save and load of the warded pig keeps Frostward's time.
- `YetiKingGameTests.aFrostwardDrinkerIsNotChilledByTheYeti`: his blizzard and breath frost leave a Winter Brine
  drinker unfrozen; frost already gathered is gone after a tick; with Frostward taken away his frost lands again.
- `EncyclopediaGameTests.theGlacierHallsRiteNamesWinterBrine`: the preparation fact with no grid, its time from the
  data, its four icons, the codex link only once the Alembic Arts are observed, and the Mitten alone with the
  Concordance switched off.
- `ConcordanceWinterClientGameTests`: Frostward's icon in the client's resources, and the screenshot
  `jugcraft_winter_frostward_hud` (presentation, not evidence; it shows the icon on the HUD, described under
  [Checks run for the unit](#checks-run-for-the-unit)).

Audits: `check_alchemy` mirrors `RIME_KEEP_*` and accepts a status only if Jugcraft registers it; the new
`check_winter_brine` holds the property, the timing, the heat rule in Java and Python, the test's literal, Frostward's
class and registration, the Yeti's chill, the economy loop, every word, the owner's icon and the dossier's fact (text
and icons, reading `Frostward.STATUS`). See [Checks run for the unit](#checks-run-for-the-unit).

Implemented with Claude Code; the model is named in the commit trailers.

## 2. The Rimekeeper's travel kit (B-10)

### Player experience

The owner's water art, under the owner's names (B-D2), made at a crafting table from Overworld things:

- **Lesser Focus of Water** and **Focus of Water**, worn in the Spell Focus slot: Preserve holds an Alembic Crucible's
  heat **90 seconds** or **120 seconds** instead of 60. One Spell Focus slot means a fire focus or a water focus, never
  both. A focus gives no Spell Power and holds no Focus.
- **Water Bangle**, worn in a Bracelet slot: once you understand Rimekeeping, whenever you have gone **30 seconds
  without a Brace** it braces you for 5 seconds (Brace's own Braced: the next blow or harmful push moves you half as
  far). It is not your cast, so it teaches Stratawrighting nothing and earns no one a deed; a second bangle adds
  nothing. Putting it on (or back on: taking it off forgets the count), logging in or coming back from death starts
  the count, with no Brace at once.
- **Three water sets,** each worn as the owner's GeckoLib model in its own sheet:
  - **Nereid's** Hood, Tunic, Pants and Shoes (light): the Pyromaniac's equal;
  - **Aquamancer's** Hat, Robes, Leggings and Boots (medium): leather's protection, longer-lasting;
  - **Winterguard's** Helmet, Chestplate, Leggings and Boots (heavy): iron's protection, on its own model.
- **Wearing any one piece of any set,** powder snow and vanilla's freezing never freeze or hurt you. You still sink in
  powder snow: in 26.3 only leather boots walk on it, and no kit boot does (B-P46).
- **The Yeti King's frost is his own:** in any set it still lands and slows you; the kit stops only the freezing harm,
  as leather and knitwear already do. Frostward (Winter Brine) or his Mitten keeps his frost off.
- A codex entry, **A Rimekeeper's Travel Kit** (Winter category, always shown), has each recipe and the numbers.

### How it works

- **Data,** from the new `tools/concordance_rime_kit.py`, merged by `tools/concordance.py`: the fifteen items, their
  recipes (load-conditioned on the Concordance switch), the three sets (`ARMOR_SETS`), a repair tag for each set, the
  codex entry `winter/travel_kit` at (6, 0), and every piece in `#minecraft:freeze_immune_wearables` (beside leather and
  Jugcraft's knitwear). Ember's writer writes both slices' Trinkets slot tags from one table, `concordance_ember.SLOT_ITEMS`
  (B-P51: one writer for each file). `tools/gear.py` puts the pieces in `#minecraft:head_armor` and its kin;
  `tools/concordance_equivalence.py` keeps the fifteen off the Assayer's Scale; `tools/acquisition.py` lists them as
  `OPTIONAL` (made by recipe).
- **Java** (`concordance/rime/RimeGear`, registered by `Rime.register`):
  - the three armour materials and twelve pieces, each an `EmberArmorItem` that knows its set and model: Nereid's and
    Aquamancer's on the owner's medium model (Ember's import, `jugcraft:armor/pyromancers`), Winterguard's on
    `jugcraft:armor/winterguards`;
  - the foci and bangle are `RimeTrinketItem`s: no attribute and no callback modifier; what they do is read where it
    works, only from a slot that applies effects and is not cosmetic, never from the hand;
  - `holdTicks(person)`: Preserve asks it (`Rime.preserve`), and it returns 1200, or 1800 or 2400 with a lesser focus or
    a focus worn, never past `Rime.MAX_HOLD_TICKS` (2400);
  - `bangle(player, now)`, from an `END_SERVER_TICK` listener once a second, only while the Concordance is on: a living
    Rimekeeper wearing the bangle where it works, braced by nothing for 600 ticks, is braced through the shared effect
    boundary by `brace(player)`, with no actor (as a draught is applied), so Brace records no practice and names no
    caster. When the wearer was last braced or first seen is the attachment `jugcraft:water_bangle_braced`, which is
    never saved;
  - creative tabs: the foci and bangle in Tools and Utilities, the pieces in Combat.
- **Client:** nothing new. `EmberClient` already makes one GeckoLib renderer for each set and model an `EmberArmorItem`
  names; Ember's renderer poses the bones, hides the slim sleeves and adds the glint. The heavy model has every bone
  it needs.
- **Shared with Ember:** Spell Power's attribute enchantments (Sunfire and its kind) are refused on any
  `EmberArmorItem`, so on the water sets too.

### Connections

- Tier: Practitioner. The pieces need nothing to make or wear; the foci need Preserve (Rimekeeping understood); the
  bangle needs Rimekeeping understood.
- Input producers: Overworld wool, string, snowballs, snow blocks, lapis lazuli, iron, gold, amethyst shards and
  diamonds (a crafting table). No boss item, and no Nether or End step (`check_rime_kit`; `concordance_journey.py`
  holds every piece to the first-loadout rule: one player, the Overworld, before the Adept stage).
- Output consumers: Preserve's hold (the foci); Brace's knockback (the bangle); vanilla freezing (the sets); armour
  protection.
- Unlocks: none. No research, practice or stage needs any of these items, so no unlock can become circular.
- Failure behaviour:
  - before Rimekeeping is understood, the bangle does nothing; a focus works only through Preserve, which needs it;
  - a focus or bangle in the hand or a cosmetic slot gives nothing;
  - with the Concordance switched off, the recipes do not load and the bangle and foci do nothing (Preserve is
    refused); the items stay registered and freeze-proof, as Ember's stay registered;
  - the bangle's first look only starts its count; already braced (by anyone), the count starts again; taken off,
    its count is forgotten;
  - a dead or spectating player is not looked at.
- Solo and trade: entirely solo; the items are ordinary stacks.
- Specialty use without other branches: the sets need nothing; the foci and bangle need First Light and Rimekeeping.

### Balance

Protection no greater than what each set matches (B-P23, B-P47); no Spell Power, toughness or knockback resistance; not
fire resistant:

| Set | Protection (head, chest, legs, feet) | Matches | Durability (× vanilla's base) | Enchantability | Repaired with | Recipe (Ember's patterns, a cold accent; B-P48) |
|---|---|---|---|---|---|---|
| Nereid's (light) | 1, 2, 1, 1 (5) | the Pyromaniac's (5) | ×7 (77, 112, 105, 91) | 15 | wool | wool and a snowball (the shoes two) |
| Aquamancer's (medium) | 1, 3, 2, 1 (7) | leather (7) | ×10 (110, 160, 150, 130) | 15 | wool | wool and lapis lazuli (the boots two) |
| Winterguard's (heavy) | 2, 6, 5, 2 (15) | iron (15) | ×15 (165, 240, 225, 195) | 9 | iron ingot | iron armour's iron and a snow block in the cell iron leaves empty: never cheaper than iron |
| leather (vanilla) | 1, 3, 2, 1 (7) | | ×5 | 15 | leather | |

- **Against the cold,** each piece gives what one leather piece or a knitwear piece already gives
  (`#minecraft:freeze_immune_wearables`): vanilla's freezing passes its wearer by. What each set adds over its equal is
  that alone.
- **The foci** hold 1800 and 2400 ticks, beyond the Initiate's Wand's 1200-tick duration limit (B-P50). That limit
  bounds a composed plan's effect ticks at compile time; Preserve's hold is the crucible's own state, set in Java (an
  alteration takes no duration), and the crucible cuts any hold at 2400, `EffectSpec.MAX_DURATION`, the Adept's Wand's
  limit. This answers unit 3's note. Recipes: the Lesser Focus of Water is 2 string, 2 gold ingots, 2 snowballs and an
  amethyst shard (the Lesser Focus of Fire's pattern); the Focus of Water is the lesser one with 2 diamonds and 2 gold
  ingots.
- **The bangle** gives at most 5 seconds of Brace in every 35 (30 with none, then 5), half the knockback of one blow or
  push, no Focus. Its recipe is 4 gold ingots, a snow block and an amethyst shard. Against the Yeti King's own push it
  did nothing in unit 4: his scripted push ignored knockback resistance. Since unit 5 (B-09) his every push asks
  `Brace.spend`, so the bangle's Brace halves one of his pushes (`aBracedPlayerIsPushedHalfAsFarOnce`) and, being
  nobody's cast, credits no one (`aBraceOnAFriendIsTheCastersDeed`); neither yet run in CI.
- **No conversion loops:** no recipe makes a material, and nothing turns back into its inputs.

### The kit against the Yeti King's frost

| Worn or held | Powder snow freezes or hurts | His frost lands | Slowed by it | Hurt by it when fully frozen |
|---|---|---|---|---|
| Nothing | yes | yes | yes, to −0.05 movement speed | yes, 1 every 40 ticks (it bypasses armour) |
| A leather or knitwear piece | no | yes | yes | no |
| **Any kit piece** | **no** | **yes** | **yes** | **no** |
| Frostward (Winter Brine, timed) | no: its frost is cleared each tick (at most one tick of it in powder snow) | no | no | no |
| Yeti Mitten (offhand, his loot) | no: its frost is cleared each tick | no | no | no |

The kit adds only what leather already gives (B-P23: no permanent boss immunity). The answers to his frost stay
Frostward (timed and brewed) and the Mitten (lasting, his prize). That a kit wearer is slowed but not hurt by his frost
was an inference in the contract; `aKitWearerIsSlowedButNotHurtByHisFrost` and the client test (a hundred ticks of his
frost on the real player in Winterguard's) show it, both passed in CI run 38549876709.

### Multiplayer and persistence

- Server authority: the hold is read from the wearer's Trinkets slots on the server when Preserve lands; the bangle is
  looked at on the server's tick and goes through the shared effect boundary; freezing is vanilla's, on the server. No
  packet or client claim is added.
- Cost: the bangle loop looks at every player's Trinkets slots once a second, whether or not they wear a bangle, and
  at their research only while they wear one.

| What | Effect | Test |
|---|---|---|
| 15 items, equipment assets `jugcraft:nereids`, `jugcraft:aquamancers`, `jugcraft:winterguards`, the model `jugcraft:armor/winterguards`, repair tags, codex entry | New stable ids. Vanilla stacks and Trinkets' own slot data; no format of the kit's own | `theTravelKitSurvivesASave` |
| Attachment `jugcraft:water_bangle_braced` | Not saved: a relog, restart or death restarts the count with no free Brace | `theTravelKitSurvivesASave` (`isPersistent`), `check_rime_kit` |
| The crucible's `held_until` (unit 3's) | Up to `now + 2400` from a Focus of Water, within the crucible's own cut | `theCrucibleKeepsPreservesHoldThroughASave`, extended |

### Dependencies and assets

No new dependency. Framework use: **Trinkets Updated** (Ember's Spell Focus and Bracelet slots, given by data; the
slots read on the server), **GeckoLib** (the three sets through Ember's armour renderer; a second armour model),
**Fabric API** (the server tick; a non-saved attachment; creative tabs), **Modonomicon** (the codex entry).

**Provenance.** Every file below is from the owner's magic collection (`art/owner-library/originals/Magic`, supplied
8 October 2026, [MAGIC_ASSETS.md](../../art/owner-library/MAGIC_ASSETS.md)). The owner says they used other Minecraft
mods as a starting base and then "completely redid and remade all the stuff"
([PROVENANCE.md](../../art/owner-library/PROVENANCE.md)); this record repeats that declaration, which is not an
independent rights audit. The project uses the files under the owner's project-use authorization
([LICENSE_POLICY.md](../../LICENSE_POLICY.md), owner-supplied collections). The source folder's name
(`ars_jymbaumental`) is kept here as a label; nothing in the game uses it. The imported files carry no credits or
notices (no PNG text, iTXt or eXIf chunks; the model's JSON has no credit strings), and `catalog/legacy-metadata.csv`
has no row for them. They are byte-for-byte copies under Jugcraft names, written by `tools/owner_art.py` from
`tools/concordance_rime_kit.py` `OWNER_FILES` and checked byte for byte by `owner_art.py --check` and `check_mod_data`;
nothing from the library was run. `python3 tools/owner_art.py --provenance rime_kit` prints this table:

| Runtime file (under `assets/jugcraft`) | Owner's file (under `originals/Magic/assets/ars_jymbaumental`) | SHA-256 of the owner's file |
|---|---|---|
| `geckolib/models/armor/winterguards.geo.json` | `geo/heavy_armor_water.geo.json` | `e0ef5daccfda4e3e018280bd287ad6983e54a61e3b2994f8db18593a4b43be39` |
| `textures/armor/aquamancers.png` | `textures/armor/medium_armor_water.png` | `b0866ae0995297c8b4fbaf033edfb0860ef4778d0b08a168335b794e2b9a6ae9` |
| `textures/armor/nereids.png` | `textures/armor/light_armor_water.png` | `b0866ae0995297c8b4fbaf033edfb0860ef4778d0b08a168335b794e2b9a6ae9` |
| `textures/armor/winterguards.png` | `textures/armor/heavy_armor_water.png` | `d5b7556e2f8932570a45932cda910176e2e1b69212c7d5dcec0a9832af972c68` |
| `textures/item/aquamancers_boots.png` | `textures/item/aqua_boots.png` | `1224496910993dbddc9a7a582140b4917995a4ee2ea3181e94b21643fc47fa5a` |
| `textures/item/aquamancers_hat.png` | `textures/item/aqua_hat.png` | `3077dcaeba5eef0addfb328fe7ba0b0803f7a5301a8bd7f2d6ea52e51516a23b` |
| `textures/item/aquamancers_leggings.png` | `textures/item/aqua_leggings.png` | `21427299916153a33af748839d230e6fce9ceb2938dc4db053518c0204f736a4` |
| `textures/item/aquamancers_robes.png` | `textures/item/aqua_robes.png` | `b4981d3959057a98df75a56a1581349638321fa8e853d072489a96ec936c7b1d` |
| `textures/item/lesser_water_focus.png` | `textures/item/lesser_water_focus.png` | `f84b2d2636643884d0924a44ee5a602a653db4a1dc4ad2f7ea2f1cf9f4dc779c` |
| `textures/item/nereids_hood.png` | `textures/item/aqua_hood.png` | `26d5d40f39b266dcc56c2e3b38d6ca2259ce1b2ffd5a08a46b31e4c99d3918f6` |
| `textures/item/nereids_pants.png` | `textures/item/aqua_pants.png` | `b76ed0354eb91bfc7e2dbbcb3390f0945d19f4fab00ab0f6021083f69a132bfc` |
| `textures/item/nereids_shoes.png` | `textures/item/aqua_shoes.png` | `4f064afeb4295c60b7fe1a0da35639012c8d0a056189e316a9c0a1a132456c6f` |
| `textures/item/nereids_tunic.png` | `textures/item/aqua_tunic.png` | `5e3e885a4169ba87ea7df65a3f676ccc6bd8727e400883bf7aab65b8c0fa363f` |
| `textures/item/water_bangle.png` | `textures/item/water_bangle.png` | `497df5eaeedeb0036dd5de6d964fd035c731478ec2509d6c440f09e7ee9fe488` |
| `textures/item/water_focus.png` | `textures/item/water_focus.png` | `5aa907b2d5f92553a53f7979ece978dc8ce6205e40d4eac91a3878795dcaad50` |
| `textures/item/winterguards_boots.png` | `textures/item/aqua_boots_heavy.png` | `71e5794ff064e8f9e8d244faec4d387eb64b24a7c74f69f5e9db18825cfe4f7e` |
| `textures/item/winterguards_chestplate.png` | `textures/item/aqua_chestplate.png` | `eb4a4c538c3d3016827f8714a86c3396922d09ee1f7d472fddcfd84a293da236` |
| `textures/item/winterguards_helmet.png` | `textures/item/aqua_helmet.png` | `2bb2d5b69ed46315a14fa77d47cc14f7eb61e1c644ded1fdc81076a0bc72691a` |
| `textures/item/winterguards_leggings.png` | `textures/item/aqua_leggings_heavy.png` | `983266224992726fe22184a8742f832b5bf0e616ccbdbb9def7ceda041676650` |

Also taken from the owner's files, as data rather than copies:
- the fifteen item models (`models/item/<id>.json`), each the owner's plain generated model with its texture renamed
  to the Jugcraft copy, generated from the library file each run (anything else stops the generator);
- the fifteen display names, from `assets/ars_jymbaumental/lang/en_us.json`; only the names are taken (the tooltips,
  codex text and recipes are Jugcraft's), and `check_rime_kit` compares them with that file.

Ids renamed: the owner's `aqua_hood`, `aqua_tunic`, `aqua_pants` and `aqua_shoes` are `nereids_*`; `aqua_hat`,
`aqua_robes`, `aqua_leggings` and `aqua_boots` are `aquamancers_*`; `aqua_helmet`, `aqua_chestplate`,
`aqua_leggings_heavy` and `aqua_boots_heavy` are `winterguards_*`, so each set's ids name it. The display names are the
owner's.

**The sheets and models, measured** (14 October 2026, the share of opaque pixels under each model face, the slim
sleeves left out; `check_rime_kit` holds every set to at least 65% overall and 50% for each bone):

| Model and sheet | Opaque share |
|---|---|
| the heavy sheet on the heavy model (Winterguard's) | 71% (the head 91%, the arms 58%) |
| the medium water sheet on the medium model (Nereid's and Aquamancer's) | 71% |
| Ember's fire sheets on the medium model (the baseline) | 71% |
| the medium sheet on the heavy model (the wrong pairing) | 58%, the head 0% |
| the light sheet on the owner's light model | 19% |

- The heavy model is format 1.12.0 on a 128×128 sheet with every bone Ember's renderer poses and hides; its head pivots
  at y 24, as the wearer's does, so it is copied as supplied (Ember's deferred heavy fire model pivots at 20). No face
  maps outside the sheet, and its line ends are already LF.
- The light water sheet is the medium sheet byte for byte, as with fire, so Nereid's is worn on the medium model and
  Nereid's and Aquamancer's look alike when worn; their icons are the owner's own for each piece.
- The heavy sheet's top half (body, arms and legs) is the owner's medium water sheet exactly (0 of 2,791 opaque pixels
  differ); its bottom half, the helm, matches nothing else in the library (compared with every library PNG and every
  texture embedded in a Blockbench project, under a 10% difference). So the file-specific point that holds back Ember's
  heavy fire set does not arise here.

Not imported: the owner's light model (19% fit), the owner's recipes (they use another mod's machines, netherite,
"water essence" and a powder snow bucket on a base bangle), the Caster Tome of Water, and the owner's language files.
The owner's bangle text ("Your arms chills the air around, freezing enemies on every hit") is the alternative under
B-P49, recorded and not built.

### Verification

Game tests added; all passed in CI run 38549876709 (see [Checks run for the unit](#checks-run-for-the-unit)):

- `ConcordanceWinterGameTests`: `theTravelKitIsMadeAndWornAsDesigned` (the fifteen items, their kinds, slots and tags;
  each piece's set, model, slot, equipment asset, protection against its bound, toughness and knockback 0, no other
  attribute, durability, enchantability, repair, armour, enchantable and freeze tags), `everyPieceKeepsPowderSnowFromFreezingItsWearer`
  (a pig in powder snow for each of the twelve pieces and one bare; and vanilla's powder-snow walking true for leather
  boots only, B-P46), `theWaterFociLengthenPreservesHold` (1200, 1800, 2400; the hand and a Focus of Fire give
  nothing; a recast never adds; and through Spell Engine, the real cast), `theWaterBangleBracesARimekeeperEveryThirtySeconds`
  (before Rimekeeping nothing; the count, the Brace at 600 ticks, the restart after any Brace, a second bangle, taken
  off or held, put back on: the count starts again), `theWaterBanglesBraceIsNobodysCast` (no caster, no anchoring
  practice, spent as a Brace), `spellPowerEnchantmentsAreRefusedOnTheWaterSets`.
- `YetiKingGameTests.aKitWearerIsSlowedButNotHurtByHisFrost`: in Nereid's Hood his frost lands and freezes through;
  pigs held fully frozen for 90 ticks, the dressed one at full health and the bare one hurt, both slowed.
- `ConcordancePersistenceGameTests`: new `theTravelKitSurvivesASave` (each of the fifteen, damaged and enchanted or
  named, round-trips equal; the bangle's attachment is not persistent); `theCrucibleKeepsPreservesHoldThroughASave`
  gains a Focus of Water's 2400-tick hold read back whole.
- `ConcordanceGameTests.theFirstLoadoutIsMadeInSurvival`: `FIRST_LOADOUT_RECIPES` gains the fifteen
  (`check_acquisition` holds the list to `tools/acquisition.py`).
- `ConcordanceWinterClientGameTests` (client): a GeckoLib renderer for each of the twelve pieces, one for each set, each
  set on its model in its own sheet, and the heavy model's bones; screenshots of a row of stands (iron as the control,
  the three sets, Winterguard's enchanted, on a zombie and on a small stand) in four views and close up
  (`jugcraft_rime_kit_*`); the player in Winterguard's, standing and sneaking, front and back; a hundred ticks of his
  frost on the real, ticking player, in Winterguard's (still slowed, no health lost) and without it (hurt); the real
  Trinkets slots (a Focus of Water's 2400-tick hold, and the bangle bracing the player through the server's own tick);
  the fifteen icons in frames. Nothing judges the pictures; they are described under
  [Checks run for the unit](#checks-run-for-the-unit).

Audits: the new `check_rime_kit` (the Java's numbers and ids against the tables; protection within its bounds, no
Spell Power, toughness or knockback; the foci only from a slot that applies effects, within the cap; the bangle's
Brace through the boundary, nobody's cast, never saved; every item's owner model, icon and name; every recipe from
Overworld things and colliding with no other; slots and tags; the heavy model's bones and each sheet's fit; no mixin
touching powder snow; the codex). `check_ember_regalia` reads `SLOT_ITEMS` and keeps Ember's own pieces in it.

Implemented with Claude Code; the model is named in the commit trailers.

### Changes from the plan and the contract

- **"Snow-safe boots" means freeze-proof** (B-P46). Vanilla 26.3 lets an entity walk on powder snow only for the
  entity-type tag `minecraft:powder_snow_walkable_mobs` or leather boots in the feet slot
  (`PowderSnowBlock.canEntityWalkOnPowderSnow`); there is no boots tag, and the contract said to drop the claim rather
  than add a mixin. A kit wearer can still wear leather boots with other kit pieces, and be both freeze-proof and able
  to walk on powder snow. The alternative for the owner: a mixin reading a `#jugcraft:powder_snow_walkable_boots` tag.
  Since main's #475, the Jimtafacts snowshoes walk on powder snow by their own mixin (`JimtaSnowMixin`); that is
  Jimtafacts', not the kit's, and `check_rime_kit` allows that one file and no other.
- **The bangle is a Brace,** the plan's "short Brace accessory" (02:168; B-P25), not the owner's chill on every hit.
- **The foci exceed the Initiate's Wand's duration limit,** for the reason under Balance (B-P50).
- **No JEI acquisition pages for the kit:** each item has a crafting recipe, which JEI already shows; the
  `acquisition_info` pages are for ritual and boss loot.
- **The medium model is reused** (Ember's import) for the light and medium sets, and Ember's slots, `EmberArmorItem`,
  renderer and slot writer are shared (B-P51).

## 3. Thallite and the trophies (B-10; B-P26, B-P28, B-P10)

### Player experience

- **A Frost boon slow is elective cold work.** A Rimekeeper (Rimekeeping understood) whose Frost boon arm slows a
  hostile creature at normal tolerance does one kind of cold work, counted once: it can stand for one of the three kinds
  Rimekeeping's mastery asks for, so Rimekeeping can be mastered without casting Rimebind (B-P53). Since unit 5 it
  shares that one kind with drawing the Yeti King's charge into an ice column, so after either the other adds nothing
  and a Preserve is always needed (B-P56, refining B-P53). The arms are the Yeti King's **Glacier Maul** and
  **Rimeclaw**, and the creative-only **Frost Knight Greatsword**: the boon counts, not the line (B-P52), and no
  survival route is added for the greatsword (B-P27).
- **The boon itself is unchanged:** Slowness II for 3 seconds, renewed by each hit, outside Rimebind's thaw. Its
  numbers, tooltip and the trophies' drop are as they were.
- What does not count: a slow on a pig or another creature that is not hostile, on a resistant creature (a ravager, the
  Yeti King, the Cinder Tyrant), or by someone who does not understand Rimekeeping; a killing blow; another boon; the
  Ice Breaker's slow; any slow while the Concordance is switched off (the boon still slows).
- **Thallite** links through unit 3: raw Thallite is a Stratawrighting specimen, and a blow taken in Earthbound Thallite
  on natural ground is its `rooted` elective. Unit 4 adds no recorder and changes nothing there.
- The Rimekeeper advancement, the codex's Understood page and the player guide's table name the elective.

### How it works

- `tools/concordance_rime.py`: `RIMEWORK_KEYS["frost_boon"]` needs "a Frost boon arm" (words, not a research id), which
  makes it an elective (`elective()`); the mastery's three kinds are the others, and `check_progression`'s `rimework`
  route skips electives. Since unit 5 the key is `elective` (`Rime.KEY_ELECTIVE`), which the Yeti King's column records
  too, so the two stand for one kind and every mastery still needs a Preserve (B-P56; `check_rime`).
- `weapons/ArmItem.boon`, the FROST case: the boon's own `addEffect` is kept, and only when it took does it call
  `Rime.frostBoon(attacker, target)`.
- `Rime.frostBoon`: with the Concordance switched on, for a server player wielder and a living target
  `Rimebind.restrains` (a hostile creature at normal tolerance, the predicate Rimebind's restraint now shares, B-P34),
  it records `practice:jugcraft:rimework:frost_boon` (`practice:jugcraft:rimework:elective` since unit 5, B-P56). It
  only records: no effect, push or attribute.
- Thallite: unit 3's `AFTER_DAMAGE` listener in `Strata` records `rooted`, reading Thallite's own ground tags
  (`ThalliteGear.LIVING_GROUND`, `EARTHEN_GROUND`). Nothing is added to them (B-P28).

### No number changed (B-P28)

- `check_winter_links` holds it in the tree: the FROST case puts on exactly `ArmVariants.FROST_TICKS` and
  `FROST_AMPLIFIER` (which `check_arms_variants` holds to `tools/arms_variants.py`) and only reads whether it took;
  `Rime.frostBoon` only records and is called only by `ArmItem`; Rooted is recorded once, by `Strata`; the Concordance
  only reads Thallite's gear and ground; `earthen_ground` keeps `#jugcraft:living_ground` and gains nothing cold; the
  trophies named in the elective's words are the ones `tools/arms_variants.py` drops.
- Against the unit's base (`06dc5ea5f`, unit 3's head), run on 14 October 2026:
  - `git diff --exit-code 06dc5ea5f HEAD -- tools/arms_variants.py gear/ThalliteGear.java gear/ThalliteGearItem.java
    gear/JugcraftGear.java weapons/ArmVariants.java tags/block/{earthen,living}_ground.json
    tags/item/{earthbound_armor,thallite_gear}.json loot_table/bosses/yeti_king.json` (Java and data paths in full):
    **exit 0, no difference**.
  - `tools/gear.py` differs only by four added lines, the travel kit's pieces in the vanilla armour slot tags (part 2);
    its Thallite tables (the ground lists and the gear's numbers) are unchanged.
  - `git diff -U0` of `weapons/ArmItem.java` shows only the `Rime` import, the Javadoc sentence and the FROST case.
  - CI's "Check generated JSON is up to date" keeps the generated tags and lang equal to the unchanged generators; it
    passed in run 38549876709's `mod` job on `b012875ab`.

### Save effects

- One more evidence key, `practice:jugcraft:rimework:frost_boon`, in the existing knowledge record; no schema or codec
  change. An older build keeps it as a distinct rimework key, so a Rimekeeper mastered through it stays mastered. Unit 5
  renamed it `practice:jugcraft:rimework:elective` before any release (B-P56); a world saved by this unit's unmerged
  build keeps a `frost_boon` key as a kind of its own, not migrated.
- The trophies, Thallite and their tags are unchanged, so nothing else saves differently.

### Known limits and the owner's question

- Rooted and Regrowth stay inert on snow, ice and the Glacier Hall's floors: they are not natural ground (B-P28).
- `weapons` now imports `concordance.rime`, as the contract foresaw for this hook.
- **B-P10, for the owner:** should every slow from the Concordance and the trophies share Rimebind's thaw (3 seconds
  slowed, then 2 free), or keep their own? Today the trophies' Frost slows for 3 seconds and each hit renews it.
  Recommended: keep their own; a shared thaw would change the accepted trophies and the other slows (Stitch, Arms II's
  DAZE, the maul's Quake, Dazzle and the Ice Breaker). `aFrostBoonStaysOutsideTheThaw` is the evidence for today's rule.

### Verification

Game tests added (`ConcordanceWinterGameTests`); all passed in CI run 38549876709:
- `aFrostBoonSlowIsElectiveRimePractice`: each Frost arm counts once for a Rimekeeper striking a zombie, with the
  boon's own slow; the key survives a knowledge round trip before mastery; with two kinds of Preserve it masters
  Rimekeeping; a novice, a pig, a resistant ravager, the Venom boon and a killing blow count nothing; with the
  Concordance switched off the boon still slows and nothing is learned.
- `aFrostBoonStaysOutsideTheThaw`: the boon's slow starts no thaw and is not refused by one; a hit renews it to its
  full time and leaves Rimebound and the thaw as they were.
- `thalliteStaysOutOfTheCold`: snow, powder snow, ice, packed and blue ice and the hall's drift snow, trampled snow and
  glare ice are not Thallite's natural ground, and Strata reads no ground from them.

Unchanged, and passed again in run 38549876709: `ArmsVIIGameTests.effectBoonsTakeOnTheFoe`, `ThalliteGearGameTests`,
and unit 3's `anAllyARitualAndRootedThalliteAreElective` and `thalliteIsAStrataSpecimen` (among the 1676 required);
`ArmsVIIClientGameTests` by its shard (0).

Audit: the new `check_winter_links`.

Implemented with Claude Code; the model is named in the commit trailers.

## Checks run for the unit

The first six columns were run on 14 October 2026 where the unit was written. No Minecraft jar is available there, so
no Minecraft build was compiled or run there. The last column is CI's run 38549876709 on `b012875ab` (units 1 to 4,
the unit's head; below).

| Check | A `158717e6c` | B `4be454f83` | C `eb6099144` | E `7e1efb5f9` | Records `7605c420b` | Review fixes | CI `b012875ab` |
|---|---|---|---|---|---|---|---|
| `python3 tools/owner_art.py --check` | passed | passed | passed | passed | passed | passed | not run alone; `check_mod_data` reads its errors, passed |
| `python3 -B tools/check_mod_data.py` | passed | passed | passed | passed | passed | passed | passed (`mod`, "Check material data") |
| `python3 scripts/check_repository.py` | passed | passed | passed | passed | passed | passed | not run (not in the Build workflow) |
| `python3 tools/concordance_journey.py --check` | passed | passed | passed | passed | passed | passed | not run alone; `check_mod_data` reads its problems, passed |
| `python3 tools/check_icon_maps.py` | passed | passed | passed | passed | passed | passed | not run alone; `check_mod_data` reads its errors and self-test, passed |
| `./gradlew build` (compilation and the server game tests) | not run | not run | not run | not run | not run | not run | passed (`mod`, and `optional integrations absent`): all 1676 required |
| `./gradlew runClientGameTest` | not run | not run | not run | not run | not run | not run | passed: every client test class (164), five shards |

For this record the gate was run on an export of each commit (`git archive`, outside the repository) and on the
working tree of this commit. `check_mod_data` counts 2558 material ids at commit A and 2573 from commit B (the fifteen
kit items). `check_icon_maps.py` passes with the same 27 warnings before and after the kit, none of them about its
icons (they are the owner's, not icon maps). The generated files come from `tools/owner_art.py` and
`tools/generate_material_data.py`; `tools/generate_textures.py` is not used by the unit (nothing here is drawn). The
delivery matrix (commit B) and the player guide's generated block (commit C) are pasted from their generators, which
`check_mod_data` holds them to.

**Mutation tests.** Each audit was mutation-tested by a script written with its commit, re-run for this record on a
fresh export of that commit (outside the repository, each fault restored after its run); the clean export passed each
time, and every injected fault below was reported (60 in all):

- **`check_winter_brine` and `check_alchemy`** (commit A; 13): the warm keep changed in Python only; the Rime line in
  `Mixture` deleted; Rime's status back to `minecraft:resistance`; its ticks a unit halved; its status
  `jugcraft:frostwarden` in the generator and, separately, in the data; `Frostward.warded` taken out of the Yeti's
  chill; `Frostward.register()` removed; a temporary `yeti_fur → snowball` recipe; the icon removed from `OWNER_FILES`;
  the `thawed` words deleted and, separately, `Outcome`'s `thawed` reason; the test's `kept` literal changed. The two
  generator status changes stop `check_mod_data` before its report (the codex's property table has no name for them,
  a `KeyError`), and `check_winter_brine` called directly reports each.
- **`check_rime_kit`, `check_acquisition`, `concordance_journey.py` and `owner_art.py --check`** (commit B; 24): the
  twenty planned (a focus's hold in Java; Nereid's chestplate at 3; toughness in the material; an attribute on a piece;
  the bangle's Context with an actor; an `addEffect` in `RimeGear`; a saved attachment; `Rime.preserve` back to its own
  minute; the cosmetic check dropped; blaze powder and Yeti Fur in recipes; Nereid's hood on the Pyromaniac's grid; a
  piece out of the freeze tag; the name "Nereids Hood"; Winterguard's sheet swapped for the medium one; a focus out of
  `SLOT_ITEMS`; a piece out of `EXCLUDED`; a mixin naming `PowderSnowBlock`; the bangle's Brace at 120 ticks; a piece
  out of `FIRST_LOADOUT_RECIPES`), the bangle's Brace at 120 in Python too, a bone renamed in the runtime model, one
  byte changed in an icon, and a piece made from blaze powder on disk.
- **`check_winter_links`** (commit C; 15): the boon's ticks doubled; `if (slowed)` removed; a thaw put on in
  `frostBoon`; a second caller in `Wayfaring`; `frost_boon` needing nothing; the Java key removed; a second `rooted`
  recorder; a call to `ThalliteGear.root` from `Strata`; a snow block in `EARTHEN_GROUND`; living ground taken out of it; the
  Rimeclaw renamed; and four more (the elective needing a research, `check_progression` without its elective filter, a
  second slow in the FROST case, `Rimebind.restrains` dropped from `frostBoon`).
- **The dossier's fact** (commit E; 8): an `out.recipe(` in `prepare`; `prepare` not called; the status read as a
  literal; the codex link at understood; its research as a constant; either of the two lang lines missing; an
  attachment written from `prepare`.

**Review fixes** (the unit review, 14 October 2026; the gate in the Review fixes column, on the working tree). Faults
injected on a copy outside the repository, each restored after its run, were all reported (10 of 10): `Rime.frostBoon`
without the switch, and without its living target; `RimeGear.wearsBangle` without its cosmetic check; `RimeGear.bangle`
keeping its count when the bangle is taken off; and Yeti Fur reaching a Rime-bearing ingredient by a Scale form of
snowballs, a recipe taking it through a tag, the same through a tag inside a tag, a villager trading wool for amethyst,
the plain `yeti_fur → snowball` recipe, and a recipe making gloamcap from sugar. The audit as it was before the fixes
reported only the plain recipe of those six routes. Forty of section 9.5's faults for the three new audits were re-run
against the fixed audits, all reported: 38 as first written; the name "Nereids Hood", which in the table alone is left
to CI's generated-file check, once it reaches the language file as the generator writes it; and Winterguard's sheet
swapped for the medium one, by `owner_art.py --check` (in the table) and by the fit (on the runtime sheet).
Not re-run here, as the fixes do not touch their audits: the faults caught by `check_ember_regalia`,
`check_acquisition`, `check_rime`'s key mirror, `owner_art.py --check` alone and `concordance_journey.py`.

**CI, run 38549876709** (workflow_dispatch on `b012875ab`, units 1 to 4; not merged): all nine jobs succeeded, the
unit's first CI run. `mod` passed "Check generated JSON is up to date" and "Check material data", then logged "All 1676
required tests passed" ("1676 GAME TESTS COMPLETE IN 4.293 min"; "BUILD SUCCESSFUL in 6m 24s");
`optional integrations absent` logged the same in 4.319 min ("BUILD SUCCESSFUL in 7m 6s"). The server log names no
test that passed, but each of the unit's 18 new server game tests (`ConcordanceAlchemyGameTests` 4,
`ConcordanceWinterGameTests` 10, `YetiKingGameTests` 2, `EncyclopediaGameTests` 1, `ConcordancePersistenceGameTests` 1)
is a `@GameTest` in a class the gametest `fabric.mod.json` registers, and no test sets `required = false`, so each
passed as one of the 1676; so did the two changed ones (`theCrucibleKeepsPreservesHoldThroughASave`,
`theFirstLoadoutIsMadeInSurvival`). Unit 2's run 38543250503 counted 1658 on `1266c905a`, the commit beneath the unit,
which adds 18 `@GameTest`s and removes none. `choose client tests` chose every client test class (164, this one
included) in five shards, and each shard succeeded; a failing class stops the client with its error and fails its
shard, so `ConcordanceWinterClientGameTests` passed in shard 4, every check in it held: the owner's icon, the GeckoLib
renderers, models and sheets, the row dressed as the shots need, the sneak, his frost taking no health in Winterguard's
while slowing the player and taking some bare, a Focus of Water's 2400-tick hold and the bangle's Brace. Both server
logs re-run three tests by design ("Starting re-run of tests": the lair's last rites, Arms VIII's javelin and
Tatterlace's brood, which ask for several successes). Their only WARN lines are the game-test server's usual ones (a
test record saved by a newer version, a spire entry kept as written, and "[lairs] No dimension …: the lair cannot open"
for five lairs, the Glacier Hall among them); no WARN or ERROR line names Frostward, Winter, Brine, Rime, `RimeGear`,
the bangle, a focus, Winterguard, Aquamancer, Nereid or the alchemy. Read through the GitHub API: the last 5000 lines
of each server log (of 6322 and 6091; they hold the game tests and their summary), `choose client tests`, and shard 4's
last 5000 lines (of 19785: the screenshots, but not the class's own log lines).

The screenshots (480×270 previews in shard 4's log; for people to look at, not evidence):
- `jugcraft_winter_frostward_hud`: the player in survival on a flat grass field at noon, full health and hunger; at the
  top right one status icon, Frostward's (a pale cyan heart in a frosted ring, as `frostward.png`). A chat line ("You
  got the Creative Quick Start book…") and the book in hand are the test world's.
- The row by day (`jugcraft_rime_kit_front`, `_three_quarter`, `_side`, `_back`), left to right from the front: the
  iron control; Nereid's and Aquamancer's, alike as expected (a wide-brimmed dark teal hat with yellow trim and a red
  tassel; a teal coat with yellow shoulder stripes and Greek-key marks over a pale blue front; dark blue legs and
  boots); Winterguard's (the same coat and legs, as its sheet's top half is the medium sheet, under a white helm with a
  dark visor); Winterguard's with Protection IV, whose glint cannot be told apart at this size (in the back view it
  looks slightly bluer than the plain one); Winterguard's on the zombie, its green face under the open helm and its
  raised arms in the teal sleeves, the shoulder plates on; and the small stand, bare wood with nothing drawn on it. The
  row turns as the test sets it in each view.
- The close-ups (`jugcraft_rime_kit_close_*`): the three sets fill the frame from each side; the stands' wooden neck
  and hands show under the hats and at the cuffs; the brims and tassels, the shoulder marks and the backs' teal-to-blue
  fade are drawn. Nothing is magenta, missing or cut off.
- `jugcraft_rime_kit_player`, `_player_sneaking` and `_player_sneaking_back`: the player in third person in
  Winterguard's, the white helm above the face, the coat with a large yellow Greek-key mark on the left shoulder, the
  book in hand; sneaking, the body bends and the coat follows it; from behind, the helm's white back and the coat.
- `jugcraft_rime_kit_frost_hud`: vanilla's frost at the screen's edges, the ten hearts drawn frozen and all full, and
  the armour bar at 15 points (seven and a half icons, Winterguard's 2/6/5/2).
- `jugcraft_rime_kit_bangle_brace`: five advancement toasts (Cold Against Warmth, Rimekeeping, Against the Dark, First
  Frost, A Glow of Its Own), raised by the research the test grants, cover the top right, where the HUD draws status
  icons, so Braced's icon is not seen (the server's check that the bangle braced the player passed). A large purple
  pixel shape stands in the middle of the view. Identified in unit 5 from the test's code: it is the shared effect
  boundary's mark for the Brace the bangle had just put on the player (`ConcordanceEffects.apply` shows `Sign.CHARM`,
  which `SignClient` draws as vanilla `minecraft:witch` particles around 60% of the wearer's height). The shot was taken
  in first person within 25 ticks of the bangle's once-a-second look bracing the player; those particles live 8 to 40
  ticks and float upwards, so they rise through the camera from inside the player's own body. By the code it is expected
  presentation, not a missing texture or a render defect; others see it round the wearer's body. Since unit 5 the test
  clears the toasts before this shot. No shot shows the foci or the bangle in their slots: the test fills the slots on
  the server and checks them there. Since unit 5 it also opens the survival inventory, where Trinkets shows its slot
  groups, for `jugcraft_rime_kit_slots` (Trinkets may draw one slot of a group until it is hovered, so the shot may not
  show both, and the server's checks remain the proof). In run 38591108210 (attempt 2, shard 4) the bangle's shot has
  no toasts: Braced's icon stands alone at the top right, and no purple shape is in view; the slots shot shows the
  survival inventory with the armour slots empty (the test took the armour off before) and, beside the paper doll, a
  column of four slots with grey outline icons and no item in any, so it shows neither the focus nor the bangle.
- `jugcraft_rime_kit_icons`: fifteen item frames on a wooden wall, the lesser focus, the focus, the bangle and Nereid's
  four above, Aquamancer's and Winterguard's eight below; each icon whole in its frame and, at this size, like its
  owner's file.

Not run: a client by hand (the icons, the worn sets, the codex and JEI pages, the dossier), the Trinkets screen's slots
(no client test opened it in this run; unit 5's slots shot, run 38591108210, shows no item in it), a person looking at the screenshots
(the descriptions above are of the previews, written for this record), the full-size screenshots (in the run's
`client-screenshots-4` artifact, not downloaded), two clients on a dedicated server, and play by hand.

Confirmed by CI's compilation and passing tests (they had been read from the decompiled 26.3 sources or the 26.3 files
fetched for the unit): `MobEffectCategory.BENEFICIAL`, `EntityTypes.RAVAGER` in a test and
`PowderSnowBlock.canEntityWalkOnPowderSnow`. Not confirmed: the game rule id `minecraft:natural_health_regeneration` in
the client test (Fabric's `runCommand` does not fail on a rejected command, and the part of shard 4's log holding the
class's run was not read). `AttachmentType.isPersistent()` is already asserted by earlier tests (`FacePaintGameTests`,
`LairEncounterGameTests`).

## World and event applicability

No worldgen, creatures, loot or seasons. Snow comes from cold biomes and Jugcraft's winter snow, which leave nothing
behind when the season ends; snowballs also come from snow golems anywhere. The Glacier Hall needs a Frost Horn, whose
recipe sits under the `agriculture` switch. The Yeti King is unchanged but for his chill asking for Frostward, and the
hall is untouched.

## Rollout and open questions

- **New stable ids:** the mob effect `jugcraft:frostward`; items `jugcraft:lesser_water_focus`, `water_focus`,
  `water_bangle`, `nereids_hood`, `nereids_tunic`, `nereids_pants`, `nereids_shoes`, `aquamancers_hat`,
  `aquamancers_robes`, `aquamancers_leggings`, `aquamancers_boots`, `winterguards_helmet`, `winterguards_chestplate`,
  `winterguards_leggings`, `winterguards_boots`; equipment assets `jugcraft:nereids`, `jugcraft:aquamancers`,
  `jugcraft:winterguards`; GeckoLib model `jugcraft:armor/winterguards`; tags `jugcraft:repairs_nereids_gear`,
  `jugcraft:repairs_aquamancers_gear`, `jugcraft:repairs_winterguards_gear`; the evidence key
  `practice:jugcraft:rimework:frost_boon` (`elective` since unit 5, B-P56); the non-saved attachment
  `jugcraft:water_bangle_braced`; codex entries `alembic/winter_brine` and `winter/travel_kit`. Additions to
  `#minecraft:freeze_immune_wearables`, the four vanilla armour slot tags, the Trinkets tags `chest/spell_focus` and
  `hand/bracelet`, and `#jugcraft:equivalence/excluded`.
- **Changed for everyone:** the Rime property gives Frostward, not Resistance; warmer stirs thaw Rime in amethyst and
  gloamcap brews too.
- **Save compatibility:** additive; no migration. Rime bottles made before keep Resistance; formulas replay.
- **Downgrade:** an older build does not know `jugcraft:frostward`, so its draughts do nothing there and count as
  harmful doses; meeting the unknown effect it may drop all of a creature's saved effects (see
  [unit 3's downgrade guidance](arcane-concordance-rime-strata.md#rollout-and-open-questions) for the modifiers that
  can stay behind); it drops the fifteen items, as any unknown id. Back up a world before moving it to an older build.
- **Disable behaviour:** with `concordance.enabled=false` the kit's recipes do not load, crucibles refuse work, and the
  bangle, the foci, Frostward brewing and the Frost boon's practice do nothing (the boon still slows: arms work whatever
  the switch); Frostward, the items and the attachment stay registered, and the
  pieces stay freeze-proof (the tag is not switched, as with Ember).
- **Known limits:**
  - the draught is named "Draught" and its tooltip reads "Frostward I (48.75 s)"; "Winter Brine" appears in JEI, the
    codex and the dossier. Naming the item is an option at the checkpoint. Since Concordance Chunk C (C-02, C-P2; not
    merged), a draught of Frostward carrying nothing harmful is named Winter Brine on its bottle (vanilla's item name);
    bottles made before keep "Draught" and do not stack with named ones;
  - Frostward reaches beyond the Yeti: it also clears the Frost Horn's arrival chill and a snow werewolf's frostbite
    chill (the werewolf's Slowness stays);
  - Nereid's and Aquamancer's look alike when worn (the owner's sheets are the same bytes); the "aqua" art reads as
    water, as the owner chose (B-D2); trims are kept on the item but not drawn, as with Ember; the foci and bangle are
    not drawn on the body; `frozen.png`'s meaning in the owner's set differs from Frostward's;
  - the bangle did nothing against the Yeti King's own push until B-09; since unit 5 his push calls `Brace.spend`, so
    its Brace halves one of his pushes and credits no one (passed in CI, run 38591108210);
  - the kit shares `EmberArmorItem`, `EmberClient` and Ember's slot writer, so renaming them touches the kit;
  - one more GeckoLib armour model, and about 200 more client-test ticks plus the shots
    (`ConcordanceWinterClientGameTests` has an estimated weight of 120 in `tools/select_client_tests.py`);
  - the client test's bangle shot is covered by the advancement toasts its research grant raises (run 38549876709), so
    it does not show Braced; unit 5 clears the toasts before that shot, as unit 3's test does (`fc9b404dd`; run
    38591108210 shows Braced; [the contract](concordance-chunk-b.md#b-11-evidence-and-the-owner-checkpoint)).
- **Deferred:** the owner's light model (no light-layout water sheet for it); the Caster Tome of Water (a tome casting
  one invocation needs a new instrument gate); the foci and bangle drawn on the body; the owner's on-hit bangle (B-P49's
  alternative); powder-snow walking boots (B-P46's alternative); a Rimekeeping elective for bottling Winter Brine
  (it would change `CrucibleBlockEntity`, a hot spot). The Yeti column elective came with B-09 in unit 5, as the same
  key as the Frost boon's slow (B-P56).
- **For the owner's checkpoint:** B-P40 to B-P53, among them the icon (B-P43), the set numbers and recipes (B-P47,
  B-P48: Aquamancer's in wool and gold instead of lapis), the bangle's role (B-P49), the boots (B-P46), any Frost boon
  arm counting (B-P52), the elective's gate (B-P53, refined by B-P56: one key with the Yeti King's column), and
  B-P10's question above.
