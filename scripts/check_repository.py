"""Validate repository structure and documentation links; deliberately not a Minecraft build."""
from pathlib import Path
import json
import re
import sys
from urllib.parse import unquote

root = Path(__file__).resolve().parents[1]
required = ["README.md", "CONTRIBUTING.md", "CLAUDE.md", "SECURITY.md",
            "CODE_OF_CONDUCT.md", "LICENSE_POLICY.md", "docs/DESIGN.md",
            "docs/ARCHITECTURE.md", "docs/CONTENT_BRANCHES.md", "docs/PLATFORM.md", "docs/TESTING.md",
            "docs/ROADMAP.md", "docs/MAINTAINERS.md", "docs/SETUP_STATUS.md",
            "docs/features/TEMPLATE.md", ".github/pull_request_template.md",
            ".github/ISSUE_TEMPLATE/feature.yml", ".github/ISSUE_TEMPLATE/integration.yml",
            ".github/ISSUE_TEMPLATE/bug.yml", ".github/ISSUE_TEMPLATE/config.yml",
            ".github/workflows/foundation.yml", "project-status.json"]
bootstrap_required = ["LICENSE", "build.gradle", "settings.gradle", "gradle.properties", "gradlew",
                      "gradlew.bat", "gradle/wrapper/gradle-wrapper.properties",
                      "gradle/wrapper/gradle-wrapper.jar", "src/main/resources/fabric.mod.json",
                      ".github/workflows/build.yml", "tools/check_mod_data.py"]
errors = [f"Missing: {name}" for name in required if not (root / name).is_file()]
phase = None
try:
    status = json.loads((root / "project-status.json").read_text(encoding="utf-8"))
    phase = status.get("phase")
    if phase not in {"contribution-foundation", "bootstrap"}:
        errors.append(f"Unknown phase in project-status.json: {phase}")
    if status.get("playable") is not False:
        errors.append("Do not mark the project playable until a build and two-client test are verified.")
except (OSError, ValueError) as exc:
    errors.append(f"Invalid project-status.json: {exc}")
if phase == "bootstrap":
    # Real compilation happens in the Build workflow (./gradlew build).
    errors += [f"Missing bootstrap file: {name}" for name in bootstrap_required if not (root / name).is_file()]
for path in root.rglob("*"):
    if {".git", "build", ".gradle", "run"} & set(path.parts) or not path.is_file():
        continue
    if phase != "bootstrap" and (path.suffix in {".java", ".kt", ".gradle", ".jar"} or path.name.endswith(".gradle.kts")):
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
print(f"PASS: repository structure ({phase}) and local documentation links. No gameplay/build tests performed.")
