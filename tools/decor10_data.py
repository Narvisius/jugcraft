"""JSON resources for the tenth batch of Halloween decorations, lighting and glow, from tools/agriculture.py: the Black
Light and Glow Paint, the Witch Fire Brazier, the Shadow Puppet Lamp, the Mini Pumpkin Stack and the Floating Witch
Hat; their names, messages, loot and tags, and the quads the client draws the moving parts from
(assets/jugcraft/decor10_quads.json).

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files and the earlier
batches' (tools/decor_data.py to decor9_data.py). Everything is modelled facing north (its front toward -z); a
wall-mounted Black Light hangs on the south side of its block, facing north. Glow Paint has one model per kind of
surface (a wall, the floor, the ceiling), turned for each wall.
"""
from agriculture import BLACK_LIGHT, GLOW_PAINT, BRAZIER, SHADOW_LAMP, MINI_PUMPKINS, FLOATING_HAT
from decor_data import MOD, HORIZONTAL, rid, turned, box, block_model, self_drop
from decor3_data import fitted
from decor9_data import drawn, FULL

ALL_FACINGS = ("north", "east", "south", "west", "up", "down")


# ---------------------------------------------------------------- the black light and glow paint

LIGHT_TEXTURES = {"fixture": "black_light_fixture", "tube": "black_light_tube", "tube_off": "black_light_tube_off"}


def black_light(lit):
    """A black iron fixture on the wall behind it (south), and its tube, glowing violet while it is on."""
    tube = "#tube" if lit else "#tube_off"
    return [box((1, 9, 14.5), (15, 12, 16), "#fixture"), box((1, 9.5, 12.5), (2, 11.5, 14.5), "#fixture"),
            box((14, 9.5, 12.5), (15, 11.5, 14.5), "#fixture"), box((2, 10, 13), (14, 11, 14), tube, light=15 if lit else None)]


def paint_wall(design):
    """A design painted on the wall behind (south), facing north."""
    return [box((0, 0, 15.9), (16, 16, 16), f"#{design}", faces=("north",), uvs={"north": FULL})]


def paint_floor(design):
    return [box((0, 0, 0), (16, 0.1, 16), f"#{design}", faces=("up",), uvs={"up": FULL})]


def paint_ceiling(design):
    return [box((0, 15.9, 0), (16, 16, 16), f"#{design}", faces=("down",), uvs={"down": FULL})]


# ---------------------------------------------------------------- the witch fire brazier

BRAZIER_TEXTURES = {"iron": "witch_fire_brazier_iron", "coals": "witch_fire_brazier_coals", "coals_dark": "witch_fire_brazier_coals_dark"}


def brazier(lit):
    """A black iron bowl on four legs, full of coals (glowing while it burns); the flames are drawn by the client."""
    i = "#iron"
    elements = [box((x, 0, z), (x + 1.5, 8, z + 1.5), i) for x in (2.5, 12) for z in (2.5, 12)]
    elements += [box((3, 7, 3), (13, 8, 13), i), box((2, 8, 2), (14, 11, 3), i), box((2, 8, 13), (14, 11, 14), i),
                 box((2, 8, 3), (3, 11, 13), i), box((13, 8, 3), (14, 11, 13), i),
                 box((1.5, 10.5, 1.5), (14.5, 11.5, 2.5), i), box((1.5, 10.5, 13.5), (14.5, 11.5, 14.5), i),
                 box((3, 8, 3), (13, 10, 13), "#coals" if lit else "#coals_dark", light=15 if lit else None)]
    return elements


# ---------------------------------------------------------------- the shadow puppet lamp

LAMP_TEXTURES = {"wood": "shadow_puppet_lamp_wood", "candle": "shadow_puppet_lamp_candle", "brass": "shadow_puppet_lamp_brass"}
# The shade's panels, in turning order, each with a silhouette cut out of it.
LAMP_DESIGNS = ("bat", "cat", "witch")


def lamp():
    """A turned wooden base with a brass collar and a candle; the client draws the paper shade turning over them
    (client/ShadowPuppetLampRenderer.java), each panel framed in wood in its own texture."""
    return [box((3.5, 0, 3.5), (12.5, 1.5, 12.5), "#wood"), box((4.5, 1.5, 4.5), (11.5, 2.5, 11.5), "#brass"),
            box((7, 2.5, 7), (9, 6, 9), "#candle"), box((7.75, 6, 7.75), (8.25, 6.75, 8.25), "#wood")]


