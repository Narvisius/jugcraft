"""Framed Glass (nuclear/FramedGlassBlock.java): an aluminium storefront partition, as in the plants' lobbies and the
shift supervisor's office, made to meet the glass doors flush.

Real storefront framing is a 2" x 4-1/2" extruded aluminium section (dark bronze or clear anodized) with the glass
set in the middle of its depth, and the entrance doors hang in the same frame, in the same plane as the glass. Here
the panel is 3 px deep like a door's leaf and sits at the edge of its block nearest the person placing it, as a
door does, so glass and doors placed facing the same way line up exactly; placed beside a door (or on top of other
framed glass) it takes that door's plane by itself.

The glass runs on unbroken (Justin: as continuous as possible, just glass): no mullions or rails between panels in
a run or a stack, so a wall of it is one sheet. The frame shows only where the glass needs support: a 2 px jamb where
it ends (against a wall, a door or nothing), a 3 px corner post where the run turns a corner inside the block (with a
short stub of glass along the side to the next run), two jambs where runs meet at an outside corner, a 2 px sill
and head along the bottom and top of the sheet, and round a door set in it (the jambs either side up to the door's
head, the head across over it, glass above). Glass: 1 px thick, clear with only a faint green-blue tint (no sheen),
set in the middle of the frame's depth. Models are drawn facing north (the panel along the south edge, z 13..16); the
blockstate turns them.

The glass door (tools/plant_interior.py) is drawn from the same parts: a 3D dark bronze frame with the same glass.
"""
import numpy as np
from PIL import Image, ImageDraw

import nuclear_materials as M

MOD = "jugcraft"
BLOCK = "framed_glass"
DISPLAY = "Framed Glass"
FRAME = (70, 66, 60)     # dark bronze anodized, as the glass doors
Z0, Z1 = 13, 16          # the panel's depth (facing north): the same 3 px as a door's leaf
GZ0, GZ1 = 14, 15        # the glass, in the middle of the frame's depth
# An end: join (the glass carries on, no frame), jamb (2 px), corner (a 3 px post and a stub round the corner), head
# (the glass carries on, but the next block is over a door: the corner of the door's head frame is here).
ENDS = {"join": 0, "jamb": 2, "corner": 3, "head": 0}
BOTTOMS = ("sill", "glass", "door")   # a sill under it, the glass carrying on below, or a door below (a sill as well)
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
CRAFTING = {BLOCK: (["GNG", "GNG"], {"N": "minecraft:iron_nugget", "G": "minecraft:glass_pane"}, 4)}

FRAME_TEX = "block/fg_frame"
GLASS_TEX = "block/fg_glass"


def _box(f, t, faces):
    return {"from": list(f), "to": list(t), "faces": faces}


def _frame_faces(rot=0, skip=()):
    """All six faces in the frame texture (UVs follow position, so the grain runs on across members); rot turns the
    grain to run along a horizontal member."""
    faces = {}
    for face in ("north", "south", "east", "west", "up", "down"):
        if face in skip:
            continue
        d = {"texture": "#frame"}
        if rot and face in ("north", "south"):
            d["rotation"] = rot
        faces[face] = d
    return faces


def _glass(f, t, axis="z"):
    """A pane of glass between f and t: only its two big faces (its edges are inside the frame), the whole glass
    texture stretched over each."""
    faces = ("north", "south") if axis == "z" else ("west", "east")
    return _box(f, t, {face: {"texture": "#glass", "uv": [0, 0, 16, 16]} for face in faces})


