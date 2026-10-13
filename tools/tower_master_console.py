"""One original, closed native master-console model, sliced into its six real runtime cells.

OpenAI Codex (GPT-6), October 2026. Shared approved tower materials are referenced
without modifying their originals. Run --write --check --preview for this console only.
"""
from pathlib import Path
import argparse
import hashlib
import json

from PIL import Image, ImageDraw, ImageFont
import model_writer as mw

ROOT = Path(__file__).resolve().parents[1]
BLOCK_ID = "tower_master_console"
DISPLAY = "Tower Master Console"
CRAFTING = {BLOCK_ID: (["SDS", "DCD", "SPS"], {
    "S": "#c:plates/steel", "D": "jugcraft:console_desk",
    "C": "jugcraft:advanced_circuit", "P": "jugcraft:processor"}, 1)}
FOOTPRINT = ((0, 0, 0), (-1, 0, 0), (1, 0, 0), (0, 0, 1), (-1, 0, 1), (1, 0, 1))
# North-facing source: operator at -z. Text begins at each panel's upper-right
# corner in world x, so it reads left-to-right when seen from that operator side.
SCREEN_PLANES = {
    "main": {"x": (-1.6, 17.6), "y": (15.2, 24.5), "z": 23.5, "overlay_z": 23.39},
    "power": {"x": (20.2, 30.4), "y": (16.0, 24.2), "z": 23.5, "overlay_z": 23.39},
    "supply": {"x": (-14.5, -4.2), "y": (16.0, 24.2), "z": 23.5, "overlay_z": 23.39},
}
PALETTE = {key: "tower_detail_" + key for key in ("steel", "panel", "dark", "light_steel", "rubber", "copper", "amber", "cyan")}
PALETTE.update({key: "tower_console_" + key for key in ("screen", "legend", "keyboard", "touch", "labels")})


def _font(size):
    path = Path("C:/Windows/Fonts/segoeuib.ttf")
    if not path.exists():
        path = Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")
    return ImageFont.truetype(str(path), size) if path.exists() else ImageFont.load_default()


def textures():
    """Original opaque controls. Live fleet/power numbers are drawn by the client, never painted here."""
    out = {}
    screen = Image.new("RGBA", (128, 128), (10, 21, 25, 255))
    d = ImageDraw.Draw(screen)
    for p in range(12, 128, 16):
        d.line((0, p, 127, p), fill=(12, 27, 30, 255))
        d.line((p, 0, p, 127), fill=(12, 27, 30, 255))
    out["screen"] = screen
    label = Image.new("RGBA", (256, 32), (30, 39, 42, 255))
    d = ImageDraw.Draw(label)
    d.text((128, 14), "TOWER CONTROL", font=_font(23), anchor="mm", fill=(213, 229, 226, 255))
    d.line((8, 29, 247, 29), fill=(198, 140, 65, 255), width=2)
    out["legend"] = label
    keys = Image.new("RGBA", (128, 64), (28, 33, 36, 255)); d = ImageDraw.Draw(keys)
    for y in range(5):
        for x in range(13):
            x0, y0 = 3 + x * 9, 4 + y * 10
            d.rounded_rectangle((x0, y0, x0 + 7, y0 + 7), radius=1, fill=(86, 96, 99, 255))
            d.line((x0 + 1, y0 + 1, x0 + 6, y0 + 1), fill=(137, 148, 150, 255))
    d.rounded_rectangle((37, 55, 91, 61), radius=1, fill=(89, 100, 102, 255))
    out["keyboard"] = keys
    touch = Image.new("RGBA", (128, 128), (11, 29, 33, 255)); d = ImageDraw.Draw(touch)
    d.text((64, 13), "FLIGHT DECK", font=_font(11), anchor="mm", fill=(158, 202, 204, 255))
    d.rectangle((8, 26, 119, 84), outline=(42, 77, 81, 255), width=2)
    for y in (38, 56, 74):
        for x in (27, 51, 75, 99):
            d.rectangle((x - 7, y - 5, x + 7, y + 5), outline=(139, 190, 189, 255), width=2)
    for x, name in ((9, "LINK"), (48, "PADS"), (88, "JOBS")):
        d.rounded_rectangle((x, 96, x + 31, 118), radius=3, fill=(43, 71, 76, 255))
        d.text((x + 15, 107), name, font=_font(8), anchor="mm", fill=(201, 220, 218, 255))
    out["touch"] = touch
    labels = Image.new("RGBA", (128, 64), (42, 51, 54, 255)); d = ImageDraw.Draw(labels)
    for y, text in ((10, "MANUAL / AUTO"), (30, "POWER ISOLATE"), (50, "AUXILIARY BUS")):
        d.text((64, y), text, font=_font(9), anchor="mm", fill=(209, 219, 218, 255))
    out["labels"] = labels
    return out


