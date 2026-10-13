# Workshops, living systems and pacts

**Status:** new work is PROPOSAL. Preserve the existing systems described in the [audit](01-current-state-and-integration-map.md).

## 1. Make a workshop a readable place

The desired workshop combines physical arrangement, useful instruments and a screen that explains the process. A machine should show its state in the world: the solution changes appearance, a ring fills, a shutter closes, a spirit waits at a marked loading point. The screen supplies exact quantities and explanations. Particles do not hide failures.

Use a shared diagnostic vocabulary across recipes and rituals: missing ingredient; wrong preparation; wrong heat; unavailable power; insufficient destination capacity; missing permission; unloaded target; incomplete pattern; unavailable worker; conflicting agreement; dormant occurrence. A diagnostic is a stable code plus parameters, localized by the client. Never calculate the recipe again inside a Jade renderer.

## 2. Alchemy: discovery first, reproducibility second

### Existing foundation

The current crucible has a six-part liquid volume. A bottle supplies one part and a bucket three. Ingredients contribute Radiance, Verdance, Ember, Rime, Tide and Hollow components plus contamination. Ground inputs retain 90% of their properties, with 60% of that retained amount dissolving immediately; heat bands include cold, warm at 40, hot at 90 and searing at 160. Searing can degrade Radiance/Verdance and add contamination. These are game rules, not real chemical measurements.

Assay spoon and glass already expose composition; learned formula replay reuses exact ingredients and heat bands. Hopper buffer and water piping exist. Expand this machinery rather than implement another cauldron economy.

### Three workshop experiences

**Field preparation:** hand tools, water, garden ingredients and controlled heat. Makes travel supplies and research evidence without electrical infrastructure.

**Reliable apothecary:** a crucible, assay station and stored formula recipes. The player learns why substitutions work, then repeats a known batch with ordinary inputs.

**Industrial apothecary:** existing logistics, water tanks and selected chemical processing provide convenience and throughput. It does not unlock all magic automatically. Exact ingredient identity, research and process conditions still matter.

### Proposed first product family

Values are design targets; match actual component magnitudes before committing recipes.

| Product | Input concept | Process | Useful result | Distinct failure |
|---|---|---|---|---|
| Dewglass Wash | Dewmoss, clean water, a Tide-bearing mineral | Warm extraction then cool rest | Reveals contamination category in a reagent | Overheated batch becomes ordinary wash, not an explosion |
| Hearth Lacquer | Sunpetal preparation, carbon-bearing mundane input | Controlled hot stage | One heat-resistant equipment coating | Searing damages the active component |
| Stillroot Poultice | Mendvetch and a Verdance-compatible binder | Warm, low-contamination | Limited healing preparation | Dirty batch has lower useful yield, clearly predicted |
| Winter Brine | Rime ingredient and existing salt | Cold dissolution | Preservation recipe input and short travel aid | Too much heat consumes the temporary cold property |
| Gloam Ink | Gloamcap and approved Hollow carrier | Covered extraction | Research notes, safe links and ward inscriptions | Excess contamination yields unusable sludge, recoverable through a mundane route |
| Threadwax | Existing wax and a Tether-prepared intermediate | Gentle ritual finishing | Cuts, repairs or prepares declared ritual links | Cannot rewrite an existing pact's owner |
| Assayer's Flux | Existing mineral route plus Tide preparation | Measured reduction | Improves a declared artifice process | Never produces more value than total inputs |

Prefer registered Jugcraft ingredients and existing salt, carbon, wax, glass, Thallite and garden products. “Tide-bearing mineral” is a design role: locate an appropriate registered item before inventing a new ore.

### Formula schema requirements

Extend current formula data only where needed. Record:
- Stable recipe ID and version.
- Exact or explicitly tagged ingredients, preparation and quantities.
- Heat windows, phase durations, liquid requirements and vessel limits.
- Minimum useful composition and maximum contamination.
- Outputs, recoverable byproducts and container returns.
- Knowledge requirement and evidence event.
- Replay policy and preview behavior.

Show the result range before starting if the player has sufficient knowledge. Unknown recipes can conceal the exact result but must not hide destructive risk. A previously learned formula must not silently become invalid after a datapack reload: preserve its ID and report the new incompatibility.

### Atomic replay sequence

1. Resolve the current formula and catalog revision.
2. Check capacity, ownership, heat and all ingredients, including exact component-bearing variants.
3. Simulate input withdrawal and output/container insertion in a Fabric transaction.
4. Begin only if the process contract supports its expected output.
5. Keep progress attached to the machine and recipe revision; do not re-roll results every screen opening.
6. At finish, revalidate the destination and commit inputs/outputs exactly once.
7. If blocked, show output-full and pause according to existing machine semantics. No voiding.
8. If broken, return the correct stored or reserved inputs once; spent heat or power is not duplicated.

Add one tricky regression test per real hazard: container return fills the final slot; two hoppers race; recipe reload during progress; partial fluid availability; restart immediately before output.

## 3. Ecology: make garden arrangement matter

