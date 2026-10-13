"""Roadmap step 18: logistics with reservations and accountable transit (docs/features/arcane-concordance-logistics.md).

A Courier Post takes requests: its owner's party asks for a stack of exactly an item (the item and every component it
carries). Clockwork Porters bound to the post (a Porter Key used on the post, then on the porter) claim one request at
a time, reserve the items at a container round the post, pick them up, carry them to the post and deliver them. Items
in transit are held in one place, the world's ledger (Java: concordance/logistics, pure; concordance/courier), never in
a porter: a porter removed, unloaded or restarted leaves its cargo stranded at the post for another porter to take up,
or for the requester to recover. A cancelled request, or one whose post is broken, takes its cargo back first. Every
step is written to the post's history, which the post shows.

tools/concordance.py merges these tables into its own; tools/check_mod_data.py checks the numbers the Java repeats.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# The ledger (Java: logistics/Logistics.java).
MAX_WANTED = 256
MAX_OPEN_PER_PLAYER = 16
MAX_OPEN = 1024
LEASE_TICKS = 200
RETRY_TICKS = 200
HISTORY = 32
# The post (Java: courier/Couriers.java, courier/CourierPostBlockEntity.java).
SOURCE_RADIUS = 8
SHOWN_HISTORY = 8
POST_SLOTS = 9

STATES = {
    "open": "waiting for a courier",
    "claimed": "a courier is on it",
    "in_transit": "in transit",
    "stranded": "stranded at the post (its courier is gone)",
    "returning": "being taken back",
}
NOTES = {
    "full": "the post is full",
    "lapsed": "its courier stopped answering",
    "worker_removed": "its courier was removed",
    "reassigned": "its courier was given other work",
    "cannot_navigate": "its courier could not find a way",
    "no_source": "no container round the post has it",
    "source_empty": "the container was emptied first",
    "source_unloaded": "the container is not loaded",
    "source_gone": "the container is gone",
    "blocked": "the courier's keeper may not open that container",
    "cancelled": "cancelled",
    "destination_gone": "the post was broken",
    "nowhere_to_return": "there is no room to take it back",
}
EVENTS = {
    "filed": "#%1$s filed: %2$s wanted",
    "claimed": "#%1$s: a courier takes it",
    "reserved": "#%1$s: %2$s reserved at %3$s",
    "picked_up": "#%1$s: %2$s picked up at %3$s",
    "source_empty": "#%1$s: nothing left to pick up at %3$s",
    "taken_up": "#%1$s: %2$s stranded taken up again",
    "delivered": "#%1$s: %2$s delivered",
    "done": "#%1$s done (%2$s in the last delivery)",
    "released": "#%1$s: its courier lets it go (%3$s)",
    "stranded": "#%1$s: %2$s stranded at the post (%3$s)",
    "returning": "#%1$s: %2$s to take back (%3$s)",
    "returned": "#%1$s: %2$s taken back",
    "cancelled": "#%1$s cancelled (%3$s)",
    "recovered": "#%1$s: %2$s recovered by its requester",
}
OUTCOMES = {
    "not_found": "No such open request",
    "not_yours": "That request is not yours",
    "claimed": "A courier already has that request",
    "wrong_state": "That request cannot do that now (only stranded cargo can be recovered)",
    "bad_count": "A request asks for 1 to %2$s items",
    "too_many": "You have too many open requests (at most %1$s)",
    "nothing_free": "There is nothing to move",
}

BLOCKS = {"courier_post": {"name": "Courier Post"}}
DEVICE_TOOLTIPS = {
    "courier_post": "Use with an item: request a stack of exactly it. Empty hand: its history. Porters bound by a Porter Key fetch.",
}

MESSAGES = {
    "courier.filed": "Requested %s x %s (request #%s): a courier bound to this post will fetch it from the containers round it",
    "courier.not_yours": "Only the post's owner (or their party) may use it",
    "courier.no_sample": "Hold the item you want in your main hand",
    "courier.no_post": "Stand within %s blocks of a Courier Post you may use",
    "courier.header": "Courier Post: %s open, %s in transit; %s of %s slots free",
    "courier.line": "#%s %s x %s: %s; %s delivered, %s carried%s",
    "courier.because": " (%s)",
    "courier.none": "You have no open requests",
    "courier.cancelled": "Request #%s cancelled",
    "courier.returning": "Request #%s: its cargo is being taken back first",
    "courier.recovered": "Recovered request #%s's cargo",
    "courier.key_post": "Courier Post at %s %s %s (now use the key on a porter)",
    "courier.assigned": "The porter now serves that Courier Post",
    "courier.still_carrying": "It still carries its route's load: let it deliver first",
    "courier.too_fast": "Wait a moment before asking the couriers again",
}
TOOLTIPS = {
    "porter_key.post": "Courier Post: %s",
    "jade.courier": "%s open, %s in transit",
    "jade.courier_last": "Last: %s",
}


def lang_entries(lang):
    for key, text in STATES.items():
        lang[f"compose.{MOD}.courier.state.{key}"] = text
    for key, text in NOTES.items():
        lang[f"compose.{MOD}.courier.note.{key}"] = text
    for key, text in EVENTS.items():
        lang[f"message.{MOD}.concordance.courier.event.{key}"] = text
    for key, text in OUTCOMES.items():
        lang[f"message.{MOD}.concordance.courier.refused.{key}"] = text
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"config.jade.plugin_{MOD}.courier_post"] = BLOCKS["courier_post"]["name"]


# ------------------------------------------------------------------------------------------------- codex

def codex():
    states = "\\\n".join(f"- **{key.replace('_', ' ')}**: {text}" for key, text in STATES.items())
    return {
        ("binding", "couriers"): {
            "name": "Couriers", "x": 0, "y": 2, "icon": rid("courier_post"), "condition": ("binding_arts", "understood"),
            "description": "Requests, reservations and accountable transit",
            "pages": [
                ("crafting_recipe", "Courier Post",
                 f"Use the post with an item in your hand to **request** a stack of exactly that item (a named or "
                 f"enchanted one is not a plain one). Bind a Porter Key to the post, then use the key on a Clockwork "
                 f"Porter: it serves the post, fetching from containers within {SOURCE_RADIUS} blocks into the post's "
                 f"{POST_SLOTS} slots (a hopper below takes them out).", rid("courier_post")),
                ("text", "Accountable Transit",
                 "One courier takes a request at a time and **reserves** what it will carry, so two never reach for "
                 "the same items. What a courier has picked up is held by the post's ledger, not by the courier: if it "
                 "is broken, unloaded or the world restarts, its cargo waits at the post for another courier, or for "
                 "you (**/jugcraft concordance logistics recover**). A full post keeps the rest in transit; a cancelled "
                 "request, or one whose post is broken, takes its cargo back first."),
                ("text", "Requests",
                 "Use the post with an empty hand to read its last steps. **/jugcraft concordance logistics** lists "
                 "your requests; **request** asks for a number of what you hold; **cancel** and **recover** take a "
                 "request's number:\\\n" + states),
            ],
        },
    }


# ------------------------------------------------------------------------------------------------- data and assets

def tags(tags):
    tags.add("block", "minecraft:mineable/axe", rid("courier_post"))


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    write(assets / "models" / "block" / "courier_post.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": rid("block/courier_post_top"), "side": rid("block/courier_post_side"),
                     "bottom": rid("block/courier_post_bottom")}})
    write(assets / "blockstates" / "courier_post.json", {"variants": {"": {"model": rid("block/courier_post")}}})
    write(assets / "items" / "courier_post.json", {"model": {"type": "minecraft:model", "model": rid("item/courier_post")}})
    write(assets / "models" / "item" / "courier_post.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/courier_post")}})
    write(data / "loot_table" / "blocks" / "courier_post.json", self_drop("courier_post"))
    write(data / "recipe" / "courier_post.json", dict({"fabric:load_conditions": condition("concordance")}, **{
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PBP", "PKP", "P P"],
        "key": {"P": "#minecraft:planks", "B": "minecraft:barrel", "K": "minecraft:writable_book"},
        "result": {"id": rid("courier_post"), "count": 1}}))


def write_data(write, data):
    """Logistics keeps no data-driven definitions: its rules are the ledger's (see the numbers above)."""
