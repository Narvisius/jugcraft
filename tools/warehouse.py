"""The automated warehouse (Justin, 2026-10-16: "Let's do the warehouse next ... an automated system of ground based
drones that can go grab items and pallets from storage racks"). Its pieces' models and textures; the drones' parts
(drawn by the game at their pose, client/WarehouseRenderer.java); the pallet loads.

How it works (claude/jugcraft-warehouse-plan.md): items piped into the Intake Case Packer are packed into cases; a
Stacker Drone (a mobile robot arm) stacks the cases on a pallet at a Palletizing Station and stretch-wraps them (or
straps them); a Pallet Drone (a very-narrow-aisle turret truck) takes the pallet to a free place in the Pallet Rack.
The Warehouse Control Terminal keeps the books; asked for something, the drones bring the pallet out, the stacker
picks what was asked for, and it goes through a dispatch hatch into Ready Storage (a vertical lift module), where it
can be taken out by hand or by pipe.

Every piece is drawn front to +z (south) like the plant kit; the drones' parts in their own frame (x across, y up,
z the way they drive; origin the middle of their footprint at the floor, (8, 0, 8))."""
import math

from PIL import Image, ImageDraw

import plant_panels as PP
import switchgear_room as SGR

T225 = math.tan(math.radians(22.5))

# ------------------------------------------------------------------------------------------------ materials
YELLOW, GRAPHITE, RUBBER, HAZARD = "pi_wh_yellow", "pi_wh_graphite", "pi_wh_rubber", "pi_wh_hazard"
STEEL, ALU, CHROME, BLACK = "pi_wh_steel", "pi_wh_alu", "pi_chrome", "pi_cap_black"
BLUE, BLUE_FACE, BEAM, BEAM_FACE, DECK = "pi_wh_upright", "pi_wh_upright_face", "pi_wh_beam", "pi_wh_beam_face", "pi_wh_deck"
WOOD, WOOD_END, PBLOCK = "pi_wh_wood", "pi_wh_wood_end", "pi_wh_pallet_block"
FILM = "pi_wh_film"
MACHINE, MACHINE_DARK, STAINLESS, GUARD = "pi_wh_machine", "pi_wh_machine_dark", "pi_wh_stainless", "pi_wh_guard"
FORK, MAST = "pi_wh_fork", "pi_wh_mast"
LIGHT = "pi_wh_light"          # a drone's status strip (lit)


def box(x0, y0, z0, x1, y1, z1, tex, rot=None):
    b = (round(x0, 4), round(y0, 4), round(z0, 4), round(x1, 4), round(y1, 4), round(z1, 4), tex)
    return b + (rot,) if rot else b


def rot_y(cx, y, cz, angle):
    return {"rotation": {"angle": angle, "axis": "y", "origin": [cx, y, cz]}}


def rot_x(x, cy, cz, angle):
    return {"rotation": {"angle": angle, "axis": "x", "origin": [x, cy, cz]}}


def rot_z(cx, cy, z, angle):
    return {"rotation": {"angle": angle, "axis": "z", "origin": [cx, cy, z]}}


def round_y(cx, cz, a, y0, y1, tex):
    """An octagonal upright cylinder (apothem a); for a low one (under 0.7 high) a star, flatter still a square."""
    h = y1 - y0
    if h >= 0.7:
        return PP.round_y(cx, cz, a, y0, y1, tex)
    if h >= 0.25:
        return PP.star_y(cx, cz, a * 0.92, y0, y1, tex, inset=0.1)
    s = a * 0.9
    return [box(cx - s, y0, cz - s, cx + s, y1, cz + s, tex)]


def drum_y(cx, cz, a, y0, y1, side, top, front=None):
    """An upright octagonal cylinder whose lid picture (top) lies whole across its top (each of its four boxes' tops
    taking its own share of the picture); front: a picture on its front face only (+z)."""
    out = PP.round_y(cx, cz, a, y0, y1, side)
    res = []
    for k, b in enumerate(out):
        x0, _, z0, x1, _, z1 = b[:6]
        if k < 2:
            uv = [round((x0 - cx + a) / (2 * a) * 16, 4), round((z0 - cz + a) / (2 * a) * 16, 4),
                  round((x1 - cx + a) / (2 * a) * 16, 4), round((z1 - cz + a) / (2 * a) * 16, 4)]
            tex = {"*": side, "up": (top, uv)}
        else:
            tex = {"*": side, "up": side}
        if k == 1 and front:
            tex["south"] = front + "!"
        res.append(b[:6] + (tex,) + tuple(b[7:]))
    return res


def round_x(cy, cz, a, x0, x1, tex):
    """An octagonal cylinder lying along x (a wheel, a roller, a pin): two crossing boxes and the pair turned 45
    degrees about x, each step 0.1 shorter at both ends (no two faces in one plane). Needs 0.7 of length."""
    s = a * T225
    if x1 - x0 < 0.7:
        return [box(x0, cy - a * 0.92, cz - a * 0.92, x1, cy + a * 0.92, cz + a * 0.92, tex)]
    r = rot_x(x0, cy, cz, 45)
    return [box(x0, cy - s, cz - a, x1, cy + s, cz + a, tex), box(x0 + 0.1, cy - a, cz - s, x1 - 0.1, cy + a, cz + s, tex),
            box(x0 + 0.2, cy - s, cz - a, x1 - 0.2, cy + s, cz + a, tex, r), box(x0 + 0.3, cy - a, cz - s, x1 - 0.3, cy + a, cz + s, tex, r)]


def round_z(cx, cy, a, z0, z1, tex):
    """An octagonal cylinder along z (a knob, a lens, a suction cup seen end on); needs 0.7 of length."""
    s = a * T225
    if z1 - z0 < 0.7:
        return [box(cx - a * 0.92, cy - a * 0.92, z0, cx + a * 0.92, cy + a * 0.92, z1, tex)]
    r = rot_z(cx, cy, z0, 45)
    return [box(cx - a, cy - s, z0, cx + a, cy + s, z1, tex), box(cx - s, cy - a, z0 + 0.1, cx + s, cy + a, z1 - 0.1, tex),
            box(cx - a, cy - s, z0 + 0.2, cx + a, cy + s, z1 - 0.2, tex, r), box(cx - s, cy - a, z0 + 0.3, cx + s, cy + a, z1 - 0.3, tex, r)]


def rrect_y(x0, z0, x1, z1, y0, y1, c, tex):
    """A slab with its four upright corners cut at 45 degrees (c across the cut): the body as a cross of two boxes,
    each corner's chamfer a thin box turned 45 degrees about y."""
    out = [box(x0 + c, y0, z0, x1 - c, y1, z1, tex), box(x0, y0 + 0.1, z0 + c, x1, y1 - 0.1, z1 - c, tex)]
    d = c / math.sqrt(2)
    for cx, cz, ang in ((x0 + c / 2, z0 + c / 2, 45), (x1 - c / 2, z0 + c / 2, -45), (x0 + c / 2, z1 - c / 2, -45), (x1 - c / 2, z1 - c / 2, 45)):
        out.append(box(cx - d, y0 + 0.2, cz - d, cx + d, y1 - 0.2, cz + d, tex, rot_y(cx, y0, cz, ang)))
    return out


def shifted(boxes, dx=0.0, dy=0.0, dz=0.0):
    """Boxes moved (their turned boxes' origins with them)."""
    out = []
    for b in boxes:
        nb = (round(b[0] + dx, 4), round(b[1] + dy, 4), round(b[2] + dz, 4), round(b[3] + dx, 4), round(b[4] + dy, 4), round(b[5] + dz, 4), b[6])
        if len(b) > 7 and b[7]:
            r = dict(b[7]["rotation"])
            o = r["origin"]
            r["origin"] = [round(o[0] + dx, 4), round(o[1] + dy, 4), round(o[2] + dz, 4)]
            nb += ({"rotation": r},)
        out.append(nb)
    return out


def mirror_x(boxes, cx=8.0):
    """Boxes mirrored across x = cx (a turned box's angle with them; faces keep their textures)."""
    out = []
    for b in boxes:
        tex = b[6]
        if isinstance(tex, dict) and ("east" in tex or "west" in tex):
            tex = dict(tex)
            tex["east"], tex["west"] = b[6].get("west", b[6].get("*")), b[6].get("east", b[6].get("*"))
            tex = {k: v for k, v in tex.items() if v is not None}
        nb = (round(2 * cx - b[3], 4), b[1], b[2], round(2 * cx - b[0], 4), b[4], b[5], tex)
        if len(b) > 7 and b[7]:
            r = dict(b[7]["rotation"])
            o = r["origin"]
            r["origin"] = [round(2 * cx - o[0], 4), o[1], o[2]]
            if r["axis"] in ("y", "z"):
                r["angle"] = -r["angle"]
            nb += ({"rotation": r},)
        out.append(nb)
    return out


def split_blocks(boxes, w, h, d):
    """A piece's boxes (its own pixels, w x h x d blocks) cut into its blocks: {(c, r, k): boxes in that block}, c
    along x, r up, k along z. A face with its whole texture on it ("name!") keeps its share of that texture."""
    out = {}
    for b in boxes:
        turned = len(b) > 7 and b[7]
        if turned:
            # A turned box goes whole into the block holding its origin.
            o = b[7]["rotation"]["origin"]
            c, r, k = int(min(w - 1, max(0, o[0] // 16))), int(min(h - 1, max(0, o[1] // 16))), int(min(d - 1, max(0, o[2] // 16)))
            # (Unless it would then stick out past the game's -16..32: then the block holding its middle.)
            fit = lambda i, n, at: at if -16 <= b[i] - 16 * at and b[i + 3] - 16 * at <= 32 else int(min(n - 1, max(0, (b[i] + b[i + 3]) / 2 // 16)))
            c, r, k = fit(0, w, c), fit(1, h, r), fit(2, d, k)
            out.setdefault((c, r, k), []).extend(shifted([b], -16 * c, -16 * r, -16 * k))
            continue
        # (The edge blocks keep what stands out past the piece: a canopy, a flange, a tray.)
        lo = lambda i: -1e9 if i == 0 else 16 * i
        hi = lambda i, n: 1e9 if i == n - 1 else 16 * i + 16
        for c in range(w):
            for r in range(h):
                for k in range(d):
                    x0, x1 = max(b[0], lo(c)), min(b[3], hi(c, w))
                    y0, y1 = max(b[1], lo(r)), min(b[4], hi(r, h))
                    z0, z1 = max(b[2], lo(k)), min(b[5], hi(k, d))
                    if x1 - x0 <= 0.01 or y1 - y0 <= 0.01 or z1 - z0 <= 0.01:
                        continue
                    tex = _cut_tex(b, x0, y0, z0, x1, y1, z1)
                    out.setdefault((c, r, k), []).append(box(x0 - 16 * c, y0 - 16 * r, z0 - 16 * k, x1 - 16 * c, y1 - 16 * r, z1 - 16 * k, tex))
    return out


def _cut_tex(b, x0, y0, z0, x1, y1, z1):
    """A cut box's faces: a whole-texture face ("name!") gets its share of the picture as an explicit uv."""
    tex = b[6]
    if not isinstance(tex, dict) or (x0, y0, z0, x1, y1, z1) == tuple(b[:6]):
        return tex
    out = dict(tex)
    bx0, by0, bz0, bx1, by1, bz1 = b[:6]
    f = lambda v, a, c: round((v - a) / (c - a) * 16, 4) if c > a else 0.0
    for face, t in tex.items():
        if not (isinstance(t, str) and t.endswith("!")):
            continue
        name = t[:-1]
        if face in ("south", "north"):
            u0, u1 = (f(x0, bx0, bx1), f(x1, bx0, bx1)) if face == "south" else (f(bx1 - (x1 - bx0), bx0, bx1), f(bx1 - (x0 - bx0), bx0, bx1))
            v0, v1 = 16 - f(y1, by0, by1), 16 - f(y0, by0, by1)
        elif face in ("east", "west"):
            u0, u1 = (f(bz1 - (z1 - bz0), bz0, bz1), f(bz1 - (z0 - bz0), bz0, bz1)) if face == "east" else (f(z0, bz0, bz1), f(z1, bz0, bz1))
            v0, v1 = 16 - f(y1, by0, by1), 16 - f(y0, by0, by1)
        else:
            u0, u1 = f(x0, bx0, bx1), f(x1, bx0, bx1)
            v0, v1 = (f(z0, bz0, bz1), f(z1, bz0, bz1)) if face == "up" else (16 - f(z1, bz0, bz1), 16 - f(z0, bz0, bz1))
        out[face] = (name, [u0, v0, u1, v1])
    return out


# ================================================================================================ textures
def _img(w, h, colour):
    return Image.new("RGBA", (w, h), tuple(colour) + (255,) if len(colour) == 3 else tuple(colour))


def _sq(img):
    w, h = img.size
    return img if w == h else img.resize((max(w, h), max(w, h)), Image.LANCZOS)


def _t(d, xy, s, size, fill, anchor="mm", width=None, bold=True):
    SGR._text(d, xy, s, size, fill, anchor=anchor, bold=bold, width=width)


def _barcode(d, x0, y0, x1, y1, seed, colour=(20, 20, 22)):
    import random as _r
    r = _r.Random(seed)
    x = x0
    while x < x1:
        w = r.choice((1, 1, 2, 3))
        if r.random() < 0.55:
            d.rectangle([x, y0, min(x1, x + w - 1), y1], fill=colour)
        x += w


def _hazard(size=128, w=18, a=(236, 184, 20), b=(26, 26, 28)):
    img = _img(size, size, a)
    d = ImageDraw.Draw(img)
    for k in range(-size, 2 * size, 2 * w):
        d.polygon([(k, size), (k + w, size), (k + w + size, 0), (k + size, 0)], fill=b)
    return img


def _upright_face():
    """A rack upright's front: blue powder coat with its column of teardrop slots (where the beams hook on) and the
    small round holes beside them."""
    import nuclear_materials as M
    img = PP._solid((30, 76, 150), 2101, 0.05).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    for y in range(4, 128, 16):
        d.polygon([(56, y + 1), (72, y + 1), (68, y + 9), (64, y + 11), (60, y + 9)], fill=(14, 22, 40))
        d.ellipse([40, y + 4, 46, y + 10], fill=(14, 22, 40))
        d.ellipse([82, y + 4, 88, y + 10], fill=(14, 22, 40))
    return img


def _beam_face():
    img = PP._solid((226, 100, 24), 2102, 0.06).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 127, 10], fill=(196, 82, 16))          # the step the deck sits in
    d.line([(0, 11), (127, 11)], fill=(150, 60, 10), width=2)
    return img


def _deck():
    """Wire mesh decking: galvanised wires on a 50 mm grid with its channels; see-through between."""
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for k in range(0, 128, 16):
        d.line([(k + 1, 0), (k + 1, 127)], fill=(176, 180, 182, 255), width=2)
        d.line([(0, k + 1), (127, k + 1)], fill=(160, 164, 166, 255), width=2)
    for k in (30, 96):
        d.rectangle([k, 0, k + 6, 127], fill=(150, 154, 156, 255))
    return img


def _wood(seed, size=128, end=False):
    """Pine pallet boards: pale sawn wood, its grain along, a knot or two, nail heads; an end: growth rings."""
    import random as _r
    import nuclear_materials as M
    r = _r.Random(seed)
    img = M.painted((206, 172, 122), seed, chip_count=0, drip_count=0, dirt=0.15).convert("RGBA").resize((size, size))
    d = ImageDraw.Draw(img)
    if end:
        for k in range(10):
            cx, cy = r.randint(-40, 160), r.randint(-40, 160)
            for rr in range(8, 120, 9):
                d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], outline=(178, 140, 92, 255))
        return img
    for _ in range(40):
        y = r.randint(0, size)
        d.line([(0, y), (size, y + r.randint(-4, 4))], fill=(186, 150, 102, 255), width=1)
    for _ in range(2):
        x, y = r.randint(10, size - 10), r.randint(10, size - 10)
        d.ellipse([x - 5, y - 3, x + 5, y + 3], fill=(150, 110, 66, 255))
    for x in (12, size - 14):
        for y in (20, size - 22):
            d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(90, 90, 92, 255))
    return img


def _pallet_block():
    img = _wood(2105, 128, end=True)
    d = ImageDraw.Draw(img)
    d.rectangle([30, 40, 98, 88], outline=(60, 40, 24, 255), width=3)
    _t(d, (64, 56), "HT", 26, (60, 40, 24))
    _t(d, (64, 78), "WH-1", 14, (60, 40, 24))
    return img


def _carton(seed, white=False):
    """A corrugated case's side: kraft (or white-faced) board, faint flutes, scuffs."""
    import random as _r
    import nuclear_materials as M
    r = _r.Random(seed)
    col = (232, 228, 218) if white else (184, 140, 88)
    img = M.painted(col, seed, chip_count=0, drip_count=0, dirt=0.12).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    for y in range(0, 128, 8):
        d.line([(0, y), (127, y)], fill=tuple(max(0, c - 4) for c in col) + (255,), width=1)
    for _ in range(3):
        x, y = r.randint(0, 120), r.randint(0, 120)
        d.line([(x, y), (x + r.randint(6, 20), y + r.randint(-3, 3))], fill=tuple(max(0, c - 30) for c in col) + (255,), width=1)
    return img


def _carton_top(white=False):
    img = _carton(2110 + white, white)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 52, 127, 76], fill=(206, 170, 120, 255) if not white else (214, 200, 170, 255))   # the tape down the flaps' seam
    d.line([(0, 64), (127, 64)], fill=(150, 112, 70, 255), width=2)
    return img


