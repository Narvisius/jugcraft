"""The Holo Projector: the Tri-Pillar lobby's big hologram (work list item 15, approved by Justin on 23 October 2026 after
six takes; see claude/jugcraft-tripillar-holo-head.md). It replaced the six floating holo panels over the operator
areas (Justin: "get rid of those three hologram things ... make a large one ... a 3x3 projector").

A 3 x 3 floor projector (a steel slab with a lit violet edge, a dark deck, twelve emitters round a ringed lens) throws a
big head into the air over it: his character's face (tools/holo_head.py: a real human head from MakeHuman, CC0, shaped
into her face and an angry glare, with her hair, brows, lashes and choker), 3.4 blocks from chin to crown, its turning
point 5.2 over the floor. Its head and eyes follow the nearest player within 20 blocks; nobody near, it faces where the
projector faces. Round it turns a ring of solid letters, "You are being watched" twice round in a plain bold type
(tools/holo_head_letters.py). The light opens out from the lens and fades away well under the head, so the head looks
thrown up there. client/HoloProjectorRenderer draws it all.

The hologram is see-through without a depth pre-pass: tools/holo_head.visibility works out, for 162 directions, which
of its triangles, eye triangles and hair cards can be seen from that side (only the nearest surface; the hair cards in
front of it), and the renderer draws each one only if it can be seen from its own direction to the camera (within
15 degrees). Everything translucent goes in one sorted batch.

Writes:
- assets/jugcraft/tower_mesh/holo_projector.bin.gz: the housing (the console mesh format, client/ConsoleMesh), its
  texture textures/entity/holo_projector.png;
- assets/jugcraft/tower_mesh/holo_head.bin.gz: the head, eyes, cards, visibility and letters (client/HoloHeadMesh), its
  texture textures/entity/holo_head.png (white, six lock textures, the iris, the beam's streaks and a mote);
- the blockstates, item, lang, loot tables, recipe and pickaxe tag of `holo_projector` and `holo_projector_part`.

One item places it whole (tower/HoloProjectorBlock.java): the master is the middle cell, its other 8 cells are Holo
Projector Parts (no item) that know their offset from it; its `facing` is where the head looks when nobody is near.
"""
from __future__ import annotations

import gzip
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image

import holo_head as HH
import holo_head_hair as HX
import holo_head_letters as HL
import tower_control_console as TCC

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"

ID = "holo_projector"
PART_ID = "holo_projector_part"
NAME = "Holo Projector"
MESH = ASSETS / "tower_mesh/holo_projector.bin.gz"
TEXTURE = ASSETS / "textures/entity/holo_projector.png"
HEAD = ASSETS / "tower_mesh/holo_head.bin.gz"
HEAD_TEXTURE = ASSETS / "textures/entity/holo_head.png"

CX = CZ = 0.5            # the projector's middle (the master cell's middle)
HALF = 1.5               # the slab's half width (3 cells)
SLAB = 0.20              # the slab's top
DECK_HALF, DECK = 1.38, 0.26     # the dark deck inset on it, its top
EMIT_R = 1.12            # the twelve emitters' circle
LENS_R = 0.95
Y = np.array([0, 1.0, 0])
ATLAS = (256, 256)
HEAD_ATLAS = (512, 256)
GROUPS = ("swept", "bun", "loose", "bangs", "side", "brows")
HEAD_MAGIC = 0x4A484844  # "JHHD"


# ------------------------------------------------------------------ the housing

def _lens(size=128):
    yy, xx = np.mgrid[0:size, 0:size] / (size - 1) * 2 - 1
    r = np.hypot(xx, yy)
    a = np.zeros((size, size, 4), np.float32)
    glow = np.clip(1 - r, 0, 1) ** 0.6
    rings = 0.5 + 0.5 * np.cos(r * 60)
    a[..., 0] = 0.30 + 0.55 * glow
    a[..., 1] = 0.18 + 0.35 * glow
    a[..., 2] = 0.55 + 0.45 * glow
    a[..., :3] *= (0.75 + 0.25 * rings)[..., None] * 1.4
    a[..., 3] = (r <= 1).astype(np.float32)
    return Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "RGBA")


def tiles():
    return {"steel": TCC._noise((52, 54, 60), 4, 21, size=64), "deck": TCC._noise((26, 27, 31), 3, 22, size=64),
            "emitter": TCC._solid((205, 160, 255)), "edge": TCC._solid((150, 90, 255)), "lens": _lens()}


NOISE = {"steel", "deck"}


