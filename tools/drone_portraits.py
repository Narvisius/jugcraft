"""Portraits of the 9 drone tiers, rendered from the mod's own drone models.

The geometry below is a line-by-line port of src/client/java/.../client/DroneModel.java (the in-game
models), drawn with the mod's own entity texture (textures/entity/drone_depot.png) by a small software
rasteriser, so the pictures match what the game draws. `python tools/drone_portraits.py` writes
build/drones/drone_t<n>.png and build/drones/drone_tiers.png (a sheet with names and stats).
"""
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import drones

ROOT = Path(__file__).resolve().parent.parent
TEXTURE = np.asarray(Image.open(ROOT / "src/main/resources/assets/jugcraft/textures/entity/drone_depot.png").convert("RGBA"),
                     dtype=np.float32) / 255.0


def region(col, row):
    return (col * 16, row * 16, (col + 1) * 16, (row + 1) * 16)


BODY = [region(c, 0) for c in range(8)] + [region(0, 1)]
ROTOR, CRATE, HATCH, LIFT, ARM, DUCT, SHAFT = (region(c, 1) for c in range(1, 8))
WHITE, ENVELOPE, WING, COIL, FIN, GLASS, GONDOLA, BIG_ROTOR = (region(c, 2) for c in range(8))


def argb(v):
    return ((v >> 16) & 255) / 255, ((v >> 8) & 255) / 255, (v & 255) / 255


class Geometry:
    def __init__(self):
        self.m = np.eye(4)
        self.stack = []
        self.color = (1.0, 1.0, 1.0)
        self.glow = False
        self.quads = []  # (4x3 points, normal, uv region, colour, glow)

    def push(self):
        self.stack.append(self.m.copy())

    def pop(self):
        self.m = self.stack.pop()

    def translate(self, x, y, z):
        t = np.eye(4)
        t[:3, 3] = (x, y, z)
        self.m = self.m @ t
        return self

    def yaw(self, a):
        c, s = math.cos(a), math.sin(a)
        r = np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]])
        self.m = self.m @ r
        return self

    def roll(self, a):
        c, s = math.cos(a), math.sin(a)
        r = np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])
        self.m = self.m @ r
        return self

    def pitch(self, a):
        c, s = math.cos(a), math.sin(a)
        r = np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]])
        self.m = self.m @ r
        return self

    def scale(self, s):
        self.m = self.m @ np.diag([s, s, s, 1])
        return self

    def quad(self, pts, normal, uv):
        p = np.array([list(q) + [1] for q in pts]) @ self.m.T
        n = self.m[:3, :3] @ np.array(normal)
        n = n / (np.linalg.norm(n) or 1)
        self.quads.append((p[:, :3], n, uv, self.color, self.glow))

    def box(self, x0, y0, z0, x1, y1, z1, uv):
        a, b = (x0, y0, z0), (x1, y1, z1)
        ax, ay, az = a
        bx, by, bz = b
        self.quad([(ax, by, az), (ax, by, bz), (bx, by, bz), (bx, by, az)], (0, 1, 0), uv)
        self.quad([(ax, ay, bz), (ax, ay, az), (bx, ay, az), (bx, ay, bz)], (0, -1, 0), uv)
        self.quad([(ax, ay, az), (ax, by, az), (bx, by, az), (bx, ay, az)], (0, 0, -1), uv)
        self.quad([(bx, ay, bz), (bx, by, bz), (ax, by, bz), (ax, ay, bz)], (0, 0, 1), uv)
        self.quad([(ax, ay, bz), (ax, by, bz), (ax, by, az), (ax, ay, az)], (-1, 0, 0), uv)
        self.quad([(bx, ay, az), (bx, by, az), (bx, by, bz), (bx, ay, bz)], (1, 0, 0), uv)

    def centred(self, x, y, z, sx, sy, sz, uv):
        self.box(x - sx / 2, y - sy / 2, z - sz / 2, x + sx / 2, y + sy / 2, z + sz / 2, uv)

    def disc(self, x, y, z, r, uv):
        self.quad([(x - r, y, z - r), (x - r, y, z + r), (x + r, y, z + r), (x + r, y, z - r)], (0, 1, 0), uv)


def rotor(g, x, y, z, radius, angle, uv):
    g.push()
    g.translate(x, y, z).yaw(angle)
    g.disc(0, 0, 0, radius, uv)
    g.pop()


