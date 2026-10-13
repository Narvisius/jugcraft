# Industrial agriculture: products, workshops and sidegrades

Status: owner-endorsed planning direction, recorded 6 October 2026. This document does not implement gameplay or claim that proposed machines exist. Machine names, individual product behavior, recipe quantities, processing times, energy costs and bonuses remain design proposals until their focused implementation is reviewed.

Primary specialties: farming, forestry, textiles, materials manufacturing and agricultural chemistry.
Planning stages: Homestead; bronze/brass Workshop, including first electricity; Steel Industry; Chemistry; Precision/Control; later expedition applications.
Related plans: [agriculture](../branches/AGRICULTURE.md), [chemistry](../branches/CHEMISTRY.md), [machine roadmap](../MACHINE_ROADMAP.md), [current implemented technology](../TECH_TREE.md), [content branches](../CONTENT_BRANCHES.md).

## Owner decisions and scope

The owner approved expanding agriculture into several useful industries with recognizable finished goods: a textile mill, paper mill, coatings workshop, panel factory, rubber works and later biorefinery. The initial priorities are textiles/coated fabrics, paper/packaging, then panels/linoleum; rubber and chemical applications extend those foundations.

The three explicit choices were:

1. **Construction ingredients for now.** Belts, filters, seals and insulation are installed components. Oils are ingredients for treated materials and optional durable upgrades. Do not add routine lubricant consumption, wear, breakdowns or replacement-part chores in this expansion. Revisiting upkeep requires a new design decision. Existing turbine lubricant consumption is outside this proposal and is not silently removed.
2. **Flexible basics, specialized extras.** Several suitable crops can supply common fibres or oils. Particular plants can offer better yields, distinct finishes, colours or specialty products. Do not require a different mandatory farm for every ordinary machine part.
3. **Natural rubber is an early option.** Tree tapping and reachable simple processing can supply Workshop rubber. First electricity stays reachable through ceramics and simpler materials without a rubber plantation. Synthetic rubber remains useful alongside the natural route.

Earlier progression decisions constrain this branch: a substantial range of useful pre-electric production, a short essential route into each next capability, lots of sideways products, first electricity before mod steel, and selected cross-industry connections. Ordinary small-machine recipes remain useful. Motor-driven shafts can electrify an existing workshop; larger stations are optional for throughput and may legitimately be required for a new process capability.

The stage names here reflect that planning conversation. They are not a rewrite of every older tier label in the repository.

## Existing source to extend

The inspected source already contains cotton, flax, sunflowers, direct cotton/flax-to-string recipes, wool spinning/knitting, a Cider Press and Cider Barrel, wax melting/candle work, drive belts (leather), synthetic rubber and gaskets, sawdust-to-paper, ethanol production/fuel consumers, cotton-based medical supplies and guncotton.

Useful source anchors: [agriculture data](../../tools/agriculture.py), [machine data](../../tools/machines.py), [petrochemistry data](../../tools/petro.py), [material generation](../../tools/generate_material_data.py), [rubber feature](rubber.md), [industrial chemistry feature](industrial-chemistry.md).

Those are source observations, not new playtest claims. The agricultural roadmap still lists many industrial-crop routes as planned. Check the current source before implementation, preserve existing stable IDs and consumers, and avoid creating a second cotton, paper, ethanol or rubber system.

## The shortest useful entry route

Obtain a suitable crop or animal product, process a small amount by hand, and make a useful finished item or machine component. Players can trade for those materials or produce them solo. Crop seeds and required saplings need an accessible route that does not depend on a rare biome, seasonal window, advanced machine or their own output.

Keep existing string shortcuts. Additional fibre preparation and weaving earns its cost by producing cloth, canvas and specialty components, rather than becoming a new mandatory chain for every string.

A hand oil press can prepare the small amount needed for a coating or treated part. A basic rubber route is another Workshop choice, not an entry gate for electricity. Existing ceramic insulators and simpler belt materials preserve that bootstrap.

