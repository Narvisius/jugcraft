# Arcane Concordance: combat and progression baselines

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 11) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 4, Initiate stage. Roadmap step 11.
Primary specialty and supported player role: every combat-minded player, magical or not. This is the measuring stick
the Initiate stage is held to, and the one later stages will extend.

Builds on [authored invocations](arcane-concordance-invocations.md) and [composed spells and the shared effect
system](arcane-concordance-composition.md). Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## What it is

A deterministic benchmark (`concordance/balance`, pure Java) that runs a roster of characters through five controlled
encounters and measures each one seven ways. It runs over the same compiled plans the server casts and the cast and
cooldown times of the loaded Spell Engine spells. The same code runs in a standalone harness over the generated data
and in a game test over what the server actually loaded (`ConcordanceBaselineGameTests.baselinesHoldOnTheLoadedRules`,
which writes the tables to the server log). Calibration game tests check the rules the model applies against the
server. Nothing here changes gameplay: it records where the Initiate stage stands and fails a build if a later change
breaks the acceptance rules.

**The model.** One line: the character, the foes and an objective on it; time in server ticks. Each tick the
character finishes or starts an action by a fixed priority (shield, support, escape, ranged damage, melee, then close
in; damage spells keep back the Focus a shield or support spell due within 5 seconds will need), foes move and strike,
Focus returns (1 every 2 seconds, at most 20) and lasting effects tick down. The rules it applies, each checked
against the server:

| Rule in the model | Where it comes from | Calibration |
|---|---|---|
| A spell's damage, shield, healing, push, range, radius, targets | the compiled plan (`ConcordanceRules.authored`, Spell Power scaling via `Plan.scaled`) | step 10 tests: Lance 5 and 7, Aegis 8, Lanternward 4 + 4 |
| Cast and cooldown time | the loaded Spell Engine spell, never shorter than the composition's cooldown | `castsCostWhatTheModelSays`: real casts of the Lance and Dawn Aegis |
| Focus cost | the invocation's cost for the research state, plus a tuning's | `castsCostWhatTheModelSays` |
| Armour reduces blows (vanilla's formula), not spells (Spell Power's damage bypasses armour) | Minecraft, Spell Power's damage types | `armourReducesBlowsButNotTheLance`: a husk in an iron chestplate |
| Absorption takes damage first; a hit within 10 ticks of another counts only for what it exceeds it by | Minecraft | `absorptionAndHurtImmunityMatchTheModel`: hits of 6, 4 and 9 on a shielded villager |
| A push of 1 block a tick carries a creature 5.5 blocks on flat ground | Minecraft's movement physics, measured | `aPushCarriesAsFarAsTheModelSays`: Flashstep's push of 1.2 carried a pig 7.05 blocks in CI; the test keeps the model's 6.6 within 30% of what the server does |
| A helpful status lands on the character by its stacking rule; one that changes nothing fails, at no cost (since Ember part 2) | `Stacking`, the rule `ConcordanceEffects` applies; `Invocations` charges nothing when nothing takes effect | `hearthguardBanksTheFire` (Ember part 1): 30 s of Fire Resistance on its caster for 5 Focus |
| A sword strike knocks a foe back 0.8 blocks and sweeps 1 damage to others in reach | Minecraft (approximation) | not calibrated |

**Encounters** (each at most a minute): an **isolated target** (a brute: 20 health, 2 armour, 2.3 blocks a second, 3
damage a second, from 12 blocks); a **clustered group** (three brutes from 10 to 11 blocks); a **mobile opponent**
(a runner: 16 health, 5 blocks a second, strikes for 4 and retreats to 8 blocks, then waits 2 seconds); a
**protected target** (an archer 14 blocks away, 2 damage every 1.5 seconds, behind a guard in iron: 30 health, 6
armour, 4 damage a second once you come within 4 blocks; won when the archer falls); **objective defense** (three
brutes from 12, 16 and 20 blocks going for a 20-health objective where the character stands; won if it still stands
after a minute).

