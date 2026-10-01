# Petrochemistry (the dieselpunk oil line)

Status: **in progress**, built in batches of five commits (plan: [branches/CHEMISTRY.md](../branches/CHEMISTRY.md#petrochemistry-the-dieselpunk-oil-line)). Compiles in CI; **not yet played**.
Proposal issue: none. The owner asked for this branch directly on 1 October 2026: "the Diesel Punk Chemistry branch of the science tree which should involve crude oil processing turning it into more advanced useable versions of fuel using big machines and oil fracking", planned as 15–20 commits done five at a time.
Owner: jimbozoomer-byte (implementation: Claude Opus 5.5).
Target milestone and tier: steel tier and later (dieselpunk; see [ART_DIRECTION.md](../ART_DIRECTION.md)).
Primary specialty and supported player role: industry and power engineering.

## Player experience

### Crude oil (batch 1, commit 1)
- **Crude oil** is a real fluid: black, slow and thick (it spreads every 20 ticks, three blocks at most from a source on flat ground). It has a bucket, can be placed and picked up, and goes into Jugcraft's tanks, pumps and pipes and into any other mod's fluid storage.
- It **never makes new source blocks**, unlike water, so oil can't be multiplied by placing buckets.
- It is tagged `c:crude_oil` for other mods.
- Where it comes from and what uses it arrive in the next commits (reservoirs, the pumpjack, the oil sand extractor and refining).

### Fluid processing machines (batch 1, commit 2)
The base every oil machine is built on; on its own it adds nothing a player can build yet.
- A fluid processing machine has **input tanks, output tanks and item slots**. Its screen shows a gauge per tank (input tanks on the left, output tanks on the right) with the fluid's name and amount on hover, the item slots, a progress arrow and the energy readout.
- **Input tanks** take fluid from pumps, pipes and buckets, but only fluids the machine's recipes use in that tank. **Output tanks** give fluid to buckets and pumps, and the machine pushes them out of every outer face of every block it fills, into neighbouring tanks or pipe networks: up to 1,000 mB from each output tank every 4 ticks (pipes still carry at their own rate).
- Recipes are data-driven: `data/<namespace>/recipe/<machine type>/<name>.json` with `items`, `fluids`, `fluid_results`, `results` and `time` (see `chemistry/FluidRecipe`). The n-th fluid goes in the n-th input tank, results go to the output tanks and slots in order.
- A machine runs a recipe at its JE per tick when every input is present and every result has room; it waits with its progress kept when power is short.
- Comparators read how full its tanks are. Breaking the machine loses the fluid inside, as with the tinplate tank.
- These machines have no side configuration or upgrade slots: items go in and out of any face.

### Oil reservoirs (batch 1, commit 3)
- Oil lies in **hidden reservoirs under Overworld chunks**, fixed by the world seed. Nothing marks them on the surface.
  - **Conventional oil** (about 1 chunk in 12): 50–250 buckets in porous rock, which a pumpjack can pump (commit 4).
  - **Shale oil** (about 1 chunk in 4 of the rest): 200–800 buckets locked in tight rock, which only a fracking rig can free (batch 3).
- Every reservoir is **finite**: once a chunk has given its capacity it is dry for good. How much each chunk has given is saved with the world (as `jugcraft:oil_reservoirs` in the dimension's saved data); untouched chunks store nothing.
- The **Geo-Resonance Prospector** now reports oil under the 3×3 chunks it surveys: an "Oil" reading (middle depth) for pumpable oil and a "Shale oil" reading (deep), with the same vague 1–5 signal as the ores, from how much is left.

### Pumpjack (batch 1, commit 4)
- A **dieselpunk pumpjack** (nodding donkey), one block wide, three tall and three long: a hazard-striped skid, the wellhead with its valve tree and polished rod at the front, an olive walking beam with a horse head and bridle lines on an A-frame samson post, and at the back the gear reducer, crank arms with two heavy counterweights, pitman arms and a diesel motor with an exhaust stack. A caged lamp glows while it pumps. The classic style pack has a plain version.
- Place it with the wellhead (the front block) in a chunk the prospector shows Oil under. It pumps **2 mB of crude oil a tick** (a bucket every 25 seconds) at **32 JE/t** into its 16-bucket tank, and pushes the oil into pipes and tanks touching it.
- Its screen says "Pumping oil" or "No pumpable oil here". It stops when the reservoir is dry, when its tank is full, without power, or as its redstone setting says. It can't draw on shale oil.
- Recipe: steel plates, two steel gears, an electric pump and a machine casing. Steel tier.

### Oil sand extractor (batch 1, commit 5)
- A **2×2×2 hot-water extraction plant**: an olive separation vessel with a cone bottom and the oil froth showing in its open top, a belt carrying oil sand up to a hopper, a water heater with an exhaust stack, and a control panel with a gauge and lamp.
- **Oil sand + 250 mB water → 500 mB crude oil + a block of sand** (160 ticks), or **bitumen + 100 mB water → 150 mB** (80 ticks), at 32 JE/t. Oil sand needs silk touch to keep it whole; it otherwise drops 1–2 bitumen (or crushes into 3), so the whole block is the better route.
- Its water tank (8 buckets) takes only water; its oil tank (8 buckets) pushes into pipes and tanks touching it.
- This is the route to crude oil for anyone without a reservoir nearby: oil sand is a surface rock in its biomes.
- Recipe: steel plates, a hopper, two tinplate tanks, a machine casing and a steel gear. Steel tier.

## Connections
- Existing input producer: oil reservoirs (commit 3) through the pumpjack; oil sand and bitumen (existing rock and item) through the extractor; water from pumps.
- Existing output consumer: the fluid system (tanks, steel tank, pumps, pipes); refining comes in batch 2.
- Technology connection: steel tier; extends the fluid branch with the first fluids Jugcraft adds itself.
- Magic connection: none planned.
- Reachable entry path: oil sand (an existing surface rock) and oil reservoirs; neither needs anything from this line first.
- Required vs optional: optional for every earlier tier. Oil powers the dieselpunk tier; coal, steam, lava, sun, water and wind stay complete routes.
- For infrastructure: the fluid has no recipe of its own; balance is recorded with each machine.

## Balance and automation
- Crude oil is finite: it never forms new sources, and reservoirs run dry.
- Oil sand extractor: 5,120 JE and 250 mB of water per block of oil sand, for 500 mB of crude oil (about 10,240 JE a bucket); bitumen gives 150 mB for 2,560 JE. Burning bitumen in a steam generator stays an option (51,200 JE each); refining (batch 2) is where crude oil pays more.
- Pumpjack: 32 JE/t for 2 mB/t, so 16,000 JE per bucket of crude oil. A conventional reservoir (50–250 buckets) lasts about 21 minutes to 1 hour 45 minutes of pumping. What a bucket of crude oil is worth arrives with refining (batch 2); the whole chain will be audited in BALANCE.md.
- Units: millibuckets (1 bucket = 1,000 mB).

## Multiplayer and persistence
Fluids are registered whatever the config says, so saved oil and buckets are never lost. Placing and picking up uses vanilla bucket rules, so protections that stop bucket use also stop it.

## Dependencies and assets
Fabric API's fluid rendering registry draws the fluid. Textures are original, drawn by `tools/petro_textures.py`.

## Verification
- `tools/check_mod_data.py` checks that the Java fluids match `tools/petro.py` and that every fluid has its name, block model and animated textures.
- `tools/check_mod_data.py` audits fluid recipes (`tools/petro.py`): every item and fluid resolves, recipes fit the machine's slots and tank sizes, and no recipe gives out more fluid than it takes in (a recipe that releases fluid from an item, such as oil sand, must state how much as its `source`).
- Game tests (`PetroGameTests`): `crudeOilFillsTanks` (a tinplate tank stores a bucket of crude oil) and `crudeOilMakesNoNewSources` (two sources with a gap leave flowing oil, not a new source).
- Game tests: `oilReservoirsAreSeededAndFinite` (1,600 far-away chunks read the same twice, about one in twelve holds pumpable oil, a reservoir gives exactly what it holds and then nothing, and shale can't be taken as pumpable oil) and `surveyFindsOil`.
- Game test `pumpjackPumpsOil`: a powered pumpjack over pumpable oil fills its tank with crude oil and the reservoir goes down by as much.
- Game tests `extractorTanksOnlyTakeWhatTheyUse` (its tanks take water but not lava or crude oil, through Fabric's fluid API) and `extractorWashesOilFromOilSand` (a block of oil sand and water become 500 mB of crude oil and sand, using 250 mB of water).
- Not run: client play-testing of how the fluid looks and flows.

## World and event applicability
Not applicable for the fluid itself.

## Rollout and open questions
- IDs `jugcraft:crude_oil`, `jugcraft:flowing_crude_oil` and `jugcraft:crude_oil_bucket` are new and must stay stable once released.
