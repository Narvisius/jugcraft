"""Wight King: the owner's slate-blue knight crowned with icicles and antlers (one of the designs they sent on 8 October
2026 with no words: two renders, from the front and from behind on the left, of a grey-blue knight with a crown of
icicles, two tall antlers, a black skull's face, cyan gems and a long sword) as a 3D worn model for
jugcraft:wight_king_* (helmet, chestplate, leggings, boots), on the toolkit in tools/armor_models.py (shapes) and
tools/armor_paint.py (the atlas). Its sword is an arm of its own (tools/arms_variants.py). The name is a placeholder:
the renders carry none.

What the owner drew, and where it is here (built part for part, as the owner asked: "complex models ... really
capturing the crazy unique geometry of each armor"):
    helmet      a slate helm whose face is a black skull: a brow bar over two black eyes, a bar down the nose, cheek
                plates either side of a mouth of teeth; a crown of icicles round its top, the middle one at the front
                the tallest; two tall antlers rising from its sides, bending out and then up, pale at their tips.
                Here: the helm and the crown's band; the black mask 0.4 proud of the helm's front, the brow bar, the
                nose bar, two cheek plates leaning out at their tops, four teeth and a chin bar on it; seven icicles on
                the band, each one to three stacked prisms turned on their edge and tapering, the front middle one
                tallest and the outer ones leaning out; two antlers of four bars each, every bar narrower and more
                upright than the one below it, the last one pale
    chestplate  slate plate: a raised V collar meeting at a cyan gem on the breastbone, two dark straps crossing over
                the belly; great shoulders, the right a mass of jagged ice shards, the left a layered block; mail under
                them, banded vambraces with flared cuffs, gauntlets. Here: the cuirass, the collar (two bars), the gem;
                the two straps crossing and a boss where they cross; a back plate and a spine; on the right arm the
                pauldron, its crown, four shards (flat blades with narrower tips) and two lames hinged out under it; on
                the left the pauldron, a cap on it and a second cap on that, a rim round its foot and one shard; on
                both the mail sleeve, the vambrace, the cuff with a fin swept back and the gauntlet
    leggings    a dark belt with a square cyan buckle, two plates hanging over each thigh's front, banded thighs, a
                knee cop. Here: the belt, the buckle's frame and its gem; on each leg the banded cuisse, two tassets
                hinged out from their tops, the knee band and the knee cop
    boots       greaves of bands, pointed sabatons. Here: three rings, the middle one sunk between the others; the
                sabaton and a pointed toe
Colours: armor_paint.WIGHT_KING; the icicles and antlers in its five ice tones.

The renders show the front and the left side from behind; the right is drawn from the front view. The glow round the
knight in the renders, and the bits of ice floating round it, were taken as the scene, not the armor. Every box is
closed: a face is left out only where another box of the same piece and bone covers it (the reliefs' backs on the
plates they sit on, the bars' feet inside the bar below).

The model is a Blockbench project, art/armor/wight_king.bbmodel: its cubes and its texture are the set, read by
tools/bbmodel.py (load_set) into worn_models.json and the atlas. Open it in Blockbench to look at it on a player or edit
it; save it with the texture embedded and rerun the generators. It was written on 8 October 2026, unchanged, from the
build this module held until then (in Git history).

Space (tools/armor_models.py): each bone's own space in model pixels; x is the model's LEFT, y is DOWN, z is the BACK
(the face is at -z).
Preview: python3 tools/armor_preview.py --set wight_king
"""
from pathlib import Path

import bbmodel
from armor_paint import WIGHT_KING

PROJECT = Path(__file__).resolve().parents[1] / "art/armor/wight_king.bbmodel"
ITEMS = ["wight_king_helmet", "wight_king_chestplate", "wight_king_leggings", "wight_king_boots"]

SETS = [bbmodel.load_set("wight_king", WIGHT_KING, PROJECT, items=ITEMS)]
