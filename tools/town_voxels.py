"""Block-level helpers for the walled town (tools/town.py): a sparse voxel map of block states, rotation of a
building's states when it is turned to face its street, and the shapes vanilla works out from neighbours (fence,
pane, wall and bar connections, stair corners), computed here so the mod can place every block as it is, without
neighbour updates.

States are strings in the game's own syntax, "minecraft:oak_stairs[facing=north,half=bottom]", with the properties
sorted. Coordinates are (x, y, z): x east, y up, z south.
"""
import json
from pathlib import Path

FACINGS = ["north", "east", "south", "west"]
STEP = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}


def S(name, **props):
    """A block state string: S("oak_stairs", facing="north") -> "minecraft:oak_stairs[facing=north]"."""
    if ":" not in name:
        name = "minecraft:" + name
    if not props:
        return name
    inner = ",".join(f"{k}={str(v).lower()}" for k, v in sorted(props.items()))
    return f"{name}[{inner}]"


def parse(state):
    """Splits a state string into its block id and properties."""
    if "[" not in state:
        return state, {}
    name, rest = state.split("[", 1)
    props = dict(part.split("=", 1) for part in rest.rstrip("]").split(",") if part)
    return name, props


def block_name(state):
    return parse(state)[0].split(":", 1)[1]


class Voxels:
    """A sparse map of block states; later sets win."""

    def __init__(self):
        self.b = {}

    def set(self, x, y, z, state):
        self.b[(x, y, z)] = state

    def get(self, x, y, z, default=None):
        return self.b.get((x, y, z), default)

    def remove(self, x, y, z):
        self.b.pop((x, y, z), None)

    def box(self, x0, y0, z0, x1, y1, z1, state):
        """Fills the box between two corners (both included)."""
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.b[(x, y, z)] = state(x, y, z) if callable(state) else state

    def walls(self, x0, y0, z0, x1, y1, z1, state):
        """The four side walls of a box (no floor or ceiling)."""
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in (z0, z1):
                    self.b[(x, y, z)] = state(x, y, z) if callable(state) else state
        for z in range(z0, z1 + 1):
            for y in range(y0, y1 + 1):
                for x in (x0, x1):
                    self.b[(x, y, z)] = state(x, y, z) if callable(state) else state

    def paste(self, other, dx=0, dy=0, dz=0, turns=0, origin_size=None):
        """Copies another map in, turned clockwise (seen from above) `turns` quarter turns about its own local frame of
        size origin_size = (width, depth), then moved by (dx, dy, dz)."""
        for (x, y, z), state in other.b.items():
            rx, rz = turn_xz(x, z, turns, origin_size)
            self.b[(rx + dx, y + dy, rz + dz)] = turn_state(state, turns)

    def bounds(self):
        xs = [p[0] for p in self.b]
        ys = [p[1] for p in self.b]
        zs = [p[2] for p in self.b]
        return min(xs), min(ys), min(zs), max(xs), max(ys), max(zs)


def turn_xz(x, z, turns, size):
    """Turns a local position clockwise (north up, east right) within a frame of `size` = (width, depth), so the
    turned frame again starts at 0, 0."""
    w, d = size
    turns %= 4
    if turns == 0:
        return x, z
    if turns == 1:
        return d - 1 - z, x
    if turns == 2:
        return w - 1 - x, d - 1 - z
    return z, w - 1 - x


def turn_facing(facing, turns):
    if facing not in FACINGS:
        return facing
    return FACINGS[(FACINGS.index(facing) + turns) % 4]


def turn_state(state, turns):
    turns %= 4
    if turns == 0:
        return state
    name, props = parse(state)
    out = {}
    for key, value in props.items():
        if key == "facing":
            out[key] = turn_facing(value, turns)
        elif key == "axis" and value in ("x", "z") and turns % 2:
            out[key] = "z" if value == "x" else "x"
        elif key == "rotation":
            out[key] = str((int(value) + 4 * turns) % 16)
        elif key in FACINGS:
            continue  # connections are worked out again afterwards
        elif key == "shape" and "stairs" in name:
            continue
        else:
            out[key] = value
    return S(name, **out)


