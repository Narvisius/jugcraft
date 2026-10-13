"""Wayfaring, the trinkets slice, part 1 (docs/features/arcane-concordance-trinkets.md): the owner's belt, boots and
charms, worn in Trinkets slots once Relic Lore is understood.

The owner made these from other Minecraft mods (a relics mod and a reliquary mod) as a base and redid them; their art is
imported as supplied by tools/owner_art.py from OWNER_FILES below, under Jugcraft ids. Each item keeps the owner's
English name and the kind of effect the owner's own text gives it; the code, numbers and tooltips are Jugcraft's own:

- Leather Belt (worn in the belt slot): one more Charm slot.
- Angelic Feather (charm): a fall's harm is taken from your food instead of your health while you have food, and you jump
  a little higher.
- Kraken Shell (charm): the same for drowning.
- Infernal Claws (charm): the same for fire (not lava).
- Angelheart Vial (charm): a blow that would kill you leaves you on a little health and heals you a while; the vial is
  used up.
- Phoenix Down (charm): the same, at full health and with a moment's Fire Resistance; it becomes an Angelic Feather in the
  same slot. Worn, it gives the feather's effects.
- Amphibian Boot (feet): you swim faster, and your air lasts about twice as long.
- Ice Breaker (feet): a little knockback resistance, and a fall that hurts you sends a wave through the ground that
  harms, throws back and slows the hostile creatures round you, through the shared effect boundary.

Nothing here ticks, holds Focus or charge, or needs a mixin: the attributes are Trinkets modifiers, and the rest answers
three Fabric damage events (Java: concordance/trinket/Wayfaring.java). tools/concordance.py merges these tables into its
own; numbers the Java repeats are checked by tools/check_mod_data.py.

Part 1b draws the Leather Belt and the Amphibian Boot on the wearer. The owner drew a worn sheet for each (a box-UV net,
supplied without its geometry); the sheets are imported as supplied (OWNER_FILES), WORN below fits boxes to them, and
write_worn writes their block models and Trinkets' render definitions (assets/jugcraft/trinkets/<item>.json), which
Trinkets' data-driven renderer draws in third person. The Ice Breaker has no worn sheet, so it is not drawn. Each
definition wraps its models in Jugcraft's own render element, jugcraft:unless_covered (client:
trinket/UnlessCoveredTrinketElement.java), so the belt is hidden under a chestplate or leggings and the boots under boots
(WORN_COVERED_BY), and both on a wearer whose "Show my worn trinkets" setting is off (their client tells the server, which
tells everyone who sees them: concordance/trinket/WornDisplay.java).
"""
import json
from pathlib import Path

MOD = "jugcraft"
# The owner's library (art/owner-library): read here, never written.
LIBRARY = Path(__file__).resolve().parents[1] / "art" / "owner-library" / "originals"
MAGIC = LIBRARY / "Magic"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# Charm slots a player has, and how many a worn Leather Belt adds (the owner's own: a charm slot, one more per belt).
CHARM_SLOTS = 1
BELT_CHARM_SLOTS = 1
# The owner's feet slot holds two.
FEET_SLOTS = 2
# Exhaustion each point of harm a charm takes from food instead of health costs (vanilla: 4 exhaustion is one point of
# food): half a food point a point of harm, before armour. Whole points come straight off the food bar, the rest as
# exhaustion (which saturation pays first). A 10-block fall's 7 harm costs 3.5 points. A blow the bar cannot pay for
# lands in full. A blow the charm takes never starts vanilla's hurt cooldown, so the charm keeps its own: within
# ABSORB_COOLDOWN_TICKS of a blow it took, a blow costs only what it is bigger by (fire and hot floors try to hurt every
# tick; standing in fire costs about a point a second, as often as it would land).
ABSORB_EXHAUSTION = 2.0
ABSORB_COOLDOWN_TICKS = 10
# Attribute modifiers (added values) while worn where Trinkets applies effects. Jump strength: the feather's jump from
# about 1.25 blocks to 1.42, still short of a fence. Oxygen bonus: one more chance in two of keeping a breath.
FEATHER_JUMP = 0.03
AMPHIBIAN_SWIM = 0.5
AMPHIBIAN_OXYGEN = 1.0
ICE_BREAKER_KNOCKBACK = 0.1
# The Ice Breaker's wave: its radius in blocks grows with the fall's harm, up to a ceiling; it reaches the nearest hostile
# creatures up to a count, harms them (health points), throws them back (tenths of a block a tick) and slows them
# (Slowness I, ticks).
WAVE_RADIUS = 3.0
WAVE_RADIUS_PER_HARM = 0.25
WAVE_MAX_RADIUS = 6.0
WAVE_TARGETS = 12
WAVE_DAMAGE = 2
WAVE_PUSH = 6
WAVE_SLOW_TICKS = 40
# Death saves: the vial leaves you on this much health with Regeneration II; the Phoenix Down at full health, with
# Regeneration II and Fire Resistance.
VIAL_HEALTH = 4.0
VIAL_REGENERATION_TICKS = 100
PHOENIX_REGENERATION_TICKS = 200
PHOENIX_FIRE_RESISTANCE_TICKS = 200

