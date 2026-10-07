"""JSON resources for the Theremin (fall additions 19), from tools/agriculture.py: its models (a walnut cabinet on four
slender legs, its walnut lid edged in brass; the tall copper pitch antenna on a bakelite insulator at the player's right, the copper
volume loop held out at the left; a speaker grille, two bakelite knobs and the magic-eye tube in front, dark when silent
and glowing green while it plays) and blockstate; the item; names and messages; loot; and tags. The recipe is in SHAPED;
the advancement in HALLOWEEN_ADVANCEMENTS.

Called from agriculture_data.py (assets, loot, tags). Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import THEREMIN
from decor_data import MOD, rid, box, block_model, self_drop, turned

TEXT = {
    "message.jugcraft.theremin.on": "The theremin hums into life: wave a hand at its antenna",
    "message.jugcraft.theremin.off": "The theremin falls silent",
}
SIDES = ("north", "south", "east", "west", "up", "down")


def pinned(lo, hi, texture, uv, light=None):
    """A box reaching outside the block, every face reading `uv` of its texture."""
    return box(lo, hi, texture, uvs={side: uv for side in SIDES}, light=light)


def theremin(on):
    """A theremin facing north (its player stands to the north): the pitch antenna at the west (the player's right)."""
    w, f, b, c, k = "#walnut", "#front", "#brass", "#copper", "#bakelite"
    elements = [box((3, 0, 4), (4, 8, 5), w), box((12, 0, 4), (13, 8, 5), w), box((3, 0, 11), (4, 8, 12), w), box((12, 0, 11), (13, 8, 12), w),
                box((2.5, 8, 3.5), (13.5, 14, 12.5), w, textures={"north": f}),
                # The lid: walnut, edged in brass.
                box((2, 14, 3), (14, 15, 13), w, textures={side: b for side in ("north", "south", "east", "west")}),
                # The pitch antenna: an insulator, and the copper rod rising from it.
                box((3, 15, 7), (4.5, 16, 8.5), k),
                pinned((3.5, 16, 7.5), (4, 30, 8), c, (7, 1, 8, 15)),
                # The volume loop, held out level at the east side on a copper arm.
                box((13.5, 11.75, 7.5), (14, 12.25, 8.5), c),
                pinned((14, 11.75, 5.5), (17.5, 12.25, 6), c, (0, 7, 3.5, 7.5)),
                pinned((14, 11.75, 10), (17.5, 12.25, 10.5), c, (0, 7, 3.5, 7.5)),
                pinned((17, 11.75, 6), (17.5, 12.25, 10), c, (0, 7, 4, 7.5)),
                # Two knobs and the magic eye.
                box((4.5, 9, 3), (5.5, 10, 3.5), k), box((10.5, 9, 3), (11.5, 10, 3.5), k),
                box((6.5, 10.5, 3), (9.5, 13.5, 3.5), "#eye", light=15 if on else None)]
    textures = {"walnut": "theremin_walnut", "front": "theremin_front", "brass": "theremin_brass", "copper": "theremin_copper",
                "bakelite": "theremin_bakelite", "eye": "theremin_eye_on" if on else "theremin_eye"}
    return block_model(textures, elements, "theremin_walnut")


def assets(root, write, lang):
    name = THEREMIN["block"]
    models = root / "models" / "block"
    write(models / f"{name}.json", theremin(False))
    write(models / f"{name}_on.json", theremin(True))
    write(root / "blockstates" / f"{name}.json", {"variants": {
        f"facing={f},on={o},powered={p}": turned(rid(f"block/{name}" + ("_on" if "true" in (o, p) else "")), f)
        for f in ("north", "south", "east", "west") for o in ("false", "true") for p in ("false", "true")}})
    write(root / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"block/{name}")}})
    lang[f"block.{MOD}.{name}"] = THEREMIN["display"]
    lang.update(TEXT)


def loot(out, write):
    write(out / f"{THEREMIN['block']}.json", self_drop(THEREMIN["block"]))


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid(THEREMIN["block"]))
