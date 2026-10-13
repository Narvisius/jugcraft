"""Field chemistry (batch 31, docs/features/field-chemistry.md): chemical grenades, the gas mask and medicines.

Java: weapons/FieldChemistry.java (numbers and items), Warhead, ChemicalCloud, Flash and DescribedItem.
tools/check_mod_data.py keeps the numbers here and in Java the same. The chemical reactor recipes are in tools/petro.py
(FLUID_RECIPES["chemical_reactor"]), with the rest of the reactor's work.
"""
from PIL import Image

import hitech
import material_style as ms
from hitech import Face, _faces2x, _outline

MOD = "jugcraft"

# Chlorine: radius (blocks), lifetime (ticks), damage each second. Smoke: radius and lifetime.
CHLORINE_RADIUS = 3.0
CHLORINE_TICKS = 200
CHLORINE_DAMAGE = 2.0
SMOKE_RADIUS = 4.0
SMOKE_TICKS = 300
# Thermite: the pool's radius, lifetime, damage each second and how long it sets things alight (seconds).
THERMITE_RADIUS = 2.0
THERMITE_TICKS = 120
THERMITE_DAMAGE = 4.0
THERMITE_FIRE_SECONDS = 5
# Flashbang: reach, how long a player looking at it is blinded, how long a mob staggers (ticks).
FLASH_RADIUS = 10.0
FLASH_BLIND_TICKS = 80
FLASH_STUN_TICKS = 60
# Gas mask durability multiplier (a helmet has 11 per point: 220 seconds of filter), and a sealed scuba set's oxygen
# use per second in gas instead (mB).
GAS_MASK_DURABILITY = 20
SCUBA_GAS_OXYGEN = 20
# Medicines: the first aid kit's cooldown (seconds) and the stimulant's length (ticks).
FIRST_AID_COOLDOWN = 10
STIMULANT_TICKS = 1200

ITEMS = {
    "chlorine_grenade": ("Chlorine Grenade", "Bursts into a cloud of chlorine that hurts whatever breathes inside it for "
                                             "10 seconds, through armor. Fish and the undead don't breathe."),
    "smoke_grenade": ("Smoke Grenade", "A thick smoke screen for 15 seconds: mobs lose sight of anyone inside, and "
                                       "players inside without a mask can't see."),
    "thermite": ("Thermite", "Aluminum and iron oxide: burns white-hot once lit."),
    "thermite_grenade": ("Thermite Grenade", "Leaves a white-hot pool for 6 seconds that burns whatever stands in it, "
                                             "through armor. Never lights blocks."),
    "flashbang": ("Flashbang", "A blinding flash: players who see it are blinded, mobs lose their target and stagger. "
                               "No damage."),
    "gas_mask": ("Gas Mask", "Keeps out chlorine and smoke, and its tinted lenses a flashbang. The filter wears in gas; "
                             "repair it with charcoal."),
    "first_aid_kit": ("First Aid Kit", "Heals four hearts. Then a 10 second wait before the next."),
    "antidote": ("Antidote", "Clears poison, wither and every other harmful effect, and keeps the good ones."),
    "stimulant": ("Stimulant", "Speed II and Haste II for a minute, and hungry for it."),
}
GRENADES = ["chlorine_grenade", "smoke_grenade", "thermite_grenade", "flashbang"]
MEDICINES = ["first_aid_kit", "antidote", "stimulant"]

