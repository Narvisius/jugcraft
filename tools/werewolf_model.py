"""The werewolf's model (client/WerewolfModel.java) as data: its parts, their pivots and rest rotations, and their boxes
with texture offsets. tools/werewolf_textures.py paints the texture box by box from it, and tools/check_mod_data.py
checks every box here appears in the Java (texOffs and addBox), so the texture and the model can't drift apart.

Units are model pixels (y down, the werewolf facing -z); rotations are radians. A box is
(u, v, x, y, z, width, height, depth, mirrored).
"""
LEAN = 0.5
HIP = 4.7

PARTS = [
    {"name": "body", "parent": None, "pivot": (0, HIP, 1.0), "rot": (LEAN, 0, 0), "boxes": [
        (0, 23, -5, -7, -4, 10, 7, 8, False),         # waist
        (0, 0, -7, -18, -5.5, 14, 12, 11, False),     # chest
        (50, 0, -7.5, -21, -2.5, 15, 7, 9, False),    # hump of mane
        (0, 38, -6, -19, -8, 12, 5, 5, False)]},      # ruff at the throat
    {"name": "head", "parent": "body", "pivot": (0, -19, -6), "rot": (-LEAN, 0, 0), "boxes": [
        (50, 16, -4.5, -8, -7, 9, 8, 8, False),       # skull
        (84, 17, -3, -5, -14, 6, 3, 7, False),        # muzzle
        (106, 0, -5.5, -5, -6, 2, 4, 4, False),       # cheek tufts
        (106, 0, 3.5, -5, -6, 2, 4, 4, True),
        (118, 0, -2.7, -2, -13.8, 1, 2, 1, False),    # upper fangs
        (118, 0, 1.7, -2, -13.8, 1, 2, 1, False)]},
    {"name": "jaw", "parent": "head", "pivot": (0, -2, -7.5), "rot": (0.35, 0, 0), "boxes": [
        (98, 8, -2.5, 0, -6.5, 5, 2, 7, False),
        (122, 4, -2.3, -1.5, -6.3, 1, 2, 1, False),   # lower fangs (their own patch, as they point up)
        (122, 4, 1.3, -1.5, -6.3, 1, 2, 1, False)]},
    {"name": "right_ear", "parent": "head", "pivot": (-2.8, -7.5, -2), "rot": (-0.1, 0, -0.2), "boxes": [
        (98, 0, -1, -5, -1, 2, 5, 2, False),
        (118, 3, -0.5, -6.5, -0.5, 1, 2, 1, False)]},
    {"name": "left_ear", "parent": "head", "pivot": (2.8, -7.5, -2), "rot": (-0.1, 0, 0.2), "boxes": [
        (98, 0, -1, -5, -1, 2, 5, 2, True),
        (118, 3, -0.5, -6.5, -0.5, 1, 2, 1, True)]},
]
for _side, _name in ((-1, "right"), (1, "left")):
    _m = _side > 0
    PARTS += [
        {"name": f"{_name}_arm", "parent": "body", "pivot": (_side * 8.5, -16, -2.5), "rot": (-LEAN * 0.75, 0, -_side * 0.1), "boxes": [
            (36, 32, -3, -3, -3.5, 6, 6, 7, _m),      # shoulder
            (62, 32, -2.5, 2, -2.5, 5, 10, 5, _m)]},  # upper arm
        {"name": f"{_name}_forearm", "parent": f"{_name}_arm", "pivot": (0, 11.5, 0), "rot": (-0.3, 0, 0), "boxes": [
            (82, 32, -2.5, 0, -2.5, 5, 10, 5, _m)]},
        {"name": f"{_name}_hand", "parent": f"{_name}_forearm", "pivot": (0, 9.5, 0), "rot": (0, 0, 0), "boxes": [
            (102, 32, -2.5, 0, -2.5, 5, 3, 5, _m)] + [(122, 0, -2.5 + c * 1.25, 2, -2.7, 1, 3, 1, _m) for c in range(4)]},
        {"name": f"{_name}_leg", "parent": None, "pivot": (_side * 4.5, HIP, 2), "rot": (-0.5, 0, 0), "boxes": [
            (0, 48, -3, -1, -3.5, 6, 9, 7, _m)]},     # thigh
        {"name": f"{_name}_shin", "parent": f"{_name}_leg", "pivot": (0, 8, 0), "rot": (1.15, 0, 0), "boxes": [
            (26, 48, -2.5, 0, -2.5, 5, 7, 5, _m)]},
        {"name": f"{_name}_hock", "parent": f"{_name}_shin", "pivot": (0, 6.5, 0), "rot": (-1.0, 0, 0), "boxes": [
            (46, 48, -2, 0, -2, 4, 6, 4, _m)]},
        {"name": f"{_name}_paw", "parent": f"{_name}_hock", "pivot": (0, 6, 0), "rot": (0.35, 0, 0), "boxes": [
            (62, 48, -2.5, -0.5, -4.5, 5, 2, 6, _m)]},
    ]
PARTS += [
    {"name": "tail", "parent": "body", "pivot": (0, -1, 4.5), "rot": (-1.2, 0, 0), "boxes": [(84, 48, -1.5, -1.5, 0, 3, 3, 5, False)]},
    {"name": "tail_mid", "parent": "tail", "pivot": (0, 0, 4.5), "rot": (0.15, 0, 0), "boxes": [(100, 48, -2.5, -2.5, 0, 5, 5, 7, False)]},
    {"name": "tail_tip", "parent": "tail_mid", "pivot": (0, 0, 6.5), "rot": (0.15, 0, 0), "boxes": [(0, 64, -2, -2, 0, 4, 4, 6, False)]},
]

TEXTURE_SIZE = (128, 128)
# The texture is painted this many times finer than TEXTURE_SIZE (512 by 512): the model samples it by its declared size,
# so each model pixel shows a patch of painted fur (docs/ART_DIRECTION.md, "High resolution").
TEXTURE_SCALE = 4


def boxes():
    """Every distinct (u, v, width, height, depth) the texture must paint, named by the part that first uses it."""
    seen = {}
    for part in PARTS:
        for u, v, _x, _y, _z, w, h, d, _m in part["boxes"]:
            seen.setdefault((u, v, w, h, d), part["name"])
    return seen
