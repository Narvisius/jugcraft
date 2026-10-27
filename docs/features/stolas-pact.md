# Stolas's pact

Status: implemented, unmerged (Concordance Chunk D: unit 2, D-03 to D-05; unit 3, D-06 and D-07; unit 5's evidence,
D-09); owner checkpoint answered on 26 October 2026 (the contract's D-D3 to D-D5: keep Stolas, D-P50 accepted, gardens next). Built and tested in CI on the candidate `48d99c904` (run 40133540741: every job passed
on the first attempt; see Verification), with three fixes from that run's pictures since (`b773913c4` and `d00fa2167`; run
40140478331 on `d00fa2167`: every job passed, and the pictures were seen fixed). No person has played it; two automated clients on one machine, no two people or accounts.
Proposal issue: none; the owner's selected scope ([the chunk's contract](concordance-chunk-d.md), D-D0 to D-D2).
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance Chunk D; Practitioner stage, Spiritbinders (tier: practitioner).
Primary specialty and supported player role: the Binding Arts, with the sky's observers. A pact with Stolas, the raven
of the Ars Goetia who teaches the stars, gives its holder a chosen recall of a sky pattern they observed themselves.

Builds on [spirits, familiars and constructs](arcane-concordance-workers.md) (the shared agreement holder, the Bound
Will's pact and escrow from unit 1), [celestial cycles](arcane-concordance-celestial.md) (observations, the astrolabe
and its recall) and [Styxhexenhammer](styxhexenhammer.md). Session decisions D-S1 to D-S23 (decided by the
session under D-D2) and the proposals of units 2, 3 and 5 (D-P17 to D-P50, D-P90 to D-P121, D-P122 to D-P137) are in the
[contract](concordance-chunk-d.md#decisions); none is the owner's.

## Player experience

Stolas's Regard, a Spiritbinders research entry, is found through Styxhexenhammer's hints or without him (the codex's
Binding category, the journal's Pacts section and a feather). With the Binding Arts understood:

- examine a feather or one of Styx's three flowers, harvest a ripe organism crop by hand, and dissolve a sample at an
  Assayer's Scale (in any order; Research Notes never stand in): Stolas's Regard is understood;
- record a sky observation at an Orrery Observatory while a pattern is up and visible (not research: the invitation's
  precondition, which Styx and the journal name apart);
- fill an astrolabe at an observatory you keep (only a Starwatcher, with Celestial Attunement understood, keeps one; a
  party member's observatory or a traded astrolabe also serves): the compact asks 8 Astral Resonance;
- build a **Pact Perch** where you may build and use it: Stolas arrives on it and speaks three beats, then names every
  term before anything is spent;
- **accept**: the Open Hand Compact is paid and the pact sealed in the same server action; **decline**, closing the
  screen or any refusal spends nothing, and a refusal says why;
- once a game day at the perch, **Star-reckoning** recalls a sky pattern you observed yourself, paid from your
  astrolabe; recording a sky observation while the pact is active keeps its first obligation and masters Stolas's
  Regard;
- pay offerings ahead, **suspend**, **resume** or **end** it at the perch or from the roster (ending asks first).

Breaking the perch ends nothing: Stolas leaves the world and the pact, with everything paid ahead, waits until it is
seated at a new perch of its holder's, or ended.

Styx tells each listener their own next step (he teaches and grants nothing). His four lines and his once-a-player gift
of cuttings are unchanged.

Since unit 3 (D-06, D-07): Stolas is a raven. He arrives with a flourish of wings, turns his head to listen while you
talk, looks your offering over, bows to a sealed pact or a resumed one, shakes his head at a refusal and leaves with a
last beat of wings; each with its sound and subtitle, and each answer on the screen marked with a tick or a cross. Jade
shows whether a perch or an anchor keeps a pact, with whom, its service, state and next offering, naming nobody. The
roster (its key not bound at first: Controls, then the Keys page) lists every pact, agreement, worker and courier
request of yours with what each waits for, and suspends, resumes, ends (asked first), cancels or recovers.

## Record fields (CLAUDE.md)

| Field | Stolas's pact |
|---|---|
| Tier | Practitioner, Spiritbinders: Stolas's Regard needs the Binding Arts understood |
| Input producer | a feather or Styx flower examined; a ripe organism crop harvested; a sample dissolved at the Assayer's Scale; one sky observation at an Orrery Observatory; the perch (two sticks, an amethyst shard, a dark oak log, polished deepslate); the Open Hand Compact (2 amethyst shards, 3 feathers, 8 Astral Resonance, which only an observatory's keeper gathers); offerings (`#jugcraft:stolas_offerings`) |
| Output consumer | recalled attunements (2,400 ticks); Stolas's Regard mastered, which the workshop hand needs (D-08); the Starbound Conclave hears `pact_service` |
| Costs | the compact once; a recall's own cost (6 to 16 Astral Resonance); one offering every 7 game days, at most 4 ahead |
| Unlocks | the invitation (Regard understood and an observation); Star-reckoning (the pact); the workshop hand (Regard mastered) |
| Failure behaviour | every refusal spends nothing and names its reason; a missed offering warns, gives a game day's grace, then suspends with nothing lost; a pact never ends by itself; ending returns what was paid ahead once; a broken perch ends nothing; switched off, everything waits |
| Test evidence | below (Verification), every result from CI run 40133540741 unless another run is named |

## Connections

- Tier: Practitioner (Stolas's Regard requires the Binding Arts understood, and nothing else in code: D-P17).
- Input producers:
  - the Binding Arts (understood);
  - ripe organism crops from Verdant Beds (the cultivation practice);
  - the Assayer's Scale (the assay practice);
  - Orrery Observatories (observations; and Astral Resonance, which one gathers only once a Starwatcher keeps it and
    fills into its keeper's party's astrolabes: the compact's 8 come from there, or from an astrolabe a party member or
    a trade gives, D-P123);
  - a feather, or Styx's flowers (the specimens, `#jugcraft:stolas_specimens`);
  - the perch's recipe: two sticks, an amethyst shard, a dark oak log and polished deepslate;
  - offerings (`#jugcraft:stolas_offerings`: Stolas Starflower, Ravenquill Lupine, Amethyst Mourningbell, or an
    amethyst shard).
- Output consumers:
  - recalled attunements (the six patterns' effects, 2,400 ticks each);
  - Stolas's Regard mastered (the first obligation), which unlocks D-08's workshop hand (the anchor seals it only
    then, D-P72);
  - the Starbound Conclave hears `pact_service` once a player, as it hears every recorded practice.
- Technology and magic connections:
  - the shared agreement holder (`AgreementHolder`), the Bound Will's pact, escrow and upkeep rules, and the departure
    history (unit 1);
  - the astral ledger and the one recall path (`AstrolabeItem.recall`, shared with a master's recall);
  - the research engine (examination, practices, advancements, the codex and the journal).
- Unlocks:
  - Stolas's Regard understood: the invitation (with one observation recorded);
  - the pact: Star-reckoning;
  - the first obligation: Stolas's Regard mastered.
- Reachable entry path: `tools/concordance_journey.py`'s pact hall route (`PACT`), nine steps from the Binding Arts
  understood to the pact ended and the workshop hand at work: one player alone in the Overworld (the hand's step in the
  Nether too, for its anchor's soul lantern), nothing past the Practitioner stage and no structure loot (a feather
  stands in for Styx's flowers, an amethyst shard keeps the pact). The compact's resonance makes it understand Celestial
  Attunement (D-P123). `python3 tools/concordance_journey.py --check` holds it, the [journey
  record](arcane-concordance-journey.md) shows it, and `ConcordanceJourneyGameTests.thePactHallFromStyxToDeparture`
  walks it on one survival player; that is a server test, not a played journey.
- Required versus optional:
  - Styx is optional: the feather route reaches every step;
  - offerings have a source without Styx (the amethyst shard);
  - Celestial Attunement is required to seal a pact alone (the compact's resonance has no other source, D-P123); the
    Nether is visited once, for the workshop hand's anchor (D-P124), never for the pact;
  - nothing gates core progression.
- Specialty: the pact is personal, one with Stolas a holder; its holder's party does not benefit. An observer without
  the pact keeps the master's own recall of the pattern they last observed.

## Costs, balance and automation

- **The Open Hand Compact:** 2 amethyst shards, 3 feathers and 8 Astral Resonance from one carried astrolabe, paid by
  the seal itself (`Sealing.affords` and `pay`, unit 1's D-P14) and only once it is recorded. A player with infinite
  materials (creative) pays nothing, as vanilla's crafting does (D-P26). It counts as the first offering.
- **Upkeep (D-S3, Gentle):**
  - 1 offering every 7 game days, from the offerings paid ahead into the pact's escrow (at most 4);
  - an offering due with none paid ahead: a warning, 1 game day of grace, then suspension for upkeep, with nothing
    taken or lost; an offering resumes it;
  - looked at only while the perch is loaded and its holder is online, alive, in its level and within 64 blocks, never
    through a stand-in; time away, unloaded or switched off is never owed;
  - after an absence (over 1,200 ticks without a look) the holder is warned again: a first warning gives the whole
    grace, and one given before the absence gives the grace they had left, its end moved on by the time away (D-P50,
    the unit review's proposal amending D-S3, accepted by the owner as D-D4);
  - two routes around it, found by the unit review (checkpoint question 2): before D-P50, a warned holder away for over
    a minute at least every 20 minutes got the whole grace again at each return and was never suspended, while
    Star-reckoning kept serving (closed by D-P50); and suspending, recalling and resuming once a day moves the due date
    on by the time held, so the 7-day period counts only a few ticks of active time a day (left as built: the owner
    accepted D-P50 alone, D-D4);
  - a pact never ends by itself.
- **Star-reckoning:** the pattern's own recall cost (6 to 16 resonance), 1 a game day a pact, within 8 blocks of the
  perch at any hour, for patterns the holder observed; one recall an occurrence of each pattern, shared with a master's
  recall under the same `recall:<pattern>` claim. The effect lasts 2,400 ticks whatever the sky or the season.
- **Balance, stated plainly (a checkpoint question):**
  - a holder who is not a master and once observed the Harvest Moon may recall it (Hero of the Village, with its
    villager trade discounts) any game day, and the Winter Crown (Fire Resistance) every other day, outside their event
    and season;
  - the once-a-day quota is the only hard limit. Resonance does not bound it: Star-reckoning pays from any carried
    astrolabe that holds enough (`PactCompact.astrolabe`), claims are each keeper's own
    (`ObservatoryBlockEntity`), and astrolabes can be stockpiled, handed over or filled at a keeper's observatory by
    their party, so stockpiled or traded astrolabes allow a recall every day;
  - one keeper's averages, for a player relying on their own observatory alone: it gathers about 2.09 resonance a day
    from the four year-round patterns, 3.59 with the Winter Crown (in winter, or all year with seasons off, when the
    Winter Crown can be observed in any season), so about one Harvest Moon recall (12) every 5.7 days, or every 3.3
    days with the Winter Crown, if every pattern is seen; an astrolabe holds 32;
  - a master holding the pact gains no second recall of an occurrence (both routes claim the same key), but gains a
    free choice of any pattern they observed, where a master's own recall takes only the last one;
  - ending a pact and sealing a new one could give a second Star-reckoning in the same game day, but only for a whole
    new compact (its materials and resonance) each time: no positive-gain loop (review correction 10).
- **Economy (D-P49):** Star-reckoning is a sink (`astral_resonance` → `recalled_attunement`, no way out), modelled at
  the cheapest recall; offerings are a sink of at most 1/7 of an offering a day a pact. `tools/concordance_economy.py`
  still reports "gaining cycles: none" ([economy record](arcane-concordance-economy.md)). Consumed offerings stay in
  equivalence (D-S19); only the perch's item is excluded, and `values.json` values it from its recipe at 74.
- **Automation:** none. Every action is a player's, in person, at the perch or, since unit 3, from the roster (D-07):
  no hopper, pipe or machine pays, reckons or ends (the perch holds no item and has no slots).

## Failure behaviour, multiplayer and persistence

- **Every action is server-side,** behind the rate gate (`RateGate`, one key for opening, one for acting and one for
  hearing the terms, D-P35):
  - a request is five bounded numbers (container, sequence, action, choice, revision): no text, ids or totals;
  - since unit 3 the roster's request (D-07) is four (sequence, action, a listed row's id and its revision); its
    suspend, resume and end pass the `workers` gate (4 ticks) and its cancel and recover the `logistics` gate (10
    ticks), each answered "too fast" at most once a gate's span (the unit review);
  - only the perch's owner, in person, may accept there (never their party, never a stand-in);
  - only the pact's holder, in person, may reckon, pay, suspend, resume, end or seat it;
  - a stand-in never answers for upkeep, and pays, reckons, ends and suspends nothing for its player.
- One pact with Stolas a holder; a second acceptance at one perch is refused as taken and spends nothing, and a
  player who is not the perch's owner is refused as not permitted. A perch set by a machine (a blueprint stake or a
  drone build, which never ask the placement rules) records no owner and seals and seats for nobody (D-P24, as
  corrected by the unit review); break it and place it by hand. Its screen says only its owner may invite Stolas.
- Decline closes the screen in the same call, so its answer is shown on the action bar (which the narrator reads), not
  as a state for a screen that has closed (the unit review).
- A definition whose work serves its holder alone (Star-reckoning) must name `"personal"` beneficiaries; `"party"` is
  refused with a reason (`PactParser.HOLDER_ONLY`, the unit review), so the screen never promises a party a service
  that serves only its holder.
- A stale terms revision is answered "changed" with the terms again, and nothing is spent (D-P23). Every refusal
  names its reason; Accept is offered only when nothing stands in the way, and a forced Accept is still answered with
  its reason (D-P34).
- **Breaking or blowing up the perch** (the Concordance on): the pact is left without its perch, its escrow, hold and
  terms kept; Stolas leaves the world. Seat it at a new perch of yours (`AgreementHolder.seat`: the same record,
  terms, day's count, upkeep clock and escrow; D-P45), or end it from any perch that keeps nothing or, since unit 3,
  from the roster (D-07). Broken while switched off: the record is untouched and stays kept at its vanished place, and
  once its place is loaded keeping nothing it can be seated again (`AgreementHolder.stranded`, D-P25).
- **Ending** returns what was paid ahead once, into the living holder's inventory (the rest at their feet), never to a
  dead holder; Stolas departs and the departure is kept in the holder's history. Suspend and resume are idempotent
  (D-P46).
- **A lost Stolas** (killed by damage that bypasses invulnerability, or removed) returns to the perch at no cost while
  his holder is within 64 blocks of it, or when the holder opens the perch, if their roster has room (D-P28); a lost
  presence never ends the pact.
- **Conversations** live in memory only: one a player, bound to the open screen, ending after 6,000 ticks without a
  request, when the screen closes or when the player leaves (cleared on leaving the player list and on disconnect;
  D-P27). A visiting Stolas waits while any conversation at his perch is live, the owner's or anyone else's, and leaves
  when none is (review correction 9), with the Concordance on or off (the unit review).
- **The first obligation** is kept only while the pact is active and kept at its perch: not while it is suspended, held,
  without its perch, or stranded at a perch broken while the Concordance was off (the unit review).
- **Switched off** (`concordance.enabled=false`): every clock waits; nothing is reckoned, paid, ended or seated, and the
  screen says the Concordance is off. Records, escrow and registrations are untouched.
- **Saves:**
  - the perch's block entity: `owner`, `agreement` (the terms as sealed there) and `spirit` (its presence), stamped
    version 1; no items, and no visitor;
  - Stolas: `perch` and `record`, with `WorkerEntity`'s stamp. A visiting Stolas is written with his chunk too, with
    `perch` only; he is no perch's visitor when he loads, and leaves at his first think, with the Concordance on or off;
  - the Bound Will's pact and escrow, and the history: unit 1's keys, no new key; a pact without its perch is saved with
    no `place` and no `presence` (both optional);
  - recall claims: `recall:<pattern>` on the player's astral ledger, the format unchanged, so a master's recalls before
    and after D-05 share one ledger;
  - conversations: memory only;
  - `data/jugcraft/concordance/pact/stolas.json`: schema 1, read strictly.
- **A definition that fails to load** is reported with the rules' problems and refuses new invitations with the reason;
  sealed pacts keep the terms copied onto their record.
- **Chunk unload** is modelled in the tests as a gap between the perch's looks, not as a chunk-load listener.
- **Since unit 3:** leaving (declined, timed out, ended, or a visitor whose conversation is gone), Stolas makes his
  departing gesture and is gone 40 ticks later: struck off his holder's roster at once, never saved meanwhile
  (`shouldBeSaved`), and never taken for a perch's visitor or presence again (`gone()`, D-P97); a new invitation calls a
  new Stolas. His cues and his listening flag are never saved: one reloaded, or newly in view, shows his state, never a
  replay (D-P94). Roster sessions are memory only, forgotten when their reader leaves or the server stops. Unit 3
  changes no saved data.

## The pact screen (D-04)

`PactScreen`, a native narrated screen (a plain `Screen` with `MenuAccess<PactMenu>`, the lectern's pattern; D-P38),
opened by using the perch (`PactMenu`, an extended menu with no slots):

- his three beats, a page each, then the terms page (D-P39): service, hours, area, quota, upkeep, grace, suspension,
  duration, breach, ending, returns, the perch, beneficiaries and the Open Hand Compact, its materials drawn, and what
  stands in the way;
- a kept pact: how it stands, Star-reckoning with today's remaining count and the observed patterns (each saying whether
  this occurrence can still be recalled), the offerings carried, Suspend or Resume, and End, which asks first (D-P40):
  since unit 3 through `KeepFirstConfirmScreen`, vanilla's confirmation with the keyboard starting on Keep it, where Esc
  keeps the pact (D-P117);
- a pact without its perch: Seat here, or end it;
- operable by keyboard alone: the keyboard lands on the terms' first line, never on Accept; Esc, Leave and Decline agree
  to nothing; every line is a focusable, narrated paragraph and the server's answer is shown and read aloud; since unit
  3 each answer carries a drawn tick or cross, read "Done:" or "Refused:" by the narrator (`PactScreen.Verdict`, D-P103),
  and the screen, an in-game screen, draws subtitles over itself.

It fits windows down to 320×180 scaled pixels and scrolls its body; the client test draws it at GUI scale 4 in a
1920×1080 window and at GUI scale 2 in a 640×360 one. `PactClient` sends only the five bounded numbers for the open menu
and decides nothing. `PactStatePayload`'s lists are capped at 3 beats, 16 terms, 12 notes, 8 patterns, 8 offerings and 8
compact stacks.

## Stolas's character (D-06)

Built in unit 3 (commits `9805a9a7c`, his raven body, and `773b85aba`, his presence, sounds, subtitles and Jade; the
[contract](concordance-chunk-d.md)'s D-06 As built note and proposals D-P90 to D-P104 and D-P117). GeckoLib draws him at
1.4× (D-P90) with two controllers: the worker's own plays a loop for what the server synced, and a second, `gesture`,
plays the gestures the server fires (D-P94). Each gesture starts and ends on his listening pose.

| Clip | Kind | Length | Played when (the server's choice) | Telling pose | Sound event (subtitle) |
|---|---|---|---|---|---|
| `animation.stolas.idle` | loop | 4.0 s | serving, or waiting at the perch, with no conversation open | — | — |
| `animation.stolas.listen` | loop | 3.0 s | while any conversation at his perch is open (D-P95) | — | `jugcraft:stolas.listen` ("Stolas listens"), when one opens |
| `animation.stolas.waiting` | loop | 4.0 s | switched off, his holder away or out of his terms | — | — |
| `animation.stolas.suspended` | loop | 6.0 s | his pact suspended, by its holder or for upkeep | — | — |
| `idle.flapping` (the owner's, "astir") | loop | 4.75 s | an offering due | not posed (below) | — |
| `animation.stolas.arrive` | gesture | 2.0 s | summoned to the perch: an invitation, a presence called back, a pact seated (D-P96) | 0.70-1.20 s | `jugcraft:stolas.arrive` ("Stolas alights") |
| `animation.stolas.inspect` | gesture | 2.5 s | a Star-reckoning done, an offering paid ahead | 0.40-1.10 s | `jugcraft:stolas.inspect` ("Stolas inspects") |
| `animation.stolas.assent` | gesture | 2.0 s | a pact sealed, a pact resumed at the perch | 0.45-1.25 s | `jugcraft:stolas.assent` ("Stolas assents") |
| `animation.stolas.refuse` | gesture | 2.0 s | an acceptance or a Star-reckoning refused | 0.50-1.30 s | `jugcraft:stolas.refuse` ("Stolas refuses") |
| `animation.stolas.depart` | gesture | 2.0 s (holds its last frame) | leaving: declined, ended, a visitor whose conversation is gone | 0.30-0.80 s | `jugcraft:stolas.depart` ("Stolas departs") |

- **The server's triggers** (D-P94, D-P99): each gesture is fired by the server's own action (`StolasEntity.reaction`
  for the pact screen's answers; `PactPerchBlockEntity.summon` for his arrival), so a player who comes later sees his
  state, never a replay. No gesture answers the menu's own "not offered", hearing his terms (he listens), declining or
  ending (he departs), suspending (his loop shows it) or seating (he arrives). The first obligation, kept at an
  observatory, has none.
- **Listening** (D-P95): a synced flag, set at once when a conversation opens at his perch and recomputed at every
  think; his listening cue is not voiced within 10 ticks of his arrival, whose wing beats carry the moment.
- **Leaving** (D-P97): his departing gesture lasts 40 ticks of game time, and he is gone when it ends: discarded by his
  own tick or, where his chunk no longer ticks him (a roster End from afar, a holder who goes away within the 40
  ticks), by the server's sweep at the end of every tick (`StolasEntity.registerSweep`; the unit review). He is struck
  off his holder's roster at once, never saved meanwhile, listens no more, and is never taken for a perch's visitor or
  presence again (`gone()`); his departure is synced, so one who starts watching him while he leaves sees him already
  gone, never perched until he vanishes (`WorkerClient` draws him only where his departing gesture was fired). A copied
  perch never moves the original's Stolas (`PactPerchBlockEntity.stolas` asks his own perch; the unit's critique 6).
- **Cues** (D-P100, D-P101): one of a kind at most every 10 ticks a Stolas (`CUE_TICKS`), none while the Concordance is
  off except his departure. Each is entity-attached (NEUTRAL, heard within 16 blocks) and plays mono files, which
  OpenAL places, so each is heard from him and fades with distance (the unit review). The pact screen and the roster
  are in-game screens that draw subtitles over themselves (D-P103). CI's client runners have no sound device, so the
  client test checks the subtitles as defined and as fired, never as seen there (D-P104).
- **Jade** (D-P102; the optional integration, behind its entrypoints): a pact perch's and a Spirit Anchor's agreement
  (whether it keeps one, with whom, its service, its state, the next offering or the grace left, why its presence
  waits; a copy; a visit, only while that perch's own visitor waits there, critique 7) and Stolas's own pact. It names
  no player, shows nothing of the escrow but when the next offering falls due, and only reads (`AgreementHolder.peek`).
- **One presence a pact.** His death or removal, or his perch unloading, ends nothing and drops nothing; killing him is
  never a route (`aRemovedStolasNeverEndsThePact`, `onePresencePerPact`, `killingStolasIsNeverTheFastestRoute`).
- **Not posed:** his astir loop is the only clip he plays that the client test does not photograph frozen. Making an
  offering due in that test would change the upkeep its later steps read, so it is a recorded deviation from D-06's "a
  frozen-tick pose for each clip" (the unit's critique 11); `check_geckolib` still holds the clip to his animation file.

## The roster (D-07)

Built in unit 3 (commits `a3ee00822`, the server side and the journal's pacts lines, and `66b741868`, the screen and its
client test; proposals D-P105 to D-P121; the
[workers record](arcane-concordance-workers.md#the-roster-screen-concordance-chunk-d-d-07) describes it for every
worker).

- **Rows** (D-P105): the reader's own Bound Wills first, in the order sealed (a pact without its perch, or a record not
  yet claimed by its place, included; each with its presence in the same row), then the courier requests they filed,
  oldest first, then the other workers on their roster. At most 64 rows; the rest are counted ("N more"), and the
  journal and the `workers` and `logistics` commands list everything. A row shows its portrait, kind, service, place
  and whether it is loaded, its task, its next obligation and why it waits, each state and place in words beside a drawn
  mark (D-P119).
- **Actions**, each re-checked by the server against the reader's last listing and the row's own revision (D-P106):
  suspend, resume and end a pact or agreement (`AgreementHolder.toggle` and `end`: the escrow back once, only to a
  living reader in person), cancel a courier request and recover cargo the courier ledger holds stranded
  (`CourierLedger.cancel` and `recover`, never anything a container holds). Suspend and resume are offered by the
  record's hold, so nothing toggles back (D-P108); a stand-in is dropped and a dead reader refused (D-P109).
- **Gates** (D-P107): suspend, resume and end share the Spirit Anchor's (`workers`, 4 ticks); cancel and recover the
  logistics commands' (`logistics`, 10 ticks); opening has its own (`roster`, 10 ticks), served later by the server
  rather than refused.
- **Answers:** `not_listed`, `changed` (with the roster again), `too_fast` ("One moment", once a gate's span: the rest
  are dropped, since the unit review), `not_offered`, `not_permitted` and `disabled`, besides the holder's and the
  courier ledger's own outcomes; each shown with a tick or a cross and read aloud.
- **Switched off** (D-P110): the rows are listed, none offers an action, and an action is answered `disabled`; records,
  escrow and requests are untouched.
- **The journal** (D-P111): its Pacts section lists, after Stolas's Regard's lines, every pact and agreement held (where,
  its next obligation, why it waits as the exact figure) and the last eight departures, newest first.
- **The key** (D-P112): "Concordance Roster" (`key.jugcraft.concordance_roster`), not bound at first, listed on the
  encyclopedia's Keys page; it opens the roster from the game only.
- **The screen:** native and narrated; eight rows a page (Page Up and Page Down; D-P116), each row a focusable card with
  its actions under it; End asks first through `KeepFirstConfirmScreen`, the keyboard starting on Keep it (D-P117);
  cancel and recover do not ask. It asks again every 40 client ticks while open (D-P118), fits the smallest GUI
  (320×240, in the screen fit check, D-P120) and draws subtitles over itself. Portraits are visual-only creatures (at
  most 16, never added to a level or ticked), a request's item, or the Spirit Anchor's icon (D-P115).
- **No second inventory:** the screen keeps nothing; every row is read again from `WorkerRoster`, `BoundWills` and
  `CourierLedger` when asked.

## Dependencies and assets

No new dependency (Fabric API and GeckoLib as pinned; Jade, optional, as pinned). Stolas's body (D-S5) is the owner
library's Flashjack rig in his raven form: since unit 3 (D-P90, D-P91) its geometry is still copied byte for byte by
`tools/owner_art.py` from `tools/concordance_pacts.py`'s `OWNER_FILES`, and his skin, the glowmask that lights his eyes
and his animation file are adapted from the rig's own files by `tools/stolas_body.py` (`ADAPTED`), which
`tools/owner_art.py` writes and checks. `python3 tools/owner_art.py --provenance pacts` prints these rows (the copy's
Change cell added here):

| Runtime file (`src/main/resources/assets/jugcraft/`) | Library file (`art/owner-library/`) | Library SHA-256 | Change |
|---|---|---|---|
| `geckolib/models/entity/stolas.geo.json` | `originals/Magic/assets/ars_jymbaumental/geo/flashjack.geo.json` | `d23b5372d27cb76ea5974b2a1e78f3cba7b5a8f96cd14bef54d1e14249adc837` | none, byte for byte |
| `geckolib/animations/entity/stolas.animation.json` | `originals/Magic/assets/ars_jymbaumental/animations/flashjack.animation.json` | `c4015c9351dd54c3fec3589a544db4110809dc11ce60e040ad1cc7870c9491f8` | adapted by `tools/stolas_body.py` (animations) |
| `textures/entity/stolas.png` | `originals/Magic/assets/ars_jymbaumental/textures/entity/flashjack.png` | `567b44a51e083f2a9ec81db92a8743435985b630f8aca98ef62baed2393ea514` | adapted by `tools/stolas_body.py` (skin) |
| `textures/entity/stolas_glowmask.png` | `originals/Magic/assets/ars_jymbaumental/textures/entity/flashjack.png` | `567b44a51e083f2a9ec81db92a8743435985b630f8aca98ef62baed2393ea514` | adapted by `tools/stolas_body.py` (glowmask) |

The changes, made from the library files' bytes alone (nothing in the library is executed or changed, and the same
files always give the same output):
- **Skin:** each texel recoloured in place, by part (read from the geometry's box-UV faces) and by hue: the plumage
  black, its markings a blue-black sheen, the beak, legs and toes a dark horn, the eyes amber, the crest and the breast's
  glyph old gold. Every texel keeps its place and its alpha, and the owner's shading is carried onto each new ramp;
  nothing is drawn that the owner did not draw. The recolour serves D-S5's raven form, not originality. The built
  file's SHA-256 is `aea736bd87312c8dc0e3c00db74da998363c707cf0aa36f3fc77fe69a2ff6364`.
- **Glowmask:** the four eye texels (9,10), (9,11), (14,10) and (14,11) alone, for GeckoLib's `AutoGlowingGeoLayer`
  (D-P92); SHA-256 `95ade61dbaaba6f2584833d2e2a21cb93f84e4229347eef375e14b80f019132e`.
- **Animation file:** Flashjack's five clips unchanged (`idle.flapping`, his astir loop; `attack`, `idle.air`,
  `idle.ground` and `idle.sitting`, no longer played: his idle and waiting loops start from the last two's poses) and
  nine clips of his own, `animation.stolas.*` (four loops and five gestures, keyframed on the rig's bones; D-P93):
  56,789 bytes, SHA-256 `59ae566b1745108542362eebef76e4b3202116f8ca324cf39547a0029b64df93`.
- **Not imported:** `flashjack.bbmodel`, `flapjack.png` and `bluejay.png`.
- **Supplied labels and metadata, retained:** the folder's leftover mod metadata names Ars Elemental, author Alexthw,
  `license="GNU Lesser General Public License v3.0"`
  (`art/owner-library/ports/jug-nouveau/ars_jymbaumental-1.21.1-0.7.10.4/META-INF/neoforge.mods.toml:7, 15, 25`). The
  owner's declaration is [PROVENANCE.md](../../art/owner-library/PROVENANCE.md) (lines 3-7). Catalog rows:
  `art/owner-library/catalog/files.csv:21291` (animations), `:21330` (model) and `:21676` (skin); `:21329` is the
  Blockbench project, not imported. The owner kept his look and manner (D-D3); the supplied labels are kept as provenance.
- `check_geckolib` checks his model by face (three UV regions of the rig overlap as drawn; no face overlaps, D-P42) and
  holds every clip the Java plays, looped, played once or played and held; `check_stolas_body` holds the Java, the client
  test and the files to the generator's tables.
- The perch's item icon is drawn fresh by `tools/item_icons.py` from its map (`tools/item_icons/pact_perch.txt`,
  docs/ITEM_ICONS.md). Its block model is three boxes (a stone foot, a post and a crossbar) on Jugcraft's own
  Lampwright's Bench wood and Spirit Anchor stone; provisional, unchanged by unit 3.

**Sounds** (D-P101): six events through the generator (`tools/concordance_pacts.py`'s `SOUND_EVENTS` and `SOUND_FILES`,
merged by `tools/concordance.py`; `sounds.json` written by `tools/generate_material_data.py`), each bound to `.ogg`
files already in runtime, every one mono, so each is heard from him and fades with distance (`check_pacts` reads each
file's channels; the unit review replaced the cave book's pages and the ghost's weep, which are stereo). No new audio,
no voice; the raven's call is `beast_crow`'s too. His words are the generators' (English only, D-S21).

| Event | Runtime files (`src/main/resources/assets/jugcraft/sounds/`) | Subtitle |
|---|---|---|
| `jugcraft:stolas.arrive` | `menagerie/beast/flutter_flap_0` to `_2` | Stolas alights |
| `jugcraft:stolas.listen` | `menagerie/cavern/block/spelunkery_table/spelunkery_table_paper_insert` | Stolas listens |
| `jugcraft:stolas.inspect` | `menagerie/cavern/block/spelunkery_table/spelunkery_table_paper_remove` | Stolas inspects |
| `jugcraft:stolas.assent` | `household/block/doorbell/chime` | Stolas assents |
| `jugcraft:stolas.refuse` | `menagerie/beast/crow_idle_0` to `_3` (also `beast_crow`'s) | Stolas refuses |
| `jugcraft:stolas.depart` | `menagerie/cavern/mob/vesper/vesper_flap_0` to `_3` | Stolas departs |

These files are the owner library's imports, each catalogued with its source path and SHA-256 under
`art/owner-library/catalog/edited-2026-10-18-part2/`: `beast-runtime.json` (`flutter_flap`, `crow_idle`),
`cavern-runtime.json` (`spelunkery_table_paper_insert`, `spelunkery_table_paper_remove`, `vesper_flap`) and
`household-runtime.json` (`doorbell/chime`). The supplied labels are kept where the files were imported:
`sounds.json`'s own `household.block.doorbell.chime` keeps `subtitle.refurbished_furniture.doorbell_chime`; Stolas's
events carry subtitles of their own.

## Verification

Each entry is what actually ran, with the CI run it ran in; a result is that run's, on that commit. "Server" means a
game-test server with test players, "client" one real client on the game's own dedicated server; the two-client test is
two client JVMs on one CI runner. No person has played any of it.

- **Server game tests (unit 2's 46):** compiled first in run 40090825848 (on `92868ff82`), which failed two of them in
  both server jobs, each a defect of the test: `aBrokenPactDefinitionIsReported` read Stolas's offer after refusing it on
  purpose, and `talkingGrantsNoResearch` assumed a test player joins with nothing; both were fixed in `8a3425f9e`, whose
  run 40094972598 passed all 46 in both server jobs, as did run 40133540741:
  - D-03 (9): `StyxGameTests` `styxHintsByTheListenersOwnProgress`, `talkingGrantsNoResearch`,
    `styxNamesTheSkyStepUntilOneIsRecorded`; `ConcordanceSharingGameTests.notesNeverStandInForStolasEvidence`;
    `ConcordanceProgressionGameTests.stolasRegardIsReachable`; `ConcordanceSkyGameTests`
    `aRecordedObservationCountsWithoutAWindow`, `aTurnedClockMakesNoNewClaim`; `ConcordanceJournalGameTests`
    `thePactsSectionNamesTheSkyStep`, `thePactsSectionWaitsForTheBindingArts`.
  - D-04 (17): `ConcordanceWorkerGameTests` `stolasExplainsBeforeAgreeing`, `aStaleTermsRevisionIsRefused`,
    `aRefusalSpendsNothing`, `acceptingSealsOnceAndSpendsOnce`, `oneStolasPactPerHolder`,
    `anotherPlayerCannotAcceptForTheHolder`, `twoNegotiationsAtOnePerchSealOnce`, `aVisitingStolasLeavesWithoutARecord`,
    `aCrateRefusesStolas`, `stolasTakesNoOrdinaryDamage`, `aBrokenPactDefinitionIsReported`,
    `thePactScreensMessagesAreBounded`; `ConcordancePersistenceGameTests` `aPerchKeepsItsOwnerAndPact`,
    `thePerchHoldsNothing`; `ConcordanceSharingGameTests.aCopiedPerchIsInert`;
    `ConcordanceAuthorityGameTests.aPerchIsPlacedOnlyWhereItsPlacerMayBuild`;
    `ConcordanceResourceGameTests.thePactDefinitionParsesStrictly`. Extended: `blockEntitiesStampTheirVersion` (the
    perch) and `aChangeOfOwnerMovesNoPact` (a perch whose land changes hands).
  - D-05 (20): `ConcordanceSkyGameTests` `stolasRecallsAnObservedPatternOncePerOccurrence`,
    `aMastersRecallAndStolassShareOneClaim`, `aChangedClockGivesNoSecondRecall`, `theHarvestMoonRecallsOutsideItsEvent`,
    `anUnobservedPatternIsRefused`; `ConcordanceWorkerGameTests` `theObligationMastersTheEntryOnce`,
    `aSuspendedPactRecallsNothing`, `theQuotaAndTheAreaRefuseWithReasons`, `aStandInNeverAnswersForUpkeep`,
    `endingReturnsTheEscrowOnce`, `aDeadHolderIsNeverHandedTheEscrow`, `aBrokenPerchLeavesThePactWithoutItsPerch`,
    `aPactIsSeatedAgainAtANewPerch`, `aLostPresenceIsCalledBackAndEndsNothing`, and the unit review's
    `shortAbsencesNeverRenewTheGrace`; `ConcordanceSharingGameTests`
    `aCopiedPerchRecallsNothing`, `anotherPlayerCannotUseTheHoldersService`; `ConcordancePersistenceGameTests`
    `thePactAndItsEscrowSurviveARestart`, `aRestartWhileSuspendedStaysSuspended`;
    `ConcordanceResourceGameTests.theServiceRulesKeepTheirShape`. Extended: unit 1's
    `upkeepWarnsThenSuspendsAfterGraceAndResumes`, `aReturnAfterAbsenceGetsTheFullGrace`,
    `absentAndUnloadedTimeIsNeverOwed` and `theConcordanceOffHoldsEveryClock`, each now shown on Stolas's own pact too,
    and `aChangeOfOwnerMovesNoPact` (the perch's new owner is not served).
  - Spending is shown with `TestPlayers.survivor`; vanilla's mock players are creative and pay nothing (D-P26).
  - Changed by the unit review: `twoNegotiationsAtOnePerchSealOnce` (the other player is not permitted; a perch no
    player placed seals for nobody), `theObligationMastersTheEntryOnce` (a stranded pact keeps nothing),
    `aVisitingStolasLeavesWithoutARecord` (a visitor loaded while switched off leaves), `thePactScreensMessagesAreBounded`
    (a 209-character choice id is sent and read back) and `thePactDefinitionParsesStrictly` (`"party"` for
    Star-reckoning is refused).
- **Unit 3's server game tests (18 new, the unit review's one included):** first compiled and run in run 40121823509
  on `040d73ece`, where they passed (run 40120355940 on `554d71ab1` failed to compile at the roster client test, fixed
  in `040d73ece`), and passed again in run 40133540741:
  - D-06 (8, `D06_TESTS`): `ConcordanceWorkerGameTests` `aDepartingStolasIsNeverAVisitorAgain` (a declined visitor and
    an ended presence depart, are gone for the perch, unsaved, and never return; with their cues; since the unit review
    he listens no more and his departure is synced), the unit review's `aDepartureEndsWithoutHisTicks` (the sweep, at
    explicit game times with no tick between, ends a departure at its length and never before),
    `stolasGesturesAndSoundsFollowTheirTable`, `stolasAnswersEachActionWithItsGesture`,
    `aRemovedStolasNeverEndsThePact`, `onePresencePerPact`, `killingStolasIsNeverTheFastestRoute` and
    `theJadeReadingsNameNoPlayerAndChangeNothing` (a copied perch's `stolas(level)` is nobody; no visit is read after a
    release).
  - D-07 (9, `D07_TESTS`): `ConcordanceWorkerGameTests` `theRosterEndsOnlyWhatTheReaderHolds`,
    `aRequestNamingAnotherPlayersRecordIsRefused`, `theRosterListsAPactWithoutItsPerch`,
    `endingAnOrphanReturnsItsEscrowOnce`, `aStaleRosterRevisionIsRefused`, `theRosterStopsAt64Rows`;
    `ConcordanceLogisticsGameTests` `recoveryNeverTakesCargoOutOfAnUnloadedContainer`,
    `theRosterEndsAHandAndRecoversItsCargoOnce`; `ConcordanceJournalGameTests`
    `thePactsSectionListsPactsOnlyAfterTheBindingArts`.
  - And `ConcordanceWorkerGameTests.aReplacedHandLoadingWhileSwitchedOffStaysOffTheRoster` (the workshop hand's roster
    rule, D-08's last open item).
  - Changed by unit 3 (leaving now lingers 40 ticks, so "he has left" reads `gone()`, not `isRemoved()`):
    `thePerchHoldsNothing`, `aCopiedPerchIsInert`, `aVisitingStolasLeavesWithoutARecord`, `endingReturnsTheEscrowOnce`,
    `aBrokenPerchLeavesThePactWithoutItsPerch` and the helpers `stolas` and `stolases`.
- **Client game tests** (one client on the game's own dedicated server, in survival; each passed in its shard of run
  40133540741; its shots are that run's `client-screenshots-<shard>` artifacts; the evidence bundle and what was viewed are
  in [the contract](concordance-chunk-d.md#client-evidence)). Unit 3's additions first ran in run 40121823509 on
  `040d73ece`, where they passed, after run 40120355940 failed to compile the roster client test:
  - `ConcordancePactClientGameTests` (shard 5): his beats and terms reached by Enter alone; the terms and a beat at GUI
    scale 4 in a 1920×1080 window and in a 640×360 one, every control on screen and named; Accept refused as
    `cannot_afford` (a cross), then sealed by keyboard (a tick) and spent once; his four loops and five gestures frozen
    in their telling poses, and a timed sequence through his refusal (frozen at 0.290, 0.647, 1.004 and 1.357 s), all
    photographed from his front; his refusal's and assent's subtitles defined and fired (CI has no sound device, so none
    is heard there); Jade's readings of the perch and of him, naming no player (his own Jade line showed
    "entity.minecraft." in that run's picture, fixed in `b773913c4` and seen fixed in run
    40140478331); a recall at its cost under the shared claim; an offering paid ahead; End asked first with the keyboard on
    Keep it, Esc keeping the pact, the end returning the offering once while he departs. It passed earlier in runs 40090825848 and 40094972598 (shard 12) as unit 2 left it.
  - `ConcordanceRosterClientGameTests` (shard 14): the roster empty, then a pact, a shade's agreement, a workshop hand
    and two requests with their states in words beside their marks, at three sizes; suspend and resume; an end asked
    first; a recovery and a cancel; the workshop hand carrying three copper ingots across the yard, frozen with its
    cargo aboard, then delivering them once; 64 rows paged and the rest counted.
  - `ScreenFitClientGameTests` (shard 13): the roster fits the smallest GUI, 320×240.
  - `tools/select_client_tests.py` weighs the pact class at 240 s, the roster at 170 s and the two-client class at 300
    s: estimates, not measurements.
- **Two clients (D-09, D-S22):** `ConcordancePactNetworkTests` (shard 3) starts a second, separate client JVM (offline
  name `PactPeer`, its own temporary profile) against the first client's loopback-only dedicated server, on the
  pattern of main's `OwnerCollectionsNetworkTests` (#435), not #467's harness. The stranger, south of the holder's
  perch, saw Stolas arrive, assent and depart; at the holder's perch its Accept, sent with the compact in hand, was
  refused (`not_permitted`) and spent nothing; at the kept perch its End was refused (`not_holder`); its roster listed
  nothing of the holder's and refused an End naming the holder's pact (`not_listed`); the holder's pact sealed once and
  ended once, and the stranger kept everything it carried. Passed in shard 3 of run 40133540741 and in its opt-in job
  without the optional integrations; two of its three pictures were defective there (the arrival showed "Loading
  terrain", the refusal was not in view), fixed in `b773913c4` and `d00fa2167` and seen fixed in run 40140478331 (every job passed). Two automated
  clients on one machine with offline names: no two people, no two accounts, no network between machines.
- **The whole path (D-09):** `ConcordanceJourneyGameTests.thePactHallFromStyxToDeparture` walks the route on one
  survival player from Styx's hint to Stolas's departure, ended from the roster, with the workshop hand carrying one
  request; `thePactHallsRecipesLoad` loads the route's nine recipes. Both passed in both server jobs of run 40133540741
  (`mod`: "All 2572 required tests passed"; `optional integrations absent`; the log names no passing test one by one).
  The codex's compact page now says where the resonance comes from (generated).
- **Load (D-09):** `ConcordancePactHallLoadGameTests.loadCard`, opted in. Eight pacts with their perches and Stolas, a
  visitor, two open conversations, an open roster and four workshop hands, measured against the same world without
  them, with D-09's rules (one presence a pact, one pact a holder, the perch's look once a second) sampled every 100
  ticks. Its result, the CI runner's, is in [Chunk D's contract](concordance-chunk-d.md#the-pact-hall-load-card); in run
  40133540741 the hall added +0.098 ms to the median and +0.311 ms to the p95 server tick on CI's runner, within the proposed
  budget.
- **Without the optional integrations:** every server game test, in the `optional integrations absent` job of run
  40133540741; the pact, roster and two-client classes passed in that run's opt-in job (Jade not loaded, so no Jade shot).
- **Audits** (`tools/check_mod_data.py`, run and passing): `check_stolas_regard` (D-03), `check_pacts` (D-04, D-05;
  since unit 3 its D-06 block: the cues, the reactions, the Jade readings, a copied perch moving nobody, the hand's
  roster rule), `check_pact_screen` (the client, the screen and the client test; since unit 3 the confirmation through
  `KeepFirstConfirmScreen`), `check_stolas_body` (new in unit 3: the body's Java, files and client test against the
  generator's tables), `check_roster` (new: the roster's server side, payloads, journal lines, gates, G13 and its tests),
  `check_roster_screen` (new: the roster's client classes, key and client test), `check_economy` (Stolas's terms,
  offerings and recall costs against the data), `check_geckolib` (his model by face; every clip looped, played once or
  held), `check_rime_strata_assets` (his sound files counted), `AUTHORITY_ROUTES` (Star-reckoning),
  `concordance_journey --check`, `owner_art --check` and the icon maps; and since unit 5, `check_stolas_body`'s front
  camera and refusal sequence, `check_pact_network` (the two-client class, its reasons and shots, the shards' upload
  and the opt-in job's guard) and the pact hall's rows in `check_acquisition` and `concordance_journey --check`, whose
  mutations were each reported (D-09's As built in the contract).
- **Audit mutations** (run in place on scratch edits and restored, per code commit): D-03's 18, D-04's 26, the client
  commit's 17 and D-05's 19 were each reported, and each commit's unchanged tree reported nothing. The unit review's
  are listed in the [contract](concordance-chunk-d.md#d-05-star-reckoning-the-obligation-upkeep-renew-suspend-and-end-unit-2).
  Unit 3's: the body's 22 (B1 to B22), the presence's 29 (P1 to P29), the roster's server side's 28 (S1 to S28) and its
  screen's 25 (C1 to C25) were each reported, and each commit's unchanged and restored tree reported nothing (the
  contract's D-06 and D-07 As built notes). For the records' commit, twelve more, run in place and restored: a broken
  link in each record it changes (ten) and `stolas-pact.md` removed (each reported by `scripts/check_repository.py`),
  and trailing whitespace in the contract (reported by `git diff --check`); seven of the code commits' mutations (B7,
  B21, P5, P15, S9, C13, C17) were run again on the final tree against the Concordance audit alone and each reported;
  the unchanged tree reported nothing.
- **Unit 3 in CI:** run 40120355940 on `554d71ab1` failed to compile at the roster client test (`LOGGER.info` with a
  generic `computeOnClient` result was ambiguous, two errors; fixed in `040d73ece`); run 40121823509 on `040d73ece`
  first compiled and ran it and passed every job, its server tests with and without the optional integrations
  (`optional integrations absent` after one re-run: `TurkeyGameTests.hensLayEggs`, from #112 and untouched by Chunk D,
  failed there once and passed in that run's `mod` job) and its client tests among them.
- **Design-pass simulations** (scratch copies, before the build): the progression graph with the perch had 0 problems;
  a slab in the perch's recipe gave 3 (hence polished deepslate, D-P32); the journey's fifth step was reached; the
  perch's icon map had no errors.
- **Not run:** a person playing any of it, alone or with others; two people or two accounts; a sound device (CI has
  none, so no subtitle has been seen); the narrator's audio; an aimed click on the perch (the tests open it by the
  server's use); client frame times on a player's hardware; a real chunk unload (modelled as a gap); a busy server; his
  astir loop in a frozen pose; a GeckoLib portrait on the roster seen by a person.

## World and event applicability

- The perch is placed in any dimension where its placer may build (claims, spawn and town protection apply), never in
  Styx's home (`StyxConservatory.covers`: the conservatory's footprint, and a town-built home's district and road,
  D-P31), and never by a stand-in. A perch a machine sets records no owner and seals and seats for nobody (D-P24, as
  corrected by the unit review). Star-reckoning's patterns are observed only in the Overworld.
- No structure loot, biome or boss dependency.
- The Harvest Moon is observed only on the Halloween event's Harvest Moon nights and the Winter Crown only in winter (or
  any season with seasons off); the earned pact and observations persist after the event and the season.

## Rollout and open questions

Known limits:
- Since unit 3 a pact is ended at a perch (its own, or for a pact without its perch, any perch that keeps nothing) or
  from the roster. The roster shows at most 64 rows (the journal and the commands list the rest), and an action within 4
  ticks of a Spirit Anchor's use (10 of a logistics command) answers "One moment".
- His astir loop is not posed in the client test (a recorded deviation, above). The Spirit Anchor's icon is drawn as a
  roster portrait only when the creature cannot be made; whether GeckoLib draws in the GUI is shown only by the client
  test's shots (its `drawn` records the attempt, not pixels).
- The first obligation counts once a player, not once a pact (D-P44), which is stricter than the contract's wording.
- A pact broken while switched off whose old place stays unloaded cannot be seated until that place loads; it can
  still be ended.
- A perch set by a machine is inert (no owner) and its screen says only its owner may invite Stolas; it does not say
  that it has none.
- Suspending, recalling and resuming once a day keeps the upkeep period counting only a few ticks of active time a day
  (left as built: the owner accepted D-P50 alone, D-D4).
- Stolas is summoned facing south and keeps that facing, so a holder north of the perch sees his back (the evidence
  photographs him from the south, D-P133).
- CI has no sound device: his subtitles are checked as defined and fired, never seen there.

Asked at the owner's checkpoint ([the contract](concordance-chunk-d.md#the-owner-checkpoint-as-reported)): Stolas's look
and manner; how strict pacts should feel (D-P50 and the suspend, recall and resume route); which six-spirit cohort
follows. The Harvest Moon balance, the once-a-player obligation (D-P44), the compact's numbers, its resonance's one
source (D-P123), his gestures' timing, sounds and Jade's words (D-P100), and the roster key's default (D-P112) and
portraits (D-P115) are listed in its handoff for the owner's review, not as further questions.

Ids are stable once released: `jugcraft:pact_perch` (block, item, block entity and menu), `jugcraft:stolas` (entity),
`jugcraft:stolas_regard` (research), `jugcraft:pact_service` (practice), `#jugcraft:stolas_offerings` and
`#jugcraft:stolas_specimens`, `data/jugcraft/concordance/pact/stolas.json`, and the payload types
`jugcraft:concordance_pact_request` and `jugcraft:concordance_pact_state`. Since unit 3: the sound events
`jugcraft:stolas.arrive`, `.listen`, `.inspect`, `.assent`, `.refuse` and `.depart`; the payload types
`jugcraft:concordance_roster_request` and `jugcraft:concordance_roster_state` (the roster's actions and icons are sent by
index, so append only); the key `key.jugcraft.concordance_roster`; Jade's providers `jugcraft:pact_perch` and
`jugcraft:spirit_anchor`. Unit 3 adds no saved data. Disabling the Concordance keeps every registration and every
record.
