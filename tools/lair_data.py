"""JSON resources for the lairs (tools/lairs.py, docs/features/hollow-acre.md, docs/features/spindle-loft.md,
docs/features/glacier-hall.md, docs/features/cinder-kiln.md): the lair-only blocks, the Last Rites' Mourning Wreath and
Death Knell, the Cursed Spindle, the Frost Horn, the Kiln Seal, the Mist Gate's name, the lairs' messages, and each lair's
dimension (its dimension type, level stem and biome) and structure template (tools/hollow_acre.py, tools/spindle_loft.py,
tools/glacier_hall.py, tools/cinder_kiln.py).

Called from tools/agriculture_data.py (assets, loot, recipes, tags, worldgen). Formats follow vanilla 26.3's own files:
the dimension type copies the shape of vanilla's End (logged from the running game by LairGameTests), the level stem a
flat generator with no layers, and the biome vanilla's void with no spawns.
"""
from decor_data import MOD, HORIZONTAL, rid, self_drop
import cinder_kiln
import glacier_hall
import hollow_acre
import spindle_loft
from lairs import (FIXTURES, GATE, HORN_GROUND, ICICLE_PARTS, ITEMS, LACE_PATTERNS, LAIR_BLOCKS, LAIRS, MOURNING_FLOWERS,
                   RECIPES, RITE_CANDLES, SEAL_GROUND, SEAL_LANDS, SLUICE_FLOWS, SLUICE_PARTS, THREAD_COLOURS)

# Each lair's template, by the lair's id.
TEMPLATES = {"hollow_acre": hollow_acre, "spindle_loft": spindle_loft, "glacier_hall": glacier_hall,
             "cinder_kiln": cinder_kiln}


def cube(lo, hi, faces, texture, cull=None, shade=True, uvs=None):
    out = {"from": lo, "to": hi, "faces": {}}
    for face in faces:
        entry = {"texture": texture}
        if uvs and face in uvs:
            entry["uv"] = uvs[face]
        if cull and face in cull:
            entry["cullface"] = face
        out["faces"][face] = entry
    if not shade:
        out["shade"] = False
    return out


ALL = ("north", "south", "east", "west", "up", "down")


def brazier(lit):
    """A soul brazier: a squat black-stone pedestal, an iron bowl on it and, lit, a soul flame standing in the bowl."""
    bowl = cube([3, 8, 3], [13, 11, 13], ALL, "#iron")
    bowl["faces"]["up"]["texture"] = "#coals"  # the coals, seen inside the rim
    elements = [
        cube([5, 0, 5], [11, 2, 11], ALL, "#stone"),
        cube([6, 2, 6], [10, 8, 10], ALL, "#stone"),
        bowl,
        # The rim: four closed bars round the bowl's mouth.
        cube([2, 11, 2], [14, 12, 4], ALL, "#iron"),
        cube([2, 11, 12], [14, 12, 14], ALL, "#iron"),
        cube([2, 11, 4], [4, 12, 12], ALL, "#iron"),
        cube([12, 11, 4], [14, 12, 12], ALL, "#iron"),
    ]
    if lit:
        for angle in (45, -45):
            elements.append({"from": [3, 11, 8], "to": [13, 24, 8], "shade": False,
                             "rotation": {"origin": [8, 11, 8], "axis": "y", "angle": angle},
                             "faces": {"north": {"texture": "#flame", "uv": [0, 0, 16, 16]},
                                       "south": {"texture": "#flame", "uv": [0, 0, 16, 16]}}})
    textures = {"particle": rid("block/lair_brazier_stone"), "stone": rid("block/lair_brazier_stone"),
                "iron": rid("block/lair_brazier_iron"), "coals": rid("block/lair_brazier_coals")}
    if lit:
        textures["flame"] = rid("block/lair_brazier_flame")
    return {"parent": "minecraft:block/block", "textures": textures, "elements": elements}


def mist(axis):
    """Grey Mist: a curtain of mist across the middle of the block, see-through and without collision."""
    texture = {"force_translucent": True, "sprite": rid("block/lair_exit")}
    curtain = {"from": [0, 0, 7], "to": [16, 16, 9], "shade": False,
               "faces": {"north": {"texture": "#mist"}, "south": {"texture": "#mist"}}}
    return {"parent": "minecraft:block/block", "textures": {"particle": texture, "mist": texture}, "elements": [curtain]}


