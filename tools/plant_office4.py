"""Round 7 of the plant kit: the small things done properly, all with depth.

A desk phone (handset on its cradle, angled keypad, display, coiled cord); the kitchen's microwave and bean-to-cup
coffee machine each on their own stretch of counter; the water chemistry analyzer (an instrument with a recessed
screen and an autosampler carousel of vials); fire extinguishers in glass-fronted wall cabinets; magazines with
real covers; food on the break room tables (takeout noodles, a gyro plate, a burger and fries, a salad, coffee and
a croissant); a plain mug.
"""
import random

import numpy as np
from PIL import Image, ImageDraw

import plant_office3 as O3
import plant_office5 as O5
import plant_office6 as O6

S = 128


def phone(x, z, y0):
    """A desk phone, about 4.6 x 4 px: the base, the keypad on a slope, the display, the handset on its cradle
    with the earpiece and mouthpiece bumps, the coiled cord."""
    return [
        (x, y0, z, x + 4.6, y0 + 0.7, z + 4, "pi_phone_body"),
        (x + 1.7, y0 + 0.7, z + 0.3, x + 4.4, y0 + 1.0, z + 3.7, {"*": "pi_phone_body", "up": "pi_phone_keys!"}),
        (x + 1.9, y0 + 1.0, z + 0.4, x + 4.2, y0 + 1.6, z + 1.3, {"*": "pi_phone_body", "south": "pi_phone_lcd!"}),
        (x + 0.1, y0 + 0.7, z + 0.2, x + 1.5, y0 + 1.1, z + 3.8, "pi_phone_body"),
        (x + 0.25, y0 + 1.1, z + 0.4, x + 1.35, y0 + 1.7, z + 1.1, "pi_phone_body"),
        (x + 0.25, y0 + 1.1, z + 2.9, x + 1.35, y0 + 1.7, z + 3.6, "pi_phone_body"),
        (x + 0.35, y0 + 1.1, z + 1.1, x + 1.25, y0 + 1.4, z + 2.9, "pi_phone_body"),
        (x - 0.4, y0, z + 3.2, x + 0.1, y0 + 0.4, z + 3.6, "pi_phone_cord"),
        (x - 0.6, y0, z + 3.6, x - 0.2, y0 + 0.4, z + 5.0, "pi_phone_cord"),
    ]


def mug(x, z, y0):
    """A plain white mug with coffee in it and a handle."""
    w, h, t = 2.4, 2.8, 0.3
    m = "pi_mug_plain"
    return [(x, y0, z, x + w, y0 + 0.3, z + w, m),
            (x, y0, z, x + t, y0 + h, z + w, m), (x + w - t, y0, z, x + w, y0 + h, z + w, m),
            (x + t, y0, z, x + w - t, y0 + h, z + t, m), (x + t, y0, z + w - t, x + w - t, y0 + h, z + w, m),
            (x + t, y0 + 0.3, z + t, x + w - t, y0 + h - 0.5, z + w - t, {"*": "pi_coffee", "up": "pi_coffee"}),
            (x + w, y0 + 0.7, z + 0.9, x + w + 0.9, y0 + 0.95, z + 1.5, m),
            (x + w + 0.65, y0 + 0.95, z + 0.9, x + w + 0.9, y0 + 1.95, z + 1.5, m),
            (x + w, y0 + 1.95, z + 0.9, x + w + 0.9, y0 + 2.2, z + 1.5, m)]


pen_cup = O5.pen_cup     # round 8: each pen in its own slice of the cup, none touching


# ---- kitchen appliances (each sits on a counter top at y 15, on the counter's own block)

MICROWAVE = [
    (1.2, 15, 2, 14.8, 15.4, 11.6, "pi_toe_kick"),                                     # feet
    (1.2, 15.4, 2, 14.8, 23, 11, {"*": "pi_microwave_case", "up": "pi_microwave_case"}),
    (1.4, 15.6, 11, 10.8, 22.8, 11.5, {"*": "pi_microwave_case", "south": "pi_microwave_door!"}),
    (10.8, 15.6, 11, 14.6, 22.8, 11.3, {"*": "pi_microwave_case", "south": "pi_microwave_panel!"}),
    (10.1, 16.6, 11.5, 10.6, 21.8, 12.2, "pi_chrome"),                                # the door handle
    (10.1, 16.6, 11.5, 10.6, 17.0, 11.5, "pi_chrome"),
]

