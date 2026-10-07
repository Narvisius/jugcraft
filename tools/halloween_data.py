"""Writes the JSON resources of the Agriculture branch's Halloween harvest from tools/agriculture.py: the giant
pumpkin, scooping, the Harvest Scale and ribbons, stencils, heirloom pumpkins carved by hand, the scarecrow,
corn shocks and ornamental corn bundles, the gourd birdhouse and canteen, and mums.

Called from agriculture_data.py. Formats follow vanilla Minecraft 26.3's own files, read from the game jar:
flowers and potted flowers (cross and flower_pot_cross models, a pot that drops its plant), tall flowers
(drops from the lower half only), lanterns (standing and hanging) and slabs. No vanilla model or texture is
copied; vanilla's templates are used by reference where a shape is the same (cross, flower pot, cube column).
"""
from agriculture import (GIANT_PUMPKIN, SCOOP, HARVEST_SCALE, STENCILS, CANTEEN, HALLOWEEN_DECOR, SCARECROW_SHIRT, DYE_COLORS,
                         MUMS, MUM_PATCH, CARVED_VARIETIES, CARVE_SEEDS, GOURDS, SCARECROW_HEADS, potted, giant_tile)

MOD = "jugcraft"
HORIZONTAL = ("north", "east", "south", "west")
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def tex(name):
    return rid(f"block/{name}")


def turned(model, facing, **extra):
    """A blockstate variant: a model drawn facing north, turned to `facing`."""
    variant = {"model": model}
    if FACING_Y[facing]:
        variant["y"] = FACING_Y[facing]
    variant.update(extra)
    return variant


def face(texture, uv=None, cull=None):
    out = {"texture": texture}
    if uv:
        out["uv"] = uv
    if cull:
        out["cullface"] = cull
    return out


def box(lo, hi, faces):
    return {"from": list(lo), "to": list(hi), "faces": faces}


def all_faces(texture, top=None, bottom=None, skip=()):
    out = {side: face(texture) for side in ("north", "south", "east", "west")}
    out["up"] = face(top or texture)
    out["down"] = face(bottom or top or texture)
    return {k: v for k, v in out.items() if k not in skip}


# ---------------------------------------------------------------- the giant pumpkin

def giant_part_model(size, dx, dy, dz):
    """One block of a giant pumpkin `size` blocks wide: a cube showing only the faces on the outside of the
    whole pumpkin, each with its tile of the big side or top picture. Seen from outside, a side's columns run
    left to right: x falls going right on the north side, rises on the south side; z rises going right on
    the west side and falls on the east side."""
    last = size - 1
    row = last - dy
    faces = {}
    if dz == 0:
        faces["north"] = face("#n")
    if dz == last:
        faces["south"] = face("#s")
    if dx == 0:
        faces["west"] = face("#w")
    if dx == last:
        faces["east"] = face("#e")
    if dy == last:
        faces["up"] = face("#up")
    if dy == 0:
        faces["down"] = face("#down")
    textures = {"particle": tex(giant_tile("side", size, 0, 0)),
                "n": tex(giant_tile("side", size, last - dx, row)), "s": tex(giant_tile("side", size, dx, row)),
                "w": tex(giant_tile("side", size, dz, row)), "e": tex(giant_tile("side", size, last - dz, row)),
                "up": tex(giant_tile("top", size, dx, dz)), "down": tex("giant_pumpkin_bottom")}
    used = {key for key in textures if key == "particle" or any(f["texture"] == f"#{key}" for f in faces.values())}
    model = {"parent": "minecraft:block/block", "textures": {k: v for k, v in textures.items() if k in used}}
    if faces:
        # The middle block of a 3x3x3 pumpkin shows nothing; Minecraft refuses an element without faces.
        model["elements"] = [box((0, 0, 0), (16, 16, 16), faces)]
    return model


