"""Fortifications (batch 55, docs/features/fortifications.md): what a gun tower is built from and how it is supplied.

- Bastion Concrete: board-marked cast concrete, as a block, slab, stairs and a wall (the first wall in the mod).
- Bastion Parapet: a crenellated top for a gun deck, two merlons with a gap to fire through.
- Steel Ladder: rungs on two rails for tower shafts.
- Blast Door: a heavy riveted steel door that only redstone opens, as blast-proof as blast-proof concrete.
- Ammo Hoist: stack hoist blocks into a shaft. Whatever goes into any of them climbs to the top one, which hands it to the
  container above it or beside it.
- Ready Rack: a rack of shells beside a gun. A gunner out of shells draws from any ready rack next to their gun; the
  rack shows how full it is.

Every texture is drawn here in the clean style (tools/clean_metal.py). Java: building/Fortifications.java (the blocks),
building/AmmoHoistBlock and building/ReadyRackBlock (with their block entities), and artillery/CrewedGun (drawing from
racks). tools/check_mod_data.py checks that the numbers match.
"""
import math

from PIL import Image

import clean_metal
from model_writer import element, separate_coplanar, texture_names
from steampunk_models import box, cyl

MOD = "jugcraft"

# id: (display name, kind, hardness, blast resistance).
BLOCKS = {
    "bastion_concrete": ("Bastion Concrete", "bastion", 4.0, 24.0),
    "bastion_parapet": ("Bastion Parapet", "parapet", 4.0, 24.0),
    "steel_ladder": ("Steel Ladder", "ladder", 1.5, 6.0),
    "blast_door": ("Blast Door", "door", 15.0, 1200.0),
    "ammo_hoist": ("Ammo Hoist", "hoist", 3.0, 6.0),
    "ready_rack": ("Ready Rack", "rack", 2.5, 6.0),
    # Fortification extras.
    "bunker_door": ("Bunker Door", "door_wood", 4.0, 12.0),
    "sliding_gate": ("Sliding Gate", "gate", 6.0, 24.0),
    "bastion_parapet_corner": ("Bastion Parapet Corner", "parapet_corner", 4.0, 24.0),
    "bastion_embrasure": ("Bastion Embrasure", "embrasure", 4.0, 24.0),
}
TOOLTIPS = {
    "bastion_concrete": "Board-marked cast concrete: tougher than plain concrete against blasts. Also makes walls.",
    "bastion_parapet": "A crenellated top for a gun deck: two merlons with a gap to shoot through.",
    "steel_ladder": "Rungs on two steel rails, for tower shafts.",
    "blast_door": "A heavy steel door that only redstone opens. As blast-proof as blast-proof concrete.",
    "ammo_hoist": "Stack hoists into a shaft: anything put into one climbs to the top, which hands it to the container "
                  "above or beside it. Use one with an item in hand to load it.",
    "ready_rack": "Holds shells beside a gun. A gunner with no shells draws from any ready rack next to their gun. Use "
                  "with shells to stock it, empty-handed to take a stack back.",
    "bunker_door": "A heavy strapped timber door for dugouts and bunkers. Opens by hand.",
    "sliding_gate": "Steel bars that slide aside on a redstone signal. Gates side by side or stacked open together.",
    "bastion_parapet_corner": "A parapet's corner: one big merlon to turn a ring of parapets round a corner.",
    "bastion_embrasure": "Bastion concrete with a narrow gun slit through it, to see and shoot out through.",
}
# The ammo hoist: ticks between lifts, items each lift carries, and how many items each hoist block holds.
HOIST_INTERVAL = 8
HOIST_BATCH = 4
HOIST_BUFFER = 16
# The ready rack: its slots, and how far beyond a gun's own size (in blocks) a rack can stand and still feed it.
RACK_SLOTS = 9
RACK_REACH = 2
# The shells a ready rack takes (the item tag jugcraft:artillery_shells).
SHELLS = ["heavy_shell", "flak_shell", "great_shell"]


def blocks():
    out = []
    for block, (_, kind, _, _) in BLOCKS.items():
        out.append(block)
        if kind == "bastion":
            out += [f"{block}_slab", f"{block}_stairs", f"{block}_wall"]
    return out


