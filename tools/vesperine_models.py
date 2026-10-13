"""GeckoLib bodies for Vesperine, the Last Reaper, and her fight (docs/features/vesperine.md): Vesperine herself, her
skulls Dirge and Requiem (one model), her thrown scythe and her grave thralls. Geometry and animations only, as plain
dicts; no I/O. tools/vesperine_art.py paints their sheets on the same box-UV regions, at TEXTURE_SCALE times the size the
models declare (docs/ART_DIRECTION.md, "High resolution").

Units are pixels (16 = one block); y = 0 is the entity's feet and every model faces north (-z), its left on +x, as the
other GeckoLib bodies (tools/concordance_worker_models.py, which sets out the rotation conventions: +x pitches a bone's
top forward, -x raises a hanging arm forward, a right arm turns outwards with +z and a left one with -z, +y turns the
front to the model's right). A keyframed rotation adds to a bone's rest rotation.

Every cube is named by its part; cubes of one part share one box-UV region (a left limb's mirrored cubes reuse the
right's), and `Model.pack` lays the regions out on the sheet without overlaps. GEO, ANIMATIONS and SHEETS are keyed by
entity id (Dirge and Requiem share the skull's model, as `reaper_skull`); CLIPS lists the clips the Java plays.
"""

import math

TEXTURE_SCALE = 4  # every sheet is painted four times finer than its model declares


# ------------------------------------------------------------------------------------------------- the builder

class Model:
    """A GeckoLib body built bone by bone, its cubes' box-UV regions packed onto a sheet `texture` pixels square."""

    def __init__(self, identifier, texture, bounds):
        self.identifier = identifier
        self.texture = texture
        self.bounds = bounds  # visible bounds (width, height) in blocks
        self.bones = []
        self.parts = {}  # part -> (w, h, d)
        self.uv = {}

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        bone = {"name": name, "pivot": [float(v) for v in pivot]}
        if parent:
            bone["parent"] = parent
        if rotation:
            bone["rotation"] = [float(v) for v in rotation]
        bone["cubes"] = []
        self.bones.append(bone)
        return name

    def cube(self, bone, part, origin, size, rotation=None, pivot=None, mirror=False):
        size = tuple(int(v) for v in size)
        if self.parts.setdefault(part, size) != size:
            raise ValueError(f"{self.identifier}: part {part} is {self.parts[part]}, not {size}")
        cube = {"origin": [round(float(v), 4) for v in origin], "size": list(size), "part": part}
        if rotation:
            cube["rotation"] = [round(float(v), 4) for v in rotation]
            cube["pivot"] = [round(float(v), 4) for v in (pivot or origin)]
        if mirror:
            cube["mirror"] = True
        next(b for b in self.bones if b["name"] == bone)["cubes"].append(cube)

    @staticmethod
    def region(size):
        w, h, d = size
        return 2 * (w + d), d + h

    def pack(self):
        """Shelf packing, tallest regions first: deterministic, and every region inside the sheet."""
        order = sorted(self.parts, key=lambda p: (-self.region(self.parts[p])[1], -self.region(self.parts[p])[0], p))
        x = y = shelf = 0
        for part in order:
            w, h = self.region(self.parts[part])
            if x + w > self.texture:
                x, y, shelf = 0, y + shelf, 0
            if y + h > self.texture or w > self.texture:
                raise ValueError(f"{self.identifier}: the parts do not fit a {self.texture} sheet")
            self.uv[part] = (x, y)
            x += w
            shelf = max(shelf, h)
        return self.uv

    def geo(self):
        if not self.uv:
            self.pack()
        bones = []
        for bone in self.bones:
            out = {k: v for k, v in bone.items() if k != "cubes"}
            if bone["cubes"]:
                out["cubes"] = []
                for cube in bone["cubes"]:
                    c = {k: v for k, v in cube.items() if k != "part"}
                    c["uv"] = list(self.uv[cube["part"]])
                    out["cubes"].append(c)
            bones.append(out)
        width, height = self.bounds
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{self.identifier}", "texture_width": self.texture,
                            "texture_height": self.texture, "visible_bounds_width": width,
                            "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
            "bones": bones}]}

    def regions(self):
        """Each part's (u, v, w, h, d): where tools/vesperine_art.py paints it."""
        if not self.uv:
            self.pack()
        return {part: (*self.uv[part], *self.parts[part]) for part in self.parts}


# ------------------------------------------------------------------------------------------------- animation helpers

def _key(seconds):
    return str(round(seconds, 4))


def _vec(values):
    return [round(value, 3) + 0.0 for value in values]  # + 0.0 turns -0.0 into 0.0


def still(values):
    """A pose held for the whole clip."""
    return {"0.0": _vec(values)}


def keys(frames):
    """Keyframes from {seconds: (x, y, z)}."""
    return {_key(t): _vec(v) for t, v in sorted(frames.items())}


def wave(length, cycles, base, amplitude, phase=0.0, steps=8):
    """Keyframes of base + amplitude * sin(2 pi (cycles t / length + phase)), sampled `steps` times a cycle; the first
    and last keys match, so a looping clip is seamless."""
    out = {}
    total = steps * cycles
    for i in range(total + 1):
        s = math.sin(2 * math.pi * (i / steps + phase))
        out[_key(length * i / total)] = _vec([b + a * s for b, a in zip(base, amplitude)])
    return out


