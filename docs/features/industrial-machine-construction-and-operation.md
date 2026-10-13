# Industrial machine construction and operation

Status: proposed construction and operating specification, 10 October 2026. Existing owner-selected starter costs remain identified below. New bills, profiles and handling rules are defaults prepared for review, not final registered crafting recipes or implemented behavior.

Owner: jimbozoomer-byte. AI-assisted planning: OpenAI Codex, GPT-6 family.
Related records: [delivery plan](industrial-factory-implementation-plan.md), [production lines and layouts](industrial-production-lines-and-factory-layouts.md), [machine art](industrial-machine-models-and-textures.md), [starter baseline](industrial-starter-gas-and-acid-factory.md), [workshop](industrial-starter-workshop-plan.md), [steel](industrial-steel-and-bulk-metallurgy-plan.md).

## Construction vocabulary

Construction tables list outer components. “Steel Tank” includes its existing plate/tin/glass cost; “gear” includes its own material. A raw-material display recursively expands these once, detects cycles and records shared stock identities. These are ingredient totals, not nine-slot grid diagrams.

A beam is a shared structural wood component, a ceramic piece is a compatible fired clay/refractory component, and a carbon plate is a proposed conductive carbon stock from paid milling/forming/firing. Their names are component roles until canonical existing identities and tags are assigned. Recipes cannot accept unrelated visually similar material merely because it has the same generic noun.

Standard construction groups give contributors a common starting point without adding a new kit-item registry:

| Group | Proposed contents | Applied purpose |
| --- | --- | --- |
| Mechanical drive | 1 iron shaft and 1 bronze or brass gear | Repeatable guarded motion; electrical motor can drive the same shaft later |
| Entry electrical controls | 1 basic circuit and 1 copper cable | Recipe control and status; no advanced chip in its own first producer |
| Wet containment | Appropriate existing tank, pipe and closed vessel parts | Named working liquid/gas with compatible lining/heat capability |
| Ordinary heated assembly | Existing furnace plus compatible fired ceramic pieces | Fuel-heated workshop or automatic electrical heat as the declared profile requires |
| Precision fixture | Reusable tool/pattern, earlier plates/rails and basic controls | First cutting, wet-processing and exposure operations |
| Advanced process construction | Defined stainless/PTFE components where a recipe genuinely requires them | Later compatible capability; not a universal upgrade or routine replacement |

A protected tool socket is not an extra consumed recipe input. The tool is paid once, saved once and either installed by construction or inserted later. If construction includes it, the placed machine receives that same accounted tool; dismantling cannot also refund it as both a component and a separate installed item.

## Retained starter construction baseline

