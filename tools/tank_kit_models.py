"""The tank kits' item models: the very tank the kit places, in miniature and in 3D (Justin, Oct 8: "make the tanks look
like they actually do in game, so people know what they're actually spawning in").

Each kit's model is built from its own structure (tools/tank_kits.py): every block of the formed tank becomes a scaled
box in the block's own paint (the shell painted for the kit's rating, band by band, as the outlet paints it when it
forms), and the small fittings (railing, ladder, nozzles, vents, foam chambers, vaporizer, legs) become
small boxes in their colours. The whole tank is scaled to fit one block, so the slot shows it like a block item, turned
so its front (where the label goes) faces the viewer's right and the outlet side the left. Like a block item it is a 3D
model, not a flat item icon (docs/ITEM_ICONS.md: "block items that show their 3D block model in the slot" are not item
icons).
"""
import math

import model_writer as MW
import nuclear_plants as P
import medium_plant as MP
import storage_tank as ST
import tank_kits as TK

RED, BLUE, BRASS, COPPER, FIN, GREEN = "sp_red_iron", "st_blue", "st_brass", "st_copper", "st_fin", "st_green"
GUN, GALV, PIPE, YELLOW, WHITE = "dp_gunmetal", "st_galv", "st_pipe", "pi_rail_yellow", "st_white"
STONE = "smooth"            # vanilla smooth stone (the foundation)
FULL = "full"
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


def _state(cell):
    name, _, props = cell.partition("[")
    out = {}
    for kv in props.rstrip("]").split(","):
        if "=" in kv:
            k, v = kv.split("=")
            out[k] = v
    return name.split(":")[1] if ":" in name else name, out


def kit_info(kit):
    """(part, rating, shell radius, shell height, y of the shell's first course) of a kit."""
    b, _ = TK.kits()[kit]
    if kit == "cryogenic":
        r, h = TK.CRYO
        return b, "cryogenic", r, h, 3
    rating, _, size = kit.partition("_")
    r, h = TK.SIZES[size]
    return b, rating, r, h, 1


def cells_of(kit):
    """{(x, y, z): (block, props)} of the kit as it stands formed, cropped to its blocks."""
    b, rating, r, h, y1 = kit_info(kit)
    raw = {k: _state(v) for k, v in P.part_cells(b).items() if v != "minecraft:air"}
    W, _, D = b.g.shape
    c = W / 2
    roof = y1 + h
    out = {}
    for (x, y, z), (name, props) in raw.items():
        if name in ("storage_tank_plate", "storage_tank_hatch"):
            if y == roof:
                inside = math.hypot(x + 0.5 - c, z + 0.5 - c) < r and (x, y - 1, z) not in raw
                band = "roof" if inside else "rim"
            elif y == roof - 1:
                band = "band"
            elif y == roof - 2 and roof - 2 > y1:
                band = "stripe"
            else:
                band = "wall"
            props = dict(props, band=band)
        out[(x, y, z)] = (name, props)
    xs = [k[0] for k in out]
    ys = [k[1] for k in out]
    zs = [k[2] for k in out]
    x0, y0, z0 = min(xs), min(ys), min(zs)
    return {(x - x0, y - y0, z - z0): v for (x, y, z), v in out.items()}, rating


def _fill_kind(name, props, rating):
    """The paint of a block drawn as a full cube, or None for a fitting."""
    if name in ("storage_tank_plate", "storage_tank_hatch"):
        return ST.PAINT[rating][props["band"]]
    if name == "smooth_stone":
        return STONE
    return None


