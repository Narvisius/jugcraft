"""Diagonal connections (docs/features/diagonal-connections.md) for tools/generate_material_data.py.

Fences, glass panes, bars and walls join diagonally as well as straight. The Java side (diagonal/DiagonalConnections)
gives every FenceBlock and IronBarsBlock four more properties, north_east, south_east, south_west and north_west, and
sets them only for blocks in the block tag #jugcraft:connects_diagonally. A vanilla wall in the tag keeps its own states
and becomes its diagonal wall, jugcraft:diagonal_<wall> (diagonal/DiagonalWallBlock), while it joins diagonally. Here
each block in the tag gets:

- a diagonal arm model, jugcraft:block/diagonal/<block>: a child of the block's own side model (so it keeps the
  block's textures, translucency and lighting) whose elements are turned 45 degrees about the post. Minecraft's
  "rescale" stretches a 45-degree element by the square root of 2 across the block, so an arm drawn from the edge to the
  middle reaches the corner; the widths are drawn narrower by the same factor so they come out as wide as the
  straight arm's.
- four more multipart parts in its blockstate: that arm turned 0, 90, 180 and 270 degrees for north_east, south_east,
  south_west and north_west.

Jugcraft's own walls (JUGCRAFT_WALLS) are treated exactly as vanilla's.

Vanilla's fences, panes and bars are rebuilt in assets/minecraft/blockstates: vanilla's own parts, naming vanilla's models
by ID, plus the diagonals. Vanilla's walls are left as they are; each diagonal wall gets a blockstate of its own: the
wall's post, its low sides and the diagonals. Jugcraft's fences get their diagonal parts added to the blockstates their own
generators wrote, so this runs last in generate_material_data.assets().
"""
import json
import math

MOD = "jugcraft"
TAG = f"{MOD}:connects_diagonally"
# Each diagonal property and the y rotation that turns the north-east arm toward it.
DIAGONALS = (("north_east", 0), ("south_east", 90), ("south_west", 180), ("north_west", 270))
DIAGONAL_NAMES = [name for name, _y in DIAGONALS]
# Minecraft turns a y rotation of +45 degrees counter-clockwise seen from above, so -45 takes the north arm north-east.
ANGLE = -45
NARROW = 1 / math.sqrt(2)
# How much of a texture an arm from the corner to the middle shows: the corner is 8 * sqrt(2) pixels from the middle.
ARM_UV = round(16 - 8 * math.sqrt(2), 4)

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped",
         "pale_oak", "poplar"]
VANILLA_FENCES = [f"{wood}_fence" for wood in WOODS] + ["nether_brick_fence"]
COLOURS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
           "blue", "brown", "green", "red", "black"]
VANILLA_PANES = ["glass_pane"] + [f"{colour}_stained_glass_pane" for colour in COLOURS]
# Each bars block and the model family it draws with (waxed copper bars use the unwaxed models).
VANILLA_BARS = {"iron_bars": "iron_bars"}
for _age in ("", "exposed_", "weathered_", "oxidized_"):
    VANILLA_BARS[f"{_age}copper_bars"] = f"{_age}copper_bars"
    VANILLA_BARS[f"waxed_{_age}copper_bars"] = f"{_age}copper_bars"
STONES = ["andesite", "blackstone", "brick", "cinnabar_brick", "cinnabar", "cobbled_deepslate", "cobblestone",
          "deepslate_brick", "deepslate_tile", "diorite", "end_stone_brick", "granite", "mossy_cobblestone",
          "mossy_stone_brick", "mud_brick", "nether_brick", "polished_blackstone_brick", "polished_blackstone",
          "polished_cinnabar", "polished_deepslate", "polished_sulfur", "polished_tuff", "prismarine", "red_nether_brick",
          "red_sandstone", "resin_brick", "sandstone", "stone_brick", "sulfur_brick", "sulfur", "tuff_brick", "tuff"]
VANILLA_WALLS = [f"{stone}_wall" for stone in STONES]
# Jugcraft's own walls (batch 55's bastion concrete wall): each gets a diagonal wall exactly as vanilla's do.
JUGCRAFT_WALLS = ["bastion_concrete_wall"]
# The block each wall becomes while it joins diagonally.
DIAGONAL_WALL = "diagonal_{}"


def all_walls():
    """(namespace, name) of every wall with a diagonal wall: vanilla's, then Jugcraft's."""
    return [("minecraft", name) for name in VANILLA_WALLS] + [(MOD, name) for name in JUGCRAFT_WALLS]


def jugcraft_fences():
    """Jugcraft's own fences: every wood set's and the wrought-iron cemetery fence."""
    import agriculture
    return [f"{wood}_fence" for wood in agriculture.WOOD_SETS] + [agriculture.CEMETERY_FENCE["fence"]]


