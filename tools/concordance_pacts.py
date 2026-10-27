"""Pacts with the Goetia's spirits, Stolas first (Concordance Chunk D; docs/features/concordance-chunk-d.md and
docs/features/stolas-pact.md).

D-03 (D-S12): Stolas's Regard, the research a player completes before Stolas will hear them, found through
Styxhexenhammer's hints or, without him, through the codex's Binding category and a feather.
- Its evidence is personal and server-made, and each kind is the deed itself: examine a thing Stolas regards
  (#jugcraft:stolas_specimens: three of Styx's flowers and a feather, which anyone has without Styx); harvest a ripe
  organism crop by hand (the Greenwardens' cultivation); dissolve a sample at the Assayer's Scale (the Balancewrights'
  assay); and, for mastery, keep a pact's first obligation (the practice jugcraft:pact_service, which only the pact
  records, D-05; its gate is tools/concordance_progression.py's PRACTICES). No state takes notes.
- The sky step (one observation recorded at an Orrery Observatory) is the invitation's precondition, not research, so
  Styx and the journal name it apart: ResearchEngine.explain does not cover it.
- In the game only the Binding Arts gate the entry (its requires); the practices' gates put Verdant Husbandry and the
  Assay understood before their practice in the progression graph, which so asks more than the game, never less.
D-04 (D-S6, D-S7, D-S8, D-S13 to D-S15, D-S19, D-S23): the pact perch, Stolas's offer as data, the Open Hand Compact
and the pact menu's protocol. D-05 (D-S1, D-S3, D-S6, D-S12, D-S16): Star-reckoning, the first obligation and keeping.

Java: concordance/spirits/StolasRegard.java, PactPerchBlock(Entity), PactMenu, PactRequestPayload, PactStatePayload,
PactSessions, PactCompact, PactKeeping, StarReckoning, StolasEntity; concordance/worker/PactDefinition, PactParser,
PactCatalog; client/PactClient, PactScreen and WorkerClient (Stolas's raven body, D-06 (D-S5): the Flashjack rig's
geometry copied by tools/owner_art.py from OWNER_FILES, its skin, glowmask and animations adapted by tools/stolas_body.py
from ADAPTED). tools/concordance.py merges these tables into its own; Styx's words are tools/styx.py's;
tools/check_mod_data.py (check_stolas_regard, check_pacts, check_pact_screen, check_stolas_body) holds the Java and the
data to them.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


REGARD = rid("stolas_regard")  # Java: spirits/StolasRegard.RESEARCH
PACT_SERVICE = rid("pact_service")  # Java: StolasRegard.ACTIVITY (its gate: concordance_progression.PRACTICES)
CULTIVATION = rid("cultivation")  # Java: garden/Garden.ACTIVITY
ASSAY = rid("assay")  # Java: assay/Assaying.ACTIVITY
STOLAS_SPECIMEN_TAG = f"{MOD}:stolas_specimens"  # Java: StolasRegard.SPECIMENS
STYX_SPECIMENS = {rid("stolas_starflower"): "Stolas Starflower", rid("ravenquill_lupine"): "Ravenquill Lupine",
                  rid("amethyst_mourningbell"): "Amethyst Mourningbell"}
# The specimen anyone has without Styx, who is missing from old saves and never respawned (G12; CLAUDE.md).
PLAIN_SPECIMENS = {"minecraft:feather": "Feather"}
# What Stolas waits for next (Java: StolasRegard.Step, in this order); Styx's words for each are tools/styx.py's.
STEPS = ["binding", "examine", "cultivate", "assay", "serve", "kept", "journal"]

RESEARCH = {"stolas_regard": {
    "name": "Stolas's Regard", "principle": "tether", "tradition": "spiritbinders", "stage": "practitioner",
    "icon": rid("stolas_starflower"),
    "requires": [{"research": rid("binding_arts"), "state": "understood"}],
    "states": {
        "encountered": [{"type": "examine", "specimens": f"#{STOLAS_SPECIMEN_TAG}"}],
        "observed": [{"type": "practice", "activity": CULTIVATION}],
        "understood": [{"type": "practice", "activity": ASSAY}],
        "mastered": [{"type": "practice", "activity": PACT_SERVICE}],
    },
    "unlocks": {},
    "advancements": {
        "encountered": ("Regarded", "Examine something Stolas would look at closely: a feather, or one of "
                        "Styxhexenhammer's flowers", "task", rid("stolas_starflower")),
        "observed": ("What Grows", "Harvest a ripe organism crop by hand", "task", rid("sunpetal")),
        "understood": ("Stolas's Regard", "Understand Stolas's Regard: dissolve a sample at an Assayer's Scale", "goal",
                       rid("assayers_scale")),
        "mastered": ("Star-Reckoned", "Keep your pact with Stolas: record a sky observation while it holds", "challenge",
                     rid("observatory")),
    },
    "locked": {"encountered": "Examine a feather or one of Styxhexenhammer's flowers first",
               "observed": "Harvest an organism crop first", "understood": "Understand Stolas's Regard first",
               "mastered": "Master Stolas's Regard first"},
}}

MESSAGES = {"examine.regard": "You look at the %s as closely as Stolas would"}
TOOLTIPS = {"stolas_specimen": "Stolas would look at this closely: sneak and use to examine it"}
# The journal's pacts section (Java: StolasRegard.report): Stolas's Regard's next step, and the sky step apart.
JOURNAL = {
    "pacts.regard.binding": "Stolas's Regard: understand the Binding Arts first",
    "pacts.regard.examine": "Stolas's Regard: examine a feather or one of Styxhexenhammer's flowers (sneak and use it)",
    "pacts.regard.cultivate": "Stolas's Regard: harvest a ripe organism crop by hand",
    "pacts.regard.assay": "Stolas's Regard: dissolve a sample at an Assayer's Scale",
    "pacts.regard.serve": "Stolas's Regard is understood: invite him at a pact perch and keep the pact's first obligation",
    "pacts.regard.kept": "Stolas's Regard is mastered",
    "pacts.regard.journal": "Stolas's Regard: the Research section says what comes next",
    "pacts.sky_needed": "Before Stolas will come: record a sky observation at an Orrery Observatory while a pattern is "
                        "up there (this is not research, and nothing is recorded for a sky you did not watch)",
    "pacts.sky_recorded": "A sky observation is recorded: Stolas will come when invited",
    "pacts.sky_count": "(%s patterns observed)",
}


# ------------------------------------------------------------------------------------------- D-04: the pact perch

# Java: worker/PactParser.java (check_pacts compares).
PARSER = {"MAX_COMPACT_ITEMS": 4, "MAX_COMPACT_COUNT": 64, "MAX_RESONANCE": 32, "MAX_BEATS": 3}
WORK = ["star_reckoning"]
# The works that serve only their holder (Java: PactParser.HOLDER_ONLY): a definition with one must be "personal".
HOLDER_ONLY = ["star_reckoning"]
# Java: spirits/PactStatePayload.java (MAX_*), PactSessions.java (TIMEOUT_TICKS), PactMenu.java (RATE_TICKS),
# PactPerchBlockEntity.java (LOOK_TICKS), PactKeeping.java (MAX_OFFERINGS).
STATE_LIMITS = {"MAX_BEATS": 3, "MAX_TERMS": 16, "MAX_NOTES": 12, "MAX_CHOICES": 8, "MAX_COMPACT": 8, "MAX_KEY": 64,
                "MAX_ID": 32767}
SESSION = {"TIMEOUT_TICKS": 6000, "RATE_TICKS": 4, "LOOK_TICKS": 20, "MAX_OFFERINGS": 8}
# Sent as numbers or ids (PactRequestPayload.ACTIONS, PactStatePayload's modes and TERMS, PactMenu's own reasons): append only.
ACTIONS = ["choose", "accept", "decline", "recall", "deposit", "suspend", "resume", "end", "seat"]
MODES = ["negotiate", "keep", "seat", "closed"]
TERMS = ["service", "hours", "area", "quota", "upkeep", "grace", "suspension", "duration", "breach", "ending", "returns", "perch",
         "beneficiaries", "compact"]
ANSWERS = ["changed", "too_fast", "not_offered", "sealed", "declined", "disabled", "no_definition", "missing_evidence",
           "no_observation", "deposited", "nothing_deposited"]
# The offerings (D-S3): the three Styx flowers and, reachable without Styx, an amethyst shard. The tag ships with the
# definition (D-04); D-05's upkeep consumes it.
OFFERINGS_TAG = rid("stolas_offerings")
OFFERINGS = [rid("stolas_starflower"), rid("ravenquill_lupine"), rid("amethyst_mourningbell"), "minecraft:amethyst_shard"]
# Stolas's offer (data/jugcraft/concordance/pact/stolas.json; Java: worker/PactParser.java). Starting values, recorded in the
# As built note: 8 blocks round the perch, any hour, once a game day; one offering every 7 game days, a day of grace, up to
# four ahead (D-S3); the Open Hand Compact two amethyst shards, three feathers and 8 Astral Resonance (D-S13). Ending a
# pact and sealing it anew could give a second Star-reckoning in one game day, but only for a whole new compact (its
# materials and resonance) each time, which makes nothing: no positive-gain loop (critique 10; the balance note says so).
PACTS = {
    "stolas": {
        "agreement": rid("stolas_pact"),
        "research": rid("stolas_regard"),
        "service": {"work": "star_reckoning", "radius": 8, "from": 0, "to": 24000, "quota": 1},
        "upkeep": {"offering": OFFERINGS_TAG, "count": 1, "period_days": 7, "grace_days": 1, "cap": 4},
        "beneficiaries": "personal",
        "compact": {"items": [{"item": "minecraft:amethyst_shard", "count": 2}, {"item": "minecraft:feather", "count": 3}],
                    "resonance": 8},
        "beats": ["greeting", "service", "price"],
    },
}
BLOCKS = {"pact_perch": {"name": "Pact Perch"}}
ENTITY_NAMES = {"stolas": "Stolas"}
PERCH_TOOLTIP = ("Use: invite Stolas, once you understand Stolas's Regard and have observed a star; or keep the pact seated "
                 "here. Break it and the pact stays yours, without its perch.")
# Polished deepslate, not a slab: the progression graph follows only listed vanilla items (a slab is not in
# concordance_progression.VANILLA_SOURCES).
RECIPE = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SAS", " L ", " D "],
          "key": {"S": "minecraft:stick", "A": "minecraft:amethyst_shard", "L": "minecraft:dark_oak_log",
                  "D": "minecraft:polished_deepslate"},
          "result": {"id": rid("pact_perch"), "count": 1}}


def _box(lo, hi, texture):
    return {"from": lo, "to": hi, "faces": {d: {"texture": f"#{texture}"} for d in ("north", "east", "south", "west", "up", "down")}}


# The block's shape is PactPerchBlock.SHAPE: a stone foot, a post and a crossbar (Jugcraft's own bench wood and anchor
# stone; provisional, D-06 may refine).
PERCH_MODEL = {"parent": "minecraft:block/block",
               "textures": {"particle": rid("block/lampwright_bench_wood"), "wood": rid("block/lampwright_bench_wood"),
                            "stone": rid("block/spirit_anchor_top")},
               "elements": [_box([3, 0, 3], [13, 2, 13], "stone"), _box([7, 2, 7], [9, 14, 9], "wood"),
                            _box([2, 14, 7], [14, 16, 9], "wood")]}

MESSAGES.update({
    "pact.perch_refused": "A pact perch stands only where you may build, placed by your own hand, and never in Styx's home",
    # Stolas's three beats (provisional words; his character is a checkpoint question).
    "pact.stolas.beat.greeting": "A raven settles on the perch and studies you with one bright eye. \"You have watched the sky "
                                 "and asked the flowers. I am Stolas. I teach the stars to those who keep their word.\"",
    "pact.stolas.beat.service": "\"Once a day, at this perch, I will turn the heavens back for you: any pattern you have seen "
                                "with your own eyes, recalled through your astrolabe.\"",
    "pact.stolas.beat.price": "\"In return, an offering every seven days, kept for me ahead. Miss one and I wait; I do not "
                              "punish. And now, the compact: an open hand.\"",
    # Each term's value (D-S15); the screen labels them (screen.jugcraft.pact.term.<key>).
    "pact.term.service.star_reckoning": "Star-reckoning: once a game day at this perch you recall any sky pattern you have "
                                        "observed yourself, paying its recall cost in Astral Resonance from your astrolabe",
    "pact.term.hours.any": "Any hour of the day or night",
    "pact.term.hours": "From %s to %s",
    "pact.term.area": "Within %s blocks of the perch",
    "pact.term.quota": "%s a game day",
    "pact.term.upkeep": "%s × %s every %s game days, paid ahead into the pact's keeping, at most %s periods; the Open Hand "
                        "Compact pays the first",
    "pact.term.grace": "A missed offering: a warning, then %s game day of grace",
    "pact.term.suspension": "The service stops; nothing is taken, nothing is lost, nothing accrues; an offering resumes it. "
                            "Time you are away never counts",
    "pact.term.duration": "Until you end it",
    "pact.term.breach": "Only a missed offering suspends it; nothing else breaches it, and it never ends by itself",
    "pact.term.ending": "At the perch, or from your roster",
    "pact.term.returns": "The offerings kept for it come back to you, once",
    "pact.term.perch": "If the perch is broken or blown up, the pact and its offerings stay yours: seat it at a new perch, or end it",
    "pact.term.beneficiaries.personal": "You alone",
    "pact.term.beneficiaries.party": "You and your party",
    "pact.term.compact": "Paid as you accept: %s Astral Resonance from your astrolabe, and %s",
    "pact.term.compact_item": "%s × %s",
    "pact.term.compact_paid": "Paid when the pact was sealed",
    # What is missing.
    "pact.missing.owner": "Only this perch's owner may invite Stolas here",
    "pact.missing.prerequisite": "Not yet: first understand %s",
    "pact.missing.step": "Not yet: %s, %s: %s",
    "pact.missing.observation": "Not yet: Stolas wants a star you have seen yourself. Record one at an observatory while a "
                                "pattern is up",
    "pact.missing.held": "You already keep a pact with Stolas",
    # Readings.
    "pact.reading.taken": "This perch keeps another's pact",
    "pact.reading.unreadable": "This perch's pact cannot be read by this version; it is kept as it was",
    "pact.seat.offer": "Your pact with Stolas is without its perch: seat it here, or end it",
    "pact.unavailable": "Stolas's terms could not be read on this server (the pact's definition did not load); nothing can be "
                        "agreed until it does",
    "pact.ended_here": "The pact is ended. Use the perch again to invite Stolas anew",
    # Answers.
    "pact.answer.sealed": "The pact is sealed: Stolas keeps this perch",
    "pact.answer.declined": "You decline; Stolas leaves. Nothing was spent",
    "pact.answer.changed": "The terms changed since you read them; read them again. Nothing was agreed or spent",
    "pact.answer.too_fast": "One moment",
    "pact.answer.not_offered": "That cannot be done here now",
    "pact.answer.missing_evidence": "Stolas will not agree yet: see what is missing. Nothing was spent",
    "pact.answer.cannot_afford": "The Open Hand Compact asks for the materials listed and %s Astral Resonance in one astrolabe; you "
                                 "are short. Nothing was spent",
    # Keeping (PactKeeping).
    "pact.deposited": "%s %s paid ahead (%s of at most %s periods now held)",
    "pact.deposit_none": "Nothing was taken: the escrow holds all it may, or you carry none of that offering",
    "pact.departed": "%s departs (%s); %s offerings came back to you",
    "pact.returned": "%s returns to the perch; nothing was spent",
    "pact.row.state.active": "The pact is active",
    "pact.row.state.suspended": "Suspended by you: Stolas reckons nothing and nothing falls due until you resume it",
    "pact.row.state.upkeep_due": "An offering is due: give %s %s within %s game days, or the pact is suspended (nothing is lost)",
    "pact.row.state.upkeep_held": "Suspended for its upkeep: nothing is taken and nothing accrues; %s %s resumes it",
    "pact.row.lost": "%s is not at the perch: he returns while you are near it, if your roster (at most %s) has room",
    "pact.row.next": "Next offering: %s %s in %s game days (time away is never counted)",
    "pact.row.escrow": "Paid ahead: %s of at most %s periods",
    "pact.row.area": "At this perch, within %s blocks, from %s to %s",
    "pact.row.area_any": "At this perch, within %s blocks, at any hour",
    "pact.row.end": "Ending returns what is paid ahead to you, once, and Stolas departs; breaking the perch ends nothing",
})


def lang_entries(lang):
    lang[f"tag.item.{STOLAS_SPECIMEN_TAG.replace(':', '.')}"] = "Things Stolas Regards"
    for key, text in JOURNAL.items():
        lang[f"journal.{MOD}.{key}"] = text
    for key, name in ENTITY_NAMES.items():
        lang[f"entity.{MOD}.{key}"] = name
    lang[f"container.{MOD}.pact_perch"] = BLOCKS["pact_perch"]["name"]
    lang[f"tooltip.{MOD}.pact_perch"] = PERCH_TOOLTIP
    lang[f"tag.item.{OFFERINGS_TAG.replace(':', '.')}"] = "Stolas's Offerings"
    screen_lang(lang)
    presence_lang(lang)


def codex():
    import concordance_celestial as celestial
    research = "stolas_regard"
    specimens = "\\\n".join(f"- {name}" for name in {**STYX_SPECIMENS, **PLAIN_SPECIMENS}.values())

    def when(pattern):
        info = celestial.PATTERNS[pattern]
        return f"every {info['period']} days {'in clear weather' if info['clear_sky'] else 'in any weather'}"
    entries = {("binding", research): {
        "name": "Stolas's Regard", "x": 0, "y": 4, "icon": rid("stolas_starflower"),
        "condition": ("binding_arts", "understood"), "description": "Stolas, the raven who teaches the stars: research",
        "pages": [
            ("text", "Stolas's Regard",
             "Stolas, a raven among the spirits, teaches the stars, herbs and stones, and treats only with someone who has "
             "done each for themselves. Once you understand the Binding Arts, **sneak and use** one of these to examine "
             "it:\\\n" + specimens + "\\\n\\\nStyxhexenhammer grows the flowers in his conservatory and tells whoever "
             "asks how far they have come; a feather serves as well, so nobody needs him."),
            ("text", "His Evidence",
             "Then, in any order: **harvest** a ripe organism crop by hand (a Verdant Bed's, which a Greenwarden wakes), "
             "and **dissolve** a sample at an **Assayer's Scale**. Only what you do once you understand the Binding Arts "
             "counts, so do each again if you did it before. **Research Notes** never stand in for any of it."),
            ("text", "The Sky",
             "Stolas comes only to someone who has seen a star themselves: record a sky observation at an **Orrery "
             "Observatory** while a pattern is up and visible there. It is not research, so your journal's **Pacts** "
             "section names it apart. An observation made long ago still counts; nothing is recorded for a sky you did "
             f"not watch. The Lantern Star comes round {when('lantern_star')}, the New Moon {when('new_moon')}."),
            ("text", "Understood", "Stolas will hear an invitation now: build a "
             "[pact perch](entry://jugcraft:arcane_concordance/binding/stolas_pact) where you may build, and use it. He names "
             "his terms before anything is spent.", (research, "understood")),
            ("text", "Mastered", "You kept your pact's first obligation.", (research, "mastered")),
        ]}}
    pact = PACTS["stolas"]
    service, upkeep, compact = pact["service"], pact["upkeep"], pact["compact"]
    entries[("binding", "stolas_pact")] = {
        "name": "Stolas's Pact", "x": 2, "y": 4, "icon": rid("pact_perch"),
        "condition": (research, "understood"), "description": "The pact perch, Stolas's terms and the Open Hand Compact",
        "pages": [
            ("crafting_recipe", "Pact Perch", "Build it where you may build, at your own base (never in Styx's home), and use "
             "it to invite Stolas. It holds nothing: what you pay ahead is kept with the pact, and breaking the perch never "
             "ends it.", rid("pact_perch")),
            ("text", "His Terms",
             f"Stolas's screen gives his three words, then every term before anything is spent: his service, any hour, within "
             f"{service['radius']} blocks of the perch, {service['quota']} a game day; one offering every "
             f"{upkeep['period_days']} game days, up to {upkeep['cap']} paid ahead, a warning and {upkeep['grace_days']} game "
             "day of grace, then suspension with nothing lost; until you end it; the offerings back to you, once. **Decline**, "
             "or any refusal, spends nothing and names why."),
            ("text", "The Open Hand Compact",
             f"**Accept** pays the compact in the same moment the pact is sealed: {compact['resonance']} Astral Resonance from "
             "an astrolabe you carry (an Orrery Observatory fills it for its keeper's party, and only a Starwatcher, who "
             "understands Celestial Attunement, keeps one), and " + ", ".join(f"{c['count']} × {c['item'].split(':')[1].replace('_', ' ')}"
                                                         for c in compact["items"])
             + ". It counts as the first offering. Only this perch's owner may accept here, and one pact with Stolas a holder."),
        ]}
    entries[("binding", "stolas_pact")]["pages"] += pact_pages()
    return entries


def tags(tags):
    for ref in {**STYX_SPECIMENS, **PLAIN_SPECIMENS}:
        tags.add("item", STOLAS_SPECIMEN_TAG, ref)
    for ref in OFFERINGS:
        tags.add("item", OFFERINGS_TAG, ref)
    tags.add("block", "minecraft:mineable/axe", rid("pact_perch"))


def write_all(write, assets, data, lang, condition, self_drop):
    write(assets / "models" / "block" / "pact_perch.json", PERCH_MODEL)
    write(assets / "blockstates" / "pact_perch.json", {"variants": {"": {"model": rid("block/pact_perch")}}})
    write(assets / "items" / "pact_perch.json", {"model": {"type": "minecraft:model", "model": rid("item/pact_perch")}})
    write(assets / "models" / "item" / "pact_perch.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/pact_perch")}})
    write(data / "loot_table" / "blocks" / "pact_perch.json", self_drop("pact_perch"))
    write(data / "recipe" / "pact_perch.json", dict({"fabric:load_conditions": condition("concordance")}, **RECIPE))


def write_data(write, data):
    for key, info in PACTS.items():
        write(data / "concordance" / "pact" / f"{key}.json", dict({"schema": 1}, **info))


# ------------------------------------------------------------------------------------------- D-04: the screen and body
# The terms page's label for each term (D-S15), in TERMS' order (check_pact_screen).
TERM_LABELS = {
    "service": "Service", "hours": "Hours", "area": "Area", "quota": "Daily quota", "upkeep": "Upkeep", "grace": "Grace",
    "suspension": "If suspended", "duration": "How long", "breach": "Breach", "ending": "How to end it",
    "returns": "What comes back", "perch": "If the perch is broken", "beneficiaries": "Who benefits",
    "compact": "The Open Hand Compact",
}
# The pact perch's screen (Java: client/PactScreen.java, ui("<key>")). English only (D-S21).
SCREEN = {
    "screen.jugcraft.pact.opening": "He settles on the perch...",
    "screen.jugcraft.pact.page": "%s of %s",
    "screen.jugcraft.pact.back": "Back",
    "screen.jugcraft.pact.next": "Next",
    "screen.jugcraft.pact.hear_terms": "Hear his terms",
    "screen.jugcraft.pact.leave": "Leave",
    "screen.jugcraft.pact.leave.hint": "Nothing is spent or agreed; he leaves the perch",
    "screen.jugcraft.pact.terms": "His terms",
    "screen.jugcraft.pact.term_row": "%s: %s",
    "screen.jugcraft.pact.terms_note": "Nothing is spent or agreed until you accept.",
    "screen.jugcraft.pact.in_the_way": "Before he can agree:",
    "screen.jugcraft.pact.waiting": "He considers your answer...",
    "screen.jugcraft.pact.accept": "Accept the pact",
    "screen.jugcraft.pact.accept.hint": "Pays the Open Hand Compact and seals the pact, both at once",
    "screen.jugcraft.pact.decline": "Decline",
    "screen.jugcraft.pact.decline.hint": "Nothing is spent; he leaves the perch",
    "screen.jugcraft.pact.kept": "Your pact",
    "screen.jugcraft.pact.standing": "How it stands:",
    "screen.jugcraft.pact.tab.service": "His service",
    "screen.jugcraft.pact.tab.terms": "Terms",
    "screen.jugcraft.pact.recall_heading": "Star-reckoning: recall a pattern you have observed",
    "screen.jugcraft.pact.no_patterns": "You have observed no sky pattern yet: record one at an observatory.",
    "screen.jugcraft.pact.pattern.cost": "%s resonance from your astrolabe",
    "screen.jugcraft.pact.pattern.chosen": "Chosen: %s resonance from your astrolabe",
    "screen.jugcraft.pact.pattern.claimed": "Already recalled this occurrence",
    "screen.jugcraft.pact.recall": "Recall %s",
    "screen.jugcraft.pact.recall.none": "Choose a pattern to recall",
    "screen.jugcraft.pact.offerings_heading": "Offerings for his upkeep",
    "screen.jugcraft.pact.no_offerings": "You carry none of the offerings he takes.",
    "screen.jugcraft.pact.offer": "Offer %s (%s carried)",
    "screen.jugcraft.pact.offer.hint": "Paid ahead into the pact's keeping, as many as it has room for",
    "screen.jugcraft.pact.suspend": "Suspend the pact",
    "screen.jugcraft.pact.suspend.hint": "His service stops; nothing is taken and nothing is lost",
    "screen.jugcraft.pact.resume": "Resume the pact",
    "screen.jugcraft.pact.resume.hint": "His service starts again",
    "screen.jugcraft.pact.end": "End the pact",
    "screen.jugcraft.pact.end.hint": "He leaves; what you paid ahead comes back to you once",
    "screen.jugcraft.pact.end.title": "End your pact with %s?",
    "screen.jugcraft.pact.end.confirm": "%s will leave, and what you paid ahead comes back to you once. This cannot be undone.",
    "screen.jugcraft.pact.end.yes": "End the pact",
    "screen.jugcraft.pact.end.no": "Keep it",
    "screen.jugcraft.pact.seat": "Seat the pact here",
    "screen.jugcraft.pact.seat.hint": "Your pact without its perch is kept at this perch from now on",
    "screen.jugcraft.pact.without_perch": "Without its perch",
    "screen.jugcraft.pact.closed": "At this perch:",
    "screen.jugcraft.pact.close": "Close",
    "screen.jugcraft.pact.help": "Tab and Shift+Tab move between his words and the choices; Enter or Space chooses; Esc closes. "
                                 "Closing before you accept agrees to nothing.",
}
# Stolas's body (D-S5, D-06): the owner library's Flashjack rig in his raven form (art/owner-library/catalog/files.csv:21291,
# 21330, 21676; SHA-256 d23b5372..., c4015c93..., 567b44a5...). Its geometry is copied unchanged by tools/owner_art.py
# (OWNER_FILES); his skin, the glowmask that lights his eyes and his animations are adapted from the rig's own files by
# tools/stolas_body.py (ADAPTED: runtime path -> (library file, adapter)), which tools/owner_art.py writes and checks.
# Its folder's leftover metadata labels it Ars Elemental by Alexthw, "GNU Lesser General Public License v3.0"
# (ports/jug-nouveau/ars_jymbaumental-1.21.1-0.7.10.4/META-INF/neoforge.mods.toml:7, 15, 25); the owner's declaration is
# art/owner-library/PROVENANCE.md:3-7. Not imported: flashjack.bbmodel, flapjack.png, bluejay.png; attack and idle.air
# stay in his animation file, unused. check_stolas_body holds the Java, the client test and the files to these tables.
FLASHJACK = "assets/ars_jymbaumental/"
OWNER_FILES = {"geckolib/models/entity/stolas.geo.json": FLASHJACK + "geo/flashjack.geo.json"}
ADAPTED = {
    "textures/entity/stolas.png": (FLASHJACK + "textures/entity/flashjack.png", "skin"),
    "textures/entity/stolas_glowmask.png": (FLASHJACK + "textures/entity/flashjack.png", "glowmask"),
    "geckolib/animations/entity/stolas.animation.json": (FLASHJACK + "animations/flashjack.animation.json", "animations"),
}
BODY_SCALE = 1.4  # client/WorkerClient.STOLAS_SCALE: the owner's rig is about a block tall, so he stands 1.45 (Workers.STOLAS)
# His loops (Java: StolasEntity.Loop on the worker's main controller, for what the server synced) and gestures
# (StolasEntity.Gesture on its "gesture" controller, each fired by the server's action). "astir" is the owner's idle.flapping.
LOOPS = {"idle": "animation.stolas.idle", "listen": "animation.stolas.listen", "waiting": "animation.stolas.waiting",
         "suspended": "animation.stolas.suspended", "astir": "idle.flapping"}
GESTURES = {"arrive": "animation.stolas.arrive", "inspect": "animation.stolas.inspect", "assent": "animation.stolas.assent",
            "refuse": "animation.stolas.refuse", "depart": "animation.stolas.depart"}
# Each gesture's length (seconds; Java: Gesture's ticks) and the stretch that holds its telling pose (seconds):
# ConcordancePactClientGameTests freezes the world inside it. Depart holds its last frame (he is gone by then).
GESTURE_TIMES = {"arrive": (2.0, (0.70, 1.20)), "inspect": (2.5, (0.40, 1.10)), "assent": (2.0, (0.45, 1.25)),
                 "refuse": (2.0, (0.50, 1.30)), "depart": (2.0, (0.30, 0.80))}
LOOP_LENGTHS = {"idle": 4.0, "listen": 3.0, "waiting": 4.0, "suspended": 6.0}
# His listening pose, as offsets from the perched one (degrees; the head's position in pixels): every gesture starts
# and ends on it.
ATTEND = {"head": (6, 8, -16), "head_position": (0, 0, -0.5), "body": (4, 0, 0), "wing": (0, 0, -6)}
EYE_TEXELS = ((9, 10), (9, 11), (14, 10), (14, 11))  # his skin's eyes: all his glowmask lights (measured)
# What ConcordancePactClientGameTests.poses photographs (D-06): each of his four loops of his own and his five gestures.
# Not his astir loop (the owner's idle.flapping, played while an offering is due): posing it would need an offering due
# in the client test, which would change the pact's upkeep that its later steps read (a recorded deviation from D-06's
# "a frozen-tick pose for each clip"; check_geckolib still holds the clip to his animation file).
BODY_SHOTS = ["jugcraft_stolas_idle", "jugcraft_stolas_listen", "jugcraft_stolas_suspended", "jugcraft_stolas_waiting",
              "jugcraft_stolas_inspect", "jugcraft_stolas_assent", "jugcraft_stolas_refuse", "jugcraft_stolas_depart",
              "jugcraft_stolas_arrive"]
# What ConcordancePactClientGameTests photographs (D-05 appends its own).
CLIENT_SHOTS = ["jugcraft_pact_conversation", "jugcraft_pact_terms", "jugcraft_pact_terms_scale4", "jugcraft_pact_conversation_scale4",
                "jugcraft_pact_terms_small", "jugcraft_pact_conversation_small", "jugcraft_pact_refused", "jugcraft_pact_kept",
                "jugcraft_pact_stolas"]


def screen_lang(lang):
    """The pact screen's words (D-S21: English only)."""
    lang.update(SCREEN)
    for key, label in TERM_LABELS.items():
        lang[f"screen.{MOD}.pact.term.{key}"] = label