# The slots (data/trinkets/slots/<group>/<slot>.json, given to players by data/trinkets/entities/jugcraft_wayfaring.json).
# The belt slot is Trinkets' own, with its own icon and name. The charm and feet slots are Jugcraft's, drawn with the
# owner's slot icons and named as the owner named them; a cosmetic copy is never made of them (Trinkets drops and
# restores cosmetic stacks badly on death), so a charm or boot is either worn or not.
BELT_SLOT = "legs/belt"
CHARM_SLOT = "legs/charm"
FEET_SLOT = "feet/boots"
TRINKET_SLOTS = {
    CHARM_SLOT: {"name": "Charm", "amount": CHARM_SLOTS, "icon": "charm"},
    FEET_SLOT: {"name": "Feet", "amount": FEET_SLOTS, "icon": "feet",
                "source": "data/jymbelics/curios/slots/feet.json"},
}
GRANTED_SLOTS = [BELT_SLOT, CHARM_SLOT, FEET_SLOT]

# Each item's slot, owner namespace and owner id (their file names), and its effects.
TRINKETS = {
    "leather_belt": BELT_SLOT,
    "angelic_feather": CHARM_SLOT, "kraken_shell": CHARM_SLOT, "infernal_claws": CHARM_SLOT,
    "angelheart_vial": CHARM_SLOT, "phoenix_down": CHARM_SLOT,
    "amphibian_boot": FEET_SLOT, "ice_breaker": FEET_SLOT,
}
OWNER_ITEMS = {
    "leather_belt": ("jymbelics", "leather_belt"),
    "angelic_feather": ("jymbaquary", "angelic_feather"),
    "kraken_shell": ("jymbaquary", "kraken_shell"),
    "infernal_claws": ("jymbaquary", "infernal_claws"),
    "angelheart_vial": ("jymbaquary", "angelheart_vial"),
    "phoenix_down": ("jymbaquary", "phoenix_down"),
    "amphibian_boot": ("jymbelics", "amphibian_boot"),
    "ice_breaker": ("jymbelics", "ice_breaker"),
}
# The owner's icons that are animated strips: their .png.mcmeta sidecars are imported with them.
ANIMATED = ["leather_belt", "amphibian_boot", "ice_breaker"]

# Attribute modifiers each item gives worn (attribute id -> added value). The Phoenix Down gives the feather's, under the
# feather's ids, so the two never add up; nor do two of anything (Java: WornTrinketItem names a modifier by its kind, not
# its slot).
SLOT_COUNT = "trinkets:slot_count/" + CHARM_SLOT
MODIFIERS = {
    "leather_belt": {SLOT_COUNT: BELT_CHARM_SLOTS},
    "angelic_feather": {"minecraft:jump_strength": FEATHER_JUMP},
    "phoenix_down": {"minecraft:jump_strength": FEATHER_JUMP},
    "amphibian_boot": {"minecraft:water_movement_efficiency": AMPHIBIAN_SWIM, "minecraft:oxygen_bonus": AMPHIBIAN_OXYGEN},
    "ice_breaker": {"minecraft:knockback_resistance": ICE_BREAKER_KNOCKBACK},
}
# The kind each item's modifiers are named after (Java: WornTrinketItem's kind).
KIND = {item: "angelic_feather" if item == "phoenix_down" else item for item in TRINKETS}
# What harm each charm takes from food instead of health (Java: Wayfaring.allowDamage): vanilla damage types.
ABSORBS = {
    "angelic_feather": ["minecraft:fall"],
    "phoenix_down": ["minecraft:fall"],
    "kraken_shell": ["minecraft:drown"],
    "infernal_claws": ["minecraft:in_fire", "minecraft:on_fire", "minecraft:campfire", "minecraft:hot_floor"],
}

