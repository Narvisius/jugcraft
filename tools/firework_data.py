"""JSON resources for spooky fireworks (fall additions 5), from tools/agriculture.py: the four spooky firework items and
their recipes (paper, one to three gunpowder for the flight, the picture's ingredients, and glowstone dust to twinkle),
the Show Launcher (a model for each mode, its blockstates, item, loot and recipe), the spark particle, names, tooltips
and messages.

Called from agriculture_data.py (assets, loot, recipes, tags). Formats follow vanilla Minecraft 26.3's own files. The
rockets peeking out of the launcher's tubes are drawn by the client's ShowLauncherRenderer; the bursts by SpookyBursts.
"""
from agriculture import FIREWORKS
from decor_data import MOD, rid, box, block_model, turned, self_drop, flat_item

MODES = ("sequence", "volley", "finale")
TUBE_AT = (1, 6, 11)


def launcher_model(mode):
    """A crate of dark stained planks with iron corners, nine painted mortar tubes standing in it (three rows of three),
    and on its front (north) a brass dial set to `mode`."""
    c, t, h, d = "#crate", "#tube", "#hole", "#dial"
    whole = {"north": [0, 0, 16, 16], "south": [0, 0, 16, 16], "east": [0, 0, 16, 16], "west": [0, 0, 16, 16], "up": [0, 0, 16, 16]}
    elements = [box((0, 0, 0), (16, 6, 16), c, textures={"up": "#crate_top"})]
    for x in TUBE_AT:
        for z in TUBE_AT:
            elements.append(box((x, 6, z), (x + 4, 14, z + 4), t, faces=["north", "south", "east", "west", "up"], textures={"up": h}, uvs=whole))
    elements.append(box((5, 1, -0.5), (11, 5, 0), d, faces=["north", "east", "west", "up", "down"],
                        uvs={"north": [0, 0, 16, 16], "east": [0, 0, 1, 16], "west": [0, 0, 1, 16], "up": [0, 0, 16, 1], "down": [0, 0, 16, 1]}))
    return block_model({"crate": "show_launcher_crate", "crate_top": "show_launcher_crate_top", "tube": "show_launcher_tube",
                        "hole": "show_launcher_tube_top", "dial": f"show_launcher_dial_{mode}"}, elements, "show_launcher_crate")


TEXT = {
    "tooltip.jugcraft.spooky_firework.bat": "Bursts into a bat",
    "tooltip.jugcraft.spooky_firework.pumpkin": "Bursts into a jack o'lantern",
    "tooltip.jugcraft.spooky_firework.ghost": "Bursts into a ghost",
    "tooltip.jugcraft.spooky_firework.skull": "Bursts into a skull",
    "tooltip.jugcraft.spooky_firework.twinkle": "Twinkles",
    "message.jugcraft.show_launcher.full": "The tubes are full",
    "message.jugcraft.show_launcher.started": "The show begins!",
    "message.jugcraft.show_launcher.stopped": "The show stops",
    "message.jugcraft.show_launcher.empty": "Load some rockets first",
    "message.jugcraft.show_launcher.mode.sequence": "Firing in sequence: one rocket at a time",
    "message.jugcraft.show_launcher.mode.volley": "Firing in volleys: a row of three at once",
    "message.jugcraft.show_launcher.mode.finale": "Firing a finale: one from every tube at once",
}


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"
    for shape, info in FIREWORKS["shapes"].items():
        flat_item(root, write, info["item"])
        lang[f"item.{MOD}.{info['item']}"] = info["display"]
    launcher = FIREWORKS["launcher"]
    for mode in MODES:
        write(models / f"{launcher}_{mode}.json", launcher_model(mode))
    write(states / f"{launcher}.json", {"variants": {f"facing={f},mode={m}": turned(rid(f"block/{launcher}_{m}"), f)
                                                     for f in ("north", "south", "east", "west") for m in MODES}})
    write(items / f"{launcher}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{launcher}_sequence")}})
    lang[f"block.{MOD}.{launcher}"] = FIREWORKS["launcher_display"]
    lang[f"entity.{MOD}.{FIREWORKS['entity']}"] = "Spooky Firework"
    write(root / "particles" / f"{FIREWORKS['particle']}.json", {"textures": [rid(FIREWORKS["particle"])]})
    lang.update(TEXT)


def loot(out, write):
    write(out / f"{FIREWORKS['launcher']}.json", self_drop(FIREWORKS["launcher"]))


def recipes(out, write, conditions):
    """Each picture, at each flight (1 to 3 gunpowder), plain and twinkling (with glowstone dust): three rockets a craft."""
    for shape, info in FIREWORKS["shapes"].items():
        for flight in FIREWORKS["flights"]:
            for twinkle in (False, True):
                inputs = ["minecraft:paper"] + ["minecraft:gunpowder"] * flight + list(info["ingredients"])
                components = {"minecraft:fireworks": {"flight_duration": flight}}
                if twinkle:
                    inputs.append(FIREWORKS["twinkle"])
                    components[rid(FIREWORKS["component"])] = True
                name = f"{info['item']}_{flight}" + ("_twinkle" if twinkle else "")
                write(out / f"{name}.json", {"fabric:load_conditions": conditions(), "type": "minecraft:crafting_shapeless",
                                             "category": "misc", "group": info["item"], "ingredients": inputs,
                                             "result": {"id": rid(info["item"]), "count": FIREWORKS["per_craft"], "components": components}})
    write(out / f"{FIREWORKS['launcher']}.json", {
        "fabric:load_conditions": conditions(), "type": "minecraft:crafting_shaped", "category": "redstone",
        "pattern": ["III", "PDP", "PRP"],
        "key": {"I": "minecraft:iron_ingot", "P": "#minecraft:planks", "D": "minecraft:dispenser", "R": "minecraft:redstone"},
        "result": {"id": rid(FIREWORKS["launcher"]), "count": 1}})


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(FIREWORKS["launcher"]))
