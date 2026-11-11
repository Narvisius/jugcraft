"""Render actual exported tower block models/textures, with culling and a z-buffer.

This is an offline orthographic asset preview, not a Minecraft screenshot. It
resolves blockstates, composed parent models, element rotations, UVs and PNGs.
Ground pad formation is applied exactly as the depot applies it in game. Drone
models are deliberately absent. Dynamic live holograms/doors require the client.
"""
from __future__ import annotations

import argparse
import functools
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

import armor_preview as raster
import art_check
import tower_industrial as tower

ASSETS = tower.RESOURCES / "assets/jugcraft"
OUT = tower.ROOT / "build/drone_tower"
NORMALS = art_check._NORMAL


@functools.lru_cache(maxsize=None)
def texture(path):
    if path is None:
        # The only foreign namespace is the original stone foundation. This
        # neutral stone material is explicit in metadata, not a copied asset.
        return np.full((16, 16, 4), (128, 130, 132, 255), dtype=np.uint8)
    image = Image.open(path).convert("RGBA")
    if Path(str(path) + ".mcmeta").exists() and image.height > image.width:
        image = image.crop((0, 0, image.width, image.width))
    return np.array(image)


def builtin(model, textures):
    """Vanilla cube templates contain no mod geometry; instantiate their faces."""
    parent = model.get("parent", "")
    face_refs = {face: "#all" for face in NORMALS}
    if parent.endswith(("cube_bottom_top", "cube_column")):
        face_refs = {face: "#side" for face in NORMALS}
        face_refs["up"] = "#top" if "top" in textures else "#end"
        face_refs["down"] = "#bottom" if "bottom" in textures else "#end"
    elif parent.endswith("cube"):
        face_refs = {face: "#" + face for face in NORMALS}
    faces = {face: {"texture": ref} for face, ref in face_refs.items()}
    return [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]


def properties(block_state):
    name, _, prop = block_state.partition("[")
    props = {k: v for k, v in (p.split("=", 1) for p in prop.rstrip("]").split(",") if "=" in p)}
    return name.split(":", 1)[-1], props


@functools.lru_cache(maxsize=None)
def local_faces(block_state):
    name, props = properties(block_state)
    if block_state.startswith("minecraft:"):
        faces, _ = art_check._model_faces(builtin({}, {}), lambda _: None)
        return tuple((np.array(f.pts) / 16, np.array(f.uvs), np.array(f.normal), texture(None)) for f in faces), True
    state_path = ASSETS / "blockstates" / f"{name}.json"
    states = json.loads(state_path.read_text(encoding="utf-8"))
    defaults = {"facing": "north", "axis": "y", "part": "0", "half": "bottom", "shape": "straight", "type": "bottom"}
    defaults.update(props)
    choices = []
    for key, value in states.get("variants", {}).items():
        required = {k: v for k, v in (p.split("=", 1) for p in key.split(",") if "=" in p)}
        if all(defaults.get(k) == v for k, v in required.items()):
            choices.append(value[0] if isinstance(value, list) else value)
    if not choices:
        raise ValueError(f"No exported block model matches {block_state}")
    variant = choices[0]
    model_path = ASSETS / "models" / (variant["model"].split(":", 1)[-1] + ".json")
    model = json.loads(model_path.read_text(encoding="utf-8"))
    elements, lookup, textures = art_check._resolve(model, (ASSETS,))
    if elements is None:
        elements = builtin(model, textures)
    faces, _ = art_check._model_faces(elements, lookup)
    yaw, pitch = -variant.get("y", 0), -variant.get("x", 0)
    turn_y, norm_y = art_check._turner({"axis": "y", "angle": yaw, "origin": (8, 8, 8)})
    turn_x, norm_x = art_check._turner({"axis": "x", "angle": pitch, "origin": (8, 8, 8)})
    out = []
    for face in faces:
        points = np.array([turn_y(turn_x(p)) for p in face.pts]) / 16
        normal = np.array(norm_y(norm_x(face.normal)))
        out.append((points, np.array(face.uvs), normal, texture(face.tex)))
    # Only an actual opaque complete cube can cull a neighbouring block face.
    full = len(elements) == 1 and not elements[0].get("rotation") and elements[0]["from"] == [0, 0, 0] and elements[0]["to"] == [16, 16, 16]
    full = full and all(np.min(f[3][..., 3]) == 255 for f in out)
    return tuple(out), full