COFFEE_MACHINE = [
    # The body, the bean hopper and its lid on top, the water tank at the side (see-through).
    (4, 15, 2.5, 12, 24.5, 9.5, {"*": "pi_appliance", "up": "pi_appliance"}),
    (5, 24.5, 3.5, 9, 25.2, 7.5, {"*": "pi_appliance", "up": "pi_hopper_lid!"}),
    (12, 15.6, 3, 13.6, 23.5, 9, "pi_bottle_water"),
    # The front: the panel with its display and buttons up top, the recess where the cup goes, the spouts.
    (4, 20.5, 9.5, 12, 24.5, 12, {"*": "pi_appliance", "south": "pi_coffee_panel!"}),
    (4, 15, 9.5, 5, 20.5, 12, "pi_appliance"), (11, 15, 9.5, 12, 20.5, 12, "pi_appliance"),
    (5, 15, 9.5, 11, 20.5, 9.8, "pi_steel_dark"),
    (6.8, 19.6, 10, 9.2, 20.5, 11.2, "pi_chrome"),
    (7.2, 19.0, 10.4, 7.8, 19.6, 10.9, "pi_chrome"), (8.2, 19.0, 10.4, 8.8, 19.6, 10.9, "pi_chrome"),
    (4.6, 15, 9.8, 11.4, 15.6, 13, {"*": "pi_steel_dark", "up": "pi_drip_grate!"}),
] + mug(6.8, 10.1, 15.6)


ANALYZER = [
    # The instrument: the case, its name across the top on a blue band, the display in a bezel below it with the
    # status lights beside it, vents down the side.
    (2, 14.5, 2, 14, 23, 10, {"*": "pi_instrument", "south": "pi_instrument_front!", "east": "pi_instrument_vents!",
                              "west": "pi_instrument_vents!"}),
    (2, 23, 2.3, 14, 23.5, 9.7, "pi_instrument"),
    (2.4, 21.4, 10, 13.6, 22.6, 10.2, {"*": "pi_badge_blue", "south": ("pi_analyzer_badge", [0, 0, 16, 1.7])}),
    (7.8, 15.6, 10, 13.4, 20.8, 10.6, {"*": "pi_steel_dark", "south": "pi_analyzer_screen!"}),
    (2.6, 19.6, 10, 7.0, 20.8, 10.4, {"*": "pi_steel_dark", "south": "pi_analyzer_leds!"}),
    # The autosampler in front: its turntable with vials, the post and arm with the needle over a vial.
    (2.4, 14.5, 10.6, 9, 15.3, 15.4, {"*": "pi_instrument", "up": "pi_sampler_top!"}),
    (2.8, 15.3, 11.2, 3.6, 19.0, 12.0, "pi_instrument"),
    (2.8, 18.4, 11.2, 7.0, 19.0, 12.0, "pi_instrument"),
    (6.3, 17.4, 11.4, 6.6, 18.4, 11.7, "pi_chrome"),
] + [b for i in range(4) for j in range(2) for b in (
    (4.2 + i * 1.1, 15.3, 12.4 + j * 1.2, 4.9 + i * 1.1, 17.0, 13.1 + j * 1.2, "pi_lab_glass"),
    (4.25 + i * 1.1, 15.35, 12.45 + j * 1.2, 4.85 + i * 1.1, 16.4, 13.05 + j * 1.2,
     ["pi_liquid_clear", "pi_liquid_amber", "pi_liquid_blue", "pi_liquid_green"][(i + j) % 4]),
    (4.2 + i * 1.1, 17.0, 12.4 + j * 1.2, 4.9 + i * 1.1, 17.4, 13.1 + j * 1.2, "pi_cap_blue"))]


