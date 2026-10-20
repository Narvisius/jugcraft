# Remaining tree roster

Owner authorization, 15 October 2026: after completing tree-generation batch 2, implement all the other trees. Actual tool: OpenAI Codex (GPT-6).

**Scope update, 16 October:** batches 3, 4 and 4b/4c have since been implemented, tested and visually approved. The owner requested PR #395 ready for review and subsequently approved batch 5 and requested PR #396 ready for review. Batches 5b and 5c are now authorized together; batch 6 and later remain outside the current implementation.

The owner approved the new wood palettes on 6 October, and the original pine textures on 15 October. Follow the current runtime vanilla-log treatment for other woods and preserve pine originals. The roster defaults apply unless later code or owner instructions supersede them. Existing orchard, banana and rubber content must be extended without duplicate IDs or replacing their existing routes.

Implementation follows [the roster](../branches/TREES.md) in dependency order. Each wood remains a focused change with its own feature record and review evidence. Batch 2 is kept separate in PR #386. New tree shapes and biomes are for new chunks; no terrain rewriting, release, merge or server deployment is included.

| Stage | Scope | Status |
|---|---|---|
| 3 | Yew and ancient yew, leaf drawing options, Gloomweald canopy | Owner approved; PR #388 ready |
| 4 | Tree/wood split; snowpetal cherry and shimmer birch | Owner approved; PR #389 ready |
| 4b-4c | Bald cypress roots; magnolia | Owner approved; PR #395 ready |
| 5 | Seasonal fruit; rowan, elder and fruitwood | Owner approved; PR #396 ready; [record](trees-batch-5.md) |
| 5b-5c | Hawthorn; persimmon | Implemented; owner inspection pending in draft PR #397; [record](trees-batch-5bc.md) |
| 6-6b | Juniper, gorse, sagebrush, holly | Pending |
| 7-8c | Olive, stone pine, wattle, tamarisk, Joshua tree | Pending |
| 9-9b | Giant fungi, bramble, dragonblood | Pending |
| 10 | Baobab and date palm with hanging fruit | Pending |
| 11-11c | Warm/coastal trees, driftwood, banyan, mango | Pending |
| 12 | Banana integration, tree fern, emergent mahogany, rubber trees/tapping/latex | Implemented; owner inspection pending in draft PR #420; [record](trees-batch-12.md) |
| 13-14 | Walnut, bristlecone, existing orchard wood/biome updates | Pending |

Publish short shared-system notes before the tree/wood split and seasonal-fruit changes. Test growth, soil, obstacles, drops/replanting, seasons/fruit where relevant, and real new-world distribution. Keep inspection-world evidence distinct from artificial sample plots. Record any deliberate differences from the planning document and actual test limits.