# ------------------------------------------------------------------ models

CONCRETE, CAP, LADDER, CHAIN = "fw_bastion", "fw_bastion_top", "fw_ladder", "fw_chain"
SKID, GUNMETAL, BRASS, CHROME, BAND, HAZARD = "dr_skid", "dp_gunmetal", "ik_brass", "dp_chrome", "dr_band", "dp_hazard"


def model(elements, particle):
    """A block model from box elements: each texture name becomes jugcraft:block/<name>."""
    els = [element(list(f), list(t), tx, rotation=o.get("rotation")) for f, t, tx, *rest in elements
           for o in [rest[0] if rest else {}]]
    separate_coplanar(els)
    textures = {name: f"{MOD}:block/{name}" for name in texture_names(elements)}
    textures["particle"] = f"{MOD}:block/{particle}"
    return {"textures": textures, "elements": els}


def parapet():
    """Two merlons on a full-width footing: the gap between them is a crenel to fire through."""
    face = {"*": CONCRETE, "up": CAP, "down": CAP}
    return [box((0, 0, 0), (16, 8, 16), face), box((0, 8, 0), (6, 16, 16), face), box((10, 8, 0), (16, 16, 16), face)]


def hoist(top):
    """One block of the hoist shaft: four corner posts, cross braces, the chain up the middle and a shell cage; the
    top block adds a header beam and a pulley wheel."""
    m = []
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        m.append(box((x, 0, z), (x + 2, 16, z + 2), SKID))
    for y in (0, 14):
        m.append(box((1, y, 3), (3, y + 2, 13), BAND))
        m.append(box((13, y, 3), (15, y + 2, 13), BAND))
        m.append(box((3, y, 1), (13, y + 2, 3), BAND))
        m.append(box((3, y, 13), (13, y + 2, 15), BAND))
    m.append(box((7.5, 0, 7.5), (8.5, 16, 8.5), {"*": CHAIN, "up": None, "down": None}))
    m.append(box((5, 5, 5), (11, 6, 11), GUNMETAL))
    for x, z in ((5, 5), (10, 5), (5, 10), (10, 10)):
        m.append(box((x, 6, z), (x + 1, 11, z + 1), SKID))
    m += cyl("y", 8, 8, 1.4, 6, 10, BRASS, CHROME)
    if top:
        m.append(box((1, 16, 6), (15, 18, 10), {"*": GUNMETAL, "up": HAZARD}))
        m += cyl("x", 12, 8, 3, 6.5, 9.5, BAND, BRASS)
    return m


def rack(fill):
    """The ready rack facing north: a steel frame with a back panel and three shelves, and on each of the first `fill`
    shelves three shells standing upright, brass cases with steel noses, so you can see from the front how full it is."""
    m = []
    for x, z in ((0, 2), (14, 2), (0, 14), (14, 14)):
        m.append(box((x, 0, z), (x + 2, 16, z + 2), SKID))
    for y in (0, 5, 10):
        m.append(box((2, y, 2), (14, y + 1, 15), GUNMETAL))
        m.append(box((2, y + 1, 2), (14, y + 2, 3), {"*": BAND}))
    m.append(box((2, 0, 15), (14, 16, 16), GUNMETAL))
    m.append(box((0, 15, 2), (16, 16, 16), {"*": SKID, "up": GUNMETAL, "north": HAZARD}))
    for shelf in range(fill):
        y = 1 + shelf * 5
        for x in (4.5, 8, 11.5):
            m += cyl("y", x, 9, 1.8, y, y + 3, BRASS, BRASS)
            m += cyl("y", x, 9, 1.3, y + 3, y + 4, CHROME, CHROME)
    return m


def blast_door_textures(block="blast_door"):
    return {"bottom": f"{MOD}:block/fw_{block}_bottom", "top": f"{MOD}:block/fw_{block}_top"}


