"""JSON resources for the eighth batch of Halloween decorations, the mad scientist and monsters, from
tools/agriculture.py: the Tesla Coil, the Lab Table, the Specimen Jar, the Mummy Sarcophagus, the Raven on a Perch and
the Black Cat Figure; their names, messages, loot and tags, and the quads the client draws the moving parts from
(assets/jugcraft/decor8_quads.json).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files and the earlier
batches' (tools/decor_data.py, decor6_data.py, decor7_data.py). Block model rotations are right-handed: about x, a
positive angle turns +y toward +z.

The coil and the sarcophagus (two blocks tall) and the lab table (two blocks long) are modelled whole, then cut into
one model per block (`split`); their items show them whole, scaled down.
"""
from agriculture import TESLA_COIL, LAB_TABLE, SPECIMEN_JAR, SARCOPHAGUS, RAVEN, BLACK_CAT
from decor_data import MOD, HORIZONTAL, rid, turned, box, block_model, self_drop
from decor3_data import fitted
from decor6_data import quads
from decor7_data import scaled
from halloween_data import match_block

AXIS_FACES = (("west", "east"), ("down", "up"), ("north", "south"))


def split(elements, cell):
    """The part of `elements` inside the block at `cell` (cx, cy, cz, in blocks), moved into that block: boxes clipped
    to it, the faces on their cuts left out. Only unrotated boxes."""
    lo_cell = [c * 16 for c in cell]
    out = []
    for element in elements:
        assert "rotation" not in element and "rotations" not in element
        frm, to = element["from"], element["to"]
        lo = [max(frm[k], lo_cell[k]) for k in range(3)]
        hi = [min(to[k], lo_cell[k] + 16) for k in range(3)]
        if any(hi[k] - lo[k] <= 1e-6 for k in range(3)):
            continue
        faces = dict(element["faces"])
        for k in range(3):
            low_face, high_face = AXIS_FACES[k]
            if lo[k] > frm[k]:
                faces.pop(low_face, None)
            if hi[k] < to[k]:
                faces.pop(high_face, None)
        if faces:
            out.append({**element, "from": [round(lo[k] - lo_cell[k], 4) for k in range(3)],
                        "to": [round(hi[k] - lo_cell[k], 4) for k in range(3)], "faces": faces})
    return out


# ---------------------------------------------------------------- the tesla coil

COIL_TEXTURES = {"iron": "tesla_coil_iron", "copper": "tesla_coil_copper", "winding": "tesla_coil_winding",
                 "toroid": "tesla_coil_toroid"}


def coil_elements(active):
    """A riveted base, a copper primary, a tall copper winding (glowing while it runs) on an insulator, and a polished
    toroid at the top; 32 pixels tall."""
    glow = 10 if active else None
    return [box((2, 0, 2), (14, 4, 14), "#iron"), box((3, 4, 3), (13, 5, 13), "#iron"),
            box((4.5, 5, 4.5), (11.5, 7, 11.5), "#copper"),
            box((6.5, 7, 6.5), (9.5, 26, 9.5), "#winding", light=glow),
            box((7, 26, 7), (9, 27.5, 9), "#iron"),
            box((3, 27.5, 5), (13, 30.5, 11), "#toroid"), box((5, 27.5, 3), (11, 30.5, 13), "#toroid"),
            box((4, 28, 4), (12, 30, 12), "#toroid"),
            box((7.5, 30.5, 7.5), (8.5, 31.5, 8.5), "#iron")]


# ---------------------------------------------------------------- the lab table

TABLE_TEXTURES = {"steel": "lab_table_steel", "leather": "lab_table_leather", "sheet": "lab_table_sheet", "skin": "lab_table_skin"}


def table_elements():
    """A steel table two blocks long, its head to the north (z from -16 to 16; the foot block is 0 to 16): a top with a
    raised rim, four legs, a shelf below, and leather straps hanging at its sides."""
    steel, leather = "#steel", "#leather"
    elements = [box((0.5, 11, -15.5), (15.5, 12.5, 15.5), steel),
                box((0, 11.5, -15.5), (0.5, 13, 15.5), steel), box((15.5, 11.5, -15.5), (16, 13, 15.5), steel),
                box((1.5, 3, -14), (14.5, 4, 14), steel)]
    for x in (1.5, 13):
        for z in (-14.5, 13):
            elements.append(box((x, 0, z), (x + 1.5, 11, z + 1.5), steel))
    for z in (-6.5, 7.5):
        elements += [box((-0.25, 7.5, z), (0, 12.5, z + 1.5), leather), box((16, 7.5, z), (16.25, 12.5, z + 1.5), leather)]
    return elements


