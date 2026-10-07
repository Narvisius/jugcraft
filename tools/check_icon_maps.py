"""Check the 16x16 item icon maps, their materials and the item icons' sizes against docs/ITEM_ICONS.md. An offline lint
of text files, colour values and image sizes; NOT a Minecraft build or game test, and no judge of how an icon reads
(look at it for that).

    python3 tools/check_icon_maps.py                   every map in tools/arms_icons/ and tools/item_icons/, the
                                                       palettes and the icon sizes
    python3 tools/check_icon_maps.py <folder|map> ...  those maps instead (and the palettes)
    python3 tools/check_icon_maps.py --preview [out.png] [<folder|map> ...]
                                                       also draw each map in its materials at 8x, 2x and 1x, on a light
                                                       and a dark slot, to look at (by default into the system's temp
                                                       folder, so no picture lands in the repository)
    python3 tools/check_icon_maps.py --self-test       break approved maps and palettes in known ways, and confirm that
                                                       each break is caught

A map is 16 rows of 16 symbols (SYMBOLS); lines starting with # are comments, and a few are directives (DIRECTIVE).
Errors, which make the exit status 1:
- 16 rows of 16 known symbols; host rock only in a block face or ore overlay;
- a malformed directive: an unknown allowance, family, type, role or material; an allowance without its reason; a role
  the map uses but its `# materials:` line does not name;
- fill touching transparency or the canvas edge (a flame is never outlined, so it may touch transparency);
- an outline pixel with no fill on any of its four sides (a stray outline);
- a 2x2 block of outline (an outline two pixels thick);
- a pinhole, transparency the drawing encloses, unless the map declares it;
- each frame of a flickering flame (tools/arms_art.py ANIMATED), checked the same way;
- a palette the maps are coloured in (every material of tools/arms_pixel.py STYLES, and every material a map declares
  from tools/icon_materials.py) whose tones do not step from dark to light, whose outline is black, too light or too
  close to its dark, whose highlight jumps too far, or whose main metal sits too close to the copper, iron or gold it
  must be told apart from (skipped for vanilla's own metals and owner-approved palettes);
- an item icon larger than 16x16 that is not one of the legacy icons in tools/legacy_item_icons.txt (full run only).
Warnings, printed but never failing:
- a map's size outside its family's norms (FAMILIES): span and blade share for the arms and tools, how much of the
  canvas a compact, upright or flat item covers and where it sits;
- on a slim item, a highlight with main material both above it and to its left (light comes from the top left);
- a lone part of one or two pixels (a speck);
- a declared allowance the map does not need;
- the Arms VII lines' materials (tools/arms_variants_art.py LINE_STYLES) against the palette rules, until a variant of
  that line is drawn as a map, when they become errors;
- a legacy icon that has been redrawn at 16x16, or removed, and should leave the list.

tools/check_mod_data.py runs errors() and self_test() in CI.
"""
import json
import math
import os
import re
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import arms_art  # noqa: E402
import arms_icons  # noqa: E402
import arms_pixel  # noqa: E402
import arms_variants  # noqa: E402
import arms_variants_art  # noqa: E402
import generate_textures  # noqa: E402
import icon_materials  # noqa: E402

SIZE = 16
ROOT = os.path.dirname(HERE)
FOLDERS = [os.path.join(HERE, "arms_icons"), os.path.join(HERE, "item_icons")]
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "jugcraft")
LEGACY = os.path.join(HERE, "legacy_item_icons.txt")

# The symbols every icon map shares (docs/ITEM_ICONS.md, "The symbols"). A map names roles, never colours: which
# material fills each role comes from the arm's style, or from the map's `# materials:` line.
SYMBOLS = {
    ".": "transparent",
    "O": "main material: outline", "D": "dark", "M": "mid", "L": "light", "H": "highlight",
    "g": "second material: outline", "f": "dark", "F": "mid", "Y": "light",
    "w": "wood: outline (and round a wrap)", "b": "dark", "B": "light",
    "k": "wrap: dark", "K": "light",
    "a": "stone: dark", "A": "light",
    "e": "accent: dark", "E": "light",
    "c": "chain: dark (its own edge)", "C": "light",
    "x": "flame: orange", "X": "yellow",
    "r": "host rock (block faces only), darkest", "s": "dark", "t": "light", "u": "lightest",
}
# Each role: its symbols, the arms_pixel.Style attribute an arm takes it from, and the tones of a Material it shows
# (arms_icons.palette), darkest first.
ROLES = {
    "main": ("ODMLH", "blade", ("outline_dark", "dark", "mid", "light", "highlight")),
    "second": ("gfFY", "fitting", ("outline_dark", "dark", "mid", "light")),
    "wood": ("wbB", "haft", ("outline_dark", "mid", "light")),
    "wrap": ("kK", "grip", ("dark", "light")),
    "stone": ("aA", "gem", ("dark", "light")),
    "accent": ("eE", "accent", ("mid", "light")),
}
OUTLINE = set("Ogw")
BLADE = set("DMLH")
FLAME = set("xX")
CHAIN_EDGE = set("c")           # the chain's dark tone is its own edge: it may touch transparency and needs no fill
CHAIN = set("cC")                # links are small by design (one or two pixels each)
ROCK = set("rstu")
FILL = set(SYMBOLS) - OUTLINE - {"."}
TYPES = ("icon", "face", "overlay")

