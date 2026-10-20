"""One shared finishing bench, using the existing olive/silver factory material kit.

Geometry authored for Jugcraft; no loose decoration or decorative pipe ends.
"""
from starter_chemistry_models import skid, cabinet, drawer, coupling, STEEL, OLIVE, CHROME, SILVER, RUBBER
from steampunk_models import box, cyl
from biorefinery_models import pipe_run


def finisher():
    m = skid(3, 3)
    # Box-section legs and crossmembers carry the closed coolant sump and work tray.
    for x in (-28, 5):
        for z in (12, 37):
            m.append(box((x, 3, z), (x + 5, 20, z + 5), STEEL))
    m += [box((-28, 12, 12), (10, 20, 42), OLIVE),
          box((-29, 20, 11), (11, 22, 43), SILVER),
          box((-29, 22, 11), (-27, 27, 43), STEEL),
          box((9, 22, 11), (11, 27, 43), STEEL),
          box((-29, 22, 41), (11, 27, 43), STEEL)]
    # The feed/output carriage is a continuous supported route through the machine.
    m += drawer(-29, -19, 20, 28, 1, 7)
    m += drawer(-29, -19, 20, 28, 41, 47)
    m.append(box((-28, 19, 6), (-20, 23, 42), STEEL))
    for x in (-27, -22):
        m.append(box((x, 23, 7), (x + 1, 25, 42), CHROME))
    m.append(box((-27, 25, 22), (-20, 27, 32), SILVER))
    # Motor, bearing and spindle all meet the abrasive/polishing head's axis.
    m.append(box((-3, 22, 17), (9, 28, 35), OLIVE))
    m += cyl('x', 32, 26, 6, -1, 9, OLIVE, STEEL)
    m += cyl('x', 32, 26, 2, -17, -1, CHROME)
    m.append(box((-6, 26, 21), (-3, 36, 31), STEEL))
    # Guard surrounds the upper half; the lower work edge is exposed inside the tray.
    m += [box((-20, 39, 17), (-9, 41, 35), OLIVE),
          box((-20, 29, 34), (-9, 40, 36), STEEL),
          box((-11, 27, 17), (-9, 40, 35), STEEL)]
    m += cabinet(3, 4, 2, 12, 18, 10)
    # Coolant supply runs from the rear inlet into a nozzle above the cutting head.
    pipe_run(m, [(8, 24, 47.5), (8, 24, 44), (8, 42, 44), (-15, 42, 44), (-15, 42, 26), (-15, 38, 26)])
    # Sump return connects to the actual rear outlet, with no open, unused branch.
    pipe_run(m, [(8, 8, 47.5), (8, 8, 32), (8, 13, 32)])
    m.append(box((6, 8, 30), (10, 14, 34), STEEL))
    return m


MODELS = {'precision_finisher': finisher()}
ROTORS = {'precision_finisher_head': dict(
    block='precision_finisher', axis='x', center=[-16, 32, 26], property='lit',
    speed=12, ease=25, always=True, quads=coupling('x', -18, -14, 32, 26, 7))}
ITEM_EXTRAS = {'precision_finisher': cyl('x', 32, 26, 7, -18, -14, SILVER, STEEL)}
