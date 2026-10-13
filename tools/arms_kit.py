"""The Arms VI kit's models and item definitions (batch 55, docs/features/arms-vi.md): the longbows and arbalests of
tools/arms.py RANGED, held as vanilla's bow and crossbow are, and the shields of SHIELDS, built in 3D. Sprites:
tools/arms_kit_art.py.

Shields: vanilla draws its shield with a special renderer, from an entity model: a 12 x 22 x 1 plate with a handle behind it,
flipped (scale 1, -1, -1, the transformation in its items/shield.json) about the corner a JSON model's pixels count
from. In a JSON model's pixels that plate spans x -6..6, y -11..11, z 1..2, its face to the south (+z), and the handle
sits behind it (z -5..1); vanilla's shield poses centre it there (in a frame, the GUI, on the ground and on a shelf).
These shields are built in the same place, so vanilla's shield display transforms (DISPLAY, BLOCKING, read from
26.3's models/item/shield.json and shield_blocking.json) hold them as vanilla holds its shield.

Each shield is a few boxes: the body, its rim, a boss, and the handle. The face is one painted texture mapped across the
body's boxes by position, so it reads as one painted board however the outline is stepped; the back is bare boards
with a leather strap across them (the handle is wrapped in it), and the trim is the metal of the rim and the boss.
"""
import arms

MOD = arms.MOD

# The plate's span in model pixels, and so the face texture's: x and y of its top-left and bottom-right.
PLATE = {"heater_shield": (2.0, 19.0, 14.0, -3.0), "tower_shield": (1.0, 21.0, 15.0, -5.0)}
FRONT = 10.0
BACK = 9.0
# The boxes below are laid out about the middle of a block (8, 8, 8); vanilla's shield is about the origin.
OFFSET = (-8.0, -8.0, -8.0)
HANDLE = [((7.0, 5.0, 3.0), (9.0, 11.0, 9.0))]
# Where each part lies on its texture (UV units, 0..16): the trim's rim band, boss and plain metal; the back's strap.
RIM_UV = [0, 0, 16, 2]
BOSS_UV = [0, 8, 8, 16]
PLAIN_UV = [8, 8, 16, 10]
STRAP_UV = [4, 7, 12, 9]


def body_boxes(kind):
    """The body as boxes (x0, y0, x1, y1): a heater is square-shouldered and comes to a point; a tower is a tall board."""
    if kind == "heater_shield":
        return [(2.0, 4.0, 14.0, 19.0), (3.0, 1.0, 13.0, 4.0), (4.5, -1.0, 11.5, 1.0), (6.0, -2.25, 10.0, -1.0), (7.25, -3.0, 8.75, -2.25)]
    return [(1.0, -5.0, 15.0, 21.0)]


def rim_boxes(kind):
    """A raised rim along the body's edges, on its face (x0, y0, x1, y1)."""
    if kind == "heater_shield":
        return [(2.0, 18.0, 14.0, 19.0), (2.0, 4.0, 2.75, 18.0), (13.25, 4.0, 14.0, 18.0), (3.0, 1.0, 3.75, 4.0), (12.25, 1.0, 13.0, 4.0),
                (4.5, -1.0, 5.25, 1.0), (10.75, -1.0, 11.5, 1.0), (6.0, -2.25, 6.75, -1.0), (9.25, -2.25, 10.0, -1.0),
                (7.25, -3.0, 8.75, -2.25)]
    return [(1.0, 20.0, 15.0, 21.0), (1.0, -5.0, 15.0, -4.0), (1.0, -4.0, 1.75, 20.0), (14.25, -4.0, 15.0, 20.0),
            (7.5, -4.0, 8.5, 20.0)]


def boss(kind):
    """The boss at the shield's heart (x0, y0, x1, y1, depth)."""
    return (6.5, 9.5, 9.5, 12.5, 1.0) if kind == "heater_shield" else (6.0, 7.0, 10.0, 11.0, 1.25)


def face_scale(kind):
    """Face texture pixels to a model pixel: the plate's height fills the 64-pixel face texture, and its width, at the
    same scale, lies along the texture's left side (square texels, however tall the shield)."""
    _px0, py0, _px1, py1 = PLATE[kind]
    return 64.0 / (py0 - py1)


def _uv(kind, x0, y0, x1, y1):
    """Where a box's front lies on the painted face texture, in UV units (16 to the texture's 64 pixels)."""
    px0, py0, _px1, _py1 = PLATE[kind]
    k = face_scale(kind) / 4.0
    return [round((x0 - px0) * k, 4), round((py0 - y1) * k, 4), round((x1 - px0) * k, 4), round((py0 - y0) * k, 4)]


