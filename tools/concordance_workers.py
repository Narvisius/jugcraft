"""Roadmap step 17: spirits, familiars and constructs (docs/features/arcane-concordance-workers.md).

Three separate models, each owned by the server and saved with its worker, never in its brain:
- a familiar (the Hearthling) keeps close to its one person and mends them by its bond, which grows by time spent
  together (capped a day) and fades when neglected;
- a spirit (the Gathering Shade) works only within an agreement sealed at its Spirit Anchor as a Bound Will: one kind
  of work, an area, hours by the world clock and a daily quota; its holder may suspend or release it;
- a construct (the Clockwork Porter) carries what its body allows between two containers, spends Ley Charge each trip,
  wears and is repaired with copper.

Every worker always says what it is doing or exactly why not (waiting for resources, blocked by access, unable to
navigate, outside its agreement, finished, owner offline, another dimension, destination unloaded, suspended, needs
repair, no energy, full). None loads a chunk. The definitions are data (data/jugcraft/concordance/worker, read by
Java's concordance/worker). tools/concordance.py merges these tables into its own; tools/check_mod_data.py checks the
numbers the Java repeats.

Concordance Chunk D, unit 1 (D-01; docs/features/concordance-chunk-d.md): the existing agreements keep their promises. A
Spirit Anchor records who placed it, and only its owner or their party seal at it, never a stand-in; a seal is refused
with one reason (SEAL_OUTCOMES) before anything is recorded, spent or called, and a new shade is on the roster at once; a
killed shade's load goes home once and its holder calls another under the same agreement at no cost; only the holder ends
an agreement, once (Java: worker/Pacts.java, spirits/AgreementHolder.java). A familiar's bond fades only for its
person's own play time away (D-S17), never for time they are logged out, nor, while it is loaded, for play with the
Concordance switched off; one not loaded meanwhile counts that play at its first look once the switch is on (D-P13;
Java: worker/Bond.java, spirits/HearthlingEntity.java).

Concordance Chunk D, unit 1 (D-02): one pact record. A Bound Will carries its pact: where it is kept, whether it is held
and why (PACT_HOLDS), who benefits, the presence serving it, the service terms and day's count it was sealed with, and its
upkeep, with the offerings paid ahead kept beside it in the world's record, never in a block. A copied anchor is inert and
releases nothing; an anchor from before claims its record once, at its own place. Upkeep falls due on the Spire's model
(PACT: looked at only with the Concordance on and the holder within REACH, at most one period a look, never owing time
away or held; a gap of at most ABSENT_TICKS is not an absence, so a due date or grace end inside it counts), warns,
gives a grace and holds the pact without loss; only its living holder ends it, once, and the last HISTORY departures of
each holder are kept (PACT_DEPARTURES; Java: worker/Pacts.java, resource/BoundWill.java, spirits/AgreementHolder.java,
spirits/BoundWills.java, spirits/PactHistory.java).

Concordance Chunk D, unit 4 (D-08, D-S2, D-S12): a second spirit definition, the Workshop Hand (work courier), sealed at a
Spirit Anchor that a Porter Key bound to its holder's Courier Post, by one who has mastered Stolas's Regard (HAND_RESEARCH,
asked in Java). It carries the post's requests with the porter's own decision (Java: spirits/CourierWork.java), for its
holder and their party, within its hours, area, day's loads and carry. It is a spirit: its service is a spirit's, so the
Binding Arts stay mastered by each kind's (distinct 3).
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# Shared (Java: spirits/WorkerEntity.java, worker/Navigation.java).
THINK_TICKS = 20
GIVE_UP = 3
RETRY_TICKS = 600
# The familiar's bond (Java: worker/Bond.java).
BOND_MAX = 100
BOND_GAIN = 1
BOND_DAILY_GAIN = 24
BOND_NEGLECT = 4
BOND_FIRST = 20
BOND_SECOND = 60
VISIT_TICKS = 200  # Java: spirits/HearthlingEntity.java
# Parser limits (Java: worker/WorkerParser.java).
MAX_RADIUS = 12
MAX_CARRY = 64
MAX_QUOTA = 256
MAX_SUPPORT_TICKS = 200
WORK = ["gather", "courier"]
# The roster (Java: spirits/WorkerRoster.java).
ROSTER_MAX = 32
# Concordance Chunk D, unit 1 (D-01; Java: worker/Pacts.java Outcome, same order; spirits/AgreementHolder.java told). How a
# seal can end: each id is worded as message.jugcraft.concordance.workers.seal_<id> ("disabled" as the switch's). Ids are
# worded: append only.
SEAL_OUTCOMES = ["done", "disabled", "not_permitted", "taken", "already_held", "holder_full", "roster_full", "unavailable",
                 "cannot_afford"]
# Concordance Chunk D, unit 1 (D-02; Java: worker/Pacts.java, resource/BoundWill.java Hold). The pact rules' numbers (ticks
# and blocks), why a pact is held and why one departed: ids are saved or worded, so append only.
PACT = {"ABSENT_TICKS": 1200, "WRITE_TICKS": 200, "REACH": 64, "HISTORY": 8, "MAX_CAP": 4, "MAX_COUNT": 64,
        "MAX_PERIOD": 672000, "MAX_GRACE": 168000}
PACT_HOLDS = ["none", "holder", "upkeep"]
PACT_DEPARTURES = ["ended", "broken"]
# Concordance Chunk D, D-04/D-05 (D-S6): a pact without its place seated again (Java: AgreementHolder.Seat). Append only.
SEAT_OUTCOMES = ["seated", "disabled", "not_holder", "nothing", "not_permitted", "taken", "roster_full", "unavailable"]

STATUSES = {
    "following": "following its person",
    "supporting": "mending its person",
    "travelling": "on its way",
    "working": "working",
    "returning": "taking what it carries home",
    "idle": "idle: nothing to do",
    "waiting_for_resources": "waiting for resources (its source is empty or gone)",
    "blocked_by_access": "blocked: its keeper may not take from or give to that place",
    "cannot_navigate": "cannot find a way there",
    "outside_agreement": "outside its agreement (the hours, the area or the work)",
    "finished": "finished: its work for the day is done",
    "owner_offline": "waiting: its person is not here",
    "other_dimension": "waiting: its person or route is in another dimension",
    "destination_unloaded": "waiting: its destination is not loaded",
    "suspended": "suspended by its holder",
    "needs_repair": "needs repair (copper)",
    "no_energy": "out of Ley Charge (a pylon by its source fuels it)",
    "full": "full: there is no room to deliver",
    "disabled": "switched off",
    # Concordance Chunk D, D-02: a pact's upkeep (appended; ids are saved).
    "upkeep_due": "an offering is due for its upkeep (it works on through its grace)",
    "upkeep_held": "suspended for its upkeep: nothing is taken or lost, and an offering resumes it",
}

BINDING_SPECIMEN_TAG = f"{MOD}:binding_specimens"
BINDING_SPECIMENS = {
    "minecraft:lead": "Lead", "minecraft:name_tag": "Name Tag", "minecraft:soul_lantern": "Soul Lantern",
    "minecraft:saddle": "Saddle", "minecraft:echo_shard": "Echo Shard",
}
SERVICE = rid("worker_service")  # Java: Workers.ACTIVITY
HAND_RESEARCH = rid("stolas_regard")  # Java: WorkshopHandEntity.REGARD (Concordance Chunk D unit 2's research, named here only)

# ------------------------------------------------------------------------------------------------- research

RESEARCH = {
    "binding_arts": {
        "name": "Binding Arts",
        "principle": "tether",
        "tradition": "spiritbinders",
        "stage": "practitioner",
        "icon": rid("bonding_charm"),
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{BINDING_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{BINDING_SPECIMEN_TAG}", "distinct": 3}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{BINDING_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: a familiar's mending, a spirit's delivery and a construct's trip, each once.
            "mastered": [{"type": "practice", "activity": SERVICE, "distinct": 3}],
        },
        "unlocks": {},
        "advancements": {
            "encountered": ("Tethered", "Examine something that binds", "task", "minecraft:lead"),
            "observed": ("Terms and Conditions", "Examine three different things that bind", "task", "minecraft:name_tag"),
            "understood": ("Binding Arts", "Understand the Binding Arts: study something that binds at a Lampwright's Bench",
                           "goal", rid("bonding_charm")),
            "mastered": ("Keeper of Many", "Be served by a familiar, a spirit and a construct", "challenge", rid("spirit_anchor")),
        },
        "locked": {"encountered": "Examine something that binds first",
                   "observed": "Examine three different things that bind first",
                   "understood": "Understand the Binding Arts first", "mastered": "Master the Binding Arts first"},
    },
}

# ------------------------------------------------------------------------------------------------- definitions

WORKERS = {
    "hearthling": {"kind": "familiar", "follow": 8,
                   "support": {"effect": "minecraft:regeneration", "amplifier": 0, "duration": 100}},
    "gathering_shade": {"kind": "spirit", "work": "gather", "radius": 8, "from": 13000, "to": 23000, "quota": 64,
                        "carry": 16},
    # Concordance Chunk D, D-08: the Workshop Hand (a courier: carry is the most it reserves at a source a trip).
    "workshop_hand": {"kind": "spirit", "work": "courier", "radius": 12, "from": 0, "to": 12000, "quota": 32, "carry": 64},
    "clockwork_porter": {"kind": "construct", "integrity": 64, "wear": 1,
                         "repair": {"item": "minecraft:copper_ingot", "amount": 16}, "energy": 64, "trip_energy": 2,
                         "carry": 16},
}
ENTITY_NAMES = {"hearthling": "Hearthling", "gathering_shade": "Gathering Shade", "clockwork_porter": "Clockwork Porter",
                "workshop_hand": "Workshop Hand"}

# ------------------------------------------------------------------------------------------------- things

ITEMS = {
    "bonding_charm": {"name": "Bonding Charm",
                      "tooltip": "Use: bind a Hearthling, or call yours to you. Sneak: release it."},
    "clockwork_porter": {"name": "Clockwork Porter",
                         "tooltip": "Use on a block: unfold a porter. Give it a route with a Porter Key."},
    "porter_key": {"name": "Porter Key",
                   "tooltip": "Use on a container: the source, then the target. Use on a Courier Post, then on a porter or a "
                              "Spirit Anchor: bind it to the post (sneak on the anchor: unbind it)."},
}
BLOCKS = {"spirit_anchor": {"name": "Spirit Anchor"}}
DEVICE_TOOLTIPS = {
    "spirit_anchor": "Use: seal a Gathering Shade's agreement (its owner or their party), or, bound to your Courier Post by a "
                     "Porter Key, a Workshop Hand's; call your lost one again. Its holder, sneaking: suspend or resume. Break: "
                     "release.",
}

MESSAGES = {
    "workers.unknown": "You have not understood the Binding Arts (the codex says how)",
    "workers.status": "%s: %s",
    "workers.familiar": "%s; bond %s / %s",
    "workers.spirit": "%s; carrying %s",
    "workers.construct": "%s; integrity %s / %s, Ley Charge %s / %s, carrying %s",
    "workers.bound": "A Hearthling answers your charm",
    "workers.no_familiar": "You have no familiar to release",
    "workers.released": "Your familiar is released",
    "workers.not_here": "Your familiar is not near (another dimension, or not loaded)",
    "workers.recalled": "Your familiar comes to you",
    "workers.sealed": "Sealed: a Gathering Shade will gather within %s blocks, from %s to %s, up to %s times a day",
    "workers.suspended": "The agreement is suspended",
    "workers.resumed": "The agreement is resumed",
    "workers.agreement": "Agreement: within %s blocks, %s to %s; %s of %s tasks today; %s",
    "workers.is_suspended": "suspended",
    "workers.is_active": "active",
    "workers.spirit_away": "Its spirit is not loaded here",
    "workers.key_source": "Source: %s %s %s (now use it on the target)",
    "workers.key_target": "Target: %s %s %s (now use it on a porter)",
    "workers.key_unset": "The key has no route yet (use it on two containers)",
    "workers.key_refused": "You may not use that container yourself, so no porter may take or bring for you there",
    "workers.not_yours": "Only its keeper (or their party) may command it",
    "workers.routed": "The porter takes the route",
    "workers.none": "You have no workers",
    "workers.line": "%s: %s (%s, %s)",
    "workers.kind.familiar": "Familiar",
    "workers.kind.spirit": "Spirit",
    "workers.kind.construct": "Construct",
    # Concordance Chunk D, D-01 (D-S8, D-S11, D-S13, D-S18): every seal says how it went; a lost spirit is called again;
    # only its holder ends an agreement.
    "workers.seal_done": "The agreement is sealed",
    "workers.seal_not_permitted": "You may not seal an agreement here: it is not yours or your party's, or you could not build here",
    "workers.seal_taken": "This place already keeps an agreement",
    "workers.seal_already_held": "You already keep an agreement with this spirit",
    "workers.seal_holder_full": "You already hold %s Bound Wills, the most one person may: end one first. Nothing was sealed",
    "workers.seal_roster_full": "Your roster already keeps %s workers (familiars, spirits, constructs and helpers), the most it "
                                "may: release one first. Nothing was sealed",
    "workers.seal_unavailable": "No spirit answers now; nothing was sealed or spent",
    "workers.seal_cannot_afford": "You cannot pay what sealing it costs; nothing was sealed or spent",
    "workers.spirit_lost": "Its spirit was lost; its holder may use this place to call another under the same agreement",
    "workers.spirit_recalled": "Another answers the same agreement; nothing was spent",
    "workers.recall_roster_full": "Your roster already keeps %s workers, the most it may: release one, then call again",
    "workers.ended": "The agreement is ended: its Bound Will is released and its spirit departs, leaving what it carried",
    "workers.no_agreement": "No agreement is kept here",
    "workers.not_holder": "Only the agreement's holder may do that",
    "workers.end_not_permitted": "Only the agreement's holder, alive and in person, may end it",
    # Concordance Chunk D, D-02: one pact record (its upkeep, an unreadable record, why a pact departed).
    "workers.held_for_upkeep": "It is suspended for its upkeep: an offering resumes it, not your word",
    "workers.unreadable_record": "Its Bound Will cannot be read by this version: it is kept as it was, and the agreement waits",
    "workers.upkeep_paid": "An offering is taken for the agreement's upkeep (%s periods remain paid ahead)",
    "workers.upkeep_warned": "The agreement's upkeep is due: offer %s %s within %s game days, or it is suspended (nothing is "
                             "lost; time away does not count)",
    "workers.upkeep_held": "The agreement is suspended for its upkeep: nothing is taken and nothing accrues; an offering of %s "
                           "%s resumes it",
    "workers.upkeep_resumed": "Its upkeep is paid: the agreement is resumed",
    "workers.departed.ended": "ended by its holder",
    "workers.departed.broken": "released: its place was broken",
    # Concordance Chunk D, D-08 (D-S2, D-S12): the Workshop Hand, and an anchor bound to a Courier Post.
    "workers.hand_sealed": "Sealed: a Workshop Hand will carry the requests of the Courier Post at %s %s %s for you and your party, "
                           "within %s blocks of the anchor, from %s to %s, up to %s loads a day of at most %s items each",
    "workers.hand_unknown": "Only one who has mastered Stolas's Regard may seal a Workshop Hand (the codex says how)",
    "workers.hand_post": "It carries the requests of the Courier Post at %s %s %s, for its holder and their party",
    "workers.hand_no_post": "Its Courier Post is gone: it waits until a post its holder may use stands there again",
    "workers.post_bound": "This anchor now serves the Courier Post at %s %s %s: a Workshop Hand sealed here carries its requests",
    "workers.post_unbound": "This anchor serves no Courier Post now: sealed, it calls a Gathering Shade",
    "workers.post_sealed": "An agreement is kept here: end it before binding the anchor anew",
    "workers.post_unusable": "That Courier Post cannot be this anchor's: it must be one you may use, loaded, in this dimension and "
                             "within %s blocks of the anchor",
    "workers.key_no_post": "Use the key on your Courier Post first, then on the anchor (sneak to unbind it)",
    # Concordance Chunk D, D-04 (D-S6): the roster's pact kind, and a pact without its place seated again.
    "workers.kind.pact": "Pact",
    "workers.seat_seated": "The pact is seated here: its presence returns, and nothing was spent",
    "workers.seat_not_holder": "Only the pact's holder, in person, may seat it",
    "workers.seat_nothing": "You keep no pact without its place to seat here",
    "workers.seat_not_permitted": "Only this place's owner may seat a pact here, and only where they may build",
    "workers.seat_taken": "This place already keeps an agreement",
    "workers.seat_roster_full": "Your roster already keeps %s workers, the most it may: release one, then seat the pact",
    "workers.seat_unavailable": "No spirit answers now; nothing was seated",
}

TOOLTIPS = {
    "binding_specimen": "Something that binds: sneak and use to examine it",
    "porter_key.route": "Route: %s to %s",
    "jade.bond": "Bond: %s / %s",
    "jade.body": "Integrity %s / %s, Ley Charge %s / %s",
    "jade.carried": "Carrying %s",
}


def lang_entries(lang):
    lang[f"tag.item.{BINDING_SPECIMEN_TAG.replace(':', '.')}"] = "Things That Bind"
    for key, text in STATUSES.items():
        lang[f"compose.{MOD}.worker.status.{key}"] = text
    for key, name in ENTITY_NAMES.items():
        lang[f"entity.{MOD}.{key}"] = name
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"config.jade.plugin_{MOD}.worker"] = "Familiars, spirits and constructs"


# ------------------------------------------------------------------------------------------------- codex

def clock(tick):
    minutes = (tick * 60 // 1000 + 6 * 60) % (24 * 60)
    return f"{minutes // 60:02d}:{minutes % 60:02d}"


def codex():
    art = "binding_arts"
    familiar, spirit, construct = WORKERS["hearthling"], WORKERS["gathering_shade"], WORKERS["clockwork_porter"]
    hand = WORKERS["workshop_hand"]  # Concordance Chunk D, D-08
    specimens = "\\\n".join(f"- {name}" for name in BINDING_SPECIMENS.values())
    reasons = "\\\n".join(f"- **{key.replace('_', ' ')}**: {text}" for key, text in STATUSES.items())
    return {
        ("binding", "binding_arts"): {
            "name": "Binding Arts", "x": 0, "y": 0, "icon": rid("bonding_charm"), "condition": None,
            "description": "Familiars, spirits and constructs: research",
            "pages": [
                ("text", "Binding Arts",
                 "Once you understand First Light, look closely at what binds one thing to another. **Sneak and use** "
                 "one of these to examine it:\\\n" + specimens),
                ("text", "Observed", "Study one at a **Lampwright's Bench**, or read someone's **Research Notes**.",
                 (art, "observed")),
                ("text", "Understood",
                 "A familiar, a spirit and a construct will answer you now. To **master** the Binding Arts, be served "
                 "by each once: a familiar's mending, a spirit's delivery (a Workshop Hand's is a spirit's) and a "
                 "construct's trip. Stolas, a raven among the spirits, may take notice of you as well: "
                 "[Stolas's Regard](entry://jugcraft:arcane_concordance/binding/stolas_regard).", (art, "understood")),
                ("text", "Mastered", "You keep many, and each keeps its own terms.", (art, "mastered")),
            ],
        },
        ("binding", "familiars"): {
            "name": "Familiars", "x": 2, "y": 0, "icon": rid("bonding_charm"), "condition": (art, "observed"),
            "description": "A Hearthling and its bond",
            "pages": [
                ("crafting_recipe", "Bonding Charm",
                 f"Use it to bind a **Hearthling**: one familiar a person. It keeps within {familiar['follow']} blocks "
                 f"of you and, when you are badly hurt, mends you. Use the charm again to call it to your side; sneak "
                 f"to release it.", rid("bonding_charm")),
                ("text", "The Bond",
                 f"The bond grows by time spent together, {BOND_GAIN} a visit and at most {BOND_DAILY_GAIN} a day, and "
                 f"fades by {BOND_NEGLECT} for each day you play away from it, here or in another dimension; time you are "
                 f"logged out never counts, nor, while it is loaded, time you play with the Concordance switched off. At "
                 f"{BOND_FIRST} it mends you (at most once a minute); at {BOND_SECOND}, twice as often. It never follows "
                 f"you into another dimension and never waits in a place that is not loaded: it simply waits where it is."),
            ],
        },
        ("binding", "spirits"): {
            "name": "Spirits", "x": 4, "y": 0, "icon": rid("spirit_anchor"), "condition": (art, "understood"),
            "description": "Agreements with a Gathering Shade",
            "pages": [
                ("crafting_recipe", "Spirit Anchor",
                 f"Use it to seal an **agreement** with a Gathering Shade, recorded as a Bound Will: it gathers dropped "
                 f"items within {spirit['radius']} blocks of the anchor, from {clock(spirit['from'])} to "
                 f"{clock(spirit['to'])}, at most {spirit['quota']} times a day, carrying {spirit['carry']} at a time, "
                 f"and brings them to the anchor (hoppers below take them out).", rid("spirit_anchor")),
                ("text", "Its Terms",
                 "Only the anchor's owner (who placed it) or their party may seal an agreement at it, and only where "
                 "they may build (an anchor placed before owners were recorded: whoever may build there). Outside its "
                 "hours, its area or its work it waits at the anchor and says so; when its day's work is done it is "
                 "finished until tomorrow. It never takes what you could not take yourself (a protected town). Sneak "
                 "and use the anchor to **suspend** or resume it; break the anchor (with the Concordance on) to "
                 "**release** it: it leaves what it carried and departs."),
                ("text", "If It Is Lost",
                 "A shade that is killed leaves what it carried in its anchor (or where it fell, if the anchor is not "
                 "loaded or keeps someone else's agreement), and the anchor says it was lost: its holder uses it and "
                 "another answers the same agreement, at no cost. A seal that is refused says why (not yours, already "
                 "kept, too many Bound Wills, a full roster) and spends nothing. A copy of an anchor keeps nothing: it "
                 "says another agreement covers it, and breaking it releases nothing."),
            ],
        },
        ("binding", "constructs"): {
            "name": "Constructs", "x": 2, "y": 2, "icon": rid("clockwork_porter"), "condition": (art, "understood"),
            "description": "A Clockwork Porter's body",
            "pages": [
                ("crafting_recipe", "Clockwork Porter",
                 f"Unfold it on a block. Use a **Porter Key** on two containers (its source and its target), then on "
                 f"the porter. It carries {construct['carry']} items a trip, spends {construct['trip_energy']} Ley "
                 f"Charge a trip (it holds {construct['energy']}, drawn from a Ley Pylon by its source that you may "
                 f"use) and wears {construct['wear']} of its {construct['integrity']} integrity; each copper ingot "
                 f"mends {construct['repair']['amount']}.", rid("clockwork_porter")),
                ("crafting_recipe", "Porter Key", "Sets a porter's route.", rid("porter_key")),
            ],
        },
        ("binding", "reasons"): {
            "name": "Why It Waits", "x": 4, "y": 2, "icon": "minecraft:clock", "condition": (art, "encountered"),
            "description": "What a worker can tell you",
            "pages": [
                ("text", "Why It Waits",
                 "Look at a worker (Jade), use it with an empty hand, or ask **/jugcraft concordance workers** "
                 "(which also lists workers that are not loaded, with what they last said):\\\n" + reasons),
            ],
        },
        ("binding", "workshop_hand"): {
            "name": "Workshop Hand", "x": 4, "y": 4, "icon": rid("porter_key"), "condition": (art, "understood"),
            "description": "A spirit that carries a Courier Post's requests",
            "pages": [
                ("text", "Workshop Hand",
                 f"A spirit of the shade's kind, bound to carry. Once you have **mastered** "
                 f"[Stolas's Regard](entry://jugcraft:arcane_concordance/binding/stolas_regard), use a **Porter "
                 f"Key** set on your **Courier Post** on a Spirit Anchor that keeps no agreement, within {hand['radius']} "
                 f"blocks of the post (sneak to unbind it), then use the anchor: a Workshop Hand is sealed there as a Bound "
                 f"Will, for you and your party. From {clock(hand['from'])} to {clock(hand['to'])}, up to {hand['quota']} "
                 f"loads a day of at most {hand['carry']} items each, it carries that post's exact requests, an Alembic "
                 f"Crucible's own among them, from the containers round the post."),
                ("text", "Its Terms",
                 f"It takes a request only for you or your party, bound for a place within {hand['radius']} blocks of its "
                 "anchor, and fetches only from containers inside that area that you and the requester could open by hand; "
                 "other requests it leaves to other couriers, and so it does cargo waiting at the post that is more than "
                 f"it carries ({hand['carry']}) or must go back to a container beyond that area. A request it has taken it "
                 "finishes, even past its hours, suspended, or once its requester has left your party. What it carries is "
                 "the post's ledger's, never its own: a full destination keeps the cargo in transit and an unloaded one "
                 "is waited for. Killed, or its agreement ended, the cargo waits at the post for another courier or for "
                 "its requester to **recover** once; killed, its holder calls another at the anchor at no cost. It needs "
                 "no fuel and no repair. Outside its hours, its day's loads done, its post gone or its holder away, it "
                 "waits and says why."),
            ],
        },
    }


CATEGORY = {"binding": {"name": "Binding", "icon": rid("bonding_charm"), "sort": 9,
                        "description": "Familiars, spirits and constructs: bonds, agreements and bodies"}}


# ------------------------------------------------------------------------------------------------- data and assets

def tags(tags):
    for ref in BINDING_SPECIMENS:
        tags.add("item", BINDING_SPECIMEN_TAG, ref)
    tags.add("block", "minecraft:mineable/pickaxe", rid("spirit_anchor"))


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    import concordance_worker_models as models
    for key in ENTITY_NAMES:
        write(assets / "geckolib" / "models" / "entity" / f"{key}.geo.json", models.GEO[key]())
        write(assets / "geckolib" / "animations" / "entity" / f"{key}.animation.json", models.ANIMATIONS[key]())
    write(assets / "models" / "block" / "spirit_anchor.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": rid("block/spirit_anchor_top"), "side": rid("block/spirit_anchor_side"),
                     "bottom": rid("block/spirit_anchor_top")}})
    write(assets / "blockstates" / "spirit_anchor.json", {"variants": {"": {"model": rid("block/spirit_anchor")}}})
    write(assets / "items" / "spirit_anchor.json", {"model": {"type": "minecraft:model", "model": rid("item/spirit_anchor")}})
    write(assets / "models" / "item" / "spirit_anchor.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/spirit_anchor")}})
    write(data / "loot_table" / "blocks" / "spirit_anchor.json", self_drop("spirit_anchor"))
    for item in ITEMS:
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    feature = "concordance"
    recipes = {
        "bonding_charm": {"type": "minecraft:crafting_shapeless", "category": "misc",
                          "ingredients": ["minecraft:string", "minecraft:gold_ingot", "minecraft:glow_berries",
                                          "minecraft:amethyst_shard"],
                          "result": {"id": rid("bonding_charm"), "count": 1}},
        "spirit_anchor": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["DAD", "DLD", "DDD"],
                          "key": {"D": "minecraft:polished_deepslate", "A": "minecraft:amethyst_shard",
                                  "L": "minecraft:soul_lantern"},
                          "result": {"id": rid("spirit_anchor"), "count": 1}},
        "clockwork_porter": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["CBC", "CRC", "I I"],
                             "key": {"C": "minecraft:copper_ingot", "B": "minecraft:barrel", "R": "minecraft:redstone",
                                     "I": "minecraft:iron_ingot"},
                             "result": {"id": rid("clockwork_porter"), "count": 1}},
        "porter_key": {"type": "minecraft:crafting_shapeless", "category": "misc",
                       "ingredients": ["minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:iron_nugget"],
                       "result": {"id": rid("porter_key"), "count": 1}},
    }
    for name, recipe in recipes.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition(feature)}, **recipe))


def write_data(write, data):
    for key, info in WORKERS.items():
        write(data / "concordance" / "worker" / f"{key}.json", dict({"schema": 1}, **info))
