"""Concordance Chunk C, C-01 (plan 03:9; C-P6, C-P22): one diagnostic vocabulary for recipes, rituals, stations and
couriers. A diagnostic is a stable code and its parameters (Java: concordance/diagnostic/Code.java and Diagnostic.java,
pure), worded in the player's language through compose.jugcraft.diagnostic.<code> on either side, never prose built on
the server. A code is never renamed, reused or removed once released; new ones are appended. tools/check_mod_data.py
(check_diagnostics) holds Code.java to this table, in its order.
"""

MOD = "jugcraft"

# Code.Kind, in its order: shortage and blocked show a WANT sign when they begin, fault a PERIL, condition (heat, a hold,
# light, an occurrence) nothing.
KINDS = ("shortage", "blocked", "condition", "fault")

# id -> (kind, words). Plan 03:9's list first, in its order; then Chunk C's. Codes no unit raises yet take their
# parameters and fuller words from the unit that first raises them.
CODES = {
    "missing_ingredient": ("shortage", "Needs %s more: %s"),
    "wrong_preparation": ("shortage", "Needs %s, but its buffer holds %s"),
    "wrong_heat": ("condition", "Waiting for %s heat; it is %s"),
    "unavailable_power": ("shortage", "%s holds %s; it needs %s"),
    "insufficient_capacity": ("blocked", "Its output is full: take some out"),
    "missing_permission": ("blocked", "You may not change this here"),
    "unloaded_target": ("blocked", "What it works with is not loaded"),
    "incomplete_pattern": ("blocked", "%s is not complete: faults to mend: %s"),
    "unavailable_worker": ("shortage", "No worker is free for it"),
    # Concordance Chunk D, D-02: raised by a holder block whose Bound Will is kept elsewhere or for someone else (a copy).
    "conflicting_agreement": ("blocked", "Another agreement already covers it: this is a copy, so it does nothing and releases nothing"),
    "dormant_occurrence": ("condition", "It waits for its occurrence"),
    "needs_water": ("shortage", "Needs %s parts of water in its tank; it holds %s"),
    "needs_container": ("shortage", "Needs a glass bottle or a bowl to bottle into"),
    "held": ("condition", "Preserve holds it %s for %s s more"),
    "incompatible": ("fault", "Step %s (%s) cannot be made under the rules now: %s"),
    "courier_quota": ("blocked", "Needs %s more: %s; the couriers take no more of its setter's requests now, so it asks again in %s s"),
    "output_differs": ("blocked", "Its output holds something else: take it out"),
    "needs_emptying": ("blocked", "A repeat starts from an empty crucible: bottle what is in it"),
    "unreadable": ("fault", "Its formula can no longer be read: set it again, or clear it (sneak with an empty hand)"),
    # Concordance Chunk C, C-03 (ritual readiness).
    "not_lent": ("blocked", "%s belongs to someone not taking part: it lends only to rites its owner joins"),
    "needs_focus": ("shortage", "You have %s Focus; it takes %s"),
    "too_bright": ("condition", "Too bright: light %s above the anchor, where it allows at most %s"),
    "too_far": ("blocked", "Too far: stand within %s blocks of the anchor across and %s above or below"),
    "participants": ("shortage", "%s of %s participants have joined; the rest have %s s to join (an empty-hand use)"),
    # Concordance Chunk C, C-05 (the Crucible Stand).
    "no_crucible": ("blocked", "Nothing to heat: set an Alembic Crucible on it"),
}


def lang_entries(lang):
    for key, (_, words) in CODES.items():
        lang[f"compose.{MOD}.diagnostic.{key}"] = words
