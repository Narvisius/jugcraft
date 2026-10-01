"""The two Drone Tower guide books (drone/GuideBooks.java): the Field Manual, crafted from a book and a tier 1
drone, and the Creative Quick Start, given once to a player who joins in creative mode.

Each book is an ordinary written book whose pages are translatable text, so it reads like any vanilla book and
can be translated. A page holds about 14 short lines; keep each page under ~230 characters.
"""
MOD = "jugcraft"

FIELD_MANUAL = [
    "§lDRONE TOWER\nFIELD MANUAL§r\n\nA Drone Tower is a depot that grows. Its drones carry blocks and build for you, and every tier you add holds more drones and stronger ones.\n\nRead on to set one up.",
    "§l1. Pick the spot§r\n\nGet a Drone Tower Foundation blueprint from a Blueprint Table. Hold it and shift+scroll to see the finished tower. It ends up over 200 blocks tall on a 39x39 field, so leave room.",
    "§l2. Lay the plinth§r\n\nBuild a flat 15x15 square of chiseled stone bricks. The tower stands on it.\n\nThe tower does not fill in uneven ground, so build supports where the field hangs over a drop.",
    "§l3. Place the core§r\n\nCraft a Drone Tower Core from aluminium plates, a processor, advanced circuits and a Drone Depot Terminal. Put it in the very middle of the plinth. It is yours: one tower per player in each dimension.",
    "§l4. Load modules§r\n\nTiers are built from four modules: Structural, Hangar, Armour and Avionics.\n\nRight-click the core (or the terminal) holding them, or feed them in with pipes or conveyors.",
    "§l5. Tier 1§r\n\nRight-click the core to open the tower screen. It shows what the next tier needs. Press UPGRADE DRONE TOWER.\n\nThe core builds tier 1 itself: landing pads, the command room with its terminal, and two exchanges.",
    "§l6. Power§r\n\nOn the west edge of the field is the Energy Exchange. Run cables or put generators against its Energy Exchange Port; that powers every drone.\n\nLow power only slows them down.",
    "§l7. Building blocks§r\n\nOn the east edge is the Storage Exchange. Pipes, conveyors and hoppers against its Cargo Exchange Port fill the depot's store. Drones take the blocks for each job from there.",
    "§l8. Drones§r\n\nRight-click the terminal or a landing pad with a drone to link it. A tier N tower allows drones up to tier N.\n\nTier 1 holds 32 drones; a finished tower holds 133.",
    "§l9. Grow it§r\n\nLoad the next tier's modules and press UPGRADE again. From tier 2 on, your drones fly each tier in from the bottom up.\n\nKeep power and blocks coming and they keep working.",
    "§lTips§r\n\n- The terminal's mode (Personal or Party) decides who may use it.\n- Place other blueprints and your drones build those too.\n- The tower is lit, so mobs do not spawn on it.",
]

CREATIVE_GUIDE = [
    "§lCREATIVE\nQUICK START§r\n\nSet up a Drone Tower in a few minutes, with endless power and blocks.\n\nEverything below is in the creative inventory: search for it by name.",
    "§l1. Plinth and core§r\n\nLay a flat 15x15 square of chiseled stone bricks. Place a Drone Tower Core in the very middle.\n\nTip: a Drone Tower Foundation blueprint previews the finished tower (shift+scroll).",
    "§l2. Modules§r\n\nTake stacks of the Structural, Hangar, Armour and Avionics Modules. Right-click the core with each stack, a few times over: it holds up to 1024 of each, enough for every tier.",
    "§l3. Tier 1§r\n\nRight-click the core and press UPGRADE DRONE TOWER. The core builds the command room, the landing pads and two exchanges round the field by itself.",
    "§l4. Creative Energy Cell§r\n\nPlace it touching the Energy Exchange Port, on the west edge of the field. It gives endless power on all sides, so the depot never runs low.",
    "§l5. Creative Supply Crate§r\n\nPlace it anywhere within 48 blocks of the depot terminal (inside the command room is easy). Drones then get every building block they ask for, free.",
    "§l6. Drones§r\n\nRight-click the terminal with drones to link them. The Creative Drone builds fast and needs no power.\n\nA tier N tower allows drones up to tier N.",
    "§l7. Grow it§r\n\nOpen the core and press UPGRADE again for each tier. Your drones fly every tier in, from the bottom up, until the spire is done at tier 9.\n\nCraft the Field Manual (a book and a tier 1 drone) for survival.",
]

BOOKS = {
    "drone_tower_manual": ("Drone Tower Field Manual", FIELD_MANUAL),
    "creative_tower_guide": ("Creative Quick Start", CREATIVE_GUIDE),
}

LANG = {"message.jugcraft.guide.given": "You got the Creative Quick Start book: it shows how to set up a Drone Tower"}
for item, (title, pages) in BOOKS.items():
    LANG[f"item.{MOD}.{item}"] = title
    for i, text in enumerate(pages):
        LANG[f"book.{MOD}.{item}.page.{i + 1}"] = text

PAGE_COUNTS = {item: len(pages) for item, (_, pages) in BOOKS.items()}


def write_assets(write, rid, assets, data, lang):
    """Item models (the vanilla written book picture), the Field Manual recipe, page text."""
    import pathlib
    for item in BOOKS:
        write(pathlib.Path(assets) / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/written_book"}})
        write(pathlib.Path(assets) / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    write(pathlib.Path(data) / "recipe" / "drone_tower_manual.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["minecraft:book", rid("drone_t1")],
        "result": {"id": rid("drone_tower_manual"), "count": 1}})
    lang.update(LANG)