def clip(length, bones, loop=True):
    return {"loop": True if loop else "hold_on_last_frame", "animation_length": length, "bones": bones}


def animations(clips):
    return {"format_version": "1.8.0", "animations": clips}


# ------------------------------------------------------------------------------------------------- the scythe

# The Vesper Scythe as she carries it: a black pole bound with silver rings, a socket and a finial at its head, and a
# moon-pale blade, five turned segments tapering to a point, sweeping out to the bearer's right and curving down. Built
# with the pole upright through the grip (the hand's middle) at GRIP.
BLADE = ((7, 6, 10), (7, 5, -4), (7, 4, -18), (6, 3, -34), (5, 2, -52))  # length, width, angle from level (degrees)


def scythe(model, bone, grip):
    """The scythe's cubes on `bone`, the pole upright through `grip` (x, y, z); returns the blade's tip."""
    gx, gy, gz = grip
    top = gy + 33.0
    model.cube(bone, "pole", (gx - 1, gy - 17, gz - 1), (2, 50, 2))
    model.cube(bone, "butt", (gx - 1.5, gy - 18, gz - 1.5), (3, 2, 3))
    for y in (gy + 10, gy + 20, gy + 28):
        model.cube(bone, "ring", (gx - 1.5, y, gz - 1.5), (3, 1, 3))
    model.cube(bone, "socket", (gx - 2, top - 4, gz - 2), (4, 4, 4))
    model.cube(bone, "finial", (gx - 1, top, gz - 1), (2, 3, 2))
    # The blade: each segment hangs from the end of the last along its spine (its top edge), overlapping it by a pixel
    # so the turn leaves no gap, and turned down a little more.
    x, y = gx - 2.0, top - 0.5
    for i, (length, width, angle) in enumerate(BLADE):
        model.cube(bone, f"blade_{i}", (x - length, y - width, gz - 0.5), (length, width, 1), rotation=(0, 0, angle),
                   pivot=(x, y, gz))
        a = math.radians(angle)
        dx = -(length - 1)
        x, y = x + dx * math.cos(a), y - dx * math.sin(a)
    return x, y


# ------------------------------------------------------------------------------------------------- Vesperine

# Tall and pale (her model stands 3 blocks to the top of her head), in layered black armour over a torn black robe that
# ends in tatters above the ground (she glides), black hair streaming behind her, a square double halo behind her head,
# her scythe in her right hand. Seated on the Bone Throne she waits; her skirt bends at the knee as she sits.
VESPERINE_GRIP = (-6.5, 16.0, 0.0)


