# Arcane Concordance: celestial cycles and attunement

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 15) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 7, Practitioner stage (Jugcraft Workshops). Roadmap step 15.
Primary specialty and supported player role: Starwatchers. The sky is a calendar to read and plan around: what rises
when, what it needs, what it gives, and a stored way round waiting that never beats the sky's count.

Builds on [typed resources](arcane-concordance-sharing.md) (Astral Resonance and its ledger), [research and
notes](arcane-concordance-sharing.md) and the [shared effect system](arcane-concordance-composition.md). Keeps to
Jugcraft's established calendars: the overworld clock the sun, moon and grandfather clock follow, the seasons
(`season/SeasonCalendar`) and the Halloween event's Harvest Moon (`agriculture/HarvestMoon`). Contract and checklist:
[ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

Once First Light is understood, examining an instrument of the sky (spyglass, clock, compass, phantom membrane, glow ink
sac) begins **Celestial Attunement**; studying one at a Lampwright's Bench (or reading someone's notes) understands it.

**The calendar.** The sky follows the world's own clock: a day is 20 minutes, night runs from 19:00 to 05:00, and the
moon goes from full to new and back every 8 days, exactly as vanilla draws it. Six **patterns** come round on it:

| Pattern | When | Weather | Pays | Attunement (while up) | Attune / recall |
|---|---|---|---|---|---|
| The Full Moon | the full moon's night (day mod 8 = 0), 19:00 to 05:00 | clear | 4 | Moonsight: night vision | 4 / 12 |
| The New Moon | the new moon's night (day mod 8 = 4), 19:00 to 05:00 | any | 4 | Spring Tide: water breathing | 4 / 12 |
| The Lantern Star | every 3 days (day mod 3 = 1), 18:00 to 20:00 | clear | 2 | Lantern Hands: haste | 2 / 6 |
| The Echo Comet | every 19 days (day mod 19 = 7), 20:00 to 04:00 | clear | 8 | Comet's Wake: speed | 6 / 16 |
| The Winter Crown | in winter, every other night, 21:00 to 03:00 | clear | 3 | Frostmantle: fire resistance | 3 / 9 |
| The Harvest Moon | the Halloween event's Harvest Moon nights | any | 6 | Feast Favour: hero of the village | 4 / 12 |

**The forecast.** Using an Orrery Observatory with an empty hand, or `/jugcraft concordance sky`, reads today (the day,
the time, the moon's phase and the season) and every pattern up now or rising in the next 8 days: when it rises (or
until when it is up), the weather it needs and the time of year it keeps to.

**The Orrery Observatory** (spyglass, clock, gold and copper) stands under the open sky in the Overworld. Placed by a
Starwatcher (someone who understands Celestial Attunement) it is aligned to them; otherwise a Starwatcher's empty hand
aligns it. Every 5 seconds it looks up and gathers **Astral Resonance** from each pattern up and in view, up to 32,
**once each time the pattern comes round** for its keeper, however many observatories they keep. It says why it waits:
nothing up, unaligned, clouded over, something hiding the sky, not in the Overworld, full, already gathered, or too soon.
Its telescope sweeps slowly while idle and lifts to track a pattern it can see.

**The Astrolabe** (gold, a compass, an amethyst shard) draws resonance from an observatory its keeper's party may use
(use the astrolabe on it), up to 32. Used where a risen pattern can be seen, it spends the pattern's cost and **attunes**
the Starwatcher to it: the pattern's effect is theirs while it stays up, at most 8 minutes (vanilla's longest potion);
use it again to renew. Death ends an attunement. A charged astrolabe glows in hand with LambDynamicLights installed.

**Recall** is the stored alternative to waiting: a master (three different patterns observed through an observatory)
may attune when nothing is up by recalling the pattern they last observed. It costs about three times as much, lasts 2
minutes, and is allowed once each time that pattern comes round.

## How it works

**Data** (`data/<ns>/concordance/pattern/`, read by `CelestialCatalog` in `concordance/celestial`): a pattern's
Principle, period and offset (its days: `day mod period = offset`), its hours (`from` and `to`, ticks of the day), whether
it needs a clear sky, the resonance one occurrence pays, its attunement (a status effect, amplifier 0 or 1, the cost and
the dearer recall) and an optional season. The loader refuses an offset outside the period, hours that do not run
forward, an unknown Principle or season, a recall no dearer than attuning, more than 16 resonance or a cost over 32.

**The calendar** (`celestial/Calendar.java`, pure arithmetic): the world time is `level.getOverworldClockTime()`, the
clock vanilla's sun and moon (and Jugcraft's grandfather clock) follow. Day `d = floor(time / 24000)`; the moon phase is
`d mod 8` (0 full); a pattern's `k`-th **occurrence** is its window on day `offset + k * period`, and
`occurrence(time) = floor((d - offset) / period)` names the latest on or before any time. Forecasts list the windows
open or opening within a horizon (at most 32 days), soonest first.

