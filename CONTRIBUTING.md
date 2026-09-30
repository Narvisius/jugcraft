# Contributing to Jugcraft

## Owner-authorized development

The owner authorized Claude to build the Minecraft 26.3 + Fabric bootstrap without a separate proposal issue or second maintainer. That bootstrap is done: the scaffold, pinned toolchain, Build workflow, base materials, machines and power were merged in PRs #4–#7 (see [docs/PLATFORM.md](docs/PLATFORM.md)). The project license is MIT (see [LICENSE_POLICY.md](LICENSE_POLICY.md)). Still outstanding: running the client and dedicated server, and the two-client playtest.

Main requires a PR and passing checks, but zero independent approvals while there is one maintainer. The owner can merge their own PR after reviewing it. CODEOWNERS routes review; its approval is not mandatory. Keep no-force-push/no-deletion and CI protections. Restore independent review when the owner chooses to add maintainers.


## 1. Propose the idea first

Open a Feature proposal or Existing mod integration issue using the Issues tab. Search existing issues first. Describe the player experience, specialty, progression tier, inputs, outputs, and connections to other systems. Read docs/CONTENT_BRANCHES.md for the content scope. State which connections are required or optional, how trade/solo routes work, and why the specialty remains useful without mastering every branch. Small documentation corrections and bug fixes can go straight to a PR.

Wait for a maintainer to approve the scope in the issue before starting substantial work. Maintainers should assign one lead contributor to avoid duplicate work. Community proposals remain welcome. The project license is MIT (see LICENSE_POLICY.md), and the platform bootstrap is merged, so implementation proposals can now build on it.

## 2. Fork, branch, commit

Click Fork on GitHub and copy your fork's clone URL from its Code menu. Clone that URL locally, enter the repository, then:

```sh
git switch -c feature/short-feature-name
# Make your focused change.
python scripts/check_repository.py
git add <the-specific-files-you-changed>
git commit -m "feat: describe the player-visible change"
git push -u origin feature/short-feature-name
```

Replace angle-bracket placeholders with your actual file paths; do not paste them literally. Your fork is `origin`. To update it, use GitHub's Sync fork, then fetch and merge your fork's main into your feature branch, resolving conflicts and rerunning checks. Do not commit generated builds, worlds, logs, credentials, or third-party JARs.

## 3. Use the shared AI instructions

For AI-assisted feature code, select Claude Opus 5.5 in your coding tool and read CLAUDE.md plus the linked design documents. Declare the actual model and your contribution in the PR. If unavailable, request a maintainer exception before implementation; never mislabel another model. No model requirement applies to human-only proposals, art, or docs. A contributor must understand and take responsibility for the result.

Useful starter prompt:

> Read CLAUDE.md, CONTRIBUTING.md, docs/DESIGN.md, docs/ARCHITECTURE.md, docs/PLATFORM.md and the approved issue. Summarize its connections to progression. Implement only that approved scope on my branch. Do not change platform versions or add dependencies. Run the relevant checks and report actual results and limitations. Prepare the PR text without claiming tests you did not perform.

## 4. Open a pull request

On GitHub choose Compare & pull request; target the upstream repository's `main`. Keep one feature per PR, fill every applicable template section, link the approved issue, and include reproducible evidence. Add a feature record under `docs/features/` using the template there. For gameplay features, name both the upstream input and downstream use; for infrastructure explain the systems it supports instead. Screenshots are helpful but do not replace tests.

Mark unfinished work Draft. Fix review feedback in the same branch. Rebase or merge updated main as needed and rerun tests. Only maintainers merge. Prefer squash merge for one clear feature commit and easy source rollback.

## 5. What reviewers require

- Fits the design and has meaningful connections, not an isolated progression tree.
- Compiles against the pinned platform once bootstrapped; passes relevant automated checks.
- Dedicated-server test with two clients for multiplayer gameplay changes.
- Reconnect, restart, persistence, and existing-feature interactions tested.
- Exploit and performance evidence proportionate to the feature.
- Assets and dependencies have recorded provenance and compatible permissions.
- Upgrade and disable behavior documented; existing saves do not silently lose content.

See docs/TESTING.md. Documentation-only PRs explain why gameplay checks do not apply. Never invent evidence to fill the template. Passing automated checks is necessary but not sufficient for approval.
