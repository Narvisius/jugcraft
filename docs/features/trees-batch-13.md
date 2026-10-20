# Tree batch 13: walnut and bristlecone

Status: owner-approved; ready for review on `codex/tree-generation-batch-13`, based on integrated
main `511b7ec0`. Owner requested implementation on 20 October 2026, with a
walnut timber approval checkpoint before tree or biome work. The owner approved
the timber and authorized the complete Batch 13 tree/biome work on the same day.
Actual tool/model: OpenAI Codex (GPT-6).

The open upstream PRs and branch inventories of all five public forks were
checked before starting. No other open Batch 13 implementation was identified.
PR #421 concerns aged pine/maple and remains separate.

Integrated main contains a recovered `ecology_walnut` family from the 18 October
owner collection. Batch 13 keeps every recovered registry ID, placed block,
block entity and boat entity. The approved `walnut_*` nine-form timber is the
canonical output. Recovered timber drops and crafting converge on that output;
shared ingredient tags accept either inventory. Both saplings grow the same 4:1
normal/large selection; both leaf IDs have the same seasonal harvest. The old
unconditional taiga injection is removed, while its configured feature remains
a compatibility entry point. Duplicate basic forms are hidden from the creative
catalogue; recovered doors, signs, boats, cabinet, shelf, buttons and pressure
plates remain available and use the shared supply.

Recovered core block models reference the approved textures, and recovered leaf
and sapling blockstates use the new shared models. The owner library is never
rewritten; the furniture palette adjustment below changes runtime copies only.
Existing walnut aliases receive the same essence value as their canonical drops;
husks carry the established brown-dye value. Other established essence values
remain unchanged.

## First checkpoint: walnut wood

Add the established nine-form wood family: log, wood, stripped log/wood,
planks, stairs, slab, fence and fence gate. Use the existing wood registration,
recipes, tags, fuel and sawmill systems. Hand crafting yields four planks per
log; powered sawing yields six. This initial approval stage has creative
samples only; survival timber acquisition follows with walnut trees.

Use owner painting bank row 2 (`black_walnut`) as the palette reference.
The planned cool dark brown timber is `#5a4b41`; furrowed bark follows the
owner's charcoal-brown ramp. Extend the existing deterministic wood painter
and preserve all existing wood seeds and assets. The owner-library catalog
used for the initial plan predates the recovered ecology collection. That
collection was included in the initial comparison and is now reconciled above.
No library originals change.

Prepare an inspection gallery with all nine forms, exposed end grain, tiled
walls and a small pavilion. Compare against spruce, dark oak, cedar, yew and
persimmon. Verify crafting, stripping axes, drops, fuel, sawmill costs and
save/reopen behavior. Record actual results below when available.

## Batch 13 implementation

- Walnut: 10% of Dense Forest tree selections, replacing fancy oak; 4% in
  Woodland, replacing ordinary oak. One fifth of walnut selections are large.
  Preserve current attempt budgets and other species' absolute shares.
- Normal walnut target: 8–10 blocks tall, 7–9 wide; large: 12–16 tall,
  10–14 wide. Straight lower trunks, connected spreading branches and
  irregular rounded crowns. Measure multiple seeds before finalizing.
- Seasonal green/ripe husks, renewable right-click walnut harvest, yellow-gold
  autumn and bare winter. Shared food routes: raw/roasted walnuts, squirrel
  food, walnut mooncake and consumptive husk-to-brown-dye processing.
- Basin bristlecone: one attempt per four chunks, twisted asymmetric
  4–7-block trees with sparse connected pine needles, 5–8-block spread.
  Existing pine wood, roughly 70% stripped and 30% bark. Existing pine saplings.
  Supported gravel, stone or andesite without dirt pedestals.

The bounded native feature plans all logs and foliage before writing. Every
branch bend has face-connected wood; a breadth-first pass computes real leaf
distances and removes unsupported foliage. Obstructed or wet placements fail
without a partial tree. Bristlecones require solid support beneath their rocky
footing and never replace it with dirt.