def giant_assets(root, write, lang):
    models = root / "models" / "block"
    block, max_size = GIANT_PUMPKIN["block"], GIANT_PUMPKIN["max_size"]
    variants = {}
    for size in range(1, max_size + 1):
        for part in range(max_size ** 3):
            dx, dy, dz = part % size, part // (size * size), (part // size) % size
            if size == 1 or part >= size ** 3:
                # A seedling-sized giant is a plain pumpkin (vanilla's model, so resource packs apply);
                # parts a size cannot have never exist and look the same.
                model = "minecraft:block/pumpkin"
            else:
                name = f"{block}_{size}_{part}"
                write(models / f"{name}.json", giant_part_model(size, dx, dy, dz))
                model = rid(f"block/{name}")
            variants[f"part={part},size={size}"] = {"model": model}
    write(root / "blockstates" / f"{block}.json", {"variants": variants})
    lang[f"block.{MOD}.{block}"] = GIANT_PUMPKIN["display"]
    # The whole pumpkin as an item (picked up by breaking it): its own icon.
    item_icon(write, root, block)
    # The vine looks like a gourd stem (the same models); its attached form bends towards the fruit.
    write(root / "blockstates" / f"{GIANT_PUMPKIN['vine']}.json", {"variants": {
        f"age={age}": {"model": rid(f"block/gourd_stem_stage{age}")} for age in range(8)}})
    # The attached stem model bends west, so it turns by a quarter less than a model drawn facing north.
    attached = {}
    for facing, y in (("west", 0), ("north", 90), ("east", 180), ("south", 270)):
        attached[f"facing={facing}"] = {"model": rid("block/gourd_stem_attached"), "y": y} if y else {"model": rid("block/gourd_stem_attached")}
    write(root / "blockstates" / f"{GIANT_PUMPKIN['attached_vine']}.json", {"variants": attached})
    lang[f"block.{MOD}.{GIANT_PUMPKIN['vine']}"] = "Giant Pumpkin Vine"
    lang[f"block.{MOD}.{GIANT_PUMPKIN['attached_vine']}"] = "Attached Giant Pumpkin Vine"


# ---------------------------------------------------------------- decorations

def harvest_scale_model():
    """A wooden platform with an iron rim and a dial on a post at the back (the dial faces north)."""
    platform = box((0, 0, 0), (16, 4, 16), {
        **{side: face("#side", [0, 12, 16, 16]) for side in ("north", "south", "east", "west")},
        "up": face("#top"), "down": face("#top", cull="down")})
    post = box((5, 4, 12), (11, 14, 15), {
        "north": face("#dial", [5, 2, 11, 12]), "south": face("#side", [5, 2, 11, 12]),
        "east": face("#side", [1, 2, 4, 12]), "west": face("#side", [1, 2, 4, 12]), "up": face("#side", [5, 1, 11, 4])})
    return {"parent": "minecraft:block/block", "textures": {
        "particle": tex("harvest_scale_side"), "side": tex("harvest_scale_side"), "top": tex("harvest_scale_top"),
        "dial": tex("harvest_scale_dial")}, "elements": [platform, post]}


def scarecrow_lower_model():
    """The post with straw-stuffed trousers hanging from the body above (drawn facing north)."""
    post = box((7, 0, 7), (9, 16, 9), all_faces("#post", skip=("up",)))
    legs = []
    for x0 in (4.5, 8.5):
        leg = box((x0, 3, 6.5), (x0 + 3, 16, 9.5), all_faces("#trousers", skip=("up",)))
        leg["faces"]["down"] = face("#straw")
        legs.append(leg)
    return {"parent": "minecraft:block/block", "textures": {"particle": tex("scarecrow_trousers"), "post": tex("scarecrow_post"),
                                                           "trousers": tex("scarecrow_trousers"), "straw": tex("scarecrow_straw")},
            "elements": [post] + legs}


def scarecrow_upper_model(color):
    """A shirt stuffed with straw on the post's cross-bar, arms out, straw at the cuffs, and the post standing
    up as a neck for whatever head is put on top."""
    shirt = f"scarecrow_shirt_{color}"
    body = box((4, 0, 6), (12, 11, 10), all_faces("#shirt"))
    arms = [box((0, 7, 6.5), (4, 10, 9.5), all_faces("#shirt")), box((12, 7, 6.5), (16, 10, 9.5), all_faces("#shirt"))]
    arms[0]["faces"]["west"] = face("#straw")
    arms[1]["faces"]["east"] = face("#straw")
    neck = box((7, 11, 7), (9, 16, 9), all_faces("#post", skip=("down",)))
    return {"parent": "minecraft:block/block", "textures": {"particle": tex(shirt), "shirt": tex(shirt), "post": tex("scarecrow_post"),
                                                           "straw": tex("scarecrow_straw")},
            "elements": [body] + arms + [neck]}


