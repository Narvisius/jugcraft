# Trees, chemistry and progression integration — 20 October 2026

Owner selected PRs #436, #437, #391, #392, #393, #400, #403, #410, #421 and #430
for combined repair, validation and merge. Base: main `511b7ec0`.
Actual integration tool/model: OpenAI Codex (GPT-6). Original contributor attribution remains.

## Player outcome

Walnut forests, Basin bristlecones and orchard tree/fruit improvements; aspirin;
boric acid/control rods and boron solar manufacturing; sour candy and corn syrup;
PVAc glue and silicone gaskets; aged pine/maple finishes; improved extruder yields,
clean-biogas fuel credit and fire-resistant dark matter tools.
Existing IDs, owner artwork, shared machines and seven-package progression remain.
Worldgen changes affect new chunks. No dependencies or platform pins change.

## Integration repairs

- Combine walnut and orchard wood switches; retain walnut compatibility aliases.
- Preserve orange, sour and existing osmanthus candy flavours and their stable names.
- Merge additive registries, recipes, generators and test entry points without
  reverting the already integrated technology and owner content.
- Full-yield sheet/insulation/hose extrusion returns no scrap. Film/profile batches
  retain allocated offcuts. Tests use stock-equivalent units instead of treating
  every finished shape as one raw stock, while rejecting extra recoverable material.
  Saved HDPE/rubber offcuts remain registered and reclaimable; new full-yield
  recipes no longer produce them, and the acquisition audit labels that explicitly.
- Explicit chemistry essence budgets account for glucose syrup's returned bottle,
  borax's two boric-acid output, sheet yield and silicone gasket yield. Aged wood
  inherits plank values and its shaped forms derive from their recipes.

## Validation

All 155 local Python tests passed. Repository/documentation checks passed.
Local material/model/recipe audit passed: 6,165 material IDs, 11,744 block/item
models, 3,713 quad parts and 173 combined models. Compilation of main, client and
game-test sources passed after correcting the new inspection fixture's screen API.
The actual-registry export server test passed; the refreshed acquisition audit has
zero invalid ingredients and zero unresolved active chains (78 explicitly deferred
acquisition designs remain). The source PNG encoding repair preserves approved pixels.

Combined server tests with optional integrations present/absent and selected
rendered-client tests are required before merging. Final exact-head run links and
results are recorded on [integration PR #439](https://github.com/jimbozoomer-byte/jugcraft/pull/439).
Historical source-branch checks do not certify this combined candidate.
No human playtest, two-account dedicated-server test or live deployment is claimed.

## Integrated source heads

| PR | Reviewed source commit |
| --- | --- |
| #436 | `80768723c5aa5c86f8615d7c5ef6ea3682c2ee38` |
| #437 | `80edd071794538f7de0875de1bd780ed8cb4466e` |
| #391 | `6ead635d342a9f17e2e76e645b9c3eba4e36b763` |
| #392 | `6edebc9122c3e224bbccfe3a7768b8e5b6d92b4d` |
| #393 | `8c61aef2e90d46768f52a8b18b6ada384e58435a` |
| #400 | `66fa32b150555d81504a25015fcb0d412598fc08` |
| #403 | `38c39bffed0c7d7c3fda578c6cb55f17562c52d9` |
| #410 | `945623d0bb7e89e2953104069e41bb419a2127db` |
| #421 | `8eb8c7e7f9b6fdc13f1055911f8f470d90224b1d` |
| #430 | `ad94fcc2cb61c961877dcc6e12fe46657493f33c` |