def patient_legs():
    return [box((3.5, 12.5, 2), (12.5, 15, 15), "#sheet"),
            box((4, 15, 13.5), (7, 16.5, 15), "#sheet"), box((9, 15, 13.5), (12, 16.5, 15), "#sheet")]


def patient_torso():
    """The torso and head under the sheet, from the hips (z 2) to the head (z -15), and a grey hand slipped out from
    under the sheet."""
    return [box((2.5, 12.5, -10), (13.5, 16, 2), "#sheet"),
            box((5, 12.5, -15), (11, 17, -10), "#sheet"),
            box((13.5, 10.5, -4), (15, 13, -2.5), "#skin")]


# ---------------------------------------------------------------- the specimen jar

JAR_TEXTURES = {"iron": "specimen_jar_iron", "glass": "specimen_jar_glass", "fluid": "specimen_jar_fluid"}
SPECIMEN_TEXTURES = {"eye": "specimen_eye", "nerve": "specimen_nerve", "tentacle": "specimen_tentacle", "pumpkin": "specimen_pumpkin",
                     "stem": "specimen_stem", "brain": "specimen_brain"}


def jar_elements():
    """An iron base and lid round a glass jar of glowing green fluid."""
    return [box((4, 0, 4), (12, 1, 12), "#iron"),
            box((5, 1, 5), (11, 10.5, 11), "#fluid", light=15),
            box((4.5, 1, 4.5), (11.5, 12, 11.5), "#glass", faces=("north", "south", "east", "west")),
            box((4.25, 12, 4.25), (11.75, 13, 11.75), "#iron"), box((7, 13, 7), (9, 13.5, 9), "#iron")]


def specimen_elements(specimen):
    """What floats in the jar, round its middle (8, 6, 8)."""
    if specimen == "eye":
        # The Flying Eyeball's clean eye, rounder and mapped on every side (tools/decor16_data.py specimen_eye).
        import decor16_data
        return decor16_data.specimen_eye_elements("#eye")
    if specimen == "tentacle":
        return [box((7, 2.5, 7), (9, 5, 9), "#tentacle"), box((7.25, 5, 7.5), (8.75, 7.5, 9), "#tentacle"),
                box((7.5, 7.5, 8), (8.5, 9.5, 9), "#tentacle"), box((7.75, 9.5, 8.75), (8.25, 10, 9.75), "#tentacle")]
    if specimen == "pumpkin":
        return [box((6.5, 4.5, 6.5), (9.5, 7, 9.5), "#pumpkin"), box((7.75, 7, 7.75), (8.25, 7.75, 8.25), "#stem")]
    return [box((6, 4.5, 6.5), (10, 7.5, 9.5), "#brain"), box((7.5, 3.5, 7.5), (8.5, 4.5, 8.5), "#nerve")]


# ---------------------------------------------------------------- the mummy sarcophagus

SARCOPHAGUS_TEXTURES = {"case": "mummy_sarcophagus_case", "inside": "mummy_sarcophagus_inside", "gold": "mummy_sarcophagus_gold",
                        "lid_face": "mummy_sarcophagus_lid_face", "wraps": "mummy_sarcophagus_wraps", "mummy_face": "mummy_sarcophagus_mummy_face"}


def sarcophagus_case():
    """The open case, facing north (the lid is drawn by the client): a back, two sides, a gold plinth and a cap."""
    return [box((2, 0, 11), (14, 31, 14), "#case", textures={"north": "#inside"}),
            box((2, 0, 4.5), (3, 31, 11), "#case", textures={"east": "#inside"}),
            box((13, 0, 4.5), (14, 31, 11), "#case", textures={"west": "#inside"}),
            box((1.5, 0, 2), (14.5, 1, 14.5), "#gold"),
            box((2, 31, 4.5), (14, 32, 14), "#case")]


def sarcophagus_lid():
    return [box((2.25, 1, 2.5), (13.75, 31, 4.5), "#case", textures={"north": "#lid_face"}, uvs={"north": (0, 0, 16, 16)})]


