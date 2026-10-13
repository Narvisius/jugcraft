"""Paints the atlas of a 3D armor set (tools/armor_models.py): every face of every part in its own texel region, one
texel per model pixel, so the look stays chunky.

Every colour comes from the set's palette, by name: a painter only ever says "light", "seam" or "leather_dark", so
the bronze variant of a steel set is the same geometry and paint with BRONZE in place of STEEL (plus any accent
changes). The names, in ramps from light to dark (shade() steps along a ramp):
    metal      light, mid_light, mid, dark, seam, void
    leather    leather_light, leather_mid_light, leather_mid, leather_dark, leather_darkest
    under      under_light, under_mid, under_dark, under_darkest     (the dark padded layer under the plates)
    gold       gold_light, gold_dark                                  (trim: brass in BRONZE)

A palette may add tones of its own beyond these (a set's accents: feathers, a plume, a third gold), each named for what
it colours ("feather_pink"). They are used by name like any other; shade() leaves them as they are, so they suit flat
paint (solid, marks) rather than the patterned painters.

A part's paint is one spec for the whole box, or a dict of specs by face ("front", "sides", "ends", "*" for the rest;
vanilla names too). A spec is a painter name, P(name, **options), or a list of them painted in order (a plate, then
rivets on it). With a whole-box spec, faces one texel thin (the edges of a plate) get a plain edge instead of a
squeezed pattern.

Painters (options in brackets):
    plate     a hammered plate: the fill broken into runs of a tone lighter or darker, staggered like brickwork, with
              a lighter top row and darker bottom row [tone, strips "h"/"v", bevel, border]
    chevron   nested L's (or V's) one texel wide about one corner, a light border and a mottled 2x2 core at the far
              corner; with corner "o", concentric rings from the edge in, about a mottled core at the middle
              [corner "bl"/"br"/"tl"/"tr"/"in"/"out"/"v"/"^"/"o", bands, border, core, outside]
    lames     horizontal plates `rows` texels tall, each a lighter top row over darker ones, one tone darker at the
              centre line [rows, tones, centre "middle"/"in"/None]
    leather   leather fill with a light top row [tone, rim]
    strap     a leather strap with holes and an optional buckle [tone, rim, holes, buckle]
    gold      gold trim [tone, rim]
    under     the dark under-layer in 2x2 mottles with a light top edge
    rivets    a row of 1-texel rivets or 2x2 bolts lit at the top left [row, every, offset, style, tone]
    hammer    a layer over another: long runs of one tone broken by 2-3 texels a tone lighter or darker, staggered
              like brickwork, so nested L's read as the owner's hammered strips; the outer ring stays whole
              [keep_border, every]
    marks     rectangles of one tone (eye slits, breaths, holes; "clear" cuts holes in a cutout part)
              [rects (x, y, w, h[, tone]) with negative x/y from the far edge, tone, symmetric]
    solid     one tone [tone];  edge   a plain edge [tone]
    checker   alternating tones in squares `size` texels across, as mail [tones, size]
    test      the smoke test's face colours, a marker at the texture's top-left and the face's letter

Texture space on each face: row 0 is the top. On the four sides the texture's top is the visual top and texture-left is
the viewer's left looking at that face from outside (the front's left is the model's right). On the top face the
texture's top is the back; on the bottom face, the back too. A painter knows the face's `inward` side (+1 when
texture-right points toward the body's centre line, -1 when texture-left does, 0 when neither), so a pattern can point
inward on both mirrored halves.
"""
import random
import zlib
from dataclasses import dataclass, replace

import numpy as np
from PIL import Image

import armor_models as am

METAL = ("light", "mid_light", "mid", "dark", "seam", "void")
LEATHER = ("leather_light", "leather_mid_light", "leather_mid", "leather_dark", "leather_darkest")
UNDER = ("under_light", "under_mid", "under_dark", "under_darkest")
GOLD = ("gold_light", "gold_dark")
RAMPS = (METAL, LEATHER, UNDER, GOLD)

# The owner's steel knight design, sampled exactly (its render is unlit, so these are its texture colours).
STEEL = {
    "light": (180, 190, 192), "mid_light": (152, 161, 166), "mid": (126, 135, 144), "dark": (103, 112, 121),
    "seam": (81, 87, 99), "void": (65, 68, 75),
    "gold_light": (166, 131, 72), "gold_dark": (147, 114, 62),
    "leather_light": (90, 73, 66), "leather_mid_light": (77, 64, 59), "leather_mid": (71, 60, 58),
    "leather_dark": (55, 49, 49), "leather_darkest": (41, 37, 36),
    "under_light": (75, 78, 87), "under_mid": (62, 63, 68), "under_dark": (49, 52, 59), "under_darkest": (38, 39, 43),
}
# Bronze, the knight's steam-age make: a warm copper-bronze ramp with the steel's six steps, so the design's light top
# and dark skirt survive, but hue-shifted as metal is (golden highlights, coppery red shadows) and a little more
# contrast at the ends than the steel, or the hammered strips read as wood grain. It sits between tools/arms_pixel.py's
# BRONZE (mid near its mid, the darker steps between its own) and the owner's approved bronze ramp of the material
# sets (3e2410 7e5222 b4803c dcaa5c f6d696: the light step falls between its light and highlight).
# "gold" is brass, lighter than the bronze's light and yellower (near arms_pixel's BRASS), so the trim reads on it.
# The leather is darker and redder than the steel's; the under-layer is the steel's slate, which sets off both.
BRONZE = {**STEEL,
          "light": (225, 182, 107), "mid_light": (202, 139, 72), "mid": (174, 109, 55), "dark": (148, 83, 43),
          "seam": (121, 62, 32), "void": (89, 45, 26),
          "gold_light": (236, 204, 96), "gold_dark": (196, 160, 60),
          "leather_light": (82, 56, 42), "leather_mid_light": (70, 48, 36), "leather_mid": (62, 42, 32),
          "leather_dark": (46, 31, 25), "leather_darkest": (32, 22, 18)}
