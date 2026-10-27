"""Chunk A (A-04): the Jugcraft Encyclopedia's words, its first route and its Keys page.

The encyclopedia is one screen with pages the server writes for each player (guide/encyclopedia/Encyclopedia.java):
Discover (the First Light route, concordance/encyclopedia/FirstLightRoute.java), Spellcraft, Expeditions (the lair
dossiers, lair/LairDossiers.java, then the bestiary, guide/encyclopedia/Bestiary.java) and Inspect, then the client's own Keys page. This module holds every word they show
and the route's shape: its steps in order, the recipes, sales and codex entries each step names, and the game tests
that exercise it. generate_material_data.py writes the words into the language file; check_mod_data.py
(check_encyclopedia) checks the Java against PAGES, STATES, EDGES, FIRST_ROUTE, DOSSIERS (DOSSIERS also to the loot,
trophy and season data) and KEY_HELP, and the record docs/features/arcane-concordance-first-route.md against
route_table().

Run it to print the route table: python3 tools/encyclopedia.py
"""
import json
from pathlib import Path

import concordance_ember

MOD = "jugcraft"
ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src" / "main" / "resources" / "data" / MOD

# The pages, in the order the encyclopedia shows them (the last is the client's own).
PAGES = {
    "discover": "Discover",
    "spellcraft": "Spellcraft",
    "expedition": "Expeditions",
    "inspect": "Inspect",
    "nouveau": "Jug Nouveau",
    "menagerie": "Wildlife & Companions",
    "woodland": "Woodland & Fable Creatures",
    "household": "Furniture & Kitchens",
    "habitats": "Habitats & Mutations",
    "beasts": "Creature Equipment",
    "caverns": "Cave Exploration",
    "jimta_network": "Digital Workshops",
    "jimta_guides": "Workshop Guides",
    "keys": "Keys",
}
SERVER_PAGES = ["discover", "spellcraft", "expedition", "inspect", "nouveau", "menagerie", "woodland", "household", "habitats", "beasts", "caverns", "jimta_network", "jimta_guides"]

# Where a step of a route stands for a player (EncyclopediaPayload.StepState, NONE aside), in words.
STATES = {
    "done": "Done",
    "next": "Next",
    "open": "Can do now",
    "later": "Later",
    "blocked": "Not available here",
}
# How much a step, or a way to it, matters (EncyclopediaPayload.EdgeKind, NONE aside).
EDGES = {
    "required": "Required",
    "recommended": "Recommended",
    "trade": "Trade",
    "optional_boss": "Optional: after a boss",
}