def mummy_body():
    return [box((4.5, 1, 5.5), (11.5, 22.5, 10), "#wraps"),
            box((5.5, 22.5, 5.5), (10.5, 28.5, 10), "#wraps", textures={"north": "#mummy_face"}, uvs={"north": (0, 0, 16, 16)})]


def mummy_arms():
    return [box((3, 13, 6), (4.5, 22.5, 8.5), "#wraps"), box((11.5, 13, 6), (13, 22.5, 8.5), "#wraps")]


# ---------------------------------------------------------------- the raven on a perch

RAVEN_TEXTURES = {"wood": "raven_perch_wood", "feathers": "raven_feathers", "wing": "raven_wing", "beak": "raven_beak", "eye": "raven_eye"}


def perch_elements():
    wood = "#wood"
    return [box((5, 0, 5), (11, 1, 11), wood), box((7.25, 1, 7.25), (8.75, 10, 8.75), wood), box((3, 10, 7.25), (13, 11, 8.75), wood),
            box((2.5, 9.75, 7), (3, 11.25, 9), wood), box((13, 9.75, 7), (13.5, 11.25, 9), wood)]


def raven_body():
    """The raven facing north on the crossbar: body, chest, legs and a long tail sloping down behind."""
    tail = box((7, 12, 11), (9, 12.75, 15), "#feathers")
    tail["rotation"] = {"origin": [8, 12.75, 11], "axis": "x", "angle": -22.5}
    return [box((6.5, 11.5, 6), (9.5, 14.5, 11), "#feathers"), box((6.75, 11.75, 5.5), (9.25, 14, 6), "#feathers"), tail,
            box((7, 11, 7.75), (7.5, 11.5, 8.25), "#beak"), box((8.5, 11, 7.75), (9, 11.5, 8.25), "#beak")]


def raven_head():
    return [box((6.75, 14, 4.5), (9.25, 16.5, 7), "#feathers", textures={"east": "#eye", "west": "#eye"},
                uvs={"east": (0, 0, 16, 16), "west": (0, 0, 16, 16)}),
            box((7.5, 14.5, 3), (8.5, 15.25, 4.5), "#beak")]


def raven_wing(left):
    x0 = 6 if left else 9.5
    return [box((x0, 11.75, 6.5), (x0 + 0.5, 14.5, 11.5), "#wing")]


# ---------------------------------------------------------------- the black cat figure

CAT_TEXTURES = {"fur": "black_cat_fur", "face": "black_cat_face", "face_hiss": "black_cat_face_hiss"}


def cat_elements(hissing):
    """The cat facing north: sitting tall, or with its back arched and its head low (the tail is drawn by the client)."""
    fur = "#fur"
    if not hissing:
        return [box((5, 0, 7), (11, 6, 12.5), fur), box((5.5, 2, 4.5), (10.5, 8, 8), fur),
                box((5.75, 0, 4), (7.25, 3, 5.5), fur), box((8.75, 0, 4), (10.25, 3, 5.5), fur),
                box((5.5, 8, 4), (10.5, 12, 8), fur, textures={"north": "#face"}, uvs={"north": (0, 0, 16, 16)}),
                box((5.75, 12, 5), (7, 13.5, 6.5), fur), box((9, 12, 5), (10.25, 13.5, 6.5), fur)]
    return [box((5, 4, 5.5), (11, 9, 12.5), fur), box((5.5, 3, 4), (10.5, 7, 6), fur),
            box((5.5, 0, 5.5), (7, 4, 7), fur), box((9, 0, 5.5), (10.5, 4, 7), fur),
            box((5.5, 0, 11), (7, 4, 12.5), fur), box((9, 0, 11), (10.5, 4, 12.5), fur),
            box((5.5, 5, 2), (10.5, 9, 6), fur, textures={"north": "#face_hiss"}, uvs={"north": (0, 0, 16, 16)}),
            box((5.25, 8.5, 4), (7, 9.5, 5.5), fur), box((9, 8.5, 4), (10.75, 9.5, 5.5), fur)]


