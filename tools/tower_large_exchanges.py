"""Native 9-wide x 6-deep x 5-tall tower exchanges; legacy art is immutable.

OpenAI Codex (GPT-6), MIT, original closed industrial machinery. This standalone
writer exports new large models/materials/metadata only. Java owns blockstates,
registrations, inventories, layout migration and truthful live overlays.
"""
from pathlib import Path
import argparse
import copy
import hashlib
import json
import math

from PIL import Image, ImageDraw
import model_writer as mw
import tower_cargo_exchange as cargo
import tower_power_exchange as power

ROOT = Path(__file__).resolve().parents[1]
ASSETS = Path('src/main/resources/assets/jugcraft')
POWER_PREFIX = 'tower_power_large_'
CARGO_COLLISION = [[0, 0, 0, 144, 4, 96], [2, 4, .4, 142, 78.4, 94.6],
                   [63.8, 17.2, 0, 80.2, 33, 3.1]]
ENERGY_COLLISION = [[0, 0, 0, 144, 4, 96], [4, 4, .15, 140, 78, 22],
                    [33, 6, 24, 111, 73, 91], [6, 8, 27, 31, 70, 92],
                    [113, 8, 27, 138, 70, 92], [65.5, 17.5, 0, 78.5, 30.5, 2.4]]


def footprint():
    return [(x, y, z) for y in range(5) for z in range(6) for x in range(9)]


def layout(energy):
    result = copy.deepcopy(power.LAYOUT if energy else cargo.LAYOUT)
    result.update(version=3, variant='large', footprint={'size': [9, 5, 6], 'parts': 270, 'order': 'y,z,x'},
                  dimensions_wide_deep_tall=[9, 6, 5], overlay_translation=[32, 0, 0],
                  collision_boxes=ENERGY_COLLISION if energy else CARGO_COLLISION)
    result['socket']['part'] = 58
    result['socket']['center'][0] += 32
    if energy:
        result['socket']['contact'] = [68.8, 20.8, 75.2, 27.2]
        for dial in result['dials']: dial['pivot'][0] += 32
        for display in result['displays']:
            display['rect'][0] += 32; display['rect'][2] += 32; display['center_x'] += 32
    else:
        result['socket']['body'] = [69, 21, 0, 75, 27, 2.8]
        result['display']['rect'][0] += 32; result['display']['rect'][2] += 32
        result['display']['center_x'] += 32
        result['enclosure']['body'] = [2, 4, 2.2, 142, 77.8, 94]
    for lamp in result['lamps']: lamp['center'][0] += 32
    return result


def _translated(element, delta, remap=None):
    a, b, texture, options = mw.unpack(element)
    # Old tiny screw slots stand .06px proud; the new full-footprint front keeps
    # their back-to-front volume inside its z0 boundary without changing legacy.
    a = tuple(max(0, a[k]+delta[k]) for k in range(3)); b = tuple(b[k]+delta[k] for k in range(3))
    options = copy.deepcopy(options)
    if options.get('rotation'):
        axis, angle, origin, *extra = options['rotation']
        options['rotation'] = (axis, angle, tuple(origin[k]+delta[k] for k in range(3)), *extra)
    if remap:
        texture = {face: remap(tex) for face, tex in texture.items()} if isinstance(texture, dict) else remap(texture)
    return (a, b, texture, options) if options else (a, b, texture)