class Atlas:
    """The housing's tiles on one sheet (as tools/drone_display_case.Atlas)."""

    def __init__(self, t):
        self.img = Image.new("RGBA", ATLAS, (0, 0, 0, 0))
        self.box = {}
        x = y = shelf = 0
        for name, im in sorted(t.items(), key=lambda kv: (-kv[1].height, kv[0])):
            if x + im.width > ATLAS[0]:
                x, y, shelf = 0, y + shelf + 2, 0
            if y + im.height > ATLAS[1]:
                raise ValueError("atlas full")
            self.img.paste(im, (x, y))
            self.box[name] = (x, y, im.width, im.height)
            x += im.width + 2
            shelf = max(shelf, im.height)

    def uv(self, name, u, v):
        x, y, w, h = self.box[name]
        u = min(max(u, 0.0), 1.0)
        v = min(max(v, 0.0), 1.0)
        return ((x + 0.5 + u * (w - 1)) / ATLAS[0], (y + 0.5 + v * (h - 1)) / ATLAS[1])


def box(m, lo, hi, mat, faces="nsewud", lit=False):
    lo, hi = np.asarray(lo, float), np.asarray(hi, float)
    m.obox((lo + hi) / 2, (1, 0, 0), (0, 1, 0), *((hi - lo) / 2), mat, faces=faces, lit=lit)


def housing():
    m = TCC.Mesh()
    # the slab and the dark deck inset on it
    box(m, (CX - HALF, 0, CZ - HALF), (CX + HALF, SLAB, CZ + HALF), "steel", faces="nsewu")
    box(m, (CX - DECK_HALF, SLAB, CZ - DECK_HALF), (CX + DECK_HALF, DECK, CZ + DECK_HALF), "deck", faces="nsewu")
    # the lit violet strip round the slab's sides
    for x0, z0, x1, z1 in ((-1.5, -1.52, 1.5, -1.5), (-1.5, 1.5, 1.5, 1.52), (-1.52, -1.5, -1.5, 1.5), (1.5, -1.5, 1.52, 1.5)):
        box(m, (CX + x0, 0.06, CZ + z0), (CX + x1, 0.12, CZ + z1), "edge", faces="nsewud", lit=True)
    # the twelve emitters round the lens
    for k in range(12):
        a = k * math.pi / 6
        x, z = CX + math.cos(a) * EMIT_R, CZ + math.sin(a) * EMIT_R
        box(m, (x - 0.07, DECK, z - 0.07), (x + 0.07, DECK + 0.04, z + 0.07), "emitter", faces="nsewu", lit=True)
    # the ringed lens, full bright, a little over the deck
    y = DECK + 0.002
    m.oquad([(CX - LENS_R, y, CZ - LENS_R), (CX + LENS_R, y, CZ - LENS_R), (CX + LENS_R, y, CZ + LENS_R), (CX - LENS_R, y, CZ + LENS_R)], Y,
            "lens", uvf=[(0, 0), (1, 0), (1, 1), (0, 1)], lit=True)
    return m


def _uvs(atlas, q):
    mat = q["mat"]
    if mat in NOISE and q["uvf"] is None:
        P_ = q["P"]
        sw = min(np.linalg.norm(P_[1] - P_[0]) * 0.5, 0.9)
        sh = min(np.linalg.norm(P_[3] - P_[0]) * 0.5, 0.9)
        TCC._seed[0] = (TCC._seed[0] * 1103515245 + 12345) & 0x7FFFFFFF
        s = TCC._seed[0]
        ou = (s % 997) / 997 * (1 - sw)
        ov = ((s // 997) % 991) / 991 * (1 - sh)
        f = [(ou, ov), (ou + sw, ov), (ou + sw, ov + sh), (ou, ov + sh)]
    else:
        f = q["uvf"] or [(0.25, 0.25), (0.75, 0.25), (0.75, 0.75), (0.25, 0.75)]
    return [atlas.uv(mat, u, v) for u, v in f]


def write_mesh(m, atlas, path):
    """The console's mesh format ("JTCM" 1, client/ConsoleMesh); no live faces, no step edges."""
    path.parent.mkdir(parents=True, exist_ok=True)
    TCC._seed[0] = 0
    buf = bytearray(struct.pack(">iii", TCC.MAGIC, 1, len(m.quads)))
    for q in m.quads:
        P_ = q["P"]
        uv = _uvs(atlas, q)
        nb = [int(round(max(-1, min(1, v)) * 127)) for v in q["n"]]
        sh = q.get("shade", [1.0] * 4)
        buf += struct.pack(">b", q["kind"])
        for k in (0, 3, 2, 1):
            buf += struct.pack(">fffbbbffB", P_[k][0], P_[k][1], P_[k][2], nb[0], nb[1], nb[2], uv[k][0], uv[k][1],
                               int(round(255 * max(0.0, min(1.0, sh[k])))))
    buf += struct.pack(">i", 0)
    buf += struct.pack(">i", 0)
    for mat in ("steel", "edge", "steel"):
        u0, v0 = atlas.uv(mat, 0.1, 0.1)
        u1, v1 = atlas.uv(mat, 0.9, 0.9)
        buf += struct.pack(">ffff", u0, v0, u1, v1)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))