Crop multiplication comes from growth time and appropriate cultivation. Processing redistributes material into outputs and residues; it does not create seed/fibre/oil conversion loops. Do not add crop death, spoilage or neglect penalties as an implicit part of industrial processing.

## Finished-product industries

### Textile mill and cordage works

Inputs: cotton, prepared flax, wool and future suitable fibre crops. Existing string/yarn identities should be reused where their material and colour behavior fit; distinguish a textile grade only when it has a consumer.

Suggested route: fibre preparation, spinning, then weaving or rope-making. Prepared flax can use one bundled bench process for retting/separation rather than several compulsory intermediate items. Cotton should have its own accessible preparation recipe. Wool remains a useful route for yarn, knitwear and felt.

End products:

- Ordinary cloth, linen, dense canvas and patterned fabrics.
- Rope, nets, equipment straps, sacks and selected machine belts.
- Awnings, curtains, rugs, banners, upholstered furniture and fabric wall panels.
- Reinforced weaves for transport equipment and later expedition goods.

Sidegrades: cheap ordinary cloth; dense canvas for equipment; fine cloth for filtration; different fibres for appropriate specialist products; patterns and dyes for many decorative variants. Common recipes accept several suitable sources. Specialty linen remains identifiable. Every extra quality grade must unlock a product, property or production advantage.

### Fabric finishing, felt and filtration

Inputs: finished cloth or loose suitable fibres, plus wax, drying oil, natural rubber or later resin compounds.

Suggested routes: cloth through a finishing vat and dryer; loose fibres through carding/felting and a forming press. Recipes should model appropriate materials without making every fibre interchangeable for every use.

End products:

- Coated canvas, tarpaulins, transport covers and selected weatherproof garments.
- Later balloon-envelope fabrics and transport applications; a coated fabric alone does not unlock an airship.
- Felt pads, padded equipment, insulation mats and fitted machine-mounting parts.
- Filter cloth and durable filter assemblies for selected washing/filtration equipment.

Sidegrades: weave determines the substrate; coating determines which goods it can make. A waxed textile and rubberized textile can be alternatives with different recipes and applications. An installed filter can offer an approved recovery/purity capability without gaining automatic wear or replacement costs. Insulation needs a defined consumer, such as a structure's efficiency rules, rather than an unrelated global temperature simulation.

### Oil, resin, wax and coatings workshop

Inputs: appropriate oilseeds, resin from suitable trees, beeswax/other existing wax materials and pigments.

Suggested routes: screw pressing, straining where needed, then heated mixing. General oil recipes can accept a family of suitable oils while drying-oil finishes use an appropriate source such as linseed oil.

End products:

- Paints, varnishes and wood finishes.
- Treated cloth and selected treated machine components.
- Adhesives, sealants, wax blends and sealing compounds.
- Furniture finishes, painted machinery, sign materials and finished wood variants.
- Casting patterns and suitable later foundry binders.

Sidegrades: colour, pattern, gloss/finish and recipe applicability. Basic vanilla dyes remain accessible; specialist plant and mineral pigments add choices or bulk-production advantages. Do not introduce wood decay or mandatory reapplication to make coatings useful. Distinguish decorative treatment from a treatment used by a specific machine recipe.

Tree tapping should share a defined farm mechanic across suitable trees while preserving the distinction between latex, resin and unrelated sap products. A placed stockpile of logs should not count as a productive living tree. Exact tree-validation rules and production rates need a focused design.

### Paper mill and packaging works

Inputs: suitable plant fibres, prepared wood material, water and optional starch/finish ingredients.

Suggested route: pulping, sheet forming, pressing, then drying. Keep current vanilla and sawdust paper recipes reachable. The mill earns its place through bulk manufacture, appropriate yields and additional paper products.

End products:

- Paper, card, cardboard and decorative sheets.
- Books, maps, labels, cartons, sacks and wrapping materials.
- Finished paper surfaces and later resin-impregnated sheets.
- Defined bulk-storage containers or packaging components for logistics.

