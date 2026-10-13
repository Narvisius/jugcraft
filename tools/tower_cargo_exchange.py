"""Original closed logistics housing for the existing 60-part Cargo Exchange.

OpenAI Codex (GPT-6), MIT. Native closed geometry and original opaque artwork.
Only cargo art is written: no product animation, invented throughput, power art,
registrations, inventories, recipes, language, layouts or owner-library edits.
"""
from pathlib import Path
import argparse
import hashlib
import json
import math

from PIL import Image, ImageDraw, ImageFont
import model_writer as mw

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "tower_cargo_"
COLOURS = {
    "housing": (54, 57, 59, 255), "frame": (23, 25, 27, 255),
    "stainless": (151, 154, 155, 255), "brushed": (83, 86, 88, 255),
    "rubber": (20, 22, 23, 255), "brass": (160, 131, 78, 255),
    "yellow": (210, 172, 67, 255), "red": (143, 47, 41, 255),
    "white": (214, 216, 217, 255), "display": (10, 13, 15, 255),
}
INK = (17, 19, 21, 255)
PAPER = (220, 222, 223, 255)

# Major solids in canonical anchor-relative model pixels. The whole housing is
# closed; there is no exposed conveyor, inspection tunnel or second item input.
COLLISION = [
    [0, 0, 0, 80, 4, 64],
    [2, 4, .4, 78, 46.4, 62.6],                    # complete closed-in box
    [31.8, 17.2, 0, 48.2, 33, 2.2],               # integrated bulkhead union
]
LAYOUT = {
    "version": 2, "units": "anchor-relative model pixels", "front": "north/-z",
    "footprint": {"size": [5, 3, 4], "parts": 60, "order": "y,z,x"},
    "enclosure": {"closed": True, "body": [2, 4, 2.2, 78, 45.8, 62], "exposed_transport": False},
    "static_face_z": .4, "overlay_z": .3,
    "socket": {"part": 22, "center": [40, 24, 0], "body": [37, 21, 0, 43, 27, 1.2],
               "interface": "existing six-pixel square item pipe; bidirectional packager access"},
    "display": {"rect": [4.2, 28, 17.8, 35.5], "center_x": 11,
                "lines_y": [33.8, 31.7, 29.6], "font_height": .65,
                "channels": ["stock_items", "occupied_total_slots", "request_status"]},
    "lamps": [{"channel": "linked", "center": [6.2, 25.5, .3], "radius": .4, "color": "green"},
              {"channel": "requests", "center": [10.5, 25.5, .3], "radius": .4, "color": "amber"}],
    "collision_boxes": COLLISION,
}


def font(size, bold=False):
    choices = ("bahnschrift.ttf", "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf")
    for name in choices:
        for base in (Path("C:/Windows/Fonts"), Path("/usr/share/fonts/truetype/dejavu")):
            path = base / name
            if path.is_file(): return ImageFont.truetype(str(path), size)
    raise FileNotFoundError("Bahnschrift or DejaVu Sans required for original cargo markings")


def label(lines, caution=False):
    """Original screen-printed logistics plates, deliberately unlike switchgear."""
    im = Image.new("RGBA", (512, 128), INK)
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 9, 127), fill=COLOURS["yellow"] if caution else (127, 131, 133, 255))
    d.line((20, 114, 494, 114), fill=(80, 83, 85, 255), width=2)
    for i, line in enumerate(lines):
        size = 38 if len(lines) == 1 else 29
        while d.textlength(line, font=font(size, True)) > 458 and size > 10: size -= 1
        d.text((262, 60 + (i - (len(lines)-1)/2)*42), line, font=font(size, True), anchor="mm", fill=PAPER)
    return im