def panel_elements(left, right, up, bottom, z0=Z0, z1=Z1, gz0=GZ0, gz1=GZ1):
    """The elements of one block of framed glass facing north: left/right are the ends' kinds (ENDS), up whether the
    glass carries on above, bottom one of BOTTOMS."""
    lw, rw = ENDS[left], ENDS[right]
    down = bottom == "glass"
    bot = 0 if down else 2
    top = 16 if up else 14
    els = [_glass((lw, bot, gz0), (16 - rw, top, gz1))]
    if lw:
        els.append(_box((0, 0, z0), (lw, 16, z1), _frame_faces()))
    if rw:
        els.append(_box((16 - rw, 0, z0), (16, 16, z1), _frame_faces()))
    if not down:
        els.append(_box((lw, 0, z0), (16 - rw, bot, z1), _frame_faces(90)))     # sill (or a door's head)
    if not up:
        els.append(_box((lw, top, z0), (16 - rw, 16, z1), _frame_faces(90)))    # head
    for side, kind in (("left", left), ("right", right)):
        x0, x1 = (0, 2) if side == "left" else (14, 16)
        if kind == "head":
            # The corner of the head over a door beside this block: the door's jamb below stops at its head, which
            # runs across to here; glass carries on above and beside (the glass runs on inside the piece, unseen).
            els.append(_box((x0, 0, z0), (x1, 2, z1), _frame_faces(90)))
        if kind == "corner":
            # The run turns a corner here: a stub of the partition along the side, from the corner post back to the
            # next run, whose glass carries straight on from it.
            sx0, sx1 = (0, 3) if side == "left" else (13, 16)
            els.append(_glass((sx0 + 1, bot, 0), (sx1 - 1, top, z0), axis="x"))
            if not down:
                els.append(_box((sx0, 0, 0), (sx1, bot, z0), _frame_faces()))
            if not up:
                els.append(_box((sx0, top, 0), (sx1, 16, z0), _frame_faces()))
    return els


def model_name(left, right, up, bottom):
    return f"{BLOCK}_{left}_{right}_{'u' if up else 'x'}{'g' if bottom == 'glass' else 's'}"


def textures(rid):
    return {"frame": rid(FRAME_TEX), "glass": {"sprite": rid(GLASS_TEX), "force_translucent": True},
            "particle": rid(FRAME_TEX)}


def models(rid):
    out = {}
    for left in ENDS:
        for right in ENDS:
            for up in (False, True):
                for bottom in ("sill", "glass"):
                    out[model_name(left, right, up, bottom)] = {
                        "parent": "minecraft:block/block", "textures": textures(rid),
                        "elements": panel_elements(left, right, up, bottom)}
    # The item: one framed lite, in the middle of the block.
    out[f"{BLOCK}_item"] = {"parent": "minecraft:block/block", "textures": textures(rid),
                            "elements": panel_elements("jamb", "jamb", False, "sill", 6.5, 9.5, 7.5, 8.5)}
    return out


def blockstate(rid):
    variants = {}
    for facing, y in FACINGS.items():
        for left in ENDS:
            for right in ENDS:
                for up in (False, True):
                    for bottom in BOTTOMS:
                        key = f"bottom={bottom},facing={facing},left={left},right={right},up={str(up).lower()}"
                        v = {"model": rid(f"block/{model_name(left, right, up, bottom)}")}
                        if y:
                            v["y"] = y
                        variants[key] = v
    return {"variants": variants}


# ---------------------------------------------------------------- the glass door, from the same parts

STILE, TOP_RAIL, BOTTOM_RAIL, BAR_Y = 2, 2, 4, 14   # px: a narrow stile storefront door (stiles and top rail as the
# framing's 2 px members, a deeper bottom rail), a push bar at hand height


