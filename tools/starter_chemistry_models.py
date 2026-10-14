"""Native block geometry for the four starter chemistry machines.

Clean existing Jugcraft steel/ceramic/olive surfaces, one texel per model pixel; closed fittings, real doors,
flanges, rails and isolated moving couplings. No opaque cubes pretending to be the entire machine.
"""
import math
from steampunk_models import box, cyl, dial
from dieselpunk_models import GUNMETAL as STEEL, OLIVE, CHROME, RUBBER, HAZARD, GAUGE, GRILLE
import machine_rotors as rotors

# Muted 16px silver-grey steel for vessel cladding, shared by every process line.
# Keep pale ceramic/polymer item materials separate from the metal machine shells.
SILVER = "dw_steel_plate"
CERAMIC = "el_white"
COPPER = "sp_copper"
LIGHT = "dr_amber"
RIBS = "dw_ribbed_steel"
TANK = "tk_body"

def skid(width, depth):
    left = 16 - width * 16
    end = depth * 16
    m = [box((left + .5, 0, .5), (15.5, 2.5, end-.5), STEEL)]
    for x in (left+2, 10):
        m.append(box((x, 2.5, 2), (x+4, 5, end-2), RIBS))
    return m

def flange(axis, cu, cv, a0, a1, radius=2.5, material=COPPER):
    m = cyl(axis, cu, cv, radius, a0, a1, material)
    m += cyl(axis, cu, cv, radius+1, a0, a0+1, CHROME)
    m += cyl(axis, cu, cv, radius+1, a1-1, a1, CHROME)
    return m

def cabinet(x, y, z, w=10, h=18, d=9):
    m = [box((x,y,z), (x+w,y+h,z+d), OLIVE),
         box((x+1,y+1,z-.5), (x+w-1,y+h-1,z), STEEL)]
    m.append(dial("north", (x+w/2, y+h-5, z-.75), 4, .5, GAUGE, CHROME))
    m.append(dial("north", (x+3, y+4, z-.75), 2, .5, LIGHT, STEEL))
    m.append(box((x+w-2,y+6,z-1), (x+w-1,y+10,z-.5), CHROME))
    return m

def drawer(x0, x1, y0, y1, z0, z1):
    m = [box((x0,y0,z0), (x1,y1,z1), STEEL),
         box((x0+1,y0+1,z0-.5), (x1-1,y1-1,z0), OLIVE)]
    cy = (y0+y1)/2
    for x in (x0+3, x1-4):
        m.append(box((x,cy-1,z0-1.5), (x+1,cy+1,z0-.5), CHROME))
    m.append(box((x0+3,cy-.5,z0-2), (x1-3,cy+.5,z0-1.5), CHROME))
    return m

def fan(axis, a0, a1, cu, cv, radius, blades=5):
    m = rotors.disc(axis, a0-.25, a1+.25, cu, cv, 1.25, CHROME, CHROME)
    for i in range(blades):
        ang = i*2*math.pi/blades
        def p(r, w):
            return (cu+r*math.cos(ang)-w*math.sin(ang), cv+r*math.sin(ang)+w*math.cos(ang))
        m += rotors.prism(axis, a0, a1, [p(1,-.75),p(radius,-1.25),p(radius,1.25),p(1,.75)], STEEL, CHROME)
    return m

def coupling(axis, a0, a1, cu, cv, radius):
    m = rotors.disc(axis, a0, a1, cu, cv, 1.5, STEEL, STEEL)
    for start in ((a0+1,) if a1-a0 < 6 else (a0+1, a1-2)):
        m += rotors.disc(axis, start, start+.75, cu, cv, radius, CHROME, "dr_flange")
        for angle in (0, 90, 180, 270):
            u, v = cu+radius*.66*math.cos(math.radians(angle)), cv+radius*.66*math.sin(math.radians(angle))
            m += rotors.disc(axis, start+.8, start+1.25, u, v, .5, STEEL, STEEL, sides=4)
    return m

