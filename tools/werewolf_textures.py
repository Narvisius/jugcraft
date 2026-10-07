"""Original textures for full-moon werewolves (fall addition 23) (requires Pillow). For each kind of werewolf (brown, snow
and shadow) its fur, painted at TEXTURE_SCALE times the model's 128 by 128 layout (512 by 512) box by box and face by
face from tools/werewolf_model.py with tools/fur_paint.py: fur in locks, lighter on top and darker beneath, a paler blaze
down the chest, a shaggy mane cut into points, a big wet nose, a red mouth with rows of teeth and pointed fangs, glaring
eyes under a heavy brow, inner ears, pointed ivory claws and dark pads; its glowing eyes alone; its rug (the pelt and its
head) and its pelt as an item at 64 by 64; wolfsbane (a hooded violet-blue raceme over palmate leaves), the silver dagger
and the silver arrow at 16 by 16.

Called from crop_textures.crop_textures(). Every pixel is drawn here by code, from a fixed seed; no Mojang texture, nor
any other texture, is read, traced or recoloured.
"""
import random

from PIL import Image

from agriculture import WEREWOLF, WOLFSBANE
from crop_textures import rgb
from fur_paint import Painter, mix, ramp
from werewolf_model import PARTS, TEXTURE_SCALE, TEXTURE_SIZE

# Each kind: fur from darkest to lightest, its paler belly and muzzle, inner ears (dark to light), eyes (the iris from its
# dark rim to its bright point, for the glowing layer too), the socket, and the nose (dark to its wet highlight).
KINDS = {
    "brown": {"fur": [rgb("140b06"), rgb("22150c"), rgb("332013"), rgb("462c1a"), rgb("5a3a23"), rgb("704a2e"), rgb("88603e")],
              "belly": [rgb("5a3e2a"), rgb("74563c"), rgb("8f6e4e"), rgb("a8875f")], "ear": [rgb("3c201a"), rgb("6a3c34"), rgb("8a5248")],
              "eye": [rgb("4a0404"), rgb("a80c0c"), rgb("f02a1a"), rgb("ffc4a0")], "socket": rgb("0c0604"),
              "nose": [rgb("070505"), rgb("1c1614"), rgb("5a4e4a")]},
    "snow": {"fur": [rgb("6c737b"), rgb("878e96"), rgb("a2a8ae"), rgb("bcc1c6"), rgb("d4d7db"), rgb("e7e9eb"), rgb("f8f9fa")],
             "belly": [rgb("d2d6da"), rgb("e2e5e8"), rgb("f0f1f2"), rgb("fcfcfd")], "ear": [rgb("8a6a70"), rgb("b48c94"), rgb("d8b4ba")],
             "eye": [rgb("4a0606"), rgb("a81010"), rgb("f2301e"), rgb("ffc8a8")], "socket": rgb("3a3438"),
             "nose": [rgb("0e0c0e"), rgb("262226"), rgb("6a6468")]},
    "shadow": {"fur": [rgb("0a0a0c"), rgb("16161a"), rgb("222227"), rgb("2f2f35"), rgb("3e3e46"), rgb("50505a"), rgb("66666f")],
               "belly": [rgb("2a2a30"), rgb("37373e"), rgb("46464e"), rgb("575760")], "ear": [rgb("2a1c1c"), rgb("4a3232"), rgb("644444")],
               "eye": [rgb("4a2c00"), rgb("c07c06"), rgb("ffbc28"), rgb("fff0b0")], "socket": rgb("030303"),
               "nose": [rgb("040404"), rgb("141414"), rgb("4a4a4e")]},
}
# Each box the texture paints, by its role, as (u, v, width, height, depth) from tools/werewolf_model.py.
_ROLES = {"chest": ("body", 1), "waist": ("body", 0), "hump": ("body", 2), "ruff": ("body", 3), "skull": ("head", 0),
          "muzzle": ("head", 1), "cheek": ("head", 2), "fang": ("head", 4), "jaw": ("jaw", 0), "lower_fang": ("jaw", 1), "ear": ("right_ear", 0), "ear_tip": ("right_ear", 1),
          "shoulder": ("right_arm", 0), "upper_arm": ("right_arm", 1), "forearm": ("right_forearm", 0), "hand": ("right_hand", 0),
          "claw": ("right_hand", 1), "thigh": ("right_leg", 0), "shin": ("right_shin", 0), "hock": ("right_hock", 0),
          "paw": ("right_paw", 0), "tail": ("tail", 0), "tail_mid": ("tail_mid", 0), "tail_tip": ("tail_tip", 0)}