def gate(opened):
    """The sliding gate facing north: a closed panel of heavy bars between two hazard-striped rails; slid open, only its
    end post remains (the bars stacked inside it)."""
    if opened:
        return [box((0, 0, 6), (2.5, 16, 10), {"*": GUNMETAL, "up": HAZARD})]
    m = [box((0, 0, 6), (2, 16, 10), GUNMETAL), box((14, 0, 6), (16, 16, 10), GUNMETAL)]
    for x in (3.5, 6.5, 9.5, 12.5):
        m.append(box((x - 0.75, 0, 7.25), (x + 0.75, 16, 8.75), CHROME))
    for y in (2, 12):
        m.append(box((2, y, 6.5), (14, y + 2, 9.5), {"*": GUNMETAL, "north": HAZARD, "south": HAZARD}))
    return m


def parapet_corner():
    """The parapet's footing with one big merlon on its front-left corner (facing north)."""
    face = {"*": CONCRETE, "up": CAP, "down": CAP}
    return [box((0, 0, 0), (16, 8, 16), face), box((0, 8, 0), (7, 16, 7), face)]


def embrasure():
    """A full block of bastion concrete with a slit two pixels high and six wide, running along z (facing north)."""
    face = {"*": CONCRETE, "up": CAP, "down": CAP}
    return [box((0, 0, 0), (16, 9, 16), face), box((0, 11, 0), (16, 16, 16), face),
            box((0, 9, 0), (5, 11, 16), face), box((11, 9, 0), (16, 11, 16), face)]


# Rotations for vanilla door models (as tools/decor3_data.py).
DOOR_CLOSED = {"east": 0, "south": 90, "west": 180, "north": 270}
DOOR_OPEN = {"left": {"east": 90, "south": 180, "west": 270, "north": 0},
             "right": {"east": 270, "south": 0, "west": 90, "north": 180}}
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def wall_models(write, models, block, texture):
    """The vanilla wall templates with the bastion texture: a post, low and tall sides, and the inventory model."""
    tex = {"wall": f"{MOD}:block/{texture}"}
    write(models / f"{block}_post.json", {"parent": "minecraft:block/template_wall_post", "textures": tex})
    write(models / f"{block}_side.json", {"parent": "minecraft:block/template_wall_side", "textures": tex})
    write(models / f"{block}_side_tall.json", {"parent": "minecraft:block/template_wall_side_tall", "textures": tex})
    write(models / f"{block}_inventory.json", {"parent": "minecraft:block/wall_inventory", "textures": tex})


def wall_blockstate(block):
    ref = f"{MOD}:block/{block}"
    parts = [{"when": {"up": "true"}, "apply": {"model": f"{ref}_post"}}]
    for side, y in FACING_Y.items():
        for height, suffix in (("low", "_side"), ("tall", "_side_tall")):
            apply = {"model": f"{ref}{suffix}", "uvlock": True}
            if y:
                apply["y"] = y
            parts.append({"when": {side: height}, "apply": apply})
    return {"multipart": parts}


# ------------------------------------------------------------------ data

RECIPES = [
    ("bastion_concrete", "bastion_concrete", ["CRC", "RCR", "CRC"], {"C": f"{MOD}:concrete", "R": f"{MOD}:rebar"}, 8),
    ("bastion_concrete_wall", "bastion_concrete_wall", ["BBB", "BBB"], {"B": f"{MOD}:bastion_concrete"}, 6),
    ("bastion_parapet", "bastion_parapet", ["B B", "BBB"], {"B": f"{MOD}:bastion_concrete"}, 4),
    ("steel_ladder", "steel_ladder", ["S S", "SNS", "S S"], {"S": "#c:plates/steel", "N": "minecraft:iron_nugget"}, 8),
    ("blast_door", "blast_door", ["PP", "PB", "PP"], {"P": "#c:plates/steel", "B": f"{MOD}:blastproof_concrete"}, 1),
    ("ammo_hoist", "ammo_hoist", ["SCS", "SGS", "SCS"], {"S": "#c:plates/steel", "C": "minecraft:iron_chain",
                                                        "G": "#c:gears/steel"}, 4),
    ("ready_rack", "ready_rack", ["S S", "PPP", "S S"], {"S": "#c:plates/steel", "P": "#minecraft:wooden_slabs"}, 2),
    ("bunker_door", "bunker_door", ["PP", "PI", "PP"], {"P": "minecraft:spruce_planks", "I": "minecraft:iron_ingot"}, 1),
    ("sliding_gate", "sliding_gate", ["SRS", "SRS", "SRS"], {"S": "#c:plates/steel", "R": f"{MOD}:rebar"}, 3),
    ("bastion_parapet_corner", "bastion_parapet_corner", ["B ", "BB"], {"B": f"{MOD}:bastion_concrete"}, 2),
    ("bastion_embrasure", "bastion_embrasure", ["BB", "BB"], {"B": f"{MOD}:bastion_concrete"}, 4),
]


