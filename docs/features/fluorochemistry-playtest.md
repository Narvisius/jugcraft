# Fluorite, fluorochemistry and finished PTFE

Owner-requested local implementation, 13 October 2026. Built with OpenAI Codex (GPT-6 family), on the ordinary-polymer checkpoint `97b34f83c`. This work remains on `feature/starter-chemistry-playtest` for local review. Minecraft 26.3, Fabric 0.19.5, dependencies, existing item IDs and shared machine inventories are unchanged.

## Player route

Snowy-region fluorite -> Mineral Conditioning Station -> concentrate. The existing starter acid factory supplies sulfuric acid -> new Acid Concentrator -> concentrated acid. Combine concentrate and concentrated acid in the Chemical Dissolution Chamber for hydrogen fluoride gas and accounted calcium sulfate.

The Fluorocarbon Reactor makes chloroform from methane and chlorine, then chlorodifluoromethane from chloroform and hydrogen fluoride. The Fluoromonomer Cracker produces tetrafluoroethylene (TFE). Each stage retains hydrogen chloride for existing Chemical Reactor absorption. Methane from cleaned biogas and chlorine from the magnesium/electrolysis lines connect this branch to previous factories.

The existing Polymerization Kettle uses a reusable PTFE Process Insert to make PTFE powder from TFE. The insert uses earlier nickel, alumina and brick. Existing press molds shape unsintered seals, fittings and liners. The PTFE Sintering Oven finishes them. An alternative liner route mixes powder with lubricant in the Conditioner, then uses the Extruder's sheet die to produce a liner blank and an allocated offcut. Only identified unsintered offcuts can be reclaimed; sintered parts and unprepared powder are not ordinary melt-extrusion feed.

Finished seals, fittings and liners immediately build the larger Lined Fluorochemical Reactor. It performs the two starter fluorocarbon recipes in two independent lanes, has 8000 mB tanks, and pays 90% energy per lane, rounded up each tick. The starter equipment requires no PTFE, HF or other product of its own line. There is no routine liner replacement or retrofit requirement.

## Six new machines

| Machine | Width x depth x height | Function |
| --- | --- | --- |
| Acid Concentrator | 3 x 3 x 4 | Concentrated acid and recovered water |
| Chemical Dissolution Chamber | 3 x 4 x 3 | Fluorite concentrate and acid to HF plus residue |
| Fluorocarbon Reactor | 3 x 4 x 4 | Chloroform preparation and fluorination |
| Fluoromonomer Cracker | 3 x 4 x 3 | TFE monomer with HCl recovery |
| PTFE Sintering Oven | 3 x 4 x 3 | Finishes shaped PTFE blanks |
| Lined Fluorochemical Reactor | 3 x 5 x 5 | Two-lane, lower-energy-per-batch fluorocarbon processing |

All reuse FormMachine controllers, menus, Pause/Cancel, status reasons, upgrades, output reservations and saved work. New capabilities select operations rather than duplicating infrastructure. Gas cylinders, tanks, fluid/item pipes, copper cables and battery boxes are existing systems. External connection sockets match the actual sided transfer interfaces. Tools stay in protected, retained sockets. JEI, the Handbook and Encyclopedia show the recipes and connected route; acquisition goals cover all six machines.

## Recipes

These are provisional game quantities. Internal heat and process regulation are included in electricity; this is not a real chemical operating procedure.

| Operation | Feed -> products | Ticks x JE/t, entry machine |
| --- | --- | --- |
| Acid concentration | 1000 mB sulfuric acid -> 750 mB concentrated acid + 250 mB water | 240 x 64 |
| Fluorite preparation | 1 fluorite + 250 mB water -> 1 concentrate | 120 x 32 |
| Dissolution | 1 concentrate + 750 mB concentrated acid -> 500 mB HF gas + 1 calcium sulfate | 240 x 80 |
| Chloroform preparation | 500 mB methane + 1500 mB chlorine -> 500 mB chloroform + 1500 mB HCl gas | 240 x 64 |
| Fluorination | 500 mB chloroform + 1000 mB HF -> 500 mB chlorodifluoromethane + 1000 mB HCl | 240 x 80 |
| Monomer cracking | 1000 mB chlorodifluoromethane -> 500 mB TFE + 500 mB HCl | 240 x 96 |
| PTFE polymerization | 1000 mB TFE + retained PTFE insert -> 4 powder | 240 x 80 |
| Press shaping | 1 / 2 / 3 powder + retained gasket / fitting / panel mold -> 1 matching blank | 160 x 48 |
| Paste preparation | 4 powder + 250 mB lubricant -> 4 paste charges | 120 x 32 |
| Paste extrusion | 4 paste charges + retained sheet die -> 1 liner blank + 1 unsintered offcut | 200 x 64 |
| Sintering | 1 seal / fitting / liner blank -> 1 matching finished part | 240 x 80 |
| Clean offcut recovery | 4 unsintered offcuts -> 1 powder | 160 x 48 |
| Residue preparation | 2 calcium sulfate + 250 mB water -> 1 Prepared Gypsum Binder | 160 x 48 |

