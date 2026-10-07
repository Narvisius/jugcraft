"""JSON resources for Halloween nights, from tools/agriculture.py: the Wisp in a Jar, the Pumpkin Chunkin'
Trebuchet, the Horseman's Lantern and Cloak, the Headless Horseman's loot, the trebuchet's ammunition and the Candy
Bag's treats, and the names of the new creatures, items and messages.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files: the lantern
blockstate (hanging), block model element rotation and light emission, the zombie's loot table, and the leather
armor's equipment asset. Block model rotations are right-handed: about x, a positive angle tilts +y toward +z.
"""
from agriculture import WISPS, TREBUCHET, CANDY_BAG, HORSEMAN

MOD = "jugcraft"
HORIZONTAL = ("north", "east", "south", "west")
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
SIDES = ("north", "south", "east", "west")
ALL = SIDES + ("up", "down")
PUMPKIN = {"side": "minecraft:block/pumpkin_side", "top": "minecraft:block/pumpkin_top"}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def turned(model, facing):
    variant = {"model": model}
    if FACING_Y[facing]:
        variant["y"] = FACING_Y[facing]
    return variant


def box(lo, hi, texture, faces=ALL, rotation=None, light=None, front=None, up=None, front_uv=None, side_uv=None, all_uv=None):
    """A box whose faces take their UVs from its position; `front`/`up` texture its north/top faces. `front_uv` pins
    the north face's UV, `side_uv` all four sides' and `all_uv` every face's, so a raised copy shows the same part of the
    texture, and a box reaching outside the block never reads outside its texture (26.3 refuses to bake that for a
    texture with see-through pixels)."""
    out = {"from": list(lo), "to": list(hi), "faces": {}}
    for side in faces:
        texture_here = front if side == "north" and front else up if side == "up" and up else texture
        out["faces"][side] = {"texture": texture_here}
        uv = front_uv if side == "north" and front_uv else side_uv if side in SIDES and side_uv else all_uv
        if uv:
            out["faces"][side]["uv"] = list(uv)
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


# ---------------------------------------------------------------- the Wisp in a Jar

def jar_model(hanging):
    """A glass jar with a cork lid and a wisp glowing inside; hanging, it rises and hangs by a string."""
    lift = 2 if hanging else 0
    elements = [
        box((5, lift, 5), (11, 9 + lift, 11), "#glass", faces=SIDES + ("down",), side_uv=(5, 7, 11, 16)),
        box((6.5, 2.5 + lift, 6.5), (9.5, 5.5 + lift, 9.5), "#wisp", light=15, side_uv=(6.5, 6.5, 9.5, 9.5)),
        box((4.5, 9 + lift, 4.5), (11.5, 10.5 + lift, 11.5), "#lid"),
        box((7, 10.5 + lift, 7), (9, 11.5 + lift, 9), "#lid"),
    ]
    if hanging:
        elements.append(box((7.5, 13.5, 7.5), (8.5, 16, 8.5), "#string", faces=SIDES))
    return block_model({"glass": "wisp_jar_glass", "wisp": "wisp_jar_light", "lid": "wisp_jar_lid", "string": "wisp_jar_string"},
                       elements, "wisp_jar_glass")


# ---------------------------------------------------------------- the trebuchet

