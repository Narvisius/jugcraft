# Jimtafacts industrial integration

Owner scope: all supplied Jimtafacts content, original models and animations, kept local, with combined tests only after implementation. Assistance: OpenAI Codex. **This working tree is not compiled or tested yet.** Source classfiles were read statically with trusted CFR/classfile parsing; none were executed.

## Implemented locally

- **20 industrial machines:** original IC horizontal texture atlases and BC automatic workbench art. Shared JE, sided automation, recipe picker/locking and machine menus remain authoritative. Generator, battery tiers, furnace, crusher, washer, cutting mill, solar/geothermal and automatic crafting consolidate onto native implementations. Compressor, extractor, thermal centrifuge, recycler and metal cutter add missing processes. Metal Former rolling/extruding use the existing press and wire drawer. Counted batches really consume their input count. The centrifuge heats before separating washed ore and cools while idle.
- **55 equipment entries:** original tools, battery charge frames, armor and animated saber assets. Actual charge pays for powered tools, mining laser, crop analyzer, night vision, armor protection and shared jetpack flight. No research prerequisite. No creative-mode exemption to JE consumption. Native base armor still protects normally when powered bonuses run out.
- **20 robot programs / 42 items:** original robot cube geometry, original program skins and board icons, native hover/movement. Finite cargo, power, tools and fluid transport; claims and both transfer endpoints checked. Builder uses existing blueprints and real materials. Crafting preserves remainders and requires room before committing. The supplied BC robotics registry does not supply working robot AI; this is bounded native behavior, not a claim to recover absent code.
- **13 automation blocks and Gate Copier:** four tiers each of source AND/OR gates, item/fluid filters, light sensor, timer and pulsar. Gates use the native controller interface and data network. Rows targeting the same channel combine; OFF negates a condition, but missing sensors always fail closed. Materials unlock 1/2/4/8 rows. Remote toggles honor the gate's protected device list. Timer/pulsar period is 1–60 seconds, changed with sneak-empty-hand use; dyes select the channel. Original plug geometry and textures are retained. The original empty/full Gate Copier sprites now reflect a saved bounded rule set. Copy/apply checks source and target claims, row capacity and AND/OR compatibility; it never transfers the owner, inventory or access rights.
- **29 crop varieties / 42 crop and farm items:** source crop stages and seed/food/tool art, native three-stat genetics, explicit reachable breeding, bounded crossbreeding, source metal-root requirements, crop care, coffee/beer and fermentation. Seeds retain inherited genes. This is not the legacy humidity/air-quality cropcard simulation. Hazardous plants ask claims/PvP authority before harming anything. Metal flowers yield shared nuggets.
- **Recycling and cells:** six permitted junk items become one scrap; nine scraps become a one-use loot box. Recycling is restricted to an explicit junk tag. Original filled cell skins select according to the real fluid. Cells hold at most 1,000 mB, transfer transactionally, cannot mix fluids, and do not create contents when used in creative. Plant balls and biochaff feed the existing fertilizer/fermentation chain. Cell interaction currently targets fluid storage blocks, not free placement of arbitrary world-fluid blocks.
- **Compact component reactor / 37 items and blocks:** original reactor/chamber faces, source component icons and original 6×9 GUI. Three columns expand with up to six adjacent chambers. Uranium/MOX rods wear into spent rods; shared fuel assemblies and reprocessing provide acquisition. Vents, exchangers, coolant, reflectors, plating, condensators and lithium irradiation have actual grid behavior. Stored component heat/fuel wear, hull heat, fault state and finite JE persist. Cooling continues while stopped. Redstone and a full energy buffer pause fuel consumption. Active reactors prohibit rearranging components; chamber removal with occupied disabled columns shuts down safely.

