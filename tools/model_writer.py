"""Writes machine block models, blockstates and item models in one visual style.

Two styles exist. Both are visual only: they never change a block, its footprint or how it works.
- "steampunk" (tools/steampunk_models.py): detailed brass, copper and iron models for every machine.
- "classic" (tools/large_machines.py plus textured cubes): the original look.

generate_material_data.py writes DEFAULT_STYLE into the mod's own assets and the other style into
the built-in resource pack resourcepacks/alternate_machines, which players can turn on in
Options > Resource Packs. To make the other style the default, change DEFAULT_STYLE and regenerate.

Model elements are tuples (from, to, texture) or (from, to, texture, options):
- from/to are pixels in structure space (the master block is 0..16 on every axis, facing north).
- texture is a texture name, or a dict {face: name} with "*" as the default. A name ending in "!"
  stretches the whole 16x16 texture over that face (gauges, doors, windows); otherwise the texture
  is mapped by position, like vanilla's automatic UVs.
- options may hold "rotation": (axis, angle, origin) or (axis, angle, origin, rescale); rescale stretches a 45-degree
  element to span the block's diagonal (conveyor slopes). Rotated elements are never sliced between
  blocks, so keep each one inside a single block's reach.
"""
import json
from pathlib import Path

MOD = "jugcraft"
DEFAULT_STYLE = "steampunk"
STYLES = ("steampunk", "classic")
PACK_ID = "alternate_machines"
STYLE_NAMES = {"steampunk": "Steampunk Machines", "classic": "Classic Machines"}

FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
FACES = ("north", "south", "east", "west", "up", "down")
# For each face: the two axes spanning it (u, v) and the axis it faces along with its side.
FACE_AXES = {"north": (0, 1, 2, 0), "south": (0, 1, 2, 1), "east": (2, 1, 0, 1), "west": (2, 1, 0, 0),
             "up": (0, 2, 1, 1), "down": (0, 2, 1, 0)}
# LargeMachineBlock.PART is 0..3.
PART_STATES = 64


def alternate_style():
    return next(style for style in STYLES if style != DEFAULT_STYLE)


def rid(path):
    return f"{MOD}:{path}"


def write(path, obj):
    if isinstance(obj, dict) and obj.get("elements"):
        separate_coplanar(obj["elements"])
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


