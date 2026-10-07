"""Fire control (batch 56, docs/features/fire-control.md): one table directing a battery of guns.

- Fire Control Table: a plotting table with a range dial, a handset and a mode lamp. Link up to MAX_GUNS guns to it with
  Fire Control Wire. It lays every linked gun that has nobody at its controls:
  - Hold: the guns stand still.
  - Converge: every gun lays on the table's target (the mark the user made with a Range Finder), and a redstone pulse
    into the table (a button, a lever, a ringing field telephone) fires one round from each.
  - Parallel: the same, but the guns lay on points SHEAF_SPACING blocks apart across the line of fire, so their shells
    land side by side instead of on one block.
  - Creeping barrage: as converge, but after each salvo the target steps CREEP_STEP blocks further down the line of
    fire, CREEP_STEPS times, then starts again from where it was set: a wall of fire walking ahead of an advance.
  - Sentry: each gun fires on its own at the nearest hostile mob inside the table's sector, no closer than
    SENTRY_MIN_RANGE blocks to the gun and never with a player within CHECK_FIRE blocks of the mob.
  Guns the table lays fire only shells from ready racks next to them (batch 55). A comparator reads how many linked
  guns are ready (laid, reloaded and with a shell to hand). A gunner aboard a linked gun with no mark of their own lays
  it on the table's point too, and fires it when they choose.
- Fire Control Wire: a reel of signal cable. Use it on a table to start a link, then on a gun within LINK_RANGE blocks
  of the table to link (or unlink) it. Sneak and use it on a table to cut every link.

Every texture is drawn here in the clean style (tools/clean_metal.py). Java: building/FireControlTableBlock.java (the table,
its modes and links), building/FireControl.java (registration and the wire), and artillery/CrewedGun (laying and firing
for a table). tools/check_mod_data.py checks that the numbers match.
"""
from PIL import Image

import clean_metal
from model_writer import element, separate_coplanar, texture_names
from steampunk_models import box, cyl

MOD = "jugcraft"

# id: (display name, hardness, blast resistance).
BLOCKS = {"fire_control_table": ("Fire Control Table", 3.0, 6.0)}
ITEMS = {"fire_control_wire": "Fire Control Wire"}
TOOLTIPS = {
    "fire_control_table": "Lays every linked gun that has nobody at its controls. Use empty-handed to change mode (hold, "
                          "converge, parallel, creeping barrage, sentry), sneak to change the sector, with a Range Finder to set the target. "
                          "A redstone pulse fires one round from each gun.",
    "fire_control_wire": "Use on a fire control table, then on a gun nearby, to link (or unlink) it. Sneak-use on a table "
                         "to cut every link.",
}
# The table's modes, in the order using it cycles through them, with each mode's lamp colour.
MODES = ["hold", "converge", "parallel", "creeping", "sentry"]
MODE_NAMES = {"hold": "Hold", "converge": "Converge", "parallel": "Parallel", "creeping": "Creeping Barrage",
              "sentry": "Sentry"}
# The widths, in degrees, sneak-using the table cycles its sector through (centred on the way the table faces).
SECTORS = [90, 180, 270, 360]
# How many guns a table directs, and how far from it (in blocks) a linked gun may stand.
MAX_GUNS = 8
LINK_RANGE = 64
# A parallel sheaf's spacing across the line of fire, in blocks.
SHEAF_SPACING = 6
# Sentry: how far round each gun it looks for hostile mobs, how close it will not fire, how near a player must not be
# to its mark, and how often (ticks) it picks a target.
SENTRY_RANGE = 96
SENTRY_MIN_RANGE = 12
CHECK_FIRE = 8
SENTRY_SCAN = 10
# Ticks between the table's own updates (its comparator reading and lamp).
TABLE_INTERVAL = 10
# A creeping barrage: how far the target steps down range after each salvo (blocks), and how many steps before it starts
# again from where it was set.
CREEP_STEP = 5
CREEP_STEPS = 6


def blocks():
    return list(BLOCKS)


def items():
    return list(ITEMS)


# ------------------------------------------------------------------ models

PLOT, FRAME, TOP = "fc_plot", "dr_skid", "dp_gunmetal"
BRASS, CHROME, BAND, HAZARD = "ik_brass", "dp_chrome", "dr_band", "dp_hazard"
LAMPS = {mode: f"fc_lamp_{mode}" for mode in MODES}


