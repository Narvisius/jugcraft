"""Glow textures carry their brightness as opacity.

The game's eyes layer (what the mod's glows are drawn on) lays a picture over the scene by its opacity; since 26.x it
does not add its light to it. A glow painted bright on black with no see-through part therefore draws its black: the
plasma bolt was a black lump in the game (Justin's playtest, 10 November 2026). `carried` turns such a picture into the
same glow as the layer wants it: each texel its hue at full brightness, as opaque as it was bright.

    python3 tools/glow_alpha.py            converts the textures in BLACK_BACKED in place (once: a picture that
                                           already has see-through parts is left alone)

`check(err)` (tools/check_mod_data.py) fails on any texture under textures/entity named as a glow (GLOW_NAMES) that is
opaque all over with a good part of it dark: whatever is added later.
"""
from __future__ import annotations

import fnmatch
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src/main/resources/assets/jugcraft/textures/entity"
# written for added light, by hand or by a tool that no longer exists (a tool that makes one calls `carried` itself:
# tools/power_armor.py's glow sheet)
BLACK_BACKED = ["plasma_glow.png", "plasma_filament.png", "power_armor/armor_glow.png"]
GLOW_NAMES = ["*glow*.png", "*filament*.png", "*_eyes.png", "*flame*.png"]
DARK, MUCH = 40, 0.2             # a texel this dark, and how much of an opaque picture may be


def carried(rgba):
    """An RGBA picture (uint8, or floats 0..1) painted for added light, as uint8 RGBA for the eyes layer."""
    a = np.asarray(rgba)
    if a.dtype != np.uint8:
        a = (np.clip(a, 0, 1) * 255 + 0.5).astype(np.uint8)
    f = a.astype(np.float32) / 255
    top = f[..., :3].max(axis=2)
    out = np.zeros_like(f)
    lit = top > 0
    out[..., :3][lit] = f[..., :3][lit] / top[lit][:, None]
    out[..., 3] = top * f[..., 3]
    return (out * 255 + 0.5).astype(np.uint8)


def black_backed(a):
    """Whether an RGBA picture (uint8) is opaque all over with much of it dark: a glow painted for added light."""
    return bool(a[..., 3].min() == 255 and (a[..., :3].max(axis=2) < DARK).mean() > MUCH)


def check(err):
    for p in sorted(TEXTURES.rglob("*.png")):
        if not any(fnmatch.fnmatch(p.name, g) for g in GLOW_NAMES):
            continue
        with Image.open(p) as im:
            a = np.asarray(im.convert("RGBA"))
        if black_backed(a):
            err(f"glow_alpha: {p.relative_to(TEXTURES)} is a glow painted bright on black with no see-through part: "
                f"the eyes layer draws its black (run tools/glow_alpha.py on it, or carry its brightness as opacity)")


if __name__ == "__main__":
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import keep_same
    for name in BLACK_BACKED:
        p = TEXTURES / name
        with Image.open(p) as im:
            a = np.asarray(im.convert("RGBA"))
        if not black_backed(a):
            print(f"{name}: already carries its brightness as opacity")
            continue
        keep_same.png(Image.fromarray(carried(a), "RGBA"), p, optimize=True)
        print(f"{name}: converted")