def write_all(write, assets, data, lang, condition, self_drop):
    from construction import stairs_blockstate
    from dieselworks import shaped
    models = assets / "models" / "block"
    states = assets / "blockstates"

    def item(block, ref=None):
        write(assets / "items" / f"{block}.json",
              {"model": {"type": "minecraft:model", "model": ref or f"{MOD}:block/{block}"}})

    def flat_item(block, texture):
        write(assets / "models" / "item" / f"{block}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{texture}"}})
        item(block, f"{MOD}:item/{block}")

    for block, (name, kind, _, _) in BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = name
        ref = f"{MOD}:block/{block}"
        if kind == "bastion":
            tex = f"{MOD}:block/{CONCRETE}"
            side = {"bottom": tex, "top": tex, "side": tex}
            write(models / f"{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})
            write(states / f"{block}.json", {"variants": {"": {"model": ref}}})
            item(block)
            slab = f"{block}_slab"
            write(models / f"{slab}.json", {"parent": "minecraft:block/slab", "textures": side})
            write(models / f"{slab}_top.json", {"parent": "minecraft:block/slab_top", "textures": side})
            write(states / f"{slab}.json", {"variants": {"type=bottom": {"model": f"{ref}_slab"},
                                                        "type=top": {"model": f"{ref}_slab_top"},
                                                        "type=double": {"model": ref}}})
            item(slab)
            lang[f"block.{MOD}.{slab}"] = f"{name} Slab"
            stairs = f"{block}_stairs"
            for suffix in ("", "_inner", "_outer"):
                parent = "stairs" if not suffix else suffix[1:] + "_stairs"
                write(models / f"{stairs}{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": side})
            write(states / f"{stairs}.json", stairs_blockstate(f"{ref}_stairs"))
            item(stairs)
            lang[f"block.{MOD}.{stairs}"] = f"{name} Stairs"
            wall = f"{block}_wall"
            wall_models(write, models, wall, CONCRETE)
            write(states / f"{wall}.json", wall_blockstate(wall))
            item(wall, f"{MOD}:block/{wall}_inventory")
            lang[f"block.{MOD}.{wall}"] = f"{name} Wall"
            for variant in (slab, stairs, wall):
                table = self_drop(variant)
                if variant == slab:
                    table["pools"][0]["entries"][0]["modifier"] = [
                        {"type": "minecraft:set_count", "count": 2, "add": False,
                         "condition": {"type": "minecraft:match_block", "blocks": f"{MOD}:{slab}", "state": {"type": "double"}}},
                        {"type": "minecraft:explosion_decay"}]
                write(data / "loot_table" / "blocks" / f"{variant}.json", table)
            write(data / "recipe" / f"{slab}.json", shaped(condition, ["BBB"], {"B": f"{MOD}:{block}"}, slab, 6))
            write(data / "recipe" / f"{stairs}.json", shaped(condition, ["B  ", "BB ", "BBB"], {"B": f"{MOD}:{block}"}, stairs, 4))
        elif kind == "parapet":
            write(models / f"{block}.json", model(parapet(), CONCRETE))
            write(states / f"{block}.json", {"variants": {f"facing={f}": ({"model": ref, "y": y} if y else {"model": ref})
                                                          for f, y in FACING_Y.items()}})
            item(block)
        elif kind == "ladder":
            write(models / f"{block}.json", {"parent": "minecraft:block/ladder",
                                             "textures": {"texture": f"{MOD}:block/{LADDER}", "particle": f"{MOD}:block/{LADDER}"}})
            write(states / f"{block}.json", {"variants": {f"facing={f}": ({"model": ref, "y": y} if y else {"model": ref})
                                                          for f, y in FACING_Y.items()}})
            flat_item(block, "steel_ladder")
        elif kind in ("door", "door_wood"):
            for half in ("bottom", "top"):
                for hinge in ("left", "right"):
                    for opened in ("", "_open"):
                        write(models / f"{block}_{half}_{hinge}{opened}.json",
                              {"parent": f"minecraft:block/door_{half}_{hinge}{opened}", "textures": blast_door_textures(block)})
            variants = {}
            for facing in FACING_Y:
                for half, part in (("lower", "bottom"), ("upper", "top")):
                    for hinge in ("left", "right"):
                        for is_open in ("false", "true"):
                            y = DOOR_OPEN[hinge][facing] if is_open == "true" else DOOR_CLOSED[facing]
                            variant = {"model": f"{ref}_{part}_{hinge}{'_open' if is_open == 'true' else ''}"}
                            if y:
                                variant["y"] = y
                            variants[f"facing={facing},half={half},hinge={hinge},open={is_open}"] = variant
            write(states / f"{block}.json", {"variants": variants})
            flat_item(block, block)
            # A door drops one item, from its lower half only (as the crypt door's loot table).
            write(data / "loot_table" / "blocks" / f"{block}.json", {
                "type": "minecraft:block", "random_sequence": f"{MOD}:blocks/{block}",
                "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}"}],
                           "condition": {"type": "minecraft:all_of", "terms": [
                               {"type": "minecraft:survives_explosion"},
                               {"type": "minecraft:match_block", "blocks": f"{MOD}:{block}", "state": {"half": "lower"}}]}}]})
            continue
        elif kind == "gate":
            write(models / f"{block}.json", model(gate(False), GUNMETAL))
            write(models / f"{block}_open.json", model(gate(True), GUNMETAL))
            write(states / f"{block}.json", {"variants": {
                f"facing={f},open={o},powered={p}": ({"model": ref + ("_open" if o == "true" else ""), "y": y} if y
                                                      else {"model": ref + ("_open" if o == "true" else "")})
                for f, y in FACING_Y.items() for o in ("false", "true") for p in ("false", "true")}})
            item(block)
        elif kind in ("parapet_corner", "embrasure"):
            write(models / f"{block}.json", model(parapet_corner() if kind == "parapet_corner" else embrasure(), CONCRETE))
            write(states / f"{block}.json", {"variants": {f"facing={f}": ({"model": ref, "y": y} if y else {"model": ref})
                                                          for f, y in FACING_Y.items()}})
            item(block)
        elif kind == "hoist":
            write(models / f"{block}.json", model(hoist(False), SKID))
            write(models / f"{block}_top.json", model(hoist(True), SKID))
            write(states / f"{block}.json", {"variants": {"top=false": {"model": ref}, "top=true": {"model": f"{ref}_top"}}})
            item(block, f"{ref}_top")
        elif kind == "rack":
            for fill in range(4):
                write(models / f"{block}_{fill}.json", model(rack(fill), SKID))
            write(states / f"{block}.json", {"variants": {
                f"facing={f},fill={fill}": ({"model": f"{ref}_{fill}", "y": y} if y else {"model": f"{ref}_{fill}"})
                for f, y in FACING_Y.items() for fill in range(4)}})
            item(block, f"{ref}_3")
        write(data / "loot_table" / "blocks" / f"{block}.json", self_drop(block))
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"message.{MOD}.ready_rack.contents"] = "Ready rack: %s shells"
    for name, result, pattern, key, count in RECIPES:
        write(data / "recipe" / f"{name}.json", shaped(condition, pattern, key, result, count))


