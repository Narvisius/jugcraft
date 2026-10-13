# Arcane Concordance: composed spells and the shared effect system

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap steps 8 and 9) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 3, Initiate stage (Jugcraft Discovery). Roadmap steps 8 and 9.
Primary specialty and supported player role: Lampwrights (Radiance) composing their own spells; every later tradition
reuses the grammar and the effect boundary.

Builds on [First Light](arcane-concordance-first-light.md) and [shared records and typed
resources](arcane-concordance-sharing.md). Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

**Composing (step 8).** Once First Light is understood, a player can write a spell of their own and inscribe it on
the Initiate's Wand. A spell is a few words in order:

1. a **delivery**: how it leaves you (`here`, `touch` within 4 blocks, `ray` within 16);
2. a **selection**: what it chooses where it lands (`struck`, the one thing reached; `creatures`, up to 4 within 3
   blocks; `spread`, up to 4 blocks within 3);
3. one to three **operations**: what it does (`light`, `reveal`, `ward`, `sear`, `dazzle`);
4. an optional **ending** (`pulse`: three times, a second apart).

A **modifier** is joined with `+` to the word it changes (`intensify`, `prolong`, `widen`, `extend`), and `then`
starts a branch where the spell landed (`ray struck sear then here creatures dazzle`). The basic words come with First
Light understood; `ray`, `creatures`, `spread`, `sear`, `dazzle`, `pulse` and most modifiers with First Light
mastered.

The composer every player has is the command, used while holding the wand:

- `/jugcraft concordance compose check <spell>` explains what the spell does and costs, line by line, or lists every
  problem by name ("Light acts on blocks, which Creatures does not choose"; "This spell needs 10 capacity; the
  Initiate's Wand holds 8"). Component names are suggested as you type.
- `compose inscribe <spell>` writes a valid spell on the wand. Its spell then appears on the spell bar as the
  *Inscribed Spell*, cast like Kindle; the wand's tooltip shows the spell, its Focus and its cooldown.
- `compose show` reads it back as it would be cast now; `compose clear` wipes it.

The codex has a new **Composition** category: how to compose, worked examples with their cost, the wand's limits,
and one page per component. All of it is generated from the same table as the component data.

**Effects (step 9).** Every Concordance effect now goes through one boundary, whatever delivers it. Players see the
same Slowness, the same damage and the same ward from a spell as they will later from a potion, weapon, creature or
shrine; another player is harmed only where the server's PvP rules allow and never within a party; nothing harms its
own caster; and the caster is credited with the kill.

## The grammar and the compiler (step 8)

`concordance/compose/` is pure Java (no Minecraft types). A spell is a validated structure, never a script: the
parser accepts only component names, `+` and `then`, at most 256 characters and 24 names, and evaluates nothing.

The compiler checks, for the caster and the instrument, and reports **every** problem (not only the first):

| Check | Problem key (`compose.jugcraft.problem.*`) |
|---|---|
| Unknown name, bad character, empty `then`, a modifier on its own, a non-modifier after `+` | `unknown`, `character`, `then_empty`, `loose_modifier`, `not_modifier` |
| Order: delivery, selection, operations, optional ending; nothing after the ending | `expected`, `missing`, `after_termination` |
| A modifier only on a part it changes (strength, time, radius, range) | `modifier` |
| The selection suits each operation (creatures or blocks) | `selection` |
| Nothing harms its own caster (`here struck` with a harmful operation) | `harms_caster` |
| A branch starts with `here`, at most 2 deep | `branch_delivery`, `too_deep` |
| Progression: the caster's research allows every word | `research` |
| Equipment capacity, distinct targets, work, branches, duration (the instrument's limits) | `capacity`, `targets`, `work`, `branches`, `duration` |
| Focus: at most what a player can hold (20) | `focus` |
| Duplicates and counts (an operation or modifier twice, more than 3 operations or 2 modifiers) | `duplicate`, `too_many_operations`, `too_many_modifiers` |

A valid composition compiles to a **plan**: a bounded tree (the spell and its branch) with every number worked out,
its Focus, its cooldown and its limits (distinct targets, work units and branches). Work is the cost of each
application (`EffectKind`: 1 for a creature effect, 2 for light or use, 3 for harvesting or putting out fire), times
the targets the selection may choose, times the pulses, plus one for a traced delivery. The explanation the command
prints is generated from the plan's own numbers.

| Example (codex) | Focus | Capacity | Targets | Work | Cooldown |
|---|---|---|---|---|---|
| `touch struck light` | 2 | 2 of 8 | 1 | 3 | 1 s |
| `here struck ward` | 3 | 3 of 8 | 1 | 1 | 1.25 s |
| `ray struck sear then here creatures dazzle` | 8 | 8 of 8 | 5 | 6 | 2.5 s |

Absolute limits (`compose/Grammar.java`, no data can raise them): range 32, radius 6, 16 targets, 256 work, 2
branches, 2 levels of branching, 3 operations, 2 modifiers a word, 5 pulses at least half a second apart, effects of
at most 40 strength and two minutes. The Initiate's Wand (`data/jugcraft/concordance/instrument/initiate_wand.json`)
allows capacity 8, 6 targets, 48 work, 1 branch and one minute.

**At runtime the plan cannot exceed what it compiled to.** The server compiles the inscription again from its text
every time it is cast, against the current data and the caster's current research (a stale inscription is refused;
a forged Focus cost on the item is ignored). The plan then runs under one `Ledger` built from its limits, shared by
its pulses and its branch: each application is admitted only if it is not a repeat, does not reach a new target past
the limit and does not spend work past the limit. A pulsing spell among ten creatures reaches the four it compiled
for and no more, whatever wanders in later (tested).

**How it uses Spell Engine.** Spell Engine's spells are a synced registry loaded with the data, so a player's own
composition cannot become a Spell Engine spell of its own. Every inscribed spell is cast through one carrier spell,
`jugcraft:composed`: Spell Engine owns its cast time, gestures (Kindle's), HUD and base cooldown; its `CUSTOM` impact
runs the compiled plan; Focus is taken and the plan's own cooldown set (Spell Engine's cooldown manager) once, in
`COST_CONSUME`. No composed segment compiles to a Spell Engine projectile, cloud or area: deliveries are small bounded
Jugcraft operations (a server-side trace from the caster's own eyes, never the client's aim), as the brief allows for
mechanics Spell Engine cannot carry under the ledger. A cast that affects nothing fails: no Focus, no cooldown, nothing
lingers. Once a spell has taken effect, later pulses that find nothing are not refunded.

## The shared effect system (step 9)

`concordance/effect/` (pure) defines the ten operations (`EffectKind`: damage, restoration, movement, illumination,
status, interaction, harvesting, protection, detection and alteration), an effect's numbers (`EffectSpec`), its
`Cause`, the `Stacking` rules, creature `Tolerance` and the per-event `Ledger`. `ConcordanceEffects` is the one
authoritative boundary that applies them on the server. In order, for every application:

1. **Target kind**: creature operations on living creatures, block operations on blocks.
2. **Friendly fire** (harmful effects): never the effect's own actor; another player only where the actor may hurt
   them (`canHarmPlayer`, the server's PvP setting) and they share no party (Jugcraft parties); players in creative or
   spectator mode are immune. A sourceless effect (a shrine with no owner) may harm anyone else.
3. **Tolerance** (harmful pushes and statuses only): `#jugcraft:concordance/immune` (ender dragon, wither, warden,
   elder guardian) ignore them; `#jugcraft:concordance/resistant` (iron golem, ravager, piglin brute) take half the push
   or half the time. Damage is not reduced here: its Spell Power damage type already carries Spell Power's rules (it
   bypasses armour and shields, and Spell Power's magic resistance reduces it), and reducing it again would count
   resistance twice. (An earlier version of this record said armour applied; Spell Power tags its damage types
   `minecraft:bypasses_armor`, so it does not. Corrected with step 10.)
4. **Protection** (block operations): the actor must be allowed to build there (spawn protection, protected towns,
   adventure mode); using a block needs a player actor and follows the town's usable-block rule. Without a player,
   only outside protected towns.
5. **One-time accounting**: the event's ledger admits the application (above).
6. **Application, as the actor**: damage through the Spell Power school's damage type of the operation's Principle
   (Radiance: `spell_power:arcane`; generic magic with no living actor), so the actor gets the kill; statuses through
   vanilla `addEffect` naming the actor as source; protection is Absorption (four points a level); detection is
   Glowing; light is a Kindled mote (`Illumination`); harvesting gathers ripe crops and replants them as the kama does
   (`ArmItem.reap`), or breaks a plant in `#jugcraft:concordance/harvestable`; interaction uses a block in
   `#jugcraft:concordance/interactable` as the player would; alteration only puts out fire.
7. **Presentation**: particles and sound sent to clients. They never apply anything, so a client cannot repeat an
   effect.

**Stacking** (one rule per effect, the same from every source): `strongest` (default) replaces a weaker effect,
extends one as strong but shorter, and otherwise changes nothing (a weaker one never shortens or weakens what is
there); `accumulate` adds a level up to III and keeps the longer time; `exclusive` cannot be renewed while it lasts.
**Expiry** is the effect's own time (vanilla status durations; light motes count down their steps).

**Causes**: every effect carries its `Cause`: the actor, the kind of source (spell, invocation, item, ritual, potion,
weapon, creature, shrine), its id and the event serial. An effect triggered by another keeps the same actor, source
and event one level deeper (at most 3), so a branch's kill is the caster's and its work counts against the same cast.

**Scaling**: the magnitudes the boundary receives are final. Spell Power's spell-power scaling is not applied to
Concordance effects yet (step 11 sets the combat baselines), and Spell Engine's own damage and healing impacts are not
used by any Concordance spell: `check_mod_data.py` requires every spell in `#jugcraft:concordance` to act only
through Jugcraft's registered `CUSTOM` impacts, so no effect is applied or scaled twice.

**First users**: Kindle now lights through the boundary (as an invocation, under a one-target ledger), and inscribed
spells use it for everything. The carried lantern's trail light still calls `Illumination.trail` directly: it is
equipment light that follows its holder, with no target, intent or event. Potions, weapons, creatures, rituals and
shrines arrive with their steps and will call the same boundary; the game tests already deliver the same effects as
each of those sources.

## Connections

- Input producer: First Light research (understood and mastered unlock the words); Focus (the cost).
- Output consumer: the world (light, wards, damage, slowness, glow); later, every tradition's effects.
- Technology connection: none new.
- Magic connection: the grammar is the general form invocations will be checked against (step 10: authored
  invocations must stay within the same budgets); the effect boundary is what rituals, alchemy, shrines and
  creatures will reuse.
- Reachable entry path: unchanged. Composition needs First Light understood, which has three routes.
- Solo, trade and cooperative routes: solo players compose alone; an inscribed wand can be given or traded (the
  receiver casts it only if their own research allows every word).
- Specialty use without other branches: needs only First Light.

## Balance

- Focus: the sum of the words' Focus, an operation's (with its modifiers) for every pulse; at least 1, at most 20.
- Cooldown: half a second plus a quarter second per Focus (at most 10 seconds), and at least as long as the spell
  pulses plus half a second, so a player has at most one pulsing spell at a time.
- Compared with Kindle (4 Focus, light for 60 seconds, 16 blocks), `touch struck light` costs 2 Focus for light lasting
  32 seconds within 4 blocks; `ray struck light` (3 Focus, 16 blocks) needs First Light mastered.
- `sear` adds 3 Focus for 4 damage (two hearts): `touch struck sear` costs 3, `ray struck sear` 4. Composed damage is
  deliberately low until step 11 sets combat baselines.
- No conversion or resource is produced by any component.

## Multiplayer and persistence

- Server authority: composing, inscribing, casting, targeting and every effect are decided on the server. The client
  shows only the inscription's text and cost; the server recompiles it on every cast. Composing and inscribing are
  limited to once a second per player (`RateGate`).
- Persistence: the inscription is an item component, `jugcraft:inscription` (text, Focus and cooldown at inscription
  time; text at most 256 characters). Pulses after the first are kept in memory only: at most 64 on the server and 2
  per player. They end if the caster leaves, dies or changes dimension, or the place is unloaded, and they are not
  saved (a restart ends them). They never load chunks.
- Disable behaviour: with `concordance.enabled=false`, composing is refused, casts are refused by the gate and pulses
  stop; everything stays registered.
- Data packs: components and instruments are data (`data/<ns>/concordance/component`, `.../instrument`); the parser
  refuses any beyond the grammar's limits with a named reason, and a component learnt from research that does not
  exist is dropped (`/jugcraft concordance diagnose` lists both).

## Dependencies and assets

No new dependency. The *Inscribed Spell* icon is drawn by `tools/concordance_art.py`; the cast reuses Kindle's
gestures and sounds. Everything is original.

## Verification

- `python3 tools/check_mod_data.py` passes (1442 IDs). New checks: the grammar and effect limits, effect kinds and
  the compose rate match the Java; every component and instrument is within the grammar and learnt from research that
  exists; the codex examples fit the wand; every compiler text key exists; and every Concordance spell acts only
  through Jugcraft's registered `CUSTOM` impacts and has an icon.
- The pure core (`rules`, `resource`, `effect`, `compose`) compiles with JDK 21 and passes three standalone harnesses
  against the generated data: 49 checks (milestone 1), 51 (steps 6 and 7) and 87 (steps 8 and 9: every problem key
  produced by a matching composition, the codex numbers, canonical text, out-of-range data refused, an adversarial
  ledger run, the explanation order, stacking, tolerance, cause depth, and every compiler text present in the lang
  file).
- Game tests added: `ConcordanceEffectGameTests` (the same damage, status and protection from five kinds of source
  on five pigs, with attribution; friendly fire, creative immunity, a resistant iron golem, a triggered effect credited
  to the caster; stacking, one-time accounting and expiry; light, lever, ripe and green wheat, fire, and an
  adventure-mode refusal) and `ConcordanceComposeGameTests` (the grammar loads and names problems; inscribe by command,
  cast through Spell Engine, Focus taken once, cooldown started, nothing charged for a spell that affects nothing, a
  forged cost ignored, a spell refused when research is taken away; a pulsing spell among ten pigs reaching only its
  four; a branch credited to the caster within its limits).
- CI, run 37517420293 (Build workflow, run manually on this branch, commit `576fae2f`): `mod` passed with **"All 845
  required tests passed"** (the 8 new tests included; the rules load with 16 components and 0 problems) and
  `optional integrations absent` passed. The client test shards fail before any test starts, with the same OpenGL
  startup crash as on the framework foundation branch.
- Earlier runs on this branch found and fixed: two compile errors (Minecraft 26.3 tests entity type tags with
  `Entity.is` and keeps vanilla entity types in `EntityTypes`); the boundary treated vanilla's mock test player, which
  always reports creative mode, as immune (creative immunity is now the invulnerable ability, which game mode changes
  keep); and the branch test, which missed its target in three of four jobs when the target stood 5.7 blocks away (it
  now stands as close as in the inscription test, and the mock player's head faces the way it was placed).

Not yet run: any client (the composer's chat output, the wand tooltip, the spell bar entry and the icon in game), a
two-client dedicated server, a GuiLib composer screen (not built: the command is the composer every player has).

## World and event applicability

No worldgen, creatures, loot or seasons. Effects obey spawn protection, protected towns and build permission.

## Rollout and open questions

- New stable ids: component `jugcraft:inscription`; spell `jugcraft:composed`; components `jugcraft:here`, `touch`,
  `ray`, `struck`, `creatures`, `spread`, `light`, `reveal`, `ward`, `sear`, `dazzle`, `intensify`, `prolong`,
  `widen`, `extend`, `pulse`; instrument `jugcraft:initiate_wand`; tags `#jugcraft:concordance/immune`,
  `/resistant`, `/interactable`, `/harvestable`.
- Removing a component later would make inscriptions that use it fail to compile (the cast is refused with the
  reason); the wand and its inscription text are kept, so restoring the component restores the spell.
- A GuiLib composer, Spell Power scaling, and more operations (restoration, movement, interaction, harvesting and
  alteration have no component yet) are open; they need the research of the Principles they belong to.