def stook_model(texture):
    """Four planes of stalks standing round the middle, like a cross with two more planes square to it."""
    planes = []
    for frm, to, faces, angle in (([0.8, 0, 8], [15.2, 16, 8], ("north", "south"), 45), ([8, 0, 0.8], [8, 16, 15.2], ("west", "east"), 45),
                                  ([1, 0, 8], [15, 16, 8], ("north", "south"), 0), ([8, 0, 1], [8, 16, 15], ("west", "east"), 0)):
        plane = {"from": frm, "to": to, "faces": {f: face("#stalks", [0, 0, 16, 16]) for f in faces}}
        if angle:
            plane["rotation"] = {"origin": [8, 8, 8], "axis": "y", "angle": angle, "rescale": True}
        planes.append(plane)
    return {"ambientocclusion": False, "textures": {"particle": tex(texture), "stalks": tex(texture)}, "elements": planes}


def corn_bundle_model():
    """Three ears tied in a bunch, flat against the wall to the south (it faces north)."""
    return {"ambientocclusion": False, "textures": {"particle": tex("ornamental_corn_bundle"), "bundle": tex("ornamental_corn_bundle")},
            "elements": [box((0, 0, 14.5), (16, 16, 14.5), {"north": face("#bundle", [0, 0, 16, 16]),
                                                             "south": face("#bundle", [16, 0, 0, 16])})]}


def birdhouse_model(hanging):
    """A dried bottle gourd with a round door on its north side; hanging, it is raised on a string."""
    lift = 4 if hanging else 0
    bulb = box((4, lift, 4), (12, lift + 7, 12), {"north": face("#front", [4, 4, 12, 11]), "south": face("#side", [4, 4, 12, 11]),
                                                   "east": face("#side", [4, 4, 12, 11]), "west": face("#side", [4, 4, 12, 11]),
                                                   "up": face("#top", [4, 4, 12, 12]), "down": face("#top", [4, 4, 12, 12])})
    neck = box((6, lift + 7, 6), (10, lift + 10, 10), {side: face("#side", [6, 1, 10, 4]) for side in ("north", "south", "east", "west")}
               | {"up": face("#top", [6, 6, 10, 10])})
    elements = [bulb, neck]
    if hanging:
        for frm, to, faces in (([8, lift + 10, 6.5], [8, 16, 9.5], ("west", "east")), ([6.5, lift + 10, 8], [9.5, 16, 8], ("north", "south"))):
            elements.append({"from": frm, "to": to, "faces": {f: face("#string", [6.5, 0, 9.5, 16 - lift - 10]) for f in faces}})
    return {"parent": "minecraft:block/block", "textures": {
        "particle": tex("gourd_birdhouse_side"), "front": tex("gourd_birdhouse_front"), "side": tex("gourd_birdhouse_side"),
        "top": tex("gourd_birdhouse_top"), "string": tex("gourd_birdhouse_string")}, "elements": elements}


def item_icon(write, root, item, texture=None, parent="minecraft:item/generated"):
    write(root / "models" / "item" / f"{item}.json", {"parent": parent, "textures": {"layer0": texture or rid(f"item/{item}")}})
    write(root / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})


SCREEN_TEXT = {
    "screen.jugcraft.carving.stencil": "Stencil",
    "item.jugcraft.pumpkin_stencil.blank": "No design traced",
    "item.jugcraft.pumpkin_stencil.holes": "%s holes, %s shaved",
    "item.jugcraft.pumpkin_stencil.hint": "Hold in your other hand while carving",
    "item.jugcraft.gourd_canteen.water": "Water: %s/%s",
    "message.jugcraft.harvest_scale.no_pumpkin": "Put the scale beside a full-grown giant pumpkin, or set one down beside it.",
    "message.jugcraft.giant_pumpkin.no_room": "This giant pumpkin needs room to grow: clear the ground around it.",
    "message.jugcraft.giant_pumpkin_vine.no_room": "A giant pumpkin needs open ground beside its vine to set its fruit.",
    "item.jugcraft.giant_pumpkin.size": "%s × %s × %s blocks",
    "item.jugcraft.giant_pumpkin.weight": "%s kg",
    "item.jugcraft.giant_pumpkin.carved": "Carved",
    "item.jugcraft.giant_pumpkin.carved_lit": "Carved, with a torch inside",
    "message.jugcraft.harvest_scale.weight": "This giant pumpkin weighs %s kg.",
    "message.jugcraft.harvest_scale.ribbon": "It places on this scale's board: you win a %s!",
    "message.jugcraft.harvest_scale.board": "%s. %s: %s kg",
}


