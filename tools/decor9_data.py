"""JSON resources for the ninth batch of Halloween decorations, the yard and porch, from tools/agriculture.py: the Yard
Inflatables, the Animatronic Porch Witch, Grasping Hands, the Poseable Skeleton, Bone Wind Chimes, the Weathervanes,
the Spooky Sign, the Haunted Archway and the Dead Hollow Tree; their names, messages, loot and tags, and the quads the
client draws the moving parts from (assets/jugcraft/decor9_quads.json).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files and the earlier
batches' (tools/decor_data.py to decor8_data.py). Block model rotations are right-handed: about x, a positive angle
turns +y toward +z; about z, +x toward +y. Block models only turn by 22.5 or 45 degrees; quads may turn by any angle.

Everything is modelled facing north (its front toward -z). The Inflatables, the Porch Witch and the Poseable Skeleton
stand two blocks tall: the inflatables' blower and the witch's pot and robe are cut into one model per half (`split`);
the skeleton's whole body is drawn by its lower half (block models may reach 32 pixels up). The Haunted Archway and the
Dead Hollow Tree are props of several blocks: the archway is cut into one model per block (`split`), the tree's boxes
are given to the block their middle is in (`assign`), so its branches can reach out over the air beside it.
"""
import math

from agriculture import (INFLATABLES, PORCH_WITCH, GRASPING_HANDS, POSEABLE_SKELETON, WIND_CHIMES, WEATHERVANES, SPOOKY_SIGN,
                         HAUNTED_ARCHWAY, DEAD_TREE, inflatable, weathervane)
from decor_data import MOD, HORIZONTAL, rid, turned, box, block_model, self_drop
from decor3_data import fitted
from decor6_data import quads
from decor7_data import scaled
from decor8_data import split
from halloween_data import match_block

# Quads drawn with these textures are drawn cut out (their see-through pixels left out).
CUTOUT = {"weathervane_bat", "weathervane_witch"}
# Archway cells (right, up) in part order; part 0 is the master, the left pillar's foot as the player sees it.
ARCHWAY_CELLS = [(0, 0), (0, 1), (0, 2), (1, 2), (2, 2), (2, 1), (2, 0)]
# The parts with a lantern.
ARCHWAY_LANTERNS = (1, 5)
TREE_LANTERNS = (2,)
LANTERN_TEXTURES = {"lantern_iron": "yard_lantern_iron", "lantern_glass": "yard_lantern_glass"}


def pinned(elements):
    """`elements` with every face reaching outside 0..16 given UVs inside the texture (see decor3_data.fitted)."""
    return fitted({"elements": elements})["elements"]


def drawn(elements, textures):
    """The quads of `elements`, marking those to draw cut out."""
    out = quads(pinned(elements), textures)
    for quad in out:
        if quad["texture"] in CUTOUT:
            quad["cutout"] = True
    return out


def moved(elements, dx, dy, dz):
    """`elements` moved by (dx, dy, dz), rotation origins too."""
    out = []
    for element in elements:
        copy = {**element, "from": [element["from"][0] + dx, element["from"][1] + dy, element["from"][2] + dz],
                "to": [element["to"][0] + dx, element["to"][1] + dy, element["to"][2] + dz]}
        if "rotation" in element:
            origin = element["rotation"]["origin"]
            copy["rotation"] = {**element["rotation"], "origin": [origin[0] + dx, origin[1] + dy, origin[2] + dz]}
        out.append(copy)
    return out


def turned_all(elements, rotation):
    """`elements` (unrotated) all turned by `rotation`, as one rigid part."""
    out = []
    for element in elements:
        assert "rotation" not in element
        out.append({**element, "rotation": dict(rotation)})
    return out


def assign(elements, cell):
    """The elements whose middle is at the height of the block at `cell` (cx, cy, cz) of a one-block-wide column,
    moved into it whole: unlike `split`, nothing is cut, so a branch reaches out over the blocks beside the column
    (a model may reach 16 pixels past its block)."""
    lo_cell = [c * 16 for c in cell]
    out = []
    for element in elements:
        middle = (element["from"][1] + element["to"][1]) / 2
        if lo_cell[1] <= middle < lo_cell[1] + 16:
            local = moved([element], -lo_cell[0], -lo_cell[1], -lo_cell[2])[0]
            assert all(-16 <= v <= 32 for v in local["from"] + local["to"]), local
            out.append(local)
    return out


def lantern(x, y, z, glow):
    """A small iron lantern, its bottom at (x, y, z) its front-left corner, 4 pixels square and 6 tall."""
    light = 15 if glow else None
    return [box((x + 0.5, y, z + 0.5), (x + 3.5, y + 0.5, z + 3.5), "#lantern_iron"),
            box((x, y + 0.5, z), (x + 4, y + 5, z + 4), "#lantern_glass", light=light),
            box((x + 0.5, y + 5, z + 0.5), (x + 3.5, y + 6, z + 3.5), "#lantern_iron")]