def chemical_infuser():
    m = skid(2,3)
    # Vertical reaction vessel, insulated waist and banded shoulders.
    m += cyl("y", 0, 25, 10, 10, 39, STEEL)
    m += cyl("y", 0, 25, 10.5, 16, 33, SILVER)
    for y in (10,15,33,38):
        m += cyl("y", 0,25,11, y,y+1.5,CHROME)
    m += cyl("y", 0,25,8, 39,43,OLIVE)
    m += cyl("y", 0,25,4, 43,45,COPPER)
    # Two independent shoulder inlet pipes, on the actual upper front port blocks.
    for x in (-8,8):
        m += flange("z", x,40, .5,18,2)
        m += cyl("y",x,18,2,33,40,COPPER)
    # Ringed front inspection cover, with solid centre and a restrained process band.
    m += cyl("z",0,26,8,10,15,CHROME)
    m += cyl("z",0,26,6.5,9,10,STEEL)
    m.append(box((-4.5,22,8.75),(4.5,24,9),LIGHT))
    m.append(dial("north",(0,29,8.5),4, .5,GAUGE,CHROME))
    m += drawer(-8,8,5,12,3,17)
    m += cabinet(6,5,2,8,11,9)
    # Closed cooling loop behind the body, connected at both ends.
    for x in (-12,12):
        m += cyl("y",x,40,1.5,13,39,COPPER)
    m += cyl("x",39,40,1.5,-12,12,COPPER)
    m += cyl("x",13,40,1.5,-12,12,COPPER)
    m += flange("z",8,24,34,47.5,2)
    m += flange("z",-8,8,32,47.5,2)
    m += cyl("y",-8,32,2,8,15,COPPER)
    # Rear fan visible between its supports, clear of the cooling loop.
    for x in (-7.5,6.5):
        m.append(box((x,23,44),(x+1,38,46),STEEL))
    for y in (23,37):
        m.append(box((-6.5,y,44),(6.5,y+1,46),STEEL))
    return m

def chemical_oxidizer():
    m = skid(2,2)
    m += drawer(-11,11,5,12,3,27)
    m += cyl("y",0,17,10,12,30,STEEL)
    m += cyl("y",0,17,10.5,15,26,SILVER)
    for y in (12,27):
        m += cyl("y",0,17,11,y,y+2,CHROME)
    # Closed rear charging hopper with a dark recessed throat, broad rim, and guarded drive.
    m.append(box((1,30,18),(12,38,29),OLIVE))
    m.append(box((0,38,17),(13,41,30),STEEL))
    m.append(box((1,41,18),(12,43,29),RUBBER))
    for x in (0,12):
        m.append(box((x,41,17),(x+1,45,31),OLIVE))
    for z in (17,30):
        m.append(box((1,41,z),(12,45,z+1),OLIVE))
    m.append(box((2,45,17),(11,46,18),HAZARD))
    # Collector with a capped back outlet.
    m += cyl("y",-8,22,4,30,43,OLIVE)
    m += cyl("y",-8,22,5,42,44,CHROME)
    m += flange("z",-8,40,24,31.5,2)
    # Bolted inspection hatch and slit.
    m += cyl("z",0,21,7,4,8,CHROME)
    m += cyl("z",0,21,5.5,3,4,STEEL)
    m.append(box((-3.5,19,2.75),(3.5,21,3),LIGHT))
    for x in (-5,4):
        m.append(box((x,24,2.75),(x+1,25,3.25),CHROME))
    m += flange("z",-8,8,.5,15,2)
    m += cabinet(5,5,2,9,10,8)
    # Front feed coupling: an open guard with moving hardware behind it.
    for x in (-6,5):
        m.append(box((x,31,2),(x+1,39,6),STEEL))
    m.append(box((-5,38,2),(5,39,6),STEEL))
    m += cyl("z",0,35,2,6,17,STEEL)
    return m

def coal_gasifier():
    m = skid(4,4)
    # Braced feet and broad, sealed ash chamber.
    for x in (-42,-9):
        for z in (15,51):
            m.append(box((x,5,z),(x+5,16,z+5),STEEL))
            m.append(box((x-1,4,z-1),(x+6,6,z+6),CHROME))
    m += cyl("y",-23,36,20,12,27,STEEL)
    m += cyl("y",-23,36,21,12,14,CHROME)
    m += drawer(-37,-9,15,25,12,24)
    # Tall refractory column; structural bands interrupt the ceramic shell instead of noisy wear.
    m += cyl("y",-23,36,15,27,80,SILVER)
    for y in (27,43,59,75):
        m += cyl("y",-23,36,15.5,y,y+2.5,STEEL)
        for x in (-38,-10):
            m.append(box((x,y+2.5,32),(x+1,y+15,40),CHROME))
    m += cyl("y",-23,36,13,80,86,OLIVE)
    m += cyl("y",-23,36,8,86,90,STEEL)
    m += cyl("y",-23,36,9,90,92,CHROME)
    # Thick lower service door, latch and protected heat slit.
    m += cyl("z",-23,34,8,17,22,CHROME)
    m += cyl("z",-23,34,6.5,16,17,STEEL)
    m.append(box((-26,31,15.75),(-20,33,16),LIGHT))
    m.append(box((-18,32,15),(-17,38,16),CHROME))
    # Capped process header: no open chimney and no plume of hidden pollution.
    m += cyl("x",88,36,3,-40,-23,STEEL)
    m += flange("z",-40,88,35,63.5,3)
    # Stepped charging chute at the left; its descending roof reads a sealed, sloped feed path.
    for x0,y0,z0,x1,y1,z1 in [(4,48,14,15,61,33),(-2,44,20,6,57,35),(-10,40,26,0,52,38)]:
        m.append(box((x0,y0,z0),(x1,y1,z1),OLIVE))
        m.append(box((x0,y1,z0),(x1,y1+1,z1),CHROME))
    m.append(box((14.5,51,19),(15.5,58,28),RUBBER))
    m += cabinet(2,5,2,12,23,11)
    m += flange("z",8,8,45,63.5,2)
    m += cyl("y",8,45,2,8,32,COPPER)
    m += cyl("x",32,45,2,-8,8,COPPER)
    # Exposed drive coupler forward of the chute, surrounded by a fixed guard; centre x=7,y=42,z=14.
    for z in (8,19):
        m.append(box((1,36,z),(14,48,z+1),STEEL))
    m.append(box((1,47,9),(14,48,19),STEEL))
    # Upper service steps and rails, on the right of the column, within its declared envelope.
    for y in range(18,69,8):
        m.append(box((-46,y,28),(-42,y+1.5,44),CHROME))
    for z in (27,44):
        m.append(box((-45,14,z),(-43,76,z+1),STEEL))
    return m