# ------------------------------------------------------------------------------------------- D-05: Star-reckoning
# Java: worker/Pacts.Reckoning and serves; spirits/StarReckoning; sky/AstrolabeItem.recall (the one recall). Ids are worded:
# append only. check_pacts holds the Java to these.
RECKONING_WORK = "star_reckoning"  # Java: StarReckoning.WORK (Stolas's service.work in PACTS; one of WORK)
RECKONING_CHOICES = 8  # Java: StarReckoning.MAX_CHOICES (= STATE_LIMITS["MAX_CHOICES"]; the catalog has six)
RECKONING = ["recalled", "disabled", "nothing", "conflicting", "not_holder", "suspended", "upkeep_held", "outside_area",
             "outside_hours", "quota_spent", "not_observed", "no_astrolabe", "too_little", "recalled_already"]
CLIENT_SHOTS += ["jugcraft_pact_service", "jugcraft_pact_end_confirm", "jugcraft_pact_ended"]

MESSAGES.update({
    "pact.reckoning.recalled": "Stolas reckons %s for you: its effect is yours for two minutes, whatever the sky",
    "pact.reckoning.nothing": "No pact with Stolas is kept at this perch",
    "pact.reckoning.not_holder": "Only the pact's holder may ask Stolas to reckon",
    "pact.reckoning.suspended": "The pact is suspended: resume it, and Stolas will reckon again",
    "pact.reckoning.upkeep_held": "The pact is suspended for its upkeep: an offering resumes it",
    "pact.reckoning.outside_area": "Stand at the perch, within %s blocks, to ask Stolas",
    "pact.reckoning.outside_hours": "Stolas reckons only between %s and %s",
    "pact.reckoning.quota_spent": "Stolas has reckoned for you this game day; ask again tomorrow",
    "pact.reckoning.not_observed": "Stolas recalls only a pattern you observed yourself at an observatory",
    "pact.reckoning.no_astrolabe": "Carry an astrolabe: its stored resonance pays for the recall",
    "pact.reckoning.too_little": "Your astrolabe needs %s resonance to recall %s",
    "pact.reckoning.recalled_already": "You have recalled %s since it last rose",
    "pact.row.service": "Star-reckoning: recall a pattern you observed, %s a game day (%s left today), paid from your astrolabe",
    "pact.row.obligation": "First obligation: record a sky observation at an observatory while the pact is active",
    "pact.row.obligation_kept": "First obligation kept: Stolas's Regard is mastered",
    "pact.obligation_kept": "Your pact's first obligation is kept: %s regards you",
})


