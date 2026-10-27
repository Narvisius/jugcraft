"""Preview of the Retro Trader's look, faces and stomp-clap, drawn without the game: a sheet and an animated GIF.

Run from the repository root:
    python3 tools/retro_trader_preview.py OUT_DIR [--base VILLAGER_PNG --type TYPE_PNG]

It poses the vanilla villager geometry like VillagerModel and RetroTraderModelMixin do, puts the overlay and the
expression sheet on it like the game's layers and RetroTraderLayer do, and follows the routine in
RetroTraderPose.java (keep the two in step). Without --base and --type (the vanilla villager.png and a biome type
from a Minecraft 26.3 client jar, which are not in this repository) the villager's own skin, which shows only on his
nose, is a flat tone. A preview, not a screenshot: it does not show the game's lighting.
"""
import argparse
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retro_trader_look import BEANIE  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
TEX = ROOT / "src" / "main" / "resources" / "assets" / "jugcraft" / "textures" / "entity"
OVERLAY = TEX / "villager" / "profession" / "retro_trader.png"
SHEET = TEX / "retro_trader" / "expressions.png"

# The routine (RetroTraderJam.java) and the poses (RetroTraderPose.java).
BEAT, INTRO, BEATS = 15, 12, 8
FINALE = INTRO + BEATS * BEAT
LENGTH = FINALE + 18
FOLDED, READY, CLAP, TA_DA = (-0.8, -0.75, 0.0), (-1.15, -0.22, 0.0), (-1.3, -0.58, 0.0), (-0.35, 0.0, 2.0)
LIFT = 3.2
NOSE_RAISE = 2.0  # RetroTraderPose.NOSE_RAISE
EYES = ["open", "half", "closed", "look_left", "look_right", "happy", "wide", "squeeze", "frown"]
MOUTHS = ["neutral", "smile", "grin", "talk", "shout", "ooh", "frown"]


def smooth(x):
    u = min(1.0, max(0.0, x))
    return u * u * (3 - 2 * u)


def lerp(a, b, t):
    return tuple(x + (y - x) * t for x, y in zip(a, b))


def beat_tick(beat):
    return FINALE if beat >= BEATS else INTRO + beat * BEAT


def beat_kind(tick):
    if tick == FINALE:
        return "finale"
    since = tick - INTRO
    if since < 0 or since % BEAT or since // BEAT >= BEATS:
        return None
    beat = since // BEAT
    return "clap" if beat % 2 else ("right" if beat % 4 == 0 else "left")


def lift(since):
    u = since + 7
    if u < 0 or u >= 7:
        return 0.0
    if u < 5:
        return LIFT * smooth(u / 5)
    fall = (u - 5) / 2
    return LIFT * (1 - fall * fall)


def clap(since):
    if -5 <= since < 0:
        return ((since + 5) / 5) ** 2
    if 0 <= since < 6:
        return 1 - smooth(since / 6)
    return 0.0


def arms(t):
    if t < INTRO:
        return lerp(FOLDED, READY, smooth(t / INTRO))
    if t < FINALE:
        together = max([clap(t - beat_tick(b)) for b in range(1, BEATS + 1, 2)] + [clap(t - FINALE)])
        return lerp(READY, CLAP, together)
    if t < FINALE + 6:
        return lerp(CLAP, TA_DA, smooth((t - FINALE) / 6))
    if t < FINALE + 10:
        return TA_DA
    return lerp(TA_DA, FOLDED, smooth(min(1.0, (t - FINALE - 10) / (LENGTH - FINALE - 10))))


def jam_pose(t):
    pose = {"arm": arms(t), "right": 0.0, "left": 0.0, "bounce": 0.0, "nod": 0.0, "sway": 0.0}
    for beat in range(BEATS + 1):
        at = beat_tick(beat)
        kind = beat_kind(at)
        stomp = kind in ("right", "left", "finale")
        if stomp:
            side = "left" if kind == "left" else "right"
            pose[side] = max(pose[side], lift(t - at))
        since = t - at
        if 0 <= since < 5:
            pose["bounce"] = max(pose["bounce"], (0.7 if stomp else 0.35) * (1 - since / 5) ** 2)
        if 0 <= since < 8:
            pose["nod"] = max(pose["nod"], 0.2 * math.exp(-since / 3))
    if INTRO <= t < FINALE:
        pose["sway"] = 0.07 * math.sin((t - INTRO) * math.pi / BEAT / 2)
    eyes, mouth = "happy", "grin"
    if t < 4:
        eyes = "wide"
    elif t >= FINALE - 3:
        eyes, mouth = ("wide", "shout") if t < FINALE + 12 else ("open", "smile")
    else:
        for beat in range(0, BEATS, 2):
            if -3 <= t - beat_tick(beat) < 7:
                eyes, mouth = ("squeeze" if beat % 4 == 0 else "wide"), "shout"
                break
    pose["eyes"], pose["mouth"] = eyes, mouth
    return pose


# ---------------------------------------------------------------- geometry

