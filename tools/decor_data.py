"""JSON resources for the first batch of Halloween decorations, from tools/agriculture.py: the String Light Hook and
its strand, the Candy Bowl, the Coffin, the Haunted Portrait and the Fog Machine; their names and messages, loot,
tags and the fog particle's sprite list.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files: the lever
blockstate (face and facing), block model element rotation and light emission, two-block loot (the scarecrow's), and
particle definitions. Block model rotations are right-handed: about z, a positive angle turns +x toward +y.
"""
from agriculture import STRING_LIGHTS, CANDY_BOWL, COFFIN, HAUNTED_PORTRAIT, FOG_MACHINE

MOD = "jugcraft"
HORIZONTAL = ("north", "east", "south", "west")
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
SIDES = ("north", "south", "east", "west")
ALL = SIDES + ("up", "down")
# The lever's rotations for each face and facing (its model stands on the floor, pointing up).
LEVER = {("floor", "north"): (0, 0), ("floor", "east"): (0, 90), ("floor", "south"): (0, 180), ("floor", "west"): (0, 270),
         ("wall", "north"): (90, 0), ("wall", "east"): (90, 90), ("wall", "south"): (90, 180), ("wall", "west"): (90, 270),
         ("ceiling", "north"): (180, 180), ("ceiling", "east"): (180, 270), ("ceiling", "south"): (180, 0), ("ceiling", "west"): (180, 90)}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def turned(model, facing, x=0):
    variant = {"model": model}
    if x:
        variant["x"] = x
    if FACING_Y[facing]:
        variant["y"] = FACING_Y[facing]
    return variant


def box(lo, hi, texture, faces=ALL, rotation=None, light=None, textures=None, uvs=None):
    """A box whose faces take their UVs from its position; `textures`/`uvs` override single faces by side."""
    out = {"from": list(lo), "to": list(hi), "faces": {}}
    for side in faces:
        out["faces"][side] = {"texture": (textures or {}).get(side, texture)}
        if uvs and side in uvs:
            out["faces"][side]["uv"] = list(uvs[side])
    if rotation:
        out["rotation"] = rotation
    if light:
        out["light_emission"] = light
    return out


def block_model(textures, elements, particle, display=None):
    model = {"parent": "minecraft:block/block", "textures": {"particle": rid(f"block/{particle}"), **{
        key: (name if ":" in name else rid(f"block/{name}")) for key, name in textures.items()}}, "elements": elements}
    if display:
        model["display"] = display
    return model