_FOOD = f"while your food bar can pay, {ABSORB_EXHAUSTION / 4:g} food a point"
ITEMS = {
    "leather_belt": {
        "name": "Leather Belt",
        "tooltip": "Worn in the Belt slot: one more Charm slot. It cannot be taken off while that slot holds a charm."},
    "angelic_feather": {
        "name": "Angelic Feather",
        "tooltip": f"Worn as a charm: a fall's harm comes from your food instead of your health {_FOOD}, and you jump a "
                   "little higher."},
    "kraken_shell": {
        "name": "Kraken Shell",
        "tooltip": f"Worn as a charm: drowning's harm comes from your food instead of your health {_FOOD}."},
    "infernal_claws": {
        "name": "Infernal Claws",
        "tooltip": f"Worn as a charm: fire's harm (not lava's) comes from your food instead of your health {_FOOD}."},
    "angelheart_vial": {
        "name": "Angelheart Vial",
        "tooltip": f"Worn as a charm: a blow that would kill you leaves you on {VIAL_HEALTH / 2:g} hearts, healing for "
                   f"{VIAL_REGENERATION_TICKS // 20} seconds. The vial is used up."},
    "phoenix_down": {
        "name": "Phoenix Down",
        "tooltip": "Worn as a charm: a blow that would kill you leaves you at full health, healing and proof against "
                   f"fire for {PHOENIX_REGENERATION_TICKS // 20} seconds, and it becomes an Angelic Feather. Until then it "
                   "is a feather as well."},
    "amphibian_boot": {
        "name": "Amphibian Boot",
        "tooltip": "Worn on your feet: you swim faster, and your air lasts about twice as long."},
    "ice_breaker": {
        "name": "Ice Breaker",
        "tooltip": "Worn on your feet: a little knockback resistance, and a fall that hurts you sends a wave through the "
                   f"ground that harms, throws back and slows hostile creatures up to {WAVE_MAX_RADIUS:g} blocks away."},
}
BLOCKS = {}

# Shaped crafting from Overworld materials. The owner's own recipes use the base mod's ingredients (mob drops and
# essences it adds), so they are not imported, except the Phoenix Down's, which is made only of this slice's items:
# three Angelheart Vials and an Angelic Feather. A death save costs a golden apple.
RECIPES = {
    "leather_belt": {"pattern": ["LIL", "L L"], "key": {"L": "minecraft:leather", "I": "minecraft:iron_ingot"}},
    "angelic_feather": {"pattern": [" A ", "PFP", " G "],
                        "key": {"A": "minecraft:amethyst_shard", "P": "minecraft:phantom_membrane", "F": "minecraft:feather",
                                "G": "minecraft:gold_ingot"}},
    "kraken_shell": {"pattern": ["PIP", "PNP"],
                     "key": {"P": "minecraft:prismarine_shard", "I": "minecraft:ink_sac", "N": "minecraft:nautilus_shell"}},
    "infernal_claws": {"pattern": ["M M", "LGL"],
                       "key": {"M": "minecraft:magma_block", "L": "minecraft:leather", "G": "minecraft:gold_ingot"}},
    "angelheart_vial": {"pattern": [" A ", "PHP", " B "],
                        "key": {"A": "minecraft:amethyst_shard", "P": "minecraft:phantom_membrane",
                                "H": "minecraft:golden_apple", "B": "minecraft:glass_bottle"}},
    "amphibian_boot": {"pattern": ["L L", "PKP"],
                       "key": {"L": "minecraft:leather", "P": "minecraft:prismarine_shard", "K": "minecraft:dried_kelp"}},
    "ice_breaker": {"pattern": ["ILI", "I I", "F F"],
                    "key": {"I": "minecraft:iron_ingot", "L": "minecraft:leather", "F": "minecraft:flint"}},
}
# The owner's shapeless Phoenix Down recipe (data/jymbaquary/recipe/phoenix_down.json), its ids renamed.
SHAPELESS = {"phoenix_down": [rid("angelheart_vial")] * 3 + [rid("angelic_feather")]}

