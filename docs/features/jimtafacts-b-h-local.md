# Jimtafacts groups B–H — local implementation ledger

Owner-selected scope, 25 October 2026. Implemented with OpenAI Codex (user-selected
Astra High). This is a **locally implemented and combined-tested native batch**, not a published release. Group A and aircraft
repairs are preserved in the same checkout. Nothing in this batch has been
pushed, released, installed into the personal launcher or deployed to the server.

| Group | Added locally in this pass | Validation / limits |
| --- | --- | --- |
| B — factory/transport | Multipart counterflow exchanger with original geometry; blueprint library; saved-zone planner with clickable map; portable item-filter list; location/area/path survey notebook; inspection goggles; surveyed repeating robot routes; bounded pump scan and retracting original-art tube; seven-stage water gel; removable blockers/facades on passive and active endpoints, 34 original-model lenses/filters, original-art power adaptor and connected endpoint arms; solid/hollow factory facades with saved material and reversible conversion; original paintbrush with finite dye loads and saved pipe-color routing | Refinery comparison recorded below; switched facades use native colored relays; source-colored pipe wires now carry native control channels through the same conduits |
| C — industrial | Ten original-art materials/components with 21 recipes; hot-water/pahoehoe recovery; liquid construction foam and transactional backpack refill; 12-slot radioactive containment box; charged Obscurator; in-place MFE/MFSU upgrade kit; retained cutting blades with hardness requirements, nine material-cutting recipes and interface/JEI guidance; throwable/sticky/placed dynamite and ownership-bound remote; original animated plasma launcher with native thermal pulse; original-art rubber trampoline and bounce sound | Source-registry families now mapped or explicitly classified; combined automated validation passed |
| D — encounters | Three spider hit regions; scoped prey-food consumption; synchronized Keeper rise and separate compressed ghost rendering pass; supplied player statues restored in structures with 23-pose controls; original-mesh Keeper and ice-spider decorative variants | Server/client checks passed; owner visual playtest remains |
| E — magic travel/presentation | Planar Sight fog distances; shared cast/fixed-frame living-entity and owner-thrown item passage with collision/claim/ownership checks; original animated realm sky/cloud/nebula layers and noise border aura; opt-in private-room guest visits | Leashed entities and non-whitelisted vehicles remain unsupported; mounted horses/boats/basic cargo carts now use native rider-preserving transit |
| F — guides | Original-art Workshop Guide, live personal Chronicle and Encyclopedia chapter; nine adapted guide topics and six discovery milestones | Automated checks passed; no external supporter service or progression gate |
| G — spatial | 2³/16³ block exchange now includes up to 64 barrels, hoppers, idle dispensers/droppers, furnaces/smokers/blast furnaces, brewing stands and closed shulker boxes with journaled contents, names, locks and unopened loot; stale source inventories cleared; recovery budget checked before writes | 128³ paged exchange, reservation, cargo/resident identity and durable recovery implemented below; combined server validation passed |
| H — computers | Existing bounded native workshop interpreter plus a terminal program browser with named storage, explicit load, overwrite/delete confirmation and server-confirmed editor replacement | Isolated Cobalt VM, original ROM, virtual files, terminal and peripheral adapters implemented below; standalone and packaged ROM/coroutine/shell, server runtime and client terminal checks passed |

A copied resource, an icon or an alias classification is not proof of completed
gameplay. Combined automated validation is complete. Historical entries below describe earlier states; the dated closure and validation entries supersede their remaining-work notes.

## October 26 costume and decorative statue continuation

Added `chompa_costume` through the existing full-body costume system. Its brown
reindeer suit, hooves, antlers and pink cross-marked hat use the bat outfit's
body-part fit and exact transparent face-opening mask. Existing walking/sneaking/
riding transforms drive it; it adds no powers or replacement player face. The
recipe uses leather, sticks, pink wool and white wool. Both costume tags and the
trunk route include it. Geometry and pixel painters are in the established
`decor14_data.py` / `decor14_textures.py` pipeline, not imported replacement art.
Normal/slim visual fitting remains a deferred validation item.

Added separate Keeper and Ice Spider statues, using the supplied original
creature meshes and matching irons_lib stone textures. Uniform pedestal scaling
preserves model proportions and UVs; the creatures remain still in their authored
neutral shapes. Each uses a player statue plus its encounter reward (cinder
essence or icy fang), reserves two blocks, rotates, respects placement/interaction
claims and drops only once. Player statues retain their original pose/skin menu.
Corrected statue loot to the current singular-condition schema. Original library
files are unchanged and the runtime-copy manifest records both sources.

Three more deferred game tests cover phase persistence/conversion and invariant routing, dispenser ownership/claim/muzzle accounting, and creature-statue persistence. These additions have not compiled or run tests. Testing remains deferred until
the whole requested implementation batch is ready. Everything remains local.

The same continuation adds the original four factory gear sprites and a
wood → stone → shared iron → gold → diamond crafting chain, including an optional
gear-based quarry recipe. It also restores the source scrolling noise border aura
around the actual 16-block pocket rooms; 48 prebuilt UV frames avoid allocating
meshes every render, and camera/room offsets travel in the extracted render state.
Original `noise_tile.png` is unchanged.

Spatial migration now also whitelists vanilla furnaces, smokers, blast furnaces
and brewing stands. Their exact block-entity types, contents, fuel/progress and
other saved metadata move through the existing journal. Menus close before old
inventories are cleared; unloaded chunks, paired chests, other mod machines and
entities remain rejected. A deferred regression covers processor cargo/type/name
restoration. This does not claim the pending 128³ incremental transfer work.

## Playing the local additions when validation is complete

**Factory Paintbrush:** craft with wool, an iron nugget and a stick. Hold dye in
another hand and use the brush to load 64 applications; sneak-use air discards
paint. Use it on a factory pipe to label that pipe. Unlike painted pipes disconnect
for items, fluids and JE, including active endpoints, cached ports and visible
arms. Matching colors connect; an unpainted junction bridges colors. A clean
brush removes pipe paint without returning dye. Existing lens colors remain a
separate route filter; painting a pipe never modifies cargo. The brush also paints
insulated industrial JE cables (labels only, no electrical separation) and vanilla
wool/carpet, concrete/powder, terracotta, glazed terracotta and glass families.
No-op and denied paint applications spend nothing. Uses survive save/load; all
seventeen original brush sprites and the source pipe-border texture are reused.
The border's native core-sized mesh is a render helper, not a survival item.

**Rubber trampoline:** six rubber craft three thin sheets. Each uses the original
16x16 texture, one-eighth-block height and supplied spring sound. Minecraft's
current bounce handling provides 80% restitution and normal-landing protection;
crouching suppresses the bounce and restores ordinary fall damage. This native
adaptation does not port the old unbounded support scans, unsupported-sheet
collapse or jump-key amplification. No extra input binding or independent physics
loop was introduced. Actual bounce/presentation validation remains deferred.

**Portal cargo and fixed frames:** cast portals and fixed frames now use the same
bounded transit method. Players travel as themselves; other living entities need
the current portal owner's authority, including existing pet ownership checks.
Owner-thrown item entities can pass while their owner is present. Unattributed
world loot and another player's dropped items do not get swept into a portal.
Minecraft's native move retains identity, contents/components and pickup delay;
no copied-stack spawn is used. Both source footprint and arrival footprint must be
loaded and permitted, and arrival must be clear and dry. Shared two-second cooldown
prevents immediate return. At most sixteen candidates are processed per pulse.
Leashed entities, non-whitelisted vehicles and private pocket-room transit
remain excluded from generic portals; explicit pocket-room invitations use their own admission path. Cross-dimension behavior still needs the final runtime tests.