# ---------------------------------------------------------------- the yard inflatables

INFLATABLE_TEXTURES = {"blower": "inflatable_blower", "ghost": "inflatable_ghost_vinyl", "ghost_face": "inflatable_ghost_face",
                       "cat": "inflatable_cat_vinyl", "cat_face": "inflatable_cat_face", "bow": "inflatable_bow",
                       "pumpkin": "inflatable_pumpkin_vinyl", "pumpkin_face": "inflatable_pumpkin_face", "hat": "inflatable_hat",
                       "spider": "inflatable_spider_vinyl", "spider_face": "inflatable_spider_face", "leg": "inflatable_leg"}
FULL = (0, 0, 16, 16)


def blower():
    """The blower at the figure's back, and its hose."""
    return [box((5, 0, 12.5), (11, 4, 16), "#blower"), box((7, 1, 10.5), (9, 2.5, 12.5), "#blower")]


def figure(design):
    """The inflated figure, its feet at y 0, facing north."""
    if design == "ghost":
        g = "#ghost"
        return [box((2, 0, 2), (14, 6, 14), g), box((3, 6, 3), (13, 18, 13), g),
                box((3.5, 18, 3.5), (12.5, 27, 12.5), g, textures={"north": "#ghost_face"}, uvs={"north": FULL}),
                box((5, 27, 5), (11, 29, 11), g),
                box((0.5, 12, 6), (3, 16.5, 10), g), box((13, 12, 6), (15.5, 16.5, 10), g)]
    if design == "cat":
        c = "#cat"
        return [box((3.5, 0, 5), (12.5, 15, 12.5), c), box((4.5, 0, 3), (7.5, 3, 5), c), box((8.5, 0, 3), (11.5, 3, 5), c),
                box((3, 15, 4), (13, 24, 12), c, textures={"north": "#cat_face"}, uvs={"north": FULL}),
                box((3.5, 24, 6), (6.5, 28, 9), c), box((9.5, 24, 6), (12.5, 28, 9), c),
                box((11.5, 2, 12.5), (13.5, 23, 14.5), c), box((6, 13.5, 3.5), (10, 15.5, 5), "#bow")]
    if design == "pumpkin":
        p = "#pumpkin"
        return [box((1, 0, 1), (15, 12, 15), p, textures={"north": "#pumpkin_face"}, uvs={"north": FULL}),
                box((3.5, 12, 3.5), (12.5, 20.5, 12.5), p, textures={"north": "#pumpkin_face"}, uvs={"north": FULL}),
                box((2.5, 20.5, 2.5), (13.5, 21.5, 13.5), "#hat"), box((5, 21.5, 5), (11, 25.5, 11), "#hat"),
                box((6.5, 25.5, 6.5), (9.5, 28.5, 9.5), "#hat"), box((7.5, 28.5, 7.5), (8.5, 30.5, 8.5), "#hat")]
    s = "#spider"
    elements = [box((2.5, 10, 6), (13.5, 22, 15.5), s),
                box((4.5, 11, 1.5), (11.5, 18, 6), s, textures={"north": "#spider_face"}, uvs={"north": FULL})]
    for z in (3, 6.5, 10, 13.5):
        elements += [box((-3, 18, z), (2.5, 19.5, z + 1.5), "#leg"), box((-3, 0, z), (-1.5, 18, z + 1.5), "#leg"),
                     box((13.5, 18, z), (19, 19.5, z + 1.5), "#leg"), box((17.5, 0, z), (19, 18, z + 1.5), "#leg")]
    return elements


# ---------------------------------------------------------------- the porch witch

WITCH_TEXTURES = {"pot": "porch_witch_pot", "brew": "porch_witch_brew", "robe": "porch_witch_robe", "skin": "porch_witch_skin",
                  "face": "porch_witch_face", "hat": "porch_witch_hat", "hair": "porch_witch_hair", "spoon": "porch_witch_spoon"}


def witch_body():
    """Her pot in front (a bubbling green brew in it) and her robe behind it, with her left arm hanging."""
    pot = "#pot"
    return [box((5.5, 0, 0.5), (14.5, 2, 7.5), pot), box((5.5, 2, 0.5), (14.5, 7, 1.5), pot), box((5.5, 2, 6.5), (14.5, 7, 7.5), pot),
            box((5.5, 2, 1.5), (6.5, 7, 6.5), pot), box((13.5, 2, 1.5), (14.5, 7, 6.5), pot),
            box((6.5, 5.5, 1.5), (13.5, 6, 6.5), "#brew", light=12),
            box((3.5, 0, 8.5), (12.5, 13, 15.5), "#robe"), box((4.5, 13, 9), (11.5, 21, 15), "#robe"),
            box((4, 19.5, 8.5), (12, 21, 15.5), "#robe"),
            box((2.5, 13.5, 10.5), (4.5, 21, 12.5), "#robe"), box((2.5, 12, 10.5), (4.5, 13.5, 12.5), "#skin")]


