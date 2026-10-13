"""Spartan: the owner's gold plumed design (the fourth of the four designs they sent on 7 October 2026, a sheet of the
four on a blank mannequin and a screenshot of each worn in game) as 3D worn models for jugcraft:spartan_helmet,
_chestplate, _leggings and _boots, on the toolkit in tools/armor_models.py (shapes) and tools/armor_paint.py (the
atlas).

What the owner drew, and where it is here:
    helmet      a gold Corinthian helm, its face cut in a T (a slit across the eyes, a gap down to the chin between the
                cheek guards), the face showing through it; a crest of red and orange horsehair running from the brow
                over the crown and falling down the back of the head to the shoulders. Here: the bowl down to the eye
                line, a jaw block behind the face, a temple block at each end of the eye slit and a cheek guard each
                side of the gap, all 1.0 off the head; the crest's four blocks along the top, stepped to a ragged ridge
                with tufts, and its tail hanging behind
    chestplate  a gold muscle cuirass (the chest's two plates, the stomach's ridges, bronze lines between); red cloth
                over the right shoulder and across the top of the chest, falling behind as a cape to the knee on that
                side; a gold pauldron on the left shoulder with a curled scroll on its outer face; gold bracers on the
                forearms; the upper arms bare. Here: the cuirass; the red mantle over the right half of the chest's top
                and the cape down the right half of the back; on the right arm the red drape over the shoulder and the
                upper arm, the cape's tail behind the right thigh (on the leg, so it follows it); on the left arm the
                pauldron and its scroll; a bracer on each forearm
    leggings    a skirt of brown leather strips (pteruges) studded gold at their ends over the thighs, a brown belt.
                Here: the belt on the body and on each leg the strip skirt to mid-thigh; the knees bare
    boots       gold greaves from the knee, a pale gold cap over each knee, bands round the shin. Here: the greave, the
                knee cap standing 0.4 proud of it, and a brown sandal sole under it
Colours: armor_paint.SPARTAN: the gold from a pale lit gold through the warm golds of the plates to the bronze browns
of their lines and edges; the plume's oranges and reds and the cape's wine; the brown leather.

The sheet shows the set from the front, a little above and to the left, the screenshot from the front. The back is
drawn in the design's own words: the helm is gold all round behind its crest, the cuirass's back carries a spine line
and shoulder blades, the cape hangs down the right half of the back, the skirt's strips and studs run all round. The
face and the upper arms are left open, as drawn; every box is closed, so nothing shows through them alone or on an
armor stand (tools/art_check.py, H1). A box is its own plate; the muscle lines, the scroll, the strips, the studs and
the plume's locks are paint, one texel per model pixel.

The model is a Blockbench project, art/armor/spartan.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators. It was written on 8 October 2026, unchanged, from the
build this module held until then (in Git history).

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left; the left arm's pauldron has no right twin.
Preview: python3 tools/armor_preview.py --set spartan
"""
from pathlib import Path

import bbmodel
from armor_paint import SPARTAN

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/spartan.bbmodel"
ITEMS = ["spartan_helmet", "spartan_chestplate", "spartan_leggings", "spartan_boots"]

SETS = [bbmodel.load_set("spartan", SPARTAN, PROJECT, items=ITEMS)]
