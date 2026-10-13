# Mineral sands and shared chemical refining

Status: owner-endorsed planning direction, recorded 6 October and updated 10 October 2026. This document records choices for planning steps 4 and 5 and the linked tenth and thirteenth chemistry batches. Proposed deposit profiles, machine names, recipe stages and consumer examples are designs to develop, not claims of implemented gameplay. Quantities, worldgen frequency, capacities, energy costs and individual recipe behavior are not finalized.

Primary specialties: regional mining, mineral processing, ceramics, chemical refining and precision materials.
Related documentation: [chemistry](../branches/CHEMISTRY.md), [machine roadmap](../MACHINE_ROADMAP.md), [current technology](../TECH_TREE.md), [resource deposits](resource-deposits.md), [industrial chemistry](industrial-chemistry.md).

## Explicit owner choices

- **Major progression route:** several important later materials primarily come through mineral-sand processing.
- **Dedicated mineral-bearing sand:** valuable material comes from particular sand types plentiful in suitable areas. Do not incentivize destroying every ordinary beach for trace industrial minerals.
- **Strong regional specialization:** different regions supply distinct primary minerals; exploration, transport and trade are central.
- **Moderate processing depth:** a short concentration route followed by specialized separation where a product needs it. Machines serve several recipes rather than requiring every possible fraction for every product.
- **Hybrid extraction:** exposed mineral sand can be collected for startup quantities; a dedicated extractor draws sustained production from a large, finite deposit with limited surface disturbance.
- **Shared chemical machines with distinct roles:** leaching/digestion, filtration, chemical separation, precipitation/crystallization and recovery equipment handle several materials. Dedicated equipment is appropriate for genuinely different process conditions.
- **Initial named rare-earth set, with expansion explicitly planned:** begin with neodymium, cerium and yttrium products. The owner said, "A but yeah definitely mark it to be expanded." These three are the first slice, not the final roster.

Earlier choices remain constraints: useful pre-electric industry, a manageable route into each next capability, many sideways products, first electricity before mod steel, optional motor-driven reuse of workshops, and selected cross-industry dependencies. This proposal does not introduce routine lubricant consumption or replacement-part maintenance.

## Regional deposits and ordinary sand

Use recognizable mineral-bearing feedstocks and explicit processing recipes. Ordinary sand, red sand and mineral-depleted outputs do not gain valuable heavy-mineral recovery recipes simply through a generic sand tag. Keep ordinary sand useful for construction and glass.

Deposits should be plentiful within their eligible regions and large enough to support a meaningful factory. Suitable settings include inland basins, ancient coastal formations and particular river/alluvial regions as well as selected coastal areas. The following profiles are game-design candidates; they are not final biome assignments or a claim that each real geology produces one pure mineral.

| Candidate deposit family | Primary supply | Proposed consumers |
| --- | --- | --- |
| Titanium-rich mineral sand | Ilmenite and rutile-bearing feed | Titanium refining, pigment products and suitable ceramic ingredients |
| Zircon-rich mineral sand | Zircon-bearing feed | Glazes, foundry materials, refractories and later zirconium chemistry |
| Rare-earth mineral sand | Monazite and potentially xenotime-bearing feed | Rare-earth separation and named material products |
| Garnet-rich mineral sand | Suitable garnet-bearing feed and associated minerals | Abrasives, grinding media and polishing products |

Secondary minerals can be useful, but should not make every deposit a sufficient source of every region's primary material. Regional identity must survive processing: mined and extracted feed retains its deposit family through its recipe path.

Existing underground titanium/monazite finds can remain supplemental sources while industrial quantities primarily come from sand deposits. Preserve stable existing registrations and a reachable route when rebalancing acquisition; this document does not remove ores or change current worldgen.

## Hybrid mining-site experience