def geometry():
    """A connected workstation: load-bearing end pedestals, knee bay, equipment spine and monitor bridge."""
    out = []
    def box(a, b, tex, faces=None):
        material = {face: PALETTE.get(value, value) for face, value in faces.items()} if faces else PALETTE[tex]
        out.append((tuple(a), tuple(b), material))
    # Continuous structural spine and open knee space, with a real rear service chase.
    box((-15, 0.3, 22.4), (31, 2.0, 30.6), "steel")
    box((-15, 2.2, 23.5), (31, 10.7, 30.6), "panel")
    box((-15.5, 10.8, 1), (31.5, 12.8, 26), "steel")
    box((-15.2, 12.9, 1.3), (31.2, 13.3, 22.8), "panel")
    # A rounded-looking stepped nose, not six independent block boxes.
    box((-14.8, 8.0, .9), (30.8, 10.7, 2.2), "dark")
    box((-13.8, 9.1, .2), (29.8, 10.5, 1.05), "rubber")
    box((-4.9, 8.2, .05), (20.9, 10.4, .16), "legend", {"*": "dark", "north": "tower_console_legend!"})
    for x in (-14.8, 26.5):
        box((x, .3, 3.1), (x + 4.3, 10.7, 20.5), "panel")
        box((x + .2, .05, 3.3), (x + 4.1, .25, 20.3), "rubber")
        box((x + .4, 1.3, 2.7), (x + 3.9, 8.3, 3.0), "steel")
        box((x + .6, 1.6, 2.35), (x + 3.7, 8, 2.6), "dark")
        for y in (3, 4.2, 5.4, 6.6):
            box((x + .9, y, 2.05), (x + 3.4, y + .35, 2.3), "light_steel")
        box((x + .4, 9.2, .5), (x + 3.9, 10.3, .85), "amber")
    # Linked instrument bridge, three serviceable monitor housings on one frame.
    box((-14.5, 13.5, 25.6), (30.5, 14.4, 28.0), "steel")
    for x in (-12.5, 27):
        box((x, 11, 25.7), (x + 1.5, 23.7, 27.3), "steel")
    for x0, x1, y0, y1 in ((-15.0, -3.6, 14.8, 25), (-2.8, 18.8, 13.8, 25), (19.6, 31, 14.8, 25)):
        box((x0, y0, 23.8), (x1, y1, 26.8), "dark")
        box((x0 + .25, y0 + .25, 26.9), (x1 - .25, y1 - .25, 27.35), "panel")
        # Lateral vents are real gaps between closed fins with a dark backing.
        for x in (x0 + .7, x1 - 1.0):
            for y in (17, 18.5, 20, 21.5):
                box((x, y, 27.45), (x + .3, y + .6, 27.8), "steel")
    for plane in SCREEN_PLANES.values():
        x0, x1 = plane["x"]; y0, y1 = plane["y"]
        box((x0, y0, 23.5), (x1, y1, 23.7), "screen", {"*": "dark", "north": "tower_console_screen!"})
    # Keyboard, a guarded main switch, rotary controls and two touch instruments.
    box((-.6, 13.4, 5.2), (16.6, 13.85, 13.5), "dark")
    box((-.3, 13.95, 5.5), (16.3, 14.15, 13.2), "keyboard", {"*": "dark", "up": "tower_console_keyboard!"})
    box((-.5, 13.4, 14.3), (16.5, 13.8, 20.5), "rubber")
    for x in (-12.9, 21.3):
        box((x, 13.4, 8), (x + 7.6, 13.85, 19.5), "dark")
        box((x + .25, 13.95, 8.25), (x + 7.35, 14.15, 19.25), "touch", {"*": "dark", "up": "tower_console_touch!"})
    for x in (-12.2, -9.6, -7, 22, 24.6, 27.2):
        box((x, 13.4, 3.5), (x + 1.3, 13.7, 5.6), "steel")
        box((x + .25, 13.8, 3.85), (x + 1.05, 14.45, 5.25), "dark")
        box((x + .55, 14.55, 4.05), (x + .75, 14.8, 5.05), "amber")
    # Main isolator is protected by stout side guards and a recessed red button.
    box((18.1, 13.4, 2.1), (20.2, 13.85, 5.6), "dark")
    box((18.4, 13.95, 2.5), (19.9, 14.5, 4.2), "amber")
    for x in (17.8, 20.3):
        box((x, 13.5, 2.0), (x + .35, 15.0, 5.7), "steel")
    # Cable chase and mechanical fasteners only where a real connection occurs.
    for x in (-10, 8, 26):
        box((x, 3.3, 30.7), (x + 1.2, 8, 31.1), "rubber")
        box((x + .15, 3.1, 30.8), (x + 1.05, 3.3, 31.5), "copper")
        box((x + .15, 8.1, 30.8), (x + 1.05, 8.4, 31.5), "steel")
    for x in (-14.2, 30.1):
        for z in (2.4, 21.5):
            box((x, 13.4, z), (x + .35, 13.6, z + .35), "light_steel")
    return out


