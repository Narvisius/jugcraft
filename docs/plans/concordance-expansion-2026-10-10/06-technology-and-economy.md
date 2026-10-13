# Technology, shared materials and resource economics

**Design purpose:** factories and magic should improve one another without making either compulsory for every player. Preserve the owner's approved industrial plans and the working machine APIs. This chapter proposes interfaces and useful products, not a replacement industrial recipe tree.

## 1. Where industrial development already stands

Jugcraft already has JE storage, kinetic networks, powered machines, multi-output recipes, fluid networks, logistics and companion interfaces. Current baseline examples include a 32 JE/t coal generator, 64 JE/t steam generator, ordinary ore doubling in a crusher, one plate per ingot in a press and three wire per ingot in a wire drawer. New magical recipes must account for those yields.

Approved planning adds a richer manual workshop, mechanical processing, steel and first electricity in parallel, then steel-built electrical chemistry. First electricity around two to four active hours is a design target for a player who knows the recipes, not a measured promise or timer gate.

The owner has already rejected a manual pressure-management ladder and routine industrial lubricant/replacement-part chores. Gas pressure is automatic. Do not reintroduce these as “magical balance.”

The chemistry direction includes gas/acid grades, shared hydrogen/methane generation, separate lower-tier biofuel generation, magnesium before titanium, aluminum recovery loops, polymers and batteries. Another planning thread is expanding those routes. Integrate through named materials and contracts; do not overwrite their exact process recipes from this pack.

## 2. Integration tiers

| Tier | Ordinary industry | Magical benefit | Independence rule |
|---|---|---|---|
| Hand workshop | Existing tools, glass, ceramics, wire and basic metal shaping | Instruments, vessels, rune blanks, assay tools | No electricity required to begin magic |
| Mechanical workshop | Shafts, press, wire drawer, repeatable handling | More convenient components and ritual preparation | Manual alternatives remain for initial tools |
| First electricity | JE generation/storage, cables, basic machines | Existing JE-to-Ley pylon supply and controlled heating | Magic is optional for the first generator |
| Steel chemistry | Fluid recipes, acids, gases, purification, polymers | Consistent reagents, optical glass, coatings, precise substrates | Chemistry does not require a boss or pact |
| Advanced workshop | Electronic-grade materials, alloys, precision production | Specialized instruments and large useful installations | Magical attunement still needs the relevant research |
| Civic projects | Logistics and large approved machine forms | Conclave supply, conservatories and wonders | Ordinary settlements remain useful without wonders |

One-way conveniences are preferable to mutually mandatory gates. A magical preparation can shorten a process or offer a small specialty output. It should not be the only source of steel, copper cable, clean water or ordinary medicine.

## 3. Existing APIs to reuse

### Energy

`EnergyStorage` uses insertion/extraction through Fabric transactions. A machine owns its storage and limits. Never write into another block's fields from a spell to bypass its transfer contract.

Current pylon economics:
- 1,000 JE converts into 1 Ley.
- Pylon buffer is 64 Ley.
- JE intake is capped at 64 JE/t.
- Three Radiance essence can supply two Ley.
- No reverse Ley-to-JE conversion exists.

At maximum intake the pylon receives 1,280 JE per second, or 1.28 Ley per second before scheduling/remainder behavior. Filling from empty requires 64,000 JE and at least 1,000 ticks, or fifty seconds, under uninterrupted ideal supply. This is an arithmetic bound, not a measured machine timing claim.

A future visual conduit transfers existing Ley with explicit ownership and capacity. It does not create a new energy unit. A first prototype could transfer at most four Ley per second between loaded, visible endpoints within twelve blocks; this is a proposed rate requiring comparison with ritual demand.

### Items and fluids

`MachineRecipe` and `MultiMachineRecipe` already handle ordinary processing. `FluidRecipe` has item inputs, multiple fluid inputs/outputs, duration and companion effort. `FluidMachineSpec` supports at most six tanks. `FluidTank` is based on SingleFluidStorage, with external input/output direction rules.