# The First Light route (FirstLightRoute.java), in order. Each step: how much it matters, when it is done, what it waits
# for, the recipes it shows as grids (edge, recipe id), the recipes it only names, as a set's icons shown when each
# exists ("named"), the town sales it names (shop id, item id: each must be on sale there), the codex entries it links
# (entry, research or None, state or None: the entry's own unlock) and the game tests that exercise it.
FIRST_ROUTE = [
    {"id": "find", "edge": "required", "done": "First Light encountered, or a luminous specimen carried", "after": [],
     "recipes": [], "trades": [("curios", "minecraft:glow_ink_sac")],
     "codex": [("radiance/first_light", None, None)],
     "tests": ["aNewcomerSeesOnlyTheFirstSteps", "theRouteNamesOnlyRealSales"]},
    {"id": "examine", "edge": "required", "done": "First Light encountered", "after": ["find"],
     "recipes": [], "trades": [], "codex": [], "tests": ["theRouteFollowsTheResearch", "examiningBySneakUse"]},
    {"id": "dark", "edge": "required", "done": "First Light observed", "after": ["examine"],
     "recipes": [], "trades": [], "codex": [], "tests": ["theRouteFollowsTheResearch", "fieldEvidenceUnderstandsFirstLight"]},
    {"id": "understand", "edge": "required", "done": "First Light understood", "after": ["dark"],
     "recipes": [("recommended", "jugcraft:lampwright_bench")], "trades": [],
     "codex": [("radiance/lampwright_bench", None, None), ("foundations/research_notes", None, None)],
     "tests": ["theRouteFollowsTheResearch", "benchStudyUnderstandsFirstLight", "anAbsentRecipeIsNeverAdvertised"]},
    {"id": "wand", "edge": "required", "done": "an instrument carried", "after": [],
     "recipes": [("required", "jugcraft:initiate_wand")], "trades": [],
     "codex": [("foundations/initiate_wand", None, None)],
     "tests": ["everyRecipeShownIsOnTheServer", "anAbsentRecipeIsNeverAdvertised"]},
    {"id": "cast", "edge": "required", "done": "the advancement Let There Be (Kindle cast)", "after": ["understand", "wand"],
     "recipes": [], "trades": [], "codex": [("invocations/kindle", "jugcraft:first_light", "UNDERSTOOD")],
     "tests": ["theRouteFollowsTheResearch", "kindleCastsThroughSpellEngine"]},
    {"id": "compose", "edge": "required", "done": "a spell inscribed on a carried instrument", "after": ["understand", "wand"],
     "recipes": [], "trades": [], "codex": [("composition/composing", "jugcraft:first_light", "UNDERSTOOD")],
     "tests": ["theRouteFollowsTheResearch", "spellcraftShowsWhatIsHeld"]},
    {"id": "support", "edge": "recommended", "done": "an inscription that wards someone other than its caster (ward, with "
     "or without modifiers, not delivered here)", "after": ["compose"],
     "recipes": [], "trades": [], "codex": [], "tests": ["theRouteFollowsTheResearch", "aWardForAFriendIsComposedAndCast"]},
    {"id": "equip", "edge": "recommended", "done": "a Kindled Lantern, a fire-set piece, a fire focus or the Fire Bangle "
     "carried or worn", "after": ["understand"],
     "recipes": [("recommended", "jugcraft:lesser_fire_focus"), ("recommended", "jugcraft:fire_bangle")],
     "named": [("recommended", "jugcraft:pyromancers_hat"), ("recommended", "jugcraft:pyromancers_robes"),
               ("recommended", "jugcraft:pyromancers_leggings"), ("recommended", "jugcraft:pyromancers_boots"),
               ("recommended", "jugcraft:pyromaniacs_hood"), ("recommended", "jugcraft:pyromaniacs_tunic"),
               ("recommended", "jugcraft:pyromaniacs_pants"), ("recommended", "jugcraft:pyromaniacs_shoes")],
     "trades": [("general", "minecraft:lantern")], "codex": [], "tests": ["theFirstLoadoutIsMadeInSurvival"]},
    {"id": "lair", "edge": "optional_boss", "done": "a first win over any lair's keeper: The Last Harvest, Unravelled, "
     "Abominable or Tempered", "after": ["compose"],
     "recipes": [], "trades": [], "codex": [], "tests": ["aBossIsHiddenUntilDiscovered", "aWinOverAnyKeeperIsTheLairStep"]},
]

