"""The drop pod for the game (Justin's pick on sheet 3, C the dart, 7 November 2026; tools/drop_pod/dart.py): its mesh,
in the crew ship's format 2 (client/CrewShipMesh reads it), and its textures.

  assets/jugcraft/drop_pod/pod.bin.gz          parts: "body" (the hull, its doorway open), "door" (the hatch, about its
                                               middle: blown off on landing), "inner" (the cocoon and its restraints,
                                               open, seen once the hatch is gone)
  assets/jugcraft/textures/entity/drop_pod/    one texture a material

In the pod's frame: blocks, y up from the heat shield's lowest point, the hatch facing -z. Usage: python3
tools/drop_pod_export.py"""
import gzip, os, struct, sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "drop_pod"))
import pod as P            # noqa: E402
import dart as D           # noqa: E402

OUT = "src/main/resources/assets/jugcraft"


def _utf(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def export():
    m = D.dart(door_apart=True, interior=True, restraints="open", scorched=1.0)
    names, textures, arrays = [], {}, {}

    def mat_name(t):
        if isinstance(t, str):
            if t not in textures:
                textures[t] = P.mat(t) if t not in P._T else P._T[t]
            return t
        key = id(t)
        if key not in arrays:
            arrays[key] = "decal_%d" % len(arrays)
            textures[arrays[key]] = t
        return arrays[key]

    def quad_bytes(P_, UV, t, glow, piv):
        P_ = np.asarray(P_, float) - np.asarray(piv, float)
        UV = np.asarray(UV, float)
        n = -np.cross(P_[1] - P_[0], P_[2] - P_[0])
        if np.linalg.norm(n) < 1e-12:
            n = -np.cross(P_[2] - P_[0], P_[3] - P_[0])
        n = n / (np.linalg.norm(n) + 1e-12)
        name = mat_name(t)
        if name not in names:
            names.append(name)
        out = struct.pack(">BB", names.index(name), 1 if glow else 0)
        for i in (3, 2, 1, 0):                              # (the game's winding: client/CrewShipMesh)
            out += struct.pack(">5f3b", P_[i][0], P_[i][1], P_[i][2], UV[i][0], UV[i][1], *[int(round(c * 127)) for c in n])
        return out

    piv0 = (0.0, 0.0, 0.0)
    groups = [("body", piv0, m.faces), ("door", tuple(float(v) for v in m.door_centre), m.door.faces),
              ("inner", piv0, m.lining.faces + m.inner.faces)]
    body = b"JCS1" + struct.pack(">i", 2)
    blobs = []
    for gname, piv, faces in groups:
        qs = [quad_bytes(f[0], f[1], f[2], f[3], piv) for f in faces]
        blobs.append(_utf(gname) + struct.pack(">3f", *piv) + struct.pack(">i", len(qs)) + b"".join(qs))
    body += struct.pack(">i", len(names)) + b"".join(_utf(n) for n in names)
    body += struct.pack(">i", len(groups)) + b"".join(blobs)
    os.makedirs(f"{OUT}/drop_pod", exist_ok=True)
    with open(f"{OUT}/drop_pod/pod.bin.gz", "wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as fh:
        fh.write(body)
    os.makedirs(f"{OUT}/textures/entity/drop_pod", exist_ok=True)
    for name in names:
        arr = np.asarray(textures[name], np.float32)
        Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA").save(f"{OUT}/textures/entity/drop_pod/{name}.png")
    print("pod:", {g: len(f) for g, _, f in groups}, "materials", len(names), "door centre", m.door_centre, "soldier", D.SOLDIER)


if __name__ == "__main__":
    export()
