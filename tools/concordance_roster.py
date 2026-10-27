"""Concordance Chunk D, D-07 (D-S11): the roster's server side (Java: concordance/spirits/Roster.java,
RosterRequestPayload.java, RosterStatePayload.java): its limits, actions, row types, icons, gates, answers and words, the
journal's pacts lines (Roster.report, called from StolasRegard.report) and the roster kinds of Ars Nouveau's helpers (G13);
then the roster screen's words (client/RosterClient.java, RosterScreen.java; the client section below). Merged by
tools/concordance.py (MESSAGES into its MESSAGES; lang_entries from write_all); checked by tools/check_mod_data.py
check_roster and check_roster_screen. English only (D-S21).
"""

import lairs

MOD = "jugcraft"

# The most rows one roster payload carries (Java: RosterStatePayload.MAX_ROWS). 32 Bound Wills, 16 requests and 32 workers
# can be 80: records first, then requests, then workers; the rest are counted (critique 17).
MAX_ROWS = 64
# The actions a roster request names, by index (Java: RosterRequestPayload.ACTIONS). Append only.
ACTIONS = ["open", "suspend", "resume", "end", "cancel", "recover"]
# Row types by index, kept on the server only, never sent (Java: Roster.RECORD, REQUEST, WORKER).
TYPES = ["record", "request", "worker"]
# Why a row waits, as a mark beside its words, never colour alone (Java: RosterStatePayload.ICONS and its numbers). Append only.
ICONS = ["active", "waiting", "suspended", "upkeep", "unloaded", "lost", "without_place", "unclaimed", "in_transit",
         "stranded", "returning", "disabled"]
# The gates, in ticks (Java: Roster): opening its own; suspend/resume/end the Spirit Anchor's use's; cancel/recover the
# logistics commands'.
GATES = {"roster": 10, "workers": 4, "logistics": 10}
# The answers the roster gives itself (Java: Roster.NOT_LISTED, NOT_PERMITTED; PactMenu.CHANGED, TOO_FAST, NOT_OFFERED,
# DISABLED); the others are AgreementHolder's and the courier ledger's outcomes, lower-cased.
ANSWERS = ["not_listed", "changed", "too_fast", "not_offered", "not_permitted", "disabled"]
# Each service a record can hold (BoundWill.Service.work; concordance_workers.WORK and concordance_pacts.WORK).
SERVICES = {
    "gather": "Gathers what it finds in its area into its anchor",
    "courier": "Carries its Courier Post's requests",
    "star_reckoning": "Star-reckoning: once a game day, a sky pattern you observed, recalled",
}
# Ars Nouveau's helpers on the roster (G13; Java: nouveau/NouveauHelper.kind is "nouveau_" + its species,
# NouveauHelpers.register lists them).
HELPER_KINDS = {"nouveau_drygmy": "Drygmy", "nouveau_whirlisprig": "Whirlisprig", "nouveau_wixie": "Wixie",
                "nouveau_amethyst_golem": "Amethyst Golem", "nouveau_alakarkinos": "Alakarkinos"}
KIND_SERVICES = {
    "familiar": "Keeps near you and mends you",
    "spirit": "Serves its agreement at its anchor",
    "construct": "Carries along its route, or for its Courier Post",
    "pact": "Serves its pact at its perch",
    "starbuncle": "Carries between its two chests",
    **{kind: "Works for you at its home" for kind in HELPER_KINDS},
}
# The portrait of a worker that is not loaded, by kind (Java: Roster.KIND_PORTRAITS); any other kind is jugcraft:<kind>,
# which names the Ars Nouveau helpers' and the starbuncle's entity types. A spirit has none ("": the client draws the Spirit
# Anchor), since a Gathering Shade's kind and a Workshop Hand's are both spirit (the unit's critique 8). A loaded worker is
# drawn as its own entity type, and a record's row as its counterpart.
KIND_PORTRAITS = {"familiar": "jugcraft:hearthling", "spirit": "", "construct": "jugcraft:clockwork_porter",
                  "pact": "jugcraft:stolas"}
# A place's dimension in words (Java: Roster.place; any other is its id): vanilla's three, each lair by its own name
# (tools/lairs.py LAIRS' display, as lair.jugcraft.<lair> says it, its "The" lower-cased mid-sentence) and a Planarium's
# cells by the block's name (block.jugcraft.nouveau_planarium; the unit review, D8). check_roster holds it to
# data/jugcraft/dimension.
DIMENSIONS = {"minecraft:overworld": "the Overworld", "minecraft:the_nether": "the Nether", "minecraft:the_end": "the End",
              **{f"{MOD}:{lair}": "t" + spec["display"][1:] if spec["display"].startswith("The ") else spec["display"]
                 for lair, spec in lairs.LAIRS.items()},
              f"{MOD}:nouveau_planarium": "a Planarium",
              # Main's Jimtafacts spellbooks (#475): its Pocket Dimension spell's place.
              f"{MOD}:jimtafacts_pocket_dimension": "a Pocket Dimension"}