def vesperine_model():
    m = Model("vesperine", 128, (4.0, 4.0))
    m.bone("root")
    m.bone("hips", "root", (0, 22, 0))
    m.cube("hips", "waist", (-4, 22, -3), (8, 4, 6))
    m.cube("hips", "belt", (-5, 21.5, -3.5), (10, 2, 7))
    # The robe: its upper skirt, the armoured tassets over it, its lower skirt and the tatters at its hem.
    m.bone("skirt", "hips", (0, 22, 0))
    m.cube("skirt", "skirt_top", (-6, 14, -4), (12, 8, 8))
    m.cube("skirt", "tasset", (-5, 15, -5), (4, 6, 1))
    m.cube("skirt", "tasset", (1, 15, -5), (4, 6, 1), mirror=True)
    m.bone("skirt_low", "skirt", (0, 14.5, 0))
    m.cube("skirt_low", "skirt_low", (-7, 4, -5), (14, 10, 10))
    m.bone("tatters_front", "skirt_low", (0, 4, -5))
    m.cube("tatters_front", "tatters", (-7, -2, -5), (14, 6, 0))
    m.bone("tatters_back", "skirt_low", (0, 4, 5))
    m.cube("tatters_back", "tatters", (-7, -2, 5), (14, 6, 0))
    m.bone("tatters_right", "skirt_low", (-7, 4, 0))
    m.cube("tatters_right", "tatters_side", (-7, -2, -5), (0, 6, 10))
    m.bone("tatters_left", "skirt_low", (7, 4, 0))
    m.cube("tatters_left", "tatters_side", (7, -2, -5), (0, 6, 10), mirror=True)
    # The torso: chest, breastplate and gorget.
    m.bone("torso", "hips", (0, 26, 0))
    m.cube("torso", "chest", (-5, 26, -3), (10, 10, 6))
    m.cube("torso", "breastplate", (-4, 28, -4), (8, 7, 1))
    m.cube("torso", "gorget", (-4, 35, -3.5), (8, 2, 7))
    # The head: neck, face, the hair over it and the locks beside it; the long hair behind; the halo floating behind.
    m.bone("head", "torso", (0, 37, 0))
    m.cube("head", "neck", (-1.5, 36, -1.5), (3, 2, 3))
    m.cube("head", "face", (-4, 38, -4), (8, 9, 8))
    m.cube("head", "hair_cap", (-4.5, 44, -3.5), (9, 4, 8))
    m.cube("head", "lock", (-5, 37, -3.5), (2, 9, 2))
    m.cube("head", "lock", (3, 37, -3.5), (2, 9, 2), mirror=True)
    m.bone("hair_back", "head", (0, 46, 4.5))
    m.cube("hair_back", "hair_back", (-4.5, 35, 3.5), (9, 12, 3))
    m.bone("hair_tail", "hair_back", (0, 35, 5))
    m.cube("hair_tail", "hair_tail", (-4, 21, 4.5), (8, 14, 2))
    m.bone("halo", "head", (0, 44, 9))
    m.bone("halo_inner", "halo", (0, 44, 9))
    for y in (48.5, 39.5):
        m.cube("halo_inner", "halo_inner_bar", (-5, y, 8.5), (10, 1, 1))
    for x in (-5, 4):
        m.cube("halo_inner", "halo_inner_post", (x, 40.5, 8.5), (1, 8, 1))
    m.bone("halo_outer", "halo", (0, 44, 9.5), rotation=(0, 0, 45))
    for y in (50.5, 37.5):
        m.cube("halo_outer", "halo_outer_bar", (-7, y, 9), (14, 1, 1))
    for x in (-7, 6):
        m.cube("halo_outer", "halo_outer_post", (x, 38.5, 9), (1, 12, 1))
    # The cape, in two halves so it can ripple.
    m.bone("cape", "torso", (0, 36, 3.5))
    m.cube("cape", "cape", (-6, 23, 3), (12, 13, 1))
    m.bone("cape_low", "cape", (0, 23, 3.5))
    m.cube("cape_low", "cape_low", (-6, 10, 3), (12, 13, 1))
    # The arms, in layered pauldrons and gauntlets; the left mirrors the right.
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0

        def x(v):  # a right-side x, mirrored to the left
            return v if side < 0 else -v

        def ox(v, w):  # a right-side origin x for a cube `w` wide, mirrored
            return v if side < 0 else -v - w

        m.bone(f"arm_{name}", "torso", (x(-6.5), 34.5, 0))
        m.cube(f"arm_{name}", "pauldron", (ox(-9.5, 6), 33, -3), (6, 4, 6), mirror=mirror)
        m.cube(f"arm_{name}", "pauldron_low", (ox(-9, 5), 31.5, -2.5), (5, 2, 5), mirror=mirror)
        m.cube(f"arm_{name}", "upper_arm", (ox(-8, 3), 25, -1.5), (3, 9, 3), mirror=mirror)
        m.bone(f"forearm_{name}", f"arm_{name}", (x(-6.5), 25.5, 0))
        m.cube(f"forearm_{name}", "gauntlet", (ox(-8.5, 4), 18, -2), (4, 7, 4), mirror=mirror)
        m.cube(f"forearm_{name}", "cuff", (ox(-9, 5), 23, -2.5), (5, 2, 5), mirror=mirror)
        m.bone(f"hand_{name}", f"forearm_{name}", (x(-6.5), 18, 0))
        m.cube(f"hand_{name}", "hand", (ox(-8, 3), 14, -1.5), (3, 4, 3), mirror=mirror)
    # The grip turns the scythe in her hand (the body's clips); the scythe itself is shown or hidden (the scythe
    # controller's clips: she is unarmed while it is thrown). A controller sets every channel of each bone it animates,
    # so no two controllers share a bone: the halo's bones belong to the halo controller alone, likewise the scythe's.
    m.bone("scythe_grip", "hand_right", VESPERINE_GRIP)
    m.bone("scythe", "scythe_grip", VESPERINE_GRIP)
    scythe(m, "scythe", VESPERINE_GRIP)
    return m


# ------------------------------------------------------------------------------------------------- the thrown scythe

def thrown_scythe_model():
    """Her scythe thrown: it flies flat, spinning about its balance point a little up the pole (at the entity's middle)."""
    m = Model("thrown_scythe", 64, (3.0, 1.0))
    m.bone("root")
    m.bone("spin", "root", (0, 4, 0))
    # The pole lies along z, the blade flat; the grip sits 12 pixels behind the spin's centre.
    m.bone("blade_plane", "spin", (0, 4, 0), rotation=(90, 0, 0))
    scythe(m, "blade_plane", (6.5, -12.0, 0))
    return m


# ------------------------------------------------------------------------------------------------- the skulls

# Dirge and Requiem: great dark skulls, a block across, with square sockets under a heavy brow, a jaw that drops open
# when a skull looses a bolt, and black wisps trailing behind. One model; each has its own sheet (Dirge's glow is soul
# blue, Requiem's red).
def skull_model():
    m = Model("reaper_skull", 128, (2.0, 1.5))
    m.bone("root")
    m.bone("skull", "root", (0, 10, 0))
    m.cube("skull", "cranium", (-8, 7, -8), (16, 12, 16))
    m.cube("skull", "brow", (-8, 14, -9), (16, 2, 1))
    m.cube("skull", "maxilla", (-6, 3.5, -8.5), (12, 4, 12))
    m.bone("jaw", "skull", (0, 4, 2))
    m.cube("jaw", "jaw", (-6, 0.5, -8.5), (12, 3, 11))
    m.bone("wisps", "skull", (0, 13, 8))
    m.cube("wisps", "wisp_middle", (0, 6, 8), (0, 12, 14))
    m.bone("wisp_right", "wisps", (-5, 13, 8))
    m.cube("wisp_right", "wisp_side", (-5, 8, 8), (0, 10, 12))
    m.bone("wisp_left", "wisps", (5, 13, 8))
    m.cube("wisp_left", "wisp_side", (5, 8, 8), (0, 10, 12), mirror=True)
    return m


