"""JSON resources for the pumpkin regatta and trick-or-treating, from tools/agriculture.py.

Called from agriculture_data.py (assets, loot, tags) and generate_material_data.py (advancements). Loot tables
use the Minecraft 26.x formats, like the rest of the branch.
"""
from agriculture import (PUMPKIN_BOATS, HOLLOW, GIANT_PUMPKIN, REGATTA, TRICK_OR_TREAT, COSTUMES, COSTUME_TAG, COSTUME_HAT_TAG,
                         PORCH_LIGHT_TAG, PORCH_LIGHTS, HALLOWEEN_ADVANCEMENTS, CARVED_VARIETIES, CARVING)

MOD = "jugcraft"
HORIZONTAL = ("north", "east", "south", "west")
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def turned(model, facing):
    variant = {"model": model}
    if FACING_Y[facing]:
        variant["y"] = FACING_Y[facing]
    return variant


def face_uv(lo, hi, side):
    """A face's UVs: its own size in pixels from the texture's corner, clamped to the 16x16 texture."""
    w = {"north": hi[0] - lo[0], "south": hi[0] - lo[0], "east": hi[2] - lo[2], "west": hi[2] - lo[2]}.get(side, hi[0] - lo[0])
    h = {"up": hi[2] - lo[2], "down": hi[2] - lo[2]}.get(side, hi[1] - lo[1])
    return [0, 0, min(16, round(w, 3)), min(16, round(h, 3))]


def box(lo, hi, texture, faces=("north", "south", "east", "west", "up", "down"), front=None):
    """A box with explicit UVs (so tall or wide parts never read past their texture); `front` textures the north face."""
    out = {"from": list(lo), "to": list(hi), "faces": {}}
    for side in faces:
        out["faces"][side] = {"uv": face_uv(lo, hi, side), "texture": front if side == "north" and front else texture}
    return out


# ---------------------------------------------------------------- models

def flag_model():
    """A pole with a chequered flag flying from its top, the cloth on the east side when it faces north."""
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": rid("block/regatta_flag_cloth"), "pole": rid("block/regatta_flag_pole"),
                         "cloth": rid("block/regatta_flag_cloth")},
            "elements": [
                box((7.25, 0, 7.25), (8.75, 16, 8.75), "#pole"),
                {"from": [8.75, 8, 8], "to": [16, 15.5, 8], "shade": False, "faces": {
                    "north": {"uv": [0, 0, 7.25, 7.5], "texture": "#cloth"}, "south": {"uv": [0, 0, 7.25, 7.5], "texture": "#cloth"}}},
            ],
            "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}}}


def buoy_model():
    """A red-and-white striped buoy riding on the water, its foot under the surface."""
    red, white = "#red", "#white"
    return {"parent": "minecraft:block/block",
            "textures": {"particle": rid("block/regatta_buoy_red"), "red": rid("block/regatta_buoy_red"),
                         "white": rid("block/regatta_buoy_white")},
            "elements": [
                box((4, -3, 4), (12, 3, 12), red),
                box((5, 3, 5), (11, 8, 11), white),
                box((5.5, 8, 5.5), (10.5, 12, 10.5), red),
                box((7, 12, 7), (9, 14, 9), white, faces=("north", "south", "east", "west", "up")),
            ]}


# How a costume hat sits: on the head as it is (Minecraft scales head items to fit), smaller everywhere else.
def hat_display(gui_y):
    return {
        "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "gui": {"rotation": [30, 225, 0], "translation": [0, gui_y, 0], "scale": [0.55, 0.55, 0.55]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, gui_y, 0], "scale": [0.55, 0.55, 0.55]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, gui_y * 0.6, 0], "scale": [0.35, 0.35, 0.35]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.35, 0.35, 0.35]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.35, 0.35, 0.35]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, gui_y * 0.6, 0], "scale": [0.4, 0.4, 0.4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, gui_y * 0.6, 0], "scale": [0.4, 0.4, 0.4]},
    }


def witch_hat_model():
    """A wide brim, a purple band and a cone of three tiers bending back to a point. The head fills 1.6-14.4."""
    hat, band = "#hat", "#band"
    return {"textures": {"particle": rid("item/witch_hat"), "hat": rid("item/witch_hat"), "band": rid("item/witch_hat_band")},
            "elements": [
                box((-1, 14, -1), (17, 15, 17), hat),
                box((2.5, 15, 2.5), (13.5, 16.75, 13.5), band),
                box((3, 16.75, 3), (13, 20, 13), hat, faces=("north", "south", "east", "west", "up")),
                box((4.5, 20, 5), (11.5, 23.5, 12), hat, faces=("north", "south", "east", "west", "up")),
                box((6, 23.5, 7), (10, 26.5, 11), hat, faces=("north", "south", "east", "west", "up")),
                box((7.25, 26.5, 8.75), (8.75, 29, 10.25), hat, faces=("north", "south", "east", "west", "up")),
            ],
            "display": hat_display(-7)}


