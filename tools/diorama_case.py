"""The Tower Diorama Case (Justin, 27 October 2026: "you can make the diorama its own little multi-block structure. It
doesn't need to be like these hologram tables"; option A of the stand sheet, approved): a 3 x 3 showroom plinth for the
hall's tower diorama, four blocks high.

A white lacquered body on a recessed black kick (a shadow gap at the floor), a brushed steel lip round its top edge, a
graphite tray the model stands on, and over it a clear acrylic cover on slim black corner frames with a warm light strip
under its top on two sides. The model itself is client/TowerDiorama's (tools/tower_diorama.py), drawn on the tray by the
case's anchor (client/RoomKitDisplayRenderer, display "diorama"); everything else is these block models.

A room kit piece (tools/room_kit.py): one item places all 36 cells, the anchor the plinth's middle cell (part 0). Facing
north; the case is the same from every side.
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image

import model_writer as MW
from roof_plant_art import B, painted

NAME = "tower_diorama_case"
# the anchor first, then the rest of the 3 x 3 x 4 (dx, dy, dz as the piece faces north)
CASE_CELLS = [(0, 0, 0)] + [(dx, dy, dz) for dy in range(4) for dz in (-1, 0, 1) for dx in (-1, 0, 1) if (dx, dy, dz) != (0, 0, 0)]
CELLS = {NAME: CASE_CELLS}
DISPLAY = {NAME: "diorama"}
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

# px over the anchor's floor and across the 3 x 3 (the anchor block is 0..16)
E = 1.0                               # the body's inset from the footprint's edge
X0, X1 = -16 + E, 32 - E
KICK = 1.1                            # the black kick under it, set back
BODY_TOP = 13.8
LIP = 0.5
TRAY_TOP = 14.6                       # the model stands here (client/RoomKitDisplayRenderer.DIORAMA_LIFT: (14.6 - 13) / 16)
COVER_IN = 0.15                       # the cover stands this far in from the body's edge
COVER_TOP = TRAY_TOP + 41.6           # 2.6 blocks of clear height (the model's tower reaches 2.5 over its base)
F = 0.6                               # the cover's black frame (square aluminium)
PANE = 0.25                           # the acrylic's thickness

TEX = 32


def _rng(seed):
    return np.random.default_rng(seed)


def lacquer():
    """White satin lacquer: nearly flat, a faint orange peel."""
    r = _rng(901)
    a = np.zeros((TEX, TEX, 3)) + np.array([232, 231, 226], float) + r.normal(0, 1.4, (TEX, TEX, 1))
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB")


def brushed():
    """Brushed stainless: fine streaks along one way."""
    r = _rng(902)
    a = np.zeros((TEX, TEX, 3)) + 178.0
    a += np.repeat(r.normal(0, 7, (TEX, 1, 1)), TEX, axis=1)
    a += r.normal(0, 2, (TEX, TEX, 1))
    a[..., 2] += 6
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB")


def acrylic():
    """Clear acrylic: a faint cool tint, see-through, a soft sheen across it."""
    a = np.zeros((TEX, TEX, 4))
    a[..., 0], a[..., 1], a[..., 2] = 214, 232, 240
    y = np.linspace(0, 1, TEX)[:, None]
    x = np.linspace(0, 1, TEX)[None, :]
    sheen = np.exp(-((x + y - 0.9) ** 2) / 0.01)
    a[..., 3] = 34 + 30 * sheen
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def led():
    a = np.zeros((TEX, TEX, 3)) + np.array([255, 222, 168], float)
    return Image.fromarray(a.astype(np.uint8), "RGB")


MATERIALS = {
    "dc_lacquer": lacquer,
    "dc_black": lambda: painted((26, 27, 29), 903, 0.8, 0.2),
    "dc_steel": brushed,
    "dc_graphite": lambda: painted((52, 54, 57), 904, 1.0, 0.2),
    "dc_acrylic": acrylic,
    "dc_led": led,
}
EMISSIVE = {"dc_led"}


def case():
    """The case's boxes in structure space (px; the anchor block 0..16, the case -16..32 across)."""
    out = []
    k = KICK + 1.0
    out.append(B((X0 + k, 0.0, X0 + k), (X1 - k, KICK, X1 - k), "dc_black"))                       # the kick, set back
    out.append(B((X0, KICK, X0), (X1, BODY_TOP, X1), {"*": "dc_lacquer", "down": "dc_black"}))      # the body
    out.append(B((X0 - 0.15, BODY_TOP, X0 - 0.15), (X1 + 0.15, BODY_TOP + LIP, X1 + 0.15), "dc_steel"))   # the lip
    # the cover: corner posts, the top frame, the panes between them, the light strips under the top
    c0, c1 = X0 + COVER_IN, X1 - COVER_IN
    m = F / 2 - PANE / 2
    # the tray inside the panes (clear of them, the model's base clear of it: the model is -14.16..30.16)
    t0, t1 = c0 + m + PANE + 0.125, c1 - m - PANE - 0.125
    out.append(B((t0, BODY_TOP + LIP, t0), (t1, TRAY_TOP, t1), "dc_graphite"))                     # the tray
    y0, y1 = BODY_TOP + LIP, COVER_TOP
    for cx in (c0, c1 - F):
        for cz in (c0, c1 - F):
            out.append(B((cx, y0, cz), (cx + F, y1, cz + F), "dc_black"))
    out += [B((c0 + F, y1 - F, c0), (c1 - F, y1, c0 + F), "dc_black"), B((c0 + F, y1 - F, c1 - F), (c1 - F, y1, c1), "dc_black"),
            B((c0, y1 - F, c0 + F), (c0 + F, y1, c1 - F), "dc_black"), B((c1 - F, y1 - F, c0 + F), (c1, y1, c1 - F), "dc_black")]
    # (the panes stand in the frame's middle plane, between the posts; the top pane inside the top frame)
    out += [B((c0 + F, y0, c0 + m), (c1 - F, y1 - F, c0 + m + PANE), "dc_acrylic"),
            B((c0 + F, y0, c1 - m - PANE), (c1 - F, y1 - F, c1 - m), "dc_acrylic"),
            B((c0 + m, y0, c0 + F), (c0 + m + PANE, y1 - F, c1 - F), "dc_acrylic"),
            B((c1 - m - PANE, y0, c0 + F), (c1 - m, y1 - F, c1 - F), "dc_acrylic"),
            B((c0 + F, y1 - F + m, c0 + F), (c1 - F, y1 - F + m + PANE, c1 - F), "dc_acrylic")]
    # the light strips: an aluminium channel under the top pane along two sides, its warm LED face down
    under = y1 - F + m
    for z0 in (c0 + F + 0.2, c1 - F - 1.2):
        out.append(B((c0 + F + 1.0, under - 0.6, z0), (c1 - F - 1.0, under, z0 + 1.0), "dc_black"))
        out.append(B((c0 + F + 1.2, under - 0.75, z0 + 0.15), (c1 - F - 1.2, under - 0.6, z0 + 0.85), "dc_led"))
    return out


