"""Offline previews of box models (generator element tuples): every box drawn as its six faces, back faces culled,
sorted far to near, in a three-quarter orthographic view, each face flat-filled with its texture's average colour and
lit by its normal. A review aid, not what the game draws (docs/ART_DIRECTION.md asks for culled quads: a hole left open
shows here as the inside of the box behind it).
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

import model_writer

ROOT = Path(__file__).resolve().parents[1]
TEX = ROOT / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures" / "block"
COLORS = {}

FACE_CORNERS = {
    "down": ((0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)),
    "up": ((0, 1, 1), (1, 1, 1), (1, 1, 0), (0, 1, 0)),
    "north": ((1, 0, 0), (0, 0, 0), (0, 1, 0), (1, 1, 0)),
    "south": ((0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)),
    "west": ((0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)),
    "east": ((1, 0, 1), (1, 0, 0), (1, 1, 0), (1, 1, 1)),
}


def color_of(name, draw=None):
    """The average colour of a texture: the committed PNG, or `draw(name)` (a PIL image) when it is not written yet."""
    name = name.rstrip("!")
    if name not in COLORS:
        path = TEX / f"{name}.png"
        img = Image.open(path) if path.exists() else draw(name)
        COLORS[name] = img.convert("RGB").resize((1, 1), Image.BOX).getpixel((0, 0))
    return COLORS[name]


def rotate(p, rotation):
    """A point turned by (axis, angle, origin), or moved by ("move", (dx, dy, dz))."""
    if not rotation:
        return p
    if rotation[0] == "move":
        return [p[k] + rotation[1][k] for k in range(3)]
    axis, angle, origin = rotation[:3]
    a = math.radians(angle)
    i = "xyz".index(axis)
    j, k = [(1, 2), (2, 0), (0, 1)][i]
    out = list(p)
    u, v = p[j] - origin[j], p[k] - origin[k]
    out[j] = origin[j] + u * math.cos(a) - v * math.sin(a)
    out[k] = origin[k] + u * math.sin(a) + v * math.cos(a)
    return out


def faces_of(elements, draw=None):
    out = []
    for item in elements:
        frm, to, texture, options = model_writer.unpack(item)
        rotation = options.get("rotation")
        # "rotations": further turns applied after the element's own, outermost last (a part posed at its joints).
        turns = ([rotation] if rotation else []) + list(options.get("rotations", ()))
        for face, corners in FACE_CORNERS.items():
            tex = model_writer._face_texture(texture, face)
            if tex is None:
                continue
            pts = []
            for c in corners:
                p = [frm[i] + c[i] * (to[i] - frm[i]) for i in range(3)]
                for turn in turns:
                    p = rotate(p, turn)
                pts.append(p)
            out.append((pts, color_of(tex, draw)))
    return out


def view(p, yaw=35.0, pitch=28.0):
    """Orthographic three-quarter view: returns (sx, sy, depth)."""
    ya, pa = math.radians(yaw), math.radians(pitch)
    x, y, z = p
    # The world turned by yaw about y, the camera then looking down -z with x to its right (a true view, not a mirror).
    xr = x * math.cos(ya) + z * math.sin(ya)
    zr = -x * math.sin(ya) + z * math.cos(ya)
    yr = y * math.cos(pa) - zr * math.sin(pa)
    depth = y * math.sin(pa) + zr * math.cos(pa)
    return xr, -yr, depth


def render(elements, scale=4.0, margin=12, yaw=35.0, pitch=28.0, draw=None):
    faces = []
    for pts, color in faces_of(elements, draw):
        screen = [view(p, yaw, pitch) for p in pts]
        area = 0.0
        for i in range(4):
            x0, y0, _ = screen[i]
            x1, y1, _ = screen[(i + 1) % 4]
            area += x0 * y1 - x1 * y0
        if area >= 0:
            continue  # faces away
        a = [pts[1][i] - pts[0][i] for i in range(3)]
        b = [pts[3][i] - pts[0][i] for i in range(3)]
        n = [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
        length = math.sqrt(sum(c * c for c in n)) or 1
        n = [c / length for c in n]
        light = max(0.0, n[0] * -0.45 + n[1] * 0.8 + n[2] * -0.4)
        shade = 0.55 + 0.45 * light
        depth = sum(s[2] for s in screen) / 4
        faces.append((depth, [(s[0], s[1]) for s in screen], tuple(min(255, int(c * shade)) for c in color)))
    faces.sort(key=lambda f: f[0])
    xs = [x for _, poly, _ in faces for x, _ in poly]
    ys = [y for _, poly, _ in faces for _, y in poly]
    x0, y0 = min(xs), min(ys)
    img = Image.new("RGB", (int((max(xs) - x0) * scale) + 2 * margin, int((max(ys) - y0) * scale) + 2 * margin), (236, 238, 242))
    out = ImageDraw.Draw(img)
    for _, poly, color in faces:
        out.polygon([((x - x0) * scale + margin, (y - y0) * scale + margin) for x, y in poly], fill=color,
                    outline=tuple(max(0, c - 28) for c in color))
    return img