# Map directives, in comment lines:
#   # allow: pinhole - <why>       the drawing may enclose transparency
#   # allow: bare k K - <why>      those fill symbols may touch transparency, never the canvas edge
#   # family: <family>             the size norms for a map whose name is not in KIND_FAMILY (FAMILIES)
#   # type: face | overlay         a full block face (no transparency, no outline rules), or an ore overlay (the rock
#                                  is the outline, so fill may touch transparency and enclose it)
#   # materials: main=<m> second=<m> wood=<m> ...   the material of each role the map uses (tools/icon_materials.py)
DIRECTIVE = re.compile(r"^#\s*(allow|family|type|materials)\s*:\s*(.*)$")
ROCK_COLOURS = {"r": (99, 99, 99, 255), "s": (112, 112, 112, 255), "t": (125, 125, 125, 255), "u": (139, 139, 139, 255)}
LIGHT_SLOT, DARK_SLOT = (139, 139, 139), (55, 55, 55)

# Size norms, measured from the 38 approved arms maps and the material-set drafts the owner liked (docs/ITEM_ICONS.md,
# rules 2 and 3). Slim items: the span is the number of diagonal steps from the hand end to the tip (16 is corner to
# corner); the share is the blade's or head's steps (the D M L H fill) over the span. Compact, upright and flat items:
# `cover` is the share of the 256 pixels that are opaque, `centre` how far (pixels) the bounding box's middle may sit
# from the canvas's, and `base` the rows the lowest opaque pixel may sit on. None: not measured.
FAMILIES = {
    "long_sword": {"span": (14, 15), "share": (0.62, 0.75)},
    "thrusting_sword": {"span": (14, 15), "share": (0.55, 0.70)},
    "polearm": {"span": (14, 15), "share": (0.20, 0.65)},
    "staff": {"span": (15, 15), "share": None},
    "headed": {"span": (12, 15), "share": (0.20, 0.60)},
    "short": {"span": (9, 12), "share": None},
    "tool": {"span": (13, 15), "share": None},
    "compact": {"cover": (0.08, 0.68), "centre": 1.5},
    "upright": {"cover": (0.15, 0.60), "centre": 1.0, "base": (13, 15)},
    "flat": {"cover": (0.10, 0.65), "centre": 1.5},
}
SLIM = {name for name, norms in FAMILIES.items() if "span" in norms}
KIND_FAMILY = {kind: family for family, kinds in {
    "long_sword": "longsword sabre greatsword zweihander executioner battleblade moonblade nodachi",
    "thrusting_sword": "estoc rapier",
    "polearm": "spear pike glaive halberd bill lance war_fork harpoon scythe",
    "staff": "quarterstaff twinblade",
    "headed": "battle_axe labrys war_hammer maul earthbreaker flanged_mace brazier_mace flail war_pick",
    "short": "dagger katana katar kama chakram kusarigama francisca javelin",
}.items() for kind in kinds.split()}

# The copper, iron and gold a new metal must be told apart from are the mod's own stand-ins for them
# (tools/generate_textures.py, drawn fresh, not taken from vanilla), darkest first; their last four tones line up with
# a ramp's dark, mid, light and highlight. The test is our reading of why the owner chose "the alternate versions",
# not the owner's words: the palettes not chosen sat 4.7 and 4.9 from copper and iron.
NEIGHBOURS = {"copper": generate_textures.COPPER_METAL, "iron": generate_textures.IRON_METAL,
              "gold": generate_textures.GOLD_METAL}
MIN_DISTANCE = 10.0     # mean CIEDE2000 over D, M, L and H; the copper-like bronze not chosen was 4.7, the chosen 13.0
MIN_OUTLINE_GAP = 30    # luma from O up to D; the draft thallite's 25 let D merge into the outline
MAX_HIGHLIGHT_JUMP = 70  # luma from L up to H; the draft tin's 119 made every glint a speck
MAX_OUTLINE = 45        # luma of the outline (or a third of the mid tone's, for a pale material): the approved are 41
                        # and 33; the copper stand-in's 67 made a test coin soft


def luma(colour):
    r, g, b = colour[:3]
    return 0.299 * r + 0.587 * g + 0.114 * b


def lab(colour):
    def linear(v):
        v /= 255
        return v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4
    r, g, b = (linear(v) for v in colour[:3])
    x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
    y = 0.2126 * r + 0.7152 * g + 0.0722 * b
    z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883

    def f(t):
        return t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    fx, fy, fz = f(x), f(y), f(z)
    return 116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)


