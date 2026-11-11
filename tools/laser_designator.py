"""The LTD-7 laser designator's item model (Justin's sheets 4 and 5, 7 November 2026: "black and grey"; the mockup is
tools/drop_pod/designator.py): a rubber-armoured gunmetal body, the big receiving objective, the laser's aperture and
the rangefinder's receiver in its front bezel, the eyepiece and a live display at the back, a rail on top with LTD-7 on
the top front, grey strips and three buttons down its sides, a raked pistol grip with the mark trigger in its guard.

Writes into src/main/resources/assets/jugcraft/: items/laser_designator.json, models/item/laser_designator.json,
textures/item/laser_designator.png (64 x 64; a uv unit is 4 texels), textures/gui/ltd_sight.png (the sight's housing
round its field, client/LaserDesignatorClient). The body's lens end faces north (-z), as held: forward.
Usage: python3 tools/laser_designator.py"""
import json, math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ASSETS = "src/main/resources/assets/jugcraft"
FB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
T = 64                                                   # the texture's size (texels); a uv unit is T / 16 texels
RUBBER, METAL, DARK, LENS = (0, 0, 16, 16), (16, 0, 32, 16), (32, 0, 36, 4), (36, 0, 44, 8)
FRONT, BACK, TOP = (0, 16, 24, 34), (24, 16, 48, 34), (0, 34, 12, 62)


def atlas():
    """The texture: rubber armour (stippled), gunmetal, black, the eyepiece's glass, the front (objective, aperture,
    receiver in the bezel), the back (the display beside the eyepiece's ring), the top (LTD-7)."""
    rng = np.random.default_rng(7)
    t = np.zeros((T, T, 4), np.float32)
    t[..., 3] = 1
    def fill(r, rgb, var=0.06):
        x0, y0, x1, y1 = r
        t[y0:y1, x0:x1, :3] = np.array(rgb) / 255 * (1 + rng.normal(0, var, (y1 - y0, x1 - x0, 1)))
    fill(RUBBER, (38, 40, 44), 0.10)
    dots = rng.random((16, 16)) > 0.82
    t[0:16, 0:16][dots, :3] *= 0.8
    fill(METAL, (72, 75, 81), 0.05)
    t[0, 16:32, :3] *= 1.15; t[15, 16:32, :3] *= 0.7
    fill(DARK, (14, 15, 17), 0.03)
    im = Image.fromarray((np.clip(t, 0, 1) * 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(im)
    # the eyepiece's glass
    d.rectangle(LENS, fill=(10, 14, 20, 255)); d.ellipse((37, 1, 43, 7), fill=(24, 34, 52, 255)); d.point((39, 3), fill=(150, 170, 200, 255))
    # the front: gunmetal bezel, the objective (left, big), the aperture and the receiver (right)
    x0, y0, x1, y1 = FRONT
    d.rectangle((x0, y0, x1 - 1, y1 - 1), fill=(66, 69, 75, 255))
    d.rectangle((x0, y0, x1 - 1, y1 - 1), outline=(44, 46, 50, 255))
    for (cx, cy, r, glass) in ((x0 + 8.5, y0 + 9.5, 6.5, (22, 34, 58)), (x0 + 19, y0 + 5.5, 2.6, (60, 26, 30)), (x0 + 19, y0 + 13, 2.3, (22, 34, 52))):
        d.ellipse((cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1), fill=(96, 99, 106, 255))
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=glass + (255,))
        d.ellipse((cx - r * 0.55 - 0.5, cy - r * 0.6, cx - r * 0.1, cy - r * 0.2), fill=(170, 186, 210, 255))
    # the back: gunmetal, the eyepiece's ring left, the display right (the support, the range, the account)
    x0, y0, x1, y1 = BACK
    d.rectangle((x0, y0, x1 - 1, y1 - 1), fill=(66, 69, 75, 255), outline=(44, 46, 50, 255))
    d.rectangle((x0 + 11, y0 + 3, x1 - 3, y1 - 4), fill=(6, 10, 12, 255), outline=(96, 99, 106, 255))
    d.line((x0 + 13, y0 + 6, x0 + 18, y0 + 6), fill=(255, 186, 116, 255))      # SQUAD 5
    d.line((x0 + 13, y0 + 9, x0 + 19, y0 + 9), fill=(195, 219, 220, 255))      # the range
    d.line((x0 + 13, y0 + 12, x0 + 17, y0 + 12), fill=(91, 227, 138, 255))     # the account
    d.ellipse((x0 + 2, y0 + 4, x0 + 9, y0 + 11), outline=(28, 30, 34, 255))
    # the top: rubber, LTD-7 stencilled at the front end
    x0, y0, x1, y1 = TOP
    im.paste(im.crop(RUBBER).resize((x1 - x0, y1 - y0), Image.NEAREST), (x0, y0))
    lab = Image.new("RGBA", (40, 12), (0, 0, 0, 0))
    ImageDraw.Draw(lab).text((1, -1), "LTD-7", font=ImageFont.truetype(FB, 11), fill=(200, 200, 196, 255))
    lab = lab.rotate(90, expand=True).resize((6, 20), Image.LANCZOS)
    im.alpha_composite(lab, (x0 + 3, y0 + 1))
    return im


def uv(r):
    return [round(v * 16 / T, 4) for v in r]


def box(name, lo, hi, faces, rot=None):
    """An element from lo to hi (px); faces: {side: region} (a side left out isn't drawn); rot: (origin, axis, angle)."""
    e = {"name": name, "from": [round(v, 4) for v in lo], "to": [round(v, 4) for v in hi],
         "faces": {s: {"uv": uv(r), "texture": "#0"} for s, r in faces.items()}}
    if rot:
        e["rotation"] = {"origin": [round(v, 4) for v in rot[0]], "axis": rot[1], "angle": rot[2]}
    return e


def all6(r, **over):
    f = {s: r for s in ("north", "south", "east", "west", "up", "down")}
    f.update(over)
    return f


def elements():
    E = []
    # the body (5.6 wide, 4.2 high, 12.9 long), its front bezel standing proud, the eyepiece and its cup at the back
    E.append(box("body", (5.2, 5.9, 1.55), (10.8, 10.1, 14.45), all6(RUBBER, north=METAL, south=BACK, up=TOP)))
    E.append(box("bezel", (5.0, 5.7, 1.0), (11.0, 10.3, 1.55), all6(METAL, north=FRONT)))
    E.append(box("eyecup", (5.5, 6.9, 14.45), (7.7, 9.1, 15.8), all6(RUBBER, south=LENS)))
    # the rail and its teeth
    E.append(box("rail", (7.3, 10.1, 4.6), (8.7, 10.4, 11.4), all6(METAL)))
    for k in range(6):
        z = 4.9 + k * 1.15
        E.append(box(f"tooth{k}", (7.15, 10.4, z), (8.85, 10.65, z + 0.4), all6(METAL)))
    # the grey strips and the three buttons down the sides
    for s, (xa, xb) in (("l", (4.95, 5.2)), ("r", (10.8, 11.05))):
        E.append(box(f"strip_{s}", (xa, 7.9, 3.4), (xb, 8.2, 12.6), all6(METAL)))
    for k, z in enumerate((9.4, 10.6, 11.8)):
        E.append(box(f"button{k}", (4.85, 8.8, z), (5.2, 9.35, z + 0.55), all6(METAL if k == 0 else DARK)))
    # the pistol grip, raked back, its butt plate; the trigger and its guard
    rot = ((8.0, 5.9, 10.6), "x", -14)
    E.append(box("grip", (7.3, 2.4, 9.7), (8.7, 5.95, 11.5), all6(RUBBER), rot))
    E.append(box("butt", (7.2, 2.1, 9.6), (8.8, 2.45, 11.6), all6(METAL), rot))
    E.append(box("trigger", (7.75, 4.75, 9.05), (8.25, 5.9, 9.45), all6(DARK)))
    E.append(box("guard", (7.8, 4.3, 8.4), (8.2, 4.55, 10.0), all6(METAL)))
    E.append(box("guard_front", (7.8, 4.3, 8.4), (8.2, 5.9, 8.65), all6(METAL)))
    return E


def display():
    """Held like a pair of binoculars at rest, the lens forward; raised, the sight fills the screen (the client's overlay)."""
    return {"thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3.5, -2.5], "scale": [0.85] * 3},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3.5, -2.5], "scale": [0.85] * 3},
            "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [-1.5, 4.0, -3.0], "scale": [0.9] * 3},
            "firstperson_lefthand": {"rotation": [0, 0, 0], "translation": [-1.5, 4.0, -3.0], "scale": [0.9] * 3},
            "gui": {"rotation": [30, -135, 0], "translation": [0, 0.5, 0], "scale": [0.9] * 3},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5] * 3},
            "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.9] * 3}}