def _carton_front(white=False):
    """A case's front: its shipping label (consignee lines, the barcode, WH IN), THIS WAY UP arrows."""
    img = _carton(2112 + white, white).resize((256, 256))
    d = ImageDraw.Draw(img)
    d.rectangle([28, 70, 200, 196], fill=(250, 250, 246, 255), outline=(200, 200, 196, 255), width=2)
    _t(d, (40, 86), "WAREHOUSE  IN", 14, (24, 24, 26), anchor="lm")
    for k, w in enumerate((140, 110, 124)):
        d.rectangle([40, 102 + k * 12, 40 + w, 107 + k * 12], fill=(90, 90, 96, 255))
    _barcode(d, 40, 142, 188, 180, 2113)
    for x in (214, 236):
        d.polygon([(x, 26), (x - 9, 42), (x + 9, 42)], fill=(30, 30, 32, 255))
        d.rectangle([x - 3, 42, x + 3, 60], fill=(30, 30, 32, 255))
    if white:
        d.rectangle([0, 220, 255, 255], fill=(30, 90, 160, 255))
    return img


def _film():
    """Stretch film: a clear milky skin with the streaks and folds of the wrap going round."""
    import random as _r
    r = _r.Random(2120)
    img = Image.new("RGBA", (128, 128), (232, 236, 240, 46))
    d = ImageDraw.Draw(img)
    for _ in range(10):
        y = r.randint(0, 128)
        d.line([(0, y), (128, y + r.randint(-10, 10))], fill=(250, 252, 255, r.randint(60, 96)), width=1)
    for y in (30, 94):
        d.rectangle([0, y, 127, y + 2], fill=(250, 252, 255, 84))
    return img


def _sack(print_=False):
    """A woven polypropylene sack: the weave, its seam; the printed face: the band and 25 kg."""
    img = _img(128, 128, (226, 224, 214))
    d = ImageDraw.Draw(img)
    for k in range(0, 128, 3):
        d.line([(k, 0), (k, 127)], fill=(214, 212, 202, 255))
        d.line([(0, k), (127, k)], fill=(218, 216, 206, 255))
    if print_:
        d.rectangle([0, 40, 127, 88], fill=(40, 96, 170, 255))
        _t(d, (64, 64), "25 kg", 26, (250, 250, 246))
    return img


def _crate(top=False):
    """A wooden crate: horizontal slats with the gaps between, nails at the ends; its lid: boards across."""
    img = _wood(2125 + top, 128)
    d = ImageDraw.Draw(img)
    for y in range(0, 128, 32):
        d.rectangle([0, y + 27, 127, y + 31], fill=(70, 50, 32, 255))
    if not top:
        d.rectangle([0, 0, 14, 127], fill=(176, 140, 94, 255))
        d.rectangle([113, 0, 127, 127], fill=(176, 140, 94, 255))
    return img


def _drum(top=False, colour=(30, 76, 150), label=False):
    """A steel drum: its paint with the two rolling hoops and the chime; its lid: the two bungs, the chime ring."""
    img = PP._solid(colour, 2130 + top, 0.08).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    dark = tuple(max(0, c - 40) for c in colour) + (255,)
    if top:
        d.ellipse([4, 4, 123, 123], outline=dark, width=6)
        for cx, r_ in ((40, 10), (90, 6)):
            d.ellipse([cx - r_, 60 - r_, cx + r_, 60 + r_], fill=(150, 152, 156, 255), outline=dark, width=2)
        return img
    for y in (0, 40, 84, 124):
        d.rectangle([0, y, 127, y + 4], fill=dark)
    if label:
        d.rectangle([20, 52, 108, 76], fill=(246, 246, 240, 255))
        _barcode(d, 26, 56, 100, 72, 2132)
    return img


def _ingots(end=False):
    """A bundle of ingots from the side: rows of their trapezoid ends (alternately up and down); along: their tops."""
    img = _img(128, 128, (150, 154, 160))
    d = ImageDraw.Draw(img)
    for row in range(0, 128, 21):
        for k, x in enumerate(range(-8, 136, 22)):
            up = (k + row // 21) % 2
            if end:
                pts = [(x, row + 20), (x + 20, row + 20), (x + 16, row + 2), (x + 4, row + 2)] if up else \
                      [(x + 4, row + 20), (x + 16, row + 20), (x + 20, row + 2), (x, row + 2)]
            else:
                pts = [(x, row + 20), (x + 20, row + 20), (x + 20, row + 2), (x, row + 2)]
            d.polygon(pts, fill=(172, 176, 182, 255) if up else (140, 144, 150, 255), outline=(96, 98, 104, 255))
    return img


def _strap():
    img = _img(64, 64, (30, 30, 32))
    d = ImageDraw.Draw(img)
    for x in range(0, 64, 4):
        d.line([(x, 0), (x, 63)], fill=(44, 44, 48, 255))
    return img


def _lane(mask, code):
    """A drone lane floor tile from above (64 px): sealed grey concrete with a sheen, the navigation code sticker in
    the middle (a 2D code the drones read), the yellow line along each edge with no lane beyond it (mask: N 1, E 2,
    S 4, W 8)."""
    import random as _r
    import nuclear_materials as M
    r = _r.Random(2140 + code)
    img = M.concrete(2141 + code, tie_holes=False).convert("RGBA").resize((128, 128))
    over = Image.new("RGBA", (128, 128), (150, 156, 158, 120))
    img = Image.alpha_composite(img, over)
    d = ImageDraw.Draw(img)
    # The code sticker: white square, black border, a 6 x 6 data grid, the finder corner.
    d.rectangle([48, 48, 79, 79], fill=(246, 246, 242, 255))
    d.rectangle([50, 50, 77, 77], outline=(20, 20, 22, 255), width=2)
    for i in range(6):
        for j in range(6):
            if r.random() < 0.5 or (i < 2 and j < 2):
                d.rectangle([53 + i * 4, 53 + j * 4, 56 + i * 4, 56 + j * 4], fill=(20, 20, 22, 255))
    Y = (236, 190, 24, 255)
    if mask & 1:
        d.rectangle([0, 0, 127, 7], fill=Y)
    if mask & 4:
        d.rectangle([0, 120, 127, 127], fill=Y)
    if mask & 2:
        d.rectangle([120, 0, 127, 127], fill=Y)
    if mask & 8:
        d.rectangle([0, 0, 7, 127], fill=Y)
    return img.resize((64, 64), Image.LANCZOS)


def _pack_spot():
    """The palletizing spot from above: the hatched border, the white pallet outline, PALLET BUILD."""
    import nuclear_materials as M
    img = M.concrete(2150, tie_holes=False).convert("RGBA").resize((256, 256))
    img = Image.alpha_composite(img, Image.new("RGBA", (256, 256), (150, 156, 158, 120)))
    haz = _hazard(256, 22)
    mask = Image.new("L", (256, 256), 0)
    md = ImageDraw.Draw(mask)
    md.rectangle([0, 0, 255, 255], fill=255)
    md.rectangle([20, 20, 235, 235], fill=0)
    img.paste(haz, (0, 0), mask)
    d = ImageDraw.Draw(img)
    d.rectangle([34, 34, 221, 221], outline=(246, 246, 240, 255), width=5)
    _t(d, (128, 128), "PALLET BUILD", 26, (246, 246, 240), width=170)
    return img


def _dock_plate():
    """A charging dock's floor plate: checker steel, the chevrons in, CHARGE, the two contact strips."""
    import nuclear_materials as M
    img = M.brushed((70, 72, 76), 2155, "h", dirt=0.2).convert("RGBA").resize((256, 256))
    d = ImageDraw.Draw(img)
    for x in range(8, 256, 16):
        for y in range(8, 256, 16):
            d.line([(x - 4, y + 2), (x + 4, y - 2)], fill=(96, 98, 102, 255), width=2)
    for k in range(3):
        y = 200 - k * 34
        d.polygon([(40, y), (128, y - 30), (216, y), (216, y + 14), (128, y - 16), (40, y + 14)], fill=(236, 184, 20, 255))
    _t(d, (128, 40), "CHARGE", 28, (236, 184, 20))
    return img


def _charger_front():
    """The charger cabinet's front: the LED ring and bar, the display, the vents, the name plate."""
    img = _img(256, 256, (196, 200, 204))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 255], outline=(150, 154, 158, 255), width=6)
    d.rectangle([28, 30, 228, 92], fill=(20, 24, 30, 255))
    _t(d, (128, 50), "DRONE CHARGER", 18, (120, 200, 240))
    _t(d, (128, 76), "24 V  150 A", 16, (200, 210, 220))
    for k in range(10):
        d.rectangle([40 + k * 18, 112, 52 + k * 18, 124], fill=(70, 210, 120, 255) if k < 8 else (40, 60, 50, 255))
    for y in range(150, 230, 12):
        d.rectangle([40, y, 216, y + 5], fill=(120, 124, 128, 255))
    return img


def _decal(text, sub):
    """A drone's side panel: its name and number, the hazard band along the bottom, KEEP CLEAR."""
    img = _img(256, 128, (236, 184, 20))
    d = ImageDraw.Draw(img)
    haz = _hazard(256, 14).crop((0, 0, 256, 22))
    img.paste(haz, (0, 106))
    _t(d, (14, 34), text, 30, (26, 26, 28), anchor="lm", width=228)
    _t(d, (14, 74), sub, 16, (26, 26, 28), anchor="lm", width=228)
    return _sq(img)


def _mecanum():
    """A mecanum wheel's face: the hub, and round its rim the rollers set at 45 degrees."""
    img = _img(128, 128, (40, 40, 44))
    d = ImageDraw.Draw(img)
    for k in range(0, 160, 18):
        d.polygon([(k - 30, 128), (k - 18, 128), (k + 10, 0), (k - 2, 0)], fill=(70, 72, 76, 255))
    d.ellipse([40, 40, 88, 88], fill=(150, 154, 158, 255), outline=(90, 92, 96, 255), width=4)
    return img


def _tyre():
    img = _img(64, 64, (34, 34, 36))
    d = ImageDraw.Draw(img)
    for k in range(0, 64, 8):
        d.line([(k, 0), (k + 8, 63)], fill=(52, 52, 56, 255), width=3)
    return img


def _lidar():
    """A safety laser scanner's head: black, the dark window band all round."""
    img = _img(64, 64, (34, 34, 38))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 24, 63, 42], fill=(10, 14, 24, 255))
    d.line([(0, 26), (63, 26)], fill=(60, 80, 120, 255), width=1)
    return img


def _estop():
    img = _img(64, 64, (200, 30, 26))
    d = ImageDraw.Draw(img)
    d.ellipse([8, 8, 56, 56], outline=(150, 20, 18, 255), width=4)
    return img


def _screen(title, lines, accent=(120, 200, 240)):
    """A small screen's page: the title bar and its lines (a drone's panel, a machine's HMI)."""
    img = _img(256, 160, (12, 18, 28))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 30], fill=(24, 48, 84, 255))
    _t(d, (10, 15), title, 18, (236, 240, 246), anchor="lm", width=236)
    for k, (a, b, col) in enumerate(lines):
        y = 50 + k * 24
        _t(d, (10, y), a, 16, (150, 160, 178), anchor="lm", width=128 if b else 236)
        _t(d, (246, y), b, 16, col or accent, anchor="rm", width=104)
    return _sq(img)


def _guard():
    """A machine guard's polycarbonate window: clear, its scratches and reflections."""
    img = Image.new("RGBA", (128, 128), (200, 220, 230, 40))
    d = ImageDraw.Draw(img)
    for k in range(-60, 128, 40):
        d.line([(k, 128), (k + 60, 0)], fill=(240, 248, 255, 70), width=6)
    return img


def _rollers():
    """A roller conveyor from above: chrome rollers across, the dark gaps between."""
    img = _img(128, 128, (30, 30, 34))
    d = ImageDraw.Draw(img)
    for y in range(0, 128, 16):
        d.rectangle([0, y + 2, 127, y + 12], fill=(190, 194, 198, 255))
        d.line([(0, y + 4), (127, y + 4)], fill=(236, 240, 244, 255), width=2)
    return img


def _stack(colour, lit):
    c = colour if lit else tuple(int(v * 0.35) for v in colour)
    img = Image.new("RGBA", (64, 64), c + ((254,) if lit else (255,)))
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 8):
        d.line([(0, y), (63, y)], fill=tuple(min(255, v + 30) for v in c) + ((254,) if lit else (255,)))
    return img


def _blanks():
    """The case erector's magazine of flat case blanks seen edge on: brown board edges stacked."""
    img = _img(128, 128, (150, 112, 70))
    d = ImageDraw.Draw(img)
    for y in range(0, 128, 4):
        d.line([(0, y), (127, y)], fill=(190, 150, 100, 255), width=2)
    return img


def _desk_screen(kind):
    """The terminal's three monitors (their live pages drawn by the game; these are what they show in the mockup)."""
    if kind == "stock":
        return _screen("WMS  STOCK", [("PALLET PLACES", "144", None), ("IN USE", "37  (26%)", None), ("ITEMS", "41,206", None),
                                      ("ITEM TYPES", "58", None), ("INTAKE WAITING", "3 CASES", (255, 190, 60))])
    if kind == "fleet":
        return _screen("FLEET", [("PD-01", "PUTAWAY A-03-2", (110, 230, 140)), ("PD-02", "TO STATION", (110, 230, 140)),
                                 ("SD-01", "STACKING 5/8", (110, 230, 140)), ("SD-02", "CHARGING 64%", (120, 200, 240)),
                                 ("ALL CLEAR", "", None)])
    return _screen("ORDERS", [("1042 COBBLESTONE", "TO COUNTER", (110, 230, 140)), ("1043 IRON INGOT", "PICKING", (255, 190, 60)),
                              ("1044 OAK LOG", "QUEUED", None), ("READY STORAGE", "61% FULL", None)])