class Parts:
    """Normal-sized attached hardware; large shell plates slice at cell boundaries."""
    def __init__(self, energy): self.out = []; self.energy = energy

    def texture(self, material):
        if not self.energy: return cargo.PREFIX+material
        return POWER_PREFIX+material if material in ('paint', 'dark_paint', 'black') else 'tower_detail_'+material

    def box(self, a, b, material, artwork=None, face='north', rotation=None):
        assert all(a[k] < b[k] for k in range(3)), (a, b)
        texture = self.texture(material)
        if artwork:
            texture = {'*': texture, face: (POWER_PREFIX if self.energy else cargo.PREFIX)+artwork+'!'}
            if rotation is None: rotation = ('y', 0, tuple((a[k]+b[k])/2 for k in range(3)))
        self.out.append((tuple(a), tuple(b), texture, {'rotation': rotation}) if rotation else (tuple(a), tuple(b), texture))

    def cylinder(self, axis, center, low, high, radius, material):
        k = 'xyz'.index(axis); cross = [v for v in range(3) if v != k]
        cuts = [low]+[float(n) for n in range(math.floor(low/16)*16+16, math.ceil(high/16)*16, 16)
                      if low < n < high]+[high]
        for lo, hi in zip(cuts, cuts[1:]):
            for wa, wb, angle in ((radius, radius*math.tan(math.pi/8), 0),
                                  (radius*math.tan(math.pi/8), radius, 0),
                                  (radius, radius*math.tan(math.pi/8), 45),
                                  (radius*math.tan(math.pi/8), radius, 45)):
                c = list(center); c[k] = (lo+hi)/2; a = c.copy(); b = c.copy(); a[k] = lo; b[k] = hi
                for j, width in zip(cross, (wa, wb)): a[j]-=width; b[j]+=width
                self.box(a, b, material, rotation=(axis, angle, tuple(c)) if angle else None)

    def bolt(self, x, y, z, face='north'):
        steel = 'light_steel' if self.energy else 'stainless'; dark = 'dark' if self.energy else 'frame'
        if face == 'north':
            self.box((x-.28, y-.28, z), (x+.28, y+.28, z+.35), steel)
            self.box((x-.16, y-.035, z-.11), (x+.16, y+.035, z+.02), dark)
        elif face == 'south': self.box((x-.28, y-.28, z), (x+.28, y+.28, z+.4), steel)
        elif face == 'up': self.box((x-.28, y, z-.28), (x+.28, y+.4, z+.28), steel)

    def front_cover(self, x0, x1, y0, y1, label=None, handle=True):
        black = 'black' if self.energy else 'rubber'; paint = 'paint' if self.energy else 'housing'
        metal = 'light_steel' if self.energy else 'stainless'; dark = 'dark' if self.energy else 'frame'
        self.box((x0, y0, .95), (x1, y1, 2.65), black)
        self.box((x0+.5, y0+.5, .65), (x1-.5, y1-.5, 1.8), paint)
        for x in (x0+1.1, x1-1.1):
            for y in (y0+1.1, y1-1.1): self.bolt(x, y, .3)
        for y in (y0+4, y1-8):
            self.box((x0+.6, y, .3), (x0+2.4, y+3.6, 1.65), dark)
            self.cylinder('y', (x0+1.3, 0, .6), y, y+3.6, .34, metal)
        if handle:
            y = min(26, (y0+y1)/2-2.5)
            if y < y0+2: y = (y0+y1)/2-2.5
            self.box((x1-5.3, y-.7, .35), (x1-1.9, y+6.5, 1.4), dark)
            for yy in (y, y+4.4): self.box((x1-4.1, yy, .2), (x1-3.1, yy+.8, 1.2), metal)
            self.box((x1-3.9, y, .05), (x1-3.3, y+5.2, .5), black)
        if label: self.box((x0+3, y1-4, .3), (x1-3, y1-2, .75), dark, artwork=label)