# The lair dossiers (LairDossiers.java), by keeper id (its entity, its LairRewards name, its loot tables): the lair, the
# first-win advancement and trophies (one, or one of two at random: loot_table/bosses/<boss>.json), the class whose check
# is the rite and the words it gives when refused out of the Halloween season (None: it works all year), the tokens, the
# recipes the rite needs (the first makes a token), the town sales that help, the cheaper recipes for a token the loot
# opens ("after_win") and the pieces made of the loot ("made_from").
DOSSIERS = {
    "vesperine": {"lair": "hollow_acre", "advancement": "the_last_harvest", "trophies": ["vesper_scythe"],
                  "rite_class": "LastRites", "off_season": f"message.{MOD}.lair.rite.off_season",
                  "tokens": ["death_knell", "mourning_wreath"],
                  "rite": ["death_knell", "mourning_wreath", "gothic_headstone_from_stonecutting"],
                  "trades": [("florist", "minecraft:lily_of_the_valley"), ("curios", "minecraft:candle")],
                  "after_win": ["mourning_wreath_from_shade"], "made_from": ["reaper_hood"],
                  "loot": ["reaper_shade", "vesper_scythe", "reaper_hood", "dirge_skull", "requiem_skull"]},
    "tatterlace": {"lair": "spindle_loft", "advancement": "unravelled", "trophies": ["needle_rapier"],
                   "rite_class": "SpindleRite", "off_season": f"message.{MOD}.lair.spindle.off_season",
                   "tokens": ["cursed_spindle"], "rite": ["cursed_spindle", "spinning_wheel"], "trades": [],
                   "after_win": ["cursed_spindle_from_silk"], "made_from": [],
                   "loot": ["gossamer_silk", "needle_rapier", "golden_thimble", "tatterlace_headdress"]},
    "yeti_king": {"lair": "glacier_hall", "advancement": "abominable", "trophies": ["glacier_maul", "rimeclaw"],
                  "rite_class": "FrostHornRite", "off_season": None,
                  "tokens": ["frost_horn"], "rite": ["frost_horn"], "trades": [("seasonal", "minecraft:snow_block")],
                  "after_win": ["frost_horn_from_fur"], "made_from": [],
                  "loot": ["yeti_fur", "glacier_maul", "rimeclaw", "yeti_mitten", "yeti_king_crown"]},
    "cinder_tyrant": {"lair": "cinder_kiln", "advancement": "tempered", "trophies": ["cinderbrand", "magmaw"],
                      "rite_class": "KilnSealRite", "off_season": None,
                      "tokens": ["kiln_seal"], "rite": ["kiln_seal"], "trades": [],
                      "after_win": ["kiln_seal_from_scales"], "made_from": [],
                      "loot": ["tyrant_scale", "cinderbrand", "magmaw", "salamander_charm", "tyrant_crest"]},
}

# What each Jugcraft key does, for the Keys page (key.jugcraft.<name>; every key the client registers).
KEY_HELP = {
    "nouveau_familiar": "Choose an owned familiar (not bound at first)",
    "nouveau_crown": "Use the worn Alchemist’s Crown (not bound at first)",
    "nouveau_spell_style": "Set the held spell instrument’s colors, particles and sound",
    "nouveau_familiar_collection": "Toggle your familiar’s collection of drops",
    "encyclopedia": "Opens this encyclopedia from the game",
    "concordance_journal": "Opens your Concordance Journal: research, stage, and Compose",
    "concordance_composer": "Opens the Spell Composer straight away (not bound at first)",
    "concordance_config": "Opens the Concordance's settings on this computer (not bound at first)",
    "concordance_roster": "Opens your roster: pacts, agreements, familiars, spirits, constructs and courier requests, and what each waits for (not bound at first)",
    "party": "Opens the Party screen",
    "gem_abilities": "Opens worn Gem armor abilities (not bound at first)",
    "gem_night_vision": "Toggles active Gem helmet night vision (not bound at first)",
    "gem_repel": "Uses the active Gem chestplate's repel burst (not bound at first)",
    "gem_dash": "Dashes with active Gem leggings (not bound at first)",
    "gem_flight": "Toggles active Gem boots' flight (not bound at first)",
    "reload": "Reloads the gun in your hand",
    "inspect": "Inspects the gun in your hand",
    "stab": "Stabs with the bayonet on your gun",
    "exchange": "Opens the Exchange: bank, markets, land, shares and the casino",
}

# The inventory button's sprite, copied byte for byte from the owner's library (A-D8): runtime path under
# assets/jugcraft -> path under art/owner-library/originals/Magic. It came in a set with waystone sprites, which are not
# used; that set's language file credits "Icons by JoeCreates (CC-BY-SA 3.0)" (waystones.iconCredits), a notice kept
# with the asset in docs/features/arcane-concordance-first-route.md.
OWNER_FILES = {
    "textures/gui/encyclopedia/book.png": "gui/spell_book_template.png",
    "textures/gui/sprites/encyclopedia/inventory_button.png": "textures/gui/sprites/inventory/inventory_button.png",
}


