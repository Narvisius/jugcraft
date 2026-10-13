# Arcane Concordance: factions and the Starbound Conclave

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 23) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 15, Practitioner to Master stages (Jugcraft Workshops to Expeditions).
Roadmap step 23.
Primary specialty and supported player role: every tradition. The Conclave recognises the work players already do in
their own specialties, and gives parties shared projects to do together.

Builds on research and its notes ([shared research](arcane-concordance-sharing.md)), every tradition's practices,
and Jugcraft's own parties (`party/`, the `UseMode` switch). Contract and checklist:
[ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## What already existed, and what this reuses

Nothing in Jugcraft tracked reputation, standing or factions with players before this step: parties are groups of up to
eight with a leader (`JugcraftParties`, saved by the server), the town has no ownership or citizenship, Jugs are the one
coin, and the only "faction" was the hostile raiders. The traditions the Concordance's research already belongs to
(Lampwrights, Greenwardens, Alembists, Starwatchers, Circlewrights, Spiritbinders, Crimson Vigil, Runesmiths,
Balancewrights, Hexweavers, Dreamwalkers) are the Conclave's factions: renown is counted by tradition, and nothing
replaces the parties, the town or Jugs. Renown is standing, never spent and never traded: no new currency.

## Player experience

Once First Light is understood, a player swears the **Starbound Oath** at a **Conclave Lectern** (or with `/jugcraft
concordance conclave join`) and becomes an **Aspirant**. From then on the Conclave recognises their work with
**renown**, from four kinds of contribution, each bounded so that no one repeated deed can carry anyone:

| Kind | What brings renown | Bound |
|---|---|---|
| research | each state a research entry reaches: 1, 2, 5 and 10 | once per entry and state |
| commissions | the Conclave's posted tasks: deliveries, or a practice carried through this week | once a week each; 100%, then 50%, then 25%, then never |
| teaching | another player advanced an entry by reading your notes: 3 | once per entry and learner; four per learner |
| projects | each stage you helped finish (8 to 12), and the whole project (30) | once each |

**Ranks** ask for renown, breadth (traditions with at least 5 renown) and variety (kinds of contribution) together:

| Rank | Renown | Traditions | Kinds | Opens |
|---|---|---|---|---|
| Aspirant | 0 | 0 | 0 | tier I commissions |
| Fellow | 25 | 2 | 2 | tier II commissions; projects |
| Companion | 80 | 4 | 2 | tier III commissions |
| Luminary | 160 | 5 | 3 | |
| Starbound | 260 | 6 | 3 | the Conclave's highest rank |

400 renown from one tradition's research alone reaches no rank at all. A solo player can reach Starbound without
teaching: research, commissions and personal projects can bring up to 504 renown across all eleven traditions.

**Commissions** (twelve, four per tier): tier I asks for glowstone dust, wheat seeds, glass bottles or an observation of
the sky; tier II for a ritual, a bound worker's service, a dissolving at the Assayer's Scale or sweet berries; tier III
for work at the Artificer's Bench, a dream, a curse or a tended living device. Each pays goods (amethyst, emeralds,
experience bottles, ender pearls or a diamond) and never pays in what it asks for. Hold what a delivery asks for and use
a lectern, or use `/jugcraft concordance conclave fulfil <commission>`; `/jugcraft concordance conclave commissions`
lists them and why each is or is not open.

**Obligations.** A member contributes something each week. A **lapsed** member keeps every point of renown but takes no
tier II or III commission and begins no project until they contribute again; any contribution restores them.

**Projects.** From Fellow, a player begins a project for themselves (`/jugcraft concordance conclave project start
<project> personal`) or, as their party's leader, for the party (`... party`). Two projects exist:

- **The Starward Chart** (Starwatchers): lenses (32 glass panes, 8 amethyst shards, someone attuned to the sky), vigils
  (three observations, a ritual), the chart (16 paper, 4 gold ingots, a compass); 2 diamonds each.
- **The Concordance Archive** (Lampwrights): shelves (12 books, 32 oak planks), readings (someone who understands First
  Light, someone who understands the Alembic Arts, two mixtures bottled), the catalogue (16 paper, 8 ink sacs, 4
  feathers); 8 experience bottles each.

A personal project takes only its owner's work; a party's takes any member's. Every stage that asks for several
contributors (two or three) also accepts the **solo way**: contributions on that many different days. Together a party
finishes sooner, and every member who helped shares the renown, the reward and the achievement. Items are given at a
lectern (sneak and use it holding them, or `/jugcraft concordance conclave project contribute personal|party`), research
is presented with an empty hand, and practices count by themselves when they are carried through. `/jugcraft
concordance conclave project` shows each stage, its requirements and the cooperation rule.

**Lecterns.** A lectern serves the project of whoever placed it: their own, or (its owner sneaks and uses it with an
empty hand) their party's, by the same personal-or-party switch Jugcraft's automated systems use. Anyone may swear the
oath, see their standing and fulfil commissions at any lectern. Projects are kept by the world, not the lectern, so
breaking one loses nothing.

## How it works

**The rules** (`concordance/conclave`, pure Java). `Standing` (whether sworn, when, the last contribution, renown in all
and by tradition, kinds, how often each contribution was recognised and when each recurring one was last done);
`Conclave` decides every award and refusal and the rank; `Projects` decides who may begin a project, what a stage takes
(never more than it needs), what it still lacks, and when it advances; `ConclaveParser` reads commissions and projects
strictly and checks them against the research's traditions and entries.

