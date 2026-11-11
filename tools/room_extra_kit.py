"""The upper floor's pieces that are not in a room kit of their own (work list item 22 part B): the Pilot Simulator
(room 9: 2 x 2 x 2, its seat reclined 22.5 degrees, the side screens folded in toward the pilot, a Minecraft world on its
screens from tools/sim_world.py, each simulator its own scenario), the briefing room's lit sign (room 1: the map room
sign's light box with BRIEFING on it and the IN SESSION lamp), its tier risers (carpeted blocks with an aluminium
nosing and an amber step light set into their face where they step down: tools/riser_models.py) and the aisle's half
steps.
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import model_writer as MW

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
ALL = ("north", "south", "east", "west", "up", "down")


def _wj(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=1) + "\n")


def _finished(model):
    """A hand-written model made whole: no face left open, none flush with another drawn differently, every UV inside
    its sprite (the art rules every model is checked to)."""
    els = model["elements"]
    MW.close_open_faces(els)
    MW.separate_coplanar(els, pin_uv=False)
    MW.fit_uvs(els)
    return model


def write_sim(assets, ns):
    import sim_world as SW
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    import training_kit as TK
    for n in range(6):
        # (the HUD's changing figures left out: the game draws them live, client/LiveFaces)
        strip, marks = SW.pod(n, live=True)
        w, h = strip.size[0] // 3, strip.size[1]
        for i, k in enumerate(("l", "c", "r")):
            # (square, as the block atlas's textures are: the face maps the whole of it, so the view keeps its shape)
            strip.crop((i * w, 0, (i + 1) * w, strip.size[1])).resize((256, 256), Image.LANCZOS).save(tex_dir / f"sim{n}_screen_{k}.png")
            parts = []
            for mk in marks:
                x0, y0, x1, y1 = mk["rect"]
                if i * w <= (x0 + x1) / 2 < (i + 1) * w:
                    parts.append({**mk, "rect": [round((x0 - i * w) / w, 5), round(y0 / h, 5), round((x1 - i * w) / w, 5), round(y1 / h, 5)]})
            TK.TEXTURE_LIVE[f"{ns}:block/sim{n}_screen_{k}"] = parts

    def solid(name, rgb, noise=6, seed=1):
        rng = np.random.default_rng(seed)
        a = np.clip(np.array(rgb)[None, None, :] + rng.normal(0, noise, (16, 16, 1)), 0, 255).astype(np.uint8)
        Image.fromarray(a, "RGB").save(tex_dir / f"{name}.png")
    solid("sim_base", (46, 48, 52), 4, 1)
    solid("sim_frame", (28, 29, 31), 3, 2)
    rng = np.random.default_rng(3)
    a = np.zeros((16, 16, 3))
    for x in range(16):
        a[:, x] = 58 if x % 4 else 38
    a += rng.normal(0, 2.0, (16, 16, 1))
    a[::5, :] -= 6
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").save(tex_dir / "sim_seat.png")
    solid("sim_red", (170, 40, 36), 6, 4)
    solid("sim_alu", (168, 172, 176), 5, 5)
    a = np.full((32, 32, 3), 70, np.uint8)
    for y in range(32):
        for x in range(32):
            if x < 3 or y < 3 or x > 28 or y > 28:
                a[y, x] = (226, 186, 40) if ((x + y) // 3) % 2 else (24, 24, 24)
    Image.fromarray(a, "RGB").save(tex_dir / "sim_deck.png")
    E = []

    def box(f, t, tex, faces=None, rot=None, shade=True, rest=None):
        fc = {k: {"texture": f"#{tex}"} for k in (faces or ALL)}
        if rest:
            fc.update({k: {"texture": f"#{rest}"} for k in ALL if k not in fc})
        e = {"from": list(f), "to": list(t), "faces": fc}
        if rot:
            e["rotation"] = rot
        if not shade:
            e["shade"] = False
        E.append(e)
        return e
    for (x, z) in ((3, 3), (25, 3), (3, 25), (25, 25)):
        box((x, 0, z), (x + 4, 1.4, z + 4), "frame")
    box((7, 0, 7), (25, 1.2, 25), "frame")
    box((0.5, 1.4, 0.5), (31.5, 2.8, 31.5), "deck", ("up",), rest="base")
    box((10, 2.8, 5), (22, 3.8, 12), "frame")
    box((11.5, 3.8, 7), (14.5, 8.5, 8.2), "alu", rot={"angle": -22.5, "axis": "x", "origin": [13, 3.8, 7.6]})
    box((17.5, 3.8, 7), (20.5, 8.5, 8.2), "alu", rot={"angle": -22.5, "axis": "x", "origin": [19, 3.8, 7.6]})
    box((12, 2.8, 17), (20, 5.6, 23), "frame")
    box((9.4, 5.6, 15.5), (22.6, 7, 24.5), "frame")
    box((11, 7, 15.5), (21, 9.5, 24.5), "seat")
    box((9.4, 7, 16), (11, 11.5, 24.5), "seat")
    box((21, 7, 16), (22.6, 11.5, 24.5), "seat")
    rec = {"angle": 22.5, "axis": "x", "origin": [16, 7.5, 24]}
    box((10.6, 7.5, 23), (21.4, 20, 25), "seat", rot=rec)
    box((9.4, 9.5, 23.2), (10.6, 19, 25), "seat", rot=rec)
    box((21.4, 9.5, 23.2), (22.6, 19, 25), "seat", rot=rec)
    box((9.4, 9.5, 23.0), (10.6, 19, 23.2), "red", ("north",), rot=rec, rest="seat")
    box((21.4, 9.5, 23.0), (22.6, 19, 23.2), "red", ("north",), rot=rec, rest="seat")
    box((12.5, 20, 23.4), (19.5, 24, 24.8), "seat", rot=rec)
    box((12.5, 22.4, 23.2), (19.5, 22.8, 23.4), "red", ("north",), rot=rec, rest="seat")
    box((4.5, 2.8, 15), (9.4, 10.5, 25), "frame")
    box((22.6, 2.8, 15), (27.5, 10.5, 25), "frame")
    box((5.5, 10.5, 17), (8.5, 11.6, 21.5), "base")
    box((6.5, 11.6, 18.5), (7.5, 15, 19.5), "alu", rot={"angle": 22.5, "axis": "x", "origin": [7, 11.6, 19]})
    box((5.9, 15, 17.6), (8.1, 17.4, 20.2), "frame", rot={"angle": 22.5, "axis": "x", "origin": [7, 11.6, 19]})
    box((23.6, 10.5, 17.5), (26.4, 11.6, 21), "base")
    box((24.5, 11.6, 18.8), (25.5, 16.5, 19.8), "alu")
    box((23.8, 16.5, 18.2), (26.2, 20.5, 20.6), "frame")
    box((25.6, 19.6, 18.6), (26.4, 20.4, 19.2), "red")
    box((14.8, 2.8, 0.8), (17.2, 15, 3), "frame")
    box((5, 14.5, 1.2), (27, 15.6, 2.6), "frame")
    box((6.5, 15.6, 2.2), (25.5, 27.2, 3.6), "frame")
    box((7, 16.1, 3.6), (25, 26.7, 3.7), "sc", ("south",), shade=False, rest="frame")
    for side, x0, x1, o, ang in (("l", -8, 6.5, 6.5, 45), ("r", 25.5, 40, 25.5, -45)):
        r = {"angle": ang, "axis": "y", "origin": [o, 0, 2.9]}
        box((x0, 15.6, 2.2), (x1, 27.2, 3.6), "frame", rot=r)
        box((x0 + 0.5, 16.1, 3.6), (x1 - 0.5, 26.7, 3.7), f"s{side}", ("south",), rot=r, shade=False, rest="frame")
        box((x0 + 2, 14.6, 1.6), (x1, 15.5, 2.6), "frame", rot=r)
    MW.close_open_faces(E)
    MW.separate_coplanar(E, pin_uv=False)
    tex = {"frame": f"{ns}:block/sim_frame", "deck": f"{ns}:block/sim_deck", "base": f"{ns}:block/sim_base",
           "seat": f"{ns}:block/sim_seat", "red": f"{ns}:block/sim_red", "alu": f"{ns}:block/sim_alu", "particle": f"{ns}:block/sim_frame"}
    parts = {p: [] for p in range(8)}
    for e in E:
        f, t = e["from"], e["to"]
        mx, my, mz = [(a + b) / 2 for a, b in zip(f, t)]
        c = 0 if mx < 16 else 1
        row = 0 if mz < 16 else 1
        k = 0 if my < 16 else 1
        p = k * 4 + row * 2 + c
        o = [16 * c, 16 * k, 16 * row]
        e2 = json.loads(json.dumps(e))
        e2["from"] = [round(a - b, 3) for a, b in zip(f, o)]
        e2["to"] = [round(a - b, 3) for a, b in zip(t, o)]
        if "rotation" in e2:
            e2["rotation"]["origin"] = [round(a - b, 3) for a, b in zip(e2["rotation"]["origin"], o)]
        parts[p].append(e2)
    for els in parts.values():
        MW.fit_uvs(els)
        for e in els:
            for fc in e["faces"].values():
                if fc["texture"] in ("#sc", "#sl", "#sr"):
                    fc["uv"] = [0, 0, 16, 16]
    variants = {}
    for p, els in parts.items():
        screens = any(fc["texture"] in ("#sc", "#sl", "#sr") for e in els for fc in e["faces"].values())
        for n in range(6):
            t2 = dict(tex)
            if screens:
                t2.update({f"s{k}": f"{ns}:block/sim{n}_screen_{k}" for k in ("c", "l", "r")})
                name = f"pilot_simulator_{p}_s{n}"
            else:
                name = f"pilot_simulator_{p}"
            if screens or n == 0:
                _wj(model_dir / f"{name}.json", {"textures": t2, "elements": els})
            for fname, y in FACINGS.items():
                variants[f"facing={fname},part={p},sim={n}"] = {"model": f"{ns}:block/{name}", **({"y": y} if y else {})}
    _wj(bs_dir / "pilot_simulator.json", {"variants": variants})


def write_brief_sign(assets, ns, session=False):
    """The briefing room's lit sign (four blocks wide, the map room sign's light box): BRIEFING, the line under it, the
    IN SESSION lamp at its right end (lit in game while anyone is in the room; drawn off here)."""
    import aisle_containment as AC
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    W, H = 2048, 307
    im = Image.new("RGBA", (W, H), (16, 17, 19, 255))
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype(AC.FONT_B, 160)
    fs = ImageFont.truetype(AC.FONT_B, 44)
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    cx = 820
    ImageDraw.Draw(glow).text((cx, 122), "BRIEFING", font=f, fill=(255, 170, 60, 255), anchor="mm")
    im.alpha_composite(glow.filter(ImageFilter.GaussianBlur(9)))
    d.text((cx, 122), "BRIEFING", font=f, fill=(255, 196, 110, 255), anchor="mm")
    d.line((150, 228, 1490, 228), fill=(90, 92, 96, 255), width=4)
    d.text((cx, 268), "MISSIONS  ·  AIRSPACE  ·  FLEET", font=fs, fill=(226, 228, 230, 255), anchor="mm")
    x0, x1 = 1600, 1990
    d.rectangle((x0, 34, x1, 274), fill=(40, 10, 10, 255), outline=(80, 82, 86, 255), width=6)
    if session:
        g2 = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(g2).rectangle((x0 + 14, 48, x1 - 14, 260), fill=(255, 50, 40, 255))
        im.alpha_composite(g2.filter(ImageFilter.GaussianBlur(14)))
        d.rectangle((x0 + 14, 48, x1 - 14, 260), fill=(230, 40, 34, 255))
    fl = ImageFont.truetype(AC.FONT_B, 78)
    on = (255, 236, 230, 255) if session else (120, 60, 60, 255)
    d.text(((x0 + x1) / 2, 110), "IN", font=fl, fill=on, anchor="mm")
    d.text(((x0 + x1) / 2, 200), "SESSION", font=fl, fill=on, anchor="mm")
    variants = {}
    src = Path(__file__).resolve().parent.parent / "src/main/resources/assets/jugcraft/models/block"
    for p in range(4):
        tile = Image.new("RGBA", (512, 512), (16, 17, 19, 255))
        tile.paste(im.crop((p * 512, 0, p * 512 + 512, H)), (0, 0))
        tile.save(tex_dir / f"brs_face_{p}.png")
        m = json.loads((src / f"map_room_sign_{p}.json").read_text())
        m["textures"]["face"] = f"{ns}:block/brs_face_{p}"
        for e in m["elements"]:
            for fc in e.get("faces", {}).values():
                if fc.get("texture") == "#face":
                    e["shade"] = False
        _wj(model_dir / f"briefing_sign_{p}.json", m)
        for fname, y in FACINGS.items():
            variants[f"facing={fname},part={p}"] = {"model": f"{ns}:block/briefing_sign_{p}", **({"y": y} if y else {})}
    _wj(bs_dir / "briefing_sign.json", {"variants": variants})


def write_tiers(assets, ns):
    """The tier riser and the aisle half step (tools/riser_models.py: the step light set into a channel in the riser's
    face under a flush nosing, running edge to edge; Justin's walk through, note 27): a model for each set of edges the
    riser steps down on (tier_riser for none), the half step's one."""
    import riser_models as RSM
    model_dir, bs_dir = assets / "models/block", assets / "blockstates"
    variants = {}
    for key, (edges, els) in RSM.all_risers().items():
        name = "tier_riser" if key == "none" else f"tier_riser_{key}"
        _wj(model_dir / f"{name}.json", _finished({"textures": dict(RSM.TEX), "elements": els}))
        st = ",".join(f"edge_{d}={'true' if d in edges else 'false'}" for d in ("east", "north", "south", "west"))
        variants[st] = {"model": f"{ns}:block/{name}"}
    _wj(bs_dir / "tier_riser.json", {"variants": variants})
    _wj(model_dir / "aisle_step.json", _finished({"textures": dict(RSM.TEX), "elements": RSM.aisle_step()}))
    _wj(bs_dir / "aisle_step.json", {"variants": {f"facing={d}": {"model": f"{ns}:block/aisle_step", **({"y": y} if y else {})}
                                                 for d, y in FACINGS.items()}})


WALL_SCREENS = {"ops_wall_screen": (3, 2), "ops_wall_screen_large": (5, 3)}
WALL_ROLES = 16                  # the console desk's 15 roles (tower/ConsoleDeskBlock ROLES) and 15, the training room's lesson


def write_wall_screens(assets, ns):
    """The Ops Wall Screen (3 x 2) and the large one (5 x 3): a thin black panel on the wall, its glass to the north (the
    body against the wall to the south). What it shows is drawn live over the glass (client/RoomKitDisplayRenderer:
    its role's three pages in turn); the model is the same for every role."""
    tex_dir, model_dir, bs_dir = assets / "textures/block", assets / "models/block", assets / "blockstates"
    a = np.zeros((16, 16, 3), np.uint8)
    for y in range(16):
        a[y, :] = (8 + y // 5, 10 + y // 5, 12 + y // 5)
    Image.fromarray(a, "RGB").save(tex_dir / "ows_glass.png")
    tex = {"glass": f"{ns}:block/ows_glass", "body": "jugcraft:block/pi_cap_black", "particle": "jugcraft:block/pi_cap_black"}
    for name, (w, h) in WALL_SCREENS.items():
        variants = {}
        for r in range(h):
            for c in range(w):
                part = r * w + c
                # the glass to the north, the body behind it; the outer edges a little darker (the bezel's sides)
                el = [{"from": [0, 0, 14.6], "to": [16, 16, 16], "faces": {
                    "north": {"texture": "#glass", "uv": [0, 0, 16, 16]}, "south": {"texture": "#body"},
                    "east": {"texture": "#body"}, "west": {"texture": "#body"}, "up": {"texture": "#body"}, "down": {"texture": "#body"}}}]
                _wj(model_dir / f"{name}_{part}.json", {"textures": tex, "elements": el})
                for role in range(WALL_ROLES):
                    for f, y in FACINGS.items():
                        variants[f"facing={f},part={part},role={role}"] = {"model": f"{ns}:block/{name}_{part}", **({"y": y} if y else {})}
        _wj(bs_dir / f"{name}.json", {"variants": variants})


def write(assets, ns="jugcraft", pages=None):
    for d in ("textures/block", "models/block", "blockstates"):
        (assets / d).mkdir(parents=True, exist_ok=True)
    write_sim(assets, ns)
    write_brief_sign(assets, ns)
    write_tiers(assets, ns)
    write_wall_screens(assets, ns)
    return []


if __name__ == "__main__":
    write(Path(__file__).resolve().parent.parent / "src/main/resources/assets/jugcraft")
