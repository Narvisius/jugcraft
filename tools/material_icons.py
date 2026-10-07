"""The material sets in the owner's style (docs/MATERIAL_SETS.md, docs/features/material-sets.md): every metal's ingot,
nugget and storage block, every ore and deepslate ore, the mined metals' raw ores and raw blocks, and the bronze, steel
and thallite tools.

The owner asked to redraw the bronze and steel tools and the ores in the style of their chartreuse material-set sheet,
chose the alternate bronze and steel palettes, and asked for ingots and nuggets in vanilla's shape. Each thing is one
hand-drawn 16x16 map, tools/material_icons/<name>.txt: 16 lines of 16 symbols (lines starting with # are comments or,
as in PR #201's tools/check_icon_maps.py, directives such as "# type: overlay"). A map names no colours, only what each
pixel is, so one map serves every metal or ore:

  .          transparent (in an ore overlay, vanilla's stone or deepslate shows through)
  O D M L H  the material: outline, dark, mid, light, highlight (a metal's ramp, or an ore's own tones)
  w b B      a tool's stick: outline, dark, light
  r s t u    host rock, dark to light (host_stone and host_deepslate: previews and the fallback only)

Every map is drawn fresh; no Mojang texture is read, traced or recoloured. The ingot and nugget take vanilla's form,
drawn from memory; the client test (MaterialSetsClientGameTests) shows them beside vanilla's for the owner to judge.

An ore block is two layers (template()): vanilla's own stone or deepslate, referenced by name and never copied, with the
ore's overlay (cut out: alpha 0 or 255 only) on top, so it matches the rock around it. 26.3 takes the cut-out from the
overlay's alpha; no render type is set. OVERLAY = False is the fallback: the ore textures are drawn on our own host-rock
maps instead and the models are plain cube_all again.

generate_textures.py and gear_textures.py save the textures (draw_materials, draw_tools), generate_material_data.py
writes the ore models (block_model, write_templates), and check_mod_data.py checks that the committed files match.
Self-contained: Pillow and tools/materials.py only.
"""
import json
import math
import os

from PIL import Image

from materials import METALS, MINERALS

FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), "material_icons")
SIZE = 16


def hx(text):
    return tuple(int(text[i:i + 2], 16) for i in (0, 2, 4))


def ramp(text):
    """Five tones, outline to highlight, from 'O D M L H' hex codes."""
    return tuple(hx(code) for code in text.split())


# A tool's stick.
WOOD = {"w": hx("2d1c0e"), "b": hx("6b4524"), "B": hx("9a6e3c")}

# Every metal's five tones (outline, dark, mid, light, highlight): the ingot, nugget and storage block, and bronze's and
# steel's tools. Bronze and steel are the owner's chosen palettes. The other twelve are their old palettes moved onto the
# ladder those two set (luma about O 37, D 86, M 127, L 169, H 216), each with a hue, a saturation floor and ceiling and
# a luma offset, chosen so that no two metals (vanilla's iron, gold and copper included) are closer than PAIR_FLOOR and
# each ingot keeps the tint of its own metal's plate, gear and dust (docs/MATERIAL_SETS.md, "The metals").
METAL_RAMPS = {
    "tin": ramp("2a363b 5f7e89 97adb7 c7d4d8 f4f6f7"),       # pale, faintly cyan white
    "zinc": ramp("242c32 516776 7b95a7 a6becd dae7f0"),      # light blue-grey
    "lead": ramp("191b23 393f51 5a6381 838ca7 bcc1cd"),      # dark slate blue-grey
    "silver": ramp("373745 7f7f9e b5b5cc dedeed f5f5f7"),    # the brightest: a cool, faintly violet white
    "nickel": ramp("3d3b30 8e866c c1bdaa e7e5d9 f8f7f0"),    # bright warm cream
    "tungsten": ramp("1a1c1e 3d4045 5f646c 898d94 bfc1c6"),  # dark neutral grey
    "uranium": ramp("303426 6e7a55 a0ab86 cbd0be f4f5f1"),   # pale olive
    "titanium": ramp("2a3144 5e73a4 95a4c5 c5cddd f3f5f7"),  # light periwinkle grey
    # The owner's chartreuse sheet (docs/features/thallite.md). Its outline, 354514, is darkened to 303f12 (same hue and
    # saturation, CIEDE2000 2.0) so that D stands 30 luma above O, as check_mod_data.py asks of every ramp.
    "thallite": ramp("303f12 4e611d 7c8a37 aab053 dbdd85"),
    "bronze": ramp("3e2410 7e5222 b4803c dcaa5c f6d696"),    # the owner's choice
    "aluminum": ramp("2e3d4b 6c8cab afbdd3 dfe5ec f5f6f9"),  # very pale blue-white
    "brass": ramp("3c310c 8d7116 cca21a eecb5a faf2d5"),     # deep yellow, darker and more ochre than gold
    "invar": ramp("343d3b 768d88 b3c0bd e2e6e5 f5f7f6"),     # pale sage white
    "solder": ramp("2a2c31 616572 8f93a0 babcc5 e7e7ea"),    # plain mid grey
    "steel": ramp("1e2129 4a5262 6e7889 98a2b4 d0d8e4"),     # the owner's choice
}

