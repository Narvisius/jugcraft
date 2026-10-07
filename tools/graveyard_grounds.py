"""Model geometry for the graveyard pack's grounds (pack 4, tools/graveyard.py): the kerbed grave, the planted grave,
the memorial bench and the open grave (headstone styles, cut into blocks by graveyard_data.py like the others), and
the grave vase and the cemetery lamp post (blocks of their own). In pixels, facing north (the front toward -z).

Texture variables, besides the headstones': #chippings (white marble chippings), #bed (a planted bed of flowers, gone
to weeds as it is neglected), #pit (an open grave's darkness), #soil (fresh earth), #oak (weathering oak), #petals and
#leaves (a vase's flowers), #lantern (a lamp's lit glass).
"""
import math

from graveyard_models import b, edge_box, octagon, rot
from graveyard_buildings import ring, lit, LAMP_GLOW


def kerbs(tex, top_tex, height, z1=31.5):
    """The kerb round a grave, 1 m wide and 2 m long: two side kerbs, a foot and a head kerb, and a post at each
    corner with a little pyramid cap."""
    out = [b((1.0, 0, 0.5), (3.0, height, z1), tex), b((13.0, 0, 0.5), (15.0, height, z1), tex),
           b((3.0, 0, 0.5), (13.0, height, 2.5), tex), b((3.0, 0, z1 - 2.0), (13.0, height, z1), tex)]
    out += [b((1.3, height, 0.8), (2.7, height + 0.4, z1 - 0.3), top_tex), b((13.3, height, 0.8), (14.7, height + 0.4, z1 - 0.3), top_tex)]
    for x0 in (0.6, 12.6):
        for z0 in (0.1, z1 - 2.4):
            out += [b((x0, 0, z0), (x0 + 2.8, height + 1.0, z0 + 2.8), tex),
                    b((x0 + 0.4, height + 1.0, z0 + 0.4), (x0 + 2.4, height + 1.5, z0 + 2.4), top_tex),
                    b((x0 + 0.9, height + 1.5, z0 + 0.9), (x0 + 1.9, height + 1.9, z0 + 1.9), top_tex)]
    return out


def kerbed_grave():
    """A grave kerbed in polished granite and filled with white marble chippings, an open book of white marble at its
    head on a little lectern, the epitaph cut across its pages. Two blocks long, a block wide, to be set before a
    headstone or alone."""
    out = kerbs("#relief", "#relief_top", 3.0)
    out.append(b((3.0, 0, 2.5), (13.0, 2.2, 29.5), "#chippings", top="#chippings"))
    # The lectern and the book open on it.
    out += [b((5.0, 2.2, 24.6), (11.0, 3.4, 28.6), "#stone"), b((4.4, 3.4, 24.0), (11.6, 3.8, 29.2), "#stone")]
    out += [b((3.4, 3.8, 23.4), (12.6, 4.2, 29.8), "#relief_top"), b((3.7, 4.2, 23.7), (12.3, 4.8, 29.5), "#relief_top"),
            b((7.8, 4.8, 23.7), (8.2, 5.0, 29.5), "#relief_top")]
    return out


def planted_grave():
    """A grave kerbed in sandstone, its bed planted with flowers kept by whoever tends it: neat when kept, weeds
    coming through when it is let go, then grass and brambles. A little polished plaque at its head carries the
    epitaph."""
    out = kerbs("#stone", "#top", 2.6)
    out.append(b((3.0, 0, 2.5), (13.0, 1.8, 29.5), "#bed", top="#bed"))
    # Tufts of whatever grows in the bed, drawn with the bed's own texture: flowers when kept, weeds when not.
    for x, z, h in ((4.2, 5.0, 2.6), (8.6, 6.4, 3.2), (11.0, 9.2, 2.4), (5.4, 12.0, 3.0), (9.8, 14.6, 2.8), (4.6, 18.2, 2.4),
                    (10.6, 19.6, 3.4), (7.0, 22.0, 2.6)):
        out += [b((x, 1.8, z), (x + 1.4, 1.8 + h, z + 0.4), "#bed", top="#bed"), b((x + 0.5, 1.8, z - 0.5), (x + 0.9, 1.8 + h, z + 0.9), "#bed", top="#bed")]
    out += [b((5.0, 1.8, 25.0), (11.0, 2.6, 29.2), "#relief"), b((4.6, 1.8, 24.6), (11.4, 2.2, 29.6), "#stone")]
    return out