Walnut uses schedule `[105, 255, 290]`, with the existing seven-day positional
jitter. Fixed April 15 spring and October 20 autumn modes intentionally straddle
leaf-out and leaf-fall; summer is green and winter bare. Bare crowns cannot be
harvested and lose any remaining ripe state on their next random tick. The
shared deterministic foliage painter supplies six-tone green and gold leaves,
round green/dark husks and connected winter twigs. No supplied source art changes.

Right-clicking a ripe natural leaf gives 1–2 existing raw Walnuts plus one Walnut
Husk, then resets the fruit. Placed/persistent and unsupported leaves cannot be
harvested. Smelting, smoking or campfire cooking yields one Roasted Walnuts item;
the Cooking Pot consumes two roasted walnuts, two wheat, sugar and egg for four
Walnut Mooncakes. One consumed husk makes one Brown Dye. Raw and roasted walnuts
join the existing squirrel food tag.

Tree generation changes apply only to new chunks. Existing registrations and
placed blocks persist with feature switches disabled. No new dependencies,
platform changes or Batch 14 implementation are included.

## Validation

Wood approval stage, 20 October 2026:

- Five opaque 16x16 textures match their deterministic painter outputs.
  Existing wood textures are unchanged.
- `python scripts/check_repository.py`: pass.
- Client selection unit tests: all six pass.
- `python tools/check_mod_data.py`: pass, 6,145 material IDs; art audit covers
  11,745 block/item models, 3,713 quad parts and 173 combined models.
- Material data regeneration changes only walnut resources, shared membership
  lists and their generated manifest. Essence regeneration adds the nine
  walnut forms using existing wood values.
- `build runClientGameTest -I build/focused.gradle
  -PgameTestFilter=jugcraft-test:walnut_wood_game_tests*
  -PclientTests=WalnutWoodClientGameTests`: pass. Exactly four server tests
  execute: real crafting recipes, axis-preserving paid axe stripping, drops and
  fuel/tags, and actual sawmill input/output/energy behavior.
- The final client-only gallery rerun passes in 1m33s after adding the recovered
  walnut comparison. All nine forms, inventory and 55 sampled block states
  survive save/reopen. Rendered samples and the pavilion were visually checked.
- The delivery copy scans 1,554 saved region records and all other save files
  without finding actual test content. All 83 source files remain unchanged.
  Only unused development metadata in `level.dat`, `level.dat_old` and
  `data/fabric/registry_entries.dat` is removed from the copy.
- The ordinary client joins the cleaned gallery at `(0.5, -60, 5.5)` at
  06:40 on 20 October, facing the samples. It is open for owner inspection.

The export is `build/walnut-wood-preview-r2/Walnut Wood Gallery`. The flat
creative showroom has frozen time/weather/random ticks, comparison rows and a
walnut pavilion. It is not a Batch 13 world-generation demonstration.
Full tree/biome validation is recorded below.
No multiplayer or external launcher import is claimed.


Full Batch 13 validation, 20 October 2026:

- Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3,
  Java 25 and the existing 21 pinned Complete-pack dependencies.
- Repository/documentation check and the complete data/art/recipe audit pass:
  6,150 material IDs, 11,744 block/item models, 3,713 quad parts and 173 combined
  models. All 28 related unit tests pass (six walnut worldgen/compatibility,
  eight alchemy, eight recipe-loop and six client-selection tests).
- `build runClientGameTest -I build/focused.gradle
  -PgameTestFilter=jugcraft-test:walnut*
  -PclientTests=WalnutTreeClientGameTests`: all 12 focused server game tests
  pass, including the four approved timber tests. They exercise connected
  shapes, atomic obstruction/soil handling, both saplings, recovered crafting
  and drops, harvest/repeated-use protection, regrowth/seasons/decay, actual
  furnace roasting and actual Cooking Pot mooncake consumption.
- Twelve deterministic seeds per shape measured normal walnuts at 8–10 tall
  and 7–9 wide, large walnuts at 12–16 tall and 11–13 wide, and bristlecones at
  4–7 tall and 5–6 wide. All branches connect and all leaves have actual log
  distances below seven. Repeated seeds reproduce identical trees.
- Testing caught and fixed grass rejection: Minecraft 26.3's dirt tag excludes
  grass blocks. Walnut now uses the native oak-sapling survival rule. Tests also
  caught legacy salvage alternatives containing nested tags, which are expanded
  to concrete old/new item IDs; recipe-loop accounting still examines all members.
