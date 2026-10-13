"""The 3D armor sets' 16x16 inventory icons: the bronze and steel armor's, the knight armor (tools/knight_armor.py)
drawn small, and Bloodthorn Armor's, Reforged White Diamond's, Hades Armor's, Sunset Gem's, Pharaoh's, and the Dread
Knight's, Valkyrie's, Wayfarer's and Spartan's (tools/bloodthorn_armor.py, tools/white_diamond_armor.py,
tools/hades_armor.py, tools/sunset_gem_armor.py, tools/pharaoh_armor.py and the four after it, below), in the owner's
style for item icons (vanilla's own size, a one-pixel outline in each
part's darkest tone, never pure black, light from the top left, a few flat tones, chunky parts that read at a glance).

Each piece is one hand-drawn map, tools/armor_icons/<piece>.txt: 16 lines of 16 symbols, which the owner can edit
directly (lines starting with # are comments). A map names no colours, only what each pixel is made of, so one map
serves both metals: steel_<piece> is coloured from armor_paint.STEEL (the owner's sampled steel, gold and leather) and
bronze_<piece> from armor_paint.BRONZE, the same palettes the 3D armor is painted in.

What each icon carries of the knight (front view, as vanilla draws armor; boots in vanilla's stance):
    helmet      the big diamond crest overhanging the helm, with its nested chevrons and the finial, the horn fins at
                its corners, the dark seam under it, the eye slits beside the nasal bar, and the gold collar
    chestplate  the two layered pauldrons with nested L's toward the armpit, the light chevron plate over the chest
                with its bold dark V and the keel at its point, the hem lame, and the dark under-layer at the waist
    leggings    the brown leather belt (the under-layer's light top edge, two plates, the strap across, the
                under-layer showing at the centre) over the skirt of four plate lames, light over dark, widening to
                the hem, with the dark seam down the middle
    boots       greaves with a rolled top, a shadow under it and the knee cop's small chevron, and sabatons of banded
                instep lames, light over dark, with pointed toes
The bronze icons add what the bronze variant adds in brass: the finial and horn tips, rivets on the pauldron lames, the
hem lame and the skirt lames' ends, and the belt buckle.

Symbols:
    .           transparent
    O D M L H   the plate: outline, dark, mid, mid-light, light (the ramp's "dark", "mid", "mid_light", "light"; the
                outline is its "void" taken down to an item icon's outline depth, OUTLINE_LUMA)
    F Y g       the gold trim (gold on steel, brass on bronze): dark, light, outline
    R r         fittings (finial, horn tips, rivets): brass on bronze (light, dark); on steel the plate's own light
                and mid, since the steel has no brass there (knight_armor.py, the bronze variant): put R where the
                steel plate is lit and r where it is mid
    Q           the belt's centre: a brass buckle on bronze, the strap's lighter middle on steel (as the owner drew it)
    w k b B     leather: outline, strap, plate, top edge ("leather_darkest", "leather_dark", "leather_mid",
                "leather_light")
    u U         the dark under-layer: fill, top edge ("under_dark", "under_light")

A set may instead have maps of its own, tools/armor_icons/<set>/<piece>.txt, with its own symbols (OWN): Bloodthorn
Armor (tools/bloodthorn_armor.py) has bloodthorn/helmet.txt and so on, coloured from armor_paint.BLOODTHORN:
    helmet      the boxy great helm cut by three slits (two wide, a narrower one below) under its crown of spikes: the
                tall one at the centre, a long pair and a lower pair splayed wider
    chestplate  the pauldrons tilted up toward the outside, the collar with its dark inside showing, the breastplate
                running coral to crimson, and the magenta V over the dark waist
    leggings    the dark belt; the tassets, lit at the top left with a nested L toward the inner lower corner, their
                tops stepping down to the centre; the front plate with its dark strap; the diamond knee plates on the
                dark knees
    boots       two chunky boots in magenta and plum strips, notched dark at the top
Their symbols:
    .               transparent
    O               the outline: the metal's "void" taken down to OUTLINE_LUMA
    H L R M D S V   the metal, light to dark: "light" (orange), "mid_light" (coral), "gold_light" (the design's red,
                    see armor_paint.BLOODTHORN), "mid" (crimson), "dark" (magenta), "seam" (plum), "void" (dark plum)
    U m u x         the under-layer: "under_light", "under_mid", "under_dark", and "under_darkest", which is both the
                    eye slits and the under-layer's own outline
Reforged White Diamond (tools/white_diamond_armor.py) has reforged_white_diamond/helmet.txt and so on, coloured from
armor_paint.WHITE_DIAMOND and drawn after the owner's renders of each piece on its own:
    helmet      the white V crest, its point dipping into the charcoal T of the face, the light blue peak behind it, and
                the swept wings rising outside it
    chestplate  the wing pauldrons rising toward the outside over their lames, the light blue gem at the collar and the
                white V down the chest, strips at the waist (no sleeves: the owner's chestplate on its own has none)
    leggings    the belt, its top edge light blue, over the A of the two tassets, white along their outer edges and
                lined lavender, then blue, inside, with the striped under-skirt between and under them
    boots       two chunky boots under a light blue band, each with its diamond plate, a blue rim round a white heart
Their symbols:
    .               transparent
    O               the outline: the metal's "void" (lavender) taken down to OUTLINE_LUMA, a deep blue violet
    H L M D S V     the metal, light to dark: "light" (icy white), "mid_light" (pale cyan), "mid" (light cyan), "dark"
                    (light blue), "seam" (blue), "void" (lavender)
    U m u x         the charcoal under-layer: "under_light", "under_mid", "under_dark", "under_darkest" (the face)
Hades Armor (tools/hades_armor.py) has hades/helmet.txt and so on, coloured from armor_paint.HADES and drawn after the
owner's renders of each piece on its own (the boots, which the owner's sheet cuts off, after the 3D boots):
    helmet      the two horns rising from the helm's top corners, lit at their bends; the narrow crown's lit top band
                over the beak: the blue slate V of the brow falling from its outer corners to the ridge, dark above it,
                the slits on either side, the ridge below the V's point; at its foot the two cheeks and the chin, the
                dark collar between them
    chestplate  two plates a side, the mantle and the fin under it, rising toward the outside; the blue slate V at the
                collar; the breastplate flaring at the hem round the dark waist, with the blood-red cloth hanging from
                it
    leggings    the dark belt between the tops of the tassets, whose inner edges meet in a V; the lower plates flaring
                down the sides; the blood-red cloth hanging from the V over the dark under-layer, almost to the hem
    boots       two boots under a lit cuff that overhangs outward, a small blue slate V under it, the toes turned out
Their symbols:
    .               transparent
    O               the outline: the metal's "void" taken down to OUTLINE_LUMA, a dark blue grey
    H L M D S V     the metal, light to dark (cool greys): "light", "mid_light", "mid", "dark", "seam", "void"
    B               the blue slate: "gold_light" (the palette's name for it, see armor_paint.HADES)
    U m u x         the under-layer: "under_light", "under_mid", "under_dark", and "under_darkest", which is both the
                    eye slits and the under-layer's own outline
    R k r w         the blood-red cloth: "leather_light", "leather_mid_light", "leather_mid", and "leather_darkest", its
                    own outline
Sunset Gem (tools/sunset_gem_armor.py) has sunset_gem/helmet.txt and so on, coloured from armor_paint.SUNSET_GEM and
drawn after the owner's renders of each piece on its own:
    helmet      the crown of gem shards, the broad cream spike over the band, a smaller one at each corner and a shard
                splayed out at each side; the cream brow over the olive face with its yellow nose bar, peach cheek
                guards round the open chin, the coral jaw
    chestplate  the wings, cream and yellow crescents rising from the shoulders to their tips, the peach pauldrons under
                them, the breastplate with the cream U of its collar, in strips yellow and apricot down to coral (no
                forearms: the owner's chestplate on its own has none)
    leggings    the belt's red V, the flaps fanning out from it with crimson inner edges and a glint at each hip, the
                centre panel in strips, the hem with the crimson band and its mauve middle, parted between the legs
    boots       two chunky boots, a crimson cuff with a mauve middle, a peach strip, coral with crimson blocks, red
                soles, a shard flaring up from each outer side
Their symbols:
    .                   transparent
    O                   the outline of the gems: the metal's "void" (red) taken down to OUTLINE_LUMA, a deep red brown
    H L M D S V K Q     the gems, light to dark: "light" (cream), "mid_light" (pale yellow), "mid" (apricot), "dark"
                        (peach), "seam" (coral), "void" (red), "gold_light" (crimson) and "gold_dark" (mauve)
    U m u x             the olive under-layer: "under_light", "under_mid", "under_dark", "under_darkest"
    w                   the olive's own outline, for olive drawn against the edge: "leather_darkest", a dark olive
                        brown (the olive's darkest tone is far lighter than an icon's outline). None of the four maps
                        needs it yet: the olive shows only inside the helm's face.
Pharaoh (tools/pharaoh_armor.py) has pharaoh/helmet.txt and so on, coloured from armor_paint.PHARAOH and drawn after
the owner's render (its boots, which the render hides under the skirt, after the 3D boots):
    helmet      the nemes, its cap striped upright in gold and teal under the teal lobes, the gold brow band with the red
                gem on the uraeus's diamond, the side panels in level stripes; the tan face narrowing between the gold
                lappets and their dark teal ends to the chin and the short beard
    chestplate  the gold armlets on the tan shoulders, the collar's teal round its gold ring and the red gem, the
                breastplate's gold frame round the teal square, the dark waist
    leggings    the gold belt over the skirt's level bands, gold, teal, gold, tan, gold, teal, dark gold, widening to
                the dark teal hem, each band darker at the centre line, the halves parted at the foot
    boots       two sandal-greaves: the gold-rimmed teal cuff, the tan wraps with the framed teal shin plate, the gold
                strap and sole
Their symbols:
    .               transparent
    O               the outline of the gold and the tan: the metal's "void" (a dark gold-brown) taken down to
                    OUTLINE_LUMA
    H L M D         the gold, light to dark: "light", "mid_light", "mid", "dark"
    A a d e k       the teal, light to dark: "leather_light", "leather_mid_light", "leather_mid", "leather_dark", and
                    "leather_darkest", the near-black of the bands, which is the teal's own outline
    T t s n         the tan: "under_light", "under_mid", "under_dark", "under_darkest"
    R r             the red gems: "gold_light", "gold_dark" (the palette's names for them, see armor_paint.PHARAOH)
The four designs the owner sent on 7 October 2026 (docs/features/four-armor-designs.md) have maps of their own, drawn
after the owner's sheet and screenshots; each part is outlined in its own darkest tone, "O" or one named below. A tone
written "~<tone>" in OWN is that tone taken down to OUTLINE_LUMA, as "O" is, for a part whose own darkest tone is too
light to outline it.
Dread Knight (tools/dread_knight_armor.py, armor_paint.DREAD_KNIGHT): the crowned great helm with its slits and nasal
bar; the banded pauldrons with their spikes over the mottled muscle plate and black belt; the black strip skirt riveted
grey over the cuisses; the banded greaves.
    H L M D S V     the steel, light grey to near-black: "light", "mid_light", "mid", "dark", "seam", "void"
    P               the pink sheen on the lit greys: "gold_light"
    U u x X         the near-black under-layer: "under_light", "under_mid", "under_dark", and "under_darkest", the eye
                    slits and the black parts' own outline
Valkyrie (tools/valkyrie_armor.py, armor_paint.VALKYRIE): the wreath and its two wings; the red-wrapped shoulders over
the white cuirass; the studded strip skirt with its white linen; the gold-banded greaves with their small wings.
    W C B G M V     the white plate: "light", "mid_light", "mid", "dark" (blue-grey), "seam" (mauve), "void"
    Y y o g         the gold: "gold_light", "gold_mid", "gold_dark", and "~gold_dark", its outline
    R r q Q K       the red cloth: "leather_light" to "leather_dark", and "leather_darkest", its outline
    T t n N         the brown leather: "under_light", "under_mid", "under_dark", and "under_darkest", its outline
    F f v           the feathers' tints: "feather_pink", "feather_lilac", "feather_violet" (outlined "O")
Wayfarer (tools/wayfarer_armor.py, armor_paint.WAYFARER): the hood and its teal rim; the cloak with its clasp over the
dark tunic; the studded kilt; the boots and their winged ankles.
    H L M D S V     the cloak's blues, light teal to the darkest navy: "light" to "void" ("O" is its void)
    Y y             the clasp's silver: "gold_light", "gold_dark"
    B b m d k       the brown leather: "leather_light" to "leather_dark", and "leather_darkest", its outline
    U u x X         the hood's inside: "under_light", "under_mid", "under_dark", "under_darkest"
    W I F s g       the wings: "feather_white", "feather_ice", "feather_pink", "feather_steel", and "~feather_steel",
                    their outline
Spartan (tools/spartan_armor.py, armor_paint.SPARTAN): the crested helm with its T; the red cloth and the scrolled
pauldron over the gold cuirass; the studded pteruges; the gold greaves over brown soles.
    H L M D S V     the gold, pale to bronze brown: "light" to "void" ("O" is its void taken down)
    Y A a           the plume's lit tips and oranges: "plume_yellow", "gold_light", "gold_dark"
    R r q Q K       the plume's and the cloth's reds: "leather_light" to "leather_dark", and "leather_darkest", their
                    outline
    T t n N         the brown leather: "under_light", "under_mid", "under_dark", and "under_darkest", its outline
The designs the owner sent on 8 October 2026 (docs/features/armor-designs-8-october.md) likewise:
Berserker (tools/berserker_armor.py, armor_paint.BERSERKER): the horned cap with its red crest and toothed jaw frame;
the stepped red pauldrons over the keyed white breastplate; the thigh guards banded white and red; the grey boots with
red soles.
    H L M D S V     the white plate, white to dark grey: "light" to "void" ("O" is its void taken down)
    G g             the brightest whites: "gold_light", "gold_dark"
    R r q Q K       the reds: "leather_light" to "leather_dark", and "leather_darkest", their outline
    U u x X         the dark greys of the mail: "under_light" to "under_dark", and "under_darkest", their outline
Paladin and Templar (tools/crusader_armor.py, armor_paint.PALADIN and armor_paint.TEMPLAR): the great helm, the
Paladin's with its white H and purple sprig, the Templar's with its gable and crest; the spiralled pauldrons over the
shield and its cross; the spiralled tassets over the cloth; the banded greaves. One set of symbols for both:
    H L M D S V     the plate: white to slate on the Paladin, pale to near-black slate on the Templar
    G g             "gold_light", "gold_dark": the Paladin's blue gems; the Templar's near-whites
    C c q Q K       the cloth, purple or dark red: "leather_light" to "leather_dark", and "leather_darkest", its outline
    U u x X         the dark mail and belt: "under_light" to "under_dark", and "under_darkest", their outline
Sentinel (tools/sentinel_armor.py, armor_paint.SENTINEL): the gold bucket helm with its keyhole and the loop on its
crown; the great pauldron's bent plate, banded as the owner drew it, and the flat plates over the black coat, the
gorget and its pale strip, and the baldric; the black skirt with its gold plate on the left and dark one on the right; the gold boots.
    H L M D S V     the gold, cream to brown: "light" to "void" ("O" is its void taken down)
    G g             the palest cream and the deep brown: "gold_light", "gold_dark"
    C c q Q K       the browns: "leather_light" to "leather_dark", and "leather_darkest", their outline
    U u x X         the black cloth: "under_light" to "under_dark", and "under_darkest", its outline
Frost Knight (tools/frost_knight_armor.py, armor_paint.FROST_KNIGHT): the crown of ice over the white great helm, the
mane of frost round it and its face, the dark eye slit turning up at its ends and crossed by the ridge over the
grille; the frost on the right shoulder hanging down the arm, the navy pauldron on the left with its white curl and
rim, and the navy strap over the white cuirass; the white legs in stacked lames under the navy belt and its ice gem,
the knee bands; the white boots, a tuft of frost at each ankle.
    H L M D S V     the frosted white, white to slate ("O" is its void taken down)
    G g             the ice's palest and bright cyans: "gold_light", "gold_dark"
    C c q Q K       the navy: "leather_light" to "leather_dark", and "leather_darkest", its outline
    U u x X         the eye slit's and the vents' near-black: "under_light" to "under_dark", and "under_darkest",
                    its outline
    A I E F J j     the crown's ice: "ice_light", "ice", "ice_mid", "ice_dark", "ice_deep", and "~ice_deep", its
                    outline
Wight King (tools/wight_king_armor.py, armor_paint.WIGHT_KING): the antlers and the crown of icicles over the slate
helm and its black skull's face; the jagged right shoulder, the layered left one, the collar's cyan gem and the
crossed straps; the banded legs under the dark belt and its cyan buckle; the banded boots.
    H L M D S V     the slate, pale to deep ("O" is its void taken down)
    G g             the gems' cyans: "gold_light", "gold_dark"
    C c q Q K       the dark slate of the straps and belt: "leather_light" to "leather_dark", and "leather_darkest",
                    their outline
    U u x X         the face's black: "under_light" to "under_dark", and "under_darkest", its outline
    A I E F J j     the icicles' and antlers' ice: "ice_light" to "ice_deep", and "~ice_deep", its outline
Reaper (tools/reaper_armor.py, armor_paint.REAPER): the hood, its arched opening and the three bars in its black void;
the robe with its white clasp and the strap stepping down to its pouch, the grey plates and white V on its right
shoulder and the white cage on its left; the skirt of strips; the dark wrapped boots with grey wraps and a white band.
    H L M D S V     the bone-white of the plates, white to grey ("O" is its void taken down)
    G g             the pouch's reddish browns: "gold_light", "gold_dark"
    C c q Q K       the robe's browns: "leather_light" to "leather_dark", and "leather_darkest", its outline (and
                    the hood's)
    U u x X         the void's black: "under_light" to "under_dark", and "under_darkest", its outline
    a b e f         the hood's browns: "hood_light", "hood", "hood_mid", "hood_dark"
Banana (tools/banana_armor.py, armor_paint.BANANA): the tall yellow box with the hole for the face and the brown stem
on top; the yellow tube round the body, its ridges and speckles; the tube round the legs; its foot round the shins with
the brown end sticking out on the right.
    H L M D S V     the yellow, lit to shade ("O" is its void taken down)
    G g             the brown speckles: "gold_light", "gold_dark"
    C c q Q K       the stem's and the end's browns: "leather_light" to "leather_dark", and "leather_darkest", their
                    outline
    U u x X         inside the hole: "under_light" to "under_dark", and "under_darkest"
Scarab (tools/scarab_armor.py, armor_paint.SCARAB), the owner's own four icons: the nemes striped gold and lapis with
its lappets either side of the dark face; the cuirass with its striped shoulders and lapis bands; the kilt banded down
the legs; the banded boots, their toes turned out.
    H L M D S V     the gold, pale to brown ("O" is its void taken down)
    G g             its brightest and its deepest: "gold_light", "gold_dark"
    C c q Q K       the lapis: "leather_light" to "leather_dark", and "leather_darkest", its outline
    U u x X         the shadow under the headcloth: "under_light" to "under_dark", and "under_darkest", its outline
"""
import os