# How far apart any two metals' ramps must be (ramp_distance): ours, and ours against the mod's stand-ins for vanilla's
# iron, gold and copper (COPPER_METAL, IRON_METAL, GOLD_METAL in tools/generate_textures.py). check_mod_data.py checks it.
PAIR_FLOOR = 8.0

# Every ore's own five tones, as the owner approved them: its overlay, and a mined metal's raw ore and raw block.
ORE_RAMPS = {
    "tin": ramp("1c120e 362216 4e3424 6e4e36 d6cab8"),         # cassiterite: glossy brown-black
    "zinc": ramp("4a2c0e 784a1c a86c28 c89646 f0d28c"),        # sphalerite: resin brown
    "lead": ramp("2e3038 4e525e 767a88 a2a6b4 dee2ee"),        # galena: lead grey, bright cleavage
    "silver": ramp("6e7480 b8bcc4 d8dade f0f0f2 ffffff"),      # native silver
    "nickel": ramp("4e4018 826e32 aa964c cebc6e f2e6ac"),      # pentlandite: bronze-yellow
    "tungsten": ramp("101012 1c1a1c 302c2a 4a4440 a49a90"),    # wolframite: black, a grey glint
    "uranium": ramp("14140e 282818 5a6a1a bad03a e8f27a"),     # pitchblende with a yellow-green bloom
    "titanium": ramp("2e120a 4a1e12 78361e a8562c e2965c"),    # rutile: red-brown
    "salt": ramp("a08c90 c8aeb2 e2c8cc f2e8ea ffffff"),        # halite: pink-white
    "phosphate": ramp("1e4a44 2e6e66 48968c 74bcae aadcce"),   # apatite: sea green
    "lepidolite": ramp("5a3c6a 966eaa ba92cc d4b6e2 f4e4fa"),  # lilac mica
    "monazite": ramp("5a3010 965a28 ba7838 d49c52 f6d696"),    # honey-brown
    "thallite": ramp("4a5a2e 6e8048 98a86a c0cc8e e4ecb8"),    # the owner's sheet: sage green, the mineral as dug
}

# The three overlay layouts, taken in turn down ORE_RAMPS, so neighbouring ores do not look alike.
LAYOUTS = ("ore_a", "ore_b", "ore_c")
LAYOUT = {ore: LAYOUTS[index % len(LAYOUTS)] for index, ore in enumerate(ORE_RAMPS)}

# Our own host rock (host_stone, host_deepslate), dark to light: previews and the fallback only.
ROCK = {"stone": {"r": hx("6a7076"), "s": hx("7c8288"), "t": hx("8c9298"), "u": hx("9ea3a8")},
        "deepslate": {"r": hx("36383e"), "s": hx("42444b"), "t": hx("4e5058"), "u": hx("5c5e66")}}

TOOLS = ("sword", "pickaxe", "axe", "shovel", "hoe", "paxel")
TOOL_METALS = ("bronze", "steel", "thallite")
# The metals with a paxel (tools/gear.py PAXEL_TIERS); thallite has none (docs/features/thallite.md).
PAXEL_METALS = ("bronze", "steel")

# True: ores are vanilla's stone or deepslate with our overlay on top. False: the fallback, full textures on our host.
OVERLAY = True
# Each host rock's vanilla textures, referenced by name: (sides, top and bottom).
HOST = {"stone": ("minecraft:block/stone", "minecraft:block/stone"),
        "deepslate": ("minecraft:block/deepslate", "minecraft:block/deepslate_top")}
TEMPLATE = "template_ore"
FACES = ("down", "up", "north", "south", "west", "east")


def path(name):
    return os.path.join(FOLDER, name + ".txt")


def has(name):
    return os.path.exists(path(name))