# Crafting recipes; the reactor's are in petro.py. Grenades follow the frag grenade's shape: a steel case of two plates
# round the filling, with an iron nugget for the pin.
CRAFTING = {
    "thermite": {"type": "shapeless", "ingredients": ["#c:ingots/aluminum", "#c:dusts/iron", "#c:dusts/iron"],
                 "count": 3},
    "thermite_grenade": {"pattern": [" N ", "PTP", " P "], "count": 4,
                         "key": {"N": "minecraft:iron_nugget", "P": "#c:plates/steel", "T": f"{MOD}:thermite"}},
    "flashbang": {"pattern": [" N ", "PAP", " G "], "count": 4,
                  "key": {"N": "minecraft:iron_nugget", "P": "#c:plates/steel", "A": "#c:nuggets/aluminum",
                          "G": "minecraft:glowstone_dust"}},
    "gas_mask": {"pattern": ["RRR", "GSG", " C "], "count": 1,
                 "key": {"R": f"{MOD}:rubber", "G": "minecraft:glass_pane", "S": "#c:plates/steel",
                         "C": "minecraft:charcoal"}},
}
DAMAGE_TYPES = {
    "chlorine": {"message_id": f"{MOD}.chlorine", "tags": ["bypasses_armor", "no_knockback", "no_impact"],
                 "death": "%1$s choked on chlorine", "death_player": "%1$s choked on chlorine gassed by %2$s"},
    "thermite": {"message_id": f"{MOD}.thermite", "effects": "burning",
                 "tags": ["is_fire", "bypasses_armor", "no_knockback", "no_impact"],
                 "death": "%1$s was burnt down by thermite", "death_player": "%1$s was burnt down by %2$s's thermite"},
}


def items():
    return list(ITEMS)


def write_all(write, assets, data, lang, condition):
    for item, (name, tip) in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        lang[f"tooltip.{MOD}.{item}"] = tip
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    lang[f"tooltip.{MOD}.grenade"] = "Bursts where it lands, hurting living things nearby. Never breaks blocks."
    lang[f"entity.{MOD}.chemical_cloud"] = "Chemical Cloud"
    write(assets / "equipment" / "gas_mask.json", {"layers": {"humanoid": [{"texture": f"{MOD}:gas_mask"}]}})

    for item, recipe in CRAFTING.items():
        result = {"id": f"{MOD}:{item}", "count": recipe["count"]}
        if recipe.get("type") == "shapeless":
            body = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": recipe["ingredients"],
                    "result": result}
        else:
            body = {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": recipe["pattern"],
                    "key": recipe["key"], "result": result}
        write(data / "recipe" / f"{item}.json", {"fabric:load_conditions": condition("machines"), **body})

    for name, info in DAMAGE_TYPES.items():
        body = {"exhaustion": 0.0, "message_id": info["message_id"], "scaling": "when_caused_by_living_non_player"}
        if "effects" in info:
            body["effects"] = info["effects"]
        write(data / "damage_type" / f"{name}.json", body)
        lang[f"death.attack.{info['message_id']}"] = info["death"]
        lang[f"death.attack.{info['message_id']}.player"] = info["death_player"]
    # The guns' flame joins two of these tags; tools/guns.py writes those files again with its own entries added.
    for tag, values in damage_type_tags().items():
        write(data.parent / "minecraft" / "tags" / "damage_type" / f"{tag}.json", {"replace": False, "values": values})
    write(data / "tags" / "item" / "repairs_gas_mask.json", {"values": ["minecraft:charcoal"]})


def damage_type_tags():
    """The vanilla damage type tags these damage types join: {tag: [jugcraft:<type>, ...]}."""
    tags = {}
    for name, info in DAMAGE_TYPES.items():
        for tag in info["tags"]:
            tags.setdefault(tag, []).append(f"{MOD}:{name}")
    return tags


# ------------------------------------------------------------------ art

def _px(img, x, y, c):
    img.putpixel((x, y), tuple(c) + (255,) if len(c) == 3 else c)


