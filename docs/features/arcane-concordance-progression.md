# Arcane Concordance: the progression graph

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 24) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 16, every stage (Initiate to Architect: Jugcraft Discovery to Shared
wonders). Roadmap step 24.
Primary specialty and supported player role: every tradition. The stages are the Concordance's spine; each middle stage
can be reached by a specialist, a generalist or another way of its own.

Builds on every earlier step: research and its evidence, invocations and their gates, rituals, the practices each
tradition records, and the [Starbound Conclave](arcane-concordance-conclave.md)'s ranks and projects. Contract and
checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## What already existed, and what this reuses

Every research entry already named a stage (Initiate for First Light, Practitioner for the rest), docs/DESIGN.md names
Jugcraft's stages (Discovery, Workshops, Specialization, Expeditions, Shared wonders), and the Conclave has ranks. Nothing
named the Adept, Master or Architect stages, said how to reach them, or checked that anything could be reached at all:
each step's checks looked at its own tradition. This step adds the stages as data, a player's stage on the server, and
one progression graph built from the rules themselves, so that it cannot drift from what the game does.

## Player experience

The Concordance has five **stages**, each reached by any one of its **routes** once the stage before it is reached:

| Stage | Jugcraft's | Reached by any one of |
|---|---|---|
| Initiate | Discovery | examining something that holds its own light (First Light encountered) |
| Practitioner | Workshops | understanding First Light |
| Adept | Specialization | mastering two research entries; understanding five entries in five traditions; or understanding Circle Lore and mastering First Light (the Adept's Attunement) |
| Master | Expeditions | mastering five entries in three traditions; understanding nine entries in seven traditions and mastering First Light; or being a Luminary of the Starbound Conclave with two entries mastered |
| Architect | Shared wonders | being Starbound, having helped finish a Conclave project, with six entries mastered; or raising a [Concord Spire](arcane-concordance-spire.md) or helping raise one (roadmap step 25) |

Reaching a stage announces it and awards its advancement (and any before it skipped on the way). A stage once reached is
kept, through death and even if research is later forgotten. `/jugcraft concordance stage` shows a player's stage and,
for the next one, every route with exactly what it still needs ("mastered 2 (0)", "rank starbound"); the codex's
**The Five Stages** entry (Foundations, open from the start) shows every route.

## How it works

**The stages** (`concordance/progression`, pure Java; data `concordance/stage`). A stage has an order and up to eight
routes; a route asks for exact research states, a number of entries mastered or understood across a number of
traditions, a Conclave rank, a number of Conclave projects and named milestones (which features record: a raised Concord
Spire records `jugcraft:spire_raised`, whose practice gate needs Circle Lore understood and the Master stage reached). `Stages` reads a player's situation (research, the traditions it belongs to, rank, finished projects,
milestones) against the routes: the furthest stage reached, and what each route still needs. `ProgressionParser` reads
stages and practice gates strictly.

**On the server** (`concordance/stages/StageProgress`). The stage is a player attachment (`jugcraft:concordance_stage`,
kept through death), refreshed whenever the research engine records evidence that advances something, whenever the
Conclave changes a player's standing, and when a player joins; milestones are a second attachment
(`jugcraft:concordance_milestones`). A stage is never lost: the server only ever moves it forward.

**The canonical progression graph.** `ProgressionGraph` is built from the rules every time they load, not written beside
them. Its nodes are each research entry's states, each invocation and ritual, each practice, the Conclave's ranks and
projects, other players' notes, and the stages:

- a research state needs the state before it (the first state, the entry's prerequisites) and any one of its evidence
  rules: an examination or a study, an invocation, a practice (which needs its **practice gate**, data
  `concordance/practice`: the research the practice needs understood) or another player's notes;
- an invocation or a ritual needs the research state that teaches it; a ritual for more than one participant needs
  another player;
- the ranks need the oath (First Light understood) and projects a Fellow; a stage needs the stage before it and one of
  its routes.

The graph answers what one player alone reaches from a fresh world (a fixed point), which nodes a cycle blocks, which
steps need something only a later stage provides, and whether each middle stage (Adept, Master) has at least two routes
one player can take alone. The server logs what it finds wrong with the rules' other problems, and
`/jugcraft concordance progression` (operators) shows it, or that the graph is whole.

`tools/concordance_progression.py` builds the same graph over the generated data and follows it further, down to things
at hand: each examination's specimens (with as many distinct ones as the rule asks), each study's station, each
invocation's instrument, each ritual's structure (anchor and parts) and offerings, each practice's devices and the
encounters it waits on (night, the open sky, creatures, days), each commission's and project's deliveries, and each
Jugcraft item's recipes (or what makes it otherwise: the Adept's Wand from its ritual, dreamglass from dreams) down to
what the world gives, every vanilla material listed with how it is had. `tools/check_mod_data.py` runs it.

## Mandatory steps

Removing one node at a time and asking whether one player alone still reaches each stage identifies what every way to
a stage needs. This table is the graph's (`tools/check_mod_data.py` fails if it differs):

<!-- mandatory: generated -->
| Stage | Discoveries | Things at hand | Structures | Encounters | Standing and shared work |
|---|---|---|---|---|---|
| Initiate | `jugcraft:first_light` encountered | `#jugcraft:luminous_specimens` | nothing new | nothing new | nothing new |
| Practitioner | `jugcraft:first_light` observed, `jugcraft:first_light` understood | nothing new | nothing new | nothing new | nothing new |
| Adept | nothing new | `jugcraft:lampwright_bench`, `minecraft:copper_ingot`, `minecraft:stick`, `#jugcraft:concordance/luminous_matter`, `#minecraft:planks` | nothing new | nothing new | nothing new |
| Master | nothing new | nothing new | nothing new | nothing new | nothing new |
| Architect | nothing new | nothing new | nothing new | nothing new | nothing new |
<!-- mandatory: end -->

Everything else has an alternative: another specimen, another tradition, another route.

## Recovery routes

- **Finite world materials.** Every material a step can rely on is listed with how it is had. A gathered one names its
  recovery route: copper ingots from drowned, iron from iron golems, gold from zombified piglins; a found one (structure
  loot) is never relied on and names what serves instead (a lead for an echo shard, a white bed for a spore blossom, an
  eye of ender for a Heart of the Sea). The graph is checked without structure loot at all.