def wreath():
    """The Mourning Wreath lying flat: a ring of dark leaves and pale flowers on the ground, its ribbon trailing."""
    texture = rid("block/mourning_wreath")
    return {"parent": "minecraft:block/block", "textures": {"particle": texture, "wreath": texture},
            "elements": [{"from": [1, 0.25, 1], "to": [15, 1.25, 15], "faces": {
                "up": {"texture": "#wreath", "uv": [0, 0, 16, 16]},
                "down": {"texture": "#wreath", "uv": [0, 16, 16, 0]},
                "north": {"texture": "#wreath", "uv": [0, 7, 16, 8]}, "south": {"texture": "#wreath", "uv": [0, 8, 16, 9]},
                "east": {"texture": "#wreath", "uv": [7, 0, 8, 16]}, "west": {"texture": "#wreath", "uv": [8, 0, 9, 16]}}}]}


def lace(pattern):
    """Doily Lace: a two-pixel sheet of lace lying at the bottom of the block, cut out, seen from above and below."""
    texture = rid(f"block/doily_lace_{pattern}")
    edge = {"texture": "#lace", "uv": [0, 0, 16, 2]}
    return {"parent": "minecraft:block/block", "textures": {"particle": texture, "lace": texture},
            "elements": [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
                "up": {"texture": "#lace"}, "down": {"texture": "#lace"},
                "north": edge, "south": edge, "east": edge, "west": edge}}]}


def plate(texture, back, y0):
    """A one-pixel plate across the block at height y0 (pixels): the measuring tape, at the bottom or halfway up."""
    edge = {"texture": "#tape", "uv": [0, 0, 16, 1]}
    return {"parent": "minecraft:block/block", "textures": {"particle": texture, "tape": texture, "back": back},
            "elements": [{"from": [0, y0, 0], "to": [16, y0 + 1, 16], "faces": {
                "up": {"texture": "#tape"}, "down": {"texture": "#back"},
                "north": edge, "south": edge, "east": edge, "west": edge}}]}


def rod(texture, half):
    """A rod up the middle of the block, `half` pixels each side of its centre (a pin's shaft, a taut thread)."""
    lo, hi = 8 - half, 8 + half
    side = {"texture": "#rod", "uv": [lo, 0, hi, 16]}
    end = {"texture": "#rod", "uv": [lo, lo, hi, hi]}
    return {"parent": "minecraft:block/block", "textures": {"particle": texture, "rod": texture},
            "elements": [{"from": [lo, 0, lo], "to": [hi, 16, hi], "faces": {
                "north": side, "south": side, "east": side, "west": side, "up": end, "down": end}}]}


