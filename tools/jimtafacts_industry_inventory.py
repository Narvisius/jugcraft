"""Static legacy registry/recipe inventory. Never imports or executes supplied binaries."""
from pathlib import Path
import struct,json,re,sys
ROOT=Path(__file__).resolve().parents[1];SRC=ROOT/'art/owner-library/originals/Oct23/Jimtafacts';OUT=ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts'
def fields(path):
 b=path.read_bytes();n=struct.unpack_from('>H',b,8)[0];o=10;pool={};i=1
 while i<n:
  tag=b[o];o+=1
  if tag==1:
   length=struct.unpack_from('>H',b,o)[0];o+=2;pool[i]=b[o:o+length].decode('utf-8',errors='replace');o+=length
  elif tag in (3,4,9,10,11,12,17,18):o+=4
  elif tag in (5,6):o+=8;i+=1
  elif tag in (7,8,16,19,20):o+=2
  elif tag==15:o+=3
  else:raise ValueError(f'Unknown constant tag {tag} in {path}')
  i+=1
 o+=6;interfaces=struct.unpack_from('>H',b,o)[0];o+=2+2*interfaces;count=struct.unpack_from('>H',b,o)[0];o+=2;out=[]
 for _ in range(count):
  access,name,descriptor,attributes=struct.unpack_from('>HHHH',b,o);o+=8
  for _ in range(attributes):
   size=struct.unpack_from('>I',b,o+2)[0];o+=6+size
  out.append((access,pool[name],pool[descriptor]))
 return out

