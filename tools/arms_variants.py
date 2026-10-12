"""Arms VII, batch 56 (docs/features/arms-vii.md): named variants of the arms, each a kind of tools/arms.py KINDS with its
own look, in three kinds of line:

- styles, crafted at a smithing table from a steel arm, the style's pattern and a material, which keep the arm's
  enchantments and wear: gilded (takes enchantments as gold does), ironclad (dieselpunk, twice as hard-wearing),
  bonecarved (strikes the undead harder) and runebound (glowing runes that mark a foe);
- trophies of eight bosses (docs/branches/BOSSES.md): no recipe; each boss's loot table is ready
  (loot_table/bosses/<boss>.json) for its encounter to drop one of its two, and the Yeti King's and the Cinder Tyrant's,
  the first of them made, drop from them (docs/features/yeti-king.md, YetiKingLoot; docs/features/cinder-tyrant.md,
  CinderTyrantLoot); and of Vesperine, the Last Reaper
  (docs/features/vesperine.md), whose Vesper Scythe she drops (VesperineLoot; her trophy table lists it as theirs do),
  and of Madame Tatterlace (docs/features/tatterlace.md), whose Needle Rapier she drops (TatterlaceLoot);
- the arms of the owner's armor sets, each in its set's look (the Hades Armor's scythe): no recipe and, until the owner
  settles how a set is won, no loot table either; creative only for now. Like a trophy, epic and twice as hard-wearing.
  A set may have a shield of its own shape too (SET_SHIELDS: the Sentinel's four-pointed star), which blocks as the
  steel shield of its base kind does (tools/arms.py SHIELDS) and is built as the kit's shields are (tools/arms_kit.py).

A variant is an ArmItem of its kind (weapons/JugcraftArms.java VARIANTS), so its swing, reach, trait, two-handed blow,
weapon art and motion are its kind's; its line adds a perk or a boon (BOONS), worked on the server in ArmItem. The
numbers here and in Java are kept together by tools/check_mod_data.py (check_arms_variants). Art:
tools/arms_variants_art.py; the Runebound arms are smooth meshes in the hand, with icons rendered from them
(tools/arms_mesh.py).
"""
import arms
import arms_heads
import arms_icons
import arms_kit
import arms_mesh
import arms_variants_art

MOD = "jugcraft"
FEATURE = "machines"   # every variant starts from a steel arm, or is a trophy of the steel age's bosses or a set's arm