**On the server** (`concordance/starbound`). Standing is a player attachment (`jugcraft:conclave_standing`, kept through
death). Projects, the projects each owner finished and what is owed to players who were offline (a teaching, a stage's
or a project's renown and reward) are one world record (`jugcraft:conclave`), so a project never depends on a block or
on anyone being online; what is owed is given when they next join. Research advanced, practices carried through and
notes that taught are heard from the research engine itself (`ConcordanceProgress.listen`, a new hook every recorded
piece of evidence passes through), so the Conclave recognises exactly what the server recorded and nothing a client
claims. Communal projects are keyed by Jugcraft's own party id; the leader rule is the party's own.

## Connections

- Input producer: every tradition's research and practices; Research Notes; Jugcraft's parties.
- Output consumer: commission rewards (goods); projects' rewards; rank and project advancements.
- Technology connection: deliveries are ordinary crafted goods (panes, paper, compasses, books).
- Magic connection: renown is counted by the Concordance's traditions; practices from rituals, alchemy, the sky, workers,
  artifice, the scale, dreams and curses fulfil commissions and projects.
- Reachable entry path: First Light understood → craft a Conclave Lectern (gold, a book, a dark-oak log and dark-oak
  slabs) → swear the oath → fulfil "Oil for the Lamps" with 16 glowstone dust.
- Solo, trade and cooperative routes: every rank and both projects are reachable alone (the solo way of each stage is
  days); a party finishes projects sooner and shares their reward and achievement; teaching needs another player and is
  never required.
- Specialty use without other branches: research in any eleven traditions counts; ranks need breadth, not every branch.
- Mastery: the ranks themselves (advancements for each), and an advancement per finished project.

## Balance

- Renown is never spent; nothing converts it to goods. Commission rewards are bounded (three times each, ever) and never
  the item asked for; project rewards come once per player and project.
- Every source is bounded: research once per state, commissions three times, teaching four per learner, projects once.
  The ranks need several kinds of work, so no single repeatable deed reaches any rank.
- A lapsed member keeps their renown: obligations pause benefits, they never punish.

## Multiplayer and persistence

- Server authority: renown, ranks, commissions and projects are the server's; contributions are validated (membership,
  access to the project, understood research, the items held) and practices come only from what the research engine
  recorded.
- Permissions: a personal project takes only its owner's work; a party's project takes its members' work and only its
  leader begins it; a lectern's owner alone switches it. Strangers can use any lectern for their own oath and
  commissions.
- Persistence: standing on the player (kept through death); projects, finished projects and what is owed in the world
  record `jugcraft:conclave`; lecterns keep only their owner and mode.
- Disable behaviour: with `concordance.enabled=false` the Conclave recognises nothing and refuses every action; standing
  and projects are kept.

## Dependencies and assets

No new dependency. Framework use:

- **Fabric API**: the attachment, the world's saved data, the join event and commands.
- **Jugcraft parties** (`JugcraftParties`, `UseMode`): communal projects and lectern access. No new team or faction
  system.
- **Modonomicon**: a new **The Starbound Conclave** codex category (the Conclave, renown, ranks, obligations,
  commissions, each project, projects alone or together, lecterns). The book shows; standing lives on the server, and
  nothing depends on the book being visible.
- **GuiLib**: not used. The lectern's messages and the commands are the whole interface, and nothing needs a screen.
- **SmartBrainLib**: not used: the Conclave has no NPCs yet (an open question).

Art: the Conclave Lectern is a dark-oak reading desk leaning back on a square dark-oak post with a brass collar, over a
plinth of dark dressed stone; the Conclave's star chart lies open on the desk (deep blue-violet vellum, five gold-ink
stars in a W joined by faint violet lines) behind a brass lip. Its block model (`tools/concordance_conclave_models.py`)
reads one 16x16 texture of four 8x8 regions (wood, brass, chart, plinth) and its icon is a 16x16 map
(`tools/item_icons/conclave_lectern.txt`), both drawn fresh in `tools/concordance_conclave_art.py`: flat tones lit from
the top left, colours imported from Jugcraft's own ramps (the Lampwright's Bench's dark oak and brass, the Ley Pylon's
dark stone, the Kindled Lantern's smoked glass, the Concordance's violet), none added. The owner's library was searched
(lectern, desk, book stand, star chart, map, astrolabe, sky, brass, dark oak, dark stone) and nothing is used; the
module's docstring gives each candidate and why it did not fit. Nothing is copied from the library or Mojang's files.
The lectern's model and icon have not been seen in the game.

## Verification

- `python3 tools/check_mod_data.py`: new step 23 checks (`check_conclave`).
- The step 23 harness passes **55 checks** against the generated commissions and projects.
- Game tests added: `ConcordanceConclaveGameTests` (ten).
- CI: run 37646213927 (commit 9d618ffc) passes the whole Build workflow on its first run: it builds, passes the data
  checks and all 1027 required server game tests (the ten above among them), and the client test shards pass.

Not yet run: any client (the lectern's model and icon, the codex pages), a two-client dedicated server (a party of two
players working one project with one of them offline at the end), and a week of real play.

## World and event applicability

Works everywhere; no seasonal content.

## Rollout and open questions

New registrations only: block and item `jugcraft:conclave_lectern` and its block entity; attachment
`jugcraft:conclave_standing`; saved data `jugcraft:conclave`; advancements `jugcraft:conclave_oath`,
`jugcraft:conclave_rank_*` and `jugcraft:conclave_project_*`; data folders `concordance/commission` and
`concordance/project`. Removing them needs a migration.

Open items:

- No Conclave NPCs or faction-specific rewards: each tradition is a column of renown, not a separate organisation.
- Commissions are always posted (each once a week); a rotating board is a possible extension.
- A disbanded party's project stays with its old party id and can no longer be worked on.