- **Matter workshops and packaging / 9 items and blocks:** original mass fabricator, scanner, pattern storage, replicator, solid canner and fluid bottler atlases. Real JE produces finite UU-Matter with consumed scrap amplification. Scanning consumes a sample and blank crystal memory; replication rechecks the existing convenience-only output policy each operation. Canning preserves food nutrition/consume effects and returns its old bowl or bottle, consuming a tin can and 400 JE. Bottling moves real fluids transactionally in either direction, with output space checked before commit. Native machine-style interfaces show slot roles, work status and fill/empty controls. Original animated UU-Matter textures are retained.
- **Six stationary civil machines and Volume Marker:** original BC builder/filler/replacer/quarry/mining-well/flood-gate block faces, shared blueprints, JE and claims. Mark a maximum 32×32×32 loaded work box within 64 blocks. Work checks at most 16 candidate blocks and changes at most one every five ticks. Mining requires a real tool and room for every drop. Replacing preserves drops and consumes actual replacement blocks. Flood gates require an enclosed selected reservoir, consume 1,000 mB per source and reject protected edges. No terrain or chunk generation. The recovered source moving rails and drill now follow actual mining targets, with original source textures, repeated rail UVs, eased motion and reduced-motion support. Source slot overlays remain deferred. These rails are a client rendering of the bounded work area, not extra world blocks.
- **Powered infrastructure / 7 entries:** source teleporter/Tesla atlases, frequency transmitter and all four charge pad skins. Paired teleporters require an explicit right-click, safe loaded arrival and distance-priced JE. Pads charge equipped tools/armor with conserved JE at their tier rate. Defensive Tesla selects a bounded set of actual hostile targets, checks shared claims/ownership and line of sight, and never targets players or owned companions. These are native safety adaptations, not source voltage/explosion behavior.

The compact reactor uses recovered component capacities/rates and a native neighbor simulation. It deliberately shuts down on overheating instead of damaging other players' terrain. This is not exact legacy explosion, pressure, or component-vent heat recovery parity; the native fluid-vessel path is documented below. Existing large PWR/SMR/EPR plants remain separate systems. Parts keep original pixels and the GUI keeps original slot positions; there is no invented replacement texture.

## October 23 continuation: construction, heat and assembly

- 29 construction entries now registered and generated: sixteen original CF wall colors, foam/reinforced foam, curing, wooden/iron scaffold variants, alloy glass/door/fence and original paint/foam tools. Sand curing refuses to enclose entities. Foam placement and repainting use shared claims and finite consumables.
- Seven industrial upgrade modules now use existing processor slots: overclocking, storage, input tier, item eject/pull, fluid eject and inverted redstone. Shared transfer transactions and land checks remain authoritative.
- Six original heat/kinetic housings: solid/electric heat generators, electrical/kinetic Stirling outputs, electric shaft motor and manual kinetic generator. Heat transfers only between facing adjacent permitted devices. Buffers cap at 4,096 heat. Solid fuel pauses at capacity; electrical heat converts 5 JE to 4 heat; Stirling consumes 2 heat per JE/KE; the motor converts JE to shaft KE at 80%. Empty/full downstream shaft networks cost no motor power. This is a native bounded conversion chain, not the legacy heat-unit network or full fluid reactor mode.
- Assembly Table uses the supplied three-part 9-pixel-high mesh with corresponding collision height and unchanged textures. It runs the existing circuit recipes and five new redstone-chipset recipes, exposing the same recipe picker and automation. Original chipset sprites feed gate tiers, timers and electronics. Source laser transfer/integration-table behavior is still separate work.

The new spatial, construction, quarry-motion, heat and assembly changes are **uncompiled and untested**. Generating assets/catalogs is not runtime validation. Focused heat and spatial tests have been authored but not run; the owner requested one combined test batch at the end.

## October 23 continuation: fluid heat, vessel and laser tables

Prepared with OpenAI Codex, user-selected Astra High. **Local implementation only; no build or test has run for this continuation.**

