"""The Crypt Lich (docs/features/crypt-lich.md; the plan in docs/branches/BOSSES.md, "The Crypt Lich: the plan"): the
numbers the Java (src/main/java/.../lair/lich) and the data (tools/crypt_lich_data.py) share. tools/check_mod_data.py
checks the Java against them, and where his fight looks for the archive's parts against tools/crypt_archive.py.

Units: health and damage in half hearts (vanilla's points), times in ticks (20 a second), distances in blocks. Damage
and health are before the server's lairs.boss_damage and lairs.boss_health scaling (Vesperine's settings, which every
lair boss shares: 0.25 to 4, 1 by default).

His blows (the Grave Sweep, the bones of his Bone Lanes and of his Grave Pull) are a mob's attack; his Soul Bolt is a
mob's projectile, and Withers for a few seconds; his Soul Drain is magic, and heals him by what it takes.
"""

# CryptLichEntity: he, his pacing and his rule.
CRYPT_LICH = {
    "HEALTH": 400.0, "ARMOR": 8.0,
    "LEASH": 30.0,               # farther than this from the dais's middle, he is drawn back
    "PARTY_STEP": 0.5, "PARTY_MAX": 2.5,
    "WAKE_TICKS": 40,            # waking: he rises from his throne and glides down the apse's steps, unhurt
    "ABANDON_TICKS": 200, "GLOBAL_COOLDOWN": 20,
    "SPEED": 0.16,               # gliding
    "KEEP_DISTANCE": 3.0,
    "STEP": 1.0,
    "ERASE_AT": 0.5,             # at this share of his health, the Erasure
    "LAST_AT": 0.2,              # below this share, the Last Name
    "LAST_COOLDOWN": 0.6667,     # his cooldowns and his pause between attacks then: a third shorter
    # His rule, the phylactery. While a soul chain holds he is warded; the lit name's seal breaks its chain; with all
    # three broken he is exposed for EXPOSED_TICKS (staggered for the first STAGGER_TICKS), then the chains reforge over
    # REFORGE_TICKS, a seal every REFORGE_STEP. A name scraped from its stone (from the Erasure on) keeps its seal whole
    # until it is read back at the lectern, or until SCRAPED_TICKS after it is lit.
    "EXPOSED_TICKS": 240, "STAGGER_TICKS": 40, "REFORGE_TICKS": 30, "REFORGE_STEP": 10, "SCRAPED_TICKS": 600,
    # His Restless Dead: at most DEAD_CAP at once; RAISED of them rise at a Raise the Dead.
    "DEAD_CAP": 4, "RAISED": 2,
    # Grave Sweep: his staff across his front, SWEEP_HALF_ANGLE degrees either side of his face; thrown back.
    "SWEEP_DAMAGE": 7.0, "SWEEP_REACH": 3.5, "SWEEP_HALF_ANGLE": 70.0, "SWEEP_THROW": 1.2,
    # Bone Lanes: the lane to the alcove of the name he calls bursts with bones end to end: LANE_DAMAGE, tossed up.
    # In the Unwritten he calls two lanes at once. A clump of bones every CLUMP_STEP blocks down a lane's middle.
    "LANE_DAMAGE": 6.0, "LANE_TOSS": 0.7, "CLUMP_STEP": 2.0,
    # Soul Bolt: a skull of soul-fire drifts at his foe; struck back it flies at him: warded, it staggers him for
    # BOLT_STAGGER ticks (spoiling his next attack); exposed, REFLECTED_DAMAGE.
    "BOLT_DAMAGE": 6.0, "WITHER_TICKS": 60, "REFLECTED_DAMAGE": 10.0, "BOLT_STAGGER": 40,
    # The Erasure at ERASE_AT: he shrieks for ERASE_TICKS, unhurt, and the chains reforge at once.
    "ERASE_TICKS": 40,
    # Soul Drain: DRAIN every DRAIN_EVERY ticks through the attack's active ticks, healing him as much; a block between
    # them or DRAIN_RANGE breaks it.
    "DRAIN": 1.0, "DRAIN_EVERY": 10, "DRAIN_RANGE": 16.0,
    # Grave Pull: a ring of soul-fire PULL_RADIUS round him, one PULL_DARK-degree part of it dark; everyone in it but
    # those in the dark part is dragged to him, and PULL_BURST ticks later bones burst round his feet: PULL_DAMAGE within
    # PULL_REACH.
    "PULL_RADIUS": 5.0, "PULL_DARK": 60.0, "PULL_BURST": 10, "PULL_DAMAGE": 8.0, "PULL_REACH": 3.5,
    # The Last Name: his dead rise every LAST_RAISE ticks.
    "LAST_RAISE": 240,
    "EXPERIENCE": 300,
}
# His attacks (CryptLichEntity.Attack): wind-up, active and recovery ticks, cooldown, the range he uses it from
# (blocks), and in which phases ("warded": the Warded King; "unwritten": the Unwritten and the Last Name).
ATTACKS = {
    "GRAVE_SWEEP": (14, 4, 10, 40, 0, 4.0, True, True),
    "BONE_LANES": (24, 20, 10, 160, 0, 40, True, True),
    "SOUL_BOLT": (16, 1, 12, 100, 4, 24, True, True),
    "RAISE_DEAD": (30, 1, 10, 300, 0, 40, True, True),
    "SOUL_DRAIN": (20, 60, 10, 240, 0, 16, False, True),
    "GRAVE_PULL": (30, 10, 14, 280, 0, 5.0, False, True),
}
# The lanes he calls (crypt_archive.LANES): the lane to each name's alcove. The south lane, to the lectern, is never
# called: the way to the lectern is safe from his Bone Lanes.
LANE_NAMES = {"west": "sexton", "north": "crowned", "east": "scribe"}
# Where his fight stands in the archive (template cells and points, checked against tools/crypt_archive.py):
# WOKE: where he comes down to the hall's floor as he wakes, at the foot of the apse's steps on the north lane.
WOKE = (32.5, 16.5)
# Where a Restless Dead climbs out of each alcove's sarcophagus, at its foot: x, z (on the alcove's floor).
SARCOPHAGUS_DEAD = {"sexton": (8.5, 34.5), "crowned": (30.5, 7.5), "scribe": (56.5, 34.5)}
# His Restless Dead (RestlessDeadEntity): CLIMB ticks climbing out, unhurt; then they shamble at the nearest player.
RESTLESS_DEAD = {"HEALTH": 20.0, "DAMAGE": 4.0, "SPEED": 0.24, "ARMOR": 2.0, "CLIMB": 20}
# A Soul Bolt (SoulBoltEntity): it leaves his raised hand (HAND_AHEAD blocks ahead of his feet and HAND_UP over them:
# where his model's left hand is as he looses it, tools/crypt_lich_models.py HAND_Z and HAND_Y), drifts at SPEED turning
# TURN a tick toward its mark, and is gone after LIFE ticks; struck back it flies at REFLECTED_SPEED.
BOLT = {"SPEED": 0.3, "REFLECTED_SPEED": 0.6, "TURN": 0.08, "LIFE": 160, "HAND_AHEAD": 1.3125, "HAND_UP": 1.71875}
# His bones stand for their attack's active ticks, then sink for SINK ticks and are gone.
SINK = 10
# The Emptied Phylactery: held in the offhand, Wither never takes hold.