def delta_e(c1, c2):
    """CIEDE2000 colour difference: about 1 is just noticeable, 10 is plainly a different colour."""
    l1, a1, b1 = lab(c1)
    l2, a2, b2 = lab(c2)
    c_bar = (math.hypot(a1, b1) + math.hypot(a2, b2)) / 2
    g = 0.5 * (1 - math.sqrt(c_bar ** 7 / (c_bar ** 7 + 25 ** 7)))
    a1p, a2p = (1 + g) * a1, (1 + g) * a2
    c1p, c2p = math.hypot(a1p, b1), math.hypot(a2p, b2)
    h1 = math.degrees(math.atan2(b1, a1p)) % 360
    h2 = math.degrees(math.atan2(b2, a2p)) % 360
    dl, dc = l2 - l1, c2p - c1p
    dh = 0.0
    if c1p * c2p:
        dh = h2 - h1
        dh = dh - 360 if dh > 180 else dh + 360 if dh < -180 else dh
    dh_big = 2 * math.sqrt(c1p * c2p) * math.sin(math.radians(dh / 2))
    l_bar, cp_bar = (l1 + l2) / 2, (c1p + c2p) / 2
    if not c1p * c2p:
        h_bar = h1 + h2
    elif abs(h1 - h2) <= 180:
        h_bar = (h1 + h2) / 2
    else:
        h_bar = (h1 + h2 + 360) / 2 if h1 + h2 < 360 else (h1 + h2 - 360) / 2
    t = (1 - 0.17 * math.cos(math.radians(h_bar - 30)) + 0.24 * math.cos(math.radians(2 * h_bar))
         + 0.32 * math.cos(math.radians(3 * h_bar + 6)) - 0.20 * math.cos(math.radians(4 * h_bar - 63)))
    d_theta = 30 * math.exp(-((h_bar - 275) / 25) ** 2)
    r_c = 2 * math.sqrt(cp_bar ** 7 / (cp_bar ** 7 + 25 ** 7))
    s_l = 1 + 0.015 * (l_bar - 50) ** 2 / math.sqrt(20 + (l_bar - 50) ** 2)
    s_c, s_h = 1 + 0.045 * cp_bar, 1 + 0.015 * cp_bar * t
    r_t = -math.sin(math.radians(2 * d_theta)) * r_c
    return math.sqrt((dl / s_l) ** 2 + (dc / s_c) ** 2 + (dh_big / s_h) ** 2 + r_t * (dc / s_c) * (dh_big / s_h))


def ramp_distance(tones, neighbour):
    """Mean CIEDE2000 between a ramp's D, M, L, H and a neighbour's last four tones."""
    return sum(delta_e(a, b) for a, b in zip(tones, neighbour[-4:])) / 4


def palette_problems(name, material, role="main", distance=True):
    """A material's tones, as the role shows them, against the palette rules (docs/ITEM_ICONS.md, rule 6). The main
    material is a full ramp (outline, dark, mid, light, highlight); the other roles show fewer tones. `distance`: test
    the main material against copper, iron and gold (False for vanilla's own metals and owner-approved palettes)."""
    names = ROLES[role][2]
    tones = [getattr(material, tone) for tone in names]
    lumas = [luma(c) for c in tones]
    problems = []
    label = f"palette {name}"
    if any(b <= a for a, b in zip(lumas, lumas[1:])):
        problems.append(f"{label}: {' '.join(names)} must step from dark to light (luma {[round(v) for v in lumas]})")
    if names[0] == "outline_dark":
        if tuple(tones[0]) == (0, 0, 0):
            problems.append(f"{label}: the outline is pure black; use a near-black of the material's own hue")
        limit = max(MAX_OUTLINE, luma(material.mid) / 3)
        if lumas[0] > limit:
            problems.append(f"{label}: the outline is {lumas[0]:.0f} luma (at most {limit:.0f}: {MAX_OUTLINE}, or a "
                            "third of the mid tone for a pale material), so the icon looks soft")
        if names[1] == "dark" and lumas[1] - lumas[0] < MIN_OUTLINE_GAP:
            problems.append(f"{label}: D is only {lumas[1] - lumas[0]:.0f} luma above O (at least {MIN_OUTLINE_GAP}), "
                            "so dark merges into the outline")
    if role == "main":
        if lumas[4] - lumas[3] > MAX_HIGHLIGHT_JUMP:
            problems.append(f"{label}: H is {lumas[4] - lumas[3]:.0f} luma above L (at most {MAX_HIGHLIGHT_JUMP}), "
                            "so a glint reads as a speck")
        if distance:
            for other, ramp in NEIGHBOURS.items():
                gap = ramp_distance(tones[1:], ramp)
                if gap < MIN_DISTANCE:
                    problems.append(f"{label}: only {gap:.1f} from {other} (mean CIEDE2000 over D M L H, at least "
                                    f"{MIN_DISTANCE:.0f}); it would read as {other}. Use the {other} ramp if it is "
                                    "meant to be vanilla's metal, or ask the owner")
    return problems


def exempt(name):
    """Whether a palette (a named material, an arms style or an Arms VII line) skips the distance test: vanilla's own
    metal, or approved by the owner."""
    return name in icon_materials.VANILLA or name in icon_materials.OWNER_APPROVED