# Part 1b: what is drawn on the wearer. Each worn sheet (assets/jymbelics/textures/models/items/<owner id>.png, imported
# as textures/item/<item>_worn.png) is a box-UV net, laid out as vanilla's ModelPart cube lays one out; the owner supplied
# no geometry, so each box below is fitted to its sheet: (name, its net's corner on the sheet in texels, its size in
# pixels across, up and front to back, its low corner on the model part in the player model's pixels: x to the wearer's
# left, y down from the part's top, z to their back). A size of 0 across is a plane drawn on both sides. The belt sheet's
# one faint texel (28, 6) lies in no net, so it is never drawn. "sheet" is the sheet's size; "parts" the model drawn on
# each model part (Trinkets' model_part names: the player model's).
WORN = {
    # On the body: the strap round the torso's bottom rows, half a pixel out, its underside armor_models.SKIN_GAP inside
    # the torso once grown (so it is never near the skin's outer layer); the buckle on its front, half a pixel past the
    # strap above and below.
    "leather_belt": {"sheet": 32, "parts": {"body": "leather_belt_worn"}, "boxes": [
        ("band", (0, 0), (9, 2, 5), (-4.5, 9.7, -2.5)),
        ("buckle", (0, 7), (4, 3, 1), (-2, 9.2, -3.5)),
    ]},
    # On each leg: the boot a pixel round the foot, the toe cap in front of it, the cuff (an open ring: the sheet leaves its
    # top and bottom empty) on top, and the fin on the heel, its narrow end on the boot and its three fronds pointing back.
    "amphibian_boot": {"sheet": 64, "parts": {"right_leg": "amphibian_boot_worn_right", "left_leg": "amphibian_boot_worn_left"},
                       "boxes": [
        ("boot", (0, 9), (6, 7, 6), (-3, 5, -3)),
        ("toe", (18, 9), (6, 3, 2), (-3, 9, -5)),
        ("cuff", (0, 1), (6, 1, 6), (-3, 4, -3)),
        ("fin", (0, -1), (0, 5, 3), (0, 5, 3)),
    ]},
}
# The model parts whose model is drawn mirrored (vanilla's mirror flag), so the two boots are a pair. The sheet is the
# left boot: the owner's icon shows the boot from its outer side with the toe to the left (a left boot), and its fronds
# are the bright half of the fin's net, the half the sheet puts on the boot's left (+x) side. So the right boot is the
# mirror image, its bright side out as well (vanilla's humanoid model mirrors the other leg).
WORN_MIRRORED = {"right_leg"}
# How far each box stands beyond its fitted size (pixels), as vanilla's CubeDeformation grows a box: by
# armor_models.SKIN_GAP, so its sides stand clear of vanilla leggings (0.5 out, where the fitted belt lies) and boots
# (1.0 out, where the fitted boot lies). A box set on another's face moves out with that face instead of growing into
# it, and a plane only moves, so no two faces of a model facing the same way share a plane. A seated box's inner face
# lies back to back on its seat, which never shows: the buckle's back on the strap, inside the closed buckle; the toe's
# back on the boot's front and the cuff's bottom on the boot's top, both clear on the sheet. The left leg's model stands
# a step further out, so where the two boots overlap between the legs one is in front of the other.
WORN_GROW = 0.15
WORN_LEFT_STEP = 0.1
# Faces that keep their fitted plane instead of growing, named as on the part ("top" is up): the toe's top meets the line
# where the boot's front ends (the sheet leaves the front clear below it, for the toe), so a grown toe would cover the
# bottom of the row above. On the left leg such a face sits a step in, so the two toes' tops never share a plane.
WORN_KEPT = {("amphibian_boot", "toe"): ("top",)}
# How far below its leg each boot's sole lies (pixels): on the right between the skin's underside (0) and its outer
# layer's (the pants, 0.25), on the left past the pants; each at least model_writer.COPLANAR_NUDGE from the skin, the
# pants, vanilla leggings (0.5) and the other sole, so none flickers, while burying as little of the owner's bottom row
# under the ground as those clearances allow (both past the pants by armor_models.SKIN_GAP would bury a third to a half
# of it).
WORN_SOLE = {"right_leg": 0.15, "left_leg": 0.35}
# A plane (the fin) is drawn as its two sides, each lifted this far (px) along its own normal, as DecorDraw.TWO_SIDED_LIFT
# lifts them: 26.3's cutout types may not cull back faces (docs/ART_DIRECTION.md, two-sided planes), and a reversed twin
# on one plane would fight. The fin's two halves are one silhouette seen from either side, so the nearer always hides
# the other.
WORN_PLANE_LIFT = 0.05
# Where each model is anchored on its part (Trinkets' offset, in halves of the part's size from its middle): the bottom of
# the torso and the sole of each leg, both 12 pixels below the part's top. A point (x, y, z) on the part is then
# (8 + x, 20 - y, 8 - z) in the model: Trinkets draws it with up up, south to the wearer's front and east to their left.
WORN_OFFSET = [0, -1, 0]
WORN_ANCHOR_Y = 12
# The armour slots whose armour hides each worn item (the owner's choice, 10 October 2026: "belt and boots should hide under
# armor"): the belt lies on the waist, which vanilla's chestplates and leggings both cover (a chestplate would hide the
# strap and leave the buckle standing out); the boots on the feet, which boots cover. Leggings over the boots' tops do not
# hide them. Any piece for those slots counts, so a few of Jugcraft's 3D pieces that stop short of the waist hide the belt
# too (the feature record's part 1c limits).
WORN_COVERED_BY = {"leather_belt": ["chest", "legs"], "amphibian_boot": ["feet"]}
# Jugcraft's render element that draws its own elements only while those slots are bare and the player's setting is on.
WORN_ELEMENT = rid("unless_covered")
# The client setting's words (ConcordanceClientOptions, ConcordanceSettingsScreen). Each player's choice is their own
# and everyone who sees them sees it (WornDisplay).
CLIENT = {
    "screen.jugcraft.concordance.config.worn_trinkets": "Show my worn trinkets",
    "screen.jugcraft.concordance.config.worn_trinkets.tooltip": "Draw your Leather Belt and Amphibian Boots on you. "
        "Everyone who sees you sees your choice, and each player chooses for themselves. Armour worn over them hides them.",
}

