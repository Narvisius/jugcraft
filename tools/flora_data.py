"""Generated assets for the graveyard flora (tools/plants.py plants with "art": "flora", and the mandrake): models from
tools/flora_models.py, blockstates and item models. Textures are painted by flora_textures() for generate_textures.py.

Small plants turn at random (four ways) where they stand; tall ones, whose two blocks must match, stand as built.
"""
import flora_art as fa
import flora_models as fm
import plants as pl

MOD = "jugcraft"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
# Multiface blocks: the turn that takes the model (drawn against the north face) to each face, as vanilla's glow lichen.
FACE_TURNS = {"north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def turned(model):
    """A blockstate variant list: the model turned each of four ways at random."""
    return [{"model": rid(f"block/{model}")}] + [{"model": rid(f"block/{model}"), "y": y} for y in (90, 180, 270)]


def write_models(root, write, sc, display=None):
    for name, elements in sc.models.items():
        if name.endswith("_item"):
            continue
        write(root / "models" / "block" / f"{name}.json", fa.model(sc.name, elements))


def item(root, write, name, model, display):
    """An item drawn as a sculpted model (`model`: a block model, or None to write one of the item's own elements)."""
    write(root / "models" / "item" / f"{name}.json", {"parent": rid(f"block/{model}"), "display": display})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})


def plant_assets(root, write, plant):
    """Models, blockstate and item of one flora plant."""
    info = pl.PLANTS[plant]
    kind = info["kind"]
    sc = fm.build(plant)
    write_models(root, write, sc)
    states = root / "blockstates"
    if kind in ("flower", "floor_plant", "grass"):
        write(states / f"{plant}.json", {"variants": {"": turned(plant)}})
        item(root, write, plant, plant, fa.PLANT_DISPLAY)
        if kind == "flower":
            write(root / "models" / "block" / f"{pl.potted(plant)}.json", fa.potted_model(plant, sc.models[plant]))
            write(states / f"{pl.potted(plant)}.json", {"variants": {"": {"model": rid(f"block/{pl.potted(plant)}")}}})
    elif kind in pl.TALL:
        write(states / f"{plant}.json", {"variants": {"half=lower": {"model": rid(f"block/{plant}_bottom")},
                                                       "half=upper": {"model": rid(f"block/{plant}_top")}}})
        write(root / "models" / "item" / f"{plant}.json", fa.model(plant, sc.models[f"{plant}_item"], fa.TALL_DISPLAY))
        write(root / "items" / f"{plant}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{plant}")}})
    elif kind == "flowerbed":
        parts = []
        for n in range(1, 5):
            amounts = "|".join(str(a) for a in range(n, 5))
            for facing, y in FACINGS.items():
                when = {"facing": facing} if n == 1 else {"facing": facing, "flower_amount": amounts}
                apply = {"model": rid(f"block/{plant}_{n}")}
                if y:
                    apply["y"] = y
                parts.append({"apply": apply, "when": when})
        write(states / f"{plant}.json", {"multipart": parts})
        whole = [e for n in range(1, 5) for e in sc.models[f"{plant}_{n}"]]
        write(root / "models" / "item" / f"{plant}.json", fa.model(plant, whole, fa.PLANT_DISPLAY))
        write(root / "items" / f"{plant}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{plant}")}})
    elif kind == "hanging":
        write(states / f"{plant}.json", {"variants": {"tip=false": turned(plant), "tip=true": turned(f"{plant}_tip")}})
        item(root, write, plant, plant, fa.PLANT_DISPLAY)
    elif kind == "vine":
        parts = [{"apply": {"model": rid(f"block/{plant}"), **turn}, "when": {face: "true"}} for face, turn in FACE_TURNS.items()]
        write(states / f"{plant}.json", {"multipart": parts})
        write(root / "models" / "item" / f"{plant}.json", {"parent": "minecraft:item/generated",
                                                          "textures": {"layer0": rid(f"item/{plant}")}})
        write(root / "items" / f"{plant}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{plant}")}})
    else:
        raise ValueError(f"no flora art for kind {kind}")


def mandrake_assets(root, write, crop, wild, root_item, stages):
    """The mandrake crop (age -> one of four stage models), the wild mandrake and the root (a figure, screaming)."""
    sc = fm.build("mandrake")
    write_models(root, write, sc)
    write(root / "blockstates" / f"{crop}.json", {"variants": {
        f"age={age}": {"model": rid(f"block/mandrake_stage{stage}")} for age, stage in enumerate(stages)}})
    write(root / "blockstates" / f"{wild}.json", {"variants": {"": turned(wild)}})
    item(root, write, wild, wild, fa.PLANT_DISPLAY)
    item(root, write, root_item, "mandrake_root", ROOT_DISPLAY)


ROOT_DISPLAY = {
    "gui": {"rotation": [20, 200, 0], "translation": [0, -0.5, 0], "scale": [0.95, 0.95, 0.95]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 160, 0], "translation": [0, 2.5, 1.5], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"rotation": [0, 160, 0], "translation": [0, 3, 0], "scale": [0.55, 0.55, 0.55]},
}


def flora_textures():
    """(kind, name) -> image: each flora plant's texture, the mandrake's, and the ivy's item picture."""
    out = {}
    for plant in pl.flora() + ["mandrake"]:
        out[("block", plant)] = fm.build(plant).atlas.img
    ivy = fm.build("creeping_ivy")
    u0, v0, u1, v1 = ivy.atlas.uvs["sheet"]
    scale = fa.SIZE / 16
    sheet = ivy.atlas.img.crop((round(u0 * scale), round(v0 * scale), round(u1 * scale), round(v1 * scale)))
    out[("item", "creeping_ivy")] = sheet
    return out
