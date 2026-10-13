# Arcane Concordance: the Concordance Journal

Status: implemented on branch `claude/concordance-steps-26-32`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 26) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 18, every stage (Initiate to Architect). Roadmap step 26.
Primary specialty and supported player role: every player of the Concordance; no tradition of its own.

Builds on every Concordance feature's own report (research, [stages](arcane-concordance-progression.md),
[Vitae](arcane-concordance-vitae.md), [the sky](arcane-concordance-celestial.md), [workers](arcane-concordance-workers.md),
[deliveries](arcane-concordance-logistics.md), [relics](arcane-concordance-relics.md), [curses and
wards](arcane-concordance-hexes.md), [the Conclave](arcane-concordance-conclave.md), [spires](arcane-concordance-spire.md)
and [Prima Materia](arcane-concordance-equivalence.md)). Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## What already existed, and what this reuses

Every feature already answered for itself: a device with an empty hand, Jade, and a chat command per tradition
(`/jugcraft concordance status`, `stage`, `vitae`, `sky`, `workers`, `logistics`, `relics`, `hexes`, `conclave`,
`spire`). Nothing showed them together, the research status spoke in rule ids (`examine 1/3`), and a stage route's
needs were the graph's own data (`research jugcraft:first_light@mastered`). This step adds no rule: the journal shows
the reports the commands print, each feature still writing its lines once.

The library roles, each with one purpose:

| Library | Role |
|---|---|
| Modonomicon | The codex: what things are and how they work, generated from the same definitions the server uses (`tools/concordance*.py`); a new Foundations page explains the journal. |
| JEI (optional) | Recipes and uses, as before. |
| Jade (optional) | One block at a glance: its state and why it rests, as before. |
| GuiLib (optional) | The journal's workspace. Without GuiLib, the plain journal shows the same. |
| Cloth Config and Mod Menu | The Concordance's settings, now with exact values and the simple journal. |

## Player experience

**J** (Controls: *Concordance Journal*, remappable) opens the journal. It asks the server, which answers with what it
knows of this player now, in sections:

- **Overview**: Focus, the stage reached, how many research entries are met and mastered.
- **Research**: every entry the player has met, its state and what its next state asks, in the words of that state's
  advancement ("Understand circles: study a circle specimen at a Lampwright's Bench"); or which entry must come first.
  Entries not yet met are only counted, never named.
- **Stage**: the stage and, for the next, every route and what it still needs, in words: "First Light mastered",
  "5 entries mastered (you have 2)", "the Conclave rank of Starbound", "a Concord Spire raised, or helped to raise".
- Once their research is met: **Vitae**, **the Sky** (the forecast), **Workers**, **Deliveries**, **Relics**, **Prima
  Materia**; **Curses and Wards** once Sympathy is met or while a curse or ward touches the player; **the Conclave**
  for its members; **Spires** for those who keep one.

**Exact values** add, after a line, the figures behind it: how many of what each way to the next research state needs
("examined 1 of 3; or studied 0 of 1"), and caps. They are off by default; the journal's own switch (or **X**) and the
Concordance settings change it.

**Two screens, the same journal.** With GuiLib installed the journal opens as a workspace (a tab per section, a
scrolling page, the exact-values switch, a refresh button). Without GuiLib, or with **Simple journal** chosen in the
settings, it opens as a plain screen of vanilla buttons.

**Keyboard and accessibility.**

- Left and Right (or the section buttons) change section; Tab moves between buttons.
- Up, Down, Page Up and Page Down (and the mouse wheel) scroll.
- R asks again, X shows or hides exact values, Esc closes.
- Light text on a dark panel; a visible focus ring in the workspace.
- Nothing is said by colour alone: every line says what it means.
- Reduced motion (the Concordance setting) stops the workspace's sliding and its background blur.
- The plain screen's buttons are read by the narrator.
- Both screens follow the game's GUI scale.

The same research report now prints in chat with `/jugcraft concordance status`, in words with its figures after them.

## How it works

**On the server** (`concordance/journal`). `Journal` holds the sections in order, each with when it is shown and the
report that fills it. `Journal.build(player)` reads the server's own records for that player only. `JournalPayload`
carries the result (at most 24 sections of 96 lines: `JournalSection`, `JournalLine` with optional exact figures).
`JournalRequestPayload` carries nothing; the server answers it at most every 10 ticks per player (`RateGate`).

A section whose report fails is replaced by one line saying it could not be read; the server's log says why. The
others still show.

The reports are the features' own, given a sink: `StageProgress.describe`, `Starbound.status` and `commissions`,
`ConcordSpire.describe` and `report`, `Reliquary.report` and `Sympathy.report`, alongside the existing `Vigil.status`,
`Workers.list`, `Couriers.list` and `Sky.forecast`. The commands print through the same methods. `StageProgress.need`
turns the graph's need tokens into words; the pure `Stages.missing` is unchanged.

