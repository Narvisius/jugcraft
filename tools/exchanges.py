"""The Drone Tower's two exchanges, on opposite edges of the landing field (approved 1 Oct 2026).

Built with tier 1 (the Command Post). Energy Exchange (west edge): a bulky substation. Two transformer bodies with cooling fins and insulator stacks,
a busbar gantry overhead and a heavy armoured conduit running in under the field to the command post. Its
Power Port faces outward: any cable touching it powers the whole depot.

Storage Exchange (east edge): a loading dock. A clad warehouse block with a big bay door, two intake hoppers on
the roof and a conveyor ramp up to a Cargo Port facing outward: pipes, conveyors and hoppers touching it feed the
depot's cargo store (every supply pickup draws from it).
"""
import drone_tower as d

C = d.C
EXTRA_COLORS = {
    "girder_x": (84, 88, 96), "girder_z": (84, 88, 96), "conduit_x": (52, 56, 64), "busbar_x": (180, 110, 60),
    "power_port": (200, 54, 40), "cargo_port": (226, 170, 40), "transformer": (58, 62, 70), "fin": (40, 42, 48),
    "insulator": (210, 205, 196), "busbar": (180, 110, 60), "conduit": (52, 56, 64), "dock_floor": (92, 92, 88),
    "hopper": (64, 66, 70), "conveyor": (40, 40, 44), "bay_door": (60, 66, 76),
}
d.COLORS.update(EXTRA_COLORS)
d.GLOWING.add("power_port")


def put(b, x, y, z, m):
    b[(x, y, z)] = m


def energy_exchange(b):
    """West edge, centred on the field's middle row: x -3..5, z C-5..C+5."""
    x0, x1, z0, z1 = -3, 5, C - 5, C + 5
    # Heavy concrete pad with a hazard border.
    # Outside the field only: the landing platform itself stays whole (the depot reads its pads from it).
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if 0 <= x < d.SIZE:
                continue
            edge = x == x0 or z in (z0, z1)
            put(b, x, 0, z, "hazard_plating" if edge else "reinforced_concrete")
    # Two transformers (3x3x4) with cooling fins on their outer faces.
    for tz in (z0 + 1, z1 - 3):
        for x in range(-1, 2):
            for z in range(tz, tz + 3):
                for y in range(1, 5):
                    put(b, x, y, z, "transformer")
        for y in range(1, 4):
            for z in range(tz, tz + 3):
                put(b, -2, y, z, "fin")
        # Insulator stacks on top, glowing caps.
        for z in (tz, tz + 2):
            put(b, 0, 5, z, "insulator")
            put(b, 0, 6, z, "insulator")
            put(b, 0, 7, z, "light_red")
    # The centre: the main bus. Armoured conduit comes in from the Power Port to a transformer block between
    # the two transformers, with a copper busbar on top and an insulated riser up to the gantry.
    for x in range(-2, 3):
        put(b, x, 1, C, "conduit_x")
    for x in range(-1, 2):
        for z in (C - 1, C + 1):
            for y in (1, 2):
                put(b, x, y, z, "transformer")
        put(b, x, 2, C, "transformer")
        for z in range(C - 1, C + 2):
            put(b, x, 3, z, "busbar_x" if z == C else "hazard_plating")
    for y in range(4, 8):
        put(b, 0, y, C, "insulator")
    put(b, -2, 2, C, "conduit_x")
    put(b, -2, 1, C - 1, "fin")
    put(b, -2, 1, C + 1, "fin")
    put(b, 2, 2, C, "conduit_x")
    # Busbar gantry over the whole yard.
    for z in range(z0 + 1, z1):
        put(b, 0, 8, z, "busbar")  # copper busbar running along z
    for z in (z0 + 1, z1 - 1):
        for y in range(1, 8):
            put(b, 3, y, z, "steel_girder")
        for x in range(0, 4):
            put(b, x, 8, z, "girder_x")
    # Switchgear cabinet and the armoured conduit running in under the field.
    for z in range(C - 1, C + 2):
        for y in range(1, 4):
            put(b, 3, y, z, "steel_armor")
        put(b, 3, 4, z, "hazard_plating")
    # Laid on the field floor towards the command post (the tier 2 deck's leg later stands through it at x 8).
    for x in range(4, 12):
        put(b, x, 1, C, "conduit_x")
        put(b, x, 1, C - 1, "conduit_x")
    # The Power Port on the outer face (cables connect here), framed in hazard plating.
    put(b, x0, 2, C, "power_port")
    for y in (1, 3):
        put(b, x0, y, C, "hazard_plating")
    put(b, x0, 2, C - 1, "hazard_plating")
    put(b, x0, 2, C + 1, "hazard_plating")
    for z in (C - 2, C + 2):
        put(b, x0, 4, z, "light_red")