def cargo_geometry():
    p = Parts(False); b = p.box
    b((.05, 0, .05), (143.95, 4, 95.95), 'brushed')
    b((2, 4, 2.2), (142, 77.8, 94), 'frame')
    for a, z in (((2, 4.2, .9), (142, 6.5, 2.6)), ((2, 4.2, 93.7), (142, 6.5, 95))): b(a, z, 'brushed')
    for x in (1.3, 141.3): b((x, 4.2, 2), (x+1.4, 6.5, 94), 'brushed')
    b((1.5, 76.8, 1.4), (142.5, 78.4, 94.6), 'housing')
    b((2, 75.5, .9), (142, 77.1, 2.4), 'frame')
    # Existing operator and bulkhead hardware retain human-scale dimensions.
    for element in cargo.geometry():
        low, high, _, _ = mw.unpack(element)
        if low[1] >= 7 and high[2] <= 3.2:
            p.out.append(_translated(element, (32, 0, 0)))
    for x0, x1 in ((3.2, 31.5), (112.5, 140.8)):
        p.front_cover(x0, x1, 7.2, 43.7, 'interlock')
        b((x0+4, 10, .3), (x1-8, 17.2, 1.5), 'frame')
        for yy in (10.8, 12.2, 13.6, 15): b((x0+4.7, yy, .05), (x1-8.7, yy+.55, .9), 'housing')
    # Upper service modules are independent covers, not oversized operator doors.
    for x in (4, 32, 60, 88, 116): p.front_cover(x, x+24, 47, 73.6, 'access', False)
    # Six normal-size closed top hatches with backed hinges and lifting handles.
    for x in (8, 53, 98):
        for z in (12, 53):
            b((x, 78.3, z), (x+35, 78.75, z+34), 'rubber')
            b((x+.6, 78.65, z+.6), (x+34.4, 79.15, z+33.4), 'housing')
            for xx in (x+2, x+33):
                for zz in (z+2, z+32): p.bolt(xx, 79.1, zz, 'up')
            for zz in (z+7, z+26):
                b((x+.4, 79, zz), (x+1.5, 79.8, zz+3), 'frame')
            for xx in (x+13, x+21): b((xx, 79.1, z+16), (xx+1, 79.75, z+18), 'frame')
            b((x+13, 79.5, z+16.5), (x+22, 79.95, z+17.5), 'rubber')
    # Fully backed side cassettes, hinges, filtered vents and clipped service runs.
    for east in (False, True):
        for z in (7, 35, 63):
            for y0, y1 in ((8, 40.8), (44, 73.8)):
                x0, x1 = (141.7, 142.9) if east else (1.1, 2.3)
                b((x0, y0, z), (x1, y1, z+24), 'rubber')
                lo, hi = (142.65, 143.2) if east else (.8, 1.35)
                b((lo, y0+.6, z+.6), (hi, y1-.6, z+23.4), 'housing')
                for yy in (y0+2.2, y1-2.2):
                    for zz in (z+2, z+22):
                        a, c = (143.1, 143.55) if east else (.45, .9)
                        b((a, yy-.3, zz-.3), (c, yy+.3, zz+.3), 'stainless')
                for yy in (y0+7, y1-8):
                    a, c = (143.15, 143.65) if east else (.35, .85)
                    b((a, yy, z+21.5), (c, yy+3.8, z+22.9), 'frame')
                a, c = (143.1, 143.6) if east else (.4, .9)
                b((a, y0+12, z+3), (c, y0+19, z+4), 'rubber')
            a, c = (143.1, 143.45) if east else (.55, .9)
            b((a, 11, z+3), (c, 19.4, z+20), 'frame')
            for yy in (11.8, 13.3, 14.8, 16.3, 17.8):
                a, c = (143.35, 143.8) if east else (.2, .65)
                b((a, yy, z+3.7), (c, yy+.55, z+19.3), 'housing')
        a, c = (142, 142.9) if east else (1.1, 2)
        b((a, 74.7, 5), (c, 75.6, 92), 'rubber')
        for zz in (9, 23, 37, 51, 65, 79, 90):
            a, c = (142.75, 143.3) if east else (.7, 1.25)
            b((a, 74.4, zz), (c, 75.9, zz+1.3), 'stainless')
    for x in (5, 32, 59, 86, 113):
        for y0, y1 in ((7.5, 40.8), (44, 73.8)):
            b((x, y0, 93.8), (x+25, y1, 94.6), 'rubber')
            b((x+.5, y0+.5, 94.5), (x+24.5, y1-.5, 95.05), 'housing')
            for xx in (x+1.5, x+23.5):
                for yy in (y0+2, y1-2): p.bolt(xx, yy, 95, 'south')
            for xx in (x+8, x+16): b((xx, y0+12, 95), (xx+.8, y0+17, 95.65), 'rubber')
    b((57, 41.5, 94.6), (87, 43.8, 95), 'frame', artwork='rear', face='south')
    return p.out


