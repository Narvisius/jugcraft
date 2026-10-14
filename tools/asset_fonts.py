"""Locate the same DejaVu font files on Linux and Windows; never silently change artwork fonts."""
import os
from pathlib import Path


def font_path(name):
    directory = Path(os.environ.get("JUGCRAFT_FONT_DIR", "/usr/share/fonts/truetype/dejavu"))
    path = directory / name
    if not path.is_file():
        raise FileNotFoundError(
            f"Missing DejaVu font {name}. Install DejaVu fonts or set JUGCRAFT_FONT_DIR to their ttf directory."
        )
    return str(path)
