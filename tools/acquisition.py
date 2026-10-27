"""Where the things only a ritual or a lair boss gives come from, for JEI (Concordance Chunk A, unit 4:
docs/features/concordance-first-loadout.md; the Yeti King and the Cinder Tyrant since Chunk B, unit 1).

JEI shows a crafting or machine recipe for most things. The Adept's Wand and, since Concordance Chunk C, the Rooted
Thallite Foundation (each made by a ritual) and what the lair bosses give (rolled from their loot tables when they fall:
Vesperine, Madame Tatterlace, the Yeti King and the Cinder Tyrant) have none, so each gets an information page instead,
written here from the generators' own tables (tools/vesperine.py, tools/tatterlace.py, tools/yeti_king.py,
tools/cinder_tyrant.py, tools/concordance_rituals.py, tools/lairs.py and tools/biomes.py) so that its numbers are the
data's.

The pages are a client copy of the default data, as the rest of recipe_view.json is: a server whose data pack changes a
boss's loot table or the Adept's Attunement is not reflected, so every page says "with the default data". A player's
first victory bringing the boss's trophy is code (each boss's loot class: VesperineLoot, TatterlaceLoot, YetiKingLoot,
CinderTyrantLoot), not data; so is the Halloween event's extra, which only the Witching Season's two bosses have.

Written by generate_material_data.py: the rows to assets/jugcraft/recipe_view.json "acquisition_info" (read by
client/compat/JugcraftJeiPlugin, which adds each as an information page) and the words to the language file, as
jei.jugcraft.acquire.<key>. The first magic loadout the record recommends is listed here too (LOADOUT), so the checker
can confirm survival already makes every piece of it (with its optional branches: the Wayfaring trinkets and, since
Chunk B, the Rime travel kit), and so are the owner's armour tiers that stay creative-only
(CREATIVE_ONLY), so it can confirm nothing gives them yet, and, since Concordance Chunk C (C-10), the living workshop's
things (WORKSHOP; the factory's parts in FACTORY; Chunk C's own items in CHUNK_C_ITEMS), so it can confirm survival makes
each as its row says. tools/check_mod_data.py (check_acquisition) ties each page, the loadout, the workshop and the
creative-only tiers to the files on disk.

Pure data: it imports only the generators' tables, which import nothing of the generator's.
"""
import biomes
import cinder_tyrant as ct
import concordance_ember as ember
import concordance_rime_kit as rime_kit
import concordance_rituals as rituals
import concordance_trinkets as trinkets
import gear
import lairs
import tatterlace as tt
import vesperine as vs
import yeti_king as yk

MOD = "jugcraft"
PREFIX = f"jei.{MOD}.acquire"
# Every page says so: its numbers are those of the data the mod ships, which a server's data pack may change.
DEFAULT = "with the default data"
# Who is paid when a lair boss falls (lair/Encounter, docs/features/lair-encounter-credit.md): the kinds of part a player
# may play in that boss's fight, not their thresholds, so the words need not change if the owner moves those. Every boss
# paid through lair/LairRewards credits a blow, healing and a ward; an objective only where its fight has one (a deed,
# Encounter.deed). check_acquisition holds each boss's words to its code.
PARTS = ("a blow", "healing", "a ward")
OBJECTIVE = "an objective"