# Every owner file this slice uses, copied as supplied by tools/owner_art.py: runtime path under assets/jugcraft -> path
# under originals/Magic (or, for OWNER_BLOCKS_FILES, under originals/Blocks). The only changes are the names and, for an
# animation sidecar, its line ends (LF, as Git stores the mod's text).
OWNER_FILES = {
    **{f"textures/item/{item}.png": f"assets/{ns}/textures/item/{owner}.png" for item, (ns, owner) in OWNER_ITEMS.items()},
    **{f"textures/item/{item}.png.mcmeta": f"assets/{OWNER_ITEMS[item][0]}/textures/item/{OWNER_ITEMS[item][1]}.png.mcmeta"
       for item in ANIMATED},
    f"textures/gui/sprites/container/slots/{TRINKET_SLOTS[FEET_SLOT]['icon']}.png": "assets/jymbelics/textures/slot/empty_feet_slot.png",
    # Part 1b: the worn sheets, in the items atlas (Trinkets bakes its models from the block and item atlases).
    **{f"textures/item/{item}_worn.png": f"assets/{OWNER_ITEMS[item][0]}/textures/models/items/{OWNER_ITEMS[item][1]}.png"
       for item in WORN},
}
OWNER_BLOCKS_FILES = {
    f"textures/gui/sprites/container/slots/{TRINKET_SLOTS[CHARM_SLOT]['icon']}.png": "Trinket Type Mod/slot/empty_charm_slot.png",
}
OWNER_LANGS = {"jymbelics": "assets/jymbelics/lang/en_us.json", "jymbaquary": "assets/jymbaquary/lang/en_us.json"}