def duct(g, x, y, z, r, h, wall):
    g.box(x - r, y, z - r, x + r, y + h, z - r + wall, DUCT)
    g.box(x - r, y, z + r - wall, x + r, y + h, z + r, DUCT)
    g.box(x - r, y, z - r, x - r + wall, y + h, z + r, DUCT)
    g.box(x + r - wall, y, z - r, x + r, y + h, z + r, DUCT)


def glow(g, colour, part):
    g.color, g.glow = argb(colour), True
    part()
    g.color, g.glow = (1.0, 1.0, 1.0), False


def multirotor(g, body, arms, s, spin, dome):
    g.push()
    g.scale(s)
    g.centred(0, 0.115, 0, 0.30, 0.11, 0.38, body)
    g.box(-0.13, 0, -0.13, -0.10, 0.06, 0.13, ARM)
    g.box(0.10, 0, -0.13, 0.13, 0.06, 0.13, ARM)
    if dome:
        g.box(-0.06, 0.02, 0.02, 0.06, 0.07, 0.14, SHAFT)
    glow(g, 0xFF2E6F80 if spin == 0 else 0xFF6FE8FF, lambda: g.box(-0.03, 0.10, 0.19, 0.03, 0.14, 0.21, WHITE))
    reach = 0.36 if arms == 8 else 0.34
    blade = 0.13 if arms == 8 else 0.15
    for k in range(arms):
        angle = 2 * math.pi * k / arms + (math.pi / 4 if arms == 4 else 0)
        g.push()
        g.yaw(angle)
        g.box(-0.018, 0.118, 0, 0.018, 0.142, reach, ARM)
        g.box(-0.03, 0.11, reach - 0.03, 0.03, 0.165, reach + 0.03, ARM)
        rotor(g, 0, 0.17, reach, blade, spin * (1 if k % 2 == 0 else -1) + k, ROTOR)
        g.pop()
    g.pop()


def ducted_runner(g, body, spin):
    g.centred(0, 0.26, 0, 0.62, 0.28, 0.95, body)
    g.box(-0.3, 0, -0.3, -0.24, 0.12, 0.3, ARM)
    g.box(0.24, 0, -0.3, 0.3, 0.12, 0.3, ARM)
    glow(g, 0xFF2E6F80 if spin == 0 else 0xFF6FE8FF, lambda: g.box(-0.08, 0.24, 0.47, 0.08, 0.32, 0.5, WHITE))
    for k in range(4):
        angle = math.pi / 4 + k * math.pi / 2
        fx, fz = math.sin(angle) * 0.62, math.cos(angle) * 0.62
        g.push()
        g.yaw(angle)
        g.box(-0.04, 0.24, 0, 0.04, 0.3, 0.45, ARM)
        g.pop()
        duct(g, fx, 0.18, fz, 0.3, 0.2, 0.05)
        rotor(g, fx, 0.27, fz, 0.26, spin * (1 if k % 2 == 0 else -1) + k, ROTOR)


def tiltrotor(g, body, spin, cruising):
    g.centred(0, 0.36, 0, 0.44, 0.4, 1.8, body)
    g.centred(0, 0.34, 0.98, 0.3, 0.3, 0.2, GLASS)
    g.centred(0, 0.62, -0.95, 0.06, 0.45, 0.35, FIN)
    g.centred(0, 0.86, -0.95, 0.9, 0.05, 0.3, FIN)
    g.centred(0, 0.62, 0.1, 2.4, 0.07, 0.42, WING)
    g.box(-0.2, 0, 0.5, -0.14, 0.18, 0.56, ARM)
    g.box(0.14, 0, 0.5, 0.2, 0.18, 0.56, ARM)
    g.box(-0.03, 0, -0.6, 0.03, 0.18, -0.54, ARM)
    for side in (-1, 1):
        g.push()
        g.translate(side * 1.2, 0.62, 0.1).pitch(cruising * math.pi / 2)
        g.centred(0, 0.1, 0, 0.16, 0.36, 0.16, body)
        rotor(g, 0, 0.3, 0, 0.62, spin * side * 1.4, BIG_ROTOR)
        g.pop()


def tandem(g, body, spin):
    g.centred(0, 0.46, 0, 0.72, 0.62, 2.3, body)
    g.centred(0, 0.5, 1.2, 0.5, 0.4, 0.14, GLASS)
    g.centred(0, 0.35, -1.18, 0.6, 0.35, 0.08, FIN)
    g.centred(0, 0.9, 0.95, 0.3, 0.2, 0.3, body)
    g.centred(0, 1.05, -0.95, 0.4, 0.55, 0.45, body)
    for wz in (0.8, -0.8):
        g.centred(-0.32, 0.08, wz, 0.1, 0.16, 0.16, ARM)
        g.centred(0.32, 0.08, wz, 0.1, 0.16, 0.16, ARM)
    rotor(g, 0, 1.02, 0.95, 0.85, spin * 1.2, BIG_ROTOR)
    rotor(g, 0, 1.34, -0.95, 0.85, -spin * 1.2 + 0.5, BIG_ROTOR)


