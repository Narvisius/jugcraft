"""Roadmap step 24: the complete progression graph (docs/features/arcane-concordance-progression.md).

The Concordance's five stages (Initiate, Practitioner, Adept, Master, Architect) are data
(data/jugcraft/concordance/stage), each reached by any one of its ROUTES once the stage before it is. The canonical
progression graph is not written beside the rules: Java's progression/ProgressionGraph builds it from them (research
and its evidence, invocations, rituals, the practice gates here, the Conclave's ranks, the stages), and so does audit()
below, independently, over the generator's own tables. Both answer the same questions: can one player alone reach every
research state and every stage from a fresh world; does any step need something only a later stage provides; does a
cycle block anything; does each middle stage have at least two routes of its own.

What the graph leaves to things at hand (specimens, stations, offerings, the devices a practice needs) is checked by
availability(): every specimen tag holds something craftable or renewable, every station, offering and device can be
made from obtainable things (the generated recipes, recursively), and every vanilla item a required step needs is in
VANILLA_SOURCES with how it is obtained and, for finite world materials, a recovery route.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# Java: progression/ProgressionParser.java.
MAX_ROUTES = 8
MAX_COUNT = 32
RESEARCH_STATES = ["encountered", "observed", "understood", "mastered"]

# Each stage: its order, its Jugcraft stage (docs/DESIGN.md), and its routes (any one, once the stage before is reached).
STAGES = {
    "initiate": {"order": 1, "name": "Initiate", "jugcraft": "Discovery", "routes": [
        {"id": "first_light", "research": [f"{MOD}:first_light@encountered"]}]},
    "practitioner": {"order": 2, "name": "Practitioner", "jugcraft": "Workshops", "routes": [
        {"id": "first_light", "research": [f"{MOD}:first_light@understood"]}]},
    "adept": {"order": 3, "name": "Adept", "jugcraft": "Specialization", "routes": [
        {"id": "specialist", "mastered": 2},
        {"id": "generalist", "understood": 5, "traditions": 5},
        {"id": "attuned", "research": [f"{MOD}:circle_lore@understood", f"{MOD}:first_light@mastered"]}]},
    "master": {"order": 4, "name": "Master", "jugcraft": "Expeditions", "routes": [
        {"id": "specialist", "mastered": 5, "traditions": 3},
        {"id": "generalist", "understood": 9, "traditions": 7, "research": [f"{MOD}:first_light@mastered"]},
        {"id": "fellowship", "rank": "luminary", "mastered": 2}]},
    "architect": {"order": 5, "name": "Architect", "jugcraft": "Shared wonders", "routes": [
        {"id": "shared_wonder", "rank": "starbound", "projects": 1, "mastered": 6},
        {"id": "raised_spire", "milestones": [f"{MOD}:spire_raised"]}]},
}

ROUTE_TEXT = {
    ("initiate", "first_light"): "examine something that holds its own light",
    ("practitioner", "first_light"): "understand First Light",
    ("adept", "specialist"): "master two research entries",
    ("adept", "generalist"): "understand five entries in five traditions",
    ("adept", "attuned"): "understand Circle Lore and master First Light (the Adept's Attunement)",
    ("master", "specialist"): "master five entries in three traditions",
    ("master", "generalist"): "understand nine entries in seven traditions and master First Light",
    ("master", "fellowship"): "be a Luminary of the Starbound Conclave and master two entries",
    ("architect", "shared_wonder"): "be Starbound, help finish a Conclave project and master six entries",
    ("architect", "raised_spire"): "raise a Concord Spire, or help raise one",
}

# What makes each practice possible: its tradition, the research it needs understood (the Java gate: the class that
# records the practice checks the same entry), the devices (any one) it needs, and the encounters it waits on.
PRACTICES = {
    "alchemy": ("alembists", "alembic_arts", ["crucible"], []),
    "artifice": ("runesmiths", "runesmithing", ["artificer_bench"], []),
    "assay": ("balancewrights", "assay", ["assayers_scale"], []),
    "cultivation": ("greenwardens", "verdant_husbandry", ["verdant_bed"], ["days"]),
    "dream": ("dreamwalkers", "dreamwalking", ["oneiric_censer"], ["night"]),
    # Kindling hearths with Hearthspark (the graph asks for the invocation and a hearth: see graph()).
    "hearthkeeping": ("hearthbinders", "hearthbinding", [], []),
    "living_growth": ("crimson_vigil", "crimson_rites", ["thornheart_blade"], ["creatures", "days"]),
    "observation": ("starwatchers", "celestial_attunement", ["observatory"], ["night", "open_sky"]),
    "relic_pulse": ("runesmiths", "relic_lore", ["wardlight", "hearthstone", "stormglass", "owlsight_circlet"], []),
    "ritual": ("circlewrights", "circle_lore", ["circle_anchor"], []),
    "sympathy": ("hexweavers", "sympathy", ["taglock"], ["creatures"]),
    "worker_service": ("spiritbinders", "binding_arts", ["bonding_charm", "spirit_anchor", "porter_key"], ["days"]),
}

# Milestones features record (roadmap step 25), each with what it needs (its gate: research, and a stage reached first)
# and the wonder whose raising records it; the graph follows the wonder's configurations down to things at hand.
MILESTONES = {
    "spire_raised": {"stage": "master", "requires": [{"research": rid("circle_lore"), "state": "understood"}],
                     "wonder": "concord_spire", "name": "a Concord Spire raised, or helped to raise"},
}

# What the world gives by itself, and how a player who lacks it gets it anyway.
ENCOUNTERS = {
    "night": ("night falls every day", "wait for it: every day has one"),
    "days": ("days of play (bonds, growth and harvests take days)", "time passes for everyone"),
    "open_sky": ("a view of the open sky", "build above the canopy, anywhere in the Overworld"),
    "creatures": ("living creatures nearby", "animals breed and monsters spawn in the dark, everywhere"),
}

# Jugcraft things made by something other than a recipe (or a bench working, which the graph reads itself): the node
# that makes them, and how.
PRODUCERS = {
    rid("adept_wand"): (f"ritual:{rid('adept_attunement')}", "the Adept's Attunement ritual transforms an Initiate's Wand"),
    rid("dreamglass"): (f"practice:{rid('dream')}", "dream wisps caught in a dream"),
}

# Where Ley Charge comes from (rituals draw it from their pylons; a pylon is filled by pouring a Kindled Lantern's
# Radiance in, or by a Verdant Heart's Verdance beside it): any one of these.
LEY_SOURCES = [[f"item:{rid('kindled_lantern')}"],
               [f"item:{rid('verdant_heart')}", f"research:{rid('verdant_husbandry')}@understood"]]

# Every vanilla item a required step can need: (how it is had, how, recovery, dimension). How it is had: "craftable" (a
# vanilla recipe from other listed things), "renewable" (farmed, bred, dropped by creatures that keep spawning, traded or
# grown), "gathered" (a finite world material: its recovery says how to have it once the world's supply is gone, used or
# out of reach) or "found" (structure loot only: never relied on, and its recovery says what serves instead). The
# dimension is where it is had ("" for the Overworld or anywhere).
VANILLA_SOURCES = {
    # Specimens: luminous.
    "minecraft:amethyst_shard": ("renewable", "budding amethyst regrows clusters",
                                 "the Conclave's First Watch pays amethyst; wandering traders sell budding-free clusters", ""),
    "minecraft:glowstone_dust": ("renewable", "witches drop it; clerics trade it", "", ""),
    "minecraft:glow_ink_sac": ("renewable", "glow squid", "", ""),
    "minecraft:glow_lichen": ("renewable", "spreads with bone meal", "", ""),
    "minecraft:glow_berries": ("renewable", "cave vines; wandering traders", "", ""),
    # Specimens: circles, celestial.
    "minecraft:compass": ("craftable", "iron and redstone", "", ""),
    "minecraft:lead": ("craftable", "string and slime; wandering traders", "", ""),
    "minecraft:slime_ball": ("renewable", "slimes in swamps and slime chunks", "", ""),
    "minecraft:calcite": ("gathered", "geodes and mountain veins", "a compass or a lead serves instead", ""),
    "minecraft:chiseled_stone_bricks": ("craftable", "stone bricks", "", ""),
    "minecraft:spyglass": ("craftable", "copper and amethyst", "", ""),
    "minecraft:clock": ("craftable", "gold and redstone", "", ""),
    "minecraft:phantom_membrane": ("renewable", "phantoms find anyone who goes without sleep", "", ""),
    # Specimens: alchemy, crimson, verdant.
    "minecraft:sugar": ("craftable", "sugar cane", "", ""),
    "minecraft:sugar_cane": ("renewable", "grows by water", "", ""),
    "minecraft:sweet_berries": ("renewable", "berry bushes", "", ""),
    "minecraft:dried_kelp": ("craftable", "smelted kelp", "", ""),
    "minecraft:spider_eye": ("renewable", "spiders; witches", "", ""),
    "minecraft:bone_meal": ("craftable", "bones", "", ""),
    "minecraft:honeycomb": ("renewable", "bee nests, sheared", "", ""),
    "minecraft:crimson_roots": ("renewable", "crimson nylium with bone meal", "", "nether"),
    "minecraft:crimson_fungus": ("renewable", "crimson nylium with bone meal", "", "nether"),
    "minecraft:nether_wart": ("renewable", "farmed on soul sand", "", "nether"),
    "minecraft:moss_block": ("renewable", "spreads with bone meal; wandering traders", "", ""),
    "minecraft:sunflower": ("renewable", "bone meal on a sunflower", "", ""),
    "minecraft:brown_mushroom": ("renewable", "spreads in the dark", "", ""),
    "minecraft:lily_pad": ("renewable", "fishing; swamps", "", ""),
    "minecraft:fern": ("renewable", "bone meal on grass", "", ""),
    # Specimens: binding, artifice, assay.
    "minecraft:name_tag": ("craftable", "paper and an iron nugget", "", ""),
    "minecraft:soul_lantern": ("craftable", "iron nuggets and a soul torch", "", "nether"),
    "minecraft:saddle": ("craftable", "leather and iron", "", ""),
    "minecraft:echo_shard": ("found", "ancient cities", "a lead serves instead", ""),
    "minecraft:flint": ("renewable", "gravel, which mobs and traders renew", "", ""),
    "minecraft:gold_nugget": ("renewable", "zombified piglins; formed at the Assayer's Scale", "", ""),
    "minecraft:iron_nugget": ("craftable", "iron ingots; iron golems", "", ""),
    "minecraft:lapis_lazuli": ("renewable", "clerics trade it", "", ""),
    "minecraft:quartz": ("renewable", "piglins barter it", "", "nether"),
    "minecraft:raw_iron": ("gathered", "ore", "clay serves instead", ""),
    "minecraft:raw_copper": ("gathered", "ore", "clay serves instead", ""),
    "minecraft:raw_gold": ("gathered", "ore", "clay serves instead", ""),
    "minecraft:clay_ball": ("renewable", "mud dripping under pointed dripstone becomes clay", "", ""),
    # Specimens: dreams, relics, sympathy.
    "minecraft:chorus_fruit": ("renewable", "chorus plants regrow", "", "end"),
    "minecraft:ender_pearl": ("renewable", "endermen; clerics trade them", "", ""),
    "minecraft:white_bed": ("craftable", "wool and planks", "", ""),
    "minecraft:spore_blossom": ("found", "lush caves", "a white bed serves instead", ""),
    "minecraft:heart_of_the_sea": ("found", "buried treasure", "an eye of ender serves instead", ""),
    "minecraft:nautilus_shell": ("renewable", "drowned and fishing", "", ""),
    "minecraft:totem_of_undying": ("renewable", "evokers in raids", "", ""),
    "minecraft:ender_eye": ("craftable", "ender pearl and blaze powder", "", ""),
    "minecraft:recovery_compass": ("craftable", "echo shards and a compass", "", ""),
    "minecraft:fermented_spider_eye": ("craftable", "spider eye, sugar and a brown mushroom", "", ""),
    "minecraft:cobweb": ("renewable", "the weaving effect", "", ""),
    "minecraft:poisonous_potato": ("renewable", "potato harvests", "", ""),
    "minecraft:rabbit_foot": ("renewable", "rabbits", "", ""),
    "minecraft:armor_stand": ("craftable", "sticks and a smooth stone slab", "", ""),
    # Offerings and common ingredients.
    "minecraft:gold_ingot": ("gathered", "ore", "zombified piglins' nuggets; formed at the Assayer's Scale", ""),
    "minecraft:iron_ingot": ("gathered", "ore", "iron golems; formed at the Assayer's Scale", ""),
    "minecraft:copper_ingot": ("gathered", "ore", "drowned drop copper; formed at the Assayer's Scale", ""),
    "minecraft:redstone": ("renewable", "witches drop it; clerics trade it", "", ""),
    "minecraft:string": ("renewable", "spiders; cats bring it", "", ""),
    "minecraft:stick": ("craftable", "planks", "", ""),
    "minecraft:dark_oak_planks": ("craftable", "dark oak logs, from trees that regrow", "", ""),
    "minecraft:dark_oak_slab": ("craftable", "dark oak planks", "", ""),
    "minecraft:dark_oak_log": ("renewable", "dark oak trees, grown from saplings", "", ""),
    "minecraft:dirt": ("renewable", "rooted dirt under azalea trees; coarse dirt tilled", "", ""),
    "minecraft:glass_bottle": ("craftable", "glass, from sand", "", ""),
    "minecraft:glass": ("craftable", "sand smelted", "", ""),
    "minecraft:glass_pane": ("craftable", "glass", "", ""),
    "minecraft:bricks": ("craftable", "clay fired into bricks", "", ""),
    "minecraft:cauldron": ("craftable", "iron", "", ""),
    "minecraft:iron_sword": ("craftable", "iron and a stick", "", ""),
    "minecraft:lantern": ("craftable", "iron nuggets and a torch", "", ""),
    "minecraft:smithing_table": ("craftable", "iron and planks", "", ""),
    "minecraft:smooth_stone": ("craftable", "stone smelted", "", ""),
    "minecraft:smooth_stone_slab": ("craftable", "smooth stone", "", ""),
    "minecraft:book": ("craftable", "paper and leather", "", ""),
    "minecraft:paper": ("craftable", "sugar cane", "", ""),
    "minecraft:feather": ("renewable", "chickens", "", ""),
    "minecraft:ink_sac": ("renewable", "squid", "", ""),
    "minecraft:oak_planks": ("craftable", "oak logs, from trees that regrow", "", ""),
    "minecraft:experience_bottle": ("renewable", "clerics trade them", "", ""),
    "minecraft:emerald": ("renewable", "villagers trade them", "", ""),
    "minecraft:diamond": ("gathered", "ore", "toolsmiths, weaponsmiths and armorers trade diamond gear; the Conclave pays them", ""),
    "minecraft:wheat_seeds": ("renewable", "grass, broken; wheat harvests", "", ""),
    "minecraft:stone_bricks": ("craftable", "stone", "", ""),
    "minecraft:polished_deepslate": ("craftable", "cobbled deepslate, plentiful below the surface", "", ""),
    "minecraft:lightning_rod": ("craftable", "copper ingots", "", ""),
    # The Concord Spire's shaft and crowns (roadmap step 25).
    "minecraft:deepslate_bricks": ("craftable", "polished deepslate", "", ""),
    "minecraft:deepslate_tiles": ("craftable", "deepslate bricks", "", ""),
    "minecraft:chiseled_deepslate": ("craftable", "cobbled deepslate slabs", "", ""),
    "minecraft:glowstone": ("craftable", "glowstone dust", "", ""),
    "minecraft:amethyst_block": ("craftable", "amethyst shards", "", ""),
    "minecraft:flowering_azalea": ("renewable", "bone meal on a moss block grows them", "", ""),
    "minecraft:blaze_powder": ("renewable", "blazes, which keep spawning in nether fortresses", "", "nether"),
    # Specimens: Hearthbinding's (tools/concordance_ember.py), and the hearths it kindles.
    "minecraft:coal": ("gathered", "coal ore, plentiful near the surface", "charcoal, from any log, serves instead", ""),
    "minecraft:charcoal": ("craftable", "any log smelted", "", ""),
    "minecraft:torch": ("craftable", "a stick and coal or charcoal", "", ""),
    "minecraft:campfire": ("craftable", "sticks, logs and coal or charcoal", "", ""),
    "minecraft:magma_block": ("gathered", "the ocean floor and the Nether", "four magma cream make one", ""),
    "minecraft:blaze_rod": ("renewable", "blazes, which keep spawning in nether fortresses", "", "nether"),
    "minecraft:magma_cream": ("renewable", "magma cubes; or blaze powder and a slime ball", "", "nether"),
    "minecraft:fire_charge": ("craftable", "blaze powder, coal and gunpowder", "", "nether"),
    "minecraft:prismarine_shard": ("renewable", "guardians, which keep spawning around ocean monuments", "", ""),
    # Wayfaring's belt, boots and charms (tools/concordance_trinkets.py).
    "minecraft:leather": ("renewable", "cows, horses and llamas; rabbit hide", "", ""),
    "minecraft:golden_apple": ("craftable", "an apple and gold ingots", "", ""),
}

# Jugcraft things the world grows rather than a recipe makes, by the same rules as VANILLA_SOURCES.
JUGCRAFT_SOURCES = {
    rid("glowcap"): ("gathered", "grottoes, some biomes' surface and the Nether",
                     "glowstone dust, glow berries or glow lichen serve instead", ""),
    rid("glimmerbloom"): ("gathered", "frozen gardens and twilight groves", "glowstone dust or glow berries serve instead", ""),
    rid("jack_o_lantern_mushroom"): ("renewable", "spreads from its patch like other wild mushrooms", "", ""),
}

# Tags from Minecraft or the common namespace that recipes use: (how it is had, what it holds). Jugcraft's own tags are
# read from the data.
VANILLA_TAGS = {
    "minecraft:planks": ("craftable", "any planks, from logs"),
    "minecraft:logs": ("renewable", "any logs, from trees that regrow"),
    "minecraft:wooden_slabs": ("craftable", "any wooden slab, from planks"),
    "minecraft:wool": ("renewable", "sheep, sheared"),
    "minecraft:sand": ("gathered", "deserts and beaches; husks and wandering traders renew it"),
    "minecraft:candles": ("craftable", "string and honeycomb"),
    "minecraft:coals": ("renewable", "charcoal from logs"),
    "minecraft:leaves": ("renewable", "trees"),
    "minecraft:eggs": ("renewable", "chickens"),
    "c:ingots/iron": ("gathered", "iron ingots (minecraft:iron_ingot)"),
    "c:ingots/copper": ("gathered", "copper ingots (minecraft:copper_ingot)"),
    "c:ingots/gold": ("gathered", "gold ingots (minecraft:gold_ingot)"),
}

RECIPE_TYPES = ("minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smelting", "minecraft:blasting",
                "minecraft:smoking", "minecraft:campfire_cooking", "minecraft:stonecutting", "minecraft:smithing_transform")


def stage_json(info):
    return {"schema": 1, "order": info["order"], "routes": info["routes"]}


def practice_json(key):
    tradition, research, _devices, _encounters = PRACTICES[key]
    return {"schema": 1, "tradition": tradition, "requires": [{"research": rid(research), "state": "understood"}]}


def milestone_json(key):
    info = MILESTONES[key]
    return {"schema": 1, "requires": info["requires"], "stage": info["stage"]}


# ------------------------------------------------------------------------------------------------------- the graph

def node(stage, all_of=(), any_of=(), social=False, kind="", at_least=1, luck=False, where=""):
    """A node: what it needs (all), groups of which at_least must wholly hold (any), whether it needs another player
    (social) or luck (found only in structures), and the dimension it is had in."""
    return {"stage": stage, "all": list(all_of), "any": [list(group) for group in any_of], "social": social, "kind": kind,
            "at_least": at_least, "luck": luck, "where": where}


def research_node(key, state):
    return f"research:{key}@{state}"


def _rules(block):
    return block if isinstance(block, list) else block.get("any", [])


def load_at_hand(data):
    """What the graph needs to follow things at hand, read from a data folder (src/main/resources/data): the recipes
    (crafting, cooking, cutting and smithing) as {result: [[ingredient, ...], ...]} where an ingredient is an item, a
    #tag or a list of alternatives; Jugcraft's item and block tags; the ritual structures."""
    import json
    recipes = {}
    for path in sorted((data / MOD / "recipe").glob("*.json")):
        recipe = json.loads(path.read_text(encoding="utf-8"))
        if recipe.get("type") not in RECIPE_TYPES:
            continue
        result = recipe.get("result")
        result = result.get("id") if isinstance(result, dict) else result
        if not isinstance(result, str):
            continue
        if "key" in recipe:
            ingredients = list(recipe["key"].values())
        elif "ingredients" in recipe:
            ingredients = list(recipe["ingredients"])
        else:
            ingredients = [recipe[key] for key in ("ingredient", "template", "base", "addition") if key in recipe]
        recipes.setdefault(result, []).append(ingredients)
    tags = {}
    for kind in ("item", "block"):
        folder = data / MOD / "tags" / kind
        for path in sorted(folder.rglob("*.json")):
            tag = f"{MOD}:" + path.relative_to(folder).with_suffix("").as_posix()
            tags[(kind, tag)] = json.loads(path.read_text(encoding="utf-8")).get("values", [])
    def folder(name):
        return {path.stem: json.loads(path.read_text(encoding="utf-8"))
                for path in sorted((data / MOD / "concordance" / name).glob("*.json"))}
    return {"recipes": recipes, "tags": tags, "structures": folder("structure"), "commissions": folder("commission"),
            "projects": folder("project"), "workings": folder("working"), "wonders": folder("wonder"),
            "wonder_configurations": folder("wonder_configuration")}