# Bloodthorn, the owner's crimson design (tools/bloodthorn_armor.py), sampled from their render. That render is lit as
# Blockbench lights a model (front and back faces 0.8, sides 0.6, tops 1.0: its side faces measure 0.75 of the fronts,
# and a lit top edge 1.22), so each tone is the median of its texel interiors on front and back faces divided by 0.8.
# Its metal runs orange, coral, red, crimson, magenta, plum and dark plum (bright at the top of the armor, dark at the
# feet); the ramp keeps six of them and folds the red, the rarest, into coral and crimson. The near-black purple
# under-layer is the design's own, with a darker step added for the eye slits. The design has no leather or gold:
# "gold" names its red, for trim (the owner's red between coral and crimson), and the leathers are a dark wine ramp
# for straps, unused by the armor itself.
BLOODTHORN = {
    "light": (239, 92, 70), "mid_light": (232, 71, 72), "mid": (178, 34, 76), "dark": (150, 34, 89),
    "seam": (109, 28, 78), "void": (80, 21, 64),
    "gold_light": (212, 49, 71), "gold_dark": (176, 38, 62),
    "leather_light": (110, 42, 58), "leather_mid_light": (94, 35, 52), "leather_mid": (80, 29, 46),
    "leather_dark": (62, 23, 38), "leather_darkest": (44, 17, 29),
    "under_light": (66, 55, 81), "under_mid": (44, 42, 55), "under_dark": (32, 31, 39), "under_darkest": (23, 21, 29),
}
# Reforged White Diamond, the owner's icy design (tools/white_diamond_armor.py), sampled from their render. That render
# is unlit: each tone shows at one value on faces of every direction, and no darker copy of a light tone occurs (none of
# its texel interiors is 0.5 to 0.9 of one, as a lit side, bottom or front face would be), so these are its texture
# colours as sampled, as the steel knight's are. Its metal is a hue-shifted ramp of six tones, one per step: icy white,
# pale cyan, light cyan, light blue, blue and lavender. The under-layer is its charcoal, with the grey that lights the
# arms. The design has no leather or gold: "gold" names its blue and lavender, for trim, and the leathers are a slate
# ramp between the charcoal and the lavender for straps, unused by the armor itself.
WHITE_DIAMOND = {
    "light": (243, 255, 253), "mid_light": (204, 240, 240), "mid": (165, 218, 226), "dark": (140, 184, 213),
    "seam": (120, 146, 197), "void": (113, 113, 177),
    "gold_light": (120, 146, 197), "gold_dark": (113, 113, 177),
    "leather_light": (98, 104, 134), "leather_mid_light": (86, 90, 118), "leather_mid": (76, 79, 104),
    "leather_dark": (63, 64, 86), "leather_darkest": (50, 50, 68),
    "under_light": (82, 92, 102), "under_mid": (68, 74, 86), "under_dark": (58, 61, 70), "under_darkest": (48, 49, 55),
}
# Hades, the owner's underworld design (tools/hades_armor.py), sampled from their render. That render is unlit, as
# White Diamond's is: each tone shows at one value on faces of every direction (the light band round a horn is the same
# grey on its front and its sides, and the helm's front above the visor is darker than the faces beside it, where a lit
# render would light it). So these are its texture colours as sampled, the commonest value of each tone's texel
# interiors. Its metal is a cool ramp of six greys, slightly purple, from the light grey edges to the darkest slate:
# light grey, grey, pinkish grey, slate, dark slate and the darkest slate. "gold" names its blue slate, the accent of
# the chest V, the horns and the skirt's strips, over the grey of the same value; "leather" names its blood-red cloth
# (the owner's three reds, and two darker steps for the cloth's shaded side and back, which the render does not show);
# the under-layer is its near-black slate, from a blue black to the darkest of the visor's slits.
HADES = {
    "light": (147, 145, 148), "mid_light": (127, 127, 129), "mid": (111, 108, 115), "dark": (86, 85, 101),
    "seam": (74, 73, 87), "void": (61, 59, 72),
    "gold_light": (95, 97, 120), "gold_dark": (96, 95, 103),
    "leather_light": (140, 49, 58), "leather_mid_light": (111, 45, 55), "leather_mid": (95, 39, 52),
    "leather_dark": (74, 31, 43), "leather_darkest": (54, 23, 33),
    "under_light": (41, 44, 59), "under_mid": (37, 37, 49), "under_dark": (31, 28, 36), "under_darkest": (26, 24, 29),
}
# Sunset Gem, the owner's sunset design (tools/sunset_gem_armor.py), sampled from their render. That render is unlit, as
# White Diamond's is: each tone shows at one value on faces of every direction, and five of its eight warm tones keep a
# red of 255, which no lit side, bottom or front face could (Blockbench would draw them at 0.5 to 0.8 of it), so these
# are its texture colours as sampled. Its gems run as a sunset from the top of the armor to the feet in eight tones:
# cream, pale yellow, apricot, peach, coral and red make the metal's six steps; the two deepest, a crimson rose and a
# mauve that sit low on the skirt and boots, are "gold", for trim. The under-layer is its olive. The design has no
# leather: the leathers are a dark olive-brown ramp for straps, unused by the armor itself; their darkest is the olive's
# own outline for the icons (armor_icons.OWN "w"), where olive meets the edge.
SUNSET_GEM = {
    "light": (255, 255, 229), "mid_light": (255, 246, 153), "mid": (255, 211, 140), "dark": (255, 171, 116),
    "seam": (255, 120, 101), "void": (240, 79, 87),
    "gold_light": (197, 59, 93), "gold_dark": (145, 71, 108),
    "leather_light": (124, 98, 62), "leather_mid_light": (106, 83, 53), "leather_mid": (90, 70, 45),
    "leather_dark": (72, 56, 36), "leather_darkest": (52, 40, 25),
    "under_light": (206, 176, 104), "under_mid": (188, 157, 91), "under_dark": (166, 131, 74),
    "under_darkest": (139, 106, 67),
}
# Pharaoh, the owner's golden design (tools/pharaoh_armor.py), sampled from their render. That render is unlit, as White
# Diamond's is: each tone shows at one value on faces of every direction, and its ramps shift hue as they darken (the
# gold's blue rises, 26, 40, 44, 51, as its red and green fall), which no lit, darker face of one texel colour could do,
# so these are its texture colours as sampled, the commonest value of each tone's texel interiors. Its metal is the
# gold, four tones from a bright yellow gold to the dark gold-brown of the skirt's lowest band, with two darker browns
# added (unused by the armor itself) for the icons' outline. "gold" names its red gems, the bright and the dark red (its
# middle red, the chest gem's, is left out). "leather" names its teal enamel, five tones from a light green teal to the
# near-black of the wrist bands (the skirt's near-black blocks are a shade lighter in the render, folded into it);
# "under" names its tan, the linen of the face plate, sleeves, skirt band and greaves, four tones.
PHARAOH = {
    "light": (178, 167, 26), "mid_light": (156, 138, 40), "mid": (129, 110, 44), "dark": (99, 84, 51),
    "seam": (76, 63, 41), "void": (56, 46, 31),
    "gold_light": (180, 34, 21), "gold_dark": (112, 29, 31),
    "leather_light": (77, 131, 99), "leather_mid_light": (67, 102, 82), "leather_mid": (59, 80, 71),
    "leather_dark": (51, 60, 55), "leather_darkest": (34, 33, 38),
    "under_light": (206, 176, 104), "under_mid": (185, 154, 89), "under_dark": (166, 131, 73),
    "under_darkest": (147, 113, 65),
}
# The four designs the owner sent on 7 October 2026 ("Here is art of new ones that I made!"): a sheet of the four on a
# blank mannequin, lit as Blockbench lights a model (tops brightest, the fronts darker and the sides darker still), and a
# screenshot of each worn in game. The sheet's fronts are taken as about 0.8 of the texture colour, as Bloodthorn's
# were, and the game shots (lit warmer, under shaders) only to choose between near tones; the whites, which the sheet
# shows at full white, are its own.
# Dread Knight (tools/dread_knight_armor.py): cool steel greys from the light grey of the crown, the bands and the
# frames to the near-black of the plates; "gold" names the faint pink sheen the owner put on the lit greys (the crown's
# merlons and the nasal bar); the leathers are a black leather ramp for the belt and straps; the under-layer is the
# near-black of the waist and skirt, its darkest the eye slits.
DREAD_KNIGHT = {
    "light": (192, 196, 198), "mid_light": (158, 163, 165), "mid": (124, 130, 131), "dark": (93, 98, 99),
    "seam": (65, 68, 70), "void": (42, 43, 46),
    "gold_light": (214, 190, 201), "gold_dark": (168, 146, 158),
    "leather_light": (84, 78, 78), "leather_mid_light": (70, 65, 66), "leather_mid": (58, 54, 55),
    "leather_dark": (46, 43, 44), "leather_darkest": (31, 29, 31),
    "under_light": (60, 62, 66), "under_mid": (46, 47, 51), "under_dark": (34, 35, 39), "under_darkest": (19, 19, 24),
}
# Valkyrie (tools/valkyrie_armor.py): the metal is the white plate, from ivory through cream and beige to the blue-grey
# and mauve of its shading and muscle lines, and a deeper mauve for the icons' outline; "gold" the gold trim, with a
# third gold (gold_mid) for its shaded bands; "leather" the red cloth wrapped on the shoulders, from a lit coral to a
# wine; the under-layer the brown leather of the straps and the skirt. The feathers of the wings: white (the metal's
# light), then pink, lilac and violet, the owner's tints.
VALKYRIE = {
    "light": (255, 251, 238), "mid_light": (241, 230, 210), "mid": (221, 209, 192), "dark": (186, 189, 191),
    "seam": (181, 156, 165), "void": (126, 104, 117),
    "gold_light": (255, 217, 118), "gold_dark": (196, 147, 62), "gold_mid": (224, 186, 98),
    "leather_light": (210, 78, 58), "leather_mid_light": (178, 60, 46), "leather_mid": (148, 44, 36),
    "leather_dark": (118, 28, 36), "leather_darkest": (82, 15, 28),
    "under_light": (104, 58, 44), "under_mid": (84, 42, 38), "under_dark": (66, 32, 32), "under_darkest": (44, 25, 31),
    "feather_pink": (246, 210, 210), "feather_lilac": (214, 206, 240), "feather_violet": (176, 158, 204),
}
# Wayfarer (tools/wayfarer_armor.py): the metal is the cloak's blue, from the light teal of its rim and front edges
# through teal and blue to the navy of its folds and the darkest navy; "gold" the clasp's pale silver; the leathers the
# dark red-brown of the belt, the skirt and the boots; the under-layer the hood's shadowed inside and the cloak's lining.
# The boots' wings: white, ice blue and the pink of their tips, over a steel blue.
WAYFARER = {
    "light": (126, 204, 214), "mid_light": (68, 148, 168), "mid": (34, 98, 144), "dark": (26, 72, 112),
    "seam": (22, 49, 76), "void": (13, 30, 46),
    "gold_light": (234, 234, 216), "gold_dark": (170, 172, 178),
    "leather_light": (112, 62, 48), "leather_mid_light": (96, 50, 42), "leather_mid": (80, 38, 36),
    "leather_dark": (62, 28, 28), "leather_darkest": (41, 13, 18),
    "under_light": (38, 60, 82), "under_mid": (26, 43, 61), "under_dark": (17, 31, 45), "under_darkest": (10, 20, 30),
    "feather_white": (255, 250, 238), "feather_ice": (204, 232, 244), "feather_pink": (242, 206, 208),
    "feather_steel": (106, 154, 176),
}
# Spartan (tools/spartan_armor.py): the metal is the gold, from a pale lit gold through the warm golds of the muscle
# plate to the bronze browns of its lines and edges; "gold" names the plume's oranges, and "plume_yellow" its lit
# tips; "leather" the reds of the plume and the cape, from a red orange to the cape's wine; the under-layer the
# brown leather of the skirt, the belt and the sandals.
SPARTAN = {
    "light": (249, 223, 142), "mid_light": (232, 201, 117), "mid": (206, 168, 88), "dark": (175, 134, 70),
    "seam": (146, 103, 62), "void": (112, 74, 42),
    "gold_light": (238, 124, 30), "gold_dark": (206, 72, 10), "plume_yellow": (250, 178, 52),
    "leather_light": (192, 46, 10), "leather_mid_light": (166, 24, 8), "leather_mid": (138, 36, 30),
    "leather_dark": (112, 23, 14), "leather_darkest": (80, 15, 27),
    "under_light": (100, 54, 41), "under_mid": (80, 39, 36), "under_dark": (61, 28, 28), "under_darkest": (40, 15, 18),
}
# Three more of the owner's designs, sent on 8 October 2026 ("I made these 3"): Blockbench renders, lit (the tops
# brightest), the horned set on a light ground and the two knights on black; each tone is the render's front-face value
# lifted toward its lit top, and the knights' darkest steps kept as drawn.
# Berserker (tools/berserker_armor.py): the metal is the pale stone-grey plate with its grey L-marks; "gold" names the
# white of its trim stripes; "leather" the reds of the crest, pauldrons and stripes, from a bright red to the dark red of
# their shade; the under-layer the dark grey mail and boots.
BERSERKER = {
    "light": (232, 232, 230), "mid_light": (207, 206, 204), "mid": (180, 178, 176), "dark": (150, 147, 146),
    "seam": (119, 115, 115), "void": (86, 83, 85),
    "gold_light": (247, 247, 245), "gold_dark": (224, 223, 221),
    "leather_light": (226, 46, 20), "leather_mid_light": (200, 33, 11), "leather_mid": (168, 27, 10),
    "leather_dark": (132, 21, 9), "leather_darkest": (90, 15, 4),
    "under_light": (122, 121, 121), "under_mid": (94, 93, 95), "under_dark": (70, 69, 73), "under_darkest": (45, 45, 49),
}
# Paladin (tools/crusader_armor.py): the metal is the white and silver plate, from white through cool silvers to the
# blue-black of its bars; "gold" names the blue of the gems on the tassets; "leather" the purple of the underskirt and the
# plume; the under-layer the near-black mail, its checks the under-layer's two darker steps.
PALADIN = {
    "light": (252, 254, 255), "mid_light": (223, 227, 237), "mid": (185, 190, 205), "dark": (140, 146, 162),
    "seam": (96, 102, 120), "void": (54, 58, 74),
    "gold_light": (98, 114, 255), "gold_dark": (62, 74, 212),
    "leather_light": (146, 108, 224), "leather_mid_light": (118, 88, 190), "leather_mid": (96, 70, 154),
    "leather_dark": (70, 50, 120), "leather_darkest": (42, 30, 84),
    "under_light": (82, 88, 106), "under_mid": (58, 63, 80), "under_dark": (36, 40, 54), "under_darkest": (16, 20, 30),
}
# Templar (tools/crusader_armor.py): the metal is the dark slate plate, from the pale grey of its brow, lines and plume
# to the blue-black of its bars; "gold" names the near-white of the cross's middle and the plume's lit edge; "leather"
# the dark red of the underskirt and the cloth at the collar; the under-layer the blackened mail.
TEMPLAR = {
    "light": (200, 212, 218), "mid_light": (158, 170, 178), "mid": (122, 131, 144), "dark": (88, 96, 108),
    "seam": (62, 68, 80), "void": (38, 41, 54),
    "gold_light": (244, 248, 252), "gold_dark": (212, 221, 227),
    "leather_light": (152, 38, 44), "leather_mid_light": (122, 27, 33), "leather_mid": (97, 20, 26),
    "leather_dark": (72, 14, 20), "leather_darkest": (48, 10, 15),
    "under_light": (66, 72, 86), "under_mid": (48, 52, 64), "under_dark": (32, 35, 46), "under_darkest": (18, 19, 27),
}
# Two more the owner sent later on 8 October ("Just made these ones aswell want them done weapons too please"), renders
# lit from the front, so their lit faces were taken as the texture's colours and their shadowed ones set a ramp's dark end.
# Sentinel (tools/sentinel_armor.py): the metal is the gold, from the cream of its lit edges through warm golds to the
# brown of its shadows; "gold" names a still paler cream and a deep brown for the meander behind the helm; "leather" the
# browns of the helm's engraving and the plates' grooves; the under-layer the near-black cloth of the coat and robe.
SENTINEL = {
    "light": (250, 240, 170), "mid_light": (214, 182, 114), "mid": (182, 138, 88), "dark": (136, 96, 58),
    "seam": (96, 66, 46), "void": (58, 40, 32),
    "gold_light": (255, 250, 206), "gold_dark": (84, 58, 44),
    "leather_light": (150, 110, 72), "leather_mid_light": (120, 86, 58), "leather_mid": (96, 68, 48),
    "leather_dark": (72, 50, 38), "leather_darkest": (44, 30, 24),
    "under_light": (66, 66, 72), "under_mid": (44, 44, 50), "under_dark": (28, 28, 33), "under_darkest": (14, 14, 18),
}
# Frost Knight (tools/frost_knight_armor.py): the metal is the frosted white plate, from white through the lilac greys of
# its mottling to a cold slate; "gold" names the ice's two cyans, the palest and the bright; "leather" the navy of the
# strap, the belt and the left pauldron; the under-layer the near-black of the visor. The crown's ice is five tones of its
# own, by name.
FROST_KNIGHT = {
    "light": (250, 252, 255), "mid_light": (228, 230, 240), "mid": (204, 202, 220), "dark": (172, 172, 194),
    "seam": (132, 134, 160), "void": (84, 88, 116),
    "gold_light": (196, 246, 255), "gold_dark": (92, 206, 246),
    "leather_light": (84, 92, 138), "leather_mid_light": (60, 66, 106), "leather_mid": (44, 50, 84),
    "leather_dark": (30, 34, 62), "leather_darkest": (16, 18, 38),
    "under_light": (62, 66, 86), "under_mid": (40, 42, 60), "under_dark": (22, 24, 38), "under_darkest": (8, 10, 20),
    "ice_light": (200, 248, 255), "ice": (120, 224, 252), "ice_mid": (64, 184, 240), "ice_dark": (32, 136, 214),
    "ice_deep": (22, 92, 172),
}
# Wight King (tools/wight_king_armor.py): the slate-blue plate of the owner's knight crowned with icicles and antlers,
# from pale ice to deep slate; "gold" names the cyan of its gems, the palest and the deep; "leather" the dark slate of
# its straps, belt and the gaps between its bands; the under-layer the black of its face. Its icicles and antlers are
# five tones of their own, by name, paling to their tips.
WIGHT_KING = {
    "light": (178, 206, 218), "mid_light": (133, 163, 178), "mid": (95, 121, 138), "dark": (73, 98, 115),
    "seam": (53, 78, 94), "void": (37, 53, 70),
    "gold_light": (176, 255, 244), "gold_dark": (40, 178, 182),
    "leather_light": (50, 71, 89), "leather_mid_light": (37, 53, 70), "leather_mid": (27, 47, 63),
    "leather_dark": (18, 34, 47), "leather_darkest": (10, 22, 32),
    "under_light": (24, 38, 50), "under_mid": (14, 28, 39), "under_dark": (8, 18, 27), "under_darkest": (4, 10, 16),
    "ice_light": (232, 244, 250), "ice": (196, 220, 232), "ice_mid": (160, 190, 205), "ice_dark": (120, 150, 168),
    "ice_deep": (84, 112, 130),
}
# Reaper (tools/reaper_armor.py): the metal is the bone-white of its plates, white to the grey of its shoulder plates and
# wraps; "leather" the dark brown of its robe; the under-layer the black inside its hood; "gold" the reddish brown of its
# pouch. Its hood's greyer browns (and its strap's) are four tones of their own, by name.
REAPER = {
    "light": (214, 216, 202), "mid_light": (182, 184, 171), "mid": (138, 139, 132), "dark": (104, 105, 102),
    "seam": (75, 76, 78), "void": (49, 48, 53),
    "gold_light": (74, 48, 36), "gold_dark": (41, 22, 17),
    "leather_light": (73, 64, 56), "leather_mid_light": (63, 55, 48), "leather_mid": (53, 44, 37),
    "leather_dark": (40, 35, 32), "leather_darkest": (24, 18, 17),
    "under_light": (39, 34, 30), "under_mid": (29, 25, 22), "under_dark": (18, 15, 15), "under_darkest": (8, 7, 8),
    "hood_light": (82, 78, 72), "hood": (66, 62, 57), "hood_mid": (52, 48, 44), "hood_dark": (39, 35, 32),
}
# Banana (tools/banana_armor.py): the owner's banana costume: the metal is its yellow skin, from the lit ridges to the
# shade; "leather" the brown of its stem and its blackened tip; "gold" its brown speckles; the under-layer the darker
# yellow inside the hole for the face.
BANANA = {
    "light": (246, 218, 96), "mid_light": (232, 196, 70), "mid": (214, 172, 52), "dark": (184, 140, 38),
    "seam": (146, 110, 26), "void": (104, 74, 22),
    "gold_light": (150, 100, 40), "gold_dark": (124, 81, 30),
    "leather_light": (101, 70, 21), "leather_mid_light": (85, 57, 14), "leather_mid": (70, 42, 10),
    "leather_dark": (56, 33, 8), "leather_darkest": (41, 20, 5),
    "under_light": (150, 114, 34), "under_mid": (126, 94, 26), "under_dark": (100, 74, 20), "under_darkest": (64, 46, 12),
}
# Scarab (tools/scarab_armor.py): the owner's gold-and-lapis Egyptian set: the metal is its gold, from the lit yellow to
# the brown of its shade; "gold" its brightest and its deepest; "leather" the lapis, pale to navy; the under-layer the
# shadow under the headcloth.
SCARAB = {
    "light": (226, 178, 48), "mid_light": (196, 151, 37), "mid": (168, 115, 29), "dark": (139, 72, 21),
    "seam": (100, 50, 16), "void": (58, 30, 12),
    "gold_light": (240, 204, 84), "gold_dark": (112, 56, 18),
    "leather_light": (66, 116, 176), "leather_mid_light": (51, 97, 156), "leather_mid": (42, 76, 128),
    "leather_dark": (32, 52, 96), "leather_darkest": (18, 28, 58),
    "under_light": (82, 48, 20), "under_mid": (60, 34, 14), "under_dark": (42, 22, 10), "under_darkest": (24, 13, 9),
}
# The smoke test's face colours (tools/armor_smoke.py): one hue per face, so a render shows which face is where.
TEST = {**STEEL, "t_top": (230, 230, 90), "t_bottom": (90, 70, 40), "t_right": (220, 70, 70), "t_front": (80, 200, 90),
        "t_left": (70, 110, 230), "t_back": (200, 90, 210), "t_mark": (20, 20, 20), "t_rule": (255, 255, 255)}