def door_elements(half, hinge):
    """One half of the glass door, facing east like vanilla's door models (the leaf along the west edge, x 0..3):
    stiles each side, a top rail on the upper half, a deep bottom rail on the lower, the glass between, and on the
    lower half a push bar across both faces at hand height. hinge: which side the hinge is (z 0 for left)."""
    els = []
    lo = BOTTOM_RAIL if half == "bottom" else 0
    hi = 16 - TOP_RAIL if half == "top" else 16
    hinge_w, latch_w = STILE, STILE
    w0, w1 = (hinge_w, latch_w) if hinge == "left" else (latch_w, hinge_w)
    # z runs across the door (0..16); the left hinge is at z 0 seen from the west (the door's front).
    els.append(_box((0, 0, 0), (3, 16, w0), _frame_faces()))
    els.append(_box((0, 0, 16 - w1), (3, 16, 16), _frame_faces()))
    if half == "bottom":
        els.append(_box((0, 0, w0), (3, lo, 16 - w1), _frame_faces(90)))
    else:
        els.append(_box((0, hi, w0), (3, 16, 16 - w1), _frame_faces(90)))
    # The glass is one lite over both halves: each half takes its share of the texture, so the reflections run on.
    lite = 32 - TOP_RAIL - BOTTOM_RAIL
    split = (16 - BOTTOM_RAIL) / lite * 16
    v0, v1 = (split, 16) if half == "bottom" else (0, split)
    glass = _glass((1, lo, w0), (2, hi, 16 - w1), axis="x")
    for face in glass["faces"].values():
        face["uv"] = [0, round(v0, 3), 16, round(v1, 3)]
    els.append(glass)
    if half == "bottom":
        # Push bars across both faces (standing off the stiles on small brackets).
        for x0, x1, bx0, bx1 in ((-1, -0.5, -0.5, 0), (3.5, 4, 3, 3.5)):
            els.append(_box((x0, BAR_Y, 2), (x1, BAR_Y + 1, 14), {f: {"texture": "#bar"} for f in
                                                                   ("north", "south", "east", "west", "up", "down")}))
            for z in (2.5, 12.5):
                els.append(_box((bx0, BAR_Y + 0.25, z), (bx1, BAR_Y + 0.75, z + 1), {f: {"texture": "#bar"} for f in
                                                                                     ("north", "south", "up", "down", "east", "west")}))
    return els


def door_models(rid, door="glass_door"):
    tex = textures(rid)
    tex["bar"] = rid("block/fg_bar")
    out = {}
    for half in ("bottom", "top"):
        for hinge in ("left", "right"):
            els = door_elements(half, hinge)
            for opened in ("", "_open"):
                # Vanilla turns the open door with the blockstate; its models only mirror the texture, so the open
                # model is the same leaf (the hinge side swaps, as the door's front turns to the other side).
                out[f"{door}_{half}_{hinge}{opened}"] = {
                    "parent": "minecraft:block/block", "textures": tex,
                    "elements": door_elements(half, ("right" if hinge == "left" else "left") if opened else hinge)}
    return out


# ---------------------------------------------------------------- textures

def _frame_texture():
    """Dark bronze anodized aluminium: an even, slightly satin colour with a fine lengthwise grain."""
    img = M.brushed(FRAME, 7101, "v", dirt=0.08)
    a = np.asarray(img).astype(np.float32)
    a[..., :3] = FRAME + (a[..., :3] - FRAME) * 0.45   # anodizing is far smoother than brushed steel
    return M.stylize(Image.fromarray(a.clip(0, 255).astype(np.uint8), "RGBA"), colours=10)


def _bar_texture():
    """The push bars: satin stainless."""
    return M.stylize(M.brushed((176, 180, 184), 7102, "h", dirt=0.05), colours=10)


def _glass_texture(size=64):
    """Clear glass: an even, faint green-blue tint and nothing else (Justin: no sheen), so stacked panes read as one
    and the tint is the same everywhere. Partial alpha (drawn translucent, the model asks for it)."""
    out = np.zeros((size, size, 4), np.uint8)
    out[..., :3] = (192, 216, 220)
    out[..., 3] = 38
    return Image.fromarray(out, "RGBA")


