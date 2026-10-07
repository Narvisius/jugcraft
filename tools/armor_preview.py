"""Renders 3D worn armor (tools/armor_models.py) on a neutral grey player mannequin, to look at before the game does.

It draws the exported quads themselves (the worn_models.json entries, stored frame and all) the way the client layer
does: each bone's pose (ModelPart.translateAndRotate), a half turn about z, then the quads, textured from the painted
atlas with nearest-texel sampling, see-through texels cut out, no back-face culling, and a z-buffer. Faces are shaded
flat as Minecraft lights entities (two fixed lights: top 1.0, front and back 0.74, sides 0.5, bottom 0.4), or unlit.

    python3 tools/armor_preview.py                         every registered set: 4 views x 3 poses, and a sheet
    python3 tools/armor_preview.py --set steel_knight --poses walk --views front,right
    python3 tools/armor_preview.py --set steel_knight --compare path/to/owner_design.png
    python3 tools/armor_preview.py --set bloodthorn --compare path/to/owner_design.png
    python3 tools/armor_preview.py --set reforged_white_diamond --compare path/to/owner_design.png
                                                           a set with a REFERENCES layout gets each of the reference's
                                                           views beside ours from the same camera and in the same pose,
                                                           lit as the reference is (Blockbench's shading, or unlit) and
                                                           as the game does
    python3 tools/armor_preview.py --json vanguard_         entries already in worn_models.json (here the exosuit)
    python3 tools/armor_preview.py --set steel_knight --wearer --poses stand,walk,sneak
                                                           where the wearer shows through: the mannequin's skin green
                                                           and its outer layer (hat, jacket, sleeves, pants) magenta,
                                                           unlit, with the area that shows printed for each image
Images go to build/armor_preview/ (git-ignored). Views: front, back, right (the model's right side), left,
three_quarter (front-right, from a little above), top, bottom, and the Bloodthorn render's two: front_left (front
three-quarter from the model's left) and back_right (from behind, a little to its right). Poses: stand, walk (arms and
legs swung), sneak, owner (the owner's knight and White Diamond renders: arms 20 degrees out, head turned 17
degrees), joined with "+" (sneak+walk).
"""
import argparse
import functools
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import armor_models as am
import armor_paint

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/jugcraft/textures"
WORN = ROOT / "src/main/resources/assets/jugcraft/worn_models.json"
OUT = ROOT / "build/armor_preview"

BACKGROUND = (52, 55, 62)
SCALE = 20                    # image pixels per model pixel
VIEWS = {"front": (0, 0), "back": (180, 0), "right": (90, 0), "left": (270, 0), "three_quarter": (35, 15),
         "top": (0, 90), "bottom": (0, -90),
         # the owner's Bloodthorn render (REFERENCES): a front three-quarter from the model's left, and the back from a
         # little to its right, both from about head height
         "front_left": (-24, 0), "back_right": (162, 0)}   # (azimuth: 90 looks at the model's right side; elevation)
DEFAULT_VIEWS = ("front", "back", "right", "three_quarter")
DEFAULT_POSES = ("stand", "walk", "sneak")
# Minecraft's entity lights (Lighting.DIFFUSE_LIGHT_0/1) in world space, y up; the model faces north (-z).
LIGHTS = [np.array(v) / np.linalg.norm(v) for v in ((0.2, 1.0, -0.7), (-0.2, 1.0, 0.7))]

MANNEQUIN = {"mid_light": (128, 128, 128), "mid": (116, 116, 118), "seam": (72, 72, 76)}
# --wearer: the skin and its outer layer in colours no armor palette uses, so any of the wearer that shows is counted
WEARER = {"mid": (0, 255, 0), "mid_light": (255, 0, 255)}


