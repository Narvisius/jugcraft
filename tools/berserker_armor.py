"""Berserker: the owner's horned red-and-white design (the first of the three they sent on 8 October 2026, "I made these
3": a Blockbench render of the set worn, from a little to the side, beside each piece on its own) as a 3D worn model
for jugcraft:berserker_* (helmet, chestplate, leggings, boots), on the toolkit in tools/armor_models.py (shapes) and
tools/armor_paint.py (the atlas).

What the owner drew, and where it is here (the owner asked for "complex models ... really capturing the crazy unique
geometry of each armor", so each shape is built, not painted):
    helmet      an open-faced white cap with a lump of red on its crown that spills down over the brow; on each side a
                square horn standing straight up from a short foot at the cap's side; under it, round the jaw, a thin
                white frame with teeth standing up from its chin bar. Here: the cap and a rim round its foot; the
                crest, a smaller lump on it, its front down the brow and two drips under that; on each side the horn,
                its narrower tip, a grey band round its root and its foot; the jaw frame: a post down each front
                corner, the chin bar, four teeth on it, a bar back along each side of the jaw and a post up from its
                end to the cap
    chestplate  a white breastplate keyed in grey with red bands down its sides and over the shoulders, a red band at
                the waist parted at the middle under a grey one, stepped red pauldrons edged in grey and white with a
                white line on their inner side, and plain white bracers on the forearms (the upper arms bare). Here:
                the breastplate; on each side a red band raised on its front, over the shoulder and down its back; a
                grey hoop and a red hoop round the waist; on each arm the pauldron, its grey-and-white trim, a raised
                step and a crown on its outer part, a white ridge along its inner edge; the bracer with a flange at
                each end and two studs on its outer side
    leggings    thigh guards banded red and white with a red edge, over dark grey mail; a red and grey belt. Here: the
                belt and a white buckle on the body; on each leg the mail and four bands stepping out as they go down,
                white, red, white, red
    boots       grey boots with a dark band and red soles. Here: the boot, a white cuff round its top, a dark band
                round its ankle, the sole and a red toe cap
Colours: armor_paint.BERSERKER, sampled from the render (white and greys, the reds, the dark greys of the mail).

The render shows the front and the pieces from the front; the back is drawn in the design's own words (the bands run
round, the red edges repeat behind). Every box is closed: a face is left out only where another box of the same piece
and bone covers it (the crest's foot and front, the reliefs' backs, the horn's foot at both ends, the teeth's feet, the
chin bar's ends, the steps' feet, each band's foot inside the next, the boot under the sole), so the set shows no holes
alone, on an armor stand's thin limbs (tools/art_check.py, H1).

The model is a Blockbench project, art/armor/berserker.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators. It was written on 8 October 2026, unchanged, from the
build this module held until then (in Git history).

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left.
Preview: python3 tools/armor_preview.py --set berserker
"""
from pathlib import Path

import bbmodel
from armor_paint import BERSERKER

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/berserker.bbmodel"
ITEMS = ["berserker_helmet", "berserker_chestplate", "berserker_leggings", "berserker_boots"]

SETS = [bbmodel.load_set("berserker", BERSERKER, PROJECT, items=ITEMS)]
