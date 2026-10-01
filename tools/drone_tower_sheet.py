"""Design sheet for the Drone Tower proposal (build/drone_tower/drone_tower_design.png)."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import drone_tower as dt

F = "/usr/share/fonts/truetype/dejavu/"
BIG = ImageFont.truetype(F + "DejaVuSans-Bold.ttf", 34)
MID = ImageFont.truetype(F + "DejaVuSans-Bold.ttf", 20)
SM = ImageFont.truetype(F + "DejaVuSans.ttf", 16)
MONO = ImageFont.truetype(F + "DejaVuSansMono-Bold.ttf", 15)
BG, CY, AM, WH, DIM = (16, 12, 14), (220, 80, 64), (255, 192, 74), (236, 230, 230), (160, 130, 130)
MATS = [["Chiseled stone", "stone bricks, iron"], ["Reinforced concrete", "steel girders"], ["Steel armour plate", "blast glass"],
        ["Aluminium cladding"], ["Tungsten-steel frame"], ["Carbon composite"], ["Silicon carbide armour"],
        ["Depleted-uranium armour"], ["Graphene lattice", "YBCO conduit"]]
NEW = [("reinforced_concrete", "Reinforced Concrete", "concrete + steel rebar"), ("steel_girder", "Steel Girder (pillar)", "steel"),
       ("hazard_plating", "Hazard Plating", "steel + dyes"), ("steel_armor", "Steel Armour Plate", "steel"),
       ("blast_glass", "Blast Glass", "laminated glass + silicon"), ("hangar_door", "Hangar Bay Door", "steel + motor"),
       ("aluminum_cladding", "Aluminium Cladding", "aluminium"), ("tungsten_frame", "Tungsten-Steel Frame", "tungsten + steel"),
       ("carbon_composite", "Carbon Composite Panel", "pitch fibre (bitumen)"), ("sic_armor", "Silicon Carbide Armour", "silicon + coke"),
       ("du_armor", "Depleted-Uranium Armour", "uranium tailings"), ("graphene_lattice", "Graphene Lattice", "coke → graphene"),
       ("sc_conduit", "Superconducting Conduit", "YBCO (rare earths)"), ("light_cyan", "Red Light Strip (dim)", "redstone + glass"),
       ("light_red", "Warning Light", "redstone lamp")]


def main():
    renders = []
    for t in range(1, 10):
        r = dt.iso(dt.build(t), scale=2)
        renders.append(r.crop(r.getbbox()))
    cw = max(220, max(r.width for r in renders) + 12)
    rh = max(r.height for r in renders)
    W = cw * 9 + 40
    sheet = Image.new("RGB", (W, rh + 900), BG)
    d = ImageDraw.Draw(sheet)
    d.text((20, 16), "DRONE TOWER — 9-TIER DESIGN PROPOSAL (v9: core + four sloping buttresses)", font=BIG, fill=CY)
    d.text((20, 58), "100 drones, each in its own hangar sized to fit it (8 tier-1 drones on the ground pads). Tier 2 is a wide hangar deck over the pads and "
                     "command post, on legs you walk between. One continuous hangar core, four thick sloping buttresses, a slender spire; pickups on the buttresses.", font=SM, fill=DIM)
    top = 90
    for i, r in enumerate(renders):
        x = 20 + i * cw
        sheet.paste(r, (x + (cw - r.width) // 2, top + rh - r.height), r)
        y = top + rh + 8
        d.text((x + 8, y), f"TIER {i + 1}", font=MID, fill=CY)
        d.text((x + 8, y + 26), dt.TIER_NAMES[i], font=SM, fill=WH)
        d.text((x + 8, y + 46), f"{dt.TIER_TOPS[i + 1]} blocks tall", font=SM, fill=DIM)
        for j, m in enumerate(MATS[i]):
            d.text((x + 8, y + 68 + j * 20), m, font=SM, fill=AM)
        d.text((x + 8, y + 112), f"Unlocks drone T{i + 1}", font=SM, fill=WH)
        n = dt.FLOOR_CAPACITY[i + 1][0]
        total = sum(dt.FLOOR_CAPACITY[t][0] for t in range(1, i + 2))
        d.text((x + 8, y + 132), f"+{n} drones ({total} total)", font=SM, fill=WH)
    y0 = top + rh + 180
    d.text((20, y0), "TIER 1 FIELD (top view)", font=MID, fill=CY)
    sheet.paste(dt.plan().resize((324, 324), Image.NEAREST), (20, y0 + 34))
    for j, line in enumerate(["39x39 field, 8 pads, 2 per side", "supply pickup + packager (SE)", "command post in the middle"]):
        d.text((20, y0 + 366 + j * 20), line, font=SM, fill=DIM)
    sx = 400
    d.text((sx, y0), "TOWER STATUS SCREEN (mock-up)", font=MID, fill=CY)
    d.rectangle([sx, y0 + 34, sx + 520, y0 + 600], fill=(22, 14, 16), outline=(80, 34, 30), width=3)
    el = dt.elevation(3, scale=2)
    el = el.crop((0, el.height - 70 * 2, el.width, el.height)).resize((int(el.width * 2.2), int(140 * 2.2)), Image.NEAREST)
    sheet.paste(el, (sx + 20, y0 + 60))
    tx = sx + 196
    d.text((tx, y0 + 60), "▌DRONE TOWER", font=MID, fill=CY)
    d.text((tx, y0 + 92), "TIER 3 · ARMOURED BLOCK", font=MONO, fill=WH)
    d.text((tx, y0 + 116), "Drones up to T3 unlocked", font=MONO, fill=DIM)
    d.text((tx, y0 + 150), "NEXT: TIER 4 LOGISTICS SPIRE", font=MONO, fill=AM)
    for j, (m, n, h) in enumerate([("Aluminium Cladding", 1840, 1210), ("Blast Glass", 520, 520), ("Steel Girder", 260, 40),
                                   ("Hazard Plating", 180, 180)]):
        yy = y0 + 178 + j * 24
        d.text((tx, yy), m, font=MONO, fill=WH)
        d.text((tx + 196, yy), f"{h}/{n}", font=MONO, fill=CY if h >= n else AM)
    d.rectangle([tx, y0 + 290, tx + 300, y0 + 300], fill=(30, 14, 14))
    d.rectangle([tx, y0 + 290, tx + 190, y0 + 300], fill=CY)
    d.text((tx, y0 + 306), "BUILD 63% · 9 drones on it", font=MONO, fill=DIM)
    d.rectangle([tx, y0 + 350, tx + 300, y0 + 392], fill=(70, 30, 26), outline=CY, width=2)
    d.text((tx + 22, y0 + 361), "UPGRADE DRONE TOWER", font=MID, fill=WH)
    for j, line in enumerate(["Drones pull the materials from", "the storage network and build", "the next floors, bottom up."]):
        d.text((tx, y0 + 404 + j * 20), line, font=SM, fill=DIM)
    d.text((sx + 20, y0 + 440), "Solid = built,", font=SM, fill=DIM)
    d.text((sx + 20, y0 + 460), "red ghost = next tier", font=SM, fill=DIM)
    mx = 960
    d.text((mx, y0), "NEW BUILDING MATERIALS (also for reactors, the black hole facility…)", font=MID, fill=CY)
    for j, (k, n, src) in enumerate(NEW):
        xx = mx + (j // 8) * 540
        yy = y0 + 40 + (j % 8) * 52
        d.rectangle([xx, yy, xx + 40, yy + 40], fill=dt.COLORS[k], outline=(60, 70, 80))
        d.text((xx + 52, yy + 2), n, font=MID, fill=WH)
        d.text((xx + 52, yy + 24), src, font=SM, fill=DIM)
    out = Path(__file__).resolve().parent.parent / "build" / "drone_tower" / "drone_tower_design.png"
    sheet.crop((0, 0, W, y0 + 620)).save(out)


if __name__ == "__main__":
    main()
