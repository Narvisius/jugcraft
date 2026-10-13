"""Roadmap step 21: bounded material equivalence (docs/features/arcane-concordance-equivalence.md).

The Balancewrights' Assay. A restricted catalogue of mundane materials each has an exact Prima Materia value (grains a
unit, as a fraction), and says whether an Assayer's Scale may dissolve it into grains or form it from them. Dissolving
gives the batch's exact value rounded down once; forming costs FORM_MARKUP of it, rounded up once; so nothing is ever
rounded into existence and every round trip loses. Only catalogued, plain items are weighed: anything carrying an
inventory, a unique identity, a bound creature, other data or magic, or tagged as excluded, never is (Java:
equivalence/Eligibility.java), whatever a player holds or a recipe viewer shows.

The conversion graph is declared here too: the vanilla recipes between catalogued materials (with the containers they
give back and their byproducts), and the external sources that legitimately add matter (a cobblestone generator, farms,
bees). tools/check_mod_data.py and Java's CycleAudit both refuse a catalogue in which any recipe, or the scale's own
dissolving and forming, gains value, and search every cycle for a profitable one; sources are kept apart.

The definitions are data (data/jugcraft/concordance/{material,transmutation}, read by Java's concordance/equivalence;
the server side is concordance/assay). tools/concordance.py merges these tables into its own.
"""

from fractions import Fraction

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# Java: equivalence/Assay.java, equivalence/EquivalenceParser.java, equivalence/CycleAudit.java.
FORM_MARKUP = Fraction(5, 4)
MAX_BALANCE = 1_000_000
MAX_BATCH = 64
MAX_GRAINS = 100_000
MAX_PER = 64
MAX_COUNT = 64
MAX_LENGTH = 6
MAX_CYCLES = 20_000
PRIMA = rid("prima_materia")
# The scale (Java: assay/Assaying.java).
CONFIRM_TICKS = 200
EXCLUDED_TAG = rid("equivalence/excluded")

REASONS = {
    "excluded": "the scale will not weigh it: it is more than matter",
    "magical": "it carries magic the scale will not weigh",
    "inventory": "it holds other things",
    "unique": "it is one of a kind (named, written or marked)",
    "bound": "it carries a creature or a block's own state",
    "metadata": "it is not plain: something about it has been changed",
    "uncatalogued": "the scale does not know its worth",
    "not_dissolvable": "the scale knows its worth but will not dissolve it",
    "not_formable": "the scale will not form it",
    "too_poor": "you have too few grains",
    "full": "your ledger cannot hold more grains",
    "unbalanced": "the scale is out of balance: an operator must fix its data (/jugcraft concordance equivalence audit)",
}

# ------------------------------------------------------------------------------------------------- the catalogue

def value(grains, per=1):
    return {"grains": grains, "per": per}