def energy_geometry():
    p = Parts(True); b = p.box
    b((.05, 0, .05), (143.95, 4, 95.95), 'concrete')
    # A continuous black switchgear enclosure, with upper busway compartments.
    b((4, 4, 2.4), (140, 76, 22), 'dark_paint')
    b((3.5, 75.2, 1.3), (140.5, 78, 22.5), 'paint')
    b((4, 4.1, 1), (140, 7, 3), 'dark')
    for element in power.geometry():
        low, high, _, _ = mw.unpack(element)
        if low[1] >= 8 and high[2] <= 3.1:
            p.out.append(_translated(element, (32, 0, 0), lambda tex: tex.replace(power.PREFIX, POWER_PREFIX)))
    # Normal-width service bays flank the original precision instrument banks.
    for x0, x1 in ((5.5, 25.5), (27.5, 47.5), (97.5, 117.5), (119.5, 139)):
        p.front_cover(x0, x1, 8.2, 46.4, 'access')
        for y in (10, 11.3, 12.6): b((x0+3, y, .3), (x1-7, y+.5, 1), 'dark')
    for x in (6, 28, 50, 72, 94, 116):
        p.front_cover(x, x+20, 49, 73, 'service', False)
        # Backed bus-compartment ventilation, no open internal view.
        b((x+4, 54, .3), (x+16, 63, 1.5), 'black')
        for y in (55, 56.5, 58, 59.5, 61): b((x+4.7, y, .05), (x+15.3, y+.55, .9), 'paint')
    # Sealed central transformer and connected cooling assemblies; all are
    # grounded on the same concrete plinth, with no decorative top spires.
    b((33, 6, 24), (111, 73, 91), 'dark_paint')
    b((32.4, 72.7, 23.5), (111.6, 75.2, 91.6), 'paint')
    for x in (38, 102):
        b((x, 4, 28), (x+4, 9, 88), 'steel')
        for z in (31, 60, 84): b((x-.5, 4, z), (x+4.5, 5, z+4), 'light_steel')
    for x in (38, 57, 76, 95):
        b((x, 10, 90.8), (x+15, 68.5, 91.6), 'black')
        b((x+.6, 10.6, 91.5), (x+14.4, 67.9, 92.1), 'paint')
        for xx in (x+1.4, x+13.6):
            for y in (12, 66.5): p.bolt(xx, y, 92, 'south')
        for yy in (21, 56): b((x+6.5, yy, 92), (x+7.3, yy+5, 92.7), 'dark')
    b((55, 68.8, 91.3), (89, 71.3, 91.8), 'black', artwork='cooling', face='south')
    # Expansion/capped service modules and realistic roof lifting lugs.
    for x in (40, 67, 94):
        b((x, 75, 37), (x+10, 77.6, 77), 'dark_paint')
        for z in (40, 74):
            b((x+.7, 77.5, z-.5), (x+2.8, 79.3, z+.7), 'dark')
            b((x+7.2, 77.5, z-.5), (x+9.3, 79.3, z+.7), 'dark')
        for xx in (x+1, x+9):
            for z in (38, 76): p.bolt(xx, 77.6, z, 'up')
    for left in (True, False):
        x0, x1 = (6, 31) if left else (113, 138)
        b((x0+1, 4, 31), (x1-1, 8, 89), 'dark')
        for z in (32, 84): b((x0+2, 8, z), (x1-2, 13, z+4), 'steel')
        # Fins form the physical heat exchanger, not stripes painted on a cube.
        for z in range(30, 91, 4): b((x0+1, 13, z), (x1-1, 65, z+.85), 'dark_paint')
        center = (x0+x1)/2
        for y in (11, 68):
            p.cylinder('z', (center, y, 0), 27, 92, 1.45, 'steel')
            tank_edge = 34 if left else 110
            for z in (31, 86):
                p.cylinder('x', (0, y, z), min(center, tank_edge), max(center, tank_edge), 1.15, 'steel')
                p.cylinder('z', (center, y, 0), z-.5, z+.5, 1.85, 'light_steel')
        # Continuous containment guard, inset only to keep the cooling fin edge visible.
        outside = x0+.3 if left else x1-.9
        for z in (29, 90): b((outside, 8, z), (outside+.6, 70, z+1), 'dark')
        for y in (9, 69): b((outside, y, 29), (outside+.6, y+1, 91), 'dark')
    return p.out


def geometry(energy): return energy_geometry() if energy else cargo_geometry()


