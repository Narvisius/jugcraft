"""Models for plain storage blocks (style-independent), built from the steampunk helpers."""
from steampunk_models import BRASS, BRASS_PLATE, IRON, box


def item_crate():
    """A plank crate with iron corner posts and bands, and a brass label plate on each side."""
    m = [box((0.5, 0.5, 0.5), (15.5, 15.5, 15.5), "sp_wood")]
    for x in (0, 14):
        for z in (0, 14):
            m.append(box((x, 0, z), (x + 2, 16, z + 2), IRON))
    for y in (0, 14):
        m.append(box((2, y, 0), (14, y + 2, 0.75), IRON))
        m.append(box((2, y, 15.25), (14, y + 2, 16), IRON))
        m.append(box((0, y, 2), (0.75, y + 2, 14), IRON))
        m.append(box((15.25, y, 2), (16, y + 2, 14), IRON))
    # Brass label plates, standing proud of the planks.
    m.append(box((5, 6, 0.25), (11, 10, 0.5), {"*": BRASS, "north": BRASS_PLATE}))
    m.append(box((5, 6, 15.5), (11, 10, 15.75), {"*": BRASS, "south": BRASS_PLATE}))
    m.append(box((0.25, 6, 5), (0.5, 10, 11), {"*": BRASS, "west": BRASS_PLATE}))
    m.append(box((15.5, 6, 5), (15.75, 10, 11), {"*": BRASS, "east": BRASS_PLATE}))
    return m


MODELS = {"item_crate": item_crate()}