# item: (grains, per, dissolve, form). Values follow the recipes below so that no recipe gains: a block is exactly its
# nine ingots, a log its four planks; smelting is worth less than its input and fuel; farm goods are cheap.
MATERIALS = {
    "minecraft:cobblestone": (1, 1, True, True),
    "minecraft:dirt": (1, 1, True, True),
    "minecraft:sand": (1, 1, True, True),
    "minecraft:gravel": (1, 1, True, True),
    "minecraft:stone": (2, 1, True, True),
    "minecraft:glass": (2, 1, True, True),
    "minecraft:sandstone": (4, 1, True, True),
    "minecraft:clay_ball": (4, 1, True, True),
    "minecraft:clay": (16, 1, True, True),
    "minecraft:oak_sapling": (1, 1, False, False),
    "minecraft:oak_log": (16, 1, True, True),
    "minecraft:oak_planks": (4, 1, True, True),
    "minecraft:stick": (2, 1, True, True),
    "minecraft:coal": (32, 1, True, True),
    "minecraft:coal_block": (288, 1, True, True),
    "minecraft:iron_nugget": (256, 9, True, False),
    "minecraft:iron_ingot": (256, 1, True, True),
    "minecraft:iron_block": (2304, 1, True, True),
    "minecraft:bucket": (768, 1, True, False),
    "minecraft:copper_ingot": (64, 1, True, True),
    "minecraft:copper_block": (576, 1, True, True),
    "minecraft:gold_nugget": (1024, 9, True, False),
    "minecraft:gold_ingot": (1024, 1, True, True),
    "minecraft:gold_block": (9216, 1, True, True),
    "minecraft:redstone": (32, 1, True, True),
    "minecraft:redstone_block": (288, 1, True, True),
    "minecraft:lapis_lazuli": (64, 1, True, True),
    "minecraft:lapis_block": (576, 1, True, True),
    "minecraft:quartz": (64, 1, True, True),
    "minecraft:quartz_block": (256, 1, True, True),
    "minecraft:glass_bottle": (2, 1, True, True),
    "minecraft:honey_bottle": (16, 1, True, False),
    "minecraft:honey_block": (56, 1, True, False),
    "minecraft:wheat_seeds": (1, 1, True, True),
    "minecraft:wheat": (4, 1, True, True),
    "minecraft:hay_block": (36, 1, True, True),
    "minecraft:bread": (12, 1, True, False),
    "minecraft:sugar_cane": (4, 1, True, True),
    "minecraft:sugar": (4, 1, True, True),
    "minecraft:paper": (4, 1, True, True),
    "minecraft:string": (4, 1, True, True),
    "minecraft:white_wool": (16, 1, True, True),
    "minecraft:bone": (6, 1, True, True),
    "minecraft:bone_meal": (2, 1, True, True),
    "minecraft:bone_block": (18, 1, True, True),
}


def recipe(inputs, outputs, via, pattern=None, key=None, returns=None, byproducts=None, catalysts=None, kind="recipe"):
    entry = {"kind": kind, "via": via, "inputs": inputs, "outputs": outputs}
    for name, table in (("returns", returns), ("byproducts", byproducts), ("catalysts", catalysts)):
        if table:
            entry[name] = table
    if pattern:
        entry["pattern"] = pattern
        entry["key"] = key
    return entry


def compact(small, big, count=9):
    """Nine of a small thing make one big one, and one big one gives nine back (both directions are recipes)."""
    rows = ["###", "###", "###"] if count == 9 else ["##", "##"]
    return {f"{big.split(':')[1]}_from_{small.split(':')[1]}": recipe({small: count}, {big: 1}, "crafting", rows, {"#": small}),
            f"{small.split(':')[1]}_from_{big.split(':')[1]}": recipe({big: 1}, {small: count}, "crafting", ["#"], {"#": big})}


