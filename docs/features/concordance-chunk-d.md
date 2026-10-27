# Concordance Chunk D: the pact hall (contract and handoff)

Status: units 1 to 5 (D-01 to D-09) are built on this branch (not merged). The candidate `48d99c904` was run in CI run
40133540741 (a manual Build run with every client class and both opt-in inputs): every job passed on the first attempt.
Three defects its pictures showed were fixed after it in `b773913c4` and `d00fa2167` (run 40140478331: every job passed,
and the three pictures were seen fixed). Unit 1 passed
in run 40067977680. Unit 4 passed in run 40085012234, after runs 40079227985 and 40082852322 failed its twenty hand
tests at a test helper's comparison. Units 1, 2 and 4 together passed in run 40094972598 on `8a3425f9e`, after run
40090825848 failed two of unit 2's server tests, each a defect of the test. Unit 3: run 40120355940 on `554d71ab1`
failed to compile (two errors at an ambiguous `LOGGER.info` in the roster client test; fixed in `040d73ece`); run
40121823509 on `040d73ece` passed every job, unit 3's 18 server tests and its client tests run there first, after one
re-run of `optional integrations absent` for `TurkeyGameTests.hensLayEggs` (#112, untouched by Chunk D; it passed in
that run's `mod` job). Unit 5's combined fixes, whole path, load card, client evidence and two-client attempt are under
[Unit 5](#unit-5-evidence-and-handoff-d-09). The owner answered the checkpoint on 26 October 2026 (D-D3 to D-D5 under
[Decisions](#decisions)): keep Stolas, D-P50 accepted, gardens next.
Chunk D is developed on branch
`claude/chunk-d-pact-hall` (local `wip/chunk-d`), started from
Chunk C's head `779851cdb` ([#466](https://github.com/jimbozoomer-byte/jugcraft/pull/466), itself stacked on Chunk B's
[#465](https://github.com/jimbozoomer-byte/jugcraft/pull/465)); it is reviewed as the third stacked pull request, [jimbozoomer-byte/jugcraft#474](https://github.com/jimbozoomer-byte/jugcraft/pull/474),
against `claude/chunk-c-living-workshop` (D-D1). Not merged; Chunks B and C beneath it are not merged either.
Proposal: the owner chose Chunk D on 23 October 2026 ("begin chunk d"), from
[the expansion plan's delivery chunks](../plans/concordance-expansion-2026-10-10/09-claude-delivery-chunks.md) (section 6,
D1 to D4, 09:146-174), with [workshops, ecology and pacts](../plans/concordance-expansion-2026-10-10/03-workshops-ecology-and-pacts.md),
[magic progression](../plans/concordance-expansion-2026-10-10/02-magic-progression-and-spellcraft.md),
[technology and economy](../plans/concordance-expansion-2026-10-10/06-technology-and-economy.md),
[interfaces](../plans/concordance-expansion-2026-10-10/08-interfaces-animation-and-assets.md) and
[validation](../plans/concordance-expansion-2026-10-10/10-validation-and-checkins.md). The same day the owner chose the
branch (D-D1) and said "dont ask me to approve just finish this whole part" (D-D2): the session is not to stop for
approvals and is to finish Chunk D end to end. The session therefore settles the plan's open questions itself, taking
its delivery plan's recommendations as corrected by the plan's completeness critique, and records each choice as
D-S*n*, "Decided by the session under D-D2". **No D-S decision is the owner's.** Each stays open to the owner's review at
the checkpoint, which asks at most three questions (09:18).
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.

This page is the chunk's contract (chapter 9 §1 step 7 of the plan) and, as the units land, its technical handoff
(chapter 10 §7). Plan chapters are cited as `NN:line` (`docs/plans/concordance-expansion-2026-10-10/NN-*.md`); `MP` is
the plan's `CLAUDE-MASTER-PROMPT.txt`; `CC` is [Chunk C's contract](concordance-chunk-c.md). Code is cited as
`file:line` at `779851cdb`: Java paths are under `src/main/java/io/github/jimbozoomer/jugcraft/` (the Concordance's under
its `concordance/`), Peepo's under `src/main/java/` (`local/peepo/...`), client code under
`src/client/java/io/github/jimbozoomer/jugcraft/client/`, data under `src/main/resources/data/jugcraft/`, tests under
`src/gametest/java/io/github/jimbozoomer/jugcraft/test/` (Peepo's under `src/gametest/java/local/peepo/`), and
`mc263src` is the decompiled 26.3 reference (its `HEAD`). A fact marked *(survey)* comes from this chunk's seven survey
reports, its delivery plan or the plan's critique and was not re-read for this page. **D-D*n*** is the owner's decision;
**D-S*n*** is the session's, under D-D2.

## Player outcome

The plan's outcome: "Stolas is discoverable through Styx, has a distinct character, negotiates a clear agreement,
performs a useful service and leaves safely when the player ends it" (09:150); the chunk map's line is "Meet Stolas,
negotiate, use and safely end a working pact" (09:29). Corrected for what is already built (below), and with the
session's decisions D-S1 to D-S23, a survival player can:

- **Keep the agreements already in the game (unit 1):** a Gathering Shade that is killed leaves its load once, and the
  holder calls a new one under the same agreement at no cost; a failed seal says why; a new shade is on the roster at
  once; only a new anchor's owner or the owner's party seals it; a familiar no longer fades for days its person was
  logged out; and the holder can end an agreement without breaking its block.
- **Discover (unit 2):** talking to Styxhexenhammer gives that player a hint drawn from their own research progress
  (talking teaches nothing); a player without Styx finds the same entry, Stolas's Regard, through the codex's Binding
  category and its specimens. The entry asks for three kinds of personal evidence through research that already exists:
  examining a specimen (a Styx flower or a feather), harvesting an organism crop (Verdant Husbandry's practice) and
  dissolving a sample at the Assayer's Scale (the Assay's practice); the invitation also asks for one sky observation
  recorded at an observatory.
  Notes never stand in.
- **Negotiate (units 1 and 2):** the player builds a pact perch where they may build, at their own base, and invites
  Stolas. He arrives at the perch, and a conversation screen shows his terms before anything is spent: the service,
  hours, area, daily quota, upkeep and grace, what suspension does, how long the pact lasts (until ended), the breach
  rules, how to end it, what comes back, who benefits and what the Open Hand Compact costs. Accepting spends the
  compact's cost in the same server action that seals the pact. Declining, or any refusal, spends nothing and names its
  reason.
- **Use (unit 2): Star-reckoning (D-S1).** Once a game day, at the perch, the holder chooses any sky pattern they have
  personally observed and recalls it with their astrolabe's stored resonance, at that pattern's existing recall cost and
  under the same once-per-occurrence claim as a master's recall: no waiting for the 19-day Echo Comet or a clear night.
  The effect lasts 2,400 ticks wherever the holder goes.
- **Keep (units 1 and 2):** upkeep is one offering every 7 game days, paid into the pact's escrow (a Styx flower, or an
  amethyst shard), up to four periods ahead; the compact pays the first. A missed offering shows a warning, then a full
  game day of grace, then suspension: the service stops, nothing is taken and nothing is lost. Paying resumes it. Time
  the holder was away, or the perch unloaded, is never owed, and a pact never ends by itself.
- **End (units 1 to 3):** the holder ends the pact at the perch or from the roster. Stolas makes his departure gesture
  and leaves; the escrow comes back once to the holder's inventory; the reason is shown and kept. Breaking or blowing up
  the perch never ends the pact: Stolas leaves the world, and the holder seats the pact at a new perch or ends it.
- **Manage (unit 3):** a roster screen lists every familiar, spirit, construct and pact the player keeps, with its
  state, next obligation and waiting reason, and suspend, resume, end, cancel and recover actions that go through the
  existing authorities; the journal gains a "pacts" section.
- **Contrast (unit 4, D-S2):** mastering Stolas's Regard, by fulfilling the pact's first obligation, unlocks the
  **workshop hand**: a spirit sealed at a Spirit Anchor bound to the holder's Courier Post that carries that post's exact
  requests, the crucible's own formula shortage from Chunk C included, through the courier ledger.

Already built, and not rebuilt here:

| System | Where |
|---|---|
| Agreement terms and refusals (suspended, outside the agreement, finished); a day quota the clock cannot reset | `concordance/worker/Agreement.java:13-29, 43-50` |
| Bound Will record and ledger: seal, transfer, release; 32 a holder; `release` a second time is `UNKNOWN`; `transfer` has no game route | `concordance/resource/BoundWill.java:10`; `BoundWillLedger.java:16, 46-86`; `concordance/spirits/BoundWills.java:25-32, 60-76` |
| Worker roster: 32 a player, read while unloaded, written on a status change | `concordance/spirits/WorkerRoster.java:29, 69-97`; `WorkerEntity.java:93-99` |
| Worker base: think once a second, persistent, signs, `describe`, GeckoLib and SmartBrainLib, no portals, save stamp | `concordance/spirits/WorkerEntity.java:58-59, 71, 100-121, 183-189, 204-207, 216, 225, 243-258` |
| Status vocabulary: 5 intents, 14 reasons, worded by the generator | `concordance/worker/Status.java:10-31`; `tools/concordance_workers.py:48-68` |
| Spirit Anchor: seal, suspend, release, drop once; its slots unopenable by players | `concordance/spirits/SpiritAnchorBlockEntity.java:78-160, 219-222`; `SpiritAnchorBlock.java:48-78` |
| Authority: present, answering (the stand-in only when the server opts in), stand-in, mayChange | `concordance/Authority.java:59-65, 72-78, 106-108, 114-126` |
| Courier ledger (cargo held by the ledger, a courier holds its claim id), posts, the porter's courier decision | `concordance/courier/CourierLedger.java:34-42, 289-304`; `CourierPostBlockEntity.java:48-51`; `concordance/spirits/ClockworkPorterEntity.java:291-510, 586-602` |
| The crucible files its own courier requests (C-06, on Chunk C's branch only) | `concordance/CrucibleBlockEntity.java:475, 506`; failure table CC:1127-1136 |
| Sky: forecast command (no permission), observations, a master's recall of the last-observed pattern | `concordance/sky/Sky.java:310-319`; `AstralClaims.java:68-85`; `AstrolabeItem.java:107-145` |
| Upkeep precedent: one day a look, unloaded days owed nothing, a late look reschedules from now, nothing lost | `concordance/wonder/Spires.java:12-26, 152-175` (163-166) |
| Styx: an NPC hurt only by damage that bypasses invulnerability, a one-time gift, four rotating lines, never respawned | `styx/Styxhexenhammer.java:53-55, 71-90`; `styx/StyxState.java:17, 40-41`; `src/main/resources/assets/jugcraft/lang/en_us.json:12652-12657` |
| Styx flowers, renewable by bone meal | `styx/JugcraftStyx.java:24-25`; `styx/StyxFlowerBlock.java:31-36` |
| Diagnostic codes `UNAVAILABLE_WORKER` and `CONFLICTING_AGREEMENT`, declared and unused, parameters "fixed by their first user" (on Chunk C's branch only; main has no `diagnostic/` package) | `concordance/diagnostic/Code.java:31-34` |
| Journal sections: 24 at most, 13 used | `concordance/journal/Journal.java:53, 86-106`; the Academy's, `academy/JugcraftAcademy.java:47` |
| Encyclopedia: 11 pages of 11, read-only by audit | `guide/encyclopedia/Encyclopedia.java:61-75, 145`; `EncyclopediaPayload.java:27`; `tools/check_mod_data.py:19818` |
| An NPC-bound menu that re-checks every button | `town/ShopMenu.java:129-136, 143-160` |
| Native book widgets (`BookArt.Paragraph`/`Tab`, `ComposerScroll`) | `client/BookArt.java:69, 93`; `client/ComposerScroll.java` |
| Jade: one provider for every `WorkerEntity`; none for the Spirit Anchor | `compat/jade/JugcraftJadePlugin.java:28-47` (44) |
| A rite's result is `Transform` or `Effects` only | `concordance/ritual/RitualDefinition.java:56-71` |
| Equivalence exclusions (the bonding charm, porter key, astrolabe, research notes among them) | `data/jugcraft/tags/item/equivalence/excluded.json` |
| Test players that log out when their test ends; an ordinary survival test player | `TestPlayers.java:35`; `AlchemyArtifactGameTests.java:33-47` |

## Units

Each unit is a reviewable boundary: its own run of commits on `claude/chunk-d-pact-hall`, with its own records and
green checks before the next starts. The order puts the first end-to-end path (Styx, evidence, perch, conversation,
terms, acceptance, service, obligation, upkeep, end) first, in units 1 and 2, before Stolas's finished look, the roster
screen and the second service (09:16, "Implement the first end-to-end path before multiplying content variants"; MP:66).
Unit 1 lays what that path stands on (the shared holder's seal outcomes, ownership and holder-only end, the pact
record, its states and upkeep rules), together with the small routine fixes to the agreements already shipped; unit 2
completes the path. Nothing in units 3 and 4 comes before it.

| Unit | Tickets | What it delivers | Record (proposed) | State |
|---|---|---|---|---|
| 1 | D-01, D-02 | The existing agreements keep their promises (shade death and re-call, seal refusals, the roster at once, anchor ownership, the familiar's fade, ending without breaking); one pact record: the agreement holder extracted from the Spirit Anchor and shared, a place on the Bound Will, one source of truth for suspension, the upkeep rules, the departure history, the copy check and the offline, restart and feature-disable policy | `arcane-concordance-workers.md`; this contract | Built (not merged): D-01 and D-02 on `claude/chunk-d-pact-hall` (local `wip/chunk-d`; commits `56a74f4be`, `7477efbcd`, `fa5360d0d`, the records' `97e1facd4`, the unit review's fixes `3775eac3b` and 26.3's white wool `da185dd0d`). Local checks run and passed, 52 of 53 audit mutations reported (D-02's As built). Green in CI: run 40067977680 on `da185dd0d` passed every job (the build, the server game tests with and without the optional integrations, every client class) |
| 2 | D-03, D-04, D-05 | The first end-to-end path: Styx and the codex to Stolas's Regard and its evidence, the perch, Stolas's arrival, the conversation and terms, acceptance with the Open Hand Compact, Star-reckoning, the first obligation, upkeep, suspension, ending. A provisional body for Stolas is acceptable here | new `stolas-pact.md`; `styxhexenhammer.md`; `arcane-concordance-celestial.md` | Built (not merged), merged here after unit 4; checkpoint pending: D-03, D-04 and D-05 on the local `wip/chunk-d-u2`, from unit 1's records commit `97e1facd4` (commits `9daf23ae1`, `832129b95`, `7da686ad4`, `ee82c1dc7`, the records' `8f0635d9e` and the unit review's fixes `f6ef0b85d`; records: new `stolas-pact.md`, `styxhexenhammer.md`, `arcane-concordance-celestial.md`). Local checks run and passed, all 80 audit mutations reported (D-05's As built) and the unit review's 15. Merged here at `bfdf98ff9`, with `92868ff82`'s fixes to the merge; CI run 40090825848 on `92868ff82` compiled it and passed every client shard, its client test among them, but failed `aBrokenPactDefinitionIsReported` and `talkingGrantsNoResearch` in both server jobs, each a defect of the test (fixed in `8a3425f9e`); run 40094972598 on `8a3425f9e` passed every job |
| 3 | D-06, D-07 | Stolas's character: the Flashjack-derived raven body, arrive, listen, inspect, assent, refuse and depart, sounds with subtitles, Jade; the roster screen with suspend, end, cancel and recovery, and the journal's pacts section | `stolas-pact.md`; `arcane-concordance-workers.md` | Built (not merged): D-06 and D-07 on the local `wip/chunk-d-u3`, from `92868ff82`, commits `9805a9a7c` (Stolas's raven body), `773b85aba` (his presence, sounds, subtitles and Jade), `a3ee00822` (the roster's server side and the journal's pacts), `66b741868` (the roster screen and its client test), the records' `8820a86dd` and the unit review's fixes `426b16196`; merged here at `554d71ab1`, with the roster client test's compile fix `040d73ece`. Local checks run and passed, all 104 audit mutations reported (D-06's and D-07's As built) and the unit review's 27. CI run 40120355940 on `554d71ab1` failed to compile (two errors at an ambiguous `LOGGER.info` in the roster client test); run 40121823509 on `040d73ece` was its first run: its 18 server game tests and its client tests passed, every job passing after one re-run of `optional integrations absent` for `TurkeyGameTests.hensLayEggs` (#112, untouched by Chunk D; it passed in that run's `mod` job); run 40133540741 passed them again |
| 4 | D-08 | The workshop hand (a courier agreement carrying a post's exact requests) and the schema review | `arcane-concordance-logistics.md`; `arcane-concordance-workers.md`; this contract | Built (not merged): D-08 on the local `wip/chunk-d-u4`, from unit 1's head `97e1facd4` beside unit 2, commits `023b681e8`, `e51ef2b11`, the records' commit and the unit review's fixes ("Chunk D unit 4: fixes from the unit review"); merged here at `df3006850`, with the test fixes `ca2957291` and `86c4e7127`. Local checks run and passed, all 42 audit mutations reported and the nine the fixes add (D-08's As built). Compiled in CI: runs 40079227985 and 40082852322 failed its twenty hand tests at their first step (a test helper compared an item's use with the SUCCESS constant, which 26.3's `ItemStack.useOn` never returns; fixed in `86c4e7127`); run 40085012234 passed its 21 server game tests and the client companion check, every job passing after one client shard's re-run for main's `PeepoPopulationClientTests` cow-breeding flake |
| 5 | D-09 | Combined fixes, the whole-path test, load, client evidence, the two-client attempt, records, the checkpoint and the handoff | this page; `stolas-pact.md`; `arcane-concordance-journey.md`; `docs/TESTING.md` | Built (not merged): D-09, commits `90dd7a756`, `8b821326a`, `3a0b6363d`, the unit review's fixes `aca4aca1c`, the merge of unit 3's compile fix `8842c3bb5`, merged here at `48d99c904`, and the fixes from CI's pictures `b773913c4` and `d00fa2167`; the candidate `48d99c904` in run 40133540741: every job passed on the first attempt; `b773913c4` in run 40137136287: failed the second client's refusal check (fixed in `d00fa2167`); `d00fa2167` in run 40140478331: every job passed on the first attempt; local checks as run (D-09's As built) |

The plan allows three to five units (09:3); this is five plus the baseline. The completion evidence E1 to E7 that every
ticket's acceptance names is defined, and mapped to tickets and tests, in [Completion evidence](#completion-evidence).

## Baseline (D-00)

### Facts

- **Base and main:** `HEAD` is `779851cdb` (Chunk C's head, committed 23 October 2026 19:08:54Z). `origin/main` is
  `f904df3fd` (#457), read again by REST at 22:15Z and 22:43Z on 23 October 2026; it is an ancestor of `HEAD`, so a
  merge of main would change nothing. The remote branch `claude/chunk-d-pact-hall` points at `779851cdb` (REST) and has
  no pull request.
- **The stack beneath:** #465 (Chunk B, head `5c6878efe`, base `main`) and #466 (Chunk C, head `779851cdb`, base
  `claude/chunk-b-winter-stone`) are open, mergeable (`clean`) and unreviewed. #465's 21 check runs passed; #466's 21
  passed and its `workshop load card` job was skipped (REST, 22:43Z).
- **Named suites, `@GameTest` count at `779851cdb`:**

| Suite | Tests | Covers today |
|---|---|---|
| `ConcordanceWorkerGameTests` | 5 | familiar, spirit, porter; a save of the familiar and porter (its Javadoc also claims "a shade's anchor", which it does not test: `:205-207`) |
| `ConcordanceLogisticsGameTests` | 15 | the courier ledger and the crucible's requests |
| `ConcordanceSharingGameTests` | 2 | research notes and the sconce only (`:76`, `:135`) |
| `ConcordancePersistenceGameTests` | 10 | legacy records (`:141-155`, a `BoundWill` built at `:147`); block-entity stamps without the anchor (`:190-209`) |
| `ConcordanceSkyGameTests` | 6 | observatories, attunement, a master's recall (`:206`), the sky through a save |
| `StyxGameTests` | 7 | gifts, growth, pots, codec, blueprints (frozen, `:82-90`), preview, the district opt-in |
| `ConcordanceAuthorityGameTests` | 5 | light, harm, a porter's keeper, a Spire Heart's store that hoppers cannot drain (`:243`), links |
| `ConcordanceResourceGameTests` | 4 | `boundWillAstralAndPrimaKeepTheirShape` (`:126`; `BoundWill` built at `:129`, `:135`) |
| `ConcordanceJournalGameTests` | 5 | sections shown by research |
| `ConcordanceProgressionGameTests` | 8 | the progression graph |

- **Client companion tests:** `src/gametest/java/local/peepo/PeepoCompanionClientTests.java` and
  `CompanionAutomationChecks.java` exist. No client test shows a Hearthling, Gathering Shade or porter
  (`docs/features/arcane-concordance-workers.md:191-192`).
- **No pact system exists:** no Stolas entity, pact data, invitation or negotiation in `src/` or `tools/`. "Stolas"
  names only the flower `stolas_starflower` (`styx/JugcraftStyx.java:24`), Styx's line 0 (`en_us.json:12654`, written by
  `tools/styx.py:193-202`), a comment on the conservatory's shrine (`tools/styx_structure_v2.py:184`) and the docs.
- **Owner direction outside the pack** (`docs/ROADMAP.md:26-34`, 5 October 2026): all 72 spirits; each with "clearly
  stated pact benefits, costs, limits, duration, and exit/breach rules"; "Players must knowingly accept pact terms;
  another player cannot bind them without consent" (`:29`); a data-driven roster (`:30`); "Keep the current decorative
  flowers usable before pacts exist" (`:31`); server authority, protected-area rules, concurrent summons, entity caps,
  restart and unload recovery and save compatibility designed before implementation (`:32`).
- **Design source for Stolas:** "The source describes a raven form; no unrelated contemporary adaptation supplies the
  character design" (`docs/features/styxhexenhammer.md:37`).
- **Test players:** vanilla's mock player has a random identity and always reports creative mode: its `gameMode()` is
  overridden to `CREATIVE` (`mc263src` `net/minecraft/gametest/framework/GameTestHelper.java:421-432`), and
  `Player.isCreative()` reads it (`mc263src` `net/minecraft/world/entity/player/Player.java:1541-1543`), so setting
  survival on it changes nothing a creative check reads. `AlchemyArtifactGameTests.java:33-47` already makes an ordinary
  `ServerPlayer` for that reason. `TestPlayers.mock` (`TestPlayers.java:35`) logs its players out when their test ends.

### Status delta: the plan's text against the code at `779851cdb`

| The plan says | What is true | Evidence |
|---|---|---|
| "Use existing agreement/Bond/BoundWill concepts" (03:142) | All three exist as separate models (the familiar's bond, the shade's agreement, the porter's body) sharing `WorkerEntity`, `Status` and `WorkerRoster` | `worker/Agreement.java:13-14`; `worker/Bond.java:10`; `resource/BoundWill.java:10`; `spirits/WorkerRoster.java:27-29` |
| The contract's fields (03:144) | Present: agreement id (`BoundWill.id` = `Agreement.record`), owner (`holder`), service (`work`), boundary (dimension, centre, radius), a day quota. Absent: beneficiaries, upkeep, fulfilment evidence, any state but `suspended`, departure reason, presentation profile. The spirit id is `counterpart`, written unnamespaced by the anchor (`"gathering_shade"`) while tests use namespaced ids | `Agreement.java:13-14`; `SpiritAnchorBlockEntity.java:83`; `ConcordanceResourceGameTests.java:129` |
| Six states (03:146); four (09:154) | Only `suspended` is stored, on the anchor's block entity; waiting is a derived `Status`; departure removes the record and keeps no reason | `Agreement.java:13-14, 52-54`; `SpiritAnchorBlockEntity.java:105-131` |
| "Missed upkeep suspends the benefit with a visible warning and a grace period. No offline punishment" (03:146) | No worker has upkeep. The Spire's is the precedent: at most one day a look, unloaded days owed nothing, a late look reschedules from now. The familiar's bond fades for days its person was logged out (G7) | `wonder/Spires.java:12-26, 152-175`; `worker/Bond.java:33-39` |
| "A stored sky route avoids calendar lock" (09:158) | A master may recall, only when nothing up is visible where they stand and only the pattern they observed last, once an occurrence under `recall:<pattern>`, for 2,400 ticks; a recalled attunement ignores its window and season | `sky/AstrolabeItem.java:107-131, 134-145`; `celestial/Calendar.java:18`; `sky/Sky.java:298` |
| "A recorded sky occurrence through existing research" (03:131) | An observatory records the observation (`AstralClaims.observe`) and `observation` practice for anyone who uses it while a pattern is up and visible, with no research check; only becoming its keeper asks `Sky.knows`. So the sky step has no research depth, and `ResearchEngine.explain` does not cover it | `sky/ObservatoryBlockEntity.java:187-220` |
| "Prepare three evidence tokens" (03:132) | No token items exist; evidence is per-player knowledge keys, and notes never reach mastered | `rules/EvidenceRule.java:74-76` |
| The Open Hand Compact, a rite (03:102, 03:133) | A rite's result is `Transform` or `Effects` only | `ritual/RitualDefinition.java:56-71` |
| Invitation and terms before acceptance (03:134; 08:67) | The anchor seals on its first use and prints the terms afterwards; a failed seal says nothing | `spirits/SpiritAnchorBlock.java:50-60` |
| Roster page with cancel and recovery (09:162; 08:65-69) | Text only: the journal's workers section and a command; the record lists a screen as open | `journal/Journal.java:91`; `spirits/Workers.java:133-146`; `arcane-concordance-workers.md:145-146` |
| An encyclopedia "Pacts" page (08:16) | 11 pages registered of 11 allowed; a twelfth is dropped | `EncyclopediaPayload.java:27`; `Encyclopedia.java:61-75, 145` |
| GeckoLib vocabulary: idle, listen, inspect, assent, refuse, departure (03:159) | Workers loop one clip per status; no worker has a one-shot gesture; `check_geckolib` checks only `thenLoop` clips | `WorkerEntity.java:243-258`; `tools/check_mod_data.py:17258-17267` |
| Agreement diagnostics | `UNAVAILABLE_WORKER` and `CONFLICTING_AGREEMENT` are declared and unused; the package is absent from `origin/main` | `diagnostic/Code.java:31-34`; `git ls-tree origin/main` |
| "The task record should refer to an authoritative claim" (03:191) | True of the porter in courier mode and of the crucible's own requests (C-06, #466 only) | `CourierLedger.java:34-42`; `CrucibleBlockEntity.java:475, 506` |
| Stolas, owl or raven art | None in the owner's library; the nearest unused bird rig is Ars Elemental's Flashjack | assets survey §1, §4.1 *(survey)*; the files, read for this page, in D-06 |

### Defects and gaps found (D must not inherit them)

| # | Gap | Evidence | Fixed by |
|---|---|---|---|
| G1 | A Gathering Shade that is killed loses what it carries: it has no `remove` override, its load lives on the entity, and its roster entry, the anchor's `spirit` and the Bound Will go stale. The anchor then says only "spirit away", and nothing calls a shade back | `GatheringShadeEntity.java:51` (no `remove`; the porter's is `ClockworkPorterEntity.java:586-602`, the familiar's `HearthlingEntity.java:146-153`); `SpiritAnchorBlock.java:72-77` | D-01 |
| G2 | A copied anchor (its block entity's data at a second place) is never checked against the ledger. Breaking the copy releases the original's Bound Will (release asks only record and holder), sends the original's shade away and strikes it off the roster (both copies name the same `spirit`), and drops the copy's contents. Every existing Bound Will has no place, so a place check alone would miss every existing copy | `SpiritAnchorBlockEntity.java:116-131, 154-160`; `BoundWills.java:25-29` | D-02 (with a legacy claim rule) |
| G3 | Who may seal: inside a claim a stranger already cannot use another's block (`UseBlockCallback`), but outside claims anyone with the Binding Arts may seal an anchor someone else placed, and no anchor records who placed it | `finance/LandProtection.java:32-37`; `SpiritAnchorBlock.java:48-59`; `Authority.java:114-121` | D-01 |
| G4 | A failed seal is silent (a message only on success) | `SpiritAnchorBlock.java:56-59` | D-01 |
| G5 | A newly sealed shade is missing from the roster until its status changes (`seal` never notes it; the roster is written only on a change). A roster at 32 silently ignores a new worker, and that 32 is shared with Nouveau helpers and Starbuncles; Bound Wills have their own 32 | `SpiritAnchorBlockEntity.java:78-103`; `WorkerEntity.java:93-99`; `WorkerRoster.java:69-76`; `nouveau/NouveauHelpers.java:40`; `nouveau/NouveauItem.java:33`; `BoundWillLedger.java:16, 50` | D-01 (a `ROSTER_FULL` refusal) |
| G6 | The holder can end an agreement only by breaking the anchor, though the agreement says the holder may release it | `SpiritAnchorBlock.java:48-78`; `Agreement.java:10` | D-01 (the method), D-07 (the action) |
| G7 | A familiar's bond fades for days its person spent logged out. `neglected` counts every game day since `lastSeen`, and a familiar only thinks while loaded: offline and other-dimension paths never move `lastSeen`, and a familiar is usually unloaded while its person is away. This contradicts `Bond.java:6-7` ("online elsewhere"). A routine adjacent fix, not E2 evidence: the bond is not upkeep | `worker/Bond.java:33-39`; `HearthlingEntity.java:100-114`; `WorkerEntity.java:183-189` | D-01 (D-S17) |
| G8 | Stale record: it says an offline holder's shade keeps working; the code waits | `arcane-concordance-workers.md:213-214` against `GatheringShadeEntity.java:148-155` | D-01 |
| G9 | `counterpart` is unnamespaced for the shade | `SpiritAnchorBlockEntity.java:83` | D-02 (new records only) |
| G10 | The anchor is missing from the stamp test, and the save test's Javadoc claims a shade's anchor it does not test | `ConcordancePersistenceGameTests.java:190-209`; `ConcordanceWorkerGameTests.java:205` | D-02 |
| G11 | No Jade provider for the Spirit Anchor | `JugcraftJadePlugin.java:28-47` | D-06 |
| G12 | Styx is absent from old saves (the district defaults off) and from worlds without a fresh town or an operator's placement, and is never respawned | `town/TownState.java:36`; `StyxState.java:17` | D-03 (the non-Styx route) |
| G13 | Nouveau helper kinds (`nouveau_<species>`) have no roster kind lang, so the roster shows raw keys | `nouveau/NouveauHelper.java:42`; `Workers.java:142` (lang has only `familiar`, `spirit`, `construct`, `starbuncle`) | D-07 |
| G14 | Suspension would have two sources of truth once the Bound Will holds a state: the anchor's `Agreement.suspended` cannot be read or changed from a roster while the anchor is unloaded, and the status text is "suspended by its holder", wrong for upkeep | `SpiritAnchorBlockEntity.java:48, 105-113`; `tools/concordance_workers.py:63` | D-02 (D-S9) |
| G15 | The anchor is a `WorldlyContainer` whose bottom face hoppers may empty, and an explosion removes it as breaking does: the removal releases the agreement and drops its slots, so an explosion ends a shade's agreement (its loot table's `survives_explosion` governs only the anchor's own item). Unchanged for the anchor, whose slots are deliveries meant to be emptied; the perch avoids both by holding nothing (D-S6) | `SpiritAnchorBlockEntity.java:38, 154-177`; `loot_table/blocks/spirit_anchor.json`; `mc263src` `net/minecraft/world/level/block/state/BlockBehaviour.java:187`, `net/minecraft/world/level/chunk/LevelChunk.java:290-293` | D-04 (for the perch); Risks (the anchor) |

**Record defects, fixed when each unit touches its record:** `arcane-concordance-workers.md:213-214` (G8) and its
open item for a management screen (`:145-146`), closed by D-07.

### Open pull requests and conflicts (REST, 23 October 2026 22:15Z, re-read 22:43Z; refresh before each unit, 09:11)

20 open at this page's first check (22:15Z), two more than the delivery plan's read: #471 (opened 21:44:05Z, stacked on
#469) and #472 (opened 22:10:19Z). 21 open at the re-read (22:43Z): #473 (opened 22:16:51Z, stacked on #470). File lists
for every row below were read by `/pulls/N/files` at 22:43Z, except #461's: its 30,836 changed files exceed what that
listing returns, so its row is the PR survey's. In those lists, no open pull request touches Concordance Java under
`spirits/`, `worker/`, `resource/`, `sky/` or `courier/`, Styx, the journal, the research data or `TestPlayers` (#461
by the survey only); only #462 touches Concordance generators (`tools/concordance_alchemy.py`,
`tools/concordance_ember.py`).

| PR | What it is | Files it shares with D's likely edits | Bearing on D |
|---|---|---|---|
| #465, #466 | Chunks B and C, D's base | everything | Conflict by construction: a change to either in review means a merge into D. `diagnostic/`, C-06's crucible requests and `TestPlayers.mock` exist only in #466 (Chunk C's branch) |
| #452 (fork) | Peepo machine support, tree taps, spawn rates; adds `LandRegistry.mayBuildFor` (`finance/LandRegistry.java`). Changes `src/main/java/local/peepo/MobTransportCrate.java`'s `permitted` (adding `LandRegistry.mayBuild`), not `friendly`, whose MISC rule refuses workers | `.github/workflows/build.yml`, gametest `fabric.mod.json`; Peepo gametest files (`CompanionPopulationChecks.java`, not `CompanionAutomationChecks.java`) | D edits nothing under `src/main/java/local/peepo` (D-S20). D's crate test reads `friendly` and `capture`, whose refusal of MISC #452 leaves as it is |
| #461 | Aircraft, furniture, death graves: a mixin captures a player's death drops *(survey)* | lang, `values.json`, `check_mod_data.py`, `generate_material_data.py` *(survey)* | The escrow returns only into a living holder's inventory (D-S6). D's return tests cannot run with #461 on D's branch; re-run them after #461 merges |
| #453 (draft) | Keep inventory in lair deaths | `CHANGELOG.md`, `docs/WHAT_EXISTS.md`, lang, `tools/encyclopedia.py` | As #461 |
| #463 (fork) | Visitor combat: wild `Enemy` mobs fightable on others' claims unless owned (its `mayFightWithoutClaimAccess`) | gametest `fabric.mod.json`; `finance/LandProtection.java` | Stolas and the workshop hand are `WorkerEntity`s, neither `Enemy` nor `OwnableEntity`, so they stay protected on claims |
| #467 (fork) | Storage crates; an opt-in two-client harness (`StorageCrateMultiplayerHarness.java`, `tools/storage-crate-multiplayer.init.gradle`) | lang, encyclopedia `en_us.json`, `values.json`, survival registry, gametest `fabric.mod.json`, `check_mod_data.py`, `generated-material-resources.json`, `JugcraftJadePlugin.java`, `JugcraftJadeClient.java`, `JugcraftClient.java`, `docs/TESTING.md` | Textual risk in the Jade plugin and client (D-06). Its harness is an unmerged fork contribution and is not imported (D-S22) |
| #462 (fork) | Recipe conflicts (`RecipeConflictGameTests`, `FoodRecipeConflictGameTests`) and the stirring rod's recipe | `values.json`, `check_mod_data.py`, `generated-material-resources.json`, gametest `fabric.mod.json`; `tools/concordance_alchemy.py`, `tools/concordance_ember.py` | The perch's recipe must pass its fixture once merged |
| #460 (fork) | Arrow stacking, Stalker contact damage, missing JEI recipes | gametest `fabric.mod.json` | Hot files only |
| #469 | `ScreenKit.Fitted` and a screen-fit check | `CHANGELOG.md`, `docs/WHAT_EXISTS.md`, `ScreenKit.java`, gametest `fabric.mod.json`, lang | D's screens adopt `Fitted` if it merges first |
| #470 (on #459) | Guns 14F | `sounds.json`, lang, `values.json`, gametest `fabric.mod.json`, `generated-material-resources.json`, `CHANGELOG.md`, owner-library catalog | `sounds.json` is generated: D's events go through the generator (D-S5). D does not edit the catalog |
| #471 (on #469; new) | The benches in the workshop look | `CHANGELOG.md`, `docs/WHAT_EXISTS.md`, `ScreenKit.java`, `LampwrightBenchScreen.java`, lang | `ScreenKit` contention if D adopts `Fitted` |
| #472 (new) | The Crowfield and the Harvest Crown (the Pumpkin King, part 1) | `CHANGELOG.md`, lang, `values.json`, gametest `fabric.mod.json`, `check_mod_data.py`, `generated-material-resources.json` | Hot files only; no Concordance, sky or worker file |
| #473 (on #470; new at the re-read) | Guns 15A | `sounds.json`, lang, `values.json`, gametest `fabric.mod.json`, `generated-material-resources.json`, `CHANGELOG.md`, owner-library catalog | As #470 |
| #458 (draft fork) | Retro Trader faces and gestures | lang, `sounds.json`, `JugcraftClient.java`, gametest `fabric.mod.json` | Reference only; hot files |
| #468, #459, #455, #454, #449, #448 | Seasonal harvest, guns 14E and 14D, the seasonal biome audit, quartz, Frostward College | #468 and #454: `CHANGELOG.md`, gametest `fabric.mod.json`, `check_mod_data.py`, `generated-material-resources.json`; #459 and #455: `CHANGELOG.md`, lang, `values.json`, `sounds.json`, gametest `fabric.mod.json`, `generated-material-resources.json` (#455 also `check_mod_data.py`); #449: lang, `values.json`, survival registry, gametest `fabric.mod.json`, `check_mod_data.py`, `generated-material-resources.json`; #448: `JugcraftClient.java` | Hot files only |

**Hot shared files** (from the lists at 22:43Z): lang `en_us.json`; the gametest `fabric.mod.json`;
`alchemy/values.json`; `docs/audits/survival-item-registry.json` (#449, #467); `tools/check_mod_data.py`;
`tools/data/generated-material-resources.json`; `CHANGELOG.md`; `sounds.json` (generated; #455, #458, #459, #470,
#473); `JugcraftJadePlugin.java` and `JugcraftJadeClient.java` (#467); `JugcraftClient.java` (#448, #458, #467; #461
*(survey)*); `docs/WHAT_EXISTS.md` (#453, #469, #471); `docs/TESTING.md` (#467); `docs/INTEGRATION_STATUS.md` (D edits
it; no open pull request in these lists does).

### Checks before each unit lands

1. Refresh main and the open pull requests (REST, read only); merge main if it has moved past `f904df3fd`, and merge
   Chunk C's branch if it has moved in review.
2. Run the generators the unit changes (`tools/generate_material_data.py` and those it calls): the generated JSON must
   be current (`docs/TESTING.md:5`).
3. `python3 scripts/check_repository.py`, `python3 tools/check_mod_data.py`, `python -m unittest discover -s scripts/tests`.
4. `./gradlew build` (the server game tests); `-PjugcraftOptionalIntegrations=false` for every unit that changes Jade
   code or another optional boundary.
5. `./gradlew runClientGameTest -PclientTests=...` for the classes `tools/select_client_tests.py` picks; with a new
   `WEIGHTS` entry, also `python -m unittest discover -s scripts/tests -p test_client_test_selection.py`.
6. `python3 tools/concordance_journey.py --check`; `tools/check_icon_maps.py` for new icons;
   `python3 tools/owner_art.py --check` when the owner's files are imported; `git diff --check`.
7. CI by workflow_dispatch of the Build workflow, every client class, as Chunks B and C did.

Each is reported on its own as run and passed, run and failed, or not run. Nothing had been run for Chunk D when this
contract was written; what has run since is in the status line and the units table.

## Tickets

### D-01. The existing agreements keep their promises (unit 1)

**What the player sees.**
- A Gathering Shade that is killed puts its load into its anchor's slots when the anchor is loaded and still names it,
  otherwise drops it once where it fell (D-S18). The anchor then says the spirit was lost, and the holder's next use of
  the anchor calls a new shade under the same agreement and Bound Will, at no cost.
- A shade re-called while the old one was merely unloaded: the old one, when it loads, finds it is no longer its
  anchor's spirit, delivers its load into the anchor once and departs.
- A seal that fails says why: the Concordance is off; the Binding Arts are not understood; the anchor is not yours or
  your party's; you hold 32 agreements (`HOLDER_FULL`); your roster is full (`ROSTER_FULL`); the anchor already keeps an
  agreement.
- A new shade is on the roster at once.
- Only the anchor's owner or the owner's party may seal a new anchor (D-S8); an anchor placed before Chunk D, which
  records no owner, may be sealed by anyone who may change it (`Authority.mayChange`), as today plus that check; a
  stand-in never seals.
- A familiar fades only for its person's own play time away from it, never for days they were logged out (D-S17).

**Changes.**
- `concordance/spirits/GatheringShadeEntity.java`: a `remove(RemovalReason)` override on the porter's pattern
  (`ClockworkPorterEntity.java:586-602`): when the shade is destroyed, its load goes into its anchor through
  `SpiritAnchorBlockEntity.deliver` (`:134-151`) if the anchor is loaded and still names this shade, the rest (or all of
  it, the anchor unloaded) popped where it fell; the roster entry is struck. `depart` delivers into the anchor the same
  way instead of popping everything at it (`:197-207`), so a replaced shade delivers once.
- **"Lost" is read from the roster (D-S18):** the anchor's spirit is lost when its holder's roster no longer lists it,
  which the removal override guarantees for a dead shade; `level.getEntity(spirit) == null` means only "not loaded"
  (`SpiritAnchorBlock.java:72-77`).
- `SpiritAnchorBlockEntity`:
  - `seal` returns an outcome instead of null: `DONE`, `TAKEN`, `HOLDER_FULL`, `ROSTER_FULL`, `NOT_PERMITTED`,
    `DISABLED`, `UNKNOWN_RESEARCH`; it notes the shade on the roster at once (G5), and refuses `ROSTER_FULL` before
    sealing rather than letting `WorkerRoster.note` drop the note (`WorkerRoster.java:69-76`);
  - a re-call of a lost shade under the same record (no new Bound Will, nothing spent);
  - `end(ServerPlayer holder)`: holder only, once. It lands here with its test (the perch's end in D-05 uses the same
    method through the shared holder); the player reaches it for a shade from the roster in D-07. Until then, breaking
    the anchor stays the only in-game end for a shade (critique 22);
  - an optional `owner`, recorded when the anchor is placed (`setPlacedBy`); sealing asks the owner or the owner's party
    (`JugcraftParties.sameParty`, the Courier Post's rule, `CourierPostBlockEntity.java:48-51`) and
    `Authority.mayChange` at the anchor; without an owner, `mayChange` alone; `Authority.isStandIn` refuses.
- `SpiritAnchorBlock.use`: one message for each refusal (`message.jugcraft.concordance.workers.*`, from
  `tools/concordance_workers.py`).
- `HearthlingEntity` and `Bond` (D-S17): the bond stores its person's play-time statistic (`Stats.PLAY_TIME`,
  `mc263src` `net/minecraft/stats/Stats.java:28`, counted while they play, `ServerPlayer.java:715`) at the last visit, as
  an optional key `seen_played`. `neglected` counts whole days (24,000 ticks) of that play time since, so offline days
  never count, even when the familiar was unloaded all along; days played in another dimension still count, as
  `Bond.java:6-7` says. A bond without the key takes the current play time as its start and fades nothing on that look.
  The key must be optional: `HearthlingEntity` reads the bond with `orElse(Bond.NEW)` (`:179`), so a decode error would
  reset a familiar's bond.
- `docs/features/arcane-concordance-workers.md:213-214` corrected (G8).

**Reuses:** `WorkerRoster.note/remove`, `SpiritAnchorBlockEntity.deliver`, `Block.popResource`, `Authority`,
`JugcraftParties`, `RateGate` "workers", `tools/concordance_workers.py`.

**Tests (server):**

| Suite | New tests |
|---|---|
| `ConcordanceWorkerGameTests` | `aKilledShadeLeavesItsLoadOnce` (anchor loaded: into its slots; counts before and after; a second removal gives nothing); `aKilledShadeAwayFromItsAnchorDropsWhereItFell` (anchor unloaded); `aLostShadeIsCalledAgainUnderItsAgreement` (the same record id, no new Bound Will, nothing spent); `anUnloadedShadeReplacedLeavesOnceAndDeliversOnce`; `aSealedShadeIsOnTheRosterAtOnce`; `aFullRosterRefusesTheSeal` (`ROSTER_FULL`; no Bound Will made); `aFullHolderIsToldWhy` (`HOLDER_FULL`); `theHolderEndsWithoutBreaking` (once; a second end gives nothing; anyone else refused); `aFamiliarDoesNotFadeWhileItsPersonIsAway` (logged out across 3 game days, the familiar unloaded and then loaded; the same identity returns 20 blocks away; unchanged); `aFamiliarFadesWhileItsPersonPlaysElsewhere` (3 game days of the person's play time away: −12, tested for the first time) |
| `ConcordanceAuthorityGameTests` | `onlyTheOwnersSideSealsAnAnchor` (a stranger outside any claim refused; a party member allowed; an ownerless anchor: `mayChange`); `aStandInNeverSeals` |

`TestPlayers` gains `survivor(helper)`, an ordinary survival `ServerPlayer` built as
`AlchemyArtifactGameTests.player` builds one (`:33-47`) and logged out when its test ends, and `again(helper, uuid)`,
the same identity back after a logout (vanilla's mock gets a random identity each time). The existing
`aSpiritKeepsToItsAgreement` must still pass.

**Audits:** `check_workers` (the new message keys); `check_persistence` (the anchor's and the bond's optional keys keep
the stamp at 1).

**Acceptance:** E4 (a shade's death, unload and re-call return its load once); E1 (no stranger or stand-in seals
another's anchor). The familiar's fix is a routine adjacent fix and not E2 evidence (critique 6).

**Saves:** optional `owner` on the anchor; optional `seen_played` in the bond. Stamps stay 1.

**Review boundary:** fixes to existing behaviour only. No new id, block, entity or art.

**As built (unit 1: commits `56a74f4be`, D-01, and `7477efbcd`, D-S17; record
[arcane-concordance-workers.md](arcane-concordance-workers.md)).**
- **Built:**
  - `concordance/spirits/AgreementHolder` (new; the Spirit Anchor keeps one by composition and implements its `Holds`):
    the owner (`setOwner`, saved as the anchor's optional `owner`), `maySeal(level, player, partySeals)`, `seal`
    returning one `Pacts.Outcome`, the presence noted on its holder's roster at once, `spiritLost` (read from the
    roster, D-S18), `recall` (the same Bound Will, nothing spent; `ROSTER_FULL` refused), `end` (holder only, once: an
    instance method at the block and a static one by record id for D-07's roster), `toggle` and `broken`. The anchor's
    `AGREEMENT_CODEC` moved into it unchanged, so an anchor's save reads as before.
  - The pure `concordance/worker/Pacts`: `Outcome` (`DONE, DISABLED, NOT_PERMITTED, TAKEN, ALREADY_HELD, HOLDER_FULL,
    ROSTER_FULL, UNAVAILABLE, CANNOT_AFFORD`, mirrored by `SEAL_OUTCOMES` in `tools/concordance_workers.py` and each
    worded `message.jugcraft.concordance.workers.seal_<id>`), `refusal` (one reason, in that order), `holds`,
    `counterpart` (namespaced; records before G9 wrote none), `ENDED` and `BROKEN`; `WorkerRoster.full`.
  - `SpiritAnchorBlock`: `setPlacedBy` records the placer, never a stand-in; `use` says the switch, then a copy's
    `CONFLICTING_AGREEMENT` or an unreadable record (D-02), then seals (or says the one reason not), suspends or resumes
    (its holder, sneaking), calls a lost shade again (its holder), or reads its terms, state and spirit ("lost" or
    "away").
  - `GatheringShadeEntity`: a `remove(RemovalReason)` override (killed or discarded: its load into its anchor once, when
    loaded and keeping its own holder's agreement, otherwise where it fell; struck off the roster); `depart` and `leave`
    deliver the same way; it notes itself on its roster in `decide`, as its anchor's current spirit only.
  - D-S17: `Bond.seenPlayed` (key `seen_played`, read with `lenientOptionalFieldOf`, `UNSEEN` for a bond saved before),
    `neglected(played)`, `together(now, played)`, `restart(played)`; `HearthlingEntity` reads `Stats.PLAY_TIME` and
    counts the fade whenever its person is in its level, before a visit counts.
  - `TestPlayers.survivor`, `leave`, `again`, `played` and `play`; `mock` shares the end-of-test log-out.
  - Generators: `SEAL_OUTCOMES`, the D-01 messages, the anchor's tooltip, the spirits codex ("Its Terms" and a new "If
    It Is Lost" page) and "The Bond"; lang and the codex entry regenerated. G8's open item in the workers record
    removed.
- **Departures from this ticket and from the unit's specification** (proposals D-P6, D-P11 to D-P15 in Decisions):
  - the outcomes are the ticket's without `UNKNOWN_RESEARCH`, plus `ALREADY_HELD` (D-02's), `UNAVAILABLE` and
    `CANNOT_AFFORD`: "the Binding Arts are not understood" stays the anchor's own check before sealing, worded as before
    (`workers.unknown`), not an outcome (D-P6);
  - the holder was extracted once, in D-01's commit (not D-02's, as Dependencies and order planned), and extended by
    D-02, so the anchor was rewritten once;
  - `Holds.call(level, who, counterpart)` names the counterpart and returns a `WorkerEntity`; only `WorkerEntity`
    subclasses are presences (the review's corrections 7 and 8; D-P15);
  - `Sealing` carries `partySeals` (the anchor true, D-04's perch false, D-S8; correction 6; D-P6) and an optional cost,
    `affords` asked after the refusals and before anything is called, `pay` once the Bound Will is recorded, refused as
    `CANNOT_AFFORD` (correction 10; D-P14), so D-04's compact is spent in the sealing call;
  - a shade's load goes into its anchor only when that anchor keeps its own holder's agreement, never into one sealed
    again by someone else (correction 12; D-P12 as built);
  - the shade's self-note is in `decide`, for its anchor's current spirit only, so a replaced or switched-off shade
    never takes roster room (correction 11; completed by the unit review's fix 2: a change of status only updates a
    shade's existing roster entry, `WorkerEntity.joinsRosterOnStatus` being false for the shade, while familiars,
    porters and Nouveau helpers still join on a change of status);
  - a shade's load drops with `Containers.dropItemStack`, whatever the block-drop rule says (the unit review's fix 3;
    the specification had `Block.popResource`, which spawns nothing with `block_drops` off);
  - a familiar whose person is online while the Concordance is off restarts its play-time count, so no fade accrues
    while off; one not loaded meanwhile counts that play at its first look after the switch is on (correction 13;
    D-P13); the play time is read through one helper, `played(person)`, which `check_workers` audits;
  - G10's shade save is in `workersKeepTheirModelsThroughASave`; the anchor joins `blockEntitiesStampTheirVersion`
    (D-P11 for the test players).
- **Tests (passed in CI run 40067977680):** `ConcordanceWorkerGameTests` +10 (the eight above and the familiar pair),
  `ConcordanceAuthorityGameTests.onlyTheOwnersSideSealsAnAnchor` and `aStandInNeverSeals`,
  `ConcordancePersistenceGameTests.anAnchorsAgreementRoundTrips`; extended: `aSpiritKeepsToItsAgreement`,
  `workersKeepTheirModelsThroughASave`, `blockEntitiesStampTheirVersion`.
- **Audits:** `check_workers` (a worker that carries things leaves them once and is struck off its roster when killed;
  the familiar's play-time fade, its restart while switched off and its lenient key); `check_pacts` (new; D-01's part:
  the outcomes in order and worded, the refusal order, the cost's order, the stand-in, the owner saved and recorded,
  the cargo rule, the unit's test names); `AUTHORITY_ROUTES` names the holder's whole sealing check
  (`Authority.isStandIn(player) || !Authority.mayChange(...)`, correction 14).
- **Commit 1 alone is not shippable:** its interim `verify` compares holders only, so the copy check (G2) closes in
  D-02's commit; the unit lands as one.

### D-02. One pact record: the shared holder, states, copies, offline and restart (unit 1)

**What the player sees.**
- Every agreement is in one state, worded distinctly: active; waiting (with its reason); suspended by its holder;
  an offering due (with the grace's end); suspended for upkeep; without its place (a pact whose perch is gone); ended
  (with its reason, in the holder's history).
- A copied anchor (or, from D-04, a copied perch) does nothing and says why (`CONFLICTING_AGREEMENT`); breaking it
  releases nothing, sends no shade away and strikes no roster entry.
- Ending twice returns nothing the second time.

**Changes (D-S6, D-S9, D-S10, D-S16).**
- **The shared agreement holder** is extracted from `SpiritAnchorBlockEntity` into one class (seal, validate against
  the ledger, re-call, end, drop once, owner, place claim), used by the anchor here and by the perch in D-04, so the
  anchor is rewritten once (critique 27).
- **`BoundWill`** (`resource/BoundWill.java:10`) gains two optional fields: `place` (a `GlobalPos`) and `hold` (cause
  `holder` or `upkeep`, and since when). The codec (`BoundWills.java:25-29`) reads them with `optionalFieldOf`, so
  `Saved.versioned("bound_wills")` stays version 1 and `Saved.keeping` (`Saved.java:109-140`) keeps reading every record.
  A six-argument constructor stays, so `ConcordanceResourceGameTests.java:129, 135` and
  `ConcordancePersistenceGameTests.java:147` build records as before. New records name their counterpart namespaced
  (`jugcraft:gathering_shade`, G9); old ones are read as written.
- **One source of truth for suspension (D-S9):** the Bound Will's `hold` is the authority. The anchor's
  `Agreement.suspended` is read once to migrate (true with no `hold` sets `hold = holder`), stays in its codec (a
  required field, `SpiritAnchorBlockEntity.java:41-48`, so an older build still reads the anchor) and is written as the
  Bound Will says. Suspend and resume therefore work while the anchor is unloaded (from D-07's roster).
- **States (D-S9):** *invited* and *negotiating* are a transient server session: nothing is stored or spent, and a
  restart forgets them. *Active* and *suspended* (cause holder or upkeep) are stored on the Bound Will. *Waiting* is a
  derived `Status`, with two reasons appended in `tools/concordance_workers.py`'s `STATUSES`, each with its own lang:
  `upkeep_due` ("an offering is due") and `upkeep_held` ("suspended until an offering is made"), distinct from
  `suspended` ("suspended by its holder", `tools/concordance_workers.py:63`). *Departed* means the record is removed and
  one entry is added to the holder's history.
- **Pact terms and escrow live in a world record (D-S6):** a new `PactRecords` SavedData (`jugcraft:pacts`, Overworld,
  `Saved.versioned`, read entry by entry with `Saved.keeping`), keyed by Bound Will id: the terms copied at sealing (an
  `Agreement` for service, area, hours and quota; an `Upkeep` of offering tag, count, period, grace and prepaid cap; the
  beneficiaries; the definition's revision), the escrow (exact stacks, at most the cap), the upkeep clock (`due`,
  `grace_until`, `warned`, `last_look`). Nothing of it lives in a block. A copied block entity therefore copies no
  escrow, and a crash between a chunk save and the world data's save cannot split escrow from its pact.
- **Departure history (D-S6):** a separate `PactHistory` SavedData (`jugcraft:pact_history`), per holder at most 8
  entries (counterpart, reason code, game time), oldest dropped, on the courier ledger's bounded-history pattern
  (`logistics/Logistics.java:45, 441`). A separate record because a non-UUID key in the Bound Wills map would be kept as
  unread and never updated (critique 5).
- **A pure `Pacts` rules class** beside `Agreement`, holding no state:
  - seal outcomes: `DONE`, `TAKEN` (the place keeps one), `ALREADY_HELD` (the holder keeps this counterpart),
    `HOLDER_FULL`, `ROSTER_FULL`, `NOT_PERMITTED`;
  - `upkeep(...)` on the Spire's model (`Spires.java:152-175`): at most one period falls due a look, and the next is set
    a period after it or, when that is already past, a period from now (`:163-166`), so absence accrues no debt; an
    offering in escrow is taken; none means a warning (once) and `grace_until`; a passed grace with none means
    `hold = upkeep`; nothing is ever taken while held; paying clears the hold and resumes;
  - **rescheduling after an absence (D-S3, critique 2a):** a look that follows a gap (no look for more than 1,200 ticks,
    a starting value: the holder away or offline, the perch unloaded) moves any passed deadline forward: a passed due
    date becomes now, so the warning shows now; a passed grace end becomes now plus the whole grace. A returning holder
    always gets the warning and the full grace;
  - `end` is holder only and happens once (`BoundWillLedger.release` already returns `UNKNOWN` the second time,
    `:75-86`).
- **Who looks (D-S3, critique 2b):** upkeep is looked at only while the place is loaded and its holder is online, alive,
  in the place's level and within 64 blocks of it: `Authority.present` (`:59-65`) plus the distance. Never through a
  stand-in (`Authority.isStandIn`, `:106-108`), whatever `concordance.absent_owner_authority` says. Nothing simulates
  elapsed time; the checks repeat on load.
- **Feature disable (D-S16):** while the Concordance is disabled, no upkeep falls due and nothing suspends; workers and
  pacts wait with `DISABLED`; records, history and escrow are untouched (as workers already stop,
  `GatheringShadeEntity.java:87-90`; `docs/TESTING.md:43`; `CLAUDE.md:64`).
- **Consistency (D-S9):** a holder block entity's agreement counts only if the ledger holds its record with the same
  id, holder and place. **Legacy records** (no `place`) are claimed by the first anchor that loads naming the record (id
  and holder) whose `spirit` its holder's roster lists; first claim wins, and the claim writes the place. An anchor whose
  shade was already lost before Chunk D has no roster entry to match; it claims by record and holder alone on its first
  load (first claim wins again). Later copies are inert: they show `CONFLICTING_AGREEMENT` (this ticket is that code's
  first user and fixes its parameters, `Code.java:33-34`), serve nothing and release nothing when broken.
- **Uniqueness (D-S10):** one agreement per place; one Stolas pact per holder; any number of holders per world. Bound
  Wills stay non-transferable; `transfer` keeps no game route.

**Reuses:** `BoundWills`, `WorkerRoster`, `Saved`, `Authority`, `Code` (#466), the Spire's rules (as a pattern; the
Spire is not edited).

**Tests (server):**

| Suite | New tests |
|---|---|
| `ConcordancePersistenceGameTests` | `aBoundWillReadsItsOlderSave` (no `place` or `hold`); `aPactRecordRoundTrips`; `thePactHistoryKeepsEight`; an unreadable pact record kept as written; the Spirit Anchor added to `blockEntitiesStampTheirVersion` and `anAnchorsAgreementRoundTrips` (G10); `aSuspendedAnchorMigratesItsHold` (a legacy `suspended: true` becomes `hold = holder`, once) |
| `ConcordanceSharingGameTests` | `aCopiedAnchorGivesNothingAndReleasesNothing` (its data copied to a second place: the copy shows `CONFLICTING_AGREEMENT`; breaking it leaves the record, the shade and the roster entry); `aLegacyAnchorClaimsItsPlaceOnce` (a save from before Chunk D with two copies: the first to load claims); `endingTwiceReturnsOnce` |
| `ConcordanceWorkerGameTests` | The rules, with fixed times: `upkeepWarnsThenSuspendsAfterGraceAndResumes`; `aReturnAfterAbsenceGetsTheFullGrace` (critique 2a); `absentAndUnloadedTimeIsNeverOwed`. And `onlyTheHolderEnds`; `twoPlayersCannotBothSealOnePlace` (the same tick: one `DONE`, one `TAKEN`, nothing spent); `noOneBindsAnotherPlayer` (the actor is always the holder; a stand-in refused; `transfer` stays `NOT_TRANSFERABLE`); `theConcordanceOffHoldsEveryClock` |
| `ConcordanceResourceGameTests` | `boundWillAstralAndPrimaKeepTheirShape` unchanged and passing (critique 5) |

**Audits:**
- `check_persistence` (`tools/check_mod_data.py:20183-20211`): both new SavedData through `Saved.versioned`, and added to
  `ENTRY_BY_ENTRY`;
- `check_workers`: `STATUSES` in order with the two appended reasons and their lang (`:17885-17893`);
- a pact `problems()` audit, run after every step as the logistics tests run theirs: each live record has one place; no
  place keeps two; escrow is non-negative and within its cap; a history holds at most 8; no `hold` without its record.

**Acceptance:** E1 (only the holder ends; a stand-in is refused; nothing transfers); E3 (a copy is inert, a legacy copy
too); E4 (ending twice returns once); E5 (`TAKEN`). E2 is shown by the rules here and on Stolas's upkeep in D-05
(critique 7): the shade has no upkeep.

**Saves:** optional `place` and `hold` on Bound Wills (version 1 stays); the new `jugcraft:pacts` and
`jugcraft:pact_history` records; the anchor's `suspended` read once to migrate. A downgrade drops the new keys on its
next write; not supported, and said so in the record.

**Review boundary:** the schema, the shared holder and pure rules, shown on the existing anchor. No new entity, block or
art.

**As built (unit 1: commit `fa5360d0d`; records in the records' commit "Concordance Chunk D: unit 1's records":
[arcane-concordance-workers.md](arcane-concordance-workers.md),
[arcane-concordance-persistence.md](arcane-concordance-persistence.md)).**
- **Built:**
  - `resource/BoundWill`: an optional `Pact`: `place` (null: without its place), `Hold` (`none`, `holder`, `upkeep`) and
    since when, `beneficiaries` (`personal` or `party`), `presence`, `Upkeep` (offering tag, count, period, grace, cap,
    and its clock: due, grace end, last look), `Service` (work, radius, hours, quota) and the day's count (`window`,
    `done_today`). The six-argument constructor stays. `BoundWillLedger.get` and `update` (the same id and holder only).
  - `spirits/BoundWills`: each entry's optional `pact` and `escrow` (exact stacks), defaults not written, still version
    1 and read will by will; `release` hands a record's escrow back once (`Released`); `deposit` (up to the cap),
    `take`, `periods`, `escrow`, `unreadable` and `problems`.
  - `spirits/PactHistory` (`jugcraft:pact_history`, version 1, read holder by holder, the last 8 departures each).
  - `worker/Pacts` (pure): `claim`, `unclaimed`, `suspend`, `resume` (the clock moves on by the time held), `due` (the
    Spire model), `done` and `didTask` (the day's count on the record), `status`, `held`, `seated`, `unseat`, `keptAt`,
    `sealed`, `service`, `terms`, `benefits`, `valid`, `problems`, `departed` and `bounded`; the numbers `PACT` mirrors
    (`ABSENT_TICKS` 1,200, `WRITE_TICKS` 200, `REACH` 64, `HISTORY` 8, and the bounds of an upkeep's terms).
    `Status.UPKEEP_DUE` and `UPKEEP_HELD` are appended and show a want sign.
  - `AgreementHolder`: `verify` (`Standing`: `NONE`, `VALID`, `ELSEWHERE` for a copy, `UNREADABLE`), with the legacy
    claim (D-P3, D-P4) and a gone record forgotten (D-P7); `held` and `state`; `look` (D-S3, D-S16); `deposit`; `end`
    with the escrow back into the living holder's inventory (overflow at their feet) and a history entry; a static
    `toggle` by record id; `taskDone` on the record; `broken` only with the Concordance on. `Authority.presentNear`;
    `CourierLedger.PLACE` made public and shared; `Code.CONFLICTING_AGREEMENT` has its first user (no parameters).
  - The anchor seals with optional upkeep terms (`sealing(terms, upkeep)`; none in play); the shade waits on an
    unreadable record and shows an upkeep hold with its suspended clip.
  - Generators: `PACT`, `PACT_HOLDS`, `PACT_DEPARTURES`, the two statuses, the D-02 messages (`held_for_upkeep`,
    `unreadable_record`, `upkeep_paid`, `upkeep_warned`, `upkeep_held`, `upkeep_resumed`, `departed.ended`,
    `departed.broken`), `CONFLICTING_AGREEMENT`'s wording in `tools/concordance_diagnostics.py`, the codex's copy
    sentence; lang regenerated.
- **Departures from this ticket and from the unit's specification** (proposals D-P1 to D-P5, D-P7 to D-P10 and D-P16 in
  Decisions):
  - **no `jugcraft:pacts`:** the pact is on the Bound Will and its escrow in the same entry (D-P1). The ticket's
    `Agreement` "for service, area, hours and quota" became the record's `Service` with its day's count (`window`,
    `done_today`), copied at sealing or at the legacy claim (the review's correction 4), so a pact seated again at a new
    perch keeps its terms and its day (D-S1, D-S6, D-S23); the block's `Agreement` is a copy its presence and its
    reading use;
  - the legacy claim is by sealed coordinates, not "first to load" (D-P3; stricter than D-S9, and it never refuses a
    genuine anchor);
  - upkeep (D-P2 as built, correction 3): a due date that passed while nobody looked becomes the moment of return, and
    a pact resumed from an upkeep hold by an offering falls due a whole period from then, so neither time away nor time
    held is charged; resumed by its holder, its last look moves on with its clock, so an absence before the hold still
    counts (the unit review's fix 1); a look in the tick of the last one goes on, so an offering paid then resumes at
    once (fix 7); a gap of at most `ABSENT_TICKS` (1,200 ticks) is not an absence, so a due date or grace end inside it
    counts at the next look (fix 8);
  - no definition revision is stored on a pact: a sealed pact keeps its copied terms, and D-04's terms revision is
    checked per negotiation (the unit review's fix 11; D-P1);
  - breaking a holder block while the Concordance is off releases nothing (correction 5; D-P16);
  - suspend and resume by record id alone (`AgreementHolder.toggle(level, player, id, now)`) for D-07's roster while the
    place is unloaded (correction 9; D-P8); `end` has the same static form;
  - a broken anchor's escrow drops where the block stood with `Containers.dropItemStack`, whatever the block-drop rule
    says (the specification had `Block.popResource`, which `doTileDrops` would suppress: the escrow was paid, not
    mined);
  - `Pacts.unclaimed` tells a record from before Chunk D that no block has claimed from one without its place
    (correction 16), for D-07's roster;
  - the anchor has no upkeep, so E2 is shown on test terms (the vanilla `#minecraft:wool` tag, one an offering, a
    7-day period, a day's grace, 4 ahead) until D-05; `look` has no production caller until D-04's perch ticker (D-P9);
  - the Gentle loophole stays (Risks); an unreadable record is tested inside `aBoundWillReadsItsOlderSave`;
    `aChangeOfOwnerMovesNoPact` and `thePactRulesKeepTheirShape` are new.
- **Tests (passed in CI run 40067977680):** `ConcordanceWorkerGameTests` +7 (`upkeepWarnsThenSuspendsAfterGraceAndResumes`,
  `aReturnAfterAbsenceGetsTheFullGrace`, `absentAndUnloadedTimeIsNeverOwed`, `onlyTheHolderEnds`,
  `twoPlayersCannotBothSealOnePlace`, `noOneBindsAnotherPlayer`, `theConcordanceOffHoldsEveryClock`),
  `ConcordanceAuthorityGameTests.aChangeOfOwnerMovesNoPact`, `ConcordancePersistenceGameTests` +4
  (`aBoundWillReadsItsOlderSave`, `aPactRecordRoundTrips`, `thePactHistoryKeepsEight`,
  `aSuspendedAnchorMigratesItsHold`), `ConcordanceSharingGameTests` +3 (`aCopiedAnchorGivesNothingAndReleasesNothing`,
  `aLegacyAnchorClaimsItsPlaceOnce`, `endingTwiceReturnsOnce`),
  `ConcordanceResourceGameTests.thePactRulesKeepTheirShape`; extended: `everyRecordReadsItsSaveFromBeforeVersions` (a
  pact, its escrow, the history) and `aSpiritKeepsToItsAgreement` (suspended on its Bound Will). With D-01's, the unit
  adds 29 server game tests: Worker 5 to 22, Authority 5 to 8, Persistence 10 to 15, Sharing 2 to 5, Resource +1. No
  test class is new.
- **Audits:** the rest of `check_pacts` (the rules' numbers, holds and departure reasons against the generator; the
  Bound Will's optional keys; no item list in a holder block; the copy check and the claim in `verify`; the day's count
  on the record; `end`'s order; `broken` while switched off; `toggle` by record; `look`; the anchor's
  `CONFLICTING_AGREEMENT`; the shade's `verify`); `ENTRY_BY_ENTRY` gains `PactHistory`; `AUTHORITY_ROUTES` gains
  `presentNear`.
- **Commands** (run and passed before each of the unit's five commits, the unit review's fixes included):
  `python3 tools/owner_art.py --check`, `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
  `python3 tools/concordance_journey.py --check` ("every route holds"), `python3 tools/check_icon_maps.py`,
  `node --test tools/world-designer/model.test.cjs`, `python3 -m unittest discover -s scripts/tests` and
  `git diff --check`. For these notes: every changed Java file parse-checked (24 files, no syntax error; the fixes' 11
  again); the pure classes (`worker/Pacts`, `Agreement`, `Status`, `Bond`, `resource/BoundWill`, `BoundWillLedger`,
  `logistics/Place`) compiled with JDK 21's `javac` against the jspecify jar, and two pure harnesses replaying the
  upkeep tests' traces and `thePactRulesKeepTheirShape`'s rules passed (with the fixes, the upkeep harness also replays
  fixes 1 and 7, and fails on the code before them); `python3 tools/select_client_tests.py --base a8c7cd770` chose every
  client class (239: `TestPlayers.java` is a shared file, D-P11).
- **Audit mutations** (run in place on the final tree, every file restored after each and the tree verified
  unchanged; run again with the unit review's fixes): the specification's 30 (M1 to M12, M14 to M31), 16 for the
  review's corrections (the cost asked after the call and paid before the record, the cargo into any anchor,
  `cannot_afford` dropped from `SEAL_OUTCOMES`, a renamed `theHolderEndsWithoutBreaking`, the play-time helper reading
  game time, no restart while switched off, the fade counted only beyond the follow distance, a renamed
  `aFamiliarFadesWhileItsPersonPlaysElsewhere`, `broken` releasing while switched off, the record `toggle` letting a
  stand-in through, `taskDone` counting only on the block, `done_today` and `service` made required, the legacy claim
  copying nothing, the generator's `REACH` 32) and 7 for the unit review's fixes (M48 to M54: `recall` letting a
  stand-in through, `deposit` taking anyone's offering, the shade's load by `Block.popResource`, the shade joining its
  roster on any status, `setStatus` noting every worker, `resume` setting the last look to now, a look in the last
  look's tick doing nothing). 52 of the 53 are reported and the unchanged tree reports nothing. Not reported: counting
  the familiar's fade only beyond its follow distance, as before Chunk D; `aFamiliarFadesWhileItsPersonPlaysElsewhere`'s
  last step catches it when it runs. M5 (`ROSTER_FULL`'s id changed) is reported by the outcome checks, not by a
  missing lang key as the specification expected: the lang check reads the generator's list.
- **The unit review's fixes** (commit "Chunk D unit 1: fixes from the unit review", after the records' commit
  `97e1facd4`; 12 findings confirmed against the code, 24 rejected, no new proposal id):
  1. resumed by its holder, a pact's last look moves on by the time held (never past now), so an absence before the
     hold is still one: warned again with the whole grace, or paid once with the next a period from the return (D-P2).
     Tested at the end of `aReturnAfterAbsenceGetsTheFullGrace` and in `thePactRulesKeepTheirShape`;
  2. a change of status no longer puts a Gathering Shade on its holder's roster (`WorkerEntity.joinsRosterOnStatus`,
     false for the shade only), so one replaced, or loading while switched off or away from its anchor, takes no room;
     it joins only by its seal, its recall or its own decision (completing correction 11). Tested in
     `anUnloadedShadeReplacedLeavesOnceAndDeliversOnce`. A released Hearthling that loads while switched off is still
     noted back (since step 17; not changed here);
  3. a shade's load drops with `Containers.dropItemStack`, so none is lost with the block-drop rule off;
     `aKilledShadeAwayFromItsAnchorDropsWhereItFell` turns the rule off around the kill and gives it back in `finally`;
  4. the codex page "The Bond" (regenerated), the generator's docstring and the `Bond` and `HearthlingEntity` Javadocs
     say that play with the Concordance switched off does not count only while the familiar is loaded (D-P13);
  5. `check_pacts` holds `recall`'s stand-in refusal and `deposit`'s holder check; in `aStandInNeverSeals` a stand-in
     with the owner's identity is refused a recall and calls nothing, and in `onlyTheHolderEnds` a party member's
     offering is refused;
  6. "not yet pushed" is said as of the writing (CHANGELOG, INTEGRATION_STATUS);
  7. a look in the tick of the last look goes on (`now < lookedAt`), so an offering paid in the tick of a ticker's look
     resumes at once; `upkeepWarnsThenSuspendsAfterGraceAndResumes` looks in the offering's tick first;
  8. a gap of at most `ABSENT_TICKS` (1,200 ticks) is not an absence, so a due date or grace end inside it counts:
     recorded in D-P2, D-S16 and every record that said time away or switched off is never owed;
  9. one escrow stack this version cannot read keeps its whole Bound Will unread (D-P1, the persistence record); unit 2
     decides, before D-05's offerings, whether to decode escrow stack by stack;
  10. D-P4's trade-off with a crash between a chunk save and the world data's save (D-P4, Risks);
  11. no definition revision is stored on a pact (the departures above; D-P1);
  12. the saved-data paths in the `PactHistory`, `BoundWills` and `WorkerRoster` Javadocs are `data/jugcraft/<name>.dat`
      (`SavedDataStorage` resolves the id's namespace as a folder); the bond's `seen_played` is marked built in
      Interfaces.
  Beyond the findings, `check_pacts` also holds fixes 1, 2, 3 and 7 (M50 to M54 above), and `WorkerRoster`'s Javadoc
  says a shade only updates an entry it has.
- **Not run:** `./gradlew build` (with or without the optional integrations) and every server and client game test (CI
  is their first compile and run), any client, two clients, a real world saved before Chunk D, a server restart. Since
  run in CI: run 40067977680 on `da185dd0d` passed every job, these tests among them; on the merged branch, runs
  40094972598, 40121823509 and 40133540741 passed them again.
- **Not confirmed until CI:** that DFU's `optionalFieldOf` fails on a present malformed value (so an unknown hold keeps
  its entry unread; read in the DFU source, not run); that `NbtOps` refuses a string tag read as a long (the unreadable
  `seen_played` step); the game-test server's Nether, writable player and statistics files and enabled parties; that a
  Fabric `FakePlayer`'s messages are harmless; that a SmartBrainLib brain works on a Hearthling loaded and added within
  the test's tick.

### D-03. Stolas is found through Styx, or without him (unit 2)

**What the player sees.**
- Talking to Styx, a player who has begun the Binding Arts hears a line chosen from their own progress toward Stolas's
  Regard: what kind of evidence Stolas values and what to do next, and, while they have recorded no sky observation,
  that Stolas wants a star seen through an observatory. Styx grants nothing; his gift and his four lines stay.
- The journal shows the new entry and its next step, and names the sky step separately.
- A player without Styx finds the entry through the codex's Binding category and the specimens.

**Changes (D-S12).**
- **New research `jugcraft:stolas_regard`** (`data/jugcraft/concordance/research/stolas_regard.json`, from the
  generator; stable once released): principle tether, tradition spiritbinders and stage practitioner, as the Binding
  Arts (`research/binding_arts.json:3-5`); requires `binding_arts` understood. States: encountered, examine
  `#jugcraft:stolas_specimens`; observed, `practice jugcraft:cultivation`; understood, `practice jugcraft:assay`;
  mastered, `practice jugcraft:pact_service` (the pact's first obligation, D-05). **No `notes` alternative** at any
  state: copied notes never stand in (03:132; notes never reach mastered anyway, `EvidenceRule.java:74-76`). The engine
  keeps evidence for a later state until the earlier ones are reached (`rules/ResearchEngine.java:21-22`), so the
  steps can be done in any order. The plan's "three evidence tokens" become knowledge keys, not items: nothing to copy
  (a recorded deviation from 03:132).
- **The practice gate ships here (critique 11):** `practice/pact_service.json` (tradition spiritbinders, requires
  `stolas_regard` understood). Without it the progression graph would find nothing providing `practice:pact_service`
  and mastery would be unreachable (`progression/ProgressionGraph.java:93, 112-130`).
- **The reward has a consumer (critique 18):** mastering Stolas's Regard unlocks the workshop hand (D-08), checked in
  code as the Binding Arts are (`Workers.knows`, `Workers.java:123-125`). An entry's `unlocks` names only invocations,
  workings and rituals (`rules/RulesParser.java:101-110`); `binding_arts.json:53` has none. The "bounded insight" of
  03:137 is this unlock; the delivery plan's forecast insight is dropped, because `Sky.obscured` reads the present
  heightmap and weather and cannot say whether a future window will be visible (`Sky.java:187-195`).
- **Specimens `#jugcraft:stolas_specimens`:** `stolas_starflower`, `ravenquill_lupine`, `amethyst_mourningbell` (Styx
  flowers, `JugcraftStyx.java:24-25`) and `minecraft:feather`, the non-Styx specimen, so the route is reachable without
  Styx (G12; `CLAUDE.md:57`). The tag joins `#jugcraft:concordance_specimens`, which examination reads; examination
  gains a line for these specimens instead of Circle Lore's "form" line (`concordance/Examination.java:96-98`).
- **The sky step (D-S12):** the invitation's precondition (D-04) is `AstralClaims.observed(player)` being non-empty
  (`AstralClaims.java:83-85`), written by any observatory use while a pattern is up and visible
  (`ObservatoryBlockEntity.java:205-220`). It is not research evidence and `ResearchEngine.explain` does not cover it, so
  Styx's hint and the journal name it separately. A player with none waits for the next visible window (Lantern Star
  every 3 days in clear weather, New Moon every 8 in any weather: `concordance/pattern/*.json`). Nothing is recorded
  retroactively.
- **Companion data, through the generators (critique 11):** four advancements
  (`advancement/concordance_stolas_regard_{encountered,observed,understood,mastered}.json`, as Binding Arts' four); the
  Modonomicon research facts, nodes and hooks (`modonomicon/research/concordance/{facts,nodes,hooks}.json`, written by
  `tools/concordance.py`); a codex entry `modonomicon/books/arcane_concordance/entries/binding/stolas.json` in the Binding
  category, the non-Styx route's reading.
- **Styx:** `Styxhexenhammer.mobInteract` (`:84-90`) keeps the gift and the rotating lines, and adds the hint built
  from the listener's own `ResearchEngine.explain` for `stolas_regard` and their observations. The lines come from
  `tools/styx.py` (`:193-202`), not by hand; the blueprints are frozen and untouched (`StyxGameTests.java:89-90`). Talking
  teaches nothing, as the Academy's lesson already says of its instructor (`en_us.json:7`).
- **The journal's "pacts" section** (the 14th of 24, `Journal.java:53, 86-106`) begins here: shown from Binding Arts
  understood, it names Stolas's Regard's next step and the sky step; D-07 adds the pacts.
- **Route depth:** the progression graph's practice gates put Verdant Husbandry and the Assay at understood before
  their practice (`practice/cultivation.json`, `practice/assay.json`), but only the graph reads them
  (`ProgressionGraph.java:112-130`): in play the practice is recorded for anyone who harvests a ripe organism crop or
  dissolves at the Assayer's Scale (`garden/OrganismCropBlock.java:338-349`; `assay/Assaying.java:224-233, 295`), so
  in code only the Binding Arts gate Stolas's Regard. Whether D-03 checks the two researches as well is settled when it
  is built and recorded in its As built note. The sky step asks no research. `tools/concordance_journey.py` and
  `check_acquisition` confirm the route.

**Reuses:** the research engine and journal explain, Styx's interaction and rate limit, examination, `AstralClaims`, the
Modonomicon Binding category, the advancement pattern.

**Tests (server):**

| Suite | New tests |
|---|---|
| `StyxGameTests` | `styxHintsByTheListenersOwnProgress`; `talkingGrantsNoResearch`; `styxNamesTheSkyStepUntilOneIsRecorded` |
| `ConcordanceSharingGameTests` | `notesNeverStandInForStolasEvidence` |
| `ConcordanceProgressionGameTests` | `stolasRegardIsReachable` (its gates exist; nothing it needs is unprovided) |
| `ConcordanceSkyGameTests` | `aRecordedObservationCountsWithoutAWindow` (observed earlier, read later) |
| `ConcordanceJournalGameTests` | `thePactsSectionNamesTheSkyStep`; hidden before the Binding Arts |

**Audits:** the research parse; the progression graph; `check_acquisition`; `concordance_journey.py --check`; the
advancement and codex audits as for other entries; `stolas_specimens` holds a non-Styx item.

**Acceptance:** E1 and E3 for personal evidence (knowledge is per player; notes never count). The route is reachable
with and without Styx.

**Saves:** none (the knowledge attachment is unchanged).

**Review boundary:** discovery only: nothing is spent, no entity.

**As built (unit 2: commit `9daf23ae1`; records in the records' commit "Concordance Chunk D: unit 2 records (D-03, D-04,
D-05)": [stolas-pact.md](stolas-pact.md), [styxhexenhammer.md](styxhexenhammer.md),
[arcane-concordance-journal.md](arcane-concordance-journal.md)).**
- **Built:**
  - `concordance/spirits/StolasRegard` (new): `RESEARCH`, `ACTIVITY` (`jugcraft:pact_service`) and `SPECIMENS`; `step`
    reads the listener's own `ResearchEngine.explain` for the entry as one of seven steps (`binding`, `examine`,
    `cultivate`, `assay`, `serve`, `kept`, `journal`; D-P20); `hint` (Styx's words for that step, then the sky step
    until an observation is recorded, except once kept; nothing with the Concordance off) and `report` (the journal's
    Pacts section: the step, and the sky step apart). It records and grants nothing.
  - `Styxhexenhammer.says`: when he has no cuttings to give, his rotating line as before, then the listener's hint; the
    gift, `nextTalk` and the frozen blueprints are untouched.
  - `Journal`: the `pacts` section right after `logistics`, shown from the Binding Arts understood (`Workers::knows`;
    D-P21). `Examination`: a line for Stolas's specimens before Circle Lore's form line. `ConcordanceClient`: the
    specimens' tooltip.
  - Generators: `tools/concordance_pacts.py` (new, merged by `tools/concordance.py`: the research with its
    advancements and lock lines, `#jugcraft:stolas_specimens` joined to `#jugcraft:concordance_specimens`, the journal
    and examination words, the codex entry `binding/stolas_regard` at (0,4); D-P18); `tools/styx.py` (`STOLAS_LINES`
    and the sky line); `tools/concordance_progression.py` (the `pact_service` practice gate, and Styx's flowers as
    found sources, D-P19); `tools/concordance_journal.py` (`pacts`); `tools/concordance_workers.py` (the Binding Arts'
    Understood page links the entry); `tools/concordance_journey.py` (the Stolas's Regard route, steps 1 to 4).
    Generated: the research, `practice/pact_service.json`, the specimens tag, four advancements, the Modonomicon
    research facts, nodes and hooks, the codex entry and lang; the journey record's table and the guide's research
    table (`docs/ARCANE_CONCORDANCE_GUIDE.md`, from `tools/concordance_delivery.py`) pasted.
- **Departures from this ticket and from the unit's specification** (proposals D-P17 to D-P22 in Decisions):
  - only the Binding Arts gate the entry in code (D-P17); the progression graph's gates ask more, never less;
  - the codex is two entries, `binding/stolas_regard` here and `binding/stolas_pact` in D-04, not `binding/stolas.json`
    (D-P18); the Binding Arts' link is in its lang text, so its entry file did not change (the specification expected
    it to);
  - the Sky suite's second test is `aTurnedClockMakesNoNewClaim` (an observation's claim is not made again by turning
    the clock), and the Journal suite's is `thePactsSectionWaitsForTheBindingArts` (the ticket's "hidden before the
    Binding Arts");
  - the guide's research table had to be regenerated as well (the first local check said so).
- **Tests (passed in CI run 40094972598; `talkingGrantsNoResearch` failed in run 40090825848 as a test defect, fixed in
  `8a3425f9e`):** `StyxGameTests` +3 (7 to 10: `styxHintsByTheListenersOwnProgress`,
  `talkingGrantsNoResearch`, `styxNamesTheSkyStepUntilOneIsRecorded`), `ConcordanceSharingGameTests`
  `notesNeverStandInForStolasEvidence`, `ConcordanceProgressionGameTests.stolasRegardIsReachable`,
  `ConcordanceSkyGameTests` +2 (`aRecordedObservationCountsWithoutAWindow`, `aTurnedClockMakesNoNewClaim`),
  `ConcordanceJournalGameTests` +2 (`thePactsSectionNamesTheSkyStep`, `thePactsSectionWaitsForTheBindingArts`): 9.
- **Audits:** `check_stolas_regard` (new): the research's gate, states and single ways (no notes); the specimens tag
  with a thing anyone has without Styx, inside the specimens examination reads; `StolasRegard`'s constants and steps
  against `tools/concordance_pacts.py` and `tools/styx.py`; the hint and report only read; Styx keeps his gift and line;
  the journal section's place and gate; the examination line; the codex entry's opening; the D-03 test names. The
  progression graph reaches all four states at Practitioner in the Overworld; `concordance_journey --check` holds the
  route.
- **Audit mutations:** the specification's 18 (D3-M1 to D3-M18) were each reported; the unchanged tree (D3-M19) reported
  nothing.

### D-04. The perch, the invitation, the conversation, terms, acceptance and refusal (unit 2)

**What the player sees.**
- The player crafts a **pact perch** and places it where they may build (D-S15): claims and spawn protection apply, and
  Styx's protected district refuses it. The perch records its owner (D-S8).
- The owner uses it, with Stolas's Regard understood and a sky observation recorded: the invitation opens and Stolas
  arrives at the perch (D-S7). In unit 2 his body may be provisional; D-06 gives him his own.
- A conversation screen gives his three dialogue beats, then the terms page (D-S15): the service (Star-reckoning), hours
  (any hour, a starting value), area (8 blocks round the perch, a starting value), daily quota (1), upkeep (1 offering
  every 7 game days, up to 4 prepaid) and grace (1 game day), what suspension does (the service stops; nothing is taken
  or lost), duration (until ended), breach rules (a missed offering suspends; nothing else breaches; a pact never ends
  by itself), how to end (at the perch or from the roster), what returns (the escrow, once, to the holder), the
  beneficiaries (the holder alone) and the Open Hand Compact's cost.
- **Accept** spends the compact's cost in the same server action that seals (D-S13). **Decline**, or any refusal,
  spends nothing and names the reason: missing evidence (each step named); no sky observation; "this perch keeps
  another's agreement" (`TAKEN`); "you already keep a pact with Stolas" (`ALREADY_HELD`); "you hold 32 agreements"
  (`HOLDER_FULL`); a full roster (`ROSTER_FULL`); "only the perch's owner may invite here" (`NOT_PERMITTED`); short of
  the compact's materials or resonance; the Concordance is off; the pact's definition did not load.
- Two players negotiating at one perch: the second **Accept** is refused as `TAKEN` and spends nothing (D-S8).

**Changes.**
- **Block, item and block entity `jugcraft:pact_perch`** (stable once released): a place marker using the shared holder
  (D-02). It records its owner and the pact it seats and **holds no items** (D-S6): it is not a container, so hoppers,
  pipes and Fabric's item storage find nothing to take, and an explosion can destroy no escrow. Breaking or blowing it up
  never ends the pact: the pact is "without its perch", Stolas leaves the world, and the record and escrow stay
  (D-05). Its recipe is ordinary materials; its art is new under `docs/ITEM_ICONS.md` and `docs/ART_DIRECTION.md`.
- **The pact definition is data** (`data/jugcraft/concordance/pact/stolas.json`, a strict parser: `Json.only`, a
  schema version), not a fourth `WorkerDefinition` kind, so `check_workers`' three kinds and Binding Arts' "distinct 3"
  keep their meaning (`tools/check_mod_data.py:17896-17907`). It holds the service, terms, upkeep, compact cost and the
  dialogue keys. An invalid or missing definition reports cleanly on reload and refuses new invitations with a reason;
  sealed pacts keep the terms copied at sealing (D-S23; 11:59).
- **The Open Hand Compact is the acceptance cost, not a rite (D-S13):** ordinary materials and Astral Resonance from
  the holder's astrolabe (`AstrolabeItem.charge/setCharge`, `:42-56`; capacity 32, `:36`), checked and spent in the call
  that seals; a refusal spends nothing. No new ritual result kind (`RitualDefinition.java:56-71`; compare CC's Stillwater
  deferral). The compact counts as the first period's offering: the first offering falls due 7 game days after sealing.
  The materials and resonance are starting values in the generator, recorded in the As built notes.
- **Transport (D-S14):** a perch-bound `ExtendedMenuProvider` menu on the `ShopMenu` pattern
  (`town/ShopMenu.java:129-136, 143-160`): `stillValid` while the perch is loaded, the opener is within reach and the
  perch still stands. The menu has
  **no slots**, so nothing can be duplicated through them (its `quickMoveStack` returns nothing, as `ShopMenu.java:138-141`
  does); deposits are server-side actions (critique 21).
  Vanilla's `clickMenuButton` carries one int (`ShopMenu.java:144`), too little for a sequence and a revision, so
  requests are one small client-to-server payload, `PactRequestPayload(containerId, sequence, action, choice,
  revision)`, all bounded integers: `action` indexes a fixed list (choose, accept, decline, recall, deposit, suspend,
  resume, end, seat); `choice` indexes a list the server sent (an observed pattern, an offering kind); `sequence` rises
  per menu and a stale one is dropped; `revision` must equal the terms revision the server last sent, or the answer is
  "changed" and the terms are sent again. The server checks the menu's container id, `stillValid`, the actor (never a
  stand-in), a `RateGate` and the authority, then answers with a `PactStatePayload` (terms rows, choices, revision, last
  result; bounded lists). No free text, no player ids, no client totals (08:99); no chat commands (12:54); no vanilla
  Dialog, whose custom-click hook gets no player and would need a mixin (`mc263src`
  `net/minecraft/server/MinecraftServer.java:1955-1957`); no GuiLib (no narration *(survey)*).
- **A native `PactScreen`** from `BookArt.Tab`/`Paragraph` and `ComposerScroll` (`BookArt.java:69, 93`), narrated and keyboard
  operable; it adopts `ScreenKit.Fitted` if #469 merges first.
- **`StolasEntity extends WorkerEntity`**, in `concordance/spirits/` so `check_workers`' bans on chunk loading,
  dimension changes, level-taking teleports and brain memories cover it (`tools/check_mod_data.py:17925-17937`;
  critique 13); `MobCategory.MISC`, as the workers are (`Workers.java:76-81`), so a transport crate refuses him
  (`local/peepo/MobTransportCrate.java:53-57`); roster kind `pact`, with kind lang. **Lifecycle (D-S7):** he arrives at
  the perch when the invitation opens, as a visitor bound to that session; he leaves when he has neither a pact record
  nor a live session (decline, close, timeout, disconnect, restart), checked each think on the shade's pattern
  (`GatheringShadeEntity.java:99-103`), since a worker is persistent (`WorkerEntity.java:71`). After acceptance he is the
  pact's presence: one presence per pact; he takes no damage but damage that bypasses invulnerability (Styx's rule,
  `Styxhexenhammer.java:53-55`); a lost presence is called back at no cost and never ends the pact.
- New items (the perch's block item, any compact item) go into `equivalence/excluded`; `values.json` and the survival
  registry are regenerated (CC's Where to resume, item 1). Consumed offerings stay in equivalence (D-S19).

**Reuses:** D-02's holder, rules and records; `BoundWills`, `WorkerRoster`, `Authority`, `RateGate`, `AstralClaims`,
`ConcordanceProgress.knowledge`, `Code.CONFLICTING_AGREEMENT`.

**Tests (server):**

| Suite | New tests |
|---|---|
| `ConcordanceWorkerGameTests` | `stolasExplainsBeforeAgreeing` (the terms sent, nothing stored or spent); `aStaleTermsRevisionIsRefused`; `aRefusalSpendsNothing` (a survival player); `acceptingSealsOnceAndSpendsOnce` (a survival player: materials and resonance gone once; one record); `oneStolasPactPerHolder`; `anotherPlayerCannotAcceptForTheHolder` (a party member at the owner's perch: `NOT_PERMITTED`; a request naming another menu's container: dropped); `twoNegotiationsAtOnePerchSealOnce` (`TAKEN`, nothing spent); `aVisitingStolasLeavesWithoutARecord` (decline, close, timeout, disconnect, and a reload with no session); `aCrateRefusesStolas` (`MobTransportCrate.friendly` false, `capture` refused); `stolasTakesNoOrdinaryDamage`; `aBrokenPactDefinitionIsReported` (a reload with `stolas.json` invalid, then missing: invitations refused with the reason; a sealed pact keeps its terms) |
| `ConcordancePersistenceGameTests` | the perch in `blockEntitiesStampTheirVersion`; `thePerchHoldsNothing` (a hopper beneath takes nothing; an explosion drops only the perch's item; the escrow is unchanged) |
| `ConcordanceSharingGameTests` | `aCopiedPerchIsInert` |

The hopper and explosion tests are moot by design (the perch holds nothing) and are kept as proof of it.

**Tests (client):** a new `ConcordancePactClientGameTests`: the conversation and the terms page at GUI scale 2 and 4
and in a small window; acceptance by keyboard alone; toasts cleared before each shot. A `WEIGHTS` entry in
`tools/select_client_tests.py:53` if the class is long.

**Audits:** a new `check_pacts` (the definition's fields and bounds; its lang; the perch in the exclusion tag; the
roster kind's lang); the icon maps; the recipe-conflict fixture once #462 merges.

**Acceptance:** E1 (only the actor binds themself; another player cannot accept; the server trusts no client); E3 (a
copied perch is inert); E5 (one pact a place; one Stolas pact a holder; two negotiations seal once); refusal and failure
dialogue (09:158).

**Saves:** the perch's block entity (owner, pact id; stamp 1); the Bound Will's `place`; the pact record.

**Review boundary:** the first half of the end-to-end path.

**As built, server (unit 2: commit `832129b95`; records: [stolas-pact.md](stolas-pact.md),
[arcane-concordance-workers.md](arcane-concordance-workers.md)).**
- **Built:**
  - `concordance/spirits/PactPerchBlock` and `PactPerchBlockEntity` (new; `jugcraft:pact_perch` is the block, item,
    block entity and menu): placed only by a player in person where `Authority.mayChange` allows, never in Styx's home
    (`StyxConservatory.covers`, new; D-P31) and never by a stand-in; it records its placer (D-P24), keeps its pact
    through an `AgreementHolder` built with `OnBreak.UNSEAT` and an owner required (the unit review: a perch a machine
    sets, through a blueprint stake or a drone build, which never asks `getStateForPlacement` or `setPlacedBy`, records
    nobody and so seals and seats for nobody), holds no item and stamps version 1. Its ticker looks once a second,
    spread by position (`PactKeeping.tick`: the upkeep look, and a lost presence called back while its holder is within
    64 blocks; D-P28). While a conversation is open and nothing is kept there, one visiting Stolas waits on it. The
    perch never saves its visitor; the visitor himself is written with his chunk (`perch` only) and leaves at his first
    think after a load (`aVisitingStolasLeavesWithoutARecord`), with the Concordance on or off (the unit review).
  - `AgreementHolder` (unit 1's) gains `OnBreak` (`RELEASE` for the anchor, its body unit 1's exactly; `UNSEAT` for the
    perch: the record kept without its place, its presence struck off the roster and sent away), `seat` with its `Seat`
    outcomes (worded `workers.seat_*` from `SEAT_OUTCOMES`; D-P45), `stranded` (D-P25) and `told(Seat)`; `offeringName`
    and `days` are widened to the package. `worker/Pacts` gains `pactWith` (a holder's pact with a counterpart).
  - `concordance/spirits/StolasEntity` (new; `jugcraft:stolas`): a `WorkerEntity` and the holder's `Presence`, MISC (a
    transport crate refuses him), 0.7 by 1.45, fire-immune; a visitor bound to no one and on no roster while a
    conversation at his perch is live, then the pact's one presence (roster kind `pact`); only damage that bypasses
    invulnerability harms him; he saves `perch` and `record`. His renderer comes in the client commit.
  - `concordance/worker/PactDefinition`, the strict `PactParser` (schema 1) and `PactCatalog`, a new `pact` kind on
    `ConcordanceRules` (`withPacts`; D-P29), read from `data/jugcraft/concordance/pact/stolas.json`: a missing or
    invalid definition is reported with the rules' problems and refuses new invitations with the reason; sealed pacts
    keep the terms copied onto their record (D-S23).
  - `concordance/spirits/PactMenu` (an `ExtendedMenuType` with no slots and no button ids), `PactRequestPayload` (five
    VarInts), `PactStatePayload` (one shape for four modes, D-P33), `PactSessions` (memory only; forgotten when the
    player leaves, on disconnect and at server stop; D-P27), `PactCompact` (the compact's needs, the one astrolabe
    picker, `affords` and `pay`; creative pays nothing, D-P26) and `PactKeeping` (the look, deposits by kind,
    idempotent suspend and resume, ending, a pact without its perch ended from a perch keeping nothing, the page). A
    request is checked in this order: a stand-in, another container, `stillValid`, an old sequence, the rate gate
    (`pact`; `pact_choose` for CHOOSE, D-P35; `pact_open` for opening the perch), the terms revision (the offer's,
    D-P23), then the action; the inventory menu then broadcasts its changes (D-P36). ACCEPT is offered only when
    nothing is missing (D-P34); CHOOSE answers with the state alone. `Workers` registers Stolas, the perch, its block
    entity, item and menu.
  - Generators: `tools/concordance_pacts.py` (D-04's section: `PARSER`, `STATE_LIMITS`, `SESSION`, `ACTIONS`, `MODES`,
    `TERMS`, `ANSWERS`, `OFFERINGS`, `PACTS`, the perch's models, recipe, loot table and axe tag, the codex entry
    `binding/stolas_pact` at (2,4), linked from the Regard's Understood page (D-P18), and the messages);
    `tools/concordance_workers.py` (`SEAT_OUTCOMES`, `workers.kind.pact`, `workers.seat_*`);
    `tools/concordance_progression.py` (the `pact_service` gate's graph needs an observatory and a perch; D-P22);
    `tools/concordance_equivalence.py` (the perch excluded); `tools/concordance_worker_art.py` and
    `tools/item_icons/pact_perch.txt` (the icon). `#jugcraft:stolas_offerings` ships here (D-P30); the recipe uses
    polished deepslate (D-P32).
  - **Starting values** (`concordance_pacts.PACTS`, written to `concordance/pact/stolas.json`): Star-reckoning within 8
    blocks of the perch, any hour (0 to 24,000), 1 a game day; upkeep 1 offering every 7 game days, 1 game day of
    grace, at most 4 paid ahead; beneficiaries personal; the Open Hand Compact 2 amethyst shards, 3 feathers and 8
    Astral Resonance; three beats (greeting, service, price).
  - **Value and registry:** `alchemy/values.json` regenerated with `tools/build_alchemy_values.py --minecraft-jar` on
    the vanilla 26.3 data zip (Chunks B's and C's route), which values `jugcraft:pact_perch` from its recipe at 74 (the
    perch stays excluded from equivalence); `docs/audits/survival-item-registry.json` gains `jugcraft:pact_perch`, as
    Chunk C's `24a23db1a` added its items: no game export was run, and `survival-recipes.json` and `.csv` were not
    regenerated.
- **Departures from this ticket and from the unit's specification** (proposals D-P18, D-P22 to D-P37 and D-P45 to D-P48
  in Decisions):
  - the terms are 14 rows: the ticket's 13 and "perch" (what breaking it does);
  - seating a pact without its perch is `AgreementHolder.seat`, shared by every holder block, with its own outcomes;
    the perch part's `Pacts.unseated` was not added;
  - review correction 1: `broken` adds only the `UNSEAT` branch; the anchor's `RELEASE` body is unit 1's (its escrow
    dropped with `Containers.dropItemStack`, whatever the drop rule; no `Block.popResource`);
  - review correction 2: `aCrateRefusesStolas` spawns its cow with `EntityTypes.COW` (`animal.cow.Cow`);
  - review correction 9: sessions are also forgotten on `ServerPlayerEvents.LEAVE` (as `RateGate`'s limits are), not
    only on disconnect; a visitor waits while any conversation at his perch is live, the owner's or another's;
  - review correction 10: `thePerchHoldsNothing` also checks that the explosion drops at least the perch's own item and
    otherwise only block items (the blocks it broke, their own), never an offering;
  - `aBrokenPactDefinitionIsReported` and `aStaleTermsRevisionIsRefused` swap the rules inside the `try` whose
    `finally` restores them, as `check_mod_data`'s rules-swap audit requires (the specification swapped them just
    before it);
  - `aCopiedPerchIsInert` gives its survival holder the compact first (the specification's holder could not pay);
  - the perch's icon was drawn by `tools/item_icons.py` (through `tools/concordance_worker_art.py`) and checked to be
    its map's drawing; `tools/generate_textures.py`, which the specification listed, was not run;
  - test messages cite no proposal number (the specification's placeholders were renumbered for these records).
- **Tests (passed in CI run 40094972598; `aBrokenPactDefinitionIsReported` failed in run 40090825848 as a test defect,
  fixed in `8a3425f9e`):** `ConcordanceWorkerGameTests` +12 (`stolasExplainsBeforeAgreeing`,
  `aStaleTermsRevisionIsRefused`, `aRefusalSpendsNothing`, `acceptingSealsOnceAndSpendsOnce`, `oneStolasPactPerHolder`,
  `anotherPlayerCannotAcceptForTheHolder`, `twoNegotiationsAtOnePerchSealOnce`, `aVisitingStolasLeavesWithoutARecord`,
  `aCrateRefusesStolas`, `stolasTakesNoOrdinaryDamage`, `aBrokenPactDefinitionIsReported`,
  `thePactScreensMessagesAreBounded`), `ConcordancePersistenceGameTests` +2 (`aPerchKeepsItsOwnerAndPact`,
  `thePerchHoldsNothing`), `ConcordanceSharingGameTests.aCopiedPerchIsInert`,
  `ConcordanceAuthorityGameTests.aPerchIsPlacedOnlyWhereItsPlacerMayBuild`,
  `ConcordanceResourceGameTests.thePactDefinitionParsesStrictly`: 17. Extended: `blockEntitiesStampTheirVersion` (the
  perch) and `aChangeOfOwnerMovesNoPact` (the perch's land changing hands moves no pact). The helpers are
  package-private statics in `ConcordanceWorkerGameTests` (D-P37); spending tests use `TestPlayers.survivor`.
- **Audits:** `check_pacts` gains D-04's part: the parser's, payloads', sessions' and keeping's numbers against
  `tools/concordance_pacts.py`; the actions, modes, terms and answers in order; a request of five bounded numbers; the
  menu's checks in order, its rate gate and no slots; acceptance by the owner alone with the compact paid by the seal;
  the definition and offerings against the generator (one offering reachable without Styx, offerings kept in
  equivalence); the perch holding no item, recording its placer and asking `mayPlace`; `broken`, `unseat` and `seat`
  in order; Stolas MISC, a worker and a presence, hurt only past invulnerability; the perch excluded, registered,
  modelled and its icon its map; `StyxConservatory.covers`; the D-04 test names.
- **Audit mutations:** the specification's 26 (D4-M1 to D4-M26, the slab recipe among them, which gave 12 reports) were
  each reported; the unchanged tree (D4-M27) reported nothing. D4-M16 (`setPlacedBy` without its stand-in check) gave
  one report, the owner check's; the specification expected `AUTHORITY_ROUTES` to report it too, and it did not.

**As built, client (unit 2: commit `7da686ad4`; record: [stolas-pact.md](stolas-pact.md#the-pact-screen-d-04)).**
- **Built:**
  - `client/PactScreen` (new): a plain `Screen` with `MenuAccess<PactMenu>` (D-P38); the beats and the terms as pages,
    CHOOSE(0) sent once when the terms page first shows (D-P39); End through vanilla's `ConfirmScreen`, where Esc and no
    keep the pact (D-P40); every line a focusable, narrated paragraph and every control a button; the keyboard lands on
    the terms' first line, never on Accept; it fits windows down to 320×180 scaled pixels and scrolls its body.
  - `client/PactClient` (new): the five numbers for the open menu, with a rising sequence and the revision shown; the
    last state for that menu kept (a state for another menu dropped); a request waits at most 100 client ticks for
    its answer; answers read aloud; `PactScreen` registered with `MenuScreens`. `ConcordanceClient` registers it.
  - `client/WorkerClient`: Stolas drawn by GeckoLib at 1.4× (`STOLAS_SCALE`; D-P41) from the owner's Flashjack rig,
    copied byte for byte by `tools/owner_art.py` (`concordance_pacts.OWNER_FILES` in `magic_imports`); his clips
    `idle.sitting` (played and held), `idle.ground` and `idle.flapping`. The provenance (library paths, SHA-256, catalog
    rows and the folder's leftover licence label) is in [stolas-pact.md](stolas-pact.md#dependencies-and-assets).
  - Audits: `check_geckolib` pins clips played and held and gains a per-face mode for an owner's rig copied as drawn
    (`faces=True`, Stolas only: three UV-region overlaps accepted, no face overlaps; D-P42); `check_pact_screen` (new)
    holds the client, the screen, the body and the client test to `tools/concordance_pacts.py` (`TERM_LABELS`,
    `SCREEN`, `BODY_SCALE`, `BODY_CLIPS`, `CLIENT_SHOTS`).
  - `ConcordancePactClientGameTests` (new; in the gametest `fabric.mod.json`), D-04's steps: the conversation, the
    terms and a beat at three sizes, acceptance refused (`cannot_afford`) and then sealed by keyboard, Stolas on his
    perch: nine shots. `tools/select_client_tests.py` weighs it at 150 s (an estimate, not a measurement), and
    `scripts/tests/test_client_test_selection.py` gains `test_stolas_assets_select_the_pact_client_test`.
- **Departures from the unit's specification:** review correction 6 is built as each step that opens the perch waiting
  for a menu newer than the one open before, with its first state (and, in D-05's service step, one open with the
  offering given first); the test checks `PactClient.state()` for null before reading it, names
  `PactRequestPayload.ACCEPT` instead of its index and logs the service step's offering under `[pact-check]`.
- **Tests (passed in CI runs 40090825848 and 40094972598):** the client test above.
- **Audit mutations:** the specification's 17 (C3-M1 to C3-M17; C3-M14 and C3-M16 caught by the selection unit test)
  were each reported; the unchanged tree (C3-M18) reported nothing new.

### D-05. Star-reckoning, the obligation, upkeep, renew, suspend and end (unit 2)

**What the player sees (D-S1).**
- **Star-reckoning:** while the pact is active, once a game day a pact, the holder at the perch (inside the pact's area)
  chooses any sky pattern they have personally observed (`AstralClaims.observed`, `:83-85`) and recalls it with their
  astrolabe's stored resonance, at the pattern's existing `recallCost`, under the same `recall:<pattern>`
  once-per-occurrence claim as a master's recall, for `Calendar.RECALL_TICKS` (2,400 ticks, `Calendar.java:18`). The
  holder needs the pact, not celestial mastery. Season and window rules are the existing recall's: a recalled attunement
  ignores its window and season (`Sky.java:298`). It is a chosen recall, so it does not wait for an empty sky, as the
  master's fallback does (`AstrolabeItem.java:107-111`).
- **Why it is useful:** the holder chooses the pattern instead of getting the last one observed; nothing waits for the
  19-day Echo Comet (`pattern/echo_comet.json`), a winter or a clear night; the effect lasts 2,400 ticks wherever the
  holder goes.
- **What it changes (critique 10):** `Calendar.occurrence` is plain day arithmetic (`Calendar.java:51-54`) and the
  shortest gap between claims is half a period (`:94-96`). The Harvest Moon has a period of 1 (Hero of the Village,
  recall 12) and the Winter Crown a period of 2 (Fire Resistance, recall 9). A holder who has observed them (a pattern is
  up, and so observable, only in its season: the Harvest Moon during the Halloween event's Harvest Moon, the Winter Crown
  in winter or in any season when seasons are switched off, `Sky.java:169-178, 202-210`) can recall them as often as the one-a-day quota and the claim allow: daily and every other day, for 2,400 ticks, at 12 and 9 resonance. A master can already do this for whichever
  pattern they observed last (`AstrolabeItem.java:112-131`); the pact opens it to non-masters and to any observed
  pattern. No resonance is minted: it comes from the astrolabe, filled by observatories once per occurrence.
- **The first obligation:** record a sky observation at an observatory while the pact is active (not suspended). The
  pact then records `Practiced(jugcraft:pact_service, jugcraft:stolas)` once, which masters Stolas's Regard and unlocks
  the workshop hand (D-03, D-08).
- **Upkeep (D-S3, Gentle):** one offering every 7 game days from the pact's escrow (`#jugcraft:stolas_offerings`: the
  three Styx flowers and `minecraft:amethyst_shard`, the non-Styx fallback); up to 4 periods prepaid; the compact pays
  the first. Looked at only under D-02's rule (the perch loaded; the holder online, alive, in its level, within 64
  blocks; never a stand-in). A missed offering shows a warning (a sign over Stolas, a message to the holder, the
  `upkeep_due` status in Jade and the roster), then 1 game day of grace, then suspension for upkeep: the service stops,
  nothing is taken, nothing is lost, nothing accrues. Paying resumes it. A pact never ends by itself.
- **Renew:** deposit offerings at the perch (a menu action: the server takes the chosen offering from the player's
  inventory into the escrow, up to the cap). **Suspend and resume:** the holder's choice, at the perch or (D-07) from the
  roster.
- **End (D-S6):** holder only, at the perch or from the roster. Stolas makes his departure gesture and leaves; the
  escrow is returned once into the living holder's inventory, any overflow dropped at the holder's feet (as
  `CourierLedger.recover` does, `:289-304`), never into a dead player's (a dead holder cannot end until respawned); the
  reason goes to the history.
- **Without its perch:** breaking or blowing up the perch leaves the pact "without its perch": Stolas leaves the world;
  the record and escrow stay; the holder may seat the pact at a new perch they own (a menu action there) or end it from
  the roster.

**Changes.**
- **One recall path:** `AstrolabeItem`'s recall (`:107-131`) is extracted into one shared method (pattern, astrolabe,
  holder, times) that both the master's fallback and Star-reckoning call: one claim key, one cost, no minting (03:155).
  The master's route keeps its own rule (nothing up; the last pattern observed).
- The perch's server ticker looks at upkeep once a second under D-02's rule; Stolas thinks once a second
  (`WorkerEntity.java:59`).
- `#jugcraft:stolas_offerings` (generator). Consumed offerings stay in equivalence (D-S19, critique 14): an amethyst
  shard is a reference item worth 32 (`alchemy/values.json`), the Styx flowers are renewable with bone meal
  (`StyxFlowerBlock.java:31-36`; bone meal worth 1/6) and valued at the generator's fallback of 1, "balance pending"
  (`alchemy/values.json`). An offering a week is a sink, not a
  conversion; the record states the balance unit.
- `practice/pact_service.json` (shipped in D-03) is recorded from the pact's obligation only. Not `worker_service`,
  which would change Binding Arts' "distinct 3" (`research/binding_arts.json:43-50`) and feed the `bound_service`
  commission (`concordance/commission/bound_service.json:5`).

**Tests (server):**

| Suite | New tests |
|---|---|
| `ConcordanceSkyGameTests` | `stolasRecallsAnObservedPatternOncePerOccurrence`; `aMastersRecallAndStolassShareOneClaim`; `aChangedClockGivesNoSecondRecall` (10:53); `theHarvestMoonRecallsOutsideItsEvent` (the balance consequence pinned: a new occurrence each day); `anUnobservedPatternIsRefused` |
| `ConcordanceWorkerGameTests` | `theObligationMastersTheEntryOnce`; `aSuspendedPactRecallsNothing`; `theQuotaAndTheAreaRefuseWithReasons`; on Stolas (D-02's tests of these names, extended to the pact, not second copies): `upkeepWarnsThenSuspendsAfterGraceAndResumes`, `aReturnAfterAbsenceGetsTheFullGrace` (the holder away 10 game days with an empty escrow: on return the warning and a full day of grace, then suspension only if still unpaid), `absentAndUnloadedTimeIsNeverOwed` (logged out; the perch unloaded; online but 100 blocks away; in another level), `aStandInNeverAnswersForUpkeep` (the absent-owner option on), `theConcordanceOffHoldsEveryClock`; `endingReturnsTheEscrowOnce` (into the inventory; the overflow at the holder's feet); `aDeadHolderIsNeverHandedTheEscrow`; `aBrokenPerchLeavesThePactWithoutItsPerch`; `aPactIsSeatedAgainAtANewPerch`; `aLostPresenceIsCalledBackAndEndsNothing` |
| `ConcordanceSharingGameTests` | `aCopiedPerchRecallsNothing`; `anotherPlayerCannotUseTheHoldersService` |
| `ConcordancePersistenceGameTests` | `thePactAndItsEscrowSurviveARestart`; `aRestartWhileSuspendedStaysSuspended`; `aChangeOfOwnerMovesNoPact` (the holder leaves a party; the perch's land changes hands: the pact stays the holder's, D-S23) |

Spending tests use `TestPlayers.survivor` (critique 25; the Facts above). The economy check
(`tools/concordance_economy.py`) shows no gaining cycle and states the offering's daily bound.

**Acceptance:** E2 (Stolas's upkeep suspends, with nothing owed for absence and the full grace on return); E3 (one claim
an occurrence across both routes; a copy recalls nothing); E4 (the escrow back once on ending; a broken perch keeps it;
restart and unload change nothing); E7 (the path is complete); the reward has a consumer (10:168).

**Saves:** the pact record's escrow and upkeep clock; the Bound Will's `hold`; the history.

**Review boundary:** completes the first end-to-end path. The balance change (recall for non-masters) is limited to
this ticket.

**As built (unit 2: commit `ee82c1dc7`; records: [stolas-pact.md](stolas-pact.md),
[arcane-concordance-celestial.md](arcane-concordance-celestial.md), [the economy](arcane-concordance-economy.md) and
[the journey](arcane-concordance-journey.md)).**
- **Built:**
  - **One recall path (D-S1):** `sky/AstrolabeItem.recall` claims `recall:<pattern>` once an occurrence on the player's
    own ledger, spends the pattern's recall cost from the paying astrolabe and attaches a 2,400-tick recalled
    attunement. `attune`'s master fallback (nothing up; the last pattern observed) now calls it, with the same messages
    and results as before; `mayRecall` asks a copy of the ledger, for the screen only.
  - `worker/Pacts` (pure): `Reckoning` (14 ids: `recalled`, `disabled`, `nothing`, `conflicting`, `not_holder`,
    `suspended`, `upkeep_held`, `outside_area`, `outside_hours`, `quota_spent`, `not_observed`, `no_astrolabe`,
    `too_little`, `recalled_already`) and `serves` (one reason, in order).
  - `spirits/StarReckoning` (new): `reckon` refuses with one reason in the order of D-P43 before anything is spent;
    the paying astrolabe is the first carried (hands, then slots) holding enough (`PactCompact.astrolabe`); it then
    calls `AstrolabeItem.recall` and counts the day on the record (`taskDone`). No empty sky is needed and Stolas need
    not be present. `observed`, called from `ObservatoryBlockEntity.observe` after its practice is recorded, records
    `practice jugcraft:pact_service` once a player while the pact is active (D-P44), which masters Stolas's Regard;
    `kept` and `choices` (at most 8 observed patterns).
  - `PactKeeping.page` gains the Star-reckoning row with today's remaining count, the obligation row and the observed
    patterns; `PactMenu`'s RECALL reckons the pattern the menu last offered, and each offered pattern says whether this
    occurrence can still be recalled.
  - Upkeep, suspension, re-seating and ending on Stolas's own pact use unit 1's rules and the D-04 keeping, unchanged.
  - Generators: `tools/concordance_pacts.py` (D-05's section: `RECKONING_WORK`, `RECKONING_CHOICES`, `RECKONING`, the
    messages, three pages of `binding/stolas_pact`, three more client shots); `tools/concordance_celestial.py` (the
    astrolabe's Recall page names the pact); `tools/concordance_economy.py` (Star-reckoning as a sink at the cheapest
    recall, and Stolas's daily row; D-P49); `tools/concordance_journey.py` (the route's fifth step: the perch, the pact
    and its obligation). The economy and journey tables pasted into their records.
  - `ConcordancePactClientGameTests` gains its service step (a recall and an offering; one open) and its ending step
    (the confirmation, Esc keeping the pact, yes ending it once): three shots.
- **Departures from this ticket and from the unit's specification** (proposals D-P43 to D-P49 in Decisions):
  - the first obligation counts once a player, not once a pact (D-P44): stricter than the ticket's wording;
  - `aChangeOfOwnerMovesNoPact` is extended in `ConcordanceAuthorityGameTests` (where unit 1 put it), not Persistence;
  - added beyond the ticket: `aStandInNeverAnswersForUpkeep` and `ConcordanceResourceGameTests`
    `theServiceRulesKeepTheirShape`;
  - the Winter Crown's every-other-day recall is pinned inside `theHarvestMoonRecallsOutsideItsEvent`, and the hours
    refusal is tested on test terms (13,000 to 23,000) because Stolas's are any hour;
  - unloading is modelled as a gap between looks, not a chunk-load listener;
  - seating also covers a perch broken while switched off (its place loaded and keeping nothing);
  - review correction 3: `aSuspendedPactRecallsNothing` reads the due date after the resumes (the 20 ticks held are not
    owed) and times the warning and the suspension from it;
  - review correction 4: `StarReckoning`'s Javadoc no longer names `RECALL_TICKS`;
  - review correction 5: `aMastersRecallAndStolassShareOneClaim` fills the astrolabe before each refused recall, so only
    the shared claim can refuse it;
  - review correction 7: `aStandInNeverAnswersForUpkeep` offers a real amethyst shard through the stand-in;
  - review correction 8: the service part's tests (unit 1's four upkeep tests on Stolas, `aCopiedPerchRecallsNothing`,
    `anotherPlayerCannotUseTheHoldersService`, `thePactAndItsEscrowSurviveARestart`,
    `aRestartWhileSuspendedStaysSuspended`, `theServiceRulesKeepTheirShape`) were inlined from its design, their
    locals renamed where they met unit 1's (`stolasBack`, `stolasTr`, ...);
  - `aLostPresenceIsCalledBackAndEndsNothing` compares Stolas's identity with `assertTrue` rather than
    `assertValueEqual`.
- **Tests (passed in CI runs 40090825848 and 40094972598):** `ConcordanceSkyGameTests` +5 (`stolasRecallsAnObservedPatternOncePerOccurrence`,
  `aMastersRecallAndStolassShareOneClaim`, `aChangedClockGivesNoSecondRecall`, `theHarvestMoonRecallsOutsideItsEvent`,
  `anUnobservedPatternIsRefused`), `ConcordanceWorkerGameTests` +9 (`theObligationMastersTheEntryOnce`,
  `aSuspendedPactRecallsNothing`, `theQuotaAndTheAreaRefuseWithReasons`, `aStandInNeverAnswersForUpkeep`,
  `endingReturnsTheEscrowOnce`, `aDeadHolderIsNeverHandedTheEscrow`, `aBrokenPerchLeavesThePactWithoutItsPerch`,
  `aPactIsSeatedAgainAtANewPerch`, `aLostPresenceIsCalledBackAndEndsNothing`), `ConcordanceSharingGameTests` +2
  (`aCopiedPerchRecallsNothing`, `anotherPlayerCannotUseTheHoldersService`), `ConcordancePersistenceGameTests` +2
  (`thePactAndItsEscrowSurviveARestart`, `aRestartWhileSuspendedStaysSuspended`),
  `ConcordanceResourceGameTests.theServiceRulesKeepTheirShape`: 19. Extended: unit 1's
  `upkeepWarnsThenSuspendsAfterGraceAndResumes`, `aReturnAfterAbsenceGetsTheFullGrace`,
  `absentAndUnloadedTimeIsNeverOwed` and `theConcordanceOffHoldsEveryClock`, each also on Stolas's own pact (E2), and
  `aChangeOfOwnerMovesNoPact` (the perch's new owner is not served). With D-03's and D-04's, and the unit review's
  `shortAbsencesNeverRenewTheGrace`, the unit adds 46 server game tests (Styx 7 to 10, Sharing 5 to 9, Progression 8
  to 9, Sky 6 to 13, Journal 5 to 7, Worker 22 to 44, Persistence 15 to 19, Authority 8 to 9, Resource 5 to 7) and one
  client test class.
- **Audits:** `check_pacts`' D-05 part (`Reckoning` and `serves` against the generator; `reckon`'s order; claims,
  spending and attunements only through `AstrolabeItem.recall`, which `attune` calls and `mayRecall` never claims
  through; the obligation from `observe`, after its practice, once, never `worker_service`; the page and RECALL; the
  D-05 test names, unit 1's upkeep tests on Stolas and the owner change); `AUTHORITY_ROUTES` (Star-reckoning);
  `check_economy` (Stolas's upkeep, quota, offerings and recall costs against the data; a recalled attunement turns
  into nothing).
- **Audit mutations:** the specification's 19 (D5-M1 to D5-M19) were each reported; the unchanged tree (D5-M20) reported
  nothing. With D-03's, D-04's and the client commit's, all 80 of unit 2's mutations were reported. For the records'
  commit, four more, run in place and restored: a broken link in `stolas-pact.md`, the record removed, the CHANGELOG's
  link to it broken (each reported by `scripts/check_repository.py`) and the economy note moved inside the economy
  table's markers (reported by `check_economy`); the unchanged tree reported nothing.
- **Commands** (`python3 -B tools/check_mod_data.py` passed before each code commit; all of these were run again, and
  passed, on the final tree for the records' commit): `python3 tools/owner_art.py --check`,
  `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
  `python3 tools/concordance_journey.py --check` ("every route holds"), `python3 tools/check_icon_maps.py`,
  `node --test tools/world-designer/model.test.cjs`, `python3 -m unittest discover -s scripts/tests` and
  `git diff --check`. Generators were run one at a time (`python3 -B tools/generate_material_data.py`;
  `python3 tools/owner_art.py` in the client commit; the values step in D-04's). For these notes: every Java file the
  unit adds or changes parse-checked (38, no syntax error); the pure classes (`worker/Pacts`, `Agreement`, `Status`,
  `resource/BoundWill`, `BoundWillLedger`, `AstralLedger`, `celestial/Calendar`, `Pattern`, `logistics/Place`)
  compiled with `javac` against the jspecify jar, and three pure harnesses replaying the reckoning, the service rules
  and the upkeep traces passed (31, 8 and 76 checks); `python3 tools/select_client_tests.py --base 97e1facd4` chooses
  58 of 240 client classes, `ConcordancePactClientGameTests` among them.
- **Not run:** `./gradlew build` (with or without the optional integrations), every server game test and the client
  test (CI is their first compile and run), two clients, a human playthrough, a real chunk unload, a server restart
  outside the tests. Since run in CI: run 40090825848 failed two of the unit's server tests in both server jobs, each a
  defect of the test (fixed in `8a3425f9e`); run 40094972598 passed every job.
- **Not confirmed until CI:** `ItemStorage.SIDED.find` returning nothing for the perch; a power-3 explosion
  (`Level.ExplosionInteraction.TNT`) breaking a perch of resistance 3.0 in `thePerchHoldsNothing`;
  `MobTransportCrate.capture` of a cow in a game test; the route from `Mob.interact` to Stolas's `mobInteract`;
  `ResearchNotesItem.write` capping at understood; Fabric's client play receivers running on the client thread;
  `TestInput` key timing (the client test waits on sequences instead); that the local GeckoLib source equals the pinned
  5.5.7 (`withScale`, `thenPlayAndHold`, box UV, `DefaultedGeoModel`'s paths) and that Flashjack's Molang loops play
  under `thenLoop`; `ComponentSerialization.TRUSTED_STREAM_CODEC` with number arguments; `check_model_uvs` on the
  perch's model in game; the Modonomicon drawing entries at (0,4) and (2,4); server-side `Component.getString`
  translation in the D-03 tests; vanilla `ConfirmScreen`'s Esc and Tab order; that a Fabric `FakePlayer`'s messages are
  harmless; `TownProtection` near spawn in a fresh client-test world (if Accept is refused there as not permitted, the
  test's stand moves away from spawn); that `StolasEntity.remove(KILLED)` strikes the roster at once.

**Unit 2's unit review (the commit "Chunk D unit 2: fixes from the unit review"; run in CI 40090825848, then passing
in runs 40094972598 and 40133540741).** Sixteen findings were confirmed and fixed:
- **Upkeep (D-P50, amending D-S3; a checkpoint question):** a holder warned before an absence is warned again on return
  with the grace they had left, its end moved on by the time away, instead of a whole new day (`Pacts.due`, unit 1's
  code: the unit 1 merge must carry it). Time away is still never charged, and the first warning after an absence
  still gives the whole grace, so `aReturnAfterAbsenceGetsTheFullGrace` holds exactly. Before it, a warned holder who
  left for over a minute (beyond 64 blocks, or logged out) at least every 20 minutes was never suspended, and
  Star-reckoning kept serving. `shortAbsencesNeverRenewTheGrace` (new) shows a holder away 61 seconds after every
  1,000 ticks present suspended after one game day present. A second route stays open and is left to the owner:
  suspend, recall and resume once a day, since each resume moves the due date on by the time held, so the 7-day
  period counts only a few ticks of active time a day.
- **A perch a machine sets (D-P24 corrected):** `AgreementHolder` takes an owner-required flag (the perch's; the
  anchor keeps its ownerless legacy rule), so a perch that records no owner seals and seats for nobody (`maySeal`).
  `twoNegotiationsAtOnePerchSealOnce` is rewritten on a perch its first player owns: the other player is refused
  `not_permitted` and spends nothing (`TAKEN` stays shown by `acceptingSealsOnceAndSpendsOnce`), and a perch no player
  placed brings no Stolas and seals for nobody. Machines are not stopped from setting a perch (`Blueprint.FORBIDDEN` is
  unchanged); such a perch is inert until broken and placed by hand.
- **A choice's id** (an offering's or a pattern's, both named by data) is encoded up to 32,767 characters
  (`PactStatePayload.MAX_ID`, in `STATE_LIMITS`), so a long id can no longer fail to encode and disconnect its holder;
  `thePactScreensMessagesAreBounded` sends one of 209 characters.
- **The first obligation** is no longer kept by a pact stranded while the Concordance was off (`StarReckoning.observed`
  asks `AgreementHolder.stranded`); `theObligationMastersTheEntryOnce` breaks the perch while switched off, observes,
  seats the pact at a new perch and observes again.
- **The parser** refuses `"party"` beneficiaries for a work that serves its holder alone (`PactParser.HOLDER_ONLY`,
  Star-reckoning; `check_pacts` pins it); `thePactDefinitionParsesStrictly` gains the case.
- **A visitor** with no conversation waiting on him leaves whether or not the Concordance is on, so one saved with his
  chunk and loaded while it is off no longer stays; `aVisitingStolasLeavesWithoutARecord` loads one while off.
- **Decline:** the menu closes in the same call, so its answer is no longer sent as a state the client drops: it is
  shown on the action bar (which the narrator reads) and the state only returned.
- **Audits:** the read-only audit of Stolas's Regard scans the whole file (the helpers `hint` and `report` call
  included); the perch's holder is pinned with its owner flag and `maySeal`'s rule; the obligation's audit asks for the
  stranded check.
- **Records:** the balance statement (the once-a-day quota is the only hard limit; 2.09 and 3.59 resonance a day are
  one keeper's averages), the persistence record, the slab mutation's count (12), the visitor's save, `WHAT_EXISTS`,
  the stable-identifier registry, the D-04 departures' decision range, the superseded codex entry name and the
  `PactKeeping` Javadoc.
- **Audit mutations** (run in place on the focused Concordance audit and restored; the tree's digest equal before and
  after): RV-M1 to RV-M15 were each reported: `Pacts.due` renewing the whole grace again, the perch's holder without its
  owner flag, `maySeal` letting anyone seal at an ownerless block, `MAX_ID` at 64, a choice's id encoded with `MAX_KEY`,
  the obligation kept by a stranded pact (removed, or asked after the practice is recorded), the parser's check removed
  or `HOLDER_ONLY` emptied, the generator's Stolas offering his party (2 reports), a helper of the hint that writes (which
  the audit before this fix did not report), the new test removed, a visitor waiting while switched off, a decline's
  state sent to a closed menu, and a decline said nowhere. The unchanged tree (RV-M16) reported nothing.
- **Pure harness** (javac against the jspecify jar; scratch, not committed): `Pacts.due` replayed for
  `aReturnAfterAbsenceGetsTheFullGrace`'s three absences (each grace end exact) and for 61-second absences after every
  1,000 ticks present: suspended at 24,000 ticks present after 23 returns; unit 1's `Pacts.due` never suspended them in
  100 cycles. The parser read the shipped offer, and refused `"party"` and `"everyone"` with their reasons.
- **Commands** (all passed on the final tree): `python3 tools/owner_art.py --check`, `python3 -B tools/check_mod_data.py`,
  `python3 scripts/check_repository.py`, `python3 tools/concordance_journey.py --check`, `python3 tools/check_icon_maps.py`,
  `node --test tools/world-designer/model.test.cjs`, `python3 -m unittest discover -s scripts/tests` and
  `git diff --check`; every changed Java file parse-checked (12, no syntax error). No generator was needed: the
  generator's new numbers (`STATE_LIMITS["MAX_ID"]`, `HOLDER_ONLY`) are read only by the audit.
- **Not run:** `./gradlew build`, the server game tests (the new and changed ones included) and the client test: CI is
  their first compile and run. Since run in CI: run 40090825848, then passing in run 40094972598.
- **Rejected by the unit review, unchanged:** the perch's level against the player's, a test listener in a static
  initializer, the decision numbering, offer data against the parser's bounds, the economy figure, the clip mapping,
  the shot names, list sizes against the screen's bounds, figures in player text, the perch's icon against its model,
  save effects never read back, the celestial status line, and two wordings ("their own astrolabe", "ended from the
  roster").

**Merged into the pact hall after unit 4** (the commit "Merge Chunk D unit 2 (Stolas and the pact perch) into the pact
hall"; run in CI 40090825848, then passing in run 40094972598):
- `Pacts.due` carries D-P50 beside unit 1's review fixes 1 and 7 (a resumed pact's last look moves on by the time held;
  a look in the tick of the last one goes on). `aReturnAfterAbsenceGetsTheFullGrace` keeps unit 1's suspend-and-resume
  step and the Stolas step, both exact under D-P50: each holder was warned as they left, so the grace left is all of it.
- Stolas, like the shade, joins his holder's roster only by his seal, his call again or his seating
  (`joinsRosterOnStatus()` false; unit 1's fix 2), so one replaced, or loaded while the Concordance is off, takes no
  roster place; `check_pacts` holds it. (The hand still inherited true until unit 3 gave it the same rule: D-07's As
  built.)
- The hand's codex page links `binding/stolas_regard`, and `check_hand` now checks the Regard's mastered state.
- 26.3's lessons from CI, checked in unit 2's tests: none names `Items.WHITE_WOOL`, and none compares an item use's raw
  `ItemStack.useOn` answer with the SUCCESS constant (the perch's placing helper asks `consumesAction()`).
- Both units' audits run together and pass; five mutations, each reported and restored: Stolas joining on status, the
  Regard without a mastered state, the same-tick look stopped, D-P50 reverted, resume's last look not moved.

### D-06. Stolas's character: body, gestures, sounds, subtitles, Jade (unit 3)

**What the player sees.**
- Stolas, a raven, idles on the perch, listens while the screen is open, inspects (an offering, the astrolabe), assents
  on acceptance, refuses with a gesture, a subtitle line and an icon, arrives and departs. Each important cue arrives
  through two channels (08:145).
- Jade shows his service, state, next offering and grace; a new provider does the same for the perch and the Spirit
  Anchor (G11). Neither shows the escrow's contents or the holder's name.

**Changes.**
- **Body (D-S5):** a raven-form runtime copy adapted from the owner library's Flashjack, a GeckoLib bird rig
  (`art/owner-library/originals/Magic/assets/ars_jymbaumental/`):

| File | SHA-256 | What it is |
|---|---|---|
| `geo/flashjack.geo.json` | `d23b5372d27cb76ea5974b2a1e78f3cba7b5a8f96cd14bef54d1e14249adc837` | format 1.12.0, a 64×64 sheet, 20 box-UV cubes, bones `master`, `head`, `body`, `upperBody`, `wing_left`, `wing_right`, `lowerBody`, `tail`, `legs`, `feets` |
| `animations/flashjack.animation.json` | `c4015c9351dd54c3fec3589a544db4110809dc11ce60e040ad1cc7870c9491f8` | clips `idle.flapping` (4.75 s), `attack` (1.5 s), `idle.air` (3 s, looped), `idle.ground` (2 s), `idle.sitting` (1.25 s) |
| `textures/entity/flashjack.png` | `567b44a51e083f2a9ec81db92a8743435985b630f8aca98ef62baed2393ea514` | 64×64 skin (also `flapjack.png`, `bluejay.png`) |
| `geo/flashjack.bbmodel` | `7f5f6189c3bd5976bbae49d4a96ed96810166812c1991ea83fda9f4b950e1b89` | Blockbench project |

  The runtime copy (`geckolib/models/entity/stolas.geo.json`, `geckolib/animations/entity/stolas.animation.json`,
  `textures/entity/stolas.png`) is scaled up and recoloured to black plumage by a generator (the owner-art route,
  `tools/owner_art.py`, for what is copied; the new tool for what is adapted), with the missing clips authored on its
  bones: arrive, listen, inspect, assent, refuse, depart, and idle and waiting loops from `idle.sitting` and
  `idle.ground`. The originals stay intact. Provenance in `stolas-pact.md`: each source path and hash, every change,
  the archive's label and licence metadata (Ars Elemental, author Alexthw, GNU LGPL v3.0:
  `art/owner-library/ports/jug-nouveau/ars_jymbaumental-1.21.1-0.7.10.4/META-INF/neoforge.mods.toml:7, 15, 25`) and the
  owner's declaration (`art/owner-library/PROVENANCE.md:3-7`). `CLAUDE.md` authorises direct reuse and adaptation
  without a per-asset request (critique 20); the appearance stays a checkpoint question. Not the crow, the courier bird
  or the Ars familiars, which are registered identities (`beast_crow`, `mount_courierbird`, `nouveau/NouveauFamiliars.java:27`,
  `nouveau/NouveauHelpers.java:22-24`).
- **Animation:** a base controller maps the pact's state and `Status` to loops (idle, listen, waiting, suspended);
  `triggerableAnim` one-shots (arrive, inspect, assent, refuse, depart) are fired by the server's action, so a late
  joiner sees the state, not a replay (`lair/vesperine/GraveThrallEntity.java:122, 189`). SmartBrainLib
  handles look-at and perch-bound movement only; the contract stays in Jugcraft code (03:159).
- **Sounds (D-S5):** new events `jugcraft:stolas.arrive`, `.listen`, `.inspect`, `.assent`, `.refuse` and `.depart`,
  each with a subtitle, routed through the generator: `tools/concordance.py`'s `SOUND_EVENTS` and `SOUND_FILES`
  (`:1529-1561`), which `tools/generate_material_data.py:1096-1097` writes into `sounds.json` (critique 23). Each is
  bound to `.ogg` files already in runtime: wing beats (`sounds/menagerie/beast/flutter_flap_0-2.ogg`), a raven's call
  (`crow_idle_0-3.ogg`, shared with `beast_crow`), page turns (`sounds/menagerie/cavern/item/cave_book/book_*.ogg`), a
  spirit's sigh (`sounds/menagerie/herbal/ghost_weep.ogg`) and a chime (`sounds/household/block/doorbell/chime.ogg`).
  Being in `SOUND_FILES`, they get no drawn cue (`tools/concordance_sounds.py:563`). No new audio and no voice.
- **Jade:** a pact branch in `WorkerDataProvider` and the client's `WorkerTooltip`; a new provider for the perch and the
  Spirit Anchor, behind the optional `jugcraft:jade_client` entrypoint (`JugcraftJadePlugin.java:50-61`).
- **Audits:** `check_geckolib` pins `triggerableAnim` clip names as it pins `thenLoop`'s (`tools/check_mod_data.py:17265-17267`);
  `check_pacts` pins the sound events, their subtitles and their files.

**Tests.**
- Server (`ConcordanceWorkerGameTests`): `aRemovedStolasNeverEndsThePact`; `onePresencePerPact`;
  `killingStolasIsNeverTheFastestRoute` (bypass damage: no end, no refund, a re-call at no cost; 02:151).
- Client (`ConcordancePactClientGameTests`): a frozen-tick pose for each clip (`CinderTyrantClientGameTests.java:90-94`);
  the refuse and assent subtitles visible; a Jade shot; the build without optional integrations.

**Acceptance:** E4 (a presence's death returns nothing twice and ends nothing); the accessibility rule of 03:159; the
presentation bundle (08:169-181).

**Saves:** the entity's keys under `WorkerEntity`'s stamp.

**Review boundary:** presentation over D-04 and D-05's state. The look is for the owner's checkpoint.

**As built (unit 3: commits `9805a9a7c`, the body, and `773b85aba`, the presence; records:
[stolas-pact.md](stolas-pact.md#stolass-character-d-06) in the records' commit "Concordance Chunk D: unit 3's records").**
- **Built:**
  - **The body (D-P90 to D-P93):** the geometry stays the owner's file byte for byte (`OWNER_FILES`); the skin, an eye
    glowmask and the animation file are adapted from Flashjack's own files by the new `tools/stolas_body.py`, which
    `tools/owner_art.py` writes and checks (`concordance_pacts.ADAPTED`) and marks in `--provenance`: each texel
    recoloured in place by part and hue (plumage black, markings blue-black, beak, legs and toes dark horn, eyes amber,
    crest and breast glyph old gold; shading and alpha kept, nothing drawn); the glowmask the four eye texels (9,10),
    (9,11), (14,10), (14,11), drawn by `AutoGlowingGeoLayer`; the animation file Flashjack's five clips unchanged plus
    nine `animation.stolas.*` clips (56,789 bytes, SHA-256 `59ae566b17451085…`). The owner library is unchanged and
    nothing in it is executed; the provenance table is in `stolas-pact.md`. `WorkerClient` draws him at 1.4× with a
    wider culling box.
  - **The controllers (D-P94):** `StolasEntity.Loop` (idle, listen, waiting, suspended, and the owner's `idle.flapping`
    as astir) on the worker's own controller, for what the server synced; `StolasEntity.Gesture` (arrive 40 ticks,
    inspect 50, assent 40, refuse 40, depart 40, held) as triggerable clips on a second controller, `gesture`, fired by
    `gesture()` on the server.
  - **Listening (D-P95):** a synced flag set at once by `listen()` when a conversation opens at his perch
    (`PactMenu.begin` → `PactPerchBlockEntity.heard`) and recomputed every think.
  - **Arrival (D-P96)** in `PactPerchBlockEntity.summon` (an invitation, a presence called back, a pact seated), right
    after the entity is added.
  - **Departure (D-P97):** `departing()` and `gone()`; leaving plays DEPART and he is discarded 40 ticks of game time
    later (by his own tick or, since the unit review, the server's sweep where his chunk does not tick him), struck off
    at once, never saved meanwhile (`shouldBeSaved`), refused as a visitor by `unbound()`; `stolas(level)` gives the
    presence seated at this perch (`getBlockPos().equals(presence.perch())`, the unit's critique 6), else the visitor,
    never one leaving.
  - **Reactions (D-P99):** `StolasEntity.reaction` maps each answered pact-screen action to its gesture;
    `PactMenu.receive` calls `PactPerchBlockEntity.react` once an action is answered.
  - **Cues (D-P100, D-P101):** `SOUND_IDS`, `CUE_TICKS` = 10, `registerSounds()` (one line in `Workers`), `cueing()`
    inside `gesture()` and `listen()`, `cues()` for tests; six `jugcraft:stolas.*` events in `tools/concordance_pacts.py`,
    merged by `tools/concordance.py` into `SOUND_EVENTS` and `SOUND_FILES` and written to `sounds.json` by
    `tools/generate_material_data.py`, bound to runtime `.ogg` files (no new audio), mono since the unit review.
  - **Two channels (D-06's rule):** each gesture is seen and heard, with its subtitle; listening is his loop plus
    "Stolas listens", whose sound and subtitle are skipped within 10 ticks of his arrival (the arrival's carry the
    moment); a suspension is his loop plus the screen's mark and narration; each answer is a drawn tick or cross plus
    the narrator.
  - **Jade (D-P102; G11 closed):** `AgreementHolder.peek` (verify's answer without its writes); new `spirits/PactReading`
    (read only; no player, no escrow); behind Jade's entrypoints only, new `compat/jade/PactDataProvider` (the perch and
    the Spirit Anchor, uids `jugcraft:pact_perch` and `jugcraft:spirit_anchor`), a Stolas branch in `WorkerDataProvider`,
    and the client's `AgreementTooltip` and `pactLines` in `JugcraftJadeClient`. A perch keeping nothing reads
    "visiting" only while its own visitor waits there (critique 7).
  - **Marks and subtitles (D-P103, D-P104):** `PactScreen.Verdict` (a drawn tick or cross, narrated "Done:" or
    "Refused:"), `isInGameUi()` true and the deferred subtitles drawn over the screen; new `KeepFirstConfirmScreen` for
    End, the keyboard starting on Keep it (D-P117, refining unit 2's D-P40).
  - **Reconciliation** (the unit's specification, R5 to R13): the body's structure with the presence's `Loop`, cues
    inside `gesture()`; one `stolas(level)` lookup used by `heard` and `react`; one departure test; one client helper;
    selection weights 150 to 200 to 230; both screens in-game with subtitles; one answer mark; one safe confirmation;
    every `isRemoved()` that meant "he has left" now `gone()`.
  - **Audits:** `check_geckolib` also pins clips played once; `check_pact_screen`'s body block moved into the new
    `check_stolas_body`, and it pins `KeepFirstConfirmScreen`; `check_pacts`' D-06 block (`D06_TESTS`, the cue table,
    the reactions, the Jade readings, needles for critiques 6 and 7); `check_rime_strata_assets` counts Stolas's sound
    files.
- **Departures from this ticket and from the unit's specification:**
  - "additive" gestures became a second controller (D-P94): GeckoLib's additive path subtracts the base rotations;
  - the ticket's "a frozen-tick pose for each clip": his astir loop (`idle.flapping`, while an offering is due) is not
    posed, since making an offering due in the client test would change the upkeep its later steps read (the unit's
    critique 11); recorded in `tools/concordance_pacts.py`, the test and `stolas-pact.md`;
  - the ticket's "the refuse and assent subtitles visible": CI has no sound device, so they are checked as defined and
    fired, and seen only where one exists (D-P104);
  - Jade's client words a copy with the diagnostic's own key (`Diagnostic.PREFIX` with `ComposeText.show`), not through
    `Diagnostic.of`, which `check_signs` (Chunk C, C-01) forbids in `JugcraftJadeClient`; the same words;
  - `tooltip.jugcraft.concordance.jade.pact.waiting` reads "Its presence: %s", not "Waiting: %s", since the status words
    already begin "waiting: ...";
  - added beyond the specification: needles for critiques 6 and 7 and for `KeepFirstConfirmScreen`, the client test's
    subtitle and HUD switches put back in `finally` blocks, and the sharing test's `aCopiedPerchIsInert` reading
    `gone()` (critique 12).
  None of the ticket's behaviour was dropped.
- **Tests (first run in CI run 40121823509 on `040d73ece`, where they passed, after run 40120355940 on `554d71ab1` failed
  to compile at the roster client test; passed again in run 40133540741):** `ConcordanceWorkerGameTests` +7
  (`D06_TESTS`): `aDepartingStolasIsNeverAVisitorAgain`
  (commit 1; its cue checks in commit 2), `stolasGesturesAndSoundsFollowTheirTable`,
  `stolasAnswersEachActionWithItsGesture`, `aRemovedStolasNeverEndsThePact`, `onePresencePerPact`,
  `killingStolasIsNeverTheFastestRoute` and `theJadeReadingsNameNoPlayerAndChangeNothing` (with `copy.stolas(level) ==
  null` and no visit read after a release); the unit review added `aDepartureEndsWithoutHisTicks` (`D06_TESTS`, now
  8). Changed to `gone()`: `thePerchHoldsNothing` (Persistence),
  `aCopiedPerchIsInert` (Sharing), `aVisitingStolasLeavesWithoutARecord`, `endingReturnsTheEscrowOnce`,
  `aBrokenPerchLeavesThePactWithoutItsPerch` and the helpers `stolas` and `stolases` (Worker).
  `ConcordancePactClientGameTests`: his four loops of his own and five gestures frozen in their telling poses (nine
  shots), his real arrival and departure, his refusal and assent with their subtitles, each answer's mark awaited
  before it is checked (critique 4), Keep it holding the keyboard, Jade's readings and, with Jade loaded, two Jade
  shots: 23 shots with Jade, 21 without. Selection: weight 150 to 230; `test_client_test_selection.py` checks that the
  glowmask selects it.
- **Audit mutations:** the body's 22 (B1 to B22) and the presence's 29 (P1 to P29: the specification's 25 and four for
  the critiques and the Jade rule) were each reported by the audits; each commit's unchanged tree reported nothing.
- **Commands** (each run and passed before its commit): `python3 tools/owner_art.py --check`,
  `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`, `python3 tools/concordance_journey.py
  --check`, `python3 tools/check_icon_maps.py`, `node --test tools/world-designer/model.test.cjs`,
  `python3 -m unittest discover -s scripts/tests` (197 tests) and `git diff --check`; every changed Java file
  parse-checked. Generators run one at a time: `python3 tools/owner_art.py` (commit 1 wrote the three adapted files),
  then `python3 -B tools/generate_material_data.py` (lang and `sounds.json`).
- **Since run in CI:** run 40120355940 on `554d71ab1` (unit 3 merged) failed to compile at the roster client test (two
  errors; fixed in `040d73ece`); run 40121823509 on `040d73ece` built it and ran the server game tests with and without
  the optional integrations and every client class, the pact client test among them: all passed (`optional
  integrations absent` after one re-run: `TurkeyGameTests.hensLayEggs`, from #112 and untouched by Chunk D, failed there
  once and passed in that run's `mod` job). Run 40133540741 passed them again and ran the
  pact client test without the optional integrations too. Still not run: a person playing, two people.
- **Not confirmed until CI:** that the pinned GeckoLib's trigger packets reach `TestPlayers`' connections (the server
  tests assert the server's state and `cues()`, not a client's clip); `level.playSound` for an entity not in a level
  (believed a no-op send); that `tick freeze` holds a triggered clip exactly; Jade's overlays in client screenshots;
  generic inference in `PactReading` and the Jade providers; clip timing under CI load (the log prints each frozen
  time).

**Findings for other units:**
- Unit 2's tests that asserted `isRemoved()` for Stolas leaving were changed to `gone()` in commit `9805a9a7c` (above);
  `aLostPresenceIsCalledBackAndEndsNothing`'s `first.isRemoved()` stays, since he is removed with `KILLED` there.
- Chunk A's `ComposerScreen` draws subtitles under its own fill, as `PactScreen` did before D-P103; left for unit 5.
  Done in unit 5 (D-P122).

### D-07. The roster screen, with cancel and recovery (unit 3)

**What the player sees (D-S11).**
- A native roster screen, opened by a key (listed in the key help, as every key in `PartyClient.CATEGORY` is: `EncyclopediaKeys.java:26-30`), lists every
  familiar, spirit, construct and pact the player keeps, and every pact or agreement record without its place.
- Each row: a portrait (the pattern of `src/client/java/local/peepo/client/TransportCratePreview.java`; whether it draws a GeckoLib entity is
  undetermined), kind, service, location and whether it is loaded, current task, next obligation and waiting reason
  (08:65), in words and an icon, never colour alone. Nouveau helper kinds get their lang (G13).
- **Actions:** suspend, resume and end, through the shared holder's authority on the Bound Will (so they work while the
  place is unloaded); cancel and recover a courier request through `CourierLedger.cancel/recover` (`:266, 289-304`), showing
  blocked or unloaded cargo instead of moving it (08:69); end a record without its place. A confirmation (vanilla
  `ConfirmScreen`, `mc263src` `net/minecraft/client/gui/screens/ConfirmScreen.java`) comes before ending.
- The journal's "pacts" section (D-03) lists the pacts and their next obligations.

**Changes.**
- A state payload of at most 64 rows (critique 17), built from `WorkerRoster` and the reader's Bound Wills, so a pact
  without its perch and an orphaned record appear even when the roster's 32 are full. D-01's `ROSTER_FULL` refuses a
  seal rather than drop the roster note.
- A request payload `(sequence, action, rowId, revision)`: `rowId` is a record or request id the server listed, checked
  again against the reader; no player ids. A `RateGate` shared with the commands; the holder, the authority and the
  record checked on every action; no range needed to read.
- **No encyclopedia page:** the encyclopedia is full (11 of 11) and audited read-only (`EncyclopediaPayload.java:27`;
  `tools/check_mod_data.py:19818`), so it can host no actions. A recorded deviation from 08:16.

**Tests.**
- Server: `theRosterEndsOnlyWhatTheReaderHolds`; `aRequestNamingAnotherPlayersRecordIsRefused`; `theRosterListsAPactWithoutItsPerch`;
  `theRosterStopsAt64Rows`; `recoveryNeverTakesCargoOutOfAnUnloadedContainer` (`ConcordanceLogisticsGameTests`); the
  journal section shown only after research (`ConcordanceJournalGameTests`).
- Client: the roster at two scales and in a small window; an end by keyboard alone with its confirmation.

**Acceptance:** E1 (no action on another's record); E4 (cancel and recover once); 09:162's cancel and recovery path.

**Saves:** none.

**Review boundary:** an interface over authoritative state; the screen owns no second inventory.

**As built (unit 3: commits `a3ee00822`, the server side and the journal's pacts, and `66b741868`, the screen and its
client test; records: [stolas-pact.md](stolas-pact.md#the-roster-d-07) and
[the workers record](arcane-concordance-workers.md#the-roster-screen-concordance-chunk-d-d-07) in the records' commit).**
- **Built:**
  - **The request (R1):** the contract's `RosterRequestPayload` (`jugcraft:concordance_roster_request`): a sequence, an
    action (`open`, `suspend`, `resume`, `end`, `cancel`, `recover`, by index), a row id (a Bound Will's, a worker's, or
    `UUID(0, request id)`) and that row's revision; nothing else (no player id, text or total). `RosterStatePayload`
    (`jugcraft:concordance_roster_state`): at most 64 `Row`s of 14 fields (id, portrait, kind, name, service, place,
    loaded, task, next, waiting, an int icon from 12 `ICONS`, revision, actions, the end's confirmation words), whether
    the Concordance is on, how many more, and the answer; the row's type stays on the server (D-P105, D-P106).
  - **`spirits/Roster`** (registered by `JugcraftConcordance`): lists the reader's Bound Wills (in the order sealed, a
    pact without its place or a record not yet claimed included, its presence in the same row), then their courier
    requests (oldest first), then the other workers on their roster; drops a stand-in's request, one out of bounds or
    one not newer than the last; then asks the gate its action shares (D-P107), the listing (`not_listed`), the row's
    current revision (`changed`, with the roster again) and its offer (`not_offered`, or `disabled` while switched off,
    D-P110); acts only through `AgreementHolder.toggle` and `end` and `CourierLedger.cancel` and `recover`, as the
    reader alone, alive and in person (D-P108, D-P109, D-P114). Opening has its own gate, served by the server tick
    once allowed (D-P118). Sessions are memory only, dropped on leaving and at server stop. Revisions are per row
    (`Objects.hash` of a record's place, hold, presence, upkeep dates and escrow count; of a request's state, counts,
    courier and source).
  - **The journal (D-P111):** `StolasRegard.report` calls `Roster.report`: every pact and agreement held (its name, where
    it is kept or "Without its place", its next obligation, why it waits as the exact figure) and the last eight
    departures, newest first, behind D-03's Binding Arts gate.
  - **G13 closed (D-P113):** the Ars Nouveau helpers' kinds have their words (`HELPER_KINDS`, checked against
    `NouveauHelpers`' species), for the `workers` command too.
  - **The workshop hand's roster rule** (D-08's As built, last item): `WorkshopHandEntity.joinsRosterOnStatus()` is false,
    as the shade's and Stolas's, so a replaced hand loading while switched off takes no roster place; `check_pacts`
    pins it and `aReplacedHandLoadingWhileSwitchedOffStaysOffTheRoster` shows it.
  - **The client:** new `client/RosterClient` (the key "Concordance Roster", `key.jugcraft.concordance_roster`, not bound
    at first and listed on the encyclopedia's Keys page, opening from the game only, D-P112; the last roster sent;
    requests of four fields for a listed row; one action waiting at a time, at most 100 client ticks; a refresh every
    40 client ticks while open; answers narrated; all forgotten on leaving a world) and `client/RosterScreen`
    (`ScreenKit.Fitted`, in-game UI with subtitles; rows as focusable narrated cards with their actions under them;
    `PactScreen.Verdict` for answers; `KeepFirstConfirmScreen` for End with the server's own words, D-P117; a drawn mark
    for each icon and for a loaded or unloaded place, D-P119; eight rows a page, Page Up and Page Down, D-P116;
    portraits as visual-only creatures, at most 16, never added to a level or ticked, else the item or the Spirit
    Anchor's icon, D-P115). `ConcordanceClient` registers it.
  - **Generators:** new `tools/concordance_roster.py` (limits, actions, types, icons, gates, answers, service and kind
    words, portraits, the helpers' kinds; the screen's words, `ROWS_PER_PAGE`, `REFRESH_TICKS`, `CLIENT_SHOTS`), merged
    by `tools/concordance.py`; `tools/encyclopedia.py`'s `KEY_HELP` names the key; lang regenerated (116 keys added).
  - **Audits:** new `check_roster` (the server side, payloads, journal, gates, G13 and `D07_TESTS`) and
    `check_roster_screen` (client only: the classes, the key, the client test); `check_pacts` pins the hand's rule.
  - G6's roster action is done: a holder ends an agreement, a pact without its perch or a record not yet claimed from
    the roster, as well as at its block.
- **Departures from this ticket and from the unit's specification:**
  - portraits (the unit's critique 8): `KIND_PORTRAITS` maps `spirit` to none, so an unloaded spirit is drawn as the
    Spirit Anchor's icon, since a Gathering Shade's kind and a Workshop Hand's are both `spirit`; the generator, the audit
    and a mutation pin it (D-P115 as built);
  - `check_roster`'s read-only scan matches `note(...)` with arguments only: the specification's `\.note\(` flagged its
    own `Progress.note()` read in `requestRow`; a roster write is still caught (S10);
  - `Roster.act` RESUME on a record not yet claimed answers `nothing`, not `resumed` (direct calls only; the screen never
    offers it);
  - critique 3: the client test checks each row's portrait one at a time, after Tab has brought the row into view (the
    scroll body draws only what is inside it), waiting for the row to be drawn rather than a fixed two ticks;
  - critique 9: no fallback was needed: `ScreenKit.Fitted`, `panelArea()` and the screen fit line are in this branch
    (D-P121);
  - the client test also puts the window size back in its `finally` block, with the key and the GUI scale;
    `check_roster_screen` finds a roster request made with a qualified class name too (mutation C24 slipped past the
    plain needle);
  - the D-07 tests use `Items.WOOL.white()` (26.3 has no `WHITE_WOOL`, critique 1), int icons, portraits, `gone()` for
    Stolas and `disabled` through the screen; the nine tests are the roster part's text, normative for this unit
    (critique 2).
  None of the ticket's behaviour was dropped. The vanilla `ConfirmScreen` the ticket names is used through its subclass
  `KeepFirstConfirmScreen`.
- **Tests (first run in CI run 40121823509 on `040d73ece`, where they passed; passed again in run 40133540741):**
  `ConcordanceWorkerGameTests` +7 (`theRosterEndsOnlyWhatTheReaderHolds`,
  `aRequestNamingAnotherPlayersRecordIsRefused`, `theRosterListsAPactWithoutItsPerch`, `endingAnOrphanReturnsItsEscrowOnce`,
  `aStaleRosterRevisionIsRefused`, `theRosterStopsAt64Rows` and `aReplacedHandLoadingWhileSwitchedOffStaysOffTheRoster`),
  `ConcordanceLogisticsGameTests` +2 (`recoveryNeverTakesCargoOutOfAnUnloadedContainer`,
  `theRosterEndsAHandAndRecoversItsCargoOnce`), `ConcordanceJournalGameTests` +1
  (`thePactsSectionListsPactsOnlyAfterTheBindingArts`). With D-06's seven, unit 3 adds 17 server game tests (Worker 49
  to 63, Logistics 28 to 30, Journal 7 to 8). Client: new `ConcordanceRosterClientGameTests` (one client on the game's
  dedicated server: empty, rows, three sizes, suspend and resume, an end asked first and kept by Esc, then ended by
  keyboard, a recovery and a cancel, 64 rows on eight pages; ten shots; registered in the gametest `fabric.mod.json`,
  weight 140); `ScreenFitClientGameTests` opens the roster at 320×240; a selection unit test checks that the screen
  selects both classes. The D-07 helpers (`roster`, `act`, `rowOf`, `answered`, `result`) are package-private statics
  in `ConcordanceWorkerGameTests`, which the logistics tests call: no new helper file (unit 2's D-P37).
- **Audit mutations:** the server side's 28 (S1 to S22 from the specification and S23 to S28: `disabled` for an action
  not offered while switched off, the hand's override and test, the journal test, recovery through the ledger, the
  spirit portrait) and the screen's 25 (C1 to C18 and C19 to C25: the refresh period, the key over another screen, a
  state word, the fitted panel, the portraits, a second sender, a text box) were each reported by `check_roster`,
  `check_pacts`, `check_roster_screen`, the key audit or the selection unit test; each unchanged and restored tree
  reported nothing.
- **Commands** (each run and passed before its commit): as D-06's, with `python3 -m unittest discover -s scripts/tests`
  at 197 tests, then 198; every changed Java file parse-checked.
- **The records' commit** ("Concordance Chunk D: unit 3's records"; records only): run and passed on its tree,
  `python3 tools/owner_art.py --check`, `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
  `python3 tools/concordance_journey.py --check` ("every route holds"), `python3 tools/check_icon_maps.py`,
  `node --test tools/world-designer/model.test.cjs` (12), `python3 -m unittest discover -s scripts/tests` (198) and
  `git diff --check`; `python3 tools/select_client_tests.py --base 92868ff82 --head HEAD` chooses 52 of 242 client
  classes, `ConcordancePactClientGameTests`, `ConcordanceRosterClientGameTests` and `ScreenFitClientGameTests` among
  them. Its mutations: a broken link in each of the ten records it changes and `stolas-pact.md` removed (each reported
  by `scripts/check_repository.py`), trailing whitespace in this page (reported by `git diff --check`), and seven of
  the code commits' (B7, B21, P5, P15, S9, C13, C17) again on the final tree against the Concordance audit alone (each
  reported); the unchanged tree reported nothing.
- **Since run in CI:** run 40120355940 on `554d71ab1` failed to compile at the roster client test (`LOGGER.info` with a
  generic `computeOnClient` result was ambiguous; fixed in `040d73ece`); run 40121823509 on `040d73ece` passed the
  server game tests with and without the optional integrations and the client tests, `ConcordanceRosterClientGameTests`
  and `ScreenFitClientGameTests` among them; run 40133540741 passed them again. Still not run: two people; the narrator
  heard.
- **Not confirmed until CI:** generic inference for the 14-field `Row` codec with `UUIDUtil.STREAM_CODEC`; that a
  GeckoLib creature draws through the GUI's entity picture (the Spirit Anchor's icon is drawn only when the creature
  cannot be made; whether GeckoLib draws in the GUI is shown only by the shots, since the client test's `drawn` records
  the attempt, not pixels); that `setHealth(0)` on a test player leaves it not alive for one synchronous test; the
  keyboard's Tab focus scrolling a card into view at every size.

**Unit 3's unit review (`426b16196`, the commit "Chunk D unit 3: fixes from the unit review"; first run in CI run
40121823509, where it passed, and again in run 40133540741).**
Twenty-two findings were confirmed and fixed; nine were rejected and left unchanged.
- **A departure always ends (G1; D-P97 as fixed):** his 40 ticks were counted in `customServerAiStep`, which runs only
  while his entity ticks, so after a roster End from afar, or a holder who went away within the 40 ticks, he stayed
  perched with Jade reading "Departing"; and since a departing Stolas is never saved, an unloading chunk kept him in
  memory, never discarded. The end is now a game time (`departAt`), reached by his own tick or by a sweep at the end of
  every server tick (`StolasEntity.registerSweep`, once in `Workers.register`; its identity set emptied at stop) that
  discards each departure due, his entity ticking or not. New `aDepartureEndsWithoutHisTicks` runs the sweep at explicit
  game times with no tick between: not before its length, then discarded.
- **A late watcher sees him gone (G2):** leaving clears his listening and sets a synced `DEPARTING` flag
  (`leaving()`); `WorkerClient` draws a Stolas leaving only where his gesture controller was triggered with his
  departing gesture. `aDepartingStolasIsNeverAVisitorAgain` asserts both after a decline.
- **Mono cues (D1; D-P101 as fixed):** the cave book's pages and the ghost's weep are stereo, which OpenAL does not
  place, so a player 15 blocks away heard them at full volume. Listening and inspecting now play a spelunkery table's
  paper laid in and drawn out, and his going a vesper's wing beats (mono imports, catalogued in `cavern-runtime.json`);
  `check_pacts` reads every bound file's Vorbis channel count; `sounds.json` regenerated.
- **The roster's "too fast" (C1; D-P107 as fixed):** an action its gate refused was answered with the whole roster
  again, once for every packet a client numbered higher; it is now answered once a gate's span (`Session.tooFastAt`)
  and the rest dropped, as the composer drops them.
- **Client-test selection (D2):** `SHOWN_BY` maps `WorkerClient` (Stolas's renderer) and `PactDataProvider` (his perch's
  and anchor's Jade provider), which no client test names, to `ConcordancePactClientGameTests`; a selection unit test
  holds both.
- **Audits (D3, D4, D5, D11):** `check_roster_screen` checks lang keys chosen in a conditional inside `ui(...)` and, as
  for the pact screen, that the roster is an in-game screen drawing subtitles in its background; `check_roster`'s "it
  loads nothing" bans `getBlockEntity(` and `getBlockState(`; `check_pact_screen`'s docstring no longer claims Stolas's
  body.
- **Words (D10, D8's dimensions):** the Cancel hint speaks of cargo only when some is in transit (`confirm.cancel_none`);
  a place in one of the mod's ten dimensions is named (each lair as `tools/lairs.py` names it, a Planarium's cells by
  the block's name) instead of its id, and `check_roster` holds every dimension file to its words.
- **Records (R1 to R7, R9, R10, D7, G3):** the portrait fallback reworded in four places (the Spirit Anchor's icon is
  drawn only when the creature cannot be made; whether GeckoLib draws in the GUI is shown only by the shots, `drawn`
  recording the attempt); `stolas-pact.md`'s actions from the roster, its gates and four-number request, the sound
  catalogs, the waiting loop's states, a resume's assent "at the perch" (and D-P99: the roster fires no gesture), the
  unposed clips, the provenance table's added cell; the screen fit record's fourteen shots; the open terms item; the
  CHANGELOG's 64 rows; D-06's two-channel line.
- **Deviation from the review's fix:** the client hides a Stolas leaving unless his gesture controller was triggered
  with DEPART (`getCurrentRawAnimation()`), not unless it is drawing `animation.stolas.depart`: GeckoLib starts a
  triggered clip only in a render pass, which `shouldRender` returning false would skip, so the drawing test would hide
  him from every watcher. The sweep's test entry takes an explicit game time (`sweep(long)`); the server's reads each
  Stolas's own level.
- **Rejected, unchanged:** C2 (an oversized roster needs command-made names tens of KB long), C3 (a stale view lasts one
  round trip), C4 (the 2-tick margin is deterministic), C5 (no path departs a still-valid record's presence), D6 (the
  client test pairs each loop with its shot), D8's `ANSWERS` table (audited from the Java), D9 (Jade's config label is
  cosmetic), R8 (the hand's behaviours are recorded) and R11 (the 22 mutations agree).
- **Audit mutations** (in place on the focused Concordance audit or the selection unit test, each restored; the tree's
  digest equal before and after): RV3-M1 to RV3-M27 were each reported: `departAt` a tick count, his own tick ending
  nothing, the sweep unregistered, off the server tick, never fed or never emptied, still listening, no synced flag,
  saved while departing, the client drawing a late-seen departure or reading the wrong gesture, the new test removed, a
  stereo file bound, every "too fast" answered or its time never kept, a block read by the roster, two conditional keys
  without lang, the roster not in-game or its subtitles outside its background, the Cancel hint always naming cargo, a
  lair or the Planarium without words or a lair worded otherwise, and `SHOWN_BY` without either class or never applied.
  The unchanged tree reported nothing.
- **Commands** (all run and passed on the final tree): `python3 tools/owner_art.py --check`,
  `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`, `python3 tools/concordance_journey.py
  --check`, `python3 tools/check_icon_maps.py`, `node --test tools/world-designer/model.test.cjs` (12),
  `python3 -m unittest discover -s scripts/tests` (199) and `git diff --check`; the five changed Java files
  parse-checked. Generator: `python3 -B tools/generate_material_data.py` (lang: 11 keys added; `sounds.json`); its
  reordering of `docs/features/jug-nouveau-content-map.json` was restored. Server game tests now: Worker 64. After the
  commit, `python3 tools/select_client_tests.py --base 92868ff82 --head HEAD` chooses 52 of 242 client classes,
  `ConcordancePactClientGameTests`, `ConcordanceRosterClientGameTests` and `ScreenFitClientGameTests` among them.
- **Since run in CI:** the build, the server game tests (with `aDepartureEndsWithoutHisTicks`) and the client tests
  passed in run 40121823509 on `040d73ece` and in run 40133540741.
- **Not confirmed until CI:** the `shouldRender` override's signature (26.3's `EntityRenderer`, from the mirror) and
  `getManagerForId`'s inference in `WorkerClient`; that `discard()` removes a departing Stolas from a section whose chunk
  has unloaded; that a sweep run by a test at a future time, which ends every departure due by then, disturbs no other
  test (none watches a departing Stolas across ticks).
- **Confirmed from source, not run (unit 5):** `/tick freeze` stops game time (`mc263src` `ServerLevel.java:346-366`:
  the level's time moves only while the tick rate runs normally), so the sweep, which ends a departure at its game time
  (`departAt`), holds a frozen departure; and GeckoLib's animation clock is the render state's `ageInTicks`
  (`EntityRenderStateMixin:46`). No test checks either by itself.

### D-08. The contrasting service: the workshop hand (unit 4)

**What the player sees (D-S2).**
- A holder who has mastered Stolas's Regard binds a Spirit Anchor to their Courier Post (a Porter Key naming the post,
  `Couriers.KEY_POST`, `courier/Couriers.java:61-69`, used on the anchor) and seals a **workshop hand** there. Within its
  hours, area and daily quota it carries that post's exact requests through the courier ledger, the crucible's own
  formula shortage from C-06 included (a Peepo serving the same crucible subtracts the hand's live claim,
  `CrucibleBlockEntity.java:506`).
- A full destination keeps the cargo aboard; an unloaded one waits; ending the agreement strands the cargo at the post
  for one recovery.
- **Beneficiaries:** the holder and the holder's party, the Courier Post's own rule (`CourierPostBlockEntity.java:48-51`),
  stated in its terms and recorded on the agreement.

**Changes.**
- **Its own worker definition (critique 19):** `tools/concordance_workers.py` gains the work `courier` in `WORK` (`:44`;
  `worker/WorkerParser.java`'s `WORK` with it, which `check_workers` compares, `tools/check_mod_data.py:17882-17884`)
  and a definition `workshop_hand` in `WORKERS` (`:113`; kind `spirit`, so `check_workers`' three kinds stand), with its
  own entity type `jugcraft:workshop_hand`, lang and GeckoLib model, a generator variant of the Gathering Shade's body
  (`tools/concordance_worker_models.py`), not a new Goetia identity. `check_workers`' class map
  (`tools/check_mod_data.py:17910`) gains it. Its deliveries record `worker_service` with the key `spirit`, as the
  shade's do (`GatheringShadeEntity.java:132`), so Binding Arts' "distinct 3" is unchanged.
- **The porter's courier decision is extracted into a shared helper, not copied** (`ClockworkPorterEntity.java:291-510`,
  `mayTake` at `:508-510`): the porter and the hand call it; the porter behaves identically, as its existing logistics
  tests show. The hand holds only its claim id (saved `task`); its `remove` releases with `worker_removed`, stranding the
  cargo (`ClockworkPorterEntity.java:586-602`); an unload lets the lease lapse (200 ticks, `Logistics.java:41`).
- Who benefits is the Bound Will's `pact.beneficiaries` (`party` for a hand; D-P1, D-P74); `Agreement` gains nothing.
  The anchor gains an optional `post` (D-P73).
- `UNAVAILABLE_WORKER` stays unused (D-P79).
- **The schema review (09:166; 03:177):** a section of this contract written at this ticket, comparing what the
  knowledge service (Star-reckoning) needed with what the cargo service needs (claims, leases, reservations, a loaded
  destination, access for keeper and requester, exact variants, beneficiaries), and proposing the shared schema to
  freeze at the checkpoint, not before.

**Tests.**
- `ConcordanceLogisticsGameTests`: 03:193's eight cases for the hand, mirroring C-06's table (CC:1127-1136): the
  destination fills, access removed, the path fails, the target unloads, the hand dies, its holder logs out, the source
  breaks, the server restarts; and `twoHandsCannotHoldOneRequest` (`CLAIMED`), `endingWithCargoAboardStrandsItOnce`,
  `aHandServesOnlyItsBeneficiaries`. `problems()` and the conserved count checked after every step. The 15 existing
  tests pass unchanged (the porter after the extraction).
- `ConcordanceWorkerGameTests`: `aRequestOutsideTheHoursOrAreaIsRefusedWithItsReason`; `theHandNeedsStolasRegardMastered`.
- Client companion: in `CompanionAutomationChecks`, run by `PeepoCompanionClientTests`, the Peepo's need subtracts the
  hand's live claim (D-S20 allows extending the gametest companion checks).
- `ConcordancePersistenceGameTests`: a restart with a claim in progress.

**Acceptance:** E4 and E5 with real cargo; E6 (Logistics and the client companion tests); the different requirements of
a cargo service written down.

**Saves:** the hand's `task` (and, as built, its `anchor`); the anchor's `post`; who benefits on the Bound Will's pact
(`beneficiaries`, a key since unit 1; D-P74); the courier ledger's codec unchanged.

**Review boundary:** no file under `src/main/java/local/peepo` (D-S20); the porter's behaviour unchanged.

**As built (unit 4: commits `023b681e8`, the shared courier decision, and `e51ef2b11`, the hand; records and the schema
review in the records' commit "Concordance Chunk D: unit 4's records and the schema review"; the unit review's fixes in
the commit "Chunk D unit 4: fixes from the unit review":
[arcane-concordance-logistics.md](arcane-concordance-logistics.md),
[arcane-concordance-workers.md](arcane-concordance-workers.md),
[arcane-concordance-persistence.md](arcane-concordance-persistence.md)). Built on the local `wip/chunk-d-u4` from unit
1's head `97e1facd4`, beside unit 2, which is built in parallel on `wip/chunk-d-u2` and is not in this tree (the merge
points are the last item below).**
- **Built:**
  - `023b681e8`: the porter's courier decision moved, unchanged, into `spirits/CourierWork`, which holds the claim's id
    (each courier saves it as `task`) and asks a courier's `Terms`: `ready` (asked only with no live claim), `refuses`,
    `reaches`, `load`, `atPost`, `delivered` and `postGone`. The porter's terms are its body, every request, every
    container, no cap, a pylon at its post and a construct's trip on each delivery (D-P80).
    `Logistics.next(post, now, serves)` skips requests a courier does not serve and `next(post, now)` serves every one;
    `Logistics.reserve` and `CourierLedger.reserve` take a per-trip cap and their old signatures pass
    `Integer.MAX_VALUE` (D-P78). The porter's 15 logistics tests are unchanged: the diff of their file removes no line.
  - `e51ef2b11`: `spirits/WorkshopHandEntity` (`jugcraft:workshop_hand`, a spirit, D-P70, D-P71). Its decision asks, in
    order, the switch, its anchor's standing (`verify`: a copy, a record gone or another hand answering the agreement
    makes it leave), its roster place, its holder answering (`Authority.answering`), then the shared decision under its
    terms: new claims only while its Bound Will holds nothing, within its hours and day's loads; only its beneficiaries'
    requests bound inside its area (D-P74, D-P76); sources inside its area; at most its carry a trip; a load delivered
    in full one task (D-P77); each delivery a spirit's service for its holder, present. Ended it releases with
    `agreement_ended`; killed or discarded it is struck off and releases with `worker_removed` (D-P75); it saves
    `anchor` and `task` only.
  - The Spirit Anchor: an optional `post`; `handSealing` (the hand's own agreement and counterpart, party beneficiaries,
    no upkeep, any number a holder, party seals, no cost); `call` returns a `WorkerEntity<?>`, a shade or a hand by
    counterpart (D-P15). `SpiritAnchorBlock.use` seals a hand at an anchor bound to a post (`sealHand`: the Regard, who
    may seal, the post still usable, then the seal; D-P72) and shows the post (or that it is gone) when read;
    `useItemOn` passes a Porter Key through to the key. `PorterKeyItem.bind` and `Binding` (D-P73).
    `Workers.WORKSHOP_HAND` (MISC, 0.6 by 1.6, 16 health); `WorkerClient` draws it with GeckoLib; Jade's
    `WorkerDataProvider` shows its carried count. `WorkerParser.WORK` gains `courier`.
  - Generators, their output regenerated with `python3 -B tools/generate_material_data.py` and never hand-edited:
    `tools/concordance_workers.py` (`WORK`, `HAND_RESEARCH`, the definition, `ENTITY_NAMES`, the key's and anchor's
    tooltips, nine messages, the codex entry `binding/workshop_hand` at (4,4), and the Binding Arts' Understood page
    changed only in "a spirit's delivery (a Workshop Hand's is a spirit's)"); `tools/concordance_logistics.py` (the note
    `agreement_ended`; the key's post message names the anchor); `tools/concordance_worker_models.py` (the shade's body
    unchanged, a wicker pack and lid on a bone of their own, five clips); `tools/concordance_worker_art.py`
    (`hand_sheet`: the shade's sheet with amber eyes and a wicker pack with a brass buckle). The sheet was written by
    `hand_sheet()` with the textures generator's own save call; `tools/generate_textures.py` was not run.
- **Departures from this ticket and from the unit's specification** (proposals D-P70 to D-P83 in Decisions):
  - **Order:** built beside unit 2 from unit 1's head, not after Stolas works (Dependencies and order, item 5). The gate
    names unit 2's research id `jugcraft:stolas_regard` only (D-P72) and the game tests grant that id directly
    (`ConcordanceProgress.grant` stores any id). `check_hand` checks that the research exists with a mastered state only
    once unit 2's `tools/concordance_pacts.py` is in the tree; the specification had that check report until unit 2
    lands, which would have failed this tree's checks.
  - Who benefits is on the Bound Will, not `Agreement` (D-P74); `UNAVAILABLE_WORKER` stays unused (D-P79).
  - The specification's reconciliation, kept: a hold, the hours and the day's loads stop only new claims and a claim
    held is finished, with no release note (D-P75); a request that is not a beneficiary's is `IDLE` and left for other
    couriers, one bound outside the area `OUTSIDE_AGREEMENT` (D-P76); a task is one load delivered in full (D-P77);
    radius 12, 06:00 to 18:00, 32 loads, 64 a trip (D-P70); a hand is called without asking for a bound post (an anchor
    keeping an agreement cannot be unbound; with no post a hand waits, `OUTSIDE_AGREEMENT`).
  - The unit review's nine corrections: the codex entry at (4,4), since unit 2 holds (0,4) and (2,4) (1); the Understood
    page changed by its substring only, and `check_hand`'s call, definition and test lists placed by text (2);
    `aHandsServiceIsASpiritsService` asserts that exactly a familiar's and a spirit's service are recorded, and that a
    spirit's again records nothing (3); `aRequestOutsideTheHoursOrAreaIsRefusedWithItsReason` suspends the hand with a
    load claimed: it finishes the load, claims nothing new (`SUSPENDED`, asked before its day's loads) and, resumed,
    serves again (4); "checked at each new claim; a load already claimed is finished", asserted in
    `aHandServesOnlyItsBeneficiaries` with a load claimed before its requester leaves the party (5);
    `anAnchorIsBoundOnlyWhileItKeepsNoAgreement` binds through `bind` to the owner's posts 13 blocks (refused) and 12
    blocks (bound) above the anchor (6); `WorkshopHands.conserved` after every step the review named (7); the risks
    corrected (8; Risks); the optional ninth, skipping an absent requester's request, not taken (D-P81).
  - The audit's item-list pattern also matches qualified names (`java.util.List<net.minecraft.world.item.ItemStack>`),
    which the specification's pattern missed (M13 below).
  - The specification's risk "the key now binds instead of setting a useless route source" was half right: before, a key
    used on an anchor acted as an empty hand (the anchor's own use) and, sneaking, took the anchor for a route's
    container; now it binds and, sneaking, unbinds (Risks).
  - Tests: the ticket's names kept (`theHandNeedsStolasRegardMastered` in the Worker suite; the restart in the
    Persistence suite, as C-06's was). Added beyond the ticket: `aHandTakesOnlyFromInsideItsArea`,
    `theConcordanceOffHoldsTheHand`, `aHandsServiceIsASpiritsService`, `anAnchorIsBoundOnlyWhileItKeepsNoAgreement`,
    `aStrangerCannotBindAHandToAnotherPlayersPost` and `aBoundAnchorKeepsItsPost`.
- **The unit review's fixes** (commit "Chunk D unit 4: fixes from the unit review"; the review read the code at
  `0d04161a5` and confirmed 13 findings, rejecting 9; each confirmed one is handled here, two of them only as merge
  notes because what they need is not in this tree):
  1. A hand no longer takes up stranded cargo larger than its carry: a porter's (which reserves with no cap of its own,
     up to a request's 256) killed at the hand's post, or its own after a data pack lowered its carry. Its terms refuse
     it (`IDLE`) before the beneficiary and area check, so it waits for porters or its requester's one recovery, and the
     "at most 64 a trip" and "2,048 a day" promises hold (D-P82); `aHandLeavesStrandedCargoBeyondItsCarry`.
  2. Nor does it claim cargo to be taken back to a source beyond its area: it could put that only into the post, and
     with the post full it claimed, took up and stranded it again at each decision (three of the post's 32 history
     entries a cycle, bounded, nothing lost). Refused (`IDLE`) with the first (D-P82). Cargo it already holds whose
     source is beyond its area still goes into the post when cancelled, never to that source: `takeBack`'s
     `terms.reaches(from)`, which D-P76 promised, is now audited (M41) and tested
     (`aHandTakesCargoBackOnlyInsideItsArea`).
  3. A courier whose post is gone says why it waits: `Terms.postGone()` returns the status `serve` gives. A porter
     forgets its post and is `IDLE`, as before; a hand says `OUTSIDE_AGREEMENT`, as with no post, so D-P79 ("the hand's
     status already says why it waits") holds here too (D-P83).
  4. `SpiritAnchorBlock.use` asks the switch alone, then only the definition of the worker the anchor serves: a hand's
     for an anchor bound to a post with no agreement or keeping a `courier` agreement (`WorkshopHandEntity.WORK`), the
     shade's otherwise. Before, a data pack that broke `gathering_shade` made every hand's anchor answer "disabled" to
     sealing, suspending, recalling and reading, while the hand itself worked on. `sealHand` now takes the definition.
     `verify` runs before a definition is asked, as `PorterKeyItem.bind` already did (D-P83).
  5. The pack's lid is a bone of its own (`pack_lid`, on the pack, hinged at `[0, 18, 2]`, along its edge against the
     body), and the working clip turns only the lid, +30 on x, so its back edge lifts about 1.5 px. Before, the clip
     turned the whole pack -12 on x, the arms' forward sense, pushing the pack's foot about 1 px into the body and
     dropping the lid's back edge about 0.4 px. The review's example hinge, the lid's far edge `[0, 18, 5]`, would lift
     the near edge into the hood, so the near edge was taken.
  6. Records: the cargo tests' claim corrected (below; `e51ef2b11`'s message, "each checking the ledger, the Bound Wills
     and the item count after every step", is left as committed, since no commit is rewritten: the two binding tests
     move no cargo); unit 2's row in the units table; D-P15's extension on D-P80; "the code's second user" dropped from
     D-P73 (the key is its third, after `AgreementHolder` and the anchor's block); the rule for anchors placed before
     Chunk D in the binding wording (`CHANGELOG.md`, the logistics and workers records); the schema review's fulfilment
     and departures cells; the merge notes re-read against unit 2's head and against unit 1's review fixes (below).
  7. The codex's "Its Terms" page names what the hand leaves (cargo beyond its carry, or that must go back beyond its
     area) and its post gone among the reasons it waits; the lang regenerated.
- **Tests (passed in CI run 40085012234, after the helper fix):** 21 server game tests, each `maxTicks = 20` and synchronous in one tick, on a new
  `WorkshopHands` fixture (package-private, not a test class). The 19 that move cargo start at a game day's beginning
  and check `problems()`, the Bound Wills and the conserved count after every step; the two binding tests
  (`anAnchorIsBoundOnlyWhileItKeepsNoAgreement`, `aBoundAnchorKeepsItsPost`) move none:
  - `ConcordanceLogisticsGameTests` +13 (15 to 28): seven of 03:193's cases (`aHandFillsACrucibleUntilItIsFull`,
    `aHandTakesNothingWhereItsHolderMayNot`, `aHandThatCannotFindItsWayStrandsItsCargoOnce`,
    `anUnloadedDestinationKeepsTheHandWaiting`, `aKilledHandStrandsItsCargoOnce`, `anAbsentHolderStopsTheHand`,
    `aBrokenSourceReleasesTheHandsClaim`), `twoHandsCannotHoldOneRequest`, `endingWithCargoAboardStrandsItOnce`,
    `aHandServesOnlyItsBeneficiaries` and `aHandTakesOnlyFromInsideItsArea`; and, from the unit review,
    `aHandLeavesStrandedCargoBeyondItsCarry` (a porter's stranded 65 passed and left for its requester's one recovery,
    its history showing no claim by the hand; a stranded 64 behind it taken up and delivered, one load counted) and
    `aHandTakesCargoBackOnlyInsideItsArea` (under a test area of 3, a held load from a chest beyond the area, cancelled,
    goes into the post and not to the chest; a stranded load to be taken back there is never claimed and is recovered
    once).
  - `ConcordanceWorkerGameTests` +5 (22 to 27): `aRequestOutsideTheHoursOrAreaIsRefusedWithItsReason`,
    `theHandNeedsStolasRegardMastered`, `theConcordanceOffHoldsTheHand`, `aHandsServiceIsASpiritsService`,
    `anAnchorIsBoundOnlyWhileItKeepsNoAgreement`.
  - `ConcordanceAuthorityGameTests` +1 (8 to 9): `aStrangerCannotBindAHandToAnotherPlayersPost`.
  - `ConcordancePersistenceGameTests` +2 (15 to 17): `aHandsClaimSurvivesARestart` (03:193's eighth case),
    `aBoundAnchorKeepsItsPost`.
  - Client: `CompanionAutomationChecks.workshopHand` ("a crucible's need subtracts a workshop hand's live claim"), run
    by `PeepoCompanionClientTests`.
- **Audits** (`tools/check_mod_data.py`): `check_hand` (new, called after `check_logistics`): one courier decision
  (`next` and `reserve` delegate; `CourierWork.serve`'s needles and its order, terms asked only with no live claim;
  `reserve` within reach and load; neither courier repeating a ledger step), the hand a spirit whose service is a
  spirit's and no worker's service recorded under another key, the Regard at MASTERED and asked before the seal, the
  decision's order, the terms' needles, the refusal, `leave` and `remove`'s notes, no item list, the anchor's optional
  `post`, `call` and party `handSealing`, the key's order and `bind`'s, the sheet against the generator, the unit's 21
  tests, the porter's 15 and the companion check; `check_workers`' class map; `check_logistics` (release notes and item
  moves also scanned in `CourierWork` and the hand); `AUTHORITY_ROUTES` (the porter's requester check moved to
  `CourierWork`, which also names the requester's `answering` and the take-back check; the hand's `answering`; the key's
  `maySeal`). Since the unit review, `check_hand` also holds `takeBack`'s area check, `serve` returning the terms'
  reason when the post is gone, the hand's carry refusal (before its beneficiary check) and take-back refusal and its
  `postGone`, `WorkshopHandEntity.WORK` against the generator, the lid a bone of its own on the pack that the working
  clip turns instead of the pack, and `use` asking the switch alone and then the definition by the anchor's path,
  passing it to `sealHand`; the Terms' methods are read to their own closing brace.
- **Commands** (run and passed before each of the unit's three commits): `python3 tools/owner_art.py --check`,
  `python3 -B tools/check_mod_data.py`, `python3 scripts/check_repository.py`,
  `python3 tools/concordance_journey.py --check` ("every route holds"), `python3 tools/check_icon_maps.py`,
  `node --test tools/world-designer/model.test.cjs` (12 passed), `python3 -m unittest discover -s scripts/tests` (195
  passed) and `git diff --check`; the generators run one at a time after each table change. Every changed Java file
  parse-checked (19, no syntax error); `CourierWork` type-checked against stubs, and a scratch trace of the pure ledger
  with the old calls matched the ledger before the change (commit `023b681e8`).
  `python3 tools/select_client_tests.py --base 97e1facd4 --head HEAD` chooses every client class (239):
  `WorkshopHands.java` and `CompanionAutomationChecks.java` are shared files. For the unit review's fixes, the same
  commands ran and passed before their commit (`tools/check_mod_data.py` PASS, the journey "every route holds", the
  World Designer 12 passed, the script tests 195 passed, `git diff --check` clean);
  `python3 -B tools/generate_material_data.py` ran once after the generator changes (it reordered
  `docs/features/jug-nouveau-content-map.json` without changing its content, which was restored); the five changed Java
  files were parse-checked (no syntax error); nothing was type-checked against stubs this time.
- **Audit mutations** (each applied in place on the final tree to the audits the unit touches, `check_workers`,
  `check_pacts`, `check_logistics`, `check_hand`, `check_authority` and `check_persistence`, every file restored after
  each): the specification's 40 (M1 to M40; M18 in two forms with unit 2's tables
  simulated by a scratch marker, and a control with a mastered state that reports nothing) and M13 also with qualified
  names. All 42 are reported and the unchanged tree reports nothing. M13 with qualified names was not reported until the
  pattern was widened (above). As run: M2 as `Math.min(most, available)`; M3 as the porter making a ledger claim itself;
  M29 and M34 without regenerating (the audits compare the tables with the generated data); M32 is reported by
  `check_workers`' clip check rather than `check_geckolib`. For the unit review's fixes, nine more, each applied in
  place to `check_workers`, `check_pacts`, `check_logistics`, `check_hand` and `check_authority` and the file restored
  byte for byte after it: M41 `takeBack` without `&& terms.reaches(from)`; M42 the hand's carry refusal removed; M43
  its take-back refusal disabled; M44 its `postGone` returning `IDLE`; M45 `serve` returning `IDLE` whatever the terms
  say; M46 `use` asking the shade's definition for every anchor again; M47 `WORK = "gather"`; M48
  `aHandLeavesStrandedCargoBeyondItsCarry` renamed; M49 the working clip turning the pack again. All nine are reported
  by `check_hand`, and the unchanged tree reports nothing; M5, M9, M10 and M19, whose code the fixes touched, were run
  again and are still reported. The two new tests are what would catch the behaviour itself (by reading; neither has
  run): without M41's check `aHandTakesCargoBackOnlyInsideItsArea` would not see `RETURNING` (the hand would set off
  for the chest), without M43's refusal it would not see `IDLE`, and without M42's
  `aHandLeavesStrandedCargoBeyondItsCarry` would see the hand take up the 65.
- **Not run where it was written:** `./gradlew build`, with or without the optional integrations (the Jade edit), the
  21 server game tests and the client companion check; since run in CI, all passed in run 40085012234 on `86c4e7127`
  and in runs 40094972598 and 40133540741. Still not run: any client by hand, two clients, a real server restart,
  real walking (the path test sets SmartBrainLib's `CANT_REACH_WALK_TARGET_SINCE` directly; death is `remove(KILLED)`;
  the restart is codec round trips, an unload and the entity added again).
- **Not confirmed until CI:** that the whole unit compiles, in particular `CourierWork`'s use of `WorkerEntity`'s
  protected `walkTo`, `near`, `arrived`, `stop` and `centre` (same package) and the anonymous `Terms` classes assigning
  their enclosing fields; that the SmartBrainLib the repository pins keeps a `CANT_REACH_WALK_TARGET_SINCE` set directly
  (the test asserts the slot first); `addFreshEntity` of a hand read back after `remove(UNLOADED_TO_CHUNK)`; a Fabric
  `FakePlayer` through `UseOnContext` and `PorterKeyItem.useOn`; `Authority.mayChange` for the client player in the
  integrated client world; `TagValueInput.read("post", GlobalPos.CODEC)` on a string value returning empty rather than
  throwing; that the default test structure is 8 by 8 by 8; how a client-side `ItemStack.useOn` answers for the key;
  that unit 2 keeps `jugcraft:stolas_regard` with a mastered state (it does at `832129b95`) and leaves
  `AgreementHolder`'s members the hand uses unchanged (`maySeal`, `seal`, `Sealing`, `Sealed`, `verify`, `Standing`,
  `record`, `held`, `taskDone`, `told`, `spiritLost`, `recall`, `end`, `service`; unchanged at `832129b95`).
- **Where unit 4 meets unit 2 in the merge** (unit 2 read at `wip/chunk-d-u2` `832129b95`, and again for the unit
  review's fixes at its head `8f0635d9e`, after its D-05 commit `ee82c1dc7` and its records' commit, read only; a trial
  three-way `git merge-file` of each file both units change, in scratch, since deleted):
  - Generated files, `src/main/resources/assets/jugcraft/lang/en_us.json` and
    `tools/data/generated-material-resources.json`: take either side and run
    `python3 -B tools/generate_material_data.py`; never hand-merge them.
  - `tools/concordance_workers.py` (two hunks): both append to `MESSAGES` after `workers.departed.broken` (keep both
    blocks); both change the Binding Arts' Understood page (keep unit 4's substring and unit 2's appended sentence: "a
    spirit's delivery (a Workshop Hand's is a spirit's) and a construct's trip. Stolas, a raven among the spirits, may
    take notice of you as well:" and its link to `binding/stolas_regard`). Unit 2 adds `SEAT_OUTCOMES`; unit 4 the
    docstring, `WORK`, `HAND_RESEARCH`, the definition, `ENTITY_NAMES`, the tooltips and the codex entry.
  - `tools/concordance_worker_art.py` (one hunk): keep unit 2's `ICONS` line (`pact_perch`) and unit 4's `SHEETS`
    (`workshop_hand`); both edit the docstring.
  - `tools/check_mod_data.py` (two hunks in the trial). In `check_concordance` (merged cleanly): unit 2's
    `check_stolas_regard` and `check_pact_screen(co, lang)` (from `ee82c1dc7`) come before `check_logistics`, and unit
    4's `check_hand` after it; keep all four calls in that order. Unit 2 widens `check_pacts`' signature and its call;
    unit 4's `def check_hand` follows `def check_logistics`. The test tables, one hunk: unit 2's `D04_TESTS`,
    `D05_TESTS`, `D05_ON_STOLAS` and `D03_TESTS` and unit 4's `UNIT4_TESTS` and `PORTER_LOGISTICS_TESTS` all follow
    `UNIT1_TESTS`; keep every table. `AUTHORITY_ROUTES`, the other hunk: keep unit 4's `spirits/PorterKeyItem.java` line
    with both needles (`Authority.mayChange(level, player, pos)` and `holder.maySeal(level, player, true)`) beside unit
    2's new entries (`PactPerchBlock`, `PactMenu`, `PactKeeping`, `StarReckoning`): unit 2's side still has the old
    one-needle line, and taking it would drop the `maySeal` check silently. With unit 2's `tools/concordance_pacts.py`
    present, `check_hand` checks the Regard's mastered state (`stolas_regard.json` has one at `8f0635d9e`).
  - `src/client/java/io/github/jimbozoomer/jugcraft/client/WorkerClient.java` (merged cleanly in the trial): unit 2's
    `7da686ad4` adds Stolas's import, `STOLAS_SCALE` and his renderer, unit 4 the hand's import and renderer; keep both
    imports and both renderers.
  - `concordance/spirits/Workers.java` (one hunk): unit 2's Stolas and perch fields and registrations sit beside
    `WORKSHOP_HAND`'s, and both rewrite the class Javadoc's last line; keep both sentences.
  - `ConcordanceWorkerGameTests` (seven hunks in the trial), `ConcordancePersistenceGameTests` (four),
    `ConcordanceAuthorityGameTests` (one): both append tests at each class's end and add imports; keep both sides.
  - Codex positions: unit 2's `binding/stolas_regard` at (0,4) and `binding/stolas_pact` at (2,4); the hand's at (4,4).
    At the merge, link `binding/workshop_hand` to `binding/stolas_regard` (the codex's link check would fail here
    without unit 2's entry, so it is not added in this tree).
  - This contract (the status line, the units table, D-08, the schema review, Completion evidence, Interfaces, Order,
    Risks, Decisions, Where to resume), `CHANGELOG.md` (one hunk), `docs/INTEGRATION_STATUS.md` (two),
    `docs/WHAT_EXISTS.md` (one, the workers row) and `arcane-concordance-workers.md` (three), which unit 2's records'
    commit `8f0635d9e` also edits: merge by union.
  - Decision numbers: unit 1 holds D-P1 to D-P16 and unit 2's block, at `8f0635d9e`, D-P17 to D-P49; unit 4's D-P70 to
    D-P83 sit in a block reserved for it.
  - `AgreementHolder`: unit 2 adds `OnBreak`, `Seat`, `seat`, `stranded` and a second constructor, keeps the
    one-argument constructor the anchor uses and changes what breaking does by `OnBreak`; unit 4 does not edit it, and
    the members the hand uses are unchanged at `8f0635d9e`. Unit 2 does not edit `SpiritAnchorBlock`,
    `SpiritAnchorBlockEntity` or `CourierWork`.
  - After the merge: the schema review's Star-reckoning column re-read against unit 2's D-05 as built (its fulfilment
    cell already corrected from `StarReckoning.observed` at `8f0635d9e`).
- **Where unit 4 meets unit 1's review fixes** (`3775eac3b` "Chunk D unit 1: fixes from the unit review", the merge
  `55b3ca698` and `da185dd0d`, on `origin/claude/chunk-d-pact-hall`, read only; not in this tree):
  - Files both change: `CHANGELOG.md`, `docs/INTEGRATION_STATUS.md`, `docs/WHAT_EXISTS.md`, this contract, the workers
    and persistence records, `ConcordanceWorkerGameTests`, `ConcordanceAuthorityGameTests`,
    `ConcordancePersistenceGameTests`, `tools/check_mod_data.py` and `tools/concordance_workers.py`: merge by union;
    the lang is regenerated, never hand-merged.
  - Unit 1's fix 2: `WorkerEntity.setStatus` notes a worker on its holder's roster only if `joinsRosterOnStatus()`,
    true by default and false only for the shade. The hand would inherit true, so a hand replaced while its chunk was
    unloaded, loading later while the Concordance is off or its anchor unloaded, would report `DISABLED` or
    `DESTINATION_UNLOADED` and take one of its holder's 32 roster places again; `check_pacts`' needle covers only the
    shade. At the merge: add `@Override protected boolean joinsRosterOnStatus() { return false; }` to
    `WorkshopHandEntity`, extend `check_pacts`' needle to the hand, and add a test that a replaced hand loading while
    switched off stays off the roster. It cannot be done in this tree, which has no such method. Done in unit 3
    (`a3ee00822`, D-07's As built): the hand's override, `check_pacts`' needle and
    `aReplacedHandLoadingWhileSwitchedOffStaysOffTheRoster`.

### D-09. Combined fixes, evidence, the checkpoint and the handoff (unit 5)

- **The whole path in one server test:** discovery, evidence, the sky step, the invitation, a refusal, acceptance,
  Star-reckoning, the obligation, upkeep suspension and resumption, ending and departure.
- `tools/concordance_journey.py` taught the pact (its routes are "served once by each kind of worker", `:93`).
- **Load:** Stolas thinks once a second; one presence a pact; one pact a holder; the perch looks once a second.
  Measured on the CI runner and recorded as that runner's.
- **Client evidence** (08:169-181): each screen at ordinary and high scale and in a small window; keyboard-only
  walkthroughs; the build without optional integrations; a timed sequence of Stolas's gestures; Jade.
- **Two clients (D-S22):** attempted only with what this branch has; #467's harness is an unmerged fork contribution
  and is not imported. Otherwise recorded as not run: one client is not two (09:20).
- **Records:** `stolas-pact.md` with `CLAUDE.md:58`'s fields (tier, input producer, output consumer, costs, unlocks,
  failure behaviour, test evidence) and the art's provenance; `arcane-concordance-workers.md`, `-logistics.md`,
  `-celestial.md`, `-journey.md`, `styxhexenhammer.md`, `docs/TESTING.md`, `CHANGELOG.md`, `docs/WHAT_EXISTS.md`,
  `docs/INTEGRATION_STATUS.md`; this contract's handoff (10:133-146) and a 300 to 500 word checkpoint (10:87-121) with
  three questions (below).
- **The pull request:** the third of the stack, against `claude/chunk-c-living-workshop` (D-D1).

**As built (unit 5):** commits `90dd7a756` (the whole path), `8b821326a` (the load card), `3a0b6363d` (client evidence
and two clients), the unit review's fixes `aca4aca1c`, the merge of unit 3's compile fix `8842c3bb5`, merged into the
pact hall at `48d99c904`; then `b773913c4` and `d00fa2167`, the fixes from CI's pictures. AI-assisted implementation with Claude Code;
the model is named in the commit trailers.
- **Combined fixes:** the composer (Chunk A) draws subtitles over itself (D-P122; unit 3's R10, left to unit 5), held by
  `check_composer` (unseen: CI has no sound device); codex places unique within a category (unit 4's review, Data 2),
  held by `check_concordance`; units 1 and 4's identifiers added to `docs/ARCANE_CONCORDANCE.md` (unit 4's review, Data
  8; unit 3's were already there); the progression graph models the compact's resonance (`RESONANCE_SOURCES`) and the
  codex's compact page says where it comes from (D-P123); the records' CI statements for units 2, 3 and 4 brought up to
  date.
- **The whole path:** `ConcordanceJourneyGameTests.thePactHallFromStyxToDeparture` (D-P125): one survival player from
  Styx's hint to Stolas's departure, ended from the roster (D-07), with the workshop hand carrying one request between
  the first obligation and upkeep; `thePactHallsRecipesLoad` loads the route's nine recipes. Both passed in both server
  jobs of run 40133540741 (`mod`: "All 2572 required tests passed"; `optional integrations absent`; the log names no
  passing test one by one).
- **The journey:** the pact hall (`PACT`, nine steps, to the workshop hand), its things `tools/acquisition.py`
  `PACT_HALL` (D-P126); Celestial Attunement for the compact's resonance (D-P123); the hand's step in the Nether
  (D-P124); "each kind of worker" stays three (D-P127). `concordance_journey.py --check` holds it.
- **Load (D-P128 to D-P132):** `ConcordancePactHallLoadGameTests.loadCard`, the workshop card's twin, sharing its
  measuring parts. It measures only when Fabric's game test filter names exactly its id,
  `jugcraft-test:concordance_pact_hall_load_game_tests_load_card`; in every other run, every ordinary build included, it
  passes at its first tick and places nothing. A manual Build run with `load_card` ticked runs it in the `load-card` job
  after the workshop's card, in a server of its own, and repeats its lines as a notice (a workflow change for the
  owner's review).
  - **The hall.** Eight survival test players each hold Stolas's pact at a perch they placed, sealed under the shipped
    offer as unit 2's tests seal it (the compact left out), with four amethyst shards in its escrow. A ninth negotiates
    at a ninth perch, a visiting Stolas listening. Holder 0 reads their pact. Holder 1's roster is read every 40 ticks.
    Four workshop hands (holders 0 to 3), each at a Spirit Anchor bound by Porter Key to its holder's Courier Post, carry
    64 glowstone from a chest five blocks off whenever their holder files a request (every 400 ticks, staggered); the
    test empties each post back into its chest.
  - **Measured as the workshop's.** The same world twice, baseline first, with the nine players standing where they
    stand. Each span is a warm-up, then 6,000 ticks from the server's 100-tick ring, net of the test's own work. The
    players' actions are timed apart. A look, a think, a pact page and a roster listing are timed call by call after the
    spans.
  - **D-09's rules**, sampled every 100 ticks while measured: one presence a pact, one pact a holder, each pact looked at
    within 220 ticks (its perch looks every 20 ticks and writes every 200), the visitor listening, each hand its
    anchor's.
  - **Every item**, checked every 1,000 ticks: each workshop's 128 glowstone, each escrow's four shards, nothing on the
    ground, both ledgers' own checks.
  - Run 40133540741: passed; the hall added +0.098 ms to the median and +0.311 ms to the p95 server tick and +0 MiB
    retained heap beyond the baseline's change, within the proposed budget on all three. The figures are the CI
    runner's, not a player's hardware: [the pact hall load card](#the-pact-hall-load-card).
- **Client evidence** (D-P133 to D-P135): Stolas is photographed from his front (he is summoned facing south; the camera
  stands south of his perch); a timed sequence through his refusal (`jugcraft_stolas_refuse_seq_1` to `_4`, frozen at
  0.25, 0.65, 1.05 and 1.45 s; `REFUSAL_SEQUENCE`); the workshop hand at work in the roster's client test
  (`jugcraft_workshop_hand_at_work`). Held by `check_stolas_body` and `check_roster_screen`. `ConcordancePactClientGameTests`
  passed in shard 5, `ConcordanceRosterClientGameTests` in shard 14 and `ScreenFitClientGameTests` in shard 13 of run
  40133540741; the refusal's frames were frozen at 0.290, 0.647, 1.004 and 1.357 s. The bundle is under [Client
  evidence](#client-evidence).
- **Two clients** (D-P136): this branch had a two-client harness after all, main's `OwnerCollectionsNetworkTests` (#435,
  20 October 2026), which runs in every full client run and whose second client's log the shards upload.
  `ConcordancePactNetworkTests` copies its launcher with its own names (the owner's test is not edited) and puts a
  stranger at the holder's perch. Held by `check_pact_network`. Passed in shard 3 of run 40133540741 and in its opt-in
  job without the optional integrations: two automated clients on one machine, not two people.
- **Without the optional integrations** (D-P137): an opt-in Build input, `clients_without_optional`, and its job
  `pact-clients-without-optional`; its three classes passed in run 40133540741 ("BUILD SUCCESSFUL in 4m 38s"; Jade not
  loaded, so no Jade shot). A workflow change for the owner's review.
- **Fixes from CI's pictures** (`b773913c4`, `d00fa2167`): run 40133540741 passed, but three of its pictures showed what the
  assertions did not: Stolas's own Jade line read "Keeps an agreement with entity.minecraft." (now no line where a
  reading names no counterpart, held by `check_pacts`); the second client's arrival picture showed "Loading terrain"
  (it now waits until no screen is open); its refused picture showed Stolas's first words, not the refusal (it now
  turns to his terms, brings the answer into view and checks it is shown as refused). Run 40137136287 on `b773913c4`
  failed that check: the stranger turned to his terms after its refused Accept, and turning asks to hear them (CHOOSE),
  whose answer carries no result, so the refusal was gone and Tab never reached it (shard 3 and the opt-in job).
  `d00fa2167` turns to the terms first and accepts after their answer. Run [40140478331](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/40140478331) on `d00fa2167`: every job
  passed on the first attempt, and the three pictures, viewed, show Stolas arriving over the world, the refusal with
  its cross under his terms ("You may not seal an agreement here: it is not yours or your party's, or you could not build
  here", Accept greyed), and his Jade lines with no raw key.
- **Audits strengthened:** `check_load_card`'s and `check_pact_hall_load_card`'s TESTING.md tests require the whole id (a
  substring test passed `..._load_cards`); `check_pact_network` reads the peer-log upload in the client-shard job only,
  and requires the opt-in job, guarded, without the optional integrations, listing exactly its three classes;
  `check_stolas_body` requires the front camera's exact command twice and `check_roster_screen` the hand at work between
  the recovery and the full roster, both reading the tests without their comments since the unit review (`aca4aca1c`).
  Mutations (each logged when its commit was built, on scratch copies; each unchanged tree reported nothing): for
  `90dd7a756`, J1 to J15 (the journey), A1 to A9 (`check_acquisition`) and C3 (codex places) each reported; for
  `8b821326a`, H1 to H19 each reported; for `3a0b6363d`, N1 to N13, N15 to N21, C1 and C2 each reported, and N14 failed
  the selection unit test; for `aca4aca1c`, six comment-outs the two audits had missed each reported, and N1 to N13
  again.
- **Confirmed from source, not run:** `/tick freeze` stops game time (`mc263src` `ServerLevel.java:346-366`: the level's
  time moves only while the tick rate runs normally), so unit 3's review sweep, which ends a departure at a game time
  (`departAt`), holds a frozen departure; and GeckoLib's animation clock is the render state's `ageInTicks`
  (`EntityRenderStateMixin:46`). No test checks either by itself.
- **Records:** this page, `stolas-pact.md`, the journey, workers, logistics, celestial, progression, persistence,
  journal, delivery and Styx records, `docs/ARCANE_CONCORDANCE.md`, `docs/TESTING.md`, `docs/WHAT_EXISTS.md`,
  `docs/INTEGRATION_STATUS.md` and `CHANGELOG.md`.
- **The pull request:** [jimbozoomer-byte/jugcraft#474](https://github.com/jimbozoomer-byte/jugcraft/pull/474), against `claude/chunk-c-living-workshop` (D-D1).
- **Local checks** (each run and passed when its commit was built): `python3 -B tools/check_mod_data.py`;
  `python3 tools/concordance_journey.py --check`; `python3 scripts/check_repository.py`; `python3 tools/owner_art.py
  --check`; `python3 tools/check_icon_maps.py`; `node --test tools/world-designer/model.test.cjs`; `python3 -m unittest
  discover -s scripts/tests`; `git diff --check`; the Java parse check of the changed files; the mutations above;
  `python3 tools/select_client_tests.py --base 554d71ab1 --head HEAD` (0 of 242 client classes at `90dd7a756`; all
  242 at `8b821326a`; from `3a0b6363d` on, all 243 in 16 shards, as CI chose them in run 40133540741);
  `python3 -B tools/generate_material_data.py` (commit 1: one lang key, the codex's compact page). For these records:
  `python3 scripts/check_repository.py`, `python3 -B tools/check_mod_data.py`, `python3
  tools/concordance_journey.py --check`, `python3 -m unittest discover -s scripts/tests` and `git diff --check`. Not run
  here: `./gradlew` (no Minecraft build locally); CI is the build.

## Completion evidence

The plan's evidence (09:168-174), used in every ticket's acceptance, with the scenario rows 10:51 (a worker's death,
unload or restart while carrying), 10:52 (a pact: copy a token, change owner, end twice) and 10:53 (the sky: a time
change, two observatories).

| Code | Requirement | Tickets | Tests |
|---|---|---|---|
| E1 | "No one can bind another player" | D-01, D-02, D-04, D-05, D-07 | `onlyTheOwnersSideSealsAnAnchor`, `aStandInNeverSeals`, `noOneBindsAnotherPlayer`, `anotherPlayerCannotAcceptForTheHolder`, `anotherPlayerCannotUseTheHoldersService`, `aRequestNamingAnotherPlayersRecordIsRefused`, `theRosterEndsOnlyWhatTheReaderHolds`, `aChangeOfOwnerMovesNoPact`, `aStrangerCannotBindAHandToAnotherPlayersPost`, `aHandServesOnlyItsBeneficiaries`, `anAnchorIsBoundOnlyWhileItKeepsNoAgreement` |
| E2 | "Upkeep can suspend without hidden offline punishment" | D-02 (the rules), D-05 (Stolas) | `upkeepWarnsThenSuspendsAfterGraceAndResumes`, `aReturnAfterAbsenceGetsTheFullGrace`, `absentAndUnloadedTimeIsNeverOwed`, `aStandInNeverAnswersForUpkeep`, `theConcordanceOffHoldsEveryClock`, `aRestartWhileSuspendedStaysSuspended`; `ConcordanceJourneyGameTests.thePactHallFromStyxToDeparture` |
| E3 | "Agreement copying does not multiply benefits" | D-02, D-03, D-04, D-05 | `aCopiedAnchorGivesNothingAndReleasesNothing`, `aLegacyAnchorClaimsItsPlaceOnce`, `aCopiedPerchIsInert`, `aCopiedPerchRecallsNothing`, `aMastersRecallAndStolassShareOneClaim`, `aChangedClockGivesNoSecondRecall`, `notesNeverStandInForStolasEvidence` |
| E4 | "End/death/unload/restart returns appropriate property once" | D-01, D-02, D-04, D-05, D-06, D-07, D-08 | `aKilledShadeLeavesItsLoadOnce`, `aKilledShadeAwayFromItsAnchorDropsWhereItFell`, `anUnloadedShadeReplacedLeavesOnceAndDeliversOnce`, `endingTwiceReturnsOnce`, `thePerchHoldsNothing`, `endingReturnsTheEscrowOnce`, `aDeadHolderIsNeverHandedTheEscrow`, `aBrokenPerchLeavesThePactWithoutItsPerch`, `thePactAndItsEscrowSurviveARestart`, `killingStolasIsNeverTheFastestRoute`, `aRemovedStolasNeverEndsThePact`, `onePresencePerPact`, `aDepartingStolasIsNeverAVisitorAgain`, `endingAnOrphanReturnsItsEscrowOnce`, `recoveryNeverTakesCargoOutOfAnUnloadedContainer`, `theRosterEndsAHandAndRecoversItsCargoOnce`, `endingWithCargoAboardStrandsItOnce`, `aHandFillsACrucibleUntilItIsFull`, `aHandTakesNothingWhereItsHolderMayNot`, `aHandThatCannotFindItsWayStrandsItsCargoOnce`, `anUnloadedDestinationKeepsTheHandWaiting`, `aKilledHandStrandsItsCargoOnce`, `anAbsentHolderStopsTheHand`, `aBrokenSourceReleasesTheHandsClaim`, `aHandsClaimSurvivesARestart`, `aHandLeavesStrandedCargoBeyondItsCarry`, `aHandTakesCargoBackOnlyInsideItsArea`; `ConcordanceJourneyGameTests.thePactHallFromStyxToDeparture` |
| E5 | "Two users cannot both own a unique reserved service instance accidentally" | D-02, D-04, D-08, D-09 | `twoPlayersCannotBothSealOnePlace`, `twoNegotiationsAtOnePerchSealOnce`, `acceptingSealsOnceAndSpendsOnce`, `oneStolasPactPerHolder`, `twoHandsCannotHoldOneRequest`; `ConcordancePactNetworkTests` (two client JVMs at one perch, D-P136; passed in shard 3 of run 40133540741 and in its opt-in job without the optional integrations; two automated clients on one machine, not two people) |
| E6 | Extend `ConcordanceWorkerGameTests`, `ConcordanceLogisticsGameTests`, `ConcordanceSharingGameTests`, `ConcordancePersistenceGameTests` and the relevant client companion tests (09:172) | every ticket | the four suites above; `CompanionAutomationChecks.workshopHand` through `PeepoCompanionClientTests` (D-08); `ConcordancePactClientGameTests` (D-04, D-05, D-06); `ConcordanceRosterClientGameTests` and `ScreenFitClientGameTests` (D-07, as built in unit 3; the hand at work, D-09); `ConcordancePactNetworkTests` (D-09) |
| E7 | The checkpoint: "a complete conversation-to-service-to-departure sequence" (09:174) | D-09 | `ConcordanceJourneyGameTests.thePactHallFromStyxToDeparture`; `ConcordancePactClientGameTests` (the conversation, a recall, the departure) and `ConcordancePactNetworkTests` (the departure seen by a second client) |

## Schema review (D-08; 09:164-166, 03:177)

Written at D-08 (unit 4) on unit 1's one record. Unit 4 was built beside unit 2, not after Stolas's Star-reckoning
(D-05), so the knowledge service's column is taken from unit 2's branch as far as it is built (D-03, and D-04's server
side: `wip/chunk-d-u2` at `832129b95`, read only: `concordance/pact/stolas.json`, `PactPerchBlockEntity.sealing`,
`AgreementHolder.OnBreak` and `seat`, `PactKeeping`) and, for Star-reckoning itself, from the session's decisions D-S1,
D-S3, D-S6, D-S7 and D-S12, marked *(decided)*. Re-read at the merge of unit 2 against its D-05 as built (with its
unit review's fixes): both *(decided)* cells hold as built and are marked so; the upkeep, evidence and terms-screen
cells are brought up to date. **A proposal only:**
the shared schema is frozen at the checkpoint, not before (03:177), and nothing here is the owner's decision.

| Concern | Star-reckoning (knowledge service) | Workshop hand (cargo service) | Kept in | For the shared schema |
|---|---|---|---|---|
| Identity: record, agreement, counterpart, holder, sealed at, never transferable | agreement `jugcraft:stolas_pact`, counterpart `jugcraft:stolas` | agreement and counterpart `jugcraft:workshop_hand` | `BoundWill` (unit 1) | Shared, unchanged |
| Place | the pact perch, a marker holding nothing; broken with the Concordance on, the pact stays its holder's without its perch and Stolas leaves the world, to be seated at a new perch (`OnBreak.UNSEAT`, `seat`); broken while switched off, nothing changes | a Spirit Anchor (a hand leaves its slots alone); broken, it releases the agreement (G15; unit 2 names this `OnBreak.RELEASE`) | `Pact.place` | Shared; what breaking the block does belongs to the block (`OnBreak`) |
| Service terms | `star_reckoning`; radius 8 round the perch, all day (0 to 24000), quota 1 a day (`stolas.json`); a task is one recall (`StarReckoning` counts it once on the record) *(as built, D-S1)* | `courier`; 06:00 to 18:00; radius 12 round the anchor, applied to every place it goes (destinations and sources); quota 32, a task being one load delivered in full; at most its carry (64) a trip, read live from its definition | `Pact.service`, `window`, `doneToday` | Shared shape; the work id says what a task and the area mean; per-trip limits stay the definition's |
| Who benefits | its holder alone (`personal`, `stolas.json`) | its holder and their party (`party`), checked at each new claim against today's party; a load already claimed is finished, and a former member's open request is skipped (`IDLE`) | `Pact.beneficiaries` | Shared closed vocabulary; no list of named players (each would have to consent: the deferred ward keeper) |
| Upkeep, escrow, grace | Gentle (D-S3, amended by D-P50: a warning given before an absence keeps the grace left): one of `#jugcraft:stolas_offerings` every 7 days, a day's grace, up to 4 ahead (`stolas.json`), looked at by the perch's ticker (`PactKeeping`) | none | `Pact.upkeep`, escrow beside the record | Shared, optional |
| Suspension | by its holder, or by upkeep | by its holder; held, no new claim, and a claim it holds is finished | `Pact.hold`, `holdSince` | Shared; a hold stops new work and never strands work in progress |
| Presence | one a pact, called back at no cost while its holder is near (`PactKeeping`, D-S7) | mortal; killed, its claim is let go and the cargo stranded at the post; called back at no cost under the same record | `Pact.presence`, the roster | Shared |
| Claim on an outside authority | `recall:<pattern>` in `AstralClaims`, the one claim a master's recall makes too (`AstrolabeItem.recall`) *(as built, D-S1)* | a `CourierLedger` request: exclusive, leased 200 ticks and renewed each decision, `CLAIMED` for anyone else | the authority; the presence saves only the claim's id | **Not in the pact.** Shared rule: a presence keeps at most an authoritative claim's id, never a copy of property (03:191) |
| Reservations | none | at a source, at most what it holds less other live reservations, at most its carry; stranded cargo beyond its carry, or to be taken back beyond its area, never taken up (D-P82) | `CourierLedger` | Service-specific |
| Property in flight | none | owned by the request; released, stranded at the post | `CourierLedger` | Service-specific; shared rule: end, death, unload and restart move property only through its authority's steps |
| Recovery | the escrow, once, on ending (D-S6; unit 1's `end`) | `recover` by the requester, once (`NOT_FOUND` after), or another courier's take-up | `CourierLedger`; the escrow beside the record | Service-specific |
| Access | the holder in person, never a stand-in | the holder answering (a stand-in only with the absent-owner option, as for porters); the post usable by the holder; each source changeable by holder and requester | `Authority` | Shared through `Authority`; each service says whether a stand-in may answer |
| Exact variants | pattern ids | `ItemVariant` | ledger samples | Service-specific |
| A loaded destination | not applicable | `DESTINATION_UNLOADED`: waits, lease renewed, no step | the courier decision | Service-specific |
| A binding to a second place | none | the anchor's `post`: bound only by the anchor's owner's side, never a stand-in, to a post they may use in its level within its radius, only while no agreement is kept; at each decision the post must still stand, loaded, usable by the holder | the holder block | **Not in the pact:** block data, checked again at use; a copied block is inert (D-S9) |
| Unlock | Stolas's Regard understood and a recorded sky observation, to invite (D-03, D-04) | Stolas's Regard mastered, asked at sealing only | research | Per service |
| Fulfilment evidence | `pact_service`, recorded when the holder makes a sky observation while the pact serves, and only once a player, never for a pact without its perch (unit 2's `StarReckoning.observed`, with its unit review's stranded check; D-S12) | `worker_service` `spirit` on each delivery (the Binding Arts' distinct 3 unchanged) | research | Per service |
| Terms shown before accepting | the perch's screen: Stolas's three beats and every term before anything is spent (D-S15; D-04's `PactMenu` and client `PactScreen`, built) | the seal message (post, area, hours, loads, carry, for whom) and the codex | lang | Known gap: a hand has no terms page |
| How many, who seals, cost | one a holder; the perch's owner only (`partySeals` false); the Open Hand Compact (two amethyst shards, three feathers, 8 Astral Resonance) | any number within 32 Bound Wills and 32 roster places; the anchor's owner or their party; nothing | `Sealing` (`onePerHolder`, `partySeals`, `affords` and `pay`) | Shared switches |
| Departures and statuses | `ended`, `broken`; `upkeep_due`, `upkeep_held`, `suspended`, `disabled`; a pact without its place seated again (`SEAT_OUTCOMES`) | `ended`, `broken` (the ledger notes `agreement_ended`; killed, `worker_removed`); `outside_agreement` (hours, area, no post or its post gone), `finished`, `idle` (no beneficiary's request), `owner_offline`, `blocked_by_access`, `destination_unloaded`, `full`, `cannot_navigate`, `waiting_for_resources`, `suspended`, `disabled` | `PACT_DEPARTURES`, `Status` | Shared vocabularies; the hand adds no id, and `UNAVAILABLE_WORKER` stays unused |

**What the cargo service needed that the knowledge service did not:** an exclusive leased claim; reservations; property
in flight owned by an authority and stranded on release; one recovery; exact variants; a loaded destination; access for
both holder and requester; party beneficiaries checked again at each new claim; a binding to a second place; a per-trip
load; a task counted per load. **What neither needed:** a list of named beneficiaries, a new status or departure reason,
a second ledger, or a generic job adapter beyond the courier decision the porter and the hand share.

**Proposed shared schema, to freeze at the checkpoint:**
1. One record, unchanged by D-08: `BoundWill(id, agreement, counterpart, holder, sealedAt, transferable = false)` with
   its `Pact`: `place?`, `hold`, `holdSince`, `beneficiaries` (`personal` or `party`), `presence?`, `upkeep?`,
   `service(work, radius, from, to, quota)?`, `window` and `doneToday`; the escrow beside it and each holder's last 8
   departures.
2. S1: a service's claims and property in flight live with the authority that owns that kind of thing (`CourierLedger`,
   `AstralClaims`); the presence saves only a claim's id.
3. S2: the work defines a task and the subject of the area; the count lives on the record.
4. S3: beneficiaries are a closed vocabulary, checked at each new use against current membership; work already begun is
   finished.
5. S4: a holder block's bindings to other places are block data, checked at each use.
6. S5: terms gates stop new work only; work in progress is finished or handed back through its authority.
7. S6: death and unload never end a pact; ending releases claims through their authority, once.

**Left open at the checkpoint:** beneficiaries beyond the party; a job-adapter interface (canPlan, reserve, begin,
progress, complete, cancel, recover; 03:191), after a third service; a terms page for worker pacts; whether a courier
should skip an absent requester's request rather than wait on it (D-P81).

## Interfaces expected to change

**As built in unit 1** (D-01, D-02; see their As built notes): `concordance/spirits/AgreementHolder` (new: the shared
holder, its `Holds`, `Presence` and `Sealing`) and `spirits/PactHistory` (new, `jugcraft:pact_history`); the pure
`concordance/worker/Pacts` (new); `resource/BoundWill` (an optional `Pact` with `Hold`, `Upkeep` and `Service`, not the
ticket's `place` and `hold` fields) and `BoundWillLedger` (`get`, `update`); `spirits/BoundWills` (the pact and escrow
keys, `release` returning the escrow); `Status` (two appended reasons); `Bond`; `SpiritAnchorBlock` and
`SpiritAnchorBlockEntity`; `GatheringShadeEntity`; `HearthlingEntity`; `WorkerRoster.full`; `WorkerEntity.sign`;
`Authority.presentNear`; `CourierLedger.PLACE` (public); `Code`'s Javadoc; `Agreement`'s Javadoc; `TestPlayers`. No
`PactRecords` and no `jugcraft:pacts` (D-P1). `tools/concordance_workers.py`, `tools/concordance_diagnostics.py` and
`tools/check_mod_data.py` (`check_workers`, `check_pacts`, `AUTHORITY_ROUTES`, `ENTRY_BY_ENTRY`); the lang and the
spirits codex entry regenerated. No payload, block, item, entity or art.

**As built in unit 2** (D-03 to D-05; see their As built notes): new `concordance/spirits/` `StolasRegard`,
`PactPerchBlock`, `PactPerchBlockEntity`, `StolasEntity`, `PactMenu`, `PactRequestPayload`, `PactStatePayload`,
`PactSessions`, `PactCompact`, `PactKeeping` and `StarReckoning`; new `concordance/worker/` `PactDefinition`,
`PactParser` and `PactCatalog` (the `pact` data kind on `ConcordanceRules`); new client `PactClient` and `PactScreen`;
edited `AgreementHolder` (`OnBreak`, `seat`, `stranded`), `worker/Pacts` (`pactWith`, `Reckoning`, `serves`), `Workers`,
`ConcordanceRules`, `sky/AstrolabeItem` (the one `recall`, `mayRecall`), `sky/ObservatoryBlockEntity`, `Journal`,
`Examination`, `styx/Styxhexenhammer`, `styx/StyxConservatory` (`covers`), `ConcordanceClient` and `WorkerClient`.
Network: `jugcraft:concordance_pact_request` and `jugcraft:concordance_pact_state`, and the perch's menu type. Data: the
research, practice gate, tags, advancements, Modonomicon data and two codex entries (`binding/stolas_regard`,
`binding/stolas_pact`, not `binding/stolas.json`), `concordance/pact/stolas.json`, the perch's block, item, models,
icon, recipe and loot; `values.json` and the survival registry snapshot. Assets: Stolas's provisional GeckoLib model,
animations and skin (the owner's Flashjack rig). Tools: `tools/concordance_pacts.py` (new), `tools/styx.py`,
`tools/concordance_progression.py`, `tools/concordance_journal.py`, `tools/concordance_workers.py`,
`tools/concordance_journey.py`, `tools/concordance_equivalence.py`, `tools/concordance_worker_art.py`,
`tools/concordance_celestial.py`, `tools/concordance_economy.py`, `tools/owner_art.py`, `tools/select_client_tests.py`
and `tools/check_mod_data.py` (`check_stolas_regard`, `check_pacts`, `check_pact_screen`, `check_geckolib`,
`check_economy`, `AUTHORITY_ROUTES`). No sound, `sounds.json`, Jade line of Stolas's own or roster payload yet (units
3 and 4).

**As built in unit 4** (D-08; see its As built note): `concordance/spirits/CourierWork` (new: the porter's courier
decision and its `Terms`) and `WorkshopHandEntity` (new); `logistics/Logistics.next(Place, long, Predicate<Request>)`
and `reserve(..., most, ...)`; `courier/CourierLedger.reserve(..., most, ...)`; `ClockworkPorterEntity` (its decision
moved into `CourierWork`, its terms kept); `SpiritAnchorBlockEntity`'s `post`, `handSealing` and `call` (a
`WorkerEntity<?>`: a shade or a hand); `SpiritAnchorBlock.useItemOn` and `sealHand`; `PorterKeyItem.bind` and `Binding`;
`Workers.WORKSHOP_HAND`; `WorkerParser.WORK` (`courier`) and `WorkerDefinition`'s Javadoc; `client/WorkerClient` (the
hand drawn by GeckoLib); `compat/jade/WorkerDataProvider` (its carried count). `tools/concordance_workers.py`,
`tools/concordance_logistics.py`, `tools/concordance_worker_models.py` and `tools/concordance_worker_art.py`;
`tools/check_mod_data.py` (`check_hand`, new; `check_workers`' class map; `check_logistics`; `AUTHORITY_ROUTES`;
`UNIT4_TESTS` and `PORTER_LOGISTICS_TESTS`). Generated: `concordance/worker/workshop_hand.json`, the codex entry
`entries/binding/workshop_hand.json`, the hand's GeckoLib model, animations and sheet, lang. Tests: `T/WorkshopHands`
(new fixture) and the four suites; `local/peepo/CompanionAutomationChecks`. New registration: the entity
`jugcraft:workshop_hand`. No `Agreement` field, payload, block, item or saved-data id. The unit review's fixes add
`WorkshopHandEntity.WORK`, make `CourierWork.Terms.postGone()` return a `Status` (both couriers' terms follow), pass
the definition to `SpiritAnchorBlock.sealHand` and give the hand's model a `pack_lid` bone.

**As built in unit 3** (D-06, D-07; see their As built notes): new `tools/stolas_body.py`, `spirits/PactReading`,
`compat/jade/PactDataProvider`, `client/KeepFirstConfirmScreen`, `spirits/Roster`, `RosterRequestPayload`
(`jugcraft:concordance_roster_request`), `RosterStatePayload` (`jugcraft:concordance_roster_state`),
`client/RosterClient`, `client/RosterScreen`, `tools/concordance_roster.py`, and the audits `check_stolas_body`,
`check_roster` and `check_roster_screen`; edited `StolasEntity` (loops, gestures, listening, departure, cues),
`PactPerchBlockEntity` (`stolas`, `heard`, `react`; `unbound` and `summon`), `PactMenu` (two lines), `AgreementHolder.peek`,
`Workers` (one line), `JugcraftConcordance` (one line), `StolasRegard.report`, `WorkshopHandEntity`
(`joinsRosterOnStatus`), `PactScreen`, `WorkerClient`, `JugcraftJadePlugin`, `WorkerDataProvider`,
`JugcraftJadeClient`, `ConcordanceClient`, `tools/owner_art.py` (`ADAPTED`), `tools/concordance_pacts.py`,
`tools/concordance.py`, `tools/encyclopedia.py`, `tools/select_client_tests.py` and `tools/check_mod_data.py`
(`check_geckolib`, `check_pact_screen`, `check_pacts`, `check_rime_strata_assets`). Generated: Stolas's skin, glowmask
and animation file, `sounds.json` (six `jugcraft:stolas.*` events) and lang. Tests: the Worker, Persistence, Sharing,
Logistics and Journal suites, `ConcordancePactClientGameTests`, the new `ConcordanceRosterClientGameTests`,
`ScreenFitClientGameTests`, the gametest `fabric.mod.json` and `scripts/tests/test_client_test_selection.py`. New
registrations: the six sound events, two payload types and a key mapping. No saved-data key, block, item or entity.

**Existing files edited:**

| Area | Files |
|---|---|
| Records and spirits | `concordance/resource/BoundWill.java` (optional `place`, `hold`; a six-argument constructor kept); `concordance/spirits/BoundWills.java` (the codec's optional fields); `Status.java` (two appended reasons); `WorkerParser.java` (`WORK`, D-08); `Bond.java`; `SpiritAnchorBlock.java` and `SpiritAnchorBlockEntity.java` (the extracted holder, owner, outcomes, `post`); `GatheringShadeEntity.java` (removal, delivery on departure); `HearthlingEntity.java`; `ClockworkPorterEntity.java` (the extracted courier decision); `WorkerRoster.java` (if the outcome needs a capacity query) |
| Sky, Styx, journal | `concordance/sky/AstrolabeItem.java` (the shared recall); `styx/Styxhexenhammer.java` (the hint); `concordance/journal/Journal.java` (the pacts section) |
| Jade | `compat/jade/JugcraftJadePlugin.java`, `WorkerDataProvider.java`; `client/compat/JugcraftJadeClient.java` |
| Client | `JugcraftClient.java` (registrations: renderer, screens, key); `WorkerClient.java` (the hand drawn by GeckoLib, as `check_workers` requires, `tools/check_mod_data.py:17918-17919`) |

**New code and data:**
- Java: the shared agreement holder and pure `Pacts` rules (beside `Agreement`); `PactHistory` (all three built in unit
  1; `PactRecords` is not built: the pact is on the Bound Will, D-P1); the perch
  block and block entity; the pact definition and its parser; `PactMenu`, `PactRequestPayload`, `PactStatePayload`; the
  roster payloads; `StolasEntity` and `WorkshopHandEntity` in `concordance/spirits/`; the shared courier helper.
- Client: `PactScreen`, `RosterScreen`, Stolas's and the hand's renderers.
- Data: `concordance/pact/stolas.json`; `concordance/research/stolas_regard.json`; `concordance/practice/pact_service.json`;
  `concordance/worker/workshop_hand.json`; the tags `stolas_specimens` (joined to `concordance_specimens`) and
  `stolas_offerings`; four advancements; the Modonomicon research facts, nodes and hooks; the codex entry
  `entries/binding/stolas.json` (superseded in unit 2 by `binding/stolas_regard.json` and `binding/stolas_pact.json`,
  D-P18; and the hand's); the perch's recipe, loot table, blockstate and models; `equivalence/excluded`.
- Assets: Stolas's and the hand's GeckoLib models, animations and textures; the perch's art and icon; `sounds.json`
  entries through the generator; lang through the generators.
- Tools: a new `tools/concordance_pacts.py` (definition, research, tags, lang, sounds, codex), merged by
  `tools/concordance.py`'s `write_all` (`:1861`) and its `SOUND_EVENTS`/`SOUND_FILES` (`:1529-1561`);
  `tools/concordance_workers.py` (`WORK`, `STATUSES`, `WORKERS`, messages); `tools/concordance_worker_models.py` (the
  hand's body); `tools/styx.py` (the hint lines); `tools/concordance_journey.py`; `tools/owner_art.py` (a table for what
  is copied from the library); `tools/select_client_tests.py` (`WEIGHTS`).
- Audits (`tools/check_mod_data.py`): `check_workers` (`WORK`, the class map, `STATUSES`, the new kind lang);
  `check_pacts` (new); `check_geckolib` (triggerable clips); `check_persistence` (`ENTRY_BY_ENTRY`); the pure-package
  list if the rules class is pure.
- Regenerated: `alchemy/values.json` and `docs/audits/survival-item-registry.json` for the new items (the route CC's
  Where to resume, item 1, records).
- Tests: the suites named above; `TestPlayers` (`survivor`, `again`); `ConcordancePactClientGameTests`; the gametest
  `fabric.mod.json`.
- Network: two new payload pairs (the pact menu's, the roster's); a menu type for the perch.
- Saved: optional Bound Will keys (as built, `pact` and `escrow`); `jugcraft:pact_history` (no `jugcraft:pacts`, D-P1);
  the anchor's optional `owner` (built in unit 1) and `post`;
  the bond's optional `seen_played` (built in unit 1); the perch's block entity; the entities' keys. Every stamp stays at 1.

## Dependencies and order

1. D-00: this contract.
2. Unit 1: D-01, then D-02 (the holder is extracted once, in D-02; D-02's copy check is shown on the anchor). As built,
   the holder was extracted once in D-01's commit and extended in D-02's; the unit lands as one.
3. Unit 2: D-03 (data and the gate file; nothing spent), then D-04 (D-02's holder and records; D-S1 and D-S3 for the
   terms page, critique 21), then D-05 (D-02's upkeep rules; D-03's gate).
4. Unit 3: D-06 after D-04's entity; D-07 after D-02 (states) and D-05 (actions).
5. Unit 4: D-08 only after Stolas works (09:164) and Stolas's Regard can be mastered, using C-06 from #466. As built,
   unit 4 was built beside unit 2 from unit 1's head `97e1facd4` on the local `wip/chunk-d-u4`, not after it: its gate
   names unit 2's research id only, its tests grant that id, and the two join in a merge (D-08's As built). The player's
   order is unchanged: the hand still needs Stolas's Regard mastered.
6. Unit 5: D-09.

The smallest first step is D-01: no new id, block, entity or art; it closes G1 and G3 to G8 on systems already shipped
(G6's roster action waits for D-07), and D-02 closes G2, G9, G10 and G14 in the same unit.

## Risks

- **Base:** D stacks on two unmerged pull requests; a review change to #465 or #466 means a merge. `diagnostic/`, C-06
  and `TestPlayers.mock` exist only on Chunk C's branch (#466).
- **Styx is not guaranteed** (old saves, worlds without a fresh town, a resident killed for good). The non-Styx route
  is required, not optional (`CLAUDE.md:57`).
- **Route depth:** in the progression graph, the Binding Arts, Verdant Husbandry, the Assay and, for the compact's
  resonance, Celestial Attunement understood, plus one recorded observation (which asks no research) and the Nether once
  for the workshop hand's anchor; in code only the Binding Arts gate the entry (D-03), and Celestial Attunement gates
  only keeping an observatory (D-P123). `concordance_journey.py` finds the route reachable; its five understood entries in
  five traditions make a generalist Adept (open at the checkpoint).
- **Balance (D-S1):** non-masters gain recall of any pattern they observed; the Harvest Moon (Hero of the Village) can
  then be recalled daily and the Winter Crown every other day, within the one-a-day quota. Tested and stated; the owner
  may narrow it at the checkpoint.
- **Gentle upkeep's loophole:** a holder who leaves before each grace ends and returns later is warned again with a
  full grace each time, and is never suspended. Accepted as gentle (the offering is a renewable flower); the checkpoint's
  strictness question covers it. Unit 2's unit review showed that a minute away (beyond 64 blocks, or logged out) at
  least every 20 minutes was enough; D-P50 proposes closing it (the grace left carries over, its end moved on by the
  time away). Suspending, recalling and resuming once a day stays open (the checkpoint's question 2). The other way, a
  gap between looks of at most `ABSENT_TICKS` (1,200 ticks, a minute) is not an absence, so a due date or grace end
  inside it counts at the next look (D-P2 as built).
- **A crash and the legacy suspension (D-P4, as built):** the anchor's chunk and the Bound Wills are saved at different
  times, so a crash between them can lose a legacy claim after the anchor's flag was written false; the next verify
  claims the record again with no hold, and a suspension made before Chunk D is lost once (its holder can suspend
  again). Nothing else is lost.
- **Practice side effects:** the Conclave hears every recorded practice (`ConcordanceProgress.java:101-127`;
  `starbound/Starbound.java:104`). `pact_service` is recorded once a pact, so its effect on Conclave projects is bounded;
  no commission names it.
- **Audit pins:** `check_workers` fixes the kinds, the class map and "distinct 3"; `check_geckolib` checks only loops and
  assumes box UV (Flashjack's cubes are box UV; whether its regions overlap is not yet checked).
- **Test players:** vanilla's mock is creative whatever its game mode is set to; spending tests that use it would pass
  without spending. `TestPlayers.survivor` must be used, and checked to report survival.
- **Copies by operators:** `/clone`, structure blocks or pick-block can copy a block entity: D makes the copy inert but
  cannot stop operator tools duplicating items elsewhere. Escrow in a world record cannot be copied this way.
- **The legacy claim (D-P3, as built in unit 1):** a record from before Chunk D is claimed only by the block whose
  agreement's own sealed coordinates are its place, and only while its spirit, if loaded, is seated there; a copy at
  another place never claims, whichever loads first, and stays inert. Tested (`aLegacyAnchorClaimsItsPlaceOnce`);
  documented.
- **Unit 1 has not run (as built):** none of its Java has been compiled against Minecraft and none of its 29 server game
  tests has run; CI is their first compile. Two of them teleport a test player into the Nether (the repository's
  first), and the familiar and upkeep tests rely on the test world's player and statistics files being written and
  read back (`TestPlayers.leave` and `again`). If CI shows the Nether is unavailable, the Nether steps can be dropped:
  the rules are still shown 20 and 70 blocks away. Since run: with its unit review's fixes and 26.3's white wool, CI run
  40067977680 on `da185dd0d` passed them all, the Nether steps among them.
- **Unit 2 has not run (as built):** none of its 46 server game tests or its client test has run; CI is their first
  compile. Points to watch there: the client test's Accept near spawn (if `TownProtection` refuses it as not permitted
  in a fresh client-test world, its stand moves away from spawn); the explosion in `thePerchHoldsNothing` (a power-3
  blast against the perch's resistance of 3.0); the pinned GeckoLib's `thenPlayAndHold` and Flashjack's Molang loops;
  the test-only listener counting recorded `pact_service` practices stays registered for the test server's life (it only
  reads). The full list is D-05's "Not confirmed until CI". Since run: run 40090825848 failed two of its tests in both
  server jobs, each a defect of the test (fixed in `8a3425f9e`); run 40094972598 passed every job, its client test in
  shard 12.
- **Unit 2's balance (D-S1):** a holder who is not a master may recall the Harvest Moon (Hero of the Village, with its
  trade discounts) daily and the Winter Crown every other day. The once-a-day quota is the only hard limit: resonance
  is paid from any carried astrolabe that holds enough, claims are each keeper's own, and astrolabes can be stockpiled,
  handed over or filled at a keeper's observatory by party members, so one keeper's averages (about 2.09 resonance a
  day from the year-round patterns, 3.59 with the Winter Crown, which is observable all year with seasons off) bound
  only a player who relies on their own observatory alone; a master holding the pact also gains a free choice of any
  observed pattern. A checkpoint question, stated in [stolas-pact.md](stolas-pact.md#costs-balance-and-automation).
- **The Spirit Anchor keeps its own rules:** hoppers below still take its deliveries, and an explosion still ends a
  shade's agreement and drops its contents once (G15). Claims shield explosions; unclaimed land does not. Changing that
  is not in this chunk.
- **Death-property pull requests (#461, #453):** the escrow goes only to a living holder; the tests run again once
  either merges.
- **UI limits:** the encyclopedia is full; GuiLib has no narration *(survey)*; the repository has no conversation,
  confirmation or toast precedent of its own *(survey)*; whether a GeckoLib entity draws as a roster portrait is
  undetermined.
- **Assets:** the Flashjack-derived look may be rejected at the checkpoint; the raven's call is shared with
  `beast_crow`; later cohorts meet a name collision (Goetia's Murmur against `beast_murmur` and the Murmur SMG, `en_us.json:18957, 10232`).
  The licence metadata found with the source is kept with the provenance.
- **Shared hot files:** above; `sounds.json` contention with #470, `ScreenKit` with #469 and #471.
- **Two clients (as built):** two client JVMs on one CI runner (`ConcordancePactNetworkTests`, D-P136), offline names on
  a loopback server: not two people, two accounts or two machines, which stay not run. The second JVM takes 2 GiB beside
  the first client's 4 GiB, as the owner's network test does in every full run; a runner without room fails the class
  with the second client's log tail. It passed in shard 3 of run 40133540741 and in its opt-in job.
- **Unit 4 has not run (as built):** none of its Java has been compiled against Minecraft, and none of its 21 server
  game tests or its client companion check has run; CI is their first compile. It was built beside unit 2 and has not
  met it: the merge (D-08's As built) is where `check_hand` first checks the Regard's research. Since run: CI compiled
  it in runs 40079227985 and 40082852322, which failed the twenty hand tests at a test helper's comparison; with the
  helper fixed, run 40085012234 on `86c4e7127` passed all 21 and the client companion check.
- **Holds finish claims (D-P75):** a suspended hand, or one past its hours or its day's loads, finishes the load it has
  claimed. A claim on a full destination stays held until there is room or the holder ends the agreement; it blocks only
  its own request, and other couriers still claim the rest of the post's queue.
- **An absent requester at the head of the queue** holds the hand with `OWNER_OFFLINE`, as it holds porters
  (`arcane-concordance-logistics.md`, Multiplayer and persistence); not changed (D-P81).
- **Crucibles outside the area:** a crucible files at a post up to 16 blocks away (`Couriers.FILING_REACH`), so it may
  lie outside a hand's 12 blocks of its anchor; its request is refused with `OUTSIDE_AGREEMENT` and waits for a porter.
- **Carry is live:** read from the definition, not the sealed terms (as for the shade); a data pack's change alters a
  sealed hand's load a trip, and cargo of its own stranded at its post beyond a lowered carry it leaves to porters or
  its requester (D-P82).
- **Cargo a hand leaves (D-P82):** at a post served only by hands, cargo stranded beyond their carry (a killed porter's)
  or to be taken back to a container beyond their area waits for its requester's one recovery; it is counted in the
  ledger and nothing is lost.
- **The hand's balance:** no fuel, repair or upkeep; at most 32 loads of 64, 2,048 items, a day a hand (no stranded
  load beyond its carry is taken up, D-P82); nothing is created. For the owner's review at the checkpoint.
- **The Porter Key on an anchor:** used on a Spirit Anchor, the key now binds it (sneaking: unbinds it). Before, it
  acted as an empty hand there (the anchor's own use) and, sneaking, took the anchor for a route's container.
- **Simulated failures in unit 4's tests:** the path test sets SmartBrainLib's `CANT_REACH_WALK_TARGET_SINCE` directly
  (asserting first that the memory slot exists); death is `remove(KILLED)`; the restart is codec round trips, an unload
  (`UNLOADED_TO_CHUNK`) and the entity added again.
- **Leases across tests:** each new test calls `lapse` with far-future times on the shared courier ledger, as the 15
  existing logistics tests already do. `ConcordanceJourneyGameTests` (900 ticks, a real porter) and
  `ConcordanceWorkshopLoadGameTests` hold claims across ticks, so a lapse from a test running beside them can strand
  their cargo mid-flight; it is taken up again at the post, which those tests tolerate. Game-test positions are not
  reused between batches (`clearOnBatch` false), so a leftover far-future lease cannot reach a later test's request.
- **A far-reaching test area:** `anUnloadedDestinationKeepsTheHandWaiting` seals with a radius of 100,008, kept until
  that test's own end.
- **The companion check** grants research to the client player in the disposable Peepo world.
- **Client CI cost:** `WorkshopHands.java` and `CompanionAutomationChecks.java` are shared files, so every client class
  runs (239).
- **Outside Chunk D:** main's `PeepoPopulationClientTests` cow-breeding wait (the flake Chunk C's runs met) timed out
  in client shard 15 of run 40085012234's first attempt, though #357's fix (`40381834a`) is on this branch; the shard's
  re-run passed. Chunk D does not edit that test or the Peepo code.
- **Unit 3 has not run (as built):** nothing of it has been compiled against Minecraft, and none of its 17 server game
  tests, its new client class or its additions to `ConcordancePactClientGameTests` and `ScreenFitClientGameTests` has
  run; CI is their first compile. Generic inference (the 14-field `Row` codec with `UUIDUtil.STREAM_CODEC`, the Jade
  providers, nested lambdas in the client tests' `computeOnClient`) may need small fixes at the first build. Since run:
  run 40120355940 on `554d71ab1` failed to compile at one such call (`LOGGER.info` with a generic `computeOnClient`
  result, two errors; fixed in `040d73ece`); run 40121823509 on `040d73ece` passed every job, its 18 server game tests
  and client tests among them, after one re-run of `optional integrations absent` for `TurkeyGameTests.hensLayEggs`
  (#112, untouched by Chunk D; it passed in that run's `mod` job).
- **Its base (resolved):** unit 3 needed `55b3ca698` (`ScreenKit.Fitted`) and units 2 and 4 together; it was built on
  `92868ff82`, which has all three, so the specification's fallback (no `ScreenKit.Fitted`, no screen fit line) was not
  used. Unit 2's commits after `ee82c1dc7`, where its specification read unit 2 (its records `8f0635d9e` and its unit
  review's fixes `f6ef0b85d`), are in that base, so unit 3 was built on them.
- **Gesture timing:** each telling-pose window is at least half a second and the client test freezes inside it; CI
  load could drift a clip, and the test's log prints each frozen time.
- **Departure lingering** changes what "gone" means for every caller: a missed `isRemoved()` on a Stolas fails a test,
  not a player (the 40 ticks are never saved). Unit 2's assertions were changed to `gone()`.
- **Portraits** through GeckoLib in the GUI are unverified: the Spirit Anchor's icon is drawn only when the creature
  cannot be made, and whether GeckoLib draws in the GUI is shown only by the shots (the client test's `drawn` records
  the attempt, not pixels).
- **Revision collisions** (`Objects.hash`): an action could pass on a stale view, but every action re-checks the holder
  and the record, so at worst it acts on the current state.
- **Shared gates:** a roster action within 4 ticks of a Spirit Anchor's use (or 10 of a logistics command) answers "One
  moment".
- **Truncation:** 80 possible rows become 64 (`more` says so); the journal and the commands list everything.
- **Sound on CI:** the client runners have no sound device, so Stolas's subtitles are proven defined and fired there,
  never seen (D-P104); an OpenAL null driver is left to unit 5 or the owner. Unit 5 added none.
- **Open pull request #467** touches `JugcraftJadeClient`, which unit 3 edits; refresh before landing.
- **Evidence timing (D-P134, D-P135):** the refusal frames and the hand's frozen carry depend on CI load; each frozen
  time and state is logged, and an assertion, never a fixed delay, decides.
- **Stolas's facing:** summoned at yaw 0, he faces south whoever speaks to him (`PactPerchBlockEntity.summon`); only the
  evidence's camera is moved (D-P133). Turning him toward his speaker needs a client run to judge (checkpoint question 1).
- **Pictures the assertions pass:** run 40133540741 passed while three of its pictures were wrong (Stolas's own Jade
  line, the second client's arrival and refusal); `b773913c4` and `d00fa2167` change them and assert the refusal shown
  (run 40140478331: every job passed; the pictures viewed and as intended). A screenshot is evidence only once someone
  has looked at it.
- **The pact hall's load card (D-P128 to D-P132):**
  - One run on one shared runner, baseline first, so JIT warmth favours the hall's span; the drift line reports this and does not remove it.
  - Nine server-side test players, not clients: they do not move, the card acknowledges their chunk batches, payloads go only to clients that declared them (sized as such, never sent), and Jade's replies are not measured. Only holder 0's packets are sized; the other eight are counted.
  - The players' own actions are out of the net figures (timed apart).
  - No offering falls due (the shipped period is 7 game days), so paying, warning and suspending looks are not timed here.
  - A hand walks five blocks to its chest; no earlier test makes a courier walk beyond its reach. In run 40133540741 each hand carried 15 loads, so the chest stayed where it is.
  - A warm-up longer than about 5,000 ticks would bring a hand near its quota of 32 loads a game day; the card reports how often a hand was at its quota (0 in run 40133540741).
  - The figures are the CI runner's, not a player's hardware.

## Explicit don'ts

1. Do not generate 71 shallow variations, or any second Goetia spirit, before the checkpoint (09:174; 03:177). The
   workshop hand is a worker, not a Goetia identity.
2. Do not build a second UUID system, ledger, roster, courier ledger, worker base or permission model: extend
   `BoundWills`, `WorkerRoster`, `CourierLedger`, `Authority`, `Saved` and `RateGate` (03:142; 03:181).
3. Do not copy `Bond.neglected` into upkeep. Owe nothing for absence or unloaded time, accumulate no debt, never look at
   upkeep through a stand-in (03:146).
4. Do not make combat the way a pact ends; killing Stolas or a hand is never the fastest route (03:146; 02:151).
5. No unlimited ore locator, no free rare resources, no resonance outside the ledger, no revealing all research, no
   moving another player's cargo (03:137; 03:155).
6. Do not grant research for talking. Do not accept notes as personal evidence (03:132).
7. Do not record `worker_service` for Stolas, and do not add a fourth worker kind.
8. No chat commands, vanilla Dialog or mixin as the interface's protocol (12:54); no client totals, player ids or free
   text in requests (08:99); no menu slots for deposits.
9. Do not put actions in the encyclopedia, and do not ship an empty Pacts page (08:20).
10. Do not edit Styx's frozen blueprints; the perch is refused in his protected district.
11. Do not make a Bound Will transferable or give `transfer` a route; no steward bypass (02:170). Threadwax and Sever
    never touch pacts (03:38; 02:83).
12. Pact items stay out of equivalence; consumed offerings stay in it. Do not fix duplication with cooldowns (06:158;
    06:164).
13. No chunk loading (MP:54) and no dimension crossing (`WorkerEntity.java:214-218`; `check_workers`,
    `tools/check_mod_data.py:17925-17931`).
14. No edits under `src/main/java/local/peepo` (#452); do not replace Peepo with clones (03:181). The gametest companion
    checks may be extended.
15. Do not execute or bulk-import from the owner library; keep the originals; do not reuse a registered creature's
    identity (the crow, the courier bird, the Ars familiars) as Stolas; do not import a modern adaptation's design
    (03:165; `styxhexenhammer.md:37`).
16. Do not hand-edit generated data, `sounds.json` or lang; change the generators.
17. Do not mark a session decision as the owner's, count one client as two, or call the chunk complete because tests
    compile (10:162; 09:20; 09:315).

## Unit 5: evidence and handoff (D-09)

### Candidate and CI

- Candidate: `48d99c904` on `claude/chunk-d-pact-hall` (units 1 to 5 on the base `779851cdb`, with Chunk C's merge
  `55b3ca698`); pull request [jimbozoomer-byte/jugcraft#474](https://github.com/jimbozoomer-byte/jugcraft/pull/474), the third of the stack, against `claude/chunk-c-living-workshop` (D-D1). CI run
  [40133540741](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/40133540741), a workflow_dispatch of the Build
  workflow on `claude/chunk-d-pact-hall` with every client class and both opt-in inputs (`load_card`,
  `clients_without_optional`): every job passed on the first attempt.
- After it, `b773913c4` fixed three defects its pictures showed (under Combined fixes); its run, 40137136287, failed
  the second client's new refusal check, fixed in `d00fa2167`, whose run [40140478331](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/40140478331) passed every job on the first
  attempt (with both opt-in inputs); the three pictures, viewed there, show the fixes.
- The jobs of run 40133540741, each passed on its first attempt: `mod` (the build and the server game tests: "All 2572
  required tests passed"); `optional integrations absent`; `Concordance load cards` (the workshop's card, then the pact
  hall's); `choose client tests` (every class, 243, in 16 shards); `pact clients without optional integrations` ("BUILD
  SUCCESSFUL in 4m 38s"); client shards 0 to 15, the classes of Client evidence in shards 3
  (`ConcordancePactNetworkTests`), 5 (`ConcordancePactClientGameTests`), 13 (`ScreenFitClientGameTests`) and 14
  (`ConcordanceRosterClientGameTests`), main's `OwnerCollectionsNetworkTests` in shard 12; and `client`. The jobs'
  durations are not recorded here.
- Runs on the way: 40066789750 (`55b3ca698`: stopped at 18 compile errors, `Items.WHITE_WOOL`, fixed in `da185dd0d`);
  40067977680 (`da185dd0d`, unit 1: every job passed); 40079227985 (`df3006850`, unit 4 merged: both server jobs failed
  its twenty hand tests at their first step); 40082852322 (`ca2957291`: both server jobs failed the same way; cancelled with
  four client shards unfinished); 40085012234 (`86c4e7127`: every job passed after one client shard's re-run for main's
  cow-breeding flake); 40090825848 (`92868ff82`, units 1, 2 and 4: every client shard passed; both server jobs failed
  `aBrokenPactDefinitionIsReported` and `talkingGrantsNoResearch`, test defects fixed in `8a3425f9e`); 40094972598
  (`8a3425f9e`: every job passed, first attempt); 40120355940 (`554d71ab1`, unit 3 merged: failed to compile, two errors
  at `ConcordanceRosterClientGameTests.java:255`, where `LOGGER.info` with a generic `computeOnClient` result was
  ambiguous between `info(String, Object...)` and `info(String, Throwable)`; every job failed; fixed in `040d73ece`);
  40121823509 (`040d73ece`, units 1 to 4: every job passed, unit 3's 18 server game tests and its client tests run there
  first; `optional integrations absent` failed once on main's `jugcraft-test:turkey_game_tests_hens_lay_eggs` and passed
  on its one re-run, job 120963228067); 40133540741 (`48d99c904`, units 1 to 5: every job passed on the first attempt);
  40137136287 (`b773913c4`, the fixes from its pictures: failed; `ConcordancePactNetworkTests`, "Tab never reached
  result", in shard 3 and the opt-in job, fixed in `d00fa2167`; also main's `PeepoPopulationClientTests` cow-breeding
  timeout in shard 12 and `FlailClientGameTests` in shard 10 ("the flail's ball sank into the holder's body by 0.076
  blocks"), both outside Chunk D and passing in the runs before and after); 40140478331 (`d00fa2167`: every job passed on
  the first attempt).

### Combined fixes

- The composer (Chunk A) drew the HUD's subtitles under its own fill; it is now an in-game screen that draws them over
  itself (D-P122), as unit 3 made the pact and roster screens (unit 3's R10). Unseen: CI has no sound device, and
  `check_composer` reads the code.
- Codex places: no two entries of a category may share one (`check_concordance`; unit 4's review, Data 2).
- `docs/ARCANE_CONCORDANCE.md` lists units 1 and 4's identifiers (unit 4's review, Data 8; unit 3's were already
  there).
- The progression graph models the Open Hand Compact's resonance, which only an observatory's keeper gathers, and the
  codex's compact page says so (D-P123).
- Stolas photographed from his front, a timed refusal and the hand at work (D-P133 to D-P135; below).
- Two audits made exact (D-09's As built), and since the unit review (`aca4aca1c`) they read the test classes without
  their comments, so a guarded step commented out is reported.
- Unit 3's roster client test did not compile: `LOGGER.info` with a generic `computeOnClient` result was ambiguous (run
  40120355940 on `554d71ab1`, two errors, every job failed). `040d73ece` reads the rows into a `String` first; its run
  40121823509 passed every job. Merged into unit 5 at `8842c3bb5`.
- Three defects seen in run 40133540741's pictures, which its assertions had passed, fixed in `b773913c4` and
  `d00fa2167` (run 40140478331: every job passed; the fixed pictures viewed there and as intended):
  - `jugcraft_pact_stolas_jade` read "Keeps an agreement with entity.minecraft.": Stolas's own reading names no
    counterpart (he is it), and an empty id parsed as `minecraft:` with an empty path. An empty counterpart now shows
    no line; the perch's line, which names Stolas, is unchanged; `check_pacts` reports the old form.
  - `jugcraft_pact_peer_arrival` showed the second client's "Loading terrain" screen: the level was drawn, but the
    loading screen stays until the client has compiled its own section. The stranger now waits until no screen is open
    before telling the holder it is watching.
  - `jugcraft_pact_peer_refused` showed Stolas's first words, where the refusal does not appear. The stranger now turns
    to his terms first, sends its Accept after their answer (turning asks to hear them, and that answer carries no
    result: `d00fa2167`, after run 40137136287 lost the refusal that way), brings the refusal into view and checks it is
    shown as refused before the picture.

### The demonstration (E1 to E7)

"Server" means a game-test server with test players; "client" one real client on the game's own dedicated server; the
two-client test is two client JVMs on one CI runner. Every test named passed in run 40133540741 unless marked otherwise.

| Code | Evidence |
|---|---|
| E1 | the tests in Completion evidence; across the network, `ConcordancePactNetworkTests` (`not_permitted`, `not_holder`, `not_listed`) |
| E2 | the tests in Completion evidence; the whole path's upkeep |
| E3 | the tests in Completion evidence |
| E4 | the tests in Completion evidence; the whole path's end (the escrow back once); `ConcordancePactClientGameTests` (the offering back once); the roster class (a recovery once; the hand's delivery once) |
| E5 | the tests in Completion evidence; `ConcordancePactNetworkTests` (two automated clients on one machine, not two people) |
| E6 | the four suites; `ConcordancePactClientGameTests`, `ConcordanceRosterClientGameTests`, `CompanionAutomationChecks.workshopHand` |
| E7 | `ConcordanceJourneyGameTests.thePactHallFromStyxToDeparture`; the pact client class's conversation, recall and departure; the second client's arrival and departure shots (the arrival's picture defective in this run, fixed in `b773913c4` and `d00fa2167`, viewed in run 40140478331: Stolas arriving over the world) |

### Client evidence

Run 40133540741. What was viewed for these records: the 480×270 previews the client jobs print in their logs (the
screenshot steps of `.github/workflows/build.yml`), extracted from the logs; the full-size images are in the run's
`client-screenshots-<shard>` and `client-screenshots-without-optional` artifacts (the second client's under
`owner-network-evidence/.../peer-profile/screenshots/`). A screenshot shows presentation; the tests' assertions are the
evidence of behaviour. Each class named passed in its shard of that run.

| 08 item | Shots (class, shard) | What the test asserts | Seen |
|---|---|---|---|
| Each workspace at ordinary scale | `jugcraft_pact_conversation`, `jugcraft_pact_terms`, `jugcraft_pact_kept` (tick), `jugcraft_pact_refused` (cross), `jugcraft_pact_service` (pact, 5); `jugcraft_roster`, `jugcraft_roster_suspended`, `jugcraft_roster_recovered`, `jugcraft_roster_empty` (roster, 14) | fits, named, beats and terms in order, spent once | Viewed, as intended: `jugcraft_pact_conversation`, `jugcraft_pact_terms`, `jugcraft_pact_kept`, `jugcraft_pact_refused` (the refusal under the Accept button's hint), `jugcraft_pact_service`, `jugcraft_roster`. Not viewed; its shard passed: `jugcraft_roster_suspended`, `jugcraft_roster_recovered`, `jugcraft_roster_empty` |
| Small window and high scale | `jugcraft_pact_terms_scale4`, `jugcraft_pact_conversation_scale4` (GUI 4, 1920×1080), `jugcraft_pact_terms_small`, `jugcraft_pact_conversation_small` (640×360); `jugcraft_roster_scale4`, `jugcraft_roster_small`, `jugcraft_roster_full`, `jugcraft_roster_full_small` (320×240); `jugcraft_fit_concordance_roster` (`ScreenFitClientGameTests`, 13) | applied scale 4; every control on screen | Viewed, as intended: `jugcraft_pact_terms_small`, `jugcraft_roster_small`. Not viewed; its shard passed: the other seven |
| Keyboard-only walkthrough | none (the pact and roster classes' assertions; logs `[pact-check]`, `[roster-check]`) | Enter through his beats, Tab to Accept, a recall, an offering, End asked first, Esc keeps; the roster key, Tab, Enter, Page Down, Esc | not applicable |
| A timed sequence for important tells | each Stolas pose: `jugcraft_stolas_idle`, `_listen`, `_suspended`, `_waiting`, `_inspect`, `_assent`, `_refuse`, `_depart`, `_arrive`, `jugcraft_pact_stolas`; `jugcraft_stolas_refuse_seq_1` to `_4` | from his front; clip and frozen time checked (logged) | Viewed, as intended: `jugcraft_pact_stolas`, `jugcraft_stolas_arrive`, `jugcraft_stolas_depart`, `jugcraft_stolas_listen`, `jugcraft_stolas_refuse_seq_1` to `_4` (from his front; the first with wings out, the other three with his head turned away). Not viewed; its shard passed: `jugcraft_stolas_idle`, `_suspended`, `_waiting`, `_inspect`, `_assent`, `_refuse` |
| Jade | `jugcraft_pact_perch_jade`, `jugcraft_pact_stolas_jade`; `jugcraft_workshop_hand_at_work` (the overlay when Jade is loaded) | Jade's readings, naming no player (server) | Viewed, as intended: `jugcraft_pact_perch_jade` ("Keeps an agreement with Stolas ... Next offering in 7 game days"), `jugcraft_workshop_hand_at_work` ("Workshop Hand, on its way, Carrying 3"). Viewed, defective in run 40133540741: `jugcraft_pact_stolas_jade` (read "Keeps an agreement with entity.minecraft."); fixed in `b773913c4` and viewed in run 40140478331 ("Stolas / idle: nothing to do / Service: Star-reckoning / Active / Next offering in 7 game days", no raw key) |
| The contrasting service at work | `jugcraft_workshop_hand_at_work` | the three aboard on its own claim; delivered once | Viewed, as intended (above) |
| Ending | `jugcraft_pact_end_confirm`, `jugcraft_pact_ended`, `jugcraft_roster_end_confirm`, `jugcraft_roster_ended` | keyboard on Keep it; ended once; escrow back once | Viewed, as intended: `jugcraft_pact_end_confirm`, `jugcraft_pact_ended`. Not viewed; its shard passed: `jugcraft_roster_end_confirm`, `jugcraft_roster_ended` |
| Remote player and dedicated-server sync | `jugcraft_pact_peer_arrival`, `_refused`, `_departure` (network, 3) | the two-client attempt, below | Viewed, as intended: `jugcraft_pact_peer_departure` (from the stranger's client: Stolas, wings out, leaving). Viewed, defective in run 40133540741: `jugcraft_pact_peer_arrival` (the "Loading terrain" screen), `jugcraft_pact_peer_refused` (Stolas's first words; the refusal not in view); fixed in `b773913c4` and `d00fa2167` and viewed in run 40140478331 (shard 3 and the opt-in job: Stolas descending over the world, wings out; his terms with the note "Only this perch's owner may invite Stolas here" and the refusal's cross, Accept greyed) |
| Optional integrations absent | none | server: the `optional integrations absent` job; client: the pact, roster and two-client classes passed in the opt-in job of run 40133540741 | not applicable |
| First- and third-person equipment | none | not relevant: Chunk D adds no equipment | not applicable |

Logged in run 40133540741 (`[pact-check]`, the pact client test): his refusal frozen at 0.290, 0.647, 1.004 and 1.357 s
into its clip (asked 0.25, 0.65, 1.05 and 1.45 s); his inspect at 0.737 s, assent 0.830 s, refuse 0.938 s, depart
0.530 s and arrive 0.966 s; "no sound device: the sound engine is off, so no subtitle can show; checked as defined and
as fired"; Jade's readings ok; Star-reckoning recalled (astrolabe 18, claim true); an offering deposited (1); ended,
the shards back (1). `[roster-check]`, the roster client test: 5 rows (a shade, Stolas, a workshop hand, a stranded
courier request and an open one), drawn as three portraits and an icon; a suspension; an end (gone, departed, kept); a
recovery (closed, 2 glowstone carried); the hand at work (3 carried, aboard on its own claim, 0 left at the source,
seen travelling), then 3 delivered; the full roster, 64 listed and 14 more. The opt-in job's log adds "Jade is not
loaded: no Jade shot of Stolas or his perch" and the roster's "Jade not loaded".

### The pact hall load card

The "Concordance load cards" job's pact hall output, as the job logged it in run 40133540741, on `48d99c904` (the
workshop's card ran before it in the same job and passed; one trailing space trimmed, after "0 total;"):

```text
===== Concordance Chunk D pact hall load card (D-09) =====
build: Jugcraft 0.1.0-alpha; Jade loaded
runner: Minecraft Version: 26.3
runner: Operating System: Linux (amd64) version 6.17.0-1022-azure
runner: Java Version: 25.0.4.1, Eclipse Adoptium
runner: Java VM Version: OpenJDK 64-Bit Server VM (mixed mode, sharing), Eclipse Adoptium
runner: Processor Vendor: AuthenticAMD
runner: Processor Name: AMD EPYC 9V45 96-Core Processor
runner: Frequency (GHz): -0.00
runner: Number of physical packages: 1
runner: Number of physical CPUs: 2
runner: Number of logical CPUs: 4
runner: Memory: 555745280 bytes (530 MiB) / 1071644672 bytes (1022 MiB) up to 4192206848 bytes (3998 MiB)
runner: JVM Flags: 0 total;
runner: max heap 3998 MiB; collectors G1 Young Generation, G1 Concurrent GC, G1 Old Generation
runner: JVM arguments: -Dfabric-api.gametest.filter=jugcraft-test:concordance_pact_hall_load_game_tests_load_card -Dfabric-api.gametest -Dfabric.dli.config=/home/runner/work/jugcraft/jugcraft/.gradle/loom-cache/launch.cfg -Dfabric.dli.env=server -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotServer --sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Duser.country -Duser.language=en -Duser.variant
world: game test server (flat world, ticks run back to back); noon and weather held, mobs and wandering traders off, random tick speed 3; player list view 0 and simulation 0 (the chunk map views at least 2); the yard's 9 chunks force-loaded (9 by the card); players 9 in both spans (8 holders and a guest: server-side test players, no client)
spans: baseline from tick 2023, pact hall from tick 9524; each 6000 ticks in 60 windows of 100 after warm-ups of 2000 and 1500 ticks (at least 2000 and 1500, then until the loaded chunk count held 400 ticks); tick times net of the test's own work, the simulated players' requests, filings and restocking included (timed apart below)
tick ms, net of the test's own work | median p95 mean max over-50ms | 100-tick window means: median p95
baseline 0.051 0.080 0.066 64.0 1 | 0.055 0.067
pact hall 0.149 0.391 0.182 30.9 0 | 0.174 0.262
raw medians 0.052 and 0.157 ms; the test's own work 0.005 and 0.028 ms a tick
delta: median +0.098 ms, p95 +0.311 ms; baseline drift (second half less first) -0.004 ms
memory: retained after a full collection: baseline 466 -> 467 MiB, pact hall 468 -> 468 MiB (+0 MiB beyond the baseline's change); heap in use +18.03 and +117.29 MiB per 1,000 ticks; collections 1 (3 ms) and 0 (0 ms)
chunks loaded: baseline 1054 -> 1054, pact hall 1054 -> 1054
counts: Pact Perches 9 (8 keeping a pact with Stolas, the guest's keeping none), Stolas 9 (8 serving, 1 visiting), players 9 (8 holders, 1 guest), Bound Wills 12 (8 pacts, 4 hand agreements), escrow 32 amethyst shards (4 a pact), Courier Posts 4, Spirit Anchors 4 (each bound to its post), Workshop Hands 4, chests 4 (128 glowstone each, five blocks from their post), open conversations 2 (the guest negotiating at the ninth perch; holder 0 reading their pact; each asked again every 200 ticks), open rosters 1 (holder 1's, read every 40 ticks), requests filed every 400 ticks at each post (staggered by 100), when none is open
level at the end: players: 9, entities: 22,22,7,108,108,0,0 [jugcraft:stolas:9,minecraft:player:9,jugcraft:workshop_hand:4], block_entities: 9 [jugcraft:pact_perch:9], block_ticks: 0, fluid_ticks: 0, chunk_source: Chunks[S] W: 1054 E: 22,22,7,108,108,0,0
active terms: Stolas's pact star_reckoning (radius 8, hours 0 to 24000, 1 a day; upkeep 1 of #jugcraft:stolas_offerings every 168000 ticks, grace 24000, up to 4 ahead); the workshop hand's courier agreement (radius 12, hours 0 to 12000, 32 loads a day, 64 a trip)
the hall's rules (by the code's constants, not sampled: each Stolas thinks every 20 ticks, each perch looks every 20, writing at most every 200); sampled every 100 ticks while measured (60 samples): one presence a pact, one pact a holder, the visitor listening and each hand its anchor's at every one (problems below); the oldest last look seen 160 ticks; Stolas {idle=420, idle listening=60}; hands {idle=180, working=60} (at their day's quota 0)
work while measured: loads 60 (by hand [15, 15, 15, 15]; a request is one load of 64), glowstone delivered 3840, requests filed 60, conversation answers 60, roster reads 150 (2 rows at the last)
the players' actions, timed apart (part of the test's own work, so not in the tick figures above): answers 60 at 261.1 us, roster reads 150 at 140.4 us, filings 60 at 143.1 us, restocks 1200 at 9.7 us; 0.010 ms a tick in all
calls timed after the spans (one unmeasured tick; 1000 looks and thinks, 200 pages and listings, each after as many untimed): a perch's look 1.59 us, a Stolas's think 1.09 us, holder 0's pact page 7.69 us, holder 1's roster listing 20.63 us (3600 rows read)
inventory: glowstone 512 = chests 512 + posts 0 + carried 0 (each workshop's 128 on its own account); escrow 32 amethyst shards = 8 pacts x 4, none taken (no period fell due); the compact not paid (sealed as unit 2's tests seal); Bound Wills held 12; open requests 1; ground and both ledgers checked; problems none
network to holder 0, the observed player (uncompressed, unframed): baseline 952 packets 11.9 KiB (0.04 KiB/s), pact hall 4451 packets 46.3 KiB (0.15 KiB/s); unsized 0 and 0; signs in reach 0 and 0 (0.0 KiB, an upper bound); pact states a client that declared them would be sent 30 (56.9 KiB)
network to the others: the other eight players were sent 7723 and 32909 messages (counted, not sized); the guest's pact states 30 (52.8 KiB) and holder 1's roster states 150 (294.0 KiB), sized as declared clients would receive them
network, pact hall, holder 0 by kind: minecraft:move_entity_pos 2135/20.8 KiB, minecraft:set_entity_motion 824/6.4 KiB, minecraft:move_entity_pos_rot 374/4.4 KiB, minecraft:entity_position_sync 103/3.8 KiB, minecraft:set_entity_data 225/3.0 KiB, minecraft:set_time 300/2.9 KiB, minecraft:system_chat 15/2.0 KiB, minecraft:player_info_update 10/1.5 KiB, minecraft:rotate_head 465/1.4 KiB
not measured: Jade's replies (they answer a client's request); compression and framing; client frame time (no client here)
not in the workload: Star-reckoning (once a game day a pact; its 2,400-tick attunement is the sky's, as a master's recall), an offering falling due, a warning or a suspension (the shipped period is 7 game days), ending a pact, a second roster, an unloading chunk
budget (D-P129 in C-P67's form, proposed; the owner decides at merge review): median +1.0 ms: within; p95 +2.5 ms: within; retained +4 MiB beyond the baseline's: within. Asserted here: +5.0, +12.5 ms, +64 MiB
===== end =====
```

Read with D-P129's proposed budget (median +1.0 ms, p95 +2.5 ms, retained +4 MiB beyond the baseline's): within, on all
three, by the card's own budget line (the hall added +0.098 ms to the median and +0.311 ms to the p95 server tick, and
+0 MiB retained beyond the baseline's change), on this runner (an AMD EPYC 9V45, 4 logical CPUs, Java 25.0.4.1), in
this one run. The card names its runner and claims nothing for other hardware.

### The two-client attempt

`ConcordancePactNetworkTests` (shard 3) starts a second, separate client JVM (offline name `PactPeer`, its own
temporary profile) against the first client's loopback-only dedicated server, on the pattern of main's
`OwnerCollectionsNetworkTests` (#435), not #467's harness. The stranger, south of the holder's perch, saw Stolas arrive,
assent and depart; at the holder's perch its Accept, sent with the compact in hand, was refused (`not_permitted`) and
spent nothing; at the kept perch its End was refused (`not_holder`); its roster listed nothing of the holder's and refused
an End naming the holder's pact (`not_listed`); the holder's pact sealed once and ended once, and the stranger kept
everything it carried. Passed in shard 3 of run 40133540741 and in its opt-in job without the optional integrations.
The class's `[pact-network]` lines, from the opt-in job's log (shard 3's log tail, as read, did not reach them):

```text
the stranger's Accept: not_permitted; holder holds, stranger's records, stranger's pocket kept = false 0 true
the stranger's End and roster End: not_holder not_listed; held, escrow, Stolas on his perch, stranger's records = true 0 true 0
the holder ends it: ended; held, departure kept, stranger's pocket kept = false true true
```

Two of its three pictures were defective in that run (Client evidence); `b773913c4` and `d00fa2167` change what the second
client waits for before them and check the refusal is shown (run 40140478331: every job passed; both pictures viewed
and as intended; the [pact-network] lines the same as above). The second client's log: the shard's
artifact, `owner-network-evidence/pact-<id>/peer.log`. Two automated clients on one machine with offline names: no two
people, no two accounts, no network between machines.

### Not run

A person playing any of it, alone or with others; two people, two accounts or two machines; a sound device (no subtitle
has been seen); the narrator's audio; an aimed click on the perch; client frame times on a player's hardware; a busy
server; JEI's pages in a client; `ScreenFitClientGameTests` without the optional integrations.

### Known defects and limits

- Stolas faces south whoever speaks to him (repro: place a perch, stand north of it, use it; outside the screen you see
  his back); the evidence photographs him from the south (D-P133).
- Suspending after each reckoning and resuming the next day stretches the upkeep week (checkpoint question 2).
- The first obligation counts once a player (D-P44); an absent requester at the head of a hand's queue holds it (D-P81);
  a crucible beyond a hand's 12 blocks waits for a porter.
- A solo pact needs Celestial Attunement for the compact's resonance (D-P123); the route makes its player a generalist
  Adept (open question).
- CI has no sound device; subtitles are checked as defined and fired.
- Found in run 40133540741's pictures, which its assertions passed: Stolas's own Jade line, the second client's arrival
  picture and its refused picture (Combined fixes); fixed in `b773913c4` and `d00fa2167`, seen fixed in run 40140478331 (every job passed).
- Outside Chunk D: main's `PeepoPopulationClientTests` cow-breeding flake (run 40085012234, one client shard re-run);
  main's `TurkeyGameTests` (#112) `jugcraft-test:turkey_game_tests_hens_lay_eggs` failed once in `optional integrations
  absent` of run 40121823509 ("The hen laid an egg and started on the next; the tom laid none on tick 10") and passed on
  its one re-run (job 120963228067), and in that run's `mod` job. Chunk D touches neither test.
- The other as-built limits are under Risks.

### Record fields (CLAUDE.md)

| Feature | Tier | Input producer | Output consumer | Costs | Unlocks | Failure behaviour | Test evidence |
|---|---|---|---|---|---|---|---|
| Agreements keep their promises (D-01) | Practitioner (the Binding Arts) | a Spirit Anchor and its owner; a shade's load | the anchor's slots, the roster, the familiar's bond | none new | nothing | a killed shade's load goes home once or drops where it fell; a refused seal names its reason and spends nothing | D-01's tests (its ticket) |
| One pact record (D-02) | shared | every holder block's seal | the perch, the anchor, the roster, the history | none | nothing | a copy is inert and releases nothing; upkeep never owes time away; ending returns the escrow once | D-02's tests |
| Stolas's Regard (D-03) | Practitioner, Spiritbinders | the Binding Arts understood; a feather or Styx flower examined; an organism crop harvested; a sample at the Assayer's Scale; one sky observation | the invitation; mastered, the workshop hand | none beyond the steps | the invitation; mastered: the workshop hand | notes never stand in; talking teaches nothing | D-03's tests; the whole path |
| Perch, terms and compact (D-04) | Practitioner | the perch's recipe; the compact (2 amethyst shards, 3 feathers, 8 Astral Resonance from an observatory its keeper fills) | the pact (a Bound Will with escrow) | the compact, once | Star-reckoning | refusals spend nothing and name their reason; a second acceptance `taken`; a stranger `not_permitted` | D-04's tests; the whole path; the pact and network client classes |
| Star-reckoning and upkeep (D-05) | Practitioner | observed patterns; resonance (6 to 16 a recall); offerings | recalled attunements; the Regard mastered | a recall's cost; one offering every 7 game days, at most 4 ahead | the Regard mastered | warning, a day's grace, suspension with nothing lost; never ends by itself; ending returns the escrow once | D-05's tests; the whole path; the pact client class |
| Stolas's character (D-06) | presentation | the server's actions and states | the player's eyes and ears; Jade | none | nothing | a lost presence called back free; leaving takes 40 ticks, never saved; no cue while switched off but his departure | D-06's tests; poses, sequence, hearing and Jade in the pact client class |
| The roster (D-07) | management | the roster, Bound Wills and courier requests | suspend, resume, end, cancel and recover, each through its own authority | none | nothing | not listed, stale or not offered is refused with its reason; switched off, listed and nothing offered | D-07's tests; the roster client class; `ScreenFitClientGameTests`; the network class (`not_listed`); the whole path's end |
| The workshop hand (D-08) | Practitioner (the Regard mastered) | an anchor bound to its holder's post; the post's requests | the requests' destinations, a crucible's shortage among them | none in fuel or upkeep; 32 loads a day of at most 64 | nothing | hold, hours and day's loads stop only new claims; killed or ended, its cargo waits at the post for one recovery | D-08's tests; `CompanionAutomationChecks.workshopHand`; the hand at work; the whole path |
| The evidence (D-09) | none | Chunk D | the owner's checkpoint | none in play | nothing | not applicable | the whole path; the load card; the client classes; `ConcordancePactNetworkTests` |

### The handoff (10:133-146)

- **What was built:** base `779851cdb` to final `d00fa2167` (the last code commit; these records follow it): D-01 to
  D-09; the contracts changed are under Interfaces expected to change and each ticket's As built notes.
- **APIs reused:** `AgreementHolder`, `BoundWills`, `BoundWillLedger`, `WorkerRoster`, `CourierLedger`, `Logistics`,
  `CourierWork`, `Authority`, `RateGate`, `AstrolabeItem.recall`, `AstralClaims`, `ConcordanceProgress`, `Signs`,
  `BookArt`, `ComposerScroll`, `ScreenKit.Fitted`, GeckoLib's triggerable clips, the Jade providers, `TestPlayers.survivor`,
  Fabric's client game test API, `OwnerCollectionsNetworkTests`' second-client pattern, Chunk C's load card's measuring parts.
- **Schemas, defaults, migration:** the Bound Will's optional `pact` and `escrow`; `jugcraft:pact_history`; the perch's
  `owner`, `agreement` and `spirit` (stamp 1); Stolas's `perch` and `record`; the Spirit Anchor's `owner` and `post`; the
  hand's `anchor` and `task`; `data/jugcraft/concordance/pact/stolas.json` (schema 1, strict); payload types
  `jugcraft:concordance_pact_request` and `_state`, `jugcraft:concordance_roster_request` and `_state`. A legacy anchor's
  `suspended` migrates on load and a legacy record is claimed once at its own place (D-P3); older saves load; there is no
  downgrade path (Deferrals). Unit 5 changes no saved data.
- **Generators and provenance:** `tools/concordance_pacts.py`, `concordance_workers.py`, `concordance_roster.py`,
  `stolas_body.py`, `owner_art.py` (the Flashjack rows and SHA-256 in `stolas-pact.md`), `concordance.py` (sounds, lang),
  the perch's icon map, `concordance_journey.py`, `concordance_progression.py`, `acquisition.py`, `concordance_economy.py`.
- **What the owner should look at:** the checkpoint below; the shots under Client evidence.
- **How to verify:** run the Build workflow by workflow_dispatch with every client class, ticking `load_card` and
  `clients_without_optional` when wanted; locally, the commands in `docs/TESTING.md`.
- **What is not verified:** under Not run.
- **Known defects:** under Known defects and limits.
- **Decisions:** the owner's are D-D0 to D-D2 and the carried direction D-002. The session's D-S1 to D-S23 were decided
  by the session under D-D2 (23 October 2026) and are not the owner's. Proposed, reviewable at the checkpoint: D-P1 to
  D-P50 (units 1 and 2), D-P70 to D-P83 (unit 4), D-P90 to D-P121 (unit 3) and D-P122 to D-P137 (unit 5); the load
  budget (D-P129); the workflow changes (the load job's second card and `clients_without_optional`) for review in the
  pull request.
- **Where to resume:** under Where to resume.
- **Next smallest complete outcome:** the first spirit of the cohort the owner chooses, one unit on the shared pact record
  with its character brief before its art; or Gloam Ink (C-D6), whichever the owner orders first.

### The owner checkpoint (as reported)

**Chunk D: meet Stolas, negotiate, use and safely end a working pact**

**What you can do now**
- With the Binding Arts understood, learn Stolas's Regard: examine a feather or Styx flower, harvest an organism crop, dissolve a sample at an Assayer's Scale; then record a sky observation.
- Build and use a Pact Perch: Stolas lands and names every term before anything is spent. Accept pays 2 amethyst shards, 3 feathers and 8 Astral Resonance; only an observatory's keeper gathers resonance, so a solo player learns Celestial Attunement.
- Once a game day at the perch, Star-reckoning recalls a sky pattern you observed. One offering a week, paid ahead, keeps the pact.
- End it at the perch or from the new roster: he departs, and what you paid ahead comes back once.
- Mastering his Regard unlocks the workshop hand, which carries your Courier Post's requests.

**What changed**
Spirit Anchors have owners, a killed Gathering Shade's load goes home once, familiars no longer fade offline, and the composer now draws subtitles over itself (unseen: CI has no sound).

**What I checked**
Candidate `48d99c904`, CI run 40133540741: every job passed first time. One server test walks from Styx to departure. Two game clients on one CI machine at one perch: passed (a stranger was refused the holder's pact). Load card: +0.098 ms median, +0.311 ms p95 tick on CI's runner, within the proposed budget. Three faults its pictures showed are fixed, and seen fixed in run 40140478331. Not checked: anyone playing it; sound; any hardware but CI's.

**What to look at**
- `jugcraft_pact_conversation`, `jugcraft_pact_terms`, `jugcraft_stolas_refuse_seq_1` to `_4`, `jugcraft_roster`, `jugcraft_workshop_hand_at_work`, `jugcraft_pact_peer_departure` (run 40133540741).
- In creative (the compact is free, a recall is not): `/jugcraft concordance grant @s <id> understood` for `jugcraft:binding_arts`, then `jugcraft:stolas_regard`; `/give @s jugcraft:astrolabe[jugcraft:astral_charge=32]`; observe at an observatory at night, place a perch, accept, reckon; bind the roster key (unbound at first) and end it there.

**Decisions for you**
1. **Stolas's look and manner.** A raven from your library's Flashjack rig: dry and patient ("I teach the stars to those who keep their word"), five gestures, sounds and subtitles, no voice; he faces south whoever speaks. I recommend keeping him, turning him toward his speaker, and giving later spirits their own silhouettes.
2. **How strict pacts feel.** A missed offering warns, gives a day's grace, then suspends with nothing lost; a pact never ends by itself. Suspending after each reckoning still stretches the week. I recommend accepting D-P50 (short trips never renew the grace) and letting the week run while suspended.
3. **The next six spirits.** I recommend gardens: they build on your living workshop (and Gloam Ink, once built), and a spirit tending beds tests work on someone's land with permission.

**Next proposed chunk**
The cohort you choose, one spirit a unit on the shared pact record, each briefed before its art; it needs Chunks B to D merged. Its main risk is six spirits differing only in yield.

## Deferrals

- **Gloam Ink** (D-S4): not part of Chunk D. It stays a separate small outcome (C-D6, CC:2363; its timing is not yet
  decided): one formula from a mixed bed's Gloamcap, consumed by Research Notes (CC:2304-2305). Research Notes never count as Stolas's
  evidence.
- **Further spirits and the other 71** (D-002): after the checkpoint chooses the next six-spirit cohort (03:163-165),
  each with its character brief.
- **Voice lines:** none exist; subtitles only, unless the owner supplies recordings.
- **Translations** beyond English (D-S21).
- **An encyclopedia Pacts page:** needs a raised page cap (a payload change) and stays read-only (08:16, 08:20).
- **Stolas's other two topics as services:** botanical interpretation and mineral comparison (03:136, 03:149-150).
- **Pact service toward Binding Arts mastery:** `pact_service` stays out of `worker_service`.
- **The ward keeper** (beneficiaries and escrowed sigils, the delivery plan's D-Q2 B) and a machine hand through
  `MachineCompanionPort`.
- **The observatory chart** (08:61-63) and a GuiLib view of the pact screens.
- **Importing #467's two-client harness:** for its author or the owner to bring in.
- **A downgrade path** for the new keys.
- **One second-client helper:** `ConcordancePactNetworkTests` copies `OwnerCollectionsNetworkTests`' launcher (D-P136);
  moving both onto one helper is left for a third two-client test.
- **Stolas turning toward his speaker:** the evidence photographs his front (D-P133); turning him is the owner's choice
  (checkpoint question 1) and needs a client run to judge.

## Validation plan

| Unit | Server game tests | Client game tests | Checks | Save tests |
|---|---|---|---|---|
| 1 | Worker, Authority, Sharing, Persistence, Resource | none | `check_workers`, `check_persistence`, the pact `problems()` audit, `check_repository`, `gradlew build` | an older Bound Will; an anchor's agreement; a legacy `suspended`; an unreadable record kept; stamps |
| 2 | Worker, Sharing, Persistence, Sky, Styx, Progression, Journal, Authority, Resource | `ConcordancePactClientGameTests` (conversation, terms, keyboard, service, ending) | `check_stolas_regard`, `check_pacts`, `check_pact_screen`, `check_economy`, `owner_art --check`, `concordance_journey --check`, `check_acquisition`, the icon maps, the recipe-conflict fixture if #462 has merged | the perch; the escrow and clock; a restart while suspended; a reload with a broken definition |
| 3 | Worker (presence), Journal, Logistics (recovery). As built: Worker +14, Logistics +2, Journal +1, first compiled and run in CI run 40121823509 on `040d73ece`, where they passed (run 40120355940 failed to compile the roster client test); passed again in run 40133540741 | poses, subtitles, Jade, the roster; the build without optional integrations. As built: `ConcordancePactClientGameTests` extended, `ConcordanceRosterClientGameTests` new, `ScreenFitClientGameTests` extended; passed in run 40121823509, where the build also passed with and without the optional integrations (`optional integrations absent` after one re-run); in run 40133540741 they passed in shards 5, 14 and 13, the pact and roster classes also in the opt-in job without the optional integrations | `check_geckolib` (triggerables), `owner_art --check`, client-test selection. As built, run and passed: `owner_art --check`, `check_mod_data.py` (with `check_stolas_body`, `check_roster`, `check_roster_screen` and the D-06 block of `check_pacts`; 104 audit mutations reported), `check_repository.py`, `concordance_journey.py --check`, `check_icon_maps.py`, the World Designer and script tests, `git diff --check`, the Java parse check; `select_client_tests.py --base 92868ff82` chooses 52 of 242 client classes, the three above among them | the entity's keys. As built: no saved-data change (the departure and the listening flag are never saved; roster sessions are memory only) |
| 4 | Logistics (seven of 03:193's cases and four more), Worker (five), Authority (one), Persistence (two) | `PeepoCompanionClientTests` / `CompanionAutomationChecks` | `check_workers` (`WORK`, the class map), `check_hand`, the logistics audit | a claim across a restart; an anchor's post |
| 5 | the whole path (`ConcordanceJourneyGameTests.thePactHallFromStyxToDeparture`, `thePactHallsRecipesLoad`); load (`ConcordancePactHallLoadGameTests.loadCard`, opted in: a manual Build run with `load_card`) | the bundle (pact, roster, screen fit); `ConcordancePactNetworkTests`; the opt-in run without optional integrations | every check, with `check_acquisition`'s pact hall, `concordance_journey --check`, `check_load_card`, `check_pact_hall_load_card`, `check_stolas_body`'s sequence, `check_pact_network`, `check_composer` and the codex-place audit | none |

Every unit also runs `tools/check_mod_data.py`, `scripts/check_repository.py`, `scripts/tests`, the generators (their
output current) and the build with its server game tests, and its CI by workflow_dispatch of the Build workflow with
every client class.

**Evidence rules** (10:5-22):
- Each command is reported on its own as run and passed, run and failed, or not run. CI on a commit counts for that
  commit only.
- A server game test with several test players is not a party of people. One client on a dedicated server is not two
  clients; the two-client session and a human journey stay not run unless two accounts or a test environment are
  available (09:20).
- Vanilla's mock players are creative: a spending test counts only with a survival test player.
- A CI runner's timings are not a player's hardware; a software-rendered frame time is that renderer's.
- A screenshot shows presentation, not balance; nothing is called balanced without a human playtest.
- Numbers from a generator's model stay labelled so until a game test measures them.

## Decisions

Chunk A's, B's and C's decisions are unchanged: B-D0 to B-D3, C-D0, C-D0b, C-D1 to C-D3 and C-D5 to C-D8 are the
owner's, while C-D4 and every B-P and C-P stay Proposed (Chunk C's contract, Decisions and Where to resume, CC:2351,
CC:2444-2445). D-D decisions are the owner's. D-S decisions are the session's under D-D2: the owner
asked not to be asked, so they are recorded here as the session's choices, never as accepted by the owner, and stay
reviewable at the checkpoint (10:162). D-P decisions are the units' proposals: choices the session made in building a
unit where the tickets above left room or had to be corrected (D-P1 to D-P12 from unit 1's specification, D-P13 to D-P16
from its review; D-P17 to D-P49 from unit 2's specification and its review, numbered on from unit 1's; D-P50 from unit
2's unit review), each with what was built; none is the owner's, and each is reviewable at the checkpoint. Unit 4's are
numbered from D-P70, a block reserved for it so that units built in parallel never collide (unit 2 takes the numbers
after unit 1's): D-P70 to D-P80 from its specification (its D-P60 to D-P70, renumbered), D-P81 from its specification's
review and D-P82 and D-P83 from the unit review's fixes. Unit 3 takes D-P90 upward: D-P90 to D-P121 from its
reconciled specification (its four part designs' numbers, renumbered), as corrected by the specification's critique.
Unit 5 takes D-P122 upward: D-P122 to D-P137 from its reconciled specification.

| ID | Date | Decision | Status | Affects |
|---|---|---|---|---|
| D-D0 | 23 October 2026 | "begin chunk d": Chunk D, the pact hall (09:146-174), is the next chunk | Accepted (owner) | All of Chunk D |
| D-D1 | 23 October 2026 | "New branch on Chunk C (Recommended)": Chunk D is developed on `claude/chunk-d-pact-hall`, started from Chunk C's head `779851cdb` (#466, itself stacked on Chunk B's #465), with "A third stacked PR (D on C) when it's ready", its base `claude/chunk-c-living-workshop` | Accepted (owner) | D-00, every unit |
| D-D2 | 23 October 2026 | "dont ask me to approve just finish this whole part": the session does not stop for approvals and finishes Chunk D end to end. It therefore settles the plan's open questions with its recommendations as corrected by the critique (D-S1 to D-S23), each recorded as the session's, reviewable at the checkpoint | Accepted (owner) | every D-S decision |
| D-D3 | 26 October 2026 | "keep stolas" (checkpoint question 1): Stolas's look and manner are kept as built: the raven adapted from the owner library's Flashjack rig, dry and patient, five gestures, sounds and subtitles, no voice. The recommendation's other two parts were not answered separately: the next chunk gives each new spirit its own silhouette in its brief, and carries "a spirit faces whoever speaks to it" as shared groundwork that Stolas also uses (his south facing is a known defect, not part of his design) | Accepted (owner) | D-06; the garden cohort |
| D-D4 | 26 October 2026 | "do D-P50" (checkpoint question 2): D-P50 is accepted. A warning given before an absence keeps only the grace that was left, so short trips never renew it. It was already built in unit 2's review (`Pacts.due`; `shortAbsencesNeverRenewTheGrace`), so no code changes. The recommendation's second part (letting the week run while a pact is suspended) was not chosen: suspension still pauses the clock as built (D-P2, fix 1), and the stretched week stays a known limit | Accepted (owner) | D-S3, D-P2, D-P50 |
| D-D5 | 26 October 2026 | "gardens next" (checkpoint question 3): the next six-spirit cohort is gardens, the first of 03:165's suggested cohorts. It is built as the next chunk, on its own branch stacked on this one: one spirit a unit on the shared pact record, each briefed (identity, silhouette, service, upkeep, conflict, refusal) before its art, each service mechanically distinct (03:167-181) | Accepted (owner) | the garden cohort |
| D-002 | prior owner direction (10:155) | "Stolas first, then all 72 distinct spirits" | Accepted direction (carried from the pack) | D and later cohorts |
| D-S1 | 23 October 2026 | Stolas's service is Star-reckoning: once a game day a pact, the holder at the perch recalls any pattern they observed, with their astrolabe's resonance, at its existing recall cost, under the shared `recall:<pattern>` claim, for 2,400 ticks; one recall path shared with a master's; non-masters may recall the Harvest Moon and Winter Crown as often as the quota allows (D-05) | Decided by the session under D-D2 (23 October 2026) | D-04, D-05 |
| D-S2 | 23 October 2026 | The contrasting service is the workshop hand: its own worker definition, entity, lang and model (a variant of the shade's body); a Spirit Anchor bound to the holder's Courier Post; the post's exact requests within its hours, area and quota; beneficiaries the holder and party; unlocked by mastering Stolas's Regard; the porter's courier decision extracted and shared (D-08) | Decided by the session under D-D2 (23 October 2026) | D-08 |
| D-S3 | 23 October 2026 | Strictness is Gentle: one offering in 7 game days from escrow (`#jugcraft:stolas_offerings`: three Styx flowers, amethyst shard), up to 4 prepaid, the first by the compact; looked at only with the perch loaded and the holder online, alive, in its level and within 64 blocks, never a stand-in; deadlines reschedule from the first look after an absence; warning, 1 game day of grace, suspension, resumption on paying; never ends by itself; E2 shown on Stolas's upkeep (D-02, D-05). Unit 2's unit review found two routes around it: a warned holder away over a minute at least every 20 minutes got the whole grace again at each return (D-P50 proposes the grace they had left instead); and suspending, recalling and resuming once a day moves the due date on by the time held, so the period counts only a few ticks of active time a day (left to the owner) | Decided by the session under D-D2 (23 October 2026) | D-02, D-05 |
| D-S4 | 23 October 2026 | Gloam Ink is not part of Chunk D; it stays a separate small outcome. Research Notes never count as Stolas's evidence | Decided by the session under D-D2 (23 October 2026) | Deferrals |
| D-S5 | 23 October 2026 | Stolas's body adapts the library's Flashjack rig into a raven-form runtime copy with authored clips; originals intact; provenance recorded; sounds are new `jugcraft:stolas.*` events through the generator, bound to runtime files, each with a subtitle; no voice. Appearance stays a checkpoint question (D-06) | Decided by the session under D-D2 (23 October 2026) | D-06 |
| D-S6 | 23 October 2026 | Escrow and terms live in a world record; the perch is a place marker holding nothing; deposits are server-side actions; breaking or blowing up the perch never ends the pact ("without its perch"); ending returns the escrow once to the living holder, overflow at their feet; a bounded history of 8 departures in a separate saved record (D-02, D-04, D-05) | Decided by the session under D-D2 (23 October 2026) | D-02, D-04, D-05 |
| D-S7 | 23 October 2026 | Stolas arrives as a session-bound visitor and leaves without a record or session; after acceptance, one presence a pact; no damage but bypass damage; a lost presence called back at no cost, never ending the pact; in `concordance/spirits`, MISC, refused by transport crates (D-04, D-06) | Decided by the session under D-D2 (23 October 2026) | D-04, D-06 |
| D-S8 | 23 October 2026 | The perch records its owner; only the owner invites and accepts there. New anchors record their owner; sealing needs the owner or the owner's party; ownerless legacy anchors keep today's rule plus `Authority.mayChange`; a stand-in never seals; a second acceptance at one perch is `TAKEN` and spends nothing (D-01, D-04) | Decided by the session under D-D2 (23 October 2026) | D-01, D-04 |
| D-S9 | 23 October 2026 | States: invited and negotiating transient; active and suspended (holder or upkeep) on the Bound Will, the single authority, the anchor's `suspended` migrating on load; waiting derived with distinct upkeep reasons; departed removed plus a history entry. A holder block entity counts only if the record matches id, holder and place; legacy records claimed by the first matching anchor to load; later copies inert, `CONFLICTING_AGREEMENT`, releasing nothing; the shared holder extracted from the anchor in unit 1 (D-02) | Decided by the session under D-D2 (23 October 2026) | D-01, D-02 |
| D-S10 | 23 October 2026 | One agreement a place; one Stolas pact a holder; any number of holders a world; Bound Wills non-transferable (D-02) | Decided by the session under D-D2 (23 October 2026) | D-02, D-04 |
| D-S11 | 23 October 2026 | A native roster screen and a journal "pacts" section; no encyclopedia page; rows from the roster and Bound Wills, at most 64; a `ROSTER_FULL` seal refusal (D-01, D-07) | Decided by the session under D-D2 (23 October 2026) | D-01, D-03, D-07 |
| D-S12 | 23 October 2026 | Research `jugcraft:stolas_regard` (spiritbinders, practitioner, needs Binding Arts understood): examine `#jugcraft:stolas_specimens`, practise cultivation, practise assay; no notes; the sky step a separate precondition named by Styx and the journal; mastered by the first obligation (`practice jugcraft:pact_service`, its gate shipped with the research); companion advancements, Modonomicon data and a Binding codex entry; mastery unlocks the workshop hand; talking to Styx teaches nothing (D-03) | Decided by the session under D-D2 (23 October 2026) | D-03, D-05, D-08 |
| D-S13 | 23 October 2026 | The Open Hand Compact is the acceptance cost (ordinary materials and astrolabe resonance), spent in the call that seals; not a rite. Spending tests use a survival test player: vanilla's mock reports creative whatever game mode is set on it, so setting survival on the mock is not enough (Facts; D-04) | Decided by the session under D-D2 (23 October 2026) | D-04 |
| D-S14 | 23 October 2026 | A perch-bound menu (the `ShopMenu` pattern) with bounded actions and a terms revision, carried by one small client-to-server payload (container id, sequence, action, choice, revision); no free text, player ids, client totals, Dialog, chat commands, mixins or GuiLib; a native, narrated, keyboard-operable `PactScreen` (D-04) | Decided by the session under D-D2 (23 October 2026) | D-04, D-05 |
| D-S15 | 23 October 2026 | The terms page shows service, hours, area, quota, upkeep and grace, what suspension does, duration, breach rules, how to end, what returns, beneficiaries and the compact's cost. The perch is placed only where the placer may build, never in Styx's protected district; one Stolas a holder is the entity cap (D-04) | Decided by the session under D-D2 (23 October 2026) | D-04 |
| D-S16 | 23 October 2026 | With the Concordance disabled, no upkeep falls due and nothing suspends; Stolas waits (`DISABLED`); records and escrow untouched; tested (D-02, D-05). As built (unit 1): a switch-off, like any gap between looks, of at most `ABSENT_TICKS` (1,200 ticks) is not an absence, so a due date or grace end inside it counts at the first look after (D-P2; the unit review's fix 8) | Decided by the session under D-D2 (23 October 2026) | D-02, D-05 |
| D-S17 | 23 October 2026 | The familiar's fade, an adjacent routine fix and not E2 evidence: the bond fades only for its person's own play time away, so offline days never count, even unloaded; days played in another dimension still count (D-01) | Decided by the session under D-D2 (23 October 2026) | D-01 |
| D-S18 | 23 October 2026 | A shade's death: its load into the anchor's slots when the anchor is loaded, otherwise dropped where it fell; "lost" read from the roster, not from an absent entity; a re-call while the old shade is unloaded makes it leave once and deliver once (D-01) | Decided by the session under D-D2 (23 October 2026) | D-01 |
| D-S19 | 23 October 2026 | Consumed offerings stay in equivalence; only pact items (the perch's item, any compact item) are excluded; the balance is documented (D-04, D-05) | Decided by the session under D-D2 (23 October 2026) | D-04, D-05 |
| D-S20 | 23 October 2026 | No edits under `src/main/java/local/peepo` (#452); the gametest companion checks may be extended (D-08) | Decided by the session under D-D2 (23 October 2026) | D-08 |
| D-S21 | 23 October 2026 | Text comes from the generators; English only | Decided by the session under D-D2 (23 October 2026) | every ticket |
| D-S22 | 23 October 2026 | #467's two-client harness is an unmerged fork contribution and is not imported; two-client checks are attempted only with what this branch has, otherwise recorded as not run (D-09) | Decided by the session under D-D2 (23 October 2026) | D-09 |
| D-S23 | 23 October 2026 | An invalid or missing pact definition reports cleanly on reload, and sealed pacts keep the terms copied at sealing; a holder leaving a party or the perch's land changing hands never moves a pact to another player; both tested (D-04, D-05) | Decided by the session under D-D2 (23 October 2026) | D-04, D-05 |
| D-P1 | 24 October 2026 | Pact data is `BoundWill.Pact`, an optional `pact` key, and the escrow an optional `escrow` key (an `ItemStack` list) of the same `jugcraft:bound_wills` entry, still version 1; there is no `jugcraft:pacts` (the ticket's `PactRecords`), so release removes a record and its escrow in one call. The departure history is `PactHistory` (`jugcraft:pact_history`, version 1, read holder by holder, 8 entries each). As built, the pact also keeps its service terms (`service`) and day's count (`window`, `done_today`) as optional keys (the review's correction 4), so a pact seated again keeps its terms and its day. An entry is read whole: one escrow stack this version cannot read (an item or component from a removed mod) keeps the whole entry unread like an unreadable pact key, so its anchor waits, `end` finds nothing and it drops out of `heldBy` and the 32-record count, though nothing is lost; whether to decode escrow stack by stack is decided in unit 2, before D-05's offerings (the unit review's fix 9). No definition revision is stored on a pact: a sealed pact keeps its copied terms, and D-04's terms revision is checked per negotiation (fix 11) | Proposed (unit 1, reviewable at the checkpoint) | D-02, D-04, D-05, D-07 |
| D-P2 | 24 October 2026 | Upkeep runs on game time, on the Spire's model. It is looked at only with the Concordance on and the holder present: online, alive, in the place's level, within `Pacts.REACH` = 64 blocks, never a stand-in. At most one period falls due a look. With none in escrow, a warning and the grace; the grace over, an `upkeep` hold; paying resumes the pact. After a gap of more than `ABSENT_TICKS` = 1,200 ticks without a look, a warned pact is warned again with the whole grace (amended by D-P50, proposed in unit 2's unit review: with the grace left, its end moved on by the time away); a shorter gap (the holder away, the place unloaded or the Concordance off) is not an absence, so a due date or grace end inside it counts at the next look (the unit review's fix 8). The holder's own suspension pauses the clock: due date and grace move on by the time held, and so does the last look (never past now), so an absence before the hold still counts (fix 1). The last look is written at most every `WRITE_TICKS` = 200 ticks; a look in the tick of the last one goes on, so an offering paid in the tick of a ticker's look resumes at once (fix 7). As built (correction 3): paid, the next falls a period after the due date, or a period from now if that has passed; after an absence a passed due date becomes the moment of return; resumed from an upkeep hold by an offering, the next falls a whole period from then. Neither time away nor time held is charged, beyond a gap of at most `ABSENT_TICKS` | Proposed (unit 1, reviewable at the checkpoint) | D-02, D-05 |
| D-P3 | 24 October 2026 | A record from before Chunk D is claimed only by the block whose agreement's own `dimension`, `x`, `y` and `z` are its place, and only while its spirit, if loaded, is seated there; a copy elsewhere never claims, whichever loads first (stricter than D-S9's "first claim wins"). A new seal or claim at a place still keeping an orphan record leaves that record without its place, its holder and escrow kept. As built, the claim also copies the block's service terms and day's count onto the record | Proposed (unit 1, reviewable at the checkpoint) | D-02, D-04 |
| D-P4 | 24 October 2026 | After the claim the block's `Agreement.suspended` is written false; the record's hold is the only authority. A downgrade is unsupported. As built, the flag is kept for a downgrade's reading, not for crash safety: the anchor's chunk saves when it unloads and `jugcraft:bound_wills` at autosave, so a crash after the chunk (its flag now false) is saved and before the claim is loses the claim, and the next verify claims the record again with no hold; a suspension made before Chunk D is then lost once, and its holder can suspend again (the unit review's fix 10; Risks). Keeping the flag mirroring the record's holder hold would close this | Proposed (unit 1, reviewable at the checkpoint) | D-02 |
| D-P5 | 24 October 2026 | `TAKEN` means the holder block already keeps terms, not that the ledger has some record at the place: safe with game-test structure clearing (flag 818 skips side effects) and with crash orphans | Proposed (unit 1, reviewable at the checkpoint) | D-02, D-04 |
| D-P6 | 24 October 2026 | Sealing an owned holder block needs its owner, or their party where the block lets parties seal, **and** `Authority.mayChange` at the block; an ownerless legacy anchor needs only `mayChange`; a stand-in never seals. As built: `Sealing.partySeals` (the anchor true; D-04's perch false, D-S8; correction 6); "the Binding Arts are not understood" stays the anchor's own check before sealing, worded as before, not a seal outcome | Proposed (unit 1, reviewable at the checkpoint) | D-01, D-04 |
| D-P7 | 24 October 2026 | A block whose Bound Will is gone (and not merely unreadable) forgets its terms when it next verifies, and may be sealed again; its shade then departs, its load dropped at the anchor (which no longer keeps its holder's agreement, D-P12). An unreadable record makes the block and its shade wait | Proposed (unit 1, reviewable at the checkpoint) | D-02 |
| D-P8 | 24 October 2026 | `end`, `toggle`, `recall` and `deposit` are not rate-gated themselves; the anchor's use keeps `RateGate.allow(server, "workers", 4)`. D-04's and D-07's menus and roster actions must call `RateGate.allow` before `end`, `toggle`, `deposit` and `recall` (a later `check_pacts` needle can hold it at their payload handlers). As built: suspend and resume by record id alone (`AgreementHolder.toggle(level, player, id, now)`), for D-07's roster while the place is unloaded (correction 9); `end` has the same static form. As built in unit 3: the roster's suspend, resume and end ask the anchor's gate (`workers`, 4 ticks) before `toggle` and `end`, and its cancel and recover the logistics commands' (D-P107); ended from the roster (D-07) | Proposed (unit 1, reviewable at the checkpoint) | D-02, D-04, D-07 |
| D-P9 | 24 October 2026 | The anchor gets no server ticker: its definition has no upkeep. `AgreementHolder.look` is the rule's driver, called by D-04's perch ticker. `OnBreak.UNSEAT` (a broken perch leaves its pact without its place) arrives with D-04 | Proposed (unit 1, reviewable at the checkpoint) | D-02, D-04 |
| D-P10 | 24 October 2026 | Status ids `upkeep_due` and `upkeep_held` are appended (each a want sign) | Proposed (unit 1, reviewable at the checkpoint) | D-02, D-07 |
| D-P11 | 24 October 2026 | `TestPlayers` gains `survivor`, `leave`, `again`, `played` and `play`. Editing `TestPlayers.java` makes `tools/select_client_tests.py` choose every client class (a shared file); accepted. As built: every class, 239 | Proposed (unit 1, reviewable at the checkpoint) | D-01, D-02 |
| D-P12 | 24 October 2026 | A departing or killed shade's load goes into the anchor at its seat when that anchor is loaded and keeps an agreement of the shade's own holder, whether or not it still names the shade; otherwise it drops at its anchor's place (departing) or where it fell (killed), whatever the block-drop rule says (`Containers.dropItemStack`, the unit review's fix 3). The specification had any anchor at the seat; as built, never one sealed again by someone else (correction 12) | Proposed (unit 1, reviewable at the checkpoint) | D-01 |
| D-P13 | 24 October 2026 | Switched off, a loaded familiar whose person is online (in any level) restarts its play-time count at each look, so it fades for none of their play meanwhile (correction 13, D-S16). One not loaded while the Concordance was off counts its person's play meanwhile at its first look after the switch is on: no switch-off time is recorded. The residual case is accepted and said in the records | Proposed (unit 1, reviewable at the checkpoint) | D-01 |
| D-P14 | 24 October 2026 | A seal may carry a cost (`Sealing.affords` and `pay`): asked after the refusals and before anything is called, refused as the appended outcome `CANNOT_AFFORD` (`seal_cannot_afford`, nothing spent), and paid in the same server call only once the Bound Will is recorded (correction 10, D-S13). The anchor's seal costs nothing; D-04's Open Hand Compact uses it | Proposed (unit 1, reviewable at the checkpoint) | D-01, D-04 |
| D-P15 | 24 October 2026 | A holder block calls its presence by counterpart (`Holds.call(level, who, counterpart)`) and calls nothing (so `UNAVAILABLE`) for a counterpart it does not serve: the anchor serves only `jugcraft:gathering_shade`. Every presence (`AgreementHolder.Presence`) is a `WorkerEntity` subclass, unit 2's Stolas and unit 4's hand included, so each has a roster entry to note and strike (corrections 7 and 8) | Proposed (unit 1, reviewable at the checkpoint) | D-01, D-04, D-06, D-08 |
| D-P16 | 24 October 2026 | Breaking a holder block while the Concordance is off releases nothing: its record stays kept at that place with its escrow, no departure is written and its shade is not sent away (correction 5, D-S16). Once the switch is on, the shade finds no anchor and departs, its load dropped where the anchor stood; the next seal there leaves the record without its place; its holder can end it (by code until D-07's roster). As built in unit 3: ended from the roster (D-07), which lists a record left without its place, or kept at a place that keeps it no longer, and offers End | Proposed (unit 1, reviewable at the checkpoint) | D-02, D-07 |
| D-P17 | 24 October 2026 | Only the Binding Arts gate Stolas's Regard in code (its `requires`). Verdant Husbandry and the Assay are asked only by the progression graph's practice gates, so the graph asks more than the game, never less (`rules/ResearchEngine.java:41-43`; a Verdant Bed wakes only for a Greenwarden). As built: `stolas_regard.json` requires the Binding Arts understood and nothing else | Proposed (unit 2, reviewable at the checkpoint) | D-03 |
| D-P18 | 24 October 2026 | Two codex entries in the Binding category: `binding/stolas_regard` (the research, at (0,4), from the Binding Arts understood; the Binding Arts' Understood page links it, and its own Understood page links the pact's) and `binding/stolas_pact` (the perch's recipe, his terms, the Open Hand Compact, Star-reckoning, upkeep, ending and moving; at (2,4), from Stolas's Regard understood). There is no `binding/stolas.json` | Proposed (unit 2, reviewable at the checkpoint) | D-03, D-04, D-05 |
| D-P19 | 24 October 2026 | The three Styx flowers are "found" sources in `tools/concordance_progression.py` (his gift, once a player; "a feather serves instead"), so the progression graph never relies on them; the feather carries the route | Proposed (unit 2, reviewable at the checkpoint) | D-03 |
| D-P20 | 24 October 2026 | Styx's hint has seven steps (`binding`, `examine`, `cultivate`, `assay`, `serve`, `kept`, `journal`), taken from the first entry of the listener's own `ResearchEngine.explain` for Stolas's Regard; the sky line is added until an observation is recorded, except at `kept`. With the Concordance off he says only his line | Proposed (unit 2, reviewable at the checkpoint) | D-03 |
| D-P21 | 24 October 2026 | The journal's `pacts` section comes right after `logistics`, shown from the Binding Arts understood; D-07 adds the pacts kept | Proposed (unit 2, reviewable at the checkpoint) | D-03, D-07 |
| D-P22 | 24 October 2026 | The `pact_service` practice gate's graph needs an observatory **and** a pact perch, under night and open sky (the gate's file ships with D-03; the perch joins its graph in D-04) | Proposed (unit 2, reviewable at the checkpoint) | D-03, D-04 |
| D-P23 | 24 October 2026 | The terms revision counts the offer only. Every request names it, and a mismatch is answered `changed` with the terms again, nothing spent. A reload that changes the offer moves it; a perch being taken does not, so a second ACCEPT is `taken` (D-S8) | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P24 | 24 October 2026 | A perch records its placer. It is placed by hand only by a player in person (never a stand-in) where they may build (`Authority.mayChange`), never in Styx's home. As corrected by the unit review: a perch set without a placer (a blueprint stake, a drone build, a command or a structure) records no owner and seals and seats for nobody (`AgreementHolder`'s owner flag); the anchor keeps the ownerless legacy rule | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P25 | 24 October 2026 | Breaking or blowing up a perch unseats its pact only with the Concordance on (`OnBreak.UNSEAT`). Switched off, the record keeps its vanished place (D-S16, D-P16) and is "stranded" (`AgreementHolder.stranded`: no place, its level gone, or its place loaded and keeping other terms or none), so it can be seated again; one whose old place stays unloaded cannot be seated until it loads, though it can be ended | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-05 |
| D-P26 | 24 October 2026 | A player with infinite materials (creative) pays no compact, as vanilla's crafting does; spending is shown with `TestPlayers.survivor` | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P27 | 24 October 2026 | Conversations are one a player, bound to the open menu, ended after 6,000 ticks without a request, memory only; forgotten when the player leaves the player list, on disconnect and at server stop (the review's correction 9). One visitor a perch, who comes only for an invitable owner and waits while any conversation at that perch is live, the owner's or another's | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P28 | 24 October 2026 | A lost presence is called back when its holder opens the perch, and by the perch's look once a second while the holder is within 64 blocks (`Pacts.REACH`), if their roster has room; never at a cost, and a lost presence never ends the pact | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-05 |
| D-P29 | 24 October 2026 | Pact definitions are a new Concordance data kind, `pact` (`PactCatalog` on `ConcordanceRules`, read by the strict `PactParser`, schema 1). An invalid file is refused with a reason; a missing required definition is reported, except by empty rules | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P30 | 24 October 2026 | `#jugcraft:stolas_offerings` ships with the definition (D-04); D-05's upkeep consumes it | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-05 |
| D-P31 | 24 October 2026 | "Styx's protected district" is `StyxConservatory.covers`: the conservatory's footprint for any layout, plus a town-built home's district and road | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P32 | 24 October 2026 | The perch's recipe uses polished deepslate, not a slab, so the progression graph can follow it (simulated: a slab gave 3 problems; as a mutation, 12 reports) | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P33 | 24 October 2026 | One state shape: four modes (negotiate, keep, seat, closed); terms as (key, value) rows; pattern and offering choices whose ids the server keeps; the compact's stacks; `Result(sequence, reason, done, message)`. CHOOSE is answered with the state and no result | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-05 |
| D-P34 | 24 October 2026 | ACCEPT is offered only when nothing is missing; a forced ACCEPT is still answered with its reason, spending nothing | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P35 | 24 October 2026 | CHOOSE has its own rate-gate key (`pact_choose`), so hearing the terms never blocks the next press; opening the perch has `pact_open` and every other action `pact` | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P36 | 24 October 2026 | After every action the player's inventory menu broadcasts its changes, because a slotless screen otherwise syncs nothing | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P37 | 24 October 2026 | The pact test helpers are package-private statics in `ConcordanceWorkerGameTests`; there is no new helper file (one would make every client class selected) | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-05 |
| D-P38 | 24 October 2026 | `PactScreen` is a plain `Screen` with `MenuAccess<PactMenu>` (the lectern's pattern) | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P39 | 24 October 2026 | His beats and terms are pages on the client; CHOOSE(0) is sent once, when the terms page first shows | Proposed (unit 2, reviewable at the checkpoint) | D-04 |
| D-P40 | 24 October 2026 | Ending always asks first through vanilla's `ConfirmScreen`; Esc keeps the pact | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-05 |
| D-P41 | 24 October 2026 | Stolas's provisional body is the owner's Flashjack rig (model, animations and skin), copied byte for byte by `tools/owner_art.py` and drawn at 1.4×; the recolour and authored clips are D-06's | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-06 |
| D-P42 | 24 October 2026 | `check_geckolib` gains a per-face mode (`faces=True`, Stolas only: the rig's three overlapping UV regions are accepted as drawn while no face overlaps another) and pins clips played and held (`thenPlayAndHold`) | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-06 |
| D-P43 | 24 October 2026 | Star-reckoning refuses in this order: switched off, nothing kept, a copy, not the holder, suspended, held for upkeep, outside the area, outside the hours, the day's quota, not observed, no astrolabe, too little, recalled already. The paying astrolabe is the first carried (hands, then slots) that holds enough. No empty sky is needed and Stolas need not be present | Proposed (unit 2, reviewable at the checkpoint) | D-05 |
| D-P44 | 24 October 2026 | The first obligation is a direct call from `ObservatoryBlockEntity.observe` (`StarReckoning.observed`), not a listener (listeners are read-only, `ConcordanceProgress.java:114-118`). It counts once a player, not once a pact: stricter than the contract's wording | Proposed (unit 2, reviewable at the checkpoint) | D-05 |
| D-P45 | 24 October 2026 | Re-seating (`AgreementHolder.seat`): its owner alone, in person; a stranded pact only; a free place; the roster asked first; the same record, terms, day's count, upkeep clock and escrow; the presence called again at no cost | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-05 |
| D-P46 | 24 October 2026 | Suspend and resume at the perch are idempotent: a stale screen never toggles the wrong way, and only an offering lifts an upkeep hold | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-05 |
| D-P47 | 24 October 2026 | Deposits are by kind, in slot order, through `AgreementHolder.deposit` (its holder, near, switched on; up to the cap) | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-05 |
| D-P48 | 24 October 2026 | The departure line names who left, why, and how many offerings came back | Proposed (unit 2, reviewable at the checkpoint) | D-04, D-05 |
| D-P49 | 24 October 2026 | Economy: Star-reckoning is a sink (`astral_resonance` → `recalled_attunement`, modelled at the cheapest recall) and Stolas's offerings are a sink of at most 1/7 of an offering a day a pact; the gaining cycles stay none. Ending and sealing anew could give a second Star-reckoning in a game day only for a whole new compact: no positive-gain loop (the review's correction 10) | Proposed (unit 2, reviewable at the checkpoint) | D-05 |
| D-P50 | 24 October 2026 | Amends D-S3: a holder warned before an absence is warned again on return with the grace they had left, its end moved on by the time away (`Pacts.due`: `graceUntil` becomes the old end plus `now - lookedAt`), never a whole new grace; a first warning after an absence still gives the whole grace. Time away is never charged. It changes unit 1's `Pacts.due`, so the unit 1 merge carries it. Tested by `shortAbsencesNeverRenewTheGrace`; `aReturnAfterAbsenceGetsTheFullGrace` holds exactly. The second route (suspend, recall, resume once a day) is left to the owner | Accepted (owner, D-D4, 26 October 2026; proposed in unit 2's unit review) | D-02, D-05 |
| D-P70 | 24 October 2026 | Definition `jugcraft:workshop_hand`: kind `spirit`, work `courier`, radius 12, hours 0 to 12000 (06:00 to 18:00), quota 32 loads a day, carry 64 items a trip; `WorkerParser.WORK` and the generator's `WORK` gain `courier`. A MISC entity with 16 health and no loot table, the Gathering Shade's body with a wicker pack (a generator variant, D-S2); not a Goetia identity | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P71 | 24 October 2026 | Its service is a spirit's: each delivery records `Practiced(worker_service, "spirit")` for its holder, present. The Binding Arts' "distinct 3" is unchanged, so the hand gives no shortcut to mastery and no fourth kind | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P72 | 24 October 2026 | The Stolas's Regard gate: `jugcraft:stolas_regard` MASTERED, asked in code by the anchor before it seals (`WorkshopHandEntity.regarded`; research unlocks name only invocations, workings and rituals). Asked only at sealing: calling back a lost hand, or the Regard later falling, never stops a sealed hand. As built beside unit 2, the gate names the id only; `check_hand`'s check that the research exists with a mastered state runs once unit 2's `tools/concordance_pacts.py` is in the tree | Proposed (unit 4, reviewable at the checkpoint) | D-03, D-08 |
| D-P73 | 24 October 2026 | Binding an anchor to a post: the anchor gains an optional `post` (`GlobalPos`). A Porter Key set on a Courier Post binds it, and the same key used while sneaking unbinds it, only with the Concordance on; never at a copy (`CONFLICTING_AGREEMENT`); only by one who may seal there (`maySeal(level, player, true)`: the owner or their party, where they may build, never a stand-in); only while the anchor keeps no agreement; only to a post in the anchor's level, loaded, within the definition's radius and one the player may use. The key's anchor branch asks `RateGate.allow(player, "workers", 4)` and comes before its container branch; the anchor's `useItemOn` passes for the key. A bound anchor seals a hand, an unbound one a shade | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P74 | 24 October 2026 | Who benefits lives on the record: `handSealing` seals with `UseMode.PARTY`, so the Bound Will's `pact.beneficiaries` is `party`, and `Agreement` gains no field (this supersedes the ticket's "the agreement gains optional `beneficiaries`"). Checked at each new claim against today's party; a load already claimed is finished (the review's correction 5) | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P75 | 24 October 2026 | Terms gate only new claims: a hold, being outside the hours, or the day's loads met stops new claims only; a claim already held is finished, as a shade takes its load home. Releases are noted: ended, its anchor broken or another hand answering, `agreement_ended`; killed or discarded, `worker_removed`; unloaded, its holder away or switched off, no release (the 200-tick lease lapses by itself). The ledger always owns the cargo | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P76 | 24 October 2026 | The area: a request's destination, and every source (a take-back source included), must lie within the agreement's radius of the anchor. A request bound outside gives `OUTSIDE_AGREEMENT`; one not a beneficiary's is skipped (`IDLE`) and left for other couriers; the hand serves the oldest request it may, past those it skips. With nothing it may serve, it gives the reason the post's next request is not its own | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P77 | 24 October 2026 | The quota counts loads: a claim ended by a delivery that emptied its cargo calls `taskDone` once (on the record and the anchor's copy); a part delivery counts nothing | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P78 | 24 October 2026 | A per-trip cap: `Logistics.reserve(..., int most, ...)` and `CourierLedger.reserve(..., int most, ...)`; the old signatures pass `Integer.MAX_VALUE`, so the porter is unchanged. Carry is read live from the definition, not sealed, as for the shade | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P79 | 24 October 2026 | `UNAVAILABLE_WORKER` stays unused: the hand's status (Jade, the roster, the anchor, `workers`) already says why it waits, and an open request stays visible at the post | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P80 | 24 October 2026 | One courier decision: `spirits/CourierWork` holds the claim's id and makes the porter's decision; its `Terms` supply ready, refuses, reaches, load, at its post, delivered and post gone. `Logistics.next(post, now, serves)` skips requests a courier does not serve; `next(post, now)` serves every one. Tests seal a hand under test terms through `anchor.handSealing(...)`, as unit 1 seals with test upkeep. The codex entry is at (4,4): (0,4) and (2,4) are unit 2's (the review's correction 1). Extends D-P15: the anchor also calls `jugcraft:workshop_hand` (`SpiritAnchorBlockEntity.call`); unit 1's row is left as written | Proposed (unit 4, reviewable at the checkpoint) | D-01, D-08 |
| D-P81 | 24 October 2026 | An absent requester's request at the head of a hand's queue holds the hand (`OWNER_OFFLINE`), as it holds porters: the review's optional correction 9 (skip such requests in the hand's own terms, so it serves those behind them) is not taken, so the two couriers keep one rule for absent requesters; recorded as a risk and left open at the checkpoint | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P82 | 24 October 2026 | A hand never takes up cargo stranded at its post that is more than its definition's carry (a porter's, which has no cap of its own; or its own after a data pack lowered its carry), and never claims cargo to be taken back to a source beyond its area (it could put that only into the post, and with the post full would claim and strand it again at each decision): both refused (`IDLE`) in its terms before the beneficiary and area check, and left to porters or to the requester's one recovery. Cargo it already holds whose source is beyond its area still goes into the post when cancelled, never to that source (D-P76). The unit review's findings 1 and 6 | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P83 | 24 October 2026 | A courier whose post is gone says why it waits: `CourierWork.Terms.postGone()` returns the status (a porter, which forgets its post, `IDLE` as before; a hand, whose anchor keeps it, `OUTSIDE_AGREEMENT`, as with no post). The anchor's use asks the switch alone, then only the definition of the worker it serves (a hand's for an anchor bound to a post with no agreement or keeping a `courier` agreement, `WorkshopHandEntity.WORK`; the shade's otherwise), so a data pack that drops one leaves the other's anchors working. The unit review's findings 7 and 8 | Proposed (unit 4, reviewable at the checkpoint) | D-08 |
| D-P90 | 24 October 2026 | Stolas's geometry stays the owner's file byte for byte (`OWNER_FILES`); "scaled up" is the renderer's `withScale(1.4F)` (unit 2's D-P41), the hitbox 0.7×1.45 | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P91 | 24 October 2026 | His skin, glowmask and animation file are adapted from Flashjack's own files by `tools/stolas_body.py`, written and checked by `tools/owner_art.py` (`ADAPTED`): each texel recoloured in place by part (from the geometry's box-UV faces) and kind (by hue), shading and alpha kept, nothing drawn: plumage black, markings blue-black, beak, legs and toes dark horn, eyes amber, crest and breast glyph old gold. The recolour serves D-S5's raven form, not originality | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P92 | 24 October 2026 | His eyes alone glow (`AutoGlowingGeoLayer`, the four eye texels) | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P93 | 24 October 2026 | His animation file is Flashjack's five clips unchanged plus nine of his own (`animation.stolas.*`): loops idle, listen, waiting and suspended (and the owner's `idle.flapping` as astir); gestures arrive, inspect, assent, refuse and depart. `attack`, `idle.air`, `idle.ground` and `idle.sitting` are kept, unplayed | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P94 | 24 October 2026 | Gestures play on a second controller (`gesture`, a STOP handler, no transition ticks, after the worker's own), not additively (GeckoLib's additive path subtracts the base rotations); each starts and ends on his listening pose. The server fires them, so a late joiner sees his state, never a replay | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P95 | 24 October 2026 | He listens while any conversation is open at his perch: a synced flag set at once when one opens (`listen()`), recomputed every think from the live conversations | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P96 | 24 October 2026 | He arrives when summoned (ARRIVE right after the entity is added: an invitation, a presence called back, a pact seated); the trigger reaches whoever tracks him then | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P97 | 24 October 2026 | Leaving takes DEPART's 40 ticks of game time: struck off at once, never saved meanwhile (`shouldBeSaved`); `gone()` (removed or departing) replaces `isRemoved()` wherever "he has left" is meant; the clip holds its last frame. As built, `stolas(level)` also asks that the presence is seated at this perch, so a copied perch moves nobody (the unit's critique 6). As fixed by the unit review: the end is a game time (`departAt`), reached by his own tick or, where his chunk is tracked but not ticking or unloaded around him (an unsaved entity is not unloaded), by a server-tick sweep (`registerSweep`); leaving clears his listening and sets a synced flag, and the client draws a Stolas leaving only where his departing gesture was fired, so one who starts watching then sees him gone | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P98 | 24 October 2026 | The waiting pose is photographed by switching the Concordance off in process (`JugcraftConfig.setFeatureEnabled`), restored in `finally` | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P99 | 24 October 2026 | Reactions: ACCEPT sealed, assent, refused, refuse; RECALL done, inspect, refused, refuse; DEPOSIT done, inspect; RESUME done, assent; none for the menu's own `not_offered`, CHOOSE, SUSPEND, DECLINE, END or SEAT. The first obligation (at an observatory) has no gesture. Pact screen only; the roster's actions fire no gesture (its answer's mark and narration are its two channels) | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P100 | 24 October 2026 | At most one cue of a kind every `CUE_TICKS` = 10 a Stolas; listening is not voiced within 10 ticks of his arrival; no cue while the Concordance is off, except his departure | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P101 | 24 October 2026 | Sounds `jugcraft:stolas.{arrive,listen,inspect,assent,refuse,depart}` through the generator, entity-attached, NEUTRAL, heard within 16 blocks, bound to runtime files (wing flaps, a spelunkery table's paper laid in and drawn out, the doorbell chime, the crow's call shared with `beast_crow`, a vesper's wing beats); no new audio, no voice. As fixed by the unit review: every file is mono (OpenAL places only a mono source), checked from each file's Vorbis header; the cave book's pages and the ghost's weep first bound are stereo | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P102 | 24 October 2026 | Jade names no player, shows no escrow, and reads through `AgreementHolder.peek` (no writes). The perch and the Spirit Anchor get one provider each; anchors show their counterpart and service, so the workshop hand is shown without a branch of its own. As built, a perch keeping nothing reads "visiting" only while its own visitor waits there (critique 7) | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P103 | 24 October 2026 | Each answer on the pact screen and the roster carries a drawn tick or cross, narrated "Done:" or "Refused:"; both screens are in-game screens and draw subtitles over themselves | Proposed (unit 3, reviewable at the checkpoint) | D-06, D-07 |
| D-P104 | 24 October 2026 | Subtitle evidence: CI has no sound device; the client test checks subtitles as defined and fired and never claims them seen there (an OpenAL null driver is left to unit 5 or the owner) | Proposed (unit 3, reviewable at the checkpoint) | D-06 |
| D-P105 | 24 October 2026 | Roster rows: the reader's Bound Wills in the order sealed (each with its presence in the same row), then the courier requests they filed (oldest first), then roster workers not already shown; at most 64; `more` counts the rest; the journal and the `workers` and `logistics` commands list everything | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P106 | 24 October 2026 | A roster request names a row by id (a record's, a worker's, or `UUID(0, request id)`) and that row's revision: it must be listed to this reader in the last state (else `not_listed`), at its current revision (else `changed`, with the roster again), and offer the action (else `not_offered`). The row's type never travels | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P107 | 24 October 2026 | Gates: suspend, resume and end share the Spirit Anchor's `workers`, 4 ticks; cancel and recover share the logistics commands' `logistics`, 10 ticks; opening has `roster`, 10 ticks, served by the server tick once allowed. As fixed by the unit review: an action its gate refuses is answered `too_fast` (with the roster last sent) at most once a gate's span; the rest are dropped, as the composer drops them | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P108 | 24 October 2026 | Suspend and resume from the roster are offered by the record's hold and are idempotent (a second suspend is not offered; resume on an upkeep hold answers `held_for_upkeep`). As built, a direct RESUME on a record not yet claimed answers `nothing` (the screen never offers it) | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P109 | 24 October 2026 | Every roster action drops a stand-in's request and refuses a dead reader (`not_permitted`), recovery included (the logistics command does not check life) | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P110 | 24 October 2026 | Switched off: rows listed, none offered, and an action answered `disabled`; records, escrow and requests untouched (D-S16) | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P111 | 24 October 2026 | The journal's pacts section, after the Regard's lines, lists each Bound Will held (name, place, next obligation; why it waits as the exact figure) and the last 8 departures, newest first, behind D-03's Binding Arts gate | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P112 | 24 October 2026 | The roster key `key.jugcraft.concordance_roster` ("Concordance Roster") is not bound at first and is listed on the encyclopedia's Keys page | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P113 | 24 October 2026 | Ars Nouveau helper kinds get their roster words (`HELPER_KINDS`, checked against `NouveauHelpers`' species): G13 closed, for the `workers` command too | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P114 | 24 October 2026 | A request row's "loaded" is the place its next step needs; a reserved or aboard request offers cancel only; recovery only of cargo the ledger holds stranded (08:69-70) | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P115 | 24 October 2026 | Portraits: a visual-only creature per entity type the server names (never added to a level or ticked, at most 16, forgotten on a world change), still; an item's icon otherwise. A record's row names its counterpart, a loaded worker its own type, an unloaded worker its kind's (`Roster.KIND_PORTRAITS`: familiar, the Hearthling; construct, the Clockwork Porter; pact, Stolas; any other kind `jugcraft:<kind>`), a request its item. As built, a `spirit` has none, so the Spirit Anchor's icon is drawn, since a Gathering Shade and a Workshop Hand share that kind (critique 8) | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P116 | 24 October 2026 | Pages of 8 rows; Page Up and Page Down turn them; the page scrolls within; rows past 64 counted on the last page | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P117 | 24 October 2026 | Ending asks first through `KeepFirstConfirmScreen` (vanilla's confirmation with the keyboard on Keep it; Esc keeps); the pact screen's End uses it too, refining D-P40. Cancel and recover do not ask (nothing is lost by either); the Cancel action's hint carries the server's words | Proposed (unit 3, reviewable at the checkpoint) | D-06, D-07 |
| D-P118 | 24 October 2026 | The open roster asks again every 40 client ticks; an open too soon is served later by the server, never answered "too fast"; a refresh that changes nothing rebuilds nothing | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P119 | 24 October 2026 | State and place marks are drawn shapes, one for each icon, always beside its word; no new texture | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P120 | 24 October 2026 | The roster is in the screen fit check (320×240) | Proposed (unit 3, reviewable at the checkpoint) | D-07 |
| D-P121 | 24 October 2026 | Unit 3 builds on the merge of `wip/chunk-d`, unit 2 and unit 4. As built, on `92868ff82` (units 1, 4 and 2 merged with their review fixes), so the specification's fallback was not needed (critique 9) | Proposed (unit 3, reviewable at the checkpoint) | D-06, D-07 |
| D-P122 | 24 October 2026 | The composer (Chunk A) is an in-game screen that draws the HUD's deferred subtitles over its own fill, as the pact and roster screens do (unit 3's R10 finding, left to unit 5); held by `check_composer`. Unseen: CI has no sound device | Proposed (unit 5, reviewable at the checkpoint) | D-09 |
| D-P123 | 24 October 2026 | The compact's resonance has one survival source, an observatory's keeper (a Starwatcher); the graph models it (`RESONANCE_SOURCES`; `pact_service` needs the perch, the compact's items and the resonance), the route understands Celestial Attunement, and the codex says so; unit 2's route passed only unmodelled. Alternatives for the owner: a resonance source outside the Starwatchers, or a compact without resonance | Proposed (unit 5, reviewable at the checkpoint) | D-03, D-04, D-09 |
| D-P124 | 24 October 2026 | The hand's step leaves the Overworld: the anchor's soul lantern (`recipe/spirit_anchor.json`) is the Nether's; the route names that step's dimensions and the check holds the Nether needed there alone | Proposed (unit 5, reviewable at the checkpoint) | D-08, D-09 |
| D-P125 | 24 October 2026 | The whole path is one server test in `ConcordanceJourneyGameTests` (as C-10): named at every step, selecting no client test, reusing `drone_depot`. The route's research is granted, but Stolas's Regard's three evidence kinds, the sky and every action after are the player's own through server calls; later times are passed to the methods that take them; ending is from the roster | Proposed (unit 5, reviewable at the checkpoint) | D-09, E7 |
| D-P126 | 24 October 2026 | `PACT_HALL` (nine recipe rows) and `CHUNK_D_ITEMS` (= `concordance_pacts.BLOCKS`), held by `check_acquisition` through one `made_as`, and loaded by `thePactHallsRecipesLoad`; Stolas and the hand are entities, the compact a cost, Star-reckoning a service | Proposed (unit 5, reviewable at the checkpoint) | D-09 |
| D-P127 | 24 October 2026 | The routes' "each kind of worker" stays three: the hand serves as a spirit (D-P70, D-P71), held by `worker_kinds_problems` | Proposed (unit 5, reviewable at the checkpoint) | D-08, D-09 |
| D-P128 | 24 October 2026 | The pact hall's load card is its own opted-in server game test, run in the existing `load-card` job after the workshop's, in a second `runGameTest` under the same `load_card` input and guard: not a new job and not a third span of Chunk C's card, whose code changes only by widened access to `Measure`, `Traffic` and `Stats` and a payload count | Proposed (unit 5, reviewable at the checkpoint) | D-09 |
| D-P129 | 24 October 2026 | The budget, in C-P19's form (C-P67): the pact hall adds at most +1.0 ms median and +2.5 ms p95 server tick, and +4 MiB retained heap beyond the baseline's change, net of the test's own work, on the stated runner. Reported, not asserted; asserted at +5.0 ms, +12.5 ms and +64 MiB. The owner decides at merge review | Proposed (unit 5, reviewable at the checkpoint) | D-09 |
| D-P130 | 24 October 2026 | The workload is D-09's As built's hall. Not in it: Star-reckoning (once a game day; its attunement is the sky's), an offering falling due, a warning or a suspension (the shipped period is 7 game days, so each look is measured in its steady state), ending, a second roster, an unloading chunk. Requests come every 400 ticks, about twice the rate the hand's quota of 32 a game day allows on average, so the span shows hands at work | Proposed (unit 5, reviewable at the checkpoint) | D-09 |
| D-P131 | 24 October 2026 | The simulated players' requests (answers, roster reads, filings, restocking) are the test's own work and out of the net figures, as the workshop card's simulated player's are; they are timed apart and reported, with per-call costs of a look, a think, a page and a roster listing timed after the spans | Proposed (unit 5, reviewable at the checkpoint) | D-09 |
| D-P132 | 24 October 2026 | D-09's rules are asserted from the run, not from the code alone: one presence a pact, one pact a holder, each pact looked at within `WRITE_TICKS + LOOK_TICKS`, the visitor listening and each hand its anchor's, sampled every 100 ticks while measured; items, escrow and both ledgers every 1,000 ticks. Any breach fails the card | Proposed (unit 5, reviewable at the checkpoint) | D-09 |
| D-P133 | 24 October 2026 | Stolas is photographed from his front: he is summoned facing south (yaw 0), so the pact client test's camera for his body, poses, sequence and Jade stands south of him, facing north, its floor extended there. Whether he should turn toward whoever speaks to him is the owner's (checkpoint question 1) | Proposed (unit 5, reviewable at the checkpoint) | D-06, D-09 |
| D-P134 | 24 October 2026 | A timed sequence through his refusal (08:176): four frames frozen at 0.25, 0.65, 1.05 and 1.45 s into the clip, each inside it and later than the one before, its time logged (`REFUSAL_SEQUENCE`) | Proposed (unit 5, reviewable at the checkpoint) | D-06, D-09 |
| D-P135 | 24 October 2026 | The workshop hand at work is shown by the roster's client test, whose world already seals a hand at a post: three copper ingots carried across the yard, frozen while aboard and photographed from beside its path, then delivered once; before the full roster, so its sixteen requests still fit | Proposed (unit 5, reviewable at the checkpoint) | D-08, D-09 |
| D-P136 | 24 October 2026 | The two-client attempt (D-S22) uses this branch's own two-JVM pattern, main's `OwnerCollectionsNetworkTests` (#435): `ConcordancePactNetworkTests` copies its launcher with its own names (that test is not edited), never #467's harness; a stranger (offline `PactPeer`) at the holder's perch; its evidence under `owner-network-evidence/`, which the shards already upload; recorded as two automated clients on one machine, never as two people | Proposed (unit 5, reviewable at the checkpoint) | D-04, D-07, D-09 (E1, E5) |
| D-P137 | 24 October 2026 | An opt-in Build input, `clients_without_optional`, runs the pact, roster and two-client classes without the optional integrations in a job of their own; ordinary runs never do (a workflow change for the owner's review) | Proposed (unit 5, reviewable at the checkpoint) | D-09 |

**The checkpoint's three questions** (09:174; at most three, 09:18):
1. Stolas's personality and appearance: the Flashjack-derived raven, his three dialogue beats and two memorable details
   (03:168, 03:174).
2. How strict pacts should feel after play: the period, the grace, the offering, and whether a pact should ever end
   after missed offerings; and the two routes around upkeep the unit review found: leaving for over a minute at least
   every 20 minutes renewed the grace (closed by D-P50, if accepted), and suspending, recalling and resuming once a day
   leaves the period counting only a few ticks of active time a day (open).
3. Which six-spirit cohort follows, from the twelve of 03:165.

The session's decisions are listed for review beside them in the checkpoint's technical handoff, not as further
questions.

## Where to resume

Chunk D (D-01 to D-09) is built on `claude/chunk-d-pact-hall` (code at `d00fa2167`; these records follow it) and in
review as [jimbozoomer-byte/jugcraft#474](https://github.com/jimbozoomer-byte/jugcraft/pull/474), the third of the stack. The owner answered the checkpoint (09:174) on 26 October 2026: D-D3 to D-D5. Before more work:
1. Done: the answers are recorded as D-D3 ("keep stolas"), D-D4 ("do D-P50", already built, so no code change) and D-D5
   ("gardens next": the garden cohort is the next chunk, on its own branch stacked on this one).
2. Refresh main and the open pull requests (REST, read only); merge main or Chunk C if they moved; run the Build workflow
   by workflow_dispatch with every client class, `load_card` ticked (paste the pact hall block verbatim under [The pact
   hall load card](#the-pact-hall-load-card), or record it as not run). Look again at the three pictures
   `b773913c4` and `d00fa2167` fixed (`jugcraft_pact_stolas_jade`, `jugcraft_pact_peer_arrival`,
   `jugcraft_pact_peer_refused`; seen fixed in run 40140478331) in that later run.
3. The owner named the cohort (D-D5): the garden spirits are the next chunk's work, not this branch's.
4. Open items: Stolas's facing; one second-client helper (Deferrals); the opt-in workflow inputs await the owner's
   review.

## How this contract was checked

- **Re-read at `779851cdb` for this page:** `Agreement`, `Bond`, `BoundWill`, `BoundWillLedger`, `BoundWills`,
  `SpiritAnchorBlock`, `SpiritAnchorBlockEntity`, `GatheringShadeEntity` (its decision, departure and save),
  `HearthlingEntity` (its decision, removal and bond codec), `WorkerEntity`'s status, think and registration lines,
  `WorkerRoster`, `Workers`, `Status`; `Authority` (present to `mayChangeFor`); `AstrolabeItem` (charge and recall),
  `AstralClaims`, `Calendar`, `Sky` (season, obscured, up, pulse, command), `ObservatoryBlockEntity` (use and observe),
  the six pattern files; `Spires`' upkeep; `Styxhexenhammer`, `StyxState`, `JugcraftStyx`, `StyxFlowerBlock`, Styx's
  lang lines and `tools/styx.py`'s; `TownState`'s district default; `Journal`'s caps and contributors; `Encyclopedia`'s
  pages and `EncyclopediaPayload`'s cap; `ShopMenu`'s checks; `JugcraftJadePlugin`; `RitualDefinition`'s results; the
  exclusion tag; `TestPlayers`; `AlchemyArtifactGameTests.player`; `Code`; `CourierPostBlockEntity.mayUse`;
  `CourierLedger`'s cargo rule and `recover`; `Couriers.KEY_POST`; the porter's courier decision and removal (by line);
  `CrucibleBlockEntity`'s shortage and remainder (by line); `MobTransportCrate`'s `owned` and `friendly`;
  `NouveauHelpers`' roster use and `NouveauHelper.kind`; `LandProtection`'s callbacks; `Logistics`' limits;
  `EvidenceRule`'s notes rule; `ConcordanceProgress`' record and listen; `RulesParser`'s unlocks;
  `ProgressionGraph`'s practice gates; `binding_arts.json`; the practice gates `cultivation`, `assay`, `observation` and
  `worker_service`; the specimen tags; the anchor's loot table; `values.json` for the amethyst shard, feather, bone meal
  and potted Stolas Starflower; the commission `bound_service`; `tools/check_mod_data.py`'s `check_workers`,
  `check_geckolib`, `check_persistence` and the encyclopedia's forbidden list; `tools/concordance_workers.py`'s `WORK`,
  `STATUSES` and `WORKERS`; `tools/concordance.py`'s sound tables and `write_all`; `tools/generate_material_data.py`'s
  `sounds.json`; the named suites' `@GameTest` counts and the test lines cited; `docs/ROADMAP.md:26-34`,
  `styxhexenhammer.md:37`, `arcane-concordance-workers.md:145-146, 191-192, 213-214`, `docs/TESTING.md:5, 43`,
  `CLAUDE.md:57-58, 64`; plan 09:1-35 and 09:146-174, 09:294-315, 03:25-42 and 03:118-195, 10 (whole), 02:16, 41, 83,
  129, 146-153, 169-170, 06:26, 117, 152, 158, 164, 08:8-22, 55-127, 140-182, 11:51-61, 12:54, MP:50-87; Chunk C's
  contract at the lines cited.
- **Read in the owner library (nothing executed):** Flashjack's geometry (bones, UV style, sheet), animation (clips) and
  hashes; the Ars Elemental metadata; `PROVENANCE.md`; the runtime sound files named in D-06.
- **In `mc263src` `HEAD`:** `GameTestHelper.makeMockServerPlayerInLevel`, `Player.isCreative` and
  `hasInfiniteMaterials`, `ServerPlayer.gameMode` and its play-time award, `Stats.PLAY_TIME`; an explosion's block
  removal (`BlockBehaviour.onExplosionHit`, `LevelChunk.setBlockState`).
- **Read by REST on 23 October 2026 (read only):** the open pull requests (20, at 22:15Z), main's head, the branch
  `claude/chunk-d-pact-hall`, and `/pulls/N/files` for #452, #453, #462, #463, #467, #469, #470, #471 and #472
  (#452's patch to `MobTransportCrate.java` read). A verification pass at 22:43Z read again the open pull requests (21:
  #473 new), main's head, the branch and its pull requests (none), #465's and #466's mergeability, reviews and check
  runs, `/pulls/N/files` for every open pull request (#461's capped by the listing), and the patches of #452
  (`LandRegistry.mayBuildFor`) and #463 (`LandProtection`).
- **Also re-read after the first draft, so no longer marked *(survey)*:** `BookArt`'s `Paragraph` and `Tab`,
  `ComposerScroll` and `TransportCratePreview` (that they exist), `EncyclopediaKeys`' key list, `GraveThrallEntity`'s
  trigger, `CinderTyrantClientGameTests`' tick freeze, `mc263src`'s `MinecraftServer.handleCustomClickAction` and
  `ConfirmScreen`, the doorbell chime, `concordance_worker_models.py`, `concordance_journey.py:93`, `NouveauFamiliars`
  and `NouveauHelpers`, the Academy's lesson and section, the Murmur names, `Examination`'s branches.
- **Taken from the survey reports, the delivery plan or its critique without re-reading, and marked *(survey)*:**
  #461's files and mixin (its file list exceeds the REST listing); the owner library's lack of owl or raven art; the
  widgets' internals beyond their declarations; GuiLib's lack of narration and the repository's lack of its own
  conversation, confirmation or toast precedent. #463's, #467's and #462's contents, #452's
  `LandRegistry.mayBuildFor`, #465's and #466's CI state and the shared files of #468, #459, #455, #454, #449 and #448,
  marked *(survey)* in the first draft, were read by REST in the verification pass.
- **Not determined:** whether a GeckoLib entity draws as a roster portrait; whether Flashjack's UV regions pass
  `check_geckolib`; whether two clients can be run here. Since determined: two client JVMs can run on one runner, on
  main's #435 pattern (D-P136).
- **For unit 1's As built notes and proposals (24 October 2026):** re-read at the unit's head `fa5360d0d`: `Pacts`,
  `BoundWill`, `BoundWillLedger`'s additions, `BoundWills`' codecs and methods, `PactHistory`, `AgreementHolder`
  (whole), `SpiritAnchorBlock`, `SpiritAnchorBlockEntity`, `GatheringShadeEntity`, `HearthlingEntity`'s and `Bond`'s
  changes, `Authority.present`, `presentNear` and `answering`, `Status`, `WorkerEntity.sign`, the generator tables and
  audits this unit changed, and every new test's name and Javadoc; the unit's specification and its review's 16
  corrections. Run: the unit's audit mutations, the parse check, the pure classes' `javac` and harnesses, client-test
  selection and the records commit's local checks (D-02's As built).
- **For unit 2's As built notes and proposals (24 October 2026):** re-read at the unit's code head `ee82c1dc7`: every
  new and changed Java file against the unit's specification (the new classes match it token for token apart from the
  review's corrections and the departures named), the new tests' bodies against the specification's, the generator
  tables (`tools/concordance_pacts.py` whole), `concordance/pact/stolas.json`, the tags, the recipe, the value of the
  perch, the six pattern files (for the balance figures), the runtime Stolas files' SHA-256 against the library's
  catalog rows and the leftover metadata's lines; the unit's specification, its four part designs' decisions and the
  review's ten corrections. Run: the parse check, the pure classes' `javac` and three harnesses, client-test
  selection, the perch icon against its map, and the records commit's local checks (D-05's As built); the per-commit
  audit-mutation results are those logged when each code commit was built.
- **Run for this page, after the verification pass's corrections:** `python3 scripts/check_repository.py` (passed:
  repository structure and local documentation links) and `git diff --check` (clean; the page is untracked, so it was
  also checked as a new file with `git diff --no-index --check`, clean). Not run: any build, game test, client,
  generator or other check.
- **For unit 4's As built note, proposals and schema review (24 October 2026):** re-read at the unit's head `e51ef2b11`:
  `CourierWork`, `WorkshopHandEntity`, the anchor's, its block's and the key's changes, `Logistics`' and
  `CourierLedger`'s additions, the porter's terms, the generator tables and audits the unit changed, and every new
  test's name and Javadoc; the unit's specification, its three part designs and its review's nine corrections; unit 2's
  `wip/chunk-d-u2` at `832129b95`, read only (`stolas.json`, `stolas_regard.json`, `PactPerchBlockEntity.sealing`,
  `AgreementHolder`'s additions, `PactKeeping`, the codex positions in `tools/concordance_pacts.py`, and where its edits
  sit in the files both units change) for the schema review and the merge points. Run for these records: the parse
  check (19 Java files), M13's two forms again, client-test selection and the records commit's local checks; the other
  mutations ran for `e51ef2b11` (D-08's As built).
- **For the unit review's fixes (24 October 2026):** the review's 13 confirmed findings checked against the code at
  `0d04161a5` before each change; re-read for the merge notes: unit 2 at `wip/chunk-d-u2` `8f0635d9e` (`WorkerClient`,
  `check_concordance`'s calls, the test tables, `AUTHORITY_ROUTES`, `StarReckoning.observed`, `AgreementHolder`'s
  constructors and members, its decision numbers, and a trial three-way merge of each file both units change) and unit
  1's review fixes at `da185dd0d` (`WorkerEntity.setStatus` and `joinsRosterOnStatus`, `check_pacts`' needle and the
  files both change), read only. Run: the parse check, the generator, the nine new mutations and four old ones, and the
  fixes commit's local checks (D-08's As built).
- **For unit 3's As built notes and proposals (24 October 2026):** re-read at the unit's code head `66b741868`: the four
  code commits' messages and diffs against the reconciled specification, its critique's twelve corrections and the
  nine roster tests it names; `StolasEntity` (loops, gestures, departure, cues, `reaction`, `loop`),
  `PactPerchBlockEntity` (`stolas`, `heard`, `react`, `summon`), `PactReading`, `PactDataProvider`, `Roster` (its
  Javadoc, gates, answers, listing, record rows and revisions), the payloads' ids and limits, `RosterClient`'s key,
  `RosterScreen`'s pages, `Workers.STOLAS`, `WorkerClient`, the generator tables (`tools/concordance_pacts.py`'s D-06
  sections, `tools/concordance_roster.py` whole), `D06_TESTS` and `D07_TESTS`, the test classes' `@GameTest` counts and
  the generated files' SHA-256 (`tools/owner_art.py --provenance pacts` and `sha256sum`); unit 2's records at
  `8f0635d9e` and its merge fixes at `92868ff82`. Run for these notes: the records commit's local checks and its
  mutations (D-07's As built, the records' commit).
- **For unit 5's records (24 October 2026):** re-read at `b773913c4`: the Build workflow's client jobs and upload
  paths, `ConcordancePactNetworkTests`' evidence folder and second-client heap, and `b773913c4`'s diff; from the
  session's reading of the runs by REST (read only): run 40133540741's job list, the load card's block, the shard and
  opt-in job logs' previews and `[pact-check]`, `[roster-check]` and `[pact-network]` lines, and the results of runs
  40120355940 and 40121823509. Run: as in D-09's As built.