# ---------------------------------------------------------------- the mini pumpkin stack

PUMPKIN_TEXTURES = {"side": "mini_pumpkin_side", "top": "mini_pumpkin_top", "face": "mini_pumpkin_face", "face_off": "mini_pumpkin_face_off",
                    "stem": "mini_pumpkin_stem"}


def mini_pumpkin(x, y, z, size, lit):
    face = "#face" if lit else "#face_off"
    light = 13 if lit else None
    s = size
    return [box((x, y, z), (x + s, y + s * 0.8, z + s), "#side", textures={"north": face, "up": "#top"}, uvs={"north": FULL}, light=light),
            box((x + s / 2 - 0.5, y + s * 0.8, z + s / 2 - 0.5), (x + s / 2 + 0.5, y + s * 0.8 + 1.5, z + s / 2 + 0.5), "#stem")]


def pumpkin_stack(lit):
    """Two little jack o'lanterns side by side and a third on top, all grinning out to the north."""
    return mini_pumpkin(1.5, 0, 4, 6.5, lit) + mini_pumpkin(8, 0, 5, 6.5, lit) + mini_pumpkin(4.5, 5.2, 4.5, 6, lit)


# ---------------------------------------------------------------- the floating witch hat

HAT_TEXTURES = {"hat": "floating_witch_hat_felt", "band": "floating_witch_hat_band", "candle": "floating_witch_hat_candle",
                "flame": "floating_witch_hat_flame"}
HAT_LOW = 6.0