Sidegrades: sheet thickness, finish, source efficiency and useful product type. Packaging needs a concrete benefit such as a lower-cost dedicated container or a recipe for shipping equipment; avoid intermediates that serve only to add clicks. Exact storage capacities and transport behavior remain undecided and must use the shared inventory/logistics systems.

### Pressed panels, timber products and insulation

Inputs: prepared fibres, wood particles, veneers and appropriate binders.

Suggested routes: fibres/particles through mixing, mat forming and pressing; veneers through cutting, adhesive application and a panel press. Basic interior boards can use a simple reachable route. Stronger structural or moisture-resistant products may require better adhesives and controlled hot pressing.

End products:

- Fibreboard, decorative interior boards and insulation panels.
- Plywood, laminated timber products, furniture, cabinets and crates.
- Veneered or painted panel variants and selected machine housings.
- Later strengthened composite panels with clearly defined uses.

Sidegrades: low-cost interior panels, lightweight insulation, decorative surfaces and stronger laminated timber. Give each a useful property or recipe role. Plant-derived panels need not compete with refractory ceramics or blast-resistant concrete on every property. Insulation benefits should attach to explicit structures or components, consistent with the ceramics plan.

### Linoleum and surface-material works

Inputs: linseed oil, suitable tree resin/rosin, wood flour, calcite or another appropriate mineral filler, backing cloth and pigments.

Simplified proposed route: prepare the oil/resin binder, blend the dry ingredients, roll onto a backing, then cure. One initial binder step and one forming/curing recipe are enough for an entry-scale route; additional controlled processing can support industrial throughput.

End products:

- Solid-colour, marbled, striped and checkerboard flooring.
- Thin floor coverings or tile variants, skirting and matching surface panels.
- Furniture tops, desks, counters and coordinated factory/clinic interiors.

Sidegrades: pattern, finish and product form, rather than a mandatory ladder of increasingly strong floor blocks. This line deliberately connects farmers, foresters, sawmills, mineral suppliers and builders through a finished surface people can use. Basic source substitutions may be allowed where sensible; flax's drying-oil specialty keeps its purpose.

### Rubber works

Inputs: latex from rubber trees or existing synthetic-rubber production, suitable curing/compounding ingredients, cloth reinforcement where relevant, and reusable molds.

Proposed Workshop natural route: tap a tree, collect latex, coagulate using an accessible coagulant such as a simple vinegar route, roll/dry into raw sheets, then cure with sulfur and heat for applicable goods. A rubber bench can bundle the small preparation operations. Existing reachable sulfur supplies should be used; neither an industrial acid plant nor an electric chemical reactor should be required for the basic optional route.

End products:

- Rubber sheets, belts, grips, boots and padding.
- Basic gaskets, suitable hoses and fitted sealing components.
- Reinforced belts, molded goods and tires for actual compatible vehicles.
- Later specialty compounds for demanding temperatures, fluids or pressure conditions.

Sidegrades: flexible, firm or reinforced products through different molds, reinforcement and compounds. Natural and synthetic rubber can both satisfy suitable basic recipes; advanced requirements should name the required compound/capability rather than declare all synthetic rubber universally superior. Keep current drive belts (leather) and existing synthetic-rubber consumers valid. Reusable molds are installed tooling rather than consumables in every finished part.

### Feed mill, farm supplies and food processing

Inputs: suitable grain, seeds, plant residues, approved press cake/pomace, animal products and cooking ingredients. Accept materials through purposeful recipe tags; do not make every residue safe feed for every animal.

Suggested equipment: mill, mixer, forming/pellet press, existing cooking equipment, dryer and optional packaging.

End products:

- Flour, starch, syrups and preserved foods.
- Packaged expedition provisions and cooking inputs.
- Prepared feed, compost blends, animal bedding and selected fuel briquettes.

Sidegrades: convenient bulk handling, different animal applications, specialized provisions and useful outlets for residues. Feed/fertilizer advantages remain optional and bounded. Avoid growth/fertilizer/seed feedback loops and a strategy that requires excessive livestock entities. Packaging, drying and pelletizing should each earn a product benefit; ordinary eating, breeding and composting remain reachable.

