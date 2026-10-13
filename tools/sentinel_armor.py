"""Sentinel: the owner's gold-and-black knight (one of the two designs they sent later on 8 October 2026, "Just made
these ones aswell want them done weapons too please": three Blockbench renders, from the front and a little to the
right, from a little to the left, and from behind, kept in art/armor/references/sentinel/) as a 3D worn model for
jugcraft:sentinel_* (helmet, chestplate, leggings, boots). Its sword and shield are arms of their own
(tools/arms_variants.py).

The model is a Blockbench project, art/armor/sentinel.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators.

It was rebuilt on 8 October 2026 to match the owner's renders: "can you just 1:1 copy the pixel art from the source I
gave you". A camera and the figure's pose were fitted to each render (tools/armor_reference.py), the parts measured
off them, and every texel the renders show was lifted from them face by face. Their texture is drawn two texels to a
model pixel, so the atlas is too (density 2). The renders show:
    helmet      a 9 x 9 x 9 gold helm with its art painted on (the keyhole and cheek strips before, the meander's
                hooks behind and on the sides) and the pentagonal loop on its crown, two posts and a roof
    chestplate  the black coat with the stepped gold baldric painted across it and the gold gorget over the shoulders;
                on the right shoulder the great pauldron, a lower plate down the outside of the upper arm and an upper
                plate rising 51.6 degrees in toward the neck, both 3.5 px thick with the owner's bands on their faces,
                and a small foot stepping out below; the square stud; on the left shoulder the stacked gold plates;
                the sleeves, bracer, bands and gauntlets
    leggings    the hose and a plate before and behind each thigh hinged out from the hip, gold on the left and dark
                iron on the right
    boots       short gold boots whose tops rise in teeth like a crown
Where the renders show nothing (the inside of the arms, under the feet) the faces are filled from the faces opposite.
The figure in the renders holds the sword and shield with the right arm 10 degrees and the left 37 degrees forward.

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK.
Preview: python3 tools/armor_preview.py --set sentinel
"""
from pathlib import Path

import bbmodel
from armor_paint import SENTINEL

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/sentinel.bbmodel"
ITEMS = ["sentinel_helmet", "sentinel_chestplate", "sentinel_leggings", "sentinel_boots"]

SETS = [bbmodel.load_set("sentinel", SENTINEL, PROJECT, items=ITEMS)]