**Pocket-room guests:** `/pocketroom invite <player>` records up to sixteen
explicit invitations. Guests opt in with `/pocketroom accept <host>` while the
host is online and inside their own existing room. Acceptance checks combat,
source permissions and a safe arrival; each guest keeps their own return point.
Guests may look around, but cannot build, access blocks/containers, use equipment,
interact with entities, damage creatures or pick up dropped items. Existing owners
retain normal room access. `/pocketroom revoke <player>` or `clear` cancels access;
guests return home within ten ticks if their host leaves, disconnects or revokes.
`/pocketroom leave` and the existing exit frame return visitors safely. Invitation
UUIDs persist on the host; a visit never reallocates or regenerates a room. This
protocol still needs the final multiplayer/security tests.

**Factory facade variants:** craft a sampled cable facade alone to make a solid
factory facade. Craft that alone to switch solid/hollow; conversion preserves its
material and consumes one facade, never the whole input stack. Solid covers seal
item, fluid and JE connections, including cached direct ports. Hollow covers use
four rails around an eight-pixel opening and permit connections. Both can be
removed empty-handed while sneaking and reskinned with the charged Obscurator.
Previously placed shared covers remain cosmetic and keep their old routing;
existing saves are not silently converted to sealed networks. Item previews use
stone with the actual sampled material in the tooltip; placed appearances resolve
the sampled block. Two-state facade appearances are now configured with a dye and a second sampled facade (use glass for a transparent on-state). Place a matching-color relay beside the pipe; existing gates/data cables control that relay. The first facade in crafting-grid order supplies the off-state and hollow/solid mode. Phase changes are cosmetic: collision and solid/hollow transfer policy remain constant, like the source pluggable. Removal, conversion and reload preserve the phase settings.

**Dynamite:** use a normal stick on a solid face to place it unlit, or use it in air
to throw it with a four-second fuse. Sticky dynamite is thrown and fixes itself to
the first block it hits. A placed charge can be lit with flint and steel or
redstone. Sneak-use it empty-handed before ignition to recover it. The remote
links/unlinks your own unlit charges and fires up to sixteen loaded charges within
64 blocks in the same dimension, with a two-second fuse. Links preserve unique
charge identities; replacing a block at the old coordinates cannot inherit a
stale detonator. Offline owners do not cause excavation. Each small blast checks
at most 125 block candidates, respects current claims, and excludes inventories,
fluids, TNT and highly resistant blocks. Sight tests happen before destruction;
blocks behind a wall do not become newly exposed targets within that blast.
Original item and placed-charge textures are copied unchanged; the six-face
placement mesh is native geometry. Sneak-use dynamite in air to bind the stack to your identity before loading a dispenser. Only a present, living authorized owner can fire; source and muzzle claims are checked, blocked muzzles reject the shot, and the later blast rechecks ownership and claims. A rejected launch consumes nothing. Sticky stacks use the same path.

**Plasma launcher:** retains the supplied animated icon and 40,000-JE capacity.
One 1,000-JE pulse reaches up to 32 loaded blocks, with a one-second cooldown.
It either transforms a single furnace-smeltable block in place, evaporates water,
or damages a permitted creature. Only one-to-one solid block outputs qualify;
ore-to-ingot results cannot turn into free block placement. Containers and
forbidden targets are unchanged, without a charge cost. Native impact particles
are used; this is a bounded thermal adaptation, not the source radial influence,
fire placement or an unrestricted heat explosion. There is no durability bar.

**Counterflow exchanger:** place a Start, one to three Middles and an End in a
straight row, all facing in the same direction and owned by the same player.
Each endpoint holds 16 buckets on each circuit. Feed hot coolant to Start and
water/distilled water to End through any non-bottom face; extract cooled coolant
and steam underneath. Each 5-tick pulse exchanges 5/10/20 mB per circuit according
to section count. One mB hot coolant + one mB water yields one mB cooled coolant
and ten mB steam, matching the existing four-heat-unit coolant budget. Both
outputs must have room. Redstone on any part pauses the assembly. No new energy
currency, free power conversion or chunk loading is introduced. Open any part for
the two-circuit status screen. Cached ports and views recheck block identity and
current authority before changing fluids.

**Planning:** the Library opens the existing blueprint browser/print workflow.
Use a completed Volume Marker on a Zone Planner to store up to sixteen areas;
open it for selection, footprint preview, marker copying and removal. Zones retain
full signed coordinates and are limited to 32 blocks along each axis. Owners and
party members still need ordinary claim permission. The Map Location tool records
a spot, area or sixteen-point survey; sneak-use air changes mode. A completed
area can be applied to a civil workshop. Apply a path of 2–16 points to an owned carrier, delivery or fluid-carrier robot.
It follows the path forward to output and backward to input, within 32 blocks of
home. Each movement rechecks loaded space, swept collision and claims. Routes
survive reload; this is a surveyed route, not automatic obstacle pathfinding.

**Filters and inspection:** hold a sample in the other hand and use a Factory
List to add/remove its item type; sneak-use clears the nine-entry list. Samples
are identities, never duplicated stacks. Put the list in an item sorter/extraction
pipe's sample slots or a compatible Nouveau filter position. No source tag/ore
matching modes are claimed. Wear Factory Goggles to read permitted nearby JE or
fluid ports. The original item sprite is supplied; a separate original worn
model is not present in this collection.

**Factory pump:** scans downward through up to 32 loaded, permitted air blocks,
then at most 64 connected pool positions within eight blocks. It cannot scan
through a wall or protected boundary. Up to 100 mB moves per five-tick pulse for
8 JE; finite source liquids require one bucket of available buffer space. Water
retains Jugcraft's renewable spring rule. Existing non-factory pump rates remain
unchanged. The original tube texture now renders a retracting/expanding intake
column, bounded to 32 blocks and interpolated over five ticks. It is visual
geometry, not additional world blocks or an extra fluid source.

**Water gel:** craft a seed from four sand and existing asphalt binder. Use it
on source water within seven blocks. Its four spreading stages convert at most
three neighboring sources each, scanning at most 64 positions/128 discoveries
within twelve blocks of the original seed. Every change checks loaded chunks and
current owner permissions; absent owners pause the process. The final stages wait
until adjacent water is gone, then set. All seven stage textures and both item
sprites are supplied originals. Breaking a gel block gives one portable gel;
craft it with a bucket for water. Original random bonus drops and the larger
legacy scan are deliberately omitted. This is not terrain regeneration.

**Materials:** mixed-metal ingots compress into advanced alloy plates. Expensive
thermal processing of nine washed uranium ore produces eight uranium dust plus
one iridium shard; nine shards make one refined iridium. Four iridium plus four
advanced alloy plates and a diamond form an iridium composite plate. This is a
native recovery route, not a claim of unchanged legacy ore generation. Alloy
thermal processing also provides slag, which can recover stone dust and a small
iron remainder. Grin powder comes from poisonous potatoes/spider eyes and feeds
Weed-EX. Original iron/steel/diamond blade sprites now have component recipes and
alternative cutting-mill assembly recipes; blade-specific machine hardness limits
remain pending. Existing recipes are preserved.

**Heat and construction fluids:** the existing Chemical Reactor heats water
into hot water and quenches recovered pahoehoe lava into basalt plus hot water.
Liquid heat exchange reserves outputs before consuming inputs. Construction foam
mixes one powder and 1,000 mB water into 3,200 mB foam, matching the existing
32-block canister yield. A foam backpack takes or returns whole 100 mB doses via
tanks; its existing saved charges and partly-used canisters remain compatible.
Pipes and universal cells carry the fluid; the cell uses the original CF sprite.
Placed liquid does not harden automatically: use either sprayer for curing blocks.

**Containment:** the original-art twelve-slot box accepts radioactive material,
rejects nested containers and attenuates its contents' measured radiation by 99%.
The native menu uses the original GUI texture, persists contents and rejects stale
held-item sessions. It does not make the player immune to other radiation sources.

**Statues:** the supplied `Oct25/irons_lib` files resolve the previous missing
model dependency. Original wide/slim/legacy geometry, pedestal and all 23 poses
are now integrated. Six structure templates again contain their statues (14
restored palette entries), with seeded poses and original placement/rotation.
Right-click for pose selection or mirroring; a name tag naming an online player
copies their UUID. Rendering converts available vanilla skins to a bounded stone
palette; offline profiles use vanilla default appearances. The source clustering
algorithm/stone overlay and supporter service are not used. The original Sbeve
model/texture is the fallback. Creature statue textures are retained but are not
separate registered variants. Both halves respect claims; existing chunks are not
silently rewritten.