**Monotonic rewards** (`resource/AstralLedger.java`, saved per player in `AstralClaims`, the world's
`data/jugcraft_astral_claims.dat`): for each pattern the ledger keeps the latest occurrence that paid and the game time
it did. A new claim must be for a **later occurrence** (turning the clock back only reaches occurrences that have
already paid) and at least **half the pattern's period of game time** after the last claim (game time runs only forward
with the world and no command moves it, so turning the clock forward skips to a later occurrence but cannot make it pay
sooner than the world has really run). Half a period lets a player who sleeps through nights still meet every
occurrence. Recalls use the same ledger under `recall:<pattern>`.

**Observation conditions** (`Sky.obscured`, on the server): the Overworld, the open sky over the block (or player):
nothing that stops movement anywhere above it by the server's heightmap, so glass and leaves count as a roof (the
heightmap changes the moment a block is placed; sky light can lag a tick behind), and no rain for a pattern that needs a
clear sky; the pattern's season (Jugcraft's season from the server's date, any season
when seasons are off; the Harvest Moon only while `HarvestMoon.rising` says so).

**Collection** (`ObservatoryBlockEntity.gather`, every 100 ticks, staggered by position): for each pattern up and in
view, if its resonance fits, a claim against its keeper's ledger; the status is the most useful of the outcomes.
Resonance leaves only into an astrolabe, through the shared transfer rules, for the keeper's party.

**Attunement** (`Sky.pulse`, every 100 ticks over online players): a player's attunement (a saved attachment, not kept
through death) gives its effect for 140 ticks through the shared effect boundary (`ConcordanceEffects`, helpful, the
strongest of what they already have) while its pattern is up and in season, or, for a recall, until it ends. Game time,
not the world clock, ends it, so moving the clock neither stretches nor renews it.

## Connections

- Input producer: First Light (Celestial Attunement needs it understood); vanilla instruments; the sky itself.
- Output consumer: Astral Resonance is the Concordance's celestial-alignment resource (separate from Focus, Ley Charge,
  essences and Vitae); attunements now; later steps (relics, the endgame project) can draw on stored resonance.
- Technology connection: the observatory and astrolabe are crafted from copper, gold and vanilla instruments.
- Magic connection: patterns carry Principles; attunements go through the shared effect boundary and its limits.
- Reachable entry path: First Light understood → examine a spyglass, clock or compass → study at the bench → craft an
  observatory and wait for a night (the full moon comes every 8 days, the Lantern Star every 3).
- Solo, trade and cooperative routes: entirely solo; a party shares a keeper's observatory store; astrolabes carry
  resonance and can be traded.
- Specialty use without other branches: needs only First Light.

## Balance

- **No reward repeats**: each occurrence pays a keeper once, whatever the clock does; at most one occurrence per half
  period of real play. The step 15 harness turns the clock back and forth at random for 2.4 million game ticks: 5 full
  moons pay (the most possible is 26).
- **Stored, never better than the sky**: a recall costs about three times the attunement, lasts 2 minutes against up to
  8, needs mastery and the pattern's last observation, and is allowed once per occurrence.
- Effects are vanilla potion effects at level I, no longer than vanilla's longest potion (8 minutes), only while the
  pattern is up; recalls 2 minutes.
- Resonance: an observatory and an astrolabe hold 32 each. The rarest pattern (the comet, every 19 days) pays 8.
- **Simulation budget**: an observatory looks up every 5 seconds (6 patterns, a heightmap read and the weather); the
  pulse runs every 5 seconds over online players with an attunement. Nothing loads a chunk or runs per tick.

## Multiplayer and persistence

- Server authority: the clock, the weather, the sky over a block, seasons, claims, resonance and effects are the
  server's. Clients receive an observatory's resonance, status and the pattern it tracks (for its animation and Jade)
  and an astrolabe's charge; the rendered sky, shaders and client settings never decide timing, eligibility, collection
  or rewards. Uses are rate-limited.
- Ownership: an observatory's keeper (who aligned it) owns its claims; only their party may draw its resonance.
- Persistence: claims and observations in world SavedData; an observatory's resonance, keeper, status and tracked pattern;
  an astrolabe's charge as an item component; an attunement on the player (lost on death).
- Seasons: seasonal patterns never gate progression (mastery needs three of the four patterns that keep to no season);
  what a seasonal pattern paid stays paid after its season ends.
- Disable behaviour: with `concordance.enabled=false` observatories stop ("switched off"), astrolabes do not attune and
  pulses stop; everything stays registered.

## Dependencies and assets

No new dependency. Framework use:

- **GeckoLib**: the observatory's telescope (idle sweep; tracking while it can see a pattern), from the status the server
  sends.