def textures():
    result = {name: Image.new("RGBA", (256, 256), colour) for name, colour in COLOURS.items()}
    # Fine restrained rolled-sheet variation and a brushed stainless finish.
    # Geometry carries the seams, lips, bolts and louvers; no repeated cube frames.
    for name in ("housing", "frame", "stainless", "brass"):
        im = result[name]; d = ImageDraw.Draw(im); base = COLOURS[name]
        for y in range(256):
            value = round(2*math.sin(y*.065) + (1 if name in ("stainless", "brushed") and y%7 == 0 else 0))
            d.line((0, y, 255, y), fill=tuple(max(0, min(255, c+value)) for c in base[:3])+(255,))
    # Original restrained gray mineral finish for the low, continuous foundation.
    mineral = result["brushed"]; pixels = mineral.load(); base = COLOURS["brushed"]
    for y in range(256):
        for x in range(256):
            value = round(2*math.sin(x*.17)*math.sin(y*.13)+math.sin((x+y)*.73))
            pixels[x, y] = tuple(c+value for c in base[:3])+(255,)
    result["hazard"] = Image.new("RGBA", (256, 64), COLOURS["yellow"])
    d = ImageDraw.Draw(result["hazard"])
    for x in range(-64, 300, 64): d.polygon(((x, 64), (x+32, 64), (x+96, 0), (x+64, 0)), fill=INK)
    labels = {
        "header": ["CARGO INTAKE", "SEALED STORE ACCESS"], "pod": ["LOGISTICS", "OPERATOR INTERFACE"],
        "pipe": ["ITEMS", "STORE ACCESS"], "rollers": ["SEALED ITEM INTERFACE"],
        "interlock": ["SEALED ACCESS", "SERVICE COVER"], "drive": ["SEALED SERVICE BAY", "SERVICE ACCESS"],
        "rear": ["CARGO INTAKE", "REMOVABLE SERVICE COVERS"],
        "isolator": ["LOCAL ISOLATOR"], "stop": ["STOP"], "link": ["LINK"], "request": ["REQUEST"],
        "bearing": ["ACCESS COVER GUIDE"], "filter": ["VENT FILTER"], "access": ["SERVICE PANEL"],
        "live": ["BUFFER / REQUEST STATUS"], "pinch": ["SEALED COVER", "ISOLATE BEFORE SERVICE"],
    }
    for name, lines in labels.items(): result[name] = label(lines, name in ("interlock", "pinch", "stop"))
    for name, colour in (("lamp_green", (27, 76, 57, 255)), ("lamp_amber", (101, 76, 28, 255))):
        im = Image.new("RGBA", (128, 128), INK); d = ImageDraw.Draw(im)
        d.ellipse((5, 5, 123, 123), fill=COLOURS["stainless"])
        d.ellipse((15, 15, 113, 113), fill=colour)
        d.arc((24, 24, 104, 104), 190, 245, fill=tuple(min(255, c+18) for c in colour[:3])+(255,), width=3)
        result[name] = im
    return {PREFIX + name: im for name, im in result.items()}


