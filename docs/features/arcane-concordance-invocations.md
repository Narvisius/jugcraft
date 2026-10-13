# Arcane Concordance: authored invocations

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 10) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 4, Initiate stage (Jugcraft Discovery). Roadmap step 10.
Primary specialty and supported player role: Lampwrights (Radiance). The six invocations give one tradition a
damage, defense, movement, support, investigation and utility answer each, so a Lampwright can fight, scout, travel
and support a party without composing anything.

Builds on [composed spells and the shared effect system](arcane-concordance-composition.md). Contract and checklist:
[ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md). Their combat numbers are measured in [the
baselines](arcane-concordance-baselines.md) (step 11).

## Player experience

First Light now teaches six invocations. Each is cast from the spell bar while an instrument is in the main hand,
with its own gesture, sound, particles and icon:

| Invocation | Role | What it does | Focus (mastered) | Cast | Cooldown | Learned | Tunings | Counter |
|---|---|---|---|---|---|---|---|---|
| Kindle | utility | light 14 in the open block where you look, 16 blocks, 60 s | 4 (3) | 0.6 s | 1.5 s | understood | Extend | goes out by itself; only open air you may build in; a block replaces it |
| Dawn Aegis | defense | absorbs up to 8 damage for 10 s | 5 (4) | 0.4 s | 12 s | understood | Prolong, Intensify | broken by burst damage; 2 s bare between shells; does not stack |
| Revelation | investigation | up to 6 creatures within 6 blocks glow for 10 s | 3 (2) | 0.8 s | 8 s | understood | Prolong | milk clears it; 6 blocks; never your party or players you may not harm |
| Lance of Dawn | damage | 5 arcane damage to the first creature on a 16-block line, +0.5 per point of arcane Spell Power | 5 (4) | 0.5 s | 2 s | mastered | Extend, Intensify | needs a clear line; one target; a telegraphed cast; magic resistance |
| Flashstep | movement | carries you forward at 1.2 blocks a tick | 4 (3) | instant | 4 s | mastered | Intensify | walls; knockback resistance; 4 s cooldown |
| Lanternward | support | restores 4 health and absorbs up to 4 damage for 10 s, for you and up to 3 party members within 4 blocks | 7 (6) | 1 s | 15 s | mastered | Widen, Prolong | allies must be close; party only; a long, interruptible cast |

**An instrument grants nothing by itself.** The spell bar offers only the invocations your research has taught; a
second or better instrument does not add any. An instrument decides what an invocation may become: each invocation is
compiled for each instrument, and one that does not fit an instrument cannot be cast with it.

**Tuning.** With an instrument in hand, `/jugcraft concordance tune` lists the invocations you know, their Focus and
their tuning. `tune <invocation> <modifier>` joins one modifier from the short list that invocation offers, if you
could use that modifier in a composition of your own (Intensify and Extend need First Light mastered); `tune
<invocation> clear` removes it. Tuning changes numbers only, never what the invocation does: Extend adds 8 blocks of
reach, Intensify half again of its strength, Prolong doubles its time, Widen adds 2 blocks of radius. It costs the
modifier's Focus with every cast. The tunings are kept on the instrument and shown in its tooltip.

**Codex.** A new **Invocations** category: an overview of roles and tuning, and one entry per invocation with its
numbers, tunings and counters, all generated from the same table as the data. First Light's understood and mastered
pages name what each state teaches.

## How it works

**Data.** An invocation file (`data/<ns>/concordance/invocation/`) now names its `role`, its `composition` in the
shared grammar (`"ray struck lance_beam"`), its `tunings` (modifier ids, at most three), and the most `work` one cast
may spend and the most ticks anything it makes may last (`persists`), declared by its author. Its words are
components like any other; the invocation-only ones are marked `"authored": true` (`kindle_light`, `dawn_aegis`,
`survey`, `lance_beam`, `flash`, `mend`). Players cannot compose with an authored word (`problem.authored`), and a
modifier cannot be authored. `allies` is a new selection players may use too: the caster and their party within its
radius, for helpful operations only (`problem.harms_allies`).

**Compiled when the rules load** (`ConcordanceRules.compile`). Every invocation is compiled by the same compiler as a
player's composition (`Compiler.compileAuthored`: the same checks and limits, with authored words allowed and research
checked once, for the invocation itself), for every instrument, untuned and with each tuning. A tuning is the
composition with the modifier joined to the first word it changes (`Compiler.tune`), so it compiles under every limit
a player's spell does. The loader then refuses an invocation that:

- fits no instrument, or does not parse;
- costs less Focus than its composition would (and mastery may take off at most a quarter of that, rounded up):
  an invocation is never a cheaper way to the same effect than the grammar;
- spends more work or lasts longer, in any form, than it declares;
- does what another invocation does (the same delivery, selection and kinds of effect: `Plan.signature`).

A tuning that is not a modifier, changes nothing, or fits no instrument is dropped on its own, with a reason
(`/jugcraft concordance diagnose`).