def assets(root, write, lang):
    models = root / "models" / "block"
    giant_assets(root, write, lang)

    scale = HARVEST_SCALE["block"]
    write(models / f"{scale}.json", harvest_scale_model())
    write(root / "blockstates" / f"{scale}.json", {"variants": {f"facing={f}": turned(rid(f"block/{scale}"), f) for f in HORIZONTAL}})
    write(root / "items" / f"{scale}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{scale}")}})
    lang[f"block.{MOD}.{scale}"] = HARVEST_SCALE["display"]
    for ribbon, display in HARVEST_SCALE["ribbons"].items():
        item_icon(write, root, ribbon)
        lang[f"item.{MOD}.{ribbon}"] = display
    for item, display in ((STENCILS["blank"], STENCILS["blank_display"]), (STENCILS["stencil"], STENCILS["stencil_display"]),
                          (CANTEEN["item"], CANTEEN["display"])):
        item_icon(write, root, item)
        lang[f"item.{MOD}.{item}"] = display
    lang.update(SCREEN_TEXT)

    # Hand-carved heirloom pumpkins look like the pumpkin they were carved from; the carving is drawn over it.
    for variety, carved in CARVED_VARIETIES.items():
        write(root / "blockstates" / f"{carved}.json", {"variants": {"": {"model": rid(f"block/{variety}")}}})
        write(root / "items" / f"{carved}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{variety}")}})
        lang[f"block.{MOD}.{carved}"] = f"Hand-Carved {GOURDS[variety]['display']}"

    # The scarecrow: one lower model, an upper model per shirt colour.
    write(models / "scarecrow_lower.json", scarecrow_lower_model())
    variants = {}
    for color in DYE_COLORS:
        write(models / f"scarecrow_upper_{color}.json", scarecrow_upper_model(color))
    for facing in HORIZONTAL:
        variants[f"facing={facing},half=lower"] = turned(rid("block/scarecrow_lower"), facing)
        for color in DYE_COLORS:
            variants[f"facing={facing},half=upper,shirt={color}"] = turned(rid(f"block/scarecrow_upper_{color}"), facing)
    write(root / "blockstates" / "scarecrow.json", {"variants": variants})

    write(models / "corn_shock_lower.json", stook_model("corn_shock_lower"))
    write(models / "corn_shock_upper.json", stook_model("corn_shock_upper"))
    write(root / "blockstates" / "corn_shock.json", {"variants": {
        f"facing={facing},half={half}": turned(rid(f"block/corn_shock_{half}"), facing) for facing in HORIZONTAL for half in ("lower", "upper")}})

    write(models / "ornamental_corn_bundle.json", corn_bundle_model())
    write(root / "blockstates" / "ornamental_corn_bundle.json", {"variants": {
        f"facing={facing}": turned(rid("block/ornamental_corn_bundle"), facing) for facing in HORIZONTAL}})

    write(models / "gourd_birdhouse.json", birdhouse_model(False))
    write(models / "gourd_birdhouse_hanging.json", birdhouse_model(True))
    write(root / "blockstates" / "gourd_birdhouse.json", {"variants": {
        "hanging=false": {"model": rid("block/gourd_birdhouse")}, "hanging=true": {"model": rid("block/gourd_birdhouse_hanging")}}})

    for block, display in HALLOWEEN_DECOR.items():
        if block == "ornamental_corn_bundle":
            item_icon(write, root, block, tex(block))
        else:
            item_icon(write, root, block)
        lang[f"block.{MOD}.{block}"] = display

    # Mums: vanilla's flower and potted-flower shapes, with our textures.
    for mum, info in MUMS.items():
        write(models / f"{mum}.json", {"parent": "minecraft:block/cross", "textures": {"cross": tex(mum)}})
        write(models / f"{potted(mum)}.json", {"parent": "minecraft:block/flower_pot_cross", "textures": {"plant": tex(mum)}})
        write(root / "blockstates" / f"{mum}.json", {"variants": {"": {"model": rid(f"block/{mum}")}}})
        write(root / "blockstates" / f"{potted(mum)}.json", {"variants": {"": {"model": rid(f"block/{potted(mum)}")}}})
        item_icon(write, root, mum, tex(mum))
        lang[f"block.{MOD}.{mum}"] = info["display"]
        lang[f"block.{MOD}.{potted(mum)}"] = f"Potted {info['display']}"


# ---------------------------------------------------------------- loot tables