def add_tags(tags):
    """Tool and behaviour tags: walls connect, the ladder is climbable, the door is a door, shells fit ready racks."""
    wall = f"{MOD}:bastion_concrete_wall"
    tags.add("block", "minecraft:walls", wall)
    tags.add("item", "minecraft:walls", wall)
    tags.add("block", "minecraft:climbable", f"{MOD}:steel_ladder")
    tags.add("block", "minecraft:doors", f"{MOD}:blast_door")
    tags.add("item", "minecraft:doors", f"{MOD}:blast_door")
    tags.add("block", "minecraft:wooden_doors", f"{MOD}:bunker_door")
    tags.add("item", "minecraft:wooden_doors", f"{MOD}:bunker_door")
    for shell in SHELLS:
        tags.add("item", f"{MOD}:artillery_shells", f"{MOD}:{shell}")
    for block in blocks():
        tags.add("block", "minecraft:mineable/axe" if block == "bunker_door" else "minecraft:mineable/pickaxe", f"{MOD}:{block}")
        if block.startswith("bastion"):
            tags.add("block", "minecraft:needs_stone_tool", f"{MOD}:{block}")
        if block == "blast_door":
            tags.add("block", "minecraft:needs_diamond_tool", f"{MOD}:{block}")


# ------------------------------------------------------------------ art (the clean style, tools/clean_metal.py)