def trebuchet_model(arm):
    """The frame (rails, cross beams, two uprights braced fore and aft, an axle) and the arm in one of three positions.

    Facing north it throws north. The arm turns about the axle at (8, 19, 8): drawn back (`ready`, `loaded`) its long
    end is down behind and the counterweight up in front; `released`, the long end is up in front and the
    counterweight down behind. Loaded, a pumpkin rests in the sling behind.
    """
    pivot = [8, 19, 8]
    tilt = {"origin": pivot, "axis": "x", "angle": 45}
    elements = [
        box((1, 0, -4), (4, 3, 20), "#wood"),
        box((12, 0, -4), (15, 3, 20), "#wood"),
        box((4, 0, -3), (12, 2, -1), "#wood"),
        box((4, 0, 7), (12, 2, 9), "#wood"),
        box((4, 0, 17), (12, 2, 19), "#wood"),
        box((1.5, 3, 7), (3.5, 21, 9), "#beam"),
        box((12.5, 3, 7), (14.5, 21, 9), "#beam"),
        box((1.5, 3, 1), (3.5, 16, 3), "#beam", rotation={"origin": [2.5, 3, 2], "axis": "x", "angle": 22.5}),
        box((12.5, 3, 1), (14.5, 16, 3), "#beam", rotation={"origin": [13.5, 3, 2], "axis": "x", "angle": 22.5}),
        box((1.5, 3, 13), (3.5, 16, 15), "#beam", rotation={"origin": [2.5, 3, 14], "axis": "x", "angle": -22.5}),
        box((12.5, 3, 13), (14.5, 16, 15), "#beam", rotation={"origin": [13.5, 3, 14], "axis": "x", "angle": -22.5}),
        box((1.5, 18, 7), (14.5, 20, 9), "#iron"),
    ]
    if arm == "released":
        elements += [box((7, 18, -5), (9, 20, 13), "#beam", rotation=tilt),
                     box((4.5, 15, 11), (11.5, 22, 16), "#iron", rotation=tilt)]
    else:
        elements += [box((7, 18, 3), (9, 20, 21), "#beam", rotation=tilt),
                     box((4.5, 15, 0), (11.5, 22, 5), "#iron", rotation=tilt),
                     box((4.5, 2.5, 16.5), (11.5, 3, 23.5), "#rope", faces=("up", "down"), all_uv=(4.5, 4.5, 11.5, 11.5))]
    if arm == "loaded":
        elements.append(box((5, 3, 17), (11, 9, 23), "#pumpkin", up="#pumpkin_top"))
    display = {"gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.36, 0.36, 0.36]},
               "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.2, 0.2, 0.2]},
               "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.4, 0.4, 0.4]},
               "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
               "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.2, 0.2, 0.2]}}
    return block_model({"wood": "trebuchet_wood", "beam": "trebuchet_beam", "iron": "trebuchet_iron", "rope": "trebuchet_rope",
                        "pumpkin": PUMPKIN["side"], "pumpkin_top": PUMPKIN["top"]}, elements, "trebuchet_wood", display)


# ---------------------------------------------------------------- the Horseman's Lantern

def lantern_model(hanging):
    """A carved pumpkin in an iron cage, its face glowing; hanging, it rises and hangs by its handle."""
    lift = 4 if hanging else 0
    elements = [
        box((4, lift, 4), (12, 7 + lift, 12), "#pumpkin", front="#face", front_uv=(4, 9, 12, 16)),
        box((5, 1 + lift, 3.9), (11, 6 + lift, 4), "#glow", faces=("north",), light=15, front_uv=(5, 10, 11, 15)),
        box((3.5, 7 + lift, 3.5), (12.5, 8 + lift, 12.5), "#iron"),
        box((7.5, 8 + lift, 7.5), (8.5, 10 + lift, 8.5), "#iron", faces=SIDES),
        box((6, 10 + lift, 7.5), (10, 11 + lift, 8.5), "#iron"),
    ]
    if hanging:
        elements.append(box((7.5, 11 + lift, 7.5), (8.5, 16, 8.5), "#iron", faces=SIDES))
    return block_model({"pumpkin": "horseman_lantern_pumpkin", "face": "horseman_lantern_face", "glow": "horseman_lantern_glow",
                        "iron": "horseman_lantern_iron"}, elements, "horseman_lantern_pumpkin")


TEXT = {
    "entity.jugcraft.will_o_wisp": "Will-o'-Wisp",
    "entity.jugcraft.flying_pumpkin": "Flying Pumpkin",
    "entity.jugcraft.throw_marker": "Landing Marker",
    "entity.jugcraft.throw_marker.distance": "%s blocks",
    "entity.jugcraft.headless_horseman": "Headless Horseman",
    "entity.jugcraft.flaming_pumpkin": "Flaming Pumpkin",
    "item.jugcraft.candy_bag.homes": "Homes visited tonight: %s (%s fill the bag)",
    "item.jugcraft.candy_bag.no_homes": "No homes visited tonight",
    "message.jugcraft.harvest_moon.rises": "The Harvest Moon rises: crops grow fast tonight",
    "message.jugcraft.harvest_moon.sets": "The Harvest Moon sets",
    "message.jugcraft.trebuchet.board": "Release angle %s°. Longest throws:",
    "message.jugcraft.trebuchet.place": "%s. %s: %s blocks",
    "message.jugcraft.trebuchet.loaded": "Loaded! Release angle %s°: use it with an empty hand to let fly",
    "message.jugcraft.trebuchet.already_loaded": "It's already loaded",
    "message.jugcraft.trebuchet.resetting": "Wait for the arm to swing back",
    "message.jugcraft.trebuchet.angle": "Release angle: %s°",
    "message.jugcraft.trebuchet.landed": "It landed %s blocks away",
    "message.jugcraft.trebuchet.landed_place": "It landed %s blocks away: number %s on the board!",
    "message.jugcraft.horseman.summoned": "The Headless Horseman rides for his head!",
    "message.jugcraft.horseman.rides_off": "The Headless Horseman rides off into the night...",
    "message.jugcraft.horseman.defeated": "The Headless Horseman is gone, back into the dark",
    "message.jugcraft.horseman.disabled": "The Headless Horseman does not ride on this server",
    "message.jugcraft.horseman.out_of_season": "He only rides on Halloween nights",
    "message.jugcraft.horseman.wrong_hour": "He only rides at midnight",
    "message.jugcraft.horseman.wrong_place": "He only rides in the Overworld",
    "message.jugcraft.horseman.peaceful": "He won't ride on Peaceful",
    "message.jugcraft.horseman.no_head": "The scarecrow needs a lit pumpkin for a head",
    "message.jugcraft.horseman.roofed": "He only rides under the open sky",
    "message.jugcraft.horseman.already_riding": "The Headless Horseman is already riding nearby",
}


def assets(root, write, lang):
    models, states, items = root / "models", root / "blockstates", root / "items"

    jar = WISPS["jar"]
    write(models / "block" / f"{jar}.json", jar_model(False))
    write(models / "block" / f"{jar}_hanging.json", jar_model(True))
    write(states / f"{jar}.json", {"variants": {"hanging=false": {"model": rid(f"block/{jar}")},
                                                "hanging=true": {"model": rid(f"block/{jar}_hanging")}}})
    write(models / "item" / f"{jar}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{jar}")}})
    write(items / f"{jar}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{jar}")}})
    lang[f"block.{MOD}.{jar}"] = WISPS["jar_display"]

    trebuchet = TREBUCHET["block"]
    for arm in ("ready", "loaded", "released"):
        write(models / "block" / f"{trebuchet}_{arm}.json", trebuchet_model(arm))
    write(states / f"{trebuchet}.json", {"variants": {f"arm={arm},facing={f}": turned(rid(f"block/{trebuchet}_{arm}"), f)
                                                      for arm in ("ready", "loaded", "released") for f in HORIZONTAL}})
    write(items / f"{trebuchet}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{trebuchet}_loaded")}})
    lang[f"block.{MOD}.{trebuchet}"] = TREBUCHET["display"]

    lantern = HORSEMAN["lantern"]
    write(models / "block" / f"{lantern}.json", lantern_model(False))
    write(models / "block" / f"{lantern}_hanging.json", lantern_model(True))
    write(states / f"{lantern}.json", {"variants": {"hanging=false": {"model": rid(f"block/{lantern}")},
                                                    "hanging=true": {"model": rid(f"block/{lantern}_hanging")}}})
    write(models / "item" / f"{lantern}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{lantern}")}})
    write(items / f"{lantern}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{lantern}")}})
    lang[f"block.{MOD}.{lantern}"] = HORSEMAN["lantern_display"]

    cloak = HORSEMAN["cloak"]
    write(models / "item" / f"{cloak}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{cloak}")}})
    write(items / f"{cloak}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{cloak}")}})
    write(root / "equipment" / f"{cloak}.json", {"layers": {"humanoid": [{"texture": rid(cloak)}]}})
    lang[f"item.{MOD}.{cloak}"] = HORSEMAN["cloak_display"]
    lang.update(TEXT)