# Faces as (axis, sign) of the direction they face.
_FACE_DIR = {"north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1), "down": (1, -1), "up": (1, 1)}
# How far a face is pushed out to stop it sharing a plane with another (pixels): invisible, but enough for the
# depth buffer.
COPLANAR_NUDGE = 0.02


def _face_look(element, face):
    entry = element["faces"][face]
    return entry.get("texture"), tuple(entry.get("uv", ()))


def separate_coplanar(elements):
    """Stops z-fighting: where two elements have faces on the same plane, facing the same way, overlapping and
    drawn differently, the GPU flickers between them (most often a band, dial or trim laid flush on a body). The
    smaller face (the detail) is pushed out by COPLANAR_NUDGE so it always draws in front. Rotated elements are left
    alone; faces that look the same (same texture, automatic UVs) do not flicker and are left too."""
    plain = [e for e in elements if "rotation" not in e and e.get("faces")]
    for _ in range(3):  # A nudge can rarely line a face up with a third element; a few passes settle it.
        moved = False
        planes = {}
        for e in plain:
            for face in e["faces"]:
                axis, sign = _FACE_DIR[face]
                plane = round(e["to"][axis] if sign > 0 else e["from"][axis], 4)
                planes.setdefault((face, plane), []).append(e)
        for (face, _plane), group in planes.items():
            if len(group) < 2:
                continue
            axis, sign = _FACE_DIR[face]
            others = [i for i in range(3) if i != axis]
            for i, first in enumerate(group):
                for second in group[i + 1:]:
                    area = 1.0
                    for j in others:
                        overlap = min(first["to"][j], second["to"][j]) - max(first["from"][j], second["from"][j])
                        area *= max(0.0, overlap)
                    if area <= 1e-6:
                        continue
                    if _face_look(first, face) == _face_look(second, face) and not _face_look(first, face)[1]:
                        continue
                    def size(e):
                        return (e["to"][others[0]] - e["from"][others[0]]) * (e["to"][others[1]] - e["from"][others[1]])
                    detail = second if size(second) <= size(first) else first
                    if sign > 0:
                        detail["to"][axis] = round(detail["to"][axis] + COPLANAR_NUDGE, 4)
                    else:
                        detail["from"][axis] = round(detail["from"][axis] - COPLANAR_NUDGE, 4)
                    moved = True
        if not moved:
            break


def unpack(element):
    frm, to, texture = element[:3]
    options = element[3] if len(element) > 3 else {}
    return frm, to, texture, options


def _face_texture(texture, face):
    tex = texture.get(face, texture.get("*")) if isinstance(texture, dict) else texture
    return tex


def element(frm, to, texture, uv=False, skip=(), rotation=None):
    """One model element; uv=True gives explicit UVs for elements outside 0..16 or scaled ones."""
    faces = {}
    for face in FACES:
        if face in skip:
            continue
        tex = _face_texture(texture, face)
        if tex is None:
            continue
        stretch = tex.endswith("!")
        tex = tex.rstrip("!")
        ref = tex if tex.startswith("#") else f"#{tex}"
        entry = {"texture": ref}
        if stretch:
            entry["uv"] = [0, 0, 16, 16]
        elif uv:
            u_axis, v_axis = FACE_AXES[face][:2]
            width = min(16, abs(to[u_axis] - frm[u_axis]))
            height = min(16, abs(to[v_axis] - frm[v_axis]))
            entry["uv"] = [0, 0, round(width, 3), round(height, 3)]
        faces[face] = entry
    out = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}
    if rotation:
        axis, angle, origin = rotation[:3]
        out["rotation"] = {"origin": [round(v, 4) for v in origin], "axis": axis, "angle": angle}
        if len(rotation) > 3 and rotation[3]:
            out["rotation"]["rescale"] = True
    return out


def texture_names(elements):
    names = set()
    for item in elements:
        texture = unpack(item)[2]
        values = texture.values() if isinstance(texture, dict) else [texture]
        names |= {value.rstrip("!") for value in values if value is not None}
    return sorted(name for name in names if not name.startswith("#"))


def slice_model(name, elements, footprint):
    """Cuts one structure-space model into per-part models, like Immersive Engineering's split models."""
    parts = [[] for _ in footprint]
    for item in elements:
        frm, to, texture, options = unpack(item)
        rotation = options.get("rotation")
        volume = (to[0] - frm[0]) * (to[1] - frm[1]) * (to[2] - frm[2])
        pieces, covered = [], 0.0
        if not rotation:
            for index, offset in enumerate(footprint):
                low = [offset[axis] * 16 for axis in range(3)]
                a = [max(frm[axis], low[axis]) for axis in range(3)]
                b = [min(to[axis], low[axis] + 16) for axis in range(3)]
                if all(a[axis] < b[axis] for axis in range(3)):
                    covered += (b[0] - a[0]) * (b[1] - a[1]) * (b[2] - a[2])
                    # Drop faces created by the cut: they are inside the element.
                    skip = []
                    for face, (_, _, axis, side) in FACE_AXES.items():
                        edge = b[axis] if side else a[axis]
                        original = to[axis] if side else frm[axis]
                        if edge != original:
                            skip.append(face)
                    local_a = [a[axis] - low[axis] for axis in range(3)]
                    local_b = [b[axis] - low[axis] for axis in range(3)]
                    pieces.append((index, element(local_a, local_b, texture, skip=skip)))
        if pieces and abs(covered - volume) < 1e-6:
            for index, piece in pieces:
                parts[index].append(piece)
            continue
        # Reaches outside the footprint, or is rotated: keep it whole on the part nearest its center.
        center = list(rotation[2]) if rotation else [(frm[axis] + to[axis]) / 2 for axis in range(3)]
        index = min(range(len(footprint)), key=lambda i: sum(
            (center[axis] - (footprint[i][axis] * 16 + 8)) ** 2 for axis in range(3)))
        low = [footprint[index][axis] * 16 for axis in range(3)]
        local_a = [frm[axis] - low[axis] for axis in range(3)]
        local_b = [to[axis] - low[axis] for axis in range(3)]
        if min(local_a) < -16 or max(local_b) > 32:
            raise ValueError(f"{name}: element {frm}..{to} is too far from its part")
        local_rotation = None
        if rotation:
            local_rotation = (rotation[0], rotation[1], [rotation[2][axis] - low[axis] for axis in range(3)], *rotation[3:])
        inside = min(local_a) >= 0 and max(local_b) <= 16
        parts[index].append(element(local_a, local_b, texture, uv=not inside, rotation=local_rotation))
    return parts