def power_textures():
    """New dedicated charcoal material IDs; no recoloring of legacy runtime PNGs."""
    result = {name.replace(power.PREFIX, POWER_PREFIX): image.copy() for name, image in power.textures().items()}
    for name, base in (('paint', (54, 57, 59, 255)), ('dark_paint', (28, 30, 32, 255)), ('black', (18, 20, 22, 255))):
        image = Image.new('RGBA', (256, 256), base); d = ImageDraw.Draw(image)
        for x in range(256):
            delta = round(math.sin(x/256*math.pi))
            d.line((x, 0, x, 255), fill=tuple(v+delta for v in base[:3])+(255,))
        result[POWER_PREFIX+name] = image
    return result


def native_parts(energy):
    return mw.split_model(('energy' if energy else 'cargo')+'_exchange_large', geometry(energy), footprint())


def item_elements(energy):
    # All principal assemblies retained; no subpixel fasteners in the inventory.
    p = Parts(energy); b = p.box
    if energy:
        b((0, 0, 0), (144, 4, 96), 'concrete'); b((4, 4, 2.4), (140, 76, 22), 'dark_paint')
        b((3.5, 75.2, 1.3), (140.5, 78, 22.5), 'paint'); b((33, 6, 24), (111, 73, 91), 'dark_paint')
        b((32.4, 72.7, 23.5), (111.6, 75.2, 91.6), 'paint')
        for x in (6, 28, 50, 72, 94, 116):
            b((x, 8.2, .95), (x+20, 46.4, 2.65), 'paint'); b((x, 49, .95), (x+20, 73, 2.65), 'paint')
        for x in (6, 113):
            for z in range(30, 91, 8): b((x+1, 13, z), (x+24, 65, z+1.2), 'dark_paint')
            for y in (10, 67): b((x+10, y, 27), (x+14, y+2, 92), 'steel')
        b((68.8, 20.8, 0), (75.2, 27.2, 2.4), 'rubber')
    else:
        b((0, 0, 0), (144, 4, 96), 'brushed'); b((2, 4, 2.2), (142, 77.8, 94), 'frame')
        b((1.5, 76.8, 1.4), (142.5, 78.4, 94.6), 'housing')
        for x in (4, 32, 60, 88, 116):
            for y0, y1 in ((7.2, 43.7), (47, 73.6)): b((x, y0, .65), (x+24, y1, 1.8), 'housing')
        for x in (8, 53, 98):
            for z in (12, 53): b((x, 78.4, z), (x+35, 79.2, z+34), 'housing')
        for x in (.8, 142.65):
            for z in (7, 35, 63): b((x, 8.6, z+.6), (x+.55, 73.2, z+23.4), 'housing')
        b((69, 21, 0), (75, 27, 2.8), 'brass'); b((36.2, 28, .4), (49.8, 35.5, .8), 'display')
    native = mw.scaled_elements(p.out); mw.finish_elements(native); return native


def write_assets(root=ROOT):
    root = Path(root); assets = root/ASSETS; originals = root/'art/tower-large-exchanges/originals'
    originals.mkdir(parents=True, exist_ok=True); (assets/'textures/block').mkdir(parents=True, exist_ok=True)
    textures = []
    for name, image in power_textures().items():
        original = originals/(name+'.png'); target = assets/'textures/block'/original.name
        image.save(original); target.write_bytes(original.read_bytes())
        textures.append({'source': original.relative_to(root).as_posix(), 'runtime': target.relative_to(root).as_posix(),
                         'size': list(image.size), 'sha256': hashlib.sha256(original.read_bytes()).hexdigest()})
    for energy in (True, False):
        name = ('energy' if energy else 'cargo')+'_exchange_large'
        raw = geometry(energy); lookup = {tex: 'jugcraft:block/'+tex for tex in mw.texture_names(raw)}
        lookup['particle'] = 'jugcraft:block/'+(POWER_PREFIX+'paint' if energy else cargo.PREFIX+'housing')
        for part, elements in enumerate(native_parts(energy)):
            mw.write(assets/'models/block'/f'{name}_part{part}.json', {
                'parent': 'minecraft:block/block', 'ambientocclusion': False, 'textures': lookup, 'elements': elements})
        mw.write(assets/'models/item'/f'{name}.json', {'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'textures': lookup, 'elements': item_elements(energy),
            'display': {'gui': {'rotation': [25, -135, 0], 'translation': [0, 0, 0], 'scale': [.65, .65, .65]},
                        'ground': {'scale': [.3, .3, .3]}, 'fixed': {'scale': [.6, .6, .6]}}})
        mw.write(assets/f'tower_{"power" if energy else "cargo"}_exchange_large_panels.json', layout(energy))
        print(f'{name}:270parts,{len(raw)}authored solids')
    mw.write(root/'art/tower-large-exchanges/provenance.json', {
        'author': 'OpenAI Codex (GPT-6)', 'license': 'MIT', 'date': '2026-10-09',
        'source': 'Original native industrial tower machinery; original existing instrument drawings reused at their true size.',
        'materials': 'New dedicated charcoal/black power material IDs; existing neutral cargo materials and tower concrete/steel referenced unchanged.',
        'legacy': 'No old model, panel metadata, PNG, blockstate or generator is written by this exporter.',
        'references': ['Owner library README/catalog reviewed; no external or owner-library pixels copied',
                       'Owner requested9wide6deep5tall, closed cargo and dark tower palette; true-size controls and fitted connections'],
        'textures': textures})


