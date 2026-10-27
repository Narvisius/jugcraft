"""Herobrine (docs/features/herobrine.md): a still, staring figure with glowing white eyes who appears to players at a
distance, now and then right behind them, and is gone as soon as anyone comes close, stares back or hits him. He never
attacks, changes the world or speaks.

Java: creatures/herobrine/Herobrine (the entity, who saw him, why he went), HerobrineEncounters (the entity type, the live
tick, placing him, operators' commands) and HerobrineDirector (the seam a schedule implements to decide who is due an
encounter and when); client/HerobrineRenderer draws him. His body is the game's own default player texture, read from the
installed game at run time, so no game asset is copied into Jugcraft; this module draws only his eyes: a clear 64x64 skin
layer with the four eye pixels white, drawn full-bright over that face.
"""
from PIL import Image

MOD = "jugcraft"
ENTITY = "herobrine"
NAME = "Herobrine"
# Keep in step with Herobrine and HerobrineEncounters (tools/check_mod_data.py checks them).
VANISH_DISTANCE = 12.0
STARE_TICKS = 40
DISTANT_TICKS = 600
CLOSE_TICKS = 60
CLOSE_STARE_TICKS = 12
SEEN_TICKS = 10
GRACE_TICKS = 1200
IDLE_TICKS = 1800
CLOSE_DARKNESS_TICKS = 60
# The server option (config/jugcraft.properties) that switches the scheduled encounters off; commands work either way.
OPTIONS = {"herobrine.haunting": "on", "herobrine.event_last_day": "2026-11-03",
           "herobrine.timezone": "America/New_York", "herobrine.marks": "on"}
# His eyes: the two pixels of each eye on the face (the head's front is u 8-15, v 8-15 in the player skin layout).
EYES = ((9, 12), (10, 12), (13, 12), (14, 12))

FAILURES = {
    "target_gone": "%s is not in the Overworld",
    "target_unsafe": "%s is not calm enough for a close encounter",
    "already_haunted": "Herobrine is already out for %s",
    "no_visible_spot": "Herobrine found nowhere %s could see him",
}

# /jugcraft herobrine marks|mark: listing, undoing and forcing the marks he leaves (creatures/herobrine/HerobrineMarks).
MARK_MESSAGES = {
    "none": "Herobrine has left no marks",
    "entry": "%s: %s at %s %s %s, %s blocks changed",
    "unknown": "No mark %s",
    "undone": "Mark %s taken back: %s blocks restored, %s left as players changed them, %s waiting on unloaded land",
    "unknown_kind": "No such mark: %s (shrine, mossy_pyramid, sand_pyramid, sign, grove or tunnel)",
    "no_site": "Nowhere near is wild enough for a %s",
    "left": "Herobrine is leaving a %s (mark %s)",
}

# What Styxhexenhammer says to someone who has seen Herobrine: he recognises it, and will not explain it.
STYX_LINES = [
    "You have the look of someone who has been watched. I know the look. I will not tell you what watched you.",
    "White eyes at the edge of the trees? The observatory's oldest logs have a word for that. I am not going to say it.",
    "Some things are studied. Some are only endured. Let us both pretend you did not ask.",
    "If he leaves you a message, do not answer it. I said nothing.",
]

# /jugcraft herobrine wilds: the land an operator approves for the marks that take blocks away (groves and tunnels).
WILD_MESSAGES = {
    "none": "No land is approved for Herobrine's groves and tunnels; he leaves only shrines, pyramids and signs",
    "entry": "%s: %s blocks round %s %s %s, approved by %s",
    "approved": "Herobrine may strip leaves and dig tunnels within %s blocks of %s %s %s",
    "revoked": "Approval %s withdrawn; marks already made stay until undone",
    "unknown": "No approval %s",
    "overworld": "Wild land can only be approved in the Overworld, where he walks",
}


def write_all(lang):
    lang[f"entity.{MOD}.{ENTITY}"] = NAME
    lang[f"commands.{MOD}.herobrine.status.on"] = "Herobrine's haunting is on: %s encounters under way, %s players online"
    lang[f"commands.{MOD}.herobrine.status.off"] = "Herobrine's haunting is off (herobrine.haunting=off)"
    lang[f"commands.{MOD}.herobrine.status.no_schedule"] = \
        "No schedule is installed: Herobrine appears only by /jugcraft herobrine sighting or scare"
    lang[f"commands.{MOD}.herobrine.sighting"] = "Herobrine is watching %s from a distance"
    lang[f"commands.{MOD}.herobrine.scare"] = "Herobrine stands behind %s"
    lang[f"commands.{MOD}.herobrine.unknown_variant"] = "No such encounter: %s (watcher, follower, second_look or tunnel)"
    for failure, text in FAILURES.items():
        lang[f"commands.{MOD}.herobrine.failed.{failure}"] = text
    for key, text in MARK_MESSAGES.items():
        lang[f"commands.{MOD}.herobrine.marks.{key}"] = text
    for key, text in WILD_MESSAGES.items():
        lang[f"commands.{MOD}.herobrine.wilds.{key}"] = text
    for i, line in enumerate(STYX_LINES):
        lang[f"message.{MOD}.styx.herobrine.{i}"] = "Styxhexenhammer: " + line


def eyes_texture():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for x, y in EYES:
        img.putpixel((x, y), (255, 255, 255, 255))
    return img


def draw_all(save):
    save(eyes_texture(), "entity", f"{ENTITY}_eyes")