def match_block(block, **state):
    return {"type": "minecraft:match_block", "blocks": rid(block), "state": {k: str(v) for k, v in state.items()}}


def item_entry(name, count=None, *more):
    entry = {"type": "minecraft:item", "name": rid(name)}
    modifiers = []
    if count is not None:
        modifiers.append({"type": "minecraft:set_count", "count": count})
    modifiers += list(more)
    if modifiers:
        entry["modifier"] = modifiers if len(modifiers) > 1 else modifiers[0]
    return entry


def self_drop(block, condition=None):
    term = {"type": "minecraft:survives_explosion"}
    if condition:
        term = {"type": "minecraft:all_of", "terms": [term, condition]}
    return {"type": "minecraft:block", "pools": [{"condition": term, "entries": [item_entry(block)], "rolls": 1}],
            "random_sequence": rid(f"blocks/{block}")}


def binomial(p):
    return {"type": "minecraft:binomial", "n": 3, "p": p}


def loot(out, write):
    """Block loot tables (out = loot_table/blocks) and the scooping and heirloom carving tables beside it."""
    tables = out.parent
    giant, seed = GIANT_PUMPKIN["block"], GIANT_PUMPKIN["seed"]
    # Breaking a giant pumpkin picks it up whole: its master block drops one Giant Pumpkin that keeps what it is
    # (the other blocks drop nothing; breaking one breaks the master).
    write(out / f"{giant}.json", {"type": "minecraft:block", "pools": [{
        "condition": match_block(giant, part=0),
        "entries": [item_entry(giant, None, {"type": "minecraft:copy_components", "include": [rid(giant)], "source": "block_entity"})],
        "rolls": 1}], "random_sequence": rid(f"blocks/{giant}")})
    # Chopping one up with an axe gives its pumpkins: one per block of a seedling (1, 4 or 9) and, full grown, seeds.
    pools = [{"condition": match_block(giant, size=size), "entries": [item_entry("minecraft:pumpkin", count if count > 1 else None)], "rolls": 1}
             for size, count in GIANT_PUMPKIN["drops"].items()]
    low, high = GIANT_PUMPKIN["seeds"]
    pools.append({"condition": match_block(giant, size=GIANT_PUMPKIN["max_size"]),
                  "entries": [item_entry(seed, {"type": "minecraft:uniform", "min": low, "max": high})], "rolls": 1})
    write(tables / f"{GIANT_PUMPKIN['chop_table']}.json", {"type": "minecraft:block", "pools": pools,
                                                          "random_sequence": rid(GIANT_PUMPKIN["chop_table"])})
    # The vine gives seeds back like a pumpkin stem: more the older it is.
    vine, attached = GIANT_PUMPKIN["vine"], GIANT_PUMPKIN["attached_vine"]
    write(out / f"{vine}.json", {"type": "minecraft:block", "pools": [{"entries": [{
        "type": "minecraft:item", "name": rid(seed), "modifier": [
            {"type": "minecraft:set_count", "count": binomial(round((age + 1) / 15, 8)), "condition": match_block(vine, age=age)}
            for age in range(8)]}], "modifier": {"type": "minecraft:explosion_decay"}, "rolls": 1}],
        "random_sequence": rid(f"blocks/{vine}")})
    write(out / f"{attached}.json", {"type": "minecraft:block", "pools": [{"entries": [{
        "type": "minecraft:item", "name": rid(seed), "modifier": {"type": "minecraft:set_count", "count": binomial(round(8 / 15, 8))}}],
        "modifier": {"type": "minecraft:explosion_decay"}, "rolls": 1}], "random_sequence": rid(f"blocks/{attached}")})

    write(out / f"{HARVEST_SCALE['block']}.json", self_drop(HARVEST_SCALE["block"]))
    for block in HALLOWEEN_DECOR:
        tall = block in ("scarecrow", "corn_shock")
        write(out / f"{block}.json", self_drop(block, match_block(block, half="lower") if tall else None))
    for mum in MUMS:
        write(out / f"{mum}.json", self_drop(mum))
        # Vanilla's potted flowers: the pot and the flower.
        write(out / f"{potted(mum)}.json", {"type": "minecraft:block", "pools": [
            {"condition": {"type": "minecraft:survives_explosion"}, "entries": [item_entry("minecraft:flower_pot")], "rolls": 1},
            {"condition": {"type": "minecraft:survives_explosion"}, "entries": [item_entry(mum)], "rolls": 1}],
            "random_sequence": rid(f"blocks/{potted(mum)}")})
    # Hand-carved heirloom pumpkins keep their design and their torch, like the hand-carved pumpkin.
    for carved in CARVED_VARIETIES.values():
        write(out / f"{carved}.json", {"type": "minecraft:block", "pools": [{
            "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{"type": "minecraft:item", "name": rid(carved), "modifier": [
                {"type": "minecraft:copy_components", "include": [rid("carving")], "source": "block_entity"},
                {"type": "minecraft:copy_state", "block": rid(carved), "properties": ["lit", "soul"]}]}],
            "rolls": 1}], "random_sequence": rid(f"blocks/{carved}")})

    # The first cut into a pumpkin: its seeds (vanilla's carve_pumpkin for a pumpkin, these for heirlooms)
    # and what is scooped out with them.
    for variety in CARVED_VARIETIES:
        write(tables / "carve" / f"{variety}.json", {"type": "minecraft:block_interact", "pools": [{
            "entries": [item_entry(GOURDS[variety]["seed"], CARVE_SEEDS)], "rolls": 1}],
            "random_sequence": rid(f"carve/{variety}")})
    guts_low, guts_high = SCOOP["guts"]
    write(tables / f"{SCOOP['table']}.json", {"type": "minecraft:block_interact", "pools": [
        {"entries": [item_entry("pumpkin_guts", {"type": "minecraft:uniform", "min": guts_low, "max": guts_high})], "rolls": 1},
        {"condition": {"type": "minecraft:random_chance", "chance": SCOOP["giant_seed_chance"]},
         "entries": [item_entry(seed)], "rolls": 1}],
        "random_sequence": rid(SCOOP["table"])})


