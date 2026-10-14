"""Pipes and cables run inside made walls, as in RimWorld (wallrun/WallRuns.java; Justin's round 12).

Sneak-right-click a made block (concrete, bricks, planks, metal...) holding a pipe or cable and the run goes inside it;
sneak-right-click a laid pipe or cable holding a made block and the block goes round it. The wall keeps its look: the
client draws the wall's own block model (client/WallRunModel.java), so no block needs textures of its own. These run
blocks' own models are only the wall sleeves, shown where a run comes out into the open: a bolted plate on the wall's
face and a collar round the pipe (a conduit fitting with its lock nut for a cable).

Sleeves are drawn facing north, on the block's north face (z = 0) and standing out of it (z < 0).
"""
import model_writer as MW

MOD = "jugcraft"
# The plain pipe or cable, its name, and its sleeve: the plate's half-width, the collar's half-width, the collar's
# depth, and the textures of the plate, collar and bolts.
CARRIERS = {
    "bronze_fluid_pipe": ("Bronze Fluid Pipe", "fluid"),
    "steel_fluid_pipe": ("Steel Fluid Pipe", "fluid"),
    "brass_item_pipe": ("Brass Item Pipe", "item"),
    "copper_cable": ("Copper Cable", "cable"),
    "silver_cable": ("Silver Cable", "cable"),
    "aluminum_cable": ("Aluminum Cable", "cable"),
}
NAMES = {f"wall_{carrier}": f"{name} in Wall" for carrier, (name, _) in CARRIERS.items()}

GUN, CHROME, GALV = "dp_gunmetal", "dp_chrome", "st_galv"

# Natural ground and rock: never a wall for a run (WallRuns.EXCLUDED). Falling blocks (sand, gravel) are left out by
# the code.
EXCLUDED = ["#minecraft:dirt", "#minecraft:base_stone_overworld", "#minecraft:base_stone_nether", "#minecraft:nylium",
            "#c:ores", "minecraft:end_stone", "minecraft:clay", "minecraft:snow_block", "minecraft:mud", "minecraft:sculk",
            "minecraft:moss_block", "minecraft:packed_mud", "minecraft:dripstone_block", "minecraft:calcite",
            "minecraft:smooth_basalt", "minecraft:magma_block", "minecraft:soul_soil", "minecraft:bedrock"]


def sleeve(family):
    """A wall sleeve where a run leaves the wall: the plate (a pixel thick, bolted at its corners) and the collar
    round the pipe; a cable's is a conduit fitting with a lock nut."""
    if family == "fluid":
        plate, collar, depth, face = 4.5, 2.6, 2.4, GUN
    elif family == "item":
        plate, collar, depth, face = 5.2, 3.6, 2.4, GUN
    else:
        plate, collar, depth, face = 4.6, 3.4, 3.0, GALV
    c = 8
    m = [((c - plate, c - plate, -0.8), (c + plate, c + plate, 0), face),
         ((c - collar, c - collar, -depth), (c + collar, c + collar, -0.8), face)]
    if family == "cable":
        m.append(((c - collar - 0.5, c - collar - 0.5, -1.5), (c + collar + 0.5, c + collar + 0.5, -0.8), CHROME))
    k = plate - 0.9
    for x in (c - k, c + k):
        for y in (c - k, c + k):
            m.append(((x - 0.45, y - 0.45, -1.25), (x + 0.45, y + 0.45, -0.8), CHROME))
    return m


def _model(elements, rid):
    names = sorted({e[2] for e in elements})
    out = [MW.element(frm, to, tex, uv=True) for frm, to, tex in elements]
    textures = {n: rid(f"block/{n}") for n in names}
    textures["particle"] = textures[names[0]]
    return {"textures": textures, "elements": out}


def write_assets(write, rid, assets, lang):
    models = assets / "models" / "block"
    for family in ("fluid", "item", "cable"):
        write(models / f"wall_run_sleeve_{family}.json", _model(sleeve(family), rid))
    turns = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    for carrier, (name, family) in CARRIERS.items():
        block = f"wall_{carrier}"
        lang[f"block.{MOD}.{block}"] = NAMES[block]
        # The wall's look is the client's (WallRunModel); the block model is only the sleeves.
        parts = [{"when": {face: "true"}, "apply": {"model": rid(f"block/wall_run_sleeve_{family}"), **turn}} for face, turn in turns.items()]
        write(assets / "blockstates" / f"{block}.json", {"multipart": parts})


def write_loot(write, rid, out, self_drop):
    # The pipe or cable; the wall's own block drops by its own loot (WallRuns.drops).
    for carrier in CARRIERS:
        block = f"wall_{carrier}"
        write(out / f"{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:item", "name": rid(carrier)}], "condition": {"type": "minecraft:survives_explosion"}}],
            "random_sequence": rid(f"blocks/{block}")})


def write_tags(tags, rid):
    # No mining tags: a run breaks as fast as the wall it is in (WallRuns.destroyProgress).
    for entry in EXCLUDED:
        tags.add("block", f"{MOD}:wall_run_excluded", entry)