# ---------------------------------------------------------------- shapes vanilla computes from neighbours

FENCE_LIKE = ("_fence", "iron_bars", "_pane", "_wall")


def kind(state):
    """'fence', 'pane' (panes and bars), 'wall', 'gate', 'stairs' or None."""
    if state is None:
        return None
    name = block_name(state)
    if name.endswith("_fence_gate"):
        return "gate"
    if name.endswith("_fence"):
        return "fence"
    if name.endswith("_pane") or name in ("iron_bars",) or name.endswith("_bars"):
        return "pane"
    if name.endswith("_wall") and not name.endswith("_sign") and "banner" not in name and "torch" not in name \
            and "skull" not in name and "head" not in name and "fan" not in name:
        return "wall"
    if name.endswith("_stairs"):
        return "stairs"
    return None


NOT_FULL_SUFFIXES = ("_stairs", "_slab", "_fence", "_fence_gate", "_wall", "_pane", "_door", "_trapdoor", "_carpet",
                     "_button", "_pressure_plate", "torch", "lantern", "_sign", "_banner", "_bed", "_rail", "rail",
                     "ladder", "chain", "_bars", "candle", "_head", "_skull", "_leaves", "_sapling", "_flower", "_petals",
                     "_roots", "_fungus", "_mushroom", "_coral", "_fan", "_bush", "_pot", "_tulip", "_orchid", "_bluet",
                     "_daisy", "_cluster", "_bud", "_plate", "_lichen", "_vines", "vine", "_frame", "_rod", "_stem")
NOT_FULL = {"air", "water", "lava", "chest", "trapped_chest", "ender_chest", "campfire", "soul_campfire", "lectern", "bell",
            "anvil", "chipped_anvil", "damaged_anvil", "flower_pot", "cobweb", "dirt_path", "farmland", "pumpkin",
            "carved_pumpkin", "jack_o_lantern", "melon", "snow", "grindstone", "stonecutter", "enchanting_table",
            "brewing_stand", "cauldron", "water_cauldron", "composter", "hopper", "scaffolding", "cake", "end_rod",
            "lightning_rod", "decorated_pot", "short_grass", "tall_grass", "fern", "large_fern", "poppy", "dandelion",
            "cornflower", "allium", "lily_of_the_valley", "sunflower", "lilac", "rose_bush", "peony", "glass",
            "barrier", "sea_pickle", "hay_block_slab", "bamboo", "sugar_cane", "lily_pad", "conduit", "beacon",
            "spawner", "dead_bush", "azalea", "flowering_azalea", "moss_carpet", "pink_petals"}


def is_full(state, full_blocks=None):
    """Whether a state is a full, sturdy cube that fences, panes and walls join (judged from its block id: anything
    that is not a known partial shape). Leaves, pumpkins and melons count as partial, as vanilla excepts them."""
    if state is None:
        return False
    name = block_name(state)
    if name.startswith("potted_") or name in NOT_FULL:
        return False
    return not name.endswith(NOT_FULL_SUFFIXES)


def connections(vox, full_blocks=None):
    """Sets fence, pane, bar and wall connections from their neighbours, as the game would."""
    for (x, y, z), state in list(vox.b.items()):
        k = kind(state)
        if k not in ("fence", "pane", "wall"):
            continue
        name, props = parse(state)
        for facing in FACINGS:
            dx, dz = STEP[facing]
            other = vox.get(x + dx, y, z + dz)
            ok = kind(other)
            joins = is_full(other, full_blocks)
            if k == "fence":
                # Wooden fences join wooden fences, the nether brick fence only its own kind.
                same = ok == "fence" and (block_name(other) == "nether_brick_fence") == (block_name(state) == "nether_brick_fence")
                joins = joins or same or ok == "gate" and gate_faces(other, facing)
            elif k == "pane":
                joins = joins or ok in ("pane", "wall")
            else:
                joins = joins or ok in ("wall", "pane") or ok == "gate" and gate_faces(other, facing)
            if k == "wall":
                above = vox.get(x, y + 1, z)
                tall = above is not None and (is_full(above, full_blocks) or kind(above) == "wall")
                props[facing] = ("tall" if tall else "low") if joins else "none"
            else:
                props[facing] = "true" if joins else "false"
        if k == "wall":
            props["up"] = "true" if wall_post(props, vox.get(x, y + 1, z)) else "false"
        vox.b[(x, y, z)] = S(name, **props)


