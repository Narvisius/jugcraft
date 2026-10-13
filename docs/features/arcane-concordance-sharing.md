# Arcane Concordance: shared records and typed resources

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap steps 6 and 7) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 2, Initiate stage (Jugcraft Discovery). Roadmap steps 6 and 7.
Primary specialty and supported player role: Lampwrights (Radiance), with a collaboration milestone (shared notes)
and a service role (keeping other players' lamps lit).

Builds on [First Light](arcane-concordance-first-light.md). Contract and checklist:
[ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

**Research Notes (step 6).** Craft two blank sheets from paper, an ink sac and a feather. Use a blank sheet to write
down every research entry you have begun, as far as you have come, up to understood (mastery is never written down).
Hand the sheet to someone else: using it reads it. Where an entry accepts notes, reading them is a third way to
understand it (beside studying at the bench and examining three specimens in the dark), but the reader must still
encounter and observe it themselves, and must still practise to master it. So a player who reads a Lampwright's notes
on First Light only needs to examine one specimen in the dark to understand it, rather than three. The sheet is not used
up, notes from the same writer count once, and your own notes teach you nothing new. The codex's Research Notes page
explains this; the First Light page now names the third route.

**Lumen Sconce (step 7).** A brass lamp-stand (one amethyst shard over four copper ingots) that burns Radiance for a
steady light 15: one measure a minute, up to 64 (just over an hour). Use it with a Kindled Lantern to pour up to 16 in;
what does not fit stays in the lantern. Anyone may pour, so a Lampwright can keep a town's lamps burning; only the
player who placed it can sneak-use to draw Radiance back out. Use it empty-handed to read its charge. Broken, it keeps
its Radiance on the item and burns it again when placed.

## Connections

- Input producer: First Light research (notes record it); Kindled Lanterns from the Lampwright's Bench (the sconce's
  only fuel); copper and amethyst.
- Output consumer: other players' research (notes); lighting for bases, roads and towns (the sconce is a Radiance sink
  that makes the lantern economy useful beyond carried light).
- Technology connection: none new; Ley Charge is defined as its own type and not linked to Jugcraft's energy (JE).
- Magic connection: notes are the first shared record; the sconce is the first block container for an essence.
- Reachable entry path: unchanged. First Light still has no prerequisites, and every route to understanding it needs
  the reader's own encounter and observation.
- Solo, trade and cooperative routes: solo players lose nothing (the two existing routes stay); notes are a
  collaboration shortcut; sconces can be filled by anyone and their Radiance traded as lanterns.
- Specialty use without other branches: a sconce needs only Radiance; notes need only paper and someone who knows.

## Typed resources (step 7)

`concordance/resource/` holds the rules every resource container follows. It is pure Java (no Minecraft types), and the
game tests and a standalone harness exercise it:

| Kind | Type ids | Container | Moves between containers | In use now |
|---|---|---|---|---|
| Focus | `focus` | the player's `FocusPool` | never (only spent, or converted by recipe) | yes |
| Ley Charge | `ley_charge` | `Reservoir` | yes | no (rituals, step 12) |
| Principle Essence | `essence/<principle>`, ten types | `Reservoir` | yes, only into the same Principle | Radiance in lanterns and sconces |
| Vitae | `vitae` | `Reservoir` | yes | no (step 16; never drawn from health except by an offering) |
| Astral Resonance | `astral_resonance` | `Reservoir`, with `AstralLedger` for claims | yes | no (step 15) |
| Bound Will | `bound_will` | `BoundWillLedger` records | as whole records, by their holder, if transferable | no (step 17) |
| Prima Materia | `prima_materia` | `Reservoir`, valued by `PrimaValue` | yes | no (step 21) |

- **Containers** hold one type, in whole units (`long`), with a capacity and a stated overflow: `REJECT` (all or
  nothing), `FILL` (the rest stays with the sender) or `VOID` (the rest is lost, only where a sink says so).
- **Transfers** (`Transfers.move`) check the type (a transfer never converts), portability (Focus never moves),
  ownership (owner and members take out; others may only put in where the container is open for offerings), a rate
  budget per container (a limit per window of ticks) and capacity, then return both containers whole. Nothing is half
  done and the total is conserved.
- **Conversions** are data (`data/<ns>/concordance/conversion/*.json`); nothing converts without one. The table refuses,
  with a named reason, any recipe that would close a loop returning as much as went in. The bench's Channel working now
  names its conversion (`jugcraft:focus_to_radiance`, 6 Focus to 2 Radiance) instead of carrying its own numbers.
- **Allocation** of a short supply between several requests (`Allocator`) is deterministic: higher priority first;
  within a level, shares in proportion to demand (whole units), the spare units to the largest remainders and ties to
  the smaller id; nobody gets more than asked; what nobody asked for stays with the supplier. No network uses it yet
  (Ley conduits arrive with rituals); it is tested so the first network starts from a fixed policy.
- **Bound Will** records are identities (id, agreement, counterpart, holder, transferable), never amounts: sealed once,
  handed on whole by their holder only, at most 32 per holder.
- **Astral Resonance** claims are kept per pattern: the latest occurrence that paid and the game time it did. A later
  occurrence pays only once enough game time has passed, so moving the world's clock back or forward never pays twice
  (made monotonic in step 15, [celestial](arcane-concordance-celestial.md); it had kept five days of events by time,
  which a clock moved far enough could have reopened).