1. Players recognize an exposed deposit and shovel startup batches of its mineral-bearing sand.
2. A simple ground sampler identifies the mineral family and roughly indicates the deposit's extent. It should have an entry route without advanced electronics. Later prospecting equipment can provide better readings.
3. A small shaft-powered sand extractor supplies unprocessed feed from the deeper reserve, using reachable early metals and mechanical parts.
4. A motor drives the same equipment after electrification. Larger extractors improve throughput and handling where useful.
5. The deposit is large but finite. Surface collection and powered extraction must account for material already removed; neither can collect the same reserve twice.

Exact reserve representation is undecided. Exposed blocks and a deeper stored reserve must have a defined resource budget, persist on restart/unload, and resist duplication through replacing blocks, repeated sampling or moving the extractor. These are implementation requirements, not a decision to copy the existing deposit capacity unchanged.

Depletion should have readable feedback. Larger equipment changes extraction rate/capability, not the quantity of material magically present. Mining automation uses bounded loaded-area work, existing ownership rules and shared item/power interfaces; do not add chunk loading as an implied requirement.

## Moderate physical separation

| Operation | Proposed equipment | Output and purpose |
| --- | --- | --- |
| Screen and wash | Hand/basic preparation and later screening/washing equipment | Remove oversized material and prepare suitable feed |
| Gravity concentration | Shaft-powered shaking table or spiral concentrator | Recover a heavy concentrate while separating most ordinary sand |
| Selected magnetic separation | Magnetic drum using reachable electromagnets | Recover appropriate magnetic fractions |
| Selected electrical separation | Later electrostatic separator | Separate fractions requiring that capability |
| Grinding and grading | Mill and grading screens | Prepare usable powders or approved abrasive grades |

Each feed/product recipe uses the needed subset of these operations. Use only intermediates with a clear processing, storage, trade or consumer role. Basic suitable abrasives and ceramic materials can have direct mechanical preparation routes; not every mineral must undergo chemical refining.

Starter magnetic equipment must not require rare-earth magnets produced by its own separation chain. Reachable copper/iron electromagnetic equipment can provide the initial capability; rare-earth components can support later approved upgrades. Electrostatic separation requires an electrical capability and appropriate equipment rather than pretending to be a purely mechanical operation.

Mineral-depleted sand should have useful construction outlets and remain depleted. Define concentrate recovery and material accounting before applying any existing ore-yield multipliers. Generic washing, compaction, grading or ore tags must not permit repeated mineral recovery from the same input.

## Shared chemical equipment

| Role | Proposed shared uses | Capability boundaries |
| --- | --- | --- |
| Leaching/digestion vessel | Appropriate acid or alkaline treatment of different mineral feeds | Material compatibility and heating; gas pipes automatically supply process pressure under the later owner choice |
| Filter press | Separate useful solutions from solid residues | Suitable durable filters; approved recovery/purity behavior |
| Precipitation/crystallization tank | Recover useful compounds from solution | Required reagents, temperature and product-specific recipes |
| Chemical separation unit | Separate selected dissolved materials, including rare-earth fractions | Appropriate separation chemistry; avoid one universal recipe for unrelated feeds |
| Calciner | Convert prepared compounds into useful oxide powders | Required temperature/atmosphere with ceramic equipment connections |
| Electrolytic cell | Selected metal and reagent production | Appropriate electrodes and bath conditions; molten-salt and aqueous operations need their real capability distinction |
| Sealed reduction retort | Controlled-atmosphere reduction and appropriate recovery operations | Dedicated process hardware where shared open vessels cannot perform the job |

A line uses the operations it actually needs. Small batches stay practical. Larger stations can offer throughput; control, heat, lining or electrode changes can unlock genuinely different processes. The later chemistry choice makes gas-pipe pressurization automatic, rather than a player-managed pressure tier or compressor prerequisite. Preserve shared kinetic/electrical/item/fluid systems rather than inventing parallel APIs or energy currencies.

Connections to earlier industries include ceramic linings/insulators, suitable agricultural filter cloth and seals, basic metal vessels and later compatible materials. Essential components need a reachable hand/solo/trade route. These connections must not require every farmer or engineer to complete every specialty.

