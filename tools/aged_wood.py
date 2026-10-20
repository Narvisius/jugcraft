"""Aged wood (docs/features/aged-wood.md): pine and maple planks darkened with nitric acid, as the owner asked for
(docs/features/industrial-chemical-catalog-and-routes.md), each with its stairs and slab. The Chemical Reactor treats
eight planks with 100 mB of nitric acid. Keep WOODS in sync with building/AgedWood.java; tools/check_mod_data.py checks
it.

The textures are Jugcraft's own pine and maple planks (agriculture's wood sets) with each tone moved, darkest to
lightest, onto an aged ramp: the grain is unchanged. Pine goes from its olive to a honey amber, maple from its pale tan
to a deep red-brown, as nitric acid browns the wood. No Mojang texture is read, traced or recoloured.
"""
from PIL import Image

MOD = "jugcraft"
# Each wood's display name and its aged ramp, dark to light: the wood's own tones map onto it by rank.
WOODS = {
    "pine": ("Aged Pine", ["5a3a16", "6c481c", "7e5822", "8a6228", "986e2e", "a67a34", "b2863c"]),
    "maple": ("Aged Maple", ["5a2a18", "74381f", "8a4a2e", "9e5e40"]),
}
# The Chemical Reactor's treatment: this many planks and this much nitric acid (mB) give as many aged planks.
PLANKS_PER_BATCH = 8
ACID_MB = 100
TICKS = 100


def blocks():
    return [f"aged_{wood}_{part}" for wood in WOODS for part in ("planks", "stairs", "slab")]


def reactor_recipes():
    """Chemical Reactor recipes (tools/petro.py FLUID_RECIPES["chemical_reactor"])."""
    return [{"name": f"aged_{wood}_planks", "items": [(f"{MOD}:{wood}_planks", PLANKS_PER_BATCH)],
             "fluids": [(f"{MOD}:nitric_acid", ACID_MB)], "results": [(f"{MOD}:aged_{wood}_planks", PLANKS_PER_BATCH)],
             "ticks": TICKS, "features": ["machines", "biomes"]} for wood in WOODS]


def texture(wood, source):
    """The aged planks: the wood's own planks with every tone moved, by rank, onto its aged ramp."""
    img = source.convert("RGBA")
    tones = sorted({px[:3] for px in img.getdata() if px[3]}, key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])
    ramp = [tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) for h in WOODS[wood][1]]
    mapping = {tone: ramp[round(i * (len(ramp) - 1) / max(1, len(tones) - 1))] for i, tone in enumerate(tones)}
    out = Image.new("RGBA", img.size)
    out.putdata([mapping[px[:3]] + (px[3],) if px[3] else px for px in img.getdata()])
    return out


def draw(save, textures):
    """save(img, kind, name) as in tools/generate_textures.py; textures is the block texture folder."""
    for wood in WOODS:
        with Image.open(textures / f"{wood}_planks.png") as source:
            save(texture(wood, source), "block", f"aged_{wood}_planks")


def write_all(write, assets, data, lang, condition, self_drop):
    from construction import stairs_blockstate
    models = assets / "models" / "block"
    for wood, (display, _) in WOODS.items():
        aged = f"aged_{wood}"
        planks = f"{MOD}:block/{aged}_planks"
        three = {"bottom": planks, "side": planks, "top": planks}
        write(models / f"{aged}_planks.json", {"parent": "minecraft:block/cube_all", "textures": {"all": planks}})
        write(assets / "blockstates" / f"{aged}_planks.json", {"variants": {"": {"model": planks}}})
        for suffix in ("", "_inner", "_outer"):
            write(models / f"{aged}_stairs{suffix}.json",
                  {"parent": f"minecraft:block/{suffix[1:] + '_' if suffix else ''}stairs", "textures": three})
        write(assets / "blockstates" / f"{aged}_stairs.json", stairs_blockstate(f"{MOD}:block/{aged}_stairs"))
        write(models / f"{aged}_slab.json", {"parent": "minecraft:block/slab", "textures": three})
        write(models / f"{aged}_slab_top.json", {"parent": "minecraft:block/slab_top", "textures": three})
        write(assets / "blockstates" / f"{aged}_slab.json", {"variants": {
            "type=bottom": {"model": f"{MOD}:block/{aged}_slab"}, "type=top": {"model": f"{MOD}:block/{aged}_slab_top"},
            "type=double": {"model": planks}}})
        for part, name in (("planks", "Planks"), ("stairs", "Stairs"), ("slab", "Slab")):
            block = f"{aged}_{part}"
            lang[f"block.{MOD}.{block}"] = f"{display} {name}"
            write(assets / "items" / f"{block}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{block}"}})
            table = self_drop(block)
            if part == "slab":
                table["pools"][0]["entries"][0]["modifier"] = [
                    {"type": "minecraft:set_count", "count": 2, "add": False,
                     "condition": {"type": "minecraft:match_block", "blocks": f"{MOD}:{block}", "state": {"type": "double"}}},
                    {"type": "minecraft:explosion_decay"}]
            write(data / "loot_table" / "blocks" / f"{block}.json", table)
        key = {"#": f"{MOD}:{aged}_planks"}
        for part, pattern, count, group in (("stairs", ["#  ", "## ", "###"], 4, "wooden_stairs"),
                                            ("slab", ["###"], 6, "wooden_slab")):
            write(data / "recipe" / f"{aged}_{part}.json", {
                "fabric:load_conditions": condition("biomes"), "type": "minecraft:crafting_shaped",
                "category": "building", "group": group, "pattern": pattern, "key": key,
                "result": {"id": f"{MOD}:{aged}_{part}", "count": count}})


def add_tags(tags):
    """Mined with an axe; vanilla's plank, wooden stairs and wooden slab tags, so they craft and burn as planks do."""
    for wood in WOODS:
        aged = f"{MOD}:aged_{wood}"
        for block in (f"{aged}_planks", f"{aged}_stairs", f"{aged}_slab"):
            tags.add("block", "minecraft:mineable/axe", block)
        for kind in ("block", "item"):
            tags.add(kind, "minecraft:planks", f"{aged}_planks")
            tags.add(kind, "minecraft:wooden_stairs", f"{aged}_stairs")
            tags.add(kind, "minecraft:wooden_slabs", f"{aged}_slab")