def item_model(kit, rid):
    cells, rating = cells_of(kit)
    W = max(x for x, _, _ in cells) + 1
    H = max(y for _, y, _ in cells) + 1
    D = max(z for _, _, z in cells) + 1
    s = 15.0 / max(W, H, D)
    ox, oz = (16 - W * s) / 2, (16 - D * s) / 2

    # Turned half round: the kit's front (south in its structure) faces the model's north, which a block item shows on
    # the viewer's right in the slot (minecraft:block/block's gui rotation, 30 and 225 degrees).
    def to_model(x, z):
        return W - 1 - x, D - 1 - z

    def turn(facing):
        return OPPOSITE.get(facing, facing)

    full = {}
    for (x, y, z), (name, props) in cells.items():
        paint = _fill_kind(name, props, rating)
        if paint is not None:
            mx, mz = to_model(x, z)
            full[(mx, y, mz)] = paint
    elements = []

    def box(x0, y0, z0, x1, y1, z1, texture, skip=()):
        frm = (ox + x0 * s, y0 * s, oz + z0 * s)
        to = (ox + x1 * s, y1 * s, oz + z1 * s)
        elements.append(MW.element(frm, to, texture, skip=skip))

    # Full cubes, merged along x where their paint matches (the checker band stays block by block, so its squares
    # read); a face against another full cube is left out.
    done = set()
    for (x, y, z) in sorted(full, key=lambda k: (k[1], k[2], k[0])):
        if (x, y, z) in done:
            continue
        paint = full[(x, y, z)]
        x1 = x
        if paint != "st_band":
            while (x1 + 1, y, z) in full and full[(x1 + 1, y, z)] == paint and (x1 + 1, y, z) not in done:
                x1 += 1
        run = [(xx, y, z) for xx in range(x, x1 + 1)]
        done.update(run)
        skip = set()
        if (x - 1, y, z) in full:
            skip.add("west")
        if (x1 + 1, y, z) in full:
            skip.add("east")
        for face, (dy, dz) in (("up", (1, 0)), ("down", (-1, 0)), ("south", (0, 1)), ("north", (0, -1))):
            if all((xx, y + dy, z + dz) in full for xx in range(x, x1 + 1)):
                skip.add(face)
        if y == 0:
            skip.add("down")
        box(x, y, z, x1 + 1, y + 1, z + 1, paint + "!", skip=tuple(skip))

    # The fittings, each a small box or two in its colours, against whatever it stands on or hangs from.
    for (x, y, z), (name, props) in cells.items():
        mx, mz = to_model(x, z)
        facing = turn(props.get("facing", "north"))
        if name in ("storage_tank_plate", "storage_tank_hatch", "smooth_stone"):
            continue
        # The side of the cell towards what it is fixed to (a wall-mounted fitting's back, opposite its facing).
        bx, bz = STEP[OPPOSITE[facing]]

        def against(depth, y0=0.0, y1=1.0, width=1.0):
            """A box of the cell set back against its wall: depth into the cell from the wall side, width across."""
            w0, w1 = 0.5 - width / 2, 0.5 + width / 2
            if bx:
                xs = (1 - depth, 1) if bx > 0 else (0, depth)
                return (mx + xs[0], y + y0, mz + w0, mx + xs[1], y + y1, mz + w1)
            zs = (1 - depth, 1) if bz > 0 else (0, depth)
            return (mx + w0, y + y0, mz + zs[0], mx + w1, y + y1, mz + zs[1])

        def centred(width, y0, y1):
            a, b = 0.5 - width / 2, 0.5 + width / 2
            return (mx + a, y + y0, mz + a, mx + b, y + y1, mz + b)

        if name == "smooth_stone_slab":
            box(mx, y, mz, mx + 1, y + 0.5, mz + 1, STONE + "!", skip=("down",))
        elif name in ("railing", "railing_gate"):
            # The top rail: a thin yellow line from the cell's middle towards each railing beside it, so the rail runs
            # round the roof's edge unbroken, on a post where the railing has one. The pieces only touch (a middle
            # square, arms from its sides, the post under it), so no two faces lie on each other.
            if name == "railing_gate":
                joined = ["north", "south"] if props.get("facing") in ("east", "west") else ["east", "west"]
            else:
                joined = [d for d in ("north", "south", "east", "west") if props.get(d) == "true"]
            a, b, r0, r1 = 0.42, 0.58, 0.62, 0.76
            box(mx + a, y + r0, mz + a, mx + b, y + r1, mz + b, YELLOW)
            for d in joined:
                dx, dz = STEP[turn(d)]
                if dx > 0:
                    box(mx + b, y + r0, mz + a, mx + 1, y + r1, mz + b, YELLOW)
                elif dx < 0:
                    box(mx, y + r0, mz + a, mx + a, y + r1, mz + b, YELLOW)
                elif dz > 0:
                    box(mx + a, y + r0, mz + b, mx + b, y + r1, mz + 1, YELLOW)
                else:
                    box(mx + a, y + r0, mz, mx + b, y + r1, mz + a, YELLOW)
            if props.get("post", "true") == "true" or not joined:
                box(mx + a, y, mz + a, mx + b, y + r0, mz + b, YELLOW)
        elif name == "caged_ladder":
            box(*against(0.45, width=0.8), {"*": GALV, "up": GALV})
        elif name in ("storage_tank_outlet", "storage_tank_inlet"):
            box(*against(0.9, 0.0, 0.7, 0.6), GUN)
            box(*centred(0.5, 0.7, 0.85), RED)
        elif name == "storage_tank_fill_pipe":
            box(*against(0.4, width=0.35), PIPE)
        elif name == "storage_tank_manway":
            box(*against(0.15, 0.1, 0.9, 0.8), WHITE)
        elif name in ("storage_tank_vent", "storage_tank_pv_vent", "storage_tank_scrubber_vent", "tank_relief_valves"):
            colour = {"storage_tank_vent": PIPE, "storage_tank_pv_vent": RED, "storage_tank_scrubber_vent": BLUE,
                      "tank_relief_valves": BRASS}[name]
            box(*centred(0.55, 0.0, 0.8), colour)
        elif name == "tank_foam_chamber":
            box(*against(0.6, 0.2, 0.9, 0.7), RED)
        elif name == "tank_foam_line":
            box(*against(0.35, width=0.3), RED)
        elif name == "tank_earthing_boss":
            box(*against(0.3, 0.0, 0.45, 0.35), COPPER)
        elif name == "cryogenic_vaporizer":
            box(*centred(0.75, 0.0, 1.0), FIN)
        elif name == "tank_leg":
            box(*centred(0.45, 0.0, 1.0), GALV)
        else:
            raise ValueError(f"tank kit {kit}: no miniature for {name}")
    names = sorted({f["texture"][1:] for e in elements for f in e["faces"].values()})
    textures = {n: rid(f"block/{n}") for n in names if n != STONE}
    if STONE in names:
        textures[STONE] = "minecraft:block/smooth_stone"
    textures["particle"] = rid("block/st_wall")
    return {"parent": "minecraft:block/block", "textures": textures, "elements": elements}


def write_models(write, rid, models_item):
    for kit in ST.KITS:
        write(models_item / f"tank_kit_{kit}.json", item_model(kit, rid))
