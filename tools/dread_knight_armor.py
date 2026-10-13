"""Dread Knight: the owner's dark crowned knight (the first of the four designs they sent on 7 October 2026, a sheet of
the four on a blank mannequin and a screenshot of each worn in game) as 3D worn models for jugcraft:dread_knight_helmet,
_chestplate, _leggings and _boots, on the toolkit in tools/armor_models.py (shapes) and tools/armor_paint.py (the atlas).

What the owner drew, and where it is here:
    helmet      a great helm a little bigger than the head, near-black, under a crown of light grey merlons: a broad one
                over the brow, smaller ones at the corners and the sides, notched between; a light grey brow band, the
                eyes two black slits either side of a nasal bar, a light frame down beside each eye and under it, the
                mouth and chin dark; a dark window framed light on each side. Here: the helm (closed underneath, 0.65
                below the head); the crown band round its top and the merlons on it, the front and back ones the tallest;
                the brow band, the nasal bar, the posts and the cheek plates standing 0.5 proud of the helm's front, its
                slits painted between them; the windows painted on the sides
    chestplate  a dark cuirass under a mottled grey muscle plate (the chest and the ridges of the stomach, a dark line
                down the middle); big blocky pauldrons in light and dark bands with two small spikes standing on each;
                the arms banded light and dark to the wrist, a black band at the elbow. Here: the cuirass, the muscle
                plate on its front and a banded plate on its back; on each arm the pauldron, a flared lame under it, its
                two spikes leaning out, then the upper arm plate, the black elbow band, the vambrace and a flared cuff
    leggings    a black belt over a black skirt of upright strips riveted grey along its foot, the legs black to the
                knee. Here: the belt with a grey buckle, and on each leg a dark cuisse to the ankle and the strip skirt
                over it to mid-thigh, riveted along its foot
    boots       banded greaves: a light grey cuff at the knee, then dark, a light band, dark, and light grey sabatons.
                Here: the cuff, the greave and the sabaton, longer at the toe
Colours: armor_paint.DREAD_KNIGHT: steel greys from the crown's light grey to near-black, the owner's faint pink sheen
on the lit greys, and a near-black under-layer whose darkest is the eye slits.

The sheet shows the set from the front, a little above and to the left; the screenshot from the front. The back is
drawn in the design's own words: the crown runs all round; the cuirass's back carries bands; the pauldrons, arms, skirt
and boots are banded all round. Every box is closed: a face is left out only where another box of the same piece and
bone covers it (the helm's top inside the crown band, the merlons' feet on it, the face plates' backs on the helm, each
arm plate's end inside the next), so the set shows no holes alone, on an armor stand's thin limbs (tools/art_check.py,
H1). A box is its own plate; the bands, slits, strips and rivets are paint, one texel per model pixel.

The model is a Blockbench project, art/armor/dread_knight.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators. It was written on 8 October 2026, unchanged, from the
build this module held until then (in Git history).

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left.
Preview: python3 tools/armor_preview.py --set dread_knight
"""
from pathlib import Path

import bbmodel
from armor_paint import DREAD_KNIGHT

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/dread_knight.bbmodel"
ITEMS = ["dread_knight_helmet", "dread_knight_chestplate", "dread_knight_leggings", "dread_knight_boots"]

SETS = [bbmodel.load_set("dread_knight", DREAD_KNIGHT, PROJECT, items=ITEMS)]
