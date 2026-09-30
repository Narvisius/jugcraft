"""Models for item logistics blocks (style-independent: the same brass look in both machine styles).

Written facing north (the working face is z = 0); blockstates turn them to all six directions.
"""
from steampunk_models import BRASS, BRASS_PLATE, COPPER, IRON, IRON_PLATE, box, cyl, dial, wheel


def pneumatic_extractor(frame=BRASS, bell=COPPER, bands=BRASS, gauge=False):
    """A suction bell on the front, iron manifold body, tube collars on the other five faces.
    The high-pressure version is dark iron with brass bands and a pressure gauge."""
    m = [box((1, 1, 0), (15, 15, 1.25), {"*": frame, "north": "sp_grate"})]
    m += cyl("z", 8, 8, 5.5, 1.25, 3.5, bell, BRASS_PLATE)
    m += cyl("z", 8, 8, 4, 3.5, 12.5, IRON_PLATE)
    for z in (5.5, 10):
        m += cyl("z", 8, 8, 4.35, z, z + 0.75, bands)
    # Collars where tubes attach (back, left, right, top, bottom).
    m.append(box((5, 5, 12.5), (11, 11, 16), frame))
    m.append(box((0, 5, 5), (4.25, 11, 11), frame))
    m.append(box((11.75, 5, 5), (16, 11, 11), frame))
    m.append(box((5, 11.75, 5), (11, 16, 11), frame))
    m.append(box((5, 0, 5), (11, 4.25, 11), frame))
    if gauge:
        m.append(box((11.75, 11, 7.25), (13.25, 13, 8.75), BRASS))
        m.append(dial("up", (12.5, 13.75, 8), 2.5, depth=0.75))
    return m


def item_sorter():
    """Riveted brass valve body with a glass inspection window, a red selector wheel and a copper outlet."""
    m = [box((2, 2, 2), (14, 14, 14), {"*": BRASS_PLATE, "up": "sp_glass"})]
    m.append(box((4, 4, 0), (12, 12, 2), COPPER))
    m.append(box((3, 3, 0), (13, 13, 0.75), {"*": BRASS, "north": "sp_hopper_inside"}))
    m += wheel("x", 10.5, 8, 2.5, 14, 14.75, "sp_red_iron", BRASS)
    m.append(dial("south", (8, 11, 14.5), 2.5))
    # Collars where tubes attach (back, left, right, top, bottom).
    m.append(box((5, 5, 14), (11, 9.5, 16), BRASS))
    m.append(box((0, 5, 5), (2, 11, 11), BRASS))
    m.append(box((14.75, 5, 5), (16, 11, 8.5), BRASS))
    m.append(box((5, 14, 5), (11, 16, 11), BRASS))
    m.append(box((5, 0, 5), (11, 2, 11), BRASS))
    return m


MODELS = {"pneumatic_extractor": pneumatic_extractor(), "item_sorter": item_sorter(),
          "high_pressure_extractor": pneumatic_extractor(frame=IRON, bell=IRON_PLATE, bands=BRASS, gauge=True)}
# Blockstate rotation for each facing, for models whose working face is north.
FACING_ROTATION = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
                   "up": {"x": 270}, "down": {"x": 90}}
