# Starter gas and acid factory construction and balance

Status: **owner-selected starter direction and provisional processing baseline**, updated 10 October 2026. The owner answered A to all ten starter-factory questions in the eighth planning batch: smaller steel/basic-circuit variants, the 20-plate Separator, automatic electrical heating, the full first sulfuric-acid chain, a permanent one-nickel/ceramic methane bed, default excess-water draining, one initial cleaning residue, 128/256 JE/t starter generator output, moderate processing times/power budgets, and retained lye with production stopping when full. The ninth batch selects an independent mineral-derived vanadium contact bed, a retained/recirculating starter acid charge, Chemical Infuser contact conversion, existing sulfur first, coal/coke and a lower-yield/lower-pollution charcoal option, optional later oxygen assistance, combined gas cleanup/separation and a stabilized road/filler use for mixed residue. The thirteenth batch selects specifically vanadium-bearing iron concentrates for earlier independent catalyst preparation. Exact source/preparation recipes, other equipment bills, material/solution conversions and efficiency/recovery limits remain to specify or review. No gameplay is implemented here.

The factory turns an existing steel workshop and electrical supply into useful hydrogen, methane and ordinary acids. Gas processing remains substantial without requiring aluminum, titanium, advanced chips, PTFE, stainless steel or completion of every specialty. Magnesium and metal expansion follows this delivery milestone. First electricity remains independently reachable.