def ghost_sheet_model():
    """A sheet over the head with eye holes in front, hanging down over the shoulders (the icon and the dropped
    item). Worn, it draws nothing on the head: the client's GhostSheetLayer drapes the whole sheet over the wearer."""
    sheet = "#sheet"
    display = hat_display(2)
    display["head"] = {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0, 0, 0]}
    return {"textures": {"particle": rid("item/ghost_sheet"), "sheet": rid("item/ghost_sheet"), "face": rid("item/ghost_sheet_face")},
            "elements": [
                box((1, 0, 1), (15, 15.5, 15), sheet, front="#face"),
                box((0, -7, 0), (16, 1, 16), sheet, faces=("north", "south", "east", "west")),
            ],
            "display": display}


def scarecrow_hat_model():
    """A wide straw hat with a red band."""
    straw, band = "#straw", "#band"
    return {"textures": {"particle": rid("item/scarecrow_hat"), "straw": rid("item/scarecrow_hat"), "band": rid("item/scarecrow_hat_band")},
            "elements": [
                box((-2, 14, -2), (18, 15, 18), straw),
                box((3.5, 15, 3.5), (12.5, 16.5, 12.5), band, faces=("north", "south", "east", "west")),
                box((3.75, 15, 3.75), (12.25, 19.5, 12.25), straw, faces=("north", "south", "east", "west", "up")),
            ],
            "display": hat_display(-4.5)}


TEXT = {
    "item.jugcraft.pumpkin_boat.weight": "Weighs %s kg",
    "item.jugcraft.pumpkin_boat.carved": "Carved",
    "item.jugcraft.pumpkin_boat.carved_lit": "Carved, with a torch inside",
    "message.jugcraft.hollow.hint": "Sneak and use the knife on top to hollow it out into a boat",
    "message.jugcraft.hollow.no_knife": "Hollowing out needs a Carving Knife",
    "message.jugcraft.hollow.not_allowed": "You can't hollow out a pumpkin here",
    "message.jugcraft.hollow.too_small": "Only a giant pumpkin 2 or 3 blocks wide makes a boat",
    "message.jugcraft.hollow.not_a_pumpkin": "That isn't a giant pumpkin",
    "message.jugcraft.regatta.countdown": "%s...",
    "message.jugcraft.regatta.go": "Go!",
    "message.jugcraft.regatta.mark": "Buoy %s of %s: %s",
    "message.jugcraft.regatta.finished": "Finished in %s!",
    "message.jugcraft.regatta.no_flag": "Finished in %s, but the flag is gone: no board to put it on",
    "message.jugcraft.regatta.too_slow": "Out of time: this run doesn't count",
    "message.jugcraft.regatta.too_fast": "Faster than any paddle: this run doesn't count",
    "message.jugcraft.regatta.abandoned": "You left the boat: this run doesn't count",
    "message.jugcraft.regatta.course": "This course has %s buoys. Best times:",
    "message.jugcraft.regatta.board": "%s. %s: %s",
    "message.jugcraft.regatta.no_course": "No numbered buoys near this flag yet",
    "message.jugcraft.regatta.ready": "Get ready: round %s buoys in order, then back to the flag",
    "message.jugcraft.regatta.not_driving": "Only the driver of a pumpkin boat can start a run",
    "message.jugcraft.regatta.buoy": "Buoy %s",
    "message.jugcraft.trick_or_treat.treat": "Treat! %s x%s",
    "message.jugcraft.trick_or_treat.trick": "Trick! Someone's played a prank on you",
    "message.jugcraft.trick_or_treat.out_of_season": "Nobody's handing out treats tonight",
    "message.jugcraft.trick_or_treat.wrong_hour": "Trick-or-treating is from dusk until midnight",
    "message.jugcraft.trick_or_treat.no_costume": "\"Where's your costume?\"",
    "message.jugcraft.trick_or_treat.no_porch_light": "No porch light: they aren't handing out treats here",
    "message.jugcraft.trick_or_treat.nobody_home": "Nobody seems to live here",
    "message.jugcraft.trick_or_treat.not_a_door": "Knock on a wooden door",
    "message.jugcraft.trick_or_treat.busy": "Wait for them to answer!",
    "message.jugcraft.trick_or_treat.gone": "You'd walked off before they answered",
}


