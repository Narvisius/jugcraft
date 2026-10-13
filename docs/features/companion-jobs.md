# Companion jobs, budgets, status, lunch and schedules

For current player controls and setup, use the [Peepo and Jughead player guide](../PEEPO_COMPANION_GUIDE.md). This feature record retains implementation history; earlier single-container, Porter-command and single-recipe-slot descriptions have been superseded.

Owner-directed stages 1-4 of the companion roadmap, implemented locally with OpenAI Codex (GPT-6) on `peepo-companion`. This extends the existing wheel, inventory, planner and rest behavior. [Cooking Pot assistance and recipe plans](companion-cooking.md) and the processor helper teams below extend that shared job system.

## Player use

Planning caches, paced client updates, rest checks and path reuse are documented in [companion performance](companion-performance.md).

Tools are drawn only for matching work and hazardous surfaces are avoided by travel and job positioning; see [tool stowing and surface safety](companion-tools-safety.md).

- Shift-right-click your Peepo or Jughead to open its inventory. The Jobs panel retains home, four ordered workstations and their remove/priority buttons. It now includes a separate Lunch row.
- The companion pauses walking and work while an authorized player has its settings/inventory menu open. It releases its current station safely and waits in place, then resumes its current commands after the last viewer closes the menu. Editing never silently changes Follow/Home/Work to Stay. Multiple viewers share the pause; menu replacement, invalid access/range, death, disconnect and unload cannot leave a saved pause behind. Carried deliveries remain real cargo and resume through the existing transport lifecycle.
- Select the companion with the Companion Planner, then right-click a lunch crate or the actual lunch cover block to assign its food source. Left-click with the planner, or use the Lunch row's x, to clear it. A lunch source does not consume a workstation slot. Existing loaded/dimension/reach and owner/party checks remain.
- Assignment text is colored by server status. Hover a row for its full location, dimension, priority and live status; the detail line also shows the hovered status. The left panel shows the companion's overall activity. Unimplemented machines explicitly report Unsupported job.
- Use the Routine button to switch the right panel to settings while keeping storage, costume, hand and player inventory accessible.
- Schedule cycles Auto, Day shift, Night shift. Auto preserves energy-driven work at any time. Day and Night use the server's Overworld clock even in other dimensions; night is ticks 13000-22999. Off-shift workers go home/rest. Sleeping remains night-only in the Overworld; other dimensions use sitting/passive recovery, avoiding an endless sleep in a fixed-dark dimension.
- Break at starts recovery at or below the chosen energy percentage. Resume at ends it. Defaults remain 0% and 80%. Break is adjustable from 0-70%; resume up to 95%, always at least 10 percentage points above break. Settings persist and are server-validated.
- Best meal favors the existing food regeneration quality score. Small meal favors lower-quality meals. Both apply to carried meals and lunch-container selection. Neither consumes the equipped hand or costume slot.
- Carry meals cycles 0-4 (default 2). With a bound lunch source, a companion can refill its eight cargo slots even when healthy. Player-supplied food also works; the reserve is a refill target, not a limit on what a player can give it.
- Food already carried is eaten before searching for loose food or lunch, under the existing health/energy/digestion rules. Meals still heal and give their existing energy regeneration buff; no stacking bonus was added.
- Bowls/bottles go back to a nearby source when possible, otherwise into cargo and back to the lunch source on a later visit. If cargo is completely full, the remainder drops rather than being lost. Full return destinations cause a 30-second retry backoff. Containers removed by a player are not recreated.
- Alerts default off. Enabled alerts play a quiet local chime on a new problem transition, at most once per 20 seconds. They do not send chat messages or poll remote players.

Follow and Stay retain their movement restrictions. A lunch assignment is not permission to abandon those orders or access somebody else's storage. An unavailable assigned source does not fall back to another lunch container; unassigned companions keep the bounded nearby-source behavior.

## Shared productive job contract

The menu pause revision (OpenAI Codex, GPT-6, from `ce52fe43`) tracks only that companion's open server menu instances, removing stale viewers by menu identity and normal validity checks. A higher-priority MOVE/LOOK goal suppresses navigation without setting or persisting NoAI. Horizontal motion stops immediately; gravity, swimming, ongoing eating and energy updates continue. Existing station release/bed exit and porter stop hooks preserve claims and carried inventory. Validation is compilation/assembly and launcher packaging only; no automated or in-game tests were run. Manually check walking/work interruption, changing orders while open, simultaneous viewers, disconnect, permission removal and closing/reopening while carrying cargo.

`CompanionJob` extends the existing station lifecycle and adds cheap readiness/status, a bounded productive work operation, and a worth-starting predicate. `CompanionJobs` resolves a loaded block directly or through explicitly registered adapters. The routine owns navigation, priority, claiming, travel timeouts, renewal and cancellation; the job owns its actual recipe/energy transaction.