def load(name):
    """The map's 16 rows (comment and directive lines, starting with #, are skipped)."""
    with open(path(name), encoding="utf-8") as f:
        rows = [line.rstrip("\n") for line in f if line.strip() and not line.startswith("#")]
    if len(rows) != SIZE or any(len(row) != SIZE for row in rows):
        raise ValueError(f"{path(name)}: a map is {SIZE} rows of {SIZE} symbols, not {[len(r) for r in rows]}")
    return rows


def draw(name, palette, base=None):
    """`name`'s map coloured from `palette` (symbol -> RGB), over `base` if given."""
    image = base.copy() if base else Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(load(name)):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch not in palette:
                raise ValueError(f"{path(name)}: unknown symbol {ch!r} at {x},{y}")
            image.putpixel((x, y), tuple(palette[ch]) + (255,))
    return image


def tones(five):
    return dict(zip("ODMLH", five))


# The owner's ingot (tools/material_icons/ingot.txt) is drawn in eight greens, 1 the darkest to 8 the lightest. Each
# metal recolours it, never redraws it: a green's place between the darkest and lightest green, by luma, is the same
# place on the metal's own ramp from its outline O to its highlight H, blended between the two ramp tones around it.
OWNER_INGOT_GREENS = {"1": "017b12", "2": "01831e", "3": "009424", "4": "01aa28",
                      "5": "3ff084", "6": "82f4ad", "7": "affacc", "8": "dafcea"}


def ingot_palette(five):
    """The owner's ingot's eight symbols coloured from a metal's five-tone ramp (outline to highlight), by luma."""
    greens = {k: luma(hx(v)) for k, v in OWNER_INGOT_GREENS.items()}
    low, high = min(greens.values()), max(greens.values())
    ramp = [tuple(c) for c in five]
    places = [(luma(c) - luma(ramp[0])) / (luma(ramp[-1]) - luma(ramp[0])) for c in ramp]
    palette = {}
    for symbol, value in greens.items():
        t = (value - low) / (high - low)
        for i in range(len(ramp) - 1):
            if places[i] <= t <= places[i + 1] or i == len(ramp) - 2:
                span = places[i + 1] - places[i]
                f = 0.0 if span <= 0 else min(1.0, max(0.0, (t - places[i]) / span))
                palette[symbol] = tuple(round(a + (b - a) * f) for a, b in zip(ramp[i], ramp[i + 1]))
                break
    return palette


def mined():
    return [metal for metal, info in METALS.items() if info["mined"]]


def ore_blocks():
    """(block, ore, rock) for every ore block: the stone ores, then the deepslate ores."""
    ores = mined() + list(MINERALS)
    return [(f"{ore}_ore", ore, "stone") for ore in ores] + [(f"deepslate_{ore}_ore", ore, "deepslate") for ore in ores]


def material_textures():
    """{(kind, name): image} for every texture but the tools, in a fixed order."""
    out = {}
    for metal in METALS:
        palette = tones(METAL_RAMPS[metal])
        out[("item", f"{metal}_ingot")] = draw("ingot", ingot_palette(METAL_RAMPS[metal]))
        out[("item", f"{metal}_nugget")] = draw("nugget", palette)
        out[("block", f"{metal}_block")] = draw("storage_block", palette)
    for block, ore, rock in ore_blocks():
        base = None if OVERLAY else draw("host_" + rock, ROCK[rock])
        out[("block", block)] = draw(LAYOUT[ore], tones(ORE_RAMPS[ore]), base)
    for metal in mined():
        palette = tones(ORE_RAMPS[metal])
        out[("item", f"raw_{metal}")] = draw("raw", palette)
        out[("block", f"raw_{metal}_block")] = draw("raw_block", palette)
    return out


def tool_textures():
    """{(kind, name): image} for the bronze, steel and thallite tools (and the bronze and steel paxels)."""
    return {("item", f"{metal}_{tool}"): draw(tool, dict(WOOD, **tones(METAL_RAMPS[metal])))
            for metal in TOOL_METALS for tool in TOOLS if tool != "paxel" or metal in PAXEL_METALS}


def textures():
    """Every texture the material sets draw."""
    return {**material_textures(), **tool_textures()}


def draw_materials(save):
    """save(image, kind, name) as in generate_textures.py, for everything but the tools."""
    for (kind, name), image in material_textures().items():
        save(image, kind, name)


def draw_tools(save):
    """save(image, kind, name) as in generate_textures.py, for the bronze, steel and thallite tools."""
    for (kind, name), image in tool_textures().items():
        save(image, kind, name)