def makers():
    import nuclear_materials as M
    solid = PP._solid
    out = {
        YELLOW: lambda: solid((236, 184, 20), 2160, 0.04), GRAPHITE: lambda: solid((52, 56, 60), 2161, 0.05),
        RUBBER: lambda: solid((28, 28, 30), 2162, 0.03), HAZARD: lambda: _hazard(),
        STEEL: lambda: M.brushed((176, 180, 182), 2163, "v", dirt=0.08), ALU: lambda: M.brushed((198, 202, 206), 2164, "v", dirt=0.04),
        BLUE: lambda: solid((30, 76, 150), 2165, 0.05), BLUE_FACE: _upright_face, BEAM: lambda: solid((226, 100, 24), 2166, 0.06),
        BEAM_FACE: _beam_face, DECK: _deck, WOOD: lambda: _wood(2103), WOOD_END: lambda: _wood(2104, end=True), PBLOCK: _pallet_block,
        FILM: _film, "pi_wh_carton": lambda: _carton(2107), "pi_wh_carton_top": lambda: _carton_top(False),
        "pi_wh_carton_front": lambda: _carton_front(False), "pi_wh_carton_w": lambda: _carton(2108, True),
        "pi_wh_carton_w_top": lambda: _carton_top(True), "pi_wh_carton_w_front": lambda: _carton_front(True),
        "pi_wh_sack": lambda: _sack(False), "pi_wh_sack_print": lambda: _sack(True), "pi_wh_crate": lambda: _crate(False),
        "pi_wh_crate_top": lambda: _crate(True), "pi_wh_drum": lambda: _drum(False), "pi_wh_drum_top": lambda: _drum(True),
        "pi_wh_drum_label": lambda: _drum(False, label=True),
        "pi_wh_ingot_end": lambda: _ingots(True), "pi_wh_ingot_side": lambda: _ingots(False), "pi_wh_strap": _strap,
        "pi_wh_band": lambda: M.brushed((150, 154, 158), 2167, "h", dirt=0.05),
        "pi_wh_pack_spot": _pack_spot, "pi_wh_lane_side": lambda: M.concrete(2168, tie_holes=False),
        MACHINE: lambda: solid((70, 96, 124), 2169, 0.04), MACHINE_DARK: lambda: solid((44, 58, 76), 2170, 0.04),
        STAINLESS: lambda: M.brushed((196, 200, 204), 2171, "v", dirt=0.03), GUARD: _guard,
        "pi_wh_rollers": _rollers, "pi_wh_conv_frame": lambda: M.brushed((170, 174, 178), 2172, "h", dirt=0.04),
        "pi_wh_blanks": _blanks, "pi_wh_tape": lambda: solid((176, 132, 80), 2173, 0.02),
        "pi_wh_hmi_intake": lambda: _screen("INTAKE 1", [("STATUS", "RUNNING", (110, 230, 140)), ("ITEMS WAITING", "1,284", None),
                                                          ("CASES PACKED", "312", None), ("BLANKS", "OK", (110, 230, 140))]),
        FORK: lambda: M.brushed((64, 64, 66), 2174, "h", dirt=0.3), MAST: lambda: solid((44, 46, 50), 2175, 0.06),
        LIGHT: lambda: _stack((80, 220, 240), True), "pi_wh_beacon": lambda: _stack((255, 170, 30), True),
        "pi_wh_lamp_red": lambda: _stack((255, 60, 40), False), "pi_wh_lamp_amber": lambda: _stack((255, 170, 30), False),
        "pi_wh_lamp_green": lambda: _stack((60, 220, 90), True), "pi_wh_lamp_blue": lambda: _stack((60, 120, 255), False),
        "pi_wh_lidar": _lidar, "pi_wh_estop": _estop, "pi_wh_mecanum": _mecanum, "pi_wh_tyre": _tyre,
        "pi_wh_cup": lambda: solid((24, 24, 26), 2176, 0.02), "pi_wh_film_roll": lambda: solid((236, 240, 244), 2177, 0.02),
        "pi_wh_core": lambda: solid((150, 110, 70), 2178, 0.04), "pi_wh_hose": lambda: solid((22, 22, 24), 2179, 0.02),
        "pi_wh_copper": lambda: M.brushed((196, 120, 70), 2180, "h", dirt=0.1),
        "pi_wh_pd_decal": lambda: _decal("PALLET DRONE", "AUTOMATED - KEEP CLEAR"),
        "pi_wh_sd_decal": lambda: _decal("STACKER DRONE", "ROBOT ARM - KEEP CLEAR"),
        "pi_wh_pd_screen": lambda: _screen("PD-01", [("TASK", "PUTAWAY", (110, 230, 140)), ("TO", "A-03-2", None), ("BATTERY", "82%", None)]),
        "pi_wh_dock_plate": _dock_plate, "pi_wh_charger_front": _charger_front,
        "pi_wh_desk": lambda: solid((214, 216, 214), 2181, 0.03), "pi_wh_desk_leg": lambda: solid((60, 62, 66), 2182, 0.04),
        "pi_wh_scr_stock": lambda: _desk_screen("stock"), "pi_wh_scr_fleet": lambda: _desk_screen("fleet"),
        "pi_wh_scr_orders": lambda: _desk_screen("orders"),
    }
    for m in range(16):
        for c in range(4):
            out[f"pi_wh_lane_{m}_{c}"] = lambda m=m, c=c: _lane(m, c)
    return out


SHARP = {"pi_wh_carton_front", "pi_wh_carton_w_front", "pi_wh_sack_print", "pi_wh_pack_spot", "pi_wh_dock_plate", "pi_wh_charger_front",
         "pi_wh_pd_decal", "pi_wh_sd_decal", "pi_wh_pd_screen", "pi_wh_hmi_intake", "pi_wh_scr_stock", "pi_wh_scr_fleet",
         "pi_wh_scr_orders", PBLOCK} | {f"pi_wh_lane_{m}_{c}" for m in range(16) for c in range(4)}
SEE_THROUGH = {FILM, GUARD, DECK}


# ================================================================================================ the pallet and its loads
PALLET_H = 2.3          # a pallet's height (144 mm): its loads stand on it from here
P0, P1 = 1.0, 15.0      # its footprint, x and z


def pallet():
    """A wooden pallet (a four-way block pallet, 1.2 by 0.8 m made square to its block): three bottom boards, nine
    blocks (stamped HT on their ends), three stringer boards across them, five top deck boards. The fork pockets
    between the blocks, front and side, are open."""
    out = []
    end = {"*": WOOD, "north": WOOD_END, "south": WOOD_END}
    side = {"*": WOOD, "east": WOOD_END, "west": WOOD_END}
    for x0, x1 in ((1.0, 3.2), (6.9, 9.1), (12.8, 15.0)):
        out.append(box(x0, 0.0, P0, x1, 0.4, P1, end))                                 # bottom boards, along z
    for x0, x1 in ((1.0, 3.2), (6.9, 9.1), (12.8, 15.0)):
        for z0, z1 in ((1.0, 3.4), (6.8, 9.2), (12.6, 15.0)):
            out.append(box(x0 + 0.05, 0.4, z0 + 0.05, x1 - 0.05, 1.6, z1 - 0.05, {"*": PBLOCK + "!", "up": WOOD_END, "down": WOOD_END}))
    for z0, z1 in ((1.0, 3.4), (6.8, 9.2), (12.6, 15.0)):
        out.append(box(P0, 1.6, z0, P1, 1.95, z1, side))                               # stringers, along x
    for x0, x1 in ((1.0, 3.4), (4.2, 6.2), (6.9, 9.1), (9.8, 11.8), (12.6, 15.0)):
        out.append(box(x0, 1.95, P0, x1, PALLET_H, P1, end))                           # top deck boards, along z
    return out


CASE = 6.8          # a case's width and depth (on a 2 x 2 layer)
CASE_H = 5.5        # its height (two layers to a pallet)
LOADS = ("carton", "carton_w", "sack", "crate", "drum", "ingot", "mixed")


def _case(x0, y0, z0, kind, front):
    t = {"carton": ("pi_wh_carton", "pi_wh_carton_top", "pi_wh_carton_front"), "carton_w": ("pi_wh_carton_w", "pi_wh_carton_w_top", "pi_wh_carton_w_front"),
         "crate": ("pi_wh_crate", "pi_wh_crate_top", "pi_wh_crate")}[kind]
    tex = {"*": t[0], "up": t[1]}
    if front:
        tex["south"] = t[2] + ("!" if kind != "crate" else "")
    return box(x0, y0, z0, x0 + CASE, y0 + CASE_H, z0 + CASE, tex)


