"""Bodies drawn over a pipe's core for the fluid valve and fluid filter (batch 8). The pipe arms come from the
ordinary pipe models (generate_material_data); these sit at the centre, the same in every pipe direction, and are
added to the pipe's multipart block state, chosen by its block state where they differ.
"""
from steampunk_models import box, dial, wheel

GUNMETAL, CHROME = "dp_gunmetal", "dp_chrome"


def valve(lamp):
    """A gunmetal valve body round the pipe's core, a bonnet and stem with a red handwheel on top, and a lamp on the
    front: green while open, amber while closed."""
    m = [box((4.5, 4.5, 4.5), (11.5, 11.5, 11.5), GUNMETAL)]
    m.append(box((5.5, 11.5, 5.5), (10.5, 12.5, 10.5), CHROME))
    m.append(box((7.25, 12.5, 7.25), (8.75, 14.5, 8.75), CHROME))
    m += wheel("y", 8, 8, 3.25, 14.5, 15.25, "sp_red_iron", CHROME)
    m.append(dial("north", (8, 8, 4.25), 1.25, texture=lamp, body=CHROME))
    return m


def strainer(lamp):
    """A filter housing: a chrome canister round the pipe's core with gunmetal end caps and a mesh window, and a lamp
    on top: lit when a fluid is set, dark when not."""
    m = [box((4.75, 4.75, 4.75), (11.25, 11.25, 11.25), CHROME)]
    for z0 in (4.25, 11.25):
        m.append(box((5.25, 5.25, z0), (10.75, 10.75, z0 + 0.5), GUNMETAL))
    for x0, face in ((4.5, "west"), (11.25, "east")):
        m.append(box((x0, 6, 6), (x0 + 0.25, 10, 10), {"*": GUNMETAL, face: "dp_grille"}))
    m.append(dial("up", (8, 11.5, 8), 1.25, texture=lamp, body=GUNMETAL))
    return m


MODELS = {"fluid_valve_open": valve("el_lamp_on"), "fluid_valve_closed": valve("dp_lamp_on"),
          "fluid_filter_set": strainer("el_lamp_on"), "fluid_filter_unset": strainer("el_lamp")}
# Per pipe block: (body model, multipart "when" or None for always) for each body.
BODIES = {"fluid_valve": [("fluid_valve_open", {"powered": "false"}), ("fluid_valve_closed", {"powered": "true"})],
          "fluid_filter": [("fluid_filter_set", {"set": "true"}), ("fluid_filter_unset", {"set": "false"})]}
# The body drawn in the item model.
ITEM_BODY = {"fluid_valve": "fluid_valve_open", "fluid_filter": "fluid_filter_unset"}