**Characters.** Eight kits, each a different approach with different equipment (vanilla numbers: iron sword 6 every
0.65 s, wooden sword 4, fist 1 every 0.25 s; armour leather 7, chain 12, iron 15): the **Fighter** (iron armour and
sword, no magic); the **Initiate** (First Light understood: Dawn Aegis, Kindle, Revelation; leather, wooden sword);
the **Striker** (mastered: Dawn Aegis, Flashstep, Lance; no armour, wooden sword); the **geared striker** (the same
with iron armour and +4 arcane Spell Power from equipment); the **Warden** (Lanternward and Lance; chain); the
**Skirmisher** (Flashstep and the Lance tuned with Extend; leather, iron sword); the **Composer** (Dawn Aegis and the
inscribed `ray struck sear then here creatures dazzle`; leather); since Ember part 2, the **geared hearthbinder**
(Hearthbinding mastered: Hearthguard, Cinderbolt and Hearthflare, with +6 fire Spell Power, the most the
[regalia](arcane-concordance-ember-regalia.md) gives; the Pyromancer's set, as leather; wooden sword). Then one
bare-handed, unarmoured character per invocation, and the Initiate without Dawn Aegis. Kindle and Revelation have no
combat effect, so the model never casts them; they are there because the Initiate carries them.

**Measures.** Sustained output (damage a second over a minute against a dummy within reach), burst (damage in the
first 5 seconds, Focus full), survivability (seconds lasted, at most 60, beside a brute that cannot be killed),
movement (blocks its abilities carry it in a minute), control (foe-seconds slowed or pushed back in a minute),
resource efficiency (spell damage per Focus) and support (health and absorption given to each ally in a minute under
fire from an archer that cannot be killed; Lanternward gives every party member within 4 blocks the same).

## Results

From the generated data (`Step11Harness`, the standalone harness); the game test computes the same tables from the
server's loaded rules and spells. Since Ember part 1 they include its four invocations; since Ember part 2, the geared
hearthbinder, and helpful statuses land in the model (see Findings).

| Character | Isolated target | Clustered group | Mobile opponent | Protected target | Objective defense |
|---|---|---|---|---|---|
| Fighter (no magic) | won 3.4 s, 19 HP | won 7.0 s, 13 HP | won 9.9 s, 18 HP | won 7.8 s, 13 HP | won 11.2 s, 20 HP |
| Initiate (utility) | won 5.2 s, 20 HP | won 9.4 s, 9 HP | won 11.5 s, 17 HP | won 10.9 s, 8 HP | lost 9.1 s |
| Striker | won 6.7 s, 20 HP | won 14.4 s, 10 HP | won 6.5 s, 16 HP | won 16.0 s, 8 HP | lost 11.1 s |
| Geared striker (+4 arcane, iron) | won 5.6 s, 20 HP | won 13.1 s, 20 HP | won 5.8 s, 18 HP | won 13.6 s, 17 HP | won 11.3 s, 20 HP |
| Warden | won 6.9 s, 20 HP | won 11.4 s, 14 HP | won 7.0 s, 20 HP | won 15.6 s, 13 HP | lost 11.7 s |
| Skirmisher | won 4.7 s, 20 HP | won 12.4 s, 11 HP | won 3.1 s, 17 HP | won 13.5 s, 6 HP | won 10.6 s, 20 HP |
| Composer | won 6.1 s, 20 HP | won 10.9 s, 20 HP | won 4.3 s, 20 HP | won 12.9 s, 8 HP | lost 10.1 s |
| Geared hearthbinder (+6 fire, Pyromancer's) | won 6.7 s, 18 HP | won 8.2 s, 11 HP | won 5.6 s, 17 HP | won 16.5 s, 3 HP | won 12.6 s, 20 HP |
| Only jugcraft:aegis | lost 11.8 s | lost 6.7 s | won 49.8 s, 12 HP | lost 10.0 s | lost 8.8 s |
| Only jugcraft:cinderbolt | won 10.6 s, 8 HP | lost 10.9 s | won 10.6 s, 12 HP | lost 12.1 s | lost 9.1 s |
| Only jugcraft:flashstep | lost 9.3 s | lost 8.4 s | won 40.4 s, 4 HP | lost 6.6 s | lost 8.8 s |
| Only jugcraft:hearthflare | lost 8.7 s | lost 8.4 s | lost 35.6 s | lost 6.4 s | lost 8.5 s |
| Only jugcraft:hearthguard | lost 8.1 s | lost 7.8 s | lost 39.5 s | lost 7.2 s | lost 8.8 s |
| Only jugcraft:hearthspark | lost 9.3 s | lost 8.4 s | won 40.4 s, 4 HP | lost 6.6 s | lost 8.8 s |
| Only jugcraft:kindle | lost 9.3 s | lost 8.4 s | won 40.4 s, 4 HP | lost 6.6 s | lost 8.8 s |
| Only jugcraft:lance | won 8.1 s, 11 HP | lost 7.2 s | won 5.6 s, 16 HP | lost 13.6 s | lost 9.0 s |
| Only jugcraft:lanternward | lost 10.4 s | lost 10.1 s | won 15.7 s, 20 HP | lost 9.6 s | lost 8.8 s |
| Only jugcraft:revelation | lost 9.3 s | lost 8.4 s | won 40.4 s, 4 HP | lost 6.6 s | lost 8.8 s |
| Initiate without Aegis | won 4.7 s, 15 HP | won 9.0 s, 4 HP | won 10.6 s, 17 HP | won 10.4 s, 5 HP | lost 9.1 s |

| Character | Sustained (dmg/s) | Burst (5 s) | Survival (s) | Movement (blocks/min) | Control (foe-s/min) | Efficiency (dmg/Focus) | Support (per ally/min) |
|---|---|---|---|---|---|---|---|
| Fighter (no magic) | 9.15 | 47.2 | 19.3 | 0.0 | 0.0 | 0.00 | 0.0 |
| Initiate (utility) | 6.10 | 31.5 | 20.4 | 0.0 | 0.0 | 0.00 | 0.0 |
| Striker | 5.90 | 25.7 | 19.2 | 105.6 | 0.0 | 1.25 | 0.0 |
| Geared striker (+4 arcane, iron) | 6.27 | 29.7 | 32.5 | 105.6 | 0.0 | 1.75 | 0.0 |
| Warden | 5.95 | 25.7 | 16.4 | 0.0 | 0.0 | 1.25 | 21.1 |
| Skirmisher | 8.57 | 35.5 | 17.4 | 105.6 | 0.0 | 1.00 | 0.0 |
| Composer | 5.91 | 27.7 | 19.1 | 0.0 | 2.3 | 0.50 | 0.0 |
| Geared hearthbinder (+6 fire, Pyromancer's) | 6.13 | 30.8 | 9.2 | 0.0 | 4.5 | 1.45 | 0.0 |
| Only jugcraft:aegis | 1.88 | 9.4 | 9.9 | 0.0 | 0.0 | 0.00 | 0.0 |
| Only jugcraft:cinderbolt | 2.05 | 11.6 | 6.5 | 0.0 | 4.1 | 0.60 | 0.0 |
| Only jugcraft:flashstep | 1.88 | 9.4 | 7.5 | 105.6 | 0.0 | 0.00 | 0.0 |
| Only jugcraft:hearthflare | 2.04 | 11.5 | 6.8 | 0.0 | 2.7 | 0.67 | 0.0 |
| Only jugcraft:hearthguard | 1.88 | 9.4 | 6.6 | 0.0 | 0.0 | 0.00 | 0.0 |
| Only jugcraft:hearthspark | 1.88 | 9.4 | 7.5 | 0.0 | 0.0 | 0.00 | 0.0 |
| Only jugcraft:kindle | 1.88 | 9.4 | 7.5 | 0.0 | 0.0 | 0.00 | 0.0 |
| Only jugcraft:lance | 2.50 | 15.6 | 6.5 | 0.0 | 0.0 | 1.25 | 0.0 |
| Only jugcraft:lanternward | 1.88 | 9.4 | 8.5 | 0.0 | 0.0 | 0.00 | 14.0 |
| Only jugcraft:revelation | 1.88 | 9.4 | 7.5 | 0.0 | 0.0 | 0.00 | 0.0 |
| Initiate without Aegis | 6.10 | 31.5 | 11.5 | 0.0 | 0.0 | 0.00 | 0.0 |

## Acceptance

The game test fails the build unless all of these hold (`Baselines.report`):

1. **No early ability trivializes the encounters.** No character with a single invocation wins every encounter (the
   strongest, the Lance and Cinderbolt, win two of five), and no character wins every encounter all but unharmed
   (losing at most a heart in each).
2. **Several approaches succeed without identical equipment.** Every encounter is won by characters whose armour or
   weapon differ.
3. **The utility character keeps meaningful survival options.** The Initiate wins the isolated encounter, and Dawn
   Aegis lets it last 20.4 seconds beside the unkillable brute against 11.5 without it.
4. **Spell Power changes only what scales with it.** The geared striker's Lance deals 7 to the plain striker's 5; its
   shield is the same. Since Ember part 2 ([regalia](arcane-concordance-ember-regalia.md)) the same holds for fire: the
   geared hearthbinder's Cinderbolt and Hearthflare deal more than the plain ones (6 and 7 against 3 and 4), and its
   Hearthguard does not change.

## Findings

- **The geared hearthbinder (Ember part 2).** With the regalia at its most (a Focus of Fire and four pieces of a fire
  set: +6 fire) the Hearthbinder's mastered kit wins all five encounters, losing health in all but objective defense
  (18, 11, 17, 3 and 20 health left). The gear is what carries it there. Run the same way, the kit with no fire gear
  wins three (it loses the protected target and objective defense), with a Lesser Focus of Fire (+2) four, and with a
  Focus of Fire (+4) all five; the whole regalia then wins the clustered group and the runner sooner (8.2 s against 9.5,
  5.6 against 8.1) and nearly doubles damage per Focus (0.74 at the base, 1.45). Like the geared striker it is a geared
  kit that wins every encounter, though not unharmed in all of them. The model has no on-hit statuses, so the Fire
  Bangle's blow is not in it, and it counts Smoulder as control (a slow: 4.5 foe-seconds a minute), not as burning
  damage.
- **Helpful statuses now land in the model.** Adding the geared hearthbinder showed that the model could not apply
  Hearthguard's Fire Resistance. It took the cast for a refusal (no Focus, no cooldown) and, the shield coming first in
  its priority, began it again every tick while a foe was close, so the character never struck: in CI's first run with
  the character (run 37980438983) it lost three encounters, and Hearthguard alone dealt nothing. On the server the ward
  lands. The model now applies a helpful status by its stacking rule, as `ConcordanceEffects` does (a recast that
  changes nothing fails, at no cost); Fire Resistance wards against nothing these foes deal, so it changes nothing else.
  Only the hearthbinder's row and Hearthguard alone's changed.

- **Initiate magic does not outclass iron.** The Fighter, with no magic at all, wins all five encounters and has the
  highest sustained output (9.15 a second). Casters trade sustained damage for reach, mobility, shields and support.
- **No single invocation carries a fight.** Alone and bare-handed, the Lance and Cinderbolt win the isolated and mobile
  encounters; Hearthflare and Hearthguard win none; every other invocation wins only against the runner, slowly.
- **Objective defense is where Initiate casters fall short.** The Striker, Warden, Composer and Initiate lose it: none
  of them can stop three brutes reaching the objective. The Skirmisher (an iron sword), the geared striker and the
  geared hearthbinder win it. At step 11 no invocation gave control and the composed `dazzle` was the only control (2.3
  foe-seconds a minute); since Ember part 1, Smoulder's slow gives Cinderbolt 4.1 and Hearthflare 2.7.
- **Gear matters as it should.** +4 arcane Spell Power raises the Lance's damage per Focus from 1.25 to 1.75; with
  iron armour the geared striker lasts far longer beside the brute (32.5 s against 19.2). It wins every encounter
  and comes closest to trivializing them (17 health left in the protected encounter, a point above the line): a watch
  item for later equipment.
- **Flashstep is real mobility.** Each dash carries about 6.6 blocks (the server measured 7.05), about 106 blocks a
  minute if cast whenever it is ready; it buys the Striker survival time (19.2 s beside the unkillable brute, against
  16.4 for the Warden, who has no dash).
- **Support is measurable.** The Warden gives each ally 21.1 health and absorption a minute under fire.

## Integration actually exercised

- **Spell Engine**: real casts through `SpellExecution.performSpell` (cast pipeline, cooldown manager); the model's
  cast and cooldown times are read from the loaded spells.
- **Spell Power**: the arcane attribute raising the Lance through `SpellPower.getSpellPower` (step 10 test); its
  damage type bypassing armour (calibration).
- **Jugcraft**: the compiled plans, the cost and cooldown floor, the effect boundary (absorption, healing, a push).
- **Armour**: vanilla armour on a husk, calibrated against the model's formula.
- **Not exercised**: Trinkets (the characters carry their Spell Power as numbers; a worn Resonant Ring's and an Ember
  focus's modifiers are tested by their own game tests); Jugcraft's weapon affixes and Spell Engine's weapon
  skills (Jugcraft's weapons opt out of Spell Engine's automatic skills); triggered effects beyond a composition's
  `then` branch (modelled here; its server behaviour is tested with step 8); offerings (there are none yet).
- **SmartBrainLib**: not used. The encounters are controlled scenarios in the model, where scripted foes are
  repeatable; the calibration tests use vanilla creatures with no AI (and one pig with its own). A brain would add
  nothing these measurements need.

## Presentation options

The client's display settings (`config/jugcraft-client.properties`: the Focus line and reduced motion) never reach
server logic. `presentationOptionsChangeNoOutcome` casts Kindle and Dawn Aegis with reduced motion off and on and gets
the same light, shell and Focus. `tools/check_mod_data.py` fails the build if shared code reads
`ConcordanceClientOptions`, or reads reduced motion outside `animateTick` (which runs only on the client).

## Model limits

One line, not a world: no terrain, line of sight, flanking, strafing, jumping, sprinting, shields or critical hits;
the archer never moves and the guard holds until approached; foes never focus a party; knockback and sweep are
approximations; the character's priority list is fixed, not a player's judgement. The results are baselines to
compare changes against, not predictions of every fight.

## Connections

- Input producer: the compiled invocation and composition plans, the loaded Spell Engine spells.
- Output consumer: maintainers and later roadmap steps (equipment, traditions and creatures are measured against it).
- Reachable entry path, solo/trade/cooperative routes, specialty use: no change to gameplay.

## Verification

- `python3 tools/check_mod_data.py` passes (1442 IDs). New step 11 check: the benchmark and the rules it runs stay
  pure Java; no shared class reads `ConcordanceClientOptions`; reduced motion is read only in `animateTick`.
  Mutation-tested: a server tick reading reduced motion, a Minecraft import in the model and shared code reading the
  client options are each caught.
- The pure core compiles with JDK 21. `Step11Harness` passes 60 checks over the generated data: acceptance holds; the
  benchmark is deterministic (two runs give the same tables); the armour formula and hurt immunity give the vanilla
  numbers; and movement, support, control and efficiency are non-zero exactly for the characters with Flashstep,
  Lanternward, the composed dazzle and damage spells.
- Game tests added: `ConcordanceBaselineGameTests` (six: the benchmark over the loaded rules and spells, and the five
  calibrations above).
- An earlier version of the model assumed a push carried a creature 3 blocks for each block a tick and the test only
  checked the server did at least that; CI measured 7.05 blocks for Flashstep's 1.2 (5.9 for each), so the model now
  uses 5.5 and the test bounds it from both sides.
- CI, run 37528166057 (Build workflow, run manually on this branch, commit `5c4d8cf8`): `mod` passed with **"All 861
  required tests passed"** (the six baseline tests included) and `optional integrations absent` passed. The tables the
  game test wrote to the server log are identical to the ones above, which the harness computed from the generated
  data. The push calibration logged 7.05 blocks against the model's 6.6. The client test shards fail before any test
  starts, with the same OpenGL startup crash as on the framework foundation branch.
- Earlier run 37527119459 passed 860 of 861: the armour calibration's zombie, standing diagonally behind the husk,
  knocked it off the Lance's line (the blow itself matched the model); the zombie now stands straight behind it.
- Ember part 2: CI run 37980438983 (commit `043a810a`) computed the table with the geared hearthbinder, before helpful
  statuses landed in the model, and it was identical row for row to the harness's over the same data. The game test
  failed only on its roster count, which still said seven kits. With the count at eight and helpful statuses in the
  model, `Step11Harness` over the generated data finds acceptance holding and the benchmark deterministic; the variants
  of the fire kit in Findings are the same code with the fire Spell Power changed. CI run 37986172795 (commit
  `2da07f9c`): **"All 1177 required tests passed"**, and the tables the game test logged are identical, row for row, to
  the harness's above.

Not yet run: any client, a two-client dedicated server, a fight in a real world against these encounters, a trinket.

## Rollout and open questions

- No new stable ids, data or items. New code only (`concordance/balance`, tests, a checker rule).
- Open: area control beyond Smoulder's slow (the gap above); arcane equipment with Spell Power (the geared striker's +4
  is an attribute modifier, not an item yet; fire's is the regalia's items since Ember part 2).
