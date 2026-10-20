# Synthetic fuels and recovery — package 6

Technology package 6, implemented locally with OpenAI Codex (GPT-6). On 16 October
2026 the owner authorized integration with main and local validation of all seven
packages. The earlier testing hold is superseded. **Keep this work local until the
owner has played it and approves publication.**

Current build, test evidence, draft reconciliation and remaining release checks:
[seven-package integration record](tech-seven-local-integration.md). The detailed
verification lists below are release checklists; only results explicitly recorded
there are claimed complete.

## Player route and equipment

Coal or renewable charcoal, water and power feed the existing Coal Gasifier.
The existing Gas Scrubber separates dirty synthesis gas into clean CO and hydrogen
and collects mixed gas-cleaning residue. The earlier stabilization recipe remains.
Use existing electrolysis for supplemental hydrogen, or trade some CO through
the new powered gas-balancing recipe. No dirty-gas bypass is added.

Prepare a reusable cobalt/ceramic bed from independently mined material, then run
the Chemical Infuser for Synthetic Crude. Refine it in the existing Distillation
Tower and use the existing Catalytic Cracker, Hydrotreater and heavy-fuel routes
for finishing. There are **no new machines, ports, tank slots or fuel grades**.
Existing formed-machine picker, previews, lock, stock targets and tool reservation
handle the new recipes; the compact refinery keeps its existing controls.

## Independent cobalt

The new mineral family `cobaltite` has stone/deepslate ores, a mineral item and
a reversible nine-item storage block. The deposit uses the shared surface-linked
ore feature: size 24, rarity 12, Y -48..32, outcrop chance denominator 4, under
windswept hills/gravelly hills/forest, stony peaks, Shield and Highland. These are
provisional gameplay choices, not a claim about real deposits. Iron-tier mining,
standard loot and conventional ore tags apply. Only newly generated terrain changes.
Ore Survey includes cobaltite; this also adds the previously omitted local chromite
family to the existing survey list. No additional scanning loop is introduced.

Condition two cobaltite with 500 mB water into two Cobalt Concentrate and 500 mB
existing Spent Mineral Wash. Arc-smelt two concentrate with one Coke into two
Cobalt Ingots. Craft both ingots over three fired vanilla bricks into one
Cobalt-Ceramic Catalyst Bed. All equipment and supplies precede synthetic fuel;
the new route does not alter nickel production or require synthetic products.

The bed stacks to one, is admitted by both current/legacy catalyst tags, is
retained without durability, and uses the existing locked-during-work tool socket.
Nickel methanation and vanadium contact conversion keep their distinct tool IDs.

## Explicit game receipts

These are abstract game recipes, not real chemical processes or operating advice.

| Equipment | Inputs | Outputs | Base processing |
|---|---|---|---|
| Mineral Conditioner | 2 cobaltite + 500 mB water | 2 cobalt concentrate + 500 mB spent mineral wash (outlet 0) | 240 ticks, 48 JE/t |
| Arc Furnace | 2 cobalt concentrate + 1 Coke | 2 cobalt ingots | 320 ticks, existing machine draw |
| Chemical Infuser, cobalt bed | 500 mB CO + 1000 mB H2 | 500 mB synthetic crude (0) + 500 mB water (1) | 400 ticks, 128 JE/t |
| Same Infuser/bed, gas balancing | 250 mB CO + 500 mB water | 250 mB H2 (0) + 250 mB CO2 (1) | 200 ticks, 128 JE/t |
| Distillation Tower | 500 mB synthetic crude | 50 mB refinery gas (0), 150 mB naphtha (1), 250 mB diesel (2), 50 mB heavy fuel oil (3) | 160 ticks, existing machine draw |
| Mineral Conditioner | 1000 mB spent wafer mixture + 250 mB lye + 1 Calcite | 1000 mB treated process water (0) + 250 mB recovery concentrate (1) | 320 ticks, 64 JE/t |
| Chemical Distillation Station | 1000 mB treated process water | 750 mB water (0) + 250 mB recovery concentrate (1) | 320 ticks, 64 JE/t |
| Mineral Conditioner | 1000 mB recovery concentrate + 1 clay ball | 4 stabilized process residue | 320 ticks, 64 JE/t |
| Crafting / vanilla Crafter | 2 stabilized residue + 2 sand + 2 gravel | 4 gray concrete powder | No powered processing |

