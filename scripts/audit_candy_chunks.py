"""Read-only Candy Cavity survey of an OFFLINE world copy. Never changes or deletes chunks.

Reports candidate chunk coordinates and review flags, not permission to regenerate.
Run: python scripts/audit_candy_chunks.py --world BACKUP/world --output candy-review.json
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import sys
import zlib

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
import nouveau_nbt

CANDY = "jugcraft:cavern_candy_cavity"
LIMIT = 32 * 1024 * 1024

def value(compound, key, default=None):
    return compound.get(key, (0, default))[1]

def unpack(raw, compression):
    if compression == 3:
        if len(raw) > LIMIT: raise ValueError("Oversize uncompressed chunk")
        return raw
    if compression not in (1, 2): raise ValueError(f"Unsupported compression {compression}; review manually")
    decoder = zlib.decompressobj(31 if compression == 1 else 15)
    try: data = decoder.decompress(raw, LIMIT + 1)
    except zlib.error as exc: raise ValueError("Corrupt compressed chunk") from exc
    if len(data) > LIMIT or decoder.unconsumed_tail or not decoder.eof:
        raise ValueError("Oversize or truncated compressed chunk")
    return data

def inspect_chunk(root):
    root = value(root, "Level", root)
    sections = value(root, "sections", (10, []))[1]
    palettes = [value(value(section, "biomes", {}), "palette", (8, []))[1] for section in sections]
    # Match the exact runtime biome, retaining a separately reported legacy alias.
    matches = sorted({name for palette in palettes for name in palette
                      if name in (CANDY, "jugcraft:candy_cavity", "alexscaves:candy_cavity")})
    if not matches: return None
    block_entities = value(root, "block_entities", (10, []))[1]
    structures = value(root, "structures", {})
    starts = value(structures, "starts", {})
    return {
        "biomes": matches,
        "inhabitedTicks": value(root, "InhabitedTime", 0),
        "blockEntities": [value(entity, "id", "unknown") for entity in block_entities],
        "structureStarts": sorted(starts),
        "status": value(root, "Status", "unknown"),
        "automaticResetAllowed": False,
        "reviewRequired": ["Surface and underground builds", "Claims and parties", "Entities and inventories",
                           "Adjacent structure pieces", "Matching backup of region, entities and poi"],
    }

def scan_region(path):
    match = re.fullmatch(r"r\.(-?\d+)\.(-?\d+)\.mca", path.name)
    if not match: raise ValueError("Unexpected region name")
    rx, rz = map(int, match.groups())
    found, errors = [], []
    with path.open("rb") as stream:
        header = stream.read(8192)
        if len(header) != 8192: raise ValueError("Truncated region header")
        size = path.stat().st_size
        for slot in range(1024):
            location = int.from_bytes(header[slot*4:slot*4+4], "big")
            if not location: continue
            cx, cz = rx*32+slot%32, rz*32+slot//32
            try:
                sector, count = location >> 8, location & 255
                if sector < 2 or count == 0 or (sector+count)*4096 > size: raise ValueError("Invalid region sector")
                stream.seek(sector*4096)
                length = struct.unpack(">I", stream.read(4))[0]
                if length < 1 or length > count*4096-4: raise ValueError("Invalid chunk length")
                compression = stream.read(1)[0]
                if compression & 128: raise ValueError("External .mcc chunk; review manually")
                data = unpack(stream.read(length-1), compression)
                root = nouveau_nbt.read(data)[2]
                record = inspect_chunk(root)
                if record:
                    record.update(chunkX=cx, chunkZ=cz, blockBounds=[cx*16, cz*16, cx*16+15, cz*16+15],
                                  region=path.name, chunkSha256=hashlib.sha256(data).hexdigest())
                    found.append(record)
            except (ValueError, OSError, EOFError, IndexError, KeyError, struct.error, UnicodeError, RecursionError) as exc:
                errors.append({"region":path.name,"chunkX":cx,"chunkZ":cz,"error":str(exc)})
    return found, errors

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--world",type=Path,required=True)
    parser.add_argument("--output",type=Path,required=True)
    args=parser.parse_args()
    world=args.world.resolve(); output=args.output.resolve()
    if output==world or world in output.parents: parser.error("Write the report outside the world directory")
    region=world/"dimensions"/"minecraft"/"overworld"/"region"
    if not region.is_dir(): region=world/"region"
    if not region.is_dir(): parser.error("Expected a world root or Overworld dimension folder containing region/")
    report={"schemaVersion":1,"world":str(world),"readOnly":True,"candidates":[],"errors":[],
            "warning":"Low inhabited time and no block entities do NOT prove a chunk has no player builds. No chunks are approved for reset."}
    for path in sorted(region.glob("r.*.*.mca")):
        try:
            found,errors=scan_region(path);report["candidates"].extend(found);report["errors"].extend(errors)
        except (ValueError,OSError) as exc:report["errors"].append({"region":path.name,"error":str(exc)})
    output.write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print(f"Read-only report: {len(report['candidates'])} candy chunks, {len(report['errors'])} unreadable entries. No world files changed.")
    return 1 if report["errors"] else 0

if __name__=="__main__":raise SystemExit(main())
