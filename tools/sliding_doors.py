"""Automatic sliding doors (tower/SlidingDoorBlock.java, SlidingDoorStyle.java; Justin, 8 November 2026: "I really like
these server doors ... add similar ones to the front of the facility and then other places"): one helper for every door
of every kind, wherever a generator puts one.

place() writes a door whole, as the block places itself: its master (the bottom cell at one end, x 0 of the door's own
frame) and a sliding_door_part in each other cell, knowing its offset from the master and whether mobs may plan a way
through it. The door's frame: the door facing north (its outside, where a room door's leaves hang, at -z), x across from
the master, y up; ``outside`` is the way the outside faces (the side you walk up from). A double room door's second cell
is to the walker's left (frame +x), as the tower's hinged pairs were; a single's leaf slides to the walker's left unless
``mirrored`` (then to the right).

check() is run on the finished tower: a room door's leaves slide a block along the wall's outside face beside its doorway
and hang from a track cover over it, so the cells outside the wall there (the doorway's height and the row over it) must
be empty and the wall there standing (glass or solid) for the leaves to slide over.
"""

STYLES = {
    # style: (block, width, height, leaves)
    "entrance": ("automatic_entrance_door", 6, 3, 2),
    "double_glass": ("automatic_sliding_door_glass", 2, 2, 2),
    "double_steel": ("automatic_sliding_door_steel", 2, 2, 2),
    "single_glass": ("automatic_sliding_door_single_glass", 1, 2, 1),
    "single_steel": ("automatic_sliding_door_single_steel", 1, 2, 1),
}
PART = "jugcraft:sliding_door_part"
SPAN = 5                       # SlidingDoorPartBlock.SPAN: the farthest a cell is from its master across
MASTERS = {f"jugcraft:{block}" for block, *_ in STYLES.values()}
NAMES = MASTERS | {PART}
PLACED = []                    # every door place() has written: (x, y, z, style, outside, mirrored)


def turn(dx, dz, facing):
    """A frame offset turned to face ``facing`` (AisleContainmentDoorBlock.turn)."""
    return {"east": (-dz, dx), "south": (-dx, -dz), "west": (dz, -dx)}.get(facing, (dx, dz))


def frame_x(ox, oz, facing):
    """The frame's x of a cell at (ox, oz) from the master (SlidingDoorBlock.frameCell)."""
    back = {"east": "west", "west": "east"}.get(facing, facing)
    return turn(ox, oz, back)[0]


def walkable(style, dx, dy):
    """SlidingDoorStyle.walkable: the entrance's doorway cells (its sidelights' and header's no); a room door's all."""
    _, w, _, _ = STYLES[style]
    if style == "entrance":
        return dy < 2 and 2 <= dx <= w - 3
    return True


def states(style, outside, mirrored=False):
    """{(ox, oy, oz): block state} for every cell of a door, the master at (0, 0, 0)."""
    block, w, h, _ = STYLES[style]
    out = {}
    for dy in range(h):
        for dx in range(w):
            ox, oz = turn(dx, 0, outside)
            if (dx, dy) == (0, 0):
                out[(0, 0, 0)] = f"jugcraft:{block}[facing={outside},mirrored={str(mirrored).lower()},open=false]"
            else:
                walk = str(walkable(style, frame_x(ox, oz, outside), dy)).lower()
                out[(ox, dy, oz)] = f"{PART}[ox={ox + SPAN},oy={dy},oz={oz + SPAN},walk={walk}]"
    return out


def place(put, x, y, z, style, outside, mirrored=False):
    """Writes the door with its master at (x, y, z): put(x, y, z, state) for each cell."""
    for (ox, oy, oz), s in states(style, outside, mirrored).items():
        put(x + ox, y + oy, z + oz, s)
    PLACED.append((x, y, z, style, outside, mirrored))


def _cell(x, y, z, outside, fx, fy, fz):
    ox, oz = turn(fx, fz, outside)
    return (x + ox, y + fy, z + oz)


def slide_cells(x, y, z, style, outside, mirrored=False):
    """A room door's cells beside its doorway: (outside, wall) for each, the cells outside the wall a leaf slides into
    (the doorway's two rows) or its track cover runs over (the row above), and the wall cells the leaves slide over."""
    _, w, _, leaves = STYLES[style]
    if style == "entrance":
        return [], []
    sides = (-1, w) if leaves == 2 else ((-1,) if mirrored else (w,))
    outside_cells, wall = [], []
    for fx in sides:
        for fy in (0, 1, 2):
            outside_cells.append(_cell(x, y, z, outside, fx, fy, -1))
        for fy in (0, 1):
            wall.append(_cell(x, y, z, outside, fx, fy, 0))
    for fx in range(w):           # the track cover over the doorway
        outside_cells.append(_cell(x, y, z, outside, fx, 2, -1))
    return outside_cells, wall


def clear(free, x, y, z, style, outside, mirrored=False):
    """Whether the cells outside the wall a room door's leaves need are clear (free(x, y, z): nothing there yet)."""
    out, _ = slide_cells(x, y, z, style, outside, mirrored)
    return all(free(*c) for c in out)


def passable(state):
    """Whether the crew walk through this cell of a sliding door (its doorway's bottom row)."""
    if state is None:
        return False
    n = state.split("[")[0]
    if n == PART:
        return "oy=0" in state and "walk=true" in state
    return n in MASTERS and n != "jugcraft:automatic_entrance_door"


def check(get):
    """Every placed room door's leaves free to slide (get(x, y, z): what stands there, or None)."""
    for x, y, z, style, outside, mirrored in PLACED:
        assert (get(x, y, z) or "").startswith("jugcraft:" + STYLES[style][0]), ("the door stands", x, y, z, style, get(x, y, z))
        out, wall = slide_cells(x, y, z, style, outside, mirrored)
        for c in out:
            assert get(*c) is None, ("a sliding door's leaves and track need this cell outside the wall clear", (x, y, z), style, c, get(*c))
        for c in wall:
            assert get(*c) is not None, ("a sliding door's leaves slide over the wall here: it must stand", (x, y, z), style, c)