EXTINGUISHER_CABINET = [
    # A white steel cabinet on the wall: its back, sides, top and bottom.
    (3, 0.5, 0, 13, 15.5, 0.5, "pi_cab_white"),
    (3, 0.5, 0.5, 3.8, 15.5, 4, "pi_cab_white"), (12.2, 0.5, 0.5, 13, 15.5, 4, "pi_cab_white"),
    (3.8, 14.7, 0.5, 12.2, 15.5, 4, "pi_cab_white"), (3.8, 0.5, 0.5, 12.2, 1.3, 4, "pi_cab_white"),
    # The extinguisher in it: the red body with its instruction label, the neck, the valve, the lever, the hose.
    (6, 1.3, 1.0, 10, 11.6, 3.2, {"*": "pi_extinguisher_red", "south": "pi_extinguisher_label!"}),
    (7.3, 11.6, 1.6, 8.7, 12.4, 2.6, "pi_chrome"),
    (6.6, 12.4, 1.4, 9.4, 13.2, 2.8, "pi_steel_dark"),
    (6.4, 13.2, 1.6, 9.8, 13.6, 2.6, "pi_chrome"),
    (9.4, 12.2, 1.8, 10.6, 12.6, 2.4, "pi_steel_dark"),
    (10.2, 4.5, 1.8, 10.8, 12.6, 2.4, "pi_steel_dark"),
    (9.6, 4.0, 1.8, 10.8, 4.6, 2.4, "pi_steel_dark"),
    # The glazed door: a white frame round clear glass, the black pull handle, FIRE across the top.
    (3.8, 1.3, 3.5, 12.2, 2.1, 4, "pi_cab_white"), (3.8, 13.9, 3.5, 12.2, 14.7, 4, "pi_cab_white"),
    (3.8, 2.1, 3.5, 4.6, 13.9, 4, "pi_cab_white"), (11.4, 2.1, 3.5, 12.2, 13.9, 4, "pi_cab_white"),
    (4.6, 2.1, 3.6, 11.4, 13.9, 3.75, "pi_cabinet_glass"),
    (10.6, 6, 4, 11.0, 10, 4.6, "pi_steel_dark"),
    (4.6, 13.0, 3.8, 11.4, 13.9, 3.85, "pi_fire_strip"),
]


def magazines(x, z, y0):
    """Two magazines lying one on the other, slightly askew: pages at the edges, a cover on top."""
    return [(x, y0, z, x + 3.4, y0 + 0.3, z + 4.6, {"*": "pi_pages", "up": "pi_magazine_0!"}),
            (x + 0.6, y0 + 0.3, z + 0.4, x + 4.0, y0 + 0.6, z + 5.0, {"*": "pi_pages", "up": "pi_magazine_1!"})]


# ---- food for the break room tables: round 8's, much rounder and realer (plant_office5)

FOOD, FOOD_STEAM = O5.FOOD, O5.FOOD_STEAM


# ---- textures

def _img(c, size=S):
    a = np.zeros((size, size, 4), np.uint8)
    a[..., :3] = c
    a[..., 3] = 255
    return Image.fromarray(a, "RGBA")


def _noise(img, seed, colours, count):
    d = ImageDraw.Draw(img)
    r = random.Random(seed)
    w, h = img.size
    for _ in range(count):
        d.point((r.randrange(w), r.randrange(h)), fill=r.choice(colours))
    return img


