"""Writes Adam-ondi-Ahman's data from tools/adam_ondi_ahman.py: the knoll's template (world/AltarKnolls builds it), the
Ancient Altar's block assets, loot table and tags, and names. The valley's biome is tools/biomes.py's
(placed round its sites, "placement": "site"). Called by agriculture_data.py, as the lairs' data is."""
import adam_ondi_ahman as aa

MOD = "jugcraft"


def rid(path):
    return path if ":" in path else f"{MOD}:{path}"


def assets(root, write, lang):
    models, states = root / "models" / "block", root / "blockstates"
    write(models / f"{aa.ALTAR}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": rid(f"block/{aa.ALTAR}_top"), "side": rid(f"block/{aa.ALTAR}_side"), "bottom": rid(f"block/{aa.ALTAR}_bottom")}})
    write(states / f"{aa.ALTAR}.json", {"variants": {"": {"model": rid(f"block/{aa.ALTAR}")}}})
    lang[f"block.{MOD}.{aa.ALTAR}"] = aa.ALTAR_DISPLAY
    lang[f"message.{MOD}.adam_ondi_ahman.dormant"] = aa.DORMANT


def loot(out, write):
    """The altar gives nothing: it cannot be broken."""
    write(out / f"{aa.ALTAR}.json", {"type": "minecraft:block", "pools": [], "random_sequence": rid(f"blocks/{aa.ALTAR}")})


def tags(tags):
    for tag in ("minecraft:wither_immune", "minecraft:dragon_immune", "minecraft:features_cannot_replace"):
        tags.add("block", tag, rid(aa.ALTAR))


def worldgen(data, write):
    aa.write(data / MOD / "structure" / f"{aa.TEMPLATE}.nbt")
