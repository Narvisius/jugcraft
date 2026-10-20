"""Supported wet bench, enclosed drive and connected service manifold; shared silver/olive materials."""
from starter_chemistry_models import skid, cabinet, drawer, coupling, STEEL, OLIVE, CHROME, SILVER, RUBBER
from steampunk_models import box, cyl
from biorefinery_models import pipe_run

def bench():
    m=skid(3,3)
    # Full cabinet supports both the wash tray and hood. No floating fixtures.
    m += [box((-28,4,8),(11,21,43),OLIVE), box((-29,21,7),(12,23,44),SILVER),
          box((-27,23,10),(10,24,41),RUBBER)]
    for x in (-28,9):
        m += [box((x,23,9),(x+2,42,42),STEEL)]
    m += [box((-28,40,9),(11,43,42),OLIVE),box((-26,24,40),(9,40,42),SILVER)]
    # Wash basin with a drain; carriage rails are mechanically joined to both drawers.
    m += [box((-17,23,16),(7,25,38),SILVER),box((-17,25,16),(-15,29,38),STEEL),
          box((5,25,16),(7,29,38),STEEL),box((-17,25,16),(7,29,18),STEEL),
          box((-17,25,36),(7,29,38),STEEL)]
    for x in (-26,-20): m.append(box((x,23,5),(x+1,25,43),CHROME))
    m.append(box((-26,25,22),(-19,27,32),SILVER))
    m += drawer(-29,-19,20,28,1,7) + drawer(-29,-19,20,28,41,47)
    m += cabinet(3,4,1,12,18,8)
    # Rear reagent and rinse sockets route to the wash head inside the hood.
    pipe_run(m,[(8,24,47.5),(8,24,44),(8,37,44),(8,37,30),(2,37,30),(2,32,30)])
    pipe_run(m,[(-8,24,47.5),(-8,24,44),(-8,35,44),(-8,35,22),(-8,31,22)])
    # The low waste outlet is connected to the basin's enclosed pump and sump.
    pipe_run(m,[(8,8,47.5),(8,8,34),(-4,8,34),(-4,16,34)])
    m.append(box((-10,10,27),(1,18,39),STEEL))
    m += cyl('x',14,31,3,-15,-9,CHROME)
    m += cyl('x',14,31,4,-23,-17,OLIVE,STEEL)
    m.append(box((-24,4,25),(-16,10,38),STEEL))
    return m

MODELS={'wet_processing_bench':bench()}
ROTORS={'wet_bench_pump':dict(block='wet_processing_bench',axis='x',center=[-16,14,31],
    property='lit',speed=8,ease=20,always=True,quads=coupling('x',-18,-14,14,31,3))}
ITEM_EXTRAS={'wet_processing_bench':cyl('x',14,31,3,-18,-14,SILVER,STEEL)}