# ------------------------------------------------------------------------------------------------- the grave thrall

# A grave thrall: a skeleton two blocks tall, its eyes soul flames, its ribs open (cut out between them), clawing its
# way out of the soil.
def thrall_model():
    m = Model("grave_thrall", 64, (1.5, 2.5))
    m.bone("root")
    m.bone("body", "root", (0, 12, 0))
    m.cube("body", "pelvis", (-3, 11, -1.5), (6, 2, 3))
    m.cube("body", "spine", (-1, 13, -0.5), (2, 10, 2))
    m.cube("body", "ribs", (-4, 17, -2), (8, 7, 4))
    m.bone("head", "body", (0, 24, 0))
    m.cube("head", "skull", (-4, 25, -4), (8, 7, 8))
    m.bone("jaw", "head", (0, 25.5, 2))
    m.cube("jaw", "thrall_jaw", (-3, 23, -4), (6, 2, 6))
    for side, name in ((-1, "right"), (1, "left")):
        mirror = side > 0
        m.bone(f"arm_{name}", "body", (side * 5, 23, 0))
        m.cube(f"arm_{name}", "arm", ((-6 if side < 0 else 4), 11, -1), (2, 12, 2), mirror=mirror)
        m.bone(f"leg_{name}", "root", (side * 2, 12, 0))
        m.cube(f"leg_{name}", "leg", ((-3 if side < 0 else 1), 0, -1), (2, 12, 2), mirror=mirror)
    return m


# ------------------------------------------------------------------------------------------------- posing

# Bones whose secondary motion (hair, cloth) a looping clip adds as waves.
CLOTH = ("hair_back", "hair_tail", "cape", "cape_low", "skirt", "skirt_low", "tatters_front", "tatters_back",
         "tatters_right", "tatters_left")


def posed(length, timeline, loop=False, waves=None):
    """A clip from a timeline of poses [(seconds, pose)]; a pose maps a bone to its rotation, or to {"rotation": ...,
    "position": ...}. Every bone a pose names is keyed at every time of the timeline (at rest where a pose leaves it out),
    so the clip moves smoothly from pose to pose; `waves` adds looping channels ({bone: {channel: keyframes}}) for bones
    the timeline does not key on that channel."""
    channels = {}
    for _, pose in timeline:
        for bone, value in pose.items():
            for channel in (value if isinstance(value, dict) else {"rotation": value}):
                channels.setdefault(bone, set()).add(channel)
    bones = {}
    for bone, wanted in channels.items():
        for channel in sorted(wanted):
            frames = {}
            for t, pose in timeline:
                value = pose.get(bone, {})
                value = value if isinstance(value, dict) else {"rotation": value}
                frames[t] = value.get(channel, (0, 0, 0))
            bones.setdefault(bone, {})[channel] = keys(frames)
    for bone, extra in (waves or {}).items():
        for channel, value in extra.items():
            bones.setdefault(bone, {}).setdefault(channel, value)
    return clip(length, bones, loop=loop)


def grip(pose, tilt=0.0, roll=0.0):
    """The pose with the scythe's grip turned back against the arm's pitch, so the pole stands upright (leaning `tilt`
    degrees forward and `roll` out to her right)."""
    pitch = sum(pose.get(bone, (0, 0, 0))[0] for bone in ("hips", "torso", "arm_right", "forearm_right", "hand_right"))
    out = dict(pose)
    out["scythe_grip"] = (tilt - pitch, 0, -roll - pose.get("arm_right", (0, 0, 0))[2])
    return out


def drift(length, cycles=1, scale=1.0, back=0.0):
    """Hair and cloth adrift (her hair floats as if underwater): each sways on its own beat, trailing `back` degrees."""
    s = scale
    return {
        "hair_back": {"rotation": wave(length, cycles, (10 + back, 0, 0), (5 * s, 0, 2 * s))},
        "hair_tail": {"rotation": wave(length, cycles, (8 + back * 0.5, 0, 0), (7 * s, 0, 3 * s), phase=0.15)},
        "cape": {"rotation": wave(length, cycles, (5 + back, 0, 0), (3 * s, 0, 1 * s), phase=0.05)},
        "cape_low": {"rotation": wave(length, cycles, (5 + back * 0.4, 0, 0), (5 * s, 0, 0), phase=0.2)},
        "skirt": {"rotation": wave(length, cycles, (back * 0.4, 0, 0), (2 * s, 0, 1 * s))},
        "skirt_low": {"rotation": wave(length, cycles, (2 + back * 0.3, 0, 0), (2 * s, 0, 0), phase=0.2)},
        "tatters_front": {"rotation": wave(length, cycles, (-8 + back * 0.5, 0, 0), (-6 * s, 0, 0), phase=0.3)},
        "tatters_back": {"rotation": wave(length, cycles, (8 + back, 0, 0), (6 * s, 0, 0), phase=0.1)},
        "tatters_right": {"rotation": wave(length, cycles, (back * 0.5, 0, 8), (0, 0, 5 * s), phase=0.4)},
        "tatters_left": {"rotation": wave(length, cycles, (back * 0.5, 0, -8), (0, 0, -5 * s), phase=0.4)},
    }


def hover(length, cycles=1, height=1.0, base=0.5):
    return {"root": {"position": wave(length, cycles, (0, base, 0), (0, height, 0), phase=-0.25)}}