def exported_assembly(energy, root=ROOT):
    import art_check
    assets = Path(root)/ASSETS; name = ('energy' if energy else 'cargo')+'_exchange_large'
    members = [(assets/'models/block'/f'{name}_part{part}.json', tuple(c*16 for c in cell))
               for part, cell in enumerate(footprint())]
    faces, boxes = art_check._assembly(members, (assets,))
    return members, faces, boxes


def check_engineered_contract(energy, faces, boxes):
    """Readback regression for a fitted real-size port and the sealed cargo box."""
    if energy:
        contact = [68.8, 20.8, 0]; extent = [75.2, 27.2, .65]
    else:
        contact = [69, 21, 0]; extent = [75, 27, 2.8]
    assert any(all(abs(low[k]-contact[k]) < .00001 for k in range(3))
               and all(abs(high[k]-extent[k]) < .00001 for k in range(2)) and high[2] >= extent[2]-.00001
               for _, _, low, high, *_ in boxes), 'Large fitted neck no longer meets the boundary'
    count = 0
    # The large cargo machine is one continuous closed box, not a larger tunnel.
    if not energy:
        body = ((2, 4, 2.2), (142, 77.8, 94))
        assert any(tuple(a) == body[0] and tuple(b) == body[1] for a, b, *_ in geometry(False))
        for axis in range(3):
            cross = [k for k in range(3) if k != axis]
            for side in (0, 1):
                plane = body[side][axis]+(.02 if side == 0 else -.02)
                sections = [(lo, hi) for _, _, lo, hi, *_ in boxes if lo[axis]-.001 <= plane <= hi[axis]+.001]
                for u in range(math.ceil(body[0][cross[0]]), math.floor(body[1][cross[0]])):
                    for v in range(math.ceil(body[0][cross[1]]), math.floor(body[1][cross[1]])):
                        point = [0, 0, 0]; point[axis] = plane; point[cross[0]] = u+.5; point[cross[1]] = v+.5
                        assert any(all(lo[k]-.001 <= point[k] <= hi[k]+.001 for k in range(3))
                                   for lo, hi in sections), ('Open large cargo shell', point)
                        count += 1
        assert not layout(False)['enclosure']['exposed_transport']
    # Controls, displays and percentage scales remain at their true human size.
    source = power.LAYOUT if energy else cargo.LAYOUT; target = layout(energy)
    if energy:
        assert all(new['pivot'] == [old['pivot'][0]+32, *old['pivot'][1:]]
                   and new['radius'] == old['radius'] for old, new in zip(source['dials'], target['dials']))
    assert all(new['center'] == [old['center'][0]+32, *old['center'][1:]]
               and new['radius'] == old['radius'] for old, new in zip(source['lamps'], target['lamps']))
    return {'fitted_neck_at_boundary': True, 'normal_size_controls_preserved': True,
            'closed_cargo_boundary_probes': count}