def _cuboid(frm, to, faces):
    """A box, from the numbers here (laid out as in a block, 0..16) to where vanilla's shield is (OFFSET)."""
    return {"from": [round(v + o, 4) for v, o in zip(frm, OFFSET)], "to": [round(v + o, 4) for v, o in zip(to, OFFSET)],
            "faces": faces}


# An armor set's shield in a shape of its own (tools/arms_variants.py SET_SHIELDS): the Sentinel's, after the owner's
# design, a four-pointed star of gold, a raised diamond frame on it, a dark recess inside the frame and a chequered
# diamond at its heart with a pale stone at the very middle. Each part is a stack of rows (reach from the middle row,
# half width; the frame's also its opening's half width), laid out on half-pixel steps about the middle of the block so
# that every edge falls on the face texture's grid: its plate is 32 pixels tall, 2 texels to a model pixel.
PLATE["star_shield"] = (1.0, 24.0, 15.0, -8.0)
STAR = [(0.5, 7.0), (1.5, 6.0), (2.5, 5.0), (3.5, 4.0), (4.5, 3.0), (5.5, 2.5), (6.5, 2.0), (7.5, 1.0), (8.5, 0.5),
        (9.5, 0.5), (10.5, 0.5)]
STAR_FRAME = [(0.5, 4.5, 3.0), (1.5, 4.0, 2.5), (2.5, 3.0, 1.5), (3.5, 2.5, 1.0), (4.5, 2.0, 0.5), (5.5, 1.5, 0.0),
              (6.5, 0.5, 0.0)]
STAR_CENTRE = [(0.5, 2.0), (1.5, 1.5), (2.5, 1.0), (3.5, 0.5)]
STAR_STONE = [(0.5, 0.5)]
# Each raised part's depth on the star's face: (from, to) above FRONT. The stone sits on the chequered centre.
STAR_RISE = {"frame": (0.0, 0.75), "centre": (0.0, 0.4), "stone": (0.4, 0.7)}


def _ring(steps, cx=8.0, cy=8.0):
    """Boxes (x0, y0, x1, y1) of a shape symmetric about (cx, cy), a row for each step (reach, half width[, opening]):
    the first is the middle row, the others a row above and below it from the last reach to theirs, each open in the
    middle where a step has an opening (an opening's half width) and else whole."""
    out = []
    last = 0.0
    for reach, half, *opening in steps:
        inner = opening[0] if opening else 0.0
        rows = [(cy - reach, cy + reach)] if last == 0.0 else [(cy + last, cy + reach), (cy - reach, cy - last)]
        for y0, y1 in rows:
            if inner > 0.0:
                out += [(cx - half, y0, cx - inner, y1), (cx + inner, y0, cx + half, y1)]
            else:
                out.append((cx - half, y0, cx + half, y1))
        last = reach
    return out


def _step_at(steps, d):
    """The step a distance `d` from the middle row lies in, or None past the last."""
    for step in steps:
        if d < step[0]:
            return step
    return None


def star_part(x, y):
    """Which part of the star shield's face a point (model pixels) lies on: stone, centre, frame, recess (inside the
    frame), star, or None (off the shield)."""
    dx, dy = abs(x - 8.0), abs(y - 8.0)
    for part, steps in (("stone", STAR_STONE), ("centre", STAR_CENTRE)):
        step = _step_at(steps, dy)
        if step and dx < step[1]:
            return part
    step = _step_at(STAR_FRAME, dy)
    if step and dx < step[1]:
        return "frame" if dx >= step[2] else "recess"
    step = _step_at(STAR, dy)
    return "star" if step and dx < step[1] else None


def star_elements():
    """The star shield's elements: the stepped star, its front the painted face (#face), its edges plain metal (#trim)
    and its back the boards (#back); the raised frame, chequered centre and stone, their fronts the same painted face
    where they lie (so the painting runs on across them) and their sides plain metal; and the handle."""
    kind = "star_shield"
    out = []
    metal = {face: {"texture": "#trim", "uv": PLAIN_UV} for face in ("east", "west", "up", "down")}
    for x0, y0, x1, y1 in _ring(STAR):
        out.append(_cuboid((x0, y0, BACK), (x1, y1, FRONT), {
            "south": {"texture": "#face", "uv": _uv(kind, x0, y0, x1, y1)},
            "north": {"texture": "#back", "uv": _uv(kind, x0, y0, x1, y1)}, **metal}))
    for part, steps in (("frame", STAR_FRAME), ("centre", STAR_CENTRE), ("stone", STAR_STONE)):
        z0, z1 = (FRONT + rise for rise in STAR_RISE[part])
        for x0, y0, x1, y1 in _ring(steps):
            out.append(_cuboid((x0, y0, z0), (x1, y1, z1), {
                "south": {"texture": "#face", "uv": _uv(kind, x0, y0, x1, y1)}, **metal}))
    for frm, to in HANDLE:
        out.append(_cuboid(frm, to, {face: {"texture": "#back", "uv": STRAP_UV}
                                     for face in ("north", "south", "east", "west", "up", "down")}))
    return out