def rot(p, x=0.0, y=0.0, z=0.0):
    """A model part's rotation: x first, then y, then z (Quaternionf.rotationZYX)."""
    px, py, pz = p
    c, s = math.cos(x), math.sin(x)
    py, pz = py * c - pz * s, py * s + pz * c
    c, s = math.cos(y), math.sin(y)
    px, pz = px * c + pz * s, -px * s + pz * c
    c, s = math.cos(z), math.sin(z)
    px, py = px * c - py * s, px * s + py * c
    return px, py, pz


def faces(origin, size, uv, grow=0.0, mirror=False, tex=None):
    """A box's faces; tex gives the texture's whole-pixel size where it differs from the box's size."""
    (x, y, z), (u, v) = origin, uv
    x0, y0, z0 = x - grow, y - grow, z - grow
    x1, y1, z1 = x + size[0] + grow, y + size[1] + grow, z + size[2] + grow
    w, h, d = tex or size
    out = [((x0, y0, z0), (x1 - x0, 0, 0), (0, y1 - y0, 0), (u + d, v + d, w, h)),
           ((x1, y0, z1), (x0 - x1, 0, 0), (0, y1 - y0, 0), (u + 2 * d + w, v + d, w, h))]
    right = ((x0, y0, z1), (0, 0, z0 - z1), (0, y1 - y0, 0))
    left = ((x1, y0, z0), (0, 0, z1 - z0), (0, y1 - y0, 0))
    if mirror:
        right, left = left, right
    out += [(*right, (u, v + d, d, h)), (*left, (u + d + w, v + d, d, h)),
            ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1), (u + d, v, w, d)),
            ((x0, y1, z0), (x1 - x0, 0, 0), (0, 0, z1 - z0), (u + d + w, v, w, d))]
    return out


def texels(tex, face_list, transform, mirror=False):
    quads = []
    for corner, eu, ev, (tu, tv, tw, th) in face_list:
        for j in range(th):
            for i in range(tw):
                col = tex.getpixel((tu + (tw - 1 - i if mirror else i), tv + j))
                if col[3] < 16:
                    continue
                pts = [transform(tuple(corner[k] + eu[k] * a / tw + ev[k] * b / th for k in range(3)))
                       for a, b in ((i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1))]
                quads.append((pts, col))
    return quads


def part(pivot, angles=(0.0, 0.0, 0.0), parent=None):
    def transform(p):
        q = rot(p, *angles)
        q = (q[0] + pivot[0], q[1] + pivot[1], q[2] + pivot[2])
        return parent(q) if parent else q
    return transform