WITCH_SHOULDER = (12.5, 20.5, 11.5)
WITCH_NECK = (8, 21, 12)


def witch_arm():
    """Her right arm, reaching forward and down to the spoon in the pot (turned about her shoulder as she stirs)."""
    shoulder = {"origin": list(WITCH_SHOULDER), "axis": "x", "angle": 45}
    spoon = {"origin": [10, 3.5, 4.75], "axis": "z", "angle": -11.3}
    return [box((11.5, 13, 10.5), (13.5, 21, 12.5), "#robe", rotation=shoulder),
            box((11.5, 11.5, 10.5), (13.5, 13, 12.5), "#skin", rotation=shoulder),
            box((9.5, 3.5, 4.25), (10.5, 16, 5.25), "#spoon", rotation=spoon),
            box((9, 2.5, 3.75), (11, 4, 5.75), "#spoon", rotation=spoon)]


def witch_head():
    """Her head, hair and tall pointed hat (turned about her neck)."""
    return [box((5, 21, 9), (11, 27, 15), "#skin", textures={"north": "#face"}, uvs={"north": FULL}),
            box((7.5, 23, 7.5), (8.5, 25, 9), "#skin"),
            box((4.5, 19, 10), (5, 26, 15.5), "#hair"), box((11, 19, 10), (11.5, 26, 15.5), "#hair"), box((5, 19, 15), (11, 26, 15.5), "#hair"),
            box((2.5, 27, 6.5), (13.5, 28, 17.5), "#hat"), box((4.5, 28, 8.5), (11.5, 31, 15.5), "#hat"),
            box((5.5, 31, 9.5), (10.5, 34, 14.5), "#hat"), box((6.5, 34, 10.5), (9.5, 37, 13.5), "#hat"),
            box((7.25, 37, 11.25), (8.75, 39.5, 12.75), "#hat")]


# ---------------------------------------------------------------- grasping hands

HANDS_TEXTURES = {"dirt": "grasping_hands_dirt", "skin": "grasping_hands_skin"}
# (left height, right height, fingers open) for each phase.
HANDS_PHASES = {"rest": (4, 2.5, True), "grab": (8, 7, False), "recover": (1.5, 0.5, True)}


def hand(x, z, height, open_hand):
    """A rotting forearm out of the ground at (x, z), `height` pixels up, its hand clawing open or clenched."""
    skin = "#skin"
    top = 2 + height
    elements = [box((x, 2, z), (x + 3, top, z + 3), skin), box((x - 0.5, top, z - 0.5), (x + 3.5, top + 2.5, z + 3.5), skin)]
    palm = top + 2.5
    if open_hand:
        for i in range(4):
            fx = x - 0.25 + i
            elements.append(box((fx, palm, z), (fx + 0.75, palm + 3, z + 0.75), skin))
        elements.append(box((x + 3.5, palm - 1.5, z + 1.25), (x + 4.25, palm + 1.5, z + 2), skin))
    else:
        elements.append(box((x - 0.5, palm, z - 0.25), (x + 3.5, palm + 1.5, z + 1.25), skin))
    return elements


def hands_elements(phase):
    left, right, open_hand = HANDS_PHASES[phase]
    return ([box((1, 0, 1), (15, 2, 15), "#dirt"), box((3, 2, 11), (6, 3, 13.5), "#dirt"), box((10, 2, 2.5), (12.5, 2.75, 5), "#dirt")]
            + hand(3, 3.5, left, open_hand) + hand(10, 9, right, open_hand))


# ---------------------------------------------------------------- the poseable skeleton

SKELETON_TEXTURES = {"bone": "poseable_skeleton_bone", "skull": "poseable_skeleton_skull", "ribs": "poseable_skeleton_ribs"}


def torso():
    """Spine, ribs, neck, jaw and skull, the bottom of the spine at y 2 (a pelvis from 0 to 2 goes under it)."""
    b = "#bone"
    return [box((7.5, 2, 9), (8.5, 4.5, 10), b),
            box((5, 4.5, 6.5), (11, 10.5, 10.5), b, textures={"north": "#ribs", "south": "#ribs"}),
            box((7.5, 10.5, 8), (8.5, 11.5, 9), b), box((6, 11.5, 6.5), (10, 12.5, 10), b),
            box((5.5, 12.5, 6), (10.5, 17.5, 11), b, textures={"north": "#skull"}, uvs={"north": FULL})]


def pelvis():
    return [box((5.5, 0, 7), (10.5, 2, 10), "#bone")]


