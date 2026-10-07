"""Arms, batch 42 (docs/features/arms.md): longswords, greatswords, rapiers, flanged maces, war hammers, glaives,
halberds, spears and lances in bronze and steel; Arms II, batch 45: daggers, sabres, estocs, battle axes, flails,
scythes, quarterstaves and pikes, each with a trait of its own (TRAITS); Arms III, batch 46: two-handed swings for
the heavy arms (TWO_HANDED) and zweihanders, mauls, executioner's swords and bills; and Arms IV, batch 47: labryses,
battleblades, war forks, kamas and war picks, in an ornate style; and Arms V, batch 48: twinblades, nodachis,
earthbreakers, katars, moonblades and kusarigamas, each with a weapon art (ARTS), a special move with its own animation
and its own shape of damage; Arms VI, batch 55: katanas and brazier maces, longbows and arbalests (RANGED) and shields
(SHIELDS); and Arms VIII, batch 59: thrown arms, javelins, franciscas, chakrams and harpoons (THROWN), each an arm in
the hand that can also be thrown.

After studying Epic Knights (all rights reserved) and Simply Swords (Timefall Development License) for how they draw,
animate and keep their weapons cheap; none of their code, models, numbers or art is used. What carried over is the
approach: one table of weapon kinds and their traits, one sprite and one model per weapon over a shared in-hand pose
per kind, and no per-tick code. Every trait is one of 26.3's own item components (swing animation, attack range,
blocking, piercing and kinetic attacks, shield disabling), so the game runs and checks them like its own weapons.

Java: weapons/JugcraftArms.java (keep KINDS and the numbers in sync; tools/check_mod_data.py checks them). Art:
tools/arms_art.py.
"""
import math

from PIL import Image

import arms_art
import arms_heads
import arms_icons
import gear

MOD = "jugcraft"

