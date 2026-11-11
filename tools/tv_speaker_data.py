"""The surround speaker's and the Sync Key's ids and names (tv/TvSpeakerBlock.java, tv/SyncKeyItem.java): kept apart
from tools/tv_speaker.py (which needs the mesh tools) so the material data generator can name them
(plant_interior.write_assets, with the flat screen TV), list them (machines.machine_blocks, machine_items) and write the
speaker's loot table (nuclear.write_loot)."""

ID, KEY_ID = "tv_speaker", "sync_key"
LANG = {
    f"block.jugcraft.{ID}": "Surround Speaker",
    f"item.jugcraft.{KEY_ID}": "Sync Key",
    "message.jugcraft.tv_speaker.auto": "Speaker linked to the screen at %s, %s, %s",
    "message.jugcraft.tv_speaker.none": "No screen within %s blocks: link it with a Sync Key",
    "message.jugcraft.tv_speaker.linked": "Speaker linked to the screen at %s, %s, %s",
    "message.jugcraft.tv_speaker.unlinked": "Speaker unlinked",
    "message.jugcraft.tv_speaker.status_linked": "Plays the screen at %s, %s, %s",
    "message.jugcraft.tv_speaker.status_none": "Not linked: right-click it with a Sync Key set to a screen",
    "message.jugcraft.sync_key.screen": "Sync Key set to the screen at %s, %s, %s: now right-click speakers",
    "message.jugcraft.sync_key.speaker": "Speaker picked: now right-click a screen",
    "message.jugcraft.sync_key.gone": "That screen isn't there any more",
    "message.jugcraft.sync_key.far": "Too far: a speaker links to a screen within %s blocks",
    "message.jugcraft.sync_key.cleared": "Sync Key cleared",
    "tooltip.jugcraft.sync_key.screen": "Set to the screen at %s, %s, %s",
    "tooltip.jugcraft.sync_key.speaker": "Holding the speaker at %s, %s, %s",
    "tooltip.jugcraft.sync_key.empty": "Right-click a screen, then speakers",
}

BLOCKS = [ID]
ITEMS = [KEY_ID]


def loot(rid=lambda path: f"jugcraft:{path}"):
    """The speaker's loot table: only its lower half drops it (breaking either half takes both)."""
    return {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": rid(ID)}],
        "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
                                                           {"type": "minecraft:match_block", "blocks": rid(ID), "state": {"half": "lower"}}]}}],
        "random_sequence": rid(f"blocks/{ID}")}