def spindle_loft_assets(models, states, write, simple):
    """The Spindle Loft's lair-only blocks (tools/spindle_loft.py)."""
    def cube(name, texture=None):
        return {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{texture or name}")}}

    for p in range(LACE_PATTERNS):
        write(models / f"doily_lace_{p}.json", lace(p))
    write(states / "doily_lace.json", {"variants": {
        f"pattern={p}": {"model": rid(f"block/doily_lace_{p}")} for p in range(LACE_PATTERNS)}})
    for name in ("spool_wood", "pincushion", "pincushion_seam", "pincushion_leaf", "needle_steel", "thimble_metal",
                 "grimy_skylight"):
        simple(name, cube(name))
    for colour in THREAD_COLOURS:
        write(models / f"spool_thread_{colour}.json", cube(f"spool_thread_{colour}"))
        write(models / f"taut_thread_{colour}.json", rod(rid(f"block/spool_thread_{colour}"), 1))
    write(states / "spool_thread.json", {"variants": {
        f"colour={c}": {"model": rid(f"block/spool_thread_{c}")} for c in THREAD_COLOURS}})
    # A taut thread is drawn upright and turned onto its axis.
    turns = {"y": {}, "z": {"x": 90}, "x": {"x": 90, "y": 90}}
    write(states / "taut_thread.json", {"variants": {
        f"axis={a},colour={c}": {"model": rid(f"block/taut_thread_{c}"), **turns[a]} for a in "xyz" for c in THREAD_COLOURS}})
    write(models / "pin_shaft.json", rod(rid("block/pin_shaft"), 2))
    write(states / "pin_shaft.json", {"variants": {"": {"model": rid("block/pin_shaft")}}})
    for mark in range(4):
        texture, back = rid(f"block/measuring_tape_{mark}"), rid("block/measuring_tape_back")
        write(models / f"measuring_tape_{mark}.json", plate(texture, back, 0))
        write(models / f"measuring_tape_{mark}_upper.json", plate(texture, back, 8))
    write(states / "measuring_tape.json", {"variants": {
        f"half={h},mark={m}": {"model": rid(f"block/measuring_tape_{m}" + ("_upper" if h == "upper" else ""))}
        for h in ("lower", "upper") for m in range(4)}})


def half_block(top, side):
    """A half block, the bottom half of the block (the trampled snow's half steps on the snow ramp)."""
    edge = {"texture": "#side", "uv": [0, 0, 16, 8]}
    return {"parent": "minecraft:block/block", "textures": {"particle": side, "top": top, "side": side},
            "elements": [{"from": [0, 0, 0], "to": [16, 8, 16], "faces": {
                "up": {"texture": "#top"}, "down": {"texture": "#side", "cullface": "down"},
                "north": {**edge, "cullface": "north"}, "south": {**edge, "cullface": "south"},
                "east": {**edge, "cullface": "east"}, "west": {**edge, "cullface": "west"}}}]}


# A giant icicle's parts, from the vault down: boxes (half-width in pixels, from y, to y), each narrower than the last.
ICICLE_BOXES = {"base": ((7, 8, 16), (6, 0, 8)), "middle": ((5, 8, 16), (4, 0, 8)), "tip": ((3, 9, 16), (2, 3, 9), (1, 0, 3))}


def icicle(part):
    """One part of a giant icicle hanging from the vault: square boxes of ice down the middle of the block."""
    elements = []
    for half, y0, y1 in ICICLE_BOXES[part]:
        lo, hi = 8 - half, 8 + half
        side = {"texture": "#ice", "uv": [lo, 16 - y1, hi, 16 - y0]}
        end = {"texture": "#ice", "uv": [lo, lo, hi, hi]}
        elements.append({"from": [lo, y0, lo], "to": [hi, y1, hi], "faces": {
            "north": side, "south": side, "east": side, "west": side, "up": end, "down": end}})
    texture = rid("block/giant_icicle")
    return {"parent": "minecraft:block/block", "textures": {"particle": texture, "ice": texture}, "elements": elements}


def glacier_hall_assets(models, states, write, simple):
    """The Glacier Hall's lair-only blocks (tools/glacier_hall.py)."""
    for name in ("drift_snow", "glare_ice", "mammoth_tusk", "frozen_hoard"):
        simple(name, {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{name}")}})
    top, side = rid("block/trampled_snow"), rid("block/trampled_snow_side")
    write(models / "trampled_snow.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": top, "side": side, "bottom": side}})
    write(models / "trampled_snow_half.json", half_block(top, side))
    write(states / "trampled_snow.json", {"variants": {"height=1": {"model": rid("block/trampled_snow_half")},
                                                       "height=2": {"model": rid("block/trampled_snow")}}})
    for part in ICICLE_PARTS:
        write(models / f"giant_icicle_{part}.json", icicle(part))
    write(states / "giant_icicle.json", {"variants": {
        f"part={part}": {"model": rid(f"block/giant_icicle_{part}")} for part in ICICLE_PARTS}})


# The Cinder Kiln's trough: its stone's top TROUGH_TOP pixels up (a shallow channel in the floor), and the water a flooded
# trough is filled with up to TROUGH_WATER.
TROUGH_TOP = 13
TROUGH_WATER = 15
# Which way each facing turns a model drawn facing north.
FACING_TURNS = {"north": 0, "east": 90, "south": 180, "west": 270}


def trough(flooded):
    """Trough stone: a paved block sunk TROUGH_TOP pixels high, and, flooded, a sheet of water over it nearly to the brim."""
    side = {"texture": "#side", "uv": [0, 16 - TROUGH_TOP, 16, 16]}
    elements = [{"from": [0, 0, 0], "to": [16, TROUGH_TOP, 16], "faces": {
        "up": {"texture": "#top"}, "down": {"texture": "#side", "cullface": "down"},
        "north": {**side, "cullface": "north"}, "south": {**side, "cullface": "south"},
        "east": {**side, "cullface": "east"}, "west": {**side, "cullface": "west"}}}]
    textures = {"particle": rid("block/trough_stone_side"), "top": rid("block/trough_stone_top"),
                "side": rid("block/trough_stone_side")}
    if flooded:
        textures["water"] = {"force_translucent": True, "sprite": rid("block/trough_water")}
        elements.append({"from": [0, TROUGH_TOP, 0], "to": [16, TROUGH_WATER, 16], "faces": {"up": {"texture": "#water"}}})
    return {"parent": "minecraft:block/block", "textures": textures, "elements": elements}


def sluice_panel(open_):
    """A sluice gate's panel, drawn facing north: shut, a riveted iron plate filling the block; open, raised into the
    lintel, and a sheet of water gushing where it was (seen from the kiln and from behind)."""
    if not open_:
        return {"parent": "minecraft:block/block", "textures": {
            "particle": rid("block/sluice_panel"), "plate": rid("block/sluice_panel"), "edge": rid("block/sluice_frame")},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                "north": {"texture": "#plate"}, "south": {"texture": "#plate"}, "east": {"texture": "#edge"},
                "west": {"texture": "#edge"}, "up": {"texture": "#edge"}, "down": {"texture": "#edge"}}}]}
    water = {"force_translucent": True, "sprite": rid("block/sluice_water")}
    return {"parent": "minecraft:block/block", "textures": {"particle": rid("block/sluice_frame"), "water": water},
            "elements": [{"from": [0, 0, 6], "to": [16, 16, 10], "shade": False, "faces": {
                "north": {"texture": "#water"}, "south": {"texture": "#water"}}}]}