# Each kind of arm. Attack damage is added to the metal's bonus (bronze 2, steel 2.5) and the hand's 1, as a sword's
# 3 is; the iron sword makes 6 at 1.6 attacks a second. Speed is the attack-speed modifier (the sword's is -2.4: 4 -
# 2.4 = 1.6 a second). swing: the attack animation and how long it lasts in ticks (a plain swing is 6). reach: the
# nearest and farthest a hit lands, in blocks (a hand reaches 3; creative adds 2); margin widens the target's hitbox.
# disable: seconds a hit stops a shield blocking (the axe's 5). wear: durability a hit costs. knockback: extra attack
# knockback. parry: the share of damage from in front that blocking with it stops (a shield stops all).
# held: how much larger than a sword it is held. pierce: it thrusts through every target in line, as the spear's
# jab does. tags: the vanilla item tags it joins (the enchantments follow).
KINDS = {
    "longsword": {"display": "Longsword", "damage": 4.0, "speed": -2.7, "swing": ("whack", 8), "reach": (0.0, 3.25),
                  "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.6, "held": 1.3,
                  "tags": ["swords"], "pattern": [" # ", " # ", "#L#"],
                  "tooltip": "Use to parry: blocks 60% of the damage from in front."},
    "greatsword": {"display": "Greatsword", "damage": 7.0, "speed": -3.2, "swing": ("whack", 20), "reach": (0.0, 3.75),
                   "margin": 0.0, "disable": 2.0, "wear": 1, "knockback": 0.5, "parry": 0.0, "held": 1.7,
                   "tags": ["swords"], "pattern": [" # ", "###", "#L#"],
                   "tooltip": "Slow, heavy sweeps with a long reach. Staggers shields."},
    "rapier": {"display": "Rapier", "damage": 1.5, "speed": -2.0, "swing": ("stab", 5), "reach": (0.0, 3.5),
               "margin": 0.125, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.35, "held": 1.2,
               "tags": ["swords"], "pattern": ["  #", " # ", "L  "],
               "tooltip": "Quick thrusts. Use to parry: blocks 35% of the damage from in front."},
    "flanged_mace": {"display": "Flanged Mace", "damage": 6.0, "speed": -3.1, "swing": ("whack", 9),
                     "reach": (0.0, 3.0), "margin": 0.0, "disable": 3.0, "wear": 1, "knockback": 0.0, "parry": 0.0,
                     "held": 1.15, "tags": ["enchantable/melee_weapon", "enchantable/durability"],
                     "pattern": [" ##", " ##", "S  "], "tooltip": "Breaks a shield's guard for 3 seconds."},
    "war_hammer": {"display": "War Hammer", "damage": 8.0, "speed": -3.3, "swing": ("whack", 22),
                   "reach": (0.0, 3.0), "margin": 0.0, "disable": 5.0, "wear": 2, "knockback": 1.0, "parry": 0.0,
                   "held": 1.35, "tags": ["enchantable/melee_weapon", "enchantable/durability"],
                   "pattern": ["###", "#S#", " S "], "tooltip": "Knocks foes back and breaks a shield's guard for 5 seconds."},
    "glaive": {"display": "Glaive", "damage": 6.0, "speed": -3.1, "swing": ("whack", 18), "reach": (0.0, 4.25),
               "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 2.0,
               "tags": ["swords"], "pattern": [" ##", " S#", "S  "],
               "tooltip": "A blade on a pole: sweeps at a long reach."},
    "halberd": {"display": "Halberd", "damage": 7.0, "speed": -3.2, "swing": ("stab", 12), "reach": (1.0, 4.5),
                "margin": 0.125, "disable": 3.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 2.1, "pierce": True,
                "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["###", " S#", "S  "],
                "tooltip": "Thrusts through every foe in line, at a long reach. Breaks a shield's guard for 3 seconds."},
    # The spear and the lance charge like vanilla's spears: use and hold to charge, faster with a run or a horse.
    "spear": {"display": "Spear", "pattern": ["  #", " S ", "S  "], "tags": ["spears"], "held": 1.0,
              "tooltip": "Jab, or hold use to charge with it."},
    "lance": {"display": "Lance", "pattern": ["  #", "#S#", "S  "], "tags": ["spears"], "held": 1.3,
              "tooltip": "A horseman's charge: hits harder and unhorses riders. Long reach, slow jabs."},
    # Arms II (batch 45): each has a trait (TRAITS) besides its numbers.
    "dagger": {"display": "Dagger", "damage": 1.0, "speed": -1.7, "swing": ("stab", 4), "reach": (0.0, 2.5),
               "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 0.85,
               "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": [" #", "L "],
               "trait": "backstab", "tooltip": "Quick stabs at a short reach. A blow from behind deals half again as much."},
    "sabre": {"display": "Sabre", "damage": 2.0, "speed": -2.2, "swing": ("whack", 6), "reach": (0.0, 3.0),
              "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.2,
              "tags": ["swords"], "pattern": [" #", " #", "L "], "trait": "saddle",
              "tooltip": "A horseman's blade: quick sweeping cuts, 3 more damage from the saddle."},
    "estoc": {"display": "Estoc", "damage": 3.0, "speed": -2.6, "swing": ("stab", 7), "reach": (0.0, 3.5),
              "margin": 0.125, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.35,
              "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["  #", " # ", "#L "],
              "trait": "armor_pierce", "tooltip": "Thrusts through mail: more damage the more armor the foe wears."},
    "battle_axe": {"display": "Battle Axe", "damage": 8.0, "speed": -3.3, "swing": ("whack", 22), "reach": (0.0, 3.25),
                   "margin": 0.0, "disable": 5.0, "wear": 2, "knockback": 0.5, "parry": 0.0, "held": 1.5,
                   "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["###", "#S ", " S "],
                   "trait": "chop", "tooltip": "Chops wood like an axe and breaks a shield's guard for 5 seconds."},
    "flail": {"display": "Flail", "damage": 5.0, "speed": -3.0, "swing": ("whack", 10), "reach": (0.0, 3.25),
              "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.2,
              "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["  #", " # ", "S  "],
              "trait": "daze", "tooltip": "A hit dazes the foe, slowing it for 2 seconds."},
    "scythe": {"display": "Scythe", "damage": 5.0, "speed": -3.0, "swing": ("whack", 18), "reach": (0.0, 4.0),
               "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.9,
               "tags": ["swords"], "pattern": ["###", "  S", " S "], "trait": "reap",
               "tooltip": "Wide sweeps at a long reach. Use on ripe crops to reap them, 3 by 3, and replant."},
    "quarterstaff": {"display": "Quarterstaff", "damage": 2.0, "speed": -2.4, "swing": ("whack", 12), "reach": (0.0, 3.5),
                     "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 1.0, "parry": 0.5, "held": 1.8,
                     "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["  #", " S ", "#  "],
                     "tooltip": "Knocks foes back. Use to parry: blocks 50% of the damage from in front."},
    "pike": {"display": "Pike", "damage": 5.0, "speed": -3.2, "swing": ("stab", 16), "reach": (2.0, 5.0),
             "margin": 0.125, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 2.3,
             "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["  #", " S ", "SS "],
             "trait": "riders",
             "tooltip": "The longest reach, but nothing nearer than 2 blocks. Half again as much damage to riders and mounts."},
    # Arms III (batch 46): two-handed arms, swung as TWO_HANDED says.
    "zweihander": {"display": "Zweihander", "damage": 7.5, "speed": -3.2, "swing": ("whack", 22), "reach": (0.0, 4.0),
                   "margin": 0.0, "disable": 2.0, "wear": 1, "knockback": 0.5, "parry": 0.5, "held": 2.1,
                   "tags": ["swords"], "pattern": ["  #", "## ", "L# "],
                   "tooltip": "The widest cleave. Use to guard: blocks 50% of the damage from in front."},
    "maul": {"display": "Maul", "damage": 10.0, "speed": -3.45, "swing": ("whack", 24), "reach": (0.0, 3.25),
             "margin": 0.0, "disable": 5.0, "wear": 2, "knockback": 1.5, "parry": 0.0, "held": 1.7,
             "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["###", "###", " S "],
             "trait": "quake",
             "tooltip": "The heaviest blow. Its finishing blow shakes the ground, staggering every foe close by."},
    "executioner": {"display": "Executioner's Sword", "damage": 8.0, "speed": -3.3, "swing": ("whack", 22),
                    "reach": (0.0, 3.5), "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.5, "parry": 0.0,
                    "held": 1.9, "tags": ["swords"], "pattern": ["## ", "## ", " L#"], "trait": "execute",
                    "tooltip": "Half again as much damage to a foe at or below 30% of its health."},
    "bill": {"display": "Bill", "damage": 6.0, "speed": -3.1, "swing": ("whack", 18), "reach": (0.0, 4.5),
             "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 2.2,
             "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": [" ##", " S ", "S  "],
             "trait": "hook", "tooltip": "A hooked polearm: pulls the foes it strikes towards you and drags riders from the saddle."},
    # Arms IV (batch 47): shapes from the owner's reference sheets, in the ornate style (tools/arms_art.py).
    "labrys": {"display": "Labrys", "damage": 8.5, "speed": -3.3, "swing": ("whack", 22), "reach": (0.0, 3.25),
               "margin": 0.0, "disable": 3.0, "wear": 1, "knockback": 0.5, "parry": 0.0, "held": 1.6,
               "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["#S#", "#S#", " S "],
               "trait": "whirl", "tooltip": "A double-bitted axe. Its finishing blow whirls right round, striking every foe about you."},
    "battleblade": {"display": "Battleblade", "damage": 7.0, "speed": -3.2, "swing": ("whack", 20), "reach": (0.0, 3.5),
                    "margin": 0.0, "disable": 2.0, "wear": 1, "knockback": 0.5, "parry": 0.0, "held": 1.8,
                    "tags": ["swords"], "pattern": [" ##", "###", "L# "], "trait": "sunder",
                    "tooltip": "A great saw-backed cleaver: each hit wears every piece of the foe's armor."},
    "war_fork": {"display": "War Fork", "damage": 5.5, "speed": -3.1, "swing": ("stab", 18), "reach": (0.0, 4.5),
                 "margin": 0.125, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 2.2,
                 "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["# #", "#S#", " S "],
                 "trait": "brace",
                 "tooltip": "A barbed fork set against a charge: half again as much damage to a foe coming at you."},
    "kama": {"display": "Kama", "damage": 1.5, "speed": -2.0, "swing": ("whack", 5), "reach": (0.0, 2.75),
               "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.15,
               "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["## ", "  #", " S "],
               "trait": "clear",
               "tooltip": "Quick hooking cuts. Use on grass, ferns, vines or leaves to cut all of them about it, 3 by 3 by 3."},
    "war_pick": {"display": "War Pick", "damage": 3.0, "speed": -2.6, "swing": ("whack", 7), "reach": (0.0, 3.0),
                 "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.25,
                 "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["## ", " S#", " S "],
                 "trait": "delve", "tooltip": "A beaked war pick that mines stone and ore as its metal's pickaxe does."},
    # Arms V (batch 48): each has a weapon art (ARTS), a special move of its own, used with the use key.
    "twinblade": {"display": "Twinblade", "damage": 4.0, "speed": -2.8, "swing": ("whack", 14), "reach": (0.0, 3.25),
                  "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.7,
                  "tags": ["swords"], "pattern": [" ##", " L ", "## "], "art": "cyclone",
                  "tooltip": "A blade at each end of the grip: quick cuts, turn and turn about."},
    "nodachi": {"display": "Nodachi", "damage": 6.5, "speed": -3.2, "swing": ("whack", 20), "reach": (0.0, 4.0),
                "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 2.0,
                "tags": ["swords"], "pattern": ["  #", " ##", "L  "], "art": "iaido",
                "tooltip": "A great curved sword, drawn and swung with both hands at a long reach."},
    "earthbreaker": {"display": "Earthbreaker", "damage": 9.5, "speed": -3.45, "swing": ("whack", 24), "reach": (0.0, 3.25),
                     "margin": 0.0, "disable": 5.0, "wear": 2, "knockback": 1.0, "parry": 0.0, "held": 1.8,
                     "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["###", "#S#", " SS"],
                     "art": "leap_slam", "tooltip": "A siege hammer. Knocks foes back and breaks a shield's guard for 5 seconds."},
    "katar": {"display": "Katar", "damage": 1.5, "speed": -2.0, "swing": ("stab", 5), "reach": (0.0, 2.75),
              "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 0.95,
              "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["# #", "#L#"], "art": "flurry",
              "tooltip": "A punching blade, gripped across its frame: quick straight jabs at a short reach."},
    "moonblade": {"display": "Moonblade", "damage": 6.0, "speed": -3.1, "swing": ("whack", 18), "reach": (0.0, 3.75),
                  "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.8,
                  "tags": ["swords"], "pattern": ["## ", "  #", "L# "], "art": "crescent",
                  "tooltip": "A crescent-bladed greatsword: broad, sweeping cuts."},
    "kusarigama": {"display": "Kusarigama", "damage": 2.0, "speed": -2.3, "swing": ("whack", 6), "reach": (0.0, 3.25),
                   "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.15,
                   "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["## ", "  #", "NN "],
                   "art": "chain_lash", "tooltip": "A sickle on a weighted chain: quick hooking cuts."},
    # Arms VI (batch 55): after the owner's reference sheets of a twin-katana set and an iron-and-wood war kit (studied
    # for their look; nothing is copied). The bows, crossbows and shields of the batch are in RANGED and SHIELDS.
    "katana": {"display": "Katana", "damage": 3.0, "speed": -2.5, "swing": ("whack", 7), "reach": (0.0, 3.25),
               "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.3,
               "tags": ["swords"], "pattern": ["  #", " #N", "L  "], "art": "seven_cuts",
               "tooltip": "A curved single-edged blade, drawn and cut in one motion: quick, clean cuts."},
    "brazier_mace": {"display": "Brazier Mace", "damage": 5.0, "speed": -3.0, "swing": ("whack", 10), "reach": (0.0, 3.0),
                     "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.25,
                     "tags": ["enchantable/melee_weapon", "enchantable/durability", "enchantable/fire_aspect"],
                     "pattern": ["#C#", " # ", " S "], "trait": "ignite",
                     "tooltip": "A mace whose head is a burning brazier: sets foes alight. Use it to light a campfire, a candle or the ground."},
    # Arms VIII (batch 59): thrown arms. Each fights in the hand by its numbers here, and is thrown with the use key
    # (THROWN): hold to wind it back, let go to throw.
    "javelin": {"display": "Javelin", "damage": 2.5, "speed": -2.6, "swing": ("stab", 7), "reach": (0.0, 3.25),
                "margin": 0.125, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.6,
                "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["  #", " S ", "SN "],
                "tooltip": "A light spear made for throwing: quick jabs in the hand."},
    "francisca": {"display": "Francisca", "damage": 4.0, "speed": -2.9, "swing": ("whack", 9), "reach": (0.0, 3.0),
                  "margin": 0.0, "disable": 2.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.0,
                  "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": [" #", "#S"], "trait": "chop",
                  "tooltip": "A throwing axe with an upswept bit. Chops wood like an axe."},
    "chakram": {"display": "Chakram", "damage": 2.0, "speed": -2.2, "swing": ("whack", 6), "reach": (0.0, 2.75),
                "margin": 0.0, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 0.95,
                "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": [" # ", "# #", " L "],
                "tooltip": "A bladed ring, held by its wrapped rim: quick slicing cuts."},
    "harpoon": {"display": "Harpoon", "damage": 3.0, "speed": -2.7, "swing": ("stab", 8), "reach": (0.0, 3.25),
                "margin": 0.125, "disable": 0.0, "wear": 1, "knockback": 0.0, "parry": 0.0, "held": 1.6,
                "tags": ["enchantable/melee_weapon", "enchantable/durability"], "pattern": ["  #", "#S ", "T  "],
                "tooltip": "A barbed spear on a line: stabs in the hand."},
}
# Arms II traits (weapons/ArmItem.java; JugcraftArms.TRAITS). backstab: a blow landing within BACKSTAB_ANGLE degrees of
# straight behind the target's body deals BACKSTAB more (a share of the blow). saddle: SADDLE more damage while riding.
# armor_pierce: ARMOR_PIERCE more for each point of the target's armor, at most ARMOR_PIERCE_MAX. chop: mines wood as an
# axe of its metal does. daze: a hit slows the target (DAZE: ticks, Slowness amplifier). reap: use on a ripe crop to
# harvest every ripe crop within REAP_RADIUS and replant it, at REAP_WEAR durability each. riders: RIDERS more (a share)
# against anything riding or ridden.
TRAITS = {kind: info["trait"] for kind, info in KINDS.items() if "trait" in info}
BACKSTAB = 0.5
BACKSTAB_ANGLE = 70.0
SADDLE = 3.0
ARMOR_PIERCE = 0.3
ARMOR_PIERCE_MAX = 6.0
DAZE = (40, 1)
REAP_RADIUS = 1
REAP_WEAR = 1
RIDERS = 0.5
# Arms III traits. quake: the maul's finishing blow also strikes every foe within QUAKE_RADIUS blocks (and a block of the
# wielder's footing) that the cleave missed, for QUAKE_SHARE of the blow, and slows every foe there (QUAKE_DAZE: ticks,
# Slowness amplifier). execute: EXECUTE
# more (a share of the blow) against a foe at or below EXECUTE_HEALTH of its most health. hook: a hit pulls the foe
# towards the wielder at HOOK blocks a tick (less its knockback resistance) and drags a rider from the saddle.
QUAKE_RADIUS = 2.5
QUAKE_SHARE = 0.5
QUAKE_DAZE = (40, 1)
EXECUTE = 0.5
EXECUTE_HEALTH = 0.3
HOOK = 0.6
# Arms IV traits. whirl: the labrys's finishing blow sweeps all round (WHIRL_ARC degrees) and strikes up to WHIRL_TARGETS.
# sunder: each hit wears every piece of armor the foe wears by SUNDER more. brace: BRACE more (a share of the blow) to a
# foe closing on the wielder at BRACE_SPEED blocks a tick or faster (from where it was a tick before). clear: use on a
# block in #jugcraft:kama_cuts to cut every such block within CLEAR_RADIUS (a cube), dropping what each drops, at
# CLEAR_WEAR durability each. delve: mines as its metal's pickaxe.
WHIRL_ARC = 360
WHIRL_TARGETS = 6
SUNDER = 4
BRACE = 0.5
BRACE_SPEED = 0.1
CLEAR_RADIUS = 1
CLEAR_WEAR = 1
# Arms VI traits. ignite: a hit sets the foe alight for IGNITE_SECONDS; use on a block lights it as flint and steel does
# (a campfire or candle, or fire on the face used), at IGNITE_WEAR durability.
IGNITE_SECONDS = 4
IGNITE_WEAR = 1
# What a kama cuts (data/jugcraft/tags/block/kama_cuts.json): leaves and the plants that grow wild, never crops.
KAMA_CUTS = ["#minecraft:leaves", "minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern",
               "minecraft:vine", "minecraft:dead_bush", "minecraft:glow_lichen", "minecraft:hanging_roots",
               {"id": "minecraft:short_dry_grass", "required": False}, {"id": "minecraft:tall_dry_grass", "required": False},
               {"id": "minecraft:bush", "required": False}, {"id": "minecraft:leaf_litter", "required": False}]