def geometry():
    """One closed armored body; all relief remains attached to its sealed shell."""
    out = []

    def box(a, b, material="frame", rotation=None, face=None, artwork=None):
        assert all(a[i] < b[i] for i in range(3)), (a, b)
        texture = PREFIX + material
        if artwork: texture = {"*": texture, face or "north": PREFIX + artwork + "!"}
        # Whole artwork plates preserve their single continuous face across cells.
        if artwork and rotation is None:
            rotation = ("y", 0, tuple((a[i]+b[i])/2 for i in range(3)))
        out.append((a, b, texture, {"rotation": rotation}) if rotation else (a, b, texture))

    def cylinder(axis, center, start, end, radius, material="stainless"):
        k = "xyz".index(axis); cross = [a for a in range(3) if a != k]
        # Short closed octagonal handles, hinge pins and union rings only.
        for wa, wb, angle in ((radius, radius*math.tan(math.pi/8), 0),
                              (radius, radius*math.tan(math.pi/8), 45),
                              (radius*math.tan(math.pi/8), radius, 0),
                              (radius*math.tan(math.pi/8), radius, 45)):
            c = list(center); c[k] = (start+end)/2
            a = c.copy(); b = c.copy(); a[k] = start; b[k] = end
            for j, width in zip(cross, (wa, wb)): a[j]-=width; b[j]+=width
            box(tuple(a), tuple(b), material, (axis, angle, tuple(c)) if angle else None)

    def plate(rect, word, z=.7, back="frame"):
        x0, y0, x1, y1 = rect
        box((x0, y0, z), (x1, y1, z+.4), back, artwork=word)

    def front_bolt(x, y, z, radius=.28):
        box((x-radius, y-radius, z), (x+radius, y+radius, z+.34), "stainless")
        box((x-radius*.56, y-.035, z-.11), (x+radius*.56, y+.035, z+.02), "frame")

    def front_handle(x, y, z, height=5):
        for yy in (y, y+height-.7):
            box((x-.45, yy, z+.3), (x+.45, yy+.7, z+1.5), "stainless")
        box((x-.3, y, z), (x+.3, y+height, z+.4), "rubber")

    def front_cover(x0, x1, y0=7.2, y1=38.8):
        # The uninterrupted solid body behind each gasket keeps the housing shut.
        box((x0, y0, .95), (x1, y1, 2.6), "rubber")
        box((x0+.45, y0+.45, .65), (x1-.45, y1-.45, 1.8), "housing")
        for x in (x0+1, x1-1):
            for y in (y0+1, y1-1): front_bolt(x, y, .3)

    # Full-area mineral foundation and closed load-bearing black steel casing.
    # No legs, exposed machine bed, conveyor, inspection opening or pass-through.
    box((.05, 0, .05), (79.95, 4, 63.95), "brushed")
    box((2, 4, 2.2), (78, 45.8, 62), "frame")
    box((2, 4.2, .9), (78, 6.5, 2.6), "brushed")
    box((2, 4.2, 61.7), (78, 6.5, 63), "brushed")
    for x in (1.3, 77.3): box((x, 4.2, 2), (x+1.4, 6.5, 62), "brushed")
    # Continuous armor roof, folded drip edge, two bolted removable top hatches.
    box((1.5, 44.8, 1.4), (78.5, 46.4, 62.6), "housing")
    box((2, 43.5, .9), (78, 45.1, 2.4), "frame")
    for x in (8, 42):
        box((x, 46.3, 13), (x+29, 46.75, 52), "rubber")
        box((x+.6, 46.65, 13.6), (x+28.4, 47.15, 51.4), "housing")
        for xx in (x+2, x+27):
            for z in (16, 49): box((xx-.3, 47.1, z-.3), (xx+.3, 47.55, z+.3), "stainless")
        for z in (21, 43):
            box((x+.4, 47, z), (x+1.5, 47.8, z+3), "frame")
            cylinder("z", (x+.9, 47.35, 0), z, z+3, .35)
        for xx in (x+10, x+18):
            box((xx, 47.1, 30), (xx+1, 47.75, 32), "frame")
        box((x+10, 47.5, 30.5), (x+19, 47.95, 31.5), "rubber")
    # Distinct front service banks and a thick integrated pipe bulkhead.
    front_cover(2.8, 20.2)
    front_cover(21.8, 48.6)
    front_cover(50.2, 77.2)
    box((31.8, 17.2, .9), (48.2, 33, 3.1), "frame")
    box((33, 18.4, .65), (47, 30.9, 2.8), "housing")
    # The rear of this collar overlaps solid housing. The six-pixel neck reaches
    # z0 exactly, seating standard pipe and the thicker wall-pipe sleeve inside it.
    cylinder("z", (40, 24, 0), 1.15, 3.1, 5.2, "frame")
    box((36.4, 20.4, .65), (43.6, 27.6, 2.8), "rubber")
    box((37, 21, 0), (43, 27, 2.8), "brass")
    for x in (34, 46):
        for y in (19.5, 29.8): front_bolt(x, y, .25, .38)
    plate((33, 31.3, 47, 33), "pipe", .3)
    plate((31.8, 40.4, 63.6, 43.1), "header", .3)
    # Right-hand gasketed cover with sunk pull, hinges, captive screws and filter.
    box((68.4, 19, .4), (73.8, 28, 1.5), "frame")
    front_handle(71.1, 20.1, .05, 6.3)
    for y in (11, 32):
        box((50.6, y, .3), (52.8, y+4, 1.6), "frame")
        cylinder("y", (51.55, 0, .6), y, y+4, .36)
    box((54.7, 10, .35), (65.9, 17.2, 1.6), "frame")
    for y in (10.8, 12.2, 13.6, 15):
        box((55.4, y, .05), (65.2, y+.55, .9), "housing")
    plate((54.8, 17.8, 65.8, 19.1), "filter", .3)
    plate((55, 34.5, 73.5, 36), "interlock", .3)
    # Operator readout is recessed into the same closed box; its live planes and
    # lenses retain the exact existing telemetry contract, with no baked figures.
    plate((3.4, 36.3, 18.6, 38), "pod", .3)
    box((3.7, 27.5, .65), (18.3, 36, 1.7), "frame")
    box((4.2, 28, .4), (17.8, 35.5, .8), "display")
    plate((4.2, 26.4, 17.8, 27.3), "live", .3)
    for x, artwork in ((6.2, "lamp_green"), (10.5, "lamp_amber")):
        box((x-.6, 24.9, .4), (x+.6, 26.1, .8), "frame", artwork=artwork)
    plate((4.5, 23.5, 7.9, 24.4), "link", .3)
    plate((8.5, 23.5, 12.6, 24.4), "request", .3)
    cylinder("z", (6.8, 20.8, 0), .5, 1.6, 1.45, "yellow")
    cylinder("z", (6.8, 20.8, 0), .05, .7, 1.03, "red")
    plate((4.8, 18.4, 8.8, 19.3), "stop", .3)
    cylinder("z", (14.4, 20.7, 0), .4, 1.6, 1.2, "frame")
    box((14.05, 19.85, .1), (14.75, 21.55, .75), "rubber")
    plate((10.8, 18.4, 18, 19.3), "isolator", .3)
    box((4, 8.4, .4), (18, 17.5, 1.6), "housing")
    front_handle(16.3, 10.5, .05, 5)
    for y in (10, 14): cylinder("y", (4.6, 0, .6), y, y+1.8, .38)
    plate((5.3, 8.7, 14.5, 10), "access", .3)
    # Recessed front covers below and above the bulkhead remain closed/armored.
    for x0, x1 in ((23, 30.3), (32, 47.5)):
        box((x0, 8.3, .4), (x1, 15.8, 1.8), "housing")
        for x in (x0+.7, x1-.7):
            for y in (9, 15.1): front_bolt(x, y, .05, .2)
    for y in (20, 33):
        box((22.1, y, .3), (24.3, y+3.5, 1.6), "frame")
        cylinder("y", (23, 0, .6), y, y+3.5, .36)
    front_handle(28.4, 20, .05, 6)
    plate((25.3, 35.1, 45.7, 37), "rollers", .3)
    # Paired side service covers: heavy attached panels, recessed closed vents,
    # protected pull handles and continuous clipped conduit against their backing.
    for east in (False, True):
        x0, x1 = (77.7, 78.9) if east else (1.1, 2.3)
        outside = 79.05 if east else .95
        for z in (7, 35):
            box((x0, 8, z), (x1, 40.8, z+24), "rubber")
            lo, hi = (78.65, 79.2) if east else (.8, 1.35)
            box((lo, 8.6, z+.6), (hi, 40.2, z+23.4), "housing")
            for yy in (10.2, 38.6):
                for zz in (z+2, z+22):
                    a, b = (79.1, 79.55) if east else (.45, .9)
                    box((a, yy-.3, zz-.3), (b, yy+.3, zz+.3), "stainless")
            # Real hinge knuckles at rear edge of each side cover.
            for yy in (15, 32):
                a, b = (79.15, 79.65) if east else (.35, .85)
                box((a, yy, z+21.5), (b, yy+3.8, z+22.9), "frame")
            a, b = (79.1, 79.6) if east else (.4, .9)
            box((a, 20, z+3), (b, 27, z+4), "rubber")
        # Backed side ventilation bank; no transparent or open grille cavity.
        a, b = (79.1, 79.45) if east else (.55, .9)
        box((a, 11, 10), (b, 19.4, 27), "frame")
        for yy in (11.8, 13.3, 14.8, 16.3, 17.8):
            a, b = (79.35, 79.8) if east else (.2, .65)
            box((a, yy, 10.7), (b, yy+.55, 26.3), "housing")
        # Clipped armored cable against shell (not a suspended decorative rail).
        a, b = (78, 78.9) if east else (1.1, 2)
        box((a, 41.5, 5), (b, 42.4, 60), "rubber")
        for zz in (9, 23, 37, 51, 58):
            a, b = (78.75, 79.3) if east else (.7, 1.25)
            box((a, 41.2, zz), (b, 42.7, zz+1.3), "stainless")
    # Three rear removable covers with no opening into the body.
    for x in (5, 29, 53):
        box((x, 7.5, 61.8), (x+22, 39.8, 62.6), "rubber")
        box((x+.5, 8, 62.5), (x+21.5, 39.3, 63.05), "housing")
        for xx in (x+1.5, x+20.5):
            for y in (9.5, 37.8):
                box((xx-.3, y-.3, 63), (xx+.3, y+.3, 63.5), "stainless")
        for xx in (x+7, x+14):
            box((xx, 22, 63), (xx+.8, 27, 63.65), "rubber")
    box((25, 41, 62.5), (55, 43.6, 63), "frame", face="south", artwork="rear")
    # Small rear filtered bank, seated on solid shell and framed by the cover.
    box((32, 10.5, 63), (48, 18.5, 63.45), "frame")
    for y in (11.3, 12.8, 14.3, 15.8, 17.3):
        box((32.7, y, 63.35), (47.3, y+.5, 63.9), "housing")
    return out