- Fourteen heat/kinetic devices now include Fluid Heat Generator, Liquid Heat Exchanger, Steam Generator, Steam Kinetic Generator, both radioisotope generator variants, and Wind/Water Kinetic Generators. Full original face atlases and coolant/steam animation sidecars are retained. A shared industrial screen shows heat, tank levels, JE and connection instructions. The solid-fuel and isotope slots use the actual saved item.
- The chemical reactor mixes 1,000 mB water and two lapis into 1,000 mB coolant. Heated coolant passes through the exchanger: each mB returns one mB cold coolant and four heat. Its front connects to the front of a Stirling receiver or boiler; bottom faces expose fluid outputs, all other faces inputs. Other input/output fluids are refused, and a blocked output pauses consumption.
- The boiler consumes four heat plus one mB water for ten mB contained steam. The turbine returns one mB water and two KE from ten mB steam. It retains at most 64 paid KE when its shaft is disconnected. Fluid fuel uses the existing generator fuel values before the Stirling/steam conversion loss. These are bounded native ratios, not legacy HU/steam-pressure equivalence.
- RTG pellets use original source art and three plutonium pellets plus iron plates. One pellet supplies four heat/tick for 20,000 working seconds, with wear stored on the item. Full buffers pause wear; moving a pellet cannot reset its lifetime. This intentionally replaces the old indefinite fuel behavior.
- The electrical radioisotope generator uses that same finite pellet at two JE/tick and pauses at full storage; fuel cannot be recharged by moving it between heat and electrical generators.
- Wind/water devices now use the supplied four-blade rotor geometry, original blade pitches, mirrored 32×256 UVs and four source textures. Wood/iron/steel/carbon diameters remain 5/7/9/11 blocks with original 10,800/86,400/172,800/604,800 working-second lifetimes. Eight rotor/blade items have reachable recipes. A short native hub joins the recovered model to its casing. Rotation stops without jumping to a new pose and respects reduced motion. KE exits behind the casing. Loaded swept-space and claims checks run at most once per second; wind uses the existing altitude/weather rates and water needs a sufficiently submerged, flowing, unobstructed area. Wooden rotors remain wind-only. Wear follows the item and advances only when shaft power is accepted. This is native environmental balancing, not the source's stochastic wind manager or storm-damage formula.
- A complete 5×5×5 reactor vessel requires all six chambers around its central core, air in remaining interior cells and at least one fluid port. Original vessel, fluid-port, inventory-hatch and redstone-port atlases are retained. Every shell position must be loaded and permitted. Fluid mode converts fuel output to hull heat instead of JE; coolant removes four hull heat per mB and produces equal hot-coolant volume. Existing component vents still dissipate heat rather than exporting their cooling into coolant. Missing coolant/full hot output pauses fuel. A broken shell faults and stops the core, requiring a deliberate reset. Redstone ports pause fuel; cooling remains available while stopped. Hatches only automate component changes while stopped.
- Workshop Laser uses the original source emitter mesh and eight-frame beam texture. It sends up to 128 JE/tick to assembly/integration tables on a straight, loaded ray up to 16 blocks. Every traversed claim is checked, solid obstructions stop the beam, redstone pauses it, and a Fabric transaction commits both sides together. No JE gain, chunk loading, hostile-target damage or arbitrary remote inventory access. Render packets carry beam distance only. Six-facing collision matches the supplied emitter geometry.
- Integration Table retains its original three-part mesh and shares circuit recipes, recipe selection/locking and wired or laser JE. Eight AND/OR gate production recipes connect it to existing gate tiers. The Assembly Table remains usable with wired JE too. Legacy arbitrary gate expansion-chip metadata is still separate work.

Authored but **not run**: coolant output backpressure, steam/water conservation, retained turbine credit, shell-break shutdown, real laser transfer/obstruction and finite isotope wear. The full batch still waits until the remaining implementation is complete.

## October 23 continuation — transport and gate parameters

Local implementation only, with OpenAI Codex, user-selected Astra High. No builds,
tests, pack installation or publication were performed.

- **32 transport/control blocks** use byte-for-byte supplied pipe textures with connected
  native pipe geometry: cobblestone, stone, andesite, sandstone, quartz and gold
  item/fluid lines; five corresponding JE lines; iron item/fluid outlets; wooden
  item/fluid extractors; diamond item/fluid sorting fittings. Recipes and resource
  catalogs are included. Source MJ/RF variants consolidate to JE rather than adding
  a separate energy currency. This is not the original travelling-item renderer.
- Item push caps are 8/16/32/64 for the native material tiers. Fluid caps are
  250/500/1,000/2,000 mB per push, and JE caps 256/1,024/2,048/4,096.
  These follow existing native per-push semantics, not a server-wide per-tick quota.
  Stone and cobblestone item/fluid lines do not directly join; sandstone routes
  onward through pipes instead of delivering into adjacent containers. Iron's
  marked outlet is selectable by empty-hand use. Shared routing now caches
  directional graphs by entry, refuses oversized searches, and skips unloaded
  endpoints. Existing nondirectional networks retain shared caches.
