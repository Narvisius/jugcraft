# Industrial machine foundation (factory package 1)

Status: implemented (CI green; pending review). Shared infrastructure only: no new survival block, item or recipe yet. The first real forms arrive with package 2, the starter gas and acid factory.
Proposal issue: none. Package 1, "Shared machine foundation", of the owner's [factory implementation plan in PR #302](https://github.com/jimbozoomer-byte/jugcraft/pull/302) (draft planning, 10 October 2026); the owner asked on 10 October 2026 to begin implementing that roadmap.
Owner: jimbozoomer-byte
Target milestone and tier: steel-era industry; every later industrial package builds on it, starting with the [starter gas and acid factory](industrial-starter-gas-and-acid-factory.md)
Primary specialty and supported player role: machinery and factory operations

## Player experience

Nothing new to craft yet. This change gives every coming industrial machine the same dependable behaviour, proved on two test forms that exist only in the test mod.

**Forms.** A machine form is a validated description of one physical installation, within an envelope of **2 to 6 blocks on each axis** (width × depth × height, as the [art specification](industrial-machine-models-and-textures.md) gives them), so at most 216 positions. Each position is one of three kinds:

| Position | Meaning |
| --- | --- |
| Structure | A block of the machine. One of them, on the ground layer, is the controller: the only block that holds the machine's contents and does any work. |
| Clearance | Room a moving part sweeps through, such as a press head or a rotor. It must be free to place the machine, and anything put there later stops the machine until it is cleared. |
| Access | Open space inside the envelope, such as a walkway or the gap under a hood. Anything may stand there. |

A form also names its tanks by role (water, hydrogen, lye, and so on), its item slots, its **ports**, its **tool sockets**, the **process capabilities** it offers and its **operating profile**.

A form is refused when it is built if:
- an axis is outside 2 to 6, or the declared envelope is not actually used;
- it has no controller, more than one, or one off the ground;
- a part floats loose from the rest;
- a port faces into its own block or a clearance, two ports share a face, or a port reaches a tank or slot that does not exist;
- it has more than six tanks or two tanks with one role;
- it has no capability, or its family has no fluid recipes.

**Placing and breaking.** Placing the item checks every structure and clearance position first. If one is blocked, nothing is placed, and the player is told what is in the way and where: "Form Test Rig doesn't fit: Stone is in the way at 12, 64, -3 (column 1, row 1, layer 3)". Otherwise the parts are filled in and the controller stands where the player clicked, facing them. This works in all four orientations.

Breaking any part removes the whole machine and drops its item once, with the slots' contents and any inputs a running batch was holding. Nothing next to it or in its access space is touched. Pistons cannot move a form, because every part is an entity block.

**Ports.** Pipes, cables, conveyors and hoppers reach a form only at its declared port faces:
- fluid inputs only fill their tank, and only with a fluid the form's recipes use there;
- fluid outputs only drain their tank;
- item inputs only take what the recipes use; item outputs only give results;
- the power port takes energy.

Output ports push their tank or slots into the pipe or storage beyond, like the other machines. Every other face is closed, and tool sockets are never reachable through any face. The controller is deliberately not a generic container, so no fallback lookup can reach past the ports. Buckets work on any part.

**Screen and status.** Using any part opens the form's screen, in its family's existing theme:
- the energy bar, tank gauges and slots;
- a progress arrow, with one thin bar under it for each extra lane;
- a pale band in each output tank for the room reserved by running batches;
- the tool sockets and upgrade slots in the terminal panel, with a red edge on a socket whose tool is in use;
- Pause and Cancel buttons.

The terminal shows one of eight states:

| State | When | Example reason |
| --- | --- | --- |
| Unformed | A part is missing or a clearance is blocked; nothing runs, and paid work waits | "Something blocks the moving parts at column 1, row 1, layer 3" |
| Idle | Nothing to do, or paused | "Add inputs", "Paused" |
| Waiting for input | Inputs are present but make no complete batch | "Water tank needs 600 mB more Water"; "Water tank: nothing here uses Lava" |
| Waiting for a tool | A batch has its inputs but not its tool | "Bed socket needs Block of Iron" |
| Waiting for power | A running batch cannot pay this tick | "Needs 96 JE per tick" |
| Warming up | The first paid ticks of a batch started from cold | |
| Processing | Batches are running | |
| Output blocked | A batch's results would not fit, or a tank holds another fluid | "Hydrogen tank is full", "Hydrogen tank holds another fluid (Oxygen)" |