- The five approved timber PNGs remain byte-identical to the approval checkpoint.
  Material regeneration and reconciliation were checked for stable output;
  existing non-walnut essence entries remain unchanged.

- The complete build and client tour pass (8m45s combined). Nine planted trees,
  harvest/season displays and recovered blocks survive save/reopen. Eight game
  screenshots were inspected, including natural Basin bristlecones.
- The normal seed `jugcraft` has verified stops at Dense Forest `(832,85,1744)`,
  Woodland `(-1664,118,1728)` and Basin `(2744,118,5832)`. Walnut is present in
  both intended forests; weathered pine is present in Basin. No old walnut-log
  worldgen was found in those surveys.
- Delivery-copy inspection scans 7,352 saved region records and other save files
  without actual test content. All 109 original export files remain untouched.
  Only unused development metadata in `level.dat`, `level.dat_old` and
  `data/fabric/registry_entries.dat` changes in the copy.

Full tree-world export: `build/tree-batch-13-preview/Walnut and Bristlecone Inspection`.
The sibling delivery folder `tree-batch-13-preview-2026-10-20` contains the matching
mod, launcher pack, standalone world, screenshots and `TOUR.txt` coordinates.
The original approved timber gallery remains separate and preserved.

The ordinary client joined the cleaned inspection world at `(1080.5,180,-987.5)`
at 07:39 local time on 20 October. It is open for owner review.

### Furniture colour follow-up

The owner requested darker door, trapdoor, shelf and cabinet wood to match the
approved planks. `tools/walnut_furniture.py` adapts nine textures from
`art/owner-library/originals/Edited-2026-10-18-Part2/jimbologics/textures`:
`block/walnut_door_bottom.png`, `block/walnut_door_top.png`,
`item/walnut_door.png`, `block/walnut_trapdoor.png`, `block/walnut_shelf.png`
and all four `block/farmersdelight/walnut_cabinet_{front,front_open,side,top}.png`.
Only warm wood colours move to the approved walnut ramp; the supplied pixel
layout, dimensions, cutouts and metal hardware remain intact. Both resource
generation routes apply the same adaptation from originals, preventing
cumulative darkening. Boats and the approved log/plank textures are unchanged.
The before/after sheet and exact alpha/hardware preservation were checked.
This is a texture-only follow-up; the gameplay suite is not rerun.
The texture-only JAR build passes, and all nine packaged images match the
updated resources exactly. The refreshed preview pack passes archive/world
integrity checks. The ordinary client reopened the existing save at the owner's
last position `(1063.14,180,-971.47)` at 07:57; the darker furniture was inspected
in the running game. The owner approved the darker furniture and requested PR #436 ready for review.

The focused local run does not claim the entire server/client suite, two-client
multiplayer, external launcher import, real-day seasonal transitions or a
population-level performance benchmark. CI retains the normal full server suite.

### Foundation CI repair (20 October 2026)

The game build passed, but Foundation exposed two stale checks. The saved item
registry lacked 14 new walnut IDs, and the Batch 6b holly test still expected the
pre-walnut oak shares. OpenAI Codex (GPT-6) refreshed the registry using
`SurvivalRecipeGameTests.exportActualItemRegistryForTheAcquisitionAudit`: all
7,714 real IDs are retained, with 14 additions and no removals.

The acquisition adapter now records the actual walnut log/leaf generation and
seasonal nut/husk harvest. Only the eleven retired basic walnut IDs are marked
as saved-world compatibility entries; they no longer seed fresh-world chains.
Nuts, recovered furniture and boats remain active audited survival items.
Invalid-ingredient checks, unresolved-chain checks and cycle detection remain
intact. The holly test still checks exact species sets, absolute shares and tree
attempt counts, including normal/large walnuts and the reduced default oak slot.

Validation: the single registry-export server test passes with
`-PgameTestFilter=jugcraft-test:*export_actual_item_registry*`; 22 relevant Python
tests pass across survival recipes, agriculture recipe loops, holly and walnut
worldgen. Repository documentation and diff checks pass. No gameplay, textures,
preview saves or dependencies changed for this repair.
