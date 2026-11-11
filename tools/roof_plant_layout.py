"""The Tri-Pillar's roof plant laid out on the control building's roof (tower_tripillar.build calls fit() with tier 1,
the roof pads; take 1, approved by Justin 26 Oct). The pieces are tools/roof_plant.py's; walk level y 21, the slab y 20.

- Two rooftop air handlers north-west, over the upper floor (AHU-R1 facing east, AHU-R2 facing west: their doors face
  each other across a two-block service aisle); each one's supply and return ducts (hv_duct_elbow_down) turn down
  into duct roof curbs beside it.
- Two dry coolers by the west vertex, over the data hall (DC-1, DC-2, their header ends east): four insulated pipes on
  roof pipe supports from their headers into two pipe doghouses, and the walkway over the pipes on a pipe crossover.
- Six roof exhaust fans over the rooms they serve: EF-1 and EF-2 the restrooms (north-west), EF-3 the crew kitchen's
  hood (upblast, grease box) and EF-4 its general extract (south), EF-5 the showers (south-west), EF-6 the battery
  room's hydrogen extract (upblast, south-east).
- The comms mast at the stair house's west end (its base on the roof, eight sections up to the top, tied to the
  house's wall at the second and fourth), two microwave dishes on it, the RF cabinet at its foot with the cable
  bridge to it.
- The walkways: the warehouse walkway paint in the slab (as the pillar rooms), straight runs with right-angle turns: a
  ring round the pillars outside every bay's flight path, spurs to every pad's edge, the three pillar doors, the
  plant, the stair house's door; east of pillar B it runs down past the goods lift's house (work list item 20).
"""

Y = 21                      # walk level (the roof slab is y 20)
SLAB = 20

# (tag variant, master cell (x, z), facing)
AHUS = [(0, (-27, -26), "east"), (1, (-34, -19), "west")]
AHU_DUCTS = [((1, 1, -1), (0, 0, 1)), ((-1, 1, 6), (1, 0, 0))]   # the supply and return cells, and which way the unit is
COOLERS = [(0, (-42, -3), "east"), (1, (-42, 0), "east")]
PIPE_X = (-41, -40, -39)
PIPE_Z = (-3, -2, 0, 1)
SUPPORT_X = -40              # under the crossover's deck
DOGHOUSES = [(-38, -3), (-38, 0)]
CROSSOVER = (-40, -4)        # along z at x -40: its stair z -4, the deck z -3..1, the stair down z 2
# (tag number, cell (x, z), piece, facing)
FANS = [(1, (-26, -36), "roof_exhaust_fan", "south"), (2, (-23, -37), "roof_exhaust_fan", "south"),
        (3, (-2, 41), "kitchen_exhaust_fan", "north"), (4, (3, 41), "roof_exhaust_fan", "north"),
        (5, (-27, 36), "roof_exhaust_fan", "north"), (6, (27, 36), "roof_upblast_fan", "north")]
MAST = (-18, -30)            # the base at y 21, sections 22..29 (wall ties at 22 and 24), the top at 30
MAST_TIES = (Y + 1, Y + 3)
DISHES = [((-19, Y + 6, -30), "west"), ((-18, Y + 4, -31), "north")]
CABINET = ((-18, -28), "west")
# The walkways as polylines (the line down the east side runs past the goods lift's house again since layout 40: the
# landing platform it crossed is gone, the supply pickup on the house's top).
NETWORK = [
    [(-9, -29), (-9, -30), (28, -30), (28, -24), (34, -24), (34, 4), (32, 4), (32, 23), (20, 23), (20, 37), (-20, 37),
     (-20, 23), (-30, 23)],
    [(-9, -30), (-9, -31)],                                   # the north-west pad
    [(11, -30), (11, -31)],                                   # the north-east pad
    [(34, 4), (36, 4)],                                       # the east pad
    [(34, 3), (16, 3), (16, 2)],                              # pillar B's doors
    [(20, 23), (11, 23), (11, 19), (10, 19), (10, 18)],      # pillar A's doors
    [(20, 37), (26, 37)],                                     # EF-6 (battery room)
    [(-20, 37), (-26, 37)],                                   # EF-5 (showers)
    [(0, 37), (0, 40)],                                       # EF-3 and EF-4 (kitchen)
    [(-30, 23), (-32, 23), (-32, 4), (-40, 4), (-40, 3)],     # the south-west and west pads, the crossover's south foot
    [(-31, 3), (-16, 3), (-16, 2)],                           # pillar C's doors
    [(-40, -5), (-40, -13), (-37, -13), (-37, -20), (-36, -20), (-36, -22), (-24, -22), (-24, -27), (-9, -27), (-9, -29)],
    [(-34, -23), (-27, -23)],                                 # the air handlers' service aisle, its second row
    [(-19, -27), (-19, -28)],                                 # the RF cabinet's door
    [(-24, -27), (-24, -36)],                                 # EF-1 and EF-2 (restrooms)
]