def gas_cleaning_rack():
    m = skid(3,5)
    # Rear scrubber on a broad collection pot.
    m += cyl("y",-17,61,12,6,20,STEEL)
    m += cyl("y",-17,61,12.5,19,21,CHROME)
    m += cyl("y",-17,61,9,21,51,TANK)
    for y in (23,35,47):
        m += cyl("y",-17,61,9.5,y,y+1.5,CHROME)
    m += cyl("y",-17,61,7,51,57,OLIVE)
    m += cyl("y",-17,61,5,57,60,STEEL)
    m += flange("z",-24,56,59,79.5,2)
    m += cyl("x",56,61,2,-24,-17,STEEL)
    # Short separator at the left, with a closed service top.
    m += cyl("y",7,40,6,7,38,OLIVE)
    m += cyl("y",7,40,6.5,12,14,CHROME)
    m += cyl("y",7,40,6.5,34,36,CHROME)
    m += cyl("y",7,40,4,38,42,STEEL)
    # Two unmistakable gas manifolds on the front upper row; water low on the left.
    for x in (8,-8):
        m += flange("z",x,40,.5,20,2)
        m += cyl("y",x,20,2,27,40,COPPER)
    m += cyl("x",27,20,2,-17,8,COPPER)
    m += cyl("z",-17,27,2,20,51,COPPER)
    m += flange("x",8,8,6,15.5,2)
    m += cyl("z",6,8,2,8,34,COPPER)
    # Collection drawer, controls and a service bridge linking the vessels.
    m += drawer(-15,0,5,13,3,25)
    m += cabinet(-29,5,2,12,22,12)
    m.append(box((-26,42,39),(14,44,47),STEEL))
    for x in (-27,13):
        m.append(box((x,5,43),(x+1,53,44),CHROME))
    m.append(box((-27,52,43),(14,53,44),CHROME))
    m.append(box((-27,48,43),(14,49,44),STEEL))
    # Pump with a visible guarded coupling at the front right, separate from its motor and volute.
    m += cyl("z",-22,30,5,17,26,OLIVE)
    m += cyl("z",-22,30,6,25,29,STEEL)
    for x in (-29,-16):
        m.append(box((x,24,7),(x+1,37,17),STEEL))
    m.append(box((-28,36,7),(-16,37,17),STEEL))
    m += cyl("z",-22,30,2,29,49,COPPER)
    return m

MODELS = {"chemical_infuser": chemical_infuser(), "chemical_oxidizer": chemical_oxidizer(),
          "coal_gasifier": coal_gasifier(), "gas_cleaning_rack": gas_cleaning_rack()}
ROTORS = {
    "infuser_cooling_fan": dict(block="chemical_infuser", axis="z", center=[0,30,45], property="lit", speed=14, ease=25, always=True, quads=fan("z",44.5,45.5,0,30,5)),
    "oxidizer_feed_drive": dict(block="chemical_oxidizer", axis="z", center=[0,35,4], property="lit", speed=7, ease=15, always=True, quads=coupling("z",2.75,6.25,0,35,3)),
    "gasifier_feed_drive": dict(block="coal_gasifier", axis="x", center=[7,42,14], property="lit", speed=5, ease=30, always=True, quads=coupling("x",2,13,42,14,3.5)),
    "cleaning_pump_drive": dict(block="gas_cleaning_rack", axis="z", center=[-22,30,12], property="lit", speed=10, ease=25, always=True, quads=coupling("z",8,17,-22,30,4)),
}
ITEM_EXTRAS = {
    "chemical_infuser": cyl("z",0,30,5,44.5,45.5,CHROME,"dr_fan"),
    "chemical_oxidizer": cyl("z",0,35,3,2.75,6.25,CHROME,"dr_flange"),
    "coal_gasifier": cyl("x",42,14,3.5,2,13,CHROME,"dr_flange"),
    "gas_cleaning_rack": cyl("z",-22,30,4,8,17,CHROME,"dr_flange"),
}