IDLE = grip({"torso": (-2, 0, 0), "head": (6, 0, 0), "arm_right": (-25, 0, 8), "forearm_right": (-45, 0, 0),
             "arm_left": (-12, 0, -10), "forearm_left": (-30, 0, 0)}, roll=4)
SEATED = grip({"root": {"position": (0, -16.5, 0)}, "skirt": (-85, 0, 0), "skirt_low": (85, 0, 0), "torso": (-4, 0, 0),
               "head": (18, 0, 0), "arm_right": (-35, 0, 8), "forearm_right": (-50, 0, 0), "arm_left": (-40, 0, -8),
               "forearm_left": (-45, 0, 0), "cape": (-10, 0, 0), "tatters_front": (-10, 0, 0)}, tilt=-4)
# Arms flung wide, head back: the Last Toll, and Death's Harvest with her arms raised to the moon.
TOLL = {"torso": (-15, 0, 0), "head": (-30, 0, 0), "arm_right": (-20, 0, 70), "forearm_right": (-15, 0, 0),
        "arm_left": (-20, 0, -70), "forearm_left": (-15, 0, 0), "scythe_grip": (35, 0, -70)}
HARVEST = {"torso": (-15, 0, 0), "head": (-35, 0, 0), "arm_right": (-165, 0, 22), "forearm_right": (-10, 0, 0),
           "arm_left": (-165, 0, -22), "forearm_left": (-10, 0, 0), "scythe_grip": (0, 0, -22)}
FLOATING = {"hair_back": (75, 0, 0), "hair_tail": (35, 0, 0), "cape": (55, 0, 0), "cape_low": (25, 0, 0),
            "tatters_front": (-35, 0, 0), "tatters_back": (35, 0, 0), "tatters_right": (0, 0, 30),
            "tatters_left": (0, 0, -30)}
# The Reaping Arc: drawn back high and twisted to her right, then a sweep of the blade round to her left.
ARC_BACK = {"hips": (0, 110, 0), "torso": (-10, 15, 0), "head": (0, -95, 0), "arm_right": (-130, 0, 35),
            "forearm_right": (-20, 0, 0), "scythe_grip": (10, 0, 0), "arm_left": (-40, 0, -50),
            "forearm_left": (-20, 0, 0), "cape": (30, 0, 0), "hair_back": (20, 0, 0)}
ARC_THROUGH = {"hips": (8, -60, 0), "torso": (12, -20, 0), "head": (0, 70, 0), "arm_right": (-90, 0, 10),
               "forearm_right": (0, 0, 0), "scythe_grip": (-10, 0, 0), "arm_left": (30, 0, -60),
               "forearm_left": (-10, 0, 0), "cape": (45, 0, 15), "hair_back": (40, 0, 10), "skirt": (10, 0, 0)}
ARC_DONE = {"hips": (6, -120, 0), "torso": (10, -25, 0), "head": (0, 120, 0), "arm_right": (-70, 0, -20),
            "forearm_right": (-10, 0, 0), "scythe_grip": (0, 0, 0), "arm_left": (25, 0, -40),
            "forearm_left": (-10, 0, 0), "cape": (25, 0, 20), "hair_back": (30, 0, 15)}
# The Harvest Lunge: a crouch with her robes flaring, then a dash with the scythe thrust ahead.
CROUCH = {"root": {"position": (0, -4, 0)}, "hips": (30, 0, 0), "torso": (10, 0, 0), "head": (-35, 0, 0),
          "arm_right": (-15, 0, 25), "forearm_right": (-40, 0, 0), "scythe_grip": (60, 0, -20),
          "arm_left": (20, 0, -25), "forearm_left": (-30, 0, 0), "skirt": (-25, 0, 0), "skirt_low": (25, 0, 0),
          "tatters_front": (-35, 0, 0), "tatters_back": (35, 0, 0), "tatters_right": (0, 0, 30),
          "tatters_left": (0, 0, -30), "cape": (65, 0, 0), "cape_low": (20, 0, 0), "hair_back": (40, 0, 0)}
DASH = {"hips": (38, 0, 0), "torso": (8, 0, 0), "head": (-40, 0, 0), "arm_right": (-100, 0, 8),
        "forearm_right": (0, 0, 0), "scythe_grip": (-30, 0, 0), "arm_left": (45, 0, -20), "forearm_left": (-15, 0, 0),
        "skirt": (40, 0, 0), "skirt_low": (30, 0, 0), "tatters_front": (30, 0, 0), "tatters_back": (50, 0, 0),
        "cape": (80, 0, 0), "cape_low": (25, 0, 0), "hair_back": (75, 0, 0), "hair_tail": (35, 0, 0)}
# The Scythe Throw: hefted overhand behind her head, then whipped forward and let go.
HEFT = {"hips": (0, 30, 0), "torso": (-15, 10, 0), "head": (0, -35, 0), "arm_right": (-165, 0, 12),
        "forearm_right": (-35, 0, 0), "scythe_grip": (30, 90, 0), "arm_left": (-85, 0, -15),
        "forearm_left": (0, 0, 0), "cape": (20, 0, 0)}
LOOSE = {"hips": (12, -25, 0), "torso": (18, -10, 0), "head": (0, 25, 0), "arm_right": (-55, 0, 0),
         "forearm_right": (-5, 0, 0), "scythe_grip": (-60, 90, 0), "arm_left": (25, 0, -30),
         "forearm_left": (-15, 0, 0), "cape": (40, 0, 0), "hair_back": (35, 0, 0)}