@functools.lru_cache(maxsize=None)
def mannequin(wearer=False):
    """The neutral grey player, as (worn_models-style entries, atlas array): each bone's own box (wide arms), and eyes
    on the face so the front reads. `wearer`: the skin green, and the skin's outer layer round it (the hat 0.5 out,
    the jacket, sleeves and pants 0.25, as every player skin has) magenta, to see where the wearer shows through."""
    if wearer:
        parts = {bone: [am.around(f"skin_{bone}", bone, 0, paint=armor_paint.P("solid", tone="mid")),
                        am.around(f"layer_{bone}", bone, max(am.SKIN_SHELLS[bone]),
                                  paint=armor_paint.P("solid", tone="mid_light"))] for bone in am.BONES}
        skin = am.ArmorSet("mannequin_wearer", WEARER, {"mannequin": parts}, width=64, texture="mannequin_wearer")
    else:
        parts = {bone: [am.around(f"skin_{bone}", bone, 0, paint=_skin_paint(bone))] for bone in am.BONES}
        skin = am.ArmorSet("mannequin", MANNEQUIN, {"mannequin": parts}, width=64, texture="mannequin")
    return json.loads(json.dumps(am.set_quads(skin))), np.asarray(armor_paint.paint_atlas(skin))


def wearer_shown(image):
    """Model pixels squared of the wearer (skin or outer layer, as --wearer paints them) in an unlit render."""
    a = np.asarray(image).astype(int)
    hit = np.zeros(a.shape[:2], dtype=bool)
    for colour in WEARER.values():
        hit |= np.abs(a - np.array(colour)).sum(axis=-1) == 0
    return int(hit.sum())


def _skin_paint(bone):
    if bone == "head":
        return {"front": [armor_paint.P("solid", tone="mid_light"),
                          armor_paint.P("marks", rects=[(1, 4, 2, 1)], tone="seam", symmetric=True)],
                "*": armor_paint.P("solid", tone="mid_light")}
    return armor_paint.P("solid", tone="mid")


# limbSwingAmount: LivingEntity sets it to min(distance moved per tick x 4, 1), so about 0.86 walking (a leg swings
# 69 degrees) and 1 sprinting (80 degrees)
WALK = 0.86


# ---------------------------------------------------------------- poses (HumanoidModel.setupAnim, 1.21-era values)
def pose_bones(pose="stand", phase=0.0, amount=WALK):
    """{bone: (pivot, (xRot, yRot, zRot) radians)} in model space (y down, pivots in model pixels)."""
    bones = {bone: [list(am.PIVOTS[bone]), [0.0, 0.0, 0.0]] for bone in am.BONES}
    for name in pose.split("+"):
        if name == "stand":
            continue
        if name == "walk":   # limbSwing phase, limbSwingAmount `amount` (WALK walking, 1 sprinting)
            t = phase * 0.6662
            bones["right_arm"][1][0] += math.cos(t + math.pi) * amount
            bones["left_arm"][1][0] += math.cos(t) * amount
            bones["right_leg"][1][0] += math.cos(t) * 1.4 * amount
            bones["left_leg"][1][0] += math.cos(t + math.pi) * 1.4 * amount
        elif name == "sneak":
            bones["body"][1][0] += 0.5
            bones["right_arm"][1][0] += 0.4
            bones["left_arm"][1][0] += 0.4
            for leg in ("right_leg", "left_leg"):
                bones[leg][0][1], bones[leg][0][2] = 12.2, 4.0
            bones["head"][0][1] = 4.2
            bones["body"][0][1] = 3.2
            bones["right_arm"][0][1] = bones["left_arm"][0][1] = 5.2
        elif name == "owner":
            bones["right_arm"][1][2] += math.radians(20)
            bones["left_arm"][1][2] -= math.radians(20)
            bones["head"][1][1] -= math.radians(17)
        else:
            raise ValueError(f"unknown pose {name!r}: stand, walk, sneak, owner")
    return bones


def bone_matrix(pivot, rot):
    """4x4 from bone space to model space: translate to the pivot, then rotate (ModelPart.translateAndRotate)."""
    m = np.eye(4)
    m[:3, :3] = np.array(am.euler_matrix(tuple(math.degrees(r) for r in rot)))
    m[:3, 3] = pivot
    return m


# ---------------------------------------------------------------- quads
def bone_of(key):
    """The bone a worn_models.json key ends with ("steel_chestplate_right_arm" -> "right_arm")."""
    for bone in sorted(am.BONES, key=len, reverse=True):
        if key.endswith("_" + bone):
            return bone
    raise ValueError(f"{key}: no bone suffix")