def generate():
 import jimtafacts_services as services, jimtafacts_personal as personal, jimtafacts_engines as engines, jimtafacts_factory as factory
 import jimtafacts_cables as cables, jimtafacts_tables as tables, jimtafacts_utilities as utilities, jimtafacts_terraforming as terraforming
 import jimtafacts_transport as transport
 import jimtafacts_heat as heat,jimtafacts_construction as construction,jimtafacts_upgrades as upgrades,jimtafacts_gear as gear,jimtafacts_industry as industry,jimtafacts_crops as crops,jimtafacts_automation as automation,jimtafacts_robots as robots,jimtafacts_production as production,jimtafacts_reactor as reactor,jimtafacts_matter as matter,jimtafacts_civil as civil,jimtafacts_infrastructure as infrastructure
 mappings={"blockNuke":("native_adaptation","jimta_nuclear_charge","Original ring GUI and housing. Independent finite charges, owner-authorized ten-second fuse, disarm, bounded protected radius-eight blast; no legacy radiation or unbounded terrain destruction.")}
 for id,row in gear.GEAR.items():
  mappings[row['icon'].replace('.off','')]=('native_adaptation',id,'Original art and shared JE/tool/armor behavior; see industrial integration record for differences.')
 mappings.update({old:('native_adaptation',id,'Shared original-art industrial material chain') for old,id in {'itemPartCarbonFibre':'jimta_carbon_fibre','itemPartCarbonMesh':'jimta_carbon_mesh','itemPartCarbonPlate':'jimta_carbon_plate','itemPartCoalBall':'jimta_coal_ball','itemPartCoalBlock':'jimta_compressed_coal_ball','itemPartCoalChunk':'jimta_coal_chunk','itemPartIndustrialDiamond':'diamond'}.items()})
 shared={'itemRubber':'rubber','itemPartCircuit':'basic_circuit','itemPartCircuitAdv':'advanced_circuit','itemPartEleMotor':'electric_motor','itemPartCoil':'copper_wire','itemHarz':'latex_bottle','itemOreIridium':None,'itemFertilizer':'fertilizer','fluidBiogas':'biogas','fluidBiomass':'biomass','fluidDistilledWater':'distilled_water','blockOreCopper':'minecraft:copper_ore','blockOreTin':'tin_ore','blockOreLead':'lead_ore','blockRubWood':'rubber_log','blockRubSapling':'rubber_tree_sapling','blockRubLeaves':'rubber_leaves','blockBasalt':'minecraft:basalt'}
 for id,row in industry.MACHINES.items():
  if row[2]:mappings[row[2].split('/')[-1]]=('native_adaptation',id,'Original source atlas; native JE/processing recipe picker and automation. No claim that all legacy GUI modes or recipes are identical.')
 for id,(_,source) in industry.MATERIALS.items():
  if not source.startswith('bc:'):mappings[source.split('/')[-1]]=('native_adaptation',id,'Original material sprite; reachable shared-machine/crafting route and downstream component use. Source metadata aliases may reference the same item.')
 for tier,source in [('iron','itemIronBlockCuttingBlade'),('steel','itemAdvIronBlockCuttingBlade'),('diamond','itemDiamondBlockCuttingBlade')]:mappings[source]=('native_adaptation','jimta_'+tier+'_cutting_blade','Original icon and source 2/5/8 material tiers. Retained blade in shared sawmill upgrade slots; nine native-cost material-cutting recipes, visible requirement/status and JEI hints. Existing wood recipes remain usable without a retrofit.')
 for metal,source in industry.CASING_METALS.items():mappings['itemCasing'+source]=('native_adaptation','jimta_'+metal+'_casing','Two casings per shared plate, reversible two-to-one compressor route; original casing sprite and downstream heat/equipment recipes.')
 for metal,source in industry.DENSE_METALS.items():mappings['itemDensePlate'+source]=('native_adaptation','jimta_dense_'+metal+'_plate','Nine plates compress to one dense plate; cutting recovers exactly nine plates. Original source icon.')
 for old,new in [('itemTurningBlanksWood','wood_turning_blank'),('itemSteamTurbine','turbine_rotor'),('itemSteamTurbineBlade','turbine_blade')]:mappings[old]=('native_adaptation','jimta_'+new,'Original source art with explicit native component and hand-lathing/heat-workshop routes. No source turning-profile minigame is claimed.')
 mappings['itemLathingTool']=('native_adaptation','jimta_lathing_tool','Source fifty-use tool and original icon. Paid five-segment kinetic lathe retains blank profiles, original GUI/preview pixels; source iron profiles finish into native shaft, thin axle, handle and drill components.')
 mappings['blockLathe']=('native_adaptation','jimta_lathe','Original active/inactive atlas, GUI and five 24-pixel profile segments. 1000 KE and one tool use per cut; saved dimensions, reserved finite shavings and exact finishing profiles, source 10000 KE buffer. Native deterministic scrap instead of random output.')
 for old in ('blockLuminator','blockLuminatorDark'):mappings[old]=('native_adaptation','jimta_luminator','Original mounted bounds and active/dark atlas, six faces and compact cable mounting, actual one JE per four ticks, switchable redstone polarity. No source contact fire damage.')
 for kind in ('rubber','carbon','electric'):mappings['boat'+kind.title()]=('native_adaptation','jimta_'+kind+'_boat','Original item and 64x32 hull textures on a recreated classic vanilla hull (the source renderer delegates to vanilla). Native boat movement/ownership; carbon rowing, repairable rubber crashes and electric four-JE equipped-power boost with scoped lava buoyancy.')
 mappings['boatRubberBroken']=('native_adaptation','jimta_broken_rubber_boat','Rubber crash drop repaired with three rubber; original broken-hull icon.')
 mappings['itemTurningBlanks']=('native_adaptation','jimta_iron_turning_blank','Saved five-segment iron blank with source width five, original profile textures and four source target profiles. Native finished components rather than legacy damage-value crafting.')
 for id,source in production.ART.items():mappings[source.split('/')[-1]]=('native_adaptation','jimta_'+id,'Conserved fluid cell or native recycling/biomass chain; original source pixels.')
 for name,source in construction.BLOCKS.items():mappings[source.split('/')[-1].split('.')[0]]=('native_adaptation','jimta_'+name,'Original construction art; native foam curing, walls, scaffold support, alloy door/glass/fence. Color/reinforcement variants retain source textures.')
 for name,source in construction.ART.items():mappings[source]=('native_adaptation','jimta_'+name,'Original construction tool art and finite foam/paint consumption.')
 for name in ('itemDoorAlloy','blockDoorAlloy'):mappings[name]=('native_adaptation','jimta_alloy_door','Original two-part source door texture, iron-door/redstone behavior.')
 for color in ('Black','Blue','Brown','Cloud','Cyan','DarkGrey','Green','LightGrey','Lime','Magenta','Orange','Pink','Purple','Red','White','Yellow'):mappings['itemToolPainter'+color]=('native_adaptation','jimta_painter','One stateful painter retains all sixteen original filled-brush sprites and finite paint charges.')
 mappings['upgradeModule']=('native_adaptation','jimta_overclocker_upgrade','Seven source modules: overclocker, energy storage, transformer, ejector, pulling, fluid ejector and redstone inversion. Native processor slots and real transfer/JE behavior.')
 for name,source in heat.DEVICES.items():mappings[source.split('/')[-1]]=('native_adaptation','jimta_'+name,'Original heat/kinetic atlas, finite native heat buffers, conserved JE and existing KE shaft networks; no conversion loop can gain energy.')
 for old,new in [('fluidCoolant','jimta_coolant'),('fluidHotCoolant','jimta_hot_coolant'),('fluidSteam','jimta_steam'),('fluidSuperheatedSteam','jimta_superheated_steam'),('blockReactorVessel','jimta_reactor_vessel'),('blockReactorFluidPort','jimta_reactor_fluid_port'),('blockReactorAccessHatch','jimta_reactor_access_hatch'),('blockReactorRedstonePort','jimta_reactor_redstone_port')]:
  mappings[old]=('native_adaptation',new,'Original source art; bounded 5x5x5 vessel with real coolant ports, heat exchange, steam/condensate and native shaft output. Fail-safe shutdown; no legacy pressure explosion.')
 for name,source in [('wood','itemwoodrotor'),('iron','itemironrotor'),('steel','itemsteelrotor'),('carbon','itemwcarbonrotor')]:
  mappings[source]=('native_adaptation','jimta_'+name+'_rotor','Recovered original four-blade geometry, pitch and UVs, source diameter and lifetime. Native claimed/loaded clearance, weather/flow rates and shaft KE; lifetime follows the item.')
  mappings['item'+name+'rotorblade']=('native_adaptation','jimta_'+name+'_rotor_blade','Original rotor blade icon and reachable assembly recipe.')
 for source,native in [('fluidHotWater','jimta_hot_water'),('fluidPahoehoeLava','jimta_pahoehoe_lava')]:
  mappings[source]=('native_adaptation',native,'Original animated fluid art, shared pipes/cells and conserved liquid heat exchange. Hot water heats in the Chemical Reactor; pahoehoe comes from lava heat recovery and quenches to basalt in the same reactor. No claim of source world-cooling timing.')
 mappings['itemCellPahoehoelava']=('consolidated_native','jimta_fluid_cell','Universal cell holds pahoehoe lava and displays its original filled-state icon; no separate disposable container.')
 mappings['fluidConstructionFoam']=('native_adaptation','jimta_construction_foam_fluid','Original animated liquid sprites; existing Chemical Reactor converts powder and water to 32 spray charges, shared tank/pipe transport and transactional foam-backpack refill. Placed liquid does not automatically harden; use the sprayer for curing blocks.')
 mappings['itemCellCF']=('consolidated_native','jimta_fluid_cell','Reusable universal cell with original CF filled-state sprite.')
 mappings['itemFluidCell']=('native_adaptation','jimta_fluid_cell','Single conserved universal fluid cell uses native fluid component and transactions; original filled skins selected by fluid.')
 mappings['itemCellHydrant']=('native_adaptation','jimta_hydrating_cell','Original icon. Refillable water-only 1000 mB shared cell, 25 mB per changed farmland moisture value, no instantaneous growth. Tank fill/drain and automation use transactional item-fluid storage; current claims and no-op conservation apply. Native moisture rather than legacy crop water-storage counters.')
 mappings['blockITNT']=('native_adaptation','jimta_industrial_tnt','Original six-face atlas and native placed charge. Owner-bound manual/redstone/remote ignition, saved fuse/identity, four-block mining burst with retained drops, current claims and inventory/high-resistance protection. No falling primed entity or unrestricted legacy blast.')
 for old,native in [('beer','jimta_barrel_beer'),('rum','jimta_barrel_rum')]:mappings[old]=('source_metadata_token',native,'Drink-family metadata token; finite original-art brewing-barrel drinks are already registered, including maturity/serving behavior.')
 mappings['blockBarrel']=mappings['itemBarrel']=('native_adaptation','jimta_brewing_barrel','Original six-face barrel/tap atlases and portable barrel icon; finite wheat/hops/water beer and sugar-cane rum, recovered ageing thresholds, tap freezes ageing, original seven drink sprites. Preserves batch on moving; native capped drink effects.')
 for old,new in [('itemToolForgeHammer','forge_hammer'),('itemToolCutter','wire_cutters'),('itemToolbox','toolbox')]:
  mappings[old]=('native_adaptation','jimta_'+new,'Original icon; native manual shared-material conversion with real input/wear, or nine actual persistent tool slots. No nested containers.')
 mappings['itemMugBooze']=('native_adaptation','jimta_barrel_beer','Seven original age/rum icons with finite barrel serving, composition and maturity. Not legacy unbounded cumulative potion amplification.')
 mappings['itemMugCoffee']=('native_adaptation','jimta_coffee_mug','Three original coffee sprites and native drink effects/recipes already registered by crop integration.')
 for name,(source,width,paint) in cables.CABLES.items():
  row=('native_adaptation','jimta_'+name+'_cable','Original cable diameter and supplied insulation/color pixels; shared JE capacities, dye labeling, detector committed-bus-traffic pulse or redstone-disconnecting splitter. No legacy EU voltage explosions or line loss.')
  mappings[source]=row
  mappings['item'+source.removeprefix('block')]=row
 mappings['itemTinCableI']=('native_adaptation','jimta_insulated_tin_cable','Original insulated tin cable with original dye pixels and 32 JE per-push capacity.')
 for source,native in [('blockMiningPipe','jimta_mining_pipe'),('blockMiningTip','jimta_mining_tip')]:mappings[source]=('native_adaptation',native,'Original pipe/tip sprites and source shapes. Basic miner consumes real pipes in a connected downward column, retains block drops, pays tool/JE costs and retracts into output slots; saved deployed positions, current claims and no chunk loading. Manual tip breaking returns one pipe.')
 mappings['blockMiner']=('native_adaptation','jimta_civil_miner','Original active/inactive atlas with native marked single-column mining, paid JE/tool use, finite output storage and protection checks. Connected finite drill pipes, paid retraction and charged 5x5/9x9 scanner surveys now deploy and recover actual lateral pipe branches, one reachable ore per depth.')
 mappings['blockMachine']=('source_metadata_token','machine_casing','All 16 base BlockMachine metadata slots audited: shared basic/advanced casing resources, original-art iron/electric furnaces, macerator, extractor, compressor, native solid canner, physical-pipe miner, shared factory pump, magnetic fence lift, electrolyzer, recycler, induction furnace, matter fabricator and terraformer. Each is functioning native code, with differences recorded separately; this family is not an additional machine.')
 mappings['blockBeam']=('source_unfinished_prototype',None,'Supplied accelerator tile only declares an energy buffer; emitter produces a decaying no-collision particle every 100 ticks and never uses its stored energy. No completed accelerator process or housing art was supplied. Not claimed as implemented survival equipment.')
 mappings['itemDebug']=('source_development_tool',None,'Supplied developer-only reflection, tick acceleration and infinite-energy debugging item; not a missing survival reward or machine. Native operator/debug facilities remain separate; this unsafe old runtime tool is not executed or ported.')
 mappings['blockIronFurnace']=('native_adaptation','jimta_iron_furnace','Original atlas with actual native furnace block entity, fuel, recipes, XP, hopper sides and UI. Keeps native smelt time rather than claiming the original faster speed.')
 mappings['blockFermenter']=('consolidated_native','industrial_fermenter','Existing controlled-fermentation vessel handles the supplied biochaff through the conserved water/broth/exhaust recipe, native ports, JE and recipe picker. No second incompatible fermentation machine.')
 mappings['blockAdvMiner']=('native_adaptation','jimta_civil_advanced_miner','Original atlas; shared bounded marked excavation, real tool/enchantments, 256 JE per mined block and 13 retained include/exclude drop samples. Empty include mode mines nothing. Native area marker replaces charged source scanner; no legacy infinite depth or Silk Touch toggle without an enchanted tool.')
 mappings['blockTerra']=('native_adaptation','jimta_civil_terraformer','Original active/inactive atlas; six reusable original-art plans, bounded marked-area soil/crop/snow/flattening operations, conserved supplies and water, 4096 JE per actual change, existing claim and container safeguards. No random biome-ID rewriting.')
 mappings['itemArmorJetpack']=('native_adaptation','jimta_biogas_jetpack','Original jetpack icon and worn layer, source 30000 mB biogas capacity, native shared flight controls at 2 mB per thrust tick; transactional tank filling and no JE charging.')
 mappings['itemArmorCFPack']=('native_adaptation','jimta_construction_foam_pack','Original icon and source-selected batpack armor; 800 finite foam block charges loaded from actual partly-used canisters, chest-slot supply to both native foam sprayers. Saved charges also bridge to transactional liquid construction foam at 100 mB per sprayed block; tank filling and emptying preserve whole doses.')
 for mode in terraforming.MODES:
  mappings['itemTFBP'+('' if mode=='blank' else mode.title())]=('native_adaptation','jimta_terraform_'+mode,'Original reusable terraforming blueprint in the selected-area native workshop; no plan consumed per operation.')
 for id,row in reactor.PARTS.items():mappings[row['source']]=('native_adaptation',id,'Original compact reactor component art and source tuning; bounded native JE simulation, fail-safe shutdown instead of terrain explosion.')
 for old,native in {'itemUran':'jimta_uranium_fuel_material','itemMOX':'jimta_mox_fuel_material','itemPlutoniumSmall':'jimta_small_plutonium'}.items():
  mappings[old]=('native_adaptation',native,'Original source pixels. Uranium material conservatively repackages one existing LEU assembly; MOX adds one recovered plutonium pellet for the single-rod route. Nine small plutonium pieces conserve one pellet and its aggregate radiation. Raw materials cannot enter the reactor grid.')
 for old,native in {'itemUran235':'jimta_uranium_235','itemUran235small':'jimta_small_uranium_235','itemUran238':'jimta_uranium_238'}.items():
  mappings[old]=('native_adaptation',native,'Original source sprites; paid thermal centrifuging of an already enriched native cylinder produces one U-235 and seven U-238. Both fractions are required to repack that cylinder. Nine small U-235 pieces equal one whole piece. Game-unit proportions, not physical isotope composition; total item radiation is conserved. No raw-ore enrichment shortcut.')
 mappings['itemIngotUran']=('consolidated_native','uranium_ingot','Uses existing uranium ingot, native ore-processing recipes, shared storage block and radiation; no parallel nonradioactive metal.')
 for id,source in matter.DEVICES.items():mappings[source]=('native_adaptation',id,'Original source machine art; native economy-constrained matter or conserved food/fluid packaging.')
 mappings['itemrawcrystalmemory']=('native_adaptation','jimta_raw_crystal_memory','Craft and smelt blank memories, then scan real samples.')
 mappings['itemcrystalmemory']=('native_adaptation','jimta_crystal_memory','Persistent item patterns with server output policy enforced during scanning and replication.')
 mappings['fluidUuMatter']=('native_adaptation','jimta_uu_matter','Finite fluid generated from real JE and optional consumed scrap amplification; original animated fluid art.')
 mappings['itemFreq']=('native_adaptation','jimta_frequency_transmitter','Pairs loaded same-dimension pads; explicit safe travel and real JE.')
 mappings['blockChargepad']=('native_adaptation','jimta_chargepad_batbox','Four original charge-pad skins, finite tiered JE transfer to equipped gear; no voltage damage.')
 mappings['blockNuclearReactor']=('native_adaptation','jimta_compact_reactor','Component grid, chamber expansion, finite fuel, original GUI, heat and JE.')
 mappings['blockReactorChamber']=('native_adaptation','jimta_reactor_chamber','Physical adjacent chamber unlocks an extra grid column.')
 for id,source in crops.ART.items():mappings[source]=('native_adaptation','jimta_'+id,'Native source crop genetics, beverages and crop care; original pixels.')
 mappings['itemCropSeed']=('native_adaptation','jimta_seed_wheat','29 explicit source crop seed bags and stages; native three-stat genetics and reachable breeding graph, not exact legacy cropcard environment math.')
 mappings['blockCrop']=('native_adaptation','jimta_crop_cross_sticks','29 original crop varieties and crossbreeding trellis.')
 for old,id in shared.items():
  if id:mappings[old]=('consolidated_native',id,'Reuses existing Jugcraft resource; legacy registry ID and source art are not duplicated.')
 for name,source in services.DEVICES.items():mappings['block'+source]=('native_adaptation','jimta_'+name,'Original housing and GUI pixels. Native claimed transactional fluid routing, 72-tick finite solar distillation or 10:1 steam recovery matching the existing boiler. Configurable rate and redstone pause.')
 for name,source in services.TRANSFORMERS.items():mappings['blockTransformer'+source]=('native_adaptation',name,'Original housing and GUI, six-sided JE ports, automatic/split/combine control. Shared per-tick and per-face transactional budgets; no parallel EU voltage system.')
 for name,source in personal.DEVICES.items():mappings['block'+source]=('native_adaptation','jimta_'+name,'Original personal-machine art; safe has recovered six-box model and eased door. Private owner/party inventory or explicitly published finite barter/held-equipment charging; no hopper access.')
 for ident,source in infrastructure.DEVICES.items():mappings[source.split('/')[-1]]=('native_adaptation',ident,'Original infrastructure housing; native owned teleporter, defended Tesla, charge pad or magnetic lift behavior as recorded separately.')
 mappings['blockSortingMachine']=('native_adaptation','jimta_sorting_machine','Original atlas with existing nine-sample sorter, single directional destination and current claim protections; not the source six-independent-face GUI.')
 mappings['blockItemBuffer']=('native_adaptation','jimta_item_buffer','Original atlas with owned nine-slot shared chute cargo, configurable output routing and optional paid JE boost; no duplicate transport economy.')
 mappings['blockElectric']=('native_adaptation','jimta_bat_box','Metadata family: BatBox/MFE/MFSU/CESU original housings consolidate shared battery tiers; LV/MV/HV/EV native directional JE transformers are separate original-art blocks. Not legacy voltage explosions.')
 mappings['blockPersonal']=('native_adaptation','jimta_personal_safe','Metadata family: original personal safe, native barter station and finite energy vendor; explicitly publish customer access without exposing private stock.')
 mappings['blockMagnetizer']=('native_adaptation','jimta_magnetizer','Source housing; native contiguous loaded iron-fence lift up to twenty blocks each way, compatible metal boots, four JE per rider per tick and crouch descent. Claims/redstone enforced; no source metadata charge simulation.')
 mappings['fluidDistilledWater']=('native_adaptation','distilled_water','Original flowing/still sprites and finite solar/steam recovery; native boiler input and one-lapis coolant route.')
 mappings['itemCoin']=('native_adaptation','jimta_iron_coin','Original iron coin sprite; conserved nine-token/one-iron conversion, barter only, no financial balance minting.')
 mappings['itemAshes']=('native_adaptation','jimta_ashes','Original ash icon; each consumed solid-fuel item in the Stirling engine reserves one real ash output. No legacy combustion probability claim.')
 for old,material in [('Lead','lead'),('AdvIron','steel'),('Bronze','bronze'),('Copper','copper'),('Tin','tin'),('Silver','silver'),('Iron','iron'),('Gold','gold'),('Sulfur','sulfur'),('Lithium','lithium'),('Coal','coal'),('Obsidian','obsidian'),('Lapi','lapis')]:
  for prefix,suffix in [('itemIngot','_ingot'),('itemDust','_dust'),('itemPlate','_plate')]:
   candidate=material+suffix
   if (ROOT/f'src/main/resources/assets/jugcraft/items/{candidate}.json').exists():mappings[prefix+old]=('consolidated_native',candidate,'Existing native material; corresponding processing uses shared recipes.')
 for old,material in [('Copper','copper'),('Gold','gold'),('Iron','iron'),('Tin','tin'),('Silver','silver'),('Lead','lead')]:
  mappings['itemPurifiedCrushed'+old+'Ore']=('consolidated_native','washed_'+material+'_ore','Native ore washing/thermal centrifuge batch chain.')
  mappings['itemCrushed'+old+'Ore']=('consolidated_native',material+'_dust','Native crushing route, not a duplicate crushed-ore material.')
 # Verified aliases and metadata tokens are recorded separately from gameplay still missing.
 for old,native in {
  'itemBatREDischarged':'jimta_re_battery','itemNanoSaberOff':'jimta_nano_saber',
  'blockOreUran':'uranium_ore','blockreactorvessel':'jimta_reactor_vessel',
  'itemCrushedUranOre':'uranium_dust','itemPurifiedCrushedUranOre':'washed_uranium_ore',
  'itemFuelCoalDust':'jimta_coal_dust','itemDustClay':'clay_powder',
  'itemIngotCopper':'minecraft:copper_ingot','itemLithium':'lithium_carbonate',
  'itemironshaft':'precision_shaft','itemsteelshaft':'precision_shaft',
 }.items():mappings[old]=('consolidated_native',native,'Native consolidated route; original metadata names are not extra independent resources. Consult the integration ledger for deliberately different native recipes.')
 for old in ('itemCellWater','itemCellLava','itemCellDistilledWater','itemCellCoolant','itemCellHotCoolant','itemCellSteam','itemCellSuperheatedSteam','itemCellBiogas','itemCellBiomass','itemCellCoal','itemCellUuMatter'):
  mappings[old]=('consolidated_native','jimta_fluid_cell','Real conserved universal cell with saved fluid contents and original fill-state sprites. Coal cell uses the existing crude-oil fluid.')
 for old,native in {'overclockerUpgrade':'overclocker','transformerUpgrade':'transformer','energyStorageUpgrade':'energy_storage','ejectorUpgrade':'ejector','fluidEjectorUpgrade':'fluid_ejector','redstoneinvUpgrade':'redstone_inverter','pullingUpgrade':'pulling'}.items():
  candidate='jimta_'+native+'_upgrade'
  if (ROOT/f'src/main/resources/assets/jugcraft/items/{candidate}.json').exists():mappings[old]=('consolidated_native',candidate,'Metadata token for the already implemented removable processor upgrade.')
 metadata=set('black red green brown blue purple cyan lightGray gray pink lime yellow lightBlue magenta orange white reinforced tap wet dry stick upgraded top bottom open off active activeAlternate'.split())
 # Explicit dropped migration names in the supplied BlocksItems.initMigration.
 # These old save IDs are not unfinished survival items; replacements are listed
 # separately where a real native resource or universal cell already exists.
 for old in ('itemCellAir','itemCellWaterElectro','itemCellCoalRef','itemDustIronSmall','itemDustBronze'):
  if old not in mappings:mappings[old]=('deprecated_source_id',None,'Explicitly retired by supplied BlocksItems.initMigration; not a current standalone source item. Do not manufacture an unusable survival entry for this legacy save ID.')
 mappings['itemContainmentbox']=('native_adaptation','jimta_containment_box','Original item pixels and 4x3 GUI; twelve slots restricted to real radioactive materials, preserved components, no nesting, and 99% attenuation in shared radiation accounting.')
 mappings['obscurator']=('native_adaptation','jimta_obscurator','Original icon; charged solid-block sampling (20000 JE) and existing facade reskin (5000 JE). Whole sampled-block appearance rather than source per-face pixel pattern; no block/inventory duplication.')
 mappings['plasmaLauncher']=('native_adaptation','jimta_plasma_launcher','Original animated icon and 40000-JE capacity. Native 32-block thermal pulse for 1000 JE: one-to-one block-only furnace transformation, water evaporation, or authorized creature hit. No source radial fire/area heat explosion; no container or ore-to-item conversion. Not unrestricted source particle parity.')
 for old,native in {'itemDynamite':'jimta_dynamite','itemDynamiteSticky':'jimta_sticky_dynamite','itemRemote':'jimta_dynamite_remote','blockDynamite':'jimta_dynamite_charge','blockDynamiteRemote':'jimta_dynamite_charge'}.items():
  mappings[old]=('native_adaptation',native,'Original item/block pixels. Thrown and sticky four-second charges; unlit six-face placed charge, manual/redstone ignition, sixteen UUID-bound owner-only remote links with 64-block loaded range and two-second remote fuse. Bounded protected blast excludes inventories/fluids/high resistance. Remote block consolidates to a saved linked state. Bound stacks support dispensers with current online-owner and source/muzzle claim checks, blocked-muzzle refusal, and one consumed item only after successful spawn.')
 mappings['blockHarz']=('native_adaptation','jimta_resin_sheet','Original resin texture and 1/8-block outline, non-solid supported sheet with native sticky slowing and fall-distance damping. One latex bottle crafts one sheet and returns its glass bottle. Native deterministic sheet recovery replaces source random resin loss; no free filled-bottle refund.')
 mappings['blockMetal']=('consolidated_native','bronze_block','All six source storage variants consolidate to existing copper, tin, bronze, uranium, lead and steel blocks and their native compression/unpacking recipes. No duplicate resource blocks or radiation-free uranium variant.')
 mappings['blockRubber']=('native_adaptation','jimta_rubber_trampoline','Original rubber texture, two-pixel sheet geometry and original bounce sound. Native 0.8 restitution with crouch suppression and safe normal landings; old unbounded lateral support scans, collapse and jump amplification are not ported.')
 mappings['itemMFSUupgradekit']=('native_adaptation','jimta_mfsu_upgrade_kit','Original icon; in-place native MFE and MFE charge-pad upgrade retains charge, facing, contents, ownership and policy. Old cached inventory/energy cleared; refusal restores the original. Native capacities remain shared JE tiers.')
 # Equipment material tokens are aliases of already registered wearable families,
 # not nineteen more unimplemented armor sets.
 for token in ('alloy','batpack','bronze','hazmat','jetpack','lappack','energypack','advbatpack','nano','nightvision','quantum','rubber','solar'):
  members=[ident for ident,row in gear.GEAR.items() if row.get('asset')==token]
  if members:mappings[token]=('source_metadata_token',members[0],'Equipment material/texture-family token, implemented by '+', '.join(members)+'. Not an additional standalone item or recipe.')
 mappings['itemBoat']=('native_adaptation','jimta_rubber_boat','Three original rubber/carbon/electric hull skins on native owned boats, electric acceleration spends equipped JE; broken rubber boat repair is separately registered. Source metadata family, not one missing boat item.')
 mappings['itemDustLithium']=('consolidated_native','lithium_carbonate','Lithium processing remains the existing carbonate/electrolyte/cathode chain; no second incompatible elemental-lithium material is introduced. Original small-pile sprite represents one ninth of native lithium carbonate.')
 for family,members in {
  'itemDust':['itemDustBronze','itemDustClay','itemDustCoal','itemDustCopper','itemDustGold','itemDustIron','itemDustSilver','itemDustTin','itemFuelCoalDust','itemDustStone','itemDustLead','itemDustObsidian','itemDustLapi','itemDustSulfur','itemDustLithium'],
  'itemDust2':['itemDustsilicondioxide','itemDustdiamond','itemDustenergium','itemAshes'],
  'itemDustSmall':['itemDust'+source+'Small' for source,_ in industry.SMALL_DUSTS.values()],
  'blockHeatGenerator':['blockSolidHeatGenerator', 'blockFluidHeatGenerator', 'blockRTHeatGenerator', 'blockElectricHeatGenerator'],
  'blockKineticGenerator':['blockWindKineticGenerator', 'blockSteamKineticGenerator', 'blockElectricKineticGenerator', 'blockManualKineticGenerator', 'blockWaterKineticGenerator', 'blockStirlingKineticGenerator'],
  'blockMachine2':['blockTeleporter', 'blockTesla', 'blockCropmatron', 'blockCentrifuge', 'blockMetalFormer', 'blockOreWashingPlant', 'blockPatternStorage', 'blockScanner', 'blockReplicator', 'blockSolidCanner', 'blockFluidBottler', 'blockAdvMiner', 'blockLiquidHeatExchanger', 'blockFermenter', 'blockFluidRegulator', 'blockCondenser'],
  'blockMachine3':['blockSteamGenerator', 'blockBlastFurnace', 'blockBlockCutter', 'blockSolarDestiller', 'blockFluidDistributor', 'blockSortingMachine', 'blockItemBuffer', 'blockCropHavester', 'blockLathe'],
  'itemIngot':['itemIngotCopper','itemIngotTin','itemIngotBronze','itemIngotAdvIron','itemIngotAlloy','itemIngotLead','itemIngotSilver'],
  'itemPlates':['itemPlateCopper','itemPlateTin','itemPlateBronze','itemPlateGold','itemPlateIron','itemPlateAdvIron','itemPlateLead','itemPlateObsidian','itemPlateLapi'],
  'itemCrushedOre':['itemCrushed'+metal+'Ore' for metal in ('Iron','Copper','Gold','Tin','Uran','Silver','Lead')],
  'itemPurifiedCrushedOre':['itemPurifiedCrushed'+metal+'Ore' for metal in ('Iron','Copper','Gold','Tin','Uran','Silver','Lead')],
  'itemupgradekit':['itemMFSUupgradekit'],
  'itemRecipePart':['itemPartCoil','itemPartEleMotor','itempowerunit','itempowerunitsmall','itemrawcrystalmemory','itemheatconductor','itemcopperboiler','itemwoodrotorblade','itemironrotorblade','itemcarbonrotorblade','itemsteelrotorblade','itemironshaft','itemsteelshaft'],
  'itemCasing':['itemCasing'+source for source in industry.CASING_METALS.values()],
  'itemDensePlates':['itemDensePlate'+source for source in industry.DENSE_METALS.values()],
 }.items():
  resolved=[name for name in members if name in mappings]
  if len(resolved)==len(members):mappings[family]=('source_metadata_token',mappings[members[0]][1],'Metadata container, not another standalone item. All declared members map separately: '+', '.join(members)+'. Source-specific native recipe differences are retained in those records.')
 records=[];path=SRC/'industrialcraft reborn by jimbo/ic2/core/init/InternalName.class'
 for access,name,descriptor in fields(path):
  if not access&0x4000:continue
  status,id,note=mappings.get(name,('source_metadata_token',None,'State/color suffix in InternalName, not a standalone block or item. Associated family behavior is tracked separately.') if name in metadata else ('unimplemented',None,'Requires source behavior/metadata mapping; no functional parity claim.'))
  records.append({'collection':'IndustrialCraft','source_registry':name,'source_class':path.relative_to(ROOT).as_posix(),'status':status,'native_id':(('minecraft:'+id) if id=='diamond' else (id if ':' in id else 'jugcraft:'+id)) if id else None,'note':note})
 bc=SRC/'buildcraft reborn by jimbo'
 bcmap={
  'BCBuildersBlocks.frame':('jimta_factory_frame','Original core/arm model coordinates, inward faces and frame texture. Placeable connected structure with matching six-sided collision; existing quarry rendering remains separately owned.'),
  'BCCoreBlocks.decorated':('jimta_factory_panel_blueprint','All six source panel types retain original textures: blueprint, template, paper, leather, warning and laser back. Native reachable decorative blocks, without imaginary machine or destruction behavior.'),
  'BCCoreItems.fragileFluidShard':('jimta_fragile_fluid_shard','Original base/fluid layers; native craftable single-fill 500 mB shell. Sealed contents can only drain, and final extraction atomically consumes the shell. Shared tanks/bottler support partial transfers; native tanks retain their existing complete-fluid drop rather than spilling shards.'),
  'BCTransportItems.waterproof':('jimta_pipe_waterproofing','Original sprite. Native rubber/green-dye sealant and optional one-for-one item-to-fluid pipe conversion; stripping returns the pipe only. Existing direct recipes remain.'),
  'BCFactoryItems.plasticSheet':('plastic_sheet','Consolidates to the existing shared plastic sheet and polymer production chain; no duplicate sheet item or replacement of existing pixels.'),
  'BCCoreItems.gearWood':('jimta_wood_gear','Original source sprite and progressive ring crafting. Shared iron gear forms the middle tier; gold and diamond gears feed an optional classic quarry route.'),
  'BCCoreItems.gearStone':('jimta_stone_gear','Original source sprite and progressive ring crafting. Shared iron gear forms the middle tier; gold and diamond gears feed an optional classic quarry route.'),
  'BCCoreItems.gearGold':('jimta_gold_gear','Original source sprite and progressive ring crafting. Shared iron gear forms the middle tier; gold and diamond gears feed an optional classic quarry route.'),
  'BCCoreItems.gearDiamond':('jimta_diamond_gear','Original source sprite and progressive ring crafting. Shared iron gear forms the middle tier; gold and diamond gears feed an optional classic quarry route.'),
  'BCTransportItems.plugPowerAdaptor':('jimta_pipe_power_adaptor','Original plug mesh/UV/pixels with native connecting stem. A power-only face on wood/diamond-wood item/fluid extractors; 2048 JE buffer, bounded 4x boost, 8 JE per extra item or 25 mB actually moved. Unpowered base extraction remains. Charge stays with the removed fitting.'),
  'BCTransportItems.plugBlocker':('jimta_pipe_blocker','Original mesh/UV/pixels with native connector stem; saved six-face blockers on factory conduits and active extraction/filter/disposal endpoints. Closed arms and real edges; cached item ports/views and direct intake/output recheck fittings.'),
  'BCSiliconItems.plugLens':('jimta_lens_clear','Original lens mesh/UV/pixels; 17 colored/clear lenses and 17 filters on through-routing pipes including lapis/daizuli/emzuli. Exit/entry transforms route-local colors; no lens on buffered endpoints and no modification of cargo.'),
  'BCSiliconItems.plugFacade':('jimta_facade','Shared sampled covers plus explicit solid/hollow factory variants and component-preserving one-item conversion. Solid seals item/fluid/JE edges and cached ports; hollow leaves an eight-pixel opening. Legacy covers retain existing routing. Two-state sampled/transparent appearance uses adjacent native colored control relays, with persisted phase components and unchanged physical routing; native procedural geometry, not original facade mesh parity.'),
  'BCFactoryItems.waterGel':('jimta_water_gel_seed','Original seed sprite; water-source use starts finite, owner-bound, claim-aware spreading and setting.'),
  'BCFactoryItems.gelledWater':('jimta_gelled_water','Original gel sprite; one per broken gel block, craft with a bucket to recover water.'),
  'BCFactoryBlocks.waterGel':('jimta_water_gel','All seven original stage textures. Bounded 64-position spread scan, at most three sources per stage, twelve-block origin radius, loaded-only and owner permissions. Source random bonus drops omitted.'),
  'BCBuildersBlocks.library':('jimta_blueprint_library','Original library housing; existing shared native blueprint browser, import and print, with the same access checks.'),
  'BCRoboticsBlocks.zonePlanner':('jimta_zone_planner','Original planner housing; sixteen saved bounded civil-workshop zones, clickable map preview, marker copy and remove, owner/party access.'),
  'BCFactoryBlocks.heatExchange':('jimta_factory_heat_start','Original multipart start/middle/end geometry; two finite fluid circuits, 1-3 sections, native coolant-to-steam heat budget, reserved outputs, claims and redstone pause.'),
  'BCCoreBlocks.powerTester':('jimta_power_tester','Original power-test cube texture, finite 1M JE sink, committed-only last-tick/total accounting, saved counters and redstone pause including cached ports. Native JE, not a second MJ network.'),
  'BCLibItems.debugger':('jimta_factory_debugger','Original debugger sprite, permitted per-player block/JE inspection and civil-machine status, plus power tester readings. No global debug target or reflective class dumping.'),
  'BCCoreItems.list':('jimta_factory_list','Original empty/full sprites; nine exact item-ID samples, toggled from the other hand without consuming or creating items. Functions in native item sorters, extraction pipes and Nouveau filters. No tag or source ore-dictionary matching modes.'),
  'BCCoreItems.mapLocation':('jimta_map_location','Original spot/area/path icons; dimension-bound location, two-corner workshop area and sixteen-point survey notebook. Carrier/delivery/fluid-carrier robots follow the surveyed path forward to output and backward to input, with loaded/claim/collision checks.'),
  'BCCoreItems.paintbrush':('jimta_paintbrush','Original clean/16-color brush sprites; one dye loads 64 uses. Factory pipe paint separates unlike colors across item/fluid/JE routes and cached ports. Unpainted junctions bridge colors. Insulated JE cable paint remains cosmetic; vanilla decorative color families can be repainted with preserved state. No traveling-item simulation.'),
  'BCCoreItems.goggles':('jimta_factory_goggles','Original item pixels; wearable zero-armor tool with authorized nearby JE/fluid readout. No original separate worn mesh was supplied.'),
  'BCSiliconBlocks.chargingTable':('jimta_charging_table','Original six-part table mesh and supplied GUI; real finite JE transfer to one chargeable item, bottom extraction only after full charge, original laser or cable supply.'),
  'BCSiliconBlocks.programmingTable':('jimta_programming_table','Original table mesh/GUI; twenty clickable native robot programs, one consumed blank/old card and 2000 JE per output, sided automation and owner/party access.'),
  'BCSiliconBlocks.advancedCraftingTable':('jimta_advanced_crafting_table','Original table mesh/textures; existing native auto-crafting system, recipes/materials/automation, plus workshop laser power.'),
  'BCSiliconBlocks.laser':('jimta_workshop_laser','Original emitter mesh and eight-frame beam, bounded 16-block line-of-sight transactional JE transfer to assembly/integration tables, claims and redstone pause.'),
  'BCSiliconBlocks.integrationTable':('jimta_integration_table','Original table mesh, native shared circuit/gate production recipes and laser or wired JE; arbitrary source expansion-chip metadata is not claimed.'),
  'BCSiliconBlocks.assemblyTable':('jimta_assembly_table','Original source table mesh/UV/textures with native JE circuit processing, recipe picker and sided automation; bounded native laser JE transport supports this table.'),
  'BCSiliconItems.redstoneChipset':('jimta_chipset_red','Five original chipset sprites; real circuit assembly and gate/electronics recipes.'),
  'BCBuildersBlocks.filler':('jimta_civil_filler','Original source block faces; bounded material filling through native protection.'),
  'BCBuildersBlocks.replacer':('jimta_civil_replacer','Conserves removed block drops and paid replacement materials; no block-entity replacement.'),
  'BCBuildersBlocks.quarry':('jimta_civil_quarry','Original machine faces; marked bounded area, real tool wear, finite JE and output inventory. Recovered source rail/drill geometry follows actual native work positions.'),
  'BCFactoryBlocks.miningWell':('jimta_civil_mining_well','Original machine faces; protected single-column mining within marked vertical range.'),
  'BCFactoryBlocks.floodGate':('jimta_civil_flood_gate','Enclosed marked water/lava reservoir, finite fluid and JE; no open uncontrolled flooding.'),
  'BCEnergyItems.globOil':('jimta_oil_glob','Original oil-glob icon. Native reversible eight globs per crude-oil bucket, with bucket remainder conserved. Supplied class scan found registration only; this is an explicit native use, not a legacy recipe claim.'),
  'BCBuildersItems.addonFillerPlanner':('jimta_filler_planner','Original planner cube and retained native nine-mode/inversion program, applied to paused authorized fillers and saved volumes.'),
  'BCCoreBlocks.markerVolume':('jimta_volume_beacon','Original placed marker mesh and pixels; notebook selects volume mode and feeds native machine work-area selection. Existing handheld corner selector remains compatible.'),
  'BCCoreBlocks.markerPath':('jimta_path_beacon','Original placed path marker mesh and pixels; ordered notebook waypoints drive real carrier routes, loaded and claim checked.'),
  'BCCoreBlocks.spring':('pumpjack','Explicit native consolidation: supplied water/oil spring is an unbreakable world-generation marker rendered as vanilla bedrock, not a craftable machine. Water remains renewable through vanilla source water and native pumps; oil uses existing finite seed-based OilReservoirs and the pumpjack/fracking chain. Infinite source oil and the old source-specific pumping advancement are intentionally not ported. No unique spring texture is omitted.'),
  'BCCoreItems.volumeBox':('jimta_volume_box','Original volume-box mesh; owned world-saved corners, consumer locking and connector draft/confirm/cancel.'),
  'BCFactoryBlocks.tube':('jimta_factory_pump','Source internal pump tube is integrated into real paid pump scan rendering/retraction; not an independent placeable survival tube.'),
  'BCTransportBlocks.pipeHolder':('jimta_pipe_stone_item','Source multipart pipe-holder container is represented by native registered pipe blocks with persistent fittings, actual item/fluid/JE routing and wire channels.'),
  'BCCoreItems.markerConnector':('jimta_marker_connector','Original connector icon; transient aim-following edits to selected owned world-saved volumes, explicit commit/cancel, loaded/claim/consumer locks and stale-edit rejection.'),
  'BCBuildersItems.snapshot':('blueprint','Existing protected blueprint capture; not exact legacy snapshot format.'),
  'BCBuildersItems.schematicSingle':('blueprint','Existing bounded blueprint placement, with original robot builder support.'),
  'BCBuildersBlocks.architect':('blueprint_table','Native blueprint capture table; original legacy architect GUI not imported.'),
  'BCBuildersBlocks.builder':('jimta_civil_builder','Original stationary builder art, real materials and existing native blueprints; bounded protected placement.'),
  'BCCoreItems.wrench':('brass_wrench','Existing machine/pipe wrench.'),
  'BCLibItems.guide':('jimta_factory_guide','Original guide art; native portable nine-topic instructions with persistent bookmark and note selection.'),
    'BCLibItems.guideNote':('jimta_factory_guide_note','Original note art; persistent selectable topic, portable reader and guide bookmark. Native instructions replace incompatible legacy recipe text.'),
    'BCTransportItems.wire':('jimta_pipe_wire_red','Sixteen source wire icons and fitted skins; paid removable conductors on saved pipe fittings, color-intersecting native control traversal, finite loaded/claim-bounded network. No legacy strength simulation.'),
    'BCCoreItems.gearIron':('iron_gear','Shared native gear resource.'),
  'BCFactoryBlocks.autoWorkbenchItems':('jimta_auto_workbench','Original workbench art on native auto crafting with real ingredients.'),
  'BCFactoryBlocks.pump':('jimta_factory_pump','Original housing and tube texture; synchronized extending intake tube. 100 mB per five-tick scan pulse, 8 JE per productive pulse; scans a loaded/protected 32-block column and bounded connected pool. Native renewable water, finite lava/other liquid consumption.'),
  'BCFactoryBlocks.tank':('jimta_glass_tank','Original narrow glass mesh and lower-join skin, native 16-bucket storage/group/comparator and fluid-preserving item. Shared tank traversal respects loaded chunks and claim boundaries.'),
  'BCFactoryBlocks.chute':('jimta_chute','Original funnel and connected outlet geometry/GUI; nine real cargo slots, directional intake, selectable round-robin outputs, actual item collection and bounded 1-item/second or 16-item/4-ticks paid boost. Claims and redstone pause; native instantaneous pipes.'),
  'BCCoreBlocks.engine':('jimta_redstone_engine','Five source engine variants and original triangular piston geometry/UVs. Native JE starter/solid-fuel/fluid-fuel/electric/creative behavior, finite buffers, shared transactional throughput and cooled combustion shutdown; no legacy MJ/RF economy or explosions.'),
  'BCEnergyBlocks.mjDynamo':('jimta_kinetic_dynamo','Original dynamo moving plate, chamber and heat-stage skins. Native bounded 64 KE/t shaft input at 85% JE conversion with saved remainder; not legacy RF/MJ conversion.'),
  'BCFactoryBlocks.distiller':('distillation_tower','Shared native refinery; source recipe family not automatically considered equivalent.'),
  'BCSiliconItems.gateCopier':('jimta_gate_copier','Original empty/full tool art, bounded rule-only copy/apply, compatible gate tiers/logic and shared claims. Never copies owner or access rights.'),
  'BCSiliconItems.plugGate':('jimta_gate_gold_and','8 gate bodies; removable lapis/quartz/diamond modifiers with source parameter capacities and row divisor, five original-art expansions, native conditions/output channels and button-only parameter editor. No claim of the complete legacy trigger/action API.'),
  'BCSiliconItems.plugLightSensor':('jimta_light_sensor','Original plug geometry; ambient-light readout and redstone/data output.'),
  'BCSiliconItems.plugTimer':('jimta_timer','Original plug geometry; adjustable 1–60s sawtooth timer on native data network.'),
  'BCSiliconItems.plugPulsar':('jimta_pulsar','Original plug geometry; adjustable periodic redstone/data pulse.'),
  'BCTransportBlocks.filteredBuffer':('jimta_item_filter','Original source filter geometry and native nine-sample filtering.'),
 }
 for path in sorted(bc.rglob('BC*Items.class'))+sorted(bc.rglob('BC*Blocks.class')):
  for access,name,descriptor in fields(path):
   if not access&8 or not re.search(r'(Item|Block)',descriptor) or name.startswith('$'):continue
   family=path.stem+'.'+name;mapped=bcmap.get(family)
   pipe=re.fullmatch(r'pipe(Item|Fluid|Power|Rf)(Wood|Stone|Cobble|Quartz|Gold|Sandstone|Iron|Diamond|DiaWood|Clay|Void|Obsidian|Lapis|Daizuli|Emzuli|Stripes)',name) if path.stem=='BCTransportItems' else None
   if pipe:
    flow,material=pipe.groups();material={'Cobble':'cobblestone','DiaWood':'diamond_wood'}.get(material,material.lower());flow={'Item':'item','Fluid':'fluid','Power':'power','Rf':'power'}[flow]
    candidate='jimta_pipe_'+material+'_'+flow
    if candidate in transport.items():mapped=(candidate,'Legacy Item field aliases the corresponding PipeDefinition; the same native pipe implements both entries. See transport record for exact behavior.')
   if family=='BCTransportItems.pipeStructure':mapped=('jimta_pipe_structure','Alias of BCTransportPipes.structure; shared native control/data line.')
   id=mapped[0] if mapped else None
   if id and not (ROOT/f'src/main/resources/assets/jugcraft/items/{id}.json').exists() and not (ROOT/f'src/main/resources/assets/jugcraft/blockstates/{id}.json').exists():mapped=None;id=None
   unused=family=='BCTransportItems.pipeFluidObsidian'
   records.append({'collection':'BuildCraft','source_registry':family,'source_class':path.relative_to(ROOT).as_posix(),'source_type':descriptor,'status':'unused_source_declaration' if unused else 'native_adaptation' if mapped else 'unimplemented','native_id':'jugcraft:'+id if id else None,'note':'No corresponding PipeDefinition is instantiated in the supplied preInit; no fluid collector texture or behavior is supplied.' if unused else mapped[1] if mapped else 'Registry field exists; explicit native behavior/art mapping pending. Metadata variants may expand this family.'})
 recipes=[]
 # PipeDefinition fields were absent from the older Item/Block-only inventory. Track them explicitly.
 pipe_source=bc/'buildcraft/transport/BCTransportPipes.class'
 for access,name,descriptor in fields(pipe_source):
  if not access&8 or 'PipeDefinition;' not in descriptor:continue
  match=re.fullmatch(r'(wood|stone|cobble|quartz|gold|sandstone|iron|diamond)(Item|Fluid|Power|Rf)',name)
  native=None;note='Distinct routing/collector/filter/color/void behavior still pending; not a complete source pipe implementation.'
  if match:
   material,flow=match.groups();material='cobblestone' if material=='cobble' else material;flow={'Item':'item','Fluid':'fluid','Power':'power','Rf':'power'}[flow]
   candidate='jimta_pipe_'+material+'_'+flow
   if candidate in transport.items():native=candidate;note='Original pipe skin with bounded shared native item/fluid/JE transport; see feature record for directional/extraction/filter behavior and intentional source differences. RF and MJ consolidate to JE.'
  special={'structure':'structure','clayItem':'clay_item','clayFluid':'clay_fluid','voidItem':'void_item','voidFluid':'void_fluid','diaWoodPower':'diamond_wood_power','diaWoodRf':'diamond_wood_power','obsidianItem':'obsidian_item','stripesItem':'stripes_item','lapisItem':'lapis_item','daizuliItem':'daizuli_item','emzuliItem':'emzuli_item','diaWoodItem':'diamond_wood_item','diaWoodFluid':'diamond_wood_fluid'}
  if name in special:
   native='jimta_pipe_'+special[name];note='Original pipe skin with explicit native collection/placement, color routing, filtered extraction, control, priority, disposal or JE behavior; consult feature record. Not a claim of legacy travelling simulation, full arbitrary item interactions or voltage conversion.'
  if name=='obsidianFluid':note='Declared field only: the supplied BCTransportPipes.preInit never constructs this definition. No obsidian fluid texture or collector behavior exists in the supplied collection.'
  records.append({'collection':'BuildCraft','source_registry':'BCTransportPipes.'+name,'source_class':pipe_source.relative_to(ROOT).as_posix(),'status':'unused_source_declaration' if name=='obsidianFluid' else 'native_adaptation' if native else 'unimplemented','native_id':'jugcraft:'+native if native else None,'note':note})
 for path in sorted((SRC/'industrialcraft reborn by jimbo/assets/ic2/config').glob('*.ini')):
  if path.stem in ('general','uu_scan_values'):continue
  pending='';line0=0
  for line,text in enumerate(path.read_text(errors='replace').splitlines(),1):
   stripped=text.strip()
   if not stripped or stripped.startswith(';'):continue
   if not pending:line0=line
   pending+=stripped.lstrip('\\')
   if pending.endswith('\\'):pending=pending[:-1];continue
   names=re.findall(r'IC2:([A-Za-z0-9_]+)',pending)
   mapped={name:mappings[name][1] for name in names if name in mappings}
   recipes.append({'source':path.relative_to(ROOT).as_posix(),'line':line0,'text':pending,'status':'ingredients_mapped_recipe_not_asserted' if names and len(mapped)==len(set(names)) else 'unresolved_source_recipe','known_native_ingredients':mapped,'unresolved_legacy_ingredients':sorted(set(names)-set(mapped))})
   pending=''
 payload={'scope':'Static source registry families and every non-comment recipe statement. Mapped ingredients do not mean the entire source recipe or metadata family is ported. Generated native additions are listed separately.','registries':records,'recipes':recipes,'native_additions':{'industrial_machines':sorted(industry.MACHINES),'industrial_materials':sorted(industry.MATERIALS),'industrial_gear':sorted(gear.items()),'robotics':sorted(robots.items()),'crops':sorted(crops.items()),'automation':sorted(automation.items()),'production':sorted(production.items()),'reactor':sorted(reactor.items()),'matter_and_packaging':sorted(matter.items()),'civil_workshops':sorted(civil.items()),'infrastructure':sorted(infrastructure.items()),'construction':sorted(construction.items()),'machine_upgrades':sorted(upgrades.items()),'heat_and_kinetics':sorted(heat.items())},'validation':'Local implementation is in combined validation; do not infer gameplay completion from registry mappings. See docs/features/jimtafacts-b-h-local.md for current evidence.'}
 payload['native_additions']['transport']=sorted(transport.items())
 payload['native_additions']['workshop_tables']=sorted(tables.items())
 payload['native_additions']['workshop_utilities']=sorted(utilities.items())
 payload['native_additions']['industrial_cables']=sorted(cables.items())
 payload['native_additions']['terraforming']=sorted(terraforming.items())
 payload['native_additions']['fluid_and_power_services']=sorted(services.items())
 payload['native_additions']['personal_workshops']=sorted(personal.items())
 payload['native_additions']['engines']=sorted(engines.items())
 payload['native_additions']['factory_housings']=sorted(factory.items())
 OUT.mkdir(parents=True,exist_ok=True);(OUT/'industry-source-inventory.json').write_text(json.dumps(payload,indent=2)+'\n',encoding='utf-8')
 counts={status:sum(r['status']==status for r in records) for status in sorted({r['status'] for r in records})}
 return len(records),len(recipes),counts
if __name__=='__main__':print(generate())
