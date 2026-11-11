"""The realistic building exterior's game data (tools/exterior_kit.py draws the pieces; Justin, 2026-10-17: "realism
max"): the blocks' models, blockstates, items, names, loot, recipes, tags and textures. Java:
io.github.jimbozoomer.jugcraft.exterior (the blocks; a wall pack lights at night, a smoke vent opens when the fire
system trips, a stair section places and breaks whole and puts its guard rails' collision over its open sides) and
client/exterior (the stairs' stringers and handrails, drawn at their 33.7 degrees from the hidden exterior_part's
models, as block models only turn by 22.5 or 45).

Every piece is drawn with its outer face to +z; a block facing that way is turned so: south 0, west 90, north 180,
east 270. A stair climbs toward -z (facing north it climbs north: north 0, east 90, south 180, west 270)."""
import exterior_kit as E
import plant_interior as PI
import warehouse as W

MOD = "jugcraft"
GLOW = [E.LENS_ON]
FACE_Y = (("south", 0), ("west", 90), ("north", 180), ("east", 270))
UP_Y = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

TONES = ("silver", "anthracite", "trapezoid")
PANEL_SHAPES = ("plain", "joint", "column", "pipe", "light", "corner")
PLINTH_SHAPES = ("plain", "joint", "pipe", "inlet", "corner")
PARAPET_SHAPES = ("plain", "scupper", "corner")
CANOPY_PARTS = ("single", "left", "mid", "right")

BLOCKS = {
    "wall_panel": "Insulated Wall Panel",
    "wall_plinth": "Precast Plinth Panel",
    "parapet": "Parapet with Coping",
    "roof_deck": "Membrane Roof Deck",
    "rooflight": "Flat Rooflight",
    "smoke_vent": "Smoke Vent",
    "roof_hatch": "Roof Hatch",
    "door_canopy": "Door Canopy",
    "yard_concrete": "Yard Concrete",
    "steel_stair": "Steel Stair Section",
    "stair_landing": "Steel Stair Landing",
}
# Items placing a wall panel of another tone (the block state component on the item).
TONE_ITEMS = {"wall_panel_anthracite": ("Insulated Wall Panel (Anthracite)", "anthracite"),
              "wall_panel_trapezoid": ("Trapezoidal Wall Sheet", "trapezoid")}
HIDDEN = {"stair_guard": "Stair Guard Rail", "exterior_part": "Exterior Part"}
ITEMS = dict(TONE_ITEMS)

# The stair's sloped parts (the hidden exterior_part block's PART values), drawn by client/exterior/StairRenderer.java
# sheared up the slope: each runs along -z from 0 to -16 (stretched to its length), its y from the pitch line through
# the nosings (posts and brackets stand straight). l / r: the cell's left or right side.
STRINGER = (0.25, 1.15)                 # the stringer plate's x on the left side (the right: 16 - these)
RAIL_WALL, RAIL_GUARD = 2.6, 0.7        # a handrail's x off the side: on brackets off a wall, or over a guard's posts


def _mirror(boxes):
    return [W.box(16.0 - b[3], b[1], b[2], 16.0 - b[0], b[4], b[5], b[6]) for b in boxes]


def _stringer():
    x0, x1 = STRINGER
    return [W.box(x0, -3.2, -16.0, x1, 1.6, 0.0, E.RAIL_PAINT),
            W.box(x1, 1.1, -16.0, x1 + 1.2, 1.6, 0.0, E.RAIL_PAINT), W.box(x1, -3.2, -16.0, x1 + 1.2, -2.7, 0.0, E.RAIL_PAINT)]


def _tube(x, a, tex):
    return W.round_z(x, 0.0, a, -16.0, 0.0, tex)


def _post(x):
    return W.round_y(x, 0.0, 0.5, 0.0, 16.0, E.RAIL_PAINT)


def _bracket():
    """A wall bracket under the handrail: the wall plate, the arm out to the rail, the saddle."""
    return [W.box(0.0, -1.6, -0.9, 0.4, 0.4, 0.9, E.RAIL_PAINT), W.box(0.4, -1.3, -0.3, RAIL_WALL, -0.7, 0.3, E.RAIL_PAINT),
            W.box(RAIL_WALL - 0.3, -0.7, -0.3, RAIL_WALL + 0.3, -0.45, 0.3, E.RAIL_PAINT)]