def load(kind, n=8, wrapped=True, y=PALLET_H):
    """What stands on a pallet (y its top): n of its eight places filled (two layers of four, bottom first):
    cases (kraft or white), crates (strapped, not wrapped), sacks (three to a layer, crossed), drums (four, strapped),
    an ingot bundle (banded); "mixed" a picked pallet: cases of two kinds. Cases and sacks are stretch-wrapped."""
    out = []
    spots = [(1.1, 1.1), (8.1, 1.1), (1.1, 8.1), (8.1, 8.1)]
    top = y
    if kind in ("carton", "carton_w", "crate", "mixed"):
        for k in range(min(n, 8)):
            layer, (x0, z0) = divmod(k, 4)[0], spots[k % 4]
            kk = kind if kind != "mixed" else ("carton" if (k % 3) else "carton_w")
            out.append(_case(x0, y + layer * (CASE_H + 0.05), z0, kk, front=z0 > 4))
            top = max(top, y + (layer + 1) * (CASE_H + 0.05))
        if kind == "crate":
            out += _straps(y, top, 2)
            wrapped = False
    elif kind == "sack":
        layers = max(1, min(3, (n + 2) // 3))
        for L in range(layers):
            y0 = y + L * 3.3
            for k in range(3):
                if L % 2 == 0:
                    z0 = 1.2 + k * 4.55
                    out += [box(1.2, y0, z0, 14.8, y0 + 3.2, z0 + 4.4, {"*": "pi_wh_sack", "south": "pi_wh_sack_print" if k == 2 else "pi_wh_sack"}),
                            box(1.6, y0 + 3.2, z0 + 0.5, 14.4, y0 + 3.25, z0 + 3.9, "pi_wh_sack")]
                else:
                    x0 = 1.2 + k * 4.55
                    out += [box(x0, y0, 1.2, x0 + 4.4, y0 + 3.2, 14.8, {"*": "pi_wh_sack", "south": "pi_wh_sack_print"}),
                            box(x0 + 0.5, y0 + 3.2, 1.6, x0 + 3.9, y0 + 3.25, 14.4, "pi_wh_sack")]
            top = y0 + 3.25
    elif kind == "drum":
        for k, (cx, cz) in enumerate(((4.6, 4.6), (11.4, 4.6), (4.6, 11.4), (11.4, 11.4))[:max(1, min(4, (n + 1) // 2))]):
            out += drum_y(cx, cz, 3.3, y, y + 9.6, "pi_wh_drum", "pi_wh_drum_top", "pi_wh_drum_label" if cz > 8 else None)
            out += round_y(cx, cz, 3.45, y + 3.1, y + 3.5, "pi_wh_drum") + round_y(cx, cz, 3.45, y + 6.2, y + 6.6, "pi_wh_drum")
        top = y + 9.6
        out += _straps(y + 1.0, y + 8.4, 1, ring=True)
        wrapped = False
    elif kind == "ingot":
        out.append(box(1.4, y, 1.4, 14.6, y + 7.2, 14.6, {"*": "pi_wh_ingot_side", "north": "pi_wh_ingot_end", "south": "pi_wh_ingot_end"}))
        top = y + 7.2
        out += _straps(y, top, 2, steel=True, lo=1.4, hi=14.6)
        wrapped = False
    if wrapped and out:
        h0, h1 = y - 0.3, top - 0.35
        out.append(box(0.85, h0, 0.85, 15.15, h1, 15.15, {"north": FILM, "south": FILM, "east": FILM, "west": FILM}))
        out.append(box(0.95, h1 - 0.6, 0.95, 15.05, top + 0.12, 15.05, {"up": FILM, "north": FILM, "south": FILM, "east": FILM, "west": FILM}))
    return out


def _straps(y0, y1, n, steel=False, ring=False, lo=1.1, hi=14.9):
    """Straps over a load (n each way over the top and down its sides, 0.1 proud of it), or one ring band round a
    group of drums at each of two heights."""
    tex = "pi_wh_band" if steel else "pi_wh_strap"
    out = []
    if ring:
        for y in (y0, y1):
            out += [box(1.15, y, 1.15, 14.85, y + 0.8, 1.35, tex), box(1.15, y, 14.65, 14.85, y + 0.8, 14.85, tex),
                    box(1.15, y, 1.35, 1.35, y + 0.8, 14.65, tex), box(14.65, y, 1.35, 14.85, y + 0.8, 14.65, tex)]
        return out
    a, b = lo - 0.2, hi + 0.2
    for k in range(n):
        c = 5.0 + k * 6.0 if n == 2 else 8.0
        out += [box(c - 0.45, y1, a, c + 0.45, y1 + 0.12, b, tex), box(c - 0.45, y0, a, c + 0.45, y1 + 0.12, lo, tex),
                box(c - 0.45, y0, hi, c + 0.45, y1 + 0.12, b, tex)]
        out += [box(a, y1 + 0.13, c - 0.45, b, y1 + 0.25, c + 0.45, tex), box(a, y0, c - 0.45, lo, y1 + 0.25, c + 0.45, tex),
                box(hi, y0, c - 0.45, b, y1 + 0.25, c + 0.45, tex)]
    return out


# ================================================================================================ the pallet rack
BEAM_TOP = 1.6          # a pallet place's beams' top (its pallet stands here); a ground place's pallet on the floor


def rack(left, right, ground, top, labels=1):
    """One pallet place of selective pallet racking (front to the aisle, +z). Blue uprights (front and back, their
    beam slots punched) at a bay's ends (left / right: this place begins / ends a bay), braced in the frame between
    them; orange step beams front and back under the pallet with a wire mesh deck between (not at the ground place,
    whose pallet stands on the floor), the location label on the front beam (labels 2: also the ground place's
    under it, on the beam over it); at the ground, the uprights' base plates and the yellow column guards in front.
    The place's pallet and load, and the labels' writing, are drawn by the game."""
    out = []
    xa = 1.4 if left else 0.0
    xb = 14.6 if right else 16.0
    for on, x0 in ((left, 0.0), (right, 14.6)):
        if not on:
            continue
        x1 = x0 + 1.4
        post = {"*": BLUE, "south": BLUE_FACE, "north": BLUE_FACE}
        for z0 in (0.0, 14.6):
            out.append(box(x0, 0.0, z0, x1, 16.0 if not top else 15.6, z0 + 1.4, post))
            if top:
                out.append(box(x0 - 0.1 if x0 > 0 else x0, 15.6, z0 - 0.0, x1 + (0.1 if x0 == 0 else 0), 15.9, z0 + 1.4, STEEL))
            if ground:
                out.append(box(x0 - 0.3 if x0 > 0 else x0, 0.0, max(0.0, z0 - 0.3), min(16.0, x1 + 0.3), 0.25, min(16.0, z0 + 1.7), STEEL))
        # The frame's bracing (in the plane between the posts): a horizontal and a diagonal, turned the other way on
        # every other level so the lacing zig-zags up the frame.
        xm0, xm1 = x0 + 0.45, x0 + 0.95
        out.append(box(xm0, 7.6, 1.4, xm1, 8.4, 14.6, BLUE))
        d = 9.33
        ang = 45 if ground else -45
        out.append(box(xm0 + 0.05, 7.6, 8.0 - d, xm1 - 0.05, 8.4, 8.0 + d, BLUE, rot_x(xm0, 8.0, 8.0, ang)))
        if ground:
            # The column guard round the front upright's foot.
            gx0, gx1 = (x0 - 0.2, x1 + 0.5) if x0 == 0 else (x0 - 0.5, x1 + 0.2)
            out += [box(max(0.0, gx0), 0.0, 16.0, min(16.0, gx1), 5.0, 16.4, YELLOW),
                    box(max(0.0, gx0), 0.0, 14.4, max(0.0, gx0) + 0.3, 5.0, 16.0, YELLOW) if x0 == 0 else
                    box(min(16.0, gx1) - 0.3, 0.0, 14.4, min(16.0, gx1), 5.0, 16.0, YELLOW)]
    if ground:
        # The ground place's floor marks: a yellow corner at each corner of its pallet's footprint.
        for cx, cz in ((1.6, 1.6), (14.4, 1.6), (1.6, 14.4), (14.4, 14.4)):
            sx = 1 if cx < 8 else -1
            sz = 1 if cz < 8 else -1
            out += [box(min(cx, cx + 2.4 * sx), 0.0, min(cz, cz + 0.5 * sz), max(cx, cx + 2.4 * sx), 0.1, max(cz, cz + 0.5 * sz), YELLOW),
                    box(min(cx, cx + 0.5 * sx), 0.0, min(cz + 0.5 * sz, cz + 2.4 * sz), max(cx, cx + 0.5 * sx), 0.1, max(cz + 0.5 * sz, cz + 2.4 * sz), YELLOW)]
    if not ground:
        out += [box(xa, 0.0, 14.8, xb, BEAM_TOP, 15.6, {"*": BEAM, "south": BEAM_FACE}),
                box(xa, 0.0, 0.4, xb, BEAM_TOP, 1.2, {"*": BEAM, "north": BEAM_FACE}),
                box(xa, 1.15, 1.2, xb, 1.45, 14.8, DECK)]
        for on, xc in ((left, 1.4), (right, 14.3)):
            if on:
                out += [box(xc, 0.0, 14.7, xc + 0.3, 2.4, 15.7, BEAM), box(xc, 0.0, 0.3, xc + 0.3, 2.4, 1.3, BEAM)]
        # The location label (two: the ground place's too).
        if labels == 2:
            out += [box(3.4, 0.25, 15.6, 7.6, 1.35, 15.7, {"south": "pi_wh_loc_label_dn!"}),
                    box(8.4, 0.25, 15.6, 12.6, 1.35, 15.7, {"south": "pi_wh_loc_label_up!"})]
        else:
            out.append(box(5.9, 0.25, 15.6, 10.1, 1.35, 15.7, {"south": "pi_wh_loc_label_up!"}))
    return out


# ================================================================================================ the floor
def lane_model(mask, code):
    """A lane tile: the floor block, its top the sealed concrete with the drones' navigation code and the yellow
    edge lines."""
    return [box(0, 0, 0, 16, 16, 16, {"*": "pi_wh_lane_side", "up": f"pi_wh_lane_{mask}_{code}!"})]


def pack_spot_model():
    return [box(0, 0, 0, 16, 16, 16, {"*": "pi_wh_lane_side", "up": "pi_wh_pack_spot!"})]


# ================================================================================================ the charging dock
def charge_dock():
    """A drone's charging dock (the space a drone parks in, against a wall at the back): the steel floor plate with
    its chevrons and CHARGE, the two spring contact shoes the drone's contacts land on; the charger cabinet hung on
    the wall at the back on the right (status bar, display, vents), its conduit up the wall."""
    out = [box(1.0, 0.0, 0.6, 15.0, 0.35, 15.4, {"*": STEEL, "up": "pi_wh_dock_plate!"})]
    for x0 in (5.0, 9.0):
        out += [box(x0, 0.35, 1.2, x0 + 2.0, 0.75, 3.0, BLACK), box(x0 + 0.2, 0.75, 1.4, x0 + 1.8, 0.95, 2.8, "pi_wh_copper")]
    # The cabinet: body, its front, the cable from it down into the plate.
    out += [box(9.2, 7.2, 0.0, 15.4, 15.6, 2.8, {"*": "pi_wh_desk", "south": "pi_wh_charger_front!"}),
            box(9.0, 7.0, 0.0, 15.6, 7.4, 3.0, GRAPHITE), box(9.0, 15.4, 0.0, 15.6, 15.8, 3.0, GRAPHITE),
            box(11.6, 0.35, 0.3, 12.6, 7.0, 1.3, BLACK), box(11.6, 0.35, 1.3, 12.6, 0.75, 1.9, BLACK)]
    out += [box(11.7, 15.8, 0.3, 12.9, 16.0, 1.5, STEEL)]           # the conduit up the wall from its top
    return out


def charge_dock_upper():
    """The dock's conduit on up the wall to the ceiling (the block over the dock)."""
    return [box(11.7, 0.0, 0.3, 12.9, 16.0, 1.5, STEEL), box(11.5, 7.6, 0.0, 13.1, 8.4, 1.7, GRAPHITE)]


def _loc_label(text, arrow):
    """A rack location label (live text in the game; the mockup's own): white, the location large, an arrow to its
    place (up: the pallet on this beam; down: the ground place under it), the barcode."""
    img = _img(256, 68, (250, 250, 246))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 67], outline=(30, 30, 32, 255), width=3)
    pts = [(22, 14), (10, 34), (34, 34)] if arrow == "up" else [(22, 54), (10, 34), (34, 34)]
    d.polygon(pts, fill=(30, 30, 32, 255))
    _t(d, (44, 22), text, 30, (20, 20, 22), anchor="lm", width=200)
    _barcode(d, 44, 42, 246, 60, hash(text) & 0xffff)
    return _sq(img)


# ================================================================================================ the palletizing station
def pallet_station(stack=7):
    """The palletizing station (one block wide, two deep, two high): at the back the pallet magazine (a yellow steel
    frame round a stack of empty pallets, the separator arms holding the stack while the bottom pallet is dispensed,
    the chain conveyor under it); at the front the build spot (a low chain bed the pallet is pushed out onto and
    stacked on, its photo-eye and the light curtain posts at its open corners); the small control box with its
    lamps and PALLETS count. stack: empty pallets in the magazine (as stored)."""
    out = []
    # The magazine's frame: four posts, the top ring, the guides.
    for x0 in (0.2, 14.6):
        for z0 in (0.2, 14.6):
            out.append(box(x0, 0.0, z0, x0 + 1.2, 30.0, z0 + 1.2, YELLOW))
            out.append(box(x0 - 0.1 if x0 > 1 else x0, 0.0, z0, x0 + 1.3 if x0 < 1 else x0 + 1.2, 0.3, z0 + 1.4, STEEL))
    out += [box(0.2, 28.8, 1.4, 1.4, 30.0, 14.6, YELLOW), box(14.6, 28.8, 1.4, 15.8, 30.0, 14.6, YELLOW),
            box(1.4, 28.8, 0.2, 14.6, 30.0, 1.4, YELLOW), box(1.4, 28.8, 14.6, 14.6, 30.0, 15.8, YELLOW)]
    # The dispensing chain conveyor under the stack (two strands along z), the motor at the back.
    for x0 in (2.2, 12.2):
        out += [box(x0, 0.0, 0.8, x0 + 1.6, 1.3, 16.0, {"*": GRAPHITE, "up": "pi_wh_strap"})]
    out += [box(3.8, 0.2, 1.0, 12.2, 0.9, 2.2, GRAPHITE)] + round_x(1.4, 15.0, 0.9, 1.2, 3.6, MACHINE)
    # The separator arms (one each side, pneumatic cylinders on the posts) and the stack itself.
    for x0, x1 in ((0.0, 1.0), (15.0, 16.0)):
        out += [box(x0, 3.6, 5.0, x1, 4.6, 11.0, GRAPHITE)] + round_y((x0 + x1) / 2, 4.0, 0.5, 4.6, 8.6, CHROME)
    y = 1.3
    for k in range(stack):
        out += shifted(pallet(), 0, y + k * (PALLET_H + 0.02), 0)
    # The control box on the front left post, its lamps.
    out += [box(-0.2, 12.0, 14.6, 0.2, 16.6, 16.0, MACHINE)]
    # The build spot: the turntable (its ring base, the turning plate) with its own two chain strands on it, which
    # take the pallet off the magazine's conveyor; the guard ring round it.
    out += PP.round_y(8.0, 24.0, 7.4, 0.0, 0.7, STEEL)
    for x0 in (2.2, 12.2):
        out += [box(x0, 0.7, 17.2, x0 + 1.6, 1.3, 30.8, {"*": GRAPHITE, "up": "pi_wh_strap"})]
    out += [box(3.8, 0.7, 23.2, 12.2, 1.1, 24.8, GRAPHITE)]
    # The light curtain's posts at the build spot's open front corners, and its photo-eye.
    for x0 in (0.0, 15.0):
        out += [box(x0, 0.0, 30.6, x0 + 1.0, 18.0, 31.6, {"*": BLACK, "south": GRAPHITE}), box(x0 - 0.1 if x0 else x0, 0.0, 30.4, x0 + 1.1 if not x0 else x0 + 1.0, 0.3, 31.8, STEEL)]
    out += [box(-0.2, 3.0, 22.0, 0.2, 4.6, 23.6, BLACK)]
    return out


# ================================================================================================ the intake case packer
CONV_Y = 11.5          # the conveyor's rollers' top


def intake():
    """The Intake Case Packer (three wide, two deep, two high): the machine at the back takes in whatever is piped
    into it (the three flanged ports on its back, the stainless hopper on top), erects cases from its magazine of
    flat blanks, fills them, folds and tapes them shut and labels them; it pushes them out of its left end onto the
    accumulation roller conveyor along the front, where they queue to the stop at the right end for a Stacker Drone
    to pick. The working section shows through its guard windows; the HMI on its swing arm, the stack light, E-stop."""
    out = []
    # ---- the machine (x 1..47, z 1..15)
    out += [box(1.0, 0.0, 1.0, 47.0, 2.0, 15.0, GRAPHITE)]
    for x in (2.0, 23.0, 44.0):
        for z in (2.0, 12.0):
            out += round_y(x + 1.0, z + 1.0, 0.8, 0.0, 0.8, STEEL)
    out += [box(1.0, 2.0, 1.0, 47.0, 10.0, 15.0, {"*": MACHINE, "south": MACHINE})]
    # Its lower doors' seams and handles.
    for x in (12.6, 24.0, 35.4):
        out.append(box(x - 0.1, 2.4, 15.0, x + 0.1, 9.6, 15.1, MACHINE_DARK))
    for x in (11.0, 22.6, 34.0, 45.4):
        out.append(box(x, 5.2, 15.0, x + 0.6, 6.8, 15.5, CHROME))
    # The working section: the frame round its windows, the windows.
    out += [box(1.0, 10.0, 1.0, 47.0, 24.0, 2.0, MACHINE), box(1.0, 24.0, 1.0, 47.0, 26.0, 15.0, MACHINE),
            box(1.0, 10.0, 2.0, 2.0, 24.0, 15.0, MACHINE), box(46.0, 10.0, 2.0, 47.0, 24.0, 15.0, MACHINE)]
    for x0, x1 in ((2.0, 17.0), (17.6, 32.4), (33.0, 46.0)):
        out += [box(x0, 10.0, 14.0, x1, 10.8, 15.0, MACHINE_DARK), box(x0, 23.2, 14.0, x1, 24.0, 15.0, MACHINE_DARK),
                box(x0 + 0.1, 10.8, 14.4, x1 - 0.1, 23.2, 14.6, {"south": GUARD, "north": GUARD})]
    for x in (17.0, 32.4):
        out.append(box(x, 10.0, 14.0, x + 0.6, 24.0, 15.0, MACHINE_DARK))
    out += [box(2.0, 10.0, 2.0, 46.0, 10.6, 14.0, STAINLESS)]           # the bed inside
    # Inside: the blank magazine (left), a case being formed (middle, its flaps open), the tape head over a case
    # being closed (right), the infeed chute down into the open case.
    out += [box(3.0, 10.6, 3.0, 9.0, 19.0, 12.0, {"*": "pi_wh_blanks", "up": "pi_wh_carton", "south": "pi_wh_carton"}),
            box(2.6, 10.6, 2.6, 3.0, 21.0, 12.4, STEEL), box(9.0, 10.6, 2.6, 9.4, 21.0, 12.4, STEEL)]
    out += [box(20.6, 10.6, 4.4, 27.4, 16.1, 11.2, {"*": "pi_wh_carton", "up": "pi_wh_carton"})]
    out += [box(20.6, 16.1, 4.4, 27.4, 19.0, 4.6, "pi_wh_carton"), box(20.6, 16.1, 11.0, 27.4, 19.0, 11.2, "pi_wh_carton")]
    out += [box(22.0, 19.0, 6.2, 26.0, 24.0, 9.4, STAINLESS)]          # the chute down from the hopper
    out += [box(36.4, 10.6, 4.4, 43.2, 16.1, 11.2, {"*": "pi_wh_carton", "up": "pi_wh_carton_top"})]
    out += [box(37.8, 18.2, 6.4, 41.8, 22.0, 9.2, GRAPHITE)] + round_x(20.0, 7.8, 1.5, 38.6, 41.0, "pi_wh_tape")
    out += [box(39.4, 16.2, 7.4, 40.2, 18.2, 8.2, STEEL), box(30.0, 23.0, 3.0, 45.0, 24.0, 4.0, STEEL)]
    # The hopper on top (stainless, a stepped funnel) and the pipe ports on the back.
    out += [box(18.0, 26.0, 3.0, 30.0, 27.0, 13.0, STAINLESS), box(17.0, 27.0, 2.0, 31.0, 30.0, 14.0, STAINLESS),
            box(16.6, 30.0, 1.6, 31.4, 30.8, 14.4, STAINLESS)]
    for x in (8.0, 24.0, 40.0):
        out += round_z(x, 18.0, 2.6, 0.0, 1.0, STEEL) + round_z(x, 18.0, 1.9, -0.8, 0.2, STEEL)
    # The exit: the case pusher's slot at the left end of the front, onto the conveyor.
    out += [box(2.0, 11.4, 15.0, 10.0, 11.6, 16.6, STAINLESS)]
    # The stack light on the top front right; the E-stop on the frame; the HMI on its arm (out over the conveyor).
    out += round_y(44.6, 13.0, 0.5, 26.0, 26.8, GRAPHITE)
    for k, lamp in enumerate(("pi_wh_lamp_red", "pi_wh_lamp_amber", "pi_wh_lamp_green", "pi_wh_lamp_blue")[::-1]):
        out += PP.round_y(44.6, 13.0, 0.75, 26.8 + k * 1.3, 28.0 + k * 1.3, lamp)
    out += [box(45.2, 15.0, 15.0, 46.8, 16.6, 15.6, YELLOW)] + round_z(46.0, 15.8, 0.55, 15.6, 16.4, "pi_wh_estop")
    out += [box(1.4, 21.0, 15.0, 2.6, 22.2, 19.0, GRAPHITE), box(1.6, 21.2, 19.0, 2.4, 22.0, 19.8, GRAPHITE)]
    out += [box(1.2, 18.0, 19.8, 9.0, 24.0, 20.6, {"*": GRAPHITE, "south": "pi_wh_hmi_intake!"})]
    # ---- the accumulation conveyor along the front (z 17..31)
    for z0 in (17.0, 30.0):
        out += [box(0.0, 9.6, z0, 48.0, 11.2, z0 + 1.0, "pi_wh_conv_frame")]
        out += [box(0.0, 11.2, z0 + (0.6 if z0 < 20 else 0.0), 48.0, 13.4, z0 + (1.0 if z0 < 20 else 0.4), "pi_wh_conv_frame")]
    out += [box(0.0, 10.4, 18.0, 48.0, CONV_Y, 30.0, {"*": GRAPHITE, "up": "pi_wh_rollers"})]
    for x in (1.0, 23.0, 46.0):
        for z in (17.2, 30.0):
            out += [box(x, 0.0, z, x + 1.0, 9.6, z + 0.8, "pi_wh_conv_frame"), box(x - 0.4, 0.0, z - 0.3, x + 1.4, 0.3, z + 1.1, STEEL)]
        out += [box(x, 3.0, 18.0, x + 1.0, 3.8, 30.0, "pi_wh_conv_frame")]
    for x in (12.0, 28.0, 40.0):
        out += [box(x, 8.4, 20.0, x + 3.0, 10.4, 23.0, BLACK)]           # the zone controllers under the rollers
    # The end stop and the photo-eye with its reflector at the pick end.
    out += [box(47.0, CONV_Y, 18.0, 48.0, 15.0, 30.0, YELLOW)]
    out += [box(44.0, 11.2, 30.4, 45.0, 14.0, 31.0, BLACK), box(44.0, 11.2, 17.0, 45.0, 14.0, 17.6, "pi_wh_lamp_red")]
    return out


# ================================================================================================ the control terminal
def terminal():
    """The Warehouse Control Terminal (three wide): a sit-stand desk with its two lifting columns, the cable tray,
    the three monitors on their arm (STOCK, FLEET, ORDERS live), keyboard and mouse, the barcode scanner in its
    cradle, the label printer, the desk phone."""
    out = []
    top = 12.2
    out += [box(0.6, top - 0.8, 0.8, 47.4, top, 15.4, {"*": GRAPHITE, "up": "pi_wh_desk"})]
    for x in (4.0, 42.0):
        out += [box(x, 0.0, 2.0, x + 2.0, 0.6, 14.0, GRAPHITE), box(x + 0.3, 0.6, 7.0, x + 1.7, 7.0, 9.4, GRAPHITE),
                box(x + 0.5, 7.0, 7.2, x + 1.5, top - 0.8, 9.2, STEEL)]
    out += [box(7.0, 9.0, 1.4, 41.0, 9.6, 6.0, GRAPHITE), box(7.0, 9.6, 1.4, 41.0, 10.8, 1.8, GRAPHITE)]
    # The monitor arm: post, bar, three screens (the outer two turned in).
    out += [box(23.2, top, 2.0, 24.8, top + 0.4, 5.0, GRAPHITE), box(23.5, top + 0.4, 2.8, 24.5, 21.4, 3.8, STEEL),
            box(7.0, 19.6, 3.0, 41.0, 20.4, 3.8, STEEL)]
    scr = [("pi_wh_scr_fleet", 3.6, 0), ("pi_wh_scr_stock", 17.2, 0), ("pi_wh_scr_orders", 30.8, 0)]
    for tex, x0, _ in scr:
        mon = [box(x0, 15.0, 4.0, x0 + 13.6, 24.0, 4.8, GRAPHITE), box(x0 + 0.3, 15.3, 4.8, x0 + 13.3, 23.7, 4.9, {"south": tex + "!"})]
        if x0 < 10:
            mon = [m + (rot_y(x0 + 13.6, 15.0, 4.0, 22.5),) for m in mon]
        elif x0 > 30:
            mon = [m + (rot_y(x0, 15.0, 4.0, -22.5),) for m in mon]
        out += mon
    # Keyboard, mouse; the scanner in its cradle; the label printer; the phone.
    out += [box(17.6, top, 8.4, 30.4, top + 0.5, 12.0, {"*": GRAPHITE, "up": "pi_keyboard!"}), box(32.0, top, 9.4, 33.4, top + 0.5, 11.2, GRAPHITE)]
    out += [box(38.0, top, 9.0, 40.2, top + 0.6, 11.2, GRAPHITE), box(38.5, top + 0.6, 9.4, 39.7, top + 3.0, 10.4, YELLOW),
            box(38.5, top + 3.0, 9.4, 39.7, top + 4.2, 12.6, YELLOW), box(38.6, top + 3.1, 12.6, 39.6, top + 4.1, 12.8, BLACK)]
    out += [box(42.0, top, 3.0, 47.0, top + 3.6, 8.6, {"*": "pi_wh_desk", "up": GRAPHITE}), box(42.6, top + 1.6, 8.6, 46.4, top + 1.8, 9.6, "pi_paper")]
    out += [box(2.0, top, 7.0, 6.4, top + 1.2, 11.0, {"*": GRAPHITE, "up": GRAPHITE}), box(2.2, top + 1.2, 7.2, 3.4, top + 1.8, 10.8, GRAPHITE)]
    return out


# ================================================================================================ the pallet drone
# A very-narrow-aisle turret truck run by itself: the chassis (battery, drive unit, the safety scanners, the blue spot
# projector, the beacon, the rear screen), the triplex mast (outer, middle, inner stages), the carriage running up
# the inner stage, and on it the turret head that turns its forks to either side or ahead and traverses across the
# front, so it never has to turn in an aisle. Parts (pd_*), each drawn by the game at its pose:
#   pd_chassis + pd_mast_outer   fixed
#   pd_mast_mid                  up by mid(lift)
#   pd_mast_inner                up by mid(lift) * 2
#   pd_carriage                  up by lift (its traverse rail)
#   pd_head                      up by lift, across by traverse, turned about (HEAD_X, HEAD_Z) by its angle
PD_FREE = 22.0          # the carriage's free lift on the inner stage before the stages move
PD_STAGE = 26.0         # each stage's travel
PD_LIFT = PD_FREE + 2 * PD_STAGE      # the forks' whole lift: 74 px, the fifth rack level
HEAD_X, HEAD_Z = 8.0, 19.3            # the turret's turning axis (forks ahead)
FORK_Y = 0.5                          # the forks' blades' underside over the carriage's bottom


def pd_stage(lift):
    """How far the middle stage is up for a lift (the inner stage is up twice that; free lift first)."""
    return max(0.0, min(PD_STAGE, (lift - PD_FREE) / 2.0))


def pd_chassis():
    out = []
    # Lower frame (graphite) with the drive wheels' arches; the drive and load wheels.
    out += rrect_y(1.0, 0.5, 15.0, 6.0, 0.8, 4.0, 1.8, GRAPHITE)[:1] + [box(1.0, 0.9, 2.4, 15.0, 3.9, 6.0, GRAPHITE)]
    out += [box(1.0, 0.8, 6.0, 15.0, 4.0, 8.2, GRAPHITE), box(2.8, 0.8, 8.2, 13.2, 4.0, 10.6, GRAPHITE), box(1.0, 0.8, 10.6, 15.0, 4.0, 12.4, GRAPHITE)]
    out += round_x(1.3, 9.4, 1.3, 1.2, 2.8, {"*": "pi_wh_tyre", "east": STEEL, "west": STEEL})
    out += round_x(1.3, 9.4, 1.3, 13.2, 14.8, {"*": "pi_wh_tyre", "east": STEEL, "west": STEEL})
    out += [box(1.0, 0.8, 0.5, 15.0, 1.0, 2.4, GRAPHITE)]
    # The upper body (yellow), its top plate, the side panels, the rear screen.
    out += rrect_y(1.2, 0.8, 14.8, 12.2, 4.0, 7.0, 1.6, YELLOW)
    out += [box(2.0, 7.0, 1.6, 14.0, 7.3, 11.6, GRAPHITE)]
    out += [box(1.1, 4.3, 3.4, 1.2, 6.7, 11.0, {"west": "pi_wh_pd_decal!"}), box(14.8, 4.3, 3.4, 14.9, 6.7, 11.0, {"east": "pi_wh_pd_decal!"})]
    out += [box(4.4, 4.4, 0.7, 11.6, 6.8, 0.8, {"north": "pi_wh_pd_screen!"})]
    # The status light strip round the top edge (lit; its colour by what it is doing, in the game).
    out += [box(1.05, 6.75, 2.4, 1.2, 7.05, 11.6, LIGHT), box(14.8, 6.75, 2.4, 14.95, 7.05, 11.6, LIGHT), box(2.4, 6.75, 0.65, 13.6, 7.05, 0.8, LIGHT)]
    # The rear bumper (rubber, hazard striped), its contacts under it for the dock.
    out += [box(1.4, 1.0, 0.0, 14.6, 2.6, 0.6, {"*": RUBBER, "north": HAZARD})]
    out += [box(5.0, 0.25, 0.8, 7.0, 0.8, 2.6, "pi_wh_copper"), box(9.0, 0.25, 0.8, 11.0, 0.8, 2.6, "pi_wh_copper")]
    # The E-stops (red mushrooms on yellow housings) on both sides at the back; the rear scanners and the beacon.
    for x0, x1, xs in ((0.5, 1.2, 0.2), (14.8, 15.5, 15.5)):
        out.append(box(x0, 5.0, 1.8, x1, 6.4, 3.2, YELLOW))
        out.append(box(xs - (0.3 if xs < 8 else 0.0), 5.25, 2.05, xs + (0.0 if xs < 8 else 0.3), 6.15, 2.95, "pi_wh_estop"))
    out += round_y(2.6, 2.2, 0.9, 7.3, 8.9, "pi_wh_lidar") + round_y(13.4, 2.2, 0.9, 7.3, 8.9, "pi_wh_lidar")
    out += round_y(8.0, 3.0, 0.9, 7.3, 7.9, GRAPHITE) + PP.round_y(8.0, 3.0, 0.75, 7.9, 9.4, "pi_wh_beacon")
    # The mast's base (graphite plate), the load wheels under it, the front scanner and the blue spot projector.
    out += [box(1.4, 0.8, 12.2, 14.6, 2.0, 14.6, GRAPHITE), box(5.6, 0.8, 14.6, 10.4, 2.4, 15.4, "pi_wh_lidar")]
    out += round_x(0.9, 14.6, 0.9, 2.4, 4.4, {"*": "pi_wh_tyre", "east": STEEL, "west": STEEL})
    out += round_x(0.9, 14.6, 0.9, 11.6, 13.6, {"*": "pi_wh_tyre", "east": STEEL, "west": STEEL})
    out += round_z(8.0, 2.9, 0.45, 14.6, 15.4, "pi_wh_lamp_blue")
    return out


def pd_mast_outer():
    out = []
    for x0 in (2.0, 12.4):
        out += [box(x0, 2.0, 12.4, x0 + 1.6, 31.0, 13.0, MAST), box(x0, 2.0, 15.0, x0 + 1.6, 31.0, 15.4, MAST),
                box(x0 + (1.0 if x0 < 8 else 0.0), 2.0, 13.0, x0 + (1.6 if x0 < 8 else 0.6), 31.0, 15.0, MAST)]
    out += [box(3.6, 29.8, 12.4, 12.4, 31.0, 13.2, MAST), box(3.6, 2.0, 12.4, 12.4, 3.2, 13.2, MAST)]
    out += round_y(8.0, 12.0, 0.9, 2.0, 15.0, MAST)                     # the lift cylinder's barrel
    out += [box(6.2, 31.0, 12.4, 9.8, 32.4, 13.6, BLACK), box(6.6, 31.3, 13.6, 9.4, 32.1, 13.7, "pi_wh_lidar")]   # the 3D camera
    out += [box(3.7, 4.0, 12.5, 4.1, 29.6, 12.9, "pi_wh_hose"), box(11.9, 4.0, 12.5, 12.3, 29.6, 12.9, "pi_wh_hose")]
    return out


def pd_mast_mid():
    out = []
    for x0 in (3.6, 11.1):
        out += [box(x0, 2.4, 13.1, x0 + 1.3, 30.4, 13.5, MAST), box(x0, 2.4, 14.6, x0 + 1.3, 30.4, 15.0, MAST),
                box(x0 + (0.8 if x0 < 8 else 0.0), 2.4, 13.5, x0 + (1.3 if x0 < 8 else 0.5), 30.4, 14.6, MAST)]
    out += [box(4.9, 29.4, 13.1, 11.1, 30.4, 13.9, MAST)]
    out += round_y(8.0, 12.0, 0.55, 15.0, 29.4, CHROME)                # the lift cylinder's rod
    return out


def pd_mast_inner():
    out = []
    for x0 in (4.9, 10.0):
        out += [box(x0, 2.8, 13.6, x0 + 1.1, 29.8, 14.6, MAST)]
    out += [box(6.0, 28.8, 13.6, 10.0, 29.8, 14.4, MAST)]
    out += round_x(28.6, 14.1, 0.8, 6.6, 9.4, STEEL)                   # the chain sheave
    out += [box(6.9, 6.0, 14.6, 7.5, 28.6, 14.9, "pi_wh_hose"), box(8.5, 6.0, 14.6, 9.1, 28.6, 14.9, "pi_wh_hose")]   # chains
    return out


def pd_carriage():
    """The carriage (its bottom at 0 here): the plate riding the inner stage on its rollers, the traverse rail
    across the front the turret head runs on."""
    out = [box(3.0, 0.0, 14.8, 13.0, 6.0, 16.0, GRAPHITE), box(4.2, 1.0, 13.9, 5.0, 5.0, 14.8, STEEL), box(11.0, 1.0, 13.9, 11.8, 5.0, 14.8, STEEL)]
    out += [box(-0.4, 1.0, 16.0, 16.4, 4.6, 17.2, {"*": GRAPHITE, "up": STEEL}), box(-0.6, 0.6, 16.0, -0.4, 5.0, 17.4, YELLOW),
            box(16.4, 0.6, 16.0, 16.6, 5.0, 17.4, YELLOW)]
    return out


def pd_head():
    """The turret head (forks ahead, +z; the carriage's bottom at 0): the traverse saddle on the rail, the turntable,
    the fork carriage with its load backrest grille, the two forks."""
    out = [box(4.4, 0.4, 17.2, 11.6, 5.2, 18.2, GRAPHITE)]
    out += round_y(HEAD_X, HEAD_Z, 2.4, 0.0, 1.0, STEEL)
    out += [box(3.0, 0.2, 18.4, 13.0, 3.6, 19.4, GRAPHITE)]                         # the fork carriage bars
    # The load backrest: frame and bars.
    out += [box(2.0, 3.6, 18.6, 14.0, 4.4, 19.2, MAST), box(2.0, 11.6, 18.6, 14.0, 12.4, 19.2, MAST),
            box(2.0, 4.4, 18.6, 2.8, 11.6, 19.2, MAST), box(13.2, 4.4, 18.6, 14.0, 11.6, 19.2, MAST)]
    for x in (5.0, 7.4, 9.8):
        out.append(box(x, 4.4, 18.7, x + 0.6, 11.6, 19.1, MAST))
    # The forks: shank down the carriage, the blade out under the load.
    for x0 in (4.3, 10.3):
        out += [box(x0, FORK_Y, 19.4, x0 + 1.4, 5.6, 20.4, FORK), box(x0, FORK_Y, 20.4, x0 + 1.4, FORK_Y + 0.9, 34.0, FORK),
                box(x0 + 0.2, FORK_Y, 34.0, x0 + 1.2, FORK_Y + 0.5, 34.6, FORK)]
    return out


# ================================================================================================ the stacker drone
# A mobile robot arm on an omnidirectional base: the arm stacks cases on a pallet (vacuum gripper; clamp jaws for
# drums), the film mast at its back carries the stretch film it wraps a finished pallet in, driving round it.
# Parts (sd_*): sd_base fixed; sd_j1 turned about y at the arm's base; sd_upper about x at the shoulder; sd_fore
# about x at the elbow; sd_wrist about x at the wrist; sd_film up the mast.
SD_BASE = (8.0, 6.4, 9.6)        # the arm's base: x, its top, z
SD_SHOULDER = 9.8                 # the shoulder's axis height
SD_UPPER = 11.0                   # the upper arm's length
SD_FORE = 10.0                    # the forearm's length (elbow to wrist)


def sd_base():
    out = []
    out += [box(2.6, 0.8, 1.2, 13.4, 3.2, 14.8, GRAPHITE), box(1.2, 1.4, 4.8, 14.8, 3.2, 11.2, GRAPHITE)]
    for x0, x1 in ((0.8, 2.5), (13.5, 15.2)):
        for cz in (3.0, 13.0):
            out += round_x(1.4, cz, 1.4, x0, x1, {"*": "pi_wh_tyre", "east": "pi_wh_mecanum", "west": "pi_wh_mecanum"})
    out += rrect_y(0.8, 0.8, 15.2, 15.2, 3.2, 4.8, 1.8, YELLOW)                 # the deck (fenders over the wheels)
    out += [box(1.6, 1.2, 0.6, 14.4, 2.4, 1.2, {"*": RUBBER, "north": HAZARD}), box(1.6, 1.2, 14.8, 14.4, 2.4, 15.4, {"*": RUBBER, "south": HAZARD})]
    out += [box(3.0, 4.75, 0.75, 13.0, 5.0, 0.85, LIGHT), box(3.0, 4.75, 15.15, 13.0, 5.0, 15.25, LIGHT)]
    out += [box(0.75, 4.75, 3.0, 0.85, 5.0, 13.0, LIGHT), box(15.15, 4.75, 3.0, 15.25, 5.0, 13.0, LIGHT)]
    out += [box(0.75, 3.3, 4.0, 0.8, 4.7, 12.0, {"west": "pi_wh_sd_decal!"}), box(15.2, 3.3, 4.0, 15.25, 4.7, 12.0, {"east": "pi_wh_sd_decal!"})]
    # Scanners at two corners, the E-stop, the charging contacts under the back.
    out += round_y(2.4, 13.6, 0.85, 4.8, 6.2, "pi_wh_lidar") + round_y(13.6, 2.4, 0.85, 4.8, 6.2, "pi_wh_lidar")
    out += [box(11.4, 4.8, 1.4, 13.0, 5.6, 3.0, YELLOW)] + round_y(12.2, 2.2, 0.6, 5.6, 6.4, "pi_wh_estop")
    out += [box(5.0, 0.25, 1.4, 7.0, 0.8, 3.2, "pi_wh_copper"), box(9.0, 0.25, 1.4, 11.0, 0.8, 3.2, "pi_wh_copper")]
    # The arm's pedestal; the film mast at the back left with its cap.
    out += round_y(SD_BASE[0], SD_BASE[2], 2.6, 4.8, SD_BASE[1], GRAPHITE)
    out += [box(1.6, 4.8, 1.6, 2.8, 20.6, 2.8, MAST), box(1.4, 20.6, 1.4, 3.0, 21.2, 3.0, GRAPHITE),
            box(1.45, 4.8, 1.45, 2.95, 5.6, 2.95, GRAPHITE)]
    return out


def sd_j1():
    """The arm's turning base and its shoulder housing (the shoulder's axis along x at SD_SHOULDER)."""
    x, y0, z = SD_BASE
    out = round_y(x, z, 2.2, y0, y0 + 1.4, YELLOW)
    out += [box(x - 2.0, y0 + 1.4, z - 1.6, x + 1.4, SD_SHOULDER + 1.6, z + 1.6, YELLOW)]
    out += round_x(SD_SHOULDER, z, 1.8, x - 3.0, x - 1.9, GRAPHITE)
    return out


def sd_upper():
    """The upper arm (upright here: shoulder at SD_SHOULDER, elbow SD_UPPER above), its joint caps."""
    x, _, z = SD_BASE
    y0, y1 = SD_SHOULDER, SD_SHOULDER + SD_UPPER
    out = [box(x - 0.6, y0, z - 1.1, x + 1.6, y1, z + 1.1, YELLOW)]
    out += round_x(y0, z, 1.6, x + 1.4, x + 2.4, GRAPHITE) + round_x(y1, z, 1.3, x + 1.4, x + 2.3, GRAPHITE)
    return out


def sd_fore():
    """The forearm (upright here, from the elbow to the wrist), the elbow's motor housing."""
    x, _, z = SD_BASE
    y0, y1 = SD_SHOULDER + SD_UPPER, SD_SHOULDER + SD_UPPER + SD_FORE
    out = [box(x - 1.6, y0 - 1.3, z - 1.3, x - 0.6, y0 + 1.3, z + 1.3, YELLOW), box(x - 1.4, y0, z - 0.9, x + 0.4, y1, z + 0.9, YELLOW)]
    out += round_x(y1, z, 1.0, x - 1.5, x + 0.5, GRAPHITE)
    return out


def sd_wrist():
    """The wrist and the gripper (pointing up here, along the forearm): the tool flange, the vacuum plate with its
    four suction cups, the clamp jaws (drums), the camera."""
    x, _, z = SD_BASE
    y0 = SD_SHOULDER + SD_UPPER + SD_FORE
    xc = x - 0.5
    out = round_y(xc, z, 0.9, y0 + 0.6, y0 + 2.0, GRAPHITE)
    out += [box(xc - 3.0, y0 + 2.0, z - 3.0, xc + 3.0, y0 + 2.8, z + 3.0, GRAPHITE)]
    for dx in (-1.8, 1.8):
        for dz in (-1.8, 1.8):
            out += round_y(xc + dx, z + dz, 0.75, y0 + 2.8, y0 + 3.6, "pi_wh_cup")
    out += [box(xc - 3.4, y0 + 1.6, z - 1.0, xc - 3.0, y0 + 4.6, z + 1.0, STEEL), box(xc + 3.0, y0 + 1.6, z - 1.0, xc + 3.4, y0 + 4.6, z + 1.0, STEEL)]
    out += [box(xc + 1.0, y0 + 0.8, z + 3.0, xc + 2.4, y0 + 2.0, z + 3.8, BLACK)]
    return out


def sd_film():
    """The film carriage (its bottom at 0 here), on the mast at the back left: the carriage, its arm out to the
    left, the roll of stretch film standing on its core, the pre-stretch rollers."""
    out = [box(1.0, 0.0, 2.8, 3.4, 3.0, 4.0, YELLOW), box(-0.8, 0.6, 3.1, 1.0, 1.6, 3.8, GRAPHITE)]
    out += PP.round_y(-1.3, 3.7, 1.15, 0.2, 5.6, "pi_wh_film_roll") + round_y(-1.3, 3.7, 0.4, 5.6, 6.0, "pi_wh_core")
    out += round_y(0.4, 5.2, 0.35, 0.4, 5.4, CHROME) + round_y(1.2, 5.6, 0.35, 0.4, 5.4, CHROME)
    return out


def trunking():
    """Steel cable trunking along a wall (one block of it, along x; the docks' conduits rise into it): its body, the
    lid's lip and the coupler at the block's end."""
    return [box(0.0, 3.0, 0.0, 16.0, 9.0, 3.2, STEEL), box(0.0, 8.6, 3.2, 16.0, 9.2, 3.5, STEEL), box(0.0, 2.8, 3.2, 16.0, 3.4, 3.5, STEEL),
            box(15.0, 2.6, 0.0, 16.0, 9.4, 3.6, GRAPHITE)]


# ================================================================================================ the outside: goods in, goods out
def _flange(cx, cy, z0, label_tex, lamp):
    """A pipe inlet or outlet on a wall panel's face (z0 the face, out to +z): the bolted flange, the stub the pipe
    couples to (reaching the block's edge), its label plate above and its flow lamp beside."""
    out = round_z(cx, cy, 3.0, z0, z0 + 0.8, GRAPHITE) + round_z(cx, cy, 2.1, z0 + 0.8, z0 + 2.0, STEEL)
    for k in range(8):
        a = math.radians(22.5 + k * 45)
        bx, by = cx + 2.55 * math.cos(a), cy + 2.55 * math.sin(a)
        out.append(box(bx - 0.3, by - 0.3, z0 + 0.8, bx + 0.3, by + 0.3, z0 + 1.1, GRAPHITE))
    out += [box(cx - 2.6, cy + 3.8, z0, cx + 2.6, cy + 5.6, z0 + 0.2, {"*": STEEL, "south": label_tex + "!"})]
    out += [box(cx + 3.6, cy - 0.7, z0, cx + 5.0, cy + 0.7, z0 + 0.4, GRAPHITE)] + round_z(cx + 4.3, cy, 0.5, z0 + 0.4, z0 + 1.2, lamp)
    return out


def inlet_panel():
    """The Goods In wall panel (three wide, two high, in the outer wall behind the Intake Case Packer, its face
    outside, +z): galvanised steel in a frame, a canopy over it; four flanged pipe inlets (IN 1 to IN 4, one on
    each side block's face) with their flow lamps; in the middle the drop-off hatch (a stainless hopper flap over a
    ledge, its screen and lamp above): what is put in it goes to the intake like what is piped in. The machine's
    ports meet it from inside."""
    face = {"*": STEEL, "south": "pi_wh_panel_face", "north": STEEL}
    out = [box(pa[0], pb[0], 0.0, pa[1], pb[1], 1.0, face) for pa, pb in _minus(0.0, 48.0, 0.0, 32.0, (19.0, 29.0, 10.0, 19.0))]
    out += [box(0.0, 0.0, 1.0, 1.0, 30.0, 2.0, GRAPHITE), box(47.0, 0.0, 1.0, 48.0, 30.0, 2.0, GRAPHITE),
            box(1.0, 0.0, 1.0, 47.0, 0.8, 2.0, GRAPHITE), box(16.0, 0.8, 1.0, 16.8, 30.0, 1.6, GRAPHITE), box(31.2, 0.8, 1.0, 32.0, 30.0, 1.6, GRAPHITE)]
    # The canopy over the panel: its sloped sheet on two brackets, the gutter.
    out += [box(0.0, 30.0, 0.0, 48.0, 31.0, 2.0, GRAPHITE), box(0.0, 31.0, 0.0, 48.0, 31.6, 9.0, {"*": GRAPHITE, "up": STEEL}),
            box(0.0, 30.4, 9.0, 48.0, 31.6, 9.8, STEEL)]
    for x in (1.0, 15.6, 31.4, 46.0):
        out.append(box(x, 26.0, 1.0, x + 1.0, 31.0, 2.0, GRAPHITE))
        out.append(box(x, 27.2, 2.0, x + 1.0, 28.0, 8.0, GRAPHITE, rot_x(x, 27.6, 2.0, 22.5)))
    out += [box(21.0, 29.4, 2.4, 27.0, 31.0, 6.0, GRAPHITE), box(21.4, 29.2, 2.8, 26.6, 29.4, 5.6, "pi_wh_wallpack")]
    # The GOODS IN sign under the canopy.
    out += [box(17.0, 26.4, 1.6, 31.0, 29.6, 1.9, {"*": STEEL, "south": "pi_wh_sign_in!"})]
    # The pipe inlets.
    for (cx, cy), label in (((8.0, 8.0), "pi_wh_lbl_in1"), ((40.0, 8.0), "pi_wh_lbl_in2"), ((8.0, 22.0), "pi_wh_lbl_in3"), ((40.0, 22.0), "pi_wh_lbl_in4")):
        out += _flange(cx, cy, 1.0, label, "pi_wh_lamp_green")
    # The drop-off hatch: its stainless surround, the flap (tilted in at the bottom), the handle, the ledge on its
    # brackets; the screen and the lamp above it.
    out += [box(18.0, 9.0, 1.0, 30.0, 10.0, 3.0, STAINLESS), box(18.0, 19.0, 1.0, 30.0, 20.0, 3.0, STAINLESS),
            box(18.0, 10.0, 1.0, 19.0, 19.0, 3.0, STAINLESS), box(29.0, 10.0, 1.0, 30.0, 19.0, 3.0, STAINLESS),
            box(19.0, 10.0, -1.4, 29.0, 19.0, -1.0, BLACK)]
    out.append(box(19.2, 10.2, 1.6, 28.8, 19.0, 2.0, {"*": STAINLESS, "south": "pi_wh_hatch_flap!"}, rot_x(19.2, 19.0, 1.8, -22.5)))
    out += [box(22.0, 11.4, 2.8, 26.0, 12.0, 3.4, CHROME)]
    out += [box(17.4, 8.2, 1.0, 30.6, 9.0, 6.0, STAINLESS), box(19.0, 6.0, 1.0, 19.8, 8.2, 4.4, STAINLESS), box(28.2, 6.0, 1.0, 29.0, 8.2, 4.4, STAINLESS)]
    out += screen_unit(19.4, 21.0, 28.6, 25.4, 1.0, "pi_wh_scr_dropoff", hood=False)
    out += [box(30.0, 22.6, 1.0, 31.6, 24.2, 1.6, GRAPHITE)] + round_z(30.8, 23.4, 0.55, 1.6, 2.4, "pi_wh_lamp_green")
    return _in_wall(out, hole=(19.0, 10.0, 29.0, 19.0, -1.4))


WALL_FACE = 13.0      # a wall panel's outer face (the wall's own thickness behind it)


def _in_wall(boxes, hole=None):
    """A wall panel drawn on a plate at z 0..1 set into the wall: the plate thickened to the wall's thickness, all
    on it moved out so its face lies 3 px inside the wall's outside face (the canopy and flanges standing out).
    hole (x0, y0, x1, y1, its back's z as drawn): an opening through the plate back to there (a window's reveal)."""
    tex = {"*": STEEL, "north": "pi_wh_panel_back"}
    if hole is None:
        plate = [box(0.0, 0.0, 0.0, 48.0, 32.0, WALL_FACE, tex)]
    else:
        x0, y0, x1, y1, zb = hole
        zb += WALL_FACE
        plate = [box(0.0, 0.0, 0.0, x0, 32.0, WALL_FACE, tex), box(x1, 0.0, 0.0, 48.0, 32.0, WALL_FACE, tex),
                 box(x0, 0.0, 0.0, x1, y0, WALL_FACE, tex), box(x0, y1, 0.0, x1, 32.0, WALL_FACE, tex),
                 box(x0, y0, 0.0, x1, y1, zb, tex)]
    return plate + shifted(boxes, 0.0, 0.0, WALL_FACE)


def screen_unit(x0, y0, x1, y1, z0, tex, hood=True):
    """A screen built as a unit on a face (z0 the face, out to +z): its housing, the black bezel round the glass,
    the glass with its page (tex, whole), a rain hood over it."""
    out = [box(x0 - 0.6, y0 - 0.6, z0, x1 + 0.6, y1 + 0.6, z0 + 1.6, GRAPHITE),
           box(x0 - 0.3, y0 - 0.3, z0 + 1.6, x1 + 0.3, y0 + 0.2, z0 + 2.0, BLACK), box(x0 - 0.3, y1 - 0.2, z0 + 1.6, x1 + 0.3, y1 + 0.3, z0 + 2.0, BLACK),
           box(x0 - 0.3, y0 + 0.2, z0 + 1.6, x0 + 0.2, y1 - 0.2, z0 + 2.0, BLACK), box(x1 - 0.2, y0 + 0.2, z0 + 1.6, x1 + 0.3, y1 - 0.2, z0 + 2.0, BLACK),
           box(x0 + 0.2, y0 + 0.2, z0 + 1.6, x1 - 0.2, y1 - 0.2, z0 + 1.8, {"*": BLACK, "south": tex + "!"})]
    if hood:
        out += [box(x0 - 0.9, y1 + 0.6, z0, x1 + 0.9, y1 + 1.0, z0 + 4.2, GRAPHITE),
                box(x0 - 0.9, y1 - 2.4, z0 + 1.6, x0 - 0.5, y1 + 0.6, z0 + 4.2, GRAPHITE),
                box(x1 + 0.5, y1 - 2.4, z0 + 1.6, x1 + 0.9, y1 + 0.6, z0 + 4.2, GRAPHITE)]
    return out


def pickup_window(game=False):
    """The Goods Out wall panel (three wide, two high, in the outer wall in front of Ready Storage's access side; its
    face outside, +z): the pickup window where Ready Storage's extractor presents a tray (the reveal round it, the
    extractor's rails and carrier inside, the light curtain's two posts with their emitters, the shelf under it);
    the order kiosk beside it (its pedestal and keypad, the screen under its hood, the scanner, the receipt slot);
    the green beacon, the GOODS OUT sign, the canopy and its light; the Output Port's flanged outlet (OUT)."""
    face = {"*": STEEL, "south": "pi_wh_panel_face", "north": STEEL}
    out = [box(pa[0], pb[0], 0.0, pa[1], pb[1], 1.0, face) for pa, pb in _minus(0.0, 48.0, 0.0, 32.0, (13.6, 34.4, 8.6, 23.4))]
    out += [box(0.0, 0.0, 1.0, 1.0, 30.0, 2.0, GRAPHITE), box(47.0, 0.0, 1.0, 48.0, 30.0, 2.0, GRAPHITE),
            box(1.0, 0.0, 1.0, 47.0, 0.8, 2.0, GRAPHITE)]
    out += [box(0.0, 30.0, 0.0, 48.0, 31.0, 2.0, GRAPHITE), box(0.0, 31.0, 0.0, 48.0, 31.6, 9.0, {"*": GRAPHITE, "up": STEEL}),
            box(0.0, 30.4, 9.0, 48.0, 31.6, 9.8, STEEL)]
    for x in (1.0, 13.6, 33.4, 46.0):
        out.append(box(x, 26.0, 1.0, x + 1.0, 31.0, 2.0, GRAPHITE))
        out.append(box(x, 27.2, 2.0, x + 1.0, 28.0, 8.0, GRAPHITE, rot_x(x, 27.6, 2.0, 22.5)))
    out += [box(15.0, 26.4, 1.6, 33.0, 29.6, 1.9, {"*": STEEL, "south": "pi_wh_sign_out!"})]
    out += [box(35.0, 29.4, 2.4, 41.0, 31.0, 6.0, GRAPHITE), box(35.4, 29.2, 2.8, 40.6, 29.4, 5.6, "pi_wh_wallpack")]
    # ---- the window: its stainless frame and reveal back into the wall, the dark inside.
    out += [box(13.0, 8.0, 1.0, 35.0, 8.6, 3.4, STAINLESS), box(13.0, 23.4, 1.0, 35.0, 24.0, 3.4, STAINLESS),
            box(13.0, 8.6, 1.0, 13.6, 23.4, 3.4, STAINLESS), box(34.4, 8.6, 1.0, 35.0, 23.4, 3.4, STAINLESS)]
    out += [box(13.6, 23.0, -8.0, 34.4, 23.4, 1.0, STAINLESS), box(13.6, 8.6, -8.0, 14.0, 23.0, 1.0, STAINLESS),
            box(34.0, 8.6, -8.0, 34.4, 23.0, 1.0, STAINLESS), box(14.0, 8.6, -8.4, 34.0, 23.0, -8.0, "pi_wh_vlm_core")]
    out += [box(15.0, 22.2, -7.6, 33.0, 23.0, -6.6, GRAPHITE), box(15.4, 22.0, -7.4, 32.6, 22.2, -6.8, "pi_wh_wallpack")]
    # The extractor inside: its two rails, the carrier the tray rides out on, the drive's toothed belt.
    out += [box(14.0, 9.2, -8.0, 15.0, 10.4, 1.0, STEEL), box(33.0, 9.2, -8.0, 34.0, 10.4, 1.0, STEEL),
            box(15.0, 9.4, -6.0, 33.0, 9.8, 0.6, STEEL), box(16.0, 13.0, -8.0, 32.0, 13.6, -7.4, "pi_wh_hose"),
            box(14.0, 14.0, -8.0, 15.0, 22.0, -7.0, GRAPHITE), box(33.0, 14.0, -8.0, 34.0, 22.0, -7.0, GRAPHITE)]
    out += round_x(11.8, -6.8, 0.8, 15.0, 33.0, STEEL)
    # The tray (presented: out past the face), its lips and handle, two cases and a tote on it (in the game: live parts).
    if not game:
        out += _tray_boxes() + _tray_load_boxes()
    # The light curtain: two aluminium posts in the reveal, their emitter windows facing each other.
    for x0, xin in ((14.0, 15.0), (33.0, 33.0)):
        out += [box(x0, 8.6, 1.4, x0 + 1.0, 23.0, 2.6, {"*": ALU, "up": BLACK, "down": BLACK})]
        ex0, ex1 = (xin, xin + 0.1) if xin < 20 else (xin - 0.1, xin)
        for k in range(9):
            y = 9.6 + k * 1.5
            out.append(box(ex0, y, 1.7, ex1, y + 0.7, 2.3, "pi_wh_emitter"))
    # The shelf under the window on its brackets.
    out += [box(13.0, 6.8, 1.0, 35.0, 7.6, 6.6, STAINLESS), box(15.0, 4.6, 1.0, 15.8, 6.8, 5.0, STAINLESS),
            box(32.2, 4.6, 1.0, 33.0, 6.8, 5.0, STAINLESS)]
    # ---- the kiosk: pedestal and its base, the head with the screen under its hood, the scanner, the receipt slot,
    # the keypad.
    out += [box(2.6, 0.0, 1.0, 10.4, 0.6, 5.4, STEEL)]
    out += rrect_y(3.2, 1.0, 9.8, 4.4, 0.6, 11.0, 0.8, GRAPHITE)
    out += [box(2.4, 11.0, 1.0, 10.6, 24.0, 4.0, GRAPHITE)]
    out += screen_unit(3.2, 14.6, 9.8, 23.0, 4.0, "pi_wh_scr_pickup")
    out += [box(3.6, 11.8, 4.0, 9.2, 13.6, 5.8, BLACK), box(4.2, 13.6, 4.4, 8.6, 13.7, 5.6, "pi_wh_scan_glass")]
    out += [box(4.4, 9.4, 4.4, 8.6, 9.8, 4.6, BLACK), box(5.2, 8.6, 4.4, 7.8, 9.5, 4.55, "pi_paper")]
    out += [box(3.8, 4.0, 4.4, 9.2, 8.2, 4.6, BLACK)]
    for i in range(3):
        for j in range(4):
            out.append(box(4.2 + i * 1.7, 4.3 + j * 1.0, 4.6, 5.3 + i * 1.7, 5.0 + j * 1.0, 4.9, "pi_wh_key"))
    # ---- the beacon over the window; the output port.
    out += round_y(24.0, 2.6, 0.9, 24.0, 24.8, GRAPHITE) + PP.round_y(24.0, 2.6, 0.75, 24.8, 26.2, "pi_wh_lamp_green")
    out += _flange(40.0, 10.0, 1.0, "pi_wh_lbl_out", "pi_wh_lamp_green")
    return _in_wall(out, hole=(13.6, 8.6, 34.4, 23.4, -8.4))


def _tray_boxes():
    """The pickup window's tray presented (out past the face): its floor, lips and handle (the panel's own frame,
    before _in_wall moves it out)."""
    return [box(15.0, 9.8, -6.0, 33.0, 10.3, 5.6, {"*": STAINLESS, "up": "pi_wh_tray"}),
            box(15.0, 10.3, 5.2, 33.0, 11.6, 5.6, STAINLESS), box(15.0, 10.3, -6.0, 33.0, 11.6, -5.6, STAINLESS),
            box(15.0, 10.3, -5.6, 15.4, 11.6, 5.2, STAINLESS), box(32.6, 10.3, -5.6, 33.0, 11.6, 5.2, STAINLESS),
            box(21.0, 9.0, 5.6, 27.0, 9.6, 6.2, CHROME), box(21.0, 9.6, 5.6, 21.6, 10.6, 6.2, CHROME), box(26.4, 9.6, 5.6, 27.0, 10.6, 6.2, CHROME)]


def _tray_load_boxes():
    return [box(16.0, 10.3, 0.2, 22.2, 15.0, 4.8, {"*": "pi_wh_carton", "up": "pi_wh_carton_top", "south": "pi_wh_carton_front!"}),
            box(23.0, 10.3, 0.6, 28.4, 14.0, 4.8, {"*": "pi_wh_carton_w", "up": "pi_wh_carton_w_top", "south": "pi_wh_carton_w_front!"}),
            box(29.0, 10.3, -4.0, 32.4, 13.0, 4.8, {"*": "pi_wh_tote", "up": "pi_wh_tote_in"}), box(29.2, 12.0, 4.8, 32.2, 12.6, 4.9, BLACK)]


def out_tray():
    """The pickup window's tray as a live part, presented, in the panel's frame (the game slides it in and out)."""
    return shifted(_tray_boxes(), 0.0, 0.0, WALL_FACE)


def out_tray_load():
    return shifted(_tray_load_boxes(), 0.0, 0.0, WALL_FACE)


def bollard():
    """A steel bollard (yellow, a black reflective band) on its base plate."""
    return [box(5.2, 0.0, 5.2, 10.8, 0.4, 10.8, STEEL)] + PP.round_y(8.0, 8.0, 2.2, 0.4, 15.0, YELLOW) + \
        round_y(8.0, 8.0, 2.3, 11.0, 12.0, BLACK) + PP.round_y(8.0, 8.0, 1.9, 15.0, 15.8, YELLOW)


def _plate_text(text, w=256, h=96, bg=(246, 246, 240), fg=(20, 20, 22), band=None):
    img = _img(w, h, bg)
    d = ImageDraw.Draw(img)
    if band:
        d.rectangle([0, 0, w - 1, h // 3], fill=band)
    d.rectangle([0, 0, w - 1, h - 1], outline=(40, 40, 44), width=4)
    _t(d, (w // 2, h // 2 + (h // 6 if band else 0)), text, h // 2, fg, width=w - 20)
    return _sq(img)


def _sign(top, sub, colour):
    img = _img(512, 116, colour)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 511, 115], outline=(250, 250, 246), width=6)
    _t(d, (256, 44), top, 46, (250, 250, 246), width=480)
    _t(d, (256, 90), sub, 22, (250, 250, 246), width=480)
    return _sq(img)


def exterior_makers():
    import nuclear_materials as M
    return {
        "pi_wh_panel_face": lambda: M.brushed((170, 174, 176), 2190, "v", dirt=0.12),
        "pi_wh_panel_back": lambda: PP._solid((150, 154, 158), 2194, 0.05),
        "pi_wh_sign_in": lambda: _sign("GOODS IN", "PIPE INLETS  -  DROP OFF", (30, 110, 60)),
        "pi_wh_sign_out": lambda: _sign("GOODS OUT", "PICKUP  -  PIPE OUTLET", (30, 76, 150)),
        "pi_wh_lbl_in1": lambda: _plate_text("IN 1"), "pi_wh_lbl_in2": lambda: _plate_text("IN 2"),
        "pi_wh_lbl_in3": lambda: _plate_text("IN 3"), "pi_wh_lbl_in4": lambda: _plate_text("IN 4"),
        "pi_wh_lbl_out": lambda: _plate_text("OUT"),
        "pi_wh_hatch_flap": _hatch_flap, "pi_wh_tray": lambda: M.brushed((200, 204, 208), 2191, "h", dirt=0.05),
        "pi_wh_vlm_dark": lambda: PP._solid((26, 28, 32), 2192, 0.03),
        "pi_wh_curtain": _curtain, "pi_wh_reader": _reader,
        "pi_wh_scr_dropoff": _dropoff_page,
        "pi_wh_scr_pickup": _pickup_page, "pi_wh_scan_glass": lambda: _stack((255, 50, 40), True),
        "pi_wh_key": lambda: PP._solid((200, 204, 208), 2196, 0.02), "pi_wh_emitter": lambda: _stack((200, 30, 24), True),
        "pi_wh_tote": lambda: PP._solid((40, 90, 170), 2197, 0.04), "pi_wh_tote_in": lambda: PP._solid((30, 70, 140), 2198, 0.04),
    }


def _hatch_flap():
    import nuclear_materials as M
    img = M.brushed((196, 200, 204), 2193, "h", dirt=0.05).convert("RGBA").resize((256, 256))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 255], outline=(140, 144, 148, 255), width=8)
    _t(d, (128, 150), "PUSH", 40, (60, 64, 70))
    d.polygon([(128, 60), (100, 100), (156, 100)], fill=(60, 64, 70, 255))
    return img


def _curtain():
    img = _img(32, 128, (24, 24, 26))
    d = ImageDraw.Draw(img)
    for y in range(4, 128, 8):
        d.rectangle([12, y, 20, y + 3], fill=(220, 40, 30, 255))
    return img


def _reader():
    img = _img(128, 64, (30, 30, 34))
    d = ImageDraw.Draw(img)
    d.rectangle([10, 10, 117, 53], outline=(90, 200, 240, 255), width=3)
    _t(d, (64, 32), "SCAN", 20, (90, 200, 240))
    return _sq(img)


def _pickup_page():
    """The kiosk's page (portrait, high resolution): PICKUP, your orders with their state, the window's state, the
    two buttons (OPEN WINDOW, SCAN ORDER)."""
    from PIL import ImageFont
    img = _img(384, 512, (10, 16, 26))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 383, 54], fill=(24, 60, 110))
    _t(d, (18, 28), "PICKUP", 30, (240, 244, 250), anchor="lm")
    _t(d, (366, 28), "WINDOW 1", 18, (150, 200, 240), anchor="rm")
    rows = [("1043", "IRON INGOT", "x128", "READY", (100, 230, 140)), ("1044", "OAK LOG", "x64", "ON ITS WAY", (255, 190, 60)),
            ("1045", "REDSTONE", "x32", "QUEUED", (150, 160, 178))]
    for k, (no, item, q, st, col) in enumerate(rows):
        y = 76 + k * 92
        d.rectangle([12, y, 371, y + 80], fill=(18, 30, 46), outline=col, width=3 if k == 0 else 1)
        _t(d, (24, y + 20), f"ORDER {no}", 18, (150, 160, 178), anchor="lm")
        _t(d, (24, y + 54), item, 26, (236, 240, 246), anchor="lm", width=220)
        _t(d, (360, y + 20), st, 18, col, anchor="rm")
        _t(d, (360, y + 54), q, 26, (236, 240, 246), anchor="rm")
    d.rectangle([12, 368, 371, 418], fill=(30, 110, 60))
    _t(d, (192, 393), "OPEN WINDOW  -  ORDER 1043", 20, (240, 250, 244), width=340)
    d.rectangle([12, 430, 371, 480], fill=(30, 50, 80), outline=(90, 200, 240), width=2)
    _t(d, (192, 455), "SCAN AN ORDER SLIP", 20, (150, 210, 240), width=340)
    return _sq(img)


def _dropoff_page():
    img = _img(512, 232, (10, 16, 26))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 511, 50], fill=(30, 100, 60))
    _t(d, (18, 26), "DROP OFF", 28, (240, 250, 244), anchor="lm")
    _t(d, (494, 26), "GOODS IN", 18, (190, 240, 210), anchor="rm")
    for k, (a, b, col) in enumerate((("Push the flap, put items in", "", None), ("To the intake", "OK", (100, 230, 140)),
                                     ("Waiting to be packed", "3 CASES", (255, 190, 60)), ("Stored today", "6,912", (236, 240, 246)))):
        y = 74 + k * 44
        _t(d, (20, y), a, 22, (190, 200, 214), anchor="lm", width=330 if b else 470)
        _t(d, (492, y), b, 22, col or (236, 240, 246), anchor="rm")
    return _sq(img)


