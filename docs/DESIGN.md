# Connected tech and magic design

Status: initial design direction; names and numbers are proposals, not implemented content.

## Core promise

Technology gives repeatability, scale, transport, and automation. Magic gives transformation, discovery, attunement, and exceptional capabilities. Neither is a complete replacement for the other. Exploration supplies discoveries; cooperation enables specialization without permanently locking solo players out.

## Proposed progression

| Tier | Technology | Magic | Connection |
| --- | --- | --- | --- |
| 0 — Discovery | Hand tools and simple processing | Find traces and learn basic attunement | Naturally obtainable materials start both branches |
| 1 — Foundation | Basic powered processing | Starter rituals | Refined metals make ritual components; rituals create machine catalysts |
| 2 — Resonance | Resource routing and repeatable production | Stabilized essences and infusions | Machines prepare substrates; infusion improves machine capabilities |
| 3 — Synthesis | Automated multi-stage production | Controlled ritual automation | Both branches produce a shared advanced component |
| 4 — Community projects | Large infrastructure | Major coordinated workings | Shared builds consume sustained outputs from multiple specialties |

Each tier must have an entry recipe that uses only already reachable resources. Draw the unlock dependencies and prove the first machine/ritual is obtainable without already having its own output.

## Concrete first feature loop

Exploration yields raw resonant material → mechanical refinement produces substrate → a starter ritual binds essence into a catalyst → a machine uses that catalyst to process materials more efficiently → those materials unlock the next ritual.

The starter ritual must be hand-accessible before the upgraded machine. The first machine uses ordinary obtainable parts. Catalysts have explicit durability, consumption, or upkeep; their value cannot be assumed to justify unlimited free production.

## Integration contract

Every gameplay addition names: tier and entry path; existing input producer; existing output consumer; how it improves a player choice; recipe/energy/time costs; multiplayer ownership; automation behavior; environmental and performance costs; and upgrade/disable behavior. New standalone resource systems need a compelling reason and a maintainer decision.

Meaningful connections change use or progression. A token recipe using one existing ingot does not integrate an otherwise isolated mod. Infrastructure, accessibility, and cosmetic additions can instead name the systems they support without inventing artificial resource dependencies.

## Economy and multiplayer

- Prefer shared material tags and one canonical form per material, with explicit conversion recipes where needed.
- Treat power and magical essence as distinct resources. Any bridge defines direction, units, throughput, loss, and limits. Audit entire conversion cycles for duplication and positive gain.
- Offer trade and specialization; avoid requiring every player to repeat every research step. Team progression and permissions require an explicit design before implementation.
- PvP, claims, machine ownership, automation access, and offline behavior must be specified before release.
- Reuse earlier outputs at later tiers through useful demand, not excessive repetitive grinding.
- Avoid world generation additions until their effect on old chunks and exploration has been reviewed.

## Acceptance question

If this feature disappeared, which existing systems and player decisions would lose something useful? If the answer is none, strengthen its integration before implementation.
