"""JSON resources for soil, compost and storage (tools/soil.py), in the owner's own textures: Rich Soil and its farmland,
Organic Compost, the produce crates, the Bag of Corn Kernels and the baskets (their models, blockstates, loot, tags and
names). The recipes come from tools/agriculture.py.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files.
"""
from decor_data import MOD, rid, turned, self_drop
from soil import RICH_SOIL, RICH_FARMLAND, COMPOST, CRATES, SACKS, SACK_TEXTURES, BASKETS

SIDES = ("north", "south", "east", "west")


def model(parent, **textures):
    return {"parent": parent, "textures": {key: rid(f"block/{value}") for key, value in textures.items()}}


def face(uv, texture, cull=None):
    out = {"texture": texture, "uv": [round(v, 3) for v in uv]}
    if cull:
        out["cullface"] = cull
    return out


def farmland(top, side):
    """Farmland's slab, 15 pixels tall: its top, its sides cut a pixel short, Rich Soil below."""
    faces = {"down": face([0, 0, 16, 16], "#soil", "down"), "up": face([0, 0, 16, 16], "#top")}
    faces.update({s: face([0, 1, 16, 16], "#side", s) for s in SIDES})
    return {"parent": "minecraft:block/block",
            "textures": {"particle": rid(f"block/{RICH_SOIL['texture']}"), "soil": rid(f"block/{RICH_SOIL['texture']}"),
                         "top": rid(f"block/{top}"), "side": rid(f"block/{side}")},
            "elements": [{"from": [0, 0, 0], "to": [16, 15, 16], "faces": faces}]}


def basket(textures):
    """The owner's basket: a woven floor and four woven walls a pixel thick, open at the top, its rim on the walls' tops.
    Each wall's outer and inner face show the side (the hand hole goes through), the floor the bottom inside and out."""
    elements = [{"from": [0, 0, 0], "to": [16, 1, 16], "faces": {
        "down": face([0, 0, 16, 16], "#bottom", "down"), "up": face([0, 0, 16, 16], "#bottom"),
        **{s: face([0, 15, 16, 16], "#side", s) for s in SIDES}}}]
    walls = {"north": ([0, 1, 0], [16, 16, 1]), "south": ([0, 1, 15], [16, 16, 16]),
             "west": ([0, 1, 1], [1, 16, 15]), "east": ([15, 1, 1], [16, 16, 15])}
    for side, (lo, hi) in walls.items():
        x0, _, z0 = lo
        x1, _, z1 = hi
        faces = {"up": face([x0, z0, x1, z1], "#top"), "down": face([x0, z0, x1, z1], "#bottom")}
        if side in ("north", "south"):
            faces.update({"north": face([0, 0, 16, 15], "#side", "north" if side == "north" else None),
                          "south": face([0, 0, 16, 15], "#side", "south" if side == "south" else None),
                          "west": face([z0, 0, z1, 15], "#side", "west"), "east": face([16 - z1, 0, 16 - z0, 15], "#side", "east")})
        else:
            faces.update({"west": face([1, 0, 15, 15], "#side", "west" if side == "west" else None),
                          "east": face([1, 0, 15, 15], "#side", "east" if side == "east" else None),
                          "north": face([16 - x1, 0, 16 - x0, 15], "#side"), "south": face([x0, 0, x1, 15], "#side")})
        elements.append({"from": lo, "to": hi, "faces": faces})
    return {"parent": "minecraft:block/block",
            "textures": {"particle": rid(f"block/{textures['side']}"), **{k: rid(f"block/{v}") for k, v in textures.items()}},
            "elements": elements}


def block_item(root, write, name, block_model=None):
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{block_model or name}")}})


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"

    name = RICH_SOIL["block"]
    write(models / f"{name}.json", model("minecraft:block/cube_all", all=RICH_SOIL["texture"]))
    write(states / f"{name}.json", {"variants": {"": {"model": rid(f"block/{name}")}}})
    block_item(root, write, name)
    lang[f"block.{MOD}.{name}"] = RICH_SOIL["display"]

    name, textures = RICH_FARMLAND["block"], RICH_FARMLAND["textures"]
    write(models / f"{name}.json", farmland(textures["dry"], RICH_SOIL["texture"]))
    write(models / f"{name}_moist.json", farmland(textures["moist"], textures["moist_side"]))
    write(states / f"{name}.json", {"variants": {f"moisture={m}": {"model": rid(f"block/{name}{'_moist' if m == 7 else ''}")}
                                                 for m in range(8)}})
    lang[f"block.{MOD}.{name}"] = RICH_FARMLAND["display"]

    name = COMPOST["block"]
    for stage, texture in enumerate(COMPOST["textures"]):
        write(models / f"{name}_{stage}.json", model("minecraft:block/cube_all", all=texture))
    write(states / f"{name}.json", {"variants": {f"composting={s}": {"model": rid(f"block/{name}_{s}")} for s in range(COMPOST["stages"])}})
    block_item(root, write, name, f"{name}_0")
    lang[f"block.{MOD}.{name}"] = COMPOST["display"]

    for name, info in CRATES.items():
        tex = info["textures"]
        write(models / f"{name}.json", model("minecraft:block/cube_bottom_top", top=tex["top"], side=tex["side"], bottom=tex["bottom"]))
        write(states / f"{name}.json", {"variants": {"": {"model": rid(f"block/{name}")}}})
        block_item(root, write, name)
        lang[f"block.{MOD}.{name}"] = info["display"]

    for name, info in SACKS.items():
        write(models / f"{name}.json", model("minecraft:block/orientable_with_bottom", top=info["top"], bottom=SACK_TEXTURES["bottom"],
                                             side=SACK_TEXTURES["side"], front=SACK_TEXTURES["front"], particle=SACK_TEXTURES["side"]))
        write(states / f"{name}.json", {"variants": {f"facing={f}": turned(rid(f"block/{name}"), f) for f in SIDES}})
        block_item(root, write, name)
        lang[f"block.{MOD}.{name}"] = info["display"]

    for name, info in BASKETS.items():
        write(models / f"{name}.json", basket(info["textures"]))
        write(states / f"{name}.json", {"variants": {"": {"model": rid(f"block/{name}")}}})
        block_item(root, write, name)
        lang[f"block.{MOD}.{name}"] = info["display"]
    lang[f"container.{MOD}.basket"] = "Basket"


def loot(out, write):
    """Each block drops itself; Rich Soil Farmland drops Rich Soil (a basket's contents spill as a container's do)."""
    for name in [RICH_SOIL["block"], COMPOST["block"], *CRATES, *SACKS, *BASKETS]:
        write(out / f"{name}.json", self_drop(name))
    name = RICH_FARMLAND["block"]
    drop = self_drop(RICH_SOIL["block"])
    drop["random_sequence"] = rid(f"blocks/{name}")
    write(out / f"{name}.json", drop)


def tags(tags):
    # Rich soil counts as dirt (saplings, flowers, rice in water over it); its farmland takes crops as farmland does.
    tags.add("block", "minecraft:dirt", rid(RICH_SOIL["block"]))
    for tag in ("minecraft:supports_crops", "minecraft:grows_crops"):
        tags.add("block", tag, rid(RICH_FARMLAND["block"]))
    for name in (RICH_SOIL["block"], RICH_FARMLAND["block"], COMPOST["block"]):
        tags.add("block", "minecraft:mineable/shovel", rid(name))
    for name in (*CRATES, *BASKETS):
        tags.add("block", "minecraft:mineable/axe", rid(name))