def storage_exchange(b):
    """East edge: x 33..41, z C-5..C+5 (mirror position of the energy yard)."""
    x0, x1, z0, z1 = 33, 41, C - 5, C + 5
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not 0 <= x < d.SIZE:
                put(b, x, 0, z, "dock_floor")
    # The warehouse: aluminium-clad box with a steel frame, 7 wide, 5 high.
    for x in range(34, 40):
        for z in range(z0 + 1, z1):
            for y in range(1, 6):
                wall = x in (34, 39) or z in (z0 + 1, z1 - 1) or y == 5
                if not wall:
                    continue
                corner = (x in (34, 39)) and (z in (z0 + 1, z1 - 1))
                put(b, x, y, z, "steel_girder" if corner or y == 5 and (x + z) % 3 == 0 else "aluminum_cladding")
    # Solid inside (a sealed store, so nothing can spawn in it): bulk storage cladding all through.
    for x in range(35, 39):
        for z in range(z0 + 2, z1 - 1):
            for y in range(1, 5):
                put(b, x, y, z, "aluminum_cladding")
    # A big bay door facing the field (west side) where the drones' cargo crates go in.
    for z in range(C - 2, C + 3):
        for y in range(1, 4):
            put(b, 34, y, z, "bay_door")
    for z in range(C - 3, C + 4):
        put(b, 34, 4, z, "hazard_plating")
    # Two intake hoppers on the roof.
    for hz in (C - 3, C + 2):
        for x in (36, 37):
            for z in (hz, hz + 1):
                put(b, x, 6, z, "hopper")
                put(b, x, 7, z, "hopper")
        put(b, 36, 8, hz, "hazard_plating")
        put(b, 37, 8, hz + 1, "hazard_plating")
    # A conveyor ramp from the outer edge up to the Cargo Port.
    for i, x in enumerate(range(41, 39, -1)):
        put(b, x, 1 + i // 2, C, "conveyor")
    put(b, 40, 1, C - 1, "steel_girder")
    put(b, 40, 1, C + 1, "steel_girder")
    put(b, 39, 2, C, "cargo_port")
    for z in (C - 1, C + 1):
        put(b, 39, 2, z, "hazard_plating")
        put(b, 39, 3, z, "hazard_plating")
    put(b, 39, 3, C, "hazard_plating")
    # Amber beacons on the corners.
    for z in (z0 + 1, z1 - 1):
        put(b, 39, 6, z, "hazard_plating")


def build(b):
    """Adds both exchanges (called by drone_tower.build for tier 1)."""
    energy_exchange(b)
    storage_exchange(b)


def blocks():
    d.PREVIEW = True
    return d.build(1)


if __name__ == "__main__":
    import sys
    from PIL import Image
    out = sys.argv[1]
    b = blocks()
    full = d.iso(b, scale=6)
    full = full.crop(full.getbbox())
    # Keep only the lower part (tier 1 is low).
    w, h = full.size
    full.save(f"{out}/exchanges_overview.png")
    # Close-ups: each exchange alone, from the side that shows its port.
    energy = {}
    energy_exchange(energy)
    storage = {}
    storage_exchange(storage)
    for name, bl, back in (("energy", energy, True), ("storage", storage, False)):
        img = d.iso(bl, scale=14, back=back)
        img.crop(img.getbbox()).save(f"{out}/exchange_{name}.png")