def write_all(root=ROOT):
    """Writes only this feature's assets/provenance; the main generator owns language and recipe metadata."""
    assets = root / "src/main/resources/assets/jugcraft"
    original = root / "art/tower-master-console/originals"; original.mkdir(parents=True, exist_ok=True)
    for key, image in textures().items():
        image.save(original / f"tower_console_{key}.png")
        target = assets / "textures/block" / f"tower_console_{key}.png"; target.parent.mkdir(parents=True, exist_ok=True); image.save(target)
    elements = geometry(); parts = mw.split_model(BLOCK_ID, elements, FOOTPRINT)
    texture_map = {name: "jugcraft:block/" + name for name in mw.texture_names(elements)}
    texture_map["particle"] = "jugcraft:block/tower_detail_steel"
    variants = {}
    for part, solids in enumerate(parts):
        name = f"{BLOCK_ID}_part{part}"
        mw.write(assets / "models/block" / (name + ".json"), {"ambientocclusion": False, "textures": texture_map, "elements": solids})
        for facing, angle in mw.FACING_Y.items():
            variants[f"facing={facing},part={part}"] = {"model": "jugcraft:block/" + name, "y": angle}
    mw.write(assets / "blockstates" / (BLOCK_ID + ".json"), {"variants": variants})
    mw.write(assets / "models/item" / (BLOCK_ID + ".json"), {
        "parent": "minecraft:block/block", "ambientocclusion": False, "textures": texture_map,
        "elements": mw.scaled_elements(elements), "display": {
            "gui": {"rotation": [25, 135, 0], "translation": [0, 0, 0], "scale": [.7, .7, .7]},
            "ground": {"scale": [.3, .3, .3]}, "fixed": {"scale": [.6, .6, .6]}}})
    mw.write(assets / "items" / (BLOCK_ID + ".json"), {"model": {"type": "minecraft:model", "model": "jugcraft:item/" + BLOCK_ID}})
    mw.write(root / "src/main/resources/data/jugcraft/loot_table/blocks" / (BLOCK_ID + ".json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "jugcraft:" + BLOCK_ID}],
        "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": "jugcraft:blocks/" + BLOCK_ID})
    provenance = {
        "author": "OpenAI Codex (GPT-6)", "license": "MIT", "source": "tools/tower_master_console.py",
        "geometry": "Original six-cell connected workstation; native closed solids, no imported model.",
        "controls": "Original programmatic opaque instrument/keyboard/label textures; original PNGs retained here.",
        "shared_materials": "Existing approved v3 tower_detail_* textures from tools/tower_realism_runtime.py; original imagegen source retained in art/tower-v3-sample.",
        "owner_library_review": "art/owner-library/catalog/files.csv reviewed; no owner-library pixels copied. Existing approved tower steel matches the room.",
        "footprint_north": FOOTPRINT, "screen_planes": SCREEN_PLANES,
        "original_sha256": {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(original.glob("*.png"))}}
    mw.write(root / "art/tower-master-console/provenance.json", provenance)
    print(f"Wrote {BLOCK_ID}: six block parts, {len(elements)} authored closed solids.")