EXTERIOR_SHARP = {"pi_wh_scan_glass", "pi_wh_emitter", "pi_wh_sign_in", "pi_wh_sign_out", "pi_wh_lbl_in1", "pi_wh_lbl_in2", "pi_wh_lbl_in3", "pi_wh_lbl_in4", "pi_wh_lbl_out",
                  "pi_wh_hatch_flap", "pi_wh_scr_dropoff", "pi_wh_scr_pickup", "pi_wh_reader"}


# ================================================================================================ Ready Storage (W2; concept)
def mirror_z(boxes, cz=0.0):
    """Boxes mirrored across z = cz (north and south faces swapped; turned boxes' angles about x and y flipped)."""
    out = []
    for b in boxes:
        tex = b[6]
        if isinstance(tex, dict) and ("north" in tex or "south" in tex):
            tex = dict(tex)
            n, so = b[6].get("north"), b[6].get("south")
            tex.pop("north", None)
            tex.pop("south", None)
            if so is not None:
                tex["north"] = so
            if n is not None:
                tex["south"] = n
        nb = (b[0], b[1], round(2 * cz - b[5], 4), b[3], b[4], round(2 * cz - b[2], 4), tex)
        if len(b) > 7 and b[7]:
            r = dict(b[7]["rotation"])
            o = r["origin"]
            r["origin"] = [o[0], o[1], round(2 * cz - o[2], 4)]
            if r["axis"] in ("x", "y"):
                r["angle"] = -r["angle"]
            nb += ({"rotation": r},)
        out.append(nb)
    return out


