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
 mappings={}
 for id,row in gear.GEAR.items():
  mappings[row['icon'].replace('.off','')]=('native_adaptation',id,'Original art and shared JE/tool/armor behavior; see industrial integration record for differences.')
 mappings.update({old:('native_adaptation',id,'Shared original-art industrial material chain') for old,id in {'itemPartCarbonFibre':'jimta_carbon_fibre','itemPartCarbonMesh':'jimta_carbon_mesh','itemPartCarbonPlate':'jimta_carbon_plate','itemPartCoalBall':'jimta_coal_ball','itemPartCoalBlock':'jimta_compressed_coal_ball','itemPartCoalChunk':'jimta_coal_chunk','itemPartIndustrialDiamond':'diamond'}.items()})
 shared={'itemRubber':'rubber','itemPartCircuit':'basic_circuit','itemPartCircuitAdv':'advanced_circuit','itemPartEleMotor':'electric_motor','itemPartCoil':'copper_wire','itemHarz':'latex_bottle','itemOreIridium':None,'itemFertilizer':'fertilizer','fluidBiogas':'biogas','fluidBiomass':'biomass','fluidDistilledWater':'distilled_water','blockOreCopper':'minecraft:copper_ore','blockOreTin':'tin_ore','blockOreLead':'lead_ore','blockRubWood':'rubber_log','blockRubSapling':'rubber_tree_sapling','blockRubLeaves':'rubber_leaves','blockBasalt':'minecraft:basalt'}
 for id,row in industry.MACHINES.items():
  if row[2]:mappings[row[2].split('/')[-1]]=('native_adaptation',id,'Original source atlas; native JE/processing recipe picker and automation. No claim that all legacy GUI modes or recipes are identical.')
 for id,(_,source) in industry.MATERIALS.items():
  if not source.startswith('bc:'):mappings[source.split('/')[-1]]=('native_adaptation',id,'Original material sprite; reachable shared-machine/crafting route and downstream component use. Source metadata aliases may reference the same item.')
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
 mappings['itemFluidCell']=('native_adaptation','jimta_fluid_cell','Single conserved universal fluid cell uses native fluid component and transactions; original filled skins selected by fluid.')
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
 mappings['blockTerra']=('native_adaptation','jimta_civil_terraformer','Original active/inactive atlas; six reusable original-art plans, bounded marked-area soil/crop/snow/flattening operations, conserved supplies and water, 4096 JE per actual change, existing claim and container safeguards. No random biome-ID rewriting.')
 mappings['itemArmorJetpack']=('native_adaptation','jimta_biogas_jetpack','Original jetpack icon and worn layer, source 30000 mB biogas capacity, native shared flight controls at 2 mB per thrust tick; transactional tank filling and no JE charging.')
 mappings['itemArmorCFPack']=('native_adaptation','jimta_construction_foam_pack','Original icon and source-selected batpack armor; 800 finite foam block charges loaded from actual partly-used canisters, chest-slot supply to both native foam sprayers. Canister-unit adaptation, not a separate CF fluid.')
 for mode in terraforming.MODES:
  mappings['itemTFBP'+('' if mode=='blank' else mode.title())]=('native_adaptation','jimta_terraform_'+mode,'Original reusable terraforming blueprint in the selected-area native workshop; no plan consumed per operation.')
 for id,row in reactor.PARTS.items():mappings[row['source']]=('native_adaptation',id,'Original compact reactor component art and source tuning; bounded native JE simulation, fail-safe shutdown instead of terrain explosion.')
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
 mappings['blockElectric']=('native_adaptation','jimta_bat_box','Metadata family: BatBox/MFE/MFSU/CESU original housings consolidate shared battery tiers; LV/MV/HV/EV native directional JE transformers are separate original-art blocks. Not legacy voltage explosions.')
 mappings['blockPersonal']=('native_adaptation','jimta_personal_safe','Metadata family: original personal safe, native barter station and finite energy vendor; explicitly publish customer access without exposing private stock.')
 mappings['blockMagnetizer']=('native_adaptation','jimta_magnetizer','Source housing; native contiguous loaded iron-fence lift up to twenty blocks each way, compatible metal boots, five JE per five-tick rider pulse and crouch descent. Claims/redstone enforced; no source metadata charge simulation.')
 mappings['fluidDistilledWater']=('native_adaptation','distilled_water','Original flowing/still sprites and finite solar/steam recovery; native boiler input and one-lapis coolant route.')
 mappings['itemAshes']=('native_adaptation','jimta_ashes','Original ash icon; each consumed solid-fuel item in the Stirling engine reserves one real ash output. No legacy combustion probability claim.')
 for old,material in [('Lead','lead'),('AdvIron','steel'),('Bronze','bronze'),('Copper','copper'),('Tin','tin'),('Silver','silver'),('Iron','iron'),('Gold','gold'),('Sulfur','sulfur'),('Lithium','lithium'),('Coal','coal'),('Obsidian','obsidian'),('Lapi','lapis')]:
  for prefix,suffix in [('itemIngot','_ingot'),('itemDust','_dust'),('itemPlate','_plate')]:
   candidate=material+suffix
   if (ROOT/f'src/main/resources/assets/jugcraft/items/{candidate}.json').exists():mappings[prefix+old]=('consolidated_native',candidate,'Existing native material; corresponding processing uses shared recipes.')
 for old,material in [('Copper','copper'),('Gold','gold'),('Iron','iron'),('Tin','tin'),('Silver','silver'),('Lead','lead')]:
  mappings['itemPurifiedCrushed'+old+'Ore']=('consolidated_native','washed_'+material+'_ore','Native ore washing/thermal centrifuge batch chain.')
  mappings['itemCrushed'+old+'Ore']=('consolidated_native',material+'_dust','Native crushing route, not a duplicate crushed-ore material.')
 records=[];path=SRC/'industrialcraft reborn by jimbo/ic2/core/init/InternalName.class'
 for access,name,descriptor in fields(path):
  if not access&0x4000:continue
  status,id,note=mappings.get(name,('unimplemented',None,'Requires source behavior/metadata mapping; no functional parity claim.'))
  records.append({'collection':'IndustrialCraft','source_registry':name,'source_class':path.relative_to(ROOT).as_posix(),'status':status,'native_id':(('minecraft:'+id) if id=='diamond' else (id if ':' in id else 'jugcraft:'+id)) if id else None,'note':note})
 bc=SRC/'buildcraft reborn by jimbo'
 bcmap={
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
  'BCCoreItems.markerConnector':('jimta_volume_marker','Two-corner selector binds bounded source civil machines.'),
  'BCBuildersItems.snapshot':('blueprint','Existing protected blueprint capture; not exact legacy snapshot format.'),
  'BCBuildersItems.schematicSingle':('blueprint','Existing bounded blueprint placement, with original robot builder support.'),
  'BCBuildersBlocks.architect':('blueprint_table','Native blueprint capture table; original legacy architect GUI not imported.'),
  'BCBuildersBlocks.builder':('jimta_civil_builder','Original stationary builder art, real materials and existing native blueprints; bounded protected placement.'),
  'BCCoreItems.wrench':('brass_wrench','Existing machine/pipe wrench.'),
  'BCCoreItems.gearIron':('iron_gear','Shared native gear resource.'),
  'BCFactoryBlocks.autoWorkbenchItems':('jimta_auto_workbench','Original workbench art on native auto crafting with real ingredients.'),
  'BCFactoryBlocks.pump':('jimta_factory_pump','Original source static housing/textures on the shared electric pump. 100 mB/t, 8 JE per productive tick, below intake and native water-source/lava-consumption rules; not source distant retractable-tube scanning.'),
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
   if id and not (ROOT/f'src/main/resources/assets/jugcraft/items/{id}.json').exists():mapped=None;id=None
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
 payload={'scope':'Static source registry families and every non-comment recipe statement. Mapped ingredients do not mean the entire source recipe or metadata family is ported. Generated native additions are listed separately.','registries':records,'recipes':recipes,'native_additions':{'industrial_machines':sorted(industry.MACHINES),'industrial_materials':sorted(industry.MATERIALS),'industrial_gear':sorted(gear.items()),'robotics':sorted(robots.items()),'crops':sorted(crops.items()),'automation':sorted(automation.items()),'production':sorted(production.items()),'reactor':sorted(reactor.items()),'matter_and_packaging':sorted(matter.items()),'civil_workshops':sorted(civil.items()),'infrastructure':sorted(infrastructure.items()),'construction':sorted(construction.items()),'machine_upgrades':sorted(upgrades.items()),'heat_and_kinetics':sorted(heat.items())},'validation':'Not built or tested; final combined validation deferred by owner.'}
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
