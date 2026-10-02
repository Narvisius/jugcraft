"""Lighting for the Drone Tower: enough block light on every floor, roof, ledge and step that no hostile mob
can spawn there (mobs need block light 0), laid out as neat flush floor lights.

For each tier, works out the block light everywhere (like the game: a source spreads its level minus one per
block through air and see-through blocks) and finds the "spawnable" surfaces: a full solid block with two
blocks of air over it and no light there. Each dark surface gets a Red Light Strip set flush into the floor,
preferring the nearest spot on a regular 6-block grid (centred on the core) so the lights line up; only where
the grid has no floor in reach does it use a 3-block grid, and only as a last resort the dark spot itself.
Lights of earlier tiers stay while their floor block is still there.
"""

# Light each material gives off (the mod's blocks: JugcraftTower and JugcraftDrones).
EMIT = {"light_cyan": 8, "light_red": 13, "ceiling_light_panel": 15, "sc_conduit": 5, "console_desk": 4,
        "equipment_rack": 2, "hangar_pad": 6, "landing_platform": 4, "power_port": 7, "cargo_port": 7}
# Blocks light passes through (glass, furniture, the thin pad plates and machines).
SEE_THROUGH = {"blast_glass", "tech_window", "landing_pad", "supply_pickup", "cargo_packager", "holo_table",
               "ceiling_light_panel", "cable_tray", "operator_chair", "console_desk", "equipment_rack",
               "control_screen", "drone_depot_terminal", "drone", "cargo_chute"}
# Floors a light may be set into (plain building blocks; never the platform the depot reads, nor machines).
FLOORS = {"stone_bricks", "reinforced_concrete", "steel_armor", "hazard_plating", "chiseled_stone", "hangar_floor",
          "access_floor_tile", "carpet_tile", "aluminum_cladding", "tungsten_frame", "carbon_composite", "sic_armor",
          "du_armor", "graphene_lattice", "dock_floor", "iron", "steel_girder", "acoustic_wall_panel", "transformer",
          "hangar_door", "concrete_column", "girder_x", "girder_z"}
FIXTURE = "light_cyan"
REACH = 6
NEIGHBOURS = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))


def base(material):
    return material.split("[")[0]


def opaque(blocks, pos):
    m = blocks.get(pos)
    return m is not None and base(m) not in SEE_THROUGH


def spread(blocks, light, pos, level):
    """Adds a source of {@code level} at {@code pos} to the light map (keeping the brighter of old and new)."""
    if light.get(pos, 0) >= level:
        return
    light[pos] = level
    queue = [(pos, level)]
    while queue:
        nxt = []
        for (x, y, z), lv in queue:
            if lv <= 1:
                continue
            for dx, dy, dz in NEIGHBOURS:
                p = (x + dx, y + dy, z + dz)
                if light.get(p, 0) >= lv - 1 or opaque(blocks, p):
                    continue
                light[p] = lv - 1
                nxt.append((p, lv - 1))
        queue = nxt


def surfaces(blocks):
    """Spawnable spots: solid full blocks with two air blocks above them."""
    out = []
    for (x, y, z), m in blocks.items():
        b = base(m)
        if b in SEE_THROUGH or b in EMIT:
            continue
        if (x, y + 1, z) in blocks or (x, y + 2, z) in blocks:
            continue
        out.append((x, y, z))
    return out


def plan(blocks, centre, lights=None):
    """The lights to add to {@code blocks} (pos -> material it replaces), given lights already kept."""
    light = {}
    for pos, m in blocks.items():
        level = EMIT.get(base(m))
        if level:
            spread(blocks, light, pos, level)
    spots = surfaces(blocks)
    by_column = {}
    for s in spots:
        by_column.setdefault((s[0], s[2]), []).append(s[1])
    floor = {s for s in spots if base(blocks[s]) in FLOORS}
    added = {}

    def grid(pos, step):
        return (pos[0] - centre) % step == 0 and (pos[2] - centre) % step == 0

    def reaches(source, target):
        """Would a light at {@code source} put any light into {@code target}?"""
        probe = {}
        spread(blocks, probe, source, EMIT[FIXTURE])
        return probe.get(target, 0) > 0

    def candidates(dark, step):
        x, y, z = dark
        found = []
        for dx in range(-REACH, REACH + 1):
            for dz in range(-(REACH - abs(dx)), REACH - abs(dx) + 1):
                for cy in by_column.get((x + dx, z + dz), ()):
                    c = (x + dx, cy, z + dz)
                    if abs(cy - y) > 1 or c not in floor or not grid(c, step):
                        continue
                    found.append((abs(dx) + abs(dz) + abs(cy - y), c))
        above = (x, y + 1, z)
        for _, c in sorted(found):
            if reaches(c, above):
                return c
        return None

    for dark in sorted(spots, key=lambda p: (p[1], p[0], p[2])):
        if light.get((dark[0], dark[1] + 1, dark[2]), 0) > 0:
            continue
        spot = candidates(dark, 6) or candidates(dark, 3) or candidates(dark, 1)
        if spot is None:
            continue
        added[spot] = blocks[spot]
        blocks[spot] = FIXTURE
        floor.discard(spot)
        spread(blocks, light, spot, EMIT[FIXTURE])
    return added