def cat_tail(up):
    if up:
        return [box((7, 9, 11.5), (9, 15.5, 13.5), "#fur"), box((7.5, 15.5, 12), (8.5, 16, 13), "#fur")]
    return [box((7.5, 0, 11.5), (8.5, 1, 14), "#fur"), box((8.5, 0, 13), (12, 1, 14), "#fur"), box((11, 0, 8), (12, 1, 13), "#fur")]


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"

    coil = TESLA_COIL["block"]
    for active in (False, True):
        suffix = "_active" if active else ""
        for half, cy in (("lower", 0), ("upper", 1)):
            write(models / f"{coil}_{half}{suffix}.json", block_model(COIL_TEXTURES, split(coil_elements(active), (0, cy, 0)), COIL_TEXTURES["iron"]))
    write(states / f"{coil}.json", {"variants": {
        f"active={str(a).lower()},enabled={str(e).lower()},facing={f},half={h}": {"model": rid(f"block/{coil}_{h}{'_active' if a else ''}")}
        for a in (False, True) for e in (False, True) for f in HORIZONTAL for h in ("lower", "upper")}})
    write(models / f"{coil}_item.json", fitted(block_model(COIL_TEXTURES, scaled(coil_elements(False), 0.5, (4, 0, 4)), COIL_TEXTURES["iron"])))
    write(root / "items" / f"{coil}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{coil}_item")}})
    lang[f"block.{MOD}.{coil}"] = TESLA_COIL["display"]
    lang[f"message.{MOD}.{coil}.on"] = "The Tesla Coil hums into life."
    lang[f"message.{MOD}.{coil}.off"] = "The Tesla Coil falls silent."

    table = LAB_TABLE["block"]
    whole = table_elements()
    for part, cz in (("foot", 0), ("head", -1)):
        write(models / f"{table}_{part}.json", block_model(TABLE_TEXTURES, split(whole, (0, 0, cz)), TABLE_TEXTURES["steel"]))
    write(states / f"{table}.json", {"variants": {f"facing={f},part={p},powered={str(pw).lower()}": turned(rid(f"block/{table}_{p}"), f)
                                                  for f in HORIZONTAL for p in ("foot", "head") for pw in (False, True)}})
    write(models / f"{table}_item.json", fitted(block_model(TABLE_TEXTURES, scaled(whole + patient_legs() + patient_torso(), 0.5, (4, 2, 8)),
                                                            TABLE_TEXTURES["steel"])))
    write(root / "items" / f"{table}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{table}_item")}})
    lang[f"block.{MOD}.{table}"] = LAB_TABLE["display"]

    jar = SPECIMEN_JAR["block"]
    write(models / f"{jar}.json", block_model(JAR_TEXTURES, jar_elements(), JAR_TEXTURES["glass"]))
    write(states / f"{jar}.json", {"variants": {f"specimen={s}": {"model": rid(f"block/{jar}")} for s in SPECIMEN_JAR["specimens"]}})
    for specimen in SPECIMEN_JAR["specimens"]:
        write(models / f"{jar}_{specimen}_item.json", block_model({**JAR_TEXTURES, **SPECIMEN_TEXTURES}, specimen_elements(specimen) + jar_elements(),
                                                                  JAR_TEXTURES["glass"]))
        lang[f"message.{MOD}.{jar}.{specimen}"] = {"eye": "An eye. It is watching you.", "tentacle": "A tentacle. It twitched.",
                                                    "pumpkin": "A tiny pumpkin, pickled.", "brain": "A brain. Someone was using that."}[specimen]
    first = SPECIMEN_JAR["specimens"][0]
    write(root / "items" / f"{jar}.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "specimen",
        "cases": [{"when": s, "model": {"type": "minecraft:model", "model": rid(f"block/{jar}_{s}_item")}}
                  for s in SPECIMEN_JAR["specimens"] if s != first],
        "fallback": {"type": "minecraft:model", "model": rid(f"block/{jar}_{first}_item")}}})
    lang[f"block.{MOD}.{jar}"] = SPECIMEN_JAR["display"]

    sarcophagus = SARCOPHAGUS["block"]
    case = sarcophagus_case()
    for half, cy in (("lower", 0), ("upper", 1)):
        write(models / f"{sarcophagus}_{half}.json", block_model(SARCOPHAGUS_TEXTURES, split(case, (0, cy, 0)), SARCOPHAGUS_TEXTURES["case"]))
    write(states / f"{sarcophagus}.json", {"variants": {
        f"facing={f},half={h},open={str(o).lower()},powered={str(p).lower()}": turned(rid(f"block/{sarcophagus}_{h}"), f)
        for f in HORIZONTAL for h in ("lower", "upper") for o in (False, True) for p in (False, True)}})
    write(models / f"{sarcophagus}_item.json", fitted(block_model(SARCOPHAGUS_TEXTURES, scaled(case + sarcophagus_lid(), 0.5, (4, 0, 4)),
                                                                  SARCOPHAGUS_TEXTURES["case"])))
    write(root / "items" / f"{sarcophagus}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{sarcophagus}_item")}})
    lang[f"block.{MOD}.{sarcophagus}"] = SARCOPHAGUS["display"]

    raven = RAVEN["block"]
    write(models / f"{raven}.json", block_model(RAVEN_TEXTURES, perch_elements(), RAVEN_TEXTURES["wood"]))
    write(states / f"{raven}.json", {"variants": {f"facing={f}": turned(rid(f"block/{raven}"), f) for f in HORIZONTAL}})
    write(models / f"{raven}_item.json", fitted(block_model(RAVEN_TEXTURES, perch_elements() + raven_body() + raven_head() + raven_wing(True)
                                                            + raven_wing(False), RAVEN_TEXTURES["wood"])))
    write(root / "items" / f"{raven}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{raven}_item")}})
    lang[f"block.{MOD}.{raven}"] = RAVEN["display"]

    cat = BLACK_CAT["block"]
    write(models / f"{cat}.json", block_model(CAT_TEXTURES, cat_elements(False), CAT_TEXTURES["fur"]))
    write(models / f"{cat}_hissing.json", block_model(CAT_TEXTURES, cat_elements(True), CAT_TEXTURES["fur"]))
    write(states / f"{cat}.json", {"variants": {f"facing={f},hissing={str(h).lower()}": turned(rid(f"block/{cat}{'_hissing' if h else ''}"), f)
                                                for f in HORIZONTAL for h in (False, True)}})
    write(models / f"{cat}_item.json", block_model(CAT_TEXTURES, cat_elements(False) + cat_tail(False), CAT_TEXTURES["fur"]))
    write(root / "items" / f"{cat}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{cat}_item")}})
    lang[f"block.{MOD}.{cat}"] = BLACK_CAT["display"]

    quad_models = {"lab_table_legs": quads(patient_legs(), TABLE_TEXTURES), "lab_table_torso": quads(patient_torso(), TABLE_TEXTURES),
                   "mummy_sarcophagus_lid": quads(sarcophagus_lid(), SARCOPHAGUS_TEXTURES),
                   "mummy_sarcophagus_mummy": quads(mummy_body(), SARCOPHAGUS_TEXTURES),
                   "mummy_sarcophagus_arms": quads(mummy_arms(), SARCOPHAGUS_TEXTURES),
                   "raven_body": quads(raven_body(), RAVEN_TEXTURES), "raven_head": quads(raven_head(), RAVEN_TEXTURES),
                   "raven_left_wing": quads(raven_wing(True), RAVEN_TEXTURES), "raven_right_wing": quads(raven_wing(False), RAVEN_TEXTURES),
                   "black_cat_tail": quads(cat_tail(False), CAT_TEXTURES), "black_cat_tail_up": quads(cat_tail(True), CAT_TEXTURES)}
    for specimen in SPECIMEN_JAR["specimens"]:
        quad_models[f"specimen_{specimen}"] = quads(specimen_elements(specimen), SPECIMEN_TEXTURES)
    write(root / "decor8_quads.json", quad_models)


def loot(out, write):
    """Each drops itself once: the coil and the sarcophagus from their lower halves, the table from its foot; a jar
    keeps its specimen."""
    for block in (RAVEN["block"], BLACK_CAT["block"]):
        write(out / f"{block}.json", self_drop(block))
    for block in (TESLA_COIL["block"], SARCOPHAGUS["block"]):
        write(out / f"{block}.json", self_drop(block, match_block(block, half="lower")))
    table = LAB_TABLE["block"]
    write(out / f"{table}.json", self_drop(table, match_block(table, part="foot")))
    jar = SPECIMEN_JAR["block"]
    write(out / f"{jar}.json", {"type": "minecraft:block", "pools": [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": rid(jar),
                     "modifier": {"type": "minecraft:copy_state", "block": rid(jar), "properties": ["specimen"]}}],
        "rolls": 1}], "random_sequence": rid(f"blocks/{jar}")})


def tags(tags):
    for block in (TESLA_COIL["block"], LAB_TABLE["block"], SARCOPHAGUS["block"], BLACK_CAT["block"]):
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    tags.add("block", "minecraft:mineable/axe", rid(RAVEN["block"]))