def write_textures(root=ROOT):
    originals = root / "art/tower-cargo-exchange/originals"
    runtime = root / "src/main/resources/assets/jugcraft/textures/block"
    originals.mkdir(parents=True, exist_ok=True); runtime.mkdir(parents=True, exist_ok=True)
    entries = []
    for name, image in textures().items():
        source = originals / (name + ".png"); target = runtime / source.name
        image.save(source); target.write_bytes(source.read_bytes())
        entries.append({"source": source.relative_to(root).as_posix(), "runtime": target.relative_to(root).as_posix(),
                        "size": list(image.size), "sha256": hashlib.sha256(source.read_bytes()).hexdigest()})
    mw.write(root / "art/tower-cargo-exchange/provenance.json", {
        "author": "OpenAI Codex (GPT-6)", "license": "MIT", "date": "2026-10-08",
        "source": "Original deterministic native geometry, material finishes and logistics markings; no borrowed model, cabinet, screenshot or external artwork.",
        "references": ["Repository ART_DIRECTION.md closed geometry and asset-readback rules",
                       "Owner's approved realistic tower/power quality level; new distinct cargo design",
                       "Owner asset catalog conveyor/industrial categories reviewed; no owner-library pixels imported"],
        "materials": "Original neutral black and charcoal paint, gray mineral foundation, gray hardware, brass pipe union and rubber; all opaque. No blue or beige finish.",
        "runtime": "Stationary logistics machine; no baked live counts, product, fictional speed or motion.",
        "layout": LAYOUT, "textures": entries})
    mw.write(root / "src/main/resources/assets/jugcraft/tower_cargo_exchange_panels.json", LAYOUT)