class _Items:
    """Adds item, tag and structure nodes to a graph as they are needed: a vanilla item by VANILLA_SOURCES, a Jugcraft
    item by any of its recipes or its producer, a tag by any of its members."""

    def __init__(self, nodes, at_hand, problems):
        self.nodes, self.at_hand, self.problems = nodes, at_hand, problems

    def item(self, item):
        key = f"item:{item}"
        if key in self.nodes:
            return key
        if item.startswith("minecraft:") or item in JUGCRAFT_SOURCES:
            source = VANILLA_SOURCES.get(item) or JUGCRAFT_SOURCES.get(item)
            if source is None:
                self.problems.append(f"{item} is needed by a step but is not in VANILLA_SOURCES")
                self.nodes[key] = node("", ["unlisted:" + item], kind="item")
            else:
                self.nodes[key] = node("", kind="item", luck=source[0] == "found", where=source[3])
            return key
        self.nodes[key] = node("", kind="item")
        options = []
        for ingredients in self.at_hand["recipes"].get(item, []):
            options.append([self.ingredient(ingredient) for ingredient in ingredients])
        if item in PRODUCERS:
            options.append([PRODUCERS[item][0]])
        for name, working in self.at_hand.get("workings", {}).items():
            if working.get("type") == "craft" and working.get("result") == item:
                options.append([self.working(name, working)])
        if not options:
            self.problems.append(f"{item} is needed by a step but nothing makes it (no recipe, no producer)")
            self.nodes[key]["all"] = ["unmade:" + item]
        self.nodes[key]["any"] = options
        return key

    def working(self, key, working):
        """A bench working that makes something: its research, its station, the work and the specimen it takes."""
        node_id = f"working:{rid(key)}"
        if node_id not in self.nodes:
            self.nodes[node_id] = node("", kind="working")
            self.nodes[node_id]["all"] = [research_node(working["research"], working["stage"]), self.item(working["station"]),
                                          self.item(working["work"]), self.item(working["specimen"])]
        return node_id

    def ley(self):
        """Ley Charge to draw: from a Kindled Lantern poured into a pylon, or a Verdant Heart beside one (LEY_SOURCES)."""
        if "resource:ley_charge" not in self.nodes:
            self.nodes["resource:ley_charge"] = node("", kind="resource")
            self.nodes["resource:ley_charge"]["any"] = [
                [self.item(need[len("item:"):]) if need.startswith("item:") else need for need in source] for source in LEY_SOURCES]
        return "resource:ley_charge"

    def ingredient(self, ingredient):
        if isinstance(ingredient, list):
            key = "anyof:" + "|".join(sorted(map(str, ingredient)))
            if key not in self.nodes:
                self.nodes[key] = node("", kind="anyof")
                self.nodes[key]["any"] = [[self.ingredient(choice)] for choice in ingredient]
            return key
        if ingredient.startswith("#"):
            return self.tag(ingredient[1:], "item")
        return self.item(ingredient)

    def tag(self, tag, kind, at_least=1):
        key = f"tag:{tag}" + (f"*{at_least}" if at_least > 1 else "")
        if key in self.nodes:
            return key
        if not tag.startswith(f"{MOD}:"):
            source = VANILLA_TAGS.get(tag)
            if source is None:
                self.problems.append(f"#{tag} is needed by a step but is not in VANILLA_TAGS")
                self.nodes[key] = node("", ["unlisted:#" + tag], kind="tag")
            else:
                self.nodes[key] = node("", kind="tag")
            return key
        members = self.at_hand["tags"].get((kind, tag))
        self.nodes[key] = node("", kind="tag", at_least=at_least)
        if members is None:
            self.problems.append(f"#{tag} ({kind} tag) is needed by a step but does not exist")
            self.nodes[key]["all"] = ["missing:#" + tag]
            return key
        self.nodes[key]["any"] = [[self.ingredient(member)] for member in members]
        return key

    def specimens(self, specimens, distinct=1):
        if specimens.startswith("#"):
            return self.tag(specimens[1:], "item", max(1, distinct))
        return self.item(specimens)

    def structure(self, structure):
        key = f"structure:{structure}"
        if key in self.nodes:
            return key
        definition = self.at_hand["structures"].get(structure.split(":")[1])
        self.nodes[key] = node("", kind="structure")
        if definition is None:
            self.problems.append(f"{structure} is needed by a ritual but is not a structure")
            self.nodes[key]["all"] = ["missing:" + structure]
            return key
        needs = [self.item(definition["anchor"])]
        for part in definition.get("parts", []):
            block = part.get("block")  # a clearance part asks for air, not a block
            if block:
                needs.append(self.tag(block[1:], "block") if block.startswith("#") else self.item(block))
        self.nodes[key]["all"] = needs
        return key