STONE = [(96, 94, 88), (114, 112, 104), (130, 128, 120), (146, 144, 136), (162, 160, 152)]
STEEL = [(40, 42, 46), (60, 62, 66), (82, 84, 88), (106, 108, 112), (136, 138, 142), (170, 172, 176)]
PAINT = [(130, 96, 18), (176, 134, 28), (214, 170, 42), (236, 196, 72)]


def bastion():
    """Board-marked concrete: four courses of plank imprints, each lit along its top edge, staggered joints and the
    round tie holes left by the formwork."""
    img = clean_metal.canvas(STONE[2])
    for course in range(4):
        y0 = course * 4
        for x in range(16):
            clean_metal.put(img, x, y0, STONE[3])
            clean_metal.put(img, x, y0 + 3, STONE[1])
        joint = (3, 11, 7, 14)[course]
        for y in range(y0 + 1, y0 + 3):
            clean_metal.put(img, joint, y, STONE[1])
        # A faint grain line along each board.
        for x in range((course * 5) % 6, 16, 7):
            clean_metal.put(img, x, y0 + 1 + course % 2, STONE[1])
    for x, y in ((4, 6), (12, 6), (4, 14), (12, 14)):
        clean_metal.put(img, x, y, STONE[0])
        clean_metal.put(img, x + 1, y, STONE[1])
    return img


def bastion_top():
    """The cap of a parapet: smooth concrete with a lit arris round the edge."""
    img = clean_metal.canvas(STONE[2])
    clean_metal.bevel(img, 0, 0, 15, 15, STONE[3], STONE[1], STONE[2])
    clean_metal.scuffs(img, STONE[3], [(4, 5, 3), (9, 10, 2)], only=STONE[2])
    return img


def ladder():
    """Two steel rails and four rungs, see-through between them, each rung lit along its top."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x, c in ((2, STEEL[4]), (3, STEEL[2]), (12, STEEL[4]), (13, STEEL[2])):
            clean_metal.put(img, x, y, c)
    for y0 in (1, 5, 9, 13):
        for x in range(4, 12):
            clean_metal.put(img, x, y0, STEEL[4])
            clean_metal.put(img, x, y0 + 1, STEEL[2])
    return img


def chain():
    """Hoist chain: links alternating face-on and edge-on, see-through around them."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        if y % 4 in (0, 3):
            for x in (6, 9):
                clean_metal.put(img, x, y, STEEL[3])
            clean_metal.put(img, 7, y, STEEL[4] if y % 4 == 0 else STEEL[1])
            clean_metal.put(img, 8, y, STEEL[4] if y % 4 == 0 else STEEL[1])
        else:
            clean_metal.put(img, 7, y, STEEL[4])
            clean_metal.put(img, 8, y, STEEL[2])
    return img