def template():
    """The ore block model's parent, built as vanilla's grass_block lays its side overlay: the host rock as a full cube,
    then the overlay as a second full cube with exactly the same corners. Each pair of faces gets the same depth, and the
    overlay, drawn after the rock, wins at every distance. Every face keeps its full UV and culls against its neighbour
    (ores are buried by the thousand)."""
    def faces(sides, top):
        return {face: {"uv": [0, 0, 16, 16], "texture": top if face in ("down", "up") else sides, "cullface": face}
                for face in FACES}
    return {"parent": "minecraft:block/block",
            "textures": {"particle": "#stone", "stone_top": "#stone"},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#stone", "#stone_top")},
                         {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#overlay", "#overlay")}]}


def block_model(block):
    """An ore block's model (vanilla's rock under its own overlay), or None for any other block or in the fallback."""
    if not OVERLAY:
        return None
    for name, _ore, rock in ore_blocks():
        if name == block:
            sides, top = HOST[rock]
            textures = {"stone": sides}
            if top != sides:
                textures["stone_top"] = top
            textures["overlay"] = f"jugcraft:block/{block}"
            return {"parent": f"jugcraft:block/{TEMPLATE}", "textures": textures}
    return None


def write_templates(assets):
    """The ore template under `assets`, unless in the fallback. Written here in generate_material_data.py's format, not
    through its write(): that runs model_writer.separate_coplanar, which would nudge one of the two coplanar cubes (and on
    the north and south faces it pushes the rock in front of the overlay)."""
    if OVERLAY:
        path = assets / "models" / "block" / f"{TEMPLATE}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(template(), indent=2) + "\n", encoding="utf-8")


def luma(colour):
    r, g, b = colour[:3]
    return 0.299 * r + 0.587 * g + 0.114 * b


def _lab(colour):
    """sRGB (0-255) to CIELAB (D65)."""
    def linear(u):
        u /= 255
        return u / 12.92 if u <= 0.04045 else ((u + 0.055) / 1.055) ** 2.4
    r, g, b = (linear(v) for v in colour[:3])
    x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
    y = 0.2126 * r + 0.7152 * g + 0.0722 * b
    z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883

    def f(t):
        return t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    return 116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z))


def delta_e(first, second):
    """CIEDE2000 colour difference between two sRGB colours."""
    l1, a1, b1 = _lab(first)
    l2, a2, b2 = _lab(second)
    c_mean = (math.hypot(a1, b1) + math.hypot(a2, b2)) / 2
    g = 0.5 * (1 - math.sqrt(c_mean ** 7 / (c_mean ** 7 + 25 ** 7)))
    a1, a2 = (1 + g) * a1, (1 + g) * a2
    c1, c2 = math.hypot(a1, b1), math.hypot(a2, b2)
    h1, h2 = math.degrees(math.atan2(b1, a1)) % 360, math.degrees(math.atan2(b2, a2)) % 360
    dh = 0 if c1 * c2 == 0 else (h2 - h1 + 180) % 360 - 180
    dl, dc = l2 - l1, c2 - c1
    dh_term = 2 * math.sqrt(c1 * c2) * math.sin(math.radians(dh / 2))
    l_mean, c_mean = (l1 + l2) / 2, (c1 + c2) / 2
    if c1 * c2 == 0:
        h_mean = h1 + h2
    elif abs(h1 - h2) <= 180:
        h_mean = (h1 + h2) / 2
    else:
        h_mean = (h1 + h2 + 360) / 2 if h1 + h2 < 360 else (h1 + h2 - 360) / 2
    t = (1 - 0.17 * math.cos(math.radians(h_mean - 30)) + 0.24 * math.cos(math.radians(2 * h_mean))
         + 0.32 * math.cos(math.radians(3 * h_mean + 6)) - 0.20 * math.cos(math.radians(4 * h_mean - 63)))
    rotation = 30 * math.exp(-((h_mean - 275) / 25) ** 2)
    rc = 2 * math.sqrt(c_mean ** 7 / (c_mean ** 7 + 25 ** 7))
    sl = 1 + 0.015 * (l_mean - 50) ** 2 / math.sqrt(20 + (l_mean - 50) ** 2)
    sc = 1 + 0.045 * c_mean
    sh = 1 + 0.015 * c_mean * t
    rt = -math.sin(math.radians(2 * rotation)) * rc
    return math.sqrt((dl / sl) ** 2 + (dc / sc) ** 2 + (dh_term / sh) ** 2 + rt * (dc / sc) * (dh_term / sh))


def ramp_distance(first, second):
    """How far apart two five-tone ramps look: the mean CIEDE2000 of their dark, mid and light tones (an ingot's body)."""
    return sum(delta_e(first[i], second[i]) for i in (1, 2, 3)) / 3