from PIL import Image

import armor_paint

FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), "armor_icons")
SIZE = 16
PIECES = ("helmet", "chestplate", "leggings", "boots")
METALS = {"steel": armor_paint.STEEL, "bronze": armor_paint.BRONZE}
# An item icon's outline sits at about this luma (the owner's outlines measure about 35 to 42; the item-icon rules in
# review on the arms-icons branch, docs/ITEM_ICONS.md, keep it at 45 or less). The knight's void (steel 68, bronze 56)
# is lighter, so the outline is the void carried down to it, keeping its hue.
OUTLINE_LUMA = 40

# symbol -> palette tone name, the same for both metals
TONES = {"D": "dark", "M": "mid", "L": "mid_light", "H": "light", "F": "gold_dark", "Y": "gold_light",
         "k": "leather_dark", "b": "leather_mid", "B": "leather_light", "w": "leather_darkest",
         "u": "under_dark", "U": "under_light"}
# symbol -> tone, per metal: the bronze's brass fittings are its own plate on the steel
FITTINGS = {"steel": {"R": "light", "r": "mid", "Q": "leather_mid"},
            "bronze": {"R": "gold_light", "r": "gold_dark", "Q": "gold_light"}}
# Sets with maps of their own (armor_icons/<set>/<piece>.txt): set -> (palette, {symbol: tone name}); "O" is always the
# metal's outline, its "void" taken down to OUTLINE_LUMA.
OWN = {"bloodthorn": (armor_paint.BLOODTHORN, {"H": "light", "L": "mid_light", "R": "gold_light", "M": "mid",
                                               "D": "dark", "S": "seam", "V": "void", "U": "under_light",
                                               "m": "under_mid", "u": "under_dark", "x": "under_darkest"}),
       "reforged_white_diamond": (armor_paint.WHITE_DIAMOND, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark",
                                                              "S": "seam", "V": "void", "U": "under_light",
                                                              "m": "under_mid", "u": "under_dark",
                                                              "x": "under_darkest"}),
       "hades": (armor_paint.HADES, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam", "V": "void",
                                     "B": "gold_light", "U": "under_light", "m": "under_mid", "u": "under_dark",
                                     "x": "under_darkest", "R": "leather_light", "k": "leather_mid_light",
                                     "r": "leather_mid", "w": "leather_darkest"}),
       "sunset_gem": (armor_paint.SUNSET_GEM, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam",
                                               "V": "void", "K": "gold_light", "Q": "gold_dark", "U": "under_light",
                                               "m": "under_mid", "u": "under_dark", "x": "under_darkest",
                                               "w": "leather_darkest"}),
       "pharaoh": (armor_paint.PHARAOH, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "A": "leather_light",
                                         "a": "leather_mid_light", "d": "leather_mid", "e": "leather_dark",
                                         "k": "leather_darkest", "T": "under_light", "t": "under_mid",
                                         "s": "under_dark", "n": "under_darkest", "R": "gold_light",
                                         "r": "gold_dark"}),
       "dread_knight": (armor_paint.DREAD_KNIGHT, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam",
                                                   "V": "void", "P": "gold_light", "U": "under_light", "u": "under_mid",
                                                   "x": "under_dark", "X": "under_darkest"}),
       "valkyrie": (armor_paint.VALKYRIE, {"W": "light", "C": "mid_light", "B": "mid", "G": "dark", "M": "seam",
                                           "V": "void", "Y": "gold_light", "y": "gold_mid", "o": "gold_dark",
                                           "g": "~gold_dark", "R": "leather_light", "r": "leather_mid_light",
                                           "q": "leather_mid", "Q": "leather_dark", "K": "leather_darkest",
                                           "T": "under_light", "t": "under_mid", "n": "under_dark", "N": "under_darkest",
                                           "F": "feather_pink", "f": "feather_lilac", "v": "feather_violet"}),
       "wayfarer": (armor_paint.WAYFARER, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam",
                                           "V": "void", "Y": "gold_light", "y": "gold_dark", "B": "leather_light",
                                           "b": "leather_mid_light", "m": "leather_mid", "d": "leather_dark",
                                           "k": "leather_darkest", "U": "under_light", "u": "under_mid",
                                           "x": "under_dark", "X": "under_darkest", "W": "feather_white",
                                           "I": "feather_ice", "F": "feather_pink", "s": "feather_steel",
                                           "g": "~feather_steel"}),
       "spartan": (armor_paint.SPARTAN, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam",
                                         "V": "void", "Y": "plume_yellow", "A": "gold_light", "a": "gold_dark",
                                         "R": "leather_light", "r": "leather_mid_light", "q": "leather_mid",
                                         "Q": "leather_dark", "K": "leather_darkest", "T": "under_light",
                                         "t": "under_mid", "n": "under_dark", "N": "under_darkest"}),
       "berserker": (armor_paint.BERSERKER, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam",
                                             "V": "void", "G": "gold_light", "g": "gold_dark", "R": "leather_light",
                                             "r": "leather_mid_light", "q": "leather_mid", "Q": "leather_dark",
                                             "K": "leather_darkest", "U": "under_light", "u": "under_mid",
                                             "x": "under_dark", "X": "under_darkest"}),
       "paladin": (armor_paint.PALADIN, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam",
                                         "V": "void", "G": "gold_light", "g": "gold_dark", "C": "leather_light",
                                         "c": "leather_mid_light", "q": "leather_mid", "Q": "leather_dark",
                                         "K": "leather_darkest", "U": "under_light", "u": "under_mid",
                                         "x": "under_dark", "X": "under_darkest"}),
       "templar": (armor_paint.TEMPLAR, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam",
                                         "V": "void", "G": "gold_light", "g": "gold_dark", "C": "leather_light",
                                         "c": "leather_mid_light", "q": "leather_mid", "Q": "leather_dark",
                                         "K": "leather_darkest", "U": "under_light", "u": "under_mid",
                                         "x": "under_dark", "X": "under_darkest"}),
       "sentinel": (armor_paint.SENTINEL, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam",
                                           "V": "void", "G": "gold_light", "g": "gold_dark", "C": "leather_light",
                                           "c": "leather_mid_light", "q": "leather_mid", "Q": "leather_dark",
                                           "K": "leather_darkest", "U": "under_light", "u": "under_mid",
                                           "x": "under_dark", "X": "under_darkest"}),
       "frost_knight": (armor_paint.FROST_KNIGHT, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark",
                                                   "S": "seam", "V": "void", "G": "gold_light", "g": "gold_dark",
                                                   "C": "leather_light", "c": "leather_mid_light", "q": "leather_mid",
                                                   "Q": "leather_dark", "K": "leather_darkest", "U": "under_light",
                                                   "u": "under_mid", "x": "under_dark", "X": "under_darkest",
                                                   "A": "ice_light", "I": "ice", "E": "ice_mid", "F": "ice_dark",
                                                   "J": "ice_deep", "j": "~ice_deep"}),
       "wight_king": (armor_paint.WIGHT_KING, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam",
                                               "V": "void", "G": "gold_light", "g": "gold_dark", "C": "leather_light",
                                               "c": "leather_mid_light", "q": "leather_mid", "Q": "leather_dark",
                                               "K": "leather_darkest", "U": "under_light", "u": "under_mid",
                                               "x": "under_dark", "X": "under_darkest", "A": "ice_light", "I": "ice",
                                               "E": "ice_mid", "F": "ice_dark", "J": "ice_deep", "j": "~ice_deep"}),
       "reaper": (armor_paint.REAPER, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam", "V": "void",
                                       "G": "gold_light", "g": "gold_dark", "C": "leather_light", "c": "leather_mid_light",
                                       "q": "leather_mid", "Q": "leather_dark", "K": "leather_darkest",
                                       "U": "under_light", "u": "under_mid", "x": "under_dark", "X": "under_darkest",
                                       "a": "hood_light", "b": "hood", "e": "hood_mid", "f": "hood_dark"}),
       "banana": (armor_paint.BANANA, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam", "V": "void",
                                       "G": "gold_light", "g": "gold_dark", "C": "leather_light", "c": "leather_mid_light",
                                       "q": "leather_mid", "Q": "leather_dark", "K": "leather_darkest",
                                       "U": "under_light", "u": "under_mid", "x": "under_dark", "X": "under_darkest"}),
       "scarab": (armor_paint.SCARAB, {"H": "light", "L": "mid_light", "M": "mid", "D": "dark", "S": "seam", "V": "void",
                                       "G": "gold_light", "g": "gold_dark", "C": "leather_light", "c": "leather_mid_light",
                                       "q": "leather_mid", "Q": "leather_dark", "K": "leather_darkest",
                                       "U": "under_light", "u": "under_mid", "x": "under_dark", "X": "under_darkest"})}