# Arms III (batch 46): two-handed swings (weapons/TwoHanded.java; client/arms/TwoHandedInput.java), after studying the
# greatsword of Fiery Combat (a Bedrock add-on; nothing of it is used). A click with one of these in the main hand
# starts a committed swing instead of vanilla's instant hit:
#   strike: the tick the blow lands, counted from the click; it is when the kind's motion lands its blow
#     (tools/arms_moves.py: MOVES[kind]["blow"] times the swing's ticks, checked to half a tick);
#   arc: how wide the blow sweeps, in degrees across the wielder's view; every foe in it within the arm's reach and
#     in sight is struck, nearest first, up to `targets`;
#   combo: how many attacks the kind's combo has (as its motion); the last is the finishing blow, FINISHER times as
#     strong.
# While a swing is in the air the wielder moves at 1 - TWO_HANDED_SLOW of their speed and cannot sprint, and a shield
# (anything that blocks) in the off hand stops a swing from starting. A click within QUEUE_TICKS of a swing's end
# waits for it; COMBO_WINDOW ticks without a click start the combo again. The blow is as strong as the attack charge
# was at the click (vanilla's rule), and each foe is struck through vanilla's own thrust attack (Player.stabAttack):
# enchantments, knockback, wear and the kind's trait.
TWO_HANDED = {
    "greatsword": {"strike": 7, "arc": 120, "targets": 4, "combo": 2},
    "war_hammer": {"strike": 8, "arc": 70, "targets": 2, "combo": 2},
    "glaive": {"strike": 6, "arc": 120, "targets": 4, "combo": 2},
    "battle_axe": {"strike": 8, "arc": 90, "targets": 3, "combo": 2},
    "scythe": {"strike": 6, "arc": 150, "targets": 5, "combo": 2},
    "quarterstaff": {"strike": 4, "arc": 100, "targets": 3, "combo": 3},
    "pike": {"strike": 5, "arc": 20, "targets": 3, "combo": 2},
    "zweihander": {"strike": 7, "arc": 140, "targets": 5, "combo": 3},
    "maul": {"strike": 9, "arc": 90, "targets": 3, "combo": 2},
    "executioner": {"strike": 8, "arc": 90, "targets": 2, "combo": 2},
    "bill": {"strike": 6, "arc": 90, "targets": 3, "combo": 2},
    "labrys": {"strike": 8, "arc": 100, "targets": 3, "combo": 2},
    "battleblade": {"strike": 7, "arc": 110, "targets": 4, "combo": 2},
    "war_fork": {"strike": 6, "arc": 30, "targets": 2, "combo": 2},
    "twinblade": {"strike": 5, "arc": 140, "targets": 3, "combo": 3},
    "nodachi": {"strike": 7, "arc": 100, "targets": 3, "combo": 2},
    "earthbreaker": {"strike": 9, "arc": 80, "targets": 2, "combo": 2},
    "moonblade": {"strike": 6, "arc": 130, "targets": 4, "combo": 2},
}
# Arms V (batch 48): weapon arts (weapons/WeaponArts.java). Each Arms V kind has one special move, used with the use key
# (right click) with the arm in the main hand: it plays its own animation (tools/arms_moves.py: MOVES[kind]["arts"]) and
# deals its damage in a shape of its own, then the arm needs `cooldown` ticks before its art is ready again (shown on
# the hotbar as an item cooldown; plain blows are not held back). `ticks` is how long the wielder is busy with it (the
# animation's length; the leap's spring, before the air), and the wielder moves at 1 - `slow` of their speed meanwhile.
# Every hit of an art is the arm's attack damage times the move's share, struck through vanilla's thrust attack
# (Player.stabAttack: enchantments, knockback, wear, the item's hit hooks) at a full charge, on the server.
ARTS = {
    "twinblade": {"move": "cyclone", "cooldown": 120, "ticks": 24, "slow": 0.3,
                  "name": "Cyclone", "text": "spin three times, cutting every foe about you each time round and drawing them in."},
    "nodachi": {"move": "iaido", "cooldown": 160, "ticks": 18, "slow": 0.0,
                "name": "Iaido", "text": "dash ahead; every foe you pass is cut a moment later, as the blade comes round."},
    "earthbreaker": {"move": "leap_slam", "cooldown": 200, "ticks": 8, "slow": 0.0,
                     "name": "Leap Slam", "text": "leap and bring the hammer down where you land: harder at the centre, and harder the further you came down."},
    "katar": {"move": "flurry", "cooldown": 100, "ticks": 20, "slow": 0.5,
              "name": "Flurry", "text": "five quick jabs at the foe ahead, too fast to be shrugged off, then a driving finish."},
    "moonblade": {"move": "crescent", "cooldown": 140, "ticks": 16, "slow": 0.5,
                  "name": "Crescent", "text": "loose a crescent wave that runs ahead, through every foe in its way, until it meets a wall."},
    "kusarigama": {"move": "chain_lash", "cooldown": 120, "ticks": 16, "slow": 0.5,
                   "name": "Chain Lash", "text": "throw the weighted chain at the first foe in line, up to 9 blocks off, haul it in and reap it."},
    # Arms VI (batch 55).
    "katana": {"move": "seven_cuts", "cooldown": 120, "ticks": 18, "slow": 0.4,
               "name": "Seven Cuts", "text": "seven cuts in a breath, each across every foe ahead, leaving arcs in the air."},
}
# cyclone: hits at CYCLONE_FIRST, then every CYCLONE_EVERY ticks, CYCLONE_HITS in all; each strikes every foe within
# CYCLONE_RADIUS (all round, up to CYCLONE_TARGETS) for CYCLONE_SHARE, without knockback, and draws it CYCLONE_PULL
# blocks a tick towards the wielder; a foe's damage cooldown is let go before each, so all three land.
CYCLONE_FIRST = 6
CYCLONE_EVERY = 6
CYCLONE_HITS = 3
CYCLONE_RADIUS = 3.0
CYCLONE_SHARE = 0.5
CYCLONE_PULL = 0.15
CYCLONE_TARGETS = 8
# iaido: from tick IAIDO_START the wielder dashes along their level view at IAIDO_SPEED blocks a tick for IAIDO_DASH
# ticks (stopped by what stops a player); every foe within IAIDO_WIDTH of the path is marked, up to IAIDO_TARGETS; the
# cut lands on them IAIDO_DELAY ticks after the dash ends, for IAIDO_SHARE, wherever they are by then (within
# IAIDO_REACH of the wielder).
IAIDO_START = 3
IAIDO_DASH = 5
IAIDO_SPEED = 1.2
IAIDO_WIDTH = 1.25
IAIDO_DELAY = 4
IAIDO_SHARE = 1.3
IAIDO_TARGETS = 6
IAIDO_REACH = 10.0
# leap_slam: only from the ground. The wielder springs up at LEAP_UP and ahead at LEAP_FORWARD blocks a tick; on
# landing (back on the ground after leaving it, at least LEAP_MIN_AIR ticks later; where they stand at LEAP_STUCK if a
# low ceiling kept them down; given up after LEAP_MAX_AIR) every foe within LEAP_RADIUS and 1.5 blocks
# of the landing height takes LEAP_SHARE at the centre, falling to LEAP_EDGE of that at the edge, plus LEAP_PER_BLOCK
# for each block the wielder landed below where they leapt from (at most LEAP_DROP_MAX), and is thrown up at LEAP_LIFT.
# The leap's own height costs no fall damage (vanilla's impulse rule: only the drop below the take-off counts).
LEAP_UP = 0.8
LEAP_FORWARD = 0.5
LEAP_MIN_AIR = 4
LEAP_STUCK = 12
LEAP_MAX_AIR = 60
LEAP_RADIUS = 3.5
LEAP_SHARE = 1.0
LEAP_EDGE = 0.5
LEAP_PER_BLOCK = 0.15
LEAP_DROP_MAX = 6.0
LEAP_LIFT = 0.45
LEAP_TARGETS = 8
# flurry: FLURRY_JABS jabs from FLURRY_FIRST, every FLURRY_EVERY ticks, then the finish; each at the nearest foe within
# the arm's reach and FLURRY_ARC degrees of the view, for FLURRY_SHARE (the finish FLURRY_FINISH, with knockback). A
# foe's damage cooldown is let go before each jab.
FLURRY_FIRST = 2
FLURRY_EVERY = 3
FLURRY_JABS = 5
FLURRY_SHARE = 0.28
FLURRY_FINISH = 0.9
FLURRY_ARC = 50.0
# crescent: at CRESCENT_RELEASE the wave leaves at waist height and runs along the level view at CRESCENT_SPEED blocks a
# tick for CRESCENT_TICKS ticks, or until a block stops it; it strikes each foe within CRESCENT_WIDTH of its line once
# (up to CRESCENT_TARGETS), for CRESCENT_SHARE, less CRESCENT_FADE of it for each foe it has already passed through. It
# runs on its own once loosed, but fades if its wielder lets go of the moonblade.
CRESCENT_RELEASE = 5
CRESCENT_SPEED = 1.2
CRESCENT_TICKS = 10
CRESCENT_WIDTH = 1.5
CRESCENT_SHARE = 0.9
CRESCENT_FADE = 0.15
CRESCENT_TARGETS = 6
# chain_lash: at LASH_THROW the chain flies from the eye along the view up to LASH_RANGE blocks, stopped by blocks; the
# first foe on it takes LASH_SHARE and is hauled in at LASH_PULL blocks a tick for each block it is off (at most
# LASH_PULL_MAX, less its knockback resistance) and dragged from the saddle; at LASH_REAP, if it is within
# LASH_REAP_REACH, the sickle reaps it for LASH_REAP_SHARE.
LASH_THROW = 4
LASH_RANGE = 9.0
LASH_SHARE = 0.5
LASH_PULL = 0.2
LASH_PULL_MAX = 1.6
LASH_REAP = 11
LASH_REAP_SHARE = 0.8
LASH_REAP_REACH = 3.5