# Lines: the four crafted styles, then the bosses whose trophies these are (in docs/branches/BOSSES.md order), then the
# owner's armor sets whose arms these are.
# A style's pattern is its smithing template: crafted from `pattern_recipe`; `addition` goes in the smithing table's
# third slot. rarity: the name's colour (vanilla's Rarity).
STYLES = {
    "gilded": {"display": "Gilded", "pattern": "gilders_pattern", "pattern_name": "Gilder's Pattern",
               "pattern_recipe": (["NNN", "NPN", "NNN"], {"N": "minecraft:gold_nugget", "P": "minecraft:paper"}),
               "addition": "minecraft:gold_ingot", "rarity": "uncommon",
               "perk": "Gilded: takes enchantments as gold does.",
               "pattern_tooltip": "A smithing template: gilds a steel longsword, rapier, sabre or halberd with a gold ingot."},
    "ironclad": {"display": "Ironclad", "pattern": "ironclad_pattern", "pattern_name": "Ironclad Pattern",
                 "pattern_recipe": (["YBY", "BPB", "YBY"], {"Y": "minecraft:yellow_dye", "B": "minecraft:black_dye",
                                                            "P": "#c:plates/steel"}),
                 "addition": "#c:plates/steel", "rarity": "uncommon",
                 "perk": "Ironclad: plated, painted and bolted; wears half as fast.",
                 "pattern_tooltip": "A smithing template: plates a steel zweihander, maul, war pick or battle axe with a steel plate."},
    "bonecarved": {"display": "Bonecarved", "pattern": "bonecarvers_pattern", "pattern_name": "Bonecarver's Pattern",
                   "pattern_recipe": (["BFB", "LPL", "BFB"], {"B": "minecraft:bone", "F": "minecraft:flint",
                                                              "L": "minecraft:leather", "P": "minecraft:paper"}),
                   "addition": "minecraft:bone_block", "rarity": "uncommon",
                   "perk": "Bonecarved: made of what death leaves; strikes the undead harder.",
                   "pattern_tooltip": "A smithing template: carves a steel dagger, flail, glaive or labrys in bone, with a bone block."},
    "runebound": {"display": "Runebound", "pattern": "runecarvers_pattern", "pattern_name": "Runecarver's Pattern",
                  "pattern_recipe": (["AEA", "EPE", "AEA"], {"A": "minecraft:amethyst_shard", "E": "jugcraft:ectoplasm",
                                                             "P": "minecraft:paper"}),
                  "addition": "jugcraft:ectoplasm", "rarity": "uncommon",
                  "perk": "Runebound: its runes glow, and mark what it strikes.",
                  "pattern_tooltip": "A smithing template: binds glowing runes to a steel nodachi, moonblade, quarterstaff or war hammer, with ectoplasm."},
}
BOSSES = {
    # Fought in the Glacier Hall (docs/features/yeti-king.md); his loot drops one of his two.
    "yeti_king": {"display": "the Yeti King"},
    # Fought in the Cinder Kiln (docs/features/cinder-tyrant.md); his loot drops one of his two.
    "cinder_tyrant": {"display": "the Cinder Tyrant"},
    "mire_hag": {"display": "the Mire Hag"},
    "crypt_lich": {"display": "the Crypt Lich"},
    "iron_dreadnought": {"display": "the Iron Dreadnought"},
    "werewolf_alpha": {"display": "the Alpha Werewolf"},
    "storm_roc": {"display": "the Storm Roc"},
    "abyssal_leviathan": {"display": "the Abyssal Leviathan"},
    # The Witching Season's first boss (docs/features/witching-season.md), fought in the Hollow Acre.
    "vesperine": {"display": "Vesperine, the Last Reaper"},
    # Its second (docs/features/tatterlace.md), fought in the Spindle Loft.
    "tatterlace": {"display": "Madame Tatterlace"},
}
# The owner's armor sets with an arm of their own ("I also want the scythe from my Hades Armor set", 7 October 2026).
# How a set is won (a boss's drop, a recipe) is still the owner's to decide, so a set's arm has neither a recipe nor a
# loot table yet; its tooltip names its set where a trophy's names its boss.
SETS = {
    "hades": {"display": "Hades Armor"},
    # The owner, 8 October 2026, sending the Sentinel and Frost Knight designs: "want them done weapons too please"
    # (docs/features/armor-designs-8-october.md).
    "sentinel": {"display": "Sentinel"},
    "frost_knight": {"display": "Frost Knight"},
    # Sent without words the same day, the Wight King's render holds a long sword (armor-designs-8-october.md).
    "wight_king": {"display": "Wight King"},
    # "He is supposed to be holding 2 short scythe weapons they arent part of the armor" (the owner, 8 October 2026, of
    # the Reaper's render): its arm is a short scythe, one for each hand.
    "reaper": {"display": "Reaper"},
}
LINES = list(STYLES) + list(BOSSES) + list(SETS)

