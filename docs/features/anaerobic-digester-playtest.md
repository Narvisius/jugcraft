# Anaerobic digestion — local factory playtest

Owner-selected implementation, 12 October 2026, by OpenAI Codex (GPT-6 family). Extends the existing local acid-factory/Encyclopedia branch; owner testing precedes publication or a main merge. Minecraft 26.3 / Fabric 0.19.5 and library pins are unchanged.

## Player outcome

Farmers turn surplus crops or rotten flesh into electricity and useful agricultural residue. The branch needs steel, an existing electrical supply and water, but no oil refinery, advanced metal or personal magic research. Trading can supply construction materials and fuel. It is independent of the earlier coal-fed CO route.

The complete required equipment is the **Anaerobic Digester**, **Biogas Cleaner**, **Digestate Separator** and **Bio-Generator**, plus existing cables, batteries, fluid pipes, tanks and optional conveyors. Manual buckets and gas cylinders also work. Direct farm items enter the digester's contained feed mechanism; no external milling station or consumable filter is required. The existing Chemical Infuser, reusable nickel bed and Gas Burning Generator provide optional methane upgrading. Existing bioethanol also feeds the Bio-Generator; a new fermentation/distillation chain is a separate delivery.

| Station | Width × depth × height | Function |
| --- | --- | --- |
| Anaerobic Digester | 6 × 6 × 6 | Large sealed tank, pale panels, dark bands, sheltered feed gallery, service ladder and slow mixer/auger drives |
| Biogas Cleaner | 2 × 3 × 3 | Banded wash/demister towers and a guarded pump coupling |
| Digestate Separator | 2 × 3 × 3 | Closed screw press, reinforced barrel, residue drawer and guarded drive |
| Bio-Generator | 3 × 4 × 3 | Compact farm engine, flywheel, generator coupling, radiator and folded exhaust |

Existing socket geometry joins the actual declared fluid, item and power faces. The shared MachineRotors renderer supplies eased start/stop movement; no new animation framework. Geometry follows the [industrial model brief](industrial-machine-models-and-textures.md), with the first digester's selected full six-block footprint. Existing native steel, pale ceramic, olive, gauge and pipe surfaces are reused. The unchanged owner asset `art/owner-library/originals/Blocks/bio_fuel_block_side.png` supplies the fiber-cake surface at `textures/block/bio_fiber_owner.png`; originals remain intact. New fluid swirls/bucket use the existing seeded petro texture generator.

## Batch ledger and progression

These are explicit local-playtest game quantities, not physical process instructions or claims about real gas composition.

| Process | Inputs | Products | Work |
| --- | --- | --- | --- |
| Digestion | 16 eligible farm items + 1000 mB water | 1000 mB raw biogas + 750 mB digestate | 400 ticks; 32 JE/t before the profile discount |
| Cleanup | 1000 mB raw biogas + 250 mB water | 1000 mB cleaned biogas + 250 mB wash digestate | 100 × 24 JE |
| Residue separation | 1000 mB digestate | 750 mB recovered water + 1 Digestate Fiber | 100 × 16 JE |
| Fertilizer | 8 Digestate Fiber | 1 existing Fertilizer | Crafting |
| Optional Infuser upgrade | 1000 mB cleaned biogas + 1600 mB H2; reusable nickel bed | 1000 mB methane + 1600 mB water | 600 × 128 JE |

Raw and cleaned biogas both represent the selected **60% methane / 40% CO2** game mixture. Cleanup changes usability, not composition. Upgrading preserves the original 600 mB methane and converts the 400 mB CO2; it never awards both the full mixture and its separated contents. Water recovery follows the existing Infuser's game conventions. Its saved drain/keep-water option remains applicable; all digester/cleaner/separator products must fit and cannot be silently vented.

Eligible feeds are an explicit datapack tag: wheat, carrot, potato, beetroot, rotten flesh, corn, barley, oats, cabbage, tomato and sweet potato. No fertilizer, fiber, bone meal or water-only recipe is accepted. Crops disabled by configuration remain registered, following project save policy; the vanilla feed route stays reachable.

