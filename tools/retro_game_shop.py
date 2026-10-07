"""Writes the Retro Game Shop, a plains village house: data/jugcraft/structure/village/plains/retro_game_shop.nbt.

Run from the repository root:  python3 tools/retro_game_shop.py
The output is deterministic (gzip without a timestamp). The shop is an original small storefront: oak and
cobblestone like the plains houses around it, a false front with a lit "Retro Games" sign, display windows, a
chequered floor, shelves of chunky cartridges (chiseled bookshelves), a counter, pixel lamps and the arcade
cabinet. A villager waits inside; the cabinet is the nearest free job site, so in practice he becomes the
Retro Trader (any unemployed villager may claim it first).

Layout (x across, z from the back wall at 0 to the front wall at 6, y up from the floor at 0): the door is in
the middle of the front, and the jigsaw block just outside it (4, 1, 7) joins the shop to a village street the
way vanilla houses do (name minecraft:building_entrance). Positions not listed are left to the terrain.

Format: 26.3's own, as in vanilla 26.3's village templates: palette entries are {"id", "properties"} (older templates
used "Name"/"Properties", which 26.3 reads as air unless the data fixer upgrades them), and DataVersion is 26.3's
(5023), so the data fixer has nothing to do. The game test retroGameShopTemplateLoads checks both against the running
game; regenerate after a platform bump.
"""
import gzip
import io
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src" / "main" / "resources" / "data" / "jugcraft" / "structure" / "village" / "plains" / "retro_game_shop.nbt"
DATA_VERSION = 5023
SIZE = (9, 8, 8)


# ---------------------------------------------------------------- a minimal NBT writer

class Byte(int):
    pass


class Double(float):
    pass


def _name(out, name):
    data = name.encode("utf-8")
    out.write(struct.pack(">H", len(data)) + data)


def _tag_id(value):
    if isinstance(value, Byte) or isinstance(value, bool):
        return 1
    if isinstance(value, int):
        return 3
    if isinstance(value, Double) or isinstance(value, float):
        return 6
    if isinstance(value, str):
        return 8
    if isinstance(value, list):
        return 9
    if isinstance(value, dict):
        return 10
    raise TypeError(value)


def _payload(out, value):
    tag = _tag_id(value)
    if tag == 1:
        out.write(struct.pack(">b", int(value)))
    elif tag == 3:
        out.write(struct.pack(">i", value))
    elif tag == 6:
        out.write(struct.pack(">d", value))
    elif tag == 8:
        _name(out, value)
    elif tag == 9:
        inner = _tag_id(value[0]) if value else 0
        out.write(struct.pack(">bi", inner, len(value)))
        for item in value:
            _payload(out, item)
    else:
        for key in value:
            out.write(struct.pack(">b", _tag_id(value[key])))
            _name(out, key)
            _payload(out, value[key])
        out.write(b"\x00")


def nbt_bytes(root):
    raw = io.BytesIO()
    raw.write(b"\x0a")
    _name(raw, "")
    _payload(raw, root)
    packed = io.BytesIO()
    with gzip.GzipFile(fileobj=packed, mode="wb", mtime=0) as gz:
        gz.write(raw.getvalue())
    return packed.getvalue()


# ---------------------------------------------------------------- the shop

def state(name, **properties):
    return (name, tuple(sorted((key, str(value).lower()) for key, value in properties.items())))


AIR = state("minecraft:air")
PLANKS = state("minecraft:oak_planks")
ROOF = state("minecraft:spruce_planks")
COBBLE = state("minecraft:cobblestone")
GLASS = state("minecraft:glass")
LOG = state("minecraft:stripped_oak_log", axis="y")
LAMP = state("jugcraft:pixel_lamp")
FLOOR_DARK = state("jugcraft:polished_circuitstone")
FLOOR_LIGHT = state("minecraft:smooth_quartz")
COUNTER = state("minecraft:oak_slab", type="top", waterlogged=False)


def shelf(pattern):
    """A chiseled bookshelf facing the shop: its filled slots read as rows of game cartridges."""
    return state("minecraft:chiseled_bookshelf", facing="south",
                 **{f"slot_{i}_occupied": ch == "#" for i, ch in enumerate(pattern)})


