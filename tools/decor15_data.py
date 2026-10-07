"""Generated data for Halloween decorations batch 15, the churchyard's ornaments (tools/decor15.py): sculpted models on
the toolkit of tools/flora_art.py (each block one 64 x 64 texture painted here), blockstates, items, names, loot and tags.
The Gargoyle is a graveyard monument and is written by tools/graveyard_data.py.

Everything is drawn here by code from fixed seeds; no Mojang texture is read, traced or copied.
"""
import math
import random

import cute_art as ca
import decor15 as d15
import flora_art as fa
from flora_art import SIDES4, Px, Sculpt, column, cube, pal, plane_xy, plane_zy, rgb, rotation, shade, solid, strip
from flora_models import split_tall

MOD = "jugcraft"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

BONE = pal("5e5442", "857a62", "a99d80", "c8bd9e", "e0d7bb", "f3eedb")
SOCKET = pal("1e1712", "33281f")
DUST = pal("4a4236", "6e6452", "938670", "b2a68a")
EARTH = pal("2e2218", "43321f", "5a442b", "6f5638")
IRON = pal("16161a", "26262c", "3a3a42", "54545e", "70707c")
VIOLET = pal("3a1458", "5a2486", "7e3cb4", "a066d8", "c9a0f0", "efe0ff")


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


# ---------------------------------------------------------------- painters

