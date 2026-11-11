"""Original sealed one-block Cargo Packager art, OpenAI Codex (GPT-6), MIT.

Run with --write --check --preview. This targeted writer never changes Java,
inventory/recipes, blockstates, item definitions, drones or existing textures.
Native full-cube collision and the six-sided six-pixel item interface remain.
"""
from pathlib import Path
import argparse
import hashlib
import json
import math

from PIL import Image, ImageDraw, ImageFont
import model_writer as mw

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "cargo_packager_art_"
COLOURS = {
    "housing": (58, 60, 61, 255), "frame": (27, 29, 30, 255),
    "steel": (134, 138, 139, 255), "rubber": (17, 19, 20, 255),
    "collar": (102, 103, 99, 255), "glass": (11, 15, 17, 255),
    "recess": (34, 37, 38, 255), "paper": (208, 212, 213, 255),
}


def font(size, bold=False):
    for name in ("bahnschrift.ttf", "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"):
        for base in (Path("C:/Windows/Fonts"), Path("/usr/share/fonts/truetype/dejavu")):
            source = base / name
            if source.is_file():
                return ImageFont.truetype(str(source), size)
    raise FileNotFoundError("Bahnschrift or DejaVu Sans is required for authored packager labels")


def label(text, small=None):
    im = Image.new("RGBA", (512, 96 if small else 64), COLOURS["frame"])
    d = ImageDraw.Draw(im)
    d.line((14, im.height-7, 498, im.height-7), fill=(83, 87, 89, 255), width=2)
    size = 34 if small else 31
    while d.textlength(text, font=font(size, True)) > 470:
        size -= 1
    d.text((256, 29 if small else 28), text, font=font(size, True),
           anchor="mm", fill=COLOURS["paper"])
    if small:
        d.text((256, 66), small, font=font(20), anchor="mm", fill=(149, 154, 156, 255))
    return im


def textures():
    result = {k: Image.new("RGBA", (256, 256), v) for k, v in COLOURS.items() if k != "paper"}
    # Restrained rolled-sheet sheen; visible hardware and seams are geometry.
    # Deterministic wide bands avoid pixel noise, bevel-painted cubes and rust.
    for name in ("housing", "steel", "collar"):
        image = result[name]
        d = ImageDraw.Draw(image)
        base = COLOURS[name]
        for y in range(256):
            step = round(1.7 * math.sin(y*.049))
            d.line((0, y, 255, y), fill=tuple(c+step for c in base[:3])+(255,))
    result["identity"] = label("CARGO PACKAGER", "SEALED LOGISTICS BUFFER")
    result["service"] = label("SERVICE COVER", "ISOLATE BEFORE OPENING")
    result["items"] = label("ITEM NETWORK")
    result["top"] = label("TOP ACCESS / SEALED BUFFER")
    # The unused sealed union has its own dark retained cap, distinct from the
    # lighter stepped metal flange and beaded rubber seal surrounding it.
    # All surrounding flange/fastener relief is geometry; these are engraved
    # face markings, not invented live measurements or a simulated screen.
    cap = Image.new("RGBA", (256,256), COLOURS["recess"])
    d = ImageDraw.Draw(cap)
    d.rectangle((8,8,247,247), outline=(103,107,109,255), width=6)
    d.rectangle((20,20,235,235), outline=(19,22,23,255), width=6)
    for x in (38,218):
        for y in (38,218):
            d.ellipse((x-10,y-10,x+10,y+10),fill=(108,112,113,255))
            d.line((x-5,y,x+5,y),fill=COLOURS["frame"],width=3)
    d.text((128,112),"ITEMS",font=font(29,True),anchor="mm",fill=(183,188,190,255))
    d.text((128,149),"SEALED UNION",font=font(16),anchor="mm",fill=(124,130,132,255))
    result["union"] = cap
    return {PREFIX+k: image for k, image in result.items()}


