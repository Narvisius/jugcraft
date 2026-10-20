"""Data-only NBT reader/writer for adapting the six supplied structure templates.

Tag types are preserved, including byte/long arrays. Nothing from the library is
imported as executable Python or launched as a program.
"""
import gzip
import io
import struct


def read(raw):
    stream = io.BytesIO(gzip.decompress(raw) if raw[:2] == b'\x1f\x8b' else raw)
    def number(fmt): return struct.unpack('>' + fmt, stream.read(struct.calcsize(fmt)))[0]
    def string(): return stream.read(number('H')).decode('utf-8')
    def payload(kind):
        if kind in range(1, 7): return number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[kind])
        if kind == 7: return stream.read(number('i'))
        if kind == 8: return string()
        if kind == 9:
            element, count = number('B'), number('i')
            return (element, [payload(element) for _ in range(count)])
        if kind == 10:
            value = {}
            while (child := number('B')):
                name = string()
                value[name] = (child, payload(child))
            return value
        if kind in (11, 12): return [number('i' if kind == 11 else 'q') for _ in range(number('i'))]
        raise ValueError(f'Unknown NBT tag {kind}')
    kind, name = number('B'), string()
    return kind, name, payload(kind)


def write(root):
    stream = io.BytesIO()
    def number(fmt, value): stream.write(struct.pack('>' + fmt, value))
    def string(value):
        data = value.encode('utf-8'); number('H', len(data)); stream.write(data)
    def payload(kind, value):
        if kind in range(1, 7): number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[kind], value)
        elif kind == 7: number('i', len(value)); stream.write(value)
        elif kind == 8: string(value)
        elif kind == 9:
            child, items = value; number('B', child); number('i', len(items))
            for item in items: payload(child, item)
        elif kind == 10:
            for name, (child, item) in value.items(): number('B', child); string(name); payload(child, item)
            number('B', 0)
        elif kind in (11, 12):
            number('i', len(value))
            for item in value: number('i' if kind == 11 else 'q', item)
        else: raise ValueError(f'Unknown NBT tag {kind}')
    kind, name, value = root; number('B', kind); string(name); payload(kind, value)
    # Python 3.11 delegates mtime=0 to zlib, whose gzip OS byte varies by host.
    compressed = bytearray(gzip.compress(stream.getvalue(), mtime=0))
    compressed[9] = 255
    return bytes(compressed)


def adapt(raw, replace):
    kind, name, root = read(raw)
    def visit(kind, value, key=""):
        if kind == 8: return replace(value)
        if kind == 9: return value[0], [visit(value[0], item) for item in value[1]]
        if kind == 10:
            result = {key: (tag, visit(tag, item, key)) for key, (tag, item) in value.items()}
            if key=='nbt' and result.get('id')==(8,'jugcraft:nouveau_ritual_brazier'):result['id']=(8,'jugcraft:nouveau_workshop')
            if 'Name' in result and result['Name'][0] == 8 and ':' in result['Name'][1]:
                result['id'] = result.pop('Name')
                if 'Properties' in result: result['properties'] = result.pop('Properties')
            return result
        return value
    root = visit(kind, root)
    root['DataVersion'] = (3, 5023)
    return write((kind, name, root))