One Prepared Gypsum Binder plus two sand and one gravel crafts two white concrete powder as game construction filler. Raw residue is not directly placed or turned into high-purity material. Preparation consumes the wash water, electricity and half the solid units. This limited outlet does not implement the future phosphate-residue/plaster-board factory.

A liner accounts for three powder units, a seal for one and a fitting for two. Extrusion allocates the fourth paste unit to scrap rather than awarding free scrap after full-yield production. Recycling is lossy and powered. All HF/HCl/product outlets must fit before work begins; these machines have no gas-vent toggle. The expanded reactor uses the same recipe IDs and independent reservations, not duplicated recipe files.

The naming and broad industrial route follow [EPA's HCFC description](https://www3.epa.gov/ghgreporting/help/tool2014/definitions/hcfc.html) and the [USITC PTFE resin report](https://www.usitc.gov/sites/default/files/publications/701_731/pub4801.pdf). Game fluid volumes are not chemical mole balances or purity claims.

## Fluorite discovery and existing worlds

Iron-tool fluorite ore and deepslate fluorite ore drop 1–2 fluorite before Fortune, support Silk Touch, and have lossless nine-item storage compaction. The existing Ore Survey recognizes the ore family.

The eligible biome tag lists snowy plains, snowy taiga, grove, snowy slopes, frozen peaks, jagged peaks, ice spikes and snowy beach. Main veins make four size-18 attempts per chunk from Y -48 to 72; one size-7 upper attempt spans Y 64 to 192. These are placement attempts, not guaranteed yields. Air-exposed ore is allowed, so some upper veins show in exposed mountain stone. This is not a guaranteed surface marker or a separate ground-deposit structure. Terrain exploration remains part of owner balance testing.

The `machines` feature controls placement. Ore appears only in newly generated eligible chunks; no existing terrain, inventories or manual saves are rewritten. Existing worlds can explore new snowy chunks, trade the mineral, or use creative supplies for testing.

## Art and assets

**Current machine finish — owner follow-up, 13 October:** all 29 affected machine bodies/fittings now use the existing muted silver-grey `dw_steel_plate` texture instead of bright white enamel. The shared `SILVER` machine material is separate from `CERAMIC`, so powders, polymers and ceramic component items retain their pale appearance. The steel texture is reused unchanged, with its crisp 16×16 pixels and restrained brushed marks; no source image or asset-library original was edited. This supersedes the white machine finish in the earlier correction below. Implemented with OpenAI Codex.

Silver-finish validation: all 1,330 changed JSON models differ only in their surface material. Industrial-form and repository audits passed. The JAR was checked for all 29 silver machine forms, the unchanged shared steel texture and preserved pale component items. The targeted native client run passed again across 18 stations, producing 126 new screenshots and passing working/stopped drive checks. The acid concentrator and dissolution chamber were visually inspected. No gameplay code changed; full server/Python suites were not repeated. The separately named Silver Machines pack preserves the stocked yard and does not overwrite manual saves.

Owner visual correction, 13 October, implemented with OpenAI Codex: replaced the glossy `ceramic_insulator` material on all industrial forms and related component items with the existing crisp 16×16 `el_white` enamel panels. This reaches the shared starter chemistry, acid, anaerobic, biorefinery, coastal metals, polymer and fluorochemistry models, as well as earlier electrical form fittings. No new bitmap, owner-library modification or rendering system is introduced. Machine geometry, ports, running animations, recipes and saved inventories are unchanged. The separately registered Ceramic Insulator building block retains its own art; it is no longer used as the industrial forms' surface material.

Surface-fix validation: regenerated all forms and component items; 29 machine forms now use the clean panels. Parsed comparison of all 1,346 changed JSON assets against the preceding commit confirmed that only the material substitution changed. Industrial-form and repository-link audits passed; the packaged JAR contains no industrial model references to the old sprite. `jar runClientGameTest -PclientTests=FluorochemistryClientGameTests` passed, producing 126 fresh screenshots across 18 stations with working/stopped animation checks, menus and connections. Close views of the dissolution chamber and large lined reactor were visually inspected. The earlier 1,422-server-test and 34-Python-test results below remain the gameplay baseline; those suites were not rerun for this material-only correction.

Supported ceramic-lined vessels, steel skids, seated condensers, reinforced furnace bodies and connected service piping use the existing industrial material kit. Existing MachineRotors provides working and stopped drive animations. Ports and short service runs terminate inside process bodies; there are no decorative dead-end pipes. The large reactor has a taller lined vessel and attached recovery housing.

The owner's `art/owner-library/originals/Blocks/block_fluorite.png` is copied unchanged to the fluorite storage block. New ore overlays and the mineral item use the established material maps and a cyan-green palette; ores reference Minecraft's own rock textures at runtime. Other new items are native material-kit models, and fluids use the existing deterministic fluid-sprite generator. No owner original or notice was changed; no library program was executed.

## Validation and remaining scope

- Final Gradle build: **all 1,422 required server tests passed**. Seven new fluorochemistry tests cover exact recipe ledgers, rotated ports, wrong acid, blocked gas/residue outlets, progress reload, two paid parallel lanes, PTFE shape/sinter boundaries, ore loot/biome tags and actual crafting of all six new machines. The earlier coastal and polymer ledger tests also exercise all nine shared-machine extensions.
- Native client test passed across **18 stations**, with **126 screenshots** covering four working views, idle views, menus and battery/cable connections. Working/stopped drive assertions cover the 17 processing machines. The flat-yard coastal intake is intentionally idle without a valid coastline; the existing server test verifies real coastal-source pumping and rejection of invalid water.
- Final Python suite: **34 tests passed**, covering packaging, machine plumbing, material budgets, regional ore, unchanged owner storage art and shared-capability recipe-view entries. The full data audit passed for **2,292 material IDs**; art checks passed for **7,276 block/item models, 3,514 quad parts and 154 combined models**, with unchanged exception counts. Repository links and all 18 locked dependencies passed.
- The first data audit found two omitted generated fluorite crusher recipes. Those were generated, and the complete audit passed. The larger reactor's recipe-view category now resolves shared recipes with its actual per-lane energy cost; no duplicate processing files are emitted. These data corrections were followed by the final complete server build. Machine models and menus were unchanged from the native visual run.
- The separate saved yard includes the six new machines, the recent ordinary polymer stations and the coastal magnesium/titanium line. It has charged batteries, sample feeds and supply chests; the samples are independent, not a fully connected continuous factory. Earlier manual worlds are preserved. The pack records its source commit, hashes and validation evidence outside Git.
- Manual launcher smoke test: the integrated server loaded the saved world and the player joined. Its first startup exposed missing particle references on 35 recent chemistry item models; their source generators and runtime models now specify the existing industrial texture. The JAR was rebuilt, every reference was checked, and the second startup had no missing-texture-reference warnings. The game was left open for owner testing. Desktop capture timed out, so final manual startup confirmation comes from the window listing and game log, not a new screenshot. The 126 automated screenshots remain available separately.

The saved local yard and launcher are ready for owner playtesting. Automated checks do not claim a two-player multiplayer session, a Modrinth app import, a complete survival progression playthrough or measured long-running factory performance.

Still separate: aqueous/electronic-grade HF and wafer etching; LiPF6 battery chemistry; elemental fluorine and unrelated fluorochemicals; further PTFE consumers; full stainless-steel progression; larger bulk plants and representative long-running multiplayer factory tests. Ordinary and concentrated sulfuric acid are implemented; electronic-grade purification remains planned. No main push, upload or merge is part of this local checkpoint.