def _set_line(name):
    """A fire set in words, from tools/concordance_ember.py: what it is made of and the protection it gives."""
    armour = concordance_ember.ARMOR_SETS[name]
    defense = armour["defense"]
    pieces = "/".join(str(defense[piece]) for piece in ("helmet", "chestplate", "leggings", "boots"))
    made = "all wool" if name == "pyromaniacs" else "wool and gold"
    return (f"{armour['name']} set: {made}; protection {pieces} ({sum(defense.values())} in all); each piece adds "
            f"{concordance_ember.ROBE_PIECE_POWER:g} fire Spell Power")


def _item_line(item):
    entry = concordance_ember.ITEMS[item]
    return f"{entry['name']}: {entry['tooltip']}"


ROUTE = {
    "heading": "From a glow to your first spell (%s)",
    "missing": "This server's data has no First Light research, so there is no first route here",
    "find": "Find something that glows",
    "examine": "Examine it: hold it, sneak (%s) and use it (%s)",
    "examine.air": "Aim at the open air: aimed at a block, a specimen that can be placed may be placed instead",
    "dark": "Examine one in the dark, at light %s or less",
    "dark.light": "Light at your eyes now: %s. The darkest you have examined one at: %s",
    "dark.light_none": "Light at your eyes now: %s",
    "dark.where": "A cave, or the open air at night, away from torches and lanterns",
    "understand": "Understand %s",
    "understand.examine": "Examine %s different glowing things in the dark (light %s or less)",
    "understand.any": "Any one of these ways is enough",
    "understand.study": "Study one at a %s: it takes %s seconds and uses the specimen up. Once you have examined one in "
                        "the dark, one study is enough on its own; a study made before that counts once you have",
    "understand.notes": "Read Research Notes written by a player who understands it",
    "understand.other": "Another way this server's data gives (your journal names it)",
    "make": "Recipe: %s",
    "wand": "Make a wand (%s) and hold it in your main hand",
    "cast": "Cast an invocation",
    "cast.kindle": "Cast %s",
    "cast.goal": "Casting %s completes this step: with the invocations you know, %s casts it",
    "cast.how": "With the wand in your main hand, %s casts the first invocation on your spell bar, and %s, %s and on "
                "cast the ones after it; while it is in your hand those keys cast instead of changing slot, so scroll "
                "to change slot",
    "cast.invocation": "%s: %s Focus",
    "cast.more": "and %s more",
    "compose": "Write a spell of your own and inscribe it on your wand",
    "compose.how": "With the wand in your main hand, open your journal (%s) and choose %s",
    "compose.example": "For example: %s",
    "support": "Inscribe a ward for a friend",
    "support.example": "For example: %s, which wards the friend you touch",
    "support.credit": "In a lair, a ward you put on another player earns your share of the keeper's reward once it has "
                      "soaked %s health from the blows of the keeper's side; cast it after the keeper wakes. %s wards "
                      "only you, and earns nothing",
    "equip": "Equip yourself for casting",
    "equip.lantern": "%s: made at a %s from %s and %s, once you understand %s",
    "equip.pyromancers": _set_line("pyromancers"),
    "equip.pyromaniacs": _set_line("pyromaniacs"),
    "equip.lesser_fire_focus": _item_line("lesser_fire_focus"),
    "equip.fire_bangle": _item_line("fire_bangle"),
    "lair": "Face a lair's keeper with friends",
    "lair.unknown": "Lairs open by rites, and a rite needs a token you make: carry one and its rite shows under Expeditions",
    "lair.known": "%s: its rite and its keeper are under Expeditions",
    "lair.known_rite": "%s: its rite is under Expeditions",
}

