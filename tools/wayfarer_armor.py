"""Wayfarer: the owner's blue hooded cloak (the third of the four designs they sent on 7 October 2026, a sheet of the
four on a blank mannequin and a screenshot of each worn in game) as 3D worn models for jugcraft:wayfarer_helmet,
_chestplate, _leggings and _boots, on the toolkit in tools/armor_models.py (shapes) and tools/armor_paint.py (the
atlas).

What the owner drew, and where it is here:
    helmet      a deep hood in mottled navy and blue, a little bigger than the head, its face opening edged in light
                teal; the face shows. Here: the hood as closed blocks round the head, 1.25 off it (a top block down to
                the brow, a block down each side and one behind), the opening between them; a teal brim over the
                opening and a teal rim down each side of it, 0.5 proud
    chestplate  a cloak over the shoulders and the upper arms, open down the front over a dark tunic, its front edges
                light teal, a round silver clasp with a teal heart at the chest; it hangs longer on the model's right
                side, to the knee; the forearms bare. Here: the mantle over the shoulders, the tunic, the two front
                panels with their teal edges and the back panel to the waist, the clasp on the left panel; on each arm
                the cloak to the elbow; behind each thigh the cloak's tail, the right one to the knee, the left shorter
                (on the legs, so it follows them)
    leggings    a short kilt of dark brown leather over the thighs, a belt, a row of light studs along its hem. Here: the
                belt on the body and on each leg the kilt to mid-thigh; the knees and shins bare, as drawn
    boots       dark brown boots from mid-shin, a lighter band and two pale laces on the front, and a small wing at each
                ankle, white and ice blue with pink tips. Here: the boot, its cuff and, on its outer side, a fan of three
                feather planks swept back
Colours: armor_paint.WAYFARER: the cloak's blues from a light teal to the darkest navy, the clasp's pale silver, the
dark red-brown leather, the hood's shadowed inside and the feathers' white, ice blue, pink and steel blue.

The sheet shows the set from the front, a little above and to the left, the screenshot from the front. The back is
drawn in the design's own words: the hood and the cloak run round in the same mottled blue, the cloak's tail behind the
thighs, the kilt's studs and the boots' bands all round. The face and the forearms are left open, as drawn; every box
is closed, so nothing shows through them alone or on an armor stand (tools/art_check.py, H1). A box is its own plate;
the mottling, the edges, the studs and the laces are paint, one texel per model pixel.

The model is a Blockbench project, art/armor/wayfarer.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators. It was written on 8 October 2026, unchanged, from the
build this module held until then (in Git history).

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left.
Preview: python3 tools/armor_preview.py --set wayfarer
"""
from pathlib import Path

import bbmodel
from armor_paint import WAYFARER

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/wayfarer.bbmodel"
ITEMS = ["wayfarer_helmet", "wayfarer_chestplate", "wayfarer_leggings", "wayfarer_boots"]

SETS = [bbmodel.load_set("wayfarer", WAYFARER, PROJECT, items=ITEMS)]
