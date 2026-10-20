"""Ceramic item icons use the repo's editable 16x16 maps and shared material palettes.

The maps, not an image-generation service or an atlas, are the source of truth.
"""
import ceramics
import item_icons


def draw_all(save):
    for name in ceramics.ITEMS:
        save(item_icons.draw(name), 'item', name)