SPELLCRAFT = {
    "instrument_heading": "In your hand",
    "instrument": "Instrument: %s",
    "instrument.limits": "Capacity %s; at most %s targets, %s work and %s branches; what it makes lasts at most %s seconds",
    "no_inscription": "Nothing is inscribed on it",
    "no_instrument": "No instrument in your main hand: hold a wand to compose and cast",
    "focus_regen": "One comes back every %s seconds",
    "invocations": "Invocations you know",
    "no_invocations": "None yet: understanding research teaches invocations",
    "invocations_unknown": "%s more are yet to be learned",
    "words": "Words you can compose with",
    "slot": "%s: %s",
    "no_words": "None yet: understanding research teaches words",
    "compose_needs": "Hold an instrument in your main hand to open the Spell Composer",
}

INSPECT = {
    "at": "%s at %s, %s, %s",
    "rituals": "Rituals you know here",
    "ritual": "Players: %s; Focus: %s each; %s steps (%s seconds), each drawing %s Ley Charge from every channel",
    "no_rituals": "You know no ritual worked here yet",
    "rituals_unknown": "%s more are yet to be learned",
    "pylon.fill": "Fill it by pouring a Kindled Lantern in (%s Radiance makes %s Ley Charge), or with Jugcraft Energy "
                  "(%s JE a Ley Charge)",
    "pylon.fill_energy": "Fill it with Jugcraft Energy (%s JE a Ley Charge)",
    "pylon.lends": "It lends its charge to your rituals",
    "pylon.owned": "It is someone else's: it lends only to rituals they take part in",
    # Concordance Chunk C, C-07: a Verdant Bed or a crop: what the plant on it needs, against what it has.
    "garden.habitat": "What %s needs, and what it has",
    "garden.here": "The habitat here",
    # Concordance Chunk C, C-01: an Alembic Crucible's slots, and the hint that a glass reads more.
    "crucible.buffer": "Waiting to go in: %s",
    "crucible.bottles": "Bottles and bowls: %s",
    "crucible.output": "Made: %s",
    "crucible.none": "nothing",
    "crucible.count": "%s %s",
    "crucible.glass": "An Assay Glass, carried by someone who understands the Alembic Arts, reads its makeup here",
    # Concordance Chunk C (C-03): a rite's readiness, what a running one has spent and holds, how the last broke off, and
    # its stages and payments.
    "readiness": "Before %s can begin",
    "readiness.ready": "Nothing is missing: use the anchor with an empty hand to begin it",
    "readiness.unknown": "These offerings answer a rite you have not learned; the codex says how to learn it",
    "unknown_rite": "a rite you have not learned",
    "under_way": "%s, under way",
    "completed": "%s, complete",
    "anchor.held": "Held until it ends, and consumed only if it completes: %s",
    "anchor.count": "%s %s",
    "last": "Last attempt: %s",
    "stages": "%s: its stages and what each pays",
}

