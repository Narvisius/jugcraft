"""The flat screen TV's block models (tv/FlatTvBlock.java, docs/features/flat-screen-tv.md): one model for each way a
block of screen can sit in a bigger screen, by which sides the screen carries on past it (left, right, up, down, as
you look at it) and whether it hangs on a wall or stands. Drawn facing north: the panel's back to the north, 5 cm
(0.8 px) deep, the glass facing south; the bezel's lip 0.12 px proud of the glass, only round the screen's outside
(0.25 px at the sides and top, the 0.55 px chin along the bottom). Standing, the panel is in the middle of the block
and its bottom row 1.6 px up on the foot. The live parts (the picture, the logo, the power light, the neck and foot)
are client/tv/TvRenderer.java's; its sizes are these. Mocked up in r22b/tv (flat_tv_take1).

    python3 tools/flat_tv.py         (writes the models, the blockstate and the textures; plant_interior.py calls it too)
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

BEZ_S, BEZ_T, BEZ_B, LIP = 0.25, 0.25, 0.55, 0.12
DEPTH = 0.8
WALL_BACK, STAND_BACK, LIFT = 0.0, 7.6, 1.6
TURN = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"


def name(left, right, up, down, stand):
    return "flat_tv_" + "".join(k if v else "_" for k, v in zip("lrud", (left, right, up, down))).replace("_", "x") + ("_stand" if stand else "_wall")


def model(left, right, up, down, stand, rid):
    """One block of screen: the panel (glass on its face, the back plain or against the wall) and the lip's pieces."""
    z0 = STAND_BACK if stand else WALL_BACK
    z1 = z0 + DEPTH
    y0 = LIFT if stand and not down else 0.0
    body_faces = {"south": {"texture": "#glass", "uv": [0, 0, 16, 16 - y0]}}
    body_faces["north"] = {"texture": "#back"} if stand else {"texture": "#back", "cullface": "north"}
    # Seen from the south, the viewer's left is the model's west (-x), their right its east (FlatTvBlock: right is
    # the facing turned clockwise).
    if not left:
        body_faces["west"] = {"texture": "#body"}
    if not right:
        body_faces["east"] = {"texture": "#body"}
    if not up:
        body_faces["up"] = {"texture": "#body"}
    if not down:
        body_faces["down"] = {"texture": "#body"}
    els = [{"from": [0, y0, z0], "to": [16, 16, z1], "faces": body_faces}]
    lip = lambda f, t, faces: els.append({"from": f, "to": t, "faces": {k: {"texture": "#body"} for k in faces}})
    ends = (["up"] if not up else []) + (["down"] if not down else [])
    if not left:
        lip([0, y0, z1], [BEZ_S, 16, z1 + LIP], ["south", "west", "east"] + ends)
    if not right:
        lip([16 - BEZ_S, y0, z1], [16, 16, z1 + LIP], ["south", "east", "west"] + ends)
    # The top and the chin run between the sides (or on into the next block): their ends are buried or another's.
    lx0, lx1 = (BEZ_S if not left else 0.0), (16 - BEZ_S if not right else 16.0)
    if not up:
        lip([lx0, 16 - BEZ_T, z1], [lx1, 16, z1 + LIP], ["south", "up", "down"])
    if not down:
        lip([lx0, y0, z1], [lx1, y0 + BEZ_B, z1 + LIP], ["south", "up", "down"])
    # The lip's ends where the screen carries on are another block's: no faces there; its back is on the glass.
    return {"textures": {"glass": rid("block/tv_glass"), "body": rid("block/tv_body"), "back": rid("block/tv_back"), "particle": rid("block/tv_body")},
            "elements": els}


def write_assets(write, rid, assets):
    """The 32 models and the blockstate (128 states: facing, the four joins, standing)."""
    variants = {}
    for bits in range(32):
        left, right, up, down, stand = (bool(bits >> k & 1) for k in range(5))
        n = name(left, right, up, down, stand)
        write(assets / "models" / "block" / f"{n}.json", model(left, right, up, down, stand, rid))
        for face, y in TURN:
            key = f"down={str(down).lower()},facing={face},left={str(left).lower()},right={str(right).lower()},stand={str(stand).lower()},up={str(up).lower()}"
            variants[key] = {"model": rid(f"block/{n}"), **({"y": y} if y else {})}
    write(assets / "blockstates" / "flat_tv.json", {"variants": variants})


def _noise(rgb, seed, amount, size=64):
    rng = np.random.default_rng(seed)
    a = np.ones((size, size, 3)) * np.array(rgb, float) + rng.normal(0, amount, (size, size, 1))
    return a


def draw_textures():
    """tv_body (the satin black bezel), tv_glass (the glass switched off), tv_back, tv_metal (the stand), tv_led."""
    out = {}
    out["tv_body"] = Image.fromarray(np.clip(_noise((27, 28, 31), 501, 1.2), 0, 255).astype(np.uint8))
    g = _noise((8, 9, 11), 502, 0.6)
    yy = np.linspace(0, 1, 64)[:, None, None]
    g += (1 - yy) * 3                       # a little of the room's light from above
    out["tv_glass"] = Image.fromarray(np.clip(g, 0, 255).astype(np.uint8))
    b = _noise((34, 35, 38), 503, 1.6)
    b[::8, :, :] -= 6                       # the back's moulded ribs
    out["tv_back"] = Image.fromarray(np.clip(b, 0, 255).astype(np.uint8))
    m = _noise((58, 60, 64), 504, 1.0)
    rng = np.random.default_rng(505)
    m += rng.normal(0, 2.2, (1, 64, 1))     # brushed across
    out["tv_metal"] = Image.fromarray(np.clip(m, 0, 255).astype(np.uint8))
    led = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    ImageDraw.Draw(led).ellipse([4, 4, 59, 59], fill=(255, 255, 255, 255))
    out["tv_led"] = led
    return out


if __name__ == "__main__":
    def _write(path, obj):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(obj, indent=2) + "\n")
    import zfight
    write_assets(zfight.unfighting(_write), lambda p: f"jugcraft:{p}", ASSETS)
    for n, img in draw_textures().items():
        img.save(ASSETS / "textures" / "block" / f"{n}.png")
    print("flat_tv: 32 models, 128 states, 5 textures")
