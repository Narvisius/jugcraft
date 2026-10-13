"""Roadmap step 27: presentation that shows what really happened (concordance/sign and the client's SignClient).

Every Concordance event a player should see is shown by a sign, sent by the server only when the event happened and drawn
by each client in its own particles, as many as its visual intensity allows. The five kinds (preparation, execution,
success, shortage, danger) differ in shape and movement and the two warnings in sound, so none is told by colour alone.
This module holds the signs, the warnings' sounds, the intensities, the words and the codex page;
tools/check_mod_data.py (check_signs) checks the Java against them.
"""

MOD = "jugcraft"

# Sign id -> (kind, particles at full intensity close by). Keep equal to concordance/sign/Sign.java, in its order.
SIGNS = {
    "gather": ("preparation", 12),
    "work": ("execution", 4),
    "flow": ("execution", 8),
    "done": ("success", 24),
    "want": ("shortage", 8),
    "peril": ("danger", 12),
    "harm": ("danger", 6),
    "mend": ("success", 6),
    "shove": ("execution", 6),
    "light": ("execution", 6),
    "charm": ("execution", 6),
    "touch": ("execution", 6),
    "unmake": ("execution", 6),
}

# The kinds, in Sign.Kind's order, and what each looks like (the codex page says the same in its own words).
KINDS = {
    "preparation": "Preparation: glyphs drawn in to the place being readied",
    "execution": "Execution: light rising where work was done, or travelling along a real transfer",
    "success": "Success: a burst of light",
    "shortage": "Shortage: grey smoke and sinking ash, with a hollow falling tone",
    "danger": "Danger: flame and sparks thrown outwards, with a sharp rising warning",
}

# The warnings' own sounds (tools/concordance_sounds.py draws them): event -> subtitle.
SOUND_EVENTS = {
    "concordance.sign_shortage": "Something is lacking",
    "concordance.sign_danger": "Danger",
}

# Intensity id -> (particles a tick, share of each sign in percent, ambient rarity: 0 none). Keep equal to
# Presentation.Intensity.
INTENSITY = {
    "full": (160, 100, 1),
    "reduced": (64, 50, 3),
    "minimal": (16, 0, 0),
}
# Presentation's other numbers: how far signs are sent and drawn, where they halve, the warnings' floor, the ambient
# share, and the server's per-level limits a tick (Signs.PER_TICK, Signs.WARNING_RESERVE).
FAR = 48
NEAR = 24
WARNING_FLOOR = 2
AMBIENT_SHARE = 50
PER_TICK = 48
WARNING_RESERVE = 16

CLIENT = {
    "screen.jugcraft.concordance.config.intensity": "Visual intensity",
    "screen.jugcraft.concordance.config.intensity.tooltip": "How many particles the Concordance draws for its signs and "
                                                            "its blocks. Warnings (shortage and danger) always show.",
    "screen.jugcraft.concordance.config.intensity.full": "Full",
    "screen.jugcraft.concordance.config.intensity.reduced": "Reduced",
    "screen.jugcraft.concordance.config.intensity.minimal": "Minimal (warnings only)",
}


def lang_entries(lang):
    lang.update(CLIENT)


def codex():
    return {("foundations", "signs"): {
        "name": "Reading the Signs", "x": 2, "y": 4, "icon": "minecraft:spyglass", "condition": None,
        "description": "What the Concordance shows, and what it means",
        "pages": [
            ("text", "Five Signs",
             "When something happens, the Concordance shows it where it happened, and only when it happened:\n"
             "**Preparation**: glyphs drawn in, as a circle gathers.\n"
             "**Execution**: light rising where work was done, or travelling along a real transfer: Ley Charge from a "
             "pylon to the circle it feeds, Radiance from a lantern into a pylon.\n"
             "**Success**: a burst of light."),
            ("text", "Two Warnings",
             "**Shortage**: grey smoke and sinking ash, with a hollow falling tone: something it needs is missing. A "
             "pylon short of charge, a missed upkeep, a formula without its ingredient, a worker that cannot go on.\n"
             "**Danger**: flame and sparks thrown outwards, with a sharp rising warning: a circle's containment failing, "
             "a searing stir, a damaged spire.\n"
             "Both sounds have subtitles."),
            ("text", "Things As They Are",
             "A crucible's liquid stands at its real volume, in the colour of its strongest property, muddied when it "
             "is murky. A searing crucible smokes and spits. A spire shows whether it is being raised, working, lacking "
             "or damaged. A worker that lacks something waits, tapping or looking about; one held up on its way stands "
             "still. A finished circle glows until its result is taken."),
            ("text", "Visual Intensity",
             "The Concordance settings choose how much is drawn: **Full**, **Reduced** or **Minimal** (warnings only). "
             "Large workshops draw no more than a set amount each moment, and warnings are never hidden. **Reduced "
             "motion** keeps everything calmer."),
        ]}}
