# Setup status

- Local contribution files: prepared.
- Local foundation check: passed.
- GitHub repository: https://github.com/jimbozoomer-byte/jugcraft (public).
- Published initial commit: 511b854d9a465175315440271fe35dd9d9561cce.
- Initial Actions run: passed, https://github.com/jimbozoomer-byte/jugcraft/actions/runs/36658871101.
- Main branch protection: configured during repository setup; GitHub Settings is the authoritative current state. Required PR, zero independent/code-owner approvals during solo-maintainer development, resolved conversations, up-to-date passing repository check, no force push/deletion, including administrators.
- CODEOWNERS: @jimbozoomer-byte owns review of all files.
- Merge strategy: squash only; merged branches automatically deleted; auto-merge disabled.
- Workflow permissions: read-only; workflow PR approval disabled.
- Project license: MIT, selected by the owner on 30 September 2026 (see LICENSE and LICENSE_POLICY.md).
- Minecraft/loader/build pins: Minecraft 26.3 + Fabric; use the exact current pins in PLATFORM.md and distribution/frameworks.lock.json. The playable scaffold and feature implementations exist; inspect WHAT_EXISTS.md and the source before starting a contribution. Server and real-client test evidence is recorded in feature documents and the Build workflow. A two-player release playtest is a separate check, not a prerequisite to starting development.

This status distinguishes prepared files from controls actually active on GitHub.

The owner can merge their own PR after checks pass; GitHub does not require a self-approval. The initial Fabric bootstrap is explicitly authorized without a separate proposal issue. Claude write access must be connected separately through a write-capable Claude Code/GitHub integration.
