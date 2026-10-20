"""Package 2 uses existing metal/ore maps and the component icon map renderer."""
import item_icons
from structural_alloys import ITEMS


def draw_all(save):
    for name in ITEMS:
        save(item_icons.draw(name), 'item', name)