def geometry():
    out = []

    def box(a, b, material="housing", face=None, artwork=None):
        tex = PREFIX+material
        if artwork:
            tex = {"*": tex, face or "north": PREFIX+artwork+"!"}
        out.append((a, b, tex))

    def oriented(a, b, material, direction, artwork=None):
        # Canonical face is NORTH. The empty blockstate deliberately has no
        # rotation: every attachable face gets its own functional pipe union.
        def turn(p):
            x, y, z = p
            return {"north": (x, y, z), "east": (16-z, y, x),
                    "south": (16-x, y, 16-z), "west": (z, y, 16-x),
                    "up": (x, 16-z, 16-y), "down": (x, z, y)}[direction]
        p, q = turn(a), turn(b)
        lo, hi = tuple(min(p[k], q[k]) for k in range(3)), tuple(max(p[k], q[k]) for k in range(3))
        box(lo, hi, material, direction, artwork)

    def detail(a, b, material, side, word=None):
        oriented(a, b, material, side, word)

    def bolt(u, v, depth, side, radius=.19):
        detail((u-radius, v-radius, depth), (u+radius, v+radius, depth+.25), "steel", side)
        # Captive hex-head centre, deliberately recessed rather than a painted dot.
        detail((u-.075, v-.055, depth-.12), (u+.075, v+.055, depth+.03), "frame", side)

    # The sealed inner casing closes every cover, socket and louver from inside.
    # Full-span foundation/roof and folded corners agree with full-cube collision.
    box((.8, 1, .8), (15.2, 15, 15.2), "frame")
    box((.2, .35, .2), (15.8, 1.1, 15.8), "frame")
    box((.2, 14.85, .2), (15.8, 15.65, 15.8), "housing")
    for x in (.2, 14.7):
        for z in (.2, 14.7):
            box((x, 1.2, z), (x+1.1, 14.65, z+1.1), "frame")

    for side in ("north", "east", "south", "west"):
        # Armored removable cover sits on a continuous compressed dark gasket.
        detail((1.45, 1.5, .54), (14.55, 14.65, 1.12), "rubber", side)
        detail((1.8, 1.85, .3), (14.2, 14.3, .96), "housing", side)
        for u in (2.3, 13.7):
            for v in (2.35, 13.8):
                bolt(u, v, .2, side)
        # External hinges and their captured steel pins, at real cover margins.
        for v in (3.3, 10.6):
            detail((1.18, v, .14), (2.05, v+1.8, .84), "frame", side)
            detail((1.43, v+.12, .02), (1.79, v+1.68, .42), "steel", side)
        # Quarter-turn latch and recessed closed pull, clear of the centre union.
        detail((12.5, 5.5, .16), (13.75, 8.7, .9), "frame", side)
        detail((12.81, 5.8, .02), (13.15, 8.4, .54), "rubber", side)
        detail((13.32, 7.08, .03), (13.65, 7.72, .44), "steel", side)
        # Recessed vent/filter bank is lined behind; no see-through slotted hull.
        detail((2.7, 2.65, .16), (12.3, 3.85, .72), "recess", side)
        for u in range(3, 12):
            detail((u+.02, 2.77, .02), (u+.44, 3.71, .48), "frame", side)
        word = "identity" if side == "north" else "service"
        detail((3, 12.65, .05), (12.1, 14.35, .51), "frame", side, word)

    # Identical six-pixel pipe necks reach the exact block boundary on ALL six
    # attachable faces. The capped neck has a lined stepped gland/bolted flange,
    # attached directly to the sealed cover (no floating plate or inset gap).
    for side in mw.FACES:
        detail((3.65, 4.0, .58), (12.35, 12.0, 1.52), "frame", side)
        detail((4.05, 4.35, .4), (11.95, 11.65, 1.38), "collar", side)
        detail((4.55, 4.55, .22), (11.45, 11.45, 1.65), "rubber", side)
        detail((5, 5, 0), (11, 11, 1.9), "collar", side, "union")
        for u in (4.2, 11.8):
            for v in (4.5, 11.5):
                bolt(u, v, .2, side, .17)
        # Port identification only; no invented throughput or running state.
        detail((5.0, 11.8, .1), (11.0, 12.42, .5), "frame", side, "items")

    # Narrow sealed inspection recess and selector bank beside the union.
    # All dark caps are passive hardware, not fabricated live running lamps.
    for a, b in (((2.45, 5.9, .05), (2.65, 10.4, .56)),
                 ((3.3, 5.9, .05), (3.5, 10.4, .56)),
                 ((2.65, 5.9, .05), (3.3, 6.12, .56)),
                 ((2.65, 10.18, .05), (3.3, 10.4, .56))):
        detail(a, b, "frame", "north")
    detail((2.68, 6.17, .18), (3.25, 10.13, .6), "glass", "north")
    for v in (9.25, 10.15, 11.05):
        detail((12.75, v, .05), (13.45, v+.55, .5), "steel", "north")

    # Top hatch segmented around its central item union; low folded perimeter
    # and captive fasteners stay strictly inside the existing one-block envelope.
    for z0, z1 in ((1.55, 3.45), (12.55, 14.45)):
        box((1.5, 15.3, z0), (14.5, 15.7, z1), "rubber")
        box((1.75, 15.65, z0+.22), (14.25, 15.88, z1-.22), "housing")
        for x in (2.3, 13.7):
            box((x-.16, 15.82, z0+.7), (x+.16, 15.99, z0+1.03), "steel")
    detail((3, 13, .11), (13, 14.25, .47), "frame", "up", "top")
    return out