VLM_WIN = (8.0, 24.0, 6.0, 58.0)     # the side window: z0, z1, y0, y1 (on the -x side)
VLM_DEEP = 12.0                       # how far in the window's bay goes (x)


HATCH = (14.6, 33.4, 32.0, 22.0)      # the dispatch hatch's opening (x0, x1, top) and its pick cell's depth (z)


def _vlm_shell(hatch=True, carriage=True, seed=0):
    """A lift module's cabinet (three wide, two deep, four high): the dark core (less the pick cell behind the hatch
    when hatch), the corner posts, the grey cladding panels with their seams (round the window on the -x side and the
    hatch on the back), the side window (frame, glass) into the bay where the trays stand on their guides, some
    loaded, the extractor's carriage on its chains between them."""
    out = []
    z0, z1, y0, y1 = VLM_WIN
    core = "pi_wh_vlm_core"
    hx0, hx1, htop, hd = HATCH
    # The core, leaving the window's bay and (with the hatch) the pick cell.
    if hatch:
        out += [box(VLM_DEEP, 0.0, 1.0, hx0, 62.0, 31.0, core), box(hx1, 0.0, 1.0, 47.0, 62.0, 31.0, core),
                box(hx0, htop, 1.0, hx1, 62.0, 31.0, core), box(hx0, 0.0, hd, hx1, htop, 31.0, core)]
    else:
        out += [box(VLM_DEEP, 0.0, 1.0, 47.0, 62.0, 31.0, core)]
    out += [box(1.0, 0.0, 1.0, VLM_DEEP, y0, 31.0, core), box(1.0, y1, 1.0, VLM_DEEP, 62.0, 31.0, core),
            box(1.0, y0, 1.0, VLM_DEEP, y1, z0, core), box(1.0, y0, z1, VLM_DEEP, y1, 31.0, core)]
    for x in (0.0, 46.4):
        for z in (0.0, 30.4):
            out.append(box(x, 0.0, z, x + 1.6, 62.0, z + 1.6, GRAPHITE))
    P = "pi_wh_vlm_panel"
    rows = [(0.2, 15.85), (16.15, 31.85), (32.15, 47.85), (48.15, 61.8)]
    hole = (HATCH[0] - 3.0, HATCH[1] + 3.0, 0.0, HATCH[2] + 4.0) if hatch else (0.0, 0.0, 0.0, 0.0)
    for ya, yb in rows:
        for za, zb in ((1.6, 15.85), (16.15, 30.4)):
            out.append(box(47.0, ya, za, 47.4, yb, zb, P))                              # +x side
            for (pa, pb) in _minus(za, zb, ya, yb, (z0 - 0.6, z1 + 0.6, y0 - 0.6, y1 + 0.6)):
                out.append(box(0.6, pb[0], pa[0], 1.0, pb[1], pa[1], P))              # -x side
        for xa, xb in ((1.6, 15.85), (16.15, 31.85), (32.15, 46.4)):
            for (pa, pb) in _minus(xa, xb, ya, yb, hole):
                out.append(box(pa[0], pb[0], 0.6, pa[1], pb[1], 1.0, P))              # back
    out += [box(0.2, y0 - 0.6, z0 - 0.6, 1.2, y0, z1 + 0.6, GRAPHITE), box(0.2, y1, z0 - 0.6, 1.2, y1 + 0.6, z1 + 0.6, GRAPHITE),
            box(0.2, y0, z0 - 0.6, 1.2, y1, z0, GRAPHITE), box(0.2, y0, z1, 1.2, y1, z1 + 0.6, GRAPHITE),
            box(0.6, y0, z0, 0.7, y1, z1, {"west": "pi_wh_vlm_glass", "east": "pi_wh_vlm_glass"})]
    out += [box(VLM_DEEP - 0.2, y0, z0, VLM_DEEP, y1, z1, "pi_wh_vlm_dark")]
    for zg in (z0, z1 - 0.8):
        out.append(box(1.4, y0, zg, VLM_DEEP - 0.2, y1, zg + 0.8, GRAPHITE))
    loads = ["carton", None, "tote", "carton_w", "carton", None, "tote", "carton", None, "carton_w", "tote", "carton"]
    k = seed
    for ty in range(int(y0) + 2, int(y1) - 3, 4):
        if carriage and 29 <= ty <= 33:
            continue                                    # where the extractor's carriage is
        out += [box(1.6, ty, z0 + 0.8, VLM_DEEP - 0.4, ty + 0.5, z1 - 0.8, STAINLESS), box(1.6, ty + 0.5, z0 + 0.8, 2.0, ty + 1.1, z1 - 0.8, STAINLESS)]
        ld = loads[k % len(loads)]
        k += 1
        if ld == "tote":
            out += [box(2.6, ty + 0.5, z0 + 2.0, 6.6, ty + 2.9, z0 + 7.0, {"*": "pi_wh_tote", "up": "pi_wh_tote_in"}),
                    box(2.6, ty + 0.5, z0 + 8.6, 6.6, ty + 2.9, z0 + 13.6, {"*": "pi_wh_tote", "up": "pi_wh_tote_in"})]
        elif ld:
            tex = {"*": f"pi_wh_{ld}", "up": f"pi_wh_{ld}_top"}
            out += [box(2.4, ty + 0.5, z0 + 1.6, 6.4, ty + 3.1, z0 + 5.6, tex), box(2.4, ty + 0.5, z0 + 6.4, 6.8, ty + 2.7, z0 + 10.0, tex),
                    box(7.0, ty + 0.5, z0 + 3.0, 10.6, ty + 3.0, z0 + 7.4, tex)]
    if carriage:
        out += [box(1.6, 29.6, z0 + 0.6, VLM_DEEP - 0.4, 31.0, z1 - 0.6, GRAPHITE), box(1.6, 31.0, z0 + 0.8, 7.0, 31.5, z1 - 0.8, STAINLESS)]
    for zc in (z0 + 1.4, z1 - 1.8):
        out.append(box(VLM_DEEP - 1.4, y0, zc, VLM_DEEP - 0.6, y1, zc + 0.4, "pi_wh_hose"))
    return out