**Realm sky:** original sky and cloud textures now drive the realm cube, six
rotating cloud layers and 45 bounded nebula clusters. Render resources are cached
and released. Border aura remains unfinished; runtime visuals remain untested.

**Encounters/travel:** spider food consumption is scoped to food dropped by its
own killed animal; nearby player food and nonfood loot are untouched. Hit-region
entities are transient and forward damage to the parent. Keeper rise data is
saved and synchronized. Ordinary paired portals can move permitted living
entities with native identity/equipment preservation, but refuse someone else's
pet, rider trees, leashes, blocked arrivals and unsafe/unloaded destinations.
Private pocket rooms retain owner-only access. No automatic chunk loads.

**Guides:** portable readers use native written-book pages; the Chronicle checks
actual inventory and pickup/crafting/use statistics. Discovery is informational,
not a requirement to use equipment. The adapted guide links the supplied topics
to Jugcraft's Focus/workshop systems instead of promising absent legacy mechanics.

## Source inventory and provenance

### October 25 transport / industrial / spatial / computer continuation

- **Pipe fittings:** use a Pipe Blocker on a factory item, fluid or power
  pipe face. It closes both the connection arm and the real transfer edge. The
  existing sampled facade item covers these same pipes. Sneak-use a face with an
  empty hand to recover its fitting. Six faces persist independently and return
  their actual items when removed. These pipes now have block entities and are
  consequently not piston-movable. Wooden/diamond-wood extraction, sorting, color
  routing, obsidian/stripes and disposal endpoints now host the same fittings.
  Their existing block entities retain real cargo, filters, ownership and fluid
  buffers; fitting-only client packets never clear those inventories. Active
  endpoint arms now reflect actual connections rather than six permanent arms.
  Cached storage/views, hopper access and direct source/output operations check
  the current face and block identity. Internal cargo work has no fictitious
  bottom face, so plugging DOWN does not inadvertently stop all other outputs.
- **Lenses:** 17 colored/clear lenses and 17 matching filters use the supplied
  lens mesh, UVs and textures. They fit through-routing factory item pipes,
  including lapis, daizuli and emzuli. A lens sets a
  route color; a filter accepts only that color. A clear lens clears a color; a
  clear filter accepts only uncolored routes. Exit and entry fittings apply in
  order, including injection from an adjacent inventory. Cargo components are
  never dyed or rewritten. Routing remains bounded native instantaneous delivery.
  Buffered extraction/disposal endpoints do not accept lenses.
- **Power adaptor:** one removable original-art fitting on a wooden or
  diamond-wood item/fluid extractor. Its face accepts JE cables and excludes
  item/fluid connections. A saved 2,048-JE buffer accepts at most 256 JE per
  insertion. Extraction pulses can move up to four times the ordinary batch,
  charging 8 JE for each extra item or started 25 mB moved. Blocked output spends
  no charge; no power means ordinary base extraction. Removing/breaking returns
  the fitting with its remaining charge. Cached power ports stop accepting when
  the fitting is removed. Original parts/UVs and pixels are retained, with a
  short stem seating the adaptor/blocker against the narrower native pipe core.
- **Cutting blades:** put an iron, steel or diamond blade into an existing upgrade
  slot in either sawmill or Industrial Cutting Mill. Blades are retained and do
  not add their strengths together. Source material tiers 2/5/8 gate nine new
  material recipes: lead/gold/bronze/tin/copper blocks, iron/lapis blocks, and
  steel/obsidian respectively. The machine status shows installed/required
  strength; JEI shows which blade is needed. A missing or insufficient blade
  cannot bank progress or consume inputs/power. Existing wood recipes need no
  retrofit. Metal blocks yield nine plates; obsidian remains four blocks per
  plate and lapis four blocks per nine plates to preserve native material costs.
  Blade inventory uses ordinary machine persistence and removal, not a parallel
  hidden tool inventory. The additional recipe codec/menu fields require matching
  client/server mod versions when this batch is eventually released.
- **Obscurator:** the original tool sprite has a reachable recipe. Sneak-use a
  solid block to sample its appearance for 20,000 JE; use on an existing sampled
  facade to reskin it for 5,000 JE. Repeating an unchanged appearance costs nothing.
  This copies whole-block appearance rather than the source's per-face pixel
  patterns. It does not create facades or copy block behavior/inventories.
- **MFSU Upgrade Kit:** use on a compact MFE or MFE Charge Pad. One kit upgrades
  it in place, preserving facing, charge, policy, ownership and compatible slots.
  The replacement decodes before mutation; old inventory/energy handles are
  emptied, and a failed replacement restores its snapshot. Shared native JE
  capacities remain authoritative. No upgrade of unrelated multiblock batteries.
- **Spatial inventories:** up to 64 vanilla barrels, hoppers, closed shulker boxes,
  furnaces/smokers/blast furnaces, brewing stands and idle dispensers/droppers can join a 2³/16³ exchange. Powered or pending dispenser/dropper pulses
  must finish before capture. Each new payload records its exact container type;
  old unmarked payloads are accepted only for barrels. The
  revision-fenced server ledger and write-ahead recovery journal preserve slots,
  item components, custom names, lock data and unopened loot. Old inventory handles
  are cleared without destruction drops. Restored barrels close stale open states.
  Hopper cooldowns remain in the saved inventory data. Open container menus close
  before detachment; outgoing scheduled block callbacks are cleared before
  restoring contents and notifying neighbors. Missing/duplicate/misplaced or
  cross-type payloads are rejected before changes. The decoded
  recovery size limit is also checked before writing a journal. Other containers,
  mod machines and entities remain excluded; this is not 128³ migration.
- **Computer programs:** open **Programs** in a computer/turtle terminal. Select a
  saved name or enter a new one, then Store, Load or Delete. Four entries per page;
  the existing limit is sixteen files/65,536 characters. Store saves the editor to
  the shelf without replacing a running program. Load stops execution and never
  autoruns. Overwrite, deleting and discarding a changed editor require a second
  click. A failed server load retains the draft; only a successful acknowledgment
  replaces it. All requests remain bound to the current authorized menu. This is
  the native Workshop OS, not Lua/CraftOS compatibility.

Originals remain untouched under `art/owner-library/originals/Oct23/Jimtafacts`.
New copy manifests: `factory-heat-runtime-copies.json`,
`planning-runtime-copies.json`, `field-tools-runtime-copies.json`,
`guide-runtime-copies.json`, `statue-runtime-copies.json`,
`water-gel-runtime-copies.json`, `transport-attachment-copies.json`,
`transport-lens-copies.json`, `transport-power-adaptor-copies.json`,
`transport-facade-adaptation.json`, `dynamite-runtime-copies.json`,
`paintbrush-runtime-copies.json`, and extended gear/heat/production/utility/industry manifests in
the collection catalog. Source class inspection used trusted CFR/javap only;
no supplied program, script or JAR was executed. The owner-supplied
`Oct25/irons_lib` import preserves 216 files and hashes in
`irons-lib-source-files.json`. Original source files remain unchanged.

The updated 612-entry inventory records 420 native adaptations, 77 consolidated
entries, 43 state/color/material tokens, 65 unresolved entries, five explicitly deprecated
source IDs and two unused declarations. This includes classification corrections,
**not 420 distinct newly implemented machines**. Family notes retain limitations and unimplemented source variants.

## Validation state

- October 26 continuation: paintbrush/routing, additional spatial inventories,
  trampoline and shared portal cargo/frame transit are implemented locally.
  **No compilation or tests have run for these edits.** Resource generation and
  static source inspection are implementation work, not validation evidence.
- Seven additional deferred cases cover brush charge/claim behavior, paint-edge
  persistence/cached-port revocation, transport-container round trips, scheduled
  pulse/cross-type snapshot rejection, trampoline shape/fall handling, and portal
  cargo conservation/cooldown plus rejected ownership/claim/blocked arrivals.