TRANSMUTATIONS = {
    "oak_planks_from_oak_log": recipe({"minecraft:oak_log": 1}, {"minecraft:oak_planks": 4}, "crafting", ["#"], {"#": "minecraft:oak_log"}),
    "stick_from_oak_planks": recipe({"minecraft:oak_planks": 2}, {"minecraft:stick": 4}, "crafting", ["#", "#"], {"#": "minecraft:oak_planks"}),
    **compact("minecraft:iron_ingot", "minecraft:iron_block"),
    **compact("minecraft:iron_nugget", "minecraft:iron_ingot"),
    **compact("minecraft:copper_ingot", "minecraft:copper_block"),
    **compact("minecraft:gold_ingot", "minecraft:gold_block"),
    **compact("minecraft:gold_nugget", "minecraft:gold_ingot"),
    **compact("minecraft:redstone", "minecraft:redstone_block"),
    **compact("minecraft:coal", "minecraft:coal_block"),
    **compact("minecraft:lapis_lazuli", "minecraft:lapis_block"),
    **compact("minecraft:wheat", "minecraft:hay_block"),
    **compact("minecraft:bone_meal", "minecraft:bone_block"),
    "bucket_from_iron_ingot": recipe({"minecraft:iron_ingot": 3}, {"minecraft:bucket": 1}, "crafting", ["# #", " # "],
                                     {"#": "minecraft:iron_ingot"}),
    "sandstone_from_sand": recipe({"minecraft:sand": 4}, {"minecraft:sandstone": 1}, "crafting", ["##", "##"], {"#": "minecraft:sand"}),
    "clay_from_clay_ball": recipe({"minecraft:clay_ball": 4}, {"minecraft:clay": 1}, "crafting", ["##", "##"], {"#": "minecraft:clay_ball"}),
    "clay_ball_from_clay": recipe({"minecraft:clay": 1}, {"minecraft:clay_ball": 4}, "breaking"),
    "quartz_block_from_quartz": recipe({"minecraft:quartz": 4}, {"minecraft:quartz_block": 1}, "crafting", ["##", "##"], {"#": "minecraft:quartz"}),
    "white_wool_from_string": recipe({"minecraft:string": 4}, {"minecraft:white_wool": 1}, "crafting", ["##", "##"], {"#": "minecraft:string"}),
    "glass_bottle_from_glass": recipe({"minecraft:glass": 3}, {"minecraft:glass_bottle": 3}, "crafting", ["# #", " # "], {"#": "minecraft:glass"}),
    "bread_from_wheat": recipe({"minecraft:wheat": 3}, {"minecraft:bread": 1}, "crafting", ["###"], {"#": "minecraft:wheat"}),
    "sugar_from_sugar_cane": recipe({"minecraft:sugar_cane": 1}, {"minecraft:sugar": 1}, "crafting", ["#"], {"#": "minecraft:sugar_cane"}),
    "paper_from_sugar_cane": recipe({"minecraft:sugar_cane": 3}, {"minecraft:paper": 3}, "crafting", ["###"], {"#": "minecraft:sugar_cane"}),
    "bone_meal_from_bone": recipe({"minecraft:bone": 1}, {"minecraft:bone_meal": 3}, "crafting", ["#"], {"#": "minecraft:bone"}),
    # A honey block gives its bottles back when it is made, and takes them again when it is undone (containers).
    "honey_block_from_honey_bottle": recipe({"minecraft:honey_bottle": 4}, {"minecraft:honey_block": 1}, "crafting", ["##", "##"],
                                            {"#": "minecraft:honey_bottle"}, returns={"minecraft:glass_bottle": 4}),
    "honey_bottle_from_honey_block": recipe({"minecraft:honey_block": 1, "minecraft:glass_bottle": 4}, {"minecraft:honey_bottle": 4},
                                            "crafting"),
    # Smelting: eight items a piece of coal.
    "stone_from_cobblestone": recipe({"minecraft:cobblestone": 8, "minecraft:coal": 1}, {"minecraft:stone": 8}, "smelting"),
    "glass_from_sand": recipe({"minecraft:sand": 8, "minecraft:coal": 1}, {"minecraft:glass": 8}, "smelting"),
    # External sources: matter that enters from outside the graph, by design. No cycle runs through them.
    "cobblestone_generator": recipe({}, {"minecraft:cobblestone": 1}, "generator", kind="source",
                                    catalysts={"minecraft:water_bucket": 1, "minecraft:lava_bucket": 1}),
    "wheat_farm": recipe({"minecraft:wheat_seeds": 1}, {"minecraft:wheat": 1}, "farming", kind="source",
                         byproducts={"minecraft:wheat_seeds": 2}),
    "oak_tree": recipe({"minecraft:oak_sapling": 1}, {"minecraft:oak_log": 5}, "farming", kind="source",
                       byproducts={"minecraft:oak_sapling": 1}),
    "sugar_cane_farm": recipe({"minecraft:sugar_cane": 1}, {"minecraft:sugar_cane": 2}, "farming", kind="source"),
    "beehive": recipe({"minecraft:glass_bottle": 1}, {"minecraft:honey_bottle": 1}, "farming", kind="source"),
}