def assets(root, write, lang):
    models = root / "models"
    for boat, info in PUMPKIN_BOATS.items():
        write(models / "item" / f"{boat}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{boat}")}})
        write(root / "items" / f"{boat}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{boat}")}})
        lang[f"item.{MOD}.{boat}"] = info["display"]
        lang[f"entity.{MOD}.{boat}"] = info["display"]

    flag, buoy = REGATTA["flag"], REGATTA["buoy"]
    write(models / "block" / f"{flag}.json", flag_model())
    write(root / "blockstates" / f"{flag}.json", {"variants": {f"facing={f}": turned(rid(f"block/{flag}"), f) for f in HORIZONTAL}})
    write(root / "items" / f"{flag}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{flag}")}})
    lang[f"block.{MOD}.{flag}"] = REGATTA["flag_display"]
    write(models / "block" / f"{buoy}.json", buoy_model())
    write(root / "blockstates" / f"{buoy}.json", {"variants": {
        f"number={n}": {"model": rid(f"block/{buoy}")} for n in range(1, REGATTA["max_number"] + 1)}})
    write(root / "items" / f"{buoy}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{buoy}")}})
    lang[f"block.{MOD}.{buoy}"] = REGATTA["buoy_display"]

    bag = TRICK_OR_TREAT["bag"]
    write(models / "item" / f"{bag}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{bag}")}})
    write(root / "items" / f"{bag}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{bag}")}})
    lang[f"item.{MOD}.{bag}"] = TRICK_OR_TREAT["bag_display"]

    # Retain the equippable asset ID so the sheet enters the render state, but omit the invalid empty file.
    # The missing-asset fallback has no layers; GhostSheetLayer alone drapes the sheet over the wearer.
    (root / "equipment" / "ghost_sheet.json").unlink(missing_ok=True)
    for hat, model in (("witch_hat", witch_hat_model()), ("ghost_sheet", ghost_sheet_model()), ("scarecrow_hat", scarecrow_hat_model())):
        write(models / "item" / f"{hat}.json", model)
        write(root / "items" / f"{hat}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{hat}")}})
        lang[f"item.{MOD}.{hat}"] = COSTUMES[hat]

    for key, info in HALLOWEEN_ADVANCEMENTS.items():
        lang[f"advancements.{MOD}.{key}.title"] = info["title"]
        lang[f"advancements.{MOD}.{key}.description"] = info["description"]
    lang.update(TEXT)


# ---------------------------------------------------------------- loot tables

def uniform(low, high):
    return {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}


def self_drop(block):
    return {"type": "minecraft:block", "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                                                  "entries": [{"type": "minecraft:item", "name": rid(block)}], "rolls": 1}],
            "random_sequence": rid(f"blocks/{block}")}


def loot(out, write):
    """The flag's and buoy's own drops (out = loot_table/blocks), the treat table and the hollowing table."""
    tables = out.parent
    for block in (REGATTA["flag"], REGATTA["buoy"]):
        write(out / f"{block}.json", self_drop(block))

    entries = []
    for item, weight, (low, high) in TRICK_OR_TREAT["treats"]:
        entry = {"type": "minecraft:item", "name": item, "weight": weight}
        if high > 1:
            entry["modifier"] = uniform(low, high)
        entries.append(entry)
    write(tables / f"{TRICK_OR_TREAT['table']}.json", {"type": "minecraft:gift", "pools": [{"entries": entries, "rolls": 1}],
                                                        "random_sequence": rid(TRICK_OR_TREAT["table"])})

    giant = GIANT_PUMPKIN["block"]
    pools = []
    for size, (low, high) in HOLLOW["guts"].items():
        pools.append({"condition": {"type": "minecraft:match_block", "blocks": rid(giant), "state": {"size": str(size)}},
                      "entries": [{"type": "minecraft:item", "name": rid("pumpkin_guts"), "modifier": uniform(low, high)}], "rolls": 1})
    for size, (low, high) in HOLLOW["seeds"].items():
        pools.append({"condition": {"type": "minecraft:match_block", "blocks": rid(giant), "state": {"size": str(size)}},
                      "entries": [{"type": "minecraft:item", "name": rid(GIANT_PUMPKIN["seed"]), "modifier": uniform(low, high)}],
                      "rolls": 1})
    write(tables / f"{HOLLOW['table']}.json", {"type": "minecraft:block_interact", "pools": pools, "random_sequence": rid(HOLLOW["table"])})


# ---------------------------------------------------------------- tags

def tags(tags):
    carved = [rid(CARVING["block"])] + [rid(c) for c in CARVED_VARIETIES.values()]
    for item in ["minecraft:carved_pumpkin"] + carved + [rid(hat) for hat in COSTUMES]:
        tags.add("item", COSTUME_TAG, item)
    for hat in COSTUMES:
        tags.add("item", COSTUME_HAT_TAG, rid(hat))
    for item in carved:
        tags.add("item", "minecraft:gaze_disguise_equipment", item)
    for block in PORCH_LIGHTS:
        tags.add("block", PORCH_LIGHT_TAG, block)
    for block in (REGATTA["flag"], REGATTA["buoy"]):
        tags.add("block", "minecraft:mineable/axe", rid(block))


# ---------------------------------------------------------------- advancements

def advancements(data, write):
    """Advancements granted from code (criterion "done"), in vanilla's Husbandry tab."""
    for key, info in HALLOWEEN_ADVANCEMENTS.items():
        write(data / MOD / "advancement" / f"{key}.json", {
            "parent": "minecraft:husbandry/root",
            "display": {"icon": {"id": info["icon"]}, "title": {"translate": f"advancements.{MOD}.{key}.title"},
                        "description": {"translate": f"advancements.{MOD}.{key}.description"}, "frame": info["frame"],
                        "show_toast": True, "announce_to_chat": info["frame"] != "task"},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})