def aerostat(g, body, spin):
    g.centred(0, 1.55, 0, 1.7, 1.25, 4.2, ENVELOPE)
    g.centred(0, 1.55, 0, 2.1, 0.9, 3.5, ENVELOPE)
    g.centred(0, 1.55, 0.1, 1.2, 1.6, 3.3, ENVELOPE)
    g.centred(0, 2.55, -1.75, 0.08, 0.75, 0.7, FIN)
    g.centred(0, 1.55, -1.85, 2.9, 0.08, 0.55, FIN)
    g.centred(0, 0.55, 0.2, 0.62, 0.42, 1.3, GONDOLA)
    g.centred(0, 0.55, 0.86, 0.5, 0.3, 0.05, GLASS)
    g.box(-0.04, 0.76, -0.2, 0.04, 1.0, 0.6, ARM)
    for sx in (-1, 1):
        for sz in (-1, 1):
            px, pz = sx * 1.45, sz * 1.1
            g.box(min(0, px), 1.1, pz - 0.04, max(0, px), 1.16, pz + 0.04, ARM)
            duct(g, px, 0.98, pz, 0.3, 0.26, 0.05)
            rotor(g, px, 1.1, pz, 0.26, spin * 1.3 * sx * sz, ROTOR)
    glow(g, 0xFF6FE8FF, lambda: g.box(-0.9, 1.52, 2.0, 0.9, 1.58, 2.08, WHITE))
    g.box(-0.3, 0, -0.3, 0.3, 0.34, 0.3, ARM)


def ion_glider(g, body, spin):
    g.centred(0, 0.42, 0.25, 0.9, 0.32, 2.0, body)
    g.centred(0, 0.58, 0.95, 0.5, 0.12, 0.4, GLASS)
    for side in (-1, 1):
        for i in range(4):
            cx = side * (0.7 + i * 0.45)
            cz = 0.2 - i * 0.22
            chord = 1.5 - i * 0.22
            g.centred(cx, 0.44, cz, 0.47, 0.09, chord, WING)
            lead, trail = cz + chord / 2, cz - chord / 2
            glow(g, 0xFF7FF0FF, lambda: g.centred(cx, 0.5, lead - 0.03, 0.45, 0.03, 0.04, WHITE))
            glow(g, 0xFF3A8AD0, lambda: g.centred(cx, 0.5, trail + 0.03, 0.45, 0.03, 0.04, WHITE))
        g.centred(side * 2.15, 0.62, -0.5, 0.06, 0.4, 0.5, FIN)
        for fx in (1.0, 1.9):
            fz = -0.2 if fx > 1.5 else 0.05
            duct(g, side * fx, 0.38, fz, 0.24, 0.13, 0.04)
            rotor(g, side * fx, 0.47, fz, 0.21, spin * 1.5 * side, ROTOR)
    g.box(-0.5, 0, 0.1, -0.44, 0.26, 0.16, ARM)
    g.box(0.44, 0, 0.1, 0.5, 0.26, 0.16, ARM)
    g.box(-0.03, 0, 0.9, 0.03, 0.26, 0.96, ARM)


def heavy_lifter(g, body, spin):
    half = 1.65
    g.box(-half, 0.85, -half - 0.1, half, 1.05, -half + 0.1, ARM)
    g.box(-half, 0.85, half - 0.1, half, 1.05, half + 0.1, ARM)
    g.box(-half - 0.1, 0.85, -half, -half + 0.1, 1.05, half, ARM)
    g.box(half - 0.1, 0.85, -half, half + 0.1, 1.05, half, ARM)
    g.box(-half, 0.9, -0.08, half, 1.0, 0.08, ARM)
    g.box(-0.08, 0.9, -half, 0.08, 1.0, half, ARM)
    g.centred(0, 0.8, 0, 1.1, 0.6, 1.1, body)
    glow(g, 0xFFB06CFF, lambda: g.centred(0, 1.12, 0, 0.5, 0.06, 0.5, WHITE))
    for sx in (-1, 1):
        for sz in (-1, 1):
            fx, fz = sx * half, sz * half

            def ring():
                g.box(fx - 0.72, 1.12, fz - 0.72, fx + 0.72, 1.16, fz - 0.64, WHITE)
                g.box(fx - 0.72, 1.12, fz + 0.64, fx + 0.72, 1.16, fz + 0.72, WHITE)
                g.box(fx - 0.72, 1.12, fz - 0.72, fx - 0.64, 1.16, fz + 0.72, WHITE)
                g.box(fx + 0.64, 1.12, fz - 0.72, fx + 0.72, 1.16, fz + 0.72, WHITE)
            duct(g, fx, 0.7, fz, 0.72, 0.42, 0.09)
            glow(g, 0xFFB06CFF, ring)
            g.centred(fx, 0.9, fz, 0.2, 0.2, 0.2, COIL)
            rotor(g, fx, 0.92, fz, 0.64, spin * 1.1 * sx * sz, BIG_ROTOR)
            g.box(fx - 0.06, 0, fz - 0.06, fx + 0.06, 0.7, fz + 0.06, ARM)
    g.box(-0.5, 0.3, -0.5, -0.42, 0.5, 0.5, ARM)
    g.box(0.42, 0.3, -0.5, 0.5, 0.5, 0.5, ARM)


