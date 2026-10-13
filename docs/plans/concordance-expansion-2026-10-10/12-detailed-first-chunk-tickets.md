# Detailed first-chunk tickets: usable foundations

This chapter makes Chunk A directly assignable. It is not an additional roadmap chunk. Names marked **proposed** describe a contract; locate current equivalents before creating classes.

Read [the chunk plan](09-claude-delivery-chunks.md) and [validation rules](10-validation-and-checkins.md). Deliver a complete usable experience before adding future spell types or more bosses.

## A-00 — Establish the baseline

1. Record base commit, working tree state, framework lock and relevant open contributions.
2. Read the journal, composer, authority and boss reward code below.
3. Run required baseline checks appropriate to the intended edit. Separate pre-existing failures from regressions.
4. List actual acquisition routes for the first wand, research tools and selected equipment.
5. Identify generator owners for language, recipes, models and test expectations.
6. Correct stale status text only after verifying code and merge state.
7. Capture the current journal in the client, if available, for before/after evidence.

Do not change dependency versions, reset another branch or declare all creative-only equipment craftable because one recipe was added.

## A-01 — One authoritative composition service

### Existing source

Under `src/main/java/io/github/jimbozoomer/jugcraft/concordance/`:
- `ConcordanceCommand.java`: current composition command.
- `ComposedSpells.java`: `instrument(player)`, `compile(player, instrument, text)` and impact execution.
- `ComposeText.java`: localized compiler explanation.
- `compose/Compiler.java`, `CompositionParser.java`, `Plan.java`, `Instrument.java`, `Text.java`.
- Existing inscription registration in `JugcraftConcordance`.

The command checks the feature switch and held instrument, uses RateGate with a 20-tick composition limit, compiles, explains and writes an Inscription from the compiled plan. The screen must share this behavior.

### Work

1. Extract a small common service only if needed to reuse the command logic from networking.
2. Accept server player, bounded expression, operation (preview/inscribe/clear) and expected item/rule revision.
3. Resolve the actual instrument from server state.
4. Apply current feature, knowledge and rate checks.
5. Compile through ComposedSpells.
6. Return structured diagnostics using existing Text/ComposeText concepts.
7. On inscription, write the existing component from the compiled plan.
8. Keep commands as adapters to the same service.
9. Preserve existing casting payment. This ticket is not a delayed-cast rewrite.

**Acceptance:** command and UI produce the same plan/errors for the same player, expression and instrument.

**Cases:** no instrument; disabled feature; unknown word; excessive length; illegal branch; legal current spell; clear; item swap before commit; unchanged command behavior. Extend ConcordanceComposeGameTests and relevant adapter tests.

## A-02 — Bounded screen protocol

### Existing foundation

The journal has `concordance/journal/Journal.java`, `JournalLine.java`, `JournalSection.java`, `JournalRequestPayload.java` and `JournalPayload.java`.

Client files include `JournalClient.java`, `JournalScreen.java` and `JournalWorkspace.kt`. Read their registration/scheduling patterns. Do not send chat commands as a private GUI protocol.

### Proposed messages

| Message | Client intent | Server responsibility |
|---|---|---|
| OpenComposer | Intended hand/slot, if necessary | Actual instrument, learned parts, budgets, inscription, bounded session/revision |
| PreviewComposition | Sequence, bounded text, expected revision | Normalized text, cost summary or structured problems |
| InscribeComposition | Sequence, bounded text, expected item/rules | Recompile, mutate once, return actual inscription |
| ClearComposition | Expected session/item state | Validate and clear eligible item |
| ComposerChanged | No trusted gameplay data | Refresh current authoritative state |

These names are proposals, not existing declarations.

### Bounds

- Retain the 256-character limit and existing compiler caps.
- Retain the current one-per-second server composition gate initially. Local editing is immediate; authoritative preview is debounced.
- Share rate accounting with commands so alternating paths cannot double work.
- Ignore stale response sequences.
- Bind the edit session to current slot/instrument state through an existing menu-style check or server-issued revision.
- Do not add persistent UUIDs to every wand merely to open a screen.
- Recheck item and rules on commit.
- Bound catalogue/diagnostic arrays and never return exception traces.
- Keep GuiLib classes out of payloads and common handlers.

**Cases:** replay, out-of-order response, rapid command/UI alternation, inventory swap, datapack reload, disconnect/rejoin, malformed length and unknown part. Final inscription must match server state.

## A-03 — Functional composer and optional rich view

1. Build one draft model for delivery, selection, operations, modifiers and legal branches.
2. Serialize through the existing normalized grammar and load current inscriptions back into that model.
3. Implement a functional native screen with readable text fallback.
4. Add GuiLib presentation over the same model.
5. Filter from server-provided learned parts.
6. Show capacity, Focus estimate, target/work limits and localized reasons.
7. Support keyboard selection, removal, reordering and local undo.
8. Preserve the draft across tabs; clear stale/private server state on disconnect.
9. Make Inscribe explicit. Draft editing alone never mutates an item.
10. Reopen and cast through the existing Spell Engine integration.

**Visual evidence:** ordinary window, small window, high GUI scale, narration, color-independent errors and reduced motion.

**Deferred:** moving projectiles, unrestricted scripting, increased compiler caps and new spell-power rules.