- Wood item extraction reuses the native pneumatic 16-item/8-tick operation.
  Wood fluid extraction moves 250 mB per 8 ticks from a real facing tank, pauses
  under redstone, and excludes that tank from return routing. It never consumes
  world source blocks. Diamond items use the existing nine-sample sorter into a
  facing inventory; diamond fluids use the existing selected-fluid endpoint filter.
  These are native adaptations, not six independent legacy filter banks.
- Clay item/fluid pipes prioritize their adjacent storage destinations before
  ordinary destinations. Iron/diamond JE limiter fittings cycle through off,
  128/256/512/1,024/2,048/4,096 JE per push with the original limit-stage textures.
  Wooden and diamond-wood JE conduits use native pushed power; they do not extract
  arbitrary foreign batteries or emulate an MJ/RF converter. The structural pipe
  joins the shared control/data network. Item/fluid void endpoints start disabled;
  empty-hand use explicitly enables disposal, and redstone pauses it. Their
  acceptance participates in the caller's transaction: aborted transfers retain
  the original contents. Enabled void endpoints are deliberate disposal routes,
  not automatically overflow-only storage.
- Five removable expansions retain original icons and animation sidecars: light,
  sawtooth timer, periodic pulse, world clock and fader. First four provide a local
  channel reading; the fader smooths valid readings while absent sensors stay
  unavailable. Use another matching chip to cycle a 1–60-second period (fader:
  1–60 percentage points/pass), or sneak-use the chip to change channel. Empty-hand
  sneak removes the installed chip. Readings appear in the existing controller
  monitor/history as well as the rule evaluation.
- Lapis/quartz/diamond modifiers retain the source enum capacities: 1/0, 1/1,
  and 3/3 extra trigger/action parameters. Quartz and diamond halve the base row
  count, with a native minimum of one row. Here parameters are additional sensor
  conditions and output channels, edited without typing; this does not claim the
  source's complete trigger/action/item-filter API. All enabled conditions on a row
  must hold; missing sensors fail closed. Old rows beyond the new capacity are
  disabled. Original raised modifier studs and source UVs are retained.
- Right-click a modified gate for parameters; **Edit primary rules** opens the
  existing rule screen. Sneak-use empty hand removes the expansion first, then the
  modifier. Removing/changing hardware clears this gate's active relay outputs;
  breaking it returns installed chips. The copier transfers primary rows only and
  explicitly retains the destination's modifier/expansion settings and owner.

### Specialized transport continuation: 39 variants

Seven more variants now have native behavior and original pipe skins:

- Obsidian collects unreserved loose items within two blocks, at most 16 items per eight ticks. Claims, pickup delay and a collision ray are checked; it cannot pull through walls. Bottom-row cargo feeds nearby item pipes.
- Stripes switches between placing simple supplied blocks and breaking the facing block with a real top-left tool. It excludes block entities, unstable multi-block placement, fluids and unbreakable/hardness-over-50 blocks. All drops must fit before breaking, powered tools must have charge, and tools wear/pay for successful work.
- Diamond Wood extracts only listed item types or fluids represented by top-row sample containers. Samples stay separate from bottom-row cargo and are never exposed to unsided automation. Real fluid has a one-bucket buffer. Item extraction moves 16 per eight ticks; fluid extraction moves 250 mB. External inventories are inspected with a 128-view limit before transfer.
- Lapis paints the carried route color. Daizuli admits only its chosen color. Emzuli has four independently colored banks of nine real sample slots; its menu has four non-takeable dye buttons. First matching bank wins; unlisted items are refused. Left/right-click a dye button to cycle its bank color. Old 18-slot saves retain their samples in the first two rows and their first bank color. Samples are never exposed to automation. Item-specific route searches recheck samples, include color in visited state and fail closed at 1,024 states. Color is routing metadata, not a persistent item component. The source travelling-item render simulation is not reproduced.
- Raised collars mark directional device faces; Emzuli's collar uses the supplied colored lapis sprites. No replacement pixel art was generated. Redstone pauses active device work. The generator records every copied source texture.

### Superheated steam continuation