def exported_assembly():
    import art_check
    assets = ROOT / "src/main/resources/assets/jugcraft"
    members = [(assets / "models/block" / f"{BLOCK_ID}_part{part}.json", tuple(v * 16 for v in cell)) for part, cell in enumerate(FOOTPRINT)]
    faces, boxes = art_check._assembly(members, (assets,))
    return members, faces, boxes


def check_assets():
    import art_check
    assets = ROOT / "src/main/resources/assets/jugcraft"
    members, faces, boxes = exported_assembly(); errors = []
    for path, _ in members + [(assets / "models/item" / (BLOCK_ID + ".json"), (0, 0, 0))]:
        model = json.loads(path.read_text(encoding="utf-8")); elements, lookup, _ = art_check._resolve(model, (assets,))
        own, uv = art_check._model_faces(elements, lookup)
        if any(min(v) < -.001 or max(v) > 16.001 for v in uv): errors.append(path.stem + ": UV outside texture")
        if art_check._open_faces(elements, own): errors.append(path.stem + ": exposed missing face")
        if any(any(e["from"][a] >= e["to"][a] for a in range(3)) or min(e["from"]) < -16 or max(e["to"]) > 32 for e in elements):
            errors.append(path.stem + ": illegal native bounds")
        for i, j, gap, polygon, u, v, normal, plane in art_check._pairs(own, art_check.N1_GAP):
            if not (gap == 0 and art_check._layered(elements, own[i], own[j])) and art_check._differ(own[i], own[j], polygon, u, v, normal, plane):
                errors.append(path.stem + ": near-coplanar differing surfaces"); break
    errors.extend(art_check._cross_errors(BLOCK_ID, faces, boxes, set(FOOTPRINT)))
    for name, image in textures().items():
        if any(v & (v - 1) for v in image.size) or image.getextrema()[3] != (255, 255): errors.append(name + ": not opaque power of two")
    if errors: raise ValueError("\n".join(errors))
    print(f"Master console: {len(faces)} runtime faces, six closed parts; bounds, UVs, seams and control textures pass.")


def preview_assets():
    """Truthful exported JSON/PNG views with a z-buffer and back-face culling, not a concept image."""
    import numpy as np
    import armor_preview as raster
    _, faces, _ = exported_assembly(); images = {f.tex: np.array(Image.open(f.tex).convert("RGBA")) for f in faces}
    panels = []
    for name, view in (("Operator side", (35, 20)), ("Rear service spine", (145, 20))):
        camera = raster.camera(view)[0]
        quads = [(np.array(f.pts), np.array(f.uvs), np.array(f.normal), images[f.tex]) for f in faces if np.dot(f.normal, camera) > 0]
        image = raster.render(quads, view, scale=11, background=(34, 39, 44), lighting="blockbench")
        panel = Image.new("RGB", (900, 650), (34, 39, 44)); panel.paste(image, ((900-image.width)//2, 40+(560-image.height)//2))
        d = ImageDraw.Draw(panel); d.text((18, 12), "Tower master console / " + name, font=_font(20), fill=(226, 233, 231))
        d.text((18, 620), "Actual exported static assets. Live text is supplied in-game by the linked terminal.", font=_font(15), fill=(180, 197, 198))
        panels.append(panel)
    target = ROOT / "build/drone_tower/master_console_native.png"; target.parent.mkdir(parents=True, exist_ok=True)
    raster.sheet(panels, 2).save(target); print(target)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for flag in ("write", "check", "preview"): parser.add_argument("--" + flag, action="store_true")
    args = parser.parse_args()
    if args.write: write_all()
    if args.check: check_assets()
    if args.preview: preview_assets()
