# Industrial steel and bulk metallurgy

Status: owner-selected planning direction, recorded 6 October and updated 7 October 2026. The steel entry, hand plates, larger workshops, shared forming dies and optional steel casting choices below are selected. The latest ten-answer batch selects accessible clay-based casting refractories, shared bench/kiln tooling, common components, internal item-based casting, automatic completion at readable heat levels, a shaft-driven larger press, an optional larger Coke Oven and heat upgrades for compatible alloy equipment. Industrial chemistry and advanced materials is the selected next topic. Exact products, equipment specifications and costs remain proposals for discussion. The existing coke route is present in source; this document implements none of the selected new recipes, tooling or equipment.
Proposal issue: direct owner choices during industrial planning; no separate issue.
Owner: jimbozoomer-byte.
Target capability: accessible steel production alongside first electricity, followed by optional bulk metallurgy and useful steel products.
Primary specialty and supported player role: metallurgy, workshop engineering and industrial construction.
Related documentation: [starter workshop plan](industrial-starter-workshop-plan.md), [existing steel feature](steel-tier.md), [current steel technology](../TECH_TREE.md#steel-tier), [machine roadmap](../MACHINE_ROADMAP.md) and [testing](../TESTING.md).

## Explicit owner choices

- **Use Coke Oven -> Steel Foundry as the simple core steel route.** Additional refining operations belong to later expansions. Do not insert mandatory pig iron and a second refining machine before the first steel ingot.
- **Allow charcoal-based starter steel production.** Coke should suit efficient bulk production. The alternative uses the same Steel Foundry and produces the same steel material, rather than a separate inferior starter-steel currency.
- **Make small batches of steel plates with the hammer at the shared workshop bench.** Machines provide faster production and handling, with the same metal yield. A shaft Metal Press is an optional mechanization purchase rather than the only route to the first steel plate.
- **Include larger foundries and presses alongside upgrades for existing machines in the first bulk-production expansion.** Larger stations offer batch handling and throughput; small stations stay useful. This selects the expansion structure, not final recipes, footprints or performance multipliers.
- **Use one larger press with reusable, interchangeable dies for plates, rods and structural shapes.** Selected 7 October; forming jobs share the larger press rather than requiring separate rolling/shaping machines for each ordinary output. Wire drawing and circuit assembly keep their separate roles.
- **Include optional steel casting at this stage, using reusable molds and suitable heat capability.** Selected 7 October; develop products such as gears and housings alongside the existing plate-based routes. Casting is a sideways expansion, not a new mandatory step for all steel products.
- **Use a casting basin attached to the larger foundry, with a suitable heat upgrade.** Selected 7 October; a separate independently heated casting station is not the initial selected arrangement.
- **Steel casting is reachable before electricity through fuel-fired heat and hand-operated bellows, with later shaft automation.** Selected 7 October; appropriate heat-resistant materials are still required. Manual and shaft bellows support the same casting capability rather than producing different steel grades.
- **Use accessible upgraded clay-based linings and molds for first steel casting.** Specialist mineral refractories improve later equipment; a rare regional refractory is not required for the first cast.
- **Make first dies and molds at the shared bench and kiln from earlier supplies.** Initial tooling does not require a dedicated toolmaking station or a product available only from the equipment it enables.
- **Use common plates, rods, gears and housings for machinery.** Specialist small parts belong where they add a useful product or capability, rather than a universal bolt/rivet/spring chain.
- **Load metal items and handle melting and transfer internally at the attached casting setup.** Exposed molten-metal storage and transport are outside the initial selected arrangement.
- **Finish casts automatically once the required heat and materials are supplied.** Players still load inputs and operate manual bellows; individual casts do not require a player pouring action.
- **Show clear heat levels and readable recipe requirements.** Numeric temperature tuning and adjustable fuel/air controls are not required for this casting entry.
- **Drive the larger press with shaft power, including later electric-motor drive.** A larger press does not introduce an electric-only forming gate.
- **Include an optional larger Coke Oven for bulk production.** Banks of ordinary ovens remain valid; the larger version rewards volume and handling.
- **Extend compatible existing alloy equipment through heat upgrades for the next alloy recipes.** A separate heavy alloy furnace is reserved for a genuinely new process capability, rather than the default next purchase.
- Retain the earlier selected industrial structure: a manageable essential route with many useful sideways products; steel develops in parallel with electricity. A first electrical generator or basic circuit does not acquire a new steel requirement.
- Earlier workshops remain useful and can accept motor drive. Larger replacements are optional for ordinary processes; new equipment is required only when a process actually needs a new capability. These principles come from the [starter workshop plan](industrial-starter-workshop-plan.md).

These directions are owner-selected. The latest batch was answered A for all ten choices on 7 October, including **industrial chemistry and advanced materials as the next major topic**. Exact charcoal quantities, work rates, construction recipes, tooling/product identities and station specifications remain to develop. The owner requested batches of **8–12 inline planning questions at a time**; use that cadence for subsequent decisions instead of two-question exchanges.

## Player experience and reachable entry

Start with ordinary iron, bricks and furnace construction. Make the foundry's iron plates through the selected early hammer/bench route or reachable mechanical press. Load iron and charcoal to obtain a small amount of steel without a power grid or a coal-coking plant. Build a Coke Oven when coal supply and production volume justify it; send its coke to the same foundry.

The intended paths are:

1. **Starter alternative:** logs -> ordinary charcoal production -> charcoal + iron -> Steel Foundry -> steel.
2. **Core bulk route:** coal -> Coke Oven -> coke + iron -> Steel Foundry -> steel.
3. **Useful outputs:** steel -> tools/armor, workshop parts, industrial building materials and fluid storage/transport; expand into larger machinery as needed.

Small steel jobs continue at the same hammer/bench used for earlier plates. A player can make the first plates and plate-based gears before purchasing a mechanical press, then automate forming at workshop scale and add a larger press when bulk orders justify it. These are options within a useful production branch, not a compulsory sequence of every station purchase.

The charcoal entry means the Coke Oven is optional for the first steel batch. Coal/coke remains an important production branch rather than an entry toll. Both routes retain a foundry construction purchase; neither needs steel to construct its own first foundry. Basic circuits, oxygen supply, mineral-sand refining and completion of the farming/chemistry branches are not starter-steel requirements.

These are planned reachability rules. The hand-produced iron-plate route is selected in the starter plan but is not claimed to exist in current recipes. Audit the whole construction chain during implementation, including the ordinary blast furnace and hopper, rather than checking only the foundry's operating power.

Solo players can stage this production using common materials; trading for iron, charcoal, coke, plates or finished steel is an equally valid shortcut. Steel production should remain worthwhile for builders, toolmakers and workshop suppliers without requiring mastery of every specialty.

## Observed source baseline

At main snapshot `038833f8`, [tools/machines.py](../../tools/machines.py) defines the following. These observations are not new owner-approved balance numbers or gameplay test results.

| Equipment or process | Current source | Planning consequence |
| --- | --- | --- |
| Coke Oven construction | 4 brick blocks + 4 iron ingots + 1 furnace | Earlier materials; no steel or electrical part |
| Steel Foundry construction | 5 brick blocks + 1 hopper + 2 iron plates + 1 blast furnace | Earlier iron plates must be reachable before its first steel |
| Coke production | 1 coal -> 1 coke; 600 ticks; no electrical or separate heating-fuel input | Reuse the existing machine and material identities |
| Steel production | 1 iron ingot + 1 coke -> 1 steel ingot; 400 ticks; no electricity | Preserve the working coke route while adding the selected charcoal alternative |
| Steel plate forming | Metal Press: 1 ingot -> 1 plate; 100 ticks | Retain this metal yield for the selected hammer/bench alternative; its manual work rate is still open |
| Optional oxygen boost | Foundry data accepts oxygen at 2 mB/t; the technology record describes doubled speed | An existing optional improvement, not a prerequisite or a newly designed refining step |

The Coke Oven currently occupies 2 x 2 blocks, 2 tall plus its chimney; the Steel Foundry occupies 2 x 2 blocks, 5 tall. Each is placed from one crafted machine item. The listed brick quantities are construction-recipe ingredients, not an instruction to build a second multiblock shell. Size/cost changes require their own reviewed design.

Current data contains only the coke-based foundry recipe. Do not imply that charcoal already works, that the approved manual iron-plate route is implemented, or that a separate heating-fuel slot already exists. Current foundry operation uses its charge rather than electricity; this plan does not silently add a third consumable.

[tools/generate_material_data.py](../../tools/generate_material_data.py) now supports crafting-table hand plates for metals with a `hand_plate` setting. At this snapshot, [tools/materials.py](../../tools/materials.py) applies it to thallite at two ingots per plate; steel has no such setting. That specific existing fallback is not the selected steel hammer/bench route or its equal-yield policy. No existing recipe is changed here.

## Selected steel forming and workshop expansion

Use the shared bench and reachable starter hammer for steel plates; no new steel-only hammer or powered station is required to make the first plate. The forming baseline is **1 steel ingot -> 1 existing steel plate**, matching the current press's 9 nugget units in and out. Hand-made plates use the same identity, tags and consumers, without a quality or metal-loss penalty. The hammer is a tool rather than a consumed recipe ingredient; ordinary tool durability/work timing remains to balance separately.

The existing **4 steel plates -> 1 steel gear** recipe then provides a reachable small-batch gear route, conserving 36 nugget units, alongside the selected optional casting branch below. Manual plates do not authorize hand manufacture of every advanced alloy/precision part. A plate-based tank, fitting, gear or construction product can be made in the quantities actually needed before a larger workshop is purchased.

| Production option | Selected role | Cost and capability boundary |
| --- | --- | --- |
| Hammer at shared bench | First steel plates and occasional small orders | Active player work; same metal yield as the press; no mandatory electricity |
| Existing workshop Metal Press | Routine production with shaft drive and later motor drive | Faster work/automation; remains useful for small factories and secondary lines |
| Existing-machine upgrades | Improve an eligible small station within its supported upgrade system | Optional alternative or companion to a larger purchase; do not assume every unpowered machine already accepts upgrade cards |
| Larger Metal Press with reusable dies | Bulk plate, rod and structural-shape orders for tanks, building stock and machinery | Shaft drive with later motor drive; exact product list, construction, drive capacity and batch rate remain to define |
| Larger Steel Foundry | Sustained steel supply for those bulk orders | Selected larger station; starter-produced steel and earlier ceramics/parts must make its construction reachable |

Larger construction should consume earlier steel plates/gears and suitable ceramics/ordinary parts, rather than requiring a component only the new machine can produce. Larger machines reward volume, throughput and handling; keep ordinary steel and plate recipes usable on small equipment. New process-specific stock or alloy capabilities can justify additional equipment only where that capability is genuinely needed.

Batch sizes, extra inventories, processing slots and drive/heat requirements remain specifications to review, not automatic benefits already implemented. Size foundry expansion against carbon supply and press expansion against shaft/motor capacity; an enlarged model alone does not prove increased output or acceptable performance. Factory-scale recipe/inventory work must remain bounded and inactive stations cheap.

## Carbon supply, material accounting and pacing

Both inputs produce ordinary `jugcraft:steel_ingot`. Preserve one iron ingot's metal in one steel ingot: 9 nugget units in and 9 out. Charcoal/coke pays for the process and does not authorize additional metal output. Plates and gears retain the shared material units and identities. Equivalent manual and machine forming recipes follow the selected equal-metal-yield policy.

**Proposed balance direction:** charcoal uses more carbon per steel and/or more foundry time than coke, while remaining practical for the first tools and workshop parts. Exact quantities and timings remain open. Coke earns its bulk role through operating economy and sustained output, without reducing the charcoal route's steel quality or adding a mandatory second steel-refining operation.

Compare complete production chains: charcoal includes wood acquisition and charcoal firing; coke includes coal acquisition, oven construction and coking time. An unboosted source-baseline foundry can consume coke every 400 ticks while one oven supplies it every 600 ticks, so a single oven limits sustained output. A faster foundry alone does not fix that supply bottleneck. Those rates are data observations, not a benchmark or a final required oven/foundry ratio.

Count operating fuel, carbon, power where applicable and upstream machine costs explicitly. The starter workshop's roughly 3–4 times manual-to-small-machine throughput target does not automatically assign the same multiplier to every steel expansion or oxygen boost. Larger equipment must conserve metal; speed or fuel improvements need distinct, audited costs.

Use the existing hopper, inventory, side-configuration and transport systems. Manual loading stays valid. Belts, pipes, buffers and filters are optional purchases rather than a compulsory factory before steel. The [conveyor performance follow-up](https://github.com/jimbozoomer-byte/jugcraft/pull/230) records the owner's conditional use of item pipes without visible moving items for costly late-game belt workloads.

## Useful steel products and sideways progression

This is a proposed expansion map, not approval of every product or a requirement to finish each branch. Reuse existing outputs and audit their actual recipes before adding new registrations. A product's visual name does not prove it currently consumes steel: for example, some Dieselworks plate/grating recipes use iron.

| Branch and end result | Forming/assembly route to develop | Existing connection and proposed depth |
| --- | --- | --- |
| Industrial buildings: beams, sheet panels, grating, catwalks, brackets and railings | Plates and shaped stock through workshop forming; larger forming equipment for batches | [Dieselworks](dieselworks.md) already records beams, panels and grating. Extend useful building sets where gaps remain, with consistent material accounting and the owner's clean steel art direction |
| Fluid works: tanks, pipes, valves, strainers and fittings | Steel plates plus reachable earlier pipe/tank components; ordinary assembly | [Fluid logistics](fluid-logistics.md) and the existing Steel Tank provide consumers. Larger storage and routing are optional improvements for farms, steam workshops and chemistry |
| Workshop equipment: gears, frames, press heads and upgrade components | Existing plate/gear routes and assembly; discuss larger press variants below | Steel plates/gears already feed [machine upgrades](machine-control.md). Proposed structural parts can support sturdier or larger equipment without adding a separate frame currency to every old recipe |
| Player equipment: tools, armor and selected workshop implements | Existing ingot-based crafting where available; forming for proposed specialist parts | [Tools and armor](tools-and-armor.md) already records steel sets. Review their reachability and usefulness; do not rebuild this branch as new electric-only gear or introduce routine machine-part replacement |
| Production industries: larger mill/press heads, cutting blades, machinery housings and supports | Shared forming plus the relevant assembly station | Candidate uses in crop milling/oil pressing, sawmilling, pumps and material handling. Choose consumers that gain a concrete capability or scale improvement rather than requiring every farmer to industrialize |
| Later alloy/refining works: durable equipment structures and suitable furnace/handling parts | Steel forming plus appropriately staged ceramics and other materials | Connect selected [mineral-sands/refining](mineral-sands-and-refining-plan.md) equipment. Steel is one construction input where sensible; producing all titanium, aluminum or rare-earth products is not needed to make ordinary steel |

Steel products should be useful as end results, trade goods and equipment materials. A building palette or tool set can be a satisfying destination independently of progressing to another tier. Existing bronze/brass products retain their roles. Magic connections may use shared ordinary materials where already appropriate; no new magic-school completion requirement is selected here.

## Selected expansion structure and proposed machine roles

The first expansion wave includes **larger Steel Foundry, Metal Press and optional Coke Oven stations alongside existing-machine upgrades**, as selected above. Extend compatible alloy equipment with heat upgrades for the next alloy recipes. The table develops those roles and marks additional equipment as candidates. Reuse shared machines, recipe families and interfaces rather than creating unrelated specialist variants for every metal.

| Expansion role | Inputs and useful output | Why build it; entry constraint |
| --- | --- | --- |
| Larger Coke Oven (selected optional station; specification draft) | Existing coal -> existing coke | Improve coal throughput/handling for several consumers. Standard ovens and oven banks remain valid; proposed construction uses previously obtainable steel and ceramics |
| Larger Steel Foundry (selected station; specification draft) | Earlier iron and approved carbon inputs -> ordinary steel in larger batches | Optional sustained production and handling/operating economy. A starter foundry supplies its steel parts; no larger-foundry-only part in its own construction |
| Larger Metal Press with reusable dies (selected arrangement; specification draft) | Suitable metal stock -> plates, rods and structural shapes | Shaft-powered batch forming with later motor drive; equal metal yields. Output recipes, drive requirements and operating costs remain to define |
| Heat upgrades for compatible alloy equipment (selected direction; recipe/specification draft) | Earlier metals and appropriate upgraded heat/lining -> compatible next-stage alloys | Extend existing equipment where its process supports the recipe. Identify each alloy's producer and consumer; separate equipment remains possible for genuinely new capabilities, without pig-iron refining in ordinary starter steel |
| Electric Arc Furnace steel option (candidate) | Reachable iron/carbon and electricity -> ordinary steel | Consider a later bulk recipe on the already registered Arc Furnace, not a duplicate machine. Its existing registration does not mean an electric steel recipe is already present or required |

Early bronze/brass ceramic gear casting does not automatically permit steel casting in the same crucible. The selected steel-casting branch uses a basin attached to the larger foundry, a suitable heat upgrade, accessible upgraded clay-based refractories and fuel-fired heat supported by manual-to-shaft bellows. Exact clay mixes, firing recipes and attachment specifications remain to develop. Furnace linings, molds, dies and machine parts remain reusable construction equipment; this plan adds no routine lubricant, filter, mold or lining replacement requirement.

## Selected reusable forming dies

The larger press has a reusable tooling position; install a suitable die to choose its supported forming recipe family. Dies are equipment, not ingredients consumed per product. Changing dies lets the same station serve different orders. Exact installation/UI details and automatic die changing remain implementation decisions; the owner selected interchangeable tooling, not an automated tool changer.

| Die family | Product role | Material and entry rule |
| --- | --- | --- |
| Plate die | Existing steel plates for tanks, fluid fittings and ordinary machinery; bulk building-panel orders | Keep the 1 ingot -> 1 plate metal baseline; machine speed/batching is the benefit |
| Rod die | Steel rod/bar stock for useful supports, fittings or selected mechanical parts | Exact stock identities, consumers and batch quantities are proposals; audit material units before adding recipes |
| Structural-shape die | Suitable beam/section stock for industrial frames, catwalk supports and buildings | Reuse existing building identities where appropriate. Audit existing beam recipes and any new forming/recovery route together; a visual shape is not a license for extra recoverable steel |

**Selected tooling entry:** make initial dies and molds through the shared bench and kiln using previously obtainable metal/parts and suitable clay-based materials. Use the station appropriate to each material; this does not require firing every metal die. Tooling cannot require a die, cast component or shaped product available only from the equipment it enables. Exact ingredients remain to design. One station can run a chosen forming job, while several presses with different dies can specialize parallel lines. Neither arrangement is required for starter steel plates.

Keep the separately selected Wire Drawer and Circuit Assembler roles. The owner selected common plates, rods, gears and housings as the ordinary component set; specialist small parts need a meaningful use. Introduce new stock/components only with clear product consumers and audited conversion units. The die choice covers the stated steel-forming families, not universal manual or press access to all advanced materials.

## Selected optional steel casting

Add an optional casting capability within the steel expansion, using suitable heat capability and reusable molds for selected products such as gears and housings. Ordinary steel production, hand/press plates and existing plate-based gear crafting stay available. Casting produces the same material/component identities where an equivalent already exists; it does not create an inferior or premium casting-only steel currency.

| Mold / product family | Proposed output and useful consumer | Accounting and reachability |
| --- | --- | --- |
| Steel gear mold | Existing steel gear for workshop construction/upgrades | Draft 4 steel ingots -> 1 gear matches 4 plates -> 1 gear: 36 nugget units in/out. Heat/work costs remain additional and unbalanced |
| Housing mold | Suitable machinery housing or casing component where it has an actual construction consumer | Product identity, ingredients and units remain proposals. Reuse an existing suitable output where available; do not require a new housing for every old machine |

Molds remain installed or recoverable/reusable after a batch; no per-cast mold loss or routine replacement is introduced. Their first construction uses earlier reachable supplies and the shared bench/kiln, without requiring the finished gear/housing they produce. Accessible upgraded clay-based materials provide first steel-casting molds and lining; exact mixes and firing recipes remain to design. Specialist mineral refractories improve later equipment. Ordinary early bronze/brass molds and basic crucible heat do not automatically support steel.

The gear quantity is an assistant recipe draft derived from the existing component units, not a new implementation or an owner-approved operating-time/fuel number. Casting can reward batch handling and an alternate product route while preserving metal parity. Do not assume casting is faster or more energy-efficient than pressing before its full heat/work costs are defined and measured.

### Selected casting attachment and fuel-fired heat

Attach a reusable-mold casting basin to the larger foundry with a suitable casting heat upgrade. The foundry supplies the process heat; the basin shapes the selected product. Load metal items; melting and metal transfer happen internally within the attached setup. The initial arrangement requires no exposed molten-metal storage or transport network. Ordinary foundry steel production remains useful without this optional attachment. Exact connection faces, footprint, inventory layout and atomic handoff behavior remain implementation details.

Fuel-fired heat and hand-operated bellows make this casting capability reachable before electricity. The bellows later accept shaft drive, following the earlier manual-to-shaft approach. Both arrangements use the same molds, recipes and metal yields; automation reduces active work. Bellows do not replace fuel, and casting heat costs are additional to the earlier production of steel stock. This is a new casting capability, not a claim that the current coke-based foundry already has a separate heating-fuel slot.

Construct the heat upgrade, basin, bellows and first molds from earlier obtainable materials. Their heat-resistant material route must be reachable without requiring a cast output they enable. Accessible upgraded clay-based materials are selected for first casting, with specialist mineral refractories reserved for later improvements. Initial supplies cannot depend solely on an electric chemical refinery. No rare refractory is newly required for ordinary steel ingots, hand plates or first steel casting.

Use clear heat levels and readable recipe requirements. Once the required heat and materials are supplied, casts finish automatically; players handle loading and manual bellows until shaft automation is installed. No player pouring action is required per batch. Exact fuel amounts, work rates, warm-up/cooling behavior and heat-upgrade costs remain to balance. Later electrical convenience can extend the same workshop; a separate electric-only casting prerequisite is not the selected entry.

Coke-oven chemical byproducts can feed the [Chemistry branch](../branches/CHEMISTRY.md) later. Optional recovery needs useful consumers and explicit storage/full-output behavior, without making an entire chemical plant mandatory for first steel. Apply the separately selected [recovery/pollution planning direction](https://github.com/jimbozoomer-byte/jugcraft/pull/210) when defining industrial emissions; this brief adds no new appearance, crop-damage or maintenance effects.

## Multiplayer, persistence, dependencies and assets

This contribution changes documentation only: no runtime registrations, recipes, feature flags, saves, assets or platform pins. Rollback is a documentation revert. Follow Minecraft 26.3 + Fabric and the existing [platform pins](../PLATFORM.md); no additional dependency or copied asset is proposed. Original planning prose follows the repository's MIT license.

Later implementation must keep server-authoritative, atomic inventory/recipe handling, shared ownership/access rules and bounded transport work. Test simultaneous players and automation, full outputs, interruption, unload/reload and restart without item loss or duplicate steel. Preserve existing coke, steel and machine IDs and saved inventories; adding charcoal must not break existing coal/coke production or disable registered content in old saves.

Follow [art direction](../ART_DIRECTION.md): larger industrial machines should communicate scale and function, and steel building sets should tile cleanly. This plan creates no new art. Regional sand rules remain in the refining plan; starter steel introduces no new ore, biome, season, realm, boss or event dependency.

## Verification and next decisions

Documentation checks are repository/local-link validation and whitespace checks; record actual results in the contribution PR. No new gameplay, two-client, restart, timing or performance tests were run for this brief.

Before implementing the charcoal or expansion recipes, audit shared material conversions and the full survival construction path; compare charcoal/coke small and bulk production; test output blockage and carbon consumption atomically; and verify persistence and two-client operation. The Encyclopedia should explain both carbon options, each useful product branch and optional expansion machinery without quest-gating ordinary production.

### Recorded planning batch: all ten answers A

The owner selected every A direction on 7 October 2026. These are decisions rather than pending alternatives; equipment/carbon quantities and casting heat/work costs still need a later balance pass.

| # | Decision | Selected direction |
| --- | --- | --- |
| 1 | First casting refractory | Accessible upgraded clay-based lining and molds; specialist mineral refractories improve later equipment |
| 2 | First dies and molds | Shared bench/kiln using earlier supplies; no dedicated initial toolmaking station |
| 3 | Component depth | Common plates, rods, gears and housings; specialist small parts only where useful |
| 4 | Molten-metal handling | Metal items enter; melting and transfer happen internally at the attached setup |
| 5 | Casting operation | Automatic completion when required heat and materials are supplied; loading/manual bellows remain player work |
| 6 | Heat model | Clear heat levels and readable recipe requirements |
| 7 | Larger press power | Shaft drive, with later electric-motor drive |
| 8 | Bulk coke equipment | Optional larger Coke Oven; ordinary ovens and oven banks remain valid |
| 9 | Next alloy equipment | Heat upgrades extend compatible existing alloy equipment; new process capabilities can justify separate equipment later |
| 10 | Next major planning topic | Industrial chemistry and advanced materials |

The steel expansion's main arrangement is settled for this pass. Develop exact tooling/product identities, charcoal supply costs, station construction, batch sizes and fuel/energy budgets during later recipe/balance work. Keep the selected manageable core, material parity, no routine equipment replacement and useful sideways products throughout.

The next selected topic is now recorded independently in the [industrial chemistry and fuel plan](industrial-chemistry-and-fuels-plan.md), based on the [Chemistry branch](../branches/CHEMISTRY.md) and [mineral-sands/refining plan](mineral-sands-and-refining-plan.md). The owner selected substantial chemistry at the electrical steel stage, prioritizing gas processing, aluminum and titanium, with connected fuel production and optional byproduct recovery. Preserve shared processing roles, regional feeds, existing materials and useful product branches; exact chemistry recipes/equipment remain proposals in that separate brief.

AI-assisted planning documentation: OpenAI Codex, GPT-6 family. No gameplay feature implementation is included.