- Latest facade/dynamite/plasma continuation: common, client and game-test sources
  compiled successfully in `build/b-h-facades-equipment-compile.log` (54 seconds,
  October 25). A remote-link generic-inference compile error was corrected before
  that successful pass. Existing compiler warnings remain. These are compilation
  results only; the five new cases have not executed.
- The earlier batch compiled in `build/b-h-compile-current-local.log`. This
  previous continuation is recorded in `build/b-h-transport-spatial-computers-compile.log`.
  The latest active-endpoint/adaptor/blade continuation is recorded in
  `build/b-h-active-fittings-blades-compile.log`: common, client and game-test
  sources compiled successfully after the final code edits (34 seconds, October 25).
  An initialization-order compile error was corrected. Existing warnings remain.
  Compilation and whitespace checks are not gameplay tests.
- Nineteen deferred regressions are authored in `JimtaFactoryExpansionGameTests`:
  heat conservation/output blocking, owner/cached-port revocation, saved zones,
  spider regions/food, Keeper persistence, filters, portals/pets, robot routes,
  containment persistence/radiation, statue persistence/claims, foam transaction
  rollback/whole-dose conservation, gel claims/ownership/setting, MFSU migration
  and Obscurator claim/energy accounting.
- Three additional transport cases cover plugged item/fluid/power edges, lens
  injection/filtering and persisted clear-lens semantics. Two spatial cases cover
  barrel cargo round trips/stale handles and malformed payload rejection. One
  computer case covers saved-program operations and rejected-write conservation.
- This continuation adds five deferred cases: active cargo/filter preservation
  with cached port/view revocation, both sorter faces and cached disposal,
  powered extraction/aborted charging/blocked-output conservation, blade tier
  rejection and persistence, and native obsidian cost conservation. They compile
  but have not run.
- The next continuation adds five more deferred cases: solid/hollow facade
  persistence and cached-port sealing; conversion conservation; remote ownership,
  claim and charge-identity checks; blast container/claim/resistance protection;
  and plasma transformation/energy conservation. No test execution yet.
- The structure-layout regression now checks restored statues and orientation,
  replacing its obsolete expectation that these decorations become air.
- **No B–H game tests, client visual tests or human playtest have run.** Execution
  remains deferred until the requested combined implementation is ready.
- Group A's prior 157 server checks, eight Python checks and seven-aircraft client
  run are separate evidence; they do not validate these new changes.

Before release, complete the remaining rows above, run combined resource/server/
client checks, inspect original models in-game, then prepare a local playtest pack.
Do not publish or restart the live server as part of this local batch.

## October 26 mounted travel and final component continuation

Generic portals now admit a bounded mounted group (eight entities maximum), using
Minecraft's own rider-preserving teleport path. The rider acts as themselves;
another player's passengers or tamed mounts are rejected. Boats and basic,
storage, hopper and furnace minecarts require a present rider; unattended vehicles,
explosive/command carts and arbitrary aircraft are excluded. Every traveler needs
permitted loaded source and destination footprints and receives the same cooldown.
A deferred regression covers owned horse movement and foreign-owner refusal.
Cross-dimension riding still needs actual runtime validation.

Invited pocket-room arrival uses an explicit presence-only check after validating
the invitation and online host, instead of the ordinary teleport helper's edit
permission requirement. Building/container rights remain denied. A deferred real
two-player regression covers entry, owner/guest rights, revocation and return.

Original pipe-waterproofing art now supplies a reachable rubber/green-dye sealant.
Each compatible item pipe can be lined into its fluid variant one-for-one; stripping
returns only the pipe, never the consumed sealant. Existing direct recipes remain.
The source plastic-sheet entry consolidates to Jugcraft's existing polymer chain,
not a duplicate material.

Spatial exchange also accepts closed vanilla shulker boxes, preserving the exact
colored block ID and inventory. Open/closing lids are refused until closed. A
deferred regression covers color mismatch refusal and stale-inventory cleanup.
All these changes are local and uncompiled/untested, as requested.

## Refinery comparison and intentional consolidation

Static inspection of the supplied `BCEnergyRecipes` found ten staged distillation
splits across three heat states, plus corresponding heating/cooling recipes.
The source represents mixtures (gas/light/dense/residue) and their temperatures
as separate fluids. Jugcraft already has a connected chemistry chain and a single
JE economy, so the native integration retains that chain instead of adding a
second set of incompatible oil intermediates:

| Supplied role | Existing native route | Difference retained explicitly |
| --- | --- | --- |
| Separate crude into useful fractions | 1,000 mB crude → 100 gas + 250 naphtha + 400 diesel + 250 heavy fuel in the distillation tower | One multi-output fractionation instead of ten staged pair splits |
| Recover heavy residue | 1,000 mB heavy fuel → 400 lubricant + two asphalt binders | Native chemical quantities; solid residue has its established construction use |
| Heat/cool refinery streams | Original multipart counterflow exchanger and shared heat/fluids | Native temperature/energy handling; no duplicated legacy MJ/temperature-named fluids |
| Produce plastics | Shared polymer processing and `plastic_sheet` | Source sheet is consolidated; existing recipes and art are retained |

This completes the behavior comparison, not exact legacy refinery parity. The
recipe paths are `data/jugcraft/recipe/distillation/crude_oil.json` and
`heavy_fuel_oil.json`; no yields were silently changed during this comparison.

## Hydrating cell

The supplied `itemCellHydrant.png` is now the `jimta_hydrating_cell` item. Craft
from a tin plate, glass and rubber. Its shared transactional cell storage accepts
water only, up to 1,000 mB; tank fill/drain and the existing fluid bottler work with
it. Use on crops or farmland to restore the soil moisture to seven for 25 mB.
Already-moist soil, protected soil and failed changes spend nothing. It does not
bonemeal plants or invent a parallel crop hydration counter. The original pixels
are unchanged. A deferred conservation/claim/non-water rejection scenario was
authored; it has not run.

## Industrial TNT, powders and cell artwork continuation

Industrial TNT uses the supplied six-face atlas with the recovered side ordering.
Its placed charge retains an owner and identity, supports manual/redstone ignition
and the existing owner-only remote, and uses a bounded four-block mining burst.
Current claim checks still apply and inventories, fluids and high-resistance blocks
are excluded. This is a stationary native charge, not an unrestricted nuclear
explosion. Its deferred regression covers remote ownership, protected ground and
nearby inventory preservation.

Six original-art powders (bronze, obsidian, lapis, silica, diamond and energium) and
eleven small piles now have native recipes. Dividing and combining conserve nine
small piles per whole item. Shared crushing/compression supplies bronze recovery,
obsidian/lapis plates, silica glass and energium energy-crystal production. Lithium
continues to mean the existing carbonate resource; the new small pile is explicitly
named accordingly. No duplicate incompatible lithium-processing chain was added.

The universal fluid cell now selects the original UU-Matter sprite when filled
with that fluid, retaining its existing conserved storage and tank operations.
The upgrade-kit, recipe-part, ingot, plate and crushed-ore registry containers are
classified only when every declared metadata member has an explicit native mapping.
Those containers are not additional missing items.

All additions remain local and uncompiled/untested. Asset generation and static
source inspection are implementation work, not validation passes.

## Fragile fluid shards

The supplied base/fluid layers now back a craftable single-fill 500 mB shell.
Glass pane plus paper makes two empty shells. Once filled (including a partial
fill), a shard only drains; completing the drain consumes it in the same Fabric
transaction. It works by using tanks or through the fluid bottler. Fluid appearance
uses the original neutral overlay; the tooltip names the exact fluid and amount.
Native tank drops continue preserving their full contents instead of being replaced
with dozens of shards. This is an explicit native acquisition adaptation.

The bottler now permits an empty container result after a successful drain; it
previously rolled back transfers whose container was consumed. Nonempty results
still require output space before committing. Deferred regressions cover sealed
partial contents, rejected fluid mixing, aborted final-drain recovery and bottler
conservation with a full output slot. None have run.

