# Progression, traditions and spellcraft

**Status:** proposals except the explicitly identified baseline. **Prerequisite:** [current-state contract](01-current-state-and-integration-map.md). All new names, costs and encounters here are working designs.

## 1. The player experience to build

A character should be recognizable by how they solve problems, not only by the color of a projectile. A Hearthbinder controls heat and ignition; a Rimekeeper preserves and creates safe movement; a Circlewright arranges space; a Spiritbinder negotiates labor; a Dreamwalker works with memories and return paths. Every specialty needs an exploration use, a workshop use and an encounter use.

Give the player four interwoven journeys:

1. **Discover:** examine a real phenomenon, receive an intelligible clue, repeat it under a changed condition.
2. **Practice:** make a useful small thing and learn why it worked.
3. **Specialize:** choose a stronger capability with an opportunity cost, rather than collect every passive bonus.
4. **Contribute:** bring a distinctive service to a settlement, expedition or shared wonder.

Existing research states and five stages remain authoritative. No new level bar, universal mana, school XP or mandatory quest currency is needed. The encyclopedia explains routes; it does not invent completion state.

### A complete opening journey

The first new implementation should let a fresh player discover Radiance, learn a simple effect, assemble a legal composition through a screen, inscribe it, use it successfully and see their research advance. Use the current acquisition recipes and rules first; test them before replacing any.

The second journey branches: a garden specialist investigates a habitat, an engineer supplies a pylon with JE, an explorer studies a lair entrance, and a caster practices Ember or Rime. All reach a useful workshop independently. Trade can supply ingredients; it cannot forge another player's practice.

The third journey makes collaboration attractive: the gardener supplies an assay ingredient, the engineer makes a durable tool, the astronomer supplies stored attunement and the caster protects a ritual. The same result must have a slower, clearly listed solo route where it is required for a core stage.

No tutorial should say “defeat all bosses” to unlock normal research. A boss can grant a special method, ornament, recipe variant or elective mastery evidence.

## 2. Sixteen traditions, sixteen distinct promises

These are expansions of existing tradition identities, not sixteen replacement subsystems.

| Tradition | First satisfying action | Adept specialization | Master project | Encounter contribution |
|---|---|---|---|---|
| Lampwrights | Reveal a concealed inscription and place a safe guiding light | Spectral analysis, targeted disclosure, temporary wards | A settlement lantern network with clear ownership | Expose real targets, identify safe routes |
| Hearthbinders | Light a controlled hearth and temper a prepared reagent | Heat shaping and Smoulder management | A precisely controlled arcane kiln | Burst during openings; clear approved combustible hazards |
| Rimekeepers | Preserve a reagent and cross a short gap on a temporary surface | Cold storage, restraint and heat buffering | A conservatory that stabilizes rare plants | Control adds and prepare safe footing |
| Stormcallers | Redirect a bounded pulse between marked conductors | Motion, charge routing and interrupt timing | A storm observatory with safe discharge | Ground charged fixtures and punish recovery |
| Stratawrights | Read stone composition and anchor against a push | Terrain permissions, load bearing and material sensing | A civic foundation or protected construction aid | Brace allies and interact with structural objectives |
| Greenwardens | Restore a stressed bed through correct care | Habitat mosaics, grafting and regrowth | A productive mixed conservatory | Effective healing, living cover and add management |
| Alembists | Assay, heat and extract one reproducible preparation | Contamination separation and ingredient substitution | A formula library with bounded automated replay | Antidotes, coatings and prepared support |
| Circlewrights | Understand why a circle fails and correct one part | Pattern variants and controlled channeling | A multi-station public ritual workshop | Operate wards, contain hazards and maintain windows |
| Spiritbinders | Negotiate a small agreement with a visible term | Distinct contracts and bounded work domains | A pact hall with diverse voluntary services | Spirit-guided objectives; attributed companion help |
| Clockhearts | Assign a porter a useful exact-variant delivery | Sensor-driven jobs with reservations | A staffed magical workshop | Supply fixtures and support devices between attacks |
| Crimson Vigil | Exchange a safe amount of health for a limited ritual reserve | Sustain versus sacrifice choices | A sanctuary for difficult restorative rites | Carefully limited rescue and self-risk support |
| Starwatchers | Forecast an occurrence and store one attunement | Choose alignments with different windows | A long-term constellation project | Read phases and align astronomical fixtures |
| Runesmiths | Put one understandable improvement into an existing tool | Capacity and mutually exclusive rune choices | A signature instrument with a recorded lineage | Specialized equipment; no universal damage stacking |
| Hexweavers | Detect and remove a minor curse | Links, counter-links and ward interaction | A public cleansing house or consensual duel tradition | Suppress marked encounter hazards, cleanse allies |
| Dreamwalkers | Enter a safe bounded memory and return with escrow intact | Memory reconstruction and dream navigation | A shared archive of discovered scenes | Reveal sequences and guide recovery routes |
| Balancewrights | Audit a mundane material conversion | Loss-aware fabrication and salvage | A public material exchange with exact accounting | Repair encounter apparatus from bounded supplied stock |

