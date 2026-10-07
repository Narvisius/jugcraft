"""The powered exosuit (batch 28, docs/features/exosuit.md): four JE-powered armor pieces in two liveries, Vanguard
and Ronin, plus a crimson Ronin katana and the smithing templates that switch livery. Keep the lists and numbers in
sync with gear/JugcraftExosuit.java and gear/Exosuit.java; tools/check_mod_data.py checks them.

Inspired by Mekanism's MekaSuit (MIT); none of its code or art is used. The looks follow two images the owner
shared (only their themes; see tools/exosuit_art.py).
"""
MOD = "jugcraft"

# Livery -> (item prefix, display prefix, equipment asset).
STYLES = {
    "vanguard": ("exosuit", "Exosuit", "exosuit"),
    "ronin": ("ronin_exosuit", "Ronin Exosuit", "ronin_exosuit"),
}
PIECES = ["helmet", "chestplate", "leggings", "boots"]
EQUIP_TAGS = {"helmet": "head_armor", "chestplate": "chest_armor", "leggings": "leg_armor", "boots": "foot_armor"}

# Energy (Java: Exosuit). Each piece holds CAPACITY JE (more with capacity modules) and charges at the charging station.
CAPACITY = 400_000
NIGHT_VISION_PER_TICK = 2      # helmet, only while it is dark
SHIELD_POINTS = 8              # chestplate: up to 4 hearts of absorption...
SHIELD_INTERVAL = 10           # ...regrown one point (half a heart) every 10 ticks
SHIELD_PER_POINT = 4_000       # ...for 4,000 JE a point
SPEED_PER_TICK = 1             # leggings: +30% walking speed
SPEED_BONUS = 0.3
BOOTS_PER_TICK = 1             # boots: no fall damage, step up a full block
STEP_BONUS = 0.5

# Smithing additions that go with each livery template.
LIVERY_DYE = {"ronin": "minecraft:red_dye", "vanguard": "minecraft:cyan_dye"}

ABILITIES = {
    "helmet": "Night vision in the dark (2 JE/t while dark)",
    "chestplate": "Energy shield: regrows up to 4 hearts of absorption (4,000 JE per half heart). Hold jump in the "
                  "air to fly, like the rocket pack",
    "leggings": "+30% walking speed (1 JE/t)",
    "boots": "No fall damage, and steps up a full block (1 JE/t)",
}


def piece_id(style, piece):
    return f"{STYLES[style][0]}_{piece}"


def items():
    """Every item this module registers, in registration order (Java: JugcraftExosuit)."""
    return ([piece_id(style, piece) for style in STYLES for piece in PIECES]
            + ["ronin_katana", "ronin_livery", "vanguard_livery"])


# ---------------------------------------------------------------- 3D parts on the body (client/WornModelLayer)
# Boxes in each body part's own space, in pixels, before it moves: x towards the model's left (the right arm and leg
# are at -x), y DOWN from the part's pivot, z towards the back. The head and body pivot at the neck; an arm pivots at
# its shoulder (its 4x12x4 box spans x -3..1 for the right arm, -1..3 for the left, y -2..10); a leg at its hip (x -2..2,
# y 0..12). Worn armor sits 1 pixel (0.5 for leggings) outside those boxes. Each entry: (from, to, texture).
def _mirror(boxes):
    """The same boxes on the other side (the left arm or leg from the right one)."""
    return [((-to[0], frm[1], frm[2]), (-frm[0], to[1], to[2]), tex) for frm, to, tex in boxes]