def blocks():
    """Every block in #jugcraft:connects_diagonally, as IDs."""
    return ([f"minecraft:{name}" for name in VANILLA_FENCES + VANILLA_PANES + list(VANILLA_BARS) + VANILLA_WALLS]
            + [f"{MOD}:{name}" for name in jugcraft_fences() + JUGCRAFT_WALLS])


# ---------------------------------------------------------------- arm geometry (the north-east arm)

def _rotation():
    return {"origin": [8, 8, 8], "axis": "y", "angle": ANGLE, "rescale": True}


def _narrow(half_width):
    """x from and to for an arm `half_width` pixels either side of the middle once rescale has widened it."""
    half = round(half_width * NARROW, 5)
    return round(8 - half, 5), round(8 + half, 5)


def fence_arm():
    """Two rails, as wide (2) and high (6-9 and 12-15) as a vanilla fence's, from the corner into the post."""
    x0, x1 = _narrow(1)
    rails = []
    for low, high, v in ((12, 15, 1), (6, 9, 7)):
        rails.append({"from": [x0, low, 0], "to": [x1, high, 8], "rotation": _rotation(), "faces": {
            "down": {"uv": [7, 0, 9, 8], "texture": "#texture"},
            "up": {"uv": [7, 0, 9, 8], "texture": "#texture"},
            "north": {"uv": [7, v, 9, v + 3], "texture": "#texture"},
            "west": {"uv": [0, v, 8, v + 3], "texture": "#texture"},
            "east": {"uv": [0, v, 8, v + 3], "texture": "#texture"}}})
    return rails


def bamboo_fence_arm():
    """The fence's rails, drawn from the bamboo fence texture's own regions (its sheet holds the rail's side, top and
    end in fixed places, as vanilla's bamboo fence side model uses them)."""
    x0, x1 = _narrow(1)
    return [{"from": [x0, low, 0], "to": [x1, high, 8], "rotation": _rotation(), "faces": {
        "down": {"uv": [13, 7, 15, 15], "texture": "#texture"},
        "up": {"uv": [13, 7, 15, 15], "texture": "#texture"},
        "north": {"uv": [13, 4, 15, 7], "rotation": 180, "texture": "#texture"},
        "west": {"uv": [4, 4, 12, 7], "texture": "#texture"},
        "east": {"uv": [4, 4, 12, 7], "texture": "#texture"}}} for low, high in ((12, 15), (6, 9))]


def pane_arm():
    """A pane as thick (2) and tall as a vanilla pane's side, from the corner to the middle; the glass is not stretched."""
    x0, x1 = _narrow(1)
    return [{"from": [x0, 0, 0], "to": [x1, 16, 8], "rotation": _rotation(), "faces": {
        "down": {"uv": [7, 0, 9, 8], "texture": "#edge"},
        "up": {"uv": [7, 0, 9, 8], "texture": "#edge"},
        "north": {"uv": [7, 0, 9, 16], "texture": "#edge"},
        "west": {"uv": [16, 0, ARM_UV, 16], "texture": "#pane"},
        "east": {"uv": [ARM_UV, 0, 16, 16], "texture": "#pane"}}}]


def bars_arm():
    """A flat plane of bars from the corner to the middle, with an edge strip at its end, top and bottom."""
    x0, x1 = _narrow(1)
    return [
        {"from": [8, 0, 0], "to": [8, 16, 8], "rotation": _rotation(), "faces": {
            "west": {"uv": [16, 0, ARM_UV, 16], "texture": "#bars"},
            "east": {"uv": [ARM_UV, 0, 16, 16], "texture": "#bars"}}},
        {"from": [x0, 0, 0], "to": [x1, 16, 0], "rotation": _rotation(), "faces": {
            "north": {"uv": [7, 0, 9, 16], "texture": "#edge"}}},
        {"from": [x0, 0.001, 0], "to": [x1, 0.001, 8], "rotation": _rotation(), "faces": {
            "down": {"uv": [9, 0, 7, 8], "texture": "#edge"}, "up": {"uv": [7, 0, 9, 8], "texture": "#edge"}}},
        {"from": [x0, 15.999, 0], "to": [x1, 15.999, 8], "rotation": _rotation(), "faces": {
            "down": {"uv": [9, 0, 7, 8], "texture": "#edge"}, "up": {"uv": [7, 0, 9, 8], "texture": "#edge"}}},
    ]