class Textures:
    """Texture name (as in a quad) -> RGBA array; in-memory atlases first, then the mod's files."""

    def __init__(self, atlases=None):
        self.cache = {k: np.asarray(v.convert("RGBA")) for k, v in (atlases or {}).items()}

    def __getitem__(self, name):
        if name not in self.cache:
            path = TEXTURES / (f"{name}.png" if "/" in name else f"block/{name}.png")
            self.cache[name] = np.asarray(Image.open(path).convert("RGBA"))
        return self.cache[name]


def set_scene(armor_sets, items=None):
    """(entries, textures) for some sets: the exported quads (through JSON, as the file holds them) and atlases."""
    entries, atlases = {}, {}
    for s in armor_sets:
        quads = json.loads(json.dumps(am.set_quads(s)))
        for key, qs in quads.items():
            if items is None or any(key.startswith(item + "_") for item in items):
                entries[key] = qs
        atlases[s.texture] = armor_paint.paint_atlas(s)
    return entries, atlases


def world_quads(entries, pose, textures, phase=0.0, amount=WALK, body=True, wearer=False):
    """[(4x3 world points (y up, feet at 0, facing -z), 4x2 uvs, world normal, texture array)] for the mannequin and
    the entries, posed. `wearer`: the mannequin as mannequin(wearer=True) paints it."""
    bones = {b: bone_matrix(*v) for b, v in pose_bones(pose, phase, amount).items()}
    groups = [entries]
    if body:
        skin, atlas = mannequin(wearer)
        textures.cache.setdefault("mannequin_wearer" if wearer else "mannequin", atlas)
        groups.insert(0, skin)
    out = []
    for quads_by_key in groups:
        for key, quads in quads_by_key.items():
            m = bones[bone_of(key)]
            for q in quads:
                v = np.array(q["vertices"], dtype=float)
                local = np.stack([-v[:, 0], -v[:, 1], v[:, 2], np.ones(4)], axis=1)   # undo the stored half turn
                model = (local @ m.T)[:, :3]
                n = m[:3, :3] @ np.array([-q["normal"][0], -q["normal"][1], q["normal"][2]])
                world = np.stack([-model[:, 0], 24 - model[:, 1], model[:, 2]], axis=1)
                out.append((world, v[:, 3:5], np.array([-n[0], -n[1], n[2]]), textures[q["texture"]]))
    return out


# ---------------------------------------------------------------- camera and rasteriser
def camera(view):
    az, el = (math.radians(a) for a in (VIEWS[view] if isinstance(view, str) else view))
    d = np.array([math.sin(az) * math.cos(el), math.sin(el), -math.cos(az) * math.cos(el)])
    f = -d
    r = np.array([-math.cos(az), 0.0, -math.sin(az)])
    u = np.cross(r, f)
    return d, r, u, f


def project(points, view, perspective=None, target=(0.0, 16.0, 0.0)):
    """(screen x, screen y up, depth) in model pixels; perspective = camera distance from `target` (None: orthographic)."""
    d, r, u, f = camera(view)
    p = points - np.array(target)
    sx, sy, depth = p @ r, p @ u, p @ f
    if perspective:
        depth = depth + perspective
        k = perspective / np.maximum(depth, 1e-3)
        sx, sy = sx * k, sy * k
    return sx, sy + target[1], depth


def shade(normal, unlit=False, lighting="game"):
    """A face's brightness: the game's entity lights, or ("blockbench") the flat face shading of Blockbench's
    preview, measured on the owner's Bloodthorn render (top 1.0, bottom 0.5, front and back 0.8, sides 0.6; a turned
    face blends them), for comparing with a render made there."""
    if unlit:
        return 1.0
    n = normal / (np.linalg.norm(normal) or 1)
    if lighting == "blockbench":
        return float(n[0] ** 2 * 0.6 + n[1] ** 2 * (1.0 if n[1] > 0 else 0.5) + n[2] ** 2 * 0.8)
    return min(1.0, 0.4 + 0.6 * sum(max(0.0, float(n @ light)) for light in LIGHTS))


