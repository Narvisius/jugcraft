"""JSON resources for the candy kitchen (fall additions 11), from tools/agriculture.py: the Candy Kettle's model (a copper
sugar pot with a thermometer dial clipped to its front) and blockstate, the Candy Tray's models (empty, and with candy on
it in the candy's colour), the candies' tinted models (candy corn's three bands each take a colour of their own), their
names, the stages', bases' and flavours' names, the tooltips and messages, loot, and the tags that say what flavours a
batch and what counts as candy.

Called from agriculture_data.py (assets, after the plain food items so candy corn's tinted model replaces its plain one;
loot; tags). Formats follow vanilla Minecraft 26.3's own files. The syrup in the kettle and the thermometer's needle are
drawn by the client's CandyKettleRenderer.
"""
from agriculture import CANDY
from decor_data import MOD, rid, box, block_model, self_drop, turned

KETTLE_TEXTURES = {"copper": "candy_kettle", "inside": "candy_kettle_inside", "dial": "candy_dial", "trivet": "candy_trivet"}
# Candies drawn in two layers: the candy (tinted by its colour) and an untinted part (a stick, a wrapper, a shine).
DETAIL = {"rock_candy": "rock_candy_stick", "salt_water_taffy": "salt_water_taffy_wrapper", "hard_candy": "hard_candy_shine",
          "lollipop": "lollipop_stick", "cream_caramel": "cream_caramel_wrapper"}


def argb(color):
    """An opaque colour as the signed int item model tints take."""
    return (0xFF000000 | color) - (1 << 32)


def kettle():
    """A copper sugar pot facing north on an iron trivet: walls a pixel thick, a rolled rim, two side handles, and on its
    front a round candy thermometer dial on a clip hooked over the rim."""
    c, i, d = "#copper", "#inside", "#dial"
    elements = [box((2, 0, 2), (14, 1, 14), c, textures={"up": i}),
                box((2, 1, 2), (14, 10, 3), c, textures={"south": i}), box((2, 1, 13), (14, 10, 14), c, textures={"north": i}),
                box((2, 1, 3), (3, 10, 13), c, textures={"east": i}), box((13, 1, 3), (14, 10, 13), c, textures={"west": i}),
                box((1.5, 9.5, 1.5), (14.5, 10.5, 2.5), c), box((1.5, 9.5, 13.5), (14.5, 10.5, 14.5), c),
                box((1.5, 9.5, 2.5), (2.5, 10.5, 13.5), c), box((13.5, 9.5, 2.5), (14.5, 10.5, 13.5), c),
                box((0.5, 6, 7), (2, 7, 9), c), box((0.5, 7, 7), (1.5, 9, 9), c),
                box((14, 6, 7), (15.5, 7, 9), c), box((14.5, 7, 7), (15.5, 9, 9), c),
                # The dial (its face to the north), its clip, and the hook over the rim.
                box((5.5, 4, 1), (10.5, 9, 1.5), c, textures={"north": d}, uvs={"north": (0, 0, 16, 16)}),
                box((7.5, 9, 1.25), (8.5, 11, 1.75), c), box((7.5, 10.5, 1.75), (8.5, 11, 3), c)]
    # An iron trivet under it, its legs reaching down to the fire it stands over (a campfire is 7 pixels tall).
    for x, z in ((3, 3), (12, 3), (3, 12), (12, 12)):
        elements.append(box((x, -9, z), (x + 1, 0, z + 1), "#trivet", uvs={side: (x, 7, x + 1, 16) for side in ("north", "south", "east", "west")}))
    elements.append(box((3, -0.5, 3), (13, 0, 13), "#trivet", faces=("down",)))
    return block_model(KETTLE_TEXTURES, elements, KETTLE_TEXTURES["copper"])


def candy_item(root, write, name):
    """A candy's model: its pale layer tinted by its dyed colour (its own colour by default), over or under its detail."""
    color = CANDY["kinds"][name]
    layers = {"layer0": rid(f"item/{name}")}
    if name in DETAIL:
        layers["layer1"] = rid(f"item/{DETAIL[name]}")
    write(root / "models" / "item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": layers})
    model = {"type": "minecraft:model", "model": rid(f"item/{name}")}
    if name != "burnt_sugar":
        model["tints"] = [{"type": "minecraft:dye", "default": argb(color)}]
    write(root / "items" / f"{name}.json", {"model": model})