# The lair bosses, each a row: their name; the pronouns their pages use (possessive, object); their lair and how it is
# opened; the loot class that pays each share (its path under src/main/java/io/github/jimbozoomer/jugcraft, which the
# checker reads: the Yeti King's is lair/yeti/YetiKingLoot.java, not one built from his name) and the loot table it
# rolls for each share (its TABLE); whether their fight has an objective (a deed); the material every share holds
# (item, (fewest, most)); the chance of each single thing; their trophies, either "first" (one trophy, among the
# chances, that a player's first victory always brings) or "trophies" (two that share one pool, (items, chance), of which
# a first victory always brings one); and, for the Witching Season's bosses only, the Halloween event's extra chance
# (item, chance).
BOSSES = {
    "vesperine": {"name": vs.ENTITIES["vesperine"], "pronouns": ("her", "her"),
                  "lair": lairs.LAIRS["hollow_acre"]["display"], "opened": "which the Last Rites open at night",
                  "loot_class": "lair/vesperine/VesperineLoot.java", "table": "entities/vesperine", "objective": True,
                  "first": "vesper_scythe", "count": ("reaper_shade", vs.SHADE), "chances": vs.CHANCES,
                  "event": ("reaper_hood", vs.EVENT_HOOD_CHANCE)},
    "tatterlace": {"name": tt.ENTITIES["tatterlace"], "pronouns": ("her", "her"),
                   "lair": lairs.LAIRS["spindle_loft"]["display"],
                   "opened": f"which a {lairs.ITEMS['cursed_spindle']} used on a Spinning Wheel opens at night",
                   "loot_class": "lair/tatterlace/TatterlaceLoot.java", "table": "entities/tatterlace", "objective": True,
                   "first": "needle_rapier", "count": ("gossamer_silk", tt.SILK), "chances": tt.CHANCES,
                   "event": ("tatterlace_headdress", tt.EVENT_HEADDRESS_CHANCE)},
    "yeti_king": {"name": yk.ENTITIES["yeti_king"], "pronouns": ("his", "him"),
                  "lair": lairs.LAIRS["glacier_hall"]["display"],
                  "opened": f"which a {lairs.ITEMS['frost_horn']} blown on snow or ice opens at night",
                  "loot_class": "lair/yeti/YetiKingLoot.java", "table": "entities/yeti_king", "objective": True,
                  "trophies": (yk.TROPHIES, yk.TROPHY_CHANCE), "count": ("yeti_fur", yk.FUR), "chances": yk.CHANCES},
    "cinder_tyrant": {"name": ct.ENTITIES["cinder_tyrant"], "pronouns": ("his", "him"),
                      "lair": lairs.LAIRS["cinder_kiln"]["display"],
                      "opened": (f"which a {lairs.ITEMS['kiln_seal']} pressed into a magma block in the Nether, "
                                 + " or ".join(f"the {biomes.BIOMES[land.split(':')[1]]['display']}"
                                               for land in lairs.SEAL_LANDS) + " opens at any hour"),
                      "loot_class": "lair/tyrant/CinderTyrantLoot.java", "table": "entities/cinder_tyrant",
                      "objective": False, "trophies": (ct.TROPHIES, ct.TROPHY_CHANCE), "count": ("tyrant_scale", ct.SCALES),
                      "chances": ct.CHANCES},
}


def _crafted(key, recipes, names):
    """A shaped recipe of one ingredient (a boss generator's RECIPES[key]): (its recipe file, the ingredient, how many
    the pattern takes, the ingredient's name)."""
    recipe = recipes[key]
    [(symbol, ingredient)] = recipe["key"].items()
    item = ingredient.split(":")[1]
    return key, item, sum(row.count(symbol) for row in recipe["pattern"]), names[item]


# Loot a crafting recipe makes as well: item -> (recipe, ingredient, count, the ingredient's name).
CRAFTED = {"reaper_hood": _crafted("reaper_hood", vs.RECIPES, vs.ITEMS)}
# Things only a ritual makes: item -> the ritual (tools/concordance_rituals.py RITUALS). The Rooted Thallite Foundation
# since Concordance Chunk C (C-04, Root and Rivet).
RITUALS = {"adept_wand": "adept_attunement", "rooted_thallite_foundation": "root_and_rivet"}

# The first magic loadout (the record's route): nothing new, only what survival already makes. Item -> how it is made:
# "recipe" (the crafting recipe of its own name) or "working:<name>" (a Lampwright's Bench working). The Wayfaring
# trinkets are an optional branch, made the same way but worn only once Relic Lore is understood; the Focus of Fire is
# the lesser focus's later upgrade (two diamonds); and Rime's travel kit (tools/concordance_rime_kit.py; Concordance
# Chunk B, B-10), its foci, bangle and three water sets, is another optional branch made the same way.
LOADOUT = {"initiate_wand": "recipe", "lampwright_bench": "recipe", "kindled_lantern": "working:kindle_lantern",
           **{piece: "recipe" for piece in ember.ARMOR}, "lesser_fire_focus": "recipe", "fire_bangle": "recipe"}
OPTIONAL = {**{item: "recipe" for item in trinkets.TRINKETS}, "fire_focus": "recipe",
            **{item: "recipe" for item in rime_kit.ITEMS}}  # Rime's travel kit (Chunk B, B-10), made the same way