## Shared machines and capability progression

One useful station can participate in several lines. Attachments should alter an understandable operation rather than turn one universal machine into every industry. A screw press, sheet roller and heated panel press perform distinct operations and should have distinct visual identity.

| Station | Reachable entry form | Products and shared roles | Later expansion |
| --- | --- | --- | --- |
| Crop processor | Hand preparation bench; shaft-driven processor | Threshing, husking, cotton separation and flax preparation with appropriate heads | Larger feed handling, automated heads and improved throughput |
| Screw press | Hand press using early wood/metal parts; extend the cider-press family where practical | Seed oils, fruit juice, press cake and pomace | Shaft drive, motor drive and an optional larger press |
| Mill/pulper | Quern or small preparation vat; mechanically driven rotor | Flour, wood flour, starch preparation and pulp via distinct recipes/heads | Better material preparation and bulk wet processing |
| Spinning wheel | Extend existing hand-operated wheel | Appropriate yarn/fibre products; preserve current wool/knitting behavior | Shaft input and larger optional spinning equipment |
| Loom | Simple accessible hand loom | Cloth, canvas, patterns and reinforced weaves | Powered loom, batch handling and specialty weave capability |
| Rope-making machine | Hand fixture or small shaft-driven station | Cordage, nets and selected belt materials | More strands or reinforcement where a consumer needs them |
| Carding/felting machine | Hand preparation and forming alternatives | Felt, pads, fibre mats and some filtration products | Better mat consistency and bulk forming |
| Heated mixing kettle | Fuel-heated small kettle; shaft mixer where useful | Adhesives, coatings, wax blends, compounds and flooring mixtures | Electric heating/control and controlled compound recipes |
| Sheet rollers | Hand or shaft-driven rollers | Rubber sheets, coated fabrics and flooring surfaces | Better sheet width/control and optional larger line |
| Forming press | Hand molds or press; consider existing press tooling where it fits | Pellets, briquettes, fibre mats and molded fibre goods | Higher throughput and stronger forming capability |
| Heated panel press | Reachable pressure frame and fuel heating for appropriate basic recipes | Boards, veneers and laminates | Steel frame, controlled heating and later composite cures |
| Drying cabinet/tunnel | Rack or small fuel-heated cabinet | Paper, cloth, boards, rubber sheets and appropriate foods | Steam/heat input, motorized feed and optional industrial tunnel |
| Finishing vat | Accessible bath/vat | Washing, dyeing, coating and impregnation | Controlled bath recipes and bulk handling |
| Fermenter and still | Simple barrel fermentation and fuel-heated still | Vinegar routes, crop fermentation and alcohol separation | Larger controlled fermenter and industrial distillation |
| Digester and separator | Later sealed processing equipment with reachable construction materials | Suitable wet residues into gas and fertilizer material | Gas conditioning and different residue recovery recipes |

Use the existing kinetic, electrical, item and fluid interfaces. Motors drive existing shaft networks; adding electricity is not a forced workshop rebuild. Improved throughput alone should not make every small station obsolete. New controlled processes, temperatures, pressure or chemical resistance can legitimately need appropriate equipment.

Heating sources are capabilities, not new currencies. Define fuel/steam/electric heat behavior within the shared machine design before implementation. Do not claim an already implemented general heat API where one has not been verified.

## Later agricultural chemistry and precision uses