DOSSIER = {
    "hollow_acre.rite": "The Last Rites, at night in the Overworld: lay a Mourning Wreath on a grave (any headstone) or "
                        "within %s blocks of it, light %s or more candles within %s blocks of the grave, and ring the "
                        "Death Knell within %s blocks of it",
    "spindle_loft.rite": "At night in the Overworld, use the Cursed Spindle on a Spinning Wheel: you prick your finger, "
                         "fall asleep and wake in the loft, and the spindle is used up. While the wheel spins, friends "
                         "follow by using it with an empty hand",
    "glacier_hall.rite": "At night in the Overworld, blow the Frost Horn standing on snow or ice: a snow layer or block, "
                         "powder snow, any ice, or Seasonal Snow where winter lays it. The snow splits into a whirl of "
                         "white mist and you fall through to the hall's ledge with frost on your skin; the horn is used "
                         "up only if the hall opens. While the whirl spins, friends follow by using it",
    # Concordance Chunk B (B-08, B-P29, B-P45): what readies a player for his frost, as text and icons, no recipe grid.
    "glacier_hall.prepare": "Against his frost: Winter Brine, snowballs stirred cold in an Alembic Crucible, gives "
                            "Frostward for up to %s seconds a draught; the Yeti Mitten from his loot keeps its holder "
                            "warm for as long as it is held",
    "glacier_hall.prepare.mitten": "Against his frost: the Yeti Mitten from his loot keeps its holder warm for as long as "
                                   "it is held",
    "cinder_kiln.rite": "At any hour, press the Kiln Seal into a magma block in the Nether, or in a volcanic land of the "
                        "Overworld (the Volcano or the Cinder Barrens). The magma cracks open into a vent of sparks and "
                        "smoke and you sink through the stone to the kiln's ledge; the seal is used up only if the kiln "
                        "opens. While the vent smokes, friends follow by using it",
    # Concordance Chunk C (C-02, C-P9): what readies a player for his slag and flame, as text and icons, no recipe grid.
    "cinder_kiln.prepare": "Against his slag and flame: the Kiln Draught, magma cream or sugar stirred in an Alembic "
                           "Crucible, gives Fire Resistance for up to %s seconds a draught, and under it the slag does not "
                           "burn and his flame does not take, though his blows, gobs and cinders still land; the Salamander "
                           "Charm from his loot, held in the other hand, keeps hot ground from burning its holder for as "
                           "long as it is held",
    "cinder_kiln.prepare.charm": "Against his slag: the Salamander Charm from his loot, held in the other hand, keeps hot "
                                 "ground from burning its holder for as long as it is held",
    "grave": "A grave: any headstone, such as a %s",
    "any_night": "The rite works on any night",
    "in_season": "The rite works now, in the Halloween season",
    "all_year": "This rite works all year, not only in the Halloween season",
    "party": "A lair holds %s players; its gate stays open %s seconds for friends to follow",
    "hollow_acre.credit": "A share of her reward goes to everyone who strikes her, her skulls Dirge and Requiem or a "
                          "Soul of the Harvest (not her Grave Thralls), gives friends back %s or more of the health her "
                          "side's blows took, wards a friend against %s or more health of those blows, or relights one of "
                          "her wards in her Harvest, within %s blocks of her arena",
    "spindle_loft.credit": "A share of her reward goes to everyone who strikes her, her Egg Sacs or her Spiderlings, "
                           "gives friends back %s or more of the health her side's blows took, wards a friend against %s "
                           "or more health of those blows, or cuts a friend free of her Binding Thread, within %s blocks "
                           "of her arena",
    "glacier_hall.credit": "A share of his reward goes to everyone who strikes him or his Yeti Whelps, gives friends back "
                           "%s or more of the health his side's blows took from them (freezing is not a blow), wards a "
                           "friend against %s or more health of those blows, braces a friend who then takes one of his "
                           "throws, or, having already taken part, baits his charge into an ice column, within %s blocks "
                           "of his lake",
    "cinder_kiln.credit": "A share of his reward goes to everyone who strikes him or his Cinderlings, gives friends back "
                          "%s or more of the health his side took from them (blows, gobs, cinders and flame; not burning "
                          "afterwards, nor slag), or wards a friend against %s or more health of those, within %s blocks "
                          "of his bowl",
    "credit.wards": "Cast a ward after the keeper wakes: one cast before is not counted. Those who fall are paid when "
                    "they are back on their feet, and those who leave the game when they come back",
    # Concordance Chunk C (C-02, C-P8): after credit.wards, while the Concordance's Verdance brews Regeneration.
    "credit.poultice": "A Stillroot Poultice you put on a friend in the fight counts as healing: each health its "
                       "Regeneration then gives back of what the keeper's side took from them, while they stay in the "
                       "fight. One on yourself earns nothing, and two on one friend heal, and earn, no faster than one",
    "won": "You have beaten this keeper: %s",
    "first_win": "A first win always brings the %s",
    "first_win.pair": "A first win always brings one of two trophies, at random: the %s or the %s",
    "after_win": "Cheaper with the keeper's loot: %s",
    "made_from": "Made from the keeper's loot: %s",
    "hollow_acre.leave": "The Grey Mist in the lych gate takes you home at any time, in or out of a fight",
    "spindle_loft.leave": "The Grey Mist in the needle's eye beside the arrival takes you home at any time, in or out of "
                          "a fight",
    "glacier_hall.leave": "The Grey Mist in the arch of blue ice beside the arrival takes you home at any time, in or out "
                          "of a fight; when he falls, Grey Mist stands in the middle of the lake too",
    "cinder_kiln.leave": "The Grey Mist in the arch of kiln brick behind the arrival takes you home at any time, in or "
                         "out of a fight; when he falls, Grey Mist stands in the middle of the bowl too",
    "grave_goods": "Dying in a lair costs nothing: you respawn with everything you carried, worn and held as it was, "
                   "and all your experience",
    "owed": "A share of the keeper's spoils is kept for you",
    "share": "Damage %s, healing %s, warded %s, objectives %s",
    "share.qualifies": "This fight: your part already earns a share",
    "share.not_yet": "This fight: not yet enough for a share",
    "share.none": "This fight: you have no part in it yet",
    "unknown_count": "Lairs yet to be found: %s",
}