Owner: jimbozoomer-byte. Proposal: direct industrial-planning instructions.
Related records: [selected chemistry plan](industrial-chemistry-and-fuels-plan.md), [chemical catalog](industrial-chemical-catalog-and-routes.md), [starter workshop](industrial-starter-workshop-plan.md), [steel workshop](industrial-steel-and-bulk-metallurgy-plan.md), [Chemistry branch](../branches/CHEMISTRY.md), [TODO](../TODO.md#industrial-chemistry-gas-fuels-and-advanced-materials).

## Existing construction and operating costs

Baseline: source at `f736ee3c5`, before this documentation change. [Machine definitions and construction](../../tools/machines.py), [fluid recipes and fuel values](../../tools/petro.py), and [upgrade calculations](../../src/main/java/io/github/jimbozoomer/jugcraft/machine/MachineUpgrades.java) describe current behavior, rather than the proposals below.

| Existing equipment or recipe | Current relevant cost or behavior | Consequence for the starter factory |
| --- | --- | --- |
| Electrolytic Cell | 4 steel plates, 2 Steel Tanks, aluminum cable, advanced circuit, Machine Casing | Aluminum cable cannot gate the cell intended to begin the later aluminum route; provide copper entry |
| Synthesis Converter | 4 steel plates, titanium ingot, 2 steel fluid pipes, Machine Casing, advanced circuit | Earlier methanation needs accessible starter hardware; retain the existing machine identity and saved contents |
| Chemical Reactor | 4 steel plates, glass, 2 Tinplate Tanks, Machine Casing, lead ingot | Existing steel-era foundation for acid absorption and other compatible reactions |
| Fuel Cell | Aluminum plates/cable, steel plates, advanced circuit, Tinplate Tank | Existing hydrogen consumer remains; the selected shared H2/methane generator needs an earlier reachable construction route |
| Gas Turbine | 2 Diesel Generators and an advanced circuit among its ingredients; 512 JE/t generation | Existing large petroleum machine is a later foundation, rather than assumed starter hardware |
| Water electrolysis | 1,000 mB water -> 500 mB H2 + 250 mB O2; 800 ticks at 256 JE/t = 204,800 JE | Useful baseline; fuel conversion and later efficiency must remain lossy |
| Ordinary brine electrolysis | 1,000 mB brine -> 250 mB H2 + 250 mB chlorine + 500 mB lye; 200 ticks at 256 JE/t = 51,200 JE | Current brine costs less per hydrogen unit than water; audit it separately when introducing gas generation and new efficiency effects |
| Ordinary sulfuric acid | 2 sulfur dust + 1,000 mB water -> 1,000 mB acid; 100 ticks at 96 JE/t = 9,600 JE | Preserve this reachable simplified route during transition; a new catalyst cannot invalidate existing progression |
| Machine efficiency | Up to 4 effective cards; each multiplies working draw by 0.8, with integer rounding | At 4 cards, water draw is 105 JE/t and brine draw is also 105 JE/t. Larger-machine savings and heat recovery need additional limits |

The existing Fuel Cell pays 128 JE per mB H2. Water electrolysis currently returns 64,000 JE if its entire hydrogen output is burned there. Four efficiency cards reduce the recipe calculation to 84,000 JE, still above that return. Brine's corresponding numbers are 32,000 JE returned and 21,000 JE processing. Brine consumes salt, so this comparison alone does not establish a closed free-energy loop; it establishes why a water-only check is insufficient.

## Proposed starter equipment and construction

The owner selected smaller steel/basic-circuit starter variants within the shared machine families, followed by larger advanced equipment. These are working roles, not final new registry IDs. Preserve existing saved IDs and recipe compatibility when adding the starter variants. The Separator's 20-plate total and permanent methane bed of one nickel ingot plus ordinary fired ceramic pieces are selected construction baselines; the other outer component bills below remain proposals.

The table lists **outer construction ingredients**, not a flattened raw-material bill or a literal crafting-grid layout. Reuse existing component identities and workshop methods. A Steel Tank already contains 8 steel plates and a Tinplate Tank; that Tinplate Tank contains 8 tin plates and glass. A Machine Casing uses existing bronze/zinc supplies. Components must be expanded once when calculating the full cost.

| Starter role | Proposed construction ingredients | Entry capability and later usefulness |
| --- | --- | --- |
| Electrolytic Separator | 4 steel plates, 2 Steel Tanks, 1 copper cable, 1 basic circuit, 1 Machine Casing | Water and ordinary-brine electrolysis; later metals use appropriate process capabilities rather than a self-output construction gate |
| Chemical Infuser | 4 steel plates, 2 steel fluid pipes, 1 Tinplate Tank, 1 copper cable, 1 basic circuit | Two gas inputs; HCl synthesis and methane reactions. Methane needs an independently made nickel bed and a water output alongside the gas |
| Chemical Oxidizer | 4 steel plates, 1 furnace, 1 electric motor, 1 glass, 1 basic circuit, 1 Machine Casing | Sulfur dust and metered oxygen produce SO2; other compatible solid/gas recipes can follow |
| Absorption and ordinary reactions | Initially reuse the existing Chemical Reactor and its construction | HCl absorption, ordinary sulfuric-acid supply and other compatible wet reactions. Its future capacity/interface changes must be explicit |
| Coal Gasifier | 4 steel plates, 2 bricks, 1 furnace, 1 steel fluid pipe, 1 Machine Casing | Coal/steam preparation with paid heat, dirty CO/H2 mixture and substantial numeric pollution |
| Gas Cleanup and Separation | 4 steel plates, 1 Steel Tank, 2 Tinplate Tanks, 1 electric motor, 1 basic circuit | Cleans and divides the gasifier mixture into CO and H2, with accounted residues; neither gas is created again by separation |
| Shared Gas Generator | 4 steel plates, 1 electric motor, 1 steel gear, 1 Tinplate Tank, 1 basic circuit, 1 Machine Casing | Burns H2 or pure methane; fits the earlier electrical network. Biogas and ethanol remain in the separate lower-output Bio-Generator. Implemented for hydrogen with this bill as the [Gas Burning Generator](industrial-gas-burning-generator.md); methane follows its Infuser |
| Ordinary Rotary Condensator | 4 steel plates, 1 Tinplate Tank, 1 electric motor, 1 copper cable, 1 basic circuit, 1 Machine Casing | Ordinary phase conversion when a consumer needs it; optional for the first hydrogen installation. Rocket cryogenics remains later |
| Reusable nickel catalyst bed | 1 existing nickel ingot + 2 ordinary fired bricks/compatible clay ceramic pieces | One permanent methanation bed per Infuser; no routine consumption or replacement. Existing nickel refining/trade must remain available |

The Separator costs **20 steel plates in total** after expanding its two Steel Tanks, plus tin, glass, circuit, copper cable and casing materials. The Infuser has 4 direct steel plates plus those already contained in its 2 pipes. This makes the first machines an investment without introducing a new precursor ladder. Exact casting/plate conversions and component costs stay shared with the workshop plans.

Begin with the Separator, portable tanks and existing power. Add HCl or methane equipment when those outputs are useful; sulfuric-acid production can develop alongside them. A complete coal-to-methane and acid installation is an expansion, rather than a compulsory purchase before the first chemical product. Keep ordinary pipes, hand transfers and normal repeat processing available without an automation chip.

The owner selected automatic heating included in the starter machines' electrical recipe budgets, with readable warming/working states. No separate external heat connection is required for these starter variants. Gasification still consumes its explicit carbon feed; any optional oxygen-assisted heating must debit that feed and fit the paid energy/material budget. Gas pressure remains automatic.

## Proposed game quantities and reaction units

All quantities below are fictional **game recipe quantities**, not laboratory measurements, operating conditions or real gas volumes. JE is Jugcraft energy. Tick durations describe processing time at 20 game ticks per second, not measured survival progression.

For these new gas recipes, equal gas mB represent equal reaction amounts. The existing water recipe supplies the water conversion anchor: **2 liquid mB water represent one reaction amount of H2O**, so 1,000 liquid mB water produces 500 gas mB H2 and 250 gas mB O2. Water produced by synthesis uses the same mapping. Do not infer any other liquid or cryogenic ratio from it. Every other fluid and solution needs its own material/phase entry before implementation.

| Proposed operation | Inputs -> outputs | Base processing proposal |
| --- | --- | --- |
| Water electrolysis | 1,000 mB water -> 500 mB H2 + 250 mB O2 | Retain 800 ticks x 256 JE/t = 204,800 JE |
| Ordinary brine electrolysis | Retain 1,000 mB brine -> 250 mB H2 + 250 mB chlorine + 500 mB lye | Increase to 400 ticks x 256 JE/t = 102,400 JE. The owner selected this change to the existing 200-tick Electrolytic Cell recipe on 11 October 2026; it is implemented with the [Electrolytic Separator](industrial-electrolytic-separator.md) |
| Hydrogen chloride synthesis | 250 gas mB H2 + 250 gas mB chlorine -> 500 gas mB HCl | 100 ticks x 96 JE/t = 9,600 JE |
| Aqueous HCl preparation | 500 gas mB HCl + 1,000 liquid mB water -> 1,000 mB ordinary aqueous HCl | 100 ticks x 96 JE/t = 9,600 JE. Solution volume is an abstraction with retained solute/water accounting, not a free condensator swap |
| CO2 methanation | 100 gas mB CO2 + 400 gas mB H2 -> 100 gas mB CH4 + 400 liquid mB water | 200 ticks x 128 JE/t = 25,600 JE; reusable nickel bed, paid heat |
| CO methanation | 100 gas mB CO + 300 gas mB H2 -> 100 gas mB CH4 + 200 liquid mB water | 200 ticks x 128 JE/t = 25,600 JE; same reusable bed, distinct input recipe |
| Sulfur oxidation | 1 sulfur dust + 250 gas mB O2 -> 250 gas mB SO2 | Assign 250 sulfur reaction amounts per dust for this draft; 100 ticks x 96 JE/t = 9,600 JE |
| Sulfur oxide conversion | 250 gas mB SO2 + 125 gas mB O2 -> 250 gas mB SO3 | 100 ticks x 128 JE/t = 12,800 JE; independent contact-catalyst supply must be specified |
| Ordinary sulfuric-acid absorption | 250 gas mB SO3 + 500 liquid mB water -> 500 mB ordinary sulfuric acid | 100 ticks x 96 JE/t = 9,600 JE; acid solution/material accounting required |

The sulfur route preserves the current nominal output of 1,000 mB ordinary acid per 2 sulfur dust while adding accounted oxygen and three meaningful process roles. Its proposed processing budget is 64,000 JE per 1,000 mB acid, before producing oxygen or conditioning feeds. The owner selected this full SO2/SO3/absorption chain as the normal first industrial-acid route once its initial catalyst and absorption supplies are independently reachable. The moderate processing times and power budgets are a provisional playtesting baseline; the simplified existing recipe remains available during transition and any necessary bootstrap. Acid concentration and electronic purity remain separate later supply capabilities; they are not granted by these ordinary-acid quantities.

Real contact processing uses catalytic SO2 oxidation and strong-acid absorption. The game's absorption entry simplifies that system; it is not a real preparation procedure. A named vanadium-oxide contact bed would require an independently reachable precursor route, rather than vanadium recovered only using the acid it is meant to produce. The methane nickel bed is not automatically a sulfuric-acid contact catalyst. Preserve the current starter acid route until the new route's catalyst and initial absorption supply are reachable. [EPA process reference](https://www.epa.gov/sites/production/files/2020-09/documents/8.10_sulfuric_acid.pdf).

The Gasifier's exact coal/steam/oxygen/heat quantities remain pending a shared carbon and fuel ledger. Cleanup cannot award a full coal fuel value and then create another full fuel value from the same feed. Brine preparation currently uses 2 salt plus 1,000 mB water per 1,000 mB brine; preserve that input receipt and account for sodium/chlorine in lye and gases. Coastal magnesium brine is a different later source, not a reinterpretation of this ordinary salt recipe.

Rotary conversion must preserve chemical amount and pay a direction-specific energy cost. Dissolving a gas into water, condensing a substance, and cryogenic liquefaction are different operations. No conversion awards electricity or duplicates retained fluid contents.

## Proposed generation and upgrade limits

Propose **128 JE/mB H2** and **448 JE/mB methane** as initial burn values. Methane then stores 3.5 times as much electricity in an equal-size gas tank. The selected starter output is 128 JE/t on H2 and 256 JE/t on methane; larger/upgraded generators can raise output without multiplying energy merely because fuel changes tanks or provenance. Existing Fuel Cell and petroleum consumers require a compatibility audit, not silent removal.

The exact burn values and numerical limits below remain balance proposals; question 8 selected generator output per tick, and question 9 selected the moderate processing baseline rather than separately approving every upgrade/recovery limit. Use two proposed independent limits for electrically produced gas:

- Electrolysis spends at least **256 JE per mB of produced H2**, including all efficiency cards and larger-machine savings. This is a recipe-specific lower limit, not a global restriction on unrelated chemistry. It corresponds to a minimum 128,000 JE for the water batch and 64,000 JE for the proposed brine batch.
- Total recoverable output, including any generation bonuses, exhaust recovery and useful exported heat, is bounded by **192 JE/mB H2** and **672 JE/mB methane**. These are combined ceilings with 50% headroom above the proposed base electrical burn values, not extra generation entitlements. Any future cross-system heat conversion must fit within the same envelope.

Thus the maximum hydrogen return is at most 75% of the minimum electrolysis cost before pumps or other processing. Efficiency upgrades still reduce energy per batch, and larger machines can be more efficient, but the hydrogen-producing recipes stop gaining savings at their lower limit. Speed increases throughput and draw, with no automatic energy saving. Show that limit in recipe/upgrade information so it does not appear broken to the player.

| Example calculation | Required electricity | Base or maximum generation | Interpretation |
| --- | --- | --- | --- |
| Current water batch, proposed base H2 burn | 204,800 JE | 64,000 JE base | 31.25% electrical return before other costs |
| Proposed water batch at the efficiency limit | At least 128,000 JE | At most 96,000 JE total recovered | Still loses at least 32,000 JE |
| Proposed brine batch at the efficiency limit | At least 64,000 JE | At most 48,000 JE total recovered | Still loses at least 16,000 JE, with salt and outputs accounted |
| 100 mB methane from CO2, using base water electrolysis | 163,840 JE for its 400 mB H2 + 25,600 JE synthesis = 189,440 JE | 44,800 JE base methane burn | Compression into a useful fuel tank, not net electrical generation from captured CO2 |
| CO2 methane at the hydrogen efficiency limit | At least 102,400 JE for H2, plus synthesis and feed preparation | At most 67,200 JE recovered from methane | Loss remains before counting synthesis costs |
| CO methane at the hydrogen efficiency limit | At least 76,800 JE for its 300 mB H2, plus synthesis and CO preparation | At most 67,200 JE recovered from methane | Loss remains even before charging for the carbon feed and processing |

Recovered methanation water does not restore all input hydrogen. The CO2 recipe's 400 mB recovered water can supply 200 mB H2 on re-electrolysis; the CO recipe's 200 mB water can supply 100 mB H2. That electrolysis is paid again. Likewise, oxygen has one material receipt and cannot be both exported and consumed in the same batch. Every alternative hydrogen source needs its own complete audit; the electrolysis floor does not turn coal or organic feedstock into an electrically created fuel.

For the selected 60/40 biogas abstraction, 1,000 gas units contain 600 methane and 400 CO2. Converting that CO2 needs 1,600 H2 and yields 400 additional methane plus 1,600 liquid mB water under this mapping. The final 1,000 methane units contain the original 600 exactly once. This is a later digester connection, not a starter requirement.

## Storage and output behavior

Use shared portable tanks and pipes, retaining exact fluid identity, amount and stored components across breaking, pickup, placement and restart. Pipe pressure is automatic. Starter buffer sizing can differ from placed-tank capacity without changing chemical units; neither requires a separate pressure tier.

Before consuming a batch, reserve space for every retained output. Ordinary processing pauses on insufficient inputs, power or output space, and reports the reason. Optional CO2 capture retains the selected default of continuing and venting excess, with an optional stop setting. That permission does not automatically extend to chlorine, acids, lye or other outputs.

The owner selected recovery of methanation water while storage has room and an explicit drain enabled by default for excess water. The player can disable that drain to retain all water and pause when storage fills. Lye has the opposite selected default: retain it and stop electrolysis when storage is full until the player chooses a disposal route. Neither decision permits automatic chlorine or acid disposal. Deliberate disposal records its material removal and any selected numeric pollution consequence; it creates neither a product nor a credit. No routine filter/catalyst replacement, pipe pressure simulation, landscape discoloration or crop-health penalty is added.

## Recorded starter factory choices eighth batch

The owner answered **A to all ten questions** on 7 October 2026. These are selected planning directions and a provisional baseline, rather than implementation or gameplay-test results.

| # | Owner answer | Selected direction |
| --- | --- | --- |
| 1 | A | Smaller steel/basic-circuit starter machines, then larger advanced versions within the same families |
| 2 | A | Separator construction totals 20 steel plates after its two Steel Tanks are counted, plus the other selected earlier materials |
| 3 | A | Automatic electrical heating, included in processing power costs; no separate starter heat supply |
| 4 | A | Full sulfur oxidation -> SO2 -> SO3 -> acid absorption as the normal first industrial-acid route once independently reachable |
| 5 | A | One existing nickel ingot plus ordinary fired ceramic pieces for a permanent reusable methane catalyst bed |
| 6 | A | Collect methanation water while storage has room; default to explicitly draining excess water when full |
| 7 | A | One mixed cleaning residue initially; useful material recovery can develop later |
| 8 | A | Starter gas-generator output of 128 JE/t on hydrogen and 256 JE/t on methane |
| 9 | A | Use the moderate processing times and power budgets above as the first provisional baseline, then tune through playtests |
| 10 | A | Retain lye and stop electrolysis when storage fills until the player provides storage/use or chooses disposal |

The exact other construction bills, fuel energy per mB, upgrade caps, gas/liquid solution representations, contact-catalyst preparation and coal quantities were not separately settled by this batch. Keep them visible as remaining proposals or design work. Normal factory operation still needs no automation chip, routine catalyst replacement or pressure settings.

## Recorded acid catalyst and coal processing choices ninth batch

The owner answered **A to all eight questions** on 7 October 2026. These choices settle the starter supply-chain direction; they are planning selections rather than implemented recipes or tested balance.

| # | Owner answer | Selected direction |
| --- | --- | --- |
| 1 | A | Named reusable vanadium-oxide/ceramic contact bed from an independent early mineral-preparation route; larger supplies connect to later vanadium industry |
| 2 | A | One small initial acid charge from the existing reachable recipe or trade, retained and recirculated while the absorber produces additional acid |
| 3 | A | Chemical Infuser family with the appropriate contact bed for SO2 conversion; players can dedicate separate Infusers to different reactions |
| 4 | A | Existing obtainable sulfur as the reliable entry feed, with refinery and gas-residue recovery as additional later sources |
| 5 | A | Coal and coke, plus a separately balanced charcoal alternative with lower gas yield and lower numeric pollution |
| 6 | A | Carbon feed, water and automatic electrical heating at entry; oxygen-assisted operation is an optional later improvement |
| 7 | A | One combined cleaning/separation station supplies usable CO/H2 initially; larger specialized equipment follows |
| 8 | A | One modest stabilized road/filler product from mixed residue through shared construction equipment, with paid binder/processing and more valuable recovery later |

### Reachable catalyst and acid startup

The current vanadium-electrolyte recipe consumes sulfuric acid to leach asphalt binder, so it cannot alone supply the first contact catalyst. The [thirteenth batch](industrial-chemistry-and-fuels-plan.md#resource-reagent-and-residue-decisions-thirteenth-batch) selects specifically vanadium-bearing iron concentrates as the mineral feed. Provide an earlier preparation route that does not consume this sulfuric-acid output or require aluminum/titanium production, advanced wafers or uranium processing. Preserve staged solo production and trade. [USGS identifies mineral and petroleum vanadium sources](https://www.usgs.gov/publications/vanadium); the selected non-sulfuric game preparation still needs its actual stages, equipment and quantities. Existing oil-residue electrolyte production remains a compatible later route.

The absorber's first carrier-acid charge comes from the existing reachable simple recipe or trade, rather than being granted by constructing the machine. Retain that charge and recirculate it; account for newly produced acid separately so the carrier is neither duplicated nor routinely consumed as upkeep. Its initial amount, working buffer and break/place behavior remain to specify. This bootstrap supports the selected full industrial SO2/SO3/absorption route without a self-output gate.

Use the shared Chemical Infuser family for contact conversion with its own appropriate reusable vanadium bed. Methane uses the separately selected nickel bed. Neither installed capability substitutes for the other; the recipe must validate the required bed and retained inputs/outputs. Catalyst switching/dedicated stations can support factory layouts without adding routine replacement or player pressure settings.

### Gas feed and useful residue

Existing sulfur supplies the first acid line. Additional refinery and gas-residue sulfur recovery can grow that line later; no new sulfide-roasting branch is required for entry. Gasification initially uses its carbon feed, water and paid automatic electrical heat. Do not require an oxygen feed before first CO/H2; optional later oxygen assistance must consume its inputs and account for any carbon burned for heat. Coal/coke and charcoal need separate material/energy/yield/pollution ledgers. The selected charcoal comparison is a game balance direction, not a universal real process claim.

The first station combines cleaning and separation, consumes the dirty mixture once and supplies its accounted CO/H2 and one mixed residue. A small stabilization recipe turns that residue plus a reachable binder into useful road/filler stock through shared construction equipment. Define the binder, any heat/energy, removed organics, final yield and numeric pollution; direct raw-residue placement is not automatically this stabilized product. Later valuable recovery consumes the same residue once. This adds useful construction output without landscape discoloration, crop-health damage, routine filters or catalyst upkeep.

### Remaining specification and following milestone

The selected starter direction is ready for recipe/capability specification. Catalyst mineral/preparation quantities, acid carrier accounting, coal yields/carbon/heat, road binder/yield, station IDs/footprints, other bills, solution units and exact energy/upgrade limits remain engineering and balance work. Numerical proposals stay distinct from owner selections. The [tenth owner batch](industrial-chemistry-and-fuels-plan.md#magnesium-aluminum-and-titanium-processing-decisions-tenth-batch), recorded 8 October 2026, now selects the following magnesium/aluminum/titanium process directions, including seawater pumping from beach or ocean biomes. Exact metal recipes remain to specify. This follow-up does not postpone starter specification or create a mandatory player quest ladder.

## Implementation and verification requirements

Implement the selected independent contact-catalyst/recirculating absorber entry and complete the coal feed ledger, per-solution composition, concrete machine forms and capability checks. Run construction reachability and integer recipe/unit/energy audits across all supported upgrade combinations, larger-machine settings, gas consumers and heat systems. These must include smallest batches and rounding; checking nominal formulas alone is insufficient.

Exercise full-output rollback, deliberate vent/drain policies, incompatible fluids, duplicate pickup/transfer attempts, joined tank ownership, restart persistence and bounded factory behavior on a dedicated server with two clients. Measure survival progression and representative factory performance. Local storage/recipe-priority automation follows the selected chip capability; remote control remains later. The separate [Encyclopedia UI plan](jugcraft-encyclopedia.md) should explain routes and blocked states through inventory/keybind access.

This contribution changes documentation only. The displayed balances are draft arithmetic, not gameplay or performance results. Target Minecraft 26.3 and existing Fabric/dependency pins remain unchanged. No registries, saves, assets or world generation change. AI attribution: OpenAI Codex, GPT-6 family.
