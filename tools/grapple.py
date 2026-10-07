"""The pneumatic grapple (batch 30, docs/features/pneumatic-grapple.md): a harpoon gun on compressed nitrogen.

Java: gear/JugcraftGrapple.java (numbers), gear/PneumaticGrappleItem.java, gear/GrappleHook.java and
client/GrappleHookRenderer.java. tools/check_mod_data.py keeps the numbers here and in Java the same.
"""
from PIL import Image

MOD = "jugcraft"

# Nitrogen the grapple holds and what a shot uses (mB); the line's length (blocks); launch speed; ticks between shots.
CAPACITY = 4_000
SHOT_COST = 25
RANGE = 32
COOLDOWN = 10


def items():
    return ["pneumatic_grapple"]


def write_all(write, assets, data, lang, condition):
    lang[f"item.{MOD}.pneumatic_grapple"] = "Pneumatic Grapple"
    lang[f"entity.{MOD}.grapple_hook"] = "Grapple Hook"
    lang[f"tooltip.{MOD}.nitrogen"] = "Nitrogen: %s / %s mB"
    lang[f"tooltip.{MOD}.pneumatic_grapple"] = ("Fires a hook on a line: reels you to a block, or a mob to you. Use again "
                                              "to let go. Fill it from nitrogen.")
    lang[f"message.{MOD}.pneumatic_grapple.empty"] = "The grapple is out of nitrogen."
    write(assets / "models" / "item" / "pneumatic_grapple.json",
          {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/pneumatic_grapple"}})
    write(assets / "items" / "pneumatic_grapple.json",
          {"model": {"type": "minecraft:model", "model": f"{MOD}:item/pneumatic_grapple"}})
    # A gun body of steel and a gasket seal, a piston to fire the hook, a tank for the nitrogen and a tripwire hook.
    write(data / "recipe" / "pneumatic_grapple.json", {
        "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["GCH", "SPT", "S  "],
        "key": {"G": f"{MOD}:gasket", "C": "minecraft:iron_chain", "H": "minecraft:tripwire_hook", "S": "#c:plates/steel",
                "P": "minecraft:piston", "T": f"{MOD}:fluid_tank"},
        "result": {"id": f"{MOD}:pneumatic_grapple"}})


# ------------------------------------------------------------------ art

STEEL = [(70, 76, 84), (108, 116, 126), (150, 158, 168), (196, 202, 210)]
BRASS = [(110, 80, 30), (160, 120, 50), (204, 164, 80), (236, 206, 130)]
RUBBER = [(26, 24, 24), (44, 40, 38), (66, 60, 56)]
NITROGEN = [(70, 84, 150), (110, 128, 196), (160, 176, 230)]
OUTLINE = (20, 20, 24, 255)


def _rect(img, x0, y0, x1, y1, ramp):
    """Fills x0..x1, y0..y1 (inclusive), shading from light at the top to dark at the bottom."""
    rows = y1 - y0 + 1
    for y in range(y0, y1 + 1):
        shade = ramp[min(len(ramp) - 1, (len(ramp) - 1) - (y - y0) * len(ramp) // rows)]
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), shade + (255,))


def _outline(img):
    """A dark one-pixel outline around everything drawn."""
    w, h = img.size
    src = img.copy()
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < w and 0 <= y + dy < h and src.getpixel((x + dx, y + dy))[3]:
                    img.putpixel((x, y), OUTLINE)
                    break


def grapple_icon():
    """32x32: a dieselpunk harpoon gun seen from the side, pointing right: a steel barrel with brass bands and the claw
    at the muzzle, a blue nitrogen bottle with a brass band under it, a rubber grip and the line along the top."""
    gun = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    _rect(gun, 6, 12, 24, 15, STEEL)            # barrel
    for x in (9, 15, 21):
        _rect(gun, x, 11, x + 1, 16, BRASS)     # bands
    _rect(gun, 25, 11, 26, 16, BRASS)           # muzzle collar
    _rect(gun, 27, 13, 29, 14, STEEL)           # hook shank
    for dx, dy in ((29, 11), (30, 10), (29, 16), (30, 17), (28, 12), (28, 15)):
        gun.putpixel((dx, dy), STEEL[3] + (255,))  # claw prongs
    _rect(gun, 8, 17, 18, 20, NITROGEN)         # nitrogen bottle
    _rect(gun, 12, 17, 13, 20, BRASS)           # bottle band
    _rect(gun, 19, 18, 20, 19, BRASS)           # valve
    _rect(gun, 3, 13, 6, 22, RUBBER)            # grip
    _rect(gun, 6, 21, 9, 22, STEEL)             # trigger guard
    for x in range(16, 22):
        gun.putpixel((x, 10), RUBBER[2] + (255,))  # the line, coiled along the top
    _outline(gun)
    return gun


def hook_texture():
    """16x16 for the hook entity: a plain light metal, tinted in the renderer (claw, collar, line)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = 228 - ((x * 7 + y * 13) % 5) * 6
            img.putpixel((x, y), (v, v, v, 255))
    return img


def draw_all(save):
    save(grapple_icon(), "item", "pneumatic_grapple")
    save(hook_texture(), "entity", "grapple_hook")