class Map:
    def __init__(self, path=None, lines=None, name=None):
        self.path = path
        self.name = name or os.path.splitext(os.path.basename(path))[0]
        self.rows, self.allow, self.bare, self.family, self.type = [], set(), set(), None, "icon"
        self.materials = {}
        self.problems, self.warnings = [], []
        if lines is None:
            with open(path, encoding="utf-8") as f:
                lines = f.read().split("\n")
        for line in lines:
            line = line.rstrip("\r\n")
            if not line.strip():
                continue
            if line.startswith("#"):
                self.directive(line)
            else:
                self.rows.append(line)

    def directive(self, line):
        found = DIRECTIVE.match(line)
        if not found:
            return
        key, words = found.group(1), found.group(2).split()
        if key == "allow":
            if words[:1] == ["pinhole"]:
                self.allow.add("pinhole")
                reason = words[1:]
            elif words[:1] == ["bare"]:
                symbols = []
                for word in words[1:]:
                    if len(word) != 1 or word not in FILL:
                        break
                    symbols.append(word)
                if not symbols:
                    self.problems.append(f"'{line}': name the fill symbols that may touch transparency")
                self.allow.add("bare")
                self.bare |= set(symbols)
                reason = words[1 + len(symbols):]
            else:
                self.problems.append(f"'{line}': unknown allowance (pinhole or bare)")
                return
            if not [word for word in reason if word not in "-:"]:
                self.problems.append(f"'{line}': give the reason after the allowance ('# allow: ... - <why>')")
        elif key == "family":
            if words[:1] and words[0] in FAMILIES:
                self.family = words[0]
            else:
                self.problems.append(f"'{line}': unknown family (one of {', '.join(FAMILIES)})")
        elif key == "type":
            if words[:1] and words[0] in TYPES:
                self.type = words[0]
            else:
                self.problems.append(f"'{line}': unknown type (one of {', '.join(TYPES)})")
        elif key == "materials":
            if not words:
                self.problems.append(f"'{line}': name a material for each role, e.g. main=copper second=brass")
            for word in words:
                role, _, material = word.partition("=")
                if role not in ROLES:
                    self.problems.append(f"'{line}': unknown role {role!r} (one of {', '.join(ROLES)})")
                elif material not in icon_materials.MATERIALS:
                    self.problems.append(f"'{line}': unknown material {material!r}; add it to tools/icon_materials.py "
                                         f"(there: {', '.join(icon_materials.MATERIALS)})")
                else:
                    self.materials[role] = material

    def shape_ok(self):
        if len(self.rows) != SIZE or any(len(row) != SIZE for row in self.rows):
            self.problems.append(f"a map is {SIZE} rows of {SIZE} symbols, not {[len(r) for r in self.rows]}")
            return False
        unknown = sorted({(x, y, ch) for y, row in enumerate(self.rows) for x, ch in enumerate(row) if ch not in SYMBOLS},
                         key=lambda p: (p[1], p[0]))
        for x, y, ch in unknown:
            self.problems.append(f"unknown symbol {ch!r} at ({x},{y})")
        if self.type == "icon":
            for x, y, ch in self.cells(self.rows):
                if ch in ROCK:
                    self.problems.append(f"host rock {ch!r} at ({x},{y}) belongs only in a block face or ore map")
        if self.materials:
            used = {ch for row in self.rows for ch in row}
            for role, (symbols, _, _) in ROLES.items():
                if role not in self.materials and used & set(symbols):
                    shown = " ".join(sorted(used & set(symbols)))
                    self.problems.append(f"uses {shown} ({role}) but its '# materials:' line names no {role} material")
        return not unknown

    def style(self):
        """The materials this map is coloured in, as an arms_pixel.Style: its `# materials:` line, or None (an arm
        takes its style's)."""
        if not self.materials:
            return None
        chosen = {ROLES[role][1]: icon_materials.MATERIALS[name] for role, name in self.materials.items()}
        filler = arms_pixel.STYLES["bronze"]     # roles the map does not use; never drawn
        return arms_pixel.Style(*(chosen.get(part, getattr(filler, part))
                                  for part in ("blade", "fitting", "grip", "haft", "gem", "accent")))

    @staticmethod
    def cells(rows):
        return [(x, y, ch) for y, row in enumerate(rows) for x, ch in enumerate(row) if ch != "."]


def at(rows, x, y):
    return rows[y][x] if 0 <= x < SIZE and 0 <= y < SIZE else None


N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
N8 = N4 + ((1, 1), (1, -1), (-1, 1), (-1, -1))   # a one-pixel diagonal blade is a staircase joined at corners


def check_frame(icon, rows, label):
    """The outline rules for one frame. Returns (problems, bare pixels used, pinholes found)."""
    problems, used_bare, holes = [], False, []
    if icon.type == "face":
        for x, y, ch in [(x, y, ch) for y, row in enumerate(rows) for x, ch in enumerate(row) if ch == "."]:
            problems.append(f"{label}transparent pixel at ({x},{y}) in a block face")
        return problems, used_bare, holes
    for x, y, ch in Map.cells(rows):
        around = [at(rows, x + dx, y + dy) for dx, dy in N4]
        if ch in OUTLINE:
            if not any(n and n != "." and n not in OUTLINE for n in around):
                problems.append(f"{label}outline {ch!r} at ({x},{y}) has no fill on any of its four sides")
            continue
        if ch in CHAIN_EDGE:
            continue
        if None in around:
            problems.append(f"{label}fill {ch!r} at ({x},{y}) is on the canvas edge")
        elif "." in around and icon.type == "icon" and ch not in FLAME:
            if ch in icon.bare:
                used_bare = True
            else:
                problems.append(f"{label}fill {ch!r} at ({x},{y}) touches transparency: outline it")
    for y in range(SIZE - 1):
        for x in range(SIZE - 1):
            if all(rows[y + dy][x + dx] in OUTLINE for dx in (0, 1) for dy in (0, 1)):
                problems.append(f"{label}a 2x2 block of outline at ({x},{y}): the outline is one pixel thick")
    outside = set()
    stack = [(x, y) for x in range(SIZE) for y in range(SIZE)
             if (x in (0, SIZE - 1) or y in (0, SIZE - 1)) and rows[y][x] == "."]
    while stack:
        x, y = stack.pop()
        if (x, y) in outside:
            continue
        outside.add((x, y))
        stack += [(x + dx, y + dy) for dx, dy in N4 if at(rows, x + dx, y + dy) == "." and (x + dx, y + dy) not in outside]
    holes = [(x, y) for y in range(SIZE) for x in range(SIZE) if rows[y][x] == "." and (x, y) not in outside]
    if holes and icon.type == "icon" and "pinhole" not in icon.allow:
        shown = " ".join(f"({x},{y})" for x, y in holes[:6]) + (" ..." if len(holes) > 6 else "")
        problems.append(f"{label}{len(holes)} enclosed transparent pixel(s) {shown}: a pinhole; fill it, open it, or "
                        "declare '# allow: pinhole - <why>'")
    return problems, used_bare, holes


