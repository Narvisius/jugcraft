"""Scarab: the owner's gold-and-lapis Egyptian set (one of the designs they sent on 8 October 2026 with no words: a
render of the set worn, from behind on the left and from the front, beside its four 16x16 icons) as a 3D worn model for
jugcraft:scarab_* (helmet, chestplate, leggings, boots), on the toolkit in tools/armor_models.py (shapes) and
tools/armor_paint.py (the atlas). Its icons are the owner's, transcribed to maps (tools/armor_icons/scarab/). The name
is a placeholder: the owner's Pharaoh Armor (tools/pharaoh_armor.py) is another set.

What the owner drew, and where it is here (built part for part, as the owner asked: "complex models ... really
capturing the crazy unique geometry of each armor"):
    helmet      a nemes headcloth striped gold and lapis: a band across the brow, the cloth over the crown and down
                behind the head, flaring out past it, and its lappets falling either side of the face onto the chest;
                the face open. Here: the crown over the head, the brow band round it (0.2 proud), the lappets either
                side of the face down past the chin, the cloth down behind the head to the neck, flaring 0.2 wider,
                three raised lapis bands round it, and a cobra rearing on the brow
    chestplate  gold plate: a broad collar of gold and lapis rows on the chest, lapis bands down either side of the
                belly and a lapis belt; on the shoulders square guards striped gold and lapis down to the elbow; the
                forearms bare. Here: the cuirass, the collar in three stepped rows, the two lapis bands, the gold plate
                between them, the belt; the back plate; on each arm the guard, a raised lapis band round it and a gold
                band round its foot
    leggings    a gold kilt to above the knee, a lapis key pattern down its front. Here: the kilt round each leg, loose
                as a kilt, and its front panel, hinged out a little from the waist
    boots       tall gold boots chequered in lapis. Here: the boot from below the knee, two raised bands round it, the
                gold sole and toe
Colours: armor_paint.SCARAB.

The forearms are bare, as drawn. The renders show the front and the left side from behind; the right side is drawn
from them. Every box is closed: a face is left out only where another box of the same piece and bone covers it.

The model is a Blockbench project, art/armor/scarab.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators. It was written on 8 October 2026, unchanged, from the
build this module held until then (in Git history).

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z).
Preview: python3 tools/armor_preview.py --set scarab
"""
from pathlib import Path

import bbmodel
from armor_paint import SCARAB

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/scarab.bbmodel"
ITEMS = ["scarab_helmet", "scarab_chestplate", "scarab_leggings", "scarab_boots"]

SETS = [bbmodel.load_set("scarab", SCARAB, PROJECT, items=ITEMS)]
