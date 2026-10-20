# Polymer molding, extrusion and clean recycling

Owner-requested local implementation, 13 October 2026, using OpenAI Codex (GPT-6 family). Builds on the local coastal-metals checkpoint `e9af7f178`. This delivery stays local for owner testing before publication. Existing world IDs, press inventory slots, recipes, platform pins and dependencies are preserved.

## Player route

The existing Polymer Molding Press retains its six reusable molds and recipes for housings, fittings, cable insulation, gaskets, hoses and vehicle panels. HDPE now joins plastic pellets/PVC as compatible rigid stock for housings and panels. Flexible LDPE deliberately does not. Press progression, inventory layout and tooling stay compatible with the previous test world.

Four new placeable multiblock machines complete the connected ordinary-polymer factory:

| Machine | Footprint | Role |
| --- | --- | --- |
| Monomer Preparation Unit | 3 x 4 x 3 | Existing refinery gas or bioethanol to named ethylene; chlorine addition to dichloride; cracking to vinyl chloride with HCl recovery |
| Polymerization Kettle | 3 x 3 x 4 | Distinct HDPE/LDPE using reusable process inserts; existing vinyl chloride/butadiene to PVC/rubber |
| Polymer Extruder | 3 x 5 x 3 | Five reusable dies for sheets, films, profiles, continuous hose and insulation; separate same-grade offcut drawer |
| Clean Polymer Reclaimer | 3 x 3 x 3 | Paid, lossy recovery of identified clean production scrap and selected unused products |

Each is crafted from earlier steel, basic circuits, copper, motors, tanks and ceramics, without requiring its own products. Dies cost four steel plates and a simple pattern item. The HDPE catalyst cartridge uses existing titanium, magnesium and brick; its LDPE counterpart uses steel, copper cable and brick. These are game tooling abstractions, not real catalyst formulations. Existing PVC supplies permit first extrusion without HDPE tooling. Gas cylinders, fluid tanks, pipelines, cables and item transport are shared infrastructure.

## Receipts and consumers

All values below are provisional fictional game units. Heat and pressure regulation are internal paid work, not a manual network-pressure system. No new fuel value is assigned to ethylene or dichloride.

| Operation | Feed -> result | Ticks x JE/t |
| --- | --- | --- |
| Refinery separation | 1000 mB refinery gas -> 500 mB ethylene | 200 x 64 |
| Renewable ethylene | 1000 mB bioethanol -> 500 mB ethylene + 250 mB water | 240 x 64 |
| Dichloride preparation | 500 mB ethylene + 500 mB chlorine -> 1000 mB ethylene dichloride | 160 x 64 |
| Dichloride cracking | 1000 mB dichloride -> 500 mB vinyl chloride + 500 mB hydrogen chloride gas | 240 x 96 |
| HDPE polymerization | 1000 mB ethylene + retained HDPE cartridge -> 4 HDPE pellets | 200 x 64 |
| LDPE polymerization | 1000 mB ethylene + retained LDPE insert -> 4 LDPE pellets | 240 x 80 |
| PVC / rubber | 500 mB vinyl chloride / butadiene -> 4 existing PVC / rubber | 160 x 64 |
| Sheets | 4 HDPE pellets or PVC -> 8 existing plastic sheets + 1 matching offcut | 160 x 24 |
| Film | 4 LDPE pellets -> 3 film rolls + 1 LDPE offcut | 160 x 24 |
| Profile | 4 PVC -> 3 profiles + 1 PVC offcut | 160 x 24 |
| Hose | 3 rubber -> 1 existing hose + 1 rubber offcut | 160 x 24 |
| Insulation | 4 rubber -> 6 existing insulation + 1 rubber offcut | 160 x 24 |
| Clean recovery | 4 matching offcuts -> 1 original stock; or 3 clean films/profiles -> 1 matching stock | 120 x 32 |

Monomer recovery always needs outlet space; it never vents HCl. Absorb HCl through the existing Chemical Reactor. Drain incompatible feeds/products before switching jobs. The old refinery/PVC/polymer routes remain available.

