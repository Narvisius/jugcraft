"""Owner-requested vanilla bark/end-grain with Jugcraft tints (11 October 2026).

Reference the player's Minecraft textures; do not redistribute Minecraft image files. This also follows resource packs.
Bark and cut wood have separate, fixed tints in both the world and inventory. Leaves/planks keep their existing art.
"""

# wood: vanilla bark species, bark multiplier, cut/stripped multiplier (RGB)
LOGS = {
    "chestnut": ("oak", 0xDAD2BC, 0xDDCCAA),
    "larch": ("spruce", 0xEAB67E, 0xFFC078),
    "maple": ("oak", 0xD1ACA0, 0xF9CBAC),
    "aspen": ("birch", 0xF1F0EE, 0xF2F0E6),
    "fir": ("spruce", 0xBAC5AA, 0xF5E0B0),
    "dead": ("pale_oak", 0x9B979F, 0xB0ADB7),
    "jacaranda": ("oak", 0xC6A6D5, 0xBD91BF),
    "willow": ("oak", 0xBFCBB0, 0xE1E9B9),
    "palm": ("jungle", 0xF2D1A9, 0xFFF1D5),
    "cypress": ("spruce", 0xD7BBB2, 0xD1A399),
    "redwood": ("spruce", 0xEFAB91, 0xF5A289),
    "eucalyptus": ("birch", 0xC6D8BB, 0xF2DDC6),
    "mahogany": ("jungle", 0xB58378, 0xB56855),
    "cedar": ("spruce", 0xC5B6A5, 0xBCAA92),
    "cinnamon": ("jungle", 0xE9B98D, 0xF7B881),
}


def column(side, end, horizontal=False, bark_ends=False):
    faces = {}
    for face in ("down", "up", "north", "south", "west", "east"):
        cap = face in ("down", "up")
        faces[face] = {"texture": "#end" if cap else "#side", "cullface": face,
                       "tintindex": 0 if bark_ends or not cap else 1}
        # Match vanilla cube_column_horizontal: blockstate rotation already turns the bark grain.
        # Only the upper end needs its UV flipped. Turning side UVs too would put grain across a beam.
        if horizontal and face == "up":
            faces[face]["rotation"] = 180
    return {"parent": "minecraft:block/block", "textures": {"side": side, "end": end, "particle": side},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]}


def write_all(write, assets):
    tints = {}
    for wood, (base, bark, cut) in LOGS.items():
        for stripped in (False, True):
            prefix = "stripped_" if stripped else ""
            side = f"minecraft:block/stripped_pale_oak_log" if stripped else f"minecraft:block/{base}_log"
            end = "minecraft:block/pale_oak_log_top"
            if stripped:
                end = "minecraft:block/stripped_pale_oak_log_top"
            for form in ("log", "wood"):
                name = f"{prefix}{wood}_{form}"
                if not (assets / "blockstates" / f"{name}.json").exists():
                    continue  # Cinnamon has logs, but no bark-all-sides wood block.
                colours = [cut if stripped else bark, cut]
                tints[name] = colours
                write(assets / "models" / "block" / f"{name}.json",
                      column(side, side if form == "wood" else end, bark_ends=form == "wood"))
                if form == "log":
                    write(assets / "models" / "block" / f"{name}_horizontal.json", column(side, end, horizontal=True))
                write(assets / "items" / f"{name}.json", {"model": {
                    "type": "minecraft:model", "model": f"jugcraft:block/{name}",
                    "tints": [{"type": "minecraft:constant", "value": colour} for colour in colours]}})
    write(assets / "wood_tints.json", tints)