# seven_cuts: CUTS_COUNT cuts from CUTS_FIRST, every CUTS_EVERY ticks; each strikes every foe within the katana's reach
# and CUTS_ARC degrees of the view (up to CUTS_TARGETS) for CUTS_SHARE, holding it in reach (no knockback) but the last; each
# leaves an arc of colour in the air (the metal's: crimson for bronze, pale gold for steel).
CUTS_FIRST = 3
CUTS_EVERY = 2
CUTS_COUNT = 7
CUTS_SHARE = 0.22
CUTS_ARC = 110.0
CUTS_TARGETS = 4


def art_share(kind):
    """What an art deals one foe, in blows of the arm (on level ground, for the leap)."""
    move = ARTS[kind]["move"]
    return {"cyclone": CYCLONE_HITS * CYCLONE_SHARE, "iaido": IAIDO_SHARE, "leap_slam": LEAP_SHARE,
            "flurry": FLURRY_JABS * FLURRY_SHARE + FLURRY_FINISH, "crescent": CRESCENT_SHARE,
            "chain_lash": LASH_SHARE + LASH_REAP_SHARE, "seven_cuts": CUTS_COUNT * CUTS_SHARE}[move]


# Arms VI (batch 55): bows and crossbows in the arms' metals (weapons/ArmBowItem.java, ArmCrossbowItem.java), after the
# owner's reference sheets. A longbow draws fully in `draw` ticks (vanilla's bow: 20) on vanilla's curve, and looses its
# arrow at `speed` blocks a tick (vanilla's bow: 3.0); an arbalest loads as vanilla's crossbow does and shoots its bolt
# at `speed` (vanilla's: 3.15). An arrow's `damage` is its base damage (vanilla's: 2.0), which the game multiplies by its
# speed when it hits: the longbow's arrows and the arbalest's bolts hit harder for flying faster. Balance: each shot hits
# harder and flies flatter than vanilla's, but a second (speed x damage over the draw, or vanilla's 1.25 s load) stays
# below vanilla's bow (3.0 x 2.0 over its 1 s draw: 6), as the melee arms stay below the sword (tools/check_mod_data.py).
RANGED_KINDS = {
    "longbow": {"display": "Longbow", "type": "bow", "pattern": ["#ST", "S T", "#ST"], "tags": ["enchantable/bow", "enchantable/durability"],
                "tooltip": "A tall bow: slower to draw than a bow, but its arrows fly faster and hit harder."},
    "arbalest": {"display": "Arbalest", "type": "crossbow", "pattern": ["###", "T$T", " S "],
                 "tags": ["enchantable/crossbow", "enchantable/durability"],
                 "tooltip": "A crossbow with a metal prod: its bolts fly faster and hit harder."},
}
RANGED = {
    ("longbow", "bronze"): {"draw": 26, "speed": 3.4, "damage": 2.0},
    ("longbow", "steel"): {"draw": 26, "speed": 3.7, "damage": 2.0},
    ("arbalest", "bronze"): {"speed": 3.4, "damage": 2.1},
    ("arbalest", "steel"): {"speed": 3.55, "damage": 2.1},
}
# Shields in the arms' metals (Arms VI): blocking as vanilla's shield does (the blocks-attacks component, on the use key),
# with their own numbers: `delay`, seconds to raise (vanilla's: 0.25); `angle`, degrees either side of straight ahead
# covered (vanilla's: 90); `disable`, how long an axe's blow stops it, as a share of vanilla's; `wear`, the share of a
# blocked blow's damage it takes in wear (vanilla's: 1); and, held in either hand, `brace` knockback resistance and
# `weight`, a share of speed lost. 3D models, tools/arms_kit.py.
SHIELD_KINDS = {
    "heater_shield": {"display": "Heater Shield", "pattern": ["#W#", "WWW", " W "], "tags": ["enchantable/durability"],
                      "tooltip": "A light shield, quick to raise."},
    "tower_shield": {"display": "Tower Shield", "pattern": ["#W#", "#W#", "#W#"], "tags": ["enchantable/durability"],
                     "tooltip": "A great shield that covers your flanks and braces you against blows; heavy to carry and slow to raise."},
}
SHIELDS = {
    ("heater_shield", "bronze"): {"delay": 0.15, "angle": 90.0, "disable": 1.0, "durability": 400, "wear": 1.0, "brace": 0.0, "weight": 0.0},
    ("heater_shield", "steel"): {"delay": 0.1, "angle": 90.0, "disable": 0.8, "durability": 900, "wear": 1.0, "brace": 0.0, "weight": 0.0},
    ("tower_shield", "bronze"): {"delay": 0.4, "angle": 130.0, "disable": 0.6, "durability": 600, "wear": 0.75, "brace": 0.4, "weight": 0.08},
    ("tower_shield", "steel"): {"delay": 0.35, "angle": 130.0, "disable": 0.5, "durability": 1350, "wear": 0.75, "brace": 0.5, "weight": 0.08},
}
VANILLA_SHIELD = {"delay": 0.25, "angle": 90.0, "disable": 1.0, "durability": 336}
VANILLA_BOW = {"draw": 20, "speed": 3.0, "damage": 2.0}
VANILLA_CROSSBOW = {"load": 25, "speed": 3.15, "damage": 2.0}
# The sprites a bow and a crossbow are drawn in: as vanilla's, at rest and drawn in three steps; a crossbow also loaded
# with an arrow or a firework. A bow is held `held` times a bow's size (vanilla's item/bow poses, scaled about its grip
# at the sprite's centre), a crossbow as vanilla's crossbow.
RANGED_SPRITES = {"bow": ["", "_pulling_0", "_pulling_1", "_pulling_2"],
                  "crossbow": ["_standby", "_pulling_0", "_pulling_1", "_pulling_2", "_arrow", "_firework"]}
