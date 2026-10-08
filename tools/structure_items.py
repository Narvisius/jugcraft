"""Items that show the very thing they place, in 3D and in miniature (Justin, Oct 8 2026: "make it look like it actually
does in game ... so people know what they're actually spawning in").

The nuclear plant's equipment (client/NuclearRenderer.java), the power line structures (client/LineRenderer.java) and
the drones (client/DroneModel.java) are drawn in the world by Java, not by block models, so their items are special
item models (client/StructureItemRenderers.java) that call that same drawing code, scaled so the slot shows them as big
as a block (15/16 of a block's outline), and shown like a block item (the base model's parent is minecraft:block/block).

    items/<id>.json       {"model": {"type": "minecraft:special", "base": "jugcraft:item/<id>",
                                     "model": {"type": "jugcraft:equipment" | "jugcraft:power_line", "kind": <id>}
                                              | {"type": "jugcraft:drone", "tier": n}}}
    models/item/<id>.json {"parent": "minecraft:block/block", "textures": {"particle": ...}}

Shrunk into a slot, a block-sized texture tile is smaller than a pixel and would sparkle, so the equipment and the
power lines are drawn in an item sheet beside their material sheet (textures/entity/<sheet>_item.png): every region one
flat colour, its average. The drones keep their own sheet (a drone is a few blocks across).
"""
from pathlib import Path

import numpy as np
from PIL import Image

MOD = "jugcraft"
# The material sheets drawn by Java with 64-pixel regions, and their flat item versions (8 pixels a region).
ITEM_SHEETS = {"nuclear_equipment": 64, "power_lines": 64}
ITEM_REGION = 8


def special_item(write, rid, assets, item, model, particle="minecraft:block/iron_block"):
    """Writes an item that shows a special model: the definition and its base model (display transforms, particle)."""
    write(assets / "models" / "item" / f"{item}.json", {"parent": "minecraft:block/block", "textures": {"particle": particle}})
    write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:special", "base": rid(f"item/{item}"), "model": model}})


def equipment_item(write, rid, assets, kind):
    special_item(write, rid, assets, kind, {"type": f"{MOD}:equipment", "kind": kind})


def line_item(write, rid, assets, kind, particle):
    special_item(write, rid, assets, kind, {"type": f"{MOD}:power_line", "kind": kind}, particle)


def drone_item(write, rid, assets, item, tier):
    special_item(write, rid, assets, item, {"type": f"{MOD}:drone", "tier": tier})


def item_sheet(sheet, region, out_region=ITEM_REGION):
    """The sheet with every region one flat colour: its average over opaque pixels. A region mostly see-through
    (grating, handrails, chain-link and barbed wire) is left out altogether, so the miniature shows what is behind it
    rather than a solid wall."""
    a = np.asarray(sheet.convert("RGBA")).astype(np.float64)
    rows, cols = a.shape[0] // region, a.shape[1] // region
    out = np.zeros((rows * out_region, cols * out_region, 4), np.uint8)
    for r in range(rows):
        for c in range(cols):
            cell = a[r * region:(r + 1) * region, c * region:(c + 1) * region]
            alpha = cell[..., 3] > 127
            if alpha.mean() < 0.5:
                continue
            colour = cell[alpha][:, :3].mean(axis=0)
            out[r * out_region:(r + 1) * out_region, c * out_region:(c + 1) * out_region, :3] = np.round(colour)
            out[r * out_region:(r + 1) * out_region, c * out_region:(c + 1) * out_region, 3] = 255
    return Image.fromarray(out, "RGBA")


def write_item_sheets(entity_dir):
    entity_dir = Path(entity_dir)
    for name, region in ITEM_SHEETS.items():
        item_sheet(Image.open(entity_dir / f"{name}.png"), region).save(entity_dir / f"{name}_item.png", optimize=True)