| Line | Proposed route | End consumers and sidegrade |
| --- | --- | --- |
| Biodiesel | Pressed suitable oil, appropriate alcohol and catalyst through a reactor; separate/refine products | Renewable fuel for compatible engines; glycerin receives defined chemical consumers |
| Ethanol and industrial alcohol | Prepare sugars/starches, ferment, then distil | Extend existing ethanol fuels, selected solvents and medical-supply recipes; crude fermentation liquid is distinct from refined alcohol |
| Biogas | Suitable wet residues through a sealed digester, followed by gas collection/conditioning and slurry separation | Fuel for explicitly compatible burners/engines; digestate supports optional fertilizer/soil products |
| Insulating laminates | Appropriate paper/cloth plus suitable resin through impregnation and hot pressing | Equipment housings, electrical supports and selected circuit substrates; not a universal replacement for ceramic insulators |
| Activated-carbon media | Charcoal through controlled activation, then crushing/grading and filter assembly | Defined air-treatment/chemical-filtration capabilities; ordinary charcoal does not automatically count as activated carbon |
| Plant-based molded materials | Prepared starch or other feedstocks through suitable processing and molding | Packaging, seed trays, containers and specialty components with defined properties |
| Medical supplies | Expand current cotton, soap and ethanol supply routes | Existing first-aid consumers and reviewed future equipment; no invented medicinal crop bonus without a separate gameplay proposal |
| Existing nitrated cotton | Improve the cotton supply for the current chemistry route | Preserve existing guncotton consumers; this plan does not expand destructive weapon behavior |

Biodiesel is a processed fuel, not merely raw vegetable oil renamed. Alcohol/catalyst inputs and glycerin recovery need a defined recipe. Neither biodiesel nor ethanol should be a universal drop-in fuel for every existing machine without an explicit compatibility decision.

Plant residues should not ferment straight into refined alcohol at early tiers merely because they share a biomass tag. Sugar/starch preparation and later cellulosic processing are distinct capabilities. Where a route extends an existing simplified recipe, document coexistence and balance instead of silently invalidating it.

Chemical feedstocks can keep farm goods in demand after petroleum processing arrives. Natural rubber, oils and fibre producers continue supplying suitable components and blends; later chemistry adds choices rather than making farming a temporary unlock chore.

The owner's [sixth industrial-chemistry batch](industrial-chemistry-and-fuels-plan.md#materials-and-useful-factory-products-sixth-batch), selected 7 October 2026, adds **named nitrogen and phosphate fertilizers with distinct crop applications and optional blends**, including ammonium-nitrate/triple-superphosphate connections. The [seventh batch](industrial-chemistry-and-fuels-plan.md#starter-resources-operation-and-delivery-seventh-batch) selects **broad crop groups and optional blends convenient for mixed farms**. Define group/tag membership, effects, blend identities and application costs alongside this agriculture plan. Preserve ordinary compost/fertilizer and independently useful farms. The selection does not introduce mandatory soil-nutrient management; benefits and seed/crop/residue feedback must remain bounded. Industrial fertilizer/refining/polymer delivery precedes new optional chemical consumer goods, while earlier foods and agricultural workshops remain useful. These are planning additions, not implemented recipes or agronomy test results.

## Sidegrade dimensions

| Choice | Possible effects to define | Boundary |
| --- | --- | --- |
| Fibre and weave | Cost, density, filtration suitability, reinforcement, appearance | Not every textile needs several quality tiers |
| Coating | A different finished product or component application | No mandatory reapplication or new weather damage |
| Panel construction | Interior cost, insulation, structural use and finish | Do not give an inexpensive panel every stone/metal property |
| Rubber compound | Flexibility, reinforcement and appropriate service conditions | Grade follows the consumer; source alone is not a universal ranking |
| Feedstock | Availability, regional advantage, yield and specialty outputs | Essential common materials need several reachable supplies |
| Pigment/finish | Colour, pattern, gloss or specialty surface | Common colours cannot depend on late mineral chemistry |
| Machine size/control | Throughput, automation or a genuinely new process capability | Earlier ordinary recipes remain valid |
| Residue allocation | Feed, compost, fuel or later chemistry | Outputs have finite yields and meaningful opportunity cost |

Useful optional upgrades consume oils or materials when crafted/installed and then remain durable under this decision. Do not smuggle periodic upkeep into an efficiency bonus.

## Example factory layouts