# The Grave Call: arms raised, then thrust down at the soil.
CALL_UP = {"torso": (-12, 0, 0), "head": (-25, 0, 0), "arm_right": (-160, 0, 30), "forearm_right": (-15, 0, 0),
           "arm_left": (-160, 0, -30), "forearm_left": (-15, 0, 0), "scythe_grip": (0, 0, -30)}
CALL_DOWN = {"hips": (12, 0, 0), "torso": (18, 0, 0), "head": (25, 0, 0), "arm_right": (-45, 0, 45),
             "forearm_right": (-10, 0, 0), "arm_left": (-45, 0, -45), "forearm_left": (-10, 0, 0),
             "scythe_grip": (20, 0, -45), "cape": (35, 0, 0), "hair_back": (30, 0, 0)}
# Twin Beam: her left hand points the skulls along the line; Crop Circles: the scythe raised high, her left hand
# turned down at the rings.
POINT = grip({"torso": (4, 0, 0), "head": (4, 0, 0), "arm_right": (-25, 0, 10), "forearm_right": (-45, 0, 0),
              "arm_left": (-88, -10, -4), "forearm_left": (0, 0, 0)}, roll=4)
RAISE = {"torso": (-6, 0, 0), "head": (14, 0, 0), "arm_right": (-170, 0, 12), "forearm_right": (-10, 0, 0),
         "scythe_grip": (0, 0, -12), "arm_left": (-50, 0, -35), "forearm_left": (-20, 0, 0)}
# Shadow Step: she crouches into her cloak.
WRAP = {"root": {"position": (0, -3, 0)}, "hips": (20, 0, 0), "torso": (12, 0, 0), "head": (20, 0, 0),
        "arm_right": (-60, 0, -30), "forearm_right": (-50, 0, 0), "scythe_grip": (110, 0, 30),
        "arm_left": (-60, 0, 35), "forearm_left": (-55, 0, 0), "cape": (-25, 0, 0), "cape_low": (-15, 0, 0),
        "hair_back": (-10, 0, 0)}
# The slam that ends Death's Harvest: the scythe driven down, everything flung out.
SLAM = {"root": {"position": (0, -6, 0)}, "hips": (38, 0, 0), "torso": (15, 0, 0), "head": (5, 0, 0),
        "arm_right": (-70, 0, 12), "forearm_right": (-20, 0, 0), "scythe_grip": (115, 0, 0), "arm_left": (-15, 0, -55),
        "forearm_left": (-10, 0, 0), "cape": (70, 0, 0), "cape_low": (35, 0, 0), "hair_back": (60, 0, 0),
        "tatters_front": (-40, 0, 0), "tatters_back": (40, 0, 0), "tatters_right": (0, 0, 35),
        "tatters_left": (0, 0, -35), "skirt": (-20, 0, 0)}
RISEN = grip({"root": {"position": (0, 3, 0)}, "skirt": (12, 0, 0), "skirt_low": (8, 0, 0), "torso": (-8, 0, 0),
              "head": (-12, 0, 0), "arm_right": (-15, 0, 45), "forearm_right": (-20, 0, 0), "arm_left": (-15, 0, -45),
              "forearm_left": (-20, 0, 0), "cape": (30, 0, 0), "hair_back": (35, 0, 0)}, roll=10)