def wall_arm(height=14):
    """A wall side, as wide (6) as vanilla's and `height` high (14 low, 16 tall), from the corner to the middle; the
    stone is not stretched along it."""
    x0, x1 = _narrow(3)
    length = round(16 - ARM_UV, 4)
    top = 16 - height
    return [{"from": [x0, 0, 0], "to": [x1, height, 8], "rotation": _rotation(), "faces": {
        "down": {"uv": [5, ARM_UV, 11, 16], "texture": "#wall"},
        "up": {"uv": [5, 0, 11, length], "texture": "#wall"},
        "north": {"uv": [5, top, 11, 16], "texture": "#wall"},
        "west": {"uv": [0, top, length, 16], "texture": "#wall"},
        "east": {"uv": [ARM_UV, top, 16, 16], "texture": "#wall"}}}]


def turned(elements):
    """Any north side model's elements as a diagonal arm. Parts that run to the block's edge (rails) are stretched to
    the corner; the rest (pickets, finials) keep their size and are spread along the arm. Widths are kept. Elements that
    are already rotated cannot take a second rotation and are left out."""
    out = []
    for element in elements:
        if "rotation" in element:
            continue
        (fx, fy, fz), (tx, ty, tz) = element["from"], element["to"]
        x0, x1 = round(8 + (fx - 8) * NARROW, 5), round(8 + (tx - 8) * NARROW, 5)
        if fz <= 0.001:
            z0, z1 = fz, tz
        else:
            middle, half = (fz + tz) / 2, (tz - fz) / 2 * NARROW
            z0, z1 = round(middle - half, 5), round(middle + half, 5)
        faces = {name: {key: value for key, value in face.items() if key != "cullface"} for name, face in element["faces"].items()}
        out.append(dict(element, **{"from": [x0, fy, z0], "to": [x1, ty, z1], "rotation": _rotation(), "faces": faces}))
    return out


ARMS = {"fence": fence_arm, "bamboo_fence": bamboo_fence_arm, "pane": pane_arm, "bars": bars_arm}


# ---------------------------------------------------------------- blockstates

def _part(model, when=None, y=0, uvlock=False):
    apply = {"model": model}
    if uvlock:
        apply["uvlock"] = True
    if y:
        apply["y"] = y
    part = {"apply": apply}
    if when:
        part["when"] = when
    return part


def diagonal_parts(arm_model, uvlock=False, when=None):
    return [_part(arm_model, {name: "true", **(when or {})}, y, uvlock) for name, y in DIAGONALS]


def vanilla_fence(name):
    """Vanilla's fence blockstate: the post and a side for each connection (uvlocked, as vanilla's). The bamboo fence
    has its own side model for each direction instead."""
    parts = [_part(f"minecraft:block/{name}_post")]
    if name == "bamboo_fence":
        for direction in ("north", "east", "south", "west"):
            parts.append(_part(f"minecraft:block/{name}_side_{direction}", {direction: "true"}))
        return parts, f"minecraft:block/{name}_side_north"
    side = f"minecraft:block/{name}_side"
    for direction, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        parts.append(_part(side, {direction: "true"}, y, uvlock=True))
    return parts, side


def vanilla_pane(name):
    """Vanilla's pane blockstate: the post, sides where joined and the bare post faces where not."""
    m = f"minecraft:block/{name}"
    parts = [_part(f"{m}_post"),
             _part(f"{m}_side", {"north": "true"}), _part(f"{m}_side", {"east": "true"}, 90),
             _part(f"{m}_side_alt", {"south": "true"}), _part(f"{m}_side_alt", {"west": "true"}, 90),
             _part(f"{m}_noside", {"north": "false"}), _part(f"{m}_noside_alt", {"east": "false"}),
             _part(f"{m}_noside_alt", {"south": "false"}, 90), _part(f"{m}_noside", {"west": "false"}, 270)]
    return parts, f"{m}_side"


def vanilla_bars(family):
    """Vanilla's bars blockstate: the post ends, the post when unjoined, a cap when joined one way, and the sides."""
    m = f"minecraft:block/{family}"
    only = lambda direction: {d: "true" if d == direction else "false" for d in ("east", "north", "south", "west")}
    parts = [_part(f"{m}_post_ends"),
             _part(f"{m}_post", {"east": "false", "north": "false", "south": "false", "west": "false"}),
             _part(f"{m}_cap", only("north")), _part(f"{m}_cap", only("east"), 90),
             _part(f"{m}_cap_alt", only("south")), _part(f"{m}_cap_alt", only("west"), 90),
             _part(f"{m}_side", {"north": "true"}), _part(f"{m}_side", {"east": "true"}, 90),
             _part(f"{m}_side_alt", {"south": "true"}), _part(f"{m}_side_alt", {"west": "true"}, 90)]
    return parts, f"{m}_side"


def vanilla_wall(name):
    """Vanilla's wall blockstate: the post when raised, and a low or tall side for each connection (uvlocked)."""
    m = f"minecraft:block/{name}"
    parts = [_part(f"{m}_post", {"up": "true"})]
    for model, height in ((f"{m}_side", "low"), (f"{m}_side_tall", "tall")):
        for direction, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            parts.append(_part(model, {direction: height}, y, uvlock=True))
    return parts, f"{m}_side"


