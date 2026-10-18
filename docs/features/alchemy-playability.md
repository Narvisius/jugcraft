# Alchemy playability — local owner test, 17 October 2026

> Publication update, 18 October 2026: the owner accepted the combined alchemy presentation and authorized publishing the alchemy additions and UI fixes. Historical local-only checkpoints below are superseded. Current scope and validation are recorded in [Alchemy interfaces and access](alchemy-interface-and-access.md).

**Current equipment access:** [the interface/access revision](alchemy-interface-and-access.md) removes the research prerequisite described below. Possessing equipment is sufficient to operate it; tablet pattern learning and essence costs remain.

Implemented with **OpenAI Codex (GPT-6)** at the owner's request. This supersedes the corresponding interface, valuation and owner-presence rules in [Connected Alchemy](connected-alchemy.md). Keep this work local until the owner has played it personally.

## Player workflow

1. Complete First Light; examine two Assay specimens and study one at the Lampwright Bench. Existing research and crafting recipes remain the starting route.
2. Open a Philosopher's Stone, Transmutation Table or Tablet. They now open the same dissolving/learned-item workspace directly.
3. Insert an item or stack. Inspect the essence preview, press **Dissolve**, then **Confirm**. The actual input is consumed and its default item identity is learned permanently. Merely inserting or previewing an item teaches nothing.
4. Choose a learned icon. Optional search, favorites and pages narrow the collection. Select **1 / 16 / 64 / Max**, then **Withdraw**. Max buys the affordable quantity that fits one output stack. The wallet is spent first, then physical carried Klein Stars. There is no surcharge. Fractional prices round down on dissolution and up on withdrawal; no rounding gain is possible.
5. Make a star, collector and condenser for a renewable workshop. Collectors choose charge export or a fuel stopping tier. Fuel upgrades pay the difference between the same input/output values used by every exchange device. Loaded collectors, condensers, relays and Matter furnaces can work offline from their own inventories and buffers.
6. Add filtered Density/Void automation, storage artifacts, and a pedestal. Set input limits and a reserve before enabling Density. Empty filters remain inert. Pedestals display effect, costs, range and last-pulse activity. Aim at a pedestal for its range footprint.
7. Select the Eye's 1×1, 3×3 or 5×5 area. Aim before using it to preview candidate positions and the maximum estimated price. Matter mining tools and mining artifacts also preview their area. The server still checks each real operation, including protections, placement and payment.

## Controls and conservation

- The power HUD separates wallet and carried stars and shows the last payment or a low-power notice. Inventory stars are visited in order; stars inside bags are not power sources.
- Gem controls stay open, show toggle/ready/cooldown state, and expose four independently bindable direct actions in Minecraft Controls. They start unbound to avoid stealing existing mod controls. The existing manual opens the controls without a key binding.
- Density has 1/4/16/64 input items per second and 0/1,024/8,192/65,536 wallet reserve presets. It makes at most one output per pulse and can use saved essence even when no new input arrives. Full output prevents input consumption. Void only credits the wallet.
- Bags/chests offer Sort, Stack and Pause. Exact component identity is required when consolidating; names and stored state are preserved. Pause disables artifacts in that storage. To configure/reactivate one, take it out, use its controls, and return it. Open menus pause carried automation to prevent competing inventory writers.
- Pedestal range selects 2/4/6 blocks. Watch remains adjacent-only; the Harvest scan remains at most 7×3×7. Effects retain their individual bounds, costs, party/hostile targeting and claim/visibility checks. Last-pulse units are targets for most effects and XP points for Mind Stone.
- Offline authority is limited to the four factory families. They save proof that the owner understood Assay, use a Fabric machine actor with the owner's UUID for current protection checks, and never touch an offline wallet or award research to that actor. Old factories acquire authorization when operated by their owner. No chunk loading is added. Pedestal combat and wallet-powered chest artifacts retain the online-owner gate.
- Condenser capacities rise to 1,048,576 / 16,777,216 essence so recipe-valued Matter/Gem gear fits. Stored block components and saves accept the larger balances; existing charge is retained. Generation and transfer rates are unchanged.
- The wallet cap rises from 1,000,000 to 2,000,000,000 essence without changing its saved attachment or losing balances. Learned identities remain saved through death and expand to a bounded 65,536 entries. Existing learned items are retained.