PARTS = {
    "stringer_l": _stringer, "stringer_r": lambda: _mirror(_stringer()),
    "rail_wall_l": lambda: _tube(RAIL_WALL, 0.62, E.RAIL_YELLOW), "rail_wall_r": lambda: _tube(16.0 - RAIL_WALL, 0.62, E.RAIL_YELLOW),
    "rail_guard_l": lambda: _tube(RAIL_GUARD, 0.62, E.RAIL_YELLOW), "rail_guard_r": lambda: _tube(16.0 - RAIL_GUARD, 0.62, E.RAIL_YELLOW),
    "knee_l": lambda: _tube(RAIL_GUARD, 0.45, E.RAIL_PAINT), "knee_r": lambda: _tube(16.0 - RAIL_GUARD, 0.45, E.RAIL_PAINT),
    "post_l": lambda: _post(RAIL_GUARD), "post_r": lambda: _post(16.0 - RAIL_GUARD),
    "bracket_l": _bracket, "bracket_r": lambda: _mirror(_bracket()),
}
PART_NAMES = list(PARTS)
PART_STATES = 48              # the hidden block's PART values (warehouse HiddenBlock.PART: 0..47)


def write_assets(write, rid, assets, lang):
    import zfight
    write = zfight.unfighting(write)
    models = assets / "models" / "block"
    states = assets / "blockstates"
    items = assets / "items"

    def model(name, boxes):
        m = PI.model(boxes, GLOW, rid)
        for key in list(m["textures"]):
            if key in E.EX_SEE_THROUGH:
                m["textures"][key] = {"sprite": m["textures"][key], "force_translucent": True}
        write(models / f"{name}.json", m)
        return rid(f"block/{name}")

    def item(name, model_id):
        write(items / f"{name}.json", {"model": {"type": "minecraft:model", "model": model_id}})

    def v(variants, key, ref, y):
        variants[key] = {"model": ref, **({"y": y} if y else {})}

    for name, display in BLOCKS.items():
        lang[f"block.{MOD}.{name}"] = display
    # Wall panels: tone, shape, lit (only a wall pack's lens differs).
    variants = {}
    for tone in TONES:
        for shape in PANEL_SHAPES:
            for lit in (False, True):
                if lit and shape != "light":
                    ref = rid(f"block/wall_panel_{tone}_{shape}_0")
                else:
                    ref = model(f"wall_panel_{tone}_{shape}_{int(lit)}", E.wall_panel(tone, shape, lit))
                for face, y in FACE_Y:
                    v(variants, f"facing={face},lit={str(lit).lower()},shape={shape},tone={tone}", ref, y)
    write(states / "wall_panel.json", {"variants": variants})
    item("wall_panel", rid("block/wall_panel_silver_plain_0"))
    for name, (display, tone) in TONE_ITEMS.items():
        item(name, rid(f"block/wall_panel_{tone}_plain_0"))
        lang[f"item.{MOD}.{name}"] = display
    variants = {}
    for row in ("lower", "upper"):
        for shape in PLINTH_SHAPES:
            ref = model(f"wall_plinth_{row}_{shape}", E.wall_plinth(row, shape))
            for face, y in FACE_Y:
                v(variants, f"facing={face},row={row},shape={shape}", ref, y)
    write(states / "wall_plinth.json", {"variants": variants})
    item("wall_plinth", rid("block/wall_plinth_upper_plain"))
    variants = {}
    for shape in PARAPET_SHAPES:
        ref = model(f"parapet_{shape}", E.parapet(shape))
        for face, y in FACE_Y:
            v(variants, f"facing={face},shape={shape}", ref, y)
    write(states / "parapet.json", {"variants": variants})
    item("parapet", rid("block/parapet_plain"))
    variants = {}
    for g in (False, True):
        ref = model(f"roof_deck_{int(g)}", E.roof_deck(g))
        v(variants, f"axis=x,girder={str(g).lower()}", ref, 0)
        v(variants, f"axis=z,girder={str(g).lower()}", ref, 90)
    write(states / "roof_deck.json", {"variants": variants})
    item("roof_deck", rid("block/roof_deck_0"))
    write(states / "rooflight.json", {"variants": {"": {"model": model("rooflight", E.rooflight())}}})
    item("rooflight", rid("block/rooflight"))
    write(states / "smoke_vent.json", {"variants": {"open=false": {"model": model("smoke_vent_0", E.smoke_vent(False))},
                                                     "open=true": {"model": model("smoke_vent_1", E.smoke_vent(True))}}})
    item("smoke_vent", rid("block/smoke_vent_0"))
    write(states / "roof_hatch.json", {"variants": {"open=false": {"model": model("roof_hatch_0", E.roof_hatch(False))},
                                                     "open=true": {"model": model("roof_hatch_1", E.roof_hatch(True))}}})
    item("roof_hatch", rid("block/roof_hatch_0"))
    variants = {}
    for part in CANOPY_PARTS:
        ref = model(f"door_canopy_{part}", E.door_canopy(part))
        for face, y in FACE_Y:
            v(variants, f"facing={face},part={part}", ref, y)
    write(states / "door_canopy.json", {"variants": variants})
    item("door_canopy", rid("block/door_canopy_single"))
    write(states / "yard_concrete.json", {"variants": {"": {"model": model("yard_concrete", E.yard())}}})
    item("yard_concrete", rid("block/yard_concrete"))
    variants = {}
    for part in range(4):
        for side in ("left", "right"):
            ref = model(f"steel_stair_{part}_{side}", E.stair_cell(part, side))
            for face, y in UP_Y:
                v(variants, f"facing={face},part={part},side={side}", ref, y)
    write(states / "steel_stair.json", {"variants": variants})
    write(assets / "models" / "item" / "steel_stair.json", _stair_item(rid))
    item("steel_stair", rid("item/steel_stair"))
    variants = {}
    for edge in (False, True):
        ref = model(f"stair_landing_{int(edge)}", E.stair_landing(edge))
        for face, y in FACE_Y:
            v(variants, f"edge={str(edge).lower()},facing={face}", ref, y)
    write(states / "stair_landing.json", {"variants": variants})
    item("stair_landing", rid("block/stair_landing_1"))
    # The hidden blocks: the guard rails' collision (nothing to draw), the stairs' sloped parts.
    empty = model("stair_guard", [W.box(7.95, 0.0, 7.95, 8.05, 0.05, 8.05, E.RAIL_PAINT)])
    write(states / "stair_guard.json", {"variants": {f"facing={f}": {"model": empty} for f, _ in UP_Y}})
    refs = [model(f"exterior_part_{n}", PARTS[n]()) for n in PART_NAMES]
    write(states / "exterior_part.json", {"variants": {f"part={i}": {"model": refs[min(i, len(refs) - 1)]} for i in range(PART_STATES)}})
    for name, display in HIDDEN.items():
        lang[f"block.{MOD}.{name}"] = display
    lang.update(LANG)