States are reported in that order of urgency: the structure first, then the first thing stopping a new batch, then the running batches. When no recipe has its inputs complete, the reason comes from the recipe that needs the fewest empty tanks and slots filled.

**Batches.** A batch starts only when its inputs, its tool and room for every one of its results are all present. Its inputs then move out of the tanks and slots into the batch, held in escrow, and its results' room is reserved. Another batch or an outside transfer can neither borrow the spent inputs nor fill the reserved room.

Paid ticks advance the batch. Finishing moves all its results out in one step. The batch's results are saved when it starts, so a restart, an unload or a recipe reload continues the same batch, never takes its inputs twice and never changes what it makes; a batch whose recipe has since been removed still finishes as started. Without power a batch waits and keeps its progress. Pause holds the running batches as they are.

Cancel returns each batch's untransformed inputs, all or nothing, and never the energy spent. A batch whose inputs no longer fit back keeps running instead.

**Tools.** A tool socket holds one reusable tool, such as a catalyst bed, pattern, mold or die. Only a player at the screen can put it in or take it out, and not while a batch that needs it is running. The work never uses the tool up.

## Connections

- Existing input producer and output consumer: none new. Forms run their family's existing data-driven fluid recipes (the [Chemistry branch](../branches/CHEMISTRY.md)), on the existing energy, fluid and item networks.
- Technology connection: a form belongs to a machine family, a `MachineKind` such as the Chemical Reactor, which owns its recipes and energy figures. The form owns its dimensions, ports, sockets, capabilities and profile. A new form therefore reuses its family's recipes without a new backend.
- Magic connection: none.
- Reachable entry path: unchanged. No progression changes until package 2 registers real forms with independently reachable construction recipes.
- For infrastructure, supported systems: the energy API (`EnergyStorage`), Fabric fluid and item transfer, fluid and item pipes, upgrade cards, the existing machine screen themes, data-driven recipes and Jugcraft's block entity saving.

## Balance and automation

- **Recipes.** Fluid recipes gain two optional fields:
  - `"capability"`: the form must declare it;
  - `"tool"`: an ingredient one of the form's sockets must hold, never consumed.

  A recipe with either field runs only in forms. The original one-model machines keep exactly the recipes they had, which have neither field. A form runs its family's recipes whose capability it declares, in recipe-id order.
- **Energy.** Each running lane pays the family's draw with the existing upgrade-card arithmetic, times its profile's share, rounded up:

  | Profile | Lanes | Tank size | Share | 96 JE/t draw becomes |
  | --- | --- | --- | --- | --- |
  | Entry | 1 | 2,000 mB | 100% | 96 |
  | Expanded | 2 | 8,000 mB | 90% | 87 |
  | Bulk | 4 | 16,000 mB | 80% | 77 |

  Lanes scale linearly: two lanes take two sets of inputs and give two sets of results. A profile changes only energy per batch, never yields. These are the plan's draft defaults.
- **Warming.** Warming is a label on the first paid ticks of a batch started from cold. It costs nothing extra, following the owner's choice of heating included in the processing power (starter factory, eighth batch, question 3).
- **No gain.** Results are exactly the recipe's. Cancelling returns only untransformed inputs and no energy. Breaking drops inputs and never results.
- **Bounded work.**
  - A structure check examines at most 216 positions, every 40 ticks or on the next tick after a part's neighbour changes.
  - Batch matching looks only at the machine's own slots and tanks and its family's recipe list.
  - An empty machine does nothing.
  - Pushing out of ports happens every 4 ticks: at most 1,000 mB per fluid port and 16 items per item port.

## Multiplayer and persistence

- **Server authority.** The screen's buttons are applied only on the server. Its data slots are read-only. The client builds the layout from the form's id and decides nothing.
- **Saving.** A form saves its slots, energy, pause state, form version, each tank under its role name, and each running batch: its escrowed inputs, results, progress, tool socket and cold-start flag.
- **Tanks by role.** Tanks are saved by role, so adding a tank to a later version cannot move one tank's contents into another. A renamed role loads through a declared alias.
- **Original machines.** Tanks saved by position now carry a layout version. A machine kind that later adds or reorders tanks declares a migration from each earlier layout. Each old tank then loads into the tank with its role, an old input into an input and an old output into an output, and nothing is clamped away. Layout 0 is every machine as released, and it saves and loads exactly as before.
- **Not done.** Two-client and dedicated-server play have not been tested.

## Dependencies and assets

None new. The test forms are registered by the test mod (`jugcraft-test`), never shipped, and draw every part with vanilla's iron block model. Real forms get their own models from `tools/industrial_forms.py`, which slices each form's one model into part models in the form's part order. Since the [Electrolytic Separator](industrial-electrolytic-separator.md), each form block numbers only its own parts (the Separator 0 to 11), not every position a 6×6×6 envelope could hold.