def _phone_keys():
    img = _img((34, 36, 40))
    d = ImageDraw.Draw(img)
    for k in range(12):
        x, y = 12 + (k % 3) * 22, 14 + (k // 3) * 26
        d.rounded_rectangle([x, y, x + 16, y + 18], radius=3, fill=(70, 72, 78, 255))
    for k in range(5):
        d.rounded_rectangle([86, 14 + k * 22, 116, 28 + k * 22], radius=3, fill=(60, 62, 68, 255))
        d.rectangle([118, 18 + k * 22, 122, 24 + k * 22], fill=(80, 200, 120, 255) if k == 0 else (60, 62, 68, 255))
    return img


def _phone_lcd():
    img = _img((24, 26, 30))
    d = ImageDraw.Draw(img)
    d.rectangle([8, 20, 120, 108], fill=(150, 190, 200, 254))
    d.text((14, 30), "Ext 2140", font=O3._font(20), fill=(20, 30, 34, 255))
    d.text((14, 64), "09:41", font=O3._font(26), fill=(20, 30, 34, 255))
    return img


def _microwave_door():
    img = _img((214, 216, 214))
    d = ImageDraw.Draw(img)
    d.rectangle([8, 14, 112, 114], fill=(26, 28, 32, 255), outline=(170, 172, 170, 255), width=4)
    for x in range(14, 108, 6):                      # the mesh behind the glass
        for y in range(20, 110, 6):
            d.point((x, y), fill=(50, 52, 58, 255))
    d.line([(20, 30), (60, 90)], fill=(80, 86, 96, 255), width=3)
    return img


def _microwave_panel():
    img = _img((200, 202, 200))
    d = ImageDraw.Draw(img)
    d.rectangle([12, 10, 116, 34], fill=(24, 30, 26, 255))
    d.text((24, 12), "12:30", font=O3._font(18), fill=(90, 230, 120, 254))
    for k in range(12):
        x, y = 16 + (k % 3) * 32, 46 + (k // 3) * 16
        d.rounded_rectangle([x, y, x + 24, y + 11], radius=3, fill=(170, 172, 170, 255), outline=(130, 132, 130, 255))
    d.rounded_rectangle([16, 112, 112, 124], radius=4, fill=(60, 140, 210, 255))
    return img


def _coffee_panel():
    img = _img((44, 46, 50))
    d = ImageDraw.Draw(img)
    d.rectangle([24, 14, 104, 48], fill=(16, 18, 22, 255))
    d.text((32, 20), "ESPRESSO", font=O3._font(14), fill=(230, 180, 90, 254))
    for k in range(4):
        d.ellipse([18 + k * 25, 64, 34 + k * 25, 80], fill=(200, 202, 206, 255))
    d.rectangle([0, 104, 128, 128], fill=(30, 31, 34, 255))
    return img


def _hopper_lid():
    img = _img((44, 46, 50))
    d = ImageDraw.Draw(img)
    d.ellipse([24, 24, 104, 104], fill=(80, 60, 40, 255), outline=(30, 30, 32, 255), width=6)
    return _noise(img, 1401, [(60, 40, 24, 255), (100, 70, 40, 255)], 300)


def _instrument(front=None):
    img = _img((222, 224, 222))
    d = ImageDraw.Draw(img)
    if front == "front":
        d.line([(0, 4), (S, 4)], fill=(200, 202, 200, 255), width=2)
        d.rectangle([8, 100, 120, 124], outline=(170, 172, 170, 255), width=2)
        for k in range(6):
            d.ellipse([16 + k * 17, 106, 26 + k * 17, 118], fill=(150, 154, 158, 255))
    elif front == "vents":
        for y in range(24, 104, 7):
            d.rounded_rectangle([24, y, 104, y + 3], radius=1, fill=(160, 162, 160, 255))
        d.rectangle([10, 112, 40, 120], fill=(200, 202, 200, 255))
    return img


def _analyzer_badge():
    """The maker's band across the instrument's top: 512 wide, the name fitted into its top strip (1.7 / 16 of it,
    which is the band's shape)."""
    N = 512
    h = int(N * 1.7 / 16)
    img = Image.new("RGBA", (N, N), (40, 90, 150, 255))
    d = ImageDraw.Draw(img)
    size = 40
    while size > 10 and (d.textlength("ION CHROM 900", font=O3._font(size)) > N * 0.6 or size > h - 10):
        size -= 1
    d.text((16, (h - size) / 2 - 3), "ION CHROM 900", font=O3._font(size), fill=(244, 246, 250, 255))
    sub = "Ion Chromatography System"
    s2 = max(10, size // 2)
    d.text((N - 16 - d.textlength(sub, font=O3._font(s2)), (h - s2) / 2 - 2), sub, font=O3._font(s2), fill=(190, 210, 236, 255))
    return img


def _analyzer_screen():
    img = _img((16, 18, 22))
    d = ImageDraw.Draw(img)
    d.rectangle([6, 6, 122, 122], fill=(232, 238, 246, 254))
    d.rectangle([6, 6, 122, 20], fill=(40, 90, 150, 254))
    d.text((10, 7), "Chromatogram  RCS-B", font=O3._font(10), fill=(255, 255, 255, 254))
    d.line([(12, 100), (116, 100)], fill=(60, 60, 64, 254), width=2)
    pts = []
    for x in range(12, 117, 2):
        y = 98 - (50 if 38 < x < 44 else 70 if 66 < x < 72 else 30 if 92 < x < 97 else 0)
        pts.append((x, y))
    d.line(pts, fill=(30, 110, 200, 254), width=2)
    d.text((40, 30), "F-", font=O3._font(10), fill=(200, 40, 40, 254))
    d.text((68, 24), "Cl-", font=O3._font(10), fill=(200, 40, 40, 254))
    d.text((92, 52), "SO4", font=O3._font(10), fill=(200, 40, 40, 254))
    d.text((12, 106), "Cl- 2.1 ppb   SAT", font=O3._font(11), fill=(30, 120, 60, 254))
    return img


def _analyzer_leds():
    img = _img((26, 28, 32))
    d = ImageDraw.Draw(img)
    for k, col in enumerate(((80, 230, 110), (80, 230, 110), (240, 180, 40), (80, 160, 240))):
        d.ellipse([10 + k * 30, 40, 30 + k * 30, 60], fill=col + (254,))
    return img


def _sampler_top():
    img = _img((200, 202, 204))
    d = ImageDraw.Draw(img)
    d.ellipse([10, 10, 118, 118], fill=(150, 152, 156, 255), outline=(120, 122, 126, 255), width=4)
    for k in range(16):
        import math
        a = k / 16 * math.tau
        x, y = 64 + math.cos(a) * 42, 64 + math.sin(a) * 42
        d.ellipse([x - 5, y - 5, x + 5, y + 5], fill=(60, 62, 66, 255))
    return img


def _cabinet_glass():
    a = np.zeros((S, S, 4), np.uint8)
    a[..., :3] = (220, 236, 240)
    a[..., 3] = 60
    img = Image.fromarray(a, "RGBA")
    d = ImageDraw.Draw(img)
    d.line([(20, 110), (60, 20)], fill=(250, 252, 255, 150), width=6)
    d.line([(40, 118), (80, 28)], fill=(250, 252, 255, 110), width=3)
    return img


def _extinguisher_label():
    img = _img((200, 26, 30))
    d = ImageDraw.Draw(img)
    d.rectangle([14, 30, 114, 98], fill=(244, 244, 238, 255))
    d.rectangle([14, 30, 114, 44], fill=(30, 30, 34, 255))
    d.text((20, 31), "INSTRUCTIONS", font=O3._font(11), fill=(250, 250, 250, 255))
    for k in range(3):
        d.rectangle([20, 50 + k * 15, 40, 62 + k * 15], outline=(30, 30, 34, 255), width=2)
        d.line([(46, 56 + k * 15), (106, 56 + k * 15)], fill=(80, 80, 86, 255), width=3)
    d.rectangle([40, 104, 88, 118], fill=(244, 244, 238, 255))
    d.text((44, 104), "ABC", font=O3._font(12), fill=(30, 30, 34, 255))
    return img


def _fire_strip():
    img = _img((200, 26, 30))
    d = ImageDraw.Draw(img)
    f = O3._font(54)
    w = d.textlength("FIRE", font=f)
    d.text(((S - w) / 2, 30), "FIRE", font=f, fill=(255, 255, 255, 255))
    return img


def _magazine(k):
    """A magazine cover: a photo (one of the scenes), the masthead, cover lines."""
    import paintings
    img = Image.new("RGBA", (S, S), (240, 240, 236, 255))
    pic = paintings.painted([7, 1, 9, 3][k % 4], 128, 128, 1500 + k).convert("RGBA")
    img.paste(pic, (0, 0))
    d = ImageDraw.Draw(img)
    name = ["POWER WEEK", "ENERGY TODAY"][k % 2]
    d.rectangle([0, 0, S, 26], fill=((200, 30, 40, 255) if k % 2 == 0 else (20, 60, 130, 255)))
    size = 22
    while d.textlength(name, font=O3._font(size)) > S - 10:
        size -= 1
    f = O3._font(size)
    d.text(((S - d.textlength(name, font=f)) / 2, (26 - size) / 2 - 1), name, font=f, fill=(255, 255, 255, 255))
    for k2, line in enumerate(("Inside the outage", "SMRs: what's next", "Grid of 2030")):
        d.rectangle([6, 84 + k2 * 14, 6 + d.textlength(line, font=O3._font(10)) + 6, 96 + k2 * 14], fill=(0, 0, 0, 160))
        d.text((9, 84 + k2 * 14), line, font=O3._font(10), fill=(255, 255, 255, 255))
    return img


def _food(kind):
    r = random.Random(1600 + hash(kind) % 1000)
    if kind == "takeout_front":
        img = _img((244, 244, 238))
        d = ImageDraw.Draw(img)
        d.rectangle([30, 30, 98, 98], outline=(200, 30, 40, 255), width=6)
        d.text((40, 48), "THANK", font=O3._font(14), fill=(200, 30, 40, 255))
        d.text((48, 66), "YOU", font=O3._font(14), fill=(200, 30, 40, 255))
        return img
    if kind == "noodles":
        img = _img((220, 180, 100))
        d = ImageDraw.Draw(img)
        for _ in range(40):
            x, y = r.randint(0, S), r.randint(0, S)
            d.arc([x - 20, y - 10, x + 20, y + 10], r.randint(0, 180), r.randint(180, 360), fill=(240, 210, 130, 255), width=3)
        for _ in range(8):
            x, y = r.randint(10, 110), r.randint(10, 110)
            d.ellipse([x, y, x + 10, y + 8], fill=r.choice([(60, 140, 50, 255), (200, 90, 40, 255)]))
        return img
    if kind == "gyro":
        img = _img((230, 200, 150))
        d = ImageDraw.Draw(img)
        d.rectangle([10, 40, 118, 90], fill=(150, 90, 50, 255))
        for _ in range(30):
            x, y = r.randint(12, 116), r.randint(42, 88)
            d.rectangle([x, y, x + 6, y + 4], fill=r.choice([(240, 240, 230, 255), (200, 40, 40, 255), (60, 150, 60, 255)]))
        return img
    if kind == "salad":
        img = _img((70, 150, 60))
        return _noise(img, 1601, [(110, 190, 80, 255), (220, 50, 50, 255), (250, 250, 240, 255), (40, 110, 40, 255), (240, 200, 60, 255)], 1800)
    if kind == "bun_top":
        img = _img((200, 130, 60))
        return _noise(img, 1602, [(250, 240, 210, 255)], 120)
    if kind == "fries":
        img = _img((180, 30, 30))
        d = ImageDraw.Draw(img)
        for x in range(4, S, 9):
            d.rectangle([x, 0, x + 6, S], fill=(240, 200, 80, 255))
        return img
    if kind == "fries_box":
        img = _img((200, 30, 30))
        d = ImageDraw.Draw(img)
        d.ellipse([40, 40, 88, 88], fill=(250, 200, 40, 255))
        return img
    if kind == "croissant":
        img = _img((210, 150, 70))
        d = ImageDraw.Draw(img)
        for x in range(10, S, 22):
            d.arc([x - 30, -20, x + 30, 148], 250, 290, fill=(160, 100, 40, 255), width=6)
        return img
    if kind == "plate":
        img = _img((246, 246, 242))
        d = ImageDraw.Draw(img)
        d.ellipse([6, 6, 122, 122], outline=(210, 214, 214, 255), width=6)
        return img
    if kind == "tzatziki":
        return _noise(_img((240, 244, 236)), 1603, [(120, 170, 100, 255)], 200)
    if kind == "can_top":
        img = _img((190, 192, 196))
        d = ImageDraw.Draw(img)
        d.ellipse([10, 10, 118, 118], outline=(150, 152, 156, 255), width=6)
        d.rounded_rectangle([50, 30, 78, 60], radius=6, fill=(120, 122, 126, 255))
        return img
    if kind == "cup_lid":
        img = _img((240, 240, 236))
        d = ImageDraw.Draw(img)
        d.ellipse([20, 20, 108, 108], outline=(200, 200, 196, 255), width=4)
        return img
    raise KeyError(kind)


def makers():
    out = {
        "pi_phone_body": lambda: _img((40, 42, 46)), "pi_phone_keys": _phone_keys, "pi_phone_lcd": _phone_lcd,
        "pi_phone_cord": lambda: _img((30, 30, 34)), "pi_mug_plain": lambda: _img((244, 244, 240)),
        "pi_microwave_case": lambda: _img((214, 216, 214)), "pi_microwave_door": _microwave_door,
        "pi_microwave_panel": _microwave_panel, "pi_coffee_panel": _coffee_panel, "pi_hopper_lid": _hopper_lid,
        "pi_instrument": _instrument, "pi_instrument_front": lambda: _instrument("front"),
        "pi_instrument_vents": lambda: _instrument("vents"), "pi_analyzer_screen": _analyzer_screen,
        "pi_analyzer_leds": _analyzer_leds, "pi_analyzer_badge": _analyzer_badge,
        "pi_badge_blue": lambda: _img((40, 90, 150)), "pi_sampler_top": _sampler_top,
        "pi_cabinet_glass": _cabinet_glass, "pi_extinguisher_red": lambda: _img((196, 26, 30)),
        "pi_extinguisher_label": _extinguisher_label, "pi_fire_strip": _fire_strip,
        "pi_magazine_0": lambda: _magazine(0), "pi_magazine_1": lambda: _magazine(1),
        "pi_takeout": lambda: _img((244, 244, 238)), "pi_takeout_front": lambda: _food("takeout_front"),
        "pi_noodles": lambda: _food("noodles"), "pi_chopstick": lambda: _img((214, 180, 120)),
        "pi_can_red": lambda: _img((200, 30, 40)), "pi_can_top": lambda: _food("can_top"),
        "pi_plate": lambda: _food("plate"), "pi_pita": lambda: _img((230, 200, 150)), "pi_gyro": lambda: _food("gyro"),
        "pi_salad": lambda: _food("salad"), "pi_cup_white": lambda: _img((244, 244, 240)),
        "pi_tzatziki": lambda: _food("tzatziki"), "pi_bun": lambda: _img((200, 130, 60)), "pi_bun_top": lambda: _food("bun_top"),
        "pi_patty": lambda: _img((90, 50, 30)), "pi_cheese": lambda: _img((250, 190, 40)), "pi_lettuce": lambda: _img((110, 190, 70)),
        "pi_fries_box": lambda: _food("fries_box"), "pi_fries": lambda: _food("fries"),
        "pi_cup_red": lambda: _img((200, 40, 40)), "pi_cup_lid": lambda: _food("cup_lid"), "pi_straw": lambda: _img((240, 240, 240)),
        "pi_bowl": lambda: _img((240, 240, 236)), "pi_napkin": lambda: _img((250, 250, 246)),
        "pi_croissant": lambda: _food("croissant"),
    }
    out.update(O5.makers())
    out.update(O6.makers())
    return out


SHARP = O5.SHARP | O6.SHARP | {"pi_analyzer_badge", "pi_phone_lcd", "pi_microwave_panel", "pi_coffee_panel", "pi_analyzer_screen", "pi_extinguisher_label",
         "pi_fire_strip", "pi_magazine_0", "pi_magazine_1", "pi_instrument_front", "pi_takeout_front"}
TRANSLUCENT = {"pi_cabinet_glass"} | O5.TRANSLUCENT | O6.TRANSLUCENT
ANALYZER = O6.ANALYZER       # round 9: like the real instrument (plant_office6)