These are proposed operational sequences, not required station checklists. Small entry recipes can bundle steps when their only purpose would otherwise be an intermediate item.

1. **Textile mill:** prepared fibres -> spinning -> weaving; branch finished cloth into a cutting/crafting station, dye bath, coating vat or reinforced fabric recipe.
2. **Paper mill:** suitable material -> pulper -> sheet former/press -> drying; branch paper into books/labels, packaging or later resin treatment.
3. **Panel works:** fibre/wood preparation -> binder mixer -> forming or veneer stacking -> heated panel press -> finished furniture/building products.
4. **Surface works:** flax seeds -> screw press; resin plus oil -> binder kettle; wood flour/mineral filler -> mixer; finished mixture plus backing -> sheet rollers -> curing -> patterned floors and furniture surfaces.
5. **Rubber works:** tree tapping -> bench preparation/rolling -> compound mixing -> reusable molds/rollers -> curing -> belts, hoses and molded goods.
6. **Biorefinery:** crop preparation branches to oil pressing or sugar/starch preparation; reactor/distillation outputs serve fuels and chemical goods; suitable residues branch to feed, digestion or compost.

One flax operation can send stems to textiles/insulation, seeds to oil/finishes/flooring and suitable residues to feed ingredients, compost or fuel. Product choices allocate the harvest; do not award every end product as a free byproduct of one input.

## Regional farming, forestry and trade

Begin with cotton, flax and sunflowers already in source. Canola, sugar beet, indigo, madder, rubber trees and suitable resin-producing trees are candidate expansions with jobs, not a mandatory roster for every player.

Regional conditions can improve yields or offer specialty plants. Provide accessible basic seeds/saplings and documented trade or staged solo alternatives for necessary materials. Permanent products remain obtainable outside seasonal events. Farmers can sell prepared fibre, oils, latex or finished supplies; manufacturers can buy those inputs without completing unrelated personal-research gates.

Do not require new biome generation merely to make the first component. Existing-world acquisition routes need to accompany new plants, with any new worldgen limited to new chunks unless an explicit migration is designed.

## Residues, tradeoffs and balance questions

- Oil pressing can produce suitable press cake; fruit pressing can produce pomace; fibre preparation can produce stalk material; mills can produce approved residues.
- Each residue needs an intentional consumer and bounded yield. Specific feed recipes decide animal suitability.
- Pellets and briquettes offer handling or approved fuel characteristics; compressing/uncompressing cannot create energy or ingredients.
- Basic methods trade lower throughput or labour for cheap entry. Improved production can use better machines, recipes or feedstocks, with explicit costs.
- Compare processed fuels with the cultivation time, raw inputs and machine energy they require. Do not infer positive-net energy from a proposed recipe before quantities exist.
- Basic interior panels and natural finishes have useful roles even when stronger industrial adhesives arrive.
- Fertilizer, feed advantages and irrigation remain optional; no recursive growth multiplier becomes the only reasonable way to farm.
- Finished goods should have understandable visual identity. Product families can gain many decorative variants through data-driven recipes without multiplying meaningless intermediate items.

Still to settle per implementation slice: input/output quantities, crop yields, machine power/heat needs, batch times, storage capacities, exact block properties, fuel compatibility, filter capabilities, grades with actual consumers and which attachments reuse an existing station.

## Recommended delivery slices

1. **Textiles and coated fabrics:** extend existing crops/spinning, add a small loom route, useful canvas/rope goods and one coating application. Prove hand and shaft entry without a circular ingredient requirement.
2. **Paper and packaging:** retain shortcuts, add a compact mill route and actual packaging/building products. Define the storage/logistics benefit before adding containers.
3. **Panels and linoleum:** implement suitable fibre/wood preparation, reachable binder production, pressing/rolling, and a broad finished surface family. Connect calcite, sawdust, cloth, oil and resin to real consumers.
4. **Natural rubber and molded goods:** accessible tree acquisition/tapping, simple preparation and curing, shared basic rubber compatibility and a small useful set of parts.
5. **Farm supplies and food processing:** extend suitable existing mills/cooking routes, residue consumers and useful feed/provision products.
6. **Chemical and precision extensions:** dedicated fermentation/distillation options, biodiesel, optional digestion and consumer-led composite/filter materials. Keep each addition scoped rather than implementing the whole table in one PR.