## Verification

- `python tools/check_mod_data.py` and `python scripts/check_repository.py`: passed locally.
- **Server game tests** (`IndustrialFoundationGameTests`), 14 tests:
  - descriptors check themselves: sizes, part numbering, rotation in all four orientations, and 15 kinds of invalid form refused;
  - placing with the item names the blocked clearance or part and places nothing, then fills exactly the rig's eight parts;
  - in every orientation only the port faces answer the fluid, item and energy lookups; the water port refuses lava;
  - breaking the far corner of the 136-part, 6×6×6 hall removes it all, drops one hall item and its slots once, and leaves blocks beside it, above it and in its access space untouched;
  - a batch escrows its water, reserves 500 + 250 mB, warms, and finishes on exactly 20 × 96 JE;
  - a tool recipe waits for its bed, then runs with the bed locked at the screen and through every face, and leaves it in place;
  - the status names a missing amount, an unused fluid, a foreign output fluid and a full output, spending nothing meanwhile;
  - without power a batch keeps its escrow and progress, then finishes on exactly the energy owed;
  - a batch saved while warming loads once with its progress and reservations, and a batch of a removed recipe finishes as started;
  - cancelling returns only the untransformed inputs, and a batch that cannot return them keeps running;
  - bulk lanes run three batches side by side at 77 JE per tick each, while the fourth set of inputs waits in its tank for room;
  - a blocked clearance or a wrong part unforms the machine without losing work;
  - breaking mid-batch drops the escrowed coal and the bed once, and makes no charcoal;
  - the Chemical Reactor never runs the form-only test recipes and its save is unchanged, and an old one-in/one-out tank save migrates into a three-in/two-out layout by role.
- **Client game test** (`IndustrialFoundationClientGameTests`): it opens the rig's screen while a batch runs, then with an unused input named, and takes `jugcraft_form_screen` and `jugcraft_form_screen_unused_input`.

### Results

- **Run [38084951488](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38084951488) (commit 529eb001): one new test failed.** Everything compiled. `aToolIsNeededAndHeldWhileItWorks` found the bed could be taken out at the screen while its batch ran. Inside the socket slot, the captured socket number was named `index`, which there means `Slot`'s own field (the slot's place in the menu), so the lock checked the wrong bit. Renamed in commit 72d91e4b. The same run also hit the intermittent `ArmsVIIIGameTests.javelinStrikesAndComesDown` failure that main has too; commit 6687b0ed carries PR #303's fix for it unchanged.
- **Run [38085442031](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38085442031) (commit 6687b0ed): green.**
  - The build and both server game test jobs passed, "All 1292 required tests passed", these 14 among them. The javelin test now runs five times and must pass all five.
  - All four client shards passed. Shard 2 ran `IndustrialFoundationClientGameTests` and took both screenshots:
    - `jugcraft_form_screen`: Form Test Rig, "Processing", 40%, power 29.3k of 30k, batches 1/1, the bed in its socket and an efficiency card beside it.
    - `jugcraft_form_screen_unused_input`: "Waiting for input" and "Water tank: nothing here uses Lava", 0%, batches 0/1.
  - The terminal clipped its longest state ("Waiting for inpu") and its power label crowded the number. Fixed with the first real form, the [Electrolytic Separator](industrial-electrolytic-separator.md): the state wraps like the reason, and the power reads in whole thousands when the label and tenths would not fit.

No survival playtest, two-client test or performance measurement has been done.

## World and event applicability

Not applicable: infrastructure inside machines, with no world generation, creatures, loot or seasons.

## Rollout and open questions

- **Next, package 2.** Its first slice is the [Electrolytic Separator](industrial-electrolytic-separator.md), in the existing electrolysis family. The second is the [Gas Burning Generator](industrial-gas-burning-generator.md), which adds **generator forms** to this foundation: a form can list the fuels it burns, with one fuel tank and power-out ports instead of recipes, and it never takes power in. The rest of the starter gas and acid factory follows, with the selected methanation-water drain.
- **Known limits:**
  - The original one-model machines' screens are unchanged; only forms show the eight states.
  - No JEI/EMI category or Jade readout for form-only recipes yet.
  - Companions do not work forms.
  - The comparator reads the share of lanes running.
  - Automated recipe priority and stock targets belong to the later automation module.
- **Reversible.** Removing the foundation would need no world migration while no form is registered in the game.
