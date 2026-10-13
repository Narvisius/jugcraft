"""Reaper: the owner's hooded reaper (one of the designs they sent on 8 October 2026 with no words: a render of a figure
in a dark hooded robe, its face a black void behind three bars under an arch, a white clasp at its throat, a brown
pouch at its hip, white angular plates on its arms, holding a short white scythe in each hand, kept in
art/armor/references/reaper/) as a 3D worn model for jugcraft:reaper_* (helmet, chestplate, leggings, boots). The name
is a placeholder. The two scythes are weapons, not armor ("He is supposed to be holding 2 short scythe weapons they
arent part of the armor", the owner, 8 October 2026): the Reaper Scythe, an Arms VII set arm (tools/arms_variants.py),
one for each hand.

The model is a Blockbench project, art/armor/reaper.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators.

Built part for part from the render in the first session, after the owner wrote of the first one "in general you just
did that one really low quality"; matched to the render again on 8 October 2026 (a camera fitted to the figure's hood,
robe and legs, tools/armor_reference.py):
    helmet      a deep hood rising to a rounded crown in two steps; the opening's arch built of seven blocks round it,
                each stepping further back toward its top, so the arch reads as cut into the cloth; the three grey
                bars in the black void, the middle one the palest and longest; three folds down each side; the fall
                down the back of the neck and the hood's point behind the crown
    chestplate  the robe, its front the owner's texels; the hood's cloth over the shoulders, ragged, with tatters
                before and behind; folds down the front and back; the white V clasp at the throat, as high as the
                render has it, just under the hood's opening; the lighter strap stepping down across the chest from
                the right shoulder to the grey buckle at the left hip, and across the back; on the right arm grey
                plates stepping down the shoulder under a white V, a white band, a white elbow plate and a grey bracer
                rimmed white; on the left arm a white cap, an open white cage round the upper arm and a white cage
                round the fist (the render shows the arms raised round the scythes' grips)
    leggings    the sash at the robe's foot; on each leg the dark under-robe and five strips of the robe round it,
                hinged out a little from the waist; on the left hip the pouch, as big as the render's (6 by 6.4 px,
                from the centre line to past the thigh's outer side, from the sash to mid-thigh), a frame of four bars
                round its sunk front and a stud; the robe's strips and the pouch carry the owner's texels
    boots       dark wrapped boots, two grey wraps round the shin, a white band at the ankle and a dark toe
The render shows the front only; the back is drawn in the design's words. Every box is closed: a face is left out only
where another box of the same piece and bone covers it.

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK.
Preview: python3 tools/armor_preview.py --set reaper
"""
from pathlib import Path

import bbmodel
from armor_paint import REAPER

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/reaper.bbmodel"
ITEMS = ["reaper_helmet", "reaper_chestplate", "reaper_leggings", "reaper_boots"]

SETS = [bbmodel.load_set("reaper", REAPER, PROJECT, items=ITEMS)]
