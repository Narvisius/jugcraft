"""JSON resources for the Bat House (fall additions 13), from tools/agriculture.py: the house's model (a slatted roost
facing out from a wall, its tray below with the guano piling up), its blockstate (facing, and how much guano shows), the
guano item, names, messages, loot (the house drops itself; its bats fly out and its guano spills when it is broken) and
the axe tag. The recipes are in SHAPED and SHAPELESS.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import BATS
from decor_data import MOD, rid, box, block_model, self_drop, turned, flat_item

TEXTURES = {"wood": "bat_house", "slats": "bat_house_slats", "guano": "bat_guano_pile"}
PILES = [((5, 1, 11), (7, 1.5, 13)), ((9, 1, 12), (11, 1.75, 14)), ((6.5, 1, 13), (9, 2.25, 14.5))]

TEXT = {
    "message.jugcraft.bat_house.status": "Bat House: %s of %s bats roosting",
    "message.jugcraft.bat_house.scooped": "Bat House: %s of %s bats roosting. You scoop up %s guano",
}


def house(guano):
    """A bat house facing north, its back against the wall to the south: a back board, a roof, two sides, a slatted front
    open at the bottom for the bats, and a tray below with `guano` piles on it."""
    w, s, g = "#wood", "#slats", "#guano"
    elements = [box((2, 0, 15), (14, 15, 16), w), box((1.5, 14, 9), (14.5, 15.5, 16), w),
                box((2, 2, 10), (3, 14, 15), w), box((13, 2, 10), (14, 14, 15), w),
                box((3, 5, 9.5), (13, 14, 10), s, textures={"north": s}),
                box((3, 0, 10), (13, 1, 15), w)]
    elements += [box(lo, hi, g) for lo, hi in PILES[:guano]]
    return block_model(TEXTURES, elements, TEXTURES["wood"])


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    name = BATS["house"]
    for guano in range(4):
        write(models / f"{name}{'' if guano == 0 else f'_guano_{guano}'}.json", house(guano))
    write(states / f"{name}.json", {"variants": {
        f"facing={f},guano={g}": turned(rid(f"block/{name}" + ("" if g == 0 else f"_guano_{g}")), f)
        for f in ("north", "south", "east", "west") for g in range(4)}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
    lang[f"block.{MOD}.{name}"] = BATS["house_display"]
    flat_item(root, write, BATS["guano"])
    lang[f"item.{MOD}.{BATS['guano']}"] = BATS["guano_display"]
    lang.update(TEXT)


def loot(out, write):
    write(out / f"{BATS['house']}.json", self_drop(BATS["house"]))


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(BATS["house"]))