Convert recipe millibuckets using the existing `FluidNetworks.DROPLETS_PER_MB`. Never hard-code a remembered conversion factor or mix amounts from another loader's API. Preserve exact item variants, component data and fluid identities in transactions.

Don't make a single super-machine with fourteen tanks to avoid designing a sensible process. Split a production line into bounded shared machines when it exceeds the existing envelope.

### Companions

Use `MachineCompanionPort`, `MachineCompanionEffort` and the courier ledger. A worker contributes a declared action or effort, not free instant recipes. Keep ordinary hopper, pipe and manual interaction viable.

### Kinetics

Reuse `KineticNetworks` for capacity and load. The proposed mechanical waterwheel/dynamo is distinct from the existing electrical Water Wheel. Any motor-to-dynamo round trip must lose energy. Do not introduce a parallel magical shaft API.

## 4. Four concrete shared production loops

These are functional recipe briefs. Locate registered ingredients before choosing final JSON IDs. Quantities must be tuned against actual existing outputs and measured throughput.

### A. Optical instrument chain

1. Prepare ordinary glass using an existing glass route.
2. Produce a copper/brass-compatible frame using existing metal forms.
3. Polish a lens using an ordinary abrasive and water route.
4. Optionally use a prepared alchemical wash for a specialty clarity property.
5. Attune the lens through a bounded Radiance/Echo ritual.
6. Install it in a survey tool, observatory display or the optional Glass Accord.

The industrial player can sell polished lenses. The magician performs attunement. A hand-polished route supplies the first tool; an advanced line improves volume. A lens does not grant research just because it was traded.

### B. Root-and-rivet construct chain

1. Obtain existing Thallite and copper/iron components.
2. Make a standard body component through current metalworking.
3. Prepare a Verdance-compatible binding from existing garden materials.
4. Attune one component in the current circle system, paying Ley and ingredients once.
5. Assemble a construct with the current roster/ownership mechanism.
6. Assign a job through a shared port and reserve its actual cargo.

This provides a reason for agriculture, metals and rituals to interact. A body upgrade chooses payload or endurance. Keep current construct repair meaningful without extending wear to every factory machine.

### C. Preservation and transport chain

1. Grow and assay a current magical crop.
2. Prepare a cold wash through the alchemy system.
3. Store an eligible reagent with an explicit preservation property.
4. Transport it through ordinary crates, a courier or manual inventory.
5. Consume it in an astronomical comparison or prepared ritual.
6. Return the container according to the declared recipe.

The preservation property is a time/condition modifier on an eligible item, not an infinite durability wrapper for all food or a method to stop every machine process. Transport must preserve it and its expiry exactly.

### D. Ritual textile chain

1. Use current mundane fiber/string and wax routes.
2. Make ordinary thread or an existing textile intermediate.
3. Attune a bounded amount for ritual link preparation.
4. Use it as an optional pattern convenience or a specific agreement reagent.
5. Offer Tatterlace silk as a specialty variant with a different property, not the only route.

The boss variant may make a link easier to inspect or reduce one nonessential preparation step. It should not multiply every output. Decorative lace and fine yarn require explicit new recipes and models; they do not already exist just because silk drops.

## 5. Magical machines worth adding

Add a machine only when it introduces a distinct interaction. Start with an upgrade/module or an existing station if that is sufficient.

| Candidate | Player action | Inputs and authority | Likely implementation |
|---|---|---|---|
| Assay Desk | Compare samples and read a useful diagnosis | Samples retained or explicitly consumed; owner research checked | Existing assay model plus server preview and screen |
| Controlled Crucible Stand | Select a known heat schedule | JE or ordinary fuel, existing crucible; no hidden input creation | Adapter around current heat/process contract |
| Ley Relay | Position a visible, bounded energy route | Existing Ley only, loaded endpoints, transfer ledger | Small block entity with visual transfer events |
| Contract Lectern | Negotiate and review spirits/jobs | Agreement records, permissions, escrow references | UI over current pact/worker state |
| Pattern Projector | Inspect an incomplete circle | No automatic block placement | Client ghosts from bounded server structure report |
| Reagent Cabinet | Preserve a limited eligible inventory | Explicit upkeep and item-state rules | Existing storage patterns with bounded timing |
| Observatory Console | Forecast and review stored attunement | Existing calendar and AstralClaims | Presentation/controller for current machinery |