The [twelfth chemistry batch](industrial-chemistry-and-fuels-plan.md#grid-storage-synthetic-fuels-and-cryogenics-decisions-twelfth-batch) selects mineral-derived vanadium electrolyte alongside refinery residue and independent cobalt stock for reusable synthetic-fuel tooling. The [thirteenth batch](industrial-chemistry-and-fuels-plan.md#resource-reagent-and-residue-decisions-thirteenth-batch) now selects galena lead, spodumene-bearing rock first with natural lithium brines later, **separate primary cobalt ore**, regional chromite concentrate and specifically vanadium-bearing iron concentrates. Cobalt mineral identity and new source regions/refining receipts remain open. Preserve current feeds and independently reachable earlier vanadium preparation for the first acid catalyst; ordinary iron does not universally yield vanadium.

The thirteenth batch also selects an optional **gypsum-rich phosphate-refining residue -> paid game purification/preparation -> suitable plaster/board ingredients** branch on compatible shared construction equipment. Define reagent/energy/yield/loss receipts and useful finished products before implementation. Raw residue is not the finished ingredient; this fictional route is distinct from stabilized coal-cleaning and bauxite filler stocks.

## Initial refining lines and finished products

### Alumina and aluminum

Candidate route: bauxite preparation, alkaline digestion, clarification/filtration, recovery of a precipitate and calcination into alumina. Alumina then branches into appropriate ceramic products or aluminum electrolysis.

The [7 October material planning follow-up](industrial-chemistry-and-fuels-plan.md#material-production-decisions-third-batch) selects aluminum's broad first product range: electrical parts, structural stock, building panels and vehicle components. Initial refining stations are compact functional installations, with optional larger bulk plants. Exact recipes/capacities remain to design; oxide/ceramic and metal consumers keep independently useful roles.

This gives the refinery a useful oxide output as well as a metal consumer. Preserve existing alumina/aluminum identities and keep prior reachable recipes until their replacement route works. The inspected electrolytic-cell recipe uses aluminum cable while producing aluminum. In the [7 October chemistry planning follow-up](industrial-chemistry-and-fuels-plan.md#machinery-and-operation-decisions-second-batch), the owner selected **earlier copper conductors for entry aluminum-processing equipment**, with aluminum improvements later. Exact construction recipes remain to design; verify this bootstrap before the current starting route is removed.

The [tenth chemistry batch](industrial-chemistry-and-fuels-plan.md#magnesium-aluminum-and-titanium-processing-decisions-tenth-batch) selects powered recovery/reuse of most digestion lye with makeup losses, consumed coke/carbon for electrolysis and optional CO2 capture. Stabilize the distinct bauxite residue into modest road/filler stock through shared construction equipment. Exact binder, yields, quantities and energy remain pending; this is not raw residue placement or routine electrode replacement.

### Magnesium feed and recovery

The owner selects **seawater pumped from beach or ocean biomes -> concentration -> magnesium-bearing brine -> magnesium hydroxide -> HCl-based preparation/conditioning -> dry prepared MgCl2 item -> internally heated molten-salt electrolysis -> magnesium/chlorine**. Beach and ocean are both eligible source categories. Exact source tags, intake validation, pumping/preparation quantities, reagent/water receipts, drying and losses remain to specify. A freshwater intake does not become mineral feed because the machine stands near a beach. This pumped route does not require stripping shorelines or introduce finite ocean depletion through the mineral-sand deposit rules.

Use earlier materials/ceramics for the first equipment and magnesium batch. Returning titanium-reduction salts can supplement that supply from the first titanium installation, with paid recovery and makeup losses; it cannot bootstrap the initial magnesium. Dry item feed melts inside the cell, without a separately transported molten-chloride requirement or player pressure management.

### Titanium materials

Separated titanium-bearing minerals can branch into pigment products or a metal-refining route. The owner selected **mineral treatment -> crude titanium chloride -> purification -> magnesium reduction -> titanium sponge -> melting**, using compatible shared equipment. The tenth batch selects shared chemical distillation for chloride purification and paid automatic sponge cleanup inside the reduction equipment, returning salts separately before melting. Exact quantities, conditioning and construction remain to design. Destinations include advanced process/precision equipment, vehicle components and a stronger titanium tool/armor tier above steel.

The current titanium recipe compresses chemical recovery into one conversion. The selected magnesium reduction needs an independently obtainable feedstock and equipment built before titanium. The owner selects recovery of magnesium/chlorine from spent MgCl2 salts from the first titanium installation, with explicit energy, conditioning and losses; it cannot supply the very first magnesium batch.

Maintain existing titanium/sponge consumers and define the purpose of any introduced intermediate. The retort, purifier and melting equipment cannot require their own titanium output to be constructed. Exact chemistry simplifications and coexistence with current recipes need a focused implementation design.

### Rare-earth products

Candidate route: prepared mineral concentrate, appropriate chemical treatment, filtration, chemical separation, then finished compounds and suitable metal/alloy recovery where needed. Do not imply that a mixed oxide automatically becomes a pure magnet metal merely by shaping it.

| Initial named family | Proposed material/output role | Proposed finished consumers |
| --- | --- | --- |
| Neodymium | Appropriate recovered compound followed by a defined metal/alloy route where required | Permanent magnets, improved motors/generators and selected equipment |
| Cerium | Appropriate oxide/polishing products | Precision glass, lenses, mirrors and surface-finishing supplies |
| Yttrium | Appropriate compounds such as oxide additions | Selected zirconia ceramic components and later reviewed precision-material applications |

Form, purity, grade and actual recipes remain to be designed. Not every named material needs ore, ingot, nugget and block forms; give it the forms its consumers need. Existing mixed rare-earth oxide and magnet recipes need a documented coexistence or staged transition, without stranding current saves or removing the only reachable route.

### Direct mineral uses

Zircon and garnet retain suitable direct preparation routes for ceramic materials and abrasives. New advanced zirconium or other metal products should be introduced with real consumers, rather than adding every possible element simply because a mineral contains it.

The later material batches select powder preparation -> shaping blanks -> firing -> shared grinding/polishing where needed, with clay foundation, porcelain-style insulators and alumina components, then specialist zirconia. Shared finishing also serves metal and optical consumers. Compact refining entry does not erase the substantial first digester or clay-based first steel casting. The selected initial alloys are chromium/nickel stainless steel and aluminum-magnesium structural alloy; titanium alloys come later and exact compositions remain to design. Regional chromium-bearing ore deposits now feed named chromite concentrate and shared refining; regions, producer recipes and stock forms remain open. The selected beach/ocean seawater and dry-chloride route supplies titanium reduction and aluminum alloying. Whole-ingot batches are selected for both first alloys; numerical ratios and certified grades remain unselected. Exact source validation, preparation receipts and compatible early hardware remain to design. This metal expansion follows the selected first gas/acid factory. Larger chemical/refining machines gain throughput and lower energy per unit, with useful upgraded small stations.

The [expanded chemical catalog](industrial-chemical-catalog-and-routes.md#fluorite-hydrofluoric-acid-and-phosphate-wet-processing) requests fluorite/concentrated-sulfuric HF production and phosphate-rock wet/hydrometallurgical processing into phosphoric acid. The owner selects substantial underground fluorite deposits in snow biomes with occasional surface outcrops; exact eligible biome IDs/tags, abundance/depth, extraction and outcrop placement remain to design. This is a game resource-distribution choice and implements no worldgen or changes to generated chunks. Shared dissolution/filtration/recovery and reagent purification use specific recipes, independently obtainable feeds and explicit residues; water conditioning is accounted within recipes. Named nitrogen/phosphate fertilizers use broad crop groups and optional mixed-farm blends. PTFE parts enter advanced-machine construction recipes rather than lining retrofits. The catalog links these minerals to advanced wafers, fertilizers and later LiPF6 storage; ordinary sands do not all become fluorite/phosphate sources.

## Rare-earth expansion is a planned follow-up

**The initial neodymium/cerium/yttrium set is not a permanent limit. Expansion is explicitly requested by the owner.** Keep it visible in the chemistry and machine roadmaps, and revisit it as precision manufacturing, optics, catalysts, advanced ceramics, improved magnets, alloys and expedition equipment gain concrete consumers.

For each additional named material:

- Name one independently useful application and preferably several connected products or sidegrades.
- Define its regional/mineral feed and a reachable separation/recovery route using shared equipment where suitable.
- Identify useful output forms, required purity and any real reason for dedicated process equipment.
- Explain its role beside existing materials, rather than adding an arbitrary compulsory ingredient to every later recipe.
- Record quantities, energy/reagent costs, losses and residue/recovery behavior when implemented.
- Add the material and its consumers together in a focused slice; preserve saved registrations and avoid ingredient cycles.

These criteria guide the required expansion; they do not cancel it. Exact additional elements and their sequence remain open. The intention is a growing roster with useful product depth, not a fixed three-material end state or a mandatory complete periodic table.

## Source observations and implementation boundaries

The inspected source includes surface resource deposits, a deposit drill, prospecting, ore washing/sifting, a Settling Plant, chemical reactors, electrolysis, simplified bauxite/titanium/rare-earth recovery and existing magnets. Use [deposits data](../../tools/deposits.py), [machine data](../../tools/machines.py), [material data](../../tools/materials.py) and [petrochemistry data](../../tools/petro.py) as current-source references. Recheck the source before implementation; these observations are not new game-test evidence.

The related [industrial agriculture plan](https://github.com/jimbozoomer-byte/jugcraft/blob/docs/industrial-agriculture-plan/docs/features/industrial-agriculture-plan.md) is published separately in [PR #208](https://github.com/jimbozoomer-byte/jugcraft/pull/208). It offers selected filter, belt, coating and finished-material connections without adding new routine upkeep. Use its merged main location once available.

This documentation change adds no gameplay, recipes, registries, runtime assets, worldgen or save changes. Future code must follow [testing guidance](../TESTING.md), prove entry paths, audit mineral/reagent/fuel loops, and test reserve persistence, full outputs, interrupted processing, ownership, unload/reload and relevant multiplayer behavior. Do not remove chemistry stand-ins until the replacement is reachable.

## Next planning step

Step 6 is waste recovery and recycling: useful outlets for depleted sand, process residues, scraps and spent fluids; optional recovery of materials/reagents; selected treatment where appropriate; and the owner's choices about mandatory handling versus additional efficiency. Pollution effects, maintenance and waste-blocking rules are not approved by this document. The existing flowback-treatment route is a starting point to inspect, not permission to extend compulsory waste to every machine.

## Primary process references

Sources inform sensible roles and distinctions; game recipes remain deliberate simplifications and do not import industrial quantities as balance numbers.

- [USGS: heavy-mineral sand deposit model](https://www.usgs.gov/publications/deposit-model-heavy-mineral-sands-coastal-environments).
- [USGS: inland/coastal distribution and physical separation](https://www.usgs.gov/centers/gggsc/science/critical-mineral-resources-heavy-mineral-sands-us-atlantic-coastal-plain).
- [Iluka: inland zircon and rare-earth mineral sands](https://www.iluka.com/operations-resource-development/resource-development/wimmera).
- [International Aluminium Institute: alumina refining](https://alustory.international-aluminium.org/mining-refining/process-refining/).
- [Toho Titanium: chlorination, purification, reduction and recovery](https://www.toho-titanium.co.jp/pdf/company/companyprofile_en.pdf).
- [Iluka: rare-earth refinery process](https://www.iluka.com/media/xyfjf0fr/iluka-investor-briefing.pdf).
- [Solvay: cerium oxide polishing material](https://www.solvay.com/en/product/opaline-polissage).
- [CeramTec: yttrium-reinforced zirconia textile cutters](https://www.ceramtec-industrial.com/en/products-applications/textile-machinery/cutters).

AI-assisted planning documentation: OpenAI Codex, GPT-6 family. No gameplay feature implementation is included.