def graph(research, invocations, rituals, at_hand=None, problems=None):
    """The progression graph over the data (Java: ProgressionGraph.build, which has no items). research: {path:
    definition}; invocations: {path: definition with research and stage}; rituals: {path: definition with research,
    stage, participants, structure and offerings}. With at_hand (load_at_hand), the graph also follows things at hand:
    specimens, stations, instruments, structures, offerings, devices and encounters, down to what the world gives.
    Returns {node id: node}."""
    problems = [] if problems is None else problems
    nodes = {"social:notes": node("", social=True, kind="social")}
    items = _Items(nodes, at_hand, problems) if at_hand is not None else None
    for key, info in research.items():
        entry = rid(key)
        previous = None
        for state in RESEARCH_STATES:
            rules = info["states"].get(state)
            if rules is None:
                continue
            if previous is None:
                needs = [research_node(r["research"], r["state"]) for r in info.get("requires", [])]
            else:
                needs = [previous]
            alternatives = []
            for rule in _rules(rules):
                kind = rule["type"]
                if kind in ("examine", "study"):
                    group = []
                    if items is not None:
                        group.append(items.specimens(rule["specimens"], rule.get("distinct", 1) if kind == "examine" else 1))
                        if kind == "study":
                            group.append(items.item(rule["station"]))
                    alternatives.append(group)
                elif kind == "invoke":
                    alternatives.append([f"invocation:{rule['invocation']}"])
                elif kind == "practice":
                    alternatives.append([f"practice:{rule['activity']}"])
                elif kind == "notes":
                    alternatives.append(["social:notes"])
            nodes[research_node(entry, state)] = node(info["stage"], needs, alternatives, kind="research")
            previous = research_node(entry, state)
    instruments = items.tag(f"{MOD}:concordance_instruments", "item") if items is not None else None
    for key, info in invocations.items():
        teacher = research.get(info["research"].split(":")[1], {})
        needs = [research_node(info["research"], info["stage"])] + ([instruments] if instruments else [])
        nodes[f"invocation:{rid(key)}"] = node(teacher.get("stage", ""), needs, kind="invocation")
    for key, info in rituals.items():
        teacher = research.get(info["research"].split(":")[1], {})
        needs = [research_node(info["research"], info["stage"])]
        if items is not None:
            needs.append(items.structure(info["structure"]))
            needs += [items.item(offering["item"]) for offering in info["offerings"]]
            if info.get("ley", 0) > 0:
                needs.append(items.ley())
        nodes[f"ritual:{rid(key)}"] = node(teacher.get("stage", ""), needs, social=info["participants"] > 1, kind="ritual")
    for key, (_tradition, gate, devices, encounters) in PRACTICES.items():
        needs = [research_node(rid(gate), "understood")]
        alternatives = []
        if items is not None:
            alternatives = [[items.item(rid(device))] for device in devices]
            if key == "ritual":
                # A ritual is the practice; each ritual already needs its circle (anchor included) and offerings.
                alternatives = [[f"ritual:{rid(ritual)}"] for ritual in rituals]
            elif key == "hearthkeeping":
                # Hearthspark kindles the hearths: campfires and candles, both made from what the world renews.
                spark = f"invocation:{rid('hearthspark')}"
                alternatives = [[spark, items.item("minecraft:campfire")], [spark, items.tag("minecraft:candles", "item")]]
            for encounter in encounters:
                nodes.setdefault(f"encounter:{encounter}", node("", kind="encounter"))
                needs.append(f"encounter:{encounter}")
        nodes[f"practice:{rid(key)}"] = node(research.get(gate, {}).get("stage", ""), needs, alternatives, kind="practice")
    for key, info in MILESTONES.items():
        # A milestone: its gate (research, and its stage reached first) and, at hand, any one of its wonder's
        # configurations raised: their research, practice, the rite, every structure, the heart, the upkeep and Ley.
        needs = [research_node(r["research"], r["state"]) for r in info["requires"]] + [f"stage:{info['stage']}"]
        alternatives = []
        if items is not None:
            wonder = at_hand["wonders"].get(info["wonder"], {})
            for configuration in at_hand["wonder_configurations"].values():
                if configuration.get("wonder") != rid(info["wonder"]):
                    continue
                group = [research_node(r["research"], r["state"]) for r in configuration["requires"]]
                group += [f"practice:{configuration['practice']}", f"ritual:{wonder['rite']}", items.item(wonder["heart"]),
                          items.item(configuration["upkeep"]["item"]), items.ley()]
                for phase in wonder["phases"]:
                    structure = phase.get("structure")
                    if structure:
                        group.append(items.structure(configuration["crown"] if structure == "crown" else structure))
                alternatives.append(group)
        nodes[f"practice:{rid(key)}"] = node(info["stage"], needs, alternatives, kind="practice")
    import concordance_conclave as conclave
    for rank in conclave.RANKS:
        nodes[f"rank:{rank[0]}"] = node("", [research_node(rid("first_light"), "understood")], kind="rank")
    nodes["project:conclave"] = node("", ["rank:fellow"], kind="project")
    if items is not None:
        # The Conclave's work, so that what a rank's renown and a project ask for can be had (tools/concordance_conclave.py
        # solo_route shows that one player's renown reaches every rank).
        tier_rank = {1: "rank:aspirant", 2: "rank:fellow", 3: "rank:companion"}
        for key, info in at_hand["commissions"].items():
            needs = [tier_rank.get(info.get("tier"), "rank:aspirant")]
            needs.append(items.item(info["deliver"]) if "deliver" in info else f"practice:{info.get('practice')}")
            nodes[f"commission:{rid(key)}"] = node("", needs, kind="commission")
        projects = []
        for key, info in at_hand["projects"].items():
            needs = ["rank:fellow"]
            for stage in info["stages"]:
                for requirement in stage["requirements"]:
                    if "deliver" in requirement:
                        needs.append(items.item(requirement["deliver"]))
                    elif "research" in requirement:
                        needs.append(research_node(requirement["research"], "understood"))
                    else:
                        needs.append(f"practice:{requirement['practice']}")
            nodes[f"project:{rid(key)}"] = node("", needs, kind="project")
            projects.append([f"project:{rid(key)}"])
        nodes["project:conclave"]["any"] = projects
    return nodes


