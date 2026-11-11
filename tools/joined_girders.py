"""The Steel Girder joined (work list item 12, "Steel girders that join into one solid beam, straight or raked",
approved by Justin on 23 October 2026 after three takes): a run of girder blocks along one axis reads as one member.

Every girder block is the same wide-flange section (as the approved mockup's columns: 14 px deep, 11.2 px across the
flanges, 1.6 px flanges, a 1.1 px web with its root fillets) in one structural paint whose texture tiles along the
member, so a run of them shows no seams. Where a run ends (the next block along its axis is not a girder of the same
axis) it ends on a plate:
- a column's foot (axis y, its low end) on a base plate the block's full width, four anchor bolts (washer, nut and the
  stud's end) between the flanges;
- a column's head (axis y, its high end) under a cap plate a little wider than the section;
- a beam's ends (axis x or z) on end plates, four bolts between the flanges.
tower/TowerGirderBlock works out `join_lo` / `join_hi` (the run carries on past that end) from its neighbours; the raked
members (the supply cradles' struts) are tools/cradle_struts.py.

Writes the girder's textures (girder_paint, girder_paint_dark, girder_plate, girder_bolt), its 12 block models
(steel_girder_<axis>_<plates>: plates "both", "lo", "hi" or "none"; steel_girder, steel_girder_x and steel_girder_z are
the lone block's) and its blockstate. tools/tower_realism_art.py and tower_realism_runtime.py call models() and
blockstate(), so either way of writing the tower's assets writes these.
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image

import model_writer as mw

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"

# The section in a column's frame (pixels): flanges square to z, the web square to x.
DEPTH = (1.0, 15.0)           # the flanges' outer faces (z)
WIDTH = (2.4, 13.6)           # the flanges' tips (x)
FLANGE = 1.6
WEB = (7.45, 8.55)            # the web (x)
FILLET = 0.55
PLATE = 1.0                   # every plate's thickness
BASE = (0.0, 16.0, 0.0, 16.0)            # the base plate (x0, x1, z0, z1)
CAP = (1.9, 14.1, 0.65, 15.35)           # the cap plate
END = (1.8, 14.2, 0.5, 15.5)             # a beam's end plate
BOLTS = [(x, z) for x in (4.4, 11.6) for z in (5.0, 11.0)]   # between the flanges, either side of the web
PAINT, PAINT_DK, PLATE_TEX, BOLT = "girder_paint", "girder_paint_dark", "girder_plate", "girder_bolt"
AXES = ("y", "x", "z")
PLATES = {"both": (True, True), "lo": (True, False), "hi": (False, True), "none": (False, False)}


def section(lo, hi):
    """The member's boxes in a column's frame from y lo to hi: two flanges, the web, four root fillets. Their ends are
    left out (a plate covers them, or the next block's section carries on), and so are the faces inside one another."""
    (z0, z1), (x0, x1), (w0, w1) = DEPTH, WIDTH, WEB
    ends = ("up", "down")
    out = [((x0, lo, z0), (x1, hi, z0 + FLANGE), PAINT, ends), ((x0, lo, z1 - FLANGE), (x1, hi, z1), PAINT, ends),
           ((w0, lo, z0 + FLANGE), (w1, hi, z1 - FLANGE), PAINT_DK, ends + ("north", "south"))]
    f = FILLET
    for zs, zskip in (((z0 + FLANGE - 0.1, z0 + FLANGE + f), "north"), ((z1 - FLANGE - f, z1 - FLANGE + 0.1), "south")):
        out.append(((w0 - f, lo, zs[0]), (w0 + 0.1, hi, zs[1]), PAINT_DK, ends + ("east", zskip)))
        out.append(((w1 - 0.1, lo, zs[0]), (w1 + f, hi, zs[1]), PAINT_DK, ends + ("west", zskip)))
    return out


def bolt(x, z, y, up):
    """An anchor bolt or a bolt's nut on a plate's face at height y, `up` the way it stands out (+1 or -1): a washer,
    the nut, the stud's end."""
    out = []
    for half, h, tex in ((0.8, 0.3, PLATE_TEX), (0.55, 0.75, BOLT), (0.25, 0.45, BOLT)):
        y0 = y if up > 0 else y - h
        out.append(((x - half, y0, z - half), (x + half, y0 + h, z + half), tex, ("down",) if up > 0 else ("up",)))
        y = y + h * up
    return out


def column(lo_plate, hi_plate):
    """The boxes of one girder block in a column's frame (axis y): plates at the low end (a base plate) and the high
    end (a cap plate) as asked. The section's ends at a plate are its own faces; where it joins on they are left out."""
    y0 = PLATE if lo_plate else 0.0
    y1 = 16.0 - PLATE if hi_plate else 16.0
    out = list(section(y0, y1))
    if lo_plate:
        a, b, c, d = BASE
        out.append(((a, 0.0, c), (b, PLATE, d), PLATE_TEX, ()))
        for x, z in BOLTS:
            out += bolt(x, z, PLATE, +1)
    if hi_plate:
        a, b, c, d = CAP
        out.append(((a, 16.0 - PLATE, c), (b, 16.0, d), PLATE_TEX, ()))
    return out


def beam(lo_plate, hi_plate):
    """A beam's boxes in a column's frame: end plates at both ends as asked, bolts' nuts on their inner faces."""
    y0 = PLATE if lo_plate else 0.0
    y1 = 16.0 - PLATE if hi_plate else 16.0
    out = list(section(y0, y1))
    a, b, c, d = END
    if lo_plate:
        out.append(((a, 0.0, c), (b, PLATE, d), PLATE_TEX, ()))
        for x, z in BOLTS:
            out += bolt(x, z, PLATE, +1)
    if hi_plate:
        out.append(((a, 16.0 - PLATE, c), (b, 16.0, d), PLATE_TEX, ()))
        for x, z in BOLTS:
            out += bolt(x, z, 16.0 - PLATE, -1)
    return out


# A column's frame -> the axis's: (x, y, z) of the result from the column's coordinates (along y, depth z, width x).
# A beam stands its web upright: along z, its depth is y and its width x; along x, its depth y and its width z.
PERM = {"y": (0, 1, 2), "z": (0, 2, 1), "x": (1, 2, 0)}
FACE_MAP = {  # where a column-frame face goes
    "y": {f: f for f in mw.FACES},
    "z": {"down": "north", "up": "south", "north": "down", "south": "up", "east": "east", "west": "west"},
    "x": {"down": "west", "up": "east", "north": "down", "south": "up", "east": "south", "west": "north"},
}


def _turn(axis, boxes):
    p = PERM[axis]
    out = []
    for frm, to, tex, skip in boxes:
        f = tuple(frm[p[k]] for k in range(3))
        t = tuple(to[p[k]] for k in range(3))
        out.append((f, t, tex, tuple(FACE_MAP[axis][s] for s in skip)))
    return out


def boxes(axis, lo_plate, hi_plate):
    b = column(lo_plate, hi_plate) if axis == "y" else beam(lo_plate, hi_plate)
    return _turn(axis, b)


def model(axis, lo_plate, hi_plate):
    elements = [mw.element(f, t, tex, skip=skip) for f, t, tex, skip in boxes(axis, lo_plate, hi_plate)]
    mw.finish_elements(elements)
    return {"parent": "minecraft:block/block", "ambientocclusion": True,
            "textures": {**{n: f"jugcraft:block/{n}" for n in (PAINT, PAINT_DK, PLATE_TEX, BOLT)}, "particle": f"jugcraft:block/{PAINT}"},
            "elements": elements}


def models():
    out = {}
    for axis in AXES:
        for name, (lo, hi) in PLATES.items():
            out[f"steel_girder_{axis}_{name}"] = model(axis, lo, hi)
    out["steel_girder"] = {"parent": "jugcraft:block/steel_girder_y_both"}
    out["steel_girder_x"] = {"parent": "jugcraft:block/steel_girder_x_both"}
    out["steel_girder_z"] = {"parent": "jugcraft:block/steel_girder_z_both"}
    out["steel_girder_horizontal"] = {"parent": "jugcraft:block/steel_girder_x"}
    return out


def blockstate():
    variants = {}
    for axis in AXES:
        for join_hi in (False, True):
            for join_lo in (False, True):
                plates = {(True, True): "both", (True, False): "lo", (False, True): "hi", (False, False): "none"}[(not join_lo, not join_hi)]
                variants[f"axis={axis},join_hi={str(join_hi).lower()},join_lo={str(join_lo).lower()}"] = {
                    "model": f"jugcraft:block/steel_girder_{axis}_{plates}"}
    return {"variants": variants}


def state(axis, join_lo, join_hi):
    return f"jugcraft:steel_girder[axis={axis},join_hi={str(join_hi).lower()},join_lo={str(join_lo).lower()}]"


STEP = {"x": (1, 0, 0), "y": (0, 1, 0), "z": (0, 0, 1)}


def joined(blocks):
    """Every steel girder in {(x, y, z): state} given its join_lo / join_hi from its neighbours (as TowerGirderBlock)."""
    def axis_of(s):
        if s is None or not s.startswith("jugcraft:steel_girder"):
            return None
        return s.split("axis=")[1][0] if "axis=" in s else "y"
    out = dict(blocks)
    for p, s in blocks.items():
        a = axis_of(s)
        if a is None:
            continue
        d = STEP[a]
        lo = axis_of(blocks.get((p[0] - d[0], p[1] - d[1], p[2] - d[2]))) == a
        hi = axis_of(blocks.get((p[0] + d[0], p[1] + d[1], p[2] + d[2]))) == a
        out[p] = state(a, lo, hi)
    return out


# ------------------------------------------------------------------ textures

def _tiling_noise(size, base, amount, seed, streak=0.0):
    """Paint: a fine noise and a faint grain along v, both repeating every tile (so a run of blocks shows no seams)."""
    rng = np.random.default_rng(seed)
    f = np.fft.fft2(rng.normal(0, 1, (size, size)))
    ky = np.fft.fftfreq(size)[:, None]
    kx = np.fft.fftfreq(size)[None, :]
    soft = np.real(np.fft.ifft2(f * np.exp(-(kx ** 2 + ky ** 2) * (size / 3.0) ** 2)))
    soft /= soft.std() + 1e-9
    fine = rng.normal(0, 1, (size, size))
    grain = np.real(np.fft.ifft2(np.fft.fft2(rng.normal(0, 1, (size, size))) * np.exp(-(kx * size / 1.5) ** 2 - (ky * size / 12) ** 2)))
    grain /= grain.std() + 1e-9
    n = soft * 0.55 + fine * 0.35 + grain * streak
    a = np.zeros((size, size, 4), np.uint8)
    for i in range(3):
        a[..., i] = np.clip(base[i] + n * amount, 0, 255)
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def textures():
    return {PAINT: _tiling_noise(64, (74, 79, 86), 4.5, 501, 0.6), PAINT_DK: _tiling_noise(64, (58, 62, 68), 4.0, 502, 0.6),
            PLATE_TEX: _tiling_noise(64, (88, 93, 100), 4.0, 503), BOLT: _tiling_noise(16, (150, 154, 160), 5.0, 504)}


def write(assets=ASSETS):
    assets = Path(assets)
    for name, img in textures().items():
        img.save(assets / "textures/block" / f"{name}.png", optimize=True)
    for name, m in models().items():
        mw.write(assets / "models/block" / f"{name}.json", m)
    mw.write(assets / "blockstates/steel_girder.json", blockstate())


if __name__ == "__main__":
    write()
    print(len(models()), "models")