RANGED_HELD = {"longbow": 1.3, "arbalest": 1.15}

# Each kind's trait as it is named in its tooltip (weapons/ArmItem.java, gear/TraitTooltips.java): the name always, and
# the kind's "tooltip" sentence under it while Shift is held (docs/features/trait-details.md). Short, so a tooltip stays
# narrow; tools/check_mod_data.py checks that every kind, bow, crossbow and shield has one.
TRAIT_NAMES = {
    "longsword": "Parry", "greatsword": "Heavy Sweeps", "rapier": "Quick Parry", "flanged_mace": "Guard Break",
    "war_hammer": "Guard Break", "glaive": "Long Reach", "halberd": "Line Thrust", "spear": "Charge",
    "lance": "Mounted Charge", "dagger": "Backstab", "sabre": "Saddle Blade", "estoc": "Armor Pierce", "battle_axe": "Chop",
    "flail": "Daze", "scythe": "Reap", "quarterstaff": "Parry", "pike": "Longest Reach", "zweihander": "Wide Cleave",
    "maul": "Quake", "executioner": "Execute", "bill": "Hook", "labrys": "Whirl", "battleblade": "Sunder",
    "war_fork": "Brace", "kama": "Clear Brush", "war_pick": "Delve", "twinblade": "Twin Blades", "nodachi": "Long Reach",
    "earthbreaker": "Guard Break", "katar": "Quick Jabs", "moonblade": "Broad Sweeps", "kusarigama": "Hooking Cuts",
    "katana": "Clean Cuts", "brazier_mace": "Ignite",
    "longbow": "Heavy Draw", "arbalest": "Metal Prod", "heater_shield": "Quick Raise", "tower_shield": "Bulwark",
    "javelin": "Light Spear", "francisca": "Chop", "chakram": "Slicing Cuts", "harpoon": "Barbed Line",
}
# A shield's sprites: its painted face, its bare back (and grip strap), and its metal trim (rim and boss).
SHIELD_SPRITES = ["_face", "_back", "_trim"]