def _counted(reached, research, least, need, traditions_needed):
    entries, across = set(), set()
    floor = RESEARCH_STATES.index(least)
    for item in reached:
        if not item.startswith("research:"):
            continue
        entry, state = item[len("research:"):].split("@")
        if RESEARCH_STATES.index(state) >= floor and entry not in entries:
            entries.add(entry)
            across.add(research.get(entry.split(":")[1], {}).get("tradition", ""))
    return len(entries) >= need and len(across) >= traditions_needed


def _route_holds(route, reached, research):
    if any(f"research:{entry}" not in reached for entry in route.get("research", [])):
        return False
    mastered = route.get("mastered", 0)
    if mastered and not _counted(reached, research, "mastered", mastered, route.get("traditions", 0)):
        return False
    understood = route.get("understood", 0)
    if understood and not _counted(reached, research, "understood", understood, 0 if mastered else route.get("traditions", 0)):
        return False
    if route.get("rank") and f"rank:{route['rank']}" not in reached:
        return False
    if route.get("projects", 0) and "project:conclave" not in reached:
        return False
    return all(f"practice:{milestone}" in reached for milestone in route.get("milestones", []))


def _order(stage):
    return STAGES[stage]["order"] if stage in STAGES else 0


def reachable(nodes, research, alone=True, only_route=None, without=(), cap=None, luck=False, dimensions=None):
    """Every node one player (alone, or with others) reaches from a fresh world: a fixed point (Java: reachable). Also:
    without some nodes (what if they were gone?), with nothing past the stage cap, with structure loot (luck), or only
    in some dimensions."""
    reached = set()
    ordered = sorted(STAGES, key=lambda key: STAGES[key]["order"])
    limit = _order(cap) if cap else None
    changed = True
    while changed:
        changed = False
        for key, info in nodes.items():
            if key in reached or key in without or (alone and info["social"]) or (info["luck"] and not luck):
                continue
            if dimensions is not None and info["where"] and info["where"] not in dimensions:
                continue
            if limit is not None and _order(info["stage"]) > limit:
                continue
            if not all(need in reached for need in info["all"]):
                continue
            if info["any"] and sum(1 for group in info["any"] if all(need in reached for need in group)) < info["at_least"]:
                continue
            reached.add(key)
            changed = True
        for index, stage in enumerate(ordered):
            if f"stage:{stage}" in reached or f"stage:{stage}" in without or (index and f"stage:{ordered[index - 1]}" not in reached):
                continue
            if limit is not None and STAGES[stage]["order"] > limit:
                continue
            for route in STAGES[stage]["routes"]:
                if only_route and stage in only_route and only_route[stage] != route["id"]:
                    continue
                if _route_holds(route, reached, research):
                    reached.add(f"stage:{stage}")
                    changed = True
                    break
    return reached


