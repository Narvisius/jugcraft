"""Pack supplied CTM tiles for the already-approved Fusion adapter.

This is lossless tile packing, not redrawing/resampling the owner's artwork.
The ordinary model remains usable when the optional Fusion dependency is absent.
"""
from pathlib import Path
import io
from PIL import Image
from menagerie_content import read


def generate(source, resources, writer):
    source = source / 'fix/assets/chisel'
    pack = resources / 'resourcepacks/fusion_textures'
    assets = pack / 'assets/jugcraft'
    connected = []
    for path in sorted((source / 'blockstates').rglob('*.json')):
        state = read(path)
        loader = state.get('athena:loader')
        if loader not in ('athena:ctm', 'athena:giant'): continue
        name = path.relative_to(source / 'blockstates').as_posix()[:-5]
        supplied = state['ctm_textures']
        if loader == 'athena:ctm':
            # Fusion's pieced layout uses the same five corner masks: isolated,
            # filled, vertical, horizontal, and four exposed inner corners.
            refs = [supplied[k] for k in ('particle', 'empty', 'vertical', 'horizontal', 'center')]
            columns, rows = 5, 1
            metadata = {'type': 'connecting', 'layout': 'pieced'}
        else:
            columns, rows = state['width'], state['height']
            refs = [supplied[str(i + 1)] for i in range(columns * rows)]
            metadata = {'type': 'continuous', 'columns': columns, 'rows': rows}
        tiles, animations = [], []
        for ref in refs:
            namespace, relative = ref.split(':', 1)
            if namespace != 'chisel': raise ValueError('Unexpected supplied CTM namespace: ' + ref)
            image_path = source / 'textures' / (relative + '.png')
            with Image.open(image_path) as image:
                tiles.append(image.convert('RGBA'))
            sidecar = image_path.with_name(image_path.name + '.mcmeta')
            animations.append(read(sidecar).get('animation') if sidecar.exists() else None)
        width = tiles[0].width
        if any(tile.width != width or tile.height % width != 0 for tile in tiles):
            raise ValueError('Incompatible supplied CTM tile dimensions: ' + name)
        frames = max(tile.height // width for tile in tiles)
        animation = next((value for value in animations if value is not None), None)
        for tile, timing in zip(tiles, animations):
            if tile.height != width and (tile.height != width * frames or timing != animation):
                raise ValueError('Unsynchronized supplied CTM animations: ' + name)
        if frames > 1 and animation is None: raise ValueError('Missing CTM animation timing: ' + name)
        sheet = Image.new('RGBA', (width * columns, width * rows * frames))
        for frame in range(frames):
            for index, tile in enumerate(tiles):
                tile_frame = 0 if tile.height == width else frame
                pixels = tile.crop((0, tile_frame * width, width, (tile_frame + 1) * width))
                sheet.paste(pixels, ((index % columns) * width, (frame * rows + index // columns) * width))
        texture = 'block/newest_connected/' + name
        target = assets / 'textures' / (texture + '.png')
        encoded = io.BytesIO(); sheet.save(encoded, format='PNG'); payload = encoded.getvalue()
        target.parent.mkdir(parents=True, exist_ok=True)
        if not target.exists() or target.read_bytes() != payload: target.write_bytes(payload)
        if name.endswith('/glass'): metadata['render_type'] = 'translucent'
        sidecar = {'fusion': metadata}
        if animation is not None:
            sidecar['animation'] = dict(animation, width=width * columns, height=width * rows)
        writer(target.with_name(target.name + '.mcmeta'), sidecar)
        original = read(source / 'models/block' / (name + '.json'))
        # The inspected source CTM models all use cube_all; reject an unexpected
        # mesh rather than silently flattening it into a cube.
        if original.get('parent') not in ('block/cube_all', 'minecraft:block/cube_all'):
            raise ValueError('CTM model needs explicit geometry adaptation: ' + name)
        model = {'loader': 'fusion:model', 'type': 'connecting' if loader == 'athena:ctm' else 'base',
                 'parent': 'minecraft:block/cube_all', 'textures': {'all': 'jugcraft:' + texture,
                 'particle': 'jugcraft:newest/chiselworks/block/' + name}}
        if loader == 'athena:ctm': model['connections'] = {'type': 'is_same_block'}
        writer(assets / 'models/newest/chiselworks/block' / (name + '.json'), model)
        connected.append({'model': name, 'source_loader': loader, 'fusion': metadata, 'tiles': refs})
    writer(resources / 'jugcraft/newest_connected.json', {'models': connected,
           'adaptation': 'Lossless original tile packing; Fusion optional pack; ordinary fallback preserved'})
    return len(connected)
