# Research notes, comparison and primary sources

**Researched:** 10 October 2026. This appendix describes mechanisms inspected in primary sources and how they informed the proposed Jugcraft design. It is not a compatibility list or an instruction to install the reference projects.

Player-facing concepts in this pack use Jugcraft's magical vocabulary. Source links retain the real repository/file identity so an implementing agent can verify the evidence. The technical dependency names are retained where necessary to use the approved libraries correctly.

## 1. How the design families fit together

| Family | What its strongest examples do | Similarity to other families | Jugcraft synthesis |
|---|---|---|---|
| Investigative scholarship | Gate understanding through observations, crafted evidence and practice | Connects strongly to ritual preparation and artifact study | Existing research states plus typed evidence and a clear journal |
| Compositional casting | Combine delivery, target/effect and modifiers | Shares effect vocabulary with fixed combat spells | Existing compiler, bounded work and a visual composer |
| Prepared combat | Distinguish instant, charged and sustained actions | Shares timing and targeting with composed spells | Spell Engine with explicit payment/impact contracts |
| Spatial ritual craft | Require a physical arrangement, reagents and phased execution | Resembles a machine recipe expressed in world geometry | Current circle runtime plus diagnostics and specialized rites |
| Living workshops | Turn habitat, tending and arrangement into useful production | Connects farming, alchemy and automation | Garden conditions, repeatable formulas and bounded worker jobs |
| Negotiated services | Summon a helper with a defined task, cost and scope | Overlaps constructs and social progression | Existing agreements/rosters with Stolas and varied services |
| Astronomical practice | Discover patterns and plan around occurrences | Overlaps research and long projects | Existing calendar/claim ledger plus forecasts and stored alternatives |
| Risk and identity | Power has a visible cost or voluntary character tradeoff | Connects curses, restoration, dreams and transformation | Distinct Vitae/exhaustion, consent and recoverable failure |
| Artifact specialization | Curated affixes, sockets, rarity and reusable relics | Connects crafting, exploration and boss rewards | Capacity, exclusions, seeded previews and elective sidegrades |
| Material accounting | Map conversions and make exchange convenient | Resembles recipe graphs and industrial conservation | Restricted exact-value catalog and full cycle audits |
| Visible industry | Convey motion, throughput and intermediate stages | Similar to spatial rituals and magical logistics | Existing JE/kinetics/transactions with readable installations |

The most useful combinations share a contract, not every feature. Research explains a ritual; a ritual attunes an industrially made lens; the lens improves a forecast; the forecast guides an expedition. That chain should retain alternate routes at its essential gates.

The most dangerous combinations share an output without sharing accounting: healing feeding sacrifice, salvage feeding equivalence, or helper cargo existing in both entity and ledger. The economy and lifecycle chapters target those boundaries.

## 2. R01 — Compositional grammar

**Observed:** a spell resolver validates its ordered spell parts before checking casting resources; parts provide shared interfaces for behavior and adjustments.

**Adopt:** keep one pure validation/compilation path for text and graphical composition. Validate grammar, target compatibility and work limits before executing effects.

**Adapt:** Jugcraft already has a bounded compiler. Add operations through its existing contracts instead of importing a second part registry or mana system.

**Verify:** invalid graphs are rejected before impact; all branches share one budget; UI and server interpret the same expression.

