"""Approximate isometric previews of the crop growth stages and an item strip for docs/images (requires Pillow).

Run from the repository root:  python3 tools/render_agriculture.py
Draws the generated crop textures on the vanilla crop model's four planes. The soil is a
simple placeholder texture drawn here. These are previews, not game screenshots.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
TEX = ROOT / "src/main/resources/assets/jugcraft/textures"
ASSETS = ROOT / "src/main/resources/assets/jugcraft"

_cache = {}


def tex(name):
    if name not in _cache:
        if name.startswith("gen:"):
            _cache[name] = GEN[name[4:]]()
        else:
            _cache[name] = Image.open(TEX / "block" / f"{name}.png").convert("RGBA")
    return _cache[name]


def noise_tex(palette, seed, pattern=None):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = rng.choice(palette)
            if pattern:
                c = pattern(x, y, c, rng)
            img.putpixel((x, y), c + (255,))
    return img


SOIL = [(84, 56, 34), (96, 64, 38), (74, 49, 30), (104, 71, 44)]
WET = [(62, 40, 24), (70, 46, 28), (56, 36, 22), (78, 52, 32)]
DIRT = [(120, 86, 58), (108, 76, 50), (132, 96, 66)]


def farmland_top():
    return noise_tex(WET, 5, lambda x, y, c, r: tuple(max(0, v - 14) for v in c) if y % 4 == 0 else c)


GEN = {
    "farmland_top": farmland_top,
    "soil_side": lambda: noise_tex(DIRT, 6),
}

COS = math.cos(math.radians(30))
SIN = math.sin(math.radians(30))


class Scene:
    def __init__(self, scale=48, flat=0.5, tall=1.0):
        self.s = scale
        self.flat = flat  # how much horizontal depth shows (0.5 = classic isometric)
        self.tall = tall  # vertical scale
        self.quads = []  # (depth, texture, p0, u_vec, v_vec, shade)

    def project(self, x, y, z):
        return ((x - z) * COS * self.s, ((x + z) * self.flat - y * self.tall) * self.s)

    def quad(self, texture, origin, u_axis, v_axis, shade=1.0, mirror=False):
        """origin: world point of texture (0,0) (its top-left); u_axis/v_axis: world vectors spanning 16 texels."""
        cx = origin[0] + (u_axis[0] + v_axis[0]) / 2
        cy = origin[1] + (u_axis[1] + v_axis[1]) / 2
        cz = origin[2] + (u_axis[2] + v_axis[2]) / 2
        depth = cx + cz + cy * 0.01
        self.quads.append((depth, texture, origin, u_axis, v_axis, shade, mirror))

    def cube(self, x, y, z, top, side, height=1.0):
        self.quad(top, (x, y + height, z), (1, 0, 0), (0, 0, 1), 1.0)
        self.quad(side, (x, y + height, z + 1), (1, 0, 0), (0, -height, 0), 0.8)
        self.quad(side, (x + 1, y + height, z + 1), (0, 0, -1), (0, -height, 0), 0.65)

    def crop(self, x, y, z, texture, lower=1 / 16):
        y0 = y - lower
        for off in (4 / 16, 12 / 16):
            self.quad(texture, (x + off, y0 + 1, z), (0, 0, 1), (0, -1, 0), 0.85)
            self.quad(texture, (x, y0 + 1, z + off), (1, 0, 0), (0, -1, 0), 0.95)

    def render(self, background=(178, 206, 236), pad=40):
        pts = []
        for _, _, o, u, v, _, _ in self.quads:
            for k in ((0, 0), (1, 0), (0, 1), (1, 1)):
                pts.append(self.project(o[0] + u[0] * k[0] + v[0] * k[1], o[1] + u[1] * k[0] + v[1] * k[1],
                                        o[2] + u[2] * k[0] + v[2] * k[1]))
        minx = min(p[0] for p in pts) - pad
        miny = min(p[1] for p in pts) - pad
        maxx = max(p[0] for p in pts) + pad
        maxy = max(p[1] for p in pts) + pad
        W, H = int(maxx - minx), int(maxy - miny)
        canvas = Image.new("RGBA", (W, H), background + (255,))
        for depth, texture, o, u, v, shade, mirror in sorted(self.quads, key=lambda q: q[0]):
            img = tex(texture)
            if shade != 1.0:
                r, g, b, a = img.split()
                img = Image.merge("RGBA", (r.point(lambda c: int(c * shade)), g.point(lambda c: int(c * shade)),
                                           b.point(lambda c: int(c * shade)), a))
            p0 = self.project(*o)
            pu = self.project(o[0] + u[0], o[1] + u[1], o[2] + u[2])
            pv = self.project(o[0] + v[0], o[1] + v[1], o[2] + v[2])
            U = ((pu[0] - p0[0]) / 16, (pu[1] - p0[1]) / 16)
            V = ((pv[0] - p0[0]) / 16, (pv[1] - p0[1]) / 16)
            corners = [p0, pu, pv, (pu[0] + pv[0] - p0[0], pu[1] + pv[1] - p0[1])]
            bx0 = int(math.floor(min(c[0] for c in corners) - minx))
            by0 = int(math.floor(min(c[1] for c in corners) - miny))
            bx1 = int(math.ceil(max(c[0] for c in corners) - minx)) + 1
            by1 = int(math.ceil(max(c[1] for c in corners) - miny)) + 1
            det = U[0] * V[1] - U[1] * V[0]
            if abs(det) < 1e-9:
                continue
            # screen = p0 + u*U + v*V  ->  (u, v) = inverse
            ia, ib = V[1] / det, -V[0] / det
            ic, id_ = -U[1] / det, U[0] / det
            ox = bx0 + minx - p0[0]
            oy = by0 + miny - p0[1]
            data = (ia, ib, ia * ox + ib * oy, ic, id_, ic * ox + id_ * oy)
            if mirror:
                img = img.transpose(Image.FLIP_LEFT_RIGHT)
            part = img.transform((bx1 - bx0, by1 - by0), Image.AFFINE, data, resample=Image.NEAREST, fillcolor=(0, 0, 0, 0))
            canvas.alpha_composite(part, (bx0, by0))
        return canvas


def tall_textures(block, age, heights):
    info = json.load(open(ASSETS / "blockstates" / f"{block}.json"))["variants"]
    return [info[f"age={age},section={s}"]["model"].split("/")[-1] for s in range(heights[age])]


def crop_texture(block, age):
    info = json.load(open(ASSETS / "blockstates" / f"{block}.json"))["variants"]
    return info[f"age={age}"]["model"].split("/")[-1]


CORN_H = [1, 1, 1, 2, 2, 3, 3, 3]
SUN_H = [1, 1, 1, 2, 2, 2, 2, 2]


def stages(path, block, heights, tall=True):
    sc = Scene(48)
    for age in range(8):
        x = age * 1.6
        if tall:
            sc.cube(x, -1, 0, "gen:farmland_top", "gen:soil_side", height=15 / 16)
            for s, t in enumerate(tall_textures(block, age, heights)):
                sc.crop(x, s, 0, t)
        else:
            sc.cube(x, -1, 0, "gen:farmland_top", "gen:soil_side", height=15 / 16)
            sc.crop(x, 0, 0, crop_texture(block, age))
    sc.render().save(path)


def save(image, path):
    """Palette PNGs keep the previews small in the repository."""
    image.convert("RGB").quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(path, optimize=True)


def item_strip(path):
    names = ["corn", "corn_kernels", "roasted_corn", "popcorn", "sunflower_seeds", "roasted_sunflower_seeds", "beans",
             "sweet_potato", "baked_sweet_potato", "flax", "flax_seeds", "three_sisters_stew", "flint_sickle", "bronze_sickle"]
    size = 64
    strip = Image.new("RGBA", (len(names) * (size + 8) + 8, size + 16), (178, 206, 236, 255))
    for i, name in enumerate(names):
        icon = Image.open(TEX / "item" / f"{name}.png").convert("RGBA").resize((size, size), Image.NEAREST)
        strip.alpha_composite(icon, (8 + i * (size + 8), 8))
    save(strip, path)


if __name__ == "__main__":
    out = ROOT / "docs" / "images"
    stages(out / "corn_stages.png", "corn_crop", CORN_H)
    save(Image.open(out / "corn_stages.png"), out / "corn_stages.png")
    stages(out / "sunflower_stages.png", "sunflower_crop", SUN_H)
    save(Image.open(out / "sunflower_stages.png"), out / "sunflower_stages.png")
    item_strip(out / "agriculture_items.png")
