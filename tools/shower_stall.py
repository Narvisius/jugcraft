"""The shower stall (work list item 22, part A: the Tri-Pillar locker room's showers; approved 23 Oct 2026). One block,
as the plant's toilet partitions are (its panels run up into the block above, to 30 px): a tiled back on the wall,
phenolic partitions down both sides (the plant restroom's), a tiled shower tray with its drain, the chrome shower head on
its arm, the mixer valve and its lever, a soap shelf, a curtain rail across the front with the curtain drawn back to one
side. Stalls stand side by side in a bank, partition to partition.

It works like the restroom's fixtures (nuclear/RestroomBlocks.Shower): right-click to turn it on (the lever swings up,
water falls from the head and churns on the tray, the spray's sound), again to turn it off; it shuts itself off after a
minute. Standing under it puts you out if you are on fire.

Writes its models (dry and running), blockstate (facing, running), item, lang, loot table, recipe, mineable tags, the
curtain texture and its sound (tools/restroom_sounds.py's shower, its sounds.json entry). Model pixels, the stall facing
north (the user looks north at the shower head; the wall behind it at z 0).
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"
ID = "shower_stall"
NAME = "Shower Stall"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
# The shower head's nozzle face (centre, pixels) and the tray's top, for the water (Java: RestroomBlocks.Shower).
HEAD = (8.0, 22.6, 6.0)
TRAY_TOP = 0.6


def box(f, t, faces, rot=None):
    e = {"from": [round(v, 3) for v in f], "to": [round(v, 3) for v in t], "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


def all_faces(tex, skip=()):
    return {k: {"texture": tex} for k in ("north", "south", "east", "west", "up", "down") if k not in skip}


def elements(running):
    tile, edge, part = "#tile", "#edge", "#partition"
    chrome, floor, grate, curtain = "#chrome", "#floor", "#grate", "#curtain"
    e = []
    # the tiled back on the wall, between the partitions
    e.append(box((0.8, 0, 0), (15.2, 16, 0.9), {"south": {"texture": tile, "uv": [0.8, 0, 15.2, 16]}}))
    e.append(box((0.8, 16, 0), (15.2, 30, 0.9), {"south": {"texture": "#cap", "uv": [0.8, 2, 15.2, 16]},
                                                  "up": {"texture": edge, "uv": [0.8, 0, 15.2, 0.9]}}))
    # the partitions down both sides (the plant's phenolic, 1.6 px off the floor as theirs are)
    for x0, x1 in ((0.3, 0.8), (15.2, 15.7)):
        e.append(box((x0, 1.6, 0), (x1, 28.5, 16), {
            "east": {"texture": part, "uv": [0, 0, 16, 16]}, "west": {"texture": part, "uv": [0, 0, 16, 16]},
            "south": {"texture": edge, "uv": [0, 0, 0.5, 16]}, "up": {"texture": edge, "uv": [0, 0, 0.5, 16]},
            "down": {"texture": edge, "uv": [0, 0, 0.5, 16]}}))
    # the tray: tiled, a lip at the front, the drain in the middle
    e.append(box((0.8, 0, 0.9), (15.2, TRAY_TOP, 16), {"up": {"texture": floor, "uv": [0.8, 0.9, 15.2, 16]},
                                                       "south": {"texture": floor, "uv": [0.8, 15, 15.2, 15.6]}}))
    e.append(box((0.8, 0.6, 14.8), (15.2, 1.4, 16), {"up": {"texture": floor}, "north": {"texture": floor}, "south": {"texture": floor}}))
    e.append(box((6.8, 0.6, 7.2), (9.2, 0.7, 9.6), {"up": {"texture": grate}}))
    # the shower head: its arm out of the wall, the head angled down
    e.append(box((7.4, 24.4, 0.9), (8.6, 25.4, 4.6), all_faces(chrome, ("north",))))
    e.append(box((6.0, 22.9, 3.6), (10.0, 23.9, 7.6), {"down": {"texture": "#nozzle"}, "up": {"texture": chrome},
                                                      "north": {"texture": chrome}, "south": {"texture": chrome},
                                                      "east": {"texture": chrome}, "west": {"texture": chrome}},
                 {"angle": 22.5, "axis": "x", "origin": [8, 24.4, 4.6]}))
    # the mixer valve's trim plate, its body and lever (off: pointing right; on: swung up 45 degrees)
    e.append(box((6.2, 15.2, 0.9), (9.8, 18.8, 1.2), all_faces(chrome, ("north",))))
    e.append(box((7.4, 16.4, 1.2), (8.6, 17.6, 2.6), all_faces(chrome, ("north",))))
    e.append(box((7.7, 16.7, 2.6), (11.2, 17.3, 3.1), all_faces(chrome),
                 {"angle": 45, "axis": "z", "origin": [8, 17, 2.85]} if running else None))
    # the soap shelf
    e.append(box((11.4, 19.0, 0.9), (14.4, 19.5, 3.4), all_faces(chrome, ("north",))))
    # the curtain rail across the front, its ends into the partitions; the curtain drawn back to the left
    e.append(box((0.8, 27.0, 14.6), (15.2, 27.5, 15.1), all_faces(chrome, ("east", "west"))))
    for k in range(5):
        x0 = 0.8 + k * 0.9
        z0 = 14.3 + (0.5 if k % 2 else 0)
        e.append(box((x0, 4.5, z0), (x0 + 0.95, 26.8, z0 + 0.35), {"north": {"texture": curtain, "uv": [k * 3, 0, k * 3 + 3, 16]},
                                                                    "south": {"texture": curtain, "uv": [k * 3, 0, k * 3 + 3, 16]},
                                                                    "east": {"texture": curtain, "uv": [0, 0, 0.5, 16]},
                                                                    "west": {"texture": curtain, "uv": [0, 0, 0.5, 16]},
                                                                    "down": {"texture": curtain, "uv": [0, 0, 1, 0.5]}}))
    if running:
        e += water()
    # every face of a box you can see into drawn (model_writer: block models cull back faces), every UV inside its sprite
    import model_writer as MW
    MW.close_open_faces(e)
    MW.fit_uvs(e)
    return e


def water():
    """The water running: streams fanning down from the head to the tray (thin see-through columns, each a little
    further out than the last as the head's spray opens), the churn on the tray round the drain."""
    out = []
    hx, hy, hz = HEAD
    for dx, dz in ((-1.6, -0.6), (-0.6, 0.4), (0.5, -0.3), (1.5, 0.5), (-1.0, 1.6), (0.9, 1.8), (0.0, 2.8)):
        x, z = hx + dx * 1.3, hz + dz * 1.3
        w = 0.22
        out.append(box((x - w, TRAY_TOP + 0.05, z - w), (x + w, hy - 0.4, z + w),
                       {f: {"texture": "#water", "uv": [0, 0, 1, 16]} for f in ("north", "south", "east", "west")}))
    # (clear of the tray and of the drain's grate under it by more than the 0.09 px that flickers)
    out.append(box((3.0, TRAY_TOP + 0.18, 3.4), (13.0, TRAY_TOP + 0.22, 12.6), {"up": {"texture": "#churn"}}))
    return out


TEXTURES = {"tile": "jugcraft:block/restroom_wall_tile", "cap": "jugcraft:block/restroom_wall_tile_cap", "edge": "jugcraft:block/pi_rr_partition_edge",
            "partition": "jugcraft:block/pi_rr_partition", "chrome": "jugcraft:block/pi_chrome",
            "floor": "jugcraft:block/restroom_floor_tile", "grate": "jugcraft:block/pi_rr_grate",
            "nozzle": "jugcraft:block/pi_rr_nozzle", "curtain": "jugcraft:block/pi_rr_curtain",
            "particle": "jugcraft:block/restroom_wall_tile"}


def model(running=False):
    tex = dict(TEXTURES)
    if running:
        tex.update({"water": "jugcraft:block/pi_rr_water", "churn": "jugcraft:block/pi_rr_churn"})
    return {"textures": tex, "elements": elements(running)}


def item_model():
    """In a slot, a hand, a frame: the stall a little smaller than a block (it is nearly two tall)."""
    return {"parent": f"jugcraft:block/{ID}", "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -3.5, 0], "scale": [0.38, 0.38, 0.38]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.18, 0.18, 0.18]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, -4, 0], "scale": [0.36, 0.36, 0.36]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}}


def curtain_texture():
    """A light grey shower curtain: soft vertical folds, a hem near the bottom, grommets along the top."""
    w, h = 32, 32
    x = np.arange(w)[None, :]
    shade = 0.86 + 0.08 * np.sin(x / w * 2 * np.pi * 3) + 0.02 * np.sin(x / w * 2 * np.pi * 9)
    shade = np.broadcast_to(shade, (h, w)).copy()
    shade[h - 4:h - 3, :] *= 0.9
    a = np.zeros((h, w, 4), np.uint8)
    base = np.array([214, 220, 224])
    for i in range(3):
        a[..., i] = np.clip(base[i] * shade, 0, 255)
    a[..., 3] = 255
    for gx in range(2, w, 6):
        a[1:3, gx:gx + 2, :3] = (170, 176, 182)
    return Image.fromarray(a, "RGBA")


def _json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


RECIPE = {"fabric:load_conditions": [{"condition": "jugcraft:feature_enabled", "feature": "machines"}],
          "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["PWP", "PIP", "PTP"],
          "key": {"P": "#c:plates/steel", "W": "minecraft:white_wool", "I": "minecraft:iron_ingot", "T": "jugcraft:restroom_floor_tile"},
          "result": {"id": f"jugcraft:{ID}", "count": 1}}


def write():
    import restroom_sounds as RS
    _json(ASSETS / "models/block" / f"{ID}.json", model(False))
    _json(ASSETS / "models/block" / f"{ID}_running.json", model(True))
    _json(ASSETS / "blockstates" / f"{ID}.json", {"variants": {
        f"facing={f},running={r}": {"model": f"jugcraft:block/{ID}{'_running' if r == 'true' else ''}", **({"y": y} if y else {})}
        for f, y in FACINGS.items() for r in ("false", "true")}})
    _json(ASSETS / "models/item" / f"{ID}.json", item_model())
    _json(ASSETS / "items" / f"{ID}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{ID}"}})
    curtain_texture().save(ASSETS / "textures/block/pi_rr_curtain.png")
    _json(DATA / "jugcraft/loot_table/blocks" / f"{ID}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jugcraft:{ID}"}],
                                             "condition": {"type": "minecraft:survives_explosion"}}],
        "random_sequence": f"jugcraft:blocks/{ID}"})
    _json(DATA / "jugcraft/recipe" / f"{ID}.json", RECIPE)
    import advanced_crafting              # made on the Advanced Workbench (the same cost, a shape bigger than 3 x 3)
    advanced_crafting.convert(DATA / "jugcraft/recipe", [ID])
    for tag in ("mineable/pickaxe", "needs_stone_tool"):
        p = DATA / "minecraft/tags/block" / f"{tag}.json"
        t = json.loads(p.read_text())
        if f"jugcraft:{ID}" not in t["values"]:
            t["values"].append(f"jugcraft:{ID}")
            p.write_text(json.dumps(t, indent=2) + "\n")
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang[f"block.jugcraft.{ID}"] = NAME
    lang.update(RS.LANG)
    if sorted_before:
        lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    # its sound, and its sounds.json entry (tools/generate_material_data.py writes the same from restroom_sounds.SOUNDS)
    RS.write("shower", RS.shower())
    sp = ASSETS / "sounds.json"
    sounds = json.loads(sp.read_text())
    if "restroom.shower" not in sounds:
        sounds["restroom.shower"] = RS.SOUNDS["restroom.shower"]
        sp.write_text(json.dumps(sounds, indent=2) + "\n")


if __name__ == "__main__":
    write()
    print("shower stall:", len(elements(False)), "elements dry,", len(elements(True)), "running")
