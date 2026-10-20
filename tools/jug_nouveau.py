"""Jug Nouveau: preserve the owner's resources; register only implemented gameplay.

The full source manifest tracks the ars_jimbaux upload, including data. Nothing in
the library is executed. Models/animation/textures used below are copied unchanged.
"""
from pathlib import Path
import csv
import json
import nouveau_content

ROOT = Path(__file__).resolve().parents[1]
MAGIC = ROOT / "art/owner-library/originals/Magic"
SOURCE = "assets/ars_jimbaux/"
COLORS = ("black", "blue", "green", "purple", "red", "white")
STAR_COLORS = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
               "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")
ITEMS = {
    "bookwyrm_charm": {"name": "Book Wyrm Charm", "tooltip": "Use on your Storage Lectern to bind a Book Wyrm. Four per lectern; eight chest links each."},
    "dominion_wand": {"name": "Dominion Wand", "tooltip": "Select a lectern to link storage, or a Starbuncle to set its pickup chest and delivery chest."},
    "starbuncle_charm": {"name": "Starbuncle Charm", "tooltip": "Use on the ground to summon your courier. Select it with a Dominion Wand, then select a pickup chest and delivery chest."},
}
BLOCKS = {"bookwyrm_lectern": {"name": "Storage Lectern"}}
ITEMS.update({value["id"]: {"name": value["name"], "tooltip": "Jug Nouveau · " + value["name"] + ". See its encyclopedia entry for use and recipes."} for value in nouveau_content.CATALOG["items"].values() if value["id"] not in ITEMS})
BLOCKS.update({value["id"]: {"name": value["name"]} for value in nouveau_content.CATALOG["blocks"].values()
               if value["kind"] not in {"internal", "wall_sign", "hanging_wall_sign", "potted"} and value["id"] not in BLOCKS})
OWNER_FILES = {
    **nouveau_content.OWNER_FILES,
    "geckolib/models/entity/book_wyrm.geo.json": SOURCE + "geo/book_wyrm.geo.json",
    "geckolib/animations/entity/book_wyrm.animation.json": SOURCE + "animations/book_wyrm_animation.json",
    "geckolib/models/entity/starbuncle.geo.json": SOURCE + "geo/starbuncle.geo.json",
    "geckolib/animations/entity/starbuncle.animation.json": SOURCE + "animations/starbuncle_animations.json",
    **{f"textures/entity/starbuncle_{color}.png": SOURCE + f"textures/entity/starbuncle_{color}.png" for color in STAR_COLORS},
    "geckolib/models/block/bookwyrm_lectern.geo.json": SOURCE + "geo/book_wyrm_lectern.geo.json",
    "geckolib/animations/block/bookwyrm_lectern.animation.json": SOURCE + "animations/book_wyrm_lectern_animations.json",
    "geckolib/models/item/bookwyrm_lectern.geo.json": SOURCE + "geo/book_wyrm_lectern.geo.json",
    "geckolib/animations/item/bookwyrm_lectern.animation.json": SOURCE + "animations/book_wyrm_lectern_animations.json",
    "textures/block/bookwyrm_lectern.png": SOURCE + "textures/block/book_wyrm_lectern.png",
    **{f"textures/entity/book_wyrm_{color}.png": SOURCE + f"textures/entity/book_wyrm_{color}.png" for color in COLORS},
    **{f"textures/item/{name}.png": SOURCE + f"textures/item/{name}.png" for name in ("bookwyrm_charm", "dominion_wand", "starbuncle_charm")},
}


