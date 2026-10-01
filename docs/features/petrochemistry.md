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

### Steel pipes and the heavy pump (batch 2, commit 6)
- **Steel Fluid Pipe:** a gunmetal pipe with hazard-striped junctions that carries **1,000 mB a tick**, four times the bronze pipe, for refinery flows. Three are made from two steel plates and a bronze pipe.
- **A pipe line now carries as much as its slowest pipe** (like cables): one bronze pipe in a steel line holds it to 250 mB a tick. Before, the rate came from whichever pipe the pump touched.
- **Heavy Pump:** a steel-tier pump with an olive volute, a chrome motor bell and hazard-striped guards. It pumps **1,000 mB a tick** from below (water as a spring, lava sources, or a tank) out of its top and sides, at **40 JE a tick**, with a 32,000 JE battery and a 16-bucket buffer. Made from steel plates, silver cable, steel gears, an electric pump and a machine casing.

### Distillation tower and the refined fluids (batch 2, commit 7)
- Four new fluids: **naphtha** (pale, runny), **diesel** (amber), **heavy fuel oil** (black and thick), each with a bucket and original textures, and **refinery gas**, a gas: it only lives in tanks and pipes, with no block and no bucket.
- The **distillation tower** is a 2×2 dieselpunk column seven blocks tall: a fired reboiler with a glowing firebox at its foot, olive sections between gunmetal flanges, three grated platforms with hazard rails, a ladder up the back and a domed cap with a vent.
- It turns **1,000 mB of crude oil into 100 mB of refinery gas, 250 mB of naphtha, 400 mB of diesel and 250 mB of heavy fuel oil** every 100 ticks, at 128 JE/t (12,800 JE a bucket). Volume is conserved.
- **Each fraction comes out at its own height**, at a chrome draw-off with a red valve on the front: heavy fuel oil at the base (layer 0), diesel two blocks up, naphtha four up and refinery gas at the top (layer 6). It pushes each only from the faces of its own layer, so one pipe or tank per draw-off keeps them apart. Crude oil goes in at any face.
- Recipe: steel plates, a steel tank, an advanced circuit and a blast furnace.

### Catalytic cracker (batch 2, commit 8)
- A **2×2 fluid catalytic cracker four blocks tall**: a slim riser-reactor and a fat regenerator vessel, olive with hazard bands, joined at the top by a chrome crossover with cyclone caps, with a catalyst hopper, a steam line, and a feed heater with a glowing firebox at the foot.
- **1,000 mB of heavy fuel oil + 250 mB of water + 1 cracking catalyst → 500 mB of diesel, 300 mB of naphtha and 200 mB of refinery gas**, every 160 ticks at 160 JE/t (25,600 JE a bucket). The water is the steam; it is used up.
- Draw-offs: diesel at the base, naphtha two blocks up, refinery gas at the top (layer 3).
- **Cracking Catalyst:** four from bauxite, sand and a nickel ingot; one is used per bucket of heavy fuel oil. It ties the cracker to the bauxite and nickel the earlier tiers already mine.
- Recipe: steel plates, an advanced circuit, two steel tanks, an arc furnace casing and a machine casing.

### Vacuum distillation (batch 2, commit 9)
- **Lubricant**: a thick golden fluid with a bucket. It is for machine upkeep (the gas turbine in batch 3 needs it).
- **Asphalt Binder**: a black lump of tar, the residue. Asphalt roads come in batch 4.
- The **vacuum distillation unit** is 2×2 and three blocks tall: a squat olive column stepping in towards the top, chrome steam ejectors and a condenser drum that keep it under vacuum, a fired heater at its foot and a residue chute at the back.
- **1,000 mB of heavy fuel oil → 400 mB of lubricant + 2 asphalt binder**, every 120 ticks at 96 JE/t (11,520 JE a bucket). The binder comes out of its item slot (hoppers and pipes can take it); the lubricant is pushed out of every face.
- Recipe: steel plates, a heavy pump, two tinplate tanks, an advanced circuit and a machine casing.

### Catalytic reformer (batch 2, commit 10)
- **Gasoline**: a thin, red-orange fluid with a bucket.
- The **catalytic reformer** is three wide, two tall and two deep: three olive reactor drums under chrome caps joined by a header, a fired heater with an exhaust stack, and a product manifold with two draw-offs.
- **1,000 mB of naphtha → 900 mB of gasoline + 100 mB of refinery gas**, every 120 ticks at 120 JE/t (14,400 JE a bucket). Gasoline comes out of the bottom row and refinery gas out of the top row.
- Recipe: steel plates, an advanced circuit, two tinplate tanks, a blast furnace and a machine casing.

### What refining gives (batch 2 summary)
From one bucket of crude oil, with every byproduct refined:

| Step | In | Out |
| --- | --- | --- |
| Distillation | 1,000 crude | 100 gas, 250 naphtha, 400 diesel, 250 heavy fuel oil |
| Cracking (or vacuum) the heavy fuel oil | 250 heavy + 62.5 water + ¼ catalyst | 125 diesel, 75 naphtha, 50 gas (or 100 lubricant + ½ asphalt binder) |
| Reforming all the naphtha | 325 naphtha | 292.5 gasoline, 32.5 gas |

So a bucket of crude oil cracked all the way gives about 525 mB of diesel, 293 mB of gasoline and 183 mB of refinery gas, for about 12,800 + 6,400 + 4,680 = 23,880 JE of refining. What those fuels are worth in generators is set in batch 3 (diesel generator, gas turbine) and audited in BALANCE.md (commit 19).

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
- Game tests `heavyPumpFillsFastThroughSteelPipes`, `bronzePipeLimitsASteelLine`, and `distillationTowerSplitsCrude` (a bucket of crude oil becomes 100/250/400/250 mB, and a tank at the diesel draw-off gets only the diesel).
- Game test `crackerCracksHeavyFuelOil` (heavy fuel oil, water and one catalyst become 500/300/200 mB of diesel, naphtha and gas).
- Game test `vacuumUnitMakesLubricantAndAsphalt` (a bucket of heavy fuel oil becomes 400 mB of lubricant and two asphalt binder).
- Game test `reformerMakesGasoline` (a bucket of naphtha becomes 900 mB of gasoline and 100 mB of refinery gas).
- Game test `pumpjackPumpsOil`: a powered pumpjack over pumpable oil fills its tank with crude oil and the reservoir goes down by as much.
- Game tests `extractorTanksOnlyTakeWhatTheyUse` (its tanks take water but not lava or crude oil, through Fabric's fluid API) and `extractorWashesOilFromOilSand` (a block of oil sand and water become 500 mB of crude oil and sand, using 250 mB of water).
- Not run: client play-testing of how the fluid looks and flows.

## World and event applicability
Not applicable for the fluid itself.

## Rollout and open questions
- IDs `jugcraft:crude_oil`, `jugcraft:flowing_crude_oil` and `jugcraft:crude_oil_bucket` are new and must stay stable once released.