def flat_item(root, write, name):
    write(root / "models" / "item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})


# ---------------------------------------------------------------- models

def hook_model(lit):
    """A small iron post on a plate with a pumpkin bulb on top, standing on the floor (the blockstate turns it)."""
    return block_model({"iron": "string_light_hook_iron", "bulb": "string_light_bulb_lit" if lit else "string_light_bulb"}, [
        box((6, 0, 6), (10, 1, 10), "#iron"),
        box((7.5, 1, 7.5), (8.5, 5, 8.5), "#iron", faces=SIDES),
        box((6.5, 5, 6.5), (9.5, 8, 9.5), "#bulb", light=15 if lit else None),
    ], "string_light_hook_iron")


def bowl_model(fill):
    """A two-tier terracotta bowl with a grin on its front (north), and candy heaped to `fill` (0-3)."""
    elements = [
        box((5, 0, 5), (11, 1, 11), "#bowl"),
        box((4, 1, 4), (12, 3, 5), "#bowl", textures={"north": "#face"}),
        box((4, 1, 11), (12, 3, 12), "#bowl"),
        box((4, 1, 5), (5, 3, 11), "#bowl"),
        box((11, 1, 5), (12, 3, 11), "#bowl"),
        box((3, 3, 3), (13, 6, 4), "#bowl", textures={"north": "#face"}),
        box((3, 3, 12), (13, 6, 13), "#bowl"),
        box((3, 3, 4), (4, 6, 12), "#bowl"),
        box((12, 3, 4), (13, 6, 12), "#bowl"),
    ]
    if fill >= 1:
        elements.append(box((5, 1, 5), (11, 2 if fill == 1 else 3, 11), "#candy", faces=("up",) if fill == 1 else ("up", "north", "south", "east", "west")))
    if fill >= 2:
        elements.append(box((4, 3, 4), (12, 4.5, 12), "#candy", faces=("up",)))
    if fill >= 3:
        elements += [box((5, 4.5, 5), (11, 6.5, 11), "#candy"), box((6.5, 6.5, 6.5), (9.5, 7.5, 9.5), "#candy")]
    return block_model({"bowl": "candy_bowl", "face": "candy_bowl_face", "candy": "candy_bowl_candy"}, elements, "candy_bowl")


def coffin_model(part, open_lid):
    """One half of the coffin, its head toward north: the body (velvet inside), the lid and brass handles. Open, the
    lid swings up 45 degrees on a hinge along its east edge."""
    if part == "head":
        body = [((3, 0, 1), (13, 8, 3)), ((1, 0, 3), (15, 8, 16))]
        lid = [((3, 8, 1), (13, 10, 3)), ((1, 8, 3), (15, 10, 16))]
        handles = [((0.5, 4, 7), (1, 5, 13)), ((15, 4, 7), (15.5, 5, 13))]
        lid_texture = "#lid"
    else:
        body = [((2, 0, 0), (14, 8, 10)), ((3, 0, 10), (13, 8, 15))]
        lid = [((2, 8, 0), (14, 10, 10)), ((3, 8, 10), (13, 10, 15))]
        handles = [((1.5, 4, 2), (2, 5, 8)), ((14, 4, 2), (14.5, 5, 8))]
        lid_texture = "#lid_plain"
    hinge = {"origin": [15, 10, 8], "axis": "z", "angle": -45} if open_lid else None
    elements = [box(lo, hi, "#wood", textures={"up": "#velvet"}) for lo, hi in body]
    elements += [box(lo, hi, "#wood", textures={"up": lid_texture, "down": "#velvet"}, rotation=hinge) for lo, hi in lid]
    elements += [box(lo, hi, "#brass") for lo, hi in handles]
    return block_model({"wood": "coffin_wood", "velvet": "coffin_velvet", "lid": "coffin_lid", "lid_plain": "coffin_lid_plain",
                        "brass": "coffin_brass"}, elements, "coffin_wood")


def portrait_model(name):
    """A painting hung on the wall to the south, facing north: the canvas, and a raised gilt frame round it."""
    return block_model({"canvas": f"haunted_portrait_{name}", "frame": "haunted_portrait_frame"}, [
        box((0, 0, 15), (16, 16, 16), "#frame", textures={"north": "#canvas"}, uvs={"north": (0, 0, 16, 16)}),
        box((0, 15, 14), (16, 16, 15), "#frame", faces=("north", "down", "up", "east", "west")),
        box((0, 0, 14), (16, 1, 15), "#frame", faces=("north", "up", "down", "east", "west")),
        box((0, 1, 14), (1, 15, 15), "#frame", faces=("north", "east", "west")),
        box((15, 1, 14), (16, 15, 15), "#frame", faces=("north", "east", "west")),
    ], f"haunted_portrait_{name}")


def fog_model(running):
    """The fog machine facing north: a riveted cabinet, a brass tank on top with a cap, a grille nozzle out of the
    front, a gauge beside it, and a lamp that glows while it runs."""
    return block_model({"body": "fog_machine_body", "steel": "fog_machine_steel", "tank": "fog_machine_tank", "grille": "fog_machine_grille",
                        "gauge": "fog_machine_gauge", "lamp": "fog_machine_lamp_lit" if running else "fog_machine_lamp"}, [
        box((2, 0, 3), (14, 10, 13), "#body", textures={"up": "#steel", "down": "#steel"}),
        box((4, 10, 4), (12, 15, 12), "#tank", textures={"up": "#steel", "down": "#steel"}),
        box((7, 15, 7), (9, 16, 9), "#steel"),
        box((6, 3, 0), (10, 7, 3), "#steel", textures={"north": "#grille"}),
        box((10, 5, 2.8), (13, 8, 3), "#gauge", faces=("north",), uvs={"north": (4, 4, 12, 12)}),
        box((3, 10, 10), (5, 13, 12), "#lamp", light=15 if running else None),
    ], "fog_machine_body")


TEXT = {
    "message.jugcraft.string_lights.first": "Now use the strand on another hook, up to %s blocks away",
    "message.jugcraft.string_lights.strung": "Strung between the hooks",
    "message.jugcraft.string_lights.same_hook": "Use it on a second hook",
    "message.jugcraft.string_lights.too_far": "Too far: hooks can be at most %s blocks apart",
    "message.jugcraft.string_lights.gone": "The first hook is gone; start again",
    "message.jugcraft.string_lights.already_strung": "Those two are already strung, or both hooks already hold a strand",
    "message.jugcraft.string_lights.taken_down": "You take the strand down",
    "message.jugcraft.candy_bowl.filled": "%s of %s treats in the bowl",
    "message.jugcraft.candy_bowl.full": "The bowl is full",
    "message.jugcraft.candy_bowl.count": "%s of %s treats in the bowl",
    "message.jugcraft.candy_bowl.taken": "You take a treat",
    "message.jugcraft.candy_bowl.empty": "The bowl is empty",
    "message.jugcraft.candy_bowl.had_one": "You've had your treat from this bowl tonight",
    "message.jugcraft.coffin.restless": "Nothing rests easy here",
    "container.jugcraft.coffin": "Coffin",
    "message.jugcraft.haunted_portrait.lady": "The Lady in Black",
    "message.jugcraft.haunted_portrait.captain": "The Old Captain",
    "message.jugcraft.haunted_portrait.cat": "The Black Cat",
    "message.jugcraft.haunted_portrait.owl": "The Owl",
    "message.jugcraft.fog_machine.on": "Fog machine on",
    "message.jugcraft.fog_machine.off": "Fog machine off",
    "message.jugcraft.fog_machine.radius": "Fog radius: %s blocks",
}


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"

    hook = STRING_LIGHTS["hook"]
    write(models / f"{hook}.json", hook_model(False))
    write(models / f"{hook}_lit.json", hook_model(True))
    write(states / f"{hook}.json", {"variants": {
        f"face={face},facing={facing},lit={str(lit).lower()}": dict({"model": rid(f"block/{hook}{'_lit' if lit else ''}")},
                                                                     **({"x": x} if x else {}), **({"y": y} if y else {}))
        for (face, facing), (x, y) in LEVER.items() for lit in (False, True)}})
    flat_item(root, write, hook)
    flat_item(root, write, STRING_LIGHTS["strand"])
    lang[f"block.{MOD}.{hook}"] = STRING_LIGHTS["hook_display"]
    lang[f"item.{MOD}.{STRING_LIGHTS['strand']}"] = STRING_LIGHTS["strand_display"]

    bowl = CANDY_BOWL["block"]
    for fill in range(4):
        write(models / f"{bowl}_{fill}.json", bowl_model(fill))
    write(states / f"{bowl}.json", {"variants": {f"facing={f},fill={fill}": turned(rid(f"block/{bowl}_{fill}"), f)
                                                 for f in HORIZONTAL for fill in range(4)}})
    flat_item(root, write, bowl)
    lang[f"block.{MOD}.{bowl}"] = CANDY_BOWL["display"]

    coffin = COFFIN["block"]
    for part in ("head", "foot"):
        for open_lid in (False, True):
            write(models / f"{coffin}_{part}{'_open' if open_lid else ''}.json", coffin_model(part, open_lid))
    write(states / f"{coffin}.json", {"variants": {
        f"facing={f},open={str(o).lower()},part={part}": turned(rid(f"block/{coffin}_{part}{'_open' if o else ''}"), f)
        for f in HORIZONTAL for o in (False, True) for part in ("head", "foot")}})
    flat_item(root, write, coffin)
    lang[f"block.{MOD}.{coffin}"] = COFFIN["display"]

    portrait = HAUNTED_PORTRAIT["block"]
    for name in HAUNTED_PORTRAIT["portraits"]:
        write(models / f"{portrait}_{name}.json", portrait_model(name))
    write(states / f"{portrait}.json", {"variants": {f"facing={f},portrait={name}": turned(rid(f"block/{portrait}_{name}"), f)
                                                     for f in HORIZONTAL for name in HAUNTED_PORTRAIT["portraits"]}})
    flat_item(root, write, portrait)
    lang[f"block.{MOD}.{portrait}"] = HAUNTED_PORTRAIT["display"]

    fog = FOG_MACHINE["block"]
    write(models / f"{fog}.json", fog_model(False))
    write(models / f"{fog}_running.json", fog_model(True))
    write(states / f"{fog}.json", {"variants": {f"facing={f},running={str(r).lower()}": turned(rid(f"block/{fog}{'_running' if r else ''}"), f)
                                                for f in HORIZONTAL for r in (False, True)}})
    flat_item(root, write, fog)
    lang[f"block.{MOD}.{fog}"] = FOG_MACHINE["display"]
    write(root / "particles" / "fog.json", {"textures": [rid("fog")]})
    lang.update(TEXT)


# ---------------------------------------------------------------- loot tables and tags

def self_drop(block, condition=None):
    terms = [{"type": "minecraft:survives_explosion"}] + ([condition] if condition else [])
    return {"type": "minecraft:block", "pools": [{"condition": terms[0] if len(terms) == 1 else {"type": "minecraft:all_of", "terms": terms},
                                                  "entries": [{"type": "minecraft:item", "name": rid(block)}], "rolls": 1}],
            "random_sequence": rid(f"blocks/{block}")}


def loot(out, write):
    """Each block drops itself; the coffin only from its head half (out = loot_table/blocks)."""
    for block in (STRING_LIGHTS["hook"], CANDY_BOWL["block"], HAUNTED_PORTRAIT["block"], FOG_MACHINE["block"]):
        write(out / f"{block}.json", self_drop(block))
    write(out / f"{COFFIN['block']}.json", self_drop(COFFIN["block"], {"type": "minecraft:match_block", "blocks": rid(COFFIN["block"]),
                                                                       "state": {"part": "head"}}))


def tags(tags):
    for block in (STRING_LIGHTS["hook"], CANDY_BOWL["block"], FOG_MACHINE["block"]):
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    for block in (COFFIN["block"], HAUNTED_PORTRAIT["block"]):
        tags.add("block", "minecraft:mineable/axe", rid(block))