## What “all items” means

Every registered **default item type**, including empty storage and uncharged gear, has a positive value and can be learned. A pattern creates the item's normal default stack. It never copies an inventory, a captured creature, stored power, an owner binding or custom component state. A non-default stack is kept intact with an explicit request to empty/reset it first; there is no silent deletion of its contents.

The value source is shared by the tablet, scale, condensers, relays and automatic conversion. `tools/build_alchemy_values.py` builds `data/jugcraft/alchemy/values.json` from numeric reference facts, vanilla recipes, smithing base/addition/template costs, item-only Jugcraft recipes and local root assignments. Recipes involving fluid/byproduct/remainder accounting need explicit assignments rather than guessed free outputs. Unknown/default optional-mod types receive a provisional value of one essence so they are usable; **this is not a claim that historical EE assigned those values**. Economy tuning for these custom roots remains an owner-playtest task.

Numeric reference data was checked against ProjectE's primary [default values](https://github.com/sinkillerj/ProjectE/blob/mc1.21.1/src/datagen/generated/data/projecte/pe_custom_conversions/defaults.json) and [metal conversions](https://github.com/sinkillerj/ProjectE/blob/mc1.21.1/src/datagen/generated/data/projecte/pe_custom_conversions/metals.json). ProjectE is a later EE-style implementation, not the exact binary/data of EE1 or EE2. Source URLs and SHA-256 hashes are recorded with the extracted numerical facts in `tools/data/alchemy_value_seeds.json`. No upstream executable code or art was imported. Existing supplied owner assets remain unchanged.

The earlier restricted 45-item conversion graph remains a separate historical audit. It does not prove balance for the universal catalogue. New checks cover reference prices, every registered default type, round-trip arithmetic, container accounting and actual exchange transactions. Custom industrial output economics and server-market balance require play feedback.

## Validation

Passed locally on 17 October 2026:

- `./gradlew build --offline --console=plain -Pkotlin.compiler.execution.strategy=in-process`: all **1,923 required server tests**, including **14 new playability regressions**. Covers universal default values, exact-price transactions, fractional batches, content/charge protection, converter budgets/reserves, storage identity, offline claims, authorization reload and expensive smithing output after condenser reload.
- **75 Python tests**, including eight value-table tests covering familiar prices, appended wood tags, metal ratios, recoverable container shells, item coverage and crafting/smithing costs.
- Full `tools/check_mod_data.py`: **3,021 material IDs**, **8,522 block/item models**, **3,709 quad parts**, recipe/data checks passed. Owner-art identity audit, 18-library/platform lock check and repository/link checks passed.
- Rendered `AlchemicalGearClientGameTests` and `CompleteAlchemyClientGameTests` passed. The final complete showroom was rebuilt with `jar runClientGameTest` after the visual-only corrections; its **22 screenshots** include bulk withdrawal, search at another GUI scale, converter budget/reserve, Eye area, pedestal range sync/HUD and Gem controls. Key screenshots were inspected, and a filter-button overlap and clipped labels were corrected. Three final wording edits were checked against their canonical generator after the full data audit.

The separate local package is `Jugcraft-Alchemy-Playability-Oct17.mrpack`, with an independently playable **Jugcraft Alchemy Playability** world and a starting wallet/star supply. The only included test-world function assigns showroom block ownership to the first player; no automated player or camera control is packaged. Build logs and `validation.json` remain in the owner's workspace/output folder, outside Git.

This is one integrated-server client plus automated server tests, not an owner playtest, independent two-client multiplayer test, fresh launcher-import result or load certification. Custom industrial/byproduct balance and provisional values remain open owner-feedback items. Nothing was pushed or published.
