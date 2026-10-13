# Companion Canning Kettle automation

For current player controls and setup, use the [Peepo and Jughead player guide](../PEEPO_COMPANION_GUIDE.md). This feature record retains implementation history; earlier single-container, Porter-command and single-recipe-slot descriptions have been superseded.

Owner-requested on 7 October 2026, implemented locally by OpenAI Codex (GPT-6) on `peepo-companion`, starting from `1ee119f6`. Extends the existing [Supply/Output transport](companion-jobs.md#supply-and-output); no new recipe, asset, dependency or tier gate.

## Setup and production

1. Put the existing Canning Kettle above an active heat source, such as a lit campfire. Leave a reachable standing space beside it, including enough headroom for Jughead.
2. Select a tamed companion with the planner and right-click the kettle to add it to the four ordered workstations. Work mode, schedules and recovery still apply.
3. Assign separate Supply and Output containers. Supply needs a water bucket if the kettle is dry, plus full, fresh, unsealed preserve jars made by the Cooking Pot. Empty Mason Jars are not kettle ingredients. No ghost recipe selection is needed.
4. Keep one of the companion's eight cargo slots free. It carries the water bucket to the kettle, fills it and collects the empty bucket for Output. Water persists between batches; a player draining it creates a new need for water.
5. It loads up to four eligible jars, then carries sealed jars to Output. The existing kettle heats for 200 ticks from cold and seals each jar after 400 boiling ticks. Jars process concurrently; no speed bonus or constant helper presence was added. The companion can do another assigned job while the kettle processes.

The Cooking Pot makes the preserves upstream; the kettle seals them for storage and future meals. For a two-companion production line, use the first companion's Cooking Pot Output chest as the second companion's kettle Supply chest. Each companion still has only one Supply and one Output, and cannot assign the same chest both roles to itself.

Only transport has a companion energy cost: up to **2 JE per moving tick**, using the existing porter rules. Heating still requires the real heat source. Loading/unloading uses ordinary porter interactions; this change does not add a continuous stirring animation or new animation clip.

## Blocked jobs, spoiled jars and manual use

- Do not fetch additional jars without water and active heat, or while the kettle contains pending output. Output is cleared before more input is fetched. A full/inaccessible Output retains products in the rack or existing physical cargo and retries without dropping them.
- Input rejects empty, sealed, partly eaten and spoiled jars. Freshness is checked again on arrival. A jar that spoils in transit is returned to Supply and will not be fetched again.
- Jars that spoil inside the kettle are carried to **Output unchanged and unsealed** as rejected products. They are never refreshed, sealed or destroyed. The workstation reports **Spoiled jars** until they are removed. The kettle no longer increments a spoiled jar's timer indefinitely. Sealed jars, by contrast, remain safe while awaiting collection.
- The single empty bucket from filling is buffered in the kettle and saved until collected. Ordinary empty-hand right-click also collects this bucket into available player inventory space, alongside the kettle's existing jar interaction. Any bucket that does not fit stays inside. Breaking the kettle drops the actual jars and any pending bucket once; water is lost as before.
- Existing manual water filling/draining, jar insertion, sealed-jar removal and shift-empty-hand removal of all jars remain available. The juice press, Cooking Pot and processor jobs keep their existing behavior.
- These are companion ports, not public hopper/pipe/fluid connections. [Hearth Oven automation](companion-hearth.md) is now implemented separately.

## Server cost, transactions and compatibility

`CompanionAssignments` recognizes the kettle as a workstation. `CompanionLogistics.Port.status()` supplies status for stations with no resident helper job, and the regular one-second report cache displays it. There is no false Unsupported job message and no helper claiming or idle standing goal for the kettle. `CompanionTransport` itself is unchanged.

Ports inspect at most four existing jars and one bucket remainder. Equal output item variants are grouped for one physical trip; custom components and freshness timestamps are preserved. Water input and bucket creation commit with cargo removal. Jar insertion/extraction commits with cargo changes, and probes roll back water, heat, remainder, deep-copied jar stacks and their associated processing times together. Removing several matching jars proceeds from the end so remaining progress stays aligned.

Shared assignment/owner/town checks, loaded-target checks, container face/lock rules, 80-99 tick staggered searches, bounded inventory views, path budgets and failure backoff remain in force. No extra block ticker, global search, chunk ticket, world entity, packet type or external API was added. Work happens on the server; no simulated player is used.

The optional saved key `companionBucket` defaults to zero in existing worlds. Existing water, heat, jar components and processing-time save fields are preserved. Spoiled jars is appended to the status enum, preserving existing status IDs. Update both client and server for the new status display.

## Validation

Compilation/assembly and launcher packaging only; no automated tests or in-game checks, following the owner's instruction. Runtime verification remains pending: one companion from dry kettle through a full batch, already-filled water, heat loss/restoration, full Output, differing preserve timestamps, grouped matching outputs, spoilage in transit/in kettle, two competing porters, manual interactions during transport, bucket recovery with full inventory, block destruction, interrupted trips, save/reload, owner/lock changes and two-client play. No claim of measured multiplayer throughput is made.