# ------------------------------------------------------------------ the head's texture

# (x, y, w, h) of each tile in holo_head.png (client/HoloHeadMesh has the same)
WHITE = (0, 0, 16, 16)
CARD_TILES = [(20 + i * 44, 0, 40, 128) for i in range(6)]
IRIS = (288, 0, 128, 128)
STREAK = (424, 0, 64, 32)
MOTE = (424, 40, 16, 16)
IRIS_DEG, PUPIL_DEG, IRIS_TILE_DEG = 38.0, 14.0, 60.0


def _iris(size=128):
    """The eye seen straight on, out to 60 degrees from the gaze (the tile's edge): the pupil to 14, the iris to 38 with
    fine radial fibres and a dark ring at its edge, then the white."""
    yy, xx = np.mgrid[0:size, 0:size] + 0.5
    dx, dy = (xx - size / 2) / (size / 2), (yy - size / 2) / (size / 2)
    deg = np.hypot(dx, dy) * IRIS_TILE_DEG
    ang = np.arctan2(dy, dx)
    rgb = np.zeros((size, size, 3), np.float32)
    white = np.array([225, 232, 255]) / 255
    iris = np.array([90, 225, 255]) / 255
    pupil = np.array([30, 10, 60]) / 255
    fibre = 0.82 + 0.18 * np.sin(ang * 47) * np.sin(ang * 13 + 1.7)
    t = np.clip((deg - PUPIL_DEG) / (IRIS_DEG - PUPIL_DEG), 0, 1)
    iris_c = iris[None, None, :] * (fibre * (0.75 + 0.35 * t) * np.where(t > 0.85, 0.6, 1.0))[..., None]
    rgb[:] = white
    rgb = np.where((deg < IRIS_DEG)[..., None], iris_c, rgb)
    rgb = np.where((deg < PUPIL_DEG)[..., None], pupil[None, None, :], rgb)
    a = np.ones((size, size), np.float32)
    return np.concatenate([np.clip(rgb, 0, 1), a[..., None]], -1)


def _streak(w=64, h=32):
    """The beam's cone: streaks round it, fading out going up (row 0 the top, at the cone's end; the last row at the lens)."""
    uu = np.linspace(0, 1, w)[None, :]
    hh = np.linspace(1, 0, h)[:, None]
    t = np.zeros((h, w, 4), np.float32)
    t[..., 0], t[..., 1], t[..., 2] = 1.0, 0.92, 1.0
    t[..., 3] = 0.2 * (1 - hh) ** 1.8 * (0.55 + 0.45 * np.cos(uu * 2 * np.pi * 2) ** 2)
    return t


def _mote(s=16):
    yy, xx = np.mgrid[0:s, 0:s] / (s - 1) * 2 - 1
    t = np.zeros((s, s, 4), np.float32)
    t[..., 0], t[..., 1], t[..., 2] = 0.95, 0.85, 1.0
    t[..., 3] = np.clip(1 - (np.abs(xx) + np.abs(yy)), 0, 1)
    return t


def head_texture():
    img = np.zeros((HEAD_ATLAS[1], HEAD_ATLAS[0], 4), np.float32)

    def put(tile, a):
        x, y, w, h = tile
        assert a.shape[:2] == (h, w), (tile, a.shape)
        img[y:y + h, x:x + w] = a
    put(WHITE, np.ones((16, 16, 4), np.float32))
    for i, tile in enumerate(CARD_TILES):
        t = HX.card_texture_dark(i, under=0.55)
        t[..., :3] = np.clip(t[..., :3] / 1.4, 0, 1)      # brightness 0..1.4 into 0..1 (the renderer scales it back)
        put(tile, t)
    put(IRIS, _iris())
    put(STREAK, _streak())
    put(MOTE, _mote())
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), "RGBA")


# ------------------------------------------------------------------ the head's data

def eye_sphere():
    ev, ef, el, _ = HX.eye()
    return ev.astype(np.float32), ef.astype(np.int32)


