"""Optional Blender architectural previews of the generated structure, without game assets.

blender --background --python tools/preview_winterhold_academy.py -- --output-dir docs/images

Flat colours and simplified block shapes are review aids, never Minecraft screenshots.
The generator remains the sole source of geometry; no .blend file is required.
"""
import argparse
import sys
from pathlib import Path

import bpy
from mathutils import Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
from winterhold_academy import FLOOR, build

COLOURS = {
    "stone_bricks": (.38, .42, .47), "cracked_stone_bricks": (.29, .33, .38),
    "chiseled_stone_bricks": (.49, .53, .58), "polished_andesite": (.52, .56, .60),
    "andesite": (.28, .31, .34), "tuff": (.23, .28, .28),
    "stone_brick_slab": (.43, .47, .52), "stone_brick_stairs": (.43, .47, .52),
    "stone_brick_wall": (.43, .47, .52), "snow": (.86, .92, .97),
    "spruce_planks": (.25, .18, .12), "spruce_stairs": (.32, .23, .16),
    "spruce_slab": (.34, .25, .17), "spruce_door": (.38, .26, .15),
    "spruce_fence": (.31, .22, .14), "light_blue_stained_glass": (.16, .40, .58),
    "blue_terracotta": (.17, .24, .40), "bookshelf": (.43, .30, .19),
    "blue_bed": (.14, .26, .52), "barrel": (.34, .23, .13),
    "amethyst_block": (.45, .27, .68), "amethyst_cluster": (.65, .43, .90),
    "lantern": (.96, .59, .18), "lampwright_bench": (.57, .35, .13),
    "crucible": (.17, .23, .29), "artificer_bench": (.43, .29, .20),
    "conclave_lectern": (.50, .37, .15), "enchanting_table": (.29, .13, .34),
    "cobbled_deepslate": (.19, .22, .26), "deepslate_tiles": (.14, .17, .21),
    "chiseled_deepslate": (.24, .27, .31), "sea_lantern": (.47, .82, .94),
    "moss_block": (.24, .39, .15), "fern": (.22, .44, .17),
    "poppy": (.65, .18, .12), "dandelion": (.76, .62, .14),
}
FACES = [((1, 0, 0), [(1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)]),
         ((-1, 0, 0), [(0, 0, 1), (0, 1, 1), (0, 1, 0), (0, 0, 0)]),
         ((0, 1, 0), [(0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)]),
         ((0, -1, 0), [(0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)]),
         ((0, 0, 1), [(1, 0, 1), (1, 1, 1), (0, 1, 1), (0, 0, 1)]),
         ((0, 0, -1), [(0, 0, 0), (0, 1, 0), (1, 1, 0), (1, 0, 0)])]


def scene_for(template, keep):
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    palette = template["palette"]
    cells = {tuple(b["pos"]): b["state"] for b in template["blocks"]
             if palette[b["state"]]["id"] != "minecraft:air" and keep(*b["pos"])}
    groups = {}
    for (x, y, z), i in cells.items():
        name = palette[i]["id"].split(":")[-1]
        props = palette[i].get("properties", {})
        low, high = 0.0, 1.0
        if name == "snow":
            high = int(props.get("layers", "1"))/8
        elif name.endswith("_slab"):
            low, high = ((.5, 1) if props.get("type") == "top" else (0, .5))
        elif name.endswith("_bed"):
            high = .5625
        vertices, polygons = groups.setdefault(name, ([], []))
        for (dx, dy, dz), corners in FACES:
            neighbour = cells.get((x+dx, y+dy, z+dz))
            neighbour_name = palette[neighbour]["id"] if neighbour is not None else ""
            if neighbour is not None and not neighbour_name.endswith(("snow", "_slab", "_bed")):
                continue
            first = len(vertices)
            vertices.extend((x+a, -z-c, y+low+b*(high-low)) for a, b, c in corners)
            polygons.append(tuple(range(first, first+4)))
    for name, (vertices, polygons) in groups.items():
        mesh = bpy.data.meshes.new(name)
        mesh.from_pydata(vertices, [], polygons)
        mesh.update()
        obj = bpy.data.objects.new(name, mesh)
        bpy.context.collection.objects.link(obj)
        mat = bpy.data.materials.new(name)
        mat.diffuse_color = (*COLOURS.get(name, (.35, .30, .25)), 1)
        mat.use_nodes = True
        node = mat.node_tree.nodes.get("Principled BSDF")
        node.inputs["Base Color"].default_value = mat.diffuse_color
        node.inputs["Roughness"].default_value = .9
        if name in {"lantern", "sea_lantern"}:
            node.inputs["Emission Color"].default_value = mat.diffuse_color
            node.inputs["Emission Strength"].default_value = 1.8
        mesh.materials.append(mat)
    bpy.ops.object.light_add(type="SUN", location=(0, -50, 120))
    bpy.context.object.rotation_euler = (.4, -.4, -.4)
    bpy.context.object.data.energy = 2.2
    bpy.ops.object.light_add(type="AREA", location=(110, -65, 95))
    bpy.context.object.rotation_euler = (Vector((68, -70, 40))-bpy.context.object.location).to_track_quat("-Z", "Y").to_euler()
    bpy.context.object.data.energy = 5000
    bpy.context.object.data.shape = "DISK"
    bpy.context.object.data.size = 60
    bpy.ops.object.camera_add(location=(195, -231, 177))
    camera = bpy.context.object
    camera.rotation_euler = (Vector((68, -77, 34))-camera.location).to_track_quat("-Z", "Y").to_euler()
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 195
    scene = bpy.context.scene
    scene.camera = camera
    scene.render.engine = "CYCLES"
    scene.cycles.samples = 16
    scene.world.color = (.19, .23, .30)
    scene.render.resolution_x, scene.render.resolution_y = 1440, 1080
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "JPEG"
    scene.render.image_settings.quality = 87
    scene.view_settings.view_transform = "Standard"
    return scene


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args(sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else [])
    args.output_dir.mkdir(parents=True, exist_ok=True)
    template = build()
    for name, keep in (("overview", lambda x, y, z: True),
                       ("cutaway", lambda x, y, z: x < 68 or y < FLOOR-2)):
        scene = scene_for(template, keep)
        scene.render.filepath = str((args.output_dir / f"frostward-academy-{name}.jpg").resolve())
        bpy.ops.render.render(write_still=True)


if __name__ == "__main__":
    main()