### Stage gates as capabilities

- **Initiate:** one observed phenomenon and a practiced utility. Explain costs, permission failures and successful use.
- **Practitioner:** one reliable workstation loop. The player can repeat a product without reading source or commands.
- **Adept:** a specialization instrument and a small project. The existing Adept attunement ritual stays relevant.
- **Master:** personal mastery plus a meaningful independent project or expedition. A boss is one eligible route, not the only route.
- **Architect:** maintain an existing shared wonder contract and demonstrate the disciplines it actually uses. Do not add an arbitrary catalogue-completion gate.

A player's stage is not permission to receive all recipes or bypass every tradition's knowledge. Display missing knowledge separately from missing material, unavailable place and optional recommendation.

## 3. Additions to spell composition

### Preserve the working compiler

Current caps: 256 text characters, 24 names, maximum range 32, radius 6, 16 targets, 256 work, two branches at depth two, three operations, two modifiers, five pulses at intervals of at least ten ticks, strength at most 40 and duration at most two minutes. Instruments impose tighter budgets: Initiate capacity 8/6 targets/48 work/one branch; Adept 12/8/72/two branches.

Treat these as baseline limits. A graphical editor does not grant a bigger budget. Raising one requires a measured test and an explicit design change.

The compiler should remain a pure rules operation. Add a normalized intermediate representation if the existing one cannot support the screen, not another parser with different semantics. Text and visual editing must round-trip through the same representation.

### Proposed operation families

Implement small families with typed target and effect contracts. Suggested initial tuning below is a hypothesis, expressed in health points rather than hearts. Map costs to the existing compiler's units after inspecting its cost calculation.

| Operation | Valid target | First behavior | Suggested starting limit | Failure behavior |
|---|---|---|---|---|
| Preserve | Owned workstation/reagent | Extend one decay or freshness window | 60 seconds or one bounded recipe step | Unsupported items unchanged; never freeze machine time globally |
| Rimebind | Hostile ordinary creature | 20% slow for 3 seconds | Same target: 2-second reapplication grace | Bosses use encounter-defined tolerance; no permanent root |
| Brace | Self or consenting ally | Reduce next eligible knockback | 5 seconds, one hit, no stack | Does not cancel scripted return, portal or boundary enforcement |
| Mend | Injured self/ally | Restore 2 health | Suggested 4 Focus, 5-second cooldown | Full health is no application; cannot repay Vitae/exhaustion |
| Ground | Tagged fixture or eligible construct | Discharge one approved charge state | One state transition, 16-block interaction ceiling | Does not drain arbitrary machine batteries |
| Unmask | Concealed encounter/object clue | Make an allowed secret readable | 6 seconds, one clue tier | No container, player inventory or undiscovered map disclosure |
| Sever | Declared temporary magical link | Weaken one link or tether | One operation per valid link | Requires authority; cannot delete player pacts or ownership |
| Recall Mark | Self within permitted scene | Return to an authored safe point | Same scene and dimension, one consumable mark | Refuse if floor/return state is unsafe; no global teleport bypass |

The first additional family should be **Rimebind + Preserve + Brace**, because it connects the planned Yeti, Thallite and workshops without duplicating Ember. Mend needs the encounter contribution and sacrifice anti-loop tests before expansion. Ground should ship with an actual compatible fixture.

### Legal composition examples

These are desired visual outcomes; do not advertise unimplemented parser words as current commands.

- **Lantern Survey:** ray → struck clue → reveal; a working early discovery tool using current concepts.
- **Hearthguard:** self protection followed by a visible expiration cue; use the existing Ember spell where it already provides this.
- **Winter's Courtesy:** touch → consenting ally → Brace, then a short ward. It protects a teammate without moving them.
- **Gardener's Examination:** touch → owned bed → inspect moisture and habitat, then reveal one correctable condition. No automatic growth in the diagnostic operation.
- **Threadcut:** ray → eligible encounter tether → Sever. A successful cut is an objective action, not fake damage.
- **Stillwater Seal:** placed, bounded Circlewright ward → reduce one allowed hazard in its area, spending local Ley. This is a device/ritual effect, not a free permanent personal spell.

### Delivery evolution

1. Finish visual editing of current here/touch/ray semantics.
2. Add one moving projectile only when a specific spell needs travel time, collision and dodge counterplay.
3. Use supported Spell Engine delivery for that capability. Do not assume current composed traces already have projectile lifetime or ownership.
4. Define cast identity, server launch state, first application, cancellation and final settlement.
5. Define continuous casting separately: reserve or bound a pulse budget, revalidate each pulse, stop on empty resources, logout, dimension change or item change.
6. Avoid a single generic “execute arbitrary operation graph” payload. Clients submit IDs and bounded selections, never executable handlers or untrusted commands.

