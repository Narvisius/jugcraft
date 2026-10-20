"""Survival availability for native Nouveau workshop outputs, including their real stations."""
import json

STATIONS = {"imbuement":"imbuement_chamber","enchanting_apparatus":"enchanting_apparatus",
            "glyph":"scribes_table","dye":"alteration_table","book_upgrade":"alteration_table",
            "caster_tome":"scribes_table","potion_flask":"potion_melder"}

def add_recipes(data, recipes):
    for path in (data/"jugcraft/nouveau_recipe").rglob("*.json"):
        r=json.loads(path.read_text(encoding="utf-8"));kind=r.get("type")
        if kind not in STATIONS:continue
        result=r.get("result",r.get("output"))
        if kind=="caster_tome":
            result={"id":"jugcraft:nouveau_caster_tome"};r={**r,"ingredients":["minecraft:book","minecraft:paper","jugcraft:nouveau_source_gem"]}
        if not isinstance(result,dict) or not ("id" in result or "item" in result):continue
        ingredients=[]
        for key in ("input","reagent"):
            if key in r:ingredients.append(r[key])
        for key in ("inputs","ingredients","pedestalItems"):
            ingredients.extend(r.get(key,[]))
        ingredients.append("jugcraft:nouveau_"+STATIONS[kind])
        if kind != "imbuement" and r.get("sourceCost",r.get("source",0))>0:
            ingredients += ["jugcraft:nouveau_source_jar","jugcraft:nouveau_agronomic_sourcelink"]
        if kind=="enchanting_apparatus":ingredients.append("jugcraft:nouveau_arcane_core")
        recipes.setdefault(result.get("id",result.get("item")),[]).append(ingredients)
    for color in ("blue","green","red","purple"):
        recipes.setdefault("jugcraft:nouveau_stripped_"+color+"_archwood_log",[]).append(["jugcraft:nouveau_"+color+"_archwood_log","minecraft:iron_axe"])
    recipes.setdefault("jugcraft:nouveau_spell_parchment",[]).append(["jugcraft:nouveau_blank_parchment","jugcraft:nouveau_scribes_table","jugcraft:nouveau_novice_spell_book"])
    recipes.setdefault("jugcraft:nouveau_magebloom",[]).append(["jugcraft:nouveau_magebloom_crop"])
    recipes.setdefault("jugcraft:nouveau_wilden_tribute",[]).append(["jugcraft:nouveau_ritual_wilden_summon","jugcraft:nouveau_ritual_brazier","jugcraft:nouveau_wilden_horn","jugcraft:nouveau_wilden_wing","jugcraft:nouveau_wilden_spike","jugcraft:nouveau_source_jar","jugcraft:nouveau_agronomic_sourcelink"])