def drone(tier, spin=0.0):
    g = Geometry()
    body = BODY[max(0, min(len(BODY) - 1, tier - 1))]
    {2: lambda: multirotor(g, body, 6, 1.0, spin, True), 3: lambda: multirotor(g, body, 8, 1.15, spin, False),
     4: lambda: ducted_runner(g, body, spin), 5: lambda: tiltrotor(g, body, spin, 0.0),
     6: lambda: tandem(g, body, spin), 7: lambda: aerostat(g, body, spin), 8: lambda: ion_glider(g, body, spin),
     9: lambda: heavy_lifter(g, body, spin)}.get(tier, lambda: multirotor(g, body, 4, 1.0, spin, False))()
    return g.quads


# ---------------------------------------------------------------- rasteriser

def render(quads, size=900, yaw_deg=-35, pitch_deg=28, ground=True, texture=None):
    """Orthographic 3/4 view from the front-right and above; z-buffered, textured, simply lit."""
    ss = 2
    W = H = size * ss
    ya, pa = math.radians(yaw_deg), math.radians(pitch_deg)
    rot_y = np.array([[math.cos(ya), 0, math.sin(ya)], [0, 1, 0], [-math.sin(ya), 0, math.cos(ya)]])
    rot_x = np.array([[1, 0, 0], [0, math.cos(pa), -math.sin(pa)], [0, math.sin(pa), math.cos(pa)]])
    view = rot_x @ rot_y
    pts = np.concatenate([q[0] for q in quads])
    v = pts @ view.T
    lo, hi = v.min(axis=0), v.max(axis=0)
    span = max(hi[0] - lo[0], hi[1] - lo[1]) * 1.25
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    k = W / span

    def screen(p):
        q = p @ view.T
        return np.stack([(q[:, 0] - cx) * k + W / 2, H / 2 - (q[:, 1] - cy) * k, q[:, 2]], axis=1)

    colour = np.zeros((H, W, 3), np.float32)
    alpha = np.zeros((H, W), np.float32)
    depth = np.full((H, W), -np.inf, np.float32)  # larger z is nearer the camera
    light = np.array([0.35, 0.85, 0.55])
    light /= np.linalg.norm(light)
    if ground:  # a soft shadow under the drone
        foot = screen(np.array([[0, 0, 0]]))[0]
        r = (hi[0] - lo[0]) * k * 0.42
        yy, xx = np.mgrid[0:H, 0:W]
        d = ((xx - foot[0]) / r) ** 2 + ((yy - foot[1]) / (r * math.sin(pa))) ** 2
        sh = np.clip(1 - d, 0, 1) * 0.55
        alpha = np.maximum(alpha, sh)
    for p3, n, uv, col, is_glow in quads:
        s = screen(p3)
        shade = 1.0 if is_glow else 0.62 + 0.5 * abs(float(n @ light))
        u0, v0, u1, v1 = uv
        uvs = np.array([(u0, v0), (u0, v1), (u1, v1), (u1, v0)], np.float32)
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = s[list(tri)]
            ta, tb, tc = uvs[list(tri)]
            xmin, xmax = int(max(0, min(a[0], b[0], c[0]))), int(min(W - 1, max(a[0], b[0], c[0]) + 1))
            ymin, ymax = int(max(0, min(a[1], b[1], c[1]))), int(min(H - 1, max(a[1], b[1], c[1]) + 1))
            if xmin > xmax or ymin > ymax:
                continue
            yy, xx = np.mgrid[ymin:ymax + 1, xmin:xmax + 1] + 0.5
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            w0 = ((b[1] - c[1]) * (xx - c[0]) + (c[0] - b[0]) * (yy - c[1])) / den
            w1 = ((c[1] - a[1]) * (xx - c[0]) + (a[0] - c[0]) * (yy - c[1])) / den
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not inside.any():
                continue
            z = w0 * a[2] + w1 * b[2] + w2 * c[2]
            tu = (w0 * ta[0] + w1 * tb[0] + w2 * tc[0])
            tv = (w0 * ta[1] + w1 * tb[1] + w2 * tc[1])
            tex = TEXTURE if texture is None else texture
            tx = np.clip(tu.astype(int), 0, tex.shape[1] - 1)
            ty = np.clip(tv.astype(int), 0, tex.shape[0] - 1)
            texel = tex[ty, tx]
            ys, xs = slice(ymin, ymax + 1), slice(xmin, xmax + 1)
            ok = inside & (texel[..., 3] > 0.5) & (z > depth[ys, xs])
            depth[ys, xs] = np.where(ok, z, depth[ys, xs])
            rgb = texel[..., :3] * np.array(col, np.float32) * shade
            colour[ys, xs] = np.where(ok[..., None], rgb, colour[ys, xs])
            alpha[ys, xs] = np.where(ok, 1.0, alpha[ys, xs])
    shadow = np.isneginf(depth)
    out = np.dstack([np.where(shadow[..., None], 0, np.clip(colour, 0, 1)), alpha])
    img = Image.fromarray((out * 255).astype(np.uint8), "RGBA")
    return img.resize((size, size), Image.LANCZOS)


