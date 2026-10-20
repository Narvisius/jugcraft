"""Delete stale generator-owned files without deleting authored resources."""
from pathlib import Path


def prune(resources: Path, previous: set[str], current: set[str]):
    root = resources.resolve()
    stale_paths = [(root / relative).resolve() for relative in previous - current]
    # Validate the entire manifest before deleting anything.
    if any(not path.is_relative_to(root) or path == root for path in stale_paths):
        raise ValueError("Generated resource manifest escapes resource directory")
    for path in stale_paths:
        if path.is_file():
            path.unlink()