def formed_blocks(blocks):
    blocks = dict(blocks)
    for pad in tower.ground_pads():
        for z in range(5):
            for x in range(5):
                p = (tower.C + pad["x"] + x, 1, tower.C + pad["z"] + z)
                if tower.base(blocks.get(p, "")) == "landing_pad":
                    blocks[p] = f"landing_pad[part={1 + z * 5 + x}]"
    # All pickups use the same runtime 3x3 part ordering.
    starts = {(29, 1, 29)}
    for tier in tower.legacy_data()["tiers"]:
        starts.update((tower.C + p["x"], p["y"], tower.C + p["z"]) for p in tier["pickups"])
    for px, py, pz in starts:
        for z in range(3):
            for x in range(3):
                p = (px + x, py, pz + z)
                if tower.base(blocks.get(p, "")) == "supply_pickup":
                    blocks[p] = f"supply_pickup[part={1 + z * 3 + x}]"
    return blocks


def scene(blocks, view):
    camera = raster.camera(view)[0]
    resolved = {p: tower.state(m, (p[0] - tower.C, p[1], p[2] - tower.C))
                for p, m in formed_blocks(blocks).items()}
    full = {p for p, m in resolved.items() if local_faces(m)[1]}
    quads = []
    for pos, block_state in resolved.items():
        offset = np.array(pos)
        for points, uv, normal, tex in local_faces(block_state)[0]:
            if normal @ camera <= 0:
                continue
            axis = int(np.argmax(np.abs(normal)))
            sign = 1 if normal[axis] > 0 else -1
            # Cull only a face exactly on a block boundary and behind a full
            # opaque neighbour. Recessed girders/vents retain their inner faces.
            plane = 1 if sign > 0 else 0
            if abs(normal[axis]) > .999 and np.max(np.abs(points[:, axis] - plane)) < 1e-6:
                neighbour = list(pos)
                neighbour[axis] += sign
                if tuple(neighbour) in full:
                    continue
            quads.append((points + offset, uv, normal, tex))
    return quads


def render(blocks, view, scale, output, title):
    quads = scene(blocks, view)
    print(f"{output.name}: {len(blocks)} blocks / {len(quads)} visible exported faces", flush=True)
    picture = raster.render(quads, view, scale=scale, background=(28, 34, 39), lighting="blockbench")
    picture = raster.label(picture, title, 28)
    picture.save(output)
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--only", choices=("overview", "ground", "hangars", "control", "underdeck"))
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    if args.only in (None, "overview"):
        render(tower.build(9), (145, 16), 11, OUT / "industrial_tower_overview.png",
               "INDUSTRIAL DRONE TOWER / 215 blocks / actual exported geometry and textures")
    if args.only in (None, "ground"):
        render(tower.build(1), (140, 27), 22, OUT / "industrial_tower_ground.png",
               "TIER 1 / service apron, formed exchanges, realistic joined pads and command post")
    if args.only in (None, "hangars"):
        blocks = {p: m for p, m in tower.build(9).items() if 16 <= p[1] <= 60 and abs(p[0] - tower.C) <= 15 and abs(p[2] - tower.C) <= 15}
        render(blocks, (140, 14), 30, OUT / "industrial_tower_hangars.png",
               "HANGAR FACADE SECTION / door housings, maintenance cages, plant bays, structural spines")
    if args.only in (None, "control"):
        # Deliberate architectural cutaway, not hidden geometry passed off as a
        # game screenshot: expose actual console/table assets and walkable aisles.
        blocks = {p: m for p, m in tower.build(1).items()
                  if abs(p[0] - tower.C) <= tower.ROOM_X + 2 and abs(p[2] - tower.C) <= tower.ROOM_Z + 2 and p[1] <= 6
                  and not (p[2] >= tower.C + tower.ROOM_Z + 1 and p[1] > 0)}
        render(blocks, (145, 30), 50, OUT / "industrial_tower_control_cutaway.png",
               "OPERATIONS ROOM CUTAWAY / actual console, 3 x 3 projection table, equipment and aisles")
    if args.only in (None, "underdeck"):
        blocks = {p: m for p, m in tower.build(9).items()
                  if p[1] <= 12 and abs(p[0] - tower.C) <= 20 and abs(p[2] - tower.C) <= 20}
        render(blocks, (160, -12), 35, OUT / "industrial_tower_underdeck.png",
               "DECK SOFFIT / actual sealed flush luminaires, clear pad volume and continuous supports")
    metadata = {"kind": "offline exported runtime model preview", "renderer": "armor_preview z-buffer",
                "foreign_material": "neutral foundation stone placeholder; vanilla artwork not copied",
                "dynamic_layers": "live hologram and client sliding doors are rendered only in Minecraft",
                "local_model_states": local_faces.cache_info().currsize}
    (OUT / "industrial_preview.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