def model(elements, particle):
    els = [element(list(f), list(t), tx, rotation=o.get("rotation")) for f, t, tx, *rest in elements
           for o in [rest[0] if rest else {}]]
    separate_coplanar(els)
    textures = {name: f"{MOD}:block/{name}" for name in texture_names(elements)}
    textures["particle"] = f"{MOD}:block/{particle}"
    return {"textures": textures, "elements": els}


def table(mode):
    """The table facing north (its user stands south of it): four legs and a stretcher, a top carrying the plotting
    board (the gridded firing chart), a brass range dial and the field handset on the far edge, and the mode lamp in
    front of the dial, lit in the mode's colour."""
    m = []
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        m.append(box((x, 0, z), (x + 2, 10, z + 2), FRAME))
    m.append(box((3, 3, 7), (13, 4, 9), BAND))
    m.append(box((7, 3, 3), (9, 4, 13), BAND))
    m.append(box((0, 10, 0), (16, 11.5, 16), {"*": FRAME, "up": TOP}))
    m.append(box((1.5, 11.5, 4), (14.5, 12.5, 15), {"*": TOP, "up": PLOT}))
    # The far edge: a raised instrument shelf with the range dial, the lamp and the handset.
    m.append(box((1, 11.5, 0.5), (15, 14, 3.5), {"*": FRAME, "up": TOP, "south": HAZARD}))
    m += cyl("z", 4.5, 15.5, 1.6, 0.6, 1.6, BRASS, CHROME)
    m.append(box((8, 14, 1.2), (10, 15.5, 2.8), {"*": LAMPS[mode]}))
    m.append(box((11, 14, 1), (14.5, 15, 3), {"*": TOP, "up": BAND}))
    m.append(box((11, 15, 1.5), (12, 16, 2.5), TOP))
    m.append(box((13.5, 15, 1.5), (14.5, 16, 2.5), TOP))
    m.append(box((10.5, 16, 1.4), (15, 17, 2.6), {"*": TOP, "up": BAND}))
    # A brass pointer and two plotting pins on the chart.
    m.append(box((5, 12.5, 8), (11, 12.75, 8.75), BRASS))
    m.append(box((10.5, 12.5, 7.5), (11.5, 13.5, 9.25), HAZARD))
    m.append(box((4, 12.5, 11), (5, 13.5, 12), CHROME))
    return m


# ------------------------------------------------------------------ data

RECIPES = [
    ("fire_control_table", "fire_control_table", ["RTC", "PMP", "S S"],
     {"R": f"{MOD}:range_finder", "T": f"{MOD}:field_telephone", "C": "minecraft:comparator", "P": "#c:plates/steel",
      "M": "minecraft:map", "S": "#c:plates/steel"}, 1),
    ("fire_control_wire", "fire_control_wire", ["WWW", "WSW", "WWW"],
     {"W": f"{MOD}:copper_wire", "S": "minecraft:stick"}, 1),
]


def write_all(write, assets, data, lang, condition, self_drop):
    from dieselworks import shaped
    models = assets / "models" / "block"
    states = assets / "blockstates"
    facing_y = {"north": 0, "east": 90, "south": 180, "west": 270}
    for block, (name, _, _) in BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = name
        ref = f"{MOD}:block/{block}"
        for mode in MODES:
            write(models / f"{block}_{mode}.json", model(table(mode), FRAME))
        write(states / f"{block}.json", {"variants": {
            f"facing={f},mode={mode}": ({"model": f"{ref}_{mode}", "y": y} if y else {"model": f"{ref}_{mode}"})
            for f, y in facing_y.items() for mode in MODES}})
        write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": f"{ref}_converge"}})
        write(data / "loot_table" / "blocks" / f"{block}.json", self_drop(block))
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    for mode in MODES:
        lang[f"message.{MOD}.fire_control.mode.{mode}"] = MODE_NAMES[mode]
    lang.update({
        f"message.{MOD}.fire_control.status": "Fire control: %s, %s of %s guns ready, sector %s°, %s",
        f"message.{MOD}.fire_control.no_target": "no target",
        f"message.{MOD}.fire_control.target": "target %s blocks away",
        f"message.{MOD}.fire_control.no_mark": "Mark a target with this Range Finder first",
        f"message.{MOD}.fire_control.target_set": "Target set, %s blocks from the table",
        f"message.{MOD}.fire_control.link_started": "Now use the wire on a gun within %s blocks of the table",
        f"message.{MOD}.fire_control.no_table": "Use the wire on a fire control table first",
        f"message.{MOD}.fire_control.too_far": "That gun is more than %s blocks from the table",
        f"message.{MOD}.fire_control.full": "That table already directs %s guns",
        f"message.{MOD}.fire_control.linked": "Gun linked (%s of %s)",
        f"message.{MOD}.fire_control.unlinked": "Gun unlinked",
        f"message.{MOD}.fire_control.cleared": "Every link to this table cut",
        f"message.{MOD}.fire_control.fire": "Fire!",
        f"message.{MOD}.fire_control.creep": "step %s of %s",
    })
    for name, result, pattern, key, count in RECIPES:
        write(data / "recipe" / f"{name}.json", shaped(condition, pattern, key, result, count))


