# Shared owner asset library

**Owner clarification, 9 October 2026:** the owner says they rebuilt the supplied work using other mods as starting bases, but some earlier names, credits and supporter text remained. [Read the standing implementation rule and metadata locations](PROVENANCE.md). These labels alone are not a reason to reject the collection or ask for the same permission again; use suitable files and address specific issues at the affected file.

The project owner supplied this collection on 6 October 2026 and explicitly authorized Jugcraft contributors and AI coding agents to **use suitable assets directly, recolor or adapt them, or use them as visual/audio reference**. Check this library before making new assets.

It is shared across the whole project: **blocks, ores, metals, machinery, guns and mounted weapons, planes and airships, sounds, animations, weapons, armor, trees, biomes, farming, food and accessories**. The folder name `Blocks` is the original collection name, not a restriction on who may use it.

## Find assets

**New: [owner magic collection and direct-use instructions](MAGIC_ASSETS.md) (8 October 2026).** Contributors and AI agents SHOULD use suitable files from these 7,909 textures, models, animations, sounds and data resources as supplied. The owner explicitly does not require redesigning, recoloring or renaming them. No repeat per-asset permission request is needed; make necessary technical compatibility changes in runtime copies and keep the source library intact. This collection-specific instruction takes precedence over generic art defaults.

- [Browse every category and file](catalog/README.md).
- [Download or search the complete CSV inventory](catalog/files.csv): filenames, types, image dimensions, animation sidecars and SHA-256 checksums.
- [Browse the original collection](originals/Blocks).
- [Browse the magic originals](originals/Magic), their [paginated catalog](catalog/magic/README.md), or [download the unchanged RAR](archives/magic-owner-2026-10-08.rar).
- [See counts and verification notes](catalog/summary.json).

Use the category pages for browsing and the CSV for searching across the collection. Texture metadata stays beside its image; sound and animation subfolders keep their supplied structure. Similar or duplicate files stay in place so references and animation pairs are preserved.

## Allowed project use

The owner's instruction is: "use these as either direct reference / straight up take to recolor or to directly use if they fit what they are working on."

Choose the approach that fits the feature. Direct reuse and recoloring of this owner-supplied collection are authorized; contributors do not need to ask again for each asset. Keep the relevant branch's material palette and art direction coherent. Record the source path and any changes in the feature's asset provenance.

The source of this import is the project owner's upload, described by the owner as textures they created. Filenames are preserved labels, not independently verified authorship or license evidence for other sources. Follow [the repository's license policy](../../LICENSE_POLICY.md) and retain any asset-specific attribution or license notices when integrating assets. This instruction covers this supplied collection; it does not grant permission for unrelated third-party material.

## Import into a feature

1. Choose a suitable file from the catalog. Keep `originals/` intact as the shared source collection.
2. Copy the required asset into the feature's runtime resources under `src/main/resources/assets/jugcraft/`, with a stable lowercase resource name.
3. Copy and adapt any adjacent `.png.mcmeta` together with its PNG. Check the frame layout and timing. Animation JSON needs matching model bones and a renderer that supports its format; audio needs an appropriate `sounds.json` entry. Publishing the library does not register those resources in the game.
4. For a sheet or packed atlas, inspect the layout and use the intended tile or UV mapping. Catalog dimensions distinguish individual textures from sheets; do not stretch a whole sheet onto one block face.
5. Describe direct reuse, recoloring, cropping or other modifications in `docs/features/`. Include the library path or CSV checksum, and keep reused files from being overwritten by a generator on the next run.
6. Run the feature's relevant asset checks and inspect the imported result in-game when available.

## Included preview sheets

The six sheets below were also attached by the owner. They are preserved unchanged as high-resolution reference previews; their pixels, aspect ratios and filenames in the source collection are not modified.

| Preview | Sheet |
|---|---|
| Dark stone with teal accents | <a href="previews/dark-teal-stone.png"><img src="previews/dark-teal-stone.png" width="300" alt="Dark stone with teal accents"></a> |
| Grey stone and moss variants | <a href="previews/grey-stone-variants.png"><img src="previews/grey-stone-variants.png" width="300" alt="Grey stone and moss variants"></a> |
| Brown stone and moss variants | <a href="previews/brown-stone-variants.png"><img src="previews/brown-stone-variants.png" width="300" alt="Brown stone and moss variants"></a> |
| Pale stone and moss variants | <a href="previews/pale-stone-variants.png"><img src="previews/pale-stone-variants.png" width="300" alt="Pale stone and moss variants"></a> |
| Stone and decorative block overview | <a href="previews/stone-and-decorative-block-overview.png"><img src="previews/stone-and-decorative-block-overview.png" width="300" alt="Stone and decorative block overview"></a> |
| Mossy stone tiles | <a href="previews/mossy-stone-tiles.png"><img src="previews/mossy-stone-tiles.png" width="300" alt="Mossy stone tiles"></a> |