# Arms VIII (batch 59): thrown arms (weapons/ThrownArmItem.java, ThrownArm.java). Hold use to wind one back and let go to
# throw it, as vanilla's trident is thrown; a hold shorter than `wind` ticks does not throw. It leaves at `speed` blocks a
# tick and falls `gravity` blocks a tick faster each tick (a snowball's 0.03; the chakram flies flat), and what it strikes
# takes `damage` (vanilla's trident: 8), more for the arm's damage enchantments, as the trident's Impaling adds to its
# throw. Then it falls where it struck, as itself, with its enchantments and wear, to be picked up again; the chakram
# flies back to its thrower. A throw wears it by THROW_WEAR, and another of its kind and metal can be thrown
# THROW_COOLDOWN ticks later. Balance: no throw hits harder than the trident's, and throwing one after another (the wind
# and the cooldown) deals less a second than a netherite sword's blows (tools/check_mod_data.py).
THROWN = {
    ("javelin", "bronze"): {"wind": 10, "speed": 2.6, "gravity": 0.035, "damage": 7.0},
    ("javelin", "steel"): {"wind": 10, "speed": 2.8, "gravity": 0.035, "damage": 8.0},
    ("francisca", "bronze"): {"wind": 6, "speed": 1.6, "gravity": 0.06, "damage": 6.0},
    ("francisca", "steel"): {"wind": 6, "speed": 1.7, "gravity": 0.06, "damage": 7.0},
    ("chakram", "bronze"): {"wind": 6, "speed": 1.4, "gravity": 0.0, "damage": 4.0},
    ("chakram", "steel"): {"wind": 6, "speed": 1.5, "gravity": 0.0, "damage": 5.0},
    ("harpoon", "bronze"): {"wind": 10, "speed": 2.0, "gravity": 0.04, "damage": 5.0},
    ("harpoon", "steel"): {"wind": 10, "speed": 2.2, "gravity": 0.04, "damage": 6.0},
}
THROWN_KINDS = ["javelin", "francisca", "chakram", "harpoon"]
THROW_WEAR = 1
THROW_COOLDOWN = 10
VANILLA_TRIDENT = {"wind": 10, "speed": 2.5, "damage": 8.0}
# What each does when thrown, after its tooltip's first line (tools/arms.py writes tooltip.jugcraft.arms.throw.<kind>).
THROW_TEXT = {
    "javelin": "It flies far and straight.",
    "francisca": "It tumbles end over end and breaks a raised shield's guard for {disable:g} seconds.",
    "chakram": "It flies flat, cuts every foe on its way out and back, and returns to your hand.",
    "harpoon": "It keeps its speed underwater and hauls what it strikes towards you.",
}
# francisca: one that strikes a foe blocking with a shield (or a parrying arm) stops it blocking for FRANCISCA_DISABLE
# seconds, as an axe's blow does (less for a shield that shrugs off axes, as the tower shield does).
FRANCISCA_DISABLE = 3.0
# chakram: it flies out until it is CHAKRAM_RANGE blocks from where it was thrown or meets a block, then turns back to
# its thrower at CHAKRAM_RETURN blocks a tick, through anything in its way. It strikes each foe it passes once on the way
# out and once on the way back, up to CHAKRAM_TARGETS each way. Within CHAKRAM_CATCH blocks of its thrower it is caught
# (into the inventory, or at their feet if that is full); if its thrower is gone, or it has flown CHAKRAM_MAX_TICKS, it
# falls where it is.
CHAKRAM_RANGE = 12.0
CHAKRAM_RETURN = 1.2
CHAKRAM_TARGETS = 4
CHAKRAM_CATCH = 1.5
CHAKRAM_MAX_TICKS = 100
# harpoon: underwater it keeps HARPOON_WATER of its speed each tick (as in air; a thrown thing keeps 0.8 in water); a foe
# it strikes is hauled towards its thrower at HARPOON_PULL blocks a tick for each block it is off (at most
# HARPOON_PULL_MAX), less its knockback resistance, and dragged from the saddle.
HARPOON_WATER = 0.99
HARPOON_PULL = 0.2
HARPOON_PULL_MAX = 1.6
# How large each is drawn in flight: its 3D model's square, in blocks (client/ThrownArmRenderer.java).
FLIGHT = {"javelin": 1.4, "francisca": 0.85, "chakram": 0.8, "harpoon": 1.4}


def throw_per_second(kind, metal):
    """A thrown arm's damage a second, thrown one after another as fast as the wind and THROW_COOLDOWN allow."""
    t = THROWN[(kind, metal)]
    return t["damage"] * 20.0 / (t["wind"] + THROW_COOLDOWN)


