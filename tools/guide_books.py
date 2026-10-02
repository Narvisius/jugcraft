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
     "Print a Drone Tower Foundation blueprint at a Blueprint Table. Hold it and shift+scroll to see how big the "
     "finished tower gets: over 200 blocks tall on a 39x39 field. Leave room.", "blueprint_preview"),
    ("2. Plinth and core",
     "Lay a flat 15x15 square of chiseled stone bricks. Craft a Drone Tower Core (aluminium plates, a processor, "
     "advanced circuits and a Drone Depot Terminal) and put it in the very middle.", "plinth"),
    ("3. Load modules",
     "Tiers are built from Structural, Hangar, Armour and Avionics Modules. Right-click the core holding them, or "
     "feed them in with pipes or conveyors. Right-click the core to see what the next tier needs.", "tower_screen"),
    ("4. Build tier 1",
     "Press UPGRADE DRONE TOWER. The core builds tier 1 itself: eight landing pads, the command room with the "
     "depot terminal, and two exchanges on the edges of the field.", "tier1"),
    ("5. Power",
     "The Energy Exchange stands on the west edge. Run cables or put generators against its Energy Exchange Port "
     "(the striped block). That powers every drone; low power only slows them.", "energy_port"),
    ("6. Building blocks",
     "The Storage Exchange stands on the east edge. Pipes, conveyors and hoppers against its Cargo Exchange Port "
     "fill the depot's store, and drones take the blocks for every job from there.", "storage_port"),
    ("7. Link drones",
     "Right-click the terminal or a landing pad with a drone to link it. A tier N tower allows drones up to tier "
     "N. Tier 1 holds 32 drones; a finished tower holds 133.", "terminal"),
    ("8. Grow the tower",
     "Load the next tier's modules and press UPGRADE again. From tier 2 on, your drones fly each tier in, from the "
     "bottom up, as long as power and blocks keep coming.", "tier2"),
    ("Range",
     "Your drones build blueprints up to 50 chunks from the tower, wherever the site's chunks are loaded; the tower "
     "keeps itself loaded. You can build more towers, each over 50 chunks from your others.", None),
    ("Tips",
     "The terminal's mode (Personal or Party) decides who may use it. Place other blueprints and your drones build "
     "those too. The tower is lit, so mobs do not spawn on it.", None),
]

CREATIVE_GUIDE = [
    ("Creative Quick Start",
     "Set up a Drone Tower in a few minutes, with endless power and building blocks. Everything here is in the "
     "creative inventory: search for it by name.", "tier9"),
    ("1. Plinth and core",
     "Lay a flat 15x15 square of chiseled stone bricks and place a Drone Tower Core in the very middle. A Drone "
     "Tower Foundation blueprint previews the finished tower (shift+scroll).", "plinth"),
    ("2. Modules go in the core",
     "Take stacks of the Structural, Hangar, Armour and Avionics Modules and right-click the core with each, a "
     "few times over. It holds 1024 of each, enough for every tier.", "tower_screen"),
    ("3. Build tier 1",
     "Right-click the core and press UPGRADE DRONE TOWER. The core builds the command room, the landing pads and "
     "two exchanges round the field by itself.", "tier1"),
    ("4. Creative Energy Cell",
     "Place it touching the Energy Exchange Port, the striped block on the west edge of the field. It gives "
     "endless power on every side, so the depot never runs low.", "creative_energy"),
    ("5. Creative Supply Crate",
     "Place it anywhere within 48 blocks of the depot terminal, for example just off the edge of the field. "
     "Drones then get every building block they ask for, free.", "creative_supply"),
    ("6. Drones",
     "Right-click the terminal with drones to link them. The Creative Drone is fast and needs no power. A tier N "
     "tower allows drones up to tier N.", "terminal"),
    ("7. Grow it",
     "Open the core and press UPGRADE again for each tier. Your drones fly every tier in until the spire is done "
     "at tier 9. For survival, craft the Field Manual (a book and a tier 1 drone).", "tier2"),
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
