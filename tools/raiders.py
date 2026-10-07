"""The raider faction (batch 57, docs/features/raiders.md): dieselpunk raiders who come for players' bases.

- Infantry: the Raider Grunt (a cleaver), the Raider Grenadier (lobs small grenades from range) and the Raider Officer
  (rallies the raiders near them; when they fall, the rest lose heart).
- The Raider Walker: the Armoured Walker (batch 58, the owner's model) in raider paint. It wades in and rams with its
  piston, and lobs grenades from its hull gun.
- The Raider Blimp: a small airship that cruises over its target and drops bombs. Flak brings it down.
- Raids: now and then a party marches on a player's base (or the town, if they are in it). It shows a bar, ends when
  every raider has fallen, and the next is a little stronger; if nobody fights it, it withdraws.

Every blast is a damage-only weapons/Blast: raiders never break, move or burn a block. They are hostile mobs, so sentry
guns (batch 56) and town guards fight them. Raids can be switched off (raiders.enabled=false, or raiders.raids=off).

Art is original: the uniforms (player skin layout, drawn with tools/town_skins.py's helpers), the raider paint, canvas
and the insignia, all in the clean style. The walker and blimp reuse the Armoured Walker's and the Zeppelin's shapes
(tools/armoured_walker.py, tools/zeppelin.py) in raider paint, the blimp at a little over half the zeppelin's size; they are
exported to assets/jugcraft/raider_quads.json. tools/check_mod_data.py keeps the Java numbers the same as these.
"""
import json
from pathlib import Path

from PIL import Image

import clean_metal
import town_skins as ts

MOD = "jugcraft"
FEATURE = "raiders"

# id: (display name, max health, attack damage, armour, movement speed). The officer and grunts fight hand to hand;
# the grenadier keeps its distance.
INFANTRY = {
    "raider_grunt": ("Raider Grunt", 24, 5, 4, 0.30),
    "raider_grenadier": ("Raider Grenadier", 20, 3, 2, 0.28),
    "raider_officer": ("Raider Officer", 32, 6, 6, 0.30),
}
# The walker and the blimp: (display name, max health, attack damage, armour, movement speed, width, height).
MACHINES = {
    "raider_walker": ("Raider Walker", 120, 14, 14, 0.22, 2.6, 4.6),
    "raider_blimp": ("Raider Blimp", 50, 0, 2, 0.12, 2.8, 4.4),
}
BOMB = "raider_bomb"
INSIGNIA = "raider_insignia"

# Grenades and bombs: (blast radius, damage). Smaller than a player's grenade (radius 4, 16).
GRENADE_BLAST = (2.5, 6.0)
BOMB_BLAST = (3.5, 10.0)
# The grenadier: ticks between throws and the range it throws from.
GRENADE_COOLDOWN = 70
GRENADE_MIN_RANGE = 6
GRENADE_MAX_RANGE = 18
# The officer's rally: every RALLY_TICKS it gives raiders within RALLY_RADIUS Speed and Strength for RALLY_EFFECT ticks.
# When an officer falls, raiders within RALLY_RADIUS * 4 / 3 lose heart: Weakness for ROUT_TICKS.
RALLY_TICKS = 40
RALLY_RADIUS = 12
RALLY_EFFECT = 60
ROUT_TICKS = 200
# The walker: ticks between punches and between launcher shots, and the launcher's range.
WALKER_PUNCH_COOLDOWN = 20
WALKER_LAUNCH_COOLDOWN = 100
WALKER_LAUNCH_MIN = 8
WALKER_LAUNCH_MAX = 24
# The blimp: how high over its target it cruises, ticks between bombs, and how close overhead (blocks) it must be.
BLIMP_CRUISE = 16
BLIMP_BOMB_COOLDOWN = 50
BLIMP_BOMB_REACH = 3
# Siege ladders: how long a grunt must be stuck against a wall (ticks), the tallest it climbs, and how long a ladder
# lasts before it crumbles (ticks). Only where mob griefing is on; they drop nothing.
LADDER_STUCK = 40
LADDER_MAX = 8
LADDER_TTL = 1200

