"""JSON resources for face paint (fall additions 10), from tools/agriculture.py: the Face Paint Kit (its item model, the
tooltip, its recipe), the designs' names and the messages.

Called from agriculture_data.py (assets, recipes). Formats follow vanilla Minecraft 26.3's own files. The paint is drawn
on faces by the client's FacePaintLayer (textures from tools/face_paint_textures.py).
"""
from agriculture import FACE_PAINT
from decor_data import MOD, rid, flat_item

TEXT = {
    "item.jugcraft.face_paint_kit.design": "Design: %s",
    "item.jugcraft.face_paint_kit.hint": "Use on a friend, or hold use to paint your own face; sneak to change the design",
    "message.jugcraft.face_paint.design": "Face paint: %s",
    "message.jugcraft.face_paint.painted": "Your face is painted: %s",
    "message.jugcraft.face_paint.washed": "Your face paint washes off",
}


def assets(root, write, lang):
    flat_item(root, write, FACE_PAINT["kit"])
    lang[f"item.{MOD}.{FACE_PAINT['kit']}"] = FACE_PAINT["kit_display"]
    for design, display in FACE_PAINT["designs"].items():
        lang[f"face_paint.{MOD}.{design}"] = display
    lang.update(TEXT)


def recipes(out, write, conditions):
    # A bowl for the tin, and white, black, orange and green greasepaint.
    write(out / f"{FACE_PAINT['kit']}.json", {
        "fabric:load_conditions": conditions(), "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": ["minecraft:bowl", "minecraft:white_dye", "minecraft:black_dye", "minecraft:orange_dye", "minecraft:green_dye"],
        "result": {"id": rid(FACE_PAINT["kit"]), "count": 1}})