def steps(values):
    return (max(values) - min(values)) // 2 + 1 if values else 0


def measures(rows):
    """Span and blade share along the diagonal (a = x - y), in the steps tools/arms_icons maps are measured in."""
    cells = Map.cells(rows)
    span = steps([x - y for x, y, _ in cells])
    blade = steps([x - y for x, y, ch in cells if ch in BLADE])
    return span, blade


def size_warnings(icon, family):
    norms, rows, warnings = FAMILIES[family], icon.rows, []
    name = family.replace("_", " ")
    if "span" in norms:
        span, blade = measures(rows)
        low, high = norms["span"]
        if not low <= span <= high:
            warnings.append(f"span {span} steps; a {name} runs {low} to {high}")
        share = norms["share"]
        if share and blade and not share[0] <= blade / span <= share[1]:
            warnings.append(f"blade or head {blade} of {span} steps ({blade / span:.2f}); a {name} has "
                            f"{share[0]:.2f} to {share[1]:.2f}")
        return warnings
    cells = Map.cells(rows)
    if not cells:
        return warnings
    cover = len(cells) / SIZE ** 2
    low, high = norms["cover"]
    if not low <= cover <= high:
        warnings.append(f"covers {cover:.0%} of the canvas; a {name} item covers {low:.0%} to {high:.0%}")
    xs, ys = [x for x, _, _ in cells], [y for _, y, _ in cells]
    off_x, off_y = (min(xs) + max(xs)) / 2 - 7.5, (min(ys) + max(ys)) / 2 - 7.5
    if abs(off_x) > norms["centre"] or ("base" not in norms and abs(off_y) > norms["centre"]):
        warnings.append(f"its middle is ({off_x:+.1f}, {off_y:+.1f}) pixels from the canvas's; a {name} item is "
                        f"centred within {norms['centre']}")
    if "base" in norms and not norms["base"][0] <= max(ys) <= norms["base"][1]:
        warnings.append(f"its base is on row {max(ys)}; an {name} item stands on row {norms['base'][0]} to "
                        f"{norms['base'][1]}")
    return warnings


def style_warnings(icon):
    warnings = []
    family = icon.family or KIND_FAMILY.get(icon.name)
    if icon.type != "icon":
        return warnings
    if family:
        warnings += size_warnings(icon, family)
    if family is None or family in SLIM:
        # A slim item's highlight runs along its lit (upper-left) edge. A compact, upright or flat item's lit top face
        # takes H along its front edge or crease too, so it is not warned.
        for x, y, ch in Map.cells(icon.rows):
            if ch == "H" and at(icon.rows, x, y - 1) in BLADE and at(icon.rows, x - 1, y) in BLADE:
                warnings.append(f"highlight at ({x},{y}) has metal above it and to its left: on a slim item H sits on "
                                "the upper-left edge (a compact item declares '# family: compact')")
    seen = set()
    for x, y, ch in Map.cells(icon.rows):
        if ch not in FILL or ch in CHAIN or ch in FLAME or (x, y) in seen:
            continue
        part, stack = [], [(x, y)]
        while stack:
            p = stack.pop()
            if p in seen:
                continue
            seen.add(p)
            part.append(p)
            stack += [(p[0] + dx, p[1] + dy) for dx, dy in N8
                      if (at(icon.rows, p[0] + dx, p[1] + dy) or ".") in FILL - CHAIN - FLAME]
        if len(part) <= 2:
            warnings.append(f"a lone part of {len(part)} pixel(s) at {part[0]}: a speck at 1x; make it 2x2 or "
                            "join it to its neighbour")
    return warnings


def frames(icon):
    """The frames to check: a flickering flame's every frame (tools/arms_art.py ANIMATED), else the map alone."""
    count = arms_art.ANIMATED.get(icon.name, (1, 0))[0]
    if count > 1 and any(ch in FLAME for row in icon.rows for ch in row):
        return [arms_icons.flicker(icon.rows, frame) for frame in range(count)]
    return [icon.rows]