# Boons, worked on the server when the arm strikes (ArmItem.hurtEnemy and getAttackDamageBonus). Each is bounded: an
# effect of at most 5 s and amplifier at most 1 (refreshed, never stacked, by another hit), a share at most half a blow.
BOONS = {
    "frost": "Frost: a hit chills the foe, slowing it (Slowness II, 3 s).",
    "ember": "Ember: a hit sets the foe alight (3 s).",
    "venom": "Venom: a hit poisons the foe (Poison, 4 s).",
    "drain": "Drain: each hit heals you half a heart.",
    "wither": "Wither: a hit withers the foe (Wither, 3 s).",
    "shock": "Shock: a hit arcs to the nearest other foe within 4 blocks, for 30% of the blow.",
    "gale": "Gale: a hit throws the foe up and back.",
    "howl": "Howl: a hit cows the foe, weakening its blows (Weakness, 3 s).",
    "tide": "Tide: 25% harder against a foe in water or rain.",
    "gravebane": "Gravebane: 20% harder against the undead.",
    "mark": "Mark: a hit makes the foe glow, seen through walls (4 s).",
    "harvest": "Harvest: a kill heals you two hearts (once every 5 s), and every fifth kill charges your next blow to loose "
               "a pale crescent.",
    "stitch": "Stitch: three hits on one foe within 4 s stitch it, slowing it (Slowness II, 2 s).",
}
FROST = (60, 1)        # Slowness: ticks, amplifier
EMBER_SECONDS = 3
VENOM = (80, 0)        # Poison
WITHER = (60, 0)       # Wither
HOWL = (60, 0)         # Weakness
MARK_TICKS = 80        # Glowing
DRAIN_HEAL = 1.0
SHOCK_SHARE = 0.3
SHOCK_RANGE = 4.0
GALE_KNOCKBACK = 0.6
GALE_LIFT = 0.35
TIDE = 0.25
GRAVEBANE = 0.2
GILDED_ENCHANTABILITY = 22   # gold tools' (steel's is 12)
IRONCLAD_DURABILITY = 2      # times steel's 900
TROPHY_DURABILITY = 2        # a boss's trophy or an armor set's arm, times steel's

# Every variant, in registration order: (id, kind, line, boon or None, name).
VARIANTS = [
    ("gilded_longsword", "longsword", "gilded", None, "Gilded Longsword"),
    ("gilded_rapier", "rapier", "gilded", None, "Gilded Rapier"),
    ("gilded_sabre", "sabre", "gilded", None, "Gilded Sabre"),
    ("gilded_halberd", "halberd", "gilded", None, "Gilded Halberd"),
    ("ironclad_zweihander", "zweihander", "ironclad", None, "Ironclad Zweihander"),
    ("ironclad_maul", "maul", "ironclad", None, "Ironclad Maul"),
    ("ironclad_war_pick", "war_pick", "ironclad", None, "Ironclad War Pick"),
    ("ironclad_battle_axe", "battle_axe", "ironclad", None, "Ironclad Battle Axe"),
    ("bonecarved_dagger", "dagger", "bonecarved", "gravebane", "Bonecarved Dagger"),
    ("bonecarved_flail", "flail", "bonecarved", "gravebane", "Bonecarved Flail"),
    ("bonecarved_glaive", "glaive", "bonecarved", "gravebane", "Bonecarved Glaive"),
    ("bonecarved_labrys", "labrys", "bonecarved", "gravebane", "Bonecarved Labrys"),
    ("runebound_nodachi", "nodachi", "runebound", "mark", "Runebound Nodachi"),
    ("runebound_moonblade", "moonblade", "runebound", "mark", "Runebound Moonblade"),
    ("runebound_staff", "quarterstaff", "runebound", "mark", "Runebound Staff"),
    ("runebound_war_hammer", "war_hammer", "runebound", "mark", "Runebound War Hammer"),
    ("glacier_maul", "maul", "yeti_king", "frost", "Glacier Maul"),
    ("rimeclaw", "katar", "yeti_king", "frost", "Rimeclaw"),
    ("cinderbrand", "greatsword", "cinder_tyrant", "ember", "Cinderbrand"),
    ("magmaw", "earthbreaker", "cinder_tyrant", "ember", "Magmaw"),
    ("hagthorn", "scythe", "mire_hag", "venom", "Hagthorn"),
    ("bogfang", "kama", "mire_hag", "venom", "Bogfang"),
    ("soulreaver", "moonblade", "crypt_lich", "drain", "Soulreaver"),
    ("gravewarden", "executioner", "crypt_lich", "wither", "Gravewarden"),
    ("dynamo_halberd", "halberd", "iron_dreadnought", "shock", "Dynamo Halberd"),
    ("piston_hammer", "war_hammer", "iron_dreadnought", "shock", "Piston Hammer"),
    ("moonfang", "sabre", "werewolf_alpha", "howl", "Moonfang"),
    ("howler", "twinblade", "werewolf_alpha", "howl", "Howler"),
    ("stormcaller", "glaive", "storm_roc", "gale", "Stormcaller"),
    ("galefeather", "estoc", "storm_roc", "gale", "Galefeather"),
    ("tidebreaker", "war_fork", "abyssal_leviathan", "tide", "Tidebreaker"),
    ("leviathans_hook", "bill", "abyssal_leviathan", "tide", "Leviathan's Hook"),
    ("vesper_scythe", "scythe", "vesperine", "harvest", "Vesper Scythe"),
    ("needle_rapier", "rapier", "tatterlace", "stitch", "Needle Rapier"),
    ("hades_scythe", "scythe", "hades", "wither", "Hades Scythe"),
    ("sentinel_longsword", "longsword", "sentinel", "mark", "Sentinel Longsword"),
    ("frost_knight_greatsword", "greatsword", "frost_knight", "frost", "Frost Knight Greatsword"),
    ("wight_king_zweihander", "zweihander", "wight_king", "drain", "Wight King Zweihander"),
    ("reaper_scythe", "kama", "reaper", "wither", "Reaper Scythe"),
]
BY_ID = {name: (kind, line, boon, display) for name, kind, line, boon, display in VARIANTS}

