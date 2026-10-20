"""Supported electrical cabinets, paired tanks and stacks using the established metal palette."""
from steampunk_models import box, cyl

def lead_bank():
    m = [box((0,0,0),(16,2,16),'el_frame'), box((1,2,2),(15,13,14),'el_casing')]
    for x in (2,9):
        m += [box((x,3,2),(x+5,12,13),'el_cell'), box((x,12,2),(x+5,13,13),'el_frame')]
        for z in (4,10):
            m.append(box((x+2,13,z),(x+3,14,z+1),'el_frame'))
    # Insulated bus bars physically join the four terminals under a guarded lid.
    for z in (4,10): m.append(box((4,13,z),(12,14,z+1),'el_frame'))
    m += [box((1,14,1),(15,15,15),'el_vent'),box((5,3,0),(11,11,2),'el_seams')]
    return m

def tank_pair():
    m=[box((0,0,0),(16,2,16),'el_frame')]
    for x in (1,9):
        m += [box((x,2,3),(x+6,14,13),'el_casing'),box((x,13,3),(x+6,15,13),'el_frame'),
              box((x+2,4,2.8),(x+4,12,3),'el_glow_blue')]
    # Full-depth paired manifolds meet neighboring modules and the controller.
    for x in (4,11):
        m += [box((x,6,0),(x+1,8,16),'el_frame'),box((x-1,5,0),(x+2,9,1),'el_seams'),
              box((x-1,5,15),(x+2,9,16),'el_seams')]
    return m

def stack_module():
    m=[box((1,0,1),(15,2,15),'el_frame'),box((1,2,1),(3,14,15),'el_frame'),box((13,2,1),(15,14,15),'el_frame')]
    for y in (3,5,7,9,11):
        m += [box((3,y,2),(13,y+1,14),'el_seams')]
    m += [box((1,14,1),(15,16,15),'el_casing'),box((6,0,6),(10,4,10),'el_frame')]
    return m

MODELS={'lead_acid_battery_bank':lead_bank(),'flow_tank_module':tank_pair(),'flow_stack_module':stack_module()}
