# Owner-edited collection — October 18, 2026

The owner supplied `Jimbos Heavily Edited Mods 10-18-2026` and requested integration
into the single Jugcraft mod. The owner describes the files as heavily edited
versions of earlier inspirations, with legacy tags remaining. This records the
owner's declaration; preserved namespace labels are not a reason to reject the
entire collection. Keep all supplied notices and record inspiration credits as
the individual features are integrated.

All 3,502 files (21,881,213 bytes), including the later 501 animal data files, are preserved byte-for-byte under
[the original collection](../../originals/Edited-2026-10-18). The
[SHA-256 inventory](files.json) accounts for every file. No programs were supplied
or executed. Inspection found no Java/Kotlin source or compiled classes.

| Collection | Files | Available inputs | Integration state |
| --- | ---: | --- | --- |
| Inventory Pets variant | 826 | Item/block textures and models, sounds, recipes/advancements, six NBT structures, language text | Preserved; native gameplay implementation remains required |
| Naturalist animal variant | 2,122 | Animal textures/sounds, item/block definitions, language text, recipes, drops, tags and 99 variant records | Preserved; entity geometry and edited gameplay source are absent |
| Mythic Mounts variant | 409 | Fourteen geometry/animation pairs, texture variants, sounds and item models | Preserved; native taming, riding, flight/swimming, ownership, persistence and spawning remain required |
| Additional sounds | 6 | Ghost audio | Preserved; entity behavior and sound-event wiring remain required |
| Structures | 84 | Worldgen structure, structure-set and template-pool JSON | Preserved; 153 referenced building templates and custom processors are absent |
| Additional textures | 55 | Ghost, pumpkin creature, gravestones, spinning wheel, gear and other textures | Preserved; related geometry/code/data still need matching |

The [completeness audit](audit.json) lists every missing template reference and
each mount's available animation clips. Its entity list includes technical
entities such as carried food and dirt trails, not just animal species.

This import is **not gameplay implementation**. Do not enable incomplete structure
pools, install external mods as a shortcut, substitute unrelated animal geometry,
or report texture copying as completed creature behavior. Reuse existing
Jugcraft protection, progression, inventory, animation and ecology systems.

Run `python -X utf8 tools/audit_owner_oct18.py` to verify the preserved collection;
use `--write` to refresh the audit after adding source inputs and updating their
inventory. This intake and audit were performed with OpenAI Codex (GPT-6).


## Current native integration

The table above records the historical intake and missing source inputs. Native gameplay is now implemented locally, using supplied models where present and newly reconstructed shapes/layouts where absent. The combined first/second-batch roster has 265 species. See [the implementation record](../../../../docs/features/owner-menagerie-oct18.md) and [October 19 validation](../edited-2026-10-18-part2/local-validation-oct19.json) for test results, adaptations and remaining art, balance and owner-playtest work. No source executable was run, originals remain preserved and nothing was published.
