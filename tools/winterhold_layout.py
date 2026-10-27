"""Apply the reviewed circular-campus layout to the recovered academy components.

The JSON operations are the single layout specification: copies retain authored
interiors, then explicit openings connect them. Coordinates here are template
coordinates, including the rocky foundation.
"""
import json

from retro_game_shop import state

LAYOUT_NAME = "winterhold_layout.json"


def load_layout(path):
    layout = json.loads(path.read_text(encoding="utf-8"))
    if layout["schema"] != 1:
        raise ValueError("Unsupported academy layout schema")
    return layout


def apply_layout(legacy, layout):
    cells, metadata = {}, {}
    size = tuple(layout["size"])

    def put(x, y, z, block, nbt=None):
        assert all(0 <= c < n for c, n in zip((x, y, z), size)), (x, y, z)
        cells[x, y, z] = block
        metadata.pop((x, y, z), None)
        if nbt:
            metadata[x, y, z] = nbt

    def distance(x, z, cx, cz):
        return (x-cx)**2 + (z-cz)**2

    def inside(x, z, cx, cz, radius):
        return distance(x, z, cx, cz) <= radius**2

    def edge(x, z, cx, cz, radius):
        return inside(x, z, cx, cz, radius) and any(
            not inside(x+dx, z+dz, cx, cz, radius)
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))

    def from_entry(entry):
        return state(entry["id"], **entry.get("properties", {}))

    palette = [from_entry(entry) for entry in legacy["palette"]]
    for op in layout["ops"]:
        if op["op"] == "foundation":
            for x in range(size[0]):
                for z in range(size[2]):
                    for y in range(25):
                        inset = (23-y)//3 if y < 16 else max(0, (24-y)//2)
                        if not any(inside(x, z, cx, cz, radius-inset)
                                   for cx, cz, radius in op["circles"]):
                            continue
                        if y == 24:
                            material = "polished_andesite"
                        elif y < 16:
                            material = "tuff" if (x+z+3*(y-16)) % 11 < 3 else "andesite"
                        else:
                            material = "cracked_stone_bricks" if (13*x+7*(y-16)+z) % 19 == 0 else "stone_bricks"
                        put(x, y, z, state("minecraft:" + material))
        elif op["op"] == "copy":
            x0, y0, z0, x1, y1, z1 = op["bounds"]
            dx, dy, dz = op["shift"]
            for entry in legacy["blocks"]:
                x, y, z = entry["pos"]
                if not (x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1):
                    continue
                if "disk" in op and not inside(x, z, *op["disk"]):
                    continue
                block, nbt = palette[entry["state"]], entry.get("nbt")
                if op.get("clear_library") and 39 <= y <= 49:
                    stair = (33 <= x <= 37 or 61 <= x <= 65) and 20 <= z <= 30
                    furnishing = block[0].split(":")[1] in {
                        "bookshelf", "spruce_slab", "spruce_fence", "spruce_door",
                        "conclave_lectern", "spruce_wall_sign"}
                    if (furnishing or block[0] == "minecraft:spruce_stairs" and not stair
                            or block[0] == "minecraft:spruce_planks" and not stair and y > 39 and 32 < x < 64):
                        block, nbt = state("minecraft:air"), None
                put(x+dx, y+dy, z+dz, block, nbt)
        elif op["op"] == "snow":
            for (x, y, z), block in list(cells.items()):
                if block[0] not in {"minecraft:stone_bricks", "minecraft:chiseled_stone_bricks",
                                    "minecraft:polished_andesite"}:
                    continue
                if y < 24 or y >= 72 or (x, y+1, z) in cells:
                    continue
                if y in (24, 32) or (x*3+z*5) % 7 >= 3:
                    continue
                put(x, y+1, z, state("minecraft:snow", layers=1))
        else:
            if "xz" in op:
                x0, z0, x1, z1 = op["xz"]
            else:
                cx, cz = op["center"]
                radius = op["radius"]
                x0, z0, x1, z1 = cx-radius, cz-radius, cx+radius, cz+radius
            block = from_entry(op["state"])
            for x in range(x0, x1+1):
                for z in range(z0, z1+1):
                    if op["op"] == "edge":
                        use = edge(x, z, *op["center"], op["radius"])
                    else:
                        use = (op["shape"] == "box" or
                               inside(x, z, *op["center"], op["radius"]) and
                               (op["shape"] != "ring" or distance(x, z, *op["center"]) > op["inner"]**2))
                    if use:
                        for y in range(op["y"][0], op["y"][1]+1):
                            put(x, y, z, block)

    from academy_interiors import furnish
    furnish(cells, put)
    from academy_architecture import (furnish_elements_hall, connect_academic_cluster, build_arcanaeum_reading_wing,
                                      build_gatehouse, open_cloister_stairs, build_archmage_annex)
    furnish_elements_hall(cells, put)
    connect_academic_cluster(cells, put, layout["academic_wing"])
    build_arcanaeum_reading_wing(cells, put, layout["library_wing"])
    build_gatehouse(cells, put, layout["gatehouse"])
    open_cloister_stairs(cells, put)
    build_archmage_annex(cells, put, layout["archmage_annex"])
    from academy_architecture import add_autumn
    add_autumn(cells, put)
    from academy_fonta59 import (enrich_hall_and_campus, furnish_academic_crosswing,
                                 open_elements_gallery, furnish_reading_wing_from_donor,
                                 plant_autumn_court, enrich_court_arcade,
                                 distinguish_residence_halls)
    enrich_hall_and_campus(cells, put)
    furnish_academic_crosswing(cells, put)
    open_elements_gallery(cells, put)
    furnish_reading_wing_from_donor(cells, put)
    plant_autumn_court(cells, put)
    enrich_court_arcade(cells, put)
    distinguish_residence_halls(cells, put)
    from academy_craft import craft_elements_hall, craft_arcanaeum, landscape_autumn_court, craft_bridge_gate
    craft_elements_hall(cells, put)
    craft_arcanaeum(cells, put)
    landscape_autumn_court(cells, put)
    craft_bridge_gate(put)
    from academy_monument import craft_monumental_elements
    craft_monumental_elements(cells, put)
    from academy_flagship import craft_flagship_rooms
    craft_flagship_rooms(cells, put)
    from academy_hall_masterwork import craft_hall_masterwork
    craft_hall_masterwork(cells, put)
    final_palette = sorted(set(cells.values()))
    indices = {block: i for i, block in enumerate(final_palette)}
    blocks = []
    for pos, block in sorted(cells.items()):
        entry = {"pos": list(pos), "state": indices[block]}
        if pos in metadata:
            entry["nbt"] = metadata[pos]
        blocks.append(entry)
    return {
        "DataVersion": legacy["DataVersion"], "size": list(size),
        "palette": [{"id": name, **({"properties": dict(props)} if props else {})}
                    for name, props in final_palette],
        "blocks": blocks, "entities": [],
    }
