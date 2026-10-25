"""Original AE drive chassis and dynamically rendered press texture; no authored art changes."""
import hashlib
import shutil


def generate(source, assets, model, dump):
    for cell in ('item', 'fluid'):
        for size in (1, 4, 16, 64, 256):
            name = f'drive_{size}k_{cell}_cell'
            # Route through the caller's source adapter so its mesh provenance remains in one manifest.
            ref = model('block/' + name)
            dump(assets / f'items/jimtafacts/ae2/{name}.json', {'model': {'type': 'minecraft:model', 'model': ref}})
    original = source / 'textures/block/inscriber_inside.png'
    target = assets / 'textures/jimtafacts/ae2/block/inscriber_inside.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(original, target)
    root = assets.parents[4]
    dump(root / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/network-render-copies.json', [{
        'source': original.relative_to(root).as_posix(), 'runtime': target.relative_to(root).as_posix(),
        'sha256': hashlib.sha256(original.read_bytes()).hexdigest(),
        'changes': 'None; source moving-press texture bytes unchanged.'}])