def _vlm_top(drive=True):
    """The module's top: its cap (a joining band when another module stands on it), and on the top one the drive
    (motor, gearbox, belt guard, cable chain) and the beacon."""
    if not drive:
        return [box(0.0, 62.0, 0.0, 48.0, 64.0, 32.0, GRAPHITE)]
    out = [box(0.0, 62.0, 0.0, 48.0, 63.0, 32.0, GRAPHITE)]
    out += [box(14.0, 63.0, 8.0, 30.0, 64.0, 24.0, GRAPHITE), box(16.0, 64.0, 10.0, 24.0, 69.0, 18.0, MACHINE)]
    out += round_x(66.0, 14.0, 2.4, 24.0, 32.0, MACHINE) + round_x(66.0, 14.0, 1.6, 32.0, 33.0, GRAPHITE)
    out += [box(24.0, 64.0, 18.2, 33.0, 69.0, 21.0, YELLOW), box(36.0, 63.0, 6.0, 44.0, 64.6, 26.0, BLACK)]
    out += round_y(4.0, 4.0, 1.0, 63.0, 63.8, GRAPHITE) + PP.round_y(4.0, 4.0, 0.85, 63.8, 65.4, "pi_wh_lamp_green")
    return out


def vlm(top=True, screen="pi_wh_scr_vlm", shutter=0.0, carriage=True):
    """Ready Storage, a vertical lift module (three wide, two deep, four high; its access side +z against the Goods
    Out panel in the outer wall, its back to the warehouse floor), all modelled: the cabinet (_vlm_shell); its top
    (_vlm_top: the drive when nothing stands on it); on the back the dispatch hatch (yellow frame, the shutter's coil
    box with its sign, the rubber edge; the shutter's slats are a live part, vlm_shutter), the pick cell behind it
    (vlm_cell) and the roller bed out front (frame, rollers, legs, stop, photo-eyes), the operator screen and E-stop,
    the READY STORAGE plate. shutter: how far open (0..1), for the mockups."""
    out = _vlm_shell(True, carriage) + _vlm_top(top)
    x0, x1, ht, _ = HATCH
    # The opening is wider than a pallet (16): its yellow posts stand either side of it.
    hatch = [box(x0 - 2.0, 0.0, -0.6, x0, ht, 0.6, YELLOW), box(x1, 0.0, -0.6, x1 + 2.0, ht, 0.6, YELLOW)]
    hatch += [box(x0 - 2.6, ht, -2.8, x1 + 2.6, ht + 4.0, 0.6, GRAPHITE), box(x0 - 2.2, ht + 0.3, -3.0, x1 + 2.2, ht + 3.7, -2.8, GRAPHITE)]
    hatch += [box(16.0, ht + 0.6, -3.1, 32.0, ht + 3.4, -3.0, {"north": "pi_wh_hatch_sign!"})]
    hatch += (vlm_shutter(shutter) if shutter is not None else []) + vlm_cell()
    hatch += [box(x0 - 1.0, 0.6, -16.0, x0, 2.4, -0.6, "pi_wh_conv_frame"), box(x1, 0.6, -16.0, x1 + 1.0, 2.4, -0.6, "pi_wh_conv_frame")]
    for k in range(10):
        hatch += round_x(1.6, -1.6 - k * 1.45, 0.55, x0, x1, CHROME)
    for zl in (-15.2, -1.4):
        hatch += [box(x0 - 0.8, 0.0, zl - 0.4, x0 - 0.2, 0.6, zl + 0.4, STEEL), box(x1 + 0.2, 0.0, zl - 0.4, x1 + 0.8, 0.6, zl + 0.4, STEEL)]
    hatch += [box(x0 - 1.0, 2.4, -16.0, x1 + 1.0, 3.2, -15.4, YELLOW)]
    hatch += [box(x0 - 2.0, 0.0, -1.6, x0 - 1.2, 5.0, -0.8, BLACK), box(x1 + 1.2, 0.0, -1.6, x1 + 2.0, 5.0, -0.8, BLACK),
              box(x0 - 1.2, 3.6, -1.4, x0 - 1.1, 4.2, -1.0, "pi_wh_emitter"), box(x1 + 1.1, 3.6, -1.4, x1 + 1.2, 4.2, -1.0, "pi_wh_emitter")]
    out += hatch
    unit = screen_unit(36.0, 11.0, 44.4, 17.6, 0.0, screen, hood=False)
    unit += [box(37.4, 6.0, 0.0, 41.0, 9.4, 1.4, YELLOW)] + round_z(39.2, 7.7, 1.1, 1.4, 2.4, "pi_wh_estop")
    out += mirror_z(unit, 0.0)
    out += [box(4.0, 40.0, -0.4, 44.0, 46.4, 0.6, {"*": STEEL, "north": "pi_wh_vlm_plate!"})]
    return out