def owner_name(item):
    """The owner's English name for `item`, from their language file (only the names are taken from it)."""
    ns, owner = OWNER_ITEMS[item]
    lang = json.loads((MAGIC / OWNER_LANGS[ns]).read_text(encoding="utf-8-sig"))
    return lang.get(f"item.{ns}.{owner}")


def owner_model(item):
    """The owner's item model for `item`, with its texture renamed to the Jugcraft copy. It must be a plain generated model
    (the parent written either way, an empty overrides list allowed); anything else stops the generator, since a changed
    library file is a decision, not a merge."""
    ns, owner = OWNER_ITEMS[item]
    model = json.loads((MAGIC / "assets" / ns / "models" / "item" / f"{owner}.json").read_text(encoding="utf-8-sig"))
    plain = (model.get("parent") in ("item/generated", "minecraft:item/generated")
             and model.get("textures") == {"layer0": f"{ns}:item/{owner}"}
             and set(model) <= {"parent", "textures", "overrides"} and not model.get("overrides"))
    if not plain:
        raise SystemExit(f"tools/concordance_trinkets.py: the owner's model for {ns}:{owner} is not the plain generated model expected")
    return {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}}


def worn_net(u, v, w, h, d):
    """The faces of a box-UV net with its corner at (u, v), for a box w x h x d (texels), as vanilla's ModelPart cube lays it
    out: each face's [u0, v0, u1, v1] on the sheet, named as the block model face Trinkets turns toward the same side of the
    wearer (south to their front, east to their left) and read the same way round (the bottom from its back edge, as
    vanilla reads it). A plane keeps only its two sides."""
    faces = {"north": [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], "east": [u + d + w, v + d, u + 2 * d + w, v + d + h],
             "south": [u + d, v + d, u + d + w, v + d + h], "west": [u, v + d, u + d, v + d + h],
             "up": [u + d, v, u + d + w, v + d], "down": [u + d + w, v + d, u + d + 2 * w, v]}
    return {face: rect for face, rect in faces.items() if rect[0] != rect[2] and rect[1] != rect[3]}


def mirror_net(net):
    """A net drawn mirrored, as vanilla's mirror flag draws it: the two sides change places and every face reads right to
    left."""
    return {{"east": "west", "west": "east"}.get(face, face): [rect[2], rect[1], rect[0], rect[3]] for face, rect in net.items()}


# Each face of a box on the part: its axis and whether it is the box's low (-1) or high (+1) side.
_PART_FACES = {"right": (0, -1), "left": (0, 1), "top": (1, -1), "bottom": (1, 1), "front": (2, -1), "back": (2, 1)}


def worn_boxes(item, part):
    """(name, net, low corner, high corner) of each of `item`'s boxes on `part`, in the player model's pixels. Each grows by
    WORN_GROW on every side (on the left leg a WORN_LEFT_STEP more), as vanilla's CubeDeformation grows a box, except: a box
    set on an earlier box's face (in the fitted layout) keeps its size across that face and moves out with it; a plane only
    moves with its seat; a WORN_KEPT face stays on its fitted plane (on the left leg a step in); and a face on a leg's
    bottom (the sole) lies WORN_SOLE below it."""
    left = part == "left_leg"
    grow = WORN_GROW + (WORN_LEFT_STEP if left else 0.0)
    out, fitted = [], []
    for name, (u, v), size, corner in WORN[item]["boxes"]:
        lo, hi = [float(c) for c in corner], [float(corner[k] + size[k]) for k in range(3)]
        seat = {}
        for index, (flo, fhi) in enumerate(fitted):
            for k in range(3):
                if all(lo[j] < fhi[j] and flo[j] < hi[j] for j in range(3) if j != k):
                    if lo[k] == fhi[k]:
                        seat[k] = (index, 1)
                    elif hi[k] == flo[k]:
                        seat[k] = (index, -1)
        fitted.append((list(lo), list(hi)))
        on_sole = part in WORN_SOLE and hi[1] == WORN_ANCHOR_Y
        for k in range(3):
            if k in seat:
                index, side = seat[k]
                face = out[index][3][k] if side > 0 else out[index][2][k]
                lo[k], hi[k] = (face, face + size[k]) if side > 0 else (face - size[k], face)
            elif 0 not in size:
                lo[k], hi[k] = lo[k] - grow, hi[k] + grow
        for face in WORN_KEPT.get((item, name), ()):
            k, side = _PART_FACES[face]
            step = WORN_LEFT_STEP if left else 0.0
            if side < 0:
                lo[k] = corner[k] + step
            else:
                hi[k] = corner[k] + size[k] - step
        if on_sole:
            hi[1] = WORN_ANCHOR_Y + WORN_SOLE[part]
        out.append((name, worn_net(u, v, *size), lo, hi))
    return out


