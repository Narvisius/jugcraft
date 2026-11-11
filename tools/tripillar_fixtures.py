"""Assets for the Tri-Pillar hall's live fittings (tower/HallFixtureBlock.java; placed by tools/tripillar_hall.py; drawn
live by client/HallFixtureRenderer.java), the glass-door equipment rack with live LEDs, and the bay number tags on the
tower's hangar jambs (client/DroneDepotRenderer.java).

Real-world basis:
- touch console: a free-standing interactive kiosk (steel base plate, slim column, a 32-inch touchscreen tilted back
  about 30 degrees at standing height, as in control-room and museum kiosks);
- info totem: a double-sided digital signage totem, 2 m tall, a portrait screen in a slim dark body with lit edges;
- holo emitter: a floor-flush projector disc (steel ring, glass lens) under each floating panel;
- drone display stand: a dark puck the drone stands on, on a plinth under glass; the service style is the stand of
  the service bay's lifted cradle (a scissor lift, drawn by the renderer);
- service arm: a 6-axis industrial robot, drawn whole and live by client/ServiceArm (meshes: tools/service_arm.py); the
  block has no model of its own and its item is an icon (tools/item_icons/drone_service_arm.txt);
- equipment rack: a 42U server rack with a tinted glass front door, the servers' status LEDs blinking behind it;
- bay tags: small engraved aluminium tags (dark letters on brushed metal, a rivet at each end), as on real hangars.

Models face north: whoever uses the piece stands south of it and looks north, so its front is its south (+z) face.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

MOD = "jugcraft"
FIXTURES = {
    "touch_console": "Touch Console",
    "tower_info_totem": "Tower Info Totem",
    "holo_emitter": "Holo Panel Emitter",
    "drone_display_stand": "Drone Display Stand",
    "drone_service_arm": "Drone Service Arm",
}
GLOW = {"fx_amber", "fx_lens", "furniture_led"}
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}

STEEL_PLATE = "#c:plates/steel"
CRAFTING = {
    "touch_console": (["G", "C", "P"], {"G": "minecraft:black_stained_glass_pane", "C": "jugcraft:basic_circuit",
                                        "P": "#c:plates/iron"}, 1),
    "tower_info_totem": (["PGP", "PCP", "PPP"], {"P": "#c:plates/iron", "G": "minecraft:black_stained_glass_pane",
                                                  "C": "jugcraft:advanced_circuit"}, 1),
    # (The Holo Panel Emitter has no recipe: a hologram, the Drone Tower's own; tower/Holograms, Justin 28 October 2026.)
    "drone_display_stand": (["PLP", "PPP"], {"P": STEEL_PLATE, "L": "minecraft:glowstone_dust"}, 2),
    "drone_service_arm": (["GM ", "PG ", "PPP"], {"G": "#c:gears/steel", "M": "jugcraft:drone_motor", "P": STEEL_PLATE}, 1),
}


def box(f, t, tex, rot=None, faces=None):
    e = {"from": list(f), "to": list(t),
         "faces": {d: {"texture": f"#{tex}"} for d in (faces or ("north", "south", "east", "west", "up", "down"))}}
    if rot:
        e["rotation"] = rot
    if tex in GLOW:
        e["light_emission"] = 12
    return e


# The touch console's head is tilted back 22.5 degrees about x through its pivot: the screen faces up and towards
# the user. client/HallFixtureRenderer draws the live screen on the same plane (TOUCH_PIVOT, TOUCH_TILT).
TOUCH_PIVOT = (8, 12, 8)
TOUCH_TILT = 22.5


def touch_console():
    tilt = {"origin": list(TOUCH_PIVOT), "axis": "x", "angle": TOUCH_TILT}
    return [
        box((3, 0, 3), (13, 1, 13), "fx_steel"),
        box((6, 1, 6), (10, 11, 10), "fx_body"),
        box((7, 2, 10), (9, 10, 10.2), "fx_amber"),
        box((5, 10.5, 5), (11, 12, 11), "fx_steel"),
        box((2, 12, 3), (14, 13, 13), "fx_body", tilt),
        box((2.6, 13, 3.6), (13.4, 13.15, 12.4), "fx_screen", tilt, faces=("up",)),
        box((2, 13, 12.4), (14, 13.4, 13), "fx_steel", tilt),
    ]


def info_totem():
    return [
        box((2, 0, 5), (14, 1, 11), "fx_steel"),
        box((3, 1, 6), (13, 31, 10), "fx_body"),
        # the screens stand proud of the body: their edges show too (the face against the body is left out)
        box((3.6, 5, 10), (12.4, 29, 10.1), "fx_screen", faces=("south", "east", "west", "up", "down")),
        box((3.6, 5, 5.9), (12.4, 29, 6), "fx_screen", faces=("north", "east", "west", "up", "down")),
        box((2.5, 31, 5.5), (13.5, 32, 10.5), "fx_steel"),
        box((2.7, 1, 10), (3.2, 31, 10.2), "fx_amber"),
        box((12.8, 1, 10), (13.3, 31, 10.2), "fx_amber"),
        box((2.7, 1, 5.8), (3.2, 31, 6), "fx_amber"),
        box((12.8, 1, 5.8), (13.3, 31, 6), "fx_amber"),
    ]


def holo_emitter():
    return [
        box((2, 0, 2), (14, 1, 14), "fx_steel"),
        box((3, 1, 3), (13, 1.4, 13), "fx_body"),
        box((4, 1.4, 4), (12, 1.6, 12), "fx_steel"),
        box((5.5, 1.6, 5.5), (10.5, 2, 10.5), "fx_lens"),
    ]


def display_stand():
    return [
        box((4, 0, 4), (12, 2, 12), "fx_body"),
        box((4.5, 2, 4.5), (11.5, 2.2, 11.5), "fx_steel"),
        box((4, 0.8, 11.9), (12, 1.2, 12.1), "fx_amber"),
        box((4, 0.8, 3.9), (12, 1.2, 4.1), "fx_amber"),
    ]


def service_arm():
    """None: client/ServiceArm draws the whole robot."""
    return []


MODELS = {"touch_console": touch_console, "tower_info_totem": info_totem, "holo_emitter": holo_emitter,
          "drone_display_stand": display_stand, "drone_service_arm": service_arm}


# ------------------------------------------------------------------ the glass-door equipment rack

def equipment_rack():
    """A 42U rack: open steel frame, the servers' faces behind (live LEDs), a tinted glass front door with a handle."""
    return [
        box((1, 0, 1), (15, 16, 2), "furniture_rack"),
        box((1, 0, 2), (2, 16, 15), "furniture_rack"),
        box((14, 0, 2), (15, 16, 15), "furniture_rack"),
        box((2, 15, 2), (14, 16, 15), "furniture_rack"),
        box((2, 0, 2), (14, 1, 15), "furniture_rack"),
        box((2, 1, 2), (14, 15, 14), "furniture_rack_servers", faces=("south",)),
        box((2, 1, 14.6), (14, 15, 15), "furniture_rack_glass", faces=("south", "north")),
        box((12.6, 6, 15), (13.2, 10, 15.5), "furniture_chrome"),
    ]