def vlm_extension(top=True, seed=5):
    """A Ready Storage extension module: the same cabinet, window, trays and carriage chains four blocks higher, with
    no hatch; stood on a module it adds its 108 slots (the drive and beacon move up onto the top one)."""
    out = _vlm_shell(False, carriage=False, seed=seed) + _vlm_top(top)
    out += [box(4.0, 40.0, -0.4, 44.0, 46.4, 0.6, {"*": STEEL, "north": "pi_wh_vlm_plate_x!"})]
    return out


def vlm_shutter(f):
    """The dispatch hatch's roll-up shutter, f open (0 shut .. 1 open): the slats still down, the rubber edge."""
    x0, x1, top, _ = HATCH
    yb = (top - 1.6) * f
    out = [box(x0, yb, -0.6, x1, yb + 0.9, 0.2, RUBBER)]
    y = yb + 0.9
    while y + 1.36 <= top + 0.01:
        out += [box(x0, y, -0.5, x1, y + 1.2, 0.2, "pi_wh_slat"), box(x0, y + 1.2, -0.3, x1, y + 1.36, 0.2, GRAPHITE)]
        y += 1.36
    return out


def vlm_cell():
    """The pick cell behind the hatch (seen when the shutter is up): its lined walls and ceiling with a light strip,
    the roller bed running on in to its stop, the side guides, the gantry's two rails under the ceiling."""
    hx0, hx1, htop, hd = HATCH
    L = "pi_wh_cell_liner"
    out = [box(hx0, 0.0, hd - 0.4, hx1, htop, hd, L), box(hx0, htop - 0.4, 0.6, hx1, htop, hd, L),
           box(hx0, 0.0, 0.6, hx0 + 0.4, htop, hd, L), box(hx1 - 0.4, 0.0, 0.6, hx1, htop, hd, L),
           box(hx0 + 0.4, 0.0, 0.6, hx1 - 0.4, 0.4, hd - 0.4, "pi_wh_vlm_dark")]
    out += [box(21.0, htop - 0.6, 2.0, 27.0, htop - 0.4, hd - 2.0, "pi_wh_wallpack")]
    out += [box(hx0 + 0.4, 0.4, 0.6, hx0 + 1.0, 2.4, hd - 0.4, "pi_wh_conv_frame"), box(hx1 - 1.0, 0.4, 0.6, hx1 - 0.4, 2.4, hd - 0.4, "pi_wh_conv_frame")]
    for k in range(int((hd - 2.4) / 1.45)):
        out += round_x(1.6, 1.4 + k * 1.45, 0.55, hx0 + 1.0, hx1 - 1.0, CHROME)
    out += [box(hx0 + 1.0, 0.4, hd - 1.2, hx1 - 1.0, 3.2, hd - 0.4, YELLOW)]
    for x in (hx0 + 0.4, hx1 - 1.2):
        out += [box(x, 2.4, 1.2, x + 0.8, 6.0, 1.8, YELLOW), box(x, 2.4, hd - 2.4, x + 0.8, 6.0, hd - 1.8, YELLOW)]
    for x in (hx0 + 0.4, hx1 - 1.6):
        out += [box(x, htop - 1.0, 0.6, x + 1.2, htop - 0.4, hd - 0.4, STEEL)]
    return out


def vlm_gantry(gz, gx, head_y):
    """The pick cell's gantry: its bridge across the rails at gz (px back from the hatch), the carriage at gx, the
    tube down to the vacuum head (four cups) whose cups' faces are at head_y."""
    hx0, hx1, htop, hd = HATCH
    out = [box(hx0 + 0.4, htop - 2.4, gz - 1.2, hx1 - 0.4, htop - 1.0, gz + 1.2, YELLOW)]
    out += [box(gx - 2.0, htop - 3.2, gz - 1.6, gx + 2.0, htop - 1.0, gz + 1.6, GRAPHITE)]
    out += [box(gx - 0.7, head_y + 1.6, gz - 0.7, gx + 0.7, htop - 3.2, gz + 0.7, CHROME)]
    out += [box(gx - 3.0, head_y + 0.8, gz - 3.0, gx + 3.0, head_y + 1.6, gz + 3.0, GRAPHITE)]
    for dx in (-1.6, 1.6):
        for dz in (-1.6, 1.6):
            out += round_y(gx + dx, gz + dz, 0.7, head_y, head_y + 0.8, BLACK)
    out += [box(gx + 2.0, htop - 2.2, gz - 0.4, hx1 - 0.6, htop - 1.8, gz + 0.4, "pi_wh_hose")]
    return out


# ---- Ready Storage in the game: the model turned so its hatch is its front (+z, toward its user on the warehouse
# floor) and its access side its back (against the outer wall); the moving bits are live parts in that frame.
def _game(boxes):
    return mirror_z(boxes, 16.0)


def ready_storage():
    """Ready Storage's static model in the game's frame (the shutter, the gantry, the drive, the pallet are drawn
    live)."""
    return _game(vlm(top=False, shutter=None, carriage=True))


def ready_storage_ext():
    return _game(vlm_extension(top=False))


def rs_slat():
    """One of the shutter's slats, in the game's frame, at y 0 (the generator writes it 16 back along z: PART_SHIFT)."""
    x0, x1 = HATCH[0], HATCH[1]
    return _game([box(x0, 0.0, -0.5, x1, 1.2, 0.2, "pi_wh_slat"), box(x0, 1.2, -0.3, x1, 1.36, 0.2, GRAPHITE)])


def rs_edge():
    return _game([box(HATCH[0], 0.0, -0.6, HATCH[1], 0.9, 0.2, RUBBER)])


def rs_bridge():
    """The gantry's bridge across its rails, its middle at z 0."""
    hx0, hx1, htop, _ = HATCH
    return [box(hx0 + 0.4, htop - 2.4, -1.2, hx1 - 0.4, htop - 1.0, 1.2, YELLOW)]


def rs_carriage():
    """The gantry's carriage on the bridge, its middle at x 0, z 0."""
    htop = HATCH[2]
    return [box(-2.0, htop - 3.2, -1.6, 2.0, htop - 1.0, 1.6, GRAPHITE), box(2.0, htop - 2.2, -0.4, 5.0, htop - 1.8, 0.4, "pi_wh_hose")]


def rs_tube():
    """The gantry's vertical tube, 16 long from y 0 (the game stretches it)."""
    return [box(-0.7, 0.0, -0.7, 0.7, 16.0, 0.7, CHROME)]


def rs_head():
    """The vacuum head: its plate and four cups, the cups' faces at y 0, its middle at x 0, z 0."""
    out = [box(-3.0, 0.8, -3.0, 3.0, 1.6, 3.0, GRAPHITE)]
    for dx in (-1.6, 1.6):
        for dz in (-1.6, 1.6):
            out += round_y(dx, dz, 0.7, 0.0, 0.8, BLACK)
    return out


def rs_drive():
    """The drive on the top module (motor, gearbox, belt guard, cable chain, the beacon), from y 0 on the top's cap,
    in the game's frame (the generator writes it 16 to the left: PART_SHIFT)."""
    out = [box(14.0, 0.0, 8.0, 30.0, 1.0, 24.0, GRAPHITE), box(16.0, 1.0, 10.0, 24.0, 6.0, 18.0, MACHINE)]
    out += round_x(3.0, 14.0, 2.4, 24.0, 32.0, MACHINE) + round_x(3.0, 14.0, 1.6, 32.0, 33.0, GRAPHITE)
    out += [box(24.0, 1.0, 18.2, 33.0, 6.0, 21.0, YELLOW), box(36.0, 0.0, 6.0, 44.0, 1.6, 26.0, BLACK)]
    out += round_y(4.0, 4.0, 1.0, 0.0, 0.8, GRAPHITE) + PP.round_y(4.0, 4.0, 0.85, 0.8, 2.4, "pi_wh_lamp_green")
    return _game(out)


def _minus(a0, a1, b0, b1, hole):
    """A panel (a0..a1 across, b0..b1 up) less a rectangle (h_a0, h_a1, h_b0, h_b1): the pieces round it."""
    ha0, ha1, hb0, hb1 = hole
    if ha1 <= a0 or ha0 >= a1 or hb1 <= b0 or hb0 >= b1:
        return [((a0, a1), (b0, b1))]
    out = []
    if hb0 > b0:
        out.append(((a0, a1), (b0, hb0)))
    if hb1 < b1:
        out.append(((a0, a1), (hb1, b1)))
    mb0, mb1 = max(b0, hb0), min(b1, hb1)
    if ha0 > a0:
        out.append(((a0, ha0), (mb0, mb1)))
    if ha1 < a1:
        out.append(((ha1, a1), (mb0, mb1)))
    return [p for p in out if p[0][1] - p[0][0] > 0.05 and p[1][1] - p[1][0] > 0.05]


def vlm_makers():
    import nuclear_materials as M
    return {
        "pi_wh_vlm_panel": lambda: PP._solid((206, 210, 212), 2195, 0.04), "pi_wh_vlm_core": lambda: PP._solid((70, 74, 80), 2199, 0.04),
        "pi_wh_vlm_glass": _vlm_glass, "pi_wh_slat": lambda: M.brushed((160, 164, 168), 2200, "h", dirt=0.08),
        "pi_wh_hatch_sign": lambda: _sign("DISPATCH HATCH", "KEEP CLEAR - AUTOMATIC SHUTTER", (200, 120, 20)),
        "pi_wh_vlm_plate": lambda: _sign("READY STORAGE", "VERTICAL LIFT MODULE  -  108 SLOTS", (30, 76, 150)),
        "pi_wh_vlm_plate_x": lambda: _sign("READY STORAGE", "EXTENSION MODULE  -  +108 SLOTS", (30, 76, 150)),
        "pi_wh_cell_liner": lambda: PP._solid((96, 100, 106), 2201, 0.05),
        "pi_wh_scr_vlm": _vlm_page,
        "pi_wh_wallpack": lambda: _stack((250, 246, 230), True),
    }


def _vlm_page():
    img = _img(512, 400, (10, 16, 26))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 511, 56], fill=(24, 60, 110))
    _t(d, (18, 28), "READY STORAGE", 30, (240, 244, 250), anchor="lm")
    _t(d, (494, 28), "VLM 1", 20, (150, 200, 240), anchor="rm")
    for k, (a, b, col) in enumerate((("Trays", "18 / 18", (236, 240, 246)), ("Full", "61 %", (100, 230, 140)),
                                     ("Dispatch hatch", "CLOSED", (236, 240, 246)), ("Orders waiting", "2", (255, 190, 60)),
                                     ("At the window", "ORDER 1043", (100, 230, 140)))):
        y = 92 + k * 58
        _t(d, (22, y), a, 26, (170, 184, 200), anchor="lm")
        _t(d, (490, y), b, 26, col, anchor="rm")
    return _sq(img)


def _vlm_glass():
    img = Image.new("RGBA", (128, 128), (200, 220, 230, 36))
    d = ImageDraw.Draw(img)
    for k in range(-80, 128, 48):
        d.line([(k, 128), (k + 80, 0)], fill=(240, 248, 255, 60), width=8)
    return img


def _vlm_panel():
    import nuclear_materials as M
    img = PP._solid((206, 210, 212), 2195, 0.04).convert("RGBA").resize((128, 128))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 127, 127], outline=(160, 164, 168, 255), width=2)
    return img


def _vlm_window():
    img = _img(128, 256, (20, 22, 26))
    d = ImageDraw.Draw(img)
    for y in range(6, 256, 14):
        d.rectangle([6, y, 121, y + 4], fill=(170, 174, 178, 255))
        for x in range(12, 116, 26):
            if (x * 7 + y) % 3:
                d.rectangle([x, y - 7, x + 18, y - 1], fill=((184, 140, 88) if (x + y) % 2 else (226, 222, 210)) + (255,))
    d.rectangle([0, 0, 127, 255], outline=(60, 64, 70, 255), width=4)
    return _sq(img)


def _shutter():
    img = _img(64, 64, (150, 154, 158))
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 6):
        d.line([(0, y), (63, y)], fill=(110, 114, 118, 255), width=2)
    return img


VLM_SEE_THROUGH = {"pi_wh_vlm_glass"}
VLM_SHARP = {"pi_wh_hatch_sign", "pi_wh_vlm_plate", "pi_wh_vlm_plate_x", "pi_wh_scr_vlm"}