Keep the [starter gas/acid receipt table](industrial-starter-gas-and-acid-factory.md#proposed-starter-equipment-and-construction) as the source for the first Separator, Infuser, Oxidizer, reactor, Gasifier, cleanup rack, generator, Rotary Condensator and nickel bed. This pass does not replace those settled/draft receipts.

The Separator's selected total is 20 steel plates after its two Steel Tanks are expanded: 4 direct plates plus 2×8 tank plates. Other outer ingredients remain copper, basic circuitry, tin/glass and the existing casing supply. Nickel tooling remains one nickel ingot plus two ordinary fired ceramic pieces. No aluminum/titanium cable, advanced chip or PTFE requirement is introduced.

For the contact bed, propose one earlier thermal-preparation operation on specifically vanadium-bearing concentrate, manual/compatible washing and recovery of named contact-grade stock, then firing onto ordinary ceramic support. Treat this as a short abstract game mineral recipe, not a real chemical procedure. Exact concentrate composition, losses and paid work must be declared before producing it. Use shared bench/thermal equipment reachable before ordinary sulfuric acid; later acid-assisted bulk vanadium refining is an alternative, not the first-bed producer.

Propose a **250 mB paid carrier-acid charge** for the first absorber. It comes from the existing simple acid recipe or trade and is reserved as working inventory. It is not consumed as a recurring ingredient and not included for free in construction. Normal acid output is separately accounted. This amount is a new draft default, not an earlier owner selection.

## Proposed workshop station bills

The bills below describe the entry industrial forms in the art brief. Simple hand benches, racks, querns, looms and baths keep their earlier reachable alternatives. Material substitutions require equivalent declared capability; a thin wooden bath is not automatically an advanced acid vessel.

| Form | Proposed outer construction | First supported work |
| --- | --- | --- |
| Crop Processor | 4 wood beams, 4 bronze plates, 2 iron plates, 1 iron shaft, 1 bronze gear | One installed threshing/husking/fibre head |
| Screw Press | 4 wood beams, 4 iron plates, 1 bronze gear, 1 iron shaft, 1 Tinplate Tank | Appropriate seed oil or fruit juice; output cake/pomace |
| Mill and Pulper | 4 wood beams, 4 stone blocks, 2 iron plates, 1 brass gear, 1 iron shaft | Dry grinding or wet pulping with its corresponding head |
| Powered Spinning Frame | 4 wood beams, 4 iron plates, 2 bronze gears, 1 iron shaft | Yarn from prepared compatible fibre |
| Powered Loom | 6 wood beams, 4 bronze plates, 2 iron shafts, 2 brass gears | Standard cloth/canvas; reusable weave fixture |
| Rope Making Machine | 6 wood beams, 2 iron plates, 2 bronze gears, 2 iron shafts | Multi-strand cordage with a reusable guide |
| Carding and Felting Machine | 4 wood beams, 4 iron plates, 2 bronze gears, 1 iron shaft | Fibre preparation and appropriate mats/pads |
| Heated Mixing Kettle | 1 Tinplate Tank, 4 iron plates, 1 furnace, 1 bronze gear | Suitable oil/wax/binder mixes with fuel heat and optional shaft stirring |
| Sheet Roller Line | 4 bronze plates, 4 iron plates, 2 bronze gears, 2 iron shafts, 2 wood beams | Compatible sheets/coated backing with reusable roller fixtures |
| Fibre and Pellet Forming Press | Shared mechanical press construction plus a reusable compatible forming head | Paid forming of named prepared feed, mat or briquette stock |
| Heated Panel Press | 8 steel plates, 8 wood beams, 4 bronze gears, 2 iron shafts, 1 furnace, 4 fired ceramic pieces | Larger boards/laminates with a defined adhesive receipt |
| Drying Tunnel | 8 wood beams, 4 iron plates, 4 fired ceramic pieces, 1 furnace, 1 shaft drive | Accounted drying/curing; a small rack remains valid for suitable recipes |
| Finishing Vat | 1 Tinplate Tank, 4 wood beams, 2 iron plates, 1 bronze gear | Basic washing/dyeing/coating; demanding baths use a compatible later vessel |
| Paper Sheet Former | Shared sheet-roller/forming construction plus 4 wood beams, 2 iron plates and a reusable forming screen | Pulp-to-sheet forming with water/trim outputs |

Mechanical power, fuel heat and electrical operation are distinct declared costs. A motor connection supplies shaft work but does not silently replace required heat. The first ceramic mold/die/screen can be crafted from earlier stock without already possessing the finished gear, paper or polymer it produces.

Expanded bronze/wood workshop forms use a second drive and additional frame/roller parts at shared material costs. Do not automatically impose a steel frame on every large textile sidegrade. Bulk metal press/foundry forms, by contrast, use the selected appropriate steel/ceramic construction.

## Proposed mineral and precision construction profiles

These are reusable receipt patterns, not one universal advanced casing ingredient for every machine. Size and silhouette follow the corresponding art brief; compatible additions are paid explicitly.

| Equipment group | Proposed entry receipt pattern | Critical independently reachable capability |
| --- | --- | --- |
| Screens/gravity separation | 8 steel plates, 2 steel gears, 1 motor, 1 basic circuit, suitable reusable screens/deck | Named mineral concentration; no generic sand produces all ores |
| Magnetic drum | Separation frame above plus copper windings and an earlier magnet assembly | Existing magnet route remains available; no rare-earth output required for its first separator |
| Electrostatic separator | Separation frame plus 4 fired insulators and copper conductors | Electrical separation of named prepared feed |
| Filter Press | 8 steel plates, 1 Steel Tank, 2 steel pipes, 1 motor, reusable filter assembly | Dedicated liquid/solid output with no replacement-filter upkeep |
| Dissolution/precipitation vessel | 8 steel plates, 1 Steel Tank, 2 pipes, 1 basic circuit, 4 fired lining pieces | First compatible acid/alkali operation before specialist PTFE/stainless stock |
| Calciner | 12 steel plates, 8 fired lining pieces, 1 furnace, 1 motor, 1 basic circuit | Paid thermal conversion and accounted outgoing stream |
| Reduction retort | 16 steel plates, 8 fired lining pieces, 2 Steel Tanks, 4 pipes, 1 basic circuit | Appropriate reduction, integrated cleanup and first-installation salt return |
| Molten-salt cell | 16 steel plates, 8 fired lining pieces, copper conductors, basic controls and appropriate reusable electrodes | Dry chloride/alumina feeds; no aluminum/magnesium output in first construction |
| Reagent purifier | 12 steel plates, 2 Steel Tanks, 1 motor, 2 basic circuits and reusable compatible separators | Demand-specific purification before electronic-grade outputs or advanced chips exist |
| Wafer wet station | 12 steel plates, 2 compatible Steel Tanks, copper controls, earlier precision fixture and compatible closed liners | Distinct clean/develop/etch jobs; first chemistry reached before specialist upgrades |
| First advanced lithography form | Existing lithography-family stock plus earlier precision rails, basic controls and a reusable pattern | No produced advanced chip in its own first exposure machine |
| Module assembler | Existing assembly family plus earlier rails and reusable placement fixture | One general advanced chip -> function-related module assembly |
| Later deposition form | Appropriate advanced process frame, compatible closed gas fixture and separate precursor preparation | Silane capability arrives later; substrate stock is still required |

Tool capability validates the operation. A titanium-retort cleanup path is part of its initial compatible form rather than an advanced optional attachment required to remove the first product. A high-purity wafer cutter can reuse the shared saw family's drive while having its own precision fixture/hood.

## Proposed giant plant purchases

Large installations consume more earlier components and keep their special roles visible. They are not granted four times the capacity merely because the model is big; process profile and storage descriptor provide those effects.

| Large form | Size W × D × H | Proposed outer construction or assembly |
| --- | --- | --- |
| Bulk Mechanical Metal Press | 4×6×6 | Entry press plus 16 steel plates, 2 steel gears, 2 shafts, 4 fired guide/liner pieces and a larger reusable die frame |
| Bulk Coke Oven | 4×4×6 | Existing oven construction plus two additional compatible refractory/firing assemblies and 8 iron plates |
| Bulk Steel Foundry | 5×6×6 | Existing foundry construction plus 24 steel plates, 8 fired lining pieces, 2 steel gears, 2 shaft drives and its paid casting bay |
| Industrial Fermenter | 3×3×4 | 8 steel plates, 1 Steel Tank, 1 motor, 2 glass, 1 basic circuit and 1 casing |
| Chemical Distillation Station | 4×4×6 | 16 steel plates, 2 Steel Tanks, 4 pipes, 1 basic circuit and 4 fired thermal/separation pieces |
| Anaerobic Digester | 6×6×6 | 32 steel plates, 4 Steel Tanks, 2 motors, 2 basic circuits, 2 casings, 8 fired lining pieces and 8 reinforced wood panels |
| Bio Generator | 3×4×3 | 8 steel plates, 1 motor, 1 steel gear, 1 Tinplate Tank, 1 basic circuit and 1 casing |
| Synthetic Fuel Reactor Train | 6×6×6 | 24 steel plates, 2 Steel Tanks, 8 pipes, 2 motors, 4 basic circuits, 2 casings, 4 fired lining pieces and a separately paid cobalt/ceramic bed |
| Lead Acid Battery Bank | 3×3×2 | 8 lead plates, 4 steel plates, 1 Tinplate Tank, 1 basic circuit, 2 copper cables and 4 compatible fired cell separators; electrolyte is filled separately |
| Advanced Lithium Battery Bank | 6×2×4 | Two compatible earlier lithium-bank/cell assemblies plus 8 aluminum plates, 4 copper cables and earlier control/connection parts; charge is not a construction output |
| Advanced Cryogenic Liquefier | 6×4×6 | Existing compatible oxygen-cooling assembly plus 8 aluminum plates, 8 stainless plates, 4 PTFE process parts and two appropriate advanced control modules; no produced cryogenic fluid in its own construction |
| Bulk Polymerization Reactor | 4×4×6 | Compatible earlier reactor assembly plus 16 steel plates, 2 Steel Tanks, 1 motor and 2 paid pipe/valve assemblies; first PTFE recipes still need an earlier compatible lining route |
| Flow controller | Within 6×6×6 bank | 16 steel plates, 1 casing, 2 basic circuits and 2 copper cables |
| Flow capacity module | 2×4×5 | 8 steel plates, 2 Steel Tanks and 2 pipes per module; electrolyte is filled separately |
| Flow cell-stack module | 2×3×4 | 8 steel plates, 4 copper plates, 4 compatible carbon plates, 4 fired separator pieces, 1 motor and 1 basic circuit |

The Digester totals **64 steel plates before expanding motors/casings or other components**: 32 direct plus four tanks containing 8 each. This is a real steel investment for a major sealed plant; natural fibre/food workshops and earlier ethanol power do not require it.

A reference flow bank assembles one controller, one cell-stack module and a complete left/right capacity pair. The subtotal is **72 steel plates before expanding pipes, motors and casings**: 16 + 2×(8 + 2×8) + 8. Electrolyte and electrical charge are separate from its construction bill. No filled tank, reagent or charge is created by placement.

The proposed first bank has one paired module bay within its 6×6×6 art envelope. Capacity grows by adding that paid complete pair; another complete bank grows a larger grid while each machine remains within the size limit. Later module densities and arrangement limits can be reviewed, but this pass does not authorize an unlimited controller graph or a single form extending beyond six blocks.

## Proposed tank and slot layouts

Keep no more than six process tanks in a normal shared machine profile. Every listed output is a separate logical destination, not a second fluid inserted into the same single-fluid tank. Capacities use the entry/expanded/bulk defaults in the delivery plan unless a storage module explicitly overrides them.

| Shared role | Inputs | Outputs | Protected tooling and item handling |
| --- | --- | --- | --- |
| Separator | 1 water/brine tank | 3 H2/O2-or-chlorine/lye destinations | Reusable electrode/configuration; incompatible operations retain named destinations |
| Infuser | 2 gas tanks | 1 product-gas and 1 water destination | Correct nickel/contact capability; not interchangeable beds |
| Synthetic fuel train | 2 conditioned gas tanks | 1 synthetic-crude and 1 water destination | Separate compatible cobalt/ceramic synthesis tooling |
| Oxidizer | 1 compatible gas tank | 1 named gas destination | Solid feed and residue slots where declared |
| Dissolution | 2 reagent/water tanks | 2 slurry/gas/liquid destinations | Prepared solid input and residue output |
| Ordinary reactor/absorber | 3 logical input positions, including protected carrier where used | 2 named product/recovery tanks | Retained carrier is one of the three inputs, not a seventh tank |
| Gas cleanup/separation | 1 composition-defined mixture tank | 2 gas destinations | One mixed residue slot |
| Rotary Condensator | 1 source tank | 1 destination tank | Phase capability and direction-specific work |
| Filter/precipitation | 2 applicable feed/reagent tanks | 2 recovered-liquid destinations | Filter/tool and one defined solid output |
| Purifier | 2 feed/reagent tanks | 2 product/spent destinations | Compatible purification tool; internal water-conditioning cost |
| Fermenter | 1 prepared-mash tank | 1 alcohol stream and 1 CO2 tank | Solid additives/residue where required |
| Digester | 2 wet-feed/water positions | 1 raw-biogas, 1 digestate and 1 optional recovered-water destination | Declared eligible organic inputs only |
| Generator | 1 valid fuel tank | No compulsory product-fluid tank at entry | Paid fuel consumption; recovery capability is a separate future receipt |
| Wafer wet station | 3 reagent positions | 1 accounted spent-mixture tank | Wafer inputs/products and reusable fixtures |
| Battery formulation | 2 reagent/solvent positions | 2 formulated/spent destinations | Salt/other item inputs; defined cell parts |

Tank indices are a planned logical layout, not a claim that existing MachineKind specifications already expose it. The current serializer orders inputs positionally; introducing tool slots or protected carrier behavior needs actual code and legacy mappings. Never resolve an input mismatch by deleting a stored fluid or changing its identity.

A profile that needs more than six tanks must split into useful shared stations or propose a reviewed schema/menu change. The default is separation into stages. Modular storage keeps its own bounded controller/module ledger; it does not expand every processor's menu into dozens of tanks.

## Recipe work and output blocking

Use a proposed eight-state lifecycle: unformed, idle, waiting for input, waiting for tool, waiting for work/energy, warming, processing and output blocked. Status precedence should identify the earliest actionable blocker; an absent ingredient is more useful than a generic “off” lamp.

At start, validate inputs, tooling, all outputs and permissions. Reserve a complete batch, then record paid progress. The exact escrow mechanism must be implemented using shared transactions: no duplicated input on restart, no refund of already spent energy and no result granted by an interrupted animation. Either escrow inputs durably at start or use a clearly specified final debit with a stable input reservation; do not mix both strategies.

Propose durable input escrow for new processing profiles. Items and fluids move once into a controller-owned work record after output capacity is reserved. Work payments advance progress; on interruption, the record pauses. If later output capacity cannot remain reserved because an external operation changed storage, completion waits without repeating paid work. Valid cancellation returns only untransformed escrow, with no energy refund. Completed outputs leave the escrow atomically.

External extraction may not take protected catalysts, patterns or carrier charge during an active batch. Dedicated output buffers should hold reservations locally; network capacity is not a reliable long-lived reservation. A recipe must fit its own output buffers before it can begin, even if connected pipes usually empty them.

A zero-cost idle machine polls no remote inventory. Resource changes mark it eligible for bounded reevaluation. Automated priority chooses from a capped recipe set and local stocks; no whole-world search. A paused half-complete operation cannot swap into a different recipe and reuse already paid progress.

## Tool changes moving and dismantling

Tool changes are allowed while idle or after explicit cancellation. Reject a bed change if the active operation requires it. Store tooling independently of consumed ingredient slots. Give manual work the same material yield as its compatible powered recipe.

Filled portable tanks are the owner's selected transport mechanism. Break/place preserves identity, amount and applicable grade/composition. A placed tank is nonstackable while filled and cannot be both dropped and emptied into a pipe. Empty stack behavior can remain compatible with existing items. Reject negative/overflow amounts and incompatible fluid merging server-side.

Whole machine movement is separate: ordinary dismantling collects contents and shared allowed recovered parts exactly once. A packed moving-machine item is not automatically introduced. Structure parts do not each drop a copy of the controller or its inventory. Block interactions from every side resolve one stable controller.

For flow storage, normal module detachment is allowed only when stored energy fits the remaining valid capacity; display that condition before detaching. Forced break/disruption removes the module's capacity once, preserves its fluid once and clamps excess stored energy with explicit recorded loss rather than transferring it into free charged electrolyte. Reattachment supplies capacity, never free charge. This handling is a new proposed default requiring tests and owner review.

## Receipt and compatibility data

Each implemented recipe requires a canonical ID, family/capability, profile, fixed inputs, composition/metal budget, outputs, reusable tool, work/time, numerical emissions and output-policy permissions. A source-provenance note records old behavior and any migration or quantity change. Exact proposed IDs are not registered by this document.

Solution grade is not inferred from color. Ordinary, concentrated and electronic-grade acid must have distinct compatible identities or validated metadata and ledgers. Per-fluid retained components determine concentration, dilution and recovery. Do not create arbitrary new mixture metadata the existing serializer cannot save/transfer; design a bounded compatible representation first.

Capture/drain defaults are process-specific: optional CO2 vent-on-full, methanation-water drain-on-full and lye retain-and-stop. Chlorine, acid and specialty reagent disposal is never inferred from those defaults. A manual disposal command debits material and records its declared consequence once.

During migration, map old indices by the old profile version. Add new logical buffers empty. Retain the old active recipe and its paid-progress semantics, or apply a documented reversible pause/cancel migration. No changed kind, removed registration or component refund is justified solely by the new artwork.

## Build acceptance scenarios

A completed construction packet needs a footprint diagram, actual local occupied/reserved positions, ground-level interaction point, logical port mapping, ingredient graph, source-asset provenance, model groups, motion bounds and profile data. Model screenshots alone do not verify recipe operation.

For each family, test obstructed placement, all four orientations, touching structures, one missing part, full/incompatible byproduct output, live transfer from two clients, restart/unload mid-batch, protected tool removal, breaking each kind of part and data reload. Test a paid cancelled batch and the smallest quantity that exercises rounding.

For flow plants, test missing partner, foreign-controller module, empty/unequal electrolyte sides, partial charge, detachment at the capacity boundary, forced break, reattachment and two players operating modules simultaneously. For gas recipes, test all efficiency/speed/profile combinations and every valid generator/phase recovery path.

These are implementation acceptance requirements. The present contribution supplies prose, construction arithmetic and documentation validation only.