# ------------------------------------------------------------------ textures

def _rng(seed):
    return random.Random(seed)


def tex_steel(size=32):
    r = _rng(11)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = 74 + r.randint(-4, 4) + (6 if (y % 8) == 0 else 0)
            img.putpixel((x, y), (v, v + 3, v + 8, 255))
    for x, y in ((2, 2), (size - 3, 2), (2, size - 3), (size - 3, size - 3)):
        img.putpixel((x, y), (140, 146, 152, 255))
    return img


def tex_body(size=32):
    r = _rng(12)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = 34 + r.randint(-2, 2)
            img.putpixel((x, y), (v, v + 2, v + 5, 255))
    return img


def tex_amber(size=16):
    img = Image.new("RGBA", (size, size), (255, 170, 70, 255))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, size - 1, size - 1), outline=(255, 200, 120, 255))
    return img


def tex_lens(size=16):
    img = Image.new("RGBA", (size, size), (40, 30, 20, 255))
    d = ImageDraw.Draw(img)
    for r, c in ((7, (180, 110, 40)), (5, (255, 170, 70)), (3, (255, 225, 170))):
        d.ellipse((8 - r, 8 - r, 7 + r, 7 + r), fill=c + (255,))
    return img


def tex_screen(size=32):
    """A switched-off screen: near-black glass with a faint diagonal sheen (the live picture is drawn over it)."""
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            s = 6 if abs((x - y) % 24 - 12) < 2 else 0
            img.putpixel((x, y), (12 + s, 14 + s, 17 + s, 255))
    return img