TWO_HANDED_SLOW = 0.6
FINISHER = 1.25
QUEUE_TICKS = 4
COMBO_WINDOW = 30
# Every gear metal makes every kind, except a metal marked "arms": False (thallite, whose drawn kinds come later).
METALS = [metal for metal, info in gear.GEAR_TIERS.items() if info.get("arms", True)]
# The charging kinds, as Item.Properties.spear takes them, by metal: jab duration (s), charge damage multiplier,
# charge delay (s), then for unhorsing, knockback and damage the longest a charge counts (s) and the speed it needs.
# Vanilla's iron spear is (0.95, 0.95, 0.6, 2.5, 11.0, 6.75, 5.1, 11.25, 4.6) and diamond (1.05, 1.075, 0.5, 3.0,
# 10.0, 6.5, 5.1, 10.0, 4.6). The lance trades a slow jab for a harder charge that unhorses at lower speeds.
CHARGE = {
    ("spear", "bronze"): (0.95, 1.0, 0.6, 2.5, 11.0, 6.75, 5.1, 11.25, 4.6),
    ("spear", "steel"): (1.0, 1.04, 0.55, 2.75, 10.5, 6.6, 5.1, 10.6, 4.6),
    ("lance", "bronze"): (1.25, 1.3, 0.9, 3.5, 9.0, 8.0, 4.5, 14.0, 4.0),
    ("lance", "steel"): (1.3, 1.4, 0.8, 4.0, 8.5, 8.5, 4.5, 15.0, 4.0),
}
# The lance's jab adds this to the spear's (the metal's bonus) and reaches farther.
LANCE_DAMAGE = 1.0
LANCE_REACH = (2.5, 5.5)
CHARGING = ("spear", "lance")
# What a parry's blocking is like: the angle either side of straight ahead it covers (degrees; a shield's is 90), the
# delay before it blocks (s; a shield's is 0.25), and its wear: a parried hit of 3 or more costs 1 + a share of it.
PARRY_ANGLE = 60.0
PARRY_DELAY = 0.1
PARRY_WEAR = (3.0, 1.0, 0.5)
# Crafting keys besides "#", the metal's ingot.
KEYS = {"S": "minecraft:stick", "L": "minecraft:leather", "N": "minecraft:iron_nugget", "C": "#minecraft:coals",
        "T": "minecraft:string", "$": "minecraft:tripwire_hook", "W": "#minecraft:planks"}

# Vanilla's item/handheld hand poses (rotation, translation, scale) and where a vanilla sword sprite is held, in
# pixels from its centre (x right, y up).
HANDHELD = {
    "thirdperson_righthand": ((0, -90, 55), (0, 4.0, 0.5), 0.85),
    "firstperson_righthand": ((0, -90, 25), (1.13, 3.2, 1.13), 0.68),
}
SWORD_GRIP = (-4.5, -4.5)
# Vanilla's item/spear_in_hand poses, for the lance (its sprite is drawn point to the top left), and where that sprite
# is held.
SPEAR_IN_HAND = {
    "thirdperson_righthand": ((5, 270, -40), (0, 2, 2), (1.7, 1.7, 0.85)),
    "firstperson_righthand": ((-20, 90, -35), (3.13, 2.0, 0.13), (1.36, 1.36, 0.68)),
}
SPEAR_GRIP = (3.0, -3.0)


def items():
    """Every arm, in registration order: each kind in bronze, then in steel; then the Arms VI kit (kit())."""
    return [f"{metal}_{kind}" for metal in METALS for kind in KINDS] + kit()


def kit():
    """The Arms VI kit (JugcraftArms.KIT), in registration order: each metal's bows and crossbows, then its shields."""
    return [f"{metal}_{kind}" for metal in METALS for table in (RANGED, SHIELDS) for kind, at in table if at == metal]


def split(item):
    metal, kind = item.split("_", 1)
    return metal, kind


def info(kind):
    """A kind's entry: in KINDS, RANGED_KINDS or SHIELD_KINDS."""
    return KINDS.get(kind) or RANGED_KINDS.get(kind) or SHIELD_KINDS[kind]


def display(item):
    metal, kind = split(item)
    return f"{gear.GEAR_TIERS[metal]['display']} {info(kind)['display']}"


def feature(item):
    return gear.GEAR_TIERS[split(item)[0]]["feature"]


def metal_content(item):
    """Nuggets of its metal an arm is made of (9 an ingot), for the recipe audit."""
    metal, kind = split(item)
    return {metal: 9 * "".join(info(kind)["pattern"]).count("#")}


def textures():
    """Every arm's textures (tools/arms_art.py): its inventory icon and its 3D model's texture; a bow's or crossbow's
    drawn and loaded sprites, and a shield's face, back and trim (tools/arms_kit_art.py)."""
    names = []
    for item in items():
        kind = split(item)[1]
        if kind in RANGED_KINDS:
            names += [f"{item}{suffix}" for suffix in RANGED_SPRITES[RANGED_KINDS[kind]["type"]]]
        elif kind in SHIELD_KINDS:
            names += [f"{item}{suffix}" for suffix in SHIELD_SPRITES]
        else:
            names += [item, f"{item}_model"]
    return names


def item_tags():
    """Vanilla item tag -> arms, merged into tools/gear.py's tag files (they share swords)."""
    tags = {}
    for item in items():
        for tag in info(split(item)[1])["tags"]:
            tags.setdefault(tag, []).append(f"{MOD}:{item}")
    return tags


def _rotation(degrees):
    """The matrix of a display rotation: about x, then y, then z, as Minecraft's rotationXYZ."""
    x, y, z = (math.radians(d) for d in degrees)
    rx = ((1, 0, 0), (0, math.cos(x), -math.sin(x)), (0, math.sin(x), math.cos(x)))
    ry = ((math.cos(y), 0, math.sin(y)), (0, 1, 0), (-math.sin(y), 0, math.cos(y)))
    rz = ((math.cos(z), -math.sin(z), 0), (math.sin(z), math.cos(z), 0), (0, 0, 1))

    def mul(a, b):
        return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)) for i in range(3))
    return mul(mul(rx, ry), rz)


def _pose(rotation, translation, scale, grip, base_scale, base_grip, factor):
    """A hand pose `factor` times the base one's size, moved so that `grip` (where this sprite is held) lands where the
    base pose holds `base_grip`. Minecraft scales a model, then rotates it, then moves it; so the grip's offset from
    the base one, scaled and rotated, is taken off the translation."""
    scales = base_scale if isinstance(base_scale, tuple) else (base_scale,) * 3
    new = tuple(round(s * factor, 3) for s in scales)
    delta = (scales[0] * base_grip[0] - new[0] * grip[0], scales[1] * base_grip[1] - new[1] * grip[1], 0.0)
    r = _rotation(rotation)
    shift = tuple(sum(r[i][k] * delta[k] for k in range(3)) for i in range(3))
    moved = tuple(round(t + d, 2) + 0.0 for t, d in zip(translation, shift))
    left_moved = (round(translation[0] - shift[0], 2) + 0.0, moved[1], moved[2])
    return ({"rotation": list(rotation), "translation": list(moved), "scale": list(new)},
            {"rotation": [rotation[0], -rotation[1], -rotation[2]], "translation": list(left_moved), "scale": list(new)})


def _grip(kind, mirrored=False):
    """Where a kind's icon is held, in pixels from the centre of a 16-pixel model (x right, y up), and its hand factor
    (tools/arms_art.py layout: how much larger than `held` it is held, for an icon a wide head makes smaller)."""
    (x, y), size, factor = arms_art.held_at(kind, KINDS[kind]["held"], mirrored=mirrored)
    return (x * 16.0 / size - 8.0, 8.0 - y * 16.0 / size), factor


