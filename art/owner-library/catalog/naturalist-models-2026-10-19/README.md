# Recovered owner wildlife models — October 19

The owner supplied `Downloads/naturalist` after reporting unacceptable reconstructed animal anatomy and texture mapping. Its `client` subtree contains the missing compiled model/animation definitions. The preserved input is [Naturalist-Models-2026-10-19.zip](../../originals/Naturalist-Models-2026-10-19.zip), with 228 client files and hashes in [source-manifest.json](source-manifest.json). Original package names are retained as provenance. The owner declaration in [PROVENANCE.md](../../PROVENANCE.md) applies.

No supplied class, mod or program was executed. CFR 0.152 decoded the class files as data; the tool's MD5 `8a85ada8cec494121246805a5562b82b` was checked against [the publisher](https://www.benf.org/other/cfr/). `tools/recover_wildlife_models.py` accepts only the declarative model/animation builder grammar and numeric literals. Unsupported statements fail extraction. It recovered 73 geometry layers and 441 keyframe clips; [recovery.json](recovery.json) records the 47 existing Jugcraft species bindings and texture gaps. [animations.json](animations.json) preserves all recovered clips, including behavior not yet bound in Jugcraft.

Runtime geometry lives under `assets/jugcraft/wildlife_models`. The native Minecraft renderer preserves box dimensions, local pivots, rotations, UV offsets, mirrored faces and inflation. It replaces the palette-patch Gecko reconstruction for wildlife only; supplied Gecko rigs in the other collections remain intact. Baby layers are used where matching supplied skins exist. Bass sizes use their separate model layers. Dye images render over a complete base skin, with saved variant indices retained. Incompatible dye overlays are omitted rather than stretched onto a different atlas. Baby-bird textures were not supplied: those birds temporarily use the original adult layer at infant scale, with the unused nestling geometry preserved.

The native renderer binds idle, locomotion, swim, flight and available sitting poses to existing Jugcraft state. It preserves alligator eye masks, jellyfish translucency and the supplied firefly glow frames. Zero-duration looping source clips are treated as static poses to avoid undefined animation transforms. Specialized source behavior (such as snail climbing/hiding, eating-specific held-item poses and synchronized attacks) is not added by this presentation recovery; its source curves remain preserved. Other collections' reconstructed models are not claimed as recovered by this Naturalist-only input.

Implementation: OpenAI Codex (GPT-6). Changes remain local. [Validation](validation.json) records four passing offline tests, the successful 49-view rendered gallery, reviewed samples, artifact hashes and limits. The full local test pack is `../naturalist-model-recovery/Jugcraft-Animal-Fixes-Oct19.mrpack` relative to the development checkout. Its embedded mod matches the tested build; launcher import and owner acceptance remain unperformed.

Reproduce extraction after decoding the preserved class archive into a separate output directory:

```powershell
python tools/recover_wildlife_models.py <decompiled>/com/crispytwig/naturalist/client
python -m unittest discover -s scripts/tests -p test_recovered_wildlife.py -v
./gradlew runClientGameTest -PclientTests=RecoveredWildlifeClientTests -I scripts/owner-network.init.gradle
```
