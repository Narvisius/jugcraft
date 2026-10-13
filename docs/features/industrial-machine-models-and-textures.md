# Industrial machine models textures and dimensions

Status: owner-requested visual planning, 10 October 2026. The owner requests detailed models and textures for every unbuilt machine in the connected industrial plan, with each dimension between **2 and 6 blocks** and several exceptionally large installations. The geometry and sizes below are concrete proposed art specifications. They do not register blocks, implement processing, finalize recipes or claim in-game art verification.

Owner: jimbozoomer-byte. AI-assisted specification: OpenAI Codex, GPT-6 family.
Related plans: [industrial production map](industrial-chemistry-and-fuels-plan.md#delivery-and-remaining-decisions), [agriculture](industrial-agriculture-plan.md), [mineral refining](mineral-sands-and-refining-plan.md), [starter workshop](industrial-starter-workshop-plan.md), [steel](industrial-steel-and-bulk-metallurgy-plan.md), [recovery and pollution](waste-recycling-and-pollution-plan.md), [art direction](../ART_DIRECTION.md), [TODO](../TODO.md).

## Size and scope

Every size is **width × depth × height**, viewed from the machine's operating front. Width runs left to right, depth runs toward the rear, and height includes feet, hoppers, chimneys, guard rails and moving parts at their greatest reach. A 4×6×3 installation has a 4-by-6 floor footprint and is 3 blocks tall. Every proposed machine, large variant and separately modeled module stays within the 2–6 range on all three axes. Outside access space and connecting pipes are factory layout, not extra hidden machine dimensions.

This covers the unbuilt physical forms and future industrial variants in the linked plans: workshop/steel expansion, agricultural processing, mineral separation, chemistry/fuels, polymers/ceramics, electronics, storage and recovery. A process using shared equipment receives one shared body, rather than a separate compulsory machine for each acid, polymer, ore or residue. Later roadmap candidates have their own clearly marked section.

The source audit used [machine definitions](../../tools/machines.py), [current footprints](../../tools/large_machines.py), [MachineKind](../../src/main/java/io/github/jimbozoomer/jugcraft/machine/MachineKind.java), [kinetic registrations](../../src/main/java/io/github/jimbozoomer/jugcraft/kinetic/JugcraftKinetics.java) and [machine consolidation](machine-consolidation.md), checked against the planning PR's integration target. Existing cells, reactors, synthesis converters, refiners, polymerization, lithium/flow batteries, lithography, sawmills, presses, dynamos and cryogenic oxygen equipment already have runtime foundations. Their entries here describe **unbuilt entry forms, additional capabilities or larger physical versions**, not a claim that their current machines are absent. Current saved IDs and existing models are not resized by this document.

Hand tools, simple hand preparation, shafts, cables, conveyors, portable UI devices and already-built small equipment are not inflated into two-block machines. The proposed industrial rope station or powered spinning frame can coexist with hand craft. Gas pressure remains automatic; there is no compressor/pressure-tier machine roster.

## Shared visual language

**Mechanical workshops** use warm wood, firebrick, dark forged iron, bronze bearings and exposed but guarded shafts. **Steel-era processing** uses olive or field-grey housings, cool gunmetal, off-white ceramic, copper busbars, black hoses and amber instruments. **Power storage** uses graphite, light cell casings and restrained mint indicators. **Precision electronics** use graphite, cool ivory, cyan inspection panels and violet conduit accents. Tanks retain the owner's white/checker-band family. Materials, layout and mechanism identify a machine before its running lights do.

Make the silhouette readable from across a factory: stepped cylindrical vessels, thick press crossheads, inclined tables, long roller beds or paired tanks. Add closed pipe elbows, capped shafts, flanged collars, separately modeled motors, feet, handle grips, latches and instrument bezels. Fine mesh and close fins should be texture patterns; a few well-spaced structural ribs should be geometry. Do not cover every surface with bolts. The underside, back, hood interior and visible hopper walls must be finished.

Use a short palette of four or five deliberate tones per material. Broad panels carry calm fills, one clean seam or regularly spaced ribs, top-left highlights and bottom-right shadows. Place scuffs only at a handle, corner or loading lip. No sprinkled rust, fractal grime or random pixels across the whole machine. Hazard bands belong at the actual heat, pinch or electrical interface. Most panel/mechanism tiles can be 32×32 or 64×64; their shapes remain chunky and coherent beside Minecraft's world. Long tanks and cylinders use seamless fills; dials and end caps have their own correctly mapped faces.

A dark blue or cyan inspection pane can be an opaque window treatment, consistent with the current machine assets. Actual transparency needs the supported rendering path and modeled interior; do not rely on transparent pixels in an opaque material. Glows occupy only the lamp, hot slit or screen shape. Colour bands identify ports and controls rather than asserting a gas's real visible colour.

Where a large variant is listed, retain the same body, mechanism, textures and animation at increased scale; rounded block envelopes can contain the scaled geometry. For an existing family, carry forward its recognizable vessel/window/drive proportions. A new side module or furnace bay is explicitly part of the new assembly, not a wholesale replacement of the familiar core. Large moving mechanisms ease into motion and remain inside the stated envelope. Gauges communicate operation; they introduce no player pressure settings or routine maintenance.

## Design index

The specification contains **79 machine and module briefs**, including seven later roadmap candidates, and **23 additional larger-variant envelopes**. Each entry specifies the silhouette, major modeled parts, texture treatment, animation and interfaces. These are proposed construction envelopes for the art pass, not implemented multiblock layouts.

- [Mechanical workshop steel and ceramics](#mechanical-workshop-steel-and-ceramics)
- [Industrial agriculture textiles and finished materials](#industrial-agriculture-textiles-and-finished-materials)
- [Mineral separation refining and shared wet processing](#mineral-separation-refining-and-shared-wet-processing)
- [Gas chemistry fuels and reagent preparation](#gas-chemistry-fuels-and-reagent-preparation)
- [Polymer forming and precision electronics](#polymer-forming-and-precision-electronics)
- [Grid storage gas tanks and cryogenic installations](#grid-storage-gas-tanks-and-cryogenic-installations)
- [Recovery recycling and emissions treatment](#recovery-recycling-and-emissions-treatment)
- [Later industrial candidates with proposed art only](#later-industrial-candidates-with-proposed-art-only)

## Largest installations

| Machine form | Width × depth × height | Dominant shape |
| --- | --- | --- |
| Bulk Gas Holder | 6×6×6 | Massive stepped sphere on deep structural feet |
| Anaerobic Digester | 6×6×6 | Huge ribbed tank, upper service ring and sheltered feed gallery |
| Synthetic Fuel Reactor Train | 6×6×6 | Tall familiar synthesis core with condensers and a low manifold gallery |
| Modular Flow Battery Plant | 6×6×6 | Paired tank towers framing a central cell stack and control station |
| Bulk Steel Foundry | 5×6×6 | Stepped furnace stack with a heavy casting bay |
| Bulk Mechanical Metal Press | 4×6×6 | Tall portal frame, exposed flywheel and deep material bed |
| Bulk Polymerization Reactor | 4×4×6 | Towering jacketed vessel with domed crown and side service frame |
| Advanced Cryogenic Liquefier | 6×4×6 | Twin insulated cold towers and a low control/pump rack |
| Advanced Lithography Station | 6×4×4 | Enlarged cyan-window clean chamber and monitor bank |
| Industrial Drying Tunnel | 3×6×4 | Long insulated tunnel with raised fans and staged doors |


## Mechanical workshop steel and ceramics

### Workshop Crucible Furnace

**Size:** 2×3×3 blocks. **Form:** New fuel-heated entry form for shared alloy work.

**Model:** A squat firebrick hearth rises behind a broad charging lip. Its open-looking front is a lined, recessed firebox with an iron door and a bronze latch; above it, a dark crucible sits in a thick collar, with a short capped exhaust and a guarded bellows linkage. Two short legs project under the front apron. Put the fuel drawer low on the left, the pouring nose forward and a reusable mold shelf on the right, all inside the footprint.

**Textures:** Warm cream and red-brown firebrick, charcoal iron straps, honey-bronze bearing caps and a copper heat gauge. Draw large brick courses and a few clean casting seams, with an orange slit behind the door.

**Motion and interfaces:** The bellows folds and linkage move during assisted heating; the glow strengthens during a batch. The pouring assembly stays closed and still unless the future recipe actually exposes a casting action.

### Ceramic Kiln

**Size:** 2×2×3 blocks; larger variant **4×4×6** blocks. **Form:** New workshop kiln and larger shared ceramic-firing form.

**Model:** Build a thick square brick shell around a rounded, iron-banded doorway. The door has a deep rim, two chunky hinges and an offset latch; stacked shelf edges sit behind an inset inspection slit. A low fuel drawer and a short chimney complete the small silhouette. The large version scales this same kiln, including the door bands and shelves, into a prominent furnace tower rather than becoming a different rectangular oven.

**Textures:** Off-white fireclay, muted terracotta brick, near-black forged iron and a warm copper dial. Shelves use pale refractory tiles; the firing slit is a narrow amber-to-orange band rather than an entire glowing wall.

**Motion and interfaces:** Keep the exterior mostly still. A small damper linkage, state lamp and readable firing glow distinguish an active kiln. Fuel-heated entry remains visually distinct from later electrical controls.

### Shaft Waterwheel

**Size:** 2×4×4 blocks. **Form:** New kinetic-output waterwheel form; the existing electric Water Wheel is separate.

**Model:** A broad wheel fills the rear three blocks of depth, with thick spokes, scooped paddle boxes and iron hoops. The front contains a low bearing house and a geared shaft housing, creating a narrow side-on footprint rather than a wheel hidden inside a cube. Add wood bearing supports, capped axle ends and a simple protective arch. Both the paddles and wheel rim fit inside the four-block height at every rotation.

**Textures:** Warm oak planks, dark end grain, iron hoops and bronze bearing covers. Paddle faces have broad plank divisions and a few wet-looking darker bands, not a coat of grime.

**Motion and interfaces:** Turn the whole wheel smoothly and drive the front shaft. The output coupling is prominent; there is no electrical socket on this proposed mechanical source.

### Mechanical Metal Press

**Size:** 2×3×3 blocks; larger variant **4×6×6** blocks. **Form:** Unbuilt shaft-driven form and selected larger die press.

**Model:** Two heavy uprights carry a broad crosshead above a recessed die bed. The front is open enough to show the ram, installed die and material stop; a guarded flywheel and eccentric sit along the rear edge. Give the crosshead a readable crown, the base deep mounting feet and the die carrier a side handle. The six-block-tall bulk form enlarges this exact portal, flywheel and stroke, making the press a centerpiece of the hall.

**Textures:** Dark iron or cool steel, bronze bearing circles, a pale polished ram and olive drive guards. Place a short yellow/black strip only along the loading edge; use bold die outlines and clean steel bevels.

**Motion and interfaces:** The flywheel turns, the eccentric drives a deliberate ram cycle and the die carrier stays fixed. Reusable plate, rod, shape and ceramic-blank tooling changes the bed insert rather than creating unrelated machines.

### Mechanical Wire Drawer

**Size:** 2×4×2 blocks; larger variant **3×6×3** blocks. **Form:** Unbuilt shaft-driven form of the shared drawing family.

**Model:** Make a long low drawing bed: feed stock enters at the front, passes a thick draw-plate bridge and reaches a large take-up spool at the rear. A shaft and reduction box run down one side behind a ribbed guard. The spool has two broad flanges and a deep hub; a short operator lever and removable-looking die holder sit beside the front guide. The larger version preserves that same continuous bed and spool arrangement.

**Textures:** Bronze guide hardware, dark steel rails, olive bearing housings and warm wooden or metal spool faces. A small copper-coloured wire band sits in the guide; avoid individual hair-thin wire geometry.

**Motion and interfaces:** Rotate the take-up spool and drive coupling while working. Use a short, bounded strand between guide and spool; material entry is on the front and finished-wire handling at the rear.

### Mechanical Circuit Assembler

**Size:** 3×4×2 blocks. **Form:** Unbuilt mechanical entry form; existing powered assembly remains.

**Model:** A substantial workshop table supports three distinct working positions: part placement, a small soldering head and a clamped assembly tray. At the back, a camshaft in bronze bearings drives short linkages above the table. A fuel-heated solder pot sits behind a heat guard, and the front has shallow component drawers and a raised positioning fence. Keep the mechanism low and readable instead of giving the early workshop a futuristic cleanroom.

**Textures:** Dark wood, iron framing, bronze cams, dull tin work trays and copper contact strips. Use warm amber near the solder pot and a simple cream dial; there are no advanced cyan screens at entry.

**Motion and interfaces:** Index the clamped tray and lift the placement/solder head in short cycles. Shaft and component access remain obvious; the installed controls do not visually depend on the circuits this station is meant to begin.

### Bulk Coke Oven

**Size:** 4×4×6 blocks. **Form:** Selected larger form of the existing brick Coke Oven.

**Model:** Scale the current oven's brick body, beehive crown, charging door and chimney into a tall four-block-wide furnace. Add substantial hinge straps, a deep lower discharge recess and a short upper loading ledge within the six-block height. The central silhouette still reads as the familiar brick oven, with a capped stack rather than an unrelated metal refinery. An optional closed side flange leads to the shared byproduct-collection equipment.

**Textures:** Warm red-brown brick in large courses, deep charcoal door iron and pale refractory around the opening. Soot is a small accent at the chimney mouth; the whole surface stays clean and patterned.

**Motion and interfaces:** A gentle orange door slit and moving damper show activity. The optional collection connector is visible, but ordinary coke production does not require the collection station.

### Bulk Steel Foundry

**Size:** 5×6×6 blocks. **Form:** Selected larger foundry with a casting-capable assembly.

**Model:** Keep the existing foundry's stepped stack, heavy front throat, olive control housing and refractory base recognizable. The enlarged core occupies the rear, while a long front bay holds a casting runner, mold ledge and low iron railing. Side columns carry the bellows/air duct and a thick heat shield. The upper stack, charging chute and capped exhaust stop at six blocks; the extra depth comes from the useful casting bay rather than a wider blank furnace wall.

**Textures:** Cool gunmetal, restrained olive paint, off-white refractory and dark iron grating. Use molten amber only in the inspection slit and casting channel; broad steel plates have single seams and large bolt bosses.

**Motion and interfaces:** Move the assisted-air linkage and a casting gate where the selected operation needs it. Mold placement, heat readout and fuel access face the front; casting is an optional capability, not the only steel route.

### Casting Basin

**Size:** 2×3×2 blocks. **Form:** Selected optional reusable-mold basin; fits within the larger foundry bay.

**Model:** A deep refractory-lined pan sits in a broad iron cradle, connected to a short raised runner. The front carries a locking mold frame, a low release handle and a substantial lifting eye on each side. Build the basin with visible interior walls and a bottom, not an open-top solid box. In the foundry assembly, this module occupies the front bay inside the overall 5×6×6 envelope.

**Textures:** Pale ceramic, charcoal iron and a copper-coloured gate collar. The casting channel has a smooth orange band during a pour; mold cavities use dark, clearly recessed surfaces.

**Motion and interfaces:** Use a short gate movement and a contained fill/cool state. The mold is reusable installed tooling; cracks and wear do not imply a replacement cycle.

### Advanced Alloy Furnace

**Size:** 4×4×6 blocks. **Form:** Unbuilt larger/heat-capable form of the shared alloy family.

**Model:** Retain the existing smelter's tall furnace and adjacent crucible silhouette, framed by thicker supports and a rear heat-services column. A broad front feed tray and a low output shelf sit beneath a large ringed vessel; dark stepped pipe elbows join the two sides. Make the crucible collar and insulated lid separate pieces. Six-block height is the complete assembly, including its crown and caps.

**Textures:** Cool steel shells, cream refractory seams, olive service panels and copper power/heat connections. Alloy-specific tooling can use a small insert or plaque; do not give each alloy a completely different machine texture.

**Motion and interfaces:** Animate only the actual feed/gate mechanism and a restrained hot inspection slit. Shared forming and earlier reachable equipment remain separate from this advanced physical form.

### Precision Grinding and Polishing Station

**Size:** 3×4×3 blocks. **Form:** Selected shared finishing form for ceramics metals optics and wafer preparation.

**Model:** A stepped machine bed carries a broad horizontal polishing platter at the front and a smaller enclosed grinding head behind it. The platter sits in a thick splash rim; the upper head rides on two solid columns with a counterweight housing. Give the right side a short adjustable-looking work clamp and the left a covered return trough. The back contains a ribbed motor case and a low pump cabinet.

**Textures:** Field-grey steel, ivory ceramic guards, dark rubber seals and a single bright metal platter. Surfaces use broad radial bands rather than microscopic abrasive speckles; delicate optical work gets a calm cyan status window.

**Motion and interfaces:** Rotate the platter and move the head over a short working range. Interchangeable fixtures distinguish ceramic, metal and optical recipes; ports sit low at the rear, with finished parts retrieved from the front tray.

## Industrial agriculture textiles and finished materials

### Crop Processor

**Size:** 3×4×3 blocks; larger variant **5×6×5** blocks. **Form:** Unbuilt shared threshing husking and fibre-preparation form.

**Model:** Build an inclined hopper above a guarded horizontal processing drum. A wide front grille exposes the shape of the working head without exposing empty space; two distinct chutes separate useful crop stock and residue. The side contains a belt drive, an offset flywheel and bronze bearing caps. A stepped top cover opens visually over the drum, and shallow rear trays show where prepared material waits.

**Textures:** Cream-painted guards, dark iron framing, warm wood hopper boards and bronze trim. Cotton/flax heads have different recognizable grille inserts, while the main body and palette stay shared.

**Motion and interfaces:** Turn the drum and pulley, with short bounded movement at the two chutes. The large form scales the same drum, hopper and chutes; suitable installed heads determine the process.

### Screw Press

**Size:** 2×4×3 blocks; larger variant **3×6×5** blocks. **Form:** Unbuilt industrial form extending the hand/cider pressing role.

**Model:** A long pressing barrel slopes gently from a broad loading hopper into a ringed compression housing. The screw drive is visible at the rear behind a guard, and a thick front end plate surrounds the press-cake nozzle. A shallow liquid trough runs underneath to a low side flange. Include a stout crank or shaft coupling, a bronze collar and a front support leg that makes the press look anchored.

**Textures:** Dark iron bands, pale timber at the entry hopper, warm bronze bearings and muted cream enamel around the liquid trough. Use clean grooves on the barrel and short oil-coloured highlights near the collection lip.

**Motion and interfaces:** Rotate the screw/drive and let a small contained ribbon indicate liquid collection. Feed enters above; liquid leaves low on one side and cake at the front, visibly supporting several crop products.

### Mill and Pulper

**Size:** 3×4×3 blocks; larger variant **5×6×5** blocks. **Form:** Unbuilt shared dry-milling and wet-pulp form.

**Model:** A thick round mill housing sits on a low frame, with a rectangular rear drive and a large top hopper. The front service plate carries an offset bearing boss; a curved lower trough leads into a broad output drawer. A wet-processing configuration adds a short return pipe and lined pulp outlet on the same frame, rather than becoming a second compulsory machine. Large bearing blocks and a removable-looking head cover communicate interchangeable tooling.

**Textures:** Stone-grey mill components, charcoal iron, bronze bearing covers and warm wood or cream-painted feed surfaces. Pulp recipes use a pale lined trough; dry milling keeps a darker chute.

**Motion and interfaces:** Rotate the guarded shaft and central bearing feature. Flour, wood flour, starch and pulp use distinct recipes/heads; avoid a spray of separate item entities around the mill.

### Powered Spinning Frame

**Size:** 2×4×3 blocks; larger variant **3×6×5** blocks. **Form:** Unbuilt powered fibre form beside the existing hand spinning wheel.

**Model:** Place a row of large bobbins along an open iron-and-wood frame. A tall feed rack at the rear leads into broad guide rollers, with a low line shaft under the spindle row. The front has a stepped bobbin shelf and a single lever, while belt wheels stay behind a guard on one side. The silhouette is a narrow textile frame with visible empty intervals between supports, not a closed machine cabinet.

**Textures:** Honey wood, dark iron, bronze spindle collars and off-white yarn bands. Make wound yarn read as a few broad diagonal wraps, with no individual hair geometry.

**Motion and interfaces:** Spin the bobbins and traverse the guide slowly. The large form enlarges the familiar frame and working parts; finished yarn is collected from the front shelf.

### Powered Loom

**Size:** 3×4×3 blocks; larger variant **5×6×5** blocks. **Form:** Unbuilt powered loom and wider batch form.

**Model:** A rectangular timber/iron frame holds a broad cloth bed, a raised rear warp beam and a large front take-up roll. The upper crossbeam carries thick guide supports; a shuttle carriage moves between two clear end stops below it. A side cam housing and shaft pulley show how the loom is driven. Give the cloth path a visible bend over the breast beam so it reads as fabric under tension.

**Textures:** Warm timber, dark iron straps, bronze guides and a restrained woven-cloth pattern. Pattern colour belongs to the cloth insert; the frame remains the same across canvas and reinforced weaves.

**Motion and interfaces:** Traverse the shuttle, lift a small heddle assembly and turn the cloth roll slowly. Bound the travel inside the frame; this industrial form does not remove simple hand weaving.

### Rope Making Machine

**Size:** 2×6×2 blocks. **Form:** Unbuilt long cordage station.

**Model:** Make a long low tension bed with a fixed spindle head at the rear and a broad travelling guide at the front. Three or four large hooks sit on one drive plate; visible strand guides converge toward a thick finished-rope ring. Twin rails, braced feet and a shallow middle trough give the six-block length a purpose. Put the drive pulley beside the head and a finished coil holder at the operator end.

**Textures:** Dark iron rails, warm wood trays, bronze hooks and tan cordage. Use a few clear twisted bands on the finished rope, avoiding dozens of fine parallel strings.

**Motion and interfaces:** Turn the hook plate and advance the guide over a short readable working section. Reinforcement changes the feed insert, not the core silhouette; feed and finished coils remain easy to distinguish.

### Carding and Felting Machine

**Size:** 3×5×3 blocks. **Form:** Unbuilt shared fibre-mat preparation form.

**Model:** A deep lower frame holds three staggered rollers under a broad, partly covered working deck. The front feed tray rises toward a large guarded carding drum; the rear has a flatter pressure pair and an output mat shelf. Keep the drum's tooth field a textured pattern, while the rim, bearing caps and guard hinges are actual geometry. A short felt pad display in the output mouth helps explain the finished product.

**Textures:** Cream guards, graphite frame, bronze bearings and soft grey or tan fibre surfaces. Drum teeth use a regular low-contrast pattern; broad fleece bands provide texture without noise.

**Motion and interfaces:** Turn the rollers at visibly different but calm speeds. Fibre travels only through a short modeled working strip; this one family handles appropriate carding and felting heads.

### Heated Mixing Kettle

**Size:** 2×2×3 blocks; larger variant **4×4×6** blocks. **Form:** Unbuilt shared coatings adhesives wax and compound vessel.

**Model:** A squat stepped kettle sits between two broad supports over a small fuel hearth. Its top carries a central gearbox and a thick agitator shaft; a hinged-looking half-lid, large side handle and low spout break up the round body. Put the operator's dial on a short side bracket and the drain in a recessed lower panel. The large version keeps the same vessel and top drive, enlarged into a tall compound tank.

**Textures:** Warm copper or dull steel vessel surfaces, dark iron supports, pale lined lid edges and a cream instrument face. Show copper through broad stepped tones, with no heavily mottled patina.

**Motion and interfaces:** Rotate the top drive and a contained paddle where visible. An amber heat slit marks fuel operation; later electric control adds a small cabinet to the same visual family.

### Sheet Roller Line

**Size:** 3×4×3 blocks; larger variant **5×6×5** blocks. **Form:** Unbuilt shared rubber cloth-coating and flooring roller form.

**Model:** Two large parallel rollers dominate the front, supported by thick side cheeks and bronze bearing blocks. A short rear feed deck guides backing cloth or prepared sheet stock toward the nip, while a low front table supports the outgoing sheet. One side contains a geared drive behind a slotted guard; the other has a large gap-setting wheel as modeled tooling. A small hood sits over heated or coating work.

**Textures:** Cool polished roller bands, dark steel frame, olive guards and cream feed surfaces. The sample sheet carries the appropriate rubber, cloth or flooring colour; the machine stays calm and industrial.

**Motion and interfaces:** Counter-rotate the rollers and advance one short sheet strip. The setting wheel is a visual mechanism, not a new required pressure-management system.

### Fibre and Pellet Forming Press

**Size:** 2×3×3 blocks; larger variant **4×6×6** blocks. **Form:** Unbuilt shared forming configuration for mats briquettes feed and suitable fertilizer.

**Model:** Use a deep C-frame with a short upper ram, a broad lower die tray and a large feed hopper set back from the operator. A guarded crank drive sits beside the frame, and a low removable-looking output drawer collects formed products. Distinct tooling presents either a broad mat platen or a ringed pellet outlet without changing the core press. Heavy feet and a sloped base apron make it feel planted.

**Textures:** Dark steel, olive drive guards, pale ceramic die surfaces and warm timber at early feed handling. A single hazard band marks the loading edge; material colours appear only in the tooling/output sample.

**Motion and interfaces:** Cycle the ram or ring die for the selected recipe. Shared forming does not make every residue animal feed or every powder fertilizer; actual material compatibility stays in recipes.

### Heated Panel Press

**Size:** 4×6×4 blocks. **Form:** Unbuilt industrial board veneer laminate and composite press.

**Model:** A long four-column frame encloses a stack of thick horizontal platens. The front has a broad loading tray, guide stops and a projecting clamp carriage; the rear contains a compact heat-services cabinet. Put short capped cylinders or screw housings above the columns, with heavy connecting beams and a protected side linkage. The six-block depth supports full panel handling instead of a tall empty shell.

**Textures:** Graphite structure, cool steel platens, olive service panels and pale insulation pads. A restrained amber seam outlines the heated platen faces, while a visible panel sample can show layered wood or laminate edges.

**Motion and interfaces:** Close the platen stack slowly, pause and reopen within the stated height. Reusable tooling and controlled heating are visible features; there is no implied routine pad replacement.

### Industrial Drying Tunnel

**Size:** 3×6×4 blocks. **Form:** Unbuilt shared drying form for cloth paper boards and appropriate foods.

**Model:** A long insulated tunnel sits on a raised bed, divided by three broad rib frames rather than six identical crates. The front portal has thick double doors and a short loading deck; the roof carries two squat fan housings with capped vents. A side inspection strip and a low heat manifold run along the length, with a small controller near the front. The rear outlet repeats the door shape so the machine is finished from either end.

**Textures:** Muted cream insulation panels, olive external ribs, charcoal grilles and dark rubber door seals. Use regular panel joins and long quiet surfaces; warmth is a thin amber indicator at the controller.

**Motion and interfaces:** Fans turn and the internal feed bed advances while working. A few contained warm-air wisps are optional decoration, not landscape pollution or a new temperature simulation.

### Finishing Vat

**Size:** 3×4×3 blocks; larger variant **5×6×5** blocks. **Form:** Unbuilt shared washing dyeing coating and impregnation form.

**Model:** Make a long lined bath with a high rear tank wall and a pair of thick guide rollers spanning its ends. A broad front lip, removable-looking basket and low side drain communicate batch work. The drive sits on a raised rear corner; short closed hoses connect to a calm services cabinet. Model the visible inner lining, bottom and roller supports, with a small overhanging splash hood inside the footprint.

**Textures:** Cream enamel, grey metal rims, black seals and bronze roller caps. A shallow colour band in the contained bath changes with the recipe; pigments do not recolour the whole machine or surrounding floor.

**Motion and interfaces:** Turn the guide rollers and lightly move the work basket. The same vessel handles appropriate finishing recipes; feed is on top/front, discharge low and services at the rear.

### Paper Sheet Former

**Size:** 4×6×3 blocks. **Form:** Unbuilt paper-specific configuration of shared sheet-forming and pressing equipment.

**Model:** A broad shallow headbox feeds onto a short mesh table between two substantial side rails. Small guide rollers lead into one large pressing pair and a front stack tray. Beneath the table, a lined collection trough and two capped drainage branches keep the wet end readable. The outer structure is low and long, with a side drive and a narrow upper hood; it should look like a paper works rather than a conveyor with a cube attached.

**Textures:** Cream-painted metal, dark steel supports, pale pulp bands and a regular soft-grey mesh pattern. Keep the fine wire mesh in the texture, with only its surrounding frame in geometry.

**Motion and interfaces:** Move a single modeled wet sheet through the roller pair. Drying is the shared tunnel's job; this form does not hide an entire paper industry inside one machine.

### Industrial Fermenter

**Size:** 3×3×4 blocks; larger variant **5×5×6** blocks. **Form:** Unbuilt controlled fermentation form beside simple barrels and existing chemistry.

**Model:** A tall stepped vessel with a shallow domed crown stands on four stout legs. A short insulated jacket, ringed manway and narrow vertical level strip define the front; the rear has a motor, a closed CO2 outlet and a compact heat-services box. A sloping bottom cone leads to a low broth outlet. The bulk version enlarges this same vessel and its readable fittings, without a second unrelated brewery model.

**Textures:** Dull stainless or cream-painted steel, charcoal bands, copper/bronze fittings and one amber instrument. Liquid colour belongs only to the level panel; a narrow green lamp marks an active batch.

**Motion and interfaces:** Rotate the enclosed top agitator drive slowly and show a contained level/state change. Optional CO2 collection uses the upper flange; fermentation can continue with the selected excess-vent default.

### Anaerobic Digester

**Size:** 6×6×6 blocks. **Form:** Selected substantial first bulk digester.

**Model:** This is a factory landmark: a huge closed cylindrical tank with a faceted domed cap, thick horizontal compression rings and a sheltered front feed gallery. One side carries a capped feed auger, the other a low digestate manifold. A raised upper ring and ladder-like service detail sit inside the six-block bounds; a dark top gas header slopes toward a side pipe flange. Add a heavy rear mixer gearbox and four broad foundation shoes, keeping the body dominant.

**Textures:** Cream or pale olive tank panels, graphite bands, dark steel supports and bronze-grey fittings. Use long quiet tank fills with a few seam courses; the front controller has restrained amber and green indicators.

**Motion and interfaces:** The feed auger and mixer coupling turn slowly. A subtle gas-status lamp and digestate sight panel convey work; do not make this large sealed vessel bubble like an open cauldron.

### Bio Generator

**Size:** 3×4×3 blocks; larger variant **5×6×5** blocks. **Form:** Selected new lower-output generator family for cleaned biogas and bioethanol.

**Model:** A compact engine-generator sits on a thick skid, with a ringed combustion body at the front, a ribbed generator housing behind and a low fuel-handling box beside it. Give the front a round inspection cover, thick mounting bolts and a sheltered ignition/instrument panel. A short black exhaust folds toward the rear, while a capped side gas inlet and low liquid inlet visibly support the two selected fuel forms.

**Textures:** Olive engine covers, gunmetal ribs, pale ceramic heat shields, dark rubber hoses and copper terminals. A small mint output lamp belongs to the electrical end; combustion stays warm amber.

**Motion and interfaces:** Turn the guarded coupling and radiator fan, with a restrained exhaust animation while running. Keep this silhouette distinct from the stronger hydrogen/methane gas generator.

## Mineral separation refining and shared wet processing

### Screening and Washing Plant

**Size:** 4×5×3 blocks. **Form:** Unbuilt larger combined preparation form extending the sieve and ore washer.

**Model:** An inclined rectangular screen sits high at the rear over a stepped wash trough. Three substantial crossbars divide its length, and a covered feed hopper opens onto the upper end. Beneath it, two angled discharge chutes lead to concentrate and depleted-feed drawers. A dark side drive, eccentric shaft and closed water header distinguish screening from washing. Keep the moving screen inside a static outer frame, with its rear wall and trough floor fully modeled.

**Textures:** Olive steel, pale lined troughs, bronze bearing caps and a soft regular mesh texture. Wet material appears as a short dark band in the working area, not scattered splashes over every plate.

**Motion and interfaces:** Shake the screen over a small travel and turn the eccentric. Water services enter from the rear; useful material and residue have separate front/side outlets.

### Gravity Concentrator

**Size:** 4×6×3 blocks. **Form:** Proposed shaft-driven shaking-table form for the selected physical separation role.

**Model:** A long tilted table fills most of the footprint, with broad stepped riffles leading toward separate low chutes. The high rear end contains a feed hopper and short water rail; the front has split collection trays. Four braced legs support the table, while a side crank and counterweight explain its motion. Keep the table open and visibly inclined so it cannot be mistaken for the screening machine.

**Textures:** Warm wood or muted green table surfaces, bronze riffle edges, dark steel legs and pale water rails. Riffles are a small number of clear raised strips; fine grain is a restrained directional pattern.

**Motion and interfaces:** Oscillate the table and crank within the frame. This art selects a shaking-table presentation; a spiral alternative need not become a second mandatory machine for the same entry process.

### Magnetic Drum Separator

**Size:** 3×4×3 blocks. **Form:** Unbuilt electromagnetic mineral-separation form.

**Model:** A broad horizontal drum hangs between two thick side cheeks above a split discharge apron. The upper rear has a feed chute and short belt section; a substantial electromagnet housing caps the drum's back half. Model the exposed drum rim, dark internal throat, bearing bosses and large rear coil cabinet. Its silhouette is dominated by one round working drum, rather than an oversized generic control box.

**Textures:** Cool steel drum bands, olive housings, charcoal belt and visible copper coil accents behind a guarded side panel. A cream faceplate and a small green lamp identify the controls.

**Motion and interfaces:** Turn the drum and short feed belt; keep the material illustration confined to the working face. Earlier electromagnets start this role without requiring its own later rare-earth magnet output.

### Electrostatic Separator

**Size:** 3×5×4 blocks. **Form:** Unbuilt later electrical-separation form.

**Model:** A tall enclosed drum chamber stands over two sloping collection bins. A raised front window shows the rim of a large rotor, while the top carries a short high-voltage bus protected by ceramic stand-offs and a closed hood. The rear includes a stepped power cabinet and low feed elevator-shaped housing. Two distinctly angled output paths make the separation job readable even when the machine is idle.

**Textures:** Graphite shell, ivory ceramic posts, cool steel drum and restrained yellow/black marking at the electrical enclosure. Use a small mint status strip; no constant lightning shower.

**Motion and interfaces:** Turn the contained drum and move the internal feed guide. Feed enters high at the rear; separate fractions leave low on either side. Electric visual detail does not create a manually managed pressure system.

### Filter Press

**Size:** 3×5×3 blocks. **Form:** Unbuilt shared filter-press form for refining and suitable spent streams.

**Model:** A row of thick square filter plates sits between two long tie beams. The front head plate has a deep central recess and two broad locking handles; the rear holds a stout screw or cylinder housing. A lined drip tray spans the bottom, with a cake drawer and low liquid flange separated clearly. Plate rims and the compression frame are geometry; cloth interiors are calmer inset faces.

**Textures:** Cream ceramic/cloth inserts, graphite plate frames, cool steel beams and bronze drive hardware. Repeat the same broad plate-edge rhythm without turning the whole machine into bright checkerboard mesh.

**Motion and interfaces:** Give the head plate a short opening/closing cycle and a modest output-drawer movement if exposed by the recipe. Durable installed filters are not shown tearing or becoming a routine consumable.

### Precipitation and Crystallization Vessel

**Size:** 3×3×4 blocks. **Form:** Unbuilt shared controlled solution-recovery form.

**Model:** A wide lower bowl supports a narrower upper vessel with a short top drive and offset feed neck. The front has a tall recessed sight panel, a large bolted manway and a cone-bottom outlet above a collection tray. A compact side jacket and short closed return pipes convey controlled heating/cooling. Use stepped vessel shoulders and visible support brackets instead of one four-block cylinder.

**Textures:** Dull stainless, charcoal structural rings, pale enamel inside the outlet tray and bronze-grey fittings. A small crystal-pattern sample can sit in the output tray; the whole tank is not covered in gem facets.

**Motion and interfaces:** Rotate the enclosed drive slowly and change the contained solution/output indication per batch. This form serves named precipitation/crystal recipes, not a universal fluid-to-crystal conversion.

### Chemical Separation Rack

**Size:** 5×6×5 blocks. **Form:** Unbuilt shared separation form including named rare-earth fractions.

**Model:** Three distinct, closed mixing/settling chambers sit at staggered heights along a long steel rack. Broad linking manifolds run between them, with one raised front control cabinet and clearly separated outlet banks. A rear support spine carries capped pipes, ladders and low pump bodies. Keep the central vessels dominant; the five-block height comes from useful process stages, not a tower of decorative screens.

**Textures:** Field-grey/cream vessels, dark steel rack, black hoses and copper-grey flanges. Coloured output bands identify approved fractions; use small amber or green lights, with no rainbow glow across all panels.

**Motion and interfaces:** Animate a few real mixer couplings and contained level strips. The same rack can separate suitable reagents/minerals using distinct recipes; it does not imply that mixed oxide turns directly into finished magnet metal.

### Calciner

**Size:** 6×4×4 blocks. **Form:** Unbuilt shared heated powder-conversion form.

**Model:** A large horizontal refractory-lined drum sits in two substantial support rings, slightly raised at the feed end. The rear has a short charging hopper; the front ends in a ringed discharge hood and powder tray. A side gearbox drives a broad gear collar, with a capped exhaust rising from the hood but remaining inside the four-block height. Give the lining visible thickness at both mouths.

**Textures:** Warm off-white refractory, dark iron rings, olive drive guards and broad soot-grey drum bands. A small amber mouth slit provides heat detail; no random rusty mottling.

**Motion and interfaces:** Rotate the drum slowly within fixed support rings and show a bounded heat state. Dedicated powder-conversion capability can coexist with earlier kilns; the finished material remains recipe-specific.

### Sealed Reduction Retort

**Size:** 4×6×5 blocks. **Form:** Unbuilt controlled reduction and integrated titanium-cleanup form.

**Model:** A thick horizontal reaction vessel sits deep in a rectangular support frame, with a large front circular closure, side heat jacket and rear services tower. The front door has a clear segmented locking ring and a recessed central hub. Beneath the main vessel, an enclosed salt-return manifold and a separate clean-sponge drawer show the selected integrated cleanup. Roof pipe loops and the short lifting frame remain within five blocks.

**Textures:** Cool steel outer plates, pale ceramic heat shields, graphite seals and dark olive support beams. Use deliberate broad bands on the vessel, clean bolted rings and one warm inspection slit.

**Motion and interfaces:** Keep the retort mostly still; move the closure latch or product tray only during a defined access state. Heat and separation are automatic paid capabilities, not a new vacuum/pressure control puzzle.

### Seawater Concentrator

**Size:** 4×5×4 blocks. **Form:** Unbuilt coastal-feed concentration configuration of shared thermal/separation equipment.

**Model:** A low front intake cabinet leads into two closed, stepped concentration vessels and a short rear cooling rack. A wide water header sits low, with a distinct mineral-bearing outlet higher on the opposite side. The top has a broad hood and capped vent, while the front displays a clear level scale. Make it look like a wet industrial service plant, not a quarry that consumes beach blocks.

**Textures:** Cream-painted tanks, graphite supports, cool stainless pipes and small sea-green feed markings. Checker accents can appear on the tank shoulders; concentrate is identified by a contained indicator rather than glowing ocean water.

**Motion and interfaces:** Turn small enclosed pump couplings and a roof fan. The intake is a visible pipe connection; actual beach/ocean eligibility and source validation remain in the source specification.

### Chemical Distillation Station

**Size:** 4×4×6 blocks. **Form:** Unbuilt shared chemical/ethanol/recovery form; existing refinery tower remains.

**Model:** A tall ringed column rises behind a low reboiler and a narrow side condenser. The front carries a two-level instrument rack, while several clearly capped draw-off flanges emerge at different heights. A short upper platform and ladder detail sit inside the six-block envelope. Keep the column offset so its condenser and lower product manifold can be read from the front, not hidden behind a featureless tower wall.

**Textures:** Dull stainless column bands, graphite base, cream insulation and copper-grey junctions. Use a narrow amber heat indicator and calm green instruments; chemical products do not demand different towers for every recipe.

**Motion and interfaces:** A small cooling fan and contained flow/level indicators are enough. Ordinary ethanol distillation, chemical purification and suitable solvent recovery share this visual family; later ethanol dehydration uses defined tooling.

### Molten Salt Electrolysis Hall

**Size:** 6×5×4 blocks. **Form:** Unbuilt bulk form of the shared cell family for appropriate aluminum and magnesium operations.

**Model:** A wide, low cell basin sits between raised ceramic sidewalls and two thick overhead busbars. Three large electrode-support shapes descend through closed upper covers; the rear carries a compact control/power cabinet. Low front metal drawers and separate rear gas/salt connections break up the footprint. The six-block width comes from the cell and its bus supports, making this installation feel like a small smelter hall.

**Textures:** Cream refractory, cool steel, substantial copper busbars, dark graphite electrode hardware and restrained olive cabinets. Warm colour stays in a protected inspection seam; black/carbon material is a reagent presentation, not a wear meter.

**Motion and interfaces:** Use a low contained bath glow and a few state indicators rather than constant sparks. Internal melting is automatic; no exposed molten-chloride pipe network is required by the visual design.

## Gas chemistry fuels and reagent preparation

### Electrolytic Separator

**Size:** 2×2×3 blocks; larger variant **4×4×6** blocks. **Form:** Unbuilt steel-entry and bulk forms extending the existing Electrolytic Cell.

**Model:** A tall central cell housing stands between two narrower collection towers on a shared plinth. The front has a stepped door, a large insulated bus connection and two distinct upper outlet collars; a lower recessed return identifies liquid handling. A small left-side control box holds two gauges and an amber lamp. The bulk form scales the same central cell and paired towers so it still reads as the separator rather than a different reactor.

**Textures:** Olive steel, off-white ceramic insulators, copper busbars and charcoal pipes. Give the collection towers pale panels and a restrained checker shoulder; hydrogen/oxygen/chlorine identification uses labels and port bands.

**Motion and interfaces:** Use gentle contained level/activity strips and a small state lamp. The oxygen/water peroxide recipe requires its own compatible capability/tooling presentation; it is not drawn as ordinary water splitting automatically producing peroxide.

### Chemical Infuser

**Size:** 2×3×3 blocks; larger variant **4×6×6** blocks. **Form:** Unbuilt entry/bulk gas-reaction forms on shared synthesis systems.

**Model:** Two substantial inlet branches enter opposite shoulders of a central stepped reaction chamber. The lower front holds a thick catalyst-bed drawer with a raised handle; above it, a ringed inspection face and heat gauge identify the reaction zone. A closed rear loop leads into a short cooling jacket and separate product/water connections. The bulk form enlarges this exact chamber, its paired inlets and installed-bed compartment into a tall synthesis installation.

**Textures:** Cool gunmetal, olive service plates, pale ceramic collar, black seals and bronze-grey flanges. Nickel, vanadium or other defined reusable tooling gets a small bed-face marker, not a different whole-machine colour.

**Motion and interfaces:** Keep the vessel stationary; animate a contained process band, short cooling fan and controller state. Paired feeds, heat and the correct catalyst are recipe capabilities, not freely interchangeable decorative pipes.

### Chemical Oxidizer

**Size:** 2×2×3 blocks; larger variant **4×4×6** blocks. **Form:** Unbuilt solid-feed oxidation form.

**Model:** A broad rear hopper feeds into a squat lined reaction throat, with a short gas column above it. The front carries a circular bolted hatch, a small inspection slit and a deep residue drawer; a capped oxidant inlet enters low on one side. Give the top a ringed collector hood and the rear a guarded feed screw. The large version preserves the same hopper-to-throat-to-column sequence.

**Textures:** Dark steel, cream refractory, olive hopper guards and warm copper fittings. A small orange reaction slit and a clearly patterned feed-lip hazard edge give focus without covering the shell in bright noise.

**Motion and interfaces:** Turn the feed screw while processing and brighten the contained reaction slit. Solid, oxidant and residue access are separate; this form does not visually promise that every dust becomes a useful gas.

### Chemical Dissolution Chamber

**Size:** 3×3×4 blocks; larger variant **5×5×6** blocks. **Form:** Unbuilt shared acid/alkaline digestion form.

**Model:** A deep, closed vessel with a broad conical lower section sits inside a four-post frame. A substantial upper mineral hatch, guarded top stirrer and thick side acid flange explain its inputs. The front has a lined sample recess, bolted manway and distinct lower slurry and residue branches. Put a compact controller on one corner so the chamber retains a strong vessel silhouette; the larger form scales the same vessel and cage.

**Textures:** Muted stainless/field-grey shell, ivory lining edges, graphite gaskets and restrained yellow port collars. An inspection panel uses dark glass with a contained slurry-colour band; no corrosive damage is painted across the body.

**Motion and interfaces:** Turn the top drive slowly and show contained fluid movement. Special heated fluorite handling has a recognizable compatible hood/tooling insert, with its gas and calcium-sulfate outputs still separate.

### Rotary Condensator

**Size:** 2×3×3 blocks; larger variant **4×6×6** blocks. **Form:** Unbuilt ordinary reversible phase-conversion form.

**Model:** A horizontal ringed drum rests in a tall U-shaped frame, with a broad guarded wheel on the front and a stepped coil jacket along its rear half. A small upper tank, closed return pipe and low receiver define the heat-exchange loop. The controller sits beside the front wheel, with distinct gas and liquid collars on opposite sides. The bulk form enlarges the same drum and frame rather than becoming the rocket cryogenic tower.

**Textures:** Cool steel, graphite wheel guard, cream insulated jacket and small copper-grey coil bands. Gas/liquid ports get different shape markers; a green activity ring can sit on the controller.

**Motion and interfaces:** Rotate the guarded drum/wheel where the final mechanism uses it, with a contained level indication. Ordinary phase changes remain visually separate from very cold rocket-liquid equipment.

### Coal Gasifier

**Size:** 4×4×6 blocks. **Form:** Selected new earlier polluting gas-production form.

**Model:** A tall refractory-backed reaction column rises above a broad lower ash chamber. A sloped side charging chute meets the column halfway up, and a short upper gas header runs toward the cleaner. Add a thick closed front door, large braced feet, a dark feed-drive cabinet and a contained lower heat throat. The top stack is capped and stays inside the six-block height; it should look heavy enough to process real bulk fuel.

**Textures:** Cool blackened steel, off-white refractory borders, olive guards and bronze-grey pipe rings. Warm orange stays in the heat throat; soot is confined to a few mouth/seam accents.

**Motion and interfaces:** Move the feed screw and a lower gate only during work. A restrained exhaust effect can identify operation, while numeric pollution and optional treatment remain system behaviour rather than painted environmental damage.

### Gas Cleaning and Separation Rack

**Size:** 3×5×4 blocks. **Form:** Selected combined initial cleanup/separation form; compatible optional scrubber configuration.

**Model:** A stepped wash tower at the rear joins a broad lower collection pot and a short side separator through closed flanged pipes. The front holds a residue drawer and two clearly separated gas outlets, while the liquid input sits low on the left. A small upper service bridge and calm control cabinet provide detail without making three unrelated machines compulsory. Optional emission-treatment configurations retain this same rack and distinct recipe/tooling inserts.

**Textures:** Cream lined vessels, graphite frame, olive pump housings and dark rubber seals. A few coloured bands distinguish feeds/outlets; the residue drawer gets a separate material insert.

**Motion and interfaces:** Turn a small pump coupling and a contained separator feature. Wash level and product-state indicators are bounded; cleaned gas, residue and any released fraction remain visibly different destinations.

### Gas Burning Generator

**Size:** 4×5×4 blocks; larger variant **5×6×5** blocks. **Form:** Selected hydrogen/methane generator form.

**Model:** A long combustion section sits beneath an arched ceramic heat shield, followed by a large ribbed generator housing. The front has a round intake/control face and a stepped ignition box; the rear carries a guarded coupling, a low exhaust turn and a substantial electrical terminal cabinet. A distinct capped gas inlet enters the side above the plinth. The larger form scales the same engine-generator train, preserving its recognizable division.

**Textures:** Graphite ribs, olive combustion covers, pale heat shields, black hoses and copper terminals. Use amber near the burner and mint green at electrical output, leaving most panels calm.

**Motion and interfaces:** Turn the guarded coupling and fan with a slow start/stop. Hydrogen/methane use the same machine family; gas inlet and power outlet are obvious without a constant cloud or shower of sparks.

### Bulk Chemical Reactor

**Size:** 4×4×4 blocks. **Form:** Unbuilt larger form of the existing acid/mixing reactor.

**Model:** Enlarge the current jacketed reactor body, its supports, side services and front controller into a substantial square-envelope vessel. Preserve its familiar tank, lid and pipe proportions; add no forest of unrelated reaction tubes. The front has a deep bolted access plate and a readable lower product manifold, while the top drive and short closed absorption inlet remain within the four-block crown. Shared acid absorption, neutralization and compatible mixing use installed recipe capabilities.

**Textures:** The existing steel/olive family with cream compatible lining edges, dark seals and broad clean vessel bands. Acid-grade or treatment tooling is a small plaque/insert rather than a new whole-body palette.

**Motion and interfaces:** Use the established top drive and restrained activity window. Retained carrier acid and new product stock have distinct handling; visible controls do not imply routine acid consumption as maintenance.

### Reagent Purification Station

**Size:** 4×4×4 blocks. **Form:** Selected shared demanding-reagent purification form.

**Model:** A broad low wet-services bed supports two unequal purification columns and a compact conditioning chamber. The front includes a deep clean-product hatch, a tall inset level panel and a quiet instrument cabinet; the rear contains short capped return loops and a low residue drain. The columns have separate collars, bolted lids and mounting shoes. Keep the shape asymmetric and useful, with the taller column giving the machine a clear profile.

**Textures:** Cool ivory enamel, graphite frames, muted steel vessels and small cyan-ready inspection panels only on the demanding electronic configuration. Reuse owner-library purification panel motifs where appropriate; long vessels stay seamless.

**Motion and interfaces:** Turn two small contained service couplings and show a product-state strip. Water conditioning is internal accounted processing, not a second stored purified-water machine requirement.

### Synthetic Fuel Reactor Train

**Size:** 6×6×6 blocks. **Form:** Unbuilt bulk synthetic-fuel configuration of the shared synthesis family.

**Model:** A familiar tall synthesis vessel dominates the rear centre, supported by thick pylons and a low forward manifold gallery. Two shorter condenser vessels flank it, while a broad front catalyst-access panel and a low water receiver make the cobalt reaction and separated water readable. Closed upper pipe bridges connect the vessels without leaving the envelope. The front corners carry a large instrument cabinet and a clearly separated synthetic-crude outlet bay.

**Textures:** Gunmetal reaction hardware, cream thermal jackets, olive frames and copper-grey flanges. The installed cobalt/ceramic bed has a small distinctive face marker. Amber reaction indicators and green delivery lights are restrained accents.

**Motion and interfaces:** Animate the actual service pumps/fans and contained product indication. This is a shared bulk capability form, not a new synthetic fuel currency or replacement for the existing downstream refinery.

## Polymer forming and precision electronics

### Polymer Molding Press

**Size:** 2×3×3 blocks; larger variant **4×6×6** blocks. **Form:** Selected new shared molding form with reusable tooling.

**Model:** A horizontal mold chamber sits between a thick fixed platen and a sliding rear platen on four broad tie bars. A small top hopper feeds a heated ringed barrel behind the chamber; the front has a guarded product tray and a short tool-change handle. A compact side drive and recessed controls keep the machine purposeful. The bulk form enlarges this exact molding train, with a deep guarded work opening rather than a new tower.

**Textures:** Olive or cool ivory panels, graphite frame, bright steel tie bars, black seals and a narrow amber heater band. Mold inserts have clear sculpted cavities; product colour belongs to the output sample.

**Motion and interfaces:** Close/open the mold platens over a short stroke and move one contained output piece. Reusable molds distinguish housings, fittings and other products; ordinary and specialty stock keep their recipe requirements.

### Polymer Extruder

**Size:** 3×5×3 blocks. **Form:** Selected new shared film hose and insulation form.

**Model:** A long heated barrel runs from a raised rear hopper into a large front die head. Under the barrel, a ribbed motor and gear housing sit on a deep skid; above it, a small insulated hood and short vent complete the stepped silhouette. A front guide assembly can present a flat film slit, ringed hose die or cable-coating guide without replacing the core body. A short receiving roll occupies one side of the front bay.

**Textures:** Cool steel barrel, ivory insulation collars, olive drive cover and graphite base. Heater rings use restrained amber bands; extrusion dies are polished steel with clean geometric mouths.

**Motion and interfaces:** Rotate the enclosed drive and receiving roll, showing only a short section of produced film/hose. Product travels inside the five-block depth; no unbounded animated material stream is required.

### Bulk Polymerization Reactor

**Size:** 4×4×6 blocks. **Form:** Unbuilt doubled form of the existing polymerization vessel.

**Model:** Scale the established jacketed vessel, supports, domed crown, controller and feed/product fittings together. The large body has a deep lower cone, a prominent top drive and a thick circular lid collar; its side service frame and low pellet outlet remain recognizable. Broad shoulders and support shoes prevent the six-block-tall tower from reading as a thin pipe. Keep the crown and all closed return loops inside the stated height.

**Textures:** The existing clean reactor palette with muted stainless/olive surfaces, dark rubber, pale lining edges and small amber/mint indicators. HDPE, LDPE and specialty compatible recipes use stock/tooling markers rather than entirely different tanks.

**Motion and interfaces:** Retain the same agitator/indicator behavior at larger scale. Internal recipe capability controls polymerization; the model does not introduce a player pressure adjustment or free gas-to-pellet conversion.

### Rubber Preparation and Curing Station

**Size:** 2×3×3 blocks. **Form:** Proposed shared bench/heat-tooling form for the selected natural-rubber side route.

**Model:** A low lined preparation bowl sits beside a small enclosed curing cabinet on one robust work base. Two broad hand/shaft rollers span the front, with a short drying ledge and a mold drawer below them. The upper cabinet has a rounded insulated door, a heavy latch and a short capped heat vent. Use one coherent station for small preparation/cure operations, with the separate large rollers and dryer still useful for bulk expansion.

**Textures:** Warm wood at entry, charcoal iron, cream enamel and a few bronze fittings. Raw rubber is a pale tan sample; cured sheet is dark and smooth with broad edges, not a glossy photoreal surface.

**Motion and interfaces:** Turn the rollers and show a narrow warm cabinet slit during curing. Fuel heat and reachable early materials keep this visual route independent of an electrical acid factory.

### Precision Wafer Cutter

**Size:** 3×5×3 blocks. **Form:** Unbuilt precision fixture/hood form of the shared saw and cutting family.

**Model:** Preserve the sawmill's long bed and drive layout, but place the precision working head inside a thick ivory hood with a broad front inspection pane. A short clamped boule carriage enters from the rear; a recessed wafer tray exits at the front. The head rides on two clear guide rails, and a low enclosed drain/collection bed prevents the delicate work area from looking like an open timber saw. A compact side cabinet carries the controls.

**Textures:** Cool ivory guards, graphite rails, clean metal clamps and a calm cyan pane with violet cable collars. Wafer surfaces are dark blue-grey disks with one broad highlight band; avoid mirror-like noise.

**Motion and interfaces:** Rotate the guarded cutting head and index the carriage slowly. The fine-cutting fixture is a shared capability, not permission to remove ordinary sawmill recipes.

### Wet Processing Station

**Size:** 4×5×3 blocks. **Form:** Selected separate connected advanced-wafer wet-processing form.

**Model:** Three lined work basins sit beneath a broad protective hood, connected by a short overhead transfer carriage. The front presents a deep clean loading tray, a small wafer holder and two recessed instrument panels. Separate capped reagent connections enter at the rear; spent mixture leaves through one clearly marked low manifold. A substantial hood fan and cabinet feet give the station volume without turning it into a fully enclosed room.

**Textures:** Cool ivory enamel, graphite framing, dark cyan inspection panels and restrained violet services. Basin rims have a single clean bevel; colour-coded ports identify cleaner, developer and appropriate etch supply without making their fluids glow.

**Motion and interfaces:** Move the wafer holder through a short cycle and turn the hood fan. Cleaning, TMAH development and HF oxide work are distinct recipe jobs on compatible shared wet hardware.

### Advanced Lithography Station

**Size:** 6×4×4 blocks. **Form:** Unbuilt doubled form of the existing 3×2×2 lithography station.

**Model:** Scale the existing cyan-window chamber, operator desk, monitor bank, rear filter housing and pass-through features together. Inside the visible work area, emphasize a stout exposure head above a flat wafer stage, with a raised pattern carrier and two guide rails. The front desk stays attached to the chamber and uses a broad keyboard shelf rather than floating controls. Finish the rear service panel and underside frame so the enlarged station is attractive from every side.

**Textures:** Established graphite/cyan/violet electronics textures, cool ivory optical hardware and dark blue-grey wafer stock. Keep large walls calm; the inspection window and monitor faces are the principal luminous elements.

**Motion and interfaces:** Index the wafer stage and move the exposure head over a small range. The reusable pattern carrier stays intact; the large form preserves the current machine's visual identity and does not imply new recipes already exist.

### Silane Deposition Chamber

**Size:** 4×6×4 blocks. **Form:** Selected later connected deposition form.

**Model:** A long closed process chamber sits on a low services frame, with a thick front transfer vestibule and a ringed main vessel behind it. The rear contains a compact gas-handling rack, short capped pipe loops and a side heat jacket. Give the chamber a substantial bolted closure, a small viewport and a wafer-carrier rail aligned with the front. The four-block height belongs to the chamber and services, not another towering cleanroom.

**Textures:** Ivory outer panels, graphite seals, dull stainless vessel bands and violet/cyan electronics accents. A narrow warm process indicator sits behind the viewport; no exaggerated green fog is used to represent silane.

**Motion and interfaces:** Slide the wafer carrier through a short transfer sequence and animate contained heater/state indications. Deposition is the later film-forming role, distinct from bulk silicon supply and wafer etching.

### Advanced Module Assembler

**Size:** 6×4×4 blocks. **Form:** Unbuilt larger shared assembly form for general-chip upgrade products.

**Model:** Enlarge the existing assembly table, placement mechanisms and rear controls into a broad clean manufacturing station. Three visible component trays feed a guarded central work nest; a short gantry carries one placement head and a thick gripper. The front has a finished-module shelf and a recessed tool panel, while the rear holds ribbed drive covers and closed cable runs. Keep the familiar assembly silhouette rather than replacing it with a featureless cyan box.

**Textures:** Cool ivory, graphite, polished steel guide rails and restrained cyan monitors. Function-related speed/efficiency/automation parts are small coloured tray inserts, not different overall machine bodies.

**Motion and interfaces:** Move the head between the trays and central nest, then index the output holder. Only actual working mechanisms animate; module identity and compatibility belong to assembly recipes.

### Advanced Electroplating Bath

**Size:** 6×3×3 blocks. **Form:** Unbuilt larger form of the existing 4×2×2 plating bath.

**Model:** Scale the established long bath, overhead carriers, electrode supports and side control hardware by one and a half. Give the widened front lip a visible ceramic lining and a low terminal-part rack, with thick hooks suspended from the upper crossbar. Short closed reagent returns and a rear power cabinet keep the plated-part route legible. Small battery terminals can sit on a broad holder without forcing microscopic mesh details.

**Textures:** Cream lined walls, cool stainless rims, dark graphite electrode hardware and copper or plated-metal highlights on the holder. Small mint power indicators stay concentrated at the cabinet.

**Motion and interfaces:** Lift the work holder slowly and move it along a short rail. The new terminal-plating capability extends the family; it does not introduce routine corrosion or mandatory repair to every machine.

## Grid storage gas tanks and cryogenic installations

### Lead Acid Battery Bank

**Size:** 3×3×2 blocks; larger variant **6×6×4** blocks. **Form:** Selected new steel-era electrical-storage form.

**Model:** Two rows of tall rectangular cell jars sit in a low steel rack, with broad terminal bars above and a compact front instrument panel. Each jar has a thick capped top and a pale inset face; dark support posts and a bottom cable channel separate the rows. A raised top guard shelters the busbars without hiding the cells. The bulk bank scales the same jars, rack and terminals into a broad six-by-six storage installation.

**Textures:** Cool grey steel, pale ivory jars, dark bakelite/graphite tops and copper terminal strips. Mint charge indicators belong to the front panel; the acid is not painted as a glowing green pool.

**Motion and interfaces:** Storage remains mostly still. Use a clear charge bar and slow state indicators rather than a moving electrical effect on every cell. Power access is on the front or a defined low rear terminal.

### Advanced Lithium Battery Bank

**Size:** 6×2×4 blocks. **Form:** Unbuilt doubled form of the existing lithium battery bank.

**Model:** Enlarge the existing stacked battery faces, supporting frame, front power interface and status strips together. The narrow depth makes this a substantial wall-like rack rather than a tank. Recessed cell drawers, thick vertical separators, a low cable channel and capped top ventilation housings provide detail. Keep the familiar current bank design visible at the larger scale; new electrolyte/cell components do not require an unrelated neon tower.

**Textures:** Existing graphite/ivory electrical surfaces, cool metal drawer rails, dark separators and restrained mint charge bars. Draw clean repeated cell divisions with quiet panel centres.

**Motion and interfaces:** Only charge/state bars and a small thermal-services fan need activity. Cell formulation and finite stored charge are processing/system work, not free energy visibly pouring from the electrolyte.

### Modular Flow Battery Plant

**Size:** 6×6×6 blocks. **Form:** Selected unbuilt paired-module controller/cell-stack arrangement extending Flow Battery.

**Model:** Two tank towers frame a central rear cell stack, with a low front controller between them. The vessels have thick support rings, capped crowns and clear lower manifold branches; a broad upper service frame joins the composition. The front two blocks of depth form the controller/access zone, while the rear four contain tank and stack hardware. Show one paired module set in this reference arrangement; actual slot/module limits remain specification work.

**Textures:** White tank bodies with bold restrained checker shoulders, graphite frame, cool steel manifolds and copper cell connections. Distinct tank-side port markers communicate the functional pair; mint charge bars stay on the controller.

**Motion and interfaces:** Small closed pump couplings and the controller's charge display show operation. Capacity and charging/output are visually separate components; additional adjacent complete banks can make a larger factory while each reference plant fits six blocks per axis.

### Flow Battery Capacity Module

**Size:** 2×4×5 blocks. **Form:** Unbuilt paired tank module; use two within the reference plant.

**Model:** A narrow tall tank sits in a rectangular frame with a thick bottom saddle, ringed shoulder and closed lid. Its front has a slim level recess; the lower rear carries two short flanged manifold branches and a large foot plate. The tank's body dominates its height, while broad corner supports keep it visibly modular. Build two corresponding functional sides, not a single decorative fluid tower standing in for the entire battery.

**Textures:** Shared white/checker tank palette, graphite frame, dark seals and a small side-specific port marker. Long vessel faces have calm seamless fills; level information occupies one inset strip.

**Motion and interfaces:** A contained level display and small pump connection are sufficient. Capacity modules enlarge stored-electrolyte space; they do not emit electricity independently or replace the cell-stack output role.

### Flow Battery Cell Stack Module

**Size:** 2×3×4 blocks. **Form:** Unbuilt charging/output module for the selected flow plant.

**Model:** A thick rectangular pack of repeated cell plates sits between two dark end frames, with four large tie rods and upper current bars. The lower section holds two clearly separate flow manifolds; a rear cable channel climbs into a capped terminal box. Keep plate spacing broad enough to read at a distance, with the stack visibly distinct from a tank. The front face includes a recessed status panel and a substantial lifting bracket.

**Textures:** Graphite cell edges, ivory separators, cool steel end frames and copper current bars. Use a regular restrained plate pattern, not hundreds of high-contrast fins.

**Motion and interfaces:** A few controller-linked state lamps show charging/discharging. Output capability belongs to the stack; tank capacity remains a separate upgrade. Decorative electrical arcs are unnecessary.

### Advanced Cryogenic Liquefier

**Size:** 6×4×6 blocks. **Form:** Unbuilt upgraded physical form extending existing oxygen cooling toward methane and hydrogen.

**Model:** Keep the existing cold-box family recognizable, then give the advanced configuration two tall insulated towers over a broad lower services rack. One tower has a short stepped crown, the other a larger transfer collar; closed loops join them above a low front receiver. Thick insulated pipes, a guarded service fan and a sheltered control cabinet give the six-block height a clear purpose. Every tower cap and pipe bend stays inside the envelope.

**Textures:** White insulation, graphite base, cool steel collars, deep cyan inspection panels and violet service accents. Frost is a small clean band around selected cold connections, not random white noise across every surface.

**Motion and interfaces:** Turn the service fan and enclosed pump couplings, with restrained cold-state indicators. Oxygen, methane and advanced hydrogen capabilities use compatible tooling/tier presentation without player pressure controls.

### Insulated Cryogenic Tank

**Size:** 2×2×3 blocks; larger variant **4×4×6** blocks. **Form:** Selected unbuilt insulated variants of the shared tank family.

**Model:** A thick double-wall-looking tank stands on four broad feet, with a recessed central lid, protected transfer collar and a vertical level panel. A short offset vent cap and dark lower ring make the vessel feel insulated. The larger variant scales the same tank, its lid and protected flange. Keep the tank top, fittings and supports inside its three- or six-block complete height.

**Textures:** Owner-style white bodies, dark rims, restrained checker shoulder and broad pale-blue insulation accents. Cold fittings have one or two deliberate frost bands; most of the vessel remains clean cream-white.

**Motion and interfaces:** Level and compatible-content markings can change with state. No perpetual vapour plume or implied routine boiloff is needed; filled pickup/placement and pipe compatibility belong to the shared tank implementation.

### Bulk Gas Holder

**Size:** 6×6×6 blocks. **Form:** Unbuilt doubled form of the existing gas sphere.

**Model:** Scale the current spherical holder, its supporting legs, lower manifold, top collar and level hardware together. Preserve the recognizable broad sphere with a stepped geometric roundness, not a six-block white cube. Give the support frame deep foundation shoes and finish the visible underside between the legs. A capped top fitting and front level/control bracket stay inside the six-block height.

**Textures:** Existing white tank/checker family, dark structural legs, graphite collars and cool metal flanges. Use large quiet curved bands; do not stamp framed panel tiles across the sphere.

**Motion and interfaces:** This is a calm buffer with level and content indications. It can support compatible gas storage/capture roles, with portable small tanks remaining available rather than all handling requiring this enormous holder.

## Recovery recycling and emissions treatment

### Equipment Disassembly Bench

**Size:** 2×3×2 blocks. **Form:** Selected shared manual-to-assisted recovery station.

**Model:** A heavy iron table sits on two broad trestles, with a recessed parts cradle, twin clamping jaws and a rear rack of reusable tools. A slotted front grate opens toward a removable-looking collection tray, while separated metal and component bins tuck below the worktop. The small shaft-assisted head parks on a stepped rear column; it is an optional powered mechanism rather than a gate to manual dismantling. Finish the back, underside and open bin interiors so it looks like a complete workshop station.

**Textures:** Dark iron feet, bronze clamp screws, warm timber drawer faces and an ivory sorting diagram. Use clean tool silhouettes, large slotted panels and a few loading-edge scuffs.

**Motion and interfaces:** The clamp closes for a batch; an assisted head makes one restrained stroke. Appropriate reclaimed outputs come from future explicit recipes, not a promise that every machine returns all its construction parts.

### Scrap Sorting and Preparation Line

**Size:** 4×6×3 blocks. **Form:** Unbuilt bulk recovery configuration of shared sorting milling and separation roles.

**Model:** A deep loading hopper stands above a guarded preparation drum. A short roller bed runs beneath a broad magnet housing and ends at three stepped discharge chutes feeding recessed bins. Put the drive gearbox beside the drum, a capped shaft under the roller bed and a small viewing hatch above the actual sorting junction. The bins, chutes and motion fit entirely within the six-block length. The line should look like a compact heavy factory, with each processing section visible through its shape.

**Textures:** Olive structural casing, gunmetal drive parts, bronze bearing caps and pale bin labels. Use one hazard band at the drum entrance, ribbed guards and distinct textured contents rather than piles of item entities.

**Motion and interfaces:** The preparation drum and rollers turn during work; the sorting head pulses subtly. Waste, mixed scrap and clean offcuts retain their defined route-specific recovery rules, with no universal lossless recycling.

### Particle Collector

**Size:** 3×3×5 blocks. **Form:** Unbuilt optional particulate-capture form from the recovery plan.

**Model:** A tall tapered cyclone body rises over a sealed collection hopper, supported by four narrow steel posts. A curved inlet enters near the upper shoulder; a capped vertical outlet finishes below the maximum height. Add a rectangular rear polishing-filter housing, a front access hatch and a short lower residue chute with a closed receiver. Model the cyclone as several stepped rings tapering into a cone, giving it a distinctive outline without excessive polygon density.

**Textures:** Field-grey barrel, ivory filter housing, charcoal feet and copper flange bolts. Large seams wrap the cylinder; the filter pattern appears only behind its small access grille.

**Motion and interfaces:** A small fan turns inside the guard and a gauge changes while capture is active. Compatible captured material and pollution reduction require accounted recipes; filters introduce no routine replacement task. Gas scrubbing uses the shared Gas Cleaning and Separation Rack, and neutralization uses the shared reactor.

## Later industrial candidates with proposed art only

The following seven forms remain optional roadmap ideas. Their art briefs do not select new gameplay, world generation, control behavior or compulsory progression gates.

### Precision Fabricator

**Size:** 5×6×4 blocks. **Form:** Unbuilt roadmap candidate for precision parts; functionality and recipes remain proposals.

**Model:** A broad enclosed gantry straddles a six-block machining bed. The large central fixture carries one replaceable-looking work nest; the left rail holds a cutting head, while the right rear corner houses a folded assembly arm. Stepped side cabinets contain a tool carousel and electrical enclosure. Deep front doors have two dark inspection panels, with an external control pedestal attached inside the envelope. Give the gantry enough mass to feel capable of producing habitat frames and navigation assemblies.

**Textures:** Cool ivory upper panels, graphite bed, steel rails and thin cyan window borders. Use calm geometric door seams, a numbered fixture plate and one violet cable sleeve.

**Motion and interfaces:** The head crosses a short path and the arm folds inward during a compatible batch. This is a reusable fabrication role, not a selected mandatory machine for every rocket or upgraded component.

### Grid Control Station

**Size:** 6×4×3 blocks. **Form:** Unbuilt later remote-control roadmap candidate; local battery controls develop first.

**Model:** A shallow arc of five recessed monitor bays rises behind a broad sloping operator desk. Two end cabinets hold switchgear and cable entries; a central raised instrument cluster separates the screen bank from the desk. Keep a clear modeled standing recess at the front and a full-height rear cable cabinet with closed louver panels. The six-block width makes this a factory control room centerpiece while its three-block height leaves it comfortable indoors.

**Textures:** Graphite frame, cool ivory desk, mint status bars, cyan screens and small amber warning lamps. Screens use large legible block diagrams, not dense microscopic text or a full glowing wall.

**Motion and interfaces:** Use a few bounded display changes for available grid information. Historical graphs and coordinated remote commands remain future functional design; visual planning does not claim they already work.

### Heavy Fuel Locomotive Generator

**Size:** 6×3×4 blocks. **Form:** Unbuilt stationary heavy-fuel generator roadmap candidate.

**Model:** A long stepped engine hood sits above a massive generator skid. Six repeated cylinder-head covers run beneath an upper spine; a tall radiator occupies one end and a round alternator housing fills the other. Wide side grilles expose dark recessed cooling fins, with a bronze starter assembly and short capped exhaust tucked into the rear shoulder. A small sloping control cab makes the silhouette resemble a locomotive power unit without adding wheels or assuming a moving vehicle.

**Textures:** Deep olive hood, charcoal grille cavities, cool steel ribs, bronze service fittings and restrained amber instruments. Repeat clean cylinder-cover seams and one thin cream side stripe.

**Motion and interfaces:** A guarded cooling fan turns and the engine vibrates subtly under load, with short contained exhaust feedback. Heavy-fuel rates and its place beside existing engines still need gameplay selection; its appearance does not create a new fuel tier.

### Pharmaceutical Processing Bench

**Size:** 3×4×3 blocks. **Form:** Unbuilt optional pharmacy roadmap candidate; effects and precursors remain to design.

**Model:** A clean workbench supports a small jacketed reaction vessel, a short condenser coil and a closed filling cabinet. Three shallow ingredient drawers sit beneath the worktop; a compact overhead rail carries a stirring head over the vessel. The filling area has a recessed tray behind a dark inspection panel and a tightly grouped output rack. Keep the apparatus substantial and industrial, with proper flanges and support posts, rather than a collection of floating glass bottles.

**Textures:** Ivory ceramic surfaces, graphite base, pale copper fittings and muted violet labels. Use clean circular lid markings and only a few cyan-lit inspection details.

**Motion and interfaces:** Stirring and filling move during defined future batches. Shared chemistry provides applicable preparation; this candidate does not decide medicine effects, potion duplication or mandatory magic ingredients.

### Deep Mineral Drill

**Size:** 6×6×6 blocks. **Form:** Unbuilt deeper-resource roadmap candidate; new deposits and reach remain proposals.

**Model:** A massive four-column gantry surrounds a central guided auger. Its upper crosshead carries a large drive housing, with a side counterweight tower and a front control balcony inside the footprint. A wide collar frames the lower drill head, while a rear enclosed conveyor chute leads to a capped output hopper. Preserve the recognizable existing deposit-drill family in the drive, rails and auger proportions; the expanded supports should make it feel like heavy civil engineering.

**Textures:** Gunmetal auger, olive frame, ivory column markings and bronze gear covers. Long clean rails and large hazard bands at the head convey scale; avoid a noisy patchwork of bolts.

**Motion and interfaces:** The auger rotates and the crosshead travels only inside the six-block height. Underground work can be represented at the collar; animation does not require placing cosmetic geometry beyond the envelope or decide new world generation.

### Automated Seed Planter

**Size:** 3×4×3 blocks. **Form:** Unbuilt optional field-work configuration of the farm-machine roadmap.

**Model:** A sloped seed hopper rises behind a low control cabinet. Beneath it, two enclosed seed-meter drums sit beside a guarded shaft; a folded distributor arm nests along the rear edge and ends at a short downward outlet. Two broad skids make the machine look planted at the edge of a field. A front ingredient drawer and a small indexed crop wheel identify its purpose without borrowing the existing harvester's cutting head.

**Textures:** Cream hopper, dark green frame, bronze meter caps and warm wood seed-drawer faces. Use simple seed silhouettes on the drawer labels and a pale crop-band indicator.

**Motion and interfaces:** The meter drum indexes for a bounded operation. Field area, ownership, planting semantics and shared harvester/farm capabilities require later design; the folded arm does not sweep beyond the footprint.

### Fertilizer Spreading Station

**Size:** 3×4×3 blocks. **Form:** Unbuilt optional field-work configuration for the farm-machine roadmap.

**Model:** A twin-pocket hopper rests above a compact guarded distributor. Two clearly separated feed throats join a low mixing chute; a front gate handle and a rear capped distribution manifold give it a practical agricultural silhouette. The lower frame carries broad legs and a clean collection recess. Show a granular metering wheel through a small dark inspection panel, and a liquid connection on one side only if a future recipe genuinely supports that feed.

**Textures:** Off-white hopper, olive lower frame, bronze gate wheels and charcoal hoses. Distinct nitrogen/phosphate label bands connect visually to the selected fertilizers without making labels into ingredient art.

**Motion and interfaces:** Metering turns during a bounded application; no continuous cloud of loose particles. Crop groups, costs and area handling remain to specify, with ordinary manual fertilizer use preserved.


## Shared process coverage

A processing role can use different recipes, removable fixtures or a compatible larger body without becoming several mandatory machine registrations. This mapping accounts for the remaining named processes in the linked plans.

| Planned work | Visual body or configuration in this specification |
| --- | --- |
| Clay/refractory blanks and ceramic molds | Forming dies at the shared press; Ceramic Kiln; Grinding and Polishing where required |
| Crop milling, mashing, starch and pulp preparation | Crop Processor, Mill and Pulper and Heated Mixing Kettle |
| Fertilizer formulation and suitable granular forming | Shared reactor/kettle and Fibre and Pellet Forming Press; optional spreading candidate |
| Fermented ethanol and demanding dehydration | Industrial Fermenter and Chemical Distillation Station with appropriate separation tooling |
| Metal screening, grading and concentration | Screening and Washing Plant, shared milling, Gravity Concentrator, Magnetic Drum and Electrostatic Separator |
| Phosphate, bauxite, fluorite and other compatible digestion | Chemical Dissolution Chamber, Filter Press and appropriate shared thermal/recovery vessels |
| Titanium purification, reduction and cleanup | Chemical Distillation Station, Sealed Reduction Retort, integrated paid salt recovery and compatible shared melting |
| Aluminum and magnesium electrolysis | Suitable independently reachable shared-cell configurations and the Molten Salt Electrolysis Hall |
| Rare-earth fractions | Chemical Separation Rack and shared filtration/precipitation |
| Sulfuric contact conversion, methanation and named gas reactions | Chemical Oxidizer, Chemical Infuser, shared reactor and gas-cleaning rack with the required reusable tooling |
| Acids, absorption, peroxide preparation and reagent purification | Compatible shared electrochemical/reactor configurations and Reagent Purification Station |
| Photoresist formulation and chip wet chemistry | Compatible kettle/reactor and purifier; Wet Processing, Lithography, precision cutting/finishing and Module Assembler |
| Synthetic crude and finished transport fuels | Synthetic Fuel Reactor Train plus existing refining/upgrading capabilities; no second gasoline identity |
| Ordinary plastics, PTFE forms and clean offcut recovery | Existing/bulk polymerization, shared Molding Press/Extruder and paid sorting/preparation |
| Solvent recovery, liquid settling and spent-mixture treatment | Shared Distillation Station, Filter Press and reactor/neutralization configurations |
| Captured dust and appropriate gas scrubbing | Particle Collector and Gas Cleaning and Separation Rack |
| Remelting retired equipment or metal scrap | Disassembly and Scrap Preparation feeding compatible shared alloy/foundry equipment |
| Optional brewery/cement/coke CO2 recovery | Capture port/tooling on compatible equipment plus shared tanks and gas preparation |
| Pollution information | Independently planned portable map UI; it is not made into a two-block mandatory machine |

The first sulfuric contact bed keeps an earlier independently reachable vanadium-bearing iron-concentrate supply. Primary cobalt has its own selected ore source. Geometry does not introduce a PTFE/HF self-construction gate, a compulsory finished advanced chip for its own first production, routine catalyst/filter/lining replacement, or manual gas pressure management. Proposed bodies extend the shared capabilities named in the chemical catalog; final IDs and registration boundaries remain an implementation decision.

## Texture sources and provenance

Use the owner's [shared asset library](../../art/owner-library/README.md) for direct use, recolouring or shape/palette reference as authorized. Originals remain intact. The [catalog](../../art/owner-library/catalog/README.md) records dimensions, companions and hashes. Useful starting references include:

- [Combining factory front](../../art/owner-library/originals/Blocks/factory/combining/combining_factory_front.png) and its [active face](../../art/owner-library/originals/Blocks/factory/combining/combining_factory_front_active.png): dark broad panels, restrained coloured trim and a lower hazard strip.
- [Purification chamber front](../../art/owner-library/originals/Blocks/purification_chamber/front.png) and [active face](../../art/owner-library/originals/Blocks/purification_chamber/front_active.png): a reference family for shared processing openings and running states.
- [Precision sawmill front](../../art/owner-library/originals/Blocks/precision_sawmill/front.png) and [active face](../../art/owner-library/originals/Blocks/precision_sawmill/front_active.png): suitable mechanisms/panels to inspect for the cutting and timber stations.
- [Formulaic assembler front](../../art/owner-library/originals/Blocks/formulaic_assemblicator/front.png): a candidate reference for the circuit/module assembly family.
- [Machine casing side](../../art/owner-library/originals/Blocks/machinecasingblockside.png): clean vertical ribs and grey panel depth.
- [Fluid pipe](../../art/owner-library/originals/Blocks/fluid_pipe.png) and [valve connector](../../art/owner-library/originals/Blocks/pipe_valve_connector.png): pipe/connection palette references.

These are reference candidates, not a claim that their pixels have been installed on any new model. Record exact source paths and recolour/crop changes when making actual textures, preserve applicable animation sidecars, and inspect the frame layout. For example, the casing top source is a **16×416 strip** and a sawmill back source is **16×48**; neither should be stretched as one whole stationary machine face. Owner art can inspire other specialties as well as machinery.

The [factory implementation plan](industrial-factory-implementation-plan.md), [construction and operation record](industrial-machine-construction-and-operation.md) and [production lines and layouts](industrial-production-lines-and-factory-layouts.md) develop the functional handoff from these art briefs. They retain the 2–6-block envelopes and distinguish new operating defaults from owner-selected directions.

## Model handoff and verification

For every built form, prepare a front/side/top block-grid plan first, then a detailed model with named moving groups, finished UVs, closed geometry and all parts inside the chosen envelope. Include the scale in the model handoff. Bulk variants preserve proportions and the familiar family mechanism; added bays and modules use their explicitly described arrangement. Keep separate surfaces slightly offset to avoid coplanar flicker, and make visible vessel interiors credible.

The description permits future bounded cosmetic motion; it does not establish a rendering framework, network state or gameplay feature. Use a small number of reusable visual groups, avoid per-item cosmetic entities, and only animate work the station actually performs. Gauge, fill, light and recipe feedback must read authoritative compatible state. Large doors, arms, rotors and hoppers stay within the envelope at every animation frame.

During implementation, verify bounds and UVs, then inspect front, back, both sides, top and underside in-game; check idle/running states, joined modules, placement orientation and supported lighting. Verify actual multiblock obstruction/formation and persistence separately from visual quality. This documentation pass checks the size range, source/link coverage and repository documentation rules. It supplies no rendered models, new textures or in-game visual test evidence.