# His loot, rolled for each participant (CryptLichLoot): Grave Dust, a chance of one of his two trophies (the Arms VII
# variants his bosses/crypt_lich table holds; one is certain on a player's first kill), and the chance of each of the
# rest.
GRAVE_DUST = (3, 6)
TROPHIES = ("soulreaver", "gravewarden")
TROPHY_CHANCE = 0.15
CHANCES = {"emptied_phylactery": 0.25, "lichs_crown": 0.2}
ADVANCEMENT = {"key": "nameless", "title": "Nameless", "icon": "jugcraft:soulreaver",
               "description": "Break the Crypt Lich's chains and bring him down in the Archive of Last Names",
               "frame": "challenge"}

ENTITIES = {
    "crypt_lich": "The Crypt Lich",
    "restless_dead": "Restless Dead",
    "soul_bolt": "Soul Bolt",
    "lich_bones": "Lich's Bones",
}
# The entities GeckoLib draws, and the model each is.
GECKO = {"crypt_lich": "crypt_lich", "restless_dead": "restless_dead", "soul_bolt": "soul_bolt", "lich_bones": "lich_bones"}
# The bodies drawn with a glow layer, and so with a glowmask: his eyes, his crown's soul-fire and the skull in his hand;
# the dead's eyes; the bolt.
GLOWING = ("crypt_lich", "restless_dead", "soul_bolt")
ITEMS = {"grave_dust": "Grave Dust", "emptied_phylactery": "Emptied Phylactery"}
COSTUMES = {"lichs_crown": "The Lich's Crown"}

RECIPES = {
    # His part of what summons him again: a Writ of Exhumation with two Grave Dust for its bones and its ectoplasm.
    "exhumation_writ_from_grave_dust": {"shapeless": ["minecraft:paper", "minecraft:paper", "minecraft:paper",
                                                      "jugcraft:grave_dust", "jugcraft:grave_dust", "minecraft:ink_sac"],
                                        "result": "exhumation_writ"},
    # For those who never fight him: bone meal for the garden. Bone meal makes no Grave Dust: no loop.
    "bone_meal_from_grave_dust": {"shapeless": ["jugcraft:grave_dust"], "result": "minecraft:bone_meal", "count": 2},
}

MESSAGES = {
    "wakes": "The Crypt Lich rises from his throne: soul-fire runs down the lanes, and his chains burn",
    "answers": "The Crypt Lich: he answers to %s",
    "exposed_bar": "The Crypt Lich: exposed!",
    "last_bar": "The Crypt Lich: the Last Name",
    "warded": "His chains turn the blow aside: break the lit name's seal",
    "chain_broken": "The seal of %s breaks, and its chain snaps",
    "seal_holds": "That name is dark: the seal holds, and the dead stir",
    "seal_scraped": "The name is scraped from its stone: read it back at the Lectern of Last Names",
    "seal_broken": "That seal is broken already",
    "exposed": "The phylactery cracks: the Crypt Lich is exposed!",
    "reforged": "The chains reforge, and a name lights",
    "erasure": "The Erasure: the Lich shrieks, and scrapes a name from its stone",
    "restored": "The scraped name burns back into its stone",
    "calls": "The Lich calls %s: the lane cracks with blue fire",
    "last_name": "The Last Name: his phylactery shatters, and his chains are gone",
    "reset": "The Crypt Lich returns to his throne, and the names are cut again",
    "defeated": "The Crypt Lich falls: his crown rolls from his skull, and the soul-fire goes out",
    "loot": "His grave goods are yours",
}


def items():
    """Every item the Crypt Lich's fight adds (the Soulreaver and the Gravewarden are Arms VII variants, listed there)."""
    return list(ITEMS) + list(COSTUMES)