def vesperine_animations():
    """Her body's clips (the "body" controller), her halo's (one per number of living skulls) and her scythe's (in her
    hand or thrown). The body's never key a halo or scythe bone, nor theirs a body bone."""
    with_idle = {**IDLE, "root": {"position": (0, 0.5, 0)}}
    return animations({
        "animation.vesperine.seated": posed(4.0, [(0.0, SEATED), (4.0, SEATED)], loop=True, waves={
            "hair_back": {"rotation": wave(4.0, 1, (4, 0, 0), (2, 0, 0))},
            "hair_tail": {"rotation": wave(4.0, 1, (4, 0, 0), (3, 0, 1), phase=0.15)}}),
        "animation.vesperine.rise": posed(2.0, [(0.0, SEATED), (0.8, {**RISEN, "root": {"position": (0, -6, 0)}}),
                                                (1.4, RISEN), (2.0, with_idle)]),
        "animation.vesperine.idle": posed(3.0, [(0.0, IDLE), (3.0, IDLE)], loop=True,
                                          waves={**drift(3.0), **hover(3.0)}),
        "animation.vesperine.glide": posed(1.6, [(0.0, GLIDE), (1.6, GLIDE)], loop=True,
                                           waves={**drift(1.6, 2, 0.6, back=30), **hover(1.6, 1, 0.5, 1.0)}),
        "animation.vesperine.arc_windup": posed(0.6, [(0.0, with_idle), (0.6, ARC_BACK)]),
        "animation.vesperine.arc_strike": posed(0.5, [(0.0, ARC_BACK), (0.12, ARC_THROUGH), (0.25, ARC_DONE),
                                                      (0.5, with_idle)]),
        "animation.vesperine.lunge_windup": posed(0.5, [(0.0, with_idle), (0.5, CROUCH)]),
        "animation.vesperine.lunge": posed(0.4, [(0.0, DASH), (0.4, DASH)], loop=True, waves={
            "hair_tail": {"rotation": wave(0.4, 1, (40, 0, 0), (8, 0, 4))},
            "cape_low": {"rotation": wave(0.4, 1, (30, 0, 0), (10, 0, 0), phase=0.2)}}),
        "animation.vesperine.throw_windup": posed(0.7, [(0.0, with_idle), (0.7, HEFT)]),
        "animation.vesperine.throw": posed(0.6, [(0.0, HEFT), (0.15, LOOSE), (0.6, with_idle)]),
        "animation.vesperine.call": posed(1.5, [(0.0, with_idle), (0.6, CALL_UP), (1.1, CALL_DOWN), (1.5, CALL_DOWN)]),
        "animation.vesperine.toll": posed(1.0, [(0.0, {**TOLL, **FLOATING}), (1.0, {**TOLL, **FLOATING})], loop=True,
                                          waves={"head": {"rotation": wave(1.0, 1, (-30, 0, 0), (-4, 0, 0))}}),
        "animation.vesperine.beam": posed(1.0, [(0.0, POINT), (1.0, POINT)], loop=True,
                                          waves={**drift(1.0, 1, 0.6), **hover(1.0, 1, 0.4)}),
        "animation.vesperine.circles": posed(1.5, [(0.0, RAISE), (1.5, RAISE)], loop=True, waves={
            **drift(1.5, 1, 0.8), "hips": {"rotation": wave(1.5, 1, (0, 0, 0), (0, 15, 0))}}),
        "animation.vesperine.step": posed(0.5, [(0.0, with_idle), (0.25, WRAP), (0.5, WRAP)]),
        "animation.vesperine.harvest": posed(2.0, [(0.0, {**HARVEST, **FLOATING}), (2.0, {**HARVEST, **FLOATING})],
                                             loop=True, waves={
                "arm_right": {"rotation": wave(2.0, 1, (-165, 0, 22), (6, 0, 4))},
                "arm_left": {"rotation": wave(2.0, 1, (-165, 0, -22), (6, 0, -4), phase=0.5)}}),
        "animation.vesperine.slam": posed(0.6, [(0.0, {**HARVEST, **FLOATING}), (0.2, SLAM), (0.6, SLAM)]),
        # The halo: two square rings turning against each other while both skulls guard her; the inner alone, askew
        # and shuddering, with one; none with neither.
        "animation.vesperine.halo_double": clip(8.0, {
            "halo_inner": {"rotation": keys({0.0: (0, 0, 0), 8.0: (0, 0, 360)})},
            "halo_outer": {"rotation": keys({0.0: (0, 0, 0), 8.0: (0, 0, -360)})},
            "halo": {"position": wave(8.0, 2, (0, 0, 0), (0, 0.5, 0))}}),
        "animation.vesperine.halo_cracked": clip(1.0, {
            "halo_inner": {"rotation": wave(1.0, 2, (0, 0, 14), (0, 0, 3)), "position": still((0.5, -0.5, 0))},
            "halo_outer": {"scale": still((0, 0, 0))},
            "halo": {"position": still((0, 0, 0))}}),
        "animation.vesperine.halo_none": clip(1.0, {"halo": {"scale": still((0, 0, 0))},
                                                    "halo_inner": {"rotation": still((0, 0, 0))},
                                                    "halo_outer": {"rotation": still((0, 0, 0))}}),
        "animation.vesperine.armed": clip(1.0, {"scythe": {"scale": still((1, 1, 1))}}),
        "animation.vesperine.unarmed": clip(1.0, {"scythe": {"scale": still((0, 0, 0))}}),
    })


GLIDE = grip({"hips": (16, 0, 0), "torso": (4, 0, 0), "head": (-14, 0, 0), "arm_right": (-10, 0, 18),
              "forearm_right": (-50, 0, 0), "arm_left": (35, 0, -12), "forearm_left": (-20, 0, 0)}, tilt=20, roll=8)


def thrown_scythe_animations():
    return animations({"animation.thrown_scythe.spin": clip(0.5, {"spin": {"rotation": keys({0.0: (0, 0, 0),
                                                                                              0.5: (0, -360, 0)})}})})


def skull_animations():
    """Floating: a slow bob and roll, the wisps streaming. Open: the jaw drops (as it looses a Grief Bolt) and holds."""
    wisps = {"wisps": {"rotation": wave(3.0, 1, (8, 0, 0), (6, 0, 0))},
             "wisp_right": {"rotation": wave(3.0, 2, (0, 12, 0), (4, 6, 0), phase=0.2)},
             "wisp_left": {"rotation": wave(3.0, 2, (0, -12, 0), (4, -6, 0), phase=0.45)}}
    return animations({
        "animation.reaper_skull.float": clip(3.0, {
            "root": {"position": wave(3.0, 1, (0, 0, 0), (0, 1.5, 0))},
            "skull": {"rotation": wave(3.0, 1, (0, 0, 0), (3, 0, 4), phase=0.25)},
            "jaw": {"rotation": wave(3.0, 2, (4, 0, 0), (3, 0, 0))}, **wisps}),
        "animation.reaper_skull.open": clip(0.5, {
            "root": {"position": still((0, 0, 0))},
            "skull": {"rotation": keys({0.0: (0, 0, 0), 0.25: (-12, 0, 0), 0.5: (-10, 0, 0)})},
            "jaw": {"rotation": keys({0.0: (4, 0, 0), 0.2: (38, 0, 0), 0.5: (34, 0, 0)})}, **wisps}, loop=False),
    })