def held_model(kind):
    """The 3D model a kind's arms are held as (tools/arms_art.py model: the same in both metals, textured #tex), posed
    as vanilla's sword (or, for the spear and lance, the spear) is held, larger by the kind's `held` and moved so the hand
    stays on the grip. It lies over the kind's icon exactly, so these poses hold it as they would the icon."""
    charging = kind in CHARGING
    base, base_grip = (SPEAR_IN_HAND, SPEAR_GRIP) if charging else (HANDHELD, SWORD_GRIP)
    grip, factor = _grip(kind, mirrored=charging)
    display = {}
    for context, (rotation, translation, scale) in base.items():
        right, left = _pose(rotation, translation, scale, grip, scale, base_grip, round(KINDS[kind]["held"] * factor, 4))
        display[context] = right
        display[context.replace("righthand", "lefthand")] = left
    _texture, elements = arms_art.model(kind, "bronze", KINDS[kind]["held"], mirrored=charging)
    return {"textures": {"particle": "#tex"}, "elements": elements, "display": display}


def write_all(write, assets, data, lang, condition):
    """Shared in-hand models, each arm's model and definition, names and tooltips, recipes and repair tags."""
    models = assets / "models" / "item"
    for kind in KINDS:
        write(models / f"arms_{kind}.json", held_model(kind))
        lang[f"tooltip.{MOD}.arms.{kind}"] = KINDS[kind]["tooltip"]
    for kind, name in TRAIT_NAMES.items():
        lang[f"tooltip.{MOD}.arms.{kind}.trait"] = name
    lang[f"tooltip.{MOD}.arms.two_handed.trait"] = "Two-Handed"
    lang[f"tooltip.{MOD}.arms.two_handed"] = "The blow lands as the swing comes round, on every foe in its arc."
    lang[f"tooltip.{MOD}.hold_shift"] = "Hold %s for details"
    lang[f"message.{MOD}.two_handed.off_hand"] = "Two hands for this one: put away what is in your off hand."
    for kind, art in ARTS.items():
        seconds = art["cooldown"] / 20
        lang[f"tooltip.{MOD}.arms.art.{art['move']}.trait"] = f"Weapon Art: {art['name']}"
        lang[f"tooltip.{MOD}.arms.art.{art['move']}"] = f"Use: {art['text']} Ready again after {seconds:g} s."
    for kind in THROWN_KINDS:
        lang[f"tooltip.{MOD}.arms.throw.{kind}"] = ("Hold use, then let go to throw it: %s damage. "
                                                     + THROW_TEXT[kind].format(disable=FRANCISCA_DISABLE))
    lang[f"message.{MOD}.arms.art.ground"] = "Your feet must be on the ground to leap."
    lang[f"message.{MOD}.arms.art.riding"] = "Not from the saddle."
    write(data / "tags" / "block" / "kama_cuts.json", {"values": KAMA_CUTS})
    for item in items():
        lang[f"item.{MOD}.{item}"] = display(item)
        _recipe(write, data, condition, item)
    import arms_kit
    arms_kit.write_all(write, assets, lang)
    write_heads(write, models)
    for item in items():
        metal, kind = split(item)
        if kind not in KINDS:
            continue
        info = KINDS[kind]
        # The icon in inventories, frames, on the ground and on shelves; in the hand (and on a head), the 3D model.
        write(models / f"{item}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(models / f"{item}_in_hand.json", {"parent": f"{MOD}:item/arms_{kind}",
                                                "textures": {"tex": f"{MOD}:item/{item}_model"}})
        model = {"type": "minecraft:select", "property": "minecraft:display_context",
                 "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"],
                            "model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}}],
                 "fallback": {"type": "minecraft:model", "model": f"{MOD}:item/{item}_in_hand"}}
        if kind in arms_heads.HEADS:
            # The flail's swinging head: its link and ball models, picked by FlailHeads' render copies of the stack.
            for part in ("link", "ball"):
                write(models / f"{item}_{part}.json", {"parent": f"{MOD}:item/arms_{kind}_{part}",
                                                       "textures": {"tex": f"{MOD}:item/{item}_model"}})
            model = arms_heads.definition(item, model)
        swap = 1.95 * info["held"] if kind in CHARGING else info["held"]
        # A longer arm comes up into the hand faster, as vanilla's spear does, so it never hangs half-raised.
        write(assets / "items" / f"{item}.json", {"model": model, "swap_animation_scale": round(swap, 2)})
    write(assets / "arms_heads.json", heads_table())


def write_heads(write, models):
    """The shared head models of the kinds whose head swings free (tools/arms_heads.py), sized by each kind's layout."""
    for kind, head in arms_heads.HEADS.items():
        _grip_model, unit, _grip, _eye = arms_art.head_layout(kind, KINDS[kind]["held"])
        for part, model in arms_heads.models(head, unit).items():
            write(models / f"arms_{kind}_{part}.json", model)


def heads_table():
    """arms_heads.json as tools/arms.py writes it (for tools/check_mod_data.py): the arms' flails and the Arms VII
    variants' (tools/arms_variants.py) whose head swings free."""
    import arms_variants   # (here: it imports this module)
    import arms_variants_art
    table = {}
    for item in items():
        metal, kind = split(item)
        if kind in arms_heads.HEADS:
            table[f"{MOD}:{item}"] = arms_heads.entry(arms_heads.HEADS[kind], arms_art.head_layout(kind, KINDS[kind]["held"]),
                                                      held_model(kind)["display"])
    for name, head in arms_heads.VARIANT_HEADS.items():
        held = KINDS[arms_variants.kind(name)]["held"]
        table[f"{MOD}:{name}"] = arms_heads.entry(head, arms_variants_art.head_layout(name, held),
                                                  arms_variants.held_model(name)["display"])
    return table


def _recipe(write, data, condition, item):
    """An arm's shaped recipe: its kind's pattern, "#" its metal's ingot and the other keys from KEYS."""
    metal, kind = split(item)
    pattern = info(kind)["pattern"]
    key = {"#": gear.GEAR_TIERS[metal]["ingot"]}
    for row in pattern:
        for ch in row:
            if ch in KEYS:
                key[ch] = KEYS[ch]
    write(data / "recipe" / f"{item}.json", {
        "fabric:load_conditions": condition(feature(item)), "type": "minecraft:crafting_shaped",
        "category": "equipment", "pattern": pattern, "key": key, "result": {"id": f"{MOD}:{item}", "count": 1}})


def draw_all(save):
    """Each arm's icon and 3D model texture (a strip of frames for a flickering one), and the kit's sprites
    (tools/arms_kit_art.py)."""
    for item in items():
        metal, kind = split(item)
        if kind not in KINDS:
            continue
        held, mirrored = KINDS[kind]["held"], kind in CHARGING
        frames, ticks = arms_art.ANIMATED.get(kind, (1, 0))
        # The inventory icon: the kind's 16x16 map (tools/arms_icons.py), or its drawing if it has none.
        icons = [arms_icons.icon(kind, metal, frame) if arms_icons.has(kind) else arms_art.draw(kind, metal, held, frame)
                 for frame in range(frames)]
        textures = [arms_art.model(kind, metal, held, frame, mirrored=mirrored)[0] for frame in range(frames)]
        for name, images in ((item, icons), (f"{item}_model", textures)):
            if frames == 1:
                save(images[0], "item", name)
                continue
            size = images[0].width
            strip = Image.new("RGBA", (size, size * frames), (0, 0, 0, 0))
            for frame, img in enumerate(images):
                strip.paste(img, (0, size * frame))
            save(strip, "item", name, animation={"frametime": ticks})
    import arms_kit_art
    arms_kit_art.draw_all(save)