def model_textures(elements):
    lookup = {name: "jugcraft:block/" + name for name in mw.texture_names(elements)}
    lookup["particle"] = "jugcraft:block/" + PREFIX + "housing"
    return lookup


def native_parts():
    from tower_exchanges import footprint
    return mw.split_model("cargo_exchange", geometry(), footprint(False))


def check_closed_shell():
    """Exported regression: a continuous closed opaque body fills every face.

    Former transport components are absent; the only short cylinders are actual
    attached hinges, control buttons and the sealed item union.
    """
    from tower_exchanges import exported_assembly, footprint
    elements = geometry()
    body = ((2, 4, 2.2), (78, 45.8, 62))
    assert any(tuple(a) == body[0] and tuple(b) == body[1] for a, b, *_ in elements)
    assert all(not mw.unpack(e)[3].get("cargo_drum") for e in elements)
    assert LAYOUT["enclosure"]["closed"] and not LAYOUT["enclosure"]["exposed_transport"]
    assert LAYOUT["socket"]["body"] == [37, 21, 0, 43, 27, 1.2]
    assert tuple(footprint(False)[22]) == (2, 1, 0)
    _, _, boxes = exported_assembly("cargo_exchange", False)
    assert any(lo == [37, 21, 0] and hi[0:2] == [43, 27] and hi[2] >= 1.2
               for _, _, lo, hi, *_ in boxes), "Item neck no longer mates the six-pixel pipe at z0"
    # Probe all six body boundaries at half-pixel intervals. Exported sliced
    # cells must still cover the complete shell after model finishing.
    covered = 0
    for axis in range(3):
        cross = [k for k in range(3) if k != axis]
        for side in (0, 1):
            plane = body[side][axis] + (.02 if side == 0 else -.02)
            sections = [(lo, hi) for _, _, lo, hi, *_ in boxes if lo[axis]-.001 <= plane <= hi[axis]+.001]
            for u in range(math.ceil(body[0][cross[0]]*2), math.floor(body[1][cross[0]]*2)):
                for v in range(math.ceil(body[0][cross[1]]*2), math.floor(body[1][cross[1]]*2)):
                    p = [0, 0, 0]; p[axis] = plane
                    p[cross[0]] = u/2+.25; p[cross[1]] = v/2+.25
                    assert any(all(lo[k]-.001 <= p[k] <= hi[k]+.001 for k in range(3))
                               for lo, hi in sections), ("Open cargo shell", p)
                    covered += 1
    print(f"Closed-shell readback:{covered} covered boundary probes; no exposed transport assembly.")
    return {"closed_shell_boundary_probes": covered, "exposed_transport_components": 0,
            "six_pixel_item_neck_reaches_boundary": True}