**Cast** (`Invocations`, the `CUSTOM` impact `jugcraft:invocation`; Spell Engine owns the cast time, gestures,
release sound and particles). On the server the impact re-checks the cast, looks up the compiled plan for the
instrument in hand and the tuning set on it (ignored if the invocation does not offer it, the instrument cannot hold
it or the caster may not use the modifier), scales its damage by Spell Power where the composition says so, and runs
it through the same code as an inscribed spell (`ComposedSpells.perform`): the delivery traced from the caster's own
view, targets chosen on the server, every effect through the shared boundary under one ledger of the plan's limits.
So an invocation has no path of its own around target checks, friendly fire, protection, branch depth or work.

If nothing took effect, the cast fails: no Focus, no cooldown, and the caster is told why (the invocation's own
message, or "You may not change that block"). Otherwise it owes its Focus (its cost for the caster's research, plus
the tuning's) and a cooldown, settled once when Spell Engine consumes the cast's cost (`ConcordanceSpells.consume`).
The cooldown is never shorter than the composition's own (`Plan.cooldown`): Spell Engine's cooldown can be shortened
by Spell Power haste or Spell Engine modifiers, and Jugcraft raises it back to that floor.

**Spell Power, only where it means the same thing.** Spell Power is damage power in a school, so only damage that
names its school may scale with it (the parser refuses `scaling` anywhere else). The Lance's beam adds `floor(0.5 ×
(arcane Spell Power − arcane base))` damage, capped at the strongest damage allowed (`Plan.scaled`,
`Invocations.powerAboveBase`). The value is Spell Power's own (`SpellPower.getSpellPower`): equipment, enchantments,
effects and attribute modifiers from any source count. Nothing else scales: a ward is not stronger for a fire mage.
Damage goes through Spell Power's damage type for the school, so Spell Power's rules apply: it bypasses armour and
shields, and Spell Power's magic resistance reduces it.

**Movement from where the target stands** (`ConcordanceEffects.push`): a push cast on oneself has no "away", so it
goes the way the target faces, level. That is Flashstep.

**Kindle** is now the invocation `ray struck kindle_light` and runs like the other five; `KindleInvocation` is gone.
Its spell id, data, mote and numbers are unchanged.

## Connections

- Input producer: First Light research (understood teaches three, mastered three more and lowers every cost); Focus.
- Output consumer: the world and other players (light, shells, healing, glow, damage, movement).
- Technology connection: none new.
- Magic connection: the same grammar, compiler and effect boundary as composed spells; later traditions add their
  invocations as data in the same form.
- Reachable entry path: unchanged (First Light has three routes to understood).
- Solo, trade and cooperative routes: a solo Lampwright has all six; Lanternward and Revelation are better in a party
  (Revelation never reveals your party; Lanternward reaches only it).
- Specialty use without other branches: needs only First Light.

## Balance

Numbers as compiled for the Initiate's Wand (Focus is 20 at most and returns one point every 2 seconds):

| Invocation | Composition cost | Work | Persists | Composition cooldown | Tuned forms (Focus) |
|---|---|---|---|---|---|
| Kindle | 4 | 3 | 1200 ticks | 30 | Extend: 5 (reach 24) |
| Dawn Aegis | 5 | 1 | 200 | 35 | Prolong: 6 (20 s); Intensify: 7 (12 absorption) |
| Revelation | 3 | 6 | 200 | 25 | Prolong: 4 (20 s) |
| Lance of Dawn | 5 | 2 | 0 | 35 | Extend: 6 (reach 24); Intensify: 7 (7 damage) |
| Flashstep | 4 | 1 | 0 | 30 | Intensify: 6 (1.8 blocks a tick) |
| Lanternward | 7 | 8 | 200 | 45 | Widen: 9 (6 blocks); Prolong: 8 (ward 20 s) |

Every invocation costs at least its composition, and its Spell Engine cooldown is at least the composition's. What
they do against benchmark encounters is recorded in [the baselines](arcane-concordance-baselines.md); no conversion or
resource is produced.

## Multiplayer and persistence

- Server authority: which invocations a player has comes from their server-side research; every cast is re-checked
  and run on the server; tunings are set by command on the server (rate-limited like composing) and re-checked on every
  cast; the Focus recorded on an item is for the client's display only and the server charges its own.
- Persistence: tunings are an item component, `jugcraft:tunings` (per spell id: the modifier id and its Focus; at most
  16 entries). Knowledge, Focus and the mote are unchanged.
- Friendly fire: Revelation's glow counts as harm (it is how players are found), so it follows the same rule as
  damage; Lanternward chooses only its caster's party.
- Disable behaviour: with `concordance.enabled=false`, casts and tuning are refused; everything stays registered.
- Data packs: an invocation that breaks a rule above is left out with its reason; one whose words a data pack removes
  stops loading and is reported, and restoring the words restores it.

## Dependencies and assets

No new dependency. Spell Engine (cast, gestures, sounds, particles, cooldown), Spell Power (damage types, the arcane
attribute), Player Animation Library (nine new original gestures: `lance_cast`, `lance_release`, `aegis_cast`,
`aegis_release`, `reveal_cast`, `reveal_release`, `flash_release`, `ward_cast`, `ward_release`). Five new icons are
drawn by `tools/concordance_art.py` and five new sounds synthesised by `tools/concordance_sounds.py`
(`concordance.aegis`, `.revelation`, `.lance`, `.flash`, `.lanternward`). Release particles are vanilla particle
types (Spell Engine skips an unknown id; `tools/check_mod_data.py` keeps them to a known list). Everything is
original.

## Verification

- `python3 tools/check_mod_data.py` passes (1442 IDs). New step 10 checks: the six roles are covered and match the
  Java enum; every invocation's composition uses known words and fits the wand with every tuning; it costs at least its
  composition (mastery at most a quarter less) and its cooldown is at least the composition's; its declared work and
  persistence cover every form; no two share a signature and no tuning changes one; nothing harms allies; authored
  words are used only by invocations and every one is used; the spell's one impact is `jugcraft:invocation`, which
  `Invocations.java` registers; range, gestures, sounds, particles, icons, codex entries and texts exist; Kindle's
  generator constants match its composition; every message key the invocation code names exists.
- `python3 scripts/check_repository.py` passes.
- The pure core compiles with JDK 21. The step 10 harness passes 92 checks against the generated data: six roles;
  each invocation's Focus, work, persistence and cooldown; distinct signatures; tunings that keep the signature, cost
  more and stay in budget; the tuned numbers; Spell Power scaling (only damage, never below the base, capped);
  authored words refused to players and left out of suggestions; allies never carrying harm; tuning a branched
  composition; and the loader refusing a cheaper invocation, too generous a mastery discount, undeclared work or
  persistence, a duplicate, a tuning that changes nothing, a composition too big for every instrument or that does not
  parse, an unknown role, an authored modifier, and scaling on anything but damage that names its school. The three
  earlier harnesses still pass (they now load components and instruments with the rest, since invocations need them).
- Game tests added: `ConcordanceInvocationGameTests` (ten: six roles load and research teaches three then six at
  falling costs; a wand grants nothing without research; the Lance deals 5, then 7 with 4 arcane Spell Power; Dawn
  Aegis shields and is tuned by command, refusing a modifier it lacks or one not yet learned; a forged tuning is
  charged the server's Focus; Kindle tuned with Extend keeps its composition's 35-tick cooldown; Flashstep pushes
  forward at 1.2; Revelation reveals six of eight under a ledger of six targets and six work; Lanternward mends and
  wards the party only; an authored word is not composable). `kindleCastsThroughSpellEngine` now aims through the
  compiled plan; the component count in `grammarLoadsAndNamesProblems` is 23.
- CI, run 37528166057 (Build workflow, run manually on this branch, commit `5c4d8cf8`, which also carries step 11):
  `mod` passed with **"All 861 required tests passed"** (the ten invocation tests included) and `optional
  integrations absent` passed. The client test shards fail before any test starts, with the same OpenGL startup crash
  as on the framework foundation branch.
- Earlier runs on this branch found and fixed: a compile error (`Entity.invulnerableTime` is private in 26.3; the two
  tests that strike twice now wait out hurt immunity instead), and a missing registration: the invocation test class
  was not a `fabric-gametest` entrypoint, so run 37525962766 built it but ran none of its tests (845 ran, as before).
  `tools/check_mod_data.py` now fails the build for any game test class that is not registered.

Not yet run: any client (gestures, sounds, particles, icons and the codex pages in game), a two-client dedicated
server (Lanternward and Revelation between two real players), a trinket item adding Spell Power.

## World and event applicability

No worldgen, creatures, loot or seasons. Effects obey spawn protection, protected towns and build permission.

## Rollout and open questions

- New stable ids: invocations and spells `jugcraft:aegis`, `jugcraft:revelation`, `jugcraft:lance`,
  `jugcraft:flashstep`, `jugcraft:lanternward`; components `jugcraft:allies`, `kindle_light`, `dawn_aegis`, `survey`,
  `lance_beam`, `flash`, `mend`; item component `jugcraft:tunings`; Spell Engine impact `jugcraft:invocation`;
  sounds `jugcraft:concordance.aegis`, `.revelation`, `.lance`, `.flash`, `.lanternward`.
- Changed: Kindle's spell now uses the impact `jugcraft:invocation` instead of `jugcraft:kindle_light` (an impact id
  is not saved anywhere). The invocation files gained required fields; a data pack's old-format invocation is refused
  with "missing" reasons rather than misread.
- Save compatibility: nothing saved changes form; worlds keep knowledge, Focus and inscriptions.
- Open: a GuiLib tuning screen (the command is the interface every player has); invocations for the other Principles
  arrive with their research.