Do not start with a “universal magical assembler” that duplicates all industrial and ritual systems. The owner should be able to look at a workshop and understand what each installation does.

For new large industrial variants, follow the approved 2–6-block model briefs. Small assay tools and lecterns do not need arbitrary six-block shells.

## 6. One transaction, one result

For each integrated process:
1. Resolve recipe and rule revision.
2. Check knowledge, ownership, structure and all resources.
3. Determine bounded possible operations from every input and output constraint.
4. Simulate item/fluid/power transfer with exact variants.
5. Persist only the state required by the process contract.
6. Commit through the existing transactional boundary.
7. Emit evidence, sound, particles and contribution after success.
8. On failure, report a stable reason and avoid partial unaccounted output.

Rendering callbacks, particle collisions and client screen events never manufacture outputs. A visual Ley beam reports a successful transfer; it is not the transfer's authoritative storage.

For a multi-stage intermediate, store its recipe identity, stage and necessary properties in a bounded component. Do not reset progress by moving it into another machine. If a final result uses randomness, determine and persist the relevant seed/result at the documented point; screen reopening must not reroll.

## 7. Economy audit

Represent resource-changing processes as a directed graph with exact rational values where possible. Include byproducts, catalysts, containers, salvage, discounts, companion effort and all energy conversion directions.

### Must-fail examples

- JE → Ley → a proposed free fuel output → more JE than initially spent.
- Health → Vitae → healing spell → more restored health with no binding exhaustion cost.
- Iron → plate/wire → Prima Materia → more iron after all costs.
- A boss-silk shortcut reducing invitation cost to zero while the boss returns more invitations.
- A spirit contract duplicating its unique agreement token through storage transfer.
- Rerolling an artifice preview by breaking/replacing the station.
- Dream or encounter tokens becoming ordinary transmutable items.
- A catalyst marked “returned” also appearing in a byproduct table.
- Repeated output-full retries charging or awarding the same completion.

Do not solve every loop by adding a random cooldown. Preserve material identity and account for every input. Cooldowns can govern experience but cannot repair duplicated inventories.

### Restricted equivalence policy

Keep the current 45 mundane materials as the initial allowlist. An agent may propose one addition with a complete cycle audit; it must not scan all recipes and automatically declare every resulting item convertible.

Blacklist-by-tag is insufficient for unique or component-bearing items. Check actual components, ownership, charge, enchantments, inventories and exact item state. Boss fragments, pacts, research tokens and advanced magic intermediates remain excluded.

## 8. Pollution, raiders and magic

Pollution is planned as a local emission/recovery system tied to existing raiders. New emissions should occur on actual processing, not idle ticks. Avoid counting a generator's fuel and every powered recipe as the same combustion twice.

Magic can participate in bounded optional recovery:
- A garden installation can process a real waste item into a declared useful output.
- A ritual can assist one recovery process at a real material/Ley cost.
- A ward can improve warning about an approaching raid.
- A civic project can display local conditions through the same pollution map.

Magic must not erase all pollution globally, move it into an unloaded area for free, or introduce smog/crop death/illness against owner direction. Raider strength changes with thresholds; the approved direction is not ever-shorter spawn cooldowns. Keep this interface narrow until the pollution implementation exists.

## 9. Integration acceptance

Demonstrate four independent journeys: manual magician, early industrialist, gardener/trader and mixed workshop owner. No journey should require a boss to start. Then demonstrate a combined workshop where the integration saves work or enables a specialty without multiplying resources.

Use a modest reproducible load: a few stations, one circle, two workers and one active processing chain; then an agreed larger workload. Record tick cost, inventory correctness and network traffic. Numbers in this chapter are specifications or arithmetic, not performance measurements.

[Next: worlds and editor](07-world-designer-and-realms.md)
