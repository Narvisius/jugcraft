# Tree batch 12: rainforest

Status: owner approved the preview and tap refinement; [PR #420](https://github.com/jimbozoomer-byte/jugcraft/pull/420) is ready for review on `codex/tree-generation-batch-12`.
Requested by the owner on 18 October 2026; actual tool/model: OpenAI Codex (GPT-6).
Based on the owner-approved Batch 11 branch in PR #418. Batch 13 and later are excluded.

## Scope and coordination

The open upstream PR list and upstream/all five public forks' branch inventories
were checked on 18 October. No overlapping public implementation was identified.
Upstream main `ffc7f7ba` is already included in this branch.

- Add renewable tree ferns to Rainforest (9% of tree attempts), Eucalyptus Forest
  (5%) and Temperate Rainforest (3%), preserving each biome's attempt budget.
- Use the existing banana stem, orchard leaves, pup and harvest route. Place
  bananas in Lush Grassland and reconcile the roster's intended absolute shares
  in Rainforest and Tropics without adding duplicate banana blocks.
- Add the roster's taller mahogany fallback as `emergent_mahogany`: a soil-supported
  2x2 mega-jungle trunk, branching crowns and vines. The native mangrove-root
  prototype failed the first flat-soil seed, so it is not shipped. All four base
  columns are checked across twelve seeds. Existing giant-mahogany saplings stay unchanged.
- The owner explicitly included the missing rubber-tree/tapping/latex route.
  It uses the existing canonical `rubber` item and shared Cooking Pot.

## Distributions

All percentages are absolute shares of existing attempts, converted into native
sequential-selector probabilities. Successful growth also depends on clearance.

| Biome | Shares | Attempts per chunk |
|---|---|---|
| Rainforest | Mahogany 50%, giant mahogany 12%, taller emergent 7%, rubber tree 12%, tree fern 9%, banana 10% | 24, occasionally 28 |
| Tropics | Small palm 25%, flame tree 10%, banana 15%, jungle bush 7.65%, palm 42.35% | 4, occasionally 5 |
| Lush Grassland | Banana 45%, oak bush 25%, mango 20%, small palm 10% | 2, occasionally 3 |
| Eucalyptus Forest | Eucalyptus 42%, big eucalyptus 25%, wattle bush 20%, giant eucalyptus 8%, tree fern 5% | 8, occasionally 9 |
| Temperate Rainforest | Fir 38%, redwood 30%, tall fir 14%, mossy maple 11%, willow 4%, tree fern 3% | 11, occasionally 12 |

Bananas retain the established fruit-crop shape, leaves, ripe-leaf harvesting and
Banana Pup. The roster's older separate frond/bunch proposal is superseded by that
working orchard implementation. Its native cherry foliage requires height four;
it is not silently replaced with the invalid height-two proposal.

## Rubber and fern acquisition

Rubber trees have a 9–12-block straight trunk and compact oval crown. Generated
and sapling-grown logs carry a `natural` state, lost when collected and replaced.
A tap attaches to one of the lowest three trunk blocks, with one tap per trunk.
It requires connected natural vertical logs, soil underneath, and at least eight
nonpersistent nearby rubber leaves at the crown. Stockpiles do not produce latex.
Rubber logs cannot be pushed into new productive arrangements with pistons.

One iron nugget and three planks craft a Tree Tap. Its scheduled tick adds one
quarter-bottle every 1,200 loaded ticks. The basic wooden collector holds one
bottle; attaching an empty iron bucket raises capacity to eight bottles (32
loaded minutes). Right-click the placed tap with a Bucket, or craft Tree Tap +
Bucket side by side. An already upgraded tap does not consume another bucket.

Right-click with a stack of Glass Bottles to collect as many whole bottles as
both the held stack and inventory allow. Fractional latex and any remaining
whole bottles stay in the tap. Neither upgrading nor harvesting postpones the
already scheduled fill tick. A stopped full collector resumes when upgraded or
emptied. Empty-hand use reports whole bottles, capacity and the next bottle's
quarter-step percentage. The larger iron collector has a rising visible cream
surface and a front fill gauge at every step, readable even above eye level.
Its item model also shows the bucket upgrade.

A heated Cooking Pot consumes **one Latex Bottle and one Cider Vinegar** in
600 ticks (30 seconds), returning one existing Rubber and both Glass Bottles.
This abstracts coagulation and drying; it retains the canonical material and
industrial route. The bottleneck is four loaded tree-minutes per rubber per tap,
plus vinegar and heat. There is no unloaded catch-up or per-tick block entity.
Breaking a tap loses unbottled contents but retains its paid bucket upgrade in
the dropped item. The next placement starts empty. The existing `latex` property
keeps its quarter-bottle units; old 0–4 saved states remain basic collectors with
the same progress, and the added `upgraded` property defaults to false.

The alchemy catalogue accounts for recoverable glass; rubber keeps its existing
value. The one-latex recipe and both returned bottles pass the no-gain check.
Its shared display name is Rubber, since both routes make the same material.

Rubber logs craft into four jungle planks, saw into six with the existing sawdust
chance, and strip into vanilla stripped jungle logs. They are not fuel logs.
Tree ferns have a 3–5-block fibrous stem and flat frond rosette. Their trunks can
be collected, burned or composted, but do not strip or turn into planks. Fronds
drop fern saplings; shears preserve the foliage. Ferns are excluded from the
timber-producing tree farm. Rubber saplings use that farm's existing timber-only
route; its collected logs are not productive living trees.

## Artwork and implementation

The owner library and tree/plant catalogs were searched. The matching `fern.png`
under the Weapon Smithing slime-foliage collection is a small ground-fern asset,
not a tree-fern trunk or tiled frond texture. The new 16px sprites extend the
established editable Canvas painter in `tools/rainforest_textures.py`, using the
roster's approved palettes. Rubber has three shallow furrows and small cream
latex marks; fern fibres have sparse pale scars and no timber rings. Existing
banana/mahogany textures are reused. No source-library file is altered.

All new blocks use native block models/placements and shared foliage, loot,
recipe, stripping and inspection systems. No new rendering framework is needed.

## Compatibility and validation

New chunks and future sapling growth use the new features; existing chunks are
not rewritten. Existing fruit, timber and recipes retain their IDs. No dependency
or platform change is included. Ferns are decorative renewable plants with no
plank conversion. Bananas continue feeding existing cooking and fermentation.

Validation on 18 October 2026, Minecraft 26.3, Java 25, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3:

- `build-local.ps1 -Tasks @('build', 'runClientGameTest', '-PclientTests=RainforestClientGameTests')`:
  the build and all **1,973 server GameTests passed**. The first client run found
  a harvest display inside a measured tree plot; it was moved to a separate pad
  before rerunning the client test alone.
- `python -m unittest discover -s scripts/tests -v`: **94 passed**.
- `python tools/check_mod_data.py`: **passed**, 3,031 material IDs plus the data,
  recipe and art audit. Agriculture checks include nonfood bottle remainders and
  registered industrial outputs, without introducing duplicate Rubber IDs.
- **707** generated tree, biome, loot, model and handbook documents matched their
  editable generators. Full material generation completed earlier in the run.
- Repository links, whitespace and the 18 pinned pack dependencies were checked.

- `build-local.ps1 -Tasks @('jar', 'runClientGameTest', '-PclientTests=RainforestClientGameTests')`:
  **passed** after the gallery correction; all 12 real-feature specimens, three
  tap displays and the ripe banana display survived save/reopen. Thirteen
  screenshots cover the trees, all five natural stops, the tap and both leaf modes.
- The final bracketed tap model passed the block/item model audit again: **8,433
  models, zero errors**. No server behavior changed after the full server pass.

The exported normal world uses seed `jugcraft` and has a separate authored garden
at `(1000..1120, 180, -1000..-1080)`. Natural surveys found bananas, rubber trees
and ferns in Rainforest; bananas in Tropics and Lush Grassland; and ferns in
Eucalyptus Forest and Temperate Rainforest. These surveys confirm presence in one
world; they do not measure global occurrence rates.

The source save is preserved. The delivery copy was checked across 7,721 saved
region records and every other save file: it contains no test blocks/entities.
Only unused development metadata changed in `level.dat`, `level.dat_old` and
Fabric's registry-presence snapshot; all other NBT and saved files are unchanged.
The cleaned runtime copy opened successfully through ordinary `runClient` (the test
mod absent), and its garden and supplied inventory were observed in the game.
The world ZIP, matching JAR, complete `.mrpack`, tour, screenshots and checksums are
provided locally in `tree-batch-12-2026-10-18`. The package reuses and verifies all
18 pinned dependency entries. The owner approved the visual preview.

External launcher import, a two-account dedicated-server session, live-server load
and an upgrade of a production save are not claimed.

## Measured shape sample

Twelve fixed seeds per shape, repeated to verify deterministic placement:

| Shape | Measured height | Measured width | Trunk/branch blocks |
|---|---|---|---|
| Tree fern | 4–6 | 5 | 3–5 |
| Rubber tree | 10–13 | 7 | 9–12 |
| Emergent mahogany | 20–31 | 14–16 | 81–126 |
| Existing banana | 5–8 | 5 | 4–6 |

These are observed samples, not promises that every possible seed has those bounds.
Seven focused server tests passed, including every base column, wrong soil,
obstacles, sapling growth, timed tap placement/filling/harvesting, duplicate-tap
and stockpile rejection, living-canopy checks, cap, propagation and shared timber
routes. The original two-latex Cooking Pot returned one Rubber and three bottles;
the owner-requested one-latex refinement below supersedes that recipe.

## Bucket-storage refinement (18 October 2026)

Owner-approved follow-up on PR #420, implemented with OpenAI Codex (GPT-6).
The underlying tree shapes and biome distributions are unchanged. The collector
extends the existing native model generator and reuses its metal and latex
textures; no external or source-library asset is modified.

Focused validation for this refinement:

- **13 focused server GameTests passed across scoped runs.** The class run used
  `-I build/focused.gradle -PgameTestFilter=jugcraft-test:rainforest_game_tests*`.
  Twelve passed immediately; the save fixture used pre-26.3 field names. After
  correcting it to `id`/`properties`, only
  `jugcraft-test:rainforest_game_tests_tap_save_states_retain_old_progress_and_bucket_contents`
  was rerun and passed. Gameplay code did not change for that repair.
- New coverage includes bucket consumption and repeated upgrades, eight-bottle
  cap, limited and full batches, off-hand use, full inventories, retained
  fractional latex, the original scheduled fill time, stopped-full resumption,
  crafted and moved upgrades, old-state decoding and new-state round trips.
  The real heated pot returns one Rubber and two bottles from one latex + vinegar.
- **5 Python Batch 12 tests passed**, including generator parity, increasing
  surface/gauge levels and no essence gain through returned bottles.
- **38 tap models passed** the geometry/UV audit. **58 generated documents**
  matched the editable sources. The recipe viewer's rubber-log ordering was
  refreshed to match its existing generator. **40 changed/new resources** were
  byte-identical in the rebuilt JAR.
- `jar` plus `runClientGameTest -PclientTests=RainforestClientGameTests` **passed**:
  the three tap examples, their upgraded flags and contents, 12 trees and banana
  display survived save/reopen. Fifteen screenshots include basic, partial and
  full bucket collectors; the tap screenshots were visually inspected.
- Repository links/whitespace and the 18 pinned pack entries passed their checks.
  The full server/Python/data-audit suites were **not rerun** for this refinement.
  The earlier full-suite results above describe the initial Batch 12 version.

The fresh export is **Rainforest and Upgraded Tap Inspection**, delivered under
`tree-batch-12-tap-upgrade-2026-10-18`. It preserves the previous inspection world.
The cleaned copy retains all 94 source files and all gameplay NBT; only the same
three development-metadata files changed, after scanning 7,723 region records.
The garden supplies buckets and bottle stacks. Basic/partial/full tap samples
are on the north sides of the rubber trees at z=-1001/-1041/-1081, x=1040.
Use `/tp @s 1040 180 -1085 0 0` for the full collector. The owner approved this
storage refinement; PR #420 is ready for review and Batch 13 is excluded.

## Recipe-checker correction (18 October 2026)

The CI material-data check treated the bucket upgrade as a `tree_tap` self-loop
because both variants share the same item ID. The checker now recognizes only
the complete one-tap-plus-one-bucket recipe producing one tap with
`minecraft:block_state.upgraded=true`. Only that recipe's tap-to-tap edge is
omitted. Its bucket-to-tap dependency remains, so a route back to buckets still
fails. Other self-loops, multi-recipe cycles, changed output counts, free or
cheaper upgrades, and unrelated component recipes remain checked.

OpenAI Codex (GPT-6) implemented this checker-only correction. Eight focused
regression tests passed, including the real agriculture graph and its integration
into `check_agriculture()`. The five existing Batch 12 Python tests also passed.
The full data/art audit, Java build and game tests were not rerun for this
Python-only change. No gameplay resources or preview-world files changed.