def luma(colour):
    r, g, b = colour[:3]
    return 0.299 * r + 0.587 * g + 0.114 * b


def deepen(colour, target=OUTLINE_LUMA):
    """`colour` scaled down to luma `target` (its hue kept)."""
    k = min(1.0, target / max(1.0, luma(colour)))
    return tuple(int(round(c * k)) for c in colour[:3])


def palette(metal):
    """Symbol -> RGBA for `metal` ("steel" or "bronze")."""
    tones = METALS[metal]
    colours = {symbol: tones[name] for symbol, name in {**TONES, **FITTINGS[metal]}.items()}
    colours["O"] = deepen(tones["void"])
    colours["g"] = deepen(tones["gold_dark"])
    return {symbol: tuple(colour) + (255,) for symbol, colour in colours.items()}


def own_palette(name):
    """Symbol -> RGBA for a set with maps of its own (OWN). A tone named "~<tone>" is that tone taken down to
    OUTLINE_LUMA, as "O" is: the outline of a part whose own darkest tone is too light for one (a gold, a feather)."""
    tones, symbols = OWN[name]
    colours = {symbol: deepen(tones[tone[1:]]) if tone.startswith("~") else tones[tone]
               for symbol, tone in symbols.items()}
    colours["O"] = deepen(tones["void"])
    return {symbol: tuple(colour) + (255,) for symbol, colour in colours.items()}


