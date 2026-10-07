"""Halloween decorations, batch 15: the churchyard's ornaments (docs/features/churchyard-ornaments.md), drawn from the
owner's reference pictures of graveyard props. Models and textures: tools/decor15_data.py, sculpted on the toolkit in
tools/flora_art.py.

- The Gargoyle is a graveyard monument (tools/graveyard.py, pack 5): it weathers, waxes and takes an inscription.
- Bone Pile (agriculture/BonePileBlock): bones and skulls heaped on the ground, `layers` layers of `layer_pixels`.
- Ossuary Wall (agriculture/OssuaryWallBlock): a full block of stacked skulls and long bones, facing its placer.
- Giant Bone Hand (agriculture/GiantBoneHandBlock): a giant's skeletal hand two blocks tall out of grave earth, which
  clenches into a fist while powered.
- Witch's Lantern (vanilla's LanternBlock): a gothic iron lantern with violet glass, standing or hanging, light `light`.
Every recipe follows the agriculture feature switch.
"""

FEATURE = "agriculture"

BONE_PILE = {"block": "bone_pile", "display": "Bone Pile", "layers": 4, "layer_pixels": 3, "rattle_chance": 6}
OSSUARY_WALL = {"block": "ossuary_wall", "display": "Ossuary Wall"}
BONE_HAND = {"block": "giant_bone_hand", "display": "Giant Bone Hand"}
WITCHS_LANTERN = {"block": "witchs_lantern", "display": "Witch's Lantern", "light": 13}

SHAPED = [
    {"id": OSSUARY_WALL["block"], "pattern": ["BCB", "CBC", "BCB"], "key": {"B": "minecraft:bone_block", "C": "minecraft:cobbled_deepslate"},
     "result": OSSUARY_WALL["block"], "count": 8, "category": "building"},
    {"id": BONE_HAND["block"], "pattern": ["B B", "BBB", " D "], "key": {"B": "minecraft:bone_block", "D": "minecraft:coarse_dirt"},
     "result": BONE_HAND["block"], "count": 1, "category": "building"},
    {"id": WITCHS_LANTERN["block"], "pattern": ["NNN", "NTN", "NAN"], "key": {"N": "minecraft:iron_nugget", "T": "minecraft:torch",
                                                                              "A": "minecraft:amethyst_shard"},
     "result": WITCHS_LANTERN["block"], "count": 1, "category": "building"},
]
SHAPELESS = [
    {"id": BONE_PILE["block"], "inputs": ["minecraft:bone", "minecraft:bone", "minecraft:bone", "minecraft:bone_meal"],
     "result": BONE_PILE["block"], "count": 1, "category": "building"},
]


def blocks():
    return [BONE_PILE["block"], OSSUARY_WALL["block"], BONE_HAND["block"], WITCHS_LANTERN["block"]]


def items():
    return blocks()


def names():
    return {info["block"]: info["display"] for info in (BONE_PILE, OSSUARY_WALL, BONE_HAND, WITCHS_LANTERN)}