The owner approved the broad direction. This sequence is a proposal for focused delivery, not a promise that all machines must be built before the first useful slice works.

## Assets and presentation

Inspect the owner's [shared asset library](https://github.com/jimbozoomer-byte/jugcraft/tree/art/owner-asset-library/art/owner-library) before creating textures, models, sounds or animations. The owner authorized direct suitable reuse, recoloring/adaptation and reference across farming, trees, biomes, machines, materials, clothing and other branches. Its publication is tracked in [PR #207](https://github.com/jimbozoomer-byte/jugcraft/pull/207); use the merged main location once available.

Preserve source originals and record selected asset paths/modifications in the feature that imports them. This planning PR imports no assets. Workshop equipment should read as functional wood/early-metal machinery with visible operations; larger industrial versions follow [art direction](../ART_DIRECTION.md).

## Implementation verification requirements

This document changes no registries, recipes, saves, worldgen, power systems or runtime assets. Its own validation is the repository documentation/link check and whitespace review.

For future gameplay slices, follow [testing guidance](../TESTING.md): audit recipe/seed/fuel loops, exercise hand/shaft/motor routes, verify bootstrap recipes, test full outputs/interrupted batches and fluid/item accounting, and perform relevant multiplayer/persistence checks. Durable installed components must not acquire hidden maintenance behavior. Existing crop/string/paper/rubber/ethanol consumers must retain a reachable route.

Use bounded loaded-area work for harvest/tapping automation, shared networks and server-owned recipes/inventories. A decorative product should not gain a per-block simulation merely because its material has an insulation/coating description. New plant acquisition, feature switches and machine inventory persistence need their own actual test evidence.

## Next planning step

Step 4 is **heavy mineral sands and physical separation**: define deposits, a reachable mechanical concentration route, later magnetic/electrical separation, useful mineral products and their downstream metallurgy/ceramics/chemistry consumers. It can connect the agriculture plan through selected cloth filters, belts, packaging or process ingredients, while keeping first mineral processing independently reachable.

Open decisions for that next step include geographic distribution, whether sands are a main route or an alternate supply, and how much separation detail to expose. Those decisions are not settled by this agriculture document.

## Real-world references

These sources inform sensible material roles and process distinctions. Game recipes remain deliberate simplifications; source quantities are not imported as balance numbers.

- [Oregon State University: oilseed flax](https://extension.oregonstate.edu/sites/extd8/files/documents/em8952.pdf): seed oil, stem fibre and retting/separation.
- [Sri Lanka National Science Foundation: latex processing](https://dl.nsf.gov.lk/bitstreams/ffe8bc00-f0f7-4888-83ea-aecf30b15aba/download): tapping, coagulation, rolling and drying.
- [NIST: natural rubber and sulfur](https://nvlpubs.nist.gov/nistpubs/jres/73A/jresv73An2p221_A1b.pdf): vulcanization as a separate material-processing step.
- [Forbo: linoleum materials](https://www.forbo.com/flooring/en-uk/commercial-products/marmoleum/c0aq3g): drying oil, rosin, wood flour, limestone and backing.
- [US Forest Service: Wood Handbook](https://research.fs.usda.gov/fpl/wood-handbook): adhesives, composite panels and timber products.
- [US Department of Energy: biodiesel production](https://afdc.energy.gov/fuels/biodiesel-production): oil/alcohol processing and glycerin coproduct.
- [US EPA: anaerobic digestion](https://www.epa.gov/anaerobic-digestion/environmental-benefits-anaerobic-digestion-ad): biogas and nutrient-bearing digestate.

AI-assisted documentation: OpenAI Codex, GPT-6 family. No AI-assisted feature code or gameplay implementation is included.