def check_icon(icon):
    if not icon.shape_ok():
        return icon
    used_bare, any_holes = False, False
    for number, rows in enumerate(frames(icon)):
        problems, bare, holes = check_frame(icon, rows, f"frame {number}: " if number else "")
        icon.problems += [p for p in problems if p not in icon.problems]
        used_bare |= bare
        any_holes |= bool(holes)
    icon.warnings += style_warnings(icon)
    if "pinhole" in icon.allow and not any_holes:
        icon.warnings.append("declares '# allow: pinhole' but encloses no transparency; remove it")
    if "bare" in icon.allow and not used_bare:
        icon.warnings.append("declares '# allow: bare' but no such fill touches transparency; remove it")
    return icon


def check_map(path):
    return check_icon(Map(path))


def map_paths(targets=None):
    paths = []
    for target in targets or FOLDERS:
        if os.path.isdir(target):
            paths += sorted(os.path.join(target, f) for f in os.listdir(target) if f.endswith(".txt"))
        else:
            paths.append(target)
    return paths


def variant_line(name):
    """The Arms VII line of a variant's map, or None."""
    return arms_variants.BY_ID[name][1] if name in arms_variants.BY_ID else None


def palettes(icons=(), lines=True):
    """(label, material, role, distance, strict) for every material the maps are coloured in: each arms style's
    materials; each Arms VII line's (with `lines`, or when one of `icons` is a variant of that line; strict only once a
    variant of that line is drawn as a map); and each material a map declares."""
    out = []
    for name, style in arms_pixel.STYLES.items():
        for role, (_, part, _) in ROLES.items():
            out.append((name if role == "main" else f"{name} {part}", getattr(style, part), role, not exempt(name),
                        True))
    drawn = {variant_line(icon.name) for icon in icons}
    for line, style in arms_variants_art.LINE_STYLES.items():
        if not lines and line not in drawn:
            continue
        for role, (_, part, _) in ROLES.items():
            out.append((line if role == "main" else f"{line} {part}", getattr(style, part), role, not exempt(line),
                        line in drawn))
    declared = sorted({(name, role) for icon in icons for role, name in icon.materials.items()})
    for name, role in declared:
        label = name if role == "main" else f"{name} as {role}"
        out.append((label, icon_materials.MATERIALS[name], role, not exempt(name), True))
    return out


def palette_report(icons=()):
    lines = []
    for name, style in arms_pixel.STYLES.items():
        material = style.blade
        tones = (material.dark, material.mid, material.light, material.highlight)
        near = ", ".join(f"{other} {ramp_distance(tones, ramp):.1f}" for other, ramp in NEIGHBOURS.items())
        lines.append(f"palette {name}: outline {luma(material.outline_dark):.0f} luma, highlight "
                     f"{luma(material.highlight):.0f}; distance (mean CIEDE2000, D M L H) to {near}")
    declared = {name for icon in icons for role, name in icon.materials.items() if role == "main"}
    for name in sorted(declared - set(arms_pixel.STYLES)):
        material = icon_materials.MATERIALS[name]
        note = " (vanilla's own metal: no distance test)" if name in icon_materials.VANILLA else ""
        lines.append(f"palette {name}: outline {luma(material.outline_dark):.0f} luma, highlight "
                     f"{luma(material.highlight):.0f}{note}")
    return lines


def legacy_icons():
    with open(LEGACY, encoding="utf-8") as f:
        return [line.strip() for line in f if line.strip() and not line.startswith("#")]


def item_icon_sizes():
    """{texture name under textures/item/: width} for every layer of a flat item model (item/generated, item/handheld,
    a bow or crossbow): the item icons. 3D model textures, shields and block items are not icons."""
    models = os.path.join(ASSETS, "models", "item")

    def load(ref):
        space, _, path = ref.partition(":") if ":" in ref else ("jugcraft", "", ref)
        file = os.path.join(ASSETS, "models", path + ".json")
        if space != "jugcraft" or not os.path.isfile(file):
            return None
        with open(file, encoding="utf-8") as f:
            return json.load(f)

    from PIL import Image
    sizes = {}
    for entry in sorted(os.listdir(models)):
        if not entry.endswith(".json"):
            continue
        with open(os.path.join(models, entry), encoding="utf-8") as f:
            model = json.load(f)
        current, top = model, None
        for _ in range(10):
            if not current or "parent" not in current:
                break
            top = current["parent"]
            current = load(top)
        if not top or not any(kind in top for kind in ("item/generated", "item/handheld", "item/bow", "item/crossbow")):
            continue
        for key, ref in model.get("textures", {}).items():
            space, _, path = ref.partition(":") if ":" in ref else ("jugcraft", "", ref)
            if not key.startswith("layer") or space != "jugcraft" or not path.startswith("item/"):
                continue
            png = os.path.join(ASSETS, "textures", path + ".png")
            if os.path.isfile(png):
                with Image.open(png) as image:
                    sizes[path[len("item/"):]] = image.size[0]
    return sizes


