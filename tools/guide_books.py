"""The two Drone Tower guide books (drone/GuideBooks.java, client/GuideBookScreen.java): the Field Manual, crafted
from a book and a tier 1 drone, and the Creative Quick Start, given once to a player who joins in creative mode.

Each page is a heading, a short body and, on most pages, a screenshot from the game. The screen draws dark ink
on light paper at a comfortable width, so pages stay readable; the text is translatable (lang keys below).
Screenshots live in textures/gui/guide/<name>.png (taken by the GuideScreenshotGameTests client test, cropped to
16:9 by tools/guide_shots.py). Keep each body under ~300 characters so it fits under its picture.
"""
import json
from pathlib import Path

MOD = "jugcraft"
ROOT = Path(__file__).resolve().parent.parent
SHOTS = ROOT / "src" / "main" / "resources" / "assets" / MOD / "textures" / "gui" / "guide"

# (heading, body, screenshot name or None)
FIELD_MANUAL = [
    ("Drone Tower Field Manual",
     "A Drone Tower is a depot that grows. Its drones carry blocks and build for you, and every tier you add holds "
     "more drones and stronger ones. Turn the pages to set one up.", "tier9"),
    ("1. Pick the spot",
     "Craft a Drone Tower Core (steel blocks, processors, advanced circuits and a Drone Depot Terminal). Hold it and "
     "look at the ground: an amber ghost shows the control building, 104 across; shift+scroll shows each tier, up to "
     "260 blocks tall. Leave room.", "core_preview"),
    ("2. Set the core down",
     "Place the core on level ground where the middle of the hall should be. It sets itself into the ground, so the "
     "hall floor ends up level with the land. Everything else is built from it.", "core_placed"),
    ("3. Load the blocks",
     "Every step costs exactly the blocks it is built from: right-click the core to see them. Right-click it with "
     "blocks, sneak-right-click with an empty hand to load your inventory, or feed it with hoppers and pipes.",
     "tower_screen"),
    ("4. Build the foundation",
     "With every block of the foundation in, press UPGRADE DRONE TOWER. The core builds the control building and its "
     "hall itself, and in the yard to the east the depot terminal, a ground pad and the two exchanges.", "tier1"),
    ("5. Roof pads",
     "Load tier 1's hangar pads and press UPGRADE again. The core lays six landing pads on the building's roof, room "
     "for 24 tier 1 drones. The hall's desks and touch consoles work the depot too.", None),
    ("6. Power",
     "The Energy Exchange stands in the yard. Run cables or put generators against its marked port. That powers "
     "every drone; low power only slows them.", "energy_port"),
    ("7. Building blocks",
     "The Storage Exchange stands beside it. Pipes, conveyors and hoppers against its marked port fill the depot's "
     "store, and drones take the blocks for every job from there.", "storage_port"),
    ("8. Link drones",
     "Right-click the terminal or a landing pad with a drone to link it. A tier N tower allows drones up to tier "
     "N. Tier 1 holds 24 drones; a finished tower holds 128.", "terminal"),
    ("9. Grow the tower",
     "Load the next tier's blocks and press UPGRADE again. From tier 2 on, your drones fly each tier in, from the "
     "bottom up, as long as power and blocks keep coming.", "tier2"),
    ("Range",
     "Your drones build blueprints up to 250 chunks from the tower, wherever the site's chunks are loaded; the tower "
     "keeps itself loaded. You can build more towers, each over 250 chunks from your others.", None),
    ("Tips",
     "The terminal's mode (Personal or Party) decides who may use it. Place other blueprints and your drones build "
     "those too. The tower is lit, so mobs do not spawn on it.", None),
]

CREATIVE_GUIDE = [
    ("Creative Quick Start",
     "Set up a Drone Tower in a few minutes, with endless power and building blocks. Everything here is in the "
     "creative inventory: search for it by name.", "tier9"),
    ("1. The core",
     "Place a Drone Tower Core on level ground where the middle of the hall should be: it sets itself into the "
     "ground. Holding it shows how big the finished tower gets (shift+scroll for each tier).", "core_placed"),
    ("2. Load it",
     "Right-click the core and press LOAD FROM INVENTORY. In creative mode that fills it with every block the whole "
     "tower needs, so every UPGRADE after it is paid for.", "tower_screen"),
    ("3. Foundation and roof pads",
     "Right-click the core and press UPGRADE DRONE TOWER: the core builds the control building, its yard and the "
     "exchanges by itself. Press UPGRADE again for the six landing pads on the roof.", "tier1"),
    ("4. Creative Energy Cell",
     "Place it touching the Energy Exchange's marked port, out in the yard east of the building. It gives endless "
     "power on every side, so the depot never runs low.", "creative_energy"),
    ("5. Creative Supply Crate",
     "Place it anywhere within 48 blocks of the depot terminal, for example in the yard beside it. Drones then get "
     "every building block they ask for, free.", "creative_supply"),
    ("6. Drones",
     "Right-click the terminal with drones to link them. The Creative Drone is fast and needs no power. A tier N "
     "tower allows drones up to tier N.", "terminal"),
    ("7. Grow it",
     "Open the core and press UPGRADE again for each tier. Your drones fly every tier in until the three pillars "
     "are done at tier 9. For survival, craft the Field Manual (a book and a tier 1 drone).", "tier2"),
]

BOOKS = {
    "drone_tower_manual": ("Drone Tower Field Manual", FIELD_MANUAL),
    "creative_tower_guide": ("Creative Quick Start", CREATIVE_GUIDE),
}

LANG = {"message.jugcraft.guide.given": "You got the Creative Quick Start book: it shows how to set up a Drone Tower"}
for item, (title, pages) in BOOKS.items():
    LANG[f"item.{MOD}.{item}"] = title
    for i, (heading, body, _) in enumerate(pages):
        LANG[f"book.{MOD}.{item}.page.{i + 1}.heading"] = heading
        LANG[f"book.{MOD}.{item}.page.{i + 1}"] = body

PAGE_COUNTS = {item: len(pages) for item, (_, pages) in BOOKS.items()}
SCREENSHOTS = sorted({shot for _, pages in BOOKS.values() for _, _, shot in pages if shot})


def write_assets(write, rid, assets, data, lang):
    """Item models, each book's page list (assets/jugcraft/guide/<book>.json), the Field Manual recipe, text."""
    import pathlib
    for item, (title, pages) in BOOKS.items():
        write(pathlib.Path(assets) / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/written_book"}})
        write(pathlib.Path(assets) / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
        write(pathlib.Path(assets) / "guide" / f"{item}.json", {"pages": [
            {"heading": f"book.{MOD}.{item}.page.{i + 1}.heading", "text": f"book.{MOD}.{item}.page.{i + 1}",
             **({"image": rid(f"textures/gui/guide/{shot}.png")} if shot else {})}
            for i, (_, _, shot) in enumerate(pages)]})
    write(pathlib.Path(data) / MOD / "recipe" / "drone_tower_manual.json", {
        # Loaded only when the tier 1 drone it needs is (the same feature switches as its recipe).
        "fabric:load_conditions": [{"condition": f"{MOD}:feature_enabled", "feature": f}
                                   for f in ("machines", "drones", "lead", "sulfur", "tin")],
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["minecraft:book", rid("drone_t1")],
        "result": {"id": rid("drone_tower_manual"), "count": 1}})
    lang.update(LANG)
