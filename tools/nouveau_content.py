"""Full owner-library port catalog and resource adapters.

Source files are inputs, never executable code. IDs for new content have the
nouveau_ prefix; existing released helper IDs remain stable. This catalog is an
implementation inventory, not a claim that a registered object has parity.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art/owner-library/originals/Magic"
ASSET = SOURCE / "assets/ars_jimbaux"
DATA = SOURCE / "data/ars_jimbaux"
LANG = json.loads((ASSET / "lang/en_us.json").read_text(encoding="utf-8"))
EXISTING = {"bookwyrm_charm", "dominion_wand", "starbuncle_charm", "bookwyrm_lectern"}
ALIASES = {"storage_lectern": "bookwyrm_lectern"}
INTERNAL = {"debug", "intangible_air", "light_block", "temporary_light_block", "magic_fire",
            "dim_boundary", "portal", "rune", "particle_block", "redstone_air"}
STATIONS = {
    "rune",
    "enchanting_apparatus", "imbuement_chamber", "alteration_table", "scribes_table",
    "arcane_core", "arcane_pedestal", "potion_jar", "potion_melder", "potion_diffuser",
    "drygmy_stone", "whirlisprig_flower", "wixie_cauldron", "ritual_brazier",
    "repository", "repository_controller", "mob_jar", "item_detector",
    "scryers_crystal", "scryers_oculus", "planarium", "arcane_platform",
    "basic_spell_turret", "spell_turret", "rotating_spell_turret", "timer_spell_turret",
    "spell_prism", "void_prism", "spell_sensor", "brazier_relay", "redstone_relay",
    "source_jar", "creative_source_jar", "relay", "relay_collector", "relay_deposit",
    "relay_splitter", "relay_warp", "agronomic_sourcelink", "alchemical_sourcelink",
    "mycelial_sourcelink", "vitalic_sourcelink", "volcanic_sourcelink",
}
DERIVED_LINKS = {"agronomic_sourcelink", "alchemical_sourcelink", "vitalic_sourcelink", "volcanic_sourcelink"}
GEO_BLOCKS = {
    **{name: name for name in DERIVED_LINKS},
    "enchanting_apparatus": "enchanting_apparatus", "imbuement_chamber": "imbuement_chamber",
    "alteration_table": "alteration_table", "scribes_table": "scribes_table",
    "arcane_core": "arcane_core", "repository": "repository", "repository_controller": "storage_catalog",
    "mob_jar": "mob_jar", "potion_melder": "potion_stirrer", "ritual_brazier": "ritual",
    "basic_spell_turret": "basic_spell_turret", "spell_turret": "spell_turret",
    "rotating_spell_turret": "spell_turret", "timer_spell_turret": "spell_turret_timer",
    "relay": "source_relay", "relay_collector": "source_collector", "relay_deposit": "source_deposit",
    "relay_splitter": "source_splitter", "relay_warp": "source_warp", "redstone_relay": "redstone_relay",
    "decor_blossom": "decor_blossom", "whirlisprig_flower": "whirlisprig_blossom",
    "mage_block": "mage_block", "rune": "rune", "scryers_oculus": "scryers_eye",
}
GEO_TEXTURES = {"storage_catalog": "storage_catalog_unfiltered", "ritual": "ritual_brazier", "scryers_eye": "scryers_eye_idle", "rune": "runes/rune"}
GEO_ANIMATIONS = {"alteration_table": "alteration_apparatus_animations","potion_stirrer": "potion_melder_animation", "ritual": "ritual", "rune": "rune", "scryers_eye": "scryers_eye_animation"}
ITEM_GEOS = {"starbuncle_shades":"starbuncle_shades", "wixie_hat":"witch_hat","spell_bow": "spellbow", "spell_crossbow": "spell_crossbow", "wand": "wand", "enchanters_sword": "sword",
             "enchanters_shield": "shield", "enchanters_fishing_rod": "enchanters_rod", "enchanters_eye": "enchanters_eye",
             "enchanters_gauntlet": "enchanters_gauntlet", "enchanters_mirror": "enchanters_mirror",
             "splash_flask_cannon": "splash_flask_cannon", "lingering_flask_cannon": "lingering_flask_cannon",
             **{book + "_spell_book": "spellbook_closed" for book in ("novice", "apprentice", "archmage", "creative")}}


def geometry_for(name):
    original = ASSET / "geo" / (name + ".geo.json")
    if original.exists(): return json.loads(original.read_text(encoding="utf-8"))
    if name not in DERIVED_LINKS: raise ValueError("No supplied geometry for " + name)
    # These four original block models ship clips for a frame bone but no Gecko file.
    # Preserve every cuboid and UV; bind the existing outer assembly to that frame.
    model = json.loads((ASSET / "models/block" / (name + ".json")).read_text(encoding="utf-8"))
    png = (ASSET / "textures/block" / (name + ".png")).read_bytes()
    width, height = int.from_bytes(png[16:20], "big"), int.from_bytes(png[20:24], "big")
    bones = [{"name": "base", "pivot": [0, 0, 0], "cubes": []}, {"name": "frame", "parent": "base", "pivot": [0, 8, 0], "cubes": []}]
    for i, element in enumerate(model["elements"]):
        start, end = element["from"], element["to"]
        cube = {"origin": [start[0]-8, start[1], start[2]-8], "size": [end[j]-start[j] for j in range(3)], "uv": {}}
        for face, binding in element["faces"].items():
            u, v, u2, v2 = binding["uv"]
            cube["uv"][face] = {"uv": [u*width/16, v*height/16], "uv_size": [(u2-u)*width/16, (v2-v)*height/16]}
        rotation = element.get("rotation", {})
        if rotation.get("angle", 0):
            cube["pivot"] = [rotation["origin"][0]-8, rotation["origin"][1], rotation["origin"][2]-8]
            cube["rotation"] = [rotation["angle"] if axis == rotation["axis"] else 0 for axis in "xyz"]
        bones[0 if i < 2 else 1]["cubes"].append(cube)
    return {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": "geometry."+name, "texture_width": width, "texture_height": height, "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]}, "bones": bones}]}


def presentation():
    result = {}
    for geometry in sorted(set(GEO_BLOCKS.values())):
        candidates = [GEO_ANIMATIONS.get(geometry, ""), geometry + "_animations", geometry + "_animation", "empty"]
        animation = next(name for name in candidates if (ASSET / "animations" / (name + ".json")).exists())
        clips = list(json.loads((ASSET / "animations" / (animation + ".json")).read_text(encoding="utf-8")).get("animations", {}))
        active = next((c for c in ("enchanting", "imbue_fast", "create_glyph2", "rotation", "rotating", "stir", "spin", "active", "gem_rotation", "key_rotation") if c in clips), clips[0] if clips else "")
        idle = next((c for c in ("floating", "float", "gem_float", "idle") if c in clips), "")
        result[geometry] = {"texture": GEO_TEXTURES.get(geometry, geometry), "animation": animation, "active": active, "idle": idle}
    return result


def id_of(name):
    name = ALIASES.get(name, name)
    return name if name in EXISTING else "nouveau_" + name


def resource_id(value):
    """Registry references; resource/model references use a separate path adapter."""
    if value.startswith("ars_nouveau:"): value="ars_jimbaux:"+value.split(":",1)[1]
    if value == "minecraft:chain": return "minecraft:iron_chain"
    if value == "jugcraft:nouveau_wilden_boss": return "jugcraft:nouveau_wilden_chimera"
    if value.startswith("#"): return "#" + resource_id(value[1:])
    if value.startswith("ars_jimbaux:"):
        name = value.split(":", 1)[1]
        aliases={"bookwyrm":"jugcraft:book_wyrm", "wilden_boss":"jugcraft:nouveau_wilden_chimera", "gift_starby":"jugcraft:starbuncle", "an_lightning":"minecraft:lightning_bolt", "linger":"minecraft:area_effect_cloud", "wall":"minecraft:area_effect_cloud"}
        if name in aliases:return aliases[name]
        if name in {"starbuncle", "book_wyrm"}: return "jugcraft:" + name
        return "jugcraft:" + id_of(name)
    return value


def asset_id(value):
    if value.startswith("ars_jimbaux:"):
        return "jugcraft:nouveau/" + value.split(":", 1)[1]
    if value.startswith(("block/", "item/")): return "minecraft:" + value
    return value


def walk(value, replace):
    if isinstance(value, str):
        return replace(value)
    if isinstance(value, list):
        return [walk(v, replace) for v in value]
    if isinstance(value, dict):
        return {replace(k): walk(v, replace) for k, v in value.items()}
    return value


def block_kind(name):
    if name == "archwood_chest": return "chest"
    if name in STATIONS: return "station"
    if name in {"mage_block", "decor_blossom"}: return "animated_decor"
    for suffix in ("hanging_wall_sign", "hanging_sign", "wall_sign", "sign", "leaves", "sapling",
                   "stairs", "slab", "fence_gate", "fence", "trapdoor", "door", "button", "pressure_plate"):
        if name.endswith("_" + suffix): return suffix
    if name.endswith(("_log", "_wood")): return "log"
    if name.endswith("_sbed"): return "bed"
    if name.endswith("_pod"): return "fruit"
    if name == "magebloom_crop": return "crop"
    if name == "sourceberry_bush": return "berry"
    if name.endswith(("_grate",)): return "grate"
    if name.endswith("sconce") or name in {"magelight_torch", "source_lamp"}: return "lamp"
    if name in {"falseweave", "ghostweave", "mirrorweave", "sky_block"}: return "weave"
    if name in INTERNAL: return "internal"
    return "building"


def item_kind(name):
    if name.startswith("glyph_") or name in {"reset", "rewind", "wololo"}: return "glyph"
    if name.startswith("ritual_"): return "ritual"
    if name.startswith("familiar_"): return "familiar"
    if name.startswith("thread_"): return "thread"
    if name.endswith("_charm"): return "charm"
    if name.endswith("_se"): return "spawn_egg"
    if name.startswith("music_disc_"): return "music_disc"
    if name.endswith(("_boots", "_hood", "_leggings", "_robes")): return "armor"
    if name.endswith("_scroll"): return "scroll"
    if name.startswith("potion_flask") or name.endswith("flask_cannon"): return "potion"
    if name in {"source_berry_pie", "source_berry_roll"}: return "food"
    if name in {"annotated_codex", "worn_notebook"}: return "guide"
    if name.endswith("spell_book") or name in {"caster_tome", "spell_parchment"}: return "spellbook"
    if name.startswith(("amulet_", "belt_", "ring_")) or name in {"jump_ring", "alchemists_crown", "shapers_focus", "summon_focus"}: return "trinket"
    if name.startswith("enchanters_") or name in {"wand", "spell_bow", "spell_crossbow"}: return "instrument"
    if name in {"experience_gem", "greater_experience_gem", "jar_of_light", "void_jar", "runic_chalk", "dowsing_rod", "starbuncle_shades", "starby_gift", "wixie_hat", "ars_stencil", "archwood_boat", "blank_parchment"}: return "utility"
    if name.endswith("_arrow"): return "arrow"
    return "material"


def catalog():
    blocks, items = {}, {}
    for key, label in LANG.items():
        if key.startswith("block.ars_jimbaux."):
            name = key.removeprefix("block.ars_jimbaux.")
            if "." not in name and name not in ALIASES:
                blocks[name] = {"id": id_of(name), "name": label, "kind": block_kind(name), "geo": GEO_BLOCKS.get(name, "")}
        if key.startswith("item.ars_jimbaux."):
            name = key.removeprefix("item.ars_jimbaux.")
            if "." not in name:
                items[name] = {"id": id_of(name), "name": label, "kind": item_kind(name)}
        if key.startswith("ars_jimbaux.glyph_name."):
            name = key.rsplit(".", 1)[1]
            if name not in {"reset", "rewind", "wololo"} and not name.startswith("glyph_"): name = "glyph_" + name
            items[name] = {"id": id_of(name), "name": label, "kind": "glyph"}
    # The old renderer-only blockstate does not declare a second item registry entry.
    blocks["bookwyrm_lectern"] = {"id": "bookwyrm_lectern", "name": "Storage Lectern", "kind": "existing", "geo": "book_wyrm_lectern"}
    for plant in ("blue_archwood_sapling", "green_archwood_sapling", "purple_archwood_sapling", "red_archwood_sapling", "magebloom_crop"):
        name="potted_"+plant;blocks[name]={"id":id_of(name),"name":"Potted "+LANG.get("block.ars_jimbaux."+plant,plant.replace("_"," ").title()),"kind":"potted","geo":""}
    blocks["redstone_air"] = {"id":id_of("redstone_air"),"name":"Temporary Redstone Signal","kind":"internal","geo":""}
    for name in blocks: items.pop(name, None)
    for name, item in items.items(): item["geo"] = ITEM_GEOS.get(name, "")
    return {"schema": 1, "blocks": blocks, "items": items}


CATALOG = catalog()
OWNER_FILES = {
    f"{kind}/nouveau/{path.relative_to(ASSET / kind).as_posix()}": "assets/ars_jimbaux/" + path.relative_to(ASSET).as_posix()
    for kind in ("textures", "sounds") for path in (ASSET / kind).rglob("*") if path.is_file()
}

OWNER_FILES.update({f"textures/nouveau/entity/{path.relative_to(ASSET / 'entity').as_posix()}": "assets/ars_jimbaux/" + path.relative_to(ASSET).as_posix() for path in (ASSET / 'entity/chest').glob('*.png')})

def ingredient(value):
    if isinstance(value, list): return [ingredient(v) for v in value]
    if isinstance(value, dict):
        if "item" in value: return resource_id(value["item"])
        if "tag" in value: return "#" + resource_id(value["tag"])
    return resource_id(value)


def adapt_recipe(source):
    r = walk(source, resource_id)
    kind = source["type"]
    if kind == "ars_jimbaux:flask":
        return "recipe", {"type": "jugcraft:nouveau_fill_flask"}
    r["type"] = kind
    if kind.startswith("minecraft:"):
        if "key" in r: r["key"] = {k: ingredient(v) for k, v in source["key"].items()}
        if "ingredients" in r: r["ingredients"] = [ingredient(v) for v in source["ingredients"]]
        if "ingredient" in r: r["ingredient"] = ingredient(source["ingredient"])
        if "conditions" in r: del r["conditions"]
        return "recipe", r
    # Runtime NouveauRecipes interprets each declared operation, preserving costs,
    # component-preserving upgrades, catalysts and the exact source output rolls.
    r["type"] = kind.removeprefix("ars_jimbaux:")
    return "nouveau_recipe", r


def write_all(write, assets, data, lang, condition, self_drop):
    write(assets.parents[1] / "jugcraft/nouveau/catalog.json", CATALOG)
    write(assets.parents[1] / "jugcraft/nouveau/presentation.json", presentation())
    for geo, info in presentation().items():
        for category in ("block", "item"):
            write(assets / "geckolib/models" / category / f"nouveau/{geo}.geo.json", geometry_for(geo))
            write(assets / "geckolib/animations" / category / f"nouveau/{geo}.animation.json", json.loads((ASSET / "animations" / (info["animation"] + ".json")).read_text(encoding="utf-8")))
    for kind in ("models", "particles"):
        for path in sorted((ASSET / kind).rglob("*.json")):
            target = assets / kind / "nouveau" / path.relative_to(ASSET / kind)
            source = json.loads(path.read_text(encoding="utf-8"))
            write(target, walk(source, asset_id))
    for group in ("blocks", "items"):
        for source, spec in CATALOG[group].items():
            if source in EXISTING: continue
            name = spec["id"]
            lang[("block" if group == "blocks" else "item") + ".jugcraft." + name] = spec["name"]
            model = f"jugcraft:nouveau/item/{source}"
            write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": model}})
            if group == "items" and spec.get("geo"):
                write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:special", "base": model, "model": {"type": "geckolib:geckolib"}}})
            write(data / "spell_assignments" / f"{name}.json", {"access": "NONE", "access_param": ""})
            if group == "blocks":
                original = ASSET / "blockstates" / f"{source}.json"
                if original.exists():
                    write(assets / "blockstates" / f"{name}.json", walk(json.loads(original.read_text(encoding="utf-8")), asset_id))
                if spec["kind"] in {"station", "animated_decor"}:
                    if spec["geo"]:
                        write(assets / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"jugcraft:nouveau/block/{source}"}}})
                        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:special", "base": model, "model": {"type": "geckolib:geckolib"}}})
                    elif original.exists():
                        state = walk(json.loads(original.read_text(encoding="utf-8")), asset_id)
                        if "variants" in state:
                            first = next(iter(state["variants"].values()))
                            if source not in {"source_jar", "creative_source_jar"}: state = {"variants": {"": first}}
                            write(assets / "blockstates" / f"{name}.json", state)
                write(data / "loot_table/blocks" / f"{name}.json", self_drop(name))
    for path in sorted((DATA / "recipe").rglob("*.json")):
        folder, recipe = adapt_recipe(json.loads(path.read_text(encoding="utf-8")))
        if folder == "recipe": recipe["fabric:load_conditions"] = condition("concordance")
        write(data / folder / "nouveau" / path.relative_to(DATA / "recipe"), recipe)
    # Internal tags are rewritten into the same stable namespace as the content.
    for path in sorted((DATA / "tags").rglob("*.json")):
        relative = path.relative_to(DATA / "tags")
        registry = "/".join(relative.parts[:2]) if relative.parts[0] == "worldgen" else relative.parts[0]
        name = "/".join(relative.parts[2:] if relative.parts[0] == "worldgen" else relative.parts[1:])
        write(data / "tags" / registry / ("nouveau_" + name),
              walk(json.loads(path.read_text(encoding="utf-8")), entity_id if registry == "entity_type" else resource_id))
    write(ROOT / "docs/features/jug-nouveau-content-map.json", {
        "status": "core implementation complete; see feature record for validation",
        "catalog": CATALOG,
        "recipe_sources": [p.relative_to(SOURCE).as_posix() for p in sorted((DATA / "recipe").rglob("*.json"))],
        "asset_sources": OWNER_FILES,
    })
    nature_data(write, data, self_drop)
    visual_data(write, assets, data, lang)
    import nouveau_models
    nouveau_models.write_all(write, assets, data)
    from owner_visual_polish import nouveau
    nouveau()
    import nouveau_spells
    nouveau_spells.write_all(write, assets, data, lang)


def visual_data(write, assets, data, lang):
    """Preserve authored geometry/animations; translate resource paths, not artwork."""
    animation_alias = {**{n: "starbuncle_animations" for n in ("goobuncle", "starbuncle_jarva", "gift_starby", "sir_squirrely", "xacris", "zieg", "bootybuncle")}, "wilden_guardian": "wilden_defender_animations", "alakarkinos": "alakarkinos.animation"}
    for path in (ASSET / "geo").glob("*.geo.json"):
        name = path.name.removesuffix(".geo.json")
        alias = "weald_walker_animations" if name.endswith("_weald_walker") else "weald_waddler_animations" if name.endswith("_weald_waddler") else animation_alias.get(name, "")
        candidates = [alias, name + "_animations", name + "_animation", GEO_ANIMATIONS.get(name, ""), name, "empty"]
        animation = next(p for n in candidates if n and (p := ASSET / "animations" / (n + ".json")).is_file())
        geometry = json.loads(path.read_text(encoding="utf-8"))
        clips = json.loads(animation.read_text(encoding="utf-8"))
        # The supplied swim-charge clip contains an undefined legacy variable.
        # One deterministic half-second cycle preserves its authored poses in Molang 5.
        clips = walk(clips, lambda value: value.replace("swimcharge", "720"))
        for category in ("entity", "item", "block", "armor"):
            write(assets / f"geckolib/models/{category}/nouveau/{name}.geo.json", geometry)
            write(assets / f"geckolib/animations/{category}/nouveau/{name}.animation.json", clips)
    # Multi-stack couriers retain the supplied mesh, atlas and clips on every tier.
    import copy
    for base in ("starbuncle", "goobuncle", "starbuncle_jarva", "gift_starby", "sir_squirrely", "xacris", "zieg", "bootybuncle", "nook", "lily"):
        mesh_path = assets / f"geckolib/models/entity/nouveau/{base}.geo.json"
        if not mesh_path.exists(): continue
        mesh = json.loads(mesh_path.read_text(encoding="utf-8"))
        motion = json.loads((assets / f"geckolib/animations/entity/nouveau/{base}.animation.json").read_text(encoding="utf-8"))
        for tiers in (2, 3, 4):
            stacked = copy.deepcopy(mesh); parts = stacked["minecraft:geometry"][0]
            original = copy.deepcopy(parts["bones"])
            for tier in range(1, tiers):
                prefix = f"stack{tier}_"; dy = tier * 10
                for bone in original:
                    extra = copy.deepcopy(bone); extra["name"] = prefix + bone["name"]
                    if "parent" in extra: extra["parent"] = prefix + extra["parent"]
                    if "pivot" in extra: extra["pivot"][1] += dy
                    for cube in extra.get("cubes", []):
                        cube["origin"][1] += dy
                        if "pivot" in cube: cube["pivot"][1] += dy
                    parts["bones"].append(extra)
            parts["description"]["visible_bounds_height"] = max(4, tiers * 2)
            clips = copy.deepcopy(motion)
            for clip in clips.get("animations", {}).values():
                original_bones = copy.deepcopy(clip.get("bones", {}))
                for tier in range(1, tiers):
                    for name, movement in original_bones.items(): clip.setdefault("bones", {})[f"stack{tier}_"+name] = copy.deepcopy(movement)
            write(assets / f"geckolib/models/entity/nouveau/{base}_stack{tiers}.geo.json", stacked)
            write(assets / f"geckolib/animations/entity/nouveau/{base}_stack{tiers}.animation.json", clips)
    sounds = json.loads((ASSET / "sounds.json").read_text(encoding="utf-8"))
    existing = json.loads((assets / "sounds.json").read_text(encoding="utf-8")) if (assets / "sounds.json").exists() else {}
    for key, value in sounds.items(): existing["nouveau." + key] = walk(value, asset_id)
    write(assets / "sounds.json", existing)
    write(assets.parents[1] / "jugcraft/nouveau/sounds.json", list(sounds))
    for group in ("painting_variant", "jukebox_song"):
        folder = DATA / group
        if not folder.exists(): continue
        for path in folder.glob("*.json"):
            value = walk(json.loads(path.read_text(encoding="utf-8")), asset_id)
            if group == "jukebox_song" and "sound_event" in value:
                original = json.loads(path.read_text(encoding="utf-8"))["sound_event"]
                value["sound_event"] = "jugcraft:nouveau." + original.split(":")[-1]
                value["description"] = {"translate": "jukebox_song.jugcraft.nouveau_" + path.stem}
            write(data / group / ("nouveau_" + path.name), value)
    for key, text in LANG.items():
        if key.startswith("entity.ars_jimbaux."): lang[key.replace("entity.ars_jimbaux.", "entity.jugcraft.nouveau_")] = text
    lang["entity.jugcraft.nouveau_familiar"] = "Bound Familiar"
    lang["entity.jugcraft.nouveau_animated_block"] = "Animated Block"
    for name, spec in CATALOG["blocks"].items():
        if spec["kind"] != "station": continue
        loot = data / f"loot_table/blocks/{spec['id']}.json"
        value = json.loads(loot.read_text(encoding="utf-8"))
        if not value.get("pools"): continue
        entry = value["pools"][0]["entries"][0]
        entry.setdefault("modifier", []).append({"type": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:custom_name", "minecraft:potion_contents", "jugcraft:nouveau_jar_doses", "jugcraft:ley_charge", "jugcraft:nouveau_captured_entity"]})
        write(loot, value)


def nature_data(write, data, self_drop):
    def tag(namespace, kind, name, entries):
        file = data.parent / namespace / "tags" / kind / (name + ".json")
        existing = json.loads(file.read_text(encoding="utf-8")) if file.exists() else {"values": []}
        existing["values"] = list(dict.fromkeys(existing.get("values", []) + entries))
        write(file, existing)
    def drop(item, count=1, condition=None):
        entry = {"type": "minecraft:item", "name": item}
        if count != 1: entry["modifier"] = [{"type": "minecraft:set_count", "count": count}]
        pool = {"rolls": 1, "entries": [entry]}
        if condition: pool["condition"] = condition
        return pool
    for color in ("blue", "green", "purple", "red"):
        prefix = color + "_archwood"
        log, leaves, sapling = ["jugcraft:" + id_of(prefix + "_" + part) for part in ("log", "leaves", "sapling")]
        tree = {"type": "minecraft:tree", "below_trunk_provider": "minecraft:soil_beneath_tree", "decorators": [],
                "trunk_provider": {"id": log, "properties": {"axis": "y"}},
                "foliage_provider": {"id": leaves, "properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}},
                "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 7, "height_rand_a": 4, "height_rand_b": 2},
                "foliage_placer": {"type": "minecraft:blob_foliage_placer", "height": 4, "offset": 0, "radius": 3},
                "minimum_size": {"type": "minecraft:two_layers_feature_size"}, "ignore_vines": True}
        write(data / f"worldgen/feature/nouveau/{prefix}.json", tree)
        write(data / f"worldgen/placed_feature/nouveau/{prefix}.json", {"feature": f"jugcraft:nouveau/{prefix}", "placement": [
            {"type": "minecraft:rarity_filter", "chance": 24}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}]})
        pools = [drop(leaves, condition={"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]}),
                 drop(sapling, condition={"type": "minecraft:all_of", "terms": [
                     {"type": "minecraft:inverted", "term": {"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]}},
                     {"type": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [.08, .1, .12, .15]}]})]
        write(data / f"loot_table/blocks/{id_of(prefix + '_leaves')}.json", {"type": "minecraft:block", "pools": pools})
        for kind in ("block", "item"):
            tag("minecraft", kind, "logs", ["jugcraft:" + id_of(n) for n in (prefix+"_log", prefix+"_wood", "stripped_"+prefix+"_log", "stripped_"+prefix+"_wood")])
            tag("minecraft", kind, "leaves", [leaves]); tag("minecraft", kind, "saplings", [sapling])
    tag("c", "item", "gems/source", ["jugcraft:" + id_of("source_gem")])
    tag("c", "item", "storage_blocks/source", ["jugcraft:" + id_of("source_gem_block")])
    for name, spec in CATALOG["blocks"].items():
        block = "jugcraft:" + spec["id"]
        if spec["kind"] in {"stairs", "slab", "fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate"}:
            plural = {"fence_gate": "fence_gates", "pressure_plate": "pressure_plates"}.get(spec["kind"], spec["kind"] + "s")
            for kind in ("block", "item"): tag("minecraft", kind, plural, [block])
        if "sourcestone" in name: tag("minecraft", "block", "mineable/pickaxe", [block])
        if "archwood" in name and spec["kind"] not in {"leaves", "sapling"}: tag("minecraft", "block", "mineable/axe", [block])
    for name, produce, age in (("magebloom_crop", "magebloom", 7), ("sourceberry_bush", "sourceberry_bush", 3)):
        block = "jugcraft:" + id_of(name)
        write(data / f"loot_table/blocks/{id_of(name)}.json", {"type": "minecraft:block", "pools": [drop(block),
            drop("jugcraft:" + id_of(produce), {"type": "minecraft:uniform", "min": 1, "max": 3},
                 {"type": "minecraft:match_block", "blocks": block, "state": {"age": str(age)}})]})
    write(data / "worldgen/feature/nouveau/sourceberries.json", {"type": "minecraft:simple_block", "to_place": {"id": "jugcraft:" + id_of("sourceberry_bush"), "properties": {"age": "3"}}})
    write(data / "worldgen/placed_feature/nouveau/sourceberries.json", {"feature": "jugcraft:nouveau/sourceberries", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 12}, {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})


def item_tags():
    """Membership in vanilla equipment tags, merged by the canonical shared writer."""
    result = {}
    for path in (SOURCE/'data/minecraft/tags/item').rglob('*.json'):
        entries = [resource_id(v) for v in json.loads(path.read_text(encoding='utf-8')).get('values',[]) if isinstance(v,str) and v.lstrip('#').startswith('ars_jimbaux:')]
        tag=path.relative_to(SOURCE/'data/minecraft/tags/item').as_posix().removesuffix('.json')
        if entries and (tag.startswith('enchantable/') or tag.endswith('_armor') or tag=='swords'):result[tag] = entries
    return result


def entity_id(value):
    aliases = {'minecraft:boat':'#minecraft:boat', 'minecraft:chest_boat':'#minecraft:boat', 'minecraft:potion':'minecraft:splash_potion', 'ars_jimbaux:bubble':'minecraft:area_effect_cloud', 'ars_jimbaux:follow_proj':'minecraft:arrow', 'ars_jimbaux:flying_item':'minecraft:item', 'ars_jimbaux:lily':'jugcraft:starbuncle'}
    if value.startswith('ars_jimbaux:familiar_'): return 'jugcraft:nouveau_familiar'
    return aliases.get(value, resource_id(value))

# Runtime type families registered by NouveauHelpers / NouveauEncounters / NouveauStatuses.
ENTITY_IDS = {'book_wyrm','starbuncle'} | {'nouveau_'+n for n in ('familiar','animated_block','archwood_boat','chimera_spike','potion_bolt','drygmy','whirlisprig','wixie','amethyst_golem','alakarkinos','wilden_hunter','wilden_guardian','wilden_stalker','wilden_chimera')} | {'nouveau_'+tree+'_weald_'+age for tree in ('blazing','cascading','flourishing','vexing') for age in ('walker','waddler')}
EFFECT_IDS = {'nouveau_'+n for n in ('blasting','bounce','flight','freezing','glide','gravity','hex','immolate','looting','magic_find','mana_regen','recovery','scrying','shielding','shocked','snared','soaked','spell_damage','summoning_sickness')}