**On the client.** `JournalClient` registers the key and keeps the last journal received, forgetting it when the
player leaves the world. `JournalScreen` is the plain screen. `JournalWorkspace` (Kotlin, `src/client/kotlin`) is the
GuiLib workspace, with its stylesheet `assets/jugcraft/ui/journal.css`. `JournalClient` opens the workspace only
behind `isModLoaded("guilib")`, so the Kotlin and GuiLib classes are never loaded without GuiLib.

## Connections

- Input producer: every Concordance feature's records (research, stages, Vitae, the sky, workers, the logistics
  ledger, relics, curses and wards, the Conclave, spires, Prima Materia).
- Output consumer: the player; nothing in the game reads the journal.
- Technology connection: deliveries and their stranded cargo; Prima Materia.
- Magic connection: every tradition's state and what it still needs.
- Reachable entry path: the key works from the first moment; a newcomer sees Focus, the stage's first route and how
  much is still unknown.
- Solo, trade and cooperative routes: each player's journal is their own; shared spires and party projects show for
  everyone sharing them.
- Specialty use without other branches: each tradition's section appears only once its research is met.
- Mastery: none (the journal shows progress; it grants nothing).

## Balance

No balance effect: the journal reads; it never records evidence, grants, changes Focus or touches a block.

## Multiplayer and persistence

- Server authority: the client asks; the server decides what that player sees and sends it. The request carries no
  fields to trust.
- Permissions and hidden information: only the asking player's own records; unmet research is counted, not named;
  another player's spire shows only when it is shared with them.
- Rate: one answer per player at most every 10 ticks; at most 24 sections of 96 lines.
- Persistence: nothing new is saved. The client keeps the last journal in memory until the player leaves the world;
  the exact-values and simple-journal choices are this computer's settings (`config/jugcraft-client.properties`).
- Disable behaviour: with `concordance.enabled=false` the journal has one line saying the Concordance is switched off.

## Dependencies and assets

No new dependency. GuiLib (optional, client, pinned 0.12.4) draws the workspace; Kotlin is compiled for that client
screen only, as [FRAMEWORKS.md](../FRAMEWORKS.md) allows, and loads only with GuiLib (which brings Fabric Language
Kotlin). Cloth Config (required) holds the two new settings. No art: the screens are drawn in code and CSS.

## Verification

- `python3 tools/check_mod_data.py`: new step 26 checks (`check_journal`). They confirm:
  - the sections are the generator's, in its order, each titled;
  - every word the journal, the stage report and both screens use has its text (states, rule kinds, milestones);
  - the journal only reads, and the request is rate-limited and carries nothing;
  - the workspace is reached only behind the GuiLib check, and its stylesheet ships.
- Game tests added: `ConcordanceJournalGameTests` (five). They check that a newcomer's research is only counted, that
  met research names its next state with figures, that a tradition's section appears once met, that a stage's needs
  are words, and that the journal survives its encoding to the client.
- Client game test added: `ConcordanceJournalClientGameTests` (the journal crosses from the server and opens, simple
  and GuiLib, with screenshots).
- CI: Build run [37663359511](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37663359511) on `73de706`,
  every job passed:
  - the first compile of the journal's Java and of its Kotlin workspace;
  - `mod` and `optional integrations absent`: "All 1047 required tests passed" (the server game tests, the five new
    ones among them);
  - client shard 2 ran `ConcordanceJournalClientGameTests` and took both screenshots,
    `jugcraft_concordance_journal_simple` and `jugcraft_concordance_journal_workspace`. In the simple screen, "Section 1
    of 4: Overview" shows Focus, stage and research with its three buttons. The workspace shows the same lines under
    its tabs (Overview, Research, Stage, Vitae). In both, the advancement toasts of the research the test grants cover
    the top right corner.

Not yet run: the narrator reading the plain screen, the GuiLib workspace's keyboard navigation by hand, the screens at
other GUI scales, and a two-client server. The screenshots above were looked at as CI's small previews only.

## World and event applicability

Works everywhere; no seasonal content.

## Rollout and open questions

New identifiers: the payloads `jugcraft:concordance_journal` and `jugcraft:concordance_journal_request`, the key
`key.jugcraft.concordance_journal`, the codex entry `foundations/journal`, and the client settings
`concordance.exact_values` and `concordance.simple_journal`. Nothing is saved in worlds.

Open items:

- The GuiLib composer, ritual schematic, crucible and observatory screens are still not built. The `compose` command,
  the circle report, the crucible's own report and the forecast are their interfaces.
- The journal shows the stage's routes but not the Conclave project progress of a party (the `conclave project`
  command does).