## Workshop frames, panels and remaining machine housings

Seven placeable workshop blocks now use supplied art: the connecting structural
frame and blueprint, template, paper, leather, warning and laser-back panels. The
frame keeps the original core/arm coordinates and inward faces. Internal cull
flags were removed so adjacent solid blocks cannot hide a floating inner face.
Six directional connections and collision arms follow actual neighboring frames;
the quarry's moving frame renderer remains independent. Panels are decorative.

Four supplied machine skins now reuse existing processor families and ordinary
housing conversion recipes: induction furnace → electric furnace, blast furnace
→ steel foundry, crop harvester → crop harvester, electrolyzer → electrolytic cell.
They preserve native recipes, energy/fuel, interfaces and automation; they do not
claim legacy EU, dual-slot induction speed or old heat-generator behavior. The
induction housing additionally requires copper wire to avoid ambiguous recipes.

The Cropmatron is a real new care device using its original active/inactive atlas.
It scans one loaded position every five ticks in the same forward 9×9 field layout
as the harvester. Each successful action costs 40 JE: restore farmland for 25 mB
water, advance an eligible crop one age using a real fertilizer/bone-meal item, or
remove a weed with one Weed-EX. Original crop anchor/light requirements remain in
force; cross sticks are never fertilized. Claim boundaries and current owner
authority are checked for soil and crop. Redstone pauses it, and owner/party members
can open its supply inventory; pipes fill its water-only 4,000 mB tank.

Deferred scenarios cover protected/no-op conservation, watering, weed treatment
and frame connectivity. All of this remains uncompiled and untested.

The supplied sorting-machine and item-buffer atlases now have actual registered
blocks, shared block entities and reversible housing recipes. The sorter retains
the native nine-sample filter with one directional outlet, rather than claiming
the original six independent face filters. The buffer shares the owned nine-slot
chute's configurable outputs, intake, redstone pause and finite powered boost.
Its stored cargo, energy and owner survive save/load through the same serializer.
Frame collision shapes are cached for all 64 connection combinations rather than
rebuilt during every collision query. These changes have not been tested yet.

## Advanced miner

The original advanced-miner atlas now backs a native surveyed excavation device.
It uses the existing bounded civil-work area, tool/enchantment requirements,
claim checks, output reservation and 256 JE per successful block. Thirteen real
filter samples remain in the input slots. A clickable Include/Exclude control
works while paused and resets the scan; an empty Include list mines nothing.
Drops are rolled once, then filtered and reserved before destruction. Silk Touch
comes from the inserted tool, not a free machine toggle. The shared area marker
replaces the source charged-scanner range mechanism; this is a native adaptation.
Deferred regressions cover filter modes/sample persistence and the item-buffer
housing's cargo, owner, routing and power serialization. None have run.

## Resin sheet and metal-family consolidation

The original resin pixels now cover a supported two-pixel sheet with no solid
collision, sticky slowing and fall-distance damping. A tapped latex bottle makes
one sheet and returns its empty bottle through the existing crafting remainder.
Breaking the sheet recovers it deterministically instead of introducing the source
random loss. It cannot refund a filled bottle or duplicate glass. The six source
metal-storage variants consolidate to the existing copper/tin/bronze/uranium/lead/
steel storage blocks; uranium retains its native radiation behavior.

## Continuation checkpoint

The current source registry inventory records 612 declarations: 442 native
adaptations, 80 consolidated native entries, 57 metadata tokens, 28 unresolved
entries, three retired IDs and two unused declarations. These are declaration
counts, not percentages of gameplay completion. In particular, large spatial
exchange and Lua/CraftOS still need real implementation regardless of the count.

A same-chat heartbeat (`finish-local-jimtafacts-collection`) was enabled on October
26 to resume this authorized local work every 30 minutes. It must respect later
owner messages and stop on completion or a genuine blocker; it does not authorize
publishing, deployment or premature testing. Local scheduled work requires the
computer and desktop app to remain running.

Next: finish unresolved source families and explicitly document intentional native
consolidations, then implement the remaining G/H systems rather than relabeling
them complete. Read the full ledger and current source before editing. All tests
added during this continuation remain deferred, and no compile has run.

## Reactor materials and early industrial housings — continued locally

Six supplied material sprites now have functioning native routes: uranium fuel
material, MOX fuel material, small plutonium, U-235, small U-235 and U-238. Uranium
fuel material repackages one existing LEU assembly reversibly; adding one recovered
plutonium pellet makes MOX material for one single rod. Existing larger-rod recipes
remain unchanged. Nine small pieces equal one whole plutonium/U-235 piece.

The thermal centrifuge can separate an already enriched cylinder into one U-235
and seven U-238 game units; all eight recombine into one cylinder. This is an
explicit gameplay ratio, not a claim about isotope composition. It does not add
a raw-ore enrichment shortcut or create an extra empty-cylinder item. Fractional
radioactivity sums to the original cylinder/pellet, and raw materials are rejected
by both reactor automation and menu slots. Original sprites are copied unchanged.

The original iron-furnace atlas uses the native furnace block entity, fuel slots,
smelting recipes, XP, menu and sided automation. Its timing remains vanilla; no
legacy speed bonus is claimed. The original basic-miner atlas uses native marked
single-column excavation with actual tool wear, paid power, output reservation
and claim checks. It does not yet reproduce the original retracting drill pipes
or charged scanner branches. The source fermenter consolidates into the existing
controlled-fermentation vessel and its real biochaff recipe rather than adding a
second incompatible fermentation process.

Latest static registry ledger: 612 declarations, 448 native adaptations, 81 native
consolidations, 58 metadata tokens, 20 unresolved, three retired and two unused.
These figures do not measure overall feature completion. G/H work remains open.
Deferred reactor scenarios cover raw-material slot rejection and proportional
radioactivity; they have not run. All edits in this continuation remain local,
uncompiled and untested. Tool: OpenAI Codex, user-selected Astra High.

Computer runtime follow-up: the supplied CobaltLuaMachine invokes the missing
Cobalt VM and its interrupt/suspend facilities; the supplied ROM does contain
BIOS, shell and API Lua files. No Lua runtime has been silently substituted, no
external dependency added, and no ROM execution claimed. The upstream runtime
warns that its APIs evolve with CC:Tweaked; a pinned engine and proper native
API/event bridge are still necessary before marking H complete. Source reference:
https://github.com/cc-tweaked/Cobalt . Furnace inventory/valid-block coverage is
also authored for the final combined run, not executed now.


### October 26 local continuation — spatial processor handoff

The original iron-furnace housing now participates in the bounded spatial
inventory transfer with its exact block identity, fuel and smelting inventory.
Sharing the vanilla furnace entity type does not allow its payload to be restored
as a different housing. Incoming containers are constructed and decoded as a
complete batch before outgoing containers are detached, reducing partial restore
failures. Recovery now parses the saved owner, plot key, cell and energy before
world mutation and rejects a recovery port inside the transferred region.
A deferred game test covers preparation without mutation, housing mismatch and
single-copy cargo handoff. No compilation, tests or game launches were performed.

This is groundwork for G, not completion of incremental 128-cubed exchange,
region reservations, entity migration or arbitrary live machine transfer. Those
remain open, as does the H Lua runtime/API bridge and remaining source equipment.
Existing Group A, aircraft, Chompa and all other dirty edits are preserved. Nothing
was committed, pushed, deployed or changed in personal launcher profiles.
Implementation tool: OpenAI Codex; no model/effort override or delegation.


### October 26 continuation — industrial configuration and survey handoff

Completed the configuration authority path shared by the civil builder, filler,
replacer, quarry, basic/advanced miners, flood gate and terraformer. Both original
field-map and volume-marker interactions now check the machine owner/party,
current claims, player state and proximity before replacing its survey. Menus
use the same live authority check, including ordinary slot clicks, so a stale
open menu cannot bypass revoked access. This closes a path where another player
on public land could repoint equipment which subsequently worked using its owner's
permissions. Existing automatic block mutations retain their per-position checks.