# The owner's armour tiers (tools/gear.py ARMOR_TIERS, the Wayfarer among them) that no recipe, drop, shop or trade gives:
# all of them, as the owner asked when each was made (6 October 2026: "for now just make the armor we can figure that out
# later"; the later designs likewise, as tools/gear.py records). The first loadout leaves that as it is. When the owner
# settles how a tier is won, take it out of here and out of the record's creative-only table.
CREATIVE_ONLY = list(gear.ARMOR_TIERS)
# Concordance Chunk C (C-10): the living workshop's route (tools/concordance_journey.py WORKSHOP). WORKSHOP: every
# Concordance thing it makes or places, each made in survival as it is: by the crafting recipe of its own name ("recipe"),
# the recipe named ("recipe:<file>": the Thallite plate's is thallite_plate_by_hand), a Lampwright's Bench working
# ("working:<name>") or a ritual RITUALS gives a JEI page ("ritual:<name>"). FACTORY: the factory's parts the powered
# workshop takes (JE and piped water), each by its own recipe; their bronze, plates and gears are the factory's
# (docs/features/machines-and-power.md), not followed by the Concordance's graph. CHUNK_C_ITEMS: every item Concordance
# Chunk C registered (C-04's foundation, C-05's stand; the Stillroot Poultice and the Kiln Draught are brews, never items,
# and a graft or a mixed bed is a bed's state), each a WORKSHOP row. check_acquisition ties all three to the files on disk
# and to the server test that loads their recipes (ConcordanceJourneyGameTests.WORKSHOP_RECIPES).
WORKSHOP = {"mendvetch": "recipe", "crucible": "recipe", "formula": "recipe", "lampwright_bench": "recipe",
            "kindled_lantern": "working:kindle_lantern", "ley_pylon": "recipe", "circle_anchor": "recipe",
            "warding_stone": "recipe", "thallite_plate": "recipe:thallite_plate_by_hand",
            "rooted_thallite_foundation": "ritual:root_and_rivet", "crucible_stand": "recipe", "courier_post": "recipe",
            "clockwork_porter": "recipe", "porter_key": "recipe", "verdant_bed": "recipe", "sunpetal": "recipe",
            "dewmoss": "recipe", "gloamcap": "recipe"}
FACTORY = {"coal_generator": "recipe", "copper_cable": "recipe", "machine_casing": "recipe", "metal_press": "recipe",
           "electric_pump": "recipe", "bronze_fluid_pipe": "recipe"}
CHUNK_C_ITEMS = ["rooted_thallite_foundation", "crucible_stand"]
# Concordance Chunk D (D-09): the pact hall's route (tools/concordance_journey.py PACT). PACT_HALL: every Concordance thing
# it makes or places, each made in survival as WORKSHOP's rows say ("recipe", "recipe:<file>", "working:<name>" or
# "ritual:<name>"): the Verdant Bed and a Sunpetal (the cultivation Stolas's Regard asks), the Assayer's Scale (its assay),
# the Orrery Observatory (the sky step, the resonance and the first obligation), the Astrolabe (the Open Hand Compact's
# resonance and Star-reckoning's), the Pact Perch, and the Spirit Anchor, Courier Post and Porter Key of the workshop hand
# it unlocks. CHUNK_D_ITEMS: every item Concordance Chunk D registered (the perch: tools/concordance_pacts.py BLOCKS;
# Stolas and the hand are entities, the compact a cost and Star-reckoning a service), each a PACT_HALL row.
# check_acquisition ties both to the files on disk and to the server test that loads their recipes
# (ConcordanceJourneyGameTests.PACT_HALL_RECIPES).
PACT_HALL = {"verdant_bed": "recipe", "sunpetal": "recipe", "assayers_scale": "recipe", "observatory": "recipe",
             "astrolabe": "recipe", "pact_perch": "recipe", "spirit_anchor": "recipe", "courier_post": "recipe",
             "porter_key": "recipe"}
CHUNK_D_ITEMS = ["pact_perch"]


def recipe_file(item, how):
    """The recipe file a WORKSHOP or FACTORY row is made by ("recipe": its own name; "recipe:<file>"), or None for a
    bench working or a ritual."""
    if how == "recipe":
        return item
    return how.split(":", 1)[1] if how.startswith("recipe:") else None


def pct(chance):
    """A chance as the pages write it: 0.15 -> 15%."""
    return f"{round(chance * 100, 2):g}%"


def _the(name):
    """A lair's name inside a sentence: "The Hollow Acre" -> "the Hollow Acre"."""
    return "the" + name[3:] if name.startswith("The ") else name


def _a(name):
    """A name with its article: "an Initiate's Wand", "a Lesser Circle"."""
    return f"{'an' if name[0] in 'AEIOU' else 'a'} {name}"