MESSAGES = {
    "roster.answer.not_listed": "That is not on your roster as you last read it; read it again. Nothing was done",
    "roster.answer.changed": "It has changed since you read it; read it again. Nothing was done",
    "roster.answer.too_fast": "One moment",
    "roster.answer.not_offered": "That cannot be done to it now",
    "roster.answer.not_permitted": "Only you, alive and in person, may do that",
    "roster.kind.request": "Courier request",
    "roster.request": "Request #%s: %s × %s",
    "roster.request.unknown_item": "an item this world no longer knows",
    "roster.service.request": "To be delivered at %s",
    "roster.service.unknown": "Its terms are kept at its place",
    "roster.place": "%s %s %s in %s",
    "roster.place.none": "Without its place",
    "roster.place.unclaimed": "Where it was sealed (not yet claimed)",
    "roster.task.carrying": "Carrying request #%s (%s aboard)",
    "roster.task.claimed": "On request #%s",
    "roster.task.none": "Nothing now",
    "roster.next.offering": "Next offering: %s %s in %s game days (%s of at most %s periods paid ahead)",
    "roster.next.at_visit": "Next offering: %s %s when you are next near its place (time away is never counted)",
    "roster.next.warned": "An offering is due: %s %s within %s game days of grace, or it is suspended (nothing is lost)",
    "roster.next.held": "Suspended for its upkeep: %s %s resumes it",
    "roster.next.suspended": "Suspended by you: nothing falls due until you resume it",
    "roster.next.quota": "%s of %s tasks done today",
    "roster.next.none": "Nothing is owed",
    "roster.next.delivered": "%s of %s delivered, %s carried",
    "roster.waiting.active": "Serving",
    "roster.waiting.unclaimed": "Kept where it was sealed, not yet claimed: it is claimed when that place next loads",
    "roster.waiting.without_place": "Without its place: seat it at a new place of yours, or end it",
    "roster.waiting.stranded_place": "Its place keeps it no longer: seat it at a new place of yours, or end it",
    "roster.waiting.lost": "Its presence is lost: it comes back to its place, at no cost, while you are near",
    "roster.waiting.unloaded": "Its place is not loaded: it waits there, and nothing is owed meanwhile",
    "roster.waiting.reserved": "%s reserved at %s",
    "roster.waiting.reserved_unloaded": "%s reserved at %s, which is not loaded: they stay in that container until it loads",
    "roster.waiting.in_transit": "Aboard its courier",
    "roster.waiting.destination_unloaded": "Aboard its courier, which waits: where it goes is not loaded",
    "roster.waiting.stranded": "Stranded at its post: recover it yourself, or another courier takes it up",
    "roster.waiting.returning": "Being taken back where it came from",
    "roster.waiting.disabled": "The Concordance is switched off on this server: nothing is done",
    "roster.confirm.end": "End your agreement with %s? Its Bound Will is released and it departs; %s offerings kept for it "
                          "come back to you, once",
    "roster.confirm.end_none": "End your agreement with %s? Its Bound Will is released and it departs",
    "roster.confirm.cancel": "Cancelling request #%s closes it; cargo in transit (%s) is first taken back where it came from",
    # The unit review (D10): with nothing in transit, the hint says nothing of cargo.
    "roster.confirm.cancel_none": "Cancelling request #%s closes it",
    **{f"roster.service.{work}": text for work, text in SERVICES.items()},
    **{f"roster.kind_service.{kind}": text for kind, text in KIND_SERVICES.items()},
    **{f"roster.dimension.{dim.replace(':', '.')}": text for dim, text in DIMENSIONS.items()},
    **{f"workers.kind.{kind}": name for kind, name in HELPER_KINDS.items()},
}

JOURNAL = {
    "journal.jugcraft.pacts.kept": "%s (%s): %s",
    "journal.jugcraft.pacts.departed": "%s departed on game day %s: %s",
}


# ------------------------------------------------------------------------------------- D-07: the roster screen's words
# A row's state word beside its mark, keyed by ICONS (Java: client/RosterScreen.stateWord; check_roster_screen).
STATES = {
    "active": "At work",
    "waiting": "Waiting",
    "suspended": "Suspended",
    "upkeep": "Upkeep",
    "unloaded": "Not loaded",
    "lost": "Lost",
    "without_place": "Without its place",
    "unclaimed": "Not yet claimed",
    "in_transit": "In transit",
    "stranded": "Stranded",
    "returning": "Going back",
    "disabled": "Switched off",
}
ROWS_PER_PAGE = 8  # client/RosterScreen.ROWS_PER_PAGE
REFRESH_TICKS = 40  # client/RosterClient.REFRESH_TICKS