def arm_down(x):
    b = "#bone"
    return [box((x, 4, 8), (x + 1.5, 10, 9.5), b), box((x, -1.5, 8), (x + 1.5, 4, 9.5), b), box((x, -3, 7.75), (x + 1.5, -1.5, 9.75), b)]


def leg_down(x):
    b = "#bone"
    return [box((x, -5.5, 8), (x + 1.5, 0, 9.5), b), box((x, -11, 8), (x + 1.5, -5.5, 9.5), b), box((x, -11.5, 6.5), (x + 1.5, -11, 9.5), b)]


def leg_out(x):
    """A leg stretched out forward along the ground from a hip at y 0."""
    b = "#bone"
    return [box((x, 0, 1.5), (x + 1.5, 1.5, 8.5), b), box((x, 0, -4.5), (x + 1.5, 1.5, 1.5), b), box((x, 1.5, -4.5), (x + 1.5, 3.5, -3), b)]


def skeleton_elements(pose):
    """The skeleton in `pose`, standing on y 0 of its lower half."""
    b = "#bone"
    if pose == "waving":
        raised = {"origin": [11, 9.25, 8.75], "axis": "z", "angle": 45}
        body = (torso() + pelvis() + arm_down(3.5) + leg_down(5.5) + leg_down(9)
                + [box((11, 8.5, 8), (17, 10, 9.5), b, rotation=raised),
                   box((14.5, 13, 8), (16, 18, 9.5), b), box((14.25, 18, 7.75), (16.25, 19.5, 9.75), b)])
        return moved(body, 0, 11.5, 0)
    if pose == "sitting":
        forearm = [box((3.5, 4, 8), (5, 10, 9.5), b), box((3.5, 2.5, 2.5), (5, 4, 8), b), box((3.5, 1.5, 1), (5, 3, 2.5), b)]
        return torso() + pelvis() + leg_out(5.5) + leg_out(9) + forearm + moved(forearm, 7.5, 0, 0)
    if pose == "lounging":
        lean = {"origin": [8, 1.5, 10], "axis": "x", "angle": 45}
        arms = [box((3.5, 10, 8.5), (5, 15.5, 10), b), box((4, 15, 11), (7.5, 16.5, 12.5), b),
                box((11, 10, 8.5), (12.5, 15.5, 10), b), box((8.5, 15, 11), (12, 16.5, 12.5), b)]
        knee = {"origin": [9.75, 0.75, 8.5], "axis": "x", "angle": 45}
        right_leg = [box((9, 0, 1.5), (10.5, 1.5, 8.5), b, rotation=knee), box((9, 0, 2.8), (10.5, 5.7, 4.3), b),
                     box((9, 0, 1.3), (10.5, 0.5, 2.8), b)]
        return pelvis() + turned_all(torso() + arms, lean) + leg_out(5.5) + right_leg
    # Hanging by its hands from the top of the upper half.
    arms = []
    for x in (3.5, 11):
        arms += [box((x, 10, 8), (x + 1.5, 14.5, 9.5), b), box((x, 14.5, 8), (x + 1.5, 19, 9.5), b), box((x, 19, 7.75), (x + 1.5, 20, 9.75), b)]
    legs = []
    for x in (5.5, 9):
        legs += [box((x, -5.5, 8), (x + 1.5, 0, 9.5), b), box((x, -11, 8.25), (x + 1.5, -5.5, 9.75), b), box((x, -11.5, 7.5), (x + 1.5, -11, 9.5), b)]
    return moved(torso() + pelvis() + arms + legs, 0, 12, 0)


# ---------------------------------------------------------------- bone wind chimes

CHIME_TEXTURES = {"iron": "wind_chimes_iron", "wood": "wind_chimes_wood", "bone": "wind_chimes_bone", "string": "wind_chimes_string",
                  "skull": "wind_chimes_skull"}
# The bones hang this far from the middle, from the bottom of the wooden plate.
CHIME_RADIUS = 3.0
CHIME_PIVOT_Y = 12.5


def chimes_elements():
    return [box((7.5, 13.5, 7.5), (8.5, 16, 8.5), "#iron"), box((4, 12.5, 4), (12, 13.5, 12), "#wood")]


def chime_bone():
    """One bone on its string, hung from (0, 12.5, 0)."""
    return [box((-0.15, 9, -0.15), (0.15, CHIME_PIVOT_Y, 0.15), "#string"), box((-0.6, 3.5, -0.6), (0.6, 8.5, 0.6), "#bone"),
            box((-0.9, 8, -0.9), (0.9, 9, 0.9), "#bone"), box((-0.9, 3, -0.9), (0.9, 4, 0.9), "#bone")]


def chime_skull():
    """The striker in the middle: a little skull on a string."""
    return [box((-0.15, 7.5, -0.15), (0.15, CHIME_PIVOT_Y, 0.15), "#string"),
            box((-1.5, 4.5, -1.5), (1.5, 7.5, 1.5), "#bone", textures={"north": "#skull"}, uvs={"north": FULL})]