**Primary source:** [SpellResolver.java](https://github.com/baileyholl/Ars-Nouveau/blob/fd8c9520722bb96a3e75febaf770d7078db5ac43/src/main/java/com/hollingsworth/arsnouveau/api/spell/SpellResolver.java) · [AbstractSpellPart.java](https://github.com/baileyholl/Ars-Nouveau/blob/fd8c9520722bb96a3e75febaf770d7078db5ac43/src/main/java/com/hollingsworth/arsnouveau/api/spell/AbstractSpellPart.java)

## 3. R02 — Visible energy and spatial production

**Observed:** an energy-spreading block tracks traveling bursts and feedback; a specialized generating plant uses a bounded cellular process rather than a generic fuel inventory.

**Adopt:** visible transfer and spatial setup can make magical automation understandable. A greenhouse can reward arrangement instead of merely holding another “power-producing flower.”

**Adapt:** use Ley Charge and current garden rules. Transfer visuals cannot own a second copy of energy. Keep finite queries and declared throughput.

**Verify:** interrupted/blocked transfer cannot duplicate power; repeated cell/plant updates stay within workload bounds.

**Primary source:** [ManaSpreaderBlockEntity.java](https://github.com/VazkiiMods/Botania/blob/310d546ff817376dcaa5281cd308365e61e76031/Xplat/src/main/java/vazkii/botania/common/block/block_entity/mana/ManaSpreaderBlockEntity.java) · [DandelifeonBlockEntity.java](https://github.com/VazkiiMods/Botania/blob/310d546ff817376dcaa5281cd308365e61e76031/Xplat/src/main/java/vazkii/botania/common/block/flower/generating/DandelifeonBlockEntity.java)

## 4. R03 — Declarative ritual and summoned-service settings

**Observed:** ritual recipe data separates the ritual type, requirements, start settings, summoned entity settings and result.

**Adopt:** separate conditions from outcomes and presentation. A pact invitation should describe the service it creates without embedding all behavior in the recipe.

**Adapt:** extend current RitualMachine and agreement state. Do not expose arbitrary command execution through user-authored formulas even where an external format permits it.

**Verify:** failed prerequisites spend nothing; successful invocation creates one agreement; reload reports invalid settings cleanly.

**Primary source:** [RitualRecipe.java](https://github.com/klikli-dev/occultism/blob/bad12d373049aaeba8a4054447e29c75553de36c/src/main/java/com/klikli_dev/occultism/crafting/recipe/RitualRecipe.java)

## 5. R04 — Distinct casting lifecycles

**Observed:** the combat spell API distinguishes instant, long and continuous cast types and associates spell properties with timing/cost behavior.

**Adopt:** sustained and delayed spells need different cancellation and settlement contracts from immediate impacts.

**Adapt:** retain the approved Spell Engine integration. Audit the existing pending settlement before adding overlapping projectiles; do not add a second generic combat casting engine.

**Verify:** cancellation before impact, interruption after one pulse, item swap, logout and duplicated callbacks all have explicit resource outcomes.

**Primary source:** [CastType.java](https://github.com/iron431/irons-spells-n-spellbooks/blob/e4056af90302d37eb1739f5ff05020b020e6e252/src/main/java/io/redspace/ironsspellbooks/api/spells/CastType.java) · [AbstractSpell.java](https://github.com/iron431/irons-spells-n-spellbooks/blob/e4056af90302d37eb1739f5ff05020b020e6e252/src/main/java/io/redspace/ironsspellbooks/api/spells/AbstractSpell.java)

## 6. R05 — Altar throughput and bounded personal networks

**Observed:** altar processing includes tier, rune/capacity and progress concepts; the network implements bounded additions and withdrawals.

**Adopt:** distinguish storage capacity, transfer rate and ritual capability. A larger buffer is not automatically a higher progression tier.

**Adapt:** Jugcraft Vitae has its own health and exhaustion contract. Do not collapse health, Focus and ritual fuel into a profitable cycle or copy a second personal network.

**Verify:** upgrades change only their declared axis; sacrifice cannot refund its limiting cost through healing.

**Primary source:** [BloodAltar.java](https://github.com/WayofTime/BloodMagic/blob/35f0188665e648d981095b6933cc75b0ca96eca5/src/main/java/wayoftime/bloodmagic/altar/BloodAltar.java) · [SoulNetwork.java](https://github.com/WayofTime/BloodMagic/blob/35f0188665e648d981095b6933cc75b0ca96eca5/src/main/java/wayoftime/bloodmagic/core/data/SoulNetwork.java)

## 7. R06 — Discovery, constellations and connected progression

**Observed:** constellation definitions carry discovery properties; a perk tree represents connected choices and prepared data.

**Adopt:** the sky can teach a sequence of observations and offer branching specializations.

**Adapt:** the working Jugcraft calendar and occurrence ledger remain authoritative. Prefer forecasts and stored attunement over arbitrary real-date waiting, and bounded choices over unlimited permanent attribute stacking.

**Verify:** duplicate observatories and time changes do not multiply claims; inaccessible timing does not hard-lock core progression.

**Primary source:** [PerkTree.java](https://github.com/HellFirePvP/AstralSorcery/blob/c39557a0aa686d61511fb2723eb04bd22eea803e/src/main/java/hellfirepvp/astralsorcery/common/perk/data/PerkTree.java) · [BaseConstellation.java](https://github.com/HellFirePvP/AstralSorcery/blob/c39557a0aa686d61511fb2723eb04bd22eea803e/src/main/java/hellfirepvp/astralsorcery/common/constellation/BaseConstellation.java)

## 8. R07 — Affix exclusions and weighted rarity

**Observed:** affix definitions include tier-aware weights and exclusivity information; rarity is an explicit part of loot construction.

**Adopt:** encode mutually exclusive properties rather than rely on informal balance notes.

**Adapt:** use Jugcraft's current substrate/capacity and seeded preview. Boss gear should offer a distinct build choice rather than every strongest affix simultaneously.

**Verify:** preview reopening cannot reroll; incompatible affixes cannot coexist; equipment cannot apply school scaling twice.

**Primary source:** [AffixDefinition.java](https://github.com/Shadows-of-Fire/Apotheosis/blob/f2e76862ed9f4dcdf19c25400dc3d5478a46dd74/src/main/java/dev/shadowsoffire/apotheosis/affix/AffixDefinition.java) · [LootRarity.java](https://github.com/Shadows-of-Fire/Apotheosis/blob/f2e76862ed9f4dcdf19c25400dc3d5478a46dd74/src/main/java/dev/shadowsoffire/apotheosis/loot/LootRarity.java)

## 9. R08 — Recipe-to-value mappings

**Observed:** a mapping handler and mapper interface accept conversion information from recipes and related sources.

**Adopt:** inspect all conversion edges when auditing material economics.

**Adapt:** use mapping as an audit technique, not permission to assign a value to every Jugcraft item. Preserve the current small allowlist, component exclusions and lossy formation.

**Verify:** returned containers, byproducts, salvage, preparation and discounts are all represented. No positive cycle can synthesize rare materials from a cheap input.

**Primary source:** [EMCMappingHandler.java](https://github.com/sinkillerj/ProjectE/blob/f432b0c66837759fb0731c9144dc53176b949c5d/src/main/java/moze_intel/projecte/emc/EMCMappingHandler.java) · [IEMCMapper.java](https://github.com/sinkillerj/ProjectE/blob/f432b0c66837759fb0731c9144dc53176b949c5d/src/api/java/moze_intel/projecte/api/mapper/IEMCMapper.java)

## 10. R09 — Nature spell properties, with a source limitation

**Observed:** the inspected spell class exposes property/cost structure. The inspected germination ritual on that branch is effectively an empty implementation stub.

**Adopt:** data-described spell properties are a useful pattern for nature utilities.

**Do not infer:** this particular source file proves a completed growth ritual or its actual performance. A class name is not feature evidence.

**Adapt and verify:** implement a small real garden operation on Jugcraft's existing ecology and prove its bounded work, permissions and outputs.

**Primary source:** [Spell.java](https://github.com/MysticMods/Roots/blob/e75e16351e90a44c0d36f37e45bd5d15bfbf337c/src/main/java/mysticmods/roots/api/spell/Spell.java) · [GerminationRitual.java](https://github.com/MysticMods/Roots/blob/e75e16351e90a44c0d36f37e45bd5d15bfbf337c/src/main/java/mysticmods/roots/ritual/GerminationRitual.java)

## 11. R10 — Typed ritual storage and presentation

**Observed:** a forge block entity coordinates multiple essence storage, a ritual manager, indicators, levels and pedestal inventory.

**Adopt:** visibly distinct typed reservoirs and preparation indicators make complex crafting readable.

**Adapt:** current Jugcraft circles, resources and artifice already cover much of this foundation. Add useful status/pattern variants, not a duplicate universal forge.

**Verify:** each essence channel stays typed; indicators reflect authoritative state; pedestal/input reservations settle once.

**Primary source:** [HephaestusForgeBlockEntity.java](https://github.com/stal111/Forbidden-Arcanus/blob/9e0fadb794d0ac317b1048cb814dac6dfff453c1/neoforge/src/main/java/com/stal111/forbidden_arcanus/common/block/entity/forge/HephaestusForgeBlockEntity.java)

## 12. R11 — Bounded utility relic charging

**Observed:** a utility tome charges on a bounded server-side interval from eligible recipe inputs and stores its charge in item data.

**Adopt:** a relic should disclose a specific input, charge cap, activation and outcome.

**Adapt:** use existing Jugcraft relic contexts and recharge rules. Do not create a general duplication catalogue as a side effect of adding a useful charm.

**Verify:** inventory movement, toggling, reload and repeated ticks cannot charge twice or consume beyond capacity.

**Primary source:** [AlkahestryTomeItem.java](https://github.com/P3pp3rF1y/Reliquary/blob/e87d46092697b8a7ff977e27c189b85f5e2083d7/src/main/java/reliquary/item/AlkahestryTomeItem.java)

## 13. R12 — Kinetic capacity and staged intermediates

**Observed:** a kinetic network tracks connected members and capacity/stress; sequenced assembly records an intermediate item's recipe identity and progress.

**Adopt:** show bottlenecks and make multi-stage work physically legible. Preserve intermediate identity across steps.

**Adapt:** Jugcraft already owns its kinetic and machine APIs. Use those and bounded components rather than introducing a second network or importing the reference machinery.

**Verify:** restarting or transferring an intermediate cannot reset costs, skip stages or reroll a final output.

**Primary source:** [KineticNetwork.java](https://github.com/Creators-of-Create/Create/blob/7ff2760e4924d35d4bc2e22c4b19923f0178cd16/src/main/java/com/simibubi/create/content/kinetics/KineticNetwork.java) · [SequencedAssemblyRecipe.java](https://github.com/Creators-of-Create/Create/blob/7ff2760e4924d35d4bc2e22c4b19923f0178cd16/src/main/java/com/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipe.java)

## 14. R13 — Cached recipes with constrained operations

**Observed:** processing evaluates available operations against inputs, outputs and energy, handles blocked/error states and performs defined consumption/completion.

**Adopt:** report why a machine is blocked and determine executable work before committing it.

**Adapt:** use Jugcraft's Fabric transactions, FluidRecipe and machine abstractions. Cross-loader source is a pattern reference, not drop-in API code.

**Verify:** output-full, partial fluids, low power and concurrent extraction never produce partial unaccounted outputs.

**Primary source:** [CachedRecipe.java](https://github.com/mekanism/Mekanism/blob/feb19590eb4bc0f7ebb4aaa295f2e2c681ab5c63/src/api/java/mekanism/api/recipes/cache/CachedRecipe.java)

## 15. R14 — Research stages and infusion requirements

**Observed:** the published API describes research stages with required actions/knowledge and infusion recipes with components, research requirements and instability.

**Adopt:** distinguish learning from crafting and make preparation requirements meaningful.

**Limitation:** this is a published API surface, not a full audit of the original runtime.

**Adapt:** current Jugcraft research and rituals already implement the broad separation. Add varied evidence, comprehensible previews and controlled failure rather than punitive world destruction.

**Verify:** shared notes do not counterfeit mastery; a recipe preview cannot bypass research; failure is bounded and explained.

**Primary source:** [ResearchStage.java](https://github.com/Azanor/thaumcraft-api/blob/79427c4b9b923315bfb7ad28a15254b0cb07160d/research/ResearchStage.java) · [InfusionRecipe.java](https://github.com/Azanor/thaumcraft-api/blob/79427c4b9b923315bfb7ad28a15254b0cb07160d/crafting/InfusionRecipe.java)

## 16. R15 — Ritual patterns and display patterns

**Observed:** the official ritual documentation separates physical rune patterns, displayed lines, reagent positions, consumption flags and handler parameters.

**Adopt:** presentation can clarify a physical pattern without changing its actual requirements. Map visual points to validated locations.

**Adapt:** use Jugcraft's existing StructurePattern and state machine. Do not expose arbitrary completion commands in a player-authored rite.

**Limitation:** related official pages for adding construct tasks and spell parts were placeholders when inspected. They cannot support detailed claims about those implementations.

**Primary source:** [Official ritual schema](https://github.com/Mithion/Mana-And-Artifice/wiki/Recipe-Type:-Ritual)

## 17. R16 — Gardens, bargains and identity-based magic

**Observed:** the author's published description emphasizes gardens, brewing, ritual circles, familiars, covens, dreams, bargains and optional transformations as a different play style from direct combat spellcasting.

**Adopt:** give noncombat preparation and character choices equal design attention.

**Adapt:** Jugcraft already has garden, hex, dream and agreement foundations. Build readable consent, reversal, bounded obligations and practical utility; keep Vitae separate from transformation.

**Limitation:** the legacy official detailed wiki was unavailable during this review. The description supports broad feature families, not exact internal algorithms.

**Primary source:** [Author-published feature description](https://www.curseforge.com/minecraft/mc-mods/witchery)

## 18. Approved framework documentation

For implementation, read the source/docs for the locked release, not merely the newest default branch:

- [Jugcraft framework contract and exact roles](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/FRAMEWORKS.md)
- [Jugcraft dependency lock](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/distribution/frameworks.lock.json)
- [Animation system documentation](https://wiki.geckolib.com/docs/geckolib5/)
- [Casting integration documentation](https://github.com/ZsoltMolnarrr/SpellEngine/wiki)

Framework documentation does not supersede Jugcraft's actual custom impact/cost listeners. Other research projects use different loaders and game versions; this review did not establish runtime compatibility with Fabric 26.3.

## 19. Jugcraft evidence map

| Claim/design boundary | Primary repository evidence |
|---|---|
| Current integrated scope and remaining work | [Integration status](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/INTEGRATION_STATUS.md) |
| Canonical resources, traditions and stages | [Concordance contract](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/ARCANE_CONCORDANCE.md) |
| Existing API/state map | [What exists](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/WHAT_EXISTS.md) |
| Cost settlement | [ConcordanceSpells](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/src/main/java/io/github/jimbozoomer/jugcraft/concordance/ConcordanceSpells.java) |
| Effect authority | [ConcordanceEffects](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/src/main/java/io/github/jimbozoomer/jugcraft/concordance/ConcordanceEffects.java) |
| Ritual phases and constraints | [Ritual feature record](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/arcane-concordance-rituals.md) |
| Vesperine behavior | [Encounter record](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/vesperine.md) |
| Tatterlace behavior | [Encounter record](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/tatterlace.md) |
| Yeti hall and proposed part two | [Open contribution](https://github.com/jimbozoomer-byte/jugcraft/pull/300); [BOSSES.md](https://github.com/jimbozoomer-byte/jugcraft/blob/e12c1d1ce15b7ee50d13306e7f72bfdfff74df8a/docs/branches/BOSSES.md) · [glacier-hall.md](https://github.com/jimbozoomer-byte/jugcraft/blob/e12c1d1ce15b7ee50d13306e7f72bfdfff74df8a/docs/features/glacier-hall.md) |
| Stolas and full roster request | [Roadmap](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/ROADMAP.md); [Styx's conservatory](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/styxhexenhammer.md) |
| Encyclopedia direction | [Owner feature brief](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/jugcraft-encyclopedia.md) |
| Industrial sequence | [Starter workshop](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/industrial-starter-workshop-plan.md); [steel](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/industrial-steel-and-bulk-metallurgy-plan.md) |
| Chemical routes | [Chemistry and fuels](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/industrial-chemistry-and-fuels-plan.md); [catalog](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/industrial-chemical-catalog-and-routes.md) |
| Pollution constraints | [Waste/recycling plan](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/waste-recycling-and-pollution-plan.md) |
| Machine visual scope | [Model briefs](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/industrial-machine-models-and-textures.md) |
| Existing editor limitations | [World Designer guide](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/WORLD_DESIGNER.md); [feature record](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/features/world-designer.md) |
| Required evidence | [Testing](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/docs/TESTING.md) |
| Approved asset inventory | [Magic assets](https://github.com/jimbozoomer-byte/jugcraft/blob/4b36d2254fd76e7db2a4cfa6ec488d746a999f1f/art/owner-library/MAGIC_ASSETS.md) |

## 20. Research limits and unresolved labels

Two earlier inspiration labels did not resolve to an unambiguous intended primary project. Their purported mechanics are not treated as verified evidence in this pack. Exact links would allow a later focused review; they do not block the current source-grounded plan.

The newer material-accounting reference was inspected; the older predecessor was not independently reverse-engineered. Historical similarities should not be presented as an audit of both implementations.

This research inspected selected source files and official documentation. It did not play every reference project, evaluate every branch, audit their entire codebases or verify that all features work in their latest releases. New encounter designs, costs, interface layouts and delivery chunks are original proposals inferred from the observed patterns and Jugcraft's direction.

[Return to owner overview](START-HERE.md)