## A-04 — Encyclopedia entry and one complete route

1. Inspect the owner's encyclopedia checklist and ten reference images.
2. Add inventory entry and configurable key using current client hooks.
3. Open one shared page/controller model.
4. Populate useful Discover, Spellcraft and Expedition pages from actual server state.
5. Link existing Modonomicon references where useful.
6. Preserve the J journal and explain key bindings.
7. Distinguish required, recommended, trade-supported and optional-boss edges.
8. Begin with guidance over existing research. Quest reward policy remains an owner decision.
9. Complete the route from observation to a learned, useful spell.

**Acceptance:** a fresh player follows it without commands or source-reading. Unknown discoveries stay hidden as intended. No page advertises an absent recipe or entity.

## A-05 — Contribution without rewriting combat

### Existing source

Inspect `lair/vesperine/VesperineEntity.java`, `VesperineLoot.java`, `lair/tatterlace/TatterlaceEntity.java`, `TatterlaceLoot.java`, `LairBosses.java` and `ConcordanceEffects.java`.

The boss records damage participation and passes eligible players to its reward function. Add contribution at that seam; do not replace attack state machines.

### Proposed record

Per encounter and actor, store bounded counters/timestamps for effective hostile damage, qualifying healing, measured ward absorption, distinct objectives, relevant presence and eligibility. Cap actor records to the instance's supported participants and authorized helper owners. Avoid an unbounded event history.

### Initial supported events

1. Existing boss/add damage.
2. Vesperine ward activation during the eligible phase.
3. Tatterlace ally-thread rescue through the actual break/sever path.
4. Effective healing through current shared effect application.
5. One ward path where actual absorbed damage is measurable.

Do not claim all external healing or arbitrary armor prevention is integrated.

### Healing algorithm sketch

Maintain a short-lived capped budget of unresolved encounter-caused injury for each eligible player. On a qualifying heal, credit no more than actual health gained and no more than unresolved injury, then reduce that budget.

Overheal, friendly/self-damage farming and refreshing a ward that absorbed nothing score zero. Adapt this proposal to actual damage/heal events. Ordinary consumables and other healing sources must be explicitly assessed, not accidentally excluded. A healer should not need a token attack.

### Objective algorithm sketch

Key an objective by encounter, phase occurrence and fixture/action. Count a successful transition once. Clicking an already lit ward repeatedly does not help. A later legitimate ward occurrence can count separately.

Eligibility is capped and threshold-based. More spam after qualification does not improve loot.

**Cases:** damage-only, healing-only, objective-only, overheal, self-damage then heal, repeated clicks, pet ownership, outsider, late arrival, qualified death before victory and reconnect.

## A-06 — Rewards and lifecycle

1. Preserve first-kill guarantees, loot tables, seasonal extras and XP unless intentionally changed.
2. Feed the expanded eligible set into the current reward seam.
3. Introduce or reuse an encounter/player reward receipt where needed.
4. Define roll, storage, delivery and acknowledgement.
5. Test duplicate defeat callbacks and reset/defeat races.
6. Define inventory-full behavior without a second authoritative copy.
7. State crash recovery limits precisely.

A boolean alone does not provide atomic delivery across world saved data, player inventories and dropped entities. Inspect GraveGoods and actual save boundaries. If a first implementation guarantees duplicate suppression during normal/rejoin paths but lacks full crash atomicity, document that limit and its recovery approach.

Persistent rewards do not imply that an active lair fight resumes after restart.

## A-07 — Acquisition and presentation

For each selected item record:
- Existing ID, model and role.
- Current acquisition and gap.
- Actual registered recipe/trade/loot ingredients.
- Knowledge requirement and explanation.
- Comparison with current Ember/Thallite equipment.
- Equivalence exclusion or justified mundane eligibility.
- Owner asset provenance and real runtime view.

Ritual fault reports and pylon charge inspection already exist. Extend current providers and expose their validated data; do not implement another structure validator for a new UI. JEI must show actual acquisition and preserve honest server-datapack limitations.

A cast pose cannot override an active gun reload or ArmsMotion-owned action. Test first/third person and remote playback where relevant.

## A-08 — Complete demonstration

Show a fresh player:
1. Finding a clue and learning through ordinary practice.
2. Opening the composer.
3. Getting a clear illegal-edit error.
4. Inscribing and casting a valid composition with correct payment.
5. Acquiring the selected useful equipment in survival.
6. Contributing to an existing boss through support/objectives.
7. Receiving the correct reward and returning safely.
8. Saving/reopening and retaining expected state.

Run relevant repository checks and client tests. Include screenshots of the composer, route page and inspection state. Two-account testing is recorded separately from automated single-client evidence.

Deliver the checkpoint from chapter 10 with exact commit, evidence, limitations and next recommendation. Stop before Chunk B for owner input.

## Review boundaries

- Unit 1: baseline/acquisition map and shared composition service.
- Unit 2: payloads plus native/rich composer.
- Unit 3: contribution and reward lifecycle.
- Unit 4: encyclopedia route, acquisition and presentation.
- Unit 5 if needed: combined fixes and evidence.

These are review boundaries, not repeated permission questions. Finish the authorized chunk's player experience.