def blast_door(top):
    """The blast door's halves: a riveted steel leaf in a heavy frame. The top half has a vision slit, the bottom
    the locking handwheel and a hazard-striped kick plate."""
    img = clean_metal.canvas(STEEL[2])
    clean_metal.rect(img, 0, 0, 15, 15, STEEL[0])
    clean_metal.bevel(img, 1, 1, 14, 14, STEEL[3], STEEL[1], STEEL[2])
    if top:
        clean_metal.inset(img, 4, 4, 11, 6, STEEL[3], STEEL[0])
        clean_metal.rect(img, 5, 5, 10, 5, (20, 22, 26))
        for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
            clean_metal.bolt(img, x, y, STEEL)
        for x in range(3, 13):
            clean_metal.put(img, x, 10, STEEL[1])
            clean_metal.put(img, x, 11, STEEL[3])
    else:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 5.5)
                if 3.4 < d < 4.6:
                    clean_metal.put(img, x, y, PAINT[3] if y < 5 else PAINT[1])
                elif d <= 1.2:
                    clean_metal.put(img, x, y, PAINT[2])
        for x, y in ((7, 2), (8, 2), (4, 5), (11, 5), (7, 9), (8, 9)):
            clean_metal.put(img, x, y, PAINT[2])
        for y in range(12, 15):
            for x in range(2, 14):
                clean_metal.put(img, x, y, (28, 26, 24) if (x + y) % 4 < 2 else PAINT[2])
        for x, y in ((2, 9), (12, 9)):
            clean_metal.bolt(img, x, y, STEEL)
    return img


TIMBER = [(70, 50, 32), (92, 68, 44), (112, 84, 54), (132, 100, 66)]


def bunker_door(top):
    """The bunker door's halves: vertical spruce boards in a frame, crossed by two black iron straps with bolt heads; the
    top half has a small viewing hatch, the bottom a ring pull."""
    img = clean_metal.canvas(TIMBER[2])
    for x in range(16):
        for y in range(16):
            if x % 4 == 0:
                clean_metal.put(img, x, y, TIMBER[0])
            elif x % 4 == 1:
                clean_metal.put(img, x, y, TIMBER[3])
    clean_metal.rect(img, 0, 0, 15, 0, TIMBER[0])
    clean_metal.rect(img, 0, 15, 15, 15, TIMBER[0])
    for y0 in ((3, 12) if top else (3, 11)):
        for x in range(1, 15):
            clean_metal.put(img, x, y0, STEEL[0])
            clean_metal.put(img, x, y0 + 1, STEEL[1])
        for x in (2, 7, 12):
            clean_metal.put(img, x, y0, STEEL[4])
    if top:
        clean_metal.rect(img, 6, 6, 9, 9, STEEL[0])
        clean_metal.rect(img, 7, 7, 8, 8, (24, 22, 20))
    else:
        for x, y in ((11, 6), (12, 6), (10, 7), (13, 7), (11, 8), (12, 8)):
            clean_metal.put(img, x, y, STEEL[3])
    return img


def icon(kind):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    if kind == "steel_ladder":
        return ladder()
    if kind == "bunker_door":
        clean_metal.rect(img, 4, 1, 11, 14, TIMBER[2])
        for x in (4, 8):
            clean_metal.rect(img, x, 1, x, 14, TIMBER[0])
        for y in (3, 11):
            clean_metal.rect(img, 4, y, 11, y, STEEL[0])
        clean_metal.rect(img, 7, 6, 8, 7, (24, 22, 20))
        return img
    if kind == "blast_door":
        clean_metal.rect(img, 4, 1, 11, 14, STEEL[2])
        clean_metal.bevel(img, 4, 1, 11, 14, STEEL[3], STEEL[1], STEEL[2])
        clean_metal.rect(img, 6, 3, 9, 3, (20, 22, 26))
        for x, y in ((7, 8), (8, 8), (6, 9), (9, 9), (7, 10), (8, 10)):
            clean_metal.put(img, x, y, PAINT[2])
        clean_metal.rect(img, 5, 13, 10, 13, PAINT[2])
    return img


def draw_all(save):
    for name, img in (("fw_bastion", bastion()), ("fw_bastion_top", bastion_top()), ("fw_ladder", ladder()),
                      ("fw_chain", chain()), ("fw_blast_door_bottom", blast_door(False)),
                      ("fw_blast_door_top", blast_door(True)), ("fw_bunker_door_bottom", bunker_door(False)),
                      ("fw_bunker_door_top", bunker_door(True))):
        save(img, "block", name)
    for item in ("steel_ladder", "blast_door", "bunker_door"):
        save(icon(item), "item", item)
