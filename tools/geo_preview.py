"""A software preview of a GeckoLib model (requires Pillow and numpy): a .geo.json body as plain dicts, posed by one of
its clips at a given time, textured from its sheet (with its glowmask drawn full bright over it) and drawn from any side,
so a model and its animations can be judged before the game draws them. Development only: nothing in the mod or its
data comes from here.

It follows GeckoLib 5's own maths, so a preview matches the game's pose:
- a geometry's x is mirrored as it loads (a bone's or cube's pivot and a cube's corner go to -x), and so are rotations
  about x and y (a keyframe's or a bone's rotation (x, y, z) in degrees turns by (-x, -y, z));
- a bone turns about its pivot by Z, then Y, then X (as JOML's rotationZYX), after its keyframed position (x mirrored)
  and before its keyframed scale; a bone's keyframed rotation adds to its rest rotation;
- a cube turns about its own pivot the same way;
- box UV lays each face out as Bedrock and Blockbench do, the U direction of every face reversed unless the cube is
  mirrored, and a mirrored cube's east and west faces trade places.

In the game the entity then turns by 180 - yaw about y, so the model's front (-z in the file) faces the way it looks.
Here the camera simply orbits the model: `yaw` 0 looks at its front, 90 at its left side (the file's +x), 180 at its
back. Faces are lit by two fixed lights as entities are. Linear keyframes only (all Jugcraft writes).
"""
import math

import numpy as np
from PIL import Image