Form profiles and upgrades determine effective energy/timing. Finished refinery
fractions total the 500 mB feed exactly. Crude itself has no generator fuel entry.
Diesel, gasoline, naphtha, refinery gas, heavy fuel oil and lubricant retain their
existing identities and consumer values. Naphtha still needs finishing, including
the existing cracker's consumable catalyst; the new reusable cobalt bed does not
replace that older catalyst. Refinery recipes require the existing crude-oil
feature alongside machines, since that feature owns the downstream equipment.

The Infuser's pre-existing **Drain excess water** option still applies only to
plain water. Disable it to retain all synthesis water; full capture then blocks
work. CO2 in its second outlet is never eligible for that drain. Clear incompatible
old products when switching recipes. There is no new vent or pollution landscape
effect. Existing upstream gasification emission counters remain active.

Gas/liquid mB are game units, not interchangeable real molar units. Gas balancing
consumes finite carbon feed and power and does not turn CO2 back into CO. No new
closed fuel-regeneration route is supplied. A full energy/economy assessment,
including electrolysis, gasifier, wash, refining, heat recovery and upgrades,
is explicitly deferred; profitability or balance is not claimed.

## Recovery boundaries

The existing spent-wafer fluid merges several reagent histories. Consequently it
does **not** return purified sulfuric acid, peroxide, photoresist, ethanol or metal:
those would be unsupported recoveries from arbitrary batches. Paid conditioning
and distillation recover only water, with consumed calcite/lye and an explicit
concentrated waste stream. The resulting ordinary water can rejoin process supply.

Each 1000 mB spent mixture plus 250 mB lye yields 750 mB usable water and 500 mB
concentrate after both stages. The concentrate is consumed once, with clay, for
fictionally stabilized building filler; it is not raw placeable waste, fuel, food
or fertilizer. Existing mineral-wash settling, gas-residue stabilization, digestate
reuse, gypsum preparation, heavy-oil lubricant/asphalt and clean polymer-offcut
recycling remain available. No extra residue silently disappears in these recipes.
Full/wrong outlets must hold back work through existing shared reservations.

## Resources and provenance

`tools/synthetic_fuels.py` owns process/crafting recipes, guide pages and new
item/fluid definitions. Mineral/worldgen tables, Java registries, survey families,
catalyst tags, shared capabilities, recipe viewers and Handbook/Encyclopedia are
connected. `tools/write_synthetic_resources.py` authors the local resource set;
the normal full generators own the same outputs. Resource generation is not a
build or test. Four new processed items, three fluids/buckets and the cobaltite
mineral family are supplied.

Directly reused without redesign: owner library
`art/owner-library/originals/Blocks/Weapon Smithing/Items/materials/cobalt_ingot.png`.
The catalog's cobalt reference sheet and ore/raw-stock entries were considered;
the ore instead extends the shared native material-map system over vanilla stone
and deepslate, with an appended palette preserving earlier ore layouts. Concentrate,
bed and stabilized-residue icons extend editable repository 16×16 maps. Existing
olive/silver machine geometry, animations and connection points remain. Originals
are preserved; no library programs were executed and no external art was downloaded.

## Deferred after package 7

- Compilation, data/icon audits and server/client suites, including optional-feature absence.
- Ore province/outcrops, mining tiers, Fortune/Silk Touch, compaction and survey display.
- Independent survival startup and sufficient cobalt availability in new terrain.
- Bed insertion/retention, wrong bed, unload during work, recipe lock and multi-lane automation.
- Infuser water-drain on/off, CO2 backpressure, input order, all outlet indices and saved batches.
- Shared refinery fraction totals and standard vehicle/generator fuel compatibility.
- Waste-input specificity, lye/calcite consumption, all blocked recovery outputs and no double recovery.
- Full upgraded energy loops, material economy, performance, save/restart and two-player behavior.
- Owner-led visual/factory playtest; no successful result is recorded yet.