def check_assets(root=ROOT):
    import art_check
    errors = []; summary = []
    assert footprint()[58] == (4, 1, 0) and len(footprint()) == 270
    for energy in (True, False):
        name = ('energy' if energy else 'cargo')+'_exchange_large'; assets = Path(root)/ASSETS
        members, faces, boxes = exported_assembly(energy, root)
        for path, _ in members+[(assets/'models/item'/f'{name}.json', (0, 0, 0))]:
            model = json.loads(path.read_bytes()); elements, lookup, _ = art_check._resolve(model, (assets, ROOT/ASSETS))
            own, raw_uv = art_check._model_faces(elements, lookup)
            if any(min(uv) < -.001 or max(uv) > 16.001 for uv in raw_uv): errors.append(f'{path.stem}: UV outside sprite')
            if art_check._open_faces(elements, own): errors.append(f'{path.stem}: exposed missing face')
            for element in elements:
                if any(element['from'][k] >= element['to'][k] for k in range(3)): errors.append(f'{path.stem}: zero/reversed volume')
                if min(element['from']) < -16 or max(element['to']) > 32: errors.append(f'{path.stem}: native bounds')
            for i, j, gap, polygon, u, v, normal, plane in art_check._pairs(own, art_check.N1_GAP):
                if not (gap == 0 and art_check._layered(elements, own[i], own[j])) and art_check._differ(own[i], own[j], polygon, u, v, normal, plane):
                    errors.append(f'{path.stem}: coplanar differing faces ({gap:.4f}px)'); break
        errors.extend(art_check._cross_errors(name, faces, boxes, set(footprint())))
        # Static faces keep the exact unchanged normal-size readout hardware.
        display_name = POWER_PREFIX+'display.png' if energy else cargo.PREFIX+'display.png'
        display = [f for f in faces if str(f.tex).endswith(display_name) and f.normal[2] < -.99]
        assert display and all(abs(point[2]-layout(energy)['static_face_z']) < .00001 for f in display for point in f.pts)
        raw = geometry(energy)
        assert all(min(a) >= 0 and all(b[k] <= (144, 80, 96)[k] for k in range(3)) for a, b, *_ in raw)
        row = {'name': name, 'parts': 270, 'authored_solids': len(raw), 'exported_faces': len(faces), 'errors': 0,
               'engineering_contract': check_engineered_contract(energy, faces, boxes)}
        summary.append(row); print(row)
    if errors: raise ValueError('\n'.join(errors))
    print('Large model bounds/UV/closed/coplanar/cross-part checks PASS.')
    return summary


def preview():
    import numpy as np
    import armor_preview as raster
    for energy in (True, False):
        _, faces, _ = exported_assembly(energy); panels = []
        images = {f.tex: np.array(Image.open(f.tex).convert('RGBA')) for f in faces}
        for title, view, scale in (('FRONT / FITTED INPUT', (32, 19), 8), ('REAR / SERVICE ASSEMBLIES', (145, 22), 8),
                                    ('TRUE-SIZE OPERATOR HARDWARE', (0, 3), 9)):
            camera = raster.camera(view)[0]
            quads = [(np.array(f.pts), np.array(f.uvs), np.array(f.normal), images[f.tex])
                     for f in faces if np.dot(f.normal, camera) > 0]
            image = raster.render(quads, view, scale=scale, background=(104, 108, 110), lighting='blockbench')
            panel = Image.new('RGB', (1700, 1250), (104, 108, 110)); panel.paste(image, ((1700-image.width)//2, 45+(1110-image.height)//2))
            d = ImageDraw.Draw(panel); d.text((24, 15), title+' | 9W x 6D x 5H | EXPORTED JSON + PNG', font=cargo.font(25, True), fill=cargo.PAPER)
            d.text((24, 1210), 'Original closed native machinery; blank live planes. Offline asset review.', font=cargo.font(22), fill=cargo.PAPER)
            panels.append(panel)
        path = ROOT/'build/drone_tower'/f'{"energy" if energy else "cargo"}_exchange_large_native.png'
        path.parent.mkdir(parents=True, exist_ok=True); raster.sheet(panels, 3).save(path); print(path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true'); parser.add_argument('--check', action='store_true'); parser.add_argument('--preview', action='store_true')
    args = parser.parse_args()
    if args.write: write_assets()
    if args.check: check_assets()
    if args.preview: preview()