BOXES = {}
for _role, (_part, _index) in _ROLES.items():
    _u, _v, _x, _y, _z, _w, _h, _d, _m = next(p for p in PARTS if p["name"] == _part)["boxes"][_index]
    BOXES[_role] = (_u, _v, _w, _h, _d)
MOUTH = [rgb("4a0c0c"), rgb("6e1414"), rgb("962222"), rgb("b23a3a")]
TOOTH = [rgb("bdb49c"), rgb("e2d9c0"), rgb("f4eedc")]
CLAW = [rgb("8a8068"), rgb("d8ceb2"), rgb("efe8d4")]
PAD = rgb("1a1412")
SILVER = [rgb("6e7480"), rgb("9aa2ae"), rgb("c4ccd6"), rgb("eef2f6")]
LEATHER = [rgb("3e2414"), rgb("5a361e")]
SHAFT = [rgb("7a5a32"), rgb("9a7444")]
FLETCH = [rgb("d8d8d8"), rgb("b0b0b0")]
STEM = [rgb("2f5a24"), rgb("3e7230"), rgb("50883c")]
HOOD = [rgb("2c2470"), rgb("3e34a0"), rgb("5a4cc8"), rgb("7e70e0")]


def faces(u, v, w, h, d):
    """A box's faces in its unwrapped texture, as (x, y, width, height). On top and bottom the last row is the front
    edge; on the right-hand side (x at the box's start) the last column is the front, on the left-hand side the first."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


SCALE = TEXTURE_SCALE


def R(rect, scale=SCALE):
    """A rect in model pixels, on the painted texture."""
    x, y, w, h = rect
    return x * scale, y * scale, w * scale, h * scale


SIDES = ("front", "back", "left", "right")


def fur_box(paint, box, colours, level=0.5, ragged=False, top_flow=(0.0, -1.0), density=1.4):
    """Fur over every face of a box: lighter on top, darker beneath and behind, laid on in locks flowing down its sides
    and back over its top, with fine hairs over them; a ragged box's sides end in points of fur."""
    for name, rect in faces(*box).items():
        r = R(rect)
        lv = level + {"top": 0.12, "bottom": -0.2, "back": -0.06}.get(name, 0.0)
        flow = top_flow if name == "top" else (0.0, 1.0)
        paint.shade(r, colours, lv - 0.06, spread=0.4, light="centre" if name in ("top", "bottom") else "top")
        paint.locks(r, colours, lv, flow=flow, density=0.45 * density, length=(12, 22) if ragged else (9, 17), width=(4.0, 7.0),
                    spread=0.09, light=0.18)
        paint.strands(r, colours, lv + 0.1, flow=flow, density=0.06, length=(4, 8), spread=0.12)
        if ragged and name in SIDES:
            paint.ragged(r, depth=7, every=5)


