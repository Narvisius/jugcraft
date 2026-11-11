"""The Tier Riser's and the Aisle Step's models (Justin's walk through of the Drone Tower, 24 October 2026, note 27: the
amber strips at the risers' foot to run all the way across, glow, and be part of the risers so the chairs don't hit
them; take 1 approved by him).

Where a riser steps down (edge_north, edge_east, ...: nothing beside it there) its face has a channel under the top
edge: the carpet up to it, the amber step light set back in it (full bright, lit by itself), the aluminium nosing over it
flush with the face (a hair proud of the tread). Nothing stands out of the block, so a chair against it in the row below
touches nothing. The light runs the block's whole width and on into the next riser's (its sides are notched where a
channel meets them; the channel's ends are capped where it stops). One model for each set of edges (16), built whole so
the corners where two edges meet mitre. The aisle's half step has the same channel, light and nosing on its front.
Every face at least 0.1 px from any other in its plane (no z-fighting); 1 px is 6.25 cm.
"""
from __future__ import annotations

import itertools

TEX = {"carpet": "jugcraft:block/carpet_tile", "nosing": "jugcraft:block/raised_floor_nosing",
       "light": "jugcraft:block/raised_floor_light", "particle": "jugcraft:block/carpet_tile"}
SIDES = ("north", "east", "south", "west")
LIGHT_Y0, LIGHT_Y1 = 13.3, 14.5      # the light across the channel
NOSE_Y1 = 16.1                       # the nosing's top (0.1 over the tread)
RECESS = 0.3                         # how far the light is set back
LENS = 0.1                           # the light's own thickness
NOSE_D = 1.5                         # the nosing's depth over the tread
BACK = RECESS + LENS                 # the channel's back (the light's back)


def _f(t, **kw):
    return {"texture": f"#{t}", **kw}


def _box(a, b, faces, **extra):
    return {"from": [round(v, 4) for v in a], "to": [round(v, 4) for v in b], "faces": faces, **extra}


# ------------------------------------------------------------------ turning (built for the north side, turned to the others)
def _turn_point(x, z, k):
    for _ in range(k):
        x, z = 16 - z, x
    return x, z


_TURN_FACE = {"north": "east", "east": "south", "south": "west", "west": "north", "up": "up", "down": "down"}


def _turn(e, k):
    """An element built for the north side turned k quarter turns clockwise (to east, south, west)."""
    if k == 0:
        return e
    (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
    ax, az = _turn_point(x0, z0, k)
    bx, bz = _turn_point(x1, z1, k)
    faces = {}
    for name, f in e["faces"].items():
        to = name
        for _ in range(k):
            to = _TURN_FACE[to]
        faces[to] = f
    out = dict(e, faces=faces)
    out["from"] = [round(min(ax, bx), 4), y0, round(min(az, bz), 4)]
    out["to"] = [round(max(ax, bx), 4), y1, round(max(az, bz), 4)]
    return out


def _k(side):
    return SIDES.index(side)


def _left(side):
    """The side at this side's west end as it is built (north: west; east: north ...)."""
    return SIDES[(_k(side) + 3) % 4]


def _right(side):
    return SIDES[(_k(side) + 1) % 4]


# ------------------------------------------------------------------ the riser
def _edge(left_edge, right_edge):
    """The north side stepping down (its face at z 0): carpet to the channel, the light, the nosing. left_edge /
    right_edge: the side at its west / east end steps down too, a corner. A corner is its right-hand side's (this side
    keeps the one at its east end, the side to its west the one at its west end): its carpet, light and nosing run
    round into it and face out round it, the other side's stop against them (so the mitre is closed).
    Where an end is no corner the channel stops at the next block (a riser carrying it on, or anything else covering it):
    its carpet, light and nosing are capped there."""
    la = BACK if left_edge else 0.0             # (the corner at the west end is the side there's: its light covers it)
    lb = 16 - RECESS if right_edge else 16.0
    xa = NOSE_D if left_edge else 0.0
    ca = BACK if left_edge else 0.0
    west = {} if left_edge else {"west": _f("carpet")}
    return [_box((ca, 0, 0), (16, LIGHT_Y0, BACK), {"north": _f("carpet"), "up": _f("carpet"), "east": _f("carpet"), **west}),
            _box((la, LIGHT_Y0, RECESS), (lb, LIGHT_Y1, BACK),
                 {"north": _f("light", uv=[0, 0, 16, 16]), "east": _f("light", uv=[15, 0, 16, 16]),
                  **({} if left_edge else {"west": _f("light", uv=[0, 0, 1, 16])})},
                 shade=False, light_emission=15),
            _box((xa, LIGHT_Y1, 0), (16, NOSE_Y1, NOSE_D),
                 {"north": _f("nosing"), "up": _f("nosing"), "down": _f("nosing"), "south": _f("nosing"), "east": _f("nosing"),
                  **({} if left_edge else {"west": _f("nosing")})})]


def riser(edges):
    """The riser with nosing, channel and light on each side in `edges`: a solid core in two layers (up to the
    nosings, and the nosings' band), each set back on the sides that step down by the channel's depth and the nosing's,
    its faces on the other sides whole (they meet the next block); and each stepping-down side's carpet, light and
    nosing."""
    def span(lo_side, hi_side, d):
        return (d if lo_side in edges else 0.0), (16 - d if hi_side in edges else 16.0)
    els = []
    for (y0, y1, d) in ((0, LIGHT_Y1, BACK), (LIGHT_Y1, 16, NOSE_D)):
        x0, x1 = span("west", "east", d)
        z0, z1 = span("north", "south", d)
        faces = {k: _f("carpet", cullface=k) for k in SIDES if k not in edges}
        if y0 == 0:
            faces["down"] = _f("carpet", cullface="down")
        else:
            faces["up"] = _f("carpet", cullface="up")
        els.append(_box((x0, y0, z0), (x1, y1, z1), faces))
    for side in SIDES:
        if side in edges:
            els += [_turn(e, _k(side)) for e in _edge(_left(side) in edges, _right(side) in edges)]
    return els


def riser_key(edges):
    return "".join(s[0] for s in SIDES if s in edges) or "none"


def all_risers():
    """{key: (edges, elements)} for every set of edges."""
    out = {}
    for n in range(5):
        for combo in itertools.combinations(SIDES, n):
            out[riser_key(combo)] = (set(combo), riser(set(combo)))
    return out


# ------------------------------------------------------------------ the aisle's half step (its front at z 16)
def aisle_step():
    y0, y1, top = 5.3, 6.5, 8.1
    front, back = 16 - RECESS, 16 - BACK
    return [
        _box((0, 0, 0), (16, 8, 16 - NOSE_D), {k: _f("carpet") for k in ("north", "east", "west", "up", "down")}),
        _box((0, 0, 16 - NOSE_D), (16, y0, 16), {k: _f("carpet") for k in ("south", "up", "east", "west", "down")}),
        _box((0, y0, 16 - NOSE_D), (16, y1, back), {k: _f("carpet") for k in ("east", "west")}),
        _box((0, y0, back), (16, y1, front), {"south": _f("light", uv=[0, 0, 16, 16]), "east": _f("light", uv=[15, 0, 16, 16]),
                                               "west": _f("light", uv=[0, 0, 1, 16])}, shade=False, light_emission=15),
        _box((0, y1, 16 - NOSE_D), (16, top, 16), {k: _f("nosing") for k in ("north", "south", "up", "down", "east", "west")}),
    ]