def _cycles(nodes, reached):
    """Cycles among unreachable nodes: the ones that block progress (Java: blockingCycles)."""
    edges = {key: [need for need in info["all"] + [n for group in info["any"] for n in group]
                   if need in nodes and need not in reached]
             for key, info in nodes.items() if key not in reached}
    cycles, done = [], set()
    for start in sorted(edges):
        if start in done:
            continue
        path, on_path, seen = [], set(), set()

        def walk(at):
            path.append(at)
            on_path.add(at)
            seen.add(at)
            for following in edges.get(at, []):
                if following in on_path:
                    return path[path.index(following):] + [following]
                if following not in seen:
                    found = walk(following)
                    if found:
                        return found
            path.pop()
            on_path.discard(at)
            return None

        cycle = walk(start)
        if cycle:
            cycles.append(cycle)
            done.update(cycle)
        done.add(start)
    return cycles


def audit(research, invocations, rituals, at_hand=None):
    """What is wrong with the progression graph (Java: ProgressionGraph's problems, which has no items): an empty list
    when one player alone, from a fresh world, without structure loot, reaches every research state and every stage;
    no step needs a node or item that only a later stage provides; no cycle blocks anything; each middle stage has two
    routes of its own; and every finite world material a step relies on has a recovery route."""
    problems = []
    nodes = graph(research, invocations, rituals, at_hand, problems)
    for key, info in nodes.items():
        for need in info["all"] + [n for group in info["any"] for n in group]:
            if need not in nodes and not need.startswith(("unlisted:", "unmade:", "missing:")) \
                    and not (need.startswith("stage:") and need[len("stage:"):] in STAGES):
                problems.append(f"{key} needs {need}, which nothing provides")
        if not info["stage"]:
            continue
        for need in info["all"]:
            if need in nodes and _order(nodes[need]["stage"]) > _order(info["stage"]):
                problems.append(f"{key} ({info['stage']}) needs {need} from a later stage")
    for key, info in research.items():
        if info["stage"] not in STAGES:
            problems.append(f"research {key}: its stage {info['stage']} is not a defined stage")
    alone = reachable(nodes, research)
    for key, info in nodes.items():
        if info["kind"] in ("research", "invocation", "practice", "rank", "project", "commission") and key not in alone \
                and not info["social"]:
            problems.append(f"{key} cannot be reached by one player alone from a fresh world")
    for stage in STAGES:
        if f"stage:{stage}" not in alone:
            problems.append(f"stage {stage} cannot be reached by one player alone from a fresh world")
    for cycle in _cycles(nodes, alone):
        problems.append("circular: " + " > ".join(cycle))
    # Nothing needs what only a later stage makes: every node is reached with nothing past its own stage.
    for stage in sorted(STAGES, key=_order):
        capped = reachable(nodes, research, cap=stage)
        for key, info in nodes.items():
            if info["stage"] == stage and key in alone and key not in capped:
                problems.append(f"{key} ({stage}) can only be reached with something a later stage provides")
    ordered = sorted(STAGES, key=_order)
    for stage in ordered[2:-1]:
        working = sum(1 for route in STAGES[stage]["routes"]
                      if f"stage:{stage}" in reachable(nodes, research, True, {stage: route["id"]}))
        if working < 2:
            problems.append(f"stage {stage} has {working} route(s) one player can take: a middle stage needs two")
    for item, (kind, how, recovery, where) in {**VANILLA_SOURCES, **JUGCRAFT_SOURCES}.items():
        if kind not in ("craftable", "renewable", "gathered", "found") or not how or where not in ("", "nether", "end"):
            problems.append(f"VANILLA_SOURCES {item}: say how it is had, and where")
        if kind in ("gathered", "found") and not recovery:
            problems.append(f"VANILLA_SOURCES {item}: a {kind} material needs its recovery route")
    for encounter, (how, recovery) in ENCOUNTERS.items():
        if not how or not recovery:
            problems.append(f"ENCOUNTERS {encounter}: say how it comes and how a player without it has it")
    return problems