def item_elements():
    """Whole closed housing, with the same attached covers, port and live pod."""
    simple = []
    def b(a, z, material): simple.append((a, z, PREFIX+material))
    b((0, 0, 0), (80, 4, 64), "brushed")
    b((2, 4, 2.2), (78, 45.8, 62), "frame")
    b((1.5, 44.8, 1.4), (78.5, 46.4, 62.6), "housing")
    b((2, 4.2, .9), (78, 6.5, 2.6), "brushed")
    for x in (8, 42): b((x, 46.4, 13), (x+29, 47.2, 52), "housing")
    for x0, x1 in ((2.8, 20.2), (21.8, 48.6), (50.2, 77.2)):
        b((x0, 7.2, .95), (x1, 38.8, 2.6), "rubber")
        b((x0+.45, 7.65, .65), (x1-.45, 38.35, 1.8), "housing")
    b((3.7, 27.5, .65), (18.3, 36, 1.7), "frame")
    b((4.2, 28, .4), (17.8, 35.5, .8), "display")
    b((5.7, 19.7, .05), (7.9, 21.9, .7), "red")
    b((14.05, 19.85, .1), (14.75, 21.55, .75), "rubber")
    b((31.8, 17.2, .9), (48.2, 33, 3.1), "frame")
    b((33, 18.4, .65), (47, 30.9, 2.8), "housing")
    b((36.4, 20.4, .4), (43.6, 27.6, 2.8), "rubber")
    b((37, 21, 0), (43, 27, 2.8), "brass")
    b((68.4, 19, .4), (73.8, 28, 1.5), "frame")
    b((70.8, 20.1, .05), (71.4, 26.4, .65), "rubber")
    b((54.7, 10, .35), (65.9, 17.2, 1.6), "frame")
    for y in (10.8, 12.2, 13.6, 15): b((55.4, y, .05), (65.2, y+.55, .9), "housing")
    for x in (5, 29, 53):
        b((x, 7.5, 61.8), (x+22, 39.8, 62.6), "rubber")
        b((x+.5, 8, 62.5), (x+21.5, 39.3, 63.05), "housing")
    for x in (.8, 78.65):
        for z in (7, 35): b((x, 8.6, z+.6), (x+.55, 40.2, z+23.4), "housing")
    native = mw.scaled_elements(simple)
    mw.finish_elements(native)
    return native


