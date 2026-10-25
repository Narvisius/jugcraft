# Jimtafacts original wearable presentation

Local October 23 implementation; AI assistance: OpenAI Codex (GPT-6). This is part of the [Jimtafacts integration](jimtafacts-integration.md). It has not been released.

All 45 source-wearable accessory designs use the owner's original mesh definitions. The five source classes under `artifacts/client/item/mesh` were statically decompiled with the workspace's trusted CFR tool, then their declarative cube builders were transcribed into Jugcraft's client source set. The source class files are never loaded as code. Cube sizes, UV offsets, inflation, rotations, child pivots and atlas dimensions are retained. Headwear, necklaces, belt charms, gloves, bracelets and footwear no longer rely on inventory icons as worn geometry.

All 62 original wearable PNGs are copied byte-for-byte, including slim/wide arm atlases and emissive overlays. [The copy manifest](../../art/owner-library/catalog/edited-2026-10-23-jimtafacts/wearable-copies.json) records source paths, SHA-256 hashes and runtime paths. The originals and retained source notices remain in the owner library. `tools/jimtafacts_wearables.py` reproduces the texture copies without executing source programs. Its optional recovery command accepts already-decompiled, inspected mesh source.

The renderer uses the installed Trinkets API rather than the source mod's equipment integration. It follows the wearer's existing pose, including Jugcraft weapon animations, without owning or changing arm actions. Arm accessories follow the actual hand/offhand slot and use the player's slim/wide skin. Original larger boot geometry is selected over worn boots. Cosmetic slots render through Trinkets' normal visibility selection; the shared “Show my worn trinkets” setting is respected. Spectators and invisible wearers are not drawn.

Recovered presentation details:

- Cloud in a Bottle: the original cloud rotates with age/50 and bobs with cosine(age/30)/2 inside its bottle.
- Scarves: the original rear cloth uses the player's cape lean/flap animation.
- Pocket Piston: the head extends in two ticks, stays out through the swing and its four-tick delay, then retracts over two ticks. This is a client visual; damage and knockback remain server-owned. An unseen wearer resets its visual tracker when it is next rendered instead of simulating missed time indefinitely.
- Warp Drive: the original deterministic one-in-ten-per-tick flicker selects among its four supplied emissive overlays.
- Fire Gauntlet, Pickaxe Heater and Night Vision Goggles: original full-bright overlays, with enchantment foil drawn separately.
- Belt charms: the source's four alternating front/back anchor poses prevent multi-slot copies from occupying exactly the same anchor.
- Gloves also render on the corresponding first-person arm through Trinkets' first-person callback.

The reusable food, umbrella and mimic egg have no source wearable layer; their held/item/entity presentation belongs to the accessory gameplay integration. This work does not invent wearable models for them.

Validation is deliberately deferred until the entire requested Jimtafacts implementation is complete, per the owner's instruction. Original geometry/atlas recovery and native rendering are implemented; compilation, first-person and third-person screenshots, slim/wide appearance, simultaneous wearers, animation playback and resource-reload checks remain to run together at the end. Do not treat inventory-icon checks from the earlier slice as evidence for these worn models.