The steam generator has a mode button. Drain its output before switching. Ordinary steam costs four heat plus one mB water for ten mB steam; superheated costs twelve heat for the same water/steam quantities. A turbine turns ten superheated steam into ten ordinary steam and four KE. A second turbine turns that exhaust into one water and two KE. Thus twelve heat can produce six KE while returning exactly the starting water. Existing steam remains compatible.

Boilers stop at 95% output capacity as a native pressure interlock. Full outputs stop consumption; disconnected turbines retain at most 64 KE instead of consuming more batches. This is a bounded JE/KE adaptation, not the original pressure/temperature/explosion simulation. New two-stage conservation tests are authored and deferred with the rest.

## Further local implementation: tables, utilities, cables and terraforming

Prepared with OpenAI Codex, user-selected Astra High. All of the following is
**local, uncompiled and untested**. Resource generation writes assets; it is not
runtime validation. Conservation and ownership scenarios are authored for the
final combined batch.

- Charging and Programming Tables retain their supplied meshes, original GUI
  sheets and textures. Both accept cable or Workshop Laser JE. Charging transfers
  up to 640 actual JE each five ticks into one Chargeable item; bottom extraction
  only exposes fully charged equipment. Programming has twenty clickable native
  robot programs, consumes one blank/old board and 2,000 JE, and pauses before
  consumption if its output cannot fit. Ownership/party access is checked for
  menus, live automation and breaking. The original Advanced Crafting Table mesh
  uses the existing AUTO_CRAFTER and also accepts workshop lasers. This brings the
  shared processor housing roster to 21.
- Original Forge Hammer and Wire Cutter sprites connect manual work to shared
  plates/wires: two ingots make one plate, one supported plate makes two wires,
  with one tool durability per action. The Toolbox carries nine actual tools,
  retaining components and charge; nested containers and moving the open held
  toolbox are refused. Its original open sprite appears while its held menu is
  active and returns to the original closed sprite afterward.
- Brewing Barrel keeps the source barrel/tap atlases and all seven drink icons.
  Real water portions plus wheat/hops make beer; sugar cane makes rum. Only loaded
  time ages a batch. Fitting the hand treetap freezes fermentation, and an empty
  mug removes exactly one serving. The source beer stage thresholds and rum batch
  timing are retained; drink effects have native caps and a shared cooldown rather
  than cumulative source potion amplification. Moving the barrel returns exactly
  one barrel with the actual remaining batch and tap; its loot table adds no
  second empty barrel. This drop/save lifecycle still requires the combined test.
- Fourteen source cable families preserve supplied insulation/dye pixels and
  recovered widths. Native per-push capacities range from 32 to 8,192 JE. Paint is
  a visible label, not a separate electrical channel. Detector cable pulses only
  after committed activity on its connected bus and resets after ten idle ticks;
  it is not a route-specific current meter. Powered splitter cable disconnects
  its segment without stalling the remaining local branch. Shared discovery now
  treats closed segments as boundaries, refuses oversized graphs and avoids
  caching incomplete chunk-edge graphs. No legacy EU voltage explosions or
  per-length losses are introduced.
- Terraformer retains its active/inactive atlas and seven original blueprint
  icons (blank plus six reusable plans). It shares the marked-area machinery,
  claims and 32³ selection bound. It scans at most sixteen cells per pass and
  changes at most one per five ticks, paying 4,096 JE only for an actual change.
  Irrigation spends 250 mB water on moist farmland; cultivation consumes real
  seeds/bone meal; chilling uses 125 mB water for each added snow layer or freezes
  source water; desertification converts supported exposed soil/snow; levelling
  uses a real tool and reserves the natural-block drops; mushroom cultivation
  spends actual mushrooms. Containers, unbreakable blocks, unsupported falling
  sand and protected/unloaded cells are excluded. These are explicit bounded
  native operations, not source biome-ID rewriting or arbitrary third-party crop
  API compatibility.
- Biogas Jetpack retains the original icon, worn layer and 30,000 mB tank. Existing
  flight controls spend two mB per server-authorized thrust tick; normal jetpacks
  still spend JE. The fuel pack supports transactional biogas fill/empty from
  real tanks and cannot recharge from a JE charger. Construction Foam Backpack
  uses its original icon and the source-selected battery-pack armor layer. It
  stores 800 finite block charges loaded from the remaining contents of real foam
  canisters, and supplies either sprayer while worn. This adapts the source 80,000
  mB foam tank to existing canister units; it does not add another foam fluid.

