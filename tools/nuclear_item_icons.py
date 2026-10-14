"""Native 16px inventory drawings for the nuclear package; worn/block models keep their authored detail.

Draw at the final pixel grid with flat material tones and a one-pixel outline. These are source drawings,
not resampled versions of the high-resolution equipment textures.
"""
from PIL import Image, ImageDraw

STEEL = ('#303b43', '#576770', '#899ba4', '#c3d0d3')
YELLOW = ('#55451e', '#957328', '#c7a440', '#ecd278')
WHITE = ('#3e4850', '#7f9196', '#bac8c7', '#e4e9db')
BLUE = ('#273441', '#405771', '#66859c', '#a0bdc8')
GREEN = ('#293c31', '#4d7650', '#81a658', '#c4d77a')


def draw(kind, colours=STEEL, accent='#81be81'):
    image = Image.new('RGBA', (16, 16))
    pen = ImageDraw.Draw(image)
    outline, dark, mid, light = colours

    def box(x0, y0, x1, y1, fill=mid):
        pen.rectangle((x0, y0, x1, y1), fill=fill, outline=outline)
        if x1 - x0 > 2 and y1 - y0 > 2:
            pen.line((x0 + 1, y0 + 1, x1 - 1, y0 + 1), fill=light)
            pen.line((x0 + 1, y0 + 1, x0 + 1, y1 - 1), fill=light)

    if kind == 'hood':
        box(3, 2, 12, 12)
        pen.rectangle((4, 5, 11, 9), fill=outline)
        pen.rectangle((5, 6, 10, 8), fill='#718f91')
        pen.line((5, 6, 9, 6), fill='#b3d1d3')
        box(6, 10, 9, 13, dark)
    elif kind in ('shirt', 'coat'):
        pen.polygon([(5, 2), (6, 3), (9, 3), (10, 2), (13, 4), (14, 8), (11, 9),
                     (11, 14), (4, 14), (4, 9), (1, 8), (2, 4)], fill=mid, outline=outline)
        pen.line((3, 4, 2, 7), fill=light)
        pen.line((5, 4, 5, 12), fill=light)
        pen.line((8, 4, 8, 13), fill=dark)
        pen.rectangle((9, 6, 10, 7), fill=accent)
        if kind == 'coat':
            pen.line((7, 10, 7, 14), fill=outline)
    elif kind == 'trousers':
        pen.polygon([(3, 2), (12, 2), (12, 14), (9, 14), (9, 7), (6, 7), (6, 14), (3, 14)], fill=mid, outline=outline)
        pen.line((4, 3, 11, 3), fill=light)
        pen.line((4, 4, 4, 12), fill=light)
        pen.line((10, 8, 10, 12), fill=dark)
    elif kind == 'boots':
        for offset in (0, 7):
            pen.polygon([(3 + offset, 3), (6 + offset, 3), (6 + offset, 13), (1 + offset, 13),
                         (1 + offset, 10), (3 + offset, 9)], fill=mid, outline=outline)
            pen.line((4 + offset, 4, 4 + offset, 9), fill=light)
            pen.line((2 + offset, 12, 5 + offset, 12), fill=dark)
    elif kind == 'cylinder':
        box(6, 1, 9, 4, dark)
        box(3, 4, 12, 13)
        pen.line((5, 6, 5, 11), fill=light)
        pen.rectangle((7, 7, 10, 10), fill=accent)
        pen.point((8, 8), fill=outline)
    elif kind == 'fuel':
        box(4, 2, 11, 13, dark)
        for x in (5, 7, 9):
            pen.line((x, 4, x, 11), fill=light)
        pen.line((5, 7, 10, 7), fill=accent)
        pen.line((5, 10, 10, 10), fill=accent)
    elif kind == 'card':
        box(2, 4, 13, 11)
        pen.rectangle((4, 6, 6, 9), fill=accent)
        pen.line((8, 7, 11, 7), fill=dark)
        pen.line((8, 9, 10, 9), fill=dark)
    elif kind in ('screen', 'sign', 'print'):
        box(1, 3, 14, 12, dark)
        pen.rectangle((3, 5, 12, 10), fill=accent)
        pen.line((4, 6, 8, 6), fill=light)
        if kind == 'screen':
            pen.line((7, 13, 8, 13), fill=outline)
            pen.line((5, 14, 10, 14), fill=outline)
        else:
            pen.line((5, 8, 11, 8), fill=dark)
    elif kind == 'door':
        box(4, 1, 11, 14)
        box(6, 3, 9, 7, accent)
        pen.point((9, 9), fill=outline)
        pen.line((6, 11, 8, 11), fill=dark)
    elif kind == 'table':
        pen.polygon([(2, 5), (10, 2), (14, 5), (7, 9), (2, 7)], fill=mid, outline=outline)
        pen.line((3, 5, 10, 3), fill=light)
        for x, y in ((3, 8), (7, 10), (12, 7)):
            pen.line((x, y, x, 13), fill=outline, width=2)
            pen.line((x, y, x, 12), fill=dark)
    elif kind == 'rack':
        box(2, 2, 13, 14, dark)
        for y in (4, 8):
            for x in (4, 8):
                box(x, y, x + 3, y + 4, accent)
        pen.line((3, 8, 12, 8), fill=light)
    elif kind == 'copier':
        box(3, 5, 12, 13)
        box(2, 2, 11, 5, dark)
        pen.rectangle((5, 7, 10, 8), fill=outline)
        pen.line((5, 10, 9, 10), fill=dark)
        pen.point((11, 6), fill=accent)
    else:  # Cabinet controls: face and display remain legible in a single inventory slot.
        box(3, 1, 12, 14)
        box(5, 4, 10, 8, dark)
        pen.line((6, 6, 9, 6), fill=accent)
        pen.point((5, 10), fill=accent)
        pen.point((8, 10), fill='#dca666')
        pen.line((5, 12, 10, 12), fill=dark)
    return image


SPECS = {
    'hazmat_hood': ('hood', YELLOW), 'hazmat_coverall': ('shirt', YELLOW),
    'hazmat_trousers': ('trousers', YELLOW), 'hazmat_boots': ('boots', YELLOW),
    'lab_coat': ('coat', WHITE), 'security_jacket': ('shirt', BLUE),
    'check_source': ('cylinder', YELLOW), 'enriched_uranium_cylinder': ('cylinder', STEEL),
    'leu_fuel_assembly': ('fuel', STEEL), 'spent_fuel_assembly': ('fuel', BLUE),
    'reactor_link_card': ('card', STEEL),
    'steel_door': ('door', STEEL), 'glass_door': ('door', WHITE),
    'building_sign': ('sign', BLUE), 'room_sign': ('sign', STEEL), 'large_print': ('print', WHITE),
    'conference_table': ('table', STEEL), 'lounge_table': ('table', BLUE), 'oval_table': ('table', WHITE),
    'office_copier': ('copier', WHITE), 'storage_rack': ('rack', STEEL),
    'station_battery': ('rack', BLUE), 'edg_battery_rack': ('rack', GREEN),
    **{name: ('screen', STEEL) for name in ('flat_tv', 'plant_map_screen', 'status_screen', 'training_screen')},
    **{name: ('cabinet', STEEL) for name in ('dc_switchboard', 'edg_battery_charger', 'edg_engine_panel',
       'edg_generator_panel', 'edg_load_sequencer', 'edg_protection_panel', 'edg_voltage_regulator')},
}


def icons():
    return {name: draw(*spec, accent='#d0a15d' if name == 'spent_fuel_assembly' else '#81be81')
            for name, spec in SPECS.items()}
