# Tree batch 14: orchard fruit

Status: owner-approved and ready for review in PR #437 on `codex/tree-generation-batch-14`, based on
upstream main `511b7ec0`. The owner requested Batch 14 on 20 October 2026 after
approving Batch 13. Actual tool/model: OpenAI Codex (GPT-6).

The open upstream PRs and branch inventories of all five public forks were
checked before starting; no competing Batch 14 tree implementation was found.
Existing kitchen orchard fruit, seeds, recipes, harvest states and saved IDs are
reused. This branch does not include unmerged Batch 13 changes.

## Scope

- Orchard keeps its 2-3 tree attempts per chunk. Replace the 20% oak-with-bees
  selection with the existing apple tree and a native 2% bee-nest decorator.
  Pear is 15%, peach 10%; retain the current azalea and plum shares, with chestnut
  taking the remainder. Express final shares through conditional probabilities.
- Pear: upright 7-8-block tree, existing fruitwood logs and existing pear foliage.
  Fruitwood recipes work under either agriculture or biomes.
- Peach: low 4-5-block forked/vase crown on vanilla cherry logs, reusing the
  existing connected small-branch trunk placer and native blob foliage.
- Lemon and orange: dense round 4-5-block trees on vanilla oak logs. Subtropics
  orange becomes 10%, removing birch and retaining other species' absolute shares
  except the default azalea remainder. Mediterranean citrus stays 5% each.
  Existing rare vanilla-biome patches remain unchanged.
- Bring apple foliage onto the shared deterministic leaf painter; align pear,
  peach and citrus palettes/blossoms with the tree roster. Preserve fruit-stage
  states and the existing fruit/seed/item designs. No new timber set.
- Pears feed the existing Cider Press; all four orchard fruits are fermentable.
  Add orange as a Candy Kettle flavour (short Jump Boost) using the existing
  flavour system. Existing juices, pies, marmalade and preserves stay intact.

## Selection shares

These are final probabilities per tree attempt, not sequential-selector inputs.
Orchard and Subtropics retain 2–3 attempts per chunk; failed placements are not
replaced by extra attempts.

| Biome | Final tree shares |
| --- | --- |
| Orchard | Apple 20%, azalea 8%, pear 15%, peach 10%, plum 6.912%, chestnut 40.088% |
| Subtropics | Live oak 30%, small palm 12.6%, vine oak 5.04%, lemon 6.804%, orange 10%, azalea 35.556% |
| Mediterranean Forest | Lemon 5%, orange 5%; other selections unchanged |

Apple's 2% native bee-nest decorator applies only to the new Orchard variant.
Existing apple saplings and Flower Isle placement retain their original feature.
The existing rare vanilla-biome orchard patches keep their one-in-16-chunk attempt.

## Validation and delivery

Local validation on 20 October 2026:

- Material/art/recipe audit: PASS, 6,136 material IDs. After the final crown
  corrections, the focused orchard and wood-switch audit also passed.
- Ten Python tests passed: Batch 14, tree biome mix and tree roster. These check
  absolute shares, attempt budgets, existing apple feature compatibility,
  processing tags, deterministic textures and retained fruit item artwork.
- All 16 focused server game tests passed: existing `OrchardGameTests` plus
  `OrchardWorldgenGameTests`. Includes sapling growth, harvest/regrowth and loot,
  12 seeds per shape, repeated-seed identity, unsupported/blocked planting,
  connected peach branches, and actual pear pressing/bottling and orange candy
  input/pouring with ingredient consumption.
- `OrchardWorldgenClientGameTests` passed: 15 real-feature trees, 15 reachable
  fruit-stage displays, all three natural biome stops and save/reopen checks.
  Both cutout and solid foliage were rendered. Screenshots inspected locally.
- Build succeeded. All 39 changed/new resource files and the compiled candy
  flavour class match the release JAR exactly. Full resource processing ran
  before the focused final rerun; that rerun used `-x processResources` after
  copying and byte-verifying the three corrected tree JSONs into its output.
  No test task was skipped. The full unrelated server/client suites were not run.

Measured shape envelopes across the 12 seeds:

| Tree | Height | Crown width |
| --- | --- | --- |
| Apple | 5–6 | 5 |
| Pear | 7–8 | 7 |
| Peach | 4–5 | 6–7 |
| Lemon/orange | 4–5 | 5 |

Normal-world seed: `jugcraft`. Inspection garden starts at `(1120, 180, -988)`.
Natural Orchard stop: `(952, 84, 1288)`; Subtropics: `(1096, 86, 1496)`;
Mediterranean Forest: `(2184, 124, 736)`. Surveys found apple, pear and peach in
Orchard and both citrus species in each warm biome. These are observed leaf
populations, not claimed empirical tree frequencies.

Delivery: `tree-batch-14-preview-2026-10-20/`, with a matching Complete-pack
Modrinth archive, standalone JAR, world ZIP, screenshots and teleport tour.
The source export is preserved. Cleanup scanned 6,958 saved region records and
all other save files: no actual test content. Only unused test registry entries
and test-mod metadata were removed from three metadata files in the delivery copy;
all other NBT values and every other saved file were verified unchanged.

The first runtime test caught unsupported leaves from vanilla forking trunks
combined with blob foliage. Peach now reuses the existing face-connected small
branch placer; the test checks every leaf and the full log connection to the root.
The compact citrus crowns use three leaf layers (native blob height 2), leaving
fruiting clearance beneath even the shortest trees; four layers touched the ground.
No world save, build or library original is
committed. New tree shapes affect new chunks and newly grown saplings only;
existing registered blocks and items remain loadable. No platform/dependency
changes or later batch work.

External launcher import and separate-process multiplayer are not claimed.

The cleaned runtime copy opened in the ordinary client at `(1120.5, 180, -987.5)`;
local player join was verified in the client log.

### Foundation CI repair (20 October 2026)

The Build workflow passed; Foundation failed one older Batch 5 assertion that
required every tested tree to use only the biomes switch. Rowan shares fruitwood
with orchard pears, and Batch 14 intentionally enables that timber through either
biomes or agriculture. The test now asserts both switches for rowan and retains
the exact biome-only expectation for elder, alongside its existing timber and
renewable sapling checks.

OpenAI Codex (GPT-6) reproduced the failure and verified the fix with all 18
relevant Python tests in Batch 5, Batch 5b/5c, Batch 14, tree biome mix and tree
roster. Diff checks pass. This repair changes only the test and this record;
the approved gameplay, textures and inspection world are unchanged.