def frame_of(quads, views, perspective=None, margin=1.5):
    """(x0, x1, y0, y1) in model pixels framing every quad in every view."""
    xs, ys = [], []
    for view in views:
        for pts, *_ in quads:
            sx, sy, _ = project(pts, view, perspective)
            xs.extend(sx)
            ys.extend(sy)
    return min(xs) - margin, max(xs) + margin, min(ys) - margin, max(ys) + margin


def render(quads, view, scale=SCALE, frame=None, unlit=False, perspective=None, background=BACKGROUND,
           lighting="game"):
    """One view as an RGB image (lighting: see shade())."""
    x0, x1, y0, y1 = frame or frame_of(quads, [view], perspective)
    width, height = int(math.ceil((x1 - x0) * scale)), int(math.ceil((y1 - y0) * scale))
    color = np.empty((height, width, 3), dtype=np.float32)
    color[:, :] = background
    zbuf = np.full((height, width), np.inf)
    for pts, uv, normal, tex in quads:
        sx, sy, depth = project(pts, view, perspective)
        px, py = (sx - x0) * scale, (y1 - sy) * scale
        light = shade(normal, unlit, lighting)
        th, tw = tex.shape[:2]
        umin, umax = uv[:, 0].min() * tw + 1e-3, uv[:, 0].max() * tw - 1e-3
        vmin, vmax = uv[:, 1].min() * th + 1e-3, uv[:, 1].max() * th - 1e-3
        inv = 1.0 / depth if perspective else np.ones(4)
        for a, b, c in ((0, 1, 2), (0, 2, 3)):
            area = (px[b] - px[a]) * (py[c] - py[a]) - (py[b] - py[a]) * (px[c] - px[a])
            if abs(area) < 1e-9:
                continue
            bx0, bx1 = max(0, int(math.floor(min(px[a], px[b], px[c])))), min(width, int(math.ceil(max(px[a], px[b], px[c]))))
            by0, by1 = max(0, int(math.floor(min(py[a], py[b], py[c])))), min(height, int(math.ceil(max(py[a], py[b], py[c]))))
            if bx0 >= bx1 or by0 >= by1:
                continue
            gx, gy = np.meshgrid(np.arange(bx0, bx1) + 0.5, np.arange(by0, by1) + 0.5)
            wa = ((px[b] - gx) * (py[c] - gy) - (py[b] - gy) * (px[c] - gx)) / area
            wb = ((px[c] - gx) * (py[a] - gy) - (py[c] - gy) * (px[a] - gx)) / area
            wc = 1.0 - wa - wb
            inside = (wa >= -1e-7) & (wb >= -1e-7) & (wc >= -1e-7)
            if not inside.any():
                continue
            wsum = wa * inv[a] + wb * inv[b] + wc * inv[c]
            z = 1.0 / wsum if perspective else wa * depth[a] + wb * depth[b] + wc * depth[c]
            u = (wa * uv[a, 0] * inv[a] + wb * uv[b, 0] * inv[b] + wc * uv[c, 0] * inv[c]) / wsum * tw
            v = (wa * uv[a, 1] * inv[a] + wb * uv[b, 1] * inv[b] + wc * uv[c, 1] * inv[c]) / wsum * th
            tx = np.clip(np.floor(np.clip(u, umin, umax)).astype(int), 0, tw - 1)
            ty = np.clip(np.floor(np.clip(v, vmin, vmax)).astype(int), 0, th - 1)
            texel = tex[ty, tx]
            region = zbuf[by0:by1, bx0:bx1]
            draw = inside & (z < region) & (texel[..., 3] >= 26)   # cutout: alpha under 0.1 is discarded
            region[draw] = z[draw]
            color[by0:by1, bx0:bx1][draw] = texel[..., :3][draw] * light
    return Image.fromarray(np.clip(color + 0.5, 0, 255).astype(np.uint8), "RGB")


# ---------------------------------------------------------------- outputs
def label(image, text, height=22):
    out = Image.new("RGB", (image.width, image.height + height), BACKGROUND)
    out.paste(image, (0, height))
    ImageDraw.Draw(out).text((6, 5), text, fill=(230, 230, 230))
    return out