Original assets and model-reference adaptations are recorded by the table,
utility, cable, terraforming and gear runtime-copy manifests. No supplied program,
class or ROM was executed. Native implementation does not imply full legacy
API/format parity.

## Source inventory and remaining work

### Further local original-art workshops

Implementation by OpenAI Codex (user-selected Astra High). The following are local
implementation additions, **not compiled or tested**:

- Kinetic Lathe retains the supplied block atlas, GUI and turnable textures. Its
  five original profile segments cost 1,000 KE and one of fifty tool uses per cut.
  Profiles persist on real wood/iron blanks. Exact source iron profiles finish as
  an iron shaft, thin axle, handle or drill component; wood produces four rods.
  Deterministic shavings replace the source random chance, with output space
  reserved before cutting. Casings, dense plates, turbine parts, conductors,
  boilers and power units have reachable shared-machine recipes and consumers.
- Industrial Luminator uses original six-face/compact bounds and active/dark
  pixels. It consumes one JE per four ticks through its support face; empty-hand
  use reverses redstone polarity. It does not reproduce contact fire damage.
- Rubber, carbon and electric boats use original item and 64×32 hull textures.
  The recovered source delegates hull geometry to legacy vanilla; the native
  renderer recreates that classic hull rather than claiming a supplied custom
  model. Carbon rowing, rubber collision damage/repair and electric four-JE
  equipped-power boost are implemented. A scoped electric-boat mixin recognizes
  lava for buoyancy. Ownership and passenger access persist. Visual movement,
  lava riding and passenger networking still need the combined playtest.
- Fluid Regulator has seven click-selected rates from 1 to 1,000 mB, per second
  or per tick, paying ten JE only for an actual transfer. It cannot be drained
  through a cached side view to bypass its configured delivery. Distributor takes
  fluid from its marked face and distributes through the others with rotating
  priority, either spreading a budget or selecting one outlet. Both use loaded
  native pipe networks, owner/party access and claim boundaries. Redstone pauses.
- Solar Distiller spends one actual water mB per 72 loaded, clear daylight ticks
  and produces one distilled mB. Steam Condenser passively recovers ten steam mB
  into one distilled mB, matching Jugcraft's boiler expansion rather than the
  legacy 100:1 ratio. Distilled water retains supplied fluid pixels, feeds the
  existing boiler and halves lapis use for coolant. Output leaves through the
  bottom even when production is blocked or sunlight ends. Source vent-slot
  acceleration and biome-dependent solar rates are not reproduced.
- Four original transformer tiers route JE through six sides, with automatic
  redstone, split and combine modes in the original GUI. Global and per-face
  tick budgets roll back with transactions; all side wrappers share buffer
  identity. JE is conserved without a separate voltage economy. CESU uses the
  original housing and native lead-acid battery behavior/recipes.
- Magnetizer uses the supplied housing and a contiguous original iron-fence
  column, up to twenty blocks above/below. Metal-boot wearers beside it rise;
  crouching descends. Each rider pulse costs five JE, duplicate same-tick lifts
  are refused, redstone pauses and current claims apply. It does not charge
  persistent fence metadata like the source implementation.
- Personal Safe keeps 54 real slots private to the owner/party and refuses
  hopper access. Its recovered six-box model, original mirrored 64×64 pixels and
  cubic eased hinged door render in-world. The original block atlas supplies the
  inventory icon. Barter Station and Energy Vendor have distinct real stock and
  payment rows. Cursor-stack samples configure the price/offer without consuming
  or creating items. Only a completed trade changes inventories. Charging spends
  real stored JE into the customer's selected held item at a selected fixed
  quantity; insufficient capacity refuses the entire payment.
  Shops start private. Publishing permits only their own trade/menu action
  across a player-land use restriction; it does not grant building, stock,
  template, hopper or town-container access. Shop owner permission is rechecked
  before every sale. Containers cannot be moved by pistons and resist explosions.