def card_strips(cards):
    """Each card as (group, variant, left corners, right corners, normals) along it."""
    out = []
    for gi, g in enumerate(GROUPS):
        c = cards[g]
        p = c["p"].astype(np.float64)
        w = c["w"].astype(np.float64)
        n = c["n"].astype(np.float64)
        t = np.gradient(p, axis=1)
        t /= np.maximum(np.linalg.norm(t, axis=2, keepdims=True), 1e-12)
        side = np.cross(n, t)
        side /= np.maximum(np.linalg.norm(side, axis=2, keepdims=True), 1e-12)
        L = p - side * (w[..., None] / 2)
        R = p + side * (w[..., None] / 2)
        for i in range(len(p)):
            out.append((gi, int(c["var"][i]), L[i], R[i], n[i]))
    return out


def head_visibility(head, strips, ev, ef):
    """The seen-from bits of every primitive: the head's triangles, both eyes' triangles, the cards' quads (in that order),
    for the 162 directions; each direction's head triangles grown by one ring (no pinholes at silhouettes)."""
    v, f, k, fa = head["v"], head["f"], head["k"], head["fa"]
    tri = v[f]
    occ = (k[f].max(1) == 0) & (fa[f].mean(1) >= 0.5)
    eye_tri = np.concatenate([(ev * head["eye_r"] + c)[ef] for c in head["eyes"]])
    quads = np.array([(L[s], R[s], R[s + 1], L[s + 1]) for _, _, L, R, _ in strips for s in range(len(L) - 1)])
    card_tri = np.concatenate([quads[:, [0, 1, 2]], quads[:, [0, 2, 3]]])
    dirs = HH.icosphere(2)
    vis = HH.visibility(np.concatenate([tri[occ], eye_tri]), np.concatenate([tri[~occ], card_tri]), dirs)
    n_occ, n_eye, nq = int(occ.sum()), len(eye_tri), len(quads)
    head_vis = np.zeros((len(dirs), len(f)), bool)
    head_vis[:, np.nonzero(occ)[0]] = vis[:, :n_occ]
    rest = vis[:, n_occ + n_eye:]
    head_vis[:, np.nonzero(~occ)[0]] = rest[:, :(~occ).sum()]
    head_vis = HH.dilate(head_vis, f, 1)
    cv = rest[:, (~occ).sum():]
    card_vis = cv[:, :nq] | cv[:, nq:]
    return dirs, np.concatenate([head_vis, vis[:, n_occ:n_occ + n_eye], card_vis], 1)


def letters():
    """The ring's glyphs (the separator dot as its own little diamond) and its items: (glyph index, x) and its length."""
    items, length = HL.ring()
    glyphs, index, ring = [], {}, []
    for ch, x in items:
        if ch not in index:
            if ch == "dot":
                gv = np.array([(0, 0.25, 0.05), (0.12, 0.37, 0.05), (0, 0.49, 0.05), (-0.12, 0.37, 0.05)]) + np.array([0.06, 0, 0])
                gf = np.array([(0, 1, 2), (0, 2, 3)])
                adv = 0.12
                code = 0xB7
            else:
                gv, gf, adv = HL.glyph(ch)
                code = ord(ch)
            if len(gf):
                index[ch] = len(glyphs)
                glyphs.append((code, adv, np.asarray(gv, np.float32), np.asarray(gf, np.int32)))
        if ch in index:
            ring.append((index[ch], x))
    return glyphs, ring, length