def add_tags(tags):
    for block in blocks():
        tags.add("block", "minecraft:mineable/pickaxe", f"{MOD}:{block}")


# ------------------------------------------------------------------ art (the clean style, tools/clean_metal.py)

PAPER = [(150, 140, 112), (186, 176, 146), (210, 200, 168), (226, 218, 190)]
INK = (70, 82, 96)
RED = (168, 52, 40)
STEEL = [(40, 42, 46), (60, 62, 66), (82, 84, 88), (106, 108, 112), (136, 138, 142), (170, 172, 176)]
COPPER = [(110, 58, 36), (160, 88, 52), (200, 120, 72), (232, 160, 108)]
WOOD = [(86, 62, 38), (116, 86, 54), (146, 110, 70)]
LAMP_COLOURS = {
    "hold": [(70, 20, 18), (120, 36, 30), (150, 52, 44)],
    "converge": [(150, 96, 20), (226, 160, 48), (255, 214, 120)],
    "parallel": [(36, 110, 52), (76, 182, 88), (160, 236, 160)],
    "creeping": [(30, 70, 140), (70, 130, 220), (170, 210, 255)],
    "sentry": [(140, 24, 20), (226, 56, 40), (255, 150, 120)],
}


def plot():
    """The firing chart: buff paper with a blue grid every four pixels, range arcs from the gun position (bottom
    centre) and the red line of fire to the target."""
    img = clean_metal.canvas(PAPER[2])
    clean_metal.bevel(img, 0, 0, 15, 15, PAPER[3], PAPER[0], PAPER[2])
    for i in range(2, 15, 4):
        for j in range(1, 15):
            clean_metal.put(img, i, j, PAPER[1])
            clean_metal.put(img, j, i, PAPER[1])
    for r in (5, 9, 13):
        for j in range(1, 15):
            for i in range(1, 15):
                d = ((i - 7.5) ** 2 + (j - 14.5) ** 2) ** 0.5
                if abs(d - r) < 0.5:
                    clean_metal.put(img, i, j, INK)
    for step in range(11):
        clean_metal.put(img, 8 + step * 4 // 10, 14 - step, RED)
    clean_metal.put(img, 11, 3, RED)
    clean_metal.put(img, 12, 3, RED)
    clean_metal.put(img, 11, 2, RED)
    clean_metal.put(img, 12, 2, RED)
    return img


def lamp(mode):
    """The mode lamp: a glass bead with a hot centre."""
    dark, mid, hot = LAMP_COLOURS[mode]
    img = clean_metal.canvas(mid)
    clean_metal.rect(img, 0, 0, 15, 15, dark)
    clean_metal.rect(img, 2, 2, 13, 13, mid)
    clean_metal.rect(img, 5, 5, 10, 10, hot)
    clean_metal.rect(img, 4, 3, 6, 4, hot)
    return img


def wire_icon():
    """A reel of signal cable: wooden cheeks, copper turns between them and a loose end."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3, 13):
        for x in range(4, 12):
            clean_metal.put(img, x, y, COPPER[1] if y % 2 else COPPER[2])
        clean_metal.put(img, 4, y, COPPER[0])
        clean_metal.put(img, 11, y, COPPER[3])
    for x in range(2, 14):
        for y, c in ((1, WOOD[2]), (2, WOOD[1]), (13, WOOD[1]), (14, WOOD[0])):
            clean_metal.put(img, x, y, c)
    clean_metal.put(img, 7, 1, STEEL[3])
    clean_metal.put(img, 8, 1, STEEL[3])
    for x, y in ((12, 7), (13, 8), (14, 9), (14, 10), (13, 11)):
        clean_metal.put(img, x, y, COPPER[2])
    return img


def draw_all(save):
    save(plot(), "block", PLOT)
    for mode in MODES:
        save(lamp(mode), "block", LAMPS[mode])
    save(wire_icon(), "item", "fire_control_wire")
