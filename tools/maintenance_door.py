"""The Maintenance Door (work list item 10, approved by Justin on 23 October 2026): a solid steel door for the Tri-Pillar's
pillar doorways, built with the plant doors' kit (tools/plant_doors.py: the pressed steel frame and its stops, the
hinges, the parallel-arm closer, the levers, the badge reader, the swing; nuclear/PlantDoorBlock.java), most often as a
pair (two leaves meeting in a 2-wide doorway).

  * The leaves: solid flush steel (no glass), painted steel grey; the frame gunmetal; stainless kick plates on both
    faces, a threshold saddle.
  * The active leaf (hinge left, from outside): a lever with a key cylinder outside, a lever inside, the astragal over
    the meeting edge, a NOTICE plate at eye height ("MAINTENANCE / AUTHORISED PERSONNEL ONLY"). The other leaf of a
    pair is flush (its flush bolts in its edge).
  * The badge reader (reader) on the wall beside the pair (drawn by the hinge-right leaf, as the plant's pairs): red
    while shut, green while open.
  * hood: a steel rain hood over the door outside with a warm LED downlight under it (the doors out on the roof and the
    tie decks); the upper half of a hooded door gives light (tower/MaintenanceDoorBlock).

The door's frame of reference is the plant doors': facing east = the way you walk in when you push it (from outside into
the pillar), x 0 the outside face, the wall along z, y 0..32 over both halves. Its leaves are part models 32 to 35 of
plant_door_part (tools/plant_doors.py parts; nuclear/PlantDoors.java leafPart), which client/PlantDoorRenderer.java swings
about the steel door's hinge pin with the steel door's closer arm (the same frame and leaf depth).

    python3 tools/maintenance_door.py      writes its models, blockstate, textures, part models, item, loot, recipe,
                                           names and tag
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import nuclear_materials as NM
import plant_doors as PD

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/jugcraft"
DATA = ROOT / "src/main/resources/data"
ID = "maintenance_door"
NAME = "Maintenance Door"
PD.SPECS[ID] = PD.Spec(ID, jw=0.8, hw=0.8, lx0=1.2, lt=1.75, sd=0.45, frame=True, closer=True, lite=False)
SP = PD.SPECS[ID]
LEAF, FRAME, NOTICE, LED, SADDLE = (f"{ID}_leaf", f"{ID}_frame", f"{ID}_notice", f"{ID}_led", f"{ID}_saddle")
GLOW = sorted(set(PD.GLOW) | {LED})
# the plant_door_part models of its leaves (tools/plant_doors.py parts): 32 + 2 * (hinge right) + paired
PART_BASE = 32
# the light the hood's downlight gives (tower/MaintenanceDoorBlock HOOD_LIGHT)
HOOD_LIGHT = 9
B = PD.B


def _recolour(boxes, old, new):
    out = []
    for b in boxes:
        t = b[6]
        if isinstance(t, dict):
            t = {f: (new if v == old else v) for f, v in t.items()}
        elif t == old:
            t = new
        out.append(b[:6] + [t] + b[7:])
    return out


def hood_boxes(paired):
    """The rain hood over the door on the outside face, above the casing (door y 32..34: the block over the door): a
    canopy 6 px out, its drip lip, the end plates, a back plate on the wall, the LED bar under it. A pair's leaf draws its
    half (the other leaf mirrors it), with an end plate at its hinge end only."""
    ze = 16 if paired else 17.2
    out = [B(-6.0, 33.1, -1.2, 0, 33.75, ze, FRAME), B(-6.0, 32.5, -1.2, -5.55, 33.1, ze, FRAME),
           B(-6.0, 32.5, -1.2, 0, 33.1, -0.75, FRAME), B(-0.6, 32.0, -1.2, 0, 33.1, ze, FRAME),
           B(-3.9, 32.9, 1.5, -2.7, 33.1, ze - (0 if paired else 1.5), LED)]
    if not paired:
        out.append(B(-6.0, 32.5, 16.75, 0, 33.1, 17.2, FRAME))
    return out


def frame_boxes(paired, reader, lit, hood):
    """The steel door's frame in this door's paint (the reader's boxes last), the hood, the threshold saddle: the saddle
    after the frame, so where they share a plane the saddle is the one moved (both halves' pieces of a jamb stay
    lined up)."""
    out = _recolour(PD.frame_boxes("steel_door", paired, reader, lit), PD.FRAME, FRAME)
    out += hood_boxes(paired) if hood else []
    return out + [B(-0.3, 0, 0, SP.depth, 0.25, 16 if paired else 16 - SP.jw, SADDLE)]


def leaf_boxes(hinge, paired):
    """The leaf shut (hinge left coordinates; a right-hinged leaf is mirrored after), y 0..32, with its hardware."""
    z0, z1 = SP.leaf_z(paired)
    a, b = SP.lx0, SP.lx1
    yb, yt = 0.3, SP.top - 0.1
    active = hinge == "left" or not paired
    out = [B(a, yb, z0, b, yt, z1, LEAF)]
    out.append(B(a - 0.1, 0.5, z0 + 0.15, a, 4.5, z1 - (0.5 if paired and active else 0.15), PD.STAINLESS))
    out.append(B(b, 0.5, z0 + 0.15, b + 0.1, 4.5, z1 - 0.15, PD.STAINLESS))
    if active:
        if paired:
            out.append(B(a - 0.25, yb, z1 - 0.35, a, yt, z1 + 0.3, FRAME))
        c = z1 - (1.75 if paired else 1.3)
        out += PD.lever(a, c, -1, key=True)
        out += PD.lever(b, c, 1)
        zc = (z0 + z1) / 2 - (0.5 if paired else 0)
        out.append(B(a - 0.06, 19.6, zc - 4.4, a, 23.8, zc + 4.4, {"*": FRAME, "west": NOTICE + "!"}))
    out += PD.closer_body(SP, z0)
    return out


def door_boxes(hinge, open_, moving, paired, reader, hood):
    """Everything in the door's two block models (y 0..32 and the hood over it, cut at 16, pictures pinned): (boxes,
    indices kept whole in the lower half: the reader). Swinging (moving), only the frame and the closer's shoe."""
    drawn = reader and (not paired or hinge == "right")
    frame = frame_boxes(paired, drawn, open_, hood)
    n_after = (len(hood_boxes(paired)) if hood else 0) + 1
    n_reader = 2 if drawn else 0
    end = len(frame) - n_after
    frame, keep = PD.cut16(frame, set(range(end - n_reader, end)))
    boxes = PD.fixed(frame)
    if not moving:
        leaf = PD.fixed(PD.cut16(leaf_boxes(hinge, paired))[0])
        arm = PD.fixed(PD.arm_boxes(SP, 90 if open_ else 0))
        if open_:
            leaf = PD.turned90(leaf, SP.pivot(), 1)
        boxes += leaf + arm
    else:
        boxes += PD.fixed(PD.arm_boxes(SP, 0)[-1:])
    if hinge == "right":
        boxes = PD.mirrored(boxes)
    return boxes, keep