def blaze(paint, rect, colours, width_top, width_bottom, level=0.55):
    """A paler patch down the middle of a face, narrowing from `width_top` to `width_bottom` (fractions of its width)."""
    x0, y0, w, h = rect
    for y in range(h):
        half = w * (width_top + (width_bottom - width_top) * y / max(1, h - 1)) / 2.0
        for x in range(w):
            if paint.clean:
                # Clean: one crisp pale band, a step lighter down its middle.
                d = abs(x + 0.5 - w / 2.0)
                if d <= half:
                    paint.put(x0 + x, y0 + y, ramp(colours, level + (0.08 if d < half * 0.5 else -0.04)))
                continue
            # Feathered: the pale fur thins out into the dark over a few pixels, unevenly.
            d = abs(x + 0.5 - w / 2.0) + 4.0 * (paint.noise(x0 + x, y0 + y, 5.0) - 0.5)
            if d <= half + 4:
                alpha = max(0.0, min(1.0, (half + 4 - d) / 8.0))
                paint.put(x0 + x, y0 + y, ramp(colours, level - 0.12 * d / max(1.0, half) + 0.1 * (paint.noise(x0 + x, y0 + y) - 0.5)), alpha)
    inner = (int(x0 + w / 2.0 - w * width_top / 2.0), y0, max(2, int(w * width_top)), h)
    paint.locks(inner, colours, level, density=0.9, length=(5, 10), width=(1.6, 3.0), spread=0.12)


def teeth_row(paint, x0, x1, y, size, down=True, gap=1):
    """A row of pointed teeth from x0 to x1 along y, pointing down (or up)."""
    x = x0 + size / 2.0
    while x <= x1 - size / 2.0:
        paint.tooth(x, y, size, int(size * 1.4), TOOTH, down=down)
        x += size + gap