def sluice_choked():
    """A sluice gate's panel choked with slag (the Cinder Tyrant's Eruption): the shut plate, and over each of its
    faces the slag forced through its seams and oozing from under it, cut out and drawn glowing."""
    shut = sluice_panel(False)
    shut["textures"]["slag"] = rid("block/sluice_slag")
    shut["elements"].append({"from": [0, 0, -0.05], "to": [16, 16, 16.05], "shade": False, "light_emission": 15, "faces": {
        "north": {"texture": "#slag"}, "south": {"texture": "#slag"}}})
    return shut


def sluice_wheel(turned):
    """A sluice gate's wheel, drawn facing north on the post south of it: an iron shaft from the post, a hub, and the
    wheel's spoked rim (cut out) between them; turned an eighth while the sluice is open."""
    disc = {"from": [1, 1, 10], "to": [15, 15, 11], "faces": {
        "north": {"texture": "#wheel"}, "south": {"texture": "#wheel"}}}
    if turned:
        disc["rotation"] = {"origin": [8, 8, 10.5], "axis": "z", "angle": 45}
    iron = {face: {"texture": "#iron"} for face in ALL}
    return {"parent": "minecraft:block/block", "textures": {
        "particle": rid("block/sluice_frame"), "iron": rid("block/sluice_frame"), "wheel": rid("block/sluice_wheel")},
        "elements": [{"from": [7, 7, 11], "to": [9, 9, 16], "faces": iron},
                     {"from": [6, 6, 8], "to": [10, 10, 11], "faces": iron}, disc]}