Changing dimension with the survey map clears old corners and path entries before
recording the new position. Loading a civil workshop with a missing/invalid survey
clears its previous coordinates and pauses non-builder work without deleting cargo
or power. Deferred game tests cover configuration ownership, revoked claims,
distance and stale saved survey state. None have run; no compilation or launch.

Inspected the supplied volume-box item: it creates a separate world-saved volume
object, rather than merely being a second survey-marker icon. That runtime is
still outstanding and was not marked implemented through a map alias. The source
mining pipe also still needs actual miner extension/retraction integration. G/H
large spatial exchange and Lua/CraftOS remain open as previously recorded.
All edits stay local. Implementation tool: OpenAI Codex; no delegation.


### October 26 continuation — persistent survey volume gameplay

Added `jimta_volume_box` with the supplied twelve-edge cube item geometry and
original connected-marker texture, a survival recipe and actual dimension-saved
volume records. It creates and edits either corner of a bounded 32³ survey without
placing blocks, selects saved owned corners, applies a saved region to civil
workshops, and supports deselection/deletion. Items retain only dimension/UUID
references; copied or stale items cannot confer another player's edit authority.
Limits are 16 volumes per owner and 1,024 per dimension. Holding the selected tool
shows eight corners and twelve edge midpoints once per second to its holder only,
within 64 blocks and already loaded chunks. Workshop execution still checks land
permissions per changed block. Original-model provenance is in
`volume-runtime-copies.json`; reproducible generator is `jimtafacts_volumes.py`.

This is a functional native survey-volume implementation, not source feature
parity: drag-following laser rendering, connector locks and volume addon execution
are still unfinished. The source registry entry therefore remains unresolved in
the full-parity inventory for now. A deferred ownership/cap/resize regression test
was authored; no compile, test suite or game ran. Existing G/H and mining-pipe
work remains outstanding. All changes stay local, with no deployment or personal
launcher changes. Tool: OpenAI Codex; no delegation or effort override.


### October 26 owner continuation — usable filler plans

The filler now selects Fill, Box, Frame, Floor, Walls, Sphere, Pyramid, Stairs
and Clear from its machine screen, with inversion and saved state. Shape changes
require a paused machine and reset its scan. Geometric skips consume neither
materials nor power. Clear uses actual tools, output reservation, claim checks,
retained drops and paid JE; it refuses containers, fluids and unbreakable blocks.

Added the original-art Filler Planner as a reusable, craftable configuration tool.
It cycles shapes/inversion, applies to a permitted paused filler, or writes its
plan into the selected Volume Box held in the other hand. Dimension-saved volumes
retain those settings across resizing/reloads and apply them with their region.
Existing volume records default to Fill/normal. The supplied planner cube model
and addon texture are preserved in runtime copies and the generator manifest.
These are native bounded shapes, not a claim that all original parameterized
2D polygons, axes, hollow modes and arbitrary statement expressions are complete.
Connector locks, drag editing, large spatial transfer, Lua/CraftOS and remaining
industrial equipment still need implementation. Source declaration counts were
not relabeled as full completion.

Deferred regression scenarios now cover exact shell/frame/inversion boundaries
and Clear's real drops, tool wear, JE and container protection. No compile/tests
or game launch occurred. Nothing was committed/pushed/deployed. OpenAI Codex.


### October 26 continued implementation — physical miner drill column

The original basic miner now consumes crafted mining pipes to extend a physical
column directly below its body. It uses the supplied pipe/tip sprites and source
pipe dimensions; the tip becomes a rod as the next segment advances. Actual tool
wear, reserved mined drops, JE, current claims and loaded-block checks apply.
A paused machine can retract one saved segment per work step into its output
inventory for 32 JE. A full output or revoked access pauses recovery; replacing
the survey is refused while rods remain. Manually broken/missing segments are
not refunded again. Breaking the machine leaves independently recoverable rods;
both rod and tip breaking yield one pipe. Deployed positions and retraction state
persist, bounded to a 32-block vertical column. Legacy mining-well behavior is
unchanged. Charged ore-scanner branching remains outstanding.

Added a deferred deployment/retraction conservation regression. No tests or
compilation run. Source inventory mapping updated, no completion-count generator
run and no release actions. All earlier dirty edits preserved. OpenAI Codex.


Persistent volume plans now bind to one civil workshop with a saved machine
reference. Binding refuses a running/retracting machine; editing/removing a linked
volume is locked. Owners disconnect using the volume tool with a Volume Marker
in the other hand, near the loaded paused workshop. No chunk loading is used.
Changing the machine's survey through another authorized tool or removing the
machine releases its binding. Machine-side UUID persistence distinguishes a
replacement block from the original consumer. This implements the native consumer
lock rather than treating a cached copy of corners as a live linked region.
Original free-drag laser editing and source parameterized addon parity remain open.
No validation has run. Owner requested silence until whole-collection completion;
automatic continuation instructions now honor that explicit notification rule.


### October 26 continued implementation — factory diagnostics

Added the original power-test cube and debugger icon as functional native tools.
The JE Load Tester is a bounded consuming load with saved committed-input totals,
last-tick readings and a finite 1M JE buffer. Redstone pauses insertion even through
cached storage handles. Transaction simulations/rollbacks do not change its
counters. The Workshop Diagnostic Probe reads authorized block IDs/face energy,
civil-workshop state and tester counters without exposing another owner's machine
or a global mutable debug target. Both have survival recipes and original asset
provenance. Deferred transaction/pause regression authored, not run. No build,
launcher, release, Git push or server actions. Native JE replaces legacy MJ.


### October 26 continuous local work — connector and field guides

Removed the scheduled continuation as requested. The Marker Connector now edits
an owned unbound volume corner through a transient aim preview with explicit
confirm/cancel. Confirmation rechecks access and unchanged source volume; moving
away, disconnecting, dying or switching tools cancels the draft. Source icon and
survival recipe are supplied. Preview uses bounded particles, not legacy lasers.

Added original-art Factory Field Guide and Topic Note, nine native instruction
chapters covering the implemented surveys, filler, mining pipes, pumps, filters,
power and ownership. Notes retain a selected topic; holding one selects the guide
bookmark. Sneak-use cycles topics without typing. Both have survival recipes and
bounded portable vanilla readers. No equipment learning gate. This is a native
manual, not execution of the incompatible old GUI/recipe engine.

No build, tests, launch, commits, publishing or deployment. Full collection remains
in progress, particularly large spatial exchange, Lua runtime and industrial
source remainder. OpenAI Codex.

Added the original iron trade token art with conserved 9:1 iron conversion. These are physical barter tokens, not minting of account currency. Resource generator and registry catalog include it. No validation run.


### October 26 continuous local work — sixteen pipe-wire channels

Added all sixteen original wire icons and installed conductor textures. Wires
consume one real item per color per factory pipe and can be recovered by
sneak-use with that color. Saved fittings synchronize them, drop each once and
render seated conductors with arms only toward connected wires/data devices.
Native control-network traversal now intersects channel masks along the route;
red cannot leak into blue across a missing conductor. Ordinary data cable carries
all channels, and traversal remains limited to 1,024 loaded conduit positions,
with bounded revisits for alternate color paths and current claim boundaries.
Cargo routing, paint and lenses remain separate. No legacy signal attenuation
is claimed. Authored deferred isolation, reconnection and persistence regression;
registered it and the earlier deferred volume/diagnostic classes in the test mod.
No compilation or tests have run.

Located and downloaded official Cobalt 0.9.9 source archive and POM into ignored
source-reading storage for API inspection. The publisher Maven host is
https://maven.squiddev.cc; the guessed older URL returned 404. No engine code was
executed, no Gradle dependency was added, and Lua/CraftOS is still unfinished.


### October 26 continuous local work — nuclear charge housing