def gate_faces(gate, facing):
    """Whether a fence gate joins a neighbour on the side `facing` of that neighbour (gates join along their line)."""
    g = parse(gate)[1].get("facing", "north")
    along = ("east", "west") if g in ("north", "south") else ("north", "south")
    return facing in along


def wall_post(props, above):
    """A wall shows its centre post unless it runs straight through on one axis with nothing on top."""
    ns = props.get("north") != "none" and props.get("south") != "none" and props.get("east") == "none" and props.get("west") == "none"
    ew = props.get("east") != "none" and props.get("west") != "none" and props.get("north") == "none" and props.get("south") == "none"
    return not (ns or ew) or above is not None and block_name(above).endswith(("_wall", "torch", "lantern"))


def stair_shapes(vox):
    """Sets stair corner shapes from their neighbours, as StairBlock does."""
    for (x, y, z), state in list(vox.b.items()):
        if kind(state) != "stairs":
            continue
        name, props = parse(state)
        props["shape"] = stair_shape(vox, x, y, z, props)
        vox.b[(x, y, z)] = S(name, **props)


def _stair(vox, x, y, z):
    state = vox.get(x, y, z)
    return parse(state)[1] if kind(state) == "stairs" else None


def stair_shape(vox, x, y, z, props):
    facing, half = props.get("facing", "north"), props.get("half", "bottom")
    fx, fz = STEP[facing]
    # Behind (in the direction it faces) and in front.
    back = _stair(vox, x + fx, y, z + fz)
    if back and back.get("half", "bottom") == half:
        bf = back.get("facing", "north")
        if axis(bf) != axis(facing) and can_take(vox, x, y, z, props, OPPOSITE[bf]):
            return "outer_left" if bf == ccw(facing) else "outer_right"
    front = _stair(vox, x - fx, y, z - fz)
    if front and front.get("half", "bottom") == half:
        ff = front.get("facing", "north")
        if axis(ff) != axis(facing) and can_take(vox, x, y, z, props, ff):
            return "inner_left" if ff == ccw(facing) else "inner_right"
    return "straight"


def can_take(vox, x, y, z, props, side):
    """Vanilla's canTakeShape: the stair on `side` is not one facing the same way at the same half."""
    dx, dz = STEP[side]
    other = _stair(vox, x + dx, y, z + dz)
    return not (other and other.get("facing") == props.get("facing") and other.get("half", "bottom") == props.get("half", "bottom"))


def axis(facing):
    return "x" if facing in ("east", "west") else "z"


def ccw(facing):
    return FACINGS[(FACINGS.index(facing) + 3) % 4]


# ---------------------------------------------------------------- vanilla block states (local check only)

SUMMARY = Path("/home/user/misode/summary263/blocks/data.json")


def vanilla_states():
    """26.3's block state definitions when a local copy of misode/mcmeta's summary is at hand (not in CI): used to check
    every state the generator writes. Returns None when it isn't."""
    if not SUMMARY.exists():
        return None
    return json.loads(SUMMARY.read_text())


def check_state(state, summary, errors):
    name, props = parse(state)
    ns, block = name.split(":", 1)
    if ns != "minecraft" or summary is None:
        return
    entry = summary.get(block)
    if entry is None:
        errors.append(f"unknown block {name}")
        return
    allowed = entry[0]
    for key, value in props.items():
        if key not in allowed:
            errors.append(f"{state}: {block} has no property {key}")
        elif value not in allowed[key]:
            errors.append(f"{state}: {key} cannot be {value}")
