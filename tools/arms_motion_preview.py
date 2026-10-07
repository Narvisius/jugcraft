"""Preview sheets for tools/arms_motion.py (a development aid; nothing it draws ships): the player model as boxes, posed
by the same kinematics as client/arms/ArmsMotion.java, at frames through each clip, from the front-right and from the
side, with the weapon held. Run: python3 tools/arms_motion_preview.py OUT_DIR [kind ...]
(with first-person sheets: the arm's own sprite, placed as FirstPersonHandsAndItemsRenderer places it)."""
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

import arms_motion as am
import arms_moves

BOXES = {"head": ((-4, -8, -4), (8, 8, 8)), "body": ((-4, 0, -2), (8, 12, 4)),
         "right_arm": ((-3, -2, -2), (4, 12, 4)), "left_arm": ((-1, -2, -2), (4, 12, 4)),
         "right_leg": ((-2, 0, -2), (4, 12, 4)), "left_leg": ((-2, 0, -2), (4, 12, 4))}
COLOURS = {"head": (226, 186, 150), "body": (70, 110, 160), "right_arm": (226, 186, 150), "left_arm": (210, 170, 136),
           "right_leg": (60, 60, 110), "left_leg": (52, 52, 96)}


class Camera:
    def __init__(self, yaw, pitch, scale, centre):
        self.r = am.mul(am._r("x", pitch), am._r("y", yaw + 180))
        self.scale = scale
        self.cx, self.cy = centre

    def project(self, p):
        # Model space is y down and faces -z; turn it so the viewer looks at the model's front.
        q = am.mv(self.r, (p[0], p[1], -p[2]))
        return (self.cx - q[0] * self.scale, self.cy + q[1] * self.scale, q[2])


def box_faces(origin, size, pivot, rot):
    x0, y0, z0 = origin
    sx, sy, sz = size
    corners = [(x0 + dx * sx, y0 + dy * sy, z0 + dz * sz) for dx in (0, 1) for dy in (0, 1) for dz in (0, 1)]
    world = [tuple(pivot[i] + am.mv(rot, c)[i] for i in range(3)) for c in corners]
    faces = [(0, 1, 3, 2), (4, 5, 7, 6), (0, 1, 5, 4), (2, 3, 7, 6), (0, 2, 6, 4), (1, 3, 7, 5)]
    return [[world[i] for i in f] for f in faces]


def draw_pose(draw, cam, pose, length):
    parts = am.skeleton(pose)
    polys = []
    for bone, (origin, size) in BOXES.items():
        pivot, rot = parts[bone]
        for face in box_faces(origin, size, pivot, rot):
            pts = [cam.project(p) for p in face]
            depth = sum(p[2] for p in pts) / 4
            normal_z = (pts[1][0] - pts[0][0]) * (pts[2][1] - pts[0][1]) - (pts[1][1] - pts[0][1]) * (pts[2][0] - pts[0][0])
            shade = 0.65 + 0.35 * min(1.0, abs(normal_z) / (cam.scale * cam.scale * 30))
            colour = tuple(int(c * shade) for c in COLOURS[bone])
            polys.append((depth, [(p[0], p[1]) for p in pts], colour))
    # Eyes on the head's front (-z), and a belt buckle on the body's, to tell front from back.
    hpivot, hrot = parts["head"]
    for ex in (-2.5, 1.5):
        quad = [(ex, -4.5, -4.05), (ex + 1, -4.5, -4.05), (ex + 1, -3.5, -4.05), (ex, -3.5, -4.05)]
        pts = [cam.project(tuple(hpivot[i] + am.mv(hrot, c)[i] for i in range(3))) for c in quad]
        polys.append((sum(p[2] for p in pts) / 4 - 0.01, [(p[0], p[1]) for p in pts], (30, 30, 60)))
    bpivot, brot = parts["body"]
    quad = [(-1, 9, -2.05), (1, 9, -2.05), (1, 11, -2.05), (-1, 11, -2.05)]
    pts = [cam.project(tuple(bpivot[i] + am.mv(brot, c)[i] for i in range(3))) for c in quad]
    polys.append((sum(p[2] for p in pts) / 4 - 0.01, [(p[0], p[1]) for p in pts], (220, 190, 60)))
    butt, grip, tip = am.weapon(pose, parts, length)
    b, g, t = cam.project(butt), cam.project(grip), cam.project(tip)
    weapon_depth = (b[2] + t[2]) / 2
    polys.append((weapon_depth, None, ((b[0], b[1]), (g[0], g[1]), (t[0], t[1]))))
    for depth, pts, colour in sorted(polys, key=lambda p: -p[0]):
        if pts is None:
            (bx, by), (gx, gy), (tx, ty) = colour
            draw.line([(bx, by), (gx, gy)], fill=(92, 60, 30), width=4)
            draw.line([(gx, gy), (tx, ty)], fill=(205, 215, 230), width=4)
            draw.ellipse([tx - 3, ty - 3, tx + 3, ty + 3], fill=(240, 80, 60))
        else:
            draw.polygon(pts, fill=colour, outline=(20, 20, 24))


