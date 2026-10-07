"""JSON resources for the third batch of Halloween decorations, the graveyard, from tools/agriculture.py: the
wrought-iron Cemetery Fence and Gate, the crypt set (crypt stone, chiseled crypt stone, crypt stone pillar, Crypt
Door), the Grave Mound, the Mourning Angel and the Pop-Up Skeleton; their names, loot, tags and stonecutter recipes.

Called from agriculture_data.py (assets, loot, tags, recipes). Formats follow vanilla Minecraft 26.3's own files: the
fence and fence gate blockstates (multipart sides, gate variants with uvlock), the door blockstate and its door_*
parent models, cube_column for a pillar, two-block loot from the lower half, and stonecutting recipes. Model
rotations are right-handed: about x a positive angle turns +y toward +z, about z it turns +x toward +y.
"""
from agriculture import CEMETERY_FENCE, CRYPT, GRAVE_MOUND, MOURNING_ANGEL, POP_UP_SKELETON
from decor_data import MOD, HORIZONTAL, SIDES, rid, turned, box, block_model, flat_item, self_drop

UP_SIDES = SIDES + ("up",)


def default_uv(side, lo, hi):
    """The UV Minecraft gives a face without one, from the element's position."""
    (fx, fy, fz), (tx, ty, tz) = lo, hi
    return {"down": [fx, 16 - tz, tx, 16 - fz], "up": [fx, fz, tx, tz], "north": [16 - tx, 16 - ty, 16 - fx, 16 - fy],
            "south": [fx, 16 - ty, tx, 16 - fy], "west": [fz, 16 - ty, tz, 16 - fy], "east": [16 - tz, 16 - ty, 16 - fz, 16 - fy]}[side]


def fitted(model):
    """Pins the UV of every face of an element reaching outside the block to the same size inside the texture, so
    nothing reads outside it (the finials and the popped skeleton stand above the block)."""
    for element in model["elements"]:
        for side, face in element["faces"].items():
            if "uv" in face:
                continue
            u0, v0, u1, v1 = default_uv(side, element["from"], element["to"])
            if min(u0, v0, u1, v1) < 0 or max(u0, v0, u1, v1) > 16:
                du = min(max(u0, 0), 16 - (u1 - u0)) - u0
                dv = min(max(v0, 0), 16 - (v1 - v0)) - v0
                face["uv"] = [round(u0 + du, 3), round(v0 + dv, 3), round(u1 + du, 3), round(v1 + dv, 3)]
    return model


# ---------------------------------------------------------------- the cemetery fence and gate

def picket(x0, z0, x1, z1, top=14.0, low=0.0):
    """A square iron bar from `low` to `top` with a spear point above it."""
    cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
    return [box((x0, low, z0), (x1, top, z1), "#iron"),
            box((cx - 0.25, top, cz - 0.25), (cx + 0.25, top + 1.5, cz + 0.25), "#iron")]


def fence_post():
    return [box((6.5, 0, 6.5), (9.5, 1, 9.5), "#iron"), box((7, 1, 7), (9, 14, 9), "#iron", faces=SIDES),
            box((6.5, 14, 6.5), (9.5, 15, 9.5), "#iron"), box((7.5, 15, 7.5), (8.5, 17, 8.5), "#iron", faces=UP_SIDES)]


def fence_side():
    """From the post toward the north edge: two rails and two spear-topped pickets."""
    return ([box((7.5, 12, 0), (8.5, 13, 7), "#iron", faces=("up", "down", "east", "west")),
             box((7.5, 2, 0), (8.5, 3, 7), "#iron", faces=("up", "down", "east", "west"))]
            + picket(7.5, 1.5, 8.5, 2.5) + picket(7.5, 4.5, 8.5, 5.5))


