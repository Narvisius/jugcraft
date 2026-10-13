"""Banana: the owner's banana costume (one of the designs they sent on 8 October 2026 with no words: a render of a player
inside a tall yellow banana, the face showing through a hole in it, a brown stem on top, the arms bare out of its sides
and the banana's brown end sticking out at the feet) as a 3D worn model for jugcraft:banana_* (helmet, chestplate,
leggings, boots), on the toolkit in tools/armor_models.py (shapes) and tools/armor_paint.py (the atlas). The name is a
placeholder.

What the owner drew, and where it is here:
    helmet      the banana's top: a tall yellow box round the head, the face showing through a hole in its front, a
                brown stem on its flat top. Here: the box built of five closed blocks round the hole (the crown above
                it, the chin below, a cheek either side, and the back between the cheeks behind the head), so the face
                shows in game; on top the banana's darker end and the stem
    chestplate  the banana's middle round the body, as deep as the head's box; the arms bare out of its sides. Here: the
                tube round the body, its ridges raised down its front and back, its speckles painted
    leggings    the banana's lower part round the legs, to below the knee. Here: a tube round each leg (so the legs
                can swing), meeting at the middle
    boots       the banana's bottom, its brown end sticking out forward by the right foot. Here: the tube's foot round
                each shin, darkening, and the brown end on the right
Colours: armor_paint.BANANA.

The arms and the feet are bare, as drawn. Every box is closed: a face is left out only where another box of the same
piece and bone covers it (the cheeks' and the back's ends inside the crown and the chin, the feet of the collar, the
stem and its tip on what each stands on, the nub's back on the brown end).

The model is a Blockbench project, art/armor/banana.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators. It was written on 8 October 2026, unchanged, from the
build this module held until then (in Git history).

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z).
Preview: python3 tools/armor_preview.py --set banana
"""
from pathlib import Path

import bbmodel
from armor_paint import BANANA

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/banana.bbmodel"
ITEMS = ["banana_helmet", "banana_chestplate", "banana_leggings", "banana_boots"]

SETS = [bbmodel.load_set("banana", BANANA, PROJECT, items=ITEMS)]