# ---------------------------------------------------------------- tags

def tags(tags):
    giant = rid(GIANT_PUMPKIN["block"])
    tags.add("block", "minecraft:mineable/axe", giant)
    tags.add("block", "minecraft:sword_efficient", giant)
    for block in (GIANT_PUMPKIN["vine"], GIANT_PUMPKIN["attached_vine"]):
        tags.add("block", "minecraft:maintains_farmland", rid(block))
    tags.add("block", "minecraft:crops", rid(GIANT_PUMPKIN["vine"]))
    for carved in CARVED_VARIETIES.values():
        tags.add("block", "minecraft:mineable/axe", rid(carved))
    for block in (HARVEST_SCALE["block"], "scarecrow", "ornamental_corn_bundle", "gourd_birdhouse"):
        tags.add("block", "minecraft:mineable/axe", rid(block))
    # What a scarecrow wears for a head: any pumpkin, carved or not, lit or not.
    for head in (["minecraft:pumpkin", "minecraft:carved_pumpkin", "minecraft:jack_o_lantern", rid("hand_carved_pumpkin")]
                 + [rid(variety) for variety in CARVED_VARIETIES] + [rid(carved) for carved in CARVED_VARIETIES.values()]):
        tags.add("item", SCARECROW_HEADS, head)
    tags.add("block", "minecraft:mineable/hoe", rid("corn_shock"))
    tags.add("block", "minecraft:sword_efficient", rid("corn_shock"))
    for mum in MUMS:
        for registry in ("block", "item"):
            tags.add(registry, "minecraft:small_flowers", rid(mum))
        tags.add("block", "minecraft:bee_attractive", rid(mum))
        tags.add("block", "minecraft:flower_pots", rid(potted(mum)))


# ---------------------------------------------------------------- worldgen

def worldgen(data, write):
    """A patch mixing all four mums in flower-rich places (new chunks only), like vanilla's flower patches."""
    folder = data / MOD / "worldgen"
    write(folder / "feature" / "mums.json", {"type": "minecraft:simple_block", "to_place": {
        "type": "minecraft:weighted", "entries": [{"data": {"id": rid(mum)}, "weight": 1} for mum in MUMS]}})
    spread = MUM_PATCH["spread_xz"]
    write(folder / "placed_feature" / "patch_mums.json", {"feature": rid("mums"), "placement": [
        {"type": "minecraft:rarity_filter", "chance": MUM_PATCH["rarity"]},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:count", "count": MUM_PATCH["tries"]},
        {"type": "minecraft:offset",
         "x": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0},
         "y": {"type": "minecraft:trapezoid", "max": MUM_PATCH["spread_y"], "min": -MUM_PATCH["spread_y"], "plateau": 0},
         "z": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0}},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:grass_block", "offset": [0, -1, 0]}]}},
    ]})
