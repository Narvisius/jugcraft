# Maintainer setup and review

Repository files do not enforce GitHub settings by themselves. Complete and verify these controls on GitHub before opening implementation contributions.

## Repository settings

- Public repository named `jugcraft`; enable Issues and Pull Requests.
- Allow squash merge, delete merged feature branches, disable auto-merge initially.
- Protect `main`: require a PR, at least one independent approval, dismiss stale approvals, resolve conversations, require up-to-date branches and the `repository` check from the Foundation workflow.
- Block force pushes and deletion. Apply protections to administrators as well where supported; use no routine bypass actors.
- Add a real CODEOWNERS file after the GitHub owner/maintainers are known. Use actual users with write access. Require code-owner review for workflows, core interfaces, progression and releases. Never leave fake handles in an active CODEOWNERS file.
- Run the Foundation workflow once before selecting its required check. After bootstrap add compilation/game-test checks as required. Verify protection with a test PR; a file describing protection is not evidence it is active.
- Keep Actions on GitHub-hosted runners for untrusted PRs, read-only token permissions, and no repository/environment secrets exposed. Require approval for outside-contributor workflow runs. Do not use `pull_request_target` to run contributor code.
- Enable private vulnerability reporting when available. Configure a named maintainer contact if unavailable.

## Triage

Suggested labels: proposal, integration, approved, needs-design, blocked-platform, bug, documentation, good-first-issue. Create labels before relying on them. Approve scope and assign one owner in the issue; record reasons when declining. New currencies, progression tiers, dependencies, worldgen and shared APIs require explicit design review.

## Review checklist

1. Does it connect to the design and approved issue?
2. Are implementation and diff understandable? Review AI-generated code as carefully as human code.
3. Are checks real and relevant? Review changes to the checker/workflows themselves.
4. Has multiplayer and integration evidence been supplied under docs/TESTING.md?
5. Are performance, abuse, data migration and dependency risks resolved?
6. Are rights/provenance clear and the project license chosen?
7. Has the combined release been staged before promotion?

Use a small trusted maintainer team for merge authority. Community participants use forks. Never give contributors server credentials merely to test a PR. Keep deployments and production world administration separate from contribution builds.

## Initial setup tracking

Track actual state in docs/SETUP_STATUS.md. Do not mark remote creation, branch protection, Actions success, or playable bootstrap complete until verified.

References: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches and https://docs.github.com/en/actions/reference/security/securely-using-pull_request_target