def mandatory(research, invocations, rituals, at_hand):
    """What every way to a stage needs (identified, not assumed): {stage: [node ids]} where a node is listed under the
    first stage one player alone could no longer reach without it."""
    nodes = graph(research, invocations, rituals, at_hand)
    alone = reachable(nodes, research)
    ordered = sorted(STAGES, key=_order)
    found = {stage: [] for stage in ordered}
    for key in sorted(alone):
        if key.startswith("stage:") or key not in nodes:
            continue
        lost = reachable(nodes, research, without={key})
        for stage in ordered:
            if f"stage:{stage}" not in lost:
                found[stage].append(key)
                break
    return found


KIND_HEADINGS = (("research", "Discoveries"), ("item", "Things at hand"), ("structure", "Structures"),
                 ("encounter", "Encounters"), ("other", "Standing and shared work"))


def mandatory_table(found):
    """The mandatory nodes as a Markdown table (the progression record shows it; tools/check_mod_data.py checks that it
    is the graph's): one row per stage, one column per kind."""
    lines = ["| Stage | " + " | ".join(heading for _, heading in KIND_HEADINGS) + " |",
             "|---" * (len(KIND_HEADINGS) + 1) + "|"]
    for stage in sorted(found, key=_order):
        columns = {kind: [] for kind, _ in KIND_HEADINGS}
        for key in found[stage]:
            kind, _, rest = key.partition(":")
            if kind == "research":
                entry, state = rest.split("@")
                columns["research"].append(f"`{entry}` {state}")
            elif kind in ("item", "tag", "anyof"):
                columns["item"].append(f"`{'#' if kind == 'tag' else ''}{rest}`")
            elif kind in ("structure", "encounter"):
                columns[kind].append(f"`{rest}`")
            else:
                columns["other"].append(f"`{key}`")
        lines.append(f"| {STAGES[stage]['name']} | " + " | ".join(", ".join(columns[kind]) or "nothing new"
                                                            for kind, _ in KIND_HEADINGS) + " |")
    return lines