Added the supplied six-face nuclear charge atlas and unchanged ring-inventory
GUI. Eight independent industrial TNT slots surround one fissile core. Explicit
owner/party arming starts a ten-second fuse; disarm retains ingredients. Loss of
owner presence or build permission disarms. Hoppers cannot load or unload it, and
armed menu slots cannot move cargo. Detonation consumes cargo before removing
the housing, then uses the common claims/PvP/line-of-sight blast policy with an
eight-block maximum radius. No legacy fallout or unbounded terrain destruction
is claimed. The housing recipe uses reinforced stone, circuit and neutron
reflector. Client controls display fuse, radius and requirements. No tests or
compilation have run; the full collection remains unfinished. OpenAI Codex.


### October 26 continuous local work — magnetic fence elevator

The base machine metadata audit exposed the missing magnetizer. Added a real
JE-powered lift with the supplied housing atlas: a front iron-fence column extends
up/down twenty loaded, authorized blocks. Owner/party riders wearing metal-tier
boots rise at bounded speed; sneaking descends. Redstone pauses, each moved rider
uses four JE/tick, and disconnected/protected columns, spectators and mounted
players are excluded. At most eight riders are affected per tick. Current movement
packets synchronize the velocity; no teleportation or chunk forcing. The old
upgrade GUI is not claimed; status/help is on interaction. Authored deferred
boots/power/redstone/claim regression and nuclear-charge fuse/cargo regression.
No compile or tests run. Remaining spatial/Lua and source behavior are still open.


### October 26 local continuation — scanner-guided physical mining

Industrial miners now accept a charged scanner in the last supply slot. Basic
scanners survey a 5×5 layer; advanced scanners survey 9×9. Each depth spends
100 scanner JE and selects one reachable tagged ore. A real, finite pipe branch
advances one block per paid work step, collects actual drops with the installed
tool, then retracts into real output slots before the main column descends.
Path/fuse-free work state survives reload; permissions and loaded chunks are
checked for every move. Original pipe textures now form connected straight and
corner rods. No remote ore extraction or chunk loading. Deferred regression
covers cost, lateral tip, reload and conserved drops/pipes. The collection is
still unfinished; no build, tests, commits, pushes or deployment.


### October 26 local continuation — isolated Lua runtime integration

Added an actual Cobalt 0.9.9 Lua runtime path, pinned and SHA-256 checked as an
embedded library. Scripts execute in separate bounded-heap Java workers, never
on the Minecraft server thread. Four workers maximum, instruction slices, pipe
response deadlines and process termination bound failures; no Lua host filesystem,
HTTP, JVM/package loader or debug API. Native hardware requests return through
the server's existing ownership, claims and power checks. Turtle movement hands
the live worker to its destination rather than replaying the script. Reload
requires explicit restart to avoid replaying world actions. Lua template button,
print/sleep, native command bridge, limited saved-file access, turtle movement,
mining/placement, selection/count/detection and energy/time requests are wired.
This is uncompiled and untested. Full CraftOS ROM/event/peripheral compatibility
remains unfinished; the worker process path also still needs packaged-JAR and
Windows validation at the final combined test stage. Not a completion claim.

### October 26 continued implementation — placed surveys, cargo and Lua APIs

Added original-geometry volume/path survey beacons with original pixels, crafting
and drops. Using the survey notebook on a beacon selects area/path mode directly;
ordered points feed the existing real carrier route and machine-area workflows.
Consecutive duplicate route points are refused. Existing handheld volume markers
remain compatible. No global marker search or forced chunk loading.

Spatial exchanges now capture single/trapped/double chests, requiring both paired
halves inside the region and rejecting cross-boundary connections before mutation.
Both inventories retain separate saved contents; stale combined-container menus
close during detach. A deferred regression covers partial-pair refusal and restore.
The pending 128-cubed incremental exchange and entity migration are NOT complete.

The supplied oil-glob sprite now has a conserved native route: one crude-oil bucket
becomes eight globs plus its bucket remainder; eight globs and a bucket restore the
filled bucket. Source inspection found registration but no source gameplay for this
item; this is an explicit native storage/convenience adaptation.

Lua now has bounded root-only file handles, deletion/existence/listing, redstone
input/output, additional turtle actions, timer/event queues and monitor/modem event
polling. These operate through current server authorization; they do not expose
host files. The worker I/O queue is bounded and deadline failures are reported.
Full CraftOS/ROM/peripheral compatibility remains unfinished. All changes are local;
no compilation, tests or game launches have run. Resource generation is not testing.

### October 26 continued implementation — paged large building exchange

Added the supplied 128-cubed cell/component icons and recipes. Large building
transfers now use 512-block pages, immutable before-images and revision-fenced
plot files, instead of processing two million blocks in one game tick. The port
reserves the region and input cell, checks loaded chunks/current permission,
allows cancellation, and restores recorded pages on interruption/restart. Port
capacity/input accommodate the existing per-block JE price. Neighbor notifications
are batched after release. Block edits, explosions, piston crossings and entity
entry are guarded while reserved. Raw entity positioning and safe login arrivals
are covered in the local implementation; these hooks still require runtime testing.

This large-cell route intentionally accepts stable building blocks only. It does
NOT yet implement machine inventory or live-entity UUID migration. The smaller
2/16-cell journal retains its explicit container whitelist. Deferred combined test
covers asynchronous capture, edit/input lock, cancellation after writes, rollback,
revision conservation and subsequent restore. Actual restart recovery and large
128-cubed performance still need validation. No build/tests/game launch occurred.

Lua peripheral discovery/wrapping now addresses slotted inventories, original
monitor displays and speakers. Inventory transfers use actual per-slot transactions;
monitor drawing targets the selected connected screen; speaker notes are bounded
and powered. Terminal line input feeds read()/events through the existing authorized
menu session. Full CraftOS ROM and remaining peripheral compatibility are still open.

Industrial base metadata audit covers all 16 machine slots. Source blockBeam is an
unfinished accelerator prototype (buffer-only accelerator; noninteractive emitter
particle), and itemDebug is an old developer reflection/infinite-energy tool. These
are explicitly recorded as source prototype/developer content, not falsely counted
as completed survival equipment. The full collection remains unfinished.

### October 26 continued implementation — ROM terminal and resident journal

Connected the supplied read-only Lua ROM to the isolated worker, retaining source
files and SPDX headers. Original colors, vector, parallel, textutils, settings and
keys APIs now load through the adapter. Source-only virtual loading cannot access
host paths or native libraries. Added a saved 48×24 terminal view, clipped drawing,
keyboard/character events, line input and shell/editor entry point. These programs
are wired, **not executed or validated**. HTTP, host mounts and arbitrary external
Java peripherals remain unavailable; exact CraftOS compatibility is not claimed.

Lua now addresses actual inventory/fluid transfers, bounded modem channels, saved
printer drafts (paper/ink consumed on completion), monitors, speakers and floppy
documents. A floppy exposes one virtual program.lua document. Access resolves
current connected hardware and permission each time. Input queues follow moving
turtles. Runtime classpaths and all new code remain uncompiled and untested.

Small spatial cells now journal up to 24 explicitly supported vanilla farm animals
and pets, retaining UUID, health, name and saved state. Players, bosses, mounted or
leashed groups and unlisted entities are refused; pet capture requires its owner.
Rollback records outgoing/incoming identities and flushes entity storage before
completion. Lathes join the small-cell whitelist with ownership and stale-buffer
checks. Authored a UUID/name/health/stale-cell regression, not run. Recovery with
asynchronously loaded entity chunks still needs review and validation. Large-cell
live-machine/entity transfer remains unfinished; 128 cells still accept buildings.

The refreshed 612-entry registry inventory had one unresolved spring marker. Its
source model is vanilla bedrock. Renewable water and finite native oil reservoirs
are an explicit consolidation, not infinite oil. Registry coverage does not prove
complete gameplay. No compilation, tests, game launch, commit, push, release,
profile change or server action occurred in this continuation.

### October 26 implementation closure and start of combined validation