def pact_pages():
    """D-05's pages of the binding/stolas_pact codex entry, from Stolas's offer (PACTS)."""
    import concordance_celestial as celestial
    service, upkeep = PACTS["stolas"]["service"], PACTS["stolas"]["upkeep"]
    return [
        ("text", "Star-reckoning",
         f"While your pact is active, stand at the perch (within {service['radius']} blocks) and ask Stolas to **reckon**: "
         "choose any sky pattern you observed yourself at an observatory, and your astrolabe pays that pattern's recall "
         f"price, {service['quota']} a game day. Its effect is yours for {celestial.RECALL_TICKS // 1200} minutes wherever "
         "you go, whatever the sky or the season. Each rising of a pattern pays one recall, whether Stolas or a master's "
         "own recall asks first."),
        ("text", "His Upkeep",
         "Pay **offerings** ahead at the perch: a Stolas Starflower, Ravenquill Lupine, Amethyst Mourningbell or an amethyst "
         f"shard. One is taken every {upkeep['period_days']} game days; up to {upkeep['cap']} wait paid ahead, and the "
         f"compact paid the first. With none waiting you are warned and have {upkeep['grace_days']} game day of grace; then "
         "the pact is suspended: nothing is taken or lost, and an offering resumes it. Time you are away, or the perch "
         "unloaded, is never owed, and a pact never ends by itself."),
        ("text", "Ending and Moving",
         "Its holder may **suspend** and **resume** it, or **end** it: what is paid ahead comes back to you once (at your "
         "feet if your pockets are full) and Stolas departs. Breaking the perch ends nothing: Stolas leaves the world and the "
         "pact waits, with all it holds, until you **seat** it at a new perch of your own. Your first obligation: record a "
         "sky observation at an observatory while the pact is active."),
    ]


