# Scary creature asset provenance

The four creatures are fan homages to the costumed villains of Scooby-Doo. Their display names are Spectral Cosmonaut, Drowned Diver, Emerald Phantom and Onyx Warden. Stable internal prototype IDs are retained so existing test saves remain readable. No official affiliation is implied.

The integration revision restores the contributor's original alternatives from commit `8f985cf84`, before sampled game recordings and recolored vanilla skull pixels were reintroduced:

- Geometry and suit/ghost/armor textures: project-generated cuboid models and atlases, including the contributor's editable Blockbench model.
- Skull pixels: original procedural drawing from `tools/scary_skull.py`, included in both runtime atlases and the editable model. No recolored Minecraft skull is shipped.
- All 26 runtime sound files: original procedural synthesis from `tools/scary_sounds.py`, under MIT. No game/show voice recordings or sampled chain audio are shipped. Restoring the already-generated files avoids unnecessary encoder differences.
- The skull screenshot is restored from the matching original-art revision. Other model screenshots are unchanged.
- Names, sound mappings, spawn eggs and tags now have editable generation sources in `tools/scary_data.json` and `tools/scary_data.py`.

This applies the owner's fan-homage direction and the repository's [asset rules](../../LICENSE_POLICY.md#fan-homages). Historical third-party files remain in prior commits; they are not part of the integration's runtime tree. No redistribution rights are claimed for those historical recordings or pixels.