def elements(kind):
    """The shield's elements: textures #face (the painted front), #back (the bare boards, a strap across their middle)
    and #trim (the metal: the rim along its top half, the boss's face at the bottom left, plain metal at the bottom
    right). A set's star shield is built by star_elements()."""
    if kind == "star_shield":
        return star_elements()
    out = []
    for x0, y0, x1, y1 in body_boxes(kind):
        side = {"texture": "#back", "uv": [0, 0, 1, 16]}
        out.append(_cuboid((x0, y0, BACK), (x1, y1, FRONT), {
            "south": {"texture": "#face", "uv": _uv(kind, x0, y0, x1, y1)},
            "north": {"texture": "#back", "uv": _uv(kind, x0, y0, x1, y1)},
            "east": side, "west": side, "up": side, "down": side}))
    for x0, y0, x1, y1 in rim_boxes(kind):
        out.append(_cuboid((x0, y0, FRONT), (x1, y1, FRONT + 0.5), {
            face: {"texture": "#trim", "uv": RIM_UV} for face in ("south", "east", "west", "up", "down")}))
    bx0, by0, bx1, by1, depth = boss(kind)
    out.append(_cuboid((bx0, by0, FRONT), (bx1, by1, FRONT + depth), {
        "south": {"texture": "#trim", "uv": BOSS_UV},
        **{face: {"texture": "#trim", "uv": PLAIN_UV} for face in ("east", "west", "up", "down")}}))
    for frm, to in HANDLE:
        out.append(_cuboid(frm, to, {face: {"texture": "#back", "uv": STRAP_UV}
                                     for face in ("north", "south", "east", "west", "up", "down")}))
    return out


# Vanilla 26.3's shield display transforms (assets/minecraft/models/item/shield.json and shield_blocking.json, read in
# game), with its front lighting.
DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [10, 6, -4], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [10, 6, 12], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, 5], "translation": [-10, 1.75, -10], "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, 5], "translation": [10, 0, -10], "scale": [1.25, 1.25, 1.25]},
    "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
    "fixed": {"rotation": [0, 180, 0], "translation": [-4.5, 4.5, -5], "scale": [0.55, 0.55, 0.55]},
    "on_shelf": {"rotation": [0, 0, 0], "translation": [11, 18.5, 8.7], "scale": [1.4, 1.4, 1.4]},
    "ground": {"rotation": [0, 0, 0], "translation": [2, 4, 2], "scale": [0.25, 0.25, 0.25]},
}
BLOCKING = {
    "thirdperson_righthand": {"rotation": [45, 155, 0], "translation": [-3.49, 11, -2], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [45, 155, 0], "translation": [11.51, 7, 2.5], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, -5], "translation": [-15, 3.25, -11], "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, -5], "translation": [5, 5, -11], "scale": [1.25, 1.25, 1.25]},
    "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
}

def _scaled(display, factor):
    """Hand poses `factor` times the size: an item's display scales about the model's centre, where a bow is gripped."""
    return {context: {**pose, "scale": [round(v * factor, 3) for v in pose["scale"]]} for context, pose in display.items()}


def _model(name):
    return {"type": "minecraft:model", "model": f"{MOD}:item/{name}"}