def cinder_kiln_assets(models, states, write, simple):
    """The Cinder Kiln's lair-only blocks (tools/cinder_kiln.py)."""
    simple("kiln_brick", {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/kiln_brick")}})
    simple("molten_slag", {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/molten_slag")}})
    simple("cracked_basalt", {"parent": "minecraft:block/cube_column", "textures": {
        "end": rid("block/cracked_basalt_top"), "side": rid("block/cracked_basalt_side")}})
    write(models / "trough_stone.json", trough(False))
    write(models / "trough_stone_flooded.json", trough(True))
    write(states / "trough_stone.json", {"variants": {"flooded=false": {"model": rid("block/trough_stone")},
                                                      "flooded=true": {"model": rid("block/trough_stone_flooded")}}})
    write(models / "sluice_frame.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/sluice_frame")}})
    write(models / "sluice_panel.json", sluice_panel(False))
    write(models / "sluice_panel_open.json", sluice_panel(True))
    write(models / "sluice_panel_choked.json", sluice_choked())
    write(models / "sluice_wheel.json", sluice_wheel(False))
    write(models / "sluice_wheel_turned.json", sluice_wheel(True))
    variants = {}
    for part in SLUICE_PARTS:
        for facing, turn in FACING_TURNS.items():
            for flow in SLUICE_FLOWS:
                panel = {"open": "sluice_panel_open", "choked": "sluice_panel_choked"}.get(flow, "sluice_panel")
                model = {"frame": "sluice_frame", "panel": panel,
                         "wheel": "sluice_wheel_turned" if flow == "open" else "sluice_wheel"}[part]
                variant = {"model": rid(f"block/{model}")}
                if turn and part != "frame":
                    variant["y"] = turn
                variants[f"facing={facing},flow={flow},part={part}"] = variant
    write(states / "sluice_gate.json", {"variants": variants})


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"

    def simple(name, model):
        write(models / f"{name}.json", model)
        write(states / f"{name}.json", {"variants": {"": {"model": rid(f"block/{name}")}}})

    simple("blighted_soil", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": rid("block/blighted_soil_top"), "side": rid("block/blighted_soil_side"), "bottom": rid("block/blighted_soil")}})
    for plant in ("black_wheat", "mown_stubble"):
        simple(plant, {"parent": "minecraft:block/crop", "textures": {"crop": rid(f"block/{plant}")}})
    write(models / "lair_brazier.json", brazier(True))
    write(models / "lair_brazier_unlit.json", brazier(False))
    write(states / "lair_brazier.json", {"variants": {"lit=true": {"model": rid("block/lair_brazier")},
                                                      "lit=false": {"model": rid("block/lair_brazier_unlit")}}})
    for name in ("lair_moon", "lair_moon_red"):
        write(models / f"{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{name}")}})
    write(states / "lair_moon.json", {"variants": {"red=false": {"model": rid("block/lair_moon")},
                                                   "red=true": {"model": rid("block/lair_moon_red")}}})
    write(models / "lair_exit.json", mist("x"))
    write(states / "lair_exit.json", {"variants": {"axis=x": {"model": rid("block/lair_exit")},
                                                   "axis=z": {"model": rid("block/lair_exit"), "y": 90}}})
    spindle_loft_assets(models, states, write, simple)
    glacier_hall_assets(models, states, write, simple)
    cinder_kiln_assets(models, states, write, simple)
    for name, display in LAIR_BLOCKS.items():
        lang[f"block.{MOD}.{name}"] = display

    # The Mourning Wreath (a block laid on a grave) and the Death Knell.
    write(models / "mourning_wreath.json", wreath())
    write(states / "mourning_wreath.json", {"variants": {f"facing={f}": {"model": rid("block/mourning_wreath"), **(
        {"y": {"north": 0, "east": 90, "south": 180, "west": 270}[f]} if f != "north" else {})} for f in HORIZONTAL}})
    for item in ITEMS:
        write(root / "models" / "item" / f"{item}.json", {"parent": "minecraft:item/generated",
                                                          "textures": {"layer0": rid(f"item/{item}")}})
        write(items / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    lang[f"block.{MOD}.mourning_wreath"] = ITEMS["mourning_wreath"]
    lang[f"item.{MOD}.death_knell"] = ITEMS["death_knell"]
    lang[f"item.{MOD}.cursed_spindle"] = ITEMS["cursed_spindle"]
    lang[f"item.{MOD}.frost_horn"] = ITEMS["frost_horn"]
    lang[f"item.{MOD}.kiln_seal"] = ITEMS["kiln_seal"]
    lang[f"tooltip.{MOD}.death_knell"] = "Rung at a grave at night, among lit candles, with a wreath laid on it"
    lang[f"tooltip.{MOD}.cursed_spindle"] = "Used on a Spinning Wheel at night: prick your finger, and wake in the Spindle Loft"
    lang[f"tooltip.{MOD}.mourning_wreath"] = "Lay it on a grave for the Last Rites"
    lang[f"tooltip.{MOD}.frost_horn"] = "Blown at night, standing on snow or ice: the snow opens onto the Glacier Hall"
    lang[f"tooltip.{MOD}.kiln_seal"] = ("Pressed into a magma block in the Nether or a volcanic land: the magma opens onto "
                                        "the Cinder Kiln")
    lang[f"entity.{MOD}.{GATE['entity']}"] = GATE["display"]

    for lair, info in LAIRS.items():
        lang[f"lair.{MOD}.{lair}"] = info["display"]
        lang[f"message.{MOD}.lair.enter.{lair}"] = info["enter"]
    lang.update({
        f"message.{MOD}.lair.rite.no_grave": "The knell rings out over no grave",
        f"message.{MOD}.lair.rite.not_overworld": "The Last Rites are said only in the Overworld",
        f"message.{MOD}.lair.rite.not_night": "The Last Rites are said at night",
        f"message.{MOD}.lair.rite.candles": "The grave needs more lit candles round it: %s of %s",
        f"message.{MOD}.lair.rite.no_wreath": "Lay a Mourning Wreath on the grave first",
        f"message.{MOD}.lair.rite.off_season": "The Last Rites open the Hollow Acre only in the Halloween season",
        f"message.{MOD}.lair.rite.disabled": "The lairs are switched off on this server",
        f"message.{MOD}.lair.rite.opened": "The candles flare blue, and the mist opens over the grave",
        f"message.{MOD}.lair.full": "Every %s is taken; try again soon",
        f"message.{MOD}.lair.party_full": "%s is full",
        f"message.{MOD}.lair.gate_closed": "The mist has closed",
        f"message.{MOD}.lair.already_inside": "You are already in a lair",
        f"message.{MOD}.lair.spindle.not_overworld": "The spindle brings sleep only in the Overworld",
        f"message.{MOD}.lair.spindle.not_night": "It is too early for sleep",
        f"message.{MOD}.lair.spindle.off_season": "The Cursed Spindle opens the Spindle Loft only in the Halloween season",
        f"message.{MOD}.lair.spindle.spinning": "The wheel is already spinning: use it with an empty hand to follow",
        f"message.{MOD}.lair.spindle.opened": "The wheel spins wild, and its thread winds round you",
        f"message.{MOD}.lair.horn.not_overworld": "The horn's call carries only over the Overworld's snow",
        f"message.{MOD}.lair.horn.not_night": "Nothing answers the horn by day",
        f"message.{MOD}.lair.horn.no_snow": "Blow it standing on snow or ice",
        f"message.{MOD}.lair.horn.whirling": "The snow already whirls open here: use the whirl to follow",
        f"message.{MOD}.lair.horn.called": "The horn's call rolls over the snow, and a roar answers it",
        f"message.{MOD}.lair.seal.not_magma": "Press the seal into a magma block",
        f"message.{MOD}.lair.seal.not_volcanic": "The seal takes only in the Nether, or in a volcanic land",
        f"message.{MOD}.lair.seal.venting": "The magma already vents here: use the vent to follow",
        f"message.{MOD}.lair.seal.opened": "The magma cracks open, and a vent of sparks and smoke rises from it",
        f"message.{MOD}.lair.sluice.open": "The sluice is already open",
        f"message.{MOD}.lair.sluice.filling": "The sluice is filling again",
        f"message.{MOD}.lair.sluice.choked": "Slag chokes the sluice: it will not turn",
        f"message.{MOD}.lair.leave": "You step back out of the mist",
        f"message.{MOD}.lair.left_behind": "Anything left lying in a lair is lost when it closes",
        f"message.{MOD}.lair.closed": "The lair has closed; you are back where you were",
        f"message.{MOD}.lair.edge": "The mist throws you back",
        f"message.{MOD}.lair.protected": "Nothing in a lair can be changed",
        f"message.{MOD}.lair.grave_goods.kept": "Your belongings are gathered up as Grave Goods",
        f"message.{MOD}.lair.grave_goods.returned": "Your Grave Goods are returned to you",
        f"commands.{MOD}.lair.not_inside": "You are not in a lair",
        f"commands.{MOD}.lair.list.none": "No lair is open",
        f"commands.{MOD}.lair.list.entry": "%s, slot %s: %s inside, open %s seconds",
        f"commands.{MOD}.lair.close.done": "Closed %s, slot %s",
        f"commands.{MOD}.lair.close.none": "%s has no open slot %s",
    })


def loot(out, write):
    """The wreath gives itself; the lair-only blocks give nothing (they cannot be broken, and none may leave a lair)."""
    write(out / "mourning_wreath.json", self_drop("mourning_wreath"))
    for block in LAIR_BLOCKS:
        write(out / f"{block}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{block}")})