def native_model():
    raw = geometry()
    elements = [mw.element(a, b, tex) for a, b, tex in raw]
    mw.finish_closed(elements)
    # Faces at exact block boundaries can safely cull against solid neighbours.
    for e in elements:
        for face, spec in e["faces"].items():
            axis, sign = mw._FACE_DIR[face]
            plane = e["from" if sign < 0 else "to"][axis]
            if abs(plane-(0 if sign < 0 else 16)) < 1e-8:
                spec["cullface"] = face
    lookup = {name: "jugcraft:block/"+name for name in mw.texture_names(raw)}
    lookup["particle"] = "jugcraft:block/"+PREFIX+"housing"
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": lookup, "elements": elements}


def write_model(root=ROOT):
    # Finish has already run: write exact native roots without a second mutation.
    path = Path(root)/"src/main/resources/assets/jugcraft/models/block/cargo_packager.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(native_model(), indent=2)+"\n", encoding="utf-8")
    return path


def write_textures(root=ROOT):
    root = Path(root)
    sources = root/"art/cargo-packager/originals"
    runtime = root/"src/main/resources/assets/jugcraft/textures/block"
    sources.mkdir(parents=True, exist_ok=True)
    runtime.mkdir(parents=True, exist_ok=True)
    entries = []
    for name, image in textures().items():
        source = sources/(name+".png")
        target = runtime/source.name
        image.save(source)
        target.write_bytes(source.read_bytes())
        entries.append({"source": source.relative_to(root).as_posix(),
                        "runtime": target.relative_to(root).as_posix(), "size": list(image.size),
                        "sha256": hashlib.sha256(source.read_bytes()).hexdigest()})
    data = {"author": "OpenAI Codex (GPT-6)", "license": "MIT", "date": "2026-10-10",
            "source": "Original code-authored cabinet geometry, neutral material finishes and labels; no copied pixels or model.",
            "references": ["docs/ART_DIRECTION.md native closed-model and readback rules",
                           "Owner-approved black/charcoal/neutral-gray tower industrial finish",
                           "Owner library factory/compressing/compressing_factory_front.png reviewed for cabinet readability; not imported"],
            "bounds_pixels": [0, 0, 0, 16, 16, 16], "blockstate": "unchanged empty variant",
            "item_ports": {"sides": list(mw.FACES), "center": [8, 8], "cross_section": [5, 5, 11, 11], "boundary": "0/16 exact"},
            "labels": "Static service/identity markings only; no fabricated measurements, counts or status.", "textures": entries}
    (root/"art/cargo-packager/provenance.json").write_text(json.dumps(data, indent=2)+"\n", encoding="utf-8")


def write_all(root=ROOT):
    write_textures(root)
    return write_model(root)


