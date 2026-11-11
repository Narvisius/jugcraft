"""Generated files that say the same thing stay as they are.

A generator run rewrites what it makes. A PNG or a gzip written by another version of the libraries can come out as
other bytes holding the very same picture or data, which then reads as a change: it lands in the next package, or gets
"put back" by hand, and putting back the wrong one ships a file that no longer fits the others (r198's tower diorama:
its palette texture was put back as "the same picture" when four of its colours had moved, 10 November 2026). These two
write a file only when what it holds really differs, so a change on disk is always a change in content.
"""
from __future__ import annotations

import gzip
from pathlib import Path

import numpy as np
from PIL import Image


def png(image, path, **options):
    """IMAGE saved at PATH, unless the picture there already has the same pixels (as RGBA). True when written."""
    path = Path(path)
    if path.is_file():
        try:
            with Image.open(path) as old:
                if old.size == image.size and np.array_equal(np.asarray(old.convert("RGBA")),
                                                             np.asarray(image.convert("RGBA"))):
                    return False
        except OSError:
            pass                                        # unreadable: write it afresh
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, **options)
    return True


def gz(payload, path, **options):
    """PAYLOAD (bytes) gzipped at PATH with no time stamp, unless the file there already unpacks to the same bytes.
    True when written."""
    path = Path(path)
    if path.is_file():
        try:
            if gzip.decompress(path.read_bytes()) == payload:
                return False
        except (OSError, EOFError):
            pass
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "wb") as raw, gzip.GzipFile(filename="", fileobj=raw, mode="wb", mtime=0, **options) as f:
        f.write(payload)
    return True