def recipes(out, write, conditions):
    for item, recipe in RECIPES.items():
        write(out / f"{item}.json", {"fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped",
                                     "category": "misc", "pattern": recipe["pattern"], "key": recipe["key"],
                                     "result": {"id": rid(item), "count": 1}})


def tags(tags):
    for flower in MOURNING_FLOWERS:
        tags.add("item", f"{MOD}:mourning_flowers", flower)
    for candle in RITE_CANDLES:
        tags.add("block", f"{MOD}:last_rites_candles", candle)
    for block in LAIR_BLOCKS:
        tags.add("block", f"{MOD}:lair_blocks", rid(block))
        tags.add("block", "minecraft:wither_immune", rid(block))
        tags.add("block", "minecraft:dragon_immune", rid(block))
    for block in FIXTURES:
        tags.add("block", f"{MOD}:lair_fixtures", rid(block))
    for block in HORN_GROUND:
        tags.add("block", f"{MOD}:frost_horn_ground", block)
    for block in SEAL_GROUND:
        tags.add("block", f"{MOD}:kiln_seal_ground", block)
    for biome in SEAL_LANDS:
        tags.add("worldgen/biome", f"{MOD}:kiln_seal_lands", biome)
    # Blighted soil is earth: plants stand on it.
    tags.add("block", "minecraft:dirt", rid("blighted_soil"))
    tags.add("block", "minecraft:mineable/hoe", rid("mourning_wreath"))


