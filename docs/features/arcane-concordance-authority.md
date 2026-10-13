# Arcane Concordance: Authority (permissions and hostile use)

Status: implemented on branch `claude/concordance-steps-26-32`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 28) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 20, every stage (Initiate to Architect). Roadmap step 28.
Primary specialty and supported player role: every player of the Concordance, and every server that runs it with
others; no tradition of its own.

The brief's acceptance criterion: an indirect magical action cannot perform a world change that the initiating player
would be forbidden to perform directly, except through an explicit game rule. Builds on the [shared effect
boundary](arcane-concordance-composition.md), [workers](arcane-concordance-workers.md),
[logistics](arcane-concordance-logistics.md), [ecology](arcane-concordance-ecology.md), [the Concord
Spire](arcane-concordance-spire.md), [hexes](arcane-concordance-hexes.md), [rituals](arcane-concordance-rituals.md) and
[alchemy](arcane-concordance-alchemy.md). Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## What already existed

Every Concordance action was decided on the server, and a player acting directly already faced the world's bounds,
spawn protection, adventure mode and Jugcraft's towns (`Illumination.mayChange`). Harm to players already followed the
server's PvP setting and parties (`ConcordanceEffects.mayHarm`), curses were checked again at every pulse, and most
requests were rate-limited per player (`RateGate`). The gaps the audit found are marked **GAP** below.

## Player experience

Magic does nothing on your behalf that you could not do by hand:

- A spell's light, harvest or quench goes only where you could break the block yourself, and a protection mod's claim
  (anything listening to Fabric's block-break question) refuses it as it would refuse you. A spell's harm reaches a
  creature that is not a monster only where you could strike it: a neighbour's claimed animal or a town's townsfolk are
  refused. Monsters may always be fought; other players follow the PvP rules and parties as before.
- Devices and workers act as their owner: the Gleaner harvests, the Verdant and Lantern Spires grow and light, porters
  fetch and couriers collect, and the Gathering Shade picks up, only where their owner could. They ask again on every
  trip and pulse, not only when they were set up.
- **When the owner is away** (offline, or in another dimension), their devices and workers wait and say so ("waiting:
  its person is not here", "waiting for its keeper"). A server may choose otherwise with
  `concordance.absent_owner_authority=true` in `config/jugcraft.properties`: a stand-in then answers for the owner, and
  every protection still judges it as that owner. A player is never harmed by someone who is not there.
- A Porter Key refuses to bind a container you may not use ("You may not use that container yourself..."). A courier
  takes from a container only if both its keeper and the person who asked for the goods could.
- A taglock's link is its maker's: handed on, it curses and scries for nobody else ("only whoever took this link can use
  it"). Scrying reaches only as far as a curse could, by the same rules.
- Breaking someone's Warding Stone during their ritual is your doing: the backlash is yours, so it reaches them only
  where you could harm them, and you are credited.
- A Spire Heart takes upkeep in through any face but gives nothing out through one, so no hopper or pipe drains it.
- Only an alembist may stop a crucible's formula, as only one may set it. A forged brew that calls poison helpful is
  judged by the poison.

## How it works

`concordance/Authority` holds the rules every route asks:

- `mayChange(level, player, pos)`: inside the world and its border, `mayInteract` (spawn protection), `mayUseItemAt`
  (adventure mode), Jugcraft's towns, then Fabric's `PlayerBlockBreakEvents.BEFORE`, asked as a question as the Diesel
  Walker already asks it. Nobody (null) may change nothing.
- `answering(level, owner)`: the owner if they are online and in this level; otherwise, only if the server's option is
  on, Fabric's `FakePlayer` carrying the owner's UUID under the name `[Concordance]` (Fabric's advice for machines, so
  protection mods judge it as the owner); otherwise nobody, and the device waits. The stand-in is only asked questions:
  nothing is given to it, recorded as it or credited to it, and `check_authority` keeps it inside `Authority`.
- `mayStrike(level, actor, behind, target)`: monsters and players pass to the existing rules; any other creature only
  where `TwoHanded.allowed` (Fabric's `AttackEntityCallback`, asked as Jugcraft's two-handed arms ask it) lets the
  person behind the effect strike it.

`ConcordanceEffects` now judges an effect by the person behind it: its actor when that is a player, otherwise the player
its cause names (a familiar's owner, a ritual's leader, a spire's keeper), but never a creature acting for itself. When
that person is away, a block change asks whoever answers for them, harm to a creature that is not a monster is refused
unless someone answers, and harm or a push to a player is refused.

Fabric's `UseBlockCallback` is not asked: Jugcraft's own listeners on it act (they place dust sheets, feed golems, give
treats) rather than answer, so it cannot be put as a question.

### The audit

Every Concordance route that changes the world for someone, with what it asks now (checked at the moment of the change
unless stated):

| Route | Asks | Was |
|---|---|---|
| Spell block effects (light, harvest, quench), triggered effects | `mayChange` as the caster, or whoever answers for them | **GAP**: no protection mod asked; nobody behind it meant the server's rules only |
| Spell, relic, ritual, salve and curse harm to creatures | `mayHarm` (players), `mayStrike` (others) | **GAP**: claims and townsfolk not asked |
| A push (movement) on another player | `mayHarm`, whatever the push's intent | **GAP**: a helpful push moved anyone |
| Harm or a push to a player with nobody here behind it | refused | **GAP**: allowed as sourceless |
| A brew's dose | its status's own category; the item's word can only make it stricter | **GAP**: the item's word was trusted |
| Kindled Lantern trail light; garden blocks used by hand | `mayChange` as the player | **GAP**: no protection mod asked |
| Gleaner harvest | `mayChange` as whoever answers for its keeper; waits otherwise | **GAP**: the server's rules only |
| Verdant Spire growth, Lantern Spire light | the same, for the spire's keeper | **GAP**: the server's rules, or the keeper without protection mods |
| Porter Key binding a container | `mayChange` as the player, when bound | **GAP**: any container |
| Porter fetch and delivery, every trip | `mayChangeFor` its owner; waits while they are away | **GAP**: the server's rules when the owner was offline |
| Courier reserve and pick-up | its keeper's and the requester's `mayChangeFor`; a request waits while its requester is away | **GAP**: the requester was not asked |
| Courier take-back of cancelled cargo | its keeper's `mayChangeFor` | **GAP**: as porter fetch |
| Courier delivery into the requester's post or Spire Heart | the requester's own block, checked when they filed (`post.mayUse`) | unchanged |
| Gathering Shade pick-up | `mayChange` as whoever answers for its holder; waits otherwise | **GAP**: the server's rules when the holder was offline |
| Spire Heart store through a face | in through any face, out through none | **GAP**: a hopper or pipe could drain it |
| Taglock link by touch | `Sympathy.allowed`, now with `mayStrike` | **GAP**: claims not asked |
| Curse cast through a link; scrying a link | only by the link's maker; scrying also within 128 blocks and by the rules | **GAP**: anyone holding the taglock, at any distance |
| Curse pulses | the rules again at every pulse; skipped while the caster is away | unchanged |
| Ritual backlash after a part is broken | as the player who broke it within the step (if here), so their PvP and parties apply | **GAP**: sourceless, whoever broke it |
| Crucible: stopping a formula | an alembist only | **GAP**: anyone |
| Lampwright Bench buttons | one press every 4 ticks a player | **GAP**: unlimited |
| `/jugcraft concordance courier request`, `cancel`, `recover` | one every 10 ticks a player (counts bounded by `Logistics.MAX_WANTED` as before) | **GAP**: unlimited |
| Circle, sconce, garden, sky, relic, spire, vigil, artifice, compose, hex, journal requests | `RateGate`, as before | unchanged |
| Ley Pylon and Lumen Sconce pouring, ritual offerings, Spire founding, Conclave and dream actions | the player directly, on the server | unchanged |

Request sizes stay bounded where they were: the courier's count, the journal's sections and lines, a composition's
length and the payloads' codecs.

## Connections

- Input producer: every Concordance route above.
- Output consumer: protection mods (through Fabric's two events), Jugcraft's towns and spawn protection, which decide.
- Technology and magic connections: porters, couriers and the Spire Heart's store interact with containers and the
  Transfer API exactly as far as their owners may.
- Reachable entry path: nothing to unlock; it applies from the first spell.
- Mastery: none.

## Balance

No numbers change. Devices of an owner who is away now wait by default; on a single-player world or a server where the
owner plays, nothing changes. The server's option restores acting while away, under the owner's protections.

## Multiplayer and persistence

- Server authority: every check is on the server, at the moment of the change; a client's word is never asked.
- Hidden information: scrying tells a link's maker no more than a curse could reach.
- Rate: as the audit table.
- Persistence: nothing new is saved. The option lives in `config/jugcraft.properties` (default `false`). The breaker
  of a circle is remembered for one step only, in memory.
- Disable behaviour: with `concordance.enabled=false` the features refuse as before.

## Dependencies and assets

No new dependency: `FakePlayer` is part of Fabric API (already required, pinned 0.161.0+26.3) and was checked against
that tag's source. No art, sound or animation; the four new messages are text.

## Verification

- Run locally before pushing:
  - `python3 tools/check_mod_data.py` on a copy of the tree, including the new `check_authority`;
  - regenerating the data on a second copy, compared;
  - `python3 scripts/check_repository.py`.
  Their results are in the pull request. None of the Java of this step can compile here (no Minecraft); CI is its
  first compile.
- `check_authority` confirms every audited route still asks `Authority`, nothing asks as nobody, the stand-in is made
  only in `Authority`, Fabric's two events are asked as questions only in `Authority`, `TwoHanded` and `DieselWalker`,
  the option defaults to `false`, the Spire Heart gives nothing out through a face, and the new words exist.
- Server game tests added, with a stand-in protection mod (`TestClaims`) that claims blocks and creatures as a claim
  mod answers Fabric's two events:
  - `ConcordanceAuthorityGameTests` (five): light only where its caster could build (a neighbour's claim refuses it,
    and the stand-in too); harm only to what its caster could strike (a claimed pig, a forged salve, a link by touch,
    a caster away); a porter only from a container its keeper could use, waiting while they are away; a Spire Heart's
    store drained by no face and no hopper; a link only its maker's.
  - `ConcordanceGardenGameTests.theGleanerWaitsForItsKeeper`, `ConcordanceAlchemyGameTests.onlyAnAlembistStopsAFormula`,
    `ConcordanceRitualGameTests.aBreakerAnswersForTheBacklash` (a vandal's backlash, whichever way the test server's PvP
    is set).
- Server game tests changed: the Gleaner's harvest test and the spire's field test now have their keepers present, and
  the field test also shows a spire whose keeper is away changing nothing.
- CI: Build run [37676787998](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37676787998) on 3bbd117:
  `mod` and `optional integrations absent` passed, "All 1064 required tests passed" in each (the eight new tests
  among them). This was the first compile of `Authority` and the new tests.

Not yet run: a real protection mod (only the stand-in listener in the tests); a two-client server with one player
trying the routes against another's claim; a server with the absent-owner option on in play.

## World and event applicability

Works everywhere; no seasonal content.

## Rollout and open questions

New identifiers: the server option `concordance.absent_owner_authority` (default `false`); messages
`message.jugcraft.concordance.workers.key_refused` and `message.jugcraft.concordance.courier.too_fast`; reasons
`compose.jugcraft.ecology.status.keeper_away` and `compose.jugcraft.hex.reason.not_yours`. The Spire Heart is now a
sided container (its store and saved data are unchanged).

Known limits:

- A protection mod is asked only through Fabric's `PlayerBlockBreakEvents.BEFORE` and `AttackEntityCallback`; one that
  protects through other hooks is not asked.
- Jugcraft's vehicles refuse their driver's melee through `AttackEntityCallback` (landship, walker, crewed gun), so a
  driver's spell cannot harm a creature that is not a monster either.
- The stand-in plays in survival: with the option on, an owner in adventure mode is judged as if they could build.
- A player in another dimension counts as away.
- A courier's delivery into the requester's own post or heart is not asked again (it puts their goods into their own
  block).

Open question for the owner: should absent owners' devices act by default (`concordance.absent_owner_authority=true`)?
It is `false` so that nothing acts without a person to answer for it.
