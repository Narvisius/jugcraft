# Companion transport controls and standard machine item ports

For current player controls and setup, use the [Peepo and Jughead player guide](../PEEPO_COMPANION_GUIDE.md). This feature record retains implementation history; earlier single-container, Porter-command and single-recipe-slot descriptions have been superseded.

Owner-requested on 7 October 2026. Implemented by OpenAI Codex (GPT-6), on `peepo-companion` from `7bfec15a`. Extends the existing vanilla-style companion menu, Fabric transfers, planner and walking porter job. No new dependencies, assets, registrations, recipes or progression gates.

## Player setup

Shift-right-click an owned/party-accessible companion. Each workstation row in **Jobs** has separate **Supply** and **Output** buttons, next to its recipe slot and priority arrows. Each cycles **Auto → On → Off → Auto**, defaulting to Auto. Hover for the current status; amber Auto means external item automation was detected. Unsupported directions are disabled. The top-right button switches only between Jobs and Routine; there is no Transport page.

The owner-requested inline layout revision uses a 480-by-312 panel so the two full button labels fit beside each job without increasing screen height. Inventory and recipe slot indices, saved settings, button actions and transport behavior remain unchanged; the ghost slots move horizontally with their job controls. Implemented by OpenAI Codex (GPT-6) from `c96d5179`.

- **Auto:** Peepo handles that direction unless the standard machine detects external transport. Input and output decisions are independent.
- **On:** Peepo may transport in that direction despite detected automation. Normal needs, storage space, permissions, orders, meals and energy recovery still apply.
- **Off:** Stop new trips for that direction. Already carried outputs finish delivery. Carried supplies turn around and return to the assigned Supply container; a full source retains the items in cargo for retry.

Modes are per companion/workstation. Reordering or compacting work slots moves the modes with the station. They do not remove the workstation, change speed assistance or alter its energy cost. A shared machine's recipe selection is global to that machine, like the Cooking Pot plan. Supply/Output containers still use the planner's separate links and saved clicked face.

Existing Cooking Pot, Cider Press, Canning Kettle and Hearth Oven transport also honors On/Off. Automatic external-transport detection currently applies to standard `MachineBlockEntity` processors; Auto allows normal companion transport at the kitchen stations. Turning Hearth Oven Output off also stops tending/collecting: its ordinary unattended burning risk remains.

## What Auto detects

The machine checks only loaded adjacent blocks around its physical footprint, sharing one cached result across all helpers and menus. It recognizes enabled, correctly oriented input hoppers, enabled hoppers below configured output faces, and unpowered extractors aimed at configured outputs. Eject-enabled item output into a neighboring accepting storage also counts as connected output automation. Face input/output configuration is respected. Electricity and fluid connections are not item automation.

Item transfers through Fabric's sided machine storage record input/output activity **only after a successful outer transaction commits**. View extraction is observed too. Rolled-back probes do not count. Machine ejection records its successful moved count. Companion transactions enter a scoped exclusion so their own deliveries/collections cannot make Auto disable itself. This covers Jugcraft pipe/sorter/conveyor transfers, including conveyor slopes, and compatible third-party Fabric transfers. Vanilla hoppers use their native Container path and are recognized by connection/orientation instead.

Successful external transfers hold that direction for **200 game ticks (10 seconds at 20 TPS)**. A passive pipe by itself does not suppress Peepo. After pipe activity stops, Auto can resume after that window and the normal search delay. Known connected hoppers/extractors remain authoritative while correctly connected/enabled, even if temporarily empty or blocked; use On to request companion backup in that setup. Unknown integrations that bypass Fabric transfers cannot be identified universally: On/Off remain explicit overrides.

Neighbor checks refresh at most once per **40 ticks per queried machine**, and immediately when its side settings change. Recent activity is read directly without scanning a network. The sided wrapper refreshes its visible slots when side configuration/facing changes. No world-wide inventory polling, worker scans, network traversal or chunk tickets are added. Transient detection resets on load; a pipe-only route may briefly share work with Peepo until its next successful transfer.

## Standard processor transport

Standard processors now expose their item result/byproduct slots to the existing walking porter. Upgrade slots and input ingredients are never collected as products. Only slots enabled on an output face are eligible. No remote chest-to-machine item teleport is added.

Recipe-driven machines show a ghost slot in Jobs. Click a finished item to select its recipe; repeated clicks cycle matching recipes in stable ID order. Right-click/empty cursor clears it. The display lists ingredient counts. The cursor item is never consumed. Selection supports Jugcraft single-input and multi-input machine recipes, vanilla smelting in the Electric Furnace, and fluid-processing recipes that have item ingredients and an item result. Fluid-only recipes cannot be selected via an item ghost.

Peepo brings only missing items for one batch. Each ingredient keeps a distinct input slot; existing compatible stacks are retained and incompatible/extraneous inputs block supply until cleared. Fluid recipe ingredients preserve their required slot order. Sided insertion restrictions, exact item components, stack limits, transaction rollback and the existing 32-item trip limit remain in force. Power, water, process fluids, heat and machine formation remain their normal requirements; Peepo does not haul fluids.

A selected recipe also locks that machine's ordinary processing to it, preventing a partially delivered multi-input batch from accidentally becoming another recipe. Changing/clearing selection resets processing progress and fractional assistance, without creating/removing ingredients. Clearing restores automatic recipe matching. A missing selected data-pack recipe pauses processing until cleared or reselected. Recipe catalogs rebuild after reload and clear at server start/stop; selected lookup is constant-time during machine ticks.

Special workflows without a supported item recipe plan (such as Auto Crafter pattern stocking, Electroplating setup, tree-farm inputs and fluid-only processes) do not gain guessed ingredient supply. Their item outputs, when present, can still be collected. The interface disables unsupported directions. Existing assistance remains available independently, including machines with no item inventory.

## Multiplayer, energy and saves

Transport continues using the shared companion search/path budgets, four explicit workstation links, bounded inventory probes, movement deadlines and failure backoff. At most one real cargo stack travels per trip. This transport change retained its original movement cost; subsequent machine assistance uses the [machine-based effort rates and optional recipe overrides](companion-jobs.md#machine-effort-balance-and-recipe-overrides). Inputs come from the assigned Supply inventory and outputs go to the assigned Output inventory; all normal production costs remain on the machine. No new currency or progression bypass.

Server menu validity, owner/party permissions, reach and command cooldown validate the new buttons. Ghost edits also check loaded targets, range, locks and town restrictions. Transport rechecks access, inventory capacity and recipe needs at mutations. Detection cannot authorize access or perform transfers itself.

Optional `SupplyMode` / `OutputMode` values are saved in each work assignment, defaulting to Auto for existing saves. They are separate from target identity, so changes do not invalidate cargo manifests. Optional `CompanionSupplyRecipe` is saved on machines. Menu data appends eight values; inventory/ghost indices and existing entity/block IDs stay unchanged. Update client and server together.

## Validation

Common/client compilation and `assemble`, followed by offline launcher packaging. No automated tests, game tests, in-game GUI inspection, or multiplayer load tests were run, per owner instruction. Compilation is not gameplay validation.

Manual acceptance: exercise each direction/mode, all machine facings and multiblock parts; connected/disabled hoppers; extractors and pipe transfers; passive pipes; Eject and side changes; slow/stalled conveyors; two companions; reordering/removal/save/reload; recipe changes or Off while carrying; blocked/locked/full containers; overlapping multi-input recipes; missing data-pack plans; and normal assistance with both transport directions Off. Confirm counters/actual items and profile the intended server population before deployment.
