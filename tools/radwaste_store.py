"""The Radwaste Building (round 14, Justin: "a radioactive materials storage building... two layers of walls and an
airlock with the doors"): where each pre-made plant keeps its radioactive waste and irradiated material.

A building in a building:
- the outer building: plain concrete outside like the rest of the plant, no windows;
- inside it, the vault: shielding concrete walls 2 thick and a 2-thick shielding concrete roof under the building's
  roof, so everything stored is behind two layers of wall all the way round (and over the top);
- between the two layers, an inspection corridor round three sides of the vault, with lead-glass windows into it;
- at the front, the entry hall (the health physics checkpoint: protective clothing lockers, a bench, the sample
  counter, a step-off pad at the controlled area's edge, lead-glass windows into the vault);
- the only way into the vault is the airlock: a lead-lined door into a small shielded chamber, then a second
  lead-lined door into the vault. The doors are interlocked (one opens only while the other is shut) and close
  themselves, so the vault is never open to the hall;
- in the vault: shielded waste drums two high along the back, lead-lined lockers down one side, lead pigs on a steel
  bench down the other, the middle left clear for what the players bring.

Coordinates as in medium_plant.Building (x across, z from the back (north) to the front (south), y from the ground
floor slab). The building is grounded (its floor level with the grass).
"""
import medium_plant as MP
from medium_plant import (Building, CONCRETE, SHIELD, EPOXY, CEIL, LIGHT, LINER, HAZARD, ANDESITE, M, entrance,
                          furniture, sign, beacon)

LEAD_GLASS = M("jugcraft:lead_glass", (196, 160, 90))
MP.GLAZING.add(LEAD_GLASS)


def drum(facing="north"):
    return M(f"jugcraft:waste_drum[facing={facing}]", (226, 186, 40))


def locker(facing):
    return M(f"jugcraft:lead_locker[facing={facing},open=false]", (210, 212, 206))


def pig(facing="north"):
    return M(f"jugcraft:lead_pig[facing={facing}]", (150, 152, 156))


# The entry hall's depth (front wall to the vault's front wall).
HALL = 9


