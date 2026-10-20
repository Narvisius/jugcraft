# Tree batches 11, 11b and 11c

Status: implemented on `codex/tree-generation-batch-11-11bc`; owner visually approved
on 18 October 2026 for review in [PR #418](https://github.com/jimbozoomer-byte/jugcraft/pull/418).

Requested by the owner on 18 October 2026. Implemented with OpenAI Codex (GPT-6).
Scope follows `docs/branches/TREES.md`: flame tree, sea grape, osmanthus,
shoreline driftwood, banyan/fig wood, and mango. Pandanus is omitted as recommended.
Batch 12 and later are outside this change.

The public upstream PR list and all five public forks' branch inventories were
checked before starting. No overlapping Batch 11 work was identified. Current
upstream main (`ffc7f7ba`) is already an ancestor of the approved Batch 10 branch.

## Intended behavior

- Flame trees use acacia timber and a low scarlet umbrella crown in Tropics and
  Jacaranda Glade; osmanthus uses olive timber and a small flowering crown in
  Karst Pinnacles. Both have their own leaves and saplings.
- Sea grape replaces the Overgrown Beach oak bushes with low fruiting shrubs.
  Dune Beach and Gravel Beach gain occasional supported horizontal driftwood.
- Banyans replace Shrine Springs' small oak share, retaining great oaks. Their
  wide crowns, narrow prop roots and hanging aerial roots use a new fig wood set.
- Mango replaces Lush Grassland's jungle-tree share and part of its jungle-bush
  share, keeping existing tree attempt counts and palms. It has a new wood set.
- Renewable sea grapes, mangoes and osmanthus flowers use shared harvest and food
  systems: jelly, chutney, fermentation, candy flavour and mooncake filling.

New generation affects new chunks only. Registration remains stable when the
biomes switch is disabled. No new dependencies or platform upgrades are planned.
## Distribution and generation

Shares below are absolute selection probabilities, converted to vanilla's
sequential selector chances. Counts remain attempts per chunk, not guaranteed
trees. Soil, space and terrain can reject a selected tree.

| Biome | New share | Existing attempt budget | Other species |
|---|---|---|---|
| Tropics | Flame tree 10% | 4, occasionally 5 | Small palm 25%, jungle bush 15%, banana 7.65%, palm 42.35% |
| Jacaranda Glade | Flame tree 8% | 6, occasionally 7 | Cherry 7%, oak 17%, azalea 6.8%, big jacaranda 15%, jacaranda 46.2% |
| Overgrown Beach | Sea grape 60% | 1, occasionally 2 | Palm 20%, small palm 20% |
| Karst Pinnacles | Osmanthus 15% | 6, occasionally 8 | Spruce bush 20%, pine 65% |
| Shrine Springs | Banyan 14% | 2, occasionally 3 | Jungle bush 30%, great oak 56% |
| Lush Grassland | Mango 20% | 2, occasionally 3 | Oak bush 30%, small palm 10%, jungle bush 40% |

The old planning table treated some conditional chances as absolute shares. The
implementation preserves actual existing companion shares: mango uses the 7%
jungle-tree share and 13 points from jungle bushes; flame uses Tropics' 9% azalea
share and one point from palms. No Batch 12 banana expansion is included.

Dune Beach attempts one driftwood strand in six chunks; Gravel Beach one in
twelve. Both remain treeless. A strand is 3–6 stripped dead logs on a horizontal
axis, with no stump. Every log needs sand or gravel directly underneath and air
at its destination. Placement requires water within four blocks and an elevation
from sea level through two blocks above it. A fixed 9×9 water check and a maximum
six-block footprint are validated before any writes; obstructed, inland,
unsupported or flooded placements fail without partial logs.

Flame trees use a forked acacia trunk with a low scarlet umbrella; sea grape uses
one concealed oak log in a rounded spreading bush; osmanthus uses a small forked
olive trunk. Banyans use four spreading fig limbs, a broad low crown, narrow prop
roots, trunk vines and sparse vanilla hanging roots. Vanilla 26.3 leaf blocks
provide no support faces, so banyan leaves alone provide an underside support
face for those roots; the other five faces keep ordinary leaf support behavior.
Roots disappear when their supporting canopy is removed. Mango has a stout straight
trunk and dense rounded evergreen crown. Saplings reproduce each species and
sea grape saplings also accept dry-vegetation soils such as sand. Existing trees
and generated chunks are not rewritten.

Measured over twelve fixed seeds per species (height / widest span / leaves):

| Species | Height | Width | Leaf blocks |
|---|---|---|---|
| Flame tree | 5–7 | 9–12 | 93–148 |
| Sea grape | 2–3 | 5 | 28–31 |
| Osmanthus | 4–6 | 3–4 | 18–46 |
| Banyan | 9–12 | 14–15 | 466–538 |
| Mango | 7–9 | 9 | 212–217 |

Vanilla 26.3 blob crowns add height above the trunk and spread farther at their
lower layers. Mango's trunk base is six to deliver the intended 7–9-block tree;
osmanthus uses a one-block base plus two random additions to keep its upward
random-spread foliage in the intended 4–6-block shrub-tree range. Banyan's wider
spread comes from a central crown and smaller crowns at the branch tips. These measured ranges
are a seed sample, not a claim that every terrain placement succeeds.

The owner's 18 October branch follow-up replaces banyan's short vanilla rising
offshoots with a dedicated native trunk placer: four limbs spread 3–4 blocks
from staggered heights, rise through connected bends, and sometimes fork for
another 1–2 blocks. Horizontal logs follow the limb direction. A blocked limb
stops at the obstacle; no log is placed beyond a gap. Twelve seed samples have
39–49 fig logs including the unchanged prop-root system (previously 15–23).
The canopy is raised one block to reveal the spreading limbs from below.
The biome share, sapling route and other tree species are unchanged. Existing
trees are not rewritten; the revised inspection export is `tree-batch-11-preview-r4`.

## Harvests, recipes and costs

Sea grape, osmanthus and mango use the existing three-stage fruiting-leaf system.
Natural leaves near logs advance a stage with a 1/10 chance per random tick.
Sea grape and osmanthus can use exposed sides as well as the canopy underside;
mango uses the underside. Hand or shears right-click on a ripe leaf gives 1–2
sea grapes, osmanthus flowers or mangoes, preserving the leaf and resetting the
harvest stage. Repeated clicks before regrowth yield nothing. Persistent
player-placed decorative foliage does not grow or yield harvests. Breaking ripe
leaves without shears/Silk Touch can drop the harvest; preservation tools return
the leaf item without copying ripe state or also duplicating produce.

- Sea grapes restore 2 hunger, mangoes 3. Both use `jugcraft:fermentable`; mangoes
  also join vanilla parrot food. Existing fermentation machinery and its costs
  are reused.
- Cooking Pot: 6 sea grapes + 2 sugar + 1 Mason Jar → 1 Sea Grape Jelly, 300 ticks.
- Cooking Pot: 4 mangoes + 1 cider vinegar + 1 sugar + 1 Mason Jar → 1 Mango
  Chutney, 300 ticks; the vinegar bottle is returned through the shared route.
- Both preserves have four servings and retain existing spoilage, canning and
  pantry behavior. Jelly restores 3 hunger/0.4 saturation modifier per serving;
  chutney 3/0.5.
- Cooking Pot: 2 osmanthus flowers + 2 wheat + 1 sugar + 1 egg → 4 Osmanthus
  Mooncakes, 300 ticks. These use the existing mooncake food and full-moon Luck
  behavior, without a separate crafting machine.
- Osmanthus flowers are a Candy Kettle flavour giving 30 seconds of Luck per
  finished piece under the existing candy rules; sugar, heat, stage, flavour
  limit and tray costs are unchanged.

Fig and mango each have the established nine-block wood family: log, wood,
stripped log/wood, planks, stairs, slab, fence and gate. Hand crafting yields four
planks per log; the powered sawmill yields six. Stripping, fire, fuel, charcoal,
tags and recipe presentation reuse the wood framework. Tree farms return the
matching renewable sapling and timber; sea grape yields one log, not six.
Banyan timber is `fig`, but foliage/sapling identity remains `banyan`, with no
edible fig item. There are no new progression tiers, power systems or dependencies.

## Art and provenance

The shared owner-library guide and complete 26,965-file catalog were inspected;
no matching species-specific source textures were found for these five trees.
New 16×16 foliage, produce and saplings use editable deterministic painters in
`tools/tropical_tree_textures.py`, consistent with the approved tree batches.
Fig uses owner painting 8's taupe, adjusted to `#7f6f63`; mango uses the second
set's elm row, adjusted to `#938167`. Their bark/grain uses `tools/wood_style.py`
and the documented owner palette in [wood repaint](wood-repaint.md). New wood
entries are appended so existing wood seeds and pixels remain unchanged.
No source-library originals are edited. Preserves and mooncakes extend their
existing painters; vanilla hanging roots and existing dead wood are reused.

Owner bark-detail follow-up (18 October, OpenAI Codex GPT-6): olive and fig log
sides retain their existing five-color palettes and gain short shaded grooves,
broken ridge highlights and small bark patches. The editable painter changes
only these two bark outputs; wood blocks inherit their log-side textures.
Planks, stripped wood, log ends and tree generation remain unchanged. Both
16×16 opaque textures were visually checked alone and tiled. Full gameplay
tests were not repeated for this texture-only preview. The owner approved both
bark textures together with the revised banyan on 18 October 2026.

## Validation and inspection

Validation on 18 October 2026 uses Minecraft 26.3, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3 and the unchanged eighteen-library framework lock.

- `python -m unittest discover -s scripts/tests -v`: all 90 tests pass.
- `python scripts/check_repository.py` and `python scripts/package_modrinth.py check`: pass.
- All 601 tree and biome JSON outputs match their current generators.
- Focused `TropicalTreeGameTests`: all six pass, including twelve varied seeds
  per shape, reproducibility, species timber, sapling obstacles and soil,
  underside-only root support/removal, hand/shears harvest and repeat-click
  rejection, persistent foliage, wood/farm/sawmill recipes, actual Cooking Pot
  output and vinegar-bottle return, driftwood length/axis/support/obstruction.
- `./build-local.ps1 -Tasks @('build','runClientGameTest','-PclientTests=TropicalTreeClientGameTests')`:
  full build passes; all **1,965 server GameTests** pass; the normal-world client
  gallery passes and reopens all fifteen trees and the harvest/root displays.
- The expanded client run passes in 3m 5s and exports the fresh normal-world
  save `build/tree-batch-11-preview-r2/Tropical Trees and Coastal Shrubs Inspection`.
  All eight natural biome stops contain the intended addition: six tree-biome
  stops plus both coast types. Dune Beach has a five-log strand starting at
  `(2100, 64, -5054)`; Gravel Beach has a three-log strand at `(-186, 64, 2930)`.
  These exact blocks and their biomes were independently read from the exported
  region NBT; no logs were planted at natural stops.
- All six focused tests also pass with `-PjugcraftOptionalIntegrations=false`.
- `python tools/check_mod_data.py`: full final audit passes, 3,023 registered
  material IDs; art audit covers 8,428 block/item models, 3,707 quad parts and
  171 combined models.
- Inspection preparation scans 30,273 saved region records and all other save
  files, finding no actual test content. All 163 source files remain untouched.
  The delivery copy changes only `level.dat`, `level.dat_old` and
  `data/fabric/registry_entries.dat`, removing unused development selections and
  five unused registry names. Every other save byte is verified unchanged.

The ordinary client successfully joins the cleaned runtime copy at
`(1120.5, 180, -987.5)`, facing the banyan display, at 12:08 on 18 October.
It was opened for owner inspection.

The first focused run caught vanilla hanging roots lacking leaf support; the
banyan-specific underside support and its removal test fix that behavior. The
full data audit also caught legacy banana metadata comparing the old conditional
chance. Metadata now follows the new selector while the regression preserves
banana's actual 7.65% share; the targeted orchard audit passes.

No claim is made for external launcher import, multiplayer load testing or a
server deployment. The inspection save freezes time/weather/random ticks for
comparison and has an authored garden separate from its untouched natural
biome samples. The owner approved the revised inspection world for PR #418.

Follow-up validation for the bark and banyan revisions: all seven focused
`TropicalTreeGameTests` pass, including the new twelve-seed connected-branch
check and a solid-wall obstruction check. The three Batch 11 Python tests pass.
The selected client gallery passes with fifteen trees saved and reopened,
natural additions in all eight sampled biomes, and a close render showing the
limbs beneath the raised canopy. The full server suite was not repeated.
The fresh `tree-batch-11-preview-r4` export preserves the earlier inspection
worlds. Its delivery copy passes the save-content scan and byte comparisons;
both archives match the rebuilt JAR and unchanged eighteen-dependency lock.
The ordinary client joins the revised world at `(1120.5, 180, -987.5)` at 14:29;
the ground-level view shows the projecting limbs beneath the canopy. This
preview includes the olive/fig bark revision. The owner approved these changes
and requested publication for review on 18 October 2026. Batch 12 remains unstarted.