# An armor set's shield, in a shape of its own (tools/arms_kit.py star_elements): it blocks exactly as its `base` shield
# kind does in steel (tools/arms.py SHIELDS), and, like a set's arm, is epic, twice as hard-wearing and creative only
# for now. Its tooltip names its shape's trait and its set.
SET_SHIELDS = {
    "sentinel_shield": {"set": "sentinel", "shape": "star_shield", "base": "heater_shield", "display": "Sentinel Shield"},
}
SHIELD_SHAPES = {
    "star_shield": {"trait": "Quick Raise",
                    "tooltip": "A four-pointed star of gold, quick to raise as a heater shield is."},
}
SET_SHIELD_METAL = "steel"


def items():
    """Every variant, then the styles' patterns, then the armor sets' shields, in registration order."""
    return [name for name, *_ in VARIANTS] + [info["pattern"] for info in STYLES.values()] + list(SET_SHIELDS)


def set_shields(armor_set):
    """An armor set's shields (SET_SHIELDS), in registration order."""
    return [name for name, info in SET_SHIELDS.items() if info["set"] == armor_set]


def patterns():
    return [info["pattern"] for info in STYLES.values()]


def kind(name):
    return BY_ID[name][0]


def line(name):
    return BY_ID[name][1]


def trophies(boss):
    return [name for name, _kind, at, *_ in VARIANTS if at == boss]


def set_arms(armor_set):
    """An armor set's arms (SETS), in registration order."""
    return [name for name, _kind, at, *_ in VARIANTS if at == armor_set]


def textures():
    """Every variant's icon and its 3D model's texture, the patterns' sprites, the Runebound meshes' textures, and each
    set shield's face, back and trim."""
    return ([n for name, *_ in VARIANTS for n in (name, f"{name}_model")] + patterns() + [arms_mesh.ATLAS, arms_mesh.RUNE]
            + [f"{name}{suffix}" for name in SET_SHIELDS for suffix in arms.SHIELD_SPRITES])


