"""Valkyrie: the owner's white and gold winged design (the second of the four designs they sent on 7 October 2026, a sheet
of the four on a blank mannequin and a screenshot of each worn in game) as 3D worn models for jugcraft:valkyrie_helmet,
_chestplate, _leggings and _boots, on the toolkit in tools/armor_models.py (shapes) and tools/armor_paint.py (the
atlas).

What the owner drew, and where it is here:
    helmet      no helm: a gold laurel wreath round the head with big leaves over the brow and a curled boss at each
                front corner, and a white feathered wing rising up and back from each temple, tinted pink and lilac,
                its feathers stepping out to a ragged edge; the wearer's face shows. Here: the wreath, four bars round the
                head 0.65 off it (0.15 off the hat layer), its leaves standing on it (a broad pair over the brow, two
                swept back along each side), the bosses at the front corners; each wing a fan of four feather planks
                from the temple, the highest the longest, rolled out from the head and swept back
    chestplate  a white muscle cuirass, the chest and the stomach's ridges in mauve and blue-grey; brown straps over the
                shoulders, buckled gold on the chest; red cloth wound round each shoulder in bands, its ends fluttering
                out, and red ribbons hanging behind the arm past the hand; dark red cloth on the upper arm; a bracer on
                each forearm, gold bands round a white panel. Here: the cuirass; the straps and their buckles; on each
                arm three wound bands (each rising a little toward the outside), a ribbon end standing out from the top
                one, two streamers hanging behind the arm, the red sleeve, the bracer
    leggings    a skirt of brown leather strips with gold studs along their ends over the thighs, white linen strips
                hanging at its front, a brown belt studded gold. Here: the belt on the body, and on each leg the strip
                skirt to mid-thigh, its front inner strips white
    boots       a greave round the shin, gold bands round a white band, a small white wing at its outer side; the feet
                bare. Here: the greave and, on its outer side, a fan of three feather planks swept back
Colours: armor_paint.VALKYRIE: ivory, cream and beige plate shaded blue-grey and mauve; three golds; the red cloth from a
lit coral to a wine; the brown leather; the feathers' pink, lilac and violet.

The sheet shows the set from the front, a little above and to the left, the screenshot from the front; they differ
only in the boots' wings, which the screenshot shows. The back is drawn in the design's own words: the wreath runs round
the head, the cuirass's back is white with a spine line, the straps cross the shoulders to buckles behind, the skirt's
strips and studs run all round. The face and the feet are left open, as drawn: the wreath, the wings and the greave
are rings and fans, every box of them closed, so nothing shows through them alone or on an armor stand
(tools/art_check.py, H1). A box is its own plate; the muscle lines, strips, studs and feather tints are paint, one texel
per model pixel.

The model is a Blockbench project, art/armor/valkyrie.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators. It was written on 8 October 2026, unchanged, from the
build this module held until then (in Git history).

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z). Right-hand parts are built and mirrored to the left.
Preview: python3 tools/armor_preview.py --set valkyrie
"""
from pathlib import Path

import bbmodel
from armor_paint import VALKYRIE

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/valkyrie.bbmodel"
ITEMS = ["valkyrie_helmet", "valkyrie_chestplate", "valkyrie_leggings", "valkyrie_boots"]

SETS = [bbmodel.load_set("valkyrie", VALKYRIE, PROJECT, items=ITEMS)]