def mirror_z(element):
    """The same element on the south side of the block (z turned to 16 - z), its faces swapped to match."""
    out = dict(element)
    (fx, fy, fz), (tx, ty, tz) = element["from"], element["to"]
    out["from"], out["to"] = [fx, fy, 16 - tz], [tx, ty, 16 - fz]
    faces = dict(element["faces"])
    if "north" in faces or "south" in faces:
        north, south = faces.pop("north", None), faces.pop("south", None)
        if south:
            faces["north"] = south
        if north:
            faces["south"] = north
    out["faces"] = faces
    return out


def gate_model(open_gate, wall):
    """The gate, facing south: two iron posts with spear finials and two leaves of pickets between top and bottom rails;
    open, each leaf swings back along its post. In a wall it sits 3 pixels lower, like vanilla's gates."""
    drop = 3 if wall else 0
    elements = [box((0, 0, 7), (2, 16, 9), "#iron"), box((14, 0, 7), (16, 16, 9), "#iron"),
                box((0.5, 16, 7.5), (1.5, 17.5, 8.5), "#iron", faces=UP_SIDES), box((14.5, 16, 7.5), (15.5, 17.5, 8.5), "#iron", faces=UP_SIDES)]
    if not open_gate:
        elements += [box((2, 13, 7.5), (14, 14, 8.5), "#iron"), box((2, 2, 7.5), (14, 3, 8.5), "#iron"),
                     box((7.5, 0.5, 7.5), (8.5, 14, 8.5), "#iron", faces=SIDES)]
        for x in (3.5, 5.5, 10, 12):
            elements += picket(x, 7.5, x + 1, 8.5, low=0.5)
    else:
        for x0 in (0.5, 14.5):
            elements += [box((x0, 13, 9), (x0 + 1, 14, 15), "#iron"), box((x0, 2, 9), (x0 + 1, 3, 15), "#iron"),
                         box((x0, 0.5, 14), (x0 + 1, 14, 15), "#iron", faces=SIDES)]
            for z in (10, 12):
                elements += picket(x0, z, x0 + 1, z + 1, low=0.5)
    for element in elements:
        element["from"][1] -= drop
        element["to"][1] -= drop
    return fitted(block_model({"iron": "cemetery_iron"}, elements, "cemetery_iron"))


# ---------------------------------------------------------------- the grave mound, angel and pop-up skeleton

def mound_model(raised):
    """A heap of earth facing north; raised, a zombie's forearm claws up out of it, fingers curled forward."""
    elements = [box((1, 0, 1), (15, 3, 15), "#side", textures={"up": "#top"}),
                box((3, 3, 2), (13, 5, 14), "#side", textures={"up": "#top"}, faces=UP_SIDES)]
    if raised:
        # A big hand, so it reads from across a graveyard: a torn sleeve, the forearm, a palm and four fingers clawing
        # forward over the top of the block, and a thumb.
        curl = {"origin": [8, 15.5, 8], "axis": "x", "angle": -22.5}
        elements += [box((6, 5, 6), (10, 8, 10), "#sleeve", faces=UP_SIDES),
                     box((6.5, 8, 6.5), (9.5, 13, 9.5), "#hand", faces=SIDES),
                     box((6, 13, 6.5), (10, 15.5, 9.5), "#hand")]
        for x in (6, 7, 8, 9):
            elements.append(box((x, 15.5, 7), (x + 0.9, 19, 8.2), "#hand", rotation=curl))
        elements.append(box((10, 13, 7.5), (11, 15.5, 8.5), "#hand"))
    return fitted(block_model({"top": "grave_mound_top", "side": "grave_mound_side", "hand": "grave_mound_hand",
                               "sleeve": "grave_mound_sleeve"}, elements, "grave_mound_side"))