# Raids. The server looks every RAID_CHECK_TICKS; a player is raided only after GRACE days of play, at most once every
# INTERVAL days (each per world, text options raiders.grace_days and raiders.interval_days), RAID_CHANCE of the
# checks after that. The party gathers SPAWN_MIN to SPAWN_MAX blocks away and marches on the target. A raid that
# lasts RAID_TIMEOUT ticks, or has nobody within ABANDON_RANGE blocks for ABANDON_TICKS, withdraws.
RAID_CHECK_TICKS = 1200
RAID_CHANCE = 0.2
SPAWN_MIN = 48
SPAWN_MAX = 64
RAID_TIMEOUT = 12000
ABANDON_TICKS = 2400
ABANDON_RANGE = 160
MAX_LEVEL = 5
# Raider camps: one chunk in CAMP_RARITY (in the plains, savanna and badlands), on flat ground only.
CAMP_RARITY = 400
# No camp this close (blocks) to the world spawn, so a new player does not start beside one.
CAMP_SPAWN_CLEARANCE = 512
CAMP_LOOT = [("minecraft:gunpowder", 1, 4), ("minecraft:iron_nugget", 2, 8), (f"{MOD}:heavy_shell", 1, 3),
             (f"{MOD}:grenade", 0, 2), ("minecraft:bread", 1, 3), (f"{MOD}:steel_plate", 0, 2)]
# Text options and their defaults (config/JugcraftConfig.java).
OPTIONS = {"raiders.raids": "on", "raiders.grace_days": "3", "raiders.interval_days": "3", "raiders.walkers": "on",
           "raiders.blimps": "on"}


def party(level):
    """Who comes at a raid level (1 to MAX_LEVEL): the counts of each kind. Kept the same in raiders/RaiderRaids.java."""
    return {"raider_grunt": 2 + level, "raider_grenadier": (level + 1) // 2, "raider_officer": 1,
            "raider_blimp": 0 if level < 2 else 1 if level < 5 else 2, "raider_walker": 0 if level < 3 else 1}


ENTITIES = {**{k: v[0] for k, v in INFANTRY.items()}, **{k: v[0] for k, v in MACHINES.items()}, BOMB: "Raider Bomb"}
ITEMS = {INSIGNIA: "Raider Insignia", "raid_horn": "Raider War Horn"}
BLOCKS = {"siege_ladder": "Siege Ladder"}
TOOLTIPS = {INSIGNIA: "Torn from a raider officer's sleeve: proof of a raid beaten off. Three make a Raider War Horn.",
            "raid_horn": "Blow it to call a raid on your base now, at the world's raid level. Used up when it brings one."}
LANG = {
    "event.jugcraft.raid": "Raid (level %s)",
    "message.jugcraft.raid.coming": "Raiders are coming! Their engines rumble to the %s.",
    "message.jugcraft.raid.won": "The raid is beaten off. The next will be stronger.",
    "message.jugcraft.raid.withdrawn": "The raiders withdraw.",
    "message.jugcraft.raid.dir.north": "north", "message.jugcraft.raid.dir.south": "south",
    "message.jugcraft.raid.dir.east": "east", "message.jugcraft.raid.dir.west": "west",
    "message.jugcraft.raid_horn.off": "Raids are switched off on this server",
    "message.jugcraft.raid_horn.dimension": "Only the Overworld's raiders answer the horn",
    "message.jugcraft.raid_horn.peaceful": "No raiders come in peaceful",
    "message.jugcraft.raid_horn.under_way": "A raid is already under way",
    "message.jugcraft.raid_horn.no_room": "The raiders found nowhere near to gather",
    "advancements.jugcraft.raid_beaten.title": "Beat Them Back",
    "advancements.jugcraft.raid_beaten.description": "See a raid through until every raider has fallen",
}

# Loot: (item, min, max, player kill only). Small: a raid comes at most every few days, and costs the fight.
LOOT = {
    "raider_grunt": [("minecraft:iron_nugget", 0, 3, False)],
    "raider_grenadier": [("minecraft:gunpowder", 0, 2, False)],
    "raider_officer": [(f"{MOD}:{INSIGNIA}", 1, 1, True), ("minecraft:iron_ingot", 0, 1, False)],
    "raider_walker": [(f"{MOD}:steel_plate", 2, 4, False), (f"{MOD}:steel_gear", 1, 2, False)],
    "raider_blimp": [(f"{MOD}:rubber", 1, 3, False), ("minecraft:string", 2, 5, False)],
}

# Raider paint for the walker and blimp, by the textures the Diesel Walker and the Zeppelin use.
REPAINT = {
    "dr_red": "rd_paint", "dr_rust": "rd_plate", "dr_rust_bare": "rd_plate", "dr_ribbed_rust": "rd_plate",
    "dr_blue": "rd_paint", "dz_canvas": "rd_canvas", "dz_canvas_stripe": "rd_canvas_stripe",
    "dz_canvas_nose": "rd_canvas_nose",
    # The Armoured Walker (batch 58, the owner's model), which the raiders field as their walker.
    "aw_plate": "rd_paint", "aw_plate_seam": "rd_paint", "aw_plate_dark": "rd_plate", "aw_leg": "rd_plate",
    "aw_canvas": "rd_canvas",
}
BLIMP_SCALE = 0.55


