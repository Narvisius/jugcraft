# October 19 combined publication

The owner authorized publishing the previously local content and integrating ready
pull requests on 19 October 2026. Actual integration tool: OpenAI Codex (GPT-6).
This record describes the combined candidate; the linked PR's final checks and
merge state determine whether it has reached main.

Publication PR: [#435](https://github.com/jimbozoomer-byte/jugcraft/pull/435).

## Included local milestone

- Seven technology packages, shared machine interfaces and production chains.
- Jug Nouveau, owner collections, original creature geometry/UVs/animations and
  the recovered native encounter adapters documented in the recovery catalog.
- Survival recipes and acquisition audits; Mecha and Tank assembly bays.
- Gun reload/crash repairs, town refinements and Diorama in the Complete pack.

## Previously separate contributions combined here

These source branches are incorporated together so their shared tags, recipe
catalogue, essence values, registrations and test entrypoints are validated as
one candidate. They must not be closed as incorporated until #435 is merged.

| Source PR | Included content |
| --- | --- |
| [#399](https://github.com/jimbozoomer-byte/jugcraft/pull/399) | Juniper, gorse and sagebrush |
| [#404](https://github.com/jimbozoomer-byte/jugcraft/pull/404) | Holly, renewable sprigs and door wreaths |
| [#408](https://github.com/jimbozoomer-byte/jugcraft/pull/408) | Olive trees, timber and pickled olives |
| [#411](https://github.com/jimbozoomer-byte/jugcraft/pull/411) | Nut pines, wattle, tamarisk and Joshua trees |
| [#413](https://github.com/jimbozoomer-byte/jugcraft/pull/413) | Giant fungi, Nether brambles and dragonblood trees |
| [#417](https://github.com/jimbozoomer-byte/jugcraft/pull/417) | Baobabs and hanging fruit |
| [#418](https://github.com/jimbozoomer-byte/jugcraft/pull/418) | Coastal shrubs, banyans, mangoes and driftwood |
| [#420](https://github.com/jimbozoomer-byte/jugcraft/pull/420) | Rainforest vegetation, rubber trees, tapping and latex |
| [#412](https://github.com/jimbozoomer-byte/jugcraft/pull/412) | Traveler's Backpack progression and Back slot; optional standalone integration |

## Integration repairs

- Track the two archwood log tags that the generic `logs/` ignore rule hid.
- Preserve imported resources during regeneration. A manifest owns generated
  paths; only stale paths in that manifest may be removed. Authored packs and
  creature catalogs are retained, including when they share generated folders.
- Apply explicit, preconditioned authored JSON repairs after generation to retain
  the approved models, labels, sounds, atlases and loot adaptations.
- Sort imported particle/banner inventories; normalize gzip headers and tiny
  trigonometric coordinate differences; use reproducible small-texture encoding.
  Before/after RGBA hashes matched for all affected town textures.
- Pin the acquisition audit's vanilla/Fabric reference inventory so it runs on
  CI without depending on the owner's Windows cache paths.
- Correct the galena essence seed and the audit's large-denominator batch math.
- Give only functional imported blocks the EntityBlock interface. Ordinary
  foliage and decoration no longer advertise nonexistent saved block entities;
  storage, cooking, fans and cave devices retain their entities and menu/events.
- Preserve rare Mushroom Fields/Flower Isle surface regions when applying the
  imported regional landscape overlay. Existing chunks are not rewritten.
- Keep the Halloween loot test's plain control mob explicitly uncostumed even
  when the real-world October spawn hook runs. Preserve the production event.
- Inspect a bounded set of natural Bayou samples for wet cypress rather than
  assuming the closest dry fragment contains it. No trees are planted by that
  natural-presence assertion.
- Preserve both Diorama and Backpack dependency pins and all test entrypoints.

## Validation record

The pre-tree/backpack combined build passed all **2,014 local server GameTests**.
After the reproducibility fixes, **92 Python tests** passed and regenerated town
textures retained identical pixels. The subsequent combined tree/backpack main,
client and GameTest sources compiled successfully. These are intermediate results,
not a substitute for the final combined Build/Foundation workflows on #435.

The combined runtime registry was exported by a passing server GameTest. The
updated acquisition audit has zero invalid ingredients and zero unresolved active
recipe chains; 78 explicitly deferred acquisition designs remain labeled as such.
All 135 Python checks pass locally. GitHub verified generated resource
reproducibility; the integration pass also corrected the two Pine Nuts display
names and refreshed the merged library matrix. Natural surface inspection now
checks distinct regional anchors when its nearest 3-D biome sample is underground.

No owner playtest, two-account multiplayer certification, live-server deployment
or production-save migration is claimed. The owner's local saves are untouched.

The full combined run subsequently passed both 2,075-test server configurations,
resource/model audits and installer packaging. Client validation found a Backpack/
Diorama N-key collision; the repair reserves N for the backpack and comma for new
waypoints, including Controls Reset. The mineral-sand survey now uses all approved
garnet biomes for its bounded retries and skips duplicate chunk centers; terrain
protection, rarity, visible natural deposit assertions and save/depletion checks
are unchanged. Final client validation of these repairs remains required.

The 16-group run also found four natural-tree surveys stopping at tiny surface
occurrences (including a dry-river sample with only 12 dry columns). Those
surveys now continue through bounded distinct regional anchors, with normal
chunk-unload ticks between them. Every target biome still requires its natural
species; no trees are planted at survey stops and generation rates are unchanged.

The full biome run exposed two placement collisions. Erupting Inferno now uses
its own Nether climate point instead of shadowing Magma Fields. All eight End
highlands biomes now share one noise-calibrated weight calculation: the original
four retain 12.5% each, the four owner additions receive 6.25% each, and vanilla
retains 25%. Outer Flats keeps its intended conditional barrens share. This
changes only newly generated terrain. Regression tests check the combined picker
thresholds and both competing Nether climate centers.

## Not included merely because a draft exists

- Stacked bulk-factory drafts #361–#365, #374 and #387 still need reconciliation
  with the approved seven-package machine/progression design; known baseline
  failures must be resolved before a separate merge.
- #401, #402, #405 and #409 overlap the existing battery, ceramics and structural
  alloy routes. Preserve canonical items and progression rather than registering
  competing versions. See `tech-seven-local-integration.md` for the decisions.
- #390 and #419 propose competing titanium equipment sets; #419 also has failed
  checks. #430 fails the extrusion material-balance audit; #421 lacks required
  essence values. They remain drafts for focused correction/review.
- #406, #407 and #422 are proposals, not evidence of completed gameplay.
- #360, #370, #372, #382, #391–#394, #400, #403, #410, #414 and #416 remain draft
  submissions; individual feature/progression review is still outstanding.
- Creature recovery still has explicitly documented special-behavior/animation
  parity work and four missing original animal geometries. This publication does
  not label those unfinished systems complete.