def icon_size_check():
    """(errors, warnings): no item icon larger than 16x16 beyond the frozen legacy list (docs/ITEM_ICONS.md, rule 1)."""
    legacy, sizes = set(legacy_icons()), item_icon_sizes()
    errors = [f"textures/item/{name}.png: an item icon is 16x16, not {width}x{width} (docs/ITEM_ICONS.md, rule 1); "
              "only the icons in tools/legacy_item_icons.txt may stay larger until they are redrawn"
              for name, width in sorted(sizes.items()) if width > SIZE and name not in legacy]
    warnings = [f"tools/legacy_item_icons.txt: {name} is now {sizes[name]}x{sizes[name]}; remove it from the list"
                if name in sizes else f"tools/legacy_item_icons.txt: {name} is no longer an item icon; remove it"
                for name in sorted(legacy) if sizes.get(name, 0) <= SIZE]
    return errors, warnings


def where(path):
    return os.path.relpath(path, ROOT) if os.path.abspath(path).startswith(ROOT + os.sep) else path


def check(targets=None):
    """(errors, warnings) for the maps in `targets` (folders or files; default FOLDERS), the palettes, and (with no
    targets) the item icons' sizes."""
    errors, warnings, icons = [], [], []
    for path in map_paths(targets):
        icon = check_map(path)
        icons.append(icon)
        errors += [f"{where(path)}: {p}" for p in icon.problems]
        warnings += [f"{where(path)}: {w}" for w in icon.warnings]
    for name, material, role, distance, strict in palettes(icons, lines=not targets):
        found = palette_problems(name, material, role, distance)
        if strict:
            errors += found
        else:
            warnings += [f"{p} (no variant of this line is drawn as a map yet)" for p in found]
    if not targets:
        found, warned = icon_size_check()
        errors += found
        warnings += warned
    return errors, warnings


def errors(targets=None):
    """The errors alone, for tools/check_mod_data.py."""
    return check(targets)[0]


def self_test():
    """Break approved maps and palettes in known ways and confirm the checker catches each. Returns the breaks it
    missed (an empty list when the checker is sound)."""
    def text(name):
        with open(arms_icons.path(name), encoding="utf-8") as f:
            return f.read().split("\n")

    def rows_of(name):
        return Map(arms_icons.path(name)).rows

    def put(rows, x, y, ch):
        rows = list(rows)
        rows[y] = rows[y][:x] + ch + rows[y][x + 1:]
        return rows

    def first(rows, test):
        return next((x, y) for y in range(SIZE) for x in range(SIZE) if test(rows, x, y))

    base = rows_of("longsword")
    exposed = first(base, lambda r, x, y: r[y][x] == "O" and "." in [at(r, x + dx, y + dy) for dx, dy in N4]
                    and any((at(r, x + dx, y + dy) or ".") in BLADE for dx, dy in N4))
    lone = first(base, lambda r, x, y: all((at(r, x + dx, y + dy) or ".") == "." for dx, dy in N8) and r[y][x] == ".")
    corner = first(base, lambda r, x, y: x < SIZE - 1 and y < SIZE - 1 and r[y][x] == "O" and r[y][x + 1] == "."
                   and r[y + 1][x] == "." and r[y + 1][x + 1] == ".")
    thick = put(put(put(base, corner[0] + 1, corner[1], "O"), corner[0], corner[1] + 1, "O"),
                corner[0] + 1, corner[1] + 1, "O")
    middle = first(base, lambda r, x, y: r[y][x] == "M")
    flame = ["......g.........", ".....gfx........", "......gx........", ".......x........"] + ["." * SIZE] * 12
    no_reason = [line for line in text("chakram") if not line.startswith("#")] + ["# allow: pinhole"]
    cases = [
        ("15 rows", Map(lines=base[:15], name="longsword"), "rows of"),
        ("an unknown symbol", Map(lines=put(base, *middle, "Z"), name="longsword"), "unknown symbol"),
        ("host rock in an icon", Map(lines=put(base, *middle, "r"), name="longsword"), "host rock"),
        ("fill on the canvas edge", Map(lines=put(base, 0, 7, "M"), name="longsword"), "canvas edge"),
        ("fill touching transparency", Map(lines=put(base, *exposed, "."), name="longsword"), "touches transparency"),
        ("a stray outline pixel", Map(lines=put(base, *lone, "O"), name="longsword"), "no fill"),
        ("a 2x2 block of outline", Map(lines=thick, name="longsword"), "2x2"),
        ("an undeclared pinhole", Map(lines=[line for line in text("chakram") if not line.startswith("#")],
                                      name="chakram"), "pinhole"),
        ("an allowance without its reason", Map(lines=no_reason, name="chakram"), "reason"),
        ("an unknown family", Map(lines=base + ["# family: spoon"], name="longsword"), "unknown family"),
        ("an unknown material", Map(lines=base + ["# materials: main=unobtainium"], name="longsword"),
         "unknown material"),
        ("a role with no material", Map(lines=base + ["# materials: main=copper"], name="longsword"),
         "names no wood"),
        ("a flame frame", Map(lines=flame, name="brazier_mace"), "frame 3: fill 'f'"),
    ]
    missed = []
    for label, icon, expect in cases:
        found = check_icon(icon).problems
        if not any(expect in problem for problem in found):
            missed.append(f"self-test: {label} was not caught (expected '{expect}', got {found})")
    old_bronze = arms_pixel.Material((61, 27, 12), (106, 46, 20), (142, 64, 28), (196, 100, 46), (228, 140, 74),
                                     (248, 196, 138))
    bronze = arms_pixel.BRONZE
    ramps = [
        ("a black outline", arms_pixel.Material((0, 0, 0), *bronze.tones()[1:]), "pure black"),
        ("an outline too light", arms_pixel.Material(*generate_textures.COPPER_METAL[:1], *bronze.tones()[1:]),
         "looks soft"),
        ("D too close to O", arms_pixel.Material((53, 69, 20), (60, 80, 24), (78, 97, 29), (124, 138, 55),
                                                 (170, 176, 83), (219, 221, 133)), "merges"),
        ("a highlight jump", arms_pixel.Material((28, 18, 14), (40, 26, 18), (54, 34, 22), (78, 52, 36),
                                                 (110, 78, 54), (214, 202, 184)), "speck"),
        ("tones out of order", arms_pixel.Material(*bronze.tones()[:3], bronze.light, bronze.mid, bronze.highlight),
         "step from dark to light"),
        ("a bronze that reads as copper", old_bronze, "from copper"),
    ]
    for label, material, expect in ramps:
        if not any(expect in problem for problem in palette_problems("test", material)):
            missed.append(f"self-test: {label} was not caught (expected '{expect}')")
    return missed