def _vanguard_parts():
    # Chunky stacked shoulder plates (sode): a cap over the shoulder, then three wide plates stepping outwards and down.
    pauldron = [((-6.0, -4.3, -3.8), (2.0, -3.1, 3.8), "exo_vanguard_plate")]
    for i in range(3):
        tex = "exo_vanguard_light" if i == 0 else "exo_vanguard_plate"
        pauldron.append(((-6.2 - 0.45 * i, -3.1 + 2.2 * i, -3.8 + 0.25 * i), (-3.4 - 0.3 * i, -1.0 + 2.2 * i, 3.8 - 0.25 * i),
                         tex))
    tassets = [
        ((-2.9, -1.4, -3.5), (2.9, 2.4, -2.8), "exo_vanguard_plate"),    # front skirt plate
        ((-3.1, 2.2, -3.8), (3.1, 5.6, -3.1), "exo_vanguard_plate"),     # lower front plate
        ((-3.5, -1.4, -2.9), (-2.8, 5.0, 2.9), "exo_vanguard_plate"),    # outer side plate
        ((-2.9, -1.4, 2.8), (2.9, 3.6, 3.5), "exo_vanguard_plate"),      # back plate
        ((-2.1, 5.9, -3.6), (2.1, 9.1, -2.6), "exo_vanguard_knee"),      # knee guard
    ]
    return {
        "helmet_head": [((-5.3, -9.7, -5.5), (5.3, -8.8, -4.5), "exo_vanguard_plate"),  # brow bar
                        ((-0.6, -10.6, -4.6), (0.6, -9.0, 3.6), "exo_vanguard_plate"),  # crest fin
                        ((-6.0, -5.6, -1.6), (-5.0, -2.4, 1.6), "exo_vanguard_light"),  # ear modules
                        ((5.0, -5.6, -1.6), (6.0, -2.4, 1.6), "exo_vanguard_light")],
        "chestplate_body": [((-3.0, 1.0, 3.0), (3.0, 9.0, 4.6), "exo_vanguard_light")],  # power pack on the back
        "chestplate_right_arm": pauldron,
        "chestplate_left_arm": _mirror(pauldron),
        "leggings_right_leg": tassets,
        "leggings_left_leg": _mirror(tassets),
    }


def _ronin_parts():
    hat = [((-11.0, -9.8, -11.0), (11.0, -9.3, 11.0), "exo_ronin_kasa")]  # the wide brim
    for step, half in enumerate((8.5, 6.2, 4.0, 2.2, 0.9)):               # rising in tiers to a point
        top = -10.3 - 0.5 * step
        hat.append(((-half, top, -half), (half, top + 0.55, half), "exo_ronin_kasa"))
    hat.append(((-0.5, -13.4, -0.5), (0.5, -12.6, 0.5), "exo_ronin_silver"))   # finial
    for x in (-7.0, 6.6):                                                      # hanging cords
        hat.append(((x, -9.3, 3.0), (x + 0.4, -1.5, 3.4), "exo_ronin_cord"))
    pauldron = [((-6.0, -4.3, -3.9), (2.2, -3.1, 3.9), "exo_ronin_silver"),     # silver cap
                ((-6.4, -3.1, -3.9), (-3.2, 3.2, 3.9), "exo_ronin_plate"),      # the big crimson shoulder plate
                ((-6.9, -1.4, -1.4), (-6.4, 1.4, 1.4), "exo_ronin_silver")]     # its round port
    skirt = [((-2.7, -1.0, -3.3), (2.7, 8.6, -2.8), "exo_ronin_cloth"),        # long front strips
             ((-3.3, -1.0, -2.7), (-2.8, 7.4, 2.7), "exo_ronin_cloth"),        # outer side
             ((-2.7, -1.0, 2.8), (2.7, 9.2, 3.3), "exo_ronin_cloth")]          # back
    return {
        "helmet_head": hat,
        "chestplate_right_arm": pauldron,
        "chestplate_left_arm": _mirror(pauldron),
        "leggings_body": [((-1.9, 11.0, -3.4), (1.9, 20.0, -2.9), "exo_ronin_tabard")],  # tabard from the belt
        "leggings_right_leg": skirt,
        "leggings_left_leg": _mirror(skirt),
    }


PARTS = {"vanguard": _vanguard_parts(), "ronin": _ronin_parts()}


def worn_models(quads):
    """name -> quads for worn_models.json, e.g. "vanguard_chestplate_right_arm". quads is kinetic_rotors.quads.
    The renderer turns each part's space half a turn about z (as for the rocket pack), so the boxes are flipped here
    to come out the right way up: x -> -x, y -> -y. Every face shows its whole texture ("!")."""
    out = {}
    for style, parts in PARTS.items():
        for name, boxes in parts.items():
            elements = []
            for frm, to, tex in boxes:
                elements.append(((-to[0], -to[1], frm[2]), (-frm[0], -frm[1], to[2]), f"{tex}!"))
            out[f"{style}_{name}"] = quads(elements)
    return out


