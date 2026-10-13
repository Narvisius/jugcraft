"""Generate compact companion beds from vanilla 26.3 bed geometry and texture references.
Usage: python tools/companion_beds.py path/to/minecraft-client-only.jar
"""
import copy, json, sys, zipfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
COLORS = "white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black".split()
def write(path, value):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
with zipfile.ZipFile(sys.argv[1]) as jar:
    templates = {part: json.loads(jar.read(f"assets/minecraft/models/block/template_bed_{part}.json")) for part in ["head", "foot"]}
    models = {(color, part): json.loads(jar.read(f"assets/minecraft/models/block/{color}_bed_{part}.json")) for color in COLORS for part in templates}
langpath = ROOT / "src/main/resources/assets/peepo_companion/lang/en_us.json"
lang = json.loads(langpath.read_text())
for color in COLORS:
    name = f"{color}_companion_bed"
    textures = {"particle": "minecraft:block/oak_planks"}
    elements = []
    for part in templates:
        template = templates[part]
        resolved = template["textures"] | models[color, part]["textures"]
        for key, value in resolved.items():
            if not value.startswith("#"): textures[part + "_" + key] = value
        for original in template["elements"]:
            e = copy.deepcopy(original)
            for bound in ["from", "to"]:
                x, y, z = e[bound]
                e[bound] = [2 + x * .75, y * 2 / 3, z * .5 + (8 if part == "foot" else 0)]
            for face in e["faces"].values():
                face.pop("cullface", None)
                face["texture"] = "#" + part + "_" + face["texture"][1:]
            elements.append(e)
    base = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    write(f"src/main/resources/assets/peepo_companion/models/block/{name}.json", base)
    bunk = copy.deepcopy(base)
    def timber(lo, hi):
        return {"from": lo, "to": hi, "faces": {face: {"uv": [0,13,3,16], "texture": "#head_west"} for face in ["up","down","north","south","east","west"]}}
    # Start above the mattress: posts through its sides caused coplanar faces.
    for x in [2, 13]:
        for z in [0, 15]:
            post = timber([x,6,z],[x+1,16,z+1])
            # These ends meet the mattress below and the upper bed legs.
            post["faces"].pop("down")
            post["faces"].pop("up")
            bunk["elements"].append(post)
    for y in [7, 10, 13, 15]: bunk["elements"].append(timber([3,y,15],[13,y+1,16]))
    write(f"src/main/resources/assets/peepo_companion/models/block/{name}_bunk.json", bunk)
    variants = {f"facing={face},stacked={str(stacked).lower()}": {"model": f"peepo_companion:block/{name}" + ("_bunk" if stacked else ""), "y": rot} for face,rot in [("north",0),("east",90),("south",180),("west",270)] for stacked in [False,True]}
    write(f"src/main/resources/assets/peepo_companion/blockstates/{name}.json", {"variants": variants})
    write(f"src/main/resources/assets/peepo_companion/items/{name}.json", {"model":{"type":"minecraft:model","model":f"peepo_companion:block/{name}"}})
    write(f"src/main/resources/data/peepo_companion/loot_table/blocks/{name}.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"peepo_companion:{name}"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
    write(f"src/main/resources/data/peepo_companion/recipe/{name}.json", {"type":"minecraft:crafting_shaped","category":"misc","pattern":["WW","PP"],"key":{"W":f"minecraft:{color}_wool","P":"#minecraft:planks"},"result":{"id":f"peepo_companion:{name}","count":1}})
    write(f"src/main/resources/data/peepo_companion/recipe/{name}_recolor.json", {"type":"minecraft:crafting_shapeless","category":"misc","ingredients":["#peepo_companion:companion_beds",f"minecraft:{color}_dye"],"result":{"id":f"peepo_companion:{name}","count":1}})
    lang[f"block.peepo_companion.{name}"] = color.replace("_", " ").title() + " Companion Bed"
write("src/main/resources/assets/peepo_companion/lang/en_us.json",lang)
write("src/main/resources/data/peepo_companion/tags/item/companion_beds.json",{"values":[f"peepo_companion:{color}_companion_bed" for color in COLORS]})
p = ROOT / "src/main/resources/data/minecraft/tags/block/mineable/axe.json"
data = json.loads(p.read_text()) if p.exists() else {"values":[]}
for color in COLORS:
    value=f"peepo_companion:{color}_companion_bed"
    if value not in data["values"]: data["values"].append(value)
write(p.relative_to(ROOT), data)