# ---------------------------------------------------------------- weathervanes

VANE_TEXTURES = {"iron": "weathervane_iron", "n": "weathervane_n", "e": "weathervane_e", "s": "weathervane_s", "w": "weathervane_w"}
VANE_Y = 20.0


def vane_post():
    """The mount, the pole, the compass points (which do not turn) and a ball on top."""
    i = "#iron"
    return [box((6, 0, 6), (10, 1.5, 10), i), box((7.4, 1.5, 7.4), (8.6, 24, 8.6), i),
            box((7.5, 15, 1.5), (8.5, 16, 14.5), i), box((1.5, 15, 7.5), (14.5, 16, 8.5), i),
            box((6.75, 15.5, 0.5), (9.25, 18, 1.5), "#n", uvs={side: FULL for side in ("north", "south", "east", "west")}),
            box((6.75, 15.5, 14.5), (9.25, 18, 15.5), "#s", uvs={side: FULL for side in ("north", "south", "east", "west")}),
            box((0.5, 15.5, 6.75), (1.5, 18, 9.25), "#w", uvs={side: FULL for side in ("north", "south", "east", "west")}),
            box((14.5, 15.5, 6.75), (15.5, 18, 9.25), "#e", uvs={side: FULL for side in ("north", "south", "east", "west")}),
            box((7, 24, 7), (9, 26, 9), i)]


def vane(design):
    """The vane, turning about the pole: an arrow pointing north (into the wind) and the bat or witch on its tail."""
    i = "#iron"
    y = VANE_Y
    return [box((7.6, y, 1), (8.4, y + 0.8, 15), i), box((7.75, y - 1, -0.5), (8.25, y + 1.8, 2.5), i),
            box((7.75, y + 0.8, 9.5), (8.25, y + 7, 16), f"#{design}", faces=("east", "west"), uvs={"east": FULL, "west": FULL})]


# ---------------------------------------------------------------- the spooky sign

SIGN_TEXTURES = {"wood": "spooky_sign_wood", "stake": "spooky_sign_stake"}


def sign_elements():
    """A weathered board on a stake, facing north."""
    return [box((7, 0, 8), (9, 7, 9.5), "#stake"), box((0.5, 6, 7.5), (15.5, 15.5, 9), "#wood")]


# ---------------------------------------------------------------- the haunted archway

ARCHWAY_TEXTURES = {"stone": "haunted_archway_stone", "iron": "haunted_archway_iron", "skull": "haunted_archway_skull",
                    "web": "haunted_archway_web", **LANTERN_TEXTURES}


def archway_elements(lit):
    """Two mossy pillars, each with a lantern on its front, joined by an iron arch with a skull at its crown; the whole
    archway, 48 pixels wide (its columns along +x) and 48 tall."""
    s, i = "#stone", "#iron"
    elements = []
    for x0 in (0, 32):
        elements += [box((x0, 0, 2), (x0 + 16, 4, 14), s), box((x0 + 2, 4, 4), (x0 + 14, 38, 12), s), box((x0 + 1, 38, 3), (x0 + 15, 41, 13), s),
                     box((x0 + 3, 41, 5), (x0 + 13, 44, 11), s), box((x0 + 5, 44, 7), (x0 + 11, 46, 9), s),
                     box((x0 + 7, 26, 1), (x0 + 9, 27, 4), i), box((x0 + 7.75, 23, 1.75), (x0 + 8.25, 26, 2.25), i)]
        elements += lantern(x0 + 6, 17, 0, lit)
    for x0, x1, y0, y1 in ((14, 17, 36.5, 39.5), (17, 20, 38.5, 41.5), (20, 24, 40.5, 43), (24, 28, 40.5, 43), (28, 31, 38.5, 41.5),
                           (31, 34, 36.5, 39.5)):
        elements.append(box((x0, y0, 6.75), (x1, y1, 9.25), i))
    for x, top in ((18, 38.5), (30, 38.5)):
        elements += [box((x - 0.4, 36.5, 7.6), (x + 0.4, top, 8.4), i), box((x - 0.4, top + 3, 7.6), (x + 0.4, top + 5, 8.4), i)]
    elements += [box((14, 34.5, 6.75), (34, 36.5, 9.25), i),
                 box((21, 36.5, 6.5), (27, 41, 9.5), i, textures={"north": "#skull", "south": "#skull"},
                     uvs={"north": FULL, "south": FULL}),
                 box((23.5, 42.5, 7.5), (24.5, 45, 8.5), i),
                 box((14, 32, 7.99), (18.5, 35, 8.01), "#web", faces=("north", "south"), uvs={"north": FULL, "south": FULL}),
                 box((29.5, 32, 7.99), (34, 35, 8.01), "#web", faces=("north", "south"), uvs={"north": FULL, "south": FULL})]
    return elements