def repaint(quads, scale=1.0):
    out = []
    for quad in quads:
        q = dict(quad)
        q["texture"] = REPAINT.get(quad["texture"], quad["texture"])
        q["vertices"] = [[round(v[0] * scale, 4), round(v[1] * scale, 4), round(v[2] * scale, 4), v[3], v[4]]
                         for v in quad["vertices"]]
        out.append(q)
    return out


def export():
    import armoured_walker
    import zeppelin
    walker = armoured_walker.export()
    blimp = zeppelin.export()
    out = {name.replace("armoured_walker", "raider_walker"): repaint(quads) for name, quads in walker.items()}
    out["raider_blimp_body"] = repaint(blimp["zeppelin_body"], BLIMP_SCALE)
    out["raider_blimp_propeller"] = repaint(blimp["zeppelin_propeller"], BLIMP_SCALE)
    return out


def write_all(write, assets, data, lang, condition):
    for entity, name in ENTITIES.items():
        lang[f"entity.{MOD}.{entity}"] = name
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang.update(LANG)
    for entity, drops in LOOT.items():
        pools = []
        for item, low, high, player in drops:
            entry = {"type": "minecraft:item", "name": item}
            if high > 1 or low != 1:
                entry["modifier"] = {"type": "minecraft:set_count",
                                     "count": {"type": "minecraft:uniform", "min": low, "max": high}}
            pool = {"rolls": 1, "entries": [entry]}
            if player:
                pool["condition"] = {"type": "minecraft:killed_by_player"}
            pools.append(pool)
        write(data / "loot_table" / "entities" / f"{entity}.json",
              {"type": "minecraft:entity", "pools": pools, "random_sequence": f"{MOD}:entities/{entity}"})
    for block, name in BLOCKS.items():
        lang[f"block.{MOD}.{block}"] = name
        ref = f"{MOD}:block/{block}"
        write(assets / "models" / "block" / f"{block}.json",
              {"parent": "minecraft:block/ladder", "textures": {"texture": f"{MOD}:block/rd_ladder", "particle": f"{MOD}:block/rd_ladder"}})
        write(assets / "blockstates" / f"{block}.json", {"variants": {
            f"facing={f}": ({"model": ref, "y": y} if y else {"model": ref})
            for f, y in {"north": 0, "east": 90, "south": 180, "west": 270}.items()}})
    write(data / "loot_table" / "chests" / "raider_camp.json", {
        "type": "minecraft:chest", "random_sequence": f"{MOD}:chests/raider_camp",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": item,
                                            "modifier": {"type": "minecraft:set_count",
                                                         "count": {"type": "minecraft:uniform", "min": low, "max": high}}}]}
                  for item, low, high in CAMP_LOOT]})
    write(data / "recipe" / "raid_horn.json", {
        "fabric:load_conditions": condition(FEATURE), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["III", " H "], "key": {"I": f"{MOD}:{INSIGNIA}", "H": "minecraft:goat_horn"},
        "result": {"id": f"{MOD}:raid_horn", "count": 1}})
    write(data / "advancement" / "raid_beaten.json", {
        "parent": "minecraft:adventure/root",
        "display": {"icon": {"id": f"{MOD}:{INSIGNIA}"}, "title": {"translate": "advancements.jugcraft.raid_beaten.title"},
                    "description": {"translate": "advancements.jugcraft.raid_beaten.description"},
                    "frame": "challenge", "show_toast": True, "announce_to_chat": True},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
    # Written compact: it is thousands of quads.
    (assets / "raider_quads.json").write_text(json.dumps(export(), separators=(",", ":")) + "\n", encoding="utf-8")


# ------------------------------------------------------------------ uniforms (player skin layout)

OLIVE = (84, 88, 58)
OLIVE_DARK = (62, 64, 42)
LEATHER = (92, 64, 40)
BLACK = (38, 38, 40)
STEEL = (120, 124, 130)
RED = (168, 40, 34)
BRASS = (196, 160, 70)
GLASS = (150, 200, 170)


def goggles(s):
    """Round brass-rimmed goggles over the eyes and a strap round the head."""
    for i in range(8):
        for side in ("front", "back", "right", "left"):
            s.dot("hat", side, i, 3, LEATHER)
    for i in (1, 2, 5, 6):
        s.dot("hat", "front", i, 3, GLASS)
        s.dot("hat", "front", i, 4, ts.darker(GLASS, 0.8))
    for i in (0, 3, 4, 7):
        s.dot("hat", "front", i, 4, BRASS)
    s.dot("hat", "front", 0, 3, BRASS)
    s.dot("hat", "front", 7, 3, BRASS)


def respirator(s):
    """A black rubber respirator over mouth and nose, with a round filter."""
    for i in range(2, 6):
        for j in (5, 6, 7):
            s.dot("hat", "front", i, j, BLACK)
    s.dot("hat", "front", 3, 6, STEEL)
    s.dot("hat", "front", 4, 6, STEEL)
    for side in ("right", "left"):
        s.dot("hat", side, 0 if side == "left" else 7, 6, BLACK)


def helmet(colour):
    def draw(s):
        s.fill("hat", colour, sides=["top"], noise=4)
        for side in ("front", "back", "right", "left"):
            for i in range(8):
                s.dot("hat", side, i, 0, colour)
                s.dot("hat", side, i, 1, ts.darker(colour, 0.85))
        for i in range(8):
            s.dot("hat", "front", i, 2, ts.darker(colour, 0.7))
    return draw


def greatcoat(colour, trim):
    """A long coat with a belt and buttons, sleeves to the wrist and the skirt hanging over the thighs."""
    def draw(s, rng, skin, hair):
        ts.tunic(colour, OLIVE_DARK, belt=LEATHER, boots=BLACK)(s, rng, skin, hair)
        for leg in ("right_pants", "left_pants"):
            s.fill(leg, colour, sides=["front", "back", "right", "left"], rows=(0, 5), noise=4)
        for j in (2, 4, 9, 11):
            s.dot("jacket", "front", 3, j, BRASS)
        for j in range(12):
            s.dot("jacket", "front", 4, j, ts.darker(colour, 0.8))
        s.fill("jacket", trim, sides=["top"])
        for i in range(8):
            s.dot("jacket", "front", i, 0, trim)
    return draw


def armband(s):
    for j in (3, 4):
        for side in ("front", "back", "right", "left"):
            for i in range(4):
                s.dot("left_sleeve", side, i, j, RED)
    s.dot("left_sleeve", "right", 1, 3, BLACK)
    s.dot("left_sleeve", "right", 2, 4, BLACK)


def bandolier(s):
    """A leather strap across the chest carrying grenades."""
    for j in range(12):
        i = j * 7 // 11
        s.dot("jacket", "front", i, j, LEATHER)
        if j % 3 == 1:
            s.dot("jacket", "front", min(7, i + 1), j, OLIVE_DARK)
        s.dot("jacket", "back", 7 - i, j, LEATHER)


def peaked_cap(s):
    s.fill("hat", BLACK, sides=["top"], noise=3)
    for side in ("back", "right", "left"):
        for i in range(8):
            s.dot("hat", side, i, 0, BLACK)
            s.dot("hat", side, i, 1, RED)
    for i in range(8):
        s.dot("hat", "front", i, 0, BLACK)
        s.dot("hat", "front", i, 1, RED)
        s.dot("hat", "front", i, 2, ts.darker(BLACK, 0.7))
    s.dot("hat", "front", 3, 1, BRASS)
    s.dot("hat", "front", 4, 1, BRASS)


SKINS = {
    "raider_grunt": (71, ts.combine(greatcoat(OLIVE, OLIVE_DARK), helmet((70, 74, 66)), goggles, respirator)),
    "raider_grenadier": (72, ts.combine(greatcoat((96, 84, 60), LEATHER), helmet(LEATHER), goggles, bandolier)),
    "raider_officer": (73, ts.combine(greatcoat(BLACK, RED), peaked_cap, armband)),
}


def add_tags(tags):
    for block in BLOCKS:
        tags.add("block", "minecraft:climbable", f"{MOD}:{block}")
        tags.add("block", "minecraft:mineable/axe", f"{MOD}:{block}")


def write_skins(out):
    out.mkdir(parents=True, exist_ok=True)
    for name, (seed, outfit) in SKINS.items():
        ts.person(seed, outfit).save(out / f"{name}.png")


# ------------------------------------------------------------------ paint (clean style)

PAINT = [(52, 56, 40), (66, 70, 50), (80, 86, 62), (98, 104, 76)]
PLATE = [(30, 31, 34), (44, 46, 50), (60, 62, 66), (80, 82, 88)]
CANVAS = [(54, 54, 52), (66, 66, 64), (78, 78, 74), (92, 92, 88)]
STRIPE = [(120, 30, 26), (156, 42, 34), (186, 60, 48)]


def paint():
    """Olive drab: a flat panel with a lit top edge and two rivets."""
    img = clean_metal.canvas(PAINT[2])
    clean_metal.bevel(img, 0, 0, 15, 15, PAINT[3], PAINT[0], PAINT[2])
    clean_metal.scuffs(img, PAINT[3], [(4, 6, 3), (10, 11, 2)], only=PAINT[2])
    for x, y in ((2, 2), (13, 13)):
        clean_metal.put(img, x, y, PAINT[0])
    return img


def plate():
    """Dark armour plate with a rivet in each corner."""
    img = clean_metal.canvas(PLATE[2])
    clean_metal.bevel(img, 0, 0, 15, 15, PLATE[3], PLATE[0], PLATE[2])
    clean_metal.corner_bolts(img, [PLATE[0], PLATE[1], PLATE[2], PLATE[3], PLATE[3], (110, 112, 118)])
    return img


def canvas(stripe=False, nose=False):
    """Charcoal envelope canvas: seams every four pixels; the stripe adds the raiders' red band, the nose a red cap."""
    img = clean_metal.canvas(CANVAS[2])
    for y in range(16):
        for x in range(16):
            if x % 4 == 0:
                clean_metal.put(img, x, y, CANVAS[1])
            elif x % 4 == 1:
                clean_metal.put(img, x, y, CANVAS[3])
    if stripe:
        for y in range(5, 11):
            for x in range(16):
                clean_metal.put(img, x, y, STRIPE[1] if 6 <= y <= 9 else STRIPE[0])
        for x in range(16):
            clean_metal.put(img, x, 6, STRIPE[2])
    if nose:
        for y in range(16):
            for x in range(16):
                clean_metal.put(img, x, y, STRIPE[1] if (x + y) % 8 else STRIPE[0])
    return img


def insignia():
    """A red cloth patch with a black cog and a white lightning bolt."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(2, 14):
        for x in range(3, 13):
            clean_metal.put(img, x, y, STRIPE[1] if (x, y) not in ((3, 2), (12, 2), (3, 13), (12, 13)) else STRIPE[0])
    for x in range(3, 13):
        clean_metal.put(img, x, 2, STRIPE[2])
        clean_metal.put(img, x, 13, STRIPE[0])
    for y, x0, x1 in ((4, 7, 8), (5, 5, 10), (6, 5, 10), (7, 4, 11), (8, 4, 11), (9, 5, 10), (10, 5, 10), (11, 7, 8)):
        for x in range(x0, x1 + 1):
            clean_metal.put(img, x, y, BLACK)
    for x, y in ((9, 5), (8, 6), (7, 7), (8, 7), (9, 7), (8, 8), (7, 9), (6, 10)):
        clean_metal.put(img, x, y, (236, 232, 220))
    return img


def ladder():
    """Rough raider poles and lashed rungs, see-through between them."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wood = [(70, 52, 34), (92, 70, 46), (114, 88, 58)]
    for y in range(16):
        for x, c in ((1, wood[2]), (2, wood[1]), (13, wood[2]), (14, wood[0])):
            clean_metal.put(img, x, y, c)
    for y0 in (2, 7, 12):
        for x in range(3, 13):
            clean_metal.put(img, x, y0, wood[2])
            clean_metal.put(img, x, y0 + 1, wood[0])
        for x in (2, 13):
            clean_metal.put(img, x, y0, STRIPE[0])
    return img


def horn():
    """The war horn: a curled horn bound in red cloth with a brass mouthpiece."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bone = [(150, 136, 110), (186, 170, 140), (214, 200, 170)]
    for i, (x, y) in enumerate(((3, 12), (4, 11), (5, 10), (6, 9), (7, 8), (8, 7), (9, 6), (10, 5), (11, 4), (12, 3))):
        r = 1 + i // 3
        for dx in range(-r, r + 1):
            clean_metal.put(img, min(15, max(0, x + dx)), y, bone[1] if dx < r else bone[0])
        clean_metal.put(img, x, y - 1, bone[2])
    for x, y in ((6, 9), (7, 9), (8, 8), (7, 8)):
        clean_metal.put(img, x, y, STRIPE[1])
    for x, y in ((2, 13), (3, 13)):
        clean_metal.put(img, x, y, BRASS)
    return img


def draw_all(save):
    save(ladder(), "block", "rd_ladder")
    save(horn(), "item", "raid_horn")
    save(paint(), "block", "rd_paint")
    save(plate(), "block", "rd_plate")
    save(canvas(), "block", "rd_canvas")
    save(canvas(stripe=True), "block", "rd_canvas_stripe")
    save(canvas(nose=True), "block", "rd_canvas_nose")
    save(insignia(), "item", INSIGNIA)
    write_skins(Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / MOD / "textures"
                / "entity" / "raider")