def scaled_elements(elements):
    """The whole machine scaled down into one block, for the inventory and hand."""
    items = [unpack(item) for item in elements]
    low = [min(min(f[axis], t[axis]) for f, t, _, _ in items) for axis in range(3)]
    high = [max(max(f[axis], t[axis]) for f, t, _, _ in items) for axis in range(3)]
    scale = 16 / max(high[axis] - low[axis] for axis in range(3))
    shift = [(16 - (high[axis] - low[axis]) * scale) / 2 for axis in range(3)]

    def place(point):
        return [(point[axis] - low[axis]) * scale + shift[axis] for axis in range(3)]

    out = []
    for frm, to, texture, options in items:
        rotation = options.get("rotation")
        if rotation:
            rotation = (rotation[0], rotation[1], place(rotation[2]), *rotation[3:])
        out.append(element(place(frm), place(to), texture, uv=True, rotation=rotation))
    return out


# ------------------------------------------------------------------ writers

class StyleWriter:
    """Writes one style's machine assets under an assets/<mod> root."""

    def __init__(self, root):
        self.root = Path(root)

    def model(self, name, obj):
        write(self.root / "models" / "block" / f"{name}.json", obj)

    def item_model(self, name, obj):
        write(self.root / "models" / "item" / f"{name}.json", obj)

    def blockstate(self, name, obj):
        write(self.root / "blockstates" / f"{name}.json", obj)

    def item(self, name, model):
        write(self.root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": model}})

    def single_variant(self, name, model):
        self.blockstate(name, {"variants": {"": {"model": model}}})

    def facing_lit_states(self, name, lit):
        variants = {}
        for facing, y in FACING_Y.items():
            rotation = {"y": y} if y else {}
            for state in ("false", "true"):
                on = state == "true" and lit
                variants[f"facing={facing},lit={state}"] = {"model": rid(f"block/{name}_on" if on else f"block/{name}"),
                                                             **rotation}
        self.blockstate(name, {"variants": variants})

    def large_states(self, name, parts, lit):
        variants = {}
        for facing, y in FACING_Y.items():
            rotation = {"y": y} if y else {}
            for state in ("false", "true"):
                for part in range(PART_STATES):
                    if part < parts:
                        on = "_on" if state == "true" and lit else ""
                        model = rid(f"block/{name}_part{part}{on}")
                    else:
                        model = rid("block/large_machine_empty")
                    variants[f"facing={facing},lit={state},part={part}"] = {"model": model, **rotation}
        self.blockstate(name, {"variants": variants})

    def empty_part(self, particle):
        self.model("large_machine_empty", {"textures": {"particle": rid(f"block/{particle}")}, "elements": []})


def write_classic(root, machines, parts, fluid_blocks):
    """The original look: textured orientable cubes, plus the large machines in tools/large_machines.py."""
    from large_machines import FOOTPRINTS, MODELS, FRONTS
    out = StyleWriter(root)
    for machine, info in machines.items():
        if machine in FOOTPRINTS:
            front = FRONTS[machine]
            textures = {name: rid(f"block/{name}") for name in texture_names(MODELS[machine])}
            textures["front"] = rid(f"block/{front}")
            textures["particle"] = rid("block/machine_side")
            for index, elements in enumerate(slice_model(machine, MODELS[machine], FOOTPRINTS[machine])):
                out.model(f"{machine}_part{index}", {"ambientocclusion": False, "textures": textures, "elements": elements})
                if info["lit"]:
                    out.model(f"{machine}_part{index}_on", {"parent": rid(f"block/{machine}_part{index}"),
                                                            "textures": {"front": rid(f"block/{front}_on")}})
            out.empty_part("machine_side")
            out.large_states(machine, len(FOOTPRINTS[machine]), info["lit"])
            out.item_model(machine, {"parent": "minecraft:block/block", "textures": textures,
                                     "elements": scaled_elements(MODELS[machine])})
            out.item(machine, rid(f"item/{machine}"))
            continue
        for suffix, front in (("", "front"), ("_on", "front_on")):
            if suffix and not info["lit"]:
                continue
            front_texture = info.get("front", f"{machine}_{front}")
            out.model(f"{machine}{suffix}", {
                "parent": "minecraft:block/orientable",
                "textures": {"top": rid(f"block/{info.get('top', 'machine_top')}"), "side": rid("block/machine_side"),
                             "front": rid(f"block/{front_texture}")},
            })
        out.facing_lit_states(machine, info["lit"])
        out.item(machine, rid(f"block/{machine}"))

    for part in parts:
        out.single_variant(part, rid(f"block/{part}"))
        out.model(part, {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{part}")}})
        out.item(part, rid(f"block/{part}"))

    for block in fluid_blocks:
        out.single_variant(block, rid(f"block/{block}"))
        out.model(block, {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": rid(f"block/{block}_top"), "side": rid(f"block/{block}_side"),
                         "bottom": rid(f"block/{block}_bottom")}})
        out.item(block, rid(f"block/{block}"))


def write_steampunk(root, machines, parts, fluid_blocks):
    """Detailed models for everything, from tools/steampunk_models.py."""
    from large_machines import FOOTPRINTS
    from steampunk_models import MODELS, GLOW, CUBES, PARTICLE
    out = StyleWriter(root)

    def textures_for(elements):
        textures = {name: rid(f"block/{name}") for name in texture_names(elements)}
        textures["particle"] = rid(f"block/{PARTICLE}")
        return textures

    def glow_override(elements):
        return {name: rid(f"block/{GLOW[name]}") for name in texture_names(elements) if name in GLOW}

    for machine, info in machines.items():
        elements = MODELS[machine]
        textures = textures_for(elements)
        lit = info["lit"] and bool(glow_override(elements))
        if machine in FOOTPRINTS:
            for index, part_elements in enumerate(slice_model(machine, elements, FOOTPRINTS[machine])):
                out.model(f"{machine}_part{index}", {"ambientocclusion": False, "textures": textures,
                                                     "elements": part_elements})
                if lit:
                    out.model(f"{machine}_part{index}_on", {"parent": rid(f"block/{machine}_part{index}"),
                                                            "textures": glow_override(elements)})
            out.empty_part(PARTICLE)
            out.large_states(machine, len(FOOTPRINTS[machine]), lit)
            out.item_model(machine, {"parent": "minecraft:block/block", "textures": textures,
                                     "elements": scaled_elements(elements)})
            out.item(machine, rid(f"item/{machine}"))
            continue
        out.model(machine, {"parent": "minecraft:block/block", "textures": textures,
                            "elements": slice_model(machine, elements, [(0, 0, 0)])[0]})
        if lit:
            out.model(f"{machine}_on", {"parent": rid(f"block/{machine}"), "textures": glow_override(elements)})
        out.facing_lit_states(machine, lit)
        out.item(machine, rid(f"block/{machine}"))

    for part in parts:
        out.single_variant(part, rid(f"block/{part}"))
        out.model(part, {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{CUBES[part]}")}})
        out.item(part, rid(f"block/{part}"))

    for block in fluid_blocks:
        elements = MODELS[block]
        out.single_variant(block, rid(f"block/{block}"))
        out.model(block, {"parent": "minecraft:block/block", "textures": textures_for(elements),
                          "elements": slice_model(block, elements, [(0, 0, 0)])[0]})
        out.item(block, rid(f"block/{block}"))


WRITERS = {"classic": write_classic, "steampunk": write_steampunk}


def pack_metadata(style):
    """pack.mcmeta for the built-in pack; the format range matches Fabric's own 26.3 test pack."""
    look = "the original classic look" if style == "classic" else "the steampunk look"
    return {"pack": {"description": f"Gives Jugcraft's machines {look}",
                     "min_format": 71, "max_format": 2048}}