def check(root=ROOT):
    import art_check
    assets = Path(root)/"src/main/resources/assets/jugcraft"
    path = assets/"models/block/cargo_packager.json"
    model = json.loads(path.read_text(encoding="utf-8"))
    elements, lookup, _ = art_check._resolve(model, (assets,))
    faces, uvs = art_check._model_faces(elements, lookup)
    assert all(all(e["from"][k] < e["to"][k] for k in range(3)) for e in elements), "Non-volume/reversed element"
    assert all(min(e["from"]) >= -1e-8 and max(e["to"]) <= 16+1e-8 for e in elements), "Escaped single-block bounds"
    assert all(min(uv) >= -1e-8 and max(uv) <= 16+1e-8 for uv in uvs), "UV outside sprite"
    assert not art_check._open_faces(elements, faces), "Open cabinet geometry"
    problems = []
    for i, j, gap, polygon, u, v, normal, plane in art_check._pairs(faces, art_check.N1_GAP):
        if not (gap == 0 and art_check._layered(elements, faces[i], faces[j])) and art_check._differ(faces[i], faces[j], polygon, u, v, normal, plane):
            problems.append((i, j, gap))
    assert not problems, f"Differently drawn near/coplanar faces: {problems[:4]}"
    for name, image in textures().items():
        source = Path(root)/"art/cargo-packager/originals"/(name+".png")
        target = assets/"textures/block"/source.name
        assert source.read_bytes() == target.read_bytes(), name
        assert Image.open(target).getextrema()[3] == (255, 255), "Nonopaque machine texture"
    # Native exported neck geometry, not just an authored/metadata assertion.
    necks = 0
    for side, (axis, sign) in mw._FACE_DIR.items():
        cross = [k for k in range(3) if k != axis]
        plane = 0 if sign < 0 else 16
        match = [e for e in elements if abs(e["from" if sign < 0 else "to"][axis]-plane)<1e-8
                 and all(e["from"][k]>=5-1e-8 and e["to"][k]<=11+1e-8 for k in cross)]
        # Probe the six-pixel mating perimeter in the exported shape.
        for u, v in ((5.2,8), (10.8,8), (8,5.2), (8,10.8)):
            assert any(e["from"][cross[0]]<=u<=e["to"][cross[0]]
                       and e["from"][cross[1]]<=v<=e["to"][cross[1]] for e in match), f"Missing six-pixel sleeve on {side}"
        necks += 1
    stats = {"model_elements": len(elements), "exported_faces": len(faces), "original_textures": len(textures()),
             "pipe_interfaces": necks, "bounds_pixels": [0,0,0,16,16,16], "bounds_uv_closed_coplanar": "pass"}
    print(json.dumps(stats))
    return stats


def preview(root=ROOT):
    import numpy as np
    import art_check
    import armor_preview as raster
    root = Path(root)
    assets = root/"src/main/resources/assets/jugcraft"
    model = json.loads((assets/"models/block/cargo_packager.json").read_text(encoding="utf-8"))
    elements, lookup, _ = art_check._resolve(model, (assets,))
    faces, _ = art_check._model_faces(elements, lookup)
    images = {f.tex: np.array(Image.open(f.tex).convert("RGBA")) for f in faces}
    panels = []
    for title, view in (("FRONT / SEALED COVER / PIPE UNION", (32, 24)),
                        ("REAR / LATCHES / LINED VENTS", (145, 18)),
                        ("TOP ACCESS / SIX-SIDED CONNECTIONS", (38, 62))):
        camera = raster.camera(view)[0]
        quads = [(np.array(f.pts), np.array(f.uvs), np.array(f.normal), images[f.tex])
                 for f in faces if np.dot(f.normal, camera) > 0]
        im = raster.render(quads, view, scale=35, background=(106, 111, 115), lighting="blockbench")
        panel = Image.new("RGB", (980, 900), (106, 111, 115))
        panel.paste(im, ((980-im.width)//2, 48+(780-im.height)//2))
        d = ImageDraw.Draw(panel)
        d.text((22, 20), title, font=font(22, True), fill=(232, 235, 236))
        d.text((22, 855), "Actual exported native JSON + PNG | offline view, not Minecraft", font=font(19), fill=(222, 225, 227))
        panels.append(panel)
    out = root/"build/drone_tower/cargo_packager_native.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    raster.sheet(panels, 3).save(out)
    print(out)
    return out


def reproduce(root=ROOT):
    """Narrow export to workspace temporary output; compare all runtime/source bytes."""
    import tempfile
    root = Path(root)
    output = root/"build/drone_tower"
    output.mkdir(parents=True, exist_ok=True)
    compared = 0
    with tempfile.TemporaryDirectory(prefix="packager-reproduce-", dir=output) as tmp:
        target = Path(tmp)
        write_all(target)
        for path in sorted(p for p in target.rglob("*") if p.is_file()):
            relative = path.relative_to(target)
            assert path.read_bytes() == (root/relative).read_bytes(), f"Not reproducible: {relative}"
            compared += 1
    print(f"Packager byte-for-byte reproduction passes: {compared} generated files.")
    return compared


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--preview", action="store_true")
    parser.add_argument("--reproduce", action="store_true")
    args = parser.parse_args()
    if args.write: write_all()
    if args.check: check()
    if args.preview: preview()
    if args.reproduce: reproduce()
    if not (args.write or args.check or args.preview or args.reproduce): parser.print_help()
