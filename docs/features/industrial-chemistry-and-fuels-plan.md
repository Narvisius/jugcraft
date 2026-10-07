# Industrial chemistry, gas fuels and advanced materials

Status: owner-selected planning direction, recorded and updated 7 October 2026. This independent brief records two completed ten-answer batches and the owner's detailed fuel-processing request. Steel-built electrical equipment is the main chemistry entry; gas processing, aluminum and titanium are the first development priorities. The latest batch selects shared hydrogen/methane generation, a separate lower-output biogas/bioethanol generator, automatic gas-pipe pressurization, reusable nickel catalyst entry, CO/hydrogen separation, a shorter initial ethanol route, automatic excess-CO2 venting, a substantial first digester and copper-conductor aluminum equipment. Exact station identities, recipes, capacities, upgrade benefits and fuel values remain to design and balance. This document implements no gameplay.

Proposal issue: direct owner instructions during industrial planning; no separate issue.
Owner: jimbozoomer-byte.
Target capability: substantial electrical steel-era chemistry, useful fuels and advanced materials, followed by optional integrated chemical industries.
Primary specialties: chemical engineering, fuel production, farming/waste utilization, refining and industrial logistics.
Related documents: [Chemistry branch](../branches/CHEMISTRY.md), [steel plan](industrial-steel-and-bulk-metallurgy-plan.md), [starter workshop](industrial-starter-workshop-plan.md), [agriculture plan](industrial-agriculture-plan.md), [mineral refining](mineral-sands-and-refining-plan.md), [machine roadmap](../MACHINE_ROADMAP.md), [TODO](../TODO.md#industrial-chemistry-gas-fuels-and-advanced-materials).

## Recorded owner choices

| # | Owner answer | Selected direction |
| --- | --- | --- |
| 1 | Electrical steel entry, with detailed gas/fuel request | Proper industrial chemistry becomes substantial with steel-built electrical equipment, particularly processing usable gasoline and other fuels. Water electrolysis, hydrogen burning, chlorine/hydrogen chemistry, catalytic methane, coal-derived CO, later digesters and staged bioethanol form connected production branches |
| 2 | Portable placeable tanks | Break and pick up filled gas tanks, retain their contents, place them again and connect them to shared pipes |
| 3 | B | More process-specific reagents and preparation chains, each with useful consumers; do not reduce the branch to a few universal reagents |
| 4 | Gas processing, aluminum and titanium | Detail these production chains first. Preserve the separately planned rare-earth expansion as a subsequent priority |
| 5 | A | Purity grades only where a precision product/process actually needs them |
| 6 | A | Expand polymers into useful rigid panels, flexible insulation, protective coatings and specialist heat-resistant parts |
| 7 | A | Advanced ceramics prioritize linings, insulators, cutting tools and precision components, with building variants |
| 8 | B | Refining reagents and electronics materials take priority over substantial new everyday adhesives/sealants/finishes |
| 9 | B, with a little A | Batteries primarily progress toward larger/better general storage, with a limited number of useful specialty options |
| 10 | A | Include optional Coke Oven byproduct collection and useful chemical processing in this expansion |

These choices replace the assistant's proposed broad pre-electric chemical-vessel entry. Earlier milling, clay firing, alloy production, natural-rubber work and other selected mechanical industries remain useful; they do not become a large chemical-reagent ladder. Steel and first electricity remain parallel and reachable without completing this new branch. No first generator, basic silicon or basic circuit is newly gated behind industrial gas chemistry.

## Machinery and operation decisions: second batch

The owner answered the following ten decisions on 7 October. Custom qualifications take precedence over the assistant's original A wording.

| # | Owner answer | Selected direction |
| --- | --- | --- |
| 1 | A, with separate biological generation | Hydrogen and methane share the gas-burning generator. Biogas has its own lower-output Bio-Generator |
| 2 | A | Methane gives more electricity per tank and supports higher output in upgraded gas-burning generators; exact fuel/output values remain to balance |
| 3 | A, plus bioethanol support | Cleaned raw biogas can power the Bio-Generator without methane upgrading. Bioethanol also works in that same lower-output generator |
| 4 | A | Make the first reusable nickel catalyst bed from existing nickel and suitable ceramic supplies; no initial chemical catalyst-preparation chain |
| 5 | Custom automatic pipe pressurization | Gas pipes handle pressurization automatically. Players do not manage pressure, compressor modules, separate compressor machines or pressure settings |
| 6 | A | Coal gasification produces a CO/hydrogen mixture, followed by cleanup/separation into useful gases |
| 7 | A | Milling/mashing, fermentation and distillation supply the earlier modest Bio-Generator; dehydration belongs to later demanding fuel/blending uses |
| 8 | A | When optional CO2 capture fills, continue production and vent excess by default; provide an optional stop-instead setting |
| 9 | A | The first advanced Anaerobic Digester is a substantial bulk-processing installation, with larger versions later |
| 10 | A | Earlier copper conductors build entry aluminum-processing equipment; aluminum improves later versions |

## Design from products and reactions

Develop connected processes in the manner requested by the owner: identify the feedstock, its composition, the transformation, the heat/catalyst capability, the separation and each usable output. Gas pressure is handled automatically by gas-carrying pipes, rather than a player-managed capability. Give gases and byproducts downstream jobs rather than treating chemical names as arbitrary crafting currencies.

The initial destinations are usable gasoline, directly usable hydrogen, upgraded methane fuel, aluminum/titanium products, polymers, ceramic components and improved electrical storage. A player can specialize in supplying gases, fuel, crops, cement, catalysts or refined materials without building every branch. Trading inputs and portable tanks is a valid alternative to making everything locally.

Use the existing fluid, energy, inventory, side-configuration and recipe systems. Shared equipment keeps distinct roles such as electrolysis, reaction/synthesis, separation, filtration, distillation and drying. More detailed reagents do not require a unique machine for every molecule. Add genuinely different equipment where operating conditions or useful handling require it.

## Reachable progression and source baseline

Start the substantial chemistry branch after obtaining ordinary steel and a working electrical supply. Early steel-era gas processing precedes the advanced organic-waste digester expansion. This is the interpretation of the requested "way earlier" coal-derived CO route; it does not move the major chemistry branch back before electricity.

Build entry vessels, conductors, pumps and controls from already reachable steel, earlier metals/ceramics and existing electrical parts. A reactor cannot require its own fuel/polymer output. First aluminum-processing equipment uses earlier copper conductors, with aluminum improvements later; its own aluminum output is not its only construction route. Titanium and any magnesium reduction route need independently obtainable inputs. The initial reusable nickel catalyst bed uses existing nickel and ceramics, obtainable before its first dependent reaction.

At main snapshot `a6303941`, the following are source observations, not new test results:

| Existing system | Observed behavior | Consequence for this plan |
| --- | --- | --- |
| [Electrolysis and fluid fuel data](../../tools/petro.py) | Water recipe: 1,000 mB water -> 500 mB hydrogen + 250 mB oxygen, 800 ticks; cell baseline 256 JE/t. Brine already supplies hydrogen/chlorine/lye | Water splitting already exists; extend its uses rather than register duplicate gases/cells. Its existing quantities are game units, not literal water/gas density conversions |
| Hydrogen Fuel Cell | Hydrogen pays 128 JE/mB; the above water batch costs 204,800 JE and its hydrogen returns 64,000 JE at that baseline | Preserve this storage/conversion loss; direct hydrogen combustion is an additional planned use, not an existing fuel-table claim |
| Gas Turbine / Advanced Engine | Gas Turbine accepts gasoline, refinery gas, bioethanol, premium gasoline and kerosene; hydrogen is currently listed for the Fuel Cell only | Review shared generator fuel support before adding methane/hydrogen combustion. A Gas Turbine name alone does not prove every gas is already accepted |
| [Oil line](petrochemistry.md) and [refinery upgrades](refinery-upgrades.md) | Distillation, cracking/reforming and gasoline/diesel uses already exist | Preserve this reachable oil branch and usable fuel identities while connecting gas and hydrogen chemistry |
| [Current bioethanol](chemical-ore-and-bioethanol.md) | Eight fermentable crops + 1,000 mB water -> 250 mB bioethanol in the Chemical Reactor. Gas Turbine baseline 192 JE/mB; Advanced Engine 256 KE/mB | Expanded milling/mashing/fermentation/distillation/dehydration and a modest bio-generator role are new planning. Retain current IDs/routes until replacement reachability is reviewed |
| [Construction chemistry](construction-chemistry.md) | Cement mix smelts into cement; calcite or bone block is an input alternative | Bulk industrial carbonate processing will gain a CO2 output; current cement recipes do not already emit collectible CO2 |
| [Portable fluid state](../../src/main/java/io/github/jimbozoomer/jugcraft/fluid/StoredFluid.java) and [tank block entity](../../src/main/java/io/github/jimbozoomer/jugcraft/fluid/FluidTankBlockEntity.java) | Existing tanks serialize their exact fluid and amount into an item component and restore them on placement | Reuse and verify this behavior for supported gas tank variants; a second incompatible portable-storage system is unnecessary |
| [Gas Cylinder](gas-storage.md) | Existing carryable gas item uses the same stored-fluid component | Keep useful cylinder support; the owner's selected placeable tank experience is not replaced by a cylinder-only solution |
| [Chemistry materials](../../tools/petro.py) and [metals](../../tools/materials.py) | Nickel and existing catalysts, aluminum, titanium, PVC, rubber and bioethanol already have identities | Reuse existing content. Methane/CO/CO2/HCl and the new process roles described here need a registration/consumer audit; do not claim they are implemented by this document |

The existing Synthesis Converter construction uses titanium. If it receives early methanation recipes, confirm its access at the chosen stage or design a compatible earlier capability without its own unavailable outputs. Do not silently assume every existing station is already a starter station.

## Chemistry conventions and corrected reaction ratios

The owner's requested gameplay is retained with balanced chemical equations. Coefficients below count molecules/moles; gases can use comparable reference-volume units. Liquid-water mB and gas mB are not interchangeable physical amounts. Define the game-unit mapping explicitly before choosing recipe numbers.

| Process | Balanced relationship | Gameplay interpretation |
| --- | --- | --- |
| Water electrolysis | 2 H2O -> 2 H2 + O2 | Electrical splitting supplies hydrogen and oxygen in a 2:1 gas ratio |
| Hydrogen chloride synthesis | H2 + Cl2 -> 2 HCl | Hydrogen and chlorine supply a process-specific reagent; dry HCl gas and hydrochloric-acid solution are distinct forms |
| CO2 methanation | CO2 + 4 H2 -> CH4 + 2 H2O | Catalytic synthesis requires four hydrogen molecules per CO2, rather than two |
| CO methanation | CO + 3 H2 -> CH4 + H2O | A separate recipe uses coal-derived CO and three hydrogen molecules |
| Hydrogen combustion | 2 H2 + O2 -> 2 H2O | Hydrogen can power a compatible gas-burning generator; account for its original production energy |
| Methane combustion | CH4 + 2 O2 -> CO2 + 2 H2O | Methane is useful fuel; carbon remains in exhaust, rather than disappearing |
| Carbonate calcination | CaCO3 -> CaO + CO2 | The carbonate-heating stage of industrial cement processing yields capturable CO2 |
| Sugar fermentation | C6H12O6 -> 2 C2H5OH + 2 CO2 | Prepared crop sugars yield ethanol and a useful CO2 stream |

Electrolysis is supported by [DOE's process explanation](https://www.energy.gov/cmei/fuels/hydrogen-production-electrolysis). The CO2 reaction and downstream water condensation are supported by [NASA's Sabatier reactor overview](https://tfaws.nasa.gov/wp-content/uploads/5_ISRU-Sabatier-Reactor-for-TFAWS-2018.pdf); CO methanation is shown in [NETL's gasification reaction overview](https://netl.doe.gov/sites/default/files/netl-file/final-env.pdf). These are process references, not final game temperatures, pressures, timings or energy budgets.

Ordinary biogas is principally methane and CO2, with other trace constituents; it is not the requested CO feed. The owner's **60% methane / 40% CO2** is the selected simplified game composition, not a universal real-world composition. [EPA describes real biogas composition and sealed digestion](https://www.epa.gov/agstar/how-does-anaerobic-digestion-work).

## Gas processing and generator uses

### Water, hydrogen and oxygen

Water -> electrical Electrolytic Cell -> hydrogen and oxygen. Hydrogen has two major selected outlets: burn it directly in supported gas generators, or store/feed it to further synthesis. Oxygen remains useful for existing industrial uses and can feed appropriate gasification/processing recipes.

Hydrogen and methane share the selected **gas-burning generator family**. A separate **lower-output Bio-Generator accepts cleaned raw biogas and bioethanol**. Raw biogas is not included in the selected hydrogen/methane generator role. Upgrading biogas into purified methane moves its fuel into the gas-burning branch. Exact station IDs, construction, fuel rates and source integration remain to design; this selection does not claim that these new fuel tables are implemented.

Reuse the existing Fuel Cell as a separate useful conversion option. Preserve existing saved fuel consumers while reviewing integration with the new family roles; the current Gas Turbine/Advanced Engine's bioethanol support is not silently removed by this planning brief. Ambient combustion air may remain abstracted as in ordinary generators. A bottled-oxygen requirement or oxygen-boost feature is not selected for every generator merely because the reaction equation includes oxygen.

Hydrogen is an energy carrier. Electrolysis followed by combustion/fuel-cell generation must return less electricity than the complete production chain consumes, even with upgrades and optional heat recovery. Its value is storage, transport, distributed power and chemical feedstock, not power created from water.

### Catalytic synthetic methane

Captured CO2 + hydrogen -> heated, pressurized catalytic synthesis -> methane/water output -> condensation/separation -> usable methane tank and recovered water.

Support the separate CO + hydrogen recipe on compatible equipment. Heating and catalyst requirements remain readable process capabilities. The owner clarified that **gas pipes automatically provide pressurization**, preserving the process explanation while removing player-managed pressure. Do not add a pressure tier, adjustable pressure value, compressor module/machine or pressure-gating check to this route. Exact heat levels and station construction remain to design.

The owner selected an accessible **reusable nickel catalyst bed constructed from existing nickel and suitable ceramic supplies**. A dedicated chemical preparation chain is not required for the first bed. Ruthenium remains an optional later catalyst candidate named by the owner, requiring its own obtainable material/producer. NASA documents [nickel and ruthenium Sabatier catalysts](https://ntrs.nasa.gov/api/citations/20020038770/downloads/20020038770.pdf). Proposed upgrade benefits include faster processing or lower operating costs; changing catalyst never permits more methane than the carbon/hydrogen charge supports.

For these new beds, use reusable installed tooling or construction rather than introducing routine catalyst replacement. This does not change the existing consumed cracking-catalyst recipes without a separate review.

Synthetic methane should reward the extra production line through a useful, concentrated gas fuel and favorable operation in compatible generators. Final JE/mB, generator JE/t and overall production economy must be balanced separately. Chemically identical pure methane from different sources should use the same usable fuel identity/value; "synthetic" provenance alone is not a magical extra-energy grade. The improvement over raw biogas comes from upgrading its CO2 fraction and removing water/impurities.

The owner selected **more usable electricity per stored tank of methane and higher output in upgraded gas-burning generators**. Compare methane/hydrogen in the gas-burning family and raw biogas in its separate lower-output Bio-Generator. This does not imply a positive electricity return from an electrolysis-fed methane loop; the additional feedstock, equipment and complete processing energy remain costs.

### Earlier coal-derived CO

Coal -> early steel-era heated gasifier with limited oxidant/appropriate steam -> **CO/hydrogen gas mixture** -> cleanup/separation -> usable CO and hydrogen tanks, plus explicitly accounted residues. Pipe pressurization supplies the pressure abstraction automatically.

Gasification explains the CO/hydrogen output. [NETL describes coal gasification into hydrogen/CO syngas](https://netl.doe.gov/node/14592). Mixed output followed by separation is owner-selected; exact game feed, oxidant source, mixture proportions and residue quantities remain recipe proposals. Do not require bottled oxygen from the gasifier's own dependent equipment to start it; use already reachable oxygen or a designed air/steam capability.

This gives a coal-fed route before advanced anaerobic digestion and emits substantial numeric pollution as requested. Capture/cleanup improves material recovery but does not automatically erase all emissions. CO feeds its own methanation recipe; CO and CO2 stay separate identities and recipes. Hydrogen is a useful separated output. Candidate ash/tar outputs need actual consumers and an audited allocation of coal's material/energy budget; do not award both an undiminished mixed-gas charge and the same separated gases.

### Advanced anaerobic digesters

Organic waste, including surplus food and suitable farm residues -> advanced sealed Anaerobic Digester -> **60/40 methane/CO2 biogas**, plus useful digestate where a recipe/consumer is designed. The first advanced digester is a **substantial bulk-processing installation**, with larger versions later; exact footprint, capacity and construction remain to design.

After basic cleanup, raw mixed biogas powers the selected lower-output **Bio-Generator**, which also accepts **bioethanol**. Methane upgrading is optional; purified methane then feeds the hydrogen/methane gas-burning generator. Keep the fixed game mixture explicit; do not create an unbounded simulation of arbitrary gas mixtures. Crop/waste input equivalence and digestion rate need comparison with fermentation and other farming outputs. The owner's goal is a more efficient advanced conversion, to be proven by the full recipe/energy budget.

For upgrading, retain existing methane while reacting the CO2 portion with added hydrogen, then remove the produced water. An illustrative **molar bookkeeping example**, not an mB recipe:

- 10 units of biogas contain 6 CH4 + 4 CO2.
- Add 16 H2; the 4 CO2 become 4 CH4 + 8 H2O.
- The ideal separated gas is 10 CH4, with 8 H2O units recovered separately.

This is the owner's pure usable methane goal: existing methane plus upgraded carbon, not merely throwing away 40% of the original mixture. An ideal complete-conversion game recipe can yield a pure methane output; actual chemical conversion is not universally 100%. Any chosen game losses/unreacted feed must be explicit. Digester output must not additionally award the same methane/CO2 as separate full-value outputs while retaining an undiminished mixed-biogas tank.

## Bioethanol and concentrated fermentation CO2

The selected industrial chain is:

**Milling & mashing -> fermentation -> distillation -> earlier Bio-Generator fuel.**

**Later demanding fuel/blending uses add dehydration after distillation.** The complete later industrial chain remains milling/mashing -> fermentation -> distillation -> dehydration, but dehydration is not required for the earlier modest generator.

| Stage | Input and machine role to develop | Output and useful connection |
| --- | --- | --- |
| Milling/mashing | Farmable sugar/starch crops; existing crop mill/crusher and suitable mixing/heating capability | Prepared mash/fermentable feed. Crushing alone does not turn crops directly into finished ethanol |
| Fermentation | Mash, water and the selected fermentation capability; shared fermenter/biorefinery arrangement to review | Ethanol-containing broth and a concentrated CO2 stream; optional capture connects brewing to methane synthesis |
| Distillation | Broth and process heat using suitable shared distillation equipment | Bioethanol usable in the earlier Bio-Generator, plus separated water/stillage with explicitly allocated residues |
| Later dehydration | Distilled ethanol and a suitable drying capability | Drier fuel for later demanding uses/blending where the consumer requires it; reusable drying equipment follows the no-routine-upkeep policy |
| Fuel use | Biogas or bioethanol supplied through shared tanks/pipes | The same lower-output Bio-Generator accepts both fuels; existing compatible fuel users and later gasoline blending remain useful |

The owner selected a separate **lower-output Bio-Generator for both cleaned biogas and crop-fed bioethanol**. It is distinct from the hydrogen/methane gas-burning family; exact block identity, footprint, construction and output rates remain to define. Keep a worthwhile farming power option without making it competitive with every high-output industrial fuel on every measure.

The existing direct Chemical Reactor recipe is a compatibility baseline, not the new complete chain. Preserve existing bioethanol and saved fuel consumers while developing the selected earlier milling/mashing -> fermentation -> distillation route. Dehydration belongs to later demanding fuel/blending uses. Exact recipe units and whether that later prepared fuel needs a distinct form remain to design; do not introduce purity versions without an actual consumer requirement.

Later breweries provide highly concentrated CO2; collection/drying/purification can deliver the selected usable pure CO2 output. Fermentation produces CO2, but raw exhaust purity is not automatically guaranteed. [DOE's ethanol process outline](https://www1.eere.energy.gov/bioenergy/pdfs/Archive/abcs_biofuels.html) describes fermentation and dehydration; [DOE's separation report](https://www1.eere.energy.gov/manufacturing/pdfs/co2_separation_report_v2020.pdf) shows fermentation CO2 cleanup. Industrial generation/collection quantities remain game-design work.

## Cement CO2, release and optional capture

Carbonate-bearing feed -> industrial heating/calcination and cement production -> cement material plus CO2. Attach optional collection/cleanup to send that gas into storage or methanation. Without purposeful collection, release it to the numeric pollution system and allow ordinary production to continue.

Produce process CO2 at the carbonate-conversion stage; milling finished cement does not create another identical CO2 batch. [EPA describes carbonate calcination and distinguishes finish grinding](https://www.epa.gov/sites/default/files/2015-02/documents/h_tsd_cement_epa_1-28-09.pdf). Classify the existing bone-block fallback separately rather than treating every non-carbonate alternative as physically identical. Exact industrial feed/product quantities and treatment of prior recipes remain to design.

Each produced byproduct amount is either captured or released; never pay both full gas recovery and full avoided-emission credit for the same quantity. Distinguish carbonate-process emissions from heating-fuel exhaust. The owner selected **continued production with automatic excess-CO2 venting when capture storage fills**, plus an optional **stop-instead** setting. Capture the portion that fits and release the remainder once; use authoritative atomic output/emission accounting. Exact control/UI details remain to design, but the operating default is settled.

Released gas contributes to the selected numeric pollution/map/marauder system. Optional recovery is useful rather than a mandatory refinery before cement. Follow the [existing recovery/pollution planning direction](https://github.com/jimbozoomer-byte/jugcraft/pull/210); no new visual terrain degradation, crop damage, player health penalties or routine equipment replacement is introduced.

## Chlorine, hydrogen chloride, plastics and rubber

Brine electrolysis -> chlorine and hydrogen -> suitable reactor -> hydrogen chloride. Give HCl explicit storage, reaction and aqueous-acid roles where useful; gas and dissolved acid must not interchange for free or duplicate material.

The owner's desired gas-to-polymer connection needs a carbon-bearing monomer preparation route. HCl by itself is not a plastic or rubber feedstock. A candidate PVC connection uses HCl within vinyl-chloride manufacture, followed by the existing polymerization/material-forming family. [EPA documents HCl's role in vinyl-chloride processes](https://nepis.epa.gov/Exe/ZyPURL.cgi?Dockey=P100RPT1.TXT). Select the simplified route and its carbon source before adding recipes; this brief does not commit to a particular historical catalyst or introduce its full real-world operating procedure.

Existing rubber from butadiene and natural-rubber alternatives remain useful. HCl can connect to an explicitly designed compatible specialty/chlorinated-polymer pathway, but it is not a universal mandatory ingredient for all rubber. Treat that specialty rubber connection as a candidate to develop, while preserving the owner's request for linked gas/polymer chemistry.

Develop multiple polymer product families with actual consumers: rigid equipment panels/housings, flexible cable insulation, protective coatings and heat-resistant specialty parts. Reuse existing PVC/plastic/rubber identities where appropriate. New types need a distinct capability or material role; do not force every circuit through every polymer family.

Process-specific reagents and meaningful intermediate preparation are selected. Purity variants remain limited to consumers that need them; detail comes from distinct reactions and uses rather than duplicate ordinary/high-purity versions of every item.

## Aluminum, titanium, ceramics and battery development

Gas processing, aluminum and titanium are the first chains to detail together:

- **Aluminum:** reachable bauxite/alkaline processing, filtration/recovery and alumina. Alumina branches into useful ceramics or electrical metal recovery. The owner selected **earlier copper conductors for entry equipment**, with aluminum improvements later. Reuse current IDs and keep the first cell reachable without its own aluminum output.
- **Titanium:** regional mineral feed separates into pigment/material uses or an appropriate chlorination/purification/reduction/sponge/melting chain. Independently source a reductant such as magnesium if the expanded recipe needs it; partial recovery cannot create the first reductant.
- **Advanced ceramics:** useful heat-resistant linings, electrical insulators, cutting heads and precision components, alongside appropriate building variants. Later mineral-based refractories improve equipment without removing the selected accessible clay-based first steel-casting route.
- **Polymers and electronics:** prioritize refining reagents, insulation and electronics materials before a broad new everyday adhesives/sealants industry. Existing soap, agricultural coatings and ordinary materials are not removed.
- **Batteries:** primarily a capacity/performance progression for general storage. Consider a small number of specialty cases, such as portable packs or high-output banks, where they provide a real use. Do not replace the owner's mostly linear preference with a large mandatory chemistry-specific battery web.

The [mineral refining plan](mineral-sands-and-refining-plan.md) retains its regional sands, shared roles and explicit later rare-earth expansion. Named neodymium/cerium/yttrium products remain the initial set, not the final roster. This chemistry priority choice does not cancel those additional materials or their useful applications.

## Optional Coke Oven chemical byproducts

Add collection and subsequent chemical uses within this expansion, as selected. The ordinary Coke Oven -> coke -> steel route remains available without a chemical plant.

Candidate outputs include a coal-derived gas stream and tar/condensate products with appropriate fuel or chemical consumers. Exact composition, identities, treatment stages and consumers remain proposals. Connect recovered gas to compatible fuel/synthesis roles and suitable condensates to later chemical products, while respecting the owner's lower priority for a large everyday-finish branch.

Collection must not award the full original coal energy/material separately in coke, gas and tar. Audit allocations, fuel conversion, optional heat recovery and storage/full-output behavior together. Ordinary ovens, the selected larger oven and banks of small ovens should share compatible output/recovery rules; exact attachment support remains to design.

## Proposed equipment map

These are functional machine roles for the selected processes, not approval of every new registration or final footprint.

| Role | Input -> output | Proposed implementation direction |
| --- | --- | --- |
| Electrolytic Cell | Water -> H2/O2; brine -> chlorine/H2/lye; alumina -> aluminum | Existing station/interfaces; selected copper-conductor entry and later aluminum improvements; exact recipe pending |
| Gas-burning generator family | Hydrogen or methane -> electrical power/exhaust | Selected shared fuel family; methane improves electricity per tank and upgraded output; exact source integration/rates pending |
| Lower-output Bio-Generator | Cleaned biogas or bioethanol -> electrical power/exhaust | Selected separate biological-fuel generator; methane upgrading is optional for raw-biogas power |
| Catalytic synthesis | CO2 or CO + H2, heat and reusable nickel catalyst -> methane/water | Compatible Synthesis Converter capability or suitable earlier module; pipes handle pressure automatically; no self-output construction gate |
| Condensation/gas separation | Reaction or raw gas stream -> usable gas, recovered water and explicitly defined residuals | Shared separation roles rather than a new separator for every gas |
| Coal Gasifier | Coal + appropriate oxidant/steam + heat -> CO/hydrogen mixture/residues | Selected mixture then cleanup/separation; early steel-era process with substantial pollution and automatic pipe pressure |
| Anaerobic Digester | Organic waste -> fixed 60/40 biogas and accounted digestate | Selected substantial first bulk installation, with larger versions later; exact size/rates pending |
| Crop preparation / fermentation | Crops -> mash -> broth/CO2 | Reuse mechanical mill/mixing roles; decide suitable shared fermenter/biorefinery capability |
| Distillation / later dehydration | Broth -> Bio-Generator ethanol; later drying -> demanding fuel/blend uses | Selected earlier generator route stops at distillation; shared distillation/drying architecture remains to choose |
| Cement kiln / gas capture | Carbonate-bearing feed -> cement-stage material + captured/released CO2 | Optional collection; full capture defaults to excess venting and continued production, with optional stop-instead |
| HCl / monomer / polymerization | H2 + chlorine and appropriate carbon-bearing intermediate feeds -> reagents/monomers -> polymers | Extend shared reaction and polymerization machinery with distinct recipes |
| Aluminum / titanium refining | Prepared regional feeds + relevant reagents/energy -> useful material products | Follow selected shared digestion/filtration/separation/recovery and process-specific electrical/retort roles |
| Coke byproduct collection | Coal-coking stream -> gas/condensates | Optional collection and chemical processing; coke supply stays independently useful |
| Portable gas tanks | Exact filled gas amount -> carried tank -> placed pipe-connected supply | Reuse existing storage components and network handling |

Heating and catalyst availability must be visible in recipes and machine states. Gas pipes pressurize automatically; pressure adds no player setting, tier or separate compressor purchase. Provide useful input/output/blocked-state explanations in the Encyclopedia and normal machine UI. This does not create a new research/quest completion gate or routine pump/filter/catalyst repair system.

## Storage, accounting, performance and persistence

The selected portable tank retains its gas identity and exact amount when broken, carried and placed. Connected pipes use that same saved content. Gas-filled tanks cannot stack or merge in a way that duplicates contents. Breaking one member of a joined tank group, simultaneous pipe transfer and two-player pickup need an atomic partition/drop transaction. Retain existing cylinders as an additional handling option.

**Automatic pressurization:** gas-carrying pipes and their compatible connections supply the process pressure abstraction. Players connect tanks and machines normally; they do not build/manage compressors, pressure modules, pressure meters, network pressure tiers or per-recipe pressure controls. Pipe diameter/pressure-loss simulation and pressure hazards are outside this selected scope. Reuse shared pipe transfers and fixed gas reference units; do not add per-pipe pressure simulation to factory tick work.

Standardize gas reference units and separate liquid/gas accounting. Chemical reactions need elemental/carbon accounting and explicit energy cost; equal raw fluid volume before and after a reaction is not a universal physical rule. The existing data audit's fluid allowances must be reviewed before implementation rather than bypassed with unexplained "source" bonuses. Preserve existing ore/material accounting exceptions where appropriate.

Budget electrolysis, pumps, heating, synthesis, separation, drying and generator conversion together. If automatic pressurization has an implicit energy overhead, account for it within the associated process budget rather than adding a player-managed compressor or per-pipe energy charge. Pure methane versus mixed biogas comparisons must include the extra hydrogen/energy and their distinct generator families. Fuel value per mB, sustained generator output, tank range and net production cost are separate balance measures. Optional exhaust/water/heat recovery and all efficiency upgrades must not enable a closed positive-power loop.

Operate bounded recipe batches and shared pipe transfers; no per-molecule entities or unbounded atmosphere simulation. The mixture can be a recipe-defined fluid with known composition. Captured output, optional release and power/material consumption must commit atomically and preserve pending progress on unload/restart.

Use authoritative server state, existing access rules and stable IDs. Preserve current fluids, saved tanks and fuel users during expansion. Feature disabling must not erase persisted registrations. Build new content from source IDs/tags rather than adding duplicate chemical currencies.

## Delivery and remaining decisions

1. Develop hydrogen/methane gas generation and the separate biogas/bioethanol Bio-Generator, with complete energy budgets and the selected copper-conductor aluminum entry.
2. Specify methanation heat capability, the selected existing-nickel/ceramic reusable bed, water separation and optional catalyst improvements. Keep gas-pipe pressurization automatic.
3. Detail the earlier coal gasifier and optional cement/Coke Oven gas capture without making capture a starter-steel prerequisite.
4. Detail the selected substantial first digester and earlier milling/mashing -> fermentation -> distillation crop-power route, with later dehydration and concentrated CO2 collection.
5. Add the selected polymer/ceramic/storage product depth using actual consumers; refine HCl's specialty-polymer connection.
6. Balance construction, work rates, gas units and electricity/fuel values; implement the selected excess-CO2 vent default and optional stop-instead setting. Additional planning questions should arrive in batches of **8–12 inline questions**, not two at a time.

The generator fuel split, methane benefit, reusable nickel entry, automatic pressure handling, CO/hydrogen mixture separation, earlier ethanol stages, CO2 capture default, first-digester scale and copper-conductor entry are selected. Remaining decisions include exact station IDs/footprints/recipes, reagent catalog, heat levels, catalyst quantities/upgrades, distillation/drying architecture, gas units/rates, generator fuel/output values, control UI and battery sidegrades. Owner-selected directions are distinct from assistant equipment/recipe proposals.

## Verification, dependencies and provenance

Documentation-only contribution. Run repository/local-link validation and whitespace checks and report actual results in the PR. No gameplay, compilation, generator-loop, survival-timing, two-client, unload/restart or performance evidence is supplied by this plan.

Future implementation needs recipe/unit/energy audits, survival reachability, generator closed-loop checks including upgrades/heat recovery, blocked-output/capture/vent behavior, mixture and gas-conversion accounting, portable/joined tank break/place tests, multiplayer transfer/pickup, restart persistence and representative large-factory performance.

Target Minecraft 26.3 + Fabric with existing [platform pins](../PLATFORM.md). No dependency, asset, worldgen, dimension-return, boss/pet or seasonal changes occur here. Original planning prose follows repository MIT; linked primary sources inform the chemistry and are not imported runtime content. Runtime art/sounds should use the existing owner-library workflow when implementation actually needs assets.

AI-assisted planning documentation: OpenAI Codex, GPT-6 family. No gameplay feature implementation is included.