Extended 128-cell pages to include at most 64 supported container/lathe inventories
and 24 whitelisted residents. Block-entity ticks, hopper access, native transfer
checks, menu clicks, resident ticks and resident damage are held while reserved.
Before-images include inventory payloads; outgoing/incoming resident records are
in the write-ahead journal and revision head. Cancellation/recovery restores the
old UUIDs and flushes both block and entity storage. Small-cell recovery waits for
asynchronous entity chunk data before resolving identities. Authored the combined
large-cell cargo/resident/cancellation regression. Double chests crossing a
512-block page boundary are refused; arbitrary external machines, player capture,
bosses, riding/leash graphs and unlisted residents remain deliberate unsupported
cases, not silently serialized. Large payloads use the same explicit whitelist.

The computer runtime now also exposes original disk API calls, bounded one-document
floppy mounts, identity/labels and the original interactive shell/editor adapters.
Its virtual registry is a private Lua table, not JVM or runtime-registry access.
No HTTP, host directory mounts or arbitrary Java upgrades. Original source ROM
programs depending on those facilities remain unavailable by design.

Implementation has moved to the final combined validation pass. First compile
attempt hit a Gradle wrapper cache permission denial; retried through the normal
tool permission mechanism. Build/resource/server/client results are pending and
must be recorded before claiming the collection is ready. No publishing/deployment.

### October 26 combined validation — first fixes

Common and client Java compilation passed after current-platform API repairs.
Game-test compilation exposed old field/class references and a test-only container
naming call; those are corrected and the combined disposable server suite is now
running. No server gameplay pass is claimed yet.

The first Python suite had 19 failures/errors, including missing local DejaVu
configuration. With the configured font and fixes, 268/271 tests passed; remaining
failures were a further note recipe collision, one omitted crafting serializer in
the viewer audit and the stale actual-registry survival snapshot. The first two
are corrected; the registry must be refreshed from the actual game test, not
invented from model definitions. Alchemy values and recipe-view data were rebuilt.

Real isolated Java/Lua worker checks passed vector/JSON/ROM access, coroutine host
calls, original shell directory listing and interactive shell exit through the
terminal-event bridge. The first run caught CRLF handling in the ROM index; the
adapter now strips both CR and LF. These are actual local worker checks, not
in-game client validation. A persistent shell regression joins the game-test suite.

Resource checking caught old loot condition keys, legacy block tags, missing test
entrypoints and iron-fence diagonal data. Repairs preserve source textures and
intentional inward-facing hollow model shells; the checker validates those shells
against named original coordinates. Combined checks are still in progress.

### October 26 combined validation — runtime and acquisition repairs

The complete data/art check passed: 24,473 block/item models, 3,729 quad parts,
180 models drawn together and 7,949 material IDs. The actual server exported the
registry snapshot. Its refreshed acquisition graph has zero unknown ingredients
and zero undocumented active routes; existing explicitly deferred acquisition
entries elsewhere in the mod remain labeled as such.

The running 2,558-test server suite exposed a trampoline crouch fall-damage issue,
a prey-loot fixture using a nonguaranteed equipment-drop chance, missing transfer
provider registration for late-added sorter variants, and Chompa missing from the
seasonal mob costume list. Fixes are prepared locally, including regression
coverage for both industrial sorter variants. These fixes have not yet passed
an updated server run; do not mistake the still-running earlier suite for their
validation. The magnetizer test also now observes its one-lift-per-tick budget.

The complete Python run finished with 270/271 passing; its one failure was the
new Chompa seasonal mob list differing from the generator definition. Both now
match, and all eight tests in that affected module pass on rerun. All 26 pinned
pack libraries passed download/hash verification. After the note-recipe fix,
8,939 alchemy values were regenerated without changing the server output policy.

An isolated JAR containing the compiled Lua worker classes and original ROM also
passed the vector/JSON/coroutine smoke and original interactive shell exit. This
checks file-JAR code-source/resource lookup (not only loose development classes).
The final complete mod JAR and graphical client checks are still pending.

The initial full server run was stopped after 1,795 passes and the four known
failures: thread inspection found passive snowshoe handling synchronously loading
terrain even when the player wore no snowshoes. Fixed it to short-circuit by worn
item and loaded chunk, and bounded rooted-boots terrain access likewise. Added an
unloaded-terrain regression. A fresh full server run, rather than a partial
repair-only result, is now rebuilding with these performance and gameplay fixes.
The stopped run is not a completed-suite pass.

The next complete server run finished: 2,554 passed / 2,560 total. Six failures
were investigated: the trampoline mock had never acknowledged client loading;
the encyclopedia expected-page list omitted the new ungated guide; the processor
fixture applied item components after filling (clearing its own contents); the
second decorative statue collided with the first fixture's upper half; a consumed
fragile shard queried ContainerItemContext amount before checking blankness; and
the headless server has no custom pocket dimension. The shard API use is fixed,
fixtures corrected, and pocket invitation/return/read-only assertions moved to a
real integrated-world client test, not skipped. The complete server suite and six
relevant client groups are running again with these fixes and the rooted-boots
loaded-chunk guard. Final combined pass remains pending.

### October 26 combined server validation passed

The rebuilt full server suite completed with **all 2,559 required tests passing**,
including the trampoline, late-registered sorters, consumed shard, processor cargo
and names, two-half statues, spatial reservations/recovery and unloaded-terrain
regressions. The cross-dimension pocket guest scenario is now a separate real-world
client test, accounting for the server count change. The same build passed **79
offline updater assertions**. Six client groups are next; no client pass or human
playtest is claimed yet. Everything remains local.

The first client run exposed a real login crash: spatial rejection bookkeeping
used WeakHashMap<Entity,...>, which calls Entity.hashCode before a client entity
has received its network ID. Replaced it with Guava weak identity keys; rejection
state now survives ID assignment without retaining dead entities. Added a server
regression whose entity hashCode deliberately throws, and restarted all six client
groups. The earlier client run is a failure, not a gameplay pass.

### October 26 combined client validation passed

All six selected client groups completed successfully after the identity-map fix:
accessory icons, original wearable geometry/equipment, original computer/turtle
models and synchronized terminal, costumes including Chompa, all aircraft steering/
engine audio/F5 checks, and real-dimension pocket invitations/read-only/revocation/
return. Screenshots are preserved under build/jimtafacts-visual-evidence-oct26.
Reviewed the accessory gallery, cowboy hat, Chompa and cargo-airship screenshots.
The terminal screenshot exposed clipping at the test's small window, despite its
functional assertions passing. Added fit-to-window rendering and matching mouse
coordinate transforms; final computer screenshot and spatial regression rerun are
in progress. This does not claim a human survival playtest or universal mod parity.


### October 26 local completion

Final repairs passed: all 15 spatial tests, including the new construction-time
identity regression, and the focused computer client test. The corrected terminal
screenshot was reviewed: header, all 24 terminal rows, input and status remain
inside the small window. The final JAR includes these repairs. Combined evidence:

- Full server run: all 2,559 required tests passed; subsequent spatial run: 15/15,
  including the added regression. These are overlapping runs, not 2,574 unique tests.
- Six relevant client classes passed; computer class rerun passed after layout fix.
- 79 updater assertions passed; asset/data audit and all 26 dependency hashes passed.
- Python suite: 270/271, followed by all eight affected costume tests passing after
  the sole failure was repaired. Alchemy regeneration checks: 10/10.
- Original ROM/vector/JSON/coroutine and interactive-shell smoke passed, including
  the complete built mod JAR. No host-library programs were executed.
- 68 initial client screenshots plus corrected terminal evidence retained locally.

Artifact: `build/libs/jugcraft-0.1.0-alpha.jar`. Evidence logs are the local
`build/jimtafacts-*-oct26.log` files; screenshots are under
`build/jimtafacts-visual-evidence-oct26`. No personal launcher/profile, live server,
client update channel, Git commits or remote branches were changed.

The implemented collection uses the documented native adaptations, not unrestricted
source-mod compatibility. Unsupported spatial payloads and external computer APIs,
legacy/debug/unfinished prototype declarations, recovery-file garbage collection
and owner visual/survival playtesting remain explicit in jimtafacts-remaining.md.
Normal/slim costume motion fit was not human-playtested. The retained source
registry classifications are an audit, not a claim of one gameplay object per file.