def angel_model(half):
    """The Mourning Angel facing north: a plinth and robe below; above, its torso, wings folded behind it (their tips no
    higher than its bowed head) and its head bowed into its hands."""
    if half == "lower":
        elements = [box((2, 0, 2), (14, 3, 14), "#plinth"), box((2.5, 3, 2.5), (13.5, 4, 13.5), "#plinth", faces=UP_SIDES),
                    box((3.5, 4, 4.5), (12.5, 6, 11.5), "#marble", faces=UP_SIDES), box((4, 6, 5), (12, 12, 11), "#marble", faces=SIDES),
                    box((4.5, 12, 5.5), (11.5, 16, 10.5), "#marble", faces=SIDES),
                    box((2.5, 8, 9), (4.5, 16, 13), "#wing", faces=UP_SIDES), box((11.5, 8, 9), (13.5, 16, 13), "#wing", faces=UP_SIDES)]
    else:
        bow = {"origin": [8, 6, 7], "axis": "x", "angle": -22.5}
        elements = [box((4.5, 0, 5.5), (11.5, 5, 10.5), "#marble", faces=SIDES), box((4, 4, 5.5), (12, 6, 10), "#marble"),
                    box((6, 6, 5), (10, 10, 9), "#marble", rotation=bow), box((5.5, 4, 8.5), (10.5, 10, 9.5), "#marble"),
                    box((5, 4, 4), (7, 8, 6), "#marble"), box((9, 4, 4), (11, 8, 6), "#marble"),
                    box((6, 7, 3.5), (10, 9.5, 5), "#marble"),
                    box((2, 0, 8.5), (4.5, 8, 12), "#wing", faces=UP_SIDES), box((2.5, 8, 9), (4, 10, 11.5), "#wing", faces=UP_SIDES),
                    box((11.5, 0, 8.5), (14, 8, 12), "#wing", faces=UP_SIDES), box((12, 8, 9), (13.5, 10, 11.5), "#wing", faces=UP_SIDES)]
    return fitted(block_model({"marble": "mourning_angel_marble", "wing": "mourning_angel_wing", "plinth": "mourning_angel_plinth"},
                              elements, "mourning_angel_marble"))


def skeleton_model(raised):
    """A plank crate facing north. Raised, its lid swings up on a hinge at the back and a skeleton on a spring rises
    out of it, skull to the front and arms flung up."""
    hinge = {"origin": [8, 10, 14.5], "axis": "x", "angle": 45} if raised else None
    elements = [box((2, 0, 2), (14, 9, 14), "#crate", textures={"up": "#inside"}),
                box((1.5, 9, 1.5), (14.5, 10, 14.5), "#lid", rotation=hinge)]
    if raised:
        elements += [box((7.5, 9, 7.5), (8.5, 13, 8.5), "#spring", faces=SIDES),
                     box((5.5, 13, 7), (10.5, 19, 9), "#bone", textures={"north": "#ribs"}),
                     box((5.5, 19, 5.5), (10.5, 24, 10.5), "#bone", textures={"north": "#skull"}),
                     box((1.5, 18, 7.5), (5.5, 19, 8.5), "#bone", rotation={"origin": [5.5, 18.5, 8], "axis": "z", "angle": -22.5}),
                     box((10.5, 18, 7.5), (14.5, 19, 8.5), "#bone", rotation={"origin": [10.5, 18.5, 8], "axis": "z", "angle": 22.5})]
    return fitted(block_model({"crate": "pop_up_skeleton_crate", "lid": "pop_up_skeleton_lid", "inside": "pop_up_skeleton_inside",
                               "spring": "pop_up_skeleton_spring", "bone": "pop_up_skeleton_bone", "ribs": "pop_up_skeleton_ribs",
                               "skull": "pop_up_skeleton_skull"}, elements, "pop_up_skeleton_crate"))