def memorial_bench():
    """A park bench in memory of someone: oak slats on cast-iron ends with scrolled arms, a bronze plaque on its top
    rail carrying the inscription. Two blocks wide (1.9 m), the seat at 0.46 m. Designed 32 pixels wide."""
    out = []
    for x0 in (1.2, 29.4):
        x1 = x0 + 1.4
        out += [b((x0, 0, 2.6), (x1, 6.8, 3.8), "#iron"), b((x0, 0, 10.4), (x1, 15.2, 11.6), "#iron"),
                b((x0 - 0.2, 0, 2.0), (x1 + 0.2, 0.6, 4.4), "#iron"), b((x0 - 0.2, 0, 9.8), (x1 + 0.2, 0.6, 12.2), "#iron"),
                b((x0, 6.0, 3.0), (x1, 6.8, 11.0), "#iron"), b((x0 - 0.3, 10.2, 2.4), (x1 + 0.3, 11.0, 11.0), "#iron")]
        # The arm's front post and its scroll.
        out += [b((x0, 6.8, 2.8), (x1, 10.2, 3.8), "#iron")]
        out += [b((x0, 8.0, 2.0), (x1, 8.6, 2.8), "#iron"), b((x0, 7.2, 1.6), (x1, 8.6, 2.2), "#iron"),
                b((x0, 4.2, 4.8), (x1, 5.0, 9.6), "#iron"), b((x0, 1.4, 6.8), (x1, 4.2, 7.6), "#iron")]
    # Seat slats and back slats in oak.
    for z0 in (3.0, 5.0, 7.0, 9.0):
        out.append(b((0.4, 6.8, z0), (31.6, 7.6, z0 + 1.6), "#oak"))
    for y0 in (8.6, 10.4):
        out.append(b((0.4, y0, 10.0), (31.6, y0 + 1.3, 10.8), "#oak"))
    out += [b((0.4, 12.2, 9.8), (31.6, 15.4, 10.8), "#oak"), b((0.2, 15.4, 9.6), (31.8, 16.0, 11.0), "#oak")]
    # The bronze plaque on the top rail, framed.
    out += [b((9.0, 12.5, 9.4), (23.0, 15.1, 9.8), "#bronze"), b((8.6, 12.3, 9.5), (23.4, 12.5, 9.8), "#bronze"),
            b((8.6, 15.1, 9.5), (23.4, 15.3, 9.8), "#bronze")]
    return out


def open_grave():
    """A grave freshly dug: the cut in the turf going down into darkness, two boards across it and the lowering straps
    laid over them, the spoil heaped along one side with the spade stuck in it, and a wooden cross waiting at its head
    with a little board for the name. Two blocks long."""
    from sculpt import Sculpture, Ellipsoid
    out = [b((6.0, 0, 2.0), (15.4, 0.15, 30.0), "#soil", faces=("up",)), b((6.8, 0, 2.8), (14.6, 0.25, 29.2), "#pit", top="#pit")]
    # Boards across the hole, and the canvas straps.
    for z0 in (8.0, 20.0):
        out += [b((5.6, 0.25, z0), (15.8, 0.95, z0 + 2.2), "#oak"), b((4.6, 0.25, z0 + 0.7), (16.0, 1.15, z0 + 1.5), "#straps")]
    # The spoil heap along the right side.
    s = Sculpture(0.5)
    s.add(Ellipsoid((3.0, 0.0, 16.0), (3.2, 3.6, 13.0), "#soil"), Ellipsoid((3.2, 0.0, 9.0), (2.8, 4.4, 5.0), "#soil"),
          Ellipsoid((2.8, 0.0, 22.0), (2.6, 3.8, 5.0), "#soil"))
    out += above_ground(s.boxes())
    # The spade stuck in the heap: blade buried, the ash handle leaning out with its D grip.
    out += [b((2.6, 2.4, 12.2), (3.8, 5.0, 12.6), "#iron"), b((3.0, 5.0, 12.2), (3.4, 15.0, 12.6), "#oak"),
            b((2.4, 15.0, 12.2), (4.0, 15.4, 12.6), "#oak"), b((2.4, 15.4, 12.2), (2.8, 16.8, 12.6), "#oak"),
            b((3.6, 15.4, 12.2), (4.0, 16.8, 12.6), "#oak"), b((2.4, 16.8, 12.2), (4.0, 17.2, 12.6), "#oak")]
    # The cross at the head, with its board.
    out += [b((10.0, 0, 30.0), (11.4, 13.0, 31.2), "#oak"), b((7.4, 8.8, 30.0), (14.0, 10.0, 31.2), "#oak"),
            b((8.2, 5.2, 29.6), (13.2, 8.2, 30.0), "#oak")]
    return out


def above_ground(elements):
    """`elements` cut off at the ground (y = 0): a heap's sculpted boxes reach below it."""
    out = []
    for e in elements:
        if e["to"][1] <= 0:
            continue
        if e["from"][1] < 0:
            e = {**e, "from": [e["from"][0], 0.0, e["from"][2]], "faces": {k: v for k, v in e["faces"].items() if k != "down"}}
        out.append(e)
    return out


# ---------------------------------------------------------------- the grave vase (its own block)

VASE_COLOURS = ("white", "red", "yellow", "purple", "mixed")