def archway_part(part, lit):
    """The model of archway part `part`: its cell, counted to the player's right, is design column 2 - right."""
    right, up = ARCHWAY_CELLS[part]
    return split(archway_elements(lit), (HAUNTED_ARCHWAY["width"] - 1 - right, up, 0))


# ---------------------------------------------------------------- the dead hollow tree

TREE_TEXTURES = {"bark": "dead_tree_bark", "end": "dead_tree_end", "hollow": "dead_tree_hollow", "face": "dead_tree_face",
                 "eye": "dead_tree_eye", **LANTERN_TEXTURES}


def tree_elements(lit):
    """The dead tree, 64 pixels tall: roots, a hollow at its foot, a face in its bark with glowing eyes, four bare
    branches reaching out over the blocks round it, and a lantern hanging from two of them."""
    b = "#bark"
    elements = [box((1, 0, 1), (15, 5, 15), b),
                box((-3, 0, 6), (1, 2.5, 10), b), box((15, 0, 5), (19, 2, 9), b), box((6, 0, 15), (10, 2.5, 19), b), box((6.5, 0, -3), (10.5, 2, 1), b),
                box((2.5, 5, 2.5), (13.5, 14, 13.5), b, textures={"north": "#hollow"}, uvs={"north": FULL}),
                box((3, 14, 3), (13, 22, 13), b),
                box((3, 22, 3), (13, 34, 13), b, textures={"north": "#face"}, uvs={"north": FULL}),
                box((5, 29, 2.9), (7, 30.5, 3), "#eye", faces=("north",), light=15),
                box((9, 29, 2.9), (11, 30.5, 3), "#eye", faces=("north",), light=15),
                box((3.5, 34, 3.5), (12.5, 48, 12.5), b), box((4.5, 48, 4.5), (11.5, 58, 11.5), b),
                box((5, 58, 5), (9, 62, 9), b, textures={"up": "#end"}), box((8, 58, 7), (11, 60, 11), b, textures={"up": "#end"}),
                box((-13, 36, 6.5), (3.5, 39, 9.5), b, rotation={"origin": [3.5, 37.5, 8], "axis": "z", "angle": -22.5}),
                box((12.5, 44, 6.5), (29, 47, 9.5), b, rotation={"origin": [12.5, 45.5, 8], "axis": "z", "angle": 22.5}),
                box((6.5, 50, -12), (9.5, 53, 4.5), b, rotation={"origin": [8, 51.5, 4.5], "axis": "x", "angle": 22.5}),
                box((6.5, 30, 11.5), (9.5, 33, 26), b, rotation={"origin": [8, 31.5, 11.5], "axis": "x", "angle": -22.5}),
                box((-9.5, 42, 7.5), (-8.5, 48, 8.5), b), box((24.5, 50.5, 7.5), (25.5, 56, 8.5), b),
                box((7.5, 35.5, 21.5), (8.5, 40, 22.5), b), box((7.5, 56.5, -8.5), (8.5, 61, -7.5), b),
                box((-6.25, 37.5, 7.75), (-5.75, 41, 8.25), "#lantern_iron"), box((21.75, 45, 7.75), (22.25, 49, 8.25), "#lantern_iron")]
    return elements + lantern(-8, 31.5, 6, lit) + lantern(20, 39, 6, lit)