def door_icon(size=128):
    """The glass door's item: the door seen from the front, two blocks tall in the icon's height (as vanilla's), the
    dark bronze frame round a pale glass lite with the push bar across."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    px = (size - 4) / 32                   # model px to icon px (32 model px of door in the icon's height)
    x0, x1 = round(size / 2 - 8 * px), round(size / 2 + 8 * px)
    y0, y1 = 2, size - 2
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=FRAME + (255,))
    gx0, gx1 = x0 + round(STILE * px), x1 - round(STILE * px)
    gy0, gy1 = y0 + round(TOP_RAIL * px), y1 - round(BOTTOM_RAIL * px)
    d.rectangle([gx0, gy0, gx1 - 1, gy1 - 1], fill=(176, 204, 210, 255))
    d.line([gx0 + 3, gy1 - 6, gx1 - 6, gy0 + 3], fill=(206, 226, 230, 255), width=3)   # a glint, so it reads as glass
    by = y1 - round((BAR_Y + 0.5) * px)
    d.rectangle([gx0 - 2, by - 2, gx1 + 1, by + 2], fill=(190, 194, 198, 255))
    return M.stylize(img, 12)


def draw_textures():
    return {"fg_frame": _frame_texture(), "fg_glass": _glass_texture(), "fg_bar": _bar_texture()}


def write_assets(write, rid, assets, lang):
    lang[f"block.{MOD}.{BLOCK}"] = DISPLAY
    for name, model in models(rid).items():
        write(assets / "models" / "block" / f"{name}.json", model)
    write(assets / "blockstates" / f"{BLOCK}.json", blockstate(rid))
    write(assets / "items" / f"{BLOCK}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{BLOCK}_item")}})


# ---------------------------------------------------------------- connections (the same rules as FramedGlassBlock)

DIRS = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
CW = {"north": "east", "east": "south", "south": "west", "west": "north"}
CCW = {v: k for k, v in CW.items()}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}


def connections(at, x, y, z):
    """The state of the framed glass at (x, y, z) from its neighbours: at(x, y, z) gives ("glass", facing, bottom) for
    framed glass (bottom: its BOTTOMS value), ("door", facing) for a door, or None. Returns (left, right, up, bottom).

    The glass carries straight on into framed glass facing the same way and into a corner stub of the next block; the
    run turns a corner here when the block in front is framed glass at right angles (a corner post); anything else at
    an end (a wall, a door, nothing) gets a jamb."""
    here = at(x, y, z)
    f = here[1]

    def near(d):
        dx, dz = DIRS[d]
        return at(x + dx, y, z + dz)

    def glass(n):
        return n is not None and n[0] == "glass"

    front = near(f)
    below = at(x, y - 1, z)
    bottom = "glass" if glass(below) and below[1] == f else "door" if below is not None and below[0] == "door" else "sill"

    def end(side, corner_when):
        if glass(front) and front[1] == corner_when:
            return "corner"          # the run turns here: a corner post and a stub to the next run
        n = near(side)
        if not glass(n):
            return "jamb"
        if n[1] == f:
            # Carries straight on; if that block is over a door and this one is not, the door's head frame ends here.
            return "head" if n[2] == "door" and bottom == "glass" else "join"
        if n[1] == OPP[side] and n[1] not in (f, OPP[f]):
            return "join"            # the next block's corner stub carries on into this one
        return "jamb"

    up = at(x, y + 1, z)
    return end(CCW[f], CW[f]), end(CW[f], CCW[f]), glass(up) and up[1] == f, bottom


def bottom_of(at, x, y, z):
    """Just the bottom value (what the neighbours need to know of a block)."""
    f = at(x, y, z)[1]
    below = at(x, y - 1, z)
    if below is not None and below[0] == "glass" and below[1] == f:
        return "glass"
    return "door" if below is not None and below[0] == "door" else "sill"


def state(at, x, y, z):
    left, right, up, bottom = connections(at, x, y, z)
    f = at(x, y, z)[1]
    return f"{MOD}:{BLOCK}[bottom={bottom},facing={f},left={left},right={right},up={str(up).lower()}]"
