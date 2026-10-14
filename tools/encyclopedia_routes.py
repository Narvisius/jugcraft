"""The Jugcraft Encyclopedia's first slice (docs/features/jugcraft-encyclopedia.md): its two entry points (a remappable
key and an inventory button, no item) and a home with the technology and magic routes.

generate_material_data.py writes the routes to assets/jugcraft/encyclopedia/en_us.json; the client screen
(client/EncyclopediaScreen.java) reads it. Nothing here is a second copy of the guides: the technology route's milestones
and chapters are the Engineer's Handbook's own (tools/handbook.py), and the magic route's chapters are the Arcane
Concordance codex's categories (tools/concordance.py), so both stay in step with the game. Only the overviews are
written here, and they describe what the game has now, not plans.

Format: {"intro": [paragraphs], "routes": [{"id", "title", "icon", "accent" (RRGGBB), "summary": [paragraphs],
"milestones": [entry], "chapters": [entry]}]}, where an entry is {"title", "icon", "text" (optional), "link"} and a link
is {"type": "handbook", "chapter", "page"} (indices into the handbook), {"type": "codex", "category"} (a codex category
id) or {"type": "journal"}. tools/check_mod_data.py check_encyclopedia() checks every item and link.
"""
MOD = "jugcraft"

LANG = {
    "key.jugcraft.encyclopedia": "Jugcraft Encyclopedia",
    "screen.jugcraft.routes.title": "Jugcraft Encyclopedia",
    # The inventory button: its message is what the narrator reads (and what tests click); it shows a book icon.
    "screen.jugcraft.routes.open": "Technology & Magic Guides",
    "screen.jugcraft.routes.open.tooltip": "Open the Jugcraft Encyclopedia",
    "screen.jugcraft.routes.open_route": "Open %s",
    "screen.jugcraft.routes.home": "Home",
    "screen.jugcraft.routes.milestones": "Milestones",
    "screen.jugcraft.routes.chapters": "Chapters",
    "screen.jugcraft.routes.missing": "The Encyclopedia's content is missing.",
    "screen.jugcraft.routes.help.home": "Click a route, or Tab to it and press Enter. Esc closes.",
    "screen.jugcraft.routes.help.route": "Up/Down and Enter open a link. Backspace goes home; Esc closes.",
    "screen.jugcraft.routes.opens.handbook": "Opens the Engineer's Handbook here",
    "screen.jugcraft.routes.opens.codex": "Opens the Arcane Concordance codex here",
    "screen.jugcraft.routes.opens.journal": "Opens your Concordance Journal",
    "screen.jugcraft.routes.unavailable": "The codex is not loaded in this world",
}

INTRO = [
    "Jugcraft has two great routes: machinery and magic. Each begins with things you can find in the world, so follow "
    "one, or both, in any order.",
    "Pick a route for its milestones and chapters. This shows what this version of Jugcraft has.",
]

TECHNOLOGY = {
    "id": "technology",
    "title": "Machinery and Technology",
    "icon": f"{MOD}:steel_ingot",
    "accent": "B08A3A",
    "summary": [
        "Machines build in stages, each needing parts from the ones before: bronze and your first power, the "
        "workshop's plates, wires and circuits, shafts and logistics, then steel, oil, chemistry and electronics.",
        "Beside that road are specialties to take up when you like: ore processing, fluids, storage, farming, "
        "renewables and rocketry.",
        "Each milestone opens the Engineer's Handbook at that stage; the chapters hold every machine's recipe and use.",
    ],
}

MAGIC = {
    "id": "magic",
    "title": "Magic: the Arcane Concordance",
    "icon": f"{MOD}:initiate_wand",
    "accent": "8A6FD0",
    "summary": [
        "The Arcane Concordance is research you earn by examining the world, and spells cast from your own Focus. It "
        "rises through five stages, Initiate to Architect, to a Concord Spire that changes the land round it.",
        "Begin by sneaking and using something that glows: an amethyst shard, glow lichen or glow berries. Then take "
        "research in any order: cultivation, exploration and combat, and crafting and infrastructure each lead to a "
        "Spire.",
        "The milestones and chapters open the codex, which shows each tradition's entries as you unlock them. Your "
        "Concordance Journal says what each step still needs.",
    ],
}

# The magic route's milestones (docs/ARCANE_CONCORDANCE_GUIDE.md, "Your first spell" and the stages): icon, label and
# the codex category that explains the step.
MAGIC_MILESTONES = [
    ("minecraft:amethyst_shard", "Examine a glow", "radiance"),
    (f"{MOD}:lampwright_bench", "Understand First Light", "radiance"),
    (f"{MOD}:initiate_wand", "Initiate's Wand", "invocations"),
    ("minecraft:compass", "Adept", "foundations"),
    (f"{MOD}:conclave_lectern", "Master", "foundations"),
    (f"{MOD}:spire_heart", "Architect: a Spire", "wonders"),
]

# How many page titles a technology chapter lists as its description.
CHAPTER_PREVIEW = 3


def codex_link(category):
    return {"type": "codex", "category": f"{MOD}:{category}"}


def technology(book):
    """The handbook's Progression stages as milestones (each opens its stage's page) and its chapters, in its order."""
    chapters = book["chapters"]
    progression = chapters[0]
    overview = progression["pages"][0]
    milestones = []
    for index, step in enumerate(overview["steps"]):
        page = progression["pages"][index + 1]
        # handbook.progression_pages() titles each stage's page "<n>. <stage>".
        assert page["title"] == f"{index + 1}. {step['label']}", f"handbook stage page {page['title']} != {step['label']}"
        milestones.append({"title": step["label"], "icon": step["item"],
                           "link": {"type": "handbook", "chapter": 0, "page": index + 1}})
    entries = []
    for index, chapter in enumerate(chapters):
        titles = [page["title"] for page in chapter["pages"]]
        text = ", ".join(titles[:CHAPTER_PREVIEW]) + (" and more" if len(titles) > CHAPTER_PREVIEW else "")
        entries.append({"title": chapter["title"], "icon": chapter["icon"], "text": text,
                        "link": {"type": "handbook", "chapter": index, "page": 0}})
    return {**TECHNOLOGY, "milestones": milestones, "chapters": entries}


def magic():
    """The first spell and the stages as milestones; the journal, then every codex category in the codex's order."""
    import concordance
    milestones = [{"title": label, "icon": icon, "link": codex_link(category)}
                  for icon, label, category in MAGIC_MILESTONES]
    entries = [{"title": "Your Concordance Journal", "icon": "minecraft:writable_book",
                "text": "What your research and stage still need", "link": {"type": "journal"}}]
    for key, info in sorted(concordance.CATEGORIES.items(), key=lambda item: item[1]["sort"]):
        entries.append({"title": info["name"], "icon": info["icon"], "text": info["description"],
                        "link": codex_link(key)})
    return {**MAGIC, "milestones": milestones, "chapters": entries}


def build(book):
    """The Encyclopedia's routes; book is tools/handbook.py's build()."""
    return {"intro": INTRO, "routes": [technology(book), magic()]}


def lang(entries):
    entries.update(LANG)
