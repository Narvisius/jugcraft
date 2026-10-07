"""Halloween decorations, batch 16: the haunted house's props (docs/features/haunted-house-props.md), drawn from the
owner's reference pictures. Models and textures: tools/decor16_data.py, sculpted on the toolkit in tools/flora_art.py.

- Flying Eyeball (agriculture/FlyingEyeballBlock, client/FlyingEyeballRenderer): a bloodshot eye on red bat wings that
  hovers in its block, bobbing `hover_pixels` up and down every `bob_ticks`, its wings beating every `flap_ticks`, and
  turns (at most `turn_speed` degrees a tick) to stare at the nearest player within `watch_range` blocks.
- Pillar Candles, ivory and black (agriculture/PillarCandleBlock, a vanilla CandleBlock): one to four church candles of
  different heights in a cluster, dripping wax. `CANDLES` gives each candle's centre, width and height in pixels; the
  flame stands `flame_above` pixels over its top.
- Spider Web (vanilla's MultifaceBlock): a web strung over any face of a block, walls and ceilings alike.
- Monster's Head (agriculture/MonsterHeadBlock): a stitched monster's great green head with bolts in its neck. Give it
  a redstone signal and it wakes: its jaw drops, its eyes glow (light `light`) and sparks crackle at the bolts.
- Harvest plushes: an owl, a hedgehog, an acorn, an ear of corn and a maple leaf, five more plush prizes for the fall
  fair midway (tools/midway.py PLUSHES).
Every recipe follows the agriculture feature switch.
"""

FEATURE = "agriculture"

FLYING_EYEBALL = {"block": "flying_eyeball", "display": "Flying Eyeball", "hover_pixels": 1.25, "bob_ticks": 60, "flap_ticks": 9,
                  "watch_range": 10, "turn_speed": 9}
PILLAR_CANDLES = {"ivory_pillar_candle": "Ivory Pillar Candle", "black_pillar_candle": "Black Pillar Candle"}
PILLAR = {"flame_above": 2.0}
# For each number of candles in the block: each candle's (centre x, centre z, width, height) in pixels.
CANDLES = {
    1: [(8.0, 8.0, 5.0, 12.0)],
    2: [(6.0, 7.0, 5.0, 13.0), (10.5, 10.0, 4.0, 8.0)],
    3: [(5.5, 6.5, 5.0, 13.0), (10.5, 7.0, 4.0, 10.0), (8.0, 11.0, 4.0, 6.0)],
    4: [(5.0, 6.0, 5.0, 14.0), (10.5, 6.0, 4.0, 10.0), (5.5, 11.0, 4.0, 8.0), (10.5, 11.0, 4.0, 5.0)],
}
SPIDER_WEB = {"block": "spider_web", "display": "Spider Web"}
MONSTER_HEAD = {"block": "monster_head", "display": "Monster's Head", "light": 6, "spark_chance": 3}
# The plushes' footprints and weights in the prize table are in tools/midway.py PLUSHES; their names here.
PLUSHES = {"owl_plush": "Owl Plush", "hedgehog_plush": "Hedgehog Plush", "acorn_plush": "Acorn Plush", "corn_plush": "Corn Plush",
           "maple_leaf_plush": "Maple Leaf Plush"}

SHAPED = [
    {"id": FLYING_EYEBALL["block"], "pattern": ["MEM"], "key": {"M": "minecraft:phantom_membrane", "E": "minecraft:spider_eye"},
     "result": FLYING_EYEBALL["block"], "count": 1, "category": "building"},
    {"id": "ivory_pillar_candle", "pattern": ["S", "H", "H"], "key": {"S": "minecraft:string", "H": "minecraft:honeycomb"},
     "result": "ivory_pillar_candle", "count": 1, "category": "building"},
    {"id": SPIDER_WEB["block"], "pattern": ["S S", " S "], "key": {"S": "minecraft:string"},
     "result": SPIDER_WEB["block"], "count": 3, "category": "building"},
    {"id": MONSTER_HEAD["block"], "pattern": ["WWW", "NFN", "FSF"],
     "key": {"W": "minecraft:black_wool", "N": "minecraft:iron_nugget", "F": "minecraft:rotten_flesh", "S": "minecraft:slime_ball"},
     "result": MONSTER_HEAD["block"], "count": 1, "category": "building"},
]
SHAPELESS = [
    {"id": "black_pillar_candle", "inputs": ["jugcraft:ivory_pillar_candle", "minecraft:black_dye"],
     "result": "black_pillar_candle", "count": 1, "category": "building"},
]


def blocks():
    """The blocks this batch adds itself (the plushes are midway blocks, in tools/midway.py)."""
    return [FLYING_EYEBALL["block"], *PILLAR_CANDLES, SPIDER_WEB["block"], MONSTER_HEAD["block"]]


def items():
    return blocks()


def names():
    out = {FLYING_EYEBALL["block"]: FLYING_EYEBALL["display"], SPIDER_WEB["block"]: SPIDER_WEB["display"],
           MONSTER_HEAD["block"]: MONSTER_HEAD["display"]}
    out.update(PILLAR_CANDLES)
    return out


def flames(count):
    """The flames' positions in blocks for `count` candles: over each wick, as PillarCandleBlock gives its particles."""
    return [(x / 16, (h + PILLAR["flame_above"]) / 16, z / 16) for x, z, _, h in CANDLES[count]]