def sheet(kind, out, frames=9):
    moves = arms_moves.MOVES[kind]
    length = arms_moves.LENGTH[kind]
    clips = list(moves["attacks"])
    rows = [("hold", None)] + [(c.name, c) for c in clips] + ([("use", moves["use"])] if moves.get("use") else [])
    cell = 150
    img = Image.new("RGB", (cell * frames, cell * 2 * len(rows) + 14 * len(rows)), (38, 40, 46))
    draw = ImageDraw.Draw(img)
    y = 0
    for name, clip in rows:
        draw.text((4, y + 1), f"{kind}: {name}", fill=(230, 230, 230))
        y += 14
        for view, (yaw, pitch) in enumerate(((-35, 12), (-95, 6))):
            for f in range(frames):
                t = f / (frames - 1)
                pose = moves["hold"] if clip is None else (am.evaluate(clip, t) if isinstance(clip, am.Clip) else clip)
                if moves.get("two_handed"):
                    pose = am.two_handed(pose, length, moves["two_handed"])
                cam = Camera(yaw, pitch, 3.2, (f * cell + cell / 2, y + view * cell + cell * 0.38))
                draw_pose(draw, cam, pose, length)
                if view == 0 and clip is not None:
                    draw.text((f * cell + 4, y + 2), f"{t:.2f}", fill=(160, 160, 170))
        y += 2 * cell
    path = Path(out) / f"{kind}.png"
    img.save(path)
    return path




def poses(named, path, length=20):
    """A grid of named poses, front-right above and side below (for tuning keys)."""
    cell = 150
    img = Image.new("RGB", (cell * len(named), cell * 2 + 14), (38, 40, 46))
    draw = ImageDraw.Draw(img)
    for i, (name, pose) in enumerate(named):
        draw.text((i * cell + 4, 1), name, fill=(230, 230, 230))
        for view, (yaw, pitch) in enumerate(((-35, 12), (-95, 6))):
            cam = Camera(yaw, pitch, 3.2, (i * cell + cell / 2, 14 + view * cell + cell * 0.38))
            draw_pose(draw, cam, pose, length)
    img.save(path)
    return path


# ---------------------------------------------------------------- first person

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "jugcraft"
FP_W, FP_H = 320, 180


def _rx(d):
    return am._r("x", d)


def _ry(d):
    return am._r("y", d)


def _rz(d):
    return am._r("z", d)


class Affine:
    """A PoseStack entry: a 3x3 matrix and an offset, composed as the game composes them (in blocks)."""

    def __init__(self, m=None, t=(0.0, 0.0, 0.0)):
        self.m = m or ((1, 0, 0), (0, 1, 0), (0, 0, 1))
        self.t = t

    def translate(self, x, y, z):
        d = am.mv(self.m, (x, y, z))
        return Affine(self.m, tuple(self.t[i] + d[i] for i in range(3)))

    def rotate(self, r):
        return Affine(am.mul(self.m, r), self.t)

    def scale(self, s):
        return Affine(tuple(tuple(c * s[j] for j, c in enumerate(row)) for row in self.m), self.t)

    def apply(self, p):
        q = am.mv(self.m, p)
        return tuple(self.t[i] + q[i] for i in range(3))


def display(kind, context="firstperson_righthand"):
    model = json.loads((ASSETS / "models" / "item" / f"arms_{kind}.json").read_text())
    d = model["display"][context]
    return d["rotation"], d["translation"], d["scale"]


def fp_vanilla(stack, p, side=1):
    """FirstPersonHandsAndItemsRenderer.swingArm (whack) as 26.3 does it."""
    f = -0.4 * math.sin(math.sqrt(p) * math.pi)
    g = 0.2 * math.sin(math.sqrt(p) * math.pi * 2)
    h = -0.2 * math.sin(p * math.pi)
    stack = stack.translate(side * f, g, h)
    i = math.sin(p * p * math.pi)
    j = math.sin(math.sqrt(p) * math.pi)
    stack = stack.rotate(_ry(side * (45 + i * -20)))
    stack = stack.rotate(_rz(side * j * -20))
    stack = stack.rotate(_rx(j * -80))
    return stack.rotate(_ry(side * -45))