- **Modonomicon**: a new **Sky** codex category (Celestial Attunement, the calendar with every pattern, the observatory,
  attunement and recall), written from the same tables as the data.
- **Jade** (optional): an observatory's status, resonance and the patterns in view now. Without Jade, an empty hand says
  the same in chat.
- **LambDynamicLights** (optional): a charged astrolabe glows in hand (`assets/jugcraft/dynamiclights/item/astrolabe.json`,
  keyed on the `jugcraft:resonant` component). Client light only; it never changes world light.
- **GuiLib**: not built. A GuiLib observatory (forecast charts and attunement controls) is planned with the other
  screens in step 26; the forecast message and command are the route every player has.

Art: two 16x16 item icon maps in `tools/item_icons/` (astrolabe in vanilla gold with an amethyst rete; observatory in
copper, glass and dark wood). The observatory's GeckoLib sheet comes from `tools/concordance_celestial_art.py`.
**Provenance:** the sheet is painted only in the colours of the owner's library
`art/owner-library/originals/Blocks/Airships and Planes/entity/telescope.png` (brass, dark wood, iron and lens; the iron
ramp extended by one darker and one lighter tone), checked by `tools/check_mod_data.py`; no pixels are copied. The
library file is read, never changed. The icons have not been shown to the owner yet.

## Verification

- `python3 tools/check_mod_data.py`: new step 15 checks (`check_celestial`): the Java calendar, limits, observatory,
  astrolabe and pulse numbers equal the generator's; the seasons and the Harvest Moon's night agree with the established
  calendars; the sky reads the overworld clock and no sky class imports client code; every pattern keeps its limits, has
  an allowed effect, a dearer recall and its name; mastery is reachable from patterns that keep to no season; every
  status and message has its text; the icons are their maps; the GeckoLib model, animations and sheet agree, and the
  sheet uses only the owner's telescope's colours. The pure-package rule covers `concordance/celestial`.
- The pure core compiles with JDK 21. The step 15 harness passes **43 checks** against the generated data: the six
  patterns load; the moon phase agrees with the grandfather clock's formula at every time tried; windows, occurrences
  (including before day zero), `next` and forecasts (ordered, bounded by their horizon and by 32 days); the ledger pays
  the same night once, pays nothing for the clock turned back, nothing sooner for the clock turned forward, and the next
  occurrence after enough game time; a month of random clock jumps pays at most once per half period; attunements end by
  game time; and the parser refuses each kind of bad pattern.
- Game tests added: `ConcordanceSkyGameTests` (six): the sky keeps the world's clock (the grandfather clock's moon, the
  Harvest Moon's nights, the forecast and clock text); an observatory gathers each occurrence once (the same night, the
  clock back a week, forward to the next full moon and far beyond pay nothing; a week of game time later the next full
  moon pays; a second observatory of the same keeper gathers nothing; a full one stops); an observatory says why it waits
  (unaligned until a Starwatcher's touch; no sky under a roof); an astrolabe attunes while the moon is up (a novice and a
  poor astrolabe cannot; night vision while it is up, none once it has set, the attunement gone at its end); a master
  recalls once per occurrence (not without an observation, not twice, again after the next full moon; a practitioner
  cannot); and claims, observations and an observatory survive a save. The step 7 resource test now checks the monotonic
  ledger.
- CI: the first push (87acb052) built and failed one server test, `anObservatorySaysWhyItWaits`: the roof was placed
  in the same tick as the check, and the sky-light answer behind `canSeeSky` updates a tick later, so the roofed
  observatory still read open sky. The observatory now reads the motion-blocking heightmap, which updates as the block
  is placed (9b9950d1). Run 37610863921 (commit 0a73cf15) passes the whole Build workflow: it builds, passes the data
  checks and all 981 required server game tests (the six above among them), and the client test shards pass.

Not yet run: any client (the GeckoLib observatory, the icons, Jade lines, the dynamic light and the codex pages in game),
a two-client dedicated server, and a real night of play.

## World and event applicability

No worldgen or creatures. Two patterns keep to established calendars: the Winter Crown to Jugcraft's winter, the
Harvest Moon to the Halloween event's Harvest Moon nights. Neither gates progression, and what they paid stays.

## Rollout and open questions

- New stable ids: block `jugcraft:observatory` and its block entity; items `jugcraft:observatory`, `jugcraft:astrolabe`;
  components `jugcraft:astral_charge`, `jugcraft:resonant`; attachment `jugcraft:celestial_attunement`; saved data
  `jugcraft:astral_claims`; research `jugcraft:celestial_attunement`; practice `jugcraft:observation`; six patterns; item
  tag `jugcraft:celestial_specimens`.
- Save compatibility: additive. The Astral ledger's format changed before release (it was unused outside tests).
- Owner review: the icons and the pattern effects were approved as built on 7 October 2026.
- Open: a GuiLib observatory (step 26).