def dimension_type(info):
    """Night that never ends: a fixed time, no skybox (no sun, moon or stars, only the sky's colour), sky light that adds
    nothing, a little ambient light, no beds or respawn anchors, no raids, the lair's music, sky and fog."""
    never = {"can_sleep": "never", "can_set_spawn": "never", "destroy_on_use": False}
    return {
        "has_fixed_time": True, "has_skylight": True, "has_ceiling": False, "has_ender_dragon_fight": False,
        "coordinate_scale": 1.0, "min_y": 0, "height": 256, "logical_height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld", "ambient_light": info["ambient_light"],
        "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0, "skybox": "none",
        "attributes": {
            "minecraft:audio/background_music": {"default": {"sound": info["music"], "min_delay": 3000, "max_delay": 9000,
                                                             "replace_current_music": True}},
            "minecraft:gameplay/bed_rule": never,
            "minecraft:gameplay/straw_bed_rule": never,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:visual/ambient_light_color": info["ambient_light_color"],
            "minecraft:visual/fog_color": info["fog"],
            "minecraft:visual/sky_color": info["sky"],
            "minecraft:visual/sky_light_color": info["sky_light_color"],
            "minecraft:visual/sky_light_factor": 0.0,
        },
        "timelines": [],
    }


def biome(info):
    """No spawns, nothing generated, drifting motes (ash, dust); the sky and fog of the dimension (a biome's would win)."""
    return {
        "has_precipitation": False, "temperature": 0.5, "downfall": 0.0,
        "attributes": {
            "minecraft:gameplay/natural_mob_spawns": {"argument": {"spawns_by_category": {}, "spawn_costs": {}},
                                                      "modifier": "overlay"},
            "minecraft:visual/sky_color": info["sky"],
            "minecraft:visual/fog_color": info["fog"],
            "minecraft:visual/water_fog_color": info["water_fog"],
            "minecraft:visual/ambient_particles": [{"particle": {"type": info["motes"][0]}, "probability": info["motes"][1]}],
        },
        "effects": {"water_color": info["water"]},
        "carvers": [],
        "features": [],
    }


def worldgen(data, write):
    for lair, info in LAIRS.items():
        write(data / MOD / "dimension_type" / f"{lair}.json", dimension_type(info))
        write(data / MOD / "worldgen" / "biome" / f"{lair}.json", biome(info))
        write(data / MOD / "dimension" / f"{lair}.json", {"type": rid(lair), "generator": {
            "type": "minecraft:flat", "settings": {"layers": [{"height": 1, "block": "minecraft:air"}], "lakes": False,
                                                   "features": False, "biome": rid(lair)}}})
    for module in TEMPLATES.values():
        module.write(data / MOD / "structure" / f"{module.TEMPLATE}.nbt")
