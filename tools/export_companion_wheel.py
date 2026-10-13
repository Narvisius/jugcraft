"""Export the checked-in Blockbench wheel meshes without changing UVs or winding.

Run from the repository root; --check verifies the runtime export instead of writing it.
"""
import argparse
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "companion-models/wheel/Companion Generator Wheel.bbmodel"
OUTPUT = ROOT / "src/main/resources/assets/jugcraft/companion_wheel_quads.json"


def export():
    model = json.loads(SOURCE.read_text(encoding="utf-8"))
    elements = {element["uuid"]: element for element in model["elements"]}
    parts = {}
    for name, group in zip(("companion_wheel_base", "companion_wheel_rotor"), model["outliner"], strict=True):
        quads = []
        for uid in group["children"]:
            element = elements[uid]
            if element["type"] != "mesh" or any(element["rotation"]) or any(element["origin"]):
                raise ValueError(f"{element['name']}: bake mesh transforms before exporting")
            for face in element["faces"].values():
                keys = face["vertices"]
                if len(keys) != 4 or face["texture"] != 0:
                    raise ValueError(f"{element['name']}: expected quads using the wheel atlas")
                points = [element["vertices"][key] for key in keys]
                a = [points[1][i] - points[0][i] for i in range(3)]
                b = [points[2][i] - points[0][i] for i in range(3)]
                normal = [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]
                length = math.sqrt(sum(value*value for value in normal))
                if length == 0:
                    raise ValueError(f"{element['name']}: degenerate face")
                quads.append({"texture": "companion_wheel", "normal": [value/length for value in normal],
                              "vertices": [point + [face["uv"][key][i]/model["resolution"][axis]
                                                     for i, axis in enumerate(("width", "height"))]
                                           for key, point in zip(keys, points, strict=True)]})
        parts[name] = quads
    return parts


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    result = export()
    if args.check:
        if json.loads(OUTPUT.read_text(encoding="utf-8")) != result:
            raise SystemExit("Wheel export is stale: run python tools/export_companion_wheel.py")
        print("PASS: Blockbench wheel and runtime quads agree")
    else:
        OUTPUT.write_text(json.dumps(result, separators=(",", ":")) + "\n", encoding="utf-8", newline="\n")