def sheet(images, columns, gap=8):
    """Images (same size) in a grid."""
    w, h = images[0].size
    rows = (len(images) + columns - 1) // columns
    out = Image.new("RGB", (columns * w + (columns + 1) * gap, rows * h + (rows + 1) * gap), (30, 32, 36))
    for i, image in enumerate(images):
        out.paste(image, (gap + (i % columns) * (w + gap), gap + (i // columns) * (h + gap)))
    return out


def render_all(name, entries, textures, out_dir, views=DEFAULT_VIEWS, poses=DEFAULT_POSES, scale=SCALE, unlit=False,
               perspective=None, phase=0.0, amount=WALK, wearer=False):
    """Every view in every pose as name_<pose>_<view>.png, and name_sheet.png (a row per pose). One frame for all of
    them, so they line up. Returns the written paths. `wearer`: the wearer-coloured mannequin, unlit, and the area of
    it that shows in each image printed."""
    out_dir.mkdir(parents=True, exist_ok=True)
    scenes = {pose: world_quads(entries, pose, textures, phase, amount, wearer=wearer) for pose in poses}
    frame = frame_of([q for qs in scenes.values() for q in qs], views, perspective)
    paths, tiles = [], []
    for pose in poses:
        for view in views:
            image = render(scenes[pose], view, scale, frame, unlit or wearer, perspective)
            path = out_dir / f"{name}_{pose.replace('+', '_')}_{view}.png"
            image.save(path)
            paths.append(path)
            text = f"{name}  {pose}  {view}"
            if wearer:
                shown = wearer_shown(image) / scale ** 2
                print(f"{name} {pose} {view}: wearer shows over {shown:.2f} model px2")
                text += f"  wearer {shown:.2f} px2"
            tiles.append(label(image, text))
    path = out_dir / f"{name}_sheet.png"
    sheet(tiles, len(views)).save(path)
    return paths + [path]


# Reference renders in more than one view, for --compare: set -> panels, each (label, crop box (x0, y0, x1, y1) of the
# reference image, view (azimuth, elevation), camera distance, camera target height above the feet, image px per
# model px at the target, (u, v) where the target lands in the image), then optionally a dict: the pose the reference
# shows ("pose", default compare_panels' own), how it is lit ("lighting": "blockbench", the default, or "unlit") and the
# colour behind ours ("background"). A set with none gets compare()'s front view.
REFERENCES = {
    # the owner's Bloodthorn render (671 x 633): a front three-quarter from the model's front left at about head
    # height, and the back from a little to the model's right; perspective cameras fitted to its two silhouettes
    "bloodthorn": (("front three-quarter", (40, 90, 345, 633), (-24, 0), 60, 30, 11.61, (181.69, 240.06)),
                   ("back", (346, 90, 651, 633), (162, 0), 60, 30, 11.09, (508.56, 250.45))),
    # the owner's Reforged White Diamond render (691 x 649): one front view at about head height, unlit, the figure in
    # the owner's pose (arms 20 degrees out, head turned 17); the camera fitted to its dark sleeves, then its scale and
    # offset to its silhouette
    "reforged_white_diamond": (("front", (30, 85, 420, 649), (0, 0), 60, 30, 12.645, (222.94, 219.94),
                                {"pose": "owner", "lighting": "unlit", "background": (117, 130, 188)}),),
}


def compare_panels(entries, textures, owner, path, panels, pose="stand"):
    """A reference image's panels, each beside ours from its camera at its scale: lit as the reference's renderer
    lights faces (Blockbench's flat shading, or none for an unlit render) and lit as the game does. One row per panel;
    a panel's options (REFERENCES) may set the pose it shows, its lighting and the background behind ours."""
    ref = Image.open(owner).convert("RGB")
    posed = {}
    tiles = []
    for name, (x0, y0, x1, y1), view, distance, height, k, (u, v), *extra in panels:
        options = extra[0] if extra else {}
        shown = options.get("pose", pose)
        if shown not in posed:
            posed[shown] = world_quads(entries, shown, textures)
        lift = np.array([0.0, height - 16.0, 0.0])   # project() aims at (0, 16, 0)
        quads = [(pts - lift, uv, n, tex) for pts, uv, n, tex in posed[shown]]
        left, top = -u / k, 16.0 + v / k
        frame = (left + x0 / k, left + x1 / k, top - y1 / k, top - y0 / k)
        background = tuple(options.get("background", (48, 50, 58)))
        tiles.append(label(ref.crop((x0, y0, x1, y1)), f"owner's design, {name}"))
        for lighting, text in ((options.get("lighting", "blockbench"), "ours, lit as their render"),
                               ("game", "ours, lit as in game")):
            unlit = lighting == "unlit"
            tiles.append(label(render(quads, view, k, frame, unlit, distance, background=background,
                                      lighting="game" if unlit else lighting), text))
    sheet(tiles, 3).save(path)
    return path


def compare(entries, textures, owner, path, pose="owner", perspective=None):
    """The owner's design image beside our front view at the same scale (16 image pixels per model pixel, feet at the
    same height), unlit like their render, and lit as the game lights it."""
    ref = Image.open(owner).convert("RGB")
    feet_y, centre_x, k = 587, 316, 16   # the owner's render: feet at y 587, body centred at x 316 (DESIGN_NOTES)
    quads = world_quads(entries, pose, textures)
    frame = (-centre_x / k, (ref.width - centre_x) / k, -(ref.height - feet_y) / k, feet_y / k)
    ours = [render(quads, "front", k, frame, unlit, perspective, background=(48, 50, 58)) for unlit in (True, False)]
    tiles = [label(ref, "owner's design"), label(ours[0], "ours, unlit (as their render)"),
             label(ours[1], "ours, lit as in game")]
    sheet(tiles, 3).save(path)
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--set", action="append", help="a registered set's name (default: every set)")
    parser.add_argument("--json", metavar="PREFIX", help="render worn_models.json entries whose keys start with PREFIX")
    parser.add_argument("--items", help="comma-separated item ids to wear (default: all of the set's)")
    parser.add_argument("--views", default=",".join(DEFAULT_VIEWS))
    parser.add_argument("--poses", default=",".join(DEFAULT_POSES))
    parser.add_argument("--scale", type=float, default=SCALE)
    parser.add_argument("--phase", type=float, default=0.0, help="walk cycle position (limbSwing)")
    parser.add_argument("--amount", type=float, default=WALK,
                        help="walk swing (limbSwingAmount: 0.86 walking, 1 sprinting)")
    parser.add_argument("--unlit", action="store_true")
    parser.add_argument("--perspective", type=float, help="camera distance in model pixels (default orthographic)")
    parser.add_argument("--compare", metavar="PNG", help="the owner's design image: write a side-by-side sheet")
    parser.add_argument("--wearer", action="store_true",
                        help="paint the mannequin's skin and outer layer in loud colours, unlit, and print how much shows")
    parser.add_argument("--out", type=Path, default=OUT)
    args = parser.parse_args()
    views, poses = args.views.split(","), args.poses.split(",")
    items = args.items.split(",") if args.items else None
    jobs = []
    if args.json:
        data = json.loads(WORN.read_text(encoding="utf-8"))
        entries = {k: v for k, v in data.items() if k.startswith(args.json) and k != "rocket_pack"}
        jobs.append((args.json.rstrip("_"), entries, Textures()))
    else:
        chosen = [s for s in am.sets() if not args.set or s.name in args.set]
        if args.set and len(chosen) != len(args.set):
            parser.error(f"unknown set in {args.set}; registered: {[s.name for s in am.sets()]}")
        for s in chosen:
            entries, atlases = set_scene([s], items)
            jobs.append((s.name, entries, Textures(atlases)))
    if not jobs:
        print("Nothing to render: no armor sets are registered (armor_models.SET_MODULES); try --json vanguard_")
    for name, entries, textures in jobs:
        for path in render_all(name, entries, textures, args.out / name, views, poses, args.scale, args.unlit,
                               args.perspective, args.phase, args.amount, args.wearer):
            print(path.relative_to(ROOT) if path.is_relative_to(ROOT) else path)
        if args.compare and name in REFERENCES:
            print(compare_panels(entries, textures, args.compare, args.out / name / f"{name}_compare.png",
                                 REFERENCES[name]))
        elif args.compare:
            print(compare(entries, textures, args.compare, args.out / name / f"{name}_compare.png",
                          perspective=args.perspective))


if __name__ == "__main__":
    main()