def source_binding(name):
    """Account for supplied resources without treating byte copies as validated gameplay."""
    a='assets/ars_jimbaux/';d='data/ars_jimbaux/'
    if name.startswith(a):
        rel=name[len(a):];family=rel.split('/')[0];tail=rel.split('/',1)[1] if '/' in rel else rel
        if family in ('textures','sounds','models','particles'): return {'adapter':'nouveau_content','runtime':['assets/jugcraft/'+family+'/nouveau/'+tail]}
        if family=='geo':return {'adapter':'nouveau_content.visual_data','runtime':['assets/jugcraft/geckolib/models/'+category+'/nouveau/'+tail for category in ('entity','block','item','armor')]}
        if family=='animations':return {'adapter':'nouveau_content.visual_data / presentation','runtime':[],'note':'Original clips are bound to the matching geometry; obsolete renderer variants remain source references.'}
        if family=='blockstates':
            key=Path(tail).stem
            aliases={'crystallizer':'imbuement_chamber','ritual':'ritual_brazier','source_gem':'source_gem_block','stripped_archwood_log':'stripped_blue_archwood_log','stripped_archwood_wood':'stripped_blue_archwood_wood','temporary_block':'mage_block'}
            key=aliases.get(key,key)
            return {'adapter':'nouveau_models / native block families','runtime':['assets/jugcraft/blockstates/'+nouveau_content.id_of(key)+'.json'],'note':'Legacy renderer names resolve to the corresponding native block; crystallizer has no supplied model/recipe and is the old imbuement reference.'}
        if family=='lang':return {'adapter':'nouveau_data / nouveau_guide','runtime':['assets/jugcraft/lang/'+tail],'note':'Registry keys translated; supplied descriptions indexed in the shared guide.'}
        if family=='sounds.json':return {'adapter':'nouveau_data.finalize_resources','runtime':['assets/jugcraft/sounds.json']}
        if family=='entity':return {'adapter':'NouveauChestRenderer / NouveauBoat / native sign models','runtime':['assets/jugcraft/textures/nouveau/entity/'+tail],'note':'Source files stored outside textures are normalized to native texture locations.'}
        if family=='dynamiclights':return {'adapter':'NouveauFollowingLight / NouveauSpellDelivery / nouveau_data','runtime':[],'note':'Optional native item/entity definitions and emissive spell rendering replace the old custom luminance types.'}
        if family=='shaders':return {'adapter':'NouveauSkyView / NouveauScrySightClient / NouveauFacadeRenderer','runtime':[],'note':'Old loader shaders are preserved as references; supported native render pipelines implement their effects.'}
    if name.startswith(d):
        rel=name[len(d):];family=rel.split('/')[0];tail=rel.split('/',1)[1] if '/' in rel else rel
        if family=='recipe':
            original=json.loads((MAGIC/name).read_text(encoding='utf-8'));folder,_=nouveau_content.adapt_recipe(original)
            return {'adapter':'nouveau_content.adapt_recipe / NouveauRecipes / NouveauFlaskRecipe','runtime':['data/jugcraft/'+folder+'/nouveau/'+tail]}
        if family=='tags':
            kind,rest=tail.split('/',1)
            if kind=='worldgen': sub,rest=rest.split('/',1);kind+='/'+sub
            return {'adapter':'nouveau_content / nouveau_data','runtime':['data/jugcraft/tags/'+kind+'/nouveau_'+rest]}
        if family=='worldgen':return {'adapter':'nouveau_data / NouveauArchwoodFeature / shared biome generator','runtime':[],'note':'Native worldgen schemas, shared terrain rules, converted structures and original biome/feature identities.'}
        if family=='structure':return {'adapter':'nouveau_nbt','runtime':['data/jugcraft/structure/nouveau_'+tail]}
        if family in ('loot_modifiers','neoforge'):return {'adapter':'NouveauResources / NouveauSpawns / NouveauNature / NouveauHelpers / NouveauEncounters','runtime':[],'note':'Fabric events replace NeoForge global modifiers.'}
        if family=='curios':return {'adapter':'nouveau_data / shared Trinkets slots','runtime':[],'note':'Existing shared slots replace a second accessory inventory.'}
        if family=='dimension_type':return {'adapter':'nouveau_data / NouveauPlanarium','runtime':['data/jugcraft/dimension_type/nouveau_planarium.json']}
        if family in ('loot_table','loot_tables'):return {'adapter':'nouveau_data','runtime':[],'note':'Original entity/chest rolls retained; block-family adapters preserve native drops and components.'}
        if family in ('advancement','banner_pattern','damage_type','enchantment','jukebox_song','painting_variant'):return {'adapter':'nouveau_data / native registries','runtime':['data/jugcraft/'+family+'/nouveau_'+tail]}
    return {'adapter':'preserved source reference','runtime':[],'note':'Requires explicit classification before claiming complete coverage.'}