def _canister(body, band, top=3, bottom=14, left=5, right=10):
    """A 16x16 grenade canister: a body lit from the left, a coloured band, a fuse cap, a spoon and a ring pin."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(top + 2, bottom + 1):
        for x in range(left, right + 1):
            if (x, y) in ((left, bottom), (right, bottom)):
                continue
            f = (x - left) / (right - left)
            _px(img, x, y, body[3] if f < 0.2 else body[2] if f < 0.6 else body[1])
        _px(img, right, y, body[0])
    for x in range(left, right + 1):
        for y in (top + 6, top + 7):
            _px(img, x, y, band[1] if x > left else band[0])
    for y in range(top, top + 2):  # fuse cap
        for x in range(left + 1, right):
            _px(img, x, y, (176, 180, 186) if x < (left + right) // 2 + 1 else (128, 132, 140))
    for y in range(top + 1, top + 8):  # spoon
        _px(img, right + 1, y, (150, 154, 160))
    for x, y in ((left - 2, top - 1), (left - 1, top - 2), (left, top - 2), (left - 2, top), (left - 1, top + 1),
                 (left, top + 1)):  # ring pin
        if 0 <= x < 16 and 0 <= y < 16:
            _px(img, x, y, (200, 204, 210))
    return img


def chlorine_grenade():
    return _canister([(70, 76, 40), (130, 140, 60), (176, 188, 82), (214, 224, 120)],
                     [(196, 170, 20), (238, 214, 60)])


def smoke_grenade():
    return _canister([(60, 64, 70), (108, 114, 122), (150, 156, 164), (196, 202, 210)],
                     [(210, 210, 214), (244, 244, 246)])


def thermite_grenade():
    return _canister([(80, 34, 24), (142, 62, 40), (186, 92, 56), (220, 132, 88)],
                     [(230, 120, 30), (255, 196, 80)])


def flashbang():
    """A slim steel cylinder pierced with vent holes (the flash escapes through them)."""
    img = _canister([(70, 74, 82), (120, 126, 136), (166, 172, 182), (214, 220, 228)],
                    [(150, 30, 30), (200, 52, 46)], left=6, right=9)
    for x, y in ((7, 6), (8, 8), (7, 11), (8, 13)):
        _px(img, x, y, (30, 30, 36))
    return img


def thermite():
    """A heap of rust-red iron oxide with grains of silver aluminium through it."""
    img = ms.dust([(92, 34, 24), (120, 46, 34), (150, 60, 40), (176, 74, 48), (198, 98, 64)])
    for x, y in ((6, 8), (10, 10), (4, 11), (8, 12), (12, 12)):
        _px(img, x, y, (196, 200, 206))
    return img


def first_aid_kit():
    """A white tin with a red cross and a carrying handle."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(5, 14):
        for x in range(2, 14):
            _px(img, x, y, (236, 236, 232) if y < 12 else (196, 196, 192))
    for x in range(2, 14):
        _px(img, x, 5, (250, 250, 248))
    for x in (2, 13):
        for y in range(5, 14):
            _px(img, x, y, (170, 170, 166))
    for x in range(6, 10):  # handle
        _px(img, x, 3, (90, 90, 96))
    _px(img, 5, 4, (90, 90, 96))
    _px(img, 10, 4, (90, 90, 96))
    for x in range(7, 9):  # the cross
        for y in range(6, 12):
            _px(img, x, y, (200, 30, 36))
    for x in range(5, 11):
        for y in (8, 9):
            _px(img, x, y, (200, 30, 36))
    return img


def _bottle(liquid, cork=(150, 110, 70)):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    glass = (190, 220, 230)
    for y in range(7, 15):
        for x in range(4, 12):
            d = ((x - 7.5) / 4) ** 2 + ((y - 10.5) / 4) ** 2
            if d <= 1:
                c = liquid[1] if x < 7 else liquid[0]
                if y < 8:
                    c = glass
                if (x, y) in ((5, 9), (5, 10), (6, 8)):
                    c = (240, 250, 252)
                _px(img, x, y, c)
    for y in range(3, 7):
        for x in (7, 8):
            _px(img, x, y, glass)
    for x in (6, 7, 8, 9):
        _px(img, x, 2, cork)
    _px(img, 6, 3, cork)
    _px(img, 9, 3, cork)
    return img


def antidote():
    """Activated charcoal slurry: near-black, with a white label band."""
    img = _bottle([(30, 30, 34), (56, 56, 62)])
    for x in range(5, 11):
        _px(img, x, 11, (226, 226, 220))
    _px(img, 7, 11, (40, 140, 60))
    _px(img, 8, 11, (40, 140, 60))
    return img