def _num(value):
    value = round(value, 4)
    return int(value) if value == int(value) else value


# Each block model face's axis and the way it faces along it.
_FACING = {"east": (0, 1), "west": (0, -1), "up": (1, 1), "down": (1, -1), "south": (2, 1), "north": (2, -1)}


def worn_model(item, part):
    """The block model Trinkets draws on `part`: `item`'s boxes there (worn_boxes; their nets mirrored on a WORN_MIRRORED
    part) at (8 + x, 20 - y, 8 - z) for a point (x, y, z) on the part, every face reading its net's rectangle of the owner's
    sheet (in sixteenths of the sheet). Faces the owner left empty are drawn too, as vanilla draws a whole box: they show
    nothing. A plane is two one-sided elements, each lifted WORN_PLANE_LIFT along its face's normal."""
    scale = 16 / WORN[item]["sheet"]
    sheet = rid(f"item/{item}_worn")
    mirrored = part in WORN_MIRRORED
    elements = []
    for _name, net, lo, hi in worn_boxes(item, part):
        top = 8 + WORN_ANCHOR_Y
        frm, to = [8 + lo[0], top - hi[1], 8 - hi[2]], [8 + hi[0], top - lo[1], 8 - lo[2]]
        faces = {face: {"uv": [_num(c * scale) for c in rect], "texture": "#sheet"}
                 for face, rect in (mirror_net(net) if mirrored else net).items()}
        sides = [{face: spec} for face, spec in faces.items()] if any(frm[k] == to[k] for k in range(3)) else [faces]
        for side in sides:
            lift = [0.0, 0.0, 0.0]
            if len(sides) > 1:
                axis, sign = _FACING[next(iter(side))]
                lift[axis] = sign * WORN_PLANE_LIFT
            elements.append({"from": [_num(frm[k] + lift[k]) for k in range(3)],
                             "to": [_num(to[k] + lift[k]) for k in range(3)], "faces": side})
    return {"textures": {"sheet": sheet, "particle": sheet}, "elements": elements}


def worn_models():
    """{model name: (item, part)} for every worn model."""
    return {model: (item, part) for item, info in WORN.items() for part, model in info["parts"].items()}


def worn_render(item):
    """Trinkets' render definition for `item`: its model on each part, anchored at WORN_OFFSET (one block-model unit is
    one pixel of the player model), all inside one WORN_ELEMENT that hides them under WORN_COVERED_BY's armour."""
    return {"target": rid(item), "render": [{"type": WORN_ELEMENT, "armour": WORN_COVERED_BY.get(item, []), "then": [
        {"type": "minecraft:model", "model_part": part, "offset": WORN_OFFSET, "model": rid(f"item/{model}")}
        for part, model in WORN[item]["parts"].items()]}]}


def write_worn(write, assets):
    """Part 1b's files: each worn model, and the render definition that attaches it."""
    for model, (item, part) in worn_models().items():
        write(assets / "models" / "item" / f"{model}.json", worn_model(item, part))
    for item in WORN:
        write(assets / "trinkets" / f"{item}.json", worn_render(item))


def lang_entries(lang):
    for slot, info in TRINKET_SLOTS.items():
        lang[f"trinkets.slot.{slot.replace('/', '.')}"] = info["name"]
    lang.update(CLIENT)


