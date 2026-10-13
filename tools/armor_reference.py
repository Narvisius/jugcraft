"""Matching the 3D armor sets to the owner's renders: fit a camera (and the figure's pose) to a render, draw our model
from it, and lift the render's texels onto the model's faces.

The owner designs armor in Blockbench and sends renders of it. A render is kept under art/armor/references/<set>/
(<view>.png), with what was fitted to it beside it (<view>.fit.json: the camera, the pose shared by the set's views,
and polygons round the weapons and effects that hide the armor). A reference is named "<set>/<view>".

World space is Blockbench's: x the model's right, y up from the feet, z back (the model faces -z), as
armor_preview.world_quads gives it. A camera is a dict: az, el (degrees: az 0 looks at the front, 90 at the model's
right side; el up), dist (model px from the target), tx, ty, tz (the target), f (focal length in image px), cx, cy (where
the target lands in the image). A pose is {bone: (xRot, yRot, zRot) degrees} in the toolkit's model space
(HumanoidModel's), as armor_preview poses it. Renders lit as Blockbench lights faces (texture.vert.glsl: 0.75 + 0.25 ny
- 0.15 nx^2 + 0.05 nz^2) or unlit are both handled (light()).

What it does:
    fit_silhouette / fit_joint    a view's camera, or every view's and their shared pose, to the render's outline
    fit_region                    a camera refined on one part's outline (a helm against the background is crisp)
    PhotoFit                      parts' parameters fitted so the art lifted from one view agrees with the others
    lift_refined, finish_atlas    the set's atlas lifted face by face (each face's corners nudged until the render's
                                  texel cells are flat: pixel art), gaps filled
    compare_crops, grid_view      the render beside ours from its camera; our texel grid laid over it

    python3 tools/armor_reference.py compare --set sentinel      build/armor_reference/<set>_compare.png
"""
import argparse
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import armor_models as am
import armor_paint
import armor_preview as pv

ROOT = Path(__file__).resolve().parents[1]
REFS = ROOT / "art/armor/references"
OUT = ROOT / "build/armor_reference"


def fit_path(name):
    """Where a reference's fit is kept: art/armor/references/<set>/<view>.fit.json."""
    return REFS / f"{name}.fit.json"


# ---------------------------------------------------------------- images
def ref(name):
    """A reference image ("<set>/<view>") as an RGB float array. "<set>/<view>@<region>" names the same image with a
    camera of its own, fitted to one region of the figure (its legs, say) where the owner's pose differs from ours."""
    return np.asarray(Image.open(REFS / f"{name.split('@')[0]}.png").convert("RGB")).astype(np.float32)