- **Prima Materia** values are exact fractions, rounded down once on a batch's total.

Fabric's Transfer API is not used yet: no pipe or automation carries essences, and the sconce is filled by hand. When
Ley conduits arrive, a Fabric `Storage` adapter over `Reservoir` can wrap these rules rather than replace them.
Jugcraft's energy interface (JE) is deliberately not reused for Ley Charge: they are different resources and no
conversion between them exists.

## Balance and automation

- Notes: 1 paper + 1 ink sac + 1 feather = 2 sheets. Reading costs nothing and does not consume the sheet. Notes save
  the reader two of the three dark examinations, or the bench study's specimen. They never save mastery (Kindle in
  8 chunks).
- Sconce: 64 measures of capacity, 1 burnt per 1200 ticks; 16 per pour or draw; at most 32 per sconce per 20 ticks.
  Radiance comes only from specimens (bench infusion) and Focus (6 to 2), so a sconce turns gathered specimens or
  time into lasting light. No conversion produces anything from Radiance, so no loop can gain.
- No automation: the sconce and the bench have no hopper or pipe faces.

## Multiplayer and persistence

- Server authority: writing and reading notes, pouring and drawing all run on the server, limited to one action per
  player every 10 ticks (`RateGate`) and, for the sconce, a per-sconce rate budget. Notes are evidence keyed by author,
  so a duplicate or repeated read records nothing. A player's own notes are refused. The reader's research changes only
  through the same engine as every other piece of evidence.
- Ownership: a sconce records the player who placed it; only they may draw from it. Anyone may pour in. Sconces with no
  recorded owner are open to all.
- Persistence: notes are an item component (author UUID and name, entries capped at understood and at 32, written
  time). The sconce saves its Radiance, clock and owner, and drops with its Radiance (`copy_components`). The codecs
  and the item round trip are tested.
- Disable behaviour: with `concordance.enabled=false`, writing and reading notes and pouring and drawing are refused;
  everything stays registered.
- Unearned pages: the codex unlocks Kindle and the lantern pages only through each player's own research advancement,
  which notes alone never award (tested).
- Jade and JEI: the sconce's Jade snapshot is its Radiance only; the bench's names no player. JEI shows the vanilla
  crafting recipes; no research is revealed through either.

## Dependencies and assets

No new dependency. The Research Notes textures (blank and written) are drawn by `tools/concordance_art.py`; the sconce
model reuses the bench's brass and lens textures. Everything is original.

## Verification

- `python3 tools/check_mod_data.py` passes (1442 IDs). New checks: conversions (valid types, whole amounts, no loop
  without loss), the channel working's conversion, notes never on mastered, the Java constants for the sconce and notes,
  the Java Principle and resource lists against `tools/concordance.py`, and every sconce message key. Each new Java
  check was shown to fail on a deliberately wrong value.
- The pure core (`concordance/rules`, `concordance/resource`) compiles with JDK 21 and passes two standalone harnesses
  against the generated data: 49 checks (milestone 1) and 51 checks (steps 6 and 7).
- Game tests added: `ConcordanceSharingGameTests` (notes between two players, codex pages unearned until observed,
  duplicates, own notes, mastery not written, the use path and its rate limit; sconce pour, refused second pour, a
  stranger's draw refused, the owner's partial draw, the rate budget, burning, conservation, Jade, and keeping its
  Radiance when broken) and `ConcordanceResourceGameTests` (the step 7 rules above, run on the test server).
- CI, run 37505487232 (Build workflow, run manually on this branch): `mod` passed with **"All 837 required tests
  passed"** (the 6 new tests included; the rules loaded with 0 problems and the codex with no errors), and
  `optional integrations absent` passed. The client test shards fail before any test starts, with the same OpenGL
  startup crash as on the framework foundation branch (see [First Light](arcane-concordance-first-light.md#verification)).
- After that run, the sconce's scheduled check was capped at one burn period (see Rollout). CI run 37506284755 on that
  commit (`4d04cb0e`): `mod` passed (build and server game tests) and `optional integrations absent` passed; the client
  shards failed with the same OpenGL startup crash.

Not yet run: any client (sconce model, notes textures and tooltips in game), a two-client dedicated server, a GuiLib
research comparison screen (not built: `/jugcraft concordance status` and the codex are the fallback).

## World and event applicability

No worldgen, creatures, loot or seasons. Notes are paper; sconces are placed blocks under the usual protections.

## Rollout and open questions

- New stable ids: item `jugcraft:research_notes`, component `jugcraft:research_notes`; block, item and block entity
  `jugcraft:lumen_sconce`; conversion `jugcraft:focus_to_radiance`.
- The channel working's data changed shape (it names a conversion). Nothing has been released, so there is no old
  data to migrate.
- Sconce ownership is the placer only; party members cannot draw yet.
- A sconce checks its charge at least once a minute (one burnt measure), so after a partial draw its light can stay on
  for up to a minute after the Radiance runs out (the level keeps one pending tick per block).
- The allocator, Bound Will, Astral and Prima rules have no in-game user yet; they are fixed and tested now so the
  steps that use them start from settled rules.