def skull_face(seed=1):
    """A skull from the front: the dome, two hollow eye sockets, a nose hole and the upper teeth."""
    def paint(p):
        solid(BONE[1:], seed)(p)
        rng = random.Random(seed)
        w, h = p.w, p.h
        for x in range(w):
            p.put(x, 0, BONE[4])
        for ex in (w * 0.18, w * 0.58):
            for dx in range(int(w * 0.24) + 1):
                for dy in range(int(h * 0.3) + 1):
                    if not (dx in (0, int(w * 0.24)) and dy in (0, int(h * 0.3))):
                        p.put(ex + dx, h * 0.3 + dy, SOCKET[0] if dy else SOCKET[1])
        p.put(w / 2 - 0.5, h * 0.66, SOCKET[0])
        p.put(w / 2 + 0.5, h * 0.66, SOCKET[0])
        for x in range(1, w - 1):
            p.put(x, h - 1, BONE[4] if x % 2 else SOCKET[1])
        for _ in range(3):
            p.put(rng.randrange(w), rng.randrange(h // 4), BONE[2])
    return paint


def jaw_front():
    def paint(p):
        solid(BONE[1:], 7)(p)
        for x in range(p.w):
            p.put(x, 0, BONE[5] if x % 2 else SOCKET[1])
    return paint


def bone_ends(seed=1):
    """The packed ends of long bones in a catacomb wall, in staggered courses: each a pale knob with a darker marrow
    pit, deep shadow between."""
    def paint(p):
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, SOCKET[0] if (x + y) % 3 else SOCKET[1])
        rng = random.Random(seed)
        step = 4
        for row, cy in enumerate(range(2, p.h + step, step)):
            for cx in range(2 + (row % 2) * 2, p.w + step, step):
                r = 1.6 + rng.random() * 0.4
                ox, oy = cx + rng.choice((-0.4, 0, 0.4)), cy + rng.choice((-0.4, 0, 0.4))
                for y in range(int(oy - r) - 1, int(oy + r) + 2):
                    for x in range(int(ox - r) - 1, int(ox + r) + 2):
                        d = math.hypot(x - ox, y - oy)
                        if d <= r:
                            k = 2 if d < 0.7 else (4 if x <= ox and y <= oy else 3)
                            p.put(x % p.w, y % p.h, shade(BONE, k))
    return paint


def bone_dust(seed=1):
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                p.put(x, y, rng.choice(DUST + [BONE[2], BONE[3]]))
    return paint


def glass(seed=1):
    """Violet lantern glass in leaded diamonds, brightest at the middle where the flame stands behind it."""
    def paint(p):
        rng = random.Random(seed)
        for y in range(p.h):
            for x in range(p.w):
                came = (x + y) % 5 == 0 or (x - y) % 5 == 0
                glow = 1 - abs(x - (p.w - 1) / 2) / p.w - abs(y - p.h * 0.6) / p.h
                k = 2 + int(glow * 4) + (0 if fa.QUIET else rng.choice((0, 0, -1)))
                p.put(x, y, IRON[1] if came else shade(VIOLET, k))
    return paint


def chain():
    def paint(p):
        for y in range(p.h):
            if y % 3 != 2:
                p.put(p.w // 2, y, IRON[3 if y % 3 == 0 else 2])
                p.put(p.w // 2 - (1 if y % 6 < 3 else 0), y, IRON[1])
    return paint


def ring():
    def paint(p):
        cx, cy = (p.w - 1) / 2, (p.h - 1) / 2
        for y in range(p.h):
            for x in range(p.w):
                d = math.hypot((x - cx) / (p.w / 2), (y - cy) / (p.h / 2))
                if 0.6 <= d <= 1.0:
                    p.put(x, y, IRON[3] if y < cy else IRON[2])
    return paint


def cute_jaw():
    """A skull's jaw: soft cream with a neat row of little teeth along its top."""
    def paint(p):
        ca.soft(ca.BONE, 2)(p)
        ca.teeth(p, 0, p.w, 0, 1, ca.BONE[4], ca.SOCKET[1], tooth=1)
    return paint


# ---------------------------------------------------------------- shared parts

def bone(sc, centre, length, yaw=0, y=0.0, width=0.9, along_z=False):
    """A long bone lying on its side: a shaft with a knobbed end each side, turned `yaw` about its middle."""
    side = sc.piece("shaft", 12, 2, ca.bevel(ca.BONE, 3, sides="tb"))
    knob = sc.piece("knob", 3, 3, ca.soft(ca.BONE, 3))
    cx, cz = centre
    rot = rotation((cx, y, cz), "y", yaw) if yaw else None
    h = width / 2
    out = []
    if along_z:
        out.append(cube((cx - h, y, cz - length / 2), (cx + h, y + width, cz + length / 2), {s: side for s in ("east", "west", "up", "down")}, rot))
        for end in (-1, 1):
            ez = cz + end * length / 2
            out.append(cube((cx - 0.9, y - 0.05, ez - 0.7), (cx + 0.9, y + width + 0.25, ez + 0.7), {s: knob for s in SIDES4 + ("up",)}, rot))
        return out
    out.append(cube((cx - length / 2, y, cz - h), (cx + length / 2, y + width, cz + h), {s: side for s in ("north", "south", "up", "down")}, rot))
    for end in (-1, 1):
        ex = cx + end * length / 2
        out.append(cube((ex - 0.7, y - 0.05, cz - 0.9), (ex + 0.7, y + width + 0.25, cz + 0.9), {s: knob for s in SIDES4 + ("up",)}, rot))
    return out


def skull(sc, centre, y=0.0, yaw=0, size=1.0, facing="north"):
    """A skull: the cranium with its face toward `facing` (north or south) and the lower jaw under its front."""
    face = sc.piece("skull_face", 10, 9, ca.skull_face(sockets=0.3, socket_y=0.45, tall=0.2, mouth=False))
    side = sc.piece("skull_side", 9, 8, ca.soft(ca.BONE, 2))
    top = sc.piece("skull_top", 9, 9, ca.soft(ca.BONE, 3))
    jaw = sc.piece("jaw", 7, 2, cute_jaw())
    cx, cz = centre
    s = size
    rot = rotation((cx, y, cz), "y", yaw) if yaw else None
    back = "south" if facing == "north" else "north"
    sign = -1 if facing == "north" else 1
    front = cz + sign * 2.1 * s
    out = [cube((cx - 2.0 * s, y + 0.9 * s, cz - 2.1 * s), (cx + 2.0 * s, y + 4.3 * s, cz + 2.1 * s),
                {facing: face, back: side, "east": side, "west": side, "up": top, "down": side}, rot)]
    j0, j1 = sorted((front, front - sign * 1.8 * s))
    out.append(cube((cx - 1.5 * s, y, j0), (cx + 1.5 * s, y + 0.9 * s, j1), {facing: jaw, "east": side, "west": side, "down": side}, rot))
    return out


# ---------------------------------------------------------------- the ornaments

def bone_pile():
    """Four heaps, each the one before and a layer more: bones crossing every way and skulls staring out."""
    sc = Sculpt(d15.BONE_PILE["block"], 61)
    fill = sc.piece("dust", 12, 12, ca.scatter(DUST, 2, 4, 2))
    rng = random.Random(9)
    yaws = (0, 22.5, 45, -22.5, -45)
    previous = []
    for layer in range(1, d15.BONE_PILE["layers"] + 1):
        base = (layer - 1) * d15.BONE_PILE["layer_pixels"]
        els = list(previous)
        if layer > 1:
            inset = 0.5 + (layer - 2) * 1.2
            els.append(cube((inset, base - 3.0, inset), (16 - inset, base - 0.3, 16 - inset), {**{s: fill for s in SIDES4}, "up": fill}))
        for i in range(5 if layer < 4 else 3):
            x = 3.5 + rng.random() * 9
            z = 3.5 + rng.random() * 9
            els += bone(sc, (x, z), 5.0 + rng.random() * 3.5, rng.choice(yaws), base + rng.random() * 1.2, along_z=rng.random() < 0.3)
        for i in range((1, 1, 2, 2)[layer - 1]):
            x = 4.5 + rng.random() * 7
            z = 4.5 + rng.random() * 7
            els += skull(sc, (x, z), base + 0.4, rng.choice((0, 22.5, -22.5, 45)), 0.9 + rng.random() * 0.2,
                         "north" if rng.random() < 0.6 else "south")
        sc.models[f"bone_pile_{layer}"] = els
        previous = els
    return sc


def ossuary_wall():
    """A catacomb wall: rows of skulls between courses of long bones laid crosswise, all set into a core of bone ends."""
    sc = Sculpt(d15.OSSUARY_WALL["block"], 62)
    ends = sc.piece("ends", 32, 32, ca.bone_ends(4))
    back = sc.piece("back", 16, 16, ca.bone_ends(5))
    shaft = sc.piece("shaft", 12, 2, ca.bevel(ca.BONE, 3, sides="tb"))
    knob = sc.piece("knob", 3, 3, ca.soft(ca.BONE, 3))
    els = [fa._el((0, 0, 2.0), (16, 16, 16), {"north": {"uv": list(back), "texture": "#p"},
                                              **{s: {"uv": list(ends), "texture": "#p", "cullface": s} for s in ("south", "east", "west", "up", "down")}})]
    for y0 in (0.0, 5.6, 11.2):
        els.append(cube((0, y0, 0.6), (16, y0 + 1.4, 2.0), {"north": shaft, "up": shaft, "down": shaft}))
        for x in (0.0, 15.2):
            els.append(cube((x, y0 - 0.1, 0.4), (x + 0.8, y0 + 1.6, 2.0), {"north": knob, "east": knob, "west": knob}))
    for row, (y0, shift) in enumerate(((1.4, 0.0), (7.0, 2.6), (12.6, 0.0))):
        xs = (2.6, 8.0, 13.4) if not shift else (5.3, 10.7)
        for x in xs:
            size = 0.86 if row < 2 else 0.78
            els += skull(sc, (x, 2.1), y0, 0, size, "north")
    sc.models["ossuary_wall"] = els
    return sc


def _hand_elements(clenched):
    """The giant's hand out of a heap of grave earth, in a frame two blocks high: radius and ulna, a knot of wrist
    bones, the palm's long bones, four fingers of three joints each and a thumb, spread and clawing, or curled into a
    fist."""
    sc = HAND
    earth = sc.piece("earth", 12, 4, ca.bevel(EARTH, 2))
    earth_top = sc.piece("earth_top", 12, 12, ca.scatter(EARTH, 2, 3, 4))
    long_bone = sc.piece("long", 3, 16, ca.bevel(ca.BONE, 3, sides="lr"))
    knuckle = sc.piece("knuckle", 3, 3, ca.soft(ca.BONE, 3))
    els = [cube((2.5, 0, 2.5), (13.5, 1.4, 13.5), {**{s: earth for s in SIDES4}, "up": earth_top}),
           cube((4.0, 1.4, 4.0), (12.0, 2.6, 12.0), {**{s: earth for s in SIDES4}, "up": earth_top})]
    # Radius and ulna rising from the earth, a little apart, to the wrist.
    for x in (6.6, 9.4):
        els.append(column(x, 8.4, 1.0, 12.6, 1.6, long_bone))
        els.append(cube((x - 1.0, 11.8, 7.4), (x + 1.0, 13.0, 9.4), {**{s: knuckle for s in SIDES4}, "up": knuckle}))
    # The wrist: a knot of small bones.
    els.append(cube((5.2, 12.8, 7.0), (10.8, 14.6, 9.8), {**{s: knuckle for s in SIDES4}, "up": knuckle, "down": knuckle}))
    # The palm's long bones, fanning out a little, and the fingers.
    for i, x in enumerate((5.6, 7.3, 9.0, 10.7)):
        lengths = (3.4, 2.5, 1.9) if i in (1, 2) else (3.0, 2.2, 1.7)
        lean = (-0.4, -0.12, 0.12, 0.4)[i]
        base = (x, 14.4, 8.4)
        e, top = fa.segment(base, 4.6 - abs(lean) * 2, "z", -22.5 if lean > 0.3 else (22.5 if lean < -0.3 else 0), width=1.1, uv=long_bone)
        els.append(e)
        angles = (-22.5, -45, -45) if clenched else (0, -22.5, -22.5)
        lengths = (2.6, 2.0, 1.5) if clenched else lengths
        for j, (length, angle) in enumerate(zip(lengths, angles)):
            els.append(cube((top[0] - 0.7, top[1] - 0.4, top[2] - 0.7), (top[0] + 0.7, top[1] + 0.4, top[2] + 0.7),
                            {**{s: knuckle for s in SIDES4}, "up": knuckle}))
            e, top = fa.segment(top, length, "x", angle, width=1.0 - j * 0.1, uv=long_bone)
            els.append(e)
        # A claw-like fingertip.
        els.append(cube((top[0] - 0.4, top[1] - 0.2, top[2] - 0.4), (top[0] + 0.4, top[1] + 0.5, top[2] + 0.4),
                        {**{s: knuckle for s in SIDES4}, "up": knuckle}))
    # The thumb, out from the wrist's side.
    e, top = fa.segment((4.8, 13.6, 7.8), 3.0, "z", 45, width=1.1, uv=long_bone)
    els.append(e)
    e, top = fa.segment(top, 2.4 if not clenched else 1.8, "z", 22.5 if not clenched else -22.5, width=1.0, uv=long_bone)
    els.append(e)
    return els


HAND = None


def giant_bone_hand():
    global HAND
    sc = Sculpt(d15.BONE_HAND["block"], 63)
    HAND = sc
    for clenched in (False, True):
        lower, upper = split_tall(_hand_elements(clenched))
        state = "clenched" if clenched else "open"
        sc.models[f"giant_bone_hand_{state}_bottom"] = lower
        sc.models[f"giant_bone_hand_{state}_top"] = upper
    sc.models["giant_bone_hand_item"] = _hand_elements(False)
    return sc


@fa.quietly
def witchs_lantern():
    """A gothic lantern: an iron base, four corner posts round violet leaded glass, a stepped roof with a spire and a
    ring; hanging, it hangs lower on a chain."""
    sc = Sculpt(d15.WITCHS_LANTERN["block"], 64)
    iron = sc.piece("iron", 8, 8, solid(IRON[1:], 5, rim=True))
    post = sc.piece("post", 2, 10, strip(IRON, 6))
    pane = sc.piece("glass", 10, 14, glass(3))
    flame = sc.piece("flame", 3, 4, solid(VIOLET[3:], 2))
    ring_uv = sc.piece("ring", 8, 8, ring())
    chain_uv = sc.piece("chain", 3, 16, chain())

    def lantern(dy):
        light = d15.WITCHS_LANTERN["light"]
        els = [cube((4.5, dy, 4.5), (11.5, dy + 1, 11.5), {**{s: iron for s in SIDES4}, "up": iron, "down": iron}),
               cube((5.6, dy + 1, 5.6), (10.4, dy + 8, 10.4), {s: pane for s in SIDES4}, light=light),
               cube((7.2, dy + 1.5, 7.2), (8.8, dy + 4.0, 8.8), {**{s: flame for s in SIDES4}, "up": flame}, light=15)]
        for x, z in ((5, 5), (11, 5), (5, 11), (11, 11)):
            els.append(column(x, z, dy + 1, dy + 8.5, 1.0, post))
        els += [cube((4.5, dy + 8.5, 4.5), (11.5, dy + 9.5, 11.5), {**{s: iron for s in SIDES4}, "up": iron, "down": iron}),
                cube((5.5, dy + 9.5, 5.5), (10.5, dy + 10.5, 10.5), {**{s: iron for s in SIDES4}, "up": iron}),
                cube((6.5, dy + 10.5, 6.5), (9.5, dy + 11.5, 9.5), {**{s: iron for s in SIDES4}, "up": iron}),
                column(8, 8, dy + 11.5, dy + 13.0, 1.0, post)]
        return els

    standing = lantern(0)
    standing += [plane_xy(7.0, 9.0, 13.0, 15.0, 8.0, ring_uv), plane_zy(7.0, 9.0, 13.0, 15.0, 8.0, ring_uv)]
    hanging = lantern(1.0)
    hanging += [plane_xy(7.25, 8.75, 14.0, 16.0, 8.0, chain_uv), plane_zy(7.25, 8.75, 14.0, 16.0, 8.0, chain_uv)]
    sc.models["witchs_lantern"] = standing
    sc.models["witchs_lantern_hanging"] = hanging
    return sc


_built = {}


def build(name):
    if name not in _built:
        _built[name] = {"bone_pile": bone_pile, "ossuary_wall": ossuary_wall, "giant_bone_hand": giant_bone_hand,
                        "witchs_lantern": witchs_lantern}[name]()
    return _built[name]


BLOCK_DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
                 "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                 "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
                 "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                 "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}


# ---------------------------------------------------------------- files

def assets(root, write, lang):
    models = root / "models" / "block"
    for name in ("bone_pile", "ossuary_wall", "giant_bone_hand", "witchs_lantern"):
        sc = build(name)
        for model, elements in sc.models.items():
            if not model.endswith("_item"):
                write(models / f"{model}.json", fa.model(sc.name, elements))
    lang.update({f"block.{MOD}.{block}": display for block, display in d15.names().items()})
    states = root / "blockstates"
    write(states / "bone_pile.json", {"variants": {f"layers={n}": {"model": rid(f"block/bone_pile_{n}")} for n in range(1, 5)}})
    write(states / "ossuary_wall.json", {"variants": {f"facing={f}": {"model": rid("block/ossuary_wall"), **({"y": y} if y else {})}
                                                      for f, y in FACINGS.items()}})
    variants = {}
    for f, y in FACINGS.items():
        for half, part in (("lower", "bottom"), ("upper", "top")):
            for powered, state in (("false", "open"), ("true", "clenched")):
                variant = {"model": rid(f"block/giant_bone_hand_{state}_{part}")}
                if y:
                    variant["y"] = y
                variants[f"facing={f},half={half},powered={powered}"] = variant
    write(states / "giant_bone_hand.json", {"variants": variants})
    write(states / "witchs_lantern.json", {"variants": {"hanging=false": {"model": rid("block/witchs_lantern")},
                                                        "hanging=true": {"model": rid("block/witchs_lantern_hanging")}}})
    items = root / "models" / "item"
    write(items / "bone_pile.json", {"parent": rid("block/bone_pile_2"), "display": fa.PLANT_DISPLAY})
    write(items / "ossuary_wall.json", {"parent": rid("block/ossuary_wall")})
    hand = build("giant_bone_hand")
    write(items / "giant_bone_hand.json", fa.model(hand.name, hand.models["giant_bone_hand_item"], fa.TALL_DISPLAY))
    write(items / "witchs_lantern.json", {"parent": rid("block/witchs_lantern"), "display": fa.PLANT_DISPLAY})
    for block in d15.items():
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{block}")}})


def loot(out, write):
    from decor_data import self_drop
    # One pile a layer, like snow.
    write(out / "bone_pile.json", {"type": "minecraft:block", "pools": [{"entries": [{"type": "minecraft:item", "modifier": [
        *[{"type": "minecraft:set_count", "condition": {"type": "minecraft:match_block", "blocks": rid("bone_pile"), "state": {"layers": str(n)}},
           "count": n} for n in range(1, 5)], {"type": "minecraft:explosion_decay"}], "name": rid("bone_pile")}], "rolls": 1}],
        "random_sequence": rid("blocks/bone_pile")})
    write(out / "ossuary_wall.json", self_drop("ossuary_wall"))
    write(out / "witchs_lantern.json", self_drop("witchs_lantern"))
    write(out / "giant_bone_hand.json", self_drop("giant_bone_hand", {"type": "minecraft:match_block", "blocks": rid("giant_bone_hand"),
                                                                       "state": {"half": "lower"}}))


def tags(tags):
    for block in ("ossuary_wall", "giant_bone_hand", "witchs_lantern"):
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    tags.add("block", "minecraft:mineable/shovel", rid("bone_pile"))


def textures():
    """(kind, name) -> image for each ornament's texture."""
    return {("block", name): build(name).atlas.img for name in ("bone_pile", "ossuary_wall", "giant_bone_hand", "witchs_lantern")}
