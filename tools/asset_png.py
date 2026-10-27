"""Keep authored PNG bytes when regenerated pixels are unchanged across Pillow versions."""
from PIL import Image


def save_rgba(image, target):
    image = image.convert("RGBA")
    if target.is_file():
        try:
            with Image.open(target) as previous:
                if previous.size == image.size and previous.convert("RGBA").tobytes() == image.tobytes():
                    return
        except OSError:
            pass  # A corrupt output must be replaced, not silently accepted.
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target, "PNG")
