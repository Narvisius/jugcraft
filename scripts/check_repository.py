"""Validate the documentation foundation; deliberately not a Minecraft build."""
from pathlib import Path
import json
import re
import sys
from urllib.parse import unquote

root = Path(__file__).resolve().parents[1]
required = ["README.md", "CONTRIBUTING.md", "CLAUDE.md", "SECURITY.md",
            "CODE_OF_CONDUCT.md", "LICENSE_POLICY.md", "docs/DESIGN.md",
            "docs/ARCHITECTURE.md", "docs/PLATFORM.md", "docs/TESTING.md",
            "docs/ROADMAP.md", "docs/MAINTAINERS.md", "docs/SETUP_STATUS.md",
            "docs/features/TEMPLATE.md", ".github/pull_request_template.md",
            ".github/ISSUE_TEMPLATE/feature.yml", ".github/ISSUE_TEMPLATE/integration.yml",
            ".github/ISSUE_TEMPLATE/bug.yml", ".github/ISSUE_TEMPLATE/config.yml",
            ".github/workflows/foundation.yml", "project-status.json"]
errors = [f"Missing: {name}" for name in required if not (root / name).is_file()]
try:
    status = json.loads((root / "project-status.json").read_text(encoding="utf-8"))
    if status.get("phase") != "contribution-foundation" or status.get("playable") is not False:
        errors.append("Bootstrap must replace this foundation gate with real platform/build checks.")
except (OSError, ValueError) as exc:
    errors.append(f"Invalid project-status.json: {exc}")
for path in root.rglob("*"):
    if ".git" in path.parts or not path.is_file():
        continue
    if path.suffix in {".java", ".kt", ".gradle", ".jar"} or path.name.endswith(".gradle.kts"):
        errors.append(f"Implementation requires approved platform bootstrap: {path.relative_to(root)}")
    if path.suffix != ".md":
        continue
    for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", path.read_text(encoding="utf-8")):
        if re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:", target) or target.startswith("#"):
            continue
        if target.startswith("../../issues/"):
            continue  # GitHub repository-relative issue form links.
        local = unquote(target.split("#")[0].split("?")[0])
        if local and not (path.parent / local).exists():
            errors.append(f"Broken link in {path.relative_to(root)}: {target}")
if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
print("PASS: contribution foundation and local documentation links. No gameplay/build tests performed.")