def preview(paths, out):
    """Each map in its materials (its `# materials:` line, a variant's line, or every arms style): 8x on a light slot,
    then 2x and 1x on a light and a dark slot. Judge the 1x and 2x first: they are what a player sees."""
    from PIL import Image, ImageDraw
    icons = [Map(path) for path in paths]

    def styles(icon):
        if icon.materials:
            return [(" ".join(f"{r}={m}" for r, m in icon.materials.items()), icon.style())]
        if variant_line(icon.name):
            return [(variant_line(icon.name), arms_variants_art.LINE_STYLES[variant_line(icon.name)])]
        return list(arms_pixel.STYLES.items())

    block = 8 * SIZE + 4 + 2 * SIZE + 4 + 2 * SIZE + 12
    columns = max([len(styles(icon)) for icon in icons] + [1])
    sheet = Image.new("RGBA", (130 + columns * block, 12 + len(paths) * (8 * SIZE + 12)), (44, 44, 50, 255))
    draw = ImageDraw.Draw(sheet)
    for i, icon in enumerate(icons):
        y = 6 + i * (8 * SIZE + 12)
        draw.text((6, y + 4), icon.name, fill=(236, 236, 240))
        if not icon.shape_ok():
            draw.text((6, y + 20), "not a 16x16 map", fill=(232, 92, 84))
            continue
        for j, (label, style) in enumerate(styles(icon)):
            colours = dict(arms_icons.palette(style), **ROCK_COLOURS)
            image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
            for x, row_y, ch in Map.cells(icon.rows):
                image.putpixel((x, row_y), colours[ch])
            x = 130 + j * block
            draw.text((x, y - 4), label, fill=(178, 178, 188))
            for left, top, scale, slot in ((x, y + 8, 8, LIGHT_SLOT), (x + 8 * SIZE + 4, y + 8, 2, LIGHT_SLOT),
                                           (x + 8 * SIZE + 4, y + 8 + 2 * SIZE + 4, 2, DARK_SLOT),
                                           (x + 10 * SIZE + 8, y + 8, 1, LIGHT_SLOT),
                                           (x + 10 * SIZE + 8, y + 8 + 2 * SIZE + 4, 1, DARK_SLOT)):
                draw.rectangle((left, top, left + scale * SIZE - 1, top + scale * SIZE - 1), fill=slot)
                sheet.alpha_composite(image.resize((scale * SIZE, scale * SIZE), Image.NEAREST), (left, top))
    sheet.save(out)
    return out


def main(argv):
    args = argv[1:]
    if "--self-test" in args:
        missed = self_test()
        for line in missed:
            print(line, file=sys.stderr)
        print(f"{'FAIL' if missed else 'PASS'}: self-test, {len(missed)} break(s) not caught.")
        return 1 if missed else 0
    out = None
    if "--preview" in args:
        at_flag = args.index("--preview")
        named = at_flag + 1 < len(args) and args[at_flag + 1].lower().endswith(".png")
        out = args[at_flag + 1] if named else os.path.join(tempfile.gettempdir(), "icon_maps.png")
        args = args[:at_flag] + args[at_flag + (2 if named else 1):]
    targets = args or None
    if out:
        print("preview: " + preview(map_paths(targets), out))
    found, warned = check(targets)
    icons = [Map(path) for path in map_paths(targets)]
    for line in palette_report(icons):
        print(line)
    for line in warned:
        print("warning: " + line)
    declared = {name for icon in icons for name in icon.materials.values()}
    lines = len({variant_line(icon.name) for icon in icons} - {None}) if targets else len(arms_variants_art.LINE_STYLES)
    count = (f"{len(map_paths(targets))} icon map(s); the materials of {len(arms_pixel.STYLES)} arms styles, {lines} "
             f"Arms VII line(s) and {len(declared)} declared material(s)")
    if not targets:
        count += f", {len(item_icon_sizes())} item icons' sizes ({len(legacy_icons())} legacy)"
    if found:
        print("\n".join(found), file=sys.stderr)
        print(f"FAIL: {len(found)} error(s) in {count}.", file=sys.stderr)
        return 1
    print(f"PASS: {count}, {len(warned)} warning(s). A lint of the maps only: look at the icons at 1x and 2x, and in "
          "game.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