def main():
    out = ROOT / "build" / "drones"
    out.mkdir(parents=True, exist_ok=True)
    F = "/usr/share/fonts/truetype/dejavu/"
    big = ImageFont.truetype(F + "DejaVuSans-Bold.ttf", 36)
    mid = ImageFont.truetype(F + "DejaVuSans-Bold.ttf", 22)
    sm = ImageFont.truetype(F + "DejaVuSans.ttf", 17)
    cell, cols = 560, 3
    sheet = Image.new("RGB", (cell * cols + 40, 100 + 3 * (cell + 150)), (16, 12, 14))
    d = ImageDraw.Draw(sheet)
    d.text((20, 20), "DRONES — ALL 9 TIERS (rendered from the in-game models)", font=big, fill=(220, 80, 64))
    d.text((20, 66), "Each drone is shown at rest, scaled to fit its frame (real sizes in blocks are given). Tower tier N unlocks drone tier N.",
           font=sm, fill=(160, 140, 140))
    sizes = {1: "about 0.8 across", 2: "about 0.8 across", 3: "about 1 across", 4: "about 1.8 across", 5: "2.4 wingspan",
             6: "2.3 long, 2 rotors", 7: "4.2 long", 8: "4.4 wingspan", 9: "4.3 square"}
    for tier in range(1, 10):
        img = render(drone(tier))
        bg = Image.new("RGBA", img.size, (28, 24, 26, 255))
        bg.alpha_composite(img)
        bg.convert("RGB").save(out / f"drone_t{tier}.png")
        info = drones.DRONE_TIERS[tier]
        x = 20 + ((tier - 1) % cols) * cell
        y = 100 + ((tier - 1) // cols) * (cell + 150)
        tile = img.resize((cell - 20, cell - 20), Image.LANCZOS)
        panel = Image.new("RGBA", tile.size, (26, 22, 24, 255))
        panel.alpha_composite(tile)
        sheet.paste(panel.convert("RGB"), (x, y))
        d.text((x + 10, y + cell - 10), f"TIER {tier}", font=mid, fill=(220, 80, 64))
        d.text((x + 110, y + cell - 10), info["display"].replace(" Drone", ""), font=mid, fill=(236, 230, 230))
        d.text((x + 10, y + cell + 22),
               f"{info['size']} · {sizes[tier]} · carries {info['capacity']} blocks/trip · {info['speed']} blocks/s",
               font=sm, fill=(200, 190, 190))
        d.text((x + 10, y + cell + 44), f"upkeep {info['upkeep']} JE/t · "
               + ("craftable now" if info["available"] else "prototype (creative only)"), font=sm, fill=(160, 140, 140))
        if not info["available"]:
            d.text((x + 10, y + cell + 66), "needs " + info["needs"], font=sm, fill=(160, 140, 140))
    sheet.save(out / "drone_tiers.png")
    print(out / "drone_tiers.png")


if __name__ == "__main__":
    main()