def model(skin, sheet, pose):
    """Texel quads of the posed villager: pose None is the plain villager with a face frame."""
    jam = pose.get("jam", False)
    bounce = pose.get("bounce", 0.0)
    head = part((0, bounce, 0), (pose.get("nod", 0.0), 0.0, pose.get("sway", 0.0)))
    body = part((0, bounce, 0))
    quads = []
    quads += texels(skin, faces((-4, -10, -4), (8, 10, 8), (0, 0)), head)
    quads += texels(skin, faces((-4, -10, -4), (8, 10, 8), (32, 0), 0.51), head)
    quads += texels(skin, faces((-1, -1, -6), (2, 4, 2), (24, 0)), part((0, -2 - NOSE_RAISE, 0), parent=head))
    quads += texels(skin, faces((-4, 0, -3), (8, 12, 6), (16, 20)), body)
    quads += texels(skin, faces((-4, 0, -3), (8, 20, 6), (0, 38), 0.5), body)
    for side, x in (("right", -2), ("left", 2)):
        up = pose.get(side, 0.0)
        leg = part((x, 12 - up, 0), (-0.03 * up, 0.0, 0.0))
        quads += texels(skin, faces((-2, 0, -2), (4, 12, 4), (0, 22), mirror=side == "left"), leg, mirror=side == "left")
    if jam:
        ax, ay, az = pose["arm"]
        for sign, uv in ((-1, (0, 32)), (1, (16, 32))):
            arm = part((6 * sign, 2 + bounce, -0.5), (ax, -sign * ay, -sign * az))
            quads += texels(sheet, faces((-2, -2, -2), (4, 10, 4), uv), arm)
    else:
        arms_part = part((0, 3, -1), (-0.75, 0.0, 0.0))
        quads += texels(skin, faces((-8, -2, -2), (4, 8, 4), (44, 22)), arms_part)
        quads += texels(skin, faces((4, -2, -2), (4, 8, 4), (44, 22), mirror=True), arms_part, mirror=True)
        quads += texels(skin, faces((-4, 2, -2), (8, 4, 4), (40, 38)), arms_part)
    # The beanie (RetroTraderLayer draws it on the head).
    for _, origin, size, tex, uv in BEANIE:
        quads += texels(sheet, faces(origin, size, uv, tex=tex), head)
    # The face, a hair in front of the head's front.
    e, m = EYES.index(pose["eyes"]), MOUTHS.index(pose["mouth"])
    z = -4.03
    quads += texels(sheet, [((-4, -10, z), (8, 0, 0), (0, 7, 0), (8 * (e % 8), 7 * (e // 8), 8, 7))], head)
    quads += texels(sheet, [((-4, -3, z), (8, 0, 0), (0, 3, 0), (8 * (m % 8), 16 + 3 * (m // 8), 8, 3))], head)
    return quads


def render(quads, yaw, pitch, scale, size):
    cy, sy, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)

    def project(p):
        x, y, z = p
        x, z = x * cy - z * sy, x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return x, y, z

    items = []
    for pts, col in quads:
        pp = [project(p) for p in pts]
        a = [pp[1][k] - pp[0][k] for k in range(3)]
        b = [pp[3][k] - pp[0][k] for k in range(3)]
        n = (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
        length = math.sqrt(sum(c * c for c in n)) or 1
        n = [c / length for c in n]
        if n[2] < -0.02:
            continue
        shade = 0.62 + 0.38 * max(0.0, -(n[0] * -0.4 + n[1] * -0.8 + n[2] * -0.45) / 1.07)
        items.append((sum(p[2] for p in pp) / 4, pp, tuple(int(col[k] * shade) for k in range(3)) + (255,)))
    items.sort(key=lambda item: -item[0])
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for _, pp, col in items:
        draw.polygon([(size / 2 + p[0] * scale, size * 0.55 + (p[1] - 6) * scale) for p in pp], fill=col)
    return img


def view(skin, sheet, pose, yaw=0.5, pitch=-0.3, size=300, scale=9):
    big = render(model(skin, sheet, pose), yaw, pitch, scale * 3, size * 3)
    return big.resize((size, size), Image.LANCZOS)


def skin_texture(base, type_):
    skin = Image.open(base).convert("RGBA") if base else Image.new("RGBA", (64, 64), (190, 140, 110, 255))
    if type_:
        layer = Image.open(type_).convert("RGBA")
        # The overlay's hat is "full": the biome type's own hat is not drawn.
        for box in ((32, 0, 64, 18), (30, 47, 64, 64)):
            layer.paste((0, 0, 0, 0), box)
        skin.alpha_composite(layer)
    skin.alpha_composite(Image.open(OVERLAY).convert("RGBA"))
    return skin


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("out", type=Path)
    parser.add_argument("--base")
    parser.add_argument("--type")
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    skin = skin_texture(args.base, args.type)
    sheet = Image.open(SHEET).convert("RGBA")
    bg = (30, 32, 40, 255)

    # 1. The look: front 3/4, front, back 3/4.
    look = Image.new("RGBA", (900, 330), bg)
    for i, yaw in enumerate((0.55, 0.0, math.pi + 0.6)):
        look.alpha_composite(view(skin, sheet, {"eyes": "open", "mouth": "neutral"}, yaw), (i * 300, 15))
    look.save(args.out / "retro_trader_look.png")

    # 2. The faces: every eye frame with a fitting mouth, close up.
    pairs = [("open", "neutral"), ("half", "neutral"), ("closed", "neutral"), ("look_left", "neutral"),
             ("look_right", "neutral"), ("open", "smile"), ("open", "talk"), ("happy", "grin"), ("open", "ooh"),
             ("wide", "shout"), ("squeeze", "shout"), ("frown", "frown")]
    faces_img = Image.new("RGBA", (6 * 160, 2 * 170), bg)
    for i, (eyes, mouth) in enumerate(pairs):
        tile = render(model(skin, sheet, {"eyes": eyes, "mouth": mouth}), 0.3, -0.12, 42, 1260).resize((420, 420), Image.LANCZOS)
        faces_img.alpha_composite(tile.crop((130, 0, 290, 170)), ((i % 6) * 160, (i // 6) * 170))
    faces_img.save(args.out / "retro_trader_faces.png")

    # 3. The stomp-clap: key frames on a strip, and the whole routine as a GIF at 20 frames a second.
    keys = [0, 6, INTRO - 2, INTRO, INTRO + 9, INTRO + BEAT, INTRO + 2 * BEAT, INTRO + 3 * BEAT, FINALE, FINALE + 8]
    strip = Image.new("RGBA", (len(keys) * 220, 300), bg)
    for i, t in enumerate(keys):
        frame = view(skin, sheet, {**jam_pose(t), "jam": True}, 0.45, -0.25, 260, 8)
        strip.alpha_composite(frame.crop((20, 0, 240, 260)), (i * 220, 20))
    strip.save(args.out / "retro_trader_jam_keys.png")
    frames = []
    for t in range(-10, LENGTH + 10):
        pose = {**jam_pose(t), "jam": True} if 0 <= t < LENGTH else {"eyes": "open", "mouth": "smile"}
        frame = Image.new("RGBA", (300, 300), bg)
        frame.alpha_composite(view(skin, sheet, pose, 0.45, -0.25, 300, 9))
        frames.append(frame.convert("P", palette=Image.ADAPTIVE))
    frames[0].save(args.out / "retro_trader_jam.gif", save_all=True, append_images=frames[1:], duration=50, loop=0)


if __name__ == "__main__":
    main()