def turn(dx, dz, facing):
    """A cell's offset from a piece's anchor (as it faces north) as the piece faces `facing` (tower/RoomKitBlock)."""
    return {"north": (dx, dz), "east": (-dz, dx), "south": (-dx, -dz), "west": (dz, -dx)}[facing]


def walk_cells():
    out = set()
    for line in NETWORK:
        for (x0, z0), (x1, z1) in zip(line, line[1:]):
            assert x0 == x1 or z0 == z1, ((x0, z0), (x1, z1))
            n = max(abs(x1 - x0), abs(z1 - z0))
            for k in range(n + 1):
                out.add((x0 + (x1 - x0) * k // n, z0 + (z1 - z0) * k // n) if n else (x0, z0))
    return out


def fit(T, b):
    """Adds the roof plant and the walkways to the tower's blocks ``b`` (the roof pads' step)."""
    import roof_plant as RP

    def put(x, y, z, state):
        assert (x, y, z) not in b, f"the roof plant's {state} runs into {b[(x, y, z)]} at {(x, y, z)}"
        T.put(b, x, y, z, state)

    def piece(name, origin, facing, variant=None):
        cells, tags, _ = RP.PIECES[name]
        ox, oy, oz = origin
        for i, (dx, dy, dz) in enumerate(cells):
            tx, tz = turn(dx, dz, facing)
            props = [f"facing={facing}"] + ([f"part={i}"] if len(cells) > 1 else []) + ([f"variant={variant}"] if len(tags) > 1 else [])
            put(ox + tx, oy + dy, oz + tz, f"jugcraft:{name}[{','.join(props)}]")

    for v, (mx, mz), f in AHUS:
        piece("rooftop_ahu", (mx, Y, mz), f, v)
        for (dx, dy, dz), (ux, _, uz) in AHU_DUCTS:
            tx, tz = turn(dx, dz, f)
            ox, oz = turn(ux, uz, f)
            opens = {(0, 1): "south", (0, -1): "north", (1, 0): "east", (-1, 0): "west"}[(ox, oz)]
            put(mx + tx, Y + 1, mz + tz, f"jugcraft:hv_duct_elbow_down[facing={opens}]")
            put(mx + tx, Y, mz + tz, "jugcraft:duct_roof_curb[facing=north]")
    for v, (mx, mz), f in COOLERS:
        piece("dry_cooler", (mx, Y, mz), f, v)
    for x in PIPE_X:
        for z in PIPE_Z:
            put(x, Y, z, "jugcraft:roof_pipe_support[facing=east]" if x == SUPPORT_X else "jugcraft:hv_chw_pipe[facing=east]")
    for (x, z) in DOGHOUSES:
        piece("pipe_doghouse", (x, Y, z), "north")
    # the crossover: its stairs' blocks and the row over the pipes (the pipes keep the blocks under its deck)
    piece("pipe_crossover", (CROSSOVER[0], Y, CROSSOVER[1]), "north")
    for n, (x, z), name, f in FANS:
        put(x, Y, z, f"jugcraft:{name}[facing={f},variant={n - 1}]")
    mx, mz = MAST
    put(mx, Y, mz, "jugcraft:comms_mast_base[facing=north]")
    for y in range(Y + 1, Y + 9):
        put(mx, y, mz, f"jugcraft:{'comms_mast_tie' if y in MAST_TIES else 'comms_mast'}[facing=north]")
    put(mx, Y + 9, mz, "jugcraft:comms_mast_top[facing=north]")
    for (x, y, z), f in DISHES:
        put(x, y, z, f"jugcraft:comms_dish[facing={f}]")
    (x, z), f = CABINET
    piece("rf_cabinet", (x, Y, z), f)
    # the walkways, painted into the slab (a floor light on a route goes: the step's light pass lights round them)
    walk = walk_cells()
    for (x, z) in walk:
        s = b.get((x, SLAB, z), "")
        assert s == "jugcraft:reinforced_concrete" or "inset_light" in s or "neon" in s or "amber_light" in s, \
            f"the roof's walkway at {(x, z)} would go over {s}"
        assert (x, Y, z) not in b, f"the roof's walkway at {(x, z)} runs under {b[(x, Y, z)]}"
    for (x, z) in walk:
        mask = sum(bit for bit, (dx, dz) in zip((1, 2, 4, 8), ((0, -1), (1, 0), (0, 1), (-1, 0))) if (x + dx, z + dz) not in walk)
        b[(x, SLAB, z)] = (f"jugcraft:warehouse_walkway[mask={mask},soffit=none,"
                           f"stencil={'true' if (x * 5 + z * 3) % 13 == 0 else 'false'}]")