def _stair_item(rid):
    """The stair section's item: its eight cells (two wide, three along, two up) as they stand."""
    import warehouse_data as WD
    groups = []
    for part, (c, lv) in enumerate(E.STAIR_CELLS):
        for s, side in enumerate(("left", "right")):
            groups.append((E.stair_cell(part, side), (16.0 * s, 16.0 * lv, -16.0 * c)))
    return WD._item_model(groups, rid, gui=0.7)


LANG = {
    "tooltip.jugcraft.exterior.wall_panel": "Sneak and use with an empty hand to change its shape: joint cover, column, downpipe, wall light, corner.",
    "tooltip.jugcraft.exterior.wall_plinth": "Precast concrete for the foot of a wall (two rows). Sneak and use to change its shape.",
    "tooltip.jugcraft.exterior.parapet": "The top of a wall over a flat roof, with its coping. Sneak and use to change its shape.",
    "tooltip.jugcraft.exterior.steel_stair": "Places a stair section two blocks wide, three along and two up, climbing away from you. "
                                             "Place another at its top to carry on.",
    "tooltip.jugcraft.exterior.smoke_vent": "Opens when the building's fire system trips (use it to open or shut it by hand).",
    "tooltip.jugcraft.exterior.rooflight": "Lets daylight into the floor under the roof.",
    "message.jugcraft.exterior.shape": "Shape: %s",
    "message.jugcraft.exterior.stair_blocked": "No room for the stair section here",
}