def without_dimension(research, invocations, rituals, at_hand, dimension):
    """The stages one player alone still reaches when nothing from {dimension} can be had (a server without it, or a
    player who cannot get there yet)."""
    nodes = graph(research, invocations, rituals, at_hand)
    allowed = {"", "nether", "end"} - {dimension}
    reached = reachable(nodes, research, dimensions=allowed)
    return [stage for stage in sorted(STAGES, key=_order) if f"stage:{stage}" in reached]


# ------------------------------------------------------------------------------------------------------- words and data

MESSAGES = {
    "stage.reached": "You are now %s of the Concordance",
    "stage.current": "You are %s of the Concordance",
    "stage.none": "You have not yet begun the Concordance: examine something that holds its own light",
    "stage.next": "To become %s, any one of:",
    "stage.route": "  %s: %s",
    "stage.route_met": "  %s: met",
    "stage.last": "There is no stage beyond this one",
    "stage.need.research": "%s %s",
    "stage.need.mastered": "%s entries mastered (you have %s)",
    "stage.need.understood": "%s entries understood (you have %s)",
    "stage.need.traditions": "across %s traditions (you have %s)",
    "stage.need.projects": "%s Conclave projects finished (you have %s)",
    "stage.need.rank": "the Conclave rank of %s",
    "stage.need.milestone": "%s",
    "progression.whole": "The progression graph is whole: %s steps, every stage reachable alone",
    "progression.problem": "Progression: %s",
}