The Generator Wheel is the first productive adapter. Its existing transaction still inserts exactly the energy debited from the companion, capped at 64 JE/t. The 128000 JE reserve, regeneration, food buffs, recipes and output faces are unchanged. A new visit requires at least 640 JE of free buffer so a draining near-full wheel does not cause constant tiny trips; an already occupied wheel keeps working while it can accept energy.

The existing per-station exclusive reservations remain the authority. Travel renews the claim; release occurs on food, orders, shift end, recovery, invalid target, failed path, removal or unload. Productive adapters must report the canonical controller position, remain available to their current claimant, expire stale claims, check their own resource/permission conditions, and return Working only after useful work commits. Future jobs must preserve power/fuel/input costs, output capacity, inventory components and recipe unlocks. Never call a whole block-entity tick twice to accelerate it.

## Processor assistance and helper teams

Animal work uses the same four assignment slots: [anchored shearing and milking jobs](companion-livestock.md), with Supply/Output, temporary animal holding and companion-operated pen gates.

Owner-requested on 7 October 2026, implemented with OpenAI Codex (GPT-6), against base commit `4bc09ffd`. The four-machine pilot is now expanded to all **36 processing machine types** in `MachineBlockEntity`: standard item recipes, fluid recipes, special crafting/plating, mining, farming and continuous pumping/separation. Electrical generators, kinetic engines, batteries and tanks have no processing-speed job and remain unsupported. The Generator Wheel and Cooking Pot retain their separate existing jobs.