Retain Sunpetal, Dewmoss, Gloamcap and Mendvetch. Distinguish their habitats with actual choices: exposure, moisture, neighboring plants, shade and disturbance. Do not create a global “nature mana” pool.

Proposed garden expansion:
- **Companion planting:** a small, declared neighborhood bonus for mixed eligible beds; count unique families, not identical copies.
- **Season shelter:** a structure that stabilizes one condition in a limited area, paid by ordinary materials and optional Ley.
- **Grafting:** combine two researched plants into a persistent variant with one useful property and one drawback.
- **Recovery:** rest, compost and tending reduce disturbance. No permanent biome corruption.
- **Harvest choice:** gather seed, reagent or mature yield; one action cannot award all three inventories.
- **Visible ecology:** inspection explains “too dry,” “needs open sky,” or “recently harvested,” with the relevant radius.

A proposed graft should contain parent IDs, trait ID, generation and stability in an item component, not an unbounded copy of the whole research catalogue. Cap trait count at one in the pilot. Breeding cannot duplicate rare unique outputs or become a Prima Materia loop.

### Example greenhouse loop

A gardener grows Dewmoss in a damp bed, assays it, produces a preservation wash and treats a Sunpetal sample before transporting it to an observatory. The observatory compares the preserved and ordinary sample during an occurrence. This yields personal research evidence and a crafting ingredient.

A farmer can still grow and sell either plant without participating in astronomy. The scientist can buy the plants but must perform the comparison. This creates trade without making another player mandatory.

### Performance contract

Use bounded neighborhoods and scheduled work. Cache structure/habitat results with invalidation from meaningful changes; do not rescan a whole greenhouse for every plant every tick. Start with a maximum 9×9 horizontal habitat query for a new bed family, then measure before expanding. Iterate only loaded areas. The number is a proposed implementation ceiling, not a claim about current code.

Keep industrial pollution separate from magical habitat disturbance. Owner pollution choices explicitly forbid crop death and biome discoloration. A magical garden is not an excuse to reintroduce those consequences.

## 4. Rituals as physical programs with clear limits

Existing StructurePattern, StructureValidator, RitualMachine and RitualRun are the execution basis. Missing-part, obstruction, ownership, unloaded-part and power diagnostics already exist through interactions, commands and Jade; the proposed planner makes those existing reports visual. A new rite declares physical pattern, ingredients, resource channels, participants, stages, output and controlled failure. It does not run arbitrary commands supplied by a player.

Proposed first new rites:
- **Stillwater Keeping:** preserves one bounded inventory of eligible reagents for a defined duration.
- **Root and Rivet:** attunes a Thallite foundation or construct component; consumes ordinary materials and Ley.
- **Open Hand Compact:** records a consensual pact with explicit service, upkeep and departure rules.
- **Lantern Assembly:** commissions a public light/ward installation under existing town permissions.
- **Memory Mending:** returns or cleans an eligible dream fragment through the existing escrow identity.
- **Glass Accord:** prepares a lens used by astronomy and one optional optical encounter.

For each rite, write a step table: stage, duration, prerequisites, energy drawn, ingredients held, world effect, interruption behavior. Show already-spent and still-reserved resources separately.

### Pattern evolution

Begin with alternatives using the current lesser-circle envelope. A wide new ring is not justified until a rite needs spatial choice. Later pattern variants can trade:
- Fewer pylons and longer duration.
- A compact expensive foundation versus a larger cheap footprint.
- More channel throughput versus smaller storage.
- Solo channeling time versus cooperative shortening.
- Greater output convenience versus stronger preparation requirements.

Do not make pattern size alone a passive universal power multiplier. A structure preview must identify the exact missing or obstructed position and offer an accessible top-down view. Only render ghosts client-side; placement remains ordinary protected block interaction.

### Recoverable failure

Default failures pause or cancel with a clear cost. Backlash can be a temporary visible obstruction, a brief self-contained debuff or loss of spent channel energy. It must not delete nearby builds, create infinite mobs, curse unrelated players or permanently ruin land. A dangerous elective rite needs an explicit in-game description of its actual consequence before start.

## 5. Stolas: a full first pact, not a roster placeholder

**Owner direction:** all 72 spirits, Stolas first, linked to Styx's conservatory, botany, astronomy and minerals. The existing NPC building and observatory room are not the same object as the Concordance observatory machine.

### Proposed pilot quest

1. Read a conservatory note or converse with Styx to learn what kind of evidence matters.
2. Observe a botanical specimen, a mineral sample and a recorded sky occurrence through existing research.
3. Prepare three evidence tokens through normal gameplay. Buying raw samples is allowed; copied personal evidence is not.
4. Arrange an Open Hand Compact with ordinary materials and stored attunement. An unavailable sky event has a documented stored/recall route.
5. Invite Stolas to a prepared perch; display appearance, voice/subtitle and terms.
6. Choose one service: botanical interpretation, mineral comparison or sky guidance.
7. Fulfill a small service obligation through the chosen activity.
8. Receive a bounded insight, recipe hint or diagnostic improvement. Do not receive an unlimited ore locator or free rare resources.
9. Renew, suspend or end the agreement. The spirit leaves cleanly and reserved property returns once.