# The bestiary (guide/encyclopedia/Bestiary.java), after the lair dossiers on the Expeditions page: every Jugcraft
# creature the player has defeated or been defeated by, with the numbers the game itself gives it, and their own tally.
BESTIARY = {
    "heading": "Field notes",
    "intro": "Creatures you have fought, with the game's own numbers. Ones you have not met stay unwritten.",
    "stats": "Health %s, attack %s, armour %s",
    "stats.no_attack": "Health %s, armour %s; it does not strike in melee",
    "record": "You have defeated it %s times; it has defeated you %s times",
    "more": "%s more creatures you have fought are not listed here",
}

GENERAL = {
    "absent": "Not available on this server: %s",
    "trade.town": "Buy at the town's %3$s: %1$s × %2$s for %4$s Jugs",
    "more": "There is more than one page can hold",
    "unreadable": "This page could not be read; the server's log says why",
    "codex_unavailable": "The codex could not open that entry",
    "discover.research": "Your research",
    "discover.stage": "Your stage",
    "row.step": "%s (%s): %s",
    "row.edge": "%s: %s",
    "state.none": "Step",
    "edge.none": "Note",
    "edge.optional_boss.step": "Optional: a boss fight",
    "link.codex": "Codex: %s",
    "link.composer": "Open the Spell Composer",
    "link.journal": "Open the Concordance Journal (%s)",
    "link.page.expedition": "Go to Expeditions",
    "grid.shaped": "Crafting table",
    "grid.shapeless": "Crafting, in any order",
    "grid.stonecutter": "Stonecutter",
    "grid.narration": "%s: %s; makes %s %s",
    "keys.jugcraft": "Jugcraft's keys",
    "keys.vanilla": "Minecraft's keys on the way",
    "keys.line": "%s: %s. %s",
    "keys.unbound": "not bound",
    "keys.shared": "Also bound to: %s",
    "keys.examine": "Examine a specimen: %s and %s together",
    "keys.cast": "Cast with a wand in your main hand: %s, %s, %s",
    "keys.inventory": "Open your inventory (%s): the scroll button there opens this encyclopedia",
    "keys.change": "Change any of them in Options, Controls, Key Binds",
}

CLIENT = {
    "key.jugcraft.encyclopedia": "Jugcraft Encyclopedia",
    "screen.jugcraft.encyclopedia.title": "Jugcraft Encyclopedia",
    "screen.jugcraft.encyclopedia.refresh": "Refresh",
    "screen.jugcraft.encyclopedia.loading": "Asking the server...",
    "screen.jugcraft.encyclopedia.empty": "Nothing to show yet",
    "screen.jugcraft.encyclopedia.unavailable": "This server sends no encyclopedia pages: only your keys are shown",
    "screen.jugcraft.encyclopedia.help": "Left and Right change page; Tab moves between controls; Page Up and Page Down "
                                         "scroll; Backspace goes back a page; F6 asks again; X shows exact figures; Esc "
                                         "or %s closes.",
    "screen.jugcraft.encyclopedia.page": "%s (%s of %s)",
    "screen.jugcraft.encyclopedia.open": "Jugcraft Encyclopedia",
    "screen.jugcraft.encyclopedia.open.tooltip": "Jugcraft Encyclopedia (%s)",
    "screen.jugcraft.encyclopedia.open.tooltip_unbound": "Jugcraft Encyclopedia",
}