def diagonal_wall(name, namespace="minecraft"):
    """A diagonal wall's own parts: the wall's post when raised and, for each joined side, its low side, or its tall
    side when the diagonal wall is tall (uvlocked, as vanilla's)."""
    m = f"{namespace}:block/{name}"
    parts = [_part(f"{m}_post", {"up": "true"})]
    for model, tall in ((f"{m}_side", "false"), (f"{m}_side_tall", "true")):
        for direction, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            parts.append(_part(model, {direction: "true", "tall": tall}, y, uvlock=True))
    return parts


def wall_arm_models(name):
    """A diagonal wall's arm models: low, and tall."""
    return f"{MOD}:block/diagonal/{name}", f"{MOD}:block/diagonal/{name}_tall"


def vanilla_blockstates():
    """name -> (vanilla's own parts, its north side model, arm kind)."""
    out = {}
    for name in VANILLA_FENCES:
        out[name] = vanilla_fence(name) + ("bamboo_fence" if name == "bamboo_fence" else "fence",)
    for name in VANILLA_PANES:
        out[name] = vanilla_pane(name) + ("pane",)
    for name, family in VANILLA_BARS.items():
        out[name] = vanilla_bars(family) + ("bars",)
    return out


def _north_side(parts):
    """The part a cross-collision blockstate shows for a north connection: its model and whether it is uvlocked."""
    for part in parts:
        if part.get("when") == {"north": "true"} and not part["apply"].get("y"):
            return part["apply"]["model"], part["apply"].get("uvlock", False)
    return None, False


def _load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_all(write, assets_dir, minecraft_assets_dir):
    """The diagonal arm models, vanilla's rebuilt blockstates and Jugcraft's fences' diagonal parts."""
    models = assets_dir / "models" / "block" / "diagonal"
    for name, (parts, side, kind) in vanilla_blockstates().items():
        arm = f"{MOD}:block/diagonal/{name}"
        write(models / f"{name}.json", {"parent": side, "elements": ARMS[kind]()})
        write(minecraft_assets_dir / "blockstates" / f"{name}.json",
              {"multipart": parts + diagonal_parts(arm, uvlock=kind == "fence")})  # as vanilla uvlocks its fence sides
    for namespace, name in all_walls():
        low, tall = wall_arm_models(name)
        write(models / f"{name}.json", {"parent": f"{namespace}:block/{name}_side", "elements": wall_arm()})
        write(models / f"{name}_tall.json", {"parent": f"{namespace}:block/{name}_side_tall", "elements": wall_arm(16)})
        write(assets_dir / "blockstates" / f"{DIAGONAL_WALL.format(name)}.json",
              {"multipart": diagonal_wall(name, namespace) + diagonal_parts(low, True, {"tall": "false"})
               + diagonal_parts(tall, True, {"tall": "true"})})
    for name in jugcraft_fences():
        path = assets_dir / "blockstates" / f"{name}.json"
        state = _load(path)
        side, uvlock = _north_side(state["multipart"])
        if side is None:
            raise ValueError(f"{name}: no north side part to turn diagonal")
        side_model = _load(assets_dir / "models" / "block" / f"{side.split(':', 1)[1].removeprefix('block/')}.json")
        elements = turned(side_model["elements"]) if "elements" in side_model else fence_arm()
        arm = f"{MOD}:block/diagonal/{name}"
        write(models / f"{name}.json", {"parent": side, "elements": elements})
        state["multipart"] = [p for p in state["multipart"] if not set(p.get("when", {})) & {d for d, _ in DIAGONALS}]
        state["multipart"] += diagonal_parts(arm, uvlock)
        write(path, state)


def arm_models(block):
    """The arm models a tagged block's blockstate names: its arm, and a wall's tall arm too."""
    namespace, name = block.split(":")
    if (namespace, name) in all_walls():
        return wall_arm_models(name)
    return (f"{MOD}:block/diagonal/{name}",)


def arm_blockstate(block):
    """(namespace, blockstate name) of the blockstate that shows a tagged block's diagonal arms: a vanilla wall's are its
    diagonal wall's."""
    namespace, name = block.split(":")
    if (namespace, name) in all_walls():
        return MOD, DIAGONAL_WALL.format(name)
    return namespace, name


def tags(tags):
    for block in blocks():
        tags.add("block", TAG, block)
    # Diagonal walls are walls to everything else: walls, fence gates and bars join them, pickaxes mine them, mobs
    # treat them as walls.
    for _, name in all_walls():
        tags.add("block", "minecraft:walls", f"{MOD}:{DIAGONAL_WALL.format(name)}")