def thrall_animations():
    """Emerging: it claws its way up out of the soil and holds there standing. Idle: swaying, arms reaching. Walking:
    a shambling stride. Attack (played once over whatever it is doing): both arms raised and brought down."""
    reach = {"arm_right": (-75, 0, 4), "arm_left": (-75, 0, -4)}
    return animations({
        "animation.grave_thrall.emerge": posed(1.0, [
            (0.0, {"root": {"position": (0, -30, 0)}, "body": (-20, 0, 0), "head": (-30, 0, 0),
                   "arm_right": (-170, 0, 15), "arm_left": (-170, 0, -15), "leg_right": (0, 0, 0),
                   "leg_left": (0, 0, 0)}),
            (0.45, {"root": {"position": (0, -14, 0)}, "body": (25, 0, 0), "head": (-10, 0, 0),
                    "arm_right": (-120, 0, 30), "arm_left": (-100, 0, -30), "leg_right": (0, 0, 0),
                    "leg_left": (0, 0, 0)}),
            (0.75, {"root": {"position": (0, -3, 0)}, "body": (30, 0, 0), "head": (10, 0, 0),
                    "arm_right": (-40, 0, 20), "arm_left": (-30, 0, -20), "leg_right": (-50, 0, 0),
                    "leg_left": (10, 0, 0)}),
            (1.0, {"root": {"position": (0, 0, 0)}, "body": (8, 0, 0), "head": (0, 0, 0), **reach,
                   "leg_right": (0, 0, 0), "leg_left": (0, 0, 0)})]),
        "animation.grave_thrall.idle": clip(2.5, {
            "body": {"rotation": wave(2.5, 1, (8, 0, 0), (2, 0, 3))},
            "head": {"rotation": wave(2.5, 1, (0, 0, 0), (4, 8, 6), phase=0.3)},
            "jaw": {"rotation": wave(2.5, 2, (6, 0, 0), (5, 0, 0))},
            "arm_right": {"rotation": wave(2.5, 1, (-75, 0, 4), (5, 0, 3))},
            "arm_left": {"rotation": wave(2.5, 1, (-75, 0, -4), (-5, 0, -3), phase=0.5)}}),
        "animation.grave_thrall.walk": clip(1.0, {
            "body": {"rotation": wave(1.0, 1, (10, 0, 0), (0, 4, 4))},
            "head": {"rotation": wave(1.0, 1, (0, 0, 0), (0, -6, -5))},
            "arm_right": {"rotation": wave(1.0, 1, (-80, 0, 4), (12, 0, 0))},
            "arm_left": {"rotation": wave(1.0, 1, (-80, 0, -4), (-12, 0, 0))},
            "leg_right": {"rotation": wave(1.0, 1, (0, 0, 0), (32, 0, 0))},
            "leg_left": {"rotation": wave(1.0, 1, (0, 0, 0), (-32, 0, 0))}}),
        "animation.grave_thrall.attack": posed(0.5, [
            (0.0, {"arm_right": (-75, 0, 4), "arm_left": (-75, 0, -4), "jaw": (0, 0, 0), "body": (8, 0, 0)}),
            (0.15, {"arm_right": (-165, 0, 10), "arm_left": (-165, 0, -10), "jaw": (30, 0, 0), "body": (-8, 0, 0)}),
            (0.3, {"arm_right": (-40, 0, 6), "arm_left": (-40, 0, -6), "jaw": (10, 0, 0), "body": (22, 0, 0)}),
            (0.5, {"arm_right": (-75, 0, 4), "arm_left": (-75, 0, -4), "jaw": (6, 0, 0), "body": (8, 0, 0)})]),
    })


# ------------------------------------------------------------------------------------------------- the tables

MODELS = {"vesperine": vesperine_model, "thrown_scythe": thrown_scythe_model, "reaper_skull": skull_model,
          "grave_thrall": thrall_model}
ANIMATIONS = {"vesperine": vesperine_animations, "thrown_scythe": thrown_scythe_animations,
              "reaper_skull": skull_animations, "grave_thrall": thrall_animations}
# Each entity's model and animations: Dirge and Requiem are both a reaper skull, each with its own sheet.
ENTITY_MODEL = {"vesperine": "vesperine", "dirge": "reaper_skull", "requiem": "reaper_skull",
                "thrown_scythe": "thrown_scythe", "grave_thrall": "grave_thrall"}
# The clips the Java names (VesperineEntity, ReaperSkullEntity, ThrownScytheEntity, GraveThrallEntity).
CLIPS = {
    "vesperine": ("seated", "rise", "idle", "glide", "arc_windup", "arc_strike", "lunge_windup", "lunge", "throw_windup",
                  "throw", "call", "toll", "beam", "circles", "step", "harvest", "slam", "halo_none", "halo_cracked",
                  "halo_double", "armed", "unarmed"),
    "reaper_skull": ("float", "open"),
    "thrown_scythe": ("spin",),
    "grave_thrall": ("emerge", "idle", "walk", "attack"),
}
# The bones each of Vesperine's controllers owns: no bone is animated by two.
CONTROLLERS = {"halo": ("halo", "halo_inner", "halo_outer"), "scythe": ("scythe",)}
