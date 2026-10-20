# Phosphate processing and fertilizers — package 5

Technology package 5, implemented locally with OpenAI Codex (GPT-6). On 16 October
2026 the owner authorized integration with main and local validation of all seven
packages. The earlier testing hold is superseded. **Keep this work local until the
owner has played it and approves publication.**

Current build, test evidence, draft reconciliation and remaining release checks:
[seven-package integration record](tech-seven-local-integration.md). The detailed
verification lists below are release checklists; only results explicitly recorded
there are claimed complete.

## Processing and existing-machine reuse

No new machines, ore deposits, power tiers, tank slots or machine block IDs.
Existing Phosphate from mining or its earlier guano route starts this optional
branch. All numbers are abstract game batches, not real manufacturing procedures.

| Existing machine | Paid batch | Products |
|---|---|---|
| Pulverizer | 1 Phosphate; 160 ticks | 1 Ground Phosphate |
| Chemical Dissolution Chamber | 2 ground phosphate + 1000 mB ordinary sulfuric acid; 320 ticks | 1000 mB Phosphate Slurry |
| Shared Filter Press | 1000 mB slurry; 240 ticks | 750 mB Industrial Phosphoric Acid in outlet 0; 250 mB water in outlet 1; 2 Gypsum-rich Residue in solid output 0 |
| Bulk Chemical Reactor | 1 ground phosphate + 250 mB phosphoric acid; 240 ticks | 2 Wet Superphosphate |
| Shared Industrial Oven | 1 wet superphosphate; 240 ticks | 1 Triple Superphosphate + 125 mB water |
| Mineral Conditioner | 1 Organic Compost + 250 mB water; 160 ticks | 4 Organic Fertilizer Carriers |
| Mineral Conditioner | 2 Digestate Fiber + 250 mB water; 160 ticks | 1 Organic Fertilizer Carrier |
| Compact Chemical Reactor | 1 ammonium nitrate + 1 carrier; 160 ticks | 2 Nitrogen Field Fertilizer |
| Crafting / vanilla Crafter | 1 nitrogen fertilizer + 1 triple superphosphate + 1 carrier | 2 Mixed-farm Fertilizer |
| Bulk Chemical Reactor | 1 mixed-farm fertilizer + 1000 mB water; 240 ticks | 1000 mB existing Nutrient Solution |
| Compact Chemical Reactor | 250 mB phosphoric acid + 500 mB HF; 280 ticks | 1 existing Fluorophosphate Precursor for batteries |

New formed recipes request base 48 JE/t; their shared profiles/upgrades determine
the actual displayed cost. Compact reactor and Pulverizer use their existing
machine energy rates. Recipe picker, exact previews, lock, local automation rules
and blocked-output reservation come from the shared machine implementations.
The dissolution outlet retains its saved identifier; only its label broadens
to explain HF versus slurry. Clear incompatible leftovers before changing jobs.

## Useful agricultural choices

Each new fertilizer treats a 5×5 area one block above/below the target, consuming
one item only if a bone-meal application succeeds. Actual growth follows the
crop's existing rules. No direct yield multiplier, infinite fertility, item
duplication or persistent soil modifier is introduced.

| Product | Tagged specialty | Other eligible crops |
|---|---|---|
| Nitrogen Field Fertilizer | 4 applications: wheat, corn, sunflower, flax, cabbage, oats, barley, rice, leafy vegetables and kitchen herbs | 1 application |
| Triple Superphosphate | 4 applications: roots, legumes, fruiting crop bushes/vines and gourds | 1 application |
| Mixed-farm Fertilizer | 3 applications to any eligible crop | Same |

Membership is defined in the `fertilizer_nitrogen_crops` and
`fertilizer_phosphate_crops` block tags. Candidates must also be in
`minecraft:crops` or use Jugcraft's TallCropBlock (including paddy crops);
decorative flowers, saplings and orchard trees are excluded.
Optional Jugcraft tag entries support feature-disabled packs. Tall plants are
normalized to their bottom and deduplicated, so a three-block plant does not
receive three full treatments. At most 75 candidate positions are scanned per
use/pulse, with at most four bone-meal attempts per unique plant.