def hat(lit):
    """A pointed witch's hat with a buckled band, floating at its brim's height, a candle's glow inside it."""
    y = HAT_LOW
    elements = [box((2, y, 2), (14, y + 1, 14), "#hat"), box((4, y + 1, 4), (12, y + 4, 12), "#hat", textures={"north": "#band", "south": "#band",
                                                                                                               "east": "#band", "west": "#band"}),
                box((5, y + 4, 5), (11, y + 7, 11), "#hat"), box((6, y + 7, 6), (10, y + 9.5, 10), "#hat"),
                box((7, y + 9.5, 7.5), (9, y + 11, 9.5), "#hat"), box((7.5, y + 11, 8.5), (8.5, y + 12.5, 10.5), "#hat"),
                box((7, y - 2.5, 7), (9, y + 0.5, 9), "#candle")]
    if lit:
        elements.append(box((7.5, y - 4, 7.5), (8.5, y - 2.5, 8.5), "#flame", light=15))
    return elements


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"

    def item_model(name, model_name):
        write(items / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{model_name}")}})

    light = BLACK_LIGHT["block"]
    for lit in (False, True):
        write(models / f"{light}{'_on' if lit else ''}.json", block_model(LIGHT_TEXTURES, black_light(lit), LIGHT_TEXTURES["fixture"]))
    write(states / f"{light}.json", {"variants": {
        f"facing={f},lit={str(lit).lower()},powered={str(p).lower()}": turned(rid(f"block/{light}{'_on' if lit else ''}"), f)
        for f in HORIZONTAL for lit in (False, True) for p in (False, True)}})
    item_model(light, f"{light}_on")
    lang[f"block.{MOD}.{light}"] = BLACK_LIGHT["display"]

    paint = GLOW_PAINT["block"]
    variants = {}
    for design in GLOW_PAINT["designs"]:
        textures = {design: f"glow_paint_{design}"}
        for surface, elements in (("wall", paint_wall(design)), ("floor", paint_floor(design)), ("ceiling", paint_ceiling(design))):
            write(models / f"{paint}_{design}_{surface}.json", block_model(textures, elements, f"glow_paint_{design}"))
        for facing in ALL_FACINGS:
            surface = "floor" if facing == "up" else "ceiling" if facing == "down" else "wall"
            model = rid(f"block/{paint}_{design}_{surface}")
            variants[f"design={design},facing={facing}"] = turned(model, facing) if surface == "wall" else {"model": model}
    write(states / f"{paint}.json", {"variants": variants})
    write(root / "models" / "item" / f"{paint}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{paint}")}})
    write(items / f"{paint}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{paint}")}})
    lang[f"block.{MOD}.{paint}"] = GLOW_PAINT["display"]

    brazier_block = BRAZIER["block"]
    for lit in (False, True):
        write(models / f"{brazier_block}{'_lit' if lit else ''}.json", block_model(BRAZIER_TEXTURES, brazier(lit), BRAZIER_TEXTURES["iron"]))
    write(states / f"{brazier_block}.json", {"variants": {
        f"flame={c},lit={str(lit).lower()}": {"model": rid(f"block/{brazier_block}{'_lit' if lit else ''}")}
        for c in BRAZIER["flames"] for lit in (False, True)}})
    item_model(brazier_block, f"{brazier_block}_lit")
    lang[f"block.{MOD}.{brazier_block}"] = BRAZIER["display"]

    lamp_block = SHADOW_LAMP["block"]
    write(models / f"{lamp_block}.json", block_model(LAMP_TEXTURES, lamp(), LAMP_TEXTURES["wood"]))
    write(states / f"{lamp_block}.json", {"variants": {f"lit={str(lit).lower()}": {"model": rid(f"block/{lamp_block}")} for lit in (False, True)}})
    write(models / f"{lamp_block}_item.json", block_model({**LAMP_TEXTURES, "paper": "shadow_puppet_lamp_paper_bat"},
                                                          lamp() + [box((4.5, 2.5, 4.5), (11.5, 11, 11.5), "#paper", uvs={s: FULL for s in
                                                                                                                    ("north", "south", "east", "west")})],
                                                          LAMP_TEXTURES["wood"]))
    item_model(lamp_block, f"{lamp_block}_item")
    lang[f"block.{MOD}.{lamp_block}"] = SHADOW_LAMP["display"]

    pumpkins = MINI_PUMPKINS["block"]
    for lit in (False, True):
        write(models / f"{pumpkins}{'_lit' if lit else ''}.json", block_model(PUMPKIN_TEXTURES, pumpkin_stack(lit), PUMPKIN_TEXTURES["side"]))
    write(states / f"{pumpkins}.json", {"variants": {f"facing={f},lit={str(lit).lower()}": turned(rid(f"block/{pumpkins}{'_lit' if lit else ''}"), f)
                                                     for f in HORIZONTAL for lit in (False, True)}})
    item_model(pumpkins, f"{pumpkins}_lit")
    lang[f"block.{MOD}.{pumpkins}"] = MINI_PUMPKINS["display"]

    hat_block = FLOATING_HAT["block"]
    write(models / f"{hat_block}.json", block_model(HAT_TEXTURES, [], HAT_TEXTURES["hat"]))
    write(states / f"{hat_block}.json", {"variants": {f"lit={str(lit).lower()}": {"model": rid(f"block/{hat_block}")} for lit in (False, True)}})
    write(models / f"{hat_block}_item.json", fitted(block_model(HAT_TEXTURES, hat(True), HAT_TEXTURES["hat"])))
    item_model(hat_block, f"{hat_block}_item")
    lang[f"block.{MOD}.{hat_block}"] = FLOATING_HAT["display"]

    quad_models = {"floating_witch_hat": drawn(hat(False), HAT_TEXTURES), "floating_witch_hat_flame": drawn(
        [box((7.5, HAT_LOW - 4, 7.5), (8.5, HAT_LOW - 2.5, 8.5), "#flame")], HAT_TEXTURES)}
    write(root / "decor10_quads.json", quad_models)


def loot(out, write):
    """Each drops itself once."""
    for block in (BLACK_LIGHT["block"], GLOW_PAINT["block"], BRAZIER["block"], SHADOW_LAMP["block"], MINI_PUMPKINS["block"], FLOATING_HAT["block"]):
        write(out / f"{block}.json", self_drop(block))


def tags(tags):
    for block in (BLACK_LIGHT["block"], BRAZIER["block"]):
        tags.add("block", "minecraft:mineable/pickaxe", rid(block))
    tags.add("block", "minecraft:mineable/axe", rid(SHADOW_LAMP["block"]))