## Import verification

16,449 source files (25,339,959 bytes) were copied and matched against SHA-256 checksums. The six attached previews were also copied byte-for-byte. Images were opened and verified; JSON/animation metadata was parsed; Ogg container signatures and texture-sidecar pairings were checked. See [summary.json](catalog/summary.json) for any supplied-file issues. This is an asset library, not an in-game integration test.

### Gun models (7 October 2026)

On 7 October 2026 the owner uploaded a further archive of gun models with the message: "I have rights for all these". Earlier the same day they said of the Guns folder: "the files in the blocks folder are all mine I made all of them myself and have all the rights to them they are inspired by scorched guns 2 but I made all of them including the animations".

Its `models` folder is kept at [originals/Blocks/Guns/models](originals/Blocks/Guns/models) and listed in the [Guns catalog](catalog/guns/README.md) and the [CSV](catalog/files.csv). It holds Blockbench Java item models of the guns: each gun split per part under `special/<gun>/` (main body, bolt, barrels, magazines, stocks, grips, sights, silencers, bayonets), per-gun item models with display transforms (beside the other item models) under `item/`, and block models under `block/`. All 2,589 files (8,511,427 bytes: 2,579 model JSON files, 8 `.scmeta` attachment metadata files and 2 `.png.mcmeta` texture metadata files) were copied byte-for-byte and matched against SHA-256 checksums, and every one parsed as JSON. The two `.png.mcmeta` files arrived without their PNGs; [summary.json](catalog/summary.json) records them. The upload's two empty `test` folders are not kept, because Git does not store empty folders.

The JSON keeps the original `scguns:` resource namespace in its references, as supplied labels. A feature that imports a model must rewrite those references to `jugcraft:` and rename it to Jugcraft's own name, following the [fan-homage rules](../../LICENSE_POLICY.md#fan-homages). The 45 item models that name the Forge-only `forge:separate_transforms` loader also need that loader replaced for Fabric.

### Magic resources (8 October 2026)

The owner supplied **MAGIC ITEMS BLOCKS TEXTURES SOUNDS MODELS I MADE.rar** and explicitly instructed contributors to use its files freely without needing to change them. The original archive and all 7,909 extracted files are preserved with SHA-256 checksums. The [magic guide](MAGIC_ASSETS.md) documents direct use, necessary technical integration steps, and supplied format/companion-file issues. No runtime content is registered by this import.

The combined searchable library now contains **26,965 source files**, excluding the six preview sheets and the preserved archive. The [global CSV](catalog/files.csv) and [summary](catalog/summary.json) include every upload.

### Effect textures (8 October 2026)

On 8 October 2026 the owner uploaded two archives with the message "heres reticles and vignette":
- **`effect`:** 18 PNG textures, kept at [originals/Blocks/Guns/effect](originals/Blocks/Guns/effect) and listed in the [Guns catalog](catalog/guns/README.md) and the [CSV](catalog/files.csv).
  - The reticles and lens vignettes the scope models name: `effect/red_dot_reticle`, `long_scope_reticle2`, `scope_vignette` and `scope_vignette_circle`.
  - A second long-scope reticle (`long_scope_reticle`) and 13 muzzle flash frames.
  - All 18 files (19,191 bytes) were copied byte-for-byte and matched against SHA-256 checksums.
- **`crosshair`:** the 20 crosshair textures, byte-identical to the [crosshair](originals/Blocks/Guns/crosshair) folder of the 6 October upload. Nothing changed.

Two of the effect files carry embedded Photoshop metadata (XMP), which [summary.json](catalog/summary.json) records:
- `red_dot_reticle.png`: its editing history shows it saved inside a Just Enough Guns mod source folder (`assets/jeg/textures/effect/`) on 3 February 2024.
- `muzzle_flash.png`: created in Photoshop CC 2019 on 12 February 2021.

Shown this and asked what the scopes should use, the owner answered that they made these files and to use them. Asked the same day about the "Big Cannons and Mounted Guns" folder, whose muzzle flash frames the guns use, they confirmed it is theirs. (It holds a file named `wariumlogo.png`; Warium is also the name of another weapons mod.)