def write_all(write, assets, data, lang, condition):
    for style, (prefix, display, asset) in STYLES.items():
        for piece in PIECES:
            lang[f"item.{MOD}.{prefix}_{piece}"] = f"{display} {piece.capitalize()}"
        write(assets / "equipment" / f"{asset}.json", {"layers": {
            "humanoid": [{"texture": f"{MOD}:{asset}"}], "humanoid_leggings": [{"texture": f"{MOD}:{asset}"}]}})
    lang[f"item.{MOD}.ronin_katana"] = "Ronin Katana"
    lang[f"item.{MOD}.ronin_livery"] = "Ronin Livery"
    lang[f"item.{MOD}.vanguard_livery"] = "Vanguard Livery"
    lang[f"tooltip.{MOD}.livery"] = "Smithing template: repaints exosuit pieces and the katana, keeping their charge"
    for piece, text in ABILITIES.items():
        lang[f"tooltip.{MOD}.exosuit_{piece}"] = text
    for item in items():
        kind = "handheld" if item == "ronin_katana" else "generated"
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": f"minecraft:item/{kind}", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})

    recipes = data / "recipe"
    shaped = {
        "exosuit_helmet": (["TPT", "TGT"], {"T": "#c:plates/titanium", "P": f"{MOD}:processor",
                                            "G": "minecraft:tinted_glass"}),
        "exosuit_chestplate": (["T T", "LRL", "TCT"], {"T": "#c:plates/titanium", "L": f"{MOD}:lithium_cell",
                                                       "R": f"{MOD}:rocket_pack", "C": f"{MOD}:advanced_circuit"}),
        "exosuit_leggings": (["TCT", "N N", "T T"], {"T": "#c:plates/titanium", "C": f"{MOD}:advanced_circuit",
                                                     "N": f"{MOD}:neodymium_magnet"}),
        "exosuit_boots": (["TFT", "N N"], {"T": "#c:plates/titanium", "F": f"{MOD}:free_runners",
                                           "N": f"{MOD}:neodymium_magnet"}),
        "ronin_livery": (["RBR", "BPB", "RBR"], {"R": "minecraft:red_dye", "B": "minecraft:black_dye",
                                                 "P": "#c:plates/steel"}),
        "vanguard_livery": (["GCG", "CPC", "GCG"], {"G": "minecraft:gray_dye", "C": "minecraft:cyan_dye",
                                                    "P": "#c:plates/steel"}),
    }
    for item, (pattern, key) in shaped.items():
        write(recipes / f"{item}.json", {
            "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped",
            "category": "equipment", "pattern": pattern, "key": key,
            "result": {"id": f"{MOD}:{item}", "count": 2 if item.endswith("_livery") else 1}})
    # Repainting at the smithing table keeps the piece's charge, modules and enchantments (smithing copies them).
    pairs = [(piece_id("vanguard", piece), piece_id("ronin", piece)) for piece in PIECES]
    pairs.append(("power_katana", "ronin_katana"))
    for vanguard, ronin in pairs:
        for template, base, result in (("ronin", vanguard, ronin), ("vanguard", ronin, vanguard)):
            write(recipes / f"{result}_from_{base}.json", {
                "fabric:load_conditions": condition("machines"), "type": "minecraft:smithing_transform",
                "template": f"{MOD}:{template}_livery", "base": f"{MOD}:{base}", "addition": LIVERY_DYE[template],
                "result": {"id": f"{MOD}:{result}"}})


def item_tags():
    """Vanilla item tag -> entries, merged into tools/gear.py's tag files (they share head_armor, swords...)."""
    by_tag = {tag: [f"{MOD}:{piece_id(style, piece)}" for style in STYLES] for piece, tag in EQUIP_TAGS.items()}
    by_tag["swords"] = [f"{MOD}:ronin_katana"]
    return by_tag