def write_assets(root=ROOT):
    from tower_exchanges import footprint
    write_textures(root)
    assets = root / "src/main/resources/assets/jugcraft"; elements = geometry(); lookup = model_textures(elements)
    parts = native_parts()
    for part, native in enumerate(parts):
        mw.write(assets / "models/block" / f"cargo_exchange_part{part}.json", {
            "parent": "minecraft:block/block", "ambientocclusion": False, "textures": lookup, "elements": native})
    item = item_elements()
    mw.write(assets / "models/item/cargo_exchange.json", {
        "parent": "minecraft:block/block", "ambientocclusion": False, "textures": lookup, "elements": item,
        "display": {"gui": {"rotation": [25, -135, 0], "translation": [0, 0, 0], "scale": [.65, .65, .65]},
                    "ground": {"scale": [.3, .3, .3]}, "fixed": {"scale": [.6, .6, .6]}}})
    print(f"Wrote Cargo Exchange only:60 active native parts,{len(elements)} authored solids,{len(textures())} original textures.")


def preview():
    import numpy as np
    import armor_preview as raster
    from tower_exchanges import exported_assembly
    _, faces, _ = exported_assembly("cargo_exchange", False)
    images = {f.tex: np.array(Image.open(f.tex).convert("RGBA")) for f in faces}
    panels = []
    for title, view, scale in (("CLOSED INTAKE / BULKHEAD", (34, 20), 12), ("REAR / SEALED SERVICE COVERS", (145, 23), 12),
                               ("OPERATOR / ITEM UNION", (0, 3), 15)):
        camera = raster.camera(view)[0]
        quads = [(np.array(f.pts), np.array(f.uvs), np.array(f.normal), images[f.tex])
                 for f in faces if np.dot(f.normal, camera) > 0]
        im = raster.render(quads, view, scale=scale, background=(104, 108, 110), lighting="blockbench")
        panel = Image.new("RGB", (1450, 1050), (104, 108, 110))
        panel.paste(im, ((1450-im.width)//2, 48+(930-im.height)//2)); d = ImageDraw.Draw(panel)
        d.text((22, 18), title+" | EXPORTED JSON + PNG", font=font(23, True), fill=PAPER)
        d.text((22, 1010), "Original static machine: blank live display, no product. Offline native asset review.", font=font(19), fill=PAPER)
        panels.append(panel)
    output = ROOT / "build/drone_tower/cargo_exchange_inspection_native.png"
    output.parent.mkdir(parents=True, exist_ok=True); raster.sheet(panels, 3).save(output); print(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true"); parser.add_argument("--check", action="store_true")
    parser.add_argument("--preview", action="store_true"); args = parser.parse_args()
    if args.write: write_assets()
    if args.check:
        from tower_exchanges import check_assets
        check_assets()
        check_closed_shell()
    if args.preview: preview()
