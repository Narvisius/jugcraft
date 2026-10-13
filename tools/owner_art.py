"""Imports the owner's own textures from the shared library (art/owner-library/originals/Blocks, docs: art/owner-library/
README.md) into the mod's resources, and checks the imported files still match their sources. Since Ember part 2 it also
imports files from the owner's magic collection (art/owner-library/originals/Magic, art/owner-library/MAGIC_ASSETS.md):
textures, a GeckoLib model and sounds, each a byte-for-byte copy under a Jugcraft name (a model's JSON with its line ends
made LF), listed in tools/concordance_ember.py OWNER_FILES. The trinkets slice (tools/concordance_trinkets.py) adds its
icons, their animation sidecars, two slot icons and (part 1b) the Leather Belt's and Amphibian Boot's worn sheets the
same way: OWNER_FILES from the magic collection and OWNER_BLOCKS_FILES from the Blocks folder's Trinket Type Mod.

On 7 October 2026 the owner asked for their farming and food textures to be used ("I have already made a ton of custom
textures and food ... I made all of the textures in there myself its all mine"). Each imported texture is a byte-for-byte
copy of its library file, with its .png.mcmeta animation sidecar when it has one (its line ends made LF, as Git stores
the mod's text files). The only changes are the recolourings listed in a feature's RECOLOURED table (the bronze and
steel knives: the owner's iron knife with its blade's tones swapped for the approved bronze and steel ramps) and the
icons listed in a feature's COMPOSED table (a serving the owner drew no icon for: their bowl with a window of their
whole-dish icon heaped in it), and ornamental corn's ripe stage (tools/garden.py ORNAMENTAL_EARS: their ripe corn with its
ears in flint corn's colours). No generator draws over these files: tools/generate_textures.py runs this
import last, and tools/check_mod_data.py fails if a runtime copy differs from what this import would write.

    python3 tools/owner_art.py                write every import (generate_textures.py also does)
    python3 tools/owner_art.py --check        report differences only
    python3 tools/owner_art.py --provenance   the magic imports' sources and SHA-256, for the feature record
    python3 tools/owner_art.py --provenance trinkets   the same for one slice's files (a tools/concordance_<slice>.py)
"""
import hashlib
import io
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

import arms_pixel  # noqa: E402
import concordance_ember  # noqa: E402
import concordance_trinkets  # noqa: E402
import feasts  # noqa: E402
import garden  # noqa: E402
import kitchen  # noqa: E402
import menu  # noqa: E402
import rice  # noqa: E402
import soil  # noqa: E402

LIBRARY = os.path.join(ROOT, "art", "owner-library", "originals", "Blocks")
FOOD = "farming and food textures"
TEXTURES = os.path.join(ROOT, "src", "main", "resources", "assets", "jugcraft", "textures")
MAGIC = os.path.join(ROOT, "art", "owner-library", "originals", "Magic")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "jugcraft")


def imports():
    """(runtime path under textures/ without .png, library path under Blocks/ without .png) for every copied texture."""
    out = []
    out.append(("block/transport_crate_wood", "biomes and tree blocks/origin_oak_planks"))
    for table in (kitchen.TEXTURES, feasts.TEXTURES, menu.TEXTURES, rice.TEXTURES, soil.TEXTURES, garden.TEXTURES):
        for target, source in table.items():
            out.append((target, f"{FOOD}/{source}"))
    return out


def magic_imports():
    """(runtime path under assets/jugcraft, library path under Magic/) for every copied magic-collection file."""
    return sorted({**concordance_ember.OWNER_FILES, **concordance_trinkets.OWNER_FILES}.items())


def block_asset_imports():
    """(runtime path under assets/jugcraft, library path under Blocks/) for every Blocks-folder file copied to a path other
    than textures/ (the trinkets slice's slot icon)."""
    return sorted(concordance_trinkets.OWNER_BLOCKS_FILES.items())


def recolourings():
    """(runtime path, library path, ramp) for every recoloured texture."""
    return [(target, f"{FOOD}/{source}", getattr(arms_pixel, ramp)) for target, (source, ramp) in kitchen.RECOLOURED.items()]


def compositions():
    """(runtime path, spec) for every composed icon (tools/feasts.py COMPOSED)."""
    return list(feasts.COMPOSED.items())


def compose(spec):
    """A serving's icon: the window `crop` of the owner's dish icon set at `at`, behind the front of their bowl (the
    rows of their bowl icon from `bowl_from` down)."""
    bowl = Image.open(_source(f"{FOOD}/{spec['bowl']}")).convert("RGBA")
    out = Image.new("RGBA", bowl.size)
    out.alpha_composite(Image.open(_source(f"{FOOD}/{spec['dish']}")).convert("RGBA").crop(tuple(spec["crop"])), tuple(spec["at"]))
    front = Image.new("RGBA", bowl.size)
    front.paste(bowl.crop((0, spec["bowl_from"], bowl.width, bowl.height)), (0, spec["bowl_from"]))
    out.alpha_composite(front)
    return out


def _source(name):
    return os.path.join(LIBRARY, name + ".png")


def _target(name):
    return os.path.join(TEXTURES, name + ".png")