def stimulant():
    """Strong coffee-brown with a bright amber glint."""
    img = _bottle([(110, 58, 20), (170, 96, 34)])
    _px(img, 9, 12, (250, 196, 80))
    return img


# The gas mask: olive rubber, two round tinted lenses in steel rims, and a filter canister under the chin.
OLIVE = (70, 78, 52)
OLIVE_HI = (104, 114, 78)
OLIVE_DARK = (44, 50, 32)
LENS = (60, 74, 70)
LENS_HI = (170, 210, 196)
FILTER = (140, 120, 60)
FILTER_HI = (196, 172, 96)


def gas_mask_icon():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    f = Face(img, (0, 0, 32, 32))
    for y in range(6, 24):  # the face piece, rounded
        for x in range(6, 26):
            d = ((x - 15.5) / 10) ** 2 + ((y - 14) / 9) ** 2
            if d <= 1:
                f.px(x, y, OLIVE_HI if x < 12 and y < 12 else OLIVE if d < 0.8 else OLIVE_DARK)
    f.rect(2, 12, 6, 14, OLIVE_DARK)  # straps
    f.rect(25, 12, 29, 14, OLIVE_DARK)
    for cx in (11.5, 20.5):  # lenses in steel rims
        for y in range(8, 18):
            for x in range(6, 26):
                d = ((x - cx) ** 2 + (y - 12.5) ** 2) ** 0.5
                if d <= 4.2:
                    f.px(x, y, hitech.STEEL_MID if d > 3.2 else LENS)
        f.px(int(cx) - 1, 10, LENS_HI)
        f.px(int(cx), 10, LENS_HI)
        f.px(int(cx) - 1, 11, LENS_HI)
    for y in range(20, 29):  # filter canister
        for x in range(12, 20):
            f.px(x, y, FILTER_HI if x < 14 else FILTER if x < 18 else (110, 92, 44))
    f.hline(12, 19, 23, (90, 76, 36))
    f.hline(12, 19, 26, (90, 76, 36))
    f.rect(14, 19, 17, 20, hitech.STEEL_DARK)
    return _outline(img, hitech.BLACK, alpha=170)


def gas_mask_layer():
    """The worn mask on the doubled head: face piece, lenses and filter on the front, straps round the back."""
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    head = _faces2x("head")
    f = Face(img, head["front"])
    f.rect(1, 4, 14, 15, OLIVE)
    f.hline(1, 14, 4, OLIVE_HI)
    for x0 in (2, 9):
        f.rect(x0, 5, x0 + 4, 9, hitech.STEEL_MID)
        f.rect(x0 + 1, 6, x0 + 3, 8, LENS)
        f.px(x0 + 1, 6, LENS_HI)
    f.rect(6, 11, 9, 15, FILTER)
    f.rect(6, 11, 6, 15, FILTER_HI)
    f.hline(6, 9, 13, (90, 76, 36))
    f.rect(7, 10, 8, 10, hitech.STEEL_DARK)
    for side in ("right", "left"):
        s = Face(img, head[side])
        s.rect(0, 5, 15, 7, OLIVE_DARK)
        s.rect(0, 11, 15, 12, OLIVE_DARK)
        edge = 13 if side == "right" else 0
        s.rect(edge, 4, edge + 2, 14, OLIVE)
    b = Face(img, head["back"])
    b.rect(0, 5, 15, 7, OLIVE_DARK)
    b.rect(0, 11, 15, 12, OLIVE_DARK)
    b.rect(6, 5, 9, 12, OLIVE)
    return img


def draw_all(save, save_armor):
    for item in GRENADES + ["thermite"] + MEDICINES:
        save(globals()[item](), "item", item)
    save(gas_mask_icon(), "item", "gas_mask")
    save_armor(gas_mask_layer(), "humanoid", "gas_mask")