CRAFTING = {
    "wall_panel": (["III", "WDW", "III"], {"I": "#c:ingots/iron", "W": "minecraft:white_wool", "D": "minecraft:white_dye"}, 16),
    "wall_panel_anthracite": (["III", "WDW", "III"], {"I": "#c:ingots/iron", "W": "minecraft:white_wool", "D": "minecraft:gray_dye"}, 16),
    "wall_panel_trapezoid": (["I I", "IDI", "I I"], {"I": "#c:ingots/iron", "D": "minecraft:gray_dye"}, 8),
    "wall_plinth": (["SSS", "SIS", "SSS"], {"S": "minecraft:smooth_stone", "I": "#c:ingots/iron"}, 8),
    "parapet": (["III", "DWD", "WWW"], {"I": "#c:ingots/iron", "W": "minecraft:white_wool", "D": "minecraft:gray_dye"}, 8),
    "roof_deck": (["WWW", "III"], {"W": "minecraft:light_gray_wool", "I": "#c:ingots/iron"}, 8),
    "rooflight": (["GGG", "IGI"], {"G": "minecraft:glass_pane", "I": "#c:ingots/iron"}, 2),
    "smoke_vent": (["GPG", "IRI"], {"G": "minecraft:glass_pane", "P": "minecraft:piston", "I": "#c:ingots/iron", "R": "minecraft:redstone"}, 1),
    "roof_hatch": (["ITI"], {"I": "#c:ingots/iron", "T": "minecraft:iron_trapdoor"}, 1),
    "door_canopy": (["III", "N N"], {"I": "#c:ingots/iron", "N": "minecraft:iron_nugget"}, 4),
    "yard_concrete": (["SSS", "SGS", "SSS"], {"S": "minecraft:smooth_stone", "G": "minecraft:gravel"}, 8),
    "steel_stair": (["Y  ", "II ", "III"], {"Y": "minecraft:yellow_dye", "I": "#c:ingots/iron"}, 1),
    "stair_landing": (["BBB", "I I"], {"B": "minecraft:iron_bars", "I": "#c:ingots/iron"}, 4),
}
FEATURE = "machines"


def recipe_features(result, feature_of):
    return set()


def write_loot(write, rid, out, self_drop):
    """A wall panel drops the item of its tone; the hidden blocks drop nothing (the others drop themselves)."""
    entries = []
    for tone, item_name in (("anthracite", "wall_panel_anthracite"), ("trapezoid", "wall_panel_trapezoid"), ("silver", "wall_panel")):
        # (26.3's loot: one "condition" each; the block's state is matched with match_block.)
        entries.append({"type": "minecraft:item", "name": rid(item_name),
                        "condition": {"type": "minecraft:match_block", "blocks": rid("wall_panel"), "state": {"tone": tone}}})
    write(out / "wall_panel.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": entries}],
                                                                           "condition": {"type": "minecraft:survives_explosion"}}],
                                    "random_sequence": rid("blocks/wall_panel")})
    for name in HIDDEN:
        write(out / f"{name}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{name}")})


def write_tags(tags, rid):
    tags.add("block", "minecraft:climbable", rid("roof_hatch"))
    for name in BLOCKS:
        tags.add("block", "minecraft:mineable/pickaxe", rid(name))


def draw_block_textures():
    out = {}
    from PIL import Image
    for name, make in E.ex_makers().items():
        img = make()
        if img.size[0] > 128 or img.size[1] > 128:
            img = img.convert("RGBA").resize((128, 128), Image.LANCZOS)
        out[name] = img
    return out