# Never weighed, whatever they carry: the Concordance's own magical things and vanilla's. (Uncatalogued items are refused
# anyway; the tag makes the refusal say why, and keeps them out of the catalogue for good.)
EXCLUDED = [
    rid("research_notes"), rid("initiate_wand"), rid("kindled_lantern"), rid("astrolabe"), rid("resonant_ring"),
    rid("wardlight"), rid("hearthstone"), rid("stormglass"), rid("owlsight_circlet"), rid("bonding_charm"), rid("porter_key"),
    rid("thornheart_blade"), rid("crimson_chalice"),
    rid("lesser_fire_focus"), rid("fire_focus"), rid("fire_bangle"), rid("pyromaniacs_hood"), rid("pyromaniacs_tunic"),
    rid("pyromaniacs_pants"), rid("pyromaniacs_shoes"), rid("pyromancers_hat"), rid("pyromancers_robes"),
    rid("pyromancers_leggings"), rid("pyromancers_boots"),
    rid("leather_belt"), rid("angelic_feather"), rid("kraken_shell"), rid("infernal_claws"), rid("angelheart_vial"),
    rid("phoenix_down"), rid("amphibian_boot"), rid("ice_breaker"),
    "minecraft:enchanted_book", "minecraft:totem_of_undying", "minecraft:nether_star", "minecraft:experience_bottle",
    "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:written_book",
    "minecraft:filled_map", "minecraft:player_head", "minecraft:spawner", "minecraft:trial_spawner", "minecraft:heart_of_the_sea",
]

ASSAY_SPECIMEN_TAG = f"{MOD}:assay_specimens"
ASSAY_SPECIMENS = {
    "minecraft:raw_iron": "Raw Iron", "minecraft:raw_copper": "Raw Copper", "minecraft:raw_gold": "Raw Gold",
    "minecraft:calcite": "Calcite", "minecraft:clay_ball": "Clay Ball",
}
ASSAY_PRACTICE = rid("assay")  # Java: Assaying.ACTIVITY

# ------------------------------------------------------------------------------------------------- the audit (Python's own)

def exact(item):
    if item == PRIMA:
        return Fraction(1)
    grains, per, _dissolve, _form = MATERIALS[item]
    return Fraction(grains, per)


def gives(entry):
    out = dict(entry["outputs"])
    for name in ("returns", "byproducts"):
        for item, count in entry.get(name, {}).items():
            out[item] = out.get(item, 0) + count
    return out


def gain(entry):
    return sum(exact(i) * c for i, c in gives(entry).items()) - sum(exact(i) * c for i, c in entry["inputs"].items())