def werewolf(kind, eyes_only=False):
    """One kind of werewolf's fur, painted at TEXTURE_SCALE times its model's texture size (or, eyes_only, just its
    glowing eyes for the eyes layer)."""
    pal = KINDS[kind]
    width, height = TEXTURE_SIZE
    paint = Painter(width * SCALE, height * SCALE, {"brown": 23101, "snow": 23102, "shadow": 23103}[kind], clean=True)
    glow = Painter(width * SCALE, height * SCALE, 0)
    fur, belly = pal["fur"], pal["belly"]

    def f(role, name):
        return R(faces(*BOXES[role])[name])

    # The body: a barrel chest with a paler blaze down its front, a darker waist, the shaggy hump of mane and the ruff.
    fur_box(paint, BOXES["chest"], fur, 0.5)
    blaze(paint, f("chest", "front"), belly, 0.55, 0.25)
    fur_box(paint, BOXES["waist"], fur, 0.4)
    blaze(paint, f("waist", "front"), belly, 0.4, 0.3, level=0.45)
    fur_box(paint, BOXES["hump"], fur, 0.55, ragged=True, density=1.8)
    x0, y0, w, h = f("hump", "top")
    paint.strands((x0, y0, w, h), fur, 0.3, flow=(0.0, -1.0), density=0.6, length=(8, 16))
    fur_box(paint, BOXES["ruff"], fur, 0.6, ragged=True, density=1.8)
    blaze(paint, f("ruff", "front"), belly, 0.9, 0.7, level=0.6)
    paint.tufts(f("ruff", "front"), belly, 0.5, depth=6)

    # The skull: a heavy brow over eyes deep in their sockets, a paler bridge down to the muzzle.
    fur_box(paint, BOXES["skull"], fur, 0.5)
    x0, y0, w, h = f("skull", "front")
    blaze(paint, (x0 + w // 2 - 5, y0 + 8, 10, h - 8), belly if kind != "shadow" else fur, 0.9, 0.7, level=0.6)
    for side in (-1, 1):
        cx = x0 + w / 2.0 + side * w * 0.28
        cy = y0 + 10.5
        # The socket, then the iris with a slit pupil and a glint, glowing too.
        paint.ellipse(cx, cy, 5.2, 3.2, pal["socket"])
        for p in (paint, glow):
            p.blob(cx, cy, 3.6, 2.2, pal["eye"][1], pal["eye"][2])
            p.ellipse(cx, cy + 0.2, 0.7, 1.9, pal["eye"][0])
            p.glint(int(cx - side * 1.4 - 1), int(cy - 1), pal["eye"][3])
        # The brow, heavy and angled down towards the snout, with light catching its ridge.
        inner = cx - side * 6
        outer = cx + side * 6
        paint.line(outer, cy - 3.5, inner, cy - 1.5, ramp(fur, 0.05), width=2.6)
        paint.line(outer, cy - 5.2, inner, cy - 3.2, ramp(fur, 0.75), width=1.2, alpha=0.7)
    # Cheek tufts and fangs.
    fur_box(paint, BOXES["cheek"], fur, 0.62, ragged=True, density=1.8)
    # Fangs: points, the upper ones down and the lower ones (their own patch of the texture) up.
    for name, rect in faces(*BOXES["fang"]).items():
        if name not in ("top", "bottom"):
            paint.point(R(rect), TOOTH, root=0.3, tip=1.0, down=True)
    for name, rect in faces(*BOXES["lower_fang"]).items():
        if name not in ("top", "bottom"):
            paint.point(R(rect), TOOTH, root=0.3, tip=1.0, down=False)

    # The muzzle: a paler bridge, a big wet nose with nostrils, a dark lip over teeth; the roof of the mouth red.
    fur_box(paint, BOXES["muzzle"], fur, 0.5, top_flow=(0.0, -1.0))
    x0, y0, w, h = f("muzzle", "top")
    blaze(paint, (x0, y0, w, h), belly if kind != "shadow" else fur, 0.55, 0.75, level=0.55)
    x0, y0, w, h = f("muzzle", "front")
    paint.shade((x0, y0, w, h), fur, 0.4, spread=0.2)
    paint.blob(x0 + w / 2.0, y0 + 3.6, w * 0.33, 3.6, pal["nose"][0], pal["nose"][2])
    for side in (-1, 1):
        paint.ellipse(x0 + w / 2.0 + side * 3.0, y0 + 4.6, 1.3, 0.9, pal["nose"][0])
    paint.glint(int(x0 + w / 2.0 - 2), int(y0 + 2), pal["nose"][2])
    paint.line(x0, y0 + h - 4.5, x0 + w, y0 + h - 4.5, MOUTH[0], width=2.0)
    teeth_row(paint, x0 + 1, x0 + w - 1, y0 + h - 3.5, 2.6)
    for side in ("left", "right"):
        x0, y0, w, h = f("muzzle", side)
        paint.line(x0, y0 + h - 4.5, x0 + w, y0 + h - 4.5, MOUTH[0], width=2.0)
        teeth_row(paint, x0 + 1, x0 + w - 1, y0 + h - 3.5, 2.6, gap=2)
    x0, y0, w, h = f("muzzle", "bottom")
    for y in range(h):
        for x in range(w):
            ridge = (y % 6) < 2 and 3 < x < w - 4
            paint.put(x0 + x, y0 + y, ramp(MOUTH, 0.35 + (0.25 if ridge else 0.0) - 0.2 * abs(x - w / 2.0) / (w / 2.0)))
    for edge_x in (x0 + 1, x0 + w - 4):
        for ty in range(y0 + 2, y0 + h - 2, 4):
            paint.tooth(edge_x + 1.5, ty, 3, 3, TOOTH, down=True)
    teeth_row(paint, x0 + 2, x0 + w - 2, y0 + h - 4, 2.6)

    # The jaw: a tongue between rows of teeth, a furred chin.
    fur_box(paint, BOXES["jaw"], fur, 0.42)
    x0, y0, w, h = f("jaw", "top")
    for y in range(h):
        for x in range(w):
            groove = abs(x + 0.5 - w / 2.0) < 1.0
            paint.put(x0 + x, y0 + y, ramp(MOUTH, (0.55 if not groove else 0.35) - 0.25 * abs(x - w / 2.0) / (w / 2.0) + 0.1 * y / h))
    for edge_x in (x0 + 1, x0 + w - 4):
        for ty in range(y0 + 4, y0 + h, 4):
            paint.tooth(edge_x + 1.5, ty, 3, 3, TOOTH, down=False)
    x0, y0, w, h = f("jaw", "front")
    teeth_row(paint, x0 + 1, x0 + w - 1, y0 + 4, 2.6, down=False)
    for side in ("left", "right"):
        x0, y0, w, h = f("jaw", side)
        teeth_row(paint, x0 + 1, x0 + w - 1, y0 + 4, 2.6, down=False, gap=2)

    # Ears: inner ear on the front, shading to the fur at its edges, tufts at the rim; dark tips.
    fur_box(paint, BOXES["ear"], fur, 0.5)
    x0, y0, w, h = f("ear", "front")
    for y in range(2, h):
        for x in range(1, w - 1):
            paint.put(x0 + x, y0 + y, ramp(pal["ear"], 0.25 + 0.6 * (y / h) - 0.3 * abs(x + 0.5 - w / 2.0) / (w / 2.0)))
    paint.strands((x0, y0, w, h), fur, 0.65, flow=(0.0, -1.0), density=0.5, length=(3, 6))
    fur_box(paint, BOXES["ear_tip"], fur, 0.15)

    # Arms: shaggy shoulders, arms darker to the wrist, hands with dark palms, claws of ivory.
    fur_box(paint, BOXES["shoulder"], fur, 0.55, ragged=True, density=1.8)
    fur_box(paint, BOXES["upper_arm"], fur, 0.5)
    fur_box(paint, BOXES["forearm"], fur, 0.45)
    for name in SIDES:
        x0, y0, w, h = f("forearm", name)
        for y in range(h - 10, h):
            for x in range(w):
                paint.put(x0 + x, y0 + y, ramp(fur, 0.1), (y - (h - 10)) / 14.0)
        paint.tufts((x0, y0, w, h), fur, 0.2, depth=5)
    fur_box(paint, BOXES["hand"], fur, 0.35)
    x0, y0, w, h = f("hand", "bottom")
    paint.shade((x0, y0, w, h), [PAD, mix(PAD, (90, 80, 76), 0.35)], 0.4, spread=0.3, light="centre")
    for k in range(4):
        paint.blob(x0 + 2.5 + k * (w - 5) / 3.0, y0 + h - 4, 2.0, 2.0, PAD, (70, 62, 58))
    paint.blob(x0 + w / 2.0, y0 + h / 2.0 - 2, w * 0.3, h * 0.25, PAD, (80, 70, 66))
    for name, rect in faces(*BOXES["claw"]).items():
        if name not in ("top", "bottom"):
            paint.point(R(rect), CLAW, root=0.1, tip=0.95, down=True)
        else:
            paint.shade(R(rect), CLAW, 0.2, spread=0.1, light="flat", edge=0.0)

    # Legs: the thigh's front paler; shins and hocks with a dark heel; paws with toes, pads and ivory claws.
    fur_box(paint, BOXES["thigh"], fur, 0.5, ragged=True)
    blaze(paint, f("thigh", "front"), fur, 0.6, 0.4, level=0.7)
    fur_box(paint, BOXES["shin"], fur, 0.45)
    fur_box(paint, BOXES["hock"], fur, 0.35)
    x0, y0, w, h = f("hock", "back")
    paint.shade((x0, y0, w, h), fur, 0.1, spread=0.1)
    fur_box(paint, BOXES["paw"], fur, 0.35)
    x0, y0, w, h = f("paw", "bottom")
    paint.shade((x0, y0, w, h), [PAD, mix(PAD, (90, 80, 76), 0.35)], 0.4, spread=0.3, light="centre")
    for k in range(4):
        paint.blob(x0 + 3 + k * (w - 6) / 3.0, y0 + h - 4, 2.2, 2.2, PAD, (70, 62, 58))
    paint.blob(x0 + w / 2.0, y0 + h / 2.0 - 2, w * 0.28, h * 0.22, PAD, (80, 70, 66))
    x0, y0, w, h = f("paw", "top")
    for k in (1, 2, 3):
        paint.line(x0 + k * w / 4.0, y0 + h * 0.4, x0 + k * w / 4.0, y0 + h, ramp(fur, 0.05), width=1.4)
    x0, y0, w, h = f("paw", "front")
    for k in range(4):
        cx = x0 + 2.5 + k * (w - 5) / 3.0
        paint.tooth(cx, y0 + h - 3, 3, 3, CLAW, down=True)

    # The tail: shaggy, a paler tip (darker on a shadow werewolf).
    fur_box(paint, BOXES["tail"], fur, 0.45)
    fur_box(paint, BOXES["tail_mid"], fur, 0.5, ragged=True, density=1.8)
    fur_box(paint, BOXES["tail_tip"], belly if kind == "snow" else fur, 0.75 if kind != "shadow" else 0.2, ragged=True, density=1.8)
    return glow.img if eyes_only else paint.img


def wolfsbane():
    """A tall stem, palmate leaves low down, and a spike of hooded violet-blue flowers above."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(4, 16):
        img.putpixel((8, y), STEM[1] + (255,))
    for side in (-1, 1):
        for finger in range(3):
            for k in range(1, 5 - finger):
                x = 8 + side * k
                y = 13 - finger - (k // 2) * (1 if finger else 0)
                if 0 <= x < 16 and 0 <= y < 16:
                    img.putpixel((x, y), STEM[(finger + k) % 3] + (255,))
    flowers = [(6, 10, 2), (10, 8, 2), (6, 6, 2), (10, 4, 1), (7, 2, 1), (9, 1, 1)]
    for fx, fy, size in flowers:
        for dy in range(-size, size + 1):
            for dx in range(-size, size + 1):
                if abs(dx) + abs(dy) <= size + 1:
                    x, y = fx + dx, fy + dy
                    if 0 <= x < 16 and 0 <= y < 16:
                        tone = 3 if dy < 0 else 2 if dy == 0 else 1
                        img.putpixel((x, y), HOOD[tone - (1 if abs(dx) == size else 0)] + (255,))
        if 0 <= fy + 1 < 16:
            img.putpixel((fx, fy + 1), HOOD[0] + (255,))
    img.putpixel((8, 0), HOOD[2] + (255,))
    return img


HD = 4


def rug(kind):
    """The pelt seen from above (64 by 64): the kind's fur flowing along it, a darker stripe down the spine."""
    pal = KINDS[kind]
    paint = Painter(16 * HD, 16 * HD, 23003 + len(kind), clean=True)
    paint.shade((0, 0, 16 * HD, 16 * HD), pal["fur"], 0.55, spread=0.2, light="centre", edge=0.1)
    paint.locks((0, 0, 16 * HD, 16 * HD), pal["fur"], 0.5, flow=(0.0, 1.0), length=(7, 11), width=(3.0, 5.0), light=0.1)
    paint.locks((26, 0, 12, 16 * HD), pal["fur"], 0.25, flow=(0.0, 1.0), length=(7, 11), width=(3.0, 5.0), light=0.0)
    return paint.img


def rug_head(kind):
    """The rug's snarling head (64 by 64): fur, glaring eyes, a black nose and a row of teeth at the front."""
    pal = KINDS[kind]
    paint = Painter(16 * HD, 16 * HD, 23004 + len(kind), clean=True)
    paint.shade((0, 0, 16 * HD, 16 * HD), pal["fur"], 0.5, spread=0.3, light="centre", edge=0.1)
    paint.locks((0, 14, 16 * HD, 36), pal["fur"], 0.5, flow=(0.0, 1.0), length=(6, 9), width=(3.0, 4.6), light=0.1)
    # The eyes where the head's top face reads (near its front edge), the nose and teeth where its front face does.
    for cx in (26, 38):
        paint.ellipse(cx, 8, 4.5, 3.0, pal["socket"])
        paint.blob(cx, 8, 3.0, 2.0, pal["eye"][1], pal["eye"][2])
        paint.glint(cx - 1, 7, pal["eye"][3])
    paint.blob(32, 57, 5, 3.0, pal["nose"][0], pal["nose"][2])
    paint.line(20, 60.5, 44, 60.5, MOUTH[0], width=1.5)
    teeth_row(paint, 20, 44, 61, 2.6)
    return paint.img


def pelt(kind):
    """A folded pelt as an item (64 by 64): the kind's fur, a paler inner edge showing, ragged ends."""
    pal = KINDS[kind]
    paint = Painter(16 * HD, 16 * HD, 23005 + len(kind), clean=True)
    for y in range(12, 54):
        inset = 6 + int(3 * abs(((y - 12) % 14) - 7) / 7.0)
        for x in range(inset, 64 - inset):
            f = 0.5 + 0.25 * (0.5 - (y - 12) / 42.0)
            paint.put(x, y, ramp(pal["fur"], round(f / 0.16) * 0.16))
    paint.locks((6, 12, 52, 40), pal["fur"], 0.5, flow=(0.0, 1.0), length=(6, 9), width=(3.0, 4.6), light=0.1)
    for x in range(8, 56):
        paint.put(x, 52, ramp(pal["belly"], 0.6))
        paint.put(x, 53, ramp(pal["belly"], 0.4))
    paint.tufts((6, 12, 52, 44), pal["fur"], 0.35, depth=4, every=4)
    # A dark outline, as items have.
    img = paint.img
    px = img.load()
    edge = [(x, y) for y in range(64) for x in range(64) if px[x, y][3] == 0
            and any(0 <= x + dx < 64 and 0 <= y + dy < 64 and px[x + dx, y + dy][3] > 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    for x, y in edge:
        px[x, y] = ramp(pal["fur"], 0.0) + (255,)
    return img


def dagger():
    """A silver dagger held point up to the right: a leaf blade, a cross-guard, a leather grip and a round pommel."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(7, 15):
        x, y = i, 15 - i
        img.putpixel((x, y), SILVER[2] + (255,))
        img.putpixel((x - 1, y), SILVER[1] + (255,))
        if i < 13:
            img.putpixel((x, y + 1), SILVER[0] + (255,))
    img.putpixel((15, 0), SILVER[3] + (255,))
    img.putpixel((14, 1), SILVER[3] + (255,))
    for x, y in ((4, 9), (5, 8), (6, 9), (7, 10), (8, 11), (5, 10), (6, 11)):
        img.putpixel((x, y), SILVER[1] + (255,))
    for x, y in ((4, 11), (3, 12), (5, 12), (4, 13)):
        img.putpixel((x, y), LEATHER[(x + y) % 2] + (255,))
    for x, y in ((2, 13), (1, 14), (2, 14), (1, 13)):
        img.putpixel((x, y), SILVER[2] + (255,))
    return img


def arrow():
    """An arrow, fletching at the lower left and a bright silver head at the upper right."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(3, 13):
        img.putpixel((i, 15 - i), SHAFT[i % 2] + (255,))
    for x, y in ((12, 1), (13, 2), (14, 1), (13, 0), (12, 2), (14, 2), (13, 1), (11, 2)):
        img.putpixel((x, y), SILVER[2 + (x + y) % 2] + (255,))
    for x, y in ((2, 11), (3, 11), (1, 12), (2, 13), (4, 13), (4, 14), (3, 14), (1, 13)):
        img.putpixel((x, y), FLETCH[(x + y) % 2] + (255,))
    return img


def werewolf_textures():
    out = {("block", WOLFSBANE["block"]): wolfsbane(), ("item", WEREWOLF["dagger"]): dagger(), ("item", WEREWOLF["arrow"]): arrow()}
    for kind, spec in WEREWOLF["kinds"].items():
        out[("entity", f"werewolf_{kind}")] = werewolf(kind)
        out[("entity", f"werewolf_{kind}_eyes")] = werewolf(kind, eyes_only=True)
        out[("block", spec["rug"])] = rug(kind)
        out[("block", f"{spec['rug']}_head")] = rug_head(kind)
        out[("item", spec["pelt"])] = pelt(kind)
    return out