def source_manifest():
    imported = {}
    for target, source in OWNER_FILES.items():
        imported.setdefault(source, []).append(target)
    rows = []
    with (ROOT / "art/owner-library/catalog/files.csv").open(encoding="utf-8-sig", newline="") as stream:
        for entry in csv.DictReader(stream):
            name = entry["path"].removeprefix("originals/Magic/")
            if name.startswith((SOURCE, "data/ars_jimbaux/")):
                rows.append({"source": name, "sha256": entry["sha256"], "kind": entry["kind"],
                             "jug_nouveau_runtime": imported.get(name),
                             "binding": source_binding(name),
                             "status": "native_adapter_recorded; see feature record for validation"})
    return {"collection": "Jug Nouveau (supplied namespace ars_jimbaux)",
            "scope": "Tracks this port; earlier Jugcraft art imports remain recorded by their own feature manifests.",
            "source_files": len(rows), "files": rows}


def write_all(write, assets, data, lang, condition, self_drop):
    nouveau_content.write_all(write, assets, data, lang, condition, self_drop)
    for target, source in OWNER_FILES.items():
        if target.endswith(".json"):
            # Preserve owner geometry/UVs/animation channels; only normalize line endings.
            (assets / target).parent.mkdir(parents=True, exist_ok=True)
            (assets / target).write_bytes((MAGIC / source).read_bytes().replace(b"\r\n", b"\n"))
    names = {**{key: value["name"] for key, value in ITEMS.items() if key in nouveau_content.EXISTING}, "bookwyrm_lectern": "Storage Lectern"}
    for name, title in names.items():
        lang[("block" if name == "bookwyrm_lectern" else "item") + ".jugcraft." + name] = title
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"jugcraft:item/{name}"}})
        if name != "bookwyrm_lectern":
            write(assets / "models/item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jugcraft:item/{name}"}})
        write(data / "spell_assignments" / f"{name}.json", {"access": "NONE", "access_param": ""})
    write(assets / "items/bookwyrm_lectern.json", {"model": {"type": "minecraft:special", "base": "jugcraft:item/bookwyrm_lectern", "model": {"type": "geckolib:geckolib"}}})
    write(assets / "models/item/bookwyrm_lectern.json", {"parent": "minecraft:block/block", "textures": {"particle": "jugcraft:block/bookwyrm_lectern"}, "display": {
        "gui": {"rotation": [15, 135, 0], "translation": [0, -3, 0], "scale": [0.48, 0.48, 0.48]},
        "ground": {"translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"scale": [0.4, 0.4, 0.4]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]}}})
    write(assets / "blockstates/bookwyrm_lectern.json", {"variants": {"": {"model": "jugcraft:block/bookwyrm_lectern"}}})
    write(assets / "models/block/bookwyrm_lectern.json", {"textures": {"particle": "jugcraft:block/bookwyrm_lectern"}, "elements": []})
    write(data / "loot_table/blocks/bookwyrm_lectern.json", self_drop("bookwyrm_lectern"))
    recipes = {
        "bookwyrm_lectern": ([" B ", "ALA", " G "], {"B": "minecraft:book", "A": "minecraft:amethyst_shard", "L": "minecraft:lectern", "G": "minecraft:gold_ingot"}),
        "bookwyrm_charm": ([" A ", "GBG", " A "], {"A": "minecraft:amethyst_shard", "G": "minecraft:gold_ingot", "B": "minecraft:book"}),
        "dominion_wand": (["  A", " S ", "G  "], {"A": "minecraft:amethyst_shard", "S": "minecraft:stick", "G": "minecraft:gold_nugget"}),
    }
    (data / "recipe/starbuncle_charm.json").unlink(missing_ok=True)
    for name, (pattern, key) in recipes.items():
        write(data / "recipe" / f"{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
              "key": key, "result": {"id": f"jugcraft:{name}", "count": 1}, "fabric:load_conditions": condition("concordance")})
    lang.update({"entity.jugcraft.book_wyrm": "Book Wyrm",
        "screen.jugcraft.bookwyrm.title": "Book Wyrm Storage", "screen.jugcraft.bookwyrm.previous": "Previous",
        "screen.jugcraft.bookwyrm.next": "Next", "screen.jugcraft.bookwyrm.unlink": "Unlink chest",
        "screen.jugcraft.bookwyrm.page": "Chest %s / %s", "screen.jugcraft.bookwyrm.wyrms": "%s / 4 Book Wyrms",
        "screen.jugcraft.bookwyrm.empty": "No linked chests", "screen.jugcraft.bookwyrm.offline": "Chest unavailable",
        "screen.jugcraft.bookwyrm.help": "Use a charm on the lectern. Select it with a Dominion Wand, then use the wand on chests or barrels within 30 blocks. Sneak-use a linked chest to unlink it.",
        "screen.jugcraft.bookwyrm.help_button": "Next setup tip",
        "screen.jugcraft.bookwyrm.step0": "1. Use a charm on your Storage Lectern.",
        "screen.jugcraft.bookwyrm.step1": "2. Select it with a Dominion Wand.",
        "screen.jugcraft.bookwyrm.step2": "3. Use the wand on a nearby chest or barrel.",
        "screen.jugcraft.bookwyrm.step3": "Each wyrm links eight chests within 30 blocks.",
        "screen.jugcraft.bookwyrm.setup": "Link chests with your Dominion Wand",
        "screen.jugcraft.bookwyrm.inventory": "Your inventory",
        "message.jugcraft.bookwyrm.selected": "Lectern selected. Use the wand on nearby chests or barrels.",
        "message.jugcraft.bookwyrm.select_first": "Select your Storage Lectern with the wand first.",
        "message.jugcraft.bookwyrm.linked": "Chest linked (%s / %s).",
        "message.jugcraft.bookwyrm.unlinked": "Chest unlinked. Its contents stay where they are.",
        "message.jugcraft.bookwyrm.refused": "Cannot link here: check ownership, distance, chest access and Book Wyrm capacity.",
        "message.jugcraft.bookwyrm.bound": "Book Wyrm bound! Each wyrm supports eight nearby chests.",
        "message.jugcraft.bookwyrm.full": "This lectern already has four Book Wyrms.",
        "message.jugcraft.bookwyrm.status": "Book Wyrm — tending its Storage Lectern. Dye it with white, black, blue, green, red or purple dye.",
        "tooltip.jugcraft.bookwyrm_charm": "Use on your Storage Lectern to bind a Book Wyrm. Four per lectern; eight chest links each.",
        "tooltip.jugcraft.dominion_wand": ITEMS["dominion_wand"]["tooltip"],
        "tooltip.jugcraft.bookwyrm_lectern": "Access linked storage through your Book Wyrms. Items stay in their real chests.",
        "entity.jugcraft.starbuncle": "Starbuncle",
        "message.jugcraft.concordance.workers.kind.starbuncle": "Starbuncle",
        "tooltip.jugcraft.starbuncle_charm": ITEMS["starbuncle_charm"]["tooltip"],
        "tooltip.jugcraft.dominion_wand.starbuncle": "Starbuncle mode: pickup chest → delivery chest. Sneak-use to restart pickup selection.",
        "message.jugcraft.starbuncle.summoned": "Starbuncle summoned! Use your Dominion Wand on it to set up deliveries.",
        "message.jugcraft.starbuncle.selected": "Starbuncle selected. Use the wand on its pickup chest, then its delivery chest.",
        "message.jugcraft.starbuncle.source": "Pickup chest selected. Now select a delivery chest within 32 blocks.",
        "message.jugcraft.starbuncle.assigned": "Route ready! Your Starbuncle carries up to 16 items per trip. Keep a walkable path clear.",
        "message.jugcraft.starbuncle.refused": "Cannot use here. Check ownership, unlocked chests, nearby loaded Starbuncle and clear space.",
        "message.jugcraft.starbuncle.route_refused": "Route refused: use two accessible chests within 32 blocks. Finish or recover any current cargo before rerouting.",
        "message.jugcraft.starbuncle.limit": "You may keep eight Starbuncles, within the shared 32-worker limit. Sneak-use one empty-handed to pack it up.",
        "message.jugcraft.starbuncle.status": "%s · Carrying %s · %s → %s",
    })


def codex():
    import nouveau_guide
    return {**nouveau_guide.codex(),("binding", "book_wyrms"): {
        "name": "Book Wyrm Storage", "x": 6, "y": 0, "icon": "jugcraft:bookwyrm_charm", "condition": None,
        "description": "Your flying librarians and a linked chest workshop",
        "pages": [
            ("crafting_recipe", "A Place for Your Wyrm", "Craft and place a **Storage Lectern**, then use a **Book Wyrm Charm** on it. Each wyrm lets the lectern reach **eight** nearby chests or barrels; four wyrms support **32** links. No research unlock is needed. The supplied wyrm and floating ledger animate in the workshop.", "jugcraft:bookwyrm_lectern"),
            ("crafting_recipe", "Bind a Book Wyrm", "One charm binds one wyrm. Use black, blue, green, purple, red or white dye on your wyrm to choose its color. Breaking the lectern returns its bound charms and removes the corresponding wyrms; the storage contents stay in their original chests.", "jugcraft:bookwyrm_charm"),
            ("crafting_recipe", "Connect Your Storage", "Use a **Dominion Wand** on the lectern to select it, then on each chest or barrel within **30 blocks**. Sneak-use a linked chest to remove its link. The owner's party can use the lectern, but every linked container must also permit access. Unlock locked containers directly first. Repositories and Archwood chests can also be linked; Ender chests remain private. Empty lecterns can link to another lectern as a remote terminal.", "jugcraft:dominion_wand"),
            ("text", "Browse and Move Items", "Use the lectern empty-handed. Search the combined item catalog, click to request a stack, right-click for one, or shift-click to withdraw directly. Craft with the adjacent grid and cycle named inventory groups. Sneak-use opens the older individual-container view. A double chest's halves can be linked separately. Put down any cursor-held stack before changing pages. **Unlink chest** removes only the connection. The setup-tip button cycles through the steps."),
            ("text", "When a Link Is Unavailable", "A lost wyrm disables excess links; binding a replacement restores that capacity. An unloaded, locked or newly protected chest stays unavailable. The lectern never loads chunks or copies items into a second hidden inventory. Walk away or remove the lectern and access closes. Book Wyrms visit the links visually; they do not carry a second copy of the contents."),
        ]}, ("binding", "starbuncles"): {
        "name": "Starbuncle Deliveries", "x": 6, "y": 2, "icon": "jugcraft:starbuncle_charm", "condition": None,
        "description": "A walking courier for your connected workshop",
        "pages": [
            ("text", "Meet Your Courier", "Craft a **Starbuncle Charm** and use it on clear ground. You can keep **eight** Starbuncles within the shared 32-worker roster. No research is required. Offer a gold nugget to a wild Starbuncle, then enchant its token with four gold ingots at the apparatus."),
            ("text", "Set a Delivery Route", "Use a **Dominion Wand** on your Starbuncle, then on a **pickup** chest or barrel, then on a **delivery** chest or barrel. The two containers must be different and within **32 blocks** of each other. Stay near the Starbuncle while setting up. Sneak-use a chest to restart pickup selection; use the wand on a lectern to return to storage-link mode."),
            ("text", "Real Carrying", "Your courier walks to pickup, takes up to **16 items** of one kind, and walks to delivery. Keep a clear walkable route. Each double-chest half is separate. It works while you are in the dimension, unless your server enables absent-owner workers. Items keep their names and components. No power, food upkeep or additional currency is required."),
            ("text", "If Work Stops", "Empty pickup, full destination, blocked paths, locked/protected storage or unloaded chunks make the worker wait and report its status. Use it empty-handed to inspect. It never loads chunks. A carrying worker cannot be rerouted. **Sneak-use empty-handed** to pack it up: the charm and actual cargo drop at its feet. Death returns them too; unload/reload keeps them on the same entity."),
            ("text", "Colors and Limits", "Use any vanilla dye to choose among all **16 supplied colors**. Packing preserves the color on the charm. Your party can manage your couriers, but storage permissions are checked again as their owner at every transfer. Repositories and Archwood chests are supported; Ender chests remain private. Apply filter scrolls, a path block, shades, a Wixie hat, or extra charms to configure the courier. Use the Dominion Wand to select a new route once its current cargo is delivered."),
        ]}}


if __name__ == "__main__":
    path = ROOT / "docs/features/jug-nouveau-source-manifest.json"
    path.write_text(json.dumps(source_manifest(), indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Recorded {source_manifest()['source_files']} source files; no library code executed.")