# ------------------------------------------------------------------ writing

def _finish(elements):
    elements = [e for e in elements if e.get("faces")]
    for e in elements:
        names = {spec["texture"].lstrip("#") for spec in e["faces"].values()}
        if names and names <= EMISSIVE:
            e["light_emission"] = 15
            e["shade"] = False
    return elements


def _textures(elements, ns):
    names = sorted({spec["texture"].lstrip("#") for e in elements for spec in e.get("faces", {}).values()})
    # (the acrylic's texture is part see-through: its quads go in the translucent layer by themselves)
    out = {n: f"{ns}:block/{n}" for n in names}
    out["particle"] = f"{ns}:block/dc_lacquer"
    return out


def write(assets, ns="jugcraft"):
    """The textures, the 36 part models and the blockstate."""
    assets = Path(assets)
    for d in ("textures/block", "models/block", "blockstates"):
        (assets / d).mkdir(parents=True, exist_ok=True)
    for name, make in MATERIALS.items():
        make().save(assets / f"textures/block/{name}.png")
    variants = {}
    for i, part in enumerate(MW.split_model(NAME, case(), [tuple(c) for c in CASE_CELLS])):
        part = _finish(part)
        model = {"parent": "minecraft:block/block", "textures": _textures(part, ns), "elements": part}
        (assets / f"models/block/{NAME}_{i}.json").write_text(json.dumps(model, indent=1) + "\n")
        for f, yr in FACINGS.items():
            variants[f"facing={f},part={i}"] = {"model": f"{ns}:block/{NAME}_{i}", **({"y": yr} if yr else {})}
    (assets / f"blockstates/{NAME}.json").write_text(json.dumps({"variants": variants}, indent=1) + "\n")
    return [NAME]