def recolour(image, ramp):
    """The image with its grey (blade) tones mapped, darkest first, onto `ramp` (an arms_pixel.Material: two outline
    tones, then dark, mid, light and highlight); the coloured (handle) pixels stay as drawn."""
    img = image.convert("RGBA")
    greys = sorted({p[:3] for p in img.get_flattened_data() if p[3] and p[0] == p[1] == p[2]}, key=sum)
    tones = [ramp.outline_dark, ramp.outline_light, ramp.dark, ramp.mid, ramp.light, ramp.highlight]
    # The two darkest greys are the outline's two tones; the rest climb the ramp from its dark tone up.
    mapping = {}
    for index, grey in enumerate(greys):
        tone = tones[0] if index == 0 else tones[1] if index <= 2 else tones[min(2 + index - 3, len(tones) - 1)]
        mapping[grey] = tuple(tone)
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if p[3] and p[:3] in mapping:
                out.putpixel((x, y), mapping[p[:3]] + (p[3],))
    return out


def _png_bytes(image):
    buffer = io.BytesIO()
    image.save(buffer, "PNG", optimize=True)
    return buffer.getvalue()


def expected():
    """{absolute runtime path: bytes} for every file the import writes."""
    files = {}
    for target, source in imports():
        with open(_source(source), "rb") as handle:
            files[_target(target)] = handle.read()
        meta = _source(source) + ".mcmeta"
        if os.path.isfile(meta):
            # A sidecar is text: the library keeps the owner's CRLF line ends, and Git stores resource text with LF
            # (.gitattributes), so the copy is written with LF to match what is committed.
            with open(meta, "rb") as handle:
                files[_target(target) + ".mcmeta"] = handle.read().replace(b"\r\n", b"\n")
    for target, source, ramp in recolourings():
        files[_target(target)] = _png_bytes(recolour(Image.open(_source(source)), ramp))
    for target, spec in compositions():
        files[_target(target)] = _png_bytes(compose(spec))
    for target, source in garden.ORNAMENTAL_EARS.items():
        # Ornamental corn's ripe stage: the owner's ripe corn, its ears in flint corn's colours (tools/garden.py).
        files[_target(target)] = _png_bytes(garden.flint_ears(Image.open(_source(f"{FOOD}/{source}"))))
    for root, table in ((MAGIC, magic_imports()), (LIBRARY, block_asset_imports())):
        for target, source in table:
            with open(os.path.join(root, source), "rb") as handle:
                data = handle.read()
            # Text (a model's JSON, an animation sidecar) is stored with LF like the mod's other resources; images and
            # sounds are copied as they are.
            files[os.path.join(ASSETS, target)] = data.replace(b"\r\n", b"\n") if target.endswith((".json", ".mcmeta")) else data
    return files


def provenance():
    """(runtime path, library path, SHA-256 of the library file) for every magic-collection import and every Blocks-folder
    file copied outside textures/."""
    out = []
    for root, name, table in ((MAGIC, "Magic", magic_imports()), (LIBRARY, "Blocks", block_asset_imports())):
        for target, source in table:
            with open(os.path.join(root, source), "rb") as handle:
                out.append((target, f"originals/{name}/{source}", hashlib.sha256(handle.read()).hexdigest()))
    return out


def write_all():
    for path, data in expected().items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as handle:
            handle.write(data)


def errors():
    """Every imported file that is missing or differs from its source (or its recolouring)."""
    out = []
    adapted = {_target(target) for target, *_ in recolourings()}
    adapted.update(_target(target) for target, _ in compositions())
    adapted.update(_target(target) for target in garden.ORNAMENTAL_EARS)
    for path, data in expected().items():
        rel = os.path.relpath(path, ROOT)
        if not os.path.isfile(path):
            out.append(f"{rel} is missing: run tools/owner_art.py (the owner's library import)")
            continue
        with open(path, "rb") as handle:
            actual = handle.read()
            same = actual == data
            if not same and path in adapted:
                # Derived PNG compression differs across Pillow/zlib platforms. Keep
                # every pixel exact; direct imports still require byte-for-byte identity.
                try:
                    original = Image.open(io.BytesIO(data)).convert("RGBA")
                    current = Image.open(io.BytesIO(actual)).convert("RGBA")
                    same = original.size == current.size and original.tobytes() == current.tobytes()
                except (OSError, ValueError):
                    same = False
            if not same:
                out.append(f"{rel} differs from the owner's library file it is imported from; the owner's art is kept as "
                           "drawn (tools/owner_art.py)")
    return out


if __name__ == "__main__":
    if "--provenance" in sys.argv:
        rest = sys.argv[sys.argv.index("--provenance") + 1:]
        only = None
        if rest:
            module = __import__(f"concordance_{rest[0]}")
            only = set(getattr(module, "OWNER_FILES", {})) | set(getattr(module, "OWNER_BLOCKS_FILES", {}))
        for target, source, digest in provenance():
            if only is None or target in only:
                print(f"| `{target}` | `{source}` | `{digest}` |")
        sys.exit(0)
    if "--check" in sys.argv:
        problems = errors()
        print("\n".join(problems) or "Every imported owner file matches its source.")
        sys.exit(1 if problems else 0)
    write_all()
    print(f"Wrote {len(expected())} files from the owner's library.")