# Vanilla's door blockstate: the y rotation of each closed door, and of each open one by its hinge.
DOOR_CLOSED = {"east": 0, "south": 90, "west": 180, "north": 270}
DOOR_OPEN = {"left": {"east": 90, "south": 180, "west": 270, "north": 0}, "right": {"east": 270, "south": 0, "west": 90, "north": 180}}


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"

    fence, gate = CEMETERY_FENCE["fence"], CEMETERY_FENCE["gate"]
    write(models / f"{fence}_post.json", fitted(block_model({"iron": "cemetery_iron"}, fence_post(), "cemetery_iron")))
    write(models / f"{fence}_side.json", fitted(block_model({"iron": "cemetery_iron"}, fence_side(), "cemetery_iron")))
    write(models / f"{fence}_inventory.json", fitted(block_model({"iron": "cemetery_iron"},
                                                                 fence_post() + fence_side() + [mirror_z(e) for e in fence_side()], "cemetery_iron")))
    parts = [{"apply": {"model": rid(f"block/{fence}_post")}}]
    for direction, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": rid(f"block/{fence}_side")}
        if y:
            apply["y"] = y
        parts.append({"apply": apply, "when": {direction: "true"}})
    write(states / f"{fence}.json", {"multipart": parts})
    write(root / "items" / f"{fence}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{fence}_inventory")}})
    lang[f"block.{MOD}.{fence}"] = CEMETERY_FENCE["fence_display"]

    for open_gate in (False, True):
        for wall in (False, True):
            suffix = ("_wall" if wall else "") + ("_open" if open_gate else "")
            write(models / f"{gate}{suffix}.json", gate_model(open_gate, wall))
    variants = {}
    for facing, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for in_wall in ("false", "true"):
            for is_open in ("false", "true"):
                suffix = ("_wall" if in_wall == "true" else "") + ("_open" if is_open == "true" else "")
                variant = {"model": rid(f"block/{gate}{suffix}")}
                if y:
                    variant["y"] = y
                variants[f"facing={facing},in_wall={in_wall},open={is_open}"] = variant
    write(states / f"{gate}.json", {"variants": variants})
    write(root / "items" / f"{gate}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{gate}")}})
    lang[f"block.{MOD}.{gate}"] = CEMETERY_FENCE["gate_display"]

    for key in ("stone", "chiseled"):
        block = CRYPT[key]
        write(models / f"{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{block}")}})
        write(states / f"{block}.json", {"variants": {"": {"model": rid(f"block/{block}")}}})
        write(root / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block}")}})
        lang[f"block.{MOD}.{block}"] = CRYPT[f"{key}_display"]
    pillar = CRYPT["pillar"]
    write(models / f"{pillar}.json", {"parent": "minecraft:block/cube_column", "textures": {"end": rid(f"block/{pillar}_top"),
                                                                                            "side": rid(f"block/{pillar}")}})
    write(states / f"{pillar}.json", {"variants": {"axis=y": {"model": rid(f"block/{pillar}")},
                                                   "axis=z": {"model": rid(f"block/{pillar}"), "x": 90},
                                                   "axis=x": {"model": rid(f"block/{pillar}"), "x": 90, "y": 90}}})
    write(root / "items" / f"{pillar}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{pillar}")}})
    lang[f"block.{MOD}.{pillar}"] = CRYPT["pillar_display"]

    door = CRYPT["door"]
    textures = {"bottom": rid(f"block/{door}_bottom"), "top": rid(f"block/{door}_top")}
    for half in ("bottom", "top"):
        for hinge in ("left", "right"):
            for open_door in ("", "_open"):
                write(models / f"{door}_{half}_{hinge}{open_door}.json",
                      {"parent": f"minecraft:block/door_{half}_{hinge}{open_door}", "textures": textures})
    variants = {}
    for facing in HORIZONTAL:
        for half, part in (("lower", "bottom"), ("upper", "top")):
            for hinge in ("left", "right"):
                for is_open in ("false", "true"):
                    y = DOOR_OPEN[hinge][facing] if is_open == "true" else DOOR_CLOSED[facing]
                    variant = {"model": rid(f"block/{door}_{part}_{hinge}{'_open' if is_open == 'true' else ''}")}
                    if y:
                        variant["y"] = y
                    variants[f"facing={facing},half={half},hinge={hinge},open={is_open}"] = variant
    write(states / f"{door}.json", {"variants": variants})
    flat_item(root, write, door)
    lang[f"block.{MOD}.{door}"] = CRYPT["door_display"]

    mound = GRAVE_MOUND["block"]
    write(models / f"{mound}.json", mound_model(False))
    write(models / f"{mound}_raised.json", mound_model(True))
    write(states / f"{mound}.json", {"variants": {f"facing={f},raised={str(r).lower()}": turned(rid(f"block/{mound}{'_raised' if r else ''}"), f)
                                                  for f in HORIZONTAL for r in (False, True)}})
    write(root / "items" / f"{mound}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{mound}_raised")}})
    lang[f"block.{MOD}.{mound}"] = GRAVE_MOUND["display"]

    angel = MOURNING_ANGEL["block"]
    for half in ("lower", "upper"):
        write(models / f"{angel}_{half}.json", angel_model(half))
    write(states / f"{angel}.json", {"variants": {f"facing={f},half={h}": turned(rid(f"block/{angel}_{h}"), f)
                                                  for f in HORIZONTAL for h in ("lower", "upper")}})
    flat_item(root, write, angel)
    lang[f"block.{MOD}.{angel}"] = MOURNING_ANGEL["display"]

    skeleton = POP_UP_SKELETON["block"]
    write(models / f"{skeleton}.json", skeleton_model(False))
    write(models / f"{skeleton}_raised.json", skeleton_model(True))
    write(states / f"{skeleton}.json", {"variants": {f"facing={f},raised={str(r).lower()}": turned(rid(f"block/{skeleton}{'_raised' if r else ''}"), f)
                                                     for f in HORIZONTAL for r in (False, True)}})
    flat_item(root, write, skeleton)
    lang[f"block.{MOD}.{skeleton}"] = POP_UP_SKELETON["display"]