Use the Companion Planner to assign the same machine to each tamed companion. Clicking any linked machine part resolves to its controller. Each companion still has its own four-job list, priority, schedule, meals and recovery settings. Full multiblock machines (including the Arc Furnace's casing structure) have **two helper positions**, each adding up to **25% of ordinary processing speed**; one worker gives 25%, two give 50%. A physically single-block machine, including an old compact copy, has one position adding up to 50%. Provide separate reachable standing spaces at the front/sides and sufficient headroom for Jughead. Workers face the machine and use the reusable valve, lever, mallet or wrench clips described below. Each helper position has a complementary role; the older machine-specific ideas remain future options.

### Companion reserve drain and animation suggestions

These numbers are **per Peepo/Jughead**, in reserve JE per productive server tick, using the machine defaults. At 20 TPS, multiply by 20 for JE/s. Machine-based effort rates replaced the original flat 8/16-JE rates on 9 October 2026, implemented with OpenAI Codex (GPT-6). Both large-machine helpers together pay the same effort as a compact copy's one +50% helper. Food regeneration can offset the net drain; passive regeneration stays suppressed while working. No effort is charged during a blocked, unpowered or idle production step. A final smaller reserve can be exhausted, and the existing recovery threshold ends the job.

The table describes full-size placed machines. **Compact legacy copies use one helper at twice their two-helper row's rate.** The Arc Furnace uses two even though only its controller is a block entity. Recipe overrides can replace these defaults as described below.

| Machine | Maximum helpers | JE/t per helper | Active clips (first / second helper) | Future specialized idea (not implemented) |
| --- | ---: | ---: | --- | --- |
| Electric Furnace | 2 | 2 | Lever / wrench | Slide a tray with tongs; adjust the control dial |
| Crusher | 2 | 4 | Mallet / lever | Feed the hopper with a little shovel; pull a lever |
| Arc Furnace Controller | 2 | 6 | Lever / wrench | Long tongs at the hatch; a second helper checks the controls |
| Alloy Smelter | 2 | 4 | Lever / wrench | Tongs at the loading hatch; turn the pour-control handwheel |
| Metal Press | 2 | 4 | Mallet / lever | Pull the press lever; arrange parts on the feed tray |
| Wire Drawer | 2 | 2 | Lever / wrench | Turn a crank; guide wire onto a spool |
| Circuit Assembler | 2 | 6 | Wrench / lever | Solder a component; inspect it with a magnifier |
| Pulverizer | 2 | 4 | Mallet / lever | Scoop feed into a chute; work a crank |
| Ore Washer | 2 | 4 | Valve / wrench | Swish a sieve basket; scrub with a small brush |
| Sieve | 2 | 2 | Mallet / lever | Rock a screening tray side to side |
| Sawmill | 2 | 2 | Lever / wrench | Guide a board along the feed table; turn a feed wheel |
| Coke Oven | 2 | 4 | Lever / wrench | Shovel coal; work a long poker |
| Steel Foundry | 2 | 6 | Lever / wrench | Work the bellows/control lever; steady a long ladle |
| Ore Drill | 2 | 6 | Lever / wrench | Brace and turn the feed crank; check a gauge |
| Deposit Drill | 2 | 4 | Lever / wrench | Adjust a lever; tighten a fitting with a wrench |
| Cobblestone Generator | 1 | 4 | Mallet | Tap and clear the output chute with a small hammer |
| Tree Farm | 1 | 8 | Lever | Prune a sapling; tend the seedling tray |
| Auto-Crafter | 1 | 4 | Wrench | Pick, place and tap parts on a small work surface |
| Crop Harvester | 2 | 4 | Lever / wrench | Sort the collection tray; adjust the cutting-height lever |
| Hydroponic Bay | 2 | 2 | Valve / wrench | Water seedlings; inspect leaves |
| Electroplating Bath | 2 | 6 | Valve / wrench | Raise and lower a parts rack |
| Rocket Workshop | 2 | 6 | Wrench / lever | Turn a wrench; inspect a panel |
| Pumpjack | 2 | 6 | Valve / wrench | Operate the stroke lever; grease a bearing |
| Fracking Rig | 2 | 16 | Valve / wrench | Turn a pressure valve; watch the gauge |
| Air Separation Unit | 2 | 6 | Valve / wrench | Turn a cold-box valve; wipe a frosted gauge |
| Distillation Tower | 2 | 10 | Valve / wrench | Turn a valve; read a temperature gauge |
| Catalytic Cracker | 2 | 16 | Valve / wrench | Work a pump lever; adjust a valve |
| Settling Plant | 2 | 6 | Valve / wrench | Rake the settling tray; brush the filter |
| Polymerization Reactor | 2 | 10 | Valve / wrench | Work the mixing control; collect a scoop of pellets |
| Electrolytic Cell | 2 | 16 | Valve / wrench | Raise an electrode rack; adjust the controls |
| Chemical Reactor | 2 | 10 | Valve / wrench | Work a mixing lever; check the sight glass |
| Synthesis Converter | 2 | 10 | Valve / wrench | Lean into a large handwheel; check pressure |
| Hydrotreater | 2 | 10 | Valve / wrench | Turn a valve; inspect a pipe fitting |
| Lithography Station | 2 | 16 | Wrench / lever | Adjust a lens; inspect a wafer |
| Ammonia Chiller | 2 | 4 | Valve / wrench | Wipe frost; turn the coolant valve |
| Cryogenic Liquefier | 1 | 20 | Valve | Turn an insulated valve; watch the gauge |

The Cooking Pot remains one helper, +50%, up to 16 JE/t (320 JE/s). The Generator Wheel remains one runner extracting up to 64 JE/t (1,280 JE/s), depending on available buffer space. These rates apply equally to Peepo and Jughead.

### Machine effort balance and recipe overrides

Effort uses the machine's **base** electrical tier, not its upgraded electricity draw. Rates per +25% contribution are 2 JE/t for base draw up to 12 JE/t, 4 for up to 24, 6 for up to 64, 10 for up to 128, and 16 above that. A lone +50% compact-machine helper pays twice that rate. A two-slot machine with only one helper pays only its +25% share. Unpowered hot-work exceptions are Coke Oven at 4 and Steel Foundry at 6 JE/t per +25%. Thus default team effort ranges from 4 to 32 JE/t and a single helper can never be charged more than 32 JE/t through this hook. Machine upgrades cannot inflate/reduce the effort rate, though faster completion changes total working time. Recipe duration contributes only through actual productive time; it is not multiplied into the rate again.

Single-input machine recipes, multi-input machine recipes and fluid recipes accept optional integer `companion_effort_per_quarter` from **0 to 16**. Omitted or zero means the machine default; 1-16 explicitly sets reserve JE per productive tick for one +25% contribution. For example, adding `"companion_effort_per_quarter": 10` to an arc-smelting recipe makes each of its two helpers spend 10 JE/t rather than the default 6, without changing processing speed or machine electricity. Existing recipes intentionally keep their defaults until a specific recipe needs different balancing. Vanilla smelting/crafting recipes and special fixed-cycle machines use their machine default; their foreign recipe formats are not modified.

Both automatic and filtered recipe selection carry the value from the already matched recipe into the existing paid-production hook. Fluid recipes do likewise. There is no extra recipe search, world scan, global ticker, persistent cache, or per-tick packet. The recipe codecs synchronize the optional value with the other recipe data; client and server must use the same updated mod build. Existing recipe JSON, constructors and saved worlds remain compatible, and data reload replaces recipe objects normally. Recipe overrides are validated on JSON decode and capped again before charging. Machine electricity, speed bonuses, inputs/outputs, partial final-reserve behavior, resting, food, cooking-pot work, crank/wheel generation and transport costs are unchanged.

Validation for this balance revision: `compileJava`, `compileClientJava` and `assemble` succeeded, and the local companion pack was rebuilt. No automated tests or gameplay runs were performed, per the owner. The existing processor reserve-cost expectation was updated for the new furnace rate but not run. Multiplayer balance and recipe reload behavior still require in-game review.

### Reusable work clips

Owner-requested on 7 October 2026, implemented with OpenAI Codex (GPT-6), against base commit `2a45e269`. All 36 supported processors now select one of four shared actions through `WorkAnimation.processor(kind, slot)`. Compact copies use their first role. Future job adapters can return any of these action IDs; the rendering layer currently validates processor targets as loaded `MachineBlock` instances. The Cooking Pot keeps its rim-standing spoon clip and the wheel keeps its running animation.

- **VALVE (100 ticks):** two hands turn a large octagonal metal handwheel back and forth while the torso rocks sideways and dips into the effort. A stationary hub shaft joins a short downpipe and floor flange; only the wheel and spokes turn.
- **LEVER (64 ticks):** both hands push/pull a broad wooden grip on a long hinged metal lever with a fixed support. The torso and head lean forward/back around the hips as the shoulders follow the effort.
- **MALLET (48 ticks):** both hands raise an oversized wooden mallet with a metal band above the brow, then make a quick downstroke and pause at contact. The head is turned 90 degrees around the handle, running front-to-back. The torso leans back for the lift and forward with a small dip on the strike.
- **WRENCH (64 ticks):** both hands turn a large open-ended wrench through a 103-degree tightening stroke and faster return. The torso/head rock sideways and shift weight; hand placement slides along the handle for leverage.

These are procedural clips on the existing vanilla `EntityModel` rig, shared by Peepo and Jughead and offset forward for the pumpkin costume. `MachineWorkClip` computes one reusable pose in each render state; both arm grips and the tool model read that pose. Feet stay planted. Action changes use the existing synchronized action/target data; phase comes from world time with a stable companion UUID offset. There are no new per-frame messages, server scans, item entities, tool inventory mutations, particles or sound events. Props disappear on cancellation, eating, resting, death or a removed/unloaded target. Equipped items return using the existing held-item layer.

Visibility revision requested on 7 October 2026, implemented with OpenAI Codex (GPT-6) against `2067fe95`: lever grip height increases from 2 to 3.4 model units, its bar width from 1.2 to 2.1, and the wrench grows from about 3.15 to 5 units overall with thicker metal and larger jaws. The enlarged lever uses a bounded fore/aft arc so the grip stays outside the machine and costume. Body transforms share a hip pivot despite the rig's different part origins, and arm grips are solved after moving the shoulders. Shirt, bare torso, shorts, head, jug and pumpkin parts follow together; feet remain braced. Clip periods and server behavior are unchanged. The existing wood/iron textures are reused without modification. Validation is compilation/packaging only; no automated or in-game visual tests were run.

Mallet/valve revision requested the same day, implemented with OpenAI Codex (GPT-6) against `fa8b9caa`: valve radius increases from 1.35 to 2.6 model units, with thicker rim/spokes and a fixed pipe extending from the hub to model-space ground at y=24. Its flange and pipe do not inherit wheel rotation or the companion's body sway. The mallet head grows from 2.2 to 3.3 units across, rotates 90 degrees around Y together with its strap, and uses a longer, thicker handle. Its grip now travels 2.8 units vertically through the lift/strike, synchronized with torso pitch and a small body dip. Both clips reuse the common hand solver and body transform, existing textures, action IDs and periods; the pipe is a visual support, with no world blocks, inventory, fluid behavior or server entities. Compilation and packaging are the only validation for this revision; automated tests and in-game visual inspection were not run.

The owner asset catalog was inspected; no suitable companion-rig clip was found. The closed-cube props and clip math are original source authored here. Wood and iron reuse unchanged Jugcraft textures `assets/jugcraft/textures/block/cider_press_wood.png` and `cider_press_iron.png` (existing original assets generated by `tools/cider_textures.py`). No library originals were modified or third-party art imported. This extends the simple existing renderer allowed by the framework policy; no new animation dependency or Blockbench export is introduced.

Reserve drain, machine power costs, production bonuses, reservations, recipe plans and save keys are unchanged. New action IDs are appended after NONE/INTERACT/STIR; update both client and server to render them. Common/client compilation and JAR assembly passed with `build-local.ps1 -Tasks assemble`; automated tests and in-game visual checks were not run at the owner's request. The packaged mod must still be inspected in game for all machine orientations, both characters, pumpkin costumes, two helpers, interruption/restart, and existing cooking/wheel actions.

### Processing and resource conservation

Each station position has an exclusive UUID reservation and a 100-tick renewable lease. The machine selects a worker's existing slot before offering a free one; a third companion sees Occupied. Positions cannot overlap, and releasing one worker leaves the other's reservation intact. Each helper refreshes only its participation request. **Only the machine's normal validated production step spends reserve and applies assistance.** A request alone never consumes energy or creates progress.

The shared hook considers at most two known UUIDs. One large-machine worker contributes a quarter-step per productive tick, or a small-machine worker contributes a half-step. Four accumulated quarters buy one bonus step; at most one bonus step is allowed in a machine tick. Extra steps spend the same electricity as ordinary steps, including existing speed/efficiency upgrades. Existing gas boosts remain separate/additive, rather than being multiplied for free. Recipe completion remains in the original path: required ingredients, water/nutrient solution, acid, containers, byproduct capacity, finite deposits, valid structure state and redstone gates are not bypassed. Short recipes, operation boundaries, resource shortages and discarded fractional effort can yield less than the nominal speed bonus.

Fluid recipes consume their ordinary batch inputs and create their ordinary outputs. Pumpjack/fracking bonus batches recheck output capacity and remaining reservoir, and fracking also consumes its extra fluid dose. Air separation makes extra nitrogen/oxygen and accumulates the corresponding fractional argon production; its small `CompanionArgonTicks` remainder is saved so reloads do not lose that byproduct share. Missing save keys start at zero. The main machine tick, item/fluid pushing, refill, source checks and world-search loops are never invoked a second time for assistance. Timed mining/harvesting only advances its existing work timer; completion still edits the original target once.

Incomplete structures, invalid recipes, insufficient inputs or output capacity, missing power for added work, disabled redstone, locks, lost assignments, off-shift/recovering workers and unloaded/removed stations stop useful assistance. A one-tick participation expiry plus per-worker/per-machine tick guards prevents stale or repeated contributions. Both current assignment and live town permission are checked before each reserve debit. Removal/unload releases reservations without teleporting helpers during entity tracking removal. Recipe ghost selection supports Cooking Pot and Hearth Oven; the Supply/Output adapters below handle Cooking Pot, Cider Press, Canning Kettle and Hearth Oven transport.

### Multiplayer cost and validation

The adapter caches at most 18 nearby approach candidates per slot for 20 ticks (front positions and controller sides); clearance is cached for 20 ticks per companion height. It checks loaded positions and separates the two workers. There are no new machine-wide worker searches, global tickers, chunk tickets or offline production. Existing cross-dimension companion search/path budgets still govern travel. Readiness comes from each machine's ordinary tick; menu inspection and assistance do **not** repeat recipe searches. Unclaimed machines examine at most two empty reservation slots at their productive step. Existing machine world queries and operation completions retain their original algorithms; higher throughput still warrants profiling a real factory.

No new assets, libraries, currencies, items, recipes or progression bypasses. Upstream food supplies companion reserve; mining/farming/chemistry supplies existing inputs, and the same downstream recipes consume outputs. Assignment keys remain compatible. Both client and server should use the updated build; old compact machines keep one-helper behavior.

Validation: `build-local.ps1 -Tasks assemble` compiles/assembles without test tasks; `scripts/package_modrinth.py build` packages the launcher build. **No tests or gameplay runs were performed for this expansion, per the owner's instruction.** Earlier companion test results predate processor assistance. Manual checks should compare one/two/three workers, separate positions and all facings, compact/full variants, thresholds and priorities, speed/electricity/resource accounting, full outputs/remainders/byproducts, depleted reservoirs, redstone, removal and save/reload before server deployment.

## Hand crank, Cider Press and transport

Owner-requested on 7 October 2026 and implemented locally with OpenAI Codex (GPT-6), starting from `9fedc278`. These are explicit assignments using the existing four work slots; Supply and Output are additional container links, separate from Home and Lunch.

### Hand crank

Assign an existing `jugcraft:hand_crank` with the planner. One companion operates it from a reachable, clear standing position. It feeds the existing kinetic network at **up to 16 KE/t**, debiting the same amount of its internal JE reserve only when a consumer accepts that energy. There are no queued companion turns or generation after leaving. Player cranking takes precedence and retains its normal exhaustion and stored-turn behavior.

Demand inspection reuses the kinetic network's bounded topology cache, at most once per second. Any receiving flywheel at **99% or higher** pauses that companion operator; it resumes only when connected flywheels fall below **90%**. The small full margin accommodates the flywheel's normal friction and avoids missing full capacity between inspections. With no accepting consumer, it stops without spending energy and backs off. Full flywheel gating is for companion operation, not existing player/engine generation. The existing flywheel remains a 2,000,000 KE store with proportional friction; a 16 KE/t crank alone cannot overcome full-speed friction or fill it to capacity.

The CRANK clip matches the actual block rotor's six degrees per tick and 4.5-pixel handle radius, without a per-companion phase offset. Its hands follow the wooden handle, the body rises and sways with it, and the short legs kick while hanging. All six facing transforms are mapped from the existing rotor resource; a wall-mounted crank is the clearest jumping presentation. Movement is visual on the client: the server entity remains at its checked approach, without per-frame teleports, extra item entities or new rotor geometry. Headroom includes the hop. Loaded-state guards now cover kinetic discovery, pushing and turning; chunk load/unload invalidates topology so reloaded connections are rediscovered without forcing chunks.

On 8 October 2026, OpenAI Codex (GPT-6) revised the operator position and clip at the owner's request. For horizontal shafts, companions now approach the right side as seen facing the exposed handle, and face across its circular path. The same-height and one-block-lower approaches still require solid footing and clear headroom; an obstructed right side reports blocked instead of silently putting the operator back in front. Vertical shafts retain the nearest clear horizontal approach because they have no unique horizontal front. Both Peepo and Jughead follow the handle forward/backward with their whole bodies, lift and kick their legs on the upper arc, and lean into the lower push. Shoulder positions account for the pumpkin costume, with both hands solved against the existing real handle. No server animation ticker, network updates, energy changes, new assets or dependencies were added. The owner asset library and catalog were inspected; this extends the original procedural rig rather than importing a matching library clip. At full acceptance the unchanged cost is **16 JE/t (320 JE/s at 20 TPS)** for **16 KE/t**; partial acceptance costs proportionally less, ordinary passive recovery is suppressed while working, and food regeneration still applies.

Validation for this revision: `compileJava`, `compileClientJava` and `assemble` succeeded, and the local companion pack was rebuilt. No automated tests or gameplay tests were run, as requested. In-game visual alignment across crank orientations and costumes remains unverified.

### Cider Press assistance

Assign an existing Cider Press. One companion automatically grinds loaded apples, then turns the screw on the pulp, using the shared **INTERACT** two-arm animation. It uses the same **one operation per eight ticks** gate as players; concurrent player interaction cannot double the work rate. Each completed grind/press operation costs up to **64 JE** (8 JE/t averaged over uninterrupted work). No work charge during the pacing delay, absent ingredients or a full juice trough. Existing food/passive recovery rules still apply between paid actions. Companion-produced pomace goes into the saved output tray instead of dropping on the ground. Manual pressing keeps its existing pomace drops. Normal apple/juice yields and the juice comparator remain unchanged.

Full Cider Press logistics were added on 7 October 2026 with OpenAI Codex (GPT-6), against `adbb7d55`. Assign the press plus distinct Supply and Output containers, keep one cargo slot free, and put tagged cider apples and empty glass bottles in Supply. The companion loads up to eight apples, works the grinder/screw, fetches bottles for existing juice, and carries bottled Sweet Cider and pomace to Output. The fixed workflow needs no ghost recipe. Bottles are converted on arrival: one real bottle plus one stored juice serving becomes one bottled cider in the tray, atomically with cargo removal. There is no bottle creation or separate fuel cost. Delivery uses the existing up-to-2-JE/t walking cost and normal search/path budgets; pressing keeps the cost above and the general INTERACT clip.

The press buffers at most eight bottled ciders and eight pomace. New apples are requested only when juice and both output trays have been cleared, and no apples enter during pressing. Missing bottles leave juice waiting in the trough and prevent a fresh batch; blocked Output also prevents another batch from starting. An already loaded batch can still finish if its juice/pomace fit. Outputs are preferred before ingredients using the existing porter order. Stock probes roll back all affected press counters; arrival rechecks current demand, and competing deliveries return unneeded supplies. Buffered outputs are saved in new optional `companionBottled`/`companionPomace` keys, defaulting to zero for old presses. Shift-right-click with an empty hand collects buffered output into available player inventory space; leftovers stay in the tray. Breaking the press drops buffered products once. Existing manual apple input, bottling and status interactions remain available.

These are companion-only item ports, not public hopper/pipe or fluid connections. The existing permission, loaded-target, sided-container and physical-cargo safeguards apply. No new block ticker, world search, packet type, recipe or asset was added. Validation: common/client compilation and assembly succeeded; no automated or in-game tests were run, per the owner. Manual acceptance still needs full-cycle production, missing bottles, blocked Output, two porters, simultaneous player use, full player inventory during tray collection, destruction and save/reload mid-trip and mid-batch. See the [Hearth Oven and canning analysis](companion-food-automation.md) for background; both [Canning Kettle logistics](companion-canning.md) and [Hearth Oven automation](companion-hearth.md) are now implemented.

### Canning Kettle

The [Canning Kettle adapter](companion-canning.md) fills water from a real bucket, returns the empty bucket to Output, loads fresh full jars and collects sealed or spoiled jars. It processes independently without a resident helper or speed bonus. The fixed workflow needs no ghost recipe. Only the existing transport movement cost applies, and active heat is still required.

### Hearth Oven

The [Hearth Oven adapter](companion-hearth.md) uses a ghost pie selection, supplies prepared raw pies and valid fuel, and approaches the front opening to load, wait, remove and carry pies with dedicated poses using the existing placeable pie models. Hot-pie tending takes precedence over ordinary supply priority. The oven holds one pie total; fuel is only requested for an actual pie, and normal burn rules remain. Moving costs up to 2 JE/t and collection costs up to 16 JE once per pie; waiting has no continuous energy drain.

### Supply and Output

The [general Porter command and role-cycling planner](companion-general-porter.md) move items directly between the assigned containers without a workstation. Porter is separate from Work; the existing machine helper jobs do not automatically empty their Supply chest.

The [transport controls and standard machine adapter](companion-transport-controls.md) add per-workstation Auto/On/Off for each direction, external item-automation detection, processor output collection and ghost-recipe ingredient supply for supported recipe-based processors. Speed assistance stays independent.

Select a companion with the planner, then **right-click containers to assign/cycle Supply (blue) and Output (yellow)**. The first two new containers fill the empty roles; clicking an assigned container switches its role, swapping an existing pair when both directions are valid. The clicked face is saved and its insertion/extraction rules are respected. Existing beds, workstations and lunch sources retain their normal assignment action. Left-click or the GUI x removes the link. Double chests are canonicalized, and locked/protected halves are checked together. A single container cannot have conflicting roles for one companion. Both container links appear beneath Lunch with names, coordinates, status and colored selection outlines.

The first concrete logistics adapter is the **Cooking Pot**. Select its finished dish in the companion's ghost recipe slot. Supply fetches only the missing ingredients for one recipe batch, respecting Ingredient predicates and counts. Output collects only the pot's result slots, including returned bowls/bottles/buckets. Automatic recipe mode still permits output collection but does not guess a supply recipe. Heat is still required and companions do not supply fuel or replace the heating block.

Porters walk between endpoints. Each trip uses one empty slot of the existing eight-slot cargo inventory and carries at most **32 items**, further limited by stack size, recipe need and destination space. A held/costume item is never cargo. Both item components and actual counts are preserved with Fabric transactions: source extraction and cargo insertion commit together; cargo extraction and destination insertion commit together. There is no remote source-to-destination teleport. Moving on a delivery trip costs up to **2 JE/t**; meals, off-shift orders, recovery and Follow/Stay interrupt transport safely.

The delivery slot is excluded from automatic eating, lunch stocking and meal-container returns. The physical item is saved in Belongings; the saved transport manifest identifies that slot and the intended endpoints and recipe. It never recreates an item. A full, locked, removed or unloaded destination retains the stack in cargo. A changed recipe or a batch supplied by another worker sends surplus ingredients back to Supply. Removing a route releases its reservation on the next eligible search and leaves its contents in ordinary cargo; player removal of the stack cancels its manifest. Pending carried deliveries can resume after eating, resting or reload. Owner inventory editing is still allowed.

Transport searches only four explicitly assigned workstations, in priority order, checking results before ingredients at each. Searches are staggered 80-99 ticks and use the shared search budget; views are capped at 128 per candidate, batches at 32. Navigation uses the shared path budget with at most two approach attempts, a 64-block range per leg, 40-tick repaths, a 600-tick deadline and 200-tick failure backoff. Travel validity is cached for ten ticks, but every inventory mutation rechecks loaded blocks, assignments, permissions, locks and recipe needs. No chunk tickets or offline transfers. `CompanionLogistics.Port` and registered adapters expose plan/demand/input/output hooks for future workstation support without changing the walking and cargo lifecycle.

Assignments 0-5 retain their meanings; Supply/Output append slots 6/7 and optional face data. Existing saves default to no transport links. Menu data appends two status fields, and its player inventory moves down to fit the extra rows; inventory and ghost-slot indices stay unchanged. Update both client and server. No new assets, dependencies, recipes, registry IDs or tier gates: these jobs consume existing companion energy/food and move existing recipe inputs/results. The crank and general interaction animation reuse the existing authored runtime rig and textures.

Validation: common/client compilation and assembly, plus launcher packaging only. No automated tests, game tests or in-game visual checks were run, following the owner's instruction. Before multiplayer deployment, manually check the crank on each facing, full/recharging flywheels, player takeover, two competing helpers, empty/full/locked/double containers, competing porters, recipe changes while carrying, player edits to cargo, unassignment, dimension changes, death, save/reload mid-trip, and return to work after meals/recovery. Large-server load and two-client concurrency remain unverified.

## Multiplayer cost and authority

[Greetings and short conversations](companion-social.md) add optional idle social gestures and sounds. Routine has a Social On/Off switch; work, food, rest, transport and commands take priority. Social discovery has its own shared budget and creates no navigation paths.

- `CompanionBudget` is keyed by MinecraftServer and shares separate FIFO admission lanes across all dimensions. It stores UUIDs, not entity/world references, and clears on shutdown. Entity unload releases current stations and removes queued requests.
- Existing `config/jugcraft.properties` gains `companions.searches_per_tick=4` (clamped 1-32) and `companions.paths_per_tick=8` (clamped 1-64). These are counts of admitted operations, not measured millisecond guarantees. Restart after config changes.
- The budget gates explicit work/rest/food searches, command and food path requests, seat path attempts, and random strolling. Expired requesters are removed, queues are capped at 4096 per lane, and deferred goals retry rather than interpreting a budget delay as an unreachable path. Vanilla navigation's own internal maintenance and the base entity simulation are not replaced.
- Explicit work links are considered first. When a usable assigned station exists, no ambient machine discovery is needed. Fallback discovery reuses the loaded-chunk index and reads at most 128 candidate positions; surface searches retain their 128-block batches. The index itself still rebuilds from loaded chunk block entities and should be included in profiling dense factories.
- Routine searches retain staggered 80-99 tick intervals, local path-attempt limits and 10-second unreachable cooldown. Productive travel is bounded to 64 blocks with a 30-second deadline. Higher-priority job rechecks are throttled and keep the current job if no better reachable station can be claimed.
- Lunch searches inspect at most four candidate stores per attempt; each store inspects at most 128 accessible views. Assignment avoids that ambient search. Refill is capped to four items per visit. Food pickup caps line-of-sight checks to 32 candidates; the existing spatial entity query still needs profiling around large dropped-item piles.
- Refill and returned-container transfers join source/destination changes in Fabric transactions. The cargo snapshot only includes slots 0-7. A failed insertion rolls back extraction; no overflow meal is dropped just because refill storage is full.
- Job status is cached server-side once a second. The open menu sends changed numeric status/preferences; there is no new per-tick status broadcast. Existing energy/eating synchronization remains unchanged and is another profiling target at large populations.
- Only the owner or authorized party can mutate preferences or assignments. Menu edits validate live entity, dimension, reach and ownership and are rate-limited. Lunch access checks current sharing, locks and town protection on use. Work and food cannot act in unloaded chunks, and there are no chunk tickets or offline generation.
- Operator command `/peepobudget` reports admitted/deferred search and path totals for the running server. It is diagnostic only; no telemetry or external reporting.

## Progression, compatibility and assets

Discovery-tier companion management: consumes existing crafted meals and uses existing planner, lunch sources, inventory and furniture. No new currency, dependency, recipe or resource gate is introduced; work props reuse existing original textures. Food production feeds companion recovery; companions continue powering the existing wheel. Production assistance includes the Cooking Pot, Cider Press, Hand Crank and the 36 processing machine types described above. Supply/output transport currently supports the Cooking Pot, Cider Press, [Canning Kettle](companion-canning.md) and [Hearth Oven](companion-hearth.md).

Existing Assignment0-4 retain home/job order. Assignment5 is the optional lunch source. Missing preferences load defaults matching prior recovery/work behavior. Meal-container return bookkeeping is bounded and saved separately; the physical items remain in the existing inventory. UI positions change, but slot indices and inventory save keys do not. Upgrade both client and server because the companion menu has new data fields.

Uses the existing Fabric event/transaction APIs, vanilla container UI and goals. No optional-library dependency is added. No textures, models or sounds were imported; alerts use an existing vanilla sound.

## Validation

Common/client compilation and packaging use `build-local.ps1 -Tasks assemble`. The initial implementation skipped automated tests at the owner's request. On 7 October 2026 the owner requested companion-only testing: [19 groups and 127 assertions passed](companion-tests.md), including gameplay, menus, save/reopen and one-client dedicated-server reconnect. Two-client concurrency and representative server-load performance remain unverified.

Entity-unload cleanup defers safe-exit movement until a later active tick/load. Moving between entity sections from Minecraft's tracking-removal callback can reenter tracking and crash shutdown; ordinary command/food/job release still exits immediately.

Manual acceptance before deployment:

1. Select each companion type; bind home, four wheels and a lunch crate/cover; reorder jobs and restart. Check no loss of existing inventory, ownership or saved assignments.
2. Fill/block/occupy/remove a wheel. Confirm state labels, priority fallback, release and recovery. Revoke party access while another player has the menu open.
3. Feed from cargo while holding a torch/tool. Refill with nearly full cargo, empty/locked lunch storage, different item components, competing companions and hoppers. Confirm bowl return, full-destination backoff, no duplicates and no lost meals.
4. Change shift at dusk/dawn, recovery thresholds while working, and dimension. Verify the 10-point recovery gap, sleeping restrictions and seated rest without forced loading.
5. Enable alerts, leave a persistent failure, and reopen the GUI. Confirm no continuous chime and correct live status.
6. Profile representative companion populations alongside the intended machines and dimensions; inspect tick time, navigation, packets and `/peepobudget`. The default limits are initial conservative settings, not a tested server-capacity claim.
