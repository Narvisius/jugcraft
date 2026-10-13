"""Frost Knight: the owner's white-and-ice knight (one of the two designs they sent later on 8 October 2026, "Just made
these ones aswell want them done weapons too please": two Blockbench renders, from the front and from behind on the
left, kept in art/armor/references/frost_knight/) as a 3D worn model for jugcraft:frost_knight_* (helmet, chestplate,
leggings, boots). Its ice sword is an arm of its own (tools/arms_variants.py).

The model is a Blockbench project, art/armor/frost_knight.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators.

Rebuilt on 8 October 2026 to match the owner's front render, after they wrote of the earlier ones: "The frost night
looks bad because of the head part and how you gave it like a mouth instead of making it match the image I gave and in
general the pants are not detailed enough", then "I don't like how robotic and square the helmet you made is or how you
are missing so many details on the shoulder and stuff can you really detail the frost knight accurately". A camera was
fitted to the helm's face and one to the legs in that render (tools/armor_reference.py: the owner's figure stands
differently from ours, so each region has its own), the crown measured off it, and its texels lifted:
    helmet      the owner's 9 x 9 x 9 box with their face on it, texel for texel (the black band across the eyes
                turning up at its ends, the white nose bar, the dark gap under it, the grille of five slits, the
                mottled white brow); the crown: a tall crystal 3 wide and 14 above the helm (its body and its tip), at
                each top corner of the face a crystal of two columns, 8 and 6 tall, a short shard leaning on the left
                one, and the gable's two white bars rising in behind the tall one, every crystal coloured as the owner
                painted it; the frost the render shows round the helm, as cut-out sheets facing the front with its
                texels: on the left two tufts, one over the other, flaring up and 7.75 px out, on the right two narrow
                sheets of spikes down its side; behind them blades along each side and along the top's back edge; on
                the back of the helm the ice cross, painted
    chestplate  as before: the white cuirass and its chest plates, the navy strap before and behind, the navy pauldron
                with its white key spiral and rim on the left shoulder, the frost on the right shoulder and its
                feathers down the arm, the drape behind
    leggings    the navy belt and its ice gem, the navy flap over the left hip, and on each leg the cuisse, three
                lames, the knee band, cop and wing, their fronts carrying the owner's texels
    boots       the cuff, the greave (its front the owner's texels), two frost feathers at the ankle, the sabaton and
                the toe cap
The back render is softer and the figure stands turned in it, so it was used to check the model by eye, not to lift
texels: the helm's back is painted (the cross on mottled white) and the parts the front render does not show keep the
earlier paint. Worn armor is drawn from both sides, so the frost sheets show from behind too, where the owner's
Blockbench view hides them.

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK.
Preview: python3 tools/armor_preview.py --set frost_knight
"""
from pathlib import Path

import bbmodel
from armor_paint import FROST_KNIGHT

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/frost_knight.bbmodel"
ITEMS = ["frost_knight_helmet", "frost_knight_chestplate", "frost_knight_leggings", "frost_knight_boots"]

SETS = [bbmodel.load_set("frost_knight", FROST_KNIGHT, PROJECT, items=ITEMS)]