def loot(out, write):
    """Every block drops itself; the door and the angel only from their lower halves."""
    for block in (CEMETERY_FENCE["fence"], CEMETERY_FENCE["gate"], CRYPT["stone"], CRYPT["chiseled"], CRYPT["pillar"], GRAVE_MOUND["block"],
                  POP_UP_SKELETON["block"]):
        write(out / f"{block}.json", self_drop(block))
    for block in (CRYPT["door"], MOURNING_ANGEL["block"]):
        write(out / f"{block}.json", self_drop(block, {"type": "minecraft:match_block", "blocks": rid(block), "state": {"half": "lower"}}))


def recipes(out, write, conditions):
    """The crypt stone's chiseled form and pillar are also cut in a stonecutter, one from one."""
    for block in (CRYPT["chiseled"], CRYPT["pillar"]):
        write(out / f"{block}_from_stonecutting.json", {"fabric:load_conditions": conditions(), "type": "minecraft:stonecutting",
                                                        "ingredient": rid(CRYPT["stone"]), "result": {"id": rid(block), "count": 1}})


def tags(tags):
    for registry in ("block", "item"):
        tags.add(registry, "minecraft:fences", rid(CEMETERY_FENCE["fence"]))
        tags.add(registry, "minecraft:fence_gates", rid(CEMETERY_FENCE["gate"]))
        tags.add(registry, "minecraft:doors", rid(CRYPT["door"]))
    for block in (CEMETERY_FENCE["fence"], CEMETERY_FENCE["gate"], CRYPT["stone"], CRYPT["chiseled"], CRYPT["pillar"], CRYPT["door"],
                  MOURNING_ANGEL["block"]):
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    tags.add("block", "minecraft:mineable/shovel", rid(GRAVE_MOUND["block"]))
    tags.add("block", "minecraft:mineable/axe", rid(POP_UP_SKELETON["block"]))
