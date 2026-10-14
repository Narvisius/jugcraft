"""Blockbench projects (.bbmodel) of the generators' box models, for review and editing in Blockbench.

A project is written in Blockbench's free format: every box in model space with the game's own UVs (vanilla's
position mapping, kept inside the sprite as model_writer.fit_uv does) and the textures embedded, so the file opens
on its own. Boxes are grouped as the caller groups them (a multi-block prop as one group; a vehicle as one group a
moving part, each with its joint as the group's origin and its rest pose as the group's rotation).

The Python model stays the source: a changed project is not read back, so a change made in Blockbench is carried
into the generator to keep the models reproducible (docs/ART_DIRECTION.md).
"""
import base64
import io
import json
import uuid
from pathlib import Path

import model_writer

MOD = "jugcraft"
ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / MOD / "textures" / "block"


def _uuid(*parts):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "jugcraft:blockbench:" + ":".join(str(p) for p in parts)))


def _texture_entry(name, index, draw=None):
    """A texture embedded as a data URI: the committed PNG, or `draw(name)` (a PIL image) when it is not written yet."""
    path = TEXTURES / f"{name}.png"
    if path.exists():
        data = path.read_bytes()
    else:
        buffer = io.BytesIO()
        draw(name).save(buffer, format="PNG")
        data = buffer.getvalue()
    return {"path": "", "name": f"{name}.png", "folder": "block", "namespace": MOD, "id": str(index), "particle": index == 0,
            "render_mode": "default", "visible": True, "mode": "bitmap", "saved": False, "uuid": _uuid("texture", name),
            "source": "data:image/png;base64," + base64.b64encode(data).decode("ascii")}


def cube(name, item, index, texture_index):
    """One Blockbench cube from a generator element (from, to, texture[, options])."""
    frm, to, texture, options = model_writer.unpack(item)
    rotation = options.get("rotation")
    spec = model_writer.element(frm, to, texture, rotation=rotation)
    faces = {}
    for face, entry in spec["faces"].items():
        tex = entry["texture"].lstrip("#")
        uv = entry.get("uv") or model_writer.fit_uv(model_writer.auto_uv(face, spec["from"], spec["to"]))
        faces[face] = {"uv": [round(v, 4) for v in uv], "texture": texture_index[tex]}
    out = {"name": f"{name}_{index}", "box_uv": False, "rescale": bool(rotation and len(rotation) > 3 and rotation[3]),
           "locked": False, "render_order": "default", "allow_mirror_modeling": True,
           "from": spec["from"], "to": spec["to"], "autouv": 0, "color": index % 8,
           "origin": [round(v, 4) for v in rotation[2]] if rotation else [0, 0, 0],
           "faces": faces, "type": "cube", "uuid": _uuid(name, index)}
    if rotation:
        turn = [0, 0, 0]
        turn["xyz".index(rotation[0])] = rotation[1]
        out["rotation"] = turn
    return out


def _keyframe(channel, time, values, index):
    # Blockbench's animation rotations about x and y run the other way from Minecraft's (it saves them negated), so
    # a rotation keyframe is written negated on those axes to turn the part the way the game would.
    if channel == "rotation":
        values = (-values[0], -values[1], values[2])
    return {"channel": channel, "data_points": [{"x": str(round(values[0], 3)), "y": str(round(values[1], 3)),
                                                  "z": str(round(values[2], 3))}],
            "uuid": _uuid("keyframe", channel, time, index), "time": round(time, 4), "color": -1,
            "interpolation": "catmullrom", "bezier_linked": True, "bezier_left_time": [-0.1, -0.1, -0.1],
            "bezier_left_value": [0, 0, 0], "bezier_right_time": [0.1, 0.1, 0.1], "bezier_right_value": [0, 0, 0]}


def animation(name, length, tracks, loop=True):
    """An animation: `tracks` maps a group name to {channel: [(time, (x, y, z)), ...]} (rotation in degrees, position in
    pixels, scale as factors), sampled keyframes that Blockbench smooths (catmull-rom)."""
    return {"name": name, "length": length, "loop": loop, "tracks": tracks}


def project(name, groups, draw=None, animations=()):
    """A project of `groups`: (group name, origin (pixels), rotation (degrees about x, y, z) or None, elements, and
    optionally the parent group's name, for a part that moves with another), with `animations` from animation()."""
    groups = [tuple(g) + (None,) * (5 - len(g)) for g in groups]
    names = sorted({t for _, _, _, elements, _ in groups for t in model_writer.texture_names(elements)})
    texture_index = {t: i for i, t in enumerate(names)}
    cubes, nodes = [], {}
    for group, origin, rotation, elements, _parent in groups:
        children = []
        for index, item in enumerate(elements):
            box = cube(group, item, index, texture_index)
            cubes.append(box)
            children.append(box["uuid"])
        nodes[group] = {"name": group, "origin": [round(v, 4) for v in origin], "rotation": list(rotation or (0, 0, 0)),
                        "color": 0, "uuid": _uuid("group", name, group), "export": True, "mirror_uv": False,
                        "isOpen": True, "locked": False, "visibility": True, "autouv": 0, "children": children}
    outliner = []
    for group, _, _, _, parent in groups:
        if parent:
            nodes[parent]["children"].append(nodes[group])
        else:
            outliner.append(nodes[group])
    group_ids = {group: _uuid("group", name, group) for group, _, _, _, _ in groups}
    clips = []
    for clip in animations:
        animators = {}
        for group, channels in clip["tracks"].items():
            keyframes = []
            for channel, samples in channels.items():
                keyframes += [_keyframe(channel, time, values, i) for i, (time, values) in enumerate(samples)]
            animators[group_ids[group]] = {"name": group, "type": "bone", "keyframes": keyframes}
        clips.append({"uuid": _uuid("animation", name, clip["name"]), "name": clip["name"],
                      "loop": "loop" if clip["loop"] else "once", "override": False, "length": clip["length"],
                      "snapping": 24, "selected": False, "anim_time_update": "", "blend_weight": "", "start_delay": "",
                      "loop_delay": "", "animators": animators})
    return {"meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
            "name": name, "model_identifier": "", "visible_box": [1, 1, 0],
            "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [],
            "unhandled_root_fields": {}, "resolution": {"width": 16, "height": 16},
            "elements": cubes, "outliner": outliner,
            "textures": [_texture_entry(t, i, draw) for i, t in enumerate(names)],
            "animations": clips}


def write(path, name, groups, draw=None, animations=()):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(project(name, groups, draw, animations), separators=(",", ":")) + "\n", encoding="utf-8")
    return path