def P(kind, **options):
    """A paint spec: painter `kind` with its options."""
    if kind not in PAINTERS:
        raise ValueError(f"unknown painter {kind!r}: use one of {sorted(PAINTERS)}")
    return (kind, options)


def shade(name, steps):
    """The tone `steps` darker (negative: lighter) along its ramp, held at the ends."""
    for ramp in RAMPS:
        if name in ramp:
            return ramp[max(0, min(len(ramp) - 1, ramp.index(name) + steps))]
    return name


@dataclass
class Face:
    """What a painter knows about the face it paints."""
    item: str
    bone: str
    part: object
    name: str           # top, bottom, right, front, left, back
    w: int              # texels
    h: int
    du: tuple           # bone-space direction of texture-right on the placed part
    dv: tuple           # bone-space direction of texture-down
    inward: int         # +1: texture-right points toward the body's centre line; -1: texture-left; 0: neither
    seed: int

    def rng(self, salt=""):
        return random.Random(self.seed ^ zlib.crc32(salt.encode()))


# ---------------------------------------------------------------- painters: each fills g (h x w tone names) in place
def _runs(rng, n, base, alt, run=(3, 6), alt_run=(2, 3)):
    """A row of n tones: runs of `base` 3-6 long broken by runs of `alt` 2-3 long, from a random phase."""
    out, use_alt, first = [], rng.random() < 0.4, True
    while len(out) < n:
        lo, hi = alt_run if use_alt else run
        length = rng.randint(lo, hi)
        if first:   # start part-way through a run, so rows stagger like brickwork
            length, first = rng.randint(1, length), False
        out.extend([alt if use_alt else base] * length)
        use_alt = not use_alt
    return out[:n]