# ------------------------------------------------------------------------------------------- D-06: Stolas's presence
# Java: spirits/StolasEntity.CUE_TICKS (the least time between two of his cues of one kind).
CUE_TICKS = 10
# His cues, in StolasEntity.SOUND_IDS' order: jugcraft:stolas.<cue>, each with its subtitle (D-S5), bound to .ogg files
# already in runtime (no new audio, no voice; the raven's call is beast_crow's too). Being in SOUND_FILES, none is drawn
# by tools/concordance_sounds.py. Every file is mono, since OpenAL places only a mono source, so each is heard from him and
# fades with distance (check_pacts reads each file's channels; the unit review, D1): the leaves of a spelunkery table's
# paper for listening and inspecting, and a vesper's wing beats for his going (the cave book's pages and the ghost's weep
# that were first bound are stereo).
SOUND_EVENTS = {
    "stolas.arrive": "Stolas alights",
    "stolas.listen": "Stolas listens",
    "stolas.inspect": "Stolas inspects",
    "stolas.assent": "Stolas assents",
    "stolas.refuse": "Stolas refuses",
    "stolas.depart": "Stolas departs",
}
SOUND_FILES = {
    "stolas.arrive": [f"menagerie/beast/flutter_flap_{n}" for n in range(3)],
    "stolas.listen": ["menagerie/cavern/block/spelunkery_table/spelunkery_table_paper_insert"],
    "stolas.inspect": ["menagerie/cavern/block/spelunkery_table/spelunkery_table_paper_remove"],
    "stolas.assent": ["household/block/doorbell/chime"],
    "stolas.refuse": [f"menagerie/beast/crow_idle_{n}" for n in range(4)],
    "stolas.depart": [f"menagerie/cavern/mob/vesper/vesper_flap_{n}" for n in range(4)],
}
# What Jade shows (Java: spirits/PactReading; client/compat/JugcraftJadeClient). Sent as ids; worded on the client.
JADE_KEYS = ["standing", "visiting", "departing", "counterpart", "service", "state", "due_days", "grace_days", "waiting"]
JADE_STANDINGS = ["none", "kept", "copy", "unreadable", "disabled"]
JADE_STATES = ["active", "suspended", "upkeep_due", "upkeep_held"]
# Every work's short name (concordance_workers.WORK and WORK here; check_pacts holds the tables together).
SERVICE_NAMES = {"gather": "Gathering", "courier": "Carrying its post's requests", "star_reckoning": "Star-reckoning"}
JADE_PROVIDERS = {"pact_perch": "Pact Perch", "spirit_anchor": "Spirit Anchor"}
TOOLTIPS.update({
    "jade.pact.none": "Keeps no agreement",
    "jade.pact.visiting": "Stolas is visiting: he waits for an answer",
    "jade.pact.departing": "Departing",
    "jade.pact.unreadable": "Its agreement cannot be read by this version; it is kept as it was",
    "jade.pact.keeps": "Keeps an agreement with %s",
    "jade.pact.service": "Service: %s",
    "jade.pact.state.active": "Active",
    "jade.pact.state.suspended": "Suspended by its holder",
    "jade.pact.state.upkeep_due": "An offering is due; it serves on through its grace",
    "jade.pact.state.upkeep_held": "Suspended for its upkeep: nothing is lost, and an offering resumes it",
    "jade.pact.next": "Next offering in %s game days",
    "jade.pact.grace": "Grace: %s game days left",
    # Its presence's own status word (compose.jugcraft.worker.status.*) follows, which may start "waiting: ...".
    "jade.pact.waiting": "Its presence: %s",
    "jade.pact.lost": "Its presence is lost: its holder, near its place, calls it back at no cost",
    **{f"jade.pact.service.{work}": name for work, name in SERVICE_NAMES.items()},
})
SCREEN.update({
    "screen.jugcraft.pact.result.done": "Done: %s",
    "screen.jugcraft.pact.result.refused": "Refused: %s",
})
CLIENT_SHOTS += ["jugcraft_pact_perch_jade", "jugcraft_pact_stolas_jade"]


def presence_lang(lang):
    """D-06: Jade's provider names (its plugin settings list them)."""
    for key, name in JADE_PROVIDERS.items():
        lang[f"config.jade.plugin_{MOD}.{key}"] = name


# ------------------------------------------------------------------------------------------- D-09: the evidence bundle
# A timed sequence through his refusal (08:176; 03:159, the tell a player most needs to read without words):
# ConcordancePactClientGameTests.refusalSequence freezes the world at each time (seconds into the clip) and photographs it.
REFUSAL_SEQUENCE = (0.25, 0.65, 1.05, 1.45)
SEQUENCE_SHOTS = ["jugcraft_stolas_refuse_seq_1", "jugcraft_stolas_refuse_seq_2", "jugcraft_stolas_refuse_seq_3",
                  "jugcraft_stolas_refuse_seq_4"]
# What ConcordancePactNetworkTests' second client photographs (D-09, D-S22).
NETWORK_SHOTS = ["jugcraft_pact_peer_arrival", "jugcraft_pact_peer_refused", "jugcraft_pact_peer_departure"]