def write_all(write, assets, lang):
    """The kit's shared and own models, item definitions and tooltips (names and recipes: tools/arms.py)."""
    models = assets / "models" / "item"
    for kind, info in arms.RANGED_KINDS.items():
        lang[f"tooltip.{MOD}.arms.{kind}"] = info["tooltip"]
        vanilla = BOW_DISPLAY if info["type"] == "bow" else CROSSBOW_DISPLAY
        write(models / f"arms_{kind}.json", {"parent": f"minecraft:item/{info['type']}",
                                             "display": _scaled(vanilla, arms.RANGED_HELD[kind])})
    lang[f"tooltip.{MOD}.arms.longbow.stats"] = "Full draw in %s s. Arrows fly %s%% faster than a bow's."
    lang[f"tooltip.{MOD}.arms.arbalest.stats"] = "Bolts fly %s%% faster and hit %s%% harder than a crossbow's."
    for kind, info in arms.SHIELD_KINDS.items():
        lang[f"tooltip.{MOD}.arms.{kind}"] = info["tooltip"]
        write(models / f"arms_{kind}.json", {"gui_light": "front", "textures": {"particle": "#back"}, "elements": elements(kind),
                                             "display": DISPLAY})
        write(models / f"arms_{kind}_blocking.json", {"parent": f"{MOD}:item/arms_{kind}", "display": BLOCKING})
    for item in arms.kit():
        metal, kind = arms.split(item)
        if kind in arms.SHIELD_KINDS:
            textures = {part: f"{MOD}:item/{item}_{part}" for part in ("face", "back", "trim")}
            for suffix in ("", "_blocking"):
                write(models / f"{item}{suffix}.json", {"parent": f"{MOD}:item/arms_{kind}{suffix}", "textures": textures})
            definition = {"type": "minecraft:condition", "property": "minecraft:using_item",
                          "on_false": _model(item), "on_true": _model(f"{item}_blocking")}
            write(assets / "items" / f"{item}.json", {"model": definition, "swap_animation_scale": 1.0})
            continue
        kind_type = arms.RANGED_KINDS[kind]["type"]
        for suffix in arms.RANGED_SPRITES[kind_type]:
            name = item if suffix == "_standby" else f"{item}{suffix}"
            write(models / f"{name}.json", {"parent": f"{MOD}:item/arms_{kind}",
                                            "textures": {"layer0": f"{MOD}:item/{item}{suffix}"}})
        if kind_type == "bow":
            # Drawn back in three steps, as vanilla's bow, at the same shares of its own (longer) full draw.
            draw = arms.RANGED[(kind, metal)]["draw"] / 20
            definition = {"type": "minecraft:condition", "property": "minecraft:using_item", "on_false": _model(item),
                          "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration",
                                      "scale": 0.05, "fallback": _model(f"{item}_pulling_0"),
                                      "entries": [{"threshold": round(share * draw, 4), "model": _model(f"{item}_pulling_{step}")}
                                                  for step, share in BOW_STEPS]}}
        else:
            # As vanilla's crossbow: shown loaded with an arrow or a firework, else wound back in three steps while used.
            definition = {"type": "minecraft:select", "property": "minecraft:charge_type",
                          "cases": [{"when": "arrow", "model": _model(f"{item}_arrow")},
                                    {"when": "rocket", "model": _model(f"{item}_firework")}],
                          "fallback": {"type": "minecraft:condition", "property": "minecraft:using_item", "on_false": _model(item),
                                       "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:crossbow/pull",
                                                   "fallback": _model(f"{item}_pulling_0"),
                                                   "entries": [{"threshold": threshold, "model": _model(f"{item}_pulling_{step}")}
                                                               for step, threshold in CROSSBOW_STEPS]}}}
        write(assets / "items" / f"{item}.json", {"model": definition, "swap_animation_scale": arms.RANGED_HELD[kind]})


def write_set_shield(write, assets, name, shape):
    """An armor set's shield (tools/arms_variants.py SET_SHIELDS): its shape's shared model and blocking model (held as
    vanilla's shield is), its own models with its textures, and its definition, raised while used."""
    models = assets / "models" / "item"
    write(models / f"arms_{shape}.json", {"gui_light": "front", "textures": {"particle": "#back"}, "elements": elements(shape),
                                          "display": DISPLAY})
    write(models / f"arms_{shape}_blocking.json", {"parent": f"{MOD}:item/arms_{shape}", "display": BLOCKING})
    textures = {part: f"{MOD}:item/{name}_{part}" for part in ("face", "back", "trim")}
    for suffix in ("", "_blocking"):
        write(models / f"{name}{suffix}.json", {"parent": f"{MOD}:item/arms_{shape}{suffix}", "textures": textures})
    definition = {"type": "minecraft:condition", "property": "minecraft:using_item",
                  "on_false": _model(name), "on_true": _model(f"{name}_blocking")}
    write(assets / "items" / f"{name}.json", {"model": definition, "swap_animation_scale": 1.0})


# Vanilla 26.3's bow and crossbow hand poses (assets/minecraft/models/item/bow.json and crossbow.json, read in game), which the
# longbow and arbalest scale up, and the steps their items draw back in (items/bow.json and crossbow.json): a bow's at
# shares of its full draw, a crossbow's at its pull (the share of its load).
BOW_DISPLAY = {
    "thirdperson_righthand": {"rotation": [-80, 260, -40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-80, -280, 40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}
CROSSBOW_DISPLAY = {
    "thirdperson_righthand": {"rotation": [-90, 0, -60], "translation": [2, 0.1, -3], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-90, 0, 30], "translation": [2, 0.1, -3], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [-90, 0, -55], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [-90, 0, 35], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}
BOW_STEPS = [(1, 0.65), (2, 0.9)]
CROSSBOW_STEPS = [(1, 0.58), (2, 1.0)]