def item_tags():
    """Vanilla item tag -> variants: each joins its kind's tags (so its enchantments are its kind's), a set shield its
    base kind's."""
    tags = {}
    for name, kind_, *_ in VARIANTS:
        for tag in arms.KINDS[kind_]["tags"]:
            tags.setdefault(tag, []).append(f"{MOD}:{name}")
    for name, info in SET_SHIELDS.items():
        for tag in arms.SHIELD_KINDS[info["base"]]["tags"]:
            tags.setdefault(tag, []).append(f"{MOD}:{name}")
    return tags


def held_model(name):
    """A variant's 3D model, posed as its kind is (tools/arms.py held_model), but held by its own design's grip."""
    kind_ = kind(name)
    held = arms.KINDS[kind_]["held"]
    (x, y), size, factor = arms_variants_art.held_at(name, held)
    grip = (x * 16.0 / size - 8.0, 8.0 - y * 16.0 / size)
    display = {}
    for context, (rotation, translation, scale) in arms.HANDHELD.items():
        right, left = arms._pose(rotation, translation, scale, grip, scale, arms.SWORD_GRIP, round(held * factor, 4))
        display[context] = right
        display[context.replace("righthand", "lefthand")] = left
    _texture, elements = arms_variants_art.model(name, held)
    texture = f"{MOD}:item/{name}_model"
    return {"textures": {"particle": texture, "tex": texture}, "elements": elements, "display": display}


def trait(text):
    """A perk or boon's text, "Name: what it does.", as its trait's name and its description (gear/TraitTooltips.java):
    ("Name", "What it does.")."""
    name, _, rest = text.partition(": ")
    return name, rest[0].upper() + rest[1:]