- **Amethyst.** The analysis found that every way to the Adept stage needed amethyst (the Lampwright's Bench), and
  amethyst comes only from geodes. The bench, the Initiate's Wand, the Lumen Sconce and the codex now take any
  **luminous matter** (`#jugcraft:concordance/luminous_matter`: an amethyst shard or glowstone dust, which witches drop
  and clerics trade).
- **Dimensions.** One player reaches every stage without anything from the Nether, and without anything from the End.
- **What a player made or learnt.** Research, the stage and Conclave standing are on the player and kept through death;
  Conclave projects are the world's. Every Concordance station drops itself when broken and has a recipe; a lost
  Adept's Wand is made again by the same ritual.

## Navigation, gates and visibility

The graph is what the rest is checked against (`tools/check_mod_data.py`):

- **The codex** (Modonomicon, a required library): every entry's and page's condition is a research state one player can
  reach; each research entry's codex entry opens before the research begins; each invocation's entry opens exactly when
  it is learnt.
- **Spell gates**: every invocation's and grammar component's gate is a research state in the graph.
- **Equipment and practice gates**: each class that records a practice gates it on the research its practice gate names
  (the Alembic Arts for alchemy, Runesmithing for the Artificer's Bench, and so on); every research gate in Java names a
  research entry.
- **Recipe visibility**: every recipe-made thing a step needs has its recipe on a codex page that can be read before it
  is needed, so no step depends on JEI (an optional library); anything made otherwise (by a ritual, a bench working or
  a practice) has a codex entry. The check found the Greenwardens' four crops (needed for the Verdant Heart) shown
  nowhere but JEI: the codex's Organisms entry now shows their recipes.
- **Optional libraries**: nothing in an optional adapter (`compat/`) records research, a milestone or Conclave work, and
  the stage command is a fallback to the codex that needs no client library.

## Connections

- Input producer: every tradition's research, practices and the Conclave's ranks and projects.
- Output consumer: stage advancements; routes for later features (milestones).
- Technology connection: recipes and stations are part of the graph down to what the world gives.
- Magic connection: the stages are the Concordance's; each tradition's mastery counts towards them.
- Reachable entry path: examine a luminous specimen (Initiate) → understand First Light (Practitioner).
- Solo, trade and cooperative routes: every stage is reachable alone (checked); notes and rituals for two are faster
  alternatives, never the only way; the Master stage has a Conclave route.
- Specialty use without other branches: the specialist routes ask for mastery in any traditions (two for Adept, three
  for Master); no single tradition is required.
- Mastery: the Architect stage.

## Balance

Stages reward nothing but recognition (advancements and the announcement); they gate nothing yet. Later features can add
routes (milestones) or ask for a stage.

## Multiplayer and persistence

- Server authority: the stage is the server's, read from what the server recorded (research, standing, milestones);
  nothing a client says moves it.
- Persistence: the stage and milestones are player attachments kept through death; the graph is rebuilt from the rules
  at every load.
- Disable behaviour: with `concordance.enabled=false` stages are not refreshed; what was reached is kept.

## Dependencies and assets

No new dependency. Framework use:

- **Fabric API**: the attachments, the join event and commands.
- **Modonomicon**: a new **The Five Stages** entry; the graph checks the codex's navigation, and the codex is how every
  required step is discovered.
- **JEI, Jade, GuiLib**: not needed by any required step (checked).

No new art: the stage advancements use vanilla icons.

## Verification

- `python3 tools/check_mod_data.py`: new step 24 checks (`check_progression`).
- The step 24 harness passes **36 checks**: the shipped rules load without problems and build a whole graph of 74 nodes (the
  same node set as `tools/concordance_progression.py`'s); one player alone reaches all five stages and all 48 research
  states; notes and the ritual for two need another player; `Stages` reads situations (each stage, each route's needs,
  a rank alone reaching nothing); broken data is refused (an Initiate entry needing a Practitioner one, a state only notes
  can teach, a practice gated on what it teaches, a middle stage with one route, a route naming no research state); and
  the parser's refusals.
- Game tests added: `ConcordanceProgressionGameTests` (five).
- CI: run 37649575481 (commit 55f4f576) builds, passes the data checks and all 1032 required server game tests (the
  five above among them), and client shards 0 and 1 pass. Shard 2 stalled installing Mesa before any test ran; the
  next push's run, 37655111143 (commit ff992934), passes every job, all three client shards among them.

Not yet run: any client (the codex entry, the advancements' toasts), a two-client dedicated server, and a real climb
from a fresh world to Architect by ordinary play (the tests climb the first two stages by ordinary evidence and grant the
rest).

## World and event applicability

Works everywhere; no seasonal content.

## Rollout and open questions

New registrations only: attachments `jugcraft:concordance_stage` and `jugcraft:concordance_milestones`; advancements
`jugcraft:concordance_stage_*`; data folders `concordance/stage` and `concordance/practice`; item tag
`jugcraft:concordance/luminous_matter`. Removing them needs a migration.

Open items:

- The graph on a server sees research, invocations, rituals, practices, ranks and stages; things at hand (recipes and
  world sources) are checked over the shipped data by `tools/check_mod_data.py`, not re-checked at runtime when a data
  pack changes recipes.
- Stages gate nothing yet.
