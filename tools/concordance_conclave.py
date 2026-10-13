"""Roadmap step 23: factions and the Starbound Conclave (docs/features/arcane-concordance-conclave.md).

The Starbound Conclave is the fellowship of the Concordance's traditions, which are its factions: every research entry
already belongs to one (Lampwrights, Greenwardens, Alembists, Starwatchers, Circlewrights, Spiritbinders, Crimson Vigil,
Runesmiths, Balancewrights, Hexweavers, Dreamwalkers, Hearthbinders). It replaces nothing: parties stay Jugcraft's
parties, the town stays the town, Jugs stay the only coin. A player who understands First Light swears its oath at a
Conclave Lectern (or with /jugcraft concordance conclave join) and earns RENOWN, which is standing and never a currency,
from four kinds of contribution, each bounded (Java: conclave/Conclave.java):

- research: each state a research entry reaches, once (1, 2, 5 and 10);
- commissions: the Conclave's posted tasks, each once a week, worth less each time and nothing after its third;
- teaching: another player advanced an entry by reading your notes, once per entry and learner, four per learner;
- projects: each stage you contributed to, and the whole project, once.

Ranks ask for renown, breadth (traditions with at least TRADITION_RENOWN) and variety (kinds) together, so no single
repeated deed reaches any. A member's obligation is to contribute something each week; a lapsed member keeps their
renown but takes no tier II or III commission and begins no project until they contribute again. Projects are personal
or a party's (Jugcraft's own parties); every stage has a solo alternative to its cooperation rule (enough contributors,
or contributions on enough different days). The definitions are data (data/jugcraft/concordance/commission and
concordance/project, read by Java's concordance/conclave). tools/concordance.py merges these tables into its own;
tools/check_mod_data.py checks the numbers the Java repeats and that a solo player can reach every rank.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# Java: conclave/Conclave.java.
WEEK = 168_000
OBLIGATION_TICKS = WEEK
RESEARCH_RENOWN = {"encountered": 1, "observed": 2, "understood": 5, "mastered": 10}
TEACHING_RENOWN = 3
TEACHING_PER_LEARNER = 4
TRADITION_RENOWN = 5
DIMINISHING = [100, 50, 25]
# Java: conclave/Rank.java: (id, renown, traditions, kinds, commission tier, may begin projects).
RANKS = [("aspirant", 0, 0, 0, 1, False), ("fellow", 25, 2, 2, 2, True), ("companion", 80, 4, 2, 3, True),
         ("luminary", 160, 5, 3, 3, True), ("starbound", 260, 6, 3, 3, True)]
RANK_NAMES = {"aspirant": "Aspirant", "fellow": "Fellow", "companion": "Companion", "luminary": "Luminary",
              "starbound": "Starbound"}
KINDS = ["research", "commission", "teaching", "project"]
# Java: conclave/ConclaveParser.java.
MAX_TIER = 3
MAX_COUNT = 256
MAX_RENOWN = 40
MAX_REWARD = 16
MAX_STAGES = 5
MAX_REQUIREMENTS = 6
MAX_CONTRIBUTORS = 4
MAX_DAYS = 7
MAX_PRACTICE = 8
MAX_PROJECT_RENOWN = 100

TRADITIONS = {"lampwrights": "Lampwrights", "greenwardens": "Greenwardens", "alembists": "Alembists",
              "starwatchers": "Starwatchers", "circlewrights": "Circlewrights", "spiritbinders": "Spiritbinders",
              "crimson_vigil": "Crimson Vigil", "runesmiths": "Runesmiths", "balancewrights": "Balancewrights",
              "hexweavers": "Hexweavers", "dreamwalkers": "Dreamwalkers", "hearthbinders": "Hearthbinders"}

# The practices commissions and projects may ask for (each a Java ACTIVITY), and how they read.
ACTIVITIES = {"jugcraft:observation": "an observation of the sky", "jugcraft:ritual": "a ritual completed",
              "jugcraft:worker_service": "a bound worker's service", "jugcraft:assay": "a dissolving at the scale",
              "jugcraft:artifice": "work at the Artificer's Bench", "jugcraft:dream": "a dream come back from",
              "jugcraft:sympathy": "a curse cast", "jugcraft:cultivation": "a living device tended",
              "jugcraft:alchemy": "a mixture bottled"}

# ------------------------------------------------------------------------------------------------- commissions

# key: (name, tradition, tier, ask, renown, reward, reward count); ask is ("deliver", item, count) or ("practice", activity).
COMMISSIONS = {
    "lamp_oil": ("Oil for the Lamps", "lampwrights", 1, ("deliver", "minecraft:glowstone_dust", 16), 4, "minecraft:amethyst_shard", 2),
    "seed_store": ("The Seed Store", "greenwardens", 1, ("deliver", "minecraft:wheat_seeds", 32), 4, "minecraft:emerald", 2),
    "clean_vials": ("Clean Vials", "alembists", 1, ("deliver", "minecraft:glass_bottle", 12), 4, "minecraft:experience_bottle", 2),
    "first_watch": ("The First Watch", "starwatchers", 1, ("practice", "jugcraft:observation"), 4, "minecraft:amethyst_shard", 2),
    "circle_witness": ("Witness to the Circle", "circlewrights", 2, ("practice", "jugcraft:ritual"), 8, "minecraft:experience_bottle", 4),
    "bound_service": ("A Service Rendered", "spiritbinders", 2, ("practice", "jugcraft:worker_service"), 8, "minecraft:emerald", 3),
    "fair_weight": ("A Fair Weight", "balancewrights", 2, ("practice", "jugcraft:assay"), 8, "minecraft:emerald", 3),
    "vigil_gift": ("A Gift for the Vigil", "crimson_vigil", 2, ("deliver", "minecraft:sweet_berries", 24), 8, "minecraft:emerald", 3),
    "runework": ("Runework for the Archive", "runesmiths", 3, ("practice", "jugcraft:artifice"), 12, "minecraft:diamond", 1),
    "dream_record": ("A Dream Recorded", "dreamwalkers", 3, ("practice", "jugcraft:dream"), 12, "minecraft:ender_pearl", 2),
    "hex_ledger": ("The Hex Ledger", "hexweavers", 3, ("practice", "jugcraft:sympathy"), 12, "minecraft:emerald", 4),
    "garden_census": ("The Garden Census", "greenwardens", 3, ("practice", "jugcraft:cultivation"), 12, "minecraft:diamond", 1),
}

# ------------------------------------------------------------------------------------------------- projects

# key: name, tradition, renown, reward, stages; a stage is (id, name, renown, contributors, days, requirements), a
# requirement (id, type, target, count).
PROJECTS = {
    "starward_chart": {
        "name": "The Starward Chart", "tradition": "starwatchers", "renown": 30, "reward": ("minecraft:diamond", 2),
        "description": "chart the starward sky from the Conclave's lenses and vigils",
        "stages": [
            ("lenses", "Grinding the Lenses", 8, 2, 2, [("panes", "deliver", "minecraft:glass_pane", 32),
                                                      ("amethyst", "deliver", "minecraft:amethyst_shard", 8),
                                                      ("attuned", "research", "jugcraft:celestial_attunement", 1)]),
            ("vigils", "Keeping the Vigils", 10, 2, 3, [("watches", "practice", "jugcraft:observation", 3),
                                                       ("circle", "practice", "jugcraft:ritual", 1)]),
            ("chart", "Drawing the Chart", 12, 3, 3, [("paper", "deliver", "minecraft:paper", 16),
                                                     ("gold", "deliver", "minecraft:gold_ingot", 4),
                                                     ("compass", "deliver", "minecraft:compass", 1)]),
        ]},
    "concordance_archive": {
        "name": "The Concordance Archive", "tradition": "lampwrights", "renown": 30, "reward": ("minecraft:experience_bottle", 8),
        "description": "gather the Concordance's learning into one archive",
        "stages": [
            ("shelves", "Raising the Shelves", 8, 2, 2, [("books", "deliver", "minecraft:book", 12),
                                                        ("planks", "deliver", "minecraft:oak_planks", 32)]),
            ("readings", "The Readings", 10, 2, 2, [("first_light", "research", "jugcraft:first_light", 1),
                                                   ("alembic", "research", "jugcraft:alembic_arts", 1),
                                                   ("mixtures", "practice", "jugcraft:alchemy", 2)]),
            ("catalogue", "The Catalogue", 12, 3, 3, [("paper", "deliver", "minecraft:paper", 16),
                                                     ("ink", "deliver", "minecraft:ink_sac", 8),
                                                     ("quills", "deliver", "minecraft:feather", 4)]),
        ]},
}

# ------------------------------------------------------------------------------------------------- words

REASONS = {
    "disabled": "the Arcane Concordance is disabled on this server",
    "unknown": "you have not understood First Light (the codex says how)",
    "already": "that is already done",
    "not_member": "you have not sworn the Conclave's oath (use a Conclave Lectern)",
    "nothing": "that brings no renown",
    "rank": "your rank does not reach that yet",
    "lapsed": "you have not contributed this week: contribute anything to be in good standing again",
    "exhausted": "the Conclave asks this of you no more",
    "cooldown": "you fulfilled this commission this week already",
    "not_practised": "you have not carried that practice through this week",
    "self": "you cannot teach yourself",
    "learner_limit": "that learner has taught you all the renown they can",
    "not_leader": "only your party's leader can begin its project",
    "busy": "a project is already under way there",
    "complete": "that project is finished",
    "full": "that part of the stage is already full",
    "not_needed": "the stage does not need that",
    "unknown_commission": "there is no such commission",
    "items": "hold the items the commission asks for",
    "unknown_project": "there is no such project",
    "no_party": "you are in no party",
    "not_yours": "this lectern serves someone else's project",
    "no_commission": "no commission you can take asks for that (sneak to give it to this lectern's project)",
}

MESSAGES = {
    "conclave.refused": "The Conclave refuses: %s",
    "conclave.sworn": "You have sworn the Starbound Oath: you are an Aspirant of the Conclave",
    "conclave.renown": "+%s renown: %s",
    "conclave.rank": "You are now a %s of the Starbound Conclave",
    "conclave.started": "%s is begun",
    "conclave.contributed": "%s given (%s) to %s",
    "conclave.stage_complete": "A stage of %s is finished",
    "conclave.project_complete": "%s is finished!",
    "conclave.mode.personal": "This lectern now serves your own project",
    "conclave.mode.party": "This lectern now serves your party's project",
    "conclave.not_member": "You have not sworn the Conclave's oath",
    "conclave.status": "%s of the Conclave: %s renown, %s traditions, %s kinds of work",
    "conclave.next": "%s needs %s renown, %s traditions with %s renown each and %s kinds of work",
    "conclave.obligation_met": "In good standing for %s more days",
    "conclave.lapsed": "Lapsed: contribute anything to be in good standing again",
    "conclave.commission": "%s (tier %s): %s; %s",
    "conclave.ask_deliver": "bring %s %s",
    "conclave.ask_practice": "%s this week",
    "conclave.no_project": "No project is under way here",
    "conclave.project_done": "%s is finished",
    "conclave.project": "%s, stage %s of %s: %s",
    "conclave.requirement": "  %s %s: %s / %s",
    "conclave.cooperation": "  contributors %s / %s, or days %s / %s",
}
TOOLTIPS = {}
DEVICE_TOOLTIPS = {
    "conclave_lectern": "Swear the Conclave's oath here; fulfil commissions with what you hold; sneak to give to its project.",
}

ITEMS = {}
BLOCKS = {"conclave_lectern": {"name": "Conclave Lectern"}}
RESEARCH = {}

ADVANCEMENTS = {
    "conclave_oath": ("The Starbound Oath", "Swear the Starbound Conclave's oath", "task", rid("conclave_lectern"),
                      "concordance_first_light_understood"),
    "conclave_rank_fellow": ("Fellow of the Conclave", "Reach the rank of Fellow", "task", "minecraft:paper", "conclave_oath"),
    "conclave_rank_companion": ("Companion of the Conclave", "Reach the rank of Companion", "goal", "minecraft:compass",
                                "conclave_rank_fellow"),
    "conclave_rank_luminary": ("Luminary of the Conclave", "Reach the rank of Luminary", "goal", "minecraft:glowstone_dust",
                               "conclave_rank_companion"),
    "conclave_rank_starbound": ("Starbound", "Reach the Conclave's highest rank", "challenge", "minecraft:nether_star",
                                "conclave_rank_luminary"),
    **{f"conclave_project_{key}": (info["name"], f"Finish {info['name']}, alone or with your party", "goal", "minecraft:book",
                                   "conclave_oath") for key, info in PROJECTS.items()},
}


def lang_entries(lang):
    for key, text in REASONS.items():
        lang[f"compose.{MOD}.conclave.reason.{key}"] = text
    for key, name in RANK_NAMES.items():
        lang[f"compose.{MOD}.conclave.rank.{key}"] = name
    for key in KINDS:
        lang[f"compose.{MOD}.conclave.kind.{key}"] = {"research": "research", "commission": "a commission",
                                                     "teaching": "teaching", "project": "a project"}[key]
    for key, name in TRADITIONS.items():
        lang[f"compose.{MOD}.conclave.tradition.{key}"] = name
    for key, info in COMMISSIONS.items():
        lang[f"compose.{MOD}.conclave.commission.{key}"] = info[0]
    for key, info in PROJECTS.items():
        lang[f"compose.{MOD}.conclave.project.{key}"] = info["name"]
        for stage in info["stages"]:
            lang[f"compose.{MOD}.conclave.stage.{key}.{stage[0]}"] = stage[1]
    for kind, text in (("deliver", "delivered"), ("practice", "practised"), ("research", "understood")):
        lang[f"compose.{MOD}.conclave.requirement.{kind}"] = text
    for activity, text in ACTIVITIES.items():
        lang[f"compose.{MOD}.conclave.activity.{activity.split(':')[1]}"] = text
    lang[f"compose.{MOD}.conclave.open"] = "open to you now"
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    for key, (title, description, _frame, _icon, _parent) in ADVANCEMENTS.items():
        lang[f"advancements.{MOD}.{key}.title"] = title
        lang[f"advancements.{MOD}.{key}.description"] = description


# ------------------------------------------------------------------------------------------------- codex

def _amount(count, item):
    """"16 glowstone dust", "2 amethyst shards", "1 diamond", "1 compass", "4 compasses" for the codex."""
    name = item.split(":")[1].replace("_", " ")
    if count != 1 and not name.endswith(("dust", "seeds", "berries", "paper", "planks", "meal")):
        name += "es" if name.endswith(("s", "x", "ch", "sh")) else "s"
    return f"{count} {name}"


def _ask(ask):
    if ask[0] == "deliver":
        return f"bring {_amount(ask[2], ask[1])}"
    return f"{ACTIVITIES[ask[1]]} in the last week"


def _requirement(requirement):
    _id, kind, target, count = requirement
    if kind == "deliver":
        return _amount(count, target)
    if kind == "practice":
        return f"{ACTIVITIES[target]} ({count})"
    return f"a contributor who understands {target.split(':')[1].replace('_', ' ').title()}"


def codex():
    rank_lines = [f"- **{RANK_NAMES[key]}**: {renown} renown, {traditions} traditions with at least {TRADITION_RENOWN} "
                  f"renown each, {kinds} kinds of work; commissions up to tier {tier}"
                  + ("; may begin projects" if projects else "")
                  for key, renown, traditions, kinds, tier, projects in RANKS]
    commission_lines = [f"- **{name}** ({TRADITIONS[tradition]}, tier {tier}): {_ask(ask)}; {renown} renown, then "
                        f"{renown // 2}, then {max(1, renown // 4)}; pays {_amount(count, reward)}"
                        for name, tradition, tier, ask, renown, reward, count in COMMISSIONS.values()]
    pages = [
        ("text", "The Starbound Conclave",
         "The Conclave is the fellowship of the Concordance's traditions, and its factions are those traditions: every "
         "research entry belongs to one. Once you understand First Light, use a **Conclave Lectern** (or **/jugcraft "
         "concordance conclave join**) to swear its oath. It replaces nothing: your party stays your party and Jugs stay "
         "the only coin. Renown is standing, never spent."),
        ("crafting_recipe", "Renown",
         f"Renown comes from four kinds of work, each bounded so no one repeated deed can carry you: **research** "
         f"(each state an entry reaches, once: {', '.join(str(v) for v in RESEARCH_RENOWN.values())}), **commissions** "
         f"(each once a week, worth less each time, nothing after its third), **teaching** (someone advanced an entry by "
         f"reading your notes: {TEACHING_RENOWN} renown, once per entry and learner, {TEACHING_PER_LEARNER} per learner) "
         f"and **projects** (each stage you helped, and the whole project, once).", rid("conclave_lectern")),
        ("text", "Ranks", "A rank asks for renown, breadth and variety together.\\\n" + "\\\n".join(rank_lines)),
        ("text", "Obligations",
         "A member contributes something each week. A lapsed member keeps every point of renown but takes no tier II or III "
         "commission and begins no project until they contribute again; any contribution restores them. **/jugcraft "
         "concordance conclave** shows your standing, what the next rank needs and your obligation."),
        ("text", "Commissions",
         "Hold what a commission asks for and use a lectern, or **/jugcraft concordance conclave fulfil <commission>**; "
         "practice commissions need only the practice carried through this week. **/jugcraft concordance conclave "
         "commissions** lists them and whether each is open to you.\\\n" + "\\\n".join(commission_lines)),
    ]
    for key, info in PROJECTS.items():
        stage_lines = []
        for stage_id, name, renown, contributors, days, requirements in info["stages"]:
            stage_lines.append(f"- **{name}** ({renown} renown each): " + "; ".join(_requirement(r) for r in requirements)
                               + f"; {contributors} contributors, or contributions on {days} different days")
        pages.append(("text", info["name"],
                      f"A project to {info['description']}, for one player or a whole party. Finished, it brings every "
                      f"contributor {info['renown']} renown and {_amount(info['reward'][1], info['reward'][0])}.\\\n"
                      + "\\\n".join(stage_lines)))
    pages.append(("text", "Projects, Alone or Together",
                  "From Fellow, begin a project for yourself (**/jugcraft concordance conclave project start <project> "
                  "personal**) or, as your party's leader, for your party (**... party**). Your own projects take only your "
                  "work; your party's take any member's. Every stage that asks for several contributors also accepts the "
                  "solo way: contributions on that many different days. Together a party finishes sooner, and every member "
                  "who helped shares the achievement and the reward. Sneak and use a lectern with items to give them; use "
                  "it with an empty hand to present what you understand; practices count by themselves. **/jugcraft "
                  "concordance conclave project** shows progress anywhere."))
    pages.append(("text", "Lecterns",
                  "A lectern serves the project of whoever placed it: their own, or (sneak and use it with an empty hand to "
                  "switch) their party's. Anyone may swear the oath, see their standing and fulfil commissions at any "
                  "lectern. Projects are kept by the world, not the lectern: breaking one loses nothing."))
    return {("conclave", "starbound"): {"name": "The Starbound Conclave", "x": 0, "y": 0, "icon": rid("conclave_lectern"),
                                        "condition": None, "description": "Renown, ranks, commissions and shared projects",
                                        "pages": pages}}


CATEGORY = {"conclave": {"name": "The Starbound Conclave", "icon": rid("conclave_lectern"), "sort": 14,
                         "description": "The fellowship of the traditions: renown, commissions and projects"}}


# ------------------------------------------------------------------------------------------------- data and assets

RECIPES = {
    "conclave_lectern": {"pattern": ["GBG", " L ", "SSS"], "key": {"G": "minecraft:gold_ingot", "B": "minecraft:book",
                                                                   "L": "minecraft:dark_oak_log", "S": "minecraft:dark_oak_slab"}},
}


def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid("conclave_lectern"))


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    import concordance_conclave_models as models
    write(assets / "blockstates" / "conclave_lectern.json", {"variants": {
        f"facing={facing}": {"model": rid("block/conclave_lectern"), **({"y": rotation} if rotation else {})}
        for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(assets / "models" / "block" / "conclave_lectern.json", models.lectern_model())
    write(assets / "items" / "conclave_lectern.json", {"model": {"type": "minecraft:model", "model": rid("item/conclave_lectern")}})
    write(assets / "models" / "item" / "conclave_lectern.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/conclave_lectern")}})
    write(data / "loot_table" / "blocks" / "conclave_lectern.json", self_drop("conclave_lectern"))
    for name, entry in RECIPES.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition("concordance")}, **{
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": entry["pattern"], "key": entry["key"],
            "result": {"id": rid(name), "count": 1}}))
    for key, (_title, _description, frame, icon, parent) in ADVANCEMENTS.items():
        write(data / "advancement" / f"{key}.json", {
            "parent": rid(parent),
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.{MOD}.{key}.title"},
                        "description": {"translate": f"advancements.{MOD}.{key}.description"}, "frame": frame,
                        "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})


def commission_json(info):
    _name, tradition, tier, ask, renown, reward, count = info
    entry = {"schema": 1, "tradition": tradition, "tier": tier}
    if ask[0] == "deliver":
        entry.update({"deliver": ask[1], "count": ask[2]})
    else:
        entry["practice"] = ask[1]
    entry.update({"renown": renown, "reward": reward, "reward_count": count})
    return entry


def project_json(info):
    stages = []
    for stage_id, _name, renown, contributors, days, requirements in info["stages"]:
        entries = []
        for requirement_id, kind, target, count in requirements:
            entry = {"id": requirement_id, kind: target}
            if kind != "research":
                entry["count"] = count
            entries.append(entry)
        stages.append({"id": stage_id, "renown": renown, "contributors": contributors, "days": days, "requirements": entries})
    return {"schema": 1, "tradition": info["tradition"], "renown": info["renown"], "reward": info["reward"][0],
            "reward_count": info["reward"][1], "stages": stages}


def write_data(write, data):
    for key, info in COMMISSIONS.items():
        write(data / "concordance" / "commission" / f"{key}.json", commission_json(info))
    for key, info in PROJECTS.items():
        write(data / "concordance" / "project" / f"{key}.json", project_json(info))


# ------------------------------------------------------------------------------------------------- the solo route

def commission_total(renown):
    """What one commission can bring in all, by DIMINISHING (Java: Conclave.commissionRenown)."""
    return sum(max(1, renown * percent // 100) for percent in DIMINISHING)


def solo_route(research):
    """The most renown a player alone can earn by tradition and kind, and the ranks it reaches, without teaching.

    research: {key: research definition from tools/concordance.py} (its tradition and the states it has). Returns
    (renown by tradition, kinds, ranks reached)."""
    by_tradition = {}
    for info in research.values():
        tradition = info.get("tradition", "")
        points = sum(RESEARCH_RENOWN[state] for state in info["states"] if state in RESEARCH_RENOWN)
        by_tradition[tradition] = by_tradition.get(tradition, 0) + points
    for _name, tradition, _tier, _ask, renown, _reward, _count in COMMISSIONS.values():
        by_tradition[tradition] = by_tradition.get(tradition, 0) + commission_total(renown)
    for info in PROJECTS.values():
        points = info["renown"] + sum(stage[2] for stage in info["stages"])
        by_tradition[info["tradition"]] = by_tradition.get(info["tradition"], 0) + points
    total = sum(by_tradition.values())
    breadth = sum(1 for points in by_tradition.values() if points >= TRADITION_RENOWN)
    kinds = 3  # research, commissions and projects: teaching needs someone else
    reached = [key for key, renown, traditions, need_kinds, _tier, _projects in RANKS
               if total >= renown and breadth >= traditions and kinds >= need_kinds]
    return by_tradition, kinds, reached