def tree_part(part, lit):
    return assign(tree_elements(lit), (0, part, 0))


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"
    booleans = (False, True)

    def item_model(name, model_name):
        write(items / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{model_name}")}})

    for design in INFLATABLES["designs"]:
        block = inflatable(design)
        textures = INFLATABLE_TEXTURES
        particle = textures[design]
        write(models / f"{block}_lower.json", block_model(textures, blower(), particle))
        write(models / f"{block}_upper.json", block_model(textures, [], particle))
        write(states / f"{block}.json", {"variants": {
            f"facing={f},half={h},on={str(o).lower()},powered={str(p).lower()}": turned(rid(f"block/{block}_{h}"), f)
            for f in HORIZONTAL for h in ("lower", "upper") for o in booleans for p in booleans}})
        write(models / f"{block}_item.json", fitted(block_model(textures, scaled(blower() + figure(design), 0.5, (4, 0, 4)), particle)))
        item_model(block, f"{block}_item")
        lang[f"block.{MOD}.{block}"] = INFLATABLES["display"][design]

    witch = PORCH_WITCH["block"]
    body = witch_body()
    for half, cy in (("lower", 0), ("upper", 1)):
        write(models / f"{witch}_{half}.json", block_model(WITCH_TEXTURES, split(body, (0, cy, 0)), WITCH_TEXTURES["robe"]))
    write(states / f"{witch}.json", {"variants": {f"cackling={str(c).lower()},facing={f},half={h}": turned(rid(f"block/{witch}_{h}"), f)
                                                  for c in booleans for f in HORIZONTAL for h in ("lower", "upper")}})
    write(models / f"{witch}_item.json", fitted(block_model(WITCH_TEXTURES, scaled(body, 0.45, (4.4, 0, 4.4)), WITCH_TEXTURES["robe"])))
    item_model(witch, f"{witch}_item")
    lang[f"block.{MOD}.{witch}"] = PORCH_WITCH["display"]

    hands = GRASPING_HANDS["block"]
    for phase in HANDS_PHASES:
        write(models / f"{hands}_{phase}.json", block_model(HANDS_TEXTURES, hands_elements(phase), HANDS_TEXTURES["dirt"]))
    write(states / f"{hands}.json", {"variants": {f"facing={f},phase={p}": turned(rid(f"block/{hands}_{p}"), f)
                                                  for f in HORIZONTAL for p in HANDS_PHASES}})
    item_model(hands, f"{hands}_rest")
    lang[f"block.{MOD}.{hands}"] = GRASPING_HANDS["display"]

    skeleton = POSEABLE_SKELETON["block"]
    for pose in POSEABLE_SKELETON["poses"]:
        write(models / f"{skeleton}_{pose}.json", fitted(block_model(SKELETON_TEXTURES, skeleton_elements(pose), SKELETON_TEXTURES["bone"])))
    write(models / f"{skeleton}_upper.json", block_model(SKELETON_TEXTURES, [], SKELETON_TEXTURES["bone"]))
    write(states / f"{skeleton}.json", {"variants": {
        f"facing={f},half={h},pose={p}": turned(rid(f"block/{skeleton}_{p if h == 'lower' else 'upper'}"), f)
        for f in HORIZONTAL for h in ("lower", "upper") for p in POSEABLE_SKELETON["poses"]}})
    write(models / f"{skeleton}_item.json", fitted(block_model(SKELETON_TEXTURES, scaled(skeleton_elements("waving"), 0.5, (4, 0, 4)),
                                                               SKELETON_TEXTURES["bone"])))
    item_model(skeleton, f"{skeleton}_item")
    lang[f"block.{MOD}.{skeleton}"] = POSEABLE_SKELETON["display"]

    chimes = WIND_CHIMES["block"]
    write(models / f"{chimes}.json", block_model(CHIME_TEXTURES, chimes_elements(), CHIME_TEXTURES["bone"]))
    write(states / f"{chimes}.json", {"variants": {"": {"model": rid(f"block/{chimes}")}}})
    item_bones = []
    for i in range(WIND_CHIMES["bones"]):
        angle = math.radians(360 * i / WIND_CHIMES["bones"])
        item_bones += moved(chime_bone(), 8 + CHIME_RADIUS * math.cos(angle), 0, 8 + CHIME_RADIUS * math.sin(angle))
    write(models / f"{chimes}_item.json", fitted(block_model(CHIME_TEXTURES, chimes_elements() + item_bones + moved(chime_skull(), 8, 0, 8),
                                                             CHIME_TEXTURES["bone"])))
    item_model(chimes, f"{chimes}_item")
    lang[f"block.{MOD}.{chimes}"] = WIND_CHIMES["display"]

    for design in WEATHERVANES["designs"]:
        block = weathervane(design)
        textures = {**VANE_TEXTURES, design: f"weathervane_{design}"}
        write(models / f"{block}.json", fitted(block_model(textures, vane_post(), VANE_TEXTURES["iron"])))
        write(states / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        write(models / f"{block}_item.json", fitted(block_model(textures, scaled(vane_post() + vane(design), 0.6, (3.2, 0, 3.2)),
                                                                VANE_TEXTURES["iron"])))
        item_model(block, f"{block}_item")
        lang[f"block.{MOD}.{block}"] = WEATHERVANES["display"][design]

    sign = SPOOKY_SIGN["block"]
    write(models / f"{sign}.json", block_model(SIGN_TEXTURES, sign_elements(), SIGN_TEXTURES["wood"]))
    write(states / f"{sign}.json", {"variants": {f"facing={f},words={w}": turned(rid(f"block/{sign}"), f)
                                                for f in HORIZONTAL for w in SPOOKY_SIGN["words"]}})
    item_model(sign, sign)
    lang[f"block.{MOD}.{sign}"] = SPOOKY_SIGN["display"]
    words = {"beware": "BEWARE", "keep_out": "KEEP OUT", "turn_back": "TURN BACK", "go_away": "GO AWAY", "no_trespassing": "NO TRESPASSING",
             "abandon_hope": "ABANDON HOPE"}
    for word in SPOOKY_SIGN["words"]:
        lang[f"message.{MOD}.{sign}.{word}"] = words[word]
    lang[f"message.{MOD}.{sign}.painted"] = "Sneak and use it with an empty hand to wipe your words off first."
    lang[f"message.{MOD}.{sign}.unnamed_tag"] = "Name the tag in an anvil first: its name is what gets painted."

    archway = HAUNTED_ARCHWAY["block"]
    variants = {}
    for part in range(len(ARCHWAY_CELLS)):
        for lit in booleans:
            name = f"{archway}_{part}" + ("_lit" if lit and part in ARCHWAY_LANTERNS else "")
            if not lit or part in ARCHWAY_LANTERNS:
                write(models / f"{name}.json", block_model(ARCHWAY_TEXTURES, archway_part(part, lit), ARCHWAY_TEXTURES["stone"]))
            for f in HORIZONTAL:
                variants[f"facing={f},lit={str(lit).lower()},part={part}"] = turned(rid(f"block/{name}"), f)
    write(states / f"{archway}.json", {"variants": variants})
    write(models / f"{archway}_item.json", fitted(block_model(ARCHWAY_TEXTURES, scaled(archway_elements(True), 1 / 3, (0, 0, 16 / 3)),
                                                              ARCHWAY_TEXTURES["stone"])))
    item_model(archway, f"{archway}_item")
    lang[f"block.{MOD}.{archway}"] = HAUNTED_ARCHWAY["display"]

    tree = DEAD_TREE["block"]
    variants = {}
    for part in range(DEAD_TREE["height"]):
        for lit in booleans:
            name = f"{tree}_{part}" + ("_lit" if lit and part in TREE_LANTERNS else "")
            if not lit or part in TREE_LANTERNS:
                write(models / f"{name}.json", block_model(TREE_TEXTURES, tree_part(part, lit), TREE_TEXTURES["bark"]))
            for f in HORIZONTAL:
                variants[f"facing={f},lit={str(lit).lower()},part={part}"] = turned(rid(f"block/{name}"), f)
    write(states / f"{tree}.json", {"variants": variants})
    write(models / f"{tree}_item.json", fitted(block_model(TREE_TEXTURES, scaled(tree_elements(True), 0.25, (6, 0, 6)), TREE_TEXTURES["bark"])))
    item_model(tree, f"{tree}_item")
    lang[f"block.{MOD}.{tree}"] = DEAD_TREE["display"]

    quad_models = {f"inflatable_{d}": drawn(figure(d), INFLATABLE_TEXTURES) for d in INFLATABLES["designs"]}
    quad_models.update({"porch_witch_arm": drawn(witch_arm(), WITCH_TEXTURES), "porch_witch_head": drawn(witch_head(), WITCH_TEXTURES),
                        "wind_chime_bone": drawn(chime_bone(), CHIME_TEXTURES), "wind_chime_skull": drawn(chime_skull(), CHIME_TEXTURES)})
    for design in WEATHERVANES["designs"]:
        quad_models[f"weathervane_{design}"] = drawn(vane(design), {**VANE_TEXTURES, design: f"weathervane_{design}"})
    write(root / "decor9_quads.json", quad_models)


def loot(out, write):
    """Each drops itself once: the two-block ones from their lower halves, the archway and tree from part 0; the sign
    keeps its words, and its own words go with it as its name (see SpookySignBlockEntity)."""
    for block in [GRASPING_HANDS["block"], WIND_CHIMES["block"]] + [weathervane(d) for d in WEATHERVANES["designs"]]:
        write(out / f"{block}.json", self_drop(block))
    for block in [inflatable(d) for d in INFLATABLES["designs"]] + [PORCH_WITCH["block"], POSEABLE_SKELETON["block"]]:
        write(out / f"{block}.json", self_drop(block, match_block(block, half="lower")))
    for block in (HAUNTED_ARCHWAY["block"], DEAD_TREE["block"]):
        write(out / f"{block}.json", self_drop(block, match_block(block, part="0")))
    sign = SPOOKY_SIGN["block"]
    write(out / f"{sign}.json", {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": rid(sign), "modifier": [
            {"type": "minecraft:copy_components", "include": ["minecraft:custom_name"], "source": "block_entity"},
            {"type": "minecraft:copy_state", "block": rid(sign), "properties": ["words"]}]}],
        "rolls": 1}], "random_sequence": rid(f"blocks/{sign}")})


def tags(tags):
    for block in [POSEABLE_SKELETON["block"], HAUNTED_ARCHWAY["block"]] + [weathervane(d) for d in WEATHERVANES["designs"]]:
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    for block in (PORCH_WITCH["block"], WIND_CHIMES["block"], SPOOKY_SIGN["block"], DEAD_TREE["block"]):
        tags.add("block", "minecraft:mineable/axe", rid(block))
    tags.add("block", "minecraft:mineable/shovel", rid(GRASPING_HANDS["block"]))
