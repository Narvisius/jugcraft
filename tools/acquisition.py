"""Where the things only a ritual or a lair boss gives come from, for JEI (Concordance Chunk A, unit 4:
docs/features/concordance-first-loadout.md).

JEI shows a crafting or machine recipe for most things. The Adept's Wand (made by a ritual) and what Vesperine and Madame
Tatterlace give (rolled from their loot tables when they fall) have none, so each gets an information page instead,
written here from the generators' own tables (tools/vesperine.py, tools/tatterlace.py, tools/concordance_rituals.py and
tools/lairs.py) so that its numbers are the data's.

The pages are a client copy of the default data, as the rest of recipe_view.json is: a server whose data pack changes a
boss's loot table or the Adept's Attunement is not reflected, so every page says "with the default data". A player's
first victory bringing the boss's trophy is code (VesperineLoot and TatterlaceLoot), not data.

Written by generate_material_data.py: the rows to assets/jugcraft/recipe_view.json "acquisition_info" (read by
client/compat/JugcraftJeiPlugin, which adds each as an information page) and the words to the language file, as
jei.jugcraft.acquire.<key>. The first magic loadout the record recommends is listed here too (LOADOUT), so the checker
can confirm survival already makes every piece of it, and so are the owner's armour tiers that stay creative-only
(CREATIVE_ONLY), so it can confirm nothing gives them yet. tools/check_mod_data.py (check_acquisition) ties each page,
the loadout and the creative-only tiers to the files on disk.

Pure data: it imports only the generators' tables, which import nothing of the generator's.
"""
import concordance_ember as ember
import concordance_rituals as rituals
import concordance_trinkets as trinkets
import gear
import lairs
import tatterlace as tt
import vesperine as vs

MOD = "jugcraft"
PREFIX = f"jei.{MOD}.acquire"
# Every page says so: its numbers are those of the data the mod ships, which a server's data pack may change.
DEFAULT = "with the default data"
# Who is paid when a lair boss falls (lair/Encounter, docs/features/lair-encounter-credit.md): the kinds of part a player
# may play, not their thresholds, so the words need not change if the owner moves those.
SHARE = "A share goes to each player who earns it in the fight by a blow, healing, a ward or an objective."

# The lair bosses: her name, her lair and how it is opened, the loot table rolled for each share (VesperineLoot.TABLE,
# TatterlaceLoot.TABLE), the trophy a player's first victory over her always brings, the material every share holds
# (item, (fewest, most)), the chance of each of the rest, and the Halloween event's extra chance (item, chance).
BOSSES = {
    "vesperine": {"name": vs.ENTITIES["vesperine"], "lair": lairs.LAIRS["hollow_acre"]["display"],
                  "opened": "which the Last Rites open at night", "table": "entities/vesperine",
                  "first": "vesper_scythe", "count": ("reaper_shade", vs.SHADE), "chances": vs.CHANCES,
                  "event": ("reaper_hood", vs.EVENT_HOOD_CHANCE)},
    "tatterlace": {"name": tt.ENTITIES["tatterlace"], "lair": lairs.LAIRS["spindle_loft"]["display"],
                   "opened": f"which a {lairs.ITEMS['cursed_spindle']} used on a Spinning Wheel opens at night",
                   "table": "entities/tatterlace", "first": "needle_rapier", "count": ("gossamer_silk", tt.SILK),
                   "chances": tt.CHANCES, "event": ("tatterlace_headdress", tt.EVENT_HEADDRESS_CHANCE)},
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
# Things only a ritual makes: item -> the ritual (tools/concordance_rituals.py RITUALS).
RITUALS = {"adept_wand": "adept_attunement"}

# The first magic loadout (the record's route): nothing new, only what survival already makes. Item -> how it is made:
# "recipe" (the crafting recipe of its own name) or "working:<name>" (a Lampwright's Bench working). The Wayfaring
# trinkets are an optional branch, made the same way but worn only once Relic Lore is understood; the Focus of Fire is
# the lesser focus's later upgrade (two diamonds).
LOADOUT = {"initiate_wand": "recipe", "lampwright_bench": "recipe", "kindled_lantern": "working:kindle_lantern",
           **{piece: "recipe" for piece in ember.ARMOR}, "lesser_fire_focus": "recipe", "fire_bangle": "recipe"}
OPTIONAL = {**{item: "recipe" for item in trinkets.TRINKETS}, "fire_focus": "recipe"}
# The owner's armour tiers (tools/gear.py ARMOR_TIERS, the Wayfarer among them) that no recipe, drop, shop or trade gives:
# all of them, as the owner asked when each was made (6 October 2026: "for now just make the armor we can figure that out
# later"; the later designs likewise, as tools/gear.py records). The first loadout leaves that as it is. When the owner
# settles how a tier is won, take it out of here and out of the record's creative-only table.
CREATIVE_ONLY = list(gear.ARMOR_TIERS)


def pct(chance):
    """A chance as the pages write it: 0.15 -> 15%."""
    return f"{round(chance * 100, 2):g}%"


def _the(name):
    """A lair's name inside a sentence: "The Hollow Acre" -> "the Hollow Acre"."""
    return "the" + name[3:] if name.startswith("The ") else name


def _a(name):
    """A name with its article: "an Initiate's Wand", "a Lesser Circle"."""
    return f"{'an' if name[0] in 'AEIOU' else 'a'} {name}"


def _boss_pages():
    pages = {}
    for info in BOSSES.values():
        # "Vesperine, the Last Reaper," as the subject of a sentence; "Madame Tatterlace" as she is.
        subject = info["name"] + ("," if "," in info["name"] else "")
        item, (low, high) = info["count"]
        rolls = {item: f"each share of her reward holds {low} to {high}"}
        for thing, chance in info["chances"].items():
            rolls[thing] = f"each share of her reward has a {pct(chance)} chance of it"
        rolls[info["first"]] += ", and a player's first victory over her always brings it"
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
                            f"{SHARE}")
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
    return (f"Not crafted or traded. {DEFAULT.capitalize()}, the {info['name']} turns "
            f"{_a(rituals._item_name(info['result']['transform']['from']))} into it, keeping what was inscribed and "
            f"tuned on it. Work it at {_a(blocks['circle_anchor']['name'])} in {_a(structure['name'])} "
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