## 4. Transaction contract for complex casts

A proposed `CastReceipt` concept may be needed; inspect existing settlement types before creating it. Required information: unique cast ID, caster, instrument identity, rules revision, compiled expression digest, authorized maximum work, reserved costs if any, applied-effect count, paid state and expiration.

State model:

`REQUESTED → VALIDATED → ACTIVE → SETTLED`, or `REFUSED/CANCELLED`.

For immediate casts, the existing path can pass through these stages within a tick. Do not force a persistence system onto every instant spell. For delayed casts, explicitly choose whether the receipt must survive save/restart; the safest first version cancels outstanding projectiles on restart and never replays unsettled effects.

A receipt must prevent:
- A late projectile charging the player's newer cast.
- A copied impact callback causing a second payment or reward.
- Disconnect and reconnect granting free posthumous effects.
- A catalog reload silently turning an approved cast into a stronger one.
- A weapon or trinket applying Spell Power twice.

Authority is rechecked at impact. A preview is not a permit to hurt a player later. Paid, partially effective multi-pulse spells do not refund completed work; rejected unused reservations must release exactly once.

## 5. Research that rewards understanding

Add typed evidence, not a spam counter. Suggested evidence categories are Observe, Demonstrate, Prepare, Compare, Maintain and Resolve. Reuse current research events and counters; these labels need not become a new storage hierarchy.

For Rime:
1. Observe snow/ice and a warm surface.
2. Preserve an allowed sample.
3. Compare warm and cold preparations.
4. Use restraint on an eligible target without exceeding tolerance.
5. Finish a preservation workshop or study Glacier Hall. Either route advances core understanding.
6. Master through varied work over distinct contexts. A Yeti charge interrupted by a column is elective field evidence.

For Strata:
1. Assay existing Thallite and ordinary stone.
2. Use a brace or earth-bound tool on an allowed substrate.
3. Compare living and earthen ground behavior.
4. Build a stable ritual foundation.
5. Supply an ordinary civic project or study a structural boss opening.

For Tether:
1. Observe an existing agreement.
2. Fulfill one negotiated small service.
3. Show that a worker can refuse an invalid job.
4. Close an agreement and recover cargo safely.
5. Resolve a pact complication through dialogue or work; killing a bound helper must not be the fastest route.

Evidence events need a bounded deduplication key: phenomenon/recipe/encounter occurrence, actor and relevant context. Count demonstrated variety, not infinitely many placements of the same cheap block. Explain the next qualifying action in the UI.

## 6. Equipment, loadouts and power budgets

A build should choose between sustained casting, burst, utility and protection. Do not let every slot grant additive spell power, cooldown reduction, critical damage and free resource generation simultaneously.

Retain seeded artifice previews and current capacity. Add exclusion groups such as:
- One conversion effect per instrument.
- One emergency prevention effect per actor.
- One on-hit resource return source per damage event.
- Mutually exclusive burst and sustained-cost runes.
- One relevant school amplifier in a slot category if current totals already reach the balance target.

Proposed gear packages:
- **Hearth attire:** existing Ember outfits and fire jewelry; measure the current +6 maximum fire contribution before adding more.
- **Rime travel kit:** preservation focus, snow-safe boots, a short Brace accessory. Resist environmental freezing without permanent boss immunity.
- **Garden scholar kit:** assay convenience, habitat visibility and modest effective healing. No passive growth tick for every equipped player.
- **Pact steward kit:** one extra visible job queue or longer assignment radius within loaded bounds. No ownership bypass.
- **Surveyor kit:** instruments and discovery assistance; no ore scanner revealing protected inventories or all hidden structures.

Boss versions should change behavior: a reflected ray, a brief persistent snow footing, a different rune slot tradeoff. They should not be mandatory raw-stat upgrades over every crafted alternative.

## 7. Balancing procedure

Use the current Ember baseline and existing boss health as comparison points. Do not “balance” by multiplying every school's damage until combat is trivial.

Measure:
- Damage over one opening and over a 60-second fight, including misses and downtime.
- Effective healing and prevented damage, not just nominal tooltip values.
- Focus spent and regenerated, item/ritual preparation and ammunition cost.
- Time outside the boss's intended risk envelope.
- Utility benefit in an ordinary workshop or journey.
- Effects at solo, two-player and four-player scale; separately test configured higher party caps.

Initial acceptance: each tradition demonstrates a distinct useful action; a crafted non-boss loadout can complete the relevant normal encounter; no two effects generate net resources by feeding one another; no support loadout becomes unable to qualify for shared rewards.

[Next: workshops, ecology and pacts](03-workshops-ecology-and-pacts.md)
