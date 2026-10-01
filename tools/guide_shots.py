"""Turns the guide screenshots from the GuideScreenshotGameTests client test (files named NNNN_guide_<name>.png,
run with JUGCRAFT_GUIDE_SHOTS=1) into the guide book pictures: centre-cropped to 16:9 and resized to 512x288 in
textures/gui/guide/<name>.png. Usage: python tools/guide_shots.py <folder with the screenshots>
"""
import re
import sys
from pathlib import Path

from PIL import Image

from guide_books import SCREENSHOTS, SHOTS

W, H = 512, 288


def main(folder):
    found = {}
    for path in sorted(Path(folder).glob("*.png")):
        match = re.fullmatch(r"(?:\d+_)?guide_(.+)\.png", path.name)
        if match and match.group(1) in SCREENSHOTS:
            found[match.group(1)] = path  # the latest numbered one wins
    missing = [name for name in SCREENSHOTS if name not in found]
    if missing:
        sys.exit("missing screenshots: " + ", ".join(missing))
    SHOTS.mkdir(parents=True, exist_ok=True)
    for name, path in found.items():
        image = Image.open(path).convert("RGB")
        w, h = image.size
        if w * 9 > h * 16:
            cw, ch = h * 16 // 9, h
        else:
            cw, ch = w, w * 9 // 16
        left, top = (w - cw) // 2, (h - ch) // 2
        image.crop((left, top, left + cw, top + ch)).resize((W, H), Image.LANCZOS).save(SHOTS / f"{name}.png", optimize=True)
        print(f"{name}: {path.name} {w}x{h} -> {W}x{H}")


if __name__ == "__main__":
    main(sys.argv[1])