# ---------------------------------------------------------------- loot tables and tags

def self_drop(block):
    return {"type": "minecraft:block", "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                                                  "entries": [{"type": "minecraft:item", "name": rid(block)}], "rolls": 1}],
            "random_sequence": rid(f"blocks/{block}")}


def loot(out, write):
    """The three blocks drop themselves; a player who defeats the Horseman gets his loot (out = loot_table/blocks)."""
    for block in (WISPS["jar"], TREBUCHET["block"], HORSEMAN["lantern"]):
        write(out / f"{block}.json", self_drop(block))
    pools = []
    for item, (low, high) in HORSEMAN["loot"]:
        entry = {"type": "minecraft:item", "name": item}
        if high > 1:
            entry["modifier"] = {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}
        pools.append({"condition": {"type": "minecraft:killed_by_player"}, "entries": [entry], "rolls": 1})
    write(out.parent / f"{HORSEMAN['table']}.json", {"type": "minecraft:entity", "pools": pools, "random_sequence": rid(HORSEMAN["table"])})


def tags(tags):
    for item in TREBUCHET["factors"]:
        tags.add("item", TREBUCHET["ammo_tag"], item)
    for item in CANDY_BAG["treats"]:
        tags.add("item", CANDY_BAG["treat_tag"], item)
    tags.add("block", "minecraft:mineable/axe", rid(TREBUCHET["block"]))
    tags.add("block", "minecraft:mineable/pickaxe", rid(HORSEMAN["lantern"]))