def halves(boxes, keep):
    """The lower and upper models (the upper's moved down 16); the hood goes with the upper half."""
    lo, up = [], []
    for i, b in enumerate(boxes):
        if i in keep or b[4] <= 16 + 1e-6:
            lo.append(b)
        else:
            up.append(PD._moved(b, -16))
    return lo, up


def leaf_part(hinge, paired):
    """The leaf shut with its hardware (y 0..32, cut at 16): the part client/PlantDoorRenderer.java turns."""
    boxes = PD.fixed(PD.cut16(leaf_boxes(hinge, paired))[0])
    return PD.mirrored(boxes) if hinge == "right" else boxes


def parts():
    """{part index: boxes} of its leaves, for tools/plant_doors.py parts."""
    return {PART_BASE + (2 if hinge == "right" else 0) + int(paired): leaf_part(hinge, paired)
            for hinge in ("left", "right") for paired in (False, True)}


def model(boxes):
    """A block model of the boxes (the plant doors' writer, its LED bar glowing)."""
    import plant_interior as PI
    import zfight
    elements, used = PI._elements(PD._separate([tuple(b) for b in boxes]), GLOW)
    textures = {t: ("minecraft:block/" + t[3:] if t.startswith("mc_") else f"jugcraft:block/{t}") for t in sorted(used)}
    textures["particle"] = f"jugcraft:block/{LEAF}"
    return zfight.fix({"textures": textures, "elements": elements})


# ---------------------------------------------------------------------------------------------------- textures

def _font(size):
    return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", size)


def notice():
    """The NOTICE plate (ANSI style): a blue header, MAINTENANCE and AUTHORISED PERSONNEL ONLY in black on white, a thin
    border; drawn for its 8.8 x 4.2 px on the leaf (0.55 x 0.26 m)."""
    w, h = 352, 168
    img = Image.new("RGBA", (w, h), (244, 244, 240, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w - 1, h - 1], outline=(30, 30, 30, 255), width=4)
    d.rectangle([8, 8, w - 9, 54], fill=(0, 87, 160, 255))
    d.text((w / 2, 31), "NOTICE", font=_font(36), fill=(255, 255, 255, 255), anchor="mm")
    d.text((w / 2, 88), "MAINTENANCE", font=_font(40), fill=(20, 20, 20, 255), anchor="mm")
    d.text((w / 2, 132), "AUTHORISED PERSONNEL ONLY", font=_font(18), fill=(20, 20, 20, 255), anchor="mm")
    return img


def textures():
    return {LEAF: NM.painted((126, 133, 139), 5101, chip_count=0, drip_count=0, dirt=0.07, size=64),
            FRAME: NM.painted((66, 72, 78), 5102, chip_count=0, drip_count=0, dirt=0.1, size=64),
            SADDLE: NM.brushed((168, 170, 172), 5103, "h", dirt=0.12, size=64),
            LED: NM.to_img(NM.base((255, 236, 200), 16)),
            NOTICE: notice()}


# ------------------------------------------------------------------------------------------------------ writing