TEXT = {
    "item.jugcraft.candy_tray.filled": "Tray of %s",
    "item.jugcraft.candy.one": "%s %s",
    "item.jugcraft.candy.two": "%s and %s %s",
    "tooltip.jugcraft.candy.flavour": "%s: %s for %s seconds",
    "tooltip.jugcraft.candy_tray.empty": "Pour candy onto it from a Candy Kettle",
    "tooltip.jugcraft.candy_tray.pieces": "%s pieces",
    "tooltip.jugcraft.candy_tray.layers": "%s of %s layers",
    "tooltip.jugcraft.candy_tray.pulls": "Pulled %s of %s times",
    "tooltip.jugcraft.candy_tray.rock_candy": "Its crystals grow for a day; then use it to break it into pieces",
    "tooltip.jugcraft.candy_tray.candy_corn": "Pour more candy corn on for another band of colour, then use it to break it up",
    "tooltip.jugcraft.candy_tray.salt_water_taffy": "Hold use to pull it while it's warm, or it sets hard",
    "tooltip.jugcraft.candy_tray.hard_candy": "Break it up with sticks in your other hand for lollipops",
    "tooltip.jugcraft.candy_tray.caramel": "Use it to break it up once it has set",
    "tooltip.jugcraft.candy_tray.fudge": "Use it to cut it up once it has set",
    "tooltip.jugcraft.candy_tray.cream_caramel": "Use it to cut it up once it has set",
    "tooltip.jugcraft.candy_tray.toffee": "Use it to break it up once it has set",
    "tooltip.jugcraft.candy_tray.burnt_sugar": "Burnt. Use it to chip it off the tray",
    "message.jugcraft.candy_kettle.too_hot": "It's too hot to add anything: put everything in before it boils",
    "message.jugcraft.candy_kettle.has_base": "It already holds %s",
    "message.jugcraft.candy_kettle.full_sugar": "It holds %s sugar at most",
    "message.jugcraft.candy_kettle.already": "That's already in it",
    "message.jugcraft.candy_kettle.two_flavours": "Two flavours is all a batch can carry",
    "message.jugcraft.candy_kettle.not_ready": "Not ready to set: keep it on the heat",
    "message.jugcraft.candy_kettle.nothing": "Nothing to pour: it needs a water bottle or milk, and sugar",
    "message.jugcraft.candy_kettle.tray_full": "That tray already has candy on it",
    "message.jugcraft.candy_kettle.layers_full": "Candy corn has %s layers at most",
    "message.jugcraft.candy_kettle.layered": "Another layer: %s of %s",
    "message.jugcraft.candy_kettle.tipped": "You tip the batch out",
    "message.jugcraft.candy_kettle.reading": "Candy Kettle: %s°C",
    "message.jugcraft.candy_kettle.empty": ", empty",
    "message.jugcraft.candy_kettle.stage": " (%s)",
    "message.jugcraft.candy_kettle.holds": ". %s, %s sugar",
    "message.jugcraft.candy_kettle.no_base": "No water or milk yet",
    "message.jugcraft.candy_kettle.makes": ". Pour now for %s",
    "message.jugcraft.candy_tray.crystallising": "Still crystallising: about %s minutes to go",
    "message.jugcraft.candy_tray.setting": "Still setting: %s seconds",
    "message.jugcraft.candy_tray.pull": "Pulled %s of %s times",
    "message.jugcraft.candy_tray.pulled": "Pulled! It's light and chewy: use it to cut it up",
}


def assets(root, write, lang):
    models, states, items = root / "models" / "block", root / "blockstates", root / "items"
    kettle_id, tray = CANDY["kettle"], CANDY["tray"]
    write(models / f"{kettle_id}.json", kettle())
    write(states / f"{kettle_id}.json", {"variants": {f"facing={f}": turned(rid(f"block/{kettle_id}"), f)
                                                      for f in ("north", "south", "east", "west")}})
    write(items / f"{kettle_id}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{kettle_id}")}})
    lang[f"block.{MOD}.{kettle_id}"] = CANDY["kettle_display"]

    write(root / "models" / "item" / f"{tray}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{tray}")}})
    write(root / "models" / "item" / f"{tray}_filled.json", {"parent": "minecraft:item/generated", "textures": {
        "layer0": rid(f"item/{tray}"), "layer1": rid(f"item/{tray}_candy")}})
    write(items / f"{tray}.json", {"model": {
        "type": "minecraft:condition", "property": "minecraft:has_component", "component": rid(CANDY["component"]),
        "on_true": {"type": "minecraft:model", "model": rid(f"item/{tray}_filled"),
                    "tints": [{"type": "minecraft:constant", "value": -1},
                              {"type": "minecraft:dye", "default": argb(CANDY["kinds"]["hard_candy"])}]},
        "on_false": {"type": "minecraft:model", "model": rid(f"item/{tray}")}}})
    lang[f"item.{MOD}.{tray}"] = CANDY["tray_display"]

    for candy, info in CANDY["candies"].items():
        candy_item(root, write, candy)
        lang[f"item.{MOD}.{candy}"] = info["display"]
    # Candy corn's three bands, tip to base, each tinted by a colour of its own (the kettle's layers), over a dark edge.
    write(root / "models" / "item" / "candy_corn.json", {"parent": "minecraft:item/generated", "textures": {
        "layer0": rid("item/candy_corn_tip"), "layer1": rid("item/candy_corn_middle"), "layer2": rid("item/candy_corn_base"),
        "layer3": rid("item/candy_corn_outline")}})
    write(items / "candy_corn.json", {"model": {"type": "minecraft:model", "model": rid("item/candy_corn"), "tints": [
        {"type": "minecraft:custom_model_data", "index": band, "default": argb(color)} for band, color in enumerate(CANDY["corn_bands"])]}})

    for stage, (display, _) in CANDY["stages"].items():
        lang[f"candy_stage.{MOD}.{stage}"] = display
    for base, display in CANDY["bases"].items():
        lang[f"candy_base.{MOD}.{base}"] = display
    for flavour, info in CANDY["flavours"].items():
        lang[f"candy_flavour.{MOD}.{flavour}"] = info["display"]
    lang.update(TEXT)


def loot(out, write):
    """The kettle drops itself (its batch is lost)."""
    write(out / f"{CANDY['kettle']}.json", self_drop(CANDY["kettle"]))


def tags(tags):
    tags.add("block", "minecraft:mineable/pickaxe", rid(CANDY["kettle"]))
    for candy in CANDY["candies"]:
        tags.add("item", "c:foods/candy", rid(candy))
    for flavour, info in CANDY["flavours"].items():
        for item in info["items"]:
            tags.add("item", f"jugcraft:candy_flavours/{flavour}", item)