def build():
    blocks = {}
    nbt = {}

    def put(x, y, z, block):
        blocks[(x, y, z)] = block

    # Floor and foundation.
    for x in range(9):
        for z in range(7):
            edge = x in (0, 8) or z in (0, 6)
            put(x, 0, z, COBBLE if edge else (FLOOR_DARK if (x + z) % 2 else FLOOR_LIGHT))
    # Clear the inside, then build walls, roof and false front.
    for x in range(1, 8):
        for y in range(1, 5):
            for z in range(1, 6):
                put(x, y, z, AIR)
    for y in range(1, 5):
        for x, z in ((0, 0), (8, 0), (0, 6), (8, 6)):
            put(x, y, z, LOG)
        for x in range(1, 8):
            put(x, y, 0, PLANKS)
        for z in range(1, 6):
            window = y in (2, 3) and z in (2, 3)
            put(0, y, z, GLASS if window else PLANKS)
            put(8, y, z, GLASS if window else PLANKS)
        for x in range(1, 8):
            if x == 4:
                continue
            put(x, y, 6, GLASS if y in (2, 3) else PLANKS)
    put(4, 3, 6, PLANKS)
    put(4, 4, 6, PLANKS)
    put(4, 1, 6, state("minecraft:oak_door", facing="north", half="lower", hinge="left", open=False, powered=False))
    put(4, 2, 6, state("minecraft:oak_door", facing="north", half="upper", hinge="left", open=False, powered=False))
    for x in range(9):
        for z in range(6):
            put(x, 5, z, ROOF)
    put(2, 5, 2, LAMP)
    put(6, 5, 3, LAMP)
    # The false front: two more rows above the roof, with marquee lamps either side of the sign.
    for x in range(9):
        for y in (5, 6):
            put(x, y, 6, PLANKS)
    put(0, 7, 6, state("minecraft:oak_slab", type="bottom", waterlogged=False))
    put(8, 7, 6, state("minecraft:oak_slab", type="bottom", waterlogged=False))
    for x in range(1, 8):
        put(x, 7, 6, PLANKS)
    put(2, 6, 6, LAMP)
    put(6, 6, 6, LAMP)
    put(4, 6, 7, state("minecraft:oak_wall_sign", facing="south", waterlogged=False))
    nbt[(4, 6, 7)] = {
        "id": "minecraft:sign",
        "front_text": {"messages": ["", "Retro", "Games", ""], "color": "orange", "has_glowing_text": Byte(1)},
        "back_text": {"messages": ["", "", "", ""], "color": "black", "has_glowing_text": Byte(0)},
        "is_waxed": Byte(1),
    }

    # Inside: cartridge shelves and barrels on the back wall, a counter, and the arcade cabinet by the window.
    for x, pattern in ((1, "##.#.#"), (2, "#.####"), (3, "###.#.")):
        put(x, 1, 1, shelf(pattern))
        put(x, 2, 1, shelf(pattern[::-1]))
    put(6, 1, 1, state("minecraft:barrel", facing="up", open=False))
    put(7, 1, 1, state("minecraft:barrel", facing="up", open=False))
    put(6, 2, 1, state("minecraft:barrel", facing="south", open=False))
    for x in range(1, 5):
        put(x, 1, 3, COUNTER)
    put(7, 1, 4, state("jugcraft:arcade_cabinet", facing="west", half="lower"))
    put(7, 2, 4, state("jugcraft:arcade_cabinet", facing="west", half="upper"))

    # The doorstep: clear a way in, and the jigsaw that joins the street.
    for x in (3, 4, 5):
        for y in (1, 2):
            put(x, y, 7, AIR)
    put(4, 1, 7, state("minecraft:jigsaw", orientation="south_up"))
    nbt[(4, 1, 7)] = {"id": "minecraft:jigsaw", "name": "minecraft:building_entrance", "target": "minecraft:empty",
                      "pool": "minecraft:empty", "final_state": "minecraft:air", "joint": "rollable",
                      "placement_priority": 0, "selection_priority": 0}

    palette = sorted(set(blocks.values()))
    index = {block: i for i, block in enumerate(palette)}
    block_list = []
    for pos in sorted(blocks):
        entry = {"pos": list(pos), "state": index[blocks[pos]]}
        if pos in nbt:
            entry["nbt"] = nbt[pos]
        block_list.append(entry)
    return {
        "DataVersion": DATA_VERSION,
        "size": list(SIZE),
        "palette": [{"id": name, **({"properties": dict(props)} if props else {})} for name, props in palette],
        "blocks": block_list,
        "entities": [{"pos": [Double(3.5), Double(1.0), Double(2.5)], "blockPos": [3, 1, 2],
                      "nbt": {"id": "minecraft:villager"}}],
    }


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(nbt_bytes(build()))


if __name__ == "__main__":
    main()