The utility, service and personal-workshop manifests record unchanged asset
copies and geometry adaptations. Deferred scenarios cover transformer simulation
rollback/rate limits, regulator delivery, steam recovery, barter rollback,
charging payment, personal inventory access, lathe output reservation and boats.
No source program, class or ROM was executed. No deployment or release occurred.

### Original piston engines and factory housings — local, untested

OpenAI Codex (user-selected Astra High) added six engine/dynamo entries using the
supplied engine_base and mj_dynamo dimensions, UVs, unchanged texture pixels and
triangular piston stroke. The inventory retains the source progress=0.2 pose.
Runtime motion follows the native running/temperature state and honors reduced
motion. Source stage-light emissiveness is not reproduced.

- Redstone Engine produces 1 JE/second; Stirling Engine burns real native solid
  fuel for 4 JE/tick and reserves an ash output before consuming fuel. Full energy
  or ash storage pauses consumption. Combustion Engine uses native diesel fuels,
  actual bottom-port cooling water and saved heat. It shuts down at 100 heat and
  resumes at 40; it never explodes. Electric Engine routes existing JE. Creative
  Engine requires a creative owner and has no survival recipe.
- Kinetic Dynamo accepts at most 64 KE/t from the rear shaft, converts at 85%,
  and saves fractional carry without generating energy from rounding. Engine JE
  input/output budgets are shared per tick and transaction-aware, so aborted
  simulations do not consume throughput. Engines require redstone to operate.
  All output is native JE; no parallel MJ/RF currency is introduced.
- Workshop Glass Tank uses the source narrow model and lower-join texture with
  the shared 16-bucket storage, comparator, fluid renderer and fluid-preserving
  dropped item. Shared tank traversal now stops at unloaded chunks and claim
  boundaries. Workshop Fluid Pump retains the original static pump housing on
  the shared 100-mB/t, 8-JE/productive-tick pump. This uses native below-block
  intake/source-water behavior, not the legacy distant retractable tube scan.
- Workshop Chute retains the original seven-section funnel and connected outlet
  meshes. It has nine cargo slots, a directional wide intake and six clickable
  output controls. Collection and round-robin delivery conserve actual items;
  a four-JE productive pulse accelerates the normal one-item/second flow to at
  most sixteen items per four ticks. Blocked delivery does not spend boost JE.
  Redstone pauses; owner/party access, pickup reservations and claim checks apply.
  Outlet necks appear only at enabled connected neighboring receivers. Source
  traveling-item simulation is replaced by the existing native item network.

New engine/factory copy manifests and deferred conservation scenarios accompany
this work. Resource generators and the static source-inventory writer were run
to author their output; no compiler, tests or game launch was run. Remaining
source systems include the factory heat exchanger and further industrial/utility
families; these entries do not establish full source parity.

[The generated static inventory](../../art/owner-library/catalog/edited-2026-10-23-jimtafacts/industry-source-inventory.json) records 612 registry families and 552 non-comment IC recipe statements with source paths/line numbers. It now includes 47 `PipeDefinition` fields omitted by the older Item/Block-only inventory. Current classifications and counts are in the generated inventory. These are **families/aliases/metadata entries, not distinct missing gameplay systems**. Some are deprecated/debug-only. Mapping ingredient names does not assert that a whole source recipe, all metadata variants or all original art has been integrated.

The latest inventory resolves duplicate `BCTransportItems` aliases to the same implemented pipe definitions. `obsidianFluid` is an unused declaration: the supplied `preInit` never constructs it, and no corresponding texture/collector exists. The inventory distinguishes that from a missing real feature. Native fluid/RT heat, superheated steam, reactor vessels, table lasers and gate parameters/expansions are local, but this does not claim complete source energy/transport parity. Unregistered decorative/obsolete textures remain preserved rather than becoming inert inventory clutter.

## Deferred validation

Written but not run: industrial processor batches/heat, robot conservation/claims/persistence, powered gear and flight, gate/filter behavior, crop genetics/anchors/claims, fluid-cell transactions and denied access, compact reactor fuel depletion/cooling/chamber removal/redstone/buffer pause, matter-output economy, canning/bottling conservation, civil machine claims/drop conservation, charge pad/teleporter safety, and source-byte/UV/recipe tests. Combined compilation, resource audit, server tests, original-model client rendering, two-player protections and owner playtest remain required before release. Nothing here is deployed or merged.