Hand use enforces build/item/spawn and town permissions, including the upward
and horizontal growth footprint. Automated application skips town-protected
footprints. No new chunk loads, global search or background tick loop is added.
Tooltips describe the specialty; use feedback reports treated plants or keeps
the unused fertilizer. These are discrete growth applications, not guaranteed
full harvests. Yield/automation economics remain for the deferred balance pass.

Ordinary Fertilizer, ammonium nitrate, bone meal and compost-to-Rich-Soil remain
available. The new carrier consumes unplaced compost; placed compost retains
its existing passive-soil role. Nutrient Solution receives no invisible bonus.

## Sprinkler integration and saves

The existing sprinkler accepts ordinary Fertilizer and the three new blends by
hand, hopper or item pipe. Capacity stays 16; only one item/component variant can
be loaded at a time. New blends retain their crop-group behavior on the existing
fertilizer pulse. Ordinary fertilizer still uses the legacy application path.

Save `fertilizer_stack` with ItemStack.CODEC, accepting the old `fertilizer`
integer as ordinary fertilizer on load. Clamp stored count to capacity. Preserve
the old integer for ordinary loads; new blends write zero there rather than
turning into ordinary fertilizer in an older version. Downgrading is not supported.
Empty-hand interaction shows type/count; sneak-empty-hand retrieves fertilizer
alongside companion buckets. Breaking the sprinkler drops the stored fertilizer.
The existing integer-based companion insertion API delegates to typed insertion
and cannot overwrite a specialist load. Hopper extraction remains disabled.

## Prepared gypsum and building products

Two Gypsum-rich Residue plus 500 mB water in the Conditioner take 240 ticks at
base 48 JE/t to make one Washed Gypsum and 500 mB existing Spent Mineral Wash.
The lost solid portion is represented by the spent wash. One washed gypsum in
the shared Oven takes 320 ticks at base 48 JE/t to make one existing Gypsum Binder.
The existing spent-wash recovery route accepts the waste; no silent drain is added.

One binder plus two sand crafts two Plaster blocks. Two binder, two paper and
one Digestate Fiber craft four Plasterboard blocks. Both have slabs/stairs,
block loot, placement models and editable native 16×16 surface generation.
Existing calcium-sulfate binder and concrete consumers remain. Raw residue is
an item, not a placeable construction block. Industrial acid is not food grade;
cola/preservation chemistry remains a separate future task.

## Authoring and art

`tools/phosphate_processing.py` owns the recipes, groups, handbook and tooltips;
Petro registries expose the eight new items and two bucketable fluids. The full
data/texture generators and the targeted `tools/write_phosphate_resources.py`
writer own the outputs. Targeted resource authoring does not build or test.
Handbook/Encyclopedia pages link phosphate, crop choices, sprinklers, hydroponics,
batteries and construction; recipe-view data includes all machine additions.

Owner library consulted: `originals/Blocks/fertilizer.png` and
`originals/Big Cannons and Mounted Guns/textures/item/phosphate.png` under
`art/owner-library/`. New icons instead extend the repository's native editable
16×16 item maps and shared material palettes to match current factory items.
No downloaded art or owner-library scripts/JARs were executed. Machines retain
the existing olive/silver models and functional connection points.

## Release verification checklist after packages 1–7

- Build, registry/data/icon audits and server/client suites, with optional features disabled.
- Full survival chain, exact hatches/tanks, wrong leftovers, output backpressure,
  recipe lock and saved automation rules; battery and nutrient consumers.
- Crop tags, mixed plots, tall-plant deduplication, mature/blocked plants, town
  and spawn borders, creative/survival use, and two-player interaction.
- Sprinkler legacy-count and typed-stack saves, hopper insertion, no type swapping,
  companion compatibility, retrieval and break drops without duplication.
- Residue preparation/recovery economics, plaster/slab/stair placement and drops.
- Owner-led visuals and farm balance/performance review. No results are claimed yet.