def share(info):
    """A boss page's last sentence: who is paid, by the kinds of part its fight credits."""
    parts = list(PARTS) + ([OBJECTIVE] if info["objective"] else [])
    return f"A share goes to each player who earns it in the fight by {', '.join(parts[:-1])} or {parts[-1]}."


def pair_roll(info):
    """What a page of one of a pair of trophies says of its pool: one roll gives one of the two, each as likely."""
    his = info["pronouns"][0]
    (first, second), chance = info["trophies"]
    return f"a {pct(chance)} chance of one of {his} two trophies ({pct(chance / 2)} each)"


def _boss_pages():
    pages = {}
    for info in BOSSES.values():
        # "Vesperine, the Last Reaper," as the subject of a sentence; "Madame Tatterlace" and "The Yeti King" as they are.
        subject = info["name"] + ("," if "," in info["name"] else "")
        his, him = info["pronouns"]
        item, (low, high) = info["count"]
        rolls = {item: f"each share of {his} reward holds {low} to {high}"}
        for thing, chance in info["chances"].items():
            rolls[thing] = f"each share of {his} reward has a {pct(chance)} chance of it"
        if "trophies" in info:
            for thing in info["trophies"][0]:
                rolls[thing] = (f"each share of {his} reward has {pair_roll(info)}, and a player's first victory over "
                                f"{him} always brings one of the two")
        else:
            rolls[info["first"]] += f", and a player's first victory over {him} always brings it"
        if "event" in info:
            thing, chance = info["event"]
            rolls[thing] += (f", and another {pct(chance)} chance during the Halloween event unless the server turns "
                             "lairs.event_loot off")
        for thing, roll in rolls.items():
            if thing in CRAFTED:
                _recipe, _ingredient, count, name = CRAFTED[thing]
                lead = f"Crafted from {count} {name}. {subject} also gives it"
            else:
                lead = f"Not crafted or traded. {subject} gives it"
            pages[thing] = (f"{lead} in {_the(info['lair'])}, {info['opened']}. {DEFAULT.capitalize()}, {roll}. "
                            f"{share(info)}")
    return pages


def _ritual_page(key):
    info = rituals.RITUALS[key]
    structure = rituals.STRUCTURES[info["structure"].split(":")[1]]
    parts = {part["role"]: len(part["at"]) for part in structure["parts"]}
    research = rituals.RESEARCH[info["research"].split(":")[1]]["name"]
    offerings = [f"{o['count']} {rituals._item_name(o['item'])}" for o in info["offerings"]]
    offer = ", ".join(offerings[:-1]) + f" and {offerings[-1]}" if len(offerings) > 1 else offerings[0]
    if info["participants"] == 1:
        takes = f"one practitioner, {info['focus']} Focus and {info['steps']} steps"
    else:
        takes = f"{info['participants']} practitioners, {info['focus']} Focus each, and {info['steps']} steps"
    blocks = rituals.BLOCKS
    transform = info["result"]["transform"]
    # Only an instrument carries an inscription and tunings over (CircleAnchorBlockEntity.commit copies both).
    keeps = ", keeping what was inscribed and tuned on it" if transform["into"].split(":")[1] in rituals.INSTRUMENTS else ""
    # A rite named for whose it is takes "the" (the Adept's Attunement); one named for what it does, none (Root and Rivet).
    rite = f"the {info['name']}" if "'s " in info["name"] else info["name"]
    return (f"Not crafted or traded. {DEFAULT.capitalize()}, {rite} turns "
            f"{_a(rituals._item_name(transform['from']))} into it{keeps}. Work it at {_a(blocks['circle_anchor']['name'])} "
            f"in {_a(structure['name'])} "
            f"({parts['channel']} {blocks['ley_pylon']['name']}s and {parts['boundary']} "
            f"{blocks['warding_stone']['name']}s round it) with {research} {info['stage']}: offer {offer}. It takes "
            f"{takes}, drawing {info['ley']} Ley Charge from each pylon a step "
            f"({info['ley'] * info['steps'] * parts['channel']} in all).")


def pages():
    """Each page's words: item -> text."""
    return {**_boss_pages(), **{item: _ritual_page(ritual) for item, ritual in RITUALS.items()}}


def rows():
    """recipe_view.json "acquisition_info": the item each page is shown for and its key, in key order."""
    return [{"item": f"{MOD}:{key}", "key": key} for key in sorted(pages())]


def lang_entries(lang):
    for key, text in pages().items():
        lang[f"{PREFIX}.{key}"] = text


if __name__ == "__main__":
    for key, text in sorted(pages().items()):
        print(f"{key}: {text}\n")