FLAGS = (False, True)


def _json(path, obj):
    """As tools/generate_material_data.py write: a model's elements finished (tools/model_writer.py)."""
    if isinstance(obj, dict) and obj.get("elements"):
        import model_writer
        model_writer.finish_elements(obj["elements"])
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def _b(v):
    return str(v).lower()


def write_assets():
    """The door's models (deduplicated), its blockstate (every state of tower/MaintenanceDoorBlock: the plant door's
    and hood; exit hardware it has none of), textures, the leaves' part models, the item."""
    for name, img in textures().items():
        img.save(ASSETS / "textures/block" / f"{name}.png", optimize=True)
    names, variants = {}, {}
    for hinge in ("left", "right"):
        for open_ in FLAGS:
            for moving in FLAGS:
                for paired in FLAGS:
                    for reader in FLAGS:
                        for hood in FLAGS:
                            boxes, keep = door_boxes(hinge, open_, moving, paired, reader, hood)
                            lo, up = halves(boxes, keep)
                            tag = (f"{'o' if open_ else 'c'}{'m' if moving else ''}{'p' if paired else ''}"
                                   f"{'r' if reader else ''}{'h' if hood else ''}")
                            for half, bx in (("lower", lo), ("upper", up)):
                                m = model(bx)
                                key = json.dumps(m, sort_keys=True)
                                if key not in names:
                                    names[key] = f"{ID}_{half}_{hinge}_{tag}"
                                    _json(ASSETS / "models/block" / f"{names[key]}.json", m)
                                for exit_ in FLAGS:
                                    for facing, y in PD.FACING_Y.items():
                                        state = (f"exit={_b(exit_)},facing={facing},half={half},hinge={hinge},hood={_b(hood)},"
                                                 f"moving={_b(moving)},open={_b(open_)},paired={_b(paired)},reader={_b(reader)}")
                                        variants[state] = {"model": f"jugcraft:block/{names[key]}", **({"y": y} if y else {})}
    _json(ASSETS / "blockstates" / f"{ID}.json", {"variants": dict(sorted(variants.items()))})
    # the leaves' part models, and plant_door_part's states for them, after the plant doors' own (tools/plant_doors.py
    # write_assets writes the same)
    path = ASSETS / "blockstates/plant_door_part.json"
    pv = json.loads(path.read_text())["variants"]
    for k, boxes in parts().items():
        _json(ASSETS / "models/block" / f"plant_door_part_{k}.json", PD._model(boxes, lambda p: f"jugcraft:{p}"))
        pv[f"part={k}"] = {"model": f"jugcraft:block/plant_door_part_{k}"}
    _json(path, {"variants": pv})
    _json(ASSETS / "models/item" / f"{ID}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{ID}"}})
    _json(ASSETS / "items" / f"{ID}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{ID}"}})
    import item_icons
    item_icons.draw(ID).save(ASSETS / f"textures/item/{ID}.png")
    return len(names), len(variants)


LANG = {f"block.jugcraft.{ID}": NAME,
        f"block.jugcraft.{ID}_reader": f"{NAME} (Badge Reader)",
        f"block.jugcraft.{ID}_hood": f"{NAME} (Rain Hood)",
        f"block.jugcraft.{ID}_reader_hood": f"{NAME} (Badge Reader, Rain Hood)"}
RECIPE = (["PP", "PC", "PP"], {"P": "#c:plates/steel", "C": "jugcraft:basic_circuit"}, 3)


def write_data():
    """Its loot (the lower half drops the door, keeping its reader and hood on the item), recipe, names, tag."""
    _json(DATA / "jugcraft/loot_table/blocks" / f"{ID}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": f"jugcraft:{ID}",
        "modifier": {"type": "minecraft:copy_state", "block": f"jugcraft:{ID}", "properties": ["reader", "hood"]},
        "condition": {"type": "minecraft:match_block", "blocks": f"jugcraft:{ID}", "state": {"half": "lower"}}}],
        "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": f"jugcraft:blocks/{ID}"})
    pattern, key, count = RECIPE
    _json(DATA / "jugcraft/recipe" / f"{ID}.json", {
        "fabric:load_conditions": [{"condition": "jugcraft:feature_enabled", "feature": "machines"},
                                   {"condition": "jugcraft:feature_enabled", "feature": "drones"}],
        "type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern, "key": key,
        "result": {"id": f"jugcraft:{ID}", "count": count}})
    import advanced_crafting              # made on the Advanced Workbench (the same cost, a shape bigger than 3 x 3)
    advanced_crafting.convert(DATA / "jugcraft/recipe", [ID])
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    sorted_before = list(lang) == sorted(lang)
    lang.update(LANG)
    if sorted_before:
        lang = dict(sorted(lang.items()))
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    p = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(p.read_text())
    if f"jugcraft:{ID}" not in t["values"]:
        t["values"].append(f"jugcraft:{ID}")
    p.write_text(json.dumps(t, indent=2) + "\n")


if __name__ == "__main__":
    print(write_assets())
    write_data()