### Contract fields

Use existing agreement/Bond/BoundWill concepts. Add a data definition or adapter rather than create a second UUID system.

Required: spirit ID, agreement ID, owner, permitted beneficiaries, current service, location boundary, upkeep requirement, fulfillment evidence, due window, state, departure reason and presentation profile.

States: invited, negotiating, active, waiting, suspended, departed. Combat defeat is not the default termination. Missed upkeep suspends the benefit with a visible warning and a grace period. No offline punishment or secretly accumulating debt.

A benefit should be an explicit service function:
- Compare two supplied mineral samples and reveal a known property.
- Suggest one eligible plant habitat correction.
- Forecast the next already-defined occurrence.
- Assist an assay within bounded daily work.
- Provide a cosmetic scholarly companion interaction.

Stolas does not mint Astral Resonance without the calendar ledger, move another player's cargo, or reveal all undiscovered research.

### Assets and animation

Search the authorized library before modeling. Create a clear idle, listen, inspect, assent, refuse and departure vocabulary using GeckoLib. SmartBrainLib may organize perception and movement; contract evaluation remains authoritative Jugcraft code. The same response must be understandable from subtitles and an icon when animation or voice is missed.

## 6. Scaling to 72 meaningful spirits

Build twelve cohorts of six after the pilot. This is a delivery grouping, not a claim that historical source classifications divide that way. The owner can revise artistic interpretations without changing contract mechanics.

Suggested cohorts: gardens; minerals; stars; craft; travel; records; wards; exchange; companionship; dreams; difficult bargains; grand civic services. Assign each actual roster member only after checking the approved character brief and source material. Do not fabricate historical claims or import a modern adaptation's design.

Every spirit dossier needs:
1. Identity, original visual silhouette, personality and three distinctive dialogue beats.
2. A discoverable invitation and ordinary entry route.
3. One signature service that is mechanically distinct from the previous five.
4. Transparent upkeep and suspension.
5. One conflict resolved by a player choice or task.
6. A refusal reason and clean departure.
7. Two small interaction details that make it memorable.
8. Accessibility, ownership, save/reload and no-duplication evidence.

Reject a cohort if its six entries differ only by name, color and yield. Also reject a universal scripting language that delays the first complete spirit indefinitely. Implement Stolas and one deliberately different service before freezing the shared schema.

## 7. Workers, constructs and physical logistics

Reuse the existing worker roster, ownership and courier ledger. Peepo/Jughead already have physical work and care behaviors; do not replace those personalities with generic summoned clones.

Proposed job families:
- Carry exact stacks between declared inventories.
- Fetch ingredients for a ritual reservation.
- Tend an eligible garden bed after checking permission.
- Operate a declared workshop action through MachineCompanionPort.
- Inspect a broken/blocked station and report a reason.
- Maintain an encounter fixture only while its encounter contract permits it.

A job adapter needs canPlan, reserve, begin, progress, complete, cancel and recover semantics, even if the existing API uses other names. The task record should refer to an authoritative claim, not store an extra copy of cargo.

Failure cases are part of the feature: destination fills, player removes access, path fails three times, target chunk unloads, entity dies, owner logs out, source is broken, server restarts. Recover or wait without teleporting items or burning unlimited resources. Preserve current bounded scheduling and backoff.

A construct upgrade can improve payload, range, task variety or durability. Do not improve all simultaneously. Existing integrity costs remain; additional machine wear is outside this plan.

## 8. Curses, dreams and voluntary transformations

### Hexweaving

Use existing links, authority, PvP rules and per-pulse validation. Add readable detection, a cure route and contextual warning. A curse cannot be applied to an unwilling player where the server disallows PvP, nor linger forever because its caster logged out.

Prototype one encounter-only hex interaction before expanding player curses: sever Tatterlace's declared binding or suppress a Lich ritual node. It gives the school a useful role without requiring player griefing.

### Dream adventures

The existing escrow must remain the source of truth for possessions and return. Begin with a short authored memory room, a clear wake exit and one reconstruction puzzle. Do not implement another full overworld as the first dream feature.

A dream completion receipt identifies actor, instance and objective. Reward once, return once, and explain what survives. Test death, disconnect, restart and dimension change at every boundary. Dream-built blocks and temporary copies cannot leak into the normal material economy.

### Vampirism and other forms

Treat transformation as a separate opt-in specialization with a preview, explicit acceptance and a practical reversal route. Costume items do not grant the transformation. Vitae infrastructure does not transform the player accidentally.

First proposed form: senses and short movement utility with a managed appetite tradeoff. No forced feeding from players, real-time offline deterioration, permanent skill loss or compulsory sunlight chores until an owner-approved brief defines them. Feeding and healing must not generate Focus, Vitae and health in a profitable loop.

A werewolf encounter can reveal a transformation story without automatically infecting victorious players. The boss, wearable costume and player specialty are different features.

[Next: existing encounters](04-existing-bosses-and-shared-encounters.md)