def path(piece, folder=None):
    """A map's file: armor_icons/<piece>.txt, or armor_icons/<folder>/<piece>.txt for a set with its own maps."""
    return os.path.join(FOLDER, folder, piece + ".txt") if folder else os.path.join(FOLDER, piece + ".txt")


def load(piece, folder=None):
    """The map's 16 rows (comment lines, starting with #, are skipped)."""
    with open(path(piece, folder), encoding="utf-8") as f:
        rows = [line.rstrip("\n") for line in f if line.strip() and not line.startswith("#")]
    if len(rows) != SIZE or any(len(row) != SIZE for row in rows):
        raise ValueError(f"{path(piece, folder)}: a map is {SIZE} rows of {SIZE} symbols, not {[len(r) for r in rows]}")
    return rows


def icon(metal, piece):
    """The 16x16 icon of `metal`_`piece` (an RGBA image): a knight metal from the shared maps, or a set of OWN from
    its own."""
    folder = metal if metal in OWN else None
    colours = own_palette(metal) if folder else palette(metal)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(load(piece, folder)):
        for x, symbol in enumerate(row):
            if symbol == ".":
                continue
            if symbol not in colours:
                raise ValueError(f"{path(piece, folder)}: unknown symbol {symbol!r} at ({x}, {y})")
            img.putpixel((x, y), colours[symbol])
    return img