Products have immediate uses: sheets retain all machinery and colored-plastic construction recipes; insulation makes cables; hoses make valves; two film rolls form insulation; film, two sheets and a profile make a cushioned housing; profile plus sheet make two existing white-plastic blocks. Housings, fittings, panels and gaskets retain all previous consumers. Extrusion makes no extra free offcut after allocating the full input: a hose accounts for three stock units; other listed extrusions for one each. Recycling loses material and consumes energy, never converts mixed waste into a better grade. Legacy sheets, assembled equipment and placed blocks are not accepted because their original grade is not tracked.

## Interfaces, automation and persistence

All machines use the existing authoritative FormMachine controller and terminal: energy, progress, inputs, products, tool socket labels, tool locks, missing-feed/tool/power/output reasons, Pause/Cancel and speed/efficiency slots. Shared JEI categories show actual receipts/tooling; Handbook and Encyclopedia provide acquisition goals and route guidance. Jade and renderer integrations remain optional as before. No new packet, UI framework, renderer, currency or storage implementation is introduced.

Power enters the front-left base. Item input is front-right, finished output rear-right at middle height, and extrusion offcuts rear-right at upper height. Fluid supplies enter the rear middle and products leave the rear bottom. Every port has a physical socket and supported connection into its process body. Tooling cannot be taken by ordinary item automation; working lanes reserve outputs and keep tools locked. Save/reload retains paid progress and pending outputs through shared serialization. The press's slot positions do not change.

## Visuals and provenance

The new native machines use the existing steel/olive/ceramic material kit: seated barrels and motors, a supported kettle, short connected service runs, a cooling bed with bearing-supported draw rollers, and an enclosed reclaimer. Existing MachineRotors animate guarded drives while working and stop them while paused. No decorative pipe ends in empty space unless it is an actual external connection.

Five unchanged owner-library 16 x 16 textures are copied from `art/owner-library/originals/Blocks/`: `mold_plate.png`, `mold_unpacking.png`, `mold_rod.png`, `mold_bullet_casing.png`, and `mold_wire.png`, for sheet/film/profile/hose/insulation dies. Other new items use small native material-kit models. New fluid sprites use the existing deterministic generator. Originals and notices remain intact; no library executables were run.

## Scope and validation

This checkpoint completed the ordinary molding/extrusion route described above. The subsequent local [fluorochemistry checkpoint](fluorochemistry-playtest.md) adds the fluorite/HF/fluorocarbon/PTFE chain and advanced-machine consumers. Additional polymer grades and larger bulk plants remain future work.

- Full Gradle build: **all 1,415 required server tests passed**, including five new polymer tests. All twenty new processing receipts pay their exact energy and retain tooling; port tests cover every orientation. Tests also exercise wrong dies, blocked offcut storage, save/reload, HDPE/LDPE separation, original-press compatibility, same-grade recovery and real construction recipes.
- Native client test renders the four new machines and updated press, checks working/stopped drives, opens their real menus and prepares connected battery/cable samples. Thirty-five screenshots cover idle/working views, menus and connections. The preserved yard starts with HDPE molding and LDPE film extrusion stocked for the owner.
- Fifteen Python packaging, geometry and polymer-budget tests passed. The full material/data/recipe audit passed for 2,267 IDs; art checks passed for 6,821 block/item models, 3,508 quad parts and 154 assembled models, retaining the existing exception counts. Repository links and all eighteen pinned pack dependencies passed validation.
- The first full run had one unrelated crow-flight timeout; the complete rerun and final delivery server run passed without changing that test. The data audit caught a missing crude-oil feature condition on the added white-plastic crafting recipe; it was corrected and the complete audit passed.

The local delivery records the source commit, JAR/pack SHA-256 checksums, saved world and validation logs outside Git. No upload or main merge. Automated testing is distinct from owner survival playtesting, Modrinth app import, two-client multiplayer and representative factory-load testing.


## Combined integration correction (20 October 2026)

Full-yield sheet, insulation and hose extrusion returns no offcut. Film/profile
batches retain their one offcut; reclaiming remains four offcuts to one stock.
This replaces the earlier claim that every improved-yield batch could also return
scrap. Tests account for each product in stock-equivalent units and verify that
a full sheet drawer blocks spending and eight sheets survive batch save/reload.
Integration validation is recorded in [the combined feature record](trees-chemistry-integration-oct20.md).