def lang_entries(lang):
    for key, info in MILESTONES.items():
        lang[f"compose.{MOD}.milestone.{key}"] = info["name"]
    for key, info in STAGES.items():
        lang[f"compose.{MOD}.stage.{key}"] = info["name"]
        for route in info["routes"]:
            lang[f"compose.{MOD}.stage.{key}.{route['id']}"] = ROUTE_TEXT[(key, route["id"])]
    for key, info in STAGES.items():
        advancement = f"concordance_stage_{key}"
        lang[f"advancements.{MOD}.{advancement}.title"] = info["name"]
        lang[f"advancements.{MOD}.{advancement}.description"] = (
            f"Reach the {info['name']} stage of the Concordance (Jugcraft's {info['jugcraft']})")


def codex():
    pages = [("text", "The Five Stages",
              "The Concordance has five stages, each reached by any one of its routes once the stage before it is: they "
              "are the same stages as Jugcraft's (Discovery, Workshops, Specialization, Expeditions, Shared wonders). "
              "**/jugcraft concordance stage** shows yours and, for the next, every route and what it still needs.")]
    for key in sorted(STAGES, key=lambda k: STAGES[k]["order"]):
        info = STAGES[key]
        lines = [f"- {ROUTE_TEXT[(key, route['id'])]}" for route in info["routes"]]
        pages.append(("text", info["name"], f"Jugcraft's {info['jugcraft']}. Reached by any one of:\\\n" + "\\\n".join(lines)))
    return {("foundations", "stages"): {"name": "The Five Stages", "x": 4, "y": 0, "icon": "minecraft:compass", "condition": None,
                                        "description": "Initiate, Practitioner, Adept, Master and Architect", "pages": pages}}


def write_all(write, data, lang):
    lang_entries(lang)
    previous = "concordance"
    for key in sorted(STAGES, key=lambda k: STAGES[k]["order"]):
        advancement = f"concordance_stage_{key}"
        write(data / "advancement" / f"{advancement}.json", {
            "parent": rid(previous),
            "display": {"icon": {"id": "minecraft:compass" if key != "architect" else "minecraft:nether_star"},
                        "title": {"translate": f"advancements.{MOD}.{advancement}.title"},
                        "description": {"translate": f"advancements.{MOD}.{advancement}.description"},
                        "frame": "goal" if key in ("master", "architect") else "task", "show_toast": True,
                        "announce_to_chat": key in ("adept", "master", "architect")},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})
        previous = advancement


def write_data(write, data):
    for key, info in STAGES.items():
        write(data / "concordance" / "stage" / f"{key}.json", stage_json(info))
    for key in PRACTICES:
        write(data / "concordance" / "practice" / f"{key}.json", practice_json(key))
    for key in MILESTONES:
        write(data / "concordance" / "practice" / f"{key}.json", milestone_json(key))
