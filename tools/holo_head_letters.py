"""The Holo Projector's ring of words: "You are being watched" twice round, in a plain bold type (DejaVu Sans Bold;
Justin: "I want it to just be, like, normal text font"), each letter a solid: its outline extruded, the front and back
filled. A glyph is drawn large, its outlines traced (marching squares), thinned, and the faces filled by a Delaunay
triangulation of the outline points kept where the glyph is ink. Units: the letter height (cap/ascender) is 1.

DejaVu fonts are free to use and change (Bitstream Vera licence); only the generated letter meshes go in the mod. Set
JUGCRAFT_HOLO_FONT to the font's path if it is not where Debian puts it.
"""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy.spatial import Delaunay
from skimage import measure

FONT = os.environ.get("JUGCRAFT_HOLO_FONT", "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")
PX = 160          # the drawing's size: pixels a letter height
TEXT = "You are being watched"
_cache = {}


def glyph_image(ch):
    f = ImageFont.truetype(FONT, PX)
    l, t, r, b = f.getbbox(ch)
    w, h = max(1, r - l) + 8, PX * 2
    img = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(img)
    d.text((4 - l, int(PX * 0.6)), ch, font=f, fill=255)
    return np.asarray(img) > 127, f.getlength(ch), int(PX * 0.6)


def simplify(c, tol=1.2):
    return measure.approximate_polygon(c, tolerance=tol)


def glyph(ch, depth=0.18):
    """(vertices (x, y, z), triangles, normals per triangle-corner) in letter heights; the advance; x from 0, the
    baseline at y 0, the front face at z +depth/2 (facing +z)."""
    if ch in _cache:
        return _cache[ch]
    if ch == " ":
        _cache[ch] = (np.zeros((0, 3), np.float32), np.zeros((0, 3), np.int32), 0.32)
        return _cache[ch]
    ink, adv, top = glyph_image(ch)
    f = ImageFont.truetype(FONT, PX)
    ascent, _ = f.getmetrics()
    base = top + ascent          # the baseline's row in the image
    pad = np.pad(ink, 1)
    contours = measure.find_contours(pad.astype(np.float32), 0.5)
    rings = [simplify(c) for c in contours if len(c) > 8]
    pts2 = np.concatenate([r[:-1] for r in rings]) if rings else np.zeros((0, 2))
    tris = []
    if len(pts2) >= 3:
        tri = Delaunay(pts2)
        cen = pts2[tri.simplices].mean(1)
        # keep the triangles whose middle and edge midpoints are ink
        ok = np.ones(len(tri.simplices), bool)
        for p in [cen] + [(pts2[tri.simplices[:, a]] + pts2[tri.simplices[:, b]]) / 2 * 0.8 + cen * 0.2 for a, b in ((0, 1), (1, 2), (2, 0))]:
            rr = np.clip(np.round(p[:, 0]).astype(int), 0, pad.shape[0] - 1)
            cc = np.clip(np.round(p[:, 1]).astype(int), 0, pad.shape[1] - 1)
            ok &= pad[rr, cc]
        tris = tri.simplices[ok]
    # image (row, col) to letter units: x = col, y up from the baseline
    def to_xy(rc):
        return np.stack([(rc[:, 1] - 1 - 4) / PX, (base - (rc[:, 0] - 1)) / PX], -1)
    xy = to_xy(pts2)
    h = depth / 2
    verts, faces = [], []
    n = len(xy)
    # front (+z) and back (-z) faces
    front = np.concatenate([xy, np.full((n, 1), h)], 1)
    back = np.concatenate([xy, np.full((n, 1), -h)], 1)
    verts.append(front)
    verts.append(back)
    for t in tris:
        a, b, c = t
        pa, pb, pc = xy[a], xy[b], xy[c]
        ccw = (pb[0] - pa[0]) * (pc[1] - pa[1]) - (pb[1] - pa[1]) * (pc[0] - pa[0]) > 0
        faces.append((a, b, c) if ccw else (a, c, b))
        faces.append((n + a, n + c, n + b) if ccw else (n + a, n + b, n + c))
    # the side walls along each outline
    off = 2 * n
    k0 = 0
    side = []
    for r in rings:
        m = len(r) - 1
        ring_xy = to_xy(r[:-1])
        # outline direction: make the walls face out of the ink
        area = 0.5 * np.sum(ring_xy[:, 0] * np.roll(ring_xy[:, 1], -1) - np.roll(ring_xy[:, 0], -1) * ring_xy[:, 1])
        for i in range(m):
            i2 = (i + 1) % m
            a, b = ring_xy[i], ring_xy[i2]
            quad = [(a[0], a[1], h), (a[0], a[1], -h), (b[0], b[1], -h), (b[0], b[1], h)]
            base_i = off + len(side) * 4
            side.append(quad)
            # an outline going anticlockwise round ink has the ink on its left: its wall faces right (outwards)
            outer_ccw = area > 0
            mid = (a + b) / 2
            nrm = np.array([b[1] - a[1], -(b[0] - a[0])])
            probe = mid + nrm / max(1e-9, np.linalg.norm(nrm)) * 1.5 / PX
            rr = int(round(base - probe[1] * PX)) + 1
            cc = int(round(probe[0] * PX)) + 1 + 4
            inside = 0 <= rr < pad.shape[0] and 0 <= cc < pad.shape[1] and pad[rr, cc]
            if inside:
                faces.append((base_i, base_i + 3, base_i + 2))
                faces.append((base_i, base_i + 2, base_i + 1))
            else:
                faces.append((base_i, base_i + 1, base_i + 2))
                faces.append((base_i, base_i + 2, base_i + 3))
            del outer_ccw
    if side:
        verts.append(np.array(side, np.float64).reshape(-1, 3))
    v = np.concatenate(verts).astype(np.float32)
    f = np.array(faces, np.int32)
    _cache[ch] = (v, f, adv / PX)
    return _cache[ch]


def ring(text=TEXT, sep=" · ", copies=2):
    """The ring's letters in order: (character, x along the line in letter heights) and the line's length."""
    s = (text + sep) * copies
    out, x = [], 0.0
    for ch in s:
        if ch == "·":
            out.append(("dot", x + 0.08))
            x += 0.28
            continue
        v, f, adv = glyph(ch)
        out.append((ch, x))
        x += adv + 0.02
    return out, x


if __name__ == "__main__":
    total = 0
    for ch in sorted(set(TEXT) - {" "}):
        v, f, adv = glyph(ch)
        total += len(f)
        print(repr(ch), len(v), "verts", len(f), "tris", round(adv, 3))
    items, length = ring()
    n = sum(len(glyph(c)[1]) for c, _ in items if c != "dot")
    print("ring length", round(length, 2), "letter heights;", n, "triangles in the ring")
