"""JSON resources for the flying broomstick (fall addition 22), from tools/agriculture.py: its item model, names, messages
and tooltips. The recipe is in SHAPELESS and the advancements in HALLOWEEN_ADVANCEMENTS; the entity is drawn by
client/BroomstickRenderer.java from its grain texture.

Called from agriculture_data.py (assets). Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import BROOMSTICK
from decor_data import MOD, flat_item

TEXT = {
    "message.jugcraft.broom.needs_ointment": "The broom won't fly without flying ointment",
    "message.jugcraft.broom.anointed": "The broom drinks in the ointment: %s of flight",
    "message.jugcraft.broom.full": "The broom can take no more ointment",
    "message.jugcraft.broom.thin": "The ointment is wearing thin",
    "message.jugcraft.broom.dry": "The broom has run dry and is sinking",
    "message.jugcraft.broom.bucked": "The broom bucks you off",
    "tooltip.jugcraft.broom.charge": "Flight left: %s",
    "tooltip.jugcraft.broom.empty": "Needs flying ointment",
    "tooltip.jugcraft.broom.how": "Look where you want to go: forward flies, back brakes, jump climbs",
}


def assets(root, write, lang):
    name = BROOMSTICK["item"]
    flat_item(root, write, name)
    lang[f"item.{MOD}.{name}"] = BROOMSTICK["display"]
    lang[f"entity.{MOD}.{name}"] = BROOMSTICK["display"]
    lang.update(TEXT)
