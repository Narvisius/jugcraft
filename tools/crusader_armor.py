"""Paladin and Templar: the owner's two crusader knights (two of the three designs they sent on 8 October 2026, "I made
these 3": Blockbench renders of each worn, from the front and from a little to the side) as 3D worn models for
jugcraft:paladin_* and jugcraft:templar_* (helmet, chestplate, leggings, boots), on the toolkit in
tools/armor_models.py (shapes) and tools/armor_paint.py (the atlas). The two share one build below the neck, as the
owner's do, and differ in colour and in their helms.

What the owner drew, and where it is here (the owner asked for "complex models ... really capturing the crazy unique
geometry of each armor", so each shape is built, not painted):
    helmet      a great helm. The Paladin's white, its visor carrying a raised white H (two uprights and a bar at the
                eyes) over a dark breath, its sides dark mail, a white comb along its crown and a purple sprig like a
                cross behind it. The Templar's slate, barred with raised ribs, a pale gable guard over its brow
                overhanging the helm's sides, and a pale crest curling up from its crown. Here: the helm, the visor
                plate and on it the Paladin's uprights and bar or the Templar's three ribs; a row of rivets down each
                side; the Paladin's comb, stepped down behind, and the sprig's stem and two bars; the Templar's two
                brow bars meeting at the peak (the left 0.15 nearer, so they never share a plane) and its crest: a
                stalk leaning forward, a bend and a flag tipped back
    chestplate  a breastplate shaped like a heater shield carrying a cross, white on the Paladin, pale on slate on the
                Templar; dark mail at the Paladin's sides and arms, red cloth at the Templar's collar; big square
                pauldrons with a square spiral, the mail sleeve showing under them; a band at the elbow, banded
                forearms; hoops round the waist. Here: the
                cuirass; the shield in three steps narrowing to its foot, and the cross raised on it; a back plate
                with a spine ridge; two waist hoops, the lower wider; on each arm the pauldron with a raised dome on
                top and the spiral boss on its outer side, the mail sleeve under it, the elbow band with a fan plate
                on its outer side, the vambrace, a cuff flaring out from the wrist and the glove
    leggings    tassets over the front and sides of the thighs carrying the spiral (the Paladin's with a blue gem), a
                cloth underskirt to the knee between and under them, purple on the Paladin, dark red on the Templar.
                Here: the belt and its buckle (the Paladin's with a gem) on the body; on each leg the cloth, flaps
                before, behind and outside it flaring to below the knee, the front tasset and the side tasset, both
                hinged out from the hip, a raised spiral boss on the front one (with the Paladin's gem)
    boots       banded greaves and sabatons. Here: the greave, a knee cop on its front, the sabaton, two instep lames
                overlapping toward the toe and a toe cap
Colours: armor_paint.PALADIN and armor_paint.TEMPLAR.

The renders show the front and a three-quarter view; the backs are drawn in the designs' own words (the bars and
spirals run round, a plain back plate with a spine, the tassets' and the cloth's flaps behind too). The weapons in two
of the pictures are not armor; they are left for another time. Every box is closed: a face is left out only where
another box of the same piece and bone covers it (the visor's, the reliefs', the shield's, the back plate's and the
bosses' backs, the crest's and the sprig's feet, each arm plate's end inside the next, the boot under the sabaton), so
the sets show no holes alone, on an armor stand's thin limbs (tools/art_check.py, H1).

Each set is a Blockbench project, art/armor/paladin.bbmodel and art/armor/templar.bbmodel: its cubes and its texture
are the set, read by tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open one in Blockbench to look at
it on a player or edit it; save it with the texture embedded and rerun the generators. They were written on 8 October
2026 from the build this module held until then (in Git history), with one change the renders showed: the two lames
under each pauldron are gone, so the mail sleeve shows under it, as in all four renders.

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z).
Preview: python3 tools/armor_preview.py --set paladin --set templar
"""
from pathlib import Path

import bbmodel
from armor_paint import PALADIN, TEMPLAR

ART = Path(__file__).resolve().parents[1] / "art/armor"


def load(kind, palette):
    return bbmodel.load_set(kind, palette, ART / f"{kind}.bbmodel",
                            items=[f"{kind}_{piece}" for piece in ("helmet", "chestplate", "leggings", "boots")])


SETS = [load("paladin", PALADIN), load("templar", TEMPLAR)]