def write_all(write, assets, data, lang, condition):
    """Each variant's models and definition, its name and its line's and boon's tooltips; the patterns, their recipes
    and the styles' smithing recipes; and each boss's trophy loot table (an armor set's arm has none yet)."""
    models = assets / "models" / "item"
    for style, info in STYLES.items():
        name, text = trait(info["perk"])
        lang[f"tooltip.{MOD}.arms.line.{style}.trait"] = name
        lang[f"tooltip.{MOD}.arms.line.{style}"] = text
        lang[f"item.{MOD}.{info['pattern']}"] = info["pattern_name"]
        lang[f"tooltip.{MOD}.{info['pattern']}"] = info["pattern_tooltip"]
        write(models / f"{info['pattern']}.json", {"parent": "minecraft:item/generated",
                                                     "textures": {"layer0": f"{MOD}:item/{info['pattern']}"}})
        write(assets / "items" / f"{info['pattern']}.json",
              {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{info['pattern']}"}})
        rows, key = info["pattern_recipe"]
        write(data / "recipe" / f"{info['pattern']}.json", {
            "fabric:load_conditions": condition(FEATURE), "type": "minecraft:crafting_shaped", "category": "misc",
            "pattern": rows, "key": key, "result": {"id": f"{MOD}:{info['pattern']}", "count": 1}})
    for boss, info in BOSSES.items():
        lang[f"tooltip.{MOD}.arms.line.{boss}.trait"] = f"Trophy of {info['display']}"
        write(data / "loot_table" / "bosses" / f"{boss}.json", {
            "type": "minecraft:entity", "random_sequence": f"{MOD}:bosses/{boss}",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"} for name in trophies(boss)]}]})
    for armor_set, info in SETS.items():
        # A set's arm names its set where a trophy names its boss. No loot table: how a set is won is not settled.
        lang[f"tooltip.{MOD}.arms.line.{armor_set}.trait"] = f"Of the {info['display']} set"
    for shape, info in SHIELD_SHAPES.items():
        lang[f"tooltip.{MOD}.arms.{shape}.trait"] = info["trait"]
        lang[f"tooltip.{MOD}.arms.{shape}"] = info["tooltip"]
    for name, info in SET_SHIELDS.items():
        # Held and raised as the kit's shields are (tools/arms_kit.py), in its shape's model and its own textures.
        lang[f"item.{MOD}.{name}"] = info["display"]
        arms_kit.write_set_shield(write, assets, name, info["shape"])
    for boon, text in BOONS.items():
        name, text = trait(text)
        lang[f"tooltip.{MOD}.arms.boon.{boon}.trait"] = name
        lang[f"tooltip.{MOD}.arms.boon.{boon}"] = text
    for name, kind_, at, _boon, display in VARIANTS:
        lang[f"item.{MOD}.{name}"] = display
        # The icon in inventories, frames, on the ground and on shelves; in the hand, the 3D model.
        write(models / f"{name}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/{name}"}})
        if name in arms_mesh.NAMES:
            # A smooth mesh (tools/arms_mesh.py), its box model kept beside it as a fallback. The shared writer does
            # to the box elements what it does to every model; the file is then rewritten a quad a line.
            write(models / f"{name}_in_hand.json", arms_mesh.held_model(name, held_model(name)))
            arms_mesh.compact(models / f"{name}_in_hand.json")
        else:
            write(models / f"{name}_in_hand.json", held_model(name))
        model = {"type": "minecraft:select", "property": "minecraft:display_context",
                 "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"],
                            "model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}"}}],
                 "fallback": {"type": "minecraft:model", "model": f"{MOD}:item/{name}_in_hand"}}
        if name in arms_heads.VARIANT_HEADS:
            # Its swinging head (tools/arms_heads.py): the spine and skull models, picked by FlailHeads' render copies.
            _grip, unit, _g, _e = arms_variants_art.head_layout(name, arms.KINDS[kind_]["held"])
            for part, head_model in arms_heads.models(arms_heads.VARIANT_HEADS[name], unit).items():
                head_model["textures"] = {"particle": f"{MOD}:item/{name}_model", "tex": f"{MOD}:item/{name}_model"}
                write(models / f"{name}_{part}.json", head_model)
            model = arms_heads.definition(name, model)
        write(assets / "items" / f"{name}.json", {"model": model, "swap_animation_scale": arms.KINDS[kind_]["held"]})
        if at in STYLES:
            style = STYLES[at]
            write(data / "recipe" / f"{name}.json", {
                "fabric:load_conditions": condition(FEATURE), "type": "minecraft:smithing_transform",
                "template": f"{MOD}:{style['pattern']}", "base": f"{MOD}:steel_{kind_}", "addition": style["addition"],
                "result": {"id": f"{MOD}:{name}"}})


def draw_all(save):
    """Each variant's icon and model texture (tools/arms_variants_art.py), the patterns' sprites, and the Runebound meshes'
    painted atlas and glowing rune strip. The inventory icon: the Runebound arms' rendered from their meshes
    (tools/arms_mesh.py), else the variant's 16x16 map in its line's materials (tools/arms_icons.py), else its drawing."""
    for name, kind_, line, *_ in VARIANTS:
        held = arms.KINDS[kind_]["held"]
        if name in arms_mesh.NAMES:
            save(arms_mesh.draw(name), "item", name)
        elif arms_icons.has(name):
            save(arms_icons.draw(name, arms_variants_art.LINE_STYLES[line]), "item", name)
        else:
            save(arms_variants_art.draw(name, held), "item", name)
        save(arms_variants_art.model(name, held)[0], "item", f"{name}_model")
    save(arms_mesh.atlas(), "item", arms_mesh.ATLAS)
    save(arms_mesh.rune_strip(), "item", arms_mesh.RUNE, animation=arms_mesh.rune_animation())
    for style, info in STYLES.items():
        save(arms_variants_art.pattern(style), "item", info["pattern"])
    for name in SET_SHIELDS:
        for suffix, image in zip(arms.SHIELD_SPRITES, arms_variants_art.set_shield_sprites(name)):
            save(image, "item", f"{name}{suffix}")