def fp_ours(stack, fp, side=1):
    """ArmsMotion.applyFirstPerson: offset (pixels), then the turn in ModelPart's Z*Y*X order."""
    rx, ry, rz, x, y, z = fp
    stack = stack.translate(side * x / 16, y / 16, z / 16)
    return stack.rotate(am.zyx(rx, side * ry, side * rz))


def fp_frame(draw, ox, oy, kind, sprite, motion, equip=0.0):
    """One first-person frame in a FP_W x FP_H cell at (ox, oy): `motion` turns the stack after vanilla's arm place."""
    stack = Affine().translate(0.56, -0.52 + equip * -0.6, -0.72)
    stack = motion(stack)
    rot, tr, sc = display(kind)
    stack = stack.translate(tr[0] / 16, tr[1] / 16, tr[2] / 16)
    rxyz = am.mul(am.mul(_rx(rot[0]), _ry(rot[1])), _rz(rot[2]))
    stack = stack.rotate(rxyz).scale(sc).translate(-0.5, -0.5, -0.5)
    focal = (FP_H / 2) / math.tan(math.radians(35))
    w, h = sprite.size
    polys = []
    px = sprite.load()
    for v in range(h):
        for u in range(w):
            r, g, b, a = px[u, v]
            if a < 128:
                continue
            corners = [(u / w, 1 - v / h), ((u + 1) / w, 1 - v / h), ((u + 1) / w, 1 - (v + 1) / h), (u / w, 1 - (v + 1) / h)]
            pts = []
            depth = 0.0
            for cx, cy in corners:
                p = stack.apply((cx, cy, 8.5 / 16))
                if p[2] > -0.05:
                    pts = None
                    break
                pts.append((ox + FP_W / 2 + focal * p[0] / -p[2], oy + FP_H / 2 - focal * p[1] / -p[2]))
                depth += p[2]
            if pts:
                polys.append((depth, pts, (r, g, b)))
    for _d, pts, colour in sorted(polys, key=lambda q: q[0]):
        draw.polygon(pts, fill=colour)


def fp_sheet(kind, out, metal="steel", frames=10):
    """First person: vanilla's swing above, then each of the kind's attacks, from the guard."""
    moves = arms_moves.MOVES[kind]
    sprite = Image.open(ASSETS / "textures" / "item" / f"{metal}_{kind}.png").convert("RGBA")
    rows = [("vanilla", lambda p: (lambda s: fp_vanilla(s, p)))]
    rows.append(("hold", lambda p, h=moves["fp_hold"]: (lambda s: fp_ours(s, h))))
    for c in moves["attacks"]:
        rows.append((c.name, lambda p, c=c: (lambda s: fp_ours(s, am.evaluate_fp(c, p)))))
    cw, ch = FP_W * 7 // 10, FP_H * 7 // 10
    img = Image.new("RGB", (cw * frames, (ch + 12) * len(rows)), (90, 120, 160))
    for r, (name, make) in enumerate(rows):
        for f in range(frames):
            p = f / (frames - 1)
            cell = Image.new("RGB", (FP_W, FP_H), (110, 150, 200))
            d = ImageDraw.Draw(cell)
            d.rectangle([0, FP_H * 0.62, FP_W, FP_H], fill=(90, 140, 70))
            fp_frame(d, 0, 0, kind, sprite, make(p))
            d.line([(FP_W / 2 - 4, FP_H / 2), (FP_W / 2 + 4, FP_H / 2)], fill=(255, 255, 255))
            d.line([(FP_W / 2, FP_H / 2 - 4), (FP_W / 2, FP_H / 2 + 4)], fill=(255, 255, 255))
            ImageDraw.Draw(cell).text((4, 4), f"{p:.2f}", fill=(255, 255, 255))
            img.paste(cell.resize((cw, ch)), (f * cw, r * (ch + 12) + 12))
        ImageDraw.Draw(img).text((3, r * (ch + 12)), f"{kind}: {name}", fill=(255, 255, 255))
    path = Path(out) / f"{kind}_fp.png"
    img.save(path)
    return path


if __name__ == "__main__":
    out = sys.argv[1]
    Path(out).mkdir(parents=True, exist_ok=True)
    for kind in (sys.argv[2:] or arms_moves.MOVES):
        print(sheet(kind, out))
        if not arms_moves.MOVES[kind].get("body_only"):
            print(fp_sheet(kind, out))