def plate(g, f, tone="mid_light", strips="h", bevel=True, border=None):
    h, w = g.shape
    rng = f.rng("plate")
    g[:, :] = tone
    lines = range(h) if strips == "h" else range(w)
    for i in lines:
        alt = shade(tone, -1 if (i + rng.randint(0, 1)) % 2 else 1)
        row = _runs(rng, w if strips == "h" else h, tone, alt)
        if strips == "h":
            g[i, :] = row
        else:
            g[:, i] = row
    if bevel and h >= 3:
        g[0, :] = _runs(rng, w, shade(tone, -1), tone, run=(4, 8), alt_run=(1, 2))
        g[-1, :] = _runs(rng, w, shade(tone, 1), shade(tone, 2), run=(4, 8), alt_run=(2, 2))
    if border:
        g[0, :] = g[-1, :] = border
        g[:, 0] = g[:, -1] = border


def _corner(f, corner):
    if corner in ("in", "out"):
        side = f.inward if corner == "in" else -f.inward
        return "br" if side > 0 else "bl"
    return corner


def chevron(g, f, corner="in", bands=("light", "mid", "dark", "mid"), border="light", core=("light", "mid_light"),
            outside="mid_light", thick=1):
    h, w = g.shape
    rng = f.rng("chevron")
    corner = _corner(f, corner)
    g[:, :] = outside
    inset = 1 if border and h > 2 and w > 2 else 0
    ih, iw = h - 2 * inset, w - 2 * inset
    for y in range(ih):
        for x in range(iw):
            if corner in ("v", "^"):
                up = ih - 1 - y if corner == "v" else y
                k = up - abs(2 * x - (iw - 1)) // 2   # an even width gets a two-texel point
            elif corner == "o":
                k = min(x, y, iw - 1 - x, ih - 1 - y)  # rings from the edge in
            else:
                dx = x if corner[1] == "l" else iw - 1 - x
                dy = ih - 1 - y if corner[0] == "b" else y
                k = min(dx, dy)
            band = k // thick if k >= 0 else -1
            if band < 0:
                tone = outside
            elif band < len(bands):
                tone = bands[band]
            else:
                block = ((x // 2) + (y // 2) + rng.randint(0, 1)) % 2
                tone = core[block % len(core)]
            g[y + inset, x + inset] = tone
    if inset:
        g[0, :] = g[-1, :] = border
        g[:, 0] = g[:, -1] = border


SKIRT = (("mid_light", "dark"), ("mid", "mid"), ("mid_light", "seam"), ("mid", "dark"))   # the owner's four lames


def lames(g, f, rows=2, tones=SKIRT, centre="middle", lighter_ends=False):
    h, w = g.shape
    rng = f.rng("lames")
    for y in range(h):
        top, low = tones[(y // rows) % len(tones)]
        tone = top if y % rows == 0 else low
        g[y, :] = _runs(rng, w, tone, shade(tone, -1 if y % rows else 1), run=(3, 6), alt_run=(2, 3))
    if centre == "middle" and w >= 6:
        cols = range(w // 2 - 2 + w % 2, w // 2 + 2)
    elif centre == "in" and f.inward:
        cols = range(w - 2, w) if f.inward > 0 else range(0, 2)
    else:
        cols = ()
    for x in cols:
        for y in range(h):
            g[y, x] = shade(g[y, x], 1)
    if lighter_ends and w >= 6:
        for x in (0, w - 1):
            for y in range(h):
                g[y, x] = shade(g[y, x], -1)


def leather(g, f, tone="leather_mid", rim="leather_light"):
    h, w = g.shape
    rng = f.rng("leather")
    for y in range(h):
        g[y, :] = _runs(rng, w, tone, "leather_mid_light", run=(3, 7), alt_run=(2, 2))
    if rim and h >= 2:
        g[0, :] = rim
    if h >= 3 and w >= 3:
        g[-1, -1 if f.inward > 0 else 0] = "leather_darkest"


def strap(g, f, tone="leather_dark", rim="leather_mid", holes=4, hole="leather_darkest", buckle=None):
    h, w = g.shape
    g[:, :] = tone
    if rim and h >= 2:
        g[0, :] = rim
    if holes:
        y = h // 2 if h >= 2 else 0
        for x in range(holes // 2, w, holes):
            g[y, x] = hole
    if buckle and w >= 3:
        x0 = w // 2 - 1 if buckle == "centre" else int(buckle)
        g[:, x0:x0 + 3] = "gold_light"
        if h >= 3:
            g[1:-1, x0 + 1] = "gold_dark"


def gold(g, f, tone="gold_dark", rim="gold_light"):
    g[:, :] = tone
    if rim and g.shape[0] >= 2:
        g[0, :] = rim


def under(g, f, light="under_light"):
    h, w = g.shape
    rng = f.rng("under")
    for y in range(h):
        for x in range(w):
            block = ((x // 2) * 7 + (y // 2) * 3 + rng.randint(0, 2)) % 3
            g[y, x] = ("under_dark", "under_mid", "under_dark")[block]
    for y in range(1, h, 3):
        x = rng.randrange(w)
        if g[y - 1, x] != "under_darkest":
            g[y, x] = "under_darkest"
    if light and h >= 2:
        g[0, :] = light


def rivets(g, f, row="middle", every=3, offset=1, style="rivet", tone="light"):
    h, w = g.shape
    y = {"top": 1, "middle": h // 2, "bottom": h - 2}.get(row, row) if isinstance(row, str) else row
    y = max(0, min(h - 1, y if y >= 0 else h + y))
    for x in range(offset, w, every):
        if style == "bolt":
            if x + 1 < w and y + 1 < h:
                g[y, x], g[y, x + 1] = tone, shade(tone, 1)
                g[y + 1, x], g[y + 1, x + 1] = shade(tone, 1), shade(tone, 3)
        else:
            g[y, x] = tone


def hammer(g, f, keep_border=True, every=5):
    h, w = g.shape
    rng = f.rng("hammer")
    src = g.copy()
    lo = 1 if keep_border and h > 2 and w > 2 else 0

    def runs(line):
        out, start = [], 0
        for i in range(1, len(line) + 1):
            if i == len(line) or line[i] != line[start]:
                out.append((start, i))
                start = i
        return out

    def broken(tone):
        up, down = shade(tone, -1), shade(tone, 1)
        if tone is None or (up == tone and down == tone):
            return tone
        return down if up == tone else up if down == tone else (up if rng.random() < 0.5 else down)

    # rows: a run of `every` or more gets one break, never at its ends, placed at random so rows stagger
    row_len = np.ones((h, w), dtype=int)
    for y in range(lo, h - lo):
        for s, e in runs(list(src[y, lo:w - lo])):
            row_len[y, lo + s:lo + e] = e - s
            n = e - s
            if n >= every and src[y, lo + s] is not None:
                k = 2 if n < 8 else rng.choice((2, 3))
                at = s + rng.randint(1, n - k - 1)
                g[y, lo + at:lo + at + k] = broken(src[y, lo + s])
    # columns: the same along one- and two-texel-wide upright bands (the upright arms of nested L's)
    for x in range(lo, w - lo):
        col = [src[y, x] if row_len[y, x] <= 2 else None for y in range(lo, h - lo)]
        for s, e in runs(col):
            n = e - s
            if n >= every and col[s] is not None:
                at = s + rng.randint(1, n - 3)
                g[lo + at:lo + at + 2, x] = broken(col[s])


def marks(g, f, rects=(), tone="void", symmetric=False):
    h, w = g.shape
    for rect in rects:
        x, y, rw, rh = rect[:4]
        t = rect[4] if len(rect) > 4 else tone
        x, y = x if x >= 0 else w + x, y if y >= 0 else h + y
        spans = [(x, x + rw)] + ([(w - x - rw, w - x)] if symmetric else [])
        for x0, x1 in spans:
            g[max(0, y):max(0, y + rh), max(0, x0):max(0, x1)] = None if t == "clear" else t


def solid(g, f, tone="mid_light"):
    g[:, :] = tone


def checker(g, f, tones=("under_dark", "under_mid"), size=1):
    h, w = g.shape
    for y in range(h):
        for x in range(w):
            g[y, x] = tones[(x // size + y // size) % len(tones)]


def edge(g, f, tone="mid_light"):
    g[:, :] = tone
    if g.shape[0] >= 2:
        g[0, :] = shade(tone, -1)


GLYPHS = {"top": ("###", ".#.", ".#.", ".#.", ".#."), "bottom": ("#.#", "#.#", "#.#", "#.#", "###"),
          "right": ("##.", "#.#", "##.", "#.#", "#.#"), "front": ("###", "#..", "##.", "#..", "#.."),
          "left": ("#..", "#..", "#..", "#..", "###"), "back": ("##.", "#.#", "##.", "#.#", "##.")}


def test(g, f):
    h, w = g.shape
    g[:, :] = f"t_{f.name}"
    m = 2 if h >= 4 and w >= 4 else 1
    g[:m, :m] = "t_mark"
    if w > m + 1:
        g[0, m + 1:min(w, m + 4)] = "t_rule"
    if w >= 5 and h >= 7:
        x0, y0 = (w - 3) // 2, (h - 5) // 2 + 1
        for dy, line in enumerate(GLYPHS[f.name]):
            for dx, c in enumerate(line):
                if c == "#":
                    g[y0 + dy, x0 + dx] = "t_mark"


PAINTERS = {"plate": plate, "chevron": chevron, "lames": lames, "leather": leather, "strap": strap, "gold": gold,
            "under": under, "rivets": rivets, "hammer": hammer, "marks": marks, "solid": solid, "edge": edge,
            "checker": checker, "test": test}
PATTERNED = {"plate", "chevron", "lames"}   # squeezed onto a one-texel edge they read as noise: an edge instead


def _layers(spec):
    if isinstance(spec, str):
        return [P(spec)]
    if isinstance(spec, tuple) and len(spec) == 2 and isinstance(spec[0], str) and isinstance(spec[1], dict):
        return [P(spec[0], **spec[1])]
    if isinstance(spec, list):
        return [layer for s in spec for layer in _layers(s)]
    raise ValueError(f"bad paint spec {spec!r}")


def face_layers(spec, face):
    """(layers, explicit) for one face: a dict spec is looked up by face name or alias, then "sides"/"ends", then
    "all"/"*"; explicit is False for a whole-box spec or the "*" fallback."""
    if not isinstance(spec, dict):
        return _layers(spec), False
    keys = {}
    for key, value in spec.items():
        for name in (am.GROUPS[key] if key in am.GROUPS else am.FACES if key == "*" else [am.face_name(key)]):
            rank = 0 if key not in am.GROUPS and key != "*" else 1 if key in ("sides", "ends") else 2
            if name not in keys or rank < keys[name][0]:
                keys[name] = (rank, value)
    if face not in keys:
        raise ValueError(f"paint spec {spec!r} has nothing for the {face} face (add '*')")
    rank, value = keys[face]
    return _layers(value), rank < 2


def _directions(part, density):
    """{face: (du, dv)} on the placed part, for every face (skipped ones too)."""
    out = {}
    for face, pts, uvs, _ in am.part_faces(replace(part, skip=frozenset()), density):
        du = dv = (0.0, 0.0, 0.0)
        for i in range(4):
            j = (i + 1) % 4
            (ui, vi), (uj, vj) = uvs[i], uvs[j]
            step = am._sub(pts[j], pts[i])
            if ui != uj and vi == vj:
                du = am._scale(step, 1 / (uj - ui))
            elif vi != vj and ui == uj:
                dv = am._scale(step, 1 / (vj - vi))
        out[face] = (du, dv)
    return out


def _inward(bone, part):
    if bone.startswith("right_"):
        return (1.0, 0.0, 0.0)
    if bone.startswith("left_"):
        return (-1.0, 0.0, 0.0)
    cx = am.place(part, part.centre)[0]
    return (-1.0 if cx > 0 else 1.0, 0.0, 0.0) if abs(cx) > 0.25 else (0.0, 0.0, 0.0)


def paint_face(spec, face):
    """The face's texels: an (h, w) array of tone names (None = see-through)."""
    g = np.full((face.h, face.w), "mid_light", dtype=object)
    layers, explicit = face_layers(spec, face.name)
    if not explicit and min(face.w, face.h) <= 1 and layers[0][0] in PATTERNED:
        layers = [P("edge", tone=layers[0][1].get("tone", "mid_light"))]
    for kind, options in layers:
        PAINTERS[kind](g, face, **options)
    return g


def sources(s):
    """{net: (item, bone, part)}: the part whose paint fills each net (the one it is named after, else the first)."""
    out = {}
    for item, bone, part in s.parts():
        current = out.get(part.key)
        if current is None or (part.name == part.key and current[2].name != part.key):
            out[part.key] = (item, bone, part)
    return out


def paint_grid(s):
    """(grid of tone names, mask of face texels) for the whole atlas (a set with an atlas of its own paints none)."""
    nets, (width, height) = am.layout(s)
    grid = np.full((height, width), None, dtype=object)
    mask = np.zeros((height, width), dtype=bool)
    for key, (item, bone, part) in sources(s).items():
        if part.uvs:
            continue
        u, v, w, h, d = nets[key]
        dirs = _directions(part, s.density)
        inward = _inward(bone, part)
        for name, (u0, v0, u1, v1) in am.face_rects(w, h, d).items():
            du, dv = dirs[name]
            side = am._dot(am._unit(du) if any(du) else du, inward)
            face = Face(item, bone, part, name, u1 - u0, v1 - v0, du, dv,
                        (1 if side > 0.5 else -1 if side < -0.5 else 0), zlib.crc32(f"{key}/{name}".encode()))
            grid[v + v0:v + v1, u + u0:u + u1] = paint_face(part.paint, face)
            mask[v + v0:v + v1, u + u0:u + u1] = True
    return grid, mask


def paint_atlas(s):
    """The set's atlas as an RGBA image. Texels round the faces copy their neighbour, so sampling at a face's very edge
    never picks up another part's colour. A set with an atlas of its own (a Blockbench project's) gives that."""
    if s.image is not None:
        return s.image.convert("RGBA")
    grid, mask = paint_grid(s)
    height, width = grid.shape
    rgba = np.zeros((height, width, 4), dtype=np.uint8)
    for y in range(height):
        for x in range(width):
            name = grid[y, x]
            if name is not None:
                if name not in s.palette:
                    raise ValueError(f"{s.name}: tone {name!r} is not in the palette")
                rgba[y, x] = (*s.palette[name], 255)
    fill = rgba.copy()
    for y in range(height):
        for x in range(width):
            if mask[y, x]:
                continue
            for dy, dx in ((0, -1), (0, 1), (-1, 0), (1, 0), (-1, -1), (-1, 1), (1, -1), (1, 1)):
                yy, xx = y + dy, x + dx
                if 0 <= yy < height and 0 <= xx < width and mask[yy, xx] and rgba[yy, xx, 3]:
                    fill[y, x] = rgba[yy, xx]
                    break
    return Image.fromarray(fill, "RGBA")


def atlas_problems(s, image):
    """Messages for an atlas image that does not fit the set: the wrong size, or a see-through texel on a drawn face of
    a part that is not cutout."""
    nets, size = am.layout(s)
    if image.size != size:
        return [f"{s.name}: atlas is {image.size}, the layout needs {size}"]
    alpha = np.asarray(image.convert("RGBA"))[:, :, 3]
    out = []
    for item, bone, part in s.parts():
        if part.cutout:
            continue
        if part.uvs:
            for name, corners in part.uvs:
                us, vs = [c[0] for c in corners], [c[1] for c in corners]
                x0, x1 = int(min(us) + 1e-6), int(-(-max(us) // 1))
                y0, y1 = int(min(vs) + 1e-6), int(-(-max(vs) // 1))
                if (alpha[y0:y1, x0:x1] < 255).any():
                    out.append(f"{s.name}: {part.name} {name} face has see-through texels")
            continue
        u, v, w, h, d = nets[part.key]
        for name, (u0, v0, u1, v1) in am.face_rects(w, h, d).items():
            if name not in part.skip and (alpha[v + v0:v + v1, u + u0:u + u1] < 255).any():
                out.append(f"{s.name}: {part.name} {name} face has see-through texels")
    return out


def save(s, textures):
    """Writes the set's atlas under the mod's textures folder."""
    path = textures / f"{s.texture}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    paint_atlas(s).save(path, optimize=True)
    return path


def draw_all(textures):
    """Paints every registered set (generate_textures.py)."""
    for s in am.sets():
        save(s, textures)
