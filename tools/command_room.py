"""Concept of the Drone Tower's command room, v2 (proposal stage): the inside of the tier-1 Command Post.

Direction (Justin): it should look real and high-tech, like an actual operations centre, not the default
sci-fi look, and no makeshift furniture (no stairs as chairs). So every piece of furniture is its own block
with a proper model, the room is lit with neutral white light panels (red is kept for status and warning
lights), and the surfaces are matte: acoustic wall panels between concrete columns, a raised access floor,
dark carpet tiles under the work stations.

New furniture blocks (each a custom block model, rotatable, with a fitting hitbox):
- Operator Chair: five-star base with casters, gas column, padded seat, mesh back, headrest, armrests.
- Console Desk: matte desk with a rear cable channel; two monitors on stands, keyboard, mouse. Connects
  side by side into a long console.
- Command Terminal: the Drone Tower terminal as a console desk with one large display (the tower status
  screen with the UPGRADE DRONE TOWER button opens from it).
- Plotting Table (3x3): a horizontal touch display showing the map, with a faint red projection of the
  tower above it (the hologram, toned down).
- Video Wall: thin-bezel display panels that join into one wall (fleet, jobs, power, map).
- Equipment Rack (2 high): server cabinet with a perforated door and small status LEDs.
- Ceiling Light Panel, Cable Tray, Acoustic Wall Panel, Access Floor Tile, Carpet Tile, Concrete Column.

`python tools/command_room.py` writes build/drone_tower/command_room_concept.png.
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

C = {
    "stone": (104, 104, 108), "plinth": (120, 120, 122), "panel": (62, 64, 68), "panel_b": (56, 58, 62),
    "column": (128, 128, 124), "trim": (92, 96, 102), "floor": (86, 88, 92), "floor_b": (80, 82, 86),
    "carpet": (40, 42, 46), "carpet_b": (36, 38, 42),
    "black": (24, 24, 26), "fabric": (34, 34, 38), "mesh": (46, 46, 50), "chrome": (170, 172, 178),
    "desk_top": (70, 72, 76), "desk_body": (48, 50, 54), "bezel": (18, 18, 20), "screen": (26, 32, 34),
    "ui_amber": (214, 156, 60), "ui_pale": (176, 186, 186), "ui_red": (196, 62, 50), "ui_dim": (62, 80, 82),
    "keys": (36, 38, 40), "table_top": (30, 36, 38), "table_body": (60, 62, 66), "holo": (240, 90, 70),
    "rack": (28, 28, 30), "rack_door": (40, 40, 44), "led_g": (90, 200, 120), "led_a": (230, 170, 60),
    "lift_glass": (60, 70, 74), "lift_frame": (96, 100, 106),
    "door": (140, 142, 148), "hazard": (214, 170, 38), "hazard_b": (30, 30, 32), "red_light": (230, 62, 44),
    "crate": (110, 96, 70), "sign": (210, 206, 196),
}
GLOW = {"screen", "ui_amber", "ui_pale", "ui_red", "ui_dim", "holo", "led_g", "led_a", "red_light", "table_top"}
ALPHA = {"holo": 70, "lift_glass": 130}

# ---------------------------------------------------------------- furniture models (1/16 block units)
# Each model is a list of boxes (x0, y0, z0, x1, y1, z1, colour) for a block facing north: the person
# using it looks towards -z.


def chair():
    return [(2, 0, 7, 14, 1, 9, "black"), (7, 0, 2, 9, 1, 14, "black"),
            (1.5, 0, 7.5, 2.5, 0.6, 8.5, "chrome"), (13.5, 0, 7.5, 14.5, 0.6, 8.5, "chrome"),
            (7.5, 0, 1.5, 8.5, 0.6, 2.5, "chrome"), (7.5, 0, 13.5, 8.5, 0.6, 14.5, "chrome"),
            (7, 1, 7, 9, 6, 9, "chrome"),
            (3, 6, 3, 13, 7.5, 13, "black"), (3.5, 7.5, 3.5, 12.5, 8.2, 12.5, "fabric"),
            (3.5, 8, 12, 12.5, 19, 13.5, "mesh"), (3, 8, 13, 13, 19.5, 14, "black"),
            (5, 19.5, 12.5, 11, 22.5, 14, "fabric"),
            (2, 7.5, 9, 3, 11, 10, "black"), (1.5, 11, 4.5, 3.5, 11.8, 10, "fabric"),
            (13, 7.5, 9, 14, 11, 10, "black"), (12.5, 11, 4.5, 14.5, 11.8, 10, "fabric")]


def desk(big_screen=False):
    """Console desk: the user sits on its +z side and looks towards -z."""
    m = [(0, 11, 0.5, 16, 12, 15.5, "desk_top"), (0, 0, 0.5, 16, 11, 3, "desk_body"),
         (0.5, 0, 3, 1.5, 11, 15, "desk_body"), (14.5, 0, 3, 15.5, 11, 15, "desk_body"),
         (2, 12, 9, 14, 12.6, 11.5, "keys"), (12.5, 12, 12, 13.8, 12.7, 14, "black")]
    if big_screen:
        m += [(6.5, 12, 2, 9.5, 12.5, 5, "black"), (7.5, 12.5, 3, 8.5, 16, 4, "chrome"),
              (0.5, 15, 3, 15.5, 25, 4, "bezel"), (1, 15.5, 3.9, 15, 24.5, 4.1, "screen"),
              (2, 22, 4.05, 9, 23.2, 4.2, "ui_pale"), (2, 17, 4.05, 6, 21, 4.2, "ui_dim"),
              (7, 17, 4.05, 14, 19, 4.2, "ui_amber"), (9, 20, 4.05, 14, 21, 4.2, "ui_red")]
    else:
        for x0 in (1, 8.5):
            m += [(x0 + 2.5, 12, 2, x0 + 4, 12.5, 4.5, "black"), (x0 + 2.8, 12.5, 3, x0 + 3.7, 15, 3.8, "chrome"),
                  (x0, 14.5, 3, x0 + 6.5, 20.5, 3.8, "bezel"), (x0 + 0.4, 14.9, 3.75, x0 + 6.1, 20.1, 3.9, "screen"),
                  (x0 + 0.8, 18.5, 3.85, x0 + 4.5, 19.4, 4.0, "ui_pale"),
                  (x0 + 0.8, 15.5, 3.85, x0 + 5.6, 17.8, 4.0, "ui_dim" if x0 < 5 else "ui_amber")]
    return m


def rack():
    m = [(1, 0, 1, 15, 32, 15, "rack"), (1.5, 1, 0.6, 14.5, 31, 1, "rack_door")]
    for y in range(3, 30, 3):
        m += [(2.5, y, 0.4, 13.5, y + 0.5, 0.6, "black"), (12, y + 1, 0.3, 12.8, y + 1.6, 0.5, "led_g")]
    m += [(12, 28, 0.3, 12.8, 28.6, 0.5, "led_a"), (2.5, 29.5, 0.3, 6, 30.5, 0.5, "sign")]
    return m


def rotate(boxes, facing):
    """Turn a north-facing model to face north/east/south/west."""
    turns = {"north": 0, "east": 1, "south": 2, "west": 3}[facing]
    out = []
    for x0, y0, z0, x1, y1, z1, c in boxes:
        pts = [(x0, z0), (x1, z1)]
        for _ in range(turns):
            pts = [(16 - z, x) for x, z in pts]
        (ax, az), (bx, bz) = pts
        out.append((min(ax, bx), y0, min(az, bz), max(ax, bx), y1, max(az, bz), c))
    return out


# ---------------------------------------------------------------- the room

H = 10


def cross_section(r, n=4.0):
    cells = set()
    span = int(math.ceil(r)) + 1
    for dx in range(-span, span + 1):
        for dz in range(-span, span + 1):
            if abs(dx / r) ** n + abs(dz / r) ** n <= 1:
                cells.add((dx, dz))
    return cells


def room():
    """{(bx, by, bz): list of boxes in 1/16 units relative to the block}."""
    cells = {}

    def full(p, colour):
        cells[p] = [(0, 0, 0, 16, 16, 16, colour)]

    def add(p, boxes):
        cells[p] = cells.get(p, []) + boxes

    outer, inner, space = cross_section(7.45), cross_section(6.45), cross_section(5.45)
    for dx in range(-7, 8):
        for dz in range(-7, 8):
            full((dx, -1, dz), "plinth")
    for dx, dz in outer:
        work = (dx <= -3 and abs(dz) <= 2) or (dx >= 2 and 3 <= dz <= 4)
        full((dx, 0, dz), ("carpet" if (dx + dz) % 2 else "carpet_b") if work else ("floor" if (dx + dz) % 2 else "floor_b"))
        for y in range(1, H):
            if (dx, dz) not in inner:
                full((dx, y, dz), "stone")
            elif (dx, dz) not in space:
                column = (dx in (-6, 6) and dz in (-6, -3, 0, 3, 6)) or (dz in (-6, 6) and dx in (-6, -3, 0, 3, 6))
                if column:
                    full((dx, y, dz), "column")
                elif y == 6:
                    full((dx, y, dz), "trim")
                else:
                    full((dx, y, dz), "panel" if y % 2 else "panel_b")
    # Video wall on the north wall: 5 x 2 panels with thin bezels and live content.
    content = ["ui_dim", "ui_amber", "ui_pale", "ui_dim", "ui_red"]
    for i, dx in enumerate(range(-2, 3)):
        for j, y in enumerate((3, 4)):
            c = content[(i + j * 2) % 5]
            cells[(dx, y, -6)] = [(0, 0, 0, 16, 16, 16, "bezel"), (0.5, 0.5, 15.9, 15.5, 15.5, 16.1, "screen"),
                                  (2, 9, 16.1, 13, 12, 16.2, c), (2, 3, 16.1, 8, 7.5, 16.2, "ui_dim"),
                                  (9, 3, 16.1, 14, 5, 16.2, "ui_pale")]
    # Plotting table (3x3) with its map display and a faint projection of the tower above it.
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            add((dx, 1, dz), [(0, 0, 0, 16, 13, 16, "table_body"), (0, 13, 0, 16, 14, 16, "table_top"),
                              (3, 14, 3, 6, 14.1, 9, "ui_amber" if (dx + dz) % 2 else "ui_dim"),
                              (9, 14, 8, 13, 14.1, 12, "ui_pale" if dx == 0 else "ui_dim")])
    for y in range(2, 6):
        add((0, y, 0), [(6, 0, 6, 10, 16, 10, "holo")] if y < 4 else [(7, 0, 7, 9, 16, 9, "holo")])
    for dx, dz in ((-1, -1), (1, 1), (-1, 1), (1, -1)):
        add((dx, 2, dz), [(4, 0, 4, 12, 4, 12, "holo")])
    # Command desk (west), facing the table: the Command Terminal in the middle, consoles either side.
    for dz in (-1, 0, 1):
        add((-3, 1, dz), rotate(desk(big_screen=dz == 0), "east"))
    for dz in (-1, 1):
        add((-4, 1, dz), rotate(chair(), "east"))
    # Operator row (south-east), facing the video wall.
    for dx in (2, 3, 4):
        add((dx, 1, 3), desk())
        add((dx, 1, 4), chair())
    # Equipment racks in the south-west corner, crates by the door.
    for dz in (3, 4):
        add((-5, 1, dz), rotate(rack(), "east"))
    add((-4, 1, 5), [(1, 0, 1, 15, 12, 15, "crate"), (1, 12, 1, 15, 13, 15, "black")])
    add((-3, 1, 5), [(2, 0, 2, 14, 10, 14, "crate")])
    # Lift (north-east corner): glass and steel shaft with a call panel.
    for y in range(1, H):
        add((4, y, -4), [(0, 0, 0, 1.5, 16, 16, "lift_frame"), (0, 0, 14.5, 16, 16, 16, "lift_frame"),
                         (1.5, 0, 0, 16, 16, 1, "lift_glass"), (15, 0, 1, 16, 16, 14.5, "lift_glass")])
    add((3, 2, -5), [(13, 4, 15, 15, 8, 16, "black"), (13.5, 6, 14.8, 14.5, 7, 15, "red_light")])
    # Blast door (south) with a hazard frame.
    for dx in (-1, 0, 1):
        for y in (1, 2, 3):
            full((dx, y, 6), "door")
    for dx in (-2, 2):
        for y in (1, 2, 3, 4):
            full((dx, y, 6), "hazard" if y % 2 else "hazard_b")
    # A stencilled sign over the video wall.
    cells[(0, 6, -6)] = [(0, 0, 0, 16, 16, 16, "trim"), (1, 5, 16, 15, 10, 16.2, "sign")]
    return cells


def cutaway(cells):
    out = {}
    space = cross_section(5.45)
    for (x, y, z), boxes in cells.items():
        if y >= 2 and x + z > 3 and (x, z) not in space:
            continue
        out[(x, y, z)] = boxes
    return out


# ---------------------------------------------------------------- renderer

def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def render(cells, scale):
    """Isometric render (from the south-east) of the boxes; scale = pixels per block (half width)."""
    a, b, h = 2 * scale / 16, scale / 16, 2 * scale / 16
    boxes = []
    for (bx, by, bz), bs in cells.items():
        for x0, y0, z0, x1, y1, z1, c in bs:
            boxes.append((bx * 16 + x0, by * 16 + y0, bz * 16 + z0, bx * 16 + x1, by * 16 + y1, bz * 16 + z1, c,
                          (bx + bz, by)))
    ks = [p[0] for p in cells] + [p[2] for p in cells]
    lo, hi = min(ks) - 1, max(ks) + 3
    ymin, ymax = min(p[1] for p in cells), max(p[1] for p in cells) + 3
    W = int((hi - lo) * 4 * scale) + 40
    Hh = int((ymax - ymin) * 2 * scale + (hi - lo) * 2 * scale + 60)
    img = Image.new("RGBA", (W, Hh), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    ox, oy = W / 2, ymax * 2 * scale + 20 - 2 * lo * scale

    def P(x, y, z):
        return (ox + (x - z) * a, oy + (x + z) * b - y * h)

    boxes.sort(key=lambda q: (q[7][0], q[7][1], (q[0] + q[3] + q[2] + q[5]) / 2, (q[1] + q[4]) / 2))
    for x0, y0, z0, x1, y1, z1, c, _ in boxes:
        col = C[c]
        glow = c in GLOW
        al = ALPHA.get(c, 255)
        top = [P(x0, y1, z0), P(x1, y1, z0), P(x1, y1, z1), P(x0, y1, z1)]
        east = [P(x1, y0, z0), P(x1, y0, z1), P(x1, y1, z1), P(x1, y1, z0)]
        south = [P(x0, y0, z1), P(x1, y0, z1), P(x1, y1, z1), P(x0, y1, z1)]
        draw.polygon(south, fill=shade(col, 0.95 if glow else 0.72) + (al,))
        draw.polygon(east, fill=shade(col, 0.9 if glow else 0.55) + (al,))
        draw.polygon(top, fill=col + (al,))
    return img.crop(img.getbbox())


def wrap(d, x, y, text, font, width, fill):
    line = ""
    for word in text.split():
        if d.textlength(line + " " + word, font=font) > width:
            d.text((x, y), line.strip(), font=font, fill=fill)
            y += 21
            line = ""
        line += " " + word
    d.text((x, y), line.strip(), font=font, fill=fill)
    return y + 21


def main():
    out = Path(__file__).resolve().parent.parent / "build" / "drone_tower"
    out.mkdir(parents=True, exist_ok=True)
    F = "/usr/share/fonts/truetype/dejavu/"
    big = ImageFont.truetype(F + "DejaVuSans-Bold.ttf", 34)
    mid = ImageFont.truetype(F + "DejaVuSans-Bold.ttf", 20)
    sm = ImageFont.truetype(F + "DejaVuSans.ttf", 17)
    red, white, dim = (220, 80, 64), (236, 230, 230), (160, 140, 140)

    room_img = render(cutaway(room()), 40)
    furniture = [("Operator Chair", {(0, 0, 0): rotate(chair(), "south")}),
                 ("Console Desk (x2, joined)", {(0, 0, 0): desk(), (1, 0, 0): desk()}),
                 ("Command Terminal", {(0, 0, 0): desk(True)}),
                 ("Equipment Rack", {(0, 0, 0): rotate(rack(), "south")})]
    tiles = [(name, render(cells, 110)) for name, cells in furniture]

    pad = 30
    notes_w = 560
    tw = sum(max(t.width, 260) for _, t in tiles) + pad * (len(tiles) - 1)
    W = max(room_img.width + notes_w + pad * 3, tw + pad * 2)
    th = max(t.height for _, t in tiles)
    sheet = Image.new("RGB", (W, 110 + room_img.height + 80 + th + 90), (16, 12, 14))
    d = ImageDraw.Draw(sheet)
    d.text((pad, 18), "DRONE TOWER — COMMAND ROOM (concept v2)", font=big, fill=red)
    wrap(d, pad, 62, "A real operations room: every piece of furniture is its own block with a proper model, neutral white "
                     "light panels, matte surfaces; red only for status and warning lights. Cut away (roof and the south and "
                     "east walls removed).", sm, W - 2 * pad, dim)
    sheet.paste(room_img, (pad, 110), room_img)
    lx = pad * 2 + room_img.width
    notes = [("Video wall", "5x2 thin-bezel panels on the north wall: fleet, jobs, power, map"),
             ("Plotting table", "3x3 horizontal touch display with the map; a faint red projection of the tower above it"),
             ("Command desk", "west, facing the table: the Command Terminal (large display; opens the tower status screen) "
                              "between two consoles, with operator chairs"),
             ("Operator row", "three joined console desks with chairs, facing the video wall"),
             ("Equipment racks", "two server cabinets (2 high) with status LEDs, south-west corner"),
             ("Lift", "glass and steel shaft in the north-east corner with a call panel, up to the hangar floors"),
             ("Room", "acoustic wall panels between concrete columns, a steel trim line, raised access floor, carpet "
                      "tiles under the work stations; blast door south (cut away)"),
             ("Ceiling (hidden)", "flush white light panels, cable trays, ducts")]
    y = 120
    for title, text in notes:
        d.text((lx, y), title, font=mid, fill=white)
        y = wrap(d, lx, y + 26, text, sm, notes_w, dim) + 12
    fy = 110 + room_img.height + 40
    d.text((pad, fy), "NEW FURNITURE BLOCKS (custom models, rotatable; shown large)", font=mid, fill=red)
    x = pad
    for name, t in tiles:
        sheet.paste(t, (x, fy + 40 + th - t.height), t)
        d.text((x, fy + 52 + th), name, font=mid, fill=white)
        x += max(t.width, 260) + pad
    sheet.save(out / "command_room_concept.png")
    print(sheet.size)


if __name__ == "__main__":
    main()