CLIENT = {
    "key.jugcraft.concordance_roster": "Concordance Roster",
    "screen.jugcraft.roster.title": "Roster",
    "screen.jugcraft.roster.page": "Page %s of %s · %s listed",
    "screen.jugcraft.roster.opening": "Reading your roster...",
    "screen.jugcraft.roster.unavailable": "This server keeps no roster: the Arcane Concordance is not on it.",
    "screen.jugcraft.roster.disabled": "The Concordance is switched off on this server: your roster is shown, and nothing can be done from it.",
    "screen.jugcraft.roster.empty": "Nothing is on your roster yet: no pact, agreement, familiar, spirit, construct or courier request of yours.",
    "screen.jugcraft.roster.empty.hint": "Bond a Hearthling, seal a Spirit Anchor or a pact, or file a request at a Courier Post, "
                                         "and it is listed here.",
    "screen.jugcraft.roster.more": "%s more are not listed: the roster shows at most %s. The journal, and the workers and logistics "
                                   "commands, list them all.",
    "screen.jugcraft.roster.waiting": "Asking the server...",
    "screen.jugcraft.roster.row.title": "%s · %s",
    "screen.jugcraft.roster.row.state": "%s: %s",
    "screen.jugcraft.roster.row.loaded": "%s (loaded)",
    "screen.jugcraft.roster.row.unloaded": "%s (not loaded: as it last reported)",
    "screen.jugcraft.roster.row.task": "Now: %s",
    "screen.jugcraft.roster.row.obligation": "Next: %s",
    "screen.jugcraft.roster.row.usage": "Its actions follow it: Tab moves to them",
    "screen.jugcraft.roster.row.usage.none": "Nothing to do for it from the roster",
    "screen.jugcraft.roster.suspend": "Suspend",
    "screen.jugcraft.roster.suspend.hint": "%s: its service stops; nothing is taken and nothing is lost",
    "screen.jugcraft.roster.resume": "Resume",
    "screen.jugcraft.roster.resume.hint": "%s: its service starts again",
    "screen.jugcraft.roster.end": "End...",
    "screen.jugcraft.roster.end.hint": "%s: asks first; then it leaves, and anything held for it comes back to you once",
    "screen.jugcraft.roster.end.title": "End the agreement with %s?",
    "screen.jugcraft.roster.end.confirm": "%s (%s) leaves, and anything held for it comes back to you once. This cannot be undone.",
    "screen.jugcraft.roster.end.yes": "End it",
    "screen.jugcraft.roster.end.no": "Keep it",
    "screen.jugcraft.roster.cancel": "Cancel",
    "screen.jugcraft.roster.cancel.hint": "%s: closes the request, or first sends what is carried back where it came from",
    "screen.jugcraft.roster.recover": "Recover",
    "screen.jugcraft.roster.recover.hint": "%s: the stranded items come to your inventory, once",
    "screen.jugcraft.roster.previous": "Previous",
    "screen.jugcraft.roster.previous.hint": "The roster's previous page (Page Up)",
    "screen.jugcraft.roster.next": "Next",
    "screen.jugcraft.roster.next.hint": "The roster's next page (Page Down)",
    "screen.jugcraft.roster.refresh": "Refresh",
    "screen.jugcraft.roster.refresh.hint": "Asks the server for your roster again",
    "screen.jugcraft.roster.close": "Close",
    "screen.jugcraft.roster.help": "Tab and Shift+Tab move between entries and their actions; Enter or Space presses; Page Up "
                                   "and Page Down turn the page; Esc closes. Ending asks first.",
}
# What ConcordanceRosterClientGameTests photographs (check_roster_screen).
CLIENT_SHOTS = ["jugcraft_roster_empty", "jugcraft_roster", "jugcraft_roster_scale4", "jugcraft_roster_small",
                "jugcraft_roster_suspended", "jugcraft_roster_end_confirm", "jugcraft_roster_ended", "jugcraft_roster_recovered",
                "jugcraft_roster_full", "jugcraft_roster_full_small"]
# D-09 (D-P135): the workshop hand at work, in the same class.
CLIENT_SHOTS += ["jugcraft_workshop_hand_at_work"]


def lang_entries(lang):
    """The journal's pacts lines, the roster screen's words and its key's name (MESSAGES go through tools/concordance.py's)."""
    lang.update(JOURNAL)
    lang.update(CLIENT)
    for state, word in STATES.items():
        lang[f"screen.{MOD}.roster.state.{state}"] = word