def tex_rack_door(size=32):
    """Tinted glass in a thin steel frame, a little reflection."""
    img = Image.new("RGBA", (size, size), (20, 26, 30, 90))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, size - 1, size - 1), outline=(70, 74, 80, 255))
    d.rectangle((1, 1, size - 2, size - 2), outline=(46, 50, 55, 255))
    for i in range(size):
        x = (i * 2) % size
        if 4 < i < size - 6:
            img.putpixel((min(size - 3, x // 2 + 8), i), (90, 110, 120, 70))
    return img


def tex_rack_servers(size=32, frames=24):
    """The servers behind the glass: 1U and 2U faces in dark grey with drive bays, and status LEDs that blink at their
    own rates (green, amber activity, one blue locate light): an animation strip of ``frames`` frames."""
    r = _rng(21)
    units = []
    y = 1
    while y < size - 2:
        h = 2 if r.random() < 0.6 else 4
        if y + h > size - 1:
            break
        units.append((y, h))
        y += h + 1
    leds = []
    for (uy, h) in units:
        n = r.randint(2, 4)
        for k in range(n):
            x = 3 + k * 2
            colour = r.choice([(70, 230, 90), (70, 230, 90), (255, 170, 60), (80, 160, 255)])
            period = r.randint(3, 12)
            phase = r.randint(0, period - 1)
            duty = r.random() * 0.6 + 0.2
            leds.append((x, uy + h // 2, colour, period, phase, duty))
    strip = Image.new("RGBA", (size, size * frames))
    for f in range(frames):
        img = Image.new("RGBA", (size, size), (16, 17, 19, 255))
        d = ImageDraw.Draw(img)
        for (uy, h) in units:
            d.rectangle((1, uy, size - 2, uy + h - 1), fill=(44, 46, 50, 255))
            for bx in range(13, size - 3, 4):
                d.rectangle((bx, uy, bx + 2, uy + h - 1), fill=(30, 32, 35, 255))
        for (x, ly, colour, period, phase, duty) in leds:
            on = ((f + phase) % period) < period * duty
            img.putpixel((x, ly), colour + (255,) if on else tuple(c // 5 for c in colour) + (255,))
        strip.paste(img, (0, f * size))
    return strip


TEXTURES = {"fx_steel": tex_steel, "fx_body": tex_body, "fx_amber": tex_amber, "fx_lens": tex_lens,
            "fx_screen": tex_screen, "furniture_rack_glass": tex_rack_door}


# ------------------------------------------------------------------ bay number tags

TAG_W, TAG_H = 64, 24
TAG_COLS = 8
TAG_ROWS_PER_PILLAR = 6   # up to 48 bays a pillar
BAY_TAGS = "textures/entity/tripillar_bay_tags.png"


def bay_tag(label):
    """One engraved aluminium tag: brushed metal, a bevelled edge, dark engraved letters, a rivet at each end."""
    r = _rng(sum(ord(c) * (i + 1) for i, c in enumerate(label)))
    img = Image.new("RGBA", (TAG_W, TAG_H))
    for y in range(TAG_H):
        base = 176 + r.randint(-3, 3)
        for x in range(TAG_W):
            v = base + r.randint(-6, 6)
            img.putpixel((x, y), (v, v + 2, v + 5, 255))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, TAG_W - 1, TAG_H - 1), outline=(120, 124, 130, 255))
    d.line((1, 1, TAG_W - 2, 1), fill=(214, 216, 220, 255))
    d.line((1, TAG_H - 2, TAG_W - 2, TAG_H - 2), fill=(132, 136, 142, 255))
    for cx in (5, TAG_W - 6):
        d.ellipse((cx - 2, TAG_H // 2 - 2, cx + 2, TAG_H // 2 + 2), fill=(150, 154, 160, 255), outline=(96, 100, 106, 255))
        d.point((cx - 1, TAG_H // 2 - 1), fill=(220, 222, 226, 255))
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf", 17)
    w = d.textlength(label, font=font)
    x0 = (TAG_W - w) / 2
    d.text((x0 + 0.6, 3.6), label, font=font, fill=(236, 238, 240, 255))   # the engraving's lit lower edge
    d.text((x0, 3), label, font=font, fill=(34, 36, 40, 255))
    return img


def bay_tag_sheet():
    """Every tag A01..C48: pillar p, number n at column (n - 1) % 8, row p * 6 + (n - 1) // 8 (DroneDepotRenderer)."""
    sheet = Image.new("RGBA", (TAG_W * TAG_COLS, TAG_H * TAG_ROWS_PER_PILLAR * 3), (0, 0, 0, 0))
    for p, letter in enumerate("ABC"):
        for n in range(1, TAG_COLS * TAG_ROWS_PER_PILLAR + 1):
            col, row = (n - 1) % TAG_COLS, p * TAG_ROWS_PER_PILLAR + (n - 1) // TAG_COLS
            sheet.paste(bay_tag(f"{letter}{n:02d}"), (col * TAG_W, row * TAG_H))
    return sheet


# ------------------------------------------------------------------ writing

def write_all(write, rid, assets, lang):
    """Models, blockstates, item models, textures, lang: the fittings, the glass-door rack, the bay tags."""
    assets = Path(assets)
    tex_dir = assets / "textures" / "block"
    tex_dir.mkdir(parents=True, exist_ok=True)
    for name, fn in TEXTURES.items():
        fn().save(tex_dir / f"{name}.png")
    tex_rack_servers().save(tex_dir / "furniture_rack_servers.png")
    (tex_dir / "furniture_rack_servers.png.mcmeta").write_text(json.dumps({"animation": {"frametime": 3}}, indent=2) + "\n")
    ent = assets / BAY_TAGS
    ent.parent.mkdir(parents=True, exist_ok=True)
    bay_tag_sheet().save(ent)

    def model(elements, translucent=()):
        if not elements:
            return {"textures": {"particle": rid("block/fx_steel")}, "elements": []}
        used = sorted({f["texture"][1:] for e in elements for f in e["faces"].values()})
        textures = {}
        for t in used:
            sprite = rid(f"block/{t}")
            textures[t] = {"force_translucent": True, "sprite": sprite} if t in translucent else sprite
        textures["particle"] = rid(f"block/{used[0]}")
        return {"ambientocclusion": False, "textures": textures, "elements": elements}

    for block, display in FIXTURES.items():
        lang[f"block.{MOD}.{block}"] = display
        write(assets / "models" / "block" / f"{block}.json", model(MODELS[block]()))
        if block == "holo_emitter":
            variants = {f"facing={f},panel={p}": {"model": rid(f"block/{block}"), **({"y": y} if y else {})}
                        for f, y in FACING_Y.items() for p in ("fleet", "tower", "supply")}
        elif block == "drone_display_stand":
            variants = {f"facing={f},style={s},tier={t}": {"model": rid(f"block/{block}"), **({"y": y} if y else {})}
                        for f, y in FACING_Y.items() for s in ("display", "service") for t in range(1, 10)}
        else:
            variants = {f"facing={f}": {"model": rid(f"block/{block}"), **({"y": y} if y else {})} for f, y in FACING_Y.items()}
        write(assets / "blockstates" / f"{block}.json", {"variants": variants})
        if block == "drone_service_arm":
            write(assets / "models" / "item" / f"{block}.json", {"parent": "minecraft:item/generated",
                                                                 "textures": {"layer0": rid(f"item/{block}")}})
            write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{block}")}})
            continue
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
    write(assets / "models" / "block" / "equipment_rack.json", model(equipment_rack(), translucent=("furniture_rack_glass",)))