def radwaste_building(key, name, W=23, D=35, H=8):
    """The building, W x D (W odd: the airlock is on the middle line). Returns it (not yet on a site)."""
    b = Building(key, name, W, D, H, grounded=True)
    b.shell_walls(CONCRETE, step=8)
    b.storey(0, 1, 1, W - 2, D - 2, EPOXY)
    cx = W // 2
    zf = D - 1 - HALL - 2          # the vault's front wall: z zf, zf + 1
    vx0, vx1, vz0, vz1 = 5, W - 6, 5, zf - 1   # the vault's floor
    vtop = H - 2                   # the vault is walked y 1..vtop; its roof y vtop + 1 .. H

    # -- the vault: shielding concrete walls 2 thick and a roof 2 thick, inside the building's own walls and roof.
    for y in range(1, H + 1):
        b.b(3, y, 3, W - 4, y, 4, SHIELD)                  # back
        b.b(3, y, 3, 4, y, zf + 1, SHIELD)                 # left (west)
        b.b(W - 5, y, 3, W - 4, y, zf + 1, SHIELD)         # right (east)
        b.b(3, y, zf, W - 4, y, zf + 1, SHIELD)            # front
    b.b(3, vtop + 1, 3, W - 4, H, zf + 1, SHIELD)          # roof

    # -- the airlock: a 3 x 3 chamber, 3 high, walled and roofed 2 thick, standing into the vault behind its front
    # wall; a lead-lined door in the front wall (from the hall) and one in the chamber's back wall (into the vault).
    za0, za1 = zf - 3, zf - 1      # the chamber's floor, front to back z
    b.b(cx - 3, 1, za0 - 2, cx + 3, 5, zf - 1, SHIELD)
    b.c(cx - 1, 1, za0, cx + 1, 3, za1)
    # Through the front wall: the outer door in its hall-side layer, the inner layer open behind it.
    b.c(cx, 1, zf, cx, 2, zf + 1)
    b.door(cx, 1, zf + 1, "north", kind="lead", label="airlock")
    # Through the chamber's back wall: the inner door in its chamber-side layer.
    b.c(cx, 1, za0 - 2, cx, 2, za0 - 1)
    b.door(cx, 1, za0 - 1, "north", kind="lead", label="waste_vault")
    # Over each door, on both its sides, an interlock beacon (green: this door may be opened; red: the other one is
    # open, so this one stays shut). A light in the chamber.
    b.p(cx, 3, zf + 2, beacon("green", "south"))       # outer door, hall side
    b.p(cx, 3, zf - 1, beacon("green", "north"))       # outer door, chamber side
    b.p(cx, 3, za0, beacon("green", "south"))          # inner door, chamber side
    b.p(cx, 3, za0 - 3, beacon("green", "north"))      # inner door, vault side
    b.p(cx - 1, 3, za0 + 1, LIGHT)

    # -- the inspection corridor between the two layers (1..2 wide round the back and sides), its ceiling at y 5.
    for x0, x1, z0, z1 in ((1, 2, 1, zf + 1), (W - 3, W - 2, 1, zf + 1), (1, W - 2, 1, 2)):
        b.b(x0, 5, z0, x1, 5, z1, CEIL)
    # Lead-glass windows into the vault from the corridor (2 high, through both layers of the vault wall).
    for z in (vz0 + 2, vz0 + 3, vz1 - 4, vz1 - 3):
        for y in (2, 3):
            b.b(3, y, z, 4, y, z, LEAD_GLASS)
            b.b(W - 5, y, z, W - 4, y, z, LEAD_GLASS)

    # -- the entry hall: its ceiling at y 5 (the corridors' too), the front doors, the checkpoint.
    zh0, zh1 = zf + 2, D - 2
    b.b(1, 5, zh0, W - 2, 5, zh1, CEIL)
    entrance(b, cx, D - 1, "s", kind="steel")
    # Lead-glass windows into the vault either side of the airlock, 3 wide and 2 high.
    for x0 in (vx0, vx1 - 2):
        b.b(x0, 2, zf, x0 + 2, 3, zf + 1, LEAD_GLASS)
    # The step-off pad at the controlled area's edge, in front of the airlock door.
    b.b(cx - 2, 0, zh0, cx + 2, 0, zh0 + 1, HAZARD)
    # Protective clothing lockers along the front wall (west of the doors), a bench in front of them.
    for x in range(1, cx - 3):
        b.stack("locker", x, 1, zh1, "south", n=2)
    b.row(("locker_bench", cx - 5), cx - 4, 1, zh1 - 3, "south")   # (a row runs to the user's right: west here)
    # The counting bench (sample counter) along the front wall east of the doors.
    names = ["lab_bench", "lab_analyzer", "lab_bench", "lab_sink", "lab_bench"][:W - 2 - (cx + 3)]
    b.row(names, W - 2, 1, zh1, "south")
    b.furniture("fire_extinguisher", W - 2, 1, zh0 + 1, "east")
    # The health physics technician's desk, beside the airlock and facing it.
    b.desk(cx + 5, 1, zh0 + 2, "west")
    # Signs: the corridors, by their openings into the hall (on the vault's front wall, never on glass).
    for x in (3, W - 4):
        b.p(x, 3, zf + 2, sign("inspection_corridor", "south"))

    # -- the vault's contents.
    # Waste drums two high in two rows along the back wall.
    for x in range(vx0, vx1 + 1):
        for z in (vz0, vz0 + 1):
            for y in (1, 2):
                b.p(x, y, z, drum("north"))
    # Lead-lined lockers down the west side, fronts to the middle.
    for z in range(vz0 + 5, vz0 + 11):
        b.p(vx0, 1, z, locker("west"))
    # A steel bench down the east side with the lead pigs on it.
    for z in range(vz0 + 5, vz0 + 11):
        b.p(vx1, 1, z, LINER)
        b.p(vx1, 2, z, pig("east"))
    # Hazard plating round the airlock's inner door (where the vault's controlled area starts).
    b.b(cx - 1, 0, za0 - 3, cx + 1, 0, za0 - 3, HAZARD)

    # -- lights: along the corridors under their ceiling, then under the vault's roof and the hall's ceiling.
    for z in range(3, zf + 2, 4):
        b.p(1, 4, z, LIGHT)
        b.p(W - 2, 4, z, LIGHT)
    for x in range(5, W - 4, 4):
        b.p(x, 4, 1, LIGHT)
    b.light_ceilings(step=4, x0=1, z0=1, x1=W - 2, z1=D - 2)
    return b


def place(s, ox, oz, key="radwaste_building", name="Radwaste Building", W=23, D=35, sign_label="radwaste_building"):
    """Builds the radwaste building on a site with its front (south) at oz + D - 1, and hangs its name on the front."""
    b = radwaste_building(key, name, W, D)
    s.add(b, ox, oz)
    MP.site_sign(s, b, W // 2 + 4, 6, 0, "s", sign_label)
    return b
