"""JSON resources for the leaf blower (fall addition 30), from tools/leaf_blower.py: its 3D item model, built from the
powered tools' dieselpunk boxes and textures (tools/tool_models.py: olive paint, gunmetal, chrome, hazard stripes,
bakelite grips), and its words. The advancement and recipe data come through HALLOWEEN_ADVANCEMENTS and SHAPED in
tools/agriculture.py.

Modelled upright as the other powered tools are (the business end at the top, +y), and turned into the hand by their
display transforms (tool_models.HANDHELD): a ribbed bakelite pistol grip and chrome trigger under a round olive motor
housing with a hazard band and a stencilled top, a louvred intake on its side, a gunmetal power cell with a charge gauge
at its back, and a long chrome blower tube, stepped down from a gunmetal collar to a rubber-lipped nozzle.
"""
from decor_data import MOD, rid
import model_writer
from steampunk_models import box, cyl, dial
from tool_models import OLIVE, STENCIL, GUNMETAL, CHROME, HAZARD, RUBBER, RIBBED, BAKELITE, GRILLE, HANDHELD
from leaf_blower import LEAF_BLOWER


def model():
    m = [box((6, 0, 7.5), (10, 1, 11), RUBBER),
         box((6.5, 1, 8), (9.5, 6.5, 10.5), {"*": BAKELITE, "north": RIBBED, "south": RIBBED}),
         box((7.5, 4.5, 6.75), (8.5, 6, 8), CHROME),
         box((7.25, 6, 5.5), (8.75, 6.5, 8), GUNMETAL)]
    # The motor housing: round, olive, a hazard band round its front and the louvred intake on its side.
    m += cyl("y", 8, 8, 4.25, 6.5, 13.5, OLIVE, STENCIL)
    m += cyl("y", 8, 8, 4.4, 12, 13, HAZARD)
    m.append(box((3.25, 7.5, 6), (4, 12, 10), GRILLE))
    m.append(box((12, 7.5, 6), (12.75, 12, 10), GRILLE))
    # The power cell at its back, with its charge gauge.
    m.append(box((5.5, 7, 12), (10.5, 12.5, 14.5), GUNMETAL))
    m.append(dial("south", (8, 9.75, 14.75), 3.5, texture="dp_gauge", body=CHROME))
    # The blower tube: a gunmetal collar, the long chrome tube, and a rubber-lipped nozzle.
    m += cyl("y", 8, 8, 2.6, 13.5, 15.5, GUNMETAL, CHROME)
    m += cyl("y", 8, 8, 1.8, 15.5, 26.5, CHROME)
    m += cyl("y", 8, 8, 2.2, 26.5, 28, RUBBER, GUNMETAL)
    return m


def assets(root, write, lang):
    item = LEAF_BLOWER["item"]
    elements = model()
    textures = {name: rid(f"block/{name}") for name in model_writer.texture_names(elements)}
    textures["particle"] = rid("block/dp_olive")
    write(root / "models" / "item" / f"{item}.json", {
        "textures": textures, "elements": model_writer.slice_model(item, elements, [(0, 0, 0)])[0], "display": HANDHELD})
    write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    lang[f"item.{MOD}.{item}"] = LEAF_BLOWER["display"]
    lang[f"item.{MOD}.leaf_blower.tooltip"] = "Hold use to blow; sneak to vacuum up leaves"
    lang[f"message.{MOD}.leaf_blower.flat"] = "The Leaf Blower needs charging at a Charging Station"