def grave_vase(colour=None, wilted=False):
    """A cemetery vase in bronze on a granite block: a fluted bowl on a stem and foot, a rolled lip. With flowers, a
    bouquet stands in it (sculpted); wilted, the heads hang and brown."""
    out = [b((5.0, 0, 5.0), (11.0, 1.6, 11.0), "#rough"), b((5.4, 1.6, 5.4), (10.6, 2.0, 10.6), "#stone")]
    out += octagon(6.2, 2.0, 6.2, 9.8, 2.8, 9.8, "#bronze") + octagon(7.1, 2.8, 7.1, 8.9, 4.0, 8.9, "#bronze")
    out += octagon(6.4, 4.0, 6.4, 9.6, 4.6, 9.6, "#bronze") + octagon(5.8, 4.6, 5.8, 10.2, 8.4, 10.2, "#bronze")
    out += octagon(5.5, 8.4, 5.5, 10.5, 9.0, 10.5, "#bronze")
    if colour is None:
        out += octagon(6.3, 8.8, 6.3, 9.7, 9.0, 9.7, "#pit")
        return out
    from sculpt import Sculpture, Ellipsoid, Limb
    s = Sculpture(0.5)
    heads = ((8.0, 14.4, 8.0), (6.4, 13.2, 7.0), (9.6, 13.4, 7.4), (7.2, 13.0, 9.6), (9.2, 12.6, 9.4), (8.2, 12.2, 6.0))
    for hx, hy, hz in heads:
        if wilted:
            # The stem bends over and the head hangs.
            tip = (8.0 + (hx - 8.0) * 1.5, hy - 1.6, 8.0 + (hz - 8.0) * 1.5)
            s.add(Limb((8.0, 8.6, 8.0), (hx, hy - 1.0, hz), 0.25, 0.2, "#leaves"), Limb((hx, hy - 1.0, hz), tip, 0.2, 0.2, "#leaves"),
                  Ellipsoid((tip[0], tip[1] - 0.5, tip[2]), (0.7, 0.9, 0.7), "#petals"))
        else:
            s.add(Limb((8.0, 8.6, 8.0), (hx, hy - 0.8, hz), 0.25, 0.2, "#leaves"), Ellipsoid((hx, hy, hz), (1.0, 0.8, 1.0), "#petals"))
    # Leaves about the rim.
    for dx, dz in ((-2.0, 0.0), (2.0, 0.4), (0.2, -2.0), (-0.4, 2.0)):
        s.add(Limb((8.0, 8.8, 8.0), (8.0 + dx, 9.6 if not wilted else 8.8, 8.0 + dz), 0.45, 0.2, "#leaves", flat=((0, 1, 0), 0.4)))
    return out + s.boxes()


# ---------------------------------------------------------------- the cemetery lamp post (its own block)

def lamp_post(lit_glass=True):
    """A Victorian cast-iron lamp post, three blocks tall (2.9 m): a fluted base, a slender column with a ladder bar
    for the lamplighter, and a four-sided lantern under a domed cap with a finial, lit at night. Designed 48 pixels
    tall; graveyard_data.py cuts it into its three blocks."""
    out = []
    out += octagon(4.0, 0, 4.0, 12.0, 1.2, 12.0, "#iron") + octagon(4.8, 1.2, 4.8, 11.2, 6.0, 11.2, "#iron")
    out += octagon(5.4, 6.0, 5.4, 10.6, 7.0, 10.6, "#iron") + octagon(6.2, 7.0, 6.2, 9.8, 8.4, 9.8, "#iron")
    # Flutes on the base.
    for dx, dz in ((-3.3, 0), (3.3, 0), (0, -3.3), (0, 3.3)):
        out.append(b((8 + dx - 0.35, 1.6, 8 + dz - 0.35), (8 + dx + 0.35, 5.6, 8 + dz + 0.35), "#iron"))
    out += octagon(6.9, 8.4, 6.9, 9.1, 34.0, 9.1, "#iron")
    # Collars, and the ladder bar.
    for y in (12.0, 22.0, 30.4):
        out += octagon(6.5, y, 6.5, 9.5, y + 0.8, 9.5, "#iron")
    out += [b((2.0, 31.0, 7.6), (14.0, 31.8, 8.4), "#iron"), b((1.6, 31.0, 7.4), (2.4, 32.4, 8.6), "#iron"), b((13.6, 31.0, 7.4), (14.4, 32.4, 8.6), "#iron")]
    # The lantern: a bowl, four panes between corner posts, a domed cap and a finial.
    out += octagon(6.0, 34.0, 6.0, 10.0, 35.2, 10.0, "#iron") + octagon(5.0, 35.2, 5.0, 11.0, 36.0, 11.0, "#iron")
    glass = b((5.4, 36.0, 5.4), (10.6, 42.0, 10.6), "#lantern")
    out.append(lit(glass, LAMP_GLOW) if lit_glass else glass)
    for sx in (5.0, 10.2):
        for sz in (5.0, 10.2):
            out.append(b((sx, 36.0, sz), (sx + 0.8, 42.0, sz + 0.8), "#iron"))
    out += octagon(4.6, 42.0, 4.6, 11.4, 42.8, 11.4, "#iron") + octagon(5.6, 42.8, 5.6, 10.4, 44.0, 10.4, "#iron")
    out += octagon(6.6, 44.0, 6.6, 9.4, 45.0, 9.4, "#iron") + octagon(7.4, 45.0, 7.4, 8.6, 46.6, 8.6, "#iron")
    out.append(b((7.7, 46.6, 7.7), (8.3, 47.6, 8.3), "#iron"))
    return out