def sight_texture(n=1024):
    """The sight's housing round its round field (transparent inside): a dark ribbed housing, the field's rim."""
    im = Image.new("RGBA", (n, n), (14, 15, 17, 255))
    d = ImageDraw.Draw(im)
    for k in range(-n, n, 12):
        d.line((k, 0, k + n, n), fill=(18, 19, 21, 255), width=2)
    r = int(n * 0.46)
    c = n // 2
    mask = Image.new("L", (n, n), 255)
    ImageDraw.Draw(mask).ellipse((c - r, c - r, c + r, c + r), fill=0)
    mask = mask.filter(ImageFilter.GaussianBlur(10))
    im.putalpha(mask)
    d = ImageDraw.Draw(im)
    d.ellipse((c - r - 6, c - r - 6, c + r + 6, c + r + 6), outline=(60, 62, 66, 255), width=8)
    return im


def main():
    os.makedirs(f"{ASSETS}/models/item", exist_ok=True)
    os.makedirs(f"{ASSETS}/items", exist_ok=True)
    os.makedirs(f"{ASSETS}/textures/item", exist_ok=True)
    os.makedirs(f"{ASSETS}/textures/gui", exist_ok=True)
    atlas().save(f"{ASSETS}/textures/item/laser_designator.png")
    sight_texture().save(f"{ASSETS}/textures/gui/ltd_sight.png")
    model = {"textures": {"0": "jugcraft:item/laser_designator", "particle": "jugcraft:item/laser_designator"},
             "elements": elements(), "display": display()}
    with open(f"{ASSETS}/models/item/laser_designator.json", "w") as f:
        json.dump(model, f, indent=1)
    with open(f"{ASSETS}/items/laser_designator.json", "w") as f:
        json.dump({"model": {"type": "minecraft:model", "model": "jugcraft:item/laser_designator"}}, f, indent=2)
    print("laser_designator:", len(model["elements"]), "elements")


if __name__ == "__main__":
    main()