The digester uses the existing EXPANDED profile: two independently reserved lanes and 8000 mB tanks. Each lane draws 29 JE/t at baseline after the 90% profile rounding. Other new forms use 2000 mB tanks. Shared speed/efficiency cards apply to processors; the Bio-Generator has no upgrades. No new capabilities are granted to old one-block machines, and existing IDs/recipes remain valid.

The Bio-Generator accepts **only cleaned biogas and existing bioethanol**, at 268 and 192 JE/mB respectively and up to 64 JE/t. Cleaned biogas matches ordinary biogas, so the cleaning step never loses power. Raw biogas must be cleaned. Methane and hydrogen retain their separate industrial generator. A baseline full batch yields 268,000 JE and spends 11,600 + 2,400 + 1,600 JE, leaving **252,400 JE per 16 farm items**, excluding farm automation, transport and commissioning. Net water demand is 500 mB after all wash recovery. This is a renewable farm-fuel route with consumed crop inputs, not electricity made from water.

One Bio-Generator burns a bucket of cleaned biogas over about 210 seconds, while a fully supplied two-lane digester makes two buckets every 20 seconds. The generator is deliberately the modest-output consumer, not a promise to burn the whole bulk output immediately: store or trade surplus, let full tanks pause production, or run parallel generators. About twenty-one unupgraded Bio-Generators match the baseline digester's peak gas output. A charged battery buffers simultaneous processor startup and the long Infuser batch.

The older bioethanol recipe needs 32 crops and 38,400 JE per bucket; advanced digestion improves conversion per crop while requiring a substantially bigger installation and more stations. Upgrading trades electricity/hydrogen for concentrated methane and higher generator output. The energy audit includes the consumed biogas's direct fuel value plus electrolytic hydrogen and processing, across all upgrade combinations. It does not credit biogas as free while counting its original methane as new energy.

## Saves, automation and testing

All four machines use the existing bounded form controller, input escrow, named tanks, output reservations, pause/cancel/restart behavior and sided transfer APIs. Two lanes reserve outputs independently. No world scanning, passive decay, world pollution simulation, disposable maintenance or new network protocol is added. Stop production and drain stored fluids/items before dismantling, following the shared form behavior.

The Encyclopedia includes a start-to-finish farm-power guide, all station pages and recipes, and optional personal acquisition goals. These goals add no rewards or mandatory progression gates.

Validated locally on Windows with the pinned JDK 25, 12 October 2026:

- `build runClientGameTest -PclientTests=AnaerobicClientGameTests --offline`: passed; **all 1,399 required server tests** plus the rendered client tour. Seven new server scenarios cover actual recipe outputs/paid energy, two simultaneous lanes across reload, blocked residue/cancel, accepted/rejected fuels and saved fractional credit, energy-loop bounds, unique construction recipes, and all declared ports in all four orientations.
- The client exercises working and paused models from four sides, verifies rotor motion/stopping, opens all four real server menus, and photographs the powered connection examples: **28 screenshots**. A separately saved creative world has stocked, paused stations, charged processor batteries, an empty generator receiving battery and a supply chest. It is a manual-transfer inspection yard, not an automatically replenishing plant. The normal launcher remains open for the owner to play.
- Full data validation initially found faceless inner slices in the large tank. These hidden render elements were removed without removing the structural blocks. The corrected texture/geometry audit passed: **5,565 block/item models, 3,489 quad parts and 154 assembled models**, with no new allow-list exceptions. A separate scan confirms every emitted element has faces. Fluid/form registry and recipe audits passed after final recipe/socket corrections.
- The first server run caught a duplicate generator construction pattern and a long-batch test limited to one internal charge. Recipes now differ and the regression test verifies every new bill; the long-batch fixture measures all supplied power and exact consumption. The final full server run above passed after both corrections.
- All **22 Python tests**, repository/document-link validation, the 18-library pack-lock check and whitespace checks passed. Catalog and balance tests cover all new Handbook pages, residue/water recovery, non-circular construction, allowed fuels and the 60/40 upgrade ledger. Existing station pages retain their positions; missing form pages now append automatically. Final guide-only regeneration was validated against the canonical catalog before packaging: 567 entries and 68 personal goals.

Human multiplayer, survival pacing, sustained factory performance, Modrinth import, optional-integrations-absent testing for this addition and owner visual approval are not claimed. No remote push, merge or live-server deployment occurred.
