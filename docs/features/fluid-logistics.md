# Fluid logistics: gas holders, valves and filters

Status: implemented (batch 8, #57)
Proposal issue: owner request, 1 October 2026 ("merge it and start the next batch"), following suggestion 4 from batch 5 ("Logistics: a multi-block gas storage tank, filters and valves for pipes")
Owner: jimbozoomer-byte
Target milestone and tier: steel tier and up, for the oil and chemistry lines
Primary specialty and supported player role: logistics; the player who plumbs factories

## Plan

| # | Commit | What it adds |
| --- | --- | --- |
| 36 | Gas holder | A 3×3×3 Horton sphere: 1,024 buckets of one gas. |
| 37 | Fluid valve | A pipe segment that a redstone signal closes. |
| 38 | Fluid filter | A pipe segment that only lets one chosen fluid out into the tanks and machines it touches. |
| 39 | Docs and PR | Advancements, handbook, docs. |

## Player experience

### Gas holder (commit 36)
- **Three blocks every way:** an olive steel sphere, stencilled round its equator between hazard bands, on six gunmetal legs, with a ladder up the side, a relief valve and gauge on top and flanged inlets at the foot of the front and back.
- **1,024 buckets of one gas**, filled and emptied by pumps and pipes through any of its 27 blocks. **Only gases:** it refuses liquids, which belong in the steel tank (128 buckets).
- Right-click with an empty hand to read it; comparators read how full it is. No power.
- Chlorine and hydrogen from the electrolytic cell, and refinery gas from the refinery, now have somewhere big to wait.
- Recipe: steel plates, four steel tanks and a steel fluid pipe.

### Fluid valve (commit 37)
- **A steel pipe segment with a valve**: a gunmetal body round the pipe, a bonnet with a red handwheel, and a lamp that is green while open and amber while closed.
- **Open, it carries fluid like a steel pipe (1,000 mB a tick). A redstone signal closes it**, and the pipe network stops there: the pipes on either side are separate lines, so a pump on one side no longer reaches the tanks on the other.
- Lets a factory shut off a line with a lever, a comparator on a tank, or any redstone logic.
- Code: `fluid/FluidValveBlock` (a `FluidPipeBlock` whose `carries(state)` is false while powered); `FluidNetworks` skips pipes that do not carry, both when it searches a network and when a pump pushes into one. Models: `tools/pipe_models.py` adds the body to the pipe's multipart block state.
- Recipe: a lever, two steel plates, a steel fluid pipe and redstone make two.

### Fluid filter (commit 38)
- **A steel pipe segment with a strainer housing**: a chrome canister round the pipe with mesh windows and a lamp on top that lights while a fluid is set.
- **Fluid passes along it like any pipe, but the tanks and machines it touches only receive its chosen fluid**, and nothing until one is chosen. A storage that is also reached through an ordinary pipe takes anything.
- **Choosing the fluid:** use a filled bucket on it; or right-click it with an empty hand beside a tank or machine that holds the fluid (the way to choose a gas, which has no bucket); sneak and right-click to clear it. Right-clicking shows the current choice.
- One pump line can now feed several machines with different fluids from one gas holder or tank farm, each through its own filter.
- Code: `fluid/FluidFilterBlock` and `FluidFilterBlockEntity` (saves the chosen `FluidVariant`). `FluidNetworks` remembers which pipe reached each storage and only moves the filter's fluid through a filter.
- Recipe: two steel plates, two iron bars and a steel fluid pipe.

### Advancements
Under Pressure (gas holder), Shut-Off Valve (fluid valve) and Strained Relations (fluid filter).

## Connections
- Existing input producer: the electrolytic cell (chlorine, hydrogen), the distillation tower, cracker, reformer and fracking rig (refinery gas).
- Existing output consumer: the fuel cell, gas turbine, chemical reactor and polymerization reactor, through pumps and pipes.
- Technology connection: fluid networks.
- Magic connection: none.
- Reachable entry path: steel tanks and steel pipes come first; no circular unlock.
- Required vs optional: optional storage.

## Balance and automation
- Storage only: nothing is made or lost. The gas holder holds 38 buckets per block, against the steel tank's 32.

## Multiplayer and persistence
Server-side; the gas holder's store saves with its master block entity like the steel tank's, and a filter's chosen fluid saves with its block entity. A valve's state comes from redstone. Filters are set on the server when a player uses them.

## Dependencies and assets
No new dependencies. Textures and models are original (`tools/dieselpunk_models.py`).

## Verification
- `tools/check_mod_data.py` checks IDs, recipes and models.
- Game test `gasHolderHoldsOnlyGas` (PetroGameTests): through its far corner block it takes exactly 1,024 buckets of hydrogen, refuses water, and refuses a second gas.
- Game test `fluidValveClosesOnRedstone` (JugcraftGameTests): with a redstone block on the valve, a pump's water does not reach the tank for 60 ticks; once the block is removed, it does.
- Game test `fluidFilterLetsOnlyItsFluidOut` (JugcraftGameTests): an unset filter and a lava filter let no water into their tank while a tank on the ordinary pipe fills; a water filter lets it through.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.