def lang_entries(lang):
    """Every word above, under its key."""
    for page, name in PAGES.items():
        lang[f"encyclopedia.{MOD}.page.{page}"] = name
    for state, name in STATES.items():
        lang[f"encyclopedia.{MOD}.state.{state}"] = name
    for edge, name in EDGES.items():
        lang[f"encyclopedia.{MOD}.edge.{edge}"] = name
    for prefix, table in (("route", ROUTE), ("spellcraft", SPELLCRAFT), ("inspect", INSPECT), ("dossier", DOSSIER),
                          ("bestiary", BESTIARY)):
        for key, text in table.items():
            lang[f"encyclopedia.{MOD}.{prefix}.{key}"] = text
    for key, text in GENERAL.items():
        lang[f"encyclopedia.{MOD}.{key}"] = text
    for key, text in KEY_HELP.items():
        lang[f"encyclopedia.{MOD}.key.{key}"] = text
    lang.update(CLIENT)
    import reading_ui
    reading_ui.lang_entries(lang)


# ---------------------------------------------------------------- the record's table

def recipe_inputs(recipe):
    """A recipe's ingredients as written in its file ("2 stick, 1 copper ingot, 1 #concordance/luminous_matter")."""
    path = DATA / "recipe" / f"{recipe.split(':', 1)[1]}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    counts = {}
    if "pattern" in data:
        for row in data["pattern"]:
            for symbol in row:
                if symbol != " ":
                    ingredient = data["key"][symbol]
                    counts[ingredient] = counts.get(ingredient, 0) + 1
    elif "ingredients" in data:
        for ingredient in data["ingredients"]:
            counts[ingredient] = counts.get(ingredient, 0) + 1
    elif "ingredient" in data:
        counts[data["ingredient"]] = 1
    return ", ".join(f"{count} {_short(ingredient)}" for ingredient, count in counts.items())


def _short(ingredient):
    tag = ingredient.startswith("#")
    name = ingredient.lstrip("#").split(":", 1)[-1].replace("_", " ")
    return f"#{name}" if tag else name


def route_table():
    """The record's route table: one row per step, from FIRST_ROUTE and the recipe files on disk."""
    lines = ["| Step | Matters | Done when | After | Recipes shown (ingredients on disk) | Trade | Codex | Tests |",
             "|---|---|---|---|---|---|---|---|"]
    for step in FIRST_ROUTE:
        recipes = "<br>".join(f"{edge}: `{recipe}` ({recipe_inputs(recipe)})" for edge, recipe in step["recipes"])
        named = step.get("named", [])
        if named:
            recipes += ("<br>" if recipes else "") + "the sets, named when each of these exists: " + ", ".join(
                f"`{recipe}`" for _edge, recipe in named)
        recipes = recipes or "-"
        trades = "<br>".join(f"{shop}: `{item}`" for shop, item in step["trades"]) or "-"
        codex = "<br>".join(f"`{entry}`" + (f" ({research.split(':')[1]} {state.lower()})" if research else "")
                            for entry, research, state in step["codex"]) or "-"
        tests = ", ".join(f"`{test}`" for test in step["tests"])
        after = ", ".join(step["after"]) or "-"
        lines.append(f"| {step['id']} | {EDGES[step['edge']]} | {step['done']} | {after} | {recipes} | {trades} | {codex} | {tests} |")
    return lines


if __name__ == "__main__":
    print("\n".join(route_table()))