def zoom(image, box, factor=4, step=10, path=None, marks=(), grid=0.35):
    """A crop of an image (array) enlarged `factor` times, with faint grid lines every `step` source pixels (stronger
    every 5 steps) and source coordinates on the margins, to read positions off. marks: [(x, y, colour)] circles at
    source coordinates."""
    x0, y0, x1, y1 = box
    crop = np.clip(image[y0:y1, x0:x1], 0, 255).astype(np.float32)
    W, H = int(round((x1 - x0) * factor)), int(round((y1 - y0) * factor))
    big = np.asarray(Image.fromarray(crop.astype(np.uint8)).resize((W, H), Image.NEAREST)).astype(np.float32)
    for x in range(((x0 + step - 1) // step) * step, x1, step):
        px = min(W - 1, int(round((x - x0) * factor)))
        a = grid if x % (step * 5) else min(1.0, grid * 2)
        big[:, px] = big[:, px] * (1 - a) + np.array([0, 220, 255]) * a
    for y in range(((y0 + step - 1) // step) * step, y1, step):
        py = min(H - 1, int(round((y - y0) * factor)))
        a = grid if y % (step * 5) else min(1.0, grid * 2)
        big[py, :] = big[py, :] * (1 - a) + np.array([0, 220, 255]) * a
    margin = 30
    canvas = Image.new("RGB", (big.shape[1] + margin, big.shape[0] + margin), (255, 255, 255))
    canvas.paste(Image.fromarray(big.astype(np.uint8)), (margin, margin))
    d = ImageDraw.Draw(canvas)
    for x in range(((x0 + step - 1) // step) * step, x1, step):
        px = margin + int(round((x - x0) * factor))
        if (x // step) % 2 == 0 or factor * step >= 40:
            d.text((px - 6, 2 + (10 if (x // step) % 4 == 2 and factor * step < 40 else 0)), str(x), fill=(0, 0, 0))
    for y in range(((y0 + step - 1) // step) * step, y1, step):
        py = margin + int(round((y - y0) * factor))
        if (y // step) % 2 == 0 or factor * step >= 24:
            d.text((1, py - 5), str(y), fill=(0, 0, 0))
    for mx, my, colour in marks:
        px, py = margin + (mx - x0) * factor, margin + (my - y0) * factor
        d.ellipse([px - 4, py - 4, px + 4, py + 4], outline=colour, width=2)
    if path:
        canvas.save(path)
    return canvas


# ---------------------------------------------------------------- camera
def basis(az, el):
    a, e = math.radians(az), math.radians(el)
    d = np.array([math.sin(a) * math.cos(e), math.sin(e), -math.cos(a) * math.cos(e)])
    f = -d
    r = np.array([-math.cos(a), 0.0, -math.sin(a)])
    u = np.cross(r, f)
    return d, r, u, f


def project(points, cam):
    """(N x 3 world points) -> (u, v, depth) image coordinates."""
    d, r, u, f = basis(cam["az"], cam["el"])
    target = np.array([cam.get("tx", 0.0), cam.get("ty", 16.0), cam.get("tz", 0.0)])
    eye = target + cam["dist"] * d
    p = np.asarray(points, dtype=float) - eye
    x, y, z = p @ r, p @ u, p @ f
    z = np.maximum(z, 1e-6)
    return cam["cx"] + cam["f"] * x / z, cam["cy"] - cam["f"] * y / z, z


# ---------------------------------------------------------------- poses and the scene
def bones_for(pose):
    """{bone: 4x4 bone space -> toolkit model space} for a pose {bone: (x, y, z) degrees}."""
    out = {}
    for bone in am.BONES:
        rot = pose.get(bone, (0.0, 0.0, 0.0))
        out[bone] = pv.bone_matrix(list(am.PIVOTS[bone]), [math.radians(a) for a in rot])
    return out


def to_world(model_points):
    p = np.asarray(model_points, dtype=float)
    return np.stack([-p[:, 0], 24 - p[:, 1], p[:, 2]], axis=1)


def bone_to_world(bone, points, pose):
    m = bones_for(pose)[bone]
    p = np.asarray(points, dtype=float)
    model = (np.c_[p, np.ones(len(p))] @ m.T)[:, :3]
    return to_world(model)


def light(normal, lighting="blockbench"):
    """A face's brightness (world normal): Blockbench's shader (texture.vert.glsl), the game's, or 1 (unlit)."""
    n = np.asarray(normal, dtype=float)
    n = n / (np.linalg.norm(n) or 1.0)
    if lighting == "unlit":
        return 1.0
    if lighting == "blockbench":
        return float(0.75 + 0.25 * n[1] - 0.15 * n[0] ** 2 + 0.05 * n[2] ** 2)
    return pv.shade(n)


class Scene:
    """The faces of some armor sets (and optionally the grey player) in a pose: for each face its world corners, its
    atlas texel corners, its atlas, set and part. Built from the toolkit's parts, not the exported quads, so every face
    knows its part."""

    def __init__(self, sets, pose=None, body=True, atlases=None, items=None):
        self.pose = pose or {}
        mats = bones_for(self.pose)
        self.faces = []      # (world 4x3, atlas uv texels 4x2, normal world, atlas index, set name, item, part, face)
        self.atlases = []
        groups = list(sets)
        if body:
            guide = am.ArmorSet("player", pv.MANNEQUIN,
                                {"player": {b: [am.around(f"player_{b}", b, 0, paint=pv._skin_paint(b))]
                                            for b in am.BONES}}, width=64, texture="player")
            groups = [guide] + groups
        for s in groups:
            atlas = (atlases or {}).get(s.name)
            atlas = np.asarray((atlas if atlas is not None else armor_paint.paint_atlas(s)).convert("RGBA"))
            index = len(self.atlases)
            self.atlases.append(atlas)
            nets, _ = am.layout(s)
            for item, bone, part in s.parts():
                if items is not None and s.name != "player" and item not in items:
                    continue
                u0, v0 = (0, 0) if part.uvs else nets[part.key][:2]
                m = mats[bone]
                for face, pts, uvs, normal in am.part_faces(part, s.density):
                    p = np.asarray(pts, dtype=float)
                    model = (np.c_[p, np.ones(4)] @ m.T)[:, :3]
                    world = to_world(model)
                    n = m[:3, :3] @ np.asarray(normal, dtype=float)
                    nw = np.array([-n[0], -n[1], n[2]])
                    uv = np.asarray(uvs, dtype=float) + np.array([u0, v0])
                    self.faces.append((world, uv, nw, index, s.name, item, part, face))


def rasterize(scene, cam, size, lighting="blockbench", background=(0, 0, 0), skip=None):
    """Renders the scene: (colour HxWx3 float, depth HxW, face index HxW (-1 none), atlas u, atlas v HxW).
    skip(face tuple) -> True leaves a face out."""
    w, h = size
    colour = np.empty((h, w, 3), dtype=np.float32)
    colour[:] = background
    depth = np.full((h, w), np.inf)
    fid = np.full((h, w), -1, dtype=np.int32)
    uu = np.zeros((h, w), dtype=np.float32)
    vv = np.zeros((h, w), dtype=np.float32)
    for index, (world, uv, normal, ai, *_rest) in enumerate(scene.faces):
        if skip and skip(scene.faces[index]):
            continue
        atlas = scene.atlases[ai]
        th, tw = atlas.shape[:2]
        px, py, z = project(world, cam)
        bright = light(normal, lighting)
        umin, umax = uv[:, 0].min() + 1e-3, uv[:, 0].max() - 1e-3
        vmin, vmax = uv[:, 1].min() + 1e-3, uv[:, 1].max() - 1e-3
        inv = 1.0 / z
        for a, b, c in ((0, 1, 2), (0, 2, 3)):
            area = (px[b] - px[a]) * (py[c] - py[a]) - (py[b] - py[a]) * (px[c] - px[a])
            if abs(area) < 1e-9:
                continue
            bx0, bx1 = max(0, int(math.floor(min(px[a], px[b], px[c])))), min(w, int(math.ceil(max(px[a], px[b], px[c]))))
            by0, by1 = max(0, int(math.floor(min(py[a], py[b], py[c])))), min(h, int(math.ceil(max(py[a], py[b], py[c]))))
            if bx0 >= bx1 or by0 >= by1:
                continue
            gx, gy = np.meshgrid(np.arange(bx0, bx1) + 0.5, np.arange(by0, by1) + 0.5)
            wa = ((px[b] - gx) * (py[c] - gy) - (py[b] - gy) * (px[c] - gx)) / area
            wb = ((px[c] - gx) * (py[a] - gy) - (py[c] - gy) * (px[a] - gx)) / area
            wc = 1.0 - wa - wb
            inside = (wa >= -1e-7) & (wb >= -1e-7) & (wc >= -1e-7)
            if not inside.any():
                continue
            ws = wa * inv[a] + wb * inv[b] + wc * inv[c]
            zz = 1.0 / ws
            u = (wa * uv[a, 0] * inv[a] + wb * uv[b, 0] * inv[b] + wc * uv[c, 0] * inv[c]) / ws
            v = (wa * uv[a, 1] * inv[a] + wb * uv[b, 1] * inv[b] + wc * uv[c, 1] * inv[c]) / ws
            tx = np.clip(np.floor(np.clip(u, umin, umax)).astype(int), 0, tw - 1)
            ty = np.clip(np.floor(np.clip(v, vmin, vmax)).astype(int), 0, th - 1)
            texel = atlas[ty, tx]
            region = depth[by0:by1, bx0:bx1]
            draw = inside & (zz < region) & (texel[..., 3] >= 3)
            region[draw] = zz[draw]
            colour[by0:by1, bx0:bx1][draw] = texel[..., :3][draw] * bright
            fid[by0:by1, bx0:bx1][draw] = index
            uu[by0:by1, bx0:bx1][draw] = u[draw]
            vv[by0:by1, bx0:bx1][draw] = v[draw]
    return colour, depth, fid, uu, vv


# ---------------------------------------------------------------- fitting
def fit(points, cam, pose, free, iterations=60, verbose=False):
    """Least squares (Levenberg-Marquardt, numeric Jacobian): points [(bone, bone-space xyz, (u, v))]; free: names of
    camera keys ("az", "f", ...) and pose entries ("right_arm.x", "head.y", ...) to fit. Returns (cam, pose, rms)."""
    cam, pose = dict(cam), {k: list(v) for k, v in pose.items()}

    def unpack(x):
        c, p = dict(cam), {k: list(v) for k, v in pose.items()}
        for name, value in zip(free, x):
            if "." in name:
                bone, axis = name.split(".")
                p.setdefault(bone, [0.0, 0.0, 0.0])["xyz".index(axis)] = value
            else:
                c[name] = value
        return c, p

    def residuals(x):
        c, p = unpack(x)
        out = []
        for bone, xyz, (u, v) in points:
            w = bone_to_world(bone, [xyz], p)
            pu, pvv, _ = project(w, c)
            out += [pu[0] - u, pvv[0] - v]
        return np.array(out)

    x = np.array([pose.setdefault(n.split(".")[0], [0.0, 0.0, 0.0])["xyz".index(n.split(".")[1])] if "." in n
                  else cam[n] for n in free], dtype=float)
    lam = 1e-2
    r = residuals(x)
    cost = float(r @ r)
    for it in range(iterations):
        J = np.empty((len(r), len(x)))
        for j in range(len(x)):
            step = 1e-4 * max(1.0, abs(x[j]))
            xp = x.copy()
            xp[j] += step
            J[:, j] = (residuals(xp) - r) / step
        A = J.T @ J
        g = J.T @ r
        improved = False
        for _ in range(10):
            dx = -np.linalg.solve(A + lam * np.diag(np.diag(A) + 1e-9), g)
            xn = x + dx
            rn = residuals(xn)
            cn = float(rn @ rn)
            if cn < cost:
                x, r, cost, lam, improved = xn, rn, cn, lam * 0.3, True
                break
            lam *= 10
        if verbose:
            print(it, math.sqrt(cost / max(1, len(r) // 2)))
        if not improved or np.abs(dx).max() < 1e-7:
            break
    c, p = unpack(x)
    return c, {k: tuple(v) for k, v in p.items()}, math.sqrt(cost / max(1, len(r) // 2)), r.reshape(-1, 2)


# ---------------------------------------------------------------- comparing and lifting texels
_CAM_OVERRIDE = {}


def side_by_side(images, labels, path, scale=1):
    tiles = []
    for im, text in zip(images, labels):
        a = Image.fromarray(np.clip(im, 0, 255).astype(np.uint8))
        if scale != 1:
            a = a.resize((int(a.width * scale), int(a.height * scale)), Image.NEAREST)
        t = Image.new("RGB", (a.width, a.height + 18), (30, 30, 34))
        t.paste(a, (0, 18))
        ImageDraw.Draw(t).text((4, 3), text, fill=(235, 235, 235))
        tiles.append(t)
    W = sum(t.width for t in tiles) + 6 * (len(tiles) + 1)
    H = max(t.height for t in tiles) + 12
    out = Image.new("RGB", (W, H), (18, 18, 20))
    x = 6
    for t in tiles:
        out.paste(t, (x, 6))
        x += t.width + 6
    out.save(path)
    return path


def overlay(reference, render, fid, path, alpha=0.5):
    """The reference with our render blended over it where our model is, and our silhouette edges in magenta."""
    out = reference.copy()
    mask = fid >= 0
    out[mask] = reference[mask] * (1 - alpha) + render[mask] * alpha
    edge = mask ^ np.roll(mask, 1, 0) | mask ^ np.roll(mask, 1, 1)
    out[edge] = (255, 0, 255)
    Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).save(path)
    return path


def lift(scene, reference, fid, uu, vv, lighting="blockbench", inner=0.25):
    """{atlas index: (sum RGB, count)} of the reference's pixels over each atlas texel, taken only near texel centres
    (fraction within [inner, 1 - inner]) and divided by the face's light, for the faces drawn."""
    out = {}
    h, w = fid.shape
    ys, xs = np.nonzero(fid >= 0)
    faces = fid[ys, xs]
    u, v = uu[ys, xs], vv[ys, xs]
    fu, fv = u - np.floor(u), v - np.floor(v)
    keep = (fu > inner) & (fu < 1 - inner) & (fv > inner) & (fv < 1 - inner)
    ys, xs, faces, u, v = ys[keep], xs[keep], faces[keep], u[keep], v[keep]
    for ai in range(len(scene.atlases)):
        th, tw = scene.atlases[ai].shape[:2]
        out[ai] = (np.zeros((th, tw, 3)), np.zeros((th, tw)))
    lights = np.array([light(f[2], lighting) for f in scene.faces])
    for y, x, fi, uu_, vv_ in zip(ys, xs, faces, u, v):
        ai = scene.faces[fi][3]
        s, c = out[ai]
        tx, ty = int(uu_), int(vv_)
        s[ty, tx] += reference[y, x] / lights[fi]
        c[ty, tx] += 1
    return out


# ---------------------------------------------------------------- silhouettes
def ref_mask(image, thresh=7.0, exclude=(), size=None):
    """(model mask, care mask) of a dark-background reference: pixels brighter than thresh are the model; care is
    False inside the exclude polygons (weapons, effects), which the fit ignores."""
    lum = image.max(axis=2)
    mask = lum > thresh
    care = np.ones(mask.shape, dtype=bool)
    if exclude:
        canvas = Image.new("L", (mask.shape[1], mask.shape[0]), 0)
        d = ImageDraw.Draw(canvas)
        for poly in exclude:
            d.polygon([tuple(p) for p in poly], fill=255)
        care = np.asarray(canvas) == 0
    return mask, care


def silhouette(scene, cam, size, scale=0.5, skip=None):
    """Our model's silhouette (every face filled) at `scale` of the image size, as a bool array."""
    w, h = int(size[0] * scale), int(size[1] * scale)
    canvas = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(canvas)
    for f in scene.faces:
        if skip and skip(f):
            continue
        px, py, _ = project(f[0], cam)
        d.polygon([(x * scale, y * scale) for x, y in zip(px, py)], fill=255)
    return np.asarray(canvas) > 0


def iou(scene_for, cam, pose, mask, care, scale=0.5, skip=None):
    scene = scene_for(pose)
    ours = silhouette(scene, cam, (mask.shape[1], mask.shape[0]), scale, skip)
    h, w = ours.shape
    m = np.asarray(Image.fromarray(mask.astype(np.uint8) * 255).resize((w, h), Image.NEAREST)) > 0
    c = np.asarray(Image.fromarray(care.astype(np.uint8) * 255).resize((w, h), Image.NEAREST)) > 0
    inter = (ours & m & c).sum()
    union = ((ours | m) & c).sum()
    return inter / max(1, union)


def nelder_mead(fn, x0, steps, iterations=400, tol=1e-6):
    n = len(x0)
    pts = [np.array(x0, dtype=float)]
    for i in range(n):
        p = np.array(x0, dtype=float)
        p[i] += steps[i]
        pts.append(p)
    vals = [fn(p) for p in pts]
    for _ in range(iterations):
        order = np.argsort(vals)
        pts = [pts[i] for i in order]
        vals = [vals[i] for i in order]
        if abs(vals[-1] - vals[0]) < tol:
            break
        centroid = np.mean(pts[:-1], axis=0)
        xr = centroid + (centroid - pts[-1])
        fr = fn(xr)
        if fr < vals[0]:
            xe = centroid + 2 * (centroid - pts[-1])
            fe = fn(xe)
            pts[-1], vals[-1] = (xe, fe) if fe < fr else (xr, fr)
        elif fr < vals[-2]:
            pts[-1], vals[-1] = xr, fr
        else:
            xc = centroid + 0.5 * (pts[-1] - centroid)
            fc = fn(xc)
            if fc < vals[-1]:
                pts[-1], vals[-1] = xc, fc
            else:
                for i in range(1, len(pts)):
                    pts[i] = pts[0] + 0.5 * (pts[i] - pts[0])
                    vals[i] = fn(pts[i])
    best = int(np.argmin(vals))
    return pts[best], vals[best]


def fit_silhouette(sets, image, cam, pose, free, exclude=(), thresh=7.0, scale=0.4, iterations=400, steps=None,
                   items=None, rounds=2):
    """Camera and pose keys in `free` fitted to the reference's silhouette (IoU, ignoring `exclude`)."""
    mask, care = ref_mask(image, thresh, exclude)
    cam, pose = dict(cam), {k: list(v) for k, v in pose.items()}
    default_steps = {"az": 4.0, "el": 4.0, "dist": 10.0, "f": 60.0, "cx": 8.0, "cy": 8.0, "tx": 1.0, "ty": 1.0}
    steps = steps or [default_steps.get(n, 8.0) for n in free]
    cache = {}

    def scene_for(p):
        k = json.dumps(p, sort_keys=True)
        if k not in cache:
            if len(cache) > 64:
                cache.clear()
            cache[k] = Scene(sets, {b: tuple(v) for b, v in p.items()}, body=True, items=items)
        return cache[k]

    def unpack(x):
        c, p = dict(cam), {k: list(v) for k, v in pose.items()}
        for name, value in zip(free, x):
            if "." in name:
                bone, axis = name.split(".")
                p.setdefault(bone, [0.0, 0.0, 0.0])["xyz".index(axis)] = float(value)
            else:
                c[name] = float(value)
        return c, p

    def cost(x):
        c, p = unpack(x)
        return -iou(scene_for, c, p, mask, care, scale)

    x = [pose.setdefault(n.split(".")[0], [0.0, 0.0, 0.0])["xyz".index(n.split(".")[1])] if "." in n else cam[n]
         for n in free]
    best = None
    for r in range(rounds):
        x, val = nelder_mead(cost, x, [s / (2 ** r) for s in steps], iterations)
        best = val
    c, p = unpack(x)
    return c, {k: tuple(v) for k, v in p.items()}, -best


def load_fit(name):
    """(camera, pose) fitted to a reference; a camera being tried (refine_camera) stands in for the saved one."""
    data = json.load(open(fit_path(name)))
    cam = dict(_CAM_OVERRIDE.get(name, data["cam"]))
    return cam, {k: tuple(v) for k, v in data["pose"].items()}


def compare_crops(sets, views, path, factor=3, lighting="blockbench", atlases=None, items=None, edges=True):
    """For each view (ref name, crop box): the reference crop, our render crop and the two blended with our silhouette
    edges, enlarged `factor` times, one row per view."""
    rows = []
    for name, box in views:
        im = ref(name)
        cam, pose = load_fit(name)
        scene = Scene(sets, pose, atlases=atlases, items=items)
        col, dep, fid, uu, vv = rasterize(scene, cam, (im.shape[1], im.shape[0]), lighting, (2, 2, 2))
        x0, y0, x1, y1 = box
        mix = im.copy()
        mask = fid >= 0
        mix[mask] = im[mask] * 0.5 + col[mask] * 0.5
        if edges:
            edge = (mask ^ np.roll(mask, 1, 0)) | (mask ^ np.roll(mask, 1, 1))
            mix[edge] = (255, 0, 255)
        tiles = [im[y0:y1, x0:x1], col[y0:y1, x0:x1], mix[y0:y1, x0:x1]]
        row = [Image.fromarray(np.clip(t, 0, 255).astype(np.uint8)).resize(((x1 - x0) * factor, (y1 - y0) * factor),
                                                                           Image.NEAREST) for t in tiles]
        rows.append(row)
    W = sum(t.width for t in rows[0]) + 4 * 4
    H = sum(r[0].height for r in rows) + 4 * (len(rows) + 1)
    out = Image.new("RGB", (max(W, max(sum(t.width for t in r) + 16 for r in rows)), H), (40, 40, 46))
    y = 4
    for r in rows:
        x = 4
        for t in r:
            out.paste(t, (x, y))
            x += t.width + 4
        y += r[0].height + 4
    out.save(path)
    return path


def lift_views(sets, names, lighting="unlit", inner=0.25, items=None, min_count=2):
    """Each atlas texel's colour lifted from the references `names` (fitted cameras). Every face takes its texels
    from one view, the one that sees the face largest (most texel-centre pixels), so a face never mixes two views'
    slightly different alignments; texels that view does not see come from the next best. Returns ({atlas index:
    (RGBA array, chosen count)}, scene of the first view)."""
    per_view = {}
    scene0 = None
    face_px = {}
    for name in names:
        im = ref(name)
        cam, pose = load_fit(name)
        scene = Scene(sets, pose, items=items)
        scene0 = scene0 or scene
        col, dep, fid, uu, vv = rasterize(scene, cam, (im.shape[1], im.shape[0]), "unlit", (0, 0, 0))
        ys, xs = np.nonzero(fid >= 0)
        faces = fid[ys, xs]
        u, v = uu[ys, xs], vv[ys, xs]
        fu, fv = u - np.floor(u), v - np.floor(v)
        keep = (fu > inner) & (fu < 1 - inner) & (fv > inner) & (fv < 1 - inner)
        ys, xs, faces, u, v = ys[keep], xs[keep], faces[keep], u[keep], v[keep]
        lights = np.array([light(f[2], lighting) for f in scene.faces])
        acc = {}
        for y, x, fi, uu_, vv_ in zip(ys, xs, faces, u, v):
            key = (int(fi), int(uu_), int(vv_))
            if key not in acc:
                acc[key] = [np.zeros(3), 0]
            acc[key][0] += im[y, x] / lights[fi]
            acc[key][1] += 1
        per_view[name] = acc
        for (fi, _, _), (_, c) in acc.items():
            face_px[(fi, name)] = face_px.get((fi, name), 0) + c
    out = {}
    for ai in range(len(scene0.atlases)):
        th, tw = scene0.atlases[ai].shape[:2]
        out[ai] = (np.zeros((th, tw, 4), dtype=np.float32), np.zeros((th, tw)))
    nfaces = len(scene0.faces)
    for fi in range(nfaces):
        ranked = sorted(names, key=lambda n: -face_px.get((fi, n), 0))
        ai = scene0.faces[fi][3]
        rgba, cnt = out[ai]
        for name in ranked:
            if face_px.get((fi, name), 0) == 0:
                continue
            for (f2, tx, ty), (sumc, c) in per_view[name].items():
                if f2 != fi or c < min_count or rgba[ty, tx, 3] > 0:
                    continue
                rgba[ty, tx, :3] = np.clip(sumc / c, 0, 255)
                rgba[ty, tx, 3] = 255
                cnt[ty, tx] = c
    return out, scene0


def texel_grid(scene, cam, fid, size, colour=(0, 255, 255), which=None):
    """An RGBA layer of our model's texel grid lines, drawn only where each face is the one seen (fid), to lay over a
    reference and see whether our texel edges fall on the owner's. which(face tuple) -> True picks faces."""
    w, h = size
    layer = np.zeros((h, w, 4), dtype=np.uint8)
    visible = set(np.unique(fid[fid >= 0]).tolist())
    for index in visible:
        f = scene.faces[index]
        if which and not which(f):
            continue
        world, uv = f[0], f[1]
        # corners by uv: the face is an axis-aligned rectangle in uv space
        u0, u1 = uv[:, 0].min(), uv[:, 0].max()
        v0, v1 = uv[:, 1].min(), uv[:, 1].max()
        if u1 - u0 < 1e-6 or v1 - v0 < 1e-6:
            continue

        def at(u, v):
            # bilinear on the quad: find weights from the four corners' uvs
            a = (u - u0) / (u1 - u0)
            b = (v - v0) / (v1 - v0)
            corner = {}
            for k in range(4):
                key = (uv[k, 0] > (u0 + u1) / 2, uv[k, 1] > (v0 + v1) / 2)
                corner[key] = world[k]
            p00, p10, p01, p11 = corner[(False, False)], corner[(True, False)], corner[(False, True)], corner[(True, True)]
            return (1 - a) * (1 - b) * p00 + a * (1 - b) * p10 + (1 - a) * b * p01 + a * b * p11
        canvas = Image.new("L", (w, h), 0)
        d = ImageDraw.Draw(canvas)
        lines = []
        for u in range(int(math.ceil(u0 - 1e-6)), int(math.floor(u1 + 1e-6)) + 1):
            lines.append((at(u, v0), at(u, v1)))
        for v in range(int(math.ceil(v0 - 1e-6)), int(math.floor(v1 + 1e-6)) + 1):
            lines.append((at(u0, v), at(u1, v)))
        for a, b in lines:
            pu, pv, _ = project(np.array([a, b]), cam)
            d.line([(pu[0], pv[0]), (pu[1], pv[1])], fill=255, width=1)
        m = (np.asarray(canvas) > 0) & (fid == index)
        layer[m] = (*colour, 255)
    return layer


def grid_view(sets, name, box, path, factor=3, atlases=None, items=None, which=None):
    """The reference crop with our texel grid (cyan) and silhouette (magenta) laid over it."""
    im = ref(name)
    cam, pose = load_fit(name)
    scene = Scene(sets, pose, atlases=atlases, items=items, body=False)
    col, dep, fid, uu, vv = rasterize(scene, cam, (im.shape[1], im.shape[0]), "unlit", (2, 2, 2))
    grid = texel_grid(scene, cam, fid, (im.shape[1], im.shape[0]), which=which)
    out = im.copy()
    g = grid[..., 3] > 0
    out[g] = out[g] * 0.3 + np.array([0, 255, 255]) * 0.7
    mask = fid >= 0
    edge = (mask ^ np.roll(mask, 1, 0)) | (mask ^ np.roll(mask, 1, 1))
    out[edge] = (255, 0, 255)
    x0, y0, x1, y1 = box
    crop = Image.fromarray(np.clip(out[y0:y1, x0:x1], 0, 255).astype(np.uint8))
    crop = crop.resize(((x1 - x0) * factor, (y1 - y0) * factor), Image.NEAREST)
    crop.save(path)
    return path


def fit_joint(sets, names, pose, cam_free, pose_free, thresh=7.0, scale=0.35, iterations=600, rounds=2, items=None,
              weights=None, save=True):
    """Every view's camera (cam_free keys) and one shared pose (pose_free entries) fitted to the views' silhouettes
    together: the sum of their IoUs (each view's own exclusions, from its fit file). Saves the fits."""
    data = {n: json.load(open(fit_path(n))) for n in names}
    masks = {n: ref_mask(ref(n), thresh, data[n].get("exclude", [])) for n in names}
    cams = {n: dict(data[n]["cam"]) for n in names}
    pose = {k: list(v) for k, v in pose.items()}
    weights = weights or {n: 1.0 for n in names}
    cache = {}

    def scene_for(p):
        k = json.dumps(p, sort_keys=True)
        if k not in cache:
            if len(cache) > 32:
                cache.clear()
            cache[k] = Scene(sets, {b: tuple(v) for b, v in p.items()}, body=True, items=items)
        return cache[k]

    layout_ = [(n, key) for n in names for key in cam_free] + [(None, key) for key in pose_free]

    def unpack(x):
        cs = {n: dict(c) for n, c in cams.items()}
        p = {k: list(v) for k, v in pose.items()}
        for (n, key), value in zip(layout_, x):
            if n is None:
                bone, axis = key.split(".")
                p.setdefault(bone, [0.0, 0.0, 0.0])["xyz".index(axis)] = float(value)
            else:
                cs[n][key] = float(value)
        return cs, p

    def cost(x):
        cs, p = unpack(x)
        return -sum(weights[n] * iou(scene_for, cs[n], p, *masks[n], scale) for n in names)

    x = [cams[n][key] if n else pose.setdefault(key.split(".")[0], [0.0, 0.0, 0.0])["xyz".index(key.split(".")[1])]
         for n, key in layout_]
    default_steps = {"az": 3.0, "el": 3.0, "dist": 8.0, "f": 50.0, "cx": 6.0, "cy": 6.0}
    steps = [default_steps.get(key, 6.0) for _, key in layout_]
    val = None
    for r in range(rounds):
        x, val = nelder_mead(cost, x, [s / (2 ** r) for s in steps], iterations)
    cs, p = unpack(x)
    if save:
        for n in names:
            json.dump({"cam": cs[n], "pose": {k: tuple(v) for k, v in p.items()}, "exclude": data[n].get("exclude", [])},
                      open(fit_path(n), "w"), indent=1)
    return cs, {k: tuple(v) for k, v in p.items()}, -val


def landmarks(name, points):
    """Image positions of bone-space points [(bone, xyz)] under a view's fitted camera and pose."""
    cam, pose = load_fit(name)
    out = []
    for bone, xyz in points:
        w = bone_to_world(bone, [xyz], pose)
        u, v, _ = project(w, cam)
        out.append((float(u[0]), float(v[0])))
    return out


def ruler_view(name, box, path, factor=2, gamma=1.0, lines=()):
    """A reference crop (optionally brightened by gamma < 1) with body lines drawn: lines = [(label, [(bone, xyz),
    ...], colour)] polylines through bone-space points."""
    im = ref(name)
    if gamma != 1.0:
        im = 255 * (im / 255.0) ** gamma
    x0, y0, x1, y1 = box
    crop = Image.fromarray(np.clip(im[y0:y1, x0:x1], 0, 255).astype(np.uint8)).resize(
        ((x1 - x0) * factor, (y1 - y0) * factor), Image.NEAREST)
    d = ImageDraw.Draw(crop)
    for label, pts, colour in lines:
        xy = [((u - x0) * factor, (v - y0) * factor) for u, v in landmarks(name, pts)]
        d.line(xy, fill=colour, width=1)
        d.text((xy[0][0] + 2, xy[0][1] - 10), label, fill=colour)
    crop.save(path)
    return path


# ---------------------------------------------------------------- texel grid registration
_EDGES = {}


def edge_map(name):
    """The reference's colour edge strength per pixel (largest RGB central difference across x or y), cached."""
    if name not in _EDGES:
        im = ref(name)
        gx = np.zeros(im.shape[:2]); gy = np.zeros(im.shape[:2])
        gx[:, 1:-1] = np.abs(im[:, 2:] - im[:, :-2]).max(axis=2)
        gy[1:-1, :] = np.abs(im[2:] - im[:-2]).max(axis=2)
        _EDGES[name] = np.maximum(gx, gy)
    return _EDGES[name]


_OCC = {}


def occluder_depth(name, sets, leave_out, items=None):
    """The depth buffer of every face of `sets` in a view but the parts named in leave_out (and the grey player's
    body), to tell where a part being placed is hidden. Cached per view and left-out names."""
    key = (name, tuple(sorted(leave_out)), id(sets))
    if key not in _OCC:
        im = ref(name)
        cam, pose = load_fit(name)
        scene = Scene(sets, pose, items=items, body=True)
        scene.faces = [f for f in scene.faces if f[6].name not in leave_out]
        _, dep, _, _, _ = rasterize(scene, cam, (im.shape[1], im.shape[0]), "unlit")
        if len(_OCC) > 24:
            _OCC.clear()
        _OCC[key] = dep
    return _OCC[key]


def part_grid_score(part, bone, names, others=None, samples=6, density=1):
    """How well a part's texel grid (all its texel lines, faces seen in each view) lies on the references' colour
    edges: the mean edge strength sampled along its projected lines, averaged over views, minus a penalty for its
    silhouette poking outside the reference's. others: a Scene-like list of faces (world corners) that hide it."""
    total, weight = 0.0, 0.0
    for name in names:
        cam, pose = load_fit(name)
        m = bones_for(pose)[bone]
        edges = edge_map(name)
        h, w = edges.shape
        im = ref(name)
        inside = im.max(axis=2) > 7
        vals, outs = [], []
        for face, pts, uvs, normal in am.part_faces(part, density):
            p = np.asarray(pts, dtype=float)
            world = to_world((np.c_[p, np.ones(4)] @ m.T)[:, :3])
            n = m[:3, :3] @ np.asarray(normal, dtype=float)
            nw = np.array([-n[0], -n[1], n[2]])
            d, r, u, f = basis(cam["az"], cam["el"])
            target = np.array([cam.get("tx", 0.0), cam.get("ty", 16.0), cam.get("tz", 0.0)])
            eye = target + cam["dist"] * d
            if np.dot(nw, eye - world.mean(axis=0)) <= 0:
                continue      # the face looks away from this camera
            uv = np.asarray(uvs, dtype=float)
            u0, u1 = uv[:, 0].min(), uv[:, 0].max()
            v0, v1 = uv[:, 1].min(), uv[:, 1].max()
            corner = {}
            for k in range(4):
                corner[(uv[k, 0] > (u0 + u1) / 2, uv[k, 1] > (v0 + v1) / 2)] = world[k]
            p00, p10, p01, p11 = corner[(False, False)], corner[(True, False)], corner[(False, True)], corner[(True, True)]

            def at(a, b):
                return (1 - a) * (1 - b) * p00 + a * (1 - b) * p10 + (1 - a) * b * p01 + a * b * p11
            nu, nv = int(round(u1 - u0)), int(round(v1 - v0))
            pts3 = []
            for i in range(nu + 1):
                for j in range(nv * samples + 1):
                    pts3.append(at(i / max(nu, 1), j / (nv * samples)))
            for j in range(nv + 1):
                for i in range(nu * samples + 1):
                    pts3.append(at(i / (nu * samples), j / max(nv, 1)))
            # face interior samples for the silhouette penalty
            interior = [at((i + 0.5) / nu, (j + 0.5) / nv) for i in range(nu) for j in range(nv)]
            pu, pv, pz = project(np.array(pts3), cam)
            ok = (pu >= 1) & (pu < w - 1) & (pv >= 1) & (pv < h - 1)
            if others is not None:
                occ = others[name]
                ok[ok] &= pz[ok] < occ[pv[ok].astype(int), pu[ok].astype(int)] + 0.05
            vals.extend(edges[pv[ok].astype(int), pu[ok].astype(int)].tolist())
            iu, iv, _ = project(np.array(interior), cam)
            okk = (iu >= 0) & (iu < w) & (iv >= 0) & (iv < h)
            outs.extend((~inside[iv[okk].astype(int), iu[okk].astype(int)]).tolist())
        if vals:
            score = float(np.mean(vals)) - 120.0 * (float(np.mean(outs)) if outs else 0.0)
            total += score * len(vals)
            weight += len(vals)
    return total / weight if weight else -1e9


def register(part, bone, names, axes=("x", "y", "z"), span=1.0, step=0.125, rounds=3, others=None):
    """Moves a part (all its placement: origin, pivot and turns) to the offset in [-span, span] on `axes` that best
    lays its texel grid on the references' edges; coordinate search, step halving. Returns (part, offset, score)."""
    from dataclasses import replace as _rep

    def moved(off):
        o = tuple(part.origin[i] + off[i] for i in range(3))
        pv = tuple(part.pivot[i] + off[i] for i in range(3))
        turns = tuple((a, deg, tuple(pp[i] + off[i] for i in range(3))) for a, deg, pp in part.turns)
        return _rep(part, origin=o, pivot=pv, turns=turns)
    best = [0.0, 0.0, 0.0]
    best_score = part_grid_score(part, bone, names, others)
    s = step * 4
    for _ in range(rounds):
        improved = True
        while improved:
            improved = False
            for ax in axes:
                i = "xyz".index(ax)
                for sign in (-1, 1):
                    cand = list(best)
                    cand[i] += sign * s
                    if abs(cand[i]) > span:
                        continue
                    sc = part_grid_score(moved(cand), bone, names, others)
                    if sc > best_score + 1e-6:
                        best, best_score, improved = cand, sc, True
        s /= 2
        if s < step:
            break
    return moved(best), tuple(best), best_score


def refine_camera(name, trusted, sets=None, iou_weight=0.0, keys=("az", "el", "dist", "f", "cx", "cy"),
                  iterations=300, rounds=2, save=True, others=None):
    """A view's camera refined so the trusted parts' texel grids lie on the reference's edges (mean grid score over
    [(part, bone)]), optionally plus iou_weight x the silhouette IoU of `sets`."""
    data = json.load(open(fit_path(name)))
    cam0, pose = dict(data["cam"]), {k: tuple(v) for k, v in data["pose"].items()}
    if iou_weight:
        mask, care = ref_mask(ref(name), 7.0, data.get("exclude", []))
        scene = Scene(sets, pose, body=True)

    def cost(x):
        cam = dict(cam0)
        for k, v in zip(keys, x):
            cam[k] = float(v)
        tmp = fit_path(name)
        _CAM_OVERRIDE[name] = cam
        try:
            score = np.mean([part_grid_score(p, b, [name], others) for p, b in trusted])
        finally:
            _CAM_OVERRIDE.pop(name, None)
        if iou_weight:
            score += iou_weight * iou(lambda p: scene, cam, pose, mask, care, 0.35)
        return -score

    steps = {"az": 1.0, "el": 1.0, "dist": 4.0, "f": 20.0, "cx": 2.0, "cy": 2.0}
    x = [cam0[k] for k in keys]
    before = -cost(x)
    for r in range(rounds):
        x, val = nelder_mead(cost, x, [steps[k] / (2 ** r) for k in keys], iterations)
    cam = dict(cam0)
    for k, v in zip(keys, x):
        cam[k] = float(v)
    if save:
        data["cam"] = cam
        json.dump(data, open(fit_path(name), "w"), indent=1)
    return cam, before, -val


def fit_region(name, sets, region, parts, keys=("az", "el", "dist", "f", "cx", "cy"), iterations=400, rounds=3,
               save=True, pose_keys=()):
    """A view's camera (and optionally pose entries such as "head.y") fitted to the reference's silhouette inside a
    window `region` (x0, y0, x1, y1) where only `parts` (names) of our model show: full-resolution IoU there. The
    reference's figure is what is brighter than the fit's "thresh" (7 unless the fit file says otherwise: a render on
    a dark blue ground needs more)."""
    data = json.load(open(fit_path(name)))
    cam0, pose0 = dict(data["cam"]), {k: list(v) for k, v in data["pose"].items()}
    im = ref(name)
    x0, y0, x1, y1 = region
    ref_m = (im.max(axis=2) > data.get("thresh", 7.0))[y0:y1, x0:x1]

    def ours(cam, pose):
        scene = Scene(sets, {k: tuple(v) for k, v in pose.items()}, body=False)
        canvas = Image.new("L", (x1 - x0, y1 - y0), 0)
        d = ImageDraw.Draw(canvas)
        for f in scene.faces:
            if f[6].name not in parts:
                continue
            pu, pv, _ = project(f[0], cam)
            d.polygon([(u - x0, v - y0) for u, v in zip(pu, pv)], fill=255)
        return np.asarray(canvas) > 0

    names = list(keys) + list(pose_keys)

    def unpack(x):
        cam, pose = dict(cam0), {k: list(v) for k, v in pose0.items()}
        for k, v in zip(names, x):
            if "." in k:
                bone, axis = k.split(".")
                pose.setdefault(bone, [0.0, 0.0, 0.0])["xyz".index(axis)] = float(v)
            else:
                cam[k] = float(v)
        return cam, pose

    def cost(x):
        cam, pose = unpack(x)
        o = ours(cam, pose)
        return -(o & ref_m).sum() / max(1, (o | ref_m).sum())

    steps = {"az": 1.5, "el": 1.5, "dist": 5.0, "f": 25.0, "cx": 3.0, "cy": 3.0}
    x = [cam0[k] if "." not in k else pose0.setdefault(k.split(".")[0], [0.0, 0.0, 0.0])["xyz".index(k.split(".")[1])]
         for k in names]
    before = -cost(x)
    val = None
    for r in range(rounds):
        x, val = nelder_mead(cost, x, [steps.get(k, 2.0) / (2 ** r) for k in names], iterations)
    cam, pose = unpack(x)
    if save:
        data["cam"] = cam
        data["pose"] = {k: tuple(v) for k, v in pose.items()}
        json.dump(data, open(fit_path(name), "w"), indent=1)
    return cam, pose, before, -val


# ---------------------------------------------------------------- per-face homographies
def homography(src, dst):
    """3x3 H with dst ~ H src for 4 point pairs (DLT)."""
    A = []
    for (x, y), (u, v) in zip(src, dst):
        A.append([-x, -y, -1, 0, 0, 0, u * x, u * y, u])
        A.append([0, 0, 0, -x, -y, -1, v * x, v * y, v])
    _, _, vt = np.linalg.svd(np.asarray(A, dtype=float))
    H = vt[-1].reshape(3, 3)
    return H / H[2, 2]


def apply_h(H, pts):
    p = np.c_[pts, np.ones(len(pts))] @ H.T
    return p[:, :2] / p[:, 2:3]


def bilinear(im, xy):
    h, w = im.shape[:2]
    x = np.clip(xy[:, 0] - 0.5, 0, w - 1.001)
    y = np.clip(xy[:, 1] - 0.5, 0, h - 1.001)
    x0, y0 = np.floor(x).astype(int), np.floor(y).astype(int)
    fx, fy = (x - x0)[:, None], (y - y0)[:, None]
    return (im[y0, x0] * (1 - fx) * (1 - fy) + im[y0, x0 + 1] * fx * (1 - fy) + im[y0 + 1, x0] * (1 - fx) * fy
            + im[y0 + 1, x0 + 1] * fx * fy)


def face_cells(w, h, k=3, inner=0.3):
    """Sample points (texel space) in each texel's central part: (w*h*k*k, 2), and each sample's cell index."""
    offs = np.linspace(inner, 1 - inner, k)
    gx, gy = np.meshgrid(offs, offs)
    base = np.stack([gx.ravel(), gy.ravel()], axis=1)
    cells = []
    pts = []
    for j in range(h):
        for i in range(w):
            pts.append(base + np.array([i, j]))
            cells.extend([j * w + i] * len(base))
    return np.concatenate(pts), np.array(cells)


def refine_face(im, corners, w, h, valid=None, span=None, iterations=400):
    """Corners (image xy of texel-space (0,0), (w,0), (w,h), (0,h)) nudged so each texel cell of the reference is as
    flat a colour as it can be (pixel art): least mean within-cell variance. valid: (h, w) bool, cells to count.
    Returns (corners, colours (h, w, 3) median per cell, flatness before, after)."""
    src = [(0, 0), (w, 0), (w, h), (0, h)]
    pts, cells = face_cells(w, h)
    ncell = w * h
    use = np.ones(ncell, dtype=bool) if valid is None else valid.ravel()
    corners = np.asarray(corners, dtype=float)
    size = max(np.linalg.norm(corners[1] - corners[0]) / w, np.linalg.norm(corners[3] - corners[0]) / h)
    span = span or size * 0.8

    def colours_for(c):
        H = homography(src, c)
        xy = apply_h(H, pts)
        return bilinear(im, xy)

    def cost(x):
        c = corners + x.reshape(4, 2)
        col = colours_for(c).reshape(ncell, -1, 3)
        var = col.var(axis=1).sum(axis=1)
        return float(var[use].mean()) + 0.02 * float(np.square(x).sum())

    x0 = np.zeros(8)
    before = cost(x0)
    best = x0
    val = before
    for r in range(3):
        best, val = nelder_mead(cost, best, [span / (2 ** r)] * 8, iterations)
    c = corners + best.reshape(4, 2)
    col = colours_for(c).reshape(ncell, -1, 3)
    med = np.median(col, axis=1).reshape(h, w, 3)
    return c, med, before, val


def face_corners_image(f, cam, density=1):
    """(corners (4, 2) image xy for the face's texel-space (0,0), (w,0), (w,h), (0,h), w, h, umin, vmin) of a scene
    face tuple (world corners and atlas uvs)."""
    world, uv = f[0], f[1]
    umin, vmin = uv[:, 0].min(), uv[:, 1].min()
    fw, fh = int(round(uv[:, 0].max() - umin)), int(round(uv[:, 1].max() - vmin))
    order = []
    for tu, tv in ((0, 0), (fw, 0), (fw, fh), (0, fh)):
        k = int(np.argmin(np.abs(uv[:, 0] - umin - tu) + np.abs(uv[:, 1] - vmin - tv)))
        order.append(world[k])
    pu, pv, _ = project(np.array(order), cam)
    return np.stack([pu, pv], axis=1), fw, fh, int(round(umin)), int(round(vmin))


def lift_refined(sets, names, set_index=1, min_visible=0.35, min_px_per_texel=3.0, refine=True, verbose=False,
                 only=None):
    """The set's atlas lifted from the references face by face: each face from the view that sees most of it, its
    projected corners nudged so the reference's texel cells are flattest, each texel the median of its cell, divided
    by the face's brightness when the fit says the render is lit ("lighting": "blockbench"). Returns
    (atlas RGBA float array with alpha 255 where lifted, {face key: (view, flatness, visible fraction)})."""
    views = {}
    for name in names:
        im = ref(name)
        cam, pose = load_fit(name)
        scene = Scene(sets, pose)
        col, dep, fid, uu, vv = rasterize(scene, cam, (im.shape[1], im.shape[0]), "unlit", (0, 0, 0))
        data = json.load(open(fit_path(name)))
        _, care = ref_mask(im, 7.0, data.get("exclude", []))
        fid = np.where(care, fid, -1)        # weapons and effects hide what is behind them
        views[name] = (im, cam, scene, fid, uu, vv, data.get("lighting", "unlit"))
    scene0 = views[names[0]][2]
    th, tw = scene0.atlases[set_index].shape[:2]
    atlas = np.zeros((th, tw, 4), dtype=np.float32)
    report = {}
    for fi, f in enumerate(scene0.faces):
        if f[3] != set_index:
            continue
        if only and f[6].name not in only:
            continue
        best = None
        for name in names:
            im, cam, scene, fid, uu, vv, _ = views[name]
            corners, fw, fh, umin, vmin = face_corners_image(scene.faces[fi], cam)
            if fw < 1 or fh < 1:
                continue
            area = abs(0.5 * sum(corners[i - 1][0] * corners[i][1] - corners[i][0] * corners[i - 1][1] for i in range(4)))
            # which texel cells are seen: the cell centre's pixel shows this face
            H = homography([(0, 0), (fw, 0), (fw, fh), (0, fh)], corners)
            centres = np.array([(i + 0.5, j + 0.5) for j in range(fh) for i in range(fw)])
            xy = apply_h(H, centres)
            hh, ww = fid.shape
            inside = (xy[:, 0] >= 0) & (xy[:, 0] < ww) & (xy[:, 1] >= 0) & (xy[:, 1] < hh)
            seen = np.zeros(len(xy), dtype=bool)
            seen[inside] = fid[xy[inside, 1].astype(int), xy[inside, 0].astype(int)] == fi
            frac = seen.mean()
            px = math.sqrt(area / max(1, fw * fh))
            score = frac * area
            if frac >= min_visible and px >= min_px_per_texel and (best is None or score > best[0]):
                best = (score, name, corners, fw, fh, umin, vmin, seen.reshape(fh, fw), frac, px)
        if best is None:
            continue
        _, name, corners, fw, fh, umin, vmin, seen, frac, px = best
        im = views[name][0]
        shade = light(views[name][2].faces[fi][2], views[name][6])   # a lit render (the fit's "lighting") is undone
        if refine:
            corners2, colours, b, a = refine_face(im, corners, fw, fh, valid=seen, span=px * 0.6, iterations=250)
        else:
            H = homography([(0, 0), (fw, 0), (fw, fh), (0, fh)], corners)
            pts, cells = face_cells(fw, fh)
            colours = np.median(bilinear(im, apply_h(H, pts)).reshape(fw * fh, -1, 3), axis=1).reshape(fh, fw, 3)
            b = a = 0.0
        region = atlas[vmin:vmin + fh, umin:umin + fw]
        region[seen, :3] = np.clip(colours[seen] / shade, 0, 255)
        region[seen, 3] = 255
        report[(f[6].name, f[7])] = (name, round(a, 1), round(float(frac), 2))
        if verbose:
            print(f[6].name, f[7], name, fw, fh, "visible", round(float(frac), 2), "flat", round(b, 1), "->", round(a, 1))
    return atlas, report


def backproject(name, bone, uv, plane_axis=2, plane_value=0.0):
    """The bone-space point where the view's camera ray through image point uv meets the bone-space plane
    {axis = value} (in the fitted pose)."""
    cam, pose = load_fit(name)
    d, r, u, f = basis(cam["az"], cam["el"])
    target = np.array([cam.get("tx", 0.0), cam.get("ty", 16.0), cam.get("tz", 0.0)])
    eye = target + cam["dist"] * d
    x = (uv[0] - cam["cx"]) / cam["f"]
    y = -(uv[1] - cam["cy"]) / cam["f"]
    ray = f + x * r + y * u
    # world -> toolkit model space: (x, y, z) -> (-x, 24 - y, z); model = M bone
    m = bones_for(pose)[bone]
    inv = np.linalg.inv(m)
    def to_bone(w):
        model = np.array([-w[0], 24 - w[1], w[2], 1.0])
        return (inv @ model)[:3]
    p0 = to_bone(eye)
    p1 = to_bone(eye + ray)
    dirv = p1 - p0
    t = (plane_value - p0[plane_axis]) / dirv[plane_axis]
    return p0 + t * dirv


# ---------------------------------------------------------------- multi-view photo-consistency
class PhotoFit:
    """Fits parameters of some parts (names starting with `prefixes`) so their art agrees across views: each face's
    texels are lifted from the view that sees it best, then the parts are drawn in every view with that art and
    compared with the references inside each view's window. Other parts are fixed occluders."""

    def __init__(self, build, prefixes, windows, density=1, set_name=None, iou_weight=60.0):
        self.build, self.prefixes, self.windows, self.density = build, tuple(prefixes), windows, density
        self.iou_weight = iou_weight
        self.views = {}
        sets = build()
        for name, box in windows:
            im = ref(name)
            cam, pose = load_fit(name)
            x0, y0, x1, y1 = box
            camw = dict(cam, cx=cam["cx"] - x0, cy=cam["cy"] - y0)
            scene = Scene(sets, pose, body=True)
            occ_faces = [f for f in scene.faces if not f[6].name.startswith(self.prefixes)]
            occ_scene = Scene([], pose, body=False)
            occ_scene.faces, occ_scene.atlases = occ_faces, scene.atlases
            _, dep, _, _, _ = rasterize(occ_scene, camw, (x1 - x0, y1 - y0), "unlit")
            crop = im[y0:y1, x0:x1]
            bg = crop.max(axis=2) <= 7
            data = json.load(open(fit_path(name)))
            _, care = ref_mask(im, 7.0, data.get("exclude", []))
            self.care = getattr(self, "care", {})
            self.care[name] = care[y0:y1, x0:x1]
            self.views[name] = (crop, camw, pose, dep, bg)

    def faces(self, sets, pose):
        scene = Scene(sets, pose, body=False)
        scene.faces = [f for f in scene.faces if f[6].name.startswith(self.prefixes)]
        return scene

    def error(self, verbose=False):
        sets = self.build()
        drawn = {}
        for name, (crop, camw, pose, dep, bg) in self.views.items():
            scene = self.faces(sets, pose)
            col, d2, fid, uu, vv = rasterize(scene, camw, (crop.shape[1], crop.shape[0]), "unlit")
            vis = (fid >= 0) & (d2 < dep - 1e-3)
            drawn[name] = (scene, fid, uu, vv, vis)
        # lift each face from the view where it shows most
        nf = len(next(iter(drawn.values()))[0].faces)
        counts = {name: np.bincount(fid[vis], minlength=nf) for name, (sc, fid, uu, vv, vis) in drawn.items()}
        best = {}
        for fi in range(nf):
            name = max(counts, key=lambda n: counts[n][fi])
            if counts[name][fi] > 0:
                best[fi] = name
        texels = {}
        for name, (scene, fid, uu, vv, vis) in drawn.items():
            crop = self.views[name][0]
            ys, xs = np.nonzero(vis)
            for y, x in zip(ys, xs):
                fi = fid[y, x]
                if best.get(fi) != name:
                    continue
                u, v = uu[y, x], vv[y, x]
                fu, fv = u - math.floor(u), v - math.floor(v)
                if 0.25 < fu < 0.75 and 0.25 < fv < 0.75:
                    key = (int(u), int(v))
                    s = texels.setdefault(key, [np.zeros(3), 0])
                    s[0] += crop[y, x]
                    s[1] += 1
        tex = {k: s / c for k, (s, c) in texels.items()}
        total, n = 0.0, 0
        for name, (scene, fid, uu, vv, vis) in drawn.items():
            crop, camw, pose, dep, bg = self.views[name]
            ys, xs = np.nonzero(vis)
            errs = []
            for y, x in zip(ys, xs):
                key = (int(uu[y, x]), int(vv[y, x]))
                if key in tex:
                    errs.append(np.abs(crop[y, x] - tex[key]).mean())
            outside = (vis & bg).sum()
            e = (sum(errs) + 80.0 * outside) / max(1, len(errs) + outside)
            care = self.care[name]
            ours = (dep < np.inf) | (fid >= 0)
            refm = ~bg
            iou_w = ((ours & refm & care).sum()) / max(1, ((ours | refm) & care).sum())
            e += self.iou_weight * (1.0 - iou_w)
            if verbose:
                print(name, "pixels", len(errs), "outside", int(outside), "iou", round(float(iou_w), 4), "err", round(e, 2))
            total += e
            n += 1
        return total / max(1, n)


OPPOSITE = {"front": "back", "back": "front", "left": "right", "right": "left", "top": "bottom", "bottom": "top"}


def finish_atlas(s, lifted, base, flip_opposite=True):
    """The set's final atlas: lifted texels where the references showed them; in a face only partly seen, the gaps
    filled from the nearest lifted texel of that face; a face never seen copied from its part's opposite face (turned
    as it would be seen from its own side), else filled with the part's commonest lifted colour, else left as painted.
    lifted, base: (H, W, 4) arrays (alpha 255 where lifted / painted)."""
    nets, size = am.layout(s)
    out = base.copy()
    have = lifted[..., 3] > 0
    out[have] = lifted[have]
    done = set()
    for item, bone, part in s.parts():
        if part.key in done or part.uvs:
            continue
        done.add(part.key)
        u, v, w, h, d = nets[part.key]
        rects = am.face_rects(w, h, d)
        seen_any = {}
        for face, (a0, b0, a1, b1) in rects.items():
            reg_have = have[v + b0:v + b1, u + a0:u + a1]
            seen_any[face] = reg_have.any()
            if reg_have.any() and not reg_have.all():
                # nearest lifted texel inside the face
                ys, xs = np.nonzero(reg_have)
                for y in range(b1 - b0):
                    for x in range(a1 - a0):
                        if not reg_have[y, x]:
                            k = int(np.argmin((ys - y) ** 2 + (xs - x) ** 2))
                            out[v + b0 + y, u + a0 + x] = out[v + b0 + ys[k], u + a0 + xs[k]]
        colours = []
        for face, (a0, b0, a1, b1) in rects.items():
            if seen_any[face]:
                colours.append(out[v + b0:v + b1, u + a0:u + a1, :3].reshape(-1, 3))
        for face, (a0, b0, a1, b1) in rects.items():
            if seen_any[face] or face in part.skip:
                continue
            opp = OPPOSITE[face]
            oa0, ob0, oa1, ob1 = rects[opp]
            if seen_any.get(opp) and (oa1 - oa0, ob1 - ob0) == (a1 - a0, b1 - b0):
                src = out[v + ob0:v + ob1, u + oa0:u + oa1].copy()
                if flip_opposite and face in ("front", "back", "left", "right"):
                    src = src[:, ::-1]
                out[v + b0:v + b1, u + a0:u + a1] = src
            elif colours:
                allc = np.concatenate(colours).astype(int)
                keys, counts = np.unique(allc // 8, axis=0, return_counts=True)
                common = allc[np.all(allc // 8 == keys[np.argmax(counts)], axis=1)].mean(axis=0)
                out[v + b0:v + b1, u + a0:u + a1, :3] = common
                out[v + b0:v + b1, u + a0:u + a1, 3] = 255
    return out


def views_of(set_name):
    """The references of a set that have fits, as names "<set>/<view>"."""
    return [f"{set_name}/{p.name[:-len('.fit.json')]}" for p in sorted((REFS / set_name).glob("*.fit.json"))]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("compare", help="each fitted reference beside our model from its camera")
    p.add_argument("--set", required=True)
    p.add_argument("--lighting", default="unlit")
    args = parser.parse_args()
    s = next(x for x in am.sets() if x.name == args.set)
    names = views_of(args.set)
    if not names:
        parser.error(f"no fitted references under {REFS / args.set}")
    views = []
    for name in names:
        h, w = ref(name).shape[:2]
        views.append((name, (0, 0, w, h)))
    OUT.mkdir(parents=True, exist_ok=True)
    print(compare_crops([s], views, OUT / f"{args.set}_compare.png", factor=1, lighting=args.lighting, edges=False))


if __name__ == "__main__":
    main()