def _rot(z, y, x):
    """Rz(z) Ry(y) Rx(x), angles in radians: a column-vector rotation applying x first."""
    cz, sz, cy, sy, cx, sx = math.cos(z), math.sin(z), math.cos(y), math.sin(y), math.cos(x), math.sin(x)
    rz = np.array([[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])
    ry = np.array([[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1]])
    rx = np.array([[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1]])
    return rz @ ry @ rx


def _move(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def _scale(x, y, z):
    return np.diag([x, y, z, 1.0])


def _radians(rotation):
    """A file's rotation (x, y, z) in degrees as GeckoLib turns it: (-x, -y, z) in radians."""
    x, y, z = rotation
    return math.radians(-x), math.radians(-y), math.radians(z)


def sample(keys, time):
    """A channel's value at `time`: its keyframes ({"seconds": [x, y, z]}, or one [x, y, z]) eased linearly."""
    if keys is None:
        return None
    if isinstance(keys, list):
        return [float(v) for v in keys]
    frames = sorted((float(t), [float(v) for v in value]) for t, value in keys.items())
    if time <= frames[0][0]:
        return frames[0][1]
    for (t0, a), (t1, b) in zip(frames, frames[1:]):
        if time <= t1:
            f = 0.0 if t1 == t0 else (time - t0) / (t1 - t0)
            return [a[i] + (b[i] - a[i]) * f for i in range(3)]
    return frames[-1][1]


def pose(animations, clips, time):
    """Each bone's (rotation, position, scale) at `time` in the named clips (later clips win, as a later controller
    does). A clip that does not loop holds its last frame; one that loops wraps."""
    posed = {}
    for name in clips:
        clip = animations["animations"][name]
        length = float(clip.get("animation_length", 0.0)) or 1.0
        t = time % length if clip.get("loop") is True else min(time, length)
        for bone, channels in clip.get("bones", {}).items():
            entry = posed.setdefault(bone, [None, None, None])
            for i, channel in enumerate(("rotation", "position", "scale")):
                value = sample(channels.get(channel), t)
                if value is not None:
                    entry[i] = value
    return posed


# The six faces: GeckoLib's vertex order for each (corners of the cube by (x, y, z) taken from its low or high end),
# and the box-UV region the face reads (u, v offsets in units of the cube's (w, h, d), and its size).
_CORNERS = {
    "west": ((0, 1, 1), (0, 1, 0), (0, 0, 0), (0, 0, 1)),
    "east": ((1, 1, 0), (1, 1, 1), (1, 0, 1), (1, 0, 0)),
    "north": ((0, 1, 0), (1, 1, 0), (1, 0, 0), (0, 0, 0)),
    "south": ((1, 1, 1), (0, 1, 1), (0, 0, 1), (1, 0, 1)),
    "up": ((0, 1, 1), (1, 1, 1), (1, 1, 0), (0, 1, 0)),
    "down": ((0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)),
}


def _uv_region(face, u, v, w, h, d):
    """(u, v, width, height) of a face's box-UV region; the bottom's height is negative (it is read upwards)."""
    return {"west": (u + d + w, v + d, d, h), "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
            "south": (u + d + w + d, v + d, w, h), "up": (u + d, v, w, d), "down": (u + d + w, v + d, w, -d)}[face]


def quads(geo, posed):
    """Every face of the posed model, as (four corners in blocks, four (u, v) in texture pixels)."""
    definition = geo["minecraft:geometry"][0]
    bones = {bone["name"]: bone for bone in definition["bones"]}
    matrices = {}

    def matrix(name):
        if name in matrices:
            return matrices[name]
        bone = bones[name]
        parent = matrix(bone["parent"]) if bone.get("parent") else np.eye(4)
        px, py, pz = bone.get("pivot", (0, 0, 0))
        pivot = np.array([-px, py, pz]) / 16.0
        rx, ry, rz = _radians(bone.get("rotation", (0, 0, 0)))
        rotation, position, scale = posed.get(name, (None, None, None))
        if rotation:
            ax, ay, az = _radians(rotation)
            rx, ry, rz = rx + ax, ry + ay, rz + az
        m = parent
        if position:
            m = m @ _move(-position[0] / 16.0, position[1] / 16.0, position[2] / 16.0)
        m = m @ _move(*pivot) @ _rot(rz, ry, rx)
        if scale:
            m = m @ _scale(*scale)
        m = m @ _move(*-pivot)
        matrices[name] = m
        return m

    out = []
    for bone in definition["bones"]:
        m = matrix(bone["name"])
        for cube in bone.get("cubes", []):
            w, h, d = cube["size"]
            ox, oy, oz = cube["origin"]
            low = np.array([-(ox + w), oy, oz]) / 16.0
            size = np.array([w, h, d]) / 16.0
            inflate = cube.get("inflate", 0.0) / 16.0
            cm = m
            if any(cube.get("rotation", (0, 0, 0))):
                cx, cy, cz = cube.get("pivot", (0, 0, 0))
                pivot = np.array([-cx, cy, cz]) / 16.0
                rx, ry, rz = _radians(cube["rotation"])
                cm = m @ _move(*pivot) @ _rot(rz, ry, rx) @ _move(*-pivot)
            mirror = bool(cube.get("mirror", False))
            u, v = cube["uv"]
            uw, uh, ud = math.floor(w), math.floor(h), math.floor(d)
            for face, corners in _CORNERS.items():
                if (w == 0 and face not in ("west", "east")) or (h == 0 and face not in ("up", "down")) or \
                        (d == 0 and face not in ("north", "south")):
                    continue
                vertex_face = {"west": "east", "east": "west"}.get(face, face) if mirror else face
                points = []
                for corner in _CORNERS[vertex_face]:
                    p = low - inflate + np.array(corner) * (size + 2 * inflate)
                    points.append((cm @ np.append(p, 1.0))[:3])
                fu, fv, fw, fh = _uv_region(face, u, v, uw, uh, ud)
                u0, u1, v0, v1 = fu, fu + fw, fv, fv + fh
                if not mirror:
                    u0, u1 = u1, u0
                out.append((points, [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]))
    return out


def _camera(yaw, pitch):
    """The view rotation for a camera orbiting the model: yaw 0 looks at its front (from -z), 90 at its left."""
    # In GeckoLib's space the front faces -z and the model's left lies at -x.
    a, b = math.radians(yaw), math.radians(pitch)
    forward = np.array([math.sin(a) * math.cos(b), -math.sin(b), math.cos(a) * math.cos(b)])  # from camera to model
    right = np.cross(forward, [0.0, 1.0, 0.0])
    right /= np.linalg.norm(right)
    up = np.cross(right, forward)
    return np.array([right, up, -forward])


# Entity lighting: two lights, as Minecraft lights an entity (a little above, front left and back right).
_LIGHTS = [np.array(v) / np.linalg.norm(v) for v in ((0.2, 1.0, -0.7), (-0.2, 1.0, 0.7))]


def _shade(normal):
    light = sum(max(0.0, float(np.dot(normal, l))) for l in _LIGHTS) * 0.6 + 0.4
    return min(1.0, light)


def render(geo, texture, posed=None, glow=None, yaw=30.0, pitch=10.0, size=(400, 520), scale=None, centre=None):
    """The posed model drawn on a transparent image `size` pixels big; `scale` is pixels per block (by default the
    model's visible bounds fill the image) and `centre` the model point at the middle of the image (in blocks)."""
    width, height = size
    definition = geo["minecraft:geometry"][0]["description"]
    tw, th = definition["texture_width"], definition["texture_height"]
    tex = np.asarray(texture.convert("RGBA"), dtype=np.float64)
    glow_tex = np.asarray(glow.convert("RGBA"), dtype=np.float64) if glow is not None else None
    sx, sy = tex.shape[1] / tw, tex.shape[0] / th
    if scale is None:
        scale = min(width, height) / max(1.0, definition.get("visible_bounds_height", 2.0)) * 0.9
    if centre is None:
        centre = (0.0, definition.get("visible_bounds_height", 2.0) / 2.0, 0.0)
    view = _camera(yaw, pitch)
    image = np.zeros((height, width, 4))
    depth = np.full((height, width), -1e9)
    for points, uvs in quads(geo, posed or {}):
        cam = [view @ (p - np.array(centre)) for p in points]
        normal = np.cross(points[1] - points[0], points[2] - points[1])
        n = np.linalg.norm(normal)
        light = _shade(normal / n) if n > 1e-9 else 1.0
        if n > 1e-9 and float(np.dot(view[2], normal / n)) < 0:
            light = _shade(-normal / n)  # a face seen from behind (GeckoLib draws entities without culling)
        screen = [(width / 2 + c[0] * scale, height / 2 - c[1] * scale, c[2]) for c in cam]
        for tri in ((0, 1, 2), (0, 2, 3)):
            _raster(image, depth, [screen[i] for i in tri], [uvs[i] for i in tri], tex, glow_tex, sx, sy, light)
    return Image.fromarray(np.clip(image, 0, 255).astype(np.uint8), "RGBA")


def _raster(image, depth, pts, uvs, tex, glow_tex, sx, sy, light):
    (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = pts
    area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
    if abs(area) < 1e-9:
        return
    h, w = depth.shape
    minx, maxx = max(0, int(math.floor(min(x0, x1, x2)))), min(w - 1, int(math.ceil(max(x0, x1, x2))))
    miny, maxy = max(0, int(math.floor(min(y0, y1, y2)))), min(h - 1, int(math.ceil(max(y0, y1, y2))))
    if minx > maxx or miny > maxy:
        return
    xs, ys = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
    w0 = ((x1 - xs) * (y2 - ys) - (x2 - xs) * (y1 - ys)) / area
    w1 = ((x2 - xs) * (y0 - ys) - (x0 - xs) * (y2 - ys)) / area
    w2 = 1.0 - w0 - w1
    inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
    if not inside.any():
        return
    z = w0 * z0 + w1 * z1 + w2 * z2
    u = (w0 * uvs[0][0] + w1 * uvs[1][0] + w2 * uvs[2][0]) * sx
    v = (w0 * uvs[0][1] + w1 * uvs[1][1] + w2 * uvs[2][1]) * sy
    ti = np.clip(np.floor(u).astype(int), 0, tex.shape[1] - 1)
    tj = np.clip(np.floor(v - 1e-6).astype(int), 0, tex.shape[0] - 1)
    texel = tex[tj, ti]
    region = depth[miny:maxy + 1, minx:maxx + 1]
    draw = inside & (texel[..., 3] > 25) & (z > region)
    if not draw.any():
        return
    colour = texel[..., :3] * light
    if glow_tex is not None:
        g = glow_tex[tj, ti]
        a = g[..., 3:4] / 255.0
        colour = colour * (1 - a) + g[..., :3] * a
    target = image[miny:maxy + 1, minx:maxx + 1]
    target[draw, :3] = colour[draw]
    target[draw, 3] = 255
    region[draw] = z[draw]


def sheet(frames, columns, background=(24, 22, 30), label=None):
    """Previews side by side on one image, `columns` to a row."""
    w, h = frames[0].size
    rows = (len(frames) + columns - 1) // columns
    out = Image.new("RGB", (w * columns, h * rows), background)
    for i, frame in enumerate(frames):
        out.paste(frame, ((i % columns) * w, (i // columns) * h), frame)
    return out