def codex():
    return {
        ("relics", "wayfaring"): {
            "name": "Belts, Charms and Boots", "x": 2, "y": 0, "icon": rid("leather_belt"), "condition": None,
            "description": "Worn things for the road",
            "pages": [
                ("text", "Belts, Charms and Boots",
                 "Once you understand **Relic Lore**, you can wear these: a belt in the Belt slot, a charm in a Charm "
                 f"slot (a Leather Belt gives a second) and up to {FEET_SLOTS} on your feet. They hold no Focus or charge "
                 "and need no pylon. Two of a kind never add up; a second vial only waits its turn. The Leather Belt and "
                 "the Amphibian Boots show on you unless armour covers them, and the Concordance settings can hide them "
                 "from everyone."),
                ("crafting_recipe", "Leather Belt",
                 "One more Charm slot. Take the second charm off before the belt.", rid("leather_belt")),
                ("crafting_recipe", "Angelic Feather",
                 f"A fall's harm comes from your food instead of your health, at {ABSORB_EXHAUSTION / 4:g} food a point "
                 "(blows in quick succession are paid once), and you jump a little higher. A fall your food bar cannot "
                 "pay for hurts as ever.",
                 rid("angelic_feather")),
                ("crafting_recipe", "Kraken Shell", "The same for drowning.", rid("kraken_shell")),
                ("crafting_recipe", "Infernal Claws", "The same for fire and hot floors, not lava.", rid("infernal_claws")),
                ("crafting_recipe", "Angelheart Vial",
                 f"A blow that would kill you leaves you on {VIAL_HEALTH / 2:g} hearts, healing for "
                 f"{VIAL_REGENERATION_TICKS // 20} seconds, and the vial is used up. It does not answer a fall into the "
                 "void, a held totem (the totem answers), or a death in a dream.", rid("angelheart_vial")),
                ("text", "Phoenix Down",
                 "Three Angelheart Vials and an Angelic Feather, crafted together. A blow that would kill you leaves you "
                 f"at full health, healing and proof against fire for {PHOENIX_REGENERATION_TICKS // 20} seconds, and "
                 "the down becomes an Angelic Feather where it was worn. Until then it is a feather as well. A vial "
                 "worn beside it answers first."),
                ("crafting_recipe", "Phoenix Down", "Three Angelheart Vials and an Angelic Feather.", rid("phoenix_down")),
                ("crafting_recipe", "Amphibian Boot", "You swim faster, and your air lasts about twice as long.",
                 rid("amphibian_boot")),
                ("crafting_recipe", "Ice Breaker",
                 f"A little knockback resistance. A fall that hurts you sends a wave through the ground: up to "
                 f"{WAVE_TARGETS} hostile creatures within {WAVE_RADIUS:g} blocks (more for a harder fall, at most "
                 f"{WAVE_MAX_RADIUS:g}) are harmed, thrown back and slowed. A few great foes, such as the Warden, take the "
                 "harm but are neither thrown nor slowed.",
                 rid("ice_breaker")),
            ],
        },
    }


def tags(tags):
    pass


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    for item in ITEMS:
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
        write(assets / "models" / "item" / f"{item}.json", owner_model(item))
    for item, recipe in RECIPES.items():
        write(data / "recipe" / f"{item}.json", {
            "fabric:load_conditions": condition("concordance"), "type": "minecraft:crafting_shaped", "category": "equipment",
            "pattern": recipe["pattern"], "key": recipe["key"], "result": {"id": rid(item), "count": 1}})
    for item, ingredients in SHAPELESS.items():
        write(data / "recipe" / f"{item}.json", {
            "fabric:load_conditions": condition("concordance"), "type": "minecraft:crafting_shapeless", "category": "equipment",
            "ingredients": ingredients, "result": {"id": rid(item), "count": 1}})
    # The two new slots (the belt slot is Trinkets' own), the items each slot takes, and players' having all three.
    trinkets = data.parent / "trinkets"
    for slot, info in TRINKET_SLOTS.items():
        group, name = slot.split("/")
        write(trinkets / "slots" / group / f"{name}.json", {"icon": rid(f"container/slots/{info['icon']}"), "amount": info["amount"],
                                                            "validator_predicates": ["trinkets:default"], "cosmetic_slots": False})
    for slot in GRANTED_SLOTS:
        group, name = slot.split("/")
        write(trinkets / "tags" / "item" / group / f"{name}.json",
              {"replace": False, "values": [rid(item) for item, worn in TRINKETS.items() if worn == slot]})
    write(trinkets / "entities" / f"{MOD}_wayfaring.json", {"entities": ["player"], "slots": sorted(GRANTED_SLOTS)})
    write_worn(write, assets)


def write_data(write, data):
    pass