def write_head(path):
    head = HH.build()
    ev, ef = eye_sphere()
    strips = card_strips(head["cards"])
    dirs, vis = head_visibility(head, strips, ev, ef)
    glyphs, ring, length = letters()
    v, f = head["v"], head["f"]
    assert len(v) < 65536 and len(ev) < 65536
    buf = bytearray(struct.pack(">ii", HEAD_MAGIC, 1))
    # the head
    buf += struct.pack(">i", len(v))
    nb = np.clip(np.round(head["n"] * 127), -127, 127).astype(np.int8)
    sh = np.clip(np.round(head["sh"] * 100), 0, 255).astype(np.uint8)
    lw = np.clip(np.round(head["lw"] * 255), 0, 255).astype(np.uint8)
    fa = np.clip(np.round(head["fa"] * 255), 0, 255).astype(np.uint8)
    for i in range(len(v)):
        buf += struct.pack(">fffbbbBBBBB", float(v[i, 0]), float(v[i, 1]), float(v[i, 2]), int(nb[i, 0]), int(nb[i, 1]), int(nb[i, 2]),
                           int(head["l"][i]), int(head["k"][i]), int(sh[i]), int(lw[i]), int(fa[i]))
    buf += struct.pack(">i", len(f))
    buf += np.asarray(f, ">u2").tobytes()
    # the eyes
    buf += struct.pack(">f", float(head["eye_r"]))
    for c in head["eyes"]:
        buf += struct.pack(">fff", *map(float, c))
    buf += struct.pack(">i", len(ev))
    buf += np.asarray(ev, ">f4").tobytes()
    buf += struct.pack(">i", len(ef))
    buf += np.asarray(ef, ">u2").tobytes()
    # the cards
    buf += struct.pack(">i", len(strips))
    for gi, var, L, R, n in strips:
        buf += struct.pack(">BBH", gi, var, len(L))
        nb = np.clip(np.round(n / np.maximum(np.linalg.norm(n, axis=1, keepdims=True), 1e-9) * 127), -127, 127).astype(np.int8)
        for s in range(len(L)):
            buf += struct.pack(">ffffffbbb", *map(float, L[s]), *map(float, R[s]), *map(int, nb[s]))
    # what can be seen from where
    buf += struct.pack(">i", len(dirs))
    buf += np.asarray(dirs, ">f4").tobytes()
    nprim = vis.shape[1]
    words = (nprim + 63) // 64
    buf += struct.pack(">i", nprim)
    for row in vis:
        bits = np.zeros(words * 64, np.uint8)
        bits[:nprim] = row
        # bit i of word i >> 6 is primitive i (least significant first)
        packed = np.packbits(bits.reshape(words, 64)[:, ::-1], axis=1).view(">u8").reshape(-1)
        buf += packed.astype(">u8").tobytes()
    # the letters
    buf += struct.pack(">i", len(glyphs))
    for code, adv, gv, gf in glyphs:
        buf += struct.pack(">Hfi", code, float(adv), len(gv))
        buf += np.asarray(gv, ">f4").tobytes()
        buf += struct.pack(">i", len(gf))
        buf += np.asarray(gf, ">u2").tobytes()
    buf += struct.pack(">i", len(ring))
    for gi, x in ring:
        buf += struct.pack(">Hf", gi, float(x))
    buf += struct.pack(">f", float(length))
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(bytes(buf))
    return {"verts": len(v), "tris": len(f), "eye_tris": len(ef), "cards": len(strips), "dirs": len(dirs), "prims": nprim,
            "glyphs": len(glyphs), "ring": len(ring), "seen": float(vis.mean())}


# ------------------------------------------------------------------ the blocks' data

def write_data():
    j = TCC._json
    j(ASSETS / "blockstates" / f"{ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(ASSETS / "blockstates" / f"{PART_ID}.json", {"variants": {"": {"model": f"jugcraft:block/{ID}"}}})
    j(ASSETS / "models/block" / f"{ID}.json", {"textures": {"particle": "jugcraft:block/carbon_composite_panel"}, "elements": []})
    j(ASSETS / "models/item" / f"{ID}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{ID}"}})
    j(ASSETS / "items" / f"{ID}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{ID}"}})
    j(DATA / "jugcraft/loot_table/blocks" / f"{ID}.json", TCC._loot(ID))
    j(DATA / "jugcraft/loot_table/blocks" / f"{PART_ID}.json",
      {"type": "minecraft:block", "pools": [], "random_sequence": f"jugcraft:blocks/{PART_ID}"})
    # No recipe: a hologram, the Drone Tower's own (tower/Holograms, Justin 28 October 2026); apply() removes an old one.
    import advanced_crafting
    advanced_crafting.apply(DATA / "jugcraft/recipe", [ID])
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang[f"block.jugcraft.{PART_ID}"] = NAME
    lang[f"message.jugcraft.{ID}.blocked"] = "The Holo Projector needs a clear space 3 by 3 blocks"
    if sorted_before:
        lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    for i in (ID, PART_ID):
        if f"jugcraft:{i}" not in t["values"]:
            t["values"].append(f"jugcraft:{i}")
    p.write_text(json.dumps(t, indent=2) + "\n")


def write_all():
    atlas = Atlas(tiles())
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    atlas.img.save(TEXTURE, optimize=True)
    m = housing()
    TCC.bake(m, floor_y=0.0, res=1 / 16, reach=0.6, rays=16)
    write_mesh(m, atlas, MESH)
    head_texture().save(HEAD_TEXTURE, optimize=True)
    stats = write_head(HEAD)
    write_data()
    import item_icons
    item_icons.draw(ID).save(ASSETS / f"textures/item/{ID}.png")
    return m, stats


if __name__ == "__main__":
    m, stats = write_all()
    kinds = [q["kind"] for q in m.quads]
    print(len(m.quads), "housing quads", {k: kinds.count(k) for k in sorted(set(kinds))})
    print(stats)