def scale_conversions():
    """The scale's own dissolving and forming, as Java's Assay.conversions builds them."""
    out = {}
    for item, (grains, per, dissolve, form) in MATERIALS.items():
        if dissolve:
            out[f"scale/dissolve/{item}"] = {"kind": "recipe", "inputs": {item: per}, "outputs": {PRIMA: grains}}
        if form:
            cost = -(-(Fraction(grains, per) * FORM_MARKUP).numerator // (Fraction(grains, per) * FORM_MARKUP).denominator)
            out[f"scale/form/{item}"] = {"kind": "recipe", "inputs": {PRIMA: cost}, "outputs": {item: 1}}
    return out


def audit():
    """Python's own audit of the declared graph and the scale (tools/check_mod_data.py runs it): problems, as strings."""
    problems = []
    graph = dict(TRANSMUTATIONS)
    graph.update(scale_conversions())
    edges = {}
    for key, entry in graph.items():
        items = set(entry["inputs"]) | set(gives(entry))
        missing = sorted(i for i in items if i != PRIMA and i not in MATERIALS)
        if missing:
            problems.append(f"transmutation {key}: no value for {missing}")
            continue
        if entry["kind"] == "source":
            continue
        g = gain(entry)
        if g > 0:
            problems.append(f"transmutation {key} gains {g} grains")
        for i, a in entry["inputs"].items():
            for o, b in gives(entry).items():
                edges.setdefault(i, []).append((key, o, a, b, g))
    # Every simple cycle up to MAX_LENGTH steps, from its least item: profitable if it returns at least what was fed in
    # while gaining value.
    found = 0

    def walk(start, at, path, seen):
        nonlocal found
        if found >= MAX_CYCLES or len(path) >= MAX_LENGTH:
            return
        for key, to, a, b, g in edges.get(at, []):
            if to == start:
                fed, total = Fraction(1), Fraction(0)
                for step_key, _to, sa, sb, sg in path + [(key, to, a, b, g)]:
                    runs = fed / sa
                    total += runs * sg
                    fed = runs * sb
                found += 1
                if fed >= 1 and total > 0:
                    problems.append(f"profitable cycle {' > '.join(p[0] for p in path + [(key,)])}: gains {total}")
            elif to > start and to not in seen:
                walk(start, to, path + [(key, to, a, b, g)], seen | {to})

    for start in sorted(edges):
        walk(start, start, [], set())
    return problems


# ------------------------------------------------------------------------------------------------- research and text

RESEARCH = {
    "assay": {
        "name": "Assay",
        "principle": "strata",
        "tradition": "balancewrights",
        "stage": "practitioner",
        "icon": rid("assayers_scale"),
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{ASSAY_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{ASSAY_SPECIMEN_TAG}", "distinct": 2}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{ASSAY_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: five different materials dissolved.
            "mastered": [{"type": "practice", "activity": ASSAY_PRACTICE, "distinct": 5}],
        },
        "unlocks": {},
        "advancements": {
            "encountered": ("Raw Matter", "Examine an unworked ore or clay", "task", "minecraft:raw_iron"),
            "observed": ("Comparisons", "Examine two different raw materials", "task", "minecraft:raw_copper"),
            "understood": ("Assay", "Understand Assay: study raw matter at a Lampwright's Bench", "goal", rid("assayers_scale")),
            "mastered": ("Balancewright", "Dissolve five different materials at an Assayer's Scale", "challenge", "minecraft:gold_ingot"),
        },
        "locked": {"encountered": "Examine an unworked ore or clay first",
                   "observed": "Examine two different raw materials first",
                   "understood": "Understand Assay first", "mastered": "Master Assay first"},
    },
}

ITEMS = {}
BLOCKS = {"assayers_scale": {"name": "Assayer's Scale"}}
DEVICE_TOOLTIPS = {
    "assayers_scale": "Use with a material to weigh it, and again to dissolve it into grains; with an empty hand and a "
                      "material in your other hand, to form one more of it",
}

MESSAGES = {
    "assay.value": "%s: worth %s grains each; %s dissolve into %s grains; one costs %s grains to form",
    "assay.confirm": "Use the scale again within ten seconds to dissolve them",
    "assay.dissolved": "%s %s dissolved into %s grains (ledger: %s)",
    "assay.formed": "%s formed for %s grains (ledger: %s)",
    "assay.balance": "Your ledger holds %s grains of Prima Materia (at most %s)",
    "assay.refused": "%s: %s",
    "assay.unknown": "You have not understood Assay (the codex says how)",
    "assay.audit_clean": "The conversion graph is balanced: %s conversions, %s cycles searched, none profitable",
    "assay.audit_finding": "%s",
}
TOOLTIPS = {
    "assay_specimen": "Raw matter: sneak and use to examine it",
    "jade.scale": "Weighs catalogued mundane materials",
}


def lang_entries(lang):
    lang[f"tag.item.{ASSAY_SPECIMEN_TAG.replace(':', '.')}"] = "Raw Matter"
    lang[f"tag.item.{EXCLUDED_TAG.replace(':', '.')}"] = "Beyond Equivalence"
    for key, text in REASONS.items():
        lang[f"compose.{MOD}.assay.reason.{key}"] = text
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text


# ------------------------------------------------------------------------------------------------- codex

def codex():
    rows = sorted(MATERIALS.items(), key=lambda kv: (Fraction(kv[1][0], kv[1][1]), kv[0]))
    table = "\\\n".join(f"- {item.split(':')[1].replace('_', ' ')}: {Fraction(g, p)}" for item, (g, p, _d, _f) in rows)
    sources = "\\\n".join(f"- {key.replace('_', ' ')}" for key, entry in TRANSMUTATIONS.items() if entry["kind"] == "source")
    pages = [
        ("text", "Assay",
         "Once you understand First Light, examine raw matter (**sneak and use**): raw iron, copper or gold, calcite or "
         "clay. Study one at a Lampwright's Bench to understand Assay."),
        ("crafting_recipe", "Assayer's Scale",
         "Use the scale with a material in your main hand to **weigh** it: its worth in grains of Prima Materia, what "
         "the stack would dissolve into, and what forming one costs. Use it again within ten seconds to **dissolve** "
         "the stack into your ledger. With an empty main hand and a material in your other hand, use it to **form** one "
         f"more of it (the one you hold is the scale's pattern and is kept). Dissolving pays the exact worth of the whole "
         f"stack rounded down; forming costs {FORM_MARKUP.numerator}/{FORM_MARKUP.denominator} of the worth rounded up, so "
         f"every round trip loses. Your ledger holds at most {MAX_BALANCE:,} grains.", rid("assayers_scale")),
        ("text", "What Is Weighed",
         "Only catalogued, plain materials. The scale refuses anything that holds other things, is named, written or "
         "marked, carries a creature or a block's state, has been changed in any other way, or carries magic: research, "
         "spells, relics, rings, living equipment, bound spirits. Owning something, or seeing it in a recipe book, never "
         "makes it weighable."),
        ("text", "The Catalogue (grains each)", table),
        ("text", "A Balanced Graph",
         "Every recipe between catalogued materials is declared, with the containers it gives back and its byproducts, "
         "and none may be worth more after than before. The scale's own dissolving and forming are part of the graph. "
         "**/jugcraft concordance equivalence audit** (operators) searches every cycle for a profitable one. Matter enters "
         "only from declared sources:\\\n" + sources),
    ]
    return {
        ("equivalence", "assay"): {
            "name": "Assay", "x": 0, "y": 0, "icon": rid("assayers_scale"), "condition": None,
            "description": "Bounded material equivalence and the Assayer's Scale",
            "pages": pages,
        },
    }


CATEGORY = {"equivalence": {"name": "Equivalence", "icon": rid("assayers_scale"), "sort": 12,
                            "description": "Mundane matter weighed exactly, and never anything more"}}


# ------------------------------------------------------------------------------------------------- data and assets

def tags(tags):
    for ref in ASSAY_SPECIMENS:
        tags.add("item", ASSAY_SPECIMEN_TAG, ref)
    for ref in EXCLUDED:
        tags.add("item", EXCLUDED_TAG, ref)
    tags.add("block", "minecraft:mineable/axe", rid("assayers_scale"))


RECIPES = {
    "assayers_scale": {"pattern": ["GBG", " I ", "SPS"], "key": {"G": "minecraft:gold_ingot", "B": "minecraft:copper_ingot",
                                                                 "I": "minecraft:iron_ingot", "S": "minecraft:smooth_stone_slab",
                                                                 "P": "minecraft:dark_oak_planks"}},
}


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    import concordance_equivalence_models as models
    write(assets / "blockstates" / "assayers_scale.json", {"variants": {
        f"facing={facing}": {"model": rid("block/assayers_scale"), **({"y": rotation} if rotation else {})}
        for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(assets / "models" / "block" / "assayers_scale.json", models.scale_model())
    write(assets / "items" / "assayers_scale.json", {"model": {"type": "minecraft:model", "model": rid("item/assayers_scale")}})
    write(assets / "models" / "item" / "assayers_scale.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/assayers_scale")}})
    write(data / "loot_table" / "blocks" / "assayers_scale.json", self_drop("assayers_scale"))
    for name, entry in RECIPES.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition("concordance")}, **{
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": entry["pattern"], "key": entry["key"],
            "result": {"id": rid(name), "count": 1}}))


def write_data(write, data):
    for item, (grains, per, dissolve, form) in MATERIALS.items():
        write(data / "concordance" / "material" / f"{item.split(':')[1]}.json",
              {"schema": 1, "item": item, "value": value(grains, per), "dissolve": dissolve, "form": form})
    for key, entry in TRANSMUTATIONS.items():
        write(data / "concordance" / "transmutation" / f"{key}.json", dict({"schema": 1}, **entry))
